# Kafka Streams 설정 (Kafka Streams Configs)

> 이 문서는 Apache Kafka 공식 문서의 "Kafka Streams Configs" 섹션을 한국어로 번역한 것입니다.
> 원본 문서: https://kafka.apache.org/documentation/#streamsconfigs

---

## 목차

1. [개요](#개요)
2. [높은 중요도 설정 (High Importance)](#높은-중요도-설정-high-importance)
3. [중간 중요도 설정 (Medium Importance)](#중간-중요도-설정-medium-importance)
4. [낮은 중요도 설정 (Low Importance)](#낮은-중요도-설정-low-importance)

---

## 개요

Kafka Streams는 Apache Kafka에서 스트림 처리 애플리케이션을 구축하기 위한 클라이언트 라이브러리입니다. 이 문서에서는 Kafka Streams 애플리케이션을 구성하는 데 사용할 수 있는 모든 설정 파라미터를 설명합니다.

설정은 중요도에 따라 세 가지 범주로 구분됩니다:
- 높은 중요도 (High): 필수적이거나 매우 중요한 설정
- 중간 중요도 (Medium): 일반적으로 사용되는 설정
- 낮은 중요도 (Low): 고급 튜닝 또는 특수한 경우에 사용되는 설정

---

## 높은 중요도 설정 (High Importance)

### application.id

스트림 처리 애플리케이션의 식별자입니다. Kafka 클러스터 내에서 고유해야 합니다. 다음 용도로 사용됩니다:
- 기본 클라이언트 ID 접두사
- 멤버십을 위한 그룹 ID
- changelog 토픽 접두사

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | 없음 (필수) |
| 중요도 | high |

### bootstrap.servers

Kafka 클러스터에 대한 초기 연결을 설정하기 위한 호스트/포트 쌍 목록입니다. 클라이언트는 부트스트래핑을 위해 여기에 지정된 서버에 관계없이 모든 서버를 사용합니다.

형식: `host1:port1,host2:port2,...`

이 목록은 전체 클러스터 멤버십을 검색하는 데 사용되는 초기 호스트에만 영향을 미칩니다. 목록은 쉼표로 구분된 형식이어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | 없음 (필수) |
| 중요도 | high |

### ensure.explicit.internal.resource.naming

`true`로 설정하면 모든 내부 토폴로지 리소스(state stores, repartition topics, changelog topics)에 대해 명시적인 이름 지정을 강제합니다. 설정하지 않거나 `false`로 설정하면 내부 리소스에 자동 생성된 이름이 사용될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 중요도 | high |

### num.standby.replicas

각 태스크에 대한 스탠바이 레플리카 수입니다. 스탠바이 레플리카는 각 태스크의 로컬 상태 저장소의 섀도우 복사본을 유지합니다. 태스크가 실패하면 스탠바이 레플리카가 있는 스트림 스레드가 더 빨리 복구할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 0 |
| 유효 값 | 0 이상 |
| 중요도 | high |

### state.dir

상태 저장소(state store)의 디렉토리 위치입니다. 이 경로는 동일한 기본 시스템에서 실행되는 각 스트림 인스턴스에 대해 고유해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | `${java.io.tmpdir}/kafka-streams` |
| 중요도 | high |

---

## 중간 중요도 설정 (Medium Importance)

### acceptable.recovery.lag

클라이언트가 활성 태스크에 대해 따라잡은(caught-up) 것으로 간주되기 위한 최대 허용 지연입니다. 스트림 태스크는 복구 중에 상태를 복원해야 할 수 있으며, 이 설정은 복구가 완료된 것으로 간주될 수 있는 지연의 상한을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 |
| 유효 값 | 0 이상 |
| 중요도 | medium |

### cache.max.bytes.buffering

> 지원 중단됨: `statestore.cache.max.bytes` 사용을 권장합니다.

모든 스레드에서 버퍼링에 사용할 최대 메모리 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10485760 (10 MB) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

### client.id

내부 소비자(consumer), 생산자(producer), 복원 소비자(restore-consumer)에 사용되는 ID 접두사 문자열입니다. 패턴은 `<client.id>-StreamThread-<threadSequenceNumber>-<consumer|producer|restore-consumer>` 형식으로 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" (빈 문자열) |
| 중요도 | medium |

### default.deserialization.exception.handler

`DeserializationExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 역직렬화 오류가 발생했을 때 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.LogAndFailExceptionHandler` |
| 중요도 | medium |

### default.key.serde

`org.apache.kafka.common.serialization.Serde` 인터페이스를 구현하는 키의 기본 직렬화기/역직렬화기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### default.list.key.serde.inner

리스트 키 타입의 내부 serde 클래스입니다. `default.list.key.serde.type`이 설정된 경우 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### default.list.key.serde.type

키에 대한 리스트 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### default.list.value.serde.inner

리스트 값 타입의 내부 serde 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### default.list.value.serde.type

값에 대한 리스트 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### default.production.exception.handler

> 지원 중단됨: `production.exception.handler` 사용을 권장합니다.

`ProductionExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.DefaultProductionExceptionHandler` |
| 중요도 | medium |

### default.timestamp.extractor

`TimestampExtractor` 인터페이스를 구현하는 기본 타임스탬프 추출기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.processor.FailOnInvalidTimestamp` |
| 중요도 | medium |

### default.value.serde

`org.apache.kafka.common.serialization.Serde` 인터페이스를 구현하는 값의 기본 직렬화기/역직렬화기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### deserialization.exception.handler

`DeserializationExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

### group.protocol

사용할 그룹 프로토콜입니다. 클래식(classic) 프로토콜 또는 스트림(streams) 프로토콜을 선택할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | classic |
| 유효 값 | classic, streams |
| 중요도 | medium |

### max.task.idle.ms

스트림 태스크가 다른 입력 파티션에서 추가 레코드를 가져오기 위해 유휴 상태로 대기하는 최대 시간(밀리초)입니다. 이 설정은 여러 입력 스트림이 있을 때 레코드 순서를 보장하는 데 도움이 됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 0 |
| 유효 값 | 0 이상 |
| 중요도 | medium |

### max.warmup.replicas

고가용성을 위해 할당될 수 있는 최대 워밍업 레플리카 수입니다. 워밍업 레플리카는 스탠바이 레플리카를 넘어서 할당될 수 있으며, 스트림의 부하 분산을 위해 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효 값 | 1 이상 |
| 중요도 | medium |

### num.stream.threads

스트림 처리를 실행할 스레드 수입니다. 각 스레드는 독립적으로 태스크를 실행합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효 값 | 1 이상 |
| 중요도 | medium |

### processing.exception.handler

`ProcessingExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 레코드 처리 중 오류가 발생했을 때의 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.LogAndFailProcessingExceptionHandler` |
| 중요도 | medium |

### processing.guarantee

처리 보장 모드입니다. Kafka Streams가 레코드 처리에 대해 제공하는 보장 수준을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | at_least_once |
| 유효 값 | at_least_once, exactly_once_v2 |
| 중요도 | medium |

유효 값 설명:
- `at_least_once`: 최소 한 번 처리 보장. 장애 시 레코드가 재처리될 수 있습니다.
- `exactly_once_v2`: 정확히 한 번 처리 보장. 트랜잭션을 사용하여 원자적 처리를 보장합니다.

### production.exception.handler

`ProductionExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 프로듀서 오류가 발생했을 때의 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.DefaultProductionExceptionHandler` |
| 중요도 | medium |

### replication.factor

스트림 처리 애플리케이션이 생성한 changelog 토픽 및 repartition 토픽의 복제 인수입니다. 클러스터 전체에 데이터를 복제하는 수준을 결정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | -1 |
| 중요도 | medium |

> 참고: -1은 브로커의 기본 복제 인수를 사용함을 의미합니다.

### security.protocol

브로커와 통신하는 데 사용되는 보안 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효 값 | PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL |
| 중요도 | medium |

### statestore.cache.max.bytes

모든 스레드에서 상태 저장소 캐시에 사용할 최대 메모리 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10485760 (10 MB) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

### task.assignor.class

태스크 할당에 사용할 `TaskAssignor` 구현 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | medium |

### task.timeout.ms

태스크가 에러를 발생시키기 전에 정지(stall) 상태로 대기할 수 있는 최대 시간(밀리초)입니다. 이 시간이 초과되면 `TaskTimeoutException`이 발생합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

### topology.optimization

토폴로지 최적화 수준입니다. 스트림 토폴로지에 적용할 최적화를 제어합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | none |
| 유효 값 | none, all, 또는 특정 최적화 조합 |
| 중요도 | medium |

유효 값 설명:
- `none`: 최적화 없음
- `all`: 모든 사용 가능한 최적화 활성화
- 특정 최적화를 쉼표로 구분하여 지정 가능

---

## 낮은 중요도 설정 (Low Importance)

### application.server

상태 저장소 검색을 위한 호스트:포트 쌍입니다. 대화형 쿼리(Interactive Queries)를 사용할 때 다른 애플리케이션 인스턴스가 이 인스턴스에 연결할 수 있는 주소를 지정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" (빈 문자열) |
| 중요도 | low |

### buffered.records.per.partition

파티션당 버퍼링할 최대 레코드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1000 |
| 유효 값 | 0 이상 |
| 중요도 | low |

### built.in.metrics.version

내장 메트릭의 버전입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | latest |
| 중요도 | low |

### commit.interval.ms

프로세서 위치를 저장하기 위해 커밋하는 빈도(밀리초)입니다. `processing.guarantee`가 `exactly_once_v2`인 경우 기본값은 100ms이고, 그렇지 않으면 30000ms(30초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (at_least_once), 100 (exactly_once_v2) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### connections.max.idle.ms

유휴 연결이 닫히기 전까지의 대기 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 540000 (9분) |
| 중요도 | low |

### default.client.supplier

`KafkaClientSupplier` 인터페이스를 구현하는 클래스입니다. Kafka 클라이언트(producer, consumer, admin) 인스턴스를 생성하는 데 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier` |
| 중요도 | low |

### default.dsl.store

DSL 연산자에 사용되는 기본 상태 저장소 타입입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rocksDB |
| 유효 값 | rocksDB, in_memory |
| 중요도 | low |

### dsl.store.suppliers.class

DSL 상태 저장소를 제공하는 `DslStoreSuppliers` 구현 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

### enable.metrics.push

내부 클라이언트 메트릭 푸시 활성화 여부입니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 중요도 | low |

### log.summary.interval.ms

Kafka Streams 상태 요약을 로깅하는 간격(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 120000 (2분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### metadata.max.age.ms

클러스터 메타데이터를 강제로 새로 고침할 때까지의 시간(밀리초)입니다. 새 브로커나 파티션을 사전에 검색하기 위해 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### metadata.recovery.rebootstrap.trigger.ms

브로커 사용 불가 시 재부트스트랩을 트리거하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 중요도 | low |

### metadata.recovery.strategy

브로커 사용 불가 시 복구 전략입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rebootstrap |
| 유효 값 | none, rebootstrap |
| 중요도 | low |

### metric.reporters

메트릭 리포터로 사용할 클래스 목록입니다. `MetricReporter` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" (빈 문자열) |
| 중요도 | low |

### metrics.num.samples

메트릭 계산을 위해 유지하는 샘플 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효 값 | 1 이상 |
| 중요도 | low |

### metrics.recording.level

메트릭의 가장 높은 기록 수준입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | INFO |
| 유효 값 | INFO, DEBUG, TRACE |
| 중요도 | low |

### metrics.sample.window.ms

메트릭 샘플이 계산되는 시간 창(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### poll.ms

입력을 기다리는 블록 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 중요도 | low |

### probing.rebalance.interval.ms

워밍업 레플리카의 상태를 확인하기 위한 탐사 리밸런스 간격(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 600000 (10분) |
| 유효 값 | 60000 이상 |
| 중요도 | low |

### processor.wrapper.class

`ProcessorWrapper` 인터페이스를 구현하는 클래스입니다. 프로세서를 래핑하여 추가 기능을 제공할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

### rack.aware.assignment.non_overlap_cost

태스크 이동 비용입니다. 랙 인식 할당에서 태스크가 다른 인스턴스로 이동할 때의 비용을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 중요도 | low |

### rack.aware.assignment.strategy

랙 인식 할당 전략입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | none |
| 유효 값 | none, min_traffic, balance_subtopology |
| 중요도 | low |

### rack.aware.assignment.tags

스탠바이 분배를 위한 클라이언트 태그입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" (빈 리스트) |
| 중요도 | low |

### rack.aware.assignment.traffic_cost

크로스 랙 트래픽 비용입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 중요도 | low |

### receive.buffer.bytes

데이터를 읽을 때 사용되는 TCP 수신 버퍼(SO_RCVBUF) 크기입니다. -1은 운영 체제 기본값을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 32768 (32 KB) |
| 유효 값 | -1 이상 |
| 중요도 | low |

### reconnect.backoff.max.ms

연결 실패 시 브로커에 대한 재연결을 시도할 때 대기할 최대 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### reconnect.backoff.ms

연결 실패 시 브로커에 대한 재연결을 시도할 때 초기 대기 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 50 |
| 유효 값 | 0 이상 |
| 중요도 | low |

### repartition.purge.interval.ms

repartition 토픽에서 완전히 소비된 레코드를 삭제하는 빈도(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### request.timeout.ms

클라이언트가 요청 응답을 기다리는 최대 시간(밀리초)입니다. 타임아웃이 경과하기 전에 응답이 수신되지 않으면 클라이언트는 필요한 경우 요청을 다시 보내거나 재시도가 소진되면 요청을 실패로 처리합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 40000 (40초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### retry.backoff.ms

실패한 요청을 재시도하기 전에 대기하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효 값 | 0 이상 |
| 중요도 | low |

### rocksdb.config.setter

RocksDB 구성을 설정하는 `RocksDBConfigSetter` 구현 클래스입니다. RocksDB 상태 저장소의 세부 설정을 사용자 정의할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

### send.buffer.bytes

데이터를 보낼 때 사용되는 TCP 송신 버퍼(SO_SNDBUF) 크기입니다. -1은 운영 체제 기본값을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 131072 (128 KB) |
| 유효 값 | -1 이상 |
| 중요도 | low |

### state.cleanup.delay.ms

파티션이 마이그레이션된 후 상태를 삭제하기 전에 대기하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 600000 (10분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

### upgrade.from

라이브 업그레이드를 허용하기 위한 호환 버전입니다. 이전 버전에서 업그레이드할 때 특정 버전을 지정하면 이전 버전과의 호환성을 유지합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효 값 | null, 0.10.0, 0.10.1, 0.10.2, 0.11.0, 1.0, 1.1, 2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4.0 |
| 중요도 | low |

### window.size.ms

deserializer 계산을 위한 창 크기(밀리초)입니다. 윈도우 serde에서만 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | null |
| 중요도 | low |

### windowed.inner.class.serde

윈도우 레코드의 내부 serde 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | low |

### windowstore.changelog.additional.retention.ms

윈도우 유지 기간에 추가되는 changelog 추가 보존 시간(밀리초)입니다. 윈도우 상태 저장소의 changelog 토픽에 대한 추가 보존 기간을 설정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 86400000 (24시간) |
| 유효 값 | 0 이상 |
| 중요도 | low |

---

## 설정 예제

### 기본 설정

```java
Properties props = new Properties();

// 필수 설정
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

// 직렬화 설정
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

// 스레드 수 설정
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);
```

### 정확히 한 번 처리 보장 설정

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "exactly-once-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
```

### 고가용성 설정

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ha-stream-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");

// 스탠바이 레플리카 설정
props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 2);

// 상태 저장소 디렉토리
props.put(StreamsConfig.STATE_DIR_CONFIG, "/var/kafka-streams");

// 복제 인수
props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
```

### 보안 설정 (SSL/SASL)

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "secure-stream-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9093");
props.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "SSL");

// SSL 설정 (consumer/producer 설정을 통해 전달)
props.put("ssl.truststore.location", "/path/to/truststore.jks");
props.put("ssl.truststore.password", "password");
props.put("ssl.keystore.location", "/path/to/keystore.jks");
props.put("ssl.keystore.password", "password");
```

### 성능 튜닝 설정

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "tuned-stream-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

// 캐시 크기 증가
props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 52428800L); // 50 MB

// 커밋 간격 조정
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

// 버퍼링할 레코드 수
props.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 5000);

// 폴링 간격
props.put(StreamsConfig.POLL_MS_CONFIG, 50);
```

---

## 참고 자료

- [Apache Kafka 공식 문서 - Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Apache Kafka Streams Configuration](https://kafka.apache.org/documentation/#streamsconfigs)
- [Kafka Streams Developer Guide](https://kafka.apache.org/documentation/streams/developer-guide/)
- [Kafka Streams Architecture](https://kafka.apache.org/documentation/streams/architecture)
