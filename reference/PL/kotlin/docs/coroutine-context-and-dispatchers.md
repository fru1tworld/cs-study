# 코루틴 컨텍스트와 디스패처

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

`Dispatchers.Unconfined` 디스패처는 호출자 스레드에서 코루틴을 시작하지만 첫 번째 일시 중단 지점까지만입니다. 일시 중단 후에는 일시 중단 함수에 의해 결정된 스레드에서 재개됩니다.

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
