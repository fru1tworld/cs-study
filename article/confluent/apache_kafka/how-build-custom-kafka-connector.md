# 커스텀 Kafka Connector를 구축하는 방법

> **원문:** [How to Build a Custom Kafka Connector](https://www.confluent.io/blog/how-build-custom-kafka-connector/)
>
> **저자:** Shivam Garg
>
> **게시일:** 2025년 3월 6일

---

## 소개

Apache Kafka는 현대 데이터 아키텍처의 기반이 되었으며, 조직이 실시간 데이터 파이프라인과 스트리밍 애플리케이션을 구축할 수 있게 해준다. 이 생태계에서 핵심 컴포넌트 중 하나는 Kafka Connect인데, 이는 Kafka와 다른 시스템 간의 확장 가능하고 안정적인 데이터 스트리밍을 위한 프레임워크이다.

Kafka Connect는 데이터베이스, 키-값 저장소, 검색 인덱스, 파일 시스템 등 다양한 시스템을 위한 풍부한 기성(ready-made) 커넥터 세트를 포함한다. 그러나 때로는 특정 시스템이나 고유한 비즈니스 요구 사항에 맞는 커넥터가 필요할 수 있다. 바로 이때 커스텀 Kafka Connector를 구축하는 것이 중요해진다.

이 블로그 포스트에서는 커스텀 Kafka Connector를 구축하는 전체 프로세스를 안내한다. Kafka Connect의 아키텍처를 이해하고, Source Connector와 Sink Connector를 모두 구현하며, 테스트 및 배포까지 다룬다.

---

## Kafka Connect 개요

Kafka Connect는 Apache Kafka와 다른 데이터 시스템 간에 데이터를 스트리밍하기 위한 도구이다. 대규모 데이터 이동을 단순하고 안정적이며 확장 가능하게 만든다. Kafka Connect는 두 가지 유형의 커넥터를 지원한다:

- **Source Connector:** 소스 시스템에서 데이터를 수집하여 Kafka 토픽에 기록한다. 예를 들어, 데이터베이스의 변경 사항을 캡처하여 Kafka로 스트리밍하는 것이다.
- **Sink Connector:** Kafka 토픽에서 데이터를 소비하여 대상(target) 시스템에 기록한다. 예를 들어, Kafka 토픽의 데이터를 Elasticsearch 인덱스나 데이터 웨어하우스에 기록하는 것이다.

### Kafka Connect의 핵심 개념

커넥터를 구축하기 전에, Kafka Connect의 몇 가지 핵심 개념을 이해하는 것이 중요하다:

- **Connector:** 데이터가 소스 시스템과 Kafka 간(또는 Kafka와 싱크 시스템 간) 어떻게 복사되어야 하는지를 정의하는 고수준 추상화이다. 커넥터는 데이터 복사 작업의 조정을 담당한다.
- **Task:** 커넥터의 작업 구현체이다. 각 커넥터 인스턴스는 실제 데이터 복사를 수행하는 하나 이상의 Task로 구성된다. 이를 통해 병렬 처리와 확장 가능한 데이터 복사가 가능하다.
- **Worker:** Connector와 Task를 실행하는 프로세스이다. Worker에는 두 가지 모드가 있다:
  - **Standalone 모드:** 단일 프로세스에서 커넥터를 실행한다. 개발 및 테스트에 유용하다.
  - **Distributed 모드:** 여러 Worker 프로세스에 걸쳐 커넥터를 실행한다. 프로덕션 환경에서의 내결함성과 확장성을 제공한다.
- **Converter:** Kafka Connect 데이터 형식과 직렬화된 형식(Kafka에 저장되는 바이트) 간의 변환을 담당한다. 일반적인 컨버터로는 JSON, Avro, Protobuf가 있다.
- **Transform (SMT):** 커넥터를 통해 흐르는 각 메시지에 적용되는 간단한 변환이다. 예를 들어, 필드를 추가하거나, 타임스탬프를 변환하거나, 메시지를 라우팅하는 것이다.

---

## 커스텀 Connector를 구축해야 하는 경우

기존 커넥터 중 본인의 사용 사례에 맞는 것이 없을 때 커스텀 커넥터가 필요하다. 일반적인 시나리오는 다음과 같다:

- 커뮤니티나 Confluent Hub에서 지원하지 않는 독점(proprietary) 또는 레거시(legacy) 시스템과의 통합
- 기존 커넥터가 제공하지 않는 특정 데이터 변환이나 비즈니스 로직이 필요한 경우
- 고유한 인증 메커니즘이나 프로토콜을 사용하는 시스템에 연결해야 하는 경우
- 기존 커넥터의 성능을 최적화하거나 특수한 에러 처리가 필요한 경우

---

## 개발 환경 설정

커스텀 커넥터를 구축하기 전에 개발 환경을 설정해야 한다.

### 사전 요구 사항

- **Java 11** 이상 (Kafka Connect는 JVM 위에서 실행된다)
- **Apache Maven** 또는 **Gradle** (빌드 도구)
- **Apache Kafka** (로컬 또는 원격 클러스터)
- IDE (IntelliJ IDEA, Eclipse 등)

### Maven 프로젝트 설정

Maven을 사용하여 새 프로젝트를 생성한다. `pom.xml`에 다음 의존성을 추가한다:

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>connect-api</artifactId>
        <version>3.6.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>connect-transforms</artifactId>
        <version>3.6.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

`scope`를 `provided`로 설정하는 이유는 Kafka Connect 런타임이 이미 이 라이브러리를 제공하기 때문이다. 커넥터 JAR에 이를 포함할 필요가 없다.

---

## Source Connector 구축

Source Connector는 외부 시스템에서 데이터를 읽어 Kafka 토픽에 기록한다. Source Connector를 구축하려면 두 가지 클래스를 구현해야 한다:

1. **`SourceConnector`** - 커넥터의 구성과 Task 관리를 담당
2. **`SourceTask`** - 실제 데이터 읽기 로직을 담당

### SourceConnector 구현

`SourceConnector` 클래스를 확장하여 커넥터를 구현한다:

```java
public class MySourceConnector extends SourceConnector {

    private Map<String, String> configProperties;

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.configProperties = props;
        // 커넥터 초기화 로직
        // 구성 유효성 검사, 리소스 설정 등
    }

    @Override
    public Class<? extends Task> taskClass() {
        return MySourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        // 각 Task에 대한 구성 생성
        // 작업을 여러 Task로 분할하여 병렬 처리 가능
        for (int i = 0; i < maxTasks; i++) {
            Map<String, String> taskConfig = new HashMap<>(configProperties);
            taskConfig.put("task.id", String.valueOf(i));
            taskConfigs.add(taskConfig);
        }
        return taskConfigs;
    }

    @Override
    public void stop() {
        // 리소스 정리 로직
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
            .define("my.source.url",
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "소스 시스템의 URL")
            .define("topic",
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "데이터를 기록할 Kafka 토픽");
    }
}
```

**주요 메서드 설명:**

- **`version()`**: 커넥터의 버전을 반환한다.
- **`start(Map<String, String> props)`**: 커넥터가 시작될 때 호출된다. 구성 속성을 받아 초기화를 수행한다.
- **`taskClass()`**: 이 커넥터의 Task 구현 클래스를 반환한다.
- **`taskConfigs(int maxTasks)`**: 각 Task에 대한 구성을 생성한다. `maxTasks` 파라미터는 프레임워크가 생성할 최대 Task 수를 나타낸다.
- **`stop()`**: 커넥터가 중지될 때 호출된다. 리소스를 정리한다.
- **`config()`**: 커넥터의 구성 정의를 반환한다. 이는 커넥터가 수용하는 구성 속성과 그 유효성 검사 규칙을 정의한다.

### SourceTask 구현

`SourceTask` 클래스를 확장하여 실제 데이터 읽기 로직을 구현한다:

```java
public class MySourceTask extends SourceTask {

    private String sourceUrl;
    private String topic;
    private Long lastOffset;

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        sourceUrl = props.get("my.source.url");
        topic = props.get("topic");

        // 이전 오프셋 복원 (재시작 시 중복 방지)
        Map<String, Object> offset = context.offsetStorageReader()
            .offset(Collections.singletonMap("source", sourceUrl));
        if (offset != null) {
            lastOffset = (Long) offset.get("position");
        } else {
            lastOffset = 0L;
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();

        // 소스 시스템에서 데이터 읽기
        // 이 예제에서는 간단한 폴링 로직을 보여준다
        try {
            List<MyData> newData = fetchDataFromSource(sourceUrl, lastOffset);

            for (MyData data : newData) {
                Map<String, String> sourcePartition =
                    Collections.singletonMap("source", sourceUrl);
                Map<String, Long> sourceOffset =
                    Collections.singletonMap("position", data.getOffset());

                SourceRecord record = new SourceRecord(
                    sourcePartition,       // 소스 파티션
                    sourceOffset,          // 소스 오프셋
                    topic,                 // 대상 토픽
                    Schema.STRING_SCHEMA,  // 키 스키마
                    data.getKey(),         // 키
                    Schema.STRING_SCHEMA,  // 값 스키마
                    data.getValue()        // 값
                );
                records.add(record);
                lastOffset = data.getOffset();
            }
        } catch (Exception e) {
            // 에러 처리
            throw new ConnectException("소스에서 데이터를 가져오는 중 오류 발생", e);
        }

        if (records.isEmpty()) {
            // 새 데이터가 없으면 잠시 대기
            Thread.sleep(1000);
        }

        return records;
    }

    @Override
    public void stop() {
        // 리소스 정리
    }

    private List<MyData> fetchDataFromSource(String url, Long offset) {
        // 소스 시스템에서 실제 데이터를 가져오는 로직
        // HTTP 클라이언트, 데이터베이스 연결 등을 사용할 수 있다
        return new ArrayList<>();
    }
}
```

**주요 메서드 설명:**

- **`start(Map<String, String> props)`**: Task가 시작될 때 호출된다. 구성을 읽고 이전 오프셋을 복원한다.
- **`poll()`**: 프레임워크에 의해 반복적으로 호출되어 소스 시스템에서 새 데이터를 가져온다. `SourceRecord` 리스트를 반환한다.
- **`stop()`**: Task가 중지될 때 호출된다.

### 오프셋 관리

Source Connector에서 가장 중요한 부분 중 하나는 오프셋 관리이다. 오프셋은 소스 시스템에서 어디까지 데이터를 읽었는지를 추적한다. Kafka Connect는 오프셋을 자동으로 관리하며, 커넥터가 재시작될 때 마지막으로 커밋된 오프셋부터 데이터를 다시 읽을 수 있게 해준다.

`SourceRecord`를 생성할 때 두 가지 핵심 정보를 제공해야 한다:

- **Source Partition:** 데이터 소스의 논리적 파티션을 식별한다 (예: 데이터베이스 테이블, 파일 경로).
- **Source Offset:** 해당 파티션 내에서의 위치를 나타낸다 (예: 타임스탬프, 시퀀스 번호, 파일 오프셋).

---

## Sink Connector 구축

Sink Connector는 Kafka 토픽에서 데이터를 읽어 외부 시스템에 기록한다. Sink Connector를 구축하려면 두 가지 클래스를 구현해야 한다:

1. **`SinkConnector`** - 커넥터의 구성과 Task 관리를 담당
2. **`SinkTask`** - 실제 데이터 쓰기 로직을 담당

### SinkConnector 구현

```java
public class MySinkConnector extends SinkConnector {

    private Map<String, String> configProperties;

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.configProperties = props;
        // 커넥터 초기화 로직
    }

    @Override
    public Class<? extends Task> taskClass() {
        return MySinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(new HashMap<>(configProperties));
        }
        return taskConfigs;
    }

    @Override
    public void stop() {
        // 리소스 정리
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
            .define("my.sink.url",
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "대상 시스템의 URL")
            .define("topics",
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "데이터를 소비할 Kafka 토픽(쉼표로 구분)");
    }
}
```

### SinkTask 구현

```java
public class MySinkTask extends SinkTask {

    private String sinkUrl;

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        sinkUrl = props.get("my.sink.url");
        // 대상 시스템 연결 초기화
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        for (SinkRecord record : records) {
            try {
                // 레코드를 대상 시스템에 기록
                writeToSink(record);
            } catch (Exception e) {
                throw new ConnectException(
                    "싱크에 레코드를 기록하는 중 오류 발생: " + record, e);
            }
        }
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 버퍼링된 데이터를 대상 시스템에 플러시
        // 트랜잭션 커밋 등의 로직을 여기에 구현할 수 있다
    }

    @Override
    public void stop() {
        // 리소스 정리 및 연결 종료
    }

    private void writeToSink(SinkRecord record) {
        // 실제 대상 시스템에 데이터를 기록하는 로직
        String key = (String) record.key();
        String value = (String) record.value();
        // HTTP 요청, 데이터베이스 삽입 등
    }
}
```

**주요 메서드 설명:**

- **`put(Collection<SinkRecord> records)`**: Kafka에서 소비한 레코드 배치를 처리한다. 이 메서드에서 대상 시스템에 데이터를 기록하는 로직을 구현한다.
- **`flush(Map<TopicPartition, OffsetAndMetadata> offsets)`**: 오프셋이 커밋되기 전에 호출된다. 버퍼링된 데이터를 대상 시스템에 플러시하는 데 사용할 수 있다.

---

## 구성 및 유효성 검사

커넥터의 구성은 `ConfigDef` 클래스를 사용하여 정의한다. 이 클래스는 구성 속성, 기본값, 유효성 검사 규칙을 선언적으로 정의할 수 있게 해준다.

### 구성 정의

```java
@Override
public ConfigDef config() {
    return new ConfigDef()
        .define("connection.url",
                ConfigDef.Type.STRING,
                ConfigDef.NO_DEFAULT_VALUE,
                ConfigDef.Importance.HIGH,
                "대상 시스템 연결 URL")
        .define("connection.user",
                ConfigDef.Type.STRING,
                "",
                ConfigDef.Importance.MEDIUM,
                "연결 사용자 이름")
        .define("connection.password",
                ConfigDef.Type.PASSWORD,
                "",
                ConfigDef.Importance.MEDIUM,
                "연결 비밀번호")
        .define("batch.size",
                ConfigDef.Type.INT,
                100,
                ConfigDef.Range.atLeast(1),
                ConfigDef.Importance.LOW,
                "배치당 처리할 레코드 수")
        .define("retry.max.times",
                ConfigDef.Type.INT,
                3,
                ConfigDef.Importance.LOW,
                "최대 재시도 횟수");
}
```

### 커스텀 유효성 검사

`ConfigDef.Validator` 인터페이스를 구현하여 커스텀 유효성 검사 로직을 추가할 수 있다:

```java
public class UrlValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(String name, Object value) {
        String url = (String) value;
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ConfigException(name, value,
                "유효한 URL이 아닙니다: " + e.getMessage());
        }
    }
}
```

이를 구성 정의에 적용한다:

```java
.define("connection.url",
        ConfigDef.Type.STRING,
        ConfigDef.NO_DEFAULT_VALUE,
        new UrlValidator(),
        ConfigDef.Importance.HIGH,
        "대상 시스템 연결 URL")
```

### 구성 그룹화 및 문서화

`ConfigDef`는 구성 속성을 그룹으로 정리하고, 권장 값과 종속성을 정의하는 것도 지원한다. 이를 통해 Kafka Connect REST API나 Confluent Control Center에서 사용자 친화적인 구성 인터페이스를 제공할 수 있다.

---

## 스키마 처리

Kafka Connect는 구조화된 데이터를 처리하기 위해 자체 스키마 시스템을 사용한다. 커넥터에서 스키마를 적절히 처리하는 것은 데이터 호환성과 다운스트림 시스템과의 통합을 위해 매우 중요하다.

### 스키마 정의

```java
Schema schema = SchemaBuilder.struct()
    .name("com.example.MyRecord")
    .field("id", Schema.INT64_SCHEMA)
    .field("name", Schema.STRING_SCHEMA)
    .field("email", Schema.OPTIONAL_STRING_SCHEMA)
    .field("created_at", Timestamp.SCHEMA)
    .field("metadata", SchemaBuilder.map(
        Schema.STRING_SCHEMA,
        Schema.STRING_SCHEMA
    ).optional().build())
    .build();
```

### Struct를 사용한 데이터 생성

```java
Struct value = new Struct(schema)
    .put("id", 12345L)
    .put("name", "홍길동")
    .put("email", "hong@example.com")
    .put("created_at", new Date())
    .put("metadata", Collections.singletonMap("source", "api"));

SourceRecord record = new SourceRecord(
    sourcePartition,
    sourceOffset,
    topic,
    Schema.INT64_SCHEMA,  // 키 스키마
    12345L,               // 키
    schema,               // 값 스키마
    value                 // 값
);
```

### 스키마 진화(Schema Evolution)

시간이 지남에 따라 소스 시스템의 스키마가 변경될 수 있다. 커넥터는 이러한 스키마 변경을 우아하게 처리해야 한다. Confluent Schema Registry와 함께 Avro, Protobuf 또는 JSON Schema 컨버터를 사용하면 스키마 진화를 효과적으로 관리할 수 있다.

---

## 에러 처리 및 재시도

프로덕션 환경에서 강건한(robust) 커넥터를 구축하려면 에러 처리와 재시도 메커니즘이 필수적이다.

### 에러 처리 전략

```java
@Override
public void put(Collection<SinkRecord> records) {
    for (SinkRecord record : records) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                writeToSink(record);
                break;  // 성공하면 루프 탈출
            } catch (RetriableException e) {
                retries++;
                if (retries >= maxRetries) {
                    throw new ConnectException(
                        "최대 재시도 횟수 초과: " + record, e);
                }
                try {
                    // 지수 백오프(exponential backoff)
                    Thread.sleep((long) Math.pow(2, retries) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ConnectException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
    }
}
```

### Dead Letter Queue (DLQ) 활용

Kafka Connect는 처리할 수 없는 레코드를 별도의 토픽(Dead Letter Queue)으로 라우팅하는 기능을 내장하고 있다. 이는 커넥터 구성에서 설정할 수 있다:

```json
{
    "errors.tolerance": "all",
    "errors.deadletterqueue.topic.name": "my-connector-dlq",
    "errors.deadletterqueue.topic.replication.factor": 3,
    "errors.deadletterqueue.context.headers.enable": true
}
```

이를 통해 개별 레코드의 실패가 전체 커넥터를 중단시키지 않으면서도, 실패한 레코드를 나중에 분석하고 재처리할 수 있다.

---

## 테스트

커스텀 커넥터를 배포하기 전에 철저한 테스트가 필요하다.

### 단위 테스트(Unit Test)

커넥터의 개별 컴포넌트를 단위 테스트로 검증한다:

```java
@Test
public void testSourceTaskPoll() {
    Map<String, String> props = new HashMap<>();
    props.put("my.source.url", "http://localhost:8080/api");
    props.put("topic", "test-topic");

    MySourceTask task = new MySourceTask();
    task.initialize(mock(SourceTaskContext.class));
    task.start(props);

    List<SourceRecord> records = task.poll();
    assertNotNull(records);
    // 추가 검증 로직
}

@Test
public void testConfig() {
    ConfigDef configDef = new MySourceConnector().config();
    Map<String, ConfigDef.ConfigKey> keys = configDef.configKeys();
    assertTrue(keys.containsKey("my.source.url"));
    assertTrue(keys.containsKey("topic"));
}
```

### 통합 테스트(Integration Test)

Kafka Connect의 `EmbeddedConnectCluster`를 사용하여 통합 테스트를 수행할 수 있다. 이를 통해 실제 Kafka 클러스터에서 커넥터를 실행하고 엔드-투-엔드 동작을 검증할 수 있다:

```java
@Test
public void testConnectorEndToEnd() throws Exception {
    EmbeddedConnectCluster connect = new EmbeddedConnectCluster.Builder()
        .name("test-cluster")
        .build();
    connect.start();

    // 커넥터 구성
    Map<String, String> connectorConfig = new HashMap<>();
    connectorConfig.put("connector.class", MySourceConnector.class.getName());
    connectorConfig.put("tasks.max", "1");
    connectorConfig.put("my.source.url", "http://localhost:8080");
    connectorConfig.put("topic", "test-topic");

    // 커넥터 배포
    connect.configureConnector("my-source-connector", connectorConfig);

    // 커넥터가 시작될 때까지 대기
    connect.assertions().assertConnectorAndAtLeastNumTasksAreRunning(
        "my-source-connector", 1, 30000);

    // 결과 검증
    // ...

    connect.stop();
}
```

### Testcontainers를 활용한 테스트

Testcontainers 라이브러리를 사용하면 Docker 컨테이너에서 Kafka와 외부 시스템을 실행하여 보다 현실적인 테스트 환경을 구성할 수 있다:

```java
@Testcontainers
public class MySinkConnectorIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Test
    public void testSinkConnector() {
        // Kafka 컨테이너를 사용한 통합 테스트
        String bootstrapServers = kafka.getBootstrapServers();
        // ...
    }
}
```

---

## 패키징 및 배포

### 커넥터 패키징

커넥터를 배포하려면 JAR 파일로 패키징해야 한다. Maven을 사용하여 모든 종속성을 포함하는 uber JAR(fat JAR)을 생성한다:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.6.0</version>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

다음 명령으로 빌드한다:

```bash
mvn clean package
```

### Kafka Connect에 배포

커넥터를 Kafka Connect에 배포하려면:

1. 빌드된 JAR 파일을 Kafka Connect의 플러그인 경로에 복사한다:

```bash
# plugin.path 디렉토리에 커넥터 JAR 복사
mkdir -p /usr/share/java/my-custom-connector/
cp target/my-connector-1.0.0-jar-with-dependencies.jar \
   /usr/share/java/my-custom-connector/
```

2. Kafka Connect Worker 구성에서 `plugin.path`를 설정한다:

```properties
plugin.path=/usr/share/java
```

3. Kafka Connect Worker를 재시작한다.

4. REST API를 사용하여 커넥터를 배포한다:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-source-connector",
    "config": {
        "connector.class": "com.example.MySourceConnector",
        "tasks.max": "3",
        "my.source.url": "http://source-system:8080/api",
        "topic": "my-topic"
    }
}'
```

### Confluent Hub에 게시

커넥터를 커뮤니티와 공유하려면 Confluent Hub에 게시할 수 있다. 이를 위해 커넥터 매니페스트 파일을 작성하고 Confluent Hub의 제출 프로세스를 따른다.

---

## 모범 사례 및 팁

커스텀 Kafka Connector를 구축할 때 다음 모범 사례를 따르면 좋다:

### 성능 최적화

- **배치 처리:** 레코드를 개별적으로 처리하는 대신 배치로 묶어서 처리하면 처리량을 크게 향상시킬 수 있다.
- **연결 풀링(Connection Pooling):** 대상 시스템과의 연결을 풀로 관리하여 연결 생성/해제 오버헤드를 줄인다.
- **비동기 처리:** 가능한 경우 비동기 I/O를 사용하여 처리량을 향상시킨다.

### 안정성

- **멱등성(Idempotency):** 가능한 한 멱등적(idempotent) 쓰기를 구현하여 중복 레코드가 문제를 일으키지 않도록 한다.
- **정확히 한 번(Exactly-Once) 의미론:** Kafka Connect의 exactly-once 지원을 활용하여 데이터 정확성을 보장한다.
- **우아한 종료(Graceful Shutdown):** `stop()` 메서드에서 모든 리소스를 적절히 정리하고, 진행 중인 작업이 완료될 수 있도록 한다.

### 모니터링 및 운영

- **JMX 메트릭:** 커넥터에 커스텀 JMX 메트릭을 추가하여 처리량, 지연 시간, 에러율 등을 모니터링할 수 있게 한다.
- **로깅:** SLF4J를 사용하여 적절한 수준의 로그를 남긴다. 디버깅에 유용한 정보를 포함하되, 과도한 로깅은 피한다.
- **상태 확인(Health Check):** Kafka Connect REST API를 통해 커넥터와 Task의 상태를 모니터링한다.

```bash
# 커넥터 상태 확인
curl http://localhost:8083/connectors/my-connector/status

# 커넥터 목록 조회
curl http://localhost:8083/connectors

# 커넥터 재시작
curl -X POST http://localhost:8083/connectors/my-connector/restart
```

### 구성 모범 사례

- 모든 구성 속성에 대해 명확한 문서를 제공한다.
- 합리적인 기본값을 설정한다.
- `ConfigDef.Importance`를 적절히 사용하여 필수 구성과 선택적 구성을 구분한다.
- 민감한 정보(비밀번호, API 키 등)에는 `ConfigDef.Type.PASSWORD`를 사용한다.

---

## 결론

커스텀 Kafka Connector를 구축하는 것은 Kafka Connect 프레임워크에 대한 이해와 몇 가지 핵심 인터페이스의 구현을 필요로 한다. 이 글에서 다룬 내용을 요약하면:

1. **Kafka Connect의 아키텍처** - Connector, Task, Worker, Converter, Transform의 역할과 관계를 이해했다.
2. **Source Connector** - 외부 시스템에서 데이터를 읽어 Kafka에 기록하는 `SourceConnector`와 `SourceTask`를 구현하는 방법을 배웠다.
3. **Sink Connector** - Kafka에서 데이터를 읽어 외부 시스템에 기록하는 `SinkConnector`와 `SinkTask`를 구현하는 방법을 배웠다.
4. **구성 관리** - `ConfigDef`를 사용하여 구성을 정의하고 유효성 검사를 수행하는 방법을 살펴보았다.
5. **스키마 처리** - Kafka Connect의 스키마 시스템을 활용하여 구조화된 데이터를 처리하는 방법을 다루었다.
6. **에러 처리** - 재시도, 지수 백오프, Dead Letter Queue를 활용한 강건한 에러 처리 전략을 구현했다.
7. **테스트** - 단위 테스트, 통합 테스트, Testcontainers를 활용한 테스트 방법을 알아보았다.
8. **패키징 및 배포** - 커넥터를 JAR로 패키징하고 Kafka Connect에 배포하는 프로세스를 다루었다.

Kafka Connect의 강력한 프레임워크를 활용하면 거의 모든 시스템과 Kafka를 통합할 수 있다. 커넥터를 구축할 때는 성능, 안정성, 운영 용이성을 항상 고려하고, 위에서 설명한 모범 사례를 따르는 것이 좋다.

더 많은 정보와 예제는 [Apache Kafka Connect 공식 문서](https://kafka.apache.org/documentation/#connect)와 [Confluent 개발자 문서](https://docs.confluent.io/platform/current/connect/devguide.html)에서 확인할 수 있다.
