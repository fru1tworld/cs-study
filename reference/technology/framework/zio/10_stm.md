# 소프트웨어 트랜잭셔널 메모리(STM)

> 원본: https://zio.dev/reference/stm/

---

## 목차

1. [STM 개요(Software Transactional Memory)](#1-stm-개요software-transactional-memory)
2. [락 기반 동시성의 한계(The Problem with Locks)](#2-락-기반-동시성의-한계the-problem-with-locks)
3. [STM이 동작하는 방식(How STM Works)](#3-stm이-동작하는-방식how-stm-works)
4. [STM의 ACID 속성(ACID Properties)](#4-stm의-acid-속성acid-properties)
5. [ZSTM 타입과 별칭(ZSTM Type and Aliases)](#5-zstm-타입과-별칭zstm-type-and-aliases)
6. [STM의 장점과 주의사항(Advantages and Caveats)](#6-stm의-장점과-주의사항advantages-and-caveats)
7. [TRef — 트랜잭셔널 참조(Transactional Reference)](#7-tref--트랜잭셔널-참조transactional-reference)
8. [TArray — 트랜잭셔널 배열(Transactional Array)](#8-tarray--트랜잭셔널-배열transactional-array)
9. [TSet — 트랜잭셔널 집합(Transactional Set)](#9-tset--트랜잭셔널-집합transactional-set)
10. [TMap — 트랜잭셔널 맵(Transactional Map)](#10-tmap--트랜잭셔널-맵transactional-map)
11. [TQueue — 트랜잭셔널 큐(Transactional Queue)](#11-tqueue--트랜잭셔널-큐transactional-queue)
12. [TPriorityQueue — 트랜잭셔널 우선순위 큐(Transactional Priority Queue)](#12-tpriorityqueue--트랜잭셔널-우선순위-큐transactional-priority-queue)
13. [TPromise — 트랜잭셔널 프로미스(Transactional Promise)](#13-tpromise--트랜잭셔널-프로미스transactional-promise)
14. [TReentrantLock — 재진입 락(Reentrant Lock)](#14-treentrantlock--재진입-락reentrant-lock)
15. [TSemaphore — 트랜잭셔널 세마포어(Transactional Semaphore)](#15-tsemaphore--트랜잭셔널-세마포어transactional-semaphore)
16. [참고 자료](#16-참고-자료)

---

## 1. STM 개요(Software Transactional Memory)

**소프트웨어 트랜잭셔널 메모리**(Software Transactional Memory, STM)는 여러 메모리 연산을 하나의 트랜잭션 안에서 원자적으로 실행할 수 있게 해 주는 모듈식이고 조합 가능한 동시성 데이터 구조입니다.

STM은 데이터베이스의 트랜잭션 개념을 메모리 연산에 적용한 것입니다. 여러 메모리 읽기/쓰기 연산을 묶어 하나의 트랜잭션으로 만들면, 전부 성공하거나(커밋) 전부 실패하여 아무 효과도 남기지 않는 것(롤백)이 보장됩니다.

ZIO의 STM은 락을 직접 다루지 않고도 동시성 문제를 안전하게 해결할 수 있으며, **조합 가능성**(composability)을 핵심 장점으로 갖습니다. 여러 작은 트랜잭션을 결합하여 더 큰 하나의 원자적 트랜잭션을 만들 수 있습니다.

---

## 2. 락 기반 동시성의 한계(The Problem with Locks)

전통적인 가변 참조(`Ref`)와 락만으로는 동시성 문제를 조합 가능한 방식으로 해결할 수 없습니다.

### 경쟁 상태(Race Condition) 문제

다음과 같은 단순한 증가(increment) 함수를 생각해 봅시다.

```scala
def inc(counter: Ref[Int], amount: Int) = for {
  c <- counter.get
  _ <- counter.set(c + amount)
} yield c
```

이 함수에 대해 10개의 동시 파이버를 실행하더라도, 결과가 항상 10이 된다고 보장할 수 없습니다. 카운터 값을 읽는 시점과 새 값을 설정하는 시점 사이에 다른 파이버가 끼어들어 값을 변경할 수 있기 때문입니다.

### 조합 불가능성(Composability Failure) 문제

고전적인 송금(money transfer) 예제는 락 기반 접근의 조합 실패를 보여 줍니다.

```scala
def deposit(accountBalance: Ref[Int], amount: Int) =
  accountBalance.update(_ + amount)

def withdraw(accountBalance: Ref[Int], amount: Int) =
  accountBalance.update(_ - amount)

def transfer(from: Ref[Int], to: Ref[Int], amount: Int) = for {
  _ <- withdraw(from, amount)
  _ <- deposit(to, amount)
} yield ()
```

`withdraw`와 `deposit`이 각각 원자적이더라도, 파이버 전반에 걸쳐 이 두 연산을 원자적으로 조합할 수는 없습니다. 출금과 입금 사이에 다른 파이버가 끼어들면 일관성이 깨질 수 있습니다.

### STM을 통한 해결

`Ref`를 **`TRef`**(Transactional Reference, 트랜잭셔널 참조)로 바꾸고 `STM`을 사용하면 이 문제를 해결할 수 있습니다.

```scala
def withdraw(accountBalance: TRef[Int], amount: Int): STM[String, Unit] =
  for {
    balance <- accountBalance.get
    _ <- if (balance < amount)
      STM.fail("Insufficient funds in you account")
    else
      accountBalance.update(_ - amount)
  } yield ()

def deposit(accountBalance: TRef[Int], amount: Int): STM[Nothing, Unit] =
  accountBalance.update(_ + amount)

def transfer(from: TRef[Int], to: TRef[Int], amount: Int):
    IO[String, Unit] =
  STM.atomically {
    for {
      _ <- withdraw(from, amount)
      _ <- deposit(to, amount)
    } yield ()
  }
```

`STM.atomically { ... }`로 감싼 블록 전체가 하나의 원자적 트랜잭션이 되므로, 출금과 입금은 함께 커밋되거나 함께 롤백됩니다.

---

## 3. STM이 동작하는 방식(How STM Works)

STM은 **낙관적 동시성(optimistic concurrency)** 모델을 사용합니다. 충돌이 드물게 발생한다고 가정하고, 비관적 락 대신 커밋 시점에만 일관성을 검증합니다.

STM 트랜잭션은 세 단계로 동작합니다.

### 1단계: 트랜잭션 시작(Starting Transaction)

런타임 시스템은 트랜잭션 로그를 추적하기 위한 가상 공간을 생성합니다. 이 로그는 읽기와 잠정적 쓰기를 기록하면서 구축됩니다.

### 2단계: 가상 실행(Virtual Execution)

시스템은 공유 메모리를 수정하지 않은 채 읽기 로그와 쓰기 로그를 유지합니다. atomic 블록 안의 모든 동작은 즉시 실행되는 것이 아니라 가상 세계에서 실행됩니다.

### 3단계: 커밋 단계(Commit Phase)

트랜잭션 완료 시점에 시스템은 트랜잭션에 관여한 트랜잭셔널 변수들이 다른 스레드에 의해 수정되었는지 검사합니다. 충돌이 감지되어 가정이 무효화되었다면 해당 트랜잭션을 폐기하고 처음부터 다시 시도합니다.

---

## 4. STM의 ACID 속성(ACID Properties)

STM은 데이터베이스 트랜잭션의 ACID 속성을 메모리 연산에 맞게 적용합니다. 단, 인메모리 연산이므로 지속성(Durability)은 제외됩니다.

- **원자성(Atomicity)**: 갱신 연산은 전부 실행되거나 전혀 실행되지 않습니다.
- **일관성(Consistency)**: 프로그램 상태에 대한 일관된 뷰를 제공하여, 상태에 대한 모든 참조가 동일한 값을 얻도록 보장합니다.
- **격리성(Isolation)**: 각 트랜잭션은 다른 동시 트랜잭션에 영향을 주지 않습니다.
- **지속성(Durability)**: 인메모리 연산에는 해당되지 않으므로 생략됩니다.

---

## 5. ZSTM 타입과 별칭(ZSTM Type and Aliases)

STM의 기반이 되는 핵심 타입은 `ZSTM[-R, +E, +A]`입니다. 환경(`R`), 오류 타입(`E`), 결과 타입(`A`)을 가진 트랜잭션을 나타내며, `ZIO[-R, +E, +A]`와 구조가 대응됩니다.

자주 사용되는 타입 별칭(type alias)은 다음과 같습니다.

```scala
type RSTM[-R, +A]  = ZSTM[R, Throwable, A]
type URSTM[-R, +A] = ZSTM[R, Nothing, A]
type STM[+E, +A]   = ZSTM[Any, E, A]
type USTM[+A]      = ZSTM[Any, Nothing, A]
type TaskSTM[+A]   = ZSTM[Any, Throwable, A]
```

- `STM[+E, +A]`: 환경이 필요 없고(`Any`), 오류 `E`와 결과 `A`를 갖는 트랜잭션
- `USTM[+A]`: 실패하지 않는(`Nothing`) 트랜잭션
- `TaskSTM[+A]`: `Throwable` 오류를 갖는 트랜잭션
- `RSTM[-R, +A]` / `URSTM[-R, +A]`: 환경 `R`이 필요한 트랜잭션

`STM` 값은 그 자체로는 아무 효과도 일으키지 않는 순수한 기술(description)입니다. 실제로 실행하려면 `.commit` 메서드(또는 `STM.atomically`)를 호출해 `ZIO` 이펙트로 변환해야 합니다.

---

## 6. STM의 장점과 주의사항(Advantages and Caveats)

### 장점(Advantages)

- 락 없이도 조합 가능한 트랜잭션을 작성할 수 있습니다.
- 저수준 프리미티브를 사용하지 않는 선언적 접근 방식을 제공합니다.
- 낙관적 동시성을 통해 더 높은 처리량을 얻을 수 있습니다.
- 락 없는 논블로킹(lock-free, non-blocking) 알고리즘을 사용합니다.
- 거친 입자 락(coarse-grained locking)의 단순함을 유지하면서도 세밀한 입자 락(fine-grained locking)의 성능을 얻습니다.

### 주의사항 및 한계(Caveats and Limitations)

- **부수 효과·I/O 금지**: 트랜잭션 내부에서 I/O나 부수 효과를 실행해서는 안 됩니다. 트랜잭션은 비결정적으로 재시도될 수 있으므로, 부수 효과가 매 재시도마다 반복 실행되는 문제가 발생합니다.
- **복사 비용**: 데이터 구조를 갱신할 때 큰 데이터 구조의 복사가 발생할 수 있습니다.
- **재시도 비용**: 비용이 큰 연산은 재시도 시 반복 실행될 수 있습니다.

---

## 7. TRef — 트랜잭셔널 참조(Transactional Reference)

`TRef[A]`는 STM 트랜잭션에 참여할 수 있는, 불변 값에 대한 가변 참조입니다. STM 시스템 안에서 원자적 수정을 가능하게 하며, 원자성·일관성·격리성을 보장합니다.

### 생성(Creating TRef Instances)

**트랜잭션 내부에서 생성:**

```scala
import zio._
import zio.stm._
val createTRef: STM[Nothing, TRef[Int]] = TRef.make(10)
```

**즉시 커밋하여 생성:**

```scala
val commitTRef: UIO[TRef[Int]] = TRef.makeCommit(10)
```

### 핵심 연산(Core Operations)

**값 가져오기(get):**

```scala
val retrieveSingle: UIO[Int] = (for {
  tRef <- TRef.make(10)
  value <- tRef.get
} yield value).commit
```

**값 설정하기(set):**

```scala
val setSingle: UIO[Int] = (for {
  tRef <- TRef.make(10)
  _ <- tRef.set(20)
  nValue <- tRef.get
} yield nValue).commit
```

**함수로 갱신하기(updateAndGet):**

```scala
val updateSingle: UIO[Int] = (for {
  tRef <- TRef.make(10)
  nValue <- tRef.updateAndGet(_ + 20)
} yield nValue).commit
```

**수정하면서 값 추출하기(modify):**

```scala
val modifySingle: UIO[(String, Int)] = (for {
  tRef <- TRef.make(10)
  mValue <- tRef.modify(v => ("Zee-Oh", v + 10))
  nValue <- tRef.get
} yield (mValue, nValue)).commit
```

### 은행 송금 예제(Bank Transfer Example)

다음 예제는 파이버 간의 조합 가능한 원자적 트랜잭션과 잔액 검증(`retryUntil`)을 보여 줍니다.

```scala
def transfer(tSender: TRef[Int],
             tReceiver: TRef[Int],
             amount: Int): UIO[Int] = {
  STM.atomically {
    for {
      _ <- tSender.get.retryUntil(_ >= amount)
      _ <- tSender.update(_ - amount)
      nAmount <- tReceiver.updateAndGet(_ + amount)
    } yield nAmount
  }
}

val transferredMoney: UIO[String] = for {
  tSender <- TRef.makeCommit(50)
  tReceiver <- TRef.makeCommit(100)
  _ <- transfer(tSender, tReceiver, 50).fork
  _ <- tSender.get.retryUntil(_ == 0).commit
  tuple2 <- tSender.get.zip(tReceiver.get).commit
  (senderBalance, receiverBalance) = tuple2
} yield s"sender: $senderBalance & receiver: $receiverBalance"
```

여기서 `retryUntil`은 조건이 충족될 때까지 트랜잭션을 재시도(retry)하게 만드는 핵심 메커니즘으로, 잔액이 충분해질 때까지 트랜잭션을 대기시킵니다.

---

## 8. TArray — 트랜잭셔널 배열(Transactional Array)

`TArray`는 STM 트랜잭션에 참여할 수 있는 가변 참조들의 배열입니다.

### 생성(Creation Methods)

**빈 배열:**

```scala
import zio._
import zio.stm._
val emptyTArray: STM[Nothing, TArray[Int]] = TArray.empty[Int]
```

**지정한 값으로 생성:**

```scala
val specifiedValuesTArray: STM[Nothing, TArray[Int]] = TArray.make(1, 2, 3)
```

**Iterable로부터 생성:**

```scala
val iterableTArray: STM[Nothing, TArray[Int]] = TArray.fromIterable(List(1, 2, 3))
```

### 핵심 연산(Core Operations)

**요소 접근:**

```scala
val tArrayGetElem: UIO[Int] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  elem   <- tArray(2)
} yield elem).commit
```

**요소 갱신:**

```scala
val tArrayUpdateElem: UIO[TArray[Int]] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  _      <- tArray.update(2, el => el + 10)
} yield tArray).commit
```

**`updateSTM`을 통한 이펙트풀(effectful) 갱신:**

```scala
val tArrayUpdateMElem: UIO[TArray[Int]] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  _      <- tArray.updateSTM(2, el => STM.succeed(el + 10))
} yield tArray).commit
```

### 변환 연산(Transformation Operations)

**모든 요소 변환:**

```scala
val transformTArray: UIO[TArray[Int]] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  _      <- tArray.transform(a => a * a)
} yield tArray).commit
```

**이펙트풀 변환:**

```scala
val transformSTMTArray: UIO[TArray[Int]] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  _      <- tArray.transformSTM(a => STM.succeed(a * a))
} yield tArray).commit
```

### 집계 연산(Aggregate Operations)

**폴드(fold):**

```scala
val foldTArray: UIO[Int] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  sum    <- tArray.fold(0)(_ + _)
} yield sum).commit
```

**이펙트풀 폴드:**

```scala
val foldSTMTArray: UIO[Int] = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  sum    <- tArray.foldSTM(0)((acc, el) => STM.succeed(acc + el))
} yield sum).commit
```

**이펙트와 함께 순회(foreach):**

```scala
val foreachTArray = (for {
  tArray <- TArray.make(1, 2, 3, 4)
  tQueue <- TQueue.unbounded[Int]
  _      <- tArray.foreach(a => tQueue.offer(a).unit)
} yield tArray).commit
```

> **주의:** 존재하지 않는 인덱스에 접근하면 `ArrayIndexOutOfBoundsException`으로 트랜잭션이 중단(abort)됩니다.

---

## 9. TSet — 트랜잭셔널 집합(Transactional Set)

`TSet[A]`는 STM 트랜잭션에 참여할 수 있는 가변 집합입니다. ZIO STM 시스템에서 동시적·원자적 연산을 위한 자료구조입니다.

### 생성(Creation Methods)

**빈 집합:**

```scala
import zio._
import zio.stm._
val emptyTSet: STM[Nothing, TSet[Int]] = TSet.empty[Int]
```

**지정한 값으로 생성:**

```scala
val specifiedValuesTSet: STM[Nothing, TSet[Int]] = TSet.make(1, 2, 3)
```

**Iterable로부터 생성:**

```scala
val iterableTSet: STM[Nothing, TSet[Int]] = TSet.fromIterable(List(1, 2, 3))
```

> 중복 값이 있을 경우 마지막 것이 채택됩니다.

### 핵심 연산(Core Operations)

**요소 추가(put):**

```scala
val putElem: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2)
  _    <- tSet.put(3)
} yield tSet).commit
```

**요소 제거:**

```scala
// 단일 요소
val deleteElem: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2, 3)
  _    <- tSet.delete(1)
} yield tSet).commit

// 조건자(predicate)로 제거
val removedEvenElems: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  _    <- tSet.removeIf(_ % 2 == 0)
} yield tSet).commit

// 조건에 맞는 요소만 유지
val retainedEvenElems: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  _    <- tSet.retainIf(_ % 2 == 0)
} yield tSet).commit
```

**집합 연산(Set Operations):**

```scala
// 합집합(Union)
val unionTSet: UIO[TSet[Int]] = (for {
  tSetA <- TSet.make(1, 2, 3, 4)
  tSetB <- TSet.make(3, 4, 5, 6)
  _     <- tSetA.union(tSetB)
} yield tSetA).commit

// 교집합(Intersection)
val intersectionTSet: UIO[TSet[Int]] = (for {
  tSetA <- TSet.make(1, 2, 3, 4)
  tSetB <- TSet.make(3, 4, 5, 6)
  _     <- tSetA.intersect(tSetB)
} yield tSetA).commit

// 차집합(Difference)
val diffTSet: UIO[TSet[Int]] = (for {
  tSetA <- TSet.make(1, 2, 3, 4)
  tSetB <- TSet.make(3, 4, 5, 6)
  _     <- tSetA.diff(tSetB)
} yield tSetA).commit
```

**변환 및 폴드(Transform & Fold):**

```scala
val transformTSet: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  _    <- tSet.transform(a => a * a)
} yield tSet).commit

val transformSTMTSet: UIO[TSet[Int]] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  _    <- tSet.transformSTM(a => STM.succeed(a * a))
} yield tSet).commit

val foldTSet: UIO[Int] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  sum  <- tSet.fold(0)(_ + _)
} yield sum).commit
```

**질의 연산(Query Operations):**

```scala
// 포함 여부 확인
val tSetContainsElem: UIO[Boolean] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  res  <- tSet.contains(3)
} yield res).commit

// List로 변환
val tSetToList: UIO[List[Int]] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  list <- tSet.toList
} yield list).commit

// 크기 확인
val tSetSize: UIO[Int] = (for {
  tSet <- TSet.make(1, 2, 3, 4)
  size <- tSet.size
} yield size).commit
```

**순회(Iteration):**

```scala
val foreachTSet = (for {
  tSet   <- TSet.make(1, 2, 3, 4)
  tQueue <- TQueue.unbounded[Int]
  _      <- tSet.foreach(a => tQueue.offer(a).unit)
} yield tSet).commit
```

---

## 10. TMap — 트랜잭셔널 맵(Transactional Map)

`TMap[K, V]`는 STM 트랜잭션에 참여할 수 있는 가변 맵입니다. ZIO STM 시스템 안에서 맵 연산에 트랜잭셔널 의미론을 제공합니다.

### 생성(Creation Methods)

**빈 맵:**

```scala
import zio._
import zio.stm._
val emptyTMap: STM[Nothing, TMap[String, Int]] = TMap.empty[String, Int]
```

**지정한 값으로 생성:**

```scala
val specifiedValuesTMap: STM[Nothing, TMap[String, Int]] =
  TMap.make(("a", 1), ("b", 2), ("c", 3))
```

**Iterable로부터 생성:**

```scala
val iterableTMap: STM[Nothing, TMap[String, Int]] =
  TMap.fromIterable(List(("a", 1), ("b", 2), ("c", 3)))
```

### 핵심 연산(Core Operations)

**항목 추가(put):**

```scala
val putElem: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2))
  _    <- tMap.put("c", 3)
} yield tMap).commit
```

**함수로 병합(merge):**

```scala
val mergeElem: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  _    <- tMap.merge("c", 4)((x, y) => x * y)
} yield tMap).commit
```

**값 가져오기(get):**

```scala
val elemGet: UIO[Option[Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  elem <- tMap.get("c")
} yield elem).commit
```

**기본값과 함께 가져오기(getOrElse):**

```scala
val elemGetOrElse: UIO[Int] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  elem <- tMap.getOrElse("d", 4)
} yield elem).commit
```

**키로 삭제(delete):**

```scala
val deleteElem: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  _    <- tMap.delete("b")
} yield tMap).commit
```

**조건부 제거(removeIf):**

```scala
val removedEvenValues: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3), ("d", 4))
  _    <- tMap.removeIf((_, v) => v % 2 == 0)
} yield tMap).commit
```

**조건부 유지(retainIf):**

```scala
val retainedEvenValues: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3), ("d", 4))
  _    <- tMap.retainIf((_, v) => v % 2 == 0)
} yield tMap).commit
```

### 변환(Transformations)

**항목 변환(transform):**

```scala
val transformTMap: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  _    <- tMap.transform((k, v) => k -> v * v)
} yield tMap).commit
```

**값 변환(transformValues):**

```scala
val transformValuesTMap: UIO[TMap[String, Int]] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  _    <- tMap.transformValues(v => v * v)
} yield tMap).commit
```

### 집계(Aggregation)

**폴드(fold):**

```scala
val foldTMap: UIO[Int] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  sum  <- tMap.fold(0) { case (acc, (_, v)) => acc + v }
} yield sum).commit
```

### 변환(Conversions)

**List, 키, 값으로 변환:**

```scala
val tMapTuplesList: UIO[List[(String, Int)]] =
  (for {
    tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
    list <- tMap.toList
  } yield list).commit

val tMapKeysList: UIO[List[String]] =
  (for {
    tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
    list <- tMap.keys
  } yield list).commit

val tMapValuesList: UIO[List[Int]] =
  (for {
    tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
    list <- tMap.values
  } yield list).commit
```

**포함 여부 확인(contains):**

```scala
val tMapContainsValue: UIO[Boolean] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  res  <- tMap.contains("a")
} yield res).commit
```

---

## 11. TQueue — 트랜잭셔널 큐(Transactional Queue)

`TQueue[A]`는 STM 트랜잭션에 참여할 수 있는 가변 큐입니다. STM 안에서 안전한 동시 큐 연산을 가능하게 합니다.

### 생성(Creation)

**유계 큐(Bounded TQueue, 용량 제한 있음):**

```scala
import zio._
import zio.stm._
val tQueueBounded: STM[Nothing, TQueue[Int]] = TQueue.bounded[Int](5)
```

**무계 큐(Unbounded TQueue, 용량 제한 없음):**

```scala
import zio._
import zio.stm._
val tQueueUnbounded: STM[Nothing, TQueue[Int]] = TQueue.unbounded[Int]
```

### 연산(Operations)

**요소 추가 — 단일(offer):**

```scala
val tQueueOffer: UIO[TQueue[Int]] = (for {
  tQueue <- TQueue.bounded[Int](3)
  _      <- tQueue.offer(1)
} yield tQueue).commit
```

**요소 추가 — 다수(offerAll):**

```scala
val tQueueOfferAll: UIO[TQueue[Int]] = (for {
  tQueue <- TQueue.bounded[Int](3)
  _      <- tQueue.offerAll(List(1, 2))
} yield tQueue).commit
```

**첫 요소 가져오기(take, 비어 있으면 블록):**

```scala
val tQueueTake: UIO[Int] = (for {
  tQueue <- TQueue.bounded[Int](3)
  _      <- tQueue.offerAll(List(1, 2))
  res    <- tQueue.take
} yield res).commit
```

**폴(poll, 논블로킹, `Option` 반환):**

```scala
val tQueuePoll: UIO[Option[Int]] = (for {
  tQueue <- TQueue.bounded[Int](3)
  res    <- tQueue.poll
} yield res).commit
```

**최대 n개 가져오기(takeUpTo):**

```scala
val tQueueTakeUpTo: UIO[Chunk[Int]] = (for {
  tQueue <- TQueue.bounded[Int](4)
  _      <- tQueue.offerAll(List(1, 2))
  res    <- tQueue.takeUpTo(3)
} yield res).commit
```

**전체 가져오기(takeAll):**

```scala
val tQueueTakeAll: UIO[Chunk[Int]] = (for {
  tQueue <- TQueue.bounded[Int](4)
  _      <- tQueue.offerAll(List(1, 2))
  res    <- tQueue.takeAll
} yield res).commit
```

**크기(size):**

```scala
val tQueueSize: UIO[Int] = (for {
  tQueue <- TQueue.bounded[Int](3)
  _      <- tQueue.offerAll(List(1, 2))
  size   <- tQueue.size
} yield size).commit
```

---

## 12. TPriorityQueue — 트랜잭셔널 우선순위 큐(Transactional Priority Queue)

`TPriorityQueue[A]`는 STM 트랜잭션에 참여할 수 있는 가변 우선순위 큐로, 요소 타입에 정의된 `Ordering`을 가집니다. `TQueue`와 달리 가장 오래된 요소가 아니라 **정렬 순서상 첫 번째(최고 우선순위)** 요소를 반환합니다.

> `TPriorityQueue`를 사용하려면 요소 타입에 대한 암시적 `Ordering`이 필요합니다.

### 생성(Creation)

**기본 정렬(오름차순)을 사용하는 빈 큐:**

```scala
import zio._
import zio.stm._
val minQueue: STM[Nothing, TPriorityQueue[Int]] = TPriorityQueue.empty
```

**커스텀 정렬(내림차순):**

```scala
val maxQueue: STM[Nothing, TPriorityQueue[Int]] =
  TPriorityQueue.empty(Ordering[Int].reverse)
```

**초기 요소와 함께 생성:**

```scala
TPriorityQueue.fromIterable(iterable)  // Iterable로부터
TPriorityQueue.make(elements)           // varargs로부터
```

### 연산(Operations)

**요소 추가(offering elements):**

```scala
queue.offer(element)           // 단일 요소
queue.offerAll(List(2, 4, 6))  // 다수 요소
```

**요소 가져오기(taking elements):**

```scala
queue.take              // 사용 가능할 때까지 블록, 가장 높은 우선순위 반환
queue.takeAll           // 현재 모든 요소를 즉시 반환
queue.takeUpTo(n)       // 최대 n개 요소를 즉시 반환
queue.takeOption        // 블로킹 없이 Option 반환
queue.peek              // 첫 요소를 제거하지 않고 관찰
```

**수정 없이 조회(inspection without modification):**

```scala
queue.toChunk    // 불변 스냅샷
queue.toList     // List로 변환
queue.toVector   // Vector로 변환
queue.size       // 현재 큐 크기
```

---

## 13. TPromise — 트랜잭셔널 프로미스(Transactional Promise)

`TPromise`는 정확히 한 번만 설정할 수 있으며 STM 트랜잭션에 참여할 수 있는 단일 할당 참조입니다. 한 파이버가 값을 설정하면 다른 파이버가 그 값을 기다릴 수 있습니다.

### 생성(Creation)

STM 트랜잭션 내부에서 `make` 팩토리 메서드로 생성합니다.

```scala
import zio._
import zio.stm._
val tPromise: STM[Nothing, TPromise[String, Int]] = TPromise.make[String, Int]
```

### 연산(Operations)

**성공으로 완료(succeed):**

```scala
val tPromiseSucceed: UIO[TPromise[String, Int]] = for {
  tPromise <- TPromise.make[String, Int].commit
  _        <- tPromise.succeed(0).commit
} yield tPromise
```

**실패로 완료(fail):**

```scala
val tPromiseFail: UIO[TPromise[String, Int]] = for {
  tPromise <- TPromise.make[String, Int].commit
  _        <- tPromise.fail("failed").commit
} yield tPromise
```

**`Either` 값으로 완료(done):**

```scala
val tPromiseDoneSucceed: UIO[TPromise[String, Int]] = for {
  tPromise <- TPromise.make[String, Int].commit
  _        <- tPromise.done(Right(0)).commit
} yield tPromise
```

**결과 폴링(poll, 완료 시 결과, 미완료 시 `None`):**

```scala
val tPromiseOptionValue: UIO[Option[Either[String, Int]]] = for {
  tPromise <- TPromise.make[String, Int].commit
  _        <- tPromise.succeed(0).commit
  res      <- tPromise.poll.commit
} yield res
```

**완료 대기 후 값 추출(await):**

```scala
val tPromiseValue: IO[String, Int] = for {
  tPromise <- TPromise.make[String, Int].commit
  _        <- tPromise.succeed(0).commit
  res      <- tPromise.await.commit
} yield res
```

---

## 14. TReentrantLock — 재진입 락(Reentrant Lock)

`TReentrantLock`는 STM 기반의 동기화 프리미티브입니다. 여러 파이버가 동시에 상태를 읽을 수 있도록 허용하되, 데이터 손상을 방지하기 위해 오직 하나의 파이버만 상태를 수정할 수 있게 합니다.

### 핵심 특성(Key Characteristics)

- **재진입성(Reentrancy)**: 하나의 파이버가 자기 자신을 블로킹하지 않고 락을 여러 번 획득할 수 있습니다. 락 획득 횟수를 추적하기 어려운 경우에 유용합니다.
- **읽기/쓰기 의미론(Read/Write Semantics)**: 쓰기 중인 파이버가 보유한 모든 쓰기 락이 해제될 때까지 읽기가 허용되지 않습니다. 다른 락이 전혀 없거나, 쓰기 락을 원하는 파이버가 이미 읽기 락을 보유하고 있고 다른 파이버가 읽기 락을 보유하고 있지 않은 경우에만 쓰기가 허용됩니다.
- **승급/강등 지원(Upgrade/Downgrade Support)**: 읽기 락에서 쓰기 락으로의 승급(upgrade)과 쓰기 락에서 읽기 락으로의 강등(downgrade)을 지원합니다.

### 생성(Creation)

```scala
import zio.stm._
val reentrantLock = TReentrantLock.make
```

### 기본 연산(Basic Operations)

- `acquireRead` — 읽기 락 획득
- `acquireWrite` — 쓰기 락 획득
- `releaseRead` — 읽기 락 해제
- `releaseWrite` — 쓰기 락 해제
- `readLocked` — 읽기 락 상태인지 확인
- `writeLocked` — 쓰기 락 상태인지 확인

### 더 안전한 메서드(Safer Methods)

```scala
val saferProgram: UIO[Unit] = for {
  lock <- TReentrantLock.make.commit
  f1   <- ZIO.scoped(lock.readLock *> ZIO.sleep(5.seconds) *> printLine("Powering down").orDie).fork
  f2   <- ZIO.scoped(lock.readLock *> lock.writeLock *> printLine("Huzzah, writes are mine").orDie).fork
  _    <- (f1 zip f2).join
} yield ()
```

`readLock`과 `writeLock` 메서드는 안전성 측면에서 권장됩니다. `Scope`를 통해 락을 자동으로 획득하고 해제하므로 락 해제를 잊는 실수를 방지할 수 있습니다.

---

## 15. TSemaphore — 트랜잭셔널 세마포어(Transactional Semaphore)

`TSemaphore`는 트랜잭셔널 의미론을 가진 세마포어로, 공유 자원에 대한 접근을 제어하는 데 사용합니다. 일정 수의 허가(permit)를 보유하며, 허가는 획득(acquire)하거나 해제(release)할 수 있습니다.

### 생성(Creation)

허가 10개를 가진 `TSemaphore` 생성:

```scala
import zio._
import zio.stm._

val tSemaphoreCreate: STM[Nothing, TSemaphore] = TSemaphore.make(10L)
```

### 핵심 연산(Core Operations)

**허가 획득(acquire):**

```scala
val tSemaphoreAcq: STM[Nothing, TSemaphore] = for {
  tSem <- TSemaphore.make(2L)
  _    <- tSem.acquire
} yield tSem
tSemaphoreAcq.commit
```

**허가 해제(release):**

```scala
val tSemaphoreRelease: STM[Nothing, TSemaphore] = for {
  tSem <- TSemaphore.make(1L)
  _    <- tSem.acquire
  _    <- tSem.release
} yield tSem
tSemaphoreRelease.commit
```

**사용 가능한 허가 수 확인(available):**

```scala
val tSemaphoreAvailable: STM[Nothing, Long] = for {
  tSem <- TSemaphore.make(2L)
  _    <- tSem.acquire
  cap  <- tSem.available
} yield cap
tSemaphoreAvailable.commit
```

**자동 획득/해제로 동작 실행(withPermit):**

```scala
val tSemaphoreWithPermit: IO[Nothing, Unit] =
  for {
    sem <- TSemaphore.make(1L).commit
    a   <- sem.withPermit(yourSTMAction.commit)
  } yield a
```

**다수 허가 획득/해제(acquireN / releaseN):**

```scala
val tSemaphoreAcquireNReleaseN: STM[Nothing, Boolean] = for {
  sem <- TSemaphore.make(3L)
  _   <- sem.acquireN(3L)
  cap <- sem.available
  _   <- sem.releaseN(3L)
} yield cap == 0
tSemaphoreAcquireNReleaseN.commit
```

---

## 16. 참고 자료

- [Software Transactional Memory (STM) — ZIO 공식 문서](https://zio.dev/reference/stm/)
- [TRef](https://zio.dev/reference/stm/tref)
- [TArray](https://zio.dev/reference/stm/tarray)
- [TSet](https://zio.dev/reference/stm/tset)
- [TMap](https://zio.dev/reference/stm/tmap)
- [TQueue](https://zio.dev/reference/stm/tqueue)
- [TPriorityQueue](https://zio.dev/reference/stm/tpriorityqueue)
- [TPromise](https://zio.dev/reference/stm/tpromise)
- [TReentrantLock](https://zio.dev/reference/stm/treentrantlock)
- [TSemaphore](https://zio.dev/reference/stm/tsemaphore)
