# Kotlin 채널

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

**참고:** 티커는 기본적으로 소비자 일시 정지에 대해 지연을 조정합니다. 고정된 요소 간 지연을 위해 `TickerMode.FIXED_DELAY`를 사용하세요.
