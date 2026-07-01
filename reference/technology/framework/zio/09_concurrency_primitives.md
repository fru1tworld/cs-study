# ZIO 동시성 프리미티브

> 원본: https://zio.dev/reference/concurrency/

---

## 목차

1. [동시성 프리미티브 개요(Overview)](#1-동시성-프리미티브-개요overview)
2. [Promise — 단일 할당 동기화 변수](#2-promise--단일-할당-동기화-변수)
3. [Queue — 비동기 동시성 큐](#3-queue--비동기-동시성-큐)
4. [Hub — 브로드캐스트 메커니즘](#4-hub--브로드캐스트-메커니즘)
5. [Semaphore — 권한 기반 접근 제어](#5-semaphore--권한-기반-접근-제어)
6. [동기화 보조 도구 개요(Synchronization Primitives)](#6-동기화-보조-도구-개요synchronization-primitives)
7. [ReentrantLock — 재진입 가능 락](#7-reentrantlock--재진입-가능-락)
8. [CountDownLatch — 카운트다운 래치](#8-countdownlatch--카운트다운-래치)
9. [CyclicBarrier — 순환 배리어](#9-cyclicbarrier--순환-배리어)
10. [ConcurrentMap — 동시성 맵](#10-concurrentmap--동시성-맵)
11. [ConcurrentSet — 동시성 셋](#11-concurrentset--동시성-셋)
12. [참고 자료](#12-참고-자료)

---

## 1. 동시성 프리미티브 개요(Overview)

대부분의 동시성 프로그램(concurrent program)에서는 여러 파이버(fiber)가 하나의 공유 상태(shared state)에 접근합니다. 이는 경쟁 상태(race condition)를 유발하므로, 모든 스레드 간에 "상태에 대한 일관된 관점(a consistent view of states)"을 유지하는 방법이 필요합니다.

### 동시성을 다루는 두 가지 접근

ZIO는 동시성 문제를 두 가지 모델로 다룹니다.

1. **공유 상태(Shared State)** — 여러 스레드가 동일한 메모리 위치를 통해 통신합니다.
2. **메시지 전달(Message Passing)** — 각 스레드가 분산된 상태(distributed state)를 유지하고 메시지를 교환합니다.

공유 상태 안에서, 전통적인 방식은 락(lock)을 사용하거나 비교-후-교환(compare-and-swap, CAS) 같은 논블로킹(non-blocking) 원자적 연산을 사용합니다.

### 전통적인 락의 한계

락 기반 동시성(lock-based concurrency)에는 다음과 같은 심각한 문제가 있습니다.

- 락을 잘못 사용하면 교착 상태(deadlock)로 이어질 수 있으며, 이를 피하려면 세심한 순서 관리(careful ordering)가 필요합니다.
- 어느 코드 영역이 취약한지(vulnerable) 파악하기가 매우 어렵습니다.
- 복잡도가 커질수록 확장(scaling)이 어려워집니다.
- 락이 걸린 구역(locked section) 내부에서 예외 처리(exception handling)를 할 때 세심한 주의가 필요합니다.
- 락 메커니즘은 프로그램 조각들의 캡슐화 속성(encapsulation property)을 위반합니다.

### ZIO의 논블로킹(Lock-Free) 대안

락 대신 ZIO는 논블로킹 알고리즘(non-blocking algorithms)의 한 변형인 락-프리(lock-free) 동시성 모델을 사용합니다. 이 메커니즘은 CAS 연산에 의존합니다. 즉, 변수는 처음 읽은 값과 일치할 때에만 갱신되며, 그렇지 않으면 연산이 재시도(retry)됩니다.

### 핵심 장점

- **조합 가능(Composable)** — 선언적(declarative) 스타일과 재사용 가능한 프리미티브를 제공합니다.
- **논블로킹(Non-blocking)** — 완전히 비동기적인(asynchronous) 연산입니다.
- **자원 안전(Resource Safe)** — 인터럽트(interruption)가 발생해도 자원이 누수(leak)되지 않습니다.

### 제공되는 동시성 프리미티브

- **Ref** — 공유되고 갱신 가능한 상태를 위한 원자적 참조(atomic reference). *(이 프리미티브는 07 문서에서 자세히 다룹니다.)*
- **Promise** — 파이버 동기화를 위한 단일 할당(single-assignment) 변수.
- **Semaphore** — 권한(permit) 기반 접근 제어.
- **Queue** — 비동기·논블로킹 메시지 큐.
- **Hub** — 여러 구독자를 위한 브로드캐스트(broadcasting) 메커니즘.

> 참고: `Ref` / `Ref.Synchronized`는 07 문서에서, STM(소프트웨어 트랜잭셔널 메모리)은 10 문서에서 각각 다룹니다.

---

## 2. Promise — 단일 할당 동기화 변수

### 정의

`Promise[E, A]`는 정확히 한 번(exactly once) 설정될 수 있는 `IO[E, A]` 타입의 변수입니다. 공식 문서에서는 이를 "아직 사용 가능하지 않을 수 있는 단일 값을 나타내는 순수 함수형 동기화 프리미티브(a purely functional synchronization primitive)"로 설명합니다.

핵심 특성은 다음과 같습니다.

- 생성 시점에는 비어 있는(empty) 상태로 시작합니다.
- 정확히 한 번만 완료(complete)됩니다.
- 완료된 이후에는 절대 다시 비워지거나(empty) 수정되지 않습니다.
- 파이버 간(fiber-to-fiber) 동기화에 유용합니다.
- 완료를 기다리는 동안 파이버는 (커널 스레드가 아니라) **의미론적으로 블로킹**(semantically block)됩니다.

### 생성(Creation)

`Promise.make[E, A]`를 사용하여 생성하며, `UIO[Promise[E, A]]`를 반환합니다.

```scala
val ioPromise1: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
```

### Promise 완료(Completing a Promise)

완료 메서드는 성공(`true`) 또는 이미 완료됨(`false`)을 나타내는 `UIO[Boolean]`을 반환합니다.

**succeed** — 성공 값으로 완료:

```scala
val ioBooleanSucceeded: UIO[Boolean] = ioPromise1.flatMap(promise => promise.succeed("I'm done"))
```

**fail** — 에러로 완료:

```scala
val ioPromise2: UIO[Promise[Exception, Nothing]] = Promise.make[Exception, Nothing]
val ioBooleanFailed: UIO[Boolean] = ioPromise2.flatMap(promise => promise.fail(new Exception("boom")))
```

그 밖의 완료 메서드:

- **done** — `Exit[E, A]` 값으로 완료합니다.
- **complete** — 이펙트(effect)를 한 번 실행하고 그 결과를 대기 중인 모든 파이버에 전파합니다.
- **completeWith** — 첫 번째 호출자가 승리(first caller wins)하며, 이펙트는 대기 중인 각 파이버마다 실행됩니다. (주의해서 사용해야 합니다.)
- **die** — `Throwable` 결함(defect)으로 실패시킵니다.
- **failCause** — `Cause[E]`로 완료합니다.
- **interrupt** — Promise를 인터럽트합니다.

> 주의: `complete`와 `completeWith`의 차이에 유의해야 합니다. `complete`는 이펙트를 한 번만 실행하여 그 결과를 모든 대기 파이버에 동일하게 전달하는 반면, `completeWith`는 대기 중인 각 파이버마다 이펙트를 실행합니다. 예를 들어 이펙트가 난수를 생성한다면 `complete`는 모두 동일한 값을, `completeWith`는 파이버마다 다른 값을 받게 됩니다.

### 대기(Awaiting)

완료될 때까지 현재 파이버를 일시 중단(suspend)합니다.

```scala
val ioPromise3: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
val ioGet: IO[Exception, String] = ioPromise3.flatMap(promise => promise.await)
```

### 폴링(Polling)

일시 중단 없이 완료 상태를 조회합니다.

```scala
val ioPromise4: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
val ioIsItDone: UIO[Option[IO[Exception, String]]] = ioPromise4.flatMap(p => p.poll)
val ioIsItDone2: IO[Option[Nothing], IO[Exception, String]] = ioPromise4.flatMap(p => p.poll.some)
```

`Option[IO[E, A]]`를 반환합니다 — 아직 완료되지 않았으면 `None`, 완료되었으면 `Some`을 반환합니다.

- **isDone** — 완료 여부를 나타내는 `UIO[Boolean]`을 반환합니다.

### 전체 예제

```scala
import java.io.IOException

val program: ZIO[Any, IOException, Unit] =
  for {
    promise         <-  Promise.make[Nothing, String]
    sendHelloWorld  =   (ZIO.succeed("hello world") <* ZIO.sleep(1.second)).flatMap(promise.succeed)
    getAndPrint     =   promise.await.flatMap(Console.printLine(_))
    fiberA          <-  sendHelloWorld.fork
    fiberB          <-  getAndPrint.fork
    _               <-  (fiberA zip fiberB).join
  } yield ()
```

이 예제는 파이버 간 통신을 보여 줍니다. 한 파이버는 지연(delay) 후에 Promise를 완료하고, 다른 파이버는 그 결과를 기다렸다가(await) 출력합니다.

---

## 3. Queue — 비동기 동시성 큐

### 정의

`Queue[A]`는 타입 `A`의 값들을 담으며 두 가지 기본 연산을 가집니다. `offer`는 `A`를 `Queue`에 넣고, `take`는 `Queue`에서 가장 오래된 값을 제거하고 반환합니다.

Queue는 "조합 가능하고 투명한 배압(composable and transparent back-pressure)을 갖춘 경량 인메모리 큐(lightweight in-memory queue)"로, ZIO 위에 구축되어 있습니다. 완전히 비동기적이며(락이나 블로킹 없음), 순수 함수형(purely-functional)이고 타입 안전(type-safe)합니다.

### Queue 생성

**Bounded(유한, 배압 적용):** 가득 차면 공간이 생길 때까지 `offer`가 일시 중단됩니다.

```scala
val boundedQueue: UIO[Queue[Int]] = Queue.bounded[Int](100)
```

**Dropping(가득 차면 새 항목 폐기):** 가득 찬 상태에서 새 항목을 버립니다.

```scala
val droppingQueue: UIO[Queue[Int]] = Queue.dropping[Int](100)
```

**Sliding(가득 차면 오래된 항목 제거):** 가득 찬 상태에서 새 항목을 넣기 위해 가장 오래된 항목을 제거합니다.

```scala
val slidingQueue: UIO[Queue[Int]] = Queue.sliding[Int](100)
```

**Unbounded(무한):** 용량 제한이 없습니다.

```scala
val unboundedQueue: UIO[Queue[Int]] = Queue.unbounded[Int]
```

### 항목 추가(Adding Items)

**단일 항목:**

```scala
val res1: UIO[Unit] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(1)
} yield ()
```

**여러 항목:**

```scala
val res3: UIO[Unit] = for {
  queue <- Queue.bounded[Int](100)
  items = Range.inclusive(1, 10).toList
  _ <- queue.offerAll(items)
} yield ()
```

### 항목 소비(Consuming Items)

**Take(비어 있으면 블로킹):** 큐가 비어 있으면 항목이 들어올 때까지 대기합니다. 아래 예제는 `take`를 먼저 `fork`하여 비동기로 대기시키고, 이후 `offer`로 값을 넣은 뒤 `join`으로 결과를 회수합니다.

```scala
val oldestItem: UIO[String] = for {
  queue <- Queue.bounded[String](100)
  f <- queue.take.fork
  _ <- queue.offer("something")
  v <- f.join
} yield v
```

**Poll(논블로킹):** 즉시 반환하며, 값이 없으면 `None`을 반환합니다.

```scala
val polled: UIO[Option[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  head <- queue.poll
} yield head
```

**TakeUpTo(최대 N개 배치 회수):** 지정한 개수까지의 항목을 한 번에 가져옵니다.

```scala
val taken: UIO[Chunk[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  chunk <- queue.takeUpTo(5)
} yield chunk
```

**TakeAll(즉시 전체 비우기):** 큐에 있는 모든 항목을 즉시 가져옵니다.

```scala
val all: UIO[Chunk[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  chunk <- queue.takeAll
} yield chunk
```

### 종료 연산(Shutdown Operations)

**shutdown(대기 중인 파이버를 인터럽트):** 큐를 종료하고, `take`/`offer`로 일시 중단된 파이버를 인터럽트합니다.

```scala
val takeFromShutdownQueue: UIO[Unit] = for {
  queue <- Queue.bounded[Int](3)
  f <- queue.take.fork
  _ <- queue.shutdown
  _ <- f.join
} yield ()
```

**awaitShutdown(종료를 대기):** 큐가 종료될 때까지 기다립니다.

```scala
val awaitShutdown: UIO[Unit] = for {
  queue <- Queue.bounded[Int](3)
  p <- Promise.make[Nothing, Boolean]
  f <- queue.awaitShutdown.fork
  _ <- queue.shutdown
  _ <- f.join
} yield ()
```

### 핵심 개념

- **배압(Back-pressure):** 유한(bounded) 큐가 가득 차면, `offer` 연산은 공간이 생길 때까지 일시 중단됩니다. 이는 빠른 생산자(producer)가 느린 소비자(consumer)를 압도하지 못하도록 자연스럽게 속도를 조절합니다.
- **fork 패턴:** 현재 파이버를 블로킹하지 않고 비동기로 대기하기 위해 `fork`를 사용합니다.

---

## 4. Hub — 브로드캐스트 메커니즘

### 정의와 핵심 개념

`Hub`는 비동기 메시지 브로드캐스트(message broadcast) 시스템입니다. `Queue`는 발행된 각 값을 하나의 소비자에게만 전달하는 반면, `Hub`는 각 값을 모든 활성 구독자(all active subscribers)에게 전달합니다. 즉, 분배(distribution)가 아닌 브로드캐스트에 최적화되어 있습니다.

### 기본 연산자

핵심 Hub 인터페이스는 다음과 같습니다.

```scala
trait Hub[A] {
  def publish(a: A): UIO[Boolean]
  def subscribe: ZIO[Scope, Nothing, Dequeue[A]]
}
```

- **publish**: 모든 구독자에게 메시지를 전송하고, 발행 성공 여부를 나타내는 `Boolean`을 반환합니다.
- **subscribe**: 발행된 메시지를 수신하기 위한 `Dequeue`를 제공하는 스코프드(scoped) 이펙트를 반환합니다. 스코프(scope)가 닫히면 구독이 해제됩니다.

### 기본 예제

```scala
Hub.bounded[String](2).flatMap { hub =>
  ZIO.scoped {
    hub.subscribe.zip(hub.subscribe).flatMap { case (left, right) =>
      for {
        _ <- hub.publish("Hello from a hub!")
        _ <- left.take.flatMap(Console.printLine(_))
        _ <- right.take.flatMap(Console.printLine(_))
      } yield ()
    }
  }
}
```

### Hub 생성자(Constructors)

**Bounded Hub** — 용량에 도달하면 배압(backpressure)을 적용합니다. 구독 중인 모든 구독자가 모든 메시지를 받습니다. 효율을 위해 2의 거듭제곱(powers of two) 용량이 권장됩니다.

```scala
def bounded[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

**Dropping Hub** — 가득 차면 값을 버리고 `publish`가 `false`를 반환합니다. 발행자(publisher)는 블로킹되지 않고 계속 진행하지만, 구독자는 일부 값을 놓칠 수 있습니다.

```scala
def dropping[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

**Sliding Hub** — 용량이 가득 찬 상태에서 새 값이 도착하면 가장 오래된 값을 제거합니다. 발행은 항상 즉시 성공합니다. 느린 구독자가 발행자를 블로킹할 수 없습니다.

```scala
def sliding[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

**Unbounded Hub** — 결코 가득 차지 않습니다. 발행자 속도 저하 없이 모든 구독자가 모든 메시지를 받음을 보장하지만, 메모리가 무한히 증가(unbounded memory growth)할 위험이 있습니다.

```scala
def unbounded[A]: UIO[Hub[A]] = ???
```

### Hub 연산자(Operators)

```scala
trait Hub[A] {
  def publishAll(as: Iterable[A]): UIO[Boolean]
  def capacity: Int
  def size: UIO[Int]
  def awaitShutdown: UIO[Unit]
  def isShutdown: UIO[Boolean]
  def shutdown: UIO[Unit]
}
```

- **publishAll**: 여러 값을 원자적으로(atomically) 발행합니다.
- **capacity**: 고정된 허브 용량(불변).
- **size**: 현재 메시지 개수(이펙트로 조회).
- **종료 연산(shutdown / awaitShutdown / isShutdown)**: 허브의 수명 주기(lifecycle)를 관리합니다.

### Hub를 Enqueue로 사용하기

```scala
trait Hub[A] extends Enqueue[A]
```

`Hub`는 `Enqueue`가 필요한 곳 어디에서나 사용할 수 있습니다. 이를 통해 스트림 연산자(stream operator)로 허브에 쓰기를 할 수 있습니다.

```scala
type Transaction = ???
val transactionStream: ZStream[Any, Nothing, Transaction] = ???
val hub: Hub[Take[Nothing, Transaction]] = ???
transactionStream.into(hub)
```

이제 여러 다운스트림 소비자(downstream consumer)가 구독을 통해 모든 트랜잭션을 받게 됩니다.

### Hub와 스트림(Streams) 통합

**허브로부터 스트림 생성:**

```scala
object ZStream {
  def fromHub[O](hub: Hub[O]): ZStream[Any, Nothing, O] = ???
  def fromHubScoped[O](
    hub: Hub[O]
  ): ZIO[Scope, Nothing, ZStream[Any, Nothing, O]] = ???
}
```

- **fromHub**: 구독한 뒤 발행된 모든 값을 방출(emit)하고, 스트림이 끝나면 자동으로 구독을 해제합니다.
- **fromHubScoped**: 스코프드 변형으로, 발행자가 구독 완료를 기다려야 하는 경우에 유용합니다.

**Promise 협응을 사용하는 스코프드 예제:**

```scala
for {
  promise <- Promise.make[Nothing, Unit]
  hub     <- Hub.bounded[String](2)
  scoped  = ZStream.fromHubScoped(hub).tap(_ => promise.succeed(()))
  stream   = ZStream.unwrapScoped(scoped)
  fiber   <- stream.take(2).runCollect.fork
  _       <- promise.await
  _       <- hub.publish("Hello")
  _       <- hub.publish("World")
  _       <- fiber.join
} yield ()
```

**스트림을 허브로 발행:**

```scala
trait ZStream[-R, +E, +O] {
  def toHub[E1 >: E, O1 >: O](
    capacity: Int
  ): ZIO[R with Scope, Nothing, Hub[Take[E1, O1]]]
  
  def runIntoHub[E1 >: E, O1 >: O](
    hub: => Hub[Take[E1, O1]]
  ): ZIO[R, E1, Unit]
}
```

- **toHub**: 유한 허브를 생성하고 스트림 요소를 그 허브에 발행합니다.
- **runIntoHub**: 기존 허브로 값을 전송합니다(`Take` 값으로 감쌈).

**Take 기반 예제:**

```scala
for {
  promise <- Promise.make[Nothing, Unit]
  hub     <- Hub.bounded[Take[Nothing, String]](2)
  scoped  = ZStream.fromHubScoped(hub).tap(_ => promise.succeed(()))
  stream   = ZStream.unwrapScoped(scoped).flattenTake
  fiber   <- stream.take(2).runCollect.fork
  _       <- promise.await
  _       <- ZStream("Hello", "World").runIntoHub(hub)
  _       <- fiber.join
} yield ()
```

**싱크(Sink) 지원:**

```scala
object ZSink {
  def fromHub[I](
    hub: Hub[I]
  ): ZSink[Any, Nothing, I, Nothing, Unit] = ???
}
```

허브로 값을 발행하는 싱크를 생성합니다. `fromHubWithShutdown` 변형도 사용할 수 있습니다.

### 브로드캐스트 연산자(Broadcast Operators)

```scala
trait ZStream[-R, +E, +O] {
  def broadcast(
    n: Int,
    maximumLag: Int
  ): ZIO[R with Scope, Nothing, List[ZStream[Any, E, O]]]
  
  def broadcastDynamic(
    maximumLag: Int
  ): ZIO[R with Scope, Nothing, ZIO[Scope, Nothing, ZStream[Any, E, O]]]
}
```

- **broadcast**: N개의 새 스트림을 생성하며, 각각이 원본 스트림의 모든 값을 받습니다.
- **broadcastDynamic**: 고정된 스트림을 만들지 않고 동적으로 구독/구독 해제할 수 있게 합니다.

두 연산자 모두 최적의 성능을 위해 내부적으로 `Hub`를 사용합니다.

### 핵심 설계 주의사항

- 구독자는 자신이 **활성으로 구독 중일 때 발행된 메시지만** 받습니다.
- 타이밍이 중요할 때는 `Promise` 협응(coordination)이나 `fromHubScoped`를 사용하세요.
- `Take`로 감싸면 스트림의 완료(completion)나 에러(error)를 허브를 통해 표현할 수 있습니다.
- 효율을 위해 허브 용량은 2의 거듭제곱이어야 합니다.
- 실시간(real-time) 애플리케이션은 종종 엄격한 전달 보장(strict delivery guarantee)을 요구하지 않습니다.

---

## 5. Semaphore — 권한 기반 접근 제어

### 정의

`Semaphore`는 파이버 간 동기화를 지원하는 ZIO의 동시성 프리미티브입니다. 권한 기반(permit-based) 시스템으로 공유 자원에 대한 동시 접근(concurrent access)을 제어합니다.

### 핵심 개념

세마포어는 사용 가능한 권한(permit)의 개수를 유지합니다. 작업(task)은 진행하기 전에 권한을 획득(acquire)하고, 작업 후에 권한을 반환(release)합니다. 사용 가능한 권한이 남아 있지 않으면, 권한을 요청하는 작업은 권한이 다시 사용 가능해질 때까지 **의미론적으로 블로킹**(semantically blocked)됩니다.

### 핵심 연산

- **withPermit**: 작업 실행을 위해 단일 권한을 안전하게 획득하고 반환합니다. 성공·실패·인터럽트 여부와 관계없이 권한 반환을 보장합니다.
- **withPermits**: 단일 연산에서 여러 개의 권한을 획득·반환할 수 있는 카운팅(counting) 변형입니다.

### 문서 예제

다음 예제는 권한이 1개인 세마포어로, 3개의 동일한 작업을 병렬(`collectAllPar`)로 실행하더라도 한 번에 하나의 작업만 임계 구역(critical section)에 진입하도록 직렬화합니다.

```scala
import java.util.concurrent.TimeUnit
import zio._
import zio.Console._

val task = for {
  _ <- printLine("start")
  _ <- ZIO.sleep(Duration(2, TimeUnit.SECONDS))
  _ <- printLine("end")
} yield ()

val semTask = (sem: Semaphore) => for {
  _ <- sem.withPermit(task)
} yield ()

val semTaskSeq = (sem: Semaphore) => (1 to 3).map(_ => semTask(sem))

val program = for {
  sem <- Semaphore.make(permits = 1)
  seq <- ZIO.succeed(semTaskSeq(sem))
  _ <- ZIO.collectAllPar(seq)
} yield ()
```

**다중 권한(Multiple Permits) 예제:** 한 작업이 한 번에 5개의 권한을 점유합니다.

```scala
val semTaskN = (sem: Semaphore) => for {
  _ <- sem.withPermits(5)(task)
} yield ()
```

### 보장(Guarantee)

각 획득(acquisition)은 작업의 성공·실패·인터럽트 여부와 관계없이 반드시 동일한 횟수의 반환(release)으로 이어집니다. 즉, 권한 누수가 발생하지 않습니다.

---

## 6. 동기화 보조 도구 개요(Synchronization Primitives)

ZIO는 동시성 환경에서 공유 자원을 다룰 때 발생할 수 있는 데이터 불일치(data inconsistency)를 방지하기 위해 `zio-concurrent` 모듈을 통해 동기화 메커니즘을 제공합니다.

### 설치(Installation)

`build.sbt`에 다음 의존성을 추가합니다.

```scala
libraryDependencies += "dev.zio" %% "zio-concurrent" % "2.x.x"
```

### 제공되는 동기화 도구

- **ReentrantLock** — 동시성 시나리오에서 "코드 블록을 동기화하기" 위한 락 메커니즘.
- **CountDownLatch** — "하나 이상의 파이버가 여러 연산의 완료를 기다릴 수 있게" 하는 협응 도구.
- **CyclicBarrier** — "여러 파이버가 공통 배리어 지점(common barrier point)에 도달할 때까지 서로를 기다리게" 하는 배리어 메커니즘.

### 동시성 자료구조(Concurrent Data Structures)

- **ConcurrentMap** — `java.util.concurrent.ConcurrentHashMap`을 감싼 스레드 안전(thread-safe) 맵.
- **ConcurrentSet** — `java.util.concurrent.ConcurrentHashMap`을 감싼 스레드 안전 셋.

이 도구들은 동시 접근 패턴을 관리하면서 데이터 무결성(data integrity)을 유지하도록 돕습니다.

---

## 7. ReentrantLock — 재진입 가능 락

### 정의

`ReentrantLock`은 같은 파이버가 여러 번 획득할 수 있는 ZIO의 동기화 프리미티브입니다. 이미 락을 보유 중인 파이버는 교착 상태(deadlock) 없이 락을 다시 획득할 수 있으며, 내부의 보유 카운트(hold count)가 중첩 획득 횟수를 추적합니다.

### 핵심 연산

**기본 연산:**

- `lock: UIO[Unit]` — 락을 획득하며, 다른 파이버가 보유 중이면 블로킹합니다.
- `unlock: UIO[Unit]` — 락을 반환하며, 보유 카운트를 감소시킵니다.

**편의 메서드:**

- `tryLock: UIO[Boolean]` — 논블로킹 획득 시도.
- `withLock: URIO[Scope, Int]` — 자동 반환(automatic release)을 동반한 스코프드 획득.

**조회 메서드:**

- `holdCount` — 현재 획득 카운트.
- `owner` — 락을 보유 중인 파이버.
- `queuedFibers` — 대기 중인 파이버 모음.

### 생성(Creation)

```scala
object ReentrantLock {
  def make(fairness: Boolean = false): UIO[ReentrantLock] = ???
}
```

기본값은 불공정(unfair) 정책으로, 대기 중인 파이버를 무작위로 선택합니다. FIFO 순서가 필요하면 `fairness = true`로 설정합니다.

### 예제: 단순 락

```scala
import zio._
import zio.concurrent._

object MainApp extends ZIOAppDefault {
  def run =
    for {
      l  <- ReentrantLock.make()
      fn <- ZIO.fiberId.map(_.threadName)
      _  <- l.lock
      _  <- ZIO.debug(s"$fn acquired the lock.")
      task =
        for {
            fn <- ZIO.fiberId.map(_.threadName)
            _  <- ZIO.debug(s"$fn attempted to acquire the lock.")
            _  <- l.lock
            _  <- ZIO.debug(s"$fn acquired the lock.")
            _  <- ZIO.debug(s"$fn will release the lock after 5 second.")
            _  <- ZIO.sleep(5.second)
            _  <- l.unlock
            _  <- ZIO.debug(s"$fn released the lock.")
          } yield ()
      f <- task.fork
      _ <- ZIO.debug(s"$fn will release the lock after 10 second.")
      _ <- ZIO.sleep(10.second)
      _ <- (l.unlock *> ZIO.debug(s"$fn released the lock.")).uninterruptible
      _ <- f.join
    } yield ()
}
```

### 예제: 재진입(Reentrancy)

```scala
import zio._
import zio.concurrent._

object MainApp extends ZIOAppDefault {
  def task(l: ReentrantLock, i: Int): ZIO[Any, Nothing, Unit] = for {
    fn <- ZIO.fiberId.map(_.threadName)
    _  <- l.lock
    hc <- l.holdCount
    _  <- ZIO.debug(s"$fn (re)entered the critical section and now the hold count is $hc")
    _  <- ZIO.when(i > 0)(task(l, i - 1))
    _  <- l.unlock
    hc <- l.holdCount
    _  <- ZIO.debug(s"$fn exited the critical section and now the hold count is $hc")
  } yield ()

  def run =
    for {
      l <- ReentrantLock.make()
      _ <- task(l, 2) zipPar task(l, 3)
    } yield ()
}
```

### 예제: 교착 상태 시나리오(Deadlock Scenario)

두 파이버가 두 락을 서로 반대 순서로 획득하려 할 때 교착 상태가 발생할 수 있음을 보여 줍니다.

```scala
import zio._
import zio.concurrent._

object MainApp extends ZIOAppDefault {
  def workflow1(l1: ReentrantLock, l2: ReentrantLock) =
    for {
      f <- ZIO.fiberId.map(_.threadName)
      _ <- l1.lock *> ZIO.debug(s"$f locked the l1")
      o <- l2.owner.map(_.map(_.threadName))
      _ <- ZIO.debug(s"$f trying to lock the l2 while the $o is its owner") *>
        l2.lock *>
        ZIO.debug(s"$f locked the l2")
      _ <- l2.unlock
      _ <- l1.unlock
    } yield ()

  def workflow2(l1: ReentrantLock, l2: ReentrantLock) =
    for {
      f <- ZIO.fiberId.map(_.threadName)
      _ <- l2.lock *> ZIO.debug(s"$f locked the l2")
      o <- l1.owner.map(_.map(_.threadName))
      _ <- ZIO.debug(s"$f trying to lock the l1 while the $o is its owner") *>
        l1.lock *>
        ZIO.debug(s"$f locked the l1")
      _ <- l1.unlock
      _ <- l2.unlock
    } yield ()

  def run =
    for {
      l1 <- ReentrantLock.make()
      l2 <- ReentrantLock.make()
      _ <- workflow1(l1, l2) <&> workflow2(l1, l2)
    } yield ()
}
```

### 동작 관련 주의사항

파이버가 락을 보유한 상태에서 `lock`을 호출하면 보유 카운트가 증가하고 실행이 즉시 계속됩니다(재진입). `unlock` 시 카운트가 감소하며, 카운트가 0에 도달할 때에만 락이 실제로 해제됩니다. 공정성 정책(fairness policy)은 해제된 락을 다음에 어느 대기 파이버가 획득할지를 결정합니다.

---

## 8. CountDownLatch — 카운트다운 래치

### 정의

다른 파이버들에서 수행되는 일련의 연산이 완료될 때까지 하나 이상의 파이버가 기다릴 수 있게 하는 동기화 보조 도구(synchronization aid)입니다.

CountDownLatch는 **일회성(one-shot)** 동기화 프리미티브로, 카운트가 0에 도달하면 다시 리셋할 수 없습니다. 리셋 가능한 동작이 필요하면 `CyclicBarrier`를 사용하세요.

### 생성(Creation)

```scala
object CountdownLatch {
  def make(n: Int): IO[Option[Nothing], CountdownLatch]
}
```

### 연산(Operations)

```scala
class CountdownLatch {
  val countDown: UIO[Unit]
  val await: UIO[Unit]
}
```

- **`countDown`**: 래치 카운트를 감소시키고, 0에 도달하면 대기 중인 모든 파이버를 해제합니다.
- **`await`**: 래치가 0에 도달할 때까지 현재 파이버를 블로킹합니다.

### 코드 예제

**단순 On/Off 래치:** 생산자가 `50`을 생성하는 순간 `countDown`이 호출되어, `await` 중이던 소비자가 비로소 시작됩니다.

```scala
import zio._
import zio.concurrent._

object MainApp extends ZIOAppDefault {
  def consume(queue: Queue[Int]): UIO[Nothing] =
    queue.take
      .flatMap(i => ZIO.debug(s"consumed: $i"))
      .forever

  def produce(queue: Queue[Int], latch: CountdownLatch): UIO[Nothing] =
    (Random
      .nextIntBounded(100)
      .tap(i => queue.offer(i))
      .tap(i => ZIO.when(i == 50)(latch.countDown)) *> ZIO.sleep(500.millis)).forever

  def run =
    for {
      latch <- CountdownLatch.make(1)
      queue <- Queue.unbounded[Int]
      _     <- produce(queue, latch) <&> (latch.await *> consume(queue))
    } yield ()
}
```

**고급 래치(다중 생산자):** 카운트가 5이며, 10개의 생산자 파이버가 동작합니다. 누적된 `countDown` 호출로 카운트가 0이 되면 소비자가 시작됩니다.

```scala
import zio._
import zio.concurrent._

object MainApp extends ZIOAppDefault {
  def consume(queue: Queue[Int]): UIO[Nothing] =
    queue.take
      .flatMap(i => ZIO.debug(s"consumed: $i"))
      .forever

  def produce(queue: Queue[Int], latch: CountdownLatch): UIO[Nothing] =
    (Random
      .nextIntBounded(100)
      .tap(i => queue.offer(i))
      .tap(i => ZIO.when(i == 50)(latch.countDown)) *> ZIO.sleep(500.millis)).forever

  def run =
    for {
      latch <- CountdownLatch.make(5)
      queue <- Queue.unbounded[Int]
      p = ZIO.collectAllParDiscard(ZIO.replicate(10)(produce(queue, latch)))
      c = latch.await *> consume(queue)
      _     <-  p <&> c
    } yield ()
}
```

---

## 9. CyclicBarrier — 순환 배리어

### 정의

고정된 파이버 그룹이 공통 배리어 지점에서 서로를 기다릴 수 있게 하는 동기화 프리미티브입니다. 여러 파이버가 공통 배리어 지점에 도달할 때까지 서로를 기다리며, 배리어 해제 이후 **재사용**(reuse)이 가능합니다.

### 생성(Creation)

```scala
object CyclicBarrier {
  def make(parties: Int): UIO[CyclicBarrier] = ???
  def make(parties: Int, action: UIO[Any]): UIO[CyclicBarrier] = ???
}
```

두 가지 생성자가 있습니다. 하나는 참가자 수(party count)만 받고, 다른 하나는 배리어 해제 시 실행되는 선택적 이펙트(`action`)를 추가로 받습니다.

### 핵심 연산

| 메서드 | 타입 | 용도 |
|--------|------|------|
| `parties` | `Int` | 배리어를 트립(trip)시키는 데 필요한 참가자 수 |
| `waiting` | `UIO[Int]` | 현재 배리어에서 일시 중단된 참가자 수 |
| `await` | `IO[Unit, Int]` | 모든 참가자가 await를 호출할 때까지 일시 중단 |
| `reset` | `UIO[Unit]` | 초기 상태로 리셋하며, 대기 중인 파이버를 인터럽트 |
| `isBroken` | `UIO[Boolean]` | 배리어가 깨진(broken) 상태인지 확인 |

### 코드 예제(단순 경우)

3개의 파티가 모두 `await`에 도달해야 배리어가 트립되어 모두 동시에 진행됩니다.

```scala
import zio._
import zio.concurrent.CyclicBarrier

object MainApp extends ZIOAppDefault {
  def task(name: String) =
    for {
      b <- ZIO.service[CyclicBarrier]
      _ <- ZIO.debug(s"task-$name: started my job right now!")
      d <- Random.nextLongBetween(1000, 10000)
      _ <- ZIO.sleep(Duration.fromMillis(d))
      _ <- ZIO.debug(s"task-$name: finished my job and waiting for other parties to finish their jobs")
      _ <- b.await
      _ <- ZIO.debug(s"task-$name: the barrier is now broken, so I'm going to exit immediately!")
    } yield ()

  def run =
    for {
      b    <- CyclicBarrier.make(3)
      tasks = task("1") <&> task("2") <&> task("3")
      _    <- tasks.provide(ZLayer.succeed(b))
    } yield ()
}
```

### 순환 동작(Cyclic Behavior)

파티(parties) 수보다 많은 작업이 있는 경우, 배리어는 해제 이후 자동으로 리셋되어 다음 그룹이 동기화될 수 있습니다. 이 반복 특성 때문에 "순환적(cyclic)"이라는 이름이 붙었습니다.

### 배리어 깨짐(Barrier Breakage)

배리어는 다음 상황에서 깨집니다.

- 대기 중인 어떤 파이버가 인터럽트(interruption), 실패(failure), 또는 타임아웃(timeout)을 겪을 때.
- 배리어에 대해 수동 `reset`이 호출될 때.

깨짐(breaking)은 "전부 아니면 전무(all-or-none)" 모델을 통해 대기 중인 모든 파티에 전파됩니다.

---

## 10. ConcurrentMap — 동시성 맵

### 정의

`ConcurrentMap`은 `java.util.concurrent.ConcurrentHashMap`을 감싼 래퍼로, 여러 파이버에서 키-값 쌍(key-value pair)에 스레드 안전(thread-safe)하게 동시 접근할 수 있도록 합니다.

### 동기(Motivation)

표준 Scala `HashMap`은 스레드 안전하지 않습니다. 여러 파이버에서 동시에 수정(concurrent modification)하면 일관되지 않은 결과(inconsistent result)가 발생할 수 있으므로, 동시성 워크플로에서는 `ConcurrentMap`을 사용해야 합니다.

### 생성자(Constructors)

```scala
ConcurrentMap.empty[String, Int]
ConcurrentMap.make(("foo", 0), ("bar", 1), ("baz", 2))
ConcurrentMap.fromIterable(List(("foo", 0), ("bar", 1), ("baz", 2)))
```

### 핵심 연산

**조회(Retrieval):**

- `get(key: K): UIO[Option[V]]` — 키로 값을 조회합니다.
- `exists(p: (K, V) => Boolean): UIO[Boolean]` — 술어(predicate)를 검사합니다.
- `collectFirst[B](pf: PartialFunction[(K, V), B]): UIO[Option[B]]` — 첫 번째 일치 항목을 찾습니다.
- `fold[S](zero: S)(f: (S, (K, V)) => S): UIO[S]` — 엔트리들을 집계(aggregate)합니다.
- `forall(p: (K, V) => Boolean): UIO[Boolean]` — 모든 요소를 검증합니다.
- `isEmpty: UIO[Boolean]`, `toChunk`, `toList`

**삽입(Insertion):**

- `put(key: K, value: V): UIO[Option[V]]`
- `putIfAbsent(key: K, value: V): UIO[Option[V]]`
- `putAll(keyValues: (K, V)*): UIO[Unit]`

**제거(Removal):**

- `remove(key: K): UIO[Option[V]]`
- `remove(key: K, value: V): UIO[Boolean]`
- `removeIf(p: (K, V) => Boolean): UIO[Unit]`
- `clear: UIO[Unit]`

**재매핑(Remapping):**

- `compute(key: K, remap: (K, V) => V): UIO[Option[V]]`
- `computeIfAbsent(key: K, map: K => V): UIO[V]`
- `computeIfPresent(key: K, remap: (K, V) => V): UIO[Option[V]]`

**교체(Replacement):**

- `replace(key: K, value: V): UIO[Option[V]]`
- `replace(key: K, oldValue: V, newValue: V): UIO[Boolean]`

---

## 11. ConcurrentSet — 동시성 셋

### 정의

`ConcurrentSet`은 `java.util.concurrent.ConcurrentHashMap`을 기반으로 구현된 Set으로, ZIO에서 스레드 안전한 집합 연산(set operation)을 제공합니다.

### 생성자(Constructors)

| 메서드 | 용도 |
|--------|------|
| `empty[A]: UIO[ConcurrentSet[A]]` | 빈 셋을 생성 |
| `empty[A](initialCapacity: Int)` | 지정된 초기 용량으로 빈 셋 생성 |
| `fromIterable[A](as: Iterable[A])` | 컬렉션으로부터 초기화 |
| `make[A](as: A*)` | 제공된 요소들로 초기화 |

### 핵심 연산

**추가(Adding Elements):**

- `add(x: A): UIO[Boolean]` — 단일 값 삽입.
- `addAll(xs: Iterable[A])` — 여러 값 삽입.

**제거(Removing Elements):**

- `remove(x: A)` — 특정 값 삭제.
- `removeAll(xs: Iterable[A])` — 여러 값 제거.
- `removeIf(p: A => Boolean)` — 술어에 일치하는 요소 제거.
- `clear` — 셋 비우기.

**조회(Querying):**

- `contains(x: A)` — 멤버십 검사.
- `size` — 요소 개수.
- `isEmpty` — 비어 있는지 검사.
- `toSet` — 불변(immutable) Set으로 변환.

**함수형 연산(Functional Operations):**

- `exists(p: A => Boolean)` — 술어를 만족하는 요소가 있는지 검사.
- `forall(p: A => Boolean)` — 모든 요소가 만족하는지 검사.
- `fold[S](zero: S)(f: (S, A) => S)` — 값들을 누적(accumulate).
- `transform(f: A => A)` — 모든 요소를 변형(modify).
- `collectFirst[B](pf: PartialFunction)` — 첫 번째 일치 요소를 찾음.

**유지(Retention):**

- `retainAll(xs: Iterable[A])` — 지정된 요소들만 유지.
- `retainIf(p: A => Boolean)` — 술어를 만족하는 요소들만 유지.

---

## 12. 참고 자료

- [Introduction to ZIO's Concurrency Primitives](https://zio.dev/reference/concurrency/)
- [Promise](https://zio.dev/reference/concurrency/promise)
- [Queue](https://zio.dev/reference/concurrency/queue)
- [Hub](https://zio.dev/reference/concurrency/hub)
- [Semaphore](https://zio.dev/reference/concurrency/semaphore)
- [Introduction to ZIO's Synchronization Primitives](https://zio.dev/reference/sync/)
- [ReentrantLock](https://zio.dev/reference/sync/reentrantlock/)
- [CountDownLatch](https://zio.dev/reference/sync/countdownlatch/)
- [CyclicBarrier](https://zio.dev/reference/sync/cyclicbarrier/)
- [ConcurrentMap](https://zio.dev/reference/sync/concurrentmap/)
- [ConcurrentSet](https://zio.dev/reference/sync/concurrentset/)
