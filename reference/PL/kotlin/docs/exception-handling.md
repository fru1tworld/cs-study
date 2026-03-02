# 코루틴 예외 처리

## 개요

이 문서는 Kotlin 코루틴에서의 예외 처리와 취소를 다룹니다. 취소된 코루틴이 일시 중단 지점에서 `CancellationException`을 던지는 방법과 취소 중 예외가 발생하거나 여러 자식이 예외를 던질 때 어떤 일이 발생하는지 설명합니다.

---

## 예외 전파

코루틴 빌더는 두 가지 유형이 있습니다:

1. **예외를 자동으로 전파**: `launch`
2. **예외를 사용자에게 노출**: `async`와 `produce`

루트 코루틴을 생성할 때 사용하면, `launch`는 예외를 처리되지 않은 것으로 취급하고 (Java의 `Thread.uncaughtExceptionHandler`와 유사), `async`와 `produce`는 사용자가 `await()` 또는 `receive()`를 통해 예외를 소비하도록 합니다.

### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val job = GlobalScope.launch { // launch를 사용한 루트 코루틴
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException() // 콘솔에 출력됨
    }
    job.join()
    println("Joined failed job")

    val deferred = GlobalScope.async { // async를 사용한 루트 코루틴
        println("Throwing exception from async")
        throw ArithmeticException() // 아무것도 출력되지 않음, 사용자가 await 호출해야 함
    }
    try {
        deferred.await()
        println("Unreached")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}
```

**출력:**
```
Throwing exception from launch
Exception in thread "DefaultDispatcher-worker-1 @coroutine#2" java.lang.IndexOutOfBoundsException
Joined failed job
Throwing exception from async
Caught ArithmeticException
```

---

## CoroutineExceptionHandler

`CoroutineExceptionHandler`는 루트 코루틴과 그 자식들의 처리되지 않은 예외 처리를 사용자 정의하는 데 사용되는 컨텍스트 요소입니다. 일반적인 `catch` 블록으로 작동합니다.

### 핵심 포인트:
- 핸들러에서 예외를 **복구할 수 없음**
- **처리되지 않은 예외에만 호출됨**
- 자식 코루틴은 예외 처리를 계층 구조를 따라 부모에게 위임
- `async` 빌더는 결과 `Deferred` 객체에서 모든 예외를 잡으므로 핸들러가 효과 없음

### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    val job = GlobalScope.launch(handler) { // 루트 코루틴
        throw AssertionError()
    }

    val deferred = GlobalScope.async(handler) { // 루트이지만 async
        throw ArithmeticException() // 아무것도 출력되지 않음
    }

    joinAll(job, deferred)
}
```

**출력:**
```
CoroutineExceptionHandler got java.lang.AssertionError
```

---

## 취소와 예외

`CancellationException`은 취소에 내부적으로 사용되며 모든 핸들러에서 무시됩니다.

### 핵심 동작:
- `Job.cancel()`로 코루틴이 취소되면 종료되지만 부모를 취소하지 않음
- 코루틴이 `CancellationException` 이외의 예외를 던지면 해당 예외로 부모를 취소
- 이 동작은 구조적 동시성을 위한 안정적인 코루틴 계층을 보장

### 예제:
```kotlin
fun main() = runBlocking {
    val job = launch {
        val child = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child is cancelled")
            }
        }
        yield()
        println("Cancelling child")
        child.cancel()
        child.join()
        yield()
        println("Parent is not cancelled")
    }
    job.join()
}
```

**출력:**
```
Cancelling child
Child is cancelled
Parent is not cancelled
```

---

## 예외 집계

여러 자식이 실패할 때:
- **첫 번째 예외가 우선**하여 처리됨
- **추가 예외**는 억제된 예외로 첨부됨

### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception with suppressed ${exception.suppressed.contentToString()}")
    }

    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw ArithmeticException() // 두 번째 예외
            }
        }
        launch {
            delay(100)
            throw IOException() // 첫 번째 예외
        }
        delay(Long.MAX_VALUE)
    }
    job.join()
}
```

**출력:**
```
CoroutineExceptionHandler got java.io.IOException with suppressed [java.lang.ArithmeticException]
```

### 취소 예외 투명성:
취소 예외는 기본적으로 언래핑되어 원래 예외가 핸들러에 도달할 수 있습니다:

```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    val job = GlobalScope.launch(handler) {
        val innerJob = launch {
            launch {
                launch {
                    throw IOException()
                }
            }
        }
        try {
            innerJob.join()
        } catch (e: CancellationException) {
            println("Rethrowing CancellationException with original cause")
            throw e
        }
    }
    job.join()
}
```

**출력:**
```
Rethrowing CancellationException with original cause
CoroutineExceptionHandler got java.io.IOException
```

---

## 슈퍼비전

단방향 취소가 필요한 경우, 슈퍼비전은 다음을 허용합니다:
- 부모 취소가 자식에게 영향
- 자식 실패가 부모에게 영향을 주지 않음
- 자식 실패가 형제를 취소하지 않음

### SupervisorJob

`SupervisorJob`은 일반 `Job`과 유사하지만 취소가 아래쪽으로만 전파됩니다.

```kotlin
fun main() = runBlocking {
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
            println("The first child is failing")
            throw AssertionError("The first child is cancelled")
        }

        val secondChild = launch {
            firstChild.join()
            println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("The second child is cancelled because the supervisor was cancelled")
            }
        }

        firstChild.join()
        println("Cancelling the supervisor")
        supervisor.cancel()
        secondChild.join()
    }
}
```

**출력:**
```
The first child is failing
The first child is cancelled: true, but the second one is still active
Cancelling the supervisor
The second child is cancelled because the supervisor was cancelled
```

### supervisorScope

`supervisorScope`는 단방향 취소로 범위 지정된 동시성을 제공합니다:

```kotlin
fun main() = runBlocking {
    try {
        supervisorScope {
            val child = launch {
                try {
                    println("The child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("The child is cancelled")
                }
            }
            yield()
            println("Throwing an exception from the scope")
            throw AssertionError()
        }
    } catch(e: AssertionError) {
        println("Caught an assertion error")
    }
}
```

**출력:**
```
The child is sleeping
Throwing an exception from the scope
The child is cancelled
Caught an assertion error
```

### 슈퍼비전된 코루틴의 예외

각 자식은 `CoroutineExceptionHandler`를 통해 독립적으로 예외를 처리합니다. 자식 실패는 부모에게 전파되지 않습니다.

```kotlin
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    supervisorScope {
        val child = launch(handler) {
            println("The child throws an exception")
            throw AssertionError()
        }
        println("The scope is completing")
    }
    println("The scope is completed")
}
```

**출력:**
```
The scope is completing
The child throws an exception
CoroutineExceptionHandler got java.lang.AssertionError
The scope is completed
```

---

## 요약

| 기능 | 동작 |
|------|------|
| `launch` | 예외를 자동으로 전파 |
| `async` | `await()`를 통해 예외 노출 |
| `CoroutineExceptionHandler` | 루트 코루틴의 처리되지 않은 예외 처리 |
| `SupervisorJob` | 단방향 취소 (부모 -> 자식만) |
| `supervisorScope` | 단방향 취소를 가진 범위 지정된 동시성 |
| 예외 집계 | 첫 번째 예외가 우선; 나머지는 억제됨 |
