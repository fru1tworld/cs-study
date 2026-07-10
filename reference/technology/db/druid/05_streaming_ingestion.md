# Druid 스트리밍 인제스천

> 원본: https://druid.apache.org/docs/latest/ingestion/streaming
> 원본: https://druid.apache.org/docs/latest/ingestion/supervisor
> 원본: https://druid.apache.org/docs/latest/ingestion/kafka-ingestion
> 원본: https://druid.apache.org/docs/latest/ingestion/kinesis-ingestion
> 원본: https://druid.apache.org/docs/latest/ingestion/tasks

Apache Kafka와 Amazon Kinesis에서 실시간으로 데이터를 받아들이는 스트리밍 인제스천(streaming ingestion)의 개요, 인제스천을 총괄하는 Supervisor, Kafka/Kinesis별 설정, 그리고 인제스천을 실제로 수행하는 태스크(task)를 차례로 설명합니다.

---

## 목차

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

## 스트리밍 인제스천 개요

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

## Supervisor

Supervisor는 Kafka, Kinesis 같은 외부 소스에서 들어오는 스트리밍 인제스천을 총괄합니다. 태스크 상태를 관리하고, 핸드오프를 조정하며, 확장성과 복제 요구 사항을 충족하도록 유지합니다.

### Supervisor spec 구조

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

### ioConfig 공통 속성

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

### 태스크 오토스케일러

`ioConfig`의 `autoScalerConfig`로 태스크 수 자동 조절을 활성화할 수 있습니다.

| 속성 | 설명 |
| --- | --- |
| `enableTaskAutoScaler` | 오토스케일러 활성화 플래그 |
| `taskCountMin` / `taskCountMax` | 태스크 수의 하한/상한 |
| `taskCountStart` | 초기 태스크 수 |
| `autoScalerStrategy` | 스케일링 전략. `lagBased` 또는 `costBased` |

- **`lagBased`**: 파티션 lag을 모니터링하고 임계값에 따라 태스크 수를 조절합니다.
- **`costBased`** (experimental): lag과 poll-to-idle 비율을 비용 함수에 반영해 태스크 수를 결정합니다.

### tuningConfig 공통 속성

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

### Supervisor 상태

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

### Supervisor 관리 작업

| 작업 | 설명 |
| --- | --- |
| **Suspend** | Supervisor를 일시 정지합니다. 로그와 메트릭은 유지되며, 태스크도 정지 상태로 남습니다. |
| **Set Offsets** | 파티션 오프셋을 재설정합니다. 데이터 유실 또는 중복 위험이 있습니다. |
| **Hard Reset** | 저장된 메타데이터를 지우고 earliest 또는 latest 위치부터 다시 읽습니다. 위험한 작업이므로 주의해야 합니다. |
| **Terminate** | Supervisor를 종료하고 세그먼트를 publish합니다. 메타데이터에 tombstone 마커를 남깁니다. |

### 용량 계획

읽기 태스크 집합과 publish 중인 태스크 집합이 동시에 존재할 수 있으므로, 최소 워커 용량은 다음과 같습니다.

```
workerCapacity = 2 * replicas * taskCount
```

publish 소요 시간이 `taskDuration`을 초과하는 경우에는 추가 용량이 필요합니다.

**다중 Supervisor**: 여러 Supervisor가 같은 데이터소스에 동시에 인제스천할 수 있습니다. 이때 spec의 `context` 필드에 `useConcurrentLocks=true`를 설정해 동시 인제스천 간 동기화를 보장해야 합니다.

---

## Kafka 인제스천

Kafka indexing service를 사용하려면 `druid-kafka-indexing-service` 익스텐션을 Overlord와 Middle Manager에 로드해야 합니다. **Apache Kafka 0.11.x 이상**이 필요합니다.

### Kafka supervisor spec 예시

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

### Kafka ioConfig

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

### 다중 토픽 인제스천

`topicPattern`으로 정규식에 매칭되는 여러 토픽을 한 번에 인제스천할 수 있습니다. 파티션은 **토픽 이름의 hashcode와 토픽 내 파티션 ID**를 기준으로 태스크에 배정되며, 개별 토픽의 파티션 부하가 서로 비슷하다는 전제를 둡니다. 패턴에 새로 매칭되는 토픽은 자동으로 인제스천 대상에 포함됩니다.

### Idle 설정

Idle 설정(experimental)을 활성화하면, 지정한 기간 동안 데이터가 들어오지 않을 때 현재 태스크가 완료된 후 새 태스크를 실행하지 않고 Supervisor가 `IDLE` 상태로 전환됩니다.

| 속성 | 설명 |
| --- | --- |
| `enabled` | idle 기능 활성화 여부 |
| `inactiveAfterMillis` | idle 상태로 전환하기까지의 비활성 시간 (ms) |

### Kafka inputFormat으로 메타데이터 읽기

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

### Kafka 배포 시 유의 사항

- Druid는 Kafka 파티션을 각 Kafka 인덱싱 태스크에 배정합니다.
- 태스크는 `maxRowsPerSegment`, `maxTotalRows`, `intermediateHandoffPeriod` 중 하나에 도달하면 새 세그먼트를 만듭니다.
- 증분 핸드오프(incremental handoff)를 지원하므로, 태스크 완료 시점이 아니라 진행 중에도 세그먼트가 점진적으로 조회 가능해집니다.
- 세그먼트 granularity와 태스크 duration이 정확히 맞아떨어지지 않으면 태스크 경계 시점에 작은 세그먼트 조각이 생길 수 있습니다. 재색인(re-indexing)으로 세그먼트를 약 500~700MB의 최적 크기로 병합할 수 있습니다.

---

## Kinesis 인제스천

Kinesis indexing service는 Overlord에서 실행되는 Supervisor로 Kinesis 인덱싱 태스크를 관리합니다. 태스크는 **Kinesis의 shard와 sequence number 메커니즘**으로 exactly-once 인제스천을 보장하며 이벤트를 읽습니다.

사용하려면 `druid-kinesis-indexing-service` core 익스텐션을 Overlord와 Middle Manager에 로드해야 합니다. 프로덕션 배포 전에 알려진 이슈를 먼저 검토하는 것이 좋습니다.

### Kinesis supervisor spec 예시

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

### Kinesis ioConfig

| 속성 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `stream` | String | 예 | — | 읽어올 Kinesis 스트림 |
| `endpoint` | String | 아니오 | `kinesis.us-east-1.amazonaws.com` | 리전에 해당하는 Kinesis 엔드포인트 |
| `useEarliestSequenceNumber` | Boolean | 아니오 | false | 최초 실행 시 earliest sequence number 사용 여부 (false면 latest) |
| `fetchDelayMillis` | Integer | 아니오 | 0 | Kinesis fetch 호출 사이 대기 시간 (ms) |
| `awsAssumedRoleArn` | String | 아니오 | — | 권한 획득에 사용할 AWS assumed role |
| `awsExternalId` | String | 아니오 | — | 권한 획득에 사용할 AWS external ID |

### Kinesis tuningConfig

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

### AWS 인증

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

### Fetch 설정과 Kinesis 처리량 한계

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

### 디애그리게이션

Kinesis indexing service는 디애그리게이션(de-aggregation)을 지원합니다. 효율적인 전송을 위해 하나의 Kinesis Data Streams 레코드에 여러 행을 담아 보낸 경우, 이를 풀어서 인제스천합니다.

### 리샤딩

리샤딩(resharding)은 데이터 유입 속도 변화에 맞춰 shard 수를 조정하는 작업입니다. 리샤딩이 진행되는 동안 Supervisor가 shard-태스크 매핑을 갱신하므로 태스크의 조기 종료와 실패가 발생하는 것이 정상입니다. 이 전환 구간은 다음 조건이 모두 충족되면 끝납니다.

- 닫힌 shard를 모두 읽고 데이터를 publish 완료
- 비활성 shard가 배정된 태스크가 모두 종료

**주의**: Supervisor가 실행 중에 새 파티션을 감지하면 `useEarliestSequence` 설정과 무관하게 earliest sequence number부터 읽습니다. 반면 Supervisor가 suspend된 상태에서 리샤딩이 일어났고 `useEarliestSequenceNumber = false`라면, 재개 시 새 shard는 latest sequence부터 읽습니다.

### 알려진 이슈

- 여러 Supervisor가 같은 스트림을 읽으면 읽기 처리량 한계를 초과할 수 있습니다. 필요하면 shard를 추가해야 합니다.
- Supervisor가 체크포인트 sequence number를 보존 기간(retention window)과 대조하는 과정에서 earliest sequence number를 조회하므로, AWS CloudWatch의 `IteratorAgeMilliseconds` 지표가 높게 보일 수 있습니다.

---

## 태스크

태스크는 Druid에서 인제스천 관련 작업을 실제로 수행하는 단위입니다.

### 태스크 유형

| 유형 | 설명 |
| --- | --- |
| `index_parallel` | 네이티브 배치 인제스천 (병렬) |
| `index_kafka` | Kafka 인제스천 (Supervisor가 제출) |
| `index_kinesis` | Kinesis 인제스천 (Supervisor가 제출) |
| `compact` | 주어진 인터벌의 세그먼트 컴팩션(compaction) |
| `kill` | 세그먼트 메타데이터 삭제 및 딥 스토리지(deep storage)에서 제거 |

### 태스크 API와 상태

태스크 API는 두 곳에서 제공됩니다.

1. **Overlord 프로세스의 HTTP API**: 태스크 제출, 취소, 상태 확인, 로그·리포트 조회 등을 수행합니다.
2. **Druid SQL의 `sys.tasks` 메타데이터 테이블**: 활성 태스크와 최근 완료된 태스크 정보를 읽기 전용으로 조회합니다.

### 태스크 리포트

#### 완료 리포트

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

#### 라이브 리포트

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

### 태스크 락

#### 락 유형

**Time chunk lock**: 기본 락 방식입니다. 태스크가 생성할 세그먼트가 기록될 데이터소스의 **time chunk 전체**를 잠급니다. 이 방식으로 생성된 세그먼트는 major version이 더 높고 minor version은 항상 `0`입니다.

**Segment lock** (deprecated): time chunk 전체가 아니라 개별 세그먼트를 잠가 동시 쓰기를 허용합니다. 다만 segment lock은 deprecated 상태이며, 잘못된 쿼리 결과로 이어질 수 있는 알려지지 않은 버그가 있을 수 있습니다. 이 방식의 세그먼트는 major version은 같고 minor version이 더 높습니다.

#### Overshadowing

어떤 세그먼트가 다른 세그먼트를 가리는(overshadow) 조건은 다음과 같습니다.

- major version이 더 높거나,
- major version이 같고 minor version이 더 높은 경우

Major version은 `"yyyy-MM-dd'T'hh:mm:ss"` 형식의 타임스탬프입니다.

#### 락 우선순위

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

### 태스크 액션

주요 태스크 액션은 다음과 같습니다.

| 액션 | 설명 |
| --- | --- |
| `lockAcquire` | time chunk 락 획득 |
| `lockRelease` | 락 해제 |
| `segmentTransactionalInsert` | 새 세그먼트 publish와 덮어쓰기/삭제를 원자적으로 처리 |
| `segmentAllocate` | pending 세그먼트를 태스크에 할당 |

**segmentAllocate 배칭**: Overlord 설정에 `druid.indexer.tasklock.batchSegmentAllocation = true`를 지정하면 여러 태스크가 같은 데이터소스·인터벌에 동시에 세그먼트를 할당할 때 성능이 개선됩니다.

### 컨텍스트 파라미터

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

### 태스크 로그와 스토리지

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

## 참고 자료

- [Streaming ingestion](https://druid.apache.org/docs/latest/ingestion/streaming)
- [Supervisor](https://druid.apache.org/docs/latest/ingestion/supervisor)
- [Apache Kafka ingestion](https://druid.apache.org/docs/latest/ingestion/kafka-ingestion)
- [Amazon Kinesis ingestion](https://druid.apache.org/docs/latest/ingestion/kinesis-ingestion)
- [Task reference](https://druid.apache.org/docs/latest/ingestion/tasks)
- [Supervisor API](https://druid.apache.org/docs/latest/api-reference/supervisor-api)
- [Tasks API](https://druid.apache.org/docs/latest/api-reference/tasks-api)
