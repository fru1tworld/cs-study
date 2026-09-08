# 04. 코루틴과 동시성

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [coroutines](https://arrow-kt.io/learn/coroutines/)
- [coroutines/parallel](https://arrow-kt.io/learn/coroutines/parallel/)
- [coroutines/racing](https://arrow-kt.io/learn/coroutines/racing/)
- [coroutines/stm](https://arrow-kt.io/learn/coroutines/stm/)
- [coroutines/concurrency-primitives](https://arrow-kt.io/learn/coroutines/concurrency-primitives/)

## 개요

> 원문: [coroutines](https://arrow-kt.io/learn/coroutines/)

Arrow의 코루틴 문서는 Kotlin의 표준 코루틴 라이브러리를 확장하는 고수준 동시성 유틸리티를 다룹니다. 이 프레임워크는 `arrow-fx-coroutines` 라이브러리의 일부이며 구조적 동시성(Structured Concurrency) 원칙을 강조합니다.

### 핵심 개념

Kotlin의 코루틴은 강력하지만, Arrow Fx는 "Kotlin 코드와 다른 프로그래밍 커뮤니티에서 유용하다고 입증된 추가 함수들"을 제공합니다. 특히 여러 개의 중단(suspend) 연산을 처리하는 데 유용합니다.

### 6가지 주요 주제

1. 병렬 처리 (Parallelism) - 독립적인 연산을 동시에 실행
2. 레이싱 (Racing) - 동시 작업에서 특정 결과가 필요한 시나리오
3. 리소스 관리 (Resource Management) - 리소스의 할당과 해제를 안전하게 처리
4. 우아한 종료 (Graceful Shutdown) - 애플리케이션의 질서 있는 종료 관리
5. 트랜잭셔널 메모리 (STM) - 동시 상태 수정을 위한 추상화
6. 동시성 기본 요소 (Concurrency Primitives) - 동시성 패턴을 위한 저수준 빌딩 블록

---

## 병렬 처리 (Parallelism)

> 원문: [coroutines/parallel](https://arrow-kt.io/learn/coroutines/parallel/)

Arrow는 독립적인 연산을 동시에 실행하기 위한 도구를 제공합니다. 주요 메커니즘으로는 병렬 작업을 결합하는 `parZip`과 컬렉션을 처리하는 `parMap`이 있습니다.

### parZip 함수 (병렬 ZIP)

서로 결과를 기다릴 필요가 없는 연산은 `parZip`으로 동시에 실행할 수 있습니다. 아래에서는 사용자 이름과 아바타를 각각 가져온 뒤 마지막 람다에서 결과를 결합합니다.

```kotlin
suspend fun getUser(id: UserId): User = parZip(
    { getUserName(id) },
    { getAvatar(id) }
) { name, avatar -> User(name, avatar) }
```

`parZip`은 여러 연산 블록과 결과를 처리할 후행 람다를 받습니다. 작업이 실패했을 때는 예외 전파와 실행 중인 다른 작업의 취소도 처리하므로, 호출하는 쪽에서 이 과정을 직접 관리할 필요가 없습니다.

### parMap (컬렉션용 병렬 처리)

병렬 작업이 컬렉션 요소에 의존할 때 `parMap`은 각 항목을 동시에 처리합니다:

```kotlin
suspend fun getFriendNames(id: UserId): List<User> =
    getFriendIds(id).parMap { getUserName(it) }
```

컬렉션이 크다고 모든 요소를 한꺼번에 처리하면 과도한 동시성 때문에 성능이 떨어질 수 있습니다. 이런 경우에는 아래처럼 동시성 제한 매개변수로 한 번에 실행할 작업 수를 제어합니다.

```kotlin
suspend fun getFriendNames(id: UserId): List<User> =
    getFriendIds(id).parMap(concurrency = 5) { getUserName(it) }
```

### Flow 통합

`parMap` 연산자는 Kotlin의 Flow 타입과 함께 작동합니다. 동시성이 1을 초과하면 내부 flow들이 병렬로 수집됩니다. 정렬되지 않은 결과의 경우 `parMapUnordered`가 정렬 제약을 제거하여 성능 이점을 제공합니다.

```kotlin
flow.parMap(concurrency = 10) { process(it) }
flow.parMapUnordered(concurrency = 10) { process(it) }
```

### 실험적 awaitAll 패턴

`async`/`.await()` 관용구를 사용하는 대안적 접근법:

```kotlin
suspend fun getUser(id: UserId): User = awaitAll {
    val name = async { getUserName(id) }
    val avatar = async { getAvatar(id) }
    User(name.await(), avatar.await())
}
```

`awaitAll` 블록은 등록된 연산이 모두 끝날 때까지 기다립니다. 연산에서 예외가 발생하면 모든 작업을 취소하는 구조적 동시성 원칙을 따릅니다.

### 타입화된 에러 통합

#### 연산자 내부의 타입화된 에러

각 병렬 분기 안에 `Raise` DSL을 두면 타입화된 에러는 해당 분기 안에서 처리됩니다. 아래에서는 각 `either`가 실패도 값으로 반환하므로 다른 분기를 취소하지 않고 세 결과를 모을 수 있습니다:

```kotlin
suspend fun example() {
    val triple = parZip(
        { either<String, Unit> { logCancellation() } },
        { either<String, Unit> { delay(100); raise("Error") } },
        { either<String, Unit> { logCancellation() } }
    ) { a, b, c -> Triple(a, b, c) }
}
```

#### Raise DSL 내부의 연산자

연산자가 `Raise` DSL 내부에 중첩되면, "타입화된 에러는 구조적 동시성과 동일한 규칙을 따르며 `CancellationException`과 동일하게 동작합니다."

```kotlin
suspend fun example() {
    val res = either {
        parZip(
            { logCancellation() },
            { delay(100); raise("Error") },
            { logCancellation() }
        ) { a, b, c -> Triple(a, b, c) }
    }
}
```

작업 실패는 다른 실행 중인 작업의 취소를 트리거합니다.

#### 에러 누적

`parMapOrAccumulate`는 실패에도 불구하고 모든 작업을 실행하여 모든 에러를 수집합니다:

```kotlin
suspend fun example() {
    val res = listOf(1, 2, 3, 4)
        .parMapOrAccumulate { failOnEven(it) }
}
```

결과: "Either.Left(NonEmptyList(Error, Error))" - 단락(short-circuit)되지 않고 모든 에러가 수집됩니다.

---

## 레이싱 (Racing)

> 원문: [coroutines/racing](https://arrow-kt.io/learn/coroutines/racing/)

레이싱은 여러 연산을 동시에 실행하면서 첫 번째 성공 결과를 선택합니다. 모든 결과를 모아야 할 때보다 여러 경로 중 하나의 결과만 필요한 경우에 사용합니다. 결과가 정해지면 남은 작업을 취소하고 리소스를 정리합니다.

### 핵심 레이싱 원칙

문서에 따르면:

> "레이서가 생성한 첫 번째 값이 레이스에서 승리합니다. 모든 예외는 기록되어야 하지만 레이스에서 승리하지 않으며, 레이스를 기다려야 합니다."

레이스가 끝나면 리소스 누수를 막기 위해 남은 연산을 모두 자동으로 취소합니다.

원하는 레이싱 동작:

- 첫 번째 성공 값이 승리
- 예외는 기록되지만 승리하지 않음; 레이스 완료를 기다림
- 모든 레이서는 레이스 종료 후 취소되어 리소스 정리 보장

### raceN을 사용한 간단한 레이싱

기본적인 2개 또는 3개 연산 레이스의 경우:

```kotlin
suspend fun file(server1: String, server2: String) = raceN(
    { downloadFrom(server1) },
    { downloadFrom(server2) }
).merge()
```

결과는 각 분기의 잠재적 결과를 나타내는 `Either<A, B>`입니다.

### 전통적인 select 접근법

표준 라이브러리의 `select` 표현식은 수동 처리가 필요합니다:

```kotlin
suspend fun getRemoteUser(id: UserId): User = coroutineScope {
    try {
        select {
            async { awaitAfterError { RemoteCache.getUser(id) } }.onAwait { it }
            async { awaitAfterError { LocalCache.getUser(id) } }.onAwait { it }
        }
    } finally {
        coroutineContext.job.cancelChildren()
    }
}

suspend fun <A> awaitAfterError(block: suspend () -> A): A = try {
    block()
} catch (e: Throwable) {
    if (e is CancellationException || NonFatal(e)) throw e
    e.printStackTrace()
    awaitCancellation()
}
```

이 방식은 코루틴 완료, 선택되지 않은 연산의 취소, 예외 로깅, 치명적 예외에 대한 보호를 처리합니다.

### 레이싱 DSL (실험적)

Arrow는 레이싱 로직을 단순화하는 고수준 DSL을 제공합니다:

```kotlin
suspend fun getUserRacing(id: UserId): User = racing {
    race { RemoteCache.getUser(id) }
    race { LocalCache.getUser(id) }
}
```

#### 타임아웃 구현

```kotlin
suspend fun getUserRacing(id: UserId): User = racing {
    race { RemoteCache.getUser(id) }
    race { LocalCache.getUser(id) }
    race {
        delay(10.milliseconds)
        throw TimeoutException()
    }
}
```

#### 예외 승리

`raceOrFail`을 사용하면 예외가 승리할 수 있습니다:

```kotlin
suspend fun getUserRacing(id: UserId): User = racing {
    raceOrFail { RemoteCache.getUser(id) }
    race { LocalCache.getUser(id) }
}
```

#### 조건부 레이싱

```kotlin
suspend fun getUserRacing(ids: NonEmptyList<UserId>): List<User> = racing {
    race { RemoteCache.getUsers(ids) }
    race(condition = { it.size == ids.size }) {
        LocalCache.getCachedUsers(ids)
    }
}
```

결과를 선택하기 전에 지정한 조건을 충족하는지 확인합니다.

#### 사용자 정의 에러 처리

```kotlin
suspend fun customErrorHandling(): String =
    withContext(CoroutineExceptionHandler { ctx, t -> t.printStackTrace() }) {
        racing {
            race {
                delay(2.seconds)
                throw RuntimeException("boom!")
            }
            race {
                delay(10.seconds)
                "Winner!"
            }
        }
    }
```

기본 전략은 "printStackTrace이지만, Ktor와 같은 설치된 핸들러를 존중합니다."

#### 타입화된 에러 통합

`raise`로 발생시킨 타입화된 에러도 레이싱 안에서는 예외처럼 처리됩니다. 표준 `race`는 에러를 성공 결과로 선택하지 않으므로, 에러를 레이스 밖으로 전파하려면 `raceOrFail`을 사용합니다.

## 소프트웨어 트랜잭셔널 메모리 (STM)

> 원문: [coroutines/stm](https://arrow-kt.io/learn/coroutines/stm/)

소프트웨어 트랜잭셔널 메모리(STM)는 "동시 상태 수정을 위한 추상화"를 제공하여 데드락과 레이스 컨디션 없이 안전한 동시 접근을 가능하게 합니다. Arrow의 구현은 Haskell의 STM 패키지와 GHC의 세밀한 잠금 접근법을 기반으로 합니다.

### 핵심 개념

TVar(트랜잭셔널 변수)는 타입 A의 값을 저장하고 동시 수정으로부터 보호합니다. 이 값을 STM 컨텍스트 안에서 읽고 수정하는 연산을 묶어 트랜잭션을 만듭니다. 여기에 `retry`, `orElse`, `catch`를 사용하면 재시도와 분기, 예외 처리도 트랜잭션 안에서 정의할 수 있습니다.

### 상태 읽기 및 쓰기

`STM`을 수신자로 사용하는 함수에 트랜잭션의 읽기와 쓰기를 정의합니다. 이 함수로 작성한 트랜잭션은 `atomically`에 전달해 실행합니다. 아래에서는 출금과 입금을 하나의 이체 연산으로 묶습니다:

```kotlin
import arrow.fx.stm.atomically
import arrow.fx.stm.TVar
import arrow.fx.stm.STM

fun STM.transfer(from: TVar<Int>, to: TVar<Int>, amount: Int) {
    withdraw(from, amount)
    deposit(to, amount)
}

fun STM.deposit(acc: TVar<Int>, amount: Int) {
    val current = acc.read()
    acc.write(current + amount)
    // 또는 축약형: acc.modify { it + amount }
}

fun STM.withdraw(acc: TVar<Int>, amount: Int) {
    val current = acc.read()
    require(current - amount >= 0) { "돈이 부족합니다!" }
    acc.write(current - amount)
}

suspend fun example() {
    val acc1 = TVar.new(500)
    val acc2 = TVar.new(300)

    acc1.unsafeRead() shouldBe 500
    acc2.unsafeRead() shouldBe 300

    atomically { transfer(acc1, acc2, 50) }

    acc1.unsafeRead() shouldBe 450
    acc2.unsafeRead() shouldBe 350
}
```

### 속성 위임

접근 구문을 단순화합니다:

```kotlin
fun STM.deposit(accVar: TVar<Int>, amount: Int) {
    var acc by accVar  // 위임
    val current = acc   // 암시적 읽기
    acc = current + amount  // 암시적 쓰기
}
```

### 추가 STM 데이터 구조

- TQueue: 트랜잭셔널 가변 큐
- TMVar: 비어있을 수 있는 트랜잭셔널 변수
- TSet/TMap: 트랜잭셔널 Set과 Map
- TArray: TVar의 배열
- TSemaphore: 트랜잭셔널 세마포어

이들은 영향받는 항목만 잠그기 때문에 일반 컬렉션을 TVar로 래핑하는 것보다 성능이 좋습니다.

### 재시도 (Retry)

트랜잭션은 유효하지 않은 상태가 발생할 때 `retry()`를 사용하여 중단할 수 있으며, 접근한 변수가 변경되면 자동으로 재시작됩니다:

```kotlin
fun STM.withdraw(acc: TVar<Int>, amount: Int) {
    val current = acc.read()
    if (current - amount >= 0) acc.write(current - amount)
    else retry()
}

suspend fun example() = coroutineScope {
    val acc1 = TVar.new(0)
    val acc2 = TVar.new(300)

    async {
        delay(500)
        atomically { acc1.write(100_000_000) }
    }

    atomically { transfer(acc1, acc2, 50) }

    acc1.unsafeRead() shouldBe (100_000_000 - 50)
    acc2.unsafeRead() shouldBe 350
}
```

중요: 트랜잭션은 임의로 중단되고 다시 실행될 수 있으므로 작고 부작용이 없게 유지하세요.

### orElse를 사용한 분기

`orElse`는 `retry()` 호출을 감지하고 대체 로직을 제공합니다:

```kotlin
fun STM.transaction(v: TVar<Int>): Int? =
    stm {
        val result = v.read()
        check(result in 0 .. 10)
        result
    } orElse { null }

suspend fun example() {
    val v = TVar.new(100)
    v.unsafeRead() shouldBe 100

    atomically { transaction(v) } shouldBe null
    atomically { v.write(5) }
    atomically { transaction(v) } shouldBe 5
}
```

### 예외 처리

Kotlin의 내장 예외 처리 대신 STM의 `catch` 함수를 사용하세요. 이는 상태 변경을 적절히 롤백합니다:

```kotlin
// 피하세요: try/catch는 상태를 되돌리지 않음
// 권장: 적절한 롤백을 위해 STM의 catch 함수 사용
```

문서는 "`try {...} catch`를 사용하는 것은 권장되지 않습니다. 왜냐하면 `try` 내부의 모든 상태 변경은 예외 발생 시 되돌려지지 않기 때문입니다."라고 경고합니다.

### 중요한 주의사항

트랜잭션은 임의로 재시작될 수 있어 부작용이나 리소스 파이널라이저에 적합하지 않습니다.

---

## 동시성 기본 요소 (Concurrency Primitives)

> 원문: [coroutines/concurrency-primitives](https://arrow-kt.io/learn/coroutines/concurrency-primitives/)

Arrow Fx는 Kotlin 멀티플랫폼에서 사용할 수 있는 기본 동시성 도구를 제공합니다.

### 개요

이 타입들은 일반 애플리케이션에서 직접 쓰기보다는 더 큰 동시성 패턴을 구현할 때 바탕이 됩니다. 테스트나 동기화 시뮬레이션에도 사용할 수 있으며, Arrow Fx는 모든 플랫폼의 Kotlin 멀티플랫폼(KMP) 프로젝트를 지원합니다.

### 구조적 동시성

페이지는 Kotlin 동시성의 핵심으로 "구조적 동시성"을 이해하는 것을 강조합니다. 예외와 취소 시 코루틴이 어떻게 동작해야 하는지에 대한 자세한 설명은 공식 Kotlin 코루틴 가이드를 참조하세요. "이 복잡성의 대부분은 Arrow Fx 고수준 연산을 사용할 때 숨겨집니다."

### 세 가지 동시성 프리미티브

#### Atomic

페이지는 "Kotlin 2.1.20부터 공통 원자적 타입을 표준 라이브러리에서 사용할 수 있습니다"라고 언급하며, `arrow-atomic` 대신 Kotlin의 네이티브 구현을 사용할 것을 권장합니다.

별도의 `arrow-atomic` 라이브러리는 `getAndSet`, `getAndUpdate`, `compareAndSet`과 같은 원자적 연산을 갖춘 멀티플랫폼 원자적 참조를 제공합니다.

중요한 주의사항: Kotlin Native에서 기본 타입과 함께 제네릭 `Atomic`을 사용하는 것을 피하세요; 대신 `AtomicInt`와 `AtomicBoolean`을 사용하세요.

```kotlin
import arrow.atomic.AtomicInt

val counter = AtomicInt(0)
counter.incrementAndGet() // 1
counter.getAndSet(10) // 1 반환, 값은 10으로 설정
counter.compareAndSet(expected = 10, new = 20) // true, 값은 20으로 설정
```

#### CountDownLatch

`CountDownLatch`는 정해진 수의 완료 신호가 올 때까지 기다립니다. 아래 예제에서는 세 작업이 각각 `countDown()`을 호출한 뒤에야 `await()` 다음 줄로 넘어갑니다. Java의 `java.util.concurrent.CountDownLatch`를 모델로 한 도구입니다.

```kotlin
import arrow.fx.coroutines.CountDownLatch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun example() = coroutineScope {
    val latch = CountDownLatch(3)

    // 3개의 작업 시작
    repeat(3) { i ->
        async {
            // 작업 수행
            println("작업 $i 완료")
            latch.countDown()
        }
    }

    // 모든 작업이 완료될 때까지 대기
    latch.await()
    println("모든 작업 완료!")
}
```

#### CyclicBarrier

`CyclicBarrier`는 여러 코루틴이 같은 지점에 도달할 때까지 서로 기다리게 합니다. 각 코루틴이 `await()`를 호출하면 정해진 수만큼 모일 때까지 일시 중단되었다가 함께 재개됩니다. Java의 `java.util.concurrent.CyclicBarrier`를 모델로 하며, 해제한 뒤 다시 사용할 수 있어 "순환적"이라는 이름이 붙었습니다.

```kotlin
import arrow.fx.coroutines.CyclicBarrier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun example() = coroutineScope {
    val barrier = CyclicBarrier(3)

    repeat(3) { i ->
        async {
            println("작업 $i: 1단계 시작")
            // 1단계 작업 수행

            barrier.await() // 모든 작업이 1단계를 완료할 때까지 대기

            println("작업 $i: 2단계 시작")
            // 2단계 작업 수행
        }
    }
}
```

## 공유 상태와 취소를 해석하는 기준

- `parZip`과 `parMap`은 실행 순서를 보장하는 순차 연산이 아님. 순서에 의존하는 부수효과는 따로 설계해야 함.
- 병렬 분기 내부에서 `either`로 실패를 값으로 반환하면, 바깥 병렬 연산에서는 그 분기가 정상적으로 값을 반환한 것으로 보임.
- 바깥 Raise 스코프까지 실패가 전파되는 경우와 분기별 결과를 모으는 경우를 구분함.
- `raceN`, 실험적 `race`, `raceOrFail`은 승자와 실패를 처리하는 규칙이 다르므로 아래 원문별 설명과 함께 사용함.
- STM 블록은 충돌 시 재실행 가능하므로 네트워크 호출, 파일 쓰기 같은 재실행 불가능한 부수효과를 넣지 않음.
- Kotlin 공통 atomics는 Kotlin 2.1.20에 도입됨. Arrow 2.2.3의 atomics 구현 변경은 [해당 릴리스 공지](https://arrow-kt.io/community/blog/2026/06/04/arrow-2-2-3/) 참고.
