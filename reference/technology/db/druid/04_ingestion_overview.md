# Druid 인제스천(Ingestion) 개요

> 원본: https://druid.apache.org/docs/latest/ingestion/
> 원본: https://druid.apache.org/docs/latest/ingestion/schema-model
> 원본: https://druid.apache.org/docs/latest/ingestion/rollup
> 원본: https://druid.apache.org/docs/latest/ingestion/partitioning
> 원본: https://druid.apache.org/docs/latest/ingestion/data-formats
> 원본: https://druid.apache.org/docs/latest/ingestion/ingestion-spec

Druid에 데이터를 적재하는 두 가지 방식(스트리밍/배치), 스키마 모델, 롤업(rollup), 파티셔닝(partitioning), 지원 데이터 포맷, 인제스천 스펙(ingestion spec)의 구조를 차례로 설명합니다.

---

## 목차

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

## 인제스천 개요

Druid에서 데이터를 적재하는 작업을 **인제스천(ingestion)** 또는 **인덱싱**(indexing)이라고 부릅니다. 인제스천은 소스 시스템에서 데이터를 읽어 **세그먼트**(segment)라는 파일로 저장하는 과정입니다. 세그먼트 하나에는 일반적으로 수백만 개의 행이 들어갑니다.

세그먼트는 딥 스토리지(deep storage)에 저장되며, Historical 프로세스가 이를 가져와 쿼리에 응답합니다.

인제스천 방식은 크게 두 갈래입니다.

- **스트리밍(streaming)**: Kafka, Kinesis 같은 스트림 소스에서 실시간으로 적재합니다.
- **배치(batch)**: 파일 등 정적 소스에서 일괄 적재합니다.

---

## 스트리밍 인제스천

스트리밍 인제스천은 계속 실행되는 **슈퍼바이저**(supervisor)가 제어합니다. 슈퍼바이저는 실시간 데이터 흐름을 관리하는 인제스천 태스크들을 감독합니다.

| 항목 | Kafka | Kinesis |
| --- | --- | --- |
| 슈퍼바이저 타입 | `kafka` | `kinesis` |
| 동작 방식 | Druid가 Apache Kafka에서 직접 읽음 | Druid가 Amazon Kinesis에서 직접 읽음 |
| 늦게 도착한 데이터(late data) 인제스천 | 지원 | 지원 |
| exactly-once 보장 | 지원 | 지원 |

---

## 배치 인제스천

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

## 스키마 모델

Druid는 관계형 데이터베이스의 테이블과 유사한 **데이터소스**(datasource)에 데이터를 저장합니다. Druid의 데이터 모델은 관계형 모델과 시계열(timeseries) 모델의 요소를 함께 갖추고 있습니다.

### 기본 타임스탬프(Primary timestamp)

Druid의 스키마에는 기본 타임스탬프가 반드시 있어야 합니다. 기본 타임스탬프는 다음과 같은 역할을 합니다.

- **데이터 구성**: Druid는 기본 타임스탬프를 기준으로 데이터를 파티셔닝하고 정렬합니다.
- **쿼리 성능**: 시간 범위에 해당하는 데이터를 빠르게 찾아 조회할 수 있습니다.
- **데이터 관리**: 시간 청크(time chunk) 삭제, 특정 기간 덮어쓰기, 보존 정책(retention) 적용 같은 작업의 기준이 됩니다.

소스의 어떤 필드에서 읽어오든 Druid는 타임스탬프를 항상 데이터소스의 **`__time`** 컬럼에 저장합니다. 타임스탬프는 인제스천 시점에 `timestampSpec`으로 설정하며, `granularitySpec`에서 추가 제어가 가능합니다.

### 디멘션(Dimensions)

디멘션은 값을 변경하지 않고 그대로 저장하는 컬럼입니다.

- 쿼리 시점에 그룹핑, 필터링, 집계에 사용할 수 있습니다.
- 롤업을 끄면 디멘션은 일반 데이터베이스의 컬럼처럼 동작합니다.
- 인제스천 시 `dimensionsSpec`으로 설정합니다.

### 메트릭(Metrics)

메트릭은 저장 시점에 집계 형태로 저장하는 컬럼입니다.

- 메트릭은 롤업을 켰을 때 가장 유용합니다.
- 인제스천 시점에 집계 함수를 미리 계산해 둘 수 있습니다.
- 특히 근사 집계자(approximate aggregator)를 쓸 때 쿼리 시점 계산이 빨라집니다.
- 인제스천 시 `metricsSpec`으로 설정합니다.

---

## 롤업

**롤업**(rollup)은 인제스천 시점에 수행하는 사전 집계(pre-aggregation)입니다. `queryGranularity`로 타임스탬프를 잘라낸(truncate) 뒤, 디멘션 값과 타임스탬프 값이 동일한 행들을 하나의 행으로 합칩니다. 저장 데이터 크기와 행 수를 크게 줄일 수 있지만, 개별 이벤트 단위 조회는 불가능해집니다.

롤업은 `granularitySpec`에서 **기본으로 켜져 있습니다**. 원본 레코드를 집계 없이 그대로 보존하려면 `rollup`을 `false`로 설정합니다.

### 롤업 사용 기준

**롤업을 켜는 경우**

- 성능 최적화가 최우선이거나 저장 공간 제약이 엄격할 때
- 고카디널리티(high-cardinality) 디멘션의 원본 값이 필요 없을 때

**롤업을 끄는 경우**

- 개별 행 단위의 결과가 필요할 때
- 임의의 컬럼에 대해 `GROUP BY`나 `WHERE` 쿼리를 실행해야 할 때

### 롤업 비율(rollup ratio) 극대화

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

### Perfect rollup과 best-effort rollup

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

## 파티셔닝

Druid는 데이터 크기를 줄이고 쿼리 성능을 높이기 위한 파티셔닝과 정렬 기능을 제공합니다. 데이터를 여러 데이터소스로 분리할 수도 있고, 하나의 데이터소스 안에서 파티셔닝할 수도 있습니다.

### 시간 청크 파티셔닝(Time chunk partitioning)

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

### 2차 파티셔닝(Secondary partitioning)

2차 파티셔닝은 시간 청크를 특정 디멘션 기준으로 다시 나누어 불변(immutable) 세그먼트를 만듭니다. 필터로 자주 사용하는 자연스러운 디멘션이나, 데이터가 어느 정도 정렬되어 들어오는 디멘션을 파티션 기준으로 선택하는 것이 좋습니다.

| 방식 | 설정 위치 |
| --- | --- |
| SQL | `CLUSTERED BY` 절 |
| Kafka/Kinesis | 업스트림 파티셔닝을 따름. 이후 `CLUSTERED BY`가 있는 `REPLACE`나 컴팩션으로 변경 가능 |
| 네이티브 배치 | `tuningConfig`의 `partitionsSpec` |

### 정렬(Sorting)

각 세그먼트 내부의 행은 정렬되어 저장되며, 이는 압축률과 지역성(locality)을 높입니다. 파티셔닝과 정렬을 함께 사용하면 — 특히 파티셔닝 디멘션을 정렬 순서의 맨 앞에 두면 — 압축과 성능이 눈에 띄게 좋아지는 경우가 많습니다.

| 방식 | 설정 위치 |
| --- | --- |
| SQL | `CLUSTERED BY`의 필드 순서 또는 `segmentSortOrder` 컨텍스트 파라미터 |
| Kafka/Kinesis | `dimensionsSpec`의 필드 순서 |
| 네이티브 배치 | `dimensionsSpec`의 필드 순서 |

기본적으로 Druid는 세그먼트 내부 행을 항상 `__time` 기준으로 먼저 정렬합니다. `forceSegmentSortByTime`을 `false`로 설정하면 이 동작을 끌 수 있는데, 이는 Druid 31 이상이 필요한 실험적(experimental) 기능이며 일부 네이티브 쿼리와 SQL 쿼리에 제약이 생깁니다.

---

## 데이터 포맷

Druid가 기본 지원하는 입력 데이터 포맷은 다음과 같습니다.

- **텍스트 포맷**: JSON, CSV, TSV(또는 커스텀 구분자), Lines(행 단위 텍스트)
- **바이너리 포맷**: ORC(`druid-orc-extensions` 필요), Parquet(`druid-parquet-extensions` 필요), Avro Stream/Avro OCF(`druid-avro-extensions` 필요), Protobuf(`druid-protobuf-extensions` 필요)
- **스트리밍 메타데이터 포맷**: Kafka, Kinesis

입력 포맷은 `ioConfig`의 `inputFormat` 필드로 지정합니다. 정규식 기반 파싱보다는 네이티브 Java `InputFormat` 익스텐션을 사용하는 편이 효율적입니다.

### JSON

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

### CSV

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

### TSV (Delimited)

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

### Lines

각 행을 UTF-8 텍스트로 읽어 `line`이라는 단일 컬럼에 담습니다.

```json
"ioConfig": {
  "inputFormat": {
    "type": "lines"
  }
}
```

### ORC

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

### Parquet

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

### Avro Stream

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

### Avro OCF

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | `avro_ocf`로 지정 | 예 |
| `flattenSpec` | JSON Object | 중첩 값 추출 (`path`만 지원) | 아니요 |
| `schema` | JSON Object | 디코딩에 사용할 reader 스키마 | 아니요 |
| `binaryAsString` | Boolean | 바이트를 UTF-8 문자열로 처리 | 아니요 |

### Protobuf

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

### Kafka

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

### Kinesis

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

### flattenSpec

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

## 인제스천 스펙

모든 인제스천 방식은 인제스천 태스크를 통해 Druid에 데이터를 적재하며, 태스크는 인제스천 스펙으로 정의합니다. 스펙은 세 가지 주요 섹션으로 구성됩니다.

- **`dataSchema`**: 데이터소스 이름, 기본 타임스탬프, 디멘션, 메트릭, 변환·필터 설정
- **`ioConfig`**: 데이터를 읽어올 소스와 파싱 방법. 인제스천 방식별로 세부 내용이 다릅니다.
- **`tuningConfig`**: 인제스천 방식별 성능 튜닝 파라미터

### 전체 예시 (`index_parallel`)

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

### dataSchema

#### dataSource

데이터를 기록할 대상 데이터소스의 이름입니다.

```json
"dataSource": "my-first-datasource"
```

#### timestampSpec

기본 타임스탬프 컬럼을 설정합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `column` | 기본 타임스탬프로 사용할 입력 필드 | `timestamp` |
| `format` | 타임스탬프 파싱 형식: `iso`, `posix`, `millis`, `micro`, `nano`, `auto`, 또는 Joda DateTimeFormat 패턴 | `auto` |
| `missingValue` | 타임스탬프가 null이거나 없을 때 사용할 ISO8601 값 | none |

Druid는 기본 타임스탬프를 반드시 요구하므로, 레코드별 타임스탬프가 없는 데이터셋을 적재할 때는 `missingValue`가 유용합니다.

#### dimensionsSpec

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

#### metricsSpec

인제스천 시점에 적용할 집계자(aggregator) 목록입니다. 롤업을 켰을 때 인제스천 시점 집계를 설정하는 수단이므로 가장 유용합니다. `count`, `doubleSum`, `longSum`, `cardinality` 같은 집계자를 사용할 수 있습니다.

#### granularitySpec

시간 청크와 롤업을 제어합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `type` | 항상 `uniform` | `uniform` |
| `segmentGranularity` | 데이터소스의 시간 청크 단위 (`day`, `month` 등) | `day` |
| `queryGranularity` | 세그먼트 안에 저장하는 타임스탬프 해상도. `segmentGranularity`와 같거나 더 세밀해야 함 | `none` |
| `rollup` | 인제스천 시점 롤업 적용 여부 | `true` |
| `intervals` | 배치 인제스천에서 세그먼트를 만들 ISO8601 시간 범위 목록 | `null` |

#### transformSpec

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

#### Projections

`projectionsSpec`으로 사전 집계 구조인 프로젝션(projection)을 정의할 수 있습니다. 정의한 프로젝션은 해당 데이터소스의 디멘션이 됩니다. 주요 속성은 다음과 같습니다.

- `name`: 프로젝션 이름
- `dimensions`: 집계 대상 디멘션의 부분 집합
- `granularity`: 집계 시간 단위
- `metrics`: 집계자 목록

### ioConfig

데이터를 읽어올 입력 소스와의 연결과 파싱을 제어합니다. `inputFormat`은 여러 방식에서 공통이며, 나머지 속성은 인제스천 방식별로 다릅니다.

```json
"ioConfig": {
  "type": "<ingestion-method-specific type code>",
  "inputFormat": { "type": "json" }
}
```

### tuningConfig

성능 관련 파라미터를 설정합니다.

| 필드 | 설명 | 기본값 |
| --- | --- | --- |
| `type` | 인제스천 방식별 타입 코드 | 필수 |
| `maxRowsInMemory` | 디스크로 persist하기 전 메모리에 유지할 최대 행 수 | 1,000,000 |
| `maxBytesInMemory` | persist를 유발하는 JVM 힙 사용량 임계값(바이트) | 최대 힙의 1/6 |
| `skipBytesInMemoryOverheadCheck` | 메모리 계산에서 오버헤드 제외 여부 | `false` |
| `indexSpec` | 세그먼트 저장 형식 옵션 | 아래 참조 |
| `indexSpecForIntermediatePersists` | 중간 persist 세그먼트의 저장 형식 | `indexSpec`과 동일 |

#### indexSpec

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

### 처리 순서

Druid는 인제스천 스펙의 구성 요소를 정해진 순서로 적용합니다. 먼저 `flattenSpec`(있는 경우), 그다음 `timestampSpec`, 그다음 `transformSpec`, 마지막으로 `dimensionsSpec`과 `metricsSpec` 순입니다.

---

## 참고 자료

- [Ingestion overview](https://druid.apache.org/docs/latest/ingestion/)
- [Druid schema model](https://druid.apache.org/docs/latest/ingestion/schema-model)
- [Data rollup](https://druid.apache.org/docs/latest/ingestion/rollup)
- [Partitioning](https://druid.apache.org/docs/latest/ingestion/partitioning)
- [Source input formats](https://druid.apache.org/docs/latest/ingestion/data-formats)
- [Ingestion spec reference](https://druid.apache.org/docs/latest/ingestion/ingestion-spec)
