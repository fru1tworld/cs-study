# 시간 측정과 Duration

# Kotlin 시간 측정 (kotlin.time)

> **원문:** https://kotlinlang.org/docs/time-measurement.html

## 개요

`kotlin.time` 패키지는 시간 간격을 표현하고 측정하기 위한 멀티플랫폼 API를 제공합니다. `java.time.Duration`이나 `Thread.sleep` 앞뒤로 `System.currentTimeMillis()`를 찍는 방식과 달리, `Duration`은 단위를 명시적으로 다루는 값 타입이고 `TimeSource`는 벽시계(wall clock)가 아니라 단조 증가(monotonic) 시계를 기본으로 삼아 시스템 시간 변경에 영향받지 않는 경과 시간 측정을 지원합니다.

핵심 구성 요소는 세 가지입니다.

- **`Duration`** — 시간의 양(길이)을 나타내는 값 타입
- **`TimeSource`** / **`TimeMark`** — 특정 시점을 찍고 그 사이 경과 시간을 재는 도구
- **`measureTime` / `measureTimedValue`** — 코드 블록 실행 시간을 재는 헬퍼 함수

## Duration 생성하기

`Int`, `Long`, `Double`에 확장 프로퍼티가 붙어 있어 숫자 리터럴로 바로 `Duration`을 만들 수 있습니다.

```kotlin
val timeout = 5.seconds
val ttl = 10.minutes
val retryDelay = 500.milliseconds
val neverExpires = Double.POSITIVE_INFINITY.days
```

단위를 변수로 다뤄야 할 때는 `toDuration(unit: DurationUnit)` 확장 함수를 쓰면 됩니다.

```kotlin
fun waitFor(amount: Long, unit: DurationUnit): Duration = amount.toDuration(unit)
```

`DurationUnit`은 `NANOSECONDS`부터 `DAYS`까지 7개 값(`NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`)을 가진 열거형입니다.

## Duration 연산

`Duration`끼리, 또는 `Duration`과 숫자 사이에 사칙연산과 비교 연산이 정의되어 있습니다.

```kotlin
val total = 5.seconds + 30.seconds       // 35s
val remaining = 30.seconds - 5.seconds   // 25s
val doubled = 5.seconds * 2              // 10s
val half = 30.seconds / 2                // 15s
val ratio = 30.seconds / 5.seconds       // 6.0 (Double)
val negated = -30.seconds                // -30s
val abs = negated.absoluteValue          // 30s

30.minutes == 0.5.hours                  // true, 단위가 달라도 값으로 비교
3000.microseconds < 25000.nanoseconds    // false
```

내부적으로 값이 `Long` 하나에 인코딩되므로 `Duration`은 `@JvmInline value class`로 구현되어 있어 박싱 오버헤드가 거의 없습니다.

## 다른 형태로 변환하기

### 문자열로

```kotlin
5887.milliseconds.toString()                       // "5.887s"
5887.milliseconds.toString(DurationUnit.SECONDS, 2) // "5.89s" (소수점 자릿수 지정)
86420.seconds.toIsoString()                         // "PT24H0M20S" (ISO-8601)
```

### 숫자로

`inWhole*` 프로퍼티는 정수(Long)로 잘라낸 값을, `to*(unit)` 함수는 다른 단위로 변환한 값을 돌려줍니다.

```kotlin
val d = 30.minutes
d.inWholeSeconds                        // 1800
270.seconds.toDouble(DurationUnit.MINUTES)  // 4.5
d.toLong(DurationUnit.MILLISECONDS)
d.toInt(DurationUnit.SECONDS)
```

### 구성 요소로 분해하기

`toComponents`는 시/분/초/나노초 등으로 쪼개서 람다에 넘겨줍니다. 이때 세밀한 단위까지 다 받을 필요가 없으면 `_`로 무시하면 됩니다.

```kotlin
val d = 30.minutes
val label = d.toComponents { hours, minutes, _, _ -> "${hours}h ${minutes}m" }
// "0h 30m"
```

## 코드 실행 시간 측정: measureTime / measureTimedValue

반환값이 필요 없으면 `measureTime`, 블록의 결과와 소요 시간을 함께 얻고 싶으면 `measureTimedValue`를 사용합니다.

```kotlin
val elapsed: Duration = measureTime {
    Thread.sleep(100)
}
// elapsed ≈ 103ms

val (value, elapsed2) = measureTimedValue {
    Thread.sleep(100)
    42
}
println(value)   // 42
println(elapsed2) // ≈ 103ms
```

`measureTimedValue`가 돌려주는 `TimedValue<T>`는 `value`와 `duration` 두 필드를 가진 데이터 클래스라서 구조 분해로 바로 꺼내 쓸 수 있습니다.

## TimeSource와 TimeMark

`measureTime` 같은 편의 함수만으로 부족할 때, 즉 코드 블록으로 감싸기 어려운 두 시점 사이의 간격을 재고 싶을 때는 `TimeSource`를 직접 다룹니다. `TimeSource.markNow()`로 현재 시점을 찍은 `TimeMark`를 얻고, 두 마크를 빼면 그 사이의 `Duration`이 나옵니다.

```kotlin
val clock = TimeSource.Monotonic
val start = clock.markNow()

doSomeWork()

val elapsed: Duration = start.elapsedNow()
```

두 마크를 직접 비교하거나 뺄 수도 있습니다.

```kotlin
val mark1 = clock.markNow()
Thread.sleep(500)
val mark2 = clock.markNow()

val diff = mark2 - mark1   // Duration
mark2 > mark1              // true
```

### 데드라인/타임아웃 체크

`TimeMark`에 `Duration`을 더하면 미래 시점을 나타내는 새 `TimeMark`가 되고, `hasPassedNow()` / `hasNotPassedNow()`로 그 시점이 지났는지 바로 확인할 수 있습니다. 별도로 "지금 시각과 비교"하는 코드를 짤 필요가 없습니다.

```kotlin
val deadline = clock.markNow() + 5.seconds

if (deadline.hasPassedNow()) {
    // 타임아웃 처리
}
```

### 기본 TimeSource

`TimeSource.Monotonic`은 플랫폼마다 실제로는 다음을 사용합니다.

- JVM: `System.nanoTime()`
- JS(Node): `process.hrtime()`, JS(브라우저): `performance.now()`
- Native: `std::chrono::steady_clock` 계열

시스템 시각이 NTP 보정 등으로 바뀌어도 영향받지 않는 단조 시계이므로, 경과 시간 측정에는 `System.currentTimeMillis()`류보다 이쪽이 항상 안전합니다.

## 커스텀 TimeSource 만들기

테스트나 특수 플랫폼 API(예: 안드로이드 `SystemClock`)를 시간 소스로 쓰고 싶다면 `AbstractLongTimeSource`를 상속해 `read()`만 구현하면 됩니다.

```kotlin
object AndroidElapsedTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
    override fun read(): Long = SystemClock.elapsedRealtimeNanos()
}

val elapsed = AndroidElapsedTimeSource.measureTime {
    doHeavyWork()
}
```

테스트에서는 시간이 실제로 흐르길 기다릴 필요 없이 `TestTimeSource`로 읽음값을 직접 조작해 결정론적인 테스트를 짤 수 있습니다.

```kotlin
val testSource = TestTimeSource()
val mark = testSource.markNow()
testSource += 5.seconds   // 시간을 임의로 흘려보냄
println(mark.elapsedNow()) // 5s
```

## 다음 단계
- `kotlin.time.Instant`, `Clock`으로 벽시계 시각(달력 날짜) 다루기
- Java 상호운용: `Duration.toJavaDuration()` / `java.time.Duration.toKotlinDuration()`

---

# kotlin.time 패키지 API 요약

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/

## 주요 타입

| 타입 | 설명 |
|---|---|
| `Duration` | `@JvmInline value class`, 시간 길이를 표현. `Comparable<Duration>` |
| `DurationUnit` | 시간 단위 열거형 (`NANOSECONDS` ~ `DAYS`) |
| `TimeSource` | 시간 소스 인터페이스. `markNow()`로 `TimeMark` 생성 |
| `TimeMark` | 특정 시점 하나. `elapsedNow()`로 경과 시간 조회 |
| `ComparableTimeMark` | 같은 `TimeSource.WithComparableMarks`에서 나온 마크끼리 뺄셈·비교 가능 |
| `TimedValue<T>` | `data class(val value: T, val duration: Duration)`. `measureTimedValue` 결과 타입 |
| `AbstractLongTimeSource` | `Long` 단위로 시각을 읽는 커스텀 `TimeSource`의 베이스 클래스 |
| `TestTimeSource` | 읽음값을 코드로 직접 조작할 수 있는 테스트용 `TimeSource` |
| `Instant` | (Kotlin 2.3+) 벽시계상의 특정 순간. `Comparable<Instant>` |
| `Clock` | (Kotlin 2.3+) `Instant`를 만들어내는 소스 |

`Duration`은 값 클래스이므로 런타임에는 대개 `Long` 하나로 표현되어 박싱 없이 동작하고, 반대로 `TimeMark`/`Instant`는 인터페이스·클래스라 소스에 바인딩된 실제 객체입니다. 이 둘을 혼동하지 않는 것이 중요합니다: `Duration`은 "길이", `TimeMark`/`Instant`는 "시점"입니다.

## 주요 함수

```kotlin
inline fun measureTime(block: () -> Unit): Duration
inline fun TimeSource.measureTime(block: () -> Unit): Duration

inline fun <T> measureTimedValue(block: () -> T): TimedValue<T>
inline fun <T> TimeSource.measureTimedValue(block: () -> T): TimedValue<T>

fun Int.toDuration(unit: DurationUnit): Duration
fun Long.toDuration(unit: DurationUnit): Duration
fun Double.toDuration(unit: DurationUnit): Duration
```

`measureTime`/`measureTimedValue`는 수신자 없이 쓰면 내부적으로 `TimeSource.Monotonic`을 사용하고, `TimeSource` 위의 확장 함수 형태로 쓰면 그 소스를 그대로 사용합니다. 둘 다 `inline` 함수라서 람다 캡처에 따른 오버헤드가 없습니다.

### Java/JS 상호운용 변환 함수

```kotlin
fun Duration.toJavaDuration(): java.time.Duration
fun java.time.Duration.toKotlinDuration(): Duration

fun DurationUnit.toTimeUnit(): java.util.concurrent.TimeUnit
fun TimeUnit.toDurationUnit(): DurationUnit

fun Instant.toJavaInstant(): java.time.Instant
fun java.time.Instant.toKotlinInstant(): Instant
```

기존에 `java.time.Duration`이나 `TimeUnit`을 받는 Java API와 연동할 때, `kotlin.time.Duration`으로 로직을 짜고 API 경계에서만 위 변환 함수로 감싸주면 내부 코드는 계속 Kotlin다운 표현을 유지할 수 있습니다.

## 요약: 이럴 때 무엇을 쓰나

- 코드 한 블록의 실행 시간만 재면 된다 → `measureTime` / `measureTimedValue`
- 블록으로 감싸기 어려운 두 시점 사이 간격, 혹은 데드라인 체크 → `TimeSource.markNow()` + `TimeMark`
- 설정값(타임아웃, TTL 등)을 표현하는 상수 → `Duration` 리터럴 (`5.seconds` 등)
- 테스트에서 시간 흐름을 직접 통제 → `TestTimeSource`
- Java API와의 경계 → `toJavaDuration()` / `toKotlinDuration()`
