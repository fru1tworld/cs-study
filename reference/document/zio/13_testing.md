# 테스팅: ZIO Test

> 원본: https://zio.dev/reference/test/

---

## 목차

1. [ZIO Test 소개(Introduction)](#1-zio-test-소개introduction)
2. [왜 ZIO Test인가(Why ZIO Test?)](#2-왜-zio-test인가why-zio-test)
3. [첫 번째 테스트 작성하기(Writing Our First Test)](#3-첫-번째-테스트-작성하기writing-our-first-test)
4. [Spec 데이터 타입(The Spec Data Type)](#4-spec-데이터-타입the-spec-data-type)
5. [단언(Assertions): 개요](#5-단언assertions-개요)
6. [스마트 단언(Smart Assertions)](#6-스마트-단언smart-assertions)
7. [클래식 단언(Classic Assertions)](#7-클래식-단언classic-assertions)
8. [테스트 애스펙트(Test Aspects)](#8-테스트-애스펙트test-aspects)
9. [프로퍼티 기반 테스트(Property-Based Testing): Gen과 check](#9-프로퍼티-기반-테스트property-based-testing-gen과-check)
10. [내장 제너레이터(Built-in Generators)](#10-내장-제너레이터built-in-generators)
11. [제너레이터의 동작 원리(How Generators Work)](#11-제너레이터의-동작-원리how-generators-work)
12. [동적 테스트 생성(Dynamic Test Generation)](#12-동적-테스트-생성dynamic-test-generation)
13. [테스트 서비스(Test Services)](#13-테스트-서비스test-services)
14. [참고 자료](#14-참고-자료)

---

## 1. ZIO Test 소개(Introduction)

ZIO Test는 "효과적인 프로그램(effectual programs)을 손쉽게 테스트할 수 있게 해 주는, 의존성이 전혀 없는(zero dependency) 테스팅 라이브러리"입니다. ZIO Test에서는 **모든 테스트가 불변 값(immutable values)**이며, 테스트가 ZIO와 긴밀하게 통합(tightly integrated)되어 있어, 효과적인 프로그램을 테스트하는 것이 순수한(pure) 프로그램을 테스트하는 것만큼이나 자연스럽습니다.

### 동기(Motivation)

전통적인 Scala 단언(assertion)은 단순한 값에는 잘 동작합니다.

```scala
assert(1 + 2 == 2 + 1)
assert("Hi" == "H" + "i")
```

그러나 함수형 효과(functional effects)를 테스트하는 것은 어렵습니다. 효과는 먼저 실행(execute)되어야 하기 때문에, 두 ZIO 효과를 표준 단언만으로 의미 있게 비교할 수 없습니다. 예를 들어 난수 생성기(random generator)를 테스트하려면 `unsafeRun`이 필요합니다.

```scala
val random = Unsafe.unsafe { implicit unsafe =>
  Runtime.default.unsafe.run(Random.nextIntBounded(10)).getOrThrowFiberFailure()
}
assert(random >= 0)
```

ZIO Test는 테스팅 맥락(context)에서 "효과를 일급 값(first-class values)"으로 다루는 프레임워크를 제공합니다.

### 핵심 설계 철학(Core Design Philosophy)

이 라이브러리는 테스트 자체를 평범한 값(ordinary values)으로 만드는 것을 핵심 목표로 삼습니다. 이로부터 비롯되는 주요 함의는 다음과 같습니다.

1. **비동기 테스트(Asynchronous Testing)**: 테스트가 `Future` 래퍼나 블로킹(blocking) 연산 없이도 ZIO 효과를 자연스럽게 다룹니다.
2. **네이티브 ZIO 통합(Native ZIO Integration)**: 재시도(retry), 타임아웃(timeout), 리소스 관리(resource management)를 ZIO의 기존 기능을 통해 기본 지원합니다.

ZIO Test는 다음 주제들을 다룹니다: 왜 ZIO Test인가, 설치, 첫 번째 테스트 작성, 테스트 실행, JUnit 통합, 단언(Assertions), 테스트 계층 구조 및 조직화, 레이어 공유(Layer Sharing), Spec, 테스트 서비스(Test Services), 테스트 애스펙트(Test Aspects), 동적 테스트 생성, 프로퍼티 테스트(Property Testing).

---

## 2. 왜 ZIO Test인가(Why ZIO Test?)

### 테스트 환경 제어(Test Environment Control)

ZIO Test는 `Clock`, `Console`, `System`, `Random` 같은 표준 서비스의 테스트 가능한(testable) 버전을 기본 제공합니다. `TestClock`을 사용하면 실제로 기다릴 필요 없이 시간을 조작할 수 있습니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion._

test("timeout") {
  for {
    fiber  <- ZIO.sleep(5.minutes).timeout(1.minute).fork
    _      <- TestClock.adjust(1.minute)
    result <- fiber.join
  } yield assertTrue(result.isEmpty)
}
```

문서에 기술된 대로, "타임아웃과 시간의 경과를 기다리는 대신, 우리는 테스트 안에서 시간을 조정(adjust)할 수 있습니다."

### 리소스 관리(Resource Management)

ZIO Test는 전통적인 before/after 훅(hook) 대신, 합성 가능한(composable) `ZLayer`를 설정(setup) 및 정리(teardown)에 사용합니다. 리소스는 개별 테스트 또는 전체 스위트(suite)에 스코프(scope)될 수 있습니다.

```scala
suite("a test suite with shared kafka layer")(test1, test2, test3)
  .provideCustomLayerShared(kafkaLayer)
```

이 방식은 "리소스를 테스트별 또는 스위트 전체에 걸쳐 획득(acquire)하고 해제(release)할 수 있게" 하며, 완전한 합성성을 보장합니다.

### 프로퍼티 기반 테스트(Property-Based Testing)

`check`와 제너레이터(generator) 클래스를 통한 기본 지원으로 프로퍼티 검증을 자동화할 수 있습니다.

```scala
val associativity = check(Gen.int, Gen.int, Gen.int) { (x, y, z) =>
  assertTrue(((x + y) + z) == (x + (y + z)))
}
```

실패(failure)는 진단을 쉽게 하기 위해 자동으로 최소 반례(minimal counterexample)로 축소(shrink)됩니다.

### 추가 이점(Additional Benefits)

- **서드파티 의존성 제로(Zero third-party dependencies)**: JVM, ScalaJS, Dotty, Scala Native 전반에서 동작합니다.
- **테스트 애스펙트(Test Aspects)**: `@@ timeout(60.seconds)`와 같은 합성 가능한 수정자(modifier)를 제공합니다.
- **명확한 리포팅(Clear Reporting)**: 실패 지점을 구체적으로 짚어 주는 상세한 단언 실패 정보를 제공합니다.

---

## 3. 첫 번째 테스트 작성하기(Writing Our First Test)

### 핵심 개념(Core Concept)

ZIO 테스트를 만들려면 `ZIOSpecDefault`를 **object로**(class가 아닙니다) 확장(extend)하고 `spec` 메서드를 구현해야 합니다. 이 트레이트(trait)는 `ZIOAppDefault`와 유사하게 동작하지만, 단일 애플리케이션 대신 테스트 스위트(test suite)를 제공합니다.

### 기본 구조(Basic Structure)

```scala
import zio.test._

object HelloWorldSpec extends ZIOSpecDefault {
  def spec =
    suite("HelloWorldSpec")(
      ??? // 모든 테스트가 여기에 들어갑니다
    )
}
```

**핵심 요구사항**: `ZIOSpecDefault`는 `spec`을 구현한 object가 확장해야 합니다. 그렇지 않으면 테스트 러너(test runner)가 테스트를 발견하지 못합니다.

### 완전한 예제(Complete Example)

```scala
import zio._
import zio.test._
import zio.test.Assertion._
import java.io.IOException
import HelloWorld._

object HelloWorld {
  def sayHello: ZIO[Any, IOException, Unit] =
    Console.printLine("Hello, World!")
}

object HelloWorldSpec extends ZIOSpecDefault {
  def spec = suite("HelloWorldSpec")(
    test("sayHello correctly displays output") {
      for {
        _      <- sayHello
        output <- TestConsole.output
      } yield assertTrue(output == Vector("Hello, World!\n"))
    }
  )
}
```

### 핵심 요소(Key Elements)

- **`suite()`**: 여러 테스트를 계층적으로(hierarchically) 묶습니다.
- **`test()`**: 개별 테스트 케이스(test case)를 정의합니다.
- **`assertTrue()`**: 불리언(boolean) 조건을 검증하는 단순한 단언입니다.
- **`TestConsole.output`**: 실제 입출력(I/O)에 영향을 주지 않고 콘솔 출력을 캡처합니다.

이 테스팅 프레임워크는 콘솔 출력 같은 효과를 자동으로 가로채어(intercept), 화면에 표시하는 대신 검증을 위해 버퍼링(buffering)합니다.

---

## 4. Spec 데이터 타입(The Spec Data Type)

ZIO Test에서 스펙(spec)은 다른 ZIO 데이터 타입과 마찬가지로 그저 값(values)입니다. `Spec` 데이터 타입은 `ZIO` 데이터 타입과 유사하게 환경 타입(environment type) `R`로 파라미터화(parameterized)되어 있습니다. 이는 스펙이 자신의 환경으로부터 서비스에 의존할 수 있게 해 줍니다.

### 테스트와 스위트의 관계(Test and Suite Relationship)

`test`와 `suite` 호출은 모두 `Spec` 값을 생성합니다. 다음 예제는 개별 `test` 호출과 `suite` 그룹화가 어떻게 `Spec` 인스턴스를 만들어 내는지 보여 줍니다.

```scala
suite("HelloWorldSpec")(
  test("sayHello correctly displays output") {
    for {
      _      <- sayHello
      output <- TestConsole.output
    } yield assertTrue(output == Vector("Hello, World!\n"))
  }
)
```

### 트리 구조(Tree Structure)

테스트와 스위트는 트리(tree) 구조를 형성합니다. `suite`는 다른 스위트나 테스트를 자식(children)으로 가질 수 있어, 테스트 계층 구조(test hierarchy)를 구성할 수 있습니다.

### 스펙 연산(Spec Operations)

스펙은 다른 ZIO 값들처럼 함수형 변환(functional transformation)을 지원합니다. 즉, 이러한 데이터 타입을 "필터링(filter)하고, 매핑(map)하거나, 조작(manipulate)"할 수 있습니다.

---

## 5. 단언(Assertions): 개요

### 핵심 개념(Core Concept)

`Assertion[A]`은 타입 `A`의 값이 특정 조건을 만족하는지 검증하는, 실행 가능한 검사(executable check)를 나타냅니다. 문서가 기술하듯이, 단언은 "코드에서 참(true)이어야 하는 프로퍼티에 대한 실행 가능한 검사"입니다.

기본 구조는 다음과 같습니다.

```scala
case class Assertion[-A](arrow: TestArrow[A, Boolean]) {
  def test(value: A): Boolean = ???
  def run(value: => A): TestResult = ???
}
```

### 단언 생성(Creating Assertions)

**기본 예제:**

```scala
import zio.test._
import zio.test.Assertion

def sut = 40 + 2
val assertion: Assertion[Int] = Assertion.equalTo[Int, Int](42)
assertion.test(sut) // true
```

`TestArrow`를 사용하여 커스텀 단언(custom assertions)을 만들 수도 있습니다.

```scala
val assertion: Assertion[Int] = Assertion(TestArrow.fromFunction(_ == 42))
assertion.test(sut) // true
```

### 논리 연산(Logical Operations)

단언은 논리곱(`&&`), 논리합(`||`), 부정(`!`)으로 합성할 수 있습니다.

```scala
val greaterThanZero: Assertion[Int] = Assertion.isPositive
val lessThanFive   : Assertion[Int] = Assertion.isLessThan(5)
val equalTo10      : Assertion[Int] = Assertion.equalTo(10)
val assertion: Assertion[Int] = greaterThanZero && lessThanFive || !equalTo10

val result: TestResult = assertion.run(10)
```

### 합성 가능한 중첩 단언(Composable Nested Assertions)

단언은 복잡한 타입에 대해 중첩 합성(nested composition)을 지원합니다.

**Option 예제:**

```scala
val assertion: Assertion[Option[Int]] = Assertion.isSome(Assertion.equalTo(5))
test("optional value is some(5)") {
  assert(Some(1 + 4))(assertion)
}
```

**깊게 중첩된 예제:**

```scala
test("either value is right(Some(5))") {
  assert(Right(Some(1 + 4)))(isRight(isSome(equalTo(5))))
}
```

이러한 합성은 `TestArrow`의 `>>>` 연산자를 통해 동작하며, 단언의 순차 합성(sequential composition)을 가능하게 합니다.

### 테스트 방식(Testing Methods)

두 가지 주요 접근법이 존재합니다.

1. **클래식 단언(Classic Assertions)**: 매크로(macro)를 사용하지 않는 전통적인 `assert` 및 `assertZIO` 메서드
2. **스마트 단언(Smart Assertions)**: 평범한 값과 ZIO 효과 모두에 대해 통합된 `assertTrue` 매크로 문법

### 주요 내장 단언(Key Built-in Assertions)

- `equalTo` — 동등성(equality) 검사
- `isSome` — `Option` 타입 검사
- `isRight` — `Either` 타입 검사
- `isPositive` — 수치 술어(numeric predicate)
- `isLessThan` — 비교 연산자
- `hasField` — 중첩 필드 접근(nested field access)

`Assertion`의 컴패니언 객체(companion object)는 미리 정의된 단언들의 포괄적인 라이브러리를 제공합니다.

---

## 6. 스마트 단언(Smart Assertions)

### 핵심 개념(Core Concept)

스마트 단언은 `assertTrue` 함수를 사용하며, 매크로 메커니즘을 통해 "평범한 값과 ZIO 효과를 모두" 통합적이고 읽기 좋은 방식으로 단언합니다.

> 거의 모든 경우에 개발자들은 클래식 단언 대신 스마트 단언을 사용하는 것이 권장됩니다.

### 평범한 값 단언하기(Asserting Ordinary Values)

기본 단언은 단순한 값에 직접 동작합니다.

```scala
import zio._
import zio.test.{test, _}

test("sum"){
  assertTrue(1 + 1 == 2)
}
```

하나의 `assertTrue` 안에 여러 단언을 결합할 수 있습니다.

```scala
test("multiple assertions"){
  assertTrue(
    true,
    1 + 1 == 2,
    Some(1 + 1) == Some(2)
  )
}
```

### ZIO 효과 단언하기(Asserting ZIO Effects)

매크로는 효과적인 계산(effectful computation)으로 확장됩니다.

```scala
test("updating ref") {
  for {
    r <- Ref.make(0)
    _ <- r.update(_ + 1)
    v <- r.get
  } yield assertTrue(v == 1)
}
```

이 접근 방식은 설정(setup), 실행(execution), 단언 검증(assertion verification)이라는 세 단계 테스트 패턴을 가능하게 합니다.

### 논리 연산자(Logical Operators)

스마트 단언은 다음 연산자를 통한 합성을 지원합니다.

- **`&&`** — 논리 AND
- **`||`** — 논리 OR
- **`!`** — 논리 NOT
- **`implies` / `==>`** — 조건부 단언(conditional assertion)
- **`iff` / `<==>`** — 쌍조건 단언(biconditional assertion)
- **`??`** — 커스텀 실패 메시지(custom failure messages)

논리 AND 예제:

```scala
test("&&") {
  check(Gen.int <*> Gen.int) { case (x: Int, y: Int) =>
    assertTrue(x + y == y + x) && assertTrue(x * y == y * x)
  }
}
```

### 중첩 값 테스트(Nested Value Testing)

`TestLens` 메커니즘은 다음과 같은 확장 메서드(extension methods)를 통해 깊은 인트로스펙션(introspection)을 가능하게 합니다.

- **`.some`** — `Option` 값 접근
- **`.right` / `.left`** — `Either` 값 접근
- **`.success` / `.failure`** — `Exit` 값 접근

중첩 값에 접근하는 예제:

```scala
test("assertion of multiple nested values (TestLens#right.some)") {
  val sut: Either[Error, Option[Int]] = Right(Some(40 + 2))
  assertTrue(sut.is(_.right.some) == 42)
}
```

### 커스텀 단언(Custom Assertions)

`CustomAssertion.make`로 도메인 특화 단언(domain-specific assertions)을 정의합니다.

```scala
val subject = CustomAssertion.make[Book] {
  case Textbook(subject) => Right(subject)
  case other => Left(s"Expected $other to be Textbook")
}
```

---

## 7. 클래식 단언(Classic Assertions)

### 핵심 함수(Core Functions)

- **`assert`**: `assert(value)(assertion)` 문법으로 평범한 값을 테스트하며, `TestResult`를 반환합니다.
- **`assertZIO`**: ZIO 효과를 테스트하기 위한 효과적인(effectful) 대응물로, 동일한 `assert(value)(assertion)` 패턴을 사용하되 값이 `ZIO`로 감싸집니다.

`assert(expression)(assertion)` 문법에서 첫 번째 부분은 계산 결과인 타입 `A`의 표현식이며, 두 번째 부분은 기대하는 단언인 타입 `Assertion[A]`입니다.

### 기본 사용 패턴(Basic Usage Patterns)

**평범한 값:**

```scala
import zio._
import zio.test.{test, _}

test("sum") {
  assert(1 + 1)(Assertion.equalTo(2))
}
```

**ZIO 효과:**

```scala
test("updating ref") {
  val value = for {
    r <- Ref.make(0)
    _ <- r.update(_ + 1)
    v <- r.get
  } yield v
  assertZIO(value)(Assertion.equalTo(1))
}
```

**for-컴프리헨션 스타일:**

```scala
test("updating ref") {
  for {
    r <- Ref.make(0)
    _ <- r.update(_ + 1)
    v <- r.get
  } yield assert(v)(Assertion.equalTo(v))
}
```

### 내장 단언 조합자(Built-in Assertion Combinators)

- **`equalTo`**: 동등성 비교
- **`isGreaterThanEqualTo`**: 수치 비교
- **`hasField`**: 객체 프로퍼티 테스트
- **`not`**: 부정 조합자
- **`fails`**: 효과가 실패하는지 테스트
- **`isSubtype`**: 오류 계층(error hierarchy)에 대한 타입 검사
- **`isSome` / `isRight`**: `Option` / `Either`의 성공 케이스
- **`isLeft`**: `Either`의 실패 케이스

### 중요한 안내(Important Note)

문서는 다음을 명시합니다: _"거의 모든 경우에 우리는 개발자가 클래식 단언 대신 스마트 단언을 사용할 것을 권장합니다."_ 클래식 단언은 스마트 단언이 충분하지 않은 경우에만 사용하십시오.

---

## 8. 테스트 애스펙트(Test Aspects)

### 테스트 애스펙트란 무엇인가(What Are Test Aspects?)

`TestAspect`는 "하나의 테스트를 다른 테스트로 변환하는, 환경(environment)이나 오류 타입(error type)을 확장할 수도 있는 다형적 함수(polymorphic function)"로 동작합니다. 테스트 동작을 수정하고 횡단 관심사(cross-cutting concerns)를 캡슐화하는 스펙 변환기(spec transformer)입니다.

### `@@` 연산자(The @@ Operator)

테스트 애스펙트는 `@@` 연산자를 사용하여 적용됩니다.

```scala
test("a single test") {
  ???
} @@ testAspect

suite("suite of multiple tests") {
  ???
} @@ testAspect
```

이 연산자는 애스펙트를 합성하고 연쇄(chain)할 수 있게 합니다. **순서가 매우 중요합니다(order matters significantly).** 애스펙트의 순서를 바꾸면 동작이 달라질 수 있습니다. 예를 들면 다음과 같습니다.

```scala
test("test") {
  assertTrue(true)
} @@ jvmOnly @@ repeat(Schedule.recurs(5))
```

### 내장 테스트 애스펙트(Built-in Test Aspects)

문서에 기록된 일반적인 애스펙트는 다음과 같습니다.

- **실행 제어(Execution Control)**: `timeout()`, `repeat()`, `eventually`, `nonFlaky`, `flaky`
- **플랫폼 특화(Platform-Specific)**: `jvmOnly`, `jsOnly`, `nativeOnly`
- **테스트 상태(Test State)**: `ignore`, `failing`
- **실행 모드(Execution Mode)**: `sequential`, `parallel`, `nondeterministic`, `silent`
- **서비스(Services)**: `withLiveClock`
- **생명 주기(Lifecycle)**: before/after/around 연산에 대한 애스펙트

여러 애스펙트를 결합한 실용 예제:

```scala
test("A flaky test that only works on the JVM") {
  assertTrue(false)
} @@ jvmOnly @@ eventually @@ timeout(20.nanos)
```

이 합성은 여러 변환(플랫폼 제약, 재시도 로직, 타임아웃 제약)을 순서대로 적용합니다.

### 8.1 타임아웃(Timeout)

`timeout` 테스트 애스펙트는 테스트 실행의 최대 지속 시간을 지정할 수 있게 합니다. 이 지속 시간을 초과하는 테스트는 즉시 취소되고 타임아웃 메시지와 함께 실패합니다. 문서에 따르면 "`timeout` 테스트 애스펙트는 지속 시간(duration)을 받아 각 테스트에 타임아웃을 적용합니다."

```scala
import zio._
import zio.test.{test, _}

test("effects can be safely interrupted") {
  for {
    _ <- ZIO.attempt(println("Still going ...")).forever
  } yield assertTrue(true)
} @@ TestAspect.timeout(1.second)
```

이 애스펙트는 ZIO의 인터럽션(interruption) 메커니즘과 통합됩니다. 테스트가 지정된 지속 시간보다 오래 실행되면 인터럽트(interrupt)되어 실패로 보고됩니다. 위 예제에서 `timeout(1.second)`를 적용하면 무한 루프가 1초 동안 실행된 후 애스펙트가 이를 종료하고 테스트를 실패로 표시합니다.

### 8.2 반복과 재시도(Repeat and Retry)

**`repeat`**: 스케줄(schedule)에 따라 테스트를 여러 번 실행합니다. 매 반복마다 통과해야만 테스트가 성공합니다.

```scala
import zio._
import zio.test.{ test, _ }

test("repeating a test based on the scheduler to ensure it passes every time") {
  ZIO.debug("repeating successful tests")
    .map(_ => assertTrue(true))
} @@ TestAspect.repeat(Schedule.recurs(5))
```

**`retry`**: 테스트가 간헐적으로 실패할 때, 제공된 스케줄에 따라 성공할 때까지 실행을 재시도합니다.

```scala
import zio._
import zio.test.{ test, _ }

test("retrying a failing test based on the schedule until it succeeds") {
  ZIO.debug("retrying a failing test")
    .map(_ => assertTrue(true))
} @@ TestAspect.retry(Schedule.recurs(5))
```

**`eventually`**: 실패하는 테스트를 통과할 때까지 무한정 재시도합니다.

```scala
import zio._
import zio.test.{ test, _ }

test("retrying a failing test until it succeeds") {
  ZIO.debug("retrying a failing test")
    .map(_ => assertTrue(true))
} @@ TestAspect.eventually
```

### 8.3 플래키 테스트와 논플래키 테스트(Flaky and Non-flaky Tests)

**`nonFlaky` 애스펙트**: 테스트를 여러 번(기본값 100회) 실행하여 일관성(consistency)을 보장합니다. "테스트를 여러 번 실행하여, 모든 회차가 통과하면 통과시키고, 그렇지 않으면 실패시킵니다." 동시성(concurrency)이나 경쟁 조건(race condition)을 다룰 때 테스트가 안정적으로 통과하는지 검증하는 데 유용합니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.TestAspect._

test("random value is always greater than zero") {
  for {
    random <- Random.nextIntBounded(100)
  } yield assertTrue(random > 0)
} @@ nonFlaky
```

**`flaky` 애스펙트**: "`TestAspect.flaky` 테스트 애스펙트는 테스트가 성공할 때까지 재시도합니다." 여러 회차의 일관된 통과를 요구하는 대신, 결국 통과할 때까지 재시도를 허용하는 반대 접근법입니다.

**핵심 차이**: `nonFlaky`는 모든 반복이 성공해야 합니다(엄격, strict). `flaky`는 성공할 때까지 재시도를 허용합니다(관대, lenient).

### 8.4 실패한 테스트 통과시키기(Passing Failed Tests)

**`failing` 애스펙트**: 테스트 결과를 반전(invert)시킵니다. 실패하는 테스트는 통과시키고, 통과하는 테스트는 실패시킵니다.

실패하는 테스트를 통과시키기:

```scala
test("passing a failing test") {
  assertTrue(false)
} @@ TestAspect.failing
```

통과하는 테스트를 실패시키기:

```scala
test("failing a passing test") {
  assertTrue(true)
} @@ TestAspect.failing
```

조건부 실패 처리(특정 실패에 대해서만 통과):

```scala
test("a test that will only pass on a specified failure") {
  ZIO.fail("Boom!").map(_ => assertTrue(true))
} @@ TestAspect.failing[String] {
  case TestFailure.Assertion(_, _) => true
  case TestFailure.Runtime(cause: Cause[String], _) => cause match {
    case Cause.Fail(value, _)
      if value == "Boom!" => true
    case _ => false
  }
}
```

### 8.5 테스트 무시하기(Ignoring Tests)

테스트 실행을 건너뛰는 주된 메커니즘은 `ignore` 테스트 애스펙트입니다.

```scala
import zio._
import zio.test.{test, _}

test("an ignored test") {
  assertTrue(false)
} @@ TestAspect.ignore
```

더 엄격한 테스트 관행을 강제하려면, 무시된 테스트를 포함하는 스위트에 `success` 애스펙트를 적용하면 됩니다. 이 경우 모든 무시된 테스트가 실패 처리됩니다.

```scala
suite("sample tests")(
  test("an ignored test") {
    assertTrue(false)
  } @@ TestAspect.ignore,
  test("another ignored test") {
    assertTrue(true)
  } @@ TestAspect.ignore
) @@ TestAspect.success
```

> 참고: 조건부 무시(예: `ifEnv`, `ifProp`)와 같은 조건부 애스펙트(Conditional Aspects)도 존재합니다.

### 8.6 테스트 구성하기(Configuring Tests)

테스트 동작을 조정하는 구성 애스펙트는 다음과 같습니다.

**`repeats(n: Int)`**: 테스트를 여러 번 실행하여 안정성을 보장합니다.

```scala
test("repeating a test") {
  ZIO.attempt("Repeating a test to ensure its stability")
    .debug
    .map(_ => assertTrue(true))
} @@ TestAspect.nonFlaky @@ TestAspect.repeats(5)
```

**`retries(n: Int)`**: "플래키 테스트를 재시도할 횟수를 지정한 값으로 설정하여 각 테스트를 실행합니다."

**`samples(n: Int)`**: 프로퍼티 기반 테스트의 반복 횟수를 조정합니다.

```scala
test("customized number of samples") {
  for {
    ref <- Ref.make(0)
    _ <- check(Gen.int)(_ => assertZIO(ref.update(_ + 1))(Assertion.anything))
    value <- ref.get
  } yield assertTrue(value == 50)
} @@ TestAspect.samples(50)
```

**`shrinks(n: Int)`**: "큰 실패를 최소화하기 위한 최대 축소(shrinking) 횟수"를 제어합니다.

---

## 9. 프로퍼티 기반 테스트(Property-Based Testing): Gen과 check

### 핵심 개념(Core Concept)

프로퍼티 기반 테스트는 전통적인 단위 테스트와 다릅니다. "개별 값을 테스트하고 그 결과를 단언하는" 대신, "테스트 대상 시스템(system under test)의 프로퍼티(properties)를 검증하는 것"에 의존합니다.

`Gen[R, A]`는 환경 `R`을 요구하는, 타입 `A`의 값을 생성하는 제너레이터(generator)를 나타냅니다. 제너레이터는 결정론적(deterministic) 및 비결정론적(non-deterministic, PRNG) 무작위 값을 생성하는 데 사용됩니다.

### 예제: 덧셈 함수(Addition Function)

`add` 함수가 만족해야 하는 수학적 프로퍼티를 식별하여 테스트합니다.

1. **교환 법칙(Commutative Property)**: `add(a, b) == add(b, a)`
2. **결합 법칙(Associative Property)**: `add(add(a, b), c) == add(a, add(b, c))`
3. **항등 법칙(Identity Property)**: `add(a, 0) == a`

```scala
import zio.test._
object AdditionSpec extends ZIOSpecDefault {
  def add(a: Int, b: Int): Int = ???
  def spec = suite("Add Spec")(
    test("add is commutative") {
      check(Gen.int, Gen.int) { (a, b) =>
        assertTrue(add(a, b) == add(b, a))
      }
    },
    test("add is associative") {
      check(Gen.int, Gen.int, Gen.int) { (a, b, c) =>
        assertTrue(add(add(a, b), c) == add(a, add(b, c)))
      }
    },
    test("add is identitive") {
      check(Gen.int) { a =>
        assertTrue(add(a, 0) == a)
      }
    }
  )
}
```

### 핵심 구성 요소(Key Components)

- **`Gen`**: 테스트 값을 생성하는 제너레이터 데이터 타입
- **`check`**: 제너레이터와 테스트 프로퍼티를 받는 함수
- **`Gen.int`**: 정수를 위한 내장 제너레이터

### 합성성과 축소(Composability and Shrinking)

`Gen` 데이터 타입은 합성 가능(composable)하므로, 제너레이터를 결합하여 더 복잡한 타입의 무작위 값을 생성할 수 있습니다. 예를 들어 `check(Gen.listOf(Gen.int))`를 사용하여 무작위로 생성된 정수 리스트에 대한 프로퍼티를 테스트할 수 있습니다.

ZIO Test의 모든 제너레이터는 내장 축소기(built-in shrinker)를 갖추고 있어, 프로퍼티가 실패하면 반례(counterexample)를 가장 단순한 형태로 축소하려고 시도합니다.

---

## 10. 내장 제너레이터(Built-in Generators)

ZIO Test 프레임워크는 `Gen` 컴패니언 객체를 통해 광범위한 제너레이터 지원을 제공합니다. 범주별 개요는 다음과 같습니다.

### 원시 타입(Primitive Types)

`Gen.int`, `Gen.string`, `Gen.boolean`, `Gen.float`, `Gen.double`, `Gen.bigInt`, `Gen.byte`, `Gen.bigDecimal`, `Gen.long`, `Gen.char`, `Gen.short`

### 문자 제너레이터(Character Generators)

`Gen.alphaChar`, `Gen.alphaNumericChar`, `Gen.asciiChar`, `Gen.unicodeChar`, `Gen.numericChar`, `Gen.printableChar`, `Gen.whitespaceChars`, `Gen.hexChar`, `Gen.hexCharLower`, `Gen.hexCharUpper`

### 문자열 제너레이터(String Generators)

- `Gen.stringBounded(min, max)(charGen)` — 크기가 제한된 문자열
- `Gen.stringN(size)(charGen)` — 고정 크기 문자열
- `Gen.string1` — 최소 1개 문자
- `Gen.alphaNumericString`, `Gen.alphaNumericStringBounded`
- `Gen.iso_8859_1`, `Gen.asciiString`

### 고정/상수 값(Fixed/Constant Values)

`Gen.const(value)`, `Gen.constSample(sample)`, `Gen.unit`, `Gen.throwable`, `Gen.empty`

### 값에서 선택하기(Selecting from Values)

- `Gen.elements(val1, val2, ...)` — 무작위 선택
- `Gen.fromIterable(collection)` — 결정론적 생성

### 컬렉션(Collections)

`Gen.setOf`, `Gen.setOf1`, `Gen.setOfBounded`, `Gen.setOfN`, 그리고 리스트(list), 벡터(vector), 청크(chunk), 맵(map)에 대한 동등한 제너레이터들

### 복합 타입(Compound Types)

```scala
val tuples: Gen[Any, (Int, Double)] =
  for {
    a <- Gen.int
    b <- Gen.double
  } yield (a, b)
```

`Gen.oneOf`, `Gen.option`, `Gen.some`, `Gen.none`, `Gen.either`, `Gen.collectAll`, `Gen.concatAll`

### 날짜/시간(Date/Time)

`Gen.dayOfWeek`, `Gen.month`, `Gen.year`, `Gen.instant`, `Gen.localDate`, `Gen.localDateTime`, `Gen.zonedDateTime`, `Gen.finiteDuration`

### 함수 제너레이터(Function Generators)

`Gen.function`, `Gen.functionWith`, `Gen.function2`~`Gen.functionN`, `Gen.partialFunction`

### ZIO 효과(ZIO Effects)

`Gen.successes`, `Gen.failures`, `Gen.died`, `Gen.causes`, `Gen.chained`, `Gen.concurrent`, `Gen.parallel`

### 고급(Advanced)

`Gen.sized`, `Gen.size`, `Gen.small`, `Gen.medium`, `Gen.large`, `Gen.suspend`, `Gen.unfoldGen`, `Gen.fromZIO`, `Gen.fromRandom`

---

## 11. 제너레이터의 동작 원리(How Generators Work)

### 핵심 개념(Core Concept)

`Gen[R, A]`는 "환경 `R`을 요구하는, 타입 `A`의 값을 생성하는 제너레이터"를 나타냅니다. 선택적 샘플(optional samples)의 스트림으로 인코딩되어 `ZStream[R, Nothing, A]`와 유사하게 동작합니다.

### 조합자(Combinators)

- **`map`**: `Gen(sample.map(f))` — 생성된 값을 변환합니다.
- **`flatMap`**: 의존적인 제너레이터를 연쇄(chaining)하는 데 사용합니다.
- **`const`**: `Gen(ZStream.succeed(a))` — 상수 값을 생성합니다.
- **`elements`**: 무작위 선택을 사용하여 지정된 값들로부터 생성합니다.
- **`fromIterable`**: 컬렉션으로부터 결정론적 제너레이터를 만듭니다.

### 핵심 연산(Key Operations)

- **`runCollect`**: 생성된 모든 값을 담은 `ZIO[Any, Nothing, List[Int]]`를 반환합니다.
- **`runCollectN`**: "필요한 만큼 제너레이터를 반복적으로 실행합니다."
- **`runHead`**: 첫 번째로 생성된 값을 반환합니다.

### 제너레이터 유형(Generator Types)

- **결정론적(Deterministic)**: `Gen.const(42)`, `Gen.fromIterable(List(1, 2, 3))`
- **무작위(Random)**: `Gen.boolean`, `Gen.int`, `Gen.elements(1, 2, 3)`

---

## 12. 동적 테스트 생성(Dynamic Test Generation)

ZIO에서 테스트는 동적(dynamic)입니다. 즉 "컴파일 타임(compile time)에 정적으로(statically) 정의될 필요가 없습니다."

외부 소스(예: CSV 데이터)로부터 테스트 데이터를 불러와 런타임에 테스트 스펙(spec)을 동적으로 생성할 수 있습니다. 불러온 파라미터를 기반으로 테스트를 구성하는 `makeTest` 함수를 사용하여 데이터를 개별 테스트 스펙으로 변환하는 것이 일반적인 패턴입니다.

---

## 13. 테스트 서비스(Test Services)

`environment` 패키지는 `TestClock`, `TestConsole`, `TestSystem`, `TestRandom` 모듈을 통해 표준 ZIO 환경 타입들의 테스트 가능한(testable) 버전을 제공합니다. ZIO Test를 사용하면 이 모두를 포함하는 `TestEnvironment`가 각 테스트에 자동으로 주입됩니다.

### 13.1 TestConsole

`TestConsole`은 표준 입출력(standard input and output)을 내부 버퍼(internal buffers)로 모델링하여, 콘솔과 상호작용하는 애플리케이션을 테스트할 수 있게 합니다.

**핵심 메서드:**

- **`feedLines`**: 이후의 `readLine` 호출이 반환할 미리 정의된 문자열로 입력 버퍼를 채워, 사용자 입력 시뮬레이션을 가능하게 합니다.
- **`output`**: 테스트 중 수행된 모든 `print` 및 `printLine` 연산으로 누적된 출력 버퍼 내용을 가져옵니다.
- **`clearInput` / `clearOutput`**: 테스트 섹션 사이에 상태를 리셋하기 위해 각각의 버퍼를 비웁니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion._

val consoleSuite = suite("ConsoleTest")(
  test("One can test output of console") {
    for {
      _              <- TestConsole.feedLines("Jimmy", "37")
      _              <- Console.printLine("What is your name?")
      name           <- Console.readLine
      _              <- Console.printLine("What is your age?")
      age            <- Console.readLine.map(_.toInt)
      questionVector <- TestConsole.output
      q1             = questionVector(0)
      q2             = questionVector(1)
    } yield {
      assertTrue(name == "Jimmy") &&
        assertTrue(age == 37) &&
        assertTrue(q1 == "What is your name?\n") &&
        assertTrue(q2 == "What is your age?\n")
    }
  }
)
```

이 접근 방식은 콘솔 테스트를 실제 I/O 연산으로부터 분리하여, 테스트를 결정론적이고 빠르게 만듭니다.

### 13.2 TestClock

`TestClock`은 실제 시간이 흐르기를 기다리지 않고, 시간의 경과를 포함하는 효과를 결정론적이고 효율적으로 테스트할 수 있게 합니다. `sleep` 및 관련 메서드로 스케줄된 효과들은 클록 시간(clock time)이 조정될 때 발동(trigger)됩니다.

**핵심 메커니즘:**

- `adjust()`와 `setTime()` 메서드로 클록 진행을 제어합니다.
- 특정 시간에 스케줄된 효과는 클록이 그 시간에 도달하면 자동으로 실행됩니다.
- 클록은 명시적으로 수정되기 전까지는 정지(static) 상태이며, 자동으로 진행하지 않습니다.

**포크 패턴(Forking Pattern):** 유용한 패턴은 "테스트 대상 효과를 포크(fork)하고, 클록 시간을 조정한 뒤, 기대한 효과가 수행되었는지 검증하는 것"입니다.

`TestClock`은 "00:00 1/1/70"으로 초기화되며, 스케줄된 효과를 발동시키려면 시간을 명시적으로 진행해야 합니다.

**예제 1 — 기본 시간 진행:**

```scala
import zio._
import zio.test.{test, _}
import java.util.concurrent.TimeUnit
import zio.Clock.currentTime
import zio.test.Assertion.isGreaterThanEqualTo

test("One can move time very fast") {
  for {
    startTime <- currentTime(TimeUnit.SECONDS)
    _         <- TestClock.adjust(1.minute)
    endTime   <- currentTime(TimeUnit.SECONDS)
  } yield assertTrue((endTime - startTime) >= 60L)
}
```

**예제 2 — 비동기 스케줄 효과:**

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion.equalTo

test("One can control time as he see fit") {
  for {
    promise <- Promise.make[Unit, Int]
    _       <- (ZIO.sleep(10.seconds) *> promise.succeed(1)).fork
    _       <- TestClock.adjust(10.seconds)
    readRef <- promise.await
  } yield assertTrue(1 == readRef)
}
```

**예제 3 — 복잡한 의존성과 실패 처리:**

```scala
import zio.test.Assertion._
import zio.test._
import zio.{test => _, _}

trait SchedulingService {
  def schedule(promise: Promise[Unit, Int]): ZIO[Any, Exception, Boolean]
}

trait LoggingService {
  def log(msg: String): ZIO[Any, Exception, Unit]
}

val schedulingLayer: ZLayer[LoggingService, Nothing, SchedulingService] =
  ZLayer.fromFunction { (loggingService: LoggingService) =>
    new SchedulingService {
      def schedule(promise: Promise[Unit, Int]): ZIO[Any, Exception, Boolean] =
        (ZIO.sleep(10.seconds) *> promise.succeed(1))
          .tap(b => loggingService.log(b.toString))
    }
  }

test("One can control time for failing effects too") {
  val failingLogger = ZLayer.succeed(new LoggingService {
    override def log(msg: String): ZIO[Any, Exception, Unit] = ZIO.fail(new Exception("BOOM"))
  })
  val layer = failingLogger >>> schedulingLayer
  val testCase =
    for {
      promise <- Promise.make[Unit, Int]
      result <- ZIO.serviceWithZIO[SchedulingService](_.schedule(promise)).exit.fork
      _ <- TestClock.adjust(10.seconds)
      readRef <- promise.await
      result <- result.join
    } yield assertTrue((1 == readRef) && result.isFailure)
  testCase.provideLayer(layer)
}
```

**예제 4 — 큐 기반 다중 값 테스트:**

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion.equalTo

test("zipLatest") {
  val s1 = ZStream.iterate(0)(_ + 1).schedule(Schedule.fixed(100.milliseconds))
  val s2 = ZStream.iterate(0)(_ + 1).schedule(Schedule.fixed(70.milliseconds))
  val s3 = s1.zipLatest(s2)
  for {
    q      <- Queue.unbounded[(Int, Int)]
    _      <- s3.foreach(q.offer).fork
    fiber  <- ZIO.collectAll(ZIO.replicate(4)(q.take)).fork
    _      <- TestClock.adjust(1.second)
    result <- fiber.join
  } yield assertTrue(result == List(0 -> 0, 0 -> 1, 1 -> 1, 1 -> 2))
}
```

**예제 5 — 라이브 클록 통합:**

```scala
import zio._
import zio.stream._
import zio.test.{test, _}
import zio.test.Assertion._
import zio.test.TestAspect._

test("live clock") {
  val stream = ZStream.iterate(0)(_ + 1).schedule(Schedule.spaced(1.second))
  val s1 = stream.take(30)
  val sink = ZSink.collectAll[Int]
  for {
    fiber <- TestClock.adjust(1.second).repeat(Schedule.spaced(10.milliseconds)).fork
    _ <- fiber.join
    runner <- s1.run(sink)
  } yield assert(runner.size)(equalTo(30))
} @@ TestAspect.withLiveClock
```

### 13.3 TestRandom

`TestRandom`은 무작위성(randomness)을 다루는 코드를 결정론적으로 테스트할 수 있게 하며, 두 가지 모드로 동작합니다.

**모드 1 — 시드 기반 의사 난수 생성(Seed-Based Pseudo-Random Generation):** `setSeed`로 초기 시드(seed)를 설정하면, 일관되고 재현 가능한(reproducible) 값 시퀀스를 얻을 수 있습니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion._

test("Use setSeed to generate stable values") {
  for {
    _ <- TestRandom.setSeed(27)
    r1 <- Random.nextLong
    r2 <- Random.nextLong
    r3 <- Random.nextLong
  } yield
    assertTrue(
      List(r1, r2, r3) == List[Long](
        -4947896108136290151L,
        -5264020926839611059L,
        -9135922664019402287L
      )
    )
}
```

**모드 2 — 미리 정의된 값 공급(Predefined Value Feeding):** `feedInts` 같은 메서드로 내부 버퍼를 채우면, 의사 난수를 생성하기 전에 버퍼의 값들이 먼저 소비됩니다.

```scala
test("One can provide its own list of ints") {
  for {
    _ <- TestRandom.feedInts(1, 9, 2, 8, 3, 7, 4, 6, 5)
    r1 <- Random.nextInt
    r2 <- Random.nextInt
    r3 <- Random.nextInt
    r4 <- Random.nextInt
    r5 <- Random.nextInt
    r6 <- Random.nextInt
    r7 <- Random.nextInt
    r8 <- Random.nextInt
    r9 <- Random.nextInt
  } yield assertTrue(
    List(1, 9, 2, 8, 3, 7, 4, 6, 5) ==
    List(r1, r2, r3, r4, r5, r6, r7, r8, r9)
  )
}
```

`clearInts`로 버퍼를 리셋하면 두 모드 간에 매끄럽게 전환할 수 있습니다.

### 13.4 TestSystem

`TestSystem`은 시스템 프로퍼티를 다루는 효과의 결정론적 테스트를 가능하게 합니다. 특히 환경 변수(environment variables)와 JVM 시스템 프로퍼티를 통한 애플리케이션 구성을 테스트하는 데 유용합니다. 실제 시스템에 영향을 주지 않고 환경 변수와 시스템 프로퍼티를 설정·접근할 수 있으며, "이러한 동작의 결과로 실제 환경 변수나 시스템 프로퍼티가 접근되거나 설정되지 않습니다."

```scala
import zio._
import zio.test._
import zio.test.Assertion._
for {
  _      <- TestSystem.putProperty("java.vm.name", "VM")
  result <- System.property("java.vm.name")
} yield assertTrue(result == Some("VM"))
```

**참조 메서드:**

- `putProperty` — JVM 시스템 프로퍼티 설정
- `putEnv` — 환경 변수 설정
- `setLineSeparator` — 줄 구분자(line separator) 설정

### 13.5 Live

`Live` 트레이트는 테스트 코드가 테스트 환경 대신 _라이브(live)_ 환경에 접근할 수 있게 합니다. 실제 콘솔 출력이나 실제 타이밍처럼 진짜 시스템 서비스가 필요한 연산에 활용됩니다.

**`Live.live()`**: 효과를 테스트 환경 대신 라이브 환경에서 실행합니다. 예를 들어 테스트에서 `Clock.currentTime()`을 호출하면 보통 `0L`(테스트 기본값)이 반환됩니다. 실제 현재 시간을 얻으려면 다음과 같이 합니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion._
import java.util.concurrent.TimeUnit

test("live can access real environment") {
  for {
    live <- Live.live(Clock.currentTime(TimeUnit.MILLISECONDS))
  } yield assertTrue(live != 0L)
}
```

**`Live.withLive()`**: 효과 자체는 테스트 환경에서 실행하되, 변환(transformation)에는 라이브 환경을 적용합니다. 테스트 연산에 실제 세계의 동작(예: 타임아웃)을 적용해야 할 때 유용합니다.

```scala
import zio._
import zio.test.{test, _}
import zio.test.Assertion._

val longRunningSUT = ZIO.attemptBlockingInterrupt {
  Thread.sleep(10000) // 긴 블로킹 연산을 시뮬레이션
}

test("withLive provides real environment to a single part of an effect") {
  assertZIO(Live.withLive(longRunningSUT)(_.timeout(3.seconds)))(anything)
}
```

이 접근 방식은 타임아웃 연산을 테스트 시간이 아닌 실제 시간에 대해 실행합니다.

### 13.6 TestConfig

`TestConfig` 서비스는 네 가지 주요 테스팅 구성을 관리합니다.

1. **Repeats** — "테스트가 안정적인지 보장하기 위해 테스트를 반복할 횟수"
2. **Retries** — "플래키 테스트를 재시도할 횟수"
3. **Samples** — "무작위 변수(random variable)를 검사하기에 충분한 샘플 수"
4. **Shrinks** — "큰 실패를 최소화하기 위한 최대 축소(shrinking) 횟수"

라이브 구현이 제공하는 기본값(defaults)은 다음과 같습니다.

```
TestConfig.live(
  repeats0 = 100,
  retries0 = 100,
  samples0 = 200,
  shrinks0 = 1000
)
```

이 설정들은 표준 테스트 환경에서 ZIO 테스트를 실행할 때 자동으로 적용됩니다. 대부분의 경우 기본값을 그대로 사용하지만, 특정 테스팅 요구사항에 맞게 커스텀 구성도 가능합니다.

### 13.7 Sized

`Sized` 서비스는 크기가 지정된 제너레이터(sized generators)가 ZIO Test 환경에서 크기(size) 파라미터를 읽을 수 있게 합니다.

**핵심 인터페이스:**

```scala
trait Sized extends Serializable {
  def size: UIO[Int]
  def withSize[R, E, A](size: Int)(zio: ZIO[R, E, A]): ZIO[R, E, A]
}
```

**`Sized.size`**: 환경으로부터 현재 크기 값을 가져옵니다. `Gen.sized` 같은 크기 지정 제너레이터에 의해 내부적으로 사용됩니다.

```scala
object Gen {
  def sized[R, A](f: Int => Gen[R, A]): Gen[R, A] = ???
}
```

크기가 지정된 정수 제너레이터 생성 예제:

```scala
import zio._
import zio.test._

val sizedInts: Gen[Any, Int] =
  Gen.sized(Gen.int(0, _))
```

샘플 값 수집:

```scala
val samples: UIO[List[Int]] =
  sizedInts.runCollectN(5).debug
```

**`Sized.withSize`**: 효과를 실행하는 동안 크기 값을 일시적으로 수정합니다.

```scala
object Sized {
  def withSize[R, E, A](size: Int)(zio: ZIO[R, E, A]): ZIO[R, E, A] = ???
}
```

사용 예제:

```scala
val effect: UIO[String] = ZIO.succeed("effect")
val sizedEffect: UIO[String] = Sized.withSize(10)(effect)
```

ZIO Test는 편의 래퍼(convenience wrapper)로 `TestAspect.size`를 제공합니다.

```scala
object SizedSpec extends ZIOSpecDefault {
  def spec =
    suite("sized") {
      test("bounded int generator shouldn't cross its boundaries") {
        check(Gen.sized(Gen.int(0, _))) { n =>
          assertTrue(n >= 0 && n <= 200)
        }
      } @@ TestAspect.size(200)
    }
}
```

---

## 14. 참고 자료

- [ZIO Test (전체 개요)](https://zio.dev/reference/test/)
- [Why ZIO Test?](https://zio.dev/reference/test/why-zio-test/)
- [Writing Our First Test](https://zio.dev/reference/test/writing-our-first-test)
- [Spec](https://zio.dev/reference/test/spec/)
- [Assertions (Overview)](https://zio.dev/reference/test/assertions/)
- [Smart Assertions](https://zio.dev/reference/test/assertions/smart-assertions/)
- [Classic Assertions](https://zio.dev/reference/test/assertions/classic-assertions/)
- [Introduction to Test Aspects](https://zio.dev/reference/test/aspects/)
- [Configuring Tests](https://zio.dev/reference/test/aspects/configuring-tests/)
- [Passing Failed Tests](https://zio.dev/reference/test/aspects/passing-failed-tests/)
- [Flaky and Non-flaky Tests](https://zio.dev/reference/test/aspects/flaky-and-non-flaky-tests/)
- [Ignoring Tests](https://zio.dev/reference/test/aspects/ignoring-tests/)
- [Timing Out Tests](https://zio.dev/reference/test/aspects/timing-out-tests/)
- [Repeat and Retry](https://zio.dev/reference/test/aspects/repeat-and-retry/)
- [Property-Based Testing](https://zio.dev/reference/test/property-testing/)
- [Getting Started With Property Checking](https://zio.dev/reference/test/property-testing/getting-started/)
- [Built-in Generators](https://zio.dev/reference/test/property-testing/built-in-generators/)
- [How Generators Work](https://zio.dev/reference/test/property-testing/how-generators-work/)
- [Shrinking](https://zio.dev/reference/test/property-testing/shrinking/)
- [Dynamic Test Generation](https://zio.dev/reference/test/dynamic-test-generation)
- [TestConsole](https://zio.dev/reference/test/services/console)
- [TestClock](https://zio.dev/reference/test/services/clock)
- [TestRandom](https://zio.dev/reference/test/services/random)
- [TestSystem](https://zio.dev/reference/test/services/system)
- [Live](https://zio.dev/reference/test/services/live)
- [TestConfig](https://zio.dev/reference/test/services/config)
- [Sized](https://zio.dev/reference/test/services/sized)
