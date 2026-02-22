# Kafka Streams

> 이 문서는 Apache Kafka 공식 문서의 "Kafka Streams" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/streams/

## 개요

Kafka Streams는 입력과 출력 데이터가 Kafka 클러스터에 저장되는 애플리케이션과 마이크로서비스를 구축하기 위한 클라이언트 라이브러리 입니다. 클라이언트 측 Java 및 Scala 애플리케이션의 단순성과 Kafka의 서버 측 클러스터 기술의 장점을 결합합니다.

### 주요 장점

- 탄력적이고 확장 가능하며 내결함성을 갖춘 아키텍처
- 컨테이너, VM, 베어메탈, 클라우드 등 유연한 배포 옵션
- 모든 규모의 프로젝트에 적합
- 완전한 Kafka 보안 통합
- 표준 Java 및 Scala 개발 지원
- 정확히 한 번 처리 시맨틱(Exactly-once processing semantics) 지원
- 별도의 처리 클러스터 불필요
- 크로스 플랫폼 개발 지원

### 실제 사용 사례

주요 기업들이 Kafka Streams를 활용하고 있습니다:

- The New York Times: 실시간 콘텐츠 배포
- Pinterest: 광고 인프라의 예측 예산 책정
- Zalando: 준실시간 비즈니스 인텔리전스
- Rabobank: 금융 알림 시스템의 실시간 이벤트 처리
- Salesforce: 이벤트 기반 레이어를 통한 실시간 고객 인사이트 생성

---

## 핵심 개념 (Core Concepts)

### 스트림 처리 토폴로지 (Stream Processing Topology)

스트림(Stream)은 무한하고 지속적으로 업데이트되는 데이터 집합 으로, 불변의 키-값 쌍으로 구성됩니다. 아키텍처는 다음을 포함합니다:

- 소스 프로세서(Source Processor): Kafka 토픽에서 데이터를 소비
- 스트림 프로세서(Stream Processor): 변환 노드
- 싱크 프로세서(Sink Processor): Kafka 토픽에 데이터 기록

### 시간 시맨틱 (Time Semantics)

Kafka Streams는 세 가지 시간 개념을 지원합니다:

| 시간 유형 | 설명 |
|-----------|------|
| 이벤트 시간(Event Time) | 원래 이벤트가 발생한 시점 |
| 처리 시간(Processing Time) | 애플리케이션이 레코드를 처리하는 시점 |
| 수집 시간(Ingestion Time) | Kafka가 레코드를 저장하는 시점 |

### 스트림-테이블 이중성 (Stream-Table Duality)

스트림과 테이블은 역관계를 가집니다. 스트림은 테이블의 변경 로그(changelog)로 기능하며, 테이블은 특정 시점에서의 스트림 상태 스냅샷을 나타냅니다.

### 집계 (Aggregations)

집계 연산은 여러 입력 레코드를 단일 출력 레코드로 결합합니다. 입력 유형에 관계없이 항상 KTable 출력을 생성합니다.

### 윈도잉 (Windowing)

윈도잉은 상태 저장 연산을 위해 키별로 레코드를 그룹화합니다. 유예 기간(grace period)은 순서가 맞지 않는 데이터의 허용 범위를 제어합니다.

### 상태 관리 (State Management)

애플리케이션은 데이터를 유지하고 쿼리하기 위해 상태 저장소(state store)를 사용합니다. 영구 키-값 저장소와 내결함성 기능을 갖춘 인메모리 구조를 모두 지원합니다.

### 처리 보장 (Processing Guarantees)

Kafka Streams는 정확히 한 번 시맨틱(exactly-once semantics, EOS v2) 을 제공합니다(버전 2.6.0 이후). 입력 오프셋, 상태 업데이트, 출력 쓰기 간의 원자적 조정을 보장합니다. 애플리케이션은 `processing.guarantee`를 `EXACTLY_ONCE_V2`로 구성해야 합니다(브로커 버전 2.5 이상 필요).

순서가 맞지 않는 데이터 처리는 연산 유형에 따라 다릅니다. 윈도우 집계와 스트림-스트림 조인은 기본적으로 이러한 데이터를 처리하지만, 다른 조인 유형은 적절한 처리를 위해 버전화된 상태 저장소가 필요합니다.

---

## 아키텍처 (Architecture)

Kafka Streams는 Kafka 프로듀서 및 컨슈머 라이브러리를 기반으로 구축되어 Kafka의 기본 기능을 활용하여 데이터 병렬 처리, 분산 조정, 내결함성, 운영 단순성 을 제공합니다.

### 스트림 파티션과 태스크 (Stream Partitions and Tasks)

Kafka Streams는 파티션과 태스크를 병렬 처리의 기본 단위로 활용합니다:

- 각 스트림 파티션은 Kafka 토픽 파티션에 직접 매핑됨
- 데이터 레코드는 개별 Kafka 메시지에 해당
- 키는 양 시스템에서 특정 파티션으로 데이터가 라우팅되는 방식을 결정

프레임워크는 입력 스트림 파티션을 기반으로 고정된 수의 태스크를 생성합니다. 각 태스크는 애플리케이션의 고정된 병렬 처리 단위 로, 할당된 파티션을 받으며 이는 변경되지 않습니다. 태스크는 레코드 버퍼에서 순차적으로 메시지를 처리하여 독립적인 병렬 처리를 가능하게 합니다.

최대 애플리케이션 병렬 처리는 입력 토픽 파티션 수에 의해 제한됩니다. 사용 가능한 파티션보다 많은 애플리케이션 인스턴스를 실행하면 일부 인스턴스가 유휴 상태가 되며, 활성 인스턴스 장애 시 활성화됩니다.

### 스레딩 모델 (Threading Model)

Kafka Streams는 애플리케이션 인스턴스 내에서 여러 스레드를 구성할 수 있습니다. 각 스레드는 하나 이상의 태스크와 해당 프로세서 토폴로지를 독립적으로 실행 할 수 있습니다. 이 아키텍처는 스레드 간에 공유 상태가 없으므로 스레드 간 조정이 필요하지 않습니다.

스케일링은 단순히 추가 애플리케이션 인스턴스를 시작하는 것으로 이루어집니다. Kafka Streams는 모든 인스턴스의 스레드 간에 파티션을 자동으로 재분배합니다. Kafka 2.8부터 클라이언트를 재시작하지 않고도 스트림 스레드를 동적으로 추가하거나 제거할 수 있습니다.

### 로컬 상태 저장소 (Local State Stores)

상태 저장소(State Store) 는 애플리케이션이 상태 저장 연산을 위한 데이터를 저장하고 쿼리할 수 있게 합니다. 프레임워크는 `join()`, `aggregate()`, 윈도잉과 같은 연산을 위해 이러한 저장소를 자동으로 생성하고 관리합니다.

각 태스크는 API를 통해 접근 가능한 전용 로컬 상태 저장소를 유지합니다. Kafka Streams는 상태 업데이트를 추적하는 복제된 변경 로그 토픽을 통해 내결함성과 자동 복구를 보장합니다. 로그 압축(log compaction)은 무제한 토픽 성장을 방지합니다.

### 내결함성 (Fault Tolerance)

아키텍처는 Kafka의 기본 내결함성을 활용합니다. 머신 장애 시 태스크는 남은 인스턴스에서 자동으로 재시작되고, 상태 저장소는 변경 로그 토픽을 재생하여 복원됩니다.

사용자는 복구 시간을 최소화하기 위해 대기 복제본(standby replicas)을 구성할 수 있습니다. `rack.aware.assignment.tags`와 `rack.aware.assignment.strategy`를 통해 제어되는 랙 인식 할당은 대기 태스크를 다른 랙에 분배하여 장애 시 크로스 랙 트래픽을 줄입니다.

---

## 빠른 시작 (Quick Start)

### WordCount 데모

빠른 시작은 무한 데이터 스트림에서 단어 발생 횟수를 계산하는 `WordCountDemo`를 사용합니다. 기존의 유한 wordcount 예제와 달리 이 버전은 무한 스트림에서 작동하며 주기적으로 현재 상태를 출력합니다.

### 단계별 가이드

1-2단계: Kafka 4.1.1을 다운로드하고 KRaft 모드로 서버를 시작합니다(클러스터 UUID 생성, 로그 디렉토리 포맷, 브로커 시작).

```bash
$ tar -xzf kafka_2.13-4.1.1.tgz
$ cd kafka_2.13-4.1.1
$ KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
$ bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
$ bin/kafka-server-start.sh config/server.properties
```

3단계: 두 개의 토픽을 생성합니다:

```bash
$ bin/kafka-topics.sh --create --topic streams-plaintext-input --bootstrap-server localhost:9092
$ bin/kafka-topics.sh --create --topic streams-wordcount-output --config cleanup.policy=compact --bootstrap-server localhost:9092
```

4단계: WordCount 애플리케이션을 시작합니다:

```bash
$ bin/kafka-run-class.sh org.apache.kafka.streams.examples.wordcount.WordCountDemo
```

5단계: 콘솔 프로듀서로 메시지를 전송하고 콘솔 컨슈머로 단어 수 업데이트를 확인합니다:

```bash
$ bin/kafka-console-producer.sh --topic streams-plaintext-input --bootstrap-server localhost:9092
>all streams lead to kafka
>hello kafka streams

$ bin/kafka-console-consumer.sh --topic streams-wordcount-output --from-beginning --bootstrap-server localhost:9092 --property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

6단계: Ctrl-C로 모든 컴포넌트를 정상적으로 종료합니다.

### 예제 출력

"all streams lead to kafka"를 처리하면 개별 단어 수가 생성됩니다. "hello kafka streams"를 처리하면 기존 항목이 업데이트됩니다(kafka: 1->2, streams: 1->2). 이는 무한 데이터에 대한 스트리밍 계산의 기본 특성인 상태 변경을 보여줍니다.

---

## 튜토리얼: 스트림 애플리케이션 작성

### Maven 프로젝트 설정

Kafka Streams Maven Archetype을 사용하여 새 프로젝트를 스캐폴딩합니다:

```bash
mvn archetype:generate \
-DarchetypeGroupId=org.apache.kafka \
-DarchetypeArtifactId=streams-quickstart-java \
-DarchetypeVersion=4.1.1 \
-DgroupId=streams.examples \
-DartifactId=streams-quickstart \
-Dversion=0.1 \
-Dpackage=myapps
```

### 첫 번째 애플리케이션: Pipe

목적: 한 Kafka 토픽에서 다른 토픽으로 데이터를 라우팅하는 가장 간단한 Streams 애플리케이션입니다.

핵심 구성 설정:

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
```

핵심 컴포넌트:

- StreamsBuilder: 처리 토폴로지를 구성
- KStream: 연속적인 레코드 스트림을 나타냄
- Topology: 소스 및 싱크 노드가 있는 계산 로직을 정의

기본 구현:

```java
final StreamsBuilder builder = new StreamsBuilder();
builder.stream("streams-plaintext-input").to("streams-pipe-output");
```

### 두 번째 애플리케이션: Line Split

목적: 텍스트 라인을 개별 단어로 분리하는 상태 비저장 변환을 도입합니다.

핵심 연산:

```java
source.flatMapValues(value -> Arrays.asList(value.split("\\W+")))
      .to("streams-linesplit-output");
```

`flatMapValues()` 연산자는 문자열 값을 분리하고 여러 출력 레코드를 생성하여 각 레코드를 처리합니다.

### 세 번째 애플리케이션: WordCount

목적: 단어 빈도 계산을 통해 상태 저장 처리를 시연합니다.

핵심 기술:

1. 그룹화(Grouping): `groupBy()` 연산자가 키(단어 자체)별로 스트림을 재구성
2. 집계(Aggregation): `count()` 연산자가 상태 저장소를 사용하여 실행 중인 집계를 유지
3. 상태 관리: 결과가 "counts-store"라는 `KeyValueStore`에 저장됨

구현 패턴:

```java
source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
      .groupBy((key, value) -> value)
      .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
      .toStream()
      .to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.Long()));
```

---

## DSL API

Kafka Streams DSL은 세 가지 주요 스트림/테이블 추상화를 제공합니다.

### KStream

무한 레코드 스트림을 나타내며, 각 데이터 레코드는 INSERT 연산으로 취급됩니다. 동일한 키를 가진 연속 레코드는 서로를 덮어쓰지 않습니다. 사용자가 (alice, 1) 그 다음 (alice, 3) 레코드를 받으면 값을 합산할 경우 결과는 3이 아닌 4가 됩니다.

### KTable

레코드가 UPSERT(INSERT/UPDATE) 연산을 나타내는 변경 로그 스트림을 모델링합니다. 동일한 키를 가진 기존 행은 덮어쓰여집니다. 동일한 예제에서 두 번째 레코드가 첫 번째를 업데이트하므로 합계는 3이 됩니다.

### GlobalKTable

각 애플리케이션 인스턴스가 토픽의 모든 파티션 데이터로 채워지는 특수한 변경 로그 스트림입니다. KTable의 파티션 방식과 달리 GlobalKTable은 모든 데이터를 모든 인스턴스에 브로드캐스트하여 공동 파티셔닝 요구 사항 없이 효율적인 스타 조인과 외래 키 조회를 가능하게 합니다.

### 상태 비저장 연산 (Stateless Operations)

상태 저장소가 필요 없는 필터링, 매핑, 분기, 병합을 포함합니다:

| 연산 | 설명 |
|------|------|
| Filter/FilterNot | 조건에 따라 레코드 유지 또는 폐기 |
| Map/MapValues | 키-값 쌍 변환 (map은 키 변경 허용; mapValues는 키 유지) |
| FlatMap/FlatMapValues | 0개, 1개 또는 여러 출력 레코드 생성 |
| Branch/Split | 조건에 따라 다른 다운스트림 경로로 레코드 라우팅 |
| Merge | 각 소스 내 상대적 순서를 유지하면서 여러 스트림 결합 |

### 상태 저장 변환 (Stateful Transformations)

집계, 조인, 윈도잉과 같은 연산에 상태 저장소가 필요합니다.

집계 는 초기화자와 가산자를 사용하여 그룹화된 데이터를 축소합니다. 롤링 집계(count, reduce, aggregate)는 비윈도우 데이터에서 작동하고, 윈도우 변형은 텀블링, 호핑, 슬라이딩, 세션 윈도우를 지원합니다.

조인 은 여러 패턴에 걸쳐 스트림과 테이블을 연결합니다:

- KStream-KStream: 공동 파티셔닝된 데이터가 필요한 윈도우 조인
- KTable-KTable: 일치하는 키에 대한 비윈도우 등가 조인
- KTable-KTable 외래 키: 공동 파티셔닝 요구 사항 없이 파생된 키에 대한 조인
- KStream-KTable: 스트림 레코드를 통한 비윈도우 테이블 조회
- KStream-GlobalKTable: 공동 파티셔닝 없이 완전히 복제된 테이블에 대한 조회

### 윈도우 유형 (Window Types)

| 윈도우 유형 | 설명 |
|-------------|------|
| 텀블링 윈도우(Tumbling Windows) | 에포크에 정렬된 고정 크기의 비중첩, 갭 없는 간격 |
| 호핑 윈도우(Hopping Windows) | 구성 가능한 진행 간격이 있는 고정 크기의 중첩 간격 |
| 슬라이딩 윈도우(Sliding Windows) | 조인을 위한 타임스탬프 차이 기반의 연속 윈도우 |
| 세션 윈도우(Session Windows) | 비활성 갭으로 구분된 동적 크기의 활동 기반 윈도우 |

---

## Processor API

Processor API는 개발자가 사용자 정의 스트림 프로세서를 구성하고 상태 저장소와 상호 작용할 수 있게 합니다. 상태 비저장 및 상태 저장 연산을 모두 지원합니다.

### 스트림 프로세서 정의

스트림 프로세서는 프로세서 토폴로지의 노드로 작동하며 한 번에 하나의 레코드를 처리합니다. 구현은 `Processor` 인터페이스를 다음 핵심 메서드로 확장해야 합니다:

- `init()`: 태스크 생성 시 호출; 레코드 메타데이터 접근 및 펑추에이션 함수 스케줄링을 위한 `ProcessorContext` 수신
- `process()`: 수신된 각 레코드에 대해 호출
- `close()`: 처리 완료 후 리소스 정리

```java
public class WordCountProcessor implements Processor<String, String, String, String> {
    private ProcessorContext<String, String> context;
    private KeyValueStore<String, Integer> kvStore;

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        this.kvStore = context.getStateStore("Counts");
    }

    @Override
    public void process(Record<String, String> record) {
        // 처리 로직
    }

    @Override
    public void close() {
        // 리소스 정리
    }
}
```

### 펑추에이션 함수 (Punctuation Functions)

스케줄된 펑추에이션은 두 가지 시간 유형에 기반한 주기적 호출을 가능하게 합니다:

1. 스트림 시간(Stream-time): 데이터 도착에 의해 트리거; 레코드가 처리될 때만 진행
2. 벽시계 시간(Wall-clock-time): 데이터 흐름과 독립적으로 실제 경과 시간에 의해 트리거

### 상태 저장소 (State Stores)

| 저장소 유형 | 저장 방식 | 내결함성 | 사용 사례 |
|-------------|-----------|----------|-----------|
| 영구 KeyValueStore | RocksDB | 예 (기본) | 대부분의 시나리오에 권장; 디스크 기반 |
| 인메모리 KeyValueStore | RAM | 예 (기본) | 제한된 디스크 가용성 환경 |

상태 저장소는 기본적으로 압축된 변경 로그 토픽으로 지원됩니다. 기능은 다음을 포함합니다:
- 장애 간 데이터 지속성
- 인스턴스 간 상태 마이그레이션
- 머신 장애로부터의 복구

### 토폴로지 구성

```java
Topology builder = new Topology();
builder.addSource("Source", "source-topic")
    .addProcessor("Process", () -> new WordCountProcessor(), "Source")
    .addStateStore(countStoreBuilder, "Process")
    .addSink("Sink", "sink-topic", "Process");
```

---

## 구성 (Configuration)

Kafka Streams는 사용 전 구성이 필요합니다. 설정은 `APPLICATION_ID_CONFIG` 및 `BOOTSTRAP_SERVERS_CONFIG`와 같은 매개변수가 있는 `java.util.Properties` 인스턴스에서 지정됩니다.

### 필수 매개변수

| 매개변수 | 설명 |
|----------|------|
| application.id | 모든 인스턴스에 걸친 스트림 처리 애플리케이션의 고유 식별자 |
| bootstrap.servers | 초기 Kafka 클러스터 연결을 위한 쉼표로 구분된 브로커 주소 목록 |

### 복원력 구성 (Resiliency Configuration)

프로덕션 내결함성을 위한 권장 설정:

| 매개변수 | 현재 | 권장 |
|----------|------|------|
| acks | "1" | "all" |
| replication.factor | -1 | 3 |
| min.insync.replicas | 1 | 2 |
| num.standby.replicas | 0 | 1 |

### 주요 선택적 매개변수

| 매개변수 | 기본값 | 설명 |
|----------|--------|------|
| num.stream.threads | 1 | 애플리케이션 인스턴스당 처리 스레드 수 |
| processing.guarantee | "at_least_once" | 처리 시맨틱 ("exactly_once_v2"는 브로커 2.5+ 필요) |
| state.dir | `/${java.io.tmpdir}/kafka-streams` | 영구 상태 저장소의 디렉토리 위치 |
| num.standby.replicas | 0 | 장애 조치 최소화를 위한 섀도우 상태 저장소 복사본 수 |
| max.task.idle.ms | 0 | 프로듀서 데이터 대기 시간을 지정하여 조인/병합 순서 맞지 않는 처리 제어 |

### 예외 처리

세 가지 구성 가능한 예외 핸들러가 다른 실패 시나리오를 관리합니다:

- deserialization.exception.handler: 역직렬화 실패 관리
- production.exception.handler: 브로커 상호 작용 오류 처리
- processing.exception.handler: 레코드 처리 실패 관리

### 클라이언트 구성 접두사

내부 클라이언트 유형별로 매개변수를 사용자 정의할 수 있습니다:

- `consumer.` / `producer.` / `admin.`: 일반 클라이언트 접두사
- `main.consumer.`, `restore.consumer.`, `global.consumer.`: 특정 컨슈머 유형
- `topic.`: 내부 리파티션/변경 로그 토픽 설정

```java
streamsSettings.put("consumer.session.timeout.ms", "60000");
```

---

## 테스팅 (Testing)

### 테스트 유틸리티 설정

테스팅 기능을 통합하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams-test-utils</artifactId>
    <version>4.1.1</version>
    <scope>test</scope>
</dependency>
```

### TopologyTestDriver

프레임워크는 `TopologyTestDriver`를 주요 테스팅 메커니즘으로 제공합니다. 이 도구는 "입력 토픽에서 레코드를 지속적으로 가져와 토폴로지를 순회하여 처리하는 라이브러리 런타임을 시뮬레이션"합니다.

주요 기능:

- Processor API 또는 DSL로 구축된 토폴로지를 통해 수동으로 파이프된 데이터 처리
- 프로세서 결과 및 상태 저장소 업데이트 검증
- 이벤트 시간 및 벽시계 시간 펑추에이션 지원
- 임베디드 상태 저장소의 사전 채우기 및 쿼리

### 입출력 토픽 테스팅

TestInputTopic 은 데이터 주입을 가능하게 합니다:

```java
TestInputTopic<String, Long> inputTopic = testDriver.createInputTopic(
    "input-topic",
    stringSerde.serializer(),
    longSerde.serializer()
);
inputTopic.pipeInput("key", 42L);
```

TestOutputTopic 은 결과 검증을 용이하게 합니다:

```java
TestOutputTopic<String, Long> outputTopic = testDriver.createOutputTopic(
    "output-topic",
    stringSerde.deserializer(),
    longSerde.deserializer()
);
assertThat(outputTopic.readKeyValue(), equalTo(new KeyValue<>("key", 42L)));
```

### 고급 테스팅 기능

펑추에이션 제어:

```java
testDriver.advanceWallClockTime(Duration.ofSeconds(20));
```

상태 저장소 접근:

```java
KeyValueStore store = testDriver.getKeyValueStore("store-name");
```

### 리소스 관리

적절한 정리가 필수적입니다. 모든 리소스가 적절하게 해제되도록 테스트 드라이버를 항상 닫아야 합니다:

```java
testDriver.close();
```

---

## 대화형 쿼리 (Interactive Queries)

대화형 쿼리는 애플리케이션이 애플리케이션 인스턴스 외부에서 상태에 접근할 수 있게 합니다. Kafka Streams는 로컬 및 원격 컴포넌트 모두를 통해 여러 애플리케이션 인스턴스에 걸쳐 분산된 상태를 쿼리하는 것을 지원합니다.

### 로컬 상태 쿼리

애플리케이션 인스턴스는 로컬로 관리되는 상태 부분을 쿼리하고 자체 로컬 상태 저장소를 직접 쿼리할 수 있습니다. 이러한 연산은 기본 저장소에 대한 무단 변경을 방지하기 위해 읽기 전용입니다.

```java
ReadOnlyKeyValueStore<String, Long> keyValueStore =
    streams.store(StoreQueryParameters.fromNameAndType("counts", QueryableStoreTypes.keyValueStore()));
Long value = keyValueStore.get("key");
```

### 원격 상태 쿼리

전체 애플리케이션 상태에 접근하려면 여러 실행 중인 인스턴스에 걸쳐 분산된 상태 조각을 연결해야 하며, 이는 인스턴스 간 네트워크 통신을 포함합니다.

전체 애플리케이션 상태 쿼리를 활성화하려면 세 가지 단계가 필요합니다:

1. RPC 레이어 추가: 애플리케이션 내에 통신 메커니즘(REST API, Thrift 등) 임베드
2. `application.server` 구성: 각 인스턴스가 네트워크 검색을 가능하게 하는 이 구성 설정에 대한 고유 값을 가짐
3. 검색 및 라우팅 구현: `StreamsMetadata` 객체를 사용하여 인스턴스를 찾고 쿼리를 적절히 전달

### 검색 메서드

- `allMetadata()`: 모든 애플리케이션 인스턴스 찾기
- `allMetadataForStore()`: 특정 저장소를 관리하는 인스턴스 찾기
- `metadataForKey()`: 특정 키의 데이터를 보유한 인스턴스 식별

---

## 보안 (Security)

Kafka Streams는 Kafka의 보안 기능과 기본적으로 통합되며 모든 클라이언트 측 보안 기능을 지원합니다. 프레임워크는 보안 조치를 구현하기 위해 Java Producer 및 Consumer API를 활용합니다.

### 주요 보안 기능

| 기능 | 설명 |
|------|------|
| 데이터 암호화 | Streams 앱과 Kafka 브로커 간의 클라이언트-서버 통신 암호화 활성화 |
| 클라이언트 인증 | 특정 애플리케이션만 Kafka 클러스터에 접근하도록 연결 제한 |
| 클라이언트 권한 부여 | 어떤 애플리케이션이 특정 토픽에서 읽거나 쓸 수 있는지 정의하는 접근 제어 |

### 보안 클러스터를 위한 필수 ACL 설정

보안된 클러스터에서 Kafka Streams를 실행할 때, 애플리케이션을 실행하는 주체는 내부 토픽을 생성, 읽기, 쓰기하기 위한 적절한 접근 제어 목록 권한이 필요합니다.

### 구성 예제

```properties
bootstrap.servers=kafka.example.com:9093
security.protocol=SSL
ssl.truststore.location=/etc/security/tls/kafka.client.truststore.jks
ssl.truststore.password=test1234
ssl.keystore.location=/etc/security/tls/kafka.client.keystore.jks
ssl.keystore.password=test1234
ssl.key.password=test1234
```

---

## 업그레이드 가이드 (Upgrade Guide)

### 4.1.1로 업그레이드

버전 3.4 이전에서 업그레이드할 때 가이드는 2단계 롤링 바운스 접근 방식을 권장합니다:

1. 첫 번째 바운스: `upgrade.from` 구성을 현재 버전으로 설정
2. 두 번째 바운스: `upgrade.from` 구성 제거

이 프로세스는 협력적 리밸런싱 프로토콜 도입, 외래 키 조인 직렬화 형식 수정, 내부 리파티션 토픽 직렬화 업데이트라는 세 가지 중요한 변경 사항을 해결합니다.

### 브로커 호환성

버전 4.0.0부터 Kafka Streams는 버전 2.1 이상의 브로커에서 실행할 때만 호환됩니다. 또한 정확히 한 번 시맨틱(EOS)은 브로커가 최소 버전 2.5 이상이어야 합니다.

### 4.1.0 기능

이 릴리스는 Streams Rebalance Protocol 의 얼리 액세스를 도입합니다. 이는 Kafka Streams 애플리케이션을 위해 특별히 설계된 브로커 주도 리밸런싱 시스템입니다.

주요 기능:
- `group.protocol=streams`를 사용한 Core Streams Group Rebalance Protocol
- 태스크 이동을 최소화하는 Sticky Task Assignor
- Interactive Query 지원
- 새로운 StreamsGroupDescribe RPC 및 `kafka-streams-groups.sh`를 통한 CLI 도구

아직 지원되지 않음: 정적 멤버십, 토폴로지 업데이트, 고가용성 할당자, regex 패턴, 리셋 연산.

### 4.0.0 주요 변경 사항

이 릴리스는 EOS 버전 1 지원을 제거하고 버전 3.5까지 누적된 더 이상 사용되지 않는 메서드를 제거합니다:
- 이전 프로세서 API
- `KStream#through()` 메서드
- Transformer 관련 클래스
- `KStream#branch` 연산
- Window 빌더 메서드

---

## 요약

| 기능 | 설명 |
|------|------|
| 핵심 개념 | 스트림 처리 토폴로지, 시간 시맨틱, 스트림-테이블 이중성 |
| 아키텍처 | 파티션/태스크 기반 병렬 처리, 스레딩 모델, 로컬 상태 저장소, 내결함성 |
| DSL API | KStream, KTable, GlobalKTable, 상태 비저장/상태 저장 연산, 윈도잉 |
| Processor API | 사용자 정의 프로세서, 펑추에이션, 상태 저장소 관리 |
| 구성 | 필수/선택적 매개변수, 복원력 설정, 예외 처리 |
| 테스팅 | TopologyTestDriver, TestInputTopic, TestOutputTopic |
| 대화형 쿼리 | 로컬/원격 상태 쿼리, 분산 상태 접근 |
| 보안 | 암호화, 인증, 권한 부여, ACL 설정 |

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Streams 문서](https://kafka.apache.org/documentation/streams/)
- [Kafka Streams 개발자 가이드](https://kafka.apache.org/documentation/streams/developer-guide/)
- [Kafka Streams Javadoc](https://kafka.apache.org/javadoc/)
- [Kafka GitHub Repository](https://github.com/apache/kafka)
