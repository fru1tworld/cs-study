# ZIO 테스팅과 메트릭·관찰 가능성

## 테스팅: ZIO Test

> 원본: https://zio.dev/reference/test/

---

### 목차

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

### 1. ZIO Test 소개(Introduction)

ZIO Test: 효과적인 프로그램(effectual programs)을 손쉽게 테스트할 수 있게 해주는, 의존성이 전혀 없는(zero dependency) 테스팅 라이브러리.

- 모든 테스트가 불변 값(immutable values)임
- 테스트가 ZIO와 긴밀하게 통합(tightly integrated)됨 → 효과적인 프로그램 테스트가 순수한(pure) 프로그램 테스트만큼 자연스러움

#### 동기(Motivation)

전통적인 Scala 단언(assertion) → 단순한 값에는 잘 동작함.

```scala
assert(1 + 2 == 2 + 1)
assert("Hi" == "H" + "i")
```

함수형 효과(functional effects) 테스트는 어려움 → 효과는 먼저 실행(execute)되어야 함 → 두 ZIO 효과를 표준 단언만으로 의미 있게 비교 불가. 예: 난수 생성기(random generator) 테스트 시 `unsafeRun` 필요.

```scala
val random = Unsafe.unsafe { implicit unsafe =>
  Runtime.default.unsafe.run(Random.nextIntBounded(10)).getOrThrowFiberFailure()
}
assert(random >= 0)
```

ZIO Test → 테스팅 맥락(context)에서 "효과를 일급 값(first-class values)"으로 다루는 프레임워크 제공.

#### 핵심 설계 철학(Core Design Philosophy)

이 라이브러리의 핵심 목표: 테스트 자체를 평범한 값(ordinary values)으로 만드는 것. 주요 함의:

1. 비동기 테스트(Asynchronous Testing): 테스트가 `Future` 래퍼나 블로킹(blocking) 연산 없이도 ZIO 효과를 자연스럽게 다룸
2. 네이티브 ZIO 통합(Native ZIO Integration): 재시도(retry)·타임아웃(timeout)·리소스 관리(resource management)를 ZIO의 기존 기능으로 기본 지원

ZIO Test 다루는 주제:

- 왜 ZIO Test인가
- 설치
- 첫 번째 테스트 작성
- 테스트 실행
- JUnit 통합
- 단언(Assertions)
- 테스트 계층 구조 및 조직화
- 레이어 공유(Layer Sharing)
- Spec
- 테스트 서비스(Test Services)
- 테스트 애스펙트(Test Aspects)
- 동적 테스트 생성
- 프로퍼티 테스트(Property Testing)

---

### 2. 왜 ZIO Test인가(Why ZIO Test?)

#### 테스트 환경 제어(Test Environment Control)

ZIO Test → `Clock`, `Console`, `System`, `Random` 같은 표준 서비스의 테스트 가능한(testable) 버전 기본 제공. `TestClock` 사용 시 실제로 기다릴 필요 없이 시간 조작 가능.

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

핵심: 타임아웃과 시간 경과를 기다리는 대신 → 테스트 안에서 시간을 조정(adjust) 가능.

#### 리소스 관리(Resource Management)

ZIO Test → 전통적인 before/after 훅(hook) 대신, 합성 가능한(composable) `ZLayer`를 설정(setup)·정리(teardown)에 사용. 리소스는 개별 테스트 또는 전체 스위트(suite)에 스코프(scope) 가능.

```scala
suite("a test suite with shared kafka layer")(test1, test2, test3)
  .provideCustomLayerShared(kafkaLayer)
```

- 리소스를 테스트별 또는 스위트 전체에 걸쳐 획득(acquire)·해제(release) 가능
- 완전한 합성성 보장

#### 프로퍼티 기반 테스트(Property-Based Testing)

`check`와 제너레이터(generator) 클래스를 통한 기본 지원으로 프로퍼티 검증 자동화 가능.

```scala
val associativity = check(Gen.int, Gen.int, Gen.int) { (x, y, z) =>
  assertTrue(((x + y) + z) == (x + (y + z)))
}
```

실패(failure) → 진단을 쉽게 하기 위해 자동으로 최소 반례(minimal counterexample)로 축소(shrink)됨.

#### 추가 이점(Additional Benefits)

- 서드파티 의존성 제로(Zero third-party dependencies): JVM·ScalaJS·Dotty·Scala Native 전반에서 동작
- 테스트 애스펙트(Test Aspects): `@@ timeout(60.seconds)`와 같은 합성 가능한 수정자(modifier) 제공
- 명확한 리포팅(Clear Reporting): 실패 지점을 구체적으로 짚어주는 상세한 단언 실패 정보 제공

---

### 3. 첫 번째 테스트 작성하기(Writing Our First Test)

#### 핵심 개념(Core Concept)

ZIO 테스트 작성 → `ZIOSpecDefault`를 object로(class 아님) 확장(extend)하고 `spec` 메서드 구현 필요. 이 트레이트(trait)는 `ZIOAppDefault`와 유사하게 동작 → 단일 애플리케이션 대신 테스트 스위트(test suite) 제공.

#### 기본 구조(Basic Structure)

```scala
import zio.test._

object HelloWorldSpec extends ZIOSpecDefault {
  def spec =
    suite("HelloWorldSpec")(
      ??? // 모든 테스트가 여기에 들어갑니다
    )
}
```

핵심 요구사항: `ZIOSpecDefault`는 `spec`을 구현한 object가 확장해야 함 → 그렇지 않으면 테스트 러너(test runner)가 테스트를 발견하지 못함.

#### 완전한 예제(Complete Example)

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

#### 핵심 요소(Key Elements)

- `suite()`: 여러 테스트를 계층적으로(hierarchically) 묶음
- `test()`: 개별 테스트 케이스(test case) 정의
- `assertTrue()`: 불리언(boolean) 조건을 검증하는 단순한 단언
- `TestConsole.output`: 실제 입출력(I/O)에 영향을 주지 않고 콘솔 출력을 캡처

이 테스팅 프레임워크는 콘솔 출력 같은 효과를 자동으로 가로채어(intercept) → 화면에 표시하는 대신 검증을 위해 버퍼링(buffering)함.

---

### 4. Spec 데이터 타입(The Spec Data Type)

ZIO Test에서 스펙(spec) = 다른 ZIO 데이터 타입과 마찬가지로 그저 값(values)임. `Spec` 데이터 타입은 `ZIO` 데이터 타입과 유사하게 환경 타입(environment type) `R`로 파라미터화(parameterized)됨 → 스펙이 자신의 환경으로부터 서비스에 의존 가능.

#### 테스트와 스위트의 관계(Test and Suite Relationship)

`test`와 `suite` 호출 모두 `Spec` 값 생성. 아래 예제 → 개별 `test` 호출과 `suite` 그룹화가 어떻게 `Spec` 인스턴스를 만드는지 보여줌.

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

#### 트리 구조(Tree Structure)

테스트와 스위트 → 트리(tree) 구조 형성. `suite`는 다른 스위트나 테스트를 자식(children)으로 가질 수 있음 → 테스트 계층 구조(test hierarchy) 구성 가능.

#### 스펙 연산(Spec Operations)

스펙은 다른 ZIO 값들처럼 함수형 변환(functional transformation) 지원 → 필터링(filter)·매핑(map)·조작(manipulate) 가능.

---

### 5. 단언(Assertions): 개요

#### 핵심 개념(Core Concept)

`Assertion[A]`: 타입 `A`의 값이 특정 조건을 만족하는지 검증하는, 실행 가능한 검사(executable check)를 나타냄. 코드에서 참(true)이어야 하는 프로퍼티에 대한 실행 가능한 검사.

기본 구조:

```scala
case class Assertion[-A](arrow: TestArrow[A, Boolean]) {
  def test(value: A): Boolean = ???
  def run(value: => A): TestResult = ???
}
```

#### 단언 생성(Creating Assertions)

기본 예제:

```scala
import zio.test._
import zio.test.Assertion

def sut = 40 + 2
val assertion: Assertion[Int] = Assertion.equalTo[Int, Int](42)
assertion.test(sut) // true
```

`TestArrow`로 커스텀 단언(custom assertions) 생성 가능.

```scala
val assertion: Assertion[Int] = Assertion(TestArrow.fromFunction(_ == 42))
assertion.test(sut) // true
```

#### 논리 연산(Logical Operations)

단언은 논리곱(`&&`)·논리합(`||`)·부정(`!`)으로 합성 가능.

```scala
val greaterThanZero: Assertion[Int] = Assertion.isPositive
val lessThanFive   : Assertion[Int] = Assertion.isLessThan(5)
val equalTo10      : Assertion[Int] = Assertion.equalTo(10)
val assertion: Assertion[Int] = greaterThanZero && lessThanFive || !equalTo10

val result: TestResult = assertion.run(10)
```

#### 합성 가능한 중첩 단언(Composable Nested Assertions)

단언 → 복잡한 타입에 대해 중첩 합성(nested composition) 지원.

Option 예제:

```scala
val assertion: Assertion[Option[Int]] = Assertion.isSome(Assertion.equalTo(5))
test("optional value is some(5)") {
  assert(Some(1 + 4))(assertion)
}
```

깊게 중첩된 예제:

```scala
test("either value is right(Some(5))") {
  assert(Right(Some(1 + 4)))(isRight(isSome(equalTo(5))))
}
```

이러한 합성은 `TestArrow`의 `>>>` 연산자를 통해 동작 → 단언의 순차 합성(sequential composition) 가능.

#### 테스트 방식(Testing Methods)

두 가지 주요 접근법:

1. 클래식 단언(Classic Assertions): 매크로(macro)를 사용하지 않는 전통적인 `assert` 및 `assertZIO` 메서드
2. 스마트 단언(Smart Assertions): 평범한 값과 ZIO 효과 모두에 대해 통합된 `assertTrue` 매크로 문법

#### 주요 내장 단언(Key Built-in Assertions)

- `equalTo` — 동등성(equality) 검사
- `isSome` — `Option` 타입 검사
- `isRight` — `Either` 타입 검사
- `isPositive` — 수치 술어(numeric predicate)
- `isLessThan` — 비교 연산자
- `hasField` — 중첩 필드 접근(nested field access)

`Assertion`의 컴패니언 객체(companion object) → 미리 정의된 단언들의 포괄적인 라이브러리 제공.

---

### 6. 스마트 단언(Smart Assertions)

#### 핵심 개념(Core Concept)

스마트 단언 → `assertTrue` 함수 사용, 매크로 메커니즘을 통해 "평범한 값과 ZIO 효과를 모두" 통합적이고 읽기 좋은 방식으로 단언.

> 거의 모든 경우 개발자들은 클래식 단언 대신 스마트 단언 사용 권장.

#### 평범한 값 단언하기(Asserting Ordinary Values)

기본 단언 → 단순한 값에 직접 동작.

```scala
import zio._
import zio.test.{test, _}

test("sum"){
  assertTrue(1 + 1 == 2)
}
```

하나의 `assertTrue` 안에 여러 단언 결합 가능.

```scala
test("multiple assertions"){
  assertTrue(
    true,
    1 + 1 == 2,
    Some(1 + 1) == Some(2)
  )
}
```

#### ZIO 효과 단언하기(Asserting ZIO Effects)

매크로는 효과적인 계산(effectful computation)으로 확장됨.

```scala
test("updating ref") {
  for {
    r <- Ref.make(0)
    _ <- r.update(_ + 1)
    v <- r.get
  } yield assertTrue(v == 1)
}
```

이 접근 방식 → 설정(setup)·실행(execution)·단언 검증(assertion verification)이라는 세 단계 테스트 패턴 가능.

#### 논리 연산자(Logical Operators)

스마트 단언은 다음 연산자를 통한 합성 지원.

- `&&` — 논리 AND
- `||` — 논리 OR
- `!` — 논리 NOT
- `implies` / `==>` — 조건부 단언(conditional assertion)
- `iff` / `<==>` — 쌍조건 단언(biconditional assertion)
- `??` — 커스텀 실패 메시지(custom failure messages)

논리 AND 예제:

```scala
test("&&") {
  check(Gen.int <*> Gen.int) { case (x: Int, y: Int) =>
    assertTrue(x + y == y + x) && assertTrue(x * y == y * x)
  }
}
```

#### 중첩 값 테스트(Nested Value Testing)

`TestLens` 메커니즘 → 다음 확장 메서드(extension methods)를 통해 깊은 인트로스펙션(introspection) 가능.

- `.some` — `Option` 값 접근
- `.right` / `.left` — `Either` 값 접근
- `.success` / `.failure` — `Exit` 값 접근

중첩 값에 접근하는 예제:

```scala
test("assertion of multiple nested values (TestLens#right.some)") {
  val sut: Either[Error, Option[Int]] = Right(Some(40 + 2))
  assertTrue(sut.is(_.right.some) == 42)
}
```

#### 커스텀 단언(Custom Assertions)

`CustomAssertion.make`로 도메인 특화 단언(domain-specific assertions) 정의.

```scala
val subject = CustomAssertion.make[Book] {
  case Textbook(subject) => Right(subject)
  case other => Left(s"Expected $other to be Textbook")
}
```

---

### 7. 클래식 단언(Classic Assertions)

#### 핵심 함수(Core Functions)

- `assert`: `assert(value)(assertion)` 문법으로 평범한 값을 테스트, `TestResult` 반환
- `assertZIO`: ZIO 효과를 테스트하기 위한 효과적인(effectful) 대응물 — 동일한 `assert(value)(assertion)` 패턴을 사용하되 값이 `ZIO`로 감싸짐

`assert(expression)(assertion)` 문법 구성:

- 첫 번째 부분: 계산 결과인 타입 `A`의 표현식
- 두 번째 부분: 기대하는 단언인 타입 `Assertion[A]`

#### 기본 사용 패턴(Basic Usage Patterns)

평범한 값:

```scala
import zio._
import zio.test.{test, _}

test("sum") {
  assert(1 + 1)(Assertion.equalTo(2))
}
```

ZIO 효과:

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

for-컴프리헨션 스타일:

```scala
test("updating ref") {
  for {
    r <- Ref.make(0)
    _ <- r.update(_ + 1)
    v <- r.get
  } yield assert(v)(Assertion.equalTo(v))
}
```

#### 내장 단언 조합자(Built-in Assertion Combinators)

- `equalTo`: 동등성 비교
- `isGreaterThanEqualTo`: 수치 비교
- `hasField`: 객체 프로퍼티 테스트
- `not`: 부정 조합자
- `fails`: 효과가 실패하는지 테스트
- `isSubtype`: 오류 계층(error hierarchy)에 대한 타입 검사
- `isSome` / `isRight`: `Option` / `Either`의 성공 케이스
- `isLeft`: `Either`의 실패 케이스

#### 중요한 안내(Important Note)

문서 명시: "거의 모든 경우 개발자가 클래식 단언 대신 스마트 단언을 사용할 것을 권장." 클래식 단언은 스마트 단언이 충분하지 않은 경우에만 사용.

---

### 8. 테스트 애스펙트(Test Aspects)

#### 테스트 애스펙트란 무엇인가(What Are Test Aspects?)

`TestAspect`: "하나의 테스트를 다른 테스트로 변환하는, 환경(environment)이나 오류 타입(error type)을 확장할 수도 있는 다형적 함수(polymorphic function)"로 동작. 테스트 동작을 수정하고 횡단 관심사(cross-cutting concerns)를 캡슐화하는 스펙 변환기(spec transformer).

#### `@@` 연산자(The @@ Operator)

테스트 애스펙트는 `@@` 연산자를 사용하여 적용.

```scala
test("a single test") {
  ???
} @@ testAspect

suite("suite of multiple tests") {
  ???
} @@ testAspect
```

- 이 연산자는 애스펙트를 합성하고 연쇄(chain) 가능
- 순서가 매우 중요 → 애스펙트의 순서를 바꾸면 동작이 달라질 수 있음

```scala
test("test") {
  assertTrue(true)
} @@ jvmOnly @@ repeat(Schedule.recurs(5))
```

#### 내장 테스트 애스펙트(Built-in Test Aspects)

- 실행 제어(Execution Control): `timeout()`, `repeat()`, `eventually`, `nonFlaky`, `flaky`
- 플랫폼 특화(Platform-Specific): `jvmOnly`, `jsOnly`, `nativeOnly`
- 테스트 상태(Test State): `ignore`, `failing`
- 실행 모드(Execution Mode): `sequential`, `parallel`, `nondeterministic`, `silent`
- 서비스(Services): `withLiveClock`
- 생명 주기(Lifecycle): before/after/around 연산에 대한 애스펙트

여러 애스펙트를 결합한 실용 예제:

```scala
test("A flaky test that only works on the JVM") {
  assertTrue(false)
} @@ jvmOnly @@ eventually @@ timeout(20.nanos)
```

이 합성 → 여러 변환(플랫폼 제약·재시도 로직·타임아웃 제약)을 순서대로 적용.

#### 8.1 타임아웃(Timeout)

`timeout` 테스트 애스펙트 → 테스트 실행의 최대 지속 시간 지정 가능. 이 지속 시간 초과 시 즉시 취소되고 타임아웃 메시지와 함께 실패. `timeout` 테스트 애스펙트는 지속 시간(duration)을 받아 각 테스트에 타임아웃을 적용함.

```scala
import zio._
import zio.test.{test, _}

test("effects can be safely interrupted") {
  for {
    _ <- ZIO.attempt(println("Still going ...")).forever
  } yield assertTrue(true)
} @@ TestAspect.timeout(1.second)
```

이 애스펙트는 ZIO의 인터럽션(interruption) 메커니즘과 통합됨 → 테스트가 지정된 지속 시간보다 오래 실행되면 인터럽트(interrupt)되어 실패로 보고됨. 위 예제 → `timeout(1.second)` 적용 시 무한 루프가 1초 동안 실행된 후 애스펙트가 종료하고 테스트를 실패로 표시.

#### 8.2 반복과 재시도(Repeat and Retry)

- `repeat`: 스케줄(schedule)에 따라 테스트를 여러 번 실행 → 매 반복마다 통과해야만 테스트 성공

```scala
import zio._
import zio.test.{ test, _ }

test("repeating a test based on the scheduler to ensure it passes every time") {
  ZIO.debug("repeating successful tests")
    .map(_ => assertTrue(true))
} @@ TestAspect.repeat(Schedule.recurs(5))
```

- `retry`: 테스트가 간헐적으로 실패할 때, 제공된 스케줄에 따라 성공할 때까지 실행 재시도

```scala
import zio._
import zio.test.{ test, _ }

test("retrying a failing test based on the schedule until it succeeds") {
  ZIO.debug("retrying a failing test")
    .map(_ => assertTrue(true))
} @@ TestAspect.retry(Schedule.recurs(5))
```

- `eventually`: 실패하는 테스트를 통과할 때까지 무한정 재시도

```scala
import zio._
import zio.test.{ test, _ }

test("retrying a failing test until it succeeds") {
  ZIO.debug("retrying a failing test")
    .map(_ => assertTrue(true))
} @@ TestAspect.eventually
```

#### 8.3 플래키 테스트와 논플래키 테스트(Flaky and Non-flaky Tests)

`nonFlaky` 애스펙트: 테스트를 여러 번(기본값 100회) 실행하여 일관성(consistency) 보장. 모든 회차가 통과하면 통과, 그렇지 않으면 실패. 동시성(concurrency)이나 경쟁 조건(race condition)을 다룰 때 테스트가 안정적으로 통과하는지 검증할 때 유용.

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

`flaky` 애스펙트: 테스트가 성공할 때까지 재시도. 여러 회차의 일관된 통과를 요구하는 대신, 결국 통과할 때까지 재시도를 허용하는 반대 접근법.

핵심 차이:

- `nonFlaky` — 모든 반복이 성공해야 함(엄격, strict)
- `flaky` — 성공할 때까지 재시도를 허용(관대, lenient)

#### 8.4 실패한 테스트 통과시키기(Passing Failed Tests)

`failing` 애스펙트: 테스트 결과를 반전(invert). 실패하는 테스트는 통과시키고, 통과하는 테스트는 실패시킴.

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

#### 8.5 테스트 무시하기(Ignoring Tests)

테스트 실행을 건너뛰는 주된 메커니즘: `ignore` 테스트 애스펙트.

```scala
import zio._
import zio.test.{test, _}

test("an ignored test") {
  assertTrue(false)
} @@ TestAspect.ignore
```

더 엄격한 테스트 관행을 강제하려면 → 무시된 테스트를 포함하는 스위트에 `success` 애스펙트 적용. 이 경우 모든 무시된 테스트가 실패 처리됨.

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

> 참고: 조건부 무시(예: `ifEnv`, `ifProp`)와 같은 조건부 애스펙트(Conditional Aspects)도 존재.

#### 8.6 테스트 구성하기(Configuring Tests)

테스트 동작을 조정하는 구성 애스펙트:

- `repeats(n: Int)`: 테스트를 여러 번 실행하여 안정성 보장

```scala
test("repeating a test") {
  ZIO.attempt("Repeating a test to ensure its stability")
    .debug
    .map(_ => assertTrue(true))
} @@ TestAspect.nonFlaky @@ TestAspect.repeats(5)
```

- `retries(n: Int)`: 플래키 테스트를 재시도할 횟수를 지정한 값으로 설정하여 각 테스트를 실행
- `samples(n: Int)`: 프로퍼티 기반 테스트의 반복 횟수 조정

```scala
test("customized number of samples") {
  for {
    ref <- Ref.make(0)
    _ <- check(Gen.int)(_ => assertZIO(ref.update(_ + 1))(Assertion.anything))
    value <- ref.get
  } yield assertTrue(value == 50)
} @@ TestAspect.samples(50)
```

- `shrinks(n: Int)`: 큰 실패를 최소화하기 위한 최대 축소(shrinking) 횟수 제어

---

### 9. 프로퍼티 기반 테스트(Property-Based Testing): Gen과 check

#### 핵심 개념(Core Concept)

프로퍼티 기반 테스트 → 전통적인 단위 테스트와 다름. 개별 값을 테스트하고 그 결과를 단언하는 대신, 테스트 대상 시스템(system under test)의 프로퍼티(properties)를 검증하는 것에 의존.

`Gen[R, A]`: 환경 `R`을 요구하는, 타입 `A`의 값을 생성하는 제너레이터(generator). 제너레이터는 결정론적(deterministic) 및 비결정론적(non-deterministic, PRNG) 무작위 값을 생성하는 데 사용.

#### 예제: 덧셈 함수(Addition Function)

`add` 함수가 만족해야 하는 수학적 프로퍼티를 식별하여 테스트.

1. 교환 법칙(Commutative Property): `add(a, b) == add(b, a)`
2. 결합 법칙(Associative Property): `add(add(a, b), c) == add(a, add(b, c))`
3. 항등 법칙(Identity Property): `add(a, 0) == a`

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

#### 핵심 구성 요소(Key Components)

- `Gen`: 테스트 값을 생성하는 제너레이터 데이터 타입
- `check`: 제너레이터와 테스트 프로퍼티를 받는 함수
- `Gen.int`: 정수를 위한 내장 제너레이터

#### 합성성과 축소(Composability and Shrinking)

`Gen` 데이터 타입 → 합성 가능(composable) → 제너레이터를 결합하여 더 복잡한 타입의 무작위 값 생성 가능. 예: `check(Gen.listOf(Gen.int))`로 무작위로 생성된 정수 리스트에 대한 프로퍼티 테스트 가능.

ZIO Test의 모든 제너레이터 → 내장 축소기(built-in shrinker) 보유 → 프로퍼티가 실패하면 반례(counterexample)를 가장 단순한 형태로 축소 시도.

---

### 10. 내장 제너레이터(Built-in Generators)

ZIO Test 프레임워크 → `Gen` 컴패니언 객체를 통해 광범위한 제너레이터 지원 제공. 범주별 개요:

#### 원시 타입(Primitive Types)

`Gen.int`, `Gen.string`, `Gen.boolean`, `Gen.float`, `Gen.double`, `Gen.bigInt`, `Gen.byte`, `Gen.bigDecimal`, `Gen.long`, `Gen.char`, `Gen.short`

#### 문자 제너레이터(Character Generators)

`Gen.alphaChar`, `Gen.alphaNumericChar`, `Gen.asciiChar`, `Gen.unicodeChar`, `Gen.numericChar`, `Gen.printableChar`, `Gen.whitespaceChars`, `Gen.hexChar`, `Gen.hexCharLower`, `Gen.hexCharUpper`

#### 문자열 제너레이터(String Generators)

- `Gen.stringBounded(min, max)(charGen)` — 크기가 제한된 문자열
- `Gen.stringN(size)(charGen)` — 고정 크기 문자열
- `Gen.string1` — 최소 1개 문자
- `Gen.alphaNumericString`, `Gen.alphaNumericStringBounded`
- `Gen.iso_8859_1`, `Gen.asciiString`

#### 고정/상수 값(Fixed/Constant Values)

`Gen.const(value)`, `Gen.constSample(sample)`, `Gen.unit`, `Gen.throwable`, `Gen.empty`

#### 값에서 선택하기(Selecting from Values)

- `Gen.elements(val1, val2, ...)` — 무작위 선택
- `Gen.fromIterable(collection)` — 결정론적 생성

#### 컬렉션(Collections)

`Gen.setOf`, `Gen.setOf1`, `Gen.setOfBounded`, `Gen.setOfN`, 그리고 리스트(list)·벡터(vector)·청크(chunk)·맵(map)에 대한 동등한 제너레이터들

#### 복합 타입(Compound Types)

```scala
val tuples: Gen[Any, (Int, Double)] =
  for {
    a <- Gen.int
    b <- Gen.double
  } yield (a, b)
```

`Gen.oneOf`, `Gen.option`, `Gen.some`, `Gen.none`, `Gen.either`, `Gen.collectAll`, `Gen.concatAll`

#### 날짜/시간(Date/Time)

`Gen.dayOfWeek`, `Gen.month`, `Gen.year`, `Gen.instant`, `Gen.localDate`, `Gen.localDateTime`, `Gen.zonedDateTime`, `Gen.finiteDuration`

#### 함수 제너레이터(Function Generators)

`Gen.function`, `Gen.functionWith`, `Gen.function2`~`Gen.functionN`, `Gen.partialFunction`

#### ZIO 효과(ZIO Effects)

`Gen.successes`, `Gen.failures`, `Gen.died`, `Gen.causes`, `Gen.chained`, `Gen.concurrent`, `Gen.parallel`

#### 고급(Advanced)

`Gen.sized`, `Gen.size`, `Gen.small`, `Gen.medium`, `Gen.large`, `Gen.suspend`, `Gen.unfoldGen`, `Gen.fromZIO`, `Gen.fromRandom`

---

### 11. 제너레이터의 동작 원리(How Generators Work)

#### 핵심 개념(Core Concept)

`Gen[R, A]`: 환경 `R`을 요구하는, 타입 `A`의 값을 생성하는 제너레이터. 선택적 샘플(optional samples)의 스트림으로 인코딩되어 `ZStream[R, Nothing, A]`와 유사하게 동작.

#### 조합자(Combinators)

- `map`: `Gen(sample.map(f))` — 생성된 값을 변환
- `flatMap`: 의존적인 제너레이터를 연쇄(chaining)하는 데 사용
- `const`: `Gen(ZStream.succeed(a))` — 상수 값 생성
- `elements`: 무작위 선택을 사용하여 지정된 값들로부터 생성
- `fromIterable`: 컬렉션으로부터 결정론적 제너레이터 생성

#### 핵심 연산(Key Operations)

- `runCollect`: 생성된 모든 값을 담은 `ZIO[Any, Nothing, List[Int]]` 반환
- `runCollectN`: 필요한 만큼 제너레이터를 반복적으로 실행
- `runHead`: 첫 번째로 생성된 값 반환

#### 제너레이터 유형(Generator Types)

- 결정론적(Deterministic): `Gen.const(42)`, `Gen.fromIterable(List(1, 2, 3))`
- 무작위(Random): `Gen.boolean`, `Gen.int`, `Gen.elements(1, 2, 3)`

---

### 12. 동적 테스트 생성(Dynamic Test Generation)

#### 핵심 개념(Core Concept)

ZIO에서 테스트는 동적(dynamic)임 → 컴파일 타임(compile time)에 정적으로(statically) 정의될 필요 없음. 외부 소스(예: CSV 데이터)로부터 테스트 데이터를 불러와 런타임에 테스트 스펙(spec)을 동적으로 생성 가능.

#### 예제: CSV로부터 덧셈 테스트 생성하기(Generating Addition Tests from a CSV File)

`add` 함수의 다양한 입력·출력 조합을 검증하는 테스트를 CSV 파일에서 읽어와 동적으로 만드는 예제.

`src/test/resources/test-data.csv` 파일:

```
0, 0, 0
1, 0, 1
0, 1, 1
0, -1, -1
-1, 0, -1
1, 1, 2
1, -1, 0
-1, 1, 0
```

각 줄은 `(a, b, expected)` 형태 → `add(a, b)`가 `expected`와 같아야 함을 나타냄.

CSV 파일을 읽어 `(Int, Int) → Int` 튜플 목록으로 변환하는 `loadTestData` 함수:

```scala
def loadTestData: Task[List[((Int, Int), Int)]] =
  ZIO.attemptBlocking(
    scala.io.Source
      .fromResource("test-data.csv")
      .getLines()
      .toList
      .map(_.split(',').map(_.trim))
      .map(i => ((i(0).toInt, i(1).toInt), i(2).toInt))
  )
```

불러온 파라미터 하나로부터 개별 `Spec`을 만드는 `makeTest` 함수:

```scala
def makeTest(a: Int, b: Int)(expected: Int): Spec[Any, Nothing] =
  test(s"test add($a, $b) == $expected") {
    assertTrue(add(a, b) == expected)
  }
```

로드된 전체 테스트 데이터를 `Spec` 목록으로 변환하는 `makeTests` 함수:

```scala
def makeTests: ZIO[Any, Throwable, List[Spec[Any, Nothing]]] =
  loadTestData.map { testData =>
    testData.map { case ((a, b), expected) =>
      makeTest(a, b)(expected)
    }
  }
```

`ZIOSpecDefault`로 이 동적 테스트들을 실행하는 전체 예제:

```scala
import zio._
import zio.test._

def add(a: Int, b: Int): Int = a + b

def loadTestData: Task[List[((Int, Int), Int)]] =
  ZIO.attemptBlocking(
    scala.io.Source
      .fromResource("test-data.csv")
      .getLines()
      .toList
      .map(_.split(',').map(_.trim))
      .map(i => ((i(0).toInt, i(1).toInt), i(2).toInt))
  )

def makeTest(a: Int, b: Int)(expected: Int): Spec[Any, Nothing] =
  test(s"test add($a, $b) == $expected") {
    assertTrue(add(a, b) == expected)
  }

def makeTests: ZIO[Any, Throwable, List[Spec[Any, Nothing]]] =
  loadTestData.map { testData =>
    testData.map { case ((a, b), expected) =>
      makeTest(a, b)(expected)
    }
  }

object AdditionSpec extends ZIOSpecDefault {
  override def spec = suite("add")(makeTests)
}
```

`suite`의 인자로 `Spec` 목록을 담은 효과(`ZIO[Any, Throwable, List[Spec[Any, Nothing]]]`)를 직접 넘길 수 있음 → 스펙 트리(spec tree) 자체가 효과적으로(effectfully) 구성될 수 있기 때문. 실행 결과:

```
+ add
  + test add(0, 0) == 0
  + test add(1, 0) == 1
  + test add(0, -1) == -1
  + test add(0, 1) == 1
  + test add(-1, 1) == 0
  + test add(1, -1) == 0
  + test add(1, 1) == 2
  + test add(-1, 0) == -1
8 tests passed. 0 tests failed. 0 tests ignored.
```

CSV의 각 행이 독립적인 테스트 케이스로 변환되어 8개의 테스트가 모두 통과함을 확인 가능. 이 패턴 → 대량의 테스트 데이터를 코드베이스 밖에 두고 관리하거나, 데이터베이스·API 등 다른 외부 소스로부터 테스트 케이스를 생성해야 할 때 유용.

---

### 13. 테스트 서비스(Test Services)

`environment` 패키지 → `TestClock`, `TestConsole`, `TestSystem`, `TestRandom` 모듈을 통해 표준 ZIO 환경 타입들의 테스트 가능한(testable) 버전 제공. ZIO Test 사용 시 이 모두를 포함하는 `TestEnvironment`가 각 테스트에 자동으로 주입됨.

#### 13.1 TestConsole

`TestConsole`: 표준 입출력(standard input and output)을 내부 버퍼(internal buffers)로 모델링 → 콘솔과 상호작용하는 애플리케이션을 테스트 가능.

핵심 메서드:

- `feedLines`: 이후의 `readLine` 호출이 반환할 미리 정의된 문자열로 입력 버퍼를 채워, 사용자 입력 시뮬레이션 가능
- `output`: 테스트 중 수행된 모든 `print` 및 `printLine` 연산으로 누적된 출력 버퍼 내용을 가져옴
- `clearInput` / `clearOutput`: 테스트 섹션 사이에 상태를 리셋하기 위해 각각의 버퍼를 비움

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

이 접근 방식 → 콘솔 테스트를 실제 I/O 연산으로부터 분리 → 테스트를 결정론적이고 빠르게 만듦.

#### 13.2 TestClock

`TestClock`: 실제 시간이 흐르기를 기다리지 않고, 시간의 경과를 포함하는 효과를 결정론적이고 효율적으로 테스트 가능. `sleep` 및 관련 메서드로 스케줄된 효과들은 클록 시간(clock time)이 조정될 때 발동(trigger)됨.

핵심 메커니즘:

- `adjust()`와 `setTime()` 메서드로 클록 진행 제어
- 특정 시간에 스케줄된 효과는 클록이 그 시간에 도달하면 자동으로 실행됨
- 클록은 명시적으로 수정되기 전까지는 정지(static) 상태 → 자동으로 진행하지 않음

포크 패턴(Forking Pattern): 테스트 대상 효과를 포크(fork)하고, 클록 시간을 조정한 뒤, 기대한 효과가 수행되었는지 검증하는 것이 유용한 패턴.

`TestClock`은 "00:00 1/1/70"으로 초기화됨 → 스케줄된 효과를 발동시키려면 시간을 명시적으로 진행 필요.

예제 1 — 기본 시간 진행:

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

예제 2 — 비동기 스케줄 효과:

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

예제 3 — 복잡한 의존성과 실패 처리:

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

예제 4 — 큐 기반 다중 값 테스트:

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

예제 5 — 라이브 클록 통합:

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

#### 13.3 TestRandom

`TestRandom`: 무작위성(randomness)을 다루는 코드를 결정론적으로 테스트 가능. 두 가지 모드로 동작.

모드 1 — 시드 기반 의사 난수 생성(Seed-Based Pseudo-Random Generation): `setSeed`로 초기 시드(seed)를 설정하면 → 일관되고 재현 가능한(reproducible) 값 시퀀스를 얻음.

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

모드 2 — 미리 정의된 값 공급(Predefined Value Feeding): `feedInts` 같은 메서드로 내부 버퍼를 채우면 → 의사 난수를 생성하기 전에 버퍼의 값들이 먼저 소비됨.

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

`clearInts`로 버퍼를 리셋하면 두 모드 간에 매끄럽게 전환 가능.

#### 13.4 TestSystem

`TestSystem`: 시스템 프로퍼티를 다루는 효과의 결정론적 테스트 가능. 환경 변수(environment variables)와 JVM 시스템 프로퍼티를 통한 애플리케이션 구성을 테스트하는 데 유용. 실제 시스템에 영향을 주지 않고 환경 변수와 시스템 프로퍼티 설정·접근 가능 → 이러한 동작의 결과로 실제 환경 변수나 시스템 프로퍼티가 접근되거나 설정되지 않음.

```scala
import zio._
import zio.test._
import zio.test.Assertion._
for {
  _      <- TestSystem.putProperty("java.vm.name", "VM")
  result <- System.property("java.vm.name")
} yield assertTrue(result == Some("VM"))
```

참조 메서드:

- `putProperty` — JVM 시스템 프로퍼티 설정
- `putEnv` — 환경 변수 설정
- `setLineSeparator` — 줄 구분자(line separator) 설정

#### 13.5 Live

`Live` 트레이트: 테스트 코드가 테스트 환경 대신 라이브(live) 환경에 접근 가능. 실제 콘솔 출력이나 실제 타이밍처럼 진짜 시스템 서비스가 필요한 연산에 활용.

`Live.live()`: 효과를 테스트 환경 대신 라이브 환경에서 실행. 예: 테스트에서 `Clock.currentTime()`을 호출하면 보통 `0L`(테스트 기본값)이 반환됨 → 실제 현재 시간을 얻으려면 다음과 같이 처리.

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

`Live.withLive()`: 효과 자체는 테스트 환경에서 실행하되, 변환(transformation)에는 라이브 환경을 적용. 테스트 연산에 실제 세계의 동작(예: 타임아웃)을 적용해야 할 때 유용.

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

이 접근 방식 → 타임아웃 연산을 테스트 시간이 아닌 실제 시간에 대해 실행.

#### 13.6 TestConfig

`TestConfig` 서비스 → 네 가지 주요 테스팅 구성 관리.

1. Repeats — 테스트가 안정적인지 보장하기 위해 테스트를 반복할 횟수
2. Retries — 플래키 테스트를 재시도할 횟수
3. Samples — 무작위 변수(random variable)를 검사하기에 충분한 샘플 수
4. Shrinks — 큰 실패를 최소화하기 위한 최대 축소(shrinking) 횟수

라이브 구현이 제공하는 기본값(defaults):

```
TestConfig.live(
  repeats0 = 100,
  retries0 = 100,
  samples0 = 200,
  shrinks0 = 1000
)
```

이 설정들 → 표준 테스트 환경에서 ZIO 테스트를 실행할 때 자동으로 적용됨. 대부분의 경우 기본값을 그대로 사용하지만, 특정 테스팅 요구사항에 맞게 커스텀 구성도 가능.

#### 13.7 Sized

`Sized` 서비스 → 크기가 지정된 제너레이터(sized generators)가 ZIO Test 환경에서 크기(size) 파라미터를 읽을 수 있게 함.

핵심 인터페이스:

```scala
trait Sized extends Serializable {
  def size: UIO[Int]
  def withSize[R, E, A](size: Int)(zio: ZIO[R, E, A]): ZIO[R, E, A]
}
```

`Sized.size`: 환경으로부터 현재 크기 값을 가져옴. `Gen.sized` 같은 크기 지정 제너레이터에 의해 내부적으로 사용됨.

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

`Sized.withSize`: 효과를 실행하는 동안 크기 값을 일시적으로 수정.

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

ZIO Test는 편의 래퍼(convenience wrapper)로 `TestAspect.size` 제공.

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

### 14. 참고 자료

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

---

## ZIO 메트릭과 관찰 가능성

> 원본: https://zio.dev/reference/observability/metrics/

---

### 목차

1. [메트릭 개요(Metrics Overview)](#1-메트릭-개요metrics-overview)
2. [카운터(Counter)](#2-카운터counter)
3. [게이지(Gauge)](#3-게이지gauge)
4. [히스토그램(Histogram)](#4-히스토그램histogram)
5. [서머리(Summary)](#5-서머리summary)
6. [프리퀀시(Frequency)](#6-프리퀀시frequency)
7. [메트릭 레이블(MetricLabel)](#7-메트릭-레이블metriclabel)
8. [메트릭 적용과 타이머(Applying Metrics & Timer)](#8-메트릭-적용과-타이머applying-metrics--timer)
9. [Prometheus 연동 예제(Prometheus Integration)](#9-prometheus-연동-예제prometheus-integration)
10. [로깅(Logging)](#10-로깅logging)
11. [슈퍼바이저(Supervisor)](#11-슈퍼바이저supervisor)
12. [Chunk(Misc)](#12-chunkmisc)
13. [참고 자료](#13-참고-자료)

---

### 1. 메트릭 개요(Metrics Overview)

분산 시스템(distributed system)에서 다운타임(downtime) 없이 환경을 원활하게 유지하는 것 → 매우 어려운 일. ZIO 메트릭(ZIO Metrics)은 복잡하게 복제된(replicated) 서비스 전반에서 메트릭을 수집(capture)하고, 이를 메트릭 서비스(metric service)로 전송하여 분석·조사할 수 있게 함.

#### 다섯 가지 메트릭 종류(Five Metric Types)

ZIO가 지원하는 다섯 가지 메트릭 범주:

1. 카운터(Counter) — 요청 수(request counts)처럼 시간이 지남에 따라 증가하기만(increases over time) 하는 값에 사용
2. 게이지(Gauge) — 메모리 사용량(memory usage)처럼 시간이 지나면서 올라가거나 내려갈 수 있는(go up or down) 단일 수치값(single numerical value)
3. 히스토그램(Histogram) — 요청 지연 시간(request latencies)처럼 관측된 값들의 집합을 버킷(bucket)의 집합에 걸쳐 분포(distribution)로 추적
4. 서머리(Summary) — 특정 백분위수(percentile)에 대한 메트릭과 함께, 시계열(time series)의 슬라이딩 윈도우(sliding window)를 나타냄
5. 프리퀀시(Frequency) — 서로 구별되는 문자열 값(distinct string values)의 발생 횟수(number of occurrences)를 셈

#### 애스펙트(Aspect)로서의 메트릭

ZIO에서 메트릭은 ZIO 애스펙트(Aspect)로 정의됨 → 이펙트(effect)의 시그니처(signature)를 변경하지 않고도 메트릭 적용 가능. 메트릭은 `@@` 연산자를 통해 이펙트에 부착됨.

```scala
import zio._
import zio.metrics._

def memoryUsage: ZIO[Any, Nothing, Double] = {
  import java.lang.Runtime._
  ZIO
    .succeed(getRuntime.totalMemory() - getRuntime.freeMemory())
    .map(_ / (1024.0 * 1024.0)) @@ Metric.gauge("memory_usage")
}
```

#### 지원되는 백엔드(Supported Backends)

ZIO 메트릭 커넥터(ZIO Metrics Connectors)가 통합하는 메트릭 백엔드(backend):

- Prometheus
- Datadog
- New Relic
- StatsD

---

### 2. 카운터(Counter)

#### 정의(Definition)

`Counter`: "시간이 지남에 따라 증가될 수 있는 단일 수치값을 나타내는 메트릭(a metric representing a single numerical value that may be incremented over time)". 시점별 스냅숏(point-in-time snapshot)이 아니라 누적값(cumulative value)을 추적 → 오직 증가하기만(monotonically increasing) 하는 수량을 모니터링하는 데 적합.

#### API 메서드

ZIO 라이브러리가 제공하는 세 가지 생성자(constructor) 메서드:

```scala
object Metric {
  def counter(name: String): Counter[Long] = ???
  def counterDouble(name: String): Counter[Double] = ???
  def counterInt(name: String): Counter[Int] = ???
}
```

#### `@@`로 적용하기

카운터는 `@@` 연산자를 사용하여 이펙트에 적용.

```scala
val countAll = Metric.counter("countAll").fromConst(1)
val myApp = for {
  _ <- ZIO.unit @@ countAll
  _ <- ZIO.unit @@ countAll
} yield ()
```

#### 사용 예시(Usage Examples)

반복(repeating) 시나리오:

```scala
(zio.Random.nextLongBounded(10) @@ 
  Metric.counter("request_counts")).repeatUntil(_ == 7)
```

값과 함께 사용:

```scala
val countBytes = Metric.counter("countBytes")
val myApp = Random.nextLongBetween(0, 100) @@ countBytes
```

#### 주요 사용 사례(Key Use Cases)

카운터 메트릭이 적합한 경우:

- 요청 수(Request counts)
- 완료된 작업(Completed tasks)
- 에러 수(Error counts)
- 단조 증가하는(monotonically increasing) 모든 값

---

### 3. 게이지(Gauge)

#### 정의(Definition)

`Gauge`: "설정(set)되거나 조정(adjusted)될 수 있는 단일 수치값(a single numerical value that may be set or adjusted)"을 나타냄. 누적값을 추적하는 카운터와 달리, 게이지는 특정 시점의 현재 값(current value)을 측정.

#### 주요 특징(Key Characteristics)

- 시간이 지나면서 변하는 `Double` 타입의 이름이 붙은 변수(named variable)
- 절대값(absolute value)으로 설정하거나, 현재 값을 기준으로 상대적으로(relative) 조정 가능
- 위아래로 변동(fluctuate)하는 메트릭에 이상적

#### API

```scala
object Metric {
  def gauge(name: String): Gauge[Double] = ???
}
```

#### 일반적인 사용 사례(Typical Use Cases)

- 메모리 사용량(Memory Usage)
- 큐 크기(Queue Size)
- 진행 중인 요청 수(In-Progress Request Counts)
- 온도(Temperature)

#### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._
val absoluteGauge = Metric.gauge("setGauge")

for {
  _ <- Random.nextDoubleBetween(0.0d, 100.0d) @@ absoluteGauge @@ countAll
} yield ()
```

위 예제 → `Double` 값을 산출하는 이펙트에 게이지를 적용하면서, `@@` 연산자를 사용해 추가적인 메트릭 애스펙트를 함께 결합.

---

### 4. 히스토그램(Histogram)

#### 정의(Definition)

히스토그램(Histogram): "시간에 걸친 누적값의 분포를 갖는 수치값들의 모음(a collection of numerical with the distribution of the cumulative values over time)"을 나타내는 메트릭. 측정값들을 서로 구별되는 구간(interval), 즉 버킷(bucket)으로 조직화 → 각 버킷에 속하는 값의 빈도(frequency)를 기록.

#### 동작 방식(How It Works)

히스토그램 → `Double` 값을 관측(observe)하여 미리 정의된 버킷에서 관측값의 개수를 셈. 각 버킷은 상한 경계(upper boundary) `b`를 가짐 → 관측된 값 `v`가 `b` 이하일 때(`v <= b`) 해당 버킷의 카운트가 1 증가. 마지막 버킷은 항상 `Double.MaxValue`로 설정 → 모든 관측값이 빠짐없이 포착되도록 보장.

#### API

```scala
object Metric {
  def histogram(
      name: String,
      boundaries: Histogram.Boundaries
  ): Histogram[Double] = ???
  
  def timer(
      name: String,
      description: String,
      chronoUnit: ChronoUnit
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
  
  def timer(
      name: String,
      chronoUnit: ChronoUnit,
      boundaries: Chunk[Double]
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
}
```

#### 경계(Boundaries)

경계는 `MetricKeyType.Histogram.Boundaries.linear(0, 10, 11)`과 같이 생성. 위 호출 → 0부터 100까지 10 단위(step)로 12개의 버킷(0~100 구간의 11개 버킷 + `Double.MaxValue` 버킷)을 만듦.

#### 코드 예제: 선형 버킷(Linear Buckets)

```scala
import zio._
import zio.metrics._

val histogram = Metric.histogram(
  "histogram", 
  MetricKeyType.Histogram.Boundaries.linear(0, 10, 11)
)

Random.nextDoubleBetween(0.0d, 120.0d) @@ histogram
```

#### 사용 사례(Use Cases)

히스토그램이 적합한 시나리오:

- 백분위수(percentile) 계산이 필요한 경우
- 값의 범위(value range)를 미리 추정할 수 있는 경우
- 정확한 값보다 허용 가능한 근사치(acceptable approximation)로 충분한 경우
- 여러 인스턴스(multi-instance)에 걸친 집계(aggregation)가 필요한 경우(예: HTTP 응답 시간·지연 시간·처리량)

---

### 5. 서머리(Summary)

#### 정의(Definition)

`Summary`: 지정된 백분위수(즉, 분위수(quantile))에 대한 메트릭과 함께, 시계열(time series) 데이터의 슬라이딩 윈도우(sliding window)를 나타냄. `Double` 값을 관측 → 히스토그램처럼 버킷 카운터를 직접 변경하는 대신 내부 샘플(internal samples)을 유지.

#### 분위수와 백분위수(Quantiles and Percentiles)

분위수(quantile): `0 <= q <= 1`을 만족하는 `Double` 값 `q`로 정의 → 그 결과 또한 `Double`로 해석(resolve)됨. 일반적으로 추적되는 분위수: 0.5(중앙값(median))와 0.95.

오차 한계(error margin) `e`를 설정하여 유연성을 둘 수 있음. 분위수 `q`는, `n`이 `v` 이하인 값의 개수이고 `s`가 샘플 집합 크기(sample set size)일 때, `(1-e)q * s <= n <= (1+e)q * s`를 만족하면 값 `v`로 해석됨.

#### API 파라미터

```scala
object Metric {
  def summary(
    name: String,
    maxAge: Duration,
    maxSize: Int,
    error: Double,
    quantiles: Chunk[Double]
  ): Summary[Double]
  
  def summaryInstant(
    name: String,
    maxAge: Duration,
    maxSize: Int,
    error: Double,
    quantiles: Chunk[Double]
  ): Summary[(Double, java.time.Instant)]
}
```

- maxAge: 슬라이딩 윈도우 안에서 샘플의 최대 수명(Maximum age of samples)
- maxSize: 유지되는 샘플의 최대 개수(Maximum number of samples retained)
- error: 분위수 계산에 적용되는 오차 한계(Error margin)
- quantiles: 원하는 백분위수를 나타내는 `Double` 값들의 Chunk

#### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._
import zio.metrics.Metric.Summary

val summary: Summary[Double] =
  Metric.summary(
    name = "mySummary",
    maxAge = 1.day,
    maxSize = 100,
    error = 0.03d,
    quantiles = Chunk(0.1, 0.5, 0.9)
  )

Random.nextDoubleBetween(100, 500) @@ summary
```

#### 사용 사례(Use Cases)

서머리는 히스토그램의 정확도가 부족한 경우의 지연 시간(latency) 모니터링, 값의 범위를 미리 추정할 수 없는 경우, 또는 인스턴스 간 집계가 필요하지 않은 경우에 적합한 선택.

---

### 6. 프리퀀시(Frequency)

#### 정의(Definition)

`Frequency` 메트릭: "지정된 값들의 발생 횟수(the number of occurrences of specified values)"를 나타냄. 자동으로 확장되는(auto-expanding) 카운터 집합처럼 동작 → 새로운 값이 관측되면 그 값에 대한 카운터가 자동으로 생성됨.

기술적으로 프리퀀시는 "같은 이름과 태그(tags)를 공유하는 관련 카운터들의 집합(a set of related counters sharing the same name and tags)"으로 구성됨. 이 카운터들은 추가로 설정 가능한 태그(configurable tag)로 서로 구별되며, 그 태그의 값들이 관측된 서로 다른 값(distinct values)을 나타냄.

#### 주요 사용 사례(Primary Use Cases)

프리퀀시 메트릭은 서로 구별되는 문자열 값의 발생을 추적 → 주로 두 가지 시나리오에서 사용.

1. 서비스 호출 횟수 집계(Service invocation counting) — 이름이 붙은 각 서비스가 몇 번 호출되는지 모니터링
2. 실패 유형 빈도(Failure type frequency) — 서로 다른 실패 유형이 얼마나 자주 발생하는지 측정

#### API 레퍼런스

핵심 생성자:

```scala
object Metric {
  def frequency(name: String): Frequency[String] = ???
}
```

#### 코드 예제(Code Example)

프리퀀시 메트릭 생성:

```scala
import zio.metrics._
val freq = Metric.frequency("MySet")
```

문자열을 산출하는 이펙트에 적용:

```scala
import zio._
(Random.nextIntBounded(10).map(v => s"MyKey-$v") @@ freq).repeatN(100)
```

위 코드 → 랜덤 키를 생성하고, 100번 반복하는 동안 서로 구별되는 각 값의 발생 횟수를 셈.

---

### 7. 메트릭 레이블(MetricLabel)

#### 정의(Definition)

`MetricLabel`: 더 세분화된(granular) 메트릭 분석을 가능하게 하는, 키-값 쌍(key-value pair) 형태의 메타데이터(metadata)를 나타냄. ZIO 메트릭은 "레이블 기반 차원 데이터 모델(label-based dimensional data model)"을 사용 → 메트릭은 이름과 함께 부착된 키-값 쌍을 가지며, 이 레이블들은 모니터링 대시보드에서 필터링(filtering)과 쿼리(querying)를 위한 일급 시민(first-class citizen)이 됨.

#### 목적(Purpose)

레이블을 사용하면 추가적인 차원(dimension)별로 메트릭을 분리하여 추적 가능. 예: 서비스 응답 시간 메트릭을 클라이언트(client)별로 레이블을 통해 구분 가능.

#### 흔한 레이블 예시(Common Label Examples)

- 엔드포인트(Endpoint): `/api/users`, `/api/documents`
- HTTP 메서드(HTTP Method): `POST`, `GET`
- 배포 환경(Deployment Environment): `Staging`, `Production`
- HTTP 응답 코드(HTTP Response Code)
- 에러 코드(Error code): `404`, `503`
- 데이터센터 존(Datacenter Zone): `us-east`, `eu-west`

#### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._

val counter = Metric.counter("http_requests")
  .tagged(
    MetricLabel("env", "production"),
    MetricLabel("method", "GET"),
    MetricLabel("endpoint", "/api/users"),
    MetricLabel("zone", "ap-northeast"),
  )
```

`.tagged()` 메서드 → 메트릭에 레이블을 부착.

#### 쿼리 측면의 이점(Querying Benefits)

레이블이 가능하게 하는 세분화된 모니터링 쿼리:

- 엔드포인트별 요청 수(Request counts per endpoint)
- 존(zone)별 SLA 위반 위험도(SLA violation risk by zone)
- 엔드포인트별 지연 시간 비교(Endpoint latency comparisons)

---

### 8. 메트릭 적용과 타이머(Applying Metrics & Timer)

#### `@@` 연산자로 메트릭 적용하기

ZIO 메트릭의 핵심 설계: 메트릭이 ZIO 애스펙트(Aspect)로 정의되어 `@@` 연산자를 통해 이펙트에 부착됨. 이렇게 하면 이펙트의 타입 시그니처(type signature)를 바꾸지 않은 채로 관측 가능성(observability) 추가 가능.

여러 메트릭 애스펙트를 하나의 이펙트에 연쇄적으로 결합 가능.

```scala
for {
  _ <- Random.nextDoubleBetween(0.0d, 100.0d) @@ absoluteGauge @@ countAll
} yield ()
```

#### `Metric.timer`

`Metric.timer`: 이펙트의 실행 소요 시간(duration)을 측정하기 위한 메트릭 → 내부적으로 히스토그램(Histogram) 기반으로 구현됨. `ChronoUnit`(시간 단위)을 받아 측정 결과를 해당 단위로 기록.

```scala
object Metric {
  def timer(
      name: String,
      description: String,
      chronoUnit: ChronoUnit
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
  
  def timer(
      name: String,
      chronoUnit: ChronoUnit,
      boundaries: Chunk[Double]
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
}
```

타이머는 `Duration` 값을 입력으로 받아, 그 분포를 히스토그램(`MetricState.Histogram`)으로 집계. 두 번째 오버로드(overload)는 직접 버킷 경계(`boundaries: Chunk[Double]`)를 지정 가능.

---

### 9. Prometheus 연동 예제(Prometheus Integration)

다음은 Prometheus로 메트릭을 수집·노출하는 완전한 예제 애플리케이션. `/metrics` 엔드포인트는 Prometheus가 스크레이프(scrape)할 수 있는 메트릭 텍스트를 반환하고, `/foo` 엔드포인트는 호출될 때마다 메모리 사용량 게이지를 갱신함.

```scala
package zio.examples.metrics

import zio._
import zio.http._
import zio.metrics.Metric
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}

object MetricAppExample extends ZIOAppDefault {
  def memoryUsage: ZIO[Any, Nothing, Double] = {
    import java.lang.Runtime._
    ZIO
      .succeed(getRuntime.totalMemory() - getRuntime.freeMemory())
      .map(_ / (1024.0 * 1024.0)) @@ Metric.gauge("memory_usage")
  }

  private val httpApp =
    Routes(
      Method.GET / "metrics" ->
        handler(ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))),
      Method.GET / "foo" -> handler {
        for {
          _    <- memoryUsage
          time <- Clock.currentDateTime
        } yield Response.text(s"$time\t/foo API called")
      }
    )

  override def run = Server
    .serve(httpApp)
    .provide(
      Server.default,
      prometheus.prometheusLayer,
      prometheus.publisherLayer,
      ZLayer.succeed(MetricsConfig(5.seconds))
    )
}
```

이 예제에서 `MetricsConfig(5.seconds)`는 메트릭 폴링(polling) 주기를 5초로 설정. `prometheus.prometheusLayer`와 `prometheus.publisherLayer`는 각각 Prometheus 메트릭 백엔드와 퍼블리셔(publisher) 레이어(layer)를 제공.

---

### 10. 로깅(Logging)

ZIO는 경량(lightweight)의 내장 로깅 파사드(logging facade)를 제공함.

#### 기본 로깅(Basic Logging)

핵심 함수: `ZIO.log`.

```scala
import zio._
val app = for {
  _ <- ZIO.log("Application started!")
  name <- Console.readLine("Please enter your name: ")
  _ <- ZIO.log(s"User entered its name: $name")
  _ <- Console.printLine(s"Hello, $name")
} yield ()
```

#### 로그 레벨(Logging Levels)

`ZIO.logLevel`을 사용하여 로그 레벨 지정 가능.

```scala
ZIO.logLevel(LogLevel.Warning) {
  ZIO.log("The response time exceeded its threshold!")
}
```

직접 사용할 수 있는 레벨별 함수들:

- `ZIO.logDebug`
- `ZIO.logError`
- `ZIO.logFatal`
- `ZIO.logInfo`
- `ZIO.logWarning`

에러 레벨(error level) 예시:

```scala
ZIO.logError("File does not exist: ~/var/www/favicon.ico")
```

#### 로그 스팬(Log Spans)

스팬(span)을 사용하면 연산의 소요 시간(duration) 측정 가능.

```scala
ZIO.logSpan("myspan") {
  ZIO.sleep(1.second) *> ZIO.log("The job is finished!")
}
```

ZIO 로깅은 해당 스팬의 실행 소요 시간(running duration)을 계산 → 그 스팬 레이블(span label)에 해당하는 로깅 데이터에 포함.

#### 로그 애너테이션(Log Annotations)

##### 내장 애너테이션(Built-in Annotations)

`ZIO.logAnnotate`를 사용하여 컨텍스트 정보(contextual information) 추가.

```scala
ZIO.logAnnotate("correlation_id", user) {
  for {
    _ <- ZIO.log("fetching user from database")
  } yield ()
}
```

##### 타입이 지정된 애너테이션(Typed Annotations)

구조화된 데이터(structured data)를 위해 `LogAnnotation`으로 커스텀 애너테이션 정의 가능.

```scala
private val userLogAnnotation = 
  LogAnnotation[User]("user", (_, u) => u, _.toJson)
```

`@@` 연산자로 적용.

```scala
ZIO.logInfo("Starting operation") @@ userLogAnnotation(user)
```

---

### 11. 슈퍼바이저(Supervisor)

#### 정의(Definition)

`Supervisor[A]`: 파이버(fiber)의 시작(launching)과 종료(termination)를 모니터링하면서, 그 슈퍼비전(supervision) 활동으로부터 타입 `A`의 관측 가능한 값(visible value)을 산출.

#### 생성 메서드(Creation Methods)

##### `Supervisor.track`

자식 파이버들을 하나의 집합(set)에 유지하는 슈퍼바이저를 생성. `weak` 불리언(boolean) 파라미터는 `WeakSet`을 사용할지 표준 집합(standard set)을 사용할지를 결정.

```scala
val supervisor = Supervisor.track(true)
```

`UIO[Supervisor[Chunk[Fiber.Runtime[Any, Any]]]]`을 반환 → 프로그램 파이버들의 상태를 주기적으로 보고(periodic status reporting) 가능.

##### `Supervisor.fibersIn`

기존의 정렬된 파이버 집합(sorted set of fibers)으로 초기화된 슈퍼바이저를 생성.

```scala
def fiberListSupervisor = for {
  ref <- ZIO.succeed(new AtomicReference(SortedSet.from(fibers)))
  s <- Supervisor.fibersIn(ref)
} yield (s)
```

#### 파이버 슈퍼비전(Supervising Fibers)

`ZIO#supervised` 함수 → 이펙트에 슈퍼비전을 적용. 슈퍼바이저를 인자로 받아, 자식 파이버의 동작이 그 슈퍼바이저에게 보고되는 이펙트를 반환.

```scala
val supervised = supervisor.flatMap(s => fib(20).supervised(s))
```

#### 전체 예제(Complete Example)

```scala
import zio._
import zio.Fiber.Status

object SupervisorExample extends ZIOAppDefault {
  def run = for {
    supervisor <- Supervisor.track(true)
    fiber <- fib(20).supervised(supervisor).fork
    policy = Schedule
      .spaced(500.milliseconds)
      .whileInputZIO[Any, Unit](_ => fiber.status.map(_ != Status.Done))
    logger <- monitorFibers(supervisor)
      .repeat(policy).fork
    _ <- logger.join
    result <- fiber.join
    _ <- Console.printLine(s"fibonacci result: $result")
  } yield ()

  def monitorFibers(supervisor: Supervisor[Chunk[Fiber.Runtime[Any, Any]]]) = for {
    length <- supervisor.value.map(_.length)
    _ <- Console.printLine(s"number of fibers: $length")
  } yield ()

  def fib(n: Int): ZIO[Any, Nothing, Int] =
    if (n <= 1) {
      ZIO.succeed(1)
    } else {
      for {
        _ <- ZIO.sleep(500.milliseconds)
        fiber1 <- fib(n - 2).fork
        fiber2 <- fib(n - 1).fork
        v2 <- fiber2.join
        v1 <- fiber1.join
      } yield v1 + v2
    }
}
```

이 예제는 피보나치(fibonacci) 계산을 위해 생성되는 파이버 수를, 500밀리초마다 모니터링하며 콘솔에 출력. `Schedule.spaced(500.milliseconds)` 정책과 `whileInputZIO`를 결합 → 메인 파이버가 `Done` 상태가 될 때까지 모니터링 루프를 반복.

---

### 12. Chunk(Misc)

#### 정의(Definition)

`Chunk[A]`: 타입 `A`의 값들로 이루어진 순서 있는 컬렉션(ordered collection). 배열(array)로 뒷받침되지만, 순수 함수형의 안전한 인터페이스(purely functional, safe interface)를 노출하면서도 높은 성능 특성을 유지. 처음에는 ZIO 스트림(stream)을 위해 작성되었으나, 이후 다른 용도로도 유용한 매력적인 범용 컬렉션 타입(general collection type)으로 발전.

#### 왜 Chunk를 사용하는가(Why Use Chunk)

- 불변성(Immutability): 스칼라(Scala)의 가변(mutable) 배열과 달리, Chunk는 성능을 희생하지 않으면서도 불변(immutable) 인터페이스를 제공
- 원시 타입에 대한 제로 박싱(Zero Boxing for Primitives): Chunk는 배열로 뒷받침되므로, 불변 인터페이스를 제공하면서도 원시 타입(primitive)에 대해 박싱(boxing)이 전혀 없음
- ClassTag 불필요(No ClassTag Requirement): Chunk는 `ClassTag`가 필요 없음 → 제네릭 배열 생성(generic array creation)의 번거로움을 없앰
- 높은 성능(High Performance): Chunk는 단일 원소를 덧붙이거나(appending) 두 Chunk를 연결(concatenating)하는 것 같은 작업에 대해 특화된 연산(specialized operations)을 가짐 → 표준 라이브러리 구현보다 뛰어난 성능 제공

> 참고: 스트림을 다룰 때는 항상 청크(chunk)를 다룸. 개별 원소(individual element)로 이루어진 스트림은 없으며, 스트림의 기반 구현(underlying implementation)에는 항상 청크가 존재함. 따라서 스트림을 평가(evaluate)하여 원소를 꺼낼 때, 실제로는 원소들의 청크(chunk of elements)를 꺼내는 것임.

#### 생성 메서드(Creation Methods)

```scala
Chunk.empty                          // 빈 청크
Chunk(1, 2, 3)                       // 직접 값으로 생성
Chunk.fromIterable(List(1, 2, 3))    // 컬렉션으로부터 생성
Chunk.fromArray(Array(1, 2, 3))      // 배열로부터 생성
Chunk.fill(3)(0)                     // 반복된 값으로 채우기
Chunk.unfold(0)(n => if (n < 8) Some((n*2, n+2)) else None)
```

#### 연산(Operations)

- 연결(Concatenation): `Chunk(1,2,3) ++ Chunk(4,5,6)`
- 수집(Collecting): `chunk.collect { case string: String => string }`
- 버리기(Dropping): `Chunk(9, 2, 5, 1, 6).drop(1)` 또는 `.dropWhile(_ >= 2)`
- 변환(Conversion): `.toArray` 및 `.toSeq` 메서드 사용 가능

---

### 13. 참고 자료

- [ZIO Metrics (Overview)](https://zio.dev/reference/observability/metrics/)
- [Counter - ZIO Metrics](https://zio.dev/reference/observability/metrics/counter)
- [Gauge - ZIO Metrics](https://zio.dev/reference/observability/metrics/gauge)
- [Histogram - ZIO Metrics](https://zio.dev/reference/observability/metrics/histogram)
- [Summary - ZIO Metrics](https://zio.dev/reference/observability/metrics/summary)
- [Frequency - ZIO Metrics](https://zio.dev/reference/observability/metrics/frequency)
- [MetricLabel - ZIO Metrics](https://zio.dev/reference/observability/metrics/metriclabel/)
- [JVM Metrics - ZIO Metrics](https://zio.dev/reference/observability/metrics/jvm/)
- [Logging - ZIO Observability](https://zio.dev/reference/observability/logging)
- [Supervisor - ZIO Observability](https://zio.dev/reference/observability/supervisor)
- [Chunk - ZIO Reference](https://zio.dev/reference/stream/chunk/)
- [ZIO Metrics Connectors](https://zio.dev/zio-metrics-connectors/metrics/metric-reference/)
