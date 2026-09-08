# ZIO 동시성: 파이버와 프리미티브

## ZIO Fiber

> 기준: ZIO 2.x 공식 Fiber 문서

>

> 원본: https://zio.dev/reference/fiber/

<a id="1-fiber-개요"></a>
### 1. Fiber 개요

Fiber는 ZIO 런타임이 관리하는 경량 가상 스레드로, 이미 실행을 시작한 이펙트를 나타낸다. JDK의 `java.lang.VirtualThread`와는 다른 ZIO의 실행 단위이며, 개념적으로 그린 스레드에 해당한다. ZIO의 모든 코드는 Fiber 위에서 실행된다. 명시적으로 `fork`하지 않은 애플리케이션 진입점도 최상위 main Fiber에서 실행된다.

하나의 JVM 스레드는 여러 Fiber를 번갈아 실행한다. 런타임이 관찰할 수 있는 이펙트 경계에서 다른 Fiber에 실행을 양보하는 협력적 멀티태스킹을 사용하므로, Fiber마다 OS 스레드를 만들지 않고도 높은 동시성을 제공한다.

#### 저수준 도구라는 점

직접 `fork`하고 수명을 관리하면 실수나 성능 문제가 생기기 쉬우므로, 일반적인 병렬 처리에는 `zipPar`, `foreachPar`, `race` 같은 고수준 연산자를 우선 사용한다. 다음과 같이 작업을 세밀하게 제어해야 할 때 Fiber를 직접 다룬다.

- 직접 Fiber를 다뤄야 하는 경우
  - 작업의 시작과 완료 시점을 별도로 제어해야 함
  - 실행 중인 작업을 명시적으로 취소해야 함
  - Fiber의 상태, ID, 자식 Fiber를 관찰해야 함
  - 특정 `Scope`에 작업 수명을 결합해야 함

<a id="2-jvm-스레드와의-차이"></a>

### 2. JVM 스레드와의 차이

#### 개수와 생성 비용

- JVM 플랫폼 스레드와 OS 스레드의 관계는 일반적으로 1:1임
- Fiber와 JVM 플랫폼 스레드의 관계는 다대일임
- Fiber 생성과 전환은 OS 스레드 생성과 컨텍스트 전환보다 가벼움
- 수십만 개 이상의 동시 작업도 제한된 JVM 스레드 풀에서 처리 가능
- 많은 Fiber를 만들 수 있다는 의미이지, 무제한 병렬 실행이 가능하다는 의미는 아님
  - 동시성: 여러 작업의 진행 구간이 겹치는 성질
  - 병렬성: 여러 CPU 코어에서 실제로 동시에 실행되는 성질

#### 타입과 합성

Java `Thread`의 실행은 `run(): Unit`에 가까워 성공 값과 오류 타입이 타입에 나타나지 않는다. `Fiber[E, A]`는 실패 타입 `E`와 성공 타입 `A`를 보존하므로 `join`, `await`, `zip`으로 실행 결과를 타입 안전하게 합성할 수 있다.

#### 인터럽션과 구조적 동시성

Java 스레드에서는 대상 코드가 인터럽션 요청을 무시할 수 있지만, ZIO Fiber의 인터럽션은 런타임이 리소스 정리와 함께 관리한다. 기본 `fork`로 만든 자식은 부모의 수명에 연결되며, 부모가 끝나면 아직 실행 중인 자식도 인터럽트된다. 자식의 수명을 따로 추적하지 못해 작업이 남는 것을 막는 구조다.

<a id="3-fiber-관련-데이터-타입"></a>

### 3. Fiber 관련 데이터 타입

#### `Fiber[E, A]`

- 실행 중이거나 이미 완료된 동시 작업의 핸들임
- 타입 매개변수
  - `E`: 실패 채널의 타입
  - `A`: 성공 값의 타입
- 환경 타입 `R`이 없는 이유
  - Fiber가 시작될 때 실행에 필요한 환경이 이미 제공되기 때문임
- `ZIO[R, E, A]`를 `fork`하면 즉시 `URIO[R, Fiber[E, A]]`를 얻음

```scala
val fiber: URIO[Any, Fiber[String, Int]] =
  ZIO.succeed(42).fork
```

#### 함께 사용되는 타입

- `Exit[E, A]`
  - Fiber의 성공, 실패, 결함, 인터럽션을 포함한 종료 결과임
- `Fiber.Status`
  - Fiber의 현재 실행 상태를 나타냄
- `FiberId`
  - Fiber의 고유 식별 정보를 나타냄

<a id="4-자식-fiber의-수명"></a>

### 4. 자식 Fiber의 수명

- Fiber 수명은 `fork` 방식과 `Scope`로 결정됨

#### 자동 슈퍼비전: `fork`

- 기본 전략임
- 자식 Fiber가 부모 Fiber의 수명에 결합됨
- 자식이 자연스럽게 완료되거나 부모가 종료될 때 종료됨
- 부모가 성공, 실패, 인터럽션 중 어떤 방식으로 끝나도 남은 자식이 인터럽트됨

```scala
val program =
  for {
    _ <- ZIO.never.fork
    _ <- ZIO.logInfo("parent finished")
  } yield ()
```

부모가 끝나면 `ZIO.never`를 실행하던 자식도 정리된다. 다만 `fork` 직후 부모가 너무 빨리 끝나면 자식의 본문이나 `onInterrupt`가 시작되기 전에 종료될 수 있다. 테스트나 초기화 코드에서 자식이 시작했는지 반드시 확인해야 한다면 `Promise` 같은 동기화 수단을 사용한다.

#### 전역 수명: `forkDaemon`

- 자식 Fiber를 전역 `Scope`에 결합함
- 부모가 끝나거나 인터럽트되어도 계속 실행됨
- 자연스럽게 완료되거나 애플리케이션의 전역 `Scope`가 닫힐 때 종료됨
- 프로세스 전체와 수명을 같이하는 백그라운드 작업에 적합함
- 제한 없이 사용하면 작업 누수와 테스트 간 간섭을 만들 수 있음

#### 로컬 수명: `forkScoped`

`forkScoped`는 Fiber의 수명을 현재 로컬 `Scope`에 연결한다. 따라서 직접적인 부모가 끝나도 실행될 수 있지만, 해당 `Scope`가 닫히면 자동으로 인터럽트된다. 서버 리스너, 구독, 폴링처럼 특정 리소스 범위와 함께 정리할 작업에 사용한다.

```scala
val scopedWorker: ZIO[Scope, Nothing, Fiber[Nothing, Nothing]] =
  ZIO.never.forkScoped
```

#### 지정한 수명: `forkIn`

- 인수로 받은 특정 `Scope`에 Fiber를 결합함
- 중첩된 코드 블록보다 바깥 `Scope`까지 작업을 유지하는 등 세밀한 수명 제어에 사용함
- 대상 `Scope`가 닫히면 Fiber도 인터럽트됨

```scala
val program = ZIO.scoped {
  for {
    outerScope <- ZIO.scope
    _ <- ZIO.scoped {
      ZIO.never.forkIn(outerScope)
    }
  } yield ()
}
```

#### 수명 전략 비교

- 부모와 함께 종료돼야 함 → `fork`
- 애플리케이션 전체와 함께 종료돼야 함 → `forkDaemon`
- 현재 리소스 범위와 함께 종료돼야 함 → `forkScoped`
- 이미 확보한 특정 범위와 함께 종료돼야 함 → `forkIn(scope)`

<a id="5-백그라운드-프로세스와-layer"></a>

### 5. 백그라운드 프로세스와 Layer

Kafka 소비 루프처럼 Layer가 백그라운드 작업을 시작할 때는 작업을 언제 끝낼지 정해야 한다. 내부에서 `forkDaemon`을 쓰면 전역 `Scope`에 연결되므로, Layer를 지역적으로 `provide`한 코드가 끝나도 작업은 계속 실행된다. Layer 인스턴스와 작업의 수명이 달라지는 것이다.

애플리케이션 전체에서 하나만 유지할 작업에는 이런 전역 수명이 맞을 수 있다. Layer가 해제될 때 함께 정리해야 한다면 다음처럼 `ZLayer.scoped`와 `forkScoped`를 조합한다.

```scala
val workerLayer: ZLayer[Any, Nothing, Unit] =
  ZLayer.scoped {
    ZIO.logInfo("worker tick")
      .repeat(Schedule.fixed(1.second))
      .forkScoped
      .unit
  }
```

- 핵심 판단 기준은 작업을 시작한 위치가 아니라 작업이 종료되어야 하는 `Scope`임

<a id="6-fiber-기본-연산"></a>

### 6. Fiber 기본 연산

#### 시작과 결과 결합: `fork`, `join`

- `fork`는 이펙트를 새 Fiber에서 시작하고 즉시 Fiber 핸들을 반환함
- `join`은 대상 Fiber가 끝날 때까지 현재 Fiber를 논리적으로 대기시킴
- 대상이 성공하면 성공 값을 반환함
- 대상이 실패하면 같은 오류로 실패함
- 대상이 인터럽트되면 조인하는 Fiber도 인터럽트됨
- 대상 Fiber의 `FiberRef` 값은 각 `FiberRef`에 정의된 결합 규칙에 따라 현재 Fiber로 병합됨
  - `join`이 완료를 기다린 뒤 내부적으로 `inheritAll`을 수행하는 의미론임

```scala
val joined: IO[String, Int] =
  for {
    fiber <- ZIO.succeed(21).map(_ * 2).fork
    value <- fiber.join
  } yield value
```

#### 종료 결과 관찰: `await`

`await`는 대상 Fiber의 종료를 기다린 뒤 `Exit[E, A]`를 반환한다. 대상이 실패하거나 인터럽트되어도 그 결과를 값으로 받으므로 성공과 모든 실패 원인을 검사할 수 있다. 이때 대상의 `FiberRef` 값은 현재 Fiber로 병합하지 않는다.

```scala
val observed: UIO[Exit[String, Int]] =
  for {
    fiber <- ZIO.fail("invalid").fork
    exit  <- fiber.await
  } yield exit
```

#### 대기하지 않는 관찰: `poll`

- `poll`은 Fiber 완료 여부를 즉시 확인함
- 아직 실행 중이면 `None`, 완료되었으면 `Some(exit)` 반환
- 완료를 기다리지 않는 상태 조회나 진단에 사용함

#### Fiber 로컬 상태 상속: `inheritAll`

- 대상 Fiber가 가진 모든 `FiberRef` 값을 현재 Fiber로 병합함
- 대상이 끝날 때까지 기다리지 않고 호출 시점의 값을 병합한 뒤 즉시 계속함
- 최종 결과까지 기다린 뒤 병합하려면 직접 호출하는 대신 `join` 사용

#### 인터럽트: `interrupt`

`interrupt`는 대상 Fiber에 인터럽션을 요청한 뒤 대상의 종료와 모든 finalizer 실행을 기다린다. 리소스 해제가 끝난 다음 호출자가 다음 단계로 진행하도록 하기 위해서다. 반환된 `Exit`로는 최종 종료 원인을 확인할 수 있다.

```scala
val stopped: UIO[Exit[Nothing, Nothing]] =
  for {
    fiber <- ZIO.never.fork
    exit  <- fiber.interrupt
  } yield exit
```

#### Fiber 합성

- `fiber1.zip(fiber2)`
  - 두 Fiber가 모두 성공하면 결과를 튜플로 결합함
- `fiber1.orElse(fiber2)`
  - 첫 Fiber가 실패하면 두 번째 Fiber의 결과를 사용함
- 이 합성은 이미 시작된 Fiber 핸들을 결합함
- 새 이펙트를 병렬 실행하려는 일반적인 코드에는 `ZIO#zipPar` 같은 고수준 연산자가 더 적합함

<a id="7-병렬-실행과-레이스"></a>

### 7. 병렬 실행과 레이스

#### 병렬 실행

- 순차 연산의 병렬 변형은 주로 `Par` 접미사를 사용함
  - `zip` → `zipPar`
  - `zipWith` → `zipWithPar`
  - `ZIO.foreach` → `ZIO.foreachPar`
  - `ZIO.collectAll` → `ZIO.collectAllPar`

`zipPar`는 한쪽이 실패하면 아직 실행 중인 다른 쪽을 인터럽트해 불필요한 작업을 남기지 않는다. 한 작업의 실패 후에도 다른 작업을 계속 실행해야 한다면, `either`나 `option`으로 실패를 성공 채널의 값으로 바꾸는 방법을 고려한다.

```scala
val both: IO[String, (Int, Int)] =
  fetchLeft.zipPar(fetchRight)
```

#### 레이스

`left.race(right)`는 두 이펙트를 병렬로 시작하고 먼저 성공한 값을 반환하며, 아직 실행 중인 패자를 인터럽트한다. 한쪽이 먼저 실패해도 다른 쪽이 성공할 수 있다면 계속 기다린다.

성공 여부와 관계없이 먼저 끝난 결과가 필요하면 양쪽을 `either`로 바꿔 레이스할 수 있다. 첫 완료 시점의 처리를 직접 정하려면 `raceWith`를 사용한다. `race`와 `zipPar`는 이렇게 내부에서 Fiber의 생성과 수명, 취소를 관리하는 고수준 연산이다.

#### 타임아웃

- `effect.timeout(duration)`은 제한 시간 안에 완료되면 `Some(value)` 반환
- 제한 시간을 넘기면 실행 중인 Fiber를 인터럽트하고 `None` 반환
- 타임아웃도 Fiber 인터럽션에 기반한 리소스 안전한 연산임

<a id="8-오류-모델"></a>

### 8. 오류 모델

- ZIO는 타입이 있는 실패와 Fiber 종료 원인을 구분함

#### 타입이 있는 실패

- `ZIO[R, E, A]`가 예상 가능한 실패 채널 `E`를 가짐
- `either`를 사용하면 실패를 `Either[E, A]` 형태의 성공 값으로 노출 가능
- 호출자가 처리할 수 있는 도메인 오류에 적합함

```scala
val handled: UIO[Either[Throwable, String]] =
  ZIO.fail(new RuntimeException("failed")).either
```

#### Fiber가 정상 성공 이외의 방식으로 끝나는 경우

- 자신 또는 다른 Fiber에 의해 인터럽트됨
- 처리되지 않은 타입 오류 `E`로 실패함
- 복구 불가능한 결함(defect)이 발생함
  - `map`, `flatMap` 등에 전달한 함수가 예외를 던지는 경우
  - 예외를 던질 수 있는 코드를 `ZIO.succeed`로 잘못 감싼 경우
- 예외 가능성이 있는 동기 코드는 `ZIO.attempt`로 가져와 실패 채널에 표현해야 함

#### 종료와 진단

- 종료 원인은 Fiber의 supervisor가 관찰 가능함
- supervisor는 로깅, 스택 트레이스 출력, 별도 복구 정책 같은 진단 작업 수행 가능
- 인터럽션 중에도 등록된 모든 finalizer 실행이 보장됨
- finalizer 자체에서 결함이 발생해도 해당 원인이 버려지지 않고 전체 실패 원인에 보존됨
- 성공, 실패, 결함, 인터럽션을 모두 검사해야 하면 `await`가 반환하는 `Exit` 사용

<a id="9-인터럽션"></a>

### 9. 인터럽션

#### 기본 성질

- Fiber는 기본적으로 인터럽트 가능함
- 인터럽션은 실행을 즉시 강제 종료하는 대신 안전한 중단 지점에서 반영됨
- 인터럽션을 영구적으로 거부할 수는 없고, 인터럽트 불가능 영역으로 반영을 지연할 수만 있음
- Fiber가 완료되거나 인터럽트되면 등록된 finalizer가 반드시 실행됨

#### 인터럽트 가능 영역과 불가능 영역

- `interruptible`
  - 이펙트를 인터럽트 가능 상태로 실행함
- `uninterruptible`
  - 이펙트를 인터럽트 불가능 상태로 실행함
  - 락 획득 후 상태 갱신처럼 중간에 끊기면 불변식이 깨지는 짧은 임계 구역에 사용함
- `uninterruptibleMask`
  - 전체 영역은 인터럽트 불가능하게 유지하면서 `restore`로 일부 대기 구간만 원래 상태로 되돌림
  - 리소스 획득, 해제 연산자 같은 저수준 추상화 구현에 사용함

#### 자식 Fiber의 인터럽트 가능성 상속

`fork`와 `forkDaemon`으로 만든 Fiber는 생성 시점의 부모 인터럽트 가능성을 상속한다. 따라서 인터럽트 불가능 영역에서 `ZIO.never.fork`를 호출하면 자식도 인터럽트 불가능해지고, 이 자식에 `interrupt`를 호출한 쪽까지 계속 기다릴 수 있다. 부모는 보호하되 자식은 취소할 수 있어야 한다면 `uninterruptibleMask`의 `restore`로 자식 이펙트의 상태를 복원한다.

```scala
val parent = ZIO.uninterruptibleMask { restore =>
  for {
    child <- restore(ZIO.never).fork
    _     <- child.interrupt
  } yield ()
}
```

#### 빠른 인터럽션

일반 `interrupt`는 대상의 finalizer가 끝날 때까지 기다린다. 정리가 끝나기 전에 다음 작업으로 넘어가야 한다면 다음 연산을 고려할 수 있다.

- `Fiber#interruptFork`: 별도 데몬 Fiber에서 인터럽션 수행
- `ZIO#disconnect`: 이펙트 실행과 인터럽션 대기를 분리

이 경우에도 finalizer는 백그라운드에서 계속 실행된다. 정리 자체를 생략하는 것이 아니라 호출자가 정리 완료를 기다리지 않는 방식이다.

#### 블로킹 연산 인터럽트

- `ZIO.attemptBlocking`은 ZIO 수준에서 인터럽트 가능하지만 JVM `Thread.interrupt()`로 자동 변환되지는 않음
- 블로킹 라이브러리가 스레드 인터럽션에 반응한다면 `ZIO.attemptBlockingInterrupt` 사용
- 스레드 인터럽션을 무시하는 API에는 명시적인 취소 동작을 받는 `ZIO.attemptBlockingCancelable` 사용 고려 가능

#### 자동 인터럽션이 발생하는 경우

- 구조적 동시성
  - 부모 Fiber 종료 → 기본 `fork`로 만든 자식 Fiber 인터럽트
- 병렬 실행
  - `zipPar`, `foreachPar` 등의 한 작업 실패 → 나머지 작업 인터럽트
- 타임아웃
  - 제한 시간 초과 → 실행 중 작업 인터럽트
- 레이스
  - 승자 결정 → 아직 실행 중인 패자 인터럽트

#### 인터럽트된 Fiber 조인

- 인터럽트된 Fiber를 `join`하면 조인하는 Fiber도 인터럽트됨
- 이때 발생하는 내부 인터럽션은 외부 Fiber가 강제로 전달한 인터럽션과 달리 복구 가능함
- 조인하는 Fiber에 등록된 finalizer는 이 경우에도 정상적으로 실행됨
- 인터럽션 전파 중에도 리소스 해제 순서를 보존하는 의미론임
- 대상의 인터럽션을 값으로 관찰하고 싶다면 `join` 대신 `await` 사용

<a id="10-jvm-스레드-전환"></a>

### 10. JVM 스레드 전환

- Fiber는 특정 JVM 스레드에 고정되지 않음
- 오래 실행되는 Fiber는 협력적 스케줄링을 위해 런타임 스레드 사이를 이동할 수 있음
- 비동기 콜백으로 재개된 Fiber는 콜백을 실행한 스레드에서 잠시 진행할 수 있음
- 일정 시간이 지나면 실행을 양보하고 ZIO 런타임 스레드 풀에서 다시 실행됨
- 같은 스레드에서 최소 시간 동안 실행하려는 최적화와 주기적인 양보를 함께 사용함
- 이러한 기본값이 제공하는 성질
  - 스택 안전성
  - 협력적 멀티태스킹
  - 제한된 스레드 풀의 공정한 공유
- Fiber 실행 코드에서 `ThreadLocal`이나 특정 스레드 이름에 의존하면 안 됨
- 자동 스레드 전환 정책은 런타임 설정으로 조정 가능하지만, 일반 애플리케이션에서는 기본값 사용이 권장됨

<a id="11-작업-유형"></a>

### 11. 작업 유형

#### CPU 작업

- 외부 I/O 없이 계산 자원을 사용하는 작업임
- `map`, `flatMap` 등 여러 ZIO 연산으로 나뉜 계산은 런타임이 양보 지점을 확보 가능
- 하나의 거대한 순수 함수나 레거시 계산을 단일 연산으로 감싸면 런타임이 중간에 개입할 수 없음
  - 해당 Fiber가 JVM 스레드 하나를 장시간 독점할 수 있음
  - 다른 Fiber의 실행 지연 발생 가능
- 빠르게 양보하지 않는 큰 CPU 작업은 전용 실행 위치로 격리 필요
- 공식 문서는 이러한 작업에 `ZIO#blocking`과 전용 스레드 풀 사용을 안내함

#### 블로킹 I/O

- 소켓, 파일, JDBC, 락처럼 호출한 JVM 스레드를 `park`하거나 대기시키는 작업임
- 블로킹된 스레드는 스택과 JVM, OS 메타데이터를 계속 점유함
- 기본 런타임 풀의 모든 스레드가 블로킹되면 실행 가능한 다른 Fiber도 대기하게 됨
- 블로킹 작업은 ZIO의 블로킹 스레드 풀로 격리해야 함
- 인터럽션 방식은 사용하는 Java API의 취소 계약에 맞춰 선택해야 함

#### 비동기 I/O

- 대기할 일이 생기면 스레드를 멈추지 않고 콜백을 등록한 뒤 즉시 반환하는 작업임
- 결과가 준비되면 콜백이 Fiber를 다시 실행 가능한 상태로 만듦
- 콜백 API는 직접 합성하기 어렵지만 ZIO 이펙트로 감싸면 순차 코드처럼 구성 가능
- 레거시 콜백 API는 `ZIO.async` 계열 생성자로 변환 가능
- `ZIO.sleep`, `Queue#take`, `Queue#offer`, `Semaphore#withPermit` 등은 호출 Fiber만 논리적으로 대기시킴
  - 기반 JVM 스레드는 반납되므로 다른 Fiber 실행 가능

<a id="12-fiberstatus"></a>

### 12. Fiber.Status

- `Fiber.Status`는 Fiber의 현재 상태를 나타냄
- 가능한 상태
  - `Done`: 성공, 실패, 인터럽션 중 하나로 실행을 마친 상태임
  - `Running`: 현재 실행 가능하거나 실행 중인 상태임
  - `Suspended`: 다른 Fiber나 비동기 결과 등을 기다리며 일시 중단된 상태임
    - `blockingOn`을 통해 무엇을 기다리는지 진단 가능

```scala
for {
  target <- ZIO.never.fork
  waiter <- target.await.fork
  blockingOn <- waiter.status
    .collect(()) {
      case Fiber.Status.Suspended(_, _, blockingOn) => blockingOn
    }
    .eventually
} yield assert(blockingOn == target.id)
```

관찰한 상태는 그 시점의 스냅샷이므로 바로 달라질 수 있다. 따라서 동기화 조건을 구현하기보다는 모니터링이나 디버깅 정보로 사용하는 편이 적합하다.

<a id="13-fiberid"></a>

### 13. FiberId

- `FiberId`는 Fiber의 정체성을 나타내는 값임
- 구성 정보
  - `id`: 원자적 카운터에서 얻은 단조 증가 고유 번호임
    - 형태는 `0, 1, 2, ...`임
  - `startTimeSeconds`: Fiber가 시작된 UTC 시각을 초 단위로 나타냄
    - `java.lang.System.currentTimeMillis / 1000`에서 유도됨
- 로그, 트레이스, Fiber 상태 진단에서 실행 단위를 연결하는 식별자로 활용 가능
- 애플리케이션의 영속적인 업무 식별자로 사용하기에는 부적합함
  - 프로세스 런타임의 Fiber 실행을 식별하는 값이기 때문임

<a id="14-실무-선택-기준"></a>

### 14. 실무 선택 기준

- 독립적인 두 이펙트를 함께 완료 → `zipPar`
- 컬렉션을 병렬 처리 → `foreachPar` 또는 병렬도 제한 변형
- 가장 먼저 성공한 결과 선택 → `race`
- 제한 시간 적용 → `timeout`
- 결과를 나중에 직접 기다리거나 취소 → `fork` 후 `join`, `interrupt`
- 성공과 모든 실패 원인을 값으로 관찰 → `await`
- 완료 여부를 기다리지 않고 확인 → `poll`
- 부모와 무관한 애플리케이션 전역 작업 → `forkDaemon`
- 리소스 범위와 함께 정리할 작업 → `forkScoped`
- 특정 범위로 수명 이동 → `forkIn`
- 블로킹 API 호출 → 블로킹 전용 연산과 해당 API의 취소 방식 확인 필요
- 직접 Fiber를 관리하기 전에 고수준 연산자로 같은 목적을 달성할 수 있는지 우선 검토 필요

<a id="15-참고-자료"></a>

### 15. 참고 자료

- [Introduction to ZIO Fibers](https://zio.dev/reference/fiber/)
- [Fiber](https://zio.dev/reference/fiber/fiber.md/)
- [Fiber.Status](https://zio.dev/reference/fiber/fiberstatus/)
- [FiberId](https://zio.dev/reference/fiber/fiberid/)
- [Basic Concurrency](https://zio.dev/overview/basic-concurrency/)
- [ZIO Interruption Model](https://zio.dev/reference/interruption/)
- [ZIO Fiber API](https://zio.dev/api/zio/fiber)

## ZIO 동시성 프리미티브

> 원본: https://zio.dev/reference/concurrency/

<a id="1-동시성-프리미티브-개요overview"></a>
### 1. 동시성 프리미티브 개요(Overview)

동시성 프로그램에서는 여러 파이버가 하나의 상태에 접근하는 경우가 많다. 이런 공유 상태를 갱신할 때 경쟁 상태가 생길 수 있으므로, 스레드 사이에서 "상태에 대한 일관된 관점(a consistent view of states)"을 유지하는 방법이 필요하다.

#### 동시성을 다루는 두 가지 접근

- ZIO는 동시성 문제를 두 가지 모델로 다룸

- 1\. 공유 상태(Shared State): 여러 스레드가 동일한 메모리 위치를 통해 통신함
- 2\. 메시지 전달(Message Passing): 각 스레드가 분산된 상태(distributed state)를 유지하고 메시지를 교환함

- 공유 상태 안에서, 전통적인 방식은 락(lock)을 사용하거나 비교-후-교환(compare-and-swap, CAS) 같은 논블로킹(non-blocking) 원자적 연산을 사용함

#### 전통적인 락의 한계

- 락 기반 동시성(lock-based concurrency)에는 다음과 같은 심각한 문제가 있음

- 락을 잘못 사용하면 교착 상태(deadlock)로 이어질 수 있으며, 이를 피하려면 세심한 순서 관리(careful ordering)가 필요함
- 어느 코드 영역이 취약한지(vulnerable) 파악하기가 매우 어려움
- 복잡도가 커질수록 확장(scaling)이 어려워짐
- 락이 걸린 구역(locked section) 내부에서 예외 처리(exception handling)를 할 때 세심한 주의가 필요함
- 락 메커니즘은 프로그램 조각들의 캡슐화 속성(encapsulation property)을 위반함

#### ZIO의 논블로킹(Lock-Free) 대안

ZIO는 논블로킹 알고리즘의 한 형태인 락 프리(lock-free) 동시성 모델을 사용한다. 이 모델에서는 CAS 연산으로 변수가 처음 읽은 값과 일치할 때만 갱신하고, 그동안 값이 바뀌었다면 연산을 재시도한다.

#### 핵심 장점

- 조합 가능(Composable): 선언적(declarative) 스타일과 재사용 가능한 프리미티브를 제공함
- 논블로킹(Non-blocking): 완전히 비동기적인(asynchronous) 연산임
- 자원 안전(Resource Safe): 인터럽트(interruption)가 발생해도 자원이 누수(leak)되지 않음

#### 제공되는 동시성 프리미티브

- Ref: 공유되고 갱신 가능한 상태를 위한 원자적 참조(atomic reference). (이 프리미티브는 07 문서에서 자세히 다룸.)
- Promise: 파이버 동기화를 위한 단일 할당(single-assignment) 변수
- Semaphore: 권한(permit) 기반 접근 제어
- Queue: 비동기, 논블로킹 메시지 큐
- Hub: 여러 구독자를 위한 브로드캐스트(broadcasting) 메커니즘

> 참고: `Ref` / `Ref.Synchronized`는 07 문서에서, STM(소프트웨어 트랜잭셔널 메모리)은 10 문서에서 각각 다룸.

<a id="2-promise--단일-할당-동기화-변수"></a>

### 2. Promise: 단일 할당 동기화 변수

#### 정의

- `Promise[E, A]`는 정확히 한 번(exactly once) 설정될 수 있는 `IO[E, A]` 타입의 변수임
  - 공식 문서에서는 이를 "아직 사용 가능하지 않을 수 있는 단일 값을 나타내는 순수 함수형 동기화 프리미티브(a purely functional synchronization primitive)"로 설명함

핵심 특성은 다음과 같다.

- 생성 시점에는 비어 있는(empty) 상태로 시작함
- 정확히 한 번만 완료(complete)됨
- 완료된 이후에는 절대 다시 비워지거나(empty) 수정되지 않음
- 파이버 간(fiber-to-fiber) 동기화에 유용함
- 완료를 기다리는 동안 파이버는 (커널 스레드가 아니라) 의미론적으로 블로킹(semantically block)됨

#### 생성(Creation)

- `Promise.make[E, A]`를 사용하여 생성하며, `UIO[Promise[E, A]]`를 반환함

```scala
val ioPromise1: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
```

#### Promise 완료(Completing a Promise)

- 완료 메서드는 성공(`true`) 또는 이미 완료됨(`false`)을 나타내는 `UIO[Boolean]`을 반환함

- succeed: 성공 값으로 완료

```scala
val ioBooleanSucceeded: UIO[Boolean] = ioPromise1.flatMap(promise => promise.succeed("I'm done"))
```

- fail: 에러로 완료

```scala
val ioPromise2: UIO[Promise[Exception, Nothing]] = Promise.make[Exception, Nothing]
val ioBooleanFailed: UIO[Boolean] = ioPromise2.flatMap(promise => promise.fail(new Exception("boom")))
```

- 그 밖의 완료 메서드:

- done: `Exit[E, A]` 값으로 완료함
- complete: 이펙트(effect)를 한 번 실행하고 그 결과를 대기 중인 모든 파이버에 전파함
- completeWith: 첫 번째 호출자가 승리(first caller wins)하며, 이펙트는 대기 중인 각 파이버마다 실행됨
  - (주의해서 사용해야 함.)
- die: `Throwable` 결함(defect)으로 실패시킴
- failCause: `Cause[E]`로 완료함
- interrupt: Promise를 인터럽트함

> 주의: `complete`와 `completeWith`의 차이에 유의해야 함. `complete`는 이펙트를 한 번만 실행하여 그 결과를 모든 대기 파이버에 동일하게 전달하는 반면, `completeWith`는 대기 중인 각 파이버마다 이펙트를 실행함. 예를 들어 이펙트가 난수를 생성한다면 `complete`는 모두 동일한 값을, `completeWith`는 파이버마다 다른 값을 받게 됨.

#### 대기(Awaiting)

- 완료될 때까지 현재 파이버를 일시 중단(suspend)함

```scala
val ioPromise3: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
val ioGet: IO[Exception, String] = ioPromise3.flatMap(promise => promise.await)
```

#### 폴링(Polling)

- 일시 중단 없이 완료 상태를 조회함

```scala
val ioPromise4: UIO[Promise[Exception, String]] = Promise.make[Exception, String]
val ioIsItDone: UIO[Option[IO[Exception, String]]] = ioPromise4.flatMap(p => p.poll)
val ioIsItDone2: IO[Option[Nothing], IO[Exception, String]] = ioPromise4.flatMap(p => p.poll.some)
```

`poll`의 반환 타입은 `Option[IO[E, A]]`다. 아직 완료되지 않았으면 `None`, 완료되었으면 `Some`을 반환한다.

- isDone: 완료 여부를 나타내는 `UIO[Boolean]`을 반환함

#### 전체 예제

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

한 파이버가 1초 뒤 Promise를 완료하면, `promise.await`에서 기다리던 다른 파이버가 결과를 받아 출력한다. 이처럼 Promise의 완료를 통해 두 파이버의 실행을 연결할 수 있다.

<a id="3-queue--비동기-동시성-큐"></a>

### 3. Queue: 비동기 동시성 큐

#### 정의

- `Queue[A]`는 타입 `A`의 값들을 담으며 두 가지 기본 연산을 가짐
  - `offer`는 `A`를 `Queue`에 넣고, `take`는 `Queue`에서 가장 오래된 값을 제거하고 반환함

- Queue는 "조합 가능하고 투명한 배압(composable and transparent back-pressure)을 갖춘 경량 인메모리 큐(lightweight in-memory queue)"로, ZIO 위에 구축되어 있음
  - 완전히 비동기적이며(락이나 블로킹 없음), 순수 함수형(purely-functional)이고 타입 안전(type-safe)함

#### Queue 생성

- Bounded(유한, 배압 적용): 가득 차면 공간이 생길 때까지 `offer`가 일시 중단됨

```scala
val boundedQueue: UIO[Queue[Int]] = Queue.bounded[Int](100)
```

- Dropping(가득 차면 새 항목 폐기): 가득 찬 상태에서 새 항목을 버림

```scala
val droppingQueue: UIO[Queue[Int]] = Queue.dropping[Int](100)
```

- Sliding(가득 차면 오래된 항목 제거): 가득 찬 상태에서 새 항목을 넣기 위해 가장 오래된 항목을 제거함

```scala
val slidingQueue: UIO[Queue[Int]] = Queue.sliding[Int](100)
```

- Unbounded(무한): 용량 제한이 없음

```scala
val unboundedQueue: UIO[Queue[Int]] = Queue.unbounded[Int]
```

#### 항목 추가(Adding Items)

- 단일 항목:

```scala
val res1: UIO[Unit] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(1)
} yield ()
```

- 여러 항목:

```scala
val res3: UIO[Unit] = for {
  queue <- Queue.bounded[Int](100)
  items = Range.inclusive(1, 10).toList
  _ <- queue.offerAll(items)
} yield ()
```

#### 항목 소비(Consuming Items)

- Take(비어 있으면 블로킹): 큐가 비어 있으면 항목이 들어올 때까지 대기함
  - 아래 예제는 `take`를 먼저 `fork`하여 비동기로 대기시키고, 이후 `offer`로 값을 넣은 뒤 `join`으로 결과를 회수함

```scala
val oldestItem: UIO[String] = for {
  queue <- Queue.bounded[String](100)
  f <- queue.take.fork
  _ <- queue.offer("something")
  v <- f.join
} yield v
```

- Poll(논블로킹): 즉시 반환하며, 값이 없으면 `None`을 반환함

```scala
val polled: UIO[Option[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  head <- queue.poll
} yield head
```

- TakeUpTo(최대 N개 배치 회수): 지정한 개수까지의 항목을 한 번에 가져옴

```scala
val taken: UIO[Chunk[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  chunk <- queue.takeUpTo(5)
} yield chunk
```

- TakeAll(즉시 전체 비우기): 큐에 있는 모든 항목을 즉시 가져옴

```scala
val all: UIO[Chunk[Int]] = for {
  queue <- Queue.bounded[Int](100)
  _ <- queue.offer(10)
  _ <- queue.offer(20)
  chunk <- queue.takeAll
} yield chunk
```

#### 종료 연산(Shutdown Operations)

- shutdown(대기 중인 파이버를 인터럽트): 큐를 종료하고, `take`/`offer`로 일시 중단된 파이버를 인터럽트함

```scala
val takeFromShutdownQueue: UIO[Unit] = for {
  queue <- Queue.bounded[Int](3)
  f <- queue.take.fork
  _ <- queue.shutdown
  _ <- f.join
} yield ()
```

- awaitShutdown(종료를 대기): 큐가 종료될 때까지 기다림

```scala
val awaitShutdown: UIO[Unit] = for {
  queue <- Queue.bounded[Int](3)
  p <- Promise.make[Nothing, Boolean]
  f <- queue.awaitShutdown.fork
  _ <- queue.shutdown
  _ <- f.join
} yield ()
```

#### 핵심 개념

- 배압(Back-pressure): 유한(bounded) 큐가 가득 차면, `offer` 연산은 공간이 생길 때까지 일시 중단됨
  - 이는 빠른 생산자(producer)가 느린 소비자(consumer)를 압도하지 못하도록 자연스럽게 속도를 조절함
- fork 패턴: 현재 파이버를 블로킹하지 않고 비동기로 대기하기 위해 `fork`를 사용함

<a id="4-hub--브로드캐스트-메커니즘"></a>

### 4. Hub: 브로드캐스트 메커니즘

#### 정의와 핵심 개념

`Hub`는 비동기 메시지 브로드캐스트 시스템이다. `Queue`가 각 값을 하나의 소비자에게 전달하는 데 비해, `Hub`는 같은 값을 모든 활성 구독자에게 전달한다. 여러 소비자가 같은 메시지를 받아야 할 때 사용하는 구조다.

#### 기본 연산자

핵심 Hub 인터페이스는 다음과 같다.

```scala
trait Hub[A] {
  def publish(a: A): UIO[Boolean]
  def subscribe: ZIO[Scope, Nothing, Dequeue[A]]
}
```

- publish: 모든 구독자에게 메시지를 전송하고, 발행 성공 여부를 나타내는 `Boolean`을 반환함
- subscribe: 발행된 메시지를 수신하기 위한 `Dequeue`를 제공하는 스코프드(scoped) 이펙트를 반환함
  - 스코프(scope)가 닫히면 구독이 해제됨

#### 기본 예제

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

#### Hub 생성자(Constructors)

- Bounded Hub: 용량에 도달하면 배압(backpressure)을 적용함
  - 구독 중인 모든 구독자가 모든 메시지를 받음
  - 효율을 위해 2의 거듭제곱(powers of two) 용량이 권장됨

```scala
def bounded[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

- Dropping Hub: 가득 차면 값을 버리고 `publish`가 `false`를 반환함
  - 발행자(publisher)는 블로킹되지 않고 계속 진행하지만, 구독자는 일부 값을 놓칠 수 있음

```scala
def dropping[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

- Sliding Hub: 용량이 가득 찬 상태에서 새 값이 도착하면 가장 오래된 값을 제거함
  - 발행은 항상 즉시 성공함
  - 느린 구독자가 발행자를 블로킹할 수 없음

```scala
def sliding[A](requestedCapacity: Int): UIO[Hub[A]] = ???
```

- Unbounded Hub: 결코 가득 차지 않음
  - 발행자 속도 저하 없이 모든 구독자가 모든 메시지를 받음을 보장하지만, 메모리가 무한히 증가(unbounded memory growth)할 위험이 있음

```scala
def unbounded[A]: UIO[Hub[A]] = ???
```

#### Hub 연산자(Operators)

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

- publishAll: 여러 값을 원자적으로(atomically) 발행함
- capacity: 고정된 허브 용량(불변).
- size: 현재 메시지 개수(이펙트로 조회).
- 종료 연산(shutdown / awaitShutdown / isShutdown): 허브의 수명 주기(lifecycle)를 관리함

#### Hub를 Enqueue로 사용하기

```scala
trait Hub[A] extends Enqueue[A]
```

- `Hub`는 `Enqueue`가 필요한 곳 어디에서나 사용할 수 있음
  - 이를 통해 스트림 연산자(stream operator)로 허브에 쓰기를 할 수 있음

```scala
type Transaction = ???
val transactionStream: ZStream[Any, Nothing, Transaction] = ???
val hub: Hub[Take[Nothing, Transaction]] = ???
transactionStream.into(hub)
```

- 이제 여러 다운스트림 소비자(downstream consumer)가 구독을 통해 모든 트랜잭션을 받게 됨

#### Hub와 스트림(Streams) 통합

- 허브로부터 스트림 생성:

```scala
object ZStream {
  def fromHub[O](hub: Hub[O]): ZStream[Any, Nothing, O] = ???
  def fromHubScoped[O](
    hub: Hub[O]
  ): ZIO[Scope, Nothing, ZStream[Any, Nothing, O]] = ???
}
```

- fromHub: 구독한 뒤 발행된 모든 값을 방출(emit)하고, 스트림이 끝나면 자동으로 구독을 해제함
- fromHubScoped: 스코프드 변형으로, 발행자가 구독 완료를 기다려야 하는 경우에 유용함

- Promise 협응을 사용하는 스코프드 예제:

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

- 스트림을 허브로 발행:

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

- toHub: 유한 허브를 생성하고 스트림 요소를 그 허브에 발행함
- runIntoHub: 기존 허브로 값을 전송함(`Take` 값으로 감쌈).

- Take 기반 예제:

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

- 싱크(Sink) 지원:

```scala
object ZSink {
  def fromHub[I](
    hub: Hub[I]
  ): ZSink[Any, Nothing, I, Nothing, Unit] = ???
}
```

- 허브로 값을 발행하는 싱크를 생성함
  - `fromHubWithShutdown` 변형도 사용할 수 있음

#### 브로드캐스트 연산자(Broadcast Operators)

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

- broadcast: N개의 새 스트림을 생성하며, 각각이 원본 스트림의 모든 값을 받음
- broadcastDynamic: 고정된 스트림을 만들지 않고 동적으로 구독/구독 해제할 수 있게 함

- 두 연산자 모두 최적의 성능을 위해 내부적으로 `Hub`를 사용함

#### 핵심 설계 주의사항

- 구독자는 자신이 활성으로 구독 중일 때 발행된 메시지만 받음
- 타이밍이 중요할 때는 `Promise` 협응(coordination)이나 `fromHubScoped`를 사용한다.
- `Take`로 감싸면 스트림의 완료(completion)나 에러(error)를 허브를 통해 표현할 수 있음
- 효율을 위해 허브 용량은 2의 거듭제곱이어야 함
- 실시간(real-time) 애플리케이션은 종종 엄격한 전달 보장(strict delivery guarantee)을 요구하지 않음

<a id="5-semaphore--권한-기반-접근-제어"></a>

### 5. Semaphore: 권한 기반 접근 제어

#### 정의

- `Semaphore`는 파이버 간 동기화를 지원하는 ZIO의 동시성 프리미티브임
  - 권한 기반(permit-based) 시스템으로 공유 자원에 대한 동시 접근(concurrent access)을 제어함

#### 핵심 개념

세마포어는 사용 가능한 권한(permit)의 수를 관리한다. 작업은 실행 전에 권한을 획득하고 끝나면 반환하며, 남은 권한이 없으면 다른 작업이 반환할 때까지 논리적으로 대기한다.

#### 핵심 연산

- withPermit: 작업 실행을 위해 단일 권한을 안전하게 획득하고 반환함
  - 성공, 실패, 인터럽트 여부와 관계없이 권한 반환을 보장함
- withPermits: 단일 연산에서 여러 개의 권한을 획득, 반환할 수 있는 카운팅(counting) 변형임

#### 문서 예제

- 다음 예제는 권한이 1개인 세마포어로, 3개의 동일한 작업을 병렬(`collectAllPar`)로 실행하더라도 한 번에 하나의 작업만 임계 구역(critical section)에 진입하도록 직렬화함

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

- 다중 권한(Multiple Permits) 예제: 한 작업이 한 번에 5개의 권한을 점유함

```scala
val semTaskN = (sem: Semaphore) => for {
  _ <- sem.withPermits(5)(task)
} yield ()
```

#### 보장(Guarantee)

- 각 획득(acquisition)은 작업의 성공, 실패, 인터럽트 여부와 관계없이 반드시 동일한 횟수의 반환(release)으로 이어짐
  - 즉, 권한 누수가 발생하지 않음

<a id="6-동기화-보조-도구-개요synchronization-primitives"></a>

### 6. 동기화 보조 도구 개요(Synchronization Primitives)

- ZIO는 동시성 환경에서 공유 자원을 다룰 때 발생할 수 있는 데이터 불일치(data inconsistency)를 방지하기 위해 `zio-concurrent` 모듈을 통해 동기화 메커니즘을 제공함

#### 설치(Installation)

- `build.sbt`에 다음 의존성을 추가함

```scala
libraryDependencies += "dev.zio" %% "zio-concurrent" % "2.x.x"
```

#### 제공되는 동기화 도구

- ReentrantLock: 동시성 시나리오에서 "코드 블록을 동기화하기" 위한 락 메커니즘
- CountDownLatch: "하나 이상의 파이버가 여러 연산의 완료를 기다릴 수 있게" 하는 협응 도구
- CyclicBarrier: "여러 파이버가 공통 배리어 지점(common barrier point)에 도달할 때까지 서로를 기다리게" 하는 배리어 메커니즘

#### 동시성 자료구조(Concurrent Data Structures)

- ConcurrentMap: `java.util.concurrent.ConcurrentHashMap`을 감싼 스레드 안전(thread-safe) 맵
- ConcurrentSet: `java.util.concurrent.ConcurrentHashMap`을 감싼 스레드 안전 셋

- 이 도구들은 동시 접근 패턴을 관리하면서 데이터 무결성(data integrity)을 유지하도록 도움

<a id="7-reentrantlock--재진입-가능-락"></a>

### 7. ReentrantLock: 재진입 가능 락

#### 정의

`ReentrantLock`은 같은 파이버가 여러 번 획득할 수 있는 락이다. 이미 보유한 파이버가 다시 획득해도 교착 상태가 발생하지 않으며, 중첩 획득 횟수는 내부의 보유 카운트(hold count)로 추적한다.

#### 핵심 연산

- 기본 연산:

- `lock: UIO[Unit]`: 락을 획득하며, 다른 파이버가 보유 중이면 블로킹함
- `unlock: UIO[Unit]`: 락을 반환하며, 보유 카운트를 감소시킴

- 편의 메서드:

- `tryLock: UIO[Boolean]`: 논블로킹 획득 시도
- `withLock: URIO[Scope, Int]`: 자동 반환(automatic release)을 동반한 스코프드 획득

- 조회 메서드:

- `holdCount`: 현재 획득 카운트
- `owner`: 락을 보유 중인 파이버
- `queuedFibers`: 대기 중인 파이버 모음

#### 생성(Creation)

```scala
object ReentrantLock {
  def make(fairness: Boolean = false): UIO[ReentrantLock] = ???
}
```

- 기본값은 불공정(unfair) 정책으로, 대기 중인 파이버를 무작위로 선택함
  - FIFO 순서가 필요하면 `fairness = true`로 설정함

#### 예제: 단순 락

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

#### 예제: 재진입(Reentrancy)

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

#### 예제: 교착 상태 시나리오(Deadlock Scenario)

- 두 파이버가 두 락을 서로 반대 순서로 획득하려 할 때 교착 상태가 발생할 수 있음을 보여 줌

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

#### 동작 관련 주의사항

이미 락을 보유한 파이버가 `lock`을 다시 호출하면 보유 카운트가 증가하고 실행을 계속한다. `unlock`을 호출할 때마다 카운트가 감소하며, 0에 도달해야 락이 실제로 해제된다. 그다음 어느 대기 파이버가 락을 획득할지는 공정성 정책(fairness policy)이 결정한다.

<a id="8-countdownlatch--카운트다운-래치"></a>

### 8. CountDownLatch: 카운트다운 래치

#### 정의

`CountDownLatch`는 다른 파이버들의 연산이 완료될 때까지 하나 이상의 파이버를 기다리게 하는 도구다. 카운트가 0이 되면 대기가 풀리며, 한 번 사용한 래치는 초기화할 수 없다. 반복해서 동기화해야 한다면 `CyclicBarrier`를 사용한다.

#### 생성(Creation)

```scala
object CountdownLatch {
  def make(n: Int): IO[Option[Nothing], CountdownLatch]
}
```

#### 연산(Operations)

```scala
class CountdownLatch {
  val countDown: UIO[Unit]
  val await: UIO[Unit]
}
```

- `countDown`: 래치 카운트를 감소시키고, 0에 도달하면 대기 중인 모든 파이버를 해제함
- `await`: 래치가 0에 도달할 때까지 현재 파이버를 블로킹함

#### 코드 예제

- 단순 On/Off 래치: 생산자가 `50`을 생성하는 순간 `countDown`이 호출되어, `await` 중이던 소비자가 비로소 시작됨

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

- 고급 래치(다중 생산자): 카운트가 5이며, 10개의 생산자 파이버가 동작함
  - 누적된 `countDown` 호출로 카운트가 0이 되면 소비자가 시작됨

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

<a id="9-cyclicbarrier--순환-배리어"></a>

### 9. CyclicBarrier: 순환 배리어

#### 정의

`CyclicBarrier`는 정해진 수의 파이버가 공통 지점에 모두 도착할 때까지 기다리게 한다. 배리어가 해제된 뒤에도 재사용할 수 있어 반복되는 작업 단계를 동기화할 때 쓴다.

#### 생성(Creation)

```scala
object CyclicBarrier {
  def make(parties: Int): UIO[CyclicBarrier] = ???
  def make(parties: Int, action: UIO[Any]): UIO[CyclicBarrier] = ???
}
```

- 두 가지 생성자가 있음
  - 하나는 참가자 수(party count)만 받고, 다른 하나는 배리어 해제 시 실행되는 선택적 이펙트(`action`)를 추가로 받음

#### 핵심 연산

- `parties`: 타입 `Int` → 배리어를 트립(trip)시키는 데 필요한 참가자 수
- `waiting`: 타입 `UIO[Int]` → 현재 배리어에서 일시 중단된 참가자 수
- `await`: 타입 `IO[Unit, Int]` → 모든 참가자가 await를 호출할 때까지 일시 중단
- `reset`: 타입 `UIO[Unit]` → 초기 상태로 리셋하며, 대기 중인 파이버를 인터럽트
- `isBroken`: 타입 `UIO[Boolean]` → 배리어가 깨진(broken) 상태인지 확인

#### 코드 예제(단순 경우)

- 3개의 파티가 모두 `await`에 도달해야 배리어가 트립되어 모두 동시에 진행됨

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

#### 순환 동작(Cyclic Behavior)

- 파티(parties) 수보다 많은 작업이 있는 경우, 배리어는 해제 이후 자동으로 리셋되어 다음 그룹이 동기화될 수 있음
  - 이 반복 특성 때문에 "순환적(cyclic)"이라는 이름이 붙었음

#### 배리어 깨짐(Barrier Breakage)

- 배리어는 다음 상황에서 깨짐

- 대기 중인 어떤 파이버가 인터럽트(interruption), 실패(failure), 또는 타임아웃(timeout)을 겪을 때
- 배리어에 수동으로 `reset`을 호출할 때

- 깨짐(breaking)은 "전부 아니면 전무(all-or-none)" 모델을 통해 대기 중인 모든 파티에 전파됨

<a id="10-concurrentmap--동시성-맵"></a>

### 10. ConcurrentMap: 동시성 맵

#### 정의

- `ConcurrentMap`은 `java.util.concurrent.ConcurrentHashMap`을 감싼 래퍼로, 여러 파이버에서 키-값 쌍(key-value pair)에 스레드 안전(thread-safe)하게 동시 접근할 수 있도록 함

#### 동기(Motivation)

- 표준 Scala `HashMap`은 스레드 안전하지 않음
  - 여러 파이버에서 동시에 수정(concurrent modification)하면 일관되지 않은 결과(inconsistent result)가 발생할 수 있으므로, 동시성 워크플로에서는 `ConcurrentMap`을 사용해야 함

#### 생성자(Constructors)

```scala
ConcurrentMap.empty[String, Int]
ConcurrentMap.make(("foo", 0), ("bar", 1), ("baz", 2))
ConcurrentMap.fromIterable(List(("foo", 0), ("bar", 1), ("baz", 2)))
```

#### 핵심 연산

- 조회(Retrieval):

- `get(key: K): UIO[Option[V]]`: 키로 값을 조회함
- `exists(p: (K, V) => Boolean): UIO[Boolean]`: 술어(predicate)를 검사함
- `collectFirst[B](pf: PartialFunction[(K, V), B]): UIO[Option[B]]`: 첫 번째 일치 항목을 찾음
- `fold[S](zero: S)(f: (S, (K, V)) => S): UIO[S]`: 엔트리들을 집계(aggregate)함
- `forall(p: (K, V) => Boolean): UIO[Boolean]`: 모든 요소를 검증함
- `isEmpty: UIO[Boolean]`, `toChunk`, `toList`

- 삽입(Insertion):

- `put(key: K, value: V): UIO[Option[V]]`
- `putIfAbsent(key: K, value: V): UIO[Option[V]]`
- `putAll(keyValues: (K, V)*): UIO[Unit]`

- 제거(Removal):

- `remove(key: K): UIO[Option[V]]`
- `remove(key: K, value: V): UIO[Boolean]`
- `removeIf(p: (K, V) => Boolean): UIO[Unit]`
- `clear: UIO[Unit]`

- 재매핑(Remapping):

- `compute(key: K, remap: (K, V) => V): UIO[Option[V]]`
- `computeIfAbsent(key: K, map: K => V): UIO[V]`
- `computeIfPresent(key: K, remap: (K, V) => V): UIO[Option[V]]`

- 교체(Replacement):

- `replace(key: K, value: V): UIO[Option[V]]`
- `replace(key: K, oldValue: V, newValue: V): UIO[Boolean]`

<a id="11-concurrentset--동시성-셋"></a>

### 11. ConcurrentSet: 동시성 셋

#### 정의

- `ConcurrentSet`은 `java.util.concurrent.ConcurrentHashMap`을 기반으로 구현된 Set → ZIO에서 스레드 안전한 집합 연산(set operation)을 제공함

#### 생성자(Constructors)

- `empty[A]: UIO[ConcurrentSet[A]]`: 빈 셋을 생성
- `empty[A](initialCapacity: Int)`: 지정된 초기 용량으로 빈 셋 생성
- `fromIterable[A](as: Iterable[A])`: 컬렉션으로부터 초기화
- `make[A](as: A*)`: 제공된 요소들로 초기화

#### 핵심 연산

- 추가(Adding Elements):

- `add(x: A): UIO[Boolean]`: 단일 값 삽입
- `addAll(xs: Iterable[A])`: 여러 값 삽입

- 제거(Removing Elements):

- `remove(x: A)`: 특정 값 삭제
- `removeAll(xs: Iterable[A])`: 여러 값 제거
- `removeIf(p: A => Boolean)`: 술어에 일치하는 요소 제거
- `clear`: 셋 비우기

- 조회(Querying):

- `contains(x: A)`: 멤버십 검사
- `size`: 요소 개수
- `isEmpty`: 비어 있는지 검사
- `toSet`: 불변(immutable) Set으로 변환

- 함수형 연산(Functional Operations):

- `exists(p: A => Boolean)`: 술어를 만족하는 요소가 있는지 검사
- `forall(p: A => Boolean)`: 모든 요소가 만족하는지 검사
- `fold[S](zero: S)(f: (S, A) => S)`: 값들을 누적(accumulate).
- `transform(f: A => A)`: 모든 요소를 변형(modify).
- `collectFirst[B](pf: PartialFunction)`: 첫 번째 일치 요소를 찾음

- 유지(Retention):

- `retainAll(xs: Iterable[A])`: 지정된 요소들만 유지
- `retainIf(p: A => Boolean)`: 술어를 만족하는 요소들만 유지

<a id="12-참고-자료"></a>

### 12. 참고 자료

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
