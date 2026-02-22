# Kafka Connect 설정

> 이 문서는 Apache Kafka 공식 문서의 "Kafka Connect Configuration" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#connectconfigs

## 개요

Kafka Connect는 Apache Kafka와 외부 시스템 간에 데이터를 스트리밍하기 위한 확장 가능하고 신뢰성 있는 프레임워크입니다. 이 문서에서는 Kafka Connect Worker와 커넥터를 구성하는 데 필요한 설정 옵션들을 설명합니다.

---

## Worker 설정

Kafka Connect Worker는 분산 모드(Distributed Mode)와 독립 실행 모드(Standalone Mode)로 실행할 수 있습니다. 다음은 Worker 설정에 필요한 주요 파라미터들입니다.

### 높은 중요도 (High Importance)

#### config.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: 커넥터 설정이 저장되는 Kafka 토픽의 이름입니다. 분산 모드에서 모든 Worker가 이 토픽을 통해 설정 정보를 공유합니다.

#### group.id

- 타입: string
- 기본값: (필수)
- 설명: 이 Worker가 속한 Connect 클러스터 그룹을 식별하는 고유 문자열입니다. 동일한 `group.id`를 가진 모든 Worker는 동일한 클러스터로 간주됩니다.

#### key.converter

- 타입: class
- 기본값: (필수)
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 키를 변환하는 데 사용되는 Converter 클래스입니다. 일반적으로 `org.apache.kafka.connect.json.JsonConverter` 또는 `io.confluent.connect.avro.AvroConverter`가 사용됩니다.

#### value.converter

- 타입: class
- 기본값: (필수)
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 값을 변환하는 데 사용되는 Converter 클래스입니다.

#### offset.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: Source 커넥터의 오프셋이 저장되는 Kafka 토픽의 이름입니다. 이 토픽은 Source 커넥터가 처리한 위치를 추적하는 데 사용됩니다.

#### status.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: 커넥터와 태스크 상태가 저장되는 Kafka 토픽의 이름입니다. 이 토픽을 통해 클러스터의 모든 Worker가 커넥터 상태를 공유합니다.

#### bootstrap.servers

- 타입: list
- 기본값: localhost:9092
- 설명: Kafka 클러스터에 대한 초기 연결을 설정하는 데 사용할 호스트/포트 쌍의 목록입니다. 쉼표로 구분하여 여러 브로커를 지정할 수 있습니다.

```properties
bootstrap.servers=broker1:9092,broker2:9092,broker3:9092
```

#### exactly.once.source.support

- 타입: string
- 유효값: DISABLED, ENABLED, PREPARING
- 기본값: disabled
- 설명: Source 커넥터에 대한 정확히 한 번(Exactly-Once) 시맨틱스를 활성화합니다. `ENABLED`로 설정하면 Source 커넥터가 트랜잭션을 사용하여 메시지 중복을 방지합니다.

---

### 중간 중요도 (Medium Importance)

#### heartbeat.interval.ms

- 타입: int
- 기본값: 3000 (3초)
- 설명: 그룹 코디네이터에게 하트비트를 보내는 예상 간격입니다. 하트비트는 Worker의 세션을 활성 상태로 유지하고 새 구성원이 그룹에 참여하거나 떠날 때 리밸런싱을 촉진하는 데 사용됩니다.

#### rebalance.timeout.ms

- 타입: int
- 기본값: 60000 (1분)
- 설명: 리밸런스가 시작된 후 각 Worker가 그룹에 참여하는 데 허용되는 최대 시간입니다. 이 시간이 지나면 응답하지 않는 Worker는 그룹에서 제거됩니다.

#### session.timeout.ms

- 타입: int
- 기본값: 10000 (10초)
- 설명: Worker 실패를 감지하는 데 사용되는 타임아웃입니다. Worker가 이 시간 내에 하트비트를 보내지 않으면 그룹에서 제거되고 리밸런스가 트리거됩니다.

#### client.dns.lookup

- 타입: string
- 유효값: use_all_dns_ips, resolve_canonical_bootstrap_servers_only
- 기본값: use_all_dns_ips
- 설명: 클라이언트가 DNS 조회를 사용하는 방법을 제어합니다. `use_all_dns_ips`를 사용하면 DNS에서 반환된 각 IP 주소에 순차적으로 연결을 시도합니다.

#### connections.max.idle.ms

- 타입: long
- 기본값: 540000 (9분)
- 설명: 유휴 연결을 닫기 전까지 대기하는 시간입니다.

#### connector.client.config.override.policy

- 타입: string
- 기본값: All
- 설명: 커넥터가 재정의할 수 있는 클라이언트 구성을 정의합니다. `All`은 모든 클라이언트 설정을 재정의할 수 있음을 의미합니다.

#### receive.buffer.bytes

- 타입: int
- 기본값: 32768 (32 KiB)
- 유효값: [-1,...]
- 설명: 데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF)의 크기입니다. -1이면 OS 기본값을 사용합니다.

#### request.timeout.ms

- 타입: int
- 기본값: 40000 (40초)
- 설명: 클라이언트가 요청에 대한 응답을 기다리는 최대 시간입니다.

#### send.buffer.bytes

- 타입: int
- 기본값: 131072 (128 KiB)
- 설명: 데이터를 전송할 때 사용할 TCP 전송 버퍼(SO_SNDBUF)의 크기입니다.

#### security.protocol

- 타입: string
- 유효값: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
- 기본값: PLAINTEXT
- 설명: 브로커와 통신하는 데 사용되는 프로토콜입니다.

#### worker.sync.timeout.ms

- 타입: int
- 기본값: 3000 (3초)
- 설명: Worker가 다른 Worker와 동기화하는 데 사용하는 타임아웃입니다.

#### worker.unsync.backoff.ms

- 타입: int
- 기본값: 300000 (5분)
- 설명: Worker가 다른 Worker와 동기화되지 않았을 때 재시도 전에 대기하는 시간입니다.

---

### 낮은 중요도 (Low Importance)

#### access.control.allow.methods

- 타입: string
- 기본값: ""
- 설명: REST API에서 교차 출처 요청에 허용되는 HTTP 메서드를 설정합니다.

#### access.control.allow.origin

- 타입: string
- 기본값: ""
- 설명: REST API에서 교차 출처 요청에 대해 허용되는 출처를 설정합니다.

#### admin.listeners

- 타입: list
- 기본값: null
- 설명: 관리 REST API가 수신할 URI 목록입니다.

#### client.id

- 타입: string
- 기본값: ""
- 설명: 요청 시 서버에 전달되는 클라이언트 ID 문자열입니다. 로깅 및 모니터링에 유용합니다.

#### config.providers

- 타입: list
- 기본값: ""
- 설명: ConfigProvider 클래스의 쉼표로 구분된 이름 목록입니다. 외부 소스에서 설정 값을 로드하는 데 사용됩니다.

#### config.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 설정 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

#### connect.protocol

- 타입: string
- 유효값: eager, compatible, sessioned
- 기본값: sessioned
- 설명: Connect 프로토콜의 버전을 지정합니다.

#### header.converter

- 타입: class
- 기본값: org.apache.kafka.connect.storage.SimpleHeaderConverter
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 헤더를 변환하는 데 사용되는 Converter 클래스입니다.

#### inter.worker.key.generation.algorithm

- 타입: string
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 내부 요청 키 생성 알고리즘입니다.

#### inter.worker.key.size

- 타입: int
- 기본값: null
- 설명: Worker 간 통신에 사용되는 내부 요청 키의 크기(비트)입니다.

#### inter.worker.key.ttl.ms

- 타입: int
- 기본값: 3600000 (1시간)
- 유효값: [0,...,2147483647]
- 설명: Worker 간 통신에 사용되는 내부 요청 키의 TTL(Time To Live)입니다.

#### inter.worker.signature.algorithm

- 타입: string
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 서명 알고리즘입니다.

#### inter.worker.verification.algorithms

- 타입: list
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 검증 알고리즘 목록입니다.

#### listeners

- 타입: list
- 기본값: http://:8083
- 설명: REST API가 수신할 쉼표로 구분된 URI 목록입니다.

```properties
listeners=http://0.0.0.0:8083,https://0.0.0.0:8084
```

#### metadata.max.age.ms

- 타입: long
- 기본값: 300000 (5분)
- 설명: 새 브로커나 파티션을 사전에 발견하기 위해 파티션 리더십 변경이 없어도 메타데이터를 강제로 새로 고치는 기간입니다.

#### metric.reporters

- 타입: list
- 기본값: ""
- 설명: 메트릭 리포터로 사용할 클래스 목록입니다.

#### metrics.num.samples

- 타입: int
- 기본값: 2
- 유효값: [1,...]
- 설명: 메트릭 계산을 위해 유지되는 샘플 수입니다.

#### metrics.recording.level

- 타입: string
- 기본값: INFO
- 유효값: INFO, DEBUG
- 설명: 메트릭의 가장 높은 기록 수준입니다.

#### metrics.sample.window.ms

- 타입: long
- 기본값: 30000 (30초)
- 설명: 메트릭 샘플이 계산되는 시간 창입니다.

#### offset.flush.interval.ms

- 타입: long
- 기본값: 60000 (1분)
- 설명: 태스크에 대한 오프셋을 커밋하려고 시도하는 간격입니다.

#### offset.flush.timeout.ms

- 타입: long
- 기본값: 5000 (5초)
- 설명: 레코드가 플러시되기를 기다리는 최대 시간(밀리초)이며, 파티션 오프셋 데이터가 오프셋 저장소에 커밋되기를 기다리는 시간입니다.

#### offset.storage.partitions

- 타입: int
- 기본값: 25
- 설명: 오프셋 저장소 토픽을 생성할 때 사용할 파티션 수입니다.

#### offset.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 오프셋 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

#### plugin.discovery

- 타입: string
- 기본값: hybrid_warn
- 유효값: ONLY_SCAN, SERVICE_LOAD, HYBRID_WARN, HYBRID_FAIL
- 설명: 플러그인 검색 방법을 제어합니다. `SERVICE_LOAD`는 Java ServiceLoader 메커니즘을 사용하여 플러그인을 검색합니다.

#### plugin.path

- 타입: list
- 기본값: null
- 설명: 플러그인(커넥터, 컨버터, 변환)이 포함된 경로의 쉼표로 구분된 목록입니다.

```properties
plugin.path=/usr/share/kafka-connect-jdbc,/usr/share/kafka-connect-elasticsearch
```

#### reconnect.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 설명: 브로커에 대한 재연결 시도 시 대기하는 최대 시간입니다.

#### reconnect.backoff.ms

- 타입: long
- 기본값: 50
- 설명: 브로커에 대한 재연결 시도 전 대기하는 기본 시간입니다.

#### response.http.headers.config

- 타입: string
- 기본값: ""
- 설명: REST API 응답에 추가할 HTTP 헤더를 설정합니다.

#### rest.advertised.host.name

- 타입: string
- 기본값: null
- 설명: 다른 Worker에게 광고할 호스트 이름입니다.

#### rest.advertised.listener

- 타입: string
- 기본값: null
- 설명: 다른 Worker에게 광고할 리스너(HTTP 또는 HTTPS)입니다.

#### rest.advertised.port

- 타입: int
- 기본값: null
- 설명: 다른 Worker에게 광고할 포트입니다.

#### rest.extension.classes

- 타입: list
- 기본값: ""
- 설명: REST 리소스를 확장하는 데 사용되는 ConnectRestExtension 클래스 목록입니다.

#### retry.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 설명: 오류가 발생한 요청을 재시도할 때 대기하는 최대 시간입니다.

#### retry.backoff.ms

- 타입: long
- 기본값: 100
- 설명: 오류가 발생한 요청을 재시도하기 전에 대기하는 기본 시간입니다.

#### sasl.mechanism

- 타입: string
- 기본값: GSSAPI
- 설명: SASL 연결에 사용되는 SASL 메커니즘입니다.

#### scheduled.rebalance.max.delay.ms

- 타입: int
- 기본값: 300000 (5분)
- 유효값: [0,...,2147483647]
- 설명: 스케줄된 리밸런스 전에 대기할 최대 지연 시간입니다.

#### socket.connection.setup.timeout.max.ms

- 타입: long
- 기본값: 30000 (30초)
- 설명: 소켓 연결 설정 시 대기하는 최대 시간입니다.

#### socket.connection.setup.timeout.ms

- 타입: long
- 기본값: 10000 (10초)
- 설명: 소켓 연결 설정 시 대기하는 기본 시간입니다.

#### status.storage.partitions

- 타입: int
- 기본값: 5
- 설명: 상태 저장소 토픽을 생성할 때 사용할 파티션 수입니다.

#### status.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 상태 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

#### task.shutdown.graceful.timeout.ms

- 타입: long
- 기본값: 5000 (5초)
- 설명: 태스크가 정상적으로 종료되기를 기다리는 시간입니다.

#### topic.creation.enable

- 타입: boolean
- 기본값: true
- 설명: Source 커넥터가 토픽을 자동으로 생성할 수 있도록 허용할지 여부입니다.

#### topic.tracking.allow.reset

- 타입: boolean
- 기본값: true
- 설명: 커넥터의 활성 토픽 세트를 재설정할 수 있도록 허용할지 여부입니다.

#### topic.tracking.enable

- 타입: boolean
- 기본값: true
- 설명: 커넥터당 활성 토픽 집합 추적을 활성화할지 여부입니다.

---

## SSL 설정

Kafka Connect와 브로커 간의 보안 통신을 위한 SSL 설정입니다.

### 높은 중요도

#### ssl.key.password

- 타입: password
- 기본값: null
- 설명: 키 저장소 파일의 개인 키 비밀번호입니다.

#### ssl.keystore.certificate.chain

- 타입: password
- 기본값: null
- 설명: PEM 형식의 인증서 체인입니다.

#### ssl.keystore.key

- 타입: password
- 기본값: null
- 설명: PEM 형식의 개인 키입니다.

#### ssl.keystore.location

- 타입: string
- 기본값: null
- 설명: 키 저장소 파일의 위치입니다.

#### ssl.keystore.password

- 타입: password
- 기본값: null
- 설명: 키 저장소 파일의 저장소 비밀번호입니다.

#### ssl.truststore.certificates

- 타입: password
- 기본값: null
- 설명: PEM 형식의 신뢰할 수 있는 인증서입니다.

#### ssl.truststore.location

- 타입: string
- 기본값: null
- 설명: 신뢰 저장소 파일의 위치입니다.

#### ssl.truststore.password

- 타입: password
- 기본값: null
- 설명: 신뢰 저장소 파일의 비밀번호입니다.

### 중간/낮은 중요도

#### ssl.enabled.protocols

- 타입: list
- 기본값: TLSv1.2
- 설명: SSL 연결에 활성화된 프로토콜 목록입니다.

#### ssl.keystore.type

- 타입: string
- 기본값: JKS
- 유효값: JKS, PKCS12, PEM
- 설명: 키 저장소 파일의 파일 형식입니다.

#### ssl.protocol

- 타입: string
- 기본값: TLSv1.2
- 설명: SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다.

#### ssl.truststore.type

- 타입: string
- 기본값: JKS
- 설명: 신뢰 저장소 파일의 파일 형식입니다.

#### ssl.cipher.suites

- 타입: list
- 기본값: null
- 설명: 암호 스위트의 목록입니다.

#### ssl.client.auth

- 타입: string
- 기본값: none
- 유효값: required, requested, none
- 설명: 클라이언트 인증 구성입니다.

#### ssl.endpoint.identification.algorithm

- 타입: string
- 기본값: https
- 설명: 서버 인증서를 사용하여 서버 호스트 이름을 확인하는 엔드포인트 식별 알고리즘입니다.

#### ssl.keymanager.algorithm

- 타입: string
- 기본값: SunX509
- 설명: SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다.

#### ssl.trustmanager.algorithm

- 타입: string
- 기본값: PKIX
- 설명: SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다.

---

## Source 커넥터 설정

Source 커넥터는 외부 시스템에서 Kafka로 데이터를 가져오는 커넥터입니다.

### 높은 중요도

#### name

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터에 사용할 전역적으로 고유한 이름입니다.

#### connector.class

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터의 클래스 이름 또는 별칭입니다. 정규화된 클래스 이름이거나 플러그인에 정의된 별칭이어야 합니다.

#### tasks.max

- 타입: int
- 기본값: 1
- 유효값: [1,...]
- 설명: 이 커넥터에 대해 생성할 최대 태스크 수입니다. 커넥터는 병렬 처리를 위해 여러 태스크를 생성할 수 있습니다.

### 중간 중요도

#### exactly.once.support

- 타입: string
- 기본값: requested
- 유효값: REQUIRED, REQUESTED
- 설명: 커넥터에서 정확히 한 번(Exactly-Once) 지원 수준을 요청합니다.

#### transaction.boundary

- 타입: string
- 기본값: poll
- 유효값: INTERVAL, POLL, CONNECTOR
- 설명: 커넥터가 생성하는 트랜잭션 경계를 정의합니다.

#### errors.tolerance

- 타입: string
- 기본값: none
- 유효값: none, all
- 설명: 오류 허용 수준입니다. `none`은 오류 발생 시 즉시 실패하고, `all`은 오류를 무시하고 계속 진행합니다.

#### errors.retry.timeout

- 타입: long
- 기본값: 0
- 설명: 실패한 작업을 재시도하는 최대 시간(밀리초)입니다. 0이면 재시도하지 않습니다.

### 낮은 중요도

#### transaction.boundary.interval.ms

- 타입: long
- 기본값: null
- 유효값: [0,...]
- 설명: `transaction.boundary`가 `INTERVAL`로 설정된 경우 트랜잭션 간 간격입니다.

#### offsets.storage.topic

- 타입: string
- 기본값: null
- 설명: 이 커넥터의 오프셋을 저장할 개별 토픽의 이름입니다. null이면 Worker의 기본 오프셋 저장소 토픽이 사용됩니다.

---

## Sink 커넥터 설정

Sink 커넥터는 Kafka에서 외부 시스템으로 데이터를 내보내는 커넥터입니다.

### 높은 중요도

#### name

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터에 사용할 전역적으로 고유한 이름입니다.

#### connector.class

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터의 클래스 이름 또는 별칭입니다.

#### tasks.max

- 타입: int
- 기본값: 1
- 유효값: [1,...]
- 설명: 이 커넥터에 대해 생성할 최대 태스크 수입니다.

#### topics

- 타입: list
- 기본값: ""
- 설명: 소비할 토픽의 쉼표로 구분된 목록입니다.

```properties
topics=topic1,topic2,topic3
```

#### topics.regex

- 타입: string
- 기본값: ""
- 설명: 소비할 토픽을 지정하는 정규 표현식입니다. `topics`와 함께 사용할 수 없습니다.

```properties
topics.regex=my-topic-.*
```

### 중간 중요도

#### errors.tolerance

- 타입: string
- 기본값: none
- 유효값: none, all
- 설명: 오류 허용 수준입니다.

#### errors.deadletterqueue.topic.name

- 타입: string
- 기본값: ""
- 설명: 데드 레터 큐로 사용할 토픽의 이름입니다. 처리에 실패한 레코드가 이 토픽으로 전송됩니다.

```properties
errors.deadletterqueue.topic.name=my-connector-dlq
```

#### errors.deadletterqueue.topic.replication.factor

- 타입: short
- 기본값: 3
- 설명: 데드 레터 큐 토픽을 생성할 때 사용할 복제 계수입니다.

#### errors.deadletterqueue.context.headers.enable

- 타입: boolean
- 기본값: false
- 설명: `true`로 설정하면 오류 컨텍스트 헤더를 데드 레터 큐의 메시지에 추가합니다. 이 헤더에는 오류 원인에 대한 정보가 포함됩니다.

---

## 설정 예제

### 분산 모드 Worker 설정 (connect-distributed.properties)

```properties
# 필수 설정
bootstrap.servers=localhost:9092
group.id=connect-cluster

# 커넥터 설정 저장 토픽
config.storage.topic=connect-configs
config.storage.replication.factor=3

# 오프셋 저장 토픽
offset.storage.topic=connect-offsets
offset.storage.replication.factor=3
offset.storage.partitions=25

# 상태 저장 토픽
status.storage.topic=connect-status
status.storage.replication.factor=3
status.storage.partitions=5

# 컨버터 설정
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false

# 플러그인 경로
plugin.path=/usr/share/kafka-connect-plugins

# REST API 설정
listeners=http://0.0.0.0:8083

# 오프셋 플러시 설정
offset.flush.interval.ms=10000
```

### 독립 실행 모드 Worker 설정 (connect-standalone.properties)

```properties
bootstrap.servers=localhost:9092

key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false

# 오프셋 저장 파일 (독립 실행 모드)
offset.storage.file.filename=/tmp/connect.offsets

# 플러그인 경로
plugin.path=/usr/share/kafka-connect-plugins
```

### Source 커넥터 예제 (FileStream Source)

```properties
name=local-file-source
connector.class=FileStreamSource
tasks.max=1
file=/tmp/test.txt
topic=connect-test
```

### Sink 커넥터 예제 (FileStream Sink)

```properties
name=local-file-sink
connector.class=FileStreamSink
tasks.max=1
file=/tmp/test.sink.txt
topics=connect-test
```

### 오류 처리가 포함된 Sink 커넥터 예제

```properties
name=jdbc-sink
connector.class=io.confluent.connect.jdbc.JdbcSinkConnector
tasks.max=3
topics=orders
connection.url=jdbc:postgresql://localhost:5432/mydb
connection.user=myuser
connection.password=mypassword

# 오류 허용 및 데드 레터 큐 설정
errors.tolerance=all
errors.deadletterqueue.topic.name=jdbc-sink-dlq
errors.deadletterqueue.topic.replication.factor=3
errors.deadletterqueue.context.headers.enable=true
```

---

## 요약

| 카테고리 | 주요 설정 | 설명 |
|---------|----------|------|
| 클러스터 | `group.id` | Connect 클러스터 식별자 |
| 연결 | `bootstrap.servers` | Kafka 브로커 연결 정보 |
| 직렬화 | `key.converter`, `value.converter` | 메시지 변환기 |
| 저장소 | `config.storage.topic`, `offset.storage.topic`, `status.storage.topic` | 내부 저장소 토픽 |
| 플러그인 | `plugin.path` | 커넥터 플러그인 경로 |
| REST API | `listeners` | REST API 수신 주소 |
| 보안 | `security.protocol`, SSL/SASL 설정 | 보안 통신 설정 |
| 오류 처리 | `errors.tolerance`, `errors.deadletterqueue.*` | 오류 허용 및 DLQ |

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Connect 사용자 가이드](https://kafka.apache.org/documentation/#connect)
- [Kafka Connect REST API](https://kafka.apache.org/documentation/#connect_rest)
- [Confluent Kafka Connect 문서](https://docs.confluent.io/platform/current/connect/index.html)
