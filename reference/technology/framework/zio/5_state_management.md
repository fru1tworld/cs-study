# ZIO 상태 관리

## ZIO 상태 관리: Ref, FiberRef, ZState

> 원본: https://zio.dev/reference/state-management/

---

### 목차

1. [상태 관리 개요(State Management Overview)](#1-상태-관리-개요state-management-overview)
2. [재귀를 통한 상태 관리(State Management Using Recursion)](#2-재귀를-통한-상태-관리state-management-using-recursion)
3. [Ref를 통한 전역 공유 상태(Global Shared State Using Ref)](#3-ref를-통한-전역-공유-상태global-shared-state-using-ref)
4. [Ref 데이터 타입 상세(Ref in Depth)](#4-ref-데이터-타입-상세ref-in-depth)
5. [Ref.Synchronized: 효과적인 원자적 갱신(Ref.Synchronized)](#5-refsynchronized-효과적인-원자적-갱신refsynchronized)
6. [파이버 로컬 상태(Fiber-local State)](#6-파이버-로컬-상태fiber-local-state)
7. [FiberRef: 파이버 로컬 저장소(FiberRef)](#7-fiberref-파이버-로컬-저장소fiberref)
8. [ZState: 환경 기반 상태(ZState)](#8-zstate-환경-기반-상태zstate)
9. [참고 자료](#9-참고-자료)

---

### 1. 상태 관리 개요(State Management Overview)

- 프로그램 실행 도중 어떤 종류의 상태(state)를 추적해야 하는 경우가 흔함
  - 객체가 상태를 가지면 그 객체의 동작(behavior)이 시간이 지남에 따라 영향을 받음
- 예시
  - 카운터(Counter): 여러 엔드포인트(endpoint)를 가진 RESTful API에서 각 엔드포인트에 들어온 요청(request) 수를 추적하는 경우
  - 은행 계좌 잔액(Bank Account Balance): 각 은행 계좌가 잔액(balance)을 가지며 입금(deposit)·출금(withdraw)에 따라 시간이 지나며 값이 변함
  - 온도(Temperature): 방의 온도가 시간이 지남에 따라 변함
  - 리스트 길이(List length): 리스트의 항목들을 순회(iterate)할 때 지금까지 본 항목 수를 기록하는 중간 상태(intermediate state) 필요
- 명령형 프로그래밍(imperative programming)에서 상태를 저장하는 일반적인 방법 하나는 변수(variable) 사용
  - 변수의 값은 제자리에서(in place) 갱신 가능
  - 상태가 여러 구성 요소(component) 사이에서 공유될 때 버그(bug)를 유발할 수 있음 → 상태 추적을 위한 변수 사용은 피하는 편이 좋음
- 동시성(concurrency) 관점에서 함수형 프로그래밍(functional programming)이 상태를 유지하는 두 가지 일반적인 접근 방식
  1. 재귀(Recursion) — 새로운 상태를 다음 구성 요소로 전달(pass)함으로써 갱신
     - 상태를 유지하는 매우 쉬운 방법이나 동시성 환경에서는 사용 불가 (여러 파이버(fiber) 사이에서 상태 공유 불가)
  2. 동시성(Concurrent) — 전역(global) 상태 관리와 파이버 로컬(fiber-local) 상태 관리 두 가지 변형 존재
     - 전역 공유 상태(Global Shared State): ZIO의 `Ref` 데이터 타입은 가변 참조(mutable reference)에 대한 서술(description) → 생산자(producer)·소비자(consumer) 구성 요소처럼 여러 파이버 사이에서 상태 공유 가능
     - 파이버 로컬 상태(Fiber-local State): ZIO는 `FiberRef`와 `ZState` 두 데이터 타입 제공 → 동시성 환경에서 상태 유지에 사용 가능하며 각 파이버가 자기 자신의 상태를 가짐 → 다른 파이버와 공유되지 않음 → 서로의 상태를 덮어써서(clobber) 망가뜨리는 일을 방지
- 이 섹션에서 위 접근 방식들을 다룸

---

### 2. 재귀를 통한 상태 관리(State Management Using Recursion)

- 변수를 사용해 상태를 추적하는 것은 매우 흔한 패턴
  - 예: 리스트 길이 계산 시 중간 결과를 `count` 변수 안에 저장

```scala
def length[T](list: List[T]): Int = {
  var count = 0
  for (_ <- list) count += 1
  count
}
```

- 함수형 프로그래밍에서는 상태를 추적하기 위해 변수를 사용하는 것을 피하고 다른 기법(technique) 사용
- 일반적인 기법 하나는 새로운 상태를 다음 함수의 인자(argument)로 전달하는 것
  - 새 버전의 상태를 만든 다음 다음 함수에 전달 → 최종 상태(final state)에 도달할 때까지 반복

- 다음 코드가 있다고 가정

```scala
var state = 5
state = state + 1
state = state * 2
state = state * state

println(state)
// Output: 144
```

- 이를 일련의 변환(transformation)으로 다시 작성 가능 → 새로운 상태를 다음 함수로 전달

```scala
def foo(state: Int): Int = bar(state + 1)
def bar(state: Int): Int = baz(state * 2)
def baz(state: Int): Int = state * state

println(foo(5)) 
// Output: 144
```

- 주어진 상태에 변환을 여러 번 적용하려면 이 기법을 재귀 함수(recursive function)와 결합해 함수를 여러 번 호출
  - 예: 리스트의 길이를 반환하는 `length` 함수

```scala
def length[T](list: List[T]): Int = {
  var count = 0
  for (_ <- list) count += 1
  count
}
```

- 위 함수를 일련의 상태 변환(state transformation)으로 바꾸려면 먼저 무엇이 상태인지 진단 필요
  - 명백한 답: `count` 변수 → 리스트 길이를 추적하는 상태
  - 덜 명백한 상태: 리스트 순회(traversal) 도중 아직 처리되지 않은 리스트의 나머지(remainder)
- `count`와 `remainder`로 구성된 상태로 모델링

```scala
case class State[T](count: Int, remainder: List[T])
```

- 상태 변환 함수 `loop` 작성

```scala
case class State[T](count: Int, remainder: List[T])

def loop[T](state: State[T]): State[T] = {
  state.remainder match {
    case Nil => state
    case _ :: tail => loop(State(state.count + 1, tail))
  }
}
```

- `State(0, List("a", "b", "c", "d"))` 상태로 호출했을 때 일어나는 일련의 `loop` 호출

```scala
loop(State(0, List("a", "b", "c", "d")))
loop(State(1, List("b", "c", "d")))
loop(State(2, List("c", "d")))
loop(State(3, List("d")))
loop(State(4, List()))
// Output:
// State(4, List())
```

- `loop` 함수를 약간 수정해 `length` 함수 안에서 사용

```scala
def length[T](list: List[T]): Int = {
  def loop(list: List[T], count: Int): Int = {
    list match {
      case Nil => count
      case _ :: tail => loop(tail, count + 1)
    }
  }

  loop(list, 0)
}
```

- 부수 효과(side effect)가 있을 때도 동일한 패턴 사용 가능
  - 예: 사용자가 입력의 끝을 나타내는 "q" 명령을 입력할 때까지 입력에서 이름을 읽는 함수

```scala
import scala.io.StdIn._

def getNames: List[String] = {
  def getName() = readLine("Please enter a name or 'q' to exit: ")
  var names = List.empty[String]
  var input = getName()
  while (input != "q") {
    names = names appended input
    input = getName()
  }
  names
} 
```

- 이전 패턴 사용 → 변수 사용 필요 없음

```scala
import scala.io.StdIn._

def getNames: Seq[String] = {
  def loop(names: List[String]): List[String] = {
    val name = readLine("Please enter a name or 'q' to exit: ")
    if (name == "q") names else loop(names appended name)
  }
  loop(List.empty[String])
}
```

- 이전 해결책의 문제: `getName`이 참조 투명(referentially transparent)하지 않음
  - 부수 효과로부터 자유롭게 만들기 위해 어떤 효과적(effectual) 연산이든 서술할 수 있는 `ZIO` 사용 가능

```scala
import zio._

def inputNames: ZIO[Any, String, List[String]] = {
  def loop(names: List[String]): ZIO[Any, String, List[String]] = {
    Console.readLine("Please enter a name or `q` to exit: ").orDie.flatMap {
      case "q" =>
        ZIO.succeed(names)
      case name =>
        loop(names appended name)
    }
  }

  loop(List.empty[String])
}
```

- 재귀를 사용해 상태를 가진 계산(stateful computation)을 수행하는 방법을 살펴봄
  - 여러 파이버가 프로그램의 상태를 동시에 변경하려는 동시성 프로그램에는 부적합
  - 다음 절에서 동시성 프로그램에서의 상태를 가진 계산을 다룸

---

### 3. Ref를 통한 전역 공유 상태(Global Shared State Using Ref)

- `Ref`의 일반적인 사용 사례 중 하나: 애플리케이션의 상태를 관리하는 것, 특히 동시성 환경(concurrent environment)에서
  - 가변 참조에 대한 순수 함수적 서술(purely functional description of a mutable reference)인 `Ref` 데이터 타입 사용

> 참고: 이 절에서는 `Ref` 데이터 타입의 기본 사용법만 다룸. `Ref`에 대해, 특히 동시성 프로그래밍에서의 사용법을 더 자세히 알고 싶다면 동시성(Concurrency) 섹션의 `Ref` 페이지(아래 4절) 참고

- 앞서 재귀 함수를 사용해 애플리케이션의 상태를 관리하는 법을 살펴봤으나 이 접근 방식에는 다음과 같은 단점 존재
  - 여러 파이버 사이에서 상태를 공유할 수 없음
  - 애플리케이션 로직을 작성하는 것이 다소 번거로움 → 함수 매개변수(parameter)로 상태를 전달하는 방식이 어색함
- `Ref` 데이터 타입 덕분에 동시성이 필요하든 아니든 `Ref`를 사용해 애플리케이션의 상태를 쉽게 관리 가능

- 앞 절에서 효과적(effectful) 연산에 대해서도 상태 관리를 할 수 있음을 확인
  - 마지막으로 다뤘던 예시

```scala
import zio._

def inputNames: ZIO[Any, String, List[String]] = {
  def loop(names: List[String]): ZIO[Any, String, List[String]] = {
    Console.readLine("Please enter a name or `q` to exit: ").orDie.flatMap {
      case "q" =>
        ZIO.succeed(names)
      case name =>
        loop(names appended name)
    }
  }

  loop(List.empty[String])
}
```

- 이 코드는 더 간단한 `Ref` 타입을 사용해 다시 작성 가능

```scala
import zio._

def getNames: ZIO[Any, String, List[String]] =
  Ref.make(List.empty[String])
    .flatMap { ref =>
      Console
        .readLine("Please enter a name or 'q' to exit: ")
        .orDie
        .repeatWhileZIO {
          case "q" => ZIO.succeed(false)
          case name => ref.update(_ appended name).as(true)
        } *> ref.get
    }
```

- 위 코드가 하는 일
  - 먼저 초기 상태 값(빈 리스트)에 대한 가변 참조를 만듦
  - 사용자가 "q" 명령을 입력할 때까지 콘솔에서 반복해서 읽음
  - 마지막으로 참조의 값을 얻어서 반환

> 참고: `Ref` 데이터 타입의 모든 연산은 효과적(effectful)임 → `Ref`에서 읽거나 쓸 때 효과적 연산을 수행하는 것

- `Ref` 데이터 타입 사용법을 배웠으므로 이를 사용해 상태를 동시적으로 관리 가능
  - 예: 콘솔에서 읽는 동안 다른 출처(source)에서 상태를 갱신하려는 또 다른 파이버가 있는 경우

```scala
import zio._

def getNames: ZIO[Any, String, List[String]] =
  for {
    ref <- Ref.make(List.empty[String])
    f1 <- Console
      .readLine("Please enter a name or 'q' to exit: ")
      .orDie
      .repeatWhileZIO {
        case "q"  => ZIO.succeed(false)
        case name => ref.update(_ appended name).as(true)
      }.fork 
      f2 <- ZIO.foreachDiscard(Seq("John", "Jane", "Joe", "Tom")) { name =>
        ref.update(_ appended name) *> ZIO.sleep(1.second)
      }
      .fork
    _ <- f1.join
    _ <- f2.join
    v <- ref.get
  } yield v
```

#### 카운터 예제(Counter Example)

- `Ref` 데이터 타입을 사용해 카운터 작성

```scala
import zio._

case class Counter(value: Ref[Int]) {
  def inc: UIO[Unit] = value.update(_ + 1)
  def dec: UIO[Unit] = value.update(_ - 1)
  def get: UIO[Int] = value.get
}

object Counter {
  def make: UIO[Counter] = Ref.make(0).map(Counter(_))
}
```

- `Counter`의 사용 예시

```scala
import zio._

object MainApp extends ZIOAppDefault {
  def run =
    for {
      c <- Counter.make
      _ <- c.inc
      _ <- c.inc
      _ <- c.dec
      _ <- c.inc
      v <- c.get
      _ <- ZIO.debug(s"This counter has a value of $v.")
    } yield ()
}
```

- 이 카운터는 동시성 환경에서 사용 가능 (예: RESTful API에서 요청 수를 세는 데 활용)
  - 예시를 위해 카운터를 동시적으로 갱신

```scala
import zio._

object MainApp extends ZIOAppDefault {
  def run =
    for {
      c <- Counter.make
      _ <- c.inc <&> c.inc <&> c.dec <&> c.inc
      v <- c.get
      _ <- ZIO.debug(s"This counter has a value of $v.")
    } yield ()
}
```

---

### 4. Ref 데이터 타입 상세(Ref in Depth)

- `Ref[A]`는 타입 `A`의 값에 대한 가변 참조(mutable reference)를 모델링하며 그 안에 불변(immutable) 데이터를 저장
  - 두 가지 기본 연산: `Ref`를 새 값으로 채우는 `set` · 현재 내용을 가져오는 `get`
- `Ref`는 인메모리 상태(in-memory state)를 함수적으로 관리하는 방법 제공
  - 모든 연산이 원자적(atomic)이며 스레드 안전(thread-safe) → 동시성 프로그램을 동기화(synchronize)하기 위한 신뢰할 수 있는 토대
- `Ref`의 특성
  - 순수 함수적(purely functional)이며 참조 투명(referentially transparent)
  - 동시성 안전(concurrent-safe)하며 락-프리(lock-free)
  - 갱신(update)과 수정(modify)이 원자적으로 이루어짐

#### 4.1 동시성 상태 애플리케이션(Concurrent Stateful Application)

- `Ref`는 동시성 상태 애플리케이션을 작성하기 위한 토대
  - 여러 파이버 사이에서 정보를 공유·갱신해야 할 때마다 파이버들은 원자성(atomicity)을 보장하는 방법으로 통신 필요
  - `Ref`는 동시성 안전하므로 동일한 `Ref`를 여러 파이버 사이에서 공유 가능
  - 모든 파이버가 `Ref`를 동시적으로 갱신 가능 → 경쟁 상태(race condition)에 대한 걱정 없음
  - 만 개의 파이버가 모두 동일한 `Ref`를 갱신해도 원자적인 갱신·수정 함수를 사용하는 한 경쟁 상태 발생하지 않음

#### 4.2 연산(Operations)

- `Ref`에는 많은 연산이 있으나 여기서는 가장 흔하고 중요한 것들만 소개

##### make

- `Ref`는 결코 비어 있지 않으며 항상 무언가를 담고 있음
- 초기값(initial value)을 `make` 메서드(생성자)에 제공하여 `Ref` 생성
  - 타입 `A`의 불변값(immutable value)을 생성자에 전달 필요 → `UIO[Ref[A]]` 값 반환

```scala
def make[A](a: A): UIO[Ref[A]]
```

- 출력이 `UIO`로 감싸여 있음 → `Ref`를 만드는 것 자체가 효과적임을 의미
  - `Ref`를 `make`, `update`, `modify`할 때마다 효과적 연산 수행

- 불변값으로부터 몇 개의 `Ref` 생성 예시

```scala
val counterRef = Ref.make(0)
val stringRef = Ref.make("initial") 

sealed trait State
case object Active  extends State
case object Changed extends State
case object Closed  extends State

val stateRef = Ref.make(Active) 
```

> 경고: `Ref`를 만들 때 흔히 저지르는 실수는 그 안에 가변(mutable) 데이터를 저장하려는 것. `Ref`는 반드시 불변 데이터와 함께 사용 필요 → 그렇지 않으면 원자성 보장을 잃게 되며 충돌(collision)과 경쟁 상태로 이어질 수 있음

- 다음 스니펫은 컴파일되지만 가변 변수가 `make`에 제공되었기 때문에 경쟁 상태로 이어짐

```scala
// 컴파일은 되지만 제대로 동작하지 않음
val init = collection.mutable.Seq(1,3,5)
val counterRef = Ref.make(init)
```

- 이를 바로잡으려면 `init`을 불변으로 변경 필요

```scala
val init = Seq(1,3,5)
val counterRef = Ref.make(init)
```

##### get

- `get` 메서드는 참조의 현재 값을 반환

```scala
def get: IO[EB, B]
```

- `Ref`의 `make`와 `get` 메서드는 효과적 → `flatMap`으로 함께 연결 가능
  - 다음 예시에서는 `initial` 값으로 `Ref`를 만든 다음 `get` 메서드로 현재 상태를 얻음

```scala
Ref.make("initial")
   .flatMap(_.get)
   .flatMap(current => Console.printLine(s"current value of ref: $current"))
```

- 가독성을 높이기 위해 일련의 `flatMap` 대신 for-comprehension 사용해 리팩터링 가능

```scala
for {
  ref   <- Ref.make("initial")
  value <- ref.get
} yield assert(value == "initial")
```

- 모나드 연산(monadic operation) 바깥에서는 공유 상태에 접근할 방법이 없음

##### set

- `set` 메서드는 새 값을 `Ref`에 원자적으로 씀

```scala
for {
  ref   <- Ref.make("initial")
  _     <- ref.set("update")
  value <- ref.get
} yield assert(value == "update")
```

##### update

- `update`를 사용하면 주어진 순수(pure) 함수로 `Ref`의 상태를 원자적으로 갱신 가능
  - 그 함수는 결정론적(deterministic)이고 부수 효과가 없어야 함

```scala
def update(f: A => A): IO[E, Unit]
```

- 카운터가 있다면 `update` 메서드로 값을 증가시킬 수 있음

```scala
val counterInitial = 0
for {
  counterRef <- Ref.make(counterInitial)
  _          <- counterRef.update(_ + 1)
  value <- counterRef.get
} yield assert(value == 1)
```

> 주의: `update`는 `get`과 `set`의 합성(composition)이 아님. 이 합성은 동시성 안전하지 않음. 상태를 갱신해야 할 때마다 `Ref`를 원자적으로 수정하는 `update` 연산 사용 필요

- 다음 스니펫은 동시성 안전하지 않은 예

```scala
// 안전하지 않은 상태 관리
object UnsafeCountRequests extends ZIOAppDefault {

  def request(counter: Ref[Int]) = for {
    current <- counter.get
    _ <- counter.set(current + 1)
  } yield ()

  private val initial = 0
  private val myApp =
    for {
      ref <- Ref.make(initial)
      _ <- request(ref) zipPar request(ref)
      rn <- ref.get
      _ <- Console.printLine(s"total requests performed: $rn")
    } yield ()

  def run = myApp
}
```

- 위 스니펫은 결정론적으로 동작하지 않음 → 이 프로그램은 때때로 `2`를 출력하고 때때로 `1`을 출력
  - `update`를 사용해 이를 고칠 수 있음

```scala
// 안전한 상태 관리
object CountRequests extends ZIOAppDefault {

  def request(counter: Ref[Int]): ZIO[Any, Nothing, Unit] = {
    for {
      _ <- counter.update(_ + 1)
      reqNumber <- counter.get
      _ <- Console.printLine(s"request number: $reqNumber").orDie
    } yield ()
  }

  private val initial = 0
  private val myApp =
    for {
      ref <- Ref.make(initial)
      _ <- request(ref) zipPar request(ref)
      rn <- ref.get
      _ <- Console.printLine(s"total requests performed: $rn").orDie
    } yield ()

  def run = myApp
}
```

- `update`의 또 다른 사용 사례: `repeat` 콤비네이터(combinator) 작성

```scala
def repeat[E, A](n: Int)(io: IO[E, A]): IO[E, Unit] =
  Ref.make(0).flatMap { iRef =>
    def loop: IO[E, Unit] = iRef.get.flatMap { i =>
      if (i < n)
        io *> iRef.update(_ + 1) *> loop
      else
        ZIO.unit
    }
    loop
  }
```

##### modify

- `modify`는 `update`의 더 강력한 버전
  - 주어진 함수로 `Ref`를 원자적으로 수정하면서 반환값(return value)도 함께 계산
  - `modify`에 전달하는 함수는 결정론적이고 부수 효과가 없는 순수 함수여야 함

```scala
def modify[B](f: A => (B, A)): IO[E, B]
```

- `CountRequest` 예시를 다시 살펴봄 → `request` 함수 안에서 각 요청의 번호를 로깅하려는 경우
  - `update`와 `get` 메서드를 합성하여 작성하면 문제 발생

```scala
// 동시성 환경에서 안전하지 않음
def request(counter: Ref[Int]) = {
  for {
    _  <- counter.update(_ + 1)
    rn <- counter.get
    _  <- Console.printLine(s"request number received: $rn")
  } yield ()
}
```

- `update`와 `get` 사이에서 다른 파이버의 두 번째 `update`가 발생하면 동시성 환경에서 결정론적으로 동작하지 않음
  - get, set, get의 조합을 원자적으로 수행할 방법 필요 → 이때 `modify` 사용

```scala
// 동시성 환경에서 안전함
def request(counter: Ref[Int]) = {
  for {
    rn <- counter.modify(c => (c + 1, c + 1))
    _  <- Console.printLine(s"request number received: $rn")
  } yield ()
}
```

#### 4.3 Java의 AtomicReference (AtomicReference in Java)

- Java 프로그래머라면 `Ref`를 `AtomicReference`로 생각 가능
  - Java에는 `AtomicReference`, `AtomicLong`, `AtomicBoolean` 등이 담긴 `java.util.concurrent.atomic` 패키지 존재
  - `Ref`는 `AtomicReference`와 대략 동일한 능력·보장·한계를 가지되 더 고수준이며 ZIO 친화적임

#### 4.4 Ref vs. State 모나드 (Ref vs. State Monad)

- `Ref`는 ZIO 안에서 State 모나드(State Monad)의 모든 능력을 제공
  - State 모나드는 실제 애플리케이션 개발에서 중요한 두 가지 기능 부재
    1. 동시성 지원(Concurrency Support)
    2. 에러 처리(Error Handling)

##### 동시성(Concurrency)

- State 모나드는 상태만 포함하는 효과 시스템(effect system) → 순수한 상태 기반 계산(pure stateful computation)만 가능
  - 상태를 get, set, update(및 관련 계산)만 가능
  - 일련의 상태 기반 계산을 순차적으로 갱신 → 비동기(async)나 동시(concurrent) 계산에는 사용 불가
- `Ref`는 동시성 및 비동기 프로그래밍에 대한 훌륭한 지원 보유

##### 에러 처리(Error Handling)

- 대부분의 실제 상태 기반 애플리케이션은 데이터베이스 IO·API 호출 등 다양한 방식으로 실패할 수 있는 동시·동기 연산 관여 → 상태 관리 외에 에러를 처리할 방법 필요
- State 모나드는 에러 관리를 모델링하는 능력이 없음
  - StateT 모나드 변환기(monad transformer)로 State 모나드와 Either 모나드를 결합할 수 있으나 막대한 성능 오버헤드(performance overhead) 발생 → `Ref`로 할 수 없는 것을 가져다주지도 않으므로 안티패턴(anti-pattern)
- ZIO 모델에서는 에러가 효과에 인코딩(encode)되어 있고 `Ref`는 이를 그대로 활용 → 상태 관리에 더해 추가 작업 없이 에러 처리 가능

#### 4.5 상태 변환기(State Transformers)

- 변이(mutation)에 익숙한 이들에게는 어디에나 상태를 추가하는 것이 편하게 느껴짐

```scala
var idCounter = 0
def freshVar: String = {
  idCounter += 1
  s"var${idCounter}"
}
val v1 = freshVar
val v2 = freshVar
val v3 = freshVar
```

- 함수형 프로그래밍에서는 상태 변이를 `S => (A, S)` 타입의 함수 형태로 포착
  - `Ref`는 그러한 인코딩을 제공: `S`는 값의 타입, `modify`가 상태 변이 함수를 구현

```scala
Ref.make(0).flatMap { idCounter =>
  def freshVar: UIO[String] =
    idCounter.modify(cpt => (s"var${cpt + 1}", cpt + 1))

  for {
    v1 <- freshVar
    v2 <- freshVar
    v3 <- freshVar
  } yield ()
}
```

#### 4.6 더 정교한 동시성 기본 요소 구축(Building more sophisticated concurrency primitives)

- `Ref`는 다른 동시성 데이터 타입의 토대가 될 수 있을 만큼 저수준(low-level)
- 예: 세마포어(semaphore) — 공유 자원에 대한 접근을 제어하는 고전적인 추상 데이터 타입
  - 삼중쌍(triplet) `S = (v, P, V)`로 정의
    - `v`: 현재 사용 가능한 자원 단위(unit)의 수
    - `P`, `V`: 각각 `v`를 감소·증가시키는 연산
    - `P`는 `v`가 음수가 아닐 때만 완료 → 음수이면 기다려야 함
- `Ref`를 사용하면 그런 세마포어를 쉽게 구현 가능
  - 유일한 어려움은 `P`: `v`가 음수이거나, 값을 읽은 순간과 갱신을 시도하는 순간 사이에 그 값이 변경된 경우 실패 후 재시도(retry) 필요
  - 단순한 구현 예시

```scala
sealed trait S {
  def P: UIO[Unit]
  def V: UIO[Unit]
}

object S {
  def apply(v: Long): UIO[S] =
    Ref.make(v).map { vref =>
      new S {
        def V = vref.update(_ + 1).unit

        def P = (vref.get.flatMap { v =>
          if (v < 0)
            ZIO.fail(())
          else
            vref.modify(v0 => if (v0 == v) (true, v - 1) else (false, v)).flatMap {
              case false => ZIO.fail(())
              case true  => ZIO.unit
            }
        } <> P).unit
      }
    }
}
```

- 세마포어 테스트 예시

```scala
import zio.Console._

val party = for {
  dancefloor <- S(10)
  dancers <- ZIO.foreachPar(1 to 100) { i =>
    dancefloor.P *> Random.nextDouble.map(d => Duration.fromNanos((d * 1000000).round)).flatMap { d =>
      printLine(s"${i} checking my boots") *> ZIO.sleep(d) *> printLine(s"${i} dancing like it's 99")
    } *> dancefloor.V
  }
} yield ()
```

- ZIO 자체의 `Semaphore`를 살펴보는 편이 좋음
  - `Semaphore`는 대기하는 동안 CPU 사이클을 낭비하지 않으면서 위 기능을 더 정교하게 제공

#### 4.7 연속(continuation)을 동반한 원자적 수정(Atomic Modify with Continuation)

- `Ref#modify`는 상태를 원자적으로 수정하고 반환값을 계산하는 순수 함수를 받음
  - `Ref#modify` 안에서 효과를 실행해서는 안 되지만, 원자적 수정 이후에 런타임이 실행하는 효과인 연속(continuation)을 도입 가능

```scala
ref.modify { state =>
  val doThisNext = someZIO
  val newState = computeNewState(state)
  (doThisNext, newState)  // "doThisNext"가 연속(continuation)
}.flatten  // 연속을 실행하기 위해 flatten
```

- `ref.modify { state => ... }`의 타입은 또 다른 효과를 생성하는 효과
  - 이 효과를 실행하면 원자적 수정을 수행하고 연속 효과를 반환
  - 연속을 실행하려면 결과를 `flatten` 필요

- 중요: 연속은 원자적 수정의 일부가 아님
  - 상태 수정은 연속이 실행되기 전에 완료되며 연속의 결과에 의존하지 않음
  - 여러 파이버가 이 패턴을 동시적으로 실행하면, 상태 수정은 원자적이더라도 그들의 연속은 서로 교차(interleave)될 수 있음
- 연속이 원자적 연산의 일부가 되어야 한다면(수정과 연속 사이에 다른 파이버도 상태를 수정할 수 없도록 보장하려면) `Ref.Synchronized` 사용
  - 자세한 내용은 5절 참고

---

### 5. Ref.Synchronized: 효과적인 원자적 갱신(Ref.Synchronized)

- `Ref.Synchronized[A]`는 타입 `A`의 값에 대한 가변 참조를 모델링하며 그 안에 불변 데이터를 저장하고 원자적이면서 효과적으로(effectfully) 갱신 가능

> 참고: `Ref.Synchronized`의 연산 대부분은 `Ref`와 동일함. `Ref`에 익숙하지 않다면 먼저 `Ref`(4절)를 읽어 보는 것을 권장

- `Ref.Synchronized`로 공유 상태를 효과적으로 갱신하는 방법
  - `update` 메서드를 비롯한 관련 메서드들은 효과적 연산을 받아 그 효과를 실행한 후 공유 상태를 변경
  - 이것이 `Ref.Synchronized`와 `Ref`의 주요 차이점

- 다음 예시에서는 갱신 연산의 서술인 `updateEffect`를 전달 필요
  - `Ref.Synchronized`는 `updateEffect`를 실행하여 `ref`를 갱신

```scala
import zio._
for {
  ref <- Ref.Synchronized.make("current")
  updateEffect = ZIO.succeed("update")
  _ <- ref.updateZIO(_ => updateEffect)
  value <- ref.get
} yield assert(value == "update")
```

- 실제 애플리케이션에서는 효과(예: 데이터베이스 질의)를 실행한 다음 공유 상태를 갱신하려는 경우 존재
  - `Ref.Synchronized`가 액터 모델(actor model)에 가까운 방식으로 공유 상태를 갱신하도록 지원
  - 공유된 가변 상태가 있지만 서로 다른 명령(command)이나 메시지(message)마다 효과를 실행하고 상태를 갱신하려는 경우에 유용

- 각 갱신마다 효과적 프로그램을 전달 가능
  - 모든 갱신은 병렬(parallel)로 수행되지만, 그 결과는 서로 다른 시점에 상태를 순서대로 반영(sequence) → 결국 일관된(consistent) 최종 상태 보장

- 다음 예시에서는 각 사용자에 대해 `getAge` 요청을 `usersApi`로 보내고 그에 따라 상태를 갱신

```scala
val meanAge =
  for {
    ref <- Ref.Synchronized.make(0)
    _ <- ZIO.foreachPar(users) { user =>
      ref.updateZIO(sumOfAges =>
        api.getAge(user).map(_ + sumOfAges)
      )
    }
    v <- ref.get
  } yield (v / users.length)
```

---

### 6. 파이버 로컬 상태(Fiber-local State)

- `FiberRef`와 `ZState` 데이터 타입은 모두 특정 파이버에 한정(scoped)된 상태 관리 도구
  - 그 값들은 해당 파이버 안에서만 접근 가능
- `FiberRef`와 `ZState` 데이터 타입은 각각 별도의 절(7절, 8절)에서 사용법을 설명

---

### 7. FiberRef: 파이버 로컬 저장소(FiberRef)

- `FiberRef`는 ZIO 파이버 내에서 스레드 로컬(thread-local) 값을 관리하고 접근하기 위한 데이터 구조
  - 스레드 로컬 저장소(Thread-local storage, TLS): 각 파이버에게 자기 자신의 분리된 저장 공간을 제공하는 메커니즘
  - `FiberRef[A]`: 특정 파이버에 로컬한 타입 `A`의 값을 저장하고 조회할 수 있게 해 주는 특수한 종류의 가변 참조(`Ref[A]`)

- `FiberRef` 데이터 구조로 가능한 연산
  - 파이버 내에서 현재 값을 읽거나, 값을 갱신하거나, 값을 원자적으로 수정
  - 서로 다른 파이버 사이에서 스레드 안전성과 격리(isolation) 보장 → 각 파이버가 `FiberRef`에 대한 자기 자신의 독립적인 값을 가짐
  - 각 파이버는 파이버 고유의 변수에 대한 자기 자신의 복사본(copy) 유지 → 한 파이버가 변수에 가한 수정이 다른 파이버가 보는 값에 영향을 주지 않음

- `FiberRef`를 사용하면 파이버별(per-fiber) 컨텍스트나 상태 정보 유지 가능
  - 리소스 관리, 애플리케이션 고유 정보 추적, 파이버 실행 전반에 걸친 컨텍스트 데이터 전달 등 다양한 시나리오에서 유용

- `FiberRef`는 기능이 강화된(on steroids) Java의 `ThreadLocal`로 볼 수 있음
  - Java에 `ThreadLocal`이 있듯 ZIO에는 `FiberRef`가 있음
  - 서로 다른 스레드가 서로 다른 `ThreadLocal`을 가지듯, 서로 다른 파이버는 서로 다른 `FiberRef` 값을 가지며 서로 교차하거나 겹치지 않음
  - `FiberRef`는 의미론(semantics) 면에서 상당히 개선된 `ThreadLocal`의 파이버 버전
    - `ThreadLocal`은 각 스레드가 자기 자신의 복사본에 접근하는 가변 상태만 제공 → 스레드는 자신의 상태를 자식(children)에게 전파하지 않음

- `Ref[A]`와 달리 `FiberRef[A]`의 값은 실행 중인 파이버에 묶여(bound) 있음
  - 동일한 `FiberRef[A]`를 가진 서로 다른 파이버는 충돌 없이 독립적으로 참조의 값을 설정하고 조회 가능

```scala
import zio._

for {
  fiberRef <- FiberRef.make[Int](0)
  _        <- fiberRef.set(10)
  v        <- fiberRef.get
} yield v == 10
```

#### 7.1 동기(Motivation)

- 한정된(scoped) 정보나 컨텍스트가 있고 이를 ZIO 환경(environment)에 저장하고 싶지 않을 때마다 `FiberRef` 사용 가능

- 이를 설명하기 위해 구조화된 로깅(Structured Logging) 문제의 해결책을 찾아봄
  - 구조화된 로깅에서는 사용자 ID, 상관 ID(correlation id), 로그 레벨(log level) 등 컨텍스트 정보를 로그 메시지에 첨부하려는 경향 존재

- 다음과 같은 코드를 작성했다고 가정

```scala
import zio._

for {
  _ <- Logging.log("Hello World!")
  _ <- ZIO.foreachParDiscard(List("Jane", "John")) { name =>
    Logging.logAnnotate("name", name) {
      for {
        _ <- Logging.log(s"Received request")
        fiberId <- ZIO.fiberId.map(_.ids.head)
        _ <- Logging.logAnnotate("fiber_id", s"$fiberId")(
          Logging.log("Processing request")
        )
        _ <- Logging.log("Finished processing request")
      } yield ()
    }
  }
  _ <- Logging.log("All requests processed")
} yield ()
```

- 다음과 같은 로그 출력을 기대

```scala
Hello World!
[name=Jane] Received request
[name=John] Received request
[name=Jane] [fiber_id=7] Processing request
[name=John] [fiber_id=8] Processing request
[name=John] Finished processing request
[name=Jane] Finished processing request
All requests processed
```

- 위 코드에는 두 사용자 `Jane`과 `John`이 있으며 각 사용자에 대한 일부 연산을 동시적으로 처리
  - 동시 연산 수행 시 각 연산을 해당 사용자·파이버 ID와 연관시킬 방법이 필요
  - 메시지를 로깅할 때 특정 이벤트에 대한 모든 정보를 로그에 담기 위함

- 이를 위해 컨텍스트 인식(context-aware) 로깅 서비스 필요
  - 이 로깅 서비스는 주석(annotation)을 저장할 장소인 상태(state)를 가져야 함
  - 이 상태는 여러 파이버에 의해 동시적으로 접근·수정될 수 있음
  - 각 파이버가 자기 자신의 격리된 상태 복사본을 가져야 함 → 한 파이버가 상태를 수정할 때 다른 파이버의 상태를 덮어쓰지 않도록

- 지금까지의 요구 사항을 두 부분으로 분류 가능
  - 명시적으로 전달하지 않으면서 어떤 컨텍스트 정보를 전달(carry)할 메커니즘 필요
  - 각 파이버가 다른 파이버의 상태에 영향을 주지 않고 상태를 갱신할 수 있는, 격리된 방식으로 상태를 갱신할 메커니즘 필요

#### 7.2 해결책(Solution)

- 이 절에서는 위에서 언급한 구조화된 로깅 문제에 대한 두 가지 해결책 살펴봄
  - 첫 번째 해결책에는 몇 가지 제약과 단점 존재 → 두 번째 해결책을 최종 해결책으로 선택

##### 해결책 1: ZIO 환경(ZIO Environment)

- ZIO 환경을 사용해 상태를 저장하는 방법
  - 첫 번째 요구 사항을 매우 잘 다룸 → ZIO 환경은 컨텍스트 상태를 저장하기 좋은 장소
  - 상태를 파이버 사이에서 격리하기 위해, 환경을 전역적으로 갱신하는 대신 새 상태를 환경에 다시 도입(reintroduce) 가능

```scala
// 해결책 1: 컨텍스트 상태를 저장하기 위해 ZIO 환경 사용
import zio._

object Logging {
  type Annotation = Map[String, String]

  def logAnnotate[R, E, A](key: String, value: String)(
    zio: ZIO[R with Annotation, E, A]
  ): ZIO[R with Annotation, E, A] = {
    for {
      s <- ZIO.service[Annotation]
      r <- zio.provideSomeLayer[R](ZLayer.succeed(s.updated(key, value)))
    } yield (r)
  }

  def log(message: String): ZIO[Annotation, Nothing, Unit] = {
    ZIO.service[Annotation].flatMap {
      case annotation if annotation.isEmpty => 
        Console.printLine(message).orDie
      case annotation =>
        val line =
          s"${annotation.map { case (k, v) => s"[$k=$v]" }.mkString(" ")} $message"
        Console.printLine(line).orDie
    }
  }
}
```

- ZIO 환경 해결책의 특징
  - 컨텍스트 데이터 타입을 다룰 때 타입 안전성(type-safety)을 명시적으로 보장
  - 이렇게 높아진 타입 안전성이 특정 시나리오에서 유연성을 제한할 수 있음
    - 예: 워크플로우(workflow)가 `Logging`, `Config`, `Metrics` 같은 여러 횡단 관심사 서비스(cross-cutting service)를 요구하는 상황
    - 이 경우 모든 애플리케이션 로직이 `ZIO[Logging & Config & Metrics & ..., IOException, Any]` 같은 타입 시그니처(type signature)를 가짐
    - 이런 방대한 타입 선언은 리팩터링과 유지보수를 어렵게 만들고 핵심 비즈니스 로직에서 주의를 분산시킴
    - 컨텍스트 데이터 타입을 수정할 때마다 프로그램 전체를 수정해야 함

- 이전 예시에서 ZIO 환경을 사용해 상태를 저장하는 데 성공했으나 관용적(idiomatic) 해결책으로 여겨지지는 않음
  - 환경에 상태 타입(이 경우 `Annotation`)을 명시적으로 노출하지 않는 편이 바람직

- 이 해결책이 특히 유익한 경우
  - 컨텍스트 서비스가 워크플로우 로직에서 핵심적인 역할을 할 때
  - ZIO 환경 내에서 서비스 타입에 대한 타입 안전성을 보장해야 할 때
  - 그러한 서비스에 대한 합리적인 기본값(default value)이 없을 때

##### 해결책 2: FiberRef

- 다른 해결책은 `FiberRef`를 사용하는 것
  - `FiberRef`는 컨텍스트 상태를 저장하고 격리하는 좋은 방법
  - `FiberRef`가 유지하는 어떤 상태든 파이버 사이에서 격리됨
  - 상태를 환경에 둘 필요가 없다는 장점

- `FiberRef`를 사용해 로깅 서비스를 구현하는 예시

```scala
// 해결책 2: 컨텍스트 상태를 저장하기 위해 FiberRef 사용
import zio._

trait Logger {
  def logAnnotate[R, E, A](key: String, value: String)(
      zio: ZIO[R, E, A]
  ): ZIO[R, E, A]
  def log(message: String): UIO[Unit]
}

object Logging extends Logger {
  def logAnnotate[R, E, A](key: String, value: String)(
      zio: ZIO[R, E, A]
  ): ZIO[R, E, A] = currentAnnotations.locallyWith(_.updated(key, value))(zio)

  def log(message: String): UIO[Unit] = {
    currentAnnotations.get.flatMap {
      case annotation if annotation.isEmpty =>
        Console.printLine(message).orDie
      case annotation =>
        val line =
          s"${annotation.map { case (k, v) => s"[$k=$v]" }.mkString(" ")} $message"
        Console.printLine(line).orDie
    }
  }

  val currentAnnotations: FiberRef[Map[String, String]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(Map.empty[String, String])
    }

}
```

- 일부 정보를 로깅하는 프로그램 작성

```scala
import zio._

object FiberRefLoggingExample extends ZIOAppDefault {
  def run =
    for {
      _ <- Logging.log("Hello World!")
      _ <- ZIO.foreachParDiscard(List("Jane", "John")) { name =>
        Logging.logAnnotate("name", name) {
          for {
            _       <- Logging.log(s"Received request")
            fiberId <- ZIO.fiberId.map(_.ids.head)
            _ <- Logging.logAnnotate("fiber_id", s"$fiberId")(
              Logging.log("Processing request")
            )
            _ <- Logging.log("Finished processing request")
          } yield ()
        }
      }
      _ <- Logging.log("All requests processed")
    } yield ()
}
```

- 출력

```scala
Hello World!
[name=Jane] Received request
[name=John] Received request
[name=Jane] [fiber_id=5] Processing request
[name=John] [fiber_id=6] Processing request
[name=John] Finished processing request
  [name=Jane] Finished processing request
All requests processed
```

> 참고: 위 해결책에서 `FiberRef`를 `Ref`로 교체하면 프로그램이 제대로 동작하지 않음. `Ref`는 격리되지 않기 때문. `Ref`는 모든 파이버 사이에서 공유되므로 각 파이버가 다른 파이버의 상태를 덮어씀

- 한 걸음 더 나아가, 사용자가 기저(underlying) 로깅 서비스를 변경할 수 있도록 이전 예시 수정

```scala
import zio._

trait Logger {
  def logAnnotate[R, E, A](key: String, value: String)(
    zio: ZIO[R, E, A]
  ): ZIO[R, E, A]

  def log(message: String): UIO[Unit]
}
```

```scala
import zio._

object Logging {

  val defaultLogger: Logger = new Logger {
    def logAnnotate[R, E, A](key: String, value: String)(
      zio: ZIO[R, E, A]
    ): ZIO[R, E, A] = currentAnnotations.locallyWith(_.updated(key, value))(zio)

    def log(message: String): UIO[Unit] = {
      currentAnnotations.get.flatMap {
        case annotation if annotation.isEmpty =>
          Console.printLine(message).orDie
        case annotation =>
          val line =
            s"${annotation.map { case (k, v) => s"[$k=$v]" }.mkString(" ")} $message"
          Console.printLine(line).orDie
      }
    }
  }

  val silentLogger: Logger = new Logger {
    def logAnnotate[R, E, A](key: String, value: String)(
      zio: ZIO[R, E, A]
    ): ZIO[R, E, A] = currentAnnotations.locallyWith(_.updated(key, value))(zio)

    def log(message: String): UIO[Unit] = ZIO.unit
  }

  def log(message: String): ZIO[Any, Nothing, Unit] =
    currentLogger.get.flatMap(_.log(message))

  def logAnnotate[R, E, A](key: String, value: String)(
    zio: ZIO[R, E, A]
  ): ZIO[R, E, A] = currentLogger.get.flatMap(_.logAnnotate(key, value)(zio))

  def locallyWithLogger[R, E, A](newLogger: Logger)(zio: ZIO[R, E, A]) = {
    currentLogger.locallyWith(_ => newLogger)(zio)
  }

  def updateLogger(logger: Logger => Logger): UIO[Unit] = currentLogger.update(logger)

  val currentLogger: FiberRef[Logger] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(defaultLogger)
    }

  val currentAnnotations: FiberRef[Map[String, String]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(Map.empty[String, String])
    }

}
```

- `Logging.locallyWithLogger` 함수로 기본 로거를 손쉽게 변경 가능
  - `Logging.silentLogger`를 활용해 예시의 특정 구간에서 기본 로거를 비활성화

```scala
import zio._

object FiberRefChangeDefaultLoggerExample extends ZIOAppDefault {
  def run = for {
    _ <- Logging.log("Hello World!")
    _ <- ZIO.foreachParDiscard(List("Jane", "John")) { name =>
      Logging.locallyWithLogger(Logging.silentLogger) {
        Logging.logAnnotate("name", name) {
          for {
            _ <- Logging.log(s"Received request")
            fiberId <- ZIO.fiberId.map(_.ids.head)
            _ <- Logging.logAnnotate("fiber_id", s"$fiberId")(
              Logging.log("Processing request")
            )
            _ <- Logging.log("Finished processing request")
          } yield ()
        }
      }
    }
    _ <- Logging.log("All requests processed")
  } yield ()
}
```

- 출력

```scala
Hello World!
All requests processed
```

- `FiberRef`를 사용하면 컨텍스트 데이터나 서비스를 타입이 드러나지 않는(untyped) 방식으로 저장하고 전파 가능
  - 환경 타입의 중복을 줄이는 데 도움
  - 예: `Logging`과 `Metrics` 서비스를 `FiberRef`로 인코딩하면 ZIO 워크플로우의 환경 타입에 이 타입들을 포함할 필요 없음
    - `ZIO[Logging & Metrics & UserRepo & DocsRepo, IOException, Unit]` → `ZIO[UserRepo & DocsRepo, IOException, Unit]`로 단순화 가능
  - 워크플로우의 상용구(boilerplate) 코드를 크게 줄여 핵심 애플리케이션 로직에 집중 가능

- `FiberRef`는 컨텍스트 서비스나 데이터에 기본값이 있을 때 유용한 해결책
  - 기본값으로 애플리케이션을 시작 → 필요할 때마다 `FiberRef#locallyWith`, `FiberRef#update`로 기저 서비스나 데이터를 로컬 또는 전역적으로 변경 가능

- 요약: 이 해결책이 특히 유리한 시나리오
  - ZIO 환경 곳곳에 포함할 필요 없이 횡단 관심사 서비스를 인코딩할 때
  - 서로 다른 파이버에 대해 격리된 상태가 필요할 때
  - 컨텍스트 서비스나 데이터에 대한 기본값이 있을 때

#### 7.3 사용 사례(Use Cases)

- 어떤 종류의 한정된 정보나 컨텍스트가 있을 때마다 그 정보를 저장하는 방법으로 `FiberRef` 고려 가능

- 애플리케이션 개발 시 `FiberRef`의 여러 사용 사례
  - 리소스 관리(Resource management): 특정 파이버에 고유한 리소스(예: 데이터베이스·네트워크 리소스에 대한 커넥션) 저장·접근에 활용 → 각 파이버가 자기 전용 리소스를 가짐 → 격리 보장, 서로 다른 파이버 간 경합(contention) 회피
  - 구성 설정(Configuration Settings): 파이버에 고유한 구성 설정 저장 → 서로 다른 파이버가 자기 자신의 구성 값을 가짐 → 세밀한 제어와 커스터마이즈 가능
  - 동기화 회피(Avoiding Synchronization): 파이버별 데이터 접근 시 락·원자적 연산 같은 동기화 메커니즘 불필요 → 각 파이버가 자기 자신의 비공개 복사본에서 동작 → 다른 파이버와의 경합 회피
  - 분산 추적(Distributed Tracing): 고도로 동시적인 워크플로우·분산 서비스 아키텍처에서 요청이 서비스들을 통과하며 전파되는 것을 추적할 필요 → `FiberRef`로 요청 범위(request-scoped) 정보를 자동으로 전파하도록 시스템 설계 가능
  - 컨텍스트 로깅(Contextual Logging): 로그가 독립적인 정보 조각이 아니라 더 큰 컨텍스트의 일부인 경우가 많음 → 메시지 로깅 외에 요청 ID, 사용자 ID, 세션 ID 등 추가 정보도 로깅 필요 → 로그 수집 시 공통 데이터 포인트를 기반으로 상호 연관(correlate) 가능 → 이 컨텍스트 정보를 명시적으로 전달하는 대신 `FiberRef` 사용 가능
  - 실행 범위 구성(Execution Scoped Configuration): 애플리케이션을 구성 가능하게 만들어 한 번 구성한 뒤 전체 구성 요소에 걸쳐 사용 → 모든 구성이 전역적인 것은 아님 → 전역적이지 않거나, 전역 기본값은 있지만 특정 영역에서 동적으로 변경해야 하는 구성 존재 → `FiberRef`가 이런 종류의 구성을 모델링하는 좋은 도구

- ZIO 자체에서도 `FiberRef`의 여러 사용 사례 존재
  1. `ZIO.withParallelism`을 사용할 때마다 코드의 한 영역에 대한 병렬성 인자(parallelism factor) 지정 가능
     - 이 정보는 모든 효과에 명시적으로 전달할 필요 없이 `FiberRef` 안에 저장됨
     - 영역을 벗어나면 병렬성 인자는 원래 값으로 복원됨

```scala
import zio._
object MainApp extends ZIOAppDefault {
  def myJob(name: String) =
    ZIO.foreachParDiscard(1 to 3)(i =>
      ZIO.debug(s"The $name-$i job started") *> ZIO.sleep(2.second)
    )

  def run =
    ZIO.withParallelismUnbounded(
      for {
        _ <- myJob("foo")
        _ <- ZIO.debug("------------------")
        _ <- ZIO.withParallelism(1)(myJob("bar"))
        _ <- ZIO.debug("------------------")
        _ <- myJob("baz")
      } yield ()
    )
}
```

  2. `ZIOAspect.annotated`를 사용하면 효과에 `correlation_id` 같은 컨텍스트 정보를 주석으로 붙일 수 있음
     - 이 정보는 `FiberRef` 안에 저장되며 동일한 부모 파이버에서 생성된 모든 파이버로 전파됨
     - 각 파이버가 자기 자신의 주석 집합을 가짐
     - 파이버 안에서 로깅할 때 로깅 서비스가 그 파이버의 고유한 주석을 사용해 로그 메시지를 생성

```scala
import zio._

object MainApp extends ZIOAppDefault {

  def handleRequest(request: String) =
    for {
      _ <- ZIO.log(s"Received request.")
      _ <- ZIO.unit // 요청으로 무언가를 함
      _ <- ZIO.log(s"Finished processing request")
    } yield ()

  def run =
    for {
      _ <- ZIO.log("Hello World!")
      _ <- ZIO.foreachParDiscard(List(("req1", "1"), ("req2", "2"), ("req3", "3"))){ case (req, id) =>
        handleRequest(req) @@ ZIOAspect.annotated("correlation_id", id)
      }
      _ <- ZIO.log("Goodbye!")
    } yield ()

}
```

- 출력(가독성을 위해 추가 열은 제거함)

```
message="Hello World!"
message="Received request." correlation_id=2
message="Received request." correlation_id=1
message="Received request." correlation_id=3
message="Finished processing request." correlation_id=3
message="Finished processing request." correlation_id=1
message="Finished processing request." correlation_id=2
message="Goodbye!"
```

  3. 로그 레벨(Log level) 또한 `FiberRef`를 사용해 유지됨
     - 로그 레벨은 `FiberRef` 안에 저장되며 원할 때마다 `ZIO.logLevel` 연산자로 변경 가능

```scala
import zio._

for {
  _ <- ZIO.log("Application started!")
  _ <- ZIO.logLevel(LogLevel.Trace) {
    for {
      _ <- ZIO.log("Entering trace log level region")
      _ <- ZIO.log("Doing something")
      _ <- ZIO.log("Leaving trace log level region")
    } yield ()
  }
  _ <- ZIO.log("Application ended!")
} yield ()
```

  4. 환경에 접근할 때(예: `ZIO.service`), ZIO 효과에 레이어(layer)를 제공할 때(예: `ZIO#provide`)에도 마찬가지
     - ZIO는 내부적으로 환경을 저장하기 위해 `FiberRef` 사용

```scala
import zio._

object MainApp extends ZIOAppDefault {
  private val fooLayer = ZLayer.succeed("foo")
  private val barLayer = ZLayer.succeed("bar") 
  
  def run =
    (for {
      _ <- ZIO.service[String].debug("context")
      _ <- ZIO.service[String].debug("context").provide(barLayer)
      _ <- ZIO.service[String].debug("context")
    } yield ()).provide(fooLayer)
}
// Output:
// context: foo
// context: bar
// context: foo
```

- ZIO 자체에는 `FiberRef`에 대한 다른 사용 사례도 여럿 존재 → 실제로 어떻게 사용되는지 아이디어를 주기 위해 그중 일부만 다룸

#### 7.4 연산(Operations)

- `FiberRef[A]`는 `Ref[A]`와 거의 동일한 API 보유
  - `FiberRef#get`: 참조의 현재 값을 반환
  - `FiberRef#set`: 참조의 현재 값을 설정
  - `FiberRef#update` / `FiberRef#updateSome`: 지정한 함수로 값을 갱신
  - `FiberRef#modify` / `FiberRef#modifySome`: 지정한 함수로 값을 수정하고 연산에 대한 반환값을 계산

- `locally`를 사용하면 주어진 효과에 대해서만 `FiberRef` 값을 한정(scope)할 수도 있음

```scala
import zio._

for {
  correlationId <- FiberRef.make[String]("")
  v1            <- correlationId.locally("my-correlation-id")(correlationId.get)
  v2            <- correlationId.get
} yield v1 == "my-correlation-id" && v2 == ""
```

#### 7.5 Ref vs. FiberRef

- 두 가지 실용적인 예시를 통해 `Ref`와 `FiberRef`의 차이 살펴봄

```scala
import zio._

object RefExample extends ZIOAppDefault {

  def run =
    for {
      ref <- Ref.make(0)
      left = ref.updateAndGet(_ + 1).debug("left1") *>
        ref.updateAndGet(_ + 1).debug("left2")
      right = ref.updateAndGet(_ + 1).debug("right1") *>
        ref.updateAndGet(_ + 3).debug("right2")
      _ <- left <&> right
    } yield ()
}
```

- 이 프로그램을 실행한 한 가지 가능한 결과

```scala
left1: 1
right1: 2
left2: 3
right2: 6
```

- `ref`가 `left`와 `right` 파이버 사이에서 공유된다는 점이 분명함
  - `FiberRef`를 사용하면 각 파이버가 자기 자신의 별도 저장소를 가지며 서로 격리됨

```scala
import zio._

object FiberRefExample extends ZIOAppDefault {
  def run =
    for {
      ref <- FiberRef.make(0)
      left = ref.updateAndGet(_ + 1).debug("left1") *>
        ref.updateAndGet(_ + 1).debug("left2")
      right = ref.updateAndGet(_ + 1).debug("right1") *>
        ref.updateAndGet(_ + 3).debug("right2")
      _ <- left <&> right
    } yield ()
}
```

- 이 프로그램의 한 가지 가능한 출력

```scala
left1: 1
right1: 1
left2: 2
right2: 4
```

- 각 파이버가 다른 파이버의 값을 방해하지 않으면서 자기 자신의 저장소를 가짐을 확인 가능

#### 7.6 전파(Propagation)

- `FiberRef`와 비교되는 `ThreadLocal`의 동작 먼저 살펴봄
  - 스레드 `A`가 `ThreadLocal` 값을 가지고 있고, 스레드 `A`가 새 스레드(스레드 `B`)를 생성한다고 가정
  - 스레드 `B`가 동일한 `ThreadLocal`을 참조하면 `A`의 값이 아닌 `ThreadLocal`의 기본값을 봄
  - 즉 `ThreadLocal`은 스레드 그래프 전반에 값을 전파하지 않음 → 한 스레드가 다른 스레드를 생성할 때 `ThreadLocal` 값은 부모에서 자식으로 전파되지 않음

- `FiberRef`는 이 모델을 크게 개선
  - 기본적으로 자식 파이버가 부모로부터 생성될 때마다 부모 파이버의 `FiberRef` 값이 자식 파이버로 전파됨

##### 포크 시 복사(Copy-on-Fork)

- `FiberRef[A]`는 `ZIO#fork`에 대해 포크 시 복사(copy-on-fork) 의미론을 가짐
  - 자식 `Fiber`가 부모의 `FiberRef` 값으로 시작함을 의미
  - 자식이 `FiberRef`의 새 값을 설정하면 그 변경은 자식 자신에게만 보임 → 부모 파이버는 여전히 자기 자신의 값을 가짐

- `FiberRef`를 만들고 값을 `5`로 설정한 다음 자식 파이버에 전달하면 자식은 값 `5`를 봄
  - 자식 파이버가 값을 `5`에서 `6`으로 수정해도 부모 파이버는 그 변경을 볼 수 없음
  - 즉 자식 파이버는 `FiberRef`의 자기 자신의 복사본을 얻어 로컬에서 수정 가능 → 그 변경은 부모 파이버에 영향을 주지 않음

```scala
import zio._

for {
  fiberRef <- FiberRef.make(5)
  promise <- Promise.make[Nothing, Int]
  _ <- fiberRef
    .updateAndGet(_ => 6)
    .flatMap(promise.succeed).fork
  childValue <- promise.await
  parentValue <- fiberRef.get
} yield assert(parentValue == 5 && childValue == 6)
```

#### 7.7 FiberRef 병합(Merging FiberRefs)

- ZIO는 `FiberRef` 값을 부모에서 자식으로 전파하는 것뿐 아니라, 이 값들을 현재 파이버로 다시 병합(merge back)하는 것도 지원
  - 이 절에서 이를 위한 여러 변형 설명

##### join

- 파이버를 `join`하면 그 `FiberRef`의 값이 부모 파이버로 다시 병합됨
  - 병합의 기본 전략: 대체(replacement)
  - 포크된 파이버가 부모 파이버에 조인될 때마다 부모의 값은 자식 `FiberRef`의 값으로 대체됨

```scala
import zio._

for {
  fiberRef <- FiberRef.make(5)
  child <- fiberRef.set(6).fork
  _ <- child.join
  parentValue <- fiberRef.get
} yield assert(parentValue == 6)
```

- 파이버를 `fork`하고 자식 파이버가 여러 `FiberRef`를 수정한 뒤 `join`하면, 그 수정들이 부모 파이버로 다시 병합됨
  - 이것이 `join`에 대한 ZIO의 의미 모델(semantic model)

- 각 파이버는 자기 자신의 `FiberRef`를 독립적으로 수정 가능
  - 여러 자식 파이버가 부모에 `join`할 때 마지막으로 조인하는 자식 파이버가 부모의 `FiberRef` 값을 자기 값으로 덮어씀

- 아래 예시에서 `child1`이 마지막 파이버이므로 그 값인 `6`이 부모로 다시 병합됨

```scala
import zio._

for {
  fiberRef <- FiberRef.make(5)
  child1 <- fiberRef.set(6).fork
  child2 <- fiberRef.set(7).fork
  _ <- child2.join
  _ <- child1.join
  parentValue <- fiberRef.get
} yield assert(parentValue == 6)
```

##### 커스텀 병합을 동반한 join (join with Custom Merge)

- 파이버가 포크될 때 값을 어떻게 초기화할지, 값들을 다시 병합할 때 어떻게 결합(combine)할지 커스터마이즈 가능
  - `FiberRef#make`로 `FiberRef`를 만들 때 원하는 동작을 지정

```scala
import zio._

for {
  fiberRef <- FiberRef.make(initial = 0, join = math.max)
  child    <- fiberRef.update(_ + 1).fork
  _        <- fiberRef.update(_ + 2)
  _        <- child.join
  value    <- fiberRef.get
} yield assert(value == 2)
```

- 이 예시에서 자식 파이버가 부모에 조인할 때 `max` 함수를 사용해 값 병합 방식 결정
  - 자식의 `FiberRef` 값(1)과 부모의 `FiberRef` 값(2)을 비교하여 더 높은 값을 병합 결과로 선택 → 이 경우 2

##### await

- `await`에는 그런 병합 동작이 없다는 점이 중요
  - `await`는 자식 파이버가 끝나기를 기다린 뒤 그 결과를 `Exit`로 반환하되 `FiberRef` 값을 부모로 다시 병합하지 않음

```scala
import zio._

for {
  fiberRef <- FiberRef.make(5)
  child <- fiberRef.set(6).fork
  _ <- child.await
  parentValue <- fiberRef.get
} yield assert(parentValue == 5)
```

- `join`은 `await`보다 고수준의 의미론을 가짐
  - 자식 파이버가 실패하면 함께 실패
  - 자식이 인터럽트(interrupt)되면 함께 인터럽트
  - 그 값을 부모로 다시 병합

##### inheritAll

- `Fiber#inheritAll` 메서드를 사용하면 해당 `Fiber`의 모든 `FiberRef` 값을 현재 파이버로 상속(inherit) 가능

```scala
import zio._

for {
  fiberRef <- FiberRef.make[Int](0)
  latch    <- Promise.make[Nothing, Unit]
  fiber    <- (fiberRef.set(10) *> latch.succeed(())).fork
  _        <- latch.await
  _        <- fiber.inheritAll
  v        <- fiberRef.get
} yield v == 10
```

- `inheritAll`은 `join` 시 자동으로 호출됨
  - `join`은 최종(final) 값을 병합하기 위해 기다림
  - `inheritAll`은 현재(current) 값을 병합한 다음 계속 진행

```scala
import zio._

val withJoin =
    for {
        fiberRef <- FiberRef.make[Int](0)
        fiber    <- (fiberRef.set(10) *> fiberRef.set(20).delay(2.seconds)).fork
        _        <- fiber.join  // 파이버의 종료를 기다린 뒤 최종 결과 20을 fiberRef로 복사
        v        <- fiberRef.get
    } yield assert(v == 20)
```

```scala
import zio._

val withoutJoin =
    for {
        fiberRef <- FiberRef.make[Int](0)
        fiber    <- (fiberRef.set(10) *> fiberRef.set(20).delay(2.seconds)).fork
        _        <- fiber.inheritAll.delay(1.second) // 중간 결과 10을 fiberRef로 복사하고 계속 진행
        v        <- fiberRef.get
    } yield assert(v == 10)
```

#### 7.8 합성 가능한 갱신과 패치 이론(Compositional Updates and Patch Theory)

- 앞 절에서 확인한 내용
  1. 자식 파이버가 부모로 다시 병합될 때마다 자식 파이버의 값이 기본적으로 부모의 값을 대체함
  2. 여러 자식 파이버가 모두 부모에 조인하면, 마지막으로 조인한 자식의 값이 부모의 값을 대체함

- 이 두 규칙을 간단한 예시로 확인

```scala
import zio._

object Main extends ZIOAppDefault {
  val retries: FiberRef[Int] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(3)
    }

  def run =
    for {
      _ <- ZIO.unit
      f1 = retries.set(10).debug("set 10").delay(2.seconds)
      f2 = retries.set(5).debug("set 5")
      _ <- f1 <&> f2
      _ <- retries.get.debug("final retries value")
    } yield ()

}
```

- 이 프로그램의 출력

```scala
set 5: ()
set 10: ()
final retries value: 10
```

- 출력에서 확인 가능한 내용
  - `f1` 워크플로우를 지연(delay)시켰기 때문에 부모에 마지막으로 조인하는 자식 파이버가 됨 → 그 값 10이 최종 값이 됨
  - 병합 시 자식의 값이 부모의 값을 대체하는 것이 기본 규칙이기 때문

##### 문제(The Problem)

- 프로그램을 개발하다 보면 `intervals` 같은 추가 구성을 더하고 싶은 경우 발생
  - `intervals` 구성을 담는 또 다른 `FiberRef`를 쉽게 포함 가능

```scala
import zio._

object Main extends ZIOAppDefault {

  val retries: FiberRef[Int] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(2)
    }

  val intervals: FiberRef[Int] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(3)
    }

  def run =
    for {
      _ <- retries.set(5) <&> intervals.set(3)
      _ <- retries.get.debug("final retries value")
      _ <- intervals.get.debug("final intervals value")
    } yield ()

}
```

- 이 프로그램의 출력

```scala
final retries value: 5
final intervals value: 3
```

- 더 많은 `FiberRef`를 도입함으로써 기저 구성 값들을 아무 문제 없이 동시적으로 갱신 가능함을 확인

- 두 구성이 서로 연관되어 있으므로 `Map[String, Int]`를 사용하는 하나의 데이터 타입으로 묶는 것이 유익할 수 있음
  - 이 접근 방식은 재시도 구성을 두 개의 별개 `FiberRef`로 인코딩할 필요를 없앰

```scala
import zio._

object Main extends ZIOAppDefault {
  val retryConfig: FiberRef[Map[String, Int]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(
        Map(
          "retries" -> 3,
          "intervals" -> 2
        )
      )
    }

  def withRetry(n: Int) = retryConfig.update(_.updated("retries", n))

  def withIntervals(n: Int) = retryConfig.update(_.updated("intervals", n))

  def run =
    for {
      - <- withRetry(5) <&> withIntervals(3)
      _ <- retryConfig.get.debug("retryConfig")
    } yield ()

}
```

- 안타깝게도 이 변경으로 출력이 의도한 결과와 다름

```scala
retryConfig: Map(retries -> 3, intervals -> 3)
```

- `intervals`는 성공적으로 갱신되었으나 `retries`는 변경되지 않음
  - 두 파이버가 동일한 맵 전체를 덮어쓰기 때문에 최종 값이 손상(corruption)됨
  - 이 시나리오에서의 갱신 과정

```scala
Parent fiber: Map(retries -> 3, intervals -> 2)
Left fiber:   Map(retries -> 5, intervals -> 2)
right fiber:  Map(retries -> 3, intervals -> 3)

Parent fiber joins the left fiber:  Map(retries -> 5, intervals -> 2)
Parent fiber joins the right fiber: Map(retries -> 3, intervals -> 3)
```

- 이처럼 `retries` 값은 오른쪽 파이버가 부모 파이버에 조인할 때 덮어써져 잘못된 값으로 끝남

- 이 문제를 해결하려면 갱신을 합성(compose)할 방법 필요
  - '`retries`를 5로 갱신한 다음 `intervals`를 3으로 갱신', 혹은 그 반대를 표현할 수 있어야 함
  - 바로 여기서 합성 가능한 갱신과 패치 이론(patch theory)이 등장

##### Differ와 Patch (Differ and Patch)

- 코드에 들어가기 전에 몇 가지 용어 확인

```scala
trait Differ[Value, Patch] {
  def combine(first: Patch, second: Patch): Patch
  def diff(oldValue: Value, newValue: Value): Patch
  def empty: Patch
  def patch(patch: Patch)(oldValue: Value): Value
}
```

- `Differ[Value, Patch]`의 인스턴스를 통해 가능한 것
  1. 타입 `Value`의 두 값을 diff하여 `Patch` 생성 → `Patch`는 한 값에서 다른 값으로의 수정을 나타내는 데이터 타입, 두 값 사이의 "diff"로 볼 수 있음
  2. combine 함수로 두 `Patch`를 하나의 `Patch`로 결합 → 갱신을 합성하는 데 유용
     - 예: `retries`를 5로 갱신하는 `Patch`와 `intervals`를 3으로 갱신하는 `Patch`를 `retries`와 `intervals` 둘 다 갱신하는 하나의 `Patch`로 결합 가능
  3. patch 함수로 `Patch`를 값에 적용하여 새 값 생성
  4. empty 함수는 아무 변경도 나타내지 않는 `Patch` 반환

- 데이터 타입에 대한 `Differ`를 구현하려면 이 4가지 함수 구현 필요
  - `Differ[Value, Patch]`의 어떤 `Differ` 값에든 결부된 다섯 가지 법칙(law)
    1. `combine` 함수는 결합 법칙(associative)을 따름 → 두 패치를 결합한 다음 그 결과를 세 번째 패치와 결합하는 것은, 첫 번째 패치를 두 번째와 세 번째 패치의 결합과 결합하는 것과 같음
    2. 패치를 빈 패치(empty patch)와 결합하는 것은 그 패치 자체와 같음
    3. 한 값을 자기 자신과 diff하면 빈 패치가 생성됨
    4. 두 값을 diff한 다음 결과 패치로 첫 번째 값을 패치하면 두 번째 값이 됨
    5. 값을 빈 패치로 패치하면 원래 값이 됨

- ZIO는 더 복잡한 데이터 타입에 대한 `Differ` 인스턴스를 만드는 데 도움이 되는 유틸리티 포함
  - `Map`, `Set`, `Chunk` 같은 일반적인 데이터 타입에 대한 `Differ` 인스턴스
  - `Differ.update[A]`: 값을 새 값으로 설정하는 함수를 반환함으로써 두 값을 diff하는 differ를 구성
  - `Differ.map`: 맵의 값들을 diff할 줄 아는 differ로부터 맵 differ를 구성
  - `Differ#zip`: 두 differ를 결합하여 값들의 튜플(tuple)에서 동작하는 단일 differ를 생성
  - `Differ#orElseEither`: 두 differ를 결합하여 두 값의 `Either`에서 동작하는 단일 differ를 생성
  - `Differ#transform`: 두 함수(Value1을 Value2로 변환하는 함수, Value2를 Value1로 변환하는 함수)를 제공함으로써, 한 타입(Value1)의 differ를 다른 타입(Value2)의 differ로 변환 가능

- `Map[String, Int]` 타입의 `FiberRef`인 `retryConfig`에 대한 `Differ` 구현

```scala
import zio._

val differ   = Differ.map[String, Int, Int => Int](Differ.update[Int])
val patch1   = differ.diff(Map("retries" -> 3), Map("retries" -> 5))
val patch2   = differ.diff(Map("intervals" -> 2), Map("intervals" -> 3))
val combined = differ.combine(patch1, patch2)
val result   = differ.patch(combined)(Map("retries" -> 3, "intervals" -> 2))
println(result)
```

- 출력

```scala
Map(retries -> 5, intervals -> 3)
```

##### 첫 번째 해결책: Map[String, Int] 데이터 타입에 대한 합성 가능한 갱신

- 이전 절에서 합성 가능한 갱신을 사용해 `retries`와 `intervals` 값을 성공적으로 갱신
  - 이제 이 differ를 사용해 `FiberRef`의 갱신을 합성 가능하게(composable) 만들 수 있음

```scala
import zio._

object Main extends ZIOAppDefault {

  val differ = Differ.map[String, Int, Int => Int](Differ.update[Int])

  val retryConfig: FiberRef[Map[String, Int]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.makePatch[Map[String, Int], Differ.MapPatch[
        String,
        Int,
        Int => Int
      ]](
        Map(
          "retries" -> 3,
          "intervals" -> 2
        ),
        differ = differ,
        fork0 = differ.empty
      )
    }

  def withRetry(n: Int): UIO[Unit] =
    retryConfig.update(_.updated("retries", n))

  def withIntervals(n: Int): UIO[Unit] =
    retryConfig.update(_.updated("intervals", n))

  def run = {
    for {
      _ <- withRetry(5) <&> withIntervals(3)
      _ <- retryConfig.get.debug("retryConfig")
    } yield ()

  }
}
```

- 출력

```scala
retryConfig: Map(retries -> 5, intervals -> 3)
```

> 참고: `Differ`의 `combine` 연산이 결합 법칙을 따르므로 갱신의 순서는 결과를 바꾸지 않음. 이는 여러 파이버가 조인할 때 동일한 값을 갱신하지만 조인 순서가 결정론적이지 않은 동시성 환경에서, 합성 가능한 갱신의 매우 중요한 속성

##### 두 번째 해결책: RetryConfig 케이스 클래스에 대한 합성 가능한 갱신

- 이 예시를 한 단계 더 발전시켜, 스칼라 케이스 클래스(case class)를 사용해 `RetryConfig`에 대한 타입 안전한 구성 데이터 타입 생성 가능

```scala
case class RetryConfig(
    retries: Int,
    intervals: Int
)
```

- `Differ#transform` 함수를 사용해 `RetryConfig`에 대한 `Differ` 생성 가능

```scala
import zio._

val differ: Differ[RetryConfig, (Int => Int, Int => Int)] =
  Differ
    .update[Int]
    .zip(Differ.update[Int])
    .transform(
      { case (x, y) => RetryConfig.apply(x, y) },
      retryConfig => (retryConfig.retries, retryConfig.intervals)
    )
```

- 이제 앞서와 마찬가지로 이 `differ`를 사용해 새 `FiberRef`의 갱신을 합성 가능하게 만들 수 있음

```scala
import zio._

object Main extends ZIOAppDefault {

  val retryConfig: FiberRef[RetryConfig] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.makePatch[RetryConfig, (Int => Int, Int => Int)](
        initialValue0 = RetryConfig(
          retries = 3,
          intervals = 2
        ),
        differ = differ,
        fork0 = differ.empty
      )
    }

  def withRetry(n: Int) = retryConfig.update(_.copy(retries = n))

  def withIntervals(n: Int) = retryConfig.update(_.copy(intervals = n))

  def run =
    for {
      _ <- withRetry(5) <&> withIntervals(3)
      _ <- retryConfig.get.debug("retryConfig")
    } yield ()
    
}
```

---

### 8. ZState: 환경 기반 상태(ZState)

- `ZState[S]`는 효과를 실행하는 동안 읽고 쓸 수 있는 타입 `S`의 값을 모델링
  - `FiberRef`와 환경 타입(environment type) 위에 구축된 고수준 구성 요소(higher-level construct)
  - 전통적으로 State 모나드 변환기(state monad transformer)를 사용했을 법한 곳에서 ZIO를 사용할 수 있도록 지원

- `ZState`를 사용하는 간단한 예시

```scala
import zio._

import java.io.IOException

object ZStateExample extends zio.ZIOAppDefault {
  val myApp: ZIO[ZState[Int], IOException, Unit] = for {
    s <- ZIO.service[ZState[Int]]
    _ <- s.update(_ + 1)
    _ <- s.update(_ + 2)
    state <- s.get
    _ <- Console.printLine(s"current state: $state")
  } yield ()

  def run = ZIO.stateful(0)(myApp)
}
```

- `ZState`를 다루는 관용적인 방법
  - 환경의 일부로 취급하면서 `ZIO`에 정의된 연산자로 `ZState`에 접근
  - 마지막으로 `ZIO.stateful` 연산자로 초기 상태를 할당(allocate)

- `ZState`는 주로 환경의 일부로 사용됨 → `Int` 같은 범용 타입보다 `MyState`처럼 전용 상태 타입 `S`를 정의하여 모호성(ambiguity)을 피하는 것이 권장됨

```scala
import zio._

import java.io.IOException

final case class MyState(counter: Int)

object ZStateExample extends zio.ZIOAppDefault {

  val myApp: ZIO[ZState[MyState], IOException, Unit] =
    for {
      counter <- ZIO.service[ZState[MyState]]
      _ <- counter.update(state => state.copy(counter = state.counter + 1))
      _ <- counter.update(state => state.copy(counter = state.counter + 2))
      state <- counter.get
      _ <- Console.printLine(s"Current state: $state")
    } yield ()

  def run = ZIO.stateful(MyState(0))(myApp)
}
```

- `ZIO` 데이터 타입에는 `ZState`를 환경으로 다루는 데 도움이 되는 헬퍼 메서드도 존재: `ZIO.updateState`, `ZIO.getState`, `ZIO.getStateWith` 등

```scala
import zio._

import java.io.IOException

final case class MyState(counter: Int)

val myApp: ZIO[ZState[MyState], IOException, Int] =
  for {
    _ <- ZIO.updateState[MyState](state => state.copy(counter = state.counter + 1))
    _ <- ZIO.updateState[MyState](state => state.copy(counter = state.counter + 2))
    state <- ZIO.getStateWith[MyState](_.counter)
    _ <- Console.printLine(s"Current state: $state")
  } yield state
```

- `ZState`는 `FiberRef` 데이터 타입 위에 구축되어 있으므로 `FiberRef`의 동작을 그대로 상속
  - 예: 파이버가 부모 파이버에 조인할 때 그 상태는 부모의 상태와 병합됨

```scala
import zio._

case class MyState(counter: Int)

object ZStateExample extends ZIOAppDefault {
  val myApp = for {
    _ <- ZIO.updateState[MyState](state => state.copy(counter = state.counter + 1))
    fiber <-
      (for {
        _ <- ZIO.updateState[MyState](state => state.copy(counter = state.counter + 1))
        state <- ZIO.getState[MyState]
        _ <- Console.printLine(s"Current state inside the forked fiber: $state")
      } yield ()).fork
    _ <- ZIO.updateState[MyState](state => state.copy(counter = state.counter + 5))
    state1 <- ZIO.getState[MyState]
    _ <- Console.printLine(s"Current state before merging the fiber: $state1")
    _ <- fiber.join
    state2 <- ZIO.getState[MyState]
    _ <- Console.printLine(s"The final state: $state2")
  } yield ()

  def run =
    ZIO.stateful(MyState(0))(myApp)
}
```

- 이 코드를 실행한 출력

```
Current state before merging the fiber: MyState(6)
Current state inside the forked fiber: MyState(2)
The final state: MyState(2)
```

---

### 9. 참고 자료

- [State Management in ZIO (Introduction)](https://zio.dev/reference/state-management/)
- [State Management Using Recursion](https://zio.dev/reference/state-management/recursion)
- [Global Shared State Using Ref](https://zio.dev/reference/state-management/global-shared-state)
- [Ref](https://zio.dev/reference/concurrency/ref)
- [Ref.Synchronized](https://zio.dev/reference/concurrency/refsynchronized)
- [Fiber-local State](https://zio.dev/reference/state-management/fiber-local-state)
- [FiberRef: Introduction to Fiber-local Storage](https://zio.dev/reference/state-management/fiberref)
- [ZState](https://zio.dev/reference/state-management/zstate)
