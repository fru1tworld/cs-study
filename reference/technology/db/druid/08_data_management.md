# Druid 데이터 관리

> 원본: https://druid.apache.org/docs/latest/data-management/
> 원본: https://druid.apache.org/docs/latest/data-management/update
> 원본: https://druid.apache.org/docs/latest/data-management/delete
> 원본: https://druid.apache.org/docs/latest/data-management/schema-changes
> 원본: https://druid.apache.org/docs/latest/data-management/compaction
> 원본: https://druid.apache.org/docs/latest/data-management/automatic-compaction

Druid에서 기존 데이터를 업데이트·삭제하고, 스키마를 변경하고, 컴팩션(compaction)으로 세그먼트(segment)를 최적화하는 방법을 설명합니다.

---

## 목차

1. [개요](#개요)
2. [데이터 업데이트](#데이터-업데이트)
3. [데이터 삭제](#데이터-삭제)
4. [스키마 변경](#스키마-변경)
5. [컴팩션](#컴팩션)
6. [자동 컴팩션](#자동-컴팩션)

---

## 개요

Druid는 데이터를 시간 청크(time chunk) 단위로 파티셔닝하여 세그먼트라는 불변(immutable) 파일에 저장합니다. 이 구조가 모든 데이터 관리 작업의 기반이 되며, 데이터 관리는 크게 네 가지 작업으로 나뉩니다.

| 작업 | 설명 |
|------|------|
| 업데이트(Update) | 기존 데이터를 새 데이터로 교체합니다 |
| 삭제(Deletion) | 더 이상 필요 없는 데이터를 제거합니다 |
| 스키마 변경(Schema changes) | 신규·기존 데이터의 스키마를 조정합니다 |
| 컴팩션(Compaction) | 기존 데이터를 재색인하여 저장 공간과 쿼리 성능을 최적화합니다. 수동 컴팩션과 자동 컴팩션이 있습니다 |

---

## 데이터 업데이트

### 덮어쓰기(Overwrite)

Druid는 데이터를 시간 청크 단위로 파티셔닝해 저장하므로, 시간 범위를 기준으로 기존 데이터를 덮어쓸 수 있습니다. 다음 배치 인제스천 방식으로 덮어쓰기를 수행합니다.

- **네이티브 배치(Native batch)**: `appendToExisting: false`로 설정하고, 덮어쓸 시간 범위를 `intervals`로 지정합니다.
- **SQL**: `REPLACE <table> OVERWRITE [ALL | WHERE ...]` 문을 사용합니다. `OVERWRITE ALL`은 테이블 전체를, `OVERWRITE WHERE ...`는 조건에 해당하는 시간 범위만 교체합니다.

주의할 점은 다음과 같습니다.

- 같은 데이터소스(datasource)의 같은 시간 범위에 대해 인제스천과 덮어쓰기를 동시에 실행할 수 없습니다. 다른 시간 범위에 대한 작업은 정상적으로 진행됩니다.
- Druid는 기본 키(primary key) 기반의 단일 레코드 업데이트를 지원하지 않습니다.

### 재색인(Reindex)

재색인은 기존 데이터 자체를 새 데이터의 원본으로 사용하는 덮어쓰기입니다. 스키마 변경, 데이터 재파티셔닝, 필터링, 데이터 보강(enrichment) 등에 활용합니다.

- **네이티브 배치**: `druid` input source로 기존 데이터를 읽고, 필요하면 `transformSpec`으로 필터링·변환합니다.
- **SQL**: `REPLACE <table> OVERWRITE`와 `SELECT ... FROM <table>` 쿼리를 조합합니다.

Druid에는 `UPDATE`나 `ALTER TABLE` 문이 없으므로, 기존 데이터를 바꾸려면 이 방식으로 재작성해야 합니다.

### 롤업 데이터소스(Rolled-up datasources)

롤업(rollup)이 적용된 데이터소스는 세그먼트를 다시 쓰지 않고 추가(append)만으로도 효율적으로 업데이트할 수 있습니다. 디멘션(dimension) 값이 동일한 행을 추가하면, 집계 연산자를 사용하는 쿼리가 쿼리 시점에 두 행을 자동으로 합칩니다. 이후 컴팩션을 실행하면 백그라운드에서 동일 디멘션 행이 물리적으로 병합됩니다.

### 룩업(Lookups)

디멘션 값이 자주 바뀌는 경우 룩업 사용을 검토합니다. 대표적인 사례는 Druid 세그먼트에 ID 디멘션을 저장해 두고, 이 ID를, 주기적으로 갱신해야 하는 읽기 쉬운 이름 문자열로 매핑하는 경우입니다. 룩업 테이블만 갱신하면 되므로 세그먼트를 재작성할 필요가 없습니다.

---

## 데이터 삭제

### 시간 범위 데이터 수동 삭제

시간 범위 기준 삭제는 두 단계로 진행합니다.

1. **소프트 삭제(마크 unused)**: 삭제할 세그먼트를 먼저 unused로 표시합니다. drop rule을 사용하거나, Coordinator API 또는 웹 콘솔에서 수동으로 표시합니다. unused로 표시된 데이터는 쿼리할 수 없지만, 세그먼트 파일은 딥 스토리지(deep storage)에, 세그먼트 레코드는 메타데이터 스토리지(metadata store)에 그대로 남습니다.
2. **하드 삭제(`kill` 태스크)**: `kill` 태스크가 세그먼트 파일을 딥 스토리지에서 영구 삭제하고 메타데이터 스토리지의 레코드를 제거합니다. 백업이 없으면 되돌릴 수 없습니다.

자세한 절차는 공식 문서의 "Tutorial: Deleting data"를 참고합니다.

### drop rule로 자동 삭제

Druid는 load rule과 drop rule을 지원합니다. load rule은 데이터를 보존할 시간 구간을, drop rule은 데이터를 버릴 시간 구간을 정의합니다. drop rule에 해당하는 데이터는 시간 범위를 수동으로 unused 처리했을 때와 같은 방식으로 unused로 표시됩니다. 이는 메타데이터만 변경하는 작업이므로, 딥 스토리지에서 영구 삭제하려면 별도로 `kill` 태스크를 실행해야 합니다.

### 특정 레코드 삭제

특정 레코드를 삭제하려면 필터를 적용한 재색인을 사용합니다. 필터는 재색인 후 **남길** 데이터를 지정하므로, 삭제하려는 데이터의 반대 조건을 작성해야 합니다.

- **네이티브 배치**: `druid` input source로 재색인하면서 `transformSpec`에 반전 필터를 지정합니다. 예를 들어 `userName`이 `'bob'`인 레코드를 삭제하려면 `"type": "not"` 필터로 해당 조건을 감쌉니다.
- **SQL**: `REPLACE` 문에 남길 데이터 조건을 지정합니다. 예를 들어 `WHERE userName <> 'bob'`처럼 작성하면 `'bob'` 레코드가 제거됩니다.

### 전체 테이블 삭제

테이블 전체 삭제도 시간 범위 삭제와 동일한 절차를 따릅니다. Coordinator API 또는 웹 콘솔에서 모든 세그먼트를 unused로 표시하고, 영구 삭제가 필요하면 `kill` 태스크를 실행합니다.

### `kill` 태스크로 영구 삭제

`kill` 태스크의 스펙(spec)은 다음과 같습니다.

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

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| `versions` | null (모든 버전) | 지정한 interval 안에서 삭제할 세그먼트 버전 목록. 지정하지 않으면 모든 unused 버전을 삭제합니다 |
| `batchSize` | 100 | 한 배치에서 삭제하는 최대 세그먼트 수. Overlord 리소스가 장시간 점유되는 것을 방지합니다 |
| `limit` | null (제한 없음) | 태스크가 삭제하는 세그먼트의 최대 개수 |
| `maxUsedStatusLastUpdatedTime` | null (기준 없음) | 이 시각 이전에 unused로 표시된 세그먼트만 삭제 대상으로 삼습니다 |

> **경고**: `kill` 태스크는 대상 세그먼트에 관한 모든 정보를 메타데이터 스토리지와 딥 스토리지에서 영구 제거합니다. 이 작업은 되돌릴 수 없습니다.

### Coordinator duty로 자동 kill

Coordinator가 주기적으로 unused 세그먼트가 있는 구간을 식별하여 `kill` 태스크를 실행하도록 설정할 수 있습니다. Coordinator의 데이터 관리 설정(configuration 문서의 관련 속성)으로 활성화합니다.

### Overlord에서 자동 kill (Experimental)

Overlord에 내장된 방식으로 kill을 실행하는 실험적 기능입니다. 다음 장점이 있습니다.

- REST API 호출이 줄어듭니다
- 삭제 대상 세그먼트를 즉시 kill합니다
- Overlord 내부에서 동작하므로 별도 태스크 슬롯(task slot)을 사용하지 않습니다
- 더 빠르게 완료되며 필요한 설정이 적습니다

단, Overlord에서 세그먼트 메타데이터 캐싱(segment metadata caching)이 활성화되어 있어야 하며, Coordinator에서 unused 세그먼트 자동 kill이 이미 활성화된 경우에는 사용하면 안 됩니다.

---

## 스키마 변경

### 신규 데이터의 스키마 변경

Druid는 기존 데이터의 스키마를 건드리지 않고도 새로 들어오는 데이터의 스키마를 바꿀 수 있습니다. 스트리밍 인제스천이라면 supervisor 스펙을 갱신하고, 배치 인제스천이라면 다음 인제스천 때 새 스키마를 제공하면 됩니다.

이런 유연성은 Druid의 세그먼트 구조에서 나옵니다. 각 세그먼트는 생성 시점에 자신의 스키마 사본을 함께 저장하며, 세그먼트마다 스키마가 달라도 Druid가 쿼리 실행 시점에 자동으로 조정(reconcile)합니다.

### 기존 데이터의 스키마 변경

이미 인제스천된 데이터의 스키마를 바꾸려면(예: 컬럼 타입 변경, 컬럼 제거) 재색인을 사용해야 합니다. 데이터 업데이트와 동일한 방식입니다. 재색인은 영향받는 모든 세그먼트를 다시 쓰는 작업이므로 시간이 오래 걸릴 수 있습니다.

---

## 컴팩션

컴팩션 태스크는 지정한 시간 구간의 기존 세그먼트들을 읽어 데이터를 하나의 새로운 "컴팩트된" 세그먼트 집합으로 결합합니다.

### 컴팩션이 필요한 경우

다음 상황에서 세그먼트 최적화를 위한 컴팩션을 고려합니다.

- 스트리밍 인제스천에서 데이터가 시간순으로 도착하지 않아 작은 세그먼트가 많이 생기는 경우
- `appendToExisting`을 사용하는 배치 인제스천이 최적 크기가 아닌 세그먼트를 만드는 경우
- 병렬 배치 인덱싱(`index_parallel`)이 다수의 작은 세그먼트를 생성하는 경우
- 태스크 설정 오류로 지나치게 큰 세그먼트가 생성된 경우

컴팩션 중에 데이터를 함께 수정할 수도 있습니다.

- 데이터가 희소한(sparse) 구간의 세그먼트 granularity 확대
- 오래된 데이터의 query granularity 축소(coarsening)
- 디멘션 순서 변경
- 사용하지 않는 컬럼 제거
- 롤업 적용 방식 변경

### 컴팩션 실행 방법

- **자동 컴팩션(권장)**: Coordinator가 segment search policy로 컴팩션이 필요한 세그먼트를 주기적으로 식별합니다. 최신 데이터부터 오래된 데이터 순으로 처리합니다.
- **수동 컴팩션**: 자동 컴팩션이 사용 가능한 태스크 슬롯 한계에 부딪히는 경우, 특정 시간 범위를 강제로 컴팩션하거나 시간순이 아닌 순서로 컴팩션해야 하는 경우에 사용합니다. 수동 컴팩션 태스크의 상세 스펙은 공식 문서의 manual compaction 페이지를 참고합니다.
- **캐스케이딩 재색인(cascading reindexing)**: 데이터 수명(age)에 따라 서로 다른 설정을 적용해야 하는 복잡한 수명주기 관리에는 캐스케이딩 재색인을 사용합니다. granularity 축소나 행 삭제까지 포함하는 고급 시나리오가 여기에 해당합니다.

### segment granularity 처리

별도로 지정하지 않으면 Druid는 컴팩트된 세그먼트에 기존 granularity를 유지하려고 시도합니다. 겹치는 구간의 세그먼트들이 서로 다른 segment granularity를 가지면, Druid는 겹치는 구간의 시작과 끝을 찾아 가장 가까운 segment granularity 수준을 사용합니다.

### query granularity 처리

기본적으로 Druid는 컴팩트된 세그먼트에 기존 query granularity를 유지합니다. 입력 세그먼트들의 query granularity가 서로 다르면 가장 세밀한(finest) 수준을 결과 세그먼트에 적용합니다.

> **주의**: query granularity를 축소하면 세밀한 데이터가 사라집니다. 이후 `kill` 태스크가 overshadowed 세그먼트를 제거하면 원래의 세밀한 데이터는 영구적으로 손실됩니다.

### 디멘션 처리

입력 세그먼트들의 디멘션이 서로 다르면, 결과 세그먼트는 입력 세그먼트 전체의 디멘션을 모두 포함합니다. 디멘션 순서는 더 최근 세그먼트의 구조를 우선합니다.

### 롤업

모든 입력 세그먼트에 `rollup`이 설정된 경우에만 Druid가 출력 세그먼트를 롤업합니다.

---

## 자동 컴팩션

자동 컴팩션은 Druid 데이터소스에서 데이터를 읽어 같은 데이터소스에 다시 쓰는 특수한 인제스천 태스크입니다. Druid가 스스로 컴팩션 태스크를 발행·실행하여 세그먼트 크기를 최적화하고 쿼리 성능을 높입니다.

자동 컴팩션을 실행하는 방법은 두 가지입니다.

- **컴팩션 슈퍼바이저(compaction supervisor, 권장)**: Overlord에서 supervisor로 실행합니다. 반응성이 더 좋고, MSQ task engine을 사용할 수 있으며, supervisor 프레임워크로 관리가 간편합니다.
- **Coordinator duty**: Coordinator의 duty로 실행합니다. 실행 주기는 `druid.coordinator.period.indexingPeriod`(기본값 30분)를 따릅니다.

### 자동 컴팩션 설정 문법

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

자동 컴팩션 전용 속성은 다음과 같습니다.

| 속성 | 설명 |
|------|------|
| `skipOffsetFromLatest` | 최신 데이터로부터 이 기간만큼은 컴팩션하지 않도록 건너뛰는 시간 윈도우를 정의합니다 |
| `taskPriority` | 컴팩션 태스크의 실행 우선순위를 지정합니다 |
| `taskContext` | 태스크 수준 설정을 제공합니다 |

일부 속성은 시스템이 자동으로 설정합니다. `type`은 `compact`로 지정되고, 태스크 `id`는 `auto` 접두어로 생성되며, `context`는 사용자가 제공한 설정을 바탕으로 구성됩니다.

### 컴팩션 슈퍼바이저 사용

**웹 콘솔로 제출**: Supervisors 탭에서 제출 메뉴를 열고 다음 형태의 스펙을 입력합니다.

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

**API로 제출**: supervisor API 엔드포인트에 POST 요청을 보냅니다.

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

### MSQ task engine으로 자동 컴팩션

자동 컴팩션에 MSQ task engine을 사용하려면 다음 요건을 충족해야 합니다.

- Overlord에서 증분 세그먼트 메타데이터 캐싱(incremental segment metadata caching)을 활성화합니다
- 컴팩션 슈퍼바이저를 사용합니다
- dynamic config 또는 supervisor 스펙에서 `engine`을 `msq`로 설정합니다
- 컴팩션 태스크 슬롯을 최소 2개 확보하거나 `spec.taskContext.maxNumTasks`를 2 이상으로 설정합니다

MSQ 엔진 사용 시 제약 사항은 다음과 같습니다.

- `metricsSpec`은 일부 aggregator만 지원합니다
- 파티셔닝은 dynamic과 range 방식만 사용할 수 있습니다
- `rollup`은 `metricsSpec`이 비어 있지 않은 경우에만 `true`로 설정합니다
- 파티셔닝은 문자열 디멘션만 가능합니다(다중값 문자열 제외)
- `maxTotalRows`를 지원하지 않으므로 `maxRowsPerSegment`를 사용합니다
- 세그먼트는 `__time`을 첫 번째 컬럼으로 하는 정렬만 지원합니다

지원되는 aggregator는 병합 가능(mergeable, 부분 집계를 결합할 수 있음)하고 멱등(idempotent, 반복 실행해도 결과가 같음)해야 합니다. 입력 컬럼과 출력 컬럼이 같은 `longSum`이 대표적인 예입니다.

### Coordinator duty 기반 자동 컴팩션

**웹 콘솔 설정**: Datasources에서 Compaction 컬럼의 편집 아이콘을 클릭하고, 대화 상자의 폼 또는 JSON 뷰로 설정을 구성한 뒤 제출합니다.

**API로 활성화**:

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

**API로 비활성화**:

```bash
curl --location --request DELETE \
'http://localhost:8081/druid/coordinator/v1/config/compaction/wikipedia'
```

**실행 주기 조정**: `coordinator/runtime.properties`에 별도의 duty group을 만들어 컴팩션 주기를 따로 지정할 수 있습니다.

```
druid.coordinator.dutyGroups=["compaction"]
druid.coordinator.compaction.duties=["compactSegments"]
druid.coordinator.compaction.period=PT60S
```

### 인제스천과의 충돌 방지

- **동시 append와 replace**: `taskContext`에 `"useConcurrentLocks": true`를 설정하면 인제스천이 진행 중인 동안에도 안전하게 데이터를 교체할 수 있습니다.
- **최신 세그먼트 건너뛰기**: `skipOffsetFromLatest`로 최근 세그먼트를 컴팩션 대상에서 제외합니다. 늦게 도착하는 데이터를 받는 스트리밍 시나리오라면 `skipOffsetFromLatest`를 몇 시간에서 하루 정도로 설정하는 것이 합리적입니다.

### 예시

**segment granularity 변경**: 시간 단위 세그먼트를 일 단위로 컴팩션하되, 최근 1주 데이터는 건너뜁니다.

```json
{
  "dataSource": "wikistream",
  "granularitySpec": {
    "segmentGranularity": "DAY"
  },
  "skipOffsetFromLatest": "P1W"
}
```

**파티셔닝 방식 최적화**: 다중 디멘션 range 파티셔닝을 적용합니다.

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

### 제약 사항

segment granularity가 `ALL`인 데이터소스는 자동 컴팩션 대상에서 제외됩니다. granularity 축소나 행 삭제를 포함하는 고급 수명주기 관리는 캐스케이딩 재색인 문서를 참고합니다.
