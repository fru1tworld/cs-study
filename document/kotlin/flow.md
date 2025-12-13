# Kotlin Flow

> 공식 문서: https://kotlinlang.org/docs/flow.html
> 라이브러리: kotlinx-coroutines-core

## 개요

Flow는 비동기적으로 계산된 여러 값을 순차적으로 방출하는 Cold 스트림입니다. `suspend` 함수는 단일 값을 비동기적으로 반환하지만, Flow는 여러 값을 비동기적으로 반환합니다.

```kotlin
// Sequence vs Flow
fun simpleSequence(): Sequence<Int> = sequence {
    for (i in 1..3) {
        Thread.sleep(100)  // 블로킹
        yield(i)
    }
}

fun simpleFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)  // 논블로킹
        emit(i)
    }
}
```

---

## Flow 생성

### flow 빌더

```kotlin
fun numbers(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(100)
        emit(i)  // 값 방출
    }
}

// 사용
fun main() = runBlocking {
    numbers().collect { value ->
        println(value)
    }
}
```

### 다른 빌더들

```kotlin
// flowOf - 고정된 값들
val flow1 = flowOf(1, 2, 3)

// asFlow - 컬렉션에서 변환
val flow2 = listOf(1, 2, 3).asFlow()
val flow3 = (1..5).asFlow()

// emptyFlow
val flow4 = emptyFlow<Int>()

// callbackFlow - 콜백 기반 API를 Flow로 변환
fun locationUpdates(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            trySend(result.lastLocation)
        }
    }

    locationClient.requestLocationUpdates(request, callback, looper)

    awaitClose {
        locationClient.removeLocationUpdates(callback)
    }
}

// channelFlow - 다른 CoroutineContext에서 방출
fun multiContextFlow(): Flow<Int> = channelFlow {
    send(1)  // 현재 컨텍스트
    withContext(Dispatchers.IO) {
        send(2)  // 다른 컨텍스트
    }
}
```

---

## Flow 속성

### Cold Stream

Flow는 수집(collect)될 때만 실행됩니다.

```kotlin
val flow = flow {
    println("Flow started")
    emit(1)
    emit(2)
}

println("Before collect")
flow.collect { println(it) }  // 여기서 "Flow started" 출력
println("After collect")

// 출력:
// Before collect
// Flow started
// 1
// 2
// After collect
```

### 순차성

Flow의 각 값은 순차적으로 처리됩니다.

```kotlin
(1..3).asFlow()
    .map { value ->
        println("Mapping $value")
        value * 2
    }
    .collect { value ->
        println("Collected $value")
    }

// 출력:
// Mapping 1
// Collected 2
// Mapping 2
// Collected 4
// Mapping 3
// Collected 6
```

### Context 보존

Flow는 컨텍스트를 보존합니다. flow 블록 내에서 `withContext`를 사용해 컨텍스트를 변경하면 안 됩니다.

```kotlin
// 잘못된 방법
fun wrongFlow(): Flow<Int> = flow {
    withContext(Dispatchers.Default) {  // 에러!
        emit(1)
    }
}

// 올바른 방법: flowOn 사용
fun correctFlow(): Flow<Int> = flow {
    emit(heavyComputation())
}.flowOn(Dispatchers.Default)
```

---

## 중간 연산자

### 변환 연산자

```kotlin
val flow = (1..5).asFlow()

// map
flow.map { it * 2 }
    .collect { println(it) }  // 2, 4, 6, 8, 10

// filter
flow.filter { it % 2 == 0 }
    .collect { println(it) }  // 2, 4

// transform (여러 값 방출 가능)
flow.transform { value ->
    emit(value)
    emit(value * 10)
}
.collect { println(it) }  // 1, 10, 2, 20, 3, 30, ...

// take
flow.take(3)
    .collect { println(it) }  // 1, 2, 3

// drop
flow.drop(2)
    .collect { println(it) }  // 3, 4, 5

// takeWhile / dropWhile
flow.takeWhile { it < 4 }
    .collect { println(it) }  // 1, 2, 3
```

### flatMap 연산자

```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First")
    delay(500)
    emit("$i: Second")
}

// flatMapConcat: 순차적으로 병합
(1..3).asFlow()
    .flatMapConcat { requestFlow(it) }
    .collect { println(it) }
// 1: First, 1: Second, 2: First, 2: Second, 3: First, 3: Second

// flatMapMerge: 동시에 병합
(1..3).asFlow()
    .flatMapMerge { requestFlow(it) }
    .collect { println(it) }
// 순서가 섞일 수 있음

// flatMapLatest: 새 값이 오면 이전 플로우 취소
(1..3).asFlow()
    .flatMapLatest { requestFlow(it) }
    .collect { println(it) }
// 마지막 값의 결과만 완전히 수집됨
```

### 결합 연산자

```kotlin
val nums = (1..3).asFlow()
val strs = flowOf("one", "two", "three")

// zip: 1:1 결합
nums.zip(strs) { a, b -> "$a -> $b" }
    .collect { println(it) }
// 1 -> one, 2 -> two, 3 -> three

// combine: 최신 값 결합
val flow1 = flow {
    emit(1)
    delay(100)
    emit(2)
}
val flow2 = flow {
    emit("a")
    delay(150)
    emit("b")
}

flow1.combine(flow2) { a, b -> "$a$b" }
    .collect { println(it) }
// 1a, 2a, 2b (각 flow에서 새 값이 올 때마다 결합)

// merge: 여러 flow를 하나로 병합
merge(flow1.map { it.toString() }, flow2)
    .collect { println(it) }
```

### 버퍼링

```kotlin
fun simpleFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)  // 생산에 100ms
        emit(i)
    }
}

// 버퍼 없이 (순차)
val time1 = measureTimeMillis {
    simpleFlow()
        .collect { value ->
            delay(300)  // 소비에 300ms
            println(value)
        }
}
println("Without buffer: ${time1}ms")  // ~1200ms

// 버퍼 사용 (동시)
val time2 = measureTimeMillis {
    simpleFlow()
        .buffer()  // 별도 코루틴에서 방출
        .collect { value ->
            delay(300)
            println(value)
        }
}
println("With buffer: ${time2}ms")  // ~1000ms

// conflate: 처리 못한 값 무시
simpleFlow()
    .conflate()
    .collect { value ->
        delay(300)
        println(value)  // 1, 3 (2는 건너뜀)
    }

// collectLatest: 새 값이 오면 이전 수집 취소
simpleFlow()
    .collectLatest { value ->
        println("Collecting $value")
        delay(300)
        println("Done $value")
    }
// 마지막 값만 "Done" 출력
```

---

## 최종 연산자

### collect

```kotlin
val flow = flowOf(1, 2, 3)

// 기본 collect
flow.collect { println(it) }

// collectLatest
flow.collectLatest {
    println("Started $it")
    delay(100)
    println("Finished $it")
}

// collectIndexed
flow.collectIndexed { index, value ->
    println("[$index] = $value")
}
```

### 집계 연산자

```kotlin
val flow = flowOf(1, 2, 3, 4, 5)

// toList / toSet
val list = flow.toList()
val set = flow.toSet()

// first / last
val first = flow.first()          // 1
val last = flow.last()            // 5
val firstOrNull = flow.firstOrNull { it > 10 }  // null

// single (하나의 값만 있어야 함)
val single = flowOf(1).single()   // 1
// flowOf(1, 2).single()          // 예외!

// count
val count = flow.count()          // 5
val filtered = flow.count { it > 2 }  // 3

// reduce
val sum = flow.reduce { a, b -> a + b }  // 15

// fold
val sumFrom10 = flow.fold(10) { acc, value -> acc + value }  // 25
```

---

## 컨텍스트 변경

### flowOn

업스트림 연산의 컨텍스트를 변경합니다.

```kotlin
fun expensiveFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        Thread.sleep(100)  // CPU 집약적 작업 시뮬레이션
        emit(i)
    }
}.flowOn(Dispatchers.Default)  // 이 위의 연산에 적용

fun main() = runBlocking {
    expensiveFlow()
        .map { it * 2 }  // Main 스레드
        .collect { println("${Thread.currentThread().name}: $it") }
}
```

### withContext와의 차이

```kotlin
// flowOn: 업스트림에 적용
flow {
    emit(1)  // Default 디스패처
}
.flowOn(Dispatchers.Default)
.map { it * 2 }  // 수집 컨텍스트
.collect { println(it) }

// withContext: flow 블록 내에서 사용 불가
flow {
    withContext(Dispatchers.Default) {  // 에러!
        emit(1)
    }
}
```

---

## 예외 처리

### try-catch

```kotlin
fun simpleFlow(): Flow<Int> = flow {
    emit(1)
    throw RuntimeException("Error!")
}

fun main() = runBlocking {
    try {
        simpleFlow().collect { println(it) }
    } catch (e: Exception) {
        println("Caught: $e")
    }
}
```

### catch 연산자

```kotlin
flow {
    emit(1)
    throw RuntimeException("Error!")
}
.catch { e ->
    println("Caught: $e")
    emit(-1)  // 대체 값 방출 가능
}
.collect { println(it) }
// 1, Caught: ..., -1

// catch는 업스트림 예외만 잡음
flow { emit(1) }
.map { check(it > 0) { "Invalid" }; it }
.catch { println("Caught in operator") }
.collect { check(it > 0) { "Invalid in collect" } }  // 여기서 발생하면 잡히지 않음
```

### onCompletion

```kotlin
flow {
    emit(1)
    emit(2)
}
.onCompletion { cause ->
    if (cause != null) {
        println("Flow completed exceptionally: $cause")
    } else {
        println("Flow completed successfully")
    }
}
.collect { println(it) }
```

---

## 취소

Flow의 취소는 협력적입니다.

```kotlin
fun main() = runBlocking {
    val flow = flow {
        for (i in 1..5) {
            println("Emitting $i")
            emit(i)
        }
    }

    withTimeoutOrNull(250) {
        flow.collect { value ->
            delay(100)
            println("Collected $value")
        }
    }
    println("Done")
}

// cancellable 연산자 (바쁜 flow에서 취소 확인)
(1..5).asFlow()
    .cancellable()  // 각 방출마다 취소 확인
    .collect { value ->
        if (value == 3) cancel()
        println(value)
    }
```

---

## StateFlow

Hot flow로, 현재 상태와 새로운 상태 업데이트를 수집자에게 방출합니다.

```kotlin
class CounterViewModel {
    // MutableStateFlow로 상태 관리
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() {
        _count.value++
        // 또는 _count.update { it + 1 }
    }
}

// 사용
fun main() = runBlocking {
    val viewModel = CounterViewModel()

    // 백그라운드에서 수집
    val job = launch {
        viewModel.count.collect { value ->
            println("Count: $value")
        }
    }

    delay(100)
    viewModel.increment()
    viewModel.increment()
    delay(100)
    job.cancel()
}
```

### StateFlow 특성

```kotlin
val stateFlow = MutableStateFlow(0)

// 항상 값을 가짐
println(stateFlow.value)  // 0

// 같은 값은 무시됨 (distinctUntilChanged와 유사)
stateFlow.value = 0  // 방출되지 않음
stateFlow.value = 1  // 방출됨

// replay = 1: 새 수집자는 현재 값을 즉시 받음
launch {
    delay(100)
    stateFlow.collect { println("Collector: $it") }  // 현재 값부터 시작
}
```

### stateIn

Flow를 StateFlow로 변환합니다.

```kotlin
val coldFlow = flow {
    emit(1)
    delay(1000)
    emit(2)
}

// stateIn으로 변환
val stateFlow = coldFlow.stateIn(
    scope = coroutineScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
)

// SharingStarted 옵션
// - Eagerly: 즉시 시작
// - Lazily: 첫 수집자가 나타날 때 시작
// - WhileSubscribed(): 수집자가 있을 때만 활성
```

---

## SharedFlow

Hot flow로, 여러 수집자에게 값을 방출합니다.

```kotlin
class EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun emit(event: Event) {
        _events.emit(event)
    }
}

// 사용
fun main() = runBlocking {
    val bus = EventBus()

    // 여러 수집자
    launch {
        bus.events.collect { println("Collector 1: $it") }
    }
    launch {
        bus.events.collect { println("Collector 2: $it") }
    }

    delay(100)
    bus.emit(Event.Click)
    delay(100)
}
```

### SharedFlow 설정

```kotlin
val sharedFlow = MutableSharedFlow<Int>(
    replay = 2,              // 새 수집자에게 재생할 값 수
    extraBufferCapacity = 3, // 추가 버퍼 용량
    onBufferOverflow = BufferOverflow.DROP_OLDEST  // 버퍼 오버플로우 정책
)

// BufferOverflow 옵션
// - SUSPEND: 버퍼가 가득 차면 일시 중단
// - DROP_OLDEST: 가장 오래된 값 삭제
// - DROP_LATEST: 가장 최근 값 삭제
```

### shareIn

Flow를 SharedFlow로 변환합니다.

```kotlin
val coldFlow = flow {
    println("Flow started")
    emit(1)
    delay(1000)
    emit(2)
}

val sharedFlow = coldFlow.shareIn(
    scope = coroutineScope,
    started = SharingStarted.Eagerly,
    replay = 1
)
```

---

## StateFlow vs SharedFlow

| 특성 | StateFlow | SharedFlow |
|------|-----------|------------|
| 초기값 | 필수 | 선택 (replay) |
| 현재 값 접근 | `.value` | 불가 |
| 같은 값 방출 | 무시 | 가능 |
| 기본 replay | 1 | 0 |
| 용도 | 상태 관리 | 이벤트 스트림 |

```kotlin
// StateFlow: 상태에 적합
val uiState: StateFlow<UiState>

// SharedFlow: 일회성 이벤트에 적합
val toastMessages: SharedFlow<String>
val navigationEvents: SharedFlow<NavigationEvent>
```

---

## 테스트

```kotlin
@Test
fun testFlow() = runTest {
    val flow = flow {
        emit(1)
        delay(100)
        emit(2)
    }

    val values = flow.toList()
    assertEquals(listOf(1, 2), values)
}

@Test
fun testStateFlow() = runTest {
    val stateFlow = MutableStateFlow(0)

    val values = mutableListOf<Int>()
    val job = launch(UnconfinedTestDispatcher()) {
        stateFlow.toList(values)
    }

    stateFlow.value = 1
    stateFlow.value = 2
    stateFlow.value = 3

    assertEquals(listOf(0, 1, 2, 3), values)
    job.cancel()
}

// Turbine 라이브러리 사용
@Test
fun testWithTurbine() = runTest {
    val flow = flowOf(1, 2, 3)

    flow.test {
        assertEquals(1, awaitItem())
        assertEquals(2, awaitItem())
        assertEquals(3, awaitItem())
        awaitComplete()
    }
}
```

---

## 실전 패턴

### 데이터 레이어에서 Flow

```kotlin
interface UserRepository {
    fun getUser(id: Long): Flow<User>
    fun getUsers(): Flow<List<User>>
}

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val api: UserApi
) : UserRepository {

    override fun getUser(id: Long): Flow<User> = flow {
        // 로컬에서 먼저 방출
        userDao.getUser(id)?.let { emit(it) }

        // 네트워크에서 가져와서 업데이트
        try {
            val remoteUser = api.getUser(id)
            userDao.insertUser(remoteUser)
            emit(remoteUser)
        } catch (e: Exception) {
            // 에러 처리
        }
    }.flowOn(Dispatchers.IO)
}
```

### ViewModel에서 StateFlow

```kotlin
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUsers()
                .catch { e ->
                    _uiState.value = UiState.Error(e.message)
                }
                .collect { users ->
                    _uiState.value = UiState.Success(users)
                }
        }
    }
}
```

---

## 참고 문서

- [Asynchronous Flow](https://kotlinlang.org/docs/flow.html)
- [StateFlow and SharedFlow](https://kotlinlang.org/docs/stateflow-and-sharedflow.html)
- [kotlinx.coroutines Flow API](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/)
