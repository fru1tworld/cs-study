# Akka 디스패처, 메일박스, 테스트, 스타일 가이드

> 이 문서는 Akka 공식 문서의 "Dispatchers", "Mailboxes", "Testing", "Style Guide", "Coordinated Shutdown", "Coexisting (Typed/Classic)" 섹션을 한국어로 번역한 것입니다.
> 원본: https://doc.akka.io/libraries/akka-core/current/

---

## 목차

1. [디스패처(Dispatchers)](#1-디스패처dispatchers)
   - [1.1 디스패처란 무엇인가](#11-디스패처란-무엇인가)
   - [1.2 기본 디스패처와 내부 디스패처](#12-기본-디스패처와-내부-디스패처)
   - [1.3 디스패처 선택(DispatcherSelector)](#13-디스패처-선택dispatcherselector)
   - [1.4 디스패처 종류](#14-디스패처-종류)
   - [1.5 Fork-Join 실행기 설정](#15-fork-join-실행기-설정)
   - [1.6 Thread-Pool 실행기 설정](#16-thread-pool-실행기-설정)
   - [1.7 블로킹(blocking) 작업 처리](#17-블로킹blocking-작업-처리)
   - [1.8 디스패처 별칭(aliases)과 조회(lookup)](#18-디스패처-별칭aliases과-조회lookup)
2. [메일박스(Mailboxes)](#2-메일박스mailboxes)
   - [2.1 메일박스란 무엇인가](#21-메일박스란-무엇인가)
   - [2.2 메일박스 선택(MailboxSelector)](#22-메일박스-선택mailboxselector)
   - [2.3 기본 제공 메일박스 구현](#23-기본-제공-메일박스-구현)
   - [2.4 커스텀 메일박스 구현](#24-커스텀-메일박스-구현)
3. [테스트(Testing)](#3-테스트testing)
   - [3.1 개요와 모듈 설정](#31-개요와-모듈-설정)
   - [3.2 비동기 테스트(Asynchronous testing)](#32-비동기-테스트asynchronous-testing)
   - [3.3 동기 테스트(Synchronous behavior testing)](#33-동기-테스트synchronous-behavior-testing)
4. [스타일 가이드(Style Guide)](#4-스타일-가이드style-guide)
   - [4.1 함수형 스타일 vs 객체지향 스타일](#41-함수형-스타일-vs-객체지향-스타일)
   - [4.2 너무 많은 파라미터 전달 문제](#42-너무-많은-파라미터-전달-문제)
   - [4.3 메시지를 어디에 정의할 것인가](#43-메시지를-어디에-정의할-것인가)
   - [4.4 공개 vs 비공개 메시지](#44-공개-vs-비공개-메시지)
   - [4.5 람다 vs 메서드 참조, ask 패턴](#45-람다-vs-메서드-참조-ask-패턴)
   - [4.6 명명 규칙(Naming conventions)](#46-명명-규칙naming-conventions)
5. [코디네이티드 셧다운(Coordinated Shutdown)](#5-코디네이티드-셧다운coordinated-shutdown)
   - [5.1 개요](#51-개요)
   - [5.2 셧다운 단계(phases)](#52-셧다운-단계phases)
   - [5.3 태스크 등록](#53-태스크-등록)
   - [5.4 셧다운 실행](#54-셧다운-실행)
   - [5.5 단계 설정과 JVM 종료](#55-단계-설정과-jvm-종료)
6. [Typed와 Classic의 공존(Coexisting)](#6-typed와-classic의-공존coexisting)
   - [6.1 개요](#61-개요)
   - [6.2 ActorSystem 변환](#62-actorsystem-변환)
   - [6.3 액터의 생성과 관리](#63-액터의-생성과-관리)
   - [6.4 슈퍼비전(supervision) 차이](#64-슈퍼비전supervision-차이)

---

## 1. 디스패처(Dispatchers)

### 1.1 디스패처란 무엇인가

Akka의 `MessageDispatcher`는 Akka 액터(actor)를 "동작하게 만드는" 핵심 요소입니다. 비유하자면 기계를 움직이게 하는 엔진(engine)과 같은 존재입니다. 모든 `MessageDispatcher` 구현체는 `ExecutionContext`(스칼라의 실행 컨텍스트)이자 `Executor`(자바의 실행기) 역할을 동시에 수행합니다. 따라서 임의의 코드를 실행하는 데에도 사용할 수 있는데, 예를 들어 `Future`(스칼라) 또는 `CompletableFuture`(자바)를 실행하는 용도로 활용할 수 있습니다.

디스패처(dispatcher)는 액터로 들어온 메시지를 처리하기 위해 스레드(thread)를 할당하는 메커니즘을 담당합니다. 즉, 어떤 액터가 어떤 스레드 풀(thread pool) 위에서 메시지를 처리할지를 결정합니다.

---

### 1.2 기본 디스패처와 내부 디스패처

모든 `ActorSystem`은 기본 디스패처(default dispatcher)를 가지며, 별도로 디스패처를 설정하지 않은 경우 자동으로 이 기본 디스패처가 사용됩니다. 기본 디스패처는 일반적으로 "fork-join-executor" 설정을 사용하며, 이 설정은 대부분의 경우 매우 우수한 성능(excellent performance)을 제공합니다.

이와 별개로 Akka는 전용 내부 디스패처(internal dispatcher)를 유지합니다. 이 내부 디스패처는 프레임워크가 생성하는 액터(framework-spawned actors)를 사용자 애플리케이션의 부하(load)로부터 보호하기 위한 것입니다. 즉, 사용자 코드가 기본 디스패처를 과도하게 점유하더라도 Akka 내부의 핵심 액터들은 영향을 받지 않고 정상적으로 동작할 수 있습니다.

---

### 1.3 디스패처 선택(DispatcherSelector)

액터를 스폰(spawn)할 때 `DispatcherSelector`를 사용하여 디스패처를 지정할 수 있습니다. 사용 가능한 선택자는 다음과 같습니다.

- `DispatcherSelector.default()` — 시스템의 기본 디스패처를 사용합니다.
- `DispatcherSelector.blocking()` — 블로킹(blocking) 작업을 위한 디스패처를 사용합니다.
- `DispatcherSelector.sameAsParent()` — 부모 액터의 디스패처를 그대로 상속받아 사용합니다.
- `DispatcherSelector.fromConfig("name")` — 설정 파일(configuration)에서 지정한 이름의 디스패처를 로드하여 사용합니다.

스폰 시 다음과 같은 형태로 적용합니다(개념 예시).

```
context.spawn(behavior, "actor-name", DispatcherSelector.fromConfig("my-dispatcher"))
```

---

### 1.4 디스패처 종류

Akka에는 두 가지 주요 디스패처 유형이 있습니다.

**Dispatcher (이벤트 기반, Event-based)**

- 액터들의 집합을 스레드 풀(thread pool)에 바인딩(binding)합니다.
- 여러 액터가 공유(shareability)할 수 있으며, 공유 가능한 액터 수에 제한이 없습니다.
- 액터마다 하나의 메일박스(mailbox)를 생성합니다.
- `ExecutorService` 구현체에 의해 구동됩니다.

**PinnedDispatcher**

- 각 액터에 고유한(unique) 스레드 하나를 전담시킵니다.
- 공유가 불가능합니다(no shareability).
- 액터마다 하나의 메일박스를 생성합니다.
- 각 액터는 자신만의 단일 스레드 풀(single-thread pool)을 가집니다.

`PinnedDispatcher`는 특정 액터가 항상 같은 스레드 위에서 동작해야 하거나, 다른 액터의 영향을 전혀 받지 않아야 하는 특수한 상황에 적합합니다.

---

### 1.5 Fork-Join 실행기 설정

`fork-join-executor`의 주요 설정 항목은 다음과 같습니다.

- `parallelism-min` / `parallelism-max` — 스레드 개수의 하한과 상한을 지정합니다.
- `parallelism-factor` — 가용 프로세서 수(available processors)에 곱해지는 배수(multiplier)입니다. 예를 들어 factor가 2.0이고 코어가 8개라면 16개의 스레드를 사용하려는 의도입니다(`parallelism-min`/`parallelism-max` 범위 내에서).
- `maximum-spare-threads` — `ManagedBlocker`를 위해 추가로 사용할 수 있는 여유 스레드(spare threads) 수입니다.
- `throughput` — 디스패처가 다른 액터로 전환하기 전에 한 액터의 메시지를 몇 개나 연속으로 처리할지를 지정합니다.

**중요한 주의사항**: `fork-join-executor`의 `parallelism-max`는 `ForkJoinPool`이 할당하는 전체 스레드 수의 상한(upper bound)을 설정하는 것이 아닙니다. 이는 풀이 동시에 실행하려고 시도하는 활성 스레드 수에 관한 설정으로, 블로킹 등으로 인해 실제 생성되는 스레드 수는 이보다 많아질 수 있습니다.

설정 예시:

```
my-fork-join-dispatcher {
  type = Dispatcher
  executor = "fork-join-executor"
  fork-join-executor {
    parallelism-min = 2
    parallelism-factor = 2.0
    parallelism-max = 10
  }
  throughput = 100
}
```

---

### 1.6 Thread-Pool 실행기 설정

`thread-pool-executor`는 스레드 개수를 명시적으로 제어할 수 있게 해줍니다. 주요 설정 항목은 다음과 같습니다.

- `fixed-pool-size` — 정확히 고정된 스레드 개수를 지정합니다.
- `core-pool-size-min` / `core-pool-size-max` / `core-pool-size-factor` — 동적으로 코어 풀 크기를 산정하기 위한 설정입니다. `core-pool-size-factor`는 프로세서 수에 곱해지는 배수이며, 결과값은 min과 max 사이로 제한됩니다.
- `keep-alive-time` — 유휴(idle) 스레드가 종료되기까지 대기하는 시간입니다.
- `allow-core-timeout` — 코어 스레드도 타임아웃(timeout)으로 종료될 수 있는지 여부를 지정합니다.

설정 예시:

```
my-thread-pool-dispatcher {
  type = Dispatcher
  executor = "thread-pool-executor"
  thread-pool-executor {
    fixed-pool-size = 32
  }
  throughput = 1
}
```

---

### 1.7 블로킹(blocking) 작업 처리

#### 문제점

기본 디스패처에서 블로킹 호출(blocking call)을 수행하면 스레드 고갈(thread starvation)이 발생할 수 있습니다. 모든 스레드가 블로킹되어 버리면 블로킹되지 않는 액터들조차 실행될 기회를 얻지 못하게 되고, 결과적으로 시스템 전체의 성능이 저하됩니다.

#### 잘못된 해결책(Non-Solutions)

- 블로킹 코드를 `context.executionContext`를 사용해 `Future`로 감싸는 것은 문제를 해결하지 못합니다. 이는 단지 문제를 기본 디스패처로 옮기는 것에 불과합니다.
- `Await`(스칼라) 또는 `CompletableFuture::get()`(자바)을 사용하면 스레드 풀에 여유 스레드를 추가하라는 신호를 보내게 됩니다. 그러나 이렇게 추가된 스레드는 블로킹이 끝난 뒤에도 한동안 그대로 할당된 상태로 남아, 불필요한 컨텍스트 스위칭(context switching)과 자원 소비를 유발합니다.

#### 권장 해결책: 전용 디스패처(Dedicated Dispatcher)

블로킹 작업 전용으로 별도의 디스패처를 구성하는 것이 권장됩니다.

```
my-blocking-dispatcher {
  type = Dispatcher
  executor = "thread-pool-executor"
  thread-pool-executor {
    fixed-pool-size = 16
  }
  throughput = 1
}
```

이렇게 하면 블로킹 동작을 격리(isolate)하여 다른 액터들에게 영향을 주지 않습니다. 고정 풀 크기(fixed pool size)는 동시에 수행되는 블로킹 작업 수를 제한하여 자원 사용을 통제합니다. 이것이 리액티브 애플리케이션(reactive application)에서 모든 종류의 블로킹을 다루는 권장 방식이며, Akka HTTP, Akka Streams 등 Akka 기반의 다양한 리액티브 프레임워크 전반에 동일하게 적용됩니다.

#### 가상 스레드(Virtual Threads) 대안 (Java 21+)

Java 21 이상에서는 `virtual-thread-executor`를 구성하여 가상 스레드(virtual threads)를 활용할 수 있습니다. 가상 스레드는 블로킹이 발생하는 동안 OS 수준 스레드(OS-level thread)로부터 분리(detach)될 수 있습니다. 다만 스레드 풀과 달리 가상 스레드에는 동시 실행 개수의 본질적인 상한이 없다는 점에 유의해야 합니다. 그럼에도 블로킹 작업에 대해서는 일반 스레드 풀보다 더 효율적입니다.

---

### 1.8 디스패처 별칭(aliases)과 조회(lookup)

#### 별칭(Aliases)

디스패처 설정에서 문자열(string) 값을 지정하면 이는 별칭(alias)으로 동작하여, 다른 설정 위치로 리다이렉트(redirect)됩니다. 이를 통해 하나의 디스패처 인스턴스를 여러 식별자(identifier)에서 공유할 수 있습니다.

```
akka.actor.internal-dispatcher = akka.actor.default-dispatcher
```

위 예시는 내부 디스패처를 기본 디스패처와 동일한 것으로 매핑합니다.

#### 디스패처 조회(Looking up Dispatchers)

`Future`나 스케줄러(scheduler) 작업에 사용하기 위해 디스패처를 프로그래밍 방식으로 조회할 수 있습니다.

```
implicit val executionContext = context.system.dispatchers
  .lookup(DispatcherSelector.fromConfig("my-dispatcher"))
```

#### 설정 베스트 프랙티스

- **CPU 바운드 작업**: `core-pool-size-factor`를 사용해 프로세서 수 기반으로 설정합니다.
- **블로킹 I/O**: 동시에 블로킹되는 스레드 수를 제한하기 위해 고정 크기 스레드 풀(fixed-size thread pool)을 사용합니다.
- **종료 동작**: `shutdown-timeout`을 적절히 설정합니다. 기본값 1초(default 1 second)는 풀이 너무 일찍 종료되는 원인이 될 수 있습니다.
- **throughput 설정**: 값이 1이면 공정성(fairness)을 제공하고, 값이 클수록 처리량(throughput)을 우선시합니다.

---

## 2. 메일박스(Mailboxes)

### 2.1 메일박스란 무엇인가

Akka에서 각 액터는 메일박스(mailbox)를 가지며, 들어오는 메시지(incoming messages)는 처리되기 전에 이 메일박스에 큐(queue) 형태로 쌓입니다.

기본적으로는 무제한(unbounded) 메일박스가 사용되어 무한히 많은 메시지를 담을 수 있습니다. 그러나 이는 생산자(producer)가 소비자(consumer)보다 빠르게 메시지를 만들어내는 경우 메모리 문제(memory issue)를 일으킬 수 있습니다. 이때 제한(bounded) 메일박스를 사용하면 용량을 초과하는 메시지를 데드레터(deadletters)로 보내거나 블로킹할 수 있으며, 설정을 통해 메일박스 선택을 외부 설정 파일로 미룰 수도 있습니다.

#### 의존성 설정

메일박스는 Akka 코어의 `akka-actor` 의존성에 포함되어 함께 제공됩니다. `akka-actor-typed`를 사용하려면 다음을 추가합니다.

**sbt:**
```
"com.typesafe.akka" %% "akka-actor-typed" % "2.10.19"
```

**Maven/Gradle:** 모듈 간 버전 일관성을 위해 Akka BOM(bill of materials)을 활용하는 것이 권장됩니다.

---

### 2.2 메일박스 선택(MailboxSelector)

#### 액터별 선택(Per-Actor Selection)

액터를 스폰할 때 `MailboxSelector`를 사용합니다.

- `MailboxSelector.bounded(100)` — 용량이 100인 제한 메일박스를 생성합니다.
- `MailboxSelector.fromConfig("path.to.config")` — 설정 파일에서 지정한 이름의 메일박스 설정을 읽어옵니다.

#### 기본 메일박스(Default Mailbox)

`SingleConsumerOnlyUnboundedMailbox`가 기본 메일박스로 사용됩니다. 이는 다중 생산자/단일 소비자(MPSC, multiple-producer single-consumer) 큐를 사용하며, 일반적인 액터 워크로드(workload)에 최적화되어 있습니다.

설정 예시:

```
my-app {
  my-special-mailbox {
    mailbox-type = "akka.dispatch.SingleConsumerOnlyUnboundedMailbox"
  }
}
```

해당 메일박스 타입은 명명된 설정 섹션(named config section)을 전달받으며, 기본 메일박스 설정으로 폴백(fallback)됩니다.

---

### 2.3 기본 제공 메일박스 구현

**무제한(Unbounded) 옵션:**

- **SingleConsumerOnlyUnboundedMailbox** (기본값): MPSC 큐 기반, 논블로킹(non-blocking). `BalancingDispatcher`와는 호환되지 않습니다.
- **UnboundedMailbox**: `ConcurrentLinkedQueue` 기반, 논블로킹.
- **UnboundedControlAwareMailbox**: 이중 큐(dual queue) 구조를 사용하여 `ControlMessage` 인스턴스를 우선 처리합니다.
- **UnboundedPriorityMailbox**: `PriorityBlockingQueue` 기반. 우선순위가 동일한 메시지들 사이의 FIFO 순서는 정의되지 않습니다(undefined).
- **UnboundedStablePriorityMailbox**: `PriorityBlockingQueue`를 안정화기(stabilizer)로 감싸, 우선순위가 동일한 메시지에 대해 FIFO 순서를 보존합니다.

**제한 + 논블로킹(Bounded Non-Blocking):**

- **NonBlockingBoundedMailbox**: 효율적인 MPSC 큐 기반. 용량을 초과한 메시지는 데드레터(deadletters)로 폐기(discard)됩니다.

**블로킹(Blocking) — `mailbox-push-timeout-time`을 0으로 설정하여 사용:**

- **BoundedMailbox**: `LinkedBlockingQueue` 기반.
- **BoundedPriorityMailbox**: 우선순위 큐 기반. 동일 우선순위 간 순서는 정의되지 않음.
- **BoundedStablePriorityMailbox**: 동일 우선순위에 대해 안정적인 FIFO 순서를 보장.
- **BoundedControlAwareMailbox**: 제어 메시지(control message)를 우선 처리하며, 용량 도달 시 블로킹.

#### 메일박스 선택 시 고려사항

- 제한 메일박스(bounded mailbox)는 무제한 메모리 증가를 방지하지만 메시지를 잃을 수 있습니다.
- 우선순위 메일박스(priority mailbox)는 추가 오버헤드(overhead)가 있으므로 메시지 순서가 중요한 경우에만 사용하세요.
- 제어 인지 메일박스(control-aware mailbox)는 관리용(administrative) 메시지에 빠르게 반응해야 하는 시스템에 적합합니다.

---

### 2.4 커스텀 메일박스 구현

커스텀 메일박스는 두 개(혹은 세 개)의 구성 요소를 구현하여 만듭니다.

**1. MessageQueue 구현:**

`MessageQueue` 인터페이스를 구현합니다. 다음과 같은 메서드를 포함합니다.

- `enqueue()` — 메시지를 큐에 넣음
- `dequeue()` — 큐에서 메시지를 꺼냄
- `numberOfMessages()` — 큐에 들어 있는 메시지 개수 반환
- `hasMessages()` — 큐에 메시지가 있는지 여부 반환
- `cleanUp()` — 정리 작업 수행

내부적으로는 `ConcurrentLinkedQueue` 같은 동시성(concurrent) 자료구조를 사용합니다.

**2. MailboxType 클래스:**

`MailboxType`을 확장(extend)하고 `ProducesMessageQueue[YourMessageQueueType]`을 구현합니다. `ActorSystem.Settings`와 `Config` 파라미터를 받는 생성자(constructor)를 반드시 포함해야 합니다 — Akka가 리플렉션(reflection)을 통해 이 생성자를 호출하기 때문입니다. 또한 새로운 `MessageQueue` 인스턴스를 반환하는 `create()` 메서드를 구현합니다.

**3. 마커 트레이트/인터페이스(Marker Trait/Interface, 선택 사항):**

메일박스 요구사항 매핑(requirement mapping)을 위한 마커 시맨틱 트레이트(marker semantic trait)를 정의할 수 있습니다. 이를 통해 디스패처가 메일박스 호환성을 검증할 수 있습니다.

**설정:**

커스텀 메일박스의 완전한 클래스 이름(fully-qualified class name)을 디스패처 또는 메일박스 설정의 `mailbox-type` 필드에 지정합니다.

#### 커스텀 구현 시 주의사항

커스텀 구현은 정리(cleanup) 작업을 적절히 처리하여, 남아 있는 메시지를 데드레터(deadletters)로 옮겨야 합니다.

---

## 3. 테스트(Testing)

### 3.1 개요와 모듈 설정

Akka Typed 액터의 테스트는 두 가지 방식으로 수행할 수 있습니다.

> "테스트는 실제 `ActorSystem`을 사용하는 비동기(asynchronously) 방식으로 하거나, `BehaviorTestKit`을 사용하여 테스트 스레드(testing thread) 위에서 동기적으로(synchronously) 수행할 수 있습니다."

**동기 테스트(Synchronous Testing)**: `Behavior` 로직을 격리하여 테스트하는 데 적합합니다. 다만 사용 가능한 기능은 제한적입니다.

**비동기 테스트(Asynchronous Testing)**: 여러 액터 간의 상호작용을 보다 현실적인 환경에서 테스트하는 데 권장됩니다.

#### 모듈 설정

- **sbt**: `akka-actor-testkit-typed` (버전 2.10.19) 사용

```
"com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.10.19" % Test
```

- **Maven / Gradle**: 스칼라 바이너리 버전을 명시한 Akka BOM 기반 설정 사용
- **테스트 프레임워크**: ScalaTest 3.2.17 사용 권장

---

### 3.2 비동기 테스트(Asynchronous testing)

비동기 테스트는 실제 `ActorSystem`을 사용하며, `ActorTestKit`을 중심으로 진행됩니다. 다루는 주제는 다음과 같습니다.

- **기본 예제(Basic examples)**: 실제 액터 시스템 위에서 액터를 스폰하고 메시지를 보내며 응답을 검증합니다. 응답 검증에는 주로 `TestProbe`를 사용합니다.
- **목 처리된 동작 관찰(Mocked behavior observation)**: 테스트 대상 액터가 다른 액터와 어떻게 상호작용하는지를 관찰합니다.
- **테스트 프레임워크 통합(Framework integration)**: ScalaTest 등과의 통합 방법을 다룹니다.
- **설정(Configuration)**: 테스트용 `ActorSystem`에 커스텀 설정을 적용하는 방법.
- **스케줄러 제어(Scheduler control)**: 시간 기반 동작을 테스트하기 위해 스케줄러를 제어합니다.
- **로깅 테스트(Logging tests)**: 액터가 기대한 로그 메시지를 출력하는지 검증합니다.

핵심 도구:

- `ActorTestKit` — 테스트용 실제 액터 시스템과 헬퍼(helper)를 제공합니다.
- `TestProbe` — 메시지를 수신하고, 기대한 메시지가 도착하는지(`expectMessage` 등) 검증하는 데 사용하는 프로브(probe)입니다. 또한 다른 액터에게 전달할 `ActorRef`를 제공하여, 응답이 프로브로 들어오게 만들 수 있습니다.

---

### 3.3 동기 테스트(Synchronous behavior testing)

동기 테스트는 `BehaviorTestKit`을 사용하여 테스트 스레드 위에서 직접 동작을 실행합니다. 실제 액터 시스템 없이 `Behavior` 로직을 단위(unit) 수준으로 검증합니다. 다루는 주제는 다음과 같습니다.

- **자식 스폰(Child spawning)**: 테스트 대상 동작이 자식 액터를 스폰하는지 검증합니다.
- **메시지 전송(Message sending)**: 동작이 다른 액터에게 메시지를 보내는지 검증합니다.
- **효과 테스트(Effect testing)**: 스폰, 메시지 전송, 워치(watch) 등 액터가 일으키는 부수 효과(effect)를 검증합니다. `BehaviorTestKit`은 이러한 효과를 기록하여 확인할 수 있게 해줍니다.
- **로그 메시지 검증(Log message verification)**: 동작이 출력한 로그 메시지를 검증합니다.

동기 테스트는 빠르고 결정적(deterministic)이지만, 실제 동시성(concurrency)이나 타이밍 관련 동작은 재현할 수 없으므로 단일 `Behavior`의 로직 검증에 한정하여 사용하는 것이 좋습니다.

#### 프로젝트 정보(참고)

- **라이선스**: BUSL-1.1
- **상태**: Lightbend가 지원(Supported)
- **JDK 지원**: 11, 17, 21
- **Scala 버전**: 2.13.17, 3.3.7

---

## 4. 스타일 가이드(Style Guide)

### 4.1 함수형 스타일 vs 객체지향 스타일

Akka Typed에서 액터 동작(behavior)을 구현하는 데에는 두 가지 주요 스타일이 있습니다.

**함수형 스타일(Functional Style)**

- 불변(immutable) 상태를 동작들 사이에서 파라미터로 전달합니다.
- 상태 변경은 새로운 동작 인스턴스를 반환(return)하는 방식으로 표현합니다.
- 예: 메시지를 처리한 뒤 갱신된 상태를 인자로 하여 다음 동작을 반환합니다.

**객체지향 스타일(Object-Oriented Style)**

- `AbstractBehavior`를 확장하는 클래스 내부의 가변(mutable) 필드를 사용합니다.
- 상태를 인스턴스 변수(instance variable)로 유지합니다.

두 방식 모두 프로젝트의 필요와 개발자의 친숙도에 따라 각자의 장점이 있습니다. 한 가지를 선택해 일관성 있게 사용하는 것이 좋습니다.

---

### 4.2 너무 많은 파라미터 전달 문제

함수형 스타일의 액터가 많은 파라미터를 필요로 하게 되면 코드가 번잡해질 수 있습니다. 이때 권장되는 방법은 다음과 같습니다.

> "생성자(constructor) 성격의 파라미터들을 불변 필드를 가진 별도의 클래스로 캡슐화(encapsulate)하고, 실제로 변하는 상태(changing state)만 별도의 파라미터로 유지한다."

즉, 변하지 않는 의존성과 설정 값들은 하나의 클래스로 묶어두고, 메시지 처리마다 실제로 변경되는 값만 동작 간에 전달함으로써, 파라미터 전달의 부담을 줄이면서도 중요한 부분에서는 함수형의 순수성(functional purity)을 유지할 수 있습니다.

---

### 4.3 메시지를 어디에 정의할 것인가

메시지는 해당 메시지를 사용하는 동작(behavior)과 함께 정의하는 것이 좋습니다. 일반적으로 다음 위치에 둡니다.

- **Scala**: 컴패니언 객체(companion object) 내부
- **Java**: 정적 내부 클래스(static inner class)로

이렇게 함께 두면(co-location) 발견 가능성(discoverability)이 좋아지고, 액터 이름을 접두사로 붙이는 명명을 자연스럽게 유도합니다. 예를 들어 그냥 `Increment`가 아니라 `Counter.Increment`처럼 사용하게 됩니다.

여러 액터가 공유하는 프로토콜(protocol)의 경우, 전용 프로토콜 클래스나 인터페이스에 메시지를 정의하는 것을 고려할 수 있습니다.

---

### 4.4 공개 vs 비공개 메시지

내부 전용(internal-only) 메시지는 비공개(private)로 표시하여, 외부 코드가 직접 그 메시지를 보내지 못하도록 막는 것이 좋습니다. 예를 들어 타이머나 어댑터를 통해서만 들어와야 하는 내부 메시지를 외부에서 직접 보내는 것을 방지합니다.

또한 커맨드(command)에 대해서는 봉인된 트레이트 계층(sealed trait hierarchy, Scala)을 사용하는 것이 권장됩니다. 이렇게 하면 패턴 매칭(pattern matching) 시 컴파일러가 모든 경우를 다루었는지(exhaustiveness) 검사해 주므로, 메시지를 추가했을 때 처리 누락을 컴파일 시점에 발견할 수 있습니다.

---

### 4.5 람다 vs 메서드 참조, ask 패턴

코드 품질을 위한 관례는 다음과 같습니다.

- 짧은 람다(lambda) 표현식에서는 로직을 직접 작성하기보다 이름 있는 메서드(named method)로 위임(delegate)하여 가독성을 높입니다.
- 람다보다 메서드 참조(method reference, 예: `this::handler`)를 우선적으로 사용합니다.
- 일관성을 위해 `?` 연산자(operator)보다 `ask` 메서드를 사용합니다.
- `setup`, `withTimers`, `withStash` 블록은 필요에 따라 중첩(nesting)하여 사용하며, 일반적으로 `setup`을 가장 바깥쪽(outermost)에 둡니다.

---

### 4.6 명명 규칙(Naming conventions)

- 응답(response)을 받을 `ActorRef` 파라미터에는 `replyTo`라는 이름을 사용합니다.
- 액터로 들어오는 메시지(incoming message)는 "커맨드(command)"라고 부르며, 상위 타입(super-type) 이름으로 `Command`를 사용합니다.
- 영속화(persist)되는 이벤트(event)에는 과거 시제(past tense)를 사용합니다. 예: `Incremented`(증가됨), `Decremented`(감소됨).

이러한 명명 규칙은 커맨드(명령, 현재형)와 이벤트(이미 일어난 사실, 과거형)를 명확히 구분하여, 특히 이벤트 소싱(event sourcing) 기반 액터에서 코드의 의도를 분명히 드러냅니다.

---

## 5. 코디네이티드 셧다운(Coordinated Shutdown)

### 5.1 개요

`CoordinatedShutdown` 확장(extension)은 `ActorSystem`의 정돈된 종료(orderly termination)를 관리합니다. 이는 설정에 정의된 단계(phase)들로 그룹화된 태스크(task)들을 등록하고, 종료 시 이 태스크들을 정해진 순서대로 실행합니다.

주요 특징:

- 단계들은 방향성 비순환 그래프(DAG, directed acyclic graph)를 형성하며, 위상 정렬(topological sorting)을 통해 실행 순서가 결정됩니다.
- 같은 단계 내의 태스크들은 병렬(parallel)로 실행되며, 다음 단계는 이전 단계가 완료될 때까지 대기합니다.
- 루트 액터(root actor)가 종료되거나 클러스터 노드(cluster node)가 떠날 때 자동으로 트리거(trigger)됩니다.

---

### 5.2 셧다운 단계(phases)

문서가 정의하는 주요 단계는 순서대로 다음과 같습니다.

1. **before-service-unbind** — 애플리케이션 태스크를 위한 최초 단계입니다.
2. **service-unbind** — 새로운 연결(connection) 수락을 중단합니다.
3. **service-requests-done** — 진행 중인 요청(in-progress requests)이 완료되기를 기다립니다.
4. **service-stop** — 남아 있는 연결을 강제로 종료합니다.
5. **before-cluster-shutdown** — 클러스터 종료 전에 수행할 애플리케이션 커스텀 태스크.
6. **cluster-sharding-shutdown-region** — Cluster Sharding을 우아하게(graceful) 종료합니다.
7. **cluster-leave** — leave 커맨드를 발행(emit)합니다.
8. **cluster-exiting** — 클러스터 싱글톤(cluster singleton)을 종료합니다.
9. **cluster-exiting-done** — exiting 완료를 기다립니다.
10. **cluster-shutdown** — 클러스터 확장(cluster extension)을 종료합니다.
11. **before-actor-system-terminate** — `ActorSystem` 종료 전에 수행할 커스텀 태스크.
12. **actor-system-terminate** — 마지막 단계로, 실제로 `ActorSystem`을 종료합니다.

클러스터 관련 단계(5~10)는 Akka Cluster를 사용할 때만 의미가 있으며, 단일 노드 애플리케이션에서는 사실상 비어 있는 채로 빠르게 지나갑니다.

---

### 5.3 태스크 등록

태스크는 `CoordinatedShutdown`의 다음 메서드를 통해 추가합니다.

- **addTask()** — `Future[Done]`(스칼라) 또는 `CompletionStage<Done>`(자바)을 반환하는 표준 셧다운 태스크를 등록합니다. 특정 단계(phase)에 소속시켜 등록합니다.
- **addCancellableTask()** — 취소 가능한(cancellable) 태스크를 등록합니다. 반환된 핸들을 통해 나중에 등록을 취소할 수 있습니다.
- **addJvmShutdownHook()** — 커스텀 JVM 셧다운 훅(JVM shutdown hook)을 등록합니다.

태스크는 자신이 속한 단계 안에서 다른 태스크들과 병렬로 실행되며, 후속 단계는 이전 단계의 모든 태스크가 완료되기를 기다립니다.

개념 예시(태스크 등록):

```
CoordinatedShutdown.get(system).addTask(
  CoordinatedShutdown.PhaseBeforeServiceUnbind(),
  "my-task-name") { () =>
    // 종료 시 수행할 작업
    Future.successful(Done)
}
```

---

### 5.4 셧다운 실행

셧다운은 다음 방법으로 시작할 수 있습니다.

- `system.terminate()` — `ActorSystemTerminateReason`을 사유(reason)로 사용하여 종료합니다.
- `CoordinatedShutdown(system).run(reason)` — 커스텀 종료 사유(custom shutdown reason)를 지정하여 실행합니다.

`run` 메서드는 멱등성(idempotent)을 가지므로 여러 번 호출해도 안전합니다. 즉, 이미 셧다운이 진행 중이라면 추가 호출은 진행 중인 셧다운의 완료를 가리키는 동일한 결과를 반환합니다.

자동 트리거:

- 루트 액터가 종료될 때 자동으로 실행됩니다.
- 클러스터 노드가 클러스터를 떠날 때(departure) 자동으로 실행됩니다.

---

### 5.5 단계 설정과 JVM 종료

각 단계는 설정으로 다음 항목을 조정할 수 있습니다.

- **timeout** — 해당 단계의 타임아웃(기본값은 단계마다 다름). 타임아웃이 지나도 완료되지 않은 태스크가 있어도, 기본적으로는 다음 단계 진행을 막지 않습니다.
- **recover = off** — 해당 단계 실패 시 셧다운을 중단(abort)하도록 설정합니다(기본은 복구하여 다음 단계로 진행).
- **enabled = off** — 해당 단계의 태스크들을 건너뛰도록(skip) 설정합니다.
- **depends-on** — 단계 의존성을 DAG 형태로 정의합니다.

전역 설정:

- **akka.coordinated-shutdown.exit-jvm** — `System.exit`를 호출하여 JVM을 강제 종료(force exit)할지 여부.
- **akka.coordinated-shutdown.run-by-jvm-shutdown-hook** — SIGTERM 등 JVM 셧다운 시 자동으로 코디네이티드 셧다운을 실행할지 여부.

중요한 동작:

- 단계들은 DAG를 이루며 위상 정렬을 통해 순서가 정해집니다.
- 타임아웃 후 완료되지 않은 태스크는 (`recover = off`가 아닌 한) 다음 단계를 막지 않습니다.
- **Kubernetes 고려사항**: 기본적으로 SIGKILL 전 30초의 그레이스 기간(grace period)이 주어집니다. 셧다운 단계들의 전체 소요 시간이 이 기간 내에 끝나도록 설정하는 것이 중요합니다.

---

## 6. Typed와 Classic의 공존(Coexisting)

### 6.1 개요

기존 시스템에서 Akka Typed로의 전환은 점진적으로(gradually) 이루어지기 때문에, Typed 액터와 Classic 액터는 같은 `ActorSystem` 안에서 공존(coexist)할 수 있도록 설계되어 있습니다.

두 개의 별도 `ActorSystem` 구현이 존재합니다.

- `akka.actor.ActorSystem` — Classic(클래식)
- `akka.actor.typed.ActorSystem` — Typed(타입)

Typed와 Classic 액터는 다음과 같은 방식으로 상호작용할 수 있습니다.

- Classic 액터 시스템이 Typed 액터를 생성할 수 있습니다.
- 메시지가 두 종류 사이에서 양방향(bidirectional)으로 흐를 수 있습니다.
- Classic 부모에서 Typed 자식을 스폰하고 슈퍼바이즈(supervise)할 수 있으며, 그 반대도 가능합니다.
- 서로를 워치(watch)할 수 있습니다.
- Classic 시스템을 Typed로 변환할 수 있습니다.

---

### 6.2 ActorSystem 변환

**Classic에서 Typed로:**

Classic `ActorSystem`을 Typed로 변환하려면 어댑터(adapter) 패턴을 사용합니다.

- **Scala**: `import akka.actor.typed.scaladsl.adapter._`를 임포트하면 `toTyped` 같은 확장 메서드(extension method)를 사용할 수 있습니다.
- **Java**: `akka.actor.typed.javadsl.Adapter` 클래스의 정적 메서드(static method)를 사용합니다.

**Typed에서 Classic으로:**

마찬가지로 Typed 시스템에서도 동일한 어댑터 임포트를 통해 Classic 기능에 접근할 수 있습니다. `toClassic` 변환을 사용하고, `Adapter.spawn()` 또는 `Adapter.actorOf()`를 통해 Classic 액터를 스폰할 수 있습니다.

---

### 6.3 액터의 생성과 관리

**Classic이 Typed를 생성:**

Classic 액터는 (어댑터를 임포트한 후) 컨텍스트 확장 메서드(context extension method)를 사용해 Typed 자식 액터를 스폰할 수 있습니다. 이 Typed 자식을 워치하고 메시지를 보낼 수 있으며, reply-to 패턴(응답 받을 `ActorRef`를 메시지에 담아 보내는 방식)을 통해 응답을 받습니다.

**Typed가 Classic을 생성:**

Typed 액터는 `Adapter.actorOf()`를 사용해 Classic 자식 액터를 생성합니다. 생명주기 모니터링을 위해 `Adapter.watch()`를 사용하며, 메시지 파라미터를 통해 발신자(sender)를 명시적으로 전달합니다(Classic의 암묵적 `sender()`가 Typed에는 없기 때문).

---

### 6.4 슈퍼비전(supervision) 차이

문서에서 강조하는 중요한 차이는 다음과 같습니다.

> "Classic 액터의 기본 슈퍼비전(default supervision)은 재시작(restart)이고, Typed의 기본 슈퍼비전은 정지(stop)이다."

즉, 두 모델의 기본 장애 처리 전략이 다릅니다. Classic은 예외가 발생하면 액터를 재시작하는 것이 기본이고, Typed는 예외가 발생하면 액터를 정지시키는 것이 기본입니다. 액터를 혼합하여 사용할 때, 자식 슈퍼비전(child supervision)의 기본값은 부모의 타입을 따릅니다. 따라서 Classic 부모 아래의 자식은 Classic의 재시작 정책을, Typed 부모 아래의 자식은 Typed의 정지 정책을 기본으로 적용받게 됩니다. 이 차이를 인지하지 못하면 마이그레이션 과정에서 장애 처리 동작이 예기치 않게 바뀔 수 있으므로 주의가 필요합니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Dispatchers](https://doc.akka.io/libraries/akka-core/current/typed/dispatchers.html)
- [Mailboxes](https://doc.akka.io/libraries/akka-core/current/typed/mailboxes.html)
- [Testing](https://doc.akka.io/libraries/akka-core/current/typed/testing.html)
- [Style Guide](https://doc.akka.io/libraries/akka-core/current/typed/style-guide.html)
- [Coordinated Shutdown](https://doc.akka.io/libraries/akka-core/current/coordinated-shutdown.html)
- [Coexisting](https://doc.akka.io/libraries/akka-core/current/typed/coexisting.html)
