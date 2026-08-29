# Akka 클러스터

## Akka 클러스터 핵심

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster.html

---

### 목차

1. [클러스터 사용하기 (Cluster Usage)](#1-클러스터-사용하기-cluster-usage)
2. [클러스터 명세 (Cluster Specification / Concepts)](#2-클러스터-명세-cluster-specification--concepts)
3. [클러스터 멤버십 라이프사이클 (Cluster Membership Lifecycle)](#3-클러스터-멤버십-라이프사이클-cluster-membership-lifecycle)
4. [장애 감지기 (Failure Detector)](#4-장애-감지기-failure-detector)
5. [클러스터를 언제, 어디에 사용할 것인가 (Choosing Akka Cluster)](#5-클러스터를-언제-어디에-사용할-것인가-choosing-akka-cluster)
6. [스플릿 브레인 리졸버 (Split Brain Resolver)](#6-스플릿-브레인-리졸버-split-brain-resolver)
7. [참고 자료](#참고-자료)

---

### 1. 클러스터 사용하기 (Cluster Usage)

Akka 클러스터(cluster)는 단일 장애 지점(single point of failure)이 없는, 결함 허용(fault-tolerant)을 갖춘 탈중앙(decentralized) P2P 기반의 클러스터 멤버십 서비스(membership service)를 제공함. 클러스터는 가십 프로토콜(gossip protocol)과 자동 장애 감지(automatic failure detection)를 사용하여 여러 노드(node)와 여러 액터 시스템(ActorSystem)에 걸친 분산 애플리케이션을 구성할 수 있게 해줌.

#### 1.1 모듈 정보 (Module Info)

클러스터 기능을 사용하려면 프로젝트 의존성에 다음 아티팩트를 추가해야 함.

```
com.typesafe.akka:akka-cluster-typed
```

(예: 버전 2.10.19 기준)

- 지원 JDK: Eclipse Temurin 11, 17, 21
- 지원 Scala: 2.13.17 또는 3.3.7
- JPMS 모듈명: `akka.cluster.typed`
- 라이선스: BUSL-1.1
- 지원 상태: Lightbend에서 공식 지원

클러스터를 사용하려면 노드 간 통신을 위한 직렬화(serialization)를 반드시 활성화해야 함. 기본 직렬화기로는 Jackson 직렬화(Jackson serialization)를 권장함. 또한 리모팅(remoting)은 Artery 프로토콜을 통해 구성됨.

---

#### 1.2 클러스터 API 확장 (Cluster API Extension)

`Cluster` 확장(extension)은 다음 세 가지 주요 인터페이스를 제공함.

- **매니저 참조 (Manager Reference)**: `Join`, `Leave`, `Down`과 같은 클러스터 명령(command)을 처리함.
- **구독 참조 (Subscriptions Reference)**: `Subscribe`, `Unsubscribe`, `GetCurrentState` 메시지를 통해 클러스터 상태(state) 변경을 수신(listen)할 수 있게 함.
- **상태 프로퍼티 (State Property)**: 현재의 `CurrentClusterState` 정보에 접근함.

---

#### 1.3 클러스터 멤버십 API (Cluster Membership API)

##### 1.3.1 클러스터 합류 (Joining)

노드가 클러스터에 합류하는 방식에는 세 가지가 있음.

1. **자동 디스커버리 (Automatic Discovery)**
   - Akka Management의 클러스터 부트스트랩(Cluster Bootstrap)을 사용함.

2. **설정 기반 (Configuration-Based)**
   - `application.conf`에 시드 노드(seed-nodes)를 정의함.
   - 시드 노드는 합류하려는 노드의 초기 접속 지점(initial contact point) 역할을 함.
   - 목록의 첫 번째 노드(first node)는 클러스터를 부트스트랩하기 위해 가장 먼저 시작되어야 함.
   - 합류 시도가 실패하면 자동으로 재시도(retry)함.

3. **프로그래밍 방식 (Programmatic)**
   - 외부 도구를 통해 노드를 동적으로(dynamically) 발견하여 합류함.

##### 1.3.2 클러스터 이탈 (Leaving)

노드를 클러스터에서 제거할 때 권장되는 방식은 코디네이티드 셧다운(Coordinated Shutdown)을 통한 우아한 종료(graceful exit)임. 코디네이티드 셧다운은 다음과 같은 경우에 트리거됨.

- 액터 시스템(ActorSystem)이 종료될 때
- SIGTERM 시그널을 수신할 때
- 수동으로 HTTP/JMX 명령을 실행할 때

##### 1.3.3 다운 처리 (Downing)

장애 감지(failure detection) 이후, 도달 불가능(unreachable)한 멤버를 제거함. Akka는 다음 설정을 통해 스플릿 브레인 리졸버(Split Brain Resolver)를 활성화할 것을 권장함.

```
akka.cluster.downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
```

---

#### 1.4 노드 역할 (Node Roles)

노드에는 역할(role)을 부여할 수 있음(예: "backend", "frontend"). 역할은 설정 프로퍼티 `akka.cluster.roles`에 정의함. 역할을 활용하면 노드 종류에 따라 특정 액터를 조건부로 시작할 수 있으며, 이 역할 정보는 멤버십 이벤트(membership event)를 통해 확인할 수 있음.

---

#### 1.5 장애 감지 (Failure Detection)

Akka는 기본적으로 PhiAccrual 장애 감지기(PhiAccrual Failure Detector)를 사용하며, 하트비트(heartbeat)를 통해 노드를 모니터링함. 주요 설정 파라미터는 다음과 같음.

- `akka.cluster.failure-detector.threshold` — 장애로 판단하는 phi 값(threshold)
- `akka.cluster.failure-detector.acceptable-heartbeat-pause` — 허용되는 비정상(abnormal) 상황의 여유 마진

자세한 내용은 [4. 장애 감지기](#4-장애-감지기-failure-detector)를 참고하십시오.

---

#### 1.6 설정 핵심 (Configuration Highlights)

##### 1.6.1 최소 필수 설정 (Minimum Configuration Required)

- 액터 프로바이더를 클러스터로 설정: `akka.actor.provider = "cluster"`
- 호스트네임/포트를 포함한 리모팅(remoting) 구성
- 시드 노드(seed-nodes) 정의

##### 1.6.2 시작 제어 (Startup Control)

```
akka.cluster.min-nr-of-members = 3
```

이 설정은 클러스터가 지정한 크기에 도달할 때까지 "Joining" 상태의 멤버를 "Up" 상태로 승격(promote)하는 것을 지연시킴.

##### 1.6.3 로깅 옵션 (Logging Options)

- `akka.cluster.log-info = off` — 클러스터 이벤트 로깅을 끔.
- `akka.cluster.log-info-verbose = on` — 문제 해결(troubleshooting)을 위한 상세(verbose) 로그를 활성화함.

##### 1.6.4 디스패처 설정 (Dispatcher Configuration)

- 클러스터 액터(cluster actor)는 기본적으로 내부 디스패처(internal dispatcher)에서 실행됨.
- `akka.cluster.use-dispatcher` — 클러스터 전용 커스텀 디스패처를 지정함.
- 이를 통해 사용자 액터(user actor)와의 간섭(interference)을 방지함.

##### 1.6.5 설정 호환성 검사 (Configuration Compatibility Check)

노드가 합류할 때 모든 클러스터 노드가 설정(compatible settings)이 호환되는지 검증함. 롤링 업데이트(rolling update)를 위해 다음 설정으로 비활성화할 수 있음.

```
akka.cluster.configuration-compatibility-check.enforce-on-join = off
```

다만 이를 비활성화하면 데이터 손상(data corruption)의 위험이 있으므로 주의해야 함.

---

#### 1.7 상위 수준 클러스터 도구 (Higher-Level Cluster Tools)

클러스터 위에서 동작하는 상위 수준 도구들은 다음과 같음.

- **클러스터 싱글톤 (Cluster Singleton)**: 클러스터 전체에서 액터 인스턴스가 단 하나만 존재하도록 보장함.
- **클러스터 샤딩 (Cluster Sharding)**: 논리적 식별자(logical identifier)를 사용하여 액터를 노드들에 분산 배치함.
- **분산 데이터 (Distributed Data)**: 노드 간 데이터를 공유하기 위한 키-값 저장소(key-value store)를 제공함.
- **분산 발행-구독 (Distributed Publish-Subscribe)**: 물리적 위치를 인지하지 않고 토픽(topic) 기반 메시징을 지원함.
- **클러스터 인지 라우터 (Cluster-Aware Routers)**: 라운드 로빈(round-robin), 일관된 해싱(consistent hashing) 등의 전략으로 메시지를 라우팅함.
- **신뢰성 있는 전달 (Reliable Delivery)**: 흐름 제어(flow control)와 함께 메시지 전달을 보장함.

---

#### 1.8 테스트 지원 (Testing Support)

- 단위 테스트(unit testing) 지원
- 클러스터 시나리오를 위한 멀티 노드 테스트(Multi Node Testing)
- 분산 환경을 위한 멀티 JVM 테스트(Multi JVM Testing)

---

### 2. 클러스터 명세 (Cluster Specification / Concepts)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster-concepts.html

#### 2.1 개요 (Overview)

Akka 클러스터는 "단일 장애 지점이 없는, 결함 허용 탈중앙 P2P 기반 클러스터 멤버십 서비스(fault-tolerant decentralized peer-to-peer based Cluster Membership Service with no single point of failure)"임. 가십 프로토콜(gossip protocol)과 자동 장애 감지(automatic failure detection)를 사용하여 여러 노드 및 여러 액터 시스템에 걸친 분산 애플리케이션을 가능하게 함.

이 멤버십 시스템은 Amazon의 Dynamo와 Basho의 Riak에서 유래했으며, 클러스터 상태 정보를 전파하는 데 가십 프로토콜을 사용함.

---

#### 2.2 핵심 용어 (Key Terminology)

- **노드 (Node)**: `hostname:port:uid` 튜플로 식별되는 논리적 클러스터 멤버임. UID 덕분에 한 물리 장비에서 여러 노드를 운영할 수 있음.
- **클러스터 (Cluster)**: 클러스터 멤버십 서비스를 통해 서로 협력(coordinate)하는, 상호 연결된 노드들의 집합임.
- **리더 (Leader)**: 클러스터의 수렴(convergence)과 멤버십 전이(membership transition)를 관리하는, 지정된 단일 노드임. 리더십은 수렴 시점에 정렬 순서(sorted order)상 가장 먼저 자격을 갖춘(first eligible) 노드에게 결정론적으로(deterministically) 할당됨.

---

#### 2.3 가십 프로토콜 아키텍처 (Gossip Protocol Architecture)

멤버십 시스템은 가십 프로토콜을 사용하여 클러스터 상태 정보를 전파함. "클러스터에 대한 정보는 특정 시점에 노드에서 로컬로 수렴(converge locally)"하며, 이는 모든 멤버가 현재 상태 버전(state version)을 관측했을 때 발생함.

##### 2.3.1 벡터 클록 (Vector Clocks)

벡터 클록(vector clock)은 `(node, counter)` 쌍을 추적하여, 분산 시스템에서 이벤트 간 부분 순서(partial event ordering)를 확립하고 인과성 위반(causality violation)을 감지할 수 있게 함. 가십 교환(gossip exchange) 시 클러스터 상태의 차이를 조정(reconcile)하는 데 사용됨.

##### 2.3.2 수렴 메커니즘 (Convergence Mechanism)

수렴(convergence)을 위해서는 모든 노드가 현재 상태 버전을 보아야 함. 노드가 도달 불가능(unreachable) 상태로 남아 있는 동안에는 수렴이 완료될 수 없음. 도달 불가능한 노드는 반드시 다시 도달 가능(reachable)해지거나, 다운(down)되거나, 제거(removed)되어야 함.

이 제약은 오직 리더 액션(leader action)에만 영향을 미치며, 애플리케이션의 동작 자체에는 영향을 주지 않음.

##### 2.3.3 장애 감지 (Failure Detection)

Phi Accrual 장애 감지기(Phi Accrual Failure Detector)가 노드의 도달 가능성(reachability)을 모니터링함. 각 노드는 해시된 링 순서(hashed ring ordering)를 통해 선택된 5개의 다른 노드를 모니터링하며, 랙(rack)이나 데이터센터(datacenter)를 가로지르는 모니터링을 유도함.

장애가 감지되면, "단 하나의 노드만 어떤 노드를 도달 불가능으로 표시(mark unreachable)해도, 가십 전파를 통해 나머지 클러스터 전체가 그 노드를 도달 불가능으로 표시"하게 됨.

격리된(quarantined) 노드는 전달할 수 없는(undeliverable) 시스템 메시지를 가지게 되며, 재시작(restart)하기 전까지는 복구될 수 없음.

##### 2.3.4 시드 노드 (Seed Nodes)

시드 노드(seed node)는 합류하려는 노드의 접속 지점(contact point) 역할을 하지만, 이미 동작 중인 클러스터의 운영에는 영향을 주지 않음. 새 멤버는 기존 클러스터 멤버 중 어느 것에든 연결할 수 있음.

##### 2.3.5 가십 최적화 (Gossip Optimization)

시스템은 다이제스트(digest)를 사용하는 푸시-풀 가십(push-pull gossip)을 채택하며, 실제 값(actual value)은 필요한 경우에만 전송함.

- 표준 간격은 1초이며, 초기 전파 단계에서는 초당 3회로 가속됨.
- 알고리즘은 현재 상태 버전을 가지지 못한 노드 쪽으로 선택을 편향(bias)시키며, 400개 노드를 초과하는 클러스터에서는 이 편향을 점진적으로 줄임.
- Protobuf 직렬화와 gzip 압축을 사용하여 페이로드(payload) 크기를 줄임.

---

### 3. 클러스터 멤버십 라이프사이클 (Cluster Membership Lifecycle)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster-membership.html

#### 3.1 멤버십 소개 (Introduction to Cluster Membership)

Akka 클러스터의 핵심은 클러스터 멤버십(cluster membership)으로, 어떤 노드가 클러스터의 일부인지 그리고 그들의 상태(health)가 어떤지를 추적함.

노드는 `hostname:port:uid` 튜플로 식별되며, 여기서 UID는 각 액터 시스템 인스턴스를 고유하게 식별함. 이 메커니즘은 시스템이 제거(removed)된 이후 다시 합류하는 것을 방지함. 다시 합류하려면 반드시 다른 UID를 가진 새 인스턴스를 생성해야 함.

---

#### 3.2 멤버 상태 (Member States)

클러스터는 다음과 같은 멤버 상태(member state)를 정의함.

- **Joining (합류 중)**: 노드가 처음 클러스터에 진입할 때의 일시적(transient) 단계임.
- **WeaklyUp (약하게 가동)**: 네트워크 파티션(network partition) 중에 사용할 수 있는 중간 상태임(`akka.cluster.allow-weakly-up-members`가 활성화된 경우). 이 상태는 완전한 수렴(full convergence)이 이루어지지 않은 상황에서도 합류 중인 노드가 진행할 수 있게 함.
- **Up (가동)**: 완전히 통합된(fully integrated) 클러스터 멤버의 표준 운영 상태임.
- **Preparing for Shutdown / Ready for Shutdown (셧다운 준비 / 셧다운 준비 완료)**: 클러스터 전체의 협조된 종료(cluster-wide termination) 직전에 거치는 선택적(optional) 상태로, 불필요한 리밸런싱(rebalancing) 작업을 방지함.
- **Leaving / Exiting (이탈 중 / 종료 중)**: 노드를 제어된 방식으로 제거할 때 거치는 우아한 이탈(graceful exit) 상태임.
- **Down (다운)**: 영구적으로 사용 불가능(permanently unavailable)으로 표시된 노드로, 클러스터 결정(decision)에서 제외됨.
- **Removed (제거됨)**: 멤버십에서 완전히 제거되었음을 나타내는 최종 툼스톤(tombstone) 상태임.

---

#### 3.3 멤버 이벤트 (Member Events)

시스템은 다음과 같은 라이프사이클 이벤트(lifecycle event)를 발행(publish)함.

- `MemberJoined`: 클러스터에 처음 진입했음을 알림.
- `MemberUp`: 완전히 통합되었음을 나타냄.
- `MemberExited`: 이탈(leaving) 프로세스가 진행 중임을 표시함.
- `MemberRemoved`: 완전한 이탈(departure)을 확정함.
- `UnreachableMember`: 장애 감지(detection failure)를 알림.
- `ReachableMember`: 연결이 복구(restore)되었음을 알림.
- `MemberPreparingForShutdown`, `MemberReadyForShutdown`: 셧다운 준비 상태를 알림.

---

#### 3.4 멤버십 라이프사이클 (Membership Lifecycle)

- **합류 프로세스 (Joining Process)**: 노드는 `join` 액션으로 시작하여 joining 상태에 진입함. "모든 노드가 (가십 수렴을 통해) 새 노드가 합류 중임을 본(have seen) 후"에, 클러스터 리더(leader)가 해당 노드를 up 상태로 전이시킴.

- **우아한 이탈 (Graceful Departure)**: `leave` 액션(보통 코디네이티드 셧다운을 통함)은 노드를 leaving 상태로 옮깁니다. 리더 수렴(leader convergence)이 확인되면 exiting을 거쳐 removed로 진행됨.

- **도달 불가능 처리 (Unreachability Handling)**: 노드가 도달 불가능(unreachable)해지면, "가십 수렴이 불가능해지고 따라서 대부분의 리더 액션도 불가능해집니다." 해당 노드는 연결을 복구하거나 명시적으로 다운(down)되어야 함.

- **시스템 재시작 시나리오 (System Restart Scenarios)**: 크래시(crash)된 노드가 동일한 주소로 재시작하면, 클러스터는 "이전 인스턴스를 자동으로 다운으로 표시(automatically marks as down)하고, 새 인스턴스는 수동 개입 없이(without manual intervention) 클러스터에 다시 합류"할 수 있음.

---

#### 3.5 리더의 역할과 책임 (Leader Role and Responsibilities)

리더(leader)는 중대한 상태 확정(state confirmation)을 수행함. 리더는 "가십 수렴 이후 각 노드가 명확하게(unambiguously) 결정할 수 있지만(can be determined)", 일관된 클러스터 전체의 합의(consistent cluster-wide agreement)를 보장하기 위해 대부분의 액션에는 수렴이 필요함.

파티션(partition) 발생 시에는 서로 분리된 네트워크 구간에 각각 별도의 리더가 존재할 수 있음. 시스템은 스플릿 브레인 리졸버(Split Brain Resolver)를 통해 "각 파티션이 어떤 노드가 도달 가능한지에 대해 서로 다른 시각(own view)을 가질 수 있는" 상황을 처리함.

---

#### 3.6 WeaklyUp 멤버 설명 (WeaklyUp Members Explanation)

이 기능이 활성화되면, "수렴이 아직 이루어지지 않은 상태에서도 합류 중인 노드가 WeaklyUp 상태로 승격(promoted)"될 수 있음.

WeaklyUp 상태의 노드는 오직 한쪽 파티션에만 존재함. 네트워크 분리(network split)의 반대편에 있는 멤버들은 이 노드의 존재를 전혀 알지 못하므로, 정족수 결정(quorum decision)에 사용하기에는 적합하지 않음.

---

#### 3.7 전체 클러스터 셧다운 (Full Cluster Shutdown)

`prepareForFullClusterShutdown()` 메서드는 임박한 클러스터 종료(imminent cluster termination)를 알리며, "클러스터 샤딩 리밸런스(Cluster sharding rebalances)"나 싱글톤 마이그레이션(singleton migration)과 같은 비용이 큰 작업을 방지함.

모든 up 멤버는 완전한 종료에 앞서 ReadyForShutdown 상태에 도달해야 함.

---

#### 3.8 상태 전이 (State Transitions)

**사용자 액션 (User Actions):**

- **Join**: 명시적 또는 자동(automatic)으로 클러스터에 진입함.
- **Leave**: 우아한 이탈을 시작함.
- **Down**: 실패한 노드를 수동 또는 자동으로 제거함.

**리더 액션 (Leader Actions):**

- joining → up (수렴 필요)
- joining → weakly up (수렴 없이 동작)
- weakly up → up (수렴이 복구된 후)
- leaving → exiting → removed
- down → removed

---

#### 3.9 장애 감지와 도달 가능성 (Failure Detection)

도달 불가능(unreachability)은 상태(state)가 아니라 플래그(flag)로 동작함. "노드는 그것을 모니터링하는 모든 노드가 다시 도달 가능하다고 볼 때에만(only after all monitoring nodes see it as reachable again) 다시 도달 가능한 것으로 간주"되며, 이를 통해 연결성에 대한 분산 합의(distributed consensus)를 보장함.

---

### 4. 장애 감지기 (Failure Detector)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/failure-detector.html

#### 4.1 개요 (Overview)

Phi Accrual 장애 감지기(Phi Accrual Failure Detector)는 Akka의 Remote DeathWatch가 하트비트 모니터링(heartbeat monitoring)을 통해 네트워크 장애와 JVM 크래시를 식별하는 데 사용하는 메커니즘임. 이는 Hayashibara 등이 발표한 논문 "The Phi Accrual Failure Detector"에 설명된 알고리즘을 구현한 것임.

---

#### 4.2 핵심 개념 (Core Concept)

이 감지기는 단순한 "up" 또는 "down"의 이진(binary) 답변을 제공하는 대신, 노드가 장애를 일으켰을 통계적 가능성(statistical likelihood)을 나타내는 **phi 값**(phi value)을 반환함. 이는 모니터링(monitoring)과 해석(interpretation)을 분리(decouple)하여, 다양한 네트워크 조건에 적응할 수 있게 함.

감지기는 수신한 하트비트로부터 과거 통계(historical statistics)를 유지하고, 단순한 예/아니오(yes/no) 판단에 의존하는 대신 이러한 지표를 사용하여 노드 상태를 통계적으로 평가함.

---

#### 4.3 Phi 계산 (Phi Calculation)

phi 값은 다음과 같이 계산됨.

```
phi = -log10(1 - F(timeSinceLastHeartbeat))
```

여기서 `F`는 정규 분포(normal distribution)의 누적 분포 함수(cumulative distribution function, CDF)이며, 과거 하트비트 도착 간격(inter-arrival time)의 평균(mean)과 표준편차(standard deviation)를 사용하여 계산됨.

---

#### 4.4 하트비트 동작 (Heartbeat Behavior)

- **기본 주기 (Default frequency)**: 초당 1회 하트비트 (설정 가능)
- **메커니즘 (Mechanism)**: 요청/응답(request/reply) 핸드셰이크 패턴
- **입력 (Input)**: 응답(reply)의 도착이 장애 감지기 알고리즘의 입력으로 들어감.

---

#### 4.5 Phi 반응성과 표준편차 (Phi Responsiveness and Standard Deviation)

곡선(curve)의 가파른 정도(steepness)가 장애 감지 속도를 결정함.

- **낮은 표준편차 (100ms)**: 가파른 곡선 → 더 빠른 장애 감지
- **높은 표준편차 (200ms)**: 완만한 기울기 → 더 느린 감지
- 네트워크 변동성(network variability)은 감지 속도에 직접적인 영향을 줌.

---

#### 4.6 설정 파라미터 (Configuration Parameters)

##### 4.6.1 `acceptable-heartbeat-pause`

가비지 컬렉션(garbage collection) 일시 정지나 일시적 네트워크 문제 같은 임시 장애에 대한 안전 마진(safety margin)을 제공함. 불안정한 환경(예: Amazon EC2와 같은 클라우드 플랫폼)에서는 기본값을 더 높게 조정할 수 있음.

```
akka.cluster.failure-detector.acceptable-heartbeat-pause = 7s
```

##### 4.6.2 `threshold`

민감도(sensitivity)를 제어하며 기본값은 8임. 값이 높을수록 거짓 양성(false positive)은 줄지만 크래시 감지가 지연됨. 클라우드 환경에서는 12 정도의 값이 필요할 수 있음.

---

#### 4.7 로깅 지표 (Logging Indicators)

장애 감지기는 다음과 같은 중요한 로그 메시지를 생성함.

- `"Marking node(s) as UNREACHABLE"` — 노드가 장애로 의심됨
- `"Marking node(s) as REACHABLE"` — 노드가 복구됨
- `"heartbeat interval is growing too large"` — 허용 일시정지(acceptable pause)의 2/3에서 출력되는 경고
- `"Scheduled sending of heartbeat was delayed"` — 조사가 필요함

UNREACHABLE/REACHABLE 사이클이 빈번하게 반복된다면, `acceptable-heartbeat-pause`를 늘리거나, 과도한 가비지 컬렉션 같은 근본적인 시스템 문제를 조사해야 함을 시사함.

---

### 5. 클러스터를 언제, 어디에 사용할 것인가 (Choosing Akka Cluster)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/choosing-cluster.html

#### 5.1 개요 (Overview)

핵심 질문은 마이크로서비스(microservices)와 전통적 분산 애플리케이션(distributed application) 접근 방식 중 무엇을 선택할 것인가이며, 이 선택은 Akka 클러스터를 어떻게 구현할지에 큰 영향을 줌.

"Stateful or Stateless applications: to Akka Cluster or not" 영상은 아키텍처에서 Akka 클러스터를 사용하는 동기를 탐구함.

---

#### 5.2 마이크로서비스 아키텍처 (Microservices Architecture)

**핵심 원칙**: 마이크로서비스에서는 서비스 간 독립성(independence)을 유지함.

##### 5.2.1 서비스 간 통신 (Inter-Service Communication)

공식 문서는 명시적으로 다음과 같이 권고함. "서로 다른 서비스 사이에 Akka 클러스터와 액터 메시징(actor messaging)을 사용하는 것을 권장하지 않음." 이는 과도한 코드 결합(code coupling)을 초래하고 독립적 배포(independent deployment)를 복잡하게 만들어, 마이크로서비스의 핵심 이점을 훼손하기 때문임.

서비스 간 통신에는 다음을 사용하십시오.

- **동기 (Synchronous)**: Akka HTTP 또는 Akka gRPC
- **비동기 (Asynchronous)**: Akka Streams Kafka 또는 Alpakka 커넥터

이러한 메커니즘은 종단 간 백프레셔(end-to-end backpressure)를 지원하며, 양쪽 모두 Akka를 사용하거나 같은 프로그래밍 언어를 공유할 필요가 없음.

##### 5.2.2 서비스 내부 통신 (Intra-Service Communication)

하나의 서비스를 구성하는 클러스터 노드들 내부에서는 Akka 클러스터가 적합함. 그 이유는 다음과 같음.

- 노드들이 동일한 코드를 공유함
- 하나의 단위(unit)로 함께 배포됨
- 단일 팀이 배포를 통제함
- 롤링 배포(rolling deployment) 시 두 버전이 일시적으로 함께 동작할 수 있음

이러한 맥락에서는 액터 메시징의 편의성과 성능 이점을 유지하면서도 더 긴밀한 결합(tighter coupling)이 허용됨.

---

#### 5.3 전통적 분산 애플리케이션 (Traditional Distributed Application)

이는 여전히 유효한 대안이며, 특히 다음 경우에 적합함.

- 시장 출시 시간(time-to-market)을 우선하는 스타트업
- 단일 팀(single-team) 조직
- 마이크로서비스의 복잡성이 불필요한 오버헤드를 더하는 시나리오

특징:

- 하나의 코드베이스(codebase)에서 나온 단일 배포 단위(single deployment unit)
- 하나의 클러스터 내 여러 노드에 배포됨
- 중앙집중적 통제(centralized control) 덕분에 더 긴밀한 결합이 허용됨
- 특화된 런타임 역할(front-end / back-end 노드)을 가질 수 있음

---

#### 5.4 안티 패턴: 분산 모놀리스 (Anti-Pattern: Distributed Monolith)

이 문제적 구성은 다음과 같은 특징을 가짐.

- 독립적으로 빌드/배포되는 여러 서비스
- 공유 클러스터(shared cluster), 공유 코드 의존성, 공유 데이터베이스 스키마(schema)
- 자율성으로 위장된 긴밀한 결합(tight coupling)

이 방식은 마이크로서비스의 이점은 희생하면서도 마이크로서비스의 비용은 그대로 떠안으며, 종종 중앙집중적 배포 조율(centralized deployment coordination)을 요구하고 의존성 지옥(dependency nightmare)을 만들어냄.

---

#### 5.5 상위 수준 클러스터 도구 (Higher-Level Cluster Tools)

Akka 클러스터는 다음과 같은 특화 패턴을 지원함.

- **클러스터 샤딩 (Cluster Sharding)**: 액터를 클러스터 노드들에 분산함.
- **클러스터 싱글톤 (Cluster Singleton)**: 클러스터 전체에서 단일 인스턴스를 보장함.
- **분산 데이터 (Distributed Data)**: 최종 일관성(eventually-consistent) 기반의 데이터 공유를 제공함.
- **분산 발행-구독 (Distributed Publish-Subscribe)**: 클러스터 내 메시징 패턴을 지원함.
- **신뢰성 있는 전달 (Reliable Delivery)**: 메시지 전달 시맨틱(semantics)을 보장함.
- **샤딩된 데몬 프로세스 (Sharded Daemon Process)**: 백그라운드 프로세스를 관리함.

---

#### 5.6 지원 인프라 (Supporting Infrastructure)

Akka 클러스터 배포는 다음 인프라의 도움을 받음.

- 직렬화 프레임워크 (Jackson 통합 제공)
- 리모팅(remoting) 및 원격 보안(remote security) 설정
- 네트워크 파티션을 위한 스플릿 브레인 리졸버(Split Brain Resolver)
- 멀티 노드 테스트(multi-node testing) 기능
- 협조(coordination) 메커니즘

---

### 6. 스플릿 브레인 리졸버 (Split Brain Resolver)

> 원본: https://doc.akka.io/libraries/akka-core/current/split-brain-resolver.html

#### 6.1 스플릿 브레인 문제란 무엇인가 (What is the Split Brain Problem?)

스플릿 브레인 리졸버(Split Brain Resolver, SBR)는 분산 시스템의 근본적인 난제를 해결함. 즉, 네트워크 파티션(network partition)과 장비 크래시(machine crash)는 관측자(observer) 입장에서 구분이 불가능하다는 점임. 문서의 설명대로, "노드는 다른 노드에 문제가 있다는 것은 관측할 수 있지만, 그 노드가 크래시되어 영영 사용 불가능해질 것인지, 아니면 단지 네트워크 문제인지는 구분할 수 없음."

네트워크 파티션이 적절히 처리되지 않으면, 양쪽 구간(both sides)이 서로를 클러스터 멤버십에서 제거하여 두 개의 분리된 클러스터(separate disconnected clusters)가 만들어질 수 있음. 이는 클러스터 싱글톤(Cluster Singleton)이나 영속성(persistence)을 사용하는 클러스터 샤딩(Cluster Sharding)을 쓰는 시스템에서 치명적이며, 동일한 영속 액터(persistent actor)의 여러 인스턴스가 동시에 쓰기(write)를 수행하는 사태를 일으킬 수 있음.

---

#### 6.2 SBR 활성화 (Enabling SBR)

스플릿 브레인 리졸버는 설정을 통해 활성화함.

```
akka.cluster.downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
```

추가로 사용할 전략(strategy)을 선택함.

```
akka.cluster.split-brain-resolver.active-strategy = keep-majority
```

---

#### 6.3 핵심 타이밍: stable-after 설정 (Critical Timing: Stable-After Setting)

리졸버는 행동에 나서기 전에 클러스터가 안정(stable)될 때까지 기다림. "stable-after" 지속 시간은 클러스터 크기를 고려해야 함.

- 5 노드: 7초
- 10 노드: 10초
- 100 노드: 20초
- 1000 노드: 30초

이는 일시적 네트워크 문제에 성급하게(hasty) 결정하는 것을 방지하는 한편, 진짜 장애에 대해서는 적시(timely)에 대응할 수 있도록 함.

---

#### 6.4 다운 전략 (Downing Strategies)

##### 6.4.1 Keep Majority (다수 유지, 기본값)

이 전략은 "현재 노드가 다수(majority) 측에 속해 있다면, 도달 불가능한 노드들을 다운"시킴. 파티션 크기가 동일한 경우, 가장 낮은 주소(lowest-addressed)를 가진 노드가 포함된 쪽이 생존함. 동적으로 크기가 변하는(dynamically sized) 클러스터에 가장 적합함.

##### 6.4.2 Static Quorum (정적 정족수)

최소 노드 임계값(threshold)을 요구함. 남은 노드 수가 `quorum-size` 이상이면 도달 불가능한 노드를 다운시키고, 그렇지 않으면 도달 가능한(reachable) 노드들이 스스로 종료함. 고정 크기(fixed-size) 클러스터에 이상적임. 주의: 전체 멤버 수가 **quorum-size × 2 − 1**을 초과하면 안 됨.

##### 6.4.3 Keep Oldest (최고령 유지)

가장 오래된(oldest) 클러스터 멤버가 포함된 쪽을 보존함(보통 이곳에서 클러스터 싱글톤이 동작함). `down-if-alone = on`으로 설정하면, 최고령 노드가 고립(isolated)되었을 경우 스스로 다운됨. 이 전략은 큰 클러스터에서 소수의 노드만 보존할 가능성이 있음.

##### 6.4.4 Down All (전체 다운)

네트워크가 매우 불안정할 때 모든 노드를 종료시킵니다. 장애 감지가 신뢰하기 어려운(unreliable) 환경에 유용함. 복구하려면 재시작 후 클러스터를 다시 구성(reform)해야 함.

##### 6.4.5 Lease-Majority (리스 다수)

외부 분산 락(external distributed lock, 보통 Kubernetes CRD)을 사용하여 어느 파티션이 생존할지 조율(coordinate)함. 단 하나의 SBR 인스턴스만 리스(lease)를 획득하여 계속 동작함. 추가 인프라 의존성(infrastructure dependency)이라는 비용을 치르고 외부 중재(external arbitration)를 제공함.

---

#### 6.5 간접 연결된 노드 (Indirectly Connected Nodes)

네트워크에서는 때때로 도달 불가능한 노드가 중개자(intermediaries)를 통해 간접적으로(indirectly) 연결되어 있는 상황, 즉 깔끔한 파티션(clean partition)이 아닌 상황이 발생함. 리졸버는 "완전히 연결된(fully connected) 노드들은 유지하고, 간접적으로만 연결된(indirectly connected) 노드들을 모두 다운"시킴.

과도한 다운을 방지하기 위해 다음을 설정할 수 있음.

```
akka.cluster.split-brain-resolver.down-all-when-indirectly-connected = 0.5
```

이 파라미터는 간접 연결로 인한 다운 결정이 지정된 비율(fraction)을 초과하게 될 경우 전체를 다운시킴.

---

#### 6.6 불안정할 때 전체 다운 (Down All When Unstable)

도달 가능성 관측(reachability observation)이 지속적으로 변하면, 시스템은 예방 차원에서 모든 노드를 다운시킬 수 있음. 다음으로 설정함.

```
akka.cluster.split-brain-resolver.down-all-when-unstable = on
```

기본적으로 이 지속 시간은 `stable-after`의 3/4(최소 4초)와 같음.

---

#### 6.7 페일오버 기대치 (Failover Expectations)

100개 노드 클러스터에서 기본 설정으로:

- 장애 감지(failure detection): 5초
- stable-after: 20초
- down-removal-margin: 20초
- **전체 예상 페일오버(failover): 약 45초**

더 작은 클러스터(10개 노드)에서는 `stable-after`를 10초로 줄여 약 25초의 페일오버를 달성할 수 있음.

---

#### 6.8 코디네이티드 셧다운 (Coordinated Shutdown)

우아한 종료(graceful termination)를 활성화함.

```
akka.coordinated-shutdown.exit-jvm = on
```

이 설정은 노드가 다운될 때 적절히 종료되도록 보장하여, 중복 싱글톤 인스턴스(duplicate singleton instance)가 생기는 것을 방지함.

---

#### 6.9 모듈 정보 (Module Information)

스플릿 브레인 리졸버는 `akka-cluster` 의존성에 포함되어 있음(예: 버전 2.10.19). JDK 11, 17, 21을 지원하며, Scala 2.13.17 및 3.3.7을 지원함.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [클러스터 사용하기 (Cluster Usage)](https://doc.akka.io/libraries/akka-core/current/typed/cluster.html)
- [클러스터 명세 (Cluster Specification)](https://doc.akka.io/libraries/akka-core/current/typed/cluster-concepts.html)
- [클러스터 멤버십 (Cluster Membership)](https://doc.akka.io/libraries/akka-core/current/typed/cluster-membership.html)
- [장애 감지기 (Failure Detector)](https://doc.akka.io/libraries/akka-core/current/typed/failure-detector.html)
- [클러스터 선택 (Choosing Akka Cluster)](https://doc.akka.io/libraries/akka-core/current/typed/choosing-cluster.html)
- [스플릿 브레인 리졸버 (Split Brain Resolver)](https://doc.akka.io/libraries/akka-core/current/split-brain-resolver.html)

---

## Akka 클러스터 도구

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

### 목차

1. [클러스터 샤딩 (Cluster Sharding)](#클러스터-샤딩-cluster-sharding)
2. [클러스터 샤딩 개념 (Cluster Sharding Concepts)](#클러스터-샤딩-개념-cluster-sharding-concepts)
3. [클러스터 싱글톤 (Cluster Singleton)](#클러스터-싱글톤-cluster-singleton)
4. [분산 데이터 (Distributed Data)](#분산-데이터-distributed-data)
5. [분산 발행-구독 (Distributed Publish Subscribe)](#분산-발행-구독-distributed-publish-subscribe)
6. [샤드 데몬 프로세스 (Sharded Daemon Process)](#샤드-데몬-프로세스-sharded-daemon-process)
7. [신뢰성 있는 전달 (Reliable Delivery)](#신뢰성-있는-전달-reliable-delivery)
8. [참고 자료](#참고-자료)

---

### 클러스터 샤딩 (Cluster Sharding)

#### 개요와 목적

클러스터 샤딩(Cluster Sharding)은 액터(actor)들을 클러스터의 여러 노드(node)에 분산 배치하면서도, 상호작용 시에는 물리적 위치를 신경 쓰지 않고 **논리적 식별자**(logical identifier)만으로 다룰 수 있게 해주는 기능임. 공식 문서에 따르면, 샤딩은 "여러 노드에 액터를 분산시키되, 그 물리적 위치를 신경 쓰지 않고 논리적 식별자로 상호작용하고 싶을 때 유용함."

샤딩은 주로 **상태를 가진 "엔티티(entity)" 액터**에 사용됨. 이러한 엔티티는 도메인 주도 설계(Domain-Driven Design, DDD)에서의 애그리거트 루트(aggregate root)인 경우가 많음. 상태를 가진 액터가 너무 많아 단일 머신의 자원으로는 감당하기 어려울 때 적합함. 반대로 상태를 가진 액터가 소수라면, 더 단순한 클러스터 싱글톤(Cluster Singleton)을 사용하는 편이 나음.

#### 핵심 개념: 엔티티, 샤드, 리전

- **엔티티(entity)**: 식별자(identifier)를 가진 개별 액터로, 클러스터 노드 전체에 분산됨. 실제 비즈니스 로직을 처리하는 주체임.
- **샤드(shard)**: 여러 엔티티를 하나로 묶는 그룹임. 기본적으로 해시(hash) 기반 분산을 사용함.
- **샤드 리전(ShardRegion)**: 메시지를 올바른 엔티티 위치로 라우팅(routing)하는 액터이며, 물리적 배치를 추상화함.

#### EntityTypeKey와 EntityRef

각 엔티티 타입은 자신이 수락하는 메시지 타입을 정의하는 `EntityTypeKey`를 가짐. `EntityRef`는 특정 엔티티에게 위치를 몰라도 메시지를 보낼 수 있는 타입 안전한(type-safe) 참조를 제공함.

#### 메시지 라우팅

엔티티에게 메시지를 보내는 방법은 다음과 같음.

- **EntityRef**: 타입 안전하며 권장되는 방식임.
- **ShardingEnvelope**: 엔티티 ID와 메시지를 함께 감싸서(wrap) 보냄.
- **커스텀 ShardingMessageExtractor**: 메시지에서 엔티티 ID와 샤드 ID를 추출하는 사용자 정의 구현임.

#### 설정과 초기화

클러스터 샤딩은 `ClusterSharding` 익스텐션(extension)을 통해 접근함. 다음과 같이 초기화함.

```
Entity.of(typeKey, ctx -> entity.create(ctx.getEntityId()))
```

`init` 메서드는 각 엔티티 타입에 대해 **모든 노드에서** 호출해야 함. 어떤 노드가 실제 엔티티를 호스팅(hosting)할지, 아니면 프록시(proxy) 역할만 할지는 역할(role) 설정에 따라 달라진다. 비헤이비어 팩토리(behavior factory)는 노드 로컬(node-local)에서 실행되므로, 로컬 `ActorRef`처럼 직렬화(serialize)할 수 없는 의존성도 안전하게 주입할 수 있음.

#### 영속성과 상태 관리

##### 이벤트 소싱과 함께 사용

엔티티는 일반적으로 영속(persistent) 상태를 위해 `EventSourcedBehavior`를 사용함. Akka Persistence의 **단일 작성자 원칙**(single-writer principle)은 동일한 `PersistenceId`를 가진 영속 액터가 오직 하나만 활성화되도록 보장함. 클러스터 샤딩은 동일한 ID를 가진 엔티티가 클러스터 전체에서 한 번만 실행되도록 보장함으로써 이 원칙을 충족시킴.

##### EntityRef 직렬화

`EntityRef`를 (메시지나 이벤트 소싱 상태 안에서) 직렬화하려면 커스텀 직렬화기(serializer)가 필요함. 역직렬화(deserialize) 시점에 `entityId`와 `typeKey`를 조회(lookup)해야 하므로, 이 정보를 직접 인코딩(encode)하는 대신 별도의 처리가 요구됨.

#### 샤드 할당 전략 (Shard Allocation Strategy)

##### 기본 전략: LeastShardAllocationStrategy

기본적으로 샤드는 이전에 할당된 샤드 수가 가장 적은 `ShardRegion`에 할당됨. `number-of-shards`(기본값 1000)는 계획된 최대 클러스터 노드 수의 대략 **10배** 정도로 설정하는 것이 좋음. 이 값은 모든 노드에서 동일해야 하며, 변경하려면 클러스터 전체를 재시작해야 함.

노드가 추가되면 가장 많은 샤드를 보유한 노드에서 새 노드로 샤드가 리밸런싱(rebalancing)됨. `rebalance-absolute-limit`(Akka 2.6.10 도입)은 라운드(round)당 리밸런싱되는 샤드의 최대 개수를 제한하여 더 빠르게 최적의 균형에 도달하게 함.

##### 외부 샤드 할당 (External Shard Allocation)

`ExternalShardAllocationStrategy`는 `ExternalShardAllocation`을 통해 샤드 배치를 명시적으로 제어할 수 있게 함. 예를 들어 Kafka 파티션(partition)을 샤드와 같은 위치(co-locate)에 배치할 때 유용함. 할당되지 않은 샤드는 기본적으로 요청한 노드에 배치되며, 명시적 할당은 `updateShardLocation`을 사용함.

##### 같은 위치에 배치된 샤드 (Colocated Shards)

`ConsistentHashingShardAllocationStrategy`는 일관된 해싱(consistent hashing)을 사용하여, 동일한 식별자를 가진 서로 다른 엔티티 타입의 샤드를 같은 노드에 배치함. 이를 위해서는 관련 엔티티에 대해 동일한 샤드 ID를 반환하는 커스텀 `ShardingMessageExtractor` 구현이 필요함.

##### 커스텀 전략

애플리케이션 고유의 로직을 위해 커스텀 `ShardAllocationStrategy`를 구현할 수 있음.

#### 패시베이션 (Passivation)

##### 수동 패시베이션

엔티티는 `ClusterSharding.Passivate`를 통해 스스로를 종료할 수 있으며, 선택적으로 `stopMessage`를 지정할 수 있음. 패시베이션 중에 도착한 메시지는 버퍼링(buffering)되었다가 엔티티의 다음 화신(incarnation)에게 전달됨. 커스텀 stop 메시지를 사용하면 종료 전에 우아한(graceful) 정리 작업을 수행할 수 있음.

##### 자동 패시베이션

자동 패시베이션 전략은 설정된 정책에 따라 엔티티를 패시베이션함. 하위 호환성(backward compatibility)을 위해 기본적으로 비활성화되어 있음. 자동 패시베이션은 엔티티 기억하기(Remembering Entities)와 함께 사용할 수 없음.

###### 유휴 엔티티 패시베이션 (Idle Entity Passivation)

지정된 시간(기본 2분) 동안 활동이 없는 엔티티는 패시베이션됨. 다음 설정으로 구성함.

```
akka.cluster.sharding.passivation.default-idle-strategy.idle-entity.timeout
```

###### 활성 엔티티 제한 (Active Entity Limits)

제한 기반 전략은 활성 엔티티 수가 임계값을 초과할 때 교체 정책(replacement policy)에 따라 패시베이션함(LRU, MRU, LFU 등). 권장되는 `default-strategy`는 어드미션 윈도우(admission window)와 필터(filter)를 결합한 복합(composite) 패시베이션(Window-TinyLFU 알고리즘)을 사용함.

**교체 정책:**

- **LRU (Least Recently Used, 최소 최근 사용)**: 가장 오랫동안 접근되지 않은 엔티티를 패시베이션함. 최근성(recency)이 강한 패턴에 좋음.
- **Segmented LRU (분할 LRU)**: 공간을 접근 빈도에 따라 세그먼트(segment)로 나누어, 자주 접근되는 엔티티를 보호함.
- **MRU (Most Recently Used, 최대 최근 사용)**: 가장 많이 접근된 엔티티를 패시베이션함. 순환(cyclic) 패턴에 유용함.
- **LFU (Least Frequently Used, 최소 빈도 사용)**: 가장 인기 없는 엔티티를 패시베이션함. 빈도(frequency)가 중요한 워크로드에 좋음.
- **동적 에이징(Dynamic Aging)을 적용한 LFU**: 인기도 변화에 적응함.

**복합 전략**(Composite Strategy)은 어드미션 윈도우(새로운 엔티티를 한 정책으로 추적)와 메인 영역(main area, 자리잡은 엔티티를 다른 정책으로 추적)을 결합하며, 선택적으로 빈도 스케치(frequency-sketch) 어드미션 필터를 둠. 어드미션 윈도우 옵티마이저(optimizer)는 힐 클라이밍(hill-climbing) 기법으로 윈도우 크기를 동적으로 조정함.

커스텀 전략은 설정 섹션을 만들고 `strategy` 설정으로 선택하여 구성함.

#### 샤딩 상태 관리

##### 상태 저장소 (State Store, ShardCoordinator 상태)

필수적으로 샤드 위치를 저장함. ShardCoordinator는 샤드를 재배치(relocate)할 때 이 상태를 로드함.

**분산 데이터 모드 (ddata, 기본값):**

- CRDT를 사용하며 `WriteMajorityPlus`/`ReadMajorityPlus` 일관성(consistency)을 적용함.
- 클러스터 전반에 복제(replicate)되지만 디스크에는 영속화되지 않음.
- 노드마다 레플리케이터(replicator)가 존재하며, 역할 기반 샤딩 사용 시 역할당 하나씩 둠.
- 롤링 업데이트(rolling update) 중에는 역할 설정이 일관되게 유지되어야 함.

**영속성 모드 (persistence, deprecated):**

- 분산 저널(journal)을 사용한 이벤트 소싱(Event Sourcing) 방식임.
- 신규 프로젝트에는 권장하지 않음.

##### 엔티티 기억하기 저장소 (Remember Entities Store)

선택적으로 활성화하며, 메시지를 기다리지 않고도 리밸런싱/크래시 이후 엔티티를 자동으로 재시작할 수 있게 함. 이 기능을 활성화하면 자동 패시베이션은 비활성화됨.

**분산 데이터 모드 (ddata, 기본값):**

- 전체 클러스터 재시작 지원을 위해 LMDB를 통해 디스크에 영속화됨.
- 필요 없다면 디스크 영속화를 끌 수 있음.
- Java 17에서는 LMDB를 위한 특정 JVM 플래그가 필요함.

**이벤트 소싱 모드 (eventsourced):**

- 이벤트 소싱을 사용하며, 영속성/스냅샷(snapshot) 플러그인 설정이 필요함.
- 재시작 사이에 디스크 접근이 불가능한 환경에 적합함.
- 마이그레이션을 위해 deprecated 영속성 모드의 데이터를 읽을 수 있음.

##### Deprecated 영속성 모드로부터의 마이그레이션

상태 저장소만 마이그레이션하는 경우 전체 클러스터 재시작이 필요함. 기억된 엔티티를 다루려면 다음 중 하나를 택함.

- ddata로 마이그레이션(전체 재시작 후 기억 정보는 손실됨)
- ddata 상태 저장소 + eventsourced 엔티티 기억하기로 마이그레이션(기억된 엔티티 보존)

기존 데이터를 읽기 위해 저널에 이벤트 어댑터(event adapter)를 설정함.

##### 클러스터 샤딩 데이터 제거

내구성 저장소 위치(`akka.cluster.sharding.distributed-data.durable.lmdb.dir`)는 기본적으로 포트별(port-specific) 경로로 지정됨. 동적 포트(0)를 사용하면 이전 데이터가 로드되지 않음. 명시적 경로를 설정하거나, 전체 클러스터 종료 후 엔티티를 재시작할 필요가 없다면 내구성을 비활성화함.

#### 시작 동작 (Startup Behavior)

`akka.cluster.min-nr-of-members`(또는 역할별 변형)를 사용하면 최소 개수의 리전이 등록될 때까지 샤드 할당을 지연할 수 있음. 이를 통해 첫 번째 리전에 조기 할당된 뒤 즉시 리밸런싱되는 현상을 방지할 수 있음.

#### 헬스 체크 (Health Checks)

Akka Management과 호환되는 헬스 체크는 로컬 샤드 리전이 코디네이터(coordinator)에 등록되면 정상(healthy)을 반환함. 기본적으로 자동 활성화되며 설정으로 비활성화할 수 있음. 특정 리전을 모니터링하려면 엔티티 타입 이름을 지정함. 안정적으로 운영 중인 상태에서는 헬스 체크가 실패하지 않음(엔티티 타입이 추가될 때 Kubernetes 롤링 업데이트가 멈추는 것을 방지하기 위해서다).

#### 클러스터 샤딩 상태 점검

- **GetShardRegionState**: 한 리전의 샤드 및 엔티티 식별자를 담은 `CurrentShardRegionState`를 반환함.
- **GetClusterShardingStats**: 클러스터 전체의 모든 리전을 조회하여, 리전별 샤드 식별자와 엔티티 개수를 담은 `ClusterShardingStats`를 반환함.

#### 주요 설정

- `akka.cluster.sharding.number-of-shards`: 전체 샤드 수(기본값 1000)
- `akka.cluster.sharding.state-store-mode`: `ddata` 또는 `persistence`
- `akka.cluster.sharding.remember-entities`: 엔티티 기억하기 활성화/비활성화
- `akka.cluster.sharding.remember-entities-store`: `ddata` 또는 `eventsourced`
- `akka.cluster.sharding.passivation.strategy`: 패시베이션 정책(`none`, `default-idle-strategy`, `default-strategy`, 커스텀)
- `akka.management.health-checks.readiness-checks.sharding`: 헬스 체크 활성화 여부

#### 중요한 경고

1. **다운(downing) 전략 주의**: 클러스터를 여러 개로 쪼갤 위험이 있는 다운 전략은 피해야 함. 분리된 서브 클러스터(sub-cluster)마다 동일한 샤드와 엔티티가 시작될 수 있음.
2. **WeaklyUp 멤버**: WeaklyUp 상태의 멤버에서는 클러스터 샤딩이 동작하지 않음.
3. **EntityRef 직렬화**: 커스텀 직렬화기가 필요하며, 내장 지원은 없음.
4. **역할 일관성**: 역할 기반 샤딩 사용 시 롤링 업데이트 동안 역할 설정이 안정적으로 유지되어야 함.
5. **메시지 활동만 집계**: 자동 패시베이션은 클러스터 샤딩 메시지만 집계하며, 직접 보낸 `ActorRef` 메시지는 세지 않음.

---

### 클러스터 샤딩 개념 (Cluster Sharding Concepts)

#### 핵심 구성 요소

##### ShardRegion (샤드 리전)

각 클러스터 노드에서 시작됨. 애플리케이션이 제공하는 함수를 사용하여 메시지에서 엔티티 식별자와 샤드 식별자를 추출함. 로컬 샤드를 관리하며, 필요에 따라 메시지를 원격 리전으로 전달함.

##### ShardCoordinator (샤드 코디네이터)

클러스터에서 가장 오래된 멤버(oldest member)에서 실행되는 싱글톤(singleton) 액터임. 샤드의 소유권(ownership)을 결정하고 적절한 ShardRegion에 알린다. 상태는 분산 데이터(Distributed Data)를 사용해 영속적으로 유지됨.

##### Shard (샤드)

서로 관련된 엔티티들의 그룹으로, 단일 ShardRegion이 묶어서 관리함.

##### Entity (엔티티)

클러스터 샤딩이 관리하는 개별 액터로, 실제 비즈니스 로직을 처리함.

#### 주요 개념 설명

- **메시지 흐름(Message Flow)**: 공식 문서에 따르면, "들어오는 메시지는 ShardRegion과 Shard를 거쳐 대상 Entity로 이동함." 샤드 위치가 아직 알려지지 않은 경우, 위치가 확정될 때까지 메시지는 버퍼링됨.
- **샤드 위치(Shard Location)**: ShardCoordinator는 플러그형(pluggable) 할당 전략을 통해 모든 노드가 일관된 샤드 할당 정보를 유지하도록 보장함.
- **리밸런싱(Rebalancing)**: 새로운 클러스터 멤버가 합류하면, 코디네이터가 샤드 이동을 조율함. 들어오는 메시지를 버퍼링하고, 엔티티를 정지시킨 뒤, 새로운 보금자리(home)로 재할당함. 복구를 위해 상태는 영속적이어야 함.
- **상태 영속성(State Persistence)**: ShardCoordinator 상태는 분산 데이터를 통해 장애를 견딤.
- **메시지 순서(Message Ordering)**: 동일한 ShardRegion 송신자를 사용할 때 순서가 보존되며, 최대 한 번(at-most-once) 전달 시맨틱(semantics)을 가짐.
- **신뢰성 있는 전달(Reliable Delivery)**: 최소 한 번(at-least-once) 시맨틱이 필요하면 Reliable Delivery 기능을 통해 사용할 수 있음.
- **성능(Performance)**: 지연(latency)은 코디네이터 조회가 필요한 미지의 샤드에 대해서만 발생함. 이후 메시지는 직접 라우팅됨.

---

### 클러스터 싱글톤 (Cluster Singleton)

#### 목적과 개요

클러스터 싱글톤(Cluster Singleton)은 클러스터 전체에서 액터 인스턴스가 **정확히 하나만** 실행되도록 보장함. 사용 사례는 다음과 같음.

- 클러스터 전역의 조율 및 의사 결정
- 외부 시스템에 대한 단일 진입점(single entry point)
- 마스터-워커(master-worker) 패턴
- 중앙 집중식 네이밍(naming) 또는 라우팅 서비스

#### 치명적인 경고

**스플릿 브레인(Split Brain) 위험**: 공식 문서는 경고함. "네트워크 문제나 시스템 과부하(긴 GC 정지 등)로 클러스터가 여러 개의 분리된 클러스터로 쪼개질 수 있는 다운(downing) 전략은 사용하지 마라. 그렇게 되면 분리된 클러스터마다 하나씩, **여러 개의 싱글톤**이 시작됨!"

#### 잠재적인 문제점

이 패턴에는 무시할 수 없는 단점이 있음.

- 클러스터 작업에 대한 단일 성능 병목(bottleneck)이 됨.
- 무중단(non-stop) 가용성을 보장할 수 없음. 노드 장애 후 마이그레이션에는 수 초가 걸림.
- 모든 싱글톤이 가장 오래된 노드에 집중되어 자원 경합(resource contention)을 일으킬 수 있음.
- 많은 시나리오에서는 클러스터 샤딩(Cluster Sharding) 같은 더 나은 대안이 바람직함.

#### 아키텍처 구성 요소

- **싱글톤 매니저(Singleton Manager)**: `ClusterSingleton.init()`을 통해 모든 클러스터 노드(또는 역할이 지정된 노드)에서 시작되며, 가장 오래된 멤버에서 실행되는 단 하나의 액터 인스턴스를 관리함. 매니저는 언제나 인스턴스가 최대 하나만 존재하도록 보장함.
- **싱글톤 프록시(Singleton Proxy)**: `init()`이 반환하는 `ActorRef`로, 위치와 무관하게 현재 싱글톤 인스턴스로 메시지를 라우팅함. 싱글톤이 일시적으로 사용 불가능할 때는 메시지를 버퍼링함(버퍼 크기 설정 가능, 기본값 1000개).

#### 구현 단계

**1. 액터 비헤이비어 정의**
표준 `Behavior` 구현을 만듦. 예시에서는 증가(increment) 명령과 값 조회를 받는 카운터(counter)를 보여줌.

**2. 싱글톤 초기화**
각 노드에서 `ClusterSingleton(system).init(SingletonActor(...))`을 호출함. 이는 메시징을 위한 프록시 `ActorRef`를 반환함.

**3. 메시지 전송**
모든 메시지는 반환된 프록시를 통해 보냄. 프록시는 자동으로 활성 싱글톤을 찾아 전달함.

#### 슈퍼비전 설정 (Supervision)

기본 정지 동작을 슈퍼비전 전략(supervision strategy)으로 재정의할 수 있음.

- `SupervisorStrategy.restart`: 실패 시 즉시 재시작
- `SupervisorStrategy.restartWithBackoff(min, max, randomFactor)`: 빠른 반복을 막는 지연 재시작

예시:

```
Behaviors.supervise(Counter()).onFailure[Exception](SupervisorStrategy.restart)
```

#### 애플리케이션 고유의 정지 메시지

`stopMessage`를 지정하면 우아한 종료(graceful shutdown)를 구성할 수 있음.

```
SingletonActor(behavior, "name").withStopMessage(ShutdownCommand)
```

이 메시지는 종료 직전에 전송되며, 핸드오버(handover)가 완료되기 전에 비동기 정리 및 자원 반납 작업을 수행할 수 있게 함.

#### 리스(Lease) 설정

분산 리스(distributed lease)를 사용하여 스플릿 브레인 시나리오에 대한 추가 안전장치를 둘 수 있음.

- **전역 설정**: `akka.cluster.singleton.use-lease`를 구성하면 클러스터 전체에 리스를 적용함.
- **싱글톤별 설정**: 커스텀 리스 설정 블록을 정의하고 `ClusterSingletonSettings.withLeaseSettings(LeaseUsageSettings(...))`로 적용함.

설정된 리스를 획득하지 못하면 싱글톤은 시작되지 않음. 리스를 잃으면 액터가 종료되고, 재시작 전에 리스를 다시 획득함.

#### 설정 파라미터

**매니저 설정 (`akka.cluster.singleton`):**

- `singleton-name`: 액터 이름(기본값 "singleton")
- `role`: 노드 역할 필터(비어 있으면 전체 노드)
- `hand-over-retry-interval`: 핸드오버 시도 주기(기본값 1s)
- `min-number-of-hand-over-retries`: 최소 재시도 횟수(기본값 15)
- `use-lease`: 리스 설정 경로
- `lease-retry-interval`: 리스 획득 재시도 주기(기본값 5s)

**프록시 설정 (`akka.cluster.singleton-proxy`):**

- `singleton-identification-interval`: 싱글톤 위치 확인 주기(기본값 1s)
- `buffer-size`: 싱글톤 사용 불가 시 메시지 버퍼 용량(기본값 1000, 최대 10000, 0이면 비활성화)

#### 데이터 센터 간 싱글톤 접근

이 기능은 계획되어 있으나 아직 미완성임(TODO #27705로 표시됨).

#### 메시지 전달 보장

프록시가 버퍼링을 제공하더라도, 분산 시스템 특성상 메시지가 유실될 수 있음. 신뢰성 있는 전달 시맨틱이 필요한 경우에는 애플리케이션 수준의 확인(acknowledgment) 및 재시도(retry) 로직을 직접 구현해야 함.

---

### 분산 데이터 (Distributed Data)

#### 목적과 핵심 개념

Akka 분산 데이터(Distributed Data)는 키-값(key-value) 저장소 인터페이스를 통해 Akka 클러스터의 노드 간에 데이터를 공유할 수 있게 해준다. **충돌 없는 복제 데이터 타입**(Conflict-Free Replicated Data Types, CRDT)을 구현하므로, 조율(coordination) 없이도 어떤 노드에서든 업데이트가 가능하다. 공식 문서에 따르면, 데이터는 "최종적으로 일관되며(eventually consistent), 낮은 지연(latency)으로 높은 읽기/쓰기 가용성(파티션 내성, partition tolerance)을 제공하도록 설계되었다."

#### 레플리케이터 패턴 (Replicator Pattern)

데이터와의 상호작용은 `DistributedData` 익스텐션을 통해 접근하는 레플리케이터(Replicator) 액터를 거쳐 이루어짐. 이 액터는 세 가지 주요 연산을 관리함.

- **Update**: `Replicator.Update` 메시지를 보내 데이터를 수정함. 키 식별자, 초기 빈 상태, 쓰기 일관성 수준, 응답 주소, 순수(pure) 수정 함수의 다섯 가지 구성 요소를 포함함. 자신의 쓰기는 즉시 반영되도록 보장됨.
- **Get**: `Replicator.Get`으로 지정된 읽기 일관성 수준에 따라 현재 값을 조회함. 자신의 쓰기에 대한 읽기는 항상 최신 값을 반환하지만, 응답 메시지의 순서는 보장되지 않음.
- **Subscribe**: 액터는 특정 키 변경 알림을 구독(subscribe)하여 `Replicator.Changed` 메시지를 받을 수 있음. 와일드카드(wildcard)를 사용한 접두사(prefix) 매칭을 지원함.

#### 일관성 모델 (Consistency Models)

##### 쓰기 일관성 수준

- **WriteLocal**: 로컬 복제본에 즉시 쓰고, 가십(gossip) 프로토콜을 통해 수 초에 걸쳐 전파함.
- **WriteTo(n)**: 로컬 복제본을 포함해 최소 n개의 복제본에 씀.
- **WriteMajority**: N/2 + 1개의 복제본에 씀. 작은 클러스터를 위한 minCap 파라미터를 포함함.
- **WriteMajorityPlus**: 과반수(majority)에 추가로 지정된 노드 수만큼 더 쓰며, 빠져나가는(exiting) 노드는 제외함.
- **WriteAll**: 빠져나가는 노드를 제외한 모든 클러스터 노드에 씀.

##### 읽기 일관성 수준

- **ReadLocal**: 로컬 복제본만 읽는다(GetFailure가 발생하지 않음).
- **ReadFrom(n)**: n개의 복제본에서 읽고 병합(merge)함.
- **ReadMajority**: minCap을 지원하며 N/2 + 1개의 복제본에서 읽음.
- **ReadMajorityPlus**: 과반수에 추가 노드를 더해 읽으며, 빠져나가는 노드는 제외함.
- **ReadAll**: 빠져나가는 노드를 제외한 모든 노드에서 읽음.

공식 문서는 강한 일관성(strong consistency)을 위한 다음 조건을 제시함: **`(쓰인 노드 수 + 읽은 노드 수) > N`** (여기서 N은 전체 클러스터 노드 수).

#### 복제 데이터 타입 (Replicated Data Types)

##### 카운터 (Counters)

- **GCounter (grow-only, 증가 전용)**: 증가만 지원함. 노드마다 하나의 카운터를 유지하며, 전체 값은 모든 노드 카운터의 합임.
- **PNCounter (positive/negative, 양수/음수)**: 내부적으로 증가(P)와 감소(N) 카운터를 별도로 추적함. 최종 값 = P - N.
- **PNCounterMap**: 여러 관련 카운터를 단일 복제 단위 안에서 관리하며, 원자적(atomic) 동시 복제를 보장함.

##### 집합 (Sets)

- **GSet (grow-only, 증가 전용)**: 제거 없이 요소만 추가함. 병합 연산은 집합 합집합(union)을 계산함.
- **ORSet (observed-remove, 관찰-제거)**: 추가와 제거를 모두 지원함. 인과관계(causality) 추적을 위해 버전 벡터(version vector)와 "출생 점(birth dots)"을 구현함. 동일 요소가 동시에 추가/제거되면 **추가가 이긴다(add wins)**.

##### 맵 (Maps)

- **ORMap (observed-remove)**: 임의의 키 타입과 ReplicatedData 값을 가지는 범용 맵임. 동일 키에 대한 동시 업데이트는 값 병합을 유발함.
- **ORMultiMap**: ORSet 값을 감싸는 특화된 ORMap으로, 일대다(one-to-many) 관계를 가능하게 함.
- **PNCounterMap**: PNCounter 값을 사용하는 이름 붙은 카운터들의 맵임.
- **LWWMap**: 최종 쓰기 우선(last-writer-wins) 레지스터 값을 사용하는 특화된 ORMap으로, 동기화된 시계(synchronized clock)에 의존함.

##### 레지스터와 플래그

- **Flag**: false로 초기화되는 불리언(boolean) 값으로, 한 번 true로 전환되면 영구적임. 병합 시 true가 이김.
- **LWWRegister**: 직렬화 가능한 값을 담는다. 병합 시 타임스탬프가 가장 높은 값을 선택함. 동률(tie)이면 주소(address)가 가장 낮은 노드의 값을 사용함. 공식 문서는 경고함 — "시계 오차(clock skew) 범위 내에서 발생하는 동시 업데이트에 대해 값의 선택이 중요하지 않을 때만 사용해야 함."

#### 델타-CRDT 지원 (Delta-CRDT)

전체 엔트리(entry) 대신 상태 변경분(state changes)만 전송하여 복제를 최적화함. 일부 타입은 인과적 전달(`RequiresCausalDeliveryOfDeltas`)을 요구하고, 다른 타입은 최종 일관성을 제공함. 전체 상태 복제는 주기적으로, 또는 클러스터 변경 시, 또는 네트워크 파티션 이후에 일어남.

#### 추가 연산

- **Delete**: `Replicator.Delete`를 통해 데이터 엔트리를 제거함. 삭제된 키는 재사용할 수 없지만 복제 오버헤드를 줄인다. 이후 연산은 `DataDeleted` 응답을 받음.
- **Expire**: 설정된 비활성(inactivity) 기간 후 자동 제거함. 설정 예시:

```
akka.cluster.distributed-data.expire-keys-after-inactivity {
  "key-1" = 10 minutes
  "cache-*" = 2 minutes
}
```

만료된 엔트리는 톰스톤(tombstone)을 남기지 않으며 키를 재사용할 수 있음.

#### 커스텀 데이터 타입 구현

커스텀 타입을 구현하려면 `ReplicatedData`를 확장하고, 단조 수렴(monotonic convergence)을 보장하는 `merge` 함수를 제공해야 함. 공식 문서에 따르면 "데이터 타입은 불변(immutable)이어야 하며, 즉 '수정' 메서드는 새 인스턴스를 반환해야 함."

직렬화가 중요하다. 커스텀 타입은 효율적인 Akka 직렬화기를 필요로 하며, 이상적으로는 Protobuf를 사용함. 집합과 맵의 요소는 동일한 SHA-1 다이제스트(digest)를 갖도록 결정적(deterministic)으로 직렬화되어야 함.

#### 내구성 저장소 (Durable Storage)

설정으로 LMDB를 사용해 디스크에 영속화할 수 있음.

```
akka.cluster.distributed-data.durable.keys = ["a", "b", "durable*"]
```

원래 클러스터의 노드가 새 클러스터에 하나 이상 참여하면 재시작 후에도 데이터가 유지됨. 설정 옵션은 다음과 같음.

- **write-behind-interval**: 플러시(flush) 전에 변경분을 모은다(기본값: off, 즉 즉시 쓰기).
- **map-size**: 메모리 맵 파일 크기(기본값 100 MiB).
- **dir**: 저장 위치.

Java 17에서는 추가 JVM 플래그가 필요하다: `--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED`

#### 제한 사항과 고려 사항

이 시스템은 "빅데이터(Big Data)" 시나리오를 위해 설계되지 않았음. 주요 제약은 다음과 같음.

- 최상위(top-level) 엔트리 최대 약 **100,000개** 권장
- 모든 데이터는 메모리에 보관됨
- 큰 개별 엔트리는 과도하게 큰 원격 메시지를 만듦
- 새로운 클러스터 노드는 가십 전송을 받으며, 완전한 동기화에 수십 초가 걸림

##### CRDT 가비지와 프루닝 (Pruning)

CRDT는 이력(history), 즉 가비지(garbage)를 누적함. 특히 GCounter는 노드별 카운터를 무기한 유지함. 레플리케이터는 `RemovedNodePruning` 메커니즘을 통해 제거된 클러스터 노드와 관련된 데이터를 자동으로 프루닝함. 프루닝 마커(marker)는 설정된 기간(`pruning-marker-time-to-live`) 동안 유지되어, 네트워크 파티션 이후 손상된 데이터가 다시 주입되는 것을 방지함.

#### 학습 자료

- 영상: Mark Shapiro의 "Strong Eventual Consistency and Conflict-free Replicated Data Types"
- 논문: Shapiro 외, "A Comprehensive Study of Convergent and Commutative Replicated Data Types"

#### 주요 설정 속성

- **gossip-interval**: 2초(가십 전파 주기)
- **notify-subscribers-interval**: 500ms(구독자 알림 주기)
- **pruning-interval**: 120초(제거된 노드 데이터 프루닝 점검)
- **max-pruning-dissemination**: 300초(모든 복제본 전파 예상 시간)
- **pruning-marker-time-to-live**: 6시간(비내구성 데이터 마커 유지)
- **prefer-oldest**: off(가장 오래된 노드로의 라우팅 선호)

---

### 분산 발행-구독 (Distributed Publish Subscribe)

#### 개요

Akka의 분산 발행-구독(pub/sub) 시스템은 토픽(topic) 액터를 사용하여 클러스터 노드 전반에 걸친 메시징을 가능하게 함. 이 기능은 `akka-cluster-typed`에 포함되어 있으며, 각 토픽을 구독과 메시지 분배를 관리하는 하나의 액터로 표현하는 방식으로 동작함.

#### 모듈 정보

이 기능은 `akka-cluster-typed` 의존성을 필요로 함(문서 기준 버전 2.10.19). 토픽은 클러스터 환경에 배포되었을 때만 노드 간에 메시지를 분배함.

#### 토픽 레지스트리와 조회

토픽은 `PubSub` 레지스트리(registry)를 통해 관리됨. 레지스트리는 각 액터 시스템마다 동일한 이름의 토픽 액터가 하나만 존재하도록 보장함. 아직 시작되지 않은 토픽을 요청하면 레지스트리가 새로 생성하여 반환하고, 이미 활성 상태라면 기존 참조를 반환함.

레지스트리는 동일한 이름을 가지면서 메시지 타입이 다른 여러 토픽 액터가 공존하는 것을 막는다. 이를 시도하면 예외(exception)가 발생함.

#### 구독 관리

로컬 액터는 특정 명령을 통해 토픽과 상호작용함.

- **구독(Subscribing)**: 액터는 토픽 액터에게 구독 메시지를 보냄.
- **구독 해지(Unsubscribing)**: 대응되는 구독 해지 명령으로 액터를 토픽에서 제거함.

공식 문서는 다음을 명시함 — "토픽이 시작되어 있고 등록된 로컬 구독자가 있는 경우에만, 발행된 메시지가 다른 노드로 전달됨."

#### 메시지 발행

발행자(publisher)는 메시지를 토픽 액터에게 직접 보냄. 시스템은 노드 수준에서 메시지를 중복 제거(deduplicate)함 — 로컬에 여러 구독자가 있더라도, 발행된 메시지는 원격 노드당 네트워크를 한 번만 횡단함.

#### TTL(Time-to-Live) 설정

토픽에 TTL 파라미터를 설정할 수 있음. 지정된 기간 동안 토픽 액터에 로컬 구독자나 메시지 활동이 없으면, 토픽은 자동으로 정지되고 레지스트리에서 제거됨.

#### 수동 토픽 생성

레지스트리 기반 조회 외에도, 토픽을 액터로 직접 스폰(spawn)할 수 있음. 이 방식은 단일 노드에 동일 토픽에 대한 여러 토픽 액터를 둘 수 있게 하며, 각각은 원격 노드에서 발행된 메시지의 사본을 별도로 받음.

#### 확장성 특성 (Scalability)

각 토픽은 하나의 `Receptionist` 서비스 키(service key)를 사용하므로, 실용적인 배포에서는 고유 토픽이 수천 개에서 수만 개로 제한됨. 고유 토픽의 생성·소멸 빈도(turnover)가 높으면 성능이 저하되어 커스텀 솔루션이 필요할 수 있음. 토픽 액터는 프록시로 동작하며, 구독자 중복 제거와 리셉셔니스트(receptionist) 등록 관리를 담당함.

#### 전달 보장 (Delivery Guarantees)

이 시스템은 **최대 한 번(at-most-once) 전달 시맨틱**을 제공함 — 전송 중에 메시지가 유실될 수 있음. 또한 구독자 레지스트리는 최종 일관성(eventual consistency)을 달성하므로, 한 노드의 새 구독자가 다른 노드의 발행자에게 알려지기까지 짧은 지연이 발생함.

더 강한 보장이 필요한 애플리케이션에는 최소 한 번(at-least-once) 전달을 위해 Alpakka Kafka 사용을 권장함.

#### 주요 제한 사항

- 최대 수천 개의 토픽(더 많이 필요하면 커스텀 솔루션 필요)
- 최종 일관적인 구독자 탐색
- 최대 한 번 메시지 전달
- 고빈도 토픽 생성/소멸 패턴에는 부적합

---

### 샤드 데몬 프로세스 (Sharded Daemon Process)

#### 목적과 소개

샤드 데몬 프로세스(Sharded Daemon Process) 기능은 "0부터 시작하는 숫자 ID를 각각 부여받은 **N개의 액터**를 실행하고, 이들을 클러스터 전반에 걸쳐 살아 있게(keep alive) 유지하며 균형 있게(balanced) 배치하는 방법"을 제공함. 분산 데이터 처리 워크로드를 정해진 수의 워커(worker)에게 분담해야 하는 시나리오에 적합함.

대표적인 사용 사례는 CQRS 애플리케이션에서 이벤트 스트림(event stream)으로부터 프로젝션(projection)을 만드는 것임. 이벤트에 태그(tag)를 붙이고(N개의 태그 중 하나), 이를 통해 처리 책임을 N개의 워커에게 분배하여 각 워커가 데이터의 일부를 처리하게 함.

#### 핵심 기능

**동작 방식:**

- 클러스터 싱글톤(Cluster Singleton)이 킵얼라이브(keep-alive) 메시지를 트리거하여 클러스터 전반의 액터를 유지함.
- 리밸런싱이 필요해지면, 킵얼라이브 신호에 기반하여 액터가 정지되고 재시작됨.
- 단일 액터 시나리오라면 샤드 데몬 프로세스 대신 클러스터 싱글톤을 권장함.

#### 기본 설정과 초기화

모든 클러스터 노드에서 동일하게 프로세스를 초기화함.

```
val tags = Vector("tag-1", "tag-2", "tag-3")
ShardedDaemonProcess(system).init("TagProcessors", tags.size, id => TagProcessor(tags(id)))
```

Java에서는 유사한 파라미터로 `ShardedDaemonProcess.get(system).init()`을 사용함. 추가 팩토리 메서드는 우아한 정지 메시지와 향상된 설정을 지원함.

#### 프로세스 주소 지정 (Addressing the Processes)

데몬 프로세스 액터와의 통신은 "시스템 리셉셔니스트(system receptionist)"에 의존함. 브로드캐스트(broadcast)를 위한 단일 `ServiceKey`를 사용하거나, 표적(targeted) 메시징을 위한 개별 키를 사용함.

#### 동적 스케일링 (Dynamic Scaling)

`initWithContext` 메서드는 `ChangeNumberOfProcesses` 명령을 받는 `ActorRef[ShardedDaemonProcessCommand]`를 반환하여 런타임(runtime) 재조정(rescaling)을 가능하게 함. 이 작업은 액터들이 우아한 종료 절차를 수행하므로 오래 걸릴 수 있음. 동시(concurrent) 재조정 요청은 실패 응답을 반환함.

**롤링 업그레이드**: 정적(static) 워커 수에서 동적(dynamic) 워커 수로의 전환은 지원됨. 단, 동적에서 정적으로의 전환은 클러스터 전체 재시작이 필요함.

#### 프로세스를 같은 위치에 배치하기 (Colocating Processes)

`ConsistentHashingShardAllocationStrategy`를 사용하면 인덱스(index)를 공유하는 프로세스들을 노드 전반에서 같은 위치에 배치할 수 있어, 자원 공유 시나리오에 유리하다. 각 데몬 프로세스 이름은 자체 전략 인스턴스를 필요로 하며, 전략을 공유할 수 없음.

#### 확장성 한계

이 도구는 "최대 수천 개의 프로세스(up to thousands of processes)"까지의 배포에 적합함. 이 범위를 넘어서면 Akka 분산 데이터(Distributed Data) 복제나 킵얼라이브 메시징 시스템에 문제가 발생할 수 있음.

#### 설정 항목

주요 설정 속성은 다음과 같음.

- **keep-alive-interval**: `10s`(핑(ping) 주기)
- **keep-alive-from-number-of-nodes**: `3`(핑을 시작하는 노드 수)
- **keep-alive-throttle-interval**: `100 ms`(메시지 간 지연)

커스텀 `ShardedDaemonProcessSettings`로 역할 제한을 포함한 클러스터 샤딩 기본값을 재정의할 수 있음.

---

### 신뢰성 있는 전달 (Reliable Delivery)

#### 목적과 핵심 개념

신뢰성 있는 전달(Reliable Delivery)은 분산 시스템의 근본적인 과제, 즉 액터 간 메시지 유실을 방지하는 문제를 다룸. 표준 메시지 전달은 "최대 한 번(at-most-once)" 시맨틱을 제공하므로 메시지가 유실될 수 있음. 신뢰성 있는 전달 프레임워크는 "최소 한 번(at-least-once)" 또는 "사실상 한 번(effectively-once)" 전달을 구현하기 위한 도구를 제공함. 처리가 완전히 완료된 시점은 애플리케이션 계층만 알 수 있으므로, 발행자(producer)와 소비자(consumer) 액터의 협력이 필요함.

#### 전달 보장 (Delivery Guarantees)

- **사실상 한 번 처리(Effectively-Once)**: 발행자와 소비자 모두 크래시(crash)하지 않는 한, 메시지는 유실이나 중복 없이 순서대로 도착하며 비즈니스 수준의 중복 제거가 필요 없음.
- **내구성 있는 최소 한 번(At-Least-Once with Durability)**: `EventSourcedProducerQueue`를 통한 내구성 큐(durable queue)가 활성화되면, 미확인(unconfirmed) 메시지가 크래시를 견디고 재전달(redeliver)됨. 즉, 발행자가 재시작되면 일부 메시지가 두 번 처리될 수 있으므로, 멱등(idempotent) 소비나 비즈니스 수준의 중복 제거가 필요함.
- **흐름 제어 메커니즘(Flow Control)**: 프레임워크는 수요 기반(demand-driven) 전송을 강제하여 빠른 발행자가 느린 소비자를 압도하거나 네트워크 용량을 고갈시키는 것을 막는다 — 발행자는 소비자가 더 많은 작업을 요청할 때만 전송함.

#### 지원하는 세 가지 패턴

##### 점대점 패턴 (Point-to-Point)

단일 발행자와 단일 소비자 사이에 신뢰성 있는 전달을 확립함. `ProducerController`와 `ConsumerController`가 메시지 래핑(wrapping), 확인(acknowledgment) 추적, 재전송(resend) 로직을 처리함.

**흐름**: 발행자가 `ProducerController`에 `Start` 메시지를 보냄 → `RequestNext`를 받음 → 메시지 하나를 보냄 → 다음 `RequestNext`를 기다림. 마찬가지로 소비자는 `ConsumerController`에 `Start`를 보내고, `Delivery`로 감싸진 메시지를 받은 뒤 `Confirmed`로 응답함.

**핵심 제약**: 발행자와 `ProducerController`는 같은 위치(colocal)에 있어야 함(런타임 검사로 강제). 성능과 전달 보장을 위해서이며, 소비자와 `ConsumerController`에도 동일하게 적용됨.

**연결(Connection)**: 컨트롤러는 `ConsumerController.RegisterToProducerController` 또는 `ProducerController.RegisterConsumer`를 통해 연결되어야 함. 애플리케이션이 이 연결을 관리하며, 보통 `Receptionist`나 일반 메시지 전달을 사용함.

**메시지 흐름**: 미확인 메시지는 버퍼링되며, 흐름 제어 윈도우(flow control window)에 의해 제한됨. 소비자 측이 수요를 주도하며, `ProducerController`는 요청된 비율을 초과하지 않음.

##### 워크 풀링 패턴 (Work Pulling)

여러 워커 액터가 공유된 작업 관리자(work manager)로부터 각자의 속도로 동적으로 작업을 끌어온다(pull). 점대점과 달리 메시지 순서는 중요하지 않으며, 각 메시지는 가용한 워커에게 무작위로(randomly) 라우팅됨.

**동적 등록**: 워커는 `ServiceKey`를 사용해 `Receptionist`에 자신을 등록함. `WorkPullingProducerController`는 이 키를 구독하여 활성 워커를 자동으로 발견함.

**스케일링**: 워커는 발행자를 명시적으로 재설정하지 않고도 어느 클러스터 노드에서든 동적으로 추가/제거될 수 있음.

**버퍼링**: 수요가 있던 모든 워커가 `RequestNext` 이후, 메시지 전송 이전에 등록을 해지하면, 새 워커가 도착하거나 수요가 돌아올 때까지 메시지가 버퍼링됨.

**로컬 요건**: 발행자와 `WorkPullingProducerController`는 같은 위치에 있어야 함(런타임 검사로 강제됨).

##### 샤딩 패턴 (Sharding)

신뢰성 있는 전달은 클러스터 샤딩(Cluster Sharding)과 통합되어, 발행자에서 샤딩된 소비자로의 시나리오를 지원함. 노드/시스템당 하나의 `ShardingProducerController`가 `entityId`로 식별되는 여러 샤딩 엔티티로 메시지를 보냄.

**구성 요소**: `ShardingProducerController`가 각 샤딩 엔티티를 감싸는 `ShardingConsumerController`로 메시지를 보냄. 둘 사이에 명시적 등록은 필요 없음.

**선택적 전송**: `RequestNext`는 수요가 있는 엔티티 정보를 포함함. 발행자는 현재 수요가 없는 엔티티(메시지가 버퍼링됨)나, 심지어 완전히 새로운 엔티티로도 보낼 수 있음.

**로컬 요건**: 발행자와 `ShardingProducerController`는 같은 위치에 있어야 함.

**다중 발행자**: 서로 다른 노드가 동일한 샤딩 엔티티 타입을 사용하는 별개의 발행자를 호스팅할 수 있으며, 각각 고유한 `producerId`로 식별됨.

#### 발행자 및 소비자 컨트롤러

**ProducerController**가 관리하는 것:

- 확인될 때까지 메시지 버퍼링
- 미확인 메시지의 재전송 로직
- 흐름 제어 윈도우 강제
- 큰 메시지의 선택적 청킹(chunking)
- 선택적 내구성 큐와의 통합

**ConsumerController**가 관리하는 것:

- 메시지 전달 래핑
- 확인 추적
- 확인 대기 중 메시지 스태싱(stashing)
- 재전달 시 중복 제거

두 컨트롤러는 각자의 비즈니스 액터와 같은 위치에 있어야 하며, 컨트롤러 프로토콜과 비즈니스 프로토콜 간 매핑을 위해 메시지 어댑터(message adapter)를 통해 통신함.

#### 흐름 제어와 확인

흐름 제어는 소비자 주도(consumer-driven) 방식임. `ConsumerController`가 `RequestNext` 메시지로 용량(capacity)을 요청함. 발행자는 이 요청을 존중하여 메일박스(mailbox)의 큐 포화(saturation)를 방지함.

**확인 패턴**: 소비자는 `Delivery(message, confirmTo)`를 받아 메시지를 처리한 뒤, `confirmTo`로 `ConsumerController.Confirmed`를 보냄. 확인이 도착하기 전까지는 다음 메시지가 전달되지 않음.

**윈도우 크기(Window Size)**: 여러 미확인 메시지가 동시에 전송 중(in flight)일 수 있으나, 설정 가능한 흐름 제어 윈도우로 제한됨.

#### 큰 메시지 청킹 (Chunking Large Messages)

점대점 패턴에서, `akka.reliable-delivery.producer-controller.chunk-large-messages`(바이트 단위)를 초과하는 메시지는 `ProducerController`에 의해 자동으로 작은 조각(piece)으로 분할됨. `ConsumerController`가 이를 투명하게(transparently) 재조립함.

**이점**: 큰 메시지의 직렬화가 작은 메시지를 지연시키는 헤드 오브 라인 블로킹(head-of-line blocking)을 방지함. 직렬화는 전송 계층(transport layer)이 아닌 컨트롤러에서 이루어짐.

**내구성 큐와의 상호작용**: 내구성 큐가 활성화되면, 영속화 전에 재조립하지 않고 청크(chunk)를 개별적으로 저장함.

**제한**: 현재 점대점 패턴에서만 지원됨.

#### 내구성 발행자 큐 (Durable Producer Queue)

`EventSourcedProducerQueue`(`akka-persistence-typed` 소속)는 이벤트 소싱을 사용해 미확인 메시지를 영속화함. 발행자 JVM이 크래시하면, 동일한 `PersistenceId`로 재시작 시 메시지가 재전달됨.

**성능 비용**: 내구성은 상당한 오버헤드를 추가함. 메시지마다 영속화 연산이 수반됨.

**고유 식별자**: `PersistenceId`는 발행자마다 전역적으로 고유해야 함. 클러스터 싱글톤이나 네이밍 규칙을 갖춘 노드별 발행자를 통해 보장할 수 있음. 동일한 ID를 여러 발행자 인스턴스에서 동시에 사용해서는 안 됨.

**전달 시맨틱**: 내구성 큐를 사용하면, 확인이 아직 저장되지 않은 상태에서 크래시가 나면 이미 처리된 메시지가 재전달될 수 있음 — 즉 최소 한 번(at-least-once) 전달을 구현함.

#### Ask 기반 확인 (Ask-Based Confirmation)

`sendNextTo`를 통한 `tell` 대신, 발행자는 `askNextTo`와 함께 `context.ask`를 사용하여 메시지를 `MessageWithConfirmation`으로 감쌀 수 있음. 응답은 내구성 큐가 활성화된 경우 성공적인 영속화를, 그렇지 않은 경우 소비자의 완전한 처리를 확인해 줌.

#### 설정

신뢰성 있는 전달 설정은 `akka.reliable-delivery` 네임스페이스(namespace)에 위치하며, 다음 세부 섹션을 가짐.

- `producer-controller`: 청크 크기, 흐름 제어 윈도우, 재전송 주기
- `consumer-controller`: only-flow-control 모드, 흐름 제어 윈도우
- `work-pulling-producer-controller`: 수요 라우팅 동작
- `sharding-producer-controller`: 엔티티 버퍼링 제한

참조 설정(reference configuration)은 actor-typed, persistence-typed, cluster-sharding-typed 모듈에서 제공됨. 운영자는 네트워크 상태, 메시지 크기, 지연 요건에 맞게 이를 튜닝(tune)함.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Cluster Sharding](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharding.html)
- [Cluster Sharding Concepts](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharding-concepts.html)
- [Cluster Singleton](https://doc.akka.io/libraries/akka-core/current/typed/cluster-singleton.html)
- [Distributed Data](https://doc.akka.io/libraries/akka-core/current/typed/distributed-data.html)
- [Distributed Publish Subscribe](https://doc.akka.io/libraries/akka-core/current/typed/distributed-pub-sub.html)
- [Sharded Daemon Process](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharded-daemon-process.html)
- [Reliable Delivery](https://doc.akka.io/libraries/akka-core/current/typed/reliable-delivery.html)
