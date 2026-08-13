# Druid 배치·SQL 기반 인제스천과 데이터 관리

## Druid 배치 인제스천

> 원본: https://druid.apache.org/docs/latest/ingestion/native-batch
> 원본: https://druid.apache.org/docs/latest/ingestion/input-sources
> 원본: https://druid.apache.org/docs/latest/ingestion/hadoop

네이티브 배치 인제스천 태스크(`index_parallel`, `index`)의 구조·튜닝·파티셔닝 방식·다양한 입력 소스(input source) 설정·Hadoop 기반 인제스천의 지원 종료 내용을 정리함.

---

### 목차

1. [네이티브 배치 인제스천 개요](#네이티브-배치-인제스천-개요)
2. [태스크 제출 방법](#태스크-제출-방법)
3. [병렬 태스크 예시](#병렬-태스크-예시)
4. [ioConfig](#ioconfig)
5. [tuningConfig](#tuningconfig)
6. [splitHintSpec](#splithintspec)
7. [partitionsSpec](#partitionsspec)
8. [세그먼트 푸시 모드](#세그먼트-푸시-모드)
9. [병렬성 설정과 용량 계획](#병렬성-설정과-용량-계획)
10. [HTTP 상태 조회 엔드포인트](#http-상태-조회-엔드포인트)
11. [입력 소스](#입력-소스)
12. [Hadoop 기반 인제스천 (지원 종료)](#hadoop-기반-인제스천-지원-종료)
13. [참고 자료](#참고-자료)

---

### 네이티브 배치 인제스천 개요

네이티브 배치 인제스천은 두 가지 태스크 타입을 제공함.

- `index_parallel`: 멀티스레드 배치 인덱싱을 지휘하는 슈퍼바이저(supervisor) 태스크. 슈퍼바이저가 입력 데이터를 분할한 뒤 워커(worker) 태스크를 생성해 각 부분을 동시에 처리 → 모든 작업이 성공하면 세그먼트를 한꺼번에 발행(publish)
- `index`: 단일 스레드로 동작하는 단순 인덱싱 태스크, 개발·테스트 환경에 적합

모든 인제스천 스펙은 세 가지 핵심 섹션으로 구성됨.

- `dataSchema`: 타임스탬프 컬럼·디멘션(dimension)·메트릭(metric)·변환(transform) 등 데이터 저장 방식 정의
- `ioConfig`: 입력 위치와 입력 포맷 지정
- `tuningConfig`: 성능 파라미터 제어. 생략 시 기본값 사용

배치 인제스천은 `bz2`, `gz`, `xz`, `zip`, `sz`(Snappy), `zst`(ZSTD) 압축 포맷을 지원함.

---

### 태스크 제출 방법

1. Load Data UI: 웹 콘솔에서 스펙을 정의하고 제출
2. Tasks API: Overlord의 `/druid/indexer/v1/task` 엔드포인트에 JSON 스펙을 POST로 전송
3. 인덱싱 스크립트: Druid 배포판에 포함된 `bin/post-index-task` 스크립트 사용

---

### 병렬 태스크 예시

`index_parallel` 태스크의 전체 스펙 예시. `single_dim` 파티셔닝과 2개의 동시 서브태스크를 사용함.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_parallel_index_test",
      "timestampSpec": {
        "column": "timestamp"
      },
      "dimensionsSpec": {
        "dimensions": [
          "country", "page", "language", "user", "unpatrolled",
          "newPage", "robot", "anonymous", "namespace",
          "continent", "region", "city"
        ]
      },
      "metricsSpec": [
        { "type": "count", "name": "count" },
        { "type": "doubleSum", "name": "added", "fieldName": "added" },
        { "type": "doubleSum", "name": "deleted", "fieldName": "deleted" },
        { "type": "doubleSum", "name": "delta", "fieldName": "delta" }
      ],
      "granularitySpec": {
        "segmentGranularity": "DAY",
        "queryGranularity": "second",
        "intervals": ["2013-08-31/2013-09-02"]
      }
    },
    "ioConfig": {
      "type": "index_parallel",
      "inputSource": {
        "type": "local",
        "baseDir": "examples/indexing/",
        "filter": "wikipedia_index_data*"
      },
      "inputFormat": {
        "type": "json"
      }
    },
    "tuningConfig": {
      "type": "index_parallel",
      "partitionsSpec": {
        "type": "single_dim",
        "partitionDimension": "country",
        "targetRowsPerSegment": 5000000
      },
      "maxNumConcurrentSubTasks": 2
    }
  }
}
```

---

### ioConfig

- `type`: 태스크 타입, `index_parallel`로 설정
  - 기본값: 없음
  - 필수: 예
- `inputFormat`: 입력 데이터 파싱 방법 지정
  - 기본값: 없음
  - 필수: 예
- `appendToExisting`: 기존 세그먼트를 대체하지 않고 추가 샤드(shard)로 세그먼트 생성 → dynamic 파티셔닝에서만 사용 가능
  - 기본값: false
  - 필수: 아니요
- `dropExisting`: `appendToExisting`이 false일 때, 지정한 interval에 완전히 포함되는 기존 세그먼트를 발행 시점에 모두 교체
  - 기본값: false
  - 필수: 아니요

---

### tuningConfig

- `type`: 태스크 타입, `index_parallel`로 설정
  - 기본값: 없음 / 필수: 예
- `maxRowsInMemory`: 중간 persist를 수행할 행 수 기준
  - 기본값: 1000000 / 필수: 아니요
- `maxBytesInMemory`: persist 전에 힙에 유지할 수 있는 집계 데이터의 메모리 한도
  - 기본값: 최대 JVM 힙의 1/6 / 필수: 아니요
- `maxColumnsToMerge`: 병합 단계에서 한 번에 병합하는 세그먼트 수 제한
  - 기본값: -1(무제한) / 필수: 아니요
- `maxTotalRows`: 사용 중단(deprecated), 대신 `partitionsSpec` 사용
  - 기본값: 20000000 / 필수: 아니요
- `numShards`: 사용 중단(deprecated), 대신 `partitionsSpec` 사용
  - 기본값: null / 필수: 아니요
- `splitHintSpec`: 1단계에서 각 태스크가 읽을 데이터 양을 제어하는 힌트
  - 기본값: size-based / 필수: 아니요
- `partitionsSpec`: 2차 파티셔닝 방식 지정
  - 기본값: dynamic 또는 hashed / 필수: 아니요
- `indexSpec`: 세그먼트 저장 포맷 옵션
  - 기본값: null / 필수: 아니요
- `indexSpecForIntermediatePersists`: 중간 persist 세그먼트의 저장 포맷 옵션
  - 기본값: `indexSpec`과 동일 / 필수: 아니요
- `maxPendingPersists`: 대기열에 쌓을 수 있는 persist의 최대 개수
  - 기본값: 0 / 필수: 아니요
- `forceGuaranteedRollup`: perfect rollup 모드 강제
  - 기본값: false / 필수: 아니요
- `reportParseExceptions`: 파싱 오류 발생 시 인제스천 중단
  - 기본값: false / 필수: 아니요
- `pushTimeout`: 세그먼트 푸시를 기다리는 시간(밀리초)
  - 기본값: 0 / 필수: 아니요
- `segmentWriteOutMediumFactory`: 세그먼트 생성에 사용할 매체(medium)
  - 기본값: druid.peon 설정 / 필수: 아니요
- `maxNumConcurrentSubTasks`: 동시에 실행할 워커 태스크의 최대 개수
  - 기본값: 1 / 필수: 아니요
- `maxRetry`: 태스크 실패 시 최대 재시도 횟수
  - 기본값: 3 / 필수: 아니요
- `maxNumSegmentsToMerge`: 동시에 병합할 수 있는 세그먼트의 최대 개수
  - 기본값: 100 / 필수: 아니요
- `totalNumMergeTasks`: 병합 단계에 사용할 태스크 수
  - 기본값: 10 / 필수: 아니요
- `taskStatusCheckPeriodMs`: 태스크 상태 폴링 주기(밀리초)
  - 기본값: 1000 / 필수: 아니요
- `chatHandlerTimeout`: 세그먼트 보고 타임아웃
  - 기본값: PT10S / 필수: 아니요
- `chatHandlerNumRetries`: 세그먼트 보고 재시도 횟수
  - 기본값: 5 / 필수: 아니요
- `awaitSegmentAvailabilityTimeoutMillis`: 인제스천 완료 후 세그먼트가 조회 가능해질 때까지 기다리는 시간
  - 기본값: 0 / 필수: 아니요

---

### splitHintSpec

첫 단계(입력 읽기)에서 각 워커 태스크에 할당하는 데이터 양을 제어함.

#### 크기 기반(Size-based, 기본값)

- `type`: `maxSize`로 설정
  - 기본값: 없음 / 필수: 예
- `maxSplitSize`: 서브태스크당 최대 바이트 수
  - 기본값: 1GiB / 필수: 아니요
- `maxNumFiles`: 서브태스크당 최대 파일 수
  - 기본값: 1000 / 필수: 아니요

#### 세그먼트 기반(Segments-based, DruidInputSource용)

- `type`: `segments`로 설정
  - 기본값: 없음 / 필수: 예
- `maxInputSegmentBytesPerTask`: 서브태스크당 최대 세그먼트 바이트 수
  - 기본값: 1GiB / 필수: 아니요
- `maxNumSegments`: 서브태스크당 최대 세그먼트 수
  - 기본값: 1000 / 필수: 아니요

---

### partitionsSpec

시간 기반 1차 파티셔닝 아래에서 세그먼트를 나누는 2차 파티셔닝 방식을 지정함.

#### dynamic 파티셔닝

행 수 기준으로 세그먼트를 생성함. 단일 단계로 실행되며, 여러 워커 태스크가 각각 독립적으로 세그먼트를 만들고 임계값을 넘으면 세그먼트를 푸시함.

- `type`: `dynamic`으로 설정
  - 기본값: 없음 / 필수: 예
- `maxRowsPerSegment`: 세그먼트당 행 수 임계값
  - 기본값: 5000000 / 필수: 아니요
- `maxTotalRows`: 세그먼트를 푸시하기 전까지 누적할 총 행 수
  - 기본값: 20000000 / 필수: 아니요

#### hashed 파티셔닝

지정한 디멘션들의 해시 값으로 행을 분배하며, 여러 단계(phase)로 실행됨.

- `type`: `hashed`로 설정
  - 기본값: 없음 / 필수: 예
- `numShards`: 샤드 개수를 명시적으로 지정
  - 기본값: 없음 / 필수: 아니요
- `targetRowsPerSegment`: 파티션당 목표 행 수
  - 기본값: 5000000 / 필수: 아니요
- `partitionDimensions`: 파티셔닝에 사용할 디멘션. 비워 두면 모든 디멘션 사용
  - 기본값: null / 필수: 아니요
- `partitionFunction`: 해시 계산 함수
  - 기본값: `murmur3_32_abs` / 필수: 아니요

실행 단계: (선택) 디멘션 카디널리티 추정 → 부분(partial) 세그먼트 생성 → 부분 세그먼트 병합

#### single_dim (단일 디멘션 범위) 파티셔닝

하나의 디멘션 값 범위로 파티셔닝하여 데이터 지역성(locality)을 높임.

- `type`: `single_dim`으로 설정
  - 기본값: 없음 / 필수: 예
- `partitionDimension`: 범위 파티셔닝에 사용할 디멘션
  - 기본값: 없음 / 필수: 예
- `targetRowsPerSegment`: 파티션당 목표 행 수
  - 기본값: 없음 / 필수: 이 속성 또는 `maxRowsPerSegment` 중 하나
- `maxRowsPerSegment`: 파티션당 소프트 최대 행 수
  - 기본값: 없음 / 필수: 이 속성 또는 `targetRowsPerSegment` 중 하나
- `assumeGrouped`: 입력 데이터가 이미 그룹핑되었다고 가정
  - 기본값: false / 필수: 아니요

실행 단계: 디멘션 분포 산출(히스토그램 구축) → 부분 세그먼트 생성 → 부분 세그먼트 병합

#### range (다중 디멘션 범위) 파티셔닝

single_dim 방식을 여러 디멘션으로 확장하여 세그먼트 분포의 균형을 개선함.

- `type`: `range`로 설정
  - 기본값: 없음 / 필수: 예
- `partitionDimensions`: 파티셔닝에 사용할 디멘션 배열. 쿼리에서 자주 사용하는 순서대로 나열
  - 기본값: 없음 / 필수: 예
- `targetRowsPerSegment`: 파티션당 목표 행 수
  - 기본값: 없음 / 필수: 이 속성 또는 `maxRowsPerSegment` 중 하나
- `maxRowsPerSegment`: 파티션당 소프트 최대 행 수
  - 기본값: 없음 / 필수: 이 속성 또는 `targetRowsPerSegment` 중 하나
- `assumeGrouped`: 입력 데이터가 이미 그룹핑되었다고 가정
  - 기본값: false / 필수: 아니요

---

### 세그먼트 푸시 모드

- 일괄 푸시(Bulk Pushing) — perfect rollup: 태스크 완료 시점에 모든 세그먼트를 한꺼번에 푸시. `forceGuaranteedRollup: true`로 활성화하며, `appendToExisting`과 함께 사용 불가
- 점진적 푸시(Incremental Pushing) — best-effort rollup: 누적 행 수가 `maxTotalRows`를 초과하면 인제스천 도중에 세그먼트를 푸시 → 이후 인제스천 계속 진행

---

### 병렬성 설정과 용량 계획

- `maxNumConcurrentSubTasks`: 슈퍼바이저는 사용 가능한 태스크 슬롯 수와 무관하게 이 값까지 워커 태스크를 생성함. 실행 중 총 태스크 수는 슈퍼바이저를 포함해 `maxNumConcurrentSubTasks + 1`. 슬롯이 부족하면 태스크가 pending 상태로 대기
- 용량 계획: 배치와 스트리밍을 함께 운영한다면, 스트림 인제스천이 막히지 않도록 `(모든 병렬 태스크의 maxNumConcurrentSubTasks 합계 + 슈퍼바이저 수) < 배치 태스크 한도`를 유지 필요

---

### HTTP 상태 조회 엔드포인트

슈퍼바이저 태스크가 제공하는 모니터링 엔드포인트.

- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/mode`: `parallel` 또는 `sequential` 반환
- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/phase`: 현재 단계 이름 반환
- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/progress`: 현재 단계의 진행률 추정치 반환
- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtasks/running`: 실행 중인 워커 태스크 ID 목록 반환
- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtaskspecs`: 전체 워커 태스크 스펙 반환
- `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtaskspec/{SUB_TASK_SPEC_ID}/state`: 워커 태스크 상태(스펙과 시도 이력 포함) 반환

---

### 입력 소스

입력 소스는 네이티브 배치 인덱싱 태스크가 데이터를 읽는 위치를 정의함. 네이티브 병렬 태스크(`index_parallel`)와 단순 태스크(`index`)만 입력 소스를 지원함.

분할 가능(splittable)한 입력 소스는 병렬 태스크에서 워커 태스크에 분산 처리 가능하며, S3, Google Cloud Storage, Azure, HDFS, HTTP, Local, Druid, Combining, Iceberg, Delta Lake가 여기에 해당함.

여러 입력 소스가 메타데이터 추적을 위한 시스템 필드를 지원함.

- `__file_uri`: 파일/객체의 전체 URI
- `__file_bucket`: 스토리지 버킷/컨테이너 이름
- `__file_path`: 파일 경로

인제스천 태스크는 Druid 프로세스를 실행하는 운영 체제 계정으로 동작한다는 점에 유의 필요.

#### S3 입력 소스

필요 익스텐션: `druid-s3-extensions`

Amazon S3에서 객체를 직접 읽음. URI 목록, prefix 목록, 명시적 객체 지정 중 하나를 사용하며, 병렬 태스크에서 분할 가능.

- `type`: `s3`로 설정
  - 기본값: 없음 / 필수: 예
- `uris`: S3 URI 문자열의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `prefixes`: S3 위치 prefix의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objects`: S3 객체(bucket/path 쌍)의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objectGlob`: 객체 경로에 적용할 glob 패턴(예: `**.json`)
  - 기본값: 없음 / 필수: 아니요
- `systemFields`: 반환할 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)
  - 기본값: 없음 / 필수: 아니요
- `endpointConfig`: 기본 S3 endpoint/region 재정의
  - 기본값: 없음 / 필수: 아니요
- `clientConfig`: 재정의한 endpoint에 사용할 S3 클라이언트 속성
  - 기본값: 없음 / 필수: 아니요
- `proxyConfig`: 프록시 설정(host, port, username, password)
  - 기본값: 없음 / 필수: 아니요
- `properties`: 접근 자격 증명과 role 위임(assume role) 설정
  - 기본값: 없음 / 필수: 아니요

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "s3",
      "objectGlob": "**.json",
      "uris": ["s3://foo/bar/file.json", "s3://bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

자격 증명을 직접 지정하는 예시.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "s3",
      "objectGlob": "**.json",
      "uris": ["s3://foo/bar/file.json"],
      "properties": {
        "accessKeyId": "KLJ78979SDFdS2",
        "secretAccessKey": "KLS89s98sKJHKJKJH8721lljkd",
        "assumeRoleArn": "arn:aws:iam::2981002874992:role/role-s3"
      }
    },
    "inputFormat": { "type": "json" }
  }
}
```

#### Google Cloud Storage 입력 소스

필요 익스텐션: `druid-google-extensions`

Google Cloud Storage에서 객체를 직접 읽으며, URI·prefix·객체 지정을 지원함. 분할 가능.

- `type`: `google`로 설정
  - 기본값: 없음 / 필수: 예
- `uris`: GCS URI의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `prefixes`: GCS URI prefix의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objects`: GCS 객체(bucket/path 쌍)의 JSON 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objectGlob`: 객체 경로에 적용할 glob 패턴(예: `**.json`)
  - 기본값: 없음 / 필수: 아니요
- `systemFields`: 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)
  - 기본값: 없음 / 필수: 아니요

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "google",
      "objectGlob": "**.json",
      "uris": ["gs://foo/bar/file.json", "gs://bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

#### Azure 입력 소스

필요 익스텐션: `druid-azure-extensions`

Azure Blob Storage 또는 Azure Data Lake에서 데이터를 읽음. 레거시 `azure` 타입보다 최신 `azureStorage` 타입 사용을 권장.

azureStorage(권장)

- `type`: `azureStorage`로 설정
  - 기본값: 없음 / 필수: 예
- `uris`: `azureStorage://storageAccount/container/path` 형식의 URI
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `prefixes`: `azureStorage://storageAccount/container/prefix` 형식의 URI prefix
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objects`: bucket/path 쌍으로 이루어진 객체 배열
  - 기본값: 없음 / 필수: `uris`, `prefixes`, `objects` 중 하나
- `objectGlob`: glob 패턴(예: `**.json`)
  - 기본값: 없음 / 필수: 아니요
- `systemFields`: 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)
  - 기본값: 없음 / 필수: 아니요
- `properties`: 인증 설정
  - 기본값: 없음 / 필수: 아니요

인증에 사용 가능한 속성.

- `sharedAccessStorageToken`: Shared Access Token 문자열
- `key`: 스토리지 계정 루트 키
- `appRegistrationClientId`: Azure 앱 클라이언트 ID
- `appRegistrationClientSecret`: Azure 앱 클라이언트 시크릿 (`appRegistrationClientId` 사용 시 필수)
- `tenantId`: Azure 테넌트 ID (`appRegistrationClientId` 사용 시 필수)

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "azureStorage",
      "objectGlob": "**.json",
      "uris": ["azureStorage://storageAccount/container/prefix1/file.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

레거시 `azure` 입력 소스는 `azure://container/path` 형식을 사용하며, 신규 구성에는 권장하지 않음.

#### HDFS 입력 소스

필요 익스텐션: `druid-hdfs-storage`

HDFS에서 파일을 직접 읽음. 와일드카드를 지원하며 병렬 태스크에서 분할 가능.

- `type`: `hdfs`로 설정
  - 기본값: 없음 / 필수: 예
- `paths`: HDFS 경로(JSON 배열 또는 쉼표 구분 문자열)
  - 기본값: 없음 / 필수: 예
- `systemFields`: 시스템 필드(`__file_uri`, `__file_path`)
  - 기본값: 없음 / 필수: 아니요

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "hdfs",
      "paths": ["hdfs://namenode_host/foo/bar/file.json", "hdfs://namenode_host/bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

#### HTTP 입력 소스

HTTP/HTTPS로 원격 파일을 읽음. 분할 가능하며, 워커 태스크 하나가 파일 하나를 읽음. 허용 프로토콜(HTTP, HTTPS, FTP, file)은 설정으로 제어 가능.

- `type`: `http`로 설정
  - 기본값: 없음 / 필수: 예
- `uris`: 입력 파일의 URI 목록
  - 기본값: 없음 / 필수: 예
- `httpAuthenticationUsername`: Basic 인증 사용자 이름
  - 기본값: 없음 / 필수: 아니요
- `httpAuthenticationPassword`: Basic 인증 패스워드의 password provider
  - 기본값: 없음 / 필수: 아니요
- `systemFields`: 시스템 필드(`__file_uri`, `__file_path`)
  - 기본값: 없음 / 필수: 아니요

password provider는 환경 변수나 커스텀 구현으로 외부 시크릿 참조 가능.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "http",
      "uris": ["http://example.com/uri1"],
      "httpAuthenticationUsername": "username",
      "httpAuthenticationPassword": "password123"
    },
    "inputFormat": { "type": "json" }
  }
}
```

#### Inline 입력 소스

스펙 안에 데이터를 직접 넣어 인제스천함. 데모나 간단한 테스트에 유용.

- `type`: `inline`으로 설정
  - 필수: 예
- `data`: 인제스천할 인라인 데이터
  - 필수: 예

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "inline",
      "data": "0,values,formatted\n1,as,CSV"
    },
    "inputFormat": { "type": "csv" }
  }
}
```

#### Local 입력 소스

로컬 스토리지의 파일을 읽음. 주로 개념 검증(proof-of-concept) 용도이며 분할 가능.

- `type`: `local`로 설정
  - 필수: 예
- `filter`: 파일 와일드카드 필터
  - 필수: `baseDir`을 지정한 경우 예
- `baseDir`: 재귀적으로 탐색할 디렉터리
  - 필수: `baseDir` 또는 `files` 중 하나 이상
- `files`: 인제스천할 파일 경로 목록
  - 필수: `baseDir` 또는 `files` 중 하나 이상
- `systemFields`: 시스템 필드(`__file_uri`, `__file_path`)
  - 필수: 아니요

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "local",
      "filter": "*.csv",
      "baseDir": "/data/directory",
      "files": ["/bar/foo", "/foo/bar"]
    },
    "inputFormat": { "type": "csv" }
  }
}
```

#### Druid 입력 소스

기존 Druid 세그먼트에서 데이터를 읽어 재색인(reindexing), 롤업(rollup), 재파티셔닝에 활용함. 분할 가능.

- `type`: `druid`로 설정
  - 필수: 예
- `dataSource`: 원본 데이터소스 이름
  - 필수: 예
- `interval`: 시간 범위를 지정하는 ISO-8601 interval
  - 필수: 예
- `filter`: 읽을 행을 제한하는 쿼리 필터
  - 필수: 아니요

주요 특징.

- 타임스탬프 컬럼은 epoch 밀리초 값을 담은 숫자형 `__time` 필드로 나타남
- 입력 데이터소스와 출력 데이터소스가 같아도 무방
- `inputFormat`이 불필요

`wikipedia_raw`를 시간 단위로 롤업해 `wikipedia_rollup`으로 만드는 예시.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_rollup",
      "timestampSpec": {
        "column": "__time",
        "format": "millis"
      },
      "dimensionsSpec": {
        "dimensions": ["countryName", "page"]
      },
      "metricsSpec": [
        { "type": "count", "name": "cnt" }
      ],
      "granularitySpec": {
        "type": "uniform",
        "queryGranularity": "HOUR",
        "segmentGranularity": "DAY",
        "intervals": ["2016-06-27/P1D"],
        "rollup": true
      }
    },
    "ioConfig": {
      "type": "index_parallel",
      "inputSource": {
        "type": "druid",
        "dataSource": "wikipedia_raw",
        "interval": "2016-06-27/P1D"
      }
    },
    "tuningConfig": {
      "type": "index_parallel",
      "partitionsSpec": { "type": "hashed" },
      "forceGuaranteedRollup": true,
      "maxNumConcurrentSubTasks": 1
    }
  }
}
```

#### SQL 입력 소스

필요 익스텐션: MySQL은 `mysql-metadata-storage`, PostgreSQL은 `postgresql-metadata-storage`

RDBMS에서 SQL 쿼리로 데이터를 직접 읽음. 쿼리 하나가 별도의 워커 태스크에서 실행됨.

- `type`: `sql`로 설정
  - 필수: 예
- `database`: 데이터베이스 연결 정보(type, connectorConfig)
  - 필수: 예
- `foldCase`: 컬럼 이름의 대소문자 접기(case folding) 여부
  - 필수: 아니요
- `sqls`: 데이터를 조회할 SQL 쿼리 목록
  - 필수: 예

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "sql",
      "database": {
        "type": "mysql",
        "connectorConfig": {
          "connectURI": "jdbc:mysql://host:port/schema",
          "user": "user",
          "password": "password"
        }
      },
      "sqls": [
        "SELECT * FROM table1 WHERE timestamp BETWEEN '2013-01-01 00:00:00' AND '2013-01-01 11:59:59'",
        "SELECT * FROM table2 WHERE timestamp BETWEEN '2013-01-01 00:00:00' AND '2013-01-01 11:59:59'"
      ]
    }
  }
}
```

권장 사항.

- 불필요한 데이터를 가져오지 않도록 SQL 쿼리를 interval 기준으로 필터링
- 워커 태스크 간 데이터가 고르게 분배되도록 페이지네이션 활용
- 인제스천 중 로컬 저장에 필요한 디스크 공간 충분히 확보
- Middle Manager/Indexer의 데이터 크기 제약 모니터링

#### Combining 입력 소스

분할 가능한 여러 입력 소스를 동시에 읽음. 모든 delegate가 같은 `inputFormat`을 사용해야 함.

- `type`: `combining`으로 설정
  - 필수: 예
- `delegates`: 분할 가능한 입력 소스의 목록
  - 필수: 예

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "combining",
      "delegates": [
        {
          "type": "local",
          "filter": "*.csv",
          "baseDir": "/data/directory",
          "files": ["/bar/foo", "/foo/bar"]
        },
        {
          "type": "druid",
          "dataSource": "wikipedia",
          "interval": "2013-01-01/2013-01-02"
        }
      ]
    },
    "inputFormat": { "type": "csv" }
  }
}
```

#### Iceberg 입력 소스

필요 익스텐션: `druid-iceberg-extensions` (그리고 `druid-s3-extensions` 같은 스토리지별 익스텐션)

Apache Iceberg 테이블 포맷의 데이터를 읽음. 설정한 카탈로그(Hive/REST/Glue)에서 최신 스냅샷을 스캔함.

- `type`: `iceberg`로 설정
  - 기본값: 없음 / 필수: 예
- `tableName`: 카탈로그에 등록된 Iceberg 테이블 이름
  - 기본값: 없음 / 필수: 예
- `namespace`: 테이블의 Iceberg namespace
  - 기본값: 없음 / 필수: 예
- `icebergCatalog`: 카탈로그 설정 객체
  - 기본값: 없음 / 필수: 예
- `icebergFilter`: 읽을 데이터 파일을 줄이는 필터
  - 기본값: 없음 / 필수: 아니요
- `warehouseSource`: 웨어하우스 파일을 읽을 네이티브 입력 소스
  - 기본값: 없음 / 필수: 예
- `snapshotTime`: 특정 스냅샷을 지정하는 ISO8601 타임스탬프
  - 기본값: 없음 / 필수: 아니요
- `residualFilterMode`: 파티션 컬럼이 아닌 컬럼에 걸린 필터 처리 방식(`ignore`/`fail`)
  - 기본값: ignore / 필수: 아니요

카탈로그 타입은 `hive`, `rest`, `glue`, `local`을 지원함. 필터 타입으로는 `equals`, `interval`, `range`, `timeWindow`와 논리 연산자 `and`, `or`, `not` 사용 가능.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "iceberg",
      "tableName": "iceberg_table",
      "namespace": "iceberg_namespace",
      "icebergCatalog": {
        "type": "hive",
        "warehousePath": "hdfs://warehouse/path",
        "catalogUri": "thrift://hive-metastore.x.com:8970",
        "catalogProperties": {
          "hive.metastore.connect.retries": "1"
        }
      },
      "icebergFilter": {
        "type": "interval",
        "filterColumn": "event_time",
        "intervals": ["2023-05-10T19:00:00.000Z/2023-05-10T20:00:00.000Z"]
      },
      "warehouseSource": { "type": "hdfs" }
    },
    "inputFormat": { "type": "parquet" }
  }
}
```

#### Delta Lake 입력 소스

필요 익스텐션: `druid-deltalake-extensions`

Delta Lake 테이블 포맷의 데이터를 읽음. 최신 스냅샷 또는 지정한 버전을 스캔함.

- `type`: `delta`로 설정
  - 기본값: 없음 / 필수: 예
- `tablePath`: Delta 테이블의 위치
  - 기본값: 없음 / 필수: 예
- `filter`: 스냅샷 안에서 읽을 데이터 파일을 거르는 필터
  - 기본값: 없음 / 필수: 아니요
- `snapshotVersion`: 읽을 스냅샷의 정수 버전
  - 기본값: 최신 스냅샷 / 필수: 아니요

필터는 비교 연산자(`=`, `>`, `>=`, `<`, `<=`)와 논리 연산자(`and`, `or`, `not`)를 지원함.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "delta",
      "tablePath": "/delta-table/foo",
      "filter": {
        "type": "and",
        "filters": [
          { "type": "=", "column": "name", "value": "Employee4" },
          { "type": ">=", "column": "age", "value": "30" }
        ]
      },
      "snapshotVersion": 3
    }
  }
}
```

---

### Hadoop 기반 인제스천 (지원 종료)

Apache Hadoop 기반 인제스천 지원은 Apache Druid 37.0.0에서 제거됨. 공식 문서는 다음 대안을 권장함.

1. SQL 기반 인제스천 (SQL-based ingestion)
2. 네이티브 배치 인제스천 (native batch ingestion)

인제스천과 무관한 Hadoop 생태계 익스텐션, 예를 들어 딥 스토리지(deep storage)용 `druid-hdfs-storage` 등은 계속 지원됨.

Firehose 기반 인제스천 역시 Druid 26.0에서 제거되었으며, 기존 firehose 설정은 입력 소스 기반 인제스천으로 마이그레이션 필요.

---

### 참고 자료

- [JSON-based batch ingestion (native batch)](https://druid.apache.org/docs/latest/ingestion/native-batch)
- [Input sources](https://druid.apache.org/docs/latest/ingestion/input-sources)
- [Hadoop-based ingestion (removed)](https://druid.apache.org/docs/latest/ingestion/hadoop)
- [SQL-based ingestion](https://druid.apache.org/docs/latest/multi-stage-query/)
- [Ingestion overview](https://druid.apache.org/docs/latest/ingestion/)

---

## Druid SQL 기반 인제스천 (Multi-Stage Query)

> 원본: https://druid.apache.org/docs/latest/multi-stage-query/
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/concepts
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/reference
> 원본: https://druid.apache.org/docs/latest/multi-stage-query/known-issues

Multi-Stage Query(MSQ) 태스크 엔진으로 SQL `INSERT`/`REPLACE` 문을 배치 태스크로 실행하는 SQL 기반 인제스천의 개요, 핵심 개념, 문법 레퍼런스, 알려진 제약을 정리함.

---

### 목차

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

### 개요

Apache Druid는 Multi-Stage Query(MSQ) 태스크 엔진으로 SQL 기반 배치 인제스천을 지원함. SQL `INSERT`와 `REPLACE` 문을 배치 태스크로 실행하며, `SELECT` 쿼리 실행도 실험적(experimental)으로 지원함.

MSQ 태스크 엔진에서는 거의 모든 `SELECT` 기능을 사용할 수 있으므로, 인제스천 과정에서 변환·필터·JOIN·집계를 수행하거나 기존 테이블을 조회한 결과로 새 테이블을 만들 수 있음.

#### 주요 용어

- Controller: 쿼리당 하나 실행되는 `query_controller` 태스크. 쿼리 실행 전체를 관리
- Worker: 쿼리를 실제로 실행하는 `query_worker` 태스크. 여러 개를 병렬로 실행 가능
- Stage: 병렬화된 쿼리 실행 단계. 스테이지 사이에서 worker 간 데이터를 교환
- Partition: 각 스테이지 출력의 조각. 마지막 스테이지의 파티션이 Druid 세그먼트가 됨
- Shuffle: worker 간에 파티션 단위로 데이터를 교환하는 과정. 클러스터링 키 기준으로 정렬

#### 보안

`EXTERN`, `S3`, `LOCALFILES` 등의 테이블 함수를 사용하려면 `EXTERNAL` 리소스 타입에 대한 READ 권한 필요. 권한이 없으면 403 오류 발생.

---

### 핵심 개념

#### MSQ 태스크 엔진

MSQ 태스크 엔진은 SQL 문을 Middle Manager에서 배치 태스크로 실행함. `INSERT`/`REPLACE` 태스크는 다른 배치 인제스천과 동일한 방식으로 세그먼트를 발행(publish)함. 쿼리 하나당 최소 두 개의 태스크 슬롯(controller 1개 + worker 1개) 필요.

#### SQL 확장 요약

- EXTERN: 네이티브 배치 인제스천의 input source와 input format으로 외부 데이터를 읽음. 여러 파일은 worker 태스크에 나눠 병렬로 읽지만, 파일 하나를 여러 worker에 분할하지는 않음 → 큰 파일의 병렬성을 높이려면 입력 데이터를 미리 여러 파일로 쪼개는 것이 유리
- INSERT: 새 데이터소스를 만들거나 기존 데이터소스에 데이터를 추가함. 표준 SQL과 달리 테이블 생성과 데이터 추가 사이에 문법적 차이가 없음. 태스크 슬롯이 충분하면 같은 데이터소스에 여러 INSERT 문을 동시에 실행 가능. INSERT는 완료 시점에 새 세그먼트를 생성하므로 마이크로배치보다는 큰 배치 인제스천에 적합
- REPLACE: `OVERWRITE` 절로 데이터소스 전체 또는 특정 시간 범위의 데이터를 덮어씀. REPLACE 문은 대상 데이터소스의 대상 시간 범위에 배타적 쓰기 락(exclusive write lock)을 잡음. REPLACE로 생성한 세그먼트는 디멘션 기반 세그먼트 프루닝(pruning)을 지원하지만, INSERT로 생성한 세그먼트는 지원하지 않음

#### 기본 타임스탬프(__time)

Druid 테이블에는 항상 기본 타임스탬프 컬럼 `__time`이 존재함. 이 컬럼이 시간 기반 파티셔닝의 기준이 되며, 날짜/시간 함수로 값 설정 가능. `PARTITIONED BY ALL`을 사용하면 시간 파티셔닝이 비활성화되고 `__time`은 1970-01-01이 기본값이 됨.

---

### EXTERN 함수

#### 입력(입력 소스)으로 사용

두 가지 문법을 지원함.

방식 1: JSON 시그니처

```sql
SELECT <column>
FROM TABLE(
  EXTERN(
    '<Druid input source>',
    '<Druid input format>',
    '<row signature>'
  ))
```

방식 2: SQL EXTEND 절

```sql
SELECT <column>
FROM TABLE(
  EXTERN(
    inputSource => '<Druid input source>',
    inputFormat => '<Druid input format>'
  )) (<columns>)
```

row signature의 각 컬럼은 이름과 타입으로 구성하며, 타입으로 `string`, `long`, `double`, `float` 사용 가능.

#### 출력(내보내기) 대상으로 사용

`INSERT INTO EXTERN(...)`으로 쿼리 결과를 외부 저장소로 내보낼 수 있음.

```sql
SET rowsPerPage=<number_of_rows>;
INSERT INTO EXTERN(<destination function>)
AS CSV
SELECT <column>
FROM <table>
```

S3로 내보내기

```sql
INSERT INTO EXTERN(
  s3(bucket => 'your_bucket', prefix => 'prefix/to/files')
)
AS CSV
SELECT <column>
FROM <table>
```

GCS로 내보내기

```sql
INSERT INTO EXTERN(
  google(bucket => 'your_bucket', prefix => 'prefix/to/files')
)
AS CSV
SELECT <column>
FROM <table>
```

로컬 저장소로 내보내기

```sql
INSERT INTO EXTERN(
  local(exportPath => 'exportLocation/query1')
)
AS CSV
SELECT <column>
FROM <table>
```

내보내기 제약.

- `INSERT` 문만 지원
- 내보내기 형식은 CSV만 지원
- `PARTITIONED BY`, `CLUSTERED BY`는 지원하지 않음

내보내기 시 Druid는 `_symlink_format_manifest` 하위 디렉터리에 메타데이터 파일을 만들며, `_symlink_format_manifest/manifest` 디렉터리의 manifest 파일이 symlink manifest 형식으로 내보낸 파일들의 절대 경로를 나열함.

---

### INSERT 문

```sql
INSERT INTO <table name>
< SELECT query >
PARTITIONED BY <time frame>
[ CLUSTERED BY <column list> ]
```

표준 SQL과 달리, INSERT는 위치(position)가 아니라 컬럼 이름을 기준으로 대상 테이블에 데이터를 적재함.

---

### REPLACE 문

전체 덮어쓰기

```sql
REPLACE INTO <target table>
OVERWRITE ALL
< SELECT query >
PARTITIONED BY <time granularity>
[ CLUSTERED BY <column list> ]
```

특정 시간 범위 덮어쓰기

```sql
REPLACE INTO <target table>
OVERWRITE WHERE __time >= TIMESTAMP '<lower bound>'
  AND __time < TIMESTAMP '<upper bound>'
< SELECT query >
PARTITIONED BY <time granularity>
[ CLUSTERED BY <column list> ]
```

---

### PARTITIONED BY와 CLUSTERED BY

#### PARTITIONED BY

데이터는 `PARTITIONED BY` 세분성(granularity)이 정의하는 시간 청크(time chunk)마다 하나 이상의 세그먼트로 나뉨. `HOUR`, `DAY`가 흔히 쓰이며, 시간 범위 필터를 통한 쿼리 최적화와 세밀한 락킹에 유리함.

사용 가능한 세분성.

- 키워드: `HOUR`, `DAY`, `MONTH`, `YEAR`
- ISO 8601 기간 문자열: `PT1S`, `PT1M`, `PT5M`, `PT10M`, `PT15M`, `PT30M`, `PT1H`, `PT6H`, `P1D`, `P1W`, `P1M`, `P3M`, `P1Y`
- `ALL` 또는 `ALL TIME` (시간 파티셔닝 비활성화)
- 함수 형태: `TIME_FLOOR(__time, 'granularity_string')` 또는 `FLOOR(__time TO TimeUnit)`

#### CLUSTERED BY

```sql
CLUSTERED BY <column list>
```

시간 청크 내부에서 세그먼트를 2차 파티셔닝하고, 세그먼트 안의 행을 정렬함. `forceSegmentSortByTime`을 `false`로 설정하지 않는 한 시스템이 `CLUSTERED BY` 컬럼 목록 앞에 `__time`을 암묵적으로 붙임.

디멘션 기반 세그먼트 프루닝을 활성화하려면 다음 조건을 만족해야 함.

- 클러스터링이 단일 값(single-valued) string 컬럼으로 시작
- 세그먼트가 INSERT가 아닌 REPLACE 문으로 생성

---

### 롤업(Rollup)

롤업은 인제스천 시점에 데이터를 미리 집계함. 세 가지가 필요함.

1. `GROUP BY`를 사용. GROUP BY 컬럼은 디멘션이 되고, 집계 함수는 메트릭이 됨
2. 컨텍스트 파라미터 `finalizeAggregations: false` 설정
3. 지원되는 집계 함수 사용: `COUNT`(쿼리 시점에는 `SUM`으로 전환), `SUM`, `MIN`, `MAX`, `EARLIEST`/`EARLIEST_BY`, `LATEST`/`LATEST_BY`, 그리고 각종 근사 distinct count 및 sketch 함수

---

### 태스크 실행 구조

#### 실행 흐름

`/druid/v2/sql/task` API 엔드포인트로 쿼리를 제출하면 다음과 같이 진행됨.

1. Broker가 SQL을 네이티브 쿼리로 계획(plan)
2. Broker가 쿼리를 `query_controller` 태스크로 감쌈
3. Controller가 `maxNumTasks` 파라미터에 따라 worker 태스크를 기동
4. `query_worker` 타입의 worker 태스크들이 쿼리를 실행
5. 결과를 controller에 반환하거나(SELECT), 세그먼트를 발행(INSERT/REPLACE)

#### 병렬성

`maxNumTasks`는 controller를 포함한 전체 태스크 수를 결정하며, 최솟값은 2(worker 1개 + controller 1개). 클러스터의 가용 슬롯보다 크게 설정하면 `TaskStartTimeout` 오류 발생. worker 태스크는 단일 스레드로 동작함.

#### 메모리 사용

- JVM heap은 processor 번들과 worker 번들로 나뉘며 각각 37.5%를 차지
- 파티션 경계 결정용 sketch는 가용 메모리의 10% 또는 300MB 중 작은 값으로 제한
- Direct memory는 `(druid.processing.numThreads + 1) * druid.processing.buffer.sizeBytes` 이상의 버퍼 요구량을 수용하도록 설정 필요

#### 디스크 사용

worker는 다음 용도로 로컬 디스크를 사용함.

- 입력 파일의 임시 복사본
- 세그먼트 생성 중간 데이터
- 외부 정렬(external sort)
- 셔플 중 스테이지 출력 저장

태스크 작업 디렉터리(`druid.indexer.task.baseDir`)는 태스크당 전체 출력 데이터셋의 압축 복사본을 담을 수 있어야 함.

---

### 컨텍스트 파라미터

- `maxNumTasks` (SELECT, INSERT, REPLACE): controller 태스크를 포함해 기동할 최대 태스크 수. 최솟값은 2(controller 1 + worker 1)
  - 기본값: 2
- `taskAssignment` (SELECT, INSERT, REPLACE): 태스크 할당 방식. `max` 또는 `auto`. `auto`는 파일 크기를 파악할 수 있을 때 태스크당 512MiB, 10,000개 파일을 넘지 않는 선에서 최소한의 태스크를 사용
  - 기본값: `max`
- `finalizeAggregations` (SELECT, INSERT, REPLACE): 집계 결과를 최종(finalized) 타입으로 반환할지, 중간(intermediate) 타입으로 반환할지 결정
  - 기본값: `true`
- `arrayIngestMode` (INSERT, REPLACE): ARRAY 저장 방식. `array`(SQL 표준 호환) 또는 `mvd`(하위 호환, VARCHAR만 지원)
  - 기본값: `mvd`
- `sqlJoinAlgorithm` (SELECT, INSERT, REPLACE): 조인 알고리즘. `broadcast` 또는 `sortMerge`
  - 기본값: `broadcast`
- `maxRowsInMemory` (INSERT, REPLACE): 세그먼트 생성 중 디스크로 flush하기 전 메모리에 보관할 최대 행 수
  - 기본값: 100,000
- `rowsInMemory` (INSERT, REPLACE): `maxRowsInMemory`의 다른 표기
  - 기본값: 100,000
- `segmentSortOrder` (INSERT, REPLACE): 기본 세그먼트 정렬 순서 재정의. 쉼표 구분 또는 JSON 배열 형식
  - 기본값: 빈 목록
- `forceSegmentSortByTime` (INSERT, REPLACE): `true`(기본값)이면 `CLUSTERED BY` 앞에 `__time`을 붙임
  - 기본값: `true`
- `maxParseExceptions` (SELECT, INSERT, REPLACE): 쿼리가 `TooManyWarningsFault`로 중단되기 전까지 무시할 파싱 예외의 최대 개수. `-1`이면 모두 무시
  - 기본값: 0
- `rowsPerSegment` (INSERT, REPLACE): 세그먼트당 목표 행 수. 실제 행 수는 다소 많거나 적을 수 있음
  - 기본값: 3,000,000
- `indexSpec` (INSERT, REPLACE): 세그먼트 생성에 사용할 indexSpec. JSON 문자열 또는 객체
  - 기본값: indexSpec 기본값
- `durableShuffleStorage` (SELECT, INSERT, REPLACE): 셔플에 durable storage를 사용할지 여부. 서버 수준에서 `druid.msq.intermediate.storage.enable=true`로 durable storage를 설정해야 사용 가능
  - 기본값: `false`
- `faultTolerance` (SELECT, INSERT, REPLACE): worker 재시도를 포함한 내결함성 모드 활성화
  - 기본값: `false`
- `selectDestination` (SELECT): 결과 저장 위치. `taskReport` 또는 `durableStorage`
  - 기본값: `taskReport`
- `waitUntilSegmentsLoad` (INSERT, REPLACE): 설정하면 인제스천 쿼리가 생성된 세그먼트의 로드가 끝날 때까지 기다린 후 종료
  - 기본값: `false`
- `includeSegmentSource` (SELECT, INSERT, REPLACE): 쿼리 대상 범위. `NONE`(딥 스토리지(deep storage)만) 또는 `REALTIME`(realtime 태스크 포함)
  - 기본값: `NONE`
- `rowsPerPage` (SELECT): `selectDestination`이 `durableStorage`일 때 페이지당 목표 행 수
  - 기본값: 100,000
- `skipTypeVerification` (INSERT, REPLACE): 마이그레이션 시 string array와 multi-value 디멘션의 타입 검증 비활성화. 쉼표 구분 또는 JSON 배열
  - 기본값: 빈 목록
- `failOnEmptyInsert` (INSERT, REPLACE): `true`이면 출력 행이 0개일 때 오류를 던짐. `false`(기본값)이면 INSERT가 no-op이 됨
  - 기본값: `false`
- `storeCompactionState` (REPLACE): 컴팩션 상태 추적을 위한 세그먼트 메타데이터 저장
  - 기본값: `false`
- `removeNullBytes` (SELECT, INSERT, REPLACE): `true`이면 MSQ 엔진이 데이터를 읽을 때 string 필드의 null byte를 제거
  - 기본값: `false`
- `includeAllCounters` (SELECT, INSERT, REPLACE): Druid 31 이후 추가된 카운터를 포함할지 여부. 하위 호환용 옵션
  - 기본값: `true`
- `maxFrameSize` (SELECT, INSERT, REPLACE): MSQ 엔진 내부 데이터 전송에 사용하는 frame 크기
  - 기본값: 1,000,000 (1MB)
- `maxInputFilesPerWorker` (SELECT, INSERT, REPLACE): worker당 최대 입력 파일/세그먼트 수. 초과하면 `TooManyInputFiles`로 실패
  - 기본값: 10,000
- `maxPartitions` (SELECT, INSERT, REPLACE): 단일 스테이지의 최대 출력 파티션 수. INSERT/REPLACE에서는 세그먼트 생성을 제어
  - 기본값: 25,000
- `maxThreads` (SELECT, INSERT, REPLACE): 처리에 사용할 최대 스레드 수. 0보다 크고 기본 스레드 수보다 작을 때만 효과
  - 기본값: 미설정(기본값 사용)

---

### 조인(Join)

#### Broadcast 조인 (기본)

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
- 제약: base 입력이 아닌 모든 leaf 입력은 broadcast 테이블 크기 제한을 넘을 수 없음. 제한은 worker당 processor 메모리 번들의 30%

#### Sort-Merge 조인

```sql
SET sqlJoinAlgorithm='sortMerge';
SELECT eventstream.__time, eventstream.user_id, users.signup_date
FROM eventstream
LEFT JOIN users ON eventstream.user_id = users.id
PARTITIONED BY HOUR
CLUSTERED BY user
```

- 지원 타입: `LEFT`, `RIGHT`, `INNER`, `FULL`, `CROSS`
- 제약: 조인 중 버퍼링 가능한 데이터는 10MB. 조인 양쪽 모두 이 제한을 넘으면 `TooManyRowsWithSameKey` 오류가 발생하고, 한쪽만 넘으면 이 오류가 발생하지 않음

---

### Durable Storage 설정

#### 공통 속성

- `druid.msq.intermediate.storage.enable`: durable storage 활성화. `true`로 설정
  - 필수: 예 / 기본값: `false`
- `druid.msq.intermediate.storage.type`: 저장소 타입: `s3`, `azure`, `google`
  - 필수: 예 / 기본값: 없음
- `druid.msq.intermediate.storage.tempDir`: 임시 파일용 로컬 디스크 디렉터리. 설정하지 않으면 태스크 임시 디렉터리를 사용
  - 필수: 예 / 기본값: 없음
- `druid.msq.intermediate.storage.maxRetry`: API 호출 최대 재시도 횟수
  - 필수: 아니요 / 기본값: 10
- `druid.msq.intermediate.storage.chunkSize`: 임시 저장 청크 크기(5MiB~5GiB)
  - 필수: 아니요 / 기본값: 100MiB

#### S3 / Google 속성

- `druid.msq.intermediate.storage.bucket`: 업로드/다운로드에 사용할 S3 또는 Google 버킷
  - 필수: 예
- `druid.msq.intermediate.storage.prefix`: 버킷 경로 앞에 붙는 경로. 클러스터마다 고유해야 함
  - 필수: 예

#### Azure 속성

- `druid.msq.intermediate.storage.container`: 업로드/다운로드에 사용할 Azure 컨테이너
  - 필수: 예
- `druid.msq.intermediate.storage.prefix`: 컨테이너 경로 앞에 붙는 경로. 클러스터마다 고유해야 함
  - 필수: 예

#### Durable Storage Cleaner

- `druid.msq.intermediate.storage.cleaner.enabled`: 중간 파일 주기 정리 활성화
  - 필수: 아니요 / 기본값: `false`
- `druid.msq.intermediate.storage.cleaner.delaySeconds`: 태스크 완료 후 정리까지의 지연 시간(초)
  - 필수: 아니요 / 기본값: 86400

---

### 제한(Limits)과 오류 코드

#### 제한

- 개별 행 frame 크기: 1MB → 초과 시 `RowTooLarge`
- 세그먼트 단위 시간 청크 수: 5,000 → 초과 시 `TooManyBuckets`
- worker당 입력 파일/세그먼트 수: 10,000 → 초과 시 `TooManyInputFiles`
- 스테이지당 출력 파티션 수: 25,000 → 초과 시 `TooManyPartitions`
- 스테이지당 출력 컬럼 수: 2,000 → 초과 시 `TooManyColumns`
- 스테이지당 cluster-by 컬럼 수: 1,500 → 초과 시 `TooManyClusteredByColumns`
- 스테이지당 worker 수: 1,000(하드 제한), 메모리에 따른 소프트 제한 → 초과 시 `TooManyWorkers`
- broadcast 테이블 메모리: processor 메모리 번들의 30% → 초과 시 `BroadcastTablesTooLarge`
- sort-merge 조인 버퍼 데이터: 10MB → 초과 시 `TooManyRowsWithSameKey`
- 윈도우 파티션 내 행 수: 100,000 → 초과 시 `TooManyRowsInAWindow`
- worker 재기동 시도: 2회(최초 실행 제외) → 초과 시 `TooManyAttemptsForWorker`
- 전체 작업 재기동 시도: 100회 → 초과 시 `TooManyAttemptsForJob`

#### 주요 오류 코드

- `BroadcastTablesTooLarge`: broadcast 테이블이 메모리 제한 초과
  - 주요 필드: `maxBroadcastTablesSize`
- `CannotParseExternalData`: 외부 데이터 파싱 실패
  - 주요 필드: `errorMessage`
- `ColumnTypeNotSupported`: 지원하지 않는 컬럼 타입
  - 주요 필드: `columnName`, `columnType`
- `InsertCannotAllocateSegment`: 세그먼트 ID 할당 충돌
  - 주요 필드: `dataSource`, `interval`, `allocatedInterval`
- `InsertCannotBeEmpty`: `failOnEmptyInsert=true`인데 출력이 없음
  - 주요 필드: `dataSource`
- `InsertTimeNull`: `__time` 필드가 null
  - 주요 필드: (상황에 따라 다름)
- `InvalidNullByte`: string에 null byte 포함
  - 주요 필드: `source`, `rowNumber`, `column`, `value`, `position`
- `RowTooLarge`: 행이 frame 크기 제한 초과
  - 주요 필드: `maxFrameSize`
- `TaskStartTimeout`: 제한 시간 안에 worker 기동 실패
  - 주요 필드: `pendingTasks`, `totalTasks`, `timeout`
- `TooManyBuckets`: 시간 청크 5,000개 초과
  - 주요 필드: `maxBuckets`
- `TooManyInputFiles`: worker당 파일 수 제한 초과
  - 주요 필드: `numInputFiles`, `maxInputFiles`, `minNumWorker`
- `TooManyPartitions`: 파티션 25,000개 제한 초과
  - 주요 필드: `maxPartitions`
- `TooManyColumns`: 컬럼 2,000개 제한 초과
  - 주요 필드: `numColumns`, `maxColumns`
- `TooManyClusteredByColumns`: 클러스터링 컬럼 1,500개 제한 초과
  - 주요 필드: `numColumns`, `maxColumns`, `stage`
- `TooManyRowsWithSameKey`: sort-merge 조인 키 버퍼 초과
  - 주요 필드: `key`, `numBytes`, `maxBytes`
- `TooManyRowsInAWindow`: 윈도우 파티션 구체화(materialization) 초과
  - 주요 필드: `numRows`, `maxRows`
- `WorkerFailed`: worker의 예기치 않은 실패
  - 주요 필드: `errorMsg`, `workerTaskId`

---

### 알려진 제약

#### MSQ 태스크 런타임

- 내결함성이 부분적으로만 지원됨. worker는 예기치 않게 종료되면 다시 기동되지만, controller에는 이런 보호가 없음
- 스테이지 출력이 `druid.indexer.task.baseDir`에 저장되므로 디스크 공간이 고갈되면 `UnknownError`가 발생할 수 있음

#### SELECT

- `GROUPING SETS`는 구현되어 있지 않음. 이 기능을 사용하는 쿼리는 `QueryNotSupported` 오류 반환

#### INSERT / REPLACE

- `INSERT INTO tbl (a, b, c) SELECT ...` 같은 컬럼 목록 지정 문법은 지원하지 않음. 표준 SQL과 달리 위치가 아니라 컬럼 이름 기준으로 삽입
- `createBitmapIndex`, `multiValueHandling` 및 일부 `indexSpec` 속성 등 몇몇 인제스천 스펙 옵션은 사용 불가

#### EXTERN

- schemaless dimensions 기능은 사용 불가. 모든 컬럼과 타입을 명시해야 함
- 파일 매칭 대상이 많으면 controller의 메모리를 많이 사용할 수 있음
- EXTERN은 외부 파일만 읽음. Druid 데이터소스를 읽으려면 `FROM` 사용

#### WINDOW 함수

- 윈도우는 최대 100,000개 요소로 제한
- MSQ 엔진에서 `leafOperators`를 피하기 위해 윈도우 처리 뒤에 추가 scan 스테이지가 붙음

#### 자동 컴팩션(Automatic Compaction)

- `metricSpec` 필드는 일부 aggregator만 지원
- 파티셔닝은 dynamic과 range 기반만 지원. string 디멘션 파티셔닝은 가능하지만 multi-valued 디멘션은 제외
- `maxTotalRows` 설정은 지원하지 않으므로 대신 `maxRowsPerSegment` 사용
- 정렬은 `__time`이 첫 컬럼인 경우로 제한

---

## Druid 데이터 관리

> 원본: https://druid.apache.org/docs/latest/data-management/
> 원본: https://druid.apache.org/docs/latest/data-management/update
> 원본: https://druid.apache.org/docs/latest/data-management/delete
> 원본: https://druid.apache.org/docs/latest/data-management/schema-changes
> 원본: https://druid.apache.org/docs/latest/data-management/compaction
> 원본: https://druid.apache.org/docs/latest/data-management/automatic-compaction

Druid에서 기존 데이터를 업데이트·삭제하고, 스키마를 변경하고, 컴팩션(compaction)으로 세그먼트(segment)를 최적화하는 방법을 설명함.

---

### 목차

1. [개요](#개요)
2. [데이터 업데이트](#데이터-업데이트)
3. [데이터 삭제](#데이터-삭제)
4. [스키마 변경](#스키마-변경)
5. [컴팩션](#컴팩션)
6. [자동 컴팩션](#자동-컴팩션)

---

### 개요

Druid는 데이터를 시간 청크(time chunk) 단위로 파티셔닝하여 세그먼트라는 불변(immutable) 파일에 저장함. 이 구조가 모든 데이터 관리 작업의 기반이 되며, 데이터 관리는 크게 네 가지 작업으로 나뉨.

- 업데이트(Update): 기존 데이터를 새 데이터로 교체
- 삭제(Deletion): 더 이상 필요 없는 데이터를 제거
- 스키마 변경(Schema changes): 신규·기존 데이터의 스키마를 조정
- 컴팩션(Compaction): 기존 데이터를 재색인하여 저장 공간과 쿼리 성능을 최적화. 수동 컴팩션과 자동 컴팩션이 있음

---

### 데이터 업데이트

#### 덮어쓰기(Overwrite)

Druid는 데이터를 시간 청크 단위로 파티셔닝해 저장하므로, 시간 범위를 기준으로 기존 데이터를 덮어쓸 수 있음. 다음 배치 인제스천 방식으로 덮어쓰기를 수행함.

- 네이티브 배치(Native batch): `appendToExisting: false`로 설정하고, 덮어쓸 시간 범위를 `intervals`로 지정
- SQL: `REPLACE <table> OVERWRITE [ALL | WHERE ...]` 문을 사용. `OVERWRITE ALL`은 테이블 전체를, `OVERWRITE WHERE ...`는 조건에 해당하는 시간 범위만 교체

주의할 점.

- 같은 데이터소스(datasource)의 같은 시간 범위에 대해 인제스천과 덮어쓰기를 동시에 실행 불가. 다른 시간 범위에 대한 작업은 정상적으로 진행됨
- Druid는 기본 키(primary key) 기반의 단일 레코드 업데이트를 지원하지 않음

#### 재색인(Reindex)

재색인은 기존 데이터 자체를 새 데이터의 원본으로 사용하는 덮어쓰기. 스키마 변경, 데이터 재파티셔닝, 필터링, 데이터 보강(enrichment) 등에 활용함.

- 네이티브 배치: `druid` input source로 기존 데이터를 읽고, 필요하면 `transformSpec`으로 필터링·변환
- SQL: `REPLACE <table> OVERWRITE`와 `SELECT ... FROM <table>` 쿼리를 조합

Druid에는 `UPDATE`나 `ALTER TABLE` 문이 없으므로, 기존 데이터를 바꾸려면 이 방식으로 재작성 필요.

#### 롤업 데이터소스(Rolled-up datasources)

롤업(rollup)이 적용된 데이터소스는 세그먼트를 다시 쓰지 않고 추가(append)만으로도 효율적으로 업데이트 가능. 디멘션(dimension) 값이 동일한 행을 추가하면, 집계 연산자를 사용하는 쿼리가 쿼리 시점에 두 행을 자동으로 합침. 이후 컴팩션을 실행하면 백그라운드에서 동일 디멘션 행이 물리적으로 병합됨.

#### 룩업(Lookups)

디멘션 값이 자주 바뀌는 경우 룩업 사용을 검토함. 대표적인 사례는 Druid 세그먼트에 ID 디멘션을 저장해 두고, 이 ID를 주기적으로 갱신해야 하는 읽기 쉬운 이름 문자열로 매핑하는 경우. 룩업 테이블만 갱신하면 되므로 세그먼트를 재작성할 필요가 없음.

---

### 데이터 삭제

#### 시간 범위 데이터 수동 삭제

시간 범위 기준 삭제는 두 단계로 진행함.

1. 소프트 삭제(마크 unused): 삭제할 세그먼트를 먼저 unused로 표시. drop rule을 사용하거나, Coordinator API 또는 웹 콘솔에서 수동으로 표시. unused로 표시된 데이터는 쿼리 불가하지만, 세그먼트 파일은 딥 스토리지(deep storage)에, 세그먼트 레코드는 메타데이터 스토리지(metadata store)에 그대로 남음
2. 하드 삭제(`kill` 태스크): `kill` 태스크가 세그먼트 파일을 딥 스토리지에서 영구 삭제하고 메타데이터 스토리지의 레코드를 제거. 백업이 없으면 되돌릴 수 없음

자세한 절차는 공식 문서의 "Tutorial: Deleting data" 참고.

#### drop rule로 자동 삭제

Druid는 load rule과 drop rule을 지원함. load rule은 데이터를 보존할 시간 구간을, drop rule은 데이터를 버릴 시간 구간을 정의함. drop rule에 해당하는 데이터는 시간 범위를 수동으로 unused 처리했을 때와 같은 방식으로 unused로 표시됨. 이는 메타데이터만 변경하는 작업이므로, 딥 스토리지에서 영구 삭제하려면 별도로 `kill` 태스크 실행 필요.

#### 특정 레코드 삭제

특정 레코드를 삭제하려면 필터를 적용한 재색인을 사용함. 필터는 재색인 후 남길 데이터를 지정하므로, 삭제하려는 데이터의 반대 조건을 작성해야 함.

- 네이티브 배치: `druid` input source로 재색인하면서 `transformSpec`에 반전 필터를 지정. 예를 들어 `userName`이 `'bob'`인 레코드를 삭제하려면 `"type": "not"` 필터로 해당 조건을 감쌈
- SQL: `REPLACE` 문에 남길 데이터 조건을 지정. 예를 들어 `WHERE userName <> 'bob'`처럼 작성하면 `'bob'` 레코드가 제거됨

#### 전체 테이블 삭제

테이블 전체 삭제도 시간 범위 삭제와 동일한 절차를 따름. Coordinator API 또는 웹 콘솔에서 모든 세그먼트를 unused로 표시하고, 영구 삭제가 필요하면 `kill` 태스크 실행.

#### `kill` 태스크로 영구 삭제

`kill` 태스크의 스펙(spec).

```json
{
  "type": "kill",
  "id": <task_id>,
  "dataSource": <task_datasource>,
  "interval": <all_unused_segments_in_this_interval_will_die!>,
  "versions": <optional_list_of_segment_versions_to_delete_in_this_interval>,
  "context": <task_context>,
  "batchSize": <optional_batch_size>,
  "limit": <optional_maximum_number_of_segments_to_delete>,
  "maxUsedStatusLastUpdatedTime": <optional_maximum_timestamp_when_segments_were_marked_as_unused>
}
```

- `versions`: 지정한 interval 안에서 삭제할 세그먼트 버전 목록. 지정하지 않으면 모든 unused 버전을 삭제
  - 기본값: null (모든 버전)
- `batchSize`: 한 배치에서 삭제하는 최대 세그먼트 수. Overlord 리소스가 장시간 점유되는 것을 방지
  - 기본값: 100
- `limit`: 태스크가 삭제하는 세그먼트의 최대 개수
  - 기본값: null (제한 없음)
- `maxUsedStatusLastUpdatedTime`: 이 시각 이전에 unused로 표시된 세그먼트만 삭제 대상으로 삼음
  - 기본값: null (기준 없음)

경고: `kill` 태스크는 대상 세그먼트에 관한 모든 정보를 메타데이터 스토리지와 딥 스토리지에서 영구 제거함. 이 작업은 되돌릴 수 없음.

#### Coordinator duty로 자동 kill

Coordinator가 주기적으로 unused 세그먼트가 있는 구간을 식별하여 `kill` 태스크를 실행하도록 설정 가능. Coordinator의 데이터 관리 설정(configuration 문서의 관련 속성)으로 활성화함.

#### Overlord에서 자동 kill (Experimental)

Overlord에 내장된 방식으로 kill을 실행하는 실험적 기능. 다음 장점이 있음.

- REST API 호출이 줄어듦
- 삭제 대상 세그먼트를 즉시 kill
- Overlord 내부에서 동작하므로 별도 태스크 슬롯(task slot)을 사용하지 않음
- 더 빠르게 완료되며 필요한 설정이 적음

단, Overlord에서 세그먼트 메타데이터 캐싱(segment metadata caching)이 활성화되어 있어야 하며, Coordinator에서 unused 세그먼트 자동 kill이 이미 활성화된 경우에는 사용 금지.

---

### 스키마 변경

#### 신규 데이터의 스키마 변경

Druid는 기존 데이터의 스키마를 건드리지 않고도 새로 들어오는 데이터의 스키마를 바꿀 수 있음. 스트리밍 인제스천이라면 supervisor 스펙을 갱신하고, 배치 인제스천이라면 다음 인제스천 때 새 스키마를 제공하면 됨.

이런 유연성은 Druid의 세그먼트 구조에서 나옴. 각 세그먼트는 생성 시점에 자신의 스키마 사본을 함께 저장하며, 세그먼트마다 스키마가 달라도 Druid가 쿼리 실행 시점에 자동으로 조정(reconcile)함.

#### 기존 데이터의 스키마 변경

이미 인제스천된 데이터의 스키마를 바꾸려면(예: 컬럼 타입 변경, 컬럼 제거) 재색인을 사용해야 함. 데이터 업데이트와 동일한 방식. 재색인은 영향받는 모든 세그먼트를 다시 쓰는 작업이므로 시간이 오래 걸릴 수 있음.

---

### 컴팩션

컴팩션 태스크는 지정한 시간 구간의 기존 세그먼트들을 읽어 데이터를 하나의 새로운 "컴팩트된" 세그먼트 집합으로 결합함.

#### 컴팩션이 필요한 경우

다음 상황에서 세그먼트 최적화를 위한 컴팩션을 고려함.

- 스트리밍 인제스천에서 데이터가 시간순으로 도착하지 않아 작은 세그먼트가 많이 생기는 경우
- `appendToExisting`을 사용하는 배치 인제스천이 최적 크기가 아닌 세그먼트를 만드는 경우
- 병렬 배치 인덱싱(`index_parallel`)이 다수의 작은 세그먼트를 생성하는 경우
- 태스크 설정 오류로 지나치게 큰 세그먼트가 생성된 경우

컴팩션 중에 데이터를 함께 수정할 수도 있음.

- 데이터가 희소한(sparse) 구간의 세그먼트 granularity 확대
- 오래된 데이터의 query granularity 축소(coarsening)
- 디멘션 순서 변경
- 사용하지 않는 컬럼 제거
- 롤업 적용 방식 변경

#### 컴팩션 실행 방법

- 자동 컴팩션(권장): Coordinator가 segment search policy로 컴팩션이 필요한 세그먼트를 주기적으로 식별. 최신 데이터부터 오래된 데이터 순으로 처리
- 수동 컴팩션: 자동 컴팩션이 사용 가능한 태스크 슬롯 한계에 부딪히는 경우, 특정 시간 범위를 강제로 컴팩션하거나 시간순이 아닌 순서로 컴팩션해야 하는 경우에 사용. 수동 컴팩션 태스크의 상세 스펙은 공식 문서의 manual compaction 페이지 참고
- 캐스케이딩 재색인(cascading reindexing): 데이터 수명(age)에 따라 서로 다른 설정을 적용해야 하는 복잡한 수명주기 관리에는 캐스케이딩 재색인 사용. granularity 축소나 행 삭제까지 포함하는 고급 시나리오가 여기에 해당

#### segment granularity 처리

별도로 지정하지 않으면 Druid는 컴팩트된 세그먼트에 기존 granularity를 유지하려고 시도함. 겹치는 구간의 세그먼트들이 서로 다른 segment granularity를 가지면, Druid는 겹치는 구간의 시작과 끝을 찾아 가장 가까운 segment granularity 수준을 사용함.

#### query granularity 처리

기본적으로 Druid는 컴팩트된 세그먼트에 기존 query granularity를 유지함. 입력 세그먼트들의 query granularity가 서로 다르면 가장 세밀한(finest) 수준을 결과 세그먼트에 적용함.

주의: query granularity를 축소하면 세밀한 데이터가 사라짐. 이후 `kill` 태스크가 overshadowed 세그먼트를 제거하면 원래의 세밀한 데이터는 영구적으로 손실됨.

#### 디멘션 처리

입력 세그먼트들의 디멘션이 서로 다르면, 결과 세그먼트는 입력 세그먼트 전체의 디멘션을 모두 포함함. 디멘션 순서는 더 최근 세그먼트의 구조를 우선함.

#### 롤업

모든 입력 세그먼트에 `rollup`이 설정된 경우에만 Druid가 출력 세그먼트를 롤업함.

---

### 자동 컴팩션

자동 컴팩션은 Druid 데이터소스에서 데이터를 읽어 같은 데이터소스에 다시 쓰는 특수한 인제스천 태스크. Druid가 스스로 컴팩션 태스크를 발행·실행하여 세그먼트 크기를 최적화하고 쿼리 성능을 높임.

자동 컴팩션을 실행하는 방법은 두 가지.

- 컴팩션 슈퍼바이저(compaction supervisor, 권장): Overlord에서 supervisor로 실행. 반응성이 더 좋고, MSQ task engine을 사용할 수 있으며, supervisor 프레임워크로 관리가 간편
- Coordinator duty: Coordinator의 duty로 실행. 실행 주기는 `druid.coordinator.period.indexingPeriod`(기본값 30분)를 따름

#### 자동 컴팩션 설정 문법

```json
{
  "dataSource": "<task_datasource>",
  "ioConfig": "<IO config>",
  "dimensionsSpec": "<custom dimensionsSpec>",
  "transformSpec": "<custom transformSpec>",
  "metricsSpec": "<custom metricsSpec>",
  "tuningConfig": "<tuningConfig>",
  "granularitySpec": "<compaction task granularitySpec>",
  "skipOffsetFromLatest": "<time period>",
  "taskPriority": "<priority>",
  "taskContext": "<task context>"
}
```

자동 컴팩션 전용 속성.

- `skipOffsetFromLatest`: 최신 데이터로부터 이 기간만큼은 컴팩션하지 않도록 건너뛰는 시간 윈도우를 정의
- `taskPriority`: 컴팩션 태스크의 실행 우선순위를 지정
- `taskContext`: 태스크 수준 설정을 제공

일부 속성은 시스템이 자동으로 설정함. `type`은 `compact`로 지정되고, 태스크 `id`는 `auto` 접두어로 생성되며, `context`는 사용자가 제공한 설정을 바탕으로 구성됨.

#### 컴팩션 슈퍼바이저 사용

웹 콘솔로 제출: Supervisors 탭에서 제출 메뉴를 열고 다음 형태의 스펙을 입력함.

```json
{
  "type": "autocompact",
  "spec": {
    "dataSource": "YOUR_DATASOURCE",
    "tuningConfig": {...},
    "granularitySpec": {...},
    "engine": "native|msq",
    "..."
  }
}
```

API로 제출: supervisor API 엔드포인트에 POST 요청을 보냄.

```bash
curl --location --request POST 'http://localhost:8081/druid/indexer/v1/supervisor' \
--header 'Content-Type: application/json' \
--data-raw '{
  "type": "autocompact",
  "suspended": false,
  "spec": {
    "dataSource": "wikipedia",
    "tuningConfig": {...},
    "granularitySpec": {...},
    "engine": "native|msq"
  }
}'
```

#### MSQ task engine으로 자동 컴팩션

자동 컴팩션에 MSQ task engine을 사용하려면 다음 요건을 충족해야 함.

- Overlord에서 증분 세그먼트 메타데이터 캐싱(incremental segment metadata caching) 활성화
- 컴팩션 슈퍼바이저 사용
- dynamic config 또는 supervisor 스펙에서 `engine`을 `msq`로 설정
- 컴팩션 태스크 슬롯을 최소 2개 확보하거나 `spec.taskContext.maxNumTasks`를 2 이상으로 설정

MSQ 엔진 사용 시 제약 사항.

- `metricsSpec`은 일부 aggregator만 지원
- 파티셔닝은 dynamic과 range 방식만 사용 가능
- `rollup`은 `metricsSpec`이 비어 있지 않은 경우에만 `true`로 설정
- 파티셔닝은 문자열 디멘션만 가능(다중값 문자열 제외)
- `maxTotalRows`를 지원하지 않으므로 `maxRowsPerSegment` 사용
- 세그먼트는 `__time`을 첫 번째 컬럼으로 하는 정렬만 지원

지원되는 aggregator는 병합 가능(mergeable, 부분 집계를 결합할 수 있음)하고 멱등(idempotent, 반복 실행해도 결과가 같음)해야 함. 입력 컬럼과 출력 컬럼이 같은 `longSum`이 대표적인 예.

#### Coordinator duty 기반 자동 컴팩션

웹 콘솔 설정: Datasources에서 Compaction 컬럼의 편집 아이콘을 클릭하고, 대화 상자의 폼 또는 JSON 뷰로 설정을 구성한 뒤 제출함.

API로 활성화

```bash
curl --location --request POST \
'http://localhost:8081/druid/coordinator/v1/config/compaction' \
--header 'Content-Type: application/json' \
--data-raw '{
  "dataSource": "wikipedia",
  "granularitySpec": {
    "segmentGranularity": "DAY"
  }
}'
```

API로 비활성화

```bash
curl --location --request DELETE \
'http://localhost:8081/druid/coordinator/v1/config/compaction/wikipedia'
```

실행 주기 조정: `coordinator/runtime.properties`에 별도의 duty group을 만들어 컴팩션 주기를 따로 지정 가능.

```
druid.coordinator.dutyGroups=["compaction"]
druid.coordinator.compaction.duties=["compactSegments"]
druid.coordinator.compaction.period=PT60S
```

#### 인제스천과의 충돌 방지

- 동시 append와 replace: `taskContext`에 `"useConcurrentLocks": true`를 설정하면 인제스천이 진행 중인 동안에도 안전하게 데이터 교체 가능
- 최신 세그먼트 건너뛰기: `skipOffsetFromLatest`로 최근 세그먼트를 컴팩션 대상에서 제외. 늦게 도착하는 데이터를 받는 스트리밍 시나리오라면 `skipOffsetFromLatest`를 몇 시간에서 하루 정도로 설정하는 것이 합리적

#### 예시

segment granularity 변경: 시간 단위 세그먼트를 일 단위로 컴팩션하되, 최근 1주 데이터는 건너뜀.

```json
{
  "dataSource": "wikistream",
  "granularitySpec": {
    "segmentGranularity": "DAY"
  },
  "skipOffsetFromLatest": "P1W"
}
```

파티셔닝 방식 최적화: 다중 디멘션 range 파티셔닝을 적용함.

```json
{
  "dataSource": "wikipedia",
  "tuningConfig": {
    "partitionsSpec": {
      "type": "range",
      "partitionDimensions": [
        "channel",
        "countryName",
        "namespace"
      ],
      "targetRowsPerSegment": 5000000
    }
  }
}
```

#### 제약 사항

segment granularity가 `ALL`인 데이터소스는 자동 컴팩션 대상에서 제외됨. granularity 축소나 행 삭제를 포함하는 고급 수명주기 관리는 캐스케이딩 재색인 문서 참고.
