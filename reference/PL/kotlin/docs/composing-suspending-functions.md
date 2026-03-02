# 일시 중단 함수 합성

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
