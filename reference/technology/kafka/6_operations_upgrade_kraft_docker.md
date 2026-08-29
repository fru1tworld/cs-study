# Kafka 운영, 업그레이드, KRaft, Docker

## Kafka 운영

> 원본: https://kafka.apache.org/documentation/#operations

### 목차

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

### 1. 기본 Kafka 운영

#### 1.1 토픽 관리

##### 토픽 생성

`kafka-topics.sh`를 사용하여 파티션과 복제 계수를 지정하여 토픽을 생성함:

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic my_topic_name \
    --partitions 20 --replication-factor 3 --config x=y
```

주요 고려사항:
- 복제 계수(Replication factor): 데이터 중복성을 제어함. 복제 계수 3은 데이터 손실 없이 브로커 2개의 동시 장애를 허용함.
- 파티션 수: 데이터 샤딩과 컨슈머 병렬성을 결정함. 폴더 이름의 최대 길이(255자) 제약으로 인해 토픽 이름은 최대 249자로 제한됨.

##### 토픽 수정

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

> 주의: Kafka는 파티션 수 감소를 지원하지 않음.

---

#### 1.2 브로커 운영

##### 정상 종료 (Graceful Shutdown)

브로커 재시작을 최적화하려면 제어된 종료를 활성화함:

```properties
controlled.shutdown.enable=true
```

장점:
- 디스크에 로그 동기화 (복구 시간 방지)
- 다른 레플리카로 리더십 자동 마이그레이션

##### 리더십 밸런싱

선호 레플리카의 자동 복원 활성화:

```properties
auto.leader.rebalance.enable=true
```

수동 리더십 복원:

```bash
bin/kafka-leader-election.sh --bootstrap-server localhost:9092 \
    --election-type preferred --all-topic-partitions
```

##### 랙 인식 (Rack Awareness)

물리적 랙에 걸쳐 레플리카 분산:

```properties
broker.rack=my-rack-id
```

이를 통해 개별 브로커 장애뿐 아니라 전체 랙 장애에 대한 내결함성도 확보할 수 있음.

---

#### 1.3 컨슈머 그룹 관리

##### 컨슈머 위치 확인

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group
```

출력에는 현재 오프셋, 로그 끝 오프셋, 컨슈머 지연(lag)이 표시됨.

##### 컨슈머 그룹 목록

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

##### 그룹 상세 정보 확인

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

##### 컨슈머 그룹 삭제

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete \
    --group my-group --group my-other-group
```

##### 컨슈머 오프셋 리셋

```bash
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets \
    --group my-group --topic topic1 --to-latest
```

리셋 시나리오 옵션:

- `--to-datetime <String>`: 특정 날짜/시간으로 리셋 (형식: YYYY-MM-DDThh:mm:ss.sss)
- `--to-earliest`: 가장 이른 사용 가능한 오프셋으로 이동
- `--to-latest`: 가장 최근 오프셋으로 이동
- `--shift-by <Long>`: 현재 위치를 N 오프셋만큼 조정 (양수/음수)
- `--from-file`: 사용자 정의 오프셋 값이 있는 CSV 파일 사용
- `--to-current`: 현재 위치로 리셋
- `--by-duration <String>`: 기간별 오프셋 조정 (형식: PnDTnHnMnS)
- `--to-offset`: 특정 오프셋 값 설정

실행 모드:
- 기본값: 제안된 변경 사항 표시
- `--execute`: 리셋 적용
- `--export`: 결과를 CSV로 출력

---

#### 1.4 공유 그룹 관리 (Share Groups)

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

#### 1.5 클러스터 확장 및 파티션 재할당

##### 데이터 자동 마이그레이션

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

##### 사용자 정의 파티션 할당

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

위와 같이 `--execute`와 `--verify`로 실행함.

---

#### 1.6 복제 관리

##### 복제 계수 증가

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

#### 1.7 대역폭 스로틀링

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

##### 안전한 스로틀링 실천 방법

1. 완료 후 즉시 스로틀 제거 (`--verify` 통해)
2. ConsumerLag 메트릭으로 복제 진행 상황 모니터링
3. 진행 유지를 위해 스로틀이 들어오는 쓰기 속도를 초과하도록 보장

---

#### 1.8 쿼터 설정

##### 사용자 및 클라이언트 쿼터 설정

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

##### 기본 쿼터 설정

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

##### 쿼터 조회

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

#### 1.9 브로커 해제 (Decommissioning)

재할당 도구는 브로커 제거 시 수동 계획이 필요함. 관리자는 대상 브로커의 모든 파티션을 나머지 브로커로 옮기는 재할당 계획을 직접 작성해야 하며, 레플리카가 특정 브로커 한 곳에 집중되지 않도록 주의해야 함.

---

### 2. 데이터센터

#### 2.1 권장 배포 패턴

권장 패턴은 "각 데이터센터에 로컬 Kafka 클러스터를 배포하고, 각 데이터센터의 애플리케이션 인스턴스는 로컬 클러스터와만 통신하며, 클러스터 간 데이터는 미러링으로 동기화"하는 것임.

이 접근 방식의 장점:
- 각 데이터센터가 독립적인 개체로 운영됨
- 데이터센터 간 복제를 중앙에서 관리하고 튜닝할 수 있음
- 데이터센터 간 링크가 실패해도 개별 시설이 작동할 수 있음
- 연결이 복원되면 미러링이 자동으로 재개됨

#### 2.2 글로벌 데이터 액세스 전략

모든 위치에서 전체 데이터를 조회해야 하는 애플리케이션에는 "각 데이터센터의 로컬 클러스터로부터 데이터를 집계하여 미러링하는 클러스터"를 별도로 두는 방식이 권장됨. 이 집계 클러스터는 전체 데이터셋이 필요한 애플리케이션에 읽기 전용으로 제공됨.

#### 2.3 높은 지연 시간 WAN 연결

원격 클러스터를 대상으로 읽기/쓰기를 수행할 수 있지만 추가 지연이 발생함. Kafka의 프로듀서·컨슈머 배치 메커니즘은 "높은 지연 시간 연결에서도 높은 처리량"을 달성할 수 있게 해줌.

WAN 링크에서 성능을 최적화하려면 `socket.send.buffer.bytes`와 `socket.receive.buffer.bytes` 설정을 사용하여 프로듀서, 컨슈머, 브로커의 TCP 소켓 버퍼 크기를 늘려야 함.

#### 2.4 안티패턴 경고

"높은 지연 시간 WAN 링크로 연결된 여러 데이터센터에 걸친 단일 Kafka 클러스터" 구성은 복제 지연 및 가용성 문제로 인해 명시적으로 권장하지 않음.

---

### 3. 지리적 복제 (클러스터 간 데이터 미러링)

#### 3.1 개요

Kafka의 지리적 복제 기능을 통해 관리자는 "개별 Kafka 클러스터, 데이터센터, 지리적 지역의 경계를 넘는 데이터 흐름"을 정의할 수 있음.

이는 Kafka Connect 프레임워크 위에 구축된 MirrorMaker 2를 통해 수행되며, "서로 다른 Kafka 환경 간에 데이터를 스트리밍 방식으로 복제하는 도구"임.

#### 3.2 주요 기능

MirrorMaker 지원 기능:
- 데이터 및 설정과 함께 토픽 복제
- 클러스터 간 컨슈머 그룹 및 오프셋 마이그레이션
- ACL 복제
- 파티션 보존
- 새로운 토픽 및 파티션 자동 감지
- 종단 간 복제 지연 시간 메트릭
- 내결함성, 수평 확장 가능한 운영

#### 3.3 복제 흐름

방향성 데이터 이동을 "복제 흐름(replication flows)"이라고 하며, `{source_cluster}->{target_cluster}` 형식을 사용함. 일반적인 패턴:

- Active/Active: `A->B, B->A`
- Active/Passive: `A->B`
- 집계(Aggregation): `A->K, B->K, C->K`
- 팬아웃(Fan-out): `K->A, K->B, K->C`

#### 3.4 설정 구문

##### 기본 설정 구조

```properties
clusters = primary, secondary
primary.bootstrap.servers = broker3-primary:9092
secondary.bootstrap.servers = broker5-secondary:9092

primary->secondary.enabled = true
primary->secondary.topics = foobar-topic, quux-.*
```

##### 주요 설정 매개변수

- `clusters`: 쉼표로 구분된 클러스터 별칭
- `{cluster}.bootstrap.servers`: 특정 클러스터의 연결 정보
- `{source}->{target}.enabled`: 복제 흐름 활성화 (true/false)
- `topics`: 복제할 토픽 (정규식 지원; 기본값: `.*`)
- `topics.exclude`: 복제에서 제외할 토픽
- `groups`: 복제할 컨슈머 그룹
- `groups.exclude`: 복제에서 제외할 컨슈머 그룹

##### 사용자 정의 클라이언트 설정

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

#### 3.5 보안 설정

MirrorMaker는 SSL 암호화를 지원함. 예시:

```properties
us-east.security.protocol=SSL
us-east.ssl.truststore.location=/path/to/truststore.jks
us-east.ssl.truststore.password=my-secret-password
us-east.ssl.keystore.location=/path/to/keystore.jks
us-east.ssl.keystore.password=my-secret-password
us-east.ssl.key.password=my-secret-password
```

#### 3.6 정확히 한 번 시맨틱스 (Exactly-Once, v3.5.0+)

대상 클러스터에 활성화:

```properties
us-east.exactly.once.source.support = enabled
```

기존 클러스터의 경우 2단계 업그레이드 사용: 먼저 `preparing`으로 설정한 다음 `enabled`로 설정함.

클러스터 내 통신 활성화:

```properties
dedicated.mode.enable.internal.rest = true
listeners = http://localhost:8080
```

중단된 트랜잭션 필터링:

```properties
us-west.consumer.isolation.level = read_committed
```

#### 3.7 대상 클러스터의 토픽 명명

기본적으로 복제된 토픽은 `{source}.{source_topic_name}` 형식을 따름. 예시:

```
us-west 클러스터          us-east 클러스터
─────────────            ───────────────
foo-topic           -->  us-west.foo-topic
```

구분자 사용자 정의:

```properties
us-west->us-east.replication.policy.separator = _
```

#### 3.8 MirrorMaker 시작

기본 시작:

```bash
bin/connect-mirror-maker.sh connect-mirror-maker.properties
```

클러스터 지정 (지연 시간 감소를 위해 권장):

```bash
bin/connect-mirror-maker.sh connect-mirror-maker.properties \
    --clusters us-west
```

> 참고: MirrorMaker 프로세스가 처음 데이터 복제를 시작하기까지 몇 분이 걸릴 수 있음.

#### 3.9 MirrorMaker 중지

```bash
kill <MirrorMaker pid>
```

#### 3.10 설정 예시

##### Active/Passive 배포

```properties
primary.bootstrap.servers = broker1-primary:9092
secondary.bootstrap.servers = broker2-secondary:9092

primary->secondary.enabled = true
secondary->primary.enabled = false

primary->secondary.topics = foo.*
```

##### Active/Active 배포

```properties
clusters = us-west, us-east
us-west.bootstrap.servers = broker1-west:9092,broker2-west:9092
us-east.bootstrap.servers = broker3-east:9092,broker4-east:9092

us-west->us-east.enabled = true
us-east->us-west.enabled = true
```

##### 데이터센터 분리가 있는 멀티 클러스터

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

#### 3.11 중요한 모범 사례

원격에서 소비, 로컬에서 생산: Kafka 프로듀서는 일반적으로 Kafka 컨슈머보다 불안정하거나 지연이 높은 네트워크 연결에서 더 큰 어려움을 겪으므로, 지연 시간을 최소화하기 위해 MirrorMaker 프로세스를 대상 클러스터 가까이에 배치함.

설정 일관성: 동일한 클러스터를 대상으로 하는 복제 흐름 간에 MirrorMaker 설정을 일관되게 유지하여 충돌을 방지함.

컨슈머 그룹 테스트: 기본적으로 콘솔 컨슈머 그룹은 제외됨. 그룹 복제 테스트 시 `groups.exclude` 수정 필요.

#### 3.12 모니터링

MirrorMaker는 `kafka.connect.mirror` 그룹 아래에 다음 태그가 붙은 메트릭을 발행함:
- `source`: 소스 클러스터 별칭
- `target`: 대상 클러스터 별칭
- `topic`: 대상의 복제된 토픽
- `partition`: 복제 중인 파티션

##### 주요 메트릭

소스 커넥터 메트릭:
- `record-count`: 복제된 레코드 수
- `record-rate`: 초당 레코드
- `record-age-ms`: 복제 시 레코드 수명
- `replication-latency-ms`: 전파 시간
- `byte-rate` / `byte-count`: 처리량 메트릭

체크포인트 커넥터 메트릭:
- `checkpoint-latency-ms`: 컨슈머 오프셋 복제 시간

#### 3.13 설정 변경 프로세스

설정 변경을 적용하려면 MirrorMaker 프로세스를 재시작해야 함.

---

### 4. 멀티 테넌시

#### 4.1 개요

"고도로 확장 가능한 이벤트 스트리밍 플랫폼인 Kafka는 다수의 조직에서 중추 시스템으로 활용되며, 다양한 팀과 사업 부문의 수많은 시스템·애플리케이션을 실시간으로 연결함."

멀티 테넌시는 6가지 핵심 영역을 포함함:
1. 사용자 공간 생성
2. 토픽 설정
3. 클러스터/토픽 보안
4. 쿼터를 통한 테넌트 격리
5. 모니터링
6. 클러스터 간 데이터 공유

#### 4.2 토픽 명명을 통한 사용자 공간 생성

Kafka 관리자는 보안 제어와 결합된 계층적 토픽 명명 체계를 통해 격리된 테넌트 환경을 구축함. 일반적으로 사용되는 두 가지 명명 방식은 다음과 같음:

팀/조직 단위별:
- 패턴: `<organization>.<team>.<dataset>.<event-name>`
- 예시: `acme.infosec.telemetry.logins`

프로젝트/제품별:
- 패턴: `<project>.<product>.<event-name>`
- 예시: `mobility.payments.suspicious`

##### 시행 방법

- 접두사 ACL (KIP-290): 특정 접두사로 토픽 생성 제한
- 사용자 정의 CreateTopicPolicy (KIP-108): 설정을 통해 엄격한 명명 패턴 시행
- 사용자 토픽 생성 비활성화: 관리자 승인 필요
- 자동 생성 비활성화: 브로커에서 `auto.create.topics.enable=false` 설정

#### 4.3 토픽 설정: 보존 정책

"관리자는 종종 `retention.bytes`(크기) 및 `retention.ms`(시간)와 같은 설정으로 토픽에 데이터가 저장되는 양과 기간을 제어하는 데이터 보존 정책을 정의해야 함."

이 접근 방식은 스토리지 소비를 제한하고 법적 규정 준수(GDPR)를 지원함.

#### 4.4 보안: 인증, 권한 부여, 암호화

세 가지 핵심 보안 범주가 적용됨:

1. 암호화
브로커-클라이언트 간, 브로커 간, 선택적 도구 간 데이터 보호

2. 인증
클라이언트/애플리케이션의 브로커 연결 및 브로커 간 통신

3. 권한 부여
클라이언트 작업 제어: 토픽 관리, 이벤트 읽기/쓰기 액세스, ACL 수정

##### ACL 관리 예시

InfoSec 팀의 Alice 사용자:

```bash
bin/kafka-acls.sh \
    --bootstrap-server localhost:9092 \
    --add --allow-principal User:Alice \
    --producer \
    --resource-pattern-type prefixed --topic acme.infosec.
```

"특히 멀티 테넌트 환경에서 계층적 토픽 명명 구조를 갖추면 접두사 ACL을 통해 토픽 접근을 편리하게 제어할 수 있다는 이점이 있음."

#### 4.5 테넌트 격리: 쿼터 및 속도 제한

클라이언트 쿼터:
- 요청 속도 쿼터: 사용자당 브로커 CPU 사용량을 제한
- 네트워크 대역폭 쿼터: 인바운드/아웃바운드 트래픽을 제어
- 토픽 작업 쿼터(`controller_mutation_rate`): 동시 작업으로 인한 클러스터 과부하를 방지

"요청 속도 쿼터는 해당 사용자에 대해 브로커가 요청 처리 경로에서 소비하는 시간을 제한하여 사용자가 브로커 CPU 사용량에 미치는 영향을 제한하는 데 도움이 되며, 이후 스로틀링이 시작됨."

서버 쿼터:
- 연결 속도 제한
- 브로커당 최대 연결 수
- IP 기반 연결 제한

#### 4.6 모니터링 및 미터링

"Kafka는 실패한 인증 시도 속도, 요청 지연 시간, 컨슈머 지연, 총 컨슈머 그룹 수, 이전 섹션에서 설명한 쿼터에 대한 메트릭 등 광범위한 메트릭을 지원함."

주요 메트릭 예시: JMX 메트릭 `kafka.log.Log.Size.<TOPIC-NAME>`을 사용하여 토픽-파티션 크기를 추적하고 스토리지 초과에 대해 알림을 설정함.

#### 4.7 멀티 테넌시와 지리적 복제

Kafka는 재해 복구 및 테넌트 간 시나리오를 위한 클러스터 간 데이터 공유를 가능하게 함. 구현 세부 사항은 지리적 복제 문서 참조.

#### 4.8 추가 고려사항: 데이터 계약

"클러스터의 데이터 생산자와 소비자 간에 이벤트 스키마를 사용하여 데이터 계약을 정의해야 할 수 있음."

이벤트 형식 검증, 잘못된 데이터 차단, 스키마 진화 관리를 위해 스키마 레지스트리를 도입하는 것이 좋음. 단, Kafka 자체에는 이 구성 요소가 포함되어 있지 않음.

---

### 5. Java 버전

#### 5.1 지원되는 Java 버전

Apache Kafka 문서에 따르면:
- Java 17 및 Java 21 이 완전히 지원됨
- Java 11 은 일부 모듈(클라이언트, 스트림 및 관련 모듈)에 대해 지원됨
- 가장 최근 LTS 이후의 새 버전은 최선의 노력(best-effort)으로 지원됨

#### 5.2 권장 사항

"성능, 효율성 및 지원 측면에서 가장 최근의 LTS 릴리스(작성 시점 기준 Java 21)"가 선호됨. 또한 보안을 위해 최신 패치 버전을 실행해야 함. 이전 버전은 종종 공개된 취약점을 포함하고 있음.

#### 5.3 권장 JVM 인수

OpenJDK 기반 구현의 경우, Kafka는 다음과 같은 일반적인 시작 매개변수를 제안함:

```
-Xmx6g -Xms6g -XX:MetaspaceSize=96m -XX:+UseG1GC
-XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35
-XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50
-XX:MaxMetaspaceFreeRatio=80 -XX:+ExplicitGCInvokesConcurrent
```

이러한 인수는 프로덕션 클러스터에서 초당 1회 미만의 Young GC와 함께 약 21ms GC 일시 중지 시간(90번째 백분위수)을 달성함.

---

### 6. 하드웨어 및 OS

#### 6.1 하드웨어 사양

Kafka는 24GB RAM을 장착한 듀얼 쿼드코어 Intel Xeon 머신에서 원활하게 작동함. 메모리 요구량은 활성 리더·라이터의 버퍼링 규모에 따라 달라지며, "쓰기 처리량 × 30초" 분량의 버퍼링을 기준으로 추정할 수 있음.

디스크 처리량이 주요 성능 병목임. "8x7200 rpm SATA 드라이브"가 기준 구성으로 언급되며, 일반적으로 드라이브 수가 많을수록 성능이 향상됨. 고RPM SAS 드라이브가 실질적인 이점을 제공하는지는 플러시 동작 설정에 따라 달라짐.

#### 6.2 운영 체제 지원

Kafka는 Linux 및 Solaris를 포함한 Unix 계열 시스템에서 안정적으로 동작함. Windows는 개선 가능성이 있으나 아직 완전한 지원이 부족함. 몇 가지 핵심 설정을 제외하면 OS 수준의 튜닝은 최소한으로 충분함.

#### 6.3 OS 수준 설정

파일 디스크립터: 브로커 프로세스의 시작 기준으로 최소 100,000으로 설정함. Kafka는 로그 세그먼트와 네트워크 연결에 파일 디스크립터를 사용함. 파티션이 많은 브로커는 "(파티션 수) × (파티션 크기 / 세그먼트 크기)" + 연결 수만큼의 디스크립터가 필요함.

소켓 버퍼 크기: 데이터센터 간 고성능 데이터 전송을 가능하게 하려면 최대 소켓 버퍼 크기를 늘림.

메모리 매핑 (vm.max_map_count): Linux 기본값은 약 65,535로, 파티션 수가 많은 환경에는 부족함. 각 로그 세그먼트는 맵 영역 2개를 필요로 하므로, 파티션 50,000개는 맵 영역 100,000개 이상을 요구하며 이를 초과하면 OutOfMemoryError가 발생함.

#### 6.4 디스크 및 파일시스템 전략

Kafka 데이터 전용 드라이브를 여러 개 사용하고, 애플리케이션 로그나 OS 활동과는 분리해야 함. 드라이브를 RAID로 묶어 단일 볼륨으로 구성하거나, 각 드라이브를 독립적인 디렉토리로 포맷할 수 있음. 파티션은 디렉토리 간에 라운드 로빈 방식으로 분산됨.

RAID는 부하 분산 효과가 있지만 쓰기 처리량과 사용 가능한 공간을 줄임. RAID 재구축은 I/O를 집중적으로 사용하므로 사실상 서버를 일시적으로 비활성화하여 실제 가용성 이점을 제한함.

#### 6.5 플러시 관리

Kafka는 "모든 데이터를 파일시스템에 즉시 기록하지만" 강제 디스크 플러시는 지연시킴. "애플리케이션 수준의 fsync를 완전히 비활성화하는 기본 플러시 설정"을 그대로 사용하고 OS 백그라운드 플러싱에 맡기는 방식이 권장됨. 이 방식은 "별도의 튜닝 없이도 뛰어난 처리량·지연 시간과 완전한 복구 보장"을 제공함.

애플리케이션 수준의 fsync 정책은 비효율성과 지연 증가를 유발함. 대부분의 Linux 파일시스템은 fsync 중 쓰기를 차단하지만, 백그라운드 플러싱은 세분화된 페이지 단위 잠금을 사용하므로 이런 문제가 없음.

#### 6.6 파일시스템 선택

EXT4와 XFS가 가장 많이 사용되는 파일시스템임. XFS는 최적화된 EXT4 구성 대비 "160ms 대 250ms+"로 우수한 성능을 보이며 디스크 성능 변동도 적음.

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

### 7. 모니터링

#### 7.1 개요

Kafka는 서버 측에 Yammer Metrics, 클라이언트 측에 Kafka Metrics를 사용하며, 두 방식 모두 JMX를 통해 메트릭을 노출하고 외부 모니터링 시스템용 플러그인 리포터를 지원함.

핵심 원칙: "모든 Kafka 속도 메트릭에는 접미사 `-total`이 있는 해당 누적 카운트 메트릭이 있음."

#### 7.2 중요 모니터링 메트릭

##### 브로커 수준 메트릭

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

##### 컨슈머/프로듀서 메트릭

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

#### 7.3 모니터링 모범 사례

1. 메트릭 접근: "사용 가능한 메트릭을 확인하는 가장 쉬운 방법은 jconsole을 실행하고 실행 중인 Kafka 클라이언트 또는 서버를 가리키는 것임."

2. 보안 우선: 원격 JMX는 기본적으로 비활성화되어 있음. 프로덕션의 경우: `JMX_PORT` 환경 변수를 통해 활성화하지만 "프로덕션 시나리오에서 원격 JMX를 활성화할 때 반드시 보안을 활성화해야 함."

3. 주요 상태 지표:
   - 디스크 사용률 및 로그 디렉토리 오프라인 상태
   - GC 시간 및 CPU 사용률
   - I/O 서비스 시간
   - 토픽별 처리량 추세

4. 컨슈머 생존력: "컨슈머가 따라잡으려면 최대 지연이 임계값보다 작아야 하고 최소 페치 속도가 0보다 커야 함."

5. 쿼터 모니터링: 사용자/클라이언트별 대역폭 및 요청 쿼터에 대한 스로틀 시간 추적

#### 7.4 전문 모니터링 영역

- 그룹 조정: 파티션 로딩 시간, 이벤트 큐 메트릭, 리밸런스 속도
- 계층형 스토리지: 원격 페치/복사 속도 및 오류, 지연 바이트/세그먼트
- KRaft 클러스터: 쿼럼 상태, 메타데이터 버전, 컨트롤러 메트릭
- Streams 애플리케이션: 스레드 상태, 태스크 메트릭, 프로세서 노드 지연 시간, 상태 저장소 작업

JMX 메트릭 이름은 `kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec`와 같은 패턴을 따라 표준 모니터링 플랫폼과의 통합을 가능하게 함.

---

### 8. KRaft

#### 8.1 개요

KRaft(Kafka Raft)는 ZooKeeper를 대체하는 Kafka 클러스터 메타데이터 관리용 합의 프로토콜로, 최신 Kafka 배포에 권장되는 아키텍처임.

#### 8.2 프로세스 역할 설정

KRaft 모드의 Kafka 서버는 세 가지 구별되는 역할로 구성할 수 있음:

- 브로커 전용: `process.roles=broker` - 메시지 생산 및 소비 처리
- 컨트롤러 전용: `process.roles=controller` - 클러스터 메타데이터 관리
- 결합: `process.roles=broker,controller` - 두 기능 모두 수행

> "결합 서버는 개발 환경과 같은 소규모 사용 사례에 대해 운영하기가 더 간단"하지만 컨트롤러 격리 감소로 인해 "중요한 배포 환경에서는 결합 모드가 권장되지 않음."

#### 8.3 컨트롤러 아키텍처

##### 쿼럼 설정

컨트롤러는 메타데이터 쿼럼에 참여함. 권장 구성은 3개 또는 5개의 컨트롤러를 사용함:
- 3개의 컨트롤러: 1개의 동시 장애 허용
- 5개의 컨트롤러: 2개의 동시 장애 허용

모든 서버는 `controller.quorum.bootstrap.servers` 속성을 통해 활성 컨트롤러를 발견해야 함:

```properties
controller.quorum.bootstrap.servers=host1:port1,host2:port2,host3:port3
```

##### 컨트롤러 설정 예시

```properties
process.roles=controller
node.id=1
listeners=CONTROLLER://controller1.example.com:9093
controller.quorum.bootstrap.servers=controller1.example.com:9093,\
  controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

#### 8.4 정적 vs 동적 쿼럼

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

#### 8.5 노드 프로비저닝

##### 클러스터 ID 생성

```bash
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

##### 단일 컨트롤러 부트스트랩

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> \
  --standalone --config config/controller.properties
```

이 명령은 meta.properties 파일과 해당 노드를 유일한 쿼럼 투표자로 설정하는 제어 레코드가 포함된 초기 스냅샷을 생성함.

##### 다중 컨트롤러 부트스트랩

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

`--initial-controllers` 값은 동일한 클러스터 ID를 공유하는 모든 컨트롤러에서 동일하게 지정해야 함.

##### 브로커 및 새 컨트롤러 포맷

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> \
  --config config/server.properties --no-initial-controllers
```

#### 8.6 동적 쿼럼으로 업그레이드

##### 1단계: 현재 버전 확인

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 describe
```

`kraft.version`이 `FinalizedVersionLevel: 0`을 표시하면 업그레이드가 필요함.

##### 2단계: 기능 버전 업그레이드

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

##### 3단계: 설정 파일 업데이트

`controller.quorum.voters`를 제거하고 `controller.quorum.bootstrap.servers`를 추가:

```properties
process.roles=...
node.id=...
controller.quorum.bootstrap.servers=controller1.example.com:9093,\
  controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

모든 클러스터 노드에 적용함.

#### 8.7 컨트롤러 멤버십 관리

##### 새 컨트롤러 추가

새 노드를 프로비저닝하고 시작한 다음 복제를 기다림:

```bash
bin/kafka-metadata-quorum.sh describe --replication
```

따라잡으면 추가함:

```bash
bin/kafka-metadata-quorum.sh --command-config config/controller.properties \
  --bootstrap-server localhost:9092 add-controller
```

또는 컨트롤러 엔드포인트 사용:

```bash
bin/kafka-metadata-quorum.sh --command-config config/controller.properties \
  --bootstrap-controller localhost:9093 add-controller
```

##### 컨트롤러 제거

먼저 대상 컨트롤러를 종료(권장)한 다음:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 \
  remove-controller --controller-id <id> \
  --controller-directory-id <directory-id>
```

#### 8.8 디버깅 도구

##### 메타데이터 쿼럼 도구

쿼럼 상태 표시:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 \
  describe --status
```

출력에는 클러스터 ID, 리더, 에포크, 워터마크, 투표자 목록, 관찰자 목록이 포함됨.

##### 덤프 로그 도구

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

##### 메타데이터 셸

메타데이터 상태를 대화식으로 검사:

```bash
bin/kafka-metadata-shell.sh --snapshot \
  metadata_log_dir/__cluster_metadata-0/\
00000000000000007228-0000000001.checkpoint
```

명령에는 `ls`, `cat`, `exit`가 포함됨.

#### 8.9 배포 모범 사례

1. 역할 분리: `process.roles`를 `broker` 또는 `controller` 중 하나로 설정, 둘 다 아님 (개발 제외)
2. 중복성: 3개 이상의 컨트롤러 사용; `N`개의 장애 허용을 위해 `2N + 1`개의 컨트롤러 배포
3. 리소스 할당: 각 컨트롤러의 메타데이터 로그 디렉토리에 5GB 메모리와 5GB 디스크 공간 할당
4. 하드웨어: 최적의 구성을 위해 하드웨어 및 OS 문서 참조

#### 8.10 ZooKeeper에서 KRaft로 마이그레이션

마이그레이션에는 브리지 릴리스가 필요함. "마지막 브리지 릴리스는 Kafka 3.9임." 세부 마이그레이션 절차는 Kafka 3.9 문서의 KRaft/ZooKeeper 마이그레이션 섹션 참고.

#### 8.11 ZooKeeper 대비 주요 개선 사항

- ZooKeeper에 대한 외부 의존성 제거
- 설정 변경 없이 동적 컨트롤러 확장 가능
- Raft 합의를 통한 개선된 메타데이터 관리
- Kafka 클러스터를 위한 더 간단한 운영 모델

---

### 9. 계층형 스토리지

#### 9.1 아키텍처 및 개요

Kafka 계층형 스토리지는 데이터 액세스 패턴을 최적화하는 2계층 스토리지 시스템을 구현함. 이 설계는 "Kafka 데이터가 대부분 OS 페이지 캐시를 활용하는 테일 읽기 방식으로 스트리밍 소비된다"는 점을 반영하며, 오래된 데이터는 디스크 액세스가 드물게 발생함. 시스템은 다음 두 계층을 분리함:

- 로컬 계층: 로컬 디스크의 전통적인 브로커 기반 스토리지
- 원격 계층: 완료된 로그 세그먼트를 위한 외부 시스템(HDFS, S3)

#### 9.2 브로커 설정 요구 사항

계층형 스토리지는 기본적으로 비활성화되어 있음. 주요 브로커 설정:

- `remote.log.storage.system.enable=true` - 기능 활성화
- `remote.log.storage.manager.class.name` - RemoteStorageManager 구현 지정
- `remote.log.metadata.manager.class.name` - 원격 세그먼트 메타데이터 관리 (Kafka는 기본 내부 토픽 기반 구현 제공)
- `remote.log.metadata.manager.listener.name` - 메타데이터 클라이언트에 필요한 리스너 지정

#### 9.3 토픽 수준 설정

토픽은 `remote.storage.enable=true`를 통해 명시적 활성화가 필요함. 시스템은 보존 제어를 도입함:

- `local.retention.ms/bytes` - 세그먼트가 원격 스토리지로 이동하기 전 기간/크기
- `retention.ms/bytes` - 전체 보존 기간 (로컬 값이 설정되지 않은 경우 기본값으로 사용)

#### 9.4 실용적 구현

문서에서는 테스트용으로 LocalTieredStorage를 사용하는 빠른 시작을 제공하며, 이는 로컬 디렉토리에서 원격 스토리지를 시뮬레이션함. 예시:

```properties
remote.storage.enable=true
local.retention.ms=1000
retention.ms=3600000
segment.bytes=1048576
```

세그먼트는 롤링 후 원격 스토리지에 자동으로 업로드되며, 이후 로컬 복사본은 보존 정책에 따라 삭제됨.

#### 9.5 기능 제한 사항

현재 제약 사항:
- 압축된 토픽 지원 없음
- 원격 스토리지에서 페치 요청당 단일 파티션
- 관리 작업에 클라이언트 버전 3.0+ 필요
- 프로듀서 스냅샷이 없는 v2.8.0 이전에 생성된 토픽과 호환되지 않음

---

### 10. 컨슈머 리밸런스 프로토콜

#### 10.1 개요

Apache Kafka 4.0은 차세대 컨슈머 리밸런스 프로토콜(KIP-848)을 정식(GA)으로 도입했음. 이 프로토콜은 "완전히 증분적인 설계 덕분에 리밸런스 시간을 줄이면서" 컨슈머 그룹 확장성을 향상시킴.

두 가지 그룹 유형이 존재함:
- 컨슈머 그룹: 새 프로토콜 사용
- 클래식 그룹: 레거시 프로토콜 사용

#### 10.2 서버 설정

새 프로토콜은 `group.version` 기능 플래그를 통해 Kafka 4.0+에서 자동으로 활성화됨. 주요 서버 측 설정:

- `group.consumer.heartbeat.interval.ms`
- `group.consumer.session.timeout.ms`
- `group.consumer.assignors` (사용 가능한 할당 전략 지정)

이제 서버가 하트비트 간격, 세션 타임아웃, 할당 전략을 제어함. 기본 할당자에는 균일(uniform) 및 범위(range) 전략이 포함됨.

#### 10.3 컨슈머 설정

클라이언트는 `group.protocol=consumer`를 사용하여 새 프로토콜을 명시적으로 활성화해야 함. 선택적 `group.remote.assignor` 설정은 서버 측 기본값을 재정의할 수 있음.

새로운 구독 방법은 정규식 패턴을 지원함: `subscribe(SubscriptionPattern)` 및 `subscribe(SubscriptionPattern, ConsumerRebalanceListener)`는 RE2J 형식을 사용하여 서버 측에서 패턴을 평가함.

#### 10.4 업그레이드 및 다운그레이드

오프라인 방식: 모든 컨슈머를 종료한 뒤 새 설정으로 재시작함. 그룹이 빌 때 자동으로 프로토콜이 전환됨.

온라인 방식: 롤링 배포로 무중단 전환이 가능함. 모든 컨슈머가 업데이트(또는 다운그레이드)될 때까지 그룹은 두 프로토콜을 상호 운용함.

#### 10.5 제한 사항

- 클라이언트 측 할당자 미지원 (KAFKA-18327)
- 랙 인식 전략 불완전 지원 (KAFKA-17747)

---

### 11. 트랜잭션 프로토콜

#### 11.1 개요

Apache Kafka 4.0은 트랜잭션 프로토콜을 강화하는 트랜잭션 서버 측 방어(KIP-890)를 도입했음. 핵심 개선 사항은 "프로듀서 에포크가 모든 트랜잭션마다 증가하여 각 트랜잭션이 의도한 메시지만 포함하고 중복 메시지가 다음 트랜잭션에 포함되지 않도록 보장"한다는 점임.

이 프로토콜은 `transaction.version` 기능 플래그를 통해 기본적으로 활성화되며, 클러스터 생성 시 또는 기능 도구를 통해 동적으로 구성할 수 있음.

#### 11.2 업그레이드 및 다운그레이드

새 프로토콜 활성화: 서버에서 `transaction.version=2`를 설정함. 프로듀서 클라이언트(4.0+)는 재시작 없이 다음 연결 시 자동으로 향상된 프로토콜을 채택함. 트랜잭션 진행 중에는 업그레이드되지 않으며 다음 트랜잭션 시작 시 전환됨.

다운그레이드: 업그레이드와 동일한 방식으로 안전하게 진행됨. 서버 측 다운그레이드를 인식한 클라이언트는 첫 번째 트랜잭션부터 이전 프로토콜로 되돌아감.

#### 11.3 성능 영향

재설계된 프로토콜은 파티션 추가를 단일 서버 측 작업으로 통합하여 오버헤드를 줄이고 기존의 클라이언트-검증 왕복을 제거함. 다만 트레이드오프가 있음.

기존에는 클라이언트가 `CONCURRENT_TRANSACTIONS` 오류에 대해 하드코딩된 20ms 재시도 백오프를 사용했음. 이제 서버가 재시도를 내부적으로 처리하므로 보고되는 프로듀서 지연 시간이 증가할 수 있음. 그러나 종단 간 트랜잭션 지연 시간(커밋까지의 시간)은 변하지 않음. 지연이 클라이언트 측 백오프에서 서버 측 처리로 이동했을 뿐임.

구성 가능한 재시도 매개변수:
- `add.partitions.to.txn.retry.backoff.ms`
- `add.partitions.to.txn.retry.backoff.max.ms`

---

### 12. 적격 리더 레플리카 (ELR)

#### 12.1 개요

ELR(Eligible Leader Replicas)은 Apache Kafka 4.0에 도입되었으며(4.1에서 기본 활성화) 리더로 승격하기에 안전한 레플리카를 식별하여 복제 신뢰성을 높임. 이 기능은 ISR이 `min.insync.replicas` 미만으로 떨어질 때 하이 워터마크 진행을 차단하는 Kafka의 "엄격한 최소 ISR" 규칙을 활용함.

리더 선택 우선순위:
KRaft 컨트롤러는 다음 순서로 리더를 선택함:
1. 사용 가능한 경우 In-Sync Replica(ISR) 집합에서
2. ISR이 비어 있으면 적격 리더 레플리카(ELR)에서
3. 언펜스된 경우 마지막으로 알려진 리더 (4.0 이전 동작)

#### 12.2 설정

ELR 활성화: 서버에서 `eligible.leader.replicas.version=1`을 설정함.

ELR 비활성화: `eligible.leader.replicas.version=0`을 설정함 (다운그레이드에 안전).

#### 12.3 중요한 제약 사항

ELR이 활성화되면:

- 클러스터 수준 `min.insync.replicas`가 없으면 자동으로 추가됨
- 클러스터 수준 `min.insync.replicas` 제거가 금지됨
- 브로커 수준 `min.insync.replicas` 설정이 제거됨
- 브로커 수준 `min.insync.replicas` 변경이 허용되지 않음
- `min.insync.replicas` 변경 시 ELR 상태가 리셋됨

#### 12.4 도구 및 접근

ELR 정보는 관리 클라이언트의 `DescribeTopicPartitions` API를 통해 조회할 수 있음.

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Operations](https://kafka.apache.org/documentation/#operations)
- [KRaft Documentation](https://kafka.apache.org/documentation/#kraft)
- [Kafka MirrorMaker 2](https://kafka.apache.org/documentation/#georeplication)

---

## Kafka 업그레이드

> 원본: https://kafka.apache.org/documentation/#upgrade

### 개요

롤링 업그레이드(Rolling Upgrade)를 통해 클러스터 가용성을 유지하면서 새로운 버전으로 업그레이드할 수 있음.

---

### 1. 업그레이드 경로 개요

#### 1.1 지원되는 업그레이드 버전

다음 주요 버전에 대한 업그레이드가 지원됨:

- 4.1.x: Queues for Kafka (Share Groups) 도입
- 4.0.x: KRaft 전용 모드, ZooKeeper 지원 제거
- 3.9.x: KRaft 안정화, 메타데이터 버전 관리 개선
- 3.x.x: KRaft 모드 도입 및 점진적 안정화

---

### 2. 일반 롤링 업그레이드 절차

대부분의 업그레이드에서 표준 절차는 다음과 같음:

#### 2.1 ZooKeeper 기반 클러스터 (3.x 이하)

1. 설정 업데이트: `inter.broker.protocol.version`을 현재 버전으로, `log.message.format.version`을 기존 메시지 형식으로 설정함.

2. 브로커 업그레이드: 브로커를 순차적으로 업그레이드함 (종료 → 코드 업데이트 → 재시작).

3. 프로토콜 버전 업데이트: 클러스터 상태를 확인한 후 `inter.broker.protocol.version`을 대상 버전으로 변경함.

4. 브로커 재시작: 새 프로토콜이 적용되도록 각 브로커를 순차적으로 재시작함.

5. 메시지 형식 업데이트: 이전에 재정의한 경우, 메시지 형식 업그레이드를 위해 롤링 재시작을 한 번 더 수행함.

#### 2.2 KRaft 기반 클러스터 (3.3.0 이상)

KRaft 클러스터는 다단계 프로토콜 버전 업데이트가 필요하지 않음:

1. 브로커 업그레이드: 브로커를 순차적으로 업그레이드함 (종료 → 업데이트 → 재시작).

2. 메타데이터 버전 업그레이드: 클러스터 안정성을 확인한 후 다음 명령을 실행함:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version <버전>
```

예시:
```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.0
```

또는 메타데이터 버전만 지정:
```bash
bin/kafka-features.sh upgrade --metadata 3.9
```

---

### 3. 버전별 업그레이드 가이드

#### 3.1 4.1.x로 업그레이드

##### 4.1.1 업그레이드

4.1.1은 다음과 같은 Kafka Streams 주요 버그를 수정함:
- 범위 스캔(Range Scan)에서의 메모리 누수
- 데이터 손실 문제

##### 4.1.0 업그레이드

새로운 기능: Queues for Kafka (Share Groups)

4.1.0은 "Queues for Kafka"를 도입하여 Share Groups를 Consumer Groups의 대안으로 제공함. 파티션 할당 없이 협력적으로 레코드를 소비할 수 있음.

#### 3.2 4.0.x로 업그레이드

##### 중요 요구 사항

KRaft 모드 필수: Apache Kafka 4.0은 KRaft 모드만 지원하며, ZooKeeper 지원이 완전히 제거되었음.

> 경고: 4.0.0 이상으로 브로커를 업그레이드하려면 KRaft 모드가 필요하며, 소프트웨어 및 메타데이터 버전이 3.3.x 이상이어야 함.

##### 클라이언트 요구 사항

Streams 및 Connect를 포함한 클라이언트는 4.0으로 업그레이드하기 전에 버전 2.1 이상이어야 함.

##### 롤링 업그레이드 절차

1. 브로커를 순차적으로 업그레이드함 (종료 → 업데이트 → 재시작).

2. 클러스터 안정성을 확인한 후 다음 명령으로 업그레이드를 완료함:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.0
```

3. 상태 변경 로그 파일 로테이션이 `stage-change.log.[date]`에서 `state-change.log.[date]`로 수정되었음.

##### 주요 호환성 변경 사항

프로토콜 및 호환성:
- 이전 프로토콜 API 버전 제거; 브로커와 클라이언트 모두 2.1 이상 필요
- Java 요구 버전 상향: 클라이언트/Streams는 Java 11, 브로커/Connect/도구는 Java 17
- Scala 2.12 지원 중단

설정 제거:
- `log.message.format.version` 및 `message.format.version` 제거
- ZooKeeper 전용 도구 및 보안 마이그레이터 제거
- 사용 중단(deprecated)된 다수의 메트릭 및 클래스 제거

Consumer/Producer 변경:
- `poll(long)` 메서드 제거; `poll(Duration)` 사용

```java
// 이전 (제거됨)
consumer.poll(100);

// 새 방식
consumer.poll(Duration.ofMillis(100));
```

- `linger.ms` 기본값이 0에서 5ms로 변경
- `enable.idempotence`가 더 이상 `max.in.flight.requests.per.connection`을 자동으로 조정하지 않음

Kafka Streams 변경:
- `KStream#transformValues()` 제거; `KStream#processValues()`로 마이그레이션 필요

```java
// 이전 방식 (제거됨)
stream.transformValues(...)

// 새 방식
stream.processValues(...)
```

- 3.6 이전에 사용 중단된 모든 API 제거

로깅 변경:
- Log4j에서 Log4j2로 전환

MirrorMaker 변경:
- 기존 MirrorMaker(MM1) 제거; Connect 기반 MM2 사용

새 기능 활성화:
- 차세대 Consumer Rebalance Protocol (KIP-848) GA(Generally Available) 전환
- Transactions Server Side Defense (KIP-890)로 트랜잭션 보장 강화
- Eligible Leader Replicas로 복제 프로토콜 개선

#### 3.3 3.9.x로 업그레이드

##### 주요 변경 사항

- 사용 중단된 프로토콜 API 버전의 요청 로그 레벨이 DEBUG에서 INFO로 변경
- 요청 로깅을 선택적으로 구성할 수 있도록 개선

##### 업그레이드 명령

```bash
bin/kafka-features.sh upgrade --metadata 3.9
```

#### 3.4 3.8.x로 업그레이드

##### ZooKeeper 요구 사항

ZooKeeper를 3.8.3 이상으로 업그레이드해야 함.

##### 압축 라이브러리 설정

`/tmp` 파티션에 실행 권한이 필요함. 실행 권한을 부여할 수 없는 경우 다음 JVM 플래그를 설정함:

```bash
-DZstdTempFolder=/opt/kafka/tmp -Dorg.xerial.snappy.tempdir=/opt/kafka/tmp
```

#### 3.5 3.0.x 이상으로 업그레이드

- `log.message.format.version` 설정이 사용 중단됨
- `inter.broker.protocol.version`이 3.0 이상인 경우 두 설정 모두 3.0으로 간주됨

#### 3.6 2.1.x 이상으로 업그레이드

Consumer 오프셋 저장 스키마가 영구적으로 변경됨. 업그레이드 후에는 2.1 이전 버전으로 다운그레이드할 수 없음.

---

### 4. 클라이언트 업그레이드

#### 4.1 롤링 클라이언트 업그레이드

클라이언트를 개별적으로 종료하고, 코드를 업데이트한 후 재시작하는 방식으로 진행함.

```bash
# 1. 클라이언트 종료
# 2. 코드 업데이트
# 3. 순차적으로 재시작
```

#### 4.2 클라이언트 호환성

- 4.0.x
  - 최소 브로커 버전: 2.1.x
  - 비고: KRaft 모드 권장
- 3.x.x
  - 최소 브로커 버전: 0.10.0.0
  - 비고: ZooKeeper 또는 KRaft
- 2.x.x
  - 최소 브로커 버전: 0.10.0.0
  - 비고: ZooKeeper

---

### 5. 성능 고려 사항

#### 5.1 메시지 형식 변환

0.10.0.0에서 업그레이드하면 메시지마다 타임스탬프 필드가 추가됨. 메시지 형식 변환이 대규모로 실행되면 CPU 사용률이 업그레이드 전 20%에서 100%까지 치솟을 수 있음.

Consumer 업그레이드가 완료될 때까지 `log.message.format.version` 업데이트를 늦추면 브로커 측의 비용이 큰 변환을 방지할 수 있음.

#### 5.2 압축 메시지

0.11.0에서 snappy로 압축된 메시지는 1KB 대신 압축 스킴의 기본 블록 크기(2 × 32KB)를 사용함. 압축률은 개선되지만 힙 사용량이 비례하여 증가함.

---

### 6. 다운그레이드 제한 사항

#### 6.1 일반적인 제한

대부분의 주요 버전 업그레이드는 프로토콜 버전 업데이트가 완료되면 다운그레이드가 차단됨.

#### 6.2 KRaft 클러스터 제한

3.3-IV0 이후 KRaft 클러스터는 메타데이터 스키마 변경으로 인해 다운그레이드가 명시적으로 차단됨.

> 경고: 이 소프트웨어 버전에는 메타데이터 변경이 포함되므로 클러스터 메타데이터 다운그레이드는 지원되지 않음.

---

### 7. ZooKeeper에서 KRaft로 마이그레이션

#### 7.1 마이그레이션 개요

Kafka 4.0부터 ZooKeeper 지원이 완전히 제거되었기 때문에, 4.0 이상으로 업그레이드하기 전에 KRaft로 마이그레이션해야 함.

#### 7.2 마이그레이션 전제 조건

1. 현재 클러스터가 3.3.x 이상에서 실행 중이어야 함
2. 모든 클라이언트가 2.1 이상이어야 함
3. 메타데이터 버전이 3.3.x 이상이어야 함

#### 7.3 마이그레이션 단계

1. KRaft 컨트롤러 배포: 새로운 KRaft 컨트롤러 노드를 배포함

2. 마이그레이션 모드 활성화: 브로커 설정에서 마이그레이션 모드를 활성화함

3. 메타데이터 마이그레이션: ZooKeeper 메타데이터를 KRaft로 마이그레이션함

4. ZooKeeper 의존성 제거: 마이그레이션이 완료되면 ZooKeeper 의존성을 제거함

5. 마이그레이션 완료: 클러스터가 완전히 KRaft 모드로 전환됨

---

### 8. 버전별 주요 변경 사항 요약

#### 8.1 4.0.0 주요 변경 사항

- 플랫폼: ZooKeeper 지원 제거
- Java: 클라이언트: Java 11, 브로커: Java 17 필수
- Scala: 2.12 지원 중단
- API: 사용 중단된 API 다수 제거
- 로깅: Log4j2로 마이그레이션
- 도구: MirrorMaker 1 제거

#### 8.2 3.2.0 주요 변경 사항

- Producer 멱등성(idempotence) 기본 활성화
- 이전 브로커에서 `IDEMPOTENT_WRITE` 권한 필요

#### 8.3 3.0.0 주요 변경 사항

- 사용 중단된 Scala 클라이언트 제거
- 새 메시지 형식에 Java 클라이언트 필수

#### 8.4 2.4.0 주요 변경 사항

- 기본 파티셔너가 스티키 할당 전략 사용
- 배치 분배에 영향

#### 8.5 2.0.0 주요 변경 사항

- Scala consumer/producer 제거
- Java 클라이언트 필수

#### 8.6 0.11.0 주요 변경 사항

- 새 메시지 형식에 타임스탬프 포함
- 이전 Scala 클라이언트는 성능 오버헤드 없이는 읽을 수 없음

---

### 9. 업그레이드 체크리스트

업그레이드를 수행하기 전에 다음 사항을 확인:

- [ ] 대상 버전의 릴리스 노트 검토
- [ ] 클라이언트 호환성 확인
- [ ] Java 버전 요구 사항 충족 확인
- [ ] 백업 수행
- [ ] 스테이징 환경에서 테스트
- [ ] 롤백 계획 수립
- [ ] 모니터링 및 알림 설정

---

### 10. 문제 해결

#### 10.1 업그레이드 후 클러스터가 불안정한 경우

1. 모든 브로커가 동일한 버전을 실행하는지 확인
2. 메타데이터 버전이 올바르게 설정되었는지 확인
3. 로그에서 오류 메시지 확인

#### 10.2 클라이언트 연결 문제

1. 클라이언트 버전이 브로커와 호환되는지 확인
2. 프로토콜 버전 설정 확인
3. 네트워크 연결 상태 확인

#### 10.3 성능 저하

1. 메시지 형식 변환이 발생하는지 확인
2. `log.message.format.version` 설정 검토
3. JVM 힙 크기 조정 고려

---

## Kafka KRaft 모드

> 원본: https://kafka.apache.org/documentation/#kraft

### 개요

KRaft는 Apache Kafka의 메타데이터 관리 시스템으로, ZooKeeper를 대체함. KRaft의 설정, 업그레이드, 노드 프로비저닝, 운영 방법 및 ZooKeeper에서 KRaft로의 마이그레이션 절차를 다룸.

---

### 1. 설정 (Configuration)

#### 프로세스 역할 (Process Roles)

`process.roles` 속성으로 각 서버의 역할을 정의함:

- `broker`: 브로커로만 동작
- `controller`: 컨트롤러로만 동작
- `broker,controller`: 두 역할 모두 수행 ("combined" 서버)

> 참고: Combined 서버는 개발 환경 등 소규모 환경에서는 운영이 단순하지만, 프로덕션 환경에서는 권장하지 않음.

#### 컨트롤러 구성

메타데이터 쿼럼(Metadata Quorum)에 참여할 컨트롤러를 선택함:

- 일반적으로 3개 또는 5개 서버 선택
- 3개 컨트롤러: 1개 장애 허용
- 5개 컨트롤러: 2개 장애 허용

필수 설정:

```properties
controller.quorum.bootstrap.servers=host1:port1,host2:port2,host3:port3
```

컨트롤러 구성 예시:

```properties
process.roles=controller
node.id=1
listeners=CONTROLLER://controller1.example.com:9093
controller.quorum.bootstrap.servers=controller1.example.com:9093,controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

---

### 2. 업그레이드 (Upgrade)

#### KRaft 버전 확인

```bash
bin/kafka-features.sh --bootstrap-controller localhost:9093 describe
```

동적 컨트롤러 구성은 `kraft.version=1` (Kafka 4.1 이상)에서 지원됨.

#### KRaft 버전 업그레이드

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.1
```

#### 설정 업데이트

`kraft.version` 1 이상으로 업그레이드 후:

- `controller.quorum.voters` 제거
- `controller.quorum.bootstrap.servers` 추가

---

### 3. 노드 프로비저닝 (Provisioning Nodes)

#### 클러스터 ID 생성

```bash
bin/kafka-storage.sh random-uuid
```

#### 독립 컨트롤러 부트스트랩

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> --standalone --config config/controller.properties
```

이 명령은 `meta.properties` 파일과 초기 스냅샷을 생성함.

#### 다중 컨트롤러로 부트스트랩

```bash
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_0_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_1_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_2_UUID="$(bin/kafka-storage.sh random-uuid)"

bin/kafka-storage.sh format --cluster-id ${CLUSTER_ID} \
  --initial-controllers "0@controller-0:1234:${CONTROLLER_0_UUID},1@controller-1:1234:${CONTROLLER_1_UUID},2@controller-2:1234:${CONTROLLER_2_UUID}" \
  --config config/controller.properties
```

#### 브로커 및 새 컨트롤러 포맷

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> --config config/server.properties --no-initial-controllers
```

---

### 4. 컨트롤러 멤버십 변경 (Controller Membership Changes)

#### 정적 vs 동적 쿼럼

- 정적 (Static): `controller.quorum.voters`로 모든 컨트롤러 명시
- 동적 (Dynamic): `controller.quorum.bootstrap.servers` 사용 (KIP-853)

#### 새 컨트롤러 추가

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 add-controller
```

또는 컨트롤러 엔드포인트 사용:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-controller localhost:9093 add-controller
```

#### 컨트롤러 제거

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 remove-controller --controller-id <id> --controller-directory-id <directory-id>
```

---

### 5. 디버깅 (Debugging)

#### 메타데이터 쿼럼 도구

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 describe --status
```

출력 예시:

```
ClusterId:              fMCL8kv1SWm87L_Md-I2hg
LeaderId:               3002
LeaderEpoch:            2
HighWatermark:          10
```

#### 로그 덤프 도구

```bash
bin/kafka-dump-log.sh --cluster-metadata-decoder --files metadata_log_dir/__cluster_metadata-0/00000000000000000000.log
```

#### 메타데이터 셸

```bash
bin/kafka-metadata-shell.sh --snapshot metadata_log_dir/__cluster_metadata-0/00000000000000007228-0000000001.checkpoint
```

---

### 6. 배포 고려사항 (Deploying Considerations)

KRaft 모드로 클러스터를 배포할 때 고려할 사항:

- `process.roles`를 `broker` 또는 `controller`로 설정 (프로덕션에서는 둘 다 아님)
- 고가용성을 위해 3개 이상의 컨트롤러 권장
- N개 장애 허용 시 2N+1개 컨트롤러 필요
- 각 컨트롤러: 약 5GB 메모리, 5GB 메타데이터 로그 디스크 공간 권장

---

### 7. ZooKeeper에서 KRaft로의 마이그레이션

> 중요: ZooKeeper에서 KRaft로 마이그레이션하려면 브릿지 릴리스(Bridge Release)를 사용해야 함. 마지막 브릿지 릴리스는 Kafka 3.9임.

#### 용어 정의

- ZK 모드: 브로커가 Apache ZooKeeper에 메타데이터를 저장하는 기존 방식
- KRaft 모드: 브로커가 KRaft 쿼럼에 메타데이터를 저장하는 신규 방식
- 마이그레이션: ZooKeeper의 클러스터 메타데이터를 KRaft 쿼럼으로 이동하는 프로세스

#### 마이그레이션 단계

마이그레이션은 5가지 주요 단계를 거침:

1. 초기 단계: 모든 브로커가 ZK 모드, ZK 기반 컨트롤러 운영
2. 메타데이터 로드: KRaft 쿼럼이 ZooKeeper에서 메타데이터 로드
3. 하이브리드 단계: 일부 브로커는 ZK 모드, 컨트롤러는 KRaft 모드
4. 듀얼-라이트 단계 (Dual-Write): 모든 브로커가 KRaft이지만 컨트롤러는 ZK에도 기록
5. 완료 단계: 더 이상 ZooKeeper에 메타데이터를 기록하지 않음

#### 주요 제약사항

- 마이그레이션 중 메타데이터 버전(`inter.broker.protocol.version`) 변경 불가능
- 완료 후 ZooKeeper 모드로의 복귀 불가능
- 다중 로그 디렉토리를 가진 ZK 브로커는 디렉토리 장애 시 종료됨

---

### 8. 마이그레이션 준비

#### 소프트웨어 요구사항

브로커를 버전 3.9.1 이상으로 업그레이드하고 `inter.broker.protocol.version`을 `3.9`로 설정함.

#### 로깅 설정

KRaft 컨트롤러의 `log4j.properties`에 다음을 추가함:

```properties
log4j.logger.org.apache.kafka.metadata.migration=TRACE
```

---

### 9. KRaft 컨트롤러 쿼럼 프로비저닝

#### 기존 클러스터의 클러스터 ID 확인

```bash
bin/zookeeper-shell.sh localhost:2181 get /cluster/id
```

#### 마이그레이션 대비 KRaft 컨트롤러 샘플 설정

```properties
process.roles=controller
node.id=3000
controller.quorum.bootstrap.servers=localhost:9093
controller.listener.names=CONTROLLER
listeners=CONTROLLER://:9093
zookeeper.metadata.migration.enable=true
zookeeper.connect=localhost:2181
inter.broker.listener.name=PLAINTEXT
```

> 중요: KRaft 클러스터의 `node.id` 값은 기존 ZK 브로커의 `broker.id`와 달라야 함.

---

### 10. 브로커 마이그레이션 모드 진입

각 브로커에 다음 설정을 추가함:

- `controller.quorum.voters`
- `controller.listener.names`
- `zookeeper.metadata.migration.enable`

#### 마이그레이션 대비 ZK 브로커 샘플 설정

```properties
broker.id=0
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
inter.broker.protocol.version=3.9
zookeeper.metadata.migration.enable=true
zookeeper.connect=localhost:2181
controller.quorum.bootstrap.servers=localhost:9093
controller.listener.names=CONTROLLER
```

ZK 브로커를 모두 재시작하면 마이그레이션이 자동으로 시작됨.

완료 신호 (컨트롤러 로그에서 확인):

```
Completed migration of metadata from Zookeeper to KRaft
```

---

### 11. 브로커를 KRaft로 마이그레이션

각 브로커의 설정을 아래와 같이 변경한 후 재시작함:

```properties
process.roles=broker
node.id=0
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
controller.quorum.voters=3000@localhost:9093
controller.listener.names=CONTROLLER
```

#### 제거할 설정

- `zookeeper.metadata.migration.enable`
- `inter.broker.protocol.version`
- `zookeeper` 관련 설정들
- `control.plane.listener.name`

#### 권한 설정 변경

```
kafka.security.authorizer.AclAuthorizer → org.apache.kafka.metadata.authorizer.StandardAuthorizer
```

---

### 12. 마이그레이션 최종화

모든 브로커가 KRaft 모드로 전환된 후 각 KRaft 컨트롤러에서 마이그레이션 플래그를 제거함:

```properties
# 다음 설정을 제거합니다:
# zookeeper.metadata.migration.enable=true
# zookeeper.connect=localhost:2181
```

컨트롤러를 한 번에 하나씩 재시작함.

완료 후 ZooKeeper 클러스터는 안전하게 폐기할 수 있음.

---

### 13. 마이그레이션 중 ZooKeeper 모드로의 복귀

완료 단계별 롤백 절차:

- 마이그레이션 준비: 작업 없음
- KRaft 쿼럼 프로비저닝: KRaft 쿼럼 폐기
- 브로커 마이그레이션 모드 진입: KRaft 쿼럼 폐기 후 `zookeeper-shell.sh`에서 `rmr /controller` 실행, 브로커 설정 되돌려 재시작
- 브로커를 KRaft로 마이그레이션: 브로커를 ZK 설정으로 복원하여 2회 재시작, `rmr /controller` 실행
- 마이그레이션 최종화: 복귀 불가능

> 주의: `/controller` 제거 시 빠르게 수행하여 컨트롤러 없는 시간을 최소화해야 함.

> 권장 사항: 일부 사용자는 ZooKeeper 클러스터를 유지하면서 1-2주 대기 후 최종화하는 것을 권장함.

---

### 요약

#### KRaft 모드의 장점

- 단순화된 아키텍처: ZooKeeper 의존성 제거로 운영 복잡성 감소
- 향상된 확장성: 더 많은 파티션 지원 가능
- 빠른 복구: 컨트롤러 장애 조치 시간 단축
- 단일 보안 모델: Kafka 자체 보안 메커니즘만 관리

#### 마이그레이션 체크리스트

- [ ] Kafka 브릿지 릴리스(3.9) 사용
- [ ] `inter.broker.protocol.version` 설정 확인
- [ ] KRaft 컨트롤러 쿼럼 프로비저닝
- [ ] 브로커 마이그레이션 모드 설정
- [ ] 브로커 KRaft 모드 전환
- [ ] 마이그레이션 최종화
- [ ] ZooKeeper 클러스터 폐기

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [KRaft 운영 가이드](https://kafka.apache.org/documentation/#kraft)
- [KIP-500: Replace ZooKeeper with a Self-Managed Metadata Quorum](https://cwiki.apache.org/confluence/display/KAFKA/KIP-500%3A+Replace+ZooKeeper+with+a+Self-Managed+Metadata+Quorum)
- [KIP-853: Dynamic Quorum Reconfiguration](https://cwiki.apache.org/confluence/display/KAFKA/KIP-853%3A+KRaft+Controller+Membership+Changes)

---

## Kafka Docker

> 원본: https://kafka.apache.org/documentation/#docker

### 개요

Apache Kafka는 공식 Docker 이미지를 제공하여 컨테이너 환경에서 Kafka를 배포하고 실행할 수 있음.

---

### 시스템 요구사항

Docker 버전 20.10.4 이상이 필요함. 이전 버전에서는 `/opt/kafka/config`와 같은 컨테이너 경로를 생성할 때 디렉토리 권한이 올바르게 설정되지 않아 권한 오류가 발생할 수 있음.

---

### Docker 이미지

Apache Kafka는 두 가지 공식 Docker 이미지를 제공함:

#### JVM 기반 이미지

표준 JVM에서 실행되는 Kafka 이미지임.

```bash
docker pull apache/kafka:4.1.1
docker run -p 9092:9092 apache/kafka:4.1.1
```

#### GraalVM Native 이미지

GraalVM으로 네이티브 컴파일된 이미지로, 더 빠른 시작 시간과 낮은 메모리 사용량을 제공함.

```bash
docker pull apache/kafka-native:4.1.1
docker run -p 9092:9092 apache/kafka-native:4.1.1
```

---

### 구성 방법

Kafka Docker 컨테이너를 구성하는 세 가지 방법이 있음:

#### 1. 기본 구성 (Default Configuration)

사용자 정의 구성(파일 입력 또는 환경 변수)이 Docker 컨테이너에 전달되지 않으면, Kafka tarball에 패키지된 기본 KRaft 구성이 사용됨. 이 기본 구성은 단일 결합 모드(combined-mode) 노드용임.

```bash
docker run -p 9092:9092 apache/kafka:latest
```

#### 2. 파일 입력 방식 (File Input)

속성 파일이 포함된 로컬 폴더를 Docker 볼륨을 통해 컨테이너에 마운트할 수 있음.

```bash
docker run --volume /path/to/property/folder:/mnt/shared/config -p 9092:9092 apache/kafka:latest
```

이 방식을 사용하면 기본 KRaft 구성이 사용자가 제공한 속성 파일로 대체됨.

#### 3. 환경 변수 방식 (Environment Variables)

환경 변수를 사용하여 Kafka 속성을 설정할 수 있음. 환경 변수 명명 규칙:

- 속성 키 앞에 `KAFKA_` 접두사를 붙임
- 점(`.`)을 밑줄(`_`)로 변환함
- 밑줄(`_`)을 이중 밑줄(`__`)로 변환함
- 하이픈(`-`)을 삼중 밑줄(`___`)로 변환함

##### 변환 예시

- `abc.def`: `KAFKA_ABC_DEF`
- `abc_def`: `KAFKA_ABC__DEF`
- `abc-def`: `KAFKA_ABC___DEF`
- `num.partitions`: `KAFKA_NUM_PARTITIONS`

참고: 환경 변수로 정의한 Kafka 속성은 속성 파일에 정의된 동일 속성 값을 덮어씀. 공통 구성은 파일로, 노드별 속성은 환경 변수로 재정의하는 방식도 가능함.

---

### 주요 환경 변수

KRaft 모드에서 Kafka를 구성하는 데 사용되는 주요 환경 변수:

- `KAFKA_NODE_ID`: 노드 식별자
- `KAFKA_PROCESS_ROLES`: 노드의 역할 (broker, controller, 또는 둘 다)
- `KAFKA_LISTENERS`: 리스너 구성
- `KAFKA_ADVERTISED_LISTENERS`: 외부에서 접근 가능한 리스너 주소
- `KAFKA_CONTROLLER_LISTENER_NAMES`: 컨트롤러 리스너 이름
- `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`: 보안 프로토콜 매핑
- `KAFKA_CONTROLLER_QUORUM_VOTERS`: KRaft 모드의 쿼럼 투표자
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`: 오프셋 토픽의 복제 인수
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR`: 트랜잭션 로그의 복제 인수
- `KAFKA_NUM_PARTITIONS`: 기본 파티션 수

#### 단일 노드 주의사항

단일 노드로 실행할 때는 `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`를 명시적으로 1로 설정해야 함. 설정하지 않으면 기본값인 3이 사용되어 오류가 발생함.

```bash
docker run -p 9092:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

---

### 배포 예제

#### 단일 노드 (Plaintext)

Docker Compose를 사용한 단일 노드 배포:

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/single-node/plaintext/docker-compose.yml up
```

메시지 생성 테스트:

```bash
bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9092
```

#### 단일 노드 Docker Compose 예제

```yaml
version: '3'
services:
  kafka:
    image: apache/kafka:latest
    hostname: broker
    container_name: broker
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@broker:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://broker:29092,CONTROLLER://broker:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_PROCESS_ROLES: 'broker,controller'
```

#### 다중 노드 클러스터 (Combined Mode - Plaintext)

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/cluster/combined/plaintext/docker-compose.yml up
```

각 브로커는 고유한 포트(29092, 39092, 49092)를 호스트에 노출함.

#### 다중 노드 클러스터 (Isolated Mode)

컨트롤러와 브로커가 별도로 구성되며, 브로커는 컨트롤러에 의존함:

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/cluster/isolated/plaintext/docker-compose.yml up
```

---

### 리스너 설정

Docker 환경에서 Kafka 리스너를 올바르게 구성하는 것이 중요함:

- `PLAINTEXT`: 컨테이너 호스트 이름을 통한 브로커 간 통신
- `PLAINTEXT_HOST`: localhost를 통한 클라이언트-브로커 통신
- `SSL` / `SSL-INTERNAL`: 프로덕션 환경을 위한 암호화된 통신

---

### SSL 구성

SSL을 사용하여 Kafka를 보안하는 권장 방법:

1. 시크릿을 컨테이너의 `/etc/kafka/secrets`에 마운트함
2. SSL 자격 증명을 환경 변수로 제공함
3. SSL 리스너와 함께 적절한 `KAFKA_ADVERTISED_LISTENERS`를 설정함

#### SSL 관련 환경 변수

- `KAFKA_SSL_KEYSTORE_FILENAME`: 키스토어 파일 이름
- `KAFKA_SSL_KEYSTORE_CREDENTIALS`: 키스토어 자격 증명 파일
- `KAFKA_SSL_TRUSTSTORE_FILENAME`: 트러스트스토어 파일 이름
- `KAFKA_SSL_TRUSTSTORE_CREDENTIALS`: 트러스트스토어 자격 증명 파일
- `KAFKA_SSL_KEY_CREDENTIALS`: SSL 키 자격 증명 파일

#### 단일 노드 SSL 배포

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/single-node/ssl/docker-compose.yml up
```

---

### Log4j 구성

Docker 환경에서 로깅을 구성할 수 있음:

- `KAFKA_LOG4J_ROOT_LOGLEVEL`: 루트 로거 레벨 설정
- `KAFKA_LOG4J_LOGGERS`: 커스텀 로거를 위한 쉼표로 구분된 속성 쌍

예시:

```bash
docker run -p 9092:9092 \
  -e KAFKA_LOG4J_ROOT_LOGLEVEL=INFO \
  -e KAFKA_LOG4J_LOGGERS="kafka.controller=WARN,kafka.producer=WARN" \
  apache/kafka:latest
```

---

### ZooKeeper vs KRaft 모드

#### KRaft 모드 (권장)

최신 Kafka 버전은 ZooKeeper 없이 클러스터 조율을 내장 처리하는 KRaft 모드를 지원함. KRaft 모드는 다음과 같은 장점을 제공함:

- 외부 ZooKeeper 의존성 제거
- 더 간단한 배포 및 운영
- 향상된 확장성

#### ZooKeeper 지원 종료

- ZooKeeper는 Kafka 버전 3.5에서 더 이상 사용되지 않음(deprecated)으로 표시되었음
- Kafka 버전 4.0에서 ZooKeeper 지원이 제거되었음

---

### 문제 해결

#### 권한 오류

Docker 버전이 20.10.4 이상인지 확인 필요. 이전 버전에서는 컨테이너 경로 생성 시 권한 오류가 발생할 수 있음.

```bash
docker --version
```

#### 복제 인수 오류

단일 노드 배포 시 다음 환경 변수를 명시적으로 설정:

```bash
-e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
-e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
```

#### 연결 문제

`KAFKA_ADVERTISED_LISTENERS`가 클라이언트가 접근할 수 있는 주소로 올바르게 설정되어 있는지 확인 필요.

---

### 요약

- JVM 이미지: `apache/kafka:latest` - 표준 JVM 기반
- Native 이미지: `apache/kafka-native:latest` - GraalVM 네이티브 컴파일
- 구성 방식: 기본 구성, 파일 입력, 환경 변수
- Docker 최소 버전: 20.10.4 이상
- 권장 모드: KRaft (ZooKeeper 없음)

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Apache Kafka Docker Hub](https://hub.docker.com/r/apache/kafka)
- [Apache Kafka Native Docker Hub](https://hub.docker.com/r/apache/kafka-native)
- [Kafka Docker 예제 (GitHub)](https://github.com/apache/kafka/blob/trunk/docker/examples/README.md)
- [Kafka Quickstart](https://kafka.apache.org/quickstart)
