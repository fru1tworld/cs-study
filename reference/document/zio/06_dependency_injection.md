# ZIO 의존성 주입: ZEnvironment와 ZLayer

> 원본: https://zio.dev/reference/di/

---

## 목차

1. [의존성 주입 개요(Dependency Injection Overview)](#1-의존성-주입-개요dependency-injection-overview)
2. [의존성 주입의 동기(Motivation)](#2-의존성-주입의-동기motivation)
3. [컨텍스트 데이터 타입과 환경(Contextual Data Types & Environment)](#3-컨텍스트-데이터-타입과-환경contextual-data-types--environment)
4. [ZEnvironment](#4-zenvironment)
5. [ZLayer](#5-zlayer)
6. [ZLayer 타입 별칭(Type Aliases)](#6-zlayer-타입-별칭type-aliases)
7. [ZLayer 생성 방법(Creating Layers)](#7-zlayer-생성-방법creating-layers)
8. [ZLayer 구성: 수직·수평 합성(Vertical & Horizontal Composition)](#8-zlayer-구성-수직수평-합성vertical--horizontal-composition)
9. [의존성 전파: provide, provideLayer, provideSome(Dependency Propagation)](#9-의존성-전파-provide-providelayer-providesomedependency-propagation)
10. [의존성 그래프 구축: 수동 vs 자동(Building the Dependency Graph)](#10-의존성-그래프-구축-수동-vs-자동building-the-dependency-graph)
11. [자동 레이어 구성(Automatic Layer Construction)](#11-자동-레이어-구성automatic-layer-construction)
12. [의존성 메모이제이션(Dependency Memoization)](#12-의존성-메모이제이션dependency-memoization)
13. [의존성 그래프 재정의(Overriding the Dependency Graph)](#13-의존성-그래프-재정의overriding-the-dependency-graph)
14. [참고 자료](#14-참고-자료)

---

## 1. 의존성 주입 개요(Dependency Injection Overview)

### 1.1 의존성이란 무엇인가(What is a Dependency)

**의존성(dependency)** 은 어떤 서비스(service)가 자신의 기능을 수행하기 위해 필요로 하는 또 다른 서비스를 의미합니다. 예를 들어 `Editor` 서비스는 코드를 포맷팅(formatting)하고 컴파일(compile)하기 위해 `Formatter`와 `Compiler` 서비스를 필요로 합니다.

### 1.2 의존성 주입이란 무엇인가(What is Dependency Injection)

**의존성 주입(dependency injection, DI)** 은 "의존성의 사용(usage)을 그 실제 생성 과정(creation process)으로부터 분리(decoupling)하는 패턴"입니다. 서비스가 자신의 의존성을 직접 생성하는 대신, 외부 소스(external source)로부터 그것을 전달받습니다.

**DI 없이 작성한 경우** — 서비스가 의존성을 내부에서 직접 생성합니다.

```scala
class Editor {
  private val formatter = new Formatter
  private val compiler = new Compiler
  def formatAndCompile(code: String): UIO[String] =
    formatter.format(code).flatMap(compiler.compile)
}
```

**생성자 기반 DI(Constructor-based DI)를 사용한 경우** — 의존성을 생성자 파라미터로 전달받습니다.

```scala
class Editor(formatter: Formatter, compiler: Compiler) {
  def formatAndCompile(code: String): UIO[String] = ???
}
```

### 1.3 ZIO에 내장된 의존성 주입(ZIO's Built-in Dependency Injection)

ZIO는 다음 세 가지 구성 요소를 통해 통합된(integrated) 의존성 주입을 제공합니다.

1. **ZIO 환경(ZIO Environment)** — `ZIO.serviceXYZ`로 서비스에 접근(access)하고, `ZIO.provideXYZ`로 서비스를 주입(inject)합니다.
2. **ZLayer** — 의존성 그래프(dependency graph)를 생성합니다.
3. **3단계 프로세스(three-step process)** — 서비스 접근(Access services) → 의존성 그래프 구축(Build dependency graph) → 서비스 제공(Provide services).

### 1.4 ZIO 의존성 주입의 특징(Features)

ZIO의 의존성 주입은 다음 다섯 가지 핵심 역량을 제공합니다.

- **합성 가능(Composable)** — 환경(Environment)과 의존성(Dependencies)을 자유롭게 합성할 수 있습니다.
- **타입 안전(Type-Safe)** — 의존성의 충족 여부를 컴파일 타임(compile-time)에 검증합니다.
- **효과적(Effectful)** — 의존성 그래프 구축 과정 자체를 이펙트(effect)로 다룰 수 있습니다.
- **자원 관리(Resourceful)** — 생성(creation)과 해제(release) 단계를 안전하게 관리합니다.
- **병렬성(Parallelism)** — 의존성을 병렬로 생성할 수 있습니다.

### 1.5 다른 프레임워크(Other Frameworks)

ZIO 외의 대표적인 DI 솔루션으로는 다음이 있습니다.

- Guice
- izumi distage
- MacWire

---

## 2. 의존성 주입의 동기(Motivation)

### 2.1 강한 결합의 문제점(Problems with Tight Coupling)

서비스가 자신의 의존성을 직접 인스턴스화(instantiate)할 때 발생하는 네 가지 핵심 문제는 다음과 같습니다.

1. **제어권 상실(Loss of Control)** — "`Editor` 서비스의 사용자는 의존성이 어떻게 생성될지에 대한 어떤 제어권도 갖지 못합니다."
2. **테스트의 어려움(Testing Difficulties)** — 의존성이 내부에서 생성되면 목(mock) 구현을 사용하기 어렵습니다.
3. **강한 결합(Tight Coupling)** — 의존성 서비스가 변경되면 그것을 사용하는 코드 전반에 변경이 강제됩니다.
4. **수동 그래프 관리(Manual Graph Management)** — 복잡도가 커질수록 객체 생성(object creation)이 번거로워집니다.

### 2.2 6단계 진화(Six-Step Evolution)

위 문제들은 다음 6단계를 거쳐 점진적으로 해결됩니다.

**1단계: 제어의 역전(Inversion of Control)** — 의존성을 내부에서 생성하는 대신 생성자 파라미터로 전달합니다.

```scala
class Editor(formatter: Formatter, compiler: Compiler) {
  def formatAndCompile(code: String): UIO[String] =
    formatter.format(code).flatMap(compiler.compile)
}
```

**2단계: 인터페이스를 통한 분리(Decoupling via Interfaces)** — 구체 클래스(concrete class)가 아닌 트레잇(trait)에 대해 프로그래밍하여, 테스트 시 구현 교체를 용이하게 합니다.

**3단계: 중앙 집중식 매핑(Centralized Mapping)** — `ZEnvironment`를 타입 레벨 맵(type-level map)으로 사용하여 인터페이스와 구현을 바인딩(bind)하고, 코드 전반의 수동 배선(manual wiring)을 제거합니다.

**4단계: 효과적인 생성 처리(Handling Effectful Construction)** — 생성 과정에 부수 효과(side effect, 예: `Ref.make`)가 수반되는 의존성을 `ZLayer`로 관리합니다. 동시(concurrent)·자원 안전(resource-safe) 초기화를 지원합니다.

**5단계: 선언적 의존성(Declarative Dependencies)** — `R` 타입 파라미터를 사용해 필요 사항을 명령적으로 "요청(asking)"하는 대신 "선언(declaring)"하는 방식으로 전환합니다.

**6단계: 자동 그래프 생성(Automatic Graph Generation)** — 매크로(macro) 지원을 받는 `provide` 메서드가 의존성 그래프를 자동으로 구성하여 수동 합성 보일러플레이트(boilerplate)를 제거합니다.

---

## 3. 컨텍스트 데이터 타입과 환경(Contextual Data Types & Environment)

ZIO는 실행 중인 이펙트의 환경(environment)을 `ZIO[R, E, A]`의 첫 번째 타입 파라미터 `R`로 인코딩(encode)합니다. `R`은 이펙트를 실행하기 위해 필요한 환경적 컨텍스트(environmental context)를 나타냅니다.

"모든 이펙트는 환경(environment)이라 불리는 특정 컨텍스트 안에서 동작할 수 있습니다." `R` 타입 파라미터는 이펙트가 실행 가능해지기 전에 충족되어야 하는 의존성을 가리킵니다.

ZIO가 제공하는 세 가지 주요 컨텍스트 추상화(contextual abstraction)는 다음과 같습니다.

1. **ZIO 환경(ZIO Environment)** — `ZIO[R, E, A]`의 `R` 타입 파라미터입니다.
2. **ZEnvironment** — 이펙트의 환경을 유지(maintain)하는 내장 타입 레벨 맵(built-in type-level map)입니다.
3. **ZLayer** — "값 `RIn`에서 출발하여 타입 `ROut`의 환경을 구축하기 위한 레시피(recipe)"입니다.

### 3.1 서비스에 접근하기(Accessing Services)

환경과 상호작용하기 위한 두 가지 접근자(accessor) 범주가 있습니다.

- **서비스 접근자(Service Accessor)** — `ZIO.service`로 환경에서 특정 서비스를 가져옵니다.
- **서비스 멤버 접근자(Service Member Accessors)** — `ZIO.serviceWith`, `ZIO.serviceWithZIO`로 반환 타입(return type)에 기반하여 서비스의 기능에 접근합니다.

### 3.2 환경 활용의 장점(Key Advantages)

ZIO의 환경 기능을 사용하면 다음이 가능해집니다.

- 구현(implementation)이 아닌 인터페이스(interface)에 대해 코딩
- 서비스 모킹(mocking)을 통한 테스트 가능한 이펙트 작성
- 강력한 타입 추론(type inference)과 함께 서비스를 합성

---

## 4. ZEnvironment

### 4.1 ZEnvironment란 무엇인가

`ZEnvironment[R]`는 ZIO 이펙트의 환경을 유지하는 내장 타입 레벨 맵입니다. 환경적 서비스와 그 구현을 저장하며, ZIO 생태계 안에서 의존성 주입을 가능하게 합니다.

ZIO와 ZEnvironment의 관계는 개념적으로 다음과 같이 표현할 수 있습니다.

```scala
type ZIO[R, E, A] = ZEnvironment[R] => Either[E, A]
```

즉, ZIO 이펙트는 개념적으로 환경(environment)에서 결과(outcome)로 가는 함수입니다.

### 4.2 ZEnvironment 생성하기

**빈 환경(Empty environment):**

```scala
val empty: ZEnvironment[Any] = ZEnvironment.empty
```

**값으로부터 생성(From a value):**

```scala
case class AppConfig(host: String, port: Int)
val config: ZEnvironment[AppConfig] =
  ZEnvironment(AppConfig("localhost", 8080))
```

**내장 서비스로 생성(With built-in services):**

```scala
val environment: ZEnvironment[Console & Clock & Random & System] =
  ZEnvironment[Console, Clock, Random, System](
    Console.ConsoleLive,
    Clock.ClockLive,
    Random.RandomLive,
    System.SystemLive
  )
```

### 4.3 핵심 연산(Core Operations)

**환경 결합(Combining environments):**

```scala
val app: ZEnvironment[AppConfig] =
  ZEnvironment.empty ++ ZEnvironment(AppConfig("localhost", 8080))
```

**서비스 추가(Adding a service):**

```scala
val app: ZEnvironment[AppConfig] =
  ZEnvironment.empty.add(AppConfig("localhost", 8080))
```

**서비스 조회(Retrieving a service):**

```scala
val appConfig: AppConfig = app.get[AppConfig]
```

### 4.4 환경 접근과 제공(Accessing and Providing Environments)

**전체 환경에 접근(Access entire environment):**

```scala
val myApp: ZIO[AppConfig, IOException, Unit] =
  ZIO.environment[AppConfig].flatMap { env =>
    val config = env.get[AppConfig]
    Console.printLine(s"Application started with config: $config")
  }
```

**환경을 제공하여 의존성 제거(Provide environment to eliminate it):**

```scala
val eliminated: IO[IOException, Unit] =
  myApp.provideEnvironment(
    ZEnvironment(AppConfig(poolSize = 10))
  )
```

### 4.5 동일 타입 다중 인스턴스 패턴(Multiple Instances Pattern)

같은 타입의 서비스를 여러 개 다루어야 할 때는 `Map[K, A]`를 사용합니다.

```scala
val database: URIO[Map[String, Database], Option[Database]] =
  ZIO.serviceAt[Database]("inmemory")
```

**다중 설정(Multiple configs) 예제:**

```scala
object AppConfig {
  val layer: ULayer[Map[String, AppConfig]] =
    ZLayer.succeedEnvironment(
      ZEnvironment(
        Map(
          "prod" -> AppConfig("production.myapp", 80),
          "dev" -> AppConfig("development.myapp", 8080)
        )
      )
    )
}
```

타입 레벨 맵 구조는 내부적으로 서비스와 그 구현을 표현하며, ZIO 애플리케이션 전반에 걸쳐 타입 안전한(type-safe) 의존성 관리를 지원합니다.

---

## 5. ZLayer

### 5.1 ZLayer란 무엇인가

`ZLayer[-RIn, +E, +ROut]`는 "애플리케이션의 한 레이어(layer)"를 나타냅니다. 모든 레이어는 입력(input)으로 일부 서비스 `RIn`을 필요로 하고, 출력(output)으로 일부 서비스 `ROut`을 생산합니다. 개념적으로는 비동기 함수(asynchronous function)와 유사합니다.

```text
RIn => async Either[E, ROut]
```

### 5.2 타입 파라미터(Type Parameters)

- **RIn** — 필요한 입력 의존성(input dependencies)
- **E** — 에러 타입(error type)
- **ROut** — 출력 서비스 타입(output service type)

레이어들은 뛰어난 합성 속성(composition properties)을 가지므로, ZIO에서 다른 서비스에 의존하는 서비스를 생성하는 관용적(idiomatic)인 방법으로 사용됩니다.

---

## 6. ZLayer 타입 별칭(Type Aliases)

`ZLayer[RIn, E, ROut]`의 흔한 형태를 간결하게 표현하기 위해 다음 타입 별칭(type alias)들이 제공됩니다.

| 별칭 | 정의 | 의미 |
| --- | --- | --- |
| `RLayer[-RIn, +ROut]` | `ZLayer[RIn, Throwable, ROut]` | `RIn`을 입력으로 요구하고, `Throwable`로 실패할 수 있으며, `ROut`을 출력합니다. |
| `URLayer[-RIn, +ROut]` | `ZLayer[RIn, Nothing, ROut]` | `RIn`을 입력으로 요구하고, 실패할 수 없으며, `ROut`을 출력합니다. |
| `Layer[+E, +ROut]` | `ZLayer[Any, E, ROut]` | 입력 서비스를 요구하지 않고, `E`로 실패할 수 있으며, `ROut`을 출력합니다. |
| `ULayer[+ROut]` | `ZLayer[Any, Nothing, ROut]` | 입력 서비스를 요구하지 않고, 실패할 수 없으며, `ROut`을 출력합니다. |
| `TaskLayer[+ROut]` | `ZLayer[Any, Throwable, ROut]` | 입력 서비스를 요구하지 않고, `Throwable`로 실패할 수 있으며, `ROut`을 출력합니다. |

- `R`(Required) 접두사: 입력 의존성 `RIn`을 요구함을 나타냅니다.
- `U`(Unfailing) 접두사: 에러 타입이 `Nothing`이어서 실패할 수 없음을 나타냅니다.
- `Task` 접두사: 에러 타입이 `Throwable`임을 나타냅니다.

---

## 7. ZLayer 생성 방법(Creating Layers)

ZLayer를 만드는 주요 생성 방법은 다음 네 가지입니다.

### 7.1 ZLayer.succeed — 값이나 기존 서비스로부터

단순한 값이나 이미 존재하는 서비스로부터 레이어를 만듭니다.

```scala
def succeed[A: Tag](a: A): ULayer[A]
```

```scala
// 값으로부터
val configLayer: ULayer[AppConfig] =
  ZLayer.succeed(AppConfig("localhost", 8080))
```

### 7.2 ZLayer.fromZIO / ZLayer.apply — 비자원적 이펙트로부터

자원 해제가 필요 없는(non-resourceful) 이펙트로부터 for-컴프리헨션(for-comprehension)을 사용해 레이어를 만듭니다. `ZLayer { ... }`는 `ZLayer.apply`의 축약입니다.

```scala
val layer: ZLayer[A & B, Nothing, C] = ZLayer {
  for {
    a <- ZIO.service[A]
    b <- ZIO.service[B]
  } yield CLive(a, b)
}
```

### 7.3 ZLayer.fromFunction — 함수로부터

함수를 레이어로 변환합니다.

```scala
val layer: ZLayer[A & B, Nothing, C] =
  ZLayer.fromFunction(CLive.apply _)
```

### 7.4 ZLayer.scoped — 자원적 이펙트로부터

획득(acquisition)과 해제(release)가 필요한 자원적(resourceful) 이펙트로부터 레이어를 만듭니다.

```scala
val database: ZLayer[Any, Throwable, Database] =
  ZLayer.scoped {
    ZIO.acquireRelease {
      Database.connect.debug("connecting to the database")
    } { database =>
      database.close
    }
  }
```

### 7.5 기타 연산자(Other Operators)

ZLayer는 다음과 같은 부가 연산도 제공합니다.

- **orElse** — 레이어 구축이 실패할 경우 대체 레이어로 폴백(fallback)합니다.
- **retry** — `Schedule`을 사용해 구축을 재시도(retry)합니다.
- **project** — 레이어의 일부분을 추출(extract)합니다.
- **tap / tapError** — 성공/실패 시 이펙트를 수행합니다.
- **build** — 레이어를 스코프(scoped) ZIO로 변환합니다.
- **launch** — 레이어를 ZIO 애플리케이션으로 변환합니다.

---

## 8. ZLayer 구성: 수직·수평 합성(Vertical & Horizontal Composition)

레이어는 두 가지 방식으로 합성(compose)됩니다.

### 8.1 수직 합성(Vertical Composition) — `>>>`

`>>>` 연산자는 레이어들을 순차적으로(sequentially) 연결합니다. 한 레이어의 출력(output)이 다음 레이어의 입력(input)이 됩니다.

```scala
// DatabaseConfig의 출력이 Database의 입력으로 전달됨
DatabaseConfig.live >>> Database.live
```

### 8.2 수평 합성(Horizontal Composition) — `++`

`++` 연산자는 서로의 출력에 의존하지 않는(independent) 레이어들을 나란히(side by side) 결합합니다. 결과 레이어의 입력은 두 레이어 입력의 합집합(union), 출력은 두 레이어 출력의 합집합이 됩니다.

```scala
val app: ZEnvironment[AppConfig] =
  ZEnvironment.empty ++ ZEnvironment(AppConfig("localhost", 8080))
```

### 8.3 수직 합성 + 좌측 출력 유지 — `>+>`

`>+>` 연산자는 수직 합성을 수행하되, 왼쪽 레이어의 출력도 함께 결과에 유지합니다. 즉, `a >+> b`는 개념적으로 `a >>> (a ++ b)`와 유사하게 왼쪽과 오른쪽 출력을 모두 제공합니다.

### 8.4 합성 예제(Composition Example)

복잡한 의존성 그래프를 수동(manual)으로 구성한 예제입니다.

```scala
ZIO
  .serviceWithZIO[App](_.execute)
  .provide(
    (((DatabaseConfig.live >>> Database.live) ++ Analytics.live >>> Users.live)
     ++ Analytics.live) >>> App.live
  )
```

또 다른 수동 구성 예제 — `DocRepo`와 `UserRepo`를 함께 제공합니다.

```scala
val appLayer: URLayer[Any, DocRepo with UserRepo] =
  ((Logging.live ++ Database.live ++ (Logging.live >>> BlobStorage.live)) >>> DocRepo.live) ++
    ((Logging.live ++ Database.live) >>> UserRepo.live)
```

---

## 9. 의존성 전파: provide, provideLayer, provideSome(Dependency Propagation)

ZIO는 환경 타입 파라미터 `R`을 통해 의존성 전파(dependency propagation)를 처리합니다. "구현을 제공하고, 애플리케이션 전체에 걸쳐 모든 의존성을 공급(feed)하고 전파(propagate)할 방법이 필요합니다."

### 9.1 ZIO#provideEnvironment

`ZEnvironment[R]` 인스턴스를 직접 받아 환경 의존성을 제거합니다.

```scala
trait ZIO[-R, +E, +A] {
  def provideEnvironment(r: => ZEnvironment[R]): IO[E, A]
}
```

### 9.2 ZIO#provide

원시 환경(raw environment) 대신 `ZLayer` 인스턴스들을 받습니다. 모든 의존성이 충족되면 `R`이 `Any`로 소거됩니다.

```scala
val mainEffect: ZIO[Any, Nothing, Unit] =
  myApp.provide(FooLive.layer, BarLive.layer)
```

위 예제는 `ZIO[Foo & Bar, Nothing, Unit]`을 `ZIO[Any, Nothing, Unit]`으로 변환합니다.

### 9.3 ZIO#provideSome

레이어를 부분적으로(partially) 적용하여 일부 요구 사항을 미충족 상태로 남겨 둡니다.

```scala
val mainEffectSome: ZIO[Bar, Nothing, Unit] =
  myApp.provideSome(FooLive.layer)
```

`Foo`만 제공하고 `Bar`는 남겨 두므로 결과 타입은 `ZIO[Bar, Nothing, Unit]`이 됩니다.

### 9.4 ZIO#provideLayer / ZIO#provideSomeLayer

이미 합성된 단일 레이어를 제공할 때는 `provideLayer`를, 일부만 제공할 때는 `provideSomeLayer[남길타입]`을 사용합니다. (`provideSomeLayer` 사용 예는 [13장](#13-의존성-그래프-재정의overriding-the-dependency-graph) 참고)

### 9.5 ZIO#provideSomeAuto

Scala 3에서 제공되는 향상 기능으로, 명시적 타입 파라미터 없이 남은 타입 요구 사항을 자동으로 추론(infer)합니다.

### 9.6 서비스 패턴 예제(Service Pattern Example)

서비스는 접근자 메서드(accessor method)에서 `ZIO.serviceWithZIO[ServiceType]`를 통해 자신의 요구 사항을 선언합니다. 이후 구현들은 `ZLayer`로 합성된 뒤 이펙트에 전파됩니다.

---

## 10. 의존성 그래프 구축: 수동 vs 자동(Building the Dependency Graph)

의존성 그래프를 구축하는 방법은 두 가지입니다.

1. **수동 레이어 구성(Manual Layer Construction)** — 합성 연산자(`++` 수평, `>>>` 수직)를 사용합니다.
2. **자동 레이어 구성(Automatic Layer Construction)** — 컴파일 타임 메타프로그래밍(compile-time metaprogramming)을 사용합니다.

### 10.1 수동 구성 예제(Manual Example)

```scala
val appLayer: URLayer[Any, DocRepo with UserRepo] =
  ((Logging.live ++ Database.live ++ (Logging.live >>> BlobStorage.live)) >>> DocRepo.live) ++
    ((Logging.live ++ Database.live) >>> UserRepo.live)
```

### 10.2 자동 구성 예제(Automatic Approach)

```scala
val res: ZIO[Any, Throwable, Unit] =
  myApp.provide(
    Logging.live,
    Database.live,
    BlobStorage.live,
    DocRepo.live,
    UserRepo.live
  )
```

자동 구성에서는 "의존성의 순서(order)는 중요하지 않습니다." 또한 누락된 의존성은 어떤 서비스가 빠졌는지 알려 주는 진단 메시지(diagnostic message)와 함께 컴파일러 에러를 유발합니다.

---

## 11. 자동 레이어 구성(Automatic Layer Construction)

ZIO는 컴파일 타임 자동 레이어 구성을 제공하여 수동 의존성 그래프 합성을 제거합니다. 모든 의존성을 컴파일 타임에 검증하며 디버깅(debugging) 기능도 제공합니다.

### 11.1 개별 레이어 제공(Providing Individual Layers)

`ZIO#provide`, `ZIO#provideSome` 등을 사용하면 컴파일러가 공급된 레이어들로부터 의존성 그래프를 자동으로 구성합니다. 레이어의 순서는 무관합니다.

**서비스 구조 예제(Service Structure):**

```scala
import zio._

trait Cake
object Cake {
  val live: ZLayer[Chocolate & Flour, Nothing, Cake] =
    for {
      _ <- ZLayer.environment[Chocolate & Flour]
      cake <- ZLayer.succeed(new Cake {})
    } yield cake
}

trait Spoon
object Spoon {
  val live: ULayer[Spoon] =
    ZLayer.succeed(new Spoon {})
}

trait Chocolate
object Chocolate {
  val live: ZLayer[Spoon, Nothing, Chocolate] =
    ZLayer.service[Spoon].project(_ => new Chocolate {})
}

trait Flour
object Flour {
  val live: ZLayer[Spoon, Nothing, Flour] =
    ZLayer.service[Spoon].project(_ => new Flour {})
}
```

**애플리케이션 정의(Application Definition):**

```scala
val myApp: ZIO[Cake, IOException, Unit] = for {
  cake <- ZIO.service[Cake]
  _    <- Console.printLine(s"Yay! I baked a cake: $cake")
} yield ()
```

**레이어 제공(Providing Layers):**

```scala
object MainApp extends ZIOAppDefault {
  def run =
    myApp.provide(
      Cake.live,
      Chocolate.live,
      Flour.live,
      Spoon.live
    )
}
```

누락된 의존성은 어떤 타입을 제공해야 하는지 알려 주는 컴파일러 에러를 발생시킵니다.

### 11.2 자동 레이어 조립(Automatic Layer Assembly)

**ZLayer.make[R]** — 개별 레이어들을 타입 `R`의 단일 레이어로 조립합니다.

```scala
val cakeLayer: ZLayer[Any, Nothing, Cake] =
  ZLayer.make[Cake](
    Cake.live,
    Chocolate.live,
    Flour.live,
    Spoon.live
  )
```

서비스 교집합(intersection)에 대해서도 사용할 수 있습니다.

```scala
val chocolateAndFlourLayer: ZLayer[Any, Nothing, Chocolate & Flour] =
  ZLayer.make[Chocolate & Flour](
    Chocolate.live,
    Flour.live,
    Spoon.live
  )
```

**ZLayer.makeSome[R0, R]** — 나머지 `R0`를 미해결 상태로 남겨 둔 채 레이어를 구성합니다.

```scala
val cakeLayer: ZLayer[Spoon, Nothing, Cake] =
  ZLayer.makeSome[Spoon, Cake](
    Cake.live,
    Chocolate.live,
    Flour.live
  )
```

### 11.3 ZLayer 구성 디버깅(Debugging ZLayer Construction)

의존성 그래프를 시각화하는 두 가지 내장 디버그 레이어가 있습니다.

**트리 출력(ZLayer.Debug.tree):**

```scala
object MainApp extends ZIOAppDefault {
  def run =
    myApp.provide(
      Cake.live,
      Chocolate.live,
      Flour.live,
      Spoon.live,
      ZLayer.Debug.tree
    )
}
```

생성되는 출력은 다음과 같습니다.

```text
◉ Cake.live
├─◑ Chocolate.live
│ ╰─◑ Spoon.live
╰─◑ Flour.live
  ╰─◑ Spoon.live
```

**Mermaid 출력(ZLayer.Debug.mermaid):** 트리 시각화와 함께, 그래프를 인터랙티브하게 탐색할 수 있는 Mermaid Live Editor 링크를 생성합니다.

### 11.4 컴파일 에러 메시지(Compilation Error Messages)

자동 구성 시스템은 구체적인 진단 피드백을 제공합니다. 예를 들어 `Chocolate`과 `Flour`가 누락된 경우 다음과 같은 메시지가 나타납니다.

```text
Please provide layers for the following 2 types:
  Required by Cake.live
  1. Chocolate
  2. Flour
```

직접 의존성(direct dependency)을 공급하고 나면, `Spoon` 같은 이행적 의존성(transitive dependency)에 대한 후속 에러가 이어서 나타납니다.

---

## 12. 의존성 메모이제이션(Dependency Memoization)

### 12.1 개요(Overview)

"레이어 메모이제이션(layer memoization)은 레이어가 한 번 생성되어 의존성 그래프에서 여러 번 사용될 수 있도록 합니다." 덕분에 ZIO 애플리케이션에서 자원을 효율적으로 활용할 수 있습니다.

### 12.2 전역 레이어 메모이제이션 — 기본 동작(Default Behavior)

레이어가 전역적으로(globally) 제공될 때, ZIO는 의존성 그래프 전체에 걸쳐 자동으로 그것을 공유(share)합니다. 여러 서비스가 동일한 레이어에 의존하더라도 그 레이어는 단 한 번만 초기화됩니다.

```scala
import zio._

trait A
trait B
trait C

case class BLive(a: A) extends B
case class CLive(a: A) extends C

val a: ZLayer[Any, Nothing, A] =
  ZLayer(ZIO.succeed(new A {}).debug("initialized"))

val b: ZLayer[A, Nothing, B] =
  ZLayer {
    for {
      a <- ZIO.service[A]
    } yield BLive(a)
  }

val c: ZLayer[A, Nothing, C] =
  ZLayer {
    for {
      a <- ZIO.service[A]
    } yield CLive(a)
  }

object MainApp extends ZIOAppDefault {
  val myApp: ZIO[B & C, Nothing, Unit] =
    for {
      _ <- ZIO.service[B]
      _ <- ZIO.service[C]
    } yield ()

  def run = myApp.provide(a, b, c)
}
```

**출력:** `initialized`가 단 한 번만 출력되어, 공유 메모이제이션(shared memoization)을 보여 줍니다.

### 12.3 ZLayer#fresh — 독립 인스턴스 생성

기본 공유를 우회(bypass)하고 독립적인 인스턴스를 생성하려면 `fresh` 조합기(combinator)를 사용합니다.

```scala
object MainApp extends ZIOAppDefault {
  val myApp: ZIO[B & C, Nothing, Unit] =
    for {
      _ <- ZIO.service[B]
      _ <- ZIO.service[C]
    } yield ()

  def run = myApp.provideLayer((a.fresh >>> b) ++ (a.fresh >>> c))
}
```

**출력:** `initialized`가 두 번 출력되어, 별도의 인스턴스화(separate instantiation)를 보여 줍니다.

### 12.4 지역 제공 시에는 메모이제이션되지 않음(Local Provision)

"레이어는 지역적으로 제공될 때 메모이제이션되지 않습니다." 즉, 개별 이펙트에 `.provide()`로 지역적으로(locally) 레이어를 제공하면 메모이제이션이 자동으로 일어나지 않습니다.

```scala
object MainApp extends ZIOAppDefault {
  val myApp: ZIO[Any, Nothing, Unit] =
    for {
      _ <- ZIO.service[A].provide(a)
      _ <- ZIO.service[A].provide(a)
    } yield ()

  def run = myApp
}
```

**출력:** 독립적인 지역 제공으로 인해 `initialized`가 두 번 출력됩니다.

### 12.5 ZLayer#memoize — 수동 메모이제이션

지역 제공 상황에서 공유를 수동으로 제어하려면 `memoize`를 사용합니다.

```scala
object MainApp extends ZIOAppDefault {
  val myApp: ZIO[Any, Nothing, Unit] =
    ZIO.scoped {
      a.memoize.flatMap { aLayer =>
        for {
          _ <- ZIO.service[A].provide(aLayer)
          _ <- ZIO.service[A].provide(aLayer)
        } yield ()
      }
    }

  def run = myApp
}
```

**출력:** `initialized`가 한 번만 출력되어, 수동 메모이제이션이 성공했음을 보여 줍니다.

**핵심 통찰:** "`memoize`는 스코프 이펙트(scoped effect)를 반환하며, 그것을 평가(evaluate)하면 이 레이어의 지연 계산된(lazily computed) 결과를 반환합니다."

---

## 13. 의존성 그래프 재정의(Overriding the Dependency Graph)

환경을 관리하는 접근법은 두 가지입니다.

### 13.1 전역 환경(Global Environment)

"ZIO 애플리케이션을 작성할 때는 '세상의 끝(end of the world)'에서 레이어를 제공하는 것이 일반적입니다." 즉, 애플리케이션 진입점에서 모든 레이어를 한꺼번에 공급합니다.

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val myApp: ZIO[ServiceA & ServiceB & ServiceC & ServiceD, Throwable, Unit] = ???

  def run = myApp.provide(a, b, c, d)
}
```

### 13.2 지역 환경(Local Environment)

이 방식은 애플리케이션의 특정 부분에 대해 환경을 선택적으로 교체(selective replacement)할 수 있게 해 줍니다. 객체 지향 패러다임에서 "메서드를 오버라이드(overriding a method)"하는 것과 유사합니다.

```scala
import zio._

object MainApp extends ZIOAppDefault {
  def myApp: ZIO[A & B & C, Throwable, Unit] = {
    def innerApp1: ZIO[A & B & C, Throwable, Unit] = ???
    def innerApp2: ZIO[A & C, Throwable, Unit] = ???
    innerApp1.provideSomeLayer[A & B](localC) *> innerApp2
  }

  def run = myApp.provide(globalA, globalB, globalC)
}
```

지역 접근법은 특정 레이어(예: `localC`)를 대체하면서 나머지는 전역 환경에서 그대로 유지할 수 있게 해 줍니다. 핵심 로직을 수정하지 않고도 테스트 구현(test implementation)을 제공하는 등의 시나리오에 유용합니다.

---

## 14. 참고 자료

- [Dependency Injection (DI 개요)](https://zio.dev/reference/di/)
- [Motivation (동기)](https://zio.dev/reference/di/motivation)
- [Building the Dependency Graph (의존성 그래프 구축)](https://zio.dev/reference/di/building-dependency-graph)
- [Dependency Propagation (의존성 전파)](https://zio.dev/reference/di/dependency-propagation)
- [Automatic Layer Construction (자동 레이어 구성)](https://zio.dev/reference/di/automatic-layer-construction/)
- [Dependency Memoization (의존성 메모이제이션)](https://zio.dev/reference/di/dependency-memoization)
- [Overriding the Dependency Graph (의존성 그래프 재정의)](https://zio.dev/reference/di/overriding-dependency-graph)
- [Introduction to ZIO's Contextual Data Types (컨텍스트 데이터 타입)](https://zio.dev/reference/contextual/)
- [ZEnvironment](https://zio.dev/reference/contextual/zenvironment)
- [ZLayer](https://zio.dev/reference/contextual/zlayer/)
- [RLayer](https://zio.dev/reference/contextual/rlayer/)
- [URLayer](https://zio.dev/reference/contextual/urlayer/)
- [ULayer](https://zio.dev/reference/contextual/ulayer/)
