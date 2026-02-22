# Kafka APIs

> 이 문서는 Apache Kafka 공식 문서의 APIs 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#api

---

## 개요

Kafka는 다섯 가지 핵심 API를 제공합니다:

1. Producer API - 토픽에 데이터 스트림 전송
2. Consumer API - 토픽에서 데이터 스트림 읽기
3. Streams API - 입력 토픽에서 출력 토픽으로 데이터 스트림 변환
4. Connect API - 외부 시스템과 Kafka 간 데이터 연동
5. Admin API - Kafka 객체 관리 및 검사

---

## Producer API

Producer API를 사용하면 애플리케이션이 Kafka 클러스터의 토픽에 데이터 스트림을 전송할 수 있습니다.

### Maven 의존성

Producer API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 주요 특징

- 비동기 전송: 메시지를 비동기적으로 전송하여 높은 처리량 달성
- 배치 처리: 여러 메시지를 배치로 묶어 네트워크 효율성 향상
- 파티셔닝: 메시지를 특정 파티션에 할당하여 순서 보장
- 직렬화: 다양한 데이터 형식 지원 (String, Avro, JSON 등)

### 기본 사용 예제

```java
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class SimpleProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        ProducerRecord<String, String> record =
            new ProducerRecord<>("my-topic", "key", "value");

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent to partition " +
                    metadata.partition() + " with offset " + metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });

        producer.close();
    }
}
```

### 주요 설정 옵션

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `bootstrap.servers` | Kafka 브로커 주소 목록 | - |
| `acks` | 메시지 확인 수준 (0, 1, all) | 1 |
| `retries` | 전송 실패 시 재시도 횟수 | 2147483647 |
| `batch.size` | 배치 크기 (바이트) | 16384 |
| `linger.ms` | 배치 전송 대기 시간 | 0 |
| `buffer.memory` | 버퍼 메모리 크기 | 33554432 |

자세한 내용은 [KafkaProducer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)을 참조하세요.

---

## Consumer API

Consumer API를 사용하면 애플리케이션이 Kafka 클러스터의 토픽에서 데이터 스트림을 읽을 수 있습니다.

### Maven 의존성

Consumer API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 주요 특징

- Consumer Group: 여러 컨슈머가 그룹을 형성하여 파티션을 분산 처리
- 오프셋 관리: 자동 또는 수동으로 오프셋 커밋 관리
- 리밸런싱: 컨슈머 추가/제거 시 자동으로 파티션 재할당
- 역직렬화: 다양한 데이터 형식 지원

### 기본 사용 예제

```java
import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-consumer-group");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));

        try {
            while (true) {
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("partition = %d, offset = %d, " +
                        "key = %s, value = %s%n",
                        record.partition(), record.offset(),
                        record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}
```

### 주요 설정 옵션

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `bootstrap.servers` | Kafka 브로커 주소 목록 | - |
| `group.id` | Consumer Group ID | - |
| `auto.offset.reset` | 초기 오프셋 설정 (earliest, latest) | latest |
| `enable.auto.commit` | 자동 오프셋 커밋 여부 | true |
| `auto.commit.interval.ms` | 자동 커밋 간격 | 5000 |
| `max.poll.records` | poll() 호출당 최대 레코드 수 | 500 |

자세한 내용은 [KafkaConsumer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)을 참조하세요.

---

## Streams API

Streams API를 사용하면 입력 토픽에서 출력 토픽으로 데이터 스트림을 변환할 수 있습니다. 이 API는 클라이언트 라이브러리로 제공되어 별도의 클러스터 없이 스트림 처리 애플리케이션을 구축할 수 있습니다.

### Maven 의존성

Streams API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
    <version>3.9.1</version>
</dependency>
```

### Scala DSL (선택 사항)

Scala 2.13 사용자를 위한 DSL 라이브러리도 제공됩니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams-scala_2.13</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 주요 특징

- Stateless 변환: filter, map, flatMap 등
- Stateful 변환: aggregation, join, windowing 등
- Exactly-once 처리: 정확히 한 번 처리 보장
- 내결함성: 상태 저장소 자동 복구
- 확장성: 파티션 기반 병렬 처리

### 기본 사용 예제

```java
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.util.Properties;

public class SimpleStreams {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
            Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
            Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> source = builder.stream("input-topic");

        source.filter((key, value) -> value.length() > 5)
              .mapValues(value -> value.toUpperCase())
              .to("output-topic");

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // 종료 훅 추가
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
```

### 주요 DSL 연산자

| 연산자 | 유형 | 설명 |
|--------|------|------|
| `filter` | Stateless | 조건에 맞는 레코드만 통과 |
| `map` | Stateless | 키-값 쌍 변환 |
| `flatMap` | Stateless | 하나의 레코드를 여러 레코드로 변환 |
| `groupByKey` | Stateful | 키별로 레코드 그룹화 |
| `aggregate` | Stateful | 그룹화된 레코드 집계 |
| `join` | Stateful | 두 스트림 또는 테이블 조인 |
| `windowedBy` | Stateful | 시간 기반 윈도우 적용 |

자세한 내용은 [Kafka Streams 문서](https://kafka.apache.org/documentation/streams/)를 참조하세요.

---

## Connect API

Connect API를 사용하면 외부 데이터 시스템에서 Kafka로 데이터를 지속적으로 가져오거나(Source Connector), Kafka에서 외부 시스템으로 데이터를 내보내는(Sink Connector) 커넥터를 구현할 수 있습니다.

### Maven 의존성

Connect API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 주요 특징

- Source Connector: 외부 시스템에서 Kafka로 데이터 가져오기
- Sink Connector: Kafka에서 외부 시스템으로 데이터 내보내기
- 분산 모드: 여러 워커에서 커넥터 분산 실행
- 스탠드얼론 모드: 단일 프로세스에서 실행
- 자동 오프셋 관리: 커넥터 진행 상태 자동 추적
- 풍부한 에코시스템: 다양한 사전 구축된 커넥터 제공

### 커넥터 유형

#### Source Connector

외부 시스템에서 Kafka로 데이터를 가져옵니다:

```json
{
  "name": "jdbc-source-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:mysql://localhost:3306/mydb",
    "connection.user": "user",
    "connection.password": "password",
    "topic.prefix": "mysql-",
    "mode": "incrementing",
    "incrementing.column.name": "id"
  }
}
```

#### Sink Connector

Kafka에서 외부 시스템으로 데이터를 내보냅니다:

```json
{
  "name": "elasticsearch-sink-connector",
  "config": {
    "connector.class":
      "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "connection.url": "http://localhost:9200",
    "topics": "my-topic",
    "type.name": "_doc",
    "key.ignore": true
  }
}
```

### 인기 있는 커넥터

| 커넥터 | 유형 | 설명 |
|--------|------|------|
| JDBC | Source/Sink | 관계형 데이터베이스 연동 |
| Elasticsearch | Sink | Elasticsearch로 데이터 전송 |
| HDFS | Sink | Hadoop HDFS에 데이터 저장 |
| S3 | Sink | AWS S3에 데이터 저장 |
| FileStream | Source/Sink | 파일 시스템 연동 |
| Debezium | Source | CDC(Change Data Capture) |

많은 사용자가 사용자 정의 커넥터 코드를 작성할 필요 없이 기존의 사전 구축된 커넥터를 사용합니다.

자세한 내용은 [Kafka Connect 문서](https://kafka.apache.org/documentation/#connect)를 참조하세요.

---

## Admin API

Admin API는 토픽, 브로커, ACL 및 기타 Kafka 객체를 관리하고 검사하는 기능을 지원합니다.

### Maven 의존성

Admin API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 주요 기능

- 토픽 관리: 토픽 생성, 삭제, 설정 변경
- 파티션 관리: 파티션 추가, 재할당
- ACL 관리: 접근 제어 목록 관리
- 설정 관리: 브로커 및 토픽 설정 조회/변경
- Consumer Group 관리: Consumer Group 정보 조회 및 삭제
- 클러스터 정보 조회: 브로커, 토픽, 파티션 메타데이터 조회

### 기본 사용 예제

```java
import org.apache.kafka.clients.admin.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SimpleAdmin {
    public static void main(String[] args)
            throws ExecutionException, InterruptedException {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        try (AdminClient admin = AdminClient.create(props)) {

            // 토픽 목록 조회
            ListTopicsResult topics = admin.listTopics();
            Set<String> topicNames = topics.names().get();
            System.out.println("Topics: " + topicNames);

            // 새 토픽 생성
            NewTopic newTopic = new NewTopic("new-topic", 3, (short) 1);
            admin.createTopics(Collections.singleton(newTopic)).all().get();
            System.out.println("Topic created successfully");

            // 토픽 상세 정보 조회
            DescribeTopicsResult describeResult =
                admin.describeTopics(Collections.singleton("new-topic"));
            Map<String, TopicDescription> descriptions =
                describeResult.allTopicNames().get();

            for (TopicDescription desc : descriptions.values()) {
                System.out.println("Topic: " + desc.name());
                System.out.println("Partitions: " + desc.partitions().size());
            }

            // 토픽 삭제
            admin.deleteTopics(Collections.singleton("new-topic")).all().get();
            System.out.println("Topic deleted successfully");
        }
    }
}
```

### Consumer Group 관리 예제

```java
import org.apache.kafka.clients.admin.*;
import java.util.*;

public class ConsumerGroupAdmin {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        try (AdminClient admin = AdminClient.create(props)) {

            // Consumer Group 목록 조회
            ListConsumerGroupsResult groups = admin.listConsumerGroups();
            Collection<ConsumerGroupListing> listings =
                groups.all().get();

            for (ConsumerGroupListing listing : listings) {
                System.out.println("Group ID: " + listing.groupId());
            }

            // Consumer Group 상세 정보 조회
            DescribeConsumerGroupsResult describeResult =
                admin.describeConsumerGroups(
                    Collections.singleton("my-consumer-group"));

            Map<String, ConsumerGroupDescription> descriptions =
                describeResult.all().get();

            for (ConsumerGroupDescription desc : descriptions.values()) {
                System.out.println("Group: " + desc.groupId());
                System.out.println("State: " + desc.state());
                System.out.println("Members: " + desc.members().size());
            }
        }
    }
}
```

### 주요 Admin 작업

| 작업 | 메서드 | 설명 |
|------|--------|------|
| 토픽 생성 | `createTopics()` | 새 토픽 생성 |
| 토픽 삭제 | `deleteTopics()` | 토픽 삭제 |
| 토픽 조회 | `listTopics()` | 토픽 목록 조회 |
| 토픽 상세 | `describeTopics()` | 토픽 상세 정보 조회 |
| 설정 조회 | `describeConfigs()` | 브로커/토픽 설정 조회 |
| 설정 변경 | `alterConfigs()` | 브로커/토픽 설정 변경 |
| ACL 생성 | `createAcls()` | ACL 규칙 생성 |
| ACL 삭제 | `deleteAcls()` | ACL 규칙 삭제 |
| Consumer Group 조회 | `listConsumerGroups()` | Consumer Group 목록 조회 |
| Consumer Group 삭제 | `deleteConsumerGroups()` | Consumer Group 삭제 |

자세한 내용은 [AdminClient Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/admin/AdminClient.html)을 참조하세요.

---

## 클라이언트 라이브러리

Apache Kafka 프로젝트는 Java 클라이언트 라이브러리만 공식적으로 유지 관리합니다. 다른 프로그래밍 언어용 클라이언트는 독립적인 오픈 소스 프로젝트로 제공됩니다.

### 공식 Java 클라이언트

모든 Java API는 `kafka-clients` 또는 `kafka-streams` 아티팩트에 포함되어 있습니다.

### 커뮤니티 클라이언트

다양한 프로그래밍 언어용 커뮤니티 클라이언트가 있습니다:

| 언어 | 클라이언트 |
|------|-----------|
| Python | confluent-kafka-python, kafka-python |
| Go | confluent-kafka-go, sarama |
| C/C++ | librdkafka |
| .NET | confluent-kafka-dotnet |
| Node.js | kafkajs, node-rdkafka |
| Ruby | ruby-kafka |
| PHP | php-kafka |
| Rust | rust-rdkafka |

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka APIs 문서](https://kafka.apache.org/documentation/#api)
- [KafkaProducer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)
- [KafkaConsumer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)
- [Kafka Streams 문서](https://kafka.apache.org/documentation/streams/)
- [Kafka Connect 문서](https://kafka.apache.org/documentation/#connect)
- [AdminClient Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/admin/AdminClient.html)
