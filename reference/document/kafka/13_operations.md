# Kafka 운영

> 이 문서는 Apache Kafka 공식 문서의 "Operations" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#operations

## 목차

1. [기본 Kafka 운영](#1-기본-kafka-운영)
2. [데이터센터](#2-데이터센터)
3. [지리적 복제 (클러스터 간 데이터 미러링)](#3-지리적-복제-클러스터-간-데이터-미러링)
4. [멀티 테넌시](#4-멀티-테넌시)
5. [Java 버전](#5-java-버전)
6. [하드웨어 및 OS](#6-하드웨어-및-os)
7. [모니터링](#7-모니터링)
8. [KRaft](#8-kraft)
9. [계층형 스토리지](#9-계층형-스토리지)
10. [컨슈머 리밸런스 프로토콜](#10-컨슈머-리밸런스-프로토콜)
11. [트랜잭션 프로토콜](#11-트랜잭션-프로토콜)
12. [적격 리더 레플리카 (ELR)](#12-적격-리더-레플리카-elr)

---

## 1. 기본 Kafka 운영

### 1.1 토픽 관리

#### 토픽 생성

`kafka-topics.sh`를 사용하여 파티션과 복제 계수를 지정하여 토픽을 생성합니다:

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic my_topic_name \
    --partitions 20 --replication-factor 3 --config x=y
```

주요 고려사항:
- 복제 계수(Replication factor): 중복성을 제어합니다. 3의 복제 계수는 데이터 손실 전에 2개의 브로커 장애를 허용합니다.
- 파티션 수: 데이터 샤딩과 컨슈머 병렬성을 결정합니다. 255자 폴더 이름 제약으로 인해 토픽 이름은 약 249자로 제한됩니다.

#### 토픽 수정

파티션 추가:

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic my_topic_name \
    --partitions 40
```

설정 추가:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics \
    --entity-name my_topic_name --alter --add-config x=y
```

설정 제거:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics \
    --entity-name my_topic_name --alter --delete-config x
```

토픽 삭제:

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic my_topic_name
```

> 주의: Kafka는 파티션 수 감소를 지원하지 않습니다.

---

### 1.2 브로커 운영

#### 정상 종료 (Graceful Shutdown)

브로커 재시작을 최적화하려면 제어된 종료를 활성화합니다:

```properties
controlled.shutdown.enable=true
```

장점:
- 디스크에 로그 동기화 (복구 시간 방지)
- 다른 레플리카로 리더십 자동 마이그레이션

#### 리더십 밸런싱

선호 레플리카의 자동 복원 활성화:

```properties
auto.leader.rebalance.enable=true
```

수동 리더십 복원:

```bash
bin/kafka-leader-election.sh --bootstrap-server localhost:9092 \
    --election-type preferred --all-topic-partitions
```

#### 랙 인식 (Rack Awareness)

물리적 랙에 걸쳐 레플리카 분산:

```properties
broker.rack=my-rack-id
```

이는 개별 브로커를 넘어 전체 랙 장애에 대한 장애 보장을 확장합니다.

---

### 1.3 컨슈머 그룹 관리

#### 컨슈머 위치 확인

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group
```

출력에는 현재 오프셋, 로그 끝 오프셋, 컨슈머 지연(lag)이 표시됩니다.

#### 컨슈머 그룹 목록

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

#### 그룹 상세 정보 확인

모든 멤버 조회:

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
    --group my-group --members
```

파티션 할당과 함께 상세 멤버 정보:

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
    --group my-group --members --verbose
```

그룹 상태 요약:

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
    --group my-group --state
```

#### 컨슈머 그룹 삭제

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete \
    --group my-group --group my-other-group
```

#### 컨슈머 오프셋 리셋

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets \
    --group my-group --topic topic1 --to-latest
```

리셋 시나리오 옵션:

| 옵션 | 설명 |
|------|------|
| `--to-datetime <String>` | 특정 날짜/시간으로 리셋 (형식: YYYY-MM-DDThh:mm:ss.sss) |
| `--to-earliest` | 가장 이른 사용 가능한 오프셋으로 이동 |
| `--to-latest` | 가장 최근 오프셋으로 이동 |
| `--shift-by <Long>` | 현재 위치를 N 오프셋만큼 조정 (양수/음수) |
| `--from-file` | 사용자 정의 오프셋 값이 있는 CSV 파일 사용 |
| `--to-current` | 현재 위치로 리셋 |
| `--by-duration <String>` | 기간별 오프셋 조정 (형식: PnDTnHnMnS) |
| `--to-offset` | 특정 오프셋 값 설정 |

실행 모드:
- 기본값: 제안된 변경 사항 표시
- `--execute`: 리셋 적용
- `--export`: 결과를 CSV로 출력

---

### 1.4 공유 그룹 관리 (Share Groups)

공유 그룹 목록:

```bash
bin/kafka-share-groups.sh --bootstrap-server localhost:9092 --list
```

그룹 상태 확인:

```bash
bin/kafka-share-groups.sh --bootstrap-server localhost:9092 --describe --group my-share-group
```

활성 멤버 조회:

```bash
bin/kafka-share-groups.sh --bootstrap-server localhost:9092 --describe \
    --group my-share-group --members
```

토픽 오프셋 삭제:

```bash
bin/kafka-share-groups.sh --bootstrap-server localhost:9092 --delete-offsets \
    --group my-share-group --topic topic1
```

공유 그룹 삭제:

```bash
bin/kafka-share-groups.sh --bootstrap-server localhost:9092 --delete --group my-share-group
```

---

### 1.5 클러스터 확장 및 파티션 재할당

#### 데이터 자동 마이그레이션

이동할 토픽 JSON 파일 생성:

```json
{
  "topics": [
    { "topic": "foo1" },
    { "topic": "foo2" }
  ],
  "version": 1
}
```

재할당 계획 생성:

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
    --topics-to-move-json-file topics-to-move.json --broker-list "5,6" --generate
```

재할당 실행:

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
    --reassignment-json-file expand-cluster-reassignment.json --execute
```

상태 확인:

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
    --reassignment-json-file expand-cluster-reassignment.json --verify
```

#### 사용자 정의 파티션 할당

사용자 정의 재할당 JSON 생성:

```json
{
  "version": 1,
  "partitions": [
    {"topic": "foo1", "partition": 0, "replicas": [5, 6]},
    {"topic": "foo2", "partition": 1, "replicas": [2, 3]}
  ]
}
```

위와 같이 `--execute`와 `--verify`로 실행합니다.

---

### 1.6 복제 관리

#### 복제 계수 증가

추가 레플리카가 포함된 JSON 생성:

```json
{
  "version": 1,
  "partitions": [
    {"topic": "foo", "partition": 0, "replicas": [5, 6, 7]}
  ]
}
```

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
    --reassignment-json-file increase-replication-factor.json --execute
```

확인:

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic foo --describe
```

---

### 1.7 대역폭 스로틀링

재할당 중 스로틀 적용:

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --execute \
    --reassignment-json-file bigger-cluster.json --throttle 50000000 \
    --replica-alter-log-dirs-throttle 100000000
```

리밸런스 중 스로틀 수정:

```bash
bin/kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --additional \
    --execute --reassignment-json-file bigger-cluster.json --throttle 700000000
```

스로틀 설정 조회:

```bash
bin/kafka-configs.sh --describe --bootstrap-server localhost:9092 --entity-type brokers
```

스로틀된 레플리카 조회:

```bash
bin/kafka-configs.sh --describe --bootstrap-server localhost:9092 --entity-type topics
```

#### 안전한 스로틀링 실천 방법

1. 완료 후 즉시 스로틀 제거 (`--verify` 통해)
2. ConsumerLag 메트릭으로 복제 진행 상황 모니터링
3. 진행 유지를 위해 스로틀이 들어오는 쓰기 속도를 초과하도록 보장

---

### 1.8 쿼터 설정

#### 사용자 및 클라이언트 쿼터 설정

특정 사용자와 클라이언트:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type users --entity-name user1 --entity-type clients --entity-name clientA
```

사용자만:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type users --entity-name user1
```

클라이언트 ID만:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type clients --entity-name clientA
```

#### 기본 쿼터 설정

사용자의 기본 클라이언트 ID 쿼터:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type users --entity-name user1 --entity-type clients --entity-default
```

기본 사용자 쿼터:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type users --entity-default
```

기본 클라이언트 ID 쿼터:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' \
    --entity-type clients --entity-default
```

#### 쿼터 조회

특정 (사용자, 클라이언트 ID):

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe \
    --entity-type users --entity-name user1 --entity-type clients --entity-name clientA
```

모든 사용자:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe --entity-type users
```

기본 쿼터:

```bash
bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe \
    --entity-type users --entity-default
```

---

### 1.9 브로커 해제 (Decommissioning)

재할당 도구는 브로커 제거를 위한 수동 계획이 필요합니다. 관리자는 대상 브로커의 모든 파티션을 나머지 브로커로 이동하는 사용자 정의 재할당 계획을 만들어야 하며, 레플리카가 단일 대체 브로커에 집중되지 않도록 해야 합니다.

---

## 2. 데이터센터

### 2.1 권장 배포 패턴

"각 데이터센터에 로컬 Kafka 클러스터를 배포하고, 각 데이터센터의 애플리케이션 인스턴스가 로컬 클러스터와만 상호 작용하며 클러스터 간에 데이터를 미러링"하는 것이 권장됩니다.

이 접근 방식의 장점:
- 각 데이터센터가 독립적인 개체로 운영됨
- 데이터센터 간 복제를 중앙에서 관리하고 튜닝할 수 있음
- 데이터센터 간 링크가 실패해도 개별 시설이 작동할 수 있음
- 연결이 복원되면 미러링이 자동으로 재개됨

### 2.2 글로벌 데이터 액세스 전략

모든 위치에서 포괄적인 데이터 가시성이 필요한 애플리케이션의 경우, "모든 데이터센터의 로컬 클러스터에서 집계 데이터를 미러링하는 클러스터"가 권장됩니다. 이러한 집계 클러스터는 전체 데이터셋이 필요한 애플리케이션에 읽기 전용 용도로 사용됩니다.

### 2.3 높은 지연 시간 WAN 연결

원격 클러스터에서 읽거나 쓰는 것은 가능하지만 추가 지연 시간이 발생합니다. 프로듀서와 컨슈머의 Kafka 배치 메커니즘은 "높은 지연 시간 연결에서도 높은 처리량"을 가능하게 합니다.

WAN 링크에서 성능을 최적화하려면 `socket.send.buffer.bytes`와 `socket.receive.buffer.bytes` 설정을 사용하여 프로듀서, 컨슈머, 브로커의 TCP 소켓 버퍼 크기를 늘려야 합니다.

### 2.4 안티패턴 경고

"높은 지연 시간 링크를 통해 여러 데이터센터에 걸쳐 있는 단일 Kafka 클러스터"는 복제 지연 시간과 가용성 문제로 인해 명시적으로 권장되지 않습니다.

---

## 3. 지리적 복제 (클러스터 간 데이터 미러링)

### 3.1 개요

Kafka의 지리적 복제 기능을 통해 관리자는 "개별 Kafka 클러스터, 데이터센터 또는 지리적 지역의 경계를 넘는 데이터 흐름"을 정의할 수 있습니다.

이는 Kafka Connect 프레임워크에 구축된 MirrorMaker 2 를 통해 수행되며, "다른 Kafka 환경 간에 스트리밍 방식으로 데이터를 복제하는 도구"입니다.

### 3.2 주요 기능

MirrorMaker 지원 기능:
- 데이터 및 설정과 함께 토픽 복제
- 클러스터 간 컨슈머 그룹 및 오프셋 마이그레이션
- ACL 복제
- 파티션 보존
- 새로운 토픽 및 파티션 자동 감지
- 종단 간 복제 지연 시간 메트릭
- 내결함성, 수평 확장 가능한 운영

### 3.3 복제 흐름

방향성 데이터 이동을 "복제 흐름(replication flows)"이라고 하며, `{source_cluster}->{target_cluster}` 형식을 사용합니다. 일반적인 패턴:

- Active/Active: `A->B, B->A`
- Active/Passive: `A->B`
- 집계(Aggregation): `A->K, B->K, C->K`
- 팬아웃(Fan-out): `K->A, K->B, K->C`

### 3.4 설정 구문

#### 기본 설정 구조

```properties
clusters = primary, secondary
primary.bootstrap.servers = broker3-primary:9092
secondary.bootstrap.servers = broker5-secondary:9092

primary->secondary.enabled = true
primary->secondary.topics = foobar-topic, quux-.*
```

#### 주요 설정 매개변수

| 설정 | 용도 |
|------|------|
| `clusters` | 쉼표로 구분된 클러스터 별칭 |
| `{cluster}.bootstrap.servers` | 특정 클러스터의 연결 정보 |
| `{source}->{target}.enabled` | 복제 흐름 활성화 (true/false) |
| `topics` | 복제할 토픽 (정규식 지원; 기본값: `.*`) |
| `topics.exclude` | 복제에서 제외할 토픽 |
| `groups` | 복제할 컨슈머 그룹 |
| `groups.exclude` | 복제에서 제외할 컨슈머 그룹 |

#### 사용자 정의 클라이언트 설정

프로듀서, 컨슈머, 관리 클라이언트의 형식:

```properties
{source}.consumer.{config_name}
{target}.producer.{config_name}
{source_or_target}.admin.{config_name}
```

예시:

```properties
us-west.consumer.isolation.level = read_committed
us-east.producer.compression.type = gzip
us-east.producer.buffer.memory = 32768
```

### 3.5 보안 설정

MirrorMaker는 SSL 암호화를 지원합니다. 예시:

```properties
us-east.security.protocol=SSL
us-east.ssl.truststore.location=/path/to/truststore.jks
us-east.ssl.truststore.password=my-secret-password
us-east.ssl.keystore.location=/path/to/keystore.jks
us-east.ssl.keystore.password=my-secret-password
us-east.ssl.key.password=my-secret-password
```

### 3.6 정확히 한 번 시맨틱스 (Exactly-Once, v3.5.0+)

대상 클러스터에 활성화:

```properties
us-east.exactly.once.source.support = enabled
```

기존 클러스터의 경우 2단계 업그레이드 사용: 먼저 `preparing`으로 설정한 다음 `enabled`로 설정합니다.

클러스터 내 통신 활성화:

```properties
dedicated.mode.enable.internal.rest = true
listeners = http://localhost:8080
```

중단된 트랜잭션 필터링:

```properties
us-west.consumer.isolation.level = read_committed
```

### 3.7 대상 클러스터의 토픽 명명

기본적으로 복제된 토픽은 `{source}.{source_topic_name}` 형식을 따릅니다. 예시:

```
us-west 클러스터          us-east 클러스터
─────────────            ───────────────
foo-topic           -->  us-west.foo-topic
```

구분자 사용자 정의:

```properties
us-west->us-east.replication.policy.separator = _
```

### 3.8 MirrorMaker 시작

기본 시작:

```bash
bin/connect-mirror-maker.sh connect-mirror-maker.properties
```

클러스터 지정 (지연 시간 감소를 위해 권장):

```bash
bin/connect-mirror-maker.sh connect-mirror-maker.properties \
    --clusters us-west
```

> 참고: MirrorMaker 프로세스가 처음 데이터 복제를 시작하기까지 몇 분이 걸릴 수 있습니다.

### 3.9 MirrorMaker 중지

```bash
kill <MirrorMaker pid>
```

### 3.10 설정 예시

#### Active/Passive 배포

```properties
primary.bootstrap.servers = broker1-primary:9092
secondary.bootstrap.servers = broker2-secondary:9092

primary->secondary.enabled = true
secondary->primary.enabled = false

primary->secondary.topics = foo.*
```

#### Active/Active 배포

```properties
clusters = us-west, us-east
us-west.bootstrap.servers = broker1-west:9092,broker2-west:9092
us-east.bootstrap.servers = broker3-east:9092,broker4-east:9092

us-west->us-east.enabled = true
us-east->us-west.enabled = true
```

#### 데이터센터 분리가 있는 멀티 클러스터

```properties
clusters: west-1, west-2, east-1, east-2, north-1, north-2

# 데이터센터 내 복제
west-1->west-2.enabled = true
west-2->west-1.enabled = true

# 데이터센터 간 복제
west-1->east-1.enabled = true
west-1->north-1.enabled = true
east-1->west-1.enabled = true
```

데이터센터별 실행:

```bash
# West DC에서:
bin/connect-mirror-maker.sh connect-mirror-maker.properties --clusters west-1 west-2

# East DC에서:
bin/connect-mirror-maker.sh connect-mirror-maker.properties --clusters east-1 east-2
```

### 3.11 중요한 모범 사례

원격에서 소비, 로컬에서 생산: 지연 시간을 최소화하기 위해 대상 클러스터 가까이에 MirrorMaker 프로세스를 배치합니다. 이유: "Kafka 프로듀서는 일반적으로 Kafka 컨슈머보다 신뢰할 수 없거나 높은 지연 시간의 네트워크 연결에서 더 어려움을 겪습니다."

설정 일관성: 동일한 클러스터를 대상으로 하는 복제 흐름 간에 MirrorMaker 설정을 일관되게 유지하여 충돌을 방지합니다.

컨슈머 그룹 테스트: 기본적으로 콘솔 컨슈머 그룹은 제외됩니다. 그룹 복제 테스트 시 `groups.exclude`를 수정하세요.

### 3.12 모니터링

MirrorMaker는 다음으로 태그된 `kafka.connect.mirror` 그룹 아래에 메트릭을 발행합니다:
- `source`: 소스 클러스터 별칭
- `target`: 대상 클러스터 별칭
- `topic`: 대상의 복제된 토픽
- `partition`: 복제 중인 파티션

#### 주요 메트릭

소스 커넥터 메트릭:
- `record-count`: 복제된 레코드 수
- `record-rate`: 초당 레코드
- `record-age-ms`: 복제 시 레코드 수명
- `replication-latency-ms`: 전파 시간
- `byte-rate` / `byte-count`: 처리량 메트릭

체크포인트 커넥터 메트릭:
- `checkpoint-latency-ms`: 컨슈머 오프셋 복제 시간

### 3.13 설정 변경 프로세스

설정 변경을 적용하려면 MirrorMaker 프로세스를 재시작해야 합니다.

---

## 4. 멀티 테넌시

### 4.1 개요

"고도로 확장 가능한 이벤트 스트리밍 플랫폼인 Kafka는 많은 사용자에 의해 중앙 신경 시스템으로 사용되며, 다양한 팀과 사업 부문의 광범위한 시스템과 애플리케이션을 실시간으로 연결합니다."

멀티 테넌시는 6가지 핵심 영역을 포함합니다:
1. 사용자 공간 생성
2. 토픽 설정
3. 클러스터/토픽 보안
4. 쿼터를 통한 테넌트 격리
5. 모니터링
6. 클러스터 간 데이터 공유

### 4.2 토픽 명명을 통한 사용자 공간 생성

Kafka 관리자는 보안 제어와 결합된 계층적 토픽 명명 구조를 통해 격리된 환경을 구축합니다. 두 가지 일반적인 조직적 접근 방식:

팀/조직 단위별:
- 패턴: `<organization>.<team>.<dataset>.<event-name>`
- 예시: `acme.infosec.telemetry.logins`

프로젝트/제품별:
- 패턴: `<project>.<product>.<event-name>`
- 예시: `mobility.payments.suspicious`

#### 시행 방법

- 접두사 ACL (KIP-290): 특정 접두사로 토픽 생성 제한
- 사용자 정의 CreateTopicPolicy (KIP-108): 설정을 통해 엄격한 명명 패턴 시행
- 사용자 토픽 생성 비활성화: 관리자 승인 필요
- 자동 생성 비활성화: 브로커에서 `auto.create.topics.enable=false` 설정

### 4.3 토픽 설정: 보존 정책

"관리자는 종종 `retention.bytes`(크기) 및 `retention.ms`(시간)와 같은 설정으로 토픽에 데이터가 저장되는 양과 기간을 제어하는 데이터 보존 정책을 정의해야 합니다."

이 접근 방식은 스토리지 소비를 제한하고 법적 규정 준수(GDPR)를 지원합니다.

### 4.4 보안: 인증, 권한 부여, 암호화

세 가지 핵심 보안 범주가 적용됩니다:

1. 암호화
브로커-클라이언트 간, 브로커 간, 선택적 도구 간 데이터 보호

2. 인증
클라이언트/애플리케이션의 브로커 연결 및 브로커 간 통신

3. 권한 부여
클라이언트 작업 제어: 토픽 관리, 이벤트 읽기/쓰기 액세스, ACL 수정

#### ACL 관리 예시

InfoSec 팀의 Alice 사용자:

```bash
bin/kafka-acls.sh \
    --bootstrap-server localhost:9092 \
    --add --allow-principal User:Alice \
    --producer \
    --resource-pattern-type prefixed --topic acme.infosec.
```

"특히 멀티 테넌트 환경의 관리자는 계층적 토픽 명명 구조를 마련함으로써 접두사 ACL을 통해 토픽에 대한 액세스를 편리하게 제어할 수 있다는 이점을 얻습니다."

### 4.5 테넌트 격리: 쿼터 및 속도 제한

클라이언트 쿼터:
- 요청 속도 쿼터는 사용자당 브로커 CPU 사용을 제한
- 네트워크 대역폭 쿼터는 들어오는/나가는 트래픽 제어
- 토픽 작업 쿼터(`controller_mutation_rate`)는 동시 작업으로 인한 클러스터 과부하 방지

"요청 속도 쿼터는 해당 사용자에 대해 브로커가 요청 처리 경로에서 소비하는 시간을 제한하여 사용자가 브로커 CPU 사용량에 미치는 영향을 제한하는 데 도움이 되며, 이후 스로틀링이 시작됩니다."

서버 쿼터:
- 연결 속도 제한
- 브로커당 최대 연결 수
- IP 기반 연결 제한

### 4.6 모니터링 및 미터링

"Kafka는 실패한 인증 시도 속도, 요청 지연 시간, 컨슈머 지연, 총 컨슈머 그룹 수, 이전 섹션에서 설명한 쿼터에 대한 메트릭 등 광범위한 메트릭을 지원합니다."

주요 메트릭 예시: JMX 메트릭 `kafka.log.Log.Size.<TOPIC-NAME>`을 사용하여 토픽-파티션 크기를 추적하고 스토리지 초과에 대해 알림을 설정합니다.

### 4.7 멀티 테넌시와 지리적 복제

Kafka는 재해 복구 및 테넌트 간 시나리오를 위한 클러스터 간 데이터 공유를 가능하게 합니다. 구현 세부 사항은 지리적 복제 문서를 참조하세요.

### 4.8 추가 고려사항: 데이터 계약

"클러스터의 데이터 생산자와 소비자 간에 이벤트 스키마를 사용하여 데이터 계약을 정의해야 할 수 있습니다."

이벤트 형식을 검증하고, 손상된 데이터를 방지하며, 스키마 진화를 관리하기 위해 스키마 레지스트리를 배포하세요. 단, Kafka 자체는 이 구성 요소를 포함하지 않습니다.

---

## 5. Java 버전

### 5.1 지원되는 Java 버전

Apache Kafka 문서에 따르면:
- Java 17 및 Java 21 이 완전히 지원됩니다
- Java 11 은 일부 모듈(클라이언트, 스트림 및 관련 모듈)에 대해 지원됩니다
- 가장 최근 LTS 이후의 새 버전은 최선의 노력(best-effort)으로 지원됩니다

### 5.2 권장 사항

"성능, 효율성 및 지원 측면에서 가장 최근의 LTS 릴리스(작성 시점 기준 Java 21)"가 선호됩니다. 또한 보안을 위해 최신 패치 버전을 실행해야 합니다. 이전 버전은 종종 공개된 취약점을 포함하고 있습니다.

### 5.3 권장 JVM 인수

OpenJDK 기반 구현의 경우, Kafka는 다음과 같은 일반적인 시작 매개변수를 제안합니다:

```
-Xmx6g -Xms6g -XX:MetaspaceSize=96m -XX:+UseG1GC
-XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35
-XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50
-XX:MaxMetaspaceFreeRatio=80 -XX:+ExplicitGCInvokesConcurrent
```

이러한 인수는 프로덕션 클러스터에서 초당 1회 미만의 Young GC와 함께 약 21ms GC 일시 중지 시간(90번째 백분위수)을 달성합니다.

---

## 6. 하드웨어 및 OS

### 6.1 하드웨어 사양

Kafka는 24GB RAM이 장착된 듀얼 쿼드코어 Intel Xeon 머신에서 잘 작동합니다. 메모리 요구 사항은 활성 리더와 라이터의 버퍼링에 따라 달라집니다. "write_throughput x 30"초의 버퍼링으로 필요량을 추정하세요.

디스크 처리량이 주요 성능 병목입니다. "8x7200 rpm SATA 드라이브"가 기준 구성으로 참조되지만, 일반적으로 더 많은 드라이브가 성능을 향상시킵니다. 플러시 동작 설정에 따라 비싼 고RPM SAS 드라이브가 의미 있는 이점을 제공하는지 결정됩니다.

### 6.2 운영 체제 지원

Kafka는 Linux 및 Solaris를 포함한 Unix 시스템에서 잘 실행됩니다. Windows는 개선 가능성에도 불구하고 포괄적인 지원이 부족합니다. 세 가지 중요한 설정 외에는 최소한의 OS 수준 튜닝만 필요합니다.

### 6.3 OS 수준 설정

파일 디스크립터: 브로커 프로세스의 경우 최소 100,000으로 설정합니다. Kafka는 로그 세그먼트와 연결에 파일 디스크립터가 필요합니다. 많은 파티션을 호스팅하는 브로커는 "(파티션 수) x (파티션 크기/세그먼트 크기)" + 연결 디스크립터가 필요합니다.

소켓 버퍼 크기: 데이터센터 간 고성능 데이터 전송을 가능하게 하려면 최대 소켓 버퍼 크기를 늘립니다.

메모리 매핑 (vm.max_map_count): 약 65,535의 기본 Linux 값은 높은 파티션 배포에 불충분합니다. 각 로그 세그먼트는 2개의 맵 영역이 필요합니다. 50,000개의 파티션은 100,000개 이상의 맵 영역이 필요하며, 이는 OutOfMemoryError 충돌을 유발합니다.

### 6.4 디스크 및 파일시스템 전략

애플리케이션 로그 및 OS 활동과 분리된 Kafka 데이터용 여러 전용 드라이브를 사용합니다. RAID로 드라이브를 단일 볼륨으로 구성하거나 각각을 독립적인 디렉토리로 포맷합니다. 파티션은 디렉토리 간에 라운드 로빈으로 분산됩니다.

RAID는 잠재적인 부하 분산을 제공하지만 쓰기 처리량과 사용 가능한 공간을 줄입니다. RAID 재구축은 I/O 집약적이어서 서버를 효과적으로 비활성화하여 실제 가용성 이점을 제한합니다.

### 6.5 플러시 관리

Kafka는 "모든 데이터를 파일시스템에 즉시 쓰지만" 강제 디스크 쓰기는 지연시킵니다. "애플리케이션 fsync를 완전히 비활성화하는 기본 플러시 설정"을 사용하고 대신 OS 백그라운드 플러싱에 의존하는 것이 권장됩니다. 이 접근 방식은 "튜닝할 노브가 없고, 훌륭한 처리량과 지연 시간, 완전한 복구 보장"을 제공합니다.

애플리케이션 수준 fsync 정책은 비효율성과 지연 시간을 초래합니다. 대부분의 Linux 파일시스템은 fsync 중에 쓰기를 차단하는 반면 백그라운드 플러싱은 세분화된 페이지 수준 잠금을 사용합니다.

### 6.6 파일시스템 선택

EXT4와 XFS가 가장 많이 사용되는 옵션입니다. XFS는 "최고의 EXT4 구성에 대해 160ms 대 250ms+"로 우수한 성능 특성을 보여주며 디스크 성능 변동이 적습니다.

마운트 옵션 (모든 파일시스템):
- `noatime`: 액세스 시간 업데이트를 비활성화하여 Kafka 작업에 영향을 주지 않으면서 불필요한 쓰기를 제거

XFS:
- 기본 튜닝이 필요 없음; 자동 튜닝이 대부분의 최적화 처리
- 선택 사항: `largeio`(최소한의 실용적 효과) 및 `nobarrier`(배터리 지원 캐시용)

EXT4 (성능을 위해 튜닝 필요):
- `data=writeback`: 지연 시간을 크게 줄이기 위해 순서 제약 제거
- 저널링 비활성화로 쓰기 지연 시간 급증 감소 (재부팅이 느려질 수 있음)
- `commit=num_secs`: 높은 값은 처리량 향상; 낮은 값은 충돌 시 데이터 손실 감소
- `nobh`: 처리량과 지연 시간 향상
- `delalloc`: 순차 쓰기와 처리량 최적화
- `fast_commit`: Linux 5.10+에서 사용 가능, 지연 시간이 감소된 경량 저널링 제공

---

## 7. 모니터링

### 7.1 개요

Kafka는 Yammer Metrics (서버 측)와 Kafka Metrics (클라이언트 측)를 통해 모니터링을 구현하며, 둘 다 외부 모니터링 시스템을 위한 플러그인 가능한 리포터와 함께 JMX를 통해 메트릭을 노출합니다.

핵심 원칙: "모든 Kafka 속도 메트릭에는 접미사 `-total`이 있는 해당 누적 카운트 메트릭이 있습니다."

### 7.2 중요 모니터링 메트릭

#### 브로커 수준 메트릭

메시지 흐름:
- 메시지 수집 속도 및 바이트 속도 (클라이언트 및 다른 브로커로의 입출력)
- 토픽별 생산/페치 요청 속도
- 크기 제약으로 인해 거부된 바이트 속도

복제 상태:
- 언더 레플리케이티드 파티션 (0이어야 함)
- ISR(In-Sync Replica) 축소/확장 속도
- 팔로워와 리더 레플리카 간 최대 지연

요청 성능:
- 요청 큐 크기 및 지연 시간 분석 (큐, 로컬, 원격, 응답 시간)
- 요청 유형별 오류율
- 네트워크 프로세서 유휴 비율 (이상적으로 >0.3)

컨트롤러 메트릭:
- 활성 컨트롤러 수 (클러스터당 정확히 1개)
- 리더 선출 속도
- 불완전한 리더 선출 (0이어야 함)
- 보류 중인 토픽/레플리카 삭제

#### 컨슈머/프로듀서 메트릭

프로듀서:
- 전송된 레코드 속도 및 오류율
- 요청 지연 시간 및 진행 중인 요청
- 배치 크기 및 압축 비율
- 버퍼 가용성 및 고갈 이벤트

컨슈머:
- 소비된 레코드 속도
- 파티션별 컨슈머 지연
- 페치 지연 시간 및 스로틀 시간
- 리밸런스 빈도 및 지연 시간
- 하트비트 상태

### 7.3 모니터링 모범 사례

1. 메트릭 접근: "사용 가능한 메트릭을 확인하는 가장 쉬운 방법은 jconsole을 실행하고 실행 중인 Kafka 클라이언트 또는 서버를 가리키는 것입니다."

2. 보안 우선: 원격 JMX는 기본적으로 비활성화되어 있습니다. 프로덕션의 경우: `JMX_PORT` 환경 변수를 통해 활성화하지만 "프로덕션 시나리오에서 원격 JMX를 활성화할 때 반드시 보안을 활성화해야 합니다."

3. 주요 상태 지표:
   - 디스크 사용률 및 로그 디렉토리 오프라인 상태
   - GC 시간 및 CPU 사용률
   - I/O 서비스 시간
   - 토픽별 처리량 추세

4. 컨슈머 생존력: "컨슈머가 따라잡으려면 최대 지연이 임계값보다 작아야 하고 최소 페치 속도가 0보다 커야 합니다."

5. 쿼터 모니터링: 사용자/클라이언트별 대역폭 및 요청 쿼터에 대한 스로틀 시간 추적

### 7.4 전문 모니터링 영역

- 그룹 조정: 파티션 로딩 시간, 이벤트 큐 메트릭, 리밸런스 속도
- 계층형 스토리지: 원격 페치/복사 속도 및 오류, 지연 바이트/세그먼트
- KRaft 클러스터: 쿼럼 상태, 메타데이터 버전, 컨트롤러 메트릭
- Streams 애플리케이션: 스레드 상태, 태스크 메트릭, 프로세서 노드 지연 시간, 상태 저장소 작업

JMX 메트릭 이름은 `kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec`와 같은 패턴을 따라 표준 모니터링 플랫폼과의 통합을 가능하게 합니다.

---

## 8. KRaft

### 8.1 개요

KRaft(Kafka Raft)는 ZooKeeper를 대체하는 클러스터 메타데이터 관리를 위한 Apache Kafka의 합의 프로토콜입니다. 현대 Kafka 배포에 권장되는 아키텍처입니다.

### 8.2 프로세스 역할 설정

KRaft 모드의 Kafka 서버는 세 가지 구별되는 역할로 구성할 수 있습니다:

- 브로커 전용: `process.roles=broker` - 메시지 생산 및 소비 처리
- 컨트롤러 전용: `process.roles=controller` - 클러스터 메타데이터 관리
- 결합: `process.roles=broker,controller` - 두 기능 모두 수행

> "결합 서버는 개발 환경과 같은 소규모 사용 사례에 대해 운영하기가 더 간단"하지만 컨트롤러 격리 감소로 인해 "중요한 배포 환경에서는 결합 모드가 권장되지 않습니다."

### 8.3 컨트롤러 아키텍처

#### 쿼럼 설정

컨트롤러는 메타데이터 쿼럼에 참여합니다. 권장 구성은 3개 또는 5개의 컨트롤러를 사용합니다:
- 3개의 컨트롤러: 1개의 동시 장애 허용
- 5개의 컨트롤러: 2개의 동시 장애 허용

모든 서버는 `controller.quorum.bootstrap.servers` 속성을 통해 활성 컨트롤러를 발견해야 합니다:

```properties
controller.quorum.bootstrap.servers=host1:port1,host2:port2,host3:port3
```

#### 컨트롤러 설정 예시

```properties
process.roles=controller
node.id=1
listeners=CONTROLLER://controller1.example.com:9093
controller.quorum.bootstrap.servers=controller1.example.com:9093,\
  controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

### 8.4 정적 vs 동적 쿼럼

정적 쿼럼 (레거시):
- `controller.quorum.voters` 설정 필요
- 모든 컨트롤러 세부 정보가 모든 노드 설정에 하드코딩됨
- `kraft.version=0`과 연관

동적 쿼럼 (권장):
- 대신 `controller.quorum.bootstrap.servers` 사용
- 설정 파일 업데이트 없이 컨트롤러 멤버십 변경 허용
- `kraft.version=1` 필요 (Kafka 4.1+)

쿼럼 유형 확인:

```bash
bin/kafka-features.sh --bootstrap-controller localhost:9093 describe
```

### 8.5 노드 프로비저닝

#### 클러스터 ID 생성

```bash
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

#### 단일 컨트롤러 부트스트랩

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> \
  --standalone --config config/controller.properties
```

이 명령은 meta.properties 파일과 노드를 유일한 쿼럼 투표자로 만드는 제어 레코드가 있는 초기 스냅샷을 생성합니다.

#### 다중 컨트롤러 부트스트랩

```bash
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_0_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_1_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_2_UUID="$(bin/kafka-storage.sh random-uuid)"

bin/kafka-storage.sh format --cluster-id ${CLUSTER_ID} \
  --initial-controllers \
  "0@controller-0:1234:${CONTROLLER_0_UUID},\
   1@controller-1:1234:${CONTROLLER_1_UUID},\
   2@controller-2:1234:${CONTROLLER_2_UUID}" \
  --config config/controller.properties
```

플래그 값은 동일한 클러스터 ID를 가진 모든 컨트롤러에서 동일해야 합니다.

#### 브로커 및 새 컨트롤러 포맷

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> \
  --config config/server.properties --no-initial-controllers
```

### 8.6 동적 쿼럼으로 업그레이드

#### 1단계: 현재 버전 확인

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 describe
```

`kraft.version`이 `FinalizedVersionLevel: 0`을 표시하면 업그레이드가 필요합니다.

#### 2단계: 기능 버전 업그레이드

최신 버전으로 업그레이드:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 \
  upgrade --release-version 4.1
```

또는 KRaft만 특별히 업그레이드:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 \
  upgrade --feature kraft.version=1
```

#### 3단계: 설정 파일 업데이트

`controller.quorum.voters`를 제거하고 `controller.quorum.bootstrap.servers`를 추가:

```properties
process.roles=...
node.id=...
controller.quorum.bootstrap.servers=controller1.example.com:9093,\
  controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

모든 클러스터 노드에 적용합니다.

### 8.7 컨트롤러 멤버십 관리

#### 새 컨트롤러 추가

새 노드를 프로비저닝하고 시작한 다음 복제를 기다립니다:

```bash
bin/kafka-metadata-quorum.sh describe --replication
```

따라잡으면 추가합니다:

```bash
bin/kafka-metadata-quorum.sh --command-config config/controller.properties \
  --bootstrap-server localhost:9092 add-controller
```

또는 컨트롤러 엔드포인트 사용:

```bash
bin/kafka-metadata-quorum.sh --command-config config/controller.properties \
  --bootstrap-controller localhost:9093 add-controller
```

#### 컨트롤러 제거

먼저 대상 컨트롤러를 종료(권장)한 다음:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 \
  remove-controller --controller-id <id> \
  --controller-directory-id <directory-id>
```

### 8.8 디버깅 도구

#### 메타데이터 쿼럼 도구

쿼럼 상태 표시:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 \
  describe --status
```

출력에는 클러스터 ID, 리더, 에포크, 워터마크, 투표자 목록, 관찰자 목록이 포함됩니다.

#### 덤프 로그 도구

메타데이터 로그 세그먼트 디코딩:

```bash
bin/kafka-dump-log.sh --cluster-metadata-decoder \
  --files metadata_log_dir/__cluster_metadata-0/00000000000000000000.log
```

스냅샷 디코딩:

```bash
bin/kafka-dump-log.sh --cluster-metadata-decoder \
  --files metadata_log_dir/__cluster_metadata-0/00000000000000000100-\
0000000001.checkpoint
```

#### 메타데이터 셸

메타데이터 상태를 대화식으로 검사:

```bash
bin/kafka-metadata-shell.sh --snapshot \
  metadata_log_dir/__cluster_metadata-0/\
00000000000000007228-0000000001.checkpoint
```

명령에는 `ls`, `cat`, `exit`가 포함됩니다.

### 8.9 배포 모범 사례

1. 역할 분리: `process.roles`를 `broker` 또는 `controller` 중 하나로 설정, 둘 다 아님 (개발 제외)
2. 중복성: 3개 이상의 컨트롤러 사용; `N`개의 장애 허용을 위해 `2N + 1`개의 컨트롤러 배포
3. 리소스 할당: 각 컨트롤러의 메타데이터 로그 디렉토리에 5GB 메모리와 5GB 디스크 공간 할당
4. 하드웨어: 최적의 구성을 위해 하드웨어 및 OS 문서 참조

### 8.10 ZooKeeper에서 KRaft로 마이그레이션

마이그레이션에는 브리지 릴리스가 필요합니다. "마지막 브리지 릴리스는 Kafka 3.9입니다." 자세한 마이그레이션 단계는 Kafka 3.9 문서의 KRaft/ZooKeeper 마이그레이션 섹션에서 사용할 수 있습니다.

### 8.11 ZooKeeper 대비 주요 개선 사항

- ZooKeeper에 대한 외부 의존성 제거
- 설정 변경 없이 동적 컨트롤러 확장 가능
- Raft 합의를 통한 개선된 메타데이터 관리
- Kafka 클러스터를 위한 더 간단한 운영 모델

---

## 9. 계층형 스토리지

### 9.1 아키텍처 및 개요

Kafka 계층형 스토리지는 데이터 액세스 패턴을 최적화하는 2계층 스토리지 시스템을 구현합니다. 이 설계는 "Kafka 데이터가 대부분 OS 페이지 캐시를 활용하는 테일 읽기를 사용하여 스트리밍 방식으로 소비된다"는 것을 인식하며, 오래된 데이터는 드물게 디스크 액세스가 필요합니다. 시스템은 다음을 분리합니다:

- 로컬 계층: 로컬 디스크의 전통적인 브로커 기반 스토리지
- 원격 계층: 완료된 로그 세그먼트를 위한 외부 시스템(HDFS, S3)

### 9.2 브로커 설정 요구 사항

계층형 스토리지는 기본적으로 비활성화되어 있습니다. 주요 브로커 설정:

- `remote.log.storage.system.enable=true` - 기능 활성화
- `remote.log.storage.manager.class.name` - RemoteStorageManager 구현 지정
- `remote.log.metadata.manager.class.name` - 원격 세그먼트 메타데이터 관리 (Kafka는 기본 내부 토픽 기반 구현 제공)
- `remote.log.metadata.manager.listener.name` - 메타데이터 클라이언트에 필요한 리스너 지정

### 9.3 토픽 수준 설정

토픽은 `remote.storage.enable=true`를 통해 명시적 활성화가 필요합니다. 시스템은 보존 제어를 도입합니다:

- `local.retention.ms/bytes` - 세그먼트가 원격 스토리지로 이동하기 전 기간/크기
- `retention.ms/bytes` - 전체 보존 기간 (로컬 값이 설정되지 않은 경우 기본값으로 사용)

### 9.4 실용적 구현

문서에서는 테스트용으로 LocalTieredStorage를 사용하는 빠른 시작을 제공하며, 이는 로컬 디렉토리에서 원격 스토리지를 시뮬레이션합니다. 예시:

```properties
remote.storage.enable=true
local.retention.ms=1000
retention.ms=3600000
segment.bytes=1048576
```

세그먼트는 롤링 후 원격 스토리지에 자동으로 업로드되며, 그런 다음 로컬 복사본은 보존 정책에 따라 삭제됩니다.

### 9.5 기능 제한 사항

현재 제약 사항:
- 압축된 토픽 지원 없음
- 원격 스토리지에서 페치 요청당 단일 파티션
- 관리 작업에 클라이언트 버전 3.0+ 필요
- 프로듀서 스냅샷이 없는 v2.8.0 이전에 생성된 토픽과 호환되지 않음

---

## 10. 컨슈머 리밸런스 프로토콜

### 10.1 개요

Apache Kafka 4.0은 현재 일반적으로 사용 가능한 차세대 컨슈머 리밸런스 프로토콜(KIP-848)을 도입했습니다. 이 프로토콜은 "완전히 증분적인 설계 덕분에 리밸런스 시간을 줄이면서" 컨슈머 그룹 확장성을 향상시킵니다.

두 가지 그룹 유형이 존재합니다:
- 컨슈머 그룹: 새 프로토콜 사용
- 클래식 그룹: 레거시 프로토콜 사용

### 10.2 서버 설정

새 프로토콜은 `group.version` 기능 플래그를 통해 Kafka 4.0+에서 자동으로 활성화됩니다. 주요 서버 측 설정:

- `group.consumer.heartbeat.interval.ms`
- `group.consumer.session.timeout.ms`
- `group.consumer.assignors` (사용 가능한 할당 전략 지정)

이제 서버가 하트비트 간격, 세션 타임아웃, 할당 전략을 제어합니다. 기본 할당자에는 균일(uniform) 및 범위(range) 전략이 포함됩니다.

### 10.3 컨슈머 설정

클라이언트는 `group.protocol=consumer`를 사용하여 새 프로토콜을 명시적으로 활성화해야 합니다. 선택적 `group.remote.assignor` 설정은 서버 측 기본값을 재정의할 수 있습니다.

새로운 구독 방법은 정규식 패턴을 지원합니다: `subscribe(SubscriptionPattern)` 및 `subscribe(SubscriptionPattern, ConsumerRebalanceListener)`는 RE2J 형식을 사용하여 서버 측에서 패턴을 평가합니다.

### 10.4 업그레이드 및 다운그레이드

오프라인 접근 방식: 모든 컨슈머를 종료하고 새 설정으로 재시작합니다. 그룹이 비어 있을 때 자동 변환됩니다.

온라인 접근 방식: 롤링 배포로 무중단 변환이 가능합니다. 모든 컨슈머가 업데이트되거나 다운그레이드될 때까지 그룹이 프로토콜 간에 상호 운용됩니다.

### 10.5 제한 사항

- 클라이언트 측 할당자 미지원 (KAFKA-18327)
- 랙 인식 전략 불완전 지원 (KAFKA-17747)

---

## 11. 트랜잭션 프로토콜

### 11.1 개요

Apache Kafka 4.0은 트랜잭션 프로토콜을 강화하는 트랜잭션 서버 측 방어(KIP-890) 를 도입했습니다. 핵심 혁신: "프로듀서 에포크가 모든 트랜잭션에서 증가하여 모든 트랜잭션이 의도한 메시지를 포함하고 중복이 다음 트랜잭션의 일부로 작성되지 않도록 보장합니다."

이 프로토콜은 `transaction.version` 기능 플래그를 통해 기본적으로 활성화되며, 클러스터 생성 시 또는 기능 도구를 통해 동적으로 구성할 수 있습니다.

### 11.2 업그레이드 및 다운그레이드

새 프로토콜 활성화: 서버에서 `transaction.version=2`를 설정합니다. 프로듀서 클라이언트(4.0+)는 재시작 없이 다음 연결 시 자동으로 향상된 프로토콜을 채택합니다. 클라이언트는 트랜잭션 중에 업그레이드되지 않지만 후속 트랜잭션 시작 시 전환됩니다.

다운그레이드: 이 프로세스는 안전하며 업그레이드를 미러링합니다. 클라이언트는 서버 측 다운그레이드를 인식한 후 첫 번째 트랜잭션에서 이전 프로토콜로 되돌아갑니다.

### 11.3 성능 영향

재설계된 프로토콜은 파티션 추가를 단일 서버 측 작업으로 통합하여 오버헤드를 줄이고 이전 클라이언트-검증 주기를 제거합니다. 그러나 이는 트레이드오프를 도입합니다:

이전에 클라이언트는 `CONCURRENT_TRANSACTIONS` 오류에 대해 하드코딩된 20ms 재시도 백오프를 사용했습니다. 이제 서버가 내부적으로 재시도를 처리하여 보고된 생산 지연 시간이 잠재적으로 증가할 수 있습니다. 종단 간 트랜잭션 지연 시간(커밋 시간)은 변경되지 않습니다 - 지연 시간이 클라이언트 측 백오프에서 서버 측 처리로 단순히 이동합니다.

구성 가능한 재시도 매개변수:
- `add.partitions.to.txn.retry.backoff.ms`
- `add.partitions.to.txn.retry.backoff.max.ms`

---

## 12. 적격 리더 레플리카 (ELR)

### 12.1 개요

ELR(Eligible Leader Replicas)은 Apache Kafka 4.0에 도입되었으며(4.1에서 기본적으로 활성화됨) 리더가 되기에 안전한 레플리카를 식별하여 복제를 개선합니다. 이 기능은 레플리카가 `min.insync.replicas` 아래로 떨어질 때 하이 워터마크의 진행을 방지하는 Kafka의 "엄격한 최소 ISR" 규칙을 활용합니다.

리더 선택 우선순위:
KRaft 컨트롤러는 다음 순서로 리더를 선택합니다:
1. 사용 가능한 경우 In-Sync Replica(ISR) 집합에서
2. ISR이 비어 있으면 적격 리더 레플리카(ELR)에서
3. 언펜스된 경우 마지막으로 알려진 리더 (4.0 이전 동작)

### 12.2 설정

ELR 활성화: 서버에서 `eligible.leader.replicas.version=1`을 설정합니다.

ELR 비활성화: `eligible.leader.replicas.version=0`을 설정합니다 (다운그레이드에 안전).

### 12.3 중요한 제약 사항

ELR이 활성화되면:

- 클러스터 수준 `min.insync.replicas`가 없으면 자동으로 추가됨
- 클러스터 수준 `min.insync.replicas` 제거가 금지됨
- 브로커 수준 `min.insync.replicas` 설정이 제거됨
- 브로커 수준 `min.insync.replicas` 변경이 허용되지 않음
- `min.insync.replicas` 변경 시 ELR 상태가 리셋됨

### 12.4 도구 및 접근

토픽을 설명하기 위해 관리 클라이언트를 사용하는 `DescribeTopicPartitions` API를 통해 ELR 정보를 쿼리합니다.

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Operations](https://kafka.apache.org/documentation/#operations)
- [KRaft Documentation](https://kafka.apache.org/documentation/#kraft)
- [Kafka MirrorMaker 2](https://kafka.apache.org/documentation/#georeplication)
