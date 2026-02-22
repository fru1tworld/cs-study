# Arrow-kt 코루틴 (동시성과 리소스)

> 이 문서는 [Arrow-kt 공식 문서](https://arrow-kt.io/learn/coroutines/)의 한국어 번역본입니다.

## 목차

1. [개요](#개요)
2. [병렬 처리 (Parallelism)](#병렬-처리-parallelism)
3. [레이싱 (Racing)](#레이싱-racing)
4. [리소스 안전성 (Resource Safety)](#리소스-안전성-resource-safety)
5. [우아한 종료 (Graceful Shutdown)](#우아한-종료-graceful-shutdown)
6. [소프트웨어 트랜잭셔널 메모리 (STM)](#소프트웨어-트랜잭셔널-메모리-stm)
7. [동시성 기본 요소 (Concurrency Primitives)](#동시성-기본-요소-concurrency-primitives)

---

## 개요

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

Arrow는 독립적인 연산을 동시에 실행하기 위한 도구를 제공합니다. 주요 메커니즘으로는 병렬 작업을 결합하는 `parZip`과 컬렉션을 처리하는 `parMap`이 있습니다.

### parZip 함수 (병렬 ZIP)

"우리는 종종 병렬로 수행하고 싶은 독립적인 연산을 가지고 있습니다." 프레임워크는 `parZip`을 사용하여 여러 동시 작업의 실행을 결합합니다.

```kotlin
suspend fun getUser(id: UserId): User = parZip(
    { getUserName(id) },
    { getAvatar(id) }
) { name, avatar -> User(name, avatar) }
```

핵심 이점: "`parZip`을 사용하는 것은 동시성에 대한 고수준 관점 때문만이 아니라... 예외 전파와 실행 중인 연산 취소라는 복잡한 작업을 처리해주기 때문에 필수적입니다."

이 함수는 여러 연산 블록과 결과를 함께 처리하는 후행 람다를 받습니다. 예를 들어, 사용자 이름과 아바타를 동시에 가져온 다음 User 객체로 결합하는 사용 사례가 있습니다. 프레임워크는 실패 발생 시 예외 전파와 작업 취소를 자동으로 관리합니다.

### parMap (컬렉션용 병렬 처리)

병렬 작업이 컬렉션 요소에 의존할 때 `parMap`은 각 항목을 동시에 처리합니다:

```kotlin
suspend fun getFriendNames(id: UserId): List<User> =
    getFriendIds(id).parMap { getUserName(it) }
```

중요한 고려사항: 대규모 데이터셋에서 과도한 동시성은 성능 문제를 일으킬 수 있습니다. Arrow는 동시에 실행되는 작업 수를 제어하는 동시성 제한 매개변수를 통해 이를 해결합니다.

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

`awaitAll` 블록 내에서 등록된 연산들은 집단적으로 대기됩니다. 어떤 예외든 모든 작업을 취소하며, 구조적 동시성 원칙을 준수합니다.

### 타입화된 에러 통합

#### 연산자 내부의 타입화된 에러

`Raise` DSL이 Arrow 연산자 내부에 중첩될 때, 타입화된 에러는 개별 람다 내에 범위가 지정되어 작업 간 오염을 방지합니다:

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

레이싱은 여러 연산이 동시에 실행되고, 첫 번째 성공 결과만이 승자를 결정하는 것을 허용합니다. 이는 모든 결과가 중요한 병렬 처리와 다릅니다. 프레임워크는 패배한 레이서들이 취소되고 그들의 리소스가 정리되도록 보장합니다.

### 핵심 레이싱 원칙

문서에 따르면:

> "레이서가 생성한 첫 번째 값이 레이스에서 승리합니다. 모든 예외는 기록되어야 하지만 레이스에서 승리하지 않으며, 레이스를 기다려야 합니다."

레이스가 완료되면, 모든 남은 참가자들은 리소스 누수를 방지하기 위해 자동으로 취소됩니다.

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

이 접근법은 네 가지 중요한 엣지 케이스를 처리합니다: 코루틴 완료, 패자 취소, 예외 로깅, 치명적 예외 보호.

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

조건은 승리를 선언하기 전에 결과가 특정 요구 사항을 충족하는지 확인합니다.

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

`raise`와 함께, 에러는 예외처럼 동작합니다. 에러 전파를 위해 `raceOrFail`을 사용하세요; 표준 `race`는 에러를 성공으로 간주하지 않습니다.

---

## 리소스 안전성 (Resource Safety)

Arrow의 Resource 시스템은 예외나 취소 시에도 리소스의 할당과 해제를 안전하게 관리합니다. Kotlin의 코루틴 및 구조적 동시성과 통합됩니다.

### 문제 이해하기

Kotlin에서 전통적인 리소스 관리는 예외 발생 시 리소스를 누수할 수 있습니다:

```kotlin
class UserProcessor {
    fun start(): Unit = println("Creating UserProcessor")
    fun shutdown(): Unit = println("Shutting down UserProcessor")
}

class DataSource {
    fun connect(): Unit = println("Connecting dataSource")
    fun close(): Unit = println("Closing dataSource")
}

class Service(val db: DataSource, val userProcessor: UserProcessor) {
    suspend fun processData(): List<String> = TODO()
}

suspend fun example() {
    val userProcessor = UserProcessor().also { it.start() }
    val dataSource = DataSource().also { it.connect() }
    val service = Service(dataSource, userProcessor)
    service.processData() // 여기서 예외 발생 = 리소스 누수
    dataSource.close()
    userProcessor.shutdown()
}
```

`use` 블록이 도움이 되지만 제한사항이 있습니다:
1. JVM 전용이며 인터페이스 구현이 필요
2. 외부 타입은 래핑이 필요
3. 깊은 중첩으로 조합성이 감소
4. 메서드 이름이 `close`로 강제됨
5. 정리 과정에서 suspend 함수 실행 불가
6. 종료 신호에 대한 가시성 없음

### 해결책: 3단계 리소스 모델

리소스는 세 단계를 따릅니다:
1. 획득 (Acquiring) - 리소스 획득
2. 사용 (Using) - 리소스 사용
3. 해제 (Releasing) - 리소스 해제

Arrow는 예외나 취소에 관계없이 적절한 정리를 보장하며 획득과 해제를 묶습니다.

### 두 가지 구현 접근법

#### 1. ResourceScope DSL

`install` 함수는 획득과 해제를 모두 처리합니다. "실행이 어떻게 종료되었는지에 따라 다른 작업을 수행할 수 있는 유연성을 제공합니다: 성공적 완료, 예외, 또는 취소."

```kotlin
suspend fun ResourceScope.userProcessor(): UserProcessor =
    install({ UserProcessor().also { it.start() } }) { p, _ ->
        p.shutdown()
    }

suspend fun ResourceScope.dataSource(): DataSource =
    install({ DataSource().also { it.connect() } }) { ds, _ ->
        ds.close()
    }

suspend fun example(): Unit = resourceScope {
    val service = parZip(
        { userProcessor() },
        { dataSource() }
    ) { userProcessor, ds -> Service(ds, userProcessor) }
    val data = service.processData()
    println(data)
}
```

핵심 참고: "Install은 부분 획득을 방지하기 위해 acquire와 release를 NonCancellable로 호출합니다."

#### 2. Resource 타입 별칭

`Resource<T>`는 획득과 해제를 위한 레시피입니다. `resourceScope` 내부에서 `.bind()`를 호출하여 활성화합니다:

```kotlin
val userProcessor: Resource<UserProcessor> = resource({
    UserProcessor().also { it.start() }
}) { p, _ -> p.shutdown() }

val dataSource: Resource<DataSource> = resource({
    DataSource().also { it.connect() }
}) { ds, exitCase ->
    println("Releasing $ds with exit: $exitCase")
    withContext(Dispatchers.IO) { ds.close() }
}

val service: Resource<Service> = resource {
    Service(dataSource.bind(), userProcessor.bind())
}

suspend fun example(): Unit = resourceScope {
    val data = service.bind().processData()
    println(data)
}
```

기술적으로, "Resource는 ResourceScope를 사용하는 매개변수 없는 함수에 대한 타입 별칭에 불과합니다."

```kotlin
typealias Resource<A> = suspend ResourceScope.() -> A
```

### Java 통합

Arrow는 JVM 환경에서 `AutoCloseable` 타입과 작업하기 위한 `closeable()` 함수를 제공합니다.

```kotlin
suspend fun example(): Unit = resourceScope {
    val connection = closeable { getConnection() }
    // connection 사용
}
```

### 타입화된 에러 통합

스코프 중첩 순서가 중요합니다:

패턴 1 (either가 외부, resourceScope가 내부):
```kotlin
suspend fun pattern1(): Either<String, Unit> = either {
    resourceScope {
        val resource = install({ acquireResource() }) { r, _ -> r.release() }
        // bind 실패 시 리소스는 Cancelled 상태로 해제됨
    }
}
```

패턴 2 (resourceScope가 외부, either가 내부):
```kotlin
suspend fun pattern2(): Unit = resourceScope {
    val result = either {
        // 에러 발생 시 리소스는 정상 완료 상태로 해제됨
    }
}
```

두 패턴 모두 적절한 정리를 보장하지만, 종료 상태 처리 방식에 따라 동작이 다릅니다.

### ExitCase 매개변수

파이널라이저는 실행이 성공했는지, 에러가 발생했는지, 또는 취소되었는지를 보여주는 종료 정보를 받습니다:

```kotlin
resource({
    acquireResource()
}) { resource, exitCase ->
    when (exitCase) {
        is ExitCase.Completed -> println("정상 완료")
        is ExitCase.Cancelled -> println("취소됨: ${exitCase.exception}")
        is ExitCase.Failure -> println("실패: ${exitCase.failure}")
    }
    resource.release()
}
```

### 핵심 기능

- 획득/해제 단계에 대한 NonCancellable 실행
- `closeable()` 함수를 통한 Java AutoCloseable 통합
- 전통적인 `use` 블록과 달리 멀티플랫폼 지원
- 다양한 종료 케이스에 반응하는 유연한 파이널라이저
- 복잡한 리소스 의존성을 위한 조합 가능한 패턴

---

## 우아한 종료 (Graceful Shutdown)

"우아한 종료가 필요한 애플리케이션을 구축할 때 일반적으로 많은 플랫폼별 코드를 작성해야 합니다. 이 라이브러리는 KotlinX Coroutines와 구조적 동시성을 사용하여 Kotlin MPP를 활용함으로써 그 문제를 해결하는 것을 목표로 합니다."

이 페이지는 Arrow의 SuspendApp이 여러 플랫폼에서 깔끔한 애플리케이션 종료를 가능하게 하는 방법을 보여줍니다.

### 간단한 예제

문서는 인터럽트 처리를 보여주는 기본 구현을 제공합니다:

```kotlin
import arrow.continuations.SuspendApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun main() = SuspendApp {
    try {
        println("앱이 시작되었습니다! 종료 요청을 기다리는 중.")
        while (true) {
            delay(2_500)
            println("Ping")
        }
    } catch (e: CancellationException) {
        println("앱 정리 중... 10초가 걸립니다...")
        withContext(NonCancellable) { delay(10_000) }
        println("정리 완료. 앱 종료를 해제합니다")
    }
}
```

사용자는 Ctrl+C (SIGINT) 또는 `kill PID` (SIGTERM)를 통해 종료를 트리거할 수 있습니다.

### Arrow의 Resource와 함께 SuspendApp 사용

Resource 패턴은 구조적 동시성과 통합됩니다:

```kotlin
fun main() = SuspendApp {
    resourceScope {
        install({ println("리소스 생성 중") }) { _, exitCase ->
            println("ExitCase: $exitCase")
            println("종료하는 데 10초가 걸립니다")
            delay(10_000)
            println("종료 완료")
        }
        println("획득한 리소스로 애플리케이션 실행 중.")
        awaitCancellation()
    }
}
```

"CoroutineScope가 취소되면, 모든 suspend 파이널라이저는 Job#join에 백프레셔를 가합니다."

Ctrl+C 후 예상 출력:
```
리소스 생성 중
획득한 리소스로 애플리케이션 실행 중.
ExitCase: Cancelled(...)
종료하는 데 10초가 걸립니다
종료 완료
```

### 지원되는 플랫폼

현재 지원되는 타겟:
- JVM
- MacOS (X64 & Arm64)
- NodeJS
- Windows (MingwX64)
- Linux

모바일과 브라우저 타겟은 지원되지 않습니다.

#### Node.js 구성

```kotlin
js(IR) {
    nodejs {
        binaries.executable()
    }
}
```

#### Native 구성

```kotlin
linuxX64 { binaries.executable() }
mingwX64 { binaries.executable() }
macosArm64 { binaries.executable() }
macosX64 { binaries.executable() }
```

### 핵심 개념

리소스는 suspend 파이널라이저를 통해 취소를 적절히 처리하여 애플리케이션 종료 전에 정리 작업이 완료되도록 보장합니다. 프레임워크는 플랫폼별 시그널 처리 복잡성을 추상화합니다.

---

## 소프트웨어 트랜잭셔널 메모리 (STM)

소프트웨어 트랜잭셔널 메모리(STM)는 "동시 상태 수정을 위한 추상화"를 제공하여 데드락과 레이스 컨디션 없이 안전한 동시 접근을 가능하게 합니다. Arrow의 구현은 Haskell의 STM 패키지와 GHC의 세밀한 잠금 접근법을 기반으로 합니다.

### 핵심 개념

TVar (트랜잭셔널 변수): 타입 A의 값을 보유하는 보호된 변수를 나타내는 기본 빌딩 블록입니다. 수정은 STM 컨텍스트 내부에 있어야 합니다.

기본 프리미티브: `retry`, `orElse`, `catch`가 복잡한 트랜잭션의 기초를 형성합니다.

### 상태 읽기 및 쓰기

함수는 트랜잭션을 설명하기 위해 `STM`을 리시버로 사용합니다. 이 설명은 즉시 실행되지 않으며 - `atomically`에 전달될 때만 실행됩니다:

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

Arrow Fx는 멀티플랫폼 Kotlin 개발을 지원하는 기초적인 동시성 빌딩 블록을 제공합니다.

### 개요

문서에 명시된 대로: "이러한 타입들은 일반적으로 애플리케이션 코드에서 발견되지 않지만, 더 큰 패턴을 위한 필수적인 기초 블록을 제공합니다." 테스트와 동기화 시뮬레이션에 특히 유용합니다.

핵심 기능: Arrow Fx는 모든 플랫폼에서 Kotlin 멀티플랫폼(KMP) 프로젝트를 지원하며 멀티플랫폼 준비가 되어 있습니다.

### 구조적 동시성

페이지는 Kotlin 동시성의 핵심으로 "구조적 동시성"을 이해하는 것을 강조합니다. 예외와 취소 시 코루틴이 어떻게 동작해야 하는지에 대한 자세한 설명은 공식 Kotlin 코루틴 가이드를 참조하세요. "이 복잡성의 대부분은 Arrow Fx 고수준 연산을 사용할 때 숨겨집니다."

### 세 가지 동시성 프리미티브

#### Atomic

페이지는 "버전 2.1.0부터 일반적인 원자적 타입들이 Kotlin 표준 라이브러리에서 직접 사용 가능합니다"라고 언급하며, `arrow-atomic` 대신 Kotlin의 네이티브 구현을 사용할 것을 권장합니다.

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

`CountDownLatch`는 "주어진 수의 카운트다운 신호를 기다리는 것을 허용"하며, Java의 `java.util.concurrent.CountDownLatch`를 모델링합니다.

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

`CyclicBarrier`는 코루틴들이 계속하기 전에 체크포인트에서 서로를 기다릴 수 있게 하는 동기화 메커니즘입니다. 각 코루틴은 `await()`를 호출하여 필요한 수가 배리어에 도달할 때까지 일시 중단되고, 그 다음 모두 함께 재개됩니다. Java의 `java.util.concurrent.CyclicBarrier`를 미러링합니다.

"순환적"이라고 불리는 이유는 해제 후 재사용 가능하기 때문입니다.

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

---

## 요약

Arrow의 코루틴 및 동시성 기능은 Kotlin의 코루틴을 확장하여 다음을 제공합니다:

| 기능 | 설명 |
|------|------|
| parZip / parMap | 구조화된 에러 처리와 취소를 포함한 병렬 실행 |
| racing | 먼저 성공한 결과가 승리하는 경쟁 연산 |
| resourceScope | 예외와 취소 시에도 안전한 리소스 관리 |
| SuspendApp | 크로스 플랫폼 우아한 종료 |
| STM | 데드락 없는 동시 상태 수정 |
| Atomic / CountDownLatch / CyclicBarrier | 저수준 동기화 프리미티브 |

이러한 도구들은 모두 구조적 동시성 원칙을 따르며, 예외와 취소 시 예측 가능한 동작을 보장합니다.

---

## 참고 자료

- [Arrow-kt 공식 문서](https://arrow-kt.io)
- [Arrow API 문서](https://apidocs.arrow-kt.io)
- [Kotlin 코루틴 가이드](https://kotlinlang.org/docs/coroutines-guide.html)
- [GitHub: arrow-kt](https://github.com/arrow-kt)
