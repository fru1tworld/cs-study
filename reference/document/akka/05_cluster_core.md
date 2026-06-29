# Akka 클러스터 핵심

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster.html

---

## 목차

1. [클러스터 사용하기 (Cluster Usage)](#1-클러스터-사용하기-cluster-usage)
2. [클러스터 명세 (Cluster Specification / Concepts)](#2-클러스터-명세-cluster-specification--concepts)
3. [클러스터 멤버십 라이프사이클 (Cluster Membership Lifecycle)](#3-클러스터-멤버십-라이프사이클-cluster-membership-lifecycle)
4. [장애 감지기 (Failure Detector)](#4-장애-감지기-failure-detector)
5. [클러스터를 언제, 어디에 사용할 것인가 (Choosing Akka Cluster)](#5-클러스터를-언제-어디에-사용할-것인가-choosing-akka-cluster)
6. [스플릿 브레인 리졸버 (Split Brain Resolver)](#6-스플릿-브레인-리졸버-split-brain-resolver)
7. [참고 자료](#참고-자료)

---

## 1. 클러스터 사용하기 (Cluster Usage)

Akka 클러스터(cluster)는 단일 장애 지점(single point of failure)이 없는, 결함 허용(fault-tolerant)을 갖춘 탈중앙(decentralized) P2P 기반의 클러스터 멤버십 서비스(membership service)를 제공합니다. 클러스터는 가십 프로토콜(gossip protocol)과 자동 장애 감지(automatic failure detection)를 사용하여 여러 노드(node)와 여러 액터 시스템(ActorSystem)에 걸친 분산 애플리케이션을 구성할 수 있게 해줍니다.

### 1.1 모듈 정보 (Module Info)

클러스터 기능을 사용하려면 프로젝트 의존성에 다음 아티팩트를 추가해야 합니다.

```
com.typesafe.akka:akka-cluster-typed
```

(예: 버전 2.10.19 기준)

- 지원 JDK: Eclipse Temurin 11, 17, 21
- 지원 Scala: 2.13.17 또는 3.3.7
- JPMS 모듈명: `akka.cluster.typed`
- 라이선스: BUSL-1.1
- 지원 상태: Lightbend에서 공식 지원

클러스터를 사용하려면 노드 간 통신을 위한 직렬화(serialization)를 반드시 활성화해야 합니다. 기본 직렬화기로는 Jackson 직렬화(Jackson serialization)를 권장합니다. 또한 리모팅(remoting)은 Artery 프로토콜을 통해 구성됩니다.

---

### 1.2 클러스터 API 확장 (Cluster API Extension)

`Cluster` 확장(extension)은 다음 세 가지 주요 인터페이스를 제공합니다.

- **매니저 참조 (Manager Reference)**: `Join`, `Leave`, `Down`과 같은 클러스터 명령(command)을 처리합니다.
- **구독 참조 (Subscriptions Reference)**: `Subscribe`, `Unsubscribe`, `GetCurrentState` 메시지를 통해 클러스터 상태(state) 변경을 수신(listen)할 수 있게 합니다.
- **상태 프로퍼티 (State Property)**: 현재의 `CurrentClusterState` 정보에 접근합니다.

---

### 1.3 클러스터 멤버십 API (Cluster Membership API)

#### 1.3.1 클러스터 합류 (Joining)

노드가 클러스터에 합류하는 방식에는 세 가지가 있습니다.

1. **자동 디스커버리 (Automatic Discovery)**
   - Akka Management의 클러스터 부트스트랩(Cluster Bootstrap)을 사용합니다.

2. **설정 기반 (Configuration-Based)**
   - `application.conf`에 시드 노드(seed-nodes)를 정의합니다.
   - 시드 노드는 합류하려는 노드의 초기 접속 지점(initial contact point) 역할을 합니다.
   - 목록의 첫 번째 노드(first node)는 클러스터를 부트스트랩하기 위해 가장 먼저 시작되어야 합니다.
   - 합류 시도가 실패하면 자동으로 재시도(retry)합니다.

3. **프로그래밍 방식 (Programmatic)**
   - 외부 도구를 통해 노드를 동적으로(dynamically) 발견하여 합류합니다.

#### 1.3.2 클러스터 이탈 (Leaving)

노드를 클러스터에서 제거할 때 권장되는 방식은 코디네이티드 셧다운(Coordinated Shutdown)을 통한 우아한 종료(graceful exit)입니다. 코디네이티드 셧다운은 다음과 같은 경우에 트리거됩니다.

- 액터 시스템(ActorSystem)이 종료될 때
- SIGTERM 시그널을 수신할 때
- 수동으로 HTTP/JMX 명령을 실행할 때

#### 1.3.3 다운 처리 (Downing)

장애 감지(failure detection) 이후, 도달 불가능(unreachable)한 멤버를 제거합니다. Akka는 다음 설정을 통해 스플릿 브레인 리졸버(Split Brain Resolver)를 활성화할 것을 권장합니다.

```
akka.cluster.downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
```

---

### 1.4 노드 역할 (Node Roles)

노드에는 역할(role)을 부여할 수 있습니다(예: "backend", "frontend"). 역할은 설정 프로퍼티 `akka.cluster.roles`에 정의합니다. 역할을 활용하면 노드 종류에 따라 특정 액터를 조건부로 시작할 수 있으며, 이 역할 정보는 멤버십 이벤트(membership event)를 통해 확인할 수 있습니다.

---

### 1.5 장애 감지 (Failure Detection)

Akka는 기본적으로 PhiAccrual 장애 감지기(PhiAccrual Failure Detector)를 사용하며, 하트비트(heartbeat)를 통해 노드를 모니터링합니다. 주요 설정 파라미터는 다음과 같습니다.

- `akka.cluster.failure-detector.threshold` — 장애로 판단하는 phi 값(threshold)
- `akka.cluster.failure-detector.acceptable-heartbeat-pause` — 허용되는 비정상(abnormal) 상황의 여유 마진

자세한 내용은 [4. 장애 감지기](#4-장애-감지기-failure-detector)를 참고하십시오.

---

### 1.6 설정 핵심 (Configuration Highlights)

#### 1.6.1 최소 필수 설정 (Minimum Configuration Required)

- 액터 프로바이더를 클러스터로 설정: `akka.actor.provider = "cluster"`
- 호스트네임/포트를 포함한 리모팅(remoting) 구성
- 시드 노드(seed-nodes) 정의

#### 1.6.2 시작 제어 (Startup Control)

```
akka.cluster.min-nr-of-members = 3
```

이 설정은 클러스터가 지정한 크기에 도달할 때까지 "Joining" 상태의 멤버를 "Up" 상태로 승격(promote)하는 것을 지연시킵니다.

#### 1.6.3 로깅 옵션 (Logging Options)

- `akka.cluster.log-info = off` — 클러스터 이벤트 로깅을 끕니다.
- `akka.cluster.log-info-verbose = on` — 문제 해결(troubleshooting)을 위한 상세(verbose) 로그를 활성화합니다.

#### 1.6.4 디스패처 설정 (Dispatcher Configuration)

- 클러스터 액터(cluster actor)는 기본적으로 내부 디스패처(internal dispatcher)에서 실행됩니다.
- `akka.cluster.use-dispatcher` — 클러스터 전용 커스텀 디스패처를 지정합니다.
- 이를 통해 사용자 액터(user actor)와의 간섭(interference)을 방지합니다.

#### 1.6.5 설정 호환성 검사 (Configuration Compatibility Check)

노드가 합류할 때 모든 클러스터 노드가 호환되는 설정(compatible settings)을 가지고 있는지 검증합니다. 롤링 업데이트(rolling update)를 위해 다음 설정으로 비활성화할 수 있습니다.

```
akka.cluster.configuration-compatibility-check.enforce-on-join = off
```

다만 이를 비활성화하면 데이터 손상(data corruption)의 위험이 있으므로 주의해야 합니다.

---

### 1.7 상위 수준 클러스터 도구 (Higher-Level Cluster Tools)

클러스터 위에서 동작하는 상위 수준 도구들은 다음과 같습니다.

- **클러스터 싱글톤 (Cluster Singleton)**: 클러스터 전체에서 액터 인스턴스가 단 하나만 존재하도록 보장합니다.
- **클러스터 샤딩 (Cluster Sharding)**: 논리적 식별자(logical identifier)를 사용하여 액터를 노드들에 분산 배치합니다.
- **분산 데이터 (Distributed Data)**: 노드 간 데이터를 공유하기 위한 키-값 저장소(key-value store)를 제공합니다.
- **분산 발행-구독 (Distributed Publish-Subscribe)**: 물리적 위치를 인지하지 않고 토픽(topic) 기반 메시징을 지원합니다.
- **클러스터 인지 라우터 (Cluster-Aware Routers)**: 라운드 로빈(round-robin), 일관된 해싱(consistent hashing) 등의 전략으로 메시지를 라우팅합니다.
- **신뢰성 있는 전달 (Reliable Delivery)**: 흐름 제어(flow control)와 함께 메시지 전달을 보장합니다.

---

### 1.8 테스트 지원 (Testing Support)

- 단위 테스트(unit testing) 지원
- 클러스터 시나리오를 위한 멀티 노드 테스트(Multi Node Testing)
- 분산 환경을 위한 멀티 JVM 테스트(Multi JVM Testing)

---

## 2. 클러스터 명세 (Cluster Specification / Concepts)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster-concepts.html

### 2.1 개요 (Overview)

Akka 클러스터는 "단일 장애 지점이 없는, 결함 허용 탈중앙 P2P 기반 클러스터 멤버십 서비스(fault-tolerant decentralized peer-to-peer based Cluster Membership Service with no single point of failure)"입니다. 가십 프로토콜(gossip protocol)과 자동 장애 감지(automatic failure detection)를 사용하여 여러 노드 및 여러 액터 시스템에 걸친 분산 애플리케이션을 가능하게 합니다.

이 멤버십 시스템은 Amazon의 Dynamo와 Basho의 Riak에서 유래했으며, 클러스터 상태 정보를 전파하는 데 가십 프로토콜을 사용합니다.

---

### 2.2 핵심 용어 (Key Terminology)

- **노드 (Node)**: `hostname:port:uid` 튜플로 식별되는 논리적 클러스터 멤버입니다. UID 덕분에 한 물리 장비에서 여러 노드를 운영할 수 있습니다.
- **클러스터 (Cluster)**: 클러스터 멤버십 서비스를 통해 서로 협력(coordinate)하는, 상호 연결된 노드들의 집합입니다.
- **리더 (Leader)**: 클러스터의 수렴(convergence)과 멤버십 전이(membership transition)를 관리하는, 지정된 단일 노드입니다. 리더십은 수렴 시점에 정렬 순서(sorted order)상 가장 먼저 자격을 갖춘(first eligible) 노드에게 결정론적으로(deterministically) 할당됩니다.

---

### 2.3 가십 프로토콜 아키텍처 (Gossip Protocol Architecture)

멤버십 시스템은 가십 프로토콜을 사용하여 클러스터 상태 정보를 전파합니다. "클러스터에 대한 정보는 특정 시점에 노드에서 로컬로 수렴(converge locally)"하며, 이는 모든 멤버가 현재 상태 버전(state version)을 관측했을 때 발생합니다.

#### 2.3.1 벡터 클록 (Vector Clocks)

벡터 클록(vector clock)은 `(node, counter)` 쌍을 추적하여, 분산 시스템에서 이벤트 간 부분 순서(partial event ordering)를 확립하고 인과성 위반(causality violation)을 감지할 수 있게 합니다. 가십 교환(gossip exchange) 시 클러스터 상태의 차이를 조정(reconcile)하는 데 사용됩니다.

#### 2.3.2 수렴 메커니즘 (Convergence Mechanism)

수렴(convergence)을 위해서는 모든 노드가 현재 상태 버전을 보아야 합니다. 노드가 도달 불가능(unreachable) 상태로 남아 있는 동안에는 수렴이 완료될 수 없습니다. 도달 불가능한 노드는 반드시 다시 도달 가능(reachable)해지거나, 다운(down)되거나, 제거(removed)되어야 합니다.

이 제약은 오직 리더 액션(leader action)에만 영향을 미치며, 애플리케이션의 동작 자체에는 영향을 주지 않습니다.

#### 2.3.3 장애 감지 (Failure Detection)

Phi Accrual 장애 감지기(Phi Accrual Failure Detector)가 노드의 도달 가능성(reachability)을 모니터링합니다. 각 노드는 해시된 링 순서(hashed ring ordering)를 통해 선택된 5개의 다른 노드를 모니터링하며, 랙(rack)이나 데이터센터(datacenter)를 가로지르는 모니터링을 유도합니다.

장애가 감지되면, "단 하나의 노드만 어떤 노드를 도달 불가능으로 표시(mark unreachable)해도, 가십 전파를 통해 나머지 클러스터 전체가 그 노드를 도달 불가능으로 표시"하게 됩니다.

격리된(quarantined) 노드는 전달할 수 없는(undeliverable) 시스템 메시지를 가지게 되며, 재시작(restart)하기 전까지는 복구될 수 없습니다.

#### 2.3.4 시드 노드 (Seed Nodes)

시드 노드(seed node)는 합류하려는 노드의 접속 지점(contact point) 역할을 하지만, 이미 동작 중인 클러스터의 운영에는 영향을 주지 않습니다. 새 멤버는 기존 클러스터 멤버 중 어느 것에든 연결할 수 있습니다.

#### 2.3.5 가십 최적화 (Gossip Optimization)

시스템은 다이제스트(digest)를 사용하는 푸시-풀 가십(push-pull gossip)을 채택하며, 실제 값(actual value)은 필요한 경우에만 전송합니다.

- 표준 간격은 1초이며, 초기 전파 단계에서는 초당 3회로 가속됩니다.
- 알고리즘은 현재 상태 버전을 가지지 못한 노드 쪽으로 선택을 편향(bias)시키며, 400개 노드를 초과하는 클러스터에서는 이 편향을 점진적으로 줄입니다.
- Protobuf 직렬화와 gzip 압축을 사용하여 페이로드(payload) 크기를 줄입니다.

---

## 3. 클러스터 멤버십 라이프사이클 (Cluster Membership Lifecycle)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cluster-membership.html

### 3.1 멤버십 소개 (Introduction to Cluster Membership)

Akka 클러스터의 핵심은 클러스터 멤버십(cluster membership)으로, 어떤 노드가 클러스터의 일부인지 그리고 그들의 상태(health)가 어떤지를 추적합니다.

노드는 `hostname:port:uid` 튜플로 식별되며, 여기서 UID는 각 액터 시스템 인스턴스를 고유하게 식별합니다. 이 메커니즘은 시스템이 제거(removed)된 이후 다시 합류하는 것을 방지합니다. 다시 합류하려면 반드시 다른 UID를 가진 새 인스턴스를 생성해야 합니다.

---

### 3.2 멤버 상태 (Member States)

클러스터는 다음과 같은 멤버 상태(member state)를 정의합니다.

- **Joining (합류 중)**: 노드가 처음 클러스터에 진입할 때의 일시적(transient) 단계입니다.
- **WeaklyUp (약하게 가동)**: 네트워크 파티션(network partition) 중에 사용할 수 있는 중간 상태입니다(`akka.cluster.allow-weakly-up-members`가 활성화된 경우). 이 상태는 완전한 수렴(full convergence)이 이루어지지 않은 상황에서도 합류 중인 노드가 진행할 수 있게 합니다.
- **Up (가동)**: 완전히 통합된(fully integrated) 클러스터 멤버의 표준 운영 상태입니다.
- **Preparing for Shutdown / Ready for Shutdown (셧다운 준비 / 셧다운 준비 완료)**: 클러스터 전체의 협조된 종료(cluster-wide termination) 직전에 거치는 선택적(optional) 상태로, 불필요한 리밸런싱(rebalancing) 작업을 방지합니다.
- **Leaving / Exiting (이탈 중 / 종료 중)**: 노드를 제어된 방식으로 제거할 때 거치는 우아한 이탈(graceful exit) 상태입니다.
- **Down (다운)**: 영구적으로 사용 불가능(permanently unavailable)으로 표시된 노드로, 클러스터 결정(decision)에서 제외됩니다.
- **Removed (제거됨)**: 멤버십에서 완전히 제거되었음을 나타내는 최종 툼스톤(tombstone) 상태입니다.

---

### 3.3 멤버 이벤트 (Member Events)

시스템은 다음과 같은 라이프사이클 이벤트(lifecycle event)를 발행(publish)합니다.

- `MemberJoined`: 클러스터에 처음 진입했음을 알립니다.
- `MemberUp`: 완전히 통합되었음을 나타냅니다.
- `MemberExited`: 이탈(leaving) 프로세스가 진행 중임을 표시합니다.
- `MemberRemoved`: 완전한 이탈(departure)을 확정합니다.
- `UnreachableMember`: 장애 감지(detection failure)를 알립니다.
- `ReachableMember`: 연결이 복구(restore)되었음을 알립니다.
- `MemberPreparingForShutdown`, `MemberReadyForShutdown`: 셧다운 준비 상태를 알립니다.

---

### 3.4 멤버십 라이프사이클 (Membership Lifecycle)

- **합류 프로세스 (Joining Process)**: 노드는 `join` 액션으로 시작하여 joining 상태에 진입합니다. "모든 노드가 (가십 수렴을 통해) 새 노드가 합류 중임을 본(have seen) 후"에, 클러스터 리더(leader)가 해당 노드를 up 상태로 전이시킵니다.

- **우아한 이탈 (Graceful Departure)**: `leave` 액션(보통 코디네이티드 셧다운을 통함)은 노드를 leaving 상태로 옮깁니다. 리더 수렴(leader convergence)이 확인되면 exiting을 거쳐 removed로 진행됩니다.

- **도달 불가능 처리 (Unreachability Handling)**: 노드가 도달 불가능(unreachable)해지면, "가십 수렴이 불가능해지고 따라서 대부분의 리더 액션도 불가능해집니다." 해당 노드는 연결을 복구하거나 명시적으로 다운(down)되어야 합니다.

- **시스템 재시작 시나리오 (System Restart Scenarios)**: 크래시(crash)된 노드가 동일한 주소로 재시작하면, 클러스터는 "이전 인스턴스를 자동으로 다운으로 표시(automatically marks as down)하고, 새 인스턴스는 수동 개입 없이(without manual intervention) 클러스터에 다시 합류"할 수 있습니다.

---

### 3.5 리더의 역할과 책임 (Leader Role and Responsibilities)

리더(leader)는 중대한 상태 확정(state confirmation)을 수행합니다. 리더는 "가십 수렴 이후 각 노드가 명확하게(unambiguously) 결정할 수 있지만(can be determined)", 일관된 클러스터 전체의 합의(consistent cluster-wide agreement)를 보장하기 위해 대부분의 액션에는 수렴이 필요합니다.

파티션(partition) 발생 시에는 서로 분리된 네트워크 구간에 각각 별도의 리더가 존재할 수 있습니다. 시스템은 스플릿 브레인 리졸버(Split Brain Resolver)를 통해 "각 파티션이 어떤 노드가 도달 가능한지에 대해 서로 다른 시각(own view)을 가질 수 있는" 상황을 처리합니다.

---

### 3.6 WeaklyUp 멤버 설명 (WeaklyUp Members Explanation)

이 기능이 활성화되면, "수렴이 아직 이루어지지 않은 상태에서도 합류 중인 노드가 WeaklyUp 상태로 승격(promoted)"될 수 있습니다.

WeaklyUp 상태의 노드는 오직 한쪽 파티션에만 존재합니다. 네트워크 분리(network split)의 반대편에 있는 멤버들은 이 노드의 존재를 전혀 알지 못하므로, 정족수 결정(quorum decision)에 사용하기에는 적합하지 않습니다.

---

### 3.7 전체 클러스터 셧다운 (Full Cluster Shutdown)

`prepareForFullClusterShutdown()` 메서드는 임박한 클러스터 종료(imminent cluster termination)를 알리며, "클러스터 샤딩 리밸런스(Cluster sharding rebalances)"나 싱글톤 마이그레이션(singleton migration)과 같은 비용이 큰 작업을 방지합니다.

모든 up 멤버는 완전한 종료에 앞서 ReadyForShutdown 상태에 도달해야 합니다.

---

### 3.8 상태 전이 (State Transitions)

**사용자 액션 (User Actions):**

- **Join**: 명시적 또는 자동(automatic)으로 클러스터에 진입합니다.
- **Leave**: 우아한 이탈을 시작합니다.
- **Down**: 실패한 노드를 수동 또는 자동으로 제거합니다.

**리더 액션 (Leader Actions):**

- joining → up (수렴 필요)
- joining → weakly up (수렴 없이 동작)
- weakly up → up (수렴이 복구된 후)
- leaving → exiting → removed
- down → removed

---

### 3.9 장애 감지와 도달 가능성 (Failure Detection)

도달 불가능(unreachability)은 상태(state)가 아니라 플래그(flag)로 동작합니다. "노드는 그것을 모니터링하는 모든 노드가 다시 도달 가능하다고 볼 때에만(only after all monitoring nodes see it as reachable again) 다시 도달 가능한 것으로 간주"되며, 이를 통해 연결성에 대한 분산 합의(distributed consensus)를 보장합니다.

---

## 4. 장애 감지기 (Failure Detector)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/failure-detector.html

### 4.1 개요 (Overview)

Phi Accrual 장애 감지기(Phi Accrual Failure Detector)는 Akka의 Remote DeathWatch가 하트비트 모니터링(heartbeat monitoring)을 통해 네트워크 장애와 JVM 크래시를 식별하는 데 사용하는 메커니즘입니다. 이는 Hayashibara 등이 발표한 논문 "The Phi Accrual Failure Detector"에 설명된 알고리즘을 구현한 것입니다.

---

### 4.2 핵심 개념 (Core Concept)

이 감지기는 단순한 "up" 또는 "down"의 이진(binary) 답변을 제공하는 대신, 노드가 장애를 일으켰을 통계적 가능성(statistical likelihood)을 나타내는 **phi 값(phi value)**을 반환합니다. 이는 모니터링(monitoring)과 해석(interpretation)을 분리(decouple)하여, 다양한 네트워크 조건에 적응할 수 있게 합니다.

감지기는 수신한 하트비트로부터 과거 통계(historical statistics)를 유지하고, 단순한 예/아니오(yes/no) 판단에 의존하는 대신 이러한 지표를 사용하여 노드 상태를 통계적으로 평가합니다.

---

### 4.3 Phi 계산 (Phi Calculation)

phi 값은 다음과 같이 계산됩니다.

```
phi = -log10(1 - F(timeSinceLastHeartbeat))
```

여기서 `F`는 정규 분포(normal distribution)의 누적 분포 함수(cumulative distribution function, CDF)이며, 과거 하트비트 도착 간격(inter-arrival time)의 평균(mean)과 표준편차(standard deviation)를 사용하여 계산됩니다.

---

### 4.4 하트비트 동작 (Heartbeat Behavior)

- **기본 주기 (Default frequency)**: 초당 1회 하트비트 (설정 가능)
- **메커니즘 (Mechanism)**: 요청/응답(request/reply) 핸드셰이크 패턴
- **입력 (Input)**: 응답(reply)의 도착이 장애 감지기 알고리즘의 입력으로 들어갑니다.

---

### 4.5 Phi 반응성과 표준편차 (Phi Responsiveness and Standard Deviation)

곡선(curve)의 가파른 정도(steepness)가 장애 감지 속도를 결정합니다.

- **낮은 표준편차 (100ms)**: 가파른 곡선 → 더 빠른 장애 감지
- **높은 표준편차 (200ms)**: 완만한 기울기 → 더 느린 감지
- 네트워크 변동성(network variability)은 감지 속도에 직접적인 영향을 줍니다.

---

### 4.6 설정 파라미터 (Configuration Parameters)

#### 4.6.1 `acceptable-heartbeat-pause`

가비지 컬렉션(garbage collection) 일시 정지나 일시적 네트워크 문제 같은 임시 장애에 대한 안전 마진(safety margin)을 제공합니다. 불안정한 환경(예: Amazon EC2와 같은 클라우드 플랫폼)에서는 기본값을 더 높게 조정할 수 있습니다.

```
akka.cluster.failure-detector.acceptable-heartbeat-pause = 7s
```

#### 4.6.2 `threshold`

민감도(sensitivity)를 제어하며 기본값은 8입니다. 값이 높을수록 거짓 양성(false positive)은 줄지만 크래시 감지가 지연됩니다. 클라우드 환경에서는 12 정도의 값이 필요할 수 있습니다.

---

### 4.7 로깅 지표 (Logging Indicators)

장애 감지기는 다음과 같은 중요한 로그 메시지를 생성합니다.

- `"Marking node(s) as UNREACHABLE"` — 노드가 장애로 의심됨
- `"Marking node(s) as REACHABLE"` — 노드가 복구됨
- `"heartbeat interval is growing too large"` — 허용 일시정지(acceptable pause)의 2/3에서 출력되는 경고
- `"Scheduled sending of heartbeat was delayed"` — 조사가 필요함

UNREACHABLE/REACHABLE 사이클이 빈번하게 반복된다면, `acceptable-heartbeat-pause`를 늘리거나, 과도한 가비지 컬렉션 같은 근본적인 시스템 문제를 조사해야 함을 시사합니다.

---

## 5. 클러스터를 언제, 어디에 사용할 것인가 (Choosing Akka Cluster)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/choosing-cluster.html

### 5.1 개요 (Overview)

핵심 질문은 마이크로서비스(microservices)와 전통적 분산 애플리케이션(distributed application) 접근 방식 중 무엇을 선택할 것인가이며, 이 선택은 Akka 클러스터를 어떻게 구현할지에 큰 영향을 줍니다.

"Stateful or Stateless applications: to Akka Cluster or not" 영상은 아키텍처에서 Akka 클러스터를 사용하는 동기를 탐구합니다.

---

### 5.2 마이크로서비스 아키텍처 (Microservices Architecture)

**핵심 원칙**: 마이크로서비스에서는 서비스 간 독립성(independence)을 유지합니다.

#### 5.2.1 서비스 간 통신 (Inter-Service Communication)

공식 문서는 명시적으로 다음과 같이 권고합니다. "서로 다른 서비스 사이에 Akka 클러스터와 액터 메시징(actor messaging)을 사용하는 것을 권장하지 않습니다." 이는 과도한 코드 결합(code coupling)을 초래하고 독립적 배포(independent deployment)를 복잡하게 만들어, 마이크로서비스의 핵심 이점을 훼손하기 때문입니다.

서비스 간 통신에는 다음을 사용하십시오.

- **동기 (Synchronous)**: Akka HTTP 또는 Akka gRPC
- **비동기 (Asynchronous)**: Akka Streams Kafka 또는 Alpakka 커넥터

이러한 메커니즘은 종단 간 백프레셔(end-to-end backpressure)를 지원하며, 양쪽 모두 Akka를 사용하거나 같은 프로그래밍 언어를 공유할 필요가 없습니다.

#### 5.2.2 서비스 내부 통신 (Intra-Service Communication)

하나의 서비스를 구성하는 클러스터 노드들 내부에서는 Akka 클러스터가 적합합니다. 그 이유는 다음과 같습니다.

- 노드들이 동일한 코드를 공유함
- 하나의 단위(unit)로 함께 배포됨
- 단일 팀이 배포를 통제함
- 롤링 배포(rolling deployment) 시 두 버전이 일시적으로 함께 동작할 수 있음

이러한 맥락에서는 액터 메시징의 편의성과 성능 이점을 유지하면서도 더 긴밀한 결합(tighter coupling)이 허용됩니다.

---

### 5.3 전통적 분산 애플리케이션 (Traditional Distributed Application)

이는 여전히 유효한 대안이며, 특히 다음 경우에 적합합니다.

- 시장 출시 시간(time-to-market)을 우선하는 스타트업
- 단일 팀(single-team) 조직
- 마이크로서비스의 복잡성이 불필요한 오버헤드를 더하는 시나리오

특징:

- 하나의 코드베이스(codebase)에서 나온 단일 배포 단위(single deployment unit)
- 하나의 클러스터 내 여러 노드에 배포됨
- 중앙집중적 통제(centralized control) 덕분에 더 긴밀한 결합이 허용됨
- 특화된 런타임 역할(front-end / back-end 노드)을 가질 수 있음

---

### 5.4 안티 패턴: 분산 모놀리스 (Anti-Pattern: Distributed Monolith)

이 문제적 구성은 다음과 같은 특징을 가집니다.

- 독립적으로 빌드/배포되는 여러 서비스
- 공유 클러스터(shared cluster), 공유 코드 의존성, 공유 데이터베이스 스키마(schema)
- 자율성으로 위장된 긴밀한 결합(tight coupling)

이 방식은 마이크로서비스의 이점은 희생하면서도 마이크로서비스의 비용은 그대로 떠안으며, 종종 중앙집중적 배포 조율(centralized deployment coordination)을 요구하고 의존성 지옥(dependency nightmare)을 만들어냅니다.

---

### 5.5 상위 수준 클러스터 도구 (Higher-Level Cluster Tools)

Akka 클러스터는 다음과 같은 특화 패턴을 지원합니다.

- **클러스터 샤딩 (Cluster Sharding)**: 액터를 클러스터 노드들에 분산합니다.
- **클러스터 싱글톤 (Cluster Singleton)**: 클러스터 전체에서 단일 인스턴스를 보장합니다.
- **분산 데이터 (Distributed Data)**: 최종 일관성(eventually-consistent) 기반의 데이터 공유를 제공합니다.
- **분산 발행-구독 (Distributed Publish-Subscribe)**: 클러스터 내 메시징 패턴을 지원합니다.
- **신뢰성 있는 전달 (Reliable Delivery)**: 메시지 전달 시맨틱(semantics)을 보장합니다.
- **샤딩된 데몬 프로세스 (Sharded Daemon Process)**: 백그라운드 프로세스를 관리합니다.

---

### 5.6 지원 인프라 (Supporting Infrastructure)

Akka 클러스터 배포는 다음 인프라의 도움을 받습니다.

- 직렬화 프레임워크 (Jackson 통합 제공)
- 리모팅(remoting) 및 원격 보안(remote security) 설정
- 네트워크 파티션을 위한 스플릿 브레인 리졸버(Split Brain Resolver)
- 멀티 노드 테스트(multi-node testing) 기능
- 협조(coordination) 메커니즘

---

## 6. 스플릿 브레인 리졸버 (Split Brain Resolver)

> 원본: https://doc.akka.io/libraries/akka-core/current/split-brain-resolver.html

### 6.1 스플릿 브레인 문제란 무엇인가 (What is the Split Brain Problem?)

스플릿 브레인 리졸버(Split Brain Resolver, SBR)는 분산 시스템의 근본적인 난제를 해결합니다. 즉, 네트워크 파티션(network partition)과 장비 크래시(machine crash)는 관측자(observer) 입장에서 구분이 불가능하다는 점입니다. 문서의 설명대로, "노드는 다른 노드에 문제가 있다는 것은 관측할 수 있지만, 그 노드가 크래시되어 영영 사용 불가능해질 것인지, 아니면 단지 네트워크 문제인지는 구분할 수 없습니다."

네트워크 파티션이 적절히 처리되지 않으면, 양쪽 구간(both sides)이 서로를 클러스터 멤버십에서 제거하여 두 개의 분리된 클러스터(separate disconnected clusters)가 만들어질 수 있습니다. 이는 클러스터 싱글톤(Cluster Singleton)이나 영속성(persistence)을 사용하는 클러스터 샤딩(Cluster Sharding)을 쓰는 시스템에서 치명적이며, 동일한 영속 액터(persistent actor)의 여러 인스턴스가 동시에 쓰기(write)를 수행하는 사태를 일으킬 수 있습니다.

---

### 6.2 SBR 활성화 (Enabling SBR)

스플릿 브레인 리졸버는 설정을 통해 활성화합니다.

```
akka.cluster.downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
```

추가로 사용할 전략(strategy)을 선택합니다.

```
akka.cluster.split-brain-resolver.active-strategy = keep-majority
```

---

### 6.3 핵심 타이밍: stable-after 설정 (Critical Timing: Stable-After Setting)

리졸버는 행동에 나서기 전에 클러스터가 안정(stable)될 때까지 기다립니다. "stable-after" 지속 시간은 클러스터 크기를 고려해야 합니다.

| 클러스터 크기 | 권장 지속 시간 |
|---|---|
| 5 노드 | 7초 |
| 10 노드 | 10초 |
| 100 노드 | 20초 |
| 1000 노드 | 30초 |

이는 일시적 네트워크 문제에 성급하게(hasty) 결정하는 것을 방지하는 한편, 진짜 장애에 대해서는 적시(timely)에 대응할 수 있도록 합니다.

---

### 6.4 다운 전략 (Downing Strategies)

#### 6.4.1 Keep Majority (다수 유지, 기본값)

이 전략은 "현재 노드가 다수(majority) 측에 속해 있다면, 도달 불가능한 노드들을 다운"시킵니다. 파티션 크기가 동일한 경우, 가장 낮은 주소(lowest-addressed)를 가진 노드가 포함된 쪽이 생존합니다. 동적으로 크기가 변하는(dynamically sized) 클러스터에 가장 적합합니다.

#### 6.4.2 Static Quorum (정적 정족수)

최소 노드 임계값(threshold)을 요구합니다. 남은 노드 수가 `quorum-size` 이상이면 도달 불가능한 노드를 다운시키고, 그렇지 않으면 도달 가능한(reachable) 노드들이 스스로 종료합니다. 고정 크기(fixed-size) 클러스터에 이상적입니다. 주의: 전체 멤버 수가 **quorum-size × 2 − 1**을 초과하면 안 됩니다.

#### 6.4.3 Keep Oldest (최고령 유지)

가장 오래된(oldest) 클러스터 멤버가 포함된 쪽을 보존합니다(보통 이곳에서 클러스터 싱글톤이 동작합니다). `down-if-alone = on`으로 설정하면, 최고령 노드가 고립(isolated)되었을 경우 스스로 다운됩니다. 이 전략은 큰 클러스터에서 소수의 노드만 보존할 가능성이 있습니다.

#### 6.4.4 Down All (전체 다운)

네트워크가 매우 불안정할 때 모든 노드를 종료시킵니다. 장애 감지가 신뢰하기 어려운(unreliable) 환경에 유용합니다. 복구하려면 재시작 후 클러스터를 다시 구성(reform)해야 합니다.

#### 6.4.5 Lease-Majority (리스 다수)

외부 분산 락(external distributed lock, 보통 Kubernetes CRD)을 사용하여 어느 파티션이 생존할지 조율(coordinate)합니다. 단 하나의 SBR 인스턴스만 리스(lease)를 획득하여 계속 동작합니다. 추가 인프라 의존성(infrastructure dependency)이라는 비용을 치르고 외부 중재(external arbitration)를 제공합니다.

---

### 6.5 간접 연결된 노드 (Indirectly Connected Nodes)

네트워크에서는 때때로 도달 불가능한 노드가 중개자(intermediaries)를 통해 간접적으로(indirectly) 연결되어 있는 상황, 즉 깔끔한 파티션(clean partition)이 아닌 상황이 발생합니다. 리졸버는 "완전히 연결된(fully connected) 노드들은 유지하고, 간접적으로만 연결된(indirectly connected) 노드들을 모두 다운"시킵니다.

과도한 다운을 방지하기 위해 다음을 설정할 수 있습니다.

```
akka.cluster.split-brain-resolver.down-all-when-indirectly-connected = 0.5
```

이 파라미터는 간접 연결로 인한 다운 결정이 지정된 비율(fraction)을 초과하게 될 경우 전체를 다운시킵니다.

---

### 6.6 불안정할 때 전체 다운 (Down All When Unstable)

도달 가능성 관측(reachability observation)이 지속적으로 변하면, 시스템은 예방 차원에서 모든 노드를 다운시킬 수 있습니다. 다음으로 설정합니다.

```
akka.cluster.split-brain-resolver.down-all-when-unstable = on
```

기본적으로 이 지속 시간은 `stable-after`의 3/4(최소 4초)와 같습니다.

---

### 6.7 페일오버 기대치 (Failover Expectations)

100개 노드 클러스터에서 기본 설정으로:

- 장애 감지(failure detection): 5초
- stable-after: 20초
- down-removal-margin: 20초
- **전체 예상 페일오버(failover): 약 45초**

더 작은 클러스터(10개 노드)에서는 `stable-after`를 10초로 줄여 약 25초의 페일오버를 달성할 수 있습니다.

---

### 6.8 코디네이티드 셧다운 (Coordinated Shutdown)

우아한 종료(graceful termination)를 활성화합니다.

```
akka.coordinated-shutdown.exit-jvm = on
```

이 설정은 노드가 다운될 때 적절히 종료되도록 보장하여, 중복 싱글톤 인스턴스(duplicate singleton instance)가 생기는 것을 방지합니다.

---

### 6.9 모듈 정보 (Module Information)

스플릿 브레인 리졸버는 `akka-cluster` 의존성에 포함되어 있습니다(예: 버전 2.10.19). JDK 11, 17, 21을 지원하며, Scala 2.13.17 및 3.3.7을 지원합니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [클러스터 사용하기 (Cluster Usage)](https://doc.akka.io/libraries/akka-core/current/typed/cluster.html)
- [클러스터 명세 (Cluster Specification)](https://doc.akka.io/libraries/akka-core/current/typed/cluster-concepts.html)
- [클러스터 멤버십 (Cluster Membership)](https://doc.akka.io/libraries/akka-core/current/typed/cluster-membership.html)
- [장애 감지기 (Failure Detector)](https://doc.akka.io/libraries/akka-core/current/typed/failure-detector.html)
- [클러스터 선택 (Choosing Akka Cluster)](https://doc.akka.io/libraries/akka-core/current/typed/choosing-cluster.html)
- [스플릿 브레인 리졸버 (Split Brain Resolver)](https://doc.akka.io/libraries/akka-core/current/split-brain-resolver.html)
