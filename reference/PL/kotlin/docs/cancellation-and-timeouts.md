# 취소와 타임아웃

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
[`isActive`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/is-active.html) 속성은 코루틴이 취소되면 `false`입니다.

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

일시 중단된 코루틴이 취소되면 값이 사용 가능하더라도 값을 반환하는 대신 `CancellationException`으로 재개됩니다. 이를 **즉시 취소(prompt cancellation)**라고 합니다.

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
