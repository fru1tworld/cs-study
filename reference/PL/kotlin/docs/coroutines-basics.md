# 코루틴 기초

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
