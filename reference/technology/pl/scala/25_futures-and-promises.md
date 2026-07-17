# Future와 Promise를 이용한 비동기 프로그래밍

> **원문:** https://docs.scala-lang.org/overviews/core/futures.html

---

## 목차

1. [개요](#1-개요)
2. [ExecutionContext — Future를 실행하는 스레드 풀](#2-executioncontext--future를-실행하는-스레드-풀)
3. [Future 만들기와 상태 변화](#3-future-만들기와-상태-변화)
4. [콜백: onComplete와 foreach](#4-콜백-oncomplete와-foreach)
5. [함수형 조합: map, flatMap, for, filter](#5-함수형-조합-map-flatmap-for-filter)
6. [실패 처리: recover, recoverWith, fallbackTo, failed](#6-실패-처리-recover-recoverwith-fallbackto-failed)
7. [andThen — 순서가 보장된 부수효과](#7-andthen--순서가-보장된-부수효과)
8. [여러 Future 묶기: zip, firstCompletedOf, sequence/traverse](#8-여러-future-묶기-zip-firstcompletedof-sequencetraverse)
9. [블로킹: Await와 blocking](#9-블로킹-await와-blocking)
10. [Promise — Future를 직접 완료시키는 도구](#10-promise--future를-직접-완료시키는-도구)
11. [예외 처리 규칙](#11-예외-처리-규칙)
12. [Duration 간단히](#12-duration-간단히)
13. [정리](#13-정리)

---

## 1. 개요

Scala의 `Future[T]`는 **아직 존재하지 않을 수도 있는 값을 담는 그릇**입니다. 오래 걸리는 계산이나 I/O를 별도 스레드에 맡기고, 호출한 쪽은 결과를 기다리며 멈추지 않고 다음 일을 계속할 수 있게 해줍니다.

핵심 규칙은 단순합니다.

- `Future[T]`는 **성공(값 T)** 아니면 **실패(예외)** 로 **딱 한 번만** 완료됩니다.
- 한 번 완료되면 그 상태는 **바뀌지 않습니다**(불변).
- Future 자체는 "결과를 읽기만 하는" 쪽이고, 그 결과를 채워 넣는 역할은 짝꿍인 **`Promise`** 가 맡습니다.

> 💡 **왜 필요한가 — 콜백 지옥 대신 값처럼 다루기**
>
> 전통적인 비동기 코드는 콜백 함수를 계속 중첩시켜야 해서("콜백 지옥") 읽기도 조합하기도 어렵습니다. `Future`는 "아직 안 왔지만 곧 올 값"을 `map`, `flatMap` 같은 익숙한 컬렉션 연산으로 조합할 수 있게 해서, 비동기 코드를 동기 코드와 비슷한 느낌으로 작성하게 해줍니다.

---

## 2. ExecutionContext — Future를 실행하는 스레드 풀

`Future { ... }` 블록은 어딘가의 스레드에서 실행되어야 하는데, "어디서 실행할지"를 결정하는 것이 **`ExecutionContext`** 입니다. Java의 `Executor`/스레드 풀과 비슷한 역할입니다.

```scala
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global   // 기본 제공 컨텍스트

val f: Future[Int] = Future {
  Thread.sleep(1000)
  21 * 2
}
```

- `ExecutionContext.global`은 **ForkJoinPool** 기반이며, 기본 병렬도는 **가용 CPU 코어 수**입니다.
- 필요하면 자바 `Executor`를 감싸서 직접 만들 수도 있습니다: `ExecutionContext.fromExecutor(executor)`.
- Scala 3에서는 `given ExecutionContext = ...`로 컨텍스트를 스코프에 두는 것이 관용적입니다(implicit → given 전환은 `02_contextual_givens_using.md` 참고).

VM 옵션으로 스레드 풀 크기를 조절할 수 있습니다.

| 옵션 | 의미 | 기본값 |
|---|---|---|
| `scala.concurrent.context.minThreads` | 최소 스레드 수 | 1 |
| `scala.concurrent.context.numThreads` | 목표 스레드 수 | 코어 수 |
| `scala.concurrent.context.maxThreads` | 최대 스레드 수 | 코어 수 |

---

## 3. Future 만들기와 상태 변화

`Future { 표현식 }`은 표현식을 (암시적으로 주어진) `ExecutionContext` 위에서 즉시 비동기로 실행하기 시작합니다.

```scala
val price: Future[Int] = Future {
  fetchStockPrice("AAPL")   // 오래 걸릴 수 있는 작업
}
```

상태는 다음 세 가지 중 하나입니다.

- **미완료(not yet completed)** — 아직 실행 중
- **성공적으로 완료(completed with a value)**
- **실패로 완료(completed with an exception)**

한 번 완료 상태에 들어가면 되돌아가지 않습니다. "성공했다가 나중에 실패로 바뀐다" 같은 일은 없습니다.

---

## 4. 콜백: onComplete와 foreach

Future가 완료되었을 때 실행할 콜백을 등록할 수 있습니다.

```scala
import scala.util.{Success, Failure}

price.onComplete {
  case Success(v)  => println(s"가격: $v")
  case Failure(ex) => println(s"실패: ${ex.getMessage}")
}

price.foreach(v => println(s"가격: $v"))   // 성공한 경우만 처리
```

- `onComplete`는 성공/실패 **양쪽 모두** 다룹니다.
- `foreach`는 **성공했을 때만** 콜백을 실행하고, 실패하면 조용히 무시합니다.

> ⚠️ **짚고 넘어가기 — 콜백 실행 순서는 보장되지 않는다**
>
> 같은 Future에 콜백을 여러 개 등록해도 **어떤 순서로 실행될지, 어느 스레드에서 실행될지는 정해져 있지 않습니다.** "언젠가는 실행된다"만 보장됩니다. 순서가 필요하면 뒤에 나오는 `andThen`을 쓰세요.

---

## 5. 함수형 조합: map, flatMap, for, filter

Future는 컬렉션처럼 `map`/`flatMap`으로 값을 가공할 수 있고, 이 연산들은 **원본을 바꾸지 않고 새 Future를 반환**합니다.

```scala
val doubled: Future[Int] = price.map(_ * 2)                  // 성공 값만 변환

val withTax: Future[Int] = price.flatMap { p =>
  Future(p + calcTax(p))                                      // Future를 반환하는 다음 단계로 연결
}
```

- `map` : 성공 값을 다른 값으로 바꿈. 실패는 그대로 전파.
- `flatMap` : "이 값을 받아서 또 다른 Future를 만드는" 다음 단계와 연결. 여러 비동기 작업을 순서대로 이어붙일 때 사용.
- `filter`/`withFilter` : 조건을 만족하지 않으면 `NoSuchElementException`으로 실패.

여러 Future를 조합할 때는 **for-컴프리헨션**이 `flatMap` 체인보다 훨씬 읽기 쉽습니다.

```scala
val usd: Future[Int] = Future(price1)
val eur: Future[Int] = Future(price2)

val total: Future[Int] = for {
  p1 <- usd
  p2 <- eur
  if p1 > 0
} yield p1 + p2
```

> ⚠️ **짚고 넘어가기 — for 안의 Future는 이미 각자 실행 중이다**
>
> `usd`, `eur`는 `for` 블록에 들어가기 **전에 이미 만들어졌으므로 병렬로 실행됩니다.** 반대로 `for` 블록 안에서 `Future(...)`를 새로 만들면 앞 줄의 결과가 나온 뒤에야 시작되어 **순차 실행**이 됩니다. "병렬로 돌리고 싶다면 Future를 미리 만들어 두라"는 것이 흔히 하는 실수 포인트입니다.

---

## 6. 실패 처리: recover, recoverWith, fallbackTo, failed

| 메서드 | 역할 |
|---|---|
| `recover { case ... => 값 }` | 특정 예외를 **성공 값**으로 바꿔치기 |
| `recoverWith { case ... => Future(...) }` | 특정 예외를 **다른 Future**로 대체 |
| `fallbackTo(다른Future)` | 실패하면 인자로 준 Future의 결과를 대신 사용 |
| `failed` | 성공/실패를 뒤집는 투영(projection). 원래 실패했을 때만 그 예외를 값으로 받음 |

```scala
val safePrice: Future[Int] =
  price.recover { case _: TimeoutException => -1 }

val withBackup: Future[Int] = price.fallbackTo(cachedPrice)

price.failed.foreach(ex => log(ex))   // price가 실패했을 때만 실행
```

- `recover`는 **동기 값**을 반환(예외 → 값), `recoverWith`는 **또 다른 Future**를 반환(예외 → Future)한다는 점이 `map`/`flatMap`의 차이와 대응됩니다.

---

## 7. andThen — 순서가 보장된 부수효과

`andThen`은 콜백을 실행하지만, **결과는 그대로 다음 `andThen`에 전달**되도록 체이닝할 수 있고 실행 순서도 등록한 순서를 따릅니다. 로깅처럼 "본 결과는 안 바꾸되 순서대로 뭔가 하고 싶을 때" 씁니다.

```scala
price
  .andThen { case Success(v) => log(s"조회 성공: $v") }
  .andThen { case _          => cleanupResources() }
```

일반 `onComplete`를 여러 번 걸면 순서가 뒤섞일 수 있지만, `andThen` 체인은 **등록한 순서대로 실행**된다는 점이 다릅니다.

---

## 8. 여러 Future 묶기: zip, firstCompletedOf, sequence/traverse

| 상황 | 도구 |
|---|---|
| 두 Future를 **모두** 기다려 튜플로 묶기 | `f1.zip(f2)` |
| 여러 Future 중 **가장 먼저** 끝난 것만 취하기 | `Future.firstCompletedOf(Seq(f1, f2, f3))` |
| `List[Future[A]]`를 `Future[List[A]]`로 뒤집기 | `Future.sequence(futures)` |
| 리스트를 순회하며 각각 Future로 변환한 뒤 한 번에 묶기 | `Future.traverse(list)(f)` |

```scala
val f1 = Future(fetchA())
val f2 = Future(fetchB())

val both: Future[(A, B)] = f1.zip(f2)                        // 둘 다 성공해야 성공

val fastest: Future[Int] = Future.firstCompletedOf(Seq(f1, f2))

val many: Future[List[Int]] = Future.sequence(List(f1, f2, f3))
```

`sequence`/`traverse`는 병렬로 실행 중인 여러 Future의 결과를 한꺼번에 모을 때 특히 유용합니다.

---

## 9. 블로킹: Await와 blocking

Future는 원래 "기다리지 않는" 것이 목적이지만, 프로그램의 진입점(`main`)이나 테스트 코드처럼 **어딘가에서는 결과를 꺼내야** 합니다. 이때 `Await`을 씁니다.

```scala
import scala.concurrent.Await
import scala.concurrent.duration._

val value = Await.result(price, 3.seconds)   // 완료까지 블로킹, 실패하면 예외를 던짐
Await.ready(price, 3.seconds)                // 완료만 기다림(결과는 안 꺼냄), 실패해도 예외 안 던짐
```

Future 내부에서 어쩔 수 없이 블로킹 작업(예: 락 대기, 동기 I/O)을 해야 한다면, `blocking { ... }`으로 감싸 스레드 풀에 "지금 이 스레드가 막혀 있다"고 알려줍니다. 실제로 어떻게 반응할지는 `ExecutionContext` 구현에 달려 있는데, `ExecutionContext.global`은 이 신호를 받아 필요하면 스레드를 더 만들어 다른 작업이 굶지 않게 하지만, 스레드 풀을 고정 크기로 쓰는 다른 `ExecutionContext`는 아무 동작도 하지 않을 수 있습니다.

```scala
Future {
  blocking {
    legacyBlockingCall()
  }
}
```

> ⚠️ **짚고 넘어가기 — 블로킹은 최후의 수단**
>
> `Await`이나 `blocking`을 남발하면 애초에 Future를 쓰는 이유(스레드를 놀리지 않기)가 사라집니다. 스레드 풀이 고갈되어 데드락처럼 보이는 현상도 생길 수 있으므로, 가능하면 `map`/`flatMap`/콜백으로 끝까지 비동기로 처리하고 `Await`은 프로그램의 최상단이나 테스트에서만 쓰는 것이 안전합니다.

---

## 10. Promise — Future를 직접 완료시키는 도구

`Future { ... }`는 "무엇을 실행할지"와 "결과가 어떻게 채워질지"가 한 덩어리이지만, **콜백 기반 API처럼 나중에 외부에서 결과를 채워 넣어야 하는 상황**도 있습니다. 이럴 때 `Promise`를 씁니다.

```scala
import scala.concurrent.Promise

val p = Promise[Int]()
val f: Future[Int] = p.future     // 아직 미완료 상태인 Future를 미리 꺼내 둠

// ... 나중에, 어디선가 ...
p.success(42)          // f가 성공(42)으로 완료됨
// 혹은
p.failure(new RuntimeException("실패"))
```

| 메서드 | 동작 |
|---|---|
| `success(v)` / `failure(e)` | 성공/실패로 완료. **이미 완료된 Promise에 다시 호출하면 예외** |
| `complete(Try[T])` | `Success`/`Failure` 값으로 한 번에 완료 |
| `trySuccess` / `tryFailure` / `tryComplete` | 이미 완료돼 있어도 예외 대신 `Boolean`으로 성공 여부만 알려줌 |
| `completeWith(다른Future)` | 다른 Future가 끝나는 대로 그 결과를 그대로 이어받아 완료 |

`Promise`는 "Future를 만드는 공장의 스위치"라고 보면 됩니다 — 콜백 기반 라이브러리(예: 네트워크 클라이언트의 `onResult` 콜백)를 Future 기반 API로 감쌀 때 전형적으로 사용합니다.

```scala
def legacyAsyncCall(onResult: Try[Int] => Unit): Unit = ???

def toFuture: Future[Int] =
  val p = Promise[Int]()
  legacyAsyncCall(p.complete)
  p.future
```

---

## 11. 예외 처리 규칙

`Future { ... }` 블록 안에서 예외가 발생하면 기본적으로 그 예외가 Future의 실패 값으로 담깁니다. 다만 몇 가지는 특별 취급됩니다.

- `scala.runtime.NonLocalReturnControl` (함수 안에서 `return`을 쓸 때 내부적으로 쓰이는 제어 예외) → 그 안에 담긴 반환값을 그대로 꺼내 **성공** 값으로 처리
- `InterruptedException`, `Error`, `ControlThrowable` → `ExecutionException`으로 한 번 감싸서 전달
- `NonFatal`로 분류되지 않는 **치명적(fatal) 예외**(예: `OutOfMemoryError`)는 Future 안에 담기지 않고 실행 중이던 스레드에서 그대로 다시 던져집니다.

즉 "어떤 예외든 조용히 Future 실패로 삼켜진다"고 가정하면 안 되고, 정말 치명적인 오류는 여전히 프로세스를 흔들 수 있다는 뜻입니다.

---

## 12. Duration 간단히

`Await`이나 타임아웃 API에서 쓰는 시간 단위는 `scala.concurrent.duration.Duration`입니다.

```scala
import scala.concurrent.duration._

val d1 = Duration(100, MILLISECONDS)
val d2 = 100.millis          // 리터럴 확장 메서드로 더 간결하게
val d3 = 1.2.seconds

val Duration(length, unit) = d2   // 패턴 매칭으로 분해도 가능
```

덧셈·비교 같은 산술 연산과 단위 변환을 지원해서, 타임아웃 값을 조합하거나 로깅할 때 편리합니다.

---

## 13. 정리

| 개념 | 한 줄 요약 |
|---|---|
| `Future[T]` | 언젠가 성공 또는 실패로 딱 한 번 완료되는 비동기 값 |
| `ExecutionContext` | Future가 실제로 실행되는 스레드 풀 |
| `onComplete` / `foreach` | 완료 후 실행할 콜백 등록 (순서 보장 없음) |
| `map` / `flatMap` / `for` | 값을 변환하거나 여러 Future를 순서대로 엮음 |
| `recover` / `recoverWith` / `fallbackTo` | 실패를 값 또는 대체 Future로 흡수 |
| `andThen` | 순서가 보장된 부수효과 체이닝 |
| `zip` / `firstCompletedOf` / `sequence` | 여러 Future를 동시에 묶어 처리 |
| `Await.result` / `Await.ready` | 결과를 동기적으로 기다림(최후의 수단) |
| `blocking { ... }` | 불가피한 블로킹 구간을 스레드 풀에 알림 |
| `Promise` | 외부에서 직접 Future를 완료시키는 손잡이 |

Future/Promise는 "값을 반환하는 계산"이라는 Scala의 표현식 중심 사고(00번 문서)를 **비동기 세계로 그대로 확장한 것**입니다. 동기 코드에서 `val`에 값을 담듯, 비동기 코드에서는 `Future`에 "곧 담길 값"을 담고 `map`/`flatMap`으로 조립한다고 생각하면 자연스럽습니다.

---

## 참고 자료

- [Scala Futures and Promises (공식 문서)](https://docs.scala-lang.org/overviews/core/futures.html)
- [scala.concurrent.Future API](https://www.scala-lang.org/api/current/scala/concurrent/Future.html)
- [scala.concurrent.Promise API](https://www.scala-lang.org/api/current/scala/concurrent/Promise.html)
