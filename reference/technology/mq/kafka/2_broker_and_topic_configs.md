# Kafka 브로커와 토픽 설정

## Kafka 브로커 설정

> 원본 참고: https://kafka.apache.org/documentation/#brokerconfigs

---

### 개요

Kafka 브로커는 수백 개 이상의 설정 옵션을 제공합니다. 여기서는 가장 중요하고 자주 사용되는 브로커 설정을 카테고리별로 설명합니다. 각 설정은 타입, 기본값, 유효값, 중요도, 업데이트 모드 정보를 포함합니다.

#### 업데이트 모드 (Update Mode)

- read-only: 브로커 재시작이 필요합니다
- per-broker: 개별 브로커에서 동적으로 업데이트 가능합니다
- cluster-wide: 클러스터 전체에 동적으로 적용됩니다

---

### 필수 설정 (Essential Configurations)

#### node.id

브로커의 고유 식별자입니다. KRaft 모드에서 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 필수 (Required) |
| 중요도 | High |
| 업데이트 모드 | read-only |

> 참고: ZooKeeper 모드에서는 `broker.id`를 사용하고, KRaft 모드에서는 `node.id`를 사용합니다. 클러스터 내에서 각 브로커는 고유한 ID를 가져야 합니다.

#### broker.id

브로커의 고유 식별자입니다. ZooKeeper 모드에서 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | -1 |
| 유효값 | 0 이상의 정수 권장 |
| 중요도 | High |
| 업데이트 모드 | read-only |

각 브로커는 클러스터 내에서 고유한 ID를 가져야 합니다. 일반적으로 0부터 시작하여 1씩 증가하는 정수를 사용합니다. 최대 2,147,483,647개의 브로커를 지원합니다 (signed 32-bit integer).

```properties
broker.id=0
```

#### process.roles

브로커가 수행할 역할을 지정합니다. KRaft 모드에서 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | 필수 (Required) |
| 유효값 | broker, controller |
| 중요도 | High |
| 업데이트 모드 | read-only |

```properties
# 브로커 전용
process.roles=broker

# 컨트롤러 전용
process.roles=controller

# 결합 모드 (Combined)
process.roles=broker,controller
```

#### log.dirs

로그 데이터가 저장될 디렉토리 목록입니다. 쉼표로 구분하여 여러 디렉토리를 지정할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null (log.dir 사용) |
| 중요도 | High |
| 업데이트 모드 | read-only |

Kafka는 파티션을 이 디렉토리들에 분산하여 저장합니다. 전용 디스크 또는 SSD 사용을 권장합니다.

```properties
log.dirs=/var/kafka-logs/disk1,/var/kafka-logs/disk2,/var/kafka-logs/disk3
```

#### log.dir

로그 데이터가 저장될 단일 디렉토리입니다. `log.dirs`가 설정되지 않은 경우에만 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | /tmp/kafka-logs |
| 중요도 | High |
| 업데이트 모드 | read-only |

```properties
log.dir=/var/kafka-logs
```

---

### 네트워크 설정 (Network Configurations)

#### listeners

브로커가 클라이언트 연결을 수신할 주소 목록입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT://:9092 |
| 중요도 | High |
| 업데이트 모드 | per-broker |

형식: `{프로토콜}://{호스트}:{포트}`

```properties
# 단일 리스너
listeners=PLAINTEXT://localhost:9092

# 다중 리스너
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093

# 보안 리스너
listeners=SASL_SSL://0.0.0.0:9094
```

지원되는 프로토콜:
- `PLAINTEXT`: 암호화 없음
- `SSL`: TLS/SSL 암호화
- `SASL_PLAINTEXT`: SASL 인증, 암호화 없음
- `SASL_SSL`: SASL 인증 + TLS/SSL 암호화

> 참고: 포트 번호가 1024 미만이면 root 권한이 필요합니다. 이는 권장되지 않습니다.

#### advertised.listeners

클라이언트와 다른 브로커에게 광고할 리스너 주소입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null (listeners 값 사용) |
| 중요도 | High |
| 업데이트 모드 | per-broker |

클라우드 환경이나 NAT 뒤에서 실행할 때 실제 리스너와 다른 주소를 외부에 알려야 하는 경우 사용합니다.

```properties
# AWS에서 퍼블릭 IP 광고
advertised.listeners=PLAINTEXT://ec2-xxx-xxx-xxx-xxx.compute.amazonaws.com:9092

# 내부/외부 분리
advertised.listeners=INTERNAL://broker1:9092,EXTERNAL://broker1.example.com:9093
```

> 주의: `0.0.0.0` 메타 주소는 `advertised.listeners`에서 사용할 수 없습니다.

#### listener.security.protocol.map

리스너 이름과 보안 프로토콜 간의 매핑을 정의합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL |
| 중요도 | High |
| 업데이트 모드 | per-broker |

```properties
listener.security.protocol.map=INTERNAL:PLAINTEXT,EXTERNAL:SSL,CONTROLLER:PLAINTEXT
```

#### inter.broker.listener.name

브로커 간 통신에 사용할 리스너 이름입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | read-only |

```properties
inter.broker.listener.name=INTERNAL
```

#### controller.listener.names

컨트롤러에서 사용할 리스너 이름 목록입니다. KRaft 모드에서 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | read-only |

```properties
controller.listener.names=CONTROLLER
```

#### controller.quorum.bootstrap.servers

컨트롤러 쿼럼의 부트스트랩 서버 목록입니다. KRaft 모드에서 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 중요도 | High |
| 업데이트 모드 | read-only |

```properties
controller.quorum.bootstrap.servers=controller1:9093,controller2:9093,controller3:9093
```

---

### ZooKeeper 설정 (ZooKeeper Configurations)

> 참고: Kafka 4.0에서 ZooKeeper 모드가 제거되었습니다. KRaft 모드를 사용하십시오.

#### zookeeper.connect

ZooKeeper 연결 문자열입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | read-only |

형식: `호스트명:포트` (쉼표로 구분하여 여러 서버 지정 가능)

```properties
# 단일 ZooKeeper
zookeeper.connect=localhost:2181

# 다중 ZooKeeper (고가용성)
zookeeper.connect=zk1:2181,zk2:2181,zk3:2181

# chroot 경로 사용
zookeeper.connect=zk1:2181,zk2:2181,zk3:2181/kafka
```

#### zookeeper.connection.timeout.ms

ZooKeeper 연결 타임아웃 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null (zookeeper.session.timeout.ms 사용) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### zookeeper.session.timeout.ms

ZooKeeper 세션 타임아웃 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 18000 (18초) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 로그 관리 설정 (Log Management Configurations)

#### log.retention.hours / log.retention.minutes / log.retention.ms

로그 세그먼트가 삭제되기 전 보관되는 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int / long |
| 기본값 | 168시간 (7일) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

`log.retention.ms` > `log.retention.minutes` > `log.retention.hours` 순으로 우선순위가 적용됩니다.

```properties
# 7일 보관
log.retention.hours=168

# 3일 보관
log.retention.hours=72

# 정밀 설정 (밀리초)
log.retention.ms=604800000
```

#### log.retention.bytes

파티션별 로그 보관 최대 크기입니다. 초과 시 오래된 세그먼트가 삭제됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | -1 (무제한) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

```properties
# 파티션당 최대 10GB
log.retention.bytes=10737418240
```

#### log.segment.bytes

단일 로그 세그먼트 파일의 최대 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1073741824 (1GB) |
| 유효값 | 14 이상 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

```properties
log.segment.bytes=1073741824
```

#### log.segment.ms

시간 기반 로그 세그먼트 롤오버 주기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | null (604800000ms = 7일) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

#### log.roll.hours / log.roll.ms

새 로그 세그먼트가 생성되기까지의 최대 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int / long |
| 기본값 | 168시간 (7일) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

#### log.flush.interval.messages

디스크에 플러시하기 전 누적할 최대 메시지 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

> 참고: 기본값은 OS 레벨 플러시에 의존합니다. 명시적 플러시는 성능에 영향을 줄 수 있습니다.

#### log.flush.interval.ms

메시지가 디스크에 플러시되기까지의 최대 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

---

### 로그 클리너 설정 (Log Cleaner Configurations)

#### log.cleaner.enable

로그 클리너를 활성화합니다. 로그 컴팩션을 사용하는 토픽에 필요합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### log.cleaner.threads

로그 클리너에 사용할 백그라운드 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

#### log.cleaner.dedupe.buffer.size

전체 클리너 스레드에서 로그 중복 제거에 사용하는 총 메모리 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 134217728 (128MB) |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

#### log.cleaner.io.buffer.size

로그 클리너 I/O 버퍼의 총 메모리 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 524288 (512KB) |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

#### log.cleaner.backoff.ms

클리닝할 로그가 없을 때 대기할 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 15000 (15초) |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

#### log.cleanup.policy

보관 기간이 지난 로그 세그먼트에 적용할 정리 정책입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | delete |
| 유효값 | delete, compact, delete,compact |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

```properties
# 삭제 정책 (기본값)
log.cleanup.policy=delete

# 컴팩션 정책
log.cleanup.policy=compact

# 둘 다 사용
log.cleanup.policy=delete,compact
```

---

### 스레드 및 성능 설정 (Threading & Performance Configurations)

#### num.network.threads

네트워크 요청을 처리하는 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 3 |
| 유효값 | 1 이상 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

네트워크에서 요청을 수신하고 응답을 전송하는 역할을 합니다. 실제 요청 처리는 I/O 스레드에서 수행됩니다.

#### num.io.threads

요청을 처리하는 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 8 |
| 유효값 | 1 이상 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

디스크 I/O를 포함한 요청 처리를 담당합니다. 최소한 디스크 수만큼은 설정하는 것이 좋습니다.

#### num.replica.fetchers

소스 브로커에서 메시지를 복제하는 데 사용되는 페처(Fetcher) 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

이 값을 늘리면 팔로워 브로커의 I/O 병렬 처리 수준을 높일 수 있습니다.

#### num.recovery.threads.per.data.dir

브로커 시작 시 로그 복구와 종료 시 플러시에 사용되는 데이터 디렉토리당 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

#### background.threads

다양한 백그라운드 처리 작업에 사용되는 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 10 |
| 유효값 | 1 이상 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

#### queued.max.requests

네트워크 스레드를 블로킹하기 전, 데이터 플레인에서 허용하는 최대 대기 요청 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 유효값 | 1 이상 |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 복제 설정 (Replication Configurations)

#### default.replication.factor

자동 생성된 토픽의 기본 복제 팩터입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

프로덕션 환경에서는 3 이상 권장합니다.

```properties
default.replication.factor=3
```

#### min.insync.replicas

프로듀서가 `acks=all`로 설정된 경우, 쓰기가 성공으로 간주되기 위해 필요한 최소 동기화 레플리카 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효값 | 1 이상 |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

```properties
# 프로덕션 권장 설정
min.insync.replicas=2
```

#### replica.lag.time.max.ms

팔로워가 리더와 동기화되지 않은 상태를 허용하는 최대 시간입니다. 이 시간을 초과하면 해당 팔로워는 ISR(In-Sync Replicas)에서 제거됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### replica.fetch.wait.max.ms

팔로워 레플리카의 페치 요청 최대 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### replica.fetch.min.bytes

각 페치 응답에 기대하는 최소 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### replica.fetch.max.bytes

파티션당 페치할 최대 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1048576 (1MB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### replica.socket.timeout.ms

레플리카 네트워크 요청에 대한 소켓 타임아웃입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30000 (30초) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 메시지 크기 설정 (Message Size Configurations)

#### message.max.bytes

Kafka에서 허용하는 레코드 배치의 최대 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1048588 (~1MB) |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

컨슈머의 `fetch.message.max.bytes` 이상으로 설정해야 합니다.

```properties
# 10MB로 증가
message.max.bytes=10485760
```

#### replica.fetch.response.max.bytes

전체 페치 응답의 최대 바이트 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 10485760 (10MB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 토픽 설정 (Topic Configurations)

#### auto.create.topics.enable

서버에서 토픽 자동 생성을 활성화합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 중요도 | High |
| 업데이트 모드 | read-only |

프로덕션에서는 `false`로 설정하고 토픽을 명시적으로 생성하는 방식을 권장합니다.

```properties
auto.create.topics.enable=false
```

#### delete.topic.enable

토픽 삭제를 활성화합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### num.partitions

토픽당 기본 파티션 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효값 | 1 이상 |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

```properties
# 기본 파티션 수 증가
num.partitions=3
```

#### compression.type

토픽의 최종 압축 타입입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | producer |
| 유효값 | uncompressed, zstd, lz4, snappy, gzip, producer |
| 중요도 | High |
| 업데이트 모드 | cluster-wide |

```properties
# 브로커에서 압축 없이 저장
compression.type=uncompressed

# 프로듀서 설정 유지 (기본값)
compression.type=producer

# Zstandard 압축 적용
compression.type=zstd
```

---

### 소켓 설정 (Socket Configurations)

#### socket.send.buffer.bytes

소켓 서버의 SO_SNDBUF 버퍼 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 102400 (100KB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### socket.receive.buffer.bytes

소켓 서버의 SO_RCVBUF 버퍼 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 102400 (100KB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### socket.request.max.bytes

소켓 요청의 최대 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 104857600 (100MB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 연결 설정 (Connection Configurations)

#### connections.max.idle.ms

유휴 연결이 닫히기 전까지의 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 600000 (10분) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### max.connections

모든 IP에서 허용되는 최대 연결 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2147483647 (무제한) |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

#### max.connections.per.ip

각 IP 주소에서 허용되는 최대 연결 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2147483647 (무제한) |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

---

### 오프셋 관리 설정 (Offset Management Configurations)

#### offsets.topic.replication.factor

오프셋 토픽(`__consumer_offsets`)의 복제 팩터입니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 3 |
| 중요도 | High |
| 업데이트 모드 | read-only |

가용성 보장을 위해 클러스터 브로커 수 이하로 설정해야 합니다.

#### offsets.topic.num.partitions

오프셋 커밋 토픽의 파티션 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 50 |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### offsets.retention.minutes

컨슈머 그룹의 모든 컨슈머가 사라진 이후 오프셋을 보존하는 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 10080 (7일) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 트랜잭션 설정 (Transaction Configurations)

#### transaction.state.log.replication.factor

트랜잭션 토픽의 복제 팩터입니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 3 |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### transaction.state.log.num.partitions

트랜잭션 토픽의 파티션 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 50 |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### transaction.max.timeout.ms

트랜잭션에 허용되는 최대 타임아웃 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 900000 (15분) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### transactional.id.expiration.ms

트랜잭션 코디네이터가 트랜잭션 ID를 만료시키기 전까지 대기하는 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 604800000 (7일) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 그룹 코디네이터 설정 (Group Coordinator Configurations)

#### group.coordinator.threads

그룹 코디네이터가 사용하는 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.initial.rebalance.delay.ms

그룹 코디네이터가 첫 번째 리밸런스를 시작하기 전에 대기하는 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 3000 (3초) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.min.session.timeout.ms

등록된 컨슈머에 허용되는 최소 세션 타임아웃입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 6000 (6초) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.max.session.timeout.ms

등록된 컨슈머에 허용되는 최대 세션 타임아웃입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1800000 (30분) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.max.size

단일 컨슈머 그룹에 허용되는 최대 멤버 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2147483647 (무제한) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

---

### SSL/TLS 보안 설정 (SSL/TLS Security Configurations)

#### ssl.keystore.location

키스토어 파일의 위치입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | per-broker |

```properties
ssl.keystore.location=/var/private/ssl/server.keystore.jks
```

#### ssl.keystore.password

키스토어 파일의 비밀번호입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | per-broker |

#### ssl.key.password

키스토어의 개인 키 비밀번호입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | per-broker |

#### ssl.truststore.location

트러스트스토어 파일의 위치입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | per-broker |

```properties
ssl.truststore.location=/var/private/ssl/server.truststore.jks
```

#### ssl.truststore.password

트러스트스토어 파일의 비밀번호입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 중요도 | High |
| 업데이트 모드 | per-broker |

#### ssl.protocol

SSL 컨텍스트를 생성하는 데 사용되는 SSL 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | TLSv1.3 |
| 중요도 | Medium |
| 업데이트 모드 | per-broker |

#### ssl.enabled.protocols

SSL 연결에 활성화된 프로토콜 목록입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | TLSv1.2,TLSv1.3 |
| 중요도 | Medium |
| 업데이트 모드 | per-broker |

#### ssl.client.auth

클라이언트 인증 구성입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | none |
| 유효값 | required, requested, none |
| 중요도 | Medium |
| 업데이트 모드 | per-broker |

```properties
# 클라이언트 인증서 필수
ssl.client.auth=required

# 클라이언트 인증서 요청 (선택)
ssl.client.auth=requested
```

---

### SASL 보안 설정 (SASL Security Configurations)

#### sasl.enabled.mechanisms

Kafka 서버에서 활성화된 SASL 메커니즘 목록입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | GSSAPI |
| 중요도 | Medium |
| 업데이트 모드 | per-broker |

지원되는 메커니즘:
- `GSSAPI`: Kerberos
- `PLAIN`: 간단한 사용자명/비밀번호
- `SCRAM-SHA-256`: SCRAM 인증
- `SCRAM-SHA-512`: SCRAM 인증
- `OAUTHBEARER`: OAuth 2.0

```properties
sasl.enabled.mechanisms=PLAIN,SCRAM-SHA-256,SCRAM-SHA-512
```

#### sasl.mechanism.inter.broker.protocol

브로커 간 통신에 사용되는 SASL 메커니즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | GSSAPI |
| 중요도 | Medium |
| 업데이트 모드 | per-broker |

#### security.inter.broker.protocol

브로커 간 통신에 사용되는 보안 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효값 | PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

---

### 메타데이터 로그 설정 (KRaft Mode - Metadata Log Configurations)

#### metadata.log.dir

메타데이터 로그가 저장될 디렉토리입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null (log.dirs 사용) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### metadata.log.segment.bytes

단일 메타데이터 로그 파일의 최대 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1073741824 (1GB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### metadata.log.segment.ms

시간 기반 메타데이터 로그 세그먼트 롤오버 주기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 604800000 (7일) |
| 중요도 | High |
| 업데이트 모드 | read-only |

#### metadata.max.retention.bytes

메타데이터 로그의 최대 보관 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 104857600 (100MB) |
| 중요도 | High |
| 업데이트 모드 | read-only |

---

### 원격 스토리지 설정 (Remote/Tiered Storage Configurations)

#### remote.log.storage.system.enable

원격 로그 스토리지 시스템을 활성화합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### remote.log.manager.thread.pool.size

원격 로그 관리자가 사용하는 스레드 풀 크기입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### remote.log.reader.threads

원격 로그 읽기에 사용되는 스레드 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 10 |
| 중요도 | Medium |
| 업데이트 모드 | cluster-wide |

---

### 공유 그룹 설정 (Share Groups Configurations)

> 참고: 공유 그룹(Share Groups)은 Kafka 4.0에서 도입된 새로운 기능입니다.

#### group.share.max.size

공유 그룹의 최대 멤버 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 200 |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.share.record.lock.duration.ms

공유 그룹에서 레코드 잠금 유지 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30000 (30초) |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

#### group.share.delivery.count.limit

공유 그룹에서 레코드 전달 시도 제한입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 5 |
| 중요도 | Medium |
| 업데이트 모드 | read-only |

---

### 브로커 설정 동적 업데이트

Kafka는 브로커를 재시작하지 않고도 일부 설정을 동적으로 변경할 수 있습니다.

#### 클러스터 전체 업데이트 (cluster-wide)

```bash
# 로그 보관 시간 변경
bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type brokers \
  --entity-default \
  --alter \
  --add-config log.retention.hours=72
```

#### 개별 브로커 업데이트 (per-broker)

```bash
# 브로커 0의 리스너 변경
bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type brokers \
  --entity-name 0 \
  --alter \
  --add-config advertised.listeners=PLAINTEXT://new-host:9092
```

#### 설정 조회

```bash
# 클러스터 전체 동적 설정 조회
bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type brokers \
  --entity-default \
  --describe

# 특정 브로커 설정 조회
bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type brokers \
  --entity-name 0 \
  --describe
```

#### 설정 삭제 (기본값으로 복원)

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type brokers \
  --entity-default \
  --alter \
  --delete-config log.retention.hours
```

---

### 프로덕션 권장 설정 예시

#### KRaft 모드 (단일 결합 노드)

```properties
# 노드 식별
node.id=1
process.roles=broker,controller
controller.quorum.bootstrap.servers=localhost:9093

# 리스너 설정
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
advertised.listeners=PLAINTEXT://broker1.example.com:9092
controller.listener.names=CONTROLLER
inter.broker.listener.name=PLAINTEXT

# 로그 저장소
log.dirs=/var/kafka-logs

# 성능 튜닝
num.network.threads=8
num.io.threads=16
num.replica.fetchers=4
num.recovery.threads.per.data.dir=4

# 복제 및 내구성
default.replication.factor=3
min.insync.replicas=2

# 로그 보관
log.retention.hours=168
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000

# 토픽 관리
auto.create.topics.enable=false
delete.topic.enable=true
num.partitions=3

# 메시지 크기
message.max.bytes=10485760

# 오프셋 및 트랜잭션
offsets.topic.replication.factor=3
transaction.state.log.replication.factor=3
```

#### 대규모 클러스터 최적화

```properties
# 네트워크 버퍼 증가
socket.send.buffer.bytes=1048576
socket.receive.buffer.bytes=1048576

# 연결 관리
connections.max.idle.ms=600000
max.connections.per.ip=100

# 로그 클리너
log.cleaner.threads=2
log.cleaner.dedupe.buffer.size=268435456

# 복제 최적화
replica.lag.time.max.ms=30000
replica.fetch.max.bytes=10485760
```

---

### 참고 자료

- [Apache Kafka 공식 문서 - Broker Configs](https://kafka.apache.org/documentation/#brokerconfigs)
- [Apache Kafka 4.1 Configuration Reference](https://kafka.apache.org/41/configuration/broker-configs/)
- [Confluent Platform Broker Configuration](https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html)
- [Kafka Configuration Best Practices](https://kafka.apache.org/documentation/#config)

---

## Kafka 토픽 설정

> 원본: https://kafka.apache.org/documentation/#topicconfigs

### 목차

1. [개요](#개요)
2. [보존 및 정리 설정](#보존-및-정리-설정)
3. [압축 설정](#압축-설정)
4. [로그 컴팩션 설정](#로그-컴팩션-설정)
5. [세그먼트 관리 설정](#세그먼트-관리-설정)
6. [메시지 처리 설정](#메시지-처리-설정)
7. [복제 및 내구성 설정](#복제-및-내구성-설정)
8. [플러싱 및 인덱싱 설정](#플러싱-및-인덱싱-설정)
9. [계층형 스토리지 설정](#계층형-스토리지-설정)
10. [기타 설정](#기타-설정)

---

### 개요

토픽 레벨 설정은 각 토픽에 대해 개별적으로 구성할 수 있는 설정입니다. 모든 설정은 토픽 생성 시 또는 이후에 `kafka-configs.sh` 도구를 사용하여 변경할 수 있습니다. 토픽 레벨 설정은 브로커의 기본 설정보다 우선합니다.

---

### 보존 및 정리 설정

#### cleanup.policy

로그 세그먼트에 사용할 보존 정책을 지정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | delete |
| 유효한 값 | compact, delete |
| 중요도 | medium |

- `delete`: 오래된 세그먼트는 보존 시간 또는 크기 제한에 도달하면 삭제됩니다.
- `compact`: 로그 컴팩션이 활성화됩니다. 각 키의 최신 값만 유지됩니다.
- 쉼표로 구분된 목록(예: `"compact,delete"`)을 사용하여 두 정책을 모두 활성화할 수 있습니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config cleanup.policy=compact
```

#### retention.ms

오래된 로그 세그먼트를 폐기하기 전까지 유지할 최대 시간을 제어합니다. 세그먼트 생성 후 이 시간이 경과하면 삭제 대상이 됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 604800000 (7일) |
| 유효한 값 | [-1, ...] |
| 중요도 | medium |

- `-1`로 설정하면 시간 기반 보존이 비활성화됩니다(무제한 보존).
- 밀리초 단위입니다.

```bash
# 3일 보존 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config retention.ms=259200000
```

#### retention.bytes

오래된 세그먼트를 폐기하기 전 파티션의 최대 크기를 제어합니다. 파티션 크기가 이 값을 초과하면 가장 오래된 세그먼트부터 삭제됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | -1 (제한 없음) |
| 중요도 | medium |

- `-1`로 설정하면 크기 기반 보존이 비활성화됩니다.
- 파티션별 적용됩니다 (토픽 전체가 아님).

```bash
# 파티션당 1GB 제한 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config retention.bytes=1073741824
```

---

### 압축 설정

#### compression.type

토픽의 최종 압축 코덱을 지정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | producer |
| 유효한 값 | uncompressed, zstd, lz4, snappy, gzip, producer |
| 중요도 | medium |

- `producer`: 프로듀서가 설정한 압축 방식을 그대로 유지합니다.
- `uncompressed`: 압축을 사용하지 않습니다.
- `zstd`, `lz4`, `snappy`, `gzip`: 해당 압축 알고리즘을 사용합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config compression.type=lz4
```

#### compression.gzip.level

GZIP 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | -1 |
| 유효한 범위 | [1, ..., 9] 또는 -1 |

- `-1`은 기본 압축 레벨을 사용합니다.
- 높은 값일수록 더 높은 압축률을 제공하지만 CPU 사용량이 증가합니다.

#### compression.lz4.level

LZ4 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9 |
| 유효한 범위 | [1, ..., 17] |

#### compression.zstd.level

Zstandard 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 3 |
| 유효한 범위 | [-131072, ..., 22] |

---

### 로그 컴팩션 설정

로그 컴팩션은 각 메시지 키의 최신 값만 유지하여 로그 크기를 줄이는 메커니즘입니다.

#### min.cleanable.dirty.ratio

로그 컴팩터가 로그 정리를 시도하는 빈도를 제어합니다. 이 비율은 컴팩션 대상이 되는 "더티(dirty)" 로그 부분과 이미 컴팩션된 "클린(clean)" 로그 부분의 비율입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0.5 |
| 중요도 | medium |

- 값이 낮을수록 컴팩션이 더 자주 발생하지만 리소스를 더 많이 사용합니다.
- 값이 높을수록 컴팩션 빈도가 줄어들지만 로그가 더 커질 수 있습니다.

#### min.compaction.lag.ms

메시지가 컴팩션되지 않은 상태로 유지되는 최소 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0 |

- 이 시간이 지나기 전에는 메시지가 컴팩션되지 않습니다.
- 컨슈머가 모든 메시지를 읽을 수 있도록 하려면 이 값을 설정하세요.

#### max.compaction.lag.ms

메시지가 컴팩션 대상이 되기까지의 최대 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |

- 이 시간이 지나면 메시지는 컴팩션 대상이 됩니다.

#### delete.retention.ms

컴팩션된 토픽에서 삭제 톰스톤(tombstone) 마커를 보존하는 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 86400000 (1일) |

- 톰스톤은 키에 대한 삭제 표시입니다.
- 컨슈머가 삭제를 확인할 수 있도록 충분한 시간을 설정해야 합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config delete.retention.ms=172800000
```

---

### 세그먼트 관리 설정

#### segment.bytes

세그먼트 파일의 크기를 제어합니다. 세그먼트가 이 크기에 도달하면 새 세그먼트가 생성됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 1073741824 (1GB) |
| 유효한 범위 | [1048576, ...] |

- 작은 세그먼트는 보존 정책이 더 세밀하게 적용되지만 파일 핸들이 더 많이 필요합니다.
- 큰 세그먼트는 I/O 효율성이 높지만 보존 정책 적용이 덜 정밀합니다.

```bash
# 512MB 세그먼트 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config segment.bytes=536870912
```

#### segment.ms

Kafka가 로그 롤(roll)을 강제하는 시간 주기입니다. 세그먼트가 가득 차지 않아도 이 시간이 지나면 새 세그먼트가 생성됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 604800000 (7일) |

```bash
# 1일마다 새 세그먼트 생성
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config segment.ms=86400000
```

#### segment.index.bytes

오프셋-위치 인덱스의 크기를 제어합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 10485760 (10MB) |
| 유효한 범위 | [4, ...] |

- 인덱스 파일은 세그먼트 파일 내에서 오프셋을 빠르게 찾는 데 사용됩니다.

#### segment.jitter.ms

예정된 세그먼트 롤 시간에서 차감되는 최대 무작위 지터(jitter)입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0 |

- 여러 파티션의 세그먼트 롤이 동시에 발생하는 것을 방지합니다.
- I/O 스파이크를 줄이는 데 유용합니다.

---

### 메시지 처리 설정

#### max.message.bytes

Kafka가 허용하는 가장 큰 레코드 배치 크기입니다 (압축 후 크기).

| 속성 | 값 |
|------|------|
| 기본값 | 1048588 |
| 중요도 | medium |

- 이 값을 늘리면 `replica.fetch.max.bytes` 브로커 설정도 함께 늘려야 합니다.
- 컨슈머의 `fetch.max.bytes` 설정도 충분히 크게 설정해야 합니다.

```bash
# 최대 메시지 크기 10MB 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config max.message.bytes=10485760
```

#### message.timestamp.type

메시지 타임스탬프가 프로듀서 생성 시간을 기준으로 할지, 브로커 로그 추가 시간을 기준으로 할지 정의합니다.

| 속성 | 값 |
|------|------|
| 기본값 | CreateTime |
| 유효한 값 | CreateTime, LogAppendTime |

- `CreateTime`: 프로듀서가 메시지를 생성한 시간을 사용합니다.
- `LogAppendTime`: 브로커가 메시지를 로그에 추가한 시간을 사용합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config message.timestamp.type=LogAppendTime
```

#### message.timestamp.after.max.ms

메시지 타임스탬프가 브로커 시간보다 늦을 때 허용되는 최대 차이입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 3600000 (1시간) |

- 이 값을 초과하는 타임스탬프를 가진 메시지는 거부됩니다.

#### message.timestamp.before.max.ms

메시지 타임스탬프가 브로커 시간보다 앞설 때 허용되는 최대 차이입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |

---

### 복제 및 내구성 설정

#### min.insync.replicas

`acks=all` (또는 `acks=-1`)로 설정된 프로듀서의 쓰기가 성공하기 위해 필요한 최소 동기화 복제본(ISR) 수입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 1 |
| 유효한 범위 | [1, ...] |

- 이 값이 충족되지 않으면 프로듀서는 `NotEnoughReplicasException`을 받습니다.
- 데이터 내구성을 보장하려면 `min.insync.replicas=2`와 `replication.factor=3`을 함께 사용하는 것이 일반적입니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config min.insync.replicas=2
```

#### unclean.leader.election.enable

ISR(In-Sync Replica) 세트에 없는 복제본이 최후의 수단으로 리더가 될 수 있는지 여부를 허용합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`: 데이터 손실 가능성이 있지만 가용성이 향상됩니다.
- `false`: 데이터 손실을 방지하지만 ISR 복제본이 없으면 파티션을 사용할 수 없습니다.

> 주의: 이 설정을 `true`로 변경하면 데이터 손실이 발생할 수 있습니다.

#### leader.replication.throttled.replicas

리더 측에서 스로틀링할 복제본 목록입니다.

| 속성 | 값 |
|------|------|
| 기본값 | "" (빈 문자열) |

- 형식: `[partition_id]:[broker_id],[partition_id]:[broker_id]...`

#### follower.replication.throttled.replicas

팔로워 측에서 스로틀링할 복제본 목록입니다.

| 속성 | 값 |
|------|------|
| 기본값 | "" (빈 문자열) |

- 형식: `[partition_id]:[broker_id],[partition_id]:[broker_id]...`

---

### 플러싱 및 인덱싱 설정

#### flush.messages

메시지 쓰기 후 fsync를 강제하는 간격입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |
| 유효한 범위 | [1, ...] |

- 기본값은 OS의 백그라운드 플러시에 의존합니다.
- 낮은 값은 내구성을 높이지만 성능에 영향을 줍니다.

> 참고: 일반적으로 복제에 의존하는 것이 fsync에 의존하는 것보다 권장됩니다.

#### flush.ms

fsync 작업을 강제하는 시간 간격입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |
| 유효한 범위 | [0, ...] |

#### index.interval.bytes

Kafka가 오프셋 인덱스에 항목을 추가하는 빈도를 제어합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 4096 (4KB) |

- 이 바이트 수만큼 데이터가 추가될 때마다 인덱스 항목이 생성됩니다.
- 작은 값은 인덱스가 더 정밀해지지만 크기가 커집니다.

#### file.delete.delay.ms

파일 시스템에서 파일을 삭제하기 전 대기 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 60000 (1분) |

---

### 계층형 스토리지 설정

계층형 스토리지(Tiered Storage)를 사용하면 오래된 데이터를 원격 스토리지(예: S3, GCS)로 이동할 수 있습니다.

#### remote.storage.enable

토픽에 대해 계층형 스토리지를 활성화합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config remote.storage.enable=true
```

#### remote.log.copy.disable

계층형 데이터를 읽기 전용으로 만들어 업로드를 비활성화합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`로 설정하면 새 데이터가 원격 스토리지로 복사되지 않습니다.

#### remote.log.delete.on.disable

계층형 스토리지가 비활성화될 때 계층형 데이터를 삭제합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

#### local.retention.ms

로컬 로그가 삭제되기 전에 유지되는 시간(밀리초)입니다.

| 속성 | 값 |
|------|------|
| 기본값 | -2 |

- `-2`: `retention.ms` 값을 사용합니다.
- `-1`: 무제한 로컬 보존입니다.
- 계층형 스토리지가 활성화된 경우에만 적용됩니다.

#### local.retention.bytes

로컬 로그의 최대 크기입니다.

| 속성 | 값 |
|------|------|
| 기본값 | -2 |

- `-2`: `retention.bytes` 값을 사용합니다.
- `-1`: 무제한 로컬 보존입니다.
- 계층형 스토리지가 활성화된 경우에만 적용됩니다.

---

### 기타 설정

#### preallocate

새 로그 세그먼트에 대해 디스크의 파일을 미리 할당할지 여부입니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`로 설정하면 세그먼트 생성 시 전체 크기가 미리 할당됩니다.
- 일부 파일 시스템에서 성능을 향상시킬 수 있습니다.

---

### 설정 관리 방법

#### 토픽 생성 시 설정 적용

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic my-topic \
  --partitions 3 \
  --replication-factor 2 \
  --config retention.ms=604800000 \
  --config cleanup.policy=compact
```

#### 기존 토픽 설정 변경

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --alter \
  --add-config retention.ms=259200000,cleanup.policy=delete
```

#### 토픽 설정 조회

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --describe
```

#### 설정 삭제 (기본값으로 복원)

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --alter \
  --delete-config retention.ms
```

---

### 요약

Kafka 토픽 설정은 토픽별로 데이터 보존, 압축, 복제, 성능 등을 세밀하게 제어할 수 있게 합니다. 주요 포인트는 다음과 같습니다:

1. 보존 정책: `retention.ms`와 `retention.bytes`로 데이터 보존 기간과 크기를 제어합니다.
2. 정리 정책: `cleanup.policy`로 삭제(delete) 또는 컴팩션(compact) 방식을 선택합니다.
3. 복제 내구성: `min.insync.replicas`로 쓰기 내구성을 보장합니다.
4. 압축: `compression.type`으로 저장 효율성을 높일 수 있습니다.
5. 세그먼트 관리: `segment.bytes`와 `segment.ms`로 세그먼트 크기와 롤링 주기를 제어합니다.
6. 계층형 스토리지: `remote.storage.enable`로 비용 효율적인 장기 보관이 가능합니다.

---

### 참고 자료

- [Apache Kafka Documentation - Topic Configs](https://kafka.apache.org/documentation/#topicconfigs)
- [Apache Kafka Documentation - Broker Configs](https://kafka.apache.org/documentation/#brokerconfigs)
- [Apache Kafka Documentation - Producer Configs](https://kafka.apache.org/documentation/#producerconfigs)
- [Apache Kafka Documentation - Consumer Configs](https://kafka.apache.org/documentation/#consumerconfigs)
