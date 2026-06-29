# ZIO 소개와 시작하기

> 원본: https://zio.dev/overview/getting-started

---

## 목차

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

## 1. ZIO란 무엇인가(What is ZIO)

ZIO는 Scala를 위한 타입 안전(type-safe)하고 합성 가능한(composable) 비동기(asynchronous)·동시성(concurrent) 프로그래밍 프레임워크입니다. 공식 요약(Summary) 문서는 ZIO를 "JVM 상에서 클라우드 네이티브(cloud-native) 애플리케이션을 구축하기 위한 차세대 프레임워크(next-generation framework)"이자, "초심자 친화적이면서도 강력한 함수형 코어(beginner-friendly yet powerful functional core)"를 갖춘 도구로 소개합니다.

ZIO로 작성한 애플리케이션은 다음과 같은 특성을 지닐 수 있습니다.

- 높은 확장성(highly scalable)
- 테스트 용이성(testable)
- 견고함(robust)
- 복원력(resilient)
- 자원 안전성(resource-safe)
- 효율성(efficient)
- 관찰 가능성(observable)

ZIO는 개발자가 **참조 투명성(referential transparency)**, **합성 가능한 데이터 타입(composable data types)**, **타입 안전성(type-safety)** 등을 활용하여 동시성·복원력 있는 애플리케이션을 Scala로 구축할 수 있게 해 줍니다.

---

## 2. 왜 ZIO를 쓰는가 — 핵심 장점(Why ZIO)

ZIO 홈페이지는 다음과 같은 핵심 기능과 장점(features & benefits)을 강조합니다.

- **고성능(High-performance)**: 런타임 오버헤드(runtime overhead)를 최소화하면서 확장 가능한 애플리케이션을 구축합니다.
- **타입 안전(Type-safe)**: Scala 컴파일러의 능력을 활용하여 버그를 컴파일 시점(compile time)에 잡아냅니다.
- **동시성(Concurrent)**: 교착 상태(deadlock), 경쟁 조건(race condition), 또는 추가적인 복잡성 없이 동시성 애플리케이션을 작성합니다.
- **비동기(Asynchronous)**: 연산이 비동기든 동기든 동일한 모습의 순차적 코드(sequential code)를 작성할 수 있습니다.
- **자원 안전(Resource-safe)**: 장애(failure) 상황에서도 스레드(thread)를 포함한 자원을 절대 누수(leak)하지 않는 애플리케이션을 개발합니다.
- **테스트 용이(Testable)**: 테스트용 서비스를 주입(inject)하여 빠르고, 결정적(deterministic)이며, 타입 안전한 테스트를 작성합니다.
- **복원력(Resilient)**: 오류를 보존하고 장애에 유연하게 대응하는 애플리케이션을 만듭니다.
- **함수형(Functional)**: 단순하고 재사용 가능한 빌딩 블록(building block)으로부터 정교한 해법을 합성(compose)합니다.

---

## 3. ZIO 데이터 타입과 타입 별칭(The ZIO Data Type)

ZIO의 토대에는 **함수형 효과(functional effect)**라 불리는 `ZIO` 데이터 타입이 있습니다. 이는 "모든 ZIO 애플리케이션의 근본적인 빌딩 블록(the fundamental building block for every ZIO application)"으로, **하나의 연산 단위(a unit of computation)**를 표현합니다. 실행 시 오류로 실패(fail)하거나 값으로 성공(succeed)하는 연산 또는 상호작용을 정밀하게 기술하는 "계획(plan)" 역할을 합니다.

### 3.1 세 가지 타입 매개변수(Type Parameters)

`ZIO[R, E, A]` 데이터 타입은 세 개의 타입 매개변수(type parameter)를 사용합니다.

- **R (환경 타입, Environment Type)**: 실행 전에 필요한 문맥 데이터(contextual data)를 나타냅니다. 예: 데이터베이스 연결(database connection), HTTP 요청(HTTP request).
- **E (실패 타입, Failure Type)**: 해당 효과가 실패할 수 있는 오류 타입을 나타냅니다.
- **A (성공 타입, Success Type)**: 실행 시 성공하여 산출되는 값의 타입을 나타냅니다.

### 3.2 타입 별칭(Type Aliases)

ZIO는 자주 쓰이는 시나리오를 위해 단순화된 타입 별칭(type alias)을 제공합니다.

- `UIO[A]` — 요구사항(R)이 없고, 실패할 수 없는 효과
- `Task[A]` — 요구사항이 없고, `Throwable`로 실패할 수 있는 효과
- `RIO[R, A]` — R을 요구하며, `Throwable`로 실패할 수 있는 효과
- `IO[E, A]` — 요구사항이 없고, 특정 오류 타입(specific error type)으로 실패할 수 있는 효과
- `URIO[R, A]` — R을 요구하며, 실패할 수 없는 효과

> 함수형 효과(functional effect)에 처음 입문하는 사용자는 `Task`부터 시작하는 것이 좋습니다.

---

## 4. 의존성 추가와 설치(Installation)

ZIO를 사용하려면 프로젝트의 `build.sbt` 파일에 의존성을 추가합니다.

```scala
libraryDependencies += "dev.zio" %% "zio" % "2.1.26"
libraryDependencies += "dev.zio" %% "zio-streams" % "2.1.26"
```

- 첫 번째 줄은 ZIO 코어 라이브러리를 추가합니다.
- 두 번째 줄(`zio-streams`)은 합성 가능한 스트리밍(streaming) 기능을 사용할 때 추가합니다(필요 없다면 생략 가능).

---

## 5. 첫 ZIO 애플리케이션(ZIOAppDefault)

ZIO 애플리케이션을 작성하는 가장 간단한 방법은 `ZIOAppDefault`를 확장(extend)하는 것입니다. `ZIOAppDefault`는 완전한 런타임 시스템(runtime system)을 내장하므로 별도로 런타임을 구성할 필요가 없습니다.

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

### `run` 메서드와 예외 없는 효과(Unexceptional Effect)

`run` 메서드는 **모든 오류가 처리된(all its errors handled)** ZIO 값을 반환해야 합니다. ZIO 용어로는 "예외 없는 ZIO 값(an unexceptional ZIO value)"이라고 합니다.

오류 처리는 일반적으로 `fold`를 사용합니다. `fold`는 두 개의 함수를 받는데, 하나는 실패(failure)를 처리하고 다른 하나는 성공(success)을 처리합니다. 최종적으로 `Int` 결과를 산출하며, 관례상 **실패는 1, 성공은 0**입니다.

---

## 6. Console 상호작용(Console Interaction)

ZIO는 콘솔(console) 입출력을 위한 연산을 제공합니다. 주요 함수는 다음과 같습니다.

- `Console.print()` — 줄바꿈 없이 출력합니다.
- `Console.printLine()` — 줄바꿈과 함께 출력합니다.
- `Console.readLine` — 사용자 입력을 한 줄 읽어 들입니다.

다음은 입력받은 줄을 그대로 다시 출력하는 에코(echo) 프로그램 예제입니다.

```scala
val echo = Console.readLine.flatMap(line => Console.printLine(line))
```

`readLine`이 읽어 들인 한 줄(`line`)을 `flatMap`으로 받아 `printLine`에 전달하여, 입력을 그대로 화면에 다시 출력합니다.

---

## 7. 기존 애플리케이션에 통합하기(Runtime)

`main` 함수를 직접 제어할 수 없어 `ZIOAppDefault`를 확장하기 어려운 기존 애플리케이션에 ZIO를 통합해야 할 때는 런타임(runtime)을 직접 생성하여 효과를 실행할 수 있습니다.

```scala
val runtime = Runtime.default
Unsafe.unsafe { implicit unsafe =>
  runtime.unsafe.run(ZIO.attempt(println("Hello World!"))).getOrThrowFiberFailure()
}
```

> **주의(Important):** 이상적으로 애플리케이션은 **단 하나의 런타임(a single runtime)**만 가져야 합니다. 각 런타임은 자체 자원(스레드 풀(thread pool)과 처리되지 않은 오류 리포터(unhandled error reporter) 포함)을 보유하기 때문입니다.

`Unsafe.unsafe { ... }` 블록은 명시적으로 안전하지 않은(unsafe) 실행 영역임을 표시하며, `getOrThrowFiberFailure()`는 파이버(fiber)의 실패를 예외로 던져 결과를 가져옵니다.

---

## 8. 지원 플랫폼(Platforms)

ZIO는 세 가지 주요 플랫폼을 지원하며 플랫폼 간 일관된 인터페이스를 제공하는 것을 목표로 합니다. 다만 불가피한 플랫폼별 차이(platform-specific differences)가 있으므로 개발자가 이를 숙지해야 합니다.

### 8.1 JVM

- Java 버전 **11 이상**과 Scala 버전 **2.12, 2.13, 3.x**를 지원합니다.
- `ZIO.blocking`, `ZIO.attemptBlocking` 같은 메서드를 통해 블로킹 연산(blocking operation)을 수행할 수 있습니다.

### 8.2 Scala.js

- **Scala.js 1.0**과 호환됩니다.
- Scala.js에는 `java.time` 구현이 없으므로 개발자가 직접 `java.time` 의존성을 제공해야 합니다. 권장 라이브러리는 **scala-java-time (버전 2.2.0)**입니다.
- 주요 제약 사항:
  - JavaScript의 단일 스레드(single-threaded) 특성으로 인해 **블로킹 연산은 Scala.js에서 지원되지 않습니다**.
  - `Console` 서비스의 `readLine` 메서드는 지원되지 않습니다.
  - `Runtime`의 동기 실행(synchronous execution) 메서드는 비동기 효과(asynchronous effect)에 대해 안전하지 않습니다(unsafe).

### 8.3 Scala Native

- Scala Native 지원은 현재 **실험적(experimental)** 단계입니다. 플랫폼이 성숙해지면 추가 세부 사항이 제공될 예정입니다.

---

## 9. 성능 특징(Performance)

ZIO는 **논블로킹 파이버(non-blocking fiber)**로 구동되는 고성능 프레임워크(high-performance framework)입니다. 향후 Project Loom 하에서 이 파이버는 **가상 스레드(virtual threads)**로 이행할 예정입니다.

주요 성능 특성은 다음과 같습니다.

- **실행 엔진(Execution Engine)**: "ZIO의 핵심 실행 엔진은 할당(allocation)을 최소화하고 사용되지 않는 연산(unused computation)을 자동으로 취소(cancel)합니다."
- **자료 구조(Data Structures)**: "ZIO에 포함된 모든 자료 구조는 고성능이며 논블로킹이고, JVM 상에서 가능한 한 최대로 박싱하지 않습니다(non-boxing)."
- **벤치마크 결과(Benchmarking Results)**: `benchmarks` 프로젝트는 ZIO를 Scala 및 Java 생태계의 유사 프로젝트들과 비교하는 다양한 벤치마크를 담고 있으며, 일부 경우 **2~100배 빠른 성능(2-100x faster performance)**을 보여 줍니다.
- **추가 비교(Additional Comparisons)**: HTTP, GraphQL, RDBMS 등 ZIO 통합 항목의 성능 비교는 각각의 해당 프로젝트에서 찾을 수 있습니다.

ZIO는 메모리 할당 최소화, 사용되지 않는 연산의 자동 정리, 그리고 유사 프레임워크 대비 성능 우위를 통해 효율성을 달성합니다.

---

## 10. ZIO 생태계 라이브러리(Ecosystem)

ZIO 홈페이지에서 소개하는 주요 생태계 라이브러리는 다음과 같습니다.

- **ZIO HTTP**: 타입 안전한 엔드포인트(type-safe endpoints)와 WebSocket을 지원하는 고성능 웹 라이브러리.
- **ZIO Streams**: 무한 데이터(infinite data)를 위한 합성 가능한 스트리밍과 자동 배압(automatic backpressure) 처리.
- **ZIO Test**: 동시성 코드 검증을 지원하는 속성 기반 테스트(property-based testing) 프레임워크.
- **ZIO STM**: 원자적(atomic)이고 합성 가능한 연산을 위한 소프트웨어 트랜잭셔널 메모리(Software Transactional Memory).
- **ZIO Schema**: 자동 파생(automatic derivation)을 지원하는 선언적 스키마(declarative schema) 정의.
- **ZIO Config**: 검증(validation)을 포함한 타입 안전 설정 관리(configuration management).
- **ZIO Logging**: 문맥 지원(contextual support)을 갖춘 구조적 로깅(structured logging).

> 홈페이지에서는 ZIO 기초부터 효과 시스템(effect systems), 오류 처리(error handling), 의존성 주입(dependency injection) 패턴 같은 고급 주제까지 다루는 무료 가이드 **"ZIONOMICON"**도 함께 안내합니다.

---

## 11. 참고 자료

- [ZIO 공식 홈페이지](https://zio.dev/)
- [Overview: Getting Started](https://zio.dev/overview/getting-started)
- [Overview: Summary](https://zio.dev/overview/summary)
- [Overview: Platforms](https://zio.dev/overview/platforms)
- [Overview: Performance](https://zio.dev/overview/performance)
