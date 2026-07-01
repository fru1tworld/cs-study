# Akka 시작하기와 일반 개념

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

## 목차

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

## 1. Akka 라이브러리 소개

Akka는 "프로세서 코어와 네트워크에 걸쳐 동작하는 확장 가능하고(scalable) 복원력 있는(resilient) 시스템을 설계하기 위한 라이브러리 집합(a set of libraries)"입니다. Akka는 개발자가 저수준의 신뢰성 관련 코드를 직접 구현하는 대신, 비즈니스 목표(business objectives)에 집중할 수 있게 해 줍니다.

### 현대 분산 시스템이 직면하는 핵심 과제

현대 분산 시스템(distributed system)은 본질적인 장애 요소들을 가지고 있습니다.

- 구성 요소(component)가 별도의 통지 없이 실패(failure)할 수 있습니다.
- 메시지(message)가 전송 도중에 손실(loss)될 수 있습니다.
- 네트워크 지연(network delay)이 예측 불가능하게 발생할 수 있습니다.

이러한 문제들은 관리되는 데이터 센터(managed data center) 환경에서도 지속적으로 발생하며, 가상화된(virtualized) 환경에서는 더욱 빈번하게 나타납니다.

### 핵심 기능(Core Capabilities)

Akka는 세 가지 근본적인 기능을 제공합니다.

1. **동시성 동작(Concurrent behavior)**: 원자적 연산(atomics)이나 락(lock) 같은 저수준 구성 요소 없이 동시성을 구현할 수 있으며, 메모리 가시성(memory visibility) 문제를 신경 쓰지 않아도 됩니다.
2. **투명한 원격 통신(Transparent remote communication)**: 네트워킹의 복잡성을 추상화(abstract)하여 원격 통신을 투명하게 처리합니다.
3. **클러스터링되고 탄력적인 아키텍처(Clustered, elastic architecture)**: 동적 확장(dynamic scaling)과 고가용성(high availability)을 지원합니다.

### 액터 모델 기반(Actor Model Foundation)

액터 모델(actor model)은 Akka의 개념적 기반(conceptual foundation)으로서, 올바른 동시성(concurrent) 및 분산(distributed) 시스템을 구축하기 위한 추상화(abstraction)를 제공합니다. 이 통합된 프로그래밍 모델(unified programming model)은 모든 Akka 라이브러리에 걸쳐 일관된 이해를 보장하며, 긴밀하게 통합(tight integration)되어 있습니다.

### 전략적 위치(Strategic Positioning)

Akka 라이브러리는 두 가지 상위 수준 제품을 지원합니다.

- **Akka SDK**: 클러스터링을 포함한 빠른 개발(rapid development)을 제공합니다.
- **Akka Automated Operations**: 탄력성(elasticity)과 다중 리전(multi-region) 가용성을 관리해 주는 관리형 인프라(managed infrastructure)입니다.

### 시작 경로(Getting Started Path)

처음 시작하는 사용자는 Hello World 예제부터 시작한 다음, 시스템의 동기(systems motivation), 액터 모델 원리(actor model principles), 라이브러리 개요(library overviews), 실습 튜토리얼(practical tutorials)을 다루는 시작하기(Getting Started) 가이드로 진행하는 것이 좋습니다.

---

## 2. 현대 시스템이 새로운 프로그래밍 모델을 필요로 하는 이유

액터 모델은 수십 년 전 Carl Hewitt가 제안했으며, 전통적인 객체 지향 프로그래밍(object-oriented programming)이 완전히 해결하지 못하는 분산 시스템의 과제들을 다룹니다. 이제 현대 하드웨어와 인프라의 역량이 Hewitt의 비전을 뒷받침할 수 있게 되면서, 액터 모델은 요구가 까다로운 애플리케이션의 프로덕션(production) 환경에서 검증된 "매우 효과적인 해결책(a highly effective solution)"이 되었습니다.

현대 아키텍처와 전통적인 프로그래밍 모델 사이에는 세 가지 주요한 불일치(mismatch)가 존재합니다.

### 2.1 캡슐화의 도전(The Challenge of Encapsulation)

**핵심 문제**: 객체 지향의 캡슐화(encapsulation)는 단일 스레드 접근(single-threaded access)을 전제로 합니다. 다중 스레드 실행(multi-threaded execution)은 이 보호 장치를 무력화합니다.

**락(lock)의 문제점**:

- 락은 "동시성을 심각하게 제한(seriously limit concurrency)"하며, 현대 CPU에서 비용이 큽니다.
- 스레드를 블로킹(blocking)하면 다른 의미 있는 작업을 수행하지 못하게 됩니다.
- 락은 교착 상태(deadlock)의 위험을 초래합니다.
- 여러 머신에 걸친 분산 락(distributed lock)은 비효율적이며 확장성(scalability)을 제한합니다.

**핵심 통찰**: "객체는 단일 스레드 접근 상황에서만 캡슐화를 보장할 수 있으며, 다중 스레드 실행은 거의 항상 내부 상태의 손상(corrupted internal state)으로 이어진다."

### 2.2 공유 메모리의 환상(The Illusion of Shared Memory)

**현실**: 현대 CPU는 메모리에 직접 기록하지 않습니다. 각 코어에 로컬한 캐시 라인(cache line)에 기록합니다. 데이터는 코어 간에 명시적으로 전달(shipped)되어야 합니다.

**도전 과제**: 모든 변수를 `volatile`로 표시하는 것은 지나치게 비용이 큽니다. 왜냐하면 "코어 간에 캐시 라인을 전달하는 것은 매우 비용이 큰 연산(a very costly operation)"이기 때문입니다.

**해결 개념**: 공유 변수(shared variable)를 통해 메시지 전달(message-passing)을 숨기기보다는, 상태(state)를 각 동시성 엔티티(concurrent entity)에 로컬하게 유지하고 데이터를 메시지를 통해 명시적으로 전파(propagate)해야 합니다.

### 2.3 호출 스택의 환상(The Illusion of a Call Stack)

**문제**: 호출 스택(call stack)은 스레드를 가로질러 확장되지 않으므로, 비동기 작업 위임(asynchronous task delegation)이 문제가 됩니다.

**예외 처리의 붕괴(Exception Handling Breakdown)**: 작업자 스레드(worker thread)가 실패하면, 예외(exception)는 원래 호출자(caller)에게 전파되지 않습니다. "호출자 스레드는 어떤 식으로든 통지를 받아야 하지만, 예외로 풀어낼(unwind) 호출 스택이 존재하지 않는다."

**치명적 시나리오(Catastrophic Scenario)**: 작업자 스레드가 충돌(crash)하면, 작업 상태(task state)가 완전히 손실됩니다. "네트워킹이 전혀 관여하지 않은 로컬 통신임에도 불구하고 우리는 메시지를 잃어버렸다."

### 동시성 시스템에 대한 함의(Implications for Concurrent Systems)

효과적인 동시성 시스템은 다음을 요구합니다.

- 호출 스택을 넘어서는 명시적 오류 신호(error signaling) 메커니즘
- 분산 시스템과 유사한 타임아웃(timeout) 처리
- 메시지/응답이 손실되거나 지연될 수 있다는 인식
- 원칙에 입각한 재시작(principled restart) 메커니즘을 통한 서비스 장애 복구(fault recovery)

---

## 3. 액터 모델이 현대 분산 시스템의 요구를 충족하는 방법

액터 모델은 현대 분산 시스템에 대한 기존 프로그래밍 관행의 단점을 해결합니다. 기존 지식을 버리는 것이 아니라, 통신 기반 시스템(communication-based system)의 사고 모델(mental model)에 더 잘 부합하는 원칙적인 해결책을 제공합니다.

### 액터의 주요 이점(Key Benefits of Actors)

액터 모델은 개발자가 다음을 할 수 있게 합니다.

- 락(lock)에 의존하지 않고 캡슐화(encapsulation)를 유지합니다.
- 신호(signal)에 반응하고, 상태를 변경하며, 서로 통신하는 협력적 엔티티(cooperative entities)를 사용하여 애플리케이션을 구축합니다.
- 실제 세계의 관점을 더 잘 반영하는 실행 모델(execution model)로 작업합니다.

### 메시지 전달은 락킹과 블로킹을 제거한다(Message Passing Eliminates Locking and Blocking)

**핵심 원칙**: 액터는 메서드 호출(method call)이 아니라 메시지(message)를 통해 통신합니다. 액터가 메시지를 보낼 때, 실행 제어(execution control)를 넘기지 않으며 블로킹 없이 계속 작업할 수 있습니다.

메서드 반환 시 실행 스레드를 해제하는 객체와 달리, 액터는 메시지를 순차적으로(sequentially) 처리하고 각 메시지를 처리한 후 제어를 반환합니다. 이 방식은 동일한 시간 내에 더 높은 처리량(throughput)을 가능하게 합니다.

**중요한 구분**: 메시지에는 반환 값(return value)이 없습니다. 작업을 위임할 때, 수신 액터(receiving actor)는 값을 직접 반환하는 대신 결국 별도의 응답 메시지(reply message)로 응답합니다.

### 순차 처리를 통한 캡슐화(Encapsulation Through Sequential Processing)

액터는 메시지를 한 번에 하나씩 처리함으로써 캡슐화를 보존합니다. 개별 액터는 메시지를 순차적으로 처리하지만, 여러 액터는 동시에(concurrently) 동작하므로 시스템이 가용한 하드웨어를 충분히 활용할 수 있습니다.

각 액터는 동시에 최대 하나의 메시지만 처리하기 때문에, 내부 불변식(internal invariants)이 자동으로 보호되며 동기화 메커니즘(synchronization mechanism)이 필요하지 않습니다.

### 액터 처리 워크플로(Actor Processing Workflow)

액터가 메시지를 수신할 때의 흐름은 다음과 같습니다.

1. 메시지가 큐(queue)에 들어갑니다.
2. 스케줄되지 않은(unscheduled) 상태라면, 액터가 준비됨(ready) 상태로 표시됩니다.
3. 스케줄러(scheduler)가 실행을 시작합니다.
4. 액터가 큐의 맨 앞 메시지를 가져옵니다.
5. 액터가 상태를 변경하고 메시지를 보냅니다.
6. 액터가 스케줄되지 않은(unscheduled) 상태가 됩니다.

### 액터 구성 요소(Actor Components)

액터는 다음 요소들을 가집니다.

- **메일박스(Mailbox)**: 들어오는 메시지를 담는 큐(queue)
- **행동(Behavior)**: 상태 정의 및 메시지 응답 로직
- **메시지(Messages)**: 메서드 호출에 비견되는 데이터 신호
- **실행 환경(Execution environment)**: 액터의 호출(invocation)을 관리하는 스레드 풀(thread pool) 메커니즘
- **주소(Address)**: 식별 메커니즘

### 문제 해결(Problem Resolution)

이 모델은 핵심 문제들을 효과적으로 해결합니다.

- 실행 분리(execution decoupling)를 통한 캡슐화 보존
- 순차적 메시지 처리를 통한 락(lock) 요구 제거
- 제한된 스레드 위에서 수백만 개의 액터를 효율적으로 실행
- 로컬 상태(local state)와 네트워크 호환 전파(network-compatible propagation) 패턴

### 오류 처리 전략(Error Handling Strategies)

오류에는 두 가지 범주가 존재합니다.

**작업 실패(Task Failures)**: 위임된 작업(예: 유효성 검증 오류)이 실패하더라도, 서비스는 계속 기능합니다. 액터는 오류 메시지(error message)로 응답하며, 오류는 평범한 도메인 메시지(ordinary domain message)가 됩니다.

**서비스 장애(Service Faults)**: 내부 장애(internal failure)는 슈퍼비전(supervision) 메커니즘을 발동시킵니다. Akka는 계층적 액터 구성(hierarchical actor organization)을 강제하며, 부모 액터(parent actor)가 정의된 전략(strategy)을 통해 자식(children)을 관리합니다. 부모는 자식 액터를 재시작(restart)하거나 정지(stop)할 수 있으며, 실패에 대해 통지를 받습니다.

**슈퍼비전의 이점(Supervision Advantage)**: 재시작은 외부에서 보이지 않으므로, 협력자(collaborator)들은 복구가 진행되는 동안에도 계속 메시지를 보낼 수 있습니다.

---

## 4. Akka 라이브러리와 모듈 개요

Akka는 액터 모델을 사용하여 분산 시스템을 구축하기 위한 프레임워크입니다. 모든 핵심 기능(core functionality)은 오픈 소스(open source)로 유지되며, Lightbend는 교육(training)과 엔터프라이즈 기능을 포함한 상용 지원(commercial support)을 제공합니다.

### 액터 라이브러리(Actor Library)

기반 모듈인 `akka-actor-typed`는 상태(state)와 실행(execution)을 모두 캡슐화하는 것을 강조하는 프로그래밍 패러다임을 구현합니다. 메서드 호출 대신 메시지 전달(message passing)을 통해 통신이 이루어집니다. 이 접근 방식은 세 가지 주요 과제를 다룹니다.

- 고성능 동시성 애플리케이션(high-performance concurrent application) 구축
- 다중 스레드 환경(multi-threaded environment)에서의 오류 관리
- 동시성의 함정(concurrency pitfalls)으로부터 프로젝트 보호

### 리모팅(Remoting)

이 모듈은 서로 다른 컴퓨터에 있는 액터들이 메시지를 투명하게(transparently) 교환할 수 있게 합니다. 리모팅은 "라이브러리라기보다는 모듈에 가까운 것"으로 묘사되며, 직접적인 API보다는 주로 설정(configuration)에 의존합니다. 리모팅은 다음과 같은 인프라 수준의 과제를 해결합니다.

- 원격 액터 주소 지정(remote actor addressing)
- 메시지 직렬화(message serialization)
- 네트워크 연결 관리(network connection management)
- 투명한 호스트 장애 감지(transparent host failure detection)

### 클러스터(Cluster)

리모팅 위에 구축된 클러스터링(clustering)은 멤버십 프로토콜(membership protocol)을 사용하여 협력하는 액터 시스템들을 통합된 "메타 시스템(meta-system)"으로 조직합니다. 문서는 다음과 같이 강조합니다: "대부분의 경우, 리모팅을 직접 사용하기보다는 클러스터 모듈을 사용하는 것이 좋다." 클러스터링은 다음을 다룹니다.

- 분산 시스템 관리
- 멤버(member)의 안전한 도입
- 장애 감지(failure detection)
- 멤버 제거(member removal)
- 계산 분산(computation distribution)
- 역할 지정(role designation)

### 클러스터 샤딩(Cluster Sharding)

이 모듈은 대규모 액터 집합을 클러스터 멤버들에 걸쳐 분산시키며, 일반적으로 영속성(persistence)과 함께 사용됩니다. 다음을 관리합니다.

- 상태를 가진 엔티티(stateful entity)의 분산
- 부하 분산(load balancing)
- 충돌(crash) 시 상태 보존
- 중복 엔티티 인스턴스를 방지하는 일관성 보장(consistency guarantees)

### 클러스터 싱글톤(Cluster Singleton)

클러스터 전체에서 단일 서비스 인스턴스(single cluster-wide service instance)가 필요한 시나리오를 위해, 이 모듈은 유일성(uniqueness)을 보장하면서 호스트 장애 시 마이그레이션(migration)을 처리합니다. 다음을 다룹니다.

- 서비스 유일성(service uniqueness)
- 시스템 충돌 중 가용성(availability)
- 마이그레이션에도 불구하고 접근 가능한 인스턴스 위치 파악

### 영속성(Persistence)

액터는 전통적으로 상태를 휘발성 메모리(volatile memory)에 저장합니다. 영속성(Persistence)은 이벤트를 저장하여 시스템 재시작 시 상태를 재구성할 수 있게 합니다. 이 모듈은 다음을 지원합니다.

- 이벤트 재생(event replay)
- CQRS(Command Query Responsibility Segregation) 구현
- 장애에도 불구하고 신뢰성 있는 메시지 전달
- 도메인 이벤트 인트로스펙션(domain event introspection)
- 이벤트 소싱(event sourcing) 패턴

### 프로젝션(Projections)

이 모듈은 간단한 API를 사용하여 다운스트림 프로젝션(downstream projection)을 위해 이벤트 스트림(event stream)을 소비합니다. 다음을 가능하게 합니다.

- 대체 뷰(alternate view) 구성
- Kafka 같은 시스템으로의 이벤트 전파
- 이벤트 소싱 및 CQRS 컨텍스트 내에서 읽기 측 프로젝션(read-side projection) 구축

### 분산 데이터(Distributed Data)

최종 일관성(eventual consistency)을 수용하는 시스템을 위해, 이 모듈은 충돌 없는 복제 데이터 타입(Conflict-Free Replicated Data Types, CRDT)을 사용하여 클러스터 노드 전반에 걸쳐 데이터를 공유합니다. 여러 노드에서의 동시 쓰기(concurrent write)와 예측 가능한 병합(predictable merging)을 허용하여, 분할 내성(partition tolerance)과 저지연 로컬 접근(low-latency local access) 요구를 충족합니다.

### 스트림(Streams)

액터 위에 구축된 이 상위 수준 추상화는, 잠재적으로 무한한 이벤트 시퀀스(infinite event sequence)를 처리하는 처리 네트워크(processing network)를 단순화하면서 리소스 조정(resource coordination)을 관리합니다. Reactive Streams 표준의 구현은 서드파티(third-party) 통합을 가능하게 합니다. 스트림은 다음을 다룹니다.

- 고성능 스트림 처리(high-performance stream handling)
- 파이프라인 합성(pipeline composition)
- 비동기 서비스 연결(asynchronous service connection)
- 리액티브 인터페이스(reactive interface) 제공

### Alpakka

Streams API 위에 구축된 별도의 모듈 모음인 Alpakka는, 클라우드 및 인프라 기술을 위한 리액티브 스트림 커넥터(reactive stream connector)를 제공합니다. 리액티브 API를 통해 인프라 구성 요소 통합 및 레거시 시스템(legacy system) 연결을 가능하게 합니다.

### HTTP

HTTP 서비스를 구축하고 소비하기 위한 도구를 제공하는 별도의 모듈로서, 대용량 데이터셋과 실시간 이벤트의 스트리밍에 특히 최적화되어 있습니다. 다음을 다룹니다.

- API 노출(API exposure)
- 대용량 데이터셋 스트리밍
- 실시간 이벤트 스트리밍

### gRPC

이 별도의 모듈은 gRPC를 HTTP 및 Streams 모듈과 통합하며, protobuf 정의로부터 클라이언트 및 서버 아티팩트(artifact)를 생성합니다. gRPC의 이점을 활용합니다.

- 스키마 우선 계약(schema-first contracts)
- 스키마 진화(schema evolution)
- 효율적인 바이너리 프로토콜(efficient binary protocol)
- 스트리밍 지원
- 상호 운용성(interoperability)
- HTTP/2 멀티플렉싱(multiplexing)

### 통합 예시(Integration Example)

문서는 모듈들이 어떻게 통합되는지 설명합니다: 상태를 가진 비즈니스 객체는 샤딩된(sharded), 영속적인(persistent) 엔티티로 모델링되어 확장 가능한 클러스터 전반에 부하 분산됩니다. 실시간 도메인 이벤트 스트림은 빠른 데이터 엔진(fast data engine)을 통해 파이프되며, 처리된 출력은 분석 대시보드(analytics dashboard)를 위한 WebSocket 연결을 갖춘 부하 분산 HTTP 서버를 통해 노출됩니다.

### 기술적 구현(Technical Implementation)

모든 모듈은 보안 토큰화된 저장소 접근(secure, tokenized repository access)을 요구합니다. 의존성 관리(dependency management)는 다양한 빌드 시스템(sbt, Maven, Gradle)에 걸쳐 BOM(Bill of Materials) 패턴을 사용합니다. 문서 예제 전반에서 버전 2.10.19가 참조됩니다.

---

## 5. 용어와 개념(Terminology and Concepts)

이 장에서는 동시성·분산 컴퓨팅에서 자주 쓰이지만 의미가 혼동되는 용어들을 정리합니다.

### 5.1 동시성 vs. 병렬성(Concurrency vs. Parallelism)

**동시성**(Concurrency)은 "두 개 이상의 작업이 동시에 실행되지는 않더라도 진행을 이루어 가는 것(making progress)"을 의미합니다. 이는 시간 분할(time slicing)을 통해 달성될 수 있습니다.

**병렬성**(Parallelism)은 "실행이 진정으로 동시에(truly simultaneous) 이루어질 때" 발생합니다.

### 5.2 비동기 vs. 동기(Asynchronous vs. Synchronous)

**동기(synchronous)** 메서드 호출은 메서드가 반환하거나 예외를 던질 때까지 호출자가 진행하지 못하게 막습니다.

**비동기(asynchronous)** 호출은 "호출자가 유한한 단계(finite number of steps) 이후에 진행할 수 있게 하며, 메서드의 완료는 어떤 추가적인 메커니즘을 통해 신호(signal)될 수 있습니다."

액터는 본질적으로 비동기(inherently asynchronous)입니다.

### 5.3 논블로킹 vs. 블로킹(Non-blocking vs. Blocking)

**블로킹**(Blocking)은 "한 스레드의 지연이 다른 스레드들 중 일부를 무한정 지연시킬 수 있을 때" 발생합니다.

**논블로킹**(Non-blocking)은 "어떤 스레드도 다른 스레드들을 무한정 지연시킬 수 없음"을 의미합니다.

문서는 블로킹 API를 항상 피할 수는 없지만, 신중하게 관리(careful management)되어야 한다고 언급합니다.

### 5.4 교착 상태 vs. 기아 상태 vs. 라이브락(Deadlock vs. Starvation vs. Livelock)

**교착 상태**(Deadlock)는 참여자들이 서로가 진행하기를 기다리면서 "Catch-22" 상황을 만들어내고, 그 결과 "영향을 받은 모든 하위 시스템이 멈추는(stall)" 경우에 발생합니다.

**기아 상태**(Starvation)는 일부 참여자는 진행할 수 있지만 다른 참여자들은 진행할 수 없을 때(흔히 스케줄링 편향(scheduling bias) 때문에) 발생합니다.

**라이브락**(Livelock)은 교착 상태와 유사하지만, "참여자들이 상태를 계속해서 변경(continuously change their state)"한다는 점에서 다릅니다. 즉, 멈춰 있는 것이 아니라 계속 움직이지만 진전을 이루지는 못합니다.

### 5.5 경쟁 조건(Race Condition)

**경쟁 조건**(race condition)은 "어떤 이벤트 집합의 순서(ordering)에 대한 가정이 외부의 비결정적 효과(external non-deterministic effects)에 의해 위반될 수 있을 때" 발생합니다. Akka는 특정 액터 쌍(specific actor pairs) 사이의 메시지 순서(message ordering)를 보장합니다.

### 5.6 논블로킹 보장(Non-blocking Guarantees)

논블로킹 알고리즘은 보장의 강도에 따라 다음과 같이 구분됩니다.

- **대기 없음(Wait-freedom)**: "모든 호출이 유한한 단계 내에 완료되는 것이 보장됩니다." 가장 강력한 보장입니다.
- **락 없음(Lock-freedom)**: "무한히 자주(infinitely often) 어떤 메서드가 유한한 단계 내에 완료됩니다." 그러나 모든 호출이 완료되는 것을 보장하지는 않습니다.
- **방해 없음(Obstruction-freedom)**: 가장 약한 보장으로, 메서드가 격리되어(in isolation) 실행될 때 완료됩니다.

---

## 6. 액터 시스템(Actor Systems)

Akka의 액터 시스템(actor system)은 "상태와 행동(state and behavior)을 캡슐화하고, 오직 메시지를 교환함으로써만 통신하며, 그 메시지는 수신자의 메일박스(recipient's mailbox)에 놓이는" 객체들로 구성됩니다. 이 프레임워크는 액터를 작업을 계층적으로 위임하는 조직 구성원(organizational member)처럼 다룹니다.

### 핵심 구조 원칙(Key Structural Principles)

**계층적 조직(Hierarchical Organization)**: 액터는 자연스럽게 계층(hierarchy)을 형성하며, 부모 액터(parent actor)가 자식(children)에게 작업을 위임합니다. 이 "오류 커널 패턴(Error Kernel Pattern)"은 위험한 작업을 자식 액터에 격리시킴으로써 중요한 상태(critical state)를 보호합니다.

**설정 컨테이너(Configuration Container)**: 액터 시스템은 스케줄링(scheduling)이나 로깅(logging) 같은 공유 시설(shared facilities)을 관리합니다. 하나의 JVM 안에 여러 시스템이 공존할 수 있으며, 이는 시스템 간 투명한 통신(transparent cross-system communication)을 갖춘 분산 애플리케이션을 가능하게 합니다.

### 모범 사례(Best Practices)

문서는 네 가지 핵심 사례를 제시합니다.

1. 외부 리소스를 기다리며 스레드를 블로킹하지 말고, 이벤트를 효율적으로 처리합니다.
2. 액터 간에 가변 객체(mutable object)를 절대 전달하지 말고, 불변 메시지(immutable message)를 사용합니다.
3. 우발적인 상태 공유(accidental state sharing)를 방지하기 위해, 메시지 안에 행동(behavior)을 담아 보내지 않습니다.
4. 최상위 액터(top-level actor)는 최소한으로 유지하고, 로직은 계층적 하위 시스템(hierarchical subsystems)에 위임합니다.

### 리소스 관리(Resource Management)

액터는 인스턴스당 약 300바이트(approximately 300 bytes per instance)로 매우 가벼우므로, 하나의 시스템 내에서 수백만 개의 액터를 둘 수 있습니다. 프레임워크가 순서(ordering)와 리소스 할당(resource allocation)을 자동으로 처리하므로, 개발자는 이러한 사항들을 일일이 관리할 필요에서 자유로워집니다.

### 시스템 종료(System Termination)

애플리케이션은 `ActorSystem`의 `terminate()` 메서드를 통해 종료되며, 이는 `CoordinatedShutdown`을 발동시켜 실행 중인 모든 액터를 우아하게(gracefully) 정지시킵니다.

---

## 7. 액터란 무엇인가(What is an Actor)

이전의 "액터 시스템(Actor Systems)" 섹션에서는 액터들이 어떻게 계층을 형성하며, 애플리케이션을 구축할 때 가장 작은 단위(smallest unit)가 되는지를 설명했습니다. 이 섹션에서는 그러한 액터 하나를 따로 떼어 살펴보면서, 액터를 구현할 때 마주치게 되는 개념들을 설명합니다. 더 심층적인 레퍼런스는 "액터 소개(Introduction to Actors)"를 참고하십시오.

Hewitt, Bishop, Steiger가 1973년에 정의한 액터 모델(Actor Model)은, 계산(computation)이 분산된다는 것이 정확히 무엇을 의미하는지를 표현하는 계산 모델(computational model)입니다. 처리 단위(processing unit)인 액터(Actor)는 오직 메시지를 교환함으로써만 통신할 수 있으며, 메시지를 수신하면 액터는 다음 세 가지 근본적인 행동(fundamental actions)을 할 수 있습니다.

1. 자신이 알고 있는 액터들에게 유한한 수의 메시지를 보낸다.
2. 유한한 수의 새로운 액터를 생성한다.
3. 다음 메시지에 적용할 행동(behavior)을 지정한다.

액터는 상태(State), 행동(Behavior), 메일박스(Mailbox), 자식 액터(Child Actors), 슈퍼바이저 전략(Supervisor Strategy)을 담는 컨테이너입니다. 이 모든 것은 액터 참조(Actor Reference) 뒤에 캡슐화됩니다. 주목할 점은, 액터가 명시적인 생명 주기(explicit lifecycle)를 가진다는 것입니다. 액터는 더 이상 참조되지 않더라도 자동으로 소멸(destroy)되지 않습니다. 액터를 생성한 후에는, 결국 종료(terminate)되도록 보장하는 것이 사용자의 책임입니다. 이를 통해 "액터가 종료될 때(When an Actor Terminates)" 리소스가 어떻게 해제되는지를 사용자가 직접 제어할 수 있습니다.

### 7.1 액터 참조(Actor Reference)

액터 모델의 이점을 누리려면 액터 객체가 외부로부터 차단(shielded)되어야 합니다. 따라서 액터는 외부에 액터 참조(actor reference)를 통해 표현되며, 이 참조는 제약 없이 자유롭게 전달할 수 있는 객체입니다. 이렇게 내부 객체(inner object)와 외부 객체(outer object)로 분리함으로써, 모든 연산에 대한 투명성(transparency)이 가능해집니다. 즉, 다른 참조를 갱신할 필요 없이 액터를 재시작하는 것, 실제 액터 객체를 원격 호스트(remote host)에 배치하는 것, 액터가 어디에서 실행되든 메시지를 보내는 것이 가능합니다. 가장 중요한 점은, 액터가 스스로 그 정보를 공개하지 않는 한 외부에서 액터 내부를 들여다보거나 그 상태(state)에 접근하는 것이 불가능하다는 것입니다.

액터 참조는 파라미터화(parameterized)되어 있으며, 지정된 타입(specified type)의 메시지만 보낼 수 있습니다.

### 7.2 상태(State)

액터 객체는 일반적으로 액터가 처할 수 있는 상태를 반영하는 여러 변수를 포함합니다. 이는 명시적인 상태 기계(state machine)일 수도 있고, 카운터(counter), 리스너 집합(set of listeners), 대기 중인 요청(pending requests) 등일 수도 있습니다. 이 데이터들은 액터를 가치 있게 만드는 것이며, 다른 액터에 의한 손상(corruption)으로부터 보호되어야 합니다. Akka 액터는 개념적으로 각자 자신만의 경량 스레드(light-weight thread)를 가지며, 이 스레드는 시스템의 나머지 부분으로부터 완전히 격리됩니다. 따라서 락(lock)으로 접근을 동기화할 필요 없이, 동시성을 의식하지 않고 액터 코드를 작성할 수 있습니다.

내부적으로 Akka는 액터 집합을 실제 스레드 집합 위에서 실행하며, 일반적으로 많은 액터가 하나의 스레드를 공유하고, 한 액터의 후속 호출(subsequent invocations)들이 서로 다른 스레드에서 처리될 수도 있습니다. Akka는 이러한 구현 세부 사항이 액터 상태 처리의 단일 스레드성(single-threadedness)에 영향을 미치지 않도록 보장합니다.

내부 상태(internal state)는 액터 동작의 핵심이므로, 일관성 없는 상태(inconsistent state)는 치명적입니다. 따라서 액터가 실패(fail)하여 슈퍼바이저(supervisor)에 의해 재시작(restart)될 때, 상태는 액터를 처음 생성할 때처럼 처음부터 새로(from scratch) 만들어집니다. 이를 통해 시스템의 자가 치유(self-healing) 능력이 실현됩니다.

선택적으로, 수신한 메시지를 영속화(persisting)하고 재시작 후 이를 재생(replaying)함으로써, 액터의 상태를 재시작 이전의 상태로 자동 복구할 수 있습니다(이벤트 소싱(Event Sourcing) 참고).

### 7.3 행동(Behavior)

메시지가 처리될 때마다, 해당 메시지는 액터의 현재 행동(current behavior)에 매칭됩니다. 행동(Behavior)이란, 그 시점에 메시지에 반응하여 취할 행동을 정의하는 함수(function)입니다. 예를 들어, 클라이언트가 인가(authorized)되었으면 요청을 전달하고 그렇지 않으면 거부하는 식입니다. 이 행동은 시간이 지나면서 변경될 수 있습니다. 서로 다른 클라이언트가 인가를 얻거나, 액터가 "서비스 불가(out-of-service)" 모드로 들어갔다가 나중에 복귀할 수 있기 때문입니다. 이러한 변경은 행동 로직에서 읽는 상태 변수(state variables)에 인코딩하거나, 다음 메시지에 사용할 다른 행동을 반환하여 런타임에 함수 자체를 교체하는 방식으로 달성됩니다. 다만, 액터 객체 생성(construction) 시점에 정의된 초기 행동(initial behavior)은 특별합니다. 액터가 재시작되면 행동이 이 초기 행동으로 재설정(reset)되기 때문입니다.

메시지는 액터 참조(Actor Reference)로 보낼 수 있으며, 이 외관(façade) 뒤에는 메시지를 수신하고 그에 따라 동작하는 행동이 존재합니다. 액터 참조와 행동 사이의 바인딩(binding)은 시간이 지나면서 변경될 수 있지만, 이는 외부에서 보이지 않습니다.

액터 참조는 파라미터화되어 있으며, 지정된 타입의 메시지만 보낼 수 있습니다. 액터 참조(및 그 액터)가 생성될 때, 액터 참조와 그 타입 파라미터(type parameter) 사이의 연관이 맺어져야 합니다. 이를 위해 각 행동(behavior) 또한 처리할 수 있는 메시지의 타입으로 파라미터화됩니다. 행동은 액터 참조 외관 뒤에서 변경될 수 있으므로, 다음 행동(next behavior)을 지정하는 것은 제약된 연산(constrained operation)입니다. 즉, 후임 행동(successor)은 그 선임(predecessor)과 동일한 타입의 메시지를 처리해야 합니다. 이는 이 액터를 가리키는 액터 참조들을 무효화하지 않기 위해 필요합니다.

이것이 가능하게 하는 것은, 메시지가 액터로 보내질 때마다 메시지의 타입이 그 액터가 처리한다고 선언한 타입 중 하나임을 정적으로(statically) 보장할 수 있다는 점입니다. 우리는 완전히 무의미한 메시지를 보내는 실수를 피할 수 있습니다. 그러나 정적으로 보장할 수 없는 것은, 우리의 메시지가 수신될 때 액터 참조 뒤의 행동이 특정 상태에 있을지 여부입니다. 그 근본적인 이유는, 액터 참조와 행동 사이의 연관이 동적인 런타임 속성(dynamic runtime property)이며, 컴파일러는 소스 코드를 번역하는 동안 그것을 알 수 없기 때문입니다.

이는 내부 변수를 가진 일반적인 Java 객체와 동일합니다. 프로그램을 컴파일할 때 우리는 그 변수들의 값이 무엇이 될지 알 수 없으며, 메서드 호출의 결과가 그 변수들에 의존한다면 그 결과는 어느 정도 불확실합니다. 우리는 다만 반환된 값이 주어진 타입(given type)임을 확신할 수 있을 뿐입니다.

액터 명령(command)의 응답 메시지 타입(reply message type)은, 그 메시지에 포함된 응답 대상(reply-to)에 대한 액터 참조의 타입으로 기술됩니다. 이는 대화(conversation)를 그 타입의 관점에서 기술할 수 있게 합니다. 응답은 타입 A이지만, 그것이 타입 B의 주소를 포함할 수도 있으며, 그러면 다른 액터가 이 새로운 액터 참조로 타입 B의 메시지를 보냄으로써 대화를 이어갈 수 있습니다. 우리는 액터의 "현재(current)" 상태를 정적으로 표현할 수는 없지만, 두 액터 사이의 프로토콜의 현재 상태(current state of a protocol)는 표현할 수 있습니다. 이는 단지 마지막으로 수신되거나 보내진 메시지의 타입에 의해 주어지기 때문입니다.

### 7.4 메일박스(Mailbox)

액터의 목적은 메시지를 처리하는 것이며, 이 메시지들은 다른 액터들(또는 액터 시스템 외부)로부터 그 액터에게 보내진 것입니다. 송신자(sender)와 수신자(receiver)를 연결하는 부분이 바로 액터의 메일박스(mailbox)입니다. 각 액터는 정확히 하나의 메일박스를 가지며, 모든 송신자가 이 메일박스에 자신의 메시지를 큐잉(enqueue)합니다. 큐잉은 송신 연산(send operations)의 시간 순서(time-order)대로 일어납니다. 이는 곧, 서로 다른 액터들로부터 보내진 메시지들은, 액터들이 스레드에 분산되는 겉보기 무작위성(apparent randomness) 때문에 런타임에 정의된 순서를 가지지 않을 수 있음을 의미합니다. 반면, 동일한 액터로부터 동일한 대상에게 여러 메시지를 보내면, 그것들은 같은 순서로 큐잉됩니다.

선택할 수 있는 다양한 메일박스 구현(mailbox implementation)이 존재하며, 기본값은 FIFO입니다. 즉, 액터가 처리하는 메시지의 순서는 그것들이 큐잉된 순서와 일치합니다. 이것은 보통 좋은 기본값이지만, 애플리케이션에 따라 일부 메시지를 다른 메시지보다 우선시(prioritize)해야 할 수도 있습니다. 이 경우, 우선순위 메일박스(priority mailbox)는 항상 끝이 아니라 메시지 우선순위(message priority)에 따른 위치에 큐잉하며, 그것은 심지어 맨 앞일 수도 있습니다. 이러한 큐를 사용하는 동안, 처리되는 메시지의 순서는 자연히 그 큐의 알고리즘에 의해 정의되며 일반적으로 FIFO가 아닙니다.

Akka가 일부 다른 액터 모델 구현과 다른 중요한 특징은, 현재 행동(current behavior)이 항상 다음으로 디큐(dequeue)된 메시지를 처리해야 한다는 점입니다. 즉, 다음에 매칭되는 메시지를 찾기 위해 메일박스를 훑어보는 일(scanning the mailbox)은 없습니다. 메시지 처리에 실패하는 것은, 이 행동이 재정의(override)되지 않는 한 일반적으로 실패(failure)로 취급됩니다.

### 7.5 자식 액터(Child Actors)

각 액터는 잠재적으로 부모(parent)가 됩니다. 하위 작업(sub-task)을 위임하기 위해 자식을 생성하면 그 자식들을 자동으로 슈퍼비전(supervise)하게 됩니다. 자식 목록(list of children)은 액터의 컨텍스트(context) 안에서 유지되며, 액터는 이 목록에 접근할 수 있습니다. 자식을 스폰(spawning)하거나 정지(stopping)하면 즉시 목록에 반영됩니다. 실제 생성 및 종료는 비동기적(asynchronous)으로 처리되므로 부모를 블로킹하지 않습니다.

### 7.6 슈퍼바이저 전략(Supervisor Strategy)

액터의 마지막 구성 요소는 예상치 못한 예외(unexpected exceptions), 즉 실패(failures)를 처리하기 위한 전략입니다. 장애 처리(fault handling)는 Akka에 의해 투명하게 수행되며, 각 실패에 대해 "장애 내성(Fault Tolerance)"에서 기술된 전략 중 하나가 적용됩니다.

### 7.7 액터가 종료될 때(When an Actor Terminates)

액터가 종료되면(재시작으로 처리되지 않는 방식으로 실패하거나, 스스로 정지하거나, 슈퍼바이저에 의해 정지되면), 해당 액터는 리소스를 해제하고 메일박스에 남아 있는 모든 메시지를 시스템의 "데드 레터 메일박스(dead letter mailbox)"로 보냅니다. 이 데드 레터 메일박스는 해당 메시지들을 `DeadLetters`로서 `EventStream`으로 전달합니다. 이후 메일박스는 액터 참조 내에서 시스템 메일박스(system mailbox)로 교체되어, 이후 수신되는 모든 메시지를 `DeadLetters`로서 `EventStream`으로 리디렉션합니다. 다만 이는 최선 노력(best effort) 기반으로 이루어지므로, "보장된 전달(guaranteed delivery)"을 위해 이에 의존해서는 안 됩니다.

---

## 8. 슈퍼비전과 모니터링(Supervision and Monitoring)

이 장에서는 슈퍼비전(supervision)의 개념, 제공되는 기본 요소(primitives)와 그 의미론(semantics)을 개괄합니다. 실제 코드로의 적용에 대한 세부 사항은 supervision을 참고하시기 바랍니다.

슈퍼비전은 클래식(classic) 이후로 변경되었습니다. 클래식 슈퍼비전에 대한 세부 사항은 "Classic Supervision"을 참고하십시오.

### 8.1 슈퍼비전이 의미하는 것(What Supervision Means)

액터에서 발생할 수 있는 예외에는 두 가지 범주가 있습니다.

1. 입력 유효성 검증 오류(Input validation errors). 일반적인 try-catch나 기타 언어 및 표준 라이브러리 도구로 처리할 수 있는 예상된 예외(expected exceptions)입니다.
2. 예상치 못한 실패(Unexpected failures). 예를 들어 네트워크 리소스를 사용할 수 없거나, 디스크 쓰기가 실패하거나, 애플리케이션 로직에 버그가 있는 경우입니다.

슈퍼비전은 실패(failures)를 다루며, 이는 비즈니스 로직(business logic)과 분리되어야 합니다. 반면 데이터의 유효성 검증과 예상된 예외의 처리는 비즈니스 로직의 필수적인 부분입니다. 따라서 슈퍼비전은 액터의 메시지 처리 로직과 뒤섞이는 무언가가 아니라, 액터에 데코레이션(decoration)으로서 추가됩니다.

슈퍼비전 대상 작업의 성격과 실패의 성격에 따라, 슈퍼비전은 다음 세 가지 전략(strategies)을 제공합니다.

1. 액터를 재개(Resume)하여, 누적된 내부 상태(accumulated internal state)를 유지합니다.
2. 액터를 재시작(Restart)하여, 누적된 내부 상태를 지우고, 잠재적으로 지연(delay)을 둔 후 다시 시작합니다.
3. 액터를 영구적으로 정지(Stop)합니다.

액터는 계층(hierarchy)의 일부이므로, 영구적인 실패를 위쪽으로 전파(propagate)하는 것이 종종 타당할 수 있습니다. 어떤 액터의 모든 자식이 예상치 못하게 정지되었다면, 그 액터 자신이 기능하는 상태로 되돌아가기 위해 재시작하거나 정지하는 것이 타당할 수 있습니다. 이는 슈퍼비전과, 자식이 종료될 때 통지를 받기 위해 자식을 감시(watching)하는 것을 조합하여 달성할 수 있습니다.

### 8.2 최상위 액터(The Top-Level Actors)

액터 시스템은 생성 도중에 최소 두 개의 액터를 시작합니다.

#### `/user`: 사용자 가디언 액터(user guardian actor)

이것은 사용자가 제공한 최상위 액터로서, 하위 시스템(subsystems)을 자식으로 스폰하여 애플리케이션을 부트스트랩(bootstrap)하기 위한 것입니다. 사용자 가디언이 정지하면 전체 액터 시스템이 종료됩니다.

#### `/system`: 시스템 가디언 액터(system guardian actor)

이 특별한 가디언은, 로깅 자체가 액터를 사용하여 구현되어 있음에도 불구하고 모든 일반 액터가 종료되는 동안 로깅이 활성 상태로 유지되는 질서 있는 종료 시퀀스(orderly shut-down sequence)를 달성하기 위해 도입되었습니다. 이는 시스템 가디언이 사용자 가디언을 감시하고, 사용자 가디언이 정지하는 것을 본 후에 자신의 종료를 시작하게 함으로써 실현됩니다.

### 8.3 재시작이 의미하는 것(What Restarting Means)

특정 메시지를 처리하다가 실패한 액터를 마주했을 때, 그 실패의 원인은 세 가지 범주로 나뉩니다.

- 수신한 특정 메시지에 대한 체계적(즉, 프로그래밍) 오류(Systematic/programming error)
- 메시지를 처리하는 동안 사용된 어떤 외부 리소스의 (일시적) 실패((Transient) failure of some external resource)
- 액터의 손상된 내부 상태(Corrupt internal state)

실패가 구체적으로 인식 가능한 것이 아니라면, 세 번째 원인(손상된 내부 상태)을 배제할 수 없으며, 이는 곧 내부 상태를 지워내야(cleared out) 한다는 결론으로 이어집니다. 만약 슈퍼바이저가 (예컨대 의식적인 오류 커널 패턴(error kernel pattern)의 적용 덕분에) 자신의 다른 자식들이나 자기 자신이 그 손상에 영향받지 않는다고 판단한다면, 따라서 그 액터를 재시작하는 것이 최선입니다. 이는 기반 `Behavior` 클래스의 새 인스턴스를 생성하고, 자식의 `ActorRef` 내부에서 실패한 인스턴스를 새 인스턴스로 교체함으로써 수행됩니다. 이렇게 할 수 있는 능력은 액터를 특별한 참조(special references) 안에 캡슐화하는 이유 중 하나입니다. 그러면 새 액터가 자신의 메일박스 처리를 재개하므로, 실패가 발생한 그 메시지가 다시 처리되지 않는다는 주목할 만한 예외를 제외하면, 재시작은 액터 자체의 외부에서 보이지 않습니다.

### 8.4 생명 주기 모니터링이 의미하는 것(What Lifecycle Monitoring Means)

> 참고: Akka에서 생명 주기 모니터링(Lifecycle Monitoring)은 보통 `DeathWatch`라고 불립니다.

위에서 설명한 부모-자식 사이의 특별한 관계와 달리, 각 액터는 다른 어떤 액터든 모니터링(monitor)할 수 있습니다. 액터는 생성과 동시에 완전히 살아있는(fully alive) 상태가 되고 재시작은 해당 슈퍼바이저 외부에서 보이지 않으므로, 모니터링에서 관찰할 수 있는 유일한 상태 변화는 살아있음(alive)에서 죽음(dead)으로의 전이뿐입니다. 따라서 모니터링은 한 액터를 다른 액터에 연결하여 종료(termination)에 반응하기 위해 사용됩니다. 이는 실패(failure)에 반응하는 슈퍼비전과는 대조적입니다.

생명 주기 모니터링은 모니터링 액터가 수신하는 `Terminated` 메시지를 통해 구현됩니다. 기본 동작은, 별도로 처리하지 않을 경우 특별한 `DeathPactException`을 던지는 것입니다. `Terminated` 메시지 수신을 시작하려면 `ActorContext.watch(targetActorRef)`를 호출하고, 수신을 중지하려면 `ActorContext.unwatch(targetActorRef)`를 호출합니다. 중요한 점은, 모니터링 등록과 대상 종료의 발생 순서에 관계없이 메시지가 전달된다는 것입니다. 즉, 등록 시점에 대상이 이미 종료된 상태더라도 `Terminated` 메시지를 받게 됩니다.

### 8.5 액터와 예외(Actors and Exceptions)

액터가 메시지를 처리하는 도중에 어떤 종류의 예외(예: 데이터베이스 예외)가 던져질 수 있습니다.

#### 메시지에는 무슨 일이 일어나는가(What happens to the Message)

메시지를 처리하는 도중(즉, 메일박스에서 꺼내져 현재 행동에 넘겨진 동안) 예외가 발생하면 해당 메시지는 손실(lost)됩니다. 메시지가 메일박스에 다시 들어가지 않는다는 점이 중요합니다. 따라서 메시지 처리를 재시도(retry)하려면, 예외를 직접 잡아(catching the exception) 흐름을 재시도하는 방식으로 처리해야 합니다. 재시도 횟수에는 반드시 상한(bound)을 두어야 합니다. 시스템이 라이브락(livelock), 즉 진전 없이 많은 CPU 사이클을 소비하는 상태에 빠지지 않게 하기 위해서입니다.

#### 메일박스에는 무슨 일이 일어나는가(What happens to the Mailbox)

메시지가 처리되는 동안 예외가 던져지더라도, 메일박스에는 아무 일도 일어나지 않습니다. 만약 액터가 재시작되면, 동일한 메일박스가 그대로 존재합니다. 따라서 그 메일박스에 있던 모든 메시지도 그대로 존재합니다.

#### 액터에는 무슨 일이 일어나는가(What happens to the Actor)

액터 내부의 코드가 예외를 던지면, 그 액터는 일시 중단(suspended)되고 슈퍼비전 프로세스가 시작됩니다. 슈퍼바이저의 결정에 따라 액터는 (아무 일도 없었던 것처럼) 재개(resumed)되거나, (내부 상태를 지우고 처음부터 시작하며) 재시작(restarted)되거나, 종료(terminated)됩니다.

---

## 9. 액터 참조, 경로, 주소(Actor References, Paths and Addresses)

이 장에서는 액터를 식별하고 위치를 파악하는 방법을 다룹니다.

### 9.1 액터 참조(Actor References)

**액터 참조**(Actor References)는 `ActorRef`의 하위 타입(subtypes)으로, 액터에게 메시지를 보낼 수 있게 합니다. 문서는 다음과 같이 언급합니다: "액터 참조는 단일 액터(single actor)를 가리키며, 그 참조의 생명 주기는 그 액터의 생명 주기와 일치한다."

시스템 설정에 따라 다양한 참조 타입이 존재합니다.

- 로컬 참조(Local references) — 네트워킹되지 않은 시스템용
- 리모팅(remoting)이 활성화된 로컬 참조
- 원격 액터 참조(Remote actor references)
- `PromiseActorRef`, `DeadLetterActorRef`, `EmptyLocalActorRef` 같은 특수 타입(special types)

### 9.2 액터 경로(Actor Paths)

**액터 경로**(Actor Paths)는 액터 시스템 내의 계층적 이름(hierarchical name)을 나타냅니다. 중요한 점은, "액터 경로는 액터가 거주할(inhabited) 수도 있고 그렇지 않을 수도 있는 이름을 나타내며, 경로 자체는 생명 주기를 가지지 않고 결코 무효화되지 않는다(never becomes invalid)"는 것입니다.

### 9.3 핵심적 구분(Critical Distinction)

근본적인 차이점: 동일한 경로(same path)로 액터를 다시 생성할 수 있지만, 그것은 새로운 화신(new incarnation)이 됩니다. "이전 액터 참조로 보내진 메시지들은, 비록 동일한 경로를 가지고 있더라도 새로운 화신에게 전달되지 않습니다."

### 9.4 경로 구조(Path Structure)

경로는 다음과 같은 형식을 따릅니다.

- 로컬: `"akka://my-sys/user/service-a/worker1"`
- 원격: `"akka://[email protected]:5678/user/service-b"`

### 9.5 최상위 스코프(Top-Level Scopes)

액터 시스템은 다음과 같은 특정 네임스페이스 분기(namespace branches)를 예약합니다.

- `/user` — 사용자가 생성한 액터(user-created actors)용
- `/system` — 시스템 액터(system actors)용
- `/deadLetters` — 전달 불가능한 메시지(undeliverable messages)용
- `/temp` — 수명이 짧은 시스템 액터(short-lived system actors)용
- `/remote` — 원격 슈퍼바이저(remote supervisors)를 가진 액터용

문서는 액터 참조를 얻는 것이, 액터를 생성하거나 리셉셔니스트(Receptionist) 같은 조회 메커니즘(lookup mechanism)을 통해 이루어진다는 점을 강조합니다.

---

## 10. 메시지 전달 신뢰성(Message Delivery Reliability)

이 문서는 로컬 및 분산 시스템 전반에서 액터 간 메시지 전달에 대한 Akka의 접근 방식을 설명합니다.

### 10.1 핵심 전달 보장(Core Delivery Guarantees)

Akka는 모든 메시지 송신에 대해 두 가지 근본적인 보장을 제공합니다.

1. **최대 한 번 전달(At-most-once delivery)** — 메시지가 손실될 수는 있지만, 인위적으로 중복(duplicate)되지는 않습니다.
2. **송신자-수신자 쌍별 메시지 순서(Message ordering per sender-receiver pair)** — 액터 A가 메시지 M1, M2, M3을 액터 B에게 직접 보내면, 그것들은 (도착한다면) 그 순서대로 도착합니다.

### 10.2 왜 보장된 전달이 아닌가(Why Not Guaranteed Delivery?)

프레임워크는 "보장된 전달(guaranteed delivery)"이라는 개념 자체가 모호하기 때문에 이를 주장하지 않습니다. "보장(guaranteed)"이 무엇을 의미하는지 판단하려면, 메시지가 네트워크로 전송되는 것인지, 호스트에 수신되는 것인지, 메일박스에 놓이는 것인지, 처리가 시작되는 것인지, 처리가 성공적으로 완료되는 것인지를 명확히 해야 합니다. 각 수준은 서로 다른 비용과 과제를 가집니다.

Akka는 "누수 추상화(leaky abstraction)"를 만드는 대신, 이러한 실패 가능성(fallibility)을 명시적으로 받아들입니다. 송신자가 성공을 확인할 수 있는 유일하게 신뢰할 수 있는 방법은, 수신자로부터 비즈니스 수준의 확인 응답 메시지(business-level acknowledgment message)를 받는 것입니다.

### 10.3 로컬 vs. 원격 동작(Local vs. Remote Behavior)

**로컬(JVM 내부) 송신**(Local (in-JVM) sends)은 메시지가 네트워크 문제 없이 직접 전달되므로 실질적으로 더 강한 신뢰성을 가집니다. 그러나 다음과 같은 이유로 여전히 실패할 수 있습니다.

- 스택 오버플로(stack overflow)
- 메모리 부족 오류(out-of-memory errors)
- 가득 찬 경계 메일박스(full bounded mailboxes)
- 액터 종료(actor termination)

**원격 송신**(Remote sends)은 추가적인 지연(latency) 과제에 직면합니다. 메시지 순서 보장은, 네트워크 전달 시간이 가변적이기 때문에 중개자(intermediaries)를 가로질러 이행적으로(transitively) 확장되지 않습니다.

### 10.4 더 강한 신뢰성 구축(Building Stronger Reliability)

보장된 전달이 필요한 애플리케이션을 위해, Akka는 상위 수준 패턴(higher-level patterns)을 제공합니다.

- 중복 제거(deduplication)를 갖춘 ACK-RETRY 프로토콜을 사용하는 **신뢰성 있는 전달(Reliable Delivery)** 기능
- 상태 재생을 위한 Akka Persistence를 통한 **이벤트 소싱(Event Sourcing)**
- 명시적 확인 응답(explicit acknowledgment)을 갖춘 커스텀 메일박스 구현

### 10.5 데드 레터(Dead Letters)

전달될 수 없는 메시지는 최선 노력(best-effort) 기반으로 `/deadLetters`라는 합성 액터(synthetic actor)에 도달합니다. 이는 전달 자체를 보장하기보다는 주로 디버깅(debugging)에 유용합니다.

---

## 11. 설정(Configuration)

### 11.1 개요(Overview)

이 페이지는 Akka 애플리케이션을 설정(configure)하는 방법을 설명합니다. "합리적인 기본값(sensible default values)이 제공"되므로 어떠한 설정 없이도 Akka를 사용할 수 있지만, 나중에 로깅(logging), 클러스터링(clustering), 직렬화기(serializers), 디스패처(dispatchers) 등의 설정을 조정해야 할 수 있습니다.

### 11.2 설정 소스 계층(Configuration Source Hierarchy)

Akka는 Typesafe Config 라이브러리를 사용합니다. 설정은 다음 순서로 로드됩니다.

1. 시스템 속성(System properties) — 가장 높은 우선순위
2. 애플리케이션 설정 파일(Application configuration files)
3. 레퍼런스 설정(Reference configuration) — 라이브러리 기본값

시스템은 클래스 패스 루트(class path root)에서 `application.conf`, `application.json`, `application.properties`를 찾으며, 이를 폴백(fallback)으로서의 `reference.conf` 파일들과 병합(merge)합니다.

### 11.3 라이선스 요구 사항(License Requirements)

"Akka는 프로덕션(production)에서 사용하려면 라이선스 키(license key)가 필요합니다. 무료 키는 https://akka.io/key 에서 얻을 수 있습니다." 로컬 개발(local development)의 경우, Akka는 키 없이 동작하지만 "키가 설정되지 않으면 일정 시간 후 종료(terminate after a while)"됩니다. 문서에는 라이선스 만료 전에 만료를 확인하는 방법을 보여 주는 코드 예제가 포함되어 있습니다.

### 11.4 주요 설정 주제(Key Configuration Topics)

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

## 12. 참고 자료

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
