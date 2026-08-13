# ZIO STM과 스케줄링·재시도

## 소프트웨어 트랜잭셔널 메모리(STM)

> 원본: https://zio.dev/reference/stm/

---

### 목차

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

### 1. STM 개요(Software Transactional Memory)

소프트웨어 트랜잭셔널 메모리(Software Transactional Memory, STM): 여러 메모리 연산을 하나의 트랜잭션 안에서 원자적으로 실행할 수 있게 해 주는 모듈식·조합 가능한 동시성 데이터 구조.

STM은 데이터베이스의 트랜잭션 개념을 메모리 연산에 적용한 것. 여러 메모리 읽기/쓰기 연산을 묶어 하나의 트랜잭션으로 만들면 → 전부 성공하거나(커밋) 전부 실패하여 아무 효과도 남기지 않는 것(롤백)이 보장됨.

ZIO의 STM은 락을 직접 다루지 않고도 동시성 문제를 안전하게 해결 가능. 조합 가능성(composability)이 핵심 장점 - 여러 작은 트랜잭션을 결합하여 더 큰 하나의 원자적 트랜잭션 생성 가능.

---

### 2. 락 기반 동시성의 한계(The Problem with Locks)

전통적인 가변 참조(`Ref`)와 락만으로는 동시성 문제를 조합 가능한 방식으로 해결 불가.

#### 경쟁 상태(Race Condition) 문제

다음과 같은 단순한 증가(increment) 함수 예시.

```scala
def inc(counter: Ref[Int], amount: Int) = for {
  c <- counter.get
  _ <- counter.set(c + amount)
} yield c
```

이 함수에 대해 10개의 동시 파이버를 실행하더라도 결과가 항상 10이 된다고 보장 불가. 카운터 값을 읽는 시점과 새 값을 설정하는 시점 사이에 다른 파이버가 끼어들어 값을 변경할 수 있기 때문.

#### 조합 불가능성(Composability Failure) 문제

고전적인 송금(money transfer) 예제로 락 기반 접근의 조합 실패를 확인.

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

`withdraw`와 `deposit`이 각각 원자적이더라도 파이버 전반에 걸쳐 이 두 연산을 원자적으로 조합 불가. 출금과 입금 사이에 다른 파이버가 끼어들면 일관성이 깨질 수 있음.

#### STM을 통한 해결

`Ref`를 `TRef`(Transactional Reference, 트랜잭셔널 참조)로 바꾸고 `STM`을 사용하면 이 문제 해결 가능.

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

`STM.atomically { ... }`로 감싼 블록 전체가 하나의 원자적 트랜잭션이 됨 → 출금과 입금은 함께 커밋되거나 함께 롤백됨.

---

### 3. STM이 동작하는 방식(How STM Works)

STM은 낙관적 동시성(optimistic concurrency) 모델 사용. 충돌이 드물게 발생한다고 가정하고, 비관적 락 대신 커밋 시점에만 일관성을 검증.

STM 트랜잭션은 세 단계로 동작.

#### 1단계: 트랜잭션 시작(Starting Transaction)

런타임 시스템이 트랜잭션 로그를 추적하기 위한 가상 공간을 생성. 이 로그는 읽기와 잠정적 쓰기를 기록하면서 구축됨.

#### 2단계: 가상 실행(Virtual Execution)

시스템은 공유 메모리를 수정하지 않은 채 읽기 로그와 쓰기 로그를 유지. atomic 블록 안의 모든 동작은 즉시 실행되지 않고 가상 세계에서 실행됨.

#### 3단계: 커밋 단계(Commit Phase)

트랜잭션 완료 시점에 시스템은 트랜잭션에 관여한 트랜잭셔널 변수들이 다른 스레드에 의해 수정되었는지 검사. 충돌이 감지되어 가정이 무효화되면 → 해당 트랜잭션을 폐기하고 처음부터 재시도.

---

### 4. STM의 ACID 속성(ACID Properties)

STM은 데이터베이스 트랜잭션의 ACID 속성을 메모리 연산에 맞게 적용. 단, 인메모리 연산이므로 지속성(Durability)은 제외.

- 원자성(Atomicity): 갱신 연산은 전부 실행되거나 전혀 실행되지 않음
- 일관성(Consistency): 프로그램 상태에 대한 일관된 뷰를 제공하여 상태에 대한 모든 참조가 동일한 값을 얻도록 보장
- 격리성(Isolation): 각 트랜잭션은 다른 동시 트랜잭션에 영향을 주지 않음
- 지속성(Durability): 인메모리 연산에는 해당하지 않으므로 생략

---

### 5. ZSTM 타입과 별칭(ZSTM Type and Aliases)

STM의 기반이 되는 핵심 타입은 `ZSTM[-R, +E, +A]`. 환경(`R`), 오류 타입(`E`), 결과 타입(`A`)을 가진 트랜잭션을 나타내며, `ZIO[-R, +E, +A]`와 구조가 대응됨.

자주 사용되는 타입 별칭(type alias)은 다음과 같음.

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

`STM` 값은 그 자체로는 아무 효과도 일으키지 않는 순수한 기술(description). 실제로 실행하려면 `.commit` 메서드(또는 `STM.atomically`)를 호출해 `ZIO` 이펙트로 변환 필요.

---

### 6. STM의 장점과 주의사항(Advantages and Caveats)

#### 장점(Advantages)

- 락 없이도 조합 가능한 트랜잭션 작성 가능
- 저수준 프리미티브를 사용하지 않는 선언적 접근 방식 제공
- 낙관적 동시성을 통해 더 높은 처리량 확보 가능
- 락 없는 논블로킹(lock-free, non-blocking) 알고리즘 사용
- 거친 입자 락(coarse-grained locking)의 단순함을 유지하면서도 세밀한 입자 락(fine-grained locking)의 성능 확보

#### 주의사항 및 한계(Caveats and Limitations)

- 부수 효과·I/O 금지: 트랜잭션 내부에서 I/O나 부수 효과 실행 금지. 트랜잭션은 비결정적으로 재시도될 수 있으므로, 부수 효과가 매 재시도마다 반복 실행되는 문제 발생
- 복사 비용: 데이터 구조를 갱신할 때 큰 데이터 구조의 복사 발생 가능
- 재시도 비용: 비용이 큰 연산은 재시도 시 반복 실행 가능

---

### 7. TRef — 트랜잭셔널 참조(Transactional Reference)

`TRef[A]`는 STM 트랜잭션에 참여할 수 있는, 불변 값에 대한 가변 참조. STM 시스템 안에서 원자적 수정을 가능하게 하며, 원자성·일관성·격리성을 보장.

#### 생성(Creating TRef Instances)

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

#### 핵심 연산(Core Operations)

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

#### 은행 송금 예제(Bank Transfer Example)

다음 예제는 파이버 간의 조합 가능한 원자적 트랜잭션과 잔액 검증(`retryUntil`)을 보여줌.

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

여기서 `retryUntil`은 조건이 충족될 때까지 트랜잭션을 재시도(retry)하게 만드는 핵심 메커니즘 → 잔액이 충분해질 때까지 트랜잭션을 대기시킴.

---

### 8. TArray — 트랜잭셔널 배열(Transactional Array)

`TArray`는 STM 트랜잭션에 참여할 수 있는 가변 참조들의 배열.

#### 생성(Creation Methods)

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

#### 핵심 연산(Core Operations)

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

#### 변환 연산(Transformation Operations)

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

#### 집계 연산(Aggregate Operations)

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

> 주의: 존재하지 않는 인덱스에 접근하면 `ArrayIndexOutOfBoundsException`으로 트랜잭션이 중단(abort)됨.

---

### 9. TSet — 트랜잭셔널 집합(Transactional Set)

`TSet[A]`는 STM 트랜잭션에 참여할 수 있는 가변 집합. ZIO STM 시스템에서 동시적·원자적 연산을 위한 자료구조.

#### 생성(Creation Methods)

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

> 중복 값이 있을 경우 마지막 것이 채택됨.

#### 핵심 연산(Core Operations)

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

### 10. TMap — 트랜잭셔널 맵(Transactional Map)

`TMap[K, V]`는 STM 트랜잭션에 참여할 수 있는 가변 맵. ZIO STM 시스템 안에서 맵 연산에 트랜잭셔널 의미론 제공.

#### 생성(Creation Methods)

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

#### 핵심 연산(Core Operations)

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

#### 변환(Transformations)

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

#### 집계(Aggregation)

**폴드(fold):**

```scala
val foldTMap: UIO[Int] = (for {
  tMap <- TMap.make(("a", 1), ("b", 2), ("c", 3))
  sum  <- tMap.fold(0) { case (acc, (_, v)) => acc + v }
} yield sum).commit
```

#### 변환(Conversions)

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

### 11. TQueue — 트랜잭셔널 큐(Transactional Queue)

`TQueue[A]`는 STM 트랜잭션에 참여할 수 있는 가변 큐. STM 안에서 안전한 동시 큐 연산을 가능하게 함.

#### 생성(Creation)

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

#### 연산(Operations)

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

### 12. TPriorityQueue — 트랜잭셔널 우선순위 큐(Transactional Priority Queue)

`TPriorityQueue[A]`는 STM 트랜잭션에 참여할 수 있는 가변 우선순위 큐로, 요소 타입에 정의된 `Ordering`을 가짐. `TQueue`와 달리 가장 오래된 요소가 아니라 정렬 순서상 첫 번째(최고 우선순위) 요소를 반환.

> `TPriorityQueue`를 사용하려면 요소 타입에 대한 암시적 `Ordering` 필요.

#### 생성(Creation)

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

#### 연산(Operations)

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

### 13. TPromise — 트랜잭셔널 프로미스(Transactional Promise)

`TPromise`는 정확히 한 번만 설정할 수 있으며 STM 트랜잭션에 참여할 수 있는 단일 할당 참조. 한 파이버가 값을 설정하면 다른 파이버가 그 값을 기다릴 수 있음.

#### 생성(Creation)

STM 트랜잭션 내부에서 `make` 팩토리 메서드로 생성.

```scala
import zio._
import zio.stm._
val tPromise: STM[Nothing, TPromise[String, Int]] = TPromise.make[String, Int]
```

#### 연산(Operations)

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

### 14. TReentrantLock — 재진입 락(Reentrant Lock)

`TReentrantLock`는 STM 기반의 동기화 프리미티브. 여러 파이버가 동시에 상태를 읽을 수 있도록 허용하되, 데이터 손상을 방지하기 위해 오직 하나의 파이버만 상태를 수정할 수 있게 함.

#### 핵심 특성(Key Characteristics)

- 재진입성(Reentrancy): 하나의 파이버가 자기 자신을 블로킹하지 않고 락을 여러 번 획득 가능. 락 획득 횟수를 추적하기 어려운 경우에 유용
- 읽기/쓰기 의미론(Read/Write Semantics): 쓰기 중인 파이버가 보유한 모든 쓰기 락이 해제될 때까지 읽기 불가. 다른 락이 전혀 없거나, 쓰기 락을 원하는 파이버가 이미 읽기 락을 보유하고 있고 다른 파이버가 읽기 락을 보유하고 있지 않은 경우에만 쓰기 허용
- 승급/강등 지원(Upgrade/Downgrade Support): 읽기 락에서 쓰기 락으로의 승급(upgrade)과 쓰기 락에서 읽기 락으로의 강등(downgrade) 지원

#### 생성(Creation)

```scala
import zio.stm._
val reentrantLock = TReentrantLock.make
```

#### 기본 연산(Basic Operations)

- `acquireRead` — 읽기 락 획득
- `acquireWrite` — 쓰기 락 획득
- `releaseRead` — 읽기 락 해제
- `releaseWrite` — 쓰기 락 해제
- `readLocked` — 읽기 락 상태인지 확인
- `writeLocked` — 쓰기 락 상태인지 확인

#### 더 안전한 메서드(Safer Methods)

```scala
val saferProgram: UIO[Unit] = for {
  lock <- TReentrantLock.make.commit
  f1   <- ZIO.scoped(lock.readLock *> ZIO.sleep(5.seconds) *> printLine("Powering down").orDie).fork
  f2   <- ZIO.scoped(lock.readLock *> lock.writeLock *> printLine("Huzzah, writes are mine").orDie).fork
  _    <- (f1 zip f2).join
} yield ()
```

`readLock`과 `writeLock` 메서드는 안전성 측면에서 권장. `Scope`를 통해 락을 자동으로 획득하고 해제하므로 락 해제를 잊는 실수 방지 가능.

---

### 15. TSemaphore — 트랜잭셔널 세마포어(Transactional Semaphore)

`TSemaphore`는 트랜잭셔널 의미론을 가진 세마포어로, 공유 자원에 대한 접근 제어에 사용. 일정 수의 허가(permit)를 보유하며, 허가는 획득(acquire)하거나 해제(release) 가능.

#### 생성(Creation)

허가 10개를 가진 `TSemaphore` 생성:

```scala
import zio._
import zio.stm._

val tSemaphoreCreate: STM[Nothing, TSemaphore] = TSemaphore.make(10L)
```

#### 핵심 연산(Core Operations)

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

### 16. 참고 자료

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

---

## 스케줄링과 재시도: Schedule

> 원본: https://zio.dev/reference/schedule/

---

### 목차

1. [Schedule 소개(Introduction)](#1-schedule-소개introduction)
2. [반복(Repetition)](#2-반복repetition)
3. [재시도(Retrying)](#3-재시도retrying)
4. [내장 스케줄(Built-in Schedules)](#4-내장-스케줄built-in-schedules)
5. [스케줄 조합자(Schedule Combinators)](#5-스케줄-조합자schedule-combinators)
6. [예제(Examples)](#6-예제examples)
7. [참고 자료](#7-참고-자료)

---

### 1. Schedule 소개(Introduction)

`Schedule[Env, In, Out]`는 반복적인(recurring) 효과적(effectful) 스케줄을 기술(describe)하는 불변 값(immutable value). 이 스케줄은 어떤 환경(environment) `Env`에서 실행되며, 타입 `In`의 값(재시도(`retry`)의 경우에는 오류(error), 반복(`repeat`)의 경우에는 값)을 소비(consume)한 후 타입 `Out`의 값을 생성(produce). 매 단계(step)마다 입력 값(input value)과 내부 상태(internal state)에 기반해, 중단(halt)할지 아니면 어떤 지연(delay) d 이후에 계속(continue)할지를 결정.

스케줄(Schedule)은 시간(time)에 걸쳐 펼쳐진, 잠재적으로 무한한(possibly infinite) 구간(interval)들의 집합으로 정의됨. 각 구간은 반복(recurrence)이 가능한 윈도우(window)를 정의.

[반복(Repetition)](#2-반복repetition)과 [재시도(Retrying)](#3-재시도retrying)는 스케줄링(scheduling) 영역에서 유사한 두 가지 개념. 이 둘은 동일한 개념이자 아이디어이며, 다만 하나는 성공(success)을 찾고 다른 하나는 실패(failure)를 찾는다는 점만 다름.

스케줄이 효과(effect)를 반복하거나 재시도하는 데 사용될 때, 스케줄이 생성하는 각 구간의 시작 경계(starting boundary)가 그 효과가 다시 실행될 시점(moment)으로 사용됨.

스케줄은 유연한 반복 스케줄(flexible recurrence schedule)을 정의하고 조합(compose) 가능 → 액션(action)을 반복(repeat)하거나, 오류가 발생한 경우에 액션을 재시도(retry) 가능.

스케줄을 변환(transform)하고 결합(combine)하기 위한 다양한 [조합자(combinator)](#5-스케줄-조합자schedule-combinators)가 존재하며, `Schedule`의 컴패니언 객체(companion object)에는 재시도와 반복을 수행하기 위한 [모든 일반적인 유형의 스케줄](#4-내장-스케줄built-in-schedules)이 포함됨.

#### 함께 보기(See Also)

- ZStream 스케줄링(ZStream Scheduling): 설정 가능한 스케줄 정책(schedule policy)을 사용해 스트림 출력의 방출 타이밍(emission timing)과 간격(spacing)을 제어하는 ZStream 스케줄링 조합자
- TestAspect: 반복과 재시도(Repetition and Retrying): 지정된 스케줄에 따라 테스트를 반복하거나 재시도하기 위한 테스트 애스펙트(test aspect)

> 이후 모든 예제는 다음 임포트를 전제로 함.
>
> ```scala
> import zio._
> ```

---

### 2. 반복(Repetition)

반복(repetition)의 경우, ZIO에는 `ZIO#repeat` 함수가 있음. 이 함수는 스케줄을 반복 정책(repetition policy)으로 받아, 그 정책에 따른 반복 전략(repetition strategy)을 가진 효과를 기술하는 또 다른 효과를 반환.

반복 정책은 다음 함수들에서 사용됨.

- `ZIO#repeat`: 스케줄이 완료될 때까지 효과를 반복
- `ZIO#repeatOrElse`: 스케줄이 완료될 때까지 효과를 반복하며, 오류에 대한 폴백(fallback) 제공

> 주의:
>
> 스케줄에 의한 반복 실행(scheduled recurrence)은 첫 번째 실행에 추가로(in addition to) 일어남. 따라서 `io.repeat(Schedule.once)`는 `io`를 실행하고, 그것이 성공하면 `io`를 한 번 더 실행하는 효과를 만들어 냄.

`ZIO#repeat` 함수로 반복 효과(repeated effect)를 만드는 방법.

```scala
val action:      ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val repeated = action repeat policy
```

오류 발생 시 폴백 전략(fallback strategy)을 제공하는 또 다른 버전의 `repeat`도 있음. `ZIO#repeatOrElse` 함수를 사용하면 반복 실패 시 실행될 `orElse` 콜백(callback) 지정 가능.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val orElse: (E, Option[B]) => ZIO[R1, E2, B] = ???

val repeated = action repeatOrElse (policy, orElse)
```

---

### 3. 재시도(Retrying)

재시도(retrying)의 경우, ZIO에는 `ZIO#retry` 함수가 있음. 이 함수는 스케줄을 반복 정책으로 받아, 원래 효과의 실패(failure)에 뒤이어 재시도를 수행하는 반복 전략을 가진 효과를 기술하는 또 다른 효과를 반환.

반복 정책은 다음 함수들에서 사용됨.

- `ZIO#retry`: 효과가 성공할 때까지 재시도
- `ZIO#retryOrElse`: 효과가 성공할 때까지 재시도하며, 오류에 대한 폴백 제공

`ZIO#retry` 함수로 재시도 효과를 만드는 방법.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, E, S] = ???

val repeated = action retry policy

```

오류 발생 시 폴백 전략을 제공하는 또 다른 버전의 `retry`도 있음. `ZIO#retryOrElse` 함수를 사용하면 재시도 실패 시 실행될 `orElse` 콜백 지정 가능.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val orElse: (E, S) => ZIO[R1, E1, A1] = ???

val repeated = action retryOrElse (policy, orElse)
```

> 반복(repeat)과 재시도(retry)의 차이: 두 함수 모두 동일한 `Schedule` 메커니즘을 사용하지만, `repeat`는 효과가 성공할 때마다 스케줄을 적용해 반복하고, `retry`는 효과가 실패할 때마다 스케줄을 적용해 다시 시도. 즉, `repeat`에서 스케줄의 입력 타입 `In`은 효과의 성공 값(`A`), `retry`에서 스케줄의 입력 타입 `In`은 효과의 오류(`E`).

---

### 4. 내장 스케줄(Built-in Schedules)

`Schedule`의 컴패니언 객체는 효과의 반복을 제어하기 위한 여러 내장 스케줄 제공. 고정 간격(fixed interval), 지수 백오프(exponential backoff), 피보나치 기반(fibonacci-based) 지연 등 다양한 지연 전략(delay strategy) 포함.

#### 4.1 succeed

지정된 상수 값(constant value)을 생성하면서 한 번 반복하는 스케줄을 반환.

```scala
val constant = Schedule.succeed(5)
```

#### 4.2 fromFunction

항상 반복하며, 입력 값을 지정된 함수를 통해 매핑(mapping)하는 스케줄.

```scala
val inc = Schedule.fromFunction[Int, Int](_ + 1)
```

#### 4.3 stop

반복하지 않고, 그냥 멈춘 뒤 하나의 `Unit` 요소를 반환하는 스케줄.

```scala
val stop = Schedule.stop
```

#### 4.4 once

한 번 반복하고 하나의 `Unit` 요소를 반환하는 스케줄.

```scala
val once = Schedule.once
```

#### 4.5 forever

항상 반복하며, 매 실행마다 반복 횟수(number of recurrence)를 생성하는 스케줄.

```scala
val forever = Schedule.forever
```

#### 4.6 recurs

지정된 횟수(specified number of times)만큼만 반복하는 스케줄.

```scala
val recurs = Schedule.recurs(5)
```

#### 4.7 spaced

연속적으로 반복하되, 각 반복(repetition)이 직전 실행으로부터 지정된 지속 시간(duration)만큼 간격을 두고(spaced) 일어나는 스케줄.

```scala
val spaced = Schedule.spaced(10.milliseconds)
```

#### 4.8 fixed

고정 간격(fixed interval)으로 반복하는 스케줄. 지금까지의 스케줄 반복 횟수(number of repetitions)를 반환.

```scala
val fixed = Schedule.fixed(10.seconds)
```

> `spaced`와 `fixed`의 차이: `spaced`는 직전 실행이 끝난 시점으로부터 지정된 시간만큼 간격을 두지만, `fixed`는 실행의 소요 시간과 무관하게 고정된 주기(period)에 맞춰 반복.

#### 4.9 exponential

지수 백오프(exponential backoff)를 사용해 반복하는 스케줄.

```scala
val exponential = Schedule.exponential(10.milliseconds)
```

#### 4.10 fibonacci

항상 반복하며, 직전 두 지연(preceding two delays)을 합산해 지연을 증가시키는(피보나치 수열과 유사) 스케줄. 반복 간의 현재 지속 시간(current duration)을 반환.

```scala
val fibonacci = Schedule.fibonacci(10.milliseconds)
```

#### 4.11 identity

항상 계속(continue)하기로 결정하는 스케줄. 어떠한 지연도 없이 영원히 반복. `identity` 스케줄은 입력을 소비하고, 그것과 동일한 것을 출력으로 방출(`Schedule[Any, A, A]`).

```scala
val identity = Schedule.identity[Int]
```

#### 4.12 unfold

지정된 상태(state)와 반복자(iterator)로부터 한 번 반복하는 스케줄.

```scala
val unfold = Schedule.unfold(0)(_ + 1)
```

---

### 5. 스케줄 조합자(Schedule Combinators)

스케줄은 상태를 가지며(stateful), 잠재적으로 효과적인(possibly effectful) 반복 이벤트 스케줄을 정의하고, 다양한 방식으로 조합(compose) 가능. 조합자(combinator)는 여러 스케줄을 결합해 새로운 스케줄을 만듦. 적절한 조합자를 갖추면 몇 가지 기본 스케줄(base schedule)과 조합자만으로도 매우 다양한 상황에 대응 가능.

#### 5.1 합성(Composition)

스케줄은 다음과 같은 주요 방식으로 합성됨.

- 합집합(Union): 두 스케줄의 구간들의 합집합(union) 수행
- 교집합(Intersection): 두 스케줄의 구간들의 교집합(intersection) 수행
- 순차 연결(Sequencing): 한 스케줄의 구간 위에 다른 스케줄의 구간을 이어 붙임(concatenate)

##### 합집합(Union)

두 스케줄을 합집합으로 결합. 두 스케줄 중 어느 하나라도 반복하기를 원하면 반복하며, 두 지연 중 최솟값(minimum)을 반복 간 지연으로 사용.

- `s1 || s2` 결합 결과
  - 타입(Type): `s1`은 `Schedule[R, A, B]` · `s2`는 `Schedule[R, A, C]` → 결과 `Schedule[R, A, (B, C)]`
  - 계속(Continue) `Boolean`: `s1`은 `b1` · `s2`는 `b2` → 결과 `b1 || b2`
  - 지연(Delay) `Duration`: `s1`은 `d1` · `s2`는 `d2` → 결과 `d1.min(d2)`
  - 방출(Emit) `(A, B)`: `s1`은 `a` · `s2`는 `b` → 결과 `(a, b)`

`||` 연산자로 두 스케줄을 합집합으로 결합 가능.

```scala
val expCapped = Schedule.exponential(100.milliseconds) || Schedule.spaced(1.second)
```

##### 교집합(Intersection)

두 스케줄을 교집합으로 결합. 두 스케줄이 모두 반복하기를 원하는 경우에만 반복하며, 두 지연 중 최댓값(maximum)을 반복 간 지연으로 사용.

- `s1 && s2` 결합 결과
  - 타입(Type): `s1`은 `Schedule[R, A, B]` · `s2`는 `Schedule[R, A, C]` → 결과 `Schedule[R, A, (B, C)]`
  - 계속(Continue) `Boolean`: `s1`은 `b1` · `s2`는 `b2` → 결과 `b1 && b2`
  - 지연(Delay) `Duration`: `s1`은 `d1` · `s2`는 `d2` → 결과 `d1.max(d2)`
  - 방출(Emit) `(A, B)`: `s1`은 `a` · `s2`는 `b` → 결과 `(a, b)`

`&&` 연산자로 두 스케줄을 교집합으로 결합 가능.

```scala
val expUpTo10 = Schedule.exponential(1.second) && Schedule.recurs(10)
```

##### 순차 연결(Sequencing)

두 스케줄을 순차적으로 결합. 첫 번째 정책(first policy)이 끝날 때까지 그것을 따른 다음 두 번째 정책(second policy)을 따름.

- `s1 andThen s2` 결합 결과
  - 타입(Type): `s1`은 `Schedule[R, A, B]` · `s2`는 `Schedule[R, A, C]` → 결과 `Schedule[R, A, C]`
  - 지연(Delay) `Duration`: `s1`은 `d1` · `s2`는 `d2` → 결과 `d1 + d2`
  - 방출(Emit) `B`: `s1`은 `a` · `s2`는 `b` → 결과 `b`

`andThen`을 사용해 두 스케줄을 순차적으로 연결 가능.

```scala
val sequential = Schedule.recurs(10) andThen Schedule.spaced(1.second)
```

#### 5.2 파이핑(Piping)

첫 번째 스케줄의 출력을 두 번째 스케줄의 입력으로 파이핑(pipe)해 두 스케줄을 결합. 첫 번째 스케줄이 기술하는 효과는 항상 두 번째 스케줄이 기술하는 효과보다 먼저 실행됨.

- `s1 >>> s2` 결합 결과
  - 타입(Type): `s1`은 `Schedule[R, A, B]` · `s2`는 `Schedule[R, B, C]` → 결과 `Schedule[R, A, C]`
  - 지연(Delay) `Duration`: `s1`은 `d1` · `s2`는 `d2` → 결과 `d1 + d2`
  - 방출(Emit) `B`: `s1`은 `a` · `s2`는 `b` → 결과 `b`

`>>>` 연산자를 사용해 두 스케줄을 파이핑 가능.

```scala
val totalElapsed = Schedule.spaced(1.second) <* Schedule.recurs(5) >>> Schedule.elapsed
```

#### 5.3 지터링(Jittering)

`jittered`는 하나의 스케줄을 받아, 지연(delay)이 무작위로(randomly) 적용된다는 점을 제외하고 동일한 타입의 또 다른 스케줄을 반환하는 조합자.

- `jittered` (입력 없음) → 출력 타입 `Schedule[Env with Random, In, Out]`
- `jittered(min: Double, max: Double)` → 출력 타입 `Schedule[Env with Random, In, Out]`

어떤 스케줄이든 `jittered`를 호출해 지터(jitter) 적용 가능.

```scala
val jitteredExp = Schedule.exponential(10.milliseconds).jittered
```

리소스가 과부하(overload)나 경합(contention)으로 사용 불가능 상태가 되면, 재시도와 백오프(backoff)만으로는 부족함 → 실패한 API 호출이 모두 동일한 시점에 백오프되면 그 자체가 또 다른 과부하나 경합을 유발하기 때문. 지터(Jitter)는 스케줄의 지연에 약간의 무작위성(randomness)을 추가해, 재시도가 우연히 동기화되어 서비스를 다시 다운시키는 상황을 방지.

`min`과 `max` 매개변수를 가진 형태는, 새 구간 크기(interval size)가 `min * 기존 구간`과 `max * 기존 구간` 사이에서 무작위로 분포되는 새로운 스케줄을 생성.

[연구](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)에 따르면 `Schedule.jittered(0.0, 1.0)`이 재시도에 매우 적합.

#### 5.4 수집(Collecting)

`collectAll`은 어떤 스케줄에 대해 호출하면, 첫 번째 스케줄의 출력들을 청크(chunk)로 수집하는 새 스케줄을 만들어 내는 조합자.

- `collectAll`: 입력 타입 `Schedule[Env, In, Out]` → 출력 타입 `Schedule[Env, In, Chunk[Out]]`

다음 예제에서는 스케줄의 모든 반복(recurrence) 출력을 `Chunk`로 수집하므로, 최종적으로 `Chunk(0, 1, 2, 3, 4)`가 됨.

```scala
val collect = Schedule.recurs(5).collectAll
```

#### 5.5 필터링(Filtering)

`whileInput`과 `whileOutput`을 사용해 스케줄의 입력(input)이나 출력(output) 필터링 가능. ZIO 스케줄에는 이 두 함수의 효과적(effectful) 버전인 `whileInputZIO`와 `whileOutputZIO`도 있음.

- `whileInput`: 입력 타입 `In1 => Boolean` → 출력 타입 `Schedule[Env, In1, Out]`
- `whileOutput`: 입력 타입 `Out => Boolean` → 출력 타입 `Schedule[Env, In, Out]`
- `whileInputZIO`: 입력 타입 `In1 => URIO[Env1, Boolean]` → 출력 타입 `Schedule[Env1, In1, Out]`
- `whileOutputZIO`: 입력 타입 `Out => URIO[Env1, Boolean]` → 출력 타입 `Schedule[Env1, In, Out]`

다음 예제에서는 출력이 5가 되기 전까지 방출되는 모든 출력을 수집하므로, `Chunk(0, 1, 2, 3, 4)`가 됨.

```scala
val res = Schedule.unfold(0)(_ + 1).whileOutput(_ < 5).collectAll
```

#### 5.6 매핑(Mapping)

스케줄을 매핑하는 두 가지 버전, `map`과 그 효과적 버전인 `mapZIO`가 있음.

- `map`: 입력 타입 `f: Out => Out2` → 출력 타입 `Schedule[Env, In, Out2]`
- `mapZIO`: 입력 타입 `f: Out => URIO[Env1, Out2]` → 출력 타입 `Schedule[Env1, In, Out2]`

#### 5.7 좌/우 적용(Left/Right Ap)

`&&` 연산자로 두 스케줄을 교집합할 때, 왼쪽 또는 오른쪽 출력을 무시(ignore)하고 싶은 경우가 있음.

- `*>`: 왼쪽 출력(left output) 무시
- `<*`: 오른쪽 출력(right output) 무시

#### 5.8 수정(Modifying)

스케줄의 지연(delay) 수정.

```scala
val boosted = Schedule.spaced(1.second).delayed(_ => 100.milliseconds)
```

#### 5.9 태핑(Tapping)

스케줄의 입력/출력을 효과적으로(effectfully) 처리해야 할 때 `tapInput`과 `tapOutput` 사용 가능. 로깅(logging) 용도로 주로 활용됨.

```scala
val tappedSchedule = Schedule.count.whileOutput(_ < 5).tapOutput(o => Console.printLine(s"retrying $o").orDie)
```

---

### 6. 예제(Examples)

스케줄을 생성하고 조합하는 예제.

1. 지정된 시간이 경과한 후 재시도 중단하기:

```scala
val expMaxElapsed = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)
```

이 스케줄은 지수 백오프(`exponential`)의 출력을 경과 시간(`elapsed`)으로 파이핑한 뒤, 경과 시간이 30초 미만인 동안에만(`whileOutput(_ < 30.seconds)`) 계속 반복 → 총 경과 시간이 30초를 넘으면 재시도 중단.

2. 특정 예외(exception)가 발생했을 때에만 재시도하기:

```scala
import scala.concurrent.TimeoutException

val whileTimeout = Schedule.exponential(10.milliseconds) && Schedule.recurWhile[Throwable] {
  case _: TimeoutException => true
  case _ => false
}
```

이 스케줄은 지수 백오프와, 입력 오류가 `TimeoutException`인 동안에만 반복하는 스케줄(`recurWhile`)을 교집합(`&&`)으로 결합 → `TimeoutException`이 발생하는 경우에만 지수 백오프 지연으로 재시도하고, 다른 예외에서는 재시도 금지.

---

### 7. 참고 자료

- [Introduction to Scheduling ZIO Effects](https://zio.dev/reference/schedule/)
- [Repetition](https://zio.dev/reference/schedule/repetition)
- [Retrying](https://zio.dev/reference/schedule/retrying)
- [Built-in Schedules](https://zio.dev/reference/schedule/built-in-schedules)
- [Schedule Combinators](https://zio.dev/reference/schedule/combinators)
- [Examples](https://zio.dev/reference/schedule/examples)
- [Exponential Backoff and Jitter (AWS Architecture Blog)](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
