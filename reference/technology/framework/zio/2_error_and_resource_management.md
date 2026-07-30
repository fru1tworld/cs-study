# ZIO 에러 관리와 리소스 관리

## ZIO 에러 관리

> 원본: https://zio.dev/reference/error-management/

---

### 목차

1. [개요: ZIO의 타입 안전한 에러 모델](#1-개요-zio의-타입-안전한-에러-모델overview)
2. [명령형 vs 선언형 에러 처리(Imperative vs Declarative)](#2-명령형-vs-선언형-에러-처리imperative-vs-declarative)
3. [에러의 세 가지 종류(Three Types of Errors)](#3-에러의-세-가지-종류three-types-of-errors)
4. [예상된 에러와 예상치 못한 에러(Expected and Unexpected Errors)](#4-예상된-에러와-예상치-못한-에러expected-and-unexpected-errors)
5. [예외적 효과와 비예외적 효과(Exceptional and Unexceptional Effects)](#5-예외적-효과와-비예외적-효과exceptional-and-unexceptional-effects)
6. [타입 에러의 보증 범위(Typed Errors Guarantees)](#6-타입-에러의-보증-범위typed-errors-guarantees)
7. [순차 에러와 병렬 에러(Sequential and Parallel Errors)](#7-순차-에러와-병렬-에러sequential-and-parallel-errors)
8. [에러로부터 복구: 캐칭(Catching)](#8-에러로부터-복구-캐칭catching)
9. [폴백(Fallback)](#9-폴백fallback)
10. [폴딩(Folding)](#10-폴딩folding)
11. [재시도(Retrying)](#11-재시도retrying)
12. [타임아웃(Timing Out)](#12-타임아웃timing-out)
13. [샌드박싱(Sandboxing)](#13-샌드박싱sandboxing)
14. [에러 채널 매핑 연산(Map Operations)](#14-에러-채널-매핑-연산map-operations)
15. [에러 들여다보기: 태핑(Tapping Errors)](#15-에러-들여다보기-태핑tapping-errors)
16. [에러를 성공 채널로 노출하기(Exposing Errors)](#16-에러를-성공-채널로-노출하기exposing-errors)
17. [에러 채널과 성공 채널 합치기(Merge & Flip)](#17-에러-채널과-성공-채널-합치기merge--flip)
18. [에러 정제: 실패와 결함의 변환(Error Refinement)](#18-에러-정제-실패와-결함의-변환error-refinement)
19. [결함을 실패로 변환하기(Converting Defects to Failures)](#19-결함을-실패로-변환하기converting-defects-to-failures)
20. [에러 누적(Error Accumulation)](#20-에러-누적error-accumulation)
21. [Cause 데이터 타입(The Cause Data Type)](#21-cause-데이터-타입the-cause-data-type)
22. [모범 사례(Best Practices)](#22-모범-사례best-practices)
23. [참고 자료](#23-참고-자료)

---

### 1. 개요: ZIO의 타입 안전한 에러 모델(Overview)

ZIO는 종합적인 에러 관리(error management) 도구를 제공합니다. ZIO는 **컴파일 타임(compile-time)에 예견된 에러(foreseen errors)를 추적**하면서, 동시에 다양한 실패 시나리오에 대한 복구 메커니즘(recovery mechanism)을 제공합니다.

ZIO의 핵심 타입은 다음과 같습니다.

```scala
ZIO[R, E, A]
```

- `R`: 효과(effect)가 필요로 하는 환경(environment) 타입
- `E`: 효과가 실패할 수 있는 에러(error) 타입 — **에러 채널(error channel)**
- `A`: 효과가 성공할 경우 산출하는 값(value) 타입 — **성공 채널(success channel)**

ZIO는 타입 에러(typed errors)에 대한 일급(first-class) 지원을 제공하며, 캐칭(catching), 전파(propagating), 변환(transforming)을 타입 안전한(typesafe) 방식으로 다룹니다.

대표적인 에러 처리 메서드는 다음과 같습니다.

- **`either`**: 실패할 수 있는 효과를 절대 실패하지 않는(infallible) 효과로 변환합니다. 실패와 성공을 모두 Scala의 `Either` 타입에 담아, 에러를 에러 채널에서 성공 채널로 옮깁니다.
- **`catchAll`**: 복구 가능한 모든 에러를 효과적인(effectful) 복구 메커니즘으로 처리합니다. 에러 핸들러는 다른 에러 타입(복구가 보장되면 `Nothing`일 수도 있음)을 가진 효과를 반환할 수 있습니다.
- **`catchSome`**: 패턴 매칭을 통해 특정 에러 타입만 처리합니다. 단, 에러 타입을 완전히 제거하지는 못합니다.
- **`orElse`**: 한 효과를 시도하고, 실패하면 대안 효과를 시도합니다. 주(primary)/백업(backup) 패턴 구현에 유용합니다.
- **`fold` / `foldZIO`**: 실패와 성공 두 경우를 모두 처리합니다.
- **`retry` / `retryOrElse`**: 일시적(transient) 에러를 `Schedule`에 따라 자동으로 재시도합니다.

---

### 2. 명령형 vs 선언형 에러 처리(Imperative vs Declarative)

#### 명령형 에러 처리(Imperative Error Handling)

명령형 방식은 예외(exception)와 try/catch 블록을 사용합니다. 에러가 발생하면 제어 흐름(control flow)이 catch 블록으로 점프합니다.

```scala
def divide(a: Int, b: Int): Int =
  if (b == 0)
    throw new IllegalArgumentException("Division by zero")
  else
    a / b

def readFromConsole: (Int, Int) = ???
val (a, b) = readFromConsole
try {
  Some(divide(a, b))
} catch {
  case _: IllegalArgumentException => None
}
```

#### 선언형 에러 처리(Declarative Error Handling)

선언형 에러 처리는 **에러를 값(value)으로 취급**합니다. `ZIO[R, E, A]`의 `E` 타입 파라미터가 잠재적 실패를 표현합니다.

```scala
import zio._

sealed trait AgeValidationException extends Exception
case class NegativeAgeException(age: Int) extends AgeValidationException
case class IllegalAgeException(age: Int) extends AgeValidationException

def validate(age: Int): ZIO[Any, AgeValidationException, Int] =
  if (age < 0)
    ZIO.fail(NegativeAgeException(age))
  else if (age < 18)
    ZIO.fail(IllegalAgeException(age))
  else ZIO.succeed(age)

validate(17).catchAll {
  case NegativeAgeException(age) => ???
  case IllegalAgeException(age) => ???
}
```

#### 선언형 방식의 핵심 이점

- **참조 투명성(Referential Transparency)**: 예외는 프로그램에 대한 추론(reasoning)을 깨뜨리지만, 선언형 에러는 조합성(composability)과 예측 가능성(predictability)을 유지합니다.
- **타입 안전성(Type Safety)**: 예외 기반에서는 함수 시그니처(signature)만 보고 어떤 에러가 던져질 수 있는지 알 수 없지만, ZIO의 타입 시스템은 에러 가능성을 시그니처에 노출합니다.
- **완전성 검사(Exhaustivity Checking)**: 컴파일러가 처리되지 않은 에러 케이스를 경고하여, 누락된 예외 핸들러로 인한 런타임 크래시(runtime crash)를 예방합니다.
- **무손실 에러 모델(Lossless Error Model)**: try/catch에서는 예외가 삼켜질 수 있지만(swallowed), ZIO는 슈퍼바이저(supervisor)와 `Exit`, `Cause` 같은 자료 구조를 통해 모든 에러를 보존합니다.

---

### 3. 에러의 세 가지 종류(Three Types of Errors)

ZIO는 에러를 세 가지 범주로 구분합니다.

#### 3.1 실패(Failures) — 예상된 에러(Expected Errors)

실패(Failures)는 예상된 에러이며, 모델링할 때 `ZIO.fail`을 사용합니다. 개발자가 대처 방법을 알고 있는 예측 가능한 문제로, 핵심 원칙은 이러한 에러를 처리하여 콜 스택(call stack) 전체로 전파되지 않도록 막는 것입니다.

```scala
val failed: ZIO[Any, String, Nothing] = ZIO.fail("Oh uh!")
```

#### 3.2 결함(Defects) — 예상치 못한 에러(Unexpected Errors)

결함(Defects)은 예상치 못한 에러이며, 모델링할 때 `ZIO.die`를 사용합니다. 결함은 예견되지 않았으므로, 상위 계층(upper layers)에서 다음 중 하나가 발생할 때까지 애플리케이션 스택을 통해 전파되어야 합니다.

- 상위 계층이 결함을 인지하여 이를 실패(failure)로 변환한 뒤 처리하거나,
- 애플리케이션이 결국 크래시(crash)됩니다.

```scala
val defect: ZIO[Any, Nothing, Nothing] = ZIO.die(new RuntimeException("Boom!"))
```

#### 3.3 치명적 에러(Fatal Errors) — 파국적 예상치 못한 에러

치명적(Fatals) 에러는 파국적인(catastrophic) 예상치 못한 에러입니다. 발생 시 더 이상 전파하지 말고 즉시 애플리케이션을 종료(kill)해야 하며, 기껏해야 에러를 로깅하고 콜 스택을 출력하는 정도만 수행합니다.

#### 핵심 구분

세 가지 차이는 "기대 수준(expectation level)"과 "처리 전략(handling strategy)"에 있습니다.

- **실패**: 우아하게(gracefully) 처리해야 함
- **결함**: 인지되거나 실패로 이어질 때까지 전파해야 함
- **치명적 에러**: 더 이상의 전파 없이 즉시 애플리케이션을 종료해야 함

---

### 4. 예상된 에러와 예상치 못한 에러(Expected and Unexpected Errors)

ZIO는 두 가지 에러 범주를 구분합니다.

**예상된 에러(Expected Errors)** — 복구 가능(recoverable)하거나 선언된(declared) 에러입니다. 정상 동작 중 발생하는 예측 가능한 문제로, 처리하고 복구할 수 있습니다. 예: 일시적 데이터베이스 사용 불가, 잘못된 사용자 입력. 워크플로(workflow)에서 예상되는 도메인(domain)·비즈니스(business) 에러를 의미합니다.

**예상치 못한 에러(Unexpected Errors)** — 복구 불가능한(non-recoverable) 결함(defects)입니다. 런타임에 처리할 수 없는 예측 불가능한 프로그래밍 문제나 환경적 실패입니다. 예: 메모리 에러, 버퍼 오버플로(buffer overflow), 널 포인터(null pointer) 접근, 손상된 데이터 파일. 일반적으로 조사와 수정이 필요한 버그(bug)를 가리킵니다.

> 예상된 에러는 정상적인 상황에서 발생할 것으로 기대하는 에러인 반면, 예상치 못한 에러는 기대하지는 않지만 당연히 발생할 수 있음을 아는 상황을 나타냅니다.

대부분의 예상치 못한 에러는 프로그래밍 실수에서 비롯됩니다 — 불충분한 입력 검증(input validation), 엣지 케이스(edge case)에 대한 불완전한 테스트, 서비스 간 데이터 계약(data contract) 불일치 등.

#### ZIO 모범 사례

- **예상된 에러**: ZIO는 타입 파라미터 `E`를 사용해 예상된 에러를 타입 시스템에 인코딩하여, 컴파일 타임 에러 추적과 도메인별 처리 전략을 가능하게 합니다.
- **예상치 못한 에러**: 예견할 수 없으므로 타입에 반영하지 않습니다. 권장 사항은 이를 샌드박싱(sandbox)하여 — 스택 트레이스(stack trace)와 함께 로깅하고, 개발자에게 경보하며, 사용자 친화적 메시지를 표시한 후 — 애플리케이션을 크래시시키는 것입니다.

근본 원칙: **예상된 에러는 타입화하고 복구 가능하게 만들며, 예상치 못한 에러는 로깅·격리하고 사후(post-incident)에 조사한다.**

---

### 5. 예외적 효과와 비예외적 효과(Exceptional and Unexceptional Effects)

ZIO는 에러 처리 가능 여부에 따라 두 범주로 나뉘는 네 가지 타입 별칭(type alias)을 제공합니다.

- **예외적 효과(Exceptional Effects)**: `Task`와 `RIO`는 에러 파라미터가 `Throwable`로 고정됩니다. 즉, 어떤 예외로든 실패할 수 있는 계산을 나타냅니다.
- **비예외적 효과(Unexceptional Effects)**: `UIO`와 `URIO`는 에러 파라미터가 `Nothing`으로 고정됩니다. 즉, 이 효과들은 실패할 수 없으며, 컴파일러가 이 제약을 강제합니다.

이 구분 덕분에 코드의 어느 지점에서든 해당 코드가 실패할 수 있는지, 아니면 성공이 보장되는지를 판단할 수 있습니다. 타입 에러는 '이것은 실패할 수 있다'와 '이것은 실패할 수 없다' 사이의 컴파일 타임 전환점(transition point)을 제공합니다.

#### 실전 예제

`ZIO.acquireReleaseWith` API가 이 원칙을 보여 줍니다.

```scala
object ZIO {
  def acquireReleaseWith[R, E, A, B](
    acquire: => ZIO[R, E, A],
    release: A => URIO[R, Any],
    use: A => ZIO[R, E, B]
  ): ZIO[R, E, B]
}
```

`release` 파라미터가 비예외적 효과인 `URIO[R, Any]`를 요구한다는 점에 주목하세요. 여기에 예외적 효과를 전달하려고 하면 컴파일이 실패하여, 정리(cleanup) 코드가 예기치 않게 실패하는 것을 방지합니다.

---

### 6. 타입 에러의 보증 범위(Typed Errors Guarantees)

ZIO의 타입 에러는 **실패(failure) 에러에 대해서만** 보증을 제공하며, **결함(defect)이나 인터럽션(interruption)의 부재를 보증하지는 않습니다.** `ZIO[R, E, A]`로 타입화된 효과는 에러 타입 `E`로 실패할 수 있지만, 그럼에도 여전히 결함(타입화되지 않은 예외)을 만나거나 인터럽트될 수 있습니다.

> "타입 에러는 결함과 인터럽션의 부재를 보증하지 않는다."

에러 채널은 **오직 실패(failure) 에러만** 다룹니다. 다른 실패 모드들은 이 타입 안전 메커니즘을 우회합니다.

```scala
import zio._

def validateNonNegativeNumber(input: String): ZIO[Any, String, Int] =
  input.toIntOption match {
    case Some(value) if value >= 0 =>
      ZIO.succeed(value)
    case Some(other) =>
      ZIO.fail(s"the entered number is negative: $other")
    case None =>
      ZIO.die(
        new NumberFormatException(
          s"the entered input is not in the correct number format: $input"
        )
      )
  }
```

이 함수는 `ZIO[Any, String, Int]`를 반환하지만, `NumberFormatException` 결함으로 여전히 die할 수 있습니다. 타입 에러 채널이 모든 에러를 완전히 포괄하지는 않음을 보여 줍니다.

또한, 파이버(fiber)는 타입 시그니처에 영향을 주지 않으면서 인터럽트될 수 있습니다.

```scala
val myApp: ZIO[Any, String, Int] =
  for {
    f <- validateNonNegativeNumber("5").fork
    _ <- f.interrupt
    r <- f.join
  } yield r
```

---

### 7. 순차 에러와 병렬 에러(Sequential and Parallel Errors)

#### 순차 에러(Sequential Errors)

`ZIO#ensuring` 같은 자원 안전(resource-safety) 연산자를 사용하면 여러 순차 에러가 발생할 수 있습니다. 종료자(finalizer)는 원래 효과의 결과와 무관하게 실행되며, 인터럽트 불가능(uninterruptible)합니다.

```scala
import zio._
object MainApp extends ZIOAppDefault {
  def run = ZIO.fail("Oh uh!").ensuring(ZIO.dieMessage("Boom!"))
}
```

이 경우, 원래 실패가 종료자의 결함에 의해 억제되어(suppressed) 에러 체인이 형성됩니다.

#### 병렬 에러(Parallel Errors)

병렬 계산에서는 서로 다른 파이버에 걸쳐 여러 에러가 동시에 표면화될 수 있습니다.

```scala
import zio._
object MainApp extends ZIOAppDefault {
  def run = ZIO.fail("Oh!") <&> ZIO.fail("Uh!")
}
```

#### `parallelErrors` 컴비네이터

ZIO는 모든 병렬 실패 에러를 에러 채널에 노출하는 `ZIO#parallelErrors`를 제공합니다.

```scala
import zio._
val result: ZIO[Any, ::[String], Nothing] =
  (ZIO.fail("Oh uh!") <&> ZIO.fail("Oh Error!")).parallelErrors
```

**주의**: "이 연산자는 실패(failures)에 대해서만 동작하며, 결함(defects)이나 인터럽션(interruptions)에는 적용되지 않습니다."

---

### 8. 에러로부터 복구: 캐칭(Catching)

ZIO는 다양한 에러 타입(타입 에러, 결함, Cause, 트레이스)으로부터 복구하기 위한 여러 연산자를 제공합니다.

#### 8.1 실패 캐칭(Catching Failures)

타입 에러를 잡는 기본 연산자는 `catchAll`입니다.

```scala
trait ZIO[-R, +E, +A] {
  def catchAll[R1 <: R, E2, A1 >: A](h: E => ZIO[R1, E2, A1]): ZIO[R1, E2, A1]
}
```

```scala
import zio._
val z: ZIO[Any, IOException, Array[Byte]] =
  readFile("primary.json").catchAll(_ =>
    readFile("backup.json"))
```

선택적 에러 처리에는 `catchSome`을 사용합니다.

```scala
trait ZIO[-R, +E, +A] {
  def catchSome[R1 <: R, E1 >: E, A1 >: A](
    pf: PartialFunction[E, ZIO[R1, E1, A1]]
  ): ZIO[R1, E1, A1]
}
```

```scala
val data: ZIO[Any, IOException, Array[Byte]] =
  readFile("primary.data").catchSome {
    case _ : FileNotFoundException =>
      readFile("backup.data")
  }
```

**주의**: `catchAll`의 매치 케이스(match case)는 완전(exhaustive)해야 합니다. 그렇지 않으면 처리되지 않은 케이스가 `MatchError` 결함이 됩니다.

#### 8.2 결함 캐칭(Catching Defects)

예견 불가능한 결함(던져진 예외)을 잡으려면 다음을 사용합니다.

```scala
trait ZIO[-R, +E, +A] {
  def catchAllDefect[R1 <: R, E1 >: E, A1 >: A](
    h: Throwable => ZIO[R1, E1, A1]
  ): ZIO[R1, E1, A1]

  def catchSomeDefect[R1 <: R, E1 >: E, A1 >: A](
    pf: PartialFunction[Throwable, ZIO[R1, E1, A1]]
  ): ZIO[R1, E1, A1]
}
```

```scala
ZIO.dieMessage("Boom!")
  .catchAllDefect {
    case e: RuntimeException if e.getMessage == "Boom!" =>
      ZIO.debug("Boom! defect caught.")
    case _: NumberFormatException =>
      ZIO.debug("NumberFormatException defect caught.")
    case _ =>
      ZIO.debug("Unknown defect caught.")
  }
```

#### 8.3 Cause 캐칭(Catching Causes)

모든 에러 타입을 포괄적으로 처리하려면 다음을 사용합니다.

```scala
trait ZIO[-R, +E, +A] {
  def catchAllCause[R1 <: R, E2, A1 >: A](
    h: Cause[E] => ZIO[R1, E2, A1]
  ): ZIO[R1, E2, A1]

  def catchSomeCause[R1 <: R, E1 >: E, A1 >: A](
    pf: PartialFunction[Cause[E], ZIO[R1, E1, A1]]
  ): ZIO[R1, E1, A1]
}
```

```scala
val exceptionalEffect = ZIO.attempt(???)
exceptionalEffect.catchAllCause {
  case Cause.Empty =>
    ZIO.debug("no error caught")
  case Cause.Fail(value, _) =>
    ZIO.debug(s"a failure caught: $value")
  case Cause.Die(value, _) =>
    ZIO.debug(s"a defect caught: $value")
  case Cause.Interrupt(fiberId, _) =>
    ZIO.debug(s"a fiber interruption caught with the fiber id: $fiberId")
  case Cause.Stackless(cause: Cause.Die, _) =>
    ZIO.debug(s"a stackless defect caught: ${cause.value}")
  case Cause.Stackless(cause: Cause[_], _) =>
    ZIO.debug(s"an unknown stackless defect caught: ${cause.squashWith(identity)}")
  case Cause.Then(left, right) =>
    ZIO.debug(s"two consequence causes caught")
  case Cause.Both(left, right) =>
    ZIO.debug(s"two parallel causes caught")
}
```

#### 8.4 트레이스 캐칭(Catching Traces)

타입 에러를 스택 트레이스 정보와 함께 잡으려면 다음을 사용합니다.

```scala
trait ZIO[-R, +E, +A] {
  def catchAllTrace[R1 <: R, E2, A1 >: A](
    h: ((E, Trace)) => ZIO[R1, E2, A1]
  ): ZIO[R1, E2, A1]

  def catchSomeTrace[R1 <: R, E1 >: E, A1 >: A](
    pf: PartialFunction[(E, Trace), ZIO[R1, E1, A1]]
  ): ZIO[R1, E1, A1]
}
```

```scala
ZIO
  .fail("Oh uh!")
  .catchAllTrace {
    case ("Oh uh!", trace)
      if trace.toJava
        .map(_.getLineNumber)
        .headOption
        .contains(4) =>
      ZIO.debug("caught a failure on the line number 4")
    case _ =>
      ZIO.debug("caught other failures")
  }
```

---

### 9. 폴백(Fallback)

ZIO는 실패 시 대안 효과를 시도하는 여러 컴비네이터를 제공합니다.

#### 9.1 orElse

"한 효과를 시도하고, 실패하면 `orElse` 컴비네이터로 다른 효과를 시도할 수 있습니다."

```scala
trait ZIO[-R, +E, +A] {
  def orElse[R1 <: R, E2, A1 >: A](that: => ZIO[R1, E2, A1]): ZIO[R1, E2, A1]
}
```

```scala
import zio._
import java.io.IOException
val primaryOrBackupData: ZIO[Any, IOException, Array[Byte]] =
  readFile("primary.data").orElse(readFile("backup.data"))
```

#### 9.2 orElseEither

폴백 효과를 시도하되, 초기 효과가 실패하면 "either" 결과를 반환합니다. 폴백의 결과 타입이 다를 때 유용합니다.

```scala
trait ZIO[-R, +E, +A] {
  def orElseEither[R1 <: R, E2, B](that: => ZIO[R1, E2, B]): ZIO[R1, E2, Either[A, B]]
}
```

```scala
val result: ZIO[Any, Throwable, Either[LocalConfig, RemoteConfig]] =
  readLocalConfig.orElseEither(readRemoteConfig)
```

#### 9.3 orElseFail / orElseSucceed

실패를 상수 값으로 대체합니다.

```scala
trait ZIO[-R, +E, +A] {
  def orElseFail[E1](e1: => E1): ZIO[R, E1, A]
  def orElseSucceed[A1 >: A](a1: => A1): ZIO[R, Nothing, A1]
}
```

```scala
// orElseFail: 에러 타입을 통일
val result: ZIO[Any, String, Int] =
  validate(3).orElseFail("invalid age")

// orElseSucceed: 폴백 값으로 성공 처리
val result: ZIO[Any, Nothing, Int] =
  validate(3).orElseSucceed(0)
```

#### 9.4 orElseOptional

실패 타입이 `Option`일 때, 실패가 `None`이면 폴백합니다.

```scala
def parseInt(input: String): ZIO[Any, Option[String], Int] =
  input.toIntOption match {
    case Some(value) => ZIO.succeed(value)
    case None =>
      if (input.trim.isEmpty)
        ZIO.fail(None)
      else
        ZIO.fail(Some(s"invalid non-integer input: $input"))
  }
val result = parseInt("  ").orElseOptional(ZIO.succeed(0)).debug
```

#### 9.5 firstSuccessOf

효과를 실행하고, 실패하면 이후 목록에서 하나가 성공할 때까지 순서대로 ZIO 효과를 실행합니다.

```scala
object ZIO {
  def firstSuccessOf[R, R1 <: R, E, A](
    zio: => ZIO[R, E, A],
    rest: => Iterable[ZIO[R1, E, A]]
  ): ZIO[R1, E, A]
}
trait ZIO[-R, +E, +A] {
  final def firstSuccessOf[R1 <: R, E1 >: E, A1 >: A](
    rest: => Iterable[ZIO[R1, E1, A1]]
  ): ZIO[R1, E1, A1]
}
```

```scala
val masterConfig: ZIO[Any, Throwable, Config] =
  remoteConfig("master")
val nodeConfigs: Seq[ZIO[Any, Throwable, Config]] =
  List("node1", "node2", "node3", "node4").map(remoteConfig)
val config: ZIO[Any, Throwable, Config] =
  ZIO.firstSuccessOf(masterConfig, nodeConfigs)
```

---

### 10. 폴딩(Folding)

ZIO는 Scala의 `Option`, `Either`와 유사하게 실패와 성공 경우를 동시에 처리하는 여러 fold 연산자를 제공합니다.

#### 10.1 fold와 foldZIO

기본 `fold`는 두 결과를 비효과적으로(non-effectfully) 처리합니다.

```scala
trait ZIO[-R, +E, +A] {
  def fold[B](
    failure: E => B,
    success: A => B
  ): ZIO[R, Nothing, B]
}
```

효과적인(effectful) 변형은 핸들러가 ZIO 효과를 반환할 수 있게 합니다.

```scala
trait ZIO[-R, +E, +A] {
  def foldZIO[R1 <: R, E2, B](
    failure: E => ZIO[R1, E2, B],
    success: A => ZIO[R1, E2, B]
  ): ZIO[R1, E2, B]
}
```

```scala
import zio._
val DefaultData: Array[Byte] = Array(0, 0)
val primaryOrDefaultData: UIO[Array[Byte]] =
  readFile("primary.data").fold(_ => DefaultData, data => data)
```

```scala
val primaryOrSecondaryData: IO[IOException, Array[Byte]] =
  readFile("primary.data").foldZIO(
    failure = _ => readFile("secondary.data"),
    success = data => ZIO.succeed(data)
  )
```

**중요한 한계**: 이 연산자들은 파이버 인터럽션을 잡지 못하며, `InterruptedException`을 결함으로 전파합니다.

#### 10.2 foldCause와 foldCauseZIO

이 강력한 변형들은 완전한 `Cause[E]`에 접근하게 하여, 인터럽션을 포함한 어떤 에러로부터든 복구할 수 있습니다.

```scala
trait ZIO[-R, +E, +A] {
  def foldCause[B](
    failure: Cause[E] => B,
    success: A => B
  ): ZIO[R, Nothing, B]

  def foldCauseZIO[R1 <: R, E2, B](
    failure: Cause[E] => ZIO[R1, E2, B],
    success: A => ZIO[R1, E2, B]
  ): ZIO[R1, E2, B]
}
```

```scala
import zio._
val exceptionalEffect: ZIO[Any, Throwable, Unit] = ???
val myApp: ZIO[Any, IOException, Unit] =
  exceptionalEffect.foldCauseZIO(
    failure = {
      case Cause.Fail(value, _) =>
        Console.printLine(s"failure: $value")
      case Cause.Die(value, _) =>
        Console.printLine(s"cause: $value")
      case Cause.Interrupt(failure, _) =>
        Console.printLine(s"${failure.threadName} interrupted!")
      case _ =>
        Console.printLine("failed due to other causes")
    },
    success = succeed => Console.printLine(s"succeeded with $succeed value")
  )
```

#### 10.3 foldTraceZIO

이 변형은 실패 시 스택 트레이스 정보에 접근하게 합니다.

```scala
trait ZIO[-R, +E, +A] {
  def foldTraceZIO[R1 <: R, E2, B](
    failure: ((E, Trace)) => ZIO[R1, E2, B],
    success: A => ZIO[R1, E2, B]
  )(implicit ev: CanFail[E]): ZIO[R1, E2, B]
}
```

```scala
import zio._
val result: ZIO[Any, Nothing, Int] =
  validate(5).foldTraceZIO(
    failure = {
      case (_: NegativeAgeException, trace) =>
        ZIO.succeed(0).debug(
          "The entered age is negative\n" +
          s"trace info: ${trace.stackTrace.mkString("\n")}"
        )
      case (_: IllegalAgeException, trace) =>
        ZIO.succeed(0).debug(
          "The entered age in not legal\n" +
          s"trace info: ${trace.stackTrace.mkString("\n")}"
        )
    },
    success = s => ZIO.succeed(s)
  )
```

`fold`, `foldZIO`와 마찬가지로 이 연산자도 파이버 인터럽션으로부터는 복구할 수 없습니다.

---

### 11. 재시도(Retrying)

ZIO는 일시적(transient) 실패를 재시도 메커니즘으로 처리하는 여러 연산자를 제공합니다.

#### 11.1 ZIO#retry

기초가 되는 재시도 메서드로, `Schedule` 파라미터를 받습니다.

```scala
trait ZIO[-R, +E, +A] {
  def retry[R1 <: R, S](policy: => Schedule[R1, E, S]): ZIO[R1, E, A]
}
```

```scala
import zio._
val retriedOpenFile: ZIO[Any, IOException, Array[Byte]] =
  readFile("primary.data").retry(Schedule.recurs(5))
```

#### 11.2 ZIO#retryN

고정 횟수만큼 재시도가 필요할 때 로직을 단순화합니다.

```scala
import zio._
val file = readFile("primary.data").retryN(5)
```

#### 11.3 ZIO#retryOrElse

재시도가 소진되면 폴백 동작을 수행합니다. 복구 함수는 최종 에러와 스케줄 출력을 받습니다.

```scala
trait ZIO[-R, +E, +A] {
  def retryOrElse[R1 <: R, A1 >: A, S, E1](
    policy: => Schedule[R1, E, S],
    orElse: (E, S) => ZIO[R1, E1, A1]
  ): ZIO[R1, E1, A1] =
}
```

```scala
import zio._
object MainApp extends ZIOAppDefault {
  def run =
    Random
      .nextIntBounded(11)
      .flatMap { n =>
        if (n < 9)
          ZIO.fail(s"$n is less than 9!").debug("failed")
        else
          ZIO.succeed(n).debug("succeeded")
      }
      .retryOrElse(
        policy = Schedule.recurs(5),
        orElse = (lastError, scheduleOutput: Long) =>
          ZIO.debug(s"after $scheduleOutput retries, we couldn't succeed!") *>
            ZIO.debug(s"the last error message we received was: $lastError") *>
            ZIO.succeed(-1)
      )
      .debug("the final result")
}
```

#### 11.4 ZIO#retryOrElseEither

성공 경로와 실패 경로를 모두 반환합니다.

```scala
import zio._
trait LocalConfig
trait RemoteConfig
def readLocalConfig: ZIO[Any, Throwable, LocalConfig] = ???
def readRemoteConfig: ZIO[Any, Throwable, RemoteConfig] = ???
val result: ZIO[Any, Throwable, Either[RemoteConfig, LocalConfig]] =
  readLocalConfig.retryOrElseEither(
    schedule0 = Schedule.fibonacci(1.seconds),
    orElse = (_, _: Duration) => readRemoteConfig
  )
```

#### 11.5 retryUntil / retryUntilZIO / retryUntilEquals

에러 채널에 대한 술어(predicate)가 만족될 때까지 재시도합니다.

```scala
trait ZIO[-R, +E, +A] {
  def retryUntil(f: E => Boolean): ZIO[R, E, A]
  def retryUntilZIO[R1 <: R](f: E => URIO[R1, Boolean]): ZIO[R1, E, A]
}
```

```scala
sealed trait ServiceError extends Exception
case object TemporarilyUnavailable extends ServiceError
case object DataCorrupted extends ServiceError
def remoteService: ZIO[Any, ServiceError, Unit] = ???

remoteService.retryUntil(_ == DataCorrupted)

// 특정 값과 같아질 때까지 재시도
remoteService.retryUntilEquals(DataCorrupted)
```

#### 11.6 retryWhile / retryWhileZIO / retryWhileEquals

`retryUntil`의 반대로, 술어가 참인 동안 계속 재시도합니다.

```scala
trait ZIO[-R, +E, +A] {
  def retryWhile(f: E => Boolean): ZIO[R, E, A]
  def retryWhileZIO[R1 <: R](f: E => URIO[R1, Boolean]): ZIO[R1, E, A]
}
```

```scala
remoteService.retryWhile(_ == TemporarilyUnavailable)

// 지정한 값과 같은 동안 재시도
remoteService.retryWhileEquals(TemporarilyUnavailable)
```

---

### 12. 타임아웃(Timing Out)

ZIO는 효과에 대한 타임아웃을 처리하는 여러 연산자를 제공합니다. 타임아웃이 발생하면 자원 낭비를 막기 위해 효과가 인터럽트됩니다.

#### 12.1 ZIO#timeout

기본 타임아웃 메서드로 `Option`을 반환합니다.

- 타임아웃 전에 효과가 완료되면 `Some(value)`
- 타임아웃이 경과하면 `None`

```scala
import zio._
object MainApp extends ZIOAppDefault {
  def run =
    myApp
      .timeout(3.second)
      .debug("output")
      .timed
      .map(_._1.toMillis / 1000)
      .debug("execution time of the whole program in second")
}
```

```scala
myApp
  .timeout(1.second)
  .debug("output")
```

효과가 `uninterruptible`로 표시되어 있으면, 타임아웃은 완료될 때까지 기다린 후 `None`을 반환합니다.

```scala
myApp
  .uninterruptible
  .timeout(1.second)
```

**조기 반환 패턴**: `disconnect`를 사용하면 타임아웃 후 조기에 반환할 수 있으며, 그 사이 기저 효과는 백그라운드에서 인터럽트된 채 계속됩니다.

```scala
myApp
  .uninterruptible
  .disconnect
  .timeout(1.second)
```

#### 12.2 ZIO#timeoutTo

타임아웃 시 결과 타입을 커스터마이징합니다.

```scala
val delayedNextInt: ZIO[Any, Nothing, Int] =
  Random.nextIntBounded(10).delay(2.second)
val r1: ZIO[Any, Nothing, Option[Int]] =
  delayedNextInt.timeoutTo(None)(Some(_))(1.seconds)
val r2: ZIO[Any, Nothing, Either[String, Int]] =
  delayedNextInt.timeoutTo(Left("timeout"))(Right(_))(1.seconds)
val r3: ZIO[Any, Nothing, Int] =
  delayedNextInt.timeoutTo(-1)(identity)(1.seconds)
```

#### 12.3 ZIO#timeoutFail / ZIO#timeoutFailCause

타임아웃 시 특정 에러나 Cause를 생성합니다.

```scala
val r1: ZIO[Any, TimeoutException, Int] =
  delayedNextInt.timeoutFail(new TimeoutException)(1.second)
val r2: ZIO[Any, Nothing, Int] =
  delayedNextInt.timeoutFailCause(Cause.die(new Error("timeout")))(1.second)
```

---

### 13. 샌드박싱(Sandboxing)

ZIO 효과는 실패(failure), 결함(defect), 파이버 인터럽션(fiber interruption) 또는 이들의 조합 등 여러 메커니즘으로 실패할 수 있습니다. `sandbox` 연산자는 모든 에러 원인(cause)을 `Cause[E]` 타입으로 에러 채널에 노출합니다.

```scala
trait ZIO[-R, +E, +A] {
  def sandbox: ZIO[R, Cause[E], A]
}
```

#### 기본 예제

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val effect: ZIO[Any, String, String] =
    ZIO.succeed("primary result") *> ZIO.fail("Oh uh!")

  val myApp: ZIO[Any, Cause[String], String] =
    effect.sandbox.catchSome {
      case Cause.Interrupt(fiberId, _) =>
        ZIO.debug(s"Caught interruption of a fiber with id: $fiberId") *>
          ZIO.succeed("fallback result on fiber interruption")
      case Cause.Die(value, _) =>
        ZIO.debug(s"Caught a defect: $value") *>
          ZIO.succeed("fallback result on defect")
      case Cause.Fail(value, _) =>
        ZIO.debug(s"Caught a failure: $value") *>
          ZIO.succeed("fallback result on failure")
    }

  val finalApp: ZIO[Any, String, String] = myApp.unsandbox.debug("final result")

  def run = finalApp
}
// 출력:
// Caught a failure: Oh uh!
// final result: fallback result on failure
```

#### 언샌드박싱(Unsandboxing)

노출된 cause를 처리한 후, `unsandbox`로 `Cause[E]`를 표준 에러 타입으로 다시 변환합니다.

```scala
effect            // ZIO[Any, String, String]
  .sandbox        // ZIO[Any, Cause[String], String]
  .catchSome(???) // ZIO[Any, Cause[String], String]
  .unsandbox      // ZIO[Any, String, String]
```

#### sandboxWith 연산자

`sandboxWith` 메서드는 샌드박싱, 에러 처리, 언샌드박싱을 한 번에 결합합니다.

```scala
trait ZIO[-R, +E, +A] {
  def sandboxWith[R1 <: R, E2, B](f: ZIO[R1, Cause[E], A] => ZIO[R1, Cause[E2], B])
}
```

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val effect: ZIO[Any, String, String] =
    ZIO.succeed("primary result") *> ZIO.fail("Oh uh!")

  val myApp =
    effect.sandboxWith[Any, String, String] { e =>
      e.catchSome {
        case Cause.Interrupt(fiberId, _) =>
          ZIO.debug(s"Caught interruption of a fiber with id: $fiberId") *>
            ZIO.succeed("fallback result on fiber interruption")
        case Cause.Die(value, _) =>
          ZIO.debug(s"Caught a defect: $value") *>
            ZIO.succeed("fallback result on defect")
        case Cause.Fail(value, _) =>
          ZIO.debug(s"Caught a failure: $value") *>
            ZIO.succeed("fallback result on failure")
      }
    }

  def run = myApp.debug
}
// 출력:
// Caught a failure: Oh uh!
// fallback result on failure
```

---

### 14. 에러 채널 매핑 연산(Map Operations)

#### 14.1 mapError / mapErrorCause

에러 채널에 접근하여 변환합니다.

```scala
trait ZIO[-R, +E, +A] {
  def mapError[E2](f: E => E2): ZIO[R, E2, A]
  def mapErrorCause[E2](h: Cause[E] => Cause[E2]): ZIO[R, E2, A]
}
```

```scala
import zio._
def parseInt(input: String): ZIO[Any, NumberFormatException, Int] = ???

// 에러를 메시지 문자열로 매핑
val r1: ZIO[Any, String, Int] =
  parseInt("five")
    .mapError(e => e.getMessage)

// Cause에서 트레이싱 제거
val r2 = parseInt("five")
  .mapErrorCause(_.untraced)
```

핵심 통찰: 효과의 성공 채널이나 에러 채널을 매핑해도 효과의 성공/실패 여부 자체는 바뀌지 않습니다.

#### 14.2 mapAttempt

체크되지 않은(unchecked) 예외를 타입 실패로 변환합니다. map 연산 내부의 예외가 결함이 되는 것을 방지합니다.

```scala
trait ZIO[-R, +E, +A] {
  def mapAttempt[B](f: A => B): ZIO[R, Throwable, B]
}
```

```scala
// 안전하지 않은 매핑 — 파싱 실패 시 결함이 됨
val result: ZIO[Any, Nothing, Int] =
  Console.readLine.orDie.map(_.toInt)

// 안전한 매핑 — 예외가 타입 실패가 됨
val result: ZIO[Any, Throwable, Int] =
  Console.readLine.orDie.mapAttempt(_.toInt)
```

#### 14.3 mapBoth

에러 채널과 성공 채널을 동시에 변환합니다.

```scala
trait ZIO[-R, +E, +A] {
  def mapBoth[E2, B](f: E => E2, g: A => B): ZIO[R, E2, B]
}
```

```scala
val result: ZIO[Any, String, Int] =
  Console.readLine.orDie
    .mapAttempt(_.toInt)
    .mapBoth(
      _ => "non-integer input",
      n => Math.abs(n)
    )
```

---

### 15. 에러 들여다보기: 태핑(Tapping Errors)

태핑(tapping) 연산은 효과의 결과를 바꾸지 않으면서 에러 값을 들여다볼 수 있게 합니다. 로깅, 모니터링, 부수 효과(side effect) 실행에 유용하며, 원래의 에러 채널 동작을 보존합니다.

```scala
trait ZIO[-R, +E, +A] {
  def tapError[R1 <: R, E1 >: E](f: E => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapErrorCause[R1 <: R, E1 >: E](f: Cause[E] => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapErrorTrace[R1 <: R, E1 >: E](f: ((E, Trace)) => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapDefect[R1 <: R, E1 >: E](f: Cause[Nothing] => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapBoth[R1 <: R, E1 >: E](f: E => ZIO[R1, E1, Any], g: A => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapEither[R1 <: R, E1 >: E](f: Either[E, A] => ZIO[R1, E1, Any]): ZIO[R1, E1, Any]
}
```

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val myApp: ZIO[Any, NumberFormatException, Int] =
    Console.readLine
      .mapAttempt(_.toInt)
      .refineToOrDie[NumberFormatException]
      .tapError { e =>
        ZIO.debug(s"user entered an invalid input: ${e}").when(e.isInstanceOf[NumberFormatException])
      }

  def run = myApp
}
```

---

### 16. 에러를 성공 채널로 노출하기(Exposing Errors)

#### 16.1 ZIO#either

`ZIO[R, E, A]` 효과를, 실패와 성공이 모두 성공 채널의 `Either[E, A]`로 들어 올려진(lifted) 효과로 변환합니다. 결과 효과는 실패할 수 없습니다.

```scala
import zio._
val age: Int = ???
val res: URIO[Any, Either[AgeValidationException, Int]] =
  validate(age).either
```

```scala
val myApp: ZIO[Any, IOException, Unit] =
  for {
    _ <- Console.print("Please enter your age: ")
    age <- Console.readLine.map(_.toInt)
    res <- validate(age).either
    _ <- res match {
      case Left(error) => ZIO.debug(s"validation failed: $error")
      case Right(age) => ZIO.debug(s"The $age validated!")
    }
  } yield ()
```

#### 16.2 ZIO#absolve / ZIO.absolve

`either`의 역연산으로, `Either`의 에러 케이스를 ZIO 안으로 가라앉힙니다(submerge).

```scala
validate(age)
  .either
  .absolve
```

```scala
def sqrt(input: ZIO[Any, Nothing, Double]):
  ZIO[Any, String, Double] =
  ZIO.absolve(
    input.map { value =>
      if (value < 0.0)
        Left("Value must be >= 0.0")
      else
        Right(Math.sqrt(value))
    }
  )
```

> 이 외에도 에러나 Cause를 성공 채널로 노출하는 `option`, `cause`, `exit`, `sandbox` 등의 연산이 있습니다.

---

### 17. 에러 채널과 성공 채널 합치기(Merge & Flip)

#### 17.1 merge

`ZIO#merge`로 에러 채널을 성공 채널로 병합할 수 있습니다.

```scala
import zio._
val merged : ZIO[Any, Nothing, String] =
  ZIO.fail("Oh uh!")     // ZIO[Any, String, Nothing]
    .merge               // ZIO[Any, Nothing, String]
```

**핵심**: 에러 채널과 성공 채널의 타입이 다를 경우, 두 채널의 공통 상위 타입(common supertype)이 선택됩니다.

#### 17.2 flip / flipWith

`flip`은 에러 채널과 성공 채널을 서로 뒤바꿉니다. `flipWith`는 뒤집힌 상태에서 변환을 적용한 뒤 다시 되돌립니다. (자세한 내용은 "Flipping Error and Success Channels" 페이지 참고)

---

### 18. 에러 정제: 실패와 결함의 변환(Error Refinement)

에러 정제(error refinement)는 실패(failure)와 결함(defect) 사이의 변환을 포함합니다. `ZIO#refine*`와 `ZIO#unrefine*`는 에러 동작 자체를 바꾸지 않고 에러 모델(error model)만 변경합니다.

#### 18.1 ZIO#refineToOrDie

에러 채널을 타입 `E`에서 `E1`로 좁히며(narrow), 매치되지 않는 에러는 결함으로 변환합니다.

```scala
trait ZIO[-R, +E, +A] {
  def refineToOrDie[E1 <: E]: ZIO[R, E1, A]
}
```

```scala
import zio._

def parseInt(input: String): ZIO[Any, NumberFormatException, Int] =
  ZIO.attempt(input.toInt)
    .refineToOrDie[NumberFormatException]
```

`NumberFormatException` 이외의 예외는 결함이 됩니다.

#### 18.2 ZIO#refineOrDie

부분 함수(partial function)를 사용해 여러 에러 타입을 정제하는 더 강력한 변형입니다.

```scala
trait ZIO[-R, +E, +A] {
  def refineOrDie[E1](pf: PartialFunction[E, E1]): ZIO[R, E1, A]
}
```

```scala
sealed abstract class DomainError(msg: String) extends Exception(msg)
case class Foo(msg: String) extends DomainError(msg)
case class Bar(msg: String) extends DomainError(msg)
case class Baz(msg: String) extends DomainError(msg)

val refined: ZIO[Any, DomainError, Unit] =
  effect.refineOrDie {
    case foo: Foo => foo
    case bar: Bar => bar
  }
```

`Baz` 케이스는 결함이 됩니다.

#### 18.3 ZIO#refineOrDieWith

에러 타입이 `Throwable`이 아닐 때, 에러를 `Throwable`로 변환하는 함수를 요구합니다.

```scala
trait ZIO[-R, +E, +A] {
  def refineOrDieWith[E1](pf: PartialFunction[E, E1])(f: E => Throwable): ZIO[R, E1, A]
}
```

```scala
val refined: ZIO[Any, String, Nothing] =
  effect("baz").refineOrDieWith {
    case "FooError" | "BarError" => "Oh Uh!"
  }(e => new Throwable(e))
```

#### 18.4 orDie

`orDie`는 에러 채널의 실패를 결함으로 변환합니다. 즉, 복구 불가능한 실패를 파이버를 죽이는 결함으로 만듭니다. (22장 모범 사례 예제 참고)

---

### 19. 결함을 실패로 변환하기(Converting Defects to Failures)

결함(타입화되지 않은 에러)을 실패(타입 에러)로 변환하는 연산자들입니다. 이들은 실패를 결함으로 바꾸는 `orDie`의 대칭적 반대 연산입니다.

```scala
trait ZIO[-R, +E, +A] {
  def absorb(implicit ev: E IsSubtypeOfError Throwable): ZIO[R, Throwable, A]
  def absorbWith(f: E => Throwable): ZIO[R, Throwable, A]
  def resurrect(implicit ev1: E IsSubtypeOfError Throwable): ZIO[R, Throwable, A]
}
```

#### absorb vs resurrect

**핵심 차이:**

- `absorb`는 `Die`와 `Interruption` cause 모두로부터 복구하여 모든 원인을 실패로 변환하되, cause 정보는 폐기합니다.
- `resurrect`는 `Die` cause로부터만 복구하며, 인터럽션은 그대로 전파합니다.

```scala
import zio._
object MainApp extends ZIOAppDefault {
  val effect1 =
    ZIO.dieMessage("Boom!")
      .absorb
      .ignore
  val effect2 =
    ZIO.interrupt
      .absorb
      .ignore
  def run =
    (effect1 <*> effect2)
      .debug("application exited successfully")
}
```

```scala
import zio._
object MainApp extends ZIOAppDefault {
  val effect1 =
    ZIO.dieMessage("Boom!")
      .resurrect
      .ignore
  val effect2 =
    ZIO.interrupt
      .resurrect
      .ignore
  def run =
    (effect1 <*> effect2)
      .debug("couldn't recover from fiber interruption")
}
```

`resurrect`는 인터럽션을 만나면 복구하지 않고 `InterruptedException`을 발생시킵니다.

---

### 20. 에러 누적(Error Accumulation)

ZIO의 순차 컴비네이터인 `zip`, `foreach`는 빠른 실패(fail-fast) 에러 처리를 사용합니다. 그러나 일부 연산자는 첫 실패에서 멈추지 않고 **에러를 누적**(accumulate)합니다.

#### 20.1 ZIO#validate

두 ZIO 효과를 순차적으로 zip합니다. 둘 다 실패하면 `Cause.Then`으로 cause를 결합합니다.

```scala
trait ZIO[-R, +E, +A] {
  def validate[R1 <: R, E1 >: E, B](that: => ZIO[R1, E1, B]): ZIO[R1, E1, (A, B)]
}
```

```scala
import zio._
object MainApp extends ZIOAppDefault {
  val f1 = ZIO.succeed(1).debug
  val f2 = ZIO.succeed(2) *> ZIO.fail("Oh uh!")
  val f3 = ZIO.succeed(3).debug
  val f4 = ZIO.succeed(4) *> ZIO.fail("Oh error!")
  val f5 = ZIO.succeed(5).debug
  val myApp: ZIO[Any, String, (Int, Int, Int)] =
    f1 validate f2 validate f3 validate f4 validate f5
  def run = myApp.cause.debug.uncause
}
// 출력:
// 1
// 3
// 5
// Then(Fail(Oh uh!,Trace(None,Chunk())),Fail(Oh error!,Trace(None,Chunk())))
```

#### 20.2 ZIO#validatePar

`validate`와 유사하지만 병렬로 실행됩니다. 실패를 `Cause.Both`로 결합합니다.

```scala
import zio._
object MainApp extends ZIOAppDefault {
  val f1 = ZIO.succeed(1).debug
  val f2 = ZIO.succeed(2) *> ZIO.fail("Oh uh!")
  val f3 = ZIO.succeed(3).debug
  val f4 = ZIO.succeed(4) *> ZIO.fail("Oh error!")
  val f5 = ZIO.succeed(5).debug
  val myApp: ZIO[Any, String, ((((Int, Int), Int), Int), Int)] =
    f1 validatePar f2 validatePar f3 validatePar f4 validatePar f5
  def run = myApp.cause.map(_.untraced).debug.uncause
}
// 가능한 출력 중 하나:
// 3
// 1
// 5
// Both(Fail(Oh uh!,Trace(None,Chunk())),Fail(Oh error!,Trace(None,Chunk())))
```

#### 20.3 ZIO.validate (컬렉션)

컬렉션 원소를 효과적인 연산으로 변환하며, 모든 에러를 에러 채널에 모읍니다.

```scala
object ZIO {
  def validate[R, E, A, B](in: Collection[A])(
    f: A => ZIO[R, E, B]
  ): ZIO[R, ::[E], Collection[B]]
  def validate[R, E, A, B](in: NonEmptyChunk[A])(
    f: A => ZIO[R, E, B]
  ): ZIO[R, ::[E], NonEmptyChunk[B]]
}
```

```scala
// 실패 시나리오
import zio._
object MainApp extends ZIOAppDefault {
  val res: ZIO[Any, ::[String], List[Int]] =
    ZIO.validate(List.range(1, 7)){ n =>
      if (n < 5)
        ZIO.succeed(n)
      else
        ZIO.fail(s"$n is not less that 5")
    }
  def run = res.debug
}
// 출력:
// <FAIL> List(5 is not less that 5, 6 is not less that 5)
```

```scala
// 성공 시나리오
import zio._
object MainApp extends ZIOAppDefault {
  val res: ZIO[Any, ::[String], List[Int]] =
    ZIO.validate(List.range(1, 4)){ n =>
      if (n < 5)
        ZIO.succeed(n)
      else
        ZIO.fail(s"$n is not less that 5")
    }
  def run = res.debug
}
// 출력:
// List(1, 2, 3)
```

**관련 변형**: `ZIO.validatePar`(병렬), `ZIO.validateDiscard`, `ZIO.validateParDiscard`(성공을 폐기하고 `Unit` 반환).

#### 20.4 ZIO.validateFirst

에러를 모두 누적하되, 첫 번째 성공만 반환합니다.

```scala
object ZIO {
  def validateFirst[R, E, A, B](in: Collection[A])(
    f: A => ZIO[R, E, B]
  ): ZIO[R, Collection[E], B]
}
```

```scala
// 실패 시나리오
import zio._
object MainApp extends ZIOAppDefault {
  val res: ZIO[Any, List[String], Int] =
    ZIO.validateFirst(List.range(5, 10)) { n =>
      if (n < 5)
        ZIO.succeed(n)
      else
        ZIO.fail(s"$n is not less that 5")
    }
  def run = res.debug
}
// 출력:
// <FAIL> List(5 is not less that 5, 6 is not less that 5, 7 is not less that 5, 8 is not less that 5, 9 is not less that 5)
```

```scala
// 성공 시나리오
import zio._
object MainApp extends ZIOAppDefault {
  val res: ZIO[Any, List[String], Int] =
    ZIO.validateFirst(List.range(1, 4)) { n =>
      if (n < 5)
        ZIO.succeed(n)
      else
        ZIO.fail(s"$n is not less that 5")
    }
  def run = res.debug
}
// 출력:
// 1
```

#### 20.5 ZIO.partition

성공 채널에 실패와 성공의 튜플(tuple)을 생성합니다. 이는 비예외적(unexceptional) 연산자입니다(에러 타입이 `Nothing`).

```scala
object ZIO {
  def partition[R, E, A, B](in: => Iterable[A])(
    f: A => ZIO[R, E, B]
  ): ZIO[R, Nothing, (Iterable[E], Iterable[B])]
}
```

```scala
import zio._
val res: ZIO[Any, Nothing, (Iterable[String], Iterable[Int])] =
  ZIO.partition(List.range(0, 7)){ n =>
    if (n % 2 == 0)
      ZIO.succeed(n)
    else
      ZIO.fail(s"$n is not even")
  }
res.debug
// 출력:
// (List(1 is not even, 3 is not even, 5 is not even),List(0, 2, 4, 6))
```

---

### 21. Cause 데이터 타입(The Cause Data Type)

`Cause[E]`는 ZIO의 기저 에러 표현(error representation)으로, "예상치 못한 에러나 결함, 스택 및 실행 트레이스(stack and execution traces), 파이버 인터럽션 원인"을 포함한 완전한 실패 이야기(failure story)를 담습니다. 이로써 ZIO의 에러 모델은 **무손실(lossless)** — 실패 처리 과정에서 어떠한 정보도 버려지지 않습니다.

#### Cause 생성자(Constructors)

`Cause`는 sealed abstract class로 구현되며 다음 케이스를 가집니다.

1. **Empty** — 에러의 부재를 나타냄. `Cause.empty`로 생성
2. **Fail[E]** — 타입 `E`의 예상된 에러를 트레이스 정보와 함께 포착
3. **Die** — 결함(예상치 못한 `Throwable` 예외)을 완전한 스택 트레이스와 함께 표현
4. **Interrupt** — 파이버 인터럽션을 나타내며, 인터럽트된 파이버의 `FiberId`와 트레이스를 저장
5. **Stackless** — 다른 cause를 감싸며, 완전한 스택 트레이스를 출력할지 제어하는 불리언(boolean) 플래그를 가짐
6. **Then** — 순차적 에러 합성을 인코딩(예: try와 finally 블록이 모두 실패한 경우)
7. **Both** — 여러 동시 연산이 동시에 실패할 때의 병렬 cause 합성을 저장

#### 세미링 구조(Semiring Structure)

이 구현은 함수형 프로그래밍의 자료 구조인 **세미링**(semiring)을 사용하여, 모든 실패 정보를 보존하면서 순차(`++`) 및 병렬(`&&`) 에러 합성을 가능하게 합니다.

#### 주요 메서드

- `++`: 순차 결합(sequential combination)
- `&&`: 병렬 결합(parallel combination)

이를 통해 ZIO는 정보 손실 없이 복잡한 실패 시나리오를 구성할 수 있습니다. 또한 `squashWith` 같은 메서드로 cause를 단일 에러로 압축(squash)할 수 있습니다(8.3절 `catchAllCause` 예제 참고).

---

### 22. 모범 사례(Best Practices)

#### 22.1 에러 모델링에 대수적 데이터 타입(ADT) 사용하기

핵심 권고는 "같은 도메인(domain) 또는 하위 도메인(subdomain) 내에서 에러를 모델링할 때 **대수적 데이터 타입**(algebraic data types, ADTs)을 사용하라"는 것입니다.

봉인된 트레이트(sealed trait)가 이 접근의 기반이 됩니다.

```scala
sealed trait UserServiceError extends Exception
case class InvalidUserId(id: ID) extends UserServiceError
case class ExpiredAuth(id: ID)   extends UserServiceError
```

이는 합 타입(sum type)을 생성하여 컴파일러가 가능한 모든 에러 변형을 인식하게 합니다. 패턴 매칭은 완전하고(exhaustive) 타입 안전(type-safe)해집니다.

```scala
userServiceError match {
  case InvalidUserId(id) => ???
  case ExpiredAuth(id)   => ???
}
```

**이점:**

- **컴파일러 지원**: 봉인된 트레이트는 불완전한 커버리지에 대해 경고하며 완전한 패턴 매칭을 가능하게 합니다.
- **치명적 경고(Fatal Warnings)**: `-Xfatal-warnings` 컴파일러 옵션은 경고를 에러로 전환하여 타입 안전성 보증을 강제합니다.
- **타입 확장(Type Widening)**: `flatMap` 같은 ZIO 컴비네이터는 가장 구체적인(most specific) 에러 타입으로 자동 확장하여 타입 추론(type inference)을 개선합니다.

```scala
val myApp: IO[Exception, Receipt] =
  for {
    service <- userAuth(token)                // IO[ExpiredAuth, UserService]
    profile <- service.userProfile(userId)    // IO[InvalidUserId, Profile]
    body    <- generateEmail(orderDetails)    // IO[Nothing, String]
    receipt <- sendEmail("Your order detail", body, profile.email)
                                              // IO[EmailDeliveryError, Unit]
  } yield receipt
```

#### 22.2 유니온 타입 사용하기(Union Types) — Scala 3

Scala 3의 유니온 타입(union type)은 더 정밀한 에러 타입 명시를 가능하게 합니다. "유니온 연산자(`|`)를 사용하면 공통 상위 타입(common supertype) 없이도 여러 에러 타입을 인코딩"할 수 있습니다.

```scala
enum StorageError extends Exception {
  case ObjectExist(name: Name)
  case ObjectNotExist(name: Name)
  case PermissionDenied(cause: String)
  case StorageLimitExceeded(limit: Int)
  case BandwidthLimitExceeded(limit: Int)
}

trait Storage {
  def upload(name: Name, obj: Array[Byte]):
    ZIO[Any, ObjectExist | StorageLimitExceeded, Unit]
  def download(name: Name):
    ZIO[Any, ObjectNotExist | BandwidthLimitExceeded, Array[Byte]]
  def delete(name: Name):
    ZIO[Any, ObjectNotExist | PermissionDenied, Unit]
}
```

유니온 타입은 공통 상위 타입 없이 완전히 독립적인 에러 정의를 결합할 수 있게 합니다.

```scala
trait FooError
trait BarError

def foo: IO[FooError, Nothing] = ZIO.fail(new FooError {})
def bar: IO[BarError, Nothing] = ZIO.fail(new BarError {})

val myApp: ZIO[Any, FooError | BarError, Unit] = for {
  _ <- foo
  _ <- bar
} yield ()
```

#### 22.3 예상치 못한 에러를 타입화하지 말 것(Don't Type Unexpected Errors)

모든 잠재적 에러를 타입 에러 파라미터에 포함하려는 유혹을 피해야 합니다. 모든 종류의 에러에서 복구할 수는 없으며, 예상치 못한 에러를 만났을 때는 할 수 있는 일이 없습니다.

Erlang 철학이 잘 들어맞습니다 — 예상치 못한 에러는 처리하려 하지 말고 애플리케이션이 크래시되게 두라는 것입니다. 에러가 예상된 것인지 아닌지는 도메인 맥락에 따라 달라집니다.

```scala
// orDie 사용 — 타입 실패를 타입화되지 않은 결함으로 변환
import zio._
Console.printLine("Hello, World") // ZIO[Any, IOException, Unit]
  .orDie                          // ZIO[Any, Nothing, Unit]
```

```scala
// refineOrDie 사용 — 복구 가능한 예외만 선택적으로 타입화
import zio._
val response: ZIO[Any, Nothing, Response] =
  ZIO
    .attemptBlocking(
      httpClient.fetchUrl(url)
    ) // ZIO[Any, Throwable, Response]
    .refineOrDie[TemporaryUnavailable] {
      case e: TemporaryUnavailable => e
    } // ZIO[Any, TemporaryUnavailable, Response]
    .retry(
      Schedule.fibonacci(1.second)
    )
    .orDie // ZIO[Any, Nothing, Response]
```

이 패턴은 복구 가능한 예외(`TemporaryUnavailable` 등)를 타입 에러로 승격(promote)시키고, `OutOfMemoryError` 같은 결함은 파이버를 죽이도록 둡니다.

#### 22.4 반사적으로 에러를 로깅하지 말 것(Don't Reflexively Log Errors)

에러를 처리하거나 복구하는 모든 지점에서 습관적으로 로깅하면, 같은 에러가 콜 스택의 여러 계층에서 중복 로깅되어 노이즈를 유발합니다. 에러는 실제로 책임지고 처리하는 단 한 곳에서만 로깅하는 것이 바람직합니다.

---

### 23. 참고 자료

- [Handling Errors (Overview)](https://zio.dev/overview/handling-errors)
- [Error Management (Reference)](https://zio.dev/reference/error-management/)
- [Three Types of Errors](https://zio.dev/reference/error-management/types/)
- [Imperative vs. Declarative](https://zio.dev/reference/error-management/imperative-vs-declarative)
- [Expected and Unexpected Errors](https://zio.dev/reference/error-management/expected-and-unexpected-errors)
- [Exceptional and Unexceptional Effects](https://zio.dev/reference/error-management/exceptional-and-unexceptional-effects)
- [Typed Errors Guarantees](https://zio.dev/reference/error-management/typed-errors-guarantees)
- [Sequential and Parallel Errors](https://zio.dev/reference/error-management/sequential-and-parallel-errors)
- [Recovering: Catching](https://zio.dev/reference/error-management/recovering/catching)
- [Recovering: Fallback](https://zio.dev/reference/error-management/recovering/fallback)
- [Recovering: Folding](https://zio.dev/reference/error-management/recovering/folding)
- [Recovering: Retrying](https://zio.dev/reference/error-management/recovering/retrying)
- [Recovering: Timing Out](https://zio.dev/reference/error-management/recovering/timing-out)
- [Recovering: Sandboxing](https://zio.dev/reference/error-management/recovering/sandboxing)
- [Operations: Map Operations](https://zio.dev/reference/error-management/operations/map-operations)
- [Operations: Tapping Errors](https://zio.dev/reference/error-management/operations/tapping-errors/)
- [Operations: Error Refinement](https://zio.dev/reference/error-management/operations/error-refinement/)
- [Operations: Converting Defects to Failures](https://zio.dev/reference/error-management/operations/converting-defects-to-failures/)
- [Error Accumulation](https://zio.dev/reference/error-management/error-accumulation)
- [Best Practices: Algebraic Data Types](https://zio.dev/reference/error-management/best-practices/algebraic-data-types)
- [Best Practices: Union Types](https://zio.dev/reference/error-management/best-practices/union-types)
- [Best Practices: Don't Type Unexpected Errors](https://zio.dev/reference/error-management/best-practices/unexpected-errors/)
- [The Cause Data Type](https://zio.dev/reference/core/cause)

---

## ZIO 리소스 관리

> 원본: https://zio.dev/reference/resource/

---

### 목차

1. [리소스 관리 개요(Resource Management Overview)](#1-리소스-관리-개요resource-management-overview)
2. [ensuring를 통한 종료 처리(Finalizing with ensuring)](#2-ensuring를-통한-종료-처리finalizing-with-ensuring)
3. [획득-해제 패턴(Acquire-Release Pattern)](#3-획득-해제-패턴acquire-release-pattern)
4. [AutoCloseable 리소스(fromAutoCloseable)](#4-autocloseable-리소스fromautocloseable)
5. [Scope: 안전하고 합성 가능한 리소스 관리(Scope)](#5-scope-안전하고-합성-가능한-리소스-관리scope)
6. [ZPool: 리소스 풀링(ZPool)](#6-zpool-리소스-풀링zpool)
7. [ZKeyedPool: 키 기반 리소스 풀링(ZKeyedPool)](#7-zkeyedpool-키-기반-리소스-풀링zkeyedpool)
8. [ScopedRef: 리소스를 위한 가변 참조(ScopedRef)](#8-scopedref-리소스를-위한-가변-참조scopedref)
9. [참고 자료](#9-참고-자료)

---

### 1. 리소스 관리 개요(Resource Management Overview)

대규모 애플리케이션에서 적절한 리소스 관리(resource management)는 매우 중요합니다. 커넥션(connection)이나 파일 디스크립터(file descriptor)를 누수(leak)시키지 않는 것이 리소스 관리의 핵심 목표입니다.

#### 전통적인 try/finally 패턴의 한계

Scala의 전통적인 `try`/`finally` 구문은 리소스 정리를 보장하는 기본적인 방법입니다.

```scala
def lines(file: String): Task[Long] = ZIO.attempt {
  def countLines(br: BufferedReader): Long = br.lines().count()
  val bufferedReader = new BufferedReader(
    new InputStreamReader(new FileInputStream("file.txt")),
    2048
  )
  try countLines(bufferedReader)
  finally bufferedReader.close()
}
```

하지만 `try`/`finally`에는 다음과 같은 단점(drawbacks)이 있습니다.

- 여러 리소스를 다룰 때 합성(compose)하기 어렵습니다.
- 코드가 지저분해지고 추론(reason)하기 어려워집니다.
- 정리(cleanup) 순서를 제어할 수 없습니다.
- 동시적인(concurrent) 리소스 합성을 처리할 수 없습니다.
- 비동기(asynchronous) 워크플로를 지원하지 않습니다.
- 수작업이 필요하며 오류가 발생하기 쉽습니다(manual and error-prone).

ZIO는 이러한 문제를 해결하기 위해 여러 가지 메커니즘을 제공하며, 비동기 연산과 동시적 이펙트(effect) 전반에 걸쳐 리소스를 안전하게 관리합니다.

---

### 2. ensuring를 통한 종료 처리(Finalizing with ensuring)

`ensuring` 메서드는 `try`/`finally`와 동일한 의미(semantics)를 가집니다. 즉, 어떤 이펙트가 실행을 시작한 뒤 정상적으로든 비정상적으로든(either normally or abnormally) 종료되면 종료자(finalizer)가 반드시 실행됨을 보장합니다.

```scala
val finalizer: UIO[Unit] = ZIO.succeed(println("Finalizing!"))
val finalized: IO[String, Unit] = ZIO.fail("Failed!").ensuring(finalizer)
```

#### 핵심 사항

- 종료자(finalizer)는 복구 가능한(recoverable) 방식으로 실패할 수 없으므로, 오류는 반드시 처리(handle)되어야 합니다.
- 중첩된(nested) 종료자들은 추가된 순서의 역순(reverse order)으로 순차적으로 실행됩니다.
- 안쪽 종료자(inner finalizer)의 실패가 바깥쪽 종료자(outer finalizer)의 실행을 막지 않습니다.

---

### 3. 획득-해제 패턴(Acquire-Release Pattern)

`ZIO.acquireReleaseWith`는 리소스의 생명 주기(lifecycle)를 안전하게 관리하며, 세 가지 구성 요소로 이루어집니다.

1. **획득 이펙트(Acquire effect)** - 리소스를 획득합니다(예: 파일 열기).
2. **해제 함수(Release function)** - 리소스를 해제하는 이펙트를 반환합니다(예: 파일 닫기).
3. **사용 함수(Use function)** - 리소스를 소비하는 이펙트입니다(예: 처리하기).

일반적인 구조는 다음과 같습니다.

```scala
val result: Task[Any] = ZIO.acquireReleaseWith(acquire)(release)(use)
```

#### 파일 읽기 예제

```scala
def lines(file: String): Task[Long] = {
  def countLines(reader: BufferedReader): Task[Long] =
    ZIO.attempt(reader.lines().count())
  def releaseReader(reader: BufferedReader): UIO[Unit] =
    ZIO.succeed(reader.close())
  def acquireReader(file: String): Task[BufferedReader] =
    ZIO.attempt(new BufferedReader(new FileReader(file), 2048))
  ZIO.acquireReleaseWith(acquireReader(file))(releaseReader)(countLines)
}
```

데이터를 그룹화하는 예제도 같은 방식으로 작성할 수 있습니다.

```scala
val groupedFileData: IO[IOException, Unit] =
  ZIO.acquireReleaseWith(openFile("data.json"))(closeFile(_)) { file =>
    for {
      data    <- decodeData(file)
      grouped <- groupData(data)
    } yield grouped
  }
```

#### 중첩 패턴을 사용한 파일 전송

두 개의 리소스를 병렬로 획득하고 해제하는 파일 전송 예제입니다.

```scala
def transfer(src: String, dst: String): ZIO[Any, Throwable, Unit] =
  ZIO.acquireReleaseWith {
    is(src).zipPar(os(dst))
  } { case (in, out) =>
    ZIO.succeed(in.close()).zipPar(ZIO.succeed(out.close()))
  } { case (in, out) =>
    copy(in, out)
  }
```

#### 한계점(Limitations)

`acquireReleaseWith` 패턴은 다음과 같은 한계가 있습니다.

- 쉽게 합성(compose)되지 않습니다.
- 여러 리소스를 다룰 때 중첩 패턴(nested pattern)이 지저분해집니다.

이 한계를 해결하기 위해 ZIO는 합성 가능한 데이터 타입인 `Scope`를 제공합니다(5장 참고).

---

### 4. AutoCloseable 리소스(fromAutoCloseable)

`AutoCloseable`을 구현한 리소스의 경우, `ZIO.fromAutoCloseable`을 사용하면 Java의 try-with-resource와 동등한 편리한 처리가 가능합니다.

```scala
val bytesInFile: IO[Throwable, Int] =
  ZIO.scoped {
    for {
      stream <- ZIO.fromAutoCloseable(openFileInputStream("data.json"))
      data   <- ZIO.attemptBlockingIO(stream.readAllBytes())
    } yield data.length
  }
```

`acquireReleaseWith` 방식과 `fromAutoCloseable` 방식 모두 오류(error)나 인터럽트(interruption)가 발생하더라도 해제(release)가 반드시 실행됨을 보장합니다.

---

### 5. Scope: 안전하고 합성 가능한 리소스 관리(Scope)

`Scope` 데이터 타입은 ZIO에서 안전하고 합성 가능한 리소스 처리(safe and composable resources handling)의 기반(foundation)입니다. 스코프(scope)는 하나 이상의 리소스의 수명(lifetime)을 나타내며, 스코프가 닫힐(close) 때 해당 리소스들이 반드시 해제됨을 보장합니다.

#### 5.1 Scope 인터페이스

```scala
trait Scope {
  def addFinalizerExit(finalizer: Exit[Any, Any] => UIO[Any]): UIO[Unit]
  def close(exit: => Exit[Any, Any]): UIO[Unit]
}
object Scope {
  def make: UIO[Scope] = ???
}
```

#### 5.2 리소스 정의 연산자(Resource Definition Operators)

스코프 리소스를 생성하는 기본 생성자(constructor)는 `acquireRelease`입니다.

```scala
def acquireRelease[R, E, A](acquire: => ZIO[R, E, A])
  (release: A => ZIO[R, Nothing, Any]): ZIO[R with Scope, E, A]
```

`acquireRelease` 연산자는 `acquire` 워크플로를 인터럽트 불가능하게(uninterruptibly) 수행합니다. 이는 리소스 획득이 부분적으로(partial) 인터럽트되는 것을 방지합니다.

종료 결과(exit)에 따라 정리 로직을 달리하고 싶을 때는 `acquireReleaseExit` 변형을 사용합니다.

```scala
def acquireReleaseExit[R, E, A](acquire: => ZIO[R, E, A])
  (release: (A, Exit[Any, Any]) => ZIO[R, Nothing, Any]): ZIO[R with Scope, E, A]
```

해제(release)가 리소스에 의존하지 않는 경우를 위한 인터럽트 가능한(interruptible) 변형도 존재합니다.

#### 5.3 스코프 닫기와 사용(ZIO.scoped)

```scala
object ZIO {
  def scoped[R, E, A](zio: ZIO[Scope with R, E, A]): ZIO[R, E, A] = ???
}
```

`ZIO.scoped`는 환경(environment)에서 `Scope`를 제거하여, 이 워크플로가 더 이상 스코프를 필요로 하는 리소스를 사용하지 않음을 나타냅니다. 즉, 스코프가 이 지점에서 닫히고 모든 종료자가 실행됩니다.

#### 5.4 Scope를 사용한 파일 전송 리팩터링

3장의 중첩 패턴을 `Scope`로 리팩터링하면, 여러 리소스를 평평하게(flat) 합성할 수 있습니다.

```scala
def transfer(from: String, to: String): ZIO[Any, Throwable, Unit] = {
  val resource = for {
    from <- ZIO.acquireRelease(is(from))(close)
    to   <- ZIO.acquireRelease(os(to))(close)
  } yield (from, to)
  ZIO.scoped {
    resource.flatMap { case (in, out) =>
      copy(in, out)
    }
  }
}
```

기본 사용 형태는 다음과 같습니다.

```scala
val scoped = ZIO.acquireRelease(acquire)(release)
```

#### 5.5 종료자 순서(Finalizer Ordering)

기본적으로, `Scope`가 닫힐 때 그 스코프에 추가된 모든 종료자(finalizer)는 추가된 순서의 역순(reverse of the order)으로 닫힙니다. 이는 리소스 의존적인 해제(resource-dependent release)가 실패하는 것을 방지합니다. 예를 들어 리소스 A를 먼저 획득하고 그에 의존하는 리소스 B를 나중에 획득했다면, 해제 시에는 B를 먼저 닫고 A를 나중에 닫게 됩니다.

#### 5.6 고급 연산(Advanced Operations)

**스코프 확장(Extending scopes):** `Scope#extend` 연산자는 `Scope`를 필요로 하는 `ZIO` 이펙트를 받아 `Scope`를 제공하되, 이후에 그것을 닫지 않습니다(without closing it afterwards). 이를 통해 스코프의 수명을 더 바깥쪽으로 연장할 수 있습니다.

**병렬 종료(Parallel finalization):** 종료자들을 병렬로 실행하려면 `ZIO.parallelFinalizers`를 사용합니다.

```scala
def zipScoped[R <: Scope, E, A, B](
  left: ZIO[R, E, A],
  right: ZIO[R, E, B]): ZIO[R, E, (A, B)] =
  ZIO.parallelFinalizers(left.zipPar(right))
```

**서비스 접근(Service access):** `ZIO.service[Scope]`를 호출하면 `Scope` 서비스를 직접 얻을 수 있습니다.

#### 5.7 데이터 타입 변환(Data Type Conversion)

스코프 리소스(scoped resource)는 `scoped` 연산자를 통해 다른 ZIO 구성체로 변환될 수 있습니다. `ZStream`, `ZLayer` 등의 타입은 `scoped` 연산자를 제공하며, 이를 통해 수명 관리(lifetime governance)의 책임을 해당 구성체로 옮길 수 있습니다.

---

### 6. ZPool: 리소스 풀링(ZPool)

`ZPool[E, A]`는 재사용 가능한 리소스 풀을 관리합니다. `E`는 발생 가능한 오류 타입을, `A`는 항목의 타입을 나타냅니다. ZPool은 비동기(asynchronous)·동시성(concurrent) 환경을 지원하는 풀 구현체입니다.

#### 6.1 동기(Motivation)

데이터베이스, 커넥션, 스레드(thread)와 같이 비용이 큰 리소스를 반복적으로 획득하면 성능 오버헤드(performance overhead)가 발생합니다. ZPool은 다음과 같은 방식으로 이 문제를 해결합니다.

- 리소스 풀을 미리 초기화(pre-initialize)하여 "빠르고 예측 가능한(fast and predictable)" 접근을 제공합니다.
- 클라이언트가 새 리소스를 생성하는 대신 기존 리소스에서 획득(acquire)하도록 합니다.
- 해제 메커니즘(release mechanism)을 통해 리소스 재활용(recycling)을 가능하게 합니다.

#### 6.2 ZPool 인터페이스

```scala
trait ZPool[+Error, Item] {
  def get: ZIO[Scope, Error, Item]
  def invalidate(item: Item): UIO[Unit]
}
```

- **`get`**: 스코프 이펙트(scoped effect) 내에서 풀로부터 항목을 가져옵니다. 반환 타입이 `ZIO[Scope, Error, Item]`이므로, 사용이 끝나면 스코프가 닫힐 때 해당 항목이 풀로 반환됩니다.
- **`invalidate`**: 결함이 있는(faulty) 리소스를 표시하여 추후 재할당(reallocation)되도록 합니다.

#### 6.3 풀 생성(Pool Construction)

##### 고정 크기 풀(Fixed-Size Pools)

`make` 생성자는 즉시 할당(eagerly-allocated)되는 풀을 생성합니다.

```scala
def make[E, A](get: ZIO[Scope, E, A], size: Int):
  ZIO[Scope, Nothing, ZPool[E, A]]
```

모든 항목(entry)이 리소스를 즉시 미리 할당(pre-allocate)합니다. 반환 타입 `ZIO[Scope, ...]`은 풀의 생명 주기 정리(lifecycle cleanup)를 자동으로 관리합니다.

정리(cleanup)가 필요 없는 리소스의 경우 `ZPool.fromIterable`을 사용하는 것이 적합합니다.

##### 동적 크기 풀(Dynamic Sizing)

두 번째 변형은 축출(eviction)을 동반한 탄력적 확장(elastic scaling)을 지원합니다.

```scala
def make[E, A](get: ZIO[Scope, E, A],
  range: Range, timeToLive: Duration):
  ZIO[Scope, Nothing, ZPool[E, A]]
```

이 풀은 최소(minimum) 개수의 항목을 미리 할당한 상태를 유지하면서, 필요에 따라(on-demand) 최대(maximum) 용량까지 확장할 수 있습니다. "축출 정책(eviction policy)"은 `timeToLive` 기간을 초과하여 사용되지 않은 항목을 제거하여, 풀을 다시 최소 크기로 축소(contract)합니다.

#### 6.4 리소스 획득 동작(Resource Acquisition Behavior)

`pool.get`을 호출할 때의 동작은 다음과 같습니다.

- 미리 할당된 리소스가 있으면 즉시(immediately) 반환됩니다.
- 수요(demand)가 용량을 초과하면 필요에 따라(on-demand) 할당이 트리거됩니다.
- 요청이 최대 크기를 초과하면 가용성(availability)을 기다리며 블록(block)됩니다.
- 획득 실패(failed acquisition)는 해당 오류를 그대로 전파(propagate)합니다.

#### 6.5 코드 예제

##### 기본 풀 생성

```scala
import zio._
object PoolExample extends ZIOAppDefault {
  def resource: ZIO[Scope, Nothing, UUID] = ZIO.acquireRelease(
    ZIO.random.flatMap(_.nextUUID).flatMap(uuid =>
      ZIO.debug(s"Acquiring the resource: $uuid!").as(uuid))
  )(uuid => ZIO.debug(s"Releasing the resource $uuid!"))

  def run =
    for {
      pool <- ZPool.make(resource, 3)
      item <- pool.get
      _ <- ZIO.debug(s"Item: $item")
    } yield ()
}
```

##### 데이터베이스 커넥션을 사용한 동적 풀

```scala
ZIO.scoped {
  ZPool.make(acquireDbConnection, 10 to 20, 60.seconds).flatMap { pool =>
    ZIO.scoped {
      pool.get.flatMap { conn => useConnection(conn) }
    }
  }
}
```

##### 재시도 패턴(Retry pattern)

획득에 실패할 수 있는 리소스는 `.eventually` 연산을 통해 성공할 때까지 재시도할 수 있습니다.

```scala
ZPool.make(acquireDbConnection, 10).flatMap { pool =>
  pool.get.flatMap(conn => useConnection(conn)).eventually
}
```

#### 6.6 리소스 무효화(Resource Invalidation)

리소스가 결함이 생기거나(faulty) 연결이 끊어지면(disconnected), `invalidate`를 호출하여 풀의 오염(pool contamination)을 방지합니다. 풀은 즉시 교체(immediate replacement)하는 대신 지연 재할당(lazy reallocation)을 수행하여 시스템 효율성을 유지합니다.

---

### 7. ZKeyedPool: 키 기반 리소스 풀링(ZKeyedPool)

`ZKeyedPool[+Err, -Key, Item]`은 `Key` 타입의 키와 연관된 `Item` 타입 항목들의 풀입니다. 예를 들어 호스트별, 또는 샤드(shard)별로 별도의 커넥션 풀을 유지할 때 유용합니다.

#### 7.1 주요 연산

인터페이스는 두 가지 핵심 메서드를 제공합니다.

1. **`get(key: Key)`**: 주어진 키와 연관된 항목을 스코프 이펙트(scoped effect) 내에서 풀로부터 가져옵니다.
2. **`invalidate(item: Item)`**: 항목을 재할당(reallocation) 대상으로 표시합니다.

#### 7.2 생성 메서드(Creation Methods)

##### 고정 크기 풀(Fixed-Size Pools)

**균일한 풀 크기(Uniform pool sizes):** 모든 키에 대해 동일한 풀 크기를 사용하려면 다음을 사용합니다.

```scala
ZKeyedPool.make(get: Key => ZIO[Env, Err, Item], size: => Int)
```

예제에서는 각 키("foo"와 "bar")마다 3개의 리소스를 획득하는 모습을 보여 줍니다.

**가변 풀 크기(Variable pool sizes):** 키마다 다른 용량을 지정하려면 다음을 사용합니다.

```scala
ZKeyedPool.make(get: Key => ZIO[Env, Err, Item], size: Key => Int)
```

문서에서는 "foo"로 시작하는 키는 풀 크기 2, "bar"는 크기 3, 그 외에는 크기 1로 설정하는 예를 보여 줍니다.

##### 동적 크기 풀(Dynamic-Size Pools)

최소/최대 용량과 time-to-live 파라미터를 갖는 풀입니다.

```scala
make(get, range: Key => Range, timeToLive: Duration)
make(get, range: Key => Range, timeToLive: Key => Duration)
```

#### 7.3 동작 특성

ZIO 공식 문서에는 리소스 획득/해제 주기(acquisition/release cycle)를 보여 주는 상세한 추적 로그(trace log)가 포함되어 있습니다. 이를 통해 풀이 키별로 별도의 컬렉션을 유지하고, 스코프 컨텍스트 안에서 생명 주기 연산을 올바르게 관리함을 확인할 수 있습니다.

---

### 8. ScopedRef: 리소스를 위한 가변 참조(ScopedRef)

`ScopedRef`는 스코프 리소스(scoped resource)를 관리하기 위해 특별히 설계된 `Ref`의 리소스 버전입니다. 가변 참조(mutable reference) 기능을 제공하면서 리소스 생명 주기를 스코프와 함께 관리합니다.

#### 8.1 핵심 연산(Core Operations)

API는 두 가지 기본 연산을 포함합니다.

1. **Get**: `ScopedRef#get`은 스코프 참조(scoped ref)의 현재 값을 반환합니다.
2. **Set**: `ScopedRef#set`은 새 리소스를 획득하여 스코프 참조의 새 값을 생성함으로써 값을 설정합니다. 새 값을 설정하면 이전 리소스(old resource)가 자동으로 해제(release)됩니다.

#### 8.2 생성 메서드(Construction Methods)

두 가지 생성자를 사용할 수 있습니다.

```scala
object ScopedRef {
  def make[A](a: => A): ZIO[Scope, Nothing, ScopedRef[A]] = ???
  def fromAcquire[R, E, A](acquire: ZIO[R, E, A]): ZIO[R with Scope, E, ScopedRef[A]] = ???
}
```

- **`make`**: 일반 값(ordinary value)으로부터 스코프 참조를 생성합니다. 리소스 획득이 필요 없는 상수 값(constant value)에 유용합니다.
- **`fromAcquire`**: 리소스 관리를 통해 값을 생성하는 이펙트로부터 스코프 참조를 생성합니다.

#### 8.3 주의 사항

`ScopedRef`는 리소스를 다루는 타입이므로 수명(lifetime)이 스코프에 의해 관리됩니다. 더 이상 필요하지 않을 때는 `ZIO.scoped` 조합자(combinator)로 해제할 수 있습니다.

#### 8.4 인터럽트 동작(Interruption Behavior)

`set` 연산은 인터럽트 불가능한 블록(uninterruptible block) 안에서 실행됩니다. 이는 `forkScoped`를 사용하여 `ScopedRef`로 파이버(Fiber)를 관리하려는 경우, 해당 파이버를 명시적으로 인터럽트 가능하게(interruptible) 만들어야 함을 의미합니다.

---

### 9. 참고 자료

- [Handling Resources (Overview)](https://zio.dev/overview/handling-resources)
- [Resource Management (Reference Index)](https://zio.dev/reference/resource/)
- [Scope](https://zio.dev/reference/resource/scope)
- [ZPool](https://zio.dev/reference/resource/zpool)
- [ZKeyedPool](https://zio.dev/reference/resource/zkeyedpool)
- [ScopedRef](https://zio.dev/reference/resource/scopedref)
