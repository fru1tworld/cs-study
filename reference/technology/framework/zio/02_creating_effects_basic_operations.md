# Effect 생성과 기본 연산

> 원본: https://zio.dev/overview/creating-effects

---

## 목차

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

## 1. Effect 생성 개요(Overview)

이 섹션에서는 값(values), 계산(computations), 그리고 흔히 쓰이는 Scala 데이터 타입으로부터 ZIO 이펙트(effect)를 생성하는 몇 가지 일반적인 방법을 살펴봅니다.

예제 코드는 다음과 같은 임포트(import)를 전제로 합니다.

```scala
import zio.{ ZIO, Task, UIO, URIO, IO }
```

---

## 2. 값으로부터 생성(From Values)

`ZIO.succeed` 메서드를 사용하면, 실행될 때 지정한 값으로 성공(succeed)하는 이펙트를 생성할 수 있습니다.

```scala
val s1 = ZIO.succeed(42)
```

`succeed` 메서드는 **이름에 의한 파라미터(by-name parameter)**를 받습니다. 메서드에 전달된 코드는 ZIO 이펙트 내부에 저장되어 ZIO가 관리하므로, 재시도(retries), 타임아웃(timeouts), 자동 오류 로깅(automatic error logging) 같은 기능을 활용할 수 있습니다.

---

## 3. 실패 값으로부터 생성(From Failure Values)

`ZIO.fail` 메서드를 사용하면, 실행될 때 지정한 값으로 실패(fail)하는 이펙트를 생성할 수 있습니다.

```scala
val f1 = ZIO.fail("Uh oh!")
```

`ZIO` 데이터 타입의 경우, 오류 타입(error type)에 아무런 제약이 없습니다. 애플리케이션에 적합하다면 문자열(string), 예외(exception), 또는 커스텀 데이터 타입(custom data type) 등 무엇이든 사용할 수 있습니다.

많은 애플리케이션은 `Throwable`이나 `Exception`을 확장한 클래스로 실패를 모델링합니다.

```scala
val f2 = ZIO.fail(new Exception("Uh oh!"))
```

---

## 4. Scala 값으로부터 생성(From Scala Values)

Scala 표준 라이브러리에는 ZIO 이펙트로 변환할 수 있는 여러 데이터 타입이 있습니다.

### 4.1 Option

`Option`은 `ZIO.fromOption`을 사용하여 ZIO 이펙트로 변환할 수 있습니다.

```scala
val zoption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(2))
```

결과 이펙트의 오류 타입은 `Option[Nothing]`입니다. 이는 이러한 이펙트가 실패할 경우 `None` 값(타입이 `Option[Nothing]`)으로 실패한다는 것을 의미합니다.

`orElseFail`을 사용하면 실패를 다른 오류 값으로 변환할 수 있습니다. 이는 ZIO가 오류 관리(error management)를 위해 제공하는 여러 메서드 중 하나입니다.

```scala
val zoption2: ZIO[Any, String, Int] = zoption.orElseFail("It wasn't there!")
```

ZIO는 `Option`을 다루는 코드와의 연동을 쉽게 만들어 주는 다양한 연산자(operator)를 제공합니다. 다음의 심화 예제에서는 `some`과 `asSomeError` 연산자를 사용하여 `Option`을 반환하는 메서드들과의 연동을 더 쉽게 만듭니다. 이는 일부 Scala 라이브러리의 `OptionT` 타입과 유사합니다.

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

### 4.2 Either

`Either`는 `ZIO.fromEither`를 사용하여 ZIO 이펙트로 변환할 수 있습니다.

```scala
val zeither: ZIO[Any, Nothing, String] = ZIO.fromEither(Right("Success!"))
```

결과 이펙트의 오류 타입은 `Left` 케이스의 타입이 되고, 성공 타입은 `Right` 케이스의 타입이 됩니다.

### 4.3 Try

`Try` 값은 `ZIO.fromTry`를 사용하여 ZIO 이펙트로 변환할 수 있습니다.

```scala
import scala.util.Try

val ztry = ZIO.fromTry(Try(42 / 0))
```

`Try`는 `Throwable` 타입의 값으로만 실패할 수 있으므로, 결과 이펙트의 오류 타입은 항상 `Throwable`입니다.

### 4.4 Future

Scala의 `Future`는 `ZIO.fromFuture`를 사용하여 ZIO 이펙트로 변환할 수 있습니다.

```scala
import scala.concurrent.Future

lazy val future = Future.successful("Hello!")

val zfuture: ZIO[Any, Throwable, String] =
  ZIO.fromFuture { implicit ec =>
    future.map(_ => "Goodbye!")
  }
```

`fromFuture`에 전달되는 함수에는 `ExecutionContext`가 제공되며, 이를 통해 ZIO는 `Future`가 어디에서 실행될지를 관리할 수 있습니다(물론 이 `ExecutionContext`를 무시해도 됩니다).

`Future` 값은 `Throwable` 타입의 값으로만 실패할 수 있으므로, 결과 이펙트의 오류 타입은 항상 `Throwable`입니다.

---

## 5. 코드로부터 생성(From Code)

ZIO는 임의의 코드(예: 어떤 메서드 호출)를 이펙트로 변환할 수 있습니다. 그 코드가 이른바 **동기(synchronous)**(값을 직접 반환)이든 **비동기(asynchronous)**(콜백(callback)에 값을 전달)이든 상관없습니다.

올바르게 변환하면 해당 코드가 이펙트 내부에 저장되어 ZIO가 관리하므로, 재시도(retries), 타임아웃(timeouts), 자동 오류 로깅(automatic error logging) 같은 기능을 활용할 수 있습니다.

ZIO가 제공하는 변환 함수를 사용하면, Scala나 Java로 작성된 non-ZIO 코드(서드파티 라이브러리 포함)와도 ZIO의 모든 기능을 매끄럽게 연동할 수 있습니다.

### 5.1 동기 코드(Synchronous Code)

동기 코드는 `ZIO.attempt`를 사용하여 ZIO 이펙트로 변환할 수 있습니다.

```scala
import scala.io.StdIn

val readLine: ZIO[Any, Throwable, String] =
  ZIO.attempt(StdIn.readLine())
```

동기 코드는 `Throwable` 타입의 어떤 값으로든 예외를 던질 수 있으므로, 결과 이펙트의 오류 타입은 항상 `Throwable`입니다.

어떤 코드가 (런타임 예외(runtime exception)를 제외하고) 예외를 던지지 않는다는 것을 확실히 안다면, `ZIO.succeed`를 사용하여 그 코드를 ZIO 이펙트로 변환할 수 있습니다.

```scala
def printLine(line: String): UIO[Unit] =
  ZIO.succeed(println(line))
```

때로는 코드가 특정 예외 타입(specific exception type)을 던진다는 것을 알 수 있고, 이를 ZIO 이펙트의 오류 파라미터(error parameter)에 반영하고 싶을 수 있습니다.

이 목적을 위해 `ZIO#refineToOrDie` 메서드를 사용할 수 있습니다.

```scala
import java.io.IOException

val readLine2: ZIO[Any, IOException, String] =
  ZIO.attempt(StdIn.readLine()).refineToOrDie[IOException]
```

### 5.2 비동기 코드(Asynchronous Code)

콜백 기반 API(callback-based API)를 노출하는 비동기 코드는 `ZIO.async`를 사용하여 ZIO 이펙트로 변환할 수 있습니다.

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

비동기 이펙트는 콜백 기반 API보다 훨씬 사용하기 쉬우며, 인터럽션(interruption), 리소스 안전성(resource-safety), 오류 관리(error management) 같은 ZIO 기능을 그대로 활용할 수 있습니다.

---

## 6. 블로킹 동기 코드(Blocking Synchronous Code)

일부 동기 코드는 이른바 **블로킹 IO(blocking IO)**를 수행합니다. 이는 운영체제 호출(operating system call)이 완료되기를 기다리는 동안 스레드를 대기 상태(waiting state)에 빠뜨립니다. 처리량(throughput)을 극대화하려면, 이러한 코드는 애플리케이션의 주 스레드 풀(primary thread pool)이 아니라 블로킹 연산에 전용으로 할당된 특수한 스레드 풀(special thread pool)에서 실행되어야 합니다.

ZIO는 런타임(runtime)에 블로킹 스레드 풀(blocking thread pool)을 내장하고 있으며, `ZIO.blocking`을 사용하여 그곳에서 이펙트를 실행할 수 있습니다.

```scala
import scala.io.{ Codec, Source }

def download(url: String) =
  ZIO.attempt {
    Source.fromURL(url)(Codec.UTF8).mkString
  }

def safeDownload(url: String) =
  ZIO.blocking(download(url))
```

대안으로, 블로킹 코드를 직접 ZIO 이펙트로 변환하고 싶다면 `ZIO.attemptBlocking` 메서드를 사용할 수 있습니다.

```scala
val sleeping =
  ZIO.attemptBlocking(Thread.sleep(Long.MaxValue))
```

결과 이펙트는 ZIO의 블로킹 스레드 풀에서 실행됩니다.

Java의 `Thread.interrupt`에 반응하는 동기 코드(예: `Thread.sleep` 또는 락 기반(lock-based) 코드)가 있다면, `ZIO.attemptBlockingInterrupt` 메서드를 사용하여 이 코드를 인터럽트 가능한(interruptible) ZIO 이펙트로 변환할 수 있습니다.

일부 동기 코드는 실행 중인 계산을 취소하는 책임을 가진 별도의 코드를 호출함으로써만 취소(cancel)될 수 있습니다. 이러한 코드를 ZIO 이펙트로 변환하려면 `ZIO.attemptBlockingCancelable` 메서드를 사용할 수 있습니다.

```scala
import java.net.ServerSocket
import zio.UIO

def accept(l: ServerSocket) =
  ZIO.attemptBlockingCancelable(l.accept())(ZIO.succeed(l.close()))
```

> **다음 단계**: 값으로부터 이펙트를 생성하고, Scala 타입을 이펙트로 변환하며, 동기·비동기 코드를 이펙트로 변환하는 데 익숙해졌다면, 다음 단계는 이펙트에 대한 기본 연산(basic operations)을 배우는 것입니다.

---

## 7. 기본 연산 개요(Basic Operations Overview)

`String` 데이터 타입이나 Scala의 컬렉션 데이터 타입(`List`, `Map`, `Set` 등)과 마찬가지로, ZIO 이펙트는 **불변(immutable)**이며 변경할 수 없습니다.

ZIO 이펙트를 변환(transform)하거나 조합(combine)하려면, ZIO 데이터 타입의 메서드들을 사용하면 됩니다. 이 메서드들은 지정된 변환이나 조합이 적용된 **새로운 이펙트(new effects)**를 반환합니다.

ZIO 데이터 타입의 메서드에는 두 가지 범주(categories)가 있습니다.

- **변환(Transformations)**: 변환 함수는 잘 정의된 방식으로 이펙트를 변경하여, 런타임 동작(runtime behavior)을 커스터마이즈할 수 있게 합니다. 예를 들어, 어떤 이펙트에 `effect.timeout(60.seconds)`를 호출하면, 실행될 때 원래 이펙트에 타임아웃(timeout)을 적용하는 새 이펙트를 반환합니다.
- **조합(Combinations)**: 조합 함수는 둘 이상의 이펙트를 하나의 이펙트로 결합합니다. 예를 들어, `effect1.orElse(effect2)`를 호출하면 두 이펙트가 결합되어, 반환된 이펙트가 실행될 때 먼저 좌측(left hand side)을 실행하고 그것이 실패하면 우측(right hand side)을 실행합니다. 이를 통해 주(primary) 이펙트가 실패할 경우의 폴백(fallback) 이펙트를 지정할 수 있습니다.

이후 예제는 다음 임포트를 전제로 합니다.

```scala
import zio._
import zio.Console._
import java.io.IOException
```

---

## 8. 매핑(Mapping)

어떤 값으로 성공하는 이펙트가 있을 때, `ZIO#map`을 사용하면 제공한 함수로 그 값을 변환하는 새로운 이펙트를 얻을 수 있습니다.

```scala
import zio._

val succeeded: ZIO[Any, Nothing, Int] = ZIO.succeed(21).map(_ * 2)
```

이와 유사하게, `ZIO#mapError` 메서드를 사용하면 하나의 오류를 가진 이펙트를 다른 오류를 가진 이펙트로 변환할 수 있습니다. 이때 변환을 수행할 함수를 제공해야 합니다.

```scala
val failed: ZIO[Any, Exception, Unit] = 
  ZIO.fail("No no!").mapError(msg => new Exception(msg))
```

이펙트의 오류 값이나 성공 값을 매핑하는 것은, 그 이펙트가 실패하는지 성공하는지 **여부 자체(whether or not)**를 바꾸지 않는다는 점에 유의하세요. 이는 Scala의 `Either` 데이터 타입에 대한 매핑이 그 `Either`가 `Left`인지 `Right`인지를 바꾸지 않는 것과 유사합니다.

---

## 9. 연쇄(Chaining)

`flatMap` 메서드를 사용하면 두 이펙트를 순차적으로(sequentially) 실행할 수 있습니다. `flatMap` 메서드에는 콜백을 전달해야 하며, 이 콜백은 첫 번째 이펙트의 성공 값을 받아 그 값에 의존하는 두 번째 이펙트를 반환해야 합니다.

```scala
val sequenced: ZIO[Any, IOException, Unit] =
  Console.readLine.flatMap(input => Console.printLine(s"You entered: $input"))
```

만약 첫 번째 이펙트가 실패하면, `flatMap`에 전달된 콜백은 절대 호출되지 않으며, `flatMap`이 반환한 이펙트 역시 실패합니다.

`flatMap`으로 생성된 **모든** 이펙트 연쇄(chain)에서, 첫 번째 실패는 전체 연쇄를 단락(short-circuit)시킵니다. 이는 예외를 던지면 일련의 문장(statements) 실행을 조기에 종료하는 것과 마찬가지입니다.

---

## 10. for 컴프리헨션(For Comprehensions)

ZIO 데이터 타입은 `flatMap`과 `map`을 모두 지원하므로, Scala의 **for 컴프리헨션(for comprehensions)**을 사용하여 명령형(imperative) 이펙트를 구성할 수 있습니다.

```scala
val program: ZIO[Any, IOException, Unit] =
  for {
    _    <- Console.printLine("Hello! What is your name?")
    name <- Console.readLine
    _    <- Console.printLine(s"Hello, ${name}, welcome to ZIO!")
  } yield ()
```

for 컴프리헨션은 이펙트 연쇄를 생성하기 위한 절차적 문법(procedural syntax)을 제공하며, 대부분의 프로그래머가 ZIO 사용에 가장 빠르게 익숙해지는 방법입니다.

---

## 11. 지핑(Zipping)

`ZIO#zip` 메서드를 사용하면 두 이펙트를 하나의 이펙트로 결합할 수 있습니다.

이 메서드는 좌측(left) 이펙트를 먼저 실행한 후 우측(right) 이펙트를 실행하고, 두 성공 값을 모두 튜플(tuple)에 담는 이펙트를 반환합니다.

```scala
val zipped: ZIO[Any, Nothing, (String, Int)] = 
  ZIO.succeed("4").zip(ZIO.succeed(2))
```

`zip` 연산에서 좌측이나 우측 중 어느 한쪽이라도 실패하면, 튜플을 구성하는 데 **두** 값이 모두 필요하므로 결합된 이펙트도 실패합니다. 좌측이 실패하면 우측은 실행되지 않습니다.

이펙트의 성공 값이 필요하지 않을 때(예: 값이 `Unit`인 경우)는 `ZIO#zipLeft`나 `ZIO#zipRight`를 사용하는 것이 더 편리합니다. 이 함수들은 `zip`을 수행한 뒤 튜플에서 한쪽 값을 버립니다(discard).

```scala
val zipRight1: ZIO[Any, IOException, String] =
  Console.printLine("What is your name?").zipRight(Console.readLine)
```

`zipRight`와 `zipLeft` 함수는 각각 `*>`와 `<*`라는 기호 별칭(symbolic aliases)을 가집니다. 일부 개발자는 이러한 연산자가 읽기 더 쉽다고 느낍니다.

```scala
val zipRight2: ZIO[Any, IOException, String] =
  Console.printLine("What is your name?") *>
    Console.readLine
```

> **다음 단계**: ZIO 이펙트의 기본 연산에 익숙해졌다면, 다음 단계는 오류 처리(error handling)에 대해 배우는 것입니다.

---

## 12. 참고 자료

- [Creating Effects (ZIO 공식 문서)](https://zio.dev/overview/creating-effects)
- [Basic Operations (ZIO 공식 문서)](https://zio.dev/overview/basic-operations)
- [Handling Errors (다음 단계)](https://zio.dev/overview/handling-errors)
- [ZIO Overview](https://zio.dev/overview/)
