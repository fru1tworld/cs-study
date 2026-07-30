# Kafka Connect·Streams·Admin Client 설정

## Kafka Connect 설정

> 원본: https://kafka.apache.org/documentation/#connectconfigs

### 개요

Kafka Connect는 Apache Kafka와 외부 시스템 간에 데이터를 스트리밍하기 위한 확장 가능하고 신뢰성 있는 프레임워크입니다. 여기서는 Kafka Connect Worker와 커넥터를 구성하는 데 필요한 설정 옵션들을 설명합니다.

---

### Worker 설정

Kafka Connect Worker는 분산 모드(Distributed Mode)와 독립 실행 모드(Standalone Mode)로 실행할 수 있습니다. 다음은 Worker 설정에 필요한 주요 파라미터들입니다.

#### 높은 중요도 (High Importance)

##### config.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: 커넥터 설정이 저장되는 Kafka 토픽의 이름입니다. 분산 모드에서 모든 Worker가 이 토픽을 통해 설정 정보를 공유합니다.

##### group.id

- 타입: string
- 기본값: (필수)
- 설명: 이 Worker가 속한 Connect 클러스터 그룹을 식별하는 고유 문자열입니다. 동일한 `group.id`를 가진 모든 Worker는 동일한 클러스터로 간주됩니다.

##### key.converter

- 타입: class
- 기본값: (필수)
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 키를 변환하는 데 사용되는 Converter 클래스입니다. 일반적으로 `org.apache.kafka.connect.json.JsonConverter` 또는 `io.confluent.connect.avro.AvroConverter`가 사용됩니다.

##### value.converter

- 타입: class
- 기본값: (필수)
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 값을 변환하는 데 사용되는 Converter 클래스입니다.

##### offset.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: Source 커넥터의 오프셋이 저장되는 Kafka 토픽의 이름입니다. 이 토픽은 Source 커넥터가 처리한 위치를 추적하는 데 사용됩니다.

##### status.storage.topic

- 타입: string
- 기본값: (필수)
- 설명: 커넥터와 태스크 상태가 저장되는 Kafka 토픽의 이름입니다. 이 토픽을 통해 클러스터의 모든 Worker가 커넥터 상태를 공유합니다.

##### bootstrap.servers

- 타입: list
- 기본값: localhost:9092
- 설명: Kafka 클러스터에 대한 초기 연결을 설정하는 데 사용할 호스트/포트 쌍의 목록입니다. 쉼표로 구분하여 여러 브로커를 지정할 수 있습니다.

```properties
bootstrap.servers=broker1:9092,broker2:9092,broker3:9092
```

##### exactly.once.source.support

- 타입: string
- 유효값: disabled, preparing, enabled
- 기본값: disabled
- 설명: Source 커넥터에 대한 정확히 한 번(Exactly-Once) 시맨틱스를 활성화합니다. `ENABLED`로 설정하면 Source 커넥터가 트랜잭션을 사용하여 메시지 중복을 방지합니다.

---

#### 중간 중요도 (Medium Importance)

##### heartbeat.interval.ms

- 타입: int
- 기본값: 3000 (3초)
- 설명: 그룹 코디네이터에 하트비트를 보내는 예상 주기입니다. 하트비트는 Worker 세션을 활성 상태로 유지하고, 새 구성원이 그룹에 참여하거나 떠날 때 리밸런싱을 촉진합니다.

##### rebalance.timeout.ms

- 타입: int
- 기본값: 60000 (1분)
- 설명: 리밸런스가 시작된 후 각 Worker가 그룹에 참여하는 데 허용되는 최대 시간입니다. 이 시간이 지나면 응답하지 않는 Worker는 그룹에서 제거됩니다.

##### session.timeout.ms

- 타입: int
- 기본값: 10000 (10초)
- 설명: Worker 실패를 감지하는 데 사용되는 타임아웃입니다. Worker가 이 시간 내에 하트비트를 보내지 않으면 그룹에서 제거되고 리밸런스가 트리거됩니다.

##### client.dns.lookup

- 타입: string
- 유효값: use_all_dns_ips, resolve_canonical_bootstrap_servers_only
- 기본값: use_all_dns_ips
- 설명: 클라이언트의 DNS 조회 방식을 제어합니다. `use_all_dns_ips`로 설정하면 DNS에서 반환된 각 IP 주소에 순차적으로 연결을 시도합니다.

##### connections.max.idle.ms

- 타입: long
- 기본값: 540000 (9분)
- 설명: 유휴 연결을 닫기 전까지 대기하는 시간입니다.

##### connector.client.config.override.policy

- 타입: string
- 기본값: All
- 설명: 커넥터가 재정의할 수 있는 클라이언트 구성을 정의합니다. `All`은 모든 클라이언트 설정을 재정의할 수 있음을 의미합니다.

##### receive.buffer.bytes

- 타입: int
- 기본값: 32768 (32 KiB)
- 유효값: [-1,...]
- 설명: 데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF)의 크기입니다. -1이면 OS 기본값을 사용합니다.

##### request.timeout.ms

- 타입: int
- 기본값: 40000 (40초)
- 설명: 클라이언트가 요청에 대한 응답을 기다리는 최대 시간입니다.

##### send.buffer.bytes

- 타입: int
- 기본값: 131072 (128 KiB)
- 설명: 데이터를 전송할 때 사용할 TCP 전송 버퍼(SO_SNDBUF)의 크기입니다.

##### security.protocol

- 타입: string
- 유효값: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
- 기본값: PLAINTEXT
- 설명: 브로커와 통신하는 데 사용되는 프로토콜입니다.

##### worker.sync.timeout.ms

- 타입: int
- 기본값: 3000 (3초)
- 설명: Worker가 다른 Worker와 동기화하는 데 사용하는 타임아웃입니다.

##### worker.unsync.backoff.ms

- 타입: int
- 기본값: 300000 (5분)
- 설명: Worker가 다른 Worker와 동기화되지 않았을 때 재시도 전에 대기하는 시간입니다.

---

#### 낮은 중요도 (Low Importance)

##### access.control.allow.methods

- 타입: string
- 기본값: ""
- 설명: REST API에서 교차 출처 요청에 허용되는 HTTP 메서드를 설정합니다.

##### access.control.allow.origin

- 타입: string
- 기본값: ""
- 설명: REST API에서 교차 출처 요청에 대해 허용되는 출처를 설정합니다.

##### admin.listeners

- 타입: list
- 기본값: null
- 설명: 관리 REST API가 수신할 URI 목록입니다.

##### client.id

- 타입: string
- 기본값: ""
- 설명: 요청 시 서버에 전달되는 클라이언트 ID 문자열입니다. 로깅 및 모니터링에 유용합니다.

##### config.providers

- 타입: list
- 기본값: ""
- 설명: ConfigProvider 클래스의 쉼표로 구분된 이름 목록입니다. 외부 소스에서 설정 값을 로드하는 데 사용됩니다.

##### config.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 설정 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

##### connect.protocol

- 타입: string
- 유효값: eager, compatible, sessioned
- 기본값: sessioned
- 설명: Connect 프로토콜의 버전을 지정합니다.

##### header.converter

- 타입: class
- 기본값: org.apache.kafka.connect.storage.SimpleHeaderConverter
- 설명: Kafka Connect 형식과 Kafka에 기록되는 직렬화된 형식 간에 헤더를 변환하는 데 사용되는 Converter 클래스입니다.

##### inter.worker.key.generation.algorithm

- 타입: string
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 내부 요청 키 생성 알고리즘입니다.

##### inter.worker.key.size

- 타입: int
- 기본값: null
- 설명: Worker 간 통신에 사용되는 내부 요청 키의 크기(비트)입니다.

##### inter.worker.key.ttl.ms

- 타입: int
- 기본값: 3600000 (1시간)
- 유효값: [0,...,2147483647]
- 설명: Worker 간 통신에 사용되는 내부 요청 키의 TTL(Time To Live)입니다.

##### inter.worker.signature.algorithm

- 타입: string
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 서명 알고리즘입니다.

##### inter.worker.verification.algorithms

- 타입: list
- 기본값: HmacSHA256
- 설명: Worker 간 통신에 사용되는 검증 알고리즘 목록입니다.

##### listeners

- 타입: list
- 기본값: http://:8083
- 설명: REST API가 수신할 쉼표로 구분된 URI 목록입니다.

```properties
listeners=http://0.0.0.0:8083,https://0.0.0.0:8084
```

##### metadata.max.age.ms

- 타입: long
- 기본값: 300000 (5분)
- 설명: 파티션 리더십 변경이 없어도 새 브로커나 파티션을 미리 감지하기 위해 메타데이터를 강제로 갱신하는 주기입니다.

##### metric.reporters

- 타입: list
- 기본값: ""
- 설명: 메트릭 리포터로 사용할 클래스 목록입니다.

##### metrics.num.samples

- 타입: int
- 기본값: 2
- 유효값: [1,...]
- 설명: 메트릭 계산을 위해 유지되는 샘플 수입니다.

##### metrics.recording.level

- 타입: string
- 기본값: INFO
- 유효값: INFO, DEBUG
- 설명: 메트릭의 가장 높은 기록 수준입니다.

##### metrics.sample.window.ms

- 타입: long
- 기본값: 30000 (30초)
- 설명: 메트릭 샘플이 계산되는 시간 창입니다.

##### offset.flush.interval.ms

- 타입: long
- 기본값: 60000 (1분)
- 설명: 태스크에 대한 오프셋을 커밋하려고 시도하는 간격입니다.

##### offset.flush.timeout.ms

- 타입: long
- 기본값: 5000 (5초)
- 설명: 레코드가 플러시되고 파티션 오프셋 데이터가 오프셋 저장소에 커밋되기까지 기다리는 최대 시간(밀리초)입니다.

##### offset.storage.partitions

- 타입: int
- 기본값: 25
- 설명: 오프셋 저장소 토픽을 생성할 때 사용할 파티션 수입니다.

##### offset.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 오프셋 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

##### plugin.discovery

- 타입: string
- 기본값: hybrid_warn
- 유효값: ONLY_SCAN, SERVICE_LOAD, HYBRID_WARN, HYBRID_FAIL
- 설명: 플러그인 검색 방법을 제어합니다. `SERVICE_LOAD`는 Java ServiceLoader 메커니즘을 사용하여 플러그인을 검색합니다.

##### plugin.path

- 타입: list
- 기본값: null
- 설명: 플러그인(커넥터, 컨버터, 변환)이 포함된 경로의 쉼표로 구분된 목록입니다.

```properties
plugin.path=/usr/share/kafka-connect-jdbc,/usr/share/kafka-connect-elasticsearch
```

##### reconnect.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 설명: 브로커에 대한 재연결 시도 시 대기하는 최대 시간입니다.

##### reconnect.backoff.ms

- 타입: long
- 기본값: 50
- 설명: 브로커에 대한 재연결 시도 전 대기하는 기본 시간입니다.

##### response.http.headers.config

- 타입: string
- 기본값: ""
- 설명: REST API 응답에 추가할 HTTP 헤더를 설정합니다.

##### rest.advertised.host.name

- 타입: string
- 기본값: null
- 설명: 다른 Worker에게 광고할 호스트 이름입니다.

##### rest.advertised.listener

- 타입: string
- 기본값: null
- 설명: 다른 Worker에게 광고할 리스너(HTTP 또는 HTTPS)입니다.

##### rest.advertised.port

- 타입: int
- 기본값: null
- 설명: 다른 Worker에게 광고할 포트입니다.

##### rest.extension.classes

- 타입: list
- 기본값: ""
- 설명: REST 리소스를 확장하는 데 사용되는 ConnectRestExtension 클래스 목록입니다.

##### retry.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 설명: 오류가 발생한 요청을 재시도할 때 대기하는 최대 시간입니다.

##### retry.backoff.ms

- 타입: long
- 기본값: 100
- 설명: 오류가 발생한 요청을 재시도하기 전에 대기하는 기본 시간입니다.

##### sasl.mechanism

- 타입: string
- 기본값: GSSAPI
- 설명: SASL 연결에 사용되는 SASL 메커니즘입니다.

##### scheduled.rebalance.max.delay.ms

- 타입: int
- 기본값: 300000 (5분)
- 유효값: [0,...,2147483647]
- 설명: 스케줄된 리밸런스 전에 대기할 최대 지연 시간입니다.

##### socket.connection.setup.timeout.max.ms

- 타입: long
- 기본값: 30000 (30초)
- 설명: 소켓 연결 설정 시 대기하는 최대 시간입니다.

##### socket.connection.setup.timeout.ms

- 타입: long
- 기본값: 10000 (10초)
- 설명: 소켓 연결 설정 시 대기하는 기본 시간입니다.

##### status.storage.partitions

- 타입: int
- 기본값: 5
- 설명: 상태 저장소 토픽을 생성할 때 사용할 파티션 수입니다.

##### status.storage.replication.factor

- 타입: short
- 기본값: 3
- 설명: 상태 저장소 토픽을 생성할 때 사용할 복제 계수입니다.

##### task.shutdown.graceful.timeout.ms

- 타입: long
- 기본값: 5000 (5초)
- 설명: 태스크가 정상적으로 종료되기를 기다리는 시간입니다.

##### topic.creation.enable

- 타입: boolean
- 기본값: true
- 설명: Source 커넥터가 토픽을 자동으로 생성할 수 있도록 허용할지 여부입니다.

##### topic.tracking.allow.reset

- 타입: boolean
- 기본값: true
- 설명: 커넥터의 활성 토픽 세트를 재설정할 수 있도록 허용할지 여부입니다.

##### topic.tracking.enable

- 타입: boolean
- 기본값: true
- 설명: 커넥터당 활성 토픽 집합 추적을 활성화할지 여부입니다.

---

### SSL 설정

Kafka Connect와 브로커 간의 보안 통신을 위한 SSL 설정입니다.

#### 높은 중요도

##### ssl.key.password

- 타입: password
- 기본값: null
- 설명: 키 저장소 파일의 개인 키 비밀번호입니다.

##### ssl.keystore.certificate.chain

- 타입: password
- 기본값: null
- 설명: PEM 형식의 인증서 체인입니다.

##### ssl.keystore.key

- 타입: password
- 기본값: null
- 설명: PEM 형식의 개인 키입니다.

##### ssl.keystore.location

- 타입: string
- 기본값: null
- 설명: 키 저장소 파일의 위치입니다.

##### ssl.keystore.password

- 타입: password
- 기본값: null
- 설명: 키 저장소 파일의 저장소 비밀번호입니다.

##### ssl.truststore.certificates

- 타입: password
- 기본값: null
- 설명: PEM 형식의 신뢰할 수 있는 인증서입니다.

##### ssl.truststore.location

- 타입: string
- 기본값: null
- 설명: 신뢰 저장소 파일의 위치입니다.

##### ssl.truststore.password

- 타입: password
- 기본값: null
- 설명: 신뢰 저장소 파일의 비밀번호입니다.

#### 중간/낮은 중요도

##### ssl.enabled.protocols

- 타입: list
- 기본값: TLSv1.2
- 설명: SSL 연결에 활성화된 프로토콜 목록입니다.

##### ssl.keystore.type

- 타입: string
- 기본값: JKS
- 유효값: JKS, PKCS12, PEM
- 설명: 키 저장소 파일의 파일 형식입니다.

##### ssl.protocol

- 타입: string
- 기본값: TLSv1.2
- 설명: SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다.

##### ssl.truststore.type

- 타입: string
- 기본값: JKS
- 설명: 신뢰 저장소 파일의 파일 형식입니다.

##### ssl.cipher.suites

- 타입: list
- 기본값: null
- 설명: 암호 스위트의 목록입니다.

##### ssl.client.auth

- 타입: string
- 기본값: none
- 유효값: required, requested, none
- 설명: 클라이언트 인증 구성입니다.

##### ssl.endpoint.identification.algorithm

- 타입: string
- 기본값: https
- 설명: 서버 인증서를 사용하여 서버 호스트 이름을 확인하는 엔드포인트 식별 알고리즘입니다.

##### ssl.keymanager.algorithm

- 타입: string
- 기본값: SunX509
- 설명: SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다.

##### ssl.trustmanager.algorithm

- 타입: string
- 기본값: PKIX
- 설명: SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다.

---

### Source 커넥터 설정

Source 커넥터는 외부 시스템에서 Kafka로 데이터를 가져오는 커넥터입니다.

#### 높은 중요도

##### name

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터에 사용할 전역적으로 고유한 이름입니다.

##### connector.class

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터의 클래스 이름 또는 별칭입니다. 정규화된 클래스 이름이거나 플러그인에 정의된 별칭이어야 합니다.

##### tasks.max

- 타입: int
- 기본값: 1
- 유효값: [1,...]
- 설명: 이 커넥터에 대해 생성할 최대 태스크 수입니다. 커넥터는 병렬 처리를 위해 여러 태스크를 생성할 수 있습니다.

#### 중간 중요도

##### exactly.once.support

- 타입: string
- 기본값: requested
- 유효값: REQUIRED, REQUESTED
- 설명: 커넥터에서 정확히 한 번(Exactly-Once) 지원 수준을 요청합니다.

##### transaction.boundary

- 타입: string
- 기본값: poll
- 유효값: INTERVAL, POLL, CONNECTOR
- 설명: 커넥터가 생성하는 트랜잭션 경계를 정의합니다.

##### errors.tolerance

- 타입: string
- 기본값: none
- 유효값: none, all
- 설명: 오류 허용 수준입니다. `none`은 오류 발생 시 즉시 실패하고, `all`은 오류를 무시하고 계속 진행합니다.

##### errors.retry.timeout

- 타입: long
- 기본값: 0
- 설명: 실패한 작업을 재시도하는 최대 시간(밀리초)입니다. 0이면 재시도하지 않습니다.

#### 낮은 중요도

##### transaction.boundary.interval.ms

- 타입: long
- 기본값: null
- 유효값: [0,...]
- 설명: `transaction.boundary`가 `INTERVAL`로 설정된 경우 트랜잭션 간 간격입니다.

##### offsets.storage.topic

- 타입: string
- 기본값: null
- 설명: 이 커넥터의 오프셋을 저장할 개별 토픽의 이름입니다. null이면 Worker의 기본 오프셋 저장소 토픽이 사용됩니다.

---

### Sink 커넥터 설정

Sink 커넥터는 Kafka에서 외부 시스템으로 데이터를 내보내는 커넥터입니다.

#### 높은 중요도

##### name

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터에 사용할 전역적으로 고유한 이름입니다.

##### connector.class

- 타입: string
- 기본값: (필수)
- 설명: 이 커넥터의 클래스 이름 또는 별칭입니다.

##### tasks.max

- 타입: int
- 기본값: 1
- 유효값: [1,...]
- 설명: 이 커넥터에 대해 생성할 최대 태스크 수입니다.

##### topics

- 타입: list
- 기본값: ""
- 설명: 소비할 토픽의 쉼표로 구분된 목록입니다.

```properties
topics=topic1,topic2,topic3
```

##### topics.regex

- 타입: string
- 기본값: ""
- 설명: 소비할 토픽을 지정하는 정규 표현식입니다. `topics`와 함께 사용할 수 없습니다.

```properties
topics.regex=my-topic-.*
```

#### 중간 중요도

##### errors.tolerance

- 타입: string
- 기본값: none
- 유효값: none, all
- 설명: 오류 허용 수준입니다.

##### errors.deadletterqueue.topic.name

- 타입: string
- 기본값: ""
- 설명: 데드 레터 큐로 사용할 토픽의 이름입니다. 처리에 실패한 레코드가 이 토픽으로 전송됩니다.

```properties
errors.deadletterqueue.topic.name=my-connector-dlq
```

##### errors.deadletterqueue.topic.replication.factor

- 타입: short
- 기본값: 3
- 설명: 데드 레터 큐 토픽을 생성할 때 사용할 복제 계수입니다.

##### errors.deadletterqueue.context.headers.enable

- 타입: boolean
- 기본값: false
- 설명: `true`로 설정하면 오류 컨텍스트 헤더를 데드 레터 큐의 메시지에 추가합니다. 이 헤더에는 오류 원인에 대한 정보가 포함됩니다.

---

### 설정 예제

#### 분산 모드 Worker 설정 (connect-distributed.properties)

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

#### 독립 실행 모드 Worker 설정 (connect-standalone.properties)

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

#### Source 커넥터 예제 (FileStream Source)

```properties
name=local-file-source
connector.class=FileStreamSource
tasks.max=1
file=/tmp/test.txt
topic=connect-test
```

#### Sink 커넥터 예제 (FileStream Sink)

```properties
name=local-file-sink
connector.class=FileStreamSink
tasks.max=1
file=/tmp/test.sink.txt
topics=connect-test
```

#### 오류 처리가 포함된 Sink 커넥터 예제

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

### 요약

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

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Connect 사용자 가이드](https://kafka.apache.org/documentation/#connect)
- [Kafka Connect REST API](https://kafka.apache.org/documentation/#connect_rest)
- [Confluent Kafka Connect 문서](https://docs.confluent.io/platform/current/connect/index.html)

---

## Kafka Streams 설정 (Kafka Streams Configs)

> 원본 문서: https://kafka.apache.org/documentation/#streamsconfigs

---

### 목차

1. [개요](#개요)
2. [높은 중요도 설정 (High Importance)](#높은-중요도-설정-high-importance)
3. [중간 중요도 설정 (Medium Importance)](#중간-중요도-설정-medium-importance)
4. [낮은 중요도 설정 (Low Importance)](#낮은-중요도-설정-low-importance)

---

### 개요

Kafka Streams는 Apache Kafka에서 스트림 처리 애플리케이션을 구축하기 위한 클라이언트 라이브러리입니다. Kafka Streams 애플리케이션을 구성하는 설정 파라미터는 중요도에 따라 세 가지 범주로 구분됩니다:
- 높은 중요도 (High): 필수적이거나 매우 중요한 설정
- 중간 중요도 (Medium): 일반적으로 사용되는 설정
- 낮은 중요도 (Low): 고급 튜닝 또는 특수한 경우에 사용되는 설정

---

### 높은 중요도 설정 (High Importance)

#### application.id

스트림 처리 애플리케이션의 식별자입니다. Kafka 클러스터 내에서 고유해야 합니다. 다음 용도로 사용됩니다:
- 기본 클라이언트 ID 접두사
- 멤버십을 위한 그룹 ID
- changelog 토픽 접두사

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | 없음 (필수) |
| 중요도 | high |

#### bootstrap.servers

Kafka 클러스터에 대한 초기 연결을 설정하기 위한 호스트/포트 쌍 목록입니다. 부트스트래핑용으로만 사용되며, 실제 클라이언트는 여기에 지정된 서버와 무관하게 클러스터 내 모든 서버를 활용합니다.

형식: `host1:port1,host2:port2,...`

이 목록은 전체 클러스터 멤버십을 파악하기 위한 초기 호스트에만 영향을 미칩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | 없음 (필수) |
| 중요도 | high |

#### ensure.explicit.internal.resource.naming

`true`로 설정하면 모든 내부 토폴로지 리소스(state stores, repartition topics, changelog topics)에 대해 명시적인 이름 지정을 강제합니다. 설정하지 않거나 `false`로 설정하면 내부 리소스에 자동 생성된 이름이 사용될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 중요도 | high |

#### num.standby.replicas

각 태스크에 대한 스탠바이 레플리카 수입니다. 스탠바이 레플리카는 각 태스크의 로컬 상태 저장소의 섀도우 복사본을 유지합니다. 태스크가 실패하면 스탠바이 레플리카가 있는 스트림 스레드가 더 빨리 복구할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 0 |
| 유효 값 | 0 이상 |
| 중요도 | high |

#### state.dir

상태 저장소(state store)의 디렉토리 위치입니다. 이 경로는 동일한 기본 시스템에서 실행되는 각 스트림 인스턴스에 대해 고유해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | `${java.io.tmpdir}/kafka-streams` |
| 중요도 | high |

---

### 중간 중요도 설정 (Medium Importance)

#### acceptable.recovery.lag

클라이언트가 활성 태스크를 따라잡은(caught-up) 것으로 간주하기 위한 최대 허용 지연입니다. 스트림 태스크는 복구 중 상태를 복원해야 할 수 있으며, 이 설정은 복구 완료로 간주할 수 있는 지연의 상한을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 |
| 유효 값 | 0 이상 |
| 중요도 | medium |

#### cache.max.bytes.buffering

> 지원 중단됨: `statestore.cache.max.bytes` 사용을 권장합니다.

모든 스레드에서 버퍼링에 사용할 최대 메모리 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10485760 (10 MB) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

#### client.id

내부 소비자(consumer), 생산자(producer), 복원 소비자(restore-consumer)에 사용되는 ID 접두사 문자열입니다. 패턴은 `<client.id>-StreamThread-<threadSequenceNumber>-<consumer|producer|restore-consumer>` 형식으로 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" (빈 문자열) |
| 중요도 | medium |

#### default.deserialization.exception.handler

`DeserializationExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 역직렬화 오류가 발생했을 때 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.LogAndFailExceptionHandler` |
| 중요도 | medium |

#### default.key.serde

`org.apache.kafka.common.serialization.Serde` 인터페이스를 구현하는 키의 기본 직렬화기/역직렬화기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### default.list.key.serde.inner

리스트 키 타입의 내부 serde 클래스입니다. `default.list.key.serde.type`이 설정된 경우 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### default.list.key.serde.type

키에 대한 리스트 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### default.list.value.serde.inner

리스트 값 타입의 내부 serde 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### default.list.value.serde.type

값에 대한 리스트 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### default.production.exception.handler

> 지원 중단됨: `production.exception.handler` 사용을 권장합니다.

`ProductionExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.DefaultProductionExceptionHandler` |
| 중요도 | medium |

#### default.timestamp.extractor

`TimestampExtractor` 인터페이스를 구현하는 기본 타임스탬프 추출기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.processor.FailOnInvalidTimestamp` |
| 중요도 | medium |

#### default.value.serde

`org.apache.kafka.common.serialization.Serde` 인터페이스를 구현하는 값의 기본 직렬화기/역직렬화기 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### deserialization.exception.handler

`DeserializationExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | medium |

#### group.protocol

사용할 그룹 프로토콜입니다. 클래식(classic) 프로토콜 또는 스트림(streams) 프로토콜을 선택할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | classic |
| 유효 값 | classic, streams |
| 중요도 | medium |

#### max.task.idle.ms

스트림 태스크가 다른 입력 파티션의 추가 레코드를 기다리며 유휴 상태로 대기하는 최대 시간(밀리초)입니다. 여러 입력 스트림이 있을 때 레코드 처리 순서를 맞추는 데 활용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 0 |
| 유효 값 | 0 이상 |
| 중요도 | medium |

#### max.warmup.replicas

고가용성을 위해 할당할 수 있는 최대 워밍업 레플리카 수입니다. 워밍업 레플리카는 스탠바이 레플리카 외에 추가로 할당될 수 있으며, 스트림의 부하 분산에 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효 값 | 1 이상 |
| 중요도 | medium |

#### num.stream.threads

스트림 처리를 실행할 스레드 수입니다. 각 스레드는 독립적으로 태스크를 실행합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효 값 | 1 이상 |
| 중요도 | medium |

#### processing.exception.handler

`ProcessingExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 레코드 처리 중 오류가 발생했을 때의 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.LogAndFailProcessingExceptionHandler` |
| 중요도 | medium |

#### processing.guarantee

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

#### production.exception.handler

`ProductionExceptionHandler` 인터페이스를 구현하는 예외 처리 클래스입니다. 프로듀서 오류가 발생했을 때의 동작을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.errors.DefaultProductionExceptionHandler` |
| 중요도 | medium |

#### replication.factor

스트림 처리 애플리케이션이 생성하는 changelog 토픽 및 repartition 토픽의 복제 인수입니다. 클러스터 전체에 데이터를 복제하는 수준을 결정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | -1 |
| 중요도 | medium |

> 참고: -1은 브로커의 기본 복제 인수를 사용함을 의미합니다.

#### security.protocol

브로커와 통신하는 데 사용되는 보안 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효 값 | PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL |
| 중요도 | medium |

#### statestore.cache.max.bytes

모든 스레드에서 상태 저장소 캐시에 사용할 최대 메모리 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10485760 (10 MB) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

#### task.assignor.class

태스크 할당에 사용할 `TaskAssignor` 구현체 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | medium |

#### task.timeout.ms

태스크가 오류를 발생시키기 전까지 정지(stall) 상태로 대기할 수 있는 최대 시간(밀리초)입니다. 이 시간을 초과하면 `TaskTimeoutException`이 발생합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효 값 | 0 이상 |
| 중요도 | medium |

#### topology.optimization

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

### 낮은 중요도 설정 (Low Importance)

#### application.server

상태 저장소 검색을 위한 호스트:포트 쌍입니다. Interactive Queries 사용 시 다른 애플리케이션 인스턴스가 이 인스턴스에 접속할 수 있는 주소를 지정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" (빈 문자열) |
| 중요도 | low |

#### buffered.records.per.partition

파티션당 버퍼링할 최대 레코드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1000 |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### built.in.metrics.version

내장 메트릭의 버전입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | latest |
| 중요도 | low |

#### commit.interval.ms

프로세서 위치를 저장하기 위해 커밋하는 빈도(밀리초)입니다. `processing.guarantee`가 `exactly_once_v2`인 경우 기본값은 100ms이고, 그렇지 않으면 30000ms(30초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (at_least_once), 100 (exactly_once_v2) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### connections.max.idle.ms

유휴 연결이 닫히기 전까지의 대기 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 540000 (9분) |
| 중요도 | low |

#### default.client.supplier

`KafkaClientSupplier` 인터페이스를 구현하는 클래스입니다. Kafka 클라이언트(producer, consumer, admin) 인스턴스를 생성할 때 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | `org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier` |
| 중요도 | low |

#### default.dsl.store

DSL 연산자에 사용되는 기본 상태 저장소 타입입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rocksDB |
| 유효 값 | rocksDB, in_memory |
| 중요도 | low |

#### dsl.store.suppliers.class

DSL 상태 저장소를 제공하는 `DslStoreSuppliers` 구현 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

#### enable.metrics.push

내부 클라이언트 메트릭 푸시 활성화 여부입니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 중요도 | low |

#### log.summary.interval.ms

Kafka Streams 상태 요약을 로깅하는 간격(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 120000 (2분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### metadata.max.age.ms

클러스터 메타데이터를 강제로 새로 고침할 때까지의 시간(밀리초)입니다. 새 브로커나 파티션을 사전에 감지하기 위해 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### metadata.recovery.rebootstrap.trigger.ms

브로커 사용 불가 시 재부트스트랩을 트리거하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 중요도 | low |

#### metadata.recovery.strategy

브로커 사용 불가 시 복구 전략입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rebootstrap |
| 유효 값 | none, rebootstrap |
| 중요도 | low |

#### metric.reporters

메트릭 리포터로 사용할 클래스 목록입니다. `MetricReporter` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" (빈 문자열) |
| 중요도 | low |

#### metrics.num.samples

메트릭 계산을 위해 유지하는 샘플 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효 값 | 1 이상 |
| 중요도 | low |

#### metrics.recording.level

메트릭의 가장 높은 기록 수준입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | INFO |
| 유효 값 | INFO, DEBUG, TRACE |
| 중요도 | low |

#### metrics.sample.window.ms

메트릭 샘플이 계산되는 시간 창(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### poll.ms

입력을 기다리는 블록 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 중요도 | low |

#### probing.rebalance.interval.ms

워밍업 레플리카 상태를 확인하기 위한 탐사적 리밸런스 간격(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 600000 (10분) |
| 유효 값 | 60000 이상 |
| 중요도 | low |

#### processor.wrapper.class

`ProcessorWrapper` 인터페이스를 구현하는 클래스입니다. 프로세서를 래핑해 추가 기능을 제공할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

#### rack.aware.assignment.non_overlap_cost

랙 인식 할당에서 태스크가 다른 인스턴스로 이동할 때의 비용입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 중요도 | low |

#### rack.aware.assignment.strategy

랙 인식 할당 전략입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | none |
| 유효 값 | none, min_traffic, balance_subtopology |
| 중요도 | low |

#### rack.aware.assignment.tags

스탠바이 분배를 위한 클라이언트 태그입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" (빈 리스트) |
| 중요도 | low |

#### rack.aware.assignment.traffic_cost

크로스 랙 트래픽 비용입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 중요도 | low |

#### receive.buffer.bytes

데이터를 읽을 때 사용되는 TCP 수신 버퍼(SO_RCVBUF) 크기입니다. -1은 운영 체제 기본값을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 32768 (32 KB) |
| 유효 값 | -1 이상 |
| 중요도 | low |

#### reconnect.backoff.max.ms

연결 실패 후 브로커 재연결 시 대기하는 최대 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### reconnect.backoff.ms

연결 실패 후 브로커 재연결 시 초기 대기 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 50 |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### repartition.purge.interval.ms

repartition 토픽에서 완전히 소비된 레코드를 제거하는 빈도(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### request.timeout.ms

클라이언트가 요청 응답을 기다리는 최대 시간(밀리초)입니다. 타임아웃 전에 응답이 수신되지 않으면 필요한 경우 요청을 재전송하고, 재시도가 모두 소진되면 해당 요청을 실패로 처리합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 40000 (40초) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### retry.backoff.ms

실패한 요청을 재시도하기 전에 대기하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### rocksdb.config.setter

RocksDB 설정을 커스터마이즈하는 `RocksDBConfigSetter` 구현체 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 중요도 | low |

#### send.buffer.bytes

데이터를 보낼 때 사용되는 TCP 송신 버퍼(SO_SNDBUF) 크기입니다. -1은 운영 체제 기본값을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 131072 (128 KB) |
| 유효 값 | -1 이상 |
| 중요도 | low |

#### state.cleanup.delay.ms

파티션이 마이그레이션된 후 상태를 삭제하기 전 대기 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 600000 (10분) |
| 유효 값 | 0 이상 |
| 중요도 | low |

#### upgrade.from

무중단(라이브) 업그레이드 시 이전 버전과의 호환성을 유지하기 위해 지정하는 버전입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효 값 | null, 0.10.0, 0.10.1, 0.10.2, 0.11.0, 1.0, 1.1, 2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4.0 |
| 중요도 | low |

#### window.size.ms

deserializer 계산을 위한 창 크기(밀리초)입니다. 윈도우 serde에서만 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | null |
| 중요도 | low |

#### windowed.inner.class.serde

윈도우 레코드의 내부 serde 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | low |

#### windowstore.changelog.additional.retention.ms

윈도우 유지 기간에 더해지는 changelog 추가 보존 시간(밀리초)입니다. 윈도우 상태 저장소의 changelog 토픽 보존 기간을 추가로 연장합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 86400000 (24시간) |
| 유효 값 | 0 이상 |
| 중요도 | low |

---

### 설정 예제

#### 기본 설정

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

#### 정확히 한 번 처리 보장 설정

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "exactly-once-app");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
```

#### 고가용성 설정

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

#### 보안 설정 (SSL/SASL)

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

#### 성능 튜닝 설정

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

### 참고 자료

- [Apache Kafka 공식 문서 - Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Apache Kafka Streams Configuration](https://kafka.apache.org/documentation/#streamsconfigs)
- [Kafka Streams Developer Guide](https://kafka.apache.org/documentation/streams/developer-guide/)
- [Kafka Streams Architecture](https://kafka.apache.org/documentation/streams/architecture)

---

## Kafka Admin Client 설정

> 원본 참고: https://kafka.apache.org/documentation/#adminclientconfigs

---

### 목차

1. [개요](#개요)
2. [높은 중요도 설정](#높은-중요도-설정)
3. [중간 중요도 설정](#중간-중요도-설정)
4. [낮은 중요도 설정](#낮은-중요도-설정)

---

### 개요

Admin Client는 Kafka 클러스터를 관리하기 위한 클라이언트입니다. 토픽 생성/삭제, ACL 관리, 설정 변경 등의 관리 작업을 수행할 수 있습니다.

---

### 높은 중요도 설정

#### bootstrap.controllers

- 타입: list
- 기본값: ""
- 중요도: high

KRaft 컨트롤러 쿼럼에 초기 연결을 설정하는 데 사용되는 호스트/포트 쌍 목록입니다. 형식은 `host1:port1,host2:port2,...`입니다.

#### bootstrap.servers

- 타입: list
- 기본값: ""
- 중요도: high

Kafka 클러스터 검색을 위한 초기 연결 지점입니다. 형식은 `host1:port1,host2:port2,...`입니다. 이 목록은 전체 클러스터 멤버십을 검색하기 위한 초기 호스트로만 사용되며, 동적으로 변경될 수 있습니다. 이 목록에 모든 서버를 포함할 필요는 없지만, 하나의 서버가 다운되었을 경우를 대비하여 최소 두 개 이상을 지정하는 것이 좋습니다.

#### ssl.key.password

- 타입: password
- 기본값: null
- 중요도: high

키 저장소 파일 또는 `ssl.keystore.key`에 지정된 PEM 키의 개인 키 비밀번호입니다.

#### ssl.keystore.certificate.chain

- 타입: password
- 기본값: null
- 중요도: high

`ssl.keystore.type`에 의해 지정된 형식의 인증서 체인입니다. 기본 SSL 엔진 팩토리는 X.509 인증서 목록이 포함된 PEM 형식만 지원합니다.

#### ssl.keystore.key

- 타입: password
- 기본값: null
- 중요도: high

`ssl.keystore.type`에 의해 지정된 형식의 개인 키입니다. 기본 SSL 엔진 팩토리는 PKCS#8 키가 포함된 PEM 형식만 지원합니다. 키가 암호화된 경우 `ssl.key.password`를 사용하여 키 비밀번호를 지정해야 합니다.

#### ssl.keystore.location

- 타입: string
- 기본값: null
- 중요도: high

키 저장소 파일의 위치입니다. 클라이언트의 경우 선택 사항이며, 클라이언트의 양방향 인증에 사용할 수 있습니다.

#### ssl.keystore.password

- 타입: password
- 기본값: null
- 중요도: high

키 저장소 파일의 저장소 비밀번호입니다. 클라이언트의 경우 선택 사항이며, `ssl.keystore.location`이 구성된 경우에만 필요합니다. PEM 형식에서는 키 저장소 비밀번호가 지원되지 않습니다.

#### ssl.truststore.certificates

- 타입: password
- 기본값: null
- 중요도: high

`ssl.truststore.type`에 의해 지정된 형식의 신뢰할 수 있는 인증서입니다. 기본 SSL 엔진 팩토리는 X.509 인증서가 포함된 PEM 형식만 지원합니다.

#### ssl.truststore.location

- 타입: string
- 기본값: null
- 중요도: high

신뢰 저장소 파일의 위치입니다.

#### ssl.truststore.password

- 타입: password
- 기본값: null
- 중요도: high

신뢰 저장소 파일의 비밀번호입니다. 비밀번호가 설정되지 않은 경우 구성된 신뢰 저장소 파일이 여전히 사용되지만 무결성 검사가 비활성화됩니다. PEM 형식에서는 신뢰 저장소 비밀번호가 지원되지 않습니다.

---

### 중간 중요도 설정

#### client.dns.lookup

- 타입: string
- 기본값: use_all_dns_ips
- 유효한 값: [use_all_dns_ips, resolve_canonical_bootstrap_servers_only]
- 중요도: medium

클라이언트가 DNS 조회를 사용하는 방법을 제어합니다. `use_all_dns_ips`로 설정하면 연결이 성공적으로 설정될 때까지 반환된 각 IP 주소에 순서대로 연결합니다. 연결 해제 후 다음 IP가 사용됩니다. 모든 IP가 한 번 사용되면 클라이언트는 호스트 이름에서 IP를 다시 확인합니다. `resolve_canonical_bootstrap_servers_only`로 설정하면 각 부트스트랩 주소를 정규 이름 목록으로 확인합니다. 부트스트랩 단계 후 이는 `use_all_dns_ips`와 동일하게 작동합니다.

#### client.id

- 타입: string
- 기본값: ""
- 중요도: medium

요청 시 서버에 전달할 ID 문자열입니다. 서버 측 요청 로그에 논리적 애플리케이션 이름을 포함시켜, IP/포트 이상의 정보로 요청 출처를 추적할 수 있습니다.

#### connections.max.idle.ms

- 타입: long
- 기본값: 300000 (5분)
- 중요도: medium

지정된 시간(밀리초) 동안 유휴 상태인 연결을 닫습니다.

#### default.api.timeout.ms

- 타입: int
- 기본값: 60000 (1분)
- 유효한 값: [0, ...]
- 중요도: medium

클라이언트 API에 대한 기본 타임아웃을 밀리초 단위로 지정합니다. 이 타임아웃은 명시적인 타임아웃 매개변수를 지정하지 않는 모든 클라이언트 작업에 사용됩니다.

#### receive.buffer.bytes

- 타입: int
- 기본값: 65536 (64KB)
- 유효한 값: [-1, ...]
- 중요도: medium

데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

#### request.timeout.ms

- 타입: int
- 기본값: 30000 (30초)
- 유효한 값: [0, ...]
- 중요도: medium

클라이언트가 요청 응답을 기다리는 최대 시간을 제어합니다. 타임아웃이 경과하기 전에 응답이 수신되지 않으면 클라이언트는 필요한 경우 요청을 재전송하거나 재시도가 소진되면 요청을 실패 처리합니다.

#### sasl.client.callback.handler.class

- 타입: class
- 기본값: null
- 중요도: medium

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 클라이언트 콜백 핸들러 클래스의 정규화된 이름입니다.

#### sasl.jaas.config

- 타입: password
- 기본값: null
- 중요도: medium

JAAS 구성 파일에서 사용되는 형식의 SASL 연결에 대한 JAAS 로그인 컨텍스트 매개변수입니다. JAAS 구성 파일 형식은 여기에 설명되어 있습니다. 값의 형식은 `loginModuleClass controlFlag (optionName=optionValue)*;`입니다. 브로커의 경우 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

#### sasl.kerberos.service.name

- 타입: string
- 기본값: null
- 중요도: medium

Kafka가 실행되는 Kerberos 주체 이름입니다. 이는 Kafka의 JAAS 구성 또는 Kafka의 구성에서 정의할 수 있습니다.

#### sasl.login.callback.handler.class

- 타입: class
- 기본값: null
- 중요도: medium

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 로그인 콜백 핸들러 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 콜백 핸들러 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

#### sasl.login.class

- 타입: class
- 기본값: null
- 중요도: medium

Login 인터페이스를 구현하는 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

#### sasl.mechanism

- 타입: string
- 기본값: GSSAPI
- 중요도: medium

클라이언트 연결에 사용되는 SASL 메커니즘입니다. 보안 제공자가 사용 가능한 모든 메커니즘이 될 수 있습니다. GSSAPI가 기본 메커니즘입니다.

#### sasl.oauthbearer.jwks.endpoint.url

- 타입: string
- 기본값: null
- 중요도: medium

제공자의 JWKS(JSON Web Key Set) 검색이 가능한 OAuth/OIDC 제공자 URL입니다. URL은 HTTP(S) 기반이거나 파일 기반일 수 있습니다. URL이 HTTP(S) 기반인 경우 JWKS 데이터는 브로커 시작 시 구성된 URL을 통해 OAuth/OIDC 제공자로부터 검색됩니다. 당시 최신이었던 모든 키는 들어오는 요청에 사용할 수 있도록 브로커에 캐시됩니다. URL이 파일 기반인 경우 브로커는 구성된 위치에서 JWKS 파일을 로드합니다.

#### sasl.oauthbearer.token.endpoint.url

- 타입: string
- 기본값: null
- 중요도: medium

OAuth/OIDC ID 제공자의 URL입니다. URL이 HTTP(S) 기반인 경우 `sasl.jaas.config`의 구성에 따라 로그인 요청이 이 URL로 전송되어 토큰을 검색하는 `sasl.login.callback.handler.class`의 기본 구현입니다. URL이 파일 기반인 경우 인가 토큰에 사용할 OAuth/OIDC ID 제공자에서 발급한 액세스 토큰(JWT 직렬화 형식)이 포함된 파일을 지정합니다.

#### security.protocol

- 타입: string
- 기본값: PLAINTEXT
- 유효한 값: [PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL]
- 중요도: medium

브로커와 통신하는 데 사용되는 프로토콜입니다.

#### send.buffer.bytes

- 타입: int
- 기본값: 131072 (128KB)
- 유효한 값: [-1, ...]
- 중요도: medium

데이터를 전송할 때 사용할 TCP 전송 버퍼(SO_SNDBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

#### socket.connection.setup.timeout.max.ms

- 타입: long
- 기본값: 30000 (30초)
- 중요도: medium

클라이언트가 소켓 연결이 설정될 때까지 기다리는 최대 시간입니다. 연결 설정 타임아웃은 연속적인 연결 실패마다 기하급수적으로 증가하며 이 최대값까지 증가합니다. 연결 폭풍을 방지하기 위해 계산된 타임아웃 값에 20% 범위의 무작위 지터가 적용됩니다.

#### socket.connection.setup.timeout.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: medium

클라이언트가 소켓 연결이 설정될 때까지 기다리는 시간입니다. 타임아웃이 경과하기 전에 연결이 설정되지 않으면 클라이언트는 소켓 채널을 닫습니다. 이 값은 초기 백오프 값이며 연속적인 연결 실패마다 `socket.connection.setup.timeout.max.ms` 값까지 기하급수적으로 증가합니다.

#### ssl.enabled.protocols

- 타입: list
- 기본값: TLSv1.2,TLSv1.3
- 중요도: medium

SSL 연결에 활성화된 프로토콜 목록입니다. Java 11 이상에서 실행할 때 기본값은 'TLSv1.2,TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. Java 11의 기본값에서 클라이언트와 서버 모두 TLSv1.3을 지원하는 경우 선호되고, 그렇지 않으면 TLSv1.2로 폴백됩니다(둘 다 최소 TLSv1.2를 지원한다고 가정).

#### ssl.keystore.type

- 타입: string
- 기본값: JKS
- 중요도: medium

키 저장소 파일의 파일 형식입니다. 클라이언트의 경우 선택 사항입니다. 사용 가능한 값은 JKS, PKCS12, PEM입니다.

#### ssl.protocol

- 타입: string
- 기본값: TLSv1.3
- 중요도: medium

SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다. Java 11 이상에서 실행할 때 기본값은 'TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. 이 값은 대부분의 사용 사례에 적합합니다. 최근 JVM에서 허용되는 값은 'TLSv1.2' 및 'TLSv1.3'입니다. 'TLS', 'TLSv1.1', 'SSL', 'SSLv2' 및 'SSLv3'은 이전 JVM에서 지원될 수 있지만 알려진 보안 취약성으로 인해 사용이 권장되지 않습니다.

#### ssl.provider

- 타입: string
- 기본값: null
- 중요도: medium

SSL 연결에 사용되는 보안 제공자의 이름입니다. 기본값은 JVM의 기본 보안 제공자입니다.

#### ssl.truststore.type

- 타입: string
- 기본값: JKS
- 중요도: medium

신뢰 저장소 파일의 파일 형식입니다. 사용 가능한 값은 JKS, PKCS12, PEM입니다.

#### sasl.oauthbearer.assertion.algorithm

- 타입: string
- 기본값: null
- 중요도: medium

JWT 서명에 사용되는 알고리즘입니다. RS256 또는 ES256을 사용할 수 있습니다.

#### sasl.oauthbearer.assertion.claim.aud

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 audience(대상) 클레임입니다.

#### sasl.oauthbearer.assertion.claim.iss

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 issuer(발급자) 클레임입니다.

#### sasl.oauthbearer.assertion.claim.sub

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 subject(주체) 클레임입니다.

#### sasl.oauthbearer.assertion.claim.jti

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 JWT ID 클레임입니다.

#### sasl.oauthbearer.assertion.file

- 타입: string
- 기본값: null
- 중요도: medium

사전 생성된 JWT 파일로, 실시간 로테이션을 지원합니다.

#### sasl.oauthbearer.assertion.private.key.file

- 타입: string
- 기본값: null
- 중요도: medium

JWT 서명에 사용되는 개인 키 파일입니다.

#### sasl.oauthbearer.assertion.private.key.passphrase

- 타입: password
- 기본값: null
- 중요도: medium

키 파일 복호화 암호입니다.

#### sasl.oauthbearer.assertion.template.file

- 타입: string
- 기본값: null
- 중요도: medium

JWT 헤더/페이로드 템플릿 파일입니다.

#### sasl.oauthbearer.client.credentials.client.id

- 타입: string
- 기본값: null
- 중요도: medium

OAuth 클라이언트 식별자입니다.

#### sasl.oauthbearer.client.credentials.client.secret

- 타입: password
- 기본값: null
- 중요도: medium

OAuth 클라이언트 시크릿입니다.

#### sasl.oauthbearer.jwt.retriever.class

- 타입: class
- 기본값: null
- 중요도: medium

사용자 정의 JWT 검색 구현 클래스입니다.

#### sasl.oauthbearer.jwt.validator.class

- 타입: class
- 기본값: null
- 중요도: medium

사용자 정의 JWT 검증 구현 클래스입니다.

#### sasl.oauthbearer.scope

- 타입: string
- 기본값: null
- 중요도: medium

리소스/API 요청에 대한 액세스 수준입니다.

---

### 낮은 중요도 설정

#### enable.metrics.push

- 타입: boolean
- 기본값: false
- 중요도: low

클라이언트 메트릭을 클러스터로 푸시할지 여부입니다.

#### metadata.max.age.ms

- 타입: long
- 기본값: 300000 (5분)
- 유효한 값: [0, ...]
- 중요도: low

파티션 리더십 변경이 없더라도 이 시간(밀리초)마다 강제로 메타데이터를 갱신하여, 새로운 브로커나 파티션을 사전에 감지합니다.

#### metadata.recovery.rebootstrap.trigger.ms

- 타입: long
- 기본값: 300000 (5분)
- 중요도: low

재부트스트랩 트리거 임계값입니다.

#### metadata.recovery.strategy

- 타입: string
- 기본값: rebootstrap
- 유효한 값: [none, rebootstrap]
- 중요도: low

복구 접근 방식입니다. `none` 또는 `rebootstrap`을 선택할 수 있습니다.

#### metric.reporters

- 타입: list
- 기본값: ""
- 중요도: low

메트릭 리포터로 사용할 클래스 목록입니다. `org.apache.kafka.common.metrics.MetricsReporter` 인터페이스를 구현하면 새 메트릭 생성 알림을 받을 수 있는 클래스를 연결할 수 있습니다. JmxReporter는 JMX 통계 등록을 위해 항상 포함됩니다.

#### metrics.num.samples

- 타입: int
- 기본값: 2
- 유효한 값: [1, ...]
- 중요도: low

메트릭 계산을 위해 유지되는 샘플 수입니다.

#### metrics.recording.level

- 타입: string
- 기본값: INFO
- 유효한 값: [INFO, DEBUG, TRACE]
- 중요도: low

메트릭을 기록할 최대 레벨입니다.

#### metrics.sample.window.ms

- 타입: long
- 기본값: 30000 (30초)
- 유효한 값: [0, ...]
- 중요도: low

메트릭 샘플이 계산되는 시간 창입니다.

#### reconnect.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 유효한 값: [0, ...]
- 중요도: low

브로커에 반복적으로 연결 실패할 때 대기할 최대 시간(밀리초)입니다. 제공되는 경우 호스트당 백오프는 연속적인 연결 실패마다 이 최대값까지 기하급수적으로 증가합니다. 연결 폭풍을 방지하기 위해 백오프 증가 계산 후 20%의 무작위 지터가 추가됩니다.

#### reconnect.backoff.ms

- 타입: long
- 기본값: 50
- 유효한 값: [0, ...]
- 중요도: low

주어진 호스트에 재연결을 시도하기 전 대기할 기본 시간입니다. 타이트한 루프에서 동일 호스트에 반복 연결하는 것을 방지합니다. 이 백오프는 클라이언트가 브로커에 시도하는 모든 연결에 적용됩니다. 이 값은 초기 백오프 값이며 연속적인 연결 실패마다 `reconnect.backoff.max.ms` 값까지 기하급수적으로 증가합니다.

#### retries

- 타입: int
- 기본값: 2147483647
- 유효한 값: [0, ...]
- 중요도: low

실패한 요청을 재시도할 횟수입니다. 기본값은 사실상 무제한입니다.

#### retry.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 유효한 값: [0, ...]
- 중요도: low

반복적인 실패 요청 시 대기할 최대 시간(밀리초)입니다. 제공되는 경우 요청당 백오프는 연속적인 실패마다 이 최대값까지 기하급수적으로 증가합니다. 연결 폭풍을 방지하기 위해 백오프 증가 계산 후 20%의 무작위 지터가 추가됩니다.

#### retry.backoff.ms

- 타입: long
- 기본값: 100
- 유효한 값: [0, ...]
- 중요도: low

실패한 요청을 재시도하기 전 대기할 기본 시간입니다. 일부 실패 시나리오에서 타이트한 루프로 요청이 반복 전송되는 것을 방지합니다. 이 값은 초기 백오프 값이며 연속적인 실패마다 `retry.backoff.max.ms` 값까지 기하급수적으로 증가합니다.

#### sasl.kerberos.kinit.cmd

- 타입: string
- 기본값: /usr/bin/kinit
- 중요도: low

Kerberos kinit 명령 경로입니다.

#### sasl.kerberos.min.time.before.relogin

- 타입: long
- 기본값: 60000 (1분)
- 중요도: low

새로 고침 시도 사이의 로그인 스레드 휴면 시간입니다.

#### sasl.kerberos.ticket.renew.jitter

- 타입: double
- 기본값: 0.05
- 중요도: low

갱신 시간에 추가되는 무작위 지터의 백분율입니다.

#### sasl.kerberos.ticket.renew.window.factor

- 타입: double
- 기본값: 0.8
- 중요도: low

로그인 스레드는 지정된 창 인자에서 마지막 새로 고침부터 티켓 만료까지의 시간이 도달할 때까지 휴면한 후 티켓 갱신을 시도합니다.

#### sasl.login.connect.timeout.ms

- 타입: int
- 기본값: null
- 중요도: low

외부 인증 제공자 연결 타임아웃입니다.

#### sasl.login.read.timeout.ms

- 타입: int
- 기본값: null
- 중요도: low

외부 인증 제공자 읽기 타임아웃입니다.

#### sasl.login.refresh.buffer.seconds

- 타입: short
- 기본값: 300 (5분)
- 유효한 값: [0, 3600]
- 중요도: low

자격 증명을 새로 고칠 때 자격 증명 만료 전 유지할 버퍼 시간(초)입니다. 새로 고침이 버퍼 시간보다 만료에 더 가깝게 발생하면 새로 고침은 가능한 한 많은 버퍼 시간을 유지하도록 앞당겨집니다. 유효한 값은 0에서 3600(1시간) 사이이며, 값이 지정되지 않으면 기본값 300(5분)이 사용됩니다.

#### sasl.login.refresh.min.period.seconds

- 타입: short
- 기본값: 60 (1분)
- 유효한 값: [0, 900]
- 중요도: low

로그인 새로 고침 스레드가 자격 증명을 새로 고치기 전 대기하는 최소 시간(초)입니다. 유효한 값은 0에서 900(15분) 사이이며, 값이 지정되지 않으면 기본값 60(1분)이 사용됩니다.

#### sasl.login.refresh.window.factor

- 타입: double
- 기본값: 0.8
- 유효한 값: [0.5, 1.0]
- 중요도: low

로그인 새로 고침 스레드는 자격 증명의 수명에 대해 지정된 창 인자에 도달할 때까지 휴면한 후 자격 증명 새로 고침을 시도합니다. 유효한 값은 0.5(50%)에서 1.0(100%) 사이이며, 값이 지정되지 않으면 기본값 0.8(80%)이 사용됩니다.

#### sasl.login.refresh.window.jitter

- 타입: double
- 기본값: 0.05
- 유효한 값: [0.0, 0.25]
- 중요도: low

로그인 새로 고침 스레드의 휴면 시간에 추가되는 자격 증명 수명에 대한 최대 무작위 지터입니다. 유효한 값은 0에서 0.25(25%) 사이이며, 값이 지정되지 않으면 기본값 0.05(5%)가 사용됩니다.

#### sasl.login.retry.backoff.max.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: low

로그인 실패 시 최대 재시도 백오프 시간입니다.

#### sasl.login.retry.backoff.ms

- 타입: long
- 기본값: 100
- 중요도: low

로그인 실패 시 초기 재시도 백오프 시간입니다.

#### sasl.oauthbearer.clock.skew.seconds

- 타입: int
- 기본값: 30
- 중요도: low

OAuth/OIDC ID 제공자와 브로커 간의 시간 차이 허용 오차(초)입니다.

#### sasl.oauthbearer.expected.audience

- 타입: list
- 기본값: null
- 중요도: low

예상되는 JWT audience 값 목록입니다. 브로커가 구성된 audience 중 하나가 JWT에 포함되어 있는지 확인합니다.

#### sasl.oauthbearer.expected.issuer

- 타입: string
- 기본값: null
- 중요도: low

예상되는 JWT issuer 값입니다.

#### sasl.oauthbearer.header.urlencode

- 타입: boolean
- 기본값: false
- 중요도: low

RFC6749에 따라 인증 헤더를 URL 인코딩할지 여부입니다.

#### sasl.oauthbearer.jwks.endpoint.refresh.ms

- 타입: long
- 기본값: 3600000 (1시간)
- 중요도: low

JWKS 캐시 새로 고침 간격(밀리초)입니다.

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.ms

- 타입: long
- 기본값: 100
- 중요도: low

JWKS 재시도 초기 백오프 시간(밀리초)입니다.

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: low

JWKS 재시도 최대 백오프 시간(밀리초)입니다.

#### sasl.oauthbearer.scope.claim.name

- 타입: string
- 기본값: scope
- 중요도: low

OAuth scope 클레임 이름입니다.

#### sasl.oauthbearer.sub.claim.name

- 타입: string
- 기본값: sub
- 중요도: low

OAuth subject 클레임 이름입니다.

#### sasl.oauthbearer.assertion.claim.exp.seconds

- 타입: int
- 기본값: 300 (5분)
- 유효한 값: [0, 86400]
- 중요도: low

JWT 만료 오프셋(초)입니다.

#### sasl.oauthbearer.assertion.claim.nbf.seconds

- 타입: int
- 기본값: 60 (1분)
- 유효한 값: [0, 3600]
- 중요도: low

JWT not-before 오프셋(초)입니다.

#### security.providers

- 타입: string
- 기본값: null
- 중요도: low

구성 가능한 생성자 클래스 목록으로, 각각 보안 알고리즘을 구현하는 제공자를 반환합니다. 이러한 클래스는 `org.apache.kafka.common.security.auth.SecurityProviderCreator` 인터페이스를 구현해야 합니다.

#### ssl.cipher.suites

- 타입: list
- 기본값: null
- 중요도: low

암호 모음 목록입니다. 이는 TLS 또는 SSL 네트워크 프로토콜을 사용하여 네트워크 연결의 보안 설정을 협상하는 데 사용되는 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합입니다. 기본적으로 사용 가능한 모든 암호 모음이 지원됩니다.

#### ssl.endpoint.identification.algorithm

- 타입: string
- 기본값: https
- 중요도: low

서버 인증서를 사용하여 서버 호스트 이름을 검증하는 엔드포인트 식별 알고리즘입니다.

#### ssl.engine.factory.class

- 타입: class
- 기본값: null
- 중요도: low

SSLEngine 객체를 제공하기 위한 `org.apache.kafka.common.security.auth.SslEngineFactory` 타입의 클래스입니다. 기본값은 `org.apache.kafka.common.security.ssl.DefaultSslEngineFactory`입니다. 또는 `org.apache.kafka.common.security.ssl.CommonNameLoggingSslEngineFactory`로 설정하면 클라이언트가 피어 인증서에서 일반 이름을 읽으려고 시도할 때 SSL 핸드셰이크가 발생합니다. 성공하면 DEBUG 수준에서 일반 이름이 로깅됩니다.

#### ssl.keymanager.algorithm

- 타입: string
- 기본값: SunX509
- 중요도: low

SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java 가상 머신에 대해 구성된 키 관리자 팩토리 알고리즘입니다.

#### ssl.secure.random.implementation

- 타입: string
- 기본값: null
- 중요도: low

SSL 암호화 작업에 사용되는 SecureRandom PRNG 구현입니다.

#### ssl.trustmanager.algorithm

- 타입: string
- 기본값: PKIX
- 중요도: low

SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java 가상 머신에 대해 구성된 신뢰 관리자 팩토리 알고리즘입니다.
