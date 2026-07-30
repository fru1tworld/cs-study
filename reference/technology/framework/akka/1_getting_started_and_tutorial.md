# Akka 시작하기와 튜토리얼

## Akka 시작하기와 일반 개념

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

### 목차

1. [Akka 라이브러리 소개](#1-akka-라이브러리-소개)
2. [현대 시스템이 새로운 프로그래밍 모델을 필요로 하는 이유](#2-현대-시스템이-새로운-프로그래밍-모델을-필요로-하는-이유)
3. [액터 모델이 현대 분산 시스템의 요구를 충족하는 방법](#3-액터-모델이-현대-분산-시스템의-요구를-충족하는-방법)
4. [Akka 라이브러리와 모듈 개요](#4-akka-라이브러리와-모듈-개요)
5. [용어와 개념(Terminology and Concepts)](#5-용어와-개념terminology-and-concepts)
6. [액터 시스템(Actor Systems)](#6-액터-시스템actor-systems)
7. [액터란 무엇인가(What is an Actor)](#7-액터란-무엇인가what-is-an-actor)
8. [슈퍼비전과 모니터링(Supervision and Monitoring)](#8-슈퍼비전과-모니터링supervision-and-monitoring)
9. [액터 참조, 경로, 주소(Actor References, Paths and Addresses)](#9-액터-참조-경로-주소actor-references-paths-and-addresses)
10. [메시지 전달 신뢰성(Message Delivery Reliability)](#10-메시지-전달-신뢰성message-delivery-reliability)
11. [설정(Configuration)](#11-설정configuration)
12. [참고 자료](#12-참고-자료)

---

### 1. Akka 라이브러리 소개

Akka는 "프로세서 코어와 네트워크에 걸쳐 동작하는 확장 가능하고(scalable) 복원력 있는(resilient) 시스템을 설계하기 위한 라이브러리 집합(a set of libraries)"입니다. Akka는 개발자가 저수준의 신뢰성 관련 코드를 직접 구현하는 대신, 비즈니스 목표(business objectives)에 집중할 수 있게 해 줍니다.

#### 현대 분산 시스템이 직면하는 핵심 과제

현대 분산 시스템(distributed system)은 본질적인 장애 요소를 안고 있습니다.

- 구성 요소(component)가 별도의 통지 없이 실패(failure)할 수 있습니다.
- 메시지(message)가 전송 도중에 손실(loss)될 수 있습니다.
- 네트워크 지연(network delay)이 예측 불가능하게 발생할 수 있습니다.

이러한 문제들은 관리되는 데이터 센터(managed data center) 환경에서도 지속적으로 발생하며, 가상화된(virtualized) 환경에서는 더욱 빈번하게 나타납니다.

#### 핵심 기능(Core Capabilities)

Akka는 세 가지 근본적인 기능을 제공합니다.

1. **동시성 동작(Concurrent behavior)**: 원자적 연산(atomics)이나 락(lock) 같은 저수준 구성 요소 없이 동시성을 구현할 수 있으며, 메모리 가시성(memory visibility) 문제를 신경 쓰지 않아도 됩니다.
2. **투명한 원격 통신(Transparent remote communication)**: 네트워킹의 복잡성을 추상화(abstract)하여 원격 통신을 투명하게 처리합니다.
3. **클러스터링되고 탄력적인 아키텍처(Clustered, elastic architecture)**: 동적 확장(dynamic scaling)과 고가용성(high availability)을 지원합니다.

#### 액터 모델 기반(Actor Model Foundation)

액터 모델(actor model)은 Akka의 개념적 기반(conceptual foundation)으로서, 올바른 동시성(concurrent) 및 분산(distributed) 시스템을 구축하기 위한 추상화(abstraction)를 제공합니다. 이 통합된 프로그래밍 모델(unified programming model)은 모든 Akka 라이브러리에 걸쳐 일관된 이해를 보장하며, 긴밀하게 통합(tight integration)되어 있습니다.

#### 전략적 위치(Strategic Positioning)

Akka 라이브러리는 두 가지 상위 수준 제품을 지원합니다.

- **Akka SDK**: 클러스터링을 포함한 빠른 개발(rapid development)을 제공합니다.
- **Akka Automated Operations**: 탄력성(elasticity)과 다중 리전(multi-region) 가용성을 관리해 주는 관리형 인프라(managed infrastructure)입니다.

#### 시작 경로(Getting Started Path)

처음 시작하는 사용자는 Hello World 예제부터 시작한 다음, 시스템의 동기(systems motivation), 액터 모델 원리(actor model principles), 라이브러리 개요(library overviews), 실습 튜토리얼(practical tutorials)을 다루는 시작하기(Getting Started) 가이드로 진행하는 것이 좋습니다.

---

### 2. 현대 시스템이 새로운 프로그래밍 모델을 필요로 하는 이유

액터 모델은 수십 년 전 Carl Hewitt가 제안했으며, 전통적인 객체 지향 프로그래밍(object-oriented programming)이 완전히 해결하지 못하는 분산 시스템의 과제들을 다룹니다. 이제 현대 하드웨어와 인프라의 역량이 Hewitt의 비전을 뒷받침할 수 있게 되면서, 액터 모델은 요구가 까다로운 애플리케이션의 프로덕션(production) 환경에서 검증된 "매우 효과적인 해결책(a highly effective solution)"이 되었습니다.

현대 아키텍처와 전통적인 프로그래밍 모델 사이에는 세 가지 주요한 불일치(mismatch)가 존재합니다.

#### 2.1 캡슐화의 도전(The Challenge of Encapsulation)

**핵심 문제**: 객체 지향의 캡슐화(encapsulation)는 단일 스레드 접근(single-threaded access)을 전제로 합니다. 다중 스레드 실행(multi-threaded execution)은 이 보호 장치를 무력화합니다.

**락(lock)의 문제점**:

- 락은 "동시성을 심각하게 제한(seriously limit concurrency)"하며, 현대 CPU에서 비용이 큽니다.
- 스레드를 블로킹(blocking)하면 다른 의미 있는 작업을 수행하지 못하게 됩니다.
- 락은 교착 상태(deadlock)의 위험을 초래합니다.
- 여러 머신에 걸친 분산 락(distributed lock)은 비효율적이며 확장성(scalability)을 제한합니다.

**핵심 통찰**: "객체는 단일 스레드 접근 상황에서만 캡슐화를 보장할 수 있으며, 다중 스레드 실행은 거의 항상 내부 상태의 손상(corrupted internal state)으로 이어진다."

#### 2.2 공유 메모리의 환상(The Illusion of Shared Memory)

**현실**: 현대 CPU는 메모리에 직접 기록하지 않습니다. 각 코어에 로컬한 캐시 라인(cache line)에 기록합니다. 데이터는 코어 간에 명시적으로 전달(shipped)되어야 합니다.

**도전 과제**: 모든 변수를 `volatile`로 표시하는 것은 지나치게 비용이 큽니다. 왜냐하면 "코어 간에 캐시 라인을 전달하는 것은 매우 비용이 큰 연산(a very costly operation)"이기 때문입니다.

**해결 개념**: 공유 변수(shared variable)를 통해 메시지 전달(message-passing)을 숨기기보다는, 상태(state)를 각 동시성 엔티티(concurrent entity)에 로컬하게 유지하고 데이터를 메시지를 통해 명시적으로 전파(propagate)해야 합니다.

#### 2.3 호출 스택의 환상(The Illusion of a Call Stack)

**문제**: 호출 스택(call stack)은 스레드를 가로질러 확장되지 않으므로, 비동기 작업 위임(asynchronous task delegation)이 문제가 됩니다.

**예외 처리의 붕괴(Exception Handling Breakdown)**: 작업자 스레드(worker thread)가 실패하면, 예외(exception)는 원래 호출자(caller)에게 전파되지 않습니다. "호출자 스레드는 어떤 식으로든 통지를 받아야 하지만, 예외로 풀어낼(unwind) 호출 스택이 존재하지 않는다."

**치명적 시나리오(Catastrophic Scenario)**: 작업자 스레드가 충돌(crash)하면, 작업 상태(task state)가 완전히 손실됩니다. "네트워킹이 전혀 관여하지 않은 로컬 통신임에도 불구하고 우리는 메시지를 잃어버렸다."

#### 동시성 시스템에 대한 함의(Implications for Concurrent Systems)

효과적인 동시성 시스템은 다음을 요구합니다.

- 호출 스택을 넘어서는 명시적 오류 신호(error signaling) 메커니즘
- 분산 시스템과 유사한 타임아웃(timeout) 처리
- 메시지/응답이 손실되거나 지연될 수 있다는 인식
- 원칙에 입각한 재시작(principled restart) 메커니즘을 통한 서비스 장애 복구(fault recovery)

---

### 3. 액터 모델이 현대 분산 시스템의 요구를 충족하는 방법

액터 모델은 현대 분산 시스템에 대한 기존 프로그래밍 관행의 단점을 해결합니다. 기존 지식을 버리는 것이 아니라, 통신 기반 시스템(communication-based system)의 사고 모델(mental model)에 더 잘 부합하는 원칙적인 해결책을 제공합니다.

#### 액터의 주요 이점(Key Benefits of Actors)

액터 모델은 개발자가 다음을 할 수 있게 합니다.

- 락(lock)에 의존하지 않고 캡슐화(encapsulation)를 유지합니다.
- 신호(signal)에 반응하고, 상태를 변경하며, 서로 통신하는 협력적 엔티티(cooperative entities)를 사용하여 애플리케이션을 구축합니다.
- 실제 세계의 관점을 더 잘 반영하는 실행 모델(execution model)로 작업합니다.

#### 메시지 전달은 락킹과 블로킹을 제거한다(Message Passing Eliminates Locking and Blocking)

**핵심 원칙**: 액터는 메서드 호출(method call)이 아니라 메시지(message)를 통해 통신합니다. 액터가 메시지를 보낼 때, 실행 제어(execution control)를 넘기지 않으며 블로킹 없이 계속 작업할 수 있습니다.

메서드 반환 시 실행 스레드를 해제하는 객체와 달리, 액터는 메시지를 순차적으로(sequentially) 처리하고 각 메시지를 처리한 후 제어를 반환합니다. 이 방식은 동일한 시간 내에 더 높은 처리량(throughput)을 가능하게 합니다.

**중요한 구분**: 메시지에는 반환 값(return value)이 없습니다. 작업을 위임할 때, 수신 액터(receiving actor)는 값을 직접 반환하는 대신 결국 별도의 응답 메시지(reply message)로 응답합니다.

#### 순차 처리를 통한 캡슐화(Encapsulation Through Sequential Processing)

액터는 메시지를 한 번에 하나씩 처리함으로써 캡슐화를 보존합니다. 개별 액터는 메시지를 순차적으로 처리하지만, 여러 액터는 동시에(concurrently) 동작하므로 시스템이 가용한 하드웨어를 충분히 활용할 수 있습니다.

각 액터는 동시에 최대 하나의 메시지만 처리하기 때문에, 내부 불변식(internal invariants)이 자동으로 보호되며 동기화 메커니즘(synchronization mechanism)이 필요하지 않습니다.

#### 액터 처리 워크플로(Actor Processing Workflow)

액터가 메시지를 수신할 때의 흐름은 다음과 같습니다.

1. 메시지가 큐(queue)에 들어갑니다.
2. 스케줄되지 않은(unscheduled) 상태라면, 액터가 준비됨(ready) 상태로 표시됩니다.
3. 스케줄러(scheduler)가 실행을 시작합니다.
4. 액터가 큐의 맨 앞 메시지를 가져옵니다.
5. 액터가 상태를 변경하고 메시지를 보냅니다.
6. 액터가 스케줄되지 않은(unscheduled) 상태가 됩니다.

#### 액터 구성 요소(Actor Components)

액터는 다음 요소들을 가집니다.

- **메일박스(Mailbox)**: 들어오는 메시지를 담는 큐(queue)
- **행동(Behavior)**: 상태 정의 및 메시지 응답 로직
- **메시지(Messages)**: 메서드 호출에 비견되는 데이터 신호
- **실행 환경(Execution environment)**: 액터의 호출(invocation)을 관리하는 스레드 풀(thread pool) 메커니즘
- **주소(Address)**: 식별 메커니즘

#### 문제 해결(Problem Resolution)

이 모델은 핵심 문제들을 효과적으로 해결합니다.

- 실행 분리(execution decoupling)를 통한 캡슐화 보존
- 순차적 메시지 처리를 통한 락(lock) 요구 제거
- 제한된 스레드 위에서 수백만 개의 액터를 효율적으로 실행
- 로컬 상태(local state)와 네트워크 호환 전파(network-compatible propagation) 패턴

#### 오류 처리 전략(Error Handling Strategies)

오류에는 두 가지 범주가 존재합니다.

**작업 실패(Task Failures)**: 위임된 작업(예: 유효성 검증 오류)이 실패하더라도, 서비스는 계속 기능합니다. 액터는 오류 메시지(error message)로 응답하며, 오류는 평범한 도메인 메시지(ordinary domain message)가 됩니다.

**서비스 장애(Service Faults)**: 내부 장애(internal failure)는 슈퍼비전(supervision) 메커니즘을 발동시킵니다. Akka는 계층적 액터 구성(hierarchical actor organization)을 강제하며, 부모 액터(parent actor)가 정의된 전략(strategy)을 통해 자식(children)을 관리합니다. 부모는 자식 액터를 재시작(restart)하거나 정지(stop)할 수 있으며, 실패에 대해 통지를 받습니다.

**슈퍼비전의 이점(Supervision Advantage)**: 재시작은 외부에서 보이지 않으므로, 협력자(collaborator)들은 복구가 진행되는 동안에도 계속 메시지를 보낼 수 있습니다.

---

### 4. Akka 라이브러리와 모듈 개요

Akka는 액터 모델을 사용하여 분산 시스템을 구축하기 위한 프레임워크입니다. 모든 핵심 기능(core functionality)은 오픈 소스(open source)로 유지되며, Lightbend는 교육(training)과 엔터프라이즈 기능을 포함한 상용 지원(commercial support)을 제공합니다.

#### 액터 라이브러리(Actor Library)

기반 모듈인 `akka-actor-typed`는 상태(state)와 실행(execution)을 모두 캡슐화하는 것을 강조하는 프로그래밍 패러다임을 구현합니다. 메서드 호출 대신 메시지 전달(message passing)을 통해 통신이 이루어집니다. 이 접근 방식은 세 가지 주요 과제를 다룹니다.

- 고성능 동시성 애플리케이션(high-performance concurrent application) 구축
- 다중 스레드 환경(multi-threaded environment)에서의 오류 관리
- 동시성의 함정(concurrency pitfalls)으로부터 프로젝트 보호

#### 리모팅(Remoting)

이 모듈은 서로 다른 컴퓨터에 있는 액터들이 메시지를 투명하게(transparently) 교환할 수 있게 합니다. 리모팅은 "라이브러리라기보다는 모듈에 가까운 것"으로 묘사되며, 직접적인 API보다는 주로 설정(configuration)에 의존합니다. 리모팅은 다음과 같은 인프라 수준의 과제를 해결합니다.

- 원격 액터 주소 지정(remote actor addressing)
- 메시지 직렬화(message serialization)
- 네트워크 연결 관리(network connection management)
- 투명한 호스트 장애 감지(transparent host failure detection)

#### 클러스터(Cluster)

리모팅 위에 구축된 클러스터링(clustering)은 멤버십 프로토콜(membership protocol)을 사용하여 협력하는 액터 시스템들을 통합된 "메타 시스템(meta-system)"으로 조직합니다. 문서는 다음과 같이 강조합니다: "대부분의 경우, 리모팅을 직접 사용하기보다는 클러스터 모듈을 사용하는 것이 좋다." 클러스터링은 다음을 다룹니다.

- 분산 시스템 관리
- 멤버(member)의 안전한 도입
- 장애 감지(failure detection)
- 멤버 제거(member removal)
- 계산 분산(computation distribution)
- 역할 지정(role designation)

#### 클러스터 샤딩(Cluster Sharding)

이 모듈은 대규모 액터 집합을 클러스터 멤버들에 걸쳐 분산시키며, 일반적으로 영속성(persistence)과 함께 사용됩니다. 다음을 관리합니다.

- 상태를 가진 엔티티(stateful entity)의 분산
- 부하 분산(load balancing)
- 충돌(crash) 시 상태 보존
- 중복 엔티티 인스턴스를 방지하는 일관성 보장(consistency guarantees)

#### 클러스터 싱글톤(Cluster Singleton)

클러스터 전체에서 단일 서비스 인스턴스(single cluster-wide service instance)가 필요한 시나리오를 위해, 이 모듈은 유일성(uniqueness)을 보장하면서 호스트 장애 시 마이그레이션(migration)을 처리합니다. 다음을 다룹니다.

- 서비스 유일성(service uniqueness)
- 시스템 충돌 중 가용성(availability)
- 마이그레이션에도 불구하고 접근 가능한 인스턴스 위치 파악

#### 영속성(Persistence)

액터는 전통적으로 상태를 휘발성 메모리(volatile memory)에 저장합니다. 영속성(Persistence)은 이벤트를 저장하여 시스템 재시작 시 상태를 재구성할 수 있게 합니다. 이 모듈은 다음을 지원합니다.

- 이벤트 재생(event replay)
- CQRS(Command Query Responsibility Segregation) 구현
- 장애에도 불구하고 신뢰성 있는 메시지 전달
- 도메인 이벤트 인트로스펙션(domain event introspection)
- 이벤트 소싱(event sourcing) 패턴

#### 프로젝션(Projections)

이 모듈은 간단한 API를 사용하여 다운스트림 프로젝션(downstream projection)을 위해 이벤트 스트림(event stream)을 소비합니다. 다음을 가능하게 합니다.

- 대체 뷰(alternate view) 구성
- Kafka 같은 시스템으로의 이벤트 전파
- 이벤트 소싱 및 CQRS 컨텍스트 내에서 읽기 측 프로젝션(read-side projection) 구축

#### 분산 데이터(Distributed Data)

최종 일관성(eventual consistency)을 수용하는 시스템을 위해, 이 모듈은 충돌 없는 복제 데이터 타입(Conflict-Free Replicated Data Types, CRDT)을 사용하여 클러스터 노드 전반에 걸쳐 데이터를 공유합니다. 여러 노드에서의 동시 쓰기(concurrent write)와 예측 가능한 병합(predictable merging)을 허용하여, 분할 내성(partition tolerance)과 저지연 로컬 접근(low-latency local access) 요구를 충족합니다.

#### 스트림(Streams)

액터 위에 구축된 이 상위 수준 추상화는, 잠재적으로 무한한 이벤트 시퀀스(infinite event sequence)를 처리하는 처리 네트워크(processing network)를 단순화하면서 리소스 조정(resource coordination)을 관리합니다. Reactive Streams 표준의 구현은 서드파티(third-party) 통합을 가능하게 합니다. 스트림은 다음을 다룹니다.

- 고성능 스트림 처리(high-performance stream handling)
- 파이프라인 합성(pipeline composition)
- 비동기 서비스 연결(asynchronous service connection)
- 리액티브 인터페이스(reactive interface) 제공

#### Alpakka

Streams API 위에 구축된 별도의 모듈 모음인 Alpakka는, 클라우드 및 인프라 기술을 위한 리액티브 스트림 커넥터(reactive stream connector)를 제공합니다. 리액티브 API를 통해 인프라 구성 요소 통합 및 레거시 시스템(legacy system) 연결을 가능하게 합니다.

#### HTTP

HTTP 서비스를 구축하고 소비하기 위한 도구를 제공하는 별도의 모듈로서, 대용량 데이터셋과 실시간 이벤트의 스트리밍에 특히 최적화되어 있습니다. 다음을 다룹니다.

- API 노출(API exposure)
- 대용량 데이터셋 스트리밍
- 실시간 이벤트 스트리밍

#### gRPC

이 별도의 모듈은 gRPC를 HTTP 및 Streams 모듈과 통합하며, protobuf 정의로부터 클라이언트 및 서버 아티팩트(artifact)를 생성합니다. gRPC의 이점을 활용합니다.

- 스키마 우선 계약(schema-first contracts)
- 스키마 진화(schema evolution)
- 효율적인 바이너리 프로토콜(efficient binary protocol)
- 스트리밍 지원
- 상호 운용성(interoperability)
- HTTP/2 멀티플렉싱(multiplexing)

#### 통합 예시(Integration Example)

문서는 모듈들이 어떻게 통합되는지 설명합니다: 상태를 가진 비즈니스 객체는 샤딩된(sharded), 영속적인(persistent) 엔티티로 모델링되어 확장 가능한 클러스터 전반에 부하 분산됩니다. 실시간 도메인 이벤트 스트림은 빠른 데이터 엔진(fast data engine)을 통해 파이프되며, 처리된 출력은 분석 대시보드(analytics dashboard)를 위한 WebSocket 연결을 갖춘 부하 분산 HTTP 서버를 통해 노출됩니다.

#### 기술적 구현(Technical Implementation)

모든 모듈은 보안 토큰화된 저장소 접근(secure, tokenized repository access)을 요구합니다. 의존성 관리(dependency management)는 다양한 빌드 시스템(sbt, Maven, Gradle)에 걸쳐 BOM(Bill of Materials) 패턴을 사용합니다. 문서 예제 전반에서 버전 2.10.19가 참조됩니다.

---

### 5. 용어와 개념(Terminology and Concepts)

이 장에서는 동시성·분산 컴퓨팅에서 자주 쓰이지만 의미가 혼동되는 용어들을 정리합니다.

#### 5.1 동시성 vs. 병렬성(Concurrency vs. Parallelism)

**동시성**(Concurrency)은 "두 개 이상의 작업이 동시에 실행되지는 않더라도 진행을 이루어 가는 것(making progress)"을 의미합니다. 이는 시간 분할(time slicing)을 통해 달성될 수 있습니다.

**병렬성**(Parallelism)은 "실행이 진정으로 동시에(truly simultaneous) 이루어질 때" 발생합니다.

#### 5.2 비동기 vs. 동기(Asynchronous vs. Synchronous)

**동기(synchronous)** 메서드 호출은 메서드가 반환하거나 예외를 던질 때까지 호출자가 진행하지 못하게 막습니다.

**비동기(asynchronous)** 호출은 "호출자가 유한한 단계(finite number of steps) 이후에 진행할 수 있게 하며, 메서드의 완료는 어떤 추가적인 메커니즘을 통해 신호(signal)될 수 있습니다."

액터는 본질적으로 비동기(inherently asynchronous)입니다.

#### 5.3 논블로킹 vs. 블로킹(Non-blocking vs. Blocking)

**블로킹**(Blocking)은 "한 스레드의 지연이 다른 스레드들 중 일부를 무한정 지연시킬 수 있을 때" 발생합니다.

**논블로킹**(Non-blocking)은 "어떤 스레드도 다른 스레드들을 무한정 지연시킬 수 없음"을 의미합니다.

문서는 블로킹 API를 항상 피할 수는 없지만, 신중하게 관리(careful management)되어야 한다고 언급합니다.

#### 5.4 교착 상태 vs. 기아 상태 vs. 라이브락(Deadlock vs. Starvation vs. Livelock)

**교착 상태**(Deadlock)는 참여자들이 서로가 진행하기를 기다리면서 "Catch-22" 상황을 만들어내고, 그 결과 "영향을 받은 모든 하위 시스템이 멈추는(stall)" 경우에 발생합니다.

**기아 상태**(Starvation)는 일부 참여자는 진행할 수 있지만 다른 참여자들은 진행할 수 없을 때(흔히 스케줄링 편향(scheduling bias) 때문에) 발생합니다.

**라이브락**(Livelock)은 교착 상태와 유사하지만, "참여자들이 상태를 계속해서 변경(continuously change their state)"한다는 점에서 다릅니다. 즉, 멈춰 있는 것이 아니라 계속 움직이지만 진전을 이루지는 못합니다.

#### 5.5 경쟁 조건(Race Condition)

**경쟁 조건**(race condition)은 "어떤 이벤트 집합의 순서(ordering)에 대한 가정이 외부의 비결정적 효과(external non-deterministic effects)에 의해 위반될 수 있을 때" 발생합니다. Akka는 특정 액터 쌍(specific actor pairs) 사이의 메시지 순서(message ordering)를 보장합니다.

#### 5.6 논블로킹 보장(Non-blocking Guarantees)

논블로킹 알고리즘은 보장의 강도에 따라 다음과 같이 구분됩니다.

- **대기 없음(Wait-freedom)**: "모든 호출이 유한한 단계 내에 완료되는 것이 보장됩니다." 가장 강력한 보장입니다.
- **락 없음(Lock-freedom)**: "무한히 자주(infinitely often) 어떤 메서드가 유한한 단계 내에 완료됩니다." 그러나 모든 호출이 완료되는 것을 보장하지는 않습니다.
- **방해 없음(Obstruction-freedom)**: 가장 약한 보장으로, 메서드가 격리되어(in isolation) 실행될 때 완료됩니다.

---

### 6. 액터 시스템(Actor Systems)

Akka의 액터 시스템(actor system)은 "상태와 행동(state and behavior)을 캡슐화하고, 오직 메시지를 교환함으로써만 통신하며, 그 메시지는 수신자의 메일박스(recipient's mailbox)에 놓이는" 객체들로 구성됩니다. 이 프레임워크는 액터를 작업을 계층적으로 위임하는 조직 구성원(organizational member)처럼 다룹니다.

#### 핵심 구조 원칙(Key Structural Principles)

**계층적 조직(Hierarchical Organization)**: 액터는 자연스럽게 계층(hierarchy)을 형성하며, 부모 액터(parent actor)가 자식(children)에게 작업을 위임합니다. 이 "오류 커널 패턴(Error Kernel Pattern)"은 위험한 작업을 자식 액터에 격리시킴으로써 중요한 상태(critical state)를 보호합니다.

**설정 컨테이너(Configuration Container)**: 액터 시스템은 스케줄링(scheduling)이나 로깅(logging) 같은 공유 시설(shared facilities)을 관리합니다. 하나의 JVM 안에 여러 시스템이 공존할 수 있으며, 이는 시스템 간 투명한 통신(transparent cross-system communication)을 갖춘 분산 애플리케이션을 가능하게 합니다.

#### 모범 사례(Best Practices)

문서는 네 가지 핵심 사례를 제시합니다.

1. 외부 리소스를 기다리며 스레드를 블로킹하지 말고, 이벤트를 효율적으로 처리합니다.
2. 액터 간에 가변 객체(mutable object)를 절대 전달하지 말고, 불변 메시지(immutable message)를 사용합니다.
3. 우발적인 상태 공유(accidental state sharing)를 방지하기 위해, 메시지 안에 행동(behavior)을 담아 보내지 않습니다.
4. 최상위 액터(top-level actor)는 최소한으로 유지하고, 로직은 계층적 하위 시스템(hierarchical subsystems)에 위임합니다.

#### 리소스 관리(Resource Management)

액터는 인스턴스당 약 300바이트(approximately 300 bytes per instance)로 매우 가벼우므로, 하나의 시스템 내에서 수백만 개의 액터를 둘 수 있습니다. 프레임워크가 순서(ordering)와 리소스 할당(resource allocation)을 자동으로 처리하므로, 개발자는 이러한 사항들을 일일이 관리할 필요에서 자유로워집니다.

#### 시스템 종료(System Termination)

애플리케이션은 `ActorSystem`의 `terminate()` 메서드를 통해 종료되며, 이는 `CoordinatedShutdown`을 발동시켜 실행 중인 모든 액터를 우아하게(gracefully) 정지시킵니다.

---

### 7. 액터란 무엇인가(What is an Actor)

이전의 "액터 시스템(Actor Systems)" 섹션에서는 액터들이 어떻게 계층을 형성하며, 애플리케이션을 구축할 때 가장 작은 단위(smallest unit)가 되는지를 설명했습니다. 이 섹션에서는 그러한 액터 하나를 따로 떼어 살펴보면서, 액터를 구현할 때 마주치게 되는 개념들을 설명합니다. 더 심층적인 레퍼런스는 "액터 소개(Introduction to Actors)"를 참고하십시오.

Hewitt, Bishop, Steiger가 1973년에 정의한 액터 모델(Actor Model)은, 계산(computation)이 분산된다는 것이 정확히 무엇을 의미하는지를 표현하는 계산 모델(computational model)입니다. 처리 단위(processing unit)인 액터(Actor)는 오직 메시지를 교환함으로써만 통신할 수 있으며, 메시지를 수신하면 액터는 다음 세 가지 근본적인 행동(fundamental actions)을 할 수 있습니다.

1. 자신이 알고 있는 액터들에게 유한한 수의 메시지를 보낸다.
2. 유한한 수의 새로운 액터를 생성한다.
3. 다음 메시지에 적용할 행동(behavior)을 지정한다.

액터는 상태(State), 행동(Behavior), 메일박스(Mailbox), 자식 액터(Child Actors), 슈퍼바이저 전략(Supervisor Strategy)을 담는 컨테이너입니다. 이 모든 것은 액터 참조(Actor Reference) 뒤에 캡슐화됩니다. 주목할 점은, 액터가 명시적인 생명 주기(explicit lifecycle)를 가진다는 것입니다. 액터는 더 이상 참조되지 않더라도 자동으로 소멸(destroy)되지 않습니다. 액터를 생성한 후에는, 결국 종료(terminate)되도록 보장하는 것이 사용자의 책임입니다. 이를 통해 "액터가 종료될 때(When an Actor Terminates)" 리소스가 어떻게 해제되는지를 사용자가 직접 제어할 수 있습니다.

#### 7.1 액터 참조(Actor Reference)

액터 모델의 이점을 누리려면 액터 객체가 외부로부터 차단(shielded)되어야 합니다. 따라서 액터는 외부에 액터 참조(actor reference)를 통해 표현되며, 이 참조는 제약 없이 자유롭게 전달할 수 있는 객체입니다. 이렇게 내부 객체(inner object)와 외부 객체(outer object)로 분리함으로써, 모든 연산에 대한 투명성(transparency)이 가능해집니다. 즉, 다른 참조를 갱신할 필요 없이 액터를 재시작하는 것, 실제 액터 객체를 원격 호스트(remote host)에 배치하는 것, 액터가 어디에서 실행되든 메시지를 보내는 것이 가능합니다. 가장 중요한 점은, 액터가 스스로 그 정보를 공개하지 않는 한 외부에서 액터 내부를 들여다보거나 그 상태(state)에 접근하는 것이 불가능하다는 것입니다.

액터 참조는 파라미터화(parameterized)되어 있으며, 지정된 타입(specified type)의 메시지만 보낼 수 있습니다.

#### 7.2 상태(State)

액터 객체는 일반적으로 액터가 처할 수 있는 상태를 반영하는 여러 변수를 포함합니다. 이는 명시적인 상태 기계(state machine)일 수도 있고, 카운터(counter), 리스너 집합(set of listeners), 대기 중인 요청(pending requests) 등일 수도 있습니다. 이 데이터들은 액터를 가치 있게 만드는 것이며, 다른 액터에 의한 손상(corruption)으로부터 보호되어야 합니다. Akka 액터는 개념적으로 각자 자신만의 경량 스레드(light-weight thread)를 가지며, 이 스레드는 시스템의 나머지 부분으로부터 완전히 격리됩니다. 따라서 락(lock)으로 접근을 동기화할 필요 없이, 동시성을 의식하지 않고 액터 코드를 작성할 수 있습니다.

내부적으로 Akka는 액터 집합을 실제 스레드 집합 위에서 실행하며, 일반적으로 많은 액터가 하나의 스레드를 공유하고, 한 액터의 후속 호출(subsequent invocations)들이 서로 다른 스레드에서 처리될 수도 있습니다. Akka는 이러한 구현 세부 사항이 액터 상태 처리의 단일 스레드성(single-threadedness)에 영향을 미치지 않도록 보장합니다.

내부 상태(internal state)는 액터 동작의 핵심이므로, 일관성 없는 상태(inconsistent state)는 치명적입니다. 따라서 액터가 실패(fail)하여 슈퍼바이저(supervisor)에 의해 재시작(restart)될 때, 상태는 액터를 처음 생성할 때처럼 처음부터 새로(from scratch) 만들어집니다. 이를 통해 시스템의 자가 치유(self-healing) 능력이 실현됩니다.

선택적으로, 수신한 메시지를 영속화(persisting)하고 재시작 후 이를 재생(replaying)함으로써, 액터의 상태를 재시작 이전의 상태로 자동 복구할 수 있습니다(이벤트 소싱(Event Sourcing) 참고).

#### 7.3 행동(Behavior)

메시지가 처리될 때마다, 해당 메시지는 액터의 현재 행동(current behavior)에 매칭됩니다. 행동(Behavior)이란, 그 시점에 메시지에 반응하여 취할 행동을 정의하는 함수(function)입니다. 예를 들어, 클라이언트가 인가(authorized)되었으면 요청을 전달하고 그렇지 않으면 거부하는 식입니다. 이 행동은 시간이 지나면서 변경될 수 있습니다. 서로 다른 클라이언트가 인가를 얻거나, 액터가 "서비스 불가(out-of-service)" 모드로 들어갔다가 나중에 복귀할 수 있기 때문입니다. 이러한 변경은 행동 로직에서 읽는 상태 변수(state variables)에 인코딩하거나, 다음 메시지에 사용할 다른 행동을 반환하여 런타임에 함수 자체를 교체하는 방식으로 달성됩니다. 다만, 액터 객체 생성(construction) 시점에 정의된 초기 행동(initial behavior)은 특별합니다. 액터가 재시작되면 행동이 이 초기 행동으로 재설정(reset)되기 때문입니다.

메시지는 액터 참조(Actor Reference)로 보낼 수 있으며, 이 외관(façade) 뒤에는 메시지를 수신하고 그에 따라 동작하는 행동이 존재합니다. 액터 참조와 행동 사이의 바인딩(binding)은 시간이 지나면서 변경될 수 있지만, 이는 외부에서 보이지 않습니다.

액터 참조는 파라미터화되어 있으며, 지정된 타입의 메시지만 보낼 수 있습니다. 액터 참조(및 그 액터)가 생성될 때, 액터 참조와 그 타입 파라미터(type parameter) 사이의 연관이 맺어져야 합니다. 이를 위해 각 행동(behavior) 또한 처리할 수 있는 메시지의 타입으로 파라미터화됩니다. 행동은 액터 참조 외관 뒤에서 변경될 수 있으므로, 다음 행동(next behavior)을 지정하는 것은 제약된 연산(constrained operation)입니다. 즉, 후임 행동(successor)은 그 선임(predecessor)과 동일한 타입의 메시지를 처리해야 합니다. 이는 이 액터를 가리키는 액터 참조들을 무효화하지 않기 위해 필요합니다.

이것이 가능하게 하는 것은, 메시지가 액터로 보내질 때마다 메시지의 타입이 그 액터가 처리한다고 선언한 타입 중 하나임을 정적으로(statically) 보장할 수 있다는 점입니다. 우리는 완전히 무의미한 메시지를 보내는 실수를 피할 수 있습니다. 그러나 정적으로 보장할 수 없는 것은, 우리의 메시지가 수신될 때 액터 참조 뒤의 행동이 특정 상태에 있을지 여부입니다. 그 근본적인 이유는, 액터 참조와 행동 사이의 연관이 동적인 런타임 속성(dynamic runtime property)이며, 컴파일러는 소스 코드를 번역하는 동안 그것을 알 수 없기 때문입니다.

이는 내부 변수를 가진 일반적인 Java 객체와 동일합니다. 프로그램을 컴파일할 때 우리는 그 변수들의 값이 무엇이 될지 알 수 없으며, 메서드 호출의 결과가 그 변수들에 의존한다면 그 결과는 어느 정도 불확실합니다. 우리는 다만 반환된 값이 주어진 타입(given type)임을 확신할 수 있을 뿐입니다.

액터 명령(command)의 응답 메시지 타입(reply message type)은, 그 메시지에 포함된 응답 대상(reply-to)에 대한 액터 참조의 타입으로 기술됩니다. 이는 대화(conversation)를 그 타입의 관점에서 기술할 수 있게 합니다. 응답은 타입 A이지만, 그것이 타입 B의 주소를 포함할 수도 있으며, 그러면 다른 액터가 이 새로운 액터 참조로 타입 B의 메시지를 보냄으로써 대화를 이어갈 수 있습니다. 우리는 액터의 "현재(current)" 상태를 정적으로 표현할 수는 없지만, 두 액터 사이의 프로토콜의 현재 상태(current state of a protocol)는 표현할 수 있습니다. 이는 단지 마지막으로 수신되거나 보내진 메시지의 타입에 의해 주어지기 때문입니다.

#### 7.4 메일박스(Mailbox)

액터의 목적은 메시지를 처리하는 것이며, 이 메시지들은 다른 액터들(또는 액터 시스템 외부)로부터 그 액터에게 보내진 것입니다. 송신자(sender)와 수신자(receiver)를 연결하는 부분이 바로 액터의 메일박스(mailbox)입니다. 각 액터는 정확히 하나의 메일박스를 가지며, 모든 송신자가 이 메일박스에 자신의 메시지를 큐잉(enqueue)합니다. 큐잉은 송신 연산(send operations)의 시간 순서(time-order)대로 일어납니다. 이는 곧, 서로 다른 액터들로부터 보내진 메시지들은, 액터들이 스레드에 분산되는 겉보기 무작위성(apparent randomness) 때문에 런타임에 정의된 순서를 가지지 않을 수 있음을 의미합니다. 반면, 동일한 액터로부터 동일한 대상에게 여러 메시지를 보내면, 그것들은 같은 순서로 큐잉됩니다.

선택할 수 있는 다양한 메일박스 구현(mailbox implementation)이 존재하며, 기본값은 FIFO입니다. 즉, 액터가 처리하는 메시지의 순서는 그것들이 큐잉된 순서와 일치합니다. 이것은 보통 좋은 기본값이지만, 애플리케이션에 따라 일부 메시지를 다른 메시지보다 우선시(prioritize)해야 할 수도 있습니다. 이 경우, 우선순위 메일박스(priority mailbox)는 항상 끝이 아니라 메시지 우선순위(message priority)에 따른 위치에 큐잉하며, 그것은 심지어 맨 앞일 수도 있습니다. 이러한 큐를 사용하는 동안, 처리되는 메시지의 순서는 자연히 그 큐의 알고리즘에 의해 정의되며 일반적으로 FIFO가 아닙니다.

Akka가 일부 다른 액터 모델 구현과 다른 중요한 특징은, 현재 행동(current behavior)이 항상 다음으로 디큐(dequeue)된 메시지를 처리해야 한다는 점입니다. 즉, 다음에 매칭되는 메시지를 찾기 위해 메일박스를 훑어보는 일(scanning the mailbox)은 없습니다. 메시지 처리에 실패하는 것은, 이 행동이 재정의(override)되지 않는 한 일반적으로 실패(failure)로 취급됩니다.

#### 7.5 자식 액터(Child Actors)

각 액터는 잠재적으로 부모(parent)가 됩니다. 하위 작업(sub-task)을 위임하기 위해 자식을 생성하면 그 자식들을 자동으로 슈퍼비전(supervise)하게 됩니다. 자식 목록(list of children)은 액터의 컨텍스트(context) 안에서 유지되며, 액터는 이 목록에 접근할 수 있습니다. 자식을 스폰(spawning)하거나 정지(stopping)하면 즉시 목록에 반영됩니다. 실제 생성 및 종료는 비동기적(asynchronous)으로 처리되므로 부모를 블로킹하지 않습니다.

#### 7.6 슈퍼바이저 전략(Supervisor Strategy)

액터의 마지막 구성 요소는 예상치 못한 예외(unexpected exceptions), 즉 실패(failures)를 처리하기 위한 전략입니다. 장애 처리(fault handling)는 Akka에 의해 투명하게 수행되며, 각 실패에 대해 "장애 내성(Fault Tolerance)"에서 기술된 전략 중 하나가 적용됩니다.

#### 7.7 액터가 종료될 때(When an Actor Terminates)

액터가 종료되면(재시작으로 처리되지 않는 방식으로 실패하거나, 스스로 정지하거나, 슈퍼바이저에 의해 정지되면), 해당 액터는 리소스를 해제하고 메일박스에 남아 있는 모든 메시지를 시스템의 "데드 레터 메일박스(dead letter mailbox)"로 보냅니다. 이 데드 레터 메일박스는 해당 메시지들을 `DeadLetters`로서 `EventStream`으로 전달합니다. 이후 메일박스는 액터 참조 내에서 시스템 메일박스(system mailbox)로 교체되어, 이후 수신되는 모든 메시지를 `DeadLetters`로서 `EventStream`으로 리디렉션합니다. 다만 이는 최선 노력(best effort) 기반으로 이루어지므로, "보장된 전달(guaranteed delivery)"을 위해 이에 의존해서는 안 됩니다.

---

### 8. 슈퍼비전과 모니터링(Supervision and Monitoring)

이 장에서는 슈퍼비전(supervision)의 개념, 제공되는 기본 요소(primitives)와 그 의미론(semantics)을 개괄합니다. 실제 코드로의 적용에 대한 세부 사항은 supervision을 참고하시기 바랍니다.

슈퍼비전은 클래식(classic) 이후로 변경되었습니다. 클래식 슈퍼비전에 대한 세부 사항은 "Classic Supervision"을 참고하십시오.

#### 8.1 슈퍼비전이 의미하는 것(What Supervision Means)

액터에서 발생할 수 있는 예외에는 두 가지 범주가 있습니다.

1. 입력 유효성 검증 오류(Input validation errors). 일반적인 try-catch나 기타 언어 및 표준 라이브러리 도구로 처리할 수 있는 예상된 예외(expected exceptions)입니다.
2. 예상치 못한 실패(Unexpected failures). 예를 들어 네트워크 리소스를 사용할 수 없거나, 디스크 쓰기가 실패하거나, 애플리케이션 로직에 버그가 있는 경우입니다.

슈퍼비전은 실패(failures)를 다루며, 이는 비즈니스 로직(business logic)과 분리되어야 합니다. 반면 데이터의 유효성 검증과 예상된 예외의 처리는 비즈니스 로직의 필수적인 부분입니다. 따라서 슈퍼비전은 액터의 메시지 처리 로직과 뒤섞이는 무언가가 아니라, 액터에 데코레이션(decoration)으로서 추가됩니다.

슈퍼비전 대상 작업의 성격과 실패의 성격에 따라, 슈퍼비전은 다음 세 가지 전략(strategies)을 제공합니다.

1. 액터를 재개(Resume)하여, 누적된 내부 상태(accumulated internal state)를 유지합니다.
2. 액터를 재시작(Restart)하여, 누적된 내부 상태를 지우고, 잠재적으로 지연(delay)을 둔 후 다시 시작합니다.
3. 액터를 영구적으로 정지(Stop)합니다.

액터는 계층(hierarchy)의 일부이므로, 영구적인 실패를 위쪽으로 전파(propagate)하는 것이 종종 타당할 수 있습니다. 어떤 액터의 모든 자식이 예상치 못하게 정지되었다면, 그 액터 자신이 기능하는 상태로 되돌아가기 위해 재시작하거나 정지하는 것이 타당할 수 있습니다. 이는 슈퍼비전과, 자식이 종료될 때 통지를 받기 위해 자식을 감시(watching)하는 것을 조합하여 달성할 수 있습니다.

#### 8.2 최상위 액터(The Top-Level Actors)

액터 시스템은 생성 도중에 최소 두 개의 액터를 시작합니다.

##### `/user`: 사용자 가디언 액터(user guardian actor)

이것은 사용자가 제공한 최상위 액터로서, 하위 시스템(subsystems)을 자식으로 스폰하여 애플리케이션을 부트스트랩(bootstrap)하기 위한 것입니다. 사용자 가디언이 정지하면 전체 액터 시스템이 종료됩니다.

##### `/system`: 시스템 가디언 액터(system guardian actor)

이 특별한 가디언은, 로깅 자체가 액터를 사용하여 구현되어 있음에도 불구하고 모든 일반 액터가 종료되는 동안 로깅이 활성 상태로 유지되는 질서 있는 종료 시퀀스(orderly shut-down sequence)를 달성하기 위해 도입되었습니다. 이는 시스템 가디언이 사용자 가디언을 감시하고, 사용자 가디언이 정지하는 것을 본 후에 자신의 종료를 시작하게 함으로써 실현됩니다.

#### 8.3 재시작이 의미하는 것(What Restarting Means)

특정 메시지를 처리하다가 실패한 액터를 마주했을 때, 그 실패의 원인은 세 가지 범주로 나뉩니다.

- 수신한 특정 메시지에 대한 체계적(즉, 프로그래밍) 오류(Systematic/programming error)
- 메시지를 처리하는 동안 사용된 어떤 외부 리소스의 (일시적) 실패((Transient) failure of some external resource)
- 액터의 손상된 내부 상태(Corrupt internal state)

실패가 구체적으로 인식 가능한 것이 아니라면, 세 번째 원인(손상된 내부 상태)을 배제할 수 없으며, 이는 곧 내부 상태를 지워내야(cleared out) 한다는 결론으로 이어집니다. 만약 슈퍼바이저가 (예컨대 의식적인 오류 커널 패턴(error kernel pattern)의 적용 덕분에) 자신의 다른 자식들이나 자기 자신이 그 손상에 영향받지 않는다고 판단한다면, 따라서 그 액터를 재시작하는 것이 최선입니다. 이는 기반 `Behavior` 클래스의 새 인스턴스를 생성하고, 자식의 `ActorRef` 내부에서 실패한 인스턴스를 새 인스턴스로 교체함으로써 수행됩니다. 이렇게 할 수 있는 능력은 액터를 특별한 참조(special references) 안에 캡슐화하는 이유 중 하나입니다. 그러면 새 액터가 자신의 메일박스 처리를 재개하므로, 실패가 발생한 그 메시지가 다시 처리되지 않는다는 주목할 만한 예외를 제외하면, 재시작은 액터 자체의 외부에서 보이지 않습니다.

#### 8.4 생명 주기 모니터링이 의미하는 것(What Lifecycle Monitoring Means)

> 참고: Akka에서 생명 주기 모니터링(Lifecycle Monitoring)은 보통 `DeathWatch`라고 불립니다.

위에서 설명한 부모-자식 사이의 특별한 관계와 달리, 각 액터는 다른 어떤 액터든 모니터링(monitor)할 수 있습니다. 액터는 생성과 동시에 완전히 살아있는(fully alive) 상태가 되고 재시작은 해당 슈퍼바이저 외부에서 보이지 않으므로, 모니터링에서 관찰할 수 있는 유일한 상태 변화는 살아있음(alive)에서 죽음(dead)으로의 전이뿐입니다. 따라서 모니터링은 한 액터를 다른 액터에 연결하여 종료(termination)에 반응하기 위해 사용됩니다. 이는 실패(failure)에 반응하는 슈퍼비전과는 대조적입니다.

생명 주기 모니터링은 모니터링 액터가 수신하는 `Terminated` 메시지를 통해 구현됩니다. 기본 동작은, 별도로 처리하지 않을 경우 특별한 `DeathPactException`을 던지는 것입니다. `Terminated` 메시지 수신을 시작하려면 `ActorContext.watch(targetActorRef)`를 호출하고, 수신을 중지하려면 `ActorContext.unwatch(targetActorRef)`를 호출합니다. 중요한 점은, 모니터링 등록과 대상 종료의 발생 순서에 관계없이 메시지가 전달된다는 것입니다. 즉, 등록 시점에 대상이 이미 종료된 상태더라도 `Terminated` 메시지를 받게 됩니다.

#### 8.5 액터와 예외(Actors and Exceptions)

액터가 메시지를 처리하는 도중에 어떤 종류의 예외(예: 데이터베이스 예외)가 던져질 수 있습니다.

##### 메시지에는 무슨 일이 일어나는가(What happens to the Message)

메시지를 처리하는 도중(즉, 메일박스에서 꺼내져 현재 행동에 넘겨진 동안) 예외가 발생하면 해당 메시지는 손실(lost)됩니다. 메시지가 메일박스에 다시 들어가지 않는다는 점이 중요합니다. 따라서 메시지 처리를 재시도(retry)하려면, 예외를 직접 잡아(catching the exception) 흐름을 재시도하는 방식으로 처리해야 합니다. 재시도 횟수에는 반드시 상한(bound)을 두어야 합니다. 시스템이 라이브락(livelock), 즉 진전 없이 많은 CPU 사이클을 소비하는 상태에 빠지지 않게 하기 위해서입니다.

##### 메일박스에는 무슨 일이 일어나는가(What happens to the Mailbox)

메시지가 처리되는 동안 예외가 던져지더라도, 메일박스에는 아무 일도 일어나지 않습니다. 만약 액터가 재시작되면, 동일한 메일박스가 그대로 존재합니다. 따라서 그 메일박스에 있던 모든 메시지도 그대로 존재합니다.

##### 액터에는 무슨 일이 일어나는가(What happens to the Actor)

액터 내부의 코드가 예외를 던지면, 그 액터는 일시 중단(suspended)되고 슈퍼비전 프로세스가 시작됩니다. 슈퍼바이저의 결정에 따라 액터는 (아무 일도 없었던 것처럼) 재개(resumed)되거나, (내부 상태를 지우고 처음부터 시작하며) 재시작(restarted)되거나, 종료(terminated)됩니다.

---

### 9. 액터 참조, 경로, 주소(Actor References, Paths and Addresses)

이 장에서는 액터를 식별하고 위치를 파악하는 방법을 다룹니다.

#### 9.1 액터 참조(Actor References)

**액터 참조**(Actor References)는 `ActorRef`의 하위 타입(subtypes)으로, 액터에게 메시지를 보낼 수 있게 합니다. 문서는 다음과 같이 언급합니다: "액터 참조는 단일 액터(single actor)를 가리키며, 그 참조의 생명 주기는 그 액터의 생명 주기와 일치한다."

시스템 설정에 따라 다양한 참조 타입이 존재합니다.

- 로컬 참조(Local references) — 네트워킹되지 않은 시스템용
- 리모팅(remoting)이 활성화된 로컬 참조
- 원격 액터 참조(Remote actor references)
- `PromiseActorRef`, `DeadLetterActorRef`, `EmptyLocalActorRef` 같은 특수 타입(special types)

#### 9.2 액터 경로(Actor Paths)

**액터 경로**(Actor Paths)는 액터 시스템 내의 계층적 이름(hierarchical name)을 나타냅니다. 중요한 점은, "액터 경로는 액터가 거주할(inhabited) 수도 있고 그렇지 않을 수도 있는 이름을 나타내며, 경로 자체는 생명 주기를 가지지 않고 결코 무효화되지 않는다(never becomes invalid)"는 것입니다.

#### 9.3 핵심적 구분(Critical Distinction)

근본적인 차이점: 동일한 경로(same path)로 액터를 다시 생성할 수 있지만, 그것은 새로운 화신(new incarnation)이 됩니다. "이전 액터 참조로 보내진 메시지들은, 비록 동일한 경로를 가지고 있더라도 새로운 화신에게 전달되지 않습니다."

#### 9.4 경로 구조(Path Structure)

경로는 다음과 같은 형식을 따릅니다.

- 로컬: `"akka://my-sys/user/service-a/worker1"`
- 원격: `"akka://[email protected]:5678/user/service-b"`

#### 9.5 최상위 스코프(Top-Level Scopes)

액터 시스템은 다음과 같은 특정 네임스페이스 분기(namespace branches)를 예약합니다.

- `/user` — 사용자가 생성한 액터(user-created actors)용
- `/system` — 시스템 액터(system actors)용
- `/deadLetters` — 전달 불가능한 메시지(undeliverable messages)용
- `/temp` — 수명이 짧은 시스템 액터(short-lived system actors)용
- `/remote` — 원격 슈퍼바이저(remote supervisors)를 가진 액터용

문서는 액터 참조를 얻는 것이, 액터를 생성하거나 리셉셔니스트(Receptionist) 같은 조회 메커니즘(lookup mechanism)을 통해 이루어진다는 점을 강조합니다.

---

### 10. 메시지 전달 신뢰성(Message Delivery Reliability)

이 문서는 로컬 및 분산 시스템 전반에서 액터 간 메시지 전달에 대한 Akka의 접근 방식을 설명합니다.

#### 10.1 핵심 전달 보장(Core Delivery Guarantees)

Akka는 모든 메시지 송신에 대해 두 가지 근본적인 보장을 제공합니다.

1. **최대 한 번 전달(At-most-once delivery)** — 메시지가 손실될 수는 있지만, 인위적으로 중복(duplicate)되지는 않습니다.
2. **송신자-수신자 쌍별 메시지 순서(Message ordering per sender-receiver pair)** — 액터 A가 메시지 M1, M2, M3을 액터 B에게 직접 보내면, 그것들은 (도착한다면) 그 순서대로 도착합니다.

#### 10.2 왜 보장된 전달이 아닌가(Why Not Guaranteed Delivery?)

프레임워크는 "보장된 전달(guaranteed delivery)"이라는 개념 자체가 모호하기 때문에 이를 주장하지 않습니다. "보장(guaranteed)"이 무엇을 의미하는지 판단하려면, 메시지가 네트워크로 전송되는 것인지, 호스트에 수신되는 것인지, 메일박스에 놓이는 것인지, 처리가 시작되는 것인지, 처리가 성공적으로 완료되는 것인지를 명확히 해야 합니다. 각 수준은 서로 다른 비용과 과제를 가집니다.

Akka는 "누수 추상화(leaky abstraction)"를 만드는 대신, 이러한 실패 가능성(fallibility)을 명시적으로 받아들입니다. 송신자가 성공을 확인할 수 있는 유일하게 신뢰할 수 있는 방법은, 수신자로부터 비즈니스 수준의 확인 응답 메시지(business-level acknowledgment message)를 받는 것입니다.

#### 10.3 로컬 vs. 원격 동작(Local vs. Remote Behavior)

**로컬(JVM 내부) 송신**(Local (in-JVM) sends)은 메시지가 네트워크 문제 없이 직접 전달되므로 실질적으로 더 강한 신뢰성을 가집니다. 그러나 다음과 같은 이유로 여전히 실패할 수 있습니다.

- 스택 오버플로(stack overflow)
- 메모리 부족 오류(out-of-memory errors)
- 가득 찬 경계 메일박스(full bounded mailboxes)
- 액터 종료(actor termination)

**원격 송신**(Remote sends)은 추가적인 지연(latency) 과제에 직면합니다. 메시지 순서 보장은, 네트워크 전달 시간이 가변적이기 때문에 중개자(intermediaries)를 가로질러 이행적으로(transitively) 확장되지 않습니다.

#### 10.4 더 강한 신뢰성 구축(Building Stronger Reliability)

보장된 전달이 필요한 애플리케이션을 위해, Akka는 상위 수준 패턴(higher-level patterns)을 제공합니다.

- 중복 제거(deduplication)를 갖춘 ACK-RETRY 프로토콜을 사용하는 **신뢰성 있는 전달(Reliable Delivery)** 기능
- 상태 재생을 위한 Akka Persistence를 통한 **이벤트 소싱(Event Sourcing)**
- 명시적 확인 응답(explicit acknowledgment)을 갖춘 커스텀 메일박스 구현

#### 10.5 데드 레터(Dead Letters)

전달될 수 없는 메시지는 최선 노력(best-effort) 기반으로 `/deadLetters`라는 합성 액터(synthetic actor)에 도달합니다. 이는 전달 자체를 보장하기보다는 주로 디버깅(debugging)에 유용합니다.

---

### 11. 설정(Configuration)

#### 11.1 개요(Overview)

이 페이지는 Akka 애플리케이션을 설정(configure)하는 방법을 설명합니다. "합리적인 기본값(sensible default values)이 제공"되므로 어떠한 설정 없이도 Akka를 사용할 수 있지만, 나중에 로깅(logging), 클러스터링(clustering), 직렬화기(serializers), 디스패처(dispatchers) 등의 설정을 조정해야 할 수 있습니다.

#### 11.2 설정 소스 계층(Configuration Source Hierarchy)

Akka는 Typesafe Config 라이브러리를 사용합니다. 설정은 다음 순서로 로드됩니다.

1. 시스템 속성(System properties) — 가장 높은 우선순위
2. 애플리케이션 설정 파일(Application configuration files)
3. 레퍼런스 설정(Reference configuration) — 라이브러리 기본값

시스템은 클래스 패스 루트(class path root)에서 `application.conf`, `application.json`, `application.properties`를 찾으며, 이를 폴백(fallback)으로서의 `reference.conf` 파일들과 병합(merge)합니다.

#### 11.3 라이선스 요구 사항(License Requirements)

"Akka는 프로덕션(production)에서 사용하려면 라이선스 키(license key)가 필요합니다. 무료 키는 https://akka.io/key 에서 얻을 수 있습니다." 로컬 개발(local development)의 경우, Akka는 키 없이 동작하지만 "키가 설정되지 않으면 일정 시간 후 종료(terminate after a while)"됩니다. 문서에는 라이선스 만료 전에 만료를 확인하는 방법을 보여 주는 코드 예제가 포함되어 있습니다.

#### 11.4 주요 설정 주제(Key Configuration Topics)

**커스텀 설정 파일(Custom Configuration Files):** 애플리케이션은 설정을 `application.conf`에 보관해야 하며, 라이브러리는 `reference.conf`를 사용합니다. 이 페이지는 로거 설정(logger configuration), 로그 레벨(log levels), 액터 시스템 설정을 보여 주는 예제 `application.conf`를 제공합니다. 예시:

```hocon
akka {
  # 시작 시 사용되는 로거; 표준 출력 로거(stdout-logger)를
  # 애플리케이션이 시작될 때까지 대체합니다.
  loggers = ["akka.event.slf4j.Slf4jLogger"]

  # akka.event.Logging.LogLevel 중 하나로 로그 레벨을 지정합니다:
  # ERROR, WARNING, INFO, DEBUG
  loglevel = "DEBUG"

  # 시작 시 사용되는 로깅이 메시지를 출력할 로그 레벨을 지정합니다.
  stdout-loglevel = "DEBUG"
}
```

**파일 포함(File Inclusion):** `include "application"` 지시문을 사용하여 다른 설정 파일을 포함할 수 있으며, 이는 환경별 재정의(environment-specific overrides)에 유용합니다.

**다중 액터 시스템(Multiple Actor Systems):** 여러 개의 `ActorSystem` 인스턴스를 실행하는 경우, 계층적 키(hierarchical keys)와 `withFallback()`을 사용하는 "하위 트리 들어올리기(lift-a-subtree)" 기법으로 그 설정들을 분리합니다.

**커스텀 위치(Custom Locations):** `-Dconfig.resource`, `-Dconfig.file`, `-Dconfig.url` 같은 시스템 속성을 사용하거나, `ConfigFactory` 메서드를 통해 프로그래밍 방식으로 기본 `application.conf`를 재정의할 수 있습니다.

**로깅 설정(Logging Configuration):** `akka.log-config-on-start = on`을 설정하면, 시작 시 전체 설정(complete configuration)을 로깅하여 어떤 설정이 활성화되어 있는지 확인하는 데 도움이 됩니다.

문서 전반에는 실용적인 구현 패턴을 보여 주는 Scala 및 Java 코드 예제가 포함되어 있습니다.

---

### 12. 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Introduction to Akka Libraries](https://doc.akka.io/libraries/akka-core/current/typed/guide/introduction.html)
- [Why modern systems need a new programming model](https://doc.akka.io/libraries/akka-core/current/typed/guide/actors-motivation.html)
- [How the Actor Model Meets the Needs of Modern, Distributed Systems](https://doc.akka.io/libraries/akka-core/current/typed/guide/actors-intro.html)
- [Overview of Akka libraries and modules](https://doc.akka.io/libraries/akka-core/current/typed/guide/modules.html)
- [Terminology and Concepts](https://doc.akka.io/libraries/akka-core/current/general/terminology.html)
- [Actor Systems](https://doc.akka.io/libraries/akka-core/current/general/actor-systems.html)
- [What is an Actor?](https://doc.akka.io/libraries/akka-core/current/general/actors.html)
- [Supervision and Monitoring](https://doc.akka.io/libraries/akka-core/current/general/supervision.html)
- [Actor References, Paths and Addresses](https://doc.akka.io/libraries/akka-core/current/general/addressing.html)
- [Message Delivery Reliability](https://doc.akka.io/libraries/akka-core/current/general/message-delivery-reliability.html)
- [Configuration](https://doc.akka.io/libraries/akka-core/current/general/configuration.html)

---

## Akka 튜토리얼: IoT 시스템 만들기

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial.html

---

### 목차

1. [예제 소개 (Introduction to the Example)](#1-예제-소개-introduction-to-the-example)
2. [Part 1: 액터 아키텍처 (Actor Architecture)](#2-part-1-액터-아키텍처-actor-architecture)
   - [Akka 액터 계층 구조 (The Akka Actor Hierarchy)](#akka-액터-계층-구조-the-akka-actor-hierarchy)
   - [첫 번째 액터 (The First Actor)](#첫-번째-액터-the-first-actor)
   - [액터 생명주기 (The Actor Lifecycle)](#액터-생명주기-the-actor-lifecycle)
   - [장애 처리 (Failure Handling)](#장애-처리-failure-handling)
3. [Part 2: 첫 번째 액터 만들기 (Creating the First Actor)](#3-part-2-첫-번째-액터-만들기-creating-the-first-actor)
4. [Part 3: 디바이스 액터 다루기 (Working with Device Actors)](#4-part-3-디바이스-액터-다루기-working-with-device-actors)
   - [디바이스에 대한 메시지 식별 (Identifying Messages for Devices)](#디바이스에-대한-메시지-식별-identifying-messages-for-devices)
   - [메시지 전달 (Message Delivery)](#메시지-전달-message-delivery)
   - [메시지 순서 (Message Ordering)](#메시지-순서-message-ordering)
   - [디바이스 메시지에 유연성 추가하기](#디바이스-메시지에-유연성-추가하기)
   - [디바이스 액터와 읽기 프로토콜 구현](#디바이스-액터와-읽기-프로토콜-구현)
   - [액터 테스트](#액터-테스트)
   - [쓰기 프로토콜 추가](#쓰기-프로토콜-추가)
   - [읽기/쓰기 메시지를 가진 액터](#읽기쓰기-메시지를-가진-액터)
5. [Part 4: 디바이스 그룹 다루기 (Working with Device Groups)](#5-part-4-디바이스-그룹-다루기-working-with-device-groups)
   - [디바이스 매니저 계층 구조](#디바이스-매니저-계층-구조)
   - [등록 프로토콜](#등록-프로토콜)
   - [디바이스 그룹 액터에 등록 지원 추가하기](#디바이스-그룹-액터에-등록-지원-추가하기)
   - [그룹 내 디바이스 액터 추적하기](#그룹-내-디바이스-액터-추적하기)
   - [디바이스 매니저 액터 만들기](#디바이스-매니저-액터-만들기)
   - [테스트 케이스](#테스트-케이스)
6. [Part 5: 디바이스 그룹 조회하기 (Querying Device Groups)](#6-part-5-디바이스-그룹-조회하기-querying-device-groups)
   - [동적 액터 멤버십 처리](#동적-액터-멤버십-처리)
   - [메시지 프로토콜](#메시지-프로토콜)
   - [DeviceGroupQuery 액터 구현](#devicegroupquery-액터-구현)
   - [쿼리를 DeviceGroup에 통합하기](#쿼리를-devicegroup에-통합하기)
   - [테스트](#테스트)
7. [참고 자료](#참고-자료)

---

### 1. 예제 소개 (Introduction to the Example)

이 튜토리얼은 액터 기반 시스템 설계를 이해하는 데 도움을 주기 위해 만들어진 Akka IoT 튜토리얼을 소개합니다. 이 가이드는 온도 센서 관리 시스템을 실용적인 예제로 사용합니다.

#### 주요 학습 목표

독자는 다음을 학습하게 됩니다.

- 액터 계층 구조(actor hierarchy)와 그것이 액터 동작(behavior)에 어떻게 영향을 미치는지
- 적절한 액터 단위(actor granularity, 액터의 입도/세분화 정도) 선택하기
- 메시징을 통한 프로토콜 정의
- 전형적인 대화형 상호작용 패턴(conversational interaction patterns)

#### IoT 사용 사례 (Use Case)

이 튜토리얼은 두 가지 주요 구성 요소로 이루어진 사물인터넷(Internet of Things, IoT) 애플리케이션을 구축하는 데 초점을 맞춥니다.

1. **디바이스 데이터 수집(Device data collection)** — 원격 디바이스의 표현(representation)을 관리하며, 여러 센서를 디바이스 그룹(device group)으로 조직화합니다.
2. **사용자 대시보드(User dashboard)** — 디바이스로부터 주기적으로 데이터를 수집하고 리포트를 생성합니다.

이 예제는 고객이 여러 구역의 측정값을 확인할 수 있도록, 가정용 센서의 온도 측정값(temperature readings)에 초점을 맞춥니다.

#### 사전 준비와 구조

독자는 먼저 Hello World 예제를 완료해야 합니다. 이 튜토리얼은 액터 아키텍처의 기초로 시작하여, 디바이스 액터(device actor) 생성, 그룹 관리(group management), 조회(querying) 시스템으로 발전해 나가는 다섯 개의 파트(part)로 진행됩니다.

#### 개발 지원

Java DSL과 Scala DSL이 함께 번들로 제공됩니다. 임포트(import) 제안 충돌을 방지하기 위해 Eclipse 및 IntelliJ에 대한 IDE 설정을 권장합니다. 본문 예제는 **Scala 코드**로 제시합니다.

---

### 2. Part 1: 액터 아키텍처 (Actor Architecture)

Akka를 사용하면 액터 시스템 인프라를 구축하고 기본적인 동작 관리를 위한 저수준(low-level) 코드를 작성하는 부담에서 벗어날 수 있습니다. 이 장점을 충분히 이해하려면, 개발자가 작성하는 액터들과 Akka가 내부적으로 생성·관리하는 액터들 사이의 관계, 그리고 액터 생명주기(actor lifecycle)와 장애(failure)가 어떻게 처리되는지를 이해해야 합니다.

#### Akka 액터 계층 구조 (The Akka Actor Hierarchy)

Akka에서 모든 액터는 반드시 부모(parent)를 가져야 합니다. 여러분은 `ActorContext.spawn()`을 호출하여 액터를 생성합니다. 이 동작을 통해 생성자(creator) 액터는 새로 생성된 자식(child) 액터의 부모가 됩니다. 그러면 자연스럽게 다음 질문이 떠오릅니다. "그렇다면 여러분이 생성하는 최초의 액터의 부모는 누구인가?"

계층 구조 다이어그램에 묘사되어 있듯이, 여러분의 모든 액터는 공통의 부모인 **사용자 가디언**(user guardian)을 공유합니다. 이 사용자 가디언은 `ActorSystem`을 시작할 때 생성되고 초기화됩니다. 첫 번째 Hello World 예제에서 다루었듯이, 액터를 생성하면 유효한 URL로서 기능하는 참조(reference)가 생성됩니다. 따라서 사용자 가디언으로부터 `context.spawn(someBehavior, "someActor")`를 사용하여 `someActor`라는 이름의 액터를 생성하면, 그 참조는 `/user/someActor` 경로를 따르게 됩니다.

여러분의 첫 번째 액터인 사용자 가디언이 동작을 시작하기 전에, Akka는 이미 시스템에 두 개의 추가적인 가디언 액터, 즉 `/`와 `/system`을 설정해 둔 상태입니다. 따라서 계층 구조의 정점(apex)에는 세 개의 가디언 액터가 존재합니다.

- **루트 가디언(root guardian)** — `/`에 위치하며, 시스템 내 모든 액터의 부모로서 기능하고, 시스템 자체가 종료될 때 가장 마지막에 종료되는 액터를 나타냅니다.
- **시스템 가디언(system guardian)** — `/system`에 위치하며, Akka 또는 Akka 위에 구축된 라이브러리가 시스템 네임스페이스 내에 자체 액터를 생성할 수 있는 곳입니다.
- **사용자 가디언(user guardian)** — `/user`에 위치하며, 애플리케이션의 다른 모든 액터를 시작하기 위해 여러분이 제공하는 최상위(top-level) 액터를 나타냅니다.

동작 중인 액터 계층 구조를 이해하는 가장 간단한 방법은 `ActorRef` 인스턴스를 살펴보는 것입니다. 간단한 실험으로, 액터를 하나 생성하여 그 참조를 출력하고, 그 액터의 자식을 생성하여 자식의 참조를 출력해 봅니다.

```scala
package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object PrintMyActorRefActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new PrintMyActorRefActor(context))
}

class PrintMyActorRefActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "printit" =>
        val secondRef = context.spawn(Behaviors.empty[String], "second-actor")
        println(s"Second: $secondRef")
        this
    }
}

object Main {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new Main(context))

}

class Main(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "start" =>
        val firstRef = context.spawn(PrintMyActorRefActor(), "first-actor")
        println(s"First: $firstRef")
        firstRef ! "printit"
        this
    }
}

object ActorHierarchyExperiments extends App {
  val testSystem = ActorSystem(Main(), "testSystem")
  testSystem ! "start"
}
```

콘솔 출력은 다음과 같습니다.

```
First: Actor[akka://testSystem/user/first-actor#1053618476]
Second: Actor[akka://testSystem/user/first-actor/second-actor#-1544706041]
```

출력을 살펴보면 참조의 구조를 알 수 있습니다. 두 경로 모두 `akka://testSystem/`로 시작합니다. 액터 참조는 유효한 URL로서 기능하기 때문에 `akka://`가 프로토콜(protocol) 필드 값으로 사용됩니다. 이후, 웹 주소와 마찬가지로 URL은 시스템을 식별합니다. 이 예에서 시스템의 이름은 `testSystem`이지만 다른 어떤 것이든 될 수 있습니다. 여러 시스템 간의 원격 통신(remote communication)이 활성화되면, URL의 이 부분에 호스트명(hostname)이 포함되어 다른 시스템이 네트워크에서 그것을 찾을 수 있게 합니다.

두 번째 액터의 참조가 `/first-actor/` 경로를 포함하고 있으므로, 이는 두 번째 액터가 첫 번째 액터의 자식임을 나타냅니다. 액터 참조의 마지막 구성 요소인 `#1053618476` 또는 `#-1544706041`과 같은 것은 고유 식별자(unique identifier)를 나타내며, 일반적으로 무시해도 됩니다.

이제 액터 계층 구조의 형태를 이해했으니, 자연스럽게 "왜 이런 계층 구조가 필요한가? 어떤 목적을 위한 것인가?"라는 질문이 떠오릅니다.

계층 구조는 액터 생명주기를 안전하게 감독(oversee)하는 중요한 역할을 합니다. 다음에는 이 측면을 살펴보고, 이 이해가 코드 품질을 어떻게 향상시키는지 알아보겠습니다.

#### 액터 생명주기 (The Actor Lifecycle)

액터는 생성될 때 존재하게 되며, 이후 사용자의 요청에 따라 정지(halt)될 수 있습니다. 액터가 정지되면, 그 모든 자식들도 재귀적으로(recursively) 함께 정지됩니다. 이 특성은 리소스 정리(resource cleanup)를 크게 간소화하고, 닫히지 않은 소켓(socket)이나 파일로 인한 리소스 누수(resource leak)를 방지합니다. 저수준 멀티스레드 프로그래밍에서는 다양한 동시성 리소스의 생명주기 관리가 종종 간과되는 어려운 과제입니다.

액터를 정지시키기 위해 권장되는 방식은 액터 내부에서 `Behaviors.stopped`를 반환하는 것이며, 이는 일반적으로 사용자 정의 정지 메시지에 대한 응답으로 또는 액터의 할당된 작업이 완료되었을 때 이루어집니다. 부모로부터 `context.stop(childRef)`를 호출하여 자식 액터를 정지시키는 것도 기술적으로는 가능하지만, 이 메커니즘으로 임의의(자식이 아닌) 액터를 종료시키는 것은 불가능합니다.

Akka 액터 API는 `PostStop`을 포함한 특정 생명주기 시그널(lifecycle signals)을 제공하는데, `PostStop`은 액터가 종료된 직후에 디스패치(dispatch)됩니다. 이 시그널 이후로는 더 이상 메시지가 처리되지 않습니다.

다음은 `PostStop` 시그널을 통한 생명주기를 보여주는 예제입니다.

```scala
object StartStopActor1 {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new StartStopActor1(context))
}

class StartStopActor1(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("first started")
  context.spawn(StartStopActor2(), "second")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "stop" => Behaviors.stopped
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("first stopped")
      this
  }

}

object StartStopActor2 {
  def apply(): Behavior[String] =
    Behaviors.setup(new StartStopActor2(_))
}

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("second started")

  override def onMessage(msg: String): Behavior[String] = {
    // no messages handled by this actor
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("second stopped")
      this
  }

}
```

사용 예시는 다음과 같습니다.

```scala
val first = context.spawn(StartStopActor1(), "first")
first ! "stop"
```

콘솔 출력은 다음과 같습니다.

```
first started
second started
second stopped
first stopped
```

첫 번째 액터를 정지시켰을 때, 그것은 자기 자신을 종료하기 전에 자식 액터를 정지시켰습니다. 이 순서는 엄격합니다. 즉, 자식들로부터의 모든 `PostStop` 시그널은 부모의 `PostStop` 시그널이 처리되기 전에 처리됩니다.

#### 장애 처리 (Failure Handling)

부모와 자식은 그들의 생애 동안 연결을 유지합니다. 액터가 장애를 겪을 때마다(예외를 던지거나 메시지 핸들러에서 처리되지 않은 예외가 발생하는 경우), 장애 정보는 **감독 전략**(supervision strategy)으로 전달되며, 감독 전략이 그 결과로 발생한 예외를 어떻게 관리할지 결정합니다. 감독 전략은 보통 부모 액터가 자식을 생성할 때 설정됩니다. 이러한 방식으로 부모는 자식의 감독자(supervisor)로서 기능합니다. **기본 감독 전략(default supervisor strategy)은 자식을 종료시킵니다.** 전략이 정의되지 않으면 모든 장애는 종료로 귀결됩니다.

장애 발생 후, 감독되는 액터는 재시작 작업이 시작되기 전에 `PreRestart` 시그널을 받습니다. 이후 액터가 재시작됩니다.

다음은 재시작(restart) 감독 전략을 사용하는 예제입니다.

```scala
object SupervisingActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisingActor(context))
}

class SupervisingActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  private val child = context.spawn(
    Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart),
    name = "supervised-actor")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "failChild" =>
        child ! "fail"
        this
    }
}

object SupervisedActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisedActor(context))
}

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised actor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("supervised actor fails now")
        throw new Exception("I failed!")
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("supervised actor will be restarted")
      this
    case PostStop =>
      println("supervised actor stopped")
      this
  }

}
```

사용 예시는 다음과 같습니다.

```scala
val supervisingActor = context.spawn(SupervisingActor(), "supervising-actor")
supervisingActor ! "failChild"
```

콘솔 출력은 다음과 같습니다.

```
supervised actor started
supervised actor fails now
supervised actor will be restarted
supervised actor started
[ERROR] [11/12/2018 12:03:27.171] [ActorHierarchyExperiments-akka.actor.default-dispatcher-2] [akka://ActorHierarchyExperiments/user/supervising-actor/supervised-actor] Supervisor akka.actor.typed.internal.RestartSupervisor@1c452254 saw failure: I failed!
java.lang.Exception: I failed!
	at typed.tutorial_1.SupervisedActor.onMessage(ActorHierarchyExperiments.scala:113)
	at typed.tutorial_1.SupervisedActor.onMessage(ActorHierarchyExperiments.scala:106)
	at akka.actor.typed.scaladsl.AbstractBehavior.receive(AbstractBehavior.scala:59)
	at akka.actor.typed.Behavior$.interpret(Behavior.scala:395)
	at akka.actor.typed.Behavior$.interpretMessage(Behavior.scala:369)
	at akka.actor.typed.internal.InterceptorImpl$$anon$2.apply(InterceptorImpl.scala:49)
	at akka.actor.typed.internal.SimpleSupervisor.aroundReceive(Supervision.scala:85)
	at akka.actor.typed.internal.InterceptorImpl.receive(InterceptorImpl.scala:70)
	at akka.actor.typed.Behavior$.interpret(Behavior.scala:395)
	at akka.actor.typed.Behavior$.interpretMessage(Behavior.scala:369)
```

폴트 톨러런스(fault tolerance, 장애 내성)와 감독 전략에 대한 포괄적인 세부 사항은 폴트 톨러런스 레퍼런스 문서를 참고하시기 바랍니다. 해당 문서는 이러한 메커니즘과 구현 세부 사항에 대해 더 깊이 있게 다룹니다.

#### 요약

지금까지 Akka가 액터를 계층적으로 관리하는 방식을 다루었으며, 부모가 자식을 감독하고 예외를 관리하는 방법을 보여주었습니다. 기본적인 액터와 그 자식을 구성하는 메커니즘을 살펴보았습니다. 다음으로, 이 지식을 예제 시나리오에 적용하여 디바이스 액터로부터 정보를 가져오는 데 필요한 통신을 모델링할 것입니다. 그 후에는 그룹으로 조직화된 액터들의 관리를 다룰 것입니다.

---

### 3. Part 2: 첫 번째 액터 만들기 (Creating the First Actor)

액터 계층 구조와 동작을 이해하고 나면, 최상위 IoT 시스템 구성 요소를 어떻게 액터로 매핑할지가 다음 질문이 됩니다. 사용자 가디언(user guardian)은 애플리케이션 전체를 나타내는 최상위 액터입니다. 디바이스와 대시보드를 관리하는 구성 요소들은 이 액터의 자식이 되어, 예제 아키텍처를 트리 구조(tree structure)로 구성할 수 있습니다.

최초의 액터인 `IotSupervisor`는 단 몇 줄의 코드만 필요합니다. 튜토리얼 애플리케이션을 시작하려면 다음과 같이 합니다.

1. `com.example` 패키지에 새로운 `IotSupervisor` 소스 파일을 생성합니다.
2. 다음 코드를 새 파일에 붙여넣어 `IotSupervisor`를 정의합니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object IotSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new IotSupervisor(context))
}

class IotSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("IoT Application started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("IoT Application stopped")
      this
  }
}
```

이 코드는 앞서의 액터 예제들과 유사하지만, `println()` 대신 Akka의 통합 로깅 기능(logging facility)을 사용합니다.

액터 시스템을 생성하는 `main` 진입점(entry point)을 제공하기 위해, 새로운 `IotApp` 오브젝트에 다음 코드를 추가합니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorSystem

object IotApp {

  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    ActorSystem[Nothing](IotSupervisor(), "iot-system")
  }

}
```

이 애플리케이션은 시작 상태를 로깅하는 것 외에 최소한의 작업만 수행합니다. 하지만 기초가 되는 액터가 확립되었으며, 추가적인 액터를 받아들일 준비가 되었습니다.

#### 다음 단계

이어지는 장(chapter)들은 다음을 통해 애플리케이션을 점진적으로 확장합니다.

1. 디바이스 표현(device representation) 생성하기
2. 디바이스 관리 구성 요소(device management component) 생성하기
3. 디바이스 그룹에 조회(query) 기능 추가하기

---

### 4. Part 3: 디바이스 액터 다루기 (Working with Device Actors)

이전 파트들에서는 액터 시스템을 **거시적으로(in the large)** 바라보는 방법, 즉 구성 요소를 어떻게 표현하고 계층 구조 안에 어떻게 배치할지를 설명했습니다. 이번 파트에서는 디바이스 액터를 구현함으로써 액터를 **미시적으로(in the small)** 살펴봅니다.

객체(object)를 다룰 때는 일반적으로 API를 **인터페이스(interface)**, 즉 실제 구현으로 채워질 추상 메서드들의 모음으로 설계합니다. 액터의 세계에서는 **프로토콜**(protocol)이 인터페이스의 역할을 대신합니다. 프로그래밍 언어로 일반적인 프로토콜을 형식화(formalize)하는 것은 불가능하지만, 그 가장 기본적인 요소인 **메시지**(message)를 구성할 수는 있습니다. 따라서 디바이스 액터에 보내고자 하는 메시지를 식별하는 것부터 시작하겠습니다.

일반적으로 메시지는 범주(category) 또는 패턴(pattern)으로 분류됩니다. 이러한 패턴을 식별하면 그것들 중에서 선택하고 구현하기가 더 쉬워집니다. 첫 번째 예제는 **요청-응답(request-respond)** 메시지 패턴을 보여줍니다.

#### 디바이스에 대한 메시지 식별 (Identifying Messages for Devices)

디바이스 액터의 작업은 간단합니다.

- 온도 측정값을 수집한다.
- 요청을 받으면, 마지막으로 측정한 온도를 보고한다.

하지만 디바이스는 즉시 온도 측정값을 갖지 못한 채 시작될 수 있습니다. 따라서 온도가 존재하지 않는 경우를 고려해야 합니다. 이것은 또한 쓰기(write) 부분이 없는 상태에서 액터의 조회(query) 부분을 테스트할 수 있게 해 주는데, 디바이스 액터가 빈 결과(empty result)를 보고할 수 있기 때문입니다.

디바이스 액터로부터 현재 온도를 얻기 위한 프로토콜은 간단합니다. 액터는 다음을 수행합니다.

1. 현재 온도에 대한 요청을 기다린다.
2. 다음 중 하나로 요청에 응답한다.
   - 현재 온도를 포함하거나,
   - 아직 온도를 사용할 수 없음을 나타낸다.

두 개의 메시지, 즉 요청용 하나와 응답용 하나가 필요합니다. 첫 시도는 다음과 같을 수 있습니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef

object Device {
  sealed trait Command
  final case class ReadTemperature(replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(value: Option[Double])
}
```

`ReadTemperature` 메시지는 디바이스 액터가 요청에 응답할 때 사용할 `ActorRef[RespondTemperature]`를 포함하고 있다는 점에 유의하세요.

이 두 메시지는 필요한 기능을 다루는 것처럼 보입니다. 하지만 우리가 선택하는 접근 방식은 애플리케이션의 분산적(distributed) 특성을 고려해야 합니다. 로컬 JVM 상의 액터와 통신하는 기본 메커니즘은 원격 액터와 통신하는 것과 동일하지만, 다음 사항을 염두에 두어야 합니다.

- 로컬 메시지와 원격 메시지 사이에는 전달 지연(latency)에서 관찰 가능한 차이가 있을 것입니다. 네트워크 링크 대역폭(bandwidth)과 메시지 크기 같은 요인도 작용하기 때문입니다.
- 원격 메시지 전송은 더 많은 단계를 수반하므로, 더 많은 것이 잘못될 수 있다는 점에서 신뢰성(reliability)이 우려됩니다.
- 로컬 전송은 동일한 JVM 내에서 메시지에 대한 참조를 전달하며, 전송되는 기반 객체에 대한 제약이 없습니다. 반면 원격 전송(remote transport)은 메시지 크기에 제한을 둡니다.

또한, 동일한 JVM 내에서의 전송은 훨씬 더 신뢰성이 높지만, 액터가 메시지를 처리하는 도중 프로그래머의 오류로 인해 실패하면, 그 효과는 원격 호스트가 메시지를 처리하는 중에 크래시되어 원격 네트워크 요청이 실패한 것과 동일합니다. 두 경우 모두 서비스는 얼마 후 복구되지만(액터는 그 감독자에 의해 재시작되고, 호스트는 운영자나 모니터링 시스템에 의해 재시작됨), 크래시 동안 개별 요청들은 손실됩니다. **따라서 모든 메시지가 손실될 가능성이 있다고 가정하고 액터를 작성하는 것이 안전하고 비관적인(pessimistic) 선택입니다.**

프로토콜에서 유연성이 필요한 이유를 더 잘 이해하려면 Akka의 메시지 순서(message ordering)와 메시지 전달 보장(message delivery guarantees)을 고려하는 것이 도움이 됩니다. Akka는 메시지 전송에 대해 다음과 같은 동작을 제공합니다.

- **최대 한 번 전달(at-most-once delivery)**, 즉 전달이 보장되지 않음.
- 메시지 순서는 **송신자-수신자 쌍(sender, receiver pair)별로** 유지됨.

다음 절들에서 이 동작을 더 자세히 다룹니다.

##### 메시지 전달 (Message Delivery)

메시징 서브시스템이 제공하는 전달 의미론(delivery semantics)은 일반적으로 다음 범주에 속합니다.

- **최대 한 번 전달(At-most-once delivery)** — 각 메시지는 0번 또는 1번 전달됩니다. 더 일상적인 표현으로는 메시지가 손실될 수는 있지만 절대 중복되지 않는다는 뜻입니다.
- **최소 한 번 전달(At-least-once delivery)** — 각 메시지를 적어도 한 번 성공할 때까지 여러 번 전달 시도가 이루어질 수 있습니다. 다시 말해, 메시지가 중복될 수는 있지만 절대 손실되지 않는다는 뜻입니다.
- **정확히 한 번 전달(Exactly-once delivery)** — 각 메시지가 수신자에게 정확히 한 번 전달됩니다. 메시지는 손실될 수도, 중복될 수도 없습니다.

첫 번째 동작인 Akka가 사용하는 방식은 가장 저렴하며 가장 높은 성능을 냅니다. 송신 측이나 전송 메커니즘에 상태를 유지하지 않고 발사 후 망각(fire-and-forget) 방식으로 처리할 수 있기 때문에 구현 오버헤드가 가장 적습니다. 두 번째인 최소 한 번 전달은 전송 손실을 상쇄하기 위해 재시도(retry)가 필요합니다. 이는 송신 측에 상태를 유지하고 수신 측에 확인 응답(acknowledgment) 메커니즘을 두는 오버헤드를 추가합니다. 정확히 한 번 전달은 가장 비싸며 최악의 성능을 냅니다. 최소 한 번 전달이 추가하는 오버헤드에 더해, 중복 전달을 걸러내기 위해 수신 측에 상태를 유지해야 합니다.

액터 시스템에서는 보장(guarantee)의 정확한 의미, 즉 시스템이 어느 시점에 전달이 완료되었다고 간주하는지를 정해야 합니다.

1. 메시지가 네트워크로 전송될 때?
2. 메시지가 대상 액터의 호스트에 수신될 때?
3. 메시지가 대상 액터의 메일박스(mailbox)에 들어갈 때?
4. 메시지 대상 액터가 메시지 처리를 시작할 때?
5. 대상 액터가 메시지를 성공적으로 처리했을 때?

보장된 전달을 주장하는 대부분의 프레임워크와 프로토콜은 실제로는 4번이나 5번과 유사한 것을 제공합니다. 합리적으로 들리지만, **실제로 유용할까요?** 그 함의를 이해하기 위해 간단하고 실용적인 예를 생각해 봅시다. 어떤 사용자가 주문을 넣으려고 하는데, 우리는 그것이 실제로 주문 데이터베이스의 디스크에 기록되었을 때에만 성공적으로 처리되었다고 주장하고 싶습니다.

메시지의 성공적인 처리에 의존한다면, 액터는 주문이 그것을 검증·처리하고 데이터베이스에 넣을 책임을 지는 내부 API에 제출되자마자 성공을 보고할 것입니다. 안타깝게도 API가 호출된 직후 다음 중 어느 것이든 발생할 수 있습니다.

- 호스트가 크래시될 수 있습니다.
- 역직렬화(deserialization)가 실패할 수 있습니다.
- 검증(validation)이 실패할 수 있습니다.
- 데이터베이스를 사용할 수 없을 수 있습니다.
- 프로그래밍 오류가 발생할 수 있습니다.

이는 **전달의 보장**(guarantee of delivery)이 **도메인 수준의 보장**(domain level guarantee)으로 변환되지 않음을 보여줍니다. 우리는 주문이 실제로 완전히 처리되고 영속화(persist)되었을 때에만 성공을 보고하고 싶습니다. **성공을 보고할 수 있는 유일한 주체는 애플리케이션 자신입니다. 오직 애플리케이션만이 요구되는 도메인 보장을 이해하고 있기 때문입니다. 어떤 일반화된 프레임워크도 특정 도메인의 세부 사항과 그 도메인에서 무엇이 성공으로 간주되는지를 파악할 수 없습니다.**

이 특정한 예에서는, 데이터베이스가 주문이 이제 안전하게 저장되었음을 확인한, 성공적인 데이터베이스 쓰기 이후에만 성공을 알리고 싶습니다. **이러한 이유로 Akka는 보장의 책임을 애플리케이션 자체에 위임합니다. 즉, 여러분이 Akka가 제공하는 도구로 직접 그것을 구현해야 합니다. 이것은 여러분이 제공하고자 하는 보장에 대한 완전한 제어권을 줍니다.** 이제 애플리케이션 로직에 대해 추론하기 쉽게 만들어 주는, Akka가 제공하는 메시지 순서를 살펴봅시다.

##### 메시지 순서 (Message Ordering)

Akka에서는 주어진 한 쌍의 액터에 대해, 첫 번째 액터에서 두 번째 액터로 **직접(directly)** 보낸 메시지는 순서가 뒤바뀐 채로 수신되지 않습니다. "직접"이라는 단어는 이 보장이 tell 연산자(operator)를 사용하여 최종 목적지로 직접 보낼 때에만 적용되며, 중개자(mediator)를 사용할 때는 적용되지 않음을 강조합니다.

만약 다음과 같다면:

- 액터 `A1`이 메시지 `M1`, `M2`, `M3`을 `A2`로 보냅니다.
- 액터 `A3`이 메시지 `M4`, `M5`, `M6`을 `A2`로 보냅니다.

이는 Akka 메시지에 대해 다음을 의미합니다.

- `M1`이 전달되면, 반드시 `M2`와 `M3`보다 먼저 전달되어야 합니다.
- `M2`가 전달되면, 반드시 `M3`보다 먼저 전달되어야 합니다.
- `M4`가 전달되면, 반드시 `M5`와 `M6`보다 먼저 전달되어야 합니다.
- `M5`가 전달되면, 반드시 `M6`보다 먼저 전달되어야 합니다.
- `A2`는 `A1`로부터의 메시지와 `A3`으로부터의 메시지가 뒤섞인(interleaved) 형태로 볼 수 있습니다.
- 전달이 보장되지 않으므로, 어떤 메시지든 누락될 수 있습니다. 즉, `A2`에 도착하지 않을 수 있습니다.

이 보장은 좋은 균형을 이룹니다. 한 액터로부터의 메시지가 순서대로 도착하는 것은 쉽게 추론할 수 있는 시스템을 구축하는 데 편리합니다. 반면, 서로 다른 액터들로부터의 메시지가 뒤섞여 도착하는 것을 허용함으로써 액터 시스템의 효율적인 구현을 위한 충분한 자유를 제공합니다.

전달 보장에 대한 전체 세부 사항은 레퍼런스 페이지를 참고하세요.

#### 디바이스 메시지에 유연성 추가하기

우리의 첫 번째 조회 프로토콜은 정확했지만, 분산 애플리케이션 실행을 고려하지 않았습니다. (타임아웃된 요청 때문에) 디바이스 액터를 조회하는 액터에서 재전송(resend)을 구현하거나, 여러 액터를 조회하고자 한다면, 요청과 응답을 상관(correlate)시킬 수 있어야 합니다. 따라서 요청자가 ID를 제공할 수 있도록 메시지에 필드를 하나 더 추가합니다(이 코드는 이후 단계에서 앱에 추가할 것입니다).

**Scala:**

```scala
sealed trait Command
final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
final case class RespondTemperature(requestId: Long, value: Option[Double])
```

#### 디바이스 액터와 읽기 프로토콜 구현

Hello World 예제에서 배웠듯이, 각 액터는 자신이 받아들일 메시지의 타입을 정의합니다. 우리의 디바이스 액터는 주어진 조회에 대한 응답에 동일한 ID 파라미터를 사용할 책임이 있는데, 이는 다음과 같은 모습이 됩니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}
```

코드에서 다음을 주목하세요.

- 컴패니언 오브젝트(companion object)의 `apply` 메서드(Java에서는 정적 `create` 메서드)는 `Device` 액터의 `Behavior`를 어떻게 생성할지 정의합니다. 파라미터로는 디바이스의 ID와 그것이 속한 그룹이 포함되며, 이후에 사용할 것입니다.
- 앞서 추론했던 메시지들은 컴패니언 오브젝트(또는 Device 클래스)에 정의됩니다.
- `Device` 클래스에서 `lastTemperatureReading`의 값은 처음에 `None`(Java에서는 `Optional.empty()`)으로 설정되며, 조회를 받으면 액터가 이를 다시 보고합니다.

#### 액터 테스트

위 액터를 기반으로 테스트를 작성할 수 있습니다. 프로젝트의 테스트 트리(test tree)에 있는 `com.example` 패키지에, 다음 코드를 `DeviceSpec.scala`(또는 Java의 경우 `DeviceTest.java`) 파일에 추가합니다. (여기서는 ScalaTest를 사용하지만 Akka Testkit과 함께 다른 테스트 프레임워크를 사용할 수도 있습니다.)

이 테스트는 sbt 프롬프트에서 `test`를 실행하거나 `mvn test`를 실행하여 수행할 수 있습니다.

**Scala:**

```scala
package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Device._

  "Device actor" must {

    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId should ===(42)
      response.value should ===(None)
    }
  }
}
```

이제 액터는 센서로부터 메시지를 받았을 때 온도의 상태를 변경할 방법이 필요합니다.

#### 쓰기 프로토콜 추가

쓰기 프로토콜의 목적은 액터가 온도를 포함하는 메시지를 받았을 때 `currentTemperature` 필드를 갱신하는 것입니다. 다시 한번, 쓰기 프로토콜을 다음과 같은 매우 단순한 메시지로 정의하고 싶은 유혹이 듭니다.

**Scala:**

```scala
sealed trait Command
final case class RecordTemperature(value: Double) extends Command
```

하지만 이 접근 방식은 온도 기록 메시지의 송신자가 메시지가 처리되었는지 아닌지를 결코 확신할 수 없다는 점을 고려하지 않습니다. 우리는 Akka가 이러한 메시지의 전달을 보장하지 않으며 성공 알림 제공을 애플리케이션에 맡긴다는 것을 보았습니다. 우리의 경우, 마지막 온도 기록을 갱신한 후 송신자에게 확인 응답(acknowledgment)을 보내고 싶습니다. 예를 들어 `TemperatureRecorded` 메시지로 응답하는 것입니다. 온도 조회와 응답의 경우와 마찬가지로, 최대한의 유연성을 제공하기 위해 ID 필드를 포함하는 것도 좋은 생각입니다.

**Scala:**

```scala
final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
    extends Command
final case class TemperatureRecorded(requestId: Long)
```

#### 읽기/쓰기 메시지를 가진 액터

읽기와 쓰기 프로토콜을 함께 결합하면, 디바이스 액터는 다음 예제와 같은 모습이 됩니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command

  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
      extends Command
  final case class TemperatureRecorded(requestId: Long)
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case RecordTemperature(id, value, replyTo) =>
        context.log.info("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}
```

이제 읽기/조회 기능과 쓰기/기록 기능을 함께 동작시키는 새로운 테스트 케이스도 작성해야 합니다.

**Scala:**

```scala
"reply with latest temperature reading" in {
  val recordProbe = createTestProbe[TemperatureRecorded]()
  val readProbe = createTestProbe[RespondTemperature]()
  val deviceActor = spawn(Device("group", "device"))

  deviceActor ! Device.RecordTemperature(requestId = 1, 24.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

  deviceActor ! Device.ReadTemperature(requestId = 2, readProbe.ref)
  val response1 = readProbe.receiveMessage()
  response1.requestId should ===(2)
  response1.value should ===(Some(24.0))

  deviceActor ! Device.RecordTemperature(requestId = 3, 55.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 3))

  deviceActor ! Device.ReadTemperature(requestId = 4, readProbe.ref)
  val response2 = readProbe.receiveMessage()
  response2.requestId should ===(4)
  response2.value should ===(Some(55.0))
}
```

#### 다음 단계

지금까지 우리는 전체 아키텍처를 설계하기 시작했고, 도메인에 직접 대응하는 첫 번째 액터를 작성했습니다. 이제 디바이스 그룹과 디바이스 액터 자체를 유지 관리하는 책임을 지는 구성 요소를 만들어야 합니다.

---

### 5. Part 4: 디바이스 그룹 다루기 (Working with Device Groups)

가정의 온도를 모니터링하는 완전한 IoT 시스템에서, 디바이스 센서들은 프로토콜을 통해 연결되어 디바이스 매니저(device manager) 구성 요소에 등록됩니다. 시스템은 센서 상태를 유지하는 책임을 지는 액터를 조회(look up)하거나 생성하여 등록을 처리합니다. 액터는 확인 응답으로 응답하며 자신의 `ActorRef`를 노출합니다. 그러면 네트워킹 구성 요소는 이 참조를 사용하여 센서와 디바이스 액터 간에 디바이스 매니저를 거치지 않고 직접 통신합니다.

주요 아키텍처상의 과제는 적절한 액터 단위(actor granularity)를 선택하는 것입니다. 지침은 더 큰 단위(larger granularity)를 선호하되, 시스템이 다음을 필요로 할 때 더 세밀한 단위를 추가하라고 제안합니다. 더 높은 동시성(concurrency)이 필요할 때, 많은 상태를 가진 복잡한 대화가 필요할 때, 액터들 사이에 나눌 만큼 충분한 상태가 있을 때, 또는 서로 무관한 여러 책임이 있어 별도의 액터가 개별적인 장애를 다른 것들에 최소한의 영향을 주며 처리하도록 할 수 있을 때입니다.

#### 디바이스 매니저 계층 구조

디바이스 매니저는 3단계 액터 트리(three-level actor tree)로 모델링됩니다.

- **최상위 감독자(Top level supervisor)**: 디바이스를 위한 시스템 구성 요소를 나타내며, 디바이스 그룹과 디바이스 액터를 조회하고 생성하는 진입점 역할을 합니다.
- **그룹 액터(Group actors)**: 각각 하나의 그룹 ID(예: 하나의 가정)에 대한 디바이스 액터들을 감독합니다. 사용 가능한 디바이스들로부터 온도 측정값을 조회하는 것과 같은 서비스를 제공합니다.
- **디바이스 액터(Device actors)**: 실제 디바이스 센서와의 상호작용을 관리하며, 온도 측정값을 저장합니다.

이 아키텍처는 다음을 제공합니다: 그룹 내 장애 격리(isolation of failures), 그룹에 속한 디바이스 조회 간소화, 각 그룹이 전용 액터에서 동시에 실행되므로 병렬성(parallelism) 향상, 개별 디바이스 장애 격리, 그리고 네트워크 연결이 개별 디바이스 액터와 직접 통신함으로써 온도 측정값 수집 시 병렬성 향상입니다.

#### 등록 프로토콜

등록 프로토콜의 기능은 필요한 동작을 다음과 같이 정의합니다.

1. `DeviceManager`가 그룹 ID와 디바이스 ID를 가진 요청을 받으면, 그것을 기존 그룹 액터로 전달하거나, 새로운 디바이스 그룹 액터를 생성하고 요청을 전달합니다.
2. `DeviceGroup` 액터는 기존 디바이스 액터의 `ActorRef`로 응답하거나, 디바이스 액터를 하나 생성하고 그 새 액터 참조로 응답합니다.
3. 센서는 직접 메시징을 위해 디바이스 액터의 `ActorRef`를 받습니다.

등록 요청과 확인 응답에 사용되는 메시지는 다음과 같이 정의됩니다.

**Scala:**

```scala
final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
    extends DeviceManager.Command
    with DeviceGroup.Command

final case class DeviceRegistered(device: ActorRef[Device.Command])
```

#### 디바이스 그룹 액터에 등록 지원 추가하기

##### 등록 요청 처리

디바이스 그룹 액터는 기존 자식의 `ActorRef`로 응답하거나 하나를 생성해야 합니다. 디바이스 ID로 자식 액터를 조회하기 위해 `Map`이 사용됩니다.

**Scala:**

```scala
object DeviceGroup {
  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup(context => new DeviceGroup(context, groupId))

  trait Command
  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String)
      extends Command
}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

#### 그룹 내 디바이스 액터 추적하기

디바이스는 생겼다가 사라지므로, 맵(map)에서 디바이스 액터를 제거해야 합니다. 디바이스가 제거되면 그에 대응하는 액터도 정지합니다. Akka는 한 액터가 다른 액터를 감시(watch)하고 그것이 정지하면 알림을 받을 수 있게 해 주는 **데스 워치(Death Watch)** 기능을 제공합니다. 감시되던 액터가 정지하면, 감시자(watcher)는 감시되던 액터 참조를 담은 `Terminated(actorRef)` 시그널을 받습니다.

하나의 디바이스가 정지한 후에도 그룹은 계속 동작해야 하므로, `Terminated(actorRef)` 시그널을 처리하는 것이 필요합니다. 그룹의 디바이스 액터는 새 디바이스 액터가 생성될 때 그것을 감시하기 시작하고, 정지를 나타내는 알림이 오면 맵에서 디바이스 액터를 제거해야 합니다.

`Terminated`에만 의존하는 대신 커스텀 메시지를 사용하면 그 메시지에 디바이스 ID를 담을 수 있습니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device actor for {} has been terminated", deviceId)
        deviceIdToActor -= deviceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

디바이스 목록 조회 기능은 새로운 조회 메시지로 추가됩니다.

**Scala:**

```scala
final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
    extends DeviceManager.Command
    with DeviceGroup.Command

final case class ReplyDeviceList(requestId: Long, ids: Set[String])
```

디바이스 그룹 액터 구현은 디바이스 목록 조회를 처리하도록 갱신됩니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      case RequestDeviceList(requestId, gId, replyTo) =>
        if (gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        } else
          Behaviors.unhandled

      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device actor for {} has been terminated", deviceId)
        deviceIdToActor -= deviceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

디바이스 제거를 테스트하기 위해, 외부에서 정지를 허용하는 `Passivate` 메시지를 Device 액터에 추가합니다.

**Scala:**

```scala
case object Passivate extends Command
```

`Passivate`를 처리하는 Device 액터 구현은 다음과 같습니다.

**Scala:**

```scala
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
      extends Command
  final case class TemperatureRecorded(requestId: Long)

  case object Passivate extends Command
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case RecordTemperature(id, value, replyTo) =>
        context.log.info("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this

      case Passivate =>
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }
}
```

#### 디바이스 매니저 액터 만들기

디바이스 매니저 구성 요소의 진입점은, 그룹 액터가 디바이스 액터를 생성하는 방식과 유사하게, 필요에 따라(on-demand) 디바이스 그룹 액터를 생성합니다.

**Scala:**

```scala
object DeviceManager {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new DeviceManager(context))

  sealed trait Command

  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
      extends DeviceManager.Command
      with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
      extends DeviceManager.Command
      with DeviceGroup.Command

  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends DeviceManager.Command
}

class DeviceManager(context: ActorContext[DeviceManager.Command])
    extends AbstractBehavior[DeviceManager.Command](context) {
  import DeviceManager._

  var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]

  context.log.info("DeviceManager started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(groupId, _, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! trackMsg
          case None =>
            context.log.info("Creating device group actor for {}", groupId)
            val groupActor = context.spawn(DeviceGroup(groupId), "group-" + groupId)
            context.watchWith(groupActor, DeviceGroupTerminated(groupId))
            groupActor ! trackMsg
            groupIdToActor += groupId -> groupActor
        }
        this

      case req @ RequestDeviceList(requestId, groupId, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! req
          case None =>
            replyTo ! ReplyDeviceList(requestId, Set.empty)
        }
        this

      case DeviceGroupTerminated(groupId) =>
        context.log.info("Device group actor for {} has been terminated", groupId)
        groupIdToActor -= groupId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceManager stopped")
      this
  }
}
```

#### 테스트 케이스

다음 테스트 케이스들은 디바이스 그룹 기능을 검증합니다.

**Scala:**

```scala
"be able to register a device actor" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered1 = probe.receiveMessage()
  val deviceActor1 = registered1.device

  // another deviceId
  groupActor ! RequestTrackDevice("group", "device2", probe.ref)
  val registered2 = probe.receiveMessage()
  val deviceActor2 = registered2.device
  deviceActor1 should !==(deviceActor2)

  // Check that the device actors are working
  val recordProbe = createTestProbe[TemperatureRecorded]()
  deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 0))
  deviceActor2 ! Device.RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))
}

"ignore requests for wrong groupId" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("wrongGroup", "device1", probe.ref)
  probe.expectNoMessage(500.milliseconds)
}

"return same actor for same deviceId" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered1 = probe.receiveMessage()

  // registering same again should be idempotent
  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered2 = probe.receiveMessage()

  registered1.device should ===(registered2.device)
}

"be able to list active devices" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  registeredProbe.receiveMessage()

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  registeredProbe.receiveMessage()

  val deviceListProbe = createTestProbe[ReplyDeviceList]()
  groupActor ! RequestDeviceList(requestId = 0, groupId = "group", deviceListProbe.ref)
  deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))
}

"be able to list active devices after one shuts down" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  val registered1 = registeredProbe.receiveMessage()
  val toShutDown = registered1.device

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  registeredProbe.receiveMessage()

  val deviceListProbe = createTestProbe[ReplyDeviceList]()
  groupActor ! RequestDeviceList(requestId = 0, groupId = "group", deviceListProbe.ref)
  deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))

  toShutDown ! Passivate
  registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

  // using awaitAssert to retry because it might take longer for the groupActor
  // to see the Terminated, that order is undefined
  registeredProbe.awaitAssert {
    groupActor ! RequestDeviceList(requestId = 1, groupId = "group", deviceListProbe.ref)
    deviceListProbe.expectMessage(ReplyDeviceList(requestId = 1, Set("device2")))
  }
}
```

#### 다음 단계

디바이스를 등록하고 추적하며 측정값을 기록하는 계층적 구성 요소가 이제 완성되었습니다. 이 구현은 여러 대화 패턴(conversation pattern)을 보여줍니다. 온도 기록을 위한 요청-응답(request-respond), 디바이스 등록을 위한 필요시 생성(create-on-demand), 그리고 정리(cleanup)를 동반한 부모-자식 관계 수립을 위한 생성-감시-종료(create-watch-terminate)입니다.

다음 장에서는 **스캐터-개더(scatter-gather)** 대화 패턴을 수립하는 그룹 조회(group query) 기능을 소개하며, 사용자가 그룹에 속한 모든 디바이스의 상태를 조회할 수 있게 하는 기능을 구현합니다.

---

### 6. Part 5: 디바이스 그룹 조회하기 (Querying Device Groups)

이 파트에서는 그룹 내 모든 디바이스 액터를 조회하는 **스캐터-개더 조회 패턴**(scatter-gather query pattern)을 구현합니다. 핵심 과제는 **그룹의 멤버십이 동적(dynamic)이라는 점입니다. 각 센서 디바이스는 언제든지 정지할 수 있는 액터로 표현됩니다.**

#### 동적 액터 멤버십 처리

이 구현은 다음과 같은 특정 동작들로 정리됩니다.

- 조회(query)가 도착하면, 그룹 액터는 기존 디바이스 액터들의 **스냅샷**(snapshot)을 취하고, 그 액터들에게만 온도를 요청합니다.
- 조회가 도착한 **이후에** 시작되는 액터들은 무시됩니다.
- 스냅샷에 포함된 액터가 응답하지 않고 조회 도중 정지하면, 우리는 그것이 정지했다는 사실을 조회 메시지의 송신자에게 보고합니다.

타임아웃 처리의 경우, 다음 두 경우 중 하나에서 조회를 완료된 것으로 간주합니다. 스냅샷에 포함된 모든 액터가 응답했거나 정지되었음을 확인한 경우, 또는 미리 정의된 마감 시한(deadline)에 도달한 경우입니다.

#### 메시지 프로토콜

이 프로토콜은 조회 도중 디바이스의 네 가지 상태를 정의합니다.

**Scala:**

```scala
final case class RequestAllTemperatures(requestId: Long, groupId: String, replyTo: ActorRef[RespondAllTemperatures])
    extends DeviceGroupQuery.Command
    with DeviceGroup.Command
    with DeviceManager.Command

final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

sealed trait TemperatureReading
final case class Temperature(value: Double) extends TemperatureReading
case object TemperatureNotAvailable extends TemperatureReading
case object DeviceNotAvailable extends TemperatureReading
case object DeviceTimedOut extends TemperatureReading
```

#### 아키텍처 근거 (Architecture Rationale)

조회 로직을 그룹 액터에 내장하는 대신, 이 설계는 **단일 조회(single query)를 나타내며 그룹 액터를 대신하여 조회를 완료하는 액터**를 별도로 생성합니다. 이 접근 방식은 그룹 디바이스 액터를 단순하게 유지하고, 조회 기능을 독립적으로 테스트할 수 있게 해 줍니다.

#### DeviceGroupQuery 액터 구현

##### 조회 액터 정의

조회 액터에는 다음이 필요합니다. 조회할 활성 디바이스 액터들의 스냅샷과 ID들, 조회를 시작한 요청의 ID(응답에 포함할 수 있도록), 조회를 보낸 액터의 참조(응답을 이 액터에게 직접 보냄), 그리고 조회가 응답을 얼마나 기다려야 하는지를 나타내는 마감 시한(deadline)입니다.

##### 타임아웃 스케줄링

이 구현은 마감 시한 관리를 강제하기 위해 `Behaviors.withTimers`와 `startSingleTimer`를 사용합니다.

**Scala:**

```scala
object DeviceGroupQuery {

  def apply(
      deviceIdToActor: Map[String, ActorRef[Device.Command]],
      requestId: Long,
      requester: ActorRef[DeviceManager.RespondAllTemperatures],
      timeout: FiniteDuration): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
      }
    }
  }

  trait Command

  private case object CollectionTimeout extends Command

  final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command

  private final case class DeviceTerminated(deviceId: String) extends Command
}

class DeviceGroupQuery(
    deviceIdToActor: Map[String, ActorRef[Device.Command]],
    requestId: Long,
    requester: ActorRef[DeviceManager.RespondAllTemperatures],
    timeout: FiniteDuration,
    context: ActorContext[DeviceGroupQuery.Command],
    timers: TimerScheduler[DeviceGroupQuery.Command])
    extends AbstractBehavior[DeviceGroupQuery.Command](context) {

  import DeviceGroupQuery._
  import DeviceManager.DeviceNotAvailable
  import DeviceManager.DeviceTimedOut
  import DeviceManager.RespondAllTemperatures
  import DeviceManager.Temperature
  import DeviceManager.TemperatureNotAvailable
  import DeviceManager.TemperatureReading

  timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

  private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)

  private var repliesSoFar = Map.empty[String, TemperatureReading]
  private var stillWaiting = deviceIdToActor.keySet

  deviceIdToActor.foreach {
    case (deviceId, device) =>
      context.watchWith(device, DeviceTerminated(deviceId))
      device ! Device.ReadTemperature(0, respondTemperatureAdapter)
  }

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case WrappedRespondTemperature(response) => onRespondTemperature(response)
      case DeviceTerminated(deviceId)          => onDeviceTerminated(deviceId)
      case CollectionTimeout                   => onCollectionTimout()
    }

  private def onRespondTemperature(response: Device.RespondTemperature): Behavior[Command] = {
    val reading = response.value match {
      case Some(value) => Temperature(value)
      case None        => TemperatureNotAvailable
    }

    val deviceId = response.deviceId
    repliesSoFar += (deviceId -> reading)
    stillWaiting -= deviceId

    respondWhenAllCollected()
  }

  private def onDeviceTerminated(deviceId: String): Behavior[Command] = {
    if (stillWaiting(deviceId)) {
      repliesSoFar += (deviceId -> DeviceNotAvailable)
      stillWaiting -= deviceId
    }
    respondWhenAllCollected()
  }

  private def onCollectionTimout(): Behavior[Command] = {
    repliesSoFar ++= stillWaiting.map(deviceId => deviceId -> DeviceTimedOut)
    stillWaiting = Set.empty
    respondWhenAllCollected()
  }

  private def respondWhenAllCollected(): Behavior[Command] = {
    if (stillWaiting.isEmpty) {
      requester ! RespondAllTemperatures(requestId, repliesSoFar)
      Behaviors.stopped
    } else {
      this
    }
  }
}
```

#### 디바이스 액터 갱신

Device 액터의 `RespondTemperature`는 디바이스 ID를 포함해야 합니다.

**Scala:**

```scala
final case class RespondTemperature(requestId: Long, deviceId: String, value: Option[Double])
```

핸들러는 다음과 같습니다.

```scala
case ReadTemperature(id, replyTo) =>
  replyTo ! RespondTemperature(id, deviceId, lastTemperatureReading)
  this
```

#### 쿼리를 DeviceGroup에 통합하기

그룹 액터는 필요에 따라 조회 액터를 생성합니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{
    DeviceRegistered,
    ReplyDeviceList,
    RequestAllTemperatures,
    RequestDeviceList,
    RequestTrackDevice
  }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      // ... other cases omitted

      case RequestAllTemperatures(requestId, gId, replyTo) =>
        if (gId == groupId) {
          context.spawnAnonymous(
            DeviceGroupQuery(deviceIdToActor, requestId = requestId, requester = replyTo, 3.seconds))
          this
        } else
          Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

#### 테스트

##### 테스트 1: 정상 경로 - 온도 값

**Scala:**

```scala
"return temperature value for working devices" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
}
```

##### 테스트 2: 온도를 사용할 수 없는 경우

**Scala:**

```scala
"return TemperatureNotAvailable for devices with no readings" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", None))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> TemperatureNotAvailable, "device2" -> Temperature(2.0))))
}
```

##### 테스트 3: 디바이스가 응답하기 전에 정지하는 경우

**Scala:**

```scala
"return DeviceNotAvailable if device stops before answering" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(2.0)))

  device2.stop()

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(2.0), "device2" -> DeviceNotAvailable)))
}
```

##### 테스트 4: 디바이스가 응답한 후에 정지하는 경우

**Scala:**

```scala
"return temperature reading even if device stops after answering" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  device2.stop()

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
}
```

##### 테스트 5: 타임아웃 처리

**Scala:**

```scala
"return DeviceTimedOut if device does not answer in time" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 200.millis))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))

  // no reply from device2

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> DeviceTimedOut)))
}
```

##### 테스트 6: DeviceGroup과의 통합

**Scala:**

```scala
"be able to collect temperatures from all active devices" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  val deviceActor1 = registeredProbe.receiveMessage().device

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  val deviceActor2 = registeredProbe.receiveMessage().device

  groupActor ! RequestTrackDevice("group", "device3", registeredProbe.ref)
  registeredProbe.receiveMessage()

  // Check that the device actors are working
  val recordProbe = createTestProbe[TemperatureRecorded]()
  deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 0))
  deviceActor2 ! RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 1))
  // No temperature for device3

  val allTempProbe = createTestProbe[RespondAllTemperatures]()
  groupActor ! RequestAllTemperatures(requestId = 0, groupId = "group", allTempProbe.ref)
  allTempProbe.expectMessage(
    RespondAllTemperatures(
      requestId = 0,
      temperatures =
        Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0), "device3" -> TemperatureNotAvailable)))
}
```

#### 요약

스캐터-개더 패턴(scatter-gather pattern) 구현은 몇 가지 핵심 액터 개념을 보여줍니다. 감시(watching)를 통한 동적 액터 생명주기 관리, 타이머(timer)를 이용한 타임아웃 조정, 별도의 액터 인스턴스를 통한 여러 동시 조회 처리, 그리고 어댑터(adapter)를 사용한 서로 다른 메시지 프로토콜 간의 변환입니다. 이 패턴은 디바이스가 나타났다 사라질 수 있고 서로 다른 속도로 응답하는 분산 시스템을 조회하는 과제를 성공적으로 해결합니다.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Part 1: Actor Architecture](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_1.html)
- [Part 2: Creating the First Actor](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_2.html)
- [Part 3: Working with Device Actors](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_3.html)
- [Part 4: Working with Device Groups](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_4.html)
- [Part 5: Querying Device Groups](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_5.html)
