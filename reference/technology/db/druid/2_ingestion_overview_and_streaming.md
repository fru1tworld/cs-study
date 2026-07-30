# Druid 인제스천 개요와 스트리밍 인제스천

## Druid 인제스천(Ingestion) 개요

> 원본: https://druid.apache.org/docs/latest/ingestion/
> 원본: https://druid.apache.org/docs/latest/ingestion/schema-model
> 원본: https://druid.apache.org/docs/latest/ingestion/rollup
> 원본: https://druid.apache.org/docs/latest/ingestion/partitioning
> 원본: https://druid.apache.org/docs/latest/ingestion/data-formats
> 원본: https://druid.apache.org/docs/latest/ingestion/ingestion-spec

Druid에 데이터를 적재하는 두 가지 방식(스트리밍/배치), 스키마 모델, 롤업(rollup), 파티셔닝(partitioning), 지원 데이터 포맷, 인제스천 스펙(ingestion spec)의 구조를 차례로 설명합니다.

---

### 목차

1. [인제스천 개요](#인제스천-개요)
2. [스트리밍 인제스천](#스트리밍-인제스천)
3. [배치 인제스천](#배치-인제스천)
4. [스키마 모델](#스키마-모델)
5. [롤업](#롤업)
6. [파티셔닝](#파티셔닝)
7. [데이터 포맷](#데이터-포맷)
8. [인제스천 스펙](#인제스천-스펙)
9. [참고 자료](#참고-자료)

---

### 인제스천 개요

Druid에서 데이터를 적재하는 작업을 **인제스천(ingestion)** 또는 **인덱싱**(indexing)이라고 부릅니다. 인제스천은 소스 시스템에서 데이터를 읽어 **세그먼트**(segment)라는 파일로 저장하는 과정입니다. 세그먼트 하나에는 일반적으로 수백만 개의 행이 들어갑니다.

세그먼트는 딥 스토리지(deep storage)에 저장되며, Historical 프로세스가 이를 가져와 쿼리에 응답합니다.

인제스천 방식은 크게 두 갈래입니다.

- **스트리밍(streaming)**: Kafka, Kinesis 같은 스트림 소스에서 실시간으로 적재합니다.
- **배치(batch)**: 파일 등 정적 소스에서 일괄 적재합니다.

---

### 스트리밍 인제스천

스트리밍 인제스천은 계속 실행되는 **슈퍼바이저**(supervisor)가 제어합니다. 슈퍼바이저는 실시간 데이터 흐름을 관리하는 인제스천 태스크들을 감독합니다.

| 항목 | Kafka | Kinesis |
| --- | --- | --- |
| 슈퍼바이저 타입 | `kafka` | `kinesis` |
| 동작 방식 | Druid가 Apache Kafka에서 직접 읽음 | Druid가 Amazon Kinesis에서 직접 읽음 |
| 늦게 도착한 데이터(late data) 인제스천 | 지원 | 지원 |
| exactly-once 보장 | 지원 | 지원 |

---

### 배치 인제스천

배치 인제스천 작업은 작업이 진행되는 동안 실행되는 **컨트롤러 태스크**(controller task)와 연결됩니다. 네이티브 배치와 SQL 기반 배치 두 가지가 있으며, 둘 다 외부 의존성 없이 동작합니다.

| 항목 | 네이티브 배치(Native batch) | SQL 기반(SQL-based) |
| --- | --- | --- |
| 컨트롤러 태스크 타입 | `index_parallel` | `query_controller` |
| 제출 방법 | 스펙(spec)을 Tasks API로 전송 | `INSERT`/`REPLACE` 문을 SQL task API로 전송 |
| 병렬성 | `maxNumConcurrentSubTasks`가 1보다 크면 서브태스크로 병렬 실행 | `query_worker` 서브태스크 사용 |
| 내결함성(fault tolerance) | 워커는 재시작됨. 컨트롤러가 실패하면 작업 실패 | 컨트롤러 또는 워커 중 하나라도 실패하면 작업 실패 |
| 추가(append) | 지원 | 지원 (`INSERT`) |
| 덮어쓰기(overwrite) | 지원 | 지원 (`REPLACE`) |
| 외부 의존성 | 없음 | 없음 |
| 입력 소스 | 모든 `inputSource` | 모든 `inputSource` 또는 Druid 데이터소스 |
| 입력 포맷 | 모든 `inputFormat` | 모든 `inputFormat` |
| 2차 파티셔닝 | dynamic, hash, range 방식 지원 | `CLUSTERED BY`를 통한 range 파티셔닝 |
| 롤업 | `forceGuaranteedRollup`을 켜면 perfect rollup | 항상 perfect rollup |

---

### 스키마 모델

Druid는 관계형 데이터베이스의 테이블과 유사한 **데이터소스**(datasource)에 데이터를 저장합니다. Druid의 데이터 모델은 관계형 모델과 시계열(timeseries) 모델의 요소를 함께 갖추고 있습니다.

#### 기본 타임스탬프(Primary timestamp)

Druid의 스키마에는 기본 타임스탬프가 반드시 있어야 합니다. 기본 타임스탬프는 다음과 같은 역할을 합니다.

- **데이터 구성**: Druid는 기본 타임스탬프를 기준으로 데이터를 파티셔닝하고 정렬합니다.
- **쿼리 성능**: 시간 범위에 해당하는 데이터를 빠르게 찾아 조회할 수 있습니다.
- **데이터 관리**: 시간 청크(time chunk) 삭제, 특정 기간 덮어쓰기, 보존 정책(retention) 적용 같은 작업의 기준이 됩니다.

소스의 어떤 필드에서 읽어오든 Druid는 타임스탬프를 항상 데이터소스의 **`__time`** 컬럼에 저장합니다. 타임스탬프는 인제스천 시점에 `timestampSpec`으로 설정하며, `granularitySpec`에서 추가 제어가 가능합니다.

#### 디멘션(Dimensions)

디멘션은 값을 변경하지 않고 그대로 저장하는 컬럼입니다.

- 쿼리 시점에 그룹핑, 필터링, 집계에 사용할 수 있습니다.
- 롤업을 끄면 디멘션은 일반 데이터베이스의 컬럼처럼 동작합니다.
- 인제스천 시 `dimensionsSpec`으로 설정합니다.

#### 메트릭(Metrics)

메트릭은 저장 시점에 집계 형태로 저장하는 컬럼입니다.

- 메트릭은 롤업을 켰을 때 가장 유용합니다.
- 인제스천 시점에 집계 함수를 미리 계산해 둘 수 있습니다.
- 특히 근사 집계자(approximate aggregator)를 쓸 때 쿼리 시점 계산이 빨라집니다.
- 인제스천 시 `metricsSpec`으로 설정합니다.

---

### 롤업

**롤업**(rollup)은 인제스천 시점에 수행하는 사전 집계(pre-aggregation)입니다. `queryGranularity`로 타임스탬프를 잘라낸(truncate) 뒤, 디멘션 값과 타임스탬프 값이 동일한 행들을 하나의 행으로 합칩니다. 저장 데이터 크기와 행 수를 크게 줄일 수 있지만, 개별 이벤트 단위 조회는 불가능해집니다.

롤업은 `granularitySpec`에서 **기본으로 켜져 있습니다**. 원본 레코드를 집계 없이 그대로 보존하려면 `rollup`을 `false`로 설정합니다.

#### 롤업 사용 기준

**롤업을 켜는 경우**

- 성능 최적화가 최우선이거나 저장 공간 제약이 엄격할 때
- 고카디널리티(high-cardinality) 디멘션의 원본 값이 필요 없을 때

**롤업을 끄는 경우**

- 개별 행 단위의 결과가 필요할 때
- 임의의 컬럼에 대해 `GROUP BY`나 `WHERE` 쿼리를 실행해야 할 때

#### 롤업 비율(rollup ratio) 극대화

롤업 효과는 적재된 이벤트 수와 Druid에 저장된 행 수를 비교해 측정합니다.

```sql
SELECT SUM("num_rows") / (COUNT(*) * 1.0) FROM datasource
```

롤업 비율을 높이는 전략은 다음과 같습니다.

1. **스키마 설계**: 디멘션 개수를 줄이고 카디널리티가 낮은 디멘션을 사용합니다.
2. **스케치(sketch) 활용**: 롤업 비율을 떨어뜨리는 고카디널리티 디멘션을 직접 저장하는 대신 스케치를 사용합니다.
3. **queryGranularity 조정**: 예를 들어 `PT1M` 대신 `PT5M`을 사용하면 같은 타임스탬프로 합쳐지는 행이 늘어납니다.
4. **데이터소스 이원화**: 전체 데이터소스와 축약 데이터소스를 함께 운영하여 쿼리 패턴별로 나눠 사용합니다.
5. **재처리**: best-effort 롤업으로 적재한 데이터를 이후에 재색인(reindex)하거나 컴팩션(compaction)하여 롤업을 개선합니다.

#### Perfect rollup과 best-effort rollup

- **Perfect rollup**: 인제스천 시점에 완전한 집계를 보장합니다. 전체 데이터를 먼저 스캔하는 전처리 단계가 필요합니다.
- **Best-effort rollup**: 여러 세그먼트에 걸쳐 완전히 집계되지 않은 행이 남을 수 있습니다. 원인은 다음과 같습니다.
  - 병렬 인제스천이 perfect rollup에 필요한 셔플(shuffle) 단계를 생략하는 경우
  - 증분 발행(incremental publishing)이 해당 시간 청크의 모든 데이터를 받기 전에 세그먼트를 확정하는 경우

| 인제스천 방식 | 롤업 보장 |
| --- | --- |
| 네이티브 배치 (`index_parallel`, `index`) | perfect 또는 best-effort (설정 가능) |
| SQL 기반 배치 | 항상 perfect |
| Kafka indexing service | 항상 best-effort |
| Kinesis indexing service | 항상 best-effort |

스트리밍 인제스천은 증분 발행 방식으로 동작하므로 항상 best-effort 롤업입니다.

---

### 파티셔닝

Druid는 데이터 크기를 줄이고 쿼리 성능을 높이기 위한 파티셔닝과 정렬 기능을 제공합니다. 데이터를 여러 데이터소스로 분리할 수도 있고, 하나의 데이터소스 안에서 파티셔닝할 수도 있습니다.

#### 시간 청크 파티셔닝(Time chunk partitioning)

모든 인제스천 방식에서 데이터소스는 인제스천 스펙의 `dataSchema`에 있는 `segmentGranularity` 값에 따라 시간 청크 단위로 나뉘고, 각 시간 청크 안에 세그먼트가 만들어집니다.

주요 이점은 다음과 같습니다.

- `__time`(SQL) 또는 `intervals`(네이티브 쿼리)로 필터링하는 쿼리가 스캔 대상 세그먼트를 줄일 수 있습니다(pruning).
- 덮어쓰기, 컴팩션 같은 데이터 관리 작업이 시간 파티션 단위로 배타적 쓰기 잠금(exclusive write lock)을 획득할 수 있습니다.
- 각 세그먼트 파일은 하나의 시간 파티션 안에 완전히 포함됩니다.

일반적으로 `hour`와 `day`를 많이 사용합니다. 스트리밍 인제스천에서는 인제스천 후 컴팩션을 빠르게 수행할 수 있도록 `hour`를 선호합니다.

| 방식 | 설정 위치 |
| --- | --- |
| SQL | `PARTITIONED BY` 절 |
| Kafka/Kinesis | `granularitySpec`의 `segmentGranularity` |
| 네이티브 배치 | `granularitySpec`의 `segmentGranularity` |

#### 2차 파티셔닝(Secondary partitioning)

2차 파티셔닝은 시간 청크를 특정 디멘션 기준으로 다시 나누어 불변(immutable) 세그먼트를 만듭니다. 필터로 자주 사용하는 자연스러운 디멘션이나, 데이터가 어느 정도 정렬되어 들어오는 디멘션을 파티션 기준으로 선택하는 것이 좋습니다.

| 방식 | 설정 위치 |
| --- | --- |
| SQL | `CLUSTERED BY` 절 |
| Kafka/Kinesis | 업스트림 파티셔닝을 따름. 이후 `CLUSTERED BY`가 있는 `REPLACE`나 컴팩션으로 변경 가능 |
| 네이티브 배치 | `tuningConfig`의 `partitionsSpec` |

#### 정렬(Sorting)

각 세그먼트 내부의 행은 정렬되어 저장되며, 이는 압축률과 지역성(locality)을 높입니다. 파티셔닝과 정렬을 함께 사용하면 — 특히 파티셔닝 디멘션을 정렬 순서의 맨 앞에 두면 — 압축과 성능이 눈에 띄게 좋아지는 경우가 많습니다.

| 방식 | 설정 위치 |
| --- | --- |
| SQL | `CLUSTERED BY`의 필드 순서 또는 `segmentSortOrder` 컨텍스트 파라미터 |
| Kafka/Kinesis | `dimensionsSpec`의 필드 순서 |
| 네이티브 배치 | `dimensionsSpec`의 필드 순서 |

기본적으로 Druid는 세그먼트 내부 행을 항상 `__time` 기준으로 먼저 정렬합니다. `forceSegmentSortByTime`을 `false`로 설정하면 이 동작을 끌 수 있는데, 이는 Druid 31 이상이 필요한 실험적(experimental) 기능이며 일부 네이티브 쿼리와 SQL 쿼리에 제약이 생깁니다.

---

### 데이터 포맷

Druid가 기본 지원하는 입력 데이터 포맷은 다음과 같습니다.

- **텍스트 포맷**: JSON, CSV, TSV(또는 커스텀 구분자), Lines(행 단위 텍스트)
- **바이너리 포맷**: ORC(`druid-orc-extensions` 필요), Parquet(`druid-parquet-extensions` 필요), Avro Stream/Avro OCF(`druid-avro-extensions` 필요), Protobuf(`druid-protobuf-extensions` 필요)
- **스트리밍 메타데이터 포맷**: Kafka, Kinesis

입력 포맷은 `ioConfig`의 `inputFormat` 필드로 지정합니다. 정규식 기반 파싱보다는 네이티브 Java `InputFormat` 익스텐션을 사용하는 편이 효율적입니다.

#### JSON

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `json`으로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 JSON 평탄화 설정 | 아니요 |
| `featureSpec` | JSON Object | Jackson JSON 파서 기능 설정 | 아니요 |
| `assumeNewlineDelimited` | Boolean | 개행 구분(newline-delimited) JSON에 대한 예외 처리를 유연하게 함 | 아니요 |
| `useJsonNodeReader` | Boolean | 파싱 예외 발생 전까지의 유효한 JSON 이벤트를 보존 | 아니요 |

```json
"ioConfig": {
  "inputFormat": {
    "type": "json"
  }
}
```

#### CSV

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `csv`로 지정 | 예 |
| `columns` | JSON array | 데이터 순서대로 나열한 컬럼 이름 | `findColumnsFromHeader`가 false면 필수 |
| `listDelimiter` | String | 다중값 디멘션(multi-value dimension) 구분자 | 아니요 |
| `findColumnsFromHeader` | Boolean | 헤더 행에서 컬럼 이름 추출 | 아니요 |
| `skipHeaderRows` | Integer | 앞에서부터 N개 행 건너뛰기 | 아니요 |
| `tryParseNumbers` | Boolean | 숫자 문자열을 long/double로 파싱 | 아니요 |

```json
"ioConfig": {
  "inputFormat": {
    "type": "csv",
    "columns": ["timestamp", "page", "language", "user",
                "unpatrolled", "newPage", "robot", "anonymous",
                "namespace", "continent", "country", "region",
                "city", "added", "deleted", "delta"]
  }
}
```

#### TSV (Delimited)

CSV와 같은 필드 구성에 더해 `delimiter`(기본값 `\t`)로 필드 구분자를 지정할 수 있습니다.

```json
"ioConfig": {
  "inputFormat": {
    "type": "tsv",
    "columns": ["timestamp", "page", "language", "user",
                "unpatrolled", "newPage", "robot", "anonymous",
                "namespace", "continent", "country", "region",
                "city", "added", "deleted", "delta"],
    "delimiter": "|"
  }
}
```

CSV/TSV 데이터 샘플에는 컬럼 헤더가 없을 수 있으므로 `columns`를 명시하거나 `findColumnsFromHeader`를 사용해야 합니다.

#### Lines

각 행을 UTF-8 텍스트로 읽어 `line`이라는 단일 컬럼에 담습니다.

```json
"ioConfig": {
  "inputFormat": {
    "type": "lines"
  }
}
```

#### ORC

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `orc`로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 데이터 평탄화 (`path` 표현식만 지원) | 아니요 |
| `binaryAsString` | Boolean | 바이너리 컬럼을 UTF-8 문자열로 처리 | 아니요 |

```json
"ioConfig": {
  "inputFormat": {
    "type": "orc",
    "flattenSpec": {
      "useFieldDiscovery": true,
      "fields": [
        {
          "type": "path",
          "name": "nested",
          "expr": "$.path.to.nested"
        }
      ]
    },
    "binaryAsString": false
  }
}
```

#### Parquet

ORC와 동일한 필드 구성입니다 (`type`을 `parquet`으로 지정).

```json
"ioConfig": {
  "inputFormat": {
    "type": "parquet",
    "flattenSpec": {
      "useFieldDiscovery": true,
      "fields": [
        {
          "type": "path",
          "name": "nested",
          "expr": "$.path.to.nested"
        }
      ]
    },
    "binaryAsString": false
  }
}
```

#### Avro Stream

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `avro_stream`으로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 값 추출 (`path` 표현식만 지원) | 아니요 |
| `avroBytesDecoder` | JSON Object | Avro 바이트 디코딩 방식 지정 | 예 |
| `binaryAsString` | Boolean | 바이트를 UTF-8 문자열로 처리 | 아니요 |

`avroBytesDecoder` 타입은 네 가지입니다.

- `schema_inline`: 고정 스키마 (스키마 마이그레이션 미지원)
- `multiple_schemas_inline`: ID로 구분하는 다중 스키마
- `schema_repo`: 스키마 저장소(schema repo) 조회
- `schema_registry`: Confluent Schema Registry 조회

```json
"ioConfig": {
  "inputFormat": {
    "type": "avro_stream",
    "avroBytesDecoder": {
      "type": "schema_inline",
      "schema": {
        "namespace": "org.apache.druid.data",
        "name": "User",
        "type": "record",
        "fields": [
          { "name": "FullName", "type": "string" },
          { "name": "Country", "type": "string" }
        ]
      }
    },
    "flattenSpec": {
      "useFieldDiscovery": true,
      "fields": [
        {
          "type": "path",
          "name": "someRecord_subInt",
          "expr": "$.someRecord.subInt"
        }
      ]
    }
  }
}
```

Confluent Schema Registry를 사용하는 예시는 다음과 같습니다.

```json
"avroBytesDecoder": {
  "type": "schema_registry",
  "urls": ["http://schema-registry:8081"],
  "config": {
    "basic.auth.credentials.source": "USER_INFO",
    "basic.auth.user.info": "user:password"
  }
}
```

#### Avro OCF

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `avro_ocf`로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 값 추출 (`path`만 지원) | 아니요 |
| `schema` | JSON Object | 디코딩에 사용할 reader 스키마 | 아니요 |
| `binaryAsString` | Boolean | 바이트를 UTF-8 문자열로 처리 | 아니요 |

#### Protobuf

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `protobuf`로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 값 추출 (`path`만 지원) | 아니요 |
| `protoBytesDecoder` | JSON Object | Protobuf 디코딩 방식 지정 | 예 |

```json
"ioConfig": {
  "inputFormat": {
    "type": "protobuf",
    "protoBytesDecoder": {
      "type": "file",
      "descriptor": "file:///tmp/metrics.desc",
      "protoMessageType": "Metrics"
    },
    "flattenSpec": {
      "useFieldDiscovery": true,
      "fields": [
        {
          "type": "path",
          "name": "someRecord_subInt",
          "expr": "$.someRecord.subInt"
        }
      ]
    }
  }
}
```

#### Kafka

Kafka `inputFormat`은 페이로드 파서를 감싸서 Kafka 메시지의 메타데이터(타임스탬프, 토픽, 헤더, 키)까지 컬럼으로 가져옵니다.

| 필드 | 타입 | 설명 | 필수 | 기본값 |
| --- | --- | --- | --- | --- |
| `type` | String | `kafka`로 지정 | 예 | — |
| `valueFormat` | InputFormat | Kafka 페이로드를 파싱할 포맷 | 예 | — |
| `timestampColumnName` | String | Kafka 타임스탬프를 담을 컬럼 | 아니요 | `kafka.timestamp` |
| `topicColumnName` | String | Kafka 토픽을 담을 컬럼 | 아니요 | `kafka.topic` |
| `headerColumnPrefix` | String | 헤더 컬럼의 접두사 | 아니요 | `kafka.header` |
| `headerFormat` | Object | 헤더 인코딩 (UTF-8, ISO-8859-1, US-ASCII, UTF-16 등) | 아니요 | — |
| `keyFormat` | InputFormat | Kafka 키를 파싱할 포맷 | 아니요 | — |
| `keyColumnName` | String | Kafka 키를 담을 컬럼 | 아니요 | `kafka.key` |

```json
"ioConfig": {
  "inputFormat": {
    "type": "kafka",
    "valueFormat": {
      "type": "json"
    },
    "timestampColumnName": "kafka.timestamp",
    "topicColumnName": "kafka.topic",
    "headerFormat": {
      "type": "string",
      "encoding": "UTF-8"
    },
    "keyFormat": {
      "type": "tsv",
      "findColumnsFromHeader": false,
      "columns": ["x"]
    },
    "keyColumnName": "kafka.key"
  }
}
```

#### Kinesis

| 필드 | 타입 | 설명 | 필수 | 기본값 |
| --- | --- | --- | --- | --- |
| `type` | String | `kinesis`로 지정 | 예 | — |
| `valueFormat` | InputFormat | Kinesis 페이로드를 파싱할 포맷 | 예 | — |
| `partitionKeyColumnName` | String | 파티션 키를 담을 컬럼 | 아니요 | `kinesis.partitionKey` |
| `timestampColumnName` | String | Kinesis 타임스탬프를 담을 컬럼 | 아니요 | `kinesis.timestamp` |

```json
"ioConfig": {
  "inputFormat": {
    "type": "kinesis",
    "valueFormat": {
      "type": "json"
    },
    "timestampColumnName": "kinesis.timestamp",
    "partitionKeyColumnName": "kinesis.partitionKey"
  }
}
```

#### flattenSpec

`flattenSpec`은 네이티브 중첩 컬럼(nested column)의 대안으로, 중첩된 입력 데이터를 평탄한(flat) Druid 데이터 모델로 변환합니다. 중첩을 지원하는 포맷(JSON, Avro, ORC, Parquet)에만 적용되며, `timestampSpec`, `transformSpec`, `dimensionsSpec`, `metricsSpec`보다 먼저 적용됩니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `useFieldDiscovery` | 루트 수준 필드 자동 탐색 | `true` |
| `fields` | 명시적으로 지정한 필드와 접근 방식 | `[]` |

```json
"flattenSpec": {
  "useFieldDiscovery": true,
  "fields": [
    { "name": "baz", "type": "root" },
    { "name": "foo_bar", "type": "path", "expr": "$.foo.bar" },
    { "name": "foo_other_bar", "type": "tree", "nodes": ["foo", "other", "bar"] },
    { "name": "first_food", "type": "jq", "expr": ".thing.food[1]" }
  ]
}
```

필드 타입별 지원 포맷은 다음과 같습니다.

| 타입 | 설명 | 지원 포맷 |
| --- | --- | --- |
| `root` | 루트 수준 필드 참조 | JSON, ORC, Parquet, Avro |
| `path` | JsonPath 표기 (예: `$.foo.bar`) | JSON, ORC, Parquet, Avro |
| `jq` | jackson-jq 표기 (jq의 부분 집합) | JSON 전용 |
| `tree` | 계층적 필드 이름 목록 | JSON 전용 |

JsonPath 함수로 `min()`, `max()`, `avg()`, `stddev()`, `length()`, `sum()`, `concat(X)`, `append(X)`, `keys()`(JSON 전용) 등을 사용할 수 있습니다.

---

### 인제스천 스펙

모든 인제스천 방식은 인제스천 태스크를 통해 Druid에 데이터를 적재하며, 태스크는 인제스천 스펙으로 정의합니다. 스펙은 세 가지 주요 섹션으로 구성됩니다.

- **`dataSchema`**: 데이터소스 이름, 기본 타임스탬프, 디멘션, 메트릭, 변환·필터 설정
- **`ioConfig`**: 데이터를 읽어올 소스와 파싱 방법. 인제스천 방식별로 세부 내용이 다릅니다.
- **`tuningConfig`**: 인제스천 방식별 성능 튜닝 파라미터

#### 전체 예시 (`index_parallel`)

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia",
      "timestampSpec": {
        "column": "timestamp",
        "format": "auto"
      },
      "dimensionsSpec": {
        "dimensions": [
          "page",
          "language",
          { "type": "long", "name": "userId" }
        ]
      },
      "metricsSpec": [
        { "type": "count", "name": "count" },
        { "type": "doubleSum", "name": "bytes_added_sum", "fieldName": "bytes_added" },
        { "type": "doubleSum", "name": "bytes_deleted_sum", "fieldName": "bytes_deleted" }
      ],
      "granularitySpec": {
        "segmentGranularity": "day",
        "queryGranularity": "none",
        "intervals": [
          "2013-08-31/2013-09-01"
        ]
      }
    },
    "ioConfig": {
      "type": "index_parallel",
      "inputSource": {
        "type": "local",
        "baseDir": "examples/indexing/",
        "filter": "wikipedia_data.json"
      },
      "inputFormat": {
        "type": "json",
        "flattenSpec": {
          "useFieldDiscovery": true,
          "fields": [
            { "type": "path", "name": "userId", "expr": "$.user.id" }
          ]
        }
      }
    },
    "tuningConfig": {
      "type": "index_parallel"
    }
  }
}
```

#### dataSchema

##### dataSource

데이터를 기록할 대상 데이터소스의 이름입니다.

```json
"dataSource": "my-first-datasource"
```

##### timestampSpec

기본 타임스탬프 컬럼을 설정합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `column` | 기본 타임스탬프로 사용할 입력 필드 | `timestamp` |
| `format` | 타임스탬프 파싱 형식: `iso`, `posix`, `millis`, `micro`, `nano`, `auto`, 또는 Joda DateTimeFormat 패턴 | `auto` |
| `missingValue` | 타임스탬프가 null이거나 없을 때 사용할 ISO8601 값 | none |

Druid는 기본 타임스탬프를 반드시 요구하므로, 레코드별 타임스탬프가 없는 데이터셋을 적재할 때는 `missingValue`가 유용합니다.

##### dimensionsSpec

디멘션을 설정합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `dimensions` | 디멘션 이름 또는 객체의 목록 | `[]` |
| `dimensionExclusions` | 제외할 이름 목록 | `[]` |
| `spatialDimensions` | 공간(spatial) 디멘션 배열 | `[]` |
| `includeAllDimensions` | 명시한 디멘션과 자동 탐색된 디멘션을 함께 포함 | `false` |
| `useSchemaDiscovery` | 자동 스키마 탐색으로 타입 추론 | `false` |
| `forceSegmentSortByTime` | 시간 기준 우선 정렬 | `true` |

디멘션 객체는 다음 속성을 지원합니다.

- `type`: `auto`, `string`, `long`, `float`, `double`, `json`
- `name`: 디멘션 이름 (필수)
- `createBitmapIndex`: string 타입에서 비트맵 인덱스 생성 여부
- `multiValueHandling`: `array`, `sorted_array`, `sorted_set`
- `maxStringLength`: 값의 최대 문자 수

##### metricsSpec

인제스천 시점에 적용할 집계자(aggregator) 목록입니다. 롤업을 켰을 때 인제스천 시점 집계를 설정하는 수단이므로 가장 유용합니다. `count`, `doubleSum`, `longSum`, `cardinality` 같은 집계자를 사용할 수 있습니다.

##### granularitySpec

시간 청크와 롤업을 제어합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `type` | 항상 `uniform` | `uniform` |
| `segmentGranularity` | 데이터소스의 시간 청크 단위 (`day`, `month` 등) | `day` |
| `queryGranularity` | 세그먼트 안에 저장하는 타임스탬프 해상도. `segmentGranularity`와 같거나 더 세밀해야 함 | `none` |
| `rollup` | 인제스천 시점 롤업 적용 여부 | `true` |
| `intervals` | 배치 인제스천에서 세그먼트를 만들 ISO8601 시간 범위 목록 | `null` |

##### transformSpec

인제스천 시점에 표현식(expression) 변환과 필터를 적용합니다.

**변환(transform)** 은 표현식 기반으로 필드를 만들거나 바꿉니다.

```json
{
  "type": "expression",
  "name": "<output name>",
  "expression": "<expr>"
}
```

변환에는 제약이 있습니다. 실제 입력 행에 존재하는 필드만 참조할 수 있으며, 다른 변환 결과를 참조할 수 없습니다.

**필터(filter)** 는 표준 Druid 쿼리 필터를 사용해 적재할 행을 선별합니다.

##### Projections

`projectionsSpec`으로 사전 집계 구조인 프로젝션(projection)을 정의할 수 있습니다. 정의한 프로젝션은 해당 데이터소스의 디멘션이 됩니다. 주요 속성은 다음과 같습니다.

- `name`: 프로젝션 이름
- `dimensions`: 집계 대상 디멘션의 부분 집합
- `granularity`: 집계 시간 단위
- `metrics`: 집계자 목록

#### ioConfig

데이터를 읽어올 입력 소스와의 연결과 파싱을 제어합니다. `inputFormat`은 여러 방식에서 공통이며, 나머지 속성은 인제스천 방식별로 다릅니다.

```json
"ioConfig": {
  "type": "<ingestion-method-specific type code>",
  "inputFormat": { "type": "json" }
}
```

#### tuningConfig

성능 관련 파라미터를 설정합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `type` | 인제스천 방식별 타입 코드 | 필수 |
| `maxRowsInMemory` | 디스크로 persist하기 전 메모리에 유지할 최대 행 수 | 1,000,000 |
| `maxBytesInMemory` | persist를 유발하는 JVM 힙 사용량 임계값(바이트) | 최대 힙의 1/6 |
| `skipBytesInMemoryOverheadCheck` | 메모리 계산에서 오버헤드 제외 여부 | `false` |
| `indexSpec` | 세그먼트 저장 형식 옵션 | 아래 참조 |
| `indexSpecForIntermediatePersists` | 중간 persist 세그먼트의 저장 형식 | `indexSpec`과 동일 |

##### indexSpec

| 필드 | 옵션 | 기본값 |
| --- | --- | --- |
| `bitmap` | `roaring`, `concise` | `roaring` |
| `dimensionCompression` | `lz4`, `lzf`, `zstd`, `uncompressed` | `lz4` |
| `metricCompression` | `lz4`, `lzf`, `zstd`, `uncompressed`, `none` | `lz4` |
| `longEncoding` | `auto`, `longs` | `longs` |
| `stringDictionaryEncoding` | `utf8`, `frontCoded` | `utf8` |
| `complexMetricCompression` | `lz4`, `lzf`, `zstd`, `uncompressed` | `uncompressed` |
| `jsonCompression` | `lz4`, `lzf`, `zstd`, `uncompressed` | `lz4` |

**Front coding**은 STRING 및 `COMPLEX<json>` 컬럼을 성능 저하를 거의 일으키지 않으면서 추가로 압축하는 선택적 증분 인코딩(incremental encoding) 전략입니다.

```json
"stringDictionaryEncoding": {
  "type": "frontCoded",
  "bucketSize": 4,
  "formatVersion": 1
}
```

#### 처리 순서

Druid는 인제스천 스펙의 구성 요소를 정해진 순서로 적용합니다. 먼저 `flattenSpec`(있는 경우), 그다음 `timestampSpec`, 그다음 `transformSpec`, 마지막으로 `dimensionsSpec`과 `metricsSpec` 순입니다.

---

### 참고 자료

- [Ingestion overview](https://druid.apache.org/docs/latest/ingestion/)
- [Druid schema model](https://druid.apache.org/docs/latest/ingestion/schema-model)
- [Data rollup](https://druid.apache.org/docs/latest/ingestion/rollup)
- [Partitioning](https://druid.apache.org/docs/latest/ingestion/partitioning)
- [Source input formats](https://druid.apache.org/docs/latest/ingestion/data-formats)
- [Ingestion spec reference](https://druid.apache.org/docs/latest/ingestion/ingestion-spec)

---

## Druid 스트리밍 인제스천

> 원본: https://druid.apache.org/docs/latest/ingestion/streaming
> 원본: https://druid.apache.org/docs/latest/ingestion/supervisor
> 원본: https://druid.apache.org/docs/latest/ingestion/kafka-ingestion
> 원본: https://druid.apache.org/docs/latest/ingestion/kinesis-ingestion
> 원본: https://druid.apache.org/docs/latest/ingestion/tasks

Apache Kafka와 Amazon Kinesis에서 실시간으로 데이터를 받아들이는 스트리밍 인제스천(streaming ingestion)의 개요, 인제스천을 총괄하는 Supervisor, Kafka/Kinesis별 설정, 그리고 인제스천을 실제로 수행하는 태스크(task)를 차례로 설명합니다.

---

### 목차

1. [스트리밍 인제스천 개요](#스트리밍-인제스천-개요)
2. [Supervisor](#supervisor)
   - [Supervisor spec 구조](#supervisor-spec-구조)
   - [ioConfig 공통 속성](#ioconfig-공통-속성)
   - [태스크 오토스케일러](#태스크-오토스케일러)
   - [tuningConfig 공통 속성](#tuningconfig-공통-속성)
   - [Supervisor 상태](#supervisor-상태)
   - [Supervisor 관리 작업](#supervisor-관리-작업)
   - [용량 계획](#용량-계획)
3. [Kafka 인제스천](#kafka-인제스천)
   - [Kafka supervisor spec 예시](#kafka-supervisor-spec-예시)
   - [Kafka ioConfig](#kafka-ioconfig)
   - [다중 토픽 인제스천](#다중-토픽-인제스천)
   - [Idle 설정](#idle-설정)
   - [Kafka inputFormat으로 메타데이터 읽기](#kafka-inputformat으로-메타데이터-읽기)
   - [Kafka 배포 시 유의 사항](#kafka-배포-시-유의-사항)
4. [Kinesis 인제스천](#kinesis-인제스천)
   - [Kinesis supervisor spec 예시](#kinesis-supervisor-spec-예시)
   - [Kinesis ioConfig](#kinesis-ioconfig)
   - [Kinesis tuningConfig](#kinesis-tuningconfig)
   - [AWS 인증](#aws-인증)
   - [Fetch 설정과 Kinesis 처리량 한계](#fetch-설정과-kinesis-처리량-한계)
   - [디애그리게이션](#디애그리게이션)
   - [리샤딩](#리샤딩)
   - [알려진 이슈](#알려진-이슈)
5. [태스크](#태스크)
   - [태스크 유형](#태스크-유형)
   - [태스크 API와 상태](#태스크-api와-상태)
   - [태스크 리포트](#태스크-리포트)
   - [태스크 락](#태스크-락)
   - [태스크 액션](#태스크-액션)
   - [컨텍스트 파라미터](#컨텍스트-파라미터)
   - [태스크 로그와 스토리지](#태스크-로그와-스토리지)
6. [참고 자료](#참고-자료)

---

### 스트리밍 인제스천 개요

Druid는 두 가지 외부 스트리밍 소스에서 데이터를 받아들일 수 있습니다.

| 소스 | 익스텐션(extension) |
| --- | --- |
| **Apache Kafka** | Kafka indexing service (`druid-kafka-indexing-service`) |
| **Amazon Kinesis** | Kinesis indexing service (`druid-kinesis-indexing-service`) |

두 방식 모두 **exactly-once** 스트림 처리 보장을 갖춘 실시간 인제스천을 제공합니다. 사용하려면 해당 익스텐션을 **Overlord와 Middle Manager 양쪽**에 로드해야 합니다.

스트리밍 인제스천은 항상 실행 중인 **Supervisor**에 의존합니다. Supervisor는 인덱싱 태스크의 상태를 관리하면서 다음을 수행합니다.

- 핸드오프(handoff) 조정
- 실패 관리
- 확장성(scalability)과 복제(replication) 요구 사항 유지

Supervisor는 JSON 명세(spec)를 Druid 웹 콘솔 또는 Supervisor API로 제출해 시작합니다.

---

### Supervisor

Supervisor는 Kafka, Kinesis 같은 외부 소스에서 들어오는 스트리밍 인제스천을 총괄합니다. 태스크 상태를 관리하고, 핸드오프를 조정하며, 확장성과 복제 요구 사항을 충족하도록 유지합니다.

#### Supervisor spec 구조

Supervisor spec의 최상위 구성 요소는 다음과 같습니다.

| 필드 | 설명 | 필수 |
| --- | --- | --- |
| `id` | Supervisor의 고유 식별자입니다. 지정하지 않으면 dataSource 이름을 사용합니다. | 아니오 |
| `type` | Supervisor 유형입니다. `kafka`, `kinesis`, `rabbit`, `autocompact` 중 하나입니다. | 예 |
| `spec` | 설정 컨테이너입니다. `dataSchema`, `ioConfig`, `tuningConfig`(선택)를 담습니다. | 예 |
| `context` | 추가 설정입니다. | 아니오 |
| `suspended` | 일시 정지(suspend) 상태 여부를 나타내는 불리언 값입니다. | 아니오 |

`spec` 내부 구성은 다음과 같습니다.

- **`dataSchema`**: 인제스천 태스크의 스키마(dataSource 이름, timestampSpec, dimensionsSpec, metricsSpec, granularitySpec 등)
- **`ioConfig`**: 스트림 연결과 입출력 설정
- **`tuningConfig`**: 성능 관련 설정 (선택)

#### ioConfig 공통 속성

Kafka와 Kinesis에 공통으로 적용되는 주요 `ioConfig` 속성입니다.

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `inputFormat` | Object | — | 데이터를 파싱할 입력 포맷 (필수) |
| `taskCount` | Integer | 1 | 하나의 replica set에서 읽기를 수행하는 최대 태스크 수 |
| `replicas` | Integer | 1 | replica set 개수 |
| `taskDuration` | ISO 8601 Period | PT1H | 태스크가 세그먼트 publish로 전환하기 전까지 읽기를 수행하는 시간 |
| `completionTimeout` | ISO 8601 Period | PT30M | publish 완료를 기다리는 제한 시간 |
| `period` | ISO 8601 Period | PT30S | Supervisor가 관리 루프를 반복하는 주기 |
| `startDelay` | ISO 8601 Period | PT5S | Supervisor 시작 시 초기 지연 |

**늦은/이른 메시지 거부 설정**

| 속성 | 설명 |
| --- | --- |
| `lateMessageRejectionStartDateTime` | 지정한 시각보다 이전 타임스탬프를 가진 메시지를 거부합니다. |
| `lateMessageRejectionPeriod` | 태스크 생성 시각을 기준으로 일정 기간보다 오래된 메시지를 거부합니다. |
| `earlyMessageRejectionPeriod` | 미래 타임스탬프를 가진 메시지를 거부합니다. |

#### 태스크 오토스케일러

`ioConfig`의 `autoScalerConfig`로 태스크 수 자동 조절을 활성화할 수 있습니다.

| 속성 | 설명 |
| --- | --- |
| `enableTaskAutoScaler` | 오토스케일러 활성화 플래그 |
| `taskCountMin` / `taskCountMax` | 태스크 수의 하한/상한 |
| `taskCountStart` | 초기 태스크 수 |
| `autoScalerStrategy` | 스케일링 전략. `lagBased` 또는 `costBased` |

- **`lagBased`**: 파티션 lag을 모니터링하고 임계값에 따라 태스크 수를 조절합니다.
- **`costBased`** (experimental): lag과 poll-to-idle 비율을 비용 함수에 반영해 태스크 수를 결정합니다.

#### tuningConfig 공통 속성

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `type` | String | — | `kafka` 또는 `kinesis` (필수) |
| `maxRowsInMemory` | Integer | 150000 | persist 전에 메모리에 유지하는 최대 행 수 |
| `maxBytesInMemory` | Long | JVM 최대 힙의 1/6 | persist를 트리거하는 힙 메모리 임계값 |
| `maxRowsPerSegment` | Integer | 5000000 | 세그먼트당 최대 행 수 |
| `maxTotalRows` | Long | 20000000 | 누적 최대 행 수 |
| `intermediatePersistPeriod` | ISO 8601 Period | PT10M | 중간 persist 주기 |
| `maxPendingPersists` | Integer | 0 | 대기(pending) 가능한 persist 최대 개수 |
| `handoffConditionTimeout` | Long | 900000ms (Kafka) / 0 (Kinesis) | 세그먼트 핸드오프 대기 시간 |
| `offsetFetchPeriod` | ISO 8601 Period | PT30S | 오프셋 조회 주기 |
| `workerThreads` | Integer | min(10, taskCount) | 요청 처리 스레드 수 |

#### Supervisor 상태

**기본 상태**

| 상태 | 설명 |
| --- | --- |
| `PENDING` | 초기화되었지만 아직 스트림에 연결하지 않은 상태 |
| `RUNNING` | 정상적으로 실행·처리 중 |
| `SUSPENDED` | 일시 정지된 상태 |
| `STOPPING` | 종료 중 |
| `UNHEALTHY_SUPERVISOR` | Supervisor 내부 오류 발생 |
| `UNHEALTHY_TASKS` | 최근 태스크 실패 발생 |

**세부 상태 (첫 번째 관리 루프에서만 표시)**

- `CONNECTING_TO_STREAM`: 스트림 연결 중
- `DISCOVERING_INITIAL_TASKS`: 초기 태스크 탐색 중
- `CREATING_TASKS`: 태스크 생성 중
- `IDLE`: 읽어올 새 데이터가 없는 상태

#### Supervisor 관리 작업

| 작업 | 설명 |
| --- | --- |
| **Suspend** | Supervisor를 일시 정지합니다. 로그와 메트릭은 유지되며, 태스크도 정지 상태로 남습니다. |
| **Set Offsets** | 파티션 오프셋을 재설정합니다. 데이터 유실 또는 중복 위험이 있습니다. |
| **Hard Reset** | 저장된 메타데이터를 지우고 earliest 또는 latest 위치부터 다시 읽습니다. 위험한 작업이므로 주의해야 합니다. |
| **Terminate** | Supervisor를 종료하고 세그먼트를 publish합니다. 메타데이터에 tombstone 마커를 남깁니다. |

#### 용량 계획

읽기 태스크 집합과 publish 중인 태스크 집합이 동시에 존재할 수 있으므로, 최소 워커 용량은 다음과 같습니다.

```
workerCapacity = 2 * replicas * taskCount
```

publish 소요 시간이 `taskDuration`을 초과하는 경우에는 추가 용량이 필요합니다.

**다중 Supervisor**: 여러 Supervisor가 같은 데이터소스에 동시에 인제스천할 수 있습니다. 이때 spec의 `context` 필드에 `useConcurrentLocks=true`를 설정해 동시 인제스천 간 동기화를 보장해야 합니다.

---

### Kafka 인제스천

Kafka indexing service를 사용하려면 `druid-kafka-indexing-service` 익스텐션을 Overlord와 Middle Manager에 로드해야 합니다. **Apache Kafka 0.11.x 이상**이 필요합니다.

#### Kafka supervisor spec 예시

```json
{
  "type": "kafka",
  "spec": {
    "dataSchema": {
      "dataSource": "metrics-kafka",
      "timestampSpec": {
        "column": "timestamp",
        "format": "auto"
      },
      "dimensionsSpec": {
        "dimensions": [],
        "dimensionExclusions": [
          "timestamp",
          "value"
        ]
      },
      "metricsSpec": [
        {
          "name": "count",
          "type": "count"
        },
        {
          "name": "value_sum",
          "fieldName": "value",
          "type": "doubleSum"
        },
        {
          "name": "value_min",
          "fieldName": "value",
          "type": "doubleMin"
        },
        {
          "name": "value_max",
          "fieldName": "value",
          "type": "doubleMax"
        }
      ],
      "granularitySpec": {
        "type": "uniform",
        "segmentGranularity": "HOUR",
        "queryGranularity": "NONE"
      }
    },
    "ioConfig": {
      "topic": "metrics",
      "inputFormat": {
        "type": "json"
      },
      "consumerProperties": {
        "bootstrap.servers": "localhost:9092"
      },
      "taskCount": 1,
      "replicas": 1,
      "taskDuration": "PT1H"
    },
    "tuningConfig": {
      "type": "kafka",
      "maxRowsPerSegment": 5000000
    }
  }
}
```

#### Kafka ioConfig

| 속성 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `topic` | String | 예 (`topicPattern` 미지정 시) | — | 인제스천할 Kafka 토픽 |
| `topicPattern` | String | 예 (`topic` 미지정 시) | — | 여러 토픽을 대상으로 하는 정규식 패턴 |
| `consumerProperties` | Object | 예 | — | Kafka consumer 설정 |
| `pollTimeout` | Long | 아니오 | 100 | consumer poll 대기 시간 (ms) |
| `useEarliestOffset` | Boolean | 아니오 | false | 최초 실행 시 earliest 오프셋 사용 여부 (false면 latest) |
| `idleConfig` | Object | 아니오 | null | idle 상태 설정 |

**`consumerProperties` 요구 사항**

- `bootstrap.servers`를 `<BROKER>:<PORT>` 형식으로 반드시 포함해야 합니다.
- `isolation.level`은 트랜잭션 안전성을 위해 기본값이 `read_committed`입니다.
- `group.id`로 자동 생성되는 consumer group ID를 재정의할 수 있습니다.
- SSL/SASL 자격 증명은 환경 변수 dynamic config provider를 사용해 전달하는 것이 좋습니다.

**Kafka 전용 tuningConfig 속성**

| 속성 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `numPersistThreads` | Integer | 아니오 | 1 | 세그먼트 생성·persist에 사용할 스레드 수 |

#### 다중 토픽 인제스천

`topicPattern`으로 정규식에 매칭되는 여러 토픽을 한 번에 인제스천할 수 있습니다. 파티션은 **토픽 이름의 hashcode와 토픽 내 파티션 ID**를 기준으로 태스크에 배정되며, 개별 토픽의 파티션 부하가 서로 비슷하다는 전제를 둡니다. 패턴에 새로 매칭되는 토픽은 자동으로 인제스천 대상에 포함됩니다.

#### Idle 설정

Idle 설정(experimental)을 활성화하면, 지정한 기간 동안 데이터가 들어오지 않을 때 현재 태스크가 완료된 후 새 태스크를 실행하지 않고 Supervisor가 `IDLE` 상태로 전환됩니다.

| 속성 | 설명 |
| --- | --- |
| `enabled` | idle 기능 활성화 여부 |
| `inactiveAfterMillis` | idle 상태로 전환하기까지의 비활성 시간 (ms) |

#### Kafka inputFormat으로 메타데이터 읽기

`kafka` inputFormat은 메시지 payload에 Kafka 메타데이터를 추가로 붙여 인제스천합니다.

**추가되는 필드**

| 필드 | 설명 |
| --- | --- |
| `kafka.timestamp` | 메시지 생성 시각 |
| `kafka.topic` | 메시지가 온 토픽 이름 |
| `kafka.header.*` | 메시지 헤더 (프리픽스 변경 가능) |
| `kafka.key` | 메시지 key 값 |

**설정 옵션**

| 옵션 | 설명 |
| --- | --- |
| `valueFormat` | payload를 파싱할 포맷 |
| `headerFormat` | 헤더 값 인코딩 (기본: UTF-8 문자열) |
| `keyFormat` | key 필드를 파싱할 포맷 (첫 번째 값만 사용) |
| `timestampColumnName` | 타임스탬프 컬럼 이름 재정의 |
| `topicColumnName` | 토픽 컬럼 이름 재정의 |
| `headerColumnPrefix` | 헤더 컬럼 프리픽스 (기본: `kafka.header.`) |
| `keyColumnName` | key 컬럼 이름 재정의 |

inputFormat 예시입니다.

```json
{
  "type": "kafka",
  "valueFormat": {
    "type": "json"
  },
  "headerFormat": {
    "type": "string"
  },
  "keyFormat": {
    "type": "tsv",
    "findColumnsFromHeader": false,
    "columns": ["x"]
  }
}
```

파싱 결과에는 payload 필드와 함께 Kafka 메타데이터 컬럼이 포함됩니다.

```json
{
  "channel": "#sv.wikipedia",
  "timestamp": "2016-06-27T00:00:11.080Z",
  "page": "Salo Toraut",
  "delta": 31,
  "namespace": "Main",
  "kafka.timestamp": 1680795276351,
  "kafka.topic": "wiki-edits",
  "kafka.header.env": "development",
  "kafka.header.zone": "z1",
  "kafka.key": "wiki-edit"
}
```

#### Kafka 배포 시 유의 사항

- Druid는 Kafka 파티션을 각 Kafka 인덱싱 태스크에 배정합니다.
- 태스크는 `maxRowsPerSegment`, `maxTotalRows`, `intermediateHandoffPeriod` 중 하나에 도달하면 새 세그먼트를 만듭니다.
- 증분 핸드오프(incremental handoff)를 지원하므로, 태스크 완료 시점이 아니라 진행 중에도 세그먼트가 점진적으로 조회 가능해집니다.
- 세그먼트 granularity와 태스크 duration이 정확히 맞아떨어지지 않으면 태스크 경계 시점에 작은 세그먼트 조각이 생길 수 있습니다. 재색인(re-indexing)으로 세그먼트를 약 500~700MB의 최적 크기로 병합할 수 있습니다.

---

### Kinesis 인제스천

Kinesis indexing service는 Overlord에서 실행되는 Supervisor로 Kinesis 인덱싱 태스크를 관리합니다. 태스크는 **Kinesis의 shard와 sequence number 메커니즘**으로 exactly-once 인제스천을 보장하며 이벤트를 읽습니다.

사용하려면 `druid-kinesis-indexing-service` core 익스텐션을 Overlord와 Middle Manager에 로드해야 합니다. 프로덕션 배포 전에 알려진 이슈를 먼저 검토하는 것이 좋습니다.

#### Kinesis supervisor spec 예시

```json
{
  "type": "kinesis",
  "spec": {
    "ioConfig": {
      "type": "kinesis",
      "stream": "KinesisStream",
      "inputFormat": {"type": "json"},
      "useEarliestSequenceNumber": true
    },
    "tuningConfig": {"type": "kinesis"},
    "dataSchema": {
      "dataSource": "KinesisStream",
      "timestampSpec": {
        "column": "timestamp",
        "format": "iso"
      },
      "dimensionsSpec": {"dimensions": [...]},
      "granularitySpec": {
        "queryGranularity": "none",
        "rollup": false,
        "segmentGranularity": "hour"
      }
    }
  }
}
```

지원하는 입력 포맷은 `kinesis`, `csv`, `tvs`, `json`, `avro_stream`, `protobuf`, `thrift`입니다.

#### Kinesis ioConfig

| 속성 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `stream` | String | 예 | — | 읽어올 Kinesis 스트림 |
| `endpoint` | String | 아니오 | `kinesis.us-east-1.amazonaws.com` | 리전에 해당하는 Kinesis 엔드포인트 |
| `useEarliestSequenceNumber` | Boolean | 아니오 | false | 최초 실행 시 earliest sequence number 사용 여부 (false면 latest) |
| `fetchDelayMillis` | Integer | 아니오 | 0 | Kinesis fetch 호출 사이 대기 시간 (ms) |
| `awsAssumedRoleArn` | String | 아니오 | — | 권한 획득에 사용할 AWS assumed role |
| `awsExternalId` | String | 아니오 | — | 권한 획득에 사용할 AWS external ID |

#### Kinesis tuningConfig

| 속성 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `skipSequenceNumberAvailabilityCheck` | Boolean | 아니오 | false | shard에 sequence number가 남아 있는지 확인을 건너뛸지 여부 |
| `recordBufferSizeBytes` | Integer | 아니오 | 아래 기본값 참고 | fetch 스레드와 인제스천 스레드 사이 버퍼로 쓸 힙 메모리 |
| `recordBufferOfferTimeout` | Integer | 아니오 | 5000 | 버퍼 공간 확보를 기다리는 시간 (ms) |
| `recordBufferFullWait` | Integer | 아니오 | 5000 | 버퍼가 비워지기를 기다리는 시간 (ms) |
| `fetchThreads` | Integer | 아니오 | 프로세서 수 × 2 | 데이터 fetch 스레드 풀 크기 |
| `maxBytesPerPoll` | Integer | 아니오 | 1000000 | poll당 가져올 최대 바이트 |
| `repartitionTransitionDuration` | ISO 8601 Period | 아니오 | PT2M | shard split/merge 시 전환 대기 시간 |
| `useListShards` | Boolean | 아니오 | false | `LimitExceededException`을 방지하기 위한 `listShards` API 사용 여부 |

#### AWS 인증

**1. 환경 변수와 인스턴스 프로파일**: Druid는 환경 변수 → Web Identity Token → 프로파일 설정 → EC2 인스턴스 메타데이터 순서로 자격 증명을 확인합니다.

**2. 장기 자격 증명**: `common.runtime.properties`에 직접 지정합니다.

```
druid.kinesis.accessKey=AKIAWxxxxxxxxxx4NCK
druid.kinesis.secretKey=Jbytxxxxxxxxxxx2+555
```

**필요한 IAM 권한**

| `useListShards` | 필요 권한 |
| --- | --- |
| `true` | `ListStreams`, `Get*` (`GetShardIterator`용), `GetRecords`, `ListShards` |
| `false` | `ListStreams`, `DescribeStream`, `Get*` (`GetShardIterator`용), `GetRecords` |

#### Fetch 설정과 Kinesis 처리량 한계

**기본값 산정 방식**

- `fetchThreads`: 프로세서 수의 2배. 단, fetch당 10MB를 가정했을 때 최대 힙의 5%를 초과하지 않도록 제한됩니다.
- `fetchDelayMillis`: 0
- `recordBufferSizeBytes`: 100MB 또는 가용 힙의 약 10% 중 작은 값
- `maxBytesPerPoll`: 1000000 바이트

**Kinesis 처리량 한계**

- 레코드: 개당 최대 1MB
- 읽기: shard당 초당 5 트랜잭션
- 처리량: shard당 초당 2MB
- `GetRecords` 응답: 최대 10MB

한계를 초과하면 `ProvisionedThroughputExceededException`이 발생하며, Druid는 `fetchDelayMillis`와 3초 중 큰 값만큼 대기한 뒤 재시도합니다.

#### 디애그리게이션

Kinesis indexing service는 디애그리게이션(de-aggregation)을 지원합니다. 효율적인 전송을 위해 하나의 Kinesis Data Streams 레코드에 여러 행을 담아 보낸 경우, 이를 풀어서 인제스천합니다.

#### 리샤딩

리샤딩(resharding)은 데이터 유입 속도 변화에 맞춰 shard 수를 조정하는 작업입니다. 리샤딩이 진행되는 동안 Supervisor가 shard-태스크 매핑을 갱신하므로 태스크의 조기 종료와 실패가 발생하는 것이 정상입니다. 이 전환 구간은 다음 조건이 모두 충족되면 끝납니다.

- 닫힌 shard를 모두 읽고 데이터를 publish 완료
- 비활성 shard가 배정된 태스크가 모두 종료

**주의**: Supervisor가 실행 중에 새 파티션을 감지하면 `useEarliestSequence` 설정과 무관하게 earliest sequence number부터 읽습니다. 반면 Supervisor가 suspend된 상태에서 리샤딩이 일어났고 `useEarliestSequenceNumber = false`라면, 재개 시 새 shard는 latest sequence부터 읽습니다.

#### 알려진 이슈

- 여러 Supervisor가 같은 스트림을 읽으면 읽기 처리량 한계를 초과할 수 있습니다. 필요하면 shard를 추가해야 합니다.
- Supervisor가 체크포인트 sequence number를 보존 기간(retention window)과 대조하는 과정에서 earliest sequence number를 조회하므로, AWS CloudWatch의 `IteratorAgeMilliseconds` 지표가 높게 보일 수 있습니다.

---

### 태스크

태스크는 Druid에서 인제스천 관련 작업을 실제로 수행하는 단위입니다.

#### 태스크 유형

| 유형 | 설명 |
| --- | --- |
| `index_parallel` | 네이티브 배치 인제스천 (병렬) |
| `index_kafka` | Kafka 인제스천 (Supervisor가 제출) |
| `index_kinesis` | Kinesis 인제스천 (Supervisor가 제출) |
| `compact` | 주어진 인터벌의 세그먼트 컴팩션(compaction) |
| `kill` | 세그먼트 메타데이터 삭제 및 딥 스토리지(deep storage)에서 제거 |

#### 태스크 API와 상태

태스크 API는 두 곳에서 제공됩니다.

1. **Overlord 프로세스의 HTTP API**: 태스크 제출, 취소, 상태 확인, 로그·리포트 조회 등을 수행합니다.
2. **Druid SQL의 `sys.tasks` 메타데이터 테이블**: 활성 태스크와 최근 완료된 태스크 정보를 읽기 전용으로 조회합니다.

#### 태스크 리포트

##### 완료 리포트

태스크 완료 후 다음 엔드포인트에서 리포트를 조회합니다.

```
http://<OVERLORD-HOST>:<OVERLORD-PORT>/druid/indexer/v1/task/{taskId}/reports
```

완료 리포트는 다음을 포함합니다.

- **`ingestionStatsAndErrors`**: 태스크 메트릭과 파싱 예외 정보
- **`taskContext`**: 태스크 설정 정보

입력 인터벌을 분할하는 compaction 태스크의 경우, 리포트에 `ingestionStatsAndErrors_0`, `ingestionStatsAndErrors_1`처럼 인덱스가 붙은 여러 집합이 담깁니다.

**세그먼트 가용성 관련 필드**

| 필드 | 설명 |
| --- | --- |
| `segmentAvailabilityConfirmed` | 생성된 모든 세그먼트가 쿼리 가능 상태가 되었는지 여부 |
| `segmentAvailabilityWaitTimeMs` | 세그먼트 가용성을 기다린 시간 (ms) |
| `recordsProcessed` | 파티션별 처리 레코드 수 |
| `segmentsRead` | compaction이 읽은 세그먼트 수 |
| `segmentsPublished` | compaction이 publish한 세그먼트 수 |

##### 라이브 리포트

실행 중인 태스크는 같은 엔드포인트에서 라이브 리포트를 조회할 수 있으며, 인제스천 상태, 파싱 불가 이벤트, 1분/5분/15분 구간의 이벤트 처리 이동 평균을 담습니다.

**인제스천 상태(ingestion state)**

| 상태 | 설명 |
| --- | --- |
| `NOT_STARTED` | 아직 행을 읽기 시작하지 않음 |
| `DETERMINE_PARTITIONS` | 파티셔닝 결정을 위해 행을 처리 중 (배치 태스크 전용) |
| `BUILD_SEGMENTS` | 세그먼트 생성을 위해 행을 처리 중 |
| `SEGMENT_AVAILABILITY_WAIT` | publish된 세그먼트의 가용성 대기 중 |
| `COMPLETED` | 태스크 완료 |

**행 통계(rowStats) 필드**

| 필드 | 의미 |
| --- | --- |
| `processed` | 오류 없이 인제스천된 행 수 |
| `processedBytes` | 처리한 비압축 바이트 총량 |
| `processedWithError` | 하나 이상의 컬럼에 파싱 오류가 있는 채로 인제스천된 행 수 |
| `thrownAway` | 건너뛴 행 수 (시간 범위 밖이거나 필터로 제외) |
| `unparseable` | 파싱할 수 없어 버려진 행 수 |

라이브 행 통계는 다음 엔드포인트에서 조회합니다.

```
http://<middlemanager-host>:<worker-port>/druid/worker/v1/chat/{taskId}/rowStats
```

Kafka indexing service는 Overlord API로도 조회할 수 있습니다.

```
http://<OVERLORD-HOST>:<OVERLORD-PORT>/druid/indexer/v1/supervisor/{supervisorId}/stats
```

파싱 불가 이벤트는 실행 중인 태스크에서 별도로 조회할 수 있습니다.

```
http://<middlemanager-host>:<worker-port>/druid/worker/v1/chat/{taskId}/unparseableEvents
```

#### 태스크 락

##### 락 유형

**Time chunk lock**: 기본 락 방식입니다. 태스크가 생성할 세그먼트가 기록될 데이터소스의 **time chunk 전체**를 잠급니다. 이 방식으로 생성된 세그먼트는 major version이 더 높고 minor version은 항상 `0`입니다.

**Segment lock** (deprecated): time chunk 전체가 아니라 개별 세그먼트를 잠가 동시 쓰기를 허용합니다. 다만 segment lock은 deprecated 상태이며, 잘못된 쿼리 결과로 이어질 수 있는 알려지지 않은 버그가 있을 수 있습니다. 이 방식의 세그먼트는 major version은 같고 minor version이 더 높습니다.

##### Overshadowing

어떤 세그먼트가 다른 세그먼트를 가리는(overshadow) 조건은 다음과 같습니다.

- major version이 더 높거나,
- major version이 같고 minor version이 더 높은 경우

Major version은 `"yyyy-MM-dd'T'hh:mm:ss"` 형식의 타임스탬프입니다.

##### 락 우선순위

| 태스크 유형 | 기본 우선순위 |
| --- | --- |
| 실시간 인덱스 태스크 | 75 |
| 배치 인덱스 태스크 (native/SQL) | 50 |
| Merge/Append/Compaction 태스크 | 25 |
| 그 외 태스크 | 0 |

우선순위는 컨텍스트로 재정의할 수 있습니다.

```json
"context": {
  "priority": 100
}
```

우선순위가 높은 태스크는 낮은 태스크의 락을 선점(preempt)할 수 있습니다. 단, 세그먼트 publish 중에는 선점되지 않습니다.

#### 태스크 액션

주요 태스크 액션은 다음과 같습니다.

| 액션 | 설명 |
| --- | --- |
| `lockAcquire` | time chunk 락 획득 |
| `lockRelease` | 락 해제 |
| `segmentTransactionalInsert` | 새 세그먼트 publish와 덮어쓰기/삭제를 원자적으로 처리 |
| `segmentAllocate` | pending 세그먼트를 태스크에 할당 |

**segmentAllocate 배칭**: Overlord 설정에 `druid.indexer.tasklock.batchSegmentAllocation = true`를 지정하면 여러 태스크가 같은 데이터소스·인터벌에 동시에 세그먼트를 할당할 때 성능이 개선됩니다.

#### 컨텍스트 파라미터

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| `forceTimeChunkLock` | time chunk 락 강제 사용. `false`는 experimental | `true` |
| `priority` | 태스크 우선순위 | 태스크 유형에 따름 |
| `storeCompactionState` | 세그먼트의 compaction 상태를 메타데이터에 저장 | compaction 태스크 `true`, 그 외 `false` |
| `storeEmptyColumns` | 인제스천 시 빈 컬럼 저장 여부 | `true` |
| `taskLockTimeout` | 락 획득 타임아웃 (ms) | 300000 |
| `useLineageBasedSegmentAllocation` | 병렬 태스크에서 lineage 기반 세그먼트 할당 사용 | `false` (0.21) / `true` (0.22+) |
| `lookupLoadingMode` | lookup 로딩 방식: `ALL`, `NONE`, `ONLY_REQUIRED` | `ALL` |
| `lookupsToLoad` | `ONLY_REQUIRED` 모드에서 로드할 lookup 이름 목록 | `null` |
| `subTaskTimeoutMillis` | 서브 태스크 최대 대기 시간 (ms) | 0 (무제한) |

#### 태스크 로그와 스토리지

**태스크 로그**

- Middle Manager의 로컬 디렉터리(`druid.worker.baseTaskDirs`)에 생성되며, 완료 시 장기 저장소로 push됩니다.
- Overlord API로 조회하면 현재 로그 위치를 자동으로 찾아줍니다.
- `druid.indexer.logs.kill` 관련 속성으로 로그 보존·자동 정리를 설정할 수 있습니다.
- indexing service를 remote 모드로 운영하는 경우 태스크 로그는 S3, Azure Blob Store, Google Cloud Storage, HDFS 중 하나에 저장해야 합니다.

**태스크 스토리지**

스토리지 할당은 다음 설정으로 결정됩니다.

1. `druid.worker.capacity`: 태스크 슬롯 수
2. `druid.worker.baseTaskDirs`: 저장 디렉터리 목록
3. `druid.worker.baseTaskDirSize`: 위치별 저장 용량

모든 슬롯에 동일한 디스크 용량을 배분할 수 있는 가장 큰 크기가 태스크마다 할당됩니다.

---

### 참고 자료

- [Streaming ingestion](https://druid.apache.org/docs/latest/ingestion/streaming)
- [Supervisor](https://druid.apache.org/docs/latest/ingestion/supervisor)
- [Apache Kafka ingestion](https://druid.apache.org/docs/latest/ingestion/kafka-ingestion)
- [Amazon Kinesis ingestion](https://druid.apache.org/docs/latest/ingestion/kinesis-ingestion)
- [Task reference](https://druid.apache.org/docs/latest/ingestion/tasks)
- [Supervisor API](https://druid.apache.org/docs/latest/api-reference/supervisor-api)
- [Tasks API](https://druid.apache.org/docs/latest/api-reference/tasks-api)
