# 공유 가변 상태와 동시성

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
