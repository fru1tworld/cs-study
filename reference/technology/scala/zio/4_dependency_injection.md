# ZIO 의존성 주입: ZEnvironment와 ZLayer

## ZIO 의존성 주입: ZEnvironment와 ZLayer

> 원본: https://zio.dev/reference/di/

---

### 목차

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

### 1. 의존성 주입 개요(Dependency Injection Overview)

#### 1.1 의존성이란 무엇인가(What is a Dependency)

**의존성(dependency)** 은 어떤 서비스(service)가 자신의 기능을 수행하기 위해 필요로 하는 또 다른 서비스. 예를 들어 `Editor` 서비스는 코드를 포맷팅(formatting)하고 컴파일(compile)하기 위해 `Formatter`와 `Compiler` 서비스가 필요.

#### 1.2 의존성 주입이란 무엇인가(What is Dependency Injection)

**의존성 주입(dependency injection, DI)** 은 "의존성의 사용(usage)을 그 실제 생성 과정(creation process)으로부터 분리(decoupling)하는 패턴". 서비스가 자신의 의존성을 직접 생성하는 대신, 외부 소스(external source)로부터 전달받음.

**DI 없이 작성한 경우** — 서비스가 의존성을 내부에서 직접 생성.

```scala
class Editor {
  private val formatter = new Formatter
  private val compiler = new Compiler
  def formatAndCompile(code: String): UIO[String] =
    formatter.format(code).flatMap(compiler.compile)
}
```

**생성자 기반 DI(Constructor-based DI)를 사용한 경우** — 의존성을 생성자 파라미터로 전달받음.

```scala
class Editor(formatter: Formatter, compiler: Compiler) {
  def formatAndCompile(code: String): UIO[String] = ???
}
```

#### 1.3 ZIO에 내장된 의존성 주입(ZIO's Built-in Dependency Injection)

ZIO는 다음 세 가지 구성 요소를 통해 통합된(integrated) 의존성 주입을 제공.

1. **ZIO 환경(ZIO Environment)** — `ZIO.serviceXYZ`로 서비스에 접근(access)하고, `ZIO.provideXYZ`로 서비스를 주입(inject)
2. **ZLayer** — 의존성 그래프(dependency graph)를 생성
3. **3단계 프로세스(three-step process)** — 서비스 접근(Access services) → 의존성 그래프 구축(Build dependency graph) → 서비스 제공(Provide services)

#### 1.4 ZIO 의존성 주입의 특징(Features)

ZIO의 의존성 주입은 다음 다섯 가지 핵심 역량을 제공.

- **합성 가능(Composable)** — 환경(Environment)과 의존성(Dependencies)을 자유롭게 합성 가능
- **타입 안전(Type-Safe)** — 의존성의 충족 여부를 컴파일 타임(compile-time)에 검증
- **효과적(Effectful)** — 의존성 그래프 구축 과정 자체를 이펙트(effect)로 취급 가능
- **자원 관리(Resourceful)** — 생성(creation)과 해제(release) 단계를 안전하게 관리
- **병렬성(Parallelism)** — 의존성을 병렬로 생성 가능

#### 1.5 다른 프레임워크(Other Frameworks)

ZIO 외의 대표적인 DI 솔루션.

- Guice
- izumi distage
- MacWire

---

### 2. 의존성 주입의 동기(Motivation)

#### 2.1 강한 결합의 문제점(Problems with Tight Coupling)

서비스가 자신의 의존성을 직접 인스턴스화(instantiate)할 때 발생하는 네 가지 핵심 문제.

1. **제어권 상실(Loss of Control)** — "`Editor` 서비스의 사용자는 의존성이 어떻게 생성될지에 대한 어떤 제어권도 갖지 못함"
2. **테스트의 어려움(Testing Difficulties)** — 의존성이 내부에서 생성되면 목(mock) 구현을 사용하기 어려움
3. **강한 결합(Tight Coupling)** — 의존성 서비스가 변경되면 그것을 사용하는 코드 전반에 변경이 강제됨
4. **수동 그래프 관리(Manual Graph Management)** — 복잡도가 커질수록 객체 생성(object creation)이 번거로워짐

#### 2.2 6단계 진화(Six-Step Evolution)

위 문제들은 다음 6단계를 거쳐 점진적으로 해결.

**1단계: 제어의 역전(Inversion of Control)** — 의존성을 내부에서 생성하는 대신 생성자 파라미터로 전달.

```scala
class Editor(formatter: Formatter, compiler: Compiler) {
  def formatAndCompile(code: String): UIO[String] =
    formatter.format(code).flatMap(compiler.compile)
}
```

**2단계: 인터페이스를 통한 분리(Decoupling via Interfaces)** — 구체 클래스(concrete class)가 아닌 트레잇(trait)을 대상으로 프로그래밍 → 테스트 시 구현 교체 용이.

**3단계: 중앙 집중식 매핑(Centralized Mapping)** — `ZEnvironment`를 타입 레벨 맵(type-level map)으로 사용해 인터페이스와 구현을 바인딩(bind) → 코드 전반의 수동 배선(manual wiring) 제거.

**4단계: 효과적인 생성 처리(Handling Effectful Construction)** — 생성 과정에 부수 효과(side effect, 예: `Ref.make`)가 수반되는 의존성을 `ZLayer`로 관리. 동시(concurrent)·자원 안전(resource-safe) 초기화 지원.

**5단계: 선언적 의존성(Declarative Dependencies)** — `R` 타입 파라미터를 사용해 필요 사항을 명령적으로 "요청(asking)"하는 대신 "선언(declaring)"하는 방식으로 전환.

**6단계: 자동 그래프 생성(Automatic Graph Generation)** — 매크로(macro) 지원을 받는 `provide` 메서드가 의존성 그래프를 자동으로 구성 → 수동 합성 보일러플레이트(boilerplate) 제거.

---

### 3. 컨텍스트 데이터 타입과 환경(Contextual Data Types & Environment)

ZIO는 실행 중인 이펙트의 환경(environment)을 `ZIO[R, E, A]`의 첫 번째 타입 파라미터 `R`로 인코딩(encode). `R`은 이펙트를 실행하기 위해 필요한 환경적 컨텍스트(environmental context)를 나타냄.

"모든 이펙트는 환경(environment)이라 불리는 특정 컨텍스트 안에서 동작 가능." `R` 타입 파라미터는 이펙트가 실행 가능해지기 전에 충족되어야 하는 의존성을 가리킴.

ZIO가 제공하는 세 가지 주요 컨텍스트 추상화(contextual abstraction).

1. **ZIO 환경(ZIO Environment)** — `ZIO[R, E, A]`의 `R` 타입 파라미터
2. **ZEnvironment** — 이펙트의 환경을 유지(maintain)하는 내장 타입 레벨 맵(built-in type-level map)
3. **ZLayer** — "값 `RIn`에서 출발하여 타입 `ROut`의 환경을 구축하기 위한 레시피(recipe)"

#### 3.1 서비스에 접근하기(Accessing Services)

환경과 상호작용하기 위한 두 가지 접근자(accessor) 범주.

- **서비스 접근자(Service Accessor)** — `ZIO.service`로 환경에서 특정 서비스를 가져옴
- **서비스 멤버 접근자(Service Member Accessors)** — `ZIO.serviceWith`, `ZIO.serviceWithZIO`로 반환 타입(return type)에 기반하여 서비스의 기능에 접근

#### 3.2 환경 활용의 장점(Key Advantages)

ZIO의 환경 기능을 사용하면 가능해지는 것.

- 구현(implementation)이 아닌 인터페이스(interface)를 대상으로 코딩
- 서비스 모킹(mocking)을 통한 테스트 가능한 이펙트 작성
- 강력한 타입 추론(type inference)과 함께 서비스를 합성

---

### 4. ZEnvironment

#### 4.1 ZEnvironment란 무엇인가

`ZEnvironment[R]`는 ZIO 이펙트의 환경을 유지하는 내장 타입 레벨 맵. 환경적 서비스와 그 구현을 저장하며, ZIO 생태계 안에서 의존성 주입을 가능하게 함.

ZIO와 ZEnvironment의 관계는 개념적으로 다음과 같이 표현 가능.

```scala
type ZIO[R, E, A] = ZEnvironment[R] => Either[E, A]
```

즉, ZIO 이펙트는 개념적으로 환경(environment)에서 결과(outcome)로 가는 함수.

#### 4.2 ZEnvironment 생성하기

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

#### 4.3 핵심 연산(Core Operations)

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

#### 4.4 환경 접근과 제공(Accessing and Providing Environments)

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

#### 4.5 동일 타입 다중 인스턴스 패턴(Multiple Instances Pattern)

같은 타입의 서비스를 여러 개 다루어야 할 때는 `Map[K, A]`를 사용.

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

타입 레벨 맵 구조는 내부적으로 서비스와 그 구현을 표현하며, ZIO 애플리케이션 전반에 걸쳐 타입 안전한(type-safe) 의존성 관리를 지원.

---

### 5. ZLayer

#### 5.1 ZLayer란 무엇인가

`ZLayer[-RIn, +E, +ROut]`는 "애플리케이션의 한 레이어(layer)"를 나타냄. 모든 레이어는 입력(input)으로 일부 서비스 `RIn`을 필요로 하고, 출력(output)으로 일부 서비스 `ROut`을 생산. 개념적으로는 비동기 함수(asynchronous function)와 유사.

```text
RIn => async Either[E, ROut]
```

#### 5.2 타입 파라미터(Type Parameters)

- **RIn** — 필요한 입력 의존성(input dependencies)
- **E** — 에러 타입(error type)
- **ROut** — 출력 서비스 타입(output service type)

레이어들은 뛰어난 합성 속성(composition properties)을 가지므로, ZIO에서 다른 서비스에 의존하는 서비스를 생성하는 관용적(idiomatic)인 방법으로 사용됨.

---

### 6. ZLayer 타입 별칭(Type Aliases)

`ZLayer[RIn, E, ROut]`의 흔한 형태를 간결하게 표현하기 위해 다음 타입 별칭(type alias)들이 제공됨.

- `RLayer[-RIn, +ROut]`: `ZLayer[RIn, Throwable, ROut]`
  - `RIn`을 입력으로 요구, `Throwable`로 실패 가능, `ROut` 출력
- `URLayer[-RIn, +ROut]`: `ZLayer[RIn, Nothing, ROut]`
  - `RIn`을 입력으로 요구, 실패 불가, `ROut` 출력
- `Layer[+E, +ROut]`: `ZLayer[Any, E, ROut]`
  - 입력 서비스 불요, `E`로 실패 가능, `ROut` 출력
- `ULayer[+ROut]`: `ZLayer[Any, Nothing, ROut]`
  - 입력 서비스 불요, 실패 불가, `ROut` 출력
- `TaskLayer[+ROut]`: `ZLayer[Any, Throwable, ROut]`
  - 입력 서비스 불요, `Throwable`로 실패 가능, `ROut` 출력

- `R`(Required) 접두사: 입력 의존성 `RIn`을 요구함을 나타냄
- `U`(Unfailing) 접두사: 에러 타입이 `Nothing`이어서 실패할 수 없음을 나타냄
- `Task` 접두사: 에러 타입이 `Throwable`임을 나타냄

---

### 7. ZLayer 생성 방법(Creating Layers)

ZLayer를 만드는 주요 생성 방법은 다음 네 가지.

#### 7.1 ZLayer.succeed — 값이나 기존 서비스로부터

단순한 값이나 이미 존재하는 서비스로부터 레이어를 생성.

```scala
def succeed[A: Tag](a: A): ULayer[A]
```

```scala
// 값으로부터
val configLayer: ULayer[AppConfig] =
  ZLayer.succeed(AppConfig("localhost", 8080))
```

#### 7.2 ZLayer.fromZIO / ZLayer.apply — 비자원적 이펙트로부터

자원 해제가 필요 없는(non-resourceful) 이펙트로부터 for-컴프리헨션(for-comprehension)을 사용해 레이어를 생성. `ZLayer { ... }`는 `ZLayer.apply`의 축약.

```scala
val layer: ZLayer[A & B, Nothing, C] = ZLayer {
  for {
    a <- ZIO.service[A]
    b <- ZIO.service[B]
  } yield CLive(a, b)
}
```

#### 7.3 ZLayer.fromFunction — 함수로부터

함수를 레이어로 변환.

```scala
val layer: ZLayer[A & B, Nothing, C] =
  ZLayer.fromFunction(CLive.apply _)
```

#### 7.4 ZLayer.scoped — 자원적 이펙트로부터

획득(acquisition)과 해제(release)가 필요한 자원적(resourceful) 이펙트로부터 레이어를 생성.

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

#### 7.5 기타 연산자(Other Operators)

ZLayer가 제공하는 부가 연산.

- **orElse** — 레이어 구축이 실패할 경우 대체 레이어로 폴백(fallback)
- **retry** — `Schedule`을 사용해 구축을 재시도(retry)
- **project** — 레이어의 일부분을 추출(extract)
- **tap / tapError** — 성공/실패 시 이펙트를 수행
- **build** — 레이어를 스코프(scoped) ZIO로 변환
- **launch** — 레이어를 ZIO 애플리케이션으로 변환

---

### 8. ZLayer 구성: 수직·수평 합성(Vertical & Horizontal Composition)

레이어는 두 가지 방식으로 합성(compose)됨.

#### 8.1 수직 합성(Vertical Composition) — `>>>`

`>>>` 연산자는 레이어들을 순차적으로(sequentially) 연결. 한 레이어의 출력(output)이 다음 레이어의 입력(input)이 됨.

```scala
// DatabaseConfig의 출력이 Database의 입력으로 전달됨
DatabaseConfig.live >>> Database.live
```

#### 8.2 수평 합성(Horizontal Composition) — `++`

`++` 연산자는 서로의 출력에 의존하지 않는(independent) 레이어들을 나란히(side by side) 결합. 결과 레이어의 입력은 두 레이어 입력의 합집합(union), 출력은 두 레이어 출력의 합집합.

```scala
val app: ZEnvironment[AppConfig] =
  ZEnvironment.empty ++ ZEnvironment(AppConfig("localhost", 8080))
```

#### 8.3 수직 합성 + 좌측 출력 유지 — `>+>`

`>+>` 연산자는 수직 합성을 수행하되, 왼쪽 레이어의 출력도 함께 결과에 유지. 즉, `a >+> b`는 개념적으로 `a >>> (a ++ b)`와 유사하게 왼쪽과 오른쪽 출력을 모두 제공.

#### 8.4 합성 예제(Composition Example)

복잡한 의존성 그래프를 수동(manual)으로 구성한 예제.

```scala
ZIO
  .serviceWithZIO[App](_.execute)
  .provide(
    (((DatabaseConfig.live >>> Database.live) ++ Analytics.live >>> Users.live)
     ++ Analytics.live) >>> App.live
  )
```

또 다른 수동 구성 예제 — `DocRepo`와 `UserRepo`를 함께 제공.

```scala
val appLayer: URLayer[Any, DocRepo with UserRepo] =
  ((Logging.live ++ Database.live ++ (Logging.live >>> BlobStorage.live)) >>> DocRepo.live) ++
    ((Logging.live ++ Database.live) >>> UserRepo.live)
```

---

### 9. 의존성 전파: provide, provideLayer, provideSome(Dependency Propagation)

ZIO는 환경 타입 파라미터 `R`을 통해 의존성 전파(dependency propagation)를 처리. "구현을 제공하고, 애플리케이션 전체에 걸쳐 모든 의존성을 공급(feed)하고 전파(propagate)할 방법이 필요."

#### 9.1 ZIO#provideEnvironment

`ZEnvironment[R]` 인스턴스를 직접 받아 환경 의존성을 제거.

```scala
trait ZIO[-R, +E, +A] {
  def provideEnvironment(r: => ZEnvironment[R]): IO[E, A]
}
```

#### 9.2 ZIO#provide

원시 환경(raw environment) 대신 `ZLayer` 인스턴스들을 받음. 모든 의존성이 충족되면 `R`이 `Any`로 소거됨.

```scala
val mainEffect: ZIO[Any, Nothing, Unit] =
  myApp.provide(FooLive.layer, BarLive.layer)
```

위 예제는 `ZIO[Foo & Bar, Nothing, Unit]`을 `ZIO[Any, Nothing, Unit]`으로 변환.

#### 9.3 ZIO#provideSome

레이어를 부분적으로(partially) 적용하여 일부 요구 사항을 미충족 상태로 남겨 둠.

```scala
val mainEffectSome: ZIO[Bar, Nothing, Unit] =
  myApp.provideSome(FooLive.layer)
```

`Foo`만 제공하고 `Bar`는 남겨 두므로 결과 타입은 `ZIO[Bar, Nothing, Unit]`.

#### 9.4 ZIO#provideLayer / ZIO#provideSomeLayer

이미 합성된 단일 레이어를 제공할 때는 `provideLayer`, 일부만 제공할 때는 `provideSomeLayer[남길타입]` 사용. (`provideSomeLayer` 사용 예는 [13장](#13-의존성-그래프-재정의overriding-the-dependency-graph) 참고)

#### 9.5 ZIO#provideSomeAuto

Scala 3에서 제공되는 향상 기능으로, 명시적 타입 파라미터 없이 남은 타입 요구 사항을 자동으로 추론(infer).

#### 9.6 서비스 패턴 예제(Service Pattern Example)

서비스는 접근자 메서드(accessor method)에서 `ZIO.serviceWithZIO[ServiceType]`를 통해 자신의 요구 사항을 선언. 이후 구현들은 `ZLayer`로 합성된 뒤 이펙트에 전파됨.

---

### 10. 의존성 그래프 구축: 수동 vs 자동(Building the Dependency Graph)

의존성 그래프를 구축하는 방법은 두 가지.

1. **수동 레이어 구성(Manual Layer Construction)** — 합성 연산자(`++` 수평, `>>>` 수직)를 사용
2. **자동 레이어 구성(Automatic Layer Construction)** — 컴파일 타임 메타프로그래밍(compile-time metaprogramming)을 사용

#### 10.1 수동 구성 예제(Manual Example)

```scala
val appLayer: URLayer[Any, DocRepo with UserRepo] =
  ((Logging.live ++ Database.live ++ (Logging.live >>> BlobStorage.live)) >>> DocRepo.live) ++
    ((Logging.live ++ Database.live) >>> UserRepo.live)
```

#### 10.2 자동 구성 예제(Automatic Approach)

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

자동 구성에서는 "의존성의 순서(order)는 중요하지 않음." 또한 누락된 의존성은 어떤 서비스가 빠졌는지 알려 주는 진단 메시지(diagnostic message)와 함께 컴파일러 에러 유발.

---

### 11. 자동 레이어 구성(Automatic Layer Construction)

ZIO는 컴파일 타임 자동 레이어 구성을 제공하여 수동 의존성 그래프 합성을 제거. 모든 의존성을 컴파일 타임에 검증하며 디버깅(debugging) 기능도 제공.

#### 11.1 개별 레이어 제공(Providing Individual Layers)

`ZIO#provide`, `ZIO#provideSome` 등을 사용하면 컴파일러가 공급된 레이어들로부터 의존성 그래프를 자동으로 구성. 레이어의 순서는 무관.

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

누락된 의존성은 어떤 타입을 제공해야 하는지 알려 주는 컴파일러 에러를 발생시킴.

#### 11.2 자동 레이어 조립(Automatic Layer Assembly)

**ZLayer.make[R]** — 개별 레이어들을 타입 `R`의 단일 레이어로 조립.

```scala
val cakeLayer: ZLayer[Any, Nothing, Cake] =
  ZLayer.make[Cake](
    Cake.live,
    Chocolate.live,
    Flour.live,
    Spoon.live
  )
```

서비스 교집합(intersection)에 대해서도 사용 가능.

```scala
val chocolateAndFlourLayer: ZLayer[Any, Nothing, Chocolate & Flour] =
  ZLayer.make[Chocolate & Flour](
    Chocolate.live,
    Flour.live,
    Spoon.live
  )
```

**ZLayer.makeSome[R0, R]** — 나머지 `R0`를 미해결 상태로 남겨 둔 채 레이어를 구성.

```scala
val cakeLayer: ZLayer[Spoon, Nothing, Cake] =
  ZLayer.makeSome[Spoon, Cake](
    Cake.live,
    Chocolate.live,
    Flour.live
  )
```

#### 11.3 ZLayer 구성 디버깅(Debugging ZLayer Construction)

의존성 그래프를 시각화하는 두 가지 내장 디버그 레이어.

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

생성되는 출력.

```text
◉ Cake.live
├─◑ Chocolate.live
│ ╰─◑ Spoon.live
╰─◑ Flour.live
  ╰─◑ Spoon.live
```

**Mermaid 출력(ZLayer.Debug.mermaid):** 트리 시각화와 함께, 그래프를 인터랙티브하게 탐색할 수 있는 Mermaid Live Editor 링크를 생성.

#### 11.4 컴파일 에러 메시지(Compilation Error Messages)

자동 구성 시스템은 구체적인 진단 피드백을 제공. 예를 들어 `Chocolate`과 `Flour`가 누락된 경우 다음과 같은 메시지가 나타남.

```text
Please provide layers for the following 2 types:
  Required by Cake.live
  1. Chocolate
  2. Flour
```

직접 의존성(direct dependency)을 공급하고 나면, `Spoon` 같은 이행적 의존성(transitive dependency)에 대한 후속 에러가 이어서 나타남.

---

### 12. 의존성 메모이제이션(Dependency Memoization)

#### 12.1 개요(Overview)

"레이어 메모이제이션(layer memoization)은 레이어가 한 번 생성되어 의존성 그래프에서 여러 번 사용될 수 있도록 함." 덕분에 ZIO 애플리케이션에서 자원을 효율적으로 활용 가능.

#### 12.2 전역 레이어 메모이제이션 — 기본 동작(Default Behavior)

레이어가 전역적으로(globally) 제공될 때, ZIO는 의존성 그래프 전체에 걸쳐 자동으로 공유(share). 여러 서비스가 동일한 레이어에 의존하더라도 그 레이어는 단 한 번만 초기화됨.

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

**출력:** `initialized`가 단 한 번만 출력 → 공유 메모이제이션(shared memoization) 확인.

#### 12.3 ZLayer#fresh — 독립 인스턴스 생성

기본 공유를 우회(bypass)하고 독립적인 인스턴스를 생성하려면 `fresh` 조합기(combinator)를 사용.

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

**출력:** `initialized`가 두 번 출력 → 별도의 인스턴스화(separate instantiation) 확인.

#### 12.4 지역 제공 시에는 메모이제이션되지 않음(Local Provision)

"레이어는 지역적으로 제공될 때 메모이제이션되지 않음." 즉, 개별 이펙트에 `.provide()`로 지역적으로(locally) 레이어를 제공하면 메모이제이션이 자동으로 일어나지 않음.

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

**출력:** 독립적인 지역 제공으로 인해 `initialized`가 두 번 출력됨.

#### 12.5 ZLayer#memoize — 수동 메모이제이션

지역 제공 상황에서 공유를 수동으로 제어하려면 `memoize`를 사용.

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

**출력:** `initialized`가 한 번만 출력 → 수동 메모이제이션 성공 확인.

**핵심 통찰:** "`memoize`는 스코프 이펙트(scoped effect)를 반환하며, 그것을 평가(evaluate)하면 이 레이어의 지연 계산된(lazily computed) 결과를 반환."

---

### 13. 의존성 그래프 재정의(Overriding the Dependency Graph)

환경을 관리하는 접근법은 두 가지.

#### 13.1 전역 환경(Global Environment)

"ZIO 애플리케이션을 작성할 때는 '세상의 끝(end of the world)'에서 레이어를 제공하는 것이 일반적." 즉, 애플리케이션 진입점에서 모든 레이어를 한꺼번에 공급.

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val myApp: ZIO[ServiceA & ServiceB & ServiceC & ServiceD, Throwable, Unit] = ???

  def run = myApp.provide(a, b, c, d)
}
```

#### 13.2 지역 환경(Local Environment)

이 방식은 애플리케이션의 특정 부분에 대해 환경을 선택적으로 교체(selective replacement)할 수 있게 함. 객체 지향 패러다임에서 "메서드를 오버라이드(overriding a method)"하는 것과 유사.

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

지역 접근법은 특정 레이어(예: `localC`)를 대체하면서 나머지는 전역 환경에서 그대로 유지 가능. 핵심 로직을 수정하지 않고도 테스트 구현(test implementation)을 제공하는 등의 시나리오에 유용.

---

### 14. 참고 자료

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

---

## ZIO 서비스 작성 패턴(Writing ZIO Services)

> 원본: https://zio.dev/reference/service-pattern/introduction

---

### 목차

1. [서비스/모듈 패턴 개요(Introduction)](#1-서비스모듈-패턴-개요introduction)
2. [서비스 패턴(Service Pattern)](#2-서비스-패턴service-pattern)
3. [ZIO 환경의 세 가지 법칙(The Three Laws of ZIO Environment)](#3-zio-환경의-세-가지-법칙the-three-laws-of-zio-environment)
4. [다형적 서비스(Polymorphic Services)](#4-다형적-서비스polymorphic-services)
5. [재로드 가능한 서비스(Reloadable Services)](#5-재로드-가능한-서비스reloadable-services)
6. [참고 자료](#6-참고-자료)

---

### 1. 서비스/모듈 패턴 개요(Introduction)

#### 객체지향 방식(OOP Approach)

전통적인 객체지향 프로그래밍에서 서비스는 다음 세 요소로 구성.

- 트레이트(trait)로 서비스 인터페이스(interface)를 정의
- 클래스(class)로 그 인터페이스를 구현
- 생성자(constructor)를 통해 의존성을 주입

#### ZIO 방식(ZIO Approach)

ZIO는 이와 동일한 발상을 함수형으로 재구성. 핵심 원칙: "프로그램을 구현이 아닌 인터페이스에 맞춰 작성"하고, `ZLayer`를 서비스의 생성자(constructor)처럼 활용. 트레이트로 서비스의 계약(contract)을 정의하고, `ZLayer`로 그 구현을 만들고 조립(assemble)함으로써 테스트 가능성(testability)과 모듈성(modularity)을 확보.

---

### 2. 서비스 패턴(Service Pattern)

서비스 패턴은 네 가지 요소로 구성.

#### 2.1 서비스 정의(Service Definition)

트레이트로 서비스 인터페이스 정의.

```scala
trait DocRepo {
  def get(id: String): ZIO[Any, Throwable, Doc]
  def save(document: Doc): ZIO[Any, Throwable, String]
  def delete(id: String): ZIO[Any, Throwable, Unit]
  def findByTitle(title: String): ZIO[Any, Throwable, List[Doc]]
}
```

#### 2.2 서비스 구현(Service Implementation)

Scala 클래스로 트레이트를 구현.

```scala
final class DocRepoLive() extends DocRepo {
  override def get(id: String): ZIO[Any, Throwable, Doc] = ???
  override def save(document: Doc): ZIO[Any, Throwable, String] = ???
  override def delete(id: String): ZIO[Any, Throwable, Unit] = ???
  override def findByTitle(title: String): ZIO[Any, Throwable, List[Doc]] = ???
}
```

#### 2.3 의존성 관리(Service Dependencies)

구현체가 다른 서비스에 의존한다면 생성자 주입(constructor injection) 방식으로 받음. 아래 예제에서 `DocRepoLive`는 `MetadataRepo`와 `BlobStorage`에 의존.

```scala
final class DocRepoLive(
  metadataRepo: MetadataRepo,
  blobStorage: BlobStorage
) extends DocRepo {
  override def get(id: String): ZIO[Any, Throwable, Doc] =
    (metadataRepo.get(id) <&> blobStorage.get(id)).map {
      case (metadata, content) =>
        Doc(
          title = metadata.title,
          description = metadata.description,
          language = metadata.language,
          format = metadata.format,
          content = content
        )
    }
  // 나머지 메서드 구현...
}
```

#### 2.4 ZLayer 정의(Constructor)

컴패니언 객체(companion object)에서 서비스 구현을 `ZLayer`로 감싸 정의. 이 레이어가 곧 서비스의 생성자 역할.

```scala
object DocRepo {
  val live: ZLayer[BlobStorage & MetadataRepo, Nothing, DocRepo] =
    ZLayer {
      for {
        metadataRepo <- ZIO.service[MetadataRepo]
        blobStorage  <- ZIO.service[BlobStorage]
      } yield new DocRepoLive(metadataRepo, blobStorage)
    }
}
```

#### 2.5 애플리케이션 조립(Assembling the Application)

`ZIOAppDefault`를 확장한 진입점에서 필요한 모든 레이어를 제공.

```scala
object MainApp extends ZIOAppDefault {
  val app = for {
    docRepo <- ZIO.service[DocRepo]
    // 비즈니스 로직...
  } yield ()

  def run = app.provide(
    DocRepo.live,
    InmemoryBlobStorage.layer,
    InmemoryMetadataRepo.layer
  )
}
```

이 패턴은 객체지향 프로그래밍의 모범 사례(생성자 주입, 인터페이스 지향 설계)를 그대로 따르면서도, `ZLayer`의 합성성·자원 안전성 같은 함수형 프로그래밍의 이점을 함께 누림.

---

### 3. ZIO 환경의 세 가지 법칙(The Three Laws of ZIO Environment)

서비스 패턴을 올바르게 적용하려면 다음 세 가지 법칙을 지켜야 함.

#### 법칙 1: 서비스 인터페이스(트레이트)

서비스 인터페이스를 정의할 때는 그 서비스 자신의 의존성을 환경(`R`)에 반영하면 안 됨. 구현 세부사항(implementation detail)을 인터페이스에 노출시키지 말아야 한다는 원칙.

잘못된 예 — `DocRepo`가 `BlobStorage`와 `MetadataRepo`에 의존한다는 사실이 트레이트 시그니처에 새어 나옴.

```scala
trait DocRepo {
  def save(document: Doc): ZIO[BlobStorage & MetadataRepo, Throwable, String]
}
```

올바른 예 — 인터페이스는 `Any` 환경만 요구.

```scala
trait DocRepo {
  def save(document: Doc): ZIO[Any, Throwable, String]
}
```

#### 법칙 2: 서비스 구현(클래스)

서비스 인터페이스를 구현할 때는 모든 의존성을 클래스 생성자에서 받아야 함. `ZIO.service`로 환경에서 의존성을 조회하는 작업은 `ZLayer` 정의 시점(레이어 생성 과정)에서 끝나야 하며, 개별 메서드 안에서 반복되어서는 안 됨.

```scala
case class DocRepoImpl(
    metadataRepo: MetadataRepo,
    blobStorage: BlobStorage) extends DocRepo {
  override def delete(id: String): ZIO[Any, Throwable, Unit] =
    for {
      _ <- blobStorage.delete(id)
      _ <- metadataRepo.delete(id)
    } yield ()
}

object DocRepoImpl {
  val layer: ZLayer[BlobStorage with MetadataRepo, Nothing, DocRepo] =
    ZLayer {
      for {
        metadataRepo <- ZIO.service[MetadataRepo]
        blobStorage  <- ZIO.service[BlobStorage]
      } yield DocRepoImpl(metadataRepo, blobStorage)
    }
}
```

#### 법칙 3: 비즈니스 로직

비즈니스 로직에서는 ZIO 환경을 통해 서비스를 소비(consume)해야 함 → `ZIO.serviceWithZIO[Service]`로 접근.

```scala
object MainApp extends ZIOAppDefault {
  val app =
    for {
      id <-
        ZIO.serviceWithZIO[DocRepo](_.save(
          Doc("제목", "설명", "언어", "형식", "내용".getBytes())
        ))
      doc <- ZIO.serviceWithZIO[DocRepo](_.get(id))
      _   <- Console.printLine(s"문서 ID: $id")
      _   <- ZIO.serviceWithZIO[DocRepo](_.delete(id))
    } yield ()

  def run =
    app.provide(
      DocRepoImpl.layer,
      InmemoryBlobStorage.layer,
      InmemoryMetadataRepo.layer
    )
}
```

#### 예외 상황(Exceptions)

일부 경우에는 트레이트 시그니처에 환경을 노출하는 것이 허용됨.

- **HTTP 요청 컨텍스트**: `ZIO[HttpRequest, ...]` — 구현 세부사항이 아니라 인터페이스 의미론(semantics)의 일부
- **데이터베이스 트랜잭션**: `ZIO[DatabaseTransaction, ...]` — 트랜잭션 안에서의 동작이 서비스 자체의 핵심 의미론에 해당

두 경우 모두 "로컬 컨텍스트(local context)"로서 구현과 무관하게 독립적이므로 법칙 1의 예외로 허용.

---

### 4. 다형적 서비스(Polymorphic Services)

타입 파라미터를 가진 서비스 인터페이스를 작성할 때는 `Tag` 타입 클래스가 필요. `ZEnvironment`는 서비스 타입에서 구현으로의 타입 레벨 매핑(type-level mapping)으로 뒷받침되는데, 이 매핑은 내부적으로 `izumi.reflect.Tag`에 의존하기 때문.

#### 문제 상황(The Problem)

타입 파라미터가 있는 서비스.

```scala
trait KeyValueStore[K, V, E, F[_, _]] {
  def get(key: K): F[E, V]
  def set(key: K, value: V): F[E, V]
  def remove(key: K): F[E, Unit]
}
```

`Tag` 없이 접근자 메서드를 작성하면 컴파일 에러 발생.

```scala
def get[K, V, E](key: K): ZIO[KeyValueStore[K, V, E, IO], E, V] =
  ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.get(key))
// could not find implicit value for izumi.reflect.Tag[K]
```

#### 해결책: Tag 컨텍스트 바운드 추가

타입 파라미터마다 `Tag` 컨텍스트 바운드(context bound)를 붙여 해결.

```scala
object KeyValueStore {
  def get[K: Tag, V: Tag, E: Tag](key: K):
    ZIO[KeyValueStore[K, V, E, IO], E, V] =
    ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.get(key))

  def set[K: Tag, V: Tag, E: Tag](key: K, value: V):
    ZIO[KeyValueStore[K, V, E, IO], E, V] =
    ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.set(key, value))

  def remove[K: Tag, V: Tag, E: Tag](key: K):
    ZIO[KeyValueStore[K, V, E, IO], E, Unit] =
    ZIO.serviceWithZIO(_.remove(key))
}
```

구현 예제:

```scala
case class InmemoryKeyValueStore(map: Ref[Map[String, Int]])
  extends KeyValueStore[String, Int, String, IO] {
  override def get(key: String): IO[String, Int] =
    map.get.map(_.get(key)).someOrFail(s"$key not found")
  override def set(key: String, value: Int): IO[String, Int] =
    map.update(_.updated(key, value)).map(_ => value)
  override def remove(key: String): IO[String, Unit] =
    map.update(_.removed(key))
}

object InmemoryKeyValueStore {
  def layer: ULayer[KeyValueStore[String, Int, String, IO]] =
    ZLayer {
      Ref.make(Map[String, Int]()).map(InmemoryKeyValueStore.apply)
    }
}
```

사용 예제:

```scala
object MainApp extends ZIOAppDefault {
  val myApp: ZIO[KeyValueStore[String, Int, String, IO], String, Unit] =
    for {
      _ <- KeyValueStore.set[String, Int, String]("key1", 3).debug
      _ <- KeyValueStore.get[String, Int, String]("key1").debug
      _ <- KeyValueStore.remove[String, Int, String]("key1")
      _ <- KeyValueStore.get[String, Int, String]("key1").either.debug
    } yield ()

  def run = myApp.provide(InmemoryKeyValueStore.layer)
}
// 출력: 3, 3, Left(key1 not found)
```

#### 고차 타입 파라미터 지원(Higher-Kinded Type Parameters)

`F[_, _]`처럼 고차 타입 생성자(higher-kinded type constructor)까지 다형적으로 다루려면 `TagKK` 컨텍스트 바운드 추가.

```scala
def get[K: Tag, V: Tag, E: Tag, F[_, _]: TagKK](key: K):
  ZIO[KeyValueStore[K, V, E, F], Nothing, F[E, V]] =
  ZIO.serviceWith[KeyValueStore[K, V, E, F]](_.get(key))
```

핵심: 다형적인 서비스 코드를 작성할 때는 항상 타입 파라미터에 `Tag`(단일 타입) 또는 `TagKK`(고차 타입 생성자) 컨텍스트 바운드를 명시.

---

### 5. 재로드 가능한 서비스(Reloadable Services)

#### 5.1 개요(Overview)

재로드 가능한 서비스(reloadable service) → 애플리케이션을 재시작하지 않고도 런타임(runtime)에 서비스를 다시 로드(reload)할 수 있게 하는 패턴. 서비스를 리로드하면 기존 리소스(파일, 네트워크 연결, DB 연결 등)가 자동으로 해제되고 새 리소스가 할당됨.

대표적인 사용 사례:

- 설정(configuration) 변경 시 서비스 리로드
- 일정한 간격(예: n분마다)으로 스케줄된 리로드
- 데이터베이스 스키마 변경 시 리로드

#### 5.2 Reloadable 데이터 타입

```scala
case class Reloadable[Service](
  scopedRef: ScopedRef[Service],
  reload: IO[Any, Unit]
) {
  def get: UIO[Service] = scopedRef.get
  def reloadFork: UIO[Unit] = reload.ignoreLogged.forkDaemon.unit
}
```

- `get` — 현재 관리 중인 서비스 인스턴스를 반환
- `reload` — 서비스를 리로드(기존 리소스 해제 후 새 리소스 할당)

#### 5.3 수동 리로드(Reloadable.manual)

명시적으로 `reload`를 호출해야 하는 방식.

```scala
object Counter {
  val live: ZLayer[Any, Nothing, Counter] = ZLayer.scoped {
    for {
      id  <- Ref.make(UUID.randomUUID())
      ref <- Ref.make(0)
      service = CounterLive(id, ref)
      _ <- service.acquire
      _ <- ZIO.addFinalizer(service.release)
    } yield service
  }

  val reloadable: ZLayer[Any, Nothing, Reloadable[Counter]] =
    Reloadable.manual(live)
    // 또는: live.reloadableManual
}
```

```scala
object ReloadableExample extends ZIOAppDefault {
  val app: ZIO[Reloadable[Counter], Any, Unit] =
    for {
      reloadable <- ZIO.service[Reloadable[Counter]]
      counter    <- reloadable.get
      _          <- counter.increment
      _          <- counter.increment
      _          <- counter.increment
      _          <- counter.get.debug("Counter value is")
      _          <- reloadable.reload *> ZIO.sleep(1.second)
      counter    <- reloadable.get
      _          <- counter.increment
      _          <- counter.increment
      _          <- counter.get.debug("Counter value is")
    } yield ()

  def run = app.provide(Counter.reloadable)
}
```

실행 결과 — 리로드 시점에 기존 인스턴스가 해제(release)되고 새 인스턴스가 획득(acquire)되며, 카운터 값이 0으로 리셋됨.

```
Acquired counter d04519a3-7332-43ca-bc86-f61fbaf2e3d6
Counter value: 3
Released counter d04519a3-7332-43ca-bc86-f61fbaf2e3d6
Acquired counter bc66ba00-0b50-4e6e-9f60-c38b6e140a82
Counter value: 2
```

#### 5.4 자동 리로드(Reloadable.auto)

`Schedule`을 지정하여 일정한 주기마다 자동으로 리로드.

```scala
object Counter {
  val live: ZLayer[Any, Nothing, Counter] = ???

  val autoReloadable: ZLayer[Any, Nothing, Reloadable[Counter]] =
    Reloadable.auto(live, Schedule.fixed(5.seconds))
    // 또는: live.reloadableAuto(Schedule.fixed(5.seconds))
}
```

5초마다 자동으로 리로드되므로 애플리케이션 코드에서 수동으로 `reload`를 호출할 필요 없음.

#### 5.5 ServiceReloader(zio-macros)

`zio-macros` 라이브러리가 제공하는 `ServiceReloader`를 사용하면 `Reloadable` 래퍼 없이 서비스에 직접 접근하면서도 리로드 가능.

```scala
libraryDependencies ++= Seq("dev.zio" %% "zio-macros" % "<version>")
```

```scala
trait ServiceReloader {
  def register[A: Tag: IsReloadable](
    serviceLayer: ZLayer[Any, Any, A]
  ): IO[ServiceReloader.Error, A]
  def reload[A: Tag]: IO[ServiceReloader.Error, Unit]
}
```

```scala
import zio._
import zio.macros._

object Counter {
  val live: ZLayer[Any, Nothing, Counter] = ???

  val reloadable: ZLayer[ServiceReloader, ServiceReloader.Error, Counter] =
    live.reloadable
}
```

```scala
object ServiceReloaderExample extends ZIOAppDefault {
  def app: ZIO[Counter with ServiceReloader, ServiceReloader.Error, Unit] =
    for {
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.get.debug("Counter value")
      _ <- ServiceReloader.reload[Counter]
      _ <- ZIO.sleep(1.seconds)
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.get.debug("Counter value")
    } yield ()

  def run = app.provide(Counter.reloadable, ServiceReloader.live)
}
```

리로드 워크플로를 애플리케이션 로직과 병렬로 실행하는 것도 가능.

```scala
object ServiceReloaderParallelExample extends ZIOAppDefault {
  def reloadWorkflow =
    ServiceReloader.reload[Counter].delay(5.seconds)

  def app: ZIO[Counter with ServiceReloader, ServiceReloader.Error, Unit] =
    for {
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.get.debug("Counter value")
      _ <- ZIO.sleep(6.seconds)
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.increment
      _ <- Counter.get.debug("Counter value")
    } yield ()

  def run = (app <&> reloadWorkflow).provide(
    Counter.reloadable,
    ServiceReloader.live
  )
}
```

#### 5.6 세 방식 비교

- `Reloadable.manual` — `reloadable.reload`를 명시적으로 호출. 코드가 다소 장황(boilerplate)하지만 리로드 시점을 완전히 제어 가능
- `Reloadable.auto` — `Schedule` 기반으로 자동 리로드. 정기적인 갱신에 적합
- `ServiceReloader` — `ServiceReloader.reload[A]`로 더 간결하게 사용 가능하지만 `zio-macros` 의존성 필요, 서비스에 직접 접근

---

### 6. 참고 자료

- [Introduction to Writing ZIO Services](https://zio.dev/reference/service-pattern/introduction)
- [Service Pattern](https://zio.dev/reference/service-pattern/)
- [The Three Laws of ZIO Environment](https://zio.dev/reference/service-pattern/the-three-laws-of-zio-environment)
- [Defining Polymorphic Services in ZIO](https://zio.dev/reference/service-pattern/defining-polymorphic-services-in-zio)
- [Reloadable Services](https://zio.dev/reference/service-pattern/reloadable-services)

---

## ZIO 설정 관리(Configuration)

> 원본: https://zio.dev/reference/configuration/

---

### 목차

1. [설정 시스템 개요(Overview)](#1-설정-시스템-개요overview)
2. [원시 설정(Primitive Configs)](#2-원시-설정primitive-configs)
3. [커스텀 설정 정의(Custom Configs)](#3-커스텀-설정-정의custom-configs)
4. [중첩 설정(Nested Configs)](#4-중첩-설정nested-configs)
5. [ConfigProvider(설정 백엔드)](#5-configprovider설정-백엔드)
6. [커스텀 ConfigProvider와 Runtime.setConfigProvider](#6-커스텀-configprovider와-runtimesetconfigprovider)
7. [테스트에서 설정 모킹하기](#7-테스트에서-설정-모킹하기)
8. [참고 자료](#8-참고-자료)

---

### 1. 설정 시스템 개요(Overview)

ZIO의 설정 시스템은 세 가지 요소로 구성.

1. **`Config[A]` 설명(description)** — 어떤 설정 데이터가 필요한지 선언적으로 기술하는 타입
2. **프론트엔드(front-end)** — `ZIO.config`로 애플리케이션이 설정을 로드
3. **백엔드(back-end)** — `ConfigProvider`가 실제 소스(환경 변수, 시스템 프로퍼티, 콘솔 등)로부터 데이터를 읽어들임

이 구조 덕분에 애플리케이션 코드는 설정이 "어디서" 오는지(환경 변수인지, 파일인지, 원격 설정 서버인지)에 신경 쓰지 않고 "무엇이" 필요한지만 선언 가능 → 소스 교체가 `ConfigProvider` 교체만으로 끝남.

---

### 2. 원시 설정(Primitive Configs)

`Config.string`, `Config.int` 같은 원시 설정 생성자로 개별 값을 선언하고, `ZIO.config`로 로드.

```scala
import zio._

object MainApp extends ZIOAppDefault {
  def run = {
    for {
      host <- ZIO.config(Config.string("host"))
      port <- ZIO.config(Config.int("port"))
      _    <- Console.printLine(s"Application started: $host:$port")
    } yield ()
  }
}
```

기본 `ConfigProvider`는 환경 변수와 시스템 프로퍼티를 순서대로 탐색.

```bash
HOST=localhost PORT=8080 sbt "runMain MainApp"
# 또는
sbt -Dhost=localhost -Dport=8080 "runMain MainApp"
```

---

### 3. 커스텀 설정 정의(Custom Configs)

#### 3.1 단순 데이터 타입

여러 원시 설정을 `++`로 결합하고 `map`으로 케이스 클래스에 매핑하여 커스텀 `Config` 인스턴스 정의.

```scala
case class HostPort(host: String, port: Int)

object HostPort {
  implicit val config: Config[HostPort] =
    (Config.string("host") ++ Config.int("port")).map { case (host, port) =>
      HostPort(host, port)
    }
}

for {
  config <- ZIO.config[HostPort]
  _      <- Console.printLine(s"Application started: $config")
} yield ()
```

#### 3.2 컬렉션 설정

`Config.listOf`로 여러 개의 설정 값을 리스트로 한 번에 로드 가능.

```scala
case class HostPorts(hostPorts: List[HostPort])

object HostPorts {
  implicit val config: Config[HostPorts] =
    Config.listOf(HostPort.config).map(HostPorts(_))
}

for {
  config <- ZIO.config[HostPorts]
  _      <- Console.printLine(s"Application started with:")
  _      <- ZIO.foreachDiscard(config.hostPorts)(e =>
              Console.printLine(s"  - http://${e.host}:${e.port}"))
} yield ()
```

```bash
HOST=host1,host2,host3 PORT=8080,8081,8082 sbt "runMain MainApp"
```

---

### 4. 중첩 설정(Nested Configs)

`nested`로 설정에 접두사(prefix)를 붙여 중첩 구조를 표현 가능.

```scala
case class ServiceConfig(hostPort: HostPort, timeout: Int)

object ServiceConfig {
  implicit val config: Config[ServiceConfig] =
    (HostPort.config.nested("hostport") ++ Config.int("timeout")).map {
      case (a, b) => ServiceConfig(a, b)
    }
}
```

이 정의는 다음 키들로 값을 조회.

- 환경 변수: `HOSTPORT_HOST`, `HOSTPORT_PORT`, `TIMEOUT`
- 시스템 프로퍼티: `hostport.host`, `hostport.port`, `timeout`

---

### 5. ConfigProvider(설정 백엔드)

`ConfigProvider`는 `Config[A]` 설명을 실제 값으로 채워 넣는 백엔드. ZIO가 기본으로 제공하는 프로바이더.

```scala
ConfigProvider.defaultProvider  // 환경변수 → 시스템 프로퍼티 순으로 탐색
ConfigProvider.envProvider      // 환경변수만 탐색
ConfigProvider.propsProvider    // 시스템 프로퍼티만 탐색
ConfigProvider.consoleProvider  // 콘솔 입력으로부터 값을 읽음
```

---

### 6. 커스텀 ConfigProvider와 Runtime.setConfigProvider

`Runtime.setConfigProvider`를 `bootstrap` 레이어에 설치하면 애플리케이션 전역의 설정 백엔드를 교체 가능.

```scala
import zio._

object MainAppScoped extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(ConfigProvider.consoleProvider)

  def run =
    for {
      host <- ZIO.config(Config.string("host"))
      port <- ZIO.config(Config.int("port"))
      _    <- Console.printLine(s"Application started: http://$host:$port")
    } yield ()
}
```

이 방식으로 실행 환경(로컬 개발, CI, 운영)에 따라 설정 소스(콘솔 입력, 환경 변수, 원격 설정 서버 등)를 자유롭게 교체 가능.

---

### 7. 테스트에서 설정 모킹하기

테스트에서는 `ConfigProvider.fromMap`으로 인메모리 맵 기반 프로바이더를 만들어 실제 환경 변수·시스템 프로퍼티 없이 설정을 모킹(mocking) 가능.

```scala
import zio._
import zio.test._

object MyServiceTest extends ZIOSpecDefault {
  val mockConfigProvider: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(
      ConfigProvider.fromMap(Map("timeout" -> "5s"))
    )

  def myService: ZIO[Any, Config.Error, Double] = ???

  override def spec = {
    val expected: Double = ???
    test("test myService") {
      for {
        result <- myService
      } yield assertTrue(result == expected)
    }
  }.provideLayer(mockConfigProvider)
}
```

`Runtime.setConfigProvider`로 만든 레이어를 `provideLayer`로 테스트 스펙에 주입 → 실제 시스템 환경에 전혀 의존하지 않는 결정론적(deterministic) 설정 테스트 작성 가능.

---

### 8. 참고 자료

- [Configuration | ZIO](https://zio.dev/reference/configuration/)
- [Config | ZIO](https://zio.dev/reference/configuration/config)
- [ConfigProvider | ZIO](https://zio.dev/reference/configuration/configprovider)
