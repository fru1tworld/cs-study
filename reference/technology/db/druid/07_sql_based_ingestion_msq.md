# Druid SQL 기반 인제스천 (Multi-Stage Query)

> 원본: https://druid.apache.org/docs/latest/multi-stage-query/
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/concepts
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/reference
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/known-issues

Multi-Stage Query(MSQ) 태스크 엔진으로 SQL `INSERT`/`REPLACE` 문을 배치 태스크로 실행하는 SQL 기반 인제스천의 개요, 핵심 개념, 문법 레퍼런스, 알려진 제약을 정리합니다.

---

## 목차

1. [개요](#개요)
2. [핵심 개념](#핵심-개념)
3. [EXTERN 함수](#extern-함수)
4. [INSERT 문](#insert-문)
5. [REPLACE 문](#replace-문)
6. [PARTITIONED BY와 CLUSTERED BY](#partitioned-by와-clustered-by)
7. [롤업(Rollup)](#롤업rollup)
8. [태스크 실행 구조](#태스크-실행-구조)
9. [컨텍스트 파라미터](#컨텍스트-파라미터)
10. [조인(Join)](#조인join)
11. [Durable Storage 설정](#durable-storage-설정)
12. [제한(Limits)과 오류 코드](#제한limits과-오류-코드)
13. [알려진 제약](#알려진-제약)

---

## 개요

Apache Druid는 Multi-Stage Query(MSQ) 태스크 엔진으로 SQL 기반 배치 인제스천을 지원합니다. SQL `INSERT`와 `REPLACE` 문을 배치 태스크로 실행하며, `SELECT` 쿼리 실행도 실험적(experimental)으로 지원합니다.

MSQ 태스크 엔진에서는 거의 모든 `SELECT` 기능을 사용할 수 있으므로, 인제스천 과정에서 변환·필터·JOIN·집계를 수행하거나 기존 테이블을 조회한 결과로 새 테이블을 만들 수 있습니다.

### 주요 용어

| 용어 | 설명 |
|------|------|
| Controller | 쿼리당 하나 실행되는 `query_controller` 태스크. 쿼리 실행 전체를 관리합니다. |
| Worker | 쿼리를 실제로 실행하는 `query_worker` 태스크. 여러 개를 병렬로 실행할 수 있습니다. |
| Stage | 병렬화된 쿼리 실행 단계. 스테이지 사이에서 worker 간 데이터를 교환합니다. |
| Partition | 각 스테이지 출력의 조각. 마지막 스테이지의 파티션이 Druid 세그먼트가 됩니다. |
| Shuffle | worker 간에 파티션 단위로 데이터를 교환하는 과정. 클러스터링 키 기준으로 정렬됩니다. |

### 보안

`EXTERN`, `S3`, `LOCALFILES` 등의 테이블 함수를 사용하려면 `EXTERNAL` 리소스 타입에 대한 READ 권한이 필요합니다. 권한이 없으면 403 오류가 발생합니다.

---

## 핵심 개념

### MSQ 태스크 엔진

MSQ 태스크 엔진은 SQL 문을 Middle Manager에서 배치 태스크로 실행합니다. `INSERT`/`REPLACE` 태스크는 다른 배치 인제스천과 동일한 방식으로 세그먼트를 발행(publish)합니다. 쿼리 하나당 최소 두 개의 태스크 슬롯(controller 1개 + worker 1개)이 필요합니다.

### SQL 확장 요약

- **EXTERN**: 네이티브 배치 인제스천의 input source와 input format으로 외부 데이터를 읽습니다. 여러 파일은 worker 태스크에 나눠 병렬로 읽지만, 파일 하나를 여러 worker에 분할하지는 않습니다. 큰 파일의 병렬성을 높이려면 입력 데이터를 미리 여러 파일로 쪼개는 것이 좋습니다.
- **INSERT**: 새 데이터소스를 만들거나 기존 데이터소스에 데이터를 추가합니다. 표준 SQL과 달리 테이블 생성과 데이터 추가 사이에 문법적 차이가 없습니다. 태스크 슬롯이 충분하면 같은 데이터소스에 여러 INSERT 문을 동시에 실행할 수 있습니다. INSERT는 완료 시점에 새 세그먼트를 생성하므로 마이크로배치보다는 큰 배치 인제스천에 적합합니다.
- **REPLACE**: `OVERWRITE` 절로 데이터소스 전체 또는 특정 시간 범위의 데이터를 덮어씁니다. REPLACE 문은 대상 데이터소스의 대상 시간 범위에 배타적 쓰기 락(exclusive write lock)을 잡습니다. REPLACE로 생성한 세그먼트는 디멘션 기반 세그먼트 프루닝(pruning)을 지원하지만, INSERT로 생성한 세그먼트는 지원하지 않습니다.

### 기본 타임스탬프(__time)

Druid 테이블에는 항상 기본 타임스탬프 컬럼 `__time`이 있습니다. 이 컬럼이 시간 기반 파티셔닝의 기준이 되며, 날짜/시간 함수로 값을 설정할 수 있습니다. `PARTITIONED BY ALL`을 사용하면 시간 파티셔닝이 비활성화되고 `__time`은 1970-01-01이 기본값이 됩니다.

---

## EXTERN 함수

### 입력(입력 소스)으로 사용

두 가지 문법을 지원합니다.

**방식 1: JSON 시그니처**

```sql
SELECT <column>
FROM TABLE(
  EXTERN(
    '<Druid input source>',
    '<Druid input format>',
    '<row signature>'
  ))
```

**방식 2: SQL EXTEND 절**

```sql
SELECT <column>
FROM TABLE(
  EXTERN(
    inputSource => '<Druid input source>',
    inputFormat => '<Druid input format>'
  )) (<columns>)
```

row signature의 각 컬럼은 이름과 타입으로 구성하며, 타입으로 `string`, `long`, `double`, `float`를 사용할 수 있습니다.

### 출력(내보내기) 대상으로 사용

`INSERT INTO EXTERN(...)`으로 쿼리 결과를 외부 저장소로 내보낼 수 있습니다.

```sql
SET rowsPerPage=<number_of_rows>;
INSERT INTO EXTERN(<destination function>)
AS CSV
SELECT <column>
FROM <table>
```

**S3로 내보내기**

```sql
INSERT INTO EXTERN(
  s3(bucket => 'your_bucket', prefix => 'prefix/to/files')
)
AS CSV
SELECT <column>
FROM <table>
```

**GCS로 내보내기**

```sql
INSERT INTO EXTERN(
  google(bucket => 'your_bucket', prefix => 'prefix/to/files')
)
AS CSV
SELECT <column>
FROM <table>
```

**로컬 저장소로 내보내기**

```sql
INSERT INTO EXTERN(
  local(exportPath => 'exportLocation/query1')
)
AS CSV
SELECT <column>
FROM <table>
```

내보내기 제약:

- `INSERT` 문만 지원합니다.
- 내보내기 형식은 CSV만 지원합니다.
- `PARTITIONED BY`, `CLUSTERED BY`는 지원하지 않습니다.

내보내기 시 Druid는 `_symlink_format_manifest` 하위 디렉터리에 메타데이터 파일을 만들며, `_symlink_format_manifest/manifest` 디렉터리의 manifest 파일이 symlink manifest 형식으로 내보낸 파일들의 절대 경로를 나열합니다.

---

## INSERT 문

```sql
INSERT INTO <table name>
< SELECT query >
PARTITIONED BY <time frame>
[ CLUSTERED BY <column list> ]
```

표준 SQL과 달리, INSERT는 위치(position)가 아니라 컬럼 이름을 기준으로 대상 테이블에 데이터를 적재합니다.

---

## REPLACE 문

**전체 덮어쓰기**

```sql
REPLACE INTO <target table>
OVERWRITE ALL
< SELECT query >
PARTITIONED BY <time granularity>
[ CLUSTERED BY <column list> ]
```

**특정 시간 범위 덮어쓰기**

```sql
REPLACE INTO <target table>
OVERWRITE WHERE __time >= TIMESTAMP '<lower bound>'
  AND __time < TIMESTAMP '<upper bound>'
< SELECT query >
PARTITIONED BY <time granularity>
[ CLUSTERED BY <column list> ]
```

---

## PARTITIONED BY와 CLUSTERED BY

### PARTITIONED BY

데이터는 `PARTITIONED BY` 세분성(granularity)이 정의하는 시간 청크(time chunk)마다 하나 이상의 세그먼트로 나뉩니다. `HOUR`, `DAY`가 흔히 쓰이며, 시간 범위 필터를 통한 쿼리 최적화와 세밀한 락킹에 유리합니다.

사용할 수 있는 세분성:

- 키워드: `HOUR`, `DAY`, `MONTH`, `YEAR`
- ISO 8601 기간 문자열: `PT1S`, `PT1M`, `PT5M`, `PT10M`, `PT15M`, `PT30M`, `PT1H`, `PT6H`, `P1D`, `P1W`, `P1M`, `P3M`, `P1Y`
- `ALL` 또는 `ALL TIME` (시간 파티셔닝 비활성화)
- 함수 형태: `TIME_FLOOR(__time, 'granularity_string')` 또는 `FLOOR(__time TO TimeUnit)`

### CLUSTERED BY

```sql
CLUSTERED BY <column list>
```

시간 청크 내부에서 세그먼트를 2차 파티셔닝하고, 세그먼트 안의 행을 정렬합니다. `forceSegmentSortByTime`을 `false`로 설정하지 않는 한 시스템이 `CLUSTERED BY` 컬럼 목록 앞에 `__time`을 암묵적으로 붙입니다.

디멘션 기반 세그먼트 프루닝을 활성화하려면 다음 조건을 만족해야 합니다.

- 클러스터링이 단일 값(single-valued) string 컬럼으로 시작해야 합니다.
- 세그먼트가 INSERT가 아닌 REPLACE 문으로 생성되어야 합니다.

---

## 롤업(Rollup)

롤업은 인제스천 시점에 데이터를 미리 집계합니다. 세 가지가 필요합니다.

1. `GROUP BY`를 사용합니다. GROUP BY 컬럼은 디멘션이 되고, 집계 함수는 메트릭이 됩니다.
2. 컨텍스트 파라미터 `finalizeAggregations: false`를 설정합니다.
3. 지원되는 집계 함수를 사용합니다: `COUNT`(쿼리 시점에는 `SUM`으로 전환), `SUM`, `MIN`, `MAX`, `EARLIEST`/`EARLIEST_BY`, `LATEST`/`LATEST_BY`, 그리고 각종 근사 distinct count 및 sketch 함수.

---

## 태스크 실행 구조

### 실행 흐름

`/druid/v2/sql/task` API 엔드포인트로 쿼리를 제출하면 다음과 같이 진행됩니다.

1. Broker가 SQL을 네이티브 쿼리로 계획(plan)합니다.
2. Broker가 쿼리를 `query_controller` 태스크로 감쌉니다.
3. Controller가 `maxNumTasks` 파라미터에 따라 worker 태스크를 기동합니다.
4. `query_worker` 타입의 worker 태스크들이 쿼리를 실행합니다.
5. 결과를 controller에 반환하거나(SELECT), 세그먼트를 발행합니다(INSERT/REPLACE).

### 병렬성

`maxNumTasks`는 controller를 포함한 전체 태스크 수를 결정하며, 최솟값은 2(worker 1개 + controller 1개)입니다. 클러스터의 가용 슬롯보다 크게 설정하면 `TaskStartTimeout` 오류가 발생합니다. worker 태스크는 단일 스레드로 동작합니다.

### 메모리 사용

- JVM heap은 processor 번들과 worker 번들로 나뉘며 각각 37.5%를 차지합니다.
- 파티션 경계 결정용 sketch는 가용 메모리의 10% 또는 300MB 중 작은 값으로 제한됩니다.
- Direct memory는 `(druid.processing.numThreads + 1) * druid.processing.buffer.sizeBytes` 이상의 버퍼 요구량을 수용하도록 설정해야 합니다.

### 디스크 사용

worker는 다음 용도로 로컬 디스크를 사용합니다.

- 입력 파일의 임시 복사본
- 세그먼트 생성 중간 데이터
- 외부 정렬(external sort)
- 셔플 중 스테이지 출력 저장

태스크 작업 디렉터리(`druid.indexer.task.baseDir`)는 태스크당 전체 출력 데이터셋의 압축 복사본을 담을 수 있어야 합니다.

---

## 컨텍스트 파라미터

| 파라미터 | 적용 범위 | 설명 | 기본값 |
|----------|-----------|------|--------|
| `maxNumTasks` | SELECT, INSERT, REPLACE | controller 태스크를 포함해 기동할 최대 태스크 수. 최솟값은 2(controller 1 + worker 1). | 2 |
| `taskAssignment` | SELECT, INSERT, REPLACE | 태스크 할당 방식. `max` 또는 `auto`. `auto`는 파일 크기를 파악할 수 있을 때 태스크당 512MiB, 10,000개 파일을 넘지 않는 선에서 최소한의 태스크를 사용합니다. | `max` |
| `finalizeAggregations` | SELECT, INSERT, REPLACE | 집계 결과를 최종(finalized) 타입으로 반환할지, 중간(intermediate) 타입으로 반환할지 결정합니다. | `true` |
| `arrayIngestMode` | INSERT, REPLACE | ARRAY 저장 방식. `array`(SQL 표준 호환) 또는 `mvd`(하위 호환, VARCHAR만 지원). | `mvd` |
| `sqlJoinAlgorithm` | SELECT, INSERT, REPLACE | 조인 알고리즘. `broadcast` 또는 `sortMerge`. | `broadcast` |
| `maxRowsInMemory` | INSERT, REPLACE | 세그먼트 생성 중 디스크로 flush하기 전 메모리에 보관할 최대 행 수. | 100,000 |
| `rowsInMemory` | INSERT, REPLACE | `maxRowsInMemory`의 다른 표기. | 100,000 |
| `segmentSortOrder` | INSERT, REPLACE | 기본 세그먼트 정렬 순서를 재정의합니다. 쉼표 구분 또는 JSON 배열 형식. | 빈 목록 |
| `forceSegmentSortByTime` | INSERT, REPLACE | `true`(기본값)이면 `CLUSTERED BY` 앞에 `__time`을 붙입니다. | `true` |
| `maxParseExceptions` | SELECT, INSERT, REPLACE | 쿼리가 `TooManyWarningsFault`로 중단되기 전까지 무시할 파싱 예외의 최대 개수. `-1`이면 모두 무시. | 0 |
| `rowsPerSegment` | INSERT, REPLACE | 세그먼트당 목표 행 수. 실제 행 수는 다소 많거나 적을 수 있습니다. | 3,000,000 |
| `indexSpec` | INSERT, REPLACE | 세그먼트 생성에 사용할 indexSpec. JSON 문자열 또는 객체. | indexSpec 기본값 |
| `durableShuffleStorage` | SELECT, INSERT, REPLACE | 셔플에 durable storage를 사용할지 여부. 서버 수준에서 `druid.msq.intermediate.storage.enable=true`로 durable storage를 설정해야 사용할 수 있습니다. | `false` |
| `faultTolerance` | SELECT, INSERT, REPLACE | worker 재시도를 포함한 내결함성 모드 활성화. | `false` |
| `selectDestination` | SELECT | 결과 저장 위치. `taskReport` 또는 `durableStorage`. | `taskReport` |
| `waitUntilSegmentsLoad` | INSERT, REPLACE | 설정하면 인제스천 쿼리가 생성된 세그먼트의 로드가 끝날 때까지 기다린 후 종료합니다. | `false` |
| `includeSegmentSource` | SELECT, INSERT, REPLACE | 쿼리 대상 범위. `NONE`(딥 스토리지(deep storage)만) 또는 `REALTIME`(realtime 태스크 포함). | `NONE` |
| `rowsPerPage` | SELECT | `selectDestination`이 `durableStorage`일 때 페이지당 목표 행 수. | 100,000 |
| `skipTypeVerification` | INSERT, REPLACE | 마이그레이션 시 string array와 multi-value 디멘션의 타입 검증을 비활성화합니다. 쉼표 구분 또는 JSON 배열. | 빈 목록 |
| `failOnEmptyInsert` | INSERT, REPLACE | `true`이면 출력 행이 0개일 때 오류를 던집니다. `false`(기본값)이면 INSERT가 no-op이 됩니다. | `false` |
| `storeCompactionState` | REPLACE | 컴팩션 상태 추적을 위한 세그먼트 메타데이터를 저장합니다. | `false` |
| `removeNullBytes` | SELECT, INSERT, REPLACE | `true`이면 MSQ 엔진이 데이터를 읽을 때 string 필드의 null byte를 제거합니다. | `false` |
| `includeAllCounters` | SELECT, INSERT, REPLACE | Druid 31 이후 추가된 카운터를 포함할지 여부. 하위 호환용 옵션. | `true` |
| `maxFrameSize` | SELECT, INSERT, REPLACE | MSQ 엔진 내부 데이터 전송에 사용하는 frame 크기. | 1,000,000 (1MB) |
| `maxInputFilesPerWorker` | SELECT, INSERT, REPLACE | worker당 최대 입력 파일/세그먼트 수. 초과하면 `TooManyInputFiles`로 실패합니다. | 10,000 |
| `maxPartitions` | SELECT, INSERT, REPLACE | 단일 스테이지의 최대 출력 파티션 수. INSERT/REPLACE에서는 세그먼트 생성을 제어합니다. | 25,000 |
| `maxThreads` | SELECT, INSERT, REPLACE | 처리에 사용할 최대 스레드 수. 0보다 크고 기본 스레드 수보다 작을 때만 효과가 있습니다. | 미설정(기본값 사용) |

---

## 조인(Join)

### Broadcast 조인 (기본)

```sql
SET sqlJoinAlgorithm='broadcast';
SELECT orders.__time, products.name, customers.name
FROM orders
LEFT JOIN products ON orders.product_id = products.id
LEFT JOIN customers ON orders.customer_id = customers.id
GROUP BY 1, 2
PARTITIONED BY HOUR
CLUSTERED BY name
```

- 지원 타입: `LEFT JOIN`, `INNER JOIN`, `CROSS JOIN`
- 제약: base 입력이 아닌 모든 leaf 입력은 broadcast 테이블 크기 제한을 넘을 수 없습니다. 제한은 worker당 processor 메모리 번들의 30%입니다.

### Sort-Merge 조인

```sql
SET sqlJoinAlgorithm='sortMerge';
SELECT eventstream.__time, eventstream.user_id, users.signup_date
FROM eventstream
LEFT JOIN users ON eventstream.user_id = users.id
PARTITIONED BY HOUR
CLUSTERED BY user
```

- 지원 타입: `LEFT`, `RIGHT`, `INNER`, `FULL`, `CROSS`
- 제약: 조인 중 버퍼링 가능한 데이터는 10MB입니다. 조인 양쪽 모두 이 제한을 넘으면 `TooManyRowsWithSameKey` 오류가 발생하고, 한쪽만 넘으면 이 오류가 발생하지 않습니다.

---

## Durable Storage 설정

### 공통 속성

| 파라미터 | 필수 | 설명 | 기본값 |
|----------|------|------|--------|
| `druid.msq.intermediate.storage.enable` | 예 | durable storage 활성화. `true`로 설정. | `false` |
| `druid.msq.intermediate.storage.type` | 예 | 저장소 타입: `s3`, `azure`, `google`. | 없음 |
| `druid.msq.intermediate.storage.tempDir` | 예 | 임시 파일용 로컬 디스크 디렉터리. 설정하지 않으면 태스크 임시 디렉터리를 사용합니다. | 없음 |
| `druid.msq.intermediate.storage.maxRetry` | 아니요 | API 호출 최대 재시도 횟수. | 10 |
| `druid.msq.intermediate.storage.chunkSize` | 아니요 | 임시 저장 청크 크기(5MiB~5GiB). | 100MiB |

### S3 / Google 속성

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `druid.msq.intermediate.storage.bucket` | 예 | 업로드/다운로드에 사용할 S3 또는 Google 버킷. |
| `druid.msq.intermediate.storage.prefix` | 예 | 버킷 경로 앞에 붙는 경로. 클러스터마다 고유해야 합니다. |

### Azure 속성

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `druid.msq.intermediate.storage.container` | 예 | 업로드/다운로드에 사용할 Azure 컨테이너. |
| `druid.msq.intermediate.storage.prefix` | 예 | 컨테이너 경로 앞에 붙는 경로. 클러스터마다 고유해야 합니다. |

### Durable Storage Cleaner

| 파라미터 | 필수 | 설명 | 기본값 |
|----------|------|------|--------|
| `druid.msq.intermediate.storage.cleaner.enabled` | 아니요 | 중간 파일 주기 정리 활성화. | `false` |
| `druid.msq.intermediate.storage.cleaner.delaySeconds` | 아니요 | 태스크 완료 후 정리까지의 지연 시간(초). | 86400 |

---

## 제한(Limits)과 오류 코드

### 제한

| 제한 항목 | 값 | 오류 |
|-----------|-----|------|
| 개별 행 frame 크기 | 1MB | `RowTooLarge` |
| 세그먼트 단위 시간 청크 수 | 5,000 | `TooManyBuckets` |
| worker당 입력 파일/세그먼트 수 | 10,000 | `TooManyInputFiles` |
| 스테이지당 출력 파티션 수 | 25,000 | `TooManyPartitions` |
| 스테이지당 출력 컬럼 수 | 2,000 | `TooManyColumns` |
| 스테이지당 cluster-by 컬럼 수 | 1,500 | `TooManyClusteredByColumns` |
| 스테이지당 worker 수 | 1,000(하드 제한), 메모리에 따른 소프트 제한 | `TooManyWorkers` |
| broadcast 테이블 메모리 | processor 메모리 번들의 30% | `BroadcastTablesTooLarge` |
| sort-merge 조인 버퍼 데이터 | 10MB | `TooManyRowsWithSameKey` |
| 윈도우 파티션 내 행 수 | 100,000 | `TooManyRowsInAWindow` |
| worker 재기동 시도 | 2회(최초 실행 제외) | `TooManyAttemptsForWorker` |
| 전체 작업 재기동 시도 | 100회 | `TooManyAttemptsForJob` |

### 주요 오류 코드

| 코드 | 원인 | 주요 필드 |
|------|------|-----------|
| `BroadcastTablesTooLarge` | broadcast 테이블이 메모리 제한 초과 | `maxBroadcastTablesSize` |
| `CannotParseExternalData` | 외부 데이터 파싱 실패 | `errorMessage` |
| `ColumnTypeNotSupported` | 지원하지 않는 컬럼 타입 | `columnName`, `columnType` |
| `InsertCannotAllocateSegment` | 세그먼트 ID 할당 충돌 | `dataSource`, `interval`, `allocatedInterval` |
| `InsertCannotBeEmpty` | `failOnEmptyInsert=true`인데 출력이 없음 | `dataSource` |
| `InsertTimeNull` | `__time` 필드가 null | (상황에 따라 다름) |
| `InvalidNullByte` | string에 null byte 포함 | `source`, `rowNumber`, `column`, `value`, `position` |
| `RowTooLarge` | 행이 frame 크기 제한 초과 | `maxFrameSize` |
| `TaskStartTimeout` | 제한 시간 안에 worker 기동 실패 | `pendingTasks`, `totalTasks`, `timeout` |
| `TooManyBuckets` | 시간 청크 5,000개 초과 | `maxBuckets` |
| `TooManyInputFiles` | worker당 파일 수 제한 초과 | `numInputFiles`, `maxInputFiles`, `minNumWorker` |
| `TooManyPartitions` | 파티션 25,000개 제한 초과 | `maxPartitions` |
| `TooManyColumns` | 컬럼 2,000개 제한 초과 | `numColumns`, `maxColumns` |
| `TooManyClusteredByColumns` | 클러스터링 컬럼 1,500개 제한 초과 | `numColumns`, `maxColumns`, `stage` |
| `TooManyRowsWithSameKey` | sort-merge 조인 키 버퍼 초과 | `key`, `numBytes`, `maxBytes` |
| `TooManyRowsInAWindow` | 윈도우 파티션 구체화(materialization) 초과 | `numRows`, `maxRows` |
| `WorkerFailed` | worker의 예기치 않은 실패 | `errorMsg`, `workerTaskId` |

---

## 알려진 제약

### MSQ 태스크 런타임

- 내결함성이 부분적으로만 지원됩니다. worker는 예기치 않게 종료되면 다시 기동되지만, controller에는 이런 보호가 없습니다.
- 스테이지 출력이 `druid.indexer.task.baseDir`에 저장되므로 디스크 공간이 고갈되면 `UnknownError`가 발생할 수 있습니다.

### SELECT

- `GROUPING SETS`는 구현되어 있지 않습니다. 이 기능을 사용하는 쿼리는 `QueryNotSupported` 오류를 반환합니다.

### INSERT / REPLACE

- `INSERT INTO tbl (a, b, c) SELECT ...` 같은 컬럼 목록 지정 문법은 지원하지 않습니다. 표준 SQL과 달리 위치가 아니라 컬럼 이름 기준으로 삽입합니다.
- `createBitmapIndex`, `multiValueHandling` 및 일부 `indexSpec` 속성 등 몇몇 인제스천 스펙 옵션은 사용할 수 없습니다.

### EXTERN

- schemaless dimensions 기능은 사용할 수 없습니다. 모든 컬럼과 타입을 명시해야 합니다.
- 파일 매칭 대상이 많으면 controller의 메모리를 많이 사용할 수 있습니다.
- EXTERN은 외부 파일만 읽습니다. Druid 데이터소스를 읽으려면 `FROM`을 사용합니다.

### WINDOW 함수

- 윈도우는 최대 100,000개 요소로 제한됩니다.
- MSQ 엔진에서 `leafOperators`를 피하기 위해 윈도우 처리 뒤에 추가 scan 스테이지가 붙습니다.

### 자동 컴팩션(Automatic Compaction)

- `metricSpec` 필드는 일부 aggregator만 지원합니다.
- 파티셔닝은 dynamic과 range 기반만 지원합니다. string 디멘션 파티셔닝은 가능하지만 multi-valued 디멘션은 제외됩니다.
- `maxTotalRows` 설정은 지원하지 않으므로 대신 `maxRowsPerSegment`를 사용합니다.
- 정렬은 `__time`이 첫 컬럼인 경우로 제한됩니다.
