# Akka 클러스터 도구

> 이 문서는 Akka 공식 문서의 "Cluster Sharding", "Cluster Sharding Concepts", "Cluster Singleton", "Distributed Data", "Distributed Publish Subscribe", "Sharded Daemon Process", "Reliable Delivery" 섹션을 한국어로 번역한 것입니다.
> 원본: https://doc.akka.io/libraries/akka-core/current/

---

## 목차

1. [클러스터 샤딩 (Cluster Sharding)](#클러스터-샤딩-cluster-sharding)
2. [클러스터 샤딩 개념 (Cluster Sharding Concepts)](#클러스터-샤딩-개념-cluster-sharding-concepts)
3. [클러스터 싱글톤 (Cluster Singleton)](#클러스터-싱글톤-cluster-singleton)
4. [분산 데이터 (Distributed Data)](#분산-데이터-distributed-data)
5. [분산 발행-구독 (Distributed Publish Subscribe)](#분산-발행-구독-distributed-publish-subscribe)
6. [샤드 데몬 프로세스 (Sharded Daemon Process)](#샤드-데몬-프로세스-sharded-daemon-process)
7. [신뢰성 있는 전달 (Reliable Delivery)](#신뢰성-있는-전달-reliable-delivery)
8. [참고 자료](#참고-자료)

---

## 클러스터 샤딩 (Cluster Sharding)

### 개요와 목적

클러스터 샤딩(Cluster Sharding)은 액터(actor)들을 클러스터의 여러 노드(node)에 분산 배치하면서도, 이들과 상호작용할 때는 물리적 위치를 신경 쓰지 않고 **논리적 식별자(logical identifier)**만으로 다룰 수 있게 해주는 기능이다. 공식 문서의 표현을 빌리면, 샤딩은 "여러 노드에 액터를 분산시키되, 그 물리적 위치를 신경 쓰지 않고 논리적 식별자로 상호작용하고 싶을 때 유용하다."

샤딩은 주로 **상태를 가진 "엔티티(entity)" 액터**에 사용된다. 이러한 엔티티는 도메인 주도 설계(Domain-Driven Design, DDD)에서의 애그리거트 루트(aggregate root)인 경우가 많다. 상태를 가진 액터가 매우 많아 단일 머신의 자원으로는 감당하기 어려운 상황에 적합하다. 반대로 상태를 가진 액터가 소수라면, 더 단순한 클러스터 싱글톤(Cluster Singleton)을 사용하는 편이 낫다.

### 핵심 개념: 엔티티, 샤드, 리전

- **엔티티(entity)**: 식별자(identifier)를 가진 개별 액터로, 클러스터 노드 전체에 분산된다. 실제 비즈니스 로직을 처리하는 주체이다.
- **샤드(shard)**: 여러 엔티티를 하나로 묶는 그룹이다. 기본적으로 해시(hash) 기반 분산을 사용한다.
- **샤드 리전(ShardRegion)**: 메시지를 올바른 엔티티 위치로 라우팅(routing)하는 액터이며, 물리적 배치를 추상화한다.

### EntityTypeKey와 EntityRef

각 엔티티 타입은 자신이 수락하는 메시지 타입을 정의하는 `EntityTypeKey`를 가진다. `EntityRef`는 특정 엔티티에게 위치를 몰라도 메시지를 보낼 수 있는 타입 안전한(type-safe) 참조를 제공한다.

### 메시지 라우팅

엔티티에게 메시지를 보내는 방법은 다음과 같다.

- **EntityRef**: 타입 안전하며 권장되는 방식이다.
- **ShardingEnvelope**: 엔티티 ID와 메시지를 함께 감싸서(wrap) 보낸다.
- **커스텀 ShardingMessageExtractor**: 메시지에서 엔티티 ID와 샤드 ID를 추출하는 사용자 정의 구현이다.

### 설정과 초기화

클러스터 샤딩은 `ClusterSharding` 익스텐션(extension)을 통해 접근한다. 다음과 같이 초기화한다.

```
Entity.of(typeKey, ctx -> entity.create(ctx.getEntityId()))
```

`init` 메서드는 각 엔티티 타입에 대해 **모든 노드에서** 호출해야 한다. 어떤 노드가 실제 엔티티를 호스팅(hosting)할지, 아니면 프록시(proxy) 역할만 할지는 역할(role) 설정에 따라 달라진다. 비헤이비어 팩토리(behavior factory)는 노드 로컬(node-local)에서 실행되므로, 로컬 `ActorRef`와 같이 직렬화(serialize)할 수 없는 의존성을 안전하게 주입할 수 있다.

### 영속성과 상태 관리

#### 이벤트 소싱과 함께 사용

엔티티는 일반적으로 영속(persistent) 상태를 위해 `EventSourcedBehavior`를 사용한다. Akka Persistence의 **단일 작성자 원칙(single-writer principle)**은 동일한 `PersistenceId`를 가진 영속 액터가 오직 하나만 활성화되도록 보장한다. 클러스터 샤딩은 동일한 ID를 가진 엔티티가 클러스터 전체에서 한 번만 실행되도록 보장함으로써 이 원칙을 충족시킨다.

#### EntityRef 직렬화

`EntityRef`를 (메시지나 이벤트 소싱 상태 안에서) 직렬화하려면 커스텀 직렬화기(serializer)가 필요하다. 역직렬화(deserialize) 시점에 `entityId`와 `typeKey`를 조회(lookup)해야 하기 때문에, 이 정보를 직접 인코딩(encode)하는 대신 별도의 처리가 요구된다.

### 샤드 할당 전략 (Shard Allocation Strategy)

#### 기본 전략: LeastShardAllocationStrategy

기본적으로 샤드는 이전에 할당된 샤드 수가 가장 적은 `ShardRegion`에 할당된다. 설정에서 `number-of-shards`(기본값 1000)를 지정하는데, 이 값은 계획된 최대 클러스터 노드 수의 대략 **10배** 정도가 좋다. 이 값은 모든 노드에서 동일해야 하며, 변경하려면 클러스터 전체를 재시작해야 한다.

노드가 추가되면, 가장 많은 샤드를 가진 노드에서 새 노드로 샤드가 리밸런싱(rebalancing)된다. `rebalance-absolute-limit`(Akka 2.6.10+의 새 알고리즘)은 라운드(round)당 리밸런싱되는 샤드의 최대 개수를 제어하여 더 빠르게 최적의 균형에 도달하게 한다.

#### 외부 샤드 할당 (External Shard Allocation)

`ExternalShardAllocationStrategy`는 `ExternalShardAllocation`을 통해 샤드 배치를 명시적으로 제어할 수 있게 한다. 예를 들어 Kafka 파티션(partition)을 샤드와 같은 위치(co-locate)에 배치할 때 유용하다. 할당되지 않은 샤드는 기본적으로 요청한 노드에 배치되며, 명시적 할당은 `updateShardLocation`을 사용한다.

#### 같은 위치에 배치된 샤드 (Colocated Shards)

`ConsistentHashingShardAllocationStrategy`는 일관된 해싱(consistent hashing)을 사용하여 동일한 식별자를 가진 서로 다른 엔티티 타입의 샤드를 같은 위치에 배치한다. 이를 위해서는 관련된 엔티티에 대해 동일한 샤드 ID를 반환하는 커스텀 `ShardingMessageExtractor` 구현이 필요하다.

#### 커스텀 전략

애플리케이션 고유의 로직을 위해 커스텀 `ShardAllocationStrategy`를 구현할 수 있다.

### 패시베이션 (Passivation)

#### 수동 패시베이션

엔티티는 `ClusterSharding.Passivate`를 통해 스스로를 정지(stop)시킬 수 있으며, 선택적으로 `stopMessage`를 지정할 수 있다. 패시베이션 중에 도착한 메시지는 버퍼링(buffering)되었다가 엔티티의 다음 화신(incarnation)에게 전달된다. 커스텀 stop 메시지를 사용하면 종료 전에 우아한(graceful) 정리 작업을 수행할 수 있다.

#### 자동 패시베이션

자동 패시베이션 전략은 설정된 정책에 따라 엔티티를 패시베이션한다. 하위 호환성(backward compatibility)을 위해 기본적으로 비활성화되어 있다. 자동 패시베이션은 엔티티 기억하기(Remembering Entities)와 함께 사용할 수 없다.

##### 유휴 엔티티 패시베이션 (Idle Entity Passivation)

지정된 시간(기본 2분) 동안 활동이 없는 엔티티는 패시베이션된다. 다음 설정으로 구성한다.

```
akka.cluster.sharding.passivation.default-idle-strategy.idle-entity.timeout
```

##### 활성 엔티티 제한 (Active Entity Limits)

제한 기반 전략은 활성 엔티티 수가 임계값을 초과할 때 교체 정책(replacement policy)에 따라 패시베이션한다(LRU, MRU, LFU 등). 권장되는 `default-strategy`는 어드미션 윈도우(admission window)와 필터(filter)를 결합한 복합(composite) 패시베이션(Window-TinyLFU 알고리즘)을 사용한다.

**교체 정책:**

- **LRU (Least Recently Used, 최소 최근 사용)**: 가장 오랫동안 접근되지 않은 엔티티를 패시베이션한다. 최근성(recency)이 강한 패턴에 좋다.
- **Segmented LRU (분할 LRU)**: 공간을 접근 빈도에 따라 세그먼트(segment)로 나누어, 자주 접근되는 엔티티를 보호한다.
- **MRU (Most Recently Used, 최대 최근 사용)**: 가장 많이 접근된 엔티티를 패시베이션한다. 순환(cyclic) 패턴에 유용하다.
- **LFU (Least Frequently Used, 최소 빈도 사용)**: 가장 인기 없는 엔티티를 패시베이션한다. 빈도(frequency)가 중요한 워크로드에 좋다.
- **동적 에이징(Dynamic Aging)을 적용한 LFU**: 인기도 변화에 적응한다.

**복합 전략(Composite Strategy)**은 어드미션 윈도우(새로운 엔티티를 한 정책으로 추적)와 메인 영역(main area, 자리잡은 엔티티를 다른 정책으로 추적)을 결합하며, 선택적으로 빈도 스케치(frequency-sketch) 어드미션 필터를 둔다. 어드미션 윈도우 옵티마이저(optimizer)는 힐 클라이밍(hill-climbing) 기법으로 윈도우 크기를 동적으로 조정한다.

커스텀 전략은 설정 섹션을 만들고 `strategy` 설정으로 선택하여 구성한다.

### 샤딩 상태 관리

#### 상태 저장소 (State Store, ShardCoordinator 상태)

필수적으로 샤드 위치를 저장한다. ShardCoordinator가 샤드를 재배치(relocate)할 때 이 상태를 로드한다.

**분산 데이터 모드 (ddata, 기본값):**

- CRDT를 사용하며 `WriteMajorityPlus`/`ReadMajorityPlus` 일관성(consistency)을 적용한다.
- 클러스터 전반에 복제(replicate)되지만 디스크에는 영속화되지 않는다.
- 노드마다 레플리케이터(replicator)가 존재하며, 역할 기반 샤딩 사용 시 역할당 하나씩 둔다.
- 롤링 업데이트(rolling update) 중에는 역할 설정이 일관되게 유지되어야 한다.

**영속성 모드 (persistence, deprecated):**

- 분산 저널(journal)을 사용한 이벤트 소싱(Event Sourcing) 방식이다.
- 신규 프로젝트에는 권장하지 않는다.

#### 엔티티 기억하기 저장소 (Remember Entities Store)

선택적으로, 메시지를 기다리지 않고도 리밸런스/크래시 이후 엔티티를 재시작할 수 있게 한다. 이 기능을 켜면 자동 패시베이션은 비활성화된다.

**분산 데이터 모드 (ddata, 기본값):**

- 전체 클러스터 재시작 지원을 위해 LMDB를 통해 디스크에 영속화된다.
- 필요 없다면 디스크 영속화를 끌 수 있다.
- Java 17에서는 LMDB를 위한 특정 JVM 플래그가 필요하다.

**이벤트 소싱 모드 (eventsourced):**

- 이벤트 소싱을 사용하며, 영속성/스냅샷(snapshot) 플러그인 설정이 필요하다.
- 재시작 사이에 디스크 접근이 불가능한 환경에 적합하다.
- 마이그레이션을 위해 deprecated 영속성 모드의 데이터를 읽을 수 있다.

#### Deprecated 영속성 모드로부터의 마이그레이션

상태 저장소만 마이그레이션하는 경우 전체 클러스터 재시작이 필요하다. 기억된 엔티티를 다루려면 다음 중 하나를 택한다.

- ddata로 마이그레이션(전체 재시작 후 기억 정보는 손실됨)
- ddata 상태 저장소 + eventsourced 엔티티 기억하기로 마이그레이션(기억된 엔티티 보존)

기존 데이터를 읽기 위해 저널에 이벤트 어댑터(event adapter)를 설정한다.

#### 클러스터 샤딩 데이터 제거

내구성 저장소 위치(`akka.cluster.sharding.distributed-data.durable.lmdb.dir`)는 기본적으로 포트별(port-specific) 경로로 지정된다. 동적 포트(0)를 사용하면 이전 데이터가 로드되지 않는다. 명시적 경로를 설정하거나, 전체 클러스터 종료 후 엔티티를 재시작할 필요가 없다면 내구성을 비활성화한다.

### 시작 동작 (Startup Behavior)

`akka.cluster.min-nr-of-members`(또는 역할별 변형)를 사용하면 최소 개수의 리전이 등록될 때까지 샤드 할당을 지연시킬 수 있다. 이렇게 하면 첫 번째 리전에 일찍 할당된 뒤 곧바로 리밸런싱되는 현상을 방지할 수 있다.

### 헬스 체크 (Health Checks)

Akka Management과 호환되는 헬스 체크는 로컬 샤드 리전이 코디네이터(coordinator)에 등록되면 정상(healthy)을 반환한다. 자동으로 활성화되며 설정으로 비활성화할 수 있다. 특정 리전을 모니터링하려면 엔티티 타입 이름을 정의한다. 지속적인 운영 후에는 헬스 체크가 실패를 멈춘다(엔티티 타입 추가 시 Kubernetes 롤링 업데이트가 멈추는 것을 방지).

### 클러스터 샤딩 상태 점검

- **GetShardRegionState**: 한 리전의 샤드 및 엔티티 식별자를 담은 `CurrentShardRegionState`를 반환한다.
- **GetClusterShardingStats**: 클러스터 전체의 모든 리전을 조회하여, 리전별 샤드 식별자와 엔티티 개수를 담은 `ClusterShardingStats`를 반환한다.

### 주요 설정

- `akka.cluster.sharding.number-of-shards`: 전체 샤드 수(기본값 1000)
- `akka.cluster.sharding.state-store-mode`: `ddata` 또는 `persistence`
- `akka.cluster.sharding.remember-entities`: 엔티티 기억하기 활성화/비활성화
- `akka.cluster.sharding.remember-entities-store`: `ddata` 또는 `eventsourced`
- `akka.cluster.sharding.passivation.strategy`: 패시베이션 정책(`none`, `default-idle-strategy`, `default-strategy`, 커스텀)
- `akka.management.health-checks.readiness-checks.sharding`: 헬스 체크 활성화 여부

### 중요한 경고

1. **다운(downing) 전략 주의**: 클러스터를 여러 개로 쪼갤 위험이 있는 다운 전략은 피해야 한다. 분리된 서브 클러스터(sub-cluster)마다 동일한 샤드와 엔티티가 시작될 수 있다.
2. **WeaklyUp 멤버**: WeaklyUp 상태의 멤버에서는 클러스터 샤딩이 동작하지 않는다.
3. **EntityRef 직렬화**: 커스텀 직렬화기가 필요하며, 내장 지원은 없다.
4. **역할 일관성**: 역할 기반 샤딩 사용 시 롤링 업데이트 동안 역할 설정이 안정적으로 유지되어야 한다.
5. **메시지 활동만 집계**: 자동 패시베이션은 클러스터 샤딩 메시지만 집계하며, 직접 보낸 `ActorRef` 메시지는 세지 않는다.

---

## 클러스터 샤딩 개념 (Cluster Sharding Concepts)

### 핵심 구성 요소

#### ShardRegion (샤드 리전)

각 클러스터 노드에서 시작된다. 애플리케이션이 제공하는 함수를 사용하여 메시지에서 엔티티 식별자와 샤드 식별자를 추출한다. 로컬 샤드를 관리하며, 필요에 따라 메시지를 원격 리전으로 전달한다.

#### ShardCoordinator (샤드 코디네이터)

클러스터에서 가장 오래된 멤버(oldest member)에서 실행되는 싱글톤(singleton) 액터이다. 샤드의 소유권(ownership)을 결정하고 적절한 ShardRegion에 알린다. 상태는 분산 데이터(Distributed Data)를 사용해 영속적으로 유지된다.

#### Shard (샤드)

서로 관련된 엔티티들의 그룹으로, 단일 ShardRegion이 묶어서 관리한다.

#### Entity (엔티티)

클러스터 샤딩이 관리하는 개별 액터로, 실제 비즈니스 로직을 처리한다.

### 주요 개념 설명

- **메시지 흐름(Message Flow)**: 공식 문서에 따르면, "들어오는 메시지는 ShardRegion과 Shard를 거쳐 대상 Entity로 이동한다." 샤드 위치가 아직 알려지지 않은 경우, 위치가 확정될 때까지 메시지는 버퍼링된다.
- **샤드 위치(Shard Location)**: ShardCoordinator는 플러그형(pluggable) 할당 전략을 통해 모든 노드가 일관된 샤드 할당 정보를 유지하도록 보장한다.
- **리밸런싱(Rebalancing)**: 새로운 클러스터 멤버가 합류하면, 코디네이터가 샤드 이동을 조율한다. 들어오는 메시지를 버퍼링하고, 엔티티를 정지시킨 뒤, 새로운 보금자리(home)로 재할당한다. 복구를 위해 상태는 영속적이어야 한다.
- **상태 영속성(State Persistence)**: ShardCoordinator 상태는 분산 데이터를 통해 장애를 견딘다.
- **메시지 순서(Message Ordering)**: 동일한 ShardRegion 송신자를 사용할 때 순서가 보존되며, 최대 한 번(at-most-once) 전달 시맨틱(semantics)을 가진다.
- **신뢰성 있는 전달(Reliable Delivery)**: 최소 한 번(at-least-once) 시맨틱이 필요하면 Reliable Delivery 기능을 통해 사용할 수 있다.
- **성능(Performance)**: 지연(latency)은 코디네이터 조회가 필요한 미지의 샤드에 대해서만 발생한다. 이후 메시지는 직접 라우팅된다.

---

## 클러스터 싱글톤 (Cluster Singleton)

### 목적과 개요

클러스터 싱글톤(Cluster Singleton)은 클러스터 전체에서 액터 인스턴스가 **정확히 하나만** 실행되도록 보장한다. 사용 사례는 다음과 같다.

- 클러스터 전역의 조율 및 의사 결정
- 외부 시스템에 대한 단일 진입점(single entry point)
- 마스터-워커(master-worker) 패턴
- 중앙 집중식 네이밍(naming) 또는 라우팅 서비스

### 치명적인 경고

**스플릿 브레인(Split Brain) 위험**: 공식 문서는 경고한다. "네트워크 문제나 시스템 과부하(긴 GC 정지 등) 시 클러스터를 여러 개의 분리된 클러스터로 쪼갤 수 있는 다운(downing) 전략은 사용하지 마라. 그렇게 되면 분리된 클러스터마다 하나씩, **여러 개의 싱글톤**이 시작되어 버린다!"

### 잠재적인 문제점

이 패턴에는 무시할 수 없는 단점이 있다.

- 클러스터 작업에 대한 단일 성능 병목(bottleneck)이 된다.
- 무중단(non-stop) 가용성을 보장할 수 없다. 노드 장애 후 마이그레이션에는 수 초가 걸린다.
- 모든 싱글톤이 가장 오래된 노드에 집중되어 자원 경합(resource contention)을 일으킬 수 있다.
- 많은 시나리오에서는 클러스터 샤딩(Cluster Sharding) 같은 더 나은 대안이 바람직하다.

### 아키텍처 구성 요소

- **싱글톤 매니저(Singleton Manager)**: `ClusterSingleton.init()`을 통해 모든 클러스터 노드(또는 역할이 지정된 노드)에서 시작되며, 가장 오래된 멤버에서 실행되는 하나의 액터 인스턴스를 관리한다. 매니저는 어느 시점에도 인스턴스가 최대 하나만 존재하도록 보장한다.
- **싱글톤 프록시(Singleton Proxy)**: `init()`이 반환하는 `ActorRef`로, 위치와 무관하게 현재 싱글톤 인스턴스로 메시지를 라우팅한다. 싱글톤이 일시적으로 사용 불가능할 때 메시지를 버퍼링한다(버퍼 크기 설정 가능, 기본 1000개).

### 구현 단계

**1. 액터 비헤이비어 정의**
표준 `Behavior` 구현을 만든다. 예시에서는 증가(increment) 명령과 값 조회를 받는 카운터(counter)를 보여준다.

**2. 싱글톤 초기화**
각 노드에서 `ClusterSingleton(system).init(SingletonActor(...))`을 호출한다. 이는 메시징을 위한 프록시 `ActorRef`를 반환한다.

**3. 메시지 전송**
모든 메시지는 반환된 프록시를 통해 보낸다. 프록시는 자동으로 활성 싱글톤을 찾아 전달한다.

### 슈퍼비전 설정 (Supervision)

기본 정지 동작을 슈퍼비전 전략(supervision strategy)으로 재정의할 수 있다.

- `SupervisorStrategy.restart`: 실패 시 즉시 재시작
- `SupervisorStrategy.restartWithBackoff(min, max, randomFactor)`: 빠른 반복을 막는 지연 재시작

예시:

```
Behaviors.supervise(Counter()).onFailure[Exception](SupervisorStrategy.restart)
```

### 애플리케이션 고유의 정지 메시지

`stopMessage`를 지정하여 우아한 종료(graceful shutdown)를 구성할 수 있다.

```
SingletonActor(behavior, "name").withStopMessage(ShutdownCommand)
```

이 메시지는 종료 직전에 전송되어, 핸드오버(handover)가 완료되기 전에 비동기 정리 및 자원 정리를 수행할 수 있게 한다.

### 리스(Lease) 설정

분산 리스(distributed lease)를 사용하여 스플릿 브레인 시나리오에 대한 추가 안전장치를 둘 수 있다.

- **전역 설정**: `akka.cluster.singleton.use-lease`를 구성하면 클러스터 전체에 리스를 적용한다.
- **싱글톤별 설정**: 커스텀 리스 설정 블록을 정의하고 `ClusterSingletonSettings.withLeaseSettings(LeaseUsageSettings(...))`로 적용한다.

설정된 리스를 성공적으로 획득하지 못하면 싱글톤은 시작되지 않는다. 리스를 잃으면 액터는 종료되고, 재시작 전에 다시 획득한다.

### 설정 파라미터

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

### 데이터 센터 간 싱글톤 접근

이 기능은 계획되어 있으나 아직 미완성이다(TODO #27705로 표시됨).

### 메시지 전달 보장

프록시가 버퍼링을 하더라도, 분산 시스템 특성상 메시지가 유실될 수 있다. 신뢰성 있는 전달 시맨틱을 위해서는 애플리케이션 수준의 확인(acknowledgment) 및 재시도(retry) 로직을 구현해야 한다.

---

## 분산 데이터 (Distributed Data)

### 목적과 핵심 개념

Akka 분산 데이터(Distributed Data)는 키-값(key-value) 저장소 인터페이스를 사용하여 Akka 클러스터의 노드 간에 데이터를 공유할 수 있게 해준다. 이 시스템은 **충돌 없는 복제 데이터 타입(Conflict-Free Replicated Data Types, CRDT)**을 구현하므로, 조율(coordination) 없이도 어떤 노드에서든 업데이트가 가능하다. 공식 문서에 따르면, 데이터는 "최종적으로 일관되며(eventually consistent), 낮은 지연(latency)으로 높은 읽기/쓰기 가용성(파티션 내성, partition tolerance)을 제공하도록 설계되었다."

### 레플리케이터 패턴 (Replicator Pattern)

데이터와의 상호작용은 `DistributedData` 익스텐션을 통해 접근하는 레플리케이터(Replicator) 액터를 거쳐 이루어진다. 이 액터는 세 가지 주요 연산을 관리한다.

- **Update**: `Replicator.Update` 메시지를 보내 데이터를 수정한다. 다섯 가지 구성 요소를 포함한다 — 키 식별자, 초기 빈 상태, 쓰기 일관성 수준, 응답 주소, 순수(pure) 수정 함수. 자신의 쓰기는 즉시 보이도록 보장된다.
- **Get**: `Replicator.Get`으로 지정된 읽기 일관성 수준에 따라 현재 값을 조회한다. 업데이트와 마찬가지로 자신의 쓰기에 대한 읽기는 항상 최신이지만, 응답 메시지 순서는 보장되지 않는다.
- **Subscribe**: 액터는 특정 키 변경 알림을 구독(subscribe)하여 `Replicator.Changed` 메시지를 받을 수 있다. 와일드카드(wildcard)를 사용한 접두사(prefix) 매칭을 지원한다.

### 일관성 모델 (Consistency Models)

#### 쓰기 일관성 수준

- **WriteLocal**: 로컬 복제본에 즉시 쓰고, 가십(gossip) 프로토콜을 통해 수 초에 걸쳐 전파한다.
- **WriteTo(n)**: 로컬 복제본을 포함해 최소 n개의 복제본에 쓴다.
- **WriteMajority**: N/2 + 1개의 복제본에 쓴다. 작은 클러스터를 위한 minCap 파라미터를 포함한다.
- **WriteMajorityPlus**: 과반수(majority)에 추가로 지정된 노드 수만큼 더 쓰며, 빠져나가는(exiting) 노드는 제외한다.
- **WriteAll**: 빠져나가는 노드를 제외한 모든 클러스터 노드에 쓴다.

#### 읽기 일관성 수준

- **ReadLocal**: 로컬 복제본만 읽는다(GetFailure가 발생하지 않음).
- **ReadFrom(n)**: n개의 복제본에서 읽고 병합(merge)한다.
- **ReadMajority**: minCap을 지원하며 N/2 + 1개의 복제본에서 읽는다.
- **ReadMajorityPlus**: 과반수에 추가 노드를 더해 읽으며, 빠져나가는 노드는 제외한다.
- **ReadAll**: 빠져나가는 노드를 제외한 모든 노드에서 읽는다.

문서는 강한 일관성(strong consistency)을 위한 다음 공식을 강조한다: **`(쓰인 노드 수 + 읽은 노드 수) > N`** (여기서 N은 전체 클러스터 노드 수).

### 복제 데이터 타입 (Replicated Data Types)

#### 카운터 (Counters)

- **GCounter (grow-only, 증가 전용)**: 증가만 지원한다. 노드마다 하나의 카운터를 유지하며, 전체 값은 모든 노드 카운터의 합이다.
- **PNCounter (positive/negative, 양수/음수)**: 내부적으로 증가(P)와 감소(N) 카운터를 별도로 추적한다. 최종 값 = P - N.
- **PNCounterMap**: 여러 관련 카운터를 단일 복제 단위 안에서 관리하며, 원자적(atomic) 동시 복제를 보장한다.

#### 집합 (Sets)

- **GSet (grow-only, 증가 전용)**: 제거 없이 요소만 추가한다. 병합 연산은 집합 합집합(union)을 계산한다.
- **ORSet (observed-remove, 관찰-제거)**: 추가와 제거를 모두 지원한다. 인과관계(causality) 추적을 위해 버전 벡터(version vector)와 "출생 점(birth dots)"을 구현한다. 동일 요소가 동시에 추가/제거되면 **추가가 이긴다(add wins)**.

#### 맵 (Maps)

- **ORMap (observed-remove)**: 임의의 키 타입과 ReplicatedData 값을 가지는 범용 맵이다. 동일 키에 대한 동시 업데이트는 값 병합을 유발한다.
- **ORMultiMap**: ORSet 값을 감싸는 특화된 ORMap으로, 일대다(one-to-many) 관계를 가능하게 한다.
- **PNCounterMap**: PNCounter 값을 사용하는 이름 붙은 카운터들의 맵이다.
- **LWWMap**: 최종 쓰기 우선(last-writer-wins) 레지스터 값을 사용하는 특화된 ORMap으로, 동기화된 시계(synchronized clock)에 의존한다.

#### 레지스터와 플래그

- **Flag**: false로 초기화되는 불리언(boolean) 값으로, 한 번 true로 전환되면 영구적이다. 병합 시 true가 이긴다.
- **LWWRegister**: 직렬화 가능한 값을 담는다. 병합 시 타임스탬프가 가장 높은 값을 선택한다. 동률(tie)이면 가장 낮은 주소(address)의 노드를 사용한다. 문서는 경고한다 — "시계 오차(clock skew) 범위 내에서 발생하는 동시 업데이트에 대해 값의 선택이 중요하지 않을 때만 사용해야 한다."

### 델타-CRDT 지원 (Delta-CRDT)

전체 엔트리(entry) 대신 상태 변경분(state changes)만 전송하여 복제를 최적화한다. 일부 타입은 인과적 전달(`RequiresCausalDeliveryOfDeltas`)을 요구하고, 다른 타입은 최종 일관성을 제공한다. 전체 상태 복제는 주기적으로, 또는 클러스터 변경 시, 또는 네트워크 파티션 이후에 일어난다.

### 추가 연산

- **Delete**: `Replicator.Delete`를 통해 데이터 엔트리를 제거한다. 삭제된 키는 재사용할 수 없지만 복제 오버헤드를 줄인다. 이후 연산은 `DataDeleted` 응답을 받는다.
- **Expire**: 설정된 비활성(inactivity) 기간 후 자동 제거한다. 설정 예시:

```
akka.cluster.distributed-data.expire-keys-after-inactivity {
  "key-1" = 10 minutes
  "cache-*" = 2 minutes
}
```

만료된 엔트리는 톰스톤(tombstone)을 남기지 않으며 키를 재사용할 수 있다.

### 커스텀 데이터 타입 구현

커스텀 타입을 구현하려면 `ReplicatedData`를 확장하고, 단조 수렴(monotonic convergence)을 보장하는 `merge` 함수를 제공해야 한다. 문서에 따르면 "데이터 타입은 불변(immutable)이어야 하며, 즉 '수정' 메서드는 새 인스턴스를 반환해야 한다."

직렬화가 중요하다. 커스텀 타입은 효율적인 Akka 직렬화기를 필요로 하며, 이상적으로는 Protobuf를 사용한다. 집합과 맵의 요소는 동일한 SHA-1 다이제스트(digest)를 갖도록 결정적(deterministic)으로 직렬화되어야 한다.

### 내구성 저장소 (Durable Storage)

설정으로 LMDB를 사용해 디스크에 영속화할 수 있다.

```
akka.cluster.distributed-data.durable.keys = ["a", "b", "durable*"]
```

원래 클러스터의 노드가 새 클러스터에 적어도 하나 참여하면 재시작 후에도 데이터가 살아남는다. 설정 옵션은 다음과 같다.

- **write-behind-interval**: 플러시(flush) 전에 변경분을 모은다(기본값: off, 즉 즉시 쓰기).
- **map-size**: 메모리 맵 파일 크기(기본값 100 MiB).
- **dir**: 저장 위치.

Java 17에서는 추가 JVM 플래그가 필요하다: `--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED`

### 제한 사항과 고려 사항

이 시스템은 "빅데이터(Big Data)" 시나리오를 위해 설계되지 않았다. 주요 제약은 다음과 같다.

- 최상위(top-level) 엔트리 최대 약 **100,000개** 권장
- 모든 데이터는 메모리에 보관됨
- 큰 개별 엔트리는 과도하게 큰 원격 메시지를 만든다
- 새로운 클러스터 노드는 가십 전송을 받으며, 완전한 동기화에 수십 초가 걸린다

#### CRDT 가비지와 프루닝 (Pruning)

CRDT는 이력(history), 즉 가비지(garbage)를 누적한다. 특히 GCounter는 노드별 카운터를 무기한 유지한다. 레플리케이터는 `RemovedNodePruning` 메커니즘을 통해 제거된 클러스터 노드와 관련된 데이터를 자동으로 프루닝한다. 프루닝 마커(marker)는 설정된 기간(`pruning-marker-time-to-live`) 동안 유지되어, 네트워크 파티션 이후 손상된 데이터가 다시 주입되는 것을 방지한다.

### 학습 자료

- 영상: Mark Shapiro의 "Strong Eventual Consistency and Conflict-free Replicated Data Types"
- 논문: Shapiro 외, "A Comprehensive Study of Convergent and Commutative Replicated Data Types"

### 주요 설정 속성

- **gossip-interval**: 2초(가십 전파 주기)
- **notify-subscribers-interval**: 500ms(구독자 알림 주기)
- **pruning-interval**: 120초(제거된 노드 데이터 프루닝 점검)
- **max-pruning-dissemination**: 300초(모든 복제본 전파 예상 시간)
- **pruning-marker-time-to-live**: 6시간(비내구성 데이터 마커 유지)
- **prefer-oldest**: off(가장 오래된 노드로의 라우팅 선호)

---

## 분산 발행-구독 (Distributed Publish Subscribe)

### 개요

Akka의 분산 발행-구독(pub/sub) 시스템은 토픽(topic) 액터를 사용하여 클러스터 노드 전반에 걸친 메시징을 가능하게 한다. 이 기능은 `akka-cluster-typed`에 포함되어 있으며, 각 토픽을 구독과 메시지 분배를 관리하는 하나의 액터로 표현하는 방식으로 동작한다.

### 모듈 정보

이 기능은 `akka-cluster-typed` 의존성을 필요로 한다(문서 기준 버전 2.10.19). 토픽은 클러스터 환경에 배포되었을 때만 노드 간에 메시지를 분배한다.

### 토픽 레지스트리와 조회

토픽은 `PubSub` 레지스트리(registry)를 통해 관리된다. 레지스트리는 각 액터 시스템마다 이름이 붙은 토픽당 하나의 액터만 존재하도록 보장한다. 아직 시작되지 않은 토픽을 요청하면 레지스트리가 이를 생성하여 반환한다. 이미 활성 상태라면 기존 참조를 제공한다.

레지스트리는 동일한 이름을 가지면서 메시지 타입이 다른 여러 토픽 액터가 공존하는 것을 막는다. 이를 시도하면 예외(exception)가 발생한다.

### 구독 관리

로컬 액터는 특정 명령을 통해 토픽과 상호작용한다.

- **구독(Subscribing)**: 액터는 토픽 액터에게 구독 메시지를 보낸다.
- **구독 해지(Unsubscribing)**: 대응되는 구독 해지 명령으로 액터를 토픽에서 제거한다.

문서는 다음을 명시한다 — "토픽이 시작되어 있고 등록된 로컬 구독자가 있는 경우에만, 발행된 메시지가 다른 노드로 전달된다."

### 메시지 발행

발행자(publisher)는 메시지를 토픽 액터에게 직접 보낸다. 시스템은 노드 수준에서 메시지를 중복 제거(deduplicate)한다 — 로컬에 여러 구독자가 있더라도, 발행된 메시지는 원격 노드당 네트워크를 한 번만 횡단한다.

### TTL(Time-to-Live) 설정

토픽에 TTL 파라미터를 설정할 수 있다. 지정된 기간 동안 토픽 액터에 로컬 구독자나 메시지 활동이 없으면, 토픽은 자동으로 정지되고 레지스트리에서 제거된다.

### 수동 토픽 생성

레지스트리 기반 조회 외에도, 토픽을 액터로 직접 스폰(spawn)할 수 있다. 이 방식은 단일 노드에 동일 토픽에 대한 여러 토픽 액터를 둘 수 있게 하며, 각각은 원격 노드에서 발행된 메시지의 사본을 별도로 받는다.

### 확장성 특성 (Scalability)

각 토픽은 하나의 `Receptionist` 서비스 키(service key)를 사용하므로, 실용적인 배포는 수천 개에서 수만 개의 고유 토픽으로 제한된다. 고유 토픽의 회전율(turnover)이 높으면 성능이 나빠지며, 커스텀 솔루션이 필요할 수 있다. 토픽 액터는 프록시로 동작하며, 구독자 중복 제거와 리셉셔니스트(receptionist) 등록 관리를 담당한다.

### 전달 보장 (Delivery Guarantees)

이 시스템은 **최대 한 번(at-most-once) 전달 시맨틱**을 제공한다 — 전송 중에 메시지가 유실될 수 있다. 또한 구독자 레지스트리는 최종 일관성(eventual consistency)을 달성하므로, 한 노드의 새 구독자가 다른 노드의 발행자에게 알려지기까지 짧은 지연이 발생한다.

더 강한 보장이 필요한 애플리케이션에는 최소 한 번(at-least-once) 전달을 위해 Alpakka Kafka 사용을 권장한다.

### 주요 제한 사항

- 최대 수천 개의 토픽(더 많이 필요하면 커스텀 솔루션 필요)
- 최종 일관적인 구독자 탐색
- 최대 한 번 메시지 전달
- 고빈도 토픽 생성/소멸 패턴에는 부적합

---

## 샤드 데몬 프로세스 (Sharded Daemon Process)

### 목적과 소개

샤드 데몬 프로세스(Sharded Daemon Process) 기능은 "0부터 시작하는 숫자 ID를 각각 부여받은 **N개의 액터**를 실행하고, 이들을 클러스터 전반에 걸쳐 살아 있게(keep alive) 유지하며 균형 있게(balanced) 배치하는 방법"을 제공한다. 이 도구는 분산 데이터 처리 워크로드를 정해진 수의 워커(worker)들에게 나누어 맡겨야 하는 시나리오에 적합하다.

대표적인 사용 사례는 CQRS 애플리케이션에서 이벤트 스트림(event stream)으로부터 프로젝션(projection)을 만드는 것이다. 이벤트에 태그(tag)를 붙이고(N개의 태그 중 하나), 이를 통해 처리 책임을 N개의 워커에게 분배하여 각 워커가 데이터의 일부를 처리하게 한다.

### 핵심 기능

**동작 방식:**

- 클러스터 싱글톤(Cluster Singleton)이 킵얼라이브(keep-alive) 메시지를 트리거하여 클러스터 전반의 액터를 유지한다.
- 리밸런싱이 필요해지면, 킵얼라이브 신호에 기반하여 액터가 정지되고 재시작된다.
- 단일 액터 시나리오라면 샤드 데몬 프로세스 대신 클러스터 싱글톤을 권장한다.

### 기본 설정과 초기화

모든 클러스터 노드에서 동일하게 프로세스를 초기화한다.

```
val tags = Vector("tag-1", "tag-2", "tag-3")
ShardedDaemonProcess(system).init("TagProcessors", tags.size, id => TagProcessor(tags(id)))
```

Java에서는 유사한 파라미터로 `ShardedDaemonProcess.get(system).init()`을 사용한다. 추가 팩토리 메서드는 우아한 정지 메시지와 향상된 설정을 지원한다.

### 프로세스 주소 지정 (Addressing the Processes)

데몬 프로세스 액터와의 통신은 "시스템 리셉셔니스트(system receptionist)"에 의존한다. 브로드캐스트(broadcast)를 위한 단일 `ServiceKey`를 사용하거나, 표적(targeted) 메시징을 위한 개별 키를 사용한다.

### 동적 스케일링 (Dynamic Scaling)

`initWithContext` 메서드는 `ChangeNumberOfProcesses` 명령을 받는 `ActorRef[ShardedDaemonProcessCommand]`를 반환하여 런타임(runtime) 재조정(rescaling)을 가능하게 한다. 이 작업은 액터들이 우아한 종료 절차를 수행하므로 오래 걸릴 수 있다. 동시(concurrent) 재조정 요청은 실패 응답을 반환한다.

**롤링 업그레이드**: 정적(static) 워커 수에서 동적(dynamic) 워커 수로의 업그레이드는 지원된다. 단, 동적에서 정적으로의 업그레이드는 전체 클러스터 종료를 요구한다.

### 프로세스를 같은 위치에 배치하기 (Colocating Processes)

`ConsistentHashingShardAllocationStrategy`를 사용하면 인덱스(index)를 공유하는 프로세스들을 노드 전반에서 같은 위치에 배치할 수 있어, 자원 공유 시나리오에 유리하다. 각 데몬 프로세스 이름은 자체 전략 인스턴스를 필요로 하며, 전략을 공유할 수 없다.

### 확장성 한계

이 도구는 "최대 수천 개의 프로세스(up to thousands of processes)"까지의 배포에 적합하다. 이 범위를 넘어서면 Akka 분산 데이터(Distributed Data) 복제나 킵얼라이브 메시징 시스템에 문제가 발생할 수 있다.

### 설정 항목

주요 설정 속성은 다음과 같다.

- **keep-alive-interval**: `10s`(핑(ping) 주기)
- **keep-alive-from-number-of-nodes**: `3`(핑을 시작하는 노드 수)
- **keep-alive-throttle-interval**: `100 ms`(메시지 간 지연)

커스텀 `ShardedDaemonProcessSettings`로 역할 제한을 포함한 클러스터 샤딩 기본값을 재정의할 수 있다.

---

## 신뢰성 있는 전달 (Reliable Delivery)

### 목적과 핵심 개념

신뢰성 있는 전달(Reliable Delivery)은 분산 시스템의 근본적 과제, 즉 액터 간에 메시지가 유실되지 않도록 보장하는 문제를 다룬다. 표준 메시지 전달은 "최대 한 번(at-most-once)" 시맨틱을 제공하므로 메시지가 유실될 수 있다. 신뢰성 있는 전달 프레임워크는 "최소 한 번(at-least-once)" 또는 "사실상 한 번(effectively-once)" 전달을 구현하기 위한 도구를 제공한다. 처리가 진정으로 완료된 시점은 애플리케이션 계층만 알 수 있으므로, 발행자(producer)와 소비자(consumer) 액터의 협력이 필요하다.

### 전달 보장 (Delivery Guarantees)

- **사실상 한 번 처리(Effectively-Once)**: 발행자와 소비자 모두 크래시(crash)하지 않으면, 메시지는 유실이나 중복 없이 순서대로 도착하며 비즈니스 수준의 중복 제거가 필요 없다.
- **내구성 있는 최소 한 번(At-Least-Once with Durability)**: `EventSourcedProducerQueue`를 통한 내구성 큐(durable queue)가 활성화되면, 미확인(unconfirmed) 메시지가 크래시를 견디고 재전달(redeliver)된다. 즉, 발행자가 재시작되면 일부 메시지가 두 번 처리될 수 있으므로, 멱등(idempotent) 소비나 비즈니스 수준의 중복 제거가 필요하다.
- **흐름 제어 메커니즘(Flow Control)**: 프레임워크는 수요 기반(demand-driven) 전송을 강제하여 빠른 발행자가 느린 소비자를 압도하거나 네트워크 용량을 고갈시키는 것을 막는다 — 발행자는 소비자가 더 많은 작업을 요청할 때만 전송한다.

### 지원하는 세 가지 패턴

#### 점대점 패턴 (Point-to-Point)

단일 발행자와 단일 소비자 사이에 신뢰성 있는 전달을 확립한다. `ProducerController`와 `ConsumerController`가 메시지 래핑(wrapping), 확인(acknowledgment) 추적, 재전송(resend) 로직을 처리한다.

**흐름**: 발행자가 `ProducerController`에 `Start` 메시지를 보냄 → `RequestNext`를 받음 → 메시지 하나를 보냄 → 다음 `RequestNext`를 기다림. 마찬가지로 소비자는 `ConsumerController`에 `Start`를 보내고, `Delivery`로 감싸진 메시지를 받은 뒤 `Confirmed`로 응답한다.

**핵심 제약**: 발행자와 `ProducerController`는 같은 위치(colocal)에 있어야 한다(런타임 검사로 강제됨). 성능과 전달 보장을 위해서이다. 소비자와 `ConsumerController`에도 동일하게 적용된다.

**연결(Connection)**: 컨트롤러는 `ConsumerController.RegisterToProducerController` 또는 `ProducerController.RegisterConsumer`를 통해 연결되어야 한다. 애플리케이션이 이 연결을 관리하며, 보통 `Receptionist`나 일반 메시지 전달을 사용한다.

**메시지 흐름**: 미확인 메시지는 버퍼링되며, 흐름 제어 윈도우(flow control window)에 의해 제한된다. 소비자 측이 수요를 주도하며, `ProducerController`는 요청된 비율을 초과하지 않는다.

#### 워크 풀링 패턴 (Work Pulling)

여러 워커 액터가 공유된 작업 관리자(work manager)로부터 각자의 속도로 동적으로 작업을 끌어온다(pull). 점대점과 달리 메시지 순서는 중요하지 않으며, 각 메시지는 가용한 워커에게 무작위로(randomly) 라우팅된다.

**동적 등록**: 워커는 `ServiceKey`를 사용해 `Receptionist`에 자신을 등록한다. `WorkPullingProducerController`는 이 키를 구독하여 활성 워커를 자동으로 발견한다.

**스케일링**: 워커는 발행자를 명시적으로 재설정하지 않고도 어느 클러스터 노드에서든 동적으로 추가/제거될 수 있다.

**버퍼링**: 수요가 있던 모든 워커가 `RequestNext` 이후, 메시지 전송 이전에 등록을 해지하면, 새 워커가 도착하거나 수요가 돌아올 때까지 메시지가 버퍼링된다.

**로컬 요건**: 발행자와 `WorkPullingProducerController`는 같은 위치에 있어야 한다(런타임 검사로 강제됨).

#### 샤딩 패턴 (Sharding)

신뢰성 있는 전달은 클러스터 샤딩(Cluster Sharding)과 통합되어, 발행자에서 샤딩된 소비자로의 시나리오를 지원한다. 노드/시스템당 하나의 `ShardingProducerController`가 `entityId`로 식별되는 여러 샤딩 엔티티로 메시지를 보낸다.

**구성 요소**: `ShardingProducerController`가 각 샤딩 엔티티를 감싸는 `ShardingConsumerController`로 메시지를 보낸다. 둘 사이에 명시적 등록은 필요 없다.

**선택적 전송**: `RequestNext`는 수요가 있는 엔티티 정보를 포함한다. 발행자는 현재 수요가 없는 엔티티(메시지가 버퍼링됨)나, 심지어 완전히 새로운 엔티티로도 보낼 수 있다.

**로컬 요건**: 발행자와 `ShardingProducerController`는 같은 위치에 있어야 한다.

**다중 발행자**: 서로 다른 노드가 동일한 샤딩 엔티티 타입을 사용하는 별개의 발행자를 호스팅할 수 있으며, 각각 고유한 `producerId`로 식별된다.

### 발행자 및 소비자 컨트롤러

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

두 컨트롤러는 각자의 비즈니스 액터와 같은 위치에 있어야 하며, 컨트롤러 프로토콜과 비즈니스 프로토콜 간 매핑을 위해 메시지 어댑터(message adapter)를 통해 통신한다.

### 흐름 제어와 확인

흐름 제어는 소비자 주도(consumer-driven) 방식이다. `ConsumerController`가 `RequestNext` 메시지로 용량(capacity)을 요청한다. 발행자는 이 요청을 존중하여 메일박스(mailbox)의 큐 포화(saturation)를 방지한다.

**확인 패턴**: 소비자는 `Delivery(message, confirmTo)`를 받아 메시지를 처리한 뒤, `confirmTo`로 `ConsumerController.Confirmed`를 보낸다. 확인이 도착하기 전까지는 다음 메시지가 전달되지 않는다.

**윈도우 크기(Window Size)**: 여러 미확인 메시지가 동시에 전송 중(in flight)일 수 있으나, 설정 가능한 흐름 제어 윈도우로 제한된다.

### 큰 메시지 청킹 (Chunking Large Messages)

점대점 패턴에서, `akka.reliable-delivery.producer-controller.chunk-large-messages`(바이트 단위)를 초과하는 메시지는 `ProducerController`에 의해 자동으로 작은 조각(piece)으로 분할된다. `ConsumerController`가 이를 투명하게(transparently) 재조립한다.

**이점**: 큰 메시지의 직렬화가 작은 메시지를 지연시키는 헤드 오브 라인 블로킹(head-of-line blocking)을 방지한다. 직렬화는 전송 계층(transport layer)이 아닌 컨트롤러에서 이루어진다.

**내구성 큐와의 상호작용**: 내구성 큐가 활성화되면, 영속화 전에 재조립하지 않고 청크(chunk)를 개별적으로 저장한다.

**제한**: 현재 점대점 패턴에서만 지원된다.

### 내구성 발행자 큐 (Durable Producer Queue)

`EventSourcedProducerQueue`(`akka-persistence-typed` 소속)는 이벤트 소싱을 사용해 미확인 메시지를 영속화한다. 발행자 JVM이 크래시하면, 동일한 `PersistenceId`로 재시작 시 메시지가 재전달된다.

**성능 비용**: 내구성은 상당한 오버헤드를 추가한다. 메시지마다 영속화 연산이 수반된다.

**고유 식별자**: `PersistenceId`는 발행자마다 전역적으로 고유해야 한다. 클러스터 싱글톤이나, 네이밍 규칙을 갖춘 노드별 발행자가 이를 보장한다. 동일한 ID를 여러 발행자 인스턴스에서 동시에 사용해서는 안 된다.

**전달 시맨틱**: 내구성 큐를 사용하면, 확인이 아직 저장되지 않은 상태에서 크래시가 나면 이미 처리된 메시지가 재전달될 수 있다 — 즉 최소 한 번(at-least-once) 전달을 구현한다.

### Ask 기반 확인 (Ask-Based Confirmation)

`sendNextTo`를 통한 `tell` 대신, 발행자는 `askNextTo`와 함께 `context.ask`를 사용하여 메시지를 `MessageWithConfirmation`으로 감쌀 수 있다. 응답은 (내구성 큐가 있으면) 성공적인 영속화를, (내구성이 없으면) 소비자의 완전한 처리를 확인해 준다.

### 설정

신뢰성 있는 전달 설정은 `akka.reliable-delivery` 네임스페이스(namespace)에 위치하며, 다음 세부 섹션을 가진다.

- `producer-controller`: 청크 크기, 흐름 제어 윈도우, 재전송 주기
- `consumer-controller`: only-flow-control 모드, 흐름 제어 윈도우
- `work-pulling-producer-controller`: 수요 라우팅 동작
- `sharding-producer-controller`: 엔티티 버퍼링 제한

참조 설정(reference configuration)은 actor-typed, persistence-typed, cluster-sharding-typed 모듈에서 제공된다. 운영자는 네트워크 상태, 메시지 크기, 지연 요건에 맞게 이를 튜닝(tune)한다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Cluster Sharding](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharding.html)
- [Cluster Sharding Concepts](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharding-concepts.html)
- [Cluster Singleton](https://doc.akka.io/libraries/akka-core/current/typed/cluster-singleton.html)
- [Distributed Data](https://doc.akka.io/libraries/akka-core/current/typed/distributed-data.html)
- [Distributed Publish Subscribe](https://doc.akka.io/libraries/akka-core/current/typed/distributed-pub-sub.html)
- [Sharded Daemon Process](https://doc.akka.io/libraries/akka-core/current/typed/cluster-sharded-daemon-process.html)
- [Reliable Delivery](https://doc.akka.io/libraries/akka-core/current/typed/reliable-delivery.html)
