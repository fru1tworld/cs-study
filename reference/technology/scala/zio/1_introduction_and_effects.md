# ZIO 소개와 Effect 기본

## ZIO 소개와 시작하기

> 원본: https://zio.dev/overview/getting-started

---

### 목차

1. [ZIO란 무엇인가(What is ZIO)](#1-zio란-무엇인가what-is-zio)
2. [왜 ZIO를 쓰는가 — 핵심 장점(Why ZIO)](#2-왜-zio를-쓰는가--핵심-장점why-zio)
3. [ZIO 데이터 타입과 타입 별칭(The ZIO Data Type)](#3-zio-데이터-타입과-타입-별칭the-zio-data-type)
4. [의존성 추가와 설치(Installation)](#4-의존성-추가와-설치installation)
5. [첫 ZIO 애플리케이션(ZIOAppDefault)](#5-첫-zio-애플리케이션zioappdefault)
6. [Console 상호작용(Console Interaction)](#6-console-상호작용console-interaction)
7. [기존 애플리케이션에 통합하기(Runtime)](#7-기존-애플리케이션에-통합하기runtime)
8. [지원 플랫폼(Platforms)](#8-지원-플랫폼platforms)
9. [성능 특징(Performance)](#9-성능-특징performance)
10. [ZIO 생태계 라이브러리(Ecosystem)](#10-zio-생태계-라이브러리ecosystem)
11. [참고 자료](#11-참고-자료)

---

### 1. ZIO란 무엇인가(What is ZIO)

ZIO는 Scala를 위한 타입 안전(type-safe)하고 합성 가능한(composable) 비동기(asynchronous)·동시성(concurrent) 프로그래밍 프레임워크임. 공식 요약(Summary) 문서는 ZIO를 "JVM 상에서 클라우드 네이티브(cloud-native) 애플리케이션을 구축하기 위한 차세대 프레임워크(next-generation framework)"이자, "초심자 친화적이면서도 강력한 함수형 코어(beginner-friendly yet powerful functional core)"를 갖춘 도구로 소개.

ZIO로 작성한 애플리케이션이 지닐 수 있는 특성:

- 높은 확장성(highly scalable)
- 테스트 용이성(testable)
- 견고함(robust)
- 복원력(resilient)
- 자원 안전성(resource-safe)
- 효율성(efficient)
- 관찰 가능성(observable)

ZIO는 개발자가 **참조 투명성**(referential transparency), **합성 가능한 데이터 타입**(composable data types), **타입 안전성**(type-safety) 등을 활용해 동시성·복원력 있는 애플리케이션을 Scala로 구축할 수 있게 함.

---

### 2. 왜 ZIO를 쓰는가 — 핵심 장점(Why ZIO)

ZIO 홈페이지가 강조하는 핵심 기능과 장점(features & benefits):

- **고성능(High-performance)**: 런타임 오버헤드(runtime overhead)를 최소화하면서 확장 가능한 애플리케이션 구축
- **타입 안전(Type-safe)**: Scala 컴파일러의 능력을 활용해 버그를 컴파일 시점(compile time)에 검출
- **동시성(Concurrent)**: 교착 상태(deadlock)·경쟁 조건(race condition)·추가적인 복잡성 없이 동시성 애플리케이션 작성 가능
- **비동기(Asynchronous)**: 연산이 비동기든 동기든 동일한 모습의 순차적 코드(sequential code) 작성 가능
- **자원 안전(Resource-safe)**: 장애(failure) 상황에서도 스레드(thread)를 포함한 자원을 절대 누수(leak)하지 않는 애플리케이션 개발
- **테스트 용이(Testable)**: 테스트용 서비스를 주입(inject)하여 빠르고, 결정적(deterministic)이며, 타입 안전한 테스트 작성
- **복원력(Resilient)**: 오류를 보존하고 장애에 유연하게 대응하는 애플리케이션 제작
- **함수형(Functional)**: 단순하고 재사용 가능한 빌딩 블록(building block)으로부터 정교한 해법을 합성(compose)

---

### 3. ZIO 데이터 타입과 타입 별칭(The ZIO Data Type)

ZIO의 토대에는 **함수형 효과**(functional effect)라 불리는 `ZIO` 데이터 타입이 있음. 이는 "모든 ZIO 애플리케이션의 근본적인 빌딩 블록(the fundamental building block for every ZIO application)"으로, **하나의 연산 단위**(a unit of computation)를 표현. 실행 시 오류로 실패(fail)하거나 값으로 성공(succeed)하는 연산 또는 상호작용을 정밀하게 기술하는 "계획(plan)" 역할.

#### 3.1 세 가지 타입 매개변수(Type Parameters)

`ZIO[R, E, A]` 데이터 타입은 세 개의 타입 매개변수(type parameter)를 사용.

- **R (환경 타입, Environment Type)**: 실행 전에 필요한 문맥 데이터(contextual data)를 나타냄. 예: 데이터베이스 연결(database connection), HTTP 요청(HTTP request)
- **E (실패 타입, Failure Type)**: 해당 효과가 실패할 수 있는 오류 타입을 나타냄
- **A (성공 타입, Success Type)**: 실행 시 성공하여 산출되는 값의 타입을 나타냄

#### 3.2 타입 별칭(Type Aliases)

ZIO는 자주 쓰이는 시나리오를 위해 단순화된 타입 별칭(type alias)을 제공.

- `UIO[A]` — 요구사항(R)이 없고, 실패할 수 없는 효과
- `Task[A]` — 요구사항이 없고, `Throwable`로 실패할 수 있는 효과
- `RIO[R, A]` — R을 요구하며, `Throwable`로 실패할 수 있는 효과
- `IO[E, A]` — 요구사항이 없고, 특정 오류 타입(specific error type)으로 실패할 수 있는 효과
- `URIO[R, A]` — R을 요구하며, 실패할 수 없는 효과

> 함수형 효과(functional effect)에 처음 입문하는 사용자는 `Task`부터 시작 권장.

---

### 4. 의존성 추가와 설치(Installation)

ZIO를 사용하려면 프로젝트의 `build.sbt` 파일에 의존성을 추가.

```scala
libraryDependencies += "dev.zio" %% "zio" % "2.1.26"
libraryDependencies += "dev.zio" %% "zio-streams" % "2.1.26"
```

- 첫 번째 줄은 ZIO 코어 라이브러리 추가
- 두 번째 줄(`zio-streams`)은 합성 가능한 스트리밍(streaming) 기능을 사용할 때 추가(필요 없다면 생략 가능)

---

### 5. 첫 ZIO 애플리케이션(ZIOAppDefault)

ZIO 애플리케이션을 작성하는 가장 간단한 방법은 `ZIOAppDefault`를 확장(extend)하는 것. `ZIOAppDefault`는 완전한 런타임 시스템(runtime system)을 내장 → 별도로 런타임을 구성할 필요 없음.

```scala
import zio._
import zio.Console._

object MyApp extends ZIOAppDefault {
  def run = myAppLogic

  val myAppLogic =
    for {
      _    <- printLine("Hello! What is your name?")
      name <- readLine
      _    <- printLine(s"Hello, ${name}, welcome to ZIO!")
    } yield ()
}
```

#### `run` 메서드와 예외 없는 효과(Unexceptional Effect)

`run` 메서드는 **모든 오류가 처리된**(all its errors handled) ZIO 값을 반환해야 함. ZIO 용어로는 "예외 없는 ZIO 값(an unexceptional ZIO value)"이라 칭함.

오류 처리는 일반적으로 `fold`를 사용. `fold`는 두 개의 함수를 받는데, 하나는 실패(failure)를 처리하고 다른 하나는 성공(success)을 처리함. 최종적으로 `Int` 결과를 산출 → 관례상 **실패는 1, 성공은 0**.

---

### 6. Console 상호작용(Console Interaction)

ZIO는 콘솔(console) 입출력을 위한 연산을 제공. 주요 함수:

- `Console.print()` — 줄바꿈 없이 출력
- `Console.printLine()` — 줄바꿈과 함께 출력
- `Console.readLine` — 사용자 입력을 한 줄 읽음

다음은 입력받은 줄을 그대로 다시 출력하는 에코(echo) 프로그램 예제.

```scala
val echo = Console.readLine.flatMap(line => Console.printLine(line))
```

`readLine`이 읽어 들인 한 줄(`line`)을 `flatMap`으로 받아 `printLine`에 전달 → 입력을 그대로 화면에 다시 출력.

---

### 7. 기존 애플리케이션에 통합하기(Runtime)

`main` 함수를 직접 제어할 수 없어 `ZIOAppDefault`를 확장하기 어려운 기존 애플리케이션에 ZIO를 통합해야 할 때 → 런타임(runtime)을 직접 생성해 효과 실행 가능.

```scala
val runtime = Runtime.default
Unsafe.unsafe { implicit unsafe =>
  runtime.unsafe.run(ZIO.attempt(println("Hello World!"))).getOrThrowFiberFailure()
}
```

> **주의(Important):** 이상적으로 애플리케이션은 **단 하나의 런타임**(a single runtime)만 가져야 함. 각 런타임은 자체 자원(스레드 풀(thread pool)과 처리되지 않은 오류 리포터(unhandled error reporter) 포함)을 보유하기 때문.

`Unsafe.unsafe { ... }` 블록은 명시적으로 안전하지 않은(unsafe) 실행 영역임을 표시 → `getOrThrowFiberFailure()`는 파이버(fiber)의 실패를 예외로 던져 결과를 가져옴.

---

### 8. 지원 플랫폼(Platforms)

ZIO는 세 가지 주요 플랫폼을 지원하며 플랫폼 간 일관된 인터페이스 제공을 목표로 함. 다만 불가피한 플랫폼별 차이(platform-specific differences)가 있으므로 개발자가 이를 숙지 필요.

#### 8.1 JVM

- Java 버전 **11 이상**과 Scala 버전 **2.12, 2.13, 3.x** 지원
- `ZIO.blocking`, `ZIO.attemptBlocking` 같은 메서드를 통해 블로킹 연산(blocking operation) 수행 가능

#### 8.2 Scala.js

- **Scala.js 1.0**과 호환
- Scala.js에는 `java.time` 구현이 없으므로 개발자가 직접 `java.time` 의존성 제공 필요. 권장 라이브러리는 **scala-java-time**(버전 2.2.0)
- 주요 제약 사항:
  - JavaScript의 단일 스레드(single-threaded) 특성으로 인해 블로킹 연산은 Scala.js에서 미지원
  - `Console` 서비스의 `readLine` 메서드는 미지원
  - `Runtime`의 동기 실행(synchronous execution) 메서드는 비동기 효과(asynchronous effect)에 대해 안전하지 않음(unsafe)

#### 8.3 Scala Native

- Scala Native 지원은 현재 **실험적**(experimental) 단계. 플랫폼이 성숙해지면 추가 세부 사항 제공 예정.

---

### 9. 성능 특징(Performance)

ZIO는 **논블로킹 파이버**(non-blocking fiber)로 구동되는 고성능 프레임워크(high-performance framework)임. 향후 Project Loom 하에서 이 파이버는 **가상 스레드**(virtual threads)로 이행 예정.

주요 성능 특성:

- **실행 엔진**(Execution Engine): "ZIO의 핵심 실행 엔진은 할당(allocation)을 최소화하고 사용되지 않는 연산(unused computation)을 자동으로 취소(cancel)함"
- **자료 구조**(Data Structures): "ZIO에 포함된 모든 자료 구조는 고성능이며 논블로킹이고, JVM 상에서 가능한 한 최대로 박싱하지 않음(non-boxing)"
- **벤치마크 결과**(Benchmarking Results): `benchmarks` 프로젝트는 ZIO를 Scala 및 Java 생태계의 유사 프로젝트들과 비교하는 다양한 벤치마크를 담고 있음 → 일부 경우 **2~100배 빠른 성능**(2-100x faster performance)
- **추가 비교**(Additional Comparisons): HTTP, GraphQL, RDBMS 등 ZIO 통합 항목의 성능 비교는 각각의 해당 프로젝트에서 확인 가능

ZIO는 메모리 할당 최소화 · 사용되지 않는 연산의 자동 정리 · 유사 프레임워크 대비 성능 우위를 통해 효율성 달성.

---

### 10. ZIO 생태계 라이브러리(Ecosystem)

ZIO 홈페이지에서 소개하는 주요 생태계 라이브러리:

- **ZIO HTTP**: 타입 안전한 엔드포인트(type-safe endpoints)와 WebSocket을 지원하는 고성능 웹 라이브러리
- **ZIO Streams**: 무한 데이터(infinite data)를 위한 합성 가능한 스트리밍과 자동 배압(automatic backpressure) 처리
- **ZIO Test**: 동시성 코드 검증을 지원하는 속성 기반 테스트(property-based testing) 프레임워크
- **ZIO STM**: 원자적(atomic)이고 합성 가능한 연산을 위한 소프트웨어 트랜잭셔널 메모리(Software Transactional Memory)
- **ZIO Schema**: 자동 파생(automatic derivation)을 지원하는 선언적 스키마(declarative schema) 정의
- **ZIO Config**: 검증(validation)을 포함한 타입 안전 설정 관리(configuration management)
- **ZIO Logging**: 문맥 지원(contextual support)을 갖춘 구조적 로깅(structured logging)

> 홈페이지에서는 ZIO 기초부터 효과 시스템(effect systems), 오류 처리(error handling), 의존성 주입(dependency injection) 패턴 같은 고급 주제까지 다루는 무료 가이드 "**ZIONOMICON**"도 함께 안내.

---

### 11. 참고 자료

- [ZIO 공식 홈페이지](https://zio.dev/)
- [Overview: Getting Started](https://zio.dev/overview/getting-started)
- [Overview: Summary](https://zio.dev/overview/summary)
- [Overview: Platforms](https://zio.dev/overview/platforms)
- [Overview: Performance](https://zio.dev/overview/performance)

---

## Effect 생성과 기본 연산

> 원본: https://zio.dev/overview/creating-effects

---

### 목차

1. [Effect 생성 개요(Overview)](#1-effect-생성-개요overview)
2. [값으로부터 생성(From Values)](#2-값으로부터-생성from-values)
3. [실패 값으로부터 생성(From Failure Values)](#3-실패-값으로부터-생성from-failure-values)
4. [Scala 값으로부터 생성(From Scala Values)](#4-scala-값으로부터-생성from-scala-values)
5. [코드로부터 생성(From Code)](#5-코드로부터-생성from-code)
6. [블로킹 동기 코드(Blocking Synchronous Code)](#6-블로킹-동기-코드blocking-synchronous-code)
7. [기본 연산 개요(Basic Operations Overview)](#7-기본-연산-개요basic-operations-overview)
8. [매핑(Mapping)](#8-매핑mapping)
9. [연쇄(Chaining)](#9-연쇄chaining)
10. [for 컴프리헨션(For Comprehensions)](#10-for-컴프리헨션for-comprehensions)
11. [지핑(Zipping)](#11-지핑zipping)
12. [참고 자료](#12-참고-자료)

---

### 1. Effect 생성 개요(Overview)

이 섹션에서는 값(values), 계산(computations), 그리고 흔히 쓰이는 Scala 데이터 타입으로부터 ZIO 이펙트(effect)를 생성하는 몇 가지 일반적인 방법을 다룸.

예제 코드는 다음과 같은 임포트(import)를 전제로 함.

```scala
import zio.{ ZIO, Task, UIO, URIO, IO }
```

---

### 2. 값으로부터 생성(From Values)

`ZIO.succeed` 메서드를 사용하면 실행될 때 지정한 값으로 성공(succeed)하는 이펙트 생성 가능.

```scala
val s1 = ZIO.succeed(42)
```

`succeed` 메서드는 **이름에 의한 파라미터**(by-name parameter)를 받음. 메서드에 전달된 코드는 ZIO 이펙트 내부에 저장되어 ZIO가 관리 → 재시도(retries)·타임아웃(timeouts)·자동 오류 로깅(automatic error logging) 같은 기능 활용 가능.

---

### 3. 실패 값으로부터 생성(From Failure Values)

`ZIO.fail` 메서드를 사용하면 실행될 때 지정한 값으로 실패(fail)하는 이펙트 생성 가능.

```scala
val f1 = ZIO.fail("Uh oh!")
```

`ZIO` 데이터 타입의 경우, 오류 타입(error type)에 아무런 제약이 없음. 애플리케이션에 적합하다면 문자열(string)·예외(exception)·커스텀 데이터 타입(custom data type) 등 무엇이든 사용 가능.

많은 애플리케이션은 `Throwable`이나 `Exception`을 확장한 클래스로 실패를 모델링.

```scala
val f2 = ZIO.fail(new Exception("Uh oh!"))
```

---

### 4. Scala 값으로부터 생성(From Scala Values)

Scala 표준 라이브러리에는 ZIO 이펙트로 변환 가능한 여러 데이터 타입이 있음.

#### 4.1 Option

`Option`은 `ZIO.fromOption`을 사용해 ZIO 이펙트로 변환 가능.

```scala
val zoption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(2))
```

결과 이펙트의 오류 타입은 `Option[Nothing]`. 이는 이러한 이펙트가 실패할 경우 `None` 값(타입이 `Option[Nothing]`)으로 실패함을 의미.

`orElseFail`을 사용하면 실패를 다른 오류 값으로 변환 가능. 이는 ZIO가 오류 관리(error management)를 위해 제공하는 여러 메서드 중 하나.

```scala
val zoption2: ZIO[Any, String, Int] = zoption.orElseFail("It wasn't there!")
```

ZIO는 `Option`을 다루는 코드와의 연동을 쉽게 만들어 주는 다양한 연산자(operator)를 제공. 다음의 심화 예제에서는 `some`과 `asSomeError` 연산자를 사용해 `Option`을 반환하는 메서드들과의 연동을 더 쉽게 만듦. 이는 일부 Scala 라이브러리의 `OptionT` 타입과 유사.

```scala
trait Team
```

```scala
val maybeId: ZIO[Any, Option[Nothing], String] = ZIO.fromOption(Some("abc123"))
def getUser(userId: String): ZIO[Any, Throwable, Option[User]] = ???
def getTeam(teamId: String): ZIO[Any, Throwable, Team] = ???


val result: ZIO[Any, Throwable, Option[(User, Team)]] = (for {
  id   <- maybeId
  user <- getUser(id).some
  team <- getTeam(user.teamId).asSomeError 
} yield (user, team)).unsome 
```

#### 4.2 Either

`Either`는 `ZIO.fromEither`를 사용해 ZIO 이펙트로 변환 가능.

```scala
val zeither: ZIO[Any, Nothing, String] = ZIO.fromEither(Right("Success!"))
```

결과 이펙트의 오류 타입은 `Left` 케이스의 타입이 되고, 성공 타입은 `Right` 케이스의 타입이 됨.

#### 4.3 Try

`Try` 값은 `ZIO.fromTry`를 사용해 ZIO 이펙트로 변환 가능.

```scala
import scala.util.Try

val ztry = ZIO.fromTry(Try(42 / 0))
```

`Try`는 `Throwable` 타입의 값으로만 실패 가능 → 결과 이펙트의 오류 타입은 항상 `Throwable`.

#### 4.4 Future

Scala의 `Future`는 `ZIO.fromFuture`를 사용해 ZIO 이펙트로 변환 가능.

```scala
import scala.concurrent.Future

lazy val future = Future.successful("Hello!")

val zfuture: ZIO[Any, Throwable, String] =
  ZIO.fromFuture { implicit ec =>
    future.map(_ => "Goodbye!")
  }
```

`fromFuture`에 전달되는 함수에는 `ExecutionContext`가 제공됨 → ZIO는 이를 통해 `Future`가 어디에서 실행될지 관리 가능(물론 이 `ExecutionContext`를 무시해도 무방).

`Future` 값은 `Throwable` 타입의 값으로만 실패 가능 → 결과 이펙트의 오류 타입은 항상 `Throwable`.

---

### 5. 코드로부터 생성(From Code)

ZIO는 임의의 코드(예: 어떤 메서드 호출)를 이펙트로 변환 가능. 그 코드가 이른바 **동기**(synchronous)(값을 직접 반환)이든 **비동기**(asynchronous)(콜백(callback)에 값을 전달)이든 상관없음.

올바르게 변환하면 해당 코드가 이펙트 내부에 저장되어 ZIO가 관리 → 재시도(retries)·타임아웃(timeouts)·자동 오류 로깅(automatic error logging) 같은 기능 활용 가능.

ZIO가 제공하는 변환 함수를 사용하면, Scala나 Java로 작성된 non-ZIO 코드(서드파티 라이브러리 포함)와도 ZIO의 모든 기능을 매끄럽게 연동 가능.

#### 5.1 동기 코드(Synchronous Code)

동기 코드는 `ZIO.attempt`를 사용해 ZIO 이펙트로 변환 가능.

```scala
import scala.io.StdIn

val readLine: ZIO[Any, Throwable, String] =
  ZIO.attempt(StdIn.readLine())
```

동기 코드는 `Throwable` 타입의 어떤 값으로든 예외를 던질 수 있음 → 결과 이펙트의 오류 타입은 항상 `Throwable`.

어떤 코드가 (런타임 예외(runtime exception)를 제외하고) 예외를 던지지 않는다는 것을 확실히 안다면, `ZIO.succeed`를 사용해 그 코드를 ZIO 이펙트로 변환 가능.

```scala
def printLine(line: String): UIO[Unit] =
  ZIO.succeed(println(line))
```

때로는 코드가 특정 예외 타입(specific exception type)을 던진다는 것을 알 수 있고, 이를 ZIO 이펙트의 오류 파라미터(error parameter)에 반영하고 싶을 수 있음.

이 목적을 위해 `ZIO#refineToOrDie` 메서드 사용 가능.

```scala
import java.io.IOException

val readLine2: ZIO[Any, IOException, String] =
  ZIO.attempt(StdIn.readLine()).refineToOrDie[IOException]
```

#### 5.2 비동기 코드(Asynchronous Code)

콜백 기반 API(callback-based API)를 노출하는 비동기 코드는 `ZIO.async`를 사용해 ZIO 이펙트로 변환 가능.

```scala
trait User { 
  def teamId: String
}
trait AuthError
```

```scala
object legacy {
  def login(
    onSuccess: User => Unit,
    onFailure: AuthError => Unit): Unit = ???
}

val login: ZIO[Any, AuthError, User] =
  ZIO.async[Any, AuthError, User] { callback =>
    legacy.login(
      user => callback(ZIO.succeed(user)),
      err  => callback(ZIO.fail(err))
    )
  }
```

비동기 이펙트는 콜백 기반 API보다 훨씬 사용하기 쉬움 → 인터럽션(interruption)·리소스 안전성(resource-safety)·오류 관리(error management) 같은 ZIO 기능을 그대로 활용 가능.

---

### 6. 블로킹 동기 코드(Blocking Synchronous Code)

일부 동기 코드는 이른바 **블로킹 IO**(blocking IO)를 수행함. 이는 운영체제 호출(operating system call)이 완료되기를 기다리는 동안 스레드를 대기 상태(waiting state)에 빠뜨림. 처리량(throughput) 극대화를 위해서는, 이러한 코드가 애플리케이션의 주 스레드 풀(primary thread pool)이 아니라 블로킹 연산에 전용으로 할당된 특수한 스레드 풀(special thread pool)에서 실행되어야 함.

ZIO는 런타임(runtime)에 블로킹 스레드 풀(blocking thread pool)을 내장 → `ZIO.blocking`을 사용해 그곳에서 이펙트 실행 가능.

```scala
import scala.io.{ Codec, Source }

def download(url: String) =
  ZIO.attempt {
    Source.fromURL(url)(Codec.UTF8).mkString
  }

def safeDownload(url: String) =
  ZIO.blocking(download(url))
```

대안으로, 블로킹 코드를 직접 ZIO 이펙트로 변환하고 싶다면 `ZIO.attemptBlocking` 메서드 사용 가능.

```scala
val sleeping =
  ZIO.attemptBlocking(Thread.sleep(Long.MaxValue))
```

결과 이펙트는 ZIO의 블로킹 스레드 풀에서 실행됨.

Java의 `Thread.interrupt`에 반응하는 동기 코드(예: `Thread.sleep` 또는 락 기반(lock-based) 코드)가 있다면, `ZIO.attemptBlockingInterrupt` 메서드를 사용해 이 코드를 인터럽트 가능한(interruptible) ZIO 이펙트로 변환 가능.

일부 동기 코드는 실행 중인 계산을 취소하는 책임을 가진 별도의 코드를 호출함으로써만 취소(cancel) 가능. 이러한 코드를 ZIO 이펙트로 변환하려면 `ZIO.attemptBlockingCancelable` 메서드 사용 가능.

```scala
import java.net.ServerSocket
import zio.UIO

def accept(l: ServerSocket) =
  ZIO.attemptBlockingCancelable(l.accept())(ZIO.succeed(l.close()))
```

> **다음 단계**: 값으로부터 이펙트를 생성하고, Scala 타입을 이펙트로 변환하며, 동기·비동기 코드를 이펙트로 변환하는 데 익숙해졌다면, 다음 단계는 이펙트에 대한 기본 연산(basic operations) 학습.

---

### 7. 기본 연산 개요(Basic Operations Overview)

`String` 데이터 타입이나 Scala의 컬렉션 데이터 타입(`List`, `Map`, `Set` 등)과 마찬가지로, ZIO 이펙트는 **불변**(immutable)이며 변경 불가.

ZIO 이펙트를 변환(transform)하거나 조합(combine)하려면 ZIO 데이터 타입의 메서드들을 사용. 이 메서드들은 지정된 변환이나 조합이 적용된 **새로운 이펙트**(new effects)를 반환.

ZIO 데이터 타입의 메서드는 두 가지 범주(categories):

- **변환(Transformations)**: 변환 함수는 잘 정의된 방식으로 이펙트를 변경 → 런타임 동작(runtime behavior) 커스터마이즈 가능. 예를 들어, 어떤 이펙트에 `effect.timeout(60.seconds)`를 호출하면, 실행될 때 원래 이펙트에 타임아웃(timeout)을 적용하는 새 이펙트 반환
- **조합(Combinations)**: 조합 함수는 둘 이상의 이펙트를 하나의 이펙트로 결합. 예를 들어, `effect1.orElse(effect2)`를 호출하면 두 이펙트가 결합되어, 반환된 이펙트가 실행될 때 먼저 좌측(left hand side)을 실행하고 그것이 실패하면 우측(right hand side)을 실행함 → 주(primary) 이펙트가 실패할 경우의 폴백(fallback) 이펙트 지정 가능

이후 예제는 다음 임포트를 전제로 함.

```scala
import zio._
import zio.Console._
import java.io.IOException
```

---

### 8. 매핑(Mapping)

어떤 값으로 성공하는 이펙트가 있을 때, `ZIO#map`을 사용하면 제공한 함수로 그 값을 변환하는 새로운 이펙트를 얻을 수 있음.

```scala
import zio._

val succeeded: ZIO[Any, Nothing, Int] = ZIO.succeed(21).map(_ * 2)
```

이와 유사하게, `ZIO#mapError` 메서드를 사용하면 하나의 오류를 가진 이펙트를 다른 오류를 가진 이펙트로 변환 가능. 이때 변환을 수행할 함수 제공 필요.

```scala
val failed: ZIO[Any, Exception, Unit] = 
  ZIO.fail("No no!").mapError(msg => new Exception(msg))
```

이펙트의 오류 값이나 성공 값을 매핑하는 것은, 그 이펙트가 실패하는지 성공하는지 **여부 자체**(whether or not)를 바꾸지 않는다는 점에 유의. 이는 Scala의 `Either` 데이터 타입에 대한 매핑이 그 `Either`가 `Left`인지 `Right`인지를 바꾸지 않는 것과 유사.

---

### 9. 연쇄(Chaining)

`flatMap` 메서드를 사용하면 두 이펙트를 순차적으로(sequentially) 실행 가능. `flatMap` 메서드에는 콜백 전달 필요 → 이 콜백은 첫 번째 이펙트의 성공 값을 받아 그 값에 의존하는 두 번째 이펙트를 반환해야 함.

```scala
val sequenced: ZIO[Any, IOException, Unit] =
  Console.readLine.flatMap(input => Console.printLine(s"You entered: $input"))
```

만약 첫 번째 이펙트가 실패하면, `flatMap`에 전달된 콜백은 절대 호출되지 않음 → `flatMap`이 반환한 이펙트 역시 실패.

`flatMap`으로 생성된 **모든** 이펙트 연쇄(chain)에서, 첫 번째 실패는 전체 연쇄를 단락(short-circuit)시킴. 이는 예외를 던지면 일련의 문장(statements) 실행을 조기에 종료하는 것과 마찬가지.

---

### 10. for 컴프리헨션(For Comprehensions)

ZIO 데이터 타입은 `flatMap`과 `map`을 모두 지원 → Scala의 **for 컴프리헨션**(for comprehensions)을 사용해 명령형(imperative) 이펙트 구성 가능.

```scala
val program: ZIO[Any, IOException, Unit] =
  for {
    _    <- Console.printLine("Hello! What is your name?")
    name <- Console.readLine
    _    <- Console.printLine(s"Hello, ${name}, welcome to ZIO!")
  } yield ()
```

for 컴프리헨션은 이펙트 연쇄를 생성하기 위한 절차적 문법(procedural syntax)을 제공 → 대부분의 프로그래머가 ZIO 사용에 가장 빠르게 익숙해지는 방법.

---

### 11. 지핑(Zipping)

`ZIO#zip` 메서드를 사용하면 두 이펙트를 하나의 이펙트로 결합 가능.

이 메서드는 좌측(left) 이펙트를 먼저 실행한 후 우측(right) 이펙트를 실행하고, 두 성공 값을 모두 튜플(tuple)에 담는 이펙트 반환.

```scala
val zipped: ZIO[Any, Nothing, (String, Int)] = 
  ZIO.succeed("4").zip(ZIO.succeed(2))
```

`zip` 연산에서 좌측이나 우측 중 어느 한쪽이라도 실패하면, 튜플을 구성하는 데 **두** 값이 모두 필요하므로 결합된 이펙트도 실패. 좌측이 실패하면 우측은 실행되지 않음.

이펙트의 성공 값이 필요하지 않을 때(예: 값이 `Unit`인 경우)는 `ZIO#zipLeft`나 `ZIO#zipRight`를 사용하는 것이 더 편리. 이 함수들은 `zip`을 수행한 뒤 튜플에서 한쪽 값을 버림(discard).

```scala
val zipRight1: ZIO[Any, IOException, String] =
  Console.printLine("What is your name?").zipRight(Console.readLine)
```

`zipRight`와 `zipLeft` 함수는 각각 `*>`와 `<*`라는 기호 별칭(symbolic aliases)을 가짐. 일부 개발자는 이러한 연산자가 읽기 더 쉽다고 평가.

```scala
val zipRight2: ZIO[Any, IOException, String] =
  Console.printLine("What is your name?") *>
    Console.readLine
```

> **다음 단계**: ZIO 이펙트의 기본 연산에 익숙해졌다면, 다음 단계는 오류 처리(error handling) 학습.

---

### 12. 참고 자료

- [Creating Effects (ZIO 공식 문서)](https://zio.dev/overview/creating-effects)
- [Basic Operations (ZIO 공식 문서)](https://zio.dev/overview/basic-operations)
- [Handling Errors (다음 단계)](https://zio.dev/overview/handling-errors)
- [ZIO Overview](https://zio.dev/overview/)

---

## ZIO 제어 흐름(Control Flow)

> 원본: https://zio.dev/reference/control-flow/

---

### 목차

1. [조건부 연산자(if / when / unless / ifZIO)](#1-조건부-연산자if--when--unless--ifzio)
2. [루프 연산자(loop, loopDiscard, iterate)](#2-루프-연산자loop-loopdiscard-iterate)
3. [foreach를 이용한 반복(Iterating with foreach)](#3-foreach를-이용한-반복iterating-with-foreach)
4. [acquireReleaseWith 기반 try/catch/finally 패턴](#4-acquirereleasewith-기반-trycatchfinally-패턴)
5. [참고 자료](#5-참고-자료)

---

### 1. 조건부 연산자(if / when / unless / ifZIO)

#### 1.1 일반 Scala의 if 표현식

효과를 조건부로 실행하는 가장 단순한 방법 → 일반적인 Scala의 `if-then-else` 표현식 그대로 사용.

```scala
def validateWeightOrFail(weight: Double): ZIO[Any, String, Double] =
  if (weight >= 0)
    ZIO.succeed(weight)
  else
    ZIO.fail(s"negative input: $weight")
```

#### 1.2 when — 조건이 참이면 실행

`ZIO.when`은 "조건이 참이면 실행(execute the effect if the condition is true)"의 의미론적 등가물. 조건과 효과를 받아, 조건이 참이면 `Some`으로 감싼 결과를, 거짓이면 `None`을 산출하는 새 효과를 반환.

```scala
def validateWeightOption(weight: Double): ZIO[Any, Nothing, Option[Double]] =
  ZIO.when(weight >= 0)(ZIO.succeed(weight))
```

조건 자체가 효과적(effectful)일 때는 `whenZIO` 사용.

```scala
val randomIntOption: ZIO[Any, Nothing, Option[Int]] =
  Random.nextInt.whenZIO(Random.nextBoolean)
```

#### 1.3 unless — 조건이 거짓이면 실행

`ZIO.unless`(및 효과적 조건을 위한 `unlessZIO`)는 `when`의 반대 → 조건이 거짓일 때만 효과를 실행.

#### 1.4 ifZIO — 효과적 조건에 따른 분기

`ZIO.ifZIO`는 효과적인(effectful) 불리언 조건에 따라 두 개의 서로 다른 효과 중 하나를 실행.

```scala
val flipTheCoin: ZIO[Any, IOException, Unit] =
  ZIO.ifZIO(Random.nextBoolean)(
    onTrue = Console.printLine("Head"),
    onFalse = Console.printLine("Tail")
  )
```

---

### 2. 루프 연산자(loop, loopDiscard, iterate)

#### 2.1 loop와 loopDiscard

`ZIO.loop`는 초기 상태(initial state)에서 시작해 조건(`cont`)이 참인 동안 증분 함수(`inc`)로 상태를 갱신하며 반복적으로 효과(`body`)를 실행. Scala의 명령형 `while` 루프의 함수형 등가물.

```scala
object ZIO {
  def loop[R, E, A, S](
    initial: => S
  )(cont: S => Boolean, inc: S => S)(body: S => ZIO[R, E, A]):
    ZIO[R, E, List[A]]

  def loopDiscard[R, E, S](
    initial: => S
  )(cont: S => Boolean, inc: S => S)(body: S => ZIO[R, E, Any]):
    ZIO[R, E, Unit]
}
```

- `loop`: 각 반복에서 산출된 결과를 모두 수집하여 `List[A]`로 반환
- `loopDiscard`: 결과를 버리고 `Unit`만 반환

```scala
val r1: ZIO[Any, Nothing, List[Int]] =
  ZIO.loop(1)(_ <= 5, _ + 1)(n => ZIO.succeed(n)).debug
// List(1, 2, 3, 4, 5)

val r4: ZIO[Any, IOException, Unit] =
  ZIO.loopDiscard(1)(_ <= 5, _ + 1) { index =>
    Console.printLine(s"Currently at index $index")
  }.debug
```

#### 2.2 iterate

`ZIO.iterate`는 초기 상태에서 시작해 조건이 참인 동안, 매 반복마다 효과적인(effectful) 연산으로 상태 자체를 변경하며 계속 진행. `loop`와 달리 다음 상태를 계산하는 함수도 효과(`ZIO[R, E, S]`)를 반환.

```scala
object ZIO {
  def iterate[R, E, S](
    initial: => S
  )(cont: S => Boolean)(body: S => ZIO[R, E, S]): ZIO[R, E, S]
}
```

사용자로부터 이름을 계속 입력받다가 "exit"를 입력하면 멈추는 예제.

```scala
def getNames: ZIO[Any, IOException, List[String]] =
  Console.print("Please enter all names") *>
    Console.printLine(" (enter \"exit\" to indicate end of the list):") *>
    ZIO.iterate((List.empty[String], true))(_._2) { case (names, _) =>
      Console.print(s"${names.length + 1}. ") *>
        Console.readLine.map {
          case "exit" => (names, false)
          case name   => (names.appended(name), true)
        }
    }
    .map(_._1)
```

---

### 3. foreach를 이용한 반복(Iterating with foreach)

명시적인 재귀(explicit recursion) 대신, `ZIO.foreach`를 사용하는 고수준(high-level) 접근으로 컬렉션의 각 원소마다 효과를 실행하고 그 결과들을 모을 수 있음.

```scala
Console.printLine("Please enter three names:") *>
  ZIO.foreach(1 to 3) { index =>
    Console.print(s"$index. ") *> Console.readLine
  }.debug
// Vector(John, Jane, Joe)
```

---

### 4. acquireReleaseWith 기반 try/catch/finally 패턴

`ZIO.acquireReleaseWith`는 명령형 프로그래밍의 `try/catch/finally`에 대응하는 함수형 등가물. 세 개의 효과를 받음.

- **acquire** — 리소스를 획득하는 효과
- **release** — 리소스를 해제하는 효과(정리 로직, finalizer)
- **use** — 획득한 리소스를 사용하는 효과

리소스 획득이 성공하면, `use` 효과의 성공·실패·인터럽트 여부와 상관없이 `release`가 항상 실행됨이 보장.

```scala
def wordCount(fileName: String): ZIO[Any, Throwable, Int] = {
  def openFile(name: => String): ZIO[Any, IOException, Source] =
    ZIO.attemptBlockingIO(Source.fromFile(name))
  def closeFile(source: => Source): ZIO[Any, Nothing, Unit] =
    ZIO.succeedBlocking(source.close())
  def wordCount(source: => Source): ZIO[Any, Throwable, Int] =
    ZIO.attemptBlocking(source.getLines().length)

  ZIO.acquireReleaseWith(openFile(fileName))(closeFile(_))(wordCount(_))
}
```

단순화한 예제로 실행 흐름을 확인 가능.

```scala
ZIO.acquireReleaseWith {
  ZIO.succeed("resource").tap(r => ZIO.debug(s"$r acquired"))
} { i =>
  ZIO.debug(s"$i released")
} { i =>
  ZIO.debug(s"start using $i")
}
// Output:
// resource acquired
// start using resource
// resource released
```

acquire → use → release 순서가 보장되며, use 도중 실패나 인터럽트가 발생해도 release는 반드시 실행됨.

---

### 5. 참고 자료

- [Control Flow | ZIO](https://zio.dev/reference/control-flow/)
- [Resource Management | ZIO](https://zio.dev/reference/resource/)

---

## ZIO 내장 서비스(Built-in Services)

> 원본: https://zio.dev/reference/services/

---

### 목차

1. [개요(Overview)](#1-개요overview)
2. [Console](#2-console)
3. [Clock](#3-clock)
4. [Random](#4-random)
5. [System](#5-system)
6. [참고 자료](#6-참고-자료)

---

### 1. 개요(Overview)

ZIO는 네 가지 표준 서비스(standard service)를 기본으로 내장 → `Console`(콘솔 입출력), `Clock`(시간·스케줄링), `Random`(난수 생성), `System`(환경 변수·시스템 프로퍼티). 이 서비스들을 사용할 때는 명시적으로 환경을 제공(provide)할 필요가 없음 → ZIO 런타임이 자동으로 라이브(live) 구현을 공급하기 때문. 테스트 시에는 `zio-test`가 제공하는 `TestClock`·`TestConsole`·`TestRandom`·`TestSystem`으로 손쉽게 대체 가능(9장 테스트 서비스 참고).

---

### 2. Console

`Console` 서비스 → 표준 입출력(standard input/output)과 에러 콘솔에서 문자열을 읽고 쓰는 작업 담당.

주요 메서드:

- `Console.print` — 줄바꿈 없이 출력
- `Console.printLine` — 줄바꿈과 함께 출력
- `Console.printError` / `Console.printLineError` — 에러 스트림에 출력
- `Console.readLine` — 표준 입력에서 한 줄 읽기

```scala
import zio._
import zio.Console._

object MyHelloApp extends ZIOAppDefault {
  val program: ZIO[Any, IOException, Unit] = for {
    _    <- printLine("이름이 무엇인가요?")
    name <- readLine
    _    <- printLine(s"환영합니다, $name!")
  } yield ()

  def run = program
}
```

모든 메서드는 효과적(effectful) → 실제 입출력은 즉시 일어나지 않고, 실행될 때 수행할 읽기·쓰기 작업의 설명(description)만 만들어짐.

---

### 3. Clock

`Clock` 서비스 → 시간(time) 및 스케줄링(scheduling)과 관련된 기능 제공. 논블로킹(non-blocking) 방식으로 동작 → 기본 스레드를 차단하지 않음.

주요 메서드:

- `Clock.currentTime(unit: TimeUnit)` — 지정한 시간 단위(time unit)로 현재 시간을 `Long`으로 반환
- `Clock.currentDateTime` — 현재 타임존의 `OffsetDateTime` 반환
- `ZIO#sleep(duration: Duration)` — 스레드를 블로킹하지 않고 지정한 시간만큼 대기

```scala
import zio._
import java.util.concurrent.TimeUnit

val inMilliseconds: UIO[Long] = Clock.currentTime(TimeUnit.MILLISECONDS)
val inDays: UIO[Long] = Clock.currentTime(TimeUnit.DAYS)

def printTimeForever: ZIO[Any, Throwable, Nothing] =
  Clock.currentDateTime.flatMap(Console.printLine(_)) *>
    ZIO.sleep(1.seconds) *> printTimeForever
```

테스트에서는 `TestClock`으로 실제 시간 경과 없이 시간을 임의로 진행 가능(9.3.2절 참고).

---

### 4. Random

`Random` 서비스 → Scala 표준 라이브러리의 `Random`을 함수형으로 감싸 난수 생성 유틸리티 제공. 생성되는 모든 값은 `URIO[Random, T]` 형태.

주요 메서드:

- `Random.nextInt` / `Random.nextBoolean` / `Random.nextDouble` — 기본 난수 생성
- `Random.nextDoubleBetween` — 지정 범위 내 난수 생성
- `Random.setSeed` — 시드(seed)를 고정하여 재현 가능한(reproducible) 수열 생성(테스트에 유용)
- `Random.shuffle` — 리스트를 무작위로 섞음
- `Random.nextGaussian` — 가우스 분포(Gaussian distribution) 난수 생성

```scala
for {
  randomInt <- Random.nextInt
  _         <- Console.printLine(s"A random Int: $randomInt")
} yield ()
```

시드를 고정하면 동일한 수열이 재현됨.

```scala
for {
  _        <- Random.setSeed(0)
  nextInts <- Random.nextInt zip Random.nextInt
} yield assert(nextInts == (-1155484576, -723955400))
```

> 주의: `Random`이 생성하는 난수는 암호학적으로 안전(cryptographically secure)하지 않음 → 비밀번호·토큰 생성 등 보안이 필요한 영역에는 부적합.

---

### 5. System

`System` 서비스 → 시스템 환경 변수(environment variable, OS 수준의 전역 변수)와 시스템 프로퍼티(system property, 애플리케이션 수준의 변수)에 접근하는 유용한 함수 제공.

주요 메서드:

- `System.env(name)` — 환경 변수를 `Option[String]`으로 조회
- `System.property(name)` — 시스템 프로퍼티를 `Option[String]`으로 조회
- `System.lineSeparator` — 운영체제별 줄 구분자(line separator) 반환

```scala
import zio._

for {
  user <- System.env("USER")
  _ <- user match {
    case Some(value) => Console.printLine(s"USER: $value")
    case None        => Console.printLine("USER 환경변수 미설정")
  }
} yield ()

for {
  logLevel <- System.property("LOG_LEVEL")
  _ <- logLevel match {
    case Some(value) => Console.printLine(s"LOG_LEVEL: $value")
    case None        => Console.printLine("LOG_LEVEL 속성 미설정")
  }
} yield ()
```

값의 존재 여부를 `Option`으로 다루므로, 미설정된 환경 변수·프로퍼티도 안전하게 처리 가능. 더 구조화된 설정 관리가 필요할 때는 4장 의존성 주입 문서의 `zio.Config`/`ConfigProvider` 사용 권장.

---

### 6. 참고 자료

- [Built-in Services | ZIO](https://zio.dev/reference/services/)
- [Console | ZIO](https://zio.dev/reference/services/console)
- [Clock | ZIO](https://zio.dev/reference/services/clock)
- [Random | ZIO](https://zio.dev/reference/services/random)
- [System | ZIO](https://zio.dev/reference/services/system)
