# ZIO 동시성 기초: 파이버(Fiber)

> 원본: https://zio.dev/reference/fiber/

---

## 목차

1. [파이버란 무엇인가(What is a Fiber)](#1-파이버란-무엇인가what-is-a-fiber)
2. [파이버가 스레드보다 나은 점(Advantages over Threads)](#2-파이버가-스레드보다-나은-점advantages-over-threads)
3. [Fiber 데이터 타입(The Fiber Data Type)](#3-fiber-데이터-타입the-fiber-data-type)
4. [포크와 조인(fork and join)](#4-포크와-조인fork-and-join)
5. [대기(await)](#5-대기await)
6. [파이버 합성(Composing Fibers)](#6-파이버-합성composing-fibers)
7. [인터럽션 모델(Interruption Model)](#7-인터럽션-모델interruption-model)
8. [인터럽트 가능성 제어(uninterruptible / interruptible)](#8-인터럽트-가능성-제어uninterruptible--interruptible)
9. [병렬성 연산(Parallelism: zipPar / foreachPar / collectAllPar)](#9-병렬성-연산parallelism-zippar--foreachpar--collectallpar)
10. [레이싱(Racing: race / raceAll)](#10-레이싱racing-race--raceall)
11. [타임아웃(Timeout)](#11-타임아웃timeout)
12. [구조적 동시성과 파이버 슈퍼비전(Structured Concurrency & Fiber Supervision)](#12-구조적-동시성과-파이버-슈퍼비전structured-concurrency--fiber-supervision)
13. [Fiber.Status](#13-fiberstatus)
14. [FiberId](#14-fiberid)
15. [참고 자료](#15-참고-자료)

---

## 1. 파이버란 무엇인가(What is a Fiber)

ZIO는 파이버(fiber)로 구동되는 고도로 동시적인(highly concurrent) 프레임워크입니다. 파이버는 "스레드에 비해 막대한 확장성(massive scalability)을 달성하는 경량 가상 스레드(lightweight virtual thread)"이며, 리소스 안전한 취소(resource-safe cancellation)가 보강되어 있습니다.

ZIO의 모든 이펙트(effect)는 어떤 파이버에 의해 실행됩니다. 즉 파이버는 ZIO에서 실행 단위(execution unit)입니다. 파이버 값(fiber value)은 "이미 실행을 시작한 `IO` 값을 모델링하며, 도덕적으로는 그린 스레드(green thread)와 동등한 것"입니다.

파이버는 서로 협력적으로 실행을 양보(cooperatively yield)하므로, JavaScript처럼 단일 스레드 환경(single-threaded environment)에서 실행되거나 JVM이 작업 스레드 1개로 설정된 경우에도 ZIO 파이버는 항상 동시적으로(concurrently) 실행됩니다.

현재 스레드를 블로킹하지 않고 이펙트를 수행하려면 파이버를 사용합니다. 파이버는 경량 동시성 메커니즘(lightweight concurrency mechanism)입니다.

### 수동 사용에 대한 경고

> "여러분은 파이버를 수동으로(manually) 사용하는 것을 피해야 합니다. ZIO는 `raceWith`, `zipPar`, `foreachPar` 같은 많은 동시성 기본 요소(concurrent primitives)를 제공하며, 이들은 별도의 수동 작업 없이도 내부적으로 파이버를 활용합니다."

일반적인 애플리케이션 개발에서는 `fork`/`join` 같은 저수준 파이버 API보다, 이를 추상화한 고수준 동시성 연산자(`race`, `zipPar`, `foreachPar` 등)를 사용하는 것이 권장됩니다.

---

## 2. 파이버가 스레드보다 나은 점(Advantages over Threads)

파이버는 `java.lang.Thread`와 비슷한 가상 스레드(virtual thread)처럼 동작하지만 성능이 우월합니다. 핵심적인 차이는, 여러 파이버가 하나의 JVM 스레드 위에서 실행된다는 점입니다. 즉 JVM 스레드가 운영체제(OS) 스레드에 1:1(one-to-one)로 매핑되는 것과 달리, 파이버는 스레드에 다대일(many-to-one)로 매핑됩니다. 이것이 훨씬 높은 확장성을 가능하게 합니다.

파이버의 주요 장점은 다음과 같습니다.

- **무한한 확장성(Unbounded Scalability)**: 파이버는 수십만~수백만 개의 동시 연산을 지원할 수 있으며, 이는 전통적인 스레드로는 불가능합니다. 현대 서버는 약 1,000개의 스레드를 효과적으로 다룰 수 있지만, 100,000개의 OS 스레드를 시도하면 시스템이 마비됩니다. 파이버 기반 애플리케이션은 100,000개 이상의 동시 연산을 높은 성능을 유지하며 관리할 수 있습니다.
- **경량 생성(Lightweight Construction)**: 비싼 스레드 생성과 달리, 파이버는 협력적 스레드(cooperative thread)로서 "그린 스레딩(green threading)"을 사용합니다. 선점형 스케줄링(preemptive scheduling)의 오버헤드 없이 서로 실행을 양보하므로, 생성과 컨텍스트 전환(context-switching) 비용이 극적으로 저렴합니다.
- **비동기적 특성(Asynchronous Nature)**: 스레드는 동기적(synchronous)이지만 파이버는 비동기적(asynchronous)으로 동작하며, 이는 곧 우월한 확장성으로 이어집니다.
- **타입 안전성과 합성 가능성(Type Safety and Composability)**: 파이버는 두 개의 타입 파라미터(`E`는 오류, `A`는 성공 값)를 가집니다. 이 타입 기반은 합성 가능하고(composable) 타입 안전한(type-safe) 동시성 프로그램을 가능하게 하며, 이는 타입이 없고 `void`를 반환하는 스레드에 비해 큰 개선입니다.
- **신뢰할 수 있는 인터럽션(Reliable Interruption)**: Java의 deprecated된 `Thread.stop()`과 달리, 파이버 인터럽션은 안전하고 거의 즉각적이며, 대상 스레드의 협조 없이도 일관되게 성공합니다.
- **구조적 동시성(Structured Concurrency)**: 자식 파이버(child fiber)는 부모 파이버(parent fiber)에 자동으로 스코프됩니다. 부모의 실행이 끝나면 모든 자식이 자동으로 종료되어, 파이버 누수(fiber leak)를 방지하고 파이버 수명에 대한 정적 추론(static reasoning)을 가능하게 합니다.

### 파이버의 작업 유형(Workload Types)

파이버의 작업은 세 가지 범주로 특징지을 수 있습니다.

1. **CPU 작업(CPU Work)**: 순수 계산 작업. `ZIO#blocking`을 통해 전용 스레드 풀(dedicated thread pool)을 요구합니다.
2. **블로킹 I/O(Blocking I/O)**: 소켓, 락 등 스레드를 멈추는(thread-parking) 연산. 블로킹 풀(blocking pool)을 통한 격리(isolation)가 필요합니다.
3. **비동기 I/O(Asynchronous I/O)**: 콜백 기반의 논블로킹(non-blocking) 연산. 주 스레드 풀(primary thread pool) 위에서 동작합니다.

> "ZIO는 100% 논블로킹(non-blocking)인 반면, Java 스레드는 그렇지 않습니다."

---

## 3. Fiber 데이터 타입(The Fiber Data Type)

`Fiber[E, A]` 타입은 두 개의 타입 파라미터를 가집니다.

- **E**: 실패 타입(failure type)
- **A**: 성공 타입(success type)

`Fiber[E, A]`는 `ZIO[R, E, A]` 이펙트를 포킹(forking)하여 생성되며, 동시적인 작업 단위(a concurrent unit of work)를 표현합니다.

> 참고: `R`(환경) 파라미터가 없는 이유는, 파이버는 모든 요구 사항이 이미 충족된(pre-satisfied) 이펙트만 실행하기 때문입니다.

파이버의 장점(스레드 대비)을 코드 차원에서 정리하면 다음과 같습니다.

- 최소한의 메모리 소비
- 스택의 동적 증가/축소
- OS 스레드 블로킹 방지
- 안전한 인터럽션 능력
- 강한 타입(strong typing)
- 일시 중단(suspend)되었을 때 자동 가비지 컬렉션

---

## 4. 포크와 조인(fork and join)

### 포킹(Forking)

포킹은 어떤 이펙트를 독립적으로 실행하는 새로운 파이버를 생성합니다.

```scala
def fib(n: Long): UIO[Long] = ZIO.suspendSucceed {
  if (n <= 1) ZIO.succeed(n)
  else fib(n - 1).zipWith(fib(n - 2))(_ + _)
}

val fib100Fiber: UIO[Fiber[Nothing, Long]] =
  for {
    fiber <- fib(100).fork
  } yield fiber
```

### 조인(Joining)

`Fiber#join`은 파이버의 결과와 동일한 결과를 갖는 이펙트를 반환합니다. 즉 "조인은 그 파이버가 값을 계산하기를 기다리는(waiting for) 방법"입니다.

```scala
for {
  fiber   <- ZIO.succeed("Hi!").fork
  message <- fiber.join
} yield message
```

포크와 조인, 인터럽트를 함께 사용하는 예시는 다음과 같습니다.

```scala
val analyzed = for {
  fiber1 <- analyzeData(data).fork
  fiber2 <- validateData(data).fork
  valid <- fiber2.join
  _ <- if (!valid) fiber1.interrupt else ZIO.unit
  analyzed <- fiber1.join
} yield analyzed
```

### 인터럽트(interrupt)

파이버를 더 이상 사용하지 않을 때는 그 파이버에 대해 `interrupt`를 호출하면 됩니다. 인터럽션은 파이버를 종료시키고 리소스를 해제합니다.

> "interrupt 연산은 파이버가 완료되거나(completed) 인터럽트되었으며 그 파이버의 모든 종료자(finalizers)가 실행될 때까지 재개되지 않습니다(does not resume)."

```scala
for {
  fiber <- ZIO.succeed("Hi!").forever.fork
  exit  <- fiber.interrupt
} yield exit
```

논블로킹 방식으로 인터럽트하려면 `Fiber#interruptFork`를 사용합니다. 파이버의 인터럽션을 백그라운드(background) 또는 별도의 데몬 파이버(daemon fiber)에서 수행합니다.

```scala
for {
  fiber <- ZIO.succeed("Hi!").forever.fork
  _     <- fiber.interrupt.fork
} yield ()
```

> 파이버가 작업을 마쳤거나 인터럽트되었을 때, 그 파이버의 종료자(finalizer)는 반드시 실행됨이 보장됩니다.

---

## 5. 대기(await)

`Fiber#await`는 완료에 관한 완전한 정보를 담은 `Exit` 값을 반환합니다. `join`과 달리, `await`는 로컬 상태(local state)를 병합하지 않으며 부모의 운명을 자식과 묶지(tie) 않습니다.

> "`await`는 파이버가 실패하거나 인터럽트되더라도 항상 `Exit` 정보와 함께 성공합니다(always succeeds with `Exit` information)."

```scala
for {
  fiber <- ZIO.succeed("Hi!").fork
  exit  <- fiber.await
} yield exit
```

`Exit` 결과를 패턴 매칭으로 분기 처리하는 예시는 다음과 같습니다.

```scala
for {
  b <- Random.nextBoolean
  fiber <- (if (b) ZIO.succeed(10) else ZIO.fail("The boolean was not true")).fork
  exitValue <- fiber.await
  _ <- exitValue match {
    case Exit.Success(value) => printLine(s"Fiber succeeded with $value")
    case Exit.Failure(cause) => printLine(s"Fiber failed")
  }
} yield ()
```

---

## 6. 파이버 합성(Composing Fibers)

### 파이버 zip(Zipping fibers)

두 파이버를 zip하면 두 파이버가 모두 성공한 경우 그 결과를 튜플(tuple)로 결합합니다.

```scala
for {
  fiber1 <- ZIO.succeed("Hi!").fork
  fiber2 <- ZIO.succeed("Bye!").fork
  fiber   = fiber1.zip(fiber2)
  tuple  <- fiber.join
} yield tuple
```

### orElse 합성(OrElse composition)

`orElse`는 첫 번째 파이버가 실패하면 두 번째 파이버의 결과로 대체합니다.

```scala
for {
  fiber1 <- ZIO.fail("Uh oh!").fork
  fiber2 <- ZIO.succeed("Hurray!").fork
  fiber   = fiber1.orElse(fiber2)
  message <- fiber.join
} yield message
```

---

## 7. 인터럽션 모델(Interruption Model)

ZIO는 **비동기 인터럽션**(asynchronous interruption)을 구현합니다. 한 파이버가 다른 파이버를, 대상 파이버가 인터럽션 상태를 폴링(poll)하지 않아도 종료시킬 수 있습니다. 이 방식은 완전히 함수형(fully functional)이며 ZIO의 패러다임과 호환되고, 명령형 언어에서 쓰이는 폴링 기반 해법과는 다릅니다.

### 인터럽션이 일어나는 경우

인터럽션은 다음과 같은 여러 상황에서 발생합니다.

1. **명시적 인터럽션(Explicit interruption)**: `Fiber#interrupt`를 통한 경우.
2. **병렬 합성(Parallel composition)**: 병렬 이펙트 중 하나가 실패하거나 인터럽트되면, 나머지도 종료됩니다.
3. **부모-자식 관계(Parent-child relationships)**: 자식 파이버는 부모에 스코프됩니다. 부모가 종료(exit)될 때 완료되지 않은 자식들은 인터럽트되며, 부모가 인터럽트되면 모든 자식이 인터럽트됩니다.
4. **타임아웃(Timeout) 연산** 및 사용자 주도의 취소(user-initiated cancellation).

### 기본 인터럽션 예시

```scala
import zio._

object MainApp extends ZIOAppDefault {
  def task = {
    for {
      fn <- ZIO.fiberId.map(_.threadName)
      _ <- ZIO.debug(s"$fn starts a long running task")
      _ <- ZIO.sleep(1.minute)
      _ <- ZIO.debug("done!")
    } yield ()
  }

  def run =
    for {
      f <-
        task.onInterrupt(
          ZIO.debug(s"Task interrupted while running")
        ).fork
      _ <- f.interrupt
    } yield ()
}
```

### 인터럽트 가능성 상속(Interruptibility Inheritance)

> "파이버(`forkDaemon`으로 생성된 것을 포함하여)는 부모의 인터럽트 가능성 상태(interruptibility status)를 상속합니다."

또한 인터럽션 모델은 오류를 잃지 않음을 보장합니다.

> "어떤 오류도 '손실(lost)'되는 상황은 존재하지 않습니다."

구조적 동시성은 인터럽션 도중에도 적절한 리소스 정리(resource cleanup)를 보장합니다.

### 블로킹 연산의 인터럽션(Blocking Operations)

- **`attemptBlockingInterrupt`**: ZIO 인터럽션을 `Thread.interrupt()`로 변환합니다.
- **`attemptBlockingCancelable`**: `InterruptedException`을 삼켜버리는(swallow) 연산을 위해, 커스텀 취소 이펙트(custom cancellation effect)를 받습니다.

```scala
val myApp =
  for {
    service <- ZIO.attempt(BlockingService())
    fiber   <- ZIO.attemptBlockingCancelable(
      effect = service.start()
    )(
      cancel = ZIO.succeed(service.close())
    ).fork
    _       <- fiber.interrupt.schedule(
      Schedule.delayed(
        Schedule.duration(3.seconds)
      )
    )
  } yield ()
```

### 모범 사례(Best Practices)

문서는 저수준에서 인터럽션을 수동으로 관리하기보다는, `onInterrupt`, `ensuring`, `ZIO.acquireRelease*` 같은 고수준 연산자와 동시성 연산(`race`, `foreachPar`)을 사용할 것을 권장합니다.

---

## 8. 인터럽트 가능성 제어(uninterruptible / interruptible)

ZIO는 임계 구역(critical section)을 보호하기 위해 `uninterruptible`과 `uninterruptibleMask` 연산을 제공합니다.

> "이 연산자들은 고급이고 매우 저수준(very low-level)이므로, 우리가 라이브러리 설계자(library designers)로서 무엇을 하고 있는지 정확히 알지 않는 한, 애플리케이션 개발에서 정기적으로 사용하지 않습니다."

`uninterruptibleMask` 변형은 인터럽트 불가능 영역(uninterruptible region) 안에서 선택적으로 인터럽트 가능성을 다시 켤 수 있도록 `restore` 함수를 제공합니다.

### 인터럽트 불가능 영역(Uninterruptible Regions)

```scala
for {
  fiber <- Clock.currentDateTime
    .flatMap(time => printLine(time))
    .schedule(Schedule.fixed(1.seconds))
    .uninterruptible
    .fork
  _ <- fiber.interrupt
} yield ()
```

### 빠른 인터럽션(interruptFork)

`Fiber#interruptFork`는 파이버의 인터럽션을 백그라운드 또는 별도의 데몬 파이버에서 수행합니다. 종료자(finalizer) 실행이 오래 걸려 인터럽트를 호출한 쪽이 블로킹되는 것을 피하고 싶을 때 유용합니다.

```scala
for {
  fiber <- printLine("Working on the first job")
    .schedule(Schedule.fixed(1.seconds))
    .ensuring {
      (printLine("Finalizing or releasing a resource that is time-consuming") 
        *> ZIO.sleep(7.seconds)).orDie
    }
    .fork
  _ <- fiber.interruptFork.delay(4.seconds)
  _ <- printLine("Starting another task while interruption happens in background")
} yield ()
```

---

## 9. 병렬성 연산(Parallelism: zipPar / foreachPar / collectAllPar)

ZIO는 순차(sequential) 연산에 대응하는 병렬(parallel) 변형을 `Par` 접미사로 제공합니다.

| 연산 | 순차(Sequential) | 병렬(Parallel) |
|------|------------------|----------------|
| 이펙트 zip | `ZIO#zip` | `ZIO#zipPar` |
| 함수로 zip | `ZIO#zipWith` | `ZIO#zipWithPar` |
| 여러 개 튜플화 | `ZIO#tupled` | `ZIO#tupledPar` |
| 전부 수집 | `ZIO.collectAll` | `ZIO.collectAllPar` |
| 이펙트로 반복 | `ZIO.foreach` | `ZIO.foreachPar` |
| 여러 개 reduce | `ZIO.reduceAll` | `ZIO.reduceAllPar` |
| 여러 개 merge | `ZIO.mergeAll` | `ZIO.mergeAllPar` |

병렬 이펙트 중 하나라도 실패하면 나머지가 자동으로 취소(auto-cancellation)됩니다. 실패하더라도 자동 취소를 유발하지 않게 하려면, `ZIO#either`나 `ZIO#option`으로 변환하여 사용합니다.

`zipPar` 사용 예시는 다음과 같습니다.

```scala
def bigCompute(m1: Matrix, m2: Matrix, v: Matrix): UIO[Matrix] =
  for {
    t <- computeInverse(m1).zipPar(computeInverse(m2))
    (i1, i2) = t
    r <- applyMatrices(i1, i2, v)
  } yield r
```

---

## 10. 레이싱(Racing: race / raceAll)

여러 이펙트를 레이스(race)하면 그것들이 병렬로 실행되며, 가장 먼저 성공적으로 완료된 액션의 값이 반환됩니다.

> "두 액션은 _레이스(raced)_ 될 수 있으며, 이는 그것들이 병렬로 실행되고 가장 먼저 성공적으로 완료되는 액션의 값이 반환됨을 의미합니다."

```scala
fib(100) race fib(200)
```

다른 예시는 다음과 같습니다.

```scala
for {
  winner <- ZIO.succeed("Hello").race(ZIO.succeed("Goodbye"))
} yield winner
```

성공 여부와 무관하게 가장 먼저 완료된 결과를 얻으려면, 양쪽을 `either`로 감싸서 레이스합니다: `left.either.race(right.either)`.

---

## 11. 타임아웃(Timeout)

리소스 안전한 타임아웃(resource-safe timeout)은 이펙트의 크기와 무관하게 동작합니다.

```scala
ZIO.succeed("Hello").timeout(10.seconds)
```

이는 `Option[A]`를 반환하며, `None`은 타임아웃을 의미합니다. 타임아웃된 이펙트는 자동으로 인터럽트됩니다.

---

## 12. 구조적 동시성과 파이버 슈퍼비전(Structured Concurrency & Fiber Supervision)

ZIO는 파이버의 수명(lifetime)이 깔끔하게 중첩되는(cleanly nested) 구조적 동시성 모델(structured concurrency model)을 사용합니다. 파이버의 수명은 부모 파이버의 수명에 종속되며, 자식 파이버는 부모 파이버에 스코프됩니다. 부모 이펙트의 실행이 끝나면 모든 자식 이펙트가 자동으로 인터럽트됩니다.

### 자식 파이버 수명 전략(Lifetime Strategies)

자식 파이버의 생명 주기는 네 가지 방식으로 관리됩니다.

1. **자동 슈퍼비전 (기본값, Automatic Supervision)**: `ZIO#fork`로 생성됩니다. 자식 파이버는 부모가 완료되거나 인터럽트될 때 종료됩니다.
2. **전역 스코프 / 데몬(Global Scope / Daemon)**: `ZIO#forkDaemon`을 사용합니다. 파이버는 독립적으로 실행되며, 애플리케이션이 종료되거나 자연스럽게 완료될 때까지 지속됩니다.
3. **로컬 스코프(Local Scope)**: `ZIO#forkScoped`를 사용합니다. 파이버는 특정 스코프(scope)에 부착되어 부모보다 오래 살아남지만, 그 스코프가 닫힐 때 종료됩니다.
4. **특정 스코프(Specific Scope)**: `ZIO#forkIn`을 사용합니다. 개발자가 정확한 스코프 경계를 지정하여 세밀한 수명 제어(fine-grained lifetime control)를 합니다.

이러한 구조적 동시성은 파이버 누수(fiber leak)를 방지하고, 인터럽션 도중에도 적절한 리소스 정리를 보장합니다.

---

## 13. Fiber.Status

`Fiber.Status`는 파이버의 현재 상태(current status)를 기술합니다. 이를 통해 실행 중인 파이버가 어떤 상태에 있는지 추적할 수 있습니다.

상태는 다음 세 가지로 구분됩니다.

1. **Done**: 파이버가 실행을 완료한 상태.
2. **Running**: 파이버가 능동적으로 실행 중인 상태.
3. **Suspended**: 파이버가 어떤 리소스나 연산을 기다리며 일시적으로 멈춘 상태.

파이버의 상태를 조회하는 예시는 다음과 같습니다. 아래 코드는 무한 파이버에 대해 `await`하고, `Suspended` 상태를 패턴 매칭하여 해당 파이버가 블로킹하며 기다리는 대상의 식별자(`blockingOn`)를 추출합니다.

```scala
import zio._
for {
  f1 <- ZIO.never.fork
  f2 <- f1.await.fork
  blockingOn <- f2.status
    .collect(()) { case Fiber.Status.Suspended(_, _, blockingOn) =>
      blockingOn
    }
    .eventually
} yield (assert(blockingOn == f1.id))
```

---

## 14. FiberId

`FiberId`는 파이버의 고유한 정체성(unique identity)을 기술합니다.

> "FiberId는 한 Fiber의 정체성으로서, 전역적으로 고유한 시퀀스 번호(globally unique sequence number)와 그 파이버가 생을 시작한 시각(time when it began life)으로 기술됩니다."

`FiberId`는 두 가지 속성으로 구성됩니다.

1. **id**: 단조 증가하는(monotonically increasing) 고유 시퀀스 번호(0, 1, 2, ...). 원자적 카운터(atomic counter)에서 유도됩니다.
2. **startTimeSeconds**: UTC 기준 초(seconds) 단위 시각. `java.lang.System.currentTimeMillis / 1000`으로 계산됩니다.

---

## 15. 참고 자료

- [Basic Concurrency | ZIO](https://zio.dev/overview/basic-concurrency/)
- [Introduction to ZIO Fibers | ZIO](https://zio.dev/reference/fiber/)
- [Fiber | ZIO](https://zio.dev/reference/fiber/fiber.md/)
- [Fiber.Status | ZIO](https://zio.dev/reference/fiber/fiberstatus/)
- [FiberId | ZIO](https://zio.dev/reference/fiber/fiberid/)
- [ZIO Interruption Model | ZIO](https://zio.dev/reference/interruption/)
- [Introduction to Concurrent Programming in ZIO | ZIO](https://zio.dev/reference/concurrency/)
