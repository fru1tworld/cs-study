# ZIO 핵심 데이터 타입

> 원본: https://zio.dev/reference/core/

---

## 목차

1. [ZIO[R, E, A] 데이터 타입(The ZIO Data Type)](#1-zior-e-a-데이터-타입the-zio-data-type)
2. [타입 별칭(Type Aliases)](#2-타입-별칭type-aliases)
3. [효과 생성(Creating Effects)](#3-효과-생성creating-effects)
4. [변환과 합성(Transformations and Composition)](#4-변환과-합성transformations-and-composition)
5. [오류 관리(Error Management)](#5-오류-관리error-management)
6. [리소스 관리와 캐싱(Resource Management and Caching)](#6-리소스-관리와-캐싱resource-management-and-caching)
7. [Exit 데이터 타입(The Exit Data Type)](#7-exit-데이터-타입the-exit-data-type)
8. [Cause 데이터 타입 개요(The Cause Data Type Overview)](#8-cause-데이터-타입-개요the-cause-data-type-overview)
9. [Runtime 데이터 타입(The Runtime Data Type)](#9-runtime-데이터-타입the-runtime-data-type)
10. [ZIOApp과 ZIOAppDefault(Application Entry Points)](#10-zioapp과-zioappdefaultapplication-entry-points)
11. [참고 자료](#11-참고-자료)

---

## 1. ZIO[R, E, A] 데이터 타입(The ZIO Data Type)

`ZIO[R, E, A]`는 ZIO 라이브러리의 핵심 타입으로, 실패하거나(fail) 성공할(succeed) 수 있는 효과적인 프로그램(effectful program)을 모델링하는 값입니다. 이 타입은 불변(immutable)이며 지연 평가(lazy)되는 워크플로 계산(workflow computation)을 기술합니다. 즉, `ZIO` 값을 생성한다고 해서 즉시 실행되는 것이 아니라, 동시성 프로그램의 실행을 기술하는 데이터 구조(blueprint)를 만드는 것입니다.

### 1.1 세 가지 타입 파라미터(Three Type Parameters)

`ZIO[R, E, A]`의 세 타입 파라미터는 각각 다음을 의미합니다.

- **R (환경, Environment)**: 효과가 실행되기 위해 필요한 환경(environment) 요구 사항입니다. `Any`로 지정하면 아무런 요구 사항이 없음을 의미합니다.
- **E (실패, Failure)**: 효과가 실패할 때의 오류 타입(error type)입니다. `Nothing`으로 지정하면 그 효과는 실패할 수 없음(cannot fail)을 의미합니다.
- **A (성공, Success)**: 효과가 성공했을 때의 결과 값 타입(success value type)입니다. `Unit`은 유용한 출력이 없음을 의미하고, `Nothing`은 무한히 실행됨(infinite execution)을 의미합니다.

### 1.2 함수로서의 직관(Intuition as a Function)

공식 문서는 다음과 같이 설명합니다.

> "`ZIO[R, E, A]` 타입의 값은 다음 함수 타입의 효과적인 버전(effectful version)과 같다: `R => Either[E, A]`"

즉, `ZIO` 값은 개념적으로 "환경 `R`을 입력으로 받아 실패 `E` 또는 성공 `A`(`Either[E, A]`)를 반환하는 함수"로 이해할 수 있습니다. 다만 이 함수는 부수 효과(side effect), 비동기성(asynchronicity), 동시성(concurrency), 리소스 안전성(resource safety) 등을 추가로 포착한다는 점이 다릅니다.

### 1.3 변성(Variance)

`ZIO`의 타입 시그니처는 다음과 같은 변성(variance) 주석을 가집니다.

```scala
trait ZIO[-R, +E, +A] {
  def tap[R1 <: R, E1 >: E](f: A => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapSome[R1 <: R, E1 >: E](f: PartialFunction[A, ZIO[R1, E1, Any]]): ZIO[R1, E1, A]
}
```

환경 `R`은 반공변(contravariant, `-R`)이고, 오류 `E`와 성공 `A`는 공변(covariant, `+E`, `+A`)입니다.

---

## 2. 타입 별칭(Type Aliases)

`ZIO`는 자주 쓰이는 타입 파라미터 조합을 위해 편리한 타입 별칭(type alias)들을 제공합니다.

```scala
type UIO[A]     = ZIO[Any, Nothing, A]      // 환경 없음, 실패 불가
type URIO[R, A] = ZIO[R, Nothing, A]        // 환경 있음, 실패 불가
type Task[A]    = ZIO[Any, Throwable, A]    // 환경 없음, Throwable 오류
type RIO[R, A]  = ZIO[R, Throwable, A]      // 환경 있음, Throwable 오류
type IO[E, A]   = ZIO[Any, E, A]            // 환경 없음, 커스텀 오류
```

각 별칭의 의미는 다음과 같습니다.

- **UIO[A]** — `ZIO[Any, Nothing, A]`. 특정 환경을 요구하지 않고, 실패할 수 없으며(cannot fail), 타입 `A`의 값으로 성공하는 효과입니다. 문서는 이를 **예외 없는 효과(unexceptional effect)**라고 부릅니다. Scala에서 `Nothing`은 거주 불가능한 타입(uninhabitable type)이므로, `UIO[A]` 값은 "오류를 낼 수 없는(infallible)" 효과로 간주됩니다. 즉, `A`를 산출할 수는 있어도 절대 실패하지 않습니다.
- **URIO[R, A]** — `ZIO[R, Nothing, A]`. 환경 `R`을 요구하지만 실패할 수 없는 효과입니다.
- **Task[A]** — `ZIO[Any, Throwable, A]`. 환경을 요구하지 않으며, 오류 타입이 `Throwable`로 고정된 효과입니다.
- **RIO[R, A]** — `ZIO[R, Throwable, A]`. 환경 `R`을 요구하고, 오류 타입이 `Throwable`로 고정된 효과입니다.
- **IO[E, A]** — `ZIO[Any, E, A]`. 환경을 요구하지 않으며, 커스텀 오류 타입 `E`를 가지는 효과입니다.

`Task`와 `RIO`는 오류 파라미터가 `Throwable`로 고정된 **예외적 효과(exceptional effect)**이고, `UIO`와 `URIO`는 오류 파라미터가 `Nothing`으로 고정되어 실패할 수 없음을 나타내는 **예외 없는 효과(unexceptional effect)**입니다.

### 2.1 UIO 예제(Fibonacci)

다음은 `UIO`를 사용하는 피보나치(Fibonacci) 구현 예제입니다. 절대 실패하지 않으므로 오류 타입이 `Nothing`인 `UIO`로 표현됩니다.

```scala
import zio.{UIO, ZIO}

def fib(n: Int): UIO[Int] =
  if (n <= 1) {
    ZIO.succeed(1)
  } else {
    for {
      fiber1 <- fib(n - 2).fork
      fiber2 <- fib(n - 1).fork
      v2     <- fiber2.join
      v1     <- fiber1.join
    } yield v1 + v2
  }
```

### 2.2 설계 철학: 최소 권한의 원칙(Principle of Least Power)

문서는 **최소 권한의 원칙(Principle of Least Power)**을 강조합니다. 더 약한(덜 강력한) 효과 타입으로 충분하다면 범용 `ZIO` 타입 대신 가장 적합한 특화된 타입 별칭을 사용하도록 권장합니다. 예를 들어 실패하지 않는 효과라면 `Task`나 `ZIO`보다 `UIO`를 사용하는 것이 타입으로 더 많은 정보를 표현합니다.

---

## 3. 효과 생성(Creating Effects)

### 3.1 성공과 실패(Success and Failure)

```scala
val s1 = ZIO.succeed(42)
val f1 = ZIO.fail("Uh oh!")
val f2 = ZIO.fail(new Exception("Uh oh!"))
```

### 3.2 Option으로부터 생성(From Option)

```scala
val zoption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(2))
val zoption2: IO[String, Int] = zoption.mapError(_ => "It wasn't there!")

val someInt: ZIO[Any, Nothing, Option[Int]] = ZIO.some(3)
val noneInt: ZIO[Any, Nothing, Option[Nothing]] = ZIO.none

val r1: ZIO[Any, Throwable, Int] = ZIO.getOrFail(parseInt("1.2"))
val r2: ZIO[Any, Unit, Int] = ZIO.getOrFailUnit(parseInt("1.2"))
val r3: ZIO[Any, NumberFormatException, Int] = 
  ZIO.getOrFailWith(new NumberFormatException("invalid input"))(parseInt("1.2"))

val optionalValue: Option[String] = ???
val r1: ZIO[Any, String, Unit] = ZIO.noneOrFail(optionalValue)
val r2: ZIO[Any, NumberFormatException, Unit] = 
  ZIO.noneOrFailWith(optionalValue)(e => new NumberFormatException(e))
```

### 3.3 Either, Try, Future로부터 생성(From Either, Try, Future)

```scala
val zeither = ZIO.fromEither(Right("Success!"))
val left: UIO[Either[A, Nothing]] = ZIO.left(value)
val right: UIO[Either[Nothing, A]] = ZIO.right(value)

import scala.util.Try
val ztry = ZIO.fromTry(Try(42 / 0))

import scala.concurrent.Future
lazy val future = Future.successful("Hello!")
val zfuture: Task[String] = ZIO.fromFuture { implicit ec =>
  future.map(_ => "Goodbye!")
}

import scala.concurrent.Promise
val func: String => String = s => s.toUpperCase
for {
  promise <- ZIO.succeed(scala.concurrent.Promise[String]())
  _ <- ZIO.attempt {
    Try(func("hello world from future")) match {
      case Success(value) => promise.success(value)
      case Failure(exception) => promise.failure(exception)
    }
  }.fork
  value <- ZIO.fromPromiseScala(promise)
  _ <- Console.printLine(s"Hello World in UpperCase: $value")
} yield ()
```

### 3.4 Fiber로부터 생성(From Fiber)

```scala
val io: IO[Nothing, String] = ZIO.fromFiber(Fiber.succeed("Hello from Fiber!"))
```

### 3.5 동기 부수 효과(Synchronous Side-Effects)

`ZIO.attempt`는 예외를 던질 수 있는 동기 코드를 안전하게 효과로 감쌉니다(오류 타입은 `Throwable`). 절대 실패하지 않는 부수 효과는 `ZIO.succeed`로 감쌉니다. `refineToOrDie`로 오류 타입을 더 구체적인 타입으로 좁힐 수 있습니다.

```scala
import scala.io.StdIn
val getLine: Task[String] = ZIO.attempt(StdIn.readLine())

def printLine(line: String): UIO[Unit] = ZIO.succeed(println(line))
val succeedTask: UIO[Long] = ZIO.succeed(java.lang.System.nanoTime())

import java.io.IOException
val printLine2: IO[IOException, String] =
  ZIO.attempt(scala.io.StdIn.readLine()).refineToOrDie[IOException]
```

### 3.6 블로킹 연산(Blocking Operations)

블로킹(blocking) 작업은 전용 블로킹 스레드 풀(dedicated blocking thread pool)에서 실행해야 합니다. 이를 위해 `ZIO.attemptBlocking`, `ZIO.blocking`, `ZIO.attemptBlockingCancelable`을 사용합니다.

```scala
def blockingTask(n: Int) = ZIO.attemptBlocking {
  do {
    println(s"Running blocking task number $n on dedicated blocking thread pool")
    Thread.sleep(3000)
  } while (true)
}

import scala.io.{ Codec, Source }
def download(url: String) =
  ZIO.attempt {
    Source.fromURL(url)(Codec.UTF8).mkString
  }
def safeDownload(url: String) =
  ZIO.blocking(download(url))

import java.net.ServerSocket
def accept(l: ServerSocket) =
  ZIO.attemptBlockingCancelable(l.accept())(ZIO.succeed(l.close()))
```

### 3.7 비동기 부수 효과(Asynchronous Side-Effects)

콜백 기반(callback-based)의 레거시 비동기 API는 `ZIO.async`를 사용해 효과로 변환할 수 있습니다.

```scala
object legacy {
  def login(
    onSuccess: User => Unit,
    onFailure: AuthError => Unit): Unit = ???
}

val login: IO[AuthError, User] =
  ZIO.async[Any, AuthError, User] { callback =>
    legacy.login(
      user => callback(ZIO.succeed(user)),
      err  => callback(ZIO.fail(err))
    )
  }
```

### 3.8 지연된 효과(Suspended Effects)

`ZIO.suspend`는 효과 생성 자체를 지연시킵니다.

```scala
import java.io.IOException
val suspendedEffect: ZIO[Any, Throwable, Unit] =
  ZIO.suspend(ZIO.attempt(Console.printLine("Suspended Hello World!")))
```

---

## 4. 변환과 합성(Transformations and Composition)

### 4.1 매핑(Mapping)

```scala
val mappedValue: UIO[Int] = ZIO.succeed(21).map(_ * 2)
```

### 4.2 탭핑(Tapping)

`tap`은 효과의 성공 값으로 부수 효과를 수행하되 원래 값은 그대로 통과시킵니다. `tapSome`은 부분 함수(partial function)로 일부 경우에만 동작합니다.

```scala
trait ZIO[-R, +E, +A] {
  def tap[R1 <: R, E1 >: E](f: A => ZIO[R1, E1, Any]): ZIO[R1, E1, A]
  def tapSome[R1 <: R, E1 >: E](f: PartialFunction[A, ZIO[R1, E1, Any]]): ZIO[R1, E1, A]
}

import java.io.IOException
object MainApp extends ZIOAppDefault {
  def isPrime(n: Int): Boolean =
    if (n <= 1) false else (2 until n).forall(i => n % i != 0)
  
  val myApp: ZIO[Any, IOException, Unit] =
    for {
      ref <- Ref.make(List.empty[Int])
      prime <-
        Random
          .nextIntBetween(0, Int.MaxValue)
          .tap(random => ref.update(_ :+ random))
          .repeatUntil(isPrime)
      _ <- Console.printLine(s"found a prime number: $prime")
      tested <- ref.get
      _ <- Console.printLine(
        s"list of tested numbers: ${tested.mkString(", ")}"
      )
    } yield ()
  
  def run = myApp
}
```

### 4.3 연쇄(Chaining)

`flatMap`으로 효과를 순차적으로 연결하며, for-comprehension으로 간결하게 표현할 수 있습니다.

```scala
val chainedActionsValue: UIO[List[Int]] = ZIO.succeed(List(1, 2, 3)).flatMap { list =>
  ZIO.succeed(list.map(_ + 1))
}

val program =
  for {
    _    <- Console.printLine("Hello! What is your name?")
    name <- Console.readLine
    _    <- Console.printLine(s"Hello, ${name}, welcome to ZIO!")
  } yield ()
```

### 4.4 지핑(Zipping)

`zip`은 두 효과의 결과를 튜플로 결합합니다. `zipRight`(또는 `*>`)는 왼쪽 효과의 결과를 버리고 오른쪽 결과만 취합니다.

```scala
val zipped: UIO[(String, Int)] =
  ZIO.succeed("4").zip(ZIO.succeed(2))

val zipRight1 =
  Console.printLine("What is your name?").zipRight(Console.readLine)

val zipRight2 =
  Console.printLine("What is your name?") *>
  Console.readLine
```

### 4.5 병렬 처리와 경쟁(Parallelism and Racing)

`race`는 두 효과를 동시에 실행하여 먼저 완료되는 쪽의 결과를 취합니다.

```scala
for {
  winner <- ZIO.succeed("Hello").race(ZIO.succeed("Goodbye"))
} yield winner
```

### 4.6 타임아웃(Timeout)

```scala
ZIO.succeed("Hello").timeout(10.seconds)
```

### 4.7 ZIO 애스펙트(ZIO Aspect)

`@@` 연산자로 횡단 관심사(cross-cutting concern)를 효과에 적용할 수 있습니다(디버깅, 재시도, 로깅 등).

```scala
val myApp: ZIO[Any, Throwable, String] =
  ZIO.attempt("Hello!") @@ ZIOAspect.debug

def download(url: String): ZIO[Any, Throwable, Chunk[Byte]] = ZIO.succeed(???)
ZIO.foreachPar(List("zio.dev", "google.com")) { url =>
  download(url) @@
    ZIOAspect.retry(Schedule.fibonacci(1.seconds)) @@
    ZIOAspect.loggedWith[Chunk[Byte]](file => s"Downloaded $url file with size of ${file.length} bytes")
}
```

---

## 5. 오류 관리(Error Management)

### 5.1 Either로의 변환(Either)

`either`는 오류를 성공 채널의 `Either`로 노출하고, `absolve`는 그 역방향 변환을 수행합니다.

```scala
val zeither: UIO[Either[String, Int]] =
  ZIO.fail("Uh oh!").either

def sqrt(io: UIO[Double]): IO[String, Double] =
  ZIO.absolve(
    io.map(value =>
      if (value < 0.0) Left("Value must be >= 0.0")
      else Right(Math.sqrt(value))
    )
  )
```

### 5.2 오류 잡기(Catching)

`catchAll`은 모든 오류를, `catchSome`은 일부 오류만 잡아 복구합니다.

```scala
val z: IO[IOException, Array[Byte]] =
  readFile("primary.json").catchAll(_ =>
    readFile("backup.json"))

val data: IO[IOException, Array[Byte]] =
  readFile("primary.data").catchSome {
    case _ : FileNotFoundException =>
      readFile("backup.data")
  }
```

### 5.3 폴백(Fallback)

`orElse`는 첫 효과가 실패하면 대안 효과를 시도합니다.

```scala
val primaryOrBackupData: IO[IOException, Array[Byte]] =
  readFile("primary.data").orElse(readFile("backup.data"))
```

### 5.4 폴딩(Folding)

`fold`/`foldZIO`는 실패와 성공 두 경우를 모두 처리합니다(`foldZIO`는 각 경우에 또 다른 효과를 반환).

```scala
lazy val DefaultData: Array[Byte] = Array(0, 0)
val primaryOrDefaultData: UIO[Array[Byte]] =
  readFile("primary.data").fold(
    _    => DefaultData,
    data => data)

val primaryOrSecondaryData: IO[IOException, Array[Byte]] =
  readFile("primary.data").foldZIO(
    _    => readFile("secondary.data"),
    data => ZIO.succeed(data))

val urls: UIO[Content] =
  readUrls("urls.json").foldZIO(
    error   => ZIO.succeed(NoContent(error)),
    success => fetchContent(success)
  )
```

### 5.5 재시도(Retrying)

`retry`는 스케줄(Schedule)에 따라 실패한 효과를 재시도하고, `retryOrElse`는 재시도 소진 시 폴백을 제공합니다.

```scala
val retriedOpenFile: ZIO[Any, IOException, Array[Byte]] =
  readFile("primary.data").retry(Schedule.recurs(5))

readFile("primary.data").retryOrElse(
  Schedule.recurs(5),
  (_, _:Long) => ZIO.succeed(DefaultData))
```

---

## 6. 리소스 관리와 캐싱(Resource Management and Caching)

### 6.1 종료자(Finalizing)

`ensuring`은 효과의 성공/실패와 무관하게 항상 실행되는 종료 로직(finalizer)을 부착합니다.

```scala
val finalizer =
  ZIO.succeed(println("Finalizing!"))
val finalized: IO[String, Unit] =
  ZIO.fail("Failed!").ensuring(finalizer)

var i: Int = 0
val action: Task[String] =
  ZIO.succeed(i += 1) *>
    ZIO.fail(new Throwable("Boom!"))
val cleanupAction: UIO[Unit] = ZIO.succeed(i -= 1)
val composite = action.ensuring(cleanupAction)
```

### 6.2 획득과 해제(Acquire Release)

`acquireReleaseWith`는 리소스를 안전하게 획득(acquire)하고, 사용(use) 후 반드시 해제(release)하도록 보장합니다.

```scala
val groupedFileData: IO[IOException, Unit] = ZIO.acquireReleaseWith(openFile("data.json"))(closeFile(_)) { file =>
  for {
    data    <- decodeData(file)
    grouped <- groupData(data)
  } yield grouped
}

import java.io.{ File, FileInputStream }
import java.nio.charset.StandardCharsets
object Main extends ZIOAppDefault {
  def run = myAcquireRelease
  
  def closeStream(is: FileInputStream) =
    ZIO.succeed(is.close())
  
  def convertBytes(is: FileInputStream, len: Long) =
    ZIO.attempt {
      val buffer = new Array[Byte](len.toInt)
      is.read(buffer)
      println(new String(buffer, StandardCharsets.UTF_8))
    }
  
  val myAcquireRelease: Task[Unit] = for {
    file   <- ZIO.attempt(new File("/tmp/hello"))
    len    = file.length
    string <- ZIO.acquireReleaseWith(ZIO.attempt(new FileInputStream(file)))(closeStream)(convertBytes(_, len))
  } yield string
}
```

### 6.3 효과 메모이제이션(Memoizing Effects)

`memoize`는 효과를 처음 한 번만 실행하고 그 결과를 캐싱하는 새 효과를 반환합니다.

```scala
val expensiveComputation: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
val memoized: ZIO[Any, Nothing, ZIO[Any, Nothing, Int]] =
  expensiveComputation.memoize

def computeValue: ZIO[Any, Nothing, Int] = {
  ZIO.succeed {
    println("Computing...")
    42
  }
}
object Example extends ZIOAppDefault {
  def run =
    for {
      memoized <- computeValue.memoize
      _        <- memoized  // prints "Computing..."
      _        <- memoized  // returns cached result, no print
      _        <- memoized  // returns cached result, no print
    } yield ()
}
```

### 6.4 함수 메모이제이션(Memoizing Functions)

`ZIO.memoize`는 효과를 반환하는 함수를 입력별로 캐싱합니다.

```scala
val expensiveLookup: String => ZIO[Any, Nothing, Int] = key => ZIO.succeed(key.length)
for {
  memoized <- ZIO.memoize(expensiveLookup)
  result1  <- memoized("hello")   // computes and caches
  result2  <- memoized("hello")   // returns cached result
  result3  <- memoized("world")   // different input, computes anew
} yield (result1, result2, result3)
```

### 6.5 시간 제한 캐싱(Time-Limited Caching)

`cached`는 지정한 TTL(time-to-live) 동안 결과를 캐싱하고, `cachedInvalidate`는 수동 무효화(invalidate) 핸들을 함께 제공합니다.

```scala
val expensiveData: ZIO[Any, Nothing, String] = ZIO.succeed("data")
for {
  cachedIO <- expensiveData.cached(5.minutes)
  result1  <- cachedIO  // runs computation and caches result
  result2  <- cachedIO  // returns cached result (within 5 minutes)
} yield (result1, result2)

def fetchUserData: ZIO[Any, Nothing, String] = ZIO.succeed("user-data")
for {
  cached <- fetchUserData.cached(5.minutes)
  _      <- cached                      // runs and caches
  _      <- ZIO.sleep(6.minutes)
  result <- cached                      // TTL expired, recomputes
} yield result

def freshData: ZIO[Any, Nothing, String] = ZIO.succeed("data")
for {
  pair              <- freshData.cachedInvalidate(1.hour)
  (cached, invalidate) = pair
  result1 <- cached      // runs and caches
  result2 <- cached      // returns cached result
  _       <- invalidate  // manually clear cache before TTL expires
  result3 <- cached      // recomputes since cache was invalidated
} yield (result1, result2, result3)

def expensiveComputation: ZIO[Any, Nothing, Int] = ZIO.succeed {
  println("Computing...")
  42
}
for {
  cached <- expensiveComputation.cached(5.minutes)
  fiber1 <- cached.fork
  fiber2 <- cached.fork
  fiber3 <- cached.fork
  result1 <- fiber1.join  // one executes the computation
  result2 <- fiber2.join  // others wait for the same result
  result3 <- fiber3.join  // all get 42, but computed only once
} yield (result1, result2, result3)
```

---

## 7. Exit 데이터 타입(The Exit Data Type)

### 7.1 개요(Overview)

`Exit[E, A]`는 파이버(fiber)가 어떻게 종료(conclude)되었는지를 기술합니다. `IO` 값을 실행한 결과(result)를 나타내며, 두 가지 가능한 결과를 표현합니다.

- `Exit.Success` — 타입 `A`의 성공 값(success value)을 담습니다.
- `Exit.Failure` — 타입 `E`의 실패 원인(`Cause`)을 담습니다.

### 7.2 데이터 타입 정의(Data Type Definition)

```scala
sealed abstract class Exit[+E, +A] extends Product with Serializable { self =>
  // Exit operators
}

object Exit {
  final case class Success[+A](value: A)
        extends Exit[Nothing, A]
  final case class Failure[+E](cause: Cause[E]) extends Exit[E, Nothing]
}
```

`Failure`가 단순한 `E`가 아니라 `Cause[E]`를 담는다는 점이 중요합니다. 이는 기대된 오류, 결함(defect), 인터럽트 등 실패의 전체 정보를 보존하기 위한 설계입니다(8장 참고).

### 7.3 사용 예제(Usage Example)

효과에 `ZIO#exit`를 호출하면 파이버의 성공 또는 실패 여부를 검사할 수 있습니다.

```scala
import zio._
import zio.Console._
import java.io.IOException

val result: ZIO[Any, IOException, Unit] =
  for {
    successExit <- ZIO.succeed(1).exit
    _ <- successExit match {
      case Exit.Success(value) =>
        printLine(s"exited with success value: ${value}")
      case Exit.Failure(cause) =>
        printLine(s"exited with failure state: $cause")
    }
  } yield ()
```

### 7.4 미리 만들어진 Exit 값(Pre-constructed Exit Values)

ZIO는 미리 만들어진(ready-made) `Exit` 값을 제공합니다. 직접 생성하지 않고 사전 정의된 exit를 반환할 때 유용합니다.

- `Exit.unit` — `Unit`을 담는 성공 exit
- `Exit.none` — `None`을 담는 성공 exit (타입: `Exit[Nothing, Option[Nothing]]`)

```scala
import zio._

val unitExit: Exit[String, Unit] = Exit.unit
val noneExit: Exit[String, Option[Nothing]] = Exit.none
```

---

## 8. Cause 데이터 타입 개요(The Cause Data Type Overview)

> 참고: Cause에 대한 심층적인 오류 관리 논의는 오류 관리(03) 문서에서 다루므로, 여기서는 개요만 정리합니다.

### 8.1 Cause란 무엇인가(What is Cause?)

`Cause` 데이터 타입은 ZIO의 내부 오류 표현 메커니즘(underlying error representation mechanism)입니다. 공식 문서는 다음과 같이 설명합니다.

> "ZIO는 실패의 전체 이야기(full story of failure)를 저장하기 위해 `Cause[E]`를 사용하므로, 그 오류 모델은 **무손실(lossless)**이다."

즉, ZIO는 예기치 않은 오류(unexpected errors), 스택 트레이스(stack traces), 실행 추적(execution traces), 파이버 인터럽트 원인(fiber interruption causes) 등 실패와 관련된 모든 정보를 보존합니다. `ZIO[R, E, A]`의 오류 타입 `E`는 다형적(polymorphic)이지만, `Cause` 데이터 구조는 임의의 오류 타입만으로는 담을 수 없는 세부 정보까지 포착합니다.

### 8.2 설계: 세미링 구조(Semiring Structure)

ZIO는 `Cause`를 함수형 프로그래밍의 세미링(semiring) 데이터 구조로 구현합니다. 이 설계는 "오류 타입을 나타내는 기본 타입 `E`를 취한 다음, 오류들의 순차적(sequential) 합성과 병렬(parallel) 합성을 완전히 무손실 방식으로 포착할 수 있게" 합니다.

```scala
sealed abstract class Cause[+E] extends Product with Serializable { self =>
  import Cause._
  def trace: Trace = ???
  final def ++[E1 >: E](that: Cause[E1]): Cause[E1] = Then(self, that)
  final def &&[E1 >: E](that: Cause[E1]): Cause[E1] = Both(self, that)
}
```

### 8.3 Cause의 종류(Cause Variations)

**Empty** — 오류가 없음을 나타냅니다. `Cause.empty`로 생성하며, 실패의 부재(absence of failure)를 표현합니다.

**Fail** — 타입 `E`의 기대된 오류(expected errors)를 나타냅니다. `Cause.fail(value)`로 생성합니다.

```scala
ZIO.failCause(Cause.fail("Oh uh!")).cause.debug
// Fail(Oh uh!,Trace(...))
```

**Die** — 예기치 않은 결함(unexpected defects, `Throwable`)을 나타냅니다. `Cause.die(throwable)`로 생성합니다.

```scala
ZIO.succeed(5 / 0).cause.debug
// Die(java.lang.ArithmeticException: / by zero,...)
```

**Interrupt** — 파이버 인터럽트(fiber interruption)를 나타내며, 파이버 ID와 스택 트레이스 정보를 담습니다.

```scala
ZIO.interrupt.cause.debug
// Interrupt(Runtime(2,1646471715),Trace(...))
```

**Stackless** — 출력에서 스택 트레이스의 상세도(verbosity)를 제어하는 불리언 플래그로 원인을 감쌉니다. `ZIO.dieMessage`가 트레이스 출력을 제한하기 위해 사용합니다.

```scala
ZIO.dieMessage("Boom!").cause.debug
// Stackless(Die(java.lang.RuntimeException: Boom!,...),true)
```

**Both** — 여러 동시 파이버가 동시에 실패할 때 오류들의 병렬 합성(parallel composition)을 인코딩합니다.

```scala
(ZIO.fail("Oh uh!") <&> ZIO.dieMessage("Boom!")).cause.debug
// Both(Fail(...), Stackless(Die(...),true))
```

**Then** — 오류들의 순차적 합성(sequential composition)을 인코딩합니다. 특히 try-finally 패턴에서 두 블록이 모두 실패하는 경우에 사용됩니다.

```scala
ZIO.fail("first").ensuring(ZIO.die(new Exception("second"))).cause.debug
// Then(Fail(first,...), Die(java.lang.Exception: second,...))
```

---

## 9. Runtime 데이터 타입(The Runtime Data Type)

### 9.1 Runtime이란 무엇인가(What is a Runtime?)

`Runtime[R]`은 환경 `R` 안에서 작업(task)을 실행할 수 있는 객체입니다. 공식 문서에 따르면 "Runtime은 효과를 실행할 수 있으며(capable of executing effects)", "런타임은 스레드 풀(thread pool)을 효과가 필요로 하는 환경(environment)과 함께 묶는다(bundle)"고 설명합니다.

### 9.2 런타임 시스템 설명(Runtime System Explained)

ZIO 런타임 시스템(Runtime System)은 효과 청사진(effect blueprint)의 실행자(executor)로 동작합니다. ZIO 코드를 작성할 때 우리는 동시성 프로그램의 실행을 기술하는 데이터 구조를 만드는 것이지 직접 실행하는 것이 아닙니다. 런타임은 이 청사진의 명령어(instructions)를 한 단계씩 해석(interpret)하여 `Either[E, A]` 값으로 결과를 산출합니다.

### 9.3 핵심 책임(Core Responsibilities)

런타임 시스템은 일곱 가지 핵심 책임을 다룹니다.

1. **청사진 단계 실행(Execute blueprint steps)** — 완료될 때까지 모든 명령어를 반복적으로 처리합니다.
2. **오류 처리(Handle errors)** — 기대된 실패와 예기치 않은 실패를 모두 관리합니다.
3. **동시 파이버 스폰(Spawn concurrent fibers)** — `fork`가 호출되면 새 파이버를 생성합니다.
4. **협력적 양보(Yield cooperatively)** — 파이버들 사이에 CPU 리소스를 공정하게 분배합니다.
5. **트레이스 포착(Capture traces)** — 상세한 진단을 위해 실행 진행 상황을 추적합니다.
6. **종료자 실행 보장(Ensure finalizers run)** — 적절한 생명 주기 시점에 정리 로직을 실행합니다.
7. **비동기 콜백 처리(Handle async callbacks)** — 비동기 연산을 투명하게 관리합니다.

### 9.4 ZIO 효과 실행하기(Running ZIO Effects)

#### `unsafe.run` 사용

고급 사용 사례나 레거시 코드와의 통합을 위해 `Runtime.default.unsafe.run()`을 사용합니다.

```scala
import zio._

object RunZIOEffectUsingUnsafeRun extends scala.App {
  val myAppLogic = for {
    _ <- Console.printLine("Hello! What is your name?")
    n <- Console.readLine
    _ <- Console.printLine("Hello, " + n + ", good to meet you!")
  } yield ()

  Unsafe.unsafe { implicit unsafe =>
    zio.Runtime.default.unsafe.run(
      myAppLogic
    ).getOrThrowFiberFailure()
  }
}
```

#### 기본 런타임(Default Runtime)

ZIO는 일반적인 애플리케이션을 위해 `Runtime.default`를 제공합니다.

```scala
object Runtime {
  val default: Runtime[Any] =
    Runtime(ZEnvironment.empty, FiberRefs.empty, RuntimeFlags.default)
}
```

직접 접근하는 방법:

```scala
object MainApp extends scala.App {
  val myAppLogic = ZIO.succeed(???)
  val runtime = Runtime.default
  
  Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.run(myAppLogic).getOrThrowFiberFailure()
  }
}
```

### 9.5 런타임 설정 유형(Runtime Configuration Types)

#### 지역 범위 설정(Locally Scoped Configuration)

특정 코드 영역에만 적용되고 그 이후에는 원래대로 되돌아가는 설정 변경입니다. `ZIO#provideXYZ` 연산자를 사용합니다.

설정 레이어(configuration layers)를 통한 방법:

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val addSimpleLogger: ZLayer[Any, Nothing, Unit] =
    Runtime.addLogger((_, _, _, message: () => Any, _, _, _, _) => println(message()))

  def run = {
    for {
      _ <- ZIO.log("Application started!")
      _ <- ZIO.log("Application is about to exit!")
    } yield ()
  }.provide(Runtime.removeDefaultLoggers ++ addSimpleLogger)
}
```

특정 영역에만 범위를 한정한 경우:

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val addSimpleLogger: ZLayer[Any, Nothing, Unit] =
    Runtime.addLogger((_, _, _, message: () => Any, _, _, _, _) => println(message()))

  def run =
    for {
      _ <- ZIO.log("Application started!")
      _ <- {
        for {
          _ <- ZIO.log("I'm not going to be logged!")
          _ <- ZIO.log("I will be logged by the simple logger.").provide(addSimpleLogger)
          _ <- ZIO.log("Reset back to the previous configuration, so I won't be logged.")
        } yield ()
      }.provide(Runtime.removeDefaultLoggers)
      _ <- ZIO.log("Application is about to exit!")
    } yield ()
}
```

#### 부트스트랩 레이어 설정(Bootstrap Layer Configuration)

`ZIOApp`의 `bootstrap` 레이어를 오버라이드하면 애플리케이션 전역에 설정을 적용할 수 있습니다.

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val addSimpleLogger: ZLayer[Any, Nothing, Unit] =
    Runtime.addLogger((_, _, _, message: () => Any, _, _, _, _) => println(message()))

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++ addSimpleLogger

  def run =
    for {
      _ <- ZIO.log("Application started!")
      _ <- ZIO.log("Application is about to exit!")
    } yield ()
}
```

효과적인(effectful) 설정의 경우:

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val addSimpleLogger: ZLayer[Any, Nothing, Unit] =
    Runtime.addLogger((_, _, _, message: () => Any, _, _, _, _) => println(message()))

  val effectfulConfiguration: ZLayer[Any, Nothing, Unit] =
    ZLayer.fromZIO(ZIO.log("Started effectful workflow to customize runtime configuration"))

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++ addSimpleLogger ++ effectfulConfiguration

  def run =
    for {
      _ <- ZIO.log("Application started!")
      _ <- ZIO.log("Application is about to exit!")
    } yield ()
}
```

#### 가상 스레드 지원(Virtual Threads Support)

JDK 21 이상에서는 런타임을 가상 스레드(virtual threads)를 사용하도록 설정할 수 있습니다.

메인 실행기(main executor):

```scala
import zio._

object MainApp extends ZIOAppDefault {
  override val bootstrap =
    Runtime.enableLoomBasedExecutor

  override def run = ZIO.attempt {
    println(s"Task running on a virtual-thread: ${Thread.currentThread().getName()}")
  }
}
```

블로킹 실행기(blocking executor):

```scala
import zio._

object MainApp extends ZIOAppDefault {
  override val bootstrap =
    Runtime.enableLoomBasedBlockingExecutor

  override def run = ZIO.attemptBlocking {
    println(s"Blocking task running on a virtual-thread: ${Thread.currentThread().getName()}")
  }
}
```

#### 최상위 런타임 설정(Top-Level Runtime Configuration)

초기화 시점부터 애플리케이션 전체의 런타임을 커스터마이징하려면 `Runtime.unsafe.fromLayer`를 사용합니다.

```scala
val runtime: Runtime[Any] =
  Unsafe.unsafe { implicit unsafe =>
    Runtime.unsafe.fromLayer(layer)
  }
```

전체 예제:

```scala
import zio._

object MainApp extends ZIOAppDefault {
  val addSimpleLogger: ZLayer[Any, Nothing, Unit] =
    Runtime.addLogger((_, _, _, message: () => Any, _, _, _, _) => println(message()))

  val layer: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++ addSimpleLogger

  override val runtime: Runtime[Any] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }

  def run = ZIO.log("Application started!")
}
```

레거시 애플리케이션과의 통합:

```scala
import zio._

object MainApp {
  val sl4jlogger: ZLogger[String, Any] = ???
  def legacyApplication(input: Int): Unit = ???
  val zioWorkflow: ZIO[Any, Nothing, Int] = ???

  val runtime: Runtime[Unit] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe
        .fromLayer(
          Runtime.removeDefaultLoggers ++ Runtime.addLogger(sl4jlogger)
        )
    }

  def zioApplication(): Int =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe
        .run(zioWorkflow)
        .getOrThrowFiberFailure()
    }

  def main(args: Array[String]): Unit = {
    val result = zioApplication()
    legacyApplication(result)
  }
}
```

### 9.6 런타임에 환경 제공하기(Providing Environment to Runtime)

미리 구성된 서비스(pre-configured services)가 포함된 커스텀 런타임을 만들면 매번 환경을 제공하는 수고를 덜 수 있습니다.

```scala
trait LoggingService {
  def log(line: String): UIO[Unit]
}

object LoggingService {
  def log(line: String): URIO[LoggingService, Unit] =
    ZIO.serviceWithZIO[LoggingService](_.log(line))
}

trait EmailService {
  def send(user: String, content: String): Task[Unit]
}

object EmailService {
  def send(user: String, content: String): ZIO[EmailService, Throwable, Unit] =
    ZIO.serviceWithZIO[EmailService](_.send(user, content))
}
```

구현체:

```scala
case class LoggingServiceLive() extends LoggingService {
  override def log(line: String): UIO[Unit] =
    ZIO.succeed(print(line))
}

case class EmailServiceFake() extends EmailService {
  override def send(user: String, content: String): Task[Unit] =
    ZIO.attempt(println(s"sending email to $user"))
}
```

커스텀 런타임 생성:

```scala
val testableRuntime = Runtime(
  ZEnvironment[LoggingService, EmailService](LoggingServiceLive(), EmailServiceFake()),
  FiberRefs.empty,
  RuntimeFlags.default)
```

또는 기본 런타임을 수정:

```scala
val testableRuntime: Runtime[LoggingService with EmailService] =
  Runtime.default.withEnvironment {
    ZEnvironment[LoggingService, EmailService](LoggingServiceLive(), EmailServiceFake())
  }
```

커스텀 런타임으로 효과 실행:

```scala
Unsafe.unsafe { implicit unsafe =>
  testableRuntime.unsafe.run(
    for {
      _ <- LoggingService.log("sending newsletter")
      _ <- EmailService.send("David", "Hi! Here is today's newsletter.")
    } yield ()
  ).getOrThrowFiberFailure()
}
```

---

## 10. ZIOApp과 ZIOAppDefault(Application Entry Points)

### 10.1 개요(Overview)

공식 문서에 따르면 "`ZIOApp` 트레이트는 애플리케이션 간에 레이어(layers)를 공유할 수 있게 해 주는 ZIO 애플리케이션의 진입점(entry point)"입니다. 또한 여러 ZIO 애플리케이션을 서로 합성(compose)할 수 있게 합니다.

`ZIOAppDefault`는 기본 ZIO 환경(default ZIO environment, ZEnv)을 사용하는 더 간단한 대안입니다.

### 10.2 ZIO 효과 실행하기(Running a ZIO Effect)

JVM에서 ZIO 애플리케이션을 실행하는 진입점은 `run` 함수입니다.

```scala
import zio._

object MyApp extends ZIOAppDefault {
  def run = for {
    _ <- Console.printLine("Hello! What is your name?")
    n <- Console.readLine
    _ <- Console.printLine("Hello, " + n + ", good to meet you!")
  } yield ()
}
```

### 10.3 명령줄 인자 접근(Accessing Command-line Arguments)

ZIO는 명령줄 인자(command-line arguments)에 접근하기 위한 내장 서비스 `ZIOAppArgs`를 제공합니다.

```scala
import zio._

object HelloApp extends ZIOAppDefault {
  def run = for {
    args <- getArgs
    _ <-
      if (args.isEmpty)
        Console.printLine("Please provide your name as an argument")
      else
        Console.printLine(s"Hello, ${args.head}!")
  } yield ()
}
```

### 10.4 커스터마이징된 런타임 (부트스트랩 레이어)(Customized Runtime / Bootstrap Layers)

`bootstrap` 값을 오버라이드하여 개인화된 실행기(executor)를 가진 커스텀 런타임을 설정할 수 있습니다.

```scala
import zio._
import zio.Executor
import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

object CustomizedRuntimeZIOApp extends ZIOAppDefault {
  override val bootstrap = Runtime.setExecutor(
    Executor.fromThreadPoolExecutor(
      new ThreadPoolExecutor(
        5,
        10,
        5000,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue[Runnable]()
      )
    )
  )
  def run = myAppLogic
}
```

### 10.5 여러 ZIO 애플리케이션 합성하기(Composing Multiple ZIO Applications)

`<>` 연산자를 사용하여 애플리케이션을 결합합니다.

```scala
import zio._

object MyApp1 extends ZIOAppDefault {
  def run = ZIO.succeed(???)
}

object MyApp2 extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Any, Any] =
    asyncProfiler ++ slf4j ++ loggly ++ newRelic
  def run = ZIO.succeed(???)
}

object Main extends ZIOApp.Proxy(MyApp1 <> MyApp2)
```

문서는 "`<>` 연산자가 두 애플리케이션의 레이어를 결합한 다음, 두 애플리케이션을 병렬로(in parallel) 실행한다"고 설명합니다.

### 10.6 우아한 종료 타임아웃(Graceful Shutdown Timeout)

인터럽트 신호(예: Ctrl+C로 인한 SIGINT)를 받으면 "런타임은 종료 전에 모든 종료자(finalizers, 정리 로직)를 실행하려고 시도합니다." 기본값은 무한(infinite)이며, `gracefulShutdownTimeout`을 오버라이드하여 변경할 수 있습니다.

#### 예제 1: 종료자가 타임아웃 내에 완료되는 경우

```scala
import zio._

object MyApp extends ZIOAppDefault {
  override def gracefulShutdownTimeout: Duration = 30.seconds

  val run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    ZIO.acquireReleaseWith(
      acquire = ZIO.logInfo("Acquiring resource...").as("MyResource")
    )(release =
      _ =>
        ZIO.logInfo("Releasing resource (3s) ...") *> ZIO.sleep(3.seconds) *>
          ZIO.logInfo("Cleanup done")
    ) { resource =>
      ZIO.logInfo(s"Running with $resource, press Ctrl+C to interrupt") *> ZIO.never
    }
}
```

#### 예제 2: 종료자가 타임아웃을 초과하는 경우

```scala
import zio._

object MyAppTimeout extends ZIOAppDefault {
  override def gracefulShutdownTimeout: Duration = 5.seconds

  val run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    ZIO.acquireReleaseWith(
      acquire = ZIO.logInfo("Acquiring resource...").as("MyResource")
    )(release =
      _ =>
        ZIO.logInfo("Releasing resource (20s) ...") *> ZIO.sleep(20.seconds) *>
          ZIO.logInfo("Cleanup done")
    ) { resource =>
      ZIO.logInfo(s"Running with $resource, press Ctrl+C to interrupt") *> ZIO.never
    }
}
```

타임아웃을 초과하면 애플리케이션은 경고(warning)를 출력하고 즉시 종료(exits immediately)합니다.

---

## 11. 참고 자료

- [Core Data Types | ZIO](https://zio.dev/reference/core/)
- [ZIO[R, E, A] | ZIO](https://zio.dev/reference/core/zio)
- [UIO | ZIO](https://zio.dev/reference/core/zio/uio/)
- [Exit | ZIO](https://zio.dev/reference/core/exit)
- [Cause | ZIO](https://zio.dev/reference/core/cause)
- [Runtime | ZIO](https://zio.dev/reference/core/runtime)
- [ZIOApp | ZIO](https://zio.dev/reference/core/zioapp)
- [Exceptional and Unexceptional Effects | ZIO](https://zio.dev/reference/error-management/exceptional-and-unexceptional-effects/)
