# Kotlin Coroutines

> 공식 문서: https://kotlinlang.org/docs/coroutines-guide.html
> 라이브러리: kotlinx-coroutines-core

## 개요

코루틴은 일시 중단 가능한 계산(suspendable computation)입니다. 스레드와 유사하지만 훨씬 가볍고, 하나의 스레드에서 여러 코루틴이 실행될 수 있습니다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch {
        delay(1000L)
        println("World!")
    }
    println("Hello")
}
// 출력:
// Hello
// World!
```

---

## 의존성 설정

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // Android
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // 테스트
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
```

---

## 코루틴 빌더

### launch

결과 없이 새 코루틴을 시작합니다. `Job`을 반환합니다.

```kotlin
fun main() = runBlocking {
    val job = launch {
        delay(1000L)
        println("Task completed")
    }

    println("Started")
    job.join()  // 코루틴 완료 대기
    println("Done")
}
// Started
// Task completed
// Done
```

### async

결과를 반환하는 코루틴을 시작합니다. `Deferred<T>`를 반환합니다.

```kotlin
fun main() = runBlocking {
    val deferred = async {
        delay(1000L)
        "Result"
    }

    println("Computing...")
    val result = deferred.await()  // 결과 대기
    println("Result: $result")
}

// 병렬 실행
suspend fun fetchTwoValues(): Pair<Int, Int> = coroutineScope {
    val one = async { fetchOne() }
    val two = async { fetchTwo() }
    Pair(one.await(), two.await())  // 동시에 실행됨
}
```

### runBlocking

블로킹 코드와 코루틴 연결. 현재 스레드를 블록합니다.

```kotlin
fun main() = runBlocking {
    // 코루틴 코드
    delay(1000L)
    println("Hello")
}

// 주로 main 함수나 테스트에서 사용
@Test
fun testSuspendFunction() = runBlocking {
    val result = mySuspendFunction()
    assertEquals("expected", result)
}
```

### coroutineScope

현재 스코프 내에서 새 스코프 생성. 모든 자식이 완료될 때까지 대기합니다.

```kotlin
suspend fun doWork() = coroutineScope {
    val job1 = launch {
        delay(1000L)
        println("Task 1")
    }
    val job2 = launch {
        delay(500L)
        println("Task 2")
    }
}  // 두 작업 모두 완료될 때까지 대기
```

---

## Dispatchers

코루틴이 실행될 스레드(풀)를 결정합니다.

| Dispatcher | 설명 | 용도 |
|------------|------|------|
| `Dispatchers.Default` | 공유 스레드 풀 (CPU 코어 수) | CPU 집약적 작업 |
| `Dispatchers.IO` | 최대 64개 스레드 | I/O 작업 (파일, 네트워크) |
| `Dispatchers.Main` | 메인/UI 스레드 | UI 업데이트 (Android) |
| `Dispatchers.Unconfined` | 호출 스레드에서 시작 | 특수 상황에서만 사용 |

```kotlin
fun main() = runBlocking {
    launch(Dispatchers.Default) {
        println("Default: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.IO) {
        println("IO: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.Unconfined) {
        println("Unconfined: ${Thread.currentThread().name}")
    }
}
```

### withContext

디스패처를 일시적으로 전환합니다.

```kotlin
suspend fun fetchData(): String = withContext(Dispatchers.IO) {
    // I/O 작업
    readFromNetwork()
}

suspend fun processData() = withContext(Dispatchers.Default) {
    // CPU 집약적 작업
    computeHeavyTask()
}

// 조합
suspend fun loadAndProcess() {
    val data = fetchData()  // IO
    val processed = processData()  // Default
    withContext(Dispatchers.Main) {
        updateUI(processed)  // Main
    }
}
```

### 커스텀 Dispatcher

```kotlin
// 단일 스레드
val singleThread = newSingleThreadContext("MySingleThread")

// 고정 스레드 풀
val fixedPool = newFixedThreadPoolContext(4, "MyPool")

// 사용 후 닫기
singleThread.close()
fixedPool.close()

// 또는 use 사용
newSingleThreadContext("temp").use { dispatcher ->
    runBlocking(dispatcher) {
        // ...
    }
}
```

---

## Structured Concurrency

부모-자식 관계로 코루틴 생명주기를 관리합니다.

### 계층 구조

```kotlin
fun main() = runBlocking {  // 부모
    launch {  // 자식 1
        launch {  // 손자 1
            delay(1000L)
            println("Grandchild 1")
        }
        println("Child 1")
    }
    launch {  // 자식 2
        println("Child 2")
    }
}  // 모든 자식이 완료될 때까지 대기
```

### 취소 전파

```kotlin
fun main() = runBlocking {
    val parent = launch {
        val child1 = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child 1 cancelled")
            }
        }
        val child2 = launch {
            delay(Long.MAX_VALUE)
        }
        delay(100L)
        println("Parent cancelling children")
    }

    delay(200L)
    parent.cancel()  // 모든 자식도 취소됨
    parent.join()
}
```

---

## Job과 취소

### Job 상태

```
                                      wait children
    +-----+ start  +--------+ complete   +-------------+  finish  +-----------+
    | New | -----> | Active | ---------> | Completing  | -------> | Completed |
    +-----+        +--------+            +-------------+          +-----------+
                       | cancel / fail       |
                       |                     |
                       V                     V
                  +----------+  finish  +-----------+
                  | Cancelling| -------> | Cancelled |
                  +----------+          +-----------+
```

### 취소

```kotlin
fun main() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            println("Job: $i")
            delay(500L)
        }
    }

    delay(1300L)
    println("Cancelling")
    job.cancel()  // 취소 요청
    job.join()    // 완료 대기
    // 또는 job.cancelAndJoin()
    println("Done")
}
```

### 협력적 취소

취소에 반응하려면 취소 상태를 확인해야 합니다.

```kotlin
fun main() = runBlocking {
    val job = launch {
        var i = 0
        // isActive 확인
        while (isActive) {
            println("Job: ${i++}")
            // CPU 집약적 작업 (delay 없음)
        }
    }

    delay(100L)
    job.cancelAndJoin()
}

// ensureActive 사용
launch {
    while (true) {
        ensureActive()  // 취소되면 CancellationException 발생
        // 작업
    }
}

// yield 사용
launch {
    while (true) {
        yield()  // 다른 코루틴에 실행 기회 제공 + 취소 확인
        // 작업
    }
}
```

### 리소스 정리

```kotlin
fun main() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                println("Job: $i")
                delay(500L)
            }
        } catch (e: CancellationException) {
            println("Job was cancelled")
            throw e  // 재발생시켜야 취소 처리됨
        } finally {
            println("Cleanup")
            // 정리 작업
        }
    }

    delay(1300L)
    job.cancelAndJoin()
}

// finally에서 suspend 함수 호출
finally {
    withContext(NonCancellable) {
        delay(1000L)  // 취소된 상태에서도 실행
        println("Cleanup with delay")
    }
}
```

### 타임아웃

```kotlin
// withTimeout - 시간 초과시 TimeoutCancellationException 발생
try {
    val result = withTimeout(1000L) {
        delay(2000L)
        "Result"
    }
} catch (e: TimeoutCancellationException) {
    println("Timed out")
}

// withTimeoutOrNull - 시간 초과시 null 반환
val result = withTimeoutOrNull(1000L) {
    delay(2000L)
    "Result"
}
println(result)  // null
```

---

## 예외 처리

### 예외 전파

```kotlin
// launch: 예외가 부모로 전파됨
fun main() = runBlocking {
    try {
        launch {
            throw RuntimeException("Error")
        }
    } catch (e: Exception) {
        // 여기서 잡히지 않음!
        println("Caught: $e")
    }
}

// async: await()에서 예외 발생
fun main() = runBlocking {
    val deferred = async {
        throw RuntimeException("Error")
    }

    try {
        deferred.await()
    } catch (e: Exception) {
        println("Caught: $e")  // 여기서 잡힘
    }
}
```

### CoroutineExceptionHandler

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught: $exception")
}

fun main() = runBlocking {
    val job = GlobalScope.launch(handler) {
        throw RuntimeException("Error")
    }
    job.join()
}
```

### SupervisorJob

자식의 실패가 다른 자식에게 영향을 주지 않습니다.

```kotlin
fun main() = runBlocking {
    val supervisor = SupervisorJob()

    with(CoroutineScope(coroutineContext + supervisor)) {
        val child1 = launch {
            println("Child 1 started")
            throw RuntimeException("Child 1 failed")
        }

        val child2 = launch {
            delay(100L)
            println("Child 2 completed")  // 실행됨
        }

        child1.join()
        child2.join()
    }
}
```

### supervisorScope

```kotlin
suspend fun main() = coroutineScope {
    supervisorScope {
        launch {
            throw RuntimeException("Failed")
        }
        launch {
            delay(100L)
            println("Still running")  // 실행됨
        }
    }
}
```

---

## suspend 함수

### 정의

```kotlin
suspend fun fetchUser(id: Long): User {
    delay(1000L)  // 일시 중단
    return User(id, "John")
}

suspend fun fetchUserWithPosts(userId: Long): UserWithPosts {
    // 순차 실행
    val user = fetchUser(userId)
    val posts = fetchPosts(userId)
    return UserWithPosts(user, posts)
}

suspend fun fetchUserWithPostsParallel(userId: Long): UserWithPosts =
    coroutineScope {
        // 병렬 실행
        val user = async { fetchUser(userId) }
        val posts = async { fetchPosts(userId) }
        UserWithPosts(user.await(), posts.await())
    }
```

### Continuation

suspend 함수는 내부적으로 Continuation을 받습니다.

```kotlin
// 컴파일러가 변환
suspend fun hello(): String

// 대략 이렇게 됨
fun hello(continuation: Continuation<String>): Any?
```

### suspendCoroutine

콜백 기반 API를 suspend 함수로 변환합니다.

```kotlin
suspend fun awaitCallback(): String = suspendCoroutine { cont ->
    someAsyncApi(object : Callback {
        override fun onSuccess(result: String) {
            cont.resume(result)
        }
        override fun onError(e: Exception) {
            cont.resumeWithException(e)
        }
    })
}

// 취소 가능한 버전
suspend fun awaitCancellable(): String = suspendCancellableCoroutine { cont ->
    val disposable = someAsyncApi(object : Callback {
        override fun onSuccess(result: String) {
            cont.resume(result)
        }
        override fun onError(e: Exception) {
            cont.resumeWithException(e)
        }
    })

    cont.invokeOnCancellation {
        disposable.dispose()
    }
}
```

---

## CoroutineScope

### 스코프 생성

```kotlin
class MyService {
    // 커스텀 스코프
    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )

    fun doWork() {
        scope.launch {
            // 작업
        }
    }

    fun close() {
        scope.cancel()  // 모든 작업 취소
    }
}
```

### CoroutineContext

코루틴의 컨텍스트를 구성합니다.

```kotlin
val context = Job() +
    Dispatchers.Default +
    CoroutineName("myCoroutine") +
    CoroutineExceptionHandler { _, e -> println(e) }

val scope = CoroutineScope(context)

// 컨텍스트 요소 접근
scope.launch {
    println(coroutineContext[CoroutineName]?.name)  // "myCoroutine"
    println(coroutineContext[Job])
}

// 컨텍스트 상속 및 오버라이드
scope.launch(Dispatchers.IO + CoroutineName("ioTask")) {
    // Dispatchers.IO와 "ioTask" 이름 사용
    // Job은 부모에서 상속
}
```

---

## Channel

코루틴 간 통신을 위한 동시성 기본 요소입니다.

### 기본 사용

```kotlin
fun main() = runBlocking {
    val channel = Channel<Int>()

    launch {
        for (x in 1..5) {
            channel.send(x * x)
        }
        channel.close()  // 닫아서 완료 알림
    }

    // 채널에서 수신
    for (y in channel) {
        println(y)  // 1, 4, 9, 16, 25
    }
}
```

### 버퍼 채널

```kotlin
// 버퍼 없음 (기본): 송신자는 수신자가 받을 때까지 대기
val rendezvous = Channel<Int>()

// 버퍼 있음: 버퍼가 가득 차면 대기
val buffered = Channel<Int>(10)

// 무제한: 메모리가 허용하는 한 무제한
val unlimited = Channel<Int>(Channel.UNLIMITED)

// Conflated: 가장 최근 값만 유지
val conflated = Channel<Int>(Channel.CONFLATED)
```

### produce 빌더

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) {
        send(x++)
    }
}

fun main() = runBlocking {
    val numbers = produceNumbers()
    repeat(5) {
        println(numbers.receive())
    }
    numbers.cancel()  // 채널 취소
}
```

### Fan-out / Fan-in

```kotlin
// Fan-out: 여러 코루틴이 하나의 채널에서 수신
fun main() = runBlocking {
    val channel = produce<Int> {
        repeat(10) { send(it) }
    }

    repeat(3) { workerId ->
        launch {
            for (msg in channel) {
                println("Worker $workerId received $msg")
            }
        }
    }
}

// Fan-in: 여러 코루틴이 하나의 채널로 송신
fun main() = runBlocking {
    val channel = Channel<String>()

    launch { repeat(3) { channel.send("A$it") } }
    launch { repeat(3) { channel.send("B$it") } }

    repeat(6) {
        println(channel.receive())
    }
}
```

---

## 동시성 원시 요소

### Mutex

상호 배제를 보장합니다.

```kotlin
val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    massiveRun {
        mutex.withLock {
            counter++
        }
    }
    println("Counter = $counter")
}
```

### Semaphore

동시 접근 수를 제한합니다.

```kotlin
val semaphore = Semaphore(3)  // 최대 3개 동시 접근

suspend fun accessResource() {
    semaphore.withPermit {
        // 리소스 접근 (최대 3개 동시)
    }
}
```

### Actor (실험적)

메시지 기반 동시성 처리입니다.

```kotlin
sealed class CounterMsg
object IncCounter : CounterMsg()
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg()

fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0
    for (msg in channel) {
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}

fun main() = runBlocking {
    val counter = counterActor()
    repeat(1000) {
        counter.send(IncCounter)
    }
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close()
}
```

---

## Select 표현식 (실험적)

여러 일시 중단 함수 중 첫 번째로 완료되는 것을 선택합니다.

```kotlin
suspend fun selectFirstValue(
    channel1: ReceiveChannel<String>,
    channel2: ReceiveChannel<String>
): String = select {
    channel1.onReceive { it }
    channel2.onReceive { it }
}

// 타임아웃과 함께
suspend fun selectWithTimeout(): String? = select {
    onTimeout(1000L) { null }
    channel.onReceive { it }
}
```

---

## 테스트

### runTest

```kotlin
@Test
fun testMySuspendFunction() = runTest {
    val result = mySuspendFunction()
    assertEquals("expected", result)
}
```

### TestDispatcher

```kotlin
@Test
fun testWithAdvanceTime() = runTest {
    var result = 0

    launch {
        delay(1000L)
        result = 1
    }

    assertEquals(0, result)

    advanceTimeBy(1000L)  // 가상 시간 진행
    runCurrent()          // 대기 중인 작업 실행

    assertEquals(1, result)
}
```

---

## 참고 문서

- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Coroutines basics](https://kotlinlang.org/docs/coroutines-basics.html)
- [Cancellation and timeouts](https://kotlinlang.org/docs/cancellation-and-timeouts.html)
- [Channels](https://kotlinlang.org/docs/channels.html)
- [Exception handling](https://kotlinlang.org/docs/exception-handling.html)
