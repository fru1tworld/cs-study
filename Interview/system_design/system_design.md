# System Design (시스템 설계)

> 카테고리: 시스템 설계
> [← 면접 질문 목록으로 돌아가기](../interview.md)

---

## 📌 이벤트, 메시지, 그리고 EDA (Event-Driven Architecture)

### SD-001

 메시지(Message) 와  이벤트(Event) 의 근본적인 차이점은 무엇인가요?

### SD-002

"메시지는 '지시(Command)'이고 이벤트는 '사실(Fact)'이다"라는 말에 대해 설명해 보세요.

### SD-003

어떤 상황에서  메시지 큐(Message Queue) 를 사용하고, 어떤 상황에서  이벤트 브로커/스트림(Event Broker/Stream) 을 사용해야 할까요?

### SD-004

이벤트는 '발신자(Publisher)'가 '수신자(Subscriber)'를 몰라야 한다는 특징이 있습니다. 이것이 시스템 설계에 어떤 이점과 단점을 주나요?

### SD-005

 이벤트 드리븐 아키텍처(EDA) 란 무엇이며, 기존의 요청-응답(Request-Response) 모델과 무엇이 다른가요?

### SD-006

EDA를 도입했을 때 얻을 수 있는 가장 큰 장점 3가지와 가장 큰 단점 3가지는 무엇인가요?

### SD-007

EDA에서 서비스 간의 데이터 흐름을 관리하는 두 가지 방식, Choreography와 Orchestration을 비교 설명해 주세요.

### SD-008

이벤트 브로커(예: Kafka, RabbitMQ)가 다운되면 전체 시스템이 마비될 수 있습니다. 이 SPOF(Single Point of Failure) 문제를 어떻게 해결할 수 있을까요?

### SD-009

메시지/이벤트 전송 보장 레벨인 'At-least-once', 'At-most-once', 'Exactly-once' 의 차이점은 무엇인가요?

### SD-010

EDA에서 'Exactly-once' 를 구현하기 어려운 이유는 무엇이며, 이를 위해 어떤 기술(예: Idempotency)이 필요한가요?

### SD-011

이벤트 스키마(Event Schema) 관리는 왜 중요한가요? 스키마가 변경될 때 하위 호환성을 어떻게 보장할 수 있을까요?

### SD-012

이벤트가  불변성(Immutability) 을 가져야 하는 이유는 무엇인가요?

### SD-013

만약 과거에 발생한 이벤트 데이터에 오류가 있었다면, 불변성 원칙을 지키면서 이 오류를 어떻게 수정(또는 보정)해야 할까요?

---

## 📌 분산 트랜잭션, SAGA, 그리고 이벤트 소싱

### SD-014

 분산 트랜잭션(Distributed Transaction) 이 무엇인지, 그리고 왜 필요한지 설명해 주세요.

### SD-015

전통적인 분산 트랜잭션 기법인 2PC(Two-Phase Commit) 프로토콜에 대해 설명해 주세요.

### SD-016

2PC의 가장 큰 단점(예: 코디네이터 장애, 블로킹)은 무엇이며, 이 때문에 실제 환경에서 잘 사용되지 않는 이유는 무엇인가요?

### SD-017

2PC의 대안으로 등장한 SAGA 패턴이 무엇인지, 그리고 2PC와 어떻게 다른지 설명해 주세요.

### SD-018

SAGA의 핵심 구성요소인 '보상 트랜잭션(Compensating Transaction)' 이란 무엇이며, 이를 설계할 때 가장 중요하게 고려해야 할 점은 무엇인가요?

### SD-019

보상 트랜잭션 자체가 실패하면 어떻게 해야 하나요?

### SD-020

SAGA 패턴에서 발생하는 '중간 상태(intermediate state)' 란 무엇을 의미하나요? (예: 주문은 완료됐으나, 결제는 진행 중인 상태)

### SD-021

이 '중간 상태'가 비즈니스 로직이나 사용자 경험(UX)에 어떤 문제를 일으킬 수 있으며, 이를 어떻게 처리해야 할까요?

### SD-022

SAGA를 구현하는 두 가지 방식, 'Choreography' 와 'Orchestration' 을 비교 설명하고, 각각의 장단점을 논해주세요.

### SD-023

이벤트 소싱(Event Sourcing) 패턴이 무엇인지 설명해 주세요.

### SD-024

이벤트 소싱에서 '현재 상태(Current State)'는 어떻게 계산하나요?

### SD-025

이벤트 소싱을 사용하면 얻을 수 있는 장점(예: 감사 로그, 시간 여행)은 무엇인가요?

### SD-026

이벤트 소싱의 단점, 특히 이벤트가 누적될수록 '현재 상태'를 재구성하는 성능 문제를 어떻게 해결할 수 있나요? (힌트: Snapshot)

### SD-027

 스냅샷(Snapshot) 의 생성 주기(frequency)는 어떻게 결정하는 것이 좋을까요?

### SD-028

서비스 로직에서 'DB 트랜잭션'과 '이벤트 발행'을 원자적으로 묶고 싶을 때(Dual-write 문제) 어떻게 해야 할까요?

### SD-029

이 문제를 해결하기 위한 'Transactional Outbox' 패턴에 대해 설명해 주세요.

### SD-030

Outbox 패턴을 사용할 때, DB Outbox 테이블에 저장된 이벤트를 어떻게 안정적으로 이벤트 브로커에게 전달할 수 있을까요? (예: CDC, Polling)

---

## 📌 CQRS (Command Query Responsibility Segregation)

### SD-031

CQRS 패턴이 무엇인지 CQS(Command Query Separation) 원칙과 비교하여 설명해 주세요.

### SD-032

CQRS를 사용하는 가장 주된 이유는 무엇인가요?

### SD-033

CQRS 패턴을 도입하면 시스템이 어떻게 복잡해지나요?

### SD-034

CQRS에서 'Command 모델'과 'Query 모델'의 데이터 동기화는 어떻게 이루어지나요?

### SD-035

이 동기화 과정에서 발생하는 '지연(lag)'으로 인해 '최종 일관성(Eventual Consistency)' 이 나타납니다. 이 문제를 어떻게 처리해야 할까요?

### SD-036

CQRS에서 'Query 모델(Read Model)'은 어떤 기술을 사용해 구현하는 것이 좋을까요? (예: RDB, NoSQL, 검색 엔진)

### SD-037

모든 시스템에 CQRS를 적용하는 것이 좋을까요? 어떤 경우에 CQRS가 적합하고, 어떤 경우에 부적합할까요?

### SD-038

"CQRS는 이벤트 소싱이 아니다" 라는 말에 대해 어떻게 생각하시나요? 둘의 관계를 설명해 주세요.

### SD-039

CQRS와 이벤트 소싱을 함께 사용할 때 얻을 수 있는 시너지는 무엇인가요?

---

## 📌 데이터베이스 샤딩 (Sharding)

### SD-040

 데이터베이스 샤딩(Sharding) 이 무엇이며, 왜 필요한가요?

### SD-041

샤딩(Sharding)과  파티셔닝(Partitioning) 의 차이점은 무엇인가요?

### SD-042

 수직 샤딩(Vertical Sharding) 과  수평 샤딩(Horizontal Sharding) 을 비교 설명해 주세요.

### SD-043

언제 수평 샤딩을 선택하고, 언제 수직 샤딩을 선택해야 할까요?

### SD-044

 샤딩 키(Shard Key) 를 선정할 때 가장 중요하게 고려해야 할 기준은 무엇인가요?

### SD-045

샤딩 키를 잘못 선정하면 어떤 문제가 발생할 수 있나요? (예: Hotspot)

### SD-046

특정 샤드에만 데이터가 몰리는 '핫스팟' 문제를 완화하기 위한 전략에는 무엇이 있을까요?

### SD-047

대표적인 샤딩 전략 3가지(예: Range, Hash, Directory-based)를 설명하고 장단점을 비교해 주세요.

### SD-048

MongoDB는 샤딩을 어떻게 구현하나요? 'mongos', 'config server', 'shard'의 역할을 설명해 주세요.

### SD-049

(MongoDB) '청크(Chunk)'란 무엇이며, '밸런서(Balancer)'는 어떤 역할을 하나요?

### SD-050

Elasticsearch는 샤딩을 어떻게 구현하나요? '인덱스', '샤드', '레플리카'의 관계를 설명해 주세요.

### SD-051

Cassandra와 같은 Dynamo-style DB는 '샤딩'이라는 용어 대신 '파티셔닝'을 사용합니다. Cassandra의 데이터 분산 방식(Consistent Hashing)에 대해 설명해 주세요.

### SD-052

(Cassandra) '가상 노드(Virtual Nodes)'가 왜 필요한가요?

### SD-053

Vitess나 Citus와 같이 RDBMS를 샤딩해주는 미들웨어는 어떤 원리로 동작하나요?

### SD-054

샤딩된 환경에서 여러 샤드에 걸친 쿼리(Cross-shard query)는 어떻게 처리해야 하며, 어떤 성능 문제가 있을까요?

### SD-055

샤딩된 환경에서 'JOIN' 연산은 어떻게 수행해야 할까요?

### SD-056

샤딩된 환경에서 '트랜잭션' 은 어떻게 처리해야 할까요? (SAGA와의 연관성)

### SD-057

 리샤딩(Resharding) 은 무엇이며, 언제 필요한가요?

### SD-058

시스템 다운타임 없이 리샤딩을 수행하는 방법에 대해 설명해 주세요.

---

## 📌 분산 시스템 이론 (CAP, Consensus)

### SD-059

CAP 이론(Theorem) 에 대해 설명해 주세요. (Consistency, Availability, Partition Tolerance)

### SD-060

CAP 이론에서 왜 현대 분산 시스템은 'P(Partition Tolerance)' 를 포기할 수 없는지 설명해 주세요.

### SD-061

CAP 이론에 따라 시스템은 CP 또는 AP를 선택해야 합니다. 각각의 특징과 대표적인 시스템 예시를 들어주세요.

### SD-062

CP 시스템은 네트워크 파티션 발생 시 어떻게 동작하나요?

### SD-063

AP 시스템은 네트워크 파티션 발생 시 어떻게 동작하나요?

### SD-064

"CAP 이론은 셋 중 둘만 선택할 수 있다는 것이 아니다"라는 비판이 있습니다. 이 비판의 근거는 무엇인가요? (예: 정상 상황에서의 Latency)

### SD-065

CAP의 'C'는 '강한 일관성(Strong Consistency)' 을 의미합니다. '최종 일관성(Eventual Consistency)' 모델이 무엇인지, AP 시스템과 어떤 관계가 있는지 설명해 주세요.

### SD-066

'강한 일관성'과 '최종 일관성' 사이에는 어떤 다른 일관성 모델들이 존재하나요? (예: Read-your-writes, Monotonic reads)

### SD-067

분산 시스템에서 '합의(Consensus)' 문제는 무엇을 해결하기 위한 것인가요?

### SD-068

 비잔티움 장군 문제(Byzantine Generals Problem) 에 대해 설명해 주세요.

### SD-069

이 문제가 분산 시스템에서 왜 중요한가요? (어떤 종류의 장애를 가정하는 것인가요?)

### SD-070

 비잔티움 장애 허용(BFT, Byzantine Fault Tolerance) 이 무엇인지, 그리고 이것이 블록체인과 어떤 관련이 있는지 설명해 주세요.

### SD-071

BFT를 구현하기 위한 알고리즘(예: PBFT)과, 그렇지 않은 합의 알고리즘(예: Raft, Paxos)의 근본적인 차이는 무엇인가요?

### SD-072

Raft 합의 알고리즘의 '리더 선출(Leader Election)' 과정에 대해 설명해 주세요.

### SD-073

Raft에서 '로그 복제(Log Replication)'는 어떻게 이루어지나요?

---

## 📌 리더십과 복제 (Replication & Leadership)

### SD-074

 데이터베이스 레플리케이션(Replication, 복제) 은 왜 필요한가요? (가용성 vs 확장성)

### SD-075

'동기식(Synchronous)' 복제와 '비동기식(Asynchronous)' 복제의 장단점을 비교 설명해 주세요.

### SD-076

'반-동기식(Semi-Synchronous)' 복제는 무엇이며, 어떤 문제를 해결하기 위해 등장했나요?

### SD-077

단일 리더(Single-Leader) 복제 아키텍처 (Master-Slave)에 대해 설명해 주세요.

### SD-078

단일 리더 아키텍처의 가장 큰 장점과 단점은 무엇인가요?

### SD-079

(단일 리더 문제) 만약 리더(Master) 노드가 다운되면 어떤 문제가 발생하나요?

### SD-080

리더가 다운되었을 때 새로운 리더를 선출하는 과정(Failover)에 대해 설명해 주세요.

### SD-081

이 Failover 과정에서 'Split-Brain' 문제가 발생할 수 있습니다. 이것이 무엇이며, 어떻게 방지할 수 있나요? (예: Fencing, Quorum)

### SD-082

비동기식 복제를 사용할 때, 리더 장애 조치(Failover) 과정에서 데이터 유실이 발생할 수 있습니다. 왜 그런지 설명해 주세요.

### SD-083

(단일 리더 문제) 리더로 모든 쓰기 요청이 몰릴 때 발생하는 쓰기 병목 현상을 어떻게 해결할 수 있을까요?

### SD-084

다중 리더(Multi-Leader) 복제 아키텍처 (Master-Master)에 대해 설명해 주세요.

### SD-085

다중 리더 아키텍처는 어떤 경우에 유용한가요? (예: Multi-Datacenter, 오프라인 작업)

### SD-086

(다중 리더 문제) 다중 리더 아키텍처의 가장 큰 단점, 즉 '쓰기 충돌(Write Conflict)' 문제에 대해 설명해 주세요.

### SD-087

두 개의 리더에서 동일한 데이터를 동시에 수정할 경우 발생하는 이 충돌을 어떻게 감지하고 해결해야 할까요?

### SD-088

'Last Write Wins (LWW)' 와 같은 자동 충돌 해결 전략의 문제점은 무엇인가요?

### SD-089

리더가 없는(Leaderless) 아키텍처 (예: Cassandra, DynamoDB)는 다중 리더와 어떻게 다른가요?

### SD-090

리더리스 아키텍처에서는 'Quorum' 을 사용하여 일관성을 조절합니다. R, W, N 값의 관계(R+W > N)에 대해 설명해 주세요.

### SD-091

리더리스 아키텍처는 '쓰기 충돌'을 어떻게 처리하나요? (예: Read-repair, Vector Clock)

---

## 📌 추가 고급 질문 (Advanced Topics)

### SD-092

RDBMS와 NoSQL(Key-Value, Document, Graph) 각각의 특징을 설명하고, 언제 어떤 것을 선택해야 할지 기준을 설명해 주세요.

### SD-093

마이크로서비스 아키텍처(MSA)를 설계할 때, 서비스 간 통신 방법(동기식 API vs 비동기식 이벤트)을 어떻게 결정해야 할까요?

### SD-094

API 게이트웨이는 MSA에서 어떤 역할을 하며, 왜 필요한가요?

### SD-095

 서비스 메시(Service Mesh) 는 무엇이며, API 게이트웨이나 기존 라이브러리 방식(예: Spring Cloud)과 어떻게 다른가요?

총 질문 95개
