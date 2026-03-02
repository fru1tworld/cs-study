# 비동기 Flow

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
방출자와 모든 연산자의 예외는 수집자에 의해 잡힙니다.

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
