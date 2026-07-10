# 코루틴과 동시성

# 코루틴

> **원문:** https://kotlinlang.org/docs/coroutines-overview.html

애플리케이션은 종종 사용자 입력에 응답하거나, 데이터를 로드하거나, 화면을 업데이트하는 등 여러 작업을 동시에 수행해야 합니다. 이를 지원하기 위해 애플리케이션은 **동시성**(concurrency)에 의존하며, 이를 통해 작업들이 서로를 차단하지 않고 독립적으로 실행될 수 있습니다.

## 전통적인 스레딩 접근 방식

가장 일반적인 방법은 스레드를 사용하는 것입니다. 스레드는 OS가 관리하는 독립적인 실행 경로입니다. 그러나 스레드는 상대적으로 무겁고, 많이 생성하면 성능 문제가 발생할 수 있습니다.

## Kotlin의 해결책: 코루틴

Kotlin은 코루틴을 중심으로 구축된 비동기 프로그래밍을 제공합니다:

- 일시 중단 함수(suspending functions)를 사용하여 자연스럽고 순차적인 스타일로 비동기 코드 작성
- 스레드의 가벼운 대안
- 시스템 리소스를 차단하지 않고 일시 중단 가능
- 리소스 친화적이며 세밀한 동시성에 더 적합

**주요 라이브러리:** `kotlinx.coroutines` - 코루틴 시작, 동시성 처리, 비동기 스트림 작업 등을 위한 도구를 제공합니다.

**시작하기:** 일시 중단 함수, 코루틴 빌더, 구조적 동시성을 포함한 핵심 개념은 [코루틴 기초](coroutines-basics.md) 가이드에서 시작하세요.

---

## 코루틴 개념

### 1. 일시 중단 함수와 코루틴 빌더

**일시 중단 함수:**
- `suspend` 키워드를 기반으로 구축됨
- 스레드를 차단하지 않고 코드를 일시 중지하고 재개할 수 있음
- 장기 실행 작업을 비동기적으로 실행할 수 있음

**코루틴 빌더:**
- **`.launch()`** - 새로운 코루틴을 시작
- **`.async()`** - 새로운 코루틴을 시작하고 결과를 반환
- 둘 다 `CoroutineScope`의 확장 함수
- `CoroutineScope`는 코루틴의 생명주기를 정의하고 코루틴 컨텍스트를 제공

**리소스:** [코루틴 기초](coroutines-basics.md)와 [일시 중단 함수 합성](composing-suspending-functions.md)에서 자세히 알아보세요.

### 2. 코루틴 컨텍스트와 동작

`CoroutineScope`에서 코루틴을 시작하면 실행을 제어하는 컨텍스트가 생성됩니다. 빌더 함수는 코루틴 동작을 정의하는 요소들을 자동으로 생성합니다:

**주요 컨텍스트 요소:**

| 요소 | 목적 |
|------|------|
| **`Job`** | 코루틴의 생명주기를 추적; 구조적 동시성을 가능하게 함 |
| **`CoroutineDispatcher`** | 코루틴이 실행되는 위치를 제어 (백그라운드 스레드, 메인 스레드 등) |
| **`CoroutineExceptionHandler`** | 처리되지 않은 예외를 처리 |

**컨텍스트 계층:**
- 컨텍스트는 기본적으로 코루틴의 부모로부터 상속됨
- 구조적 동시성을 가능하게 하는 계층 구조를 형성
- 관련 코루틴들을 함께 취소하거나 예외를 그룹으로 처리할 수 있음
- 참고: [코루틴 컨텍스트와 디스패처](coroutine-context-and-dispatchers.md), [취소와 타임아웃](cancellation-and-timeouts.md), [예외 처리](exception-handling.md)

### 3. 비동기 Flow와 공유 가변 상태

**코루틴 간 통신 옵션:**

| 옵션 | 목적 |
|------|------|
| **`Flow`** | 코루틴이 적극적으로 수집할 때만 값을 생성 |
| **`Channel`** | 여러 코루틴이 값을 송수신할 수 있음; 각 값은 정확히 하나의 코루틴에 전달 |
| **`SharedFlow`** | 모든 활성 수집 코루틴에 모든 값을 지속적으로 공유 |

**공유 가변 상태 관리:**
- **문제:** 여러 코루틴이 동일한 데이터에 접근/업데이트하면 경쟁 조건이 발생할 수 있음
- **해결책:** `StateFlow`를 사용하여 공유 데이터를 래핑
- 하나의 코루틴에서 업데이트하고 다른 코루틴에서 최신 값을 수집

**리소스:** [비동기 Flow](flow.md), [채널](channels.md), [코루틴과 채널 튜토리얼](coroutines-and-channels.md)

---

## 다음 단계 - 학습 경로

1. **기초** - [코루틴 기초 가이드](coroutines-basics.md)
2. **고급 합성** - [일시 중단 함수 합성](composing-suspending-functions.md)
3. **디버깅** - IntelliJ IDEA로 코루틴 디버그
4. **Flow 디버깅** - IntelliJ IDEA로 Kotlin Flow 디버그
5. **UI 개발** - 코루틴을 사용한 UI 프로그래밍 가이드
6. **Android 모범 사례** - 코루틴 모범 사례
7. **API 참조** - `kotlinx.coroutines` API

---

## 관련 주제

- **이전:** [비동기 프로그래밍 기법](async-programming.md)
- **다음:** [리플렉션](reflection.md)

---

# 코루틴 기초

> **원문:** https://kotlinlang.org/docs/coroutines-basics.html

코루틴은 명확하고 순차적인 스타일로 동시성 코드를 작성하기 위한 Kotlin 기능입니다. 스레드를 차단하는 대신 실행을 일시 중단할 수 있어 효율적인 리소스 활용이 가능합니다.

## 주요 섹션

### 1. 일시 중단 함수

코루틴의 기본 구성 요소로 `suspend` 키워드로 선언됩니다:

```kotlin
suspend fun greet() {
    println("Hello world from a suspending function")
}

suspend fun main() {
    showUserInfo()
}
```

일시 중단 함수는 다른 일시 중단 함수만 호출할 수 있거나, 다른 일시 중단 함수에서만 호출될 수 있습니다.

### 2. kotlinx.coroutines 라이브러리 추가

**Gradle (Kotlin DSL):**
```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
```

**Gradle (Groovy):**
```gradle
repositories { mavenCentral() }
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>1.10.2</version>
</dependency>
```

### 3. 첫 번째 코루틴 생성

필요 요소:
- 일시 중단 함수
- 코루틴 스코프
- 코루틴 빌더 (예: `launch()`)
- 스레드를 제어하는 디스패처

**완전한 예제:**
```kotlin
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

suspend fun greet() {
    println("The greet() on the thread: ${Thread.currentThread().name}")
    delay(1.seconds)
}

suspend fun main() {
    withContext(Dispatchers.Default) {
        this.launch {
            greet()
        }
        this.launch {
            println("The CoroutineScope.launch() on the thread: ${Thread.currentThread().name}")
            delay(1.seconds)
        }
        println("The withContext() on the thread: ${Thread.currentThread().name}")
    }
}
```

### 4. 코루틴 스코프와 구조적 동시성

코루틴은 부모-자식 관계를 가진 트리 계층 구조를 형성합니다. 부모 코루틴은 자식이 완료될 때까지 기다린 후 종료됩니다.

**코루틴 스코프 생성:**
```kotlin
suspend fun main() {
    coroutineScope {
        this.launch {
            this.launch {
                delay(2.seconds)
                println("Child of the enclosing coroutine completed")
            }
            println("Child coroutine 1 completed")
        }
        this.launch {
            delay(1.seconds)
            println("Child coroutine 2 completed")
        }
    }
    println("Coroutine scope completed")
}
```

**코루틴 빌더 추출:**
```kotlin
suspend fun main() {
    coroutineScope {
        launchAll()
    }
}

fun CoroutineScope.launchAll() {
    this.launch { println("1") }
    this.launch { println("2") }
}
```

### 5. 코루틴 빌더 함수

#### `CoroutineScope.launch()`
스코프를 차단하지 않고 코루틴을 시작합니다:

```kotlin
suspend fun performBackgroundWork() = coroutineScope {
    this.launch {
        delay(100.milliseconds)
        println("Sending notification in background")
    }
    println("Scope continues")
}
```

#### `CoroutineScope.async()`
기다릴 수 있는 `Deferred` 결과를 반환합니다:

```kotlin
suspend fun main() = withContext(Dispatchers.Default) {
    val firstPage = this.async {
        delay(50.milliseconds)
        "First page"
    }
    val secondPage = this.async {
        delay(100.milliseconds)
        "Second page"
    }
    val pagesAreEqual = firstPage.await() == secondPage.await()
    println("Pages are equal: $pagesAreEqual")
}
```

#### `runBlocking()`
코루틴이 완료될 때까지 현재 스레드를 차단합니다 (신중하게 사용):

```kotlin
object MyRepository : Repository {
    override fun readItem(): Int {
        return runBlocking {
            myReadItem()
        }
    }
}

suspend fun myReadItem(): Int {
    delay(100.milliseconds)
    return 4
}
```

### 6. 코루틴 디스패처

어떤 스레드에서 코루틴을 실행할지 제어합니다:

```kotlin
suspend fun runWithDispatcher() = coroutineScope {
    this.launch(Dispatchers.Default) {
        println("Running on ${Thread.currentThread().name}")
    }
}

suspend fun main() = withContext(Dispatchers.Default) {
    val one = this.async {
        val sum = (1L..500_000L).sum()
        delay(200L)
        sum
    }
    val two = this.async {
        val sum = (500_001L..1_000_000L).sum()
        sum
    }
    println("Combined total: ${one.await() + two.await()}")
}
```

일반적인 디스패처:
- `Dispatchers.Default` - CPU 집약적 작업
- `Dispatchers.IO` - I/O 작업
- `Dispatchers.Main` - UI 작업

### 7. 코루틴 vs JVM 스레드

**주요 차이점:**
- **스레드:** OS가 관리, 리소스 집약적 (각각 ~메가바이트), 최대 수천 개
- **코루틴:** 경량 (각각 ~바이트), 다른 스레드에서 일시 중단하고 재개 가능, 수백만 개 가능

**예제: 50,000개의 코루틴**
```kotlin
suspend fun printPeriods() = coroutineScope {
    repeat(50_000) {
        this.launch {
            delay(5.seconds)
            print(".")
        }
    }
}
```

동등한 스레드 구현의 ~100GB와 비교하여 ~500MB만 사용합니다.

## 관련 주제
- 일시 중단 함수 합성
- 취소와 타임아웃
- 코루틴 컨텍스트와 디스패처
- 비동기 Flow

---

# 코루틴 컨텍스트와 디스패처

> **원문:** https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html

## 개요

코루틴은 항상 Kotlin 표준 라이브러리에 정의된 `CoroutineContext` 타입으로 표현되는 컨텍스트에서 실행됩니다. 코루틴 컨텍스트는 다양한 요소들의 집합이며, 주요 요소는 코루틴의 `Job`과 디스패처입니다.

---

## 디스패처와 스레드

코루틴 컨텍스트에는 해당 코루틴이 실행에 사용하는 스레드 또는 스레드들을 결정하는 코루틴 디스패처(`CoroutineDispatcher`)가 포함됩니다. 디스패처는 다음을 수행할 수 있습니다:
- 코루틴 실행을 특정 스레드로 제한
- 스레드 풀로 디스패치
- 제한 없이 실행하도록 허용

모든 코루틴 빌더(`launch`, `async`)는 디스패처를 명시적으로 지정하기 위한 선택적 `CoroutineContext` 매개변수를 받습니다.

### 예제: 다양한 디스패처

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    launch { // 부모로부터 컨텍스트 상속
        println("main runBlocking : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Unconfined) { // 제한 없음
        println("Unconfined : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Default) { // 기본 디스패처
        println("Default : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(newSingleThreadContext("MyOwnThread")) { // 전용 스레드
        println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
    }
}
```

**출력:**
```
Unconfined : I'm working in thread main
Default : I'm working in thread DefaultDispatcher-worker-1
newSingleThreadContext: I'm working in thread MyOwnThread
main runBlocking : I'm working in thread main
```

### 디스패처 유형

- **`Dispatchers.Unconfined`**: 호출자 스레드에서 시작하여 첫 번째 일시 중단까지만, 그 후 일시 중단 함수의 스레드에서 재개
- **`Dispatchers.Default`**: 공유 백그라운드 스레드 풀 사용 (지정하지 않을 때 기본값)
- **`newSingleThreadContext(name)`**: 전용 스레드 생성 (비용이 큰 리소스 - `close()`로 해제해야 함)

---

## Unconfined vs Confined 디스패처

`Dispatchers.Unconfined` 디스패처는 호출자 스레드에서 코루틴을 시작하지만 첫 번째 일시 중단 지점까지만입니다. 일시 중단 후에는 일시 중단 함수가 결정한 스레드에서 재개됩니다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    launch(Dispatchers.Unconfined) {
        println("Unconfined : I'm working in thread ${Thread.currentThread().name}")
        delay(500)
        println("Unconfined : After delay in thread ${Thread.currentThread().name}")
    }
    launch {
        println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
        delay(1000)
        println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
    }
}
```

**출력:**
```
Unconfined : I'm working in thread main
main runBlocking: I'm working in thread main
Unconfined : After delay in thread kotlinx.coroutines.DefaultExecutor
main runBlocking: After delay in thread main
```

---

## 코루틴과 스레드 디버깅

### IDEA로 디버깅

IntelliJ IDEA의 코루틴 디버거 (Kotlin 플러그인)는 디버깅을 단순화합니다. 기능:
- 각 코루틴의 상태 확인
- 로컬 및 캡처된 변수 보기
- 전체 코루틴 생성 스택과 호출 스택 보기
- Coroutines 탭에서 우클릭으로 전체 코루틴 덤프 가져오기

`kotlinx-coroutines-core` 버전 1.3.8 이상이 필요합니다.

### 로깅을 사용한 디버깅

`-Dkotlinx.coroutines.debug` JVM 옵션으로 실행:

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking<Unit> {
    val a = async { log("I'm computing a piece of the answer"); 6 }
    val b = async { log("I'm computing another piece of the answer"); 7 }
    log("The answer is ${a.await() * b.await()}")
}
```

**출력:**
```
[main @coroutine#2] I'm computing a piece of the answer
[main @coroutine#3] I'm computing another piece of the answer
[main @coroutine#1] The answer is 42
```

---

## 스레드 간 전환

디스패처 간 전환에는 `withContext()`를 사용합니다:

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() {
    newSingleThreadContext("Ctx1").use { ctx1 ->
        newSingleThreadContext("Ctx2").use { ctx2 ->
            runBlocking(ctx1) {
                log("Started in ctx1")
                withContext(ctx2) {
                    log("Working in ctx2")
                }
                log("Back to ctx1")
            }
        }
    }
}
```

**출력:**
```
[Ctx1 @coroutine#1] Started in ctx1
[Ctx2 @coroutine#1] Working in ctx2
[Ctx1 @coroutine#1] Back to ctx1
```

---

## 컨텍스트의 Job

`coroutineContext[Job]`을 통해 코루틴의 `Job`에 접근합니다:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    println("My job is ${coroutineContext[Job]}")
}
```

**출력 (디버그 모드):**
```
My job is "coroutine#1":BlockingCoroutine{Active}@6d311334
```

`isActive`는 `coroutineContext[Job]?.isActive == true`의 단축형입니다.

---

## 코루틴의 자식들

다른 코루틴의 스코프에서 코루틴이 시작되면:
- `CoroutineScope.coroutineContext`를 통해 컨텍스트를 상속
- Job이 부모 Job의 자식이 됨
- 부모가 취소되면 함께 취소됨

### 부모-자식 관계 재정의

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val request = launch {
        launch(Job()) { // 독립적인 Job
            println("job1: I run in my own Job and execute independently!")
            delay(1000)
            println("job1: I am not affected by cancellation of the request")
        }
        launch { // 부모 컨텍스트 상속
            delay(100)
            println("job2: I am a child of the request coroutine")
            delay(1000)
            println("job2: I will not execute this line if my parent request is cancelled")
        }
    }
    delay(500)
    request.cancel()
    println("main: Who has survived request cancellation?")
    delay(1000)
}
```

**출력:**
```
job1: I run in my own Job and execute independently!
job2: I am a child of the request coroutine
main: Who has survived request cancellation?
job1: I am not affected by cancellation of the request
```

---

## 부모의 책임

부모 코루틴은 항상 모든 자식들의 완료를 기다립니다:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val request = launch {
        repeat(3) { i ->
            launch {
                delay((i + 1) * 200L)
                println("Coroutine $i is done")
            }
        }
        println("request: I'm done and I don't explicitly join my children that are still active")
    }
    request.join()
    println("Now processing of the request is complete")
}
```

**출력:**
```
request: I'm done and I don't explicitly join my children that are still active
Coroutine 0 is done
Coroutine 1 is done
Coroutine 2 is done
Now processing of the request is complete
```

---

## 디버깅을 위한 코루틴 이름 지정

더 나은 디버깅을 위해 `CoroutineName` 컨텍스트 요소를 사용합니다:

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking(CoroutineName("main")) {
    log("Started main coroutine")
    val v1 = async(CoroutineName("v1coroutine")) {
        delay(500)
        log("Computing v1")
        6
    }
    val v2 = async(CoroutineName("v2coroutine")) {
        delay(1000)
        log("Computing v2")
        7
    }
    log("The answer for v1 * v2 = ${v1.await() * v2.await()}")
}
```

**출력 (`-Dkotlinx.coroutines.debug` 사용):**
```
[main @main#1] Started main coroutine
[main @v1coroutine#2] Computing v1
[main @v2coroutine#3] Computing v2
[main @main#1] The answer for v1 * v2 = 42
```

---

## 컨텍스트 요소 결합

컨텍스트 요소를 결합하려면 `+` 연산자를 사용합니다:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    launch(Dispatchers.Default + CoroutineName("test")) {
        println("I'm working in thread ${Thread.currentThread().name}")
    }
}
```

**출력 (`-Dkotlinx.coroutines.debug` 사용):**
```
I'm working in thread DefaultDispatcher-worker-1 @test#2
```

---

## 코루틴 스코프

`CoroutineScope`는 코루틴 생명주기를 관리하며, 생명주기가 있는 객체 (예: Android 액티비티)에 유용합니다:

```kotlin
import kotlinx.coroutines.*

class Activity {
    private val mainScope = CoroutineScope(Dispatchers.Default)

    fun destroy() {
        mainScope.cancel()
    }

    fun doSomething() {
        repeat(10) { i ->
            mainScope.launch {
                delay((i + 1) * 200L)
                println("Coroutine $i is done")
            }
        }
    }
}

fun main() = runBlocking<Unit> {
    val activity = Activity()
    activity.doSomething()
    println("Launched coroutines")
    delay(500L)
    println("Destroying activity!")
    activity.destroy()
    delay(1000)
}
```

**출력:**
```
Launched coroutines
Coroutine 0 is done
Coroutine 1 is done
Destroying activity!
```

### 스레드 로컬 데이터

코루틴에서 `ThreadLocal`을 사용하려면 `asContextElement()`를 사용합니다:

```kotlin
import kotlinx.coroutines.*

val threadLocal = ThreadLocal<String?>()

fun main() = runBlocking<Unit> {
    threadLocal.set("main")
    println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")

    val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
        println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        yield()
        println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    }
    job.join()
    println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
}
```

**출력 (디버그 사용):**
```
Pre-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
Launch start, current thread: Thread[DefaultDispatcher-worker-1 @coroutine#2,5,main], thread local value: 'launch'
After yield, current thread: Thread[DefaultDispatcher-worker-2 @coroutine#2,5,main], thread local value: 'launch'
Post-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
```

**주요 제한사항:**
- 스레드 로컬 변경 사항은 코루틴 호출자에게 전파되지 않음
- 업데이트된 값은 다음 일시 중단 시 손실됨
- 값을 업데이트하려면 `withContext()` 사용
- 고급 사용의 경우 `ThreadContextElement` 인터페이스 구현

---

# 취소와 타임아웃

> **원문:** https://kotlinlang.org/docs/cancellation-and-timeouts.html

## 개요

취소를 사용하면 코루틴이 완료되기 전에 중지할 수 있습니다. UI에서 사용자가 창을 닫거나 코루틴이 실행 중인 동안 다른 곳으로 이동하는 등 더 이상 필요하지 않은 작업을 중지합니다. 또한 리소스를 조기에 해제하고 코루틴이 폐기된 객체에 접근하는 것을 방지할 수 있습니다.

취소는 코루틴의 생명주기와 부모-자식 관계를 나타내는 [`Job`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/) 핸들을 통해 작동합니다.

---

## 코루틴 취소

`Job` 핸들에서 [`cancel()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/cancel.html) 함수가 호출되면 코루틴이 취소됩니다.

코루틴이 취소되면 다음에 취소를 확인할 때 [`CancellationException`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-cancellation-exception/)을 던집니다.

### 예제: 수동 취소

```kotlin
import kotlinx.coroutines.*
import kotlin.time.Duration

suspend fun main() {
    withContext(Dispatchers.Default) {
        val job1Started = CompletableDeferred<Unit>()
        val job1: Job = launch {
            println("The coroutine has started")
            job1Started.complete(Unit)
            try {
                delay(Duration.INFINITE)
            } catch (e: CancellationException) {
                println("The coroutine was canceled: $e")
                throw e
            }
            println("This line will never be executed")
        }

        job1Started.await()
        job1.cancel()

        val job2 = async {
            println("The second coroutine has started")
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                println("The second coroutine was canceled")
                throw e
            }
        }
        job2.cancel()
    }
    println("All coroutines have completed")
}
```

### 취소 전파

구조적 동시성은 코루틴을 취소하면 모든 자식도 취소되도록 보장합니다.

```kotlin
suspend fun main() {
    withContext(Dispatchers.Default) {
        val childrenLaunched = CompletableDeferred<Unit>()
        val parentJob = launch {
            launch {
                println("Child coroutine 1 has started running")
                try {
                    awaitCancellation()
                } finally {
                    println("Child coroutine 1 has been canceled")
                }
            }
            launch {
                println("Child coroutine 2 has started running")
                try {
                    awaitCancellation()
                } finally {
                    println("Child coroutine 2 has been canceled")
                }
            }
            childrenLaunched.complete(Unit)
        }

        childrenLaunched.await()
        parentJob.cancel()
    }
}
```

---

## 코루틴이 취소에 반응하게 만들기

Kotlin에서 코루틴 취소는 **협조적**입니다. 코루틴은 일시 중단하거나 명시적으로 취소를 확인할 때만 취소에 반응합니다.

### 일시 중단 지점과 취소

코루틴이 취소되면 일시 중단 지점에 도달할 때까지 계속 실행됩니다. 코루틴이 거기서 일시 중단하면 함수는 취소되었는지 확인하고 `CancellationException`을 던집니다.

```kotlin
suspend fun main() {
    withContext(Dispatchers.Default) {
        val childJobs = listOf(
            launch { awaitCancellation() },
            launch { delay(Duration.INFINITE) },
            launch {
                val channel = Channel<Int>()
                channel.receive()
            },
            launch {
                val deferred = CompletableDeferred<Int>()
                deferred.await()
            },
            launch {
                val mutex = Mutex(locked = true)
                mutex.lock()
            }
        )

        delay(100.milliseconds)
        childJobs.forEach { it.cancel() }
    }
    println("All child jobs completed!")
}
```

### 명시적으로 취소 확인

코루틴이 오랫동안 일시 중단하지 않으면 다음을 사용합니다:

#### **isActive**
[`isActive`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/is-active.html) 프로퍼티는 코루틴이 취소되면 `false`입니다.

```kotlin
suspend fun main() {
    withContext(Dispatchers.Default) {
        val unsortedList = MutableList(10) { Random.nextInt() }
        val listSortingJob = launch {
            var i = 0
            while (isActive) {
                unsortedList.sort()
                ++i
            }
            println("Stopped sorting the list after $i iterations")
        }

        delay(100.milliseconds)
        listSortingJob.cancel()
        listSortingJob.join()
        println("The list is probably sorted: $unsortedList")
    }
}
```

#### **ensureActive()**
[`ensureActive()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html) 함수는 코루틴이 취소되면 즉시 `CancellationException`을 던집니다.

```kotlin
suspend fun main() {
    withContext(Dispatchers.Default) {
        val childJob = launch {
            var start = 0
            try {
                while (true) {
                    ++start
                    var n = start
                    while (n != 1) {
                        ensureActive()
                        n = if (n % 2 == 0) n / 2 else 3 * n + 1
                    }
                }
            } finally {
                println("Checked the Collatz conjecture for 0..${start-1}")
            }
        }

        delay(100.milliseconds)
        childJob.cancel()
    }
}
```

#### **yield()**
[`yield()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/yield.html) 함수는 코루틴을 일시 중단하고 재개하기 전에 취소를 확인합니다.

```kotlin
fun main() {
    runBlocking {
        val coroutineCount = 5
        repeat(coroutineCount) { coroutineIndex ->
            launch {
                val id = coroutineIndex + 1
                repeat(5) { iterationIndex ->
                    val iteration = iterationIndex + 1
                    yield()
                    println("$id * $iteration = ${id * iteration}")
                }
            }
        }
    }
}
```

### 블로킹 코드 인터럽트

JVM에서 코루틴을 취소할 때 블로킹 함수를 인터럽트하려면 [`runInterruptible()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-interruptible.html)을 사용합니다:

```kotlin
suspend fun main() {
    withContext(Dispatchers.Default) {
        val childStarted = CompletableDeferred<Unit>()
        val childJob = launch {
            try {
                runInterruptible {
                    childStarted.complete(Unit)
                    try {
                        Thread.sleep(Long.MAX_VALUE)
                    } catch (e: InterruptedException) {
                        println("Thread interrupted (Java): $e")
                        throw e
                    }
                }
            } catch (e: CancellationException) {
                println("Coroutine canceled (Kotlin): $e")
                throw e
            }
        }
        childStarted.await()
        childJob.cancel()
    }
}
```

---

## 코루틴 취소 시 값 안전하게 처리

일시 중단된 코루틴이 취소되면 값이 사용 가능하더라도 값을 반환하는 대신 `CancellationException`으로 재개됩니다. 이를 **즉시 취소**(prompt cancellation)라고 합니다.

### 예제: 적절한 리소스 처리

```kotlin
class ScreenWithFileContents(private val scope: CoroutineScope) {
    fun displayFile(path: Path) {
        scope.launch {
            var reader: BufferedReader? = null
            try {
                withContext(Dispatchers.IO) {
                    reader = Files.newBufferedReader(
                        path,
                        Charset.forName("US-ASCII")
                    )
                }
                updateUi(reader!!)
            } finally {
                reader?.close()
            }
        }
    }

    private suspend fun updateUi(reader: BufferedReader) {
        while (true) {
            val line = withContext(Dispatchers.IO) {
                reader.readLine()
            }
            if (line == null) break
            addOneLineToUi(line)
        }
    }

    private fun addOneLineToUi(line: String) {}

    fun leaveScreen() {
        scope.cancel()
    }
}
```

### 취소 불가능한 블록 실행

코루틴이 취소되어도 특정 작업이 완료되도록 하려면 `withContext()`와 함께 [`NonCancellable`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-non-cancellable/)을 사용합니다:

```kotlin
val serviceStarted = CompletableDeferred<Unit>()

fun startService() {
    println("Starting the service...")
    serviceStarted.complete(Unit)
}

suspend fun shutdownServiceAndWait() {
    println("Shutting down...")
    delay(100.milliseconds)
    println("Successfully shut down!")
}

suspend fun main() {
    withContext(Dispatchers.Default) {
        val childJob = launch {
            startService()
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    shutdownServiceAndWait()
                }
            }
        }
        serviceStarted.await()
        childJob.cancel()
    }
    println("Exiting the program")
}
```

---

## 타임아웃

타임아웃은 지정된 기간 후에 코루틴을 자동으로 취소합니다. [`withTimeoutOrNull()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout-or-null.html)을 사용합니다:

```kotlin
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

suspend fun slowOperation(): Int {
    try {
        delay(300.milliseconds)
        return 5
    } catch (e: CancellationException) {
        println("The slow operation has been canceled: $e")
        throw e
    }
}

suspend fun fastOperation(): Int {
    try {
        delay(15.milliseconds)
        return 14
    } catch (e: CancellationException) {
        println("The fast operation has been canceled: $e")
        throw e
    }
}

suspend fun main() {
    withContext(Dispatchers.Default) {
        val slow = withTimeoutOrNull(100.milliseconds) {
            slowOperation()
        }
        println("The slow operation finished with $slow")

        val fast = withTimeoutOrNull(100.milliseconds) {
            fastOperation()
        }
        println("The fast operation finished with $fast")
    }
}
```

타임아웃이 지정된 `Duration`을 초과하면 `withTimeoutOrNull()`은 `null`을 반환합니다.

---

# 일시 중단 함수 합성

> **원문:** https://kotlinlang.org/docs/composing-suspending-functions.html

이 섹션에서는 Kotlin 코루틴에서 일시 중단 함수를 합성하는 다양한 접근 방식을 다룹니다.

---

## 1. 기본적으로 순차적

### 개념
기본적으로 코루틴의 코드는 일반 코드처럼 순차적으로 실행됩니다. 함수들은 차례대로 호출됩니다.

### 예제 함수
```kotlin
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L)
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L)
    return 29
}
```

### 순차적 실행 예제
```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = doSomethingUsefulOne()
        val two = doSomethingUsefulTwo()
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")
}
```

**출력:**
```
The answer is 42
Completed in 2017 ms
```

**참고:** 함수들이 순차적으로 실행되기 때문에 (1초 + 1초) ~2초가 걸립니다.

---

## 2. async를 사용한 동시 실행

### 개념
함수들 간에 의존성이 없을 때, 더 빠른 실행을 위해 `async`를 사용하여 동시에 실행합니다.

### 주요 차이점: async vs launch
- **`launch`**: `Job`을 반환, 결과 값 없음
- **`async`**: `Deferred<T>`를 반환, 가벼운 논블로킹 퓨처
- 지연된 값의 결과를 얻으려면 `.await()`를 사용

### 동시 실행 예제
```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
}
```

**출력:**
```
The answer is 42
Completed in 1017 ms
```

**참고:** 두 코루틴이 동시에 실행되므로 ~1초가 걸립니다.

---

## 3. 지연 시작 async

### 개념
필요할 때만 코루틴을 시작하려면 `CoroutineStart.LAZY`를 사용합니다.

### 예제
```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }

        one.start()
        two.start()
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
}
```

**출력:**
```
The answer is 42
Completed in 1017 ms
```

### 중요 참고사항
- 코루틴은 정의되지만 명시적으로 시작할 때까지 실행되지 않음
- `.start()` 없이 `.await()`만 호출하면 실행이 순차적이 됨
- 사용 사례: 계산에 일시 중단 함수가 포함될 때 표준 `lazy` 함수의 대체

---

## 4. Async 스타일 함수

### 개념
비동기 호출을 위해 `GlobalScope`를 사용하여 async 스타일 함수를 정의합니다.

**경고:** 이 스타일은 강력히 권장되지 않습니다; 설명 목적으로만 표시됩니다.

### 예제
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

@OptIn(DelicateCoroutinesApi::class)
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}

fun main() {
    val time = measureTimeMillis {
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()

        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")
}
```

### GlobalScope의 문제점
- 작업이 중단되어도 코루틴이 백그라운드에서 계속 실행됨
- 예외 처리가 어려워짐
- 구조적 동시성의 이점이 없음

---

## 5. async를 사용한 구조적 동시성

### 개념
async 작업과 함께 구조적 동시성을 유지하려면 `coroutineScope`를 사용합니다.

### 예제
```kotlin
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}

import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    val time = measureTimeMillis {
        println("The answer is ${concurrentSum()}")
    }
    println("Completed in $time ms")
}
```

**출력:**
```
The answer is 42
Completed in 1017 ms
```

### 장점
- 예외가 발생하면 모든 코루틴이 취소됨
- 적절한 오류 처리와 리소스 정리
- 구조적 동시성 원칙 유지

### 취소 전파 예제
```kotlin
suspend fun failedConcurrentSum(): Int = coroutineScope {
    val one = async<Int> {
        try {
            delay(Long.MAX_VALUE)
            42
        } finally {
            println("First child was cancelled")
        }
    }
    val two = async<Int> {
        println("Second child throws an exception")
        throw ArithmeticException()
    }
    one.await() + two.await()
}

fun main() = runBlocking<Unit> {
    try {
        failedConcurrentSum()
    } catch(e: ArithmeticException) {
        println("Computation failed with ArithmeticException")
    }
}
```

**출력:**
```
Second child throws an exception
First child was cancelled
Computation failed with ArithmeticException
```

---

## 요약

| 접근 방식 | 사용 사례 | 실행 시간 | 장점 |
|----------|----------|----------|------|
| **순차적** | 의존적인 작업 | 가장 길음 | 단순하고 직관적 |
| **동시적 (async)** | 독립적인 작업 | 가장 빠름 | 병렬 실행 |
| **지연 async** | 지연된 실행 | 상황에 따라 다름 | 타이밍 제어 |
| **Async 스타일** | 레거시 코드 | 다양함 | 권장하지 않음 |
| **구조적 async** | 프로덕션 코드 | 빠름 | 적절한 오류 처리 |

---

# 비동기 프로그래밍 기법

> **원문:** https://kotlinlang.org/docs/async-programming.html

## 개요

이 문서는 데스크톱, 모바일 및 서버 측 애플리케이션을 포함하여 애플리케이션의 차단을 방지하기 위한 다양한 접근 방식을 다룹니다.

## 비동기 프로그래밍의 주요 접근 방식

### 1. 스레딩
**정의:** 메인 스레드 차단을 피하기 위해 별도의 스레드를 사용합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    val token = preparePost()
    val post = submitPost(token, item)
    processPost(post)
}

fun preparePost(): Token {
    // 요청을 만들고 결과적으로 메인 스레드를 차단
    return token
}
```

**단점:**
- 스레드는 비용이 큼 (컨텍스트 전환 비용이 높음)
- 스레드 수가 제한됨 (OS 의존적 병목)
- 모든 플랫폼에서 사용 불가 (예: JavaScript)
- 디버깅이 어렵고 경쟁 조건에 취약함

### 2. 콜백
**정의:** 완료 후 호출될 함수를 매개변수로 전달합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    preparePostAsync { token ->
        submitPostAsync(token, item) { post ->
            processPost(post)
        }
    }
}

fun preparePostAsync(callback: (Token) -> Unit) {
    // 요청을 만들고 즉시 반환
    // 나중에 콜백이 호출되도록 준비
}
```

**단점:**
- 콜백 지옥 / 피라미드 오브 둠 (깊게 중첩된 콜백)
- 복잡한 오류 처리

### 3. Futures, Promises 등
**정의:** 호출은 어느 시점에 해결될 `Promise` 객체를 반환합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    preparePostAsync()
        .thenCompose { token ->
            submitPostAsync(token, item)
        }
        .thenAccept { post ->
            processPost(post)
        }
}

fun preparePostAsync(): Promise<Token> {
    // 요청을 만들고 나중에 완료될 promise를 반환
    return promise
}
```

**단점:**
- 다른 프로그래밍 모델 (합성적 vs 명령형)
- 새로운 API를 배워야 함 (`thenCompose`, `thenAccept`)
- 반환 타입이 실제 데이터에서 `Promise`로 변경됨
- 오류 처리가 복잡할 수 있음

### 4. Reactive Extensions (Rx)
**정의:** Erik Meijer가 C#용으로 도입하고 Netflix의 RxJava로 인기를 얻음. 관찰 가능한 스트림을 사용하여 데이터를 처리합니다.

**핵심 개념:** "모든 것이 스트림이고, 관찰 가능하다"

**특징:**
- 개별 요소가 아닌 스트림을 반환
- 플랫폼 전반에 걸쳐 일관된 API (C#, Java, JavaScript 등)
- Futures보다 나은 오류 처리
- 프로그래밍 모델의 상당한 변화 필요

### 5. 코루틴 (Kotlin의 접근 방식)
**정의:** 함수가 실행을 일시 중단하고 나중에 재개할 수 있는 일시 중단 가능한 계산.

**예제:**
```kotlin
fun postItem(item: Item) {
    launch {
        val token = preparePost()
        val post = submitPost(token, item)
        processPost(post)
    }
}

suspend fun preparePost(): Token {
    // 요청을 만들고 코루틴을 일시 중단
    return suspendCoroutine { /* ... */ }
}
```

**장점:**
- 논블로킹임에도 불구하고 코드가 동기적으로 보임 (위에서 아래로)
- 함수 시그니처가 동일하게 유지됨 (`suspend` 키워드만 추가)
- 표준 프로그래밍 구문 사용 (루프, 예외 처리)
- 플랫폼 독립적 (JVM, JavaScript 등)
- 대부분의 기능이 라이브러리에 위임됨 (언어에는 `suspend` 키워드만)
- 구현을 플랫폼 전반에 걸쳐 컴파일러가 처리함

**핵심 이점:** 논블로킹 코드를 작성하는 것이 본질적으로 블로킹 코드를 작성하는 것과 같습니다.

## 역사적 맥락

코루틴은 새로운 것이 아니며 수십 년 동안 존재해왔고 Go와 같은 언어에서 인기가 있습니다. Kotlin의 구현은 C#과 달리 `async`와 `await` 같은 언어 키워드를 피하고 라이브러리 함수를 선호합니다.

## 관련 문서
- [코루틴 참조](coroutines-overview.md)
- [제네릭: in, out, where](generics.md)

---

# 비동기 Flow

> **원문:** https://kotlinlang.org/docs/flow.html

## 개요

일시 중단 함수는 단일 값을 비동기적으로 반환하지만, Kotlin Flow는 여러 개의 비동기적으로 계산된 값을 처리합니다. 이 포괄적인 가이드는 Flow 기본 사항, 연산자 및 패턴을 다룹니다.

## 여러 값 표현하기

### 컬렉션 (동기적)
```kotlin
fun simple(): List<Int> = listOf(1, 2, 3)

fun main() {
    simple().forEach { value -> println(value) }
}
```

### 시퀀스 (블로킹)
```kotlin
fun simple(): Sequence<Int> = sequence {
    for (i in 1..3) {
        Thread.sleep(100)
        yield(i)
    }
}
```

### 일시 중단 함수
```kotlin
suspend fun simple(): List<Int> {
    delay(1000)
    return listOf(1, 2, 3)
}

fun main() = runBlocking<Unit> {
    simple().forEach { value -> println(value) }
}
```

### Flow (비동기 스트림)
```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    launch {
        for (k in 1..3) {
            println("I'm not blocked $k")
            delay(100)
        }
    }
    simple().collect { value -> println(value) }
}
```

## 주요 Flow 특성

- **콜드 스트림**: `flow { ... }` 내부의 코드는 수집될 때까지 실행되지 않음
- **논블로킹**: 메인 스레드를 차단하지 않음
- **일시 중단 가능**: 빌더 내에서 코드가 일시 중단될 수 있음
- **방출**: `emit()`을 사용하여 값을 전송
- **수집**: `collect()`를 사용하여 값을 수신

## Flow는 콜드하다

```kotlin
fun simple(): Flow<Int> = flow {
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    println("Calling simple function...")
    val flow = simple()
    println("Calling collect...")
    flow.collect { value -> println(value) }
    println("Calling collect again...")
    flow.collect { value -> println(value) }
}
```
출력은 `collect()`가 호출될 때마다 "Flow started"가 나타남을 보여줍니다 - flow는 각 수집 시 다시 시작됩니다.

## Flow 취소 기본

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    withTimeoutOrNull(250) {
        simple().collect { value -> println(value) }
    }
    println("Done")
}
```

## Flow 빌더

```kotlin
// flowOf - 고정된 값 방출
flowOf(1, 2, 3).collect { println(it) }

// asFlow - 컬렉션/시퀀스 변환
(1..3).asFlow().collect { println(it) }
```

## 중간 Flow 연산자

### Map과 Filter
```kotlin
suspend fun performRequest(request: Int): String {
    delay(1000)
    return "response $request"
}

fun main() = runBlocking<Unit> {
    (1..3).asFlow()
        .map { request -> performRequest(request) }
        .collect { response -> println(response) }
}
```

### Transform 연산자
```kotlin
(1..3).asFlow()
    .transform { request ->
        emit("Making request $request")
        emit(performRequest(request))
    }
    .collect { response -> println(response) }
```

### 크기 제한 연산자
```kotlin
fun numbers(): Flow<Int> = flow {
    try {
        emit(1)
        emit(2)
        println("This line will not execute")
        emit(3)
    } finally {
        println("Finally in numbers")
    }
}

fun main() = runBlocking<Unit> {
    numbers()
        .take(2)
        .collect { value -> println(value) }
}
```

## 터미널 Flow 연산자

수집을 시작하는 일시 중단 함수들:

```kotlin
// toList / toSet - 컬렉션으로 변환
val list = (1..3).asFlow().toList()

// first / single - 첫 번째 또는 유일한 값 가져오기
val first = (1..3).asFlow().first()

// reduce / fold - 값 집계
val sum = (1..5).asFlow()
    .map { it * it }
    .reduce { a, b -> a + b }
println(sum) // 55
```

## Flow는 순차적이다

```kotlin
(1..5).asFlow()
    .filter { println("Filter $it"); it % 2 == 0 }
    .map { println("Map $it"); "string $it" }
    .collect { println("Collect $it") }
```
각 값은 다음 값이 들어오기 전에 모든 연산자를 순차적으로 통과합니다.

## Flow 컨텍스트

Flow는 호출 코루틴의 컨텍스트를 보존합니다:

```kotlin
fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun simple(): Flow<Int> = flow {
    log("Started simple flow")
    for (i in 1..3) {
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    simple().collect { value -> log("Collected $value") }
}
```

### withContext의 일반적인 실수

**잘못된 방법** - 컨텍스트 보존 위반:
```kotlin
fun simple(): Flow<Int> = flow {
    withContext(Dispatchers.Default) {
        for (i in 1..3) {
            Thread.sleep(100)
            emit(i)
        }
    }
}
// 예외 발생: Flow invariant is violated
```

**올바른 방법** - `flowOn` 연산자 사용:
```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        Thread.sleep(100)
        log("Emitting $i")
        emit(i)
    }
}.flowOn(Dispatchers.Default)
```

## 버퍼링

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    val time = measureTimeMillis {
        simple()
            .buffer() // 방출과 수집을 동시에 실행
            .collect { value ->
                delay(300)
                println(value)
            }
    }
    println("Collected in $time ms") // ~1200ms 대신 ~1000ms
}
```

## 합류 (Conflation)

수집자가 느릴 때 중간 값을 건너뜁니다:

```kotlin
simple()
    .conflate() // 최신 값만 처리
    .collect { value ->
        delay(300)
        println(value)
    }
```

## 최신 값 처리

새로운 방출 시 취소하고 다시 시작:

```kotlin
simple()
    .collectLatest { value ->
        println("Collecting $value")
        delay(300)
        println("Done $value")
    }
```

## 여러 Flow 합성

### Zip
두 flow의 해당 값을 결합합니다:
```kotlin
val nums = (1..3).asFlow()
val strs = flowOf("one", "two", "three")
nums.zip(strs) { a, b -> "$a -> $b" }
    .collect { println(it) }
// 출력: 1 -> one, 2 -> two, 3 -> three
```

### Combine
어떤 upstream flow가 방출하든 방출합니다:
```kotlin
val nums = (1..3).asFlow().onEach { delay(300) }
val strs = flowOf("one", "two", "three").onEach { delay(400) }
nums.combine(strs) { a, b -> "$a -> $b" }
    .collect { println(it) }
// 각 flow 방출 시 최신 값으로 방출
```

## Flow 평탄화

### flatMapConcat
순차적 처리 - 내부 flow 완료를 기다림:
```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First")
    delay(500)
    emit("$i: Second")
}

(1..3).asFlow()
    .flatMapConcat { requestFlow(it) }
    .collect { println(it) }
```

### flatMapMerge
동시 처리 - 모든 flow 병합:
```kotlin
(1..3).asFlow()
    .flatMapMerge { requestFlow(it) }
    .collect { println(it) }
```

### flatMapLatest
새 방출 시 이전 수집을 취소:
```kotlin
(1..3).asFlow()
    .flatMapLatest { requestFlow(it) }
    .collect { println(it) }
```

## Flow 예외

### 수집자 Try-Catch
```kotlin
try {
    simple().collect { value ->
        println(value)
        check(value <= 1) { "Collected $value" }
    }
} catch (e: Throwable) {
    println("Caught $e")
}
```

### 모든 것이 잡힌다
방출자와 모든 연산자의 예외는 수집자가 잡습니다.

### 예외 투명성

Flow는 예외에 대해 투명해야 합니다. `catch` 연산자를 사용하세요:

```kotlin
simple()
    .map { value ->
        check(value <= 1) { "Crashed on $value" }
        "string $value"
    }
    .catch { e -> emit("Caught $e") }
    .collect { value -> println(value) }
```

### 투명한 Catch
`catch`는 upstream 예외만 잡고 downstream은 잡지 않습니다:
```kotlin
simple()
    .catch { e -> println("Caught $e") }
    .collect { value ->
        check(value <= 1) { "Collected $value" }
        println(value)
    }
// collect의 예외는 여전히 빠져나감
```

### 선언적으로 잡기
수집자 로직을 `catch` 전에 `onEach`로 이동:
```kotlin
simple()
    .onEach { value ->
        check(value <= 1) { "Collected $value" }
        println(value)
    }
    .catch { e -> println("Caught $e") }
    .collect()
```

## Flow 완료

### 명령형 Finally 블록
```kotlin
try {
    simple().collect { value -> println(value) }
} finally {
    println("Done")
}
```

### onCompletion을 사용한 선언적 처리
```kotlin
simple()
    .onCompletion { println("Done") }
    .collect { value -> println(value) }
```

### 완료 상태 확인
```kotlin
simple()
    .onCompletion { cause ->
        if (cause != null)
            println("Flow completed exceptionally")
        else
            println("Flow completed successfully")
    }
    .catch { cause -> println("Caught exception") }
    .collect { value -> println(value) }
```

## Flow 시작

### collect() 사용
```kotlin
events()
    .onEach { event -> println("Event: $event") }
    .collect() // 완료될 때까지 대기
println("Done")
```

### launchIn() 사용
```kotlin
events()
    .onEach { event -> println("Event: $event") }
    .launchIn(this) // 별도 코루틴에서 시작
println("Done") // 즉시 계속
```

## Flow 취소 확인

### flow 빌더는 자동 확인 포함
```kotlin
fun foo(): Flow<Int> = flow {
    for (i in 1..5) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    foo().collect { value ->
        if (value == 3) cancel()
        println(value)
    }
}
// 1, 2, 3만 방출; 그 후 취소 예외
```

### asFlow()를 취소 가능하게 만들기
```kotlin
(1..5).asFlow()
    .cancellable() // 취소 확인 추가
    .collect { value ->
        if (value == 3) cancel()
        println(value)
    }
```

## Flow와 Reactive Streams

Flow 설계는 Reactive Streams에서 영감을 받았지만 Kotlin에 최적화되었습니다:
- 더 단순한 설계
- 일시 중단 친화적 API
- 구조적 동시성 지원

`kotlinx.coroutines`에서 변환 유틸리티 사용 가능:
- `kotlinx-coroutines-reactive` (Reactive Streams)
- `kotlinx-coroutines-reactor` (Project Reactor)
- `kotlinx-coroutines-rx2`/`kotlinx-coroutines-rx3` (RxJava)

---

**관련 주제:**
- [코루틴 컨텍스트와 디스패처](coroutine-context-and-dispatchers.md)
- [채널](channels.md)

---

# Kotlin 채널

> **원문:** https://kotlinlang.org/docs/channels.html

## 개요

채널은 코루틴 간에 값의 스트림을 전송하는 방법을 제공하며, 단일 값을 전송하는 Deferred 값을 보완합니다.

---

## 채널 기본

**채널**은 개념적으로 `BlockingQueue`와 유사하지만, 블로킹 작업 대신 일시 중단 함수를 사용합니다:
- `send()` - 블로킹 `put`의 일시 중단 대안
- `receive()` - 블로킹 `take`의 일시 중단 대안

### 예제:
```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) channel.send(x * x)
    }
    repeat(5) { println(channel.receive()) }
    println("Done!")
}
```

**출력:**
```
1
4
9
16
25
Done!
```

---

## 채널 닫기와 반복

채널은 더 이상 요소가 오지 않음을 나타내기 위해 닫을 수 있습니다. 일반 `for` 루프를 사용하여 요소를 수신합니다:

```kotlin
val channel = Channel<Int>()
launch {
    for (x in 1..5) channel.send(x * x)
    channel.close() // 전송 완료
}
for (y in channel) println(y) // 닫힐 때까지 반복
println("Done!")
```

---

## 채널 프로듀서 구축

프로듀서를 깔끔하게 생성하려면 `produce` 코루틴 빌더를 사용합니다:

```kotlin
fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) send(x * x)
}

fun main() = runBlocking {
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
}
```

---

## 파이프라인

파이프라인은 한 코루틴이 값을 생산하고 다른 코루틴이 처리하는 체인을 형성합니다:

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++)
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}

fun main() = runBlocking {
    val numbers = produceNumbers()
    val squares = square(numbers)
    repeat(5) { println(squares.receive()) }
    println("Done!")
    coroutineContext.cancelChildren()
}
```

---

## 파이프라인을 사용한 소수

필터를 사용한 실용적인 파이프라인 예제:

```kotlin
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++)
}

fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}

fun main() = runBlocking {
    var cur = numbersFrom(2)
    repeat(10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren()
}
```

**출력:**
```
2 3 5 7 11 13 17 19 23 29
```

**참고:** stdlib의 `iterator`를 사용할 수도 있지만, 채널은 `Dispatchers.Default`로 여러 CPU 코어를 지원합니다.

---

## 팬아웃 (Fan-Out)

여러 코루틴이 동일한 채널에서 수신하여 작업을 분배합니다:

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) {
        send(x++)
        delay(100)
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}

fun main() = runBlocking<Unit> {
    val producer = produceNumbers()
    repeat(5) { launchProcessor(it, producer) }
    delay(950)
    producer.cancel()
}
```

**출력 (샘플):**
```
Processor #2 received 1
Processor #4 received 2
Processor #0 received 3
...
```

---

## 팬인 (Fan-In)

여러 코루틴이 동일한 채널로 전송합니다:

```kotlin
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}

fun main() = runBlocking {
    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "BAR!", 500L) }
    repeat(6) { println(channel.receive()) }
    coroutineContext.cancelChildren()
}
```

**출력:**
```
foo
foo
BAR!
foo
foo
BAR!
```

---

## 버퍼가 있는 채널

채널은 일시 중단 전에 여러 전송을 허용하는 버퍼를 가질 수 있습니다:

```kotlin
val channel = Channel<Int>(4) // 용량 4
val sender = launch {
    repeat(10) {
        println("Sending $it")
        channel.send(it)
    }
}
delay(1000)
sender.cancel()
```

**출력:**
```
Sending 0
Sending 1
Sending 2
Sending 3
Sending 4
```

버퍼가 가득 차면 (4개 요소 후) 송신자가 일시 중단됩니다.

---

## 채널은 공정하다

작업은 여러 코루틴에 걸쳐 FIFO 순서로 제공됩니다:

```kotlin
data class Ball(var hits: Int)

fun main() = runBlocking {
    val table = Channel<Ball>()
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0))
    delay(1000)
    coroutineContext.cancelChildren()
}

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) {
        ball.hits++
        println("$name $ball")
        delay(300)
        table.send(ball)
    }
}
```

**출력:**
```
ping Ball(hits=1)
pong Ball(hits=2)
ping Ball(hits=3)
pong Ball(hits=4)
```

---

## 티커 채널

시간 기반 파이프라인을 위해 고정된 간격으로 `Unit`을 생산합니다:

```kotlin
fun main() = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 200, initialDelayMillis = 0)

    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement")

    nextElement = withTimeoutOrNull(100) { tickerChannel.receive() }
    println("Next element is not ready in 100 ms: $nextElement")

    nextElement = withTimeoutOrNull(120) { tickerChannel.receive() }
    println("Next element is ready in 200 ms: $nextElement")

    delay(300)
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")

    tickerChannel.cancel()
}
```

**출력:**
```
Initial element is available immediately: kotlin.Unit
Next element is not ready in 100 ms: null
Next element is ready in 200 ms: kotlin.Unit
Next element is available immediately after large consumer delay: kotlin.Unit
```

**참고:** 티커는 기본적으로 소비자 일시 정지에 맞춰 지연을 조정합니다. 고정된 요소 간 지연을 위해 `TickerMode.FIXED_DELAY`를 사용하세요.

---

# 코루틴과 채널 튜토리얼

> **원문:** https://kotlinlang.org/docs/coroutines-and-channels.html

## 개요

이것은 스레드를 차단하거나 콜백을 사용하지 않고 네트워크 요청을 수행하기 위해 코루틴과 채널을 사용하는 포괄적인 Kotlin 튜토리얼입니다.

**전제 조건:** 기본 Kotlin 구문 지식

**학습 목표:**
- 네트워크 요청에 일시 중단 함수 사용
- 코루틴을 사용하여 요청을 동시에 전송
- 채널을 사용하여 코루틴 간 정보 공유

---

## 섹션 1: 시작하기 전에

### 설정 요구 사항
1. 최신 버전의 IntelliJ IDEA 다운로드
2. 프로젝트 템플릿 클론:
```bash
git clone https://github.com/kotlin-hands-on/intro-coroutines
```

### GitHub 개발자 토큰 생성
1. https://github.com/settings/tokens/new 로 이동
2. "coroutines-tutorial" 이름의 토큰 생성
3. 어떤 스코프도 선택하지 않음
4. 생성된 토큰 복사

### 초기 프로그램 실행
1. `src/contributors/main.kt` 열기
2. GitHub 사용자 이름과 토큰 제공
3. BLOCKING 변형 선택
4. "Load contributors" 클릭
5. 출력 콘솔에서 데이터가 로드되는지 확인

---

## 섹션 2: 블로킹 요청

### GitHubService 인터페이스
```kotlin
interface GitHubService {
    @GET("orgs/{org}/repos?per_page=100")
    fun getOrgReposCall(
        @Path("org") org: String
    ): Call<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    fun getRepoContributorsCall(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<List<User>>
}
```

### loadContributorsBlocking() 구현
```kotlin
fun loadContributorsBlocking(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service
        .getOrgReposCall(req.org)      // #1
        .execute()                      // #2 - 블로킹 호출
        .also { logRepos(req, it) }     // #3
        .body() ?: emptyList()          // #4

    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            .execute()
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}
```

**핵심 포인트:**
- `execute()`는 동기적이며 스레드를 차단
- 모든 요청이 메인 UI 스레드에서 순차적으로 실행
- 로딩이 완료되는 동안 UI가 멈춤
- 로그 출력은 모든 연산이 동일 스레드에서 수행됨을 보여줌 (AWT-EventQueue-0)

### 확장 함수
```kotlin
fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: emptyList()
}
```

### 태스크 1: 집계 구현
**목표:** 중복 사용자를 결합하고 기여도 집계

**해결책:**
```kotlin
fun List<User>.aggregate(): List<User> =
    groupBy { it.login }
        .map { (login, group) ->
            User(login, group.sumOf { it.contributions })
        }
        .sortedByDescending { it.contributions }
```

`groupingBy()` 사용 대안:
```kotlin
fun List<User>.aggregate(): List<User> =
    groupingBy { it.login }
        .fold(0) { acc, user -> acc + user.contributions }
        .map { (login, contributions) -> User(login, contributions) }
        .sortedByDescending { it.contributions }
```

---

## 섹션 3: 콜백

### 블로킹의 문제점
- I/O 중에 메인 스레드가 차단됨
- UI가 응답하지 않게 됨
- 나쁜 사용자 경험

### 해결책 1: 백그라운드 스레드
```kotlin
fun loadContributorsBackground(
    service: GitHubService,
    req: RequestData,
    updateResults: (List<User>) -> Unit
) {
    thread {
        updateResults(loadContributorsBlocking(service, req))
    }
}

// 사용법
loadContributorsBackground(service, req) { users ->
    SwingUtilities.invokeLater {
        updateResults(users, startTime)
    }
}
```

**이점:** 메인 UI 스레드가 응답성을 유지

### 해결책 2: Retrofit 콜백 API

```kotlin
fun loadContributorsCallbacks(
    service: GitHubService,
    req: RequestData,
    updateResults: (List<User>) -> Unit
) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->  // #1
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name)
                .onResponse { responseUsers ->                     // #2
                    logUsers(repo, responseUsers)
                    val users = responseUsers.bodyList()
                    allUsers += users
                }
        }
    }
    updateResults(allUsers.aggregate())
}
```

**문제:** 비동기 응답이 도착하기 전에 `updateResults()`가 호출됨

### 동시 콜백 해결책

**CountDownLatch 사용 (최선):**
```kotlin
val countDownLatch = CountDownLatch(repos.size)

for (repo in repos) {
    service.getRepoContributorsCall(req.org, repo.name)
        .onResponse { responseUsers ->
            // 레포지토리 처리
            countDownLatch.countDown()
        }
}

countDownLatch.await()
updateResults(allUsers.aggregate())
```

---

## 섹션 4: 일시 중단 함수

### 새로운 GitHub 서비스 API
```kotlin
interface GitHubService {
    @GET("orgs/{org}/repos?per_page=100")
    suspend fun getOrgRepos(
        @Path("org") org: String
    ): Response<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    suspend fun getRepoContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<User>>
}
```

**주요 차이점:**
- `suspend` 키워드로 표시됨
- 결과를 직접 반환 (Call로 래핑되지 않음)
- 일시 중단 중에 스레드가 차단되지 않음
- 오류 시 예외가 던져짐

### 일시 중단 함수 구현

**해결책:**
```kotlin
suspend fun loadContributorsSuspend(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    return repos.flatMap { repo ->
        service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}
```

**참고:** 순차적 실행; 모든 요청이 이전 요청 완료를 기다림

---

## 섹션 5: 코루틴

### 개념
- **코루틴:** 일시 중지하고 재개할 수 있는 일시 중단 가능한 계산
- **일시 중단:** 계산을 일시 중지하고 스레드를 다른 작업에 해제
- 스레드에 비해 경량
- `launch`, `async`, 또는 `runBlocking`으로 시작 가능

### 코루틴 시작

**launch:** 결과를 반환하지 않고 계산 시작
```kotlin
launch {
    val users = loadContributorsSuspend(req)
    updateResults(users, startTime)
}
```

**async:** 계산을 시작하고 Deferred<T> 반환
```kotlin
val deferred: Deferred<Int> = async {
    loadData()
}
println(deferred.await())
```

**runBlocking:** 일반 함수와 일시 중단 함수 사이의 브릿지
```kotlin
fun main() = runBlocking {
    val deferred = async { loadData() }
    println(deferred.await())
}

suspend fun loadData(): Int {
    delay(1000L)
    return 42
}
```

### 다중 Async 호출
```kotlin
fun main() = runBlocking {
    val deferreds: List<Deferred<Int>> = (1..3).map {
        async {
            delay(1000L * it)
            println("Loading $it")
            it
        }
    }
    val sum = deferreds.awaitAll().sum()
    println("$sum")
}
```

---

## 섹션 6: 동시성

### 동시 로딩

**목표:** 모든 레포지토리를 동시에 로드

**해결책:**
```kotlin
suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData
): List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        async {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    deferreds.awaitAll().flatten().aggregate()
}
```

### 다른 디스패처 사용

**Dispatchers.Default:** 병렬 실행을 위한 스레드 풀
```kotlin
async(Dispatchers.Default) {
    service.getRepoContributors(req.org, repo.name)
        .also { logUsers(repo, it) }
        .bodyList()
}
```

**Dispatchers.Main:** 메인 UI 스레드
```kotlin
launch(Dispatchers.Main) {
    updateResults()
}
```

### 완전한 구현 패턴
```kotlin
launch(Dispatchers.Default) {
    val users = loadContributorsConcurrent(service, req)
    withContext(Dispatchers.Main) {
        updateResults(users, startTime)
    }
}
```

**동시 로딩의 이점:**
- 순차적: ~4초
- 동시적: ~2초 (서버에 따라 다름)
- 더 효율적인 스레드 사용

---

## 섹션 7: 구조적 동시성

### 개념
- **스코프:** 부모-자식 관계 관리
- **컨텍스트:** 기술 정보 (디스패처, 이름 등)
- **취소:** 부모에서 자식으로 자동 전파
- **대기:** 부모는 완료 전에 모든 자식을 기다림

### 코루틴 빌더
```kotlin
launch { }           // Job 반환
async { }            // Deferred<T> 반환
runBlocking { }      // 현재 스레드 차단
coroutineScope { }   // 새 코루틴 없이 새 스코프
```

### 부모-자식 관계
```kotlin
runBlocking {  // 외부 코루틴
    launch {   // 자식 코루틴 #1
        // ...
    }
    launch {   // 자식 코루틴 #2
        // ...
    }
    // 완료 전에 모든 자식을 기다림
}
```

### 로딩 취소

**취소 가능한 버전 (coroutineScope 사용):**
```kotlin
suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData
): List<User> = coroutineScope {
    val repos = service.getOrgRepos(req.org).bodyList()
    val deferreds = repos.map { repo ->
        async {
            delay(3000)  // 취소 가능
            service.getRepoContributors(req.org, repo.name).bodyList()
        }
    }
    deferreds.awaitAll().flatten().aggregate()
}
```

**취소 불가능 (GlobalScope 사용):**
```kotlin
suspend fun loadContributorsNotCancellable(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service.getOrgRepos(req.org).bodyList()
    val deferreds = repos.map { repo ->
        GlobalScope.async {  // 독립적, 취소 불가능
            delay(3000)
            service.getRepoContributors(req.org, repo.name).bodyList()
        }
    }
    return deferreds.awaitAll().flatten().aggregate()
}
```

### 취소 설정
```kotlin
val job = launch {
    val users = loadContributorsConcurrent(service, req)
    updateResults(users, startTime)
}

val listener = ActionListener {
    job.cancel()  // job과 모든 자식 취소
    updateLoadingStatus(CANCELED)
}
addCancelListener(listener)
```

---

## 섹션 8: 진행 상황 표시

### 문제
- 사용자는 모든 로딩이 완료된 후에만 결과를 봄
- 중간 피드백 없음

### 해결책
중간 결과에 대해 UI를 업데이트하는 콜백 사용:

```kotlin
suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var allUsers = emptyList<User>()
    for ((index, repo) in repos.withIndex()) {
        val users = service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()

        allUsers = (allUsers + users).aggregate()
        updateResults(allUsers, index == repos.lastIndex)
    }
}
```

**사용법:**
```kotlin
launch(Dispatchers.Default) {
    loadContributorsProgress(service, req) { users, completed ->
        withContext(Dispatchers.Main) {
            updateResults(users, startTime, completed)
        }
    }
}
```

---

## 섹션 9: 채널

### 채널 개념
- 코루틴 간 통신 프리미티브
- 프로듀서가 데이터 전송
- 컨슈머가 데이터 수신
- 공유 가변 상태 없음

### 채널 다이어그램
```
Producer -> [Channel] -> Consumer
```

### 채널 인터페이스
```kotlin
interface SendChannel<in E> {
    suspend fun send(element: E)
    fun close(): Boolean
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E> : SendChannel<E>, ReceiveChannel<E>
```

### 채널 유형

**무제한 채널**
- 크기 제한 없음
- `send()`가 절대 일시 중단되지 않음
- `receive()`가 비어있으면 일시 중단
- OutOfMemoryException 위험

```kotlin
val channel = Channel<String>(UNLIMITED)
```

**버퍼가 있는 채널**
- 고정된 크기 제한
- `send()`가 가득 차면 일시 중단
- `receive()`가 비어있으면 일시 중단

```kotlin
val channel = Channel<String>(10)
```

**랑데뷰 채널**
- 버퍼 없음 (크기 = 0)
- `send()`와 `receive()`가 "만나야" 함
- 하나가 항상 다른 하나가 호출할 때까지 일시 중단

```kotlin
val channel = Channel<String>()  // 기본값
```

**합류된 채널**
- 최신 요소만 저장
- `send()`가 절대 일시 중단되지 않음
- 새 요소가 이전 요소를 덮어씀

```kotlin
val channel = Channel<String>(CONFLATED)
```

### 채널을 사용한 동시 실행과 진행 상황

**해결책:**
```kotlin
suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val channel = Channel<List<User>>()

    for (repo in repos) {
        launch {
            val users = service
                .getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
            channel.send(users)
        }
    }

    var allUsers = emptyList<User>()
    repeat(repos.size) { index ->
        val users = channel.receive()
        allUsers = (allUsers + users).aggregate()
        updateResults(allUsers, index == repos.lastIndex)
    }
}
```

**이점:**
- 동시 요청 (모두 즉시 시작)
- 순차적 소비 (동기화 필요 없음)
- 요청 사이에 진행 상황 업데이트
- 콜백 동기화보다 단순함

---

## 섹션 10: 코루틴 테스트

### 실시간 테스트의 문제
- 각 테스트가 2-4초 걸림
- 느린 피드백 루프
- 기계 의존적 타이밍

### 가상 시간 해결책

**runTest 사용:**
```kotlin
@Test
fun testDelayInSuspend() = runTest {
    val realStartTime = System.currentTimeMillis()
    val virtualStartTime = currentTime

    foo()

    println("${System.currentTimeMillis() - realStartTime} ms")  // ~ 6 ms
    println("${currentTime - virtualStartTime} ms")              // 1000 ms
}

suspend fun foo() {
    delay(1000)  // 실제 지연 없이 자동 진행
    println("foo")
}
```

### 동시 테스트 예제
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@Test
fun testConcurrent() = runTest {
    val startTime = currentTime
    val result = loadContributorsConcurrent(MockGithubService, testRequestData)

    Assert.assertEquals("Wrong result", expectedConcurrentResults.users, result)

    val totalTime = currentTime - startTime
    Assert.assertEquals(
        "Should run concurrently: 1000 (repos) + max(1000, 1200, 800) = 2200 ms",
        2200,
        totalTime
    )
}
```

---

## 성능 비교

| 접근 방식 | 시간 | 참고 |
|----------|------|------|
| 블로킹 순차적 | ~4000ms | 1000 + 1000 + 1200 + 800 |
| 백그라운드 스레드 | ~4000ms | 더 나은 UX, 같은 시간 |
| 콜백 | ~2000ms | 동시적이지만 복잡함 |
| 일시 중단 | ~4000ms | 깔끔하지만 여전히 순차적 |
| 동시적 (코루틴) | ~2200ms | 1000 + max(1000, 1200, 800) |
| 진행 상황 | ~4000ms | 중간 결과 표시 |
| 채널 | ~2200ms | 동시적 + 진행 상황 |

---

## 핵심 요약

1. **블로킹** - 단순하지만 UI를 멈춤
2. **콜백** - 논블로킹이지만 여러 비동기 연산에서 오류 발생 쉬움
3. **일시 중단 함수** - 깔끔한 코드, 순차적 실행
4. **async를 사용한 코루틴** - 동시 실행, 경량
5. **채널** - 코루틴 간 안전한 통신
6. **구조적 동시성** - 자동 취소와 정리
7. **테스트** - 빠르고 결정적인 테스트를 위해 가상 시간 사용

---

## 관련 리소스
- [코루틴 기초](coroutines-basics.md)
- [취소와 타임아웃](cancellation-and-timeouts.md)

---

# 공유 가변 상태와 동시성

> **원문:** https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html

## 개요

코루틴은 `Dispatchers.Default`와 같은 멀티스레드 디스패처를 사용하여 병렬로 실행될 수 있으며, 이는 표준 병렬 처리 문제를 야기합니다. 주요 과제는 공유 가변 상태에 대한 접근 동기화입니다.

---

## 문제

여러 코루틴이 동기화 없이 공유 가변 상태에 동시에 접근하고 수정하면 경쟁 조건이 발생합니다. 예제:

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100 // 시작할 코루틴 수
    val k = 1000 // 각 코루틴이 액션을 반복하는 횟수
    val time = measureTimeMillis {
        coroutineScope {
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}

var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun { counter++ }
    }
    println("Counter = $counter")
}
```

**결과**: 동기화되지 않은 동시 접근으로 인해 "Counter = 100000"이 출력될 가능성이 낮습니다.

---

## Volatile은 도움이 되지 않는다

`@Volatile` 어노테이션은 원자적 읽기/쓰기를 보장하지만 증가와 같은 **복합 연산의 원자성은 보장하지 않습니다**:

```kotlin
@Volatile
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun { counter++ }
    }
    println("Counter = $counter")
}
```

**문제**: `counter++`가 세 가지 연산 (읽기, 증가, 쓰기)이기 때문에 여전히 잘못된 결과를 생성합니다.

---

## 스레드 안전 데이터 구조

간단한 연산에는 `AtomicInteger`와 같은 원자 클래스를 사용합니다:

```kotlin
import java.util.concurrent.atomic.*

val counter = AtomicInteger()

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun { counter.incrementAndGet() }
    }
    println("Counter = $counter")
}
```

**장점**: 간단한 카운터, 컬렉션, 표준 연산에 대해 가장 빠른 솔루션.
**제한사항**: 준비된 스레드 안전 구현 없이는 복잡한 상태로 확장이 어렵습니다.

---

## 세밀한 스레드 제한

단일 스레드 컨텍스트를 사용하여 공유 상태 접근을 단일 스레드로 제한합니다:

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            withContext(counterContext) {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```

**문제**: 모든 연산에 대한 컨텍스트 전환으로 인해 매우 느립니다.

---

## 거친 스레드 제한

더 큰 논리 블록을 단일 스레드로 제한합니다:

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(counterContext) {
        massiveRun { counter++ }
    }
    println("Counter = $counter")
}
```

**장점**: 합리적인 성능으로 훨씬 빠르고 올바른 결과를 생성합니다.

---

## 상호 배제

임계 섹션을 보호하기 위해 `Mutex`를 사용합니다 (`synchronized`의 코루틴 친화적 대안):

```kotlin
import kotlinx.coroutines.sync.*

val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```

**주요 차이점**: `Mutex.lock()`은 **일시 중단 함수**입니다 (스레드를 차단하지 않음).

**사용 사례**: 공유 상태를 주기적으로 수정해야 하지만 자연스러운 제한 스레드가 없을 때 가장 좋습니다.

---

## 요약 표

| 접근 방식 | 성능 | 정확성 | 적합한 용도 |
|----------|------|--------|------------|
| Volatile | 느림 | 부정확 | 해당 없음 |
| AtomicInteger | 가장 빠름 | 정확 | 간단한 카운터/연산 |
| 세밀한 제한 | 매우 느림 | 정확 | 해당 없음 |
| 거친 제한 | 좋음 | 정확 | UI 애플리케이션 |
| Mutex | 보통 | 정확 | 복잡한 주기적 업데이트 |
