# ZIO 스트리밍: ZStream, ZSink, ZPipeline

> 원본: https://zio.dev/reference/stream/

---

## 목차

1. [ZIO 스트림 소개(Introduction to ZIO Streams)](#1-zio-스트림-소개introduction-to-zio-streams)
2. [세 가지 핵심 추상화: ZStream, ZSink, ZPipeline(Core Abstractions)](#2-세-가지-핵심-추상화-zstream-zsink-zpipelinecore-abstractions)
3. [스트림은 기본적으로 청크 단위(Streams Are Chunked by Default)](#3-스트림은-기본적으로-청크-단위streams-are-chunked-by-default)
4. [ZStream 정의와 타입(ZStream Definition and Types)](#4-zstream-정의와-타입zstream-definition-and-types)
5. [ZStream 생성하기(Creating ZIO Streams)](#5-zstream-생성하기creating-zio-streams)
6. [리소스 안전 스트림(Resourceful Streams)](#6-리소스-안전-스트림resourceful-streams)
7. [ZStream 연산(ZStream Operations)](#7-zstream-연산zstream-operations)
8. [동시성과 병렬성, 백프레셔(Concurrency, Parallelism and Back-Pressure)](#8-동시성과-병렬성-백프레셔concurrency-parallelism-and-back-pressure)
9. [스트림 소비하기(Consuming Streams)](#9-스트림-소비하기consuming-streams)
10. [에러 처리(Error Handling)](#10-에러-처리error-handling)
11. [스케줄링(Scheduling)](#11-스케줄링scheduling)
12. [ZSink: 소비자와 집계(ZSink: Consumer and Aggregation)](#12-zsink-소비자와-집계zsink-consumer-and-aggregation)
13. [ZPipeline: 스트림 변환기(ZPipeline: Stream Transformer)](#13-zpipeline-스트림-변환기zpipeline-stream-transformer)
14. [SubscriptionRef: 구독 가능한 공유 상태(SubscriptionRef)](#14-subscriptionref-구독-가능한-공유-상태subscriptionref)
15. [ZChannel: 통합 기본 요소(ZChannel: The Unified Primitive)](#15-zchannel-통합-기본-요소zchannel-the-unified-primitive)
16. [참고 자료](#16-참고-자료)

---

## 1. ZIO 스트림 소개(Introduction to ZIO Streams)

ZIO 스트림(ZIO Streams)은 데이터 소스(data source)와 목적지(destination)를 사용한 읽기·쓰기 작업의 메커니즘을 추상화(abstract)하는 고수준 API입니다. 이를 통해 개발자는 저수준 구현 세부 사항이 아니라 비즈니스 로직(business logic)에 집중할 수 있습니다.

### 스트림이 나타나는 곳(Use Cases)

스트림은 다양한 프로그래밍 맥락에서 나타납니다.

- **파일(Files)**: 파일 I/O를 저수준 연산이 아닌 바이트(byte)의 스트림으로 표현
- **소켓(Sockets)**: 소켓 통신을 입력 바이트 스트림에서 출력 스트림으로의 변환으로 모델링
- **이벤트 소싱(Event-Sourcing)**: Kafka나 AMQP 같은 시스템의 이벤트를 처리하는 애플리케이션의 기반
- **UI 애플리케이션(UI Applications)**: 사용자 상호작용을 이벤트 스트림으로 표현
- **HTTP 서버(HTTP Servers)**: 서버를 요청 스트림을 응답 스트림으로 변환하는 함수로 간주

### 동기(Motivation): 배치 처리 vs. 스트리밍

다음은 배치(batch) 방식으로 1부터 1000까지의 수 중 소수(prime)를 찾아 추가 작업을 수행하는 예제입니다(`ZIO.filterPar`, `ZIO.foreachPar` 사용).

```scala
import zio.ZIOAspect._
def isPrime(number: Int): Task[Boolean] = ZIO.succeed(???)
def moreHardWork(i: Int): Task[Boolean] = ZIO.succeed(???)
val numbers = 1 to 1000
for {
  primes <- ZIO.filterPar(numbers)(isPrime)
  _      <- ZIO.foreachPar(primes)(moreHardWork) @@ parallel(20)
} yield ()
```

배치 방식의 문제점은 두 가지입니다.

- **높은 지연(High Latency)**: 다음 단계로 넘어가기 전에 전체 배치 처리를 끝내야 합니다.
- **제한된 메모리(Limited Memory)**: 전체 리스트를 메모리에 보관해야 합니다.

스트리밍 방식은 이러한 문제를 해결합니다.

```scala
def prime(number: Int): Task[(Boolean, Int)] = ZIO.succeed(???)
ZStream.fromIterable(numbers)
  .mapZIOParUnordered(20)(prime(_))
  .filter(_._1).map(_._2)
  .mapZIOParUnordered(20)(moreHardWork(_))
```

파이버(fiber)와 큐(queue)로 파이프라인을 직접 구성하면 복잡하고 오류가 생기기 쉽습니다. ZIO 스트림은 이를 선언적으로 해결합니다.

```scala
def generateElement: Task[Int]    = ZIO.succeed(???)
def process(i: Int): Task[Int]    = ZIO.succeed(???)
def printElem(i: Int): Task[Unit] = ZIO.succeed(???)
ZStream
  .repeatZIO(generateElement)
  .buffer(16)
  .mapZIO(process(_))
  .buffer(16)
  .mapZIO(process(_))
  .buffer(16)
  .tap(printElem(_))
```

### 왜 스트림인가?(Why Streams?) — 8가지 핵심 이점

1. **고수준이며 선언적(High-level and Declarative)**: "유려한 코드의 매우 짧은 스니펫만으로 터무니없이 복잡한 문제를 해결할 수 있습니다."
2. **비동기·논블로킹(Asynchronous and Non-blocking)**: 리액티브(reactive)하고, 스레드 효율적이며, 확장 가능(scalable)합니다.
3. **동시성과 병렬성(Concurrency and Parallelism)**: 안전한 동시성 연산자를 제공하며, `mapZIOPar`, `flatMapPar` 같은 병렬 변형(parallel variant)이 있습니다.
4. **리소스 안전(Resource Safety)**: 타임아웃(timeout), 인터럽트(interruption), 에러가 발생하더라도 "리소스를 절대 누수하지 않을 것을 보장(guarantee that it will never leak resources)"합니다.
5. **고성능과 효율(High Performance and Efficiency)**: 요소 단위 API를 유지하면서도 암묵적 청킹(implicit chunking)을 통해 I/O 효율을 확보합니다.
6. **ZIO와의 매끄러운 통합(Seamless Integration with ZIO)**: `Scope`, `Schedule` 등 ZIO 데이터 타입을 그대로 사용합니다.
7. **백프레셔(Back-Pressure)**: 풀 기반(pull-based) 메커니즘으로, "데이터 파이프라인의 끝에서 필요할 때 요소를 당겨오기 때문에 최소한의 계산만 필요"합니다.
8. **유한한 메모리로 무한 데이터 처리(Infinite Data using Finite Memory)**: 무한 스트림을 유한 메모리 제약 안에서 처리할 수 있습니다.

#### 파일 처리 예제 비교

전통적 방식:

```scala
for (line <- FileUtils.readFileToString(new File("file.txt")).split('\n'))
  println(line)
```

스트리밍 방식:

```scala
ZStream.fromFileName("file.txt")
  .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
  .foreach(printLine(_))
```

---

## 2. 세 가지 핵심 추상화: ZStream, ZSink, ZPipeline(Core Abstractions)

ZIO 스트리밍은 세 가지 핵심 추상화로 구성됩니다. 비유하면 파이프(pipe)와 같습니다. **ZStream은 값을 생산하는 소스(source)**, **ZSink은 값을 소비하는 수용기(receptacle)**, **ZPipeline은 값을 변환하는 변환기**(transformer)입니다.

### ZStream (소스 / 생산자)

- 값을 **생산**(produce)하는 소스 역할을 합니다.
- `ZIO[R, E, A]`와 유사하지만, 단 하나가 아니라 0개 이상의 요소를 생산합니다.
- "비어 있지 않은(non-empty) `ZStream`이라는 것은 존재하지 않습니다. 모든 `ZStream`은 비어 있으며, 임의 개수의 `A`를 생산할 수 있습니다."
- 극도로 게으르며(lazy), 소비하지 않고서는 비어 있는지조차 확인할 수 없습니다.

### ZSink (수용기 / 소비자)

- 값을 **소비**(consume)하는 수용기 역할을 합니다.
- "어떤 타입의 값을 소비하고, 작업이 끝나면 종료됩니다." 파서(parser)나 데이터베이스에 비유할 수 있습니다.
- "범주론(category theory)에서 스트림과 싱크는 쌍대(dual) 관계입니다. 하나는 값을 생산하고, 다른 하나는 값을 소비합니다."
- 합성(compositional) 가능하고 변환(transformable) 가능합니다.

### ZPipeline (변환기)

- 값을 **변환**(transform)하는 변환기 역할을 합니다.
- 타입 `A`에서 타입 `B`로 상태를 가진(stateful) 변환을 수행합니다.
- 사용 사례: 분할/카운팅(예: 줄을 단어로), 코덱(codec)(예: 바이트 → JSON → 사용자 정의 타입).
- 스트림 위에 쌓아 요소 타입을 바꾸거나, 싱크 위에 쌓아 입력 타입을 바꿀 수 있습니다.
- "요소들을 상태를 유지하며 계속 변환하는 파이프의 중간 부분"입니다.

---

## 3. 스트림은 기본적으로 청크 단위(Streams Are Chunked by Default)

스트림은 개별 요소가 아니라 청크(chunk) 단위로 동작합니다. 스트림을 평가하면 단일 항목이 아니라 요소들의 청크를 당겨옵니다.

**근거**: "이는 효율성과 성능 문제 때문입니다. 프로그래밍 세계의 모든 I/O 연산은 배치(batch) 단위로 동작합니다." 파일 디스크립터 읽기/쓰기, 소켓 연산, HTTP 서버, JDBC 드라이버 모두 성능을 위해 여러 요소를 한 번에 처리합니다.

**Chunk에 대하여**: "Chunk는 ZIO의 불변(immutable) 배열 기반 컬렉션"입니다. 원래 ZIO 스트림을 위해 설계되었으나 범용 컬렉션 타입이 되었습니다. 핵심 특징은 "프리미티브(primitive)를 박싱하지 않은(unboxed) 상태로 유지하려 한다"는 점으로, 효율적인 파일/소켓 처리와 인코딩/디코딩, 트랜스듀서(transducer) 연산에 중요합니다.

청크 관련 연산자(`mapChunks`, `grouped`, `rechunk`, `flattenChunks`)는 [연산 섹션](#7-zstream-연산zstream-operations)에서 다룹니다.

---

## 4. ZStream 정의와 타입(ZStream Definition and Types)

`ZStream[R, E, O]`는 "평가될 때 0개 이상의 타입 `O` 값을 방출(emit)할 수 있고, 타입 `E`의 에러로 실패할 수 있는 프로그램의 설명(description)"이며, 환경 타입 `R`을 요구합니다.

`ZIO[R, E, A]`와의 핵심 차이는, ZIO 이펙트는 성공 시 정확히 하나의 값을 반환하는 반면, 스트림은 0개, 여러 개, 혹은 무한 개의 값을 산출할 수 있다는 점입니다.

### 스트림이 표현할 수 있는 네 가지 시나리오

1. **빈 스트림(Empty Streams)** — 요소를 방출하지 않음
2. **단일 요소 스트림(Single Element Streams)** — 하나의 값 산출
3. **유한 다중 요소 스트림(Finite Multiple Element Streams)** — 고정된 개수의 값
4. **무한 요소 스트림(Infinite Element Streams)** — 끝없는 값 방출(예: Kafka 토픽, 소켓 스트림)

### 주요 타입 파라미터 변형

- `ZStream[Any, Nothing, O]` — `O` 값을 실패 없이(infallible) 방출
- `ZStream[Any, Throwable, O]` — `Throwable` 에러로 실패 가능한 방출
- `ZStream[Any, Nothing, Nothing]` — 요소를 방출하지 않음
- `ZStream[R, E, O]` — 서비스 의존성 `R`을 가진 일반형
- `UStream[O]` — `ZStream[Any, Nothing, O]`의 별칭(실패하지 않는 스트림)

---

## 5. ZStream 생성하기(Creating ZIO Streams)

### 5.1 공통 생성자(Common Constructors)

```scala
// 가변 인자 값으로부터 순수 스트림 생성
val stream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)

// 단일 Unit 값을 담은 스트림
val unit: ZStream[Any, Nothing, Unit] = ZStream.unit

// 값을 방출하지도, 실패하지도 않는 스트림
val never: ZStream[Any, Nothing, Nothing] = ZStream.never

// 지정한 스케줄에 따라 반복
val repeat: ZStream[Any, Nothing, Int] = ZStream(1).repeat(Schedule.forever)

// 초기값에서 시작해 함수를 반복 적용
val nats: ZStream[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1) // 1, 2, 3, ...

// 정수 범위 [min, max)
val range: ZStream[Any, Nothing, Int] = ZStream.range(1, 5) // 1, 2, 3, 4

// 환경에서 서비스 추출
trait Foo
val fooStream: ZStream[Foo, Nothing, Foo] = ZStream.service[Foo]

// 스코프(scope) 리소스로부터 단일 값 스트림 생성
val scopedStream: ZStream[Any, Throwable, BufferedReader] =
  ZStream.scoped(
    ZIO.fromAutoCloseable(
      ZIO.attemptBlocking(
        Files.newBufferedReader(java.nio.file.Paths.get("file.txt"))
      )
    )
  )
```

### 5.2 성공과 실패로부터(From Success and Failure)

```scala
val s1: ZStream[Any, String, Nothing] = ZStream.fail("Uh oh!")
val s2: ZStream[Any, Nothing, Int] = ZStream.succeed(5)
```

### 5.3 청크로부터(From Chunks)

```scala
val s1 = ZStream.fromChunk(Chunk(1, 2, 3))
val s2 = ZStream.fromChunks(Chunk(1, 2, 3), Chunk(4, 5, 6))
```

### 5.4 ZIO로부터(From ZIO)

```scala
// ZIO 워크플로로부터 스트림 생성
val readline: ZStream[Any, IOException, String] =
  ZStream.fromZIO(Console.readLine)

val randomInt: ZStream[Any, Nothing, Int] =
  ZStream.fromZIO(Random.nextInt)
```

`ZStream.fromZIOOption`은 ZIO 결과에 따라 요소를 방출하거나 빈 스트림을 반환합니다. `None`은 스트림 종료를 의미합니다.

```scala
object ZStream {
  def fromZIOOption[R, E, A](fa: ZIO[R, Option[E], A]): ZStream[R, E, A] = ???
}
```
```scala
val userInput: ZStream[Any, IOException, String] =
  ZStream.fromZIOOption(
    Console.readLine.mapError(Option(_)).flatMap {
      case "EOF" => ZIO.fail[Option[IOException]](None)
      case o     => ZIO.succeed(o)
    }
  )
```

### 5.5 비동기 콜백으로부터(From Asynchronous Callback)

```scala
def registerCallback(
    name: String,
    onEvent: Int => Unit,
    onError: Throwable => Unit): Unit = ???

val stream = ZStream.async[Any, Throwable, Int] { cb =>
  registerCallback(
    "foo",
    event => cb(ZIO.succeed(Chunk(event))),
    error => cb(ZIO.fail(error).mapError(Some(_)))
  )
}
```

### 5.6 이터레이터로부터(From Iterators)

```scala
// 예외를 던지지 않는 이터레이터
val s1: ZStream[Any, Throwable, Int] = ZStream.fromIterator(Iterator(1, 2, 3))
val s2: ZStream[Any, Throwable, Int] = ZStream.fromIterator(Iterator.range(1, 4))
val s3: ZStream[Any, Throwable, Int] = ZStream.fromIterator(Iterator.continually(0))

// 예외를 던질 수 있는 이펙트성 이터레이터
import scala.io.Source
val lines: ZStream[Any, Throwable, String] =
  ZStream.fromIteratorZIO(ZIO.attempt(Source.fromFile("file.txt").getLines()))

// 스코프 이터레이터(자원 안전)
val lines2: ZStream[Any, Throwable, String] =
  ZStream.fromIteratorScoped(
    ZIO.fromAutoCloseable(
      ZIO.attempt(scala.io.Source.fromFile("file.txt"))
    ).map(_.getLines())
  )

// Java 이터레이터
def fromJavaStream[A](stream: => java.util.stream.Stream[A]): ZStream[Any, Throwable, A] =
  ZStream.fromJavaIterator(stream.iterator())
```

### 5.7 이터러블로부터(From Iterables)

```scala
val list = ZStream.fromIterable(List(1, 2, 3))

trait Database {
  def getUsers: Task[List[User]]
}
object Database {
  def getUsers: ZIO[Database, Throwable, List[User]] =
    ZIO.serviceWithZIO[Database](_.getUsers)
}
val users: ZStream[Database, Throwable, User] =
  ZStream.fromIterableZIO(Database.getUsers)
```

### 5.8 반복으로부터(From Repetition)

```scala
// 값을 무한 반복
val repeatZero: ZStream[Any, Nothing, Int] = ZStream.repeat(0)

// 스케줄에 따라 반복
val repeatZeroEverySecond: ZStream[Any, Nothing, Int] =
  ZStream.repeatWithSchedule(0, Schedule.spaced(1.seconds))

// 이펙트를 무한 반복
val randomInts: ZStream[Any, Nothing, Int] =
  ZStream.repeatZIO(Random.nextInt)

// 조건에 따라 반복 종료 (None이면 종료)
val userInputs: ZStream[Any, IOException, String] =
  ZStream.repeatZIOOption(
    Console.readLine.mapError(Option(_)).flatMap {
      case "EOF" => ZIO.fail[Option[IOException]](None)
      case o     => ZIO.succeed(o)
    }
  )

def drainIterator[A](it: Iterator[A]): ZStream[Any, Throwable, A] =
  ZStream.repeatZIOOption {
    ZIO.attempt(it.hasNext).mapError(Some(_)).flatMap { hasNext =>
      if (hasNext) ZIO.attempt(it.next()).mapError(Some(_))
      else ZIO.fail(None)
    }
  }

// 일정 간격으로 Unit 방출
val tick: ZStream[Any, Nothing, Unit] = ZStream.tick(1.seconds)
```

### 5.9 언폴딩/페이지네이션으로부터(From Unfolding / Pagination)

```scala
object ZStream {
  def unfold[S, A](s: S)(f: S => Option[(A, S)]): ZStream[Any, Nothing, A] = ???
}
```
```scala
val nats: ZStream[Any, Nothing, Int] = ZStream.unfold(1)(n => Some((n, n + 1)))

def countdown(n: Int) = ZStream.unfold(n) {
  case 0 => None
  case s => Some((s, s - 1))
}

// 이펙트성 unfold
val inputs: ZStream[Any, IOException, String] = ZStream.unfoldZIO(()) { _ =>
  Console.readLine.map {
    case "exit"  => None
    case i => Some((i, ()))
  }
}

// paginate: unfold와 비슷하나 한 단계 더 진행해 값을 방출
val stream = ZStream.paginate(0) { s =>
  s -> (if (s < 3) Some(s + 1) else None)
}

// 페이지네이션 API를 스트림으로 변환
case class PageResult(results: Chunk[RowData], isLast: Boolean)
def listPaginated(pageNumber: Int): ZIO[Any, Throwable, PageResult] = ZIO.fail(???)
val finalAttempt: ZStream[Any, Throwable, RowData] =
  ZStream.paginateChunkZIO(0) { pageNumber =>
    for {
      page <- listPaginated(pageNumber)
    } yield page.results -> (if (!page.isLast) Some(pageNumber + 1) else None)
  }
```

### 5.10 래핑된 스트림으로부터(From Wrapped Streams)

```scala
val wrappedWithZIO: UIO[ZStream[Any, Nothing, Int]] = ZIO.succeed(ZStream(1, 2, 3))
val s1: ZStream[Any, Nothing, Int] = ZStream.unwrap(wrappedWithZIO)

val wrappedWithZIOScoped = ZIO.succeed(ZStream(1, 2, 3))
val s2: ZStream[Any, Nothing, Int] = ZStream.unwrapScoped(wrappedWithZIOScoped)
```

### 5.11 Java IO로부터(From Java IO)

```scala
import java.nio.file.Paths

val file: ZStream[Any, Throwable, Byte] = ZStream.fromPath(Paths.get("file.txt"))

val stream: ZStream[Any, IOException, Byte] =
  ZStream.fromInputStream(new FileInputStream("file.txt"))

// 소진 후 닫힘을 보장
val stream2: ZStream[Any, IOException, Byte] =
  ZStream.fromInputStreamZIO(
    ZIO.attempt(new FileInputStream("file.txt"))
      .refineToOrDie[IOException]
  )

val resource: ZStream[Any, IOException, Byte] = ZStream.fromResource("file.txt")
val reader: ZStream[Any, IOException, Char]   = ZStream.fromReader(new FileReader("file.txt"))
```

### 5.12 Java Stream / Queue / Hub / Schedule로부터

```scala
// Java Stream
val js: ZStream[Any, Throwable, Int] =
  ZStream.fromJavaStream(java.util.stream.Stream.of(1, 2, 3))

// Queue / Hub
object ZStream {
  def fromQueue[O](queue: Dequeue[O], maxChunkSize: Int = DefaultChunkSize): ZStream[Any, Nothing, O] = ???
  def fromHub[A](hub: Hub[A]): ZStream[Any, Nothing, A] = ???
}

// Schedule로부터: 스케줄의 출력을 스트림으로
val sched: ZStream[Any, Nothing, Long] =
  ZStream.fromSchedule(Schedule.spaced(1.second) >>> Schedule.recurs(10))
```

Hub로부터 스코프 스트림을 만드는 예제:

```scala
for {
  promise <- Promise.make[Nothing, Unit]
  hub     <- Hub.unbounded[Chunk[Int]]
  scoped = ZStream.fromChunkHubScoped(hub).tap(_ => promise.succeed(()))
  stream  = ZStream.unwrapScoped(scoped)
  fiber   <- stream.foreach(printLine(_)).fork
  _       <- promise.await
  _       <- hub.publish(Chunk(1, 2, 3))
  _       <- fiber.join
} yield ()
```

---

## 6. 리소스 안전 스트림(Resourceful Streams)

대부분의 `ZStream` 생성자에는 스코프 리소스(scoped resource)를 스트림으로 들어올리는 특수 변형(예: `ZStream.fromReaderScoped`)이 있으며, 이들은 리소스 안전(resource-safe) 스트림을 만듭니다.

### 6.1 획득-해제(Acquire Release)

```scala
object ZStream {
  def acquireReleaseWith[R, E, A](
    acquire: ZIO[R, E, A]
  )(
    release: A => URIO[R, Any]
  ): ZStream[R, E, A] = ???
}
```

파일을 읽는 예제 (열고 닫힘이 보장됨):

```scala
val lines: ZStream[Any, Throwable, String] =
  ZStream
    .acquireReleaseWith(
      ZIO.attempt(Source.fromFile("file.txt")) <* printLine("The file was opened.")
    )(x => ZIO.succeed(x.close()) <* printLine("The file was closed.").orDie)
    .flatMap { is =>
      ZStream.fromIterator(is.getLines())
    }
```

### 6.2 종료 처리(Finalization)

```scala
object ZStream {
  def finalizer[R](finalizer: URIO[R, Any]): ZStream[R, Nothing, Any] = ???
}
```

임시 디렉터리 정리 예제:

```scala
import zio.Console._
def application: ZStream[Any, IOException, Unit] = ZStream.fromZIO(printLine("Application Logic."))
def deleteDir(dir: Path): ZIO[Any, IOException, Unit] = printLine("Deleting file.")
val myApp: ZStream[Any, IOException, Any] =
  application ++ ZStream.finalizer(
    (deleteDir(Paths.get("tmp")) *>
      printLine("Temporary directory was deleted.")).orDie
  )
```

### 6.3 보장(Ensuring)

`ensuring` 연산자는 스트림 종료 처리 이후에 코드를 실행합니다.

```scala
ZStream
  .finalizer(Console.printLine("Finalizing the stream").orDie)
  .ensuring(
    printLine("Doing some other works after stream's finalization").orDie
  )
  // 출력:
  // Finalizing the stream
  // Doing some other works after stream's finalization
```

---

## 7. ZStream 연산(ZStream Operations)

### 7.1 탭핑(Tapping)

각 방출마다 이펙트를 실행하되 요소는 바꾸지 않습니다. 관찰·로깅에 사용합니다.

```scala
val stream: ZStream[Any, IOException, Int] = ZStream(1, 2, 3)
  .tap(x => printLine(s"before mapping: $x"))
  .map(_ * 2)
  .tap(x => printLine(s"after mapping: $x"))
```

### 7.2 요소 가져오기(Taking Elements)

```scala
val stream = ZStream.iterate(0)(_ + 1)
val s1 = stream.take(5)            // 0, 1, 2, 3, 4
val s2 = stream.takeWhile(_ < 5)   // 0, 1, 2, 3, 4
val s3 = stream.takeUntil(_ == 5)  // 0, 1, 2, 3, 4, 5
val s4 = s3.takeRight(3)           // 3, 4, 5
```

### 7.3 매핑(Mapping)

```scala
import zio.stream._
val intStream: UStream[Int] = ZStream.fromIterable(0 to 100)
val stringStream: UStream[String] = intStream.map(_.toString)
```

`mapZIOPar`는 `mapZIO`처럼 동작하되 이펙트를 병렬로 평가하고, 결과는 원래 순서대로 다운스트림에 방출합니다. `n`은 동시에 실행할 이펙트의 수입니다.

```scala
def fetchUrl(url: URL): Task[String] = ZIO.succeed(???)
def getUrls: Task[List[URL]] = ZIO.succeed(???)
val pages = ZStream.fromIterableZIO(getUrls).mapZIOPar(8)(fetchUrl)
```

`mapChunks`는 내부 청크를 한꺼번에 변환합니다.

```scala
val chunked = ZStream.fromChunks(Chunk(1, 2, 3), Chunk(4, 5), Chunk(6, 7, 8, 9))
val stream = chunked.mapChunks(x => x.tail)
// 입력:  1, 2, 3, 4, 5, 6, 7, 8, 9
// 출력:     2, 3,    5,    7, 8, 9
```

`mapAccum`은 상태를 유지하며 매핑합니다(map + 누적).

```scala
abstract class ZStream[-R, +E, +O] {
  def mapAccum[S, O1](s: S)(f: (S, O) => (S, O1)): ZStream[R, E, O1]
}
```
```scala
def runningTotal(stream: UStream[Int]): UStream[Int] =
  stream.mapAccum(0)((acc, next) => (acc + next, acc + next))
// 입력:  0, 1, 2, 3,  4,  5
// 출력: 0, 1, 3, 6, 10, 15
```

`mapConcat`은 각 요소를 0개 이상의 `Iterable` 요소로 매핑한 뒤 평탄화(flatten)합니다.

```scala
val numbers: UStream[Int] = ZStream("1-2-3", "4-5", "6")
  .mapConcat(_.split("-"))
  .map(_.toInt)
// 출력: 1, 2, 3, 4, 5, 6

// 성공 값을 상수로 매핑
val unitStream: ZStream[Any, Nothing, Unit] = ZStream.range(1, 5).as(())
```

### 7.4 필터링(Filtering)

```scala
val s1 = ZStream.range(1, 11).filter(_ % 2 == 0)        // 2, 4, 6, 8, 10

// withFilter로 for-comprehension 스타일 사용 가능
val s2 = for {
  i <- ZStream.range(1, 11).take(10)
  if i % 2 == 0
} yield i                                                // 2, 4, 6, 8, 10

val s3 = ZStream.range(1, 11).filterNot(_ % 2 == 0)      // 1, 3, 5, 7, 9
```

### 7.5 스캐닝(Scanning)

`scan`은 누적 중간 결과를 모두 방출하는 폴드(fold)입니다.

```scala
val scan = ZStream(1, 2, 3, 4, 5).scan(0)(_ + _)
// 출력: 0, 1, 3, 6, 10, 15

val fold = ZStream(1, 2, 3, 4, 5).runFold(0)(_ + _)
// 출력: 15 (15를 담은 ZIO 이펙트)
```

### 7.6 드레이닝(Draining)

`drain`은 스트림의 모든 출력 값을 버립니다(이펙트는 실행되지만 요소는 방출하지 않음).

```scala
val s1: ZStream[Any, Nothing, Nothing] = ZStream(1, 2, 3, 4, 5).drain
// 방출 요소: <빈 스트림>

val logging = ZStream.fromZIO(printLine("Starting to merge with the next stream"))
val stream = ZStream(1, 2, 3) ++ logging.drain ++ ZStream(4, 5, 6)
// 방출 요소: 1, 2, 3, 4, 5, 6

val stream2 = ZStream(1, 2, 3) ++ logging ++ ZStream(4, 5, 6)
// 방출 요소: 1, 2, 3, (), 4, 5, 6
```

### 7.7 변경 감지(Changes)

`changes`는 직전 요소와 다른 요소만 방출합니다.

```scala
val changes = ZStream(1, 1, 1, 2, 2, 3, 4).changes
// 출력: 1, 2, 3, 4

case class Event(partition: Long, offset: Long, metadata: String)
val events: ZStream[Any, Nothing, Event] = ZStream.fromIterable(???)
val uniques = events.changesWith((e1, e2) =>
  (e1.partition == e2.partition && e1.offset == e2.offset))
```

### 7.8 수집하기(Collecting)

`collect`는 필터 + 맵을 한 단계로 수행합니다.

```scala
val source1 = ZStream(1, 2, 3, 4, 0, 5, 6, 7, 8)
val s1 = source1.collect { case x if x < 6 => x * 2 }       // 2, 4, 6, 8, 0, 10
val s2 = source1.collectWhile { case x if x != 0 => x * 2 } // 2, 4, 6, 8

val source2 = ZStream(Left(1), Right(2), Right(3), Left(4), Right(5))
val s3 = source2.collectLeft               // 1, 4
val s4 = source2.collectWhileLeft          // 1
val s5 = source2.collectRight              // 2, 3, 5
val s6 = source2.drop(1).collectWhileRight // 2, 3
val s7 = source2.map(_.toOption).collectSome      // 2, 3, 5
val s8 = source2.map(_.toOption).collectWhileSome // 빈 스트림

// 성공한 이펙트 결과만 수집
val urls = ZStream("dotty.epfl.ch", "zio.dev", "zio.github.io/zio-json", "zio.github.io/zio-nio/")
def fetch(url: String): ZIO[Any, Throwable, String] = ZIO.attemptBlocking(???)
val pages = urls.mapZIO(url => fetch(url).exit).collectSuccess
```

### 7.9 지핑(Zipping)

```scala
val s1: UStream[(Int, String)] = ZStream(1, 2, 3, 4, 5, 6)
  .zipWith(ZStream("a", "b", "c"))((a, b) => (a, b))
val s2: UStream[(Int, String)] = ZStream(1, 2, 3, 4, 5, 6).zip(ZStream("a", "b", "c"))
// 출력: (1, "a"), (2, "b"), (3, "c")  (짧은 쪽에서 종료)

val s3 = ZStream(1, 2, 3).zipAll(ZStream("a", "b", "c", "d", "e"))(0, "x")
val s4 = ZStream(1, 2, 3).zipAllWith(ZStream("a", "b", "c", "d", "e"))(_ => 0, _ => "x")((a, b) => (a, b))
// 출력: (1, a), (2, b), (3, c), (0, d), (0, e)

// zipLatest: 두 스트림의 가장 최신 값을 결합
val s5 = ZStream(1, 2, 3).schedule(Schedule.spaced(1.second))
val s6 = ZStream("a", "b", "c", "d").schedule(Schedule.spaced(500.milliseconds)).rechunk(3)
s5.zipLatest(s6)
// 출력: (1, a), (1, b), (1, c), (1, d), (2, d), (3, d)

val stream: UStream[Int] = ZStream.fromIterable(1 to 5)
val s7 = stream.zipWithPrevious
val s8 = stream.zipWithNext
val s9 = stream.zipWithPreviousAndNext

val indexedStream: ZStream[Any, Nothing, (String, Long)] =
  ZStream("Mary", "James", "Robert", "Patricia").zipWithIndex
// 출력: ("Mary", 0L), ("James", 1L), ("Robert", 2L), ("Patricia", 3L)
```

### 7.10 교차곱(Cross Product)

```scala
val first = ZStream(1, 2, 3)
val second = ZStream("a", "b")
val s1 = first cross second
val s2 = first <*> second
val s3 = first.crossWith(second)((a, b) => (a, b))
// 출력: (1,a), (1,b), (2,a), (2,b), (3,a), (3,b)

val s4 = first crossLeft second   // = first <* second  => 왼쪽 값만: 1, 1, 2, 2, 3, 3
val s6 = first crossRight second  // = first *> second   => 오른쪽 값만: a, b, a, b, a, b
```

### 7.11 분할하기(Partitioning)

`partition`은 술어(predicate)에 따라 두 스트림(참-스트림, 거짓-스트림)의 튜플로 나눕니다.

```scala
val partitionResult: ZIO[Scope, Nothing, (ZStream[Any, Nothing, Int], ZStream[Any, Nothing, Int])] =
  ZStream.fromIterable(0 to 100).partition(_ % 2 == 0, buffer = 50)
```

`partitionEither`는 이펙트성 술어로 분할합니다.

```scala
abstract class ZStream[-R, +E, +O] {
  final def partitionEither[R1 <: R, E1 >: E, O2, O3](
    p: O => ZIO[R1, E1, Either[O2, O3]],
    buffer: Int = 16
  ): ZIO[R1 with Scope, E1, (ZStream[Any, E1, O2], ZStream[Any, E1, O3])]
}
```
```scala
val partitioned =
  ZStream.fromIterable(1 to 10)
    .partitionEither(x => ZIO.succeed(if (x < 5) Left(x) else Right(x)))
```

### 7.12 그룹화 키로 묶기(GroupBy)

`groupByKey`는 함수 `O => K`로 분할합니다.

```scala
import zio._
import zio.stream._

case class Exam(person: String, score: Int)
val examResults = Seq(
  Exam("Alex", 64), Exam("Michael", 97), Exam("Bill", 77),
  Exam("John", 78), Exam("Bobby", 71)
)
val groupByKeyResult: ZStream[Any, Nothing, (Int, Int)] = ZStream
  .fromIterable(examResults)
  .groupByKey(exam => exam.score / 10 * 10) {
    case (k, s) => ZStream.fromZIO(s.runCollect.map(l => k -> l.size))
  }
```

`groupBy`는 이펙트성 분할 함수 `O => ZIO[R1, E1, (K, V)]`를 받습니다.

```scala
val counted: UStream[(Char, Long)] = ZStream("Mary", "James", "Robert",
  "Patricia", "John", "Jennifer", "Rebecca", "Peter")
  .groupBy(x => ZIO.succeed((x.head, x))) { case (char, stream) =>
    ZStream.fromZIO(stream.runCount.map(count => char -> count))
  }
// 출력: (P, 2), (R, 2), (M, 1), (J, 3)
```

### 7.13 청크로 그룹화(Grouping)

```scala
// 지정 크기의 청크로 분할
val groupedResult: ZStream[Any, Nothing, Chunk[Int]] =
  ZStream.fromIterable(0 to 8).grouped(3)
// 출력: Chunk(0, 1, 2), Chunk(3, 4, 5), Chunk(6, 7, 8)

// 시간 또는 청크 크기 중 먼저 도달하는 기준으로 그룹화
import zio._
import zio.Duration._
import zio.stream._
val groupedWithinResult: ZStream[Any, Nothing, Chunk[Int]] = ZStream
  .fromIterable(0 to 10)
  .repeat(Schedule.spaced(1.seconds))
  .groupedWithin(30, 10.seconds)
```

### 7.14 연결(Concatenation)과 flatMap

```scala
val a = ZStream(1, 2, 3)
val b = ZStream(4, 5)
val c1 = a ++ b
val c2 = a concat b
val c3 = ZStream.concatAll(Chunk(a, b))

val stream = ZStream(1, 2, 3).flatMap(x => ZStream.repeat(x).take(4))
// 입력:  1, 2, 3
// 출력: 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3

def getAuthorBooks(author: String): ZStream[Any, Throwable, Book] = ZStream(???)
val authors: ZStream[Any, Throwable, String] = ZStream("Mary", "James", "Robert", "Patricia", "John")
val allBooks: ZStream[Any, Throwable, Book] = authors.flatMap(getAuthorBooks _)
```

### 7.15 병합(Merging)

`merge`는 지정된 스트림들에서 요소를 무작위로(비결정적으로) 가져옵니다.

```scala
val s1 = ZStream(1, 2, 3).rechunk(1)
val s2 = ZStream(4, 5, 6).rechunk(1)
val merged = s1 merge s2
// 출력: 4, 1, 2, 5, 6, 3 (순서는 비결정적)
```

**종료 전략(Termination Strategy)** — 네 가지: Left, Right, Both, Either(기본값 Both).

```scala
import zio.stream.ZStream.HaltStrategy
val s1 = ZStream.iterate(1)(_+1).take(5).rechunk(1)
val s2 = ZStream.repeat(0).rechunk(1)
val merged = s1.merge(s2, HaltStrategy.Left)
```

`mergeAll` / `mergeAllUnbounded`는 여러 스트리밍 컴포넌트를 동시에 결합합니다.

```scala
val main = for {
  _ <- ZStream
    .mergeAllUnbounded(16)(
      kafkaConsumer.drain,
      ZStream.fromZIO(httpServer),
      ZStream.fromZIO(scheduledJobRunner)
    )
    .runDrain
} yield ()
```

`mergeWith`는 두 스트림을 병합하면서 새 요소 타입으로 통일합니다.

```scala
val s1 = ZStream("1", "2", "3")
val s2 = ZStream(4.1, 5.3, 6.2)
val merged = s1.mergeWith(s2)(_.toInt, _.toInt)
```

### 7.16 인터리빙(Interleaving)

병합과 달리 결정적(deterministic)이며, 각 스트림에서 번갈아 한 요소씩 가져옵니다.

```scala
val s1 = ZStream(1, 2, 3)
val s2 = ZStream(4, 5, 6, 7, 8)
val interleaved = s1 interleave s2
// 출력: 1, 4, 2, 5, 3, 6, 7, 8

val s3 = ZStream(1, 3, 5, 7, 9)
val s4 = ZStream(2, 4, 6, 8, 10)
val interleaved2 = s3.interleaveWith(s4)(ZStream(true, false, false).forever)
// 출력: 1, 2, 4, 3, 6, 8, 5, 10, 7, 9
```

### 7.17 사이에 끼우기(Interspersing)

```scala
val s1 = ZStream(1, 2, 3, 4, 5).intersperse(0)
// 출력: 1, 0, 2, 0, 3, 0, 4, 0, 5

val s2 = ZStream("a", "b", "c", "d").intersperse("[", "-", "]")
// 출력: [, -, a, -, b, -, c, -, d]
```

### 7.18 브로드캐스팅(Broadcasting)

`broadcast`는 원본과 동일한 요소를 갖는 스코프 스트림 목록을 반환하며, 각 요소를 반환된 모든 스트림에 방출합니다.

```scala
val stream: ZIO[Any, IOException, Unit] = ZIO.scoped {
  ZStream
    .fromIterable(1 to 20)
    .mapZIO(_ => Random.nextInt)
    .map(Math.abs)
    .map(_ % 100)
    .tap(e => printLine(s"Emit $e element before broadcasting"))
    .broadcast(2, 5)
    .flatMap { streams =>
      for {
        out1 <- streams(0).runFold(0)((acc, e) => Math.max(acc, e))
                  .flatMap(x => printLine(s"Maximum: $x"))
                  .fork
        out2 <- streams(1).schedule(Schedule.spaced(1.second))
                  .foreach(x => printLine(s"Logging to the Console: $x"))
                  .fork
        _ <- out1.join.zipPar(out2.join)
      } yield ()
    }
}
```

### 7.19 분배(Distribution)

`distributedWith`는 broadcast보다 강력하며, `decide` 함수를 사용해 요소를 다운스트림 큐들에 분배합니다.

```scala
abstract class ZStream[-R, +E, +O] {
  final def distributedWith[E1 >: E](
    n: Int,
    maximumLag: Int,
    decide: O => UIO[Int => Boolean]
  ): ZIO[R with Scope, Nothing, List[Dequeue[Exit[Option[E1], O]]]]
}
```
```scala
val partitioned =
  ZStream.iterate(1)(_ + 1)
    .schedule(Schedule.fixed(1.seconds))
    .distributedWith(3, 10, x => ZIO.succeed(q => x % 3 == q))
    .flatMap {
      case q1 :: q2 :: q3 :: Nil =>
        ZIO.succeed(
          ZStream.fromQueue(q1).flattenExitOption,
          ZStream.fromQueue(q2).flattenExitOption,
          ZStream.fromQueue(q3).flattenExitOption
        )
      case _ => ZIO.dieMessage("Impossible!")
    }
```

### 7.20 버퍼링(Buffering)

ZIO 스트림은 풀 기반(pull-based)입니다. 빠른 생산자와 느린 소비자를 분리하려면 버퍼링 큐를 사용합니다.

```scala
ZStream
  .fromIterable(1 to 10)
  .rechunk(1)
  .tap(x => Console.printLine(s"before buffering: $x"))
  .buffer(4)
  .tap(x => Console.printLine(s"after buffering: $x"))
  .schedule(Schedule.spaced(5.second))
```

큐 종류별 버퍼 변형: `buffer(capacity)`(유계, bounded), `bufferUnbounded`, `bufferSliding(capacity)`(슬라이딩), `bufferDropping(capacity)`(드롭).

### 7.21 디바운싱(Debouncing)

`debounce`는 요소 간 최소 주기 `d`를 강제합니다.

```scala
val stream = (ZStream(1, 2, 3) ++
  ZStream.fromZIO(ZIO.sleep(500.millis)) ++ ZStream(4, 5) ++
  ZStream.fromZIO(ZIO.sleep(10.millis)) ++
  ZStream(6)).debounce(100.millis)
// 출력: 3, 6
```

### 7.22 집계(Aggregation)

싱크/트랜스듀서를 사용해 타입 `A`의 요소 하나 이상을 타입 `B`의 요소로 변환합니다.

**동기 집계(Synchronous Aggregation)** — `aggregate` / `transduce`. 트랜스듀서가 방출할 때 업스트림도 방출합니다.

```scala
val stream = ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
val s1 = stream.transduce(ZSink.collectAllN[Int](3))
// 출력: Chunk(1,2,3), Chunk(4,5,6), Chunk(7,8,9), Chunk(10)

val source = ZStream
  .iterate(1)(_ + 1)
  .take(200)
  .tap(x =>
    printLine(s"Producing Element $x")
      .schedule(Schedule.duration(1.second).jittered)
  )
val sink = ZSink.foreach((e: Chunk[Int]) =>
  printLine(s"Processing batch of events: $e")
    .schedule(Schedule.duration(3.seconds).jittered)
)
val myApp = source.transduce(ZSink.collectAllN[Int](5)).run(sink)
```

**비동기 집계(Asynchronous Aggregation)** — `aggregateAsync`, `aggregateAsyncWithin`, `aggregateAsyncWithinEither`. 다운스트림이 바쁜 동안 업스트림을 집계합니다.

```scala
abstract class ZStream[-R, +E, +O] {
  final def aggregateAsyncWithin[R1 <: R, E1 >: E, E2, A1 >: A, B](
    sink: ZSink[R1, E1, A1, E2, A1, B],
    schedule: Schedule[R1, Option[B], Any]
  )(implicit trace: Trace): ZStream[R1, E2, B]
}
```
```scala
val myApp = source.aggregateAsync(ZSink.collectAllN[Int](5)).run(sink)

dataStream.aggregateAsyncWithin(
  ZSink.collectAllN[Record](2000),
  Schedule.fixed(30.seconds)
)

val schedule: Schedule[Any, Option[Chunk[Record]], Long] =
  Schedule.fixed(30.seconds)
    .whileInput[Option[Chunk[Record]]](
      _.getOrElse(Chunk.empty).length < 100) andThen
  Schedule.fixed(5.seconds).jittered
    .whileInput[Option[Chunk[Record]]](
      _.getOrElse(Chunk.empty).length >= 1000)

dataStream
  .aggregateAsyncWithin(ZSink.collectAllN[Record](2000), schedule)
```

---

## 8. 동시성과 병렬성, 백프레셔(Concurrency, Parallelism and Back-Pressure)

ZIO 스트림은 안전한 동시성 연산자를 제공합니다.

- **병렬 매핑**: `mapZIOPar(n)`(순서 보존), `mapZIOParUnordered(n)`(순서 무시, 더 빠름). `n`은 동시 실행 이펙트 수입니다.
- **병렬 평탄화**: `flatMapPar`로 여러 하위 스트림을 동시에 실행합니다.
- **병합과 분배**: `merge`/`mergeAll`(비결정적 결합), `broadcast`(다수에게 복제), `distributedWith`(큐로 분배)로 동시 처리를 합니다.

### 백프레셔(Back-Pressure)

ZIO 스트림의 백프레셔는 **풀 기반(pull-based)** 메커니즘에 기반합니다. 소비자가 데이터 파이프라인의 끝에서 필요할 때 요소를 당겨오기 때문에, 느린 소비자가 빠른 생산자를 자연스럽게 늦춥니다. 따라서 별도의 신호 프로토콜 없이 메모리 폭주를 방지합니다.

빠른 생산자와 느린 소비자를 분리하려면 [버퍼링](#720-버퍼링buffering)을 사용해 둘 사이에 큐를 둡니다. 버퍼가 가득 차면 생산자가 자연스럽게 블로킹(논블로킹 방식으로 대기)됩니다. `bufferSliding`/`bufferDropping`을 쓰면 백프레셔 대신 오래된/새 요소를 버리는 정책을 선택할 수 있습니다.

### 리소스 안전(Resource Safety)

ZIO 스트림은 타임아웃, 인터럽트, 에러가 발생해도 리소스를 누수하지 않습니다. `Scope` 기반 자원 관리(`acquireReleaseWith`, `*Scoped` 생성자, `finalizer`, `ensuring`)를 통해 모든 종료 경로에서 종료 처리(finalizer)가 실행되도록 보장합니다.

---

## 9. 스트림 소비하기(Consuming Streams)

스트림을 소비하는 방법은 세 가지입니다: 싱크 사용, 폴드 사용, foreach 사용.

```scala
import zio._
import zio.Console._
import zio.stream._

// foreach: 각 요소에 대해 이펙트 실행
val result: Task[Unit] = ZStream.fromIterable(0 to 100).foreach(printLine(_))

// 싱크로 소비: ZStream#run에 ZSink을 전달
val sum: UIO[Int] = ZStream(1,2,3).run(ZSink.sum)

// fold로 소비
val s1: ZIO[Any, Nothing, Int] = ZStream(1, 2, 3, 4, 5).runFold(0)(_ + _)
val s2: ZIO[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1).runFoldWhile(0)(_ <= 5)(_ + _)
```

- `run(sink)`: 싱크를 사용해 스트림을 실행하고 `ZIO[R, E, Z]`를 반환
- `runFold(z)(op)`: 단일 값으로 폴드
- `runFoldWhile(z)(cont)(op)`: 술어가 참인 동안 폴드
- `foreach(f)`: 각 요소를 콜백 `O => ZIO[R1, E1, Any]`에 전달

이 외에도 ZIO API에는 `runCollect`(모든 요소를 Chunk로 수집), `runDrain`, `runForeach`, `runCount`, `runHead`, `runLast`, `runSum`, `toIterator` 등 다양한 소비 연산자가 있습니다(`runCollect`는 SubscriptionRef 예제에서 사용됨).

---

## 10. 에러 처리(Error Handling)

`ZStream[R, E, O]`는 타입이 지정된 실패(typed failure)를 `E` 채널에 담습니다. 실패는 스트림을 단락(short-circuit)시키며, 그 시점까지의 요소가 방출된 뒤 복구 로직이 작동합니다. 디펙트(defect)는 `E`를 우회하므로 `*Cause` 계열이나 `refineOrDie`로만 잡을 수 있습니다.

### 10.1 실패로부터 복구(Recovering from Failure)

`orElse`는 초기 스트림이 실패하면 대체 스트림으로 전환합니다.

```scala
import zio.stream._
val s1 = ZStream(1, 2, 3) ++ ZStream.fail("Oh! Error!") ++ ZStream(4, 5)
val s2 = ZStream(6, 7, 8)
val stream = s1.orElse(s2)
// 출력: 1, 2, 3, 6, 7, 8
```

`catchAll`은 실패 값에 따라 복구 스트림을 결정합니다.

```scala
val first = ZStream(1, 2, 3) ++ ZStream.fail("Uh Oh!") ++ ZStream(4, 5) ++ ZStream.fail("Ouch")
val second = ZStream(6, 7, 8)
val third = ZStream(9, 10, 11)
val stream = first.catchAll {
  case "Uh Oh!" => second
  case "Ouch"   => third
}
// 출력: 1, 2, 3, 6, 7, 8
```

> **전파에 대한 주의**: 스트림은 첫 번째 실패에서 멈춥니다. 위 예제에서 `"Uh Oh!"` 이전의 `1, 2, 3`만 방출된 뒤 `second`(`6, 7, 8`)로 전환되며, 원본 스트림은 이미 첫 실패에서 종료되었으므로 `4, 5`와 `"Ouch"` 분기에는 결코 도달하지 않습니다.

`orElseEither`는 동일하게 동작하되 `Either`로 요소를 태그합니다(`Left`는 첫 스트림, `Right`는 복구 스트림).

### 10.2 디펙트로부터 복구(Recovering from Defects)

`catchAllCause`는 디펙트(die)를 포함한 모든 실패 원인으로부터 복구합니다.

```scala
val s1 = ZStream(1, 2, 3) ++ ZStream.dieMessage("Oh! Boom!") ++ ZStream(4, 5)
val s2 = ZStream(7, 8, 9)
val stream = s1.catchAllCause(_ => s2)
// 출력: 1, 2, 3, 7, 8, 9
```

### 10.3 일부 에러로부터 복구(Recovery from Some Errors)

`catchSome`은 특정 실패로부터(에러에 대한 부분 함수), `catchSomeCause`는 특정 원인으로부터(`Cause`에 대한 부분 함수) 복구합니다.

```scala
val s1 = ZStream(1, 2, 3) ++ ZStream.fail("Oh! Error!") ++ ZStream(4, 5)
val s2 = ZStream(7, 8, 9)
val stream = s1.catchSome { case "Oh! Error!" => s2 }
// 출력: 1, 2, 3, 7, 8, 9
```
```scala
import zio._
import zio.Cause._
import zio.stream._
val s1 = ZStream(1, 2, 3) ++ ZStream.dieMessage("Oh! Boom!") ++ ZStream(4, 5)
val s2 = ZStream(7, 8, 9)
val stream = s1.catchSomeCause { case Die(value, _) => s2 }
```

### 10.4 ZIO 이펙트로 복구(Recovering to ZIO Effect)

`onError`는 스트림이 실패할 때 정리(cleanup) ZIO 이펙트를 실행합니다.

```scala
import zio._
import zio.stream._
val stream =
   (ZStream(1, 2, 3) ++ ZStream.dieMessage("Oh! Boom!") ++ ZStream(4, 5))
    .onError(_ => Console.printLine("Stream application closed! We are doing some cleanup jobs.").orDie)
```

### 10.5 실패하는 스트림 재시도(Retry a Failing Stream)

`retry`는 `Schedule`에 따라 스트림을 재시도합니다.

```scala
val numbers = ZStream(1, 2, 3) ++
   ZStream
    .fromZIO(
      Console.print("Enter a number: ") *> Console.readLine
        .flatMap(x =>
          x.toIntOption match {
            case Some(value) => ZIO.succeed(value)
            case None        => ZIO.fail("NaN")
          }
        )
    )
    .retry(Schedule.exponential(1.second))
```

### 10.6 Either와의 변환(From/To Either)

- `ZStream.absolve`: `Either` 값들의 스트림을 에러 채널로 들어올립니다(레거시 `Either` 반환 API 적응에 유용).
- `ZStream#either`: 에러를 값 채널의 `Either`로 노출(에러 채널은 `Nothing`이 됨).
- `ZStream#rightOrFail`: `Either` 스트림을 right 값 스트림으로 변환하고, 첫 `Left`에서 지정 에러로 실패.

```scala
def legacyFetchUrlAPI(url: URL): Either[Throwable, String] = ???
def fetchUrl(url: URL): ZStream[Any, Throwable, String] =
   ZStream.fromZIO(
    ZIO.attemptBlocking(legacyFetchUrlAPI(url))
  ).absolve

val inputs: ZStream[Any, Nothing, Either[IOException, String]] =
   ZStream.fromZIO(Console.readLine).either

val eitherStream: ZStream[Any, Nothing, Either[String, Int]] =
  ZStream(Right(1), Right(2), Left("failed to parse"), Right(4))
val stream: ZStream[Any, String, Int] = eitherStream.rightOrFail("fail")
```

### 10.7 에러 정제(Refining Errors)

`refineOrDie`는 부분 함수에 매칭되는 에러는 타입 에러 채널 `E`에 유지하고, 그 외의 에러는 디펙트로 만들어 파이버를 종료시킵니다.

```scala
val stream: ZStream[Any, Throwable, Int] = ZStream.fail(new Throwable)
val res: ZStream[Any, IllegalArgumentException, Int] =
  stream.refineOrDie { case e: IllegalArgumentException => e }
```

### 10.8 타임아웃(Timing Out)

- `timeoutFail` / `timeoutFailCause`: 지정 시간 안에 값을 생산하지 못하면 에러/원인으로 실패.
- `timeoutTo`: 지정 시간 안에 값을 생산하지 못하면 대체 스트림으로 전환.

```scala
stream.timeoutFail(new TimeoutException)(10.seconds)

val alternative = ZStream.fromZIO(ZIO.attempt(???))
stream.timeoutTo(10.seconds)(alternative)
```

---

## 11. 스케줄링(Scheduling)

스트림의 출력을 스케줄링하려면 `ZStream#schedule` 콤비네이터를 사용합니다. 다음은 각 방출 사이에 간격을 두는 예제입니다.

```scala
import zio._
import zio.stream._
val stream = ZStream(1, 2, 3, 4, 5).schedule(Schedule.spaced(1.second))
```

이 외에도 ZIO에는 `repeat`, `throttleShape`, `throttleEnforce`, `Schedule.fixed` 등 스케줄/스로틀 관련 연산자가 있으며, 위 예제들에서 `Schedule.spaced`, `Schedule.fixed`, `Schedule.exponential`, `Schedule.duration(...).jittered` 등이 함께 사용됩니다.

---

## 12. ZSink: 소비자와 집계(ZSink: Consumer and Aggregation)

### 12.1 정의(Definition)

`ZSink[R, E, I, L, Z]`는 `ZStream`이 생산한 요소를 소비하는 데 사용됩니다. 싱크는 0개, 1개, 또는 그 이상의 `I` 요소를 소비하고, 타입 `E`의 에러로 실패할 수 있으며, 최종적으로 타입 `L`의 잔여물(leftover)과 함께 타입 `Z`의 값을 산출하는 함수로 생각할 수 있습니다.

타입 파라미터:

- **R** — 필요한 서비스 / 환경
- **E** — 에러 타입
- **I** — 입력 요소 타입
- **L** — 잔여물(leftover) 타입(업스트림에서 소비했지만 사용하지 않은 요소)
- **Z** — 출력 / done 값 타입

스트림을 싱크로 소비하려면 `ZStream#run`에 싱크를 전달합니다.

```scala
import zio._
import zio.stream._

val stream = ZStream.fromIterable(1 to 1000)
val sink   = ZSink.sum[Int]
val sum    = stream.run(sink)
```

### 12.2 타입 별칭(Type Alias)

`Sink[E, A, L, B]`는 `ZSink[Any, E, A, L, B]`의 별칭으로, 서비스를 요구하지 않는 싱크입니다.

```scala
type Sink[+E, A, +L, +B] = ZSink[Any, E, A, L, B]
```

### 12.3 싱크 생성하기(Creating Sinks)

```scala
import zio._
import zio.stream._
import zio.Console._
import java.io.IOException
import java.nio.file.{Path, Paths}
```

#### 공통 생성자

```scala
// 첫 요소; 빈 스트림이면 None
val head: ZSink[Any, Nothing, Int, Int, Option[Int]] = ZSink.head[Int]
// ZStream(1, 2, 3, 4).run(head) => Some(1)

// 마지막 요소
val last: ZSink[Any, Nothing, Int, Nothing, Option[Int]] = ZSink.last[Int]
// ZStream(1, 2, 3, 4).run(last) => Some(4)

// 요소 개수 세기
val count: ZSink[Any, Nothing, Int, Nothing, Long] = ZSink.count
// ZStream(1, 2, 3, 4, 5).run(count) => 5

// 수치 합산
val sum: ZSink[Any, Nothing, Int, Nothing, Int] = ZSink.sum[Int]
// ZStream(1, 2, 3, 4, 5).run(sum) => 15

// 지정 개수만큼 가져와 Chunk로
val take: ZSink[Any, Nothing, Int, Int, Chunk[Int]] = ZSink.take[Int](3)
// ZStream(1, 2, 3, 4, 5).run(take) => Chunk(1, 2, 3)

// 입력 무시
val drain: ZSink[Any, Nothing, Any, Nothing, Unit] = ZSink.drain

// 실행 시간 측정
val timed: ZSink[Any, Nothing, Any, Nothing, Duration] = ZSink.timed
val timedStream: ZIO[Any, Nothing, Long] =
  ZStream(1, 2, 3, 4, 5).schedule(Schedule.fixed(2.seconds)).run(timed).map(_.getSeconds)
// => 10

// 각 요소에 이펙트 실행
val printer: ZSink[Any, IOException, Int, Int, Unit] =
  ZSink.foreach((i: Int) => printLine(i))
```

#### 성공과 실패로부터

```scala
val succeed: ZSink[Any, Any, Any, Nothing, Int]       = ZSink.succeed(5)
val failed : ZSink[Any, String, Any, Nothing, Nothing] = ZSink.fail("fail!")
```

#### 수집하기(Collecting)

```scala
// 모든 요소를 Chunk로
val collection: UIO[Chunk[Int]] = ZStream(1, 2, 3, 4, 5).run(ZSink.collectAll[Int])
// Chunk(1, 2, 3, 4, 5)

// Set으로
val toSet = ZSink.collectAllToSet[Int]
// ZStream(1, 3, 2, 3, 1, 5, 1).run(toSet) => Set(1, 3, 2, 5)

// Map으로 (키 함수 + 병합 함수)
val toMap = ZSink.collectAllToMap((_: Int) % 3)(_ + _)
// ZStream(1, 3, 2, 3, 1, 5, 1).run(toMap) => Map(1 -> 3, 0 -> 6, 2 -> 7)

// 최대 n개씩 Chunk로
ZStream(1, 2, 3, 4, 5).run(ZSink.collectAllN(3))
// Chunk(1,2,3), Chunk(4,5)

// 술어가 참인 동안 Chunk로 누적
ZStream(1, 2, 0, 4, 0, 6, 7).run(ZSink.collectAllWhile(_ != 0))
// Chunk(1,2), Chunk(4), Chunk(6,7)

// 최대 n개 키의 Map으로
ZStream(1, 2, 0, 4, 5).run(ZSink.collectAllToMapN[Nothing, Int, Int](10)(_ % 3)(_ + _))
// Map(1 -> 5, 2 -> 7, 0 -> 0)

// 최대 n개의 Set으로
ZStream(1, 2, 1, 2, 1, 3, 0, 5, 0, 2).run(ZSink.collectAllToSetN(3))
// Set(1,2,3), Set(0,5,2), Set(1)
```

#### 폴딩(Folding)

```scala
// 기본 폴드 누적
ZSink.foldLeft[Int, Int](0)(_ + _)

// 단락(short-circuit) 폴드: 종료 술어가 폴딩의 끝을 결정
ZStream.iterate(0)(_ + 1).run(
  ZSink.fold(0)(sum => sum <= 10)((acc, n: Int) => acc + n)
)
// 15

// foldLeft: 스트림이 끝날 때까지 폴드, 하나의 결과
ZStream(1, 2, 3, 4).run(ZSink.foldLeft[Int, Int](0)(_ + _))
// 10

// foldUntil: max개를 폴드할 때까지
ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).run(ZSink.foldUntil(0, 3)(_ + _))
// 6, 15, 24, 10
```

`foldWeighted`는 `costFn`이 정한 `max` 만큼의 가중치에 도달할 때까지 폴드한 뒤, 계산된 값을 방출하고 재시작합니다.

```scala
object ZSink {
  def foldWeighted[In, S](z: => S)(costFn: (S, In) => Long, max: Long)(
    f: (S, In) => S
  ): ZSink[Any, Nothing, In, In, S] = ???
}
```
```scala
// 각 요소 가중치 1, 3개마다 재시작 => 3개씩 묶음
ZStream(3, 2, 4, 1, 5, 6, 2, 1, 3, 5, 6)
  .transduce(
    ZSink.foldWeighted(Chunk[Int]())((_, _: Int) => 1, 3) { (acc, el) => acc ++ Chunk(el) }
  )
// Chunk(3,2,4),Chunk(1,5,6),Chunk(2,1,3),Chunk(5,6)

// 합이 max 이하인 요소들을 묶음
ZStream(1, 2, 2, 4, 2, 1, 1, 1, 0, 2, 1, 2)
  .transduce(
    ZSink.foldWeighted(Chunk[Int]())((_, i: Int) => i.toLong, 5) { (acc, el) => acc ++ Chunk(el) }
  )
// Chunk(1,2,2),Chunk(4),Chunk(2,1,1,1,0),Chunk(2,1,2)
```

> **주의(caution)**: `ZSink.foldWeighted`는 가중치가 `max`보다 큰 단일 요소를 분해(decompose)할 수 없습니다. 개별 비용이 `max`보다 큰 요소는 파이프라인이 `max`를 넘어서게 만듭니다. 이런 요소를 분해하려면 `ZSink.foldWeightedDecompose`를 사용합니다.

```scala
object ZSink {
  def foldWeightedDecompose[Err, In, S](
     z: S
   )(costFn: (S, In) => Long, max: Long, decompose: In => Chunk[In])(
     f: (S, In) => S
   ): ZSink[Any, Err, In, Err, In, S] = ???
}
```
```scala
ZStream(1, 2, 2, 2, 1, 6, 1, 7, 2, 1, 2)
  .transduce(
    ZSink.foldWeightedDecompose(Chunk[Int]())(
      (_, i: Int) => i.toLong, 5,
      (i: Int) => if (i > 5) Chunk(i - 1, 1) else Chunk(i)
    )((acc, el) => acc ++ Chunk.succeed(el))
  )
// Chunk(1,2,2),Chunk(2,1),Chunk(5),Chunk(1,1),Chunk(5),Chunk(1,1,2,1),Chunk(2)
```

#### ZIO / File / OutputStream / Queue / Hub로부터

```scala
// ZIO 워크플로로부터 단일 값 싱크
val sink = ZSink.fromZIO(ZIO.succeed(1))

// 파일로: 바이트 청크를 소비해 파일에 기록
def fileSink(path: Path): ZSink[Any, Throwable, String, Byte, Long] =
  ZSink.fromPath(path).contramapChunks[String](_.flatMap(_.getBytes))
val result = ZStream("Hello", "ZIO", "World!").intersperse("\n").run(fileSink(Paths.get("file.txt")))

// OutputStream으로
ZStream("Application", "Error", "Logs")
  .intersperse("\n")
  .run(ZSink.fromOutputStream(java.lang.System.err).contramapChunks[String](_.flatMap(_.getBytes)))

// Queue로: 각 요소를 큐에 넣음
val queueApp: IO[IOException, Unit] =
  for {
    queue    <- Queue.bounded[Int](32)
    producer <- ZStream.iterate(1)(_ + 1).schedule(Schedule.fixed(200.millis))
                  .run(ZSink.fromQueue(queue)).fork
    consumer <- queue.take.flatMap(printLine(_)).forever
    _        <- producer.zip(consumer).join
  } yield ()
```

### 12.4 싱크 연산(Sink Operations)

#### contramap

`map`은 함수의 공역(co-domain)을, `contramap`은 정의역(domain)을 변환합니다. 즉 `contramap`은 입력을 변환합니다. 예를 들어 `ZSink.sum`은 수치 입력을 합산하는데, `String` 스트림에 적용하고 싶을 때 사용합니다.

```scala
import zio._
import zio.stream._

val numericSum: ZSink[Any, Nothing, Int, Nothing, Int]    = ZSink.sum[Int]
val stringSum : ZSink[Any, Nothing, String, Nothing, Int] = numericSum.contramap((x: String) => x.toInt)
val sum: ZIO[Any, Nothing, Int] = ZStream("1", "2", "3", "4", "5").run(stringSum)
// 15
```

#### dimap

`dimap`은 입력과 출력을 모두 변환하는 확장된 `contramap`입니다.

```scala
val sumSink: ZSink[Any, Nothing, String, Nothing, String] =
  numericSum.dimap[String, String](_.toInt, _.toString)
val sum: ZIO[Any, Nothing, String] = ZStream("1", "2", "3", "4", "5").run(sumSink)
// "15"
```

#### filterInput (필터링)

싱크는 입력 요소를 필터링하는 `filterInput`을 제공합니다.

```scala
ZStream(1, -2, 0, 1, 3, -3, 4, 2, 0, 1, -3, 1, 1, 6)
  .transduce(ZSink.collectAllN[Int](3).filterInput[Int](_ > 0))
// Chunk(Chunk(1,1,3),Chunk(4,2,1),Chunk(1,1,6),Chunk())
```

### 12.5 싱크의 동시성과 병렬성(Concurrency and Parallelism)

#### 병렬 지핑(Parallel Zipping)

두 `ZSink`을 함께 지핑할 수 있습니다. 둘 다 병렬로 실행되며 결과는 튜플로 결합됩니다.

```scala
import zio._
import zio.stream._

case class Record()
val kafkaSink: ZSink[Any, Throwable, Record, Record, Unit] =
  ZSink.foreach[Any, Throwable, Record](record => ZIO.attempt(???))
val pulsarSink: ZSink[Any, Throwable, Record, Record, Unit] =
  ZSink.foreach[Any, Throwable, Record](record => ZIO.attempt(???))

val zipped: ZSink[Any, Throwable, Record, Record, Unit] = kafkaSink zipPar pulsarSink
```

#### 레이싱(Racing)

여러 싱크를 `race`할 수 있습니다. 병렬로 실행되어 먼저 완료된 쪽이 결과를 제공합니다. 어느 쪽이 이겼는지 알려면 `raceBoth`(`Either` 결과 반환)를 씁니다.

```scala
val raced: ZSink[Any, Throwable, Record, Record, Unit] = kafkaSink race pulsarSink
```

### 12.6 잔여물(Leftovers)

싱크는 업스트림에서 0개 이상의 `I` 요소를 소비합니다. 업스트림이 유한하면 `ZSink#collectLeftover`로 잔여물을 수집할 수 있습니다. 싱크의 결과와 잔여물의 튜플을 반환합니다.

```scala
val s1: ZIO[Any, Nothing, (Chunk[Int], Chunk[Int])] =
  ZStream(1, 2, 3, 4, 5).run(ZSink.take(3).collectLeftover)
// (Chunk(1, 2, 3), Chunk(4, 5))

val s2: ZIO[Any, Nothing, (Option[Int], Chunk[Int])] =
  ZStream(1, 2, 3, 4, 5).run(ZSink.head[Int].collectLeftover)
// (Some(1), Chunk(2, 3, 4, 5))
```

잔여물이 필요 없으면 `ZSink#ignoreLeftover`로 버립니다.

```scala
ZSink.take[Int](3).ignoreLeftover
```

> **참고**: `ZSink#transduce`(스트림 쪽 연산자)는 싱크를 스트림 전반에 걸쳐 반복 적용하여, 싱크가 완료될 때마다 출력 하나를 방출합니다. 집계/배칭에 활용됩니다.

---

## 13. ZPipeline: 스트림 변환기(ZPipeline: Stream Transformer)

### 13.1 정의(Definition)

`ZPipeline[-Env, +Err, -In, +Out]`은 **스트림 변환기**입니다. 스트림을 입력으로 받아 변환된 스트림을 출력으로 반환합니다. 개념적 타입 별칭은 다음과 같습니다.

```scala
type ZPipeline[Env, Err, In, Out] = ZStream[Env, Err, In] => ZStream[Env, Err, Out]
```

타입 파라미터:

- `Env` — 파이프라인을 실행하는 데 필요한 환경
- `Err` — 파이프라인이 실패할 수 있는 에러 타입
- `In` — 입력 스트림 요소 타입
- `Out` — 출력 스트림 요소 타입

**핵심 개념**: "파이프라인은 스트림 변환을 소스 스트림 자체로부터 분리합니다. 그 결과 값 수준에서 스트림 변환을 추상화할 수 있게 되어, 재사용 가능한 변환 파이프라인을 만들고, 저장하고, 전달하며, 다양한 스트림에 적용할 수 있습니다." 파이프라인이 하는 모든 일은 스트림에 직접 할 수도 있지만, 파이프라인은 변환 로직을 합성 가능한 값으로 **추상화·재사용**할 수 있게 합니다.

### 13.2 생성(Creation)

#### 함수로부터(From Function)

`ZPipeline.map`으로 함수를 파이프라인으로 변환합니다. 이펙트를 포함하는 버전으로 `ZPipeline.mapZIO`도 있습니다.

```scala
val chars = ZPipeline.map[String, Chunk[Char]](s => Chunk.fromArray(s.toArray)) >>>
  ZPipeline.mapChunks[Chunk[Char], Char](_.flatten)
```

#### 커스텀 채널로부터(From Custom Channels)

`map`이나 `mapZIO`로 표현하기 어려운 상태 유지(stateful) 변환에는 `ZPipeline.fromChannel`을 사용합니다. 다음은 각 요소를 직전 요소와 짝지어 청크를 넘나들며 `prev` 상태를 유지하는 예제입니다.

```scala
import zio.{ZNothing, Cause}
import zio.stream.ZChannel

def pairwise[A]: ZPipeline[Any, Nothing, A, (A, A)] =
  ZPipeline.fromChannel(pairwiseGo[A](None))

def pairwiseGo[A](
  prev: Option[A]
): ZChannel[Any, ZNothing, Chunk[A], Any, ZNothing, Chunk[(A, A)], Any] =
  ZChannel.readWithCause(
    (in: Chunk[A]) => {
      val buf  = Chunk.newBuilder[(A, A)]
      var last = prev
      in.foreach { a =>
        last.foreach(p => buf += ((p, a)))
        last = Some(a)
      }
      val out = buf.result()
      (if (out.nonEmpty) ZChannel.write(out) else ZChannel.unit) *> pairwiseGo(last)
    },
    (err: Cause[ZNothing]) => ZChannel.refailCause(err),
    (_: Any) => ZChannel.unit
  )
```

`ZChannel.readWithCause`는 세 가지 케이스를 받습니다: 입력 핸들러, 에러/원인 핸들러, done 핸들러.

### 13.3 내장 파이프라인(Built-in Pipelines)

#### Identity

```scala
ZStream(1,2,3).via(ZPipeline.identity[Int])
// 출력: 1, 2, 3
```

#### 분할(Splitting)

```scala
// 구분자로 문자열 분할
ZStream("5-6-7-8", "-9-10-1", "1-12-13")
  .via(ZPipeline.splitOn("-"))
  .map(_.toInt)
// 출력: 5, 6, 7, 8, 9, 10, 11, 12, 13

// 개행으로 분할 (Windows \r\n, UNIX \n 모두 처리)
ZStream("This is the first line.\nSecond line.\nAnd the last line.")
  .via(ZPipeline.splitLines)
// 출력: "This is the first line.", "Second line.", "And the last line."

// 청크 구분자로 분할
ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
  .via(ZPipeline.splitOnChunk(Chunk(4, 5, 6)))
// 출력: Chunk(1, 2, 3), Chunk(7, 8, 9, 10)
```

#### 드롭(Dropping) / 앞에 붙이기(Prepending)

```scala
// 술어가 참인 동안 버리다가, 실패하는 순간부터 소비 시작
ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
  .via(ZPipeline.dropWhile((x: Int) => x <= 5))
// 출력: 6, 7, 8, 9, 10

// 다른 값보다 먼저 지정 청크를 방출
ZStream(2, 3, 4).via(ZPipeline.prepend(Chunk(0, 1)))
// 출력: 0, 1, 2, 3, 4
```

#### 압축(Compression)

```scala
// deflate (RFC 1951)
import zio.stream.ZStream
import zio.stream.ZPipeline.deflate
import zio.stream.compression.{CompressionLevel, CompressionStrategy, FlushMode}

def compressWithDeflate(clearText: ZStream[Any, Nothing, Byte]): ZStream[Any, Nothing, Byte] = {
  val bufferSize: Int = 64 * 1024
  val noWrap: Boolean = false
  val level: CompressionLevel = CompressionLevel.DefaultCompression
  val strategy: CompressionStrategy = CompressionStrategy.DefaultStrategy
  val flushMode: FlushMode = FlushMode.NoFlush
  clearText.via(deflate(bufferSize, noWrap, level, strategy, flushMode))
}

def deflateWithDefaultParameters(clearText: ZStream[Any, Nothing, Byte]): ZStream[Any, Nothing, Byte] =
  clearText.via(deflate())

// gzip
import zio.stream.compression._
ZStream
  .fromFileName("file.txt")
  .via(
    ZPipeline.gzip(
      bufferSize = 64 * 1024,
      level = CompressionLevel.DefaultCompression,
      strategy = CompressionStrategy.DefaultStrategy,
      flushMode = FlushMode.NoFlush
    )
  )
  .run(ZSink.fromFileName("file.gz"))
```

#### 압축 해제(Decompression)

```scala
import zio.stream.ZStream
import zio.stream.ZPipeline.{ gunzip, inflate }
import zio.stream.compression.CompressionException

// inflate (RFC 1951)
def decompressDeflated(deflated: ZStream[Any, Nothing, Byte]): ZStream[Any, CompressionException, Byte] = {
  val bufferSize: Int = 64 * 1024
  val noWrap: Boolean = false
  deflated.via(inflate(bufferSize, noWrap))
}

// gunzip (RFC 1952)
def decompressGzipped(gzipped: ZStream[Any, Nothing, Byte]): ZStream[Any, CompressionException, Byte] = {
  val bufferSize: Int = 64 * 1024
  gzipped.via(gunzip(bufferSize))
}
```

`ZPipeline.gunzipAuto`는 입력이 gzip이면 압축을 풀고, 아니면 그대로 다운스트림으로 통과시킵니다.

```scala
import zio.stream.ZPipeline.gunzipAuto
def decompressMaybeGzipped(maybeGzipped: ZStream[Any, Nothing, Byte]): ZStream[Any, CompressionException, Byte] = {
  val bufferSize: Int = 64 * 1024
  maybeGzipped.via(gunzipAuto(bufferSize))
}
```

#### 디코더(Decoders)

바이트를 문자열로 디코딩하는 파이프라인 계열입니다.

| 디코더 | 입력 | 출력 |
|---------|-------|--------|
| `ZPipeline.utfDecode` | Unicode 바이트 | String |
| `ZPipeline.utf8Decode` | UTF-8 바이트 | String |
| `ZPipeline.utf16Decode` | UTF-16 | String |
| `ZPipeline.utf16BEDecode` | UTF-16BE 바이트 | String |
| `ZPipeline.utf16LEDecode` | UTF-16LE 바이트 | String |
| `ZPipeline.utf32Decode` | UTF-32 바이트 | String |
| `ZPipeline.utf32BEDecode` | UTF-32BE 바이트 | String |
| `ZPipeline.utf32LEDecode` | UTF-32LE 바이트 | String |
| `ZPipeline.usASCIIDecode` | US-ASCII 바이트 | String |

```scala
val lines: ZStream[Any, Throwable, String] =
  ZStream
    .fromFileName("file.txt")
    .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
```

### 13.4 연산(Operations)

#### 출력 변환(Output Transformation, Mapping)

성공 채널 출력은 `ZPipeline#map`으로, 실패 채널은 `ZPipeline#mapError`로 변환합니다. `ZPipeline.mapChunks`는 `Chunk[O] => Chunk[O2]` 함수를 받아 방출되는 청크를 변환합니다.

#### 입력 변환(Input Transformation, Contramap)

입력을 변환하려면 `ZPipeline#contramap`을 사용합니다. `J => I` 매핑 함수를 받아 `ZPipeline[R, E, I, O]`를 `ZPipeline[R, E, J, O]`로 바꿉니다.

```scala
class ZPipeline[-R, +E, -I, +O] {
  final def contramap[J](f: J => I): ZPipeline[R, E, J, O] = ???
}
```
```scala
val numbers: ZStream[Any, Nothing, Int] = ZStream("1-2-3-4-5")
   .mapConcat(_.split("-"))
   .via(ZPipeline.map[String, Int](_.toInt))
```

#### 합성(Composing)

파이프라인은 **다른 파이프라인**과 `>>>`로 합성되어 새 파이프라인을 만듭니다.

```scala
val lines: ZStream[Any, Throwable, String] =
  ZStream
    .fromFileName("file.txt")
    .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
```

파이프라인은 `.via(pipeline)`로 **스트림에 부착**됩니다. 또한 파이프라인은 `>>>`로 **싱크와 합성**되어 결합된 싱크를 만들고, 스트림도 `>>>`로 싱크와 합성됩니다.

```scala
import java.nio.charset.CharacterCodingException
val refine: ZIO[Any, Throwable, Long] = {
  val stream: ZStream[Any, Throwable, Byte] = ZStream.fromFileName("file.txt")
  val pipeline: ZPipeline[Any, CharacterCodingException, Byte, String] =
    ZPipeline.utf8Decode >>> ZPipeline.splitLines >>> ZPipeline.filter[String](_.contains('₿'))
  val fileSink: ZSink[Any, Throwable, String, Byte, Long] = ZSink
    .fromFileName("file.refined.txt")
    .contramapChunks[String](
      _.flatMap(line => (line + System.lineSeparator()).getBytes())
    )
  val pipeSink: ZSink[Any, Throwable, Byte, Byte, Long] = pipeline >>> fileSink
  stream >>> pipeSink
}
```

---

## 14. SubscriptionRef: 구독 가능한 공유 상태(SubscriptionRef)

`SubscriptionRef[A]`는 현재 값과 이후의 모든 변경(change)을 구독(subscribe)할 수 있는 `Ref`입니다. `Ref.Synchronized[A]`를 확장하므로 표준 Ref 연산(`get`, `set`, `update`, `modify`, ...)을 지원하며, 추가로 `changes` 스트림을 제공합니다.

`changes` 스트림을 소비하면 현재 값과 이후의 모든 변경을 관찰할 수 있습니다. `changes`를 실행하는 각 소비자는 구독 시점의 현재 값을 먼저 받고, 이후의 모든 변경을 이어서 받습니다.

```scala
object SubscriptionRef {
  def make[A](a: A): UIO[SubscriptionRef[A]] = ???
}
```

### 사용 사례(Use Case)

"`SubscriptionRef`는 하나 이상의 관찰자(observer)가 공유 상태의 모든 변경에 대해 어떤 동작을 수행해야 하는 공유 상태를 모델링하는 데 매우 유용합니다." 함수형 리액티브 프로그래밍(FRP)이나 pub-sub 도메인(예: 애플리케이션 상태 변경에 반응해 갱신되는 UI 요소)에 적합합니다. 하나의 작성자(writer)가 공유 상태를 변경하고, 다수의 관찰자가 각자 `.changes`를 구독해 모든 갱신에 반응합니다.

```scala
// 서버: 공유 상태를 무한히 증가시킴
def server(ref: Ref[Long]): UIO[Nothing] =
  ref.update(_ + 1).forever

// 클라이언트: changes 스트림을 구독해 무작위 개수의 값을 수집
def client(changes: ZStream[Any, Nothing, Long]): UIO[Chunk[Long]] =
  for {
    n     <- Random.nextLongBetween(1, 200)
    chunk <- changes.take(n).runCollect
  } yield chunk

// 연결: 서버를 포크하고, 100개의 클라이언트를 병렬 실행한 뒤 서버를 인터럽트
for {
  subscriptionRef <- SubscriptionRef.make(0L)
  server          <- server(subscriptionRef).fork
  chunks          <- ZIO.collectAllPar(List.fill(100)(client(subscriptionRef.changes)))
  _               <- server.interrupt
  _               <- ZIO.foreach(chunks)(chunk => Console.printLine(chunk))
} yield ()
```

**참고**: `subscriptionRef.changes`는 `ZStream[Any, Nothing, A]`입니다. 스트림이므로 관찰자가 소비하기 전에 변환·필터링·다른 스트림과의 병합 등 일반 스트림 연산자로 합성할 수 있습니다. 또한 `SubscriptionRef[A] <: Ref[A]`이므로 `Ref`가 기대되는 곳에 그대로 전달할 수 있고, 관찰자는 별도로 `.changes`를 탭합니다.

---

## 15. ZChannel: 통합 기본 요소(ZChannel: The Unified Primitive)

`ZChannel`은 ZIO 스트리밍 라이브러리의 근본 추상화입니다. 타입 시그니처는 다음과 같습니다.

```scala
ZChannel[-Env, -InErr, -InElem, -InDone, +OutErr, +OutElem, +OutDone]
```

이는 환경을 요구하고, 실패할 수 있는 입력 요소를 읽으며, 역시 실패할 수 있는 출력 요소를 쓰는 양방향 통신 기본 요소(bidirectional communication primitive)를 나타냅니다.

ZChannel은 세 가지 스트리밍 타입의 바탕 구현입니다.

- **ZStream** — 출력 측(요소 방출)
- **ZSink** — 입력 측(요소 소비)
- **ZPipeline** — 양측(입력을 출력으로 변환)

```scala
case class ZStream[-R, +E, +A](
  val channel: ZChannel[R, Any, Any, Any, E, Chunk[A], Any])
case class ZSink[-R, +E, -In, +L, +Z](
  val channel: ZChannel[R, ZNothing, Chunk[In], Any, E, Chunk[L], Z])
case class ZPipeline[-R, +E, -In, +Out](
  val channel: ZChannel[R, ZNothing, Chunk[In], Any, E, Chunk[Out], Any])
```

ZChannel은 강력한 저수준 제어를 제공하지만, 대부분의 사용자는 채널을 직접 다루기보다 위의 고수준 추상화를 사용합니다.

> **버전 참고**: 본 문서는 현대 ZIO 2.x API(`ZStream.fromZIO`, `acquireReleaseWith`, `Scope` 기반 자원 관리, `mapZIOPar`)를 기준으로 합니다. 구버전(ZIO 1.x) 문서/튜토리얼에서는 `fromEffect`, `bracket`, `Managed`, `mapMPar` 같은 이름을 쓸 수 있습니다.

---

## 16. 참고 자료

- [Introduction to ZIO Streams](https://zio.dev/reference/stream/)
- [ZStream — Creating ZIO Streams](https://zio.dev/reference/stream/zstream/creating-zio-streams)
- [ZStream — Operations](https://zio.dev/reference/stream/zstream/operations)
- [ZStream — Streams Are Chunked by Default](https://zio.dev/reference/stream/zstream/streams-are-chunked-by-default)
- [ZStream — Resourceful Streams](https://zio.dev/reference/stream/zstream/resourceful-streams)
- [ZStream — Consuming Streams](https://zio.dev/reference/stream/zstream/consuming-streams)
- [ZStream — Error Handling](https://zio.dev/reference/stream/zstream/error-handling)
- [ZStream — Scheduling](https://zio.dev/reference/stream/zstream/scheduling)
- [ZSink — Introduction](https://zio.dev/reference/stream/zsink/)
- [ZSink — Creating Sinks](https://zio.dev/reference/stream/zsink/creating-sinks)
- [ZSink — Operations](https://zio.dev/reference/stream/zsink/operations)
- [ZSink — Concurrency and Parallelism](https://zio.dev/reference/stream/zsink/concurrency-and-parallelism)
- [ZSink — Leftovers](https://zio.dev/reference/stream/zsink/leftovers)
- [ZPipeline](https://zio.dev/reference/stream/zpipeline/)
- [SubscriptionRef](https://zio.dev/reference/stream/subscription-ref)
- [ZChannel](https://zio.dev/reference/stream/zchannel/)
- [Chunk](https://zio.dev/reference/stream/chunk)
