# 코틀린 표준 라이브러리: 수학, 시간, 난수, 입출력

## 수학 함수와 상수

## Kotlin 수학 함수와 상수 (kotlin.math)

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.math/

### 개요

- `kotlin.math` 패키지: 삼각함수·지수·로그 함수·반올림·절댓값·최솟값·최댓값 등 수치 계산용 함수와 상수 모음
- 대부분 함수는 `Double`과 `Float` 양쪽에 오버로드됨 → 일부는 `Int`, `Long`, `UInt`, `ULong`까지 지원
- 모든 함수는 공통(Common) 코드에서 사용 가능 → JVM, JS, Native, Wasm 어디서든 동일하게 동작

### 상수: PI, E

원주율과 자연로그의 밑을 `Double` 상수로 제공.

```kotlin
println(kotlin.math.PI)  // 3.141592653589793
println(kotlin.math.E)   // 2.718281828459045

val circumference = 2 * PI * radius
```

### 삼각함수

- 각도는 라디안 단위
- `sin`, `cos`, `tan`과 역함수 `asin`, `acos`, `atan`
- 두 인자를 받아 사분면을 구분하는 `atan2`

```kotlin
val angle = PI / 6
println(sin(angle))       // 0.5
println(atan2(1.0, 1.0))  // PI / 4, 사분면을 고려한 각도
```

- `asin`, `atan`: `[-PI/2, PI/2]` 범위의 각도 반환
- `acos`: `[0, PI]` 범위 반환
- `atan2(y, x)`: 좌표 `(x, y)`가 이루는 각도를 `[-PI, PI]` 범위로 반환 → `atan(y/x)`와 달리 사분면 정보를 잃지 않음

### 쌍곡선 함수

`sinh`, `cosh`, `tanh`와 역함수 `asinh`, `acosh`, `atanh`가 동일한 패턴으로 제공됨.

```kotlin
println(sinh(1.0))   // 1.1752...
println(tanh(0.0))   // 0.0
```

### 지수·로그 함수

```kotlin
println(exp(1.0))          // e^1 = E
println(ln(E))             // 1.0
println(log10(1000.0))     // 3.0
println(log2(8.0))         // 3.0
println(log(8.0, 2.0))     // 임의의 밑(base)에 대한 로그
```

작은 값을 다룰 때는 정밀도 손실을 줄인 전용 함수 사용.

- `expm1(x)`: `exp(x) - 1`과 같음 → `x`가 0에 가까울 때 오차가 훨씬 작음
- `ln1p(x)`: `ln(x + 1)`과 같음 → 마찬가지로 작은 `x`에서 정밀함

### 거듭제곱과 제곱근

`pow`는 `Double`/`Float`의 확장 함수 → 지수가 실수인 버전과 정수인 버전 모두 있음.

```kotlin
println(2.0.pow(10))     // 1024.0, 지수가 Int
println(2.0.pow(0.5))    // 1.414..., 지수가 Double, sqrt(2)와 동일
println(sqrt(16.0))      // 4.0
println(cbrt(27.0))      // 3.0, 세제곱근
```

빗변 길이를 구하는 `hypot(x, y)`는 `sqrt(x*x + y*y)`와 결과가 같음 → 중간 계산에서 오버플로가 발생하지 않도록 구현됨.

```kotlin
println(hypot(3.0, 4.0))  // 5.0
```

### 절댓값과 부호

- `abs`: `Double`, `Float`, `Int`, `Long`을 모두 지원하는 최상위 함수
- `absoluteValue`: 같은 역할을 하는 확장 프로퍼티, 둘 중 코드 스타일에 맞는 쪽을 쓰면 됨

```kotlin
println(abs(-5))            // 5
println((-5).absoluteValue) // 5
```

부호만 필요하면 `sign` 사용 → `Double`/`Float`에서는 `1.0`, `-1.0`, `0.0`을, `Int`/`Long`에서는 `1`, `-1`, `0`을 반환.

```kotlin
println((-42.0).sign)  // -1.0
println(0.sign)        // 0
```

### 반올림 함수

용도에 따라 네 가지 반올림 방식으로 나뉨.

- `round(x)`: 가장 가까운 정수로, 정확히 중간값이면 짝수 쪽으로 반올림(round half to even)
- `ceil(x)`: 양의 무한대 방향으로 올림
- `floor(x)`: 음의 무한대 방향으로 내림
- `truncate(x)`: 소수부를 버리고 0 방향으로 자름

```kotlin
println(round(2.5))     // 2.0, 짝수로 반올림
println(round(3.5))     // 4.0
println(ceil(2.1))      // 3.0
println(floor(2.9))     // 2.0
println(truncate(-2.9)) // -2.0
```

`Int`/`Long` 값이 바로 필요하면 `roundToInt()`, `roundToLong()` 사용 → 이쪽은 `round`와 달리 중간값을 항상 양의 무한대 방향으로 반올림.

```kotlin
println(2.5.roundToInt())  // 3
```

### 최솟값·최댓값

`min`, `max`: `Double`, `Float`, `Int`, `Long`은 물론 Kotlin 1.5부터 `UInt`, `ULong`까지 지원하는 두 값 비교 함수.

```kotlin
println(min(3, 7))   // 3
println(max(3.0, 7.0))  // 7.0
```

셋 이상을 비교하려면 `maxOf`/`minOf`(가변 인자를 받는 별도 함수) 또는 컬렉션의 `maxOrNull()` 계열 사용이 낫음.

### 부동소수점 정밀 조작

부동소수점 표현의 미묘한 차이를 다뤄야 할 때 쓰는 함수들.

- `nextUp`, `nextDown`, `nextTowards`, `ulp`: `Double`이면 공통(Common) 코드에서도 그대로 쓸 수 있음, `Float` 버전만 플랫폼에 따라 지원 범위가 다름
- `IEEErem`: 예외적으로 `Double`/`Float` 모두 JVM과 Native에서만 제공됨

- `IEEErem(divisor)`: IEEE 754 표준의 나머지 연산, `%` 연산자와 반올림 방식이 다름(JVM/Native 전용)
- `nextUp()` / `nextDown()`: 표현 가능한 다음 큰 값 / 작은 값 반환
- `nextTowards(to)`: 지정한 값 방향으로 한 걸음 이동한 표현 가능한 값 반환
- `ulp`: unit in the last place, 해당 값 바로 다음 표현 가능한 값과의 차이

```kotlin
println(1.0.nextUp())    // 1.0보다 아주 조금 큰 표현 가능한 값
println(1.0.ulp)         // 1.0 근방에서 표현 가능한 최소 간격
```

이 그룹의 함수는 부동소수점 오차를 정밀하게 다뤄야 하는 수치 알고리즘이 아니면 실무에서 자주 쓰이지 않음.

### 요약

- 상수
  - `PI`, `E`
- 삼각함수
  - `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`
- 쌍곡선 함수
  - `sinh`, `cosh`, `tanh`, `asinh`, `acosh`, `atanh`
- 지수·로그
  - `exp`, `expm1`, `ln`, `ln1p`, `log10`, `log2`, `log`
- 거듭제곱·제곱근
  - `pow`, `sqrt`, `cbrt`, `hypot`
- 절댓값·부호
  - `abs`, `absoluteValue`, `sign`
- 반올림
  - `round`, `ceil`, `floor`, `truncate`, `roundToInt`, `roundToLong`
- 최솟값·최댓값
  - `min`, `max`
- 부동소수점 정밀 조작
  - `IEEErem`(JVM/Native), `nextUp`, `nextDown`, `nextTowards`, `ulp`

---

## 시간 측정과 Duration

## Kotlin 시간 측정 (kotlin.time)

> 원문: https://kotlinlang.org/docs/time-measurement.html

### 개요

`kotlin.time` 패키지: 시간 간격을 표현하고 측정하기 위한 멀티플랫폼 API 제공.

- `java.time.Duration`이나 `Thread.sleep` 앞뒤로 `System.currentTimeMillis()`를 찍는 방식과 차이
  - `Duration`은 단위를 명시적으로 다루는 값 타입
  - `TimeSource`는 벽시계(wall clock)가 아니라 단조 증가(monotonic) 시계를 기본으로 삼음 → 시스템 시간 변경에 영향받지 않는 경과 시간 측정 지원

핵심 구성 요소:

- `Duration` — 시간의 양(길이)을 나타내는 값 타입
- `TimeSource` / `TimeMark` — 특정 시점을 찍고 그 사이 경과 시간을 재는 도구
- `measureTime` / `measureTimedValue` — 코드 블록 실행 시간을 재는 헬퍼 함수

### Duration 생성하기

`Int`, `Long`, `Double`에 확장 프로퍼티가 붙어 있어 숫자 리터럴로 바로 `Duration` 생성 가능.

```kotlin
val timeout = 5.seconds
val ttl = 10.minutes
val retryDelay = 500.milliseconds
val neverExpires = Double.POSITIVE_INFINITY.days
```

단위를 변수로 다뤄야 할 때는 `toDuration(unit: DurationUnit)` 확장 함수 사용.

```kotlin
fun waitFor(amount: Long, unit: DurationUnit): Duration = amount.toDuration(unit)
```

`DurationUnit`: `NANOSECONDS`부터 `DAYS`까지 7개 값(`NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`)을 가진 열거형.

### Duration 연산

`Duration`끼리, 또는 `Duration`과 숫자 사이에 사칙연산과 비교 연산 정의됨.

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

내부적으로 값이 `Long` 하나에 인코딩됨 → `Duration`은 `@JvmInline value class`로 구현되어 박싱 오버헤드가 거의 없음.

### 다른 형태로 변환하기

#### 문자열로

```kotlin
5887.milliseconds.toString()                       // "5.887s"
5887.milliseconds.toString(DurationUnit.SECONDS, 2) // "5.89s" (소수점 자릿수 지정)
86420.seconds.toIsoString()                         // "PT24H0M20S" (ISO-8601)
```

#### 숫자로

- `inWhole*` 프로퍼티: 정수(Long)로 잘라낸 값 반환
- `to*(unit)` 함수: 다른 단위로 변환한 값 반환

```kotlin
val d = 30.minutes
d.inWholeSeconds                        // 1800
270.seconds.toDouble(DurationUnit.MINUTES)  // 4.5
d.toLong(DurationUnit.MILLISECONDS)
d.toInt(DurationUnit.SECONDS)
```

#### 구성 요소로 분해하기

`toComponents`는 시/분/초/나노초 등으로 쪼개서 람다에 넘김 → 세밀한 단위까지 다 받을 필요 없으면 `_`로 무시.

```kotlin
val d = 30.minutes
val label = d.toComponents { hours, minutes, _, _ -> "${hours}h ${minutes}m" }
// "0h 30m"
```

### 코드 실행 시간 측정: measureTime / measureTimedValue

- 반환값이 필요 없으면 `measureTime`
- 블록의 결과와 소요 시간을 함께 얻고 싶으면 `measureTimedValue`

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

`measureTimedValue`가 돌려주는 `TimedValue<T>`는 `value`와 `duration` 두 필드를 가진 데이터 클래스 → 구조 분해로 바로 꺼내 쓸 수 있음.

### TimeSource와 TimeMark

`measureTime` 같은 편의 함수만으로 부족할 때, 즉 코드 블록으로 감싸기 어려운 두 시점 사이의 간격을 재고 싶을 때는 `TimeSource`를 직접 다룸. `TimeSource.markNow()`로 현재 시점을 찍은 `TimeMark`를 얻고, 두 마크를 빼면 그 사이의 `Duration`이 나옴.

```kotlin
val clock = TimeSource.Monotonic
val start = clock.markNow()

doSomeWork()

val elapsed: Duration = start.elapsedNow()
```

두 마크를 직접 비교하거나 뺄 수도 있음.

```kotlin
val mark1 = clock.markNow()
Thread.sleep(500)
val mark2 = clock.markNow()

val diff = mark2 - mark1   // Duration
mark2 > mark1              // true
```

#### 데드라인/타임아웃 체크

`TimeMark`에 `Duration`을 더하면 미래 시점을 나타내는 새 `TimeMark`가 됨. `hasPassedNow()` / `hasNotPassedNow()`로 그 시점이 지났는지 바로 확인 가능 → 별도로 "지금 시각과 비교"하는 코드를 짤 필요 없음.

```kotlin
val deadline = clock.markNow() + 5.seconds

if (deadline.hasPassedNow()) {
    // 타임아웃 처리
}
```

#### 기본 TimeSource

`TimeSource.Monotonic`은 플랫폼마다 실제로는 다음을 사용.

- JVM: `System.nanoTime()`
- JS(Node): `process.hrtime()`, JS(브라우저): `performance.now()`
- Native: `std::chrono::steady_clock` 계열

시스템 시각이 NTP 보정 등으로 바뀌어도 영향받지 않는 단조 시계 → 경과 시간 측정에는 `System.currentTimeMillis()`류보다 이쪽이 항상 안전.

### 커스텀 TimeSource 만들기

테스트나 특수 플랫폼 API(예: 안드로이드 `SystemClock`)를 시간 소스로 쓰려면 `AbstractLongTimeSource`를 상속해 `read()`만 구현하면 됨.

```kotlin
object AndroidElapsedTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
    override fun read(): Long = SystemClock.elapsedRealtimeNanos()
}

val elapsed = AndroidElapsedTimeSource.measureTime {
    doHeavyWork()
}
```

테스트에서는 시간이 실제로 흐르길 기다릴 필요 없이 `TestTimeSource`로 읽음값을 직접 조작 → 결정론적인 테스트 작성 가능.

```kotlin
val testSource = TestTimeSource()
val mark = testSource.markNow()
testSource += 5.seconds   // 시간을 임의로 흘려보냄
println(mark.elapsedNow()) // 5s
```

### 다음 단계

- `kotlin.time.Instant`, `Clock`으로 벽시계 시각(달력 날짜) 다루기
- Java 상호운용: `Duration.toJavaDuration()` / `java.time.Duration.toKotlinDuration()`

---

## kotlin.time 패키지 API 요약

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/

### 주요 타입

- `Duration`
  - `@JvmInline value class`, 시간 길이를 표현, `Comparable<Duration>`
- `DurationUnit`
  - 시간 단위 열거형(`NANOSECONDS` ~ `DAYS`)
- `TimeSource`
  - 시간 소스 인터페이스, `markNow()`로 `TimeMark` 생성
- `TimeMark`
  - 특정 시점 하나, `elapsedNow()`로 경과 시간 조회
- `ComparableTimeMark`
  - 같은 `TimeSource.WithComparableMarks`에서 나온 마크끼리 뺄셈·비교 가능
- `TimedValue<T>`
  - `data class(val value: T, val duration: Duration)`, `measureTimedValue` 결과 타입
- `AbstractLongTimeSource`
  - `Long` 단위로 시각을 읽는 커스텀 `TimeSource`의 베이스 클래스
- `TestTimeSource`
  - 읽음값을 코드로 직접 조작할 수 있는 테스트용 `TimeSource`
- `Instant`
  - (Kotlin 2.3+) 벽시계상의 특정 순간, `Comparable<Instant>`
- `Clock`
  - (Kotlin 2.3+) `Instant`를 만들어내는 소스

`Duration`은 값 클래스 → 런타임에는 대개 `Long` 하나로 표현되어 박싱 없이 동작. 반대로 `TimeMark`/`Instant`는 인터페이스·클래스라 소스에 바인딩된 실제 객체. 이 둘을 혼동하지 않는 것이 중요 — `Duration`은 "길이", `TimeMark`/`Instant`는 "시점".

### 주요 함수

```kotlin
inline fun measureTime(block: () -> Unit): Duration
inline fun TimeSource.measureTime(block: () -> Unit): Duration

inline fun <T> measureTimedValue(block: () -> T): TimedValue<T>
inline fun <T> TimeSource.measureTimedValue(block: () -> T): TimedValue<T>

fun Int.toDuration(unit: DurationUnit): Duration
fun Long.toDuration(unit: DurationUnit): Duration
fun Double.toDuration(unit: DurationUnit): Duration
```

`measureTime`/`measureTimedValue`는 수신자 없이 쓰면 내부적으로 `TimeSource.Monotonic` 사용, `TimeSource` 위의 확장 함수 형태로 쓰면 그 소스를 그대로 사용. 둘 다 `inline` 함수라서 람다 캡처에 따른 오버헤드 없음.

#### Java/JS 상호운용 변환 함수

```kotlin
fun Duration.toJavaDuration(): java.time.Duration
fun java.time.Duration.toKotlinDuration(): Duration

fun DurationUnit.toTimeUnit(): java.util.concurrent.TimeUnit
fun TimeUnit.toDurationUnit(): DurationUnit

fun Instant.toJavaInstant(): java.time.Instant
fun java.time.Instant.toKotlinInstant(): Instant
```

기존에 `java.time.Duration`이나 `TimeUnit`을 받는 Java API와 연동할 때, `kotlin.time.Duration`으로 로직을 짜고 API 경계에서만 위 변환 함수로 감싸면 내부 코드는 계속 Kotlin다운 표현을 유지할 수 있음.

### 요약: 이럴 때 무엇을 쓰나

- 코드 한 블록의 실행 시간만 재면 됨 → `measureTime` / `measureTimedValue`
- 블록으로 감싸기 어려운 두 시점 사이 간격, 혹은 데드라인 체크 → `TimeSource.markNow()` + `TimeMark`
- 설정값(타임아웃, TTL 등)을 표현하는 상수 → `Duration` 리터럴(`5.seconds` 등)
- 테스트에서 시간 흐름을 직접 통제 → `TestTimeSource`
- Java API와의 경계 → `toJavaDuration()` / `toKotlinDuration()`

---

## 난수 생성 (kotlin.random)

## Kotlin 난수 생성

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.random/

### 개요

`kotlin.random` 패키지: 플랫폼에 종속되지 않는 난수 생성 API 제공.

- JVM에서는 `Random.Default`가 스레드마다 하나씩 만들어지는 `java.util.Random` 인스턴스에 실제 연산을 위임
- 공통 코드(Kotlin Multiplatform)에서도 동일한 API로 난수를 뽑을 수 있다는 점이 핵심
- Kotlin 1.3부터 표준 라이브러리에 포함됨

중심에는 추상 클래스 `Random`이 있고, 정수 범위나 부호 없는 타입에 대한 난수 생성은 대부분 확장 함수로 별도 제공됨.

### Random 추상 클래스

```kotlin
abstract class Random
```

난수 생성 알고리즘이 구현해야 하는 기반 타입. 직접 서브클래싱해서 커스텀 생성기를 만들 수도 있고, 대부분은 기본 제공 인스턴스인 `Random.Default`나 시드 기반 팩토리 함수 사용.

#### 반드시 구현해야 하는 것

```kotlin
abstract fun nextBits(bitCount: Int): Int
```

가장 하위 레벨의 연산 → 지정한 비트 수만큼의 난수 비트 생성. 나머지 `nextInt`, `nextLong` 등은 결국 이 함수를 조합해서 구현된 `open` 함수들. 커스텀 `Random` 구현체를 만들 때 최소한 이 함수만 오버라이드하면 됨.

#### 기본 타입별 난수 함수

- `nextInt()`
  - 반환 범위: `Int` 전체 범위
- `nextInt(until: Int)`
  - 반환 범위: `0 until until`
- `nextInt(from: Int, until: Int)`
  - 반환 범위: `from until until`
- `nextLong()` / `nextLong(until)` / `nextLong(from, until)`
  - 반환 범위: `Int`와 동일한 규칙의 `Long` 버전
- `nextDouble()`
  - 반환 범위: `0.0 <= x < 1.0`
- `nextDouble(until: Double)` / `nextDouble(from, until)`
  - 반환 범위: 지정 범위
- `nextFloat()`
  - 반환 범위: `0.0 <= x < 1.0`
- `nextBoolean()`
  - 반환 범위: `true` / `false`
- `nextBytes(size: Int)`
  - 반환 범위: 길이 `size`의 새 `ByteArray`
- `nextBytes(array: ByteArray)`
  - 반환 범위: 기존 배열을 난수로 채움
- `nextBytes(array, fromIndex, toIndex)`
  - 반환 범위: 배열의 부분 구간만 채움

경계값은 항상 `from` 포함, `until` 미포함(`[from, until)`). 상한만 주는 오버로드(`nextInt(until)` 등)는 `0`부터 시작하는 음수 아닌 값만 반환.

```kotlin
val rnd = Random.Default
val dice = rnd.nextInt(1, 7)          // 1..6
val ratio = rnd.nextDouble()          // 0.0 이상 1.0 미만
val coin = rnd.nextBoolean()
val buf = rnd.nextBytes(16)           // 16바이트 랜덤 배열
```

#### Default 컴패니언 객체

```kotlin
object Default : Random, Serializable
```

별도의 인스턴스를 만들지 않고 바로 쓸 수 있는 전역 난수 생성기. 시드를 고정하지 않았기 때문에 실행할 때마다 다른 결과가 나오며, 대부분의 일반적인 용도에는 이것만으로 충분함.

```kotlin
val n = Random.nextInt(100)  // Random.Default에 대한 정적 호출처럼 사용 가능
```

### 시드로 재현 가능한 생성기 만들기

```kotlin
fun Random(seed: Int): Random
fun Random(seed: Long): Random
```

같은 시드를 넣으면 항상 같은 순서의 난수를 뽑는 생성기 생성 → 테스트에서 결과를 고정하거나, 절차적 생성 콘텐츠를 재현해야 할 때 유용.

```kotlin
val a = Random(42)
val b = Random(42)
println(a.nextInt() == b.nextInt())  // true, 시드가 같으면 동일한 시퀀스
```

### 범위(Range)를 받는 확장 함수

`IntRange`, `LongRange`를 그대로 넘겨 난수를 뽑을 수 있는 확장 함수도 제공됨 → `for` 문에 쓰는 범위 표현을 그대로 재사용할 수 있어 가독성이 좋음.

```kotlin
fun Random.nextInt(range: IntRange): Int
fun Random.nextLong(range: LongRange): Long
```

```kotlin
val roll = Random.nextInt(1..6)
val big = Random.nextLong(0L..1_000_000L)
```

`until`을 상한으로 잡는 오버로드와 달리, `IntRange`/`LongRange`를 넘길 때는 `..`로 양 끝을 포함(closed range)한다는 점에 주의.

### 부호 없는(Unsigned) 타입 지원 (Kotlin 1.5+)

`UInt`, `ULong`에 대해서도 대칭적인 확장 함수 세트 존재.

```kotlin
fun Random.nextUInt(): UInt
fun Random.nextUInt(until: UInt): UInt
fun Random.nextUInt(range: UIntRange): UInt
fun Random.nextUInt(from: UInt, until: UInt): UInt

fun Random.nextULong(): ULong
fun Random.nextULong(until: ULong): ULong
fun Random.nextULong(range: ULongRange): ULong
fun Random.nextULong(from: ULong, until: ULong): ULong
```

```kotlin
val u = Random.nextUInt(100u)
```

부호 없는 바이트 배열을 뽑는 `nextUBytes` 계열도 있으나(`@ExperimentalUnsignedTypes`), 아직 실험적 API → 프로덕션 코드에서는 안정화 여부를 확인하고 쓰는 것이 좋음.

```kotlin
@ExperimentalUnsignedTypes
fun Random.nextUBytes(size: Int): UByteArray
```

### JVM 상호운용

JVM 타깃에서는 Kotlin `Random`과 Java `java.util.Random` 사이를 오가는 변환 함수 제공 → 기존에 `java.util.Random`을 요구하는 라이브러리(API)와 Kotlin 코드를 함께 쓸 때 유용.

```kotlin
fun Random.asJavaRandom(): java.util.Random
fun java.util.Random.asKotlinRandom(): Random
```

```kotlin
val javaRnd: java.util.Random = Random(1).asJavaRandom()
val kotlinRnd: Random = javaRnd.asKotlinRandom()
```

### 정리

- 시드를 신경 쓰지 않는 일반적인 난수는 `Random.Default`(또는 `Random.nextInt(...)`처럼 정적으로 호출)로 충분
- 테스트나 재현성이 필요한 경우 `Random(seed)`로 고정된 생성기 사용
- 범위 기반 API는 `until` 상한 오버로드와 `IntRange`/`LongRange` 오버로드 두 갈래 → 후자는 양 끝 포함이라는 차이를 기억해야 함
- 부호 없는 타입, JVM 상호운용 함수는 필요할 때만 선택적으로 사용하면 됨

---

## 표준 입력 읽기

## Kotlin 표준 입력 읽기

> 원문: https://kotlinlang.org/docs/read-standard-input.html

### 개요

콘솔 애플리케이션에서 사용자 입력을 받으려면 표준 입력(stdin)을 읽어야 함. Kotlin은 `readln()`과 `readlnOrNull()` 두 함수로 이를 지원 → 둘의 차이는 EOF(입력 끝)를 만났을 때 예외를 던지느냐 `null`을 반환하느냐.

### 한 줄 읽기: readln()

`readln()`은 표준 입력에서 한 줄을 문자열로 읽어옴. 가장 기본적이고 자주 쓰는 함수.

```kotlin
println("이름을 입력하세요:")
val name = readln()
println("반갑습니다, $name!")
```

주의할 점: 더 이상 읽을 줄이 없을 때(EOF) `RuntimeException`을 던짐. 표준 입력이 파이프나 리다이렉션으로 유한하게 공급되는 환경(예: 파일을 `<`로 넣거나 파이프로 연결하는 경우)에서 반복문으로 계속 읽다 보면 이 예외를 마주치기 쉬움.

### 안전하게 읽기: readlnOrNull()

`readlnOrNull()`은 `readln()`과 동일하게 동작하지만, EOF에 도달하면 예외 대신 `null` 반환. 입력의 끝을 모르는 상태에서 반복적으로 읽어야 할 때 적합.

```kotlin
val line: String? = readlnOrNull()
if (line == null) {
    println("입력이 더 이상 없습니다.")
}
```

#### 여러 줄을 한 번에 모으기

`generateSequence`와 조합하면 EOF까지의 모든 줄을 하나의 시퀀스(또는 문자열)로 모을 수 있음. 함수 참조 `::readlnOrNull`을 넘겨서, `null`이 나오는 순간 시퀀스가 자연스럽게 끝나도록 만드는 방식.

```kotlin
val allLines = generateSequence(::readlnOrNull).joinToString("\n")
println(allLines)
```

### 문자열을 숫자·불리언으로 변환하기

`readln()`은 항상 `String`을 반환 → 숫자나 불리언으로 다루려면 표준 변환 함수 사용.

```kotlin
val age: Int = readln().toInt()
val price: Double = readln().toDouble()
val flag: Boolean = readln().toBoolean()
```

`toInt()`, `toDouble()`, `toLong()`, `toFloat()`는 입력값이 형식에 맞지 않으면 `NumberFormatException` 등을 던짐. 사용자 입력은 언제든 잘못될 수 있으므로, 예외를 원하지 않는다면 `OrNull` 계열 함수를 쓰는 편이 안전.

```kotlin
val maybeAge: Int? = readln().toIntOrNull()
val age = maybeAge ?: run {
    println("숫자가 아닙니다. 기본값 0을 사용합니다.")
    0
}
```

이렇게 `toIntOrNull()`과 엘비스 연산자(`?:`)를 함께 쓰면 예외 처리 없이도 잘못된 입력일 때 쓸 기본값을 깔끔하게 지정할 수 있음.

### 한 줄에 여러 값 파싱하기

공백이나 쉼표 등 구분자로 여러 값이 한 줄에 들어오는 경우, `split()`으로 나눈 뒤 `map`으로 원하는 타입으로 변환.

```kotlin
// 입력: "10 20 30"
val numbers = readln().split(' ').map { it.toInt() }
println(numbers)  // [10, 20, 30]

// 입력: "1.5,2.5,3.5"
val doubles = readln().split(',').map { it.toDouble() }
println(doubles)  // [1.5, 2.5, 3.5]
```

연속된 공백까지 처리하려면 정규식을 구분자로 넘길 수도 있음.

```kotlin
val tokens = readln().trim().split(Regex("\\s+"))
```

### 입력 방식 정리

- `readln()`
  - EOF에서의 동작: 예외를 던짐
  - 안전 여부: 아니오
- `readlnOrNull()`
  - EOF에서의 동작: `null` 반환
  - 안전 여부: 예
- `String.toInt()` 등
  - EOF에서의 동작: 형식 오류 시 예외
  - 안전 여부: 아니오
- `String.toIntOrNull()` 등
  - EOF에서의 동작: 형식 오류 시 `null` 반환
  - 안전 여부: 예

### 실전 패턴: 줄 수를 모른 채 끝까지 읽기

경쟁 프로그래밍이나 파이프 입력 처리처럼 몇 줄이 들어올지 모르는 상황에서는 `readlnOrNull()`을 루프 조건으로 사용하는 패턴이 유용.

```kotlin
while (true) {
    val line = readlnOrNull() ?: break
    if (line.isEmpty()) continue
    println("처리: $line")
}
```

### 참고

- JVM 환경에서 더 세밀한 입력 제어(토큰 단위 파싱, 버퍼링 등)가 필요하면 `java.util.Scanner`나 `System.\`in\`.bufferedReader()`도 대안이 될 수 있음
- 표준 출력에는 `print()`, `println()` 사용, 서식이 필요하면 문자열 템플릿(`"$name"`, `"${expr}"`)을 활용

---

## 파일 입출력 확장 함수 (kotlin.io)

## kotlin.io 패키지 개요

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

Kotlin 표준 라이브러리는 `java.io.File`, `InputStream`, `Reader` 같은 JDK 타입에 확장 함수를 덧붙여서, 스트림을 직접 열고 닫는 보일러플레이트 없이 파일을 다루게 해줌. `kotlin.io` 패키지가 이 확장 함수들의 모음이며, 크게 다음 영역으로 나뉨.

- 텍스트/바이트 읽기: `readText`, `readLines`, `readBytes`, `forEachLine`, `useLines`
- 텍스트/바이트 쓰기: `writeText`, `appendText`, `writeBytes`, `appendBytes`
- 스트림·리더/라이터 생성: `inputStream`, `outputStream`, `bufferedReader`, `bufferedWriter`, `reader`, `writer`
- 자원 관리: `use`, `buffered`
- 파일 시스템 조작: `copyTo`, `copyRecursively`, `deleteRecursively`, `walk`, `resolve`

대부분 JVM 전용(파일 시스템 접근이 필요하므로), `print`/`println`/`readln` 등 콘솔 입출력 일부만 공통(Common) 소스셋에서도 제공됨.

### 왜 확장 함수로 제공하는가

Java에서는 파일 하나를 문자열로 읽으려면 `FileReader` → `BufferedReader` → 한 줄씩 읽어 `StringBuilder`에 누적 → `close()`까지 직접 작성해야 함. Kotlin은 이 과정을 `File.readText()` 한 줄로 압축함. 대신 그만큼 "작은 파일에서만 안전", "자동으로 닫아준다" 같은 전제가 숨어 있으므로, 함수마다 이 전제를 알고 써야 함.

```kotlin
// Java 스타일 없이 바로 문자열 획득
val config = File("app.properties").readText()
```

## 텍스트 읽기: readText, readLines

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/read-text.html

### 시그니처

```kotlin
fun File.readText(charset: Charset = Charsets.UTF_8): String
fun File.readLines(charset: Charset = Charsets.UTF_8): List<String>
fun Reader.readText(): String
fun Reader.readLines(): List<String>
fun URL.readText(charset: Charset = Charsets.UTF_8): String
```

### 핵심 동작

- 파일(또는 `Reader`, `URL`)의 전체 내용을 한 번에 메모리로 읽어 `String` 또는 `List<String>`으로 반환
- 기본 문자셋은 UTF-8, 필요하면 `Charsets.EUC_KR` 등으로 변경 가능
- `Reader.readText()`는 리더를 닫지 않음 → 호출자가 직접 닫거나 `use`로 감싸야 함

```kotlin
val text = File("notes.txt").readText()
val lines: List<String> = File("notes.txt").readLines()

File("notes.txt").reader().use { reader ->
    println(reader.readText())  // Reader 버전은 자동으로 닫히지 않으므로 use로 감싼다
}
```

### 주의: 큰 파일에는 부적합

공식 문서는 `readText`/`readLines`가 파일 전체를 메모리에 올리기 때문에 큰 파일에는 권장하지 않는다고 명시함. 내부적으로 약 2GB 크기 제한도 있음. 로그 파일이나 대용량 데이터를 다룰 때는 다음 절의 `forEachLine`/`useLines`처럼 스트리밍 방식으로 처리해야 함.

## 한 줄씩 처리하기: forEachLine, useLines, lineSequence

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/use-lines.html

### useLines

```kotlin
inline fun <T> File.useLines(
    charset: Charset = Charsets.UTF_8,
    block: (Sequence<String>) -> T
): T
```

- 파일의 모든 줄을 `Sequence<String>`으로 만들어 `block`에 넘김 → `block` 실행이 끝나면 내부 리더를 자동으로 닫음
- `Sequence`이므로 지연(lazy) 평가됨 — `block` 안에서 실제로 순회할 때만 한 줄씩 읽음. 전체를 리스트로 만드는 `readLines`와 달리 메모리에 전체 내용을 올리지 않음
- 자원 해제를 리더가 아니라 함수가 책임진다는 점에서 "대여(loan) 패턴"이라 부름. `try-with-resources`의 Kotlin식 대응
- `block`의 반환값이 그대로 `useLines`의 반환값이 됨. `block` 밖으로 시퀀스 자체를 빼돌리면 안 됨 — 그 시점엔 이미 리더가 닫혀 있어서 순회 시 예외가 남

```kotlin
val wordCount: Int = File("data.txt").useLines { lines ->
    lines.flatMap { it.split(" ") }.count()
}
```

### forEachLine

```kotlin
fun File.forEachLine(charset: Charset = Charsets.UTF_8, action: (String) -> Unit)
```

- `useLines`와 마찬가지로 한 줄씩 읽고 자동으로 자원을 닫음. 반환값이 없는 대신 각 줄에 부수 효과(side effect)를 주는 용도

```kotlin
var errorCount = 0
File("app.log").forEachLine { line ->
    if ("ERROR" in line) errorCount++
}
```

### lineSequence

```kotlin
fun BufferedReader.lineSequence(): Sequence<String>
```

- `BufferedReader`에서 직접 시퀀스를 얻고 싶을 때 사용. 다만 이 함수 자체는 리더를 닫아주지 않으므로, 대개 `use { it.lineSequence()... }` 형태로 감싸 씀

### 선택 기준

- 파일이 작고 전체 내용이 한 번에 필요
  - 추천 함수: `readText`, `readLines`
- 큰 파일을 한 줄씩 순회하며 결과값을 만들어야 함
  - 추천 함수: `useLines`
- 큰 파일을 한 줄씩 순회하며 부수 효과만 필요
  - 추천 함수: `forEachLine`

## 텍스트/바이트 쓰기

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

```kotlin
fun File.writeText(text: String, charset: Charset = Charsets.UTF_8)
fun File.appendText(text: String, charset: Charset = Charsets.UTF_8)
fun File.writeBytes(array: ByteArray)
fun File.appendBytes(array: ByteArray)
```

- `writeText`/`writeBytes`는 파일이 있으면 덮어씀. `appendText`/`appendBytes`는 뒤에 이어 붙임
- 둘 다 내부에서 스트림을 열고 닫는 과정을 알아서 처리하므로 `use`가 필요 없음

```kotlin
val out = File("result.txt")
out.writeText("첫 줄\n")
out.appendText("두 번째 줄\n")
```

## 스트림·리더/라이터 생성과 자원 관리

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

### 스트림/리더 생성 함수

```kotlin
fun File.inputStream(): FileInputStream
fun File.outputStream(): FileOutputStream
fun File.bufferedReader(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedReader
fun File.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter
fun InputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream
fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long
```

- `readText`/`writeText`류가 "한 번에 전부"를 다룬다면, 이 계열 함수는 직접 스트림을 열어 세밀하게 제어할 때 씀. 대신 닫는 책임은 호출자에게 있음
- `bufferedReader`/`bufferedWriter`는 내부적으로 `InputStreamReader`/`OutputStreamWriter`를 `BufferedReader`/`BufferedWriter`로 감싸주는 편의 함수

### use — 자동 자원 해제

```kotlin
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R
```

- `Closeable`을 구현한 모든 대상(스트림, 리더, 라이터)에 붙는 확장 함수 → `block` 실행 후 예외 발생 여부와 무관하게 항상 `close()`를 호출. Java의 `try-with-resources`와 동일한 역할
- `useLines`, `forEachLine` 같은 함수들은 내부적으로 이 `use` 패턴 위에 구현되어 있음

```kotlin
File("big.bin").outputStream().use { out ->
    File("source.bin").inputStream().use { input ->
        input.copyTo(out)
    }
}
```

## 파일 시스템 조작: 복사, 삭제, 순회

> 원문: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

### 복사와 삭제

```kotlin
fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File
fun File.copyRecursively(target: File, overwrite: Boolean = false,
                         onError: (File, IOException) -> OnErrorAction = { _, ex -> throw ex }): Boolean
fun File.deleteRecursively(): Boolean
```

- `copyTo`는 파일 하나, `copyRecursively`는 디렉터리 전체를 대상으로 함. `onError` 콜백에서 `OnErrorAction.SKIP`/`ABORT`/`CONTINUE`를 반환해 오류 발생 시 동작 제어 가능
- `deleteRecursively()`는 디렉터리와 그 하위 파일을 모두 지움, 일부라도 실패하면 `false` 반환(예외를 던지지 않음)

### 디렉터리 순회

```kotlin
fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk
fun File.walkTopDown(): FileTreeWalk   // 부모를 먼저 방문
fun File.walkBottomUp(): FileTreeWalk // 자식을 먼저 방문
```

- `FileTreeWalk` 자체가 `Sequence<File>`처럼 동작해 `filter`, `map`, `forEach` 등을 그대로 이어 쓸 수 있음
- 삭제 작업은 자식이 먼저 없어져야 하므로 보통 `walkBottomUp()`을 씀

```kotlin
File("build").walkBottomUp()
    .filter { it.isFile && it.extension == "tmp" }
    .forEach { it.delete() }
```

### 경로 조작

```kotlin
fun File.resolve(relative: String): File       // 하위 경로 결합
fun File.resolveSibling(relative: String): File // 같은 부모 아래 다른 경로
fun File.relativeTo(base: File): File           // base 기준 상대 경로
```

```kotlin
val dir = File("/home/user/project")
val configFile = dir.resolve("config/app.yml")
println(configFile.relativeTo(dir))  // config/app.yml
```

## 정리

- 한 번에 전부 처리(작은 파일): `readText`, `readLines`, `writeText`, `appendText`
- 한 줄씩 스트리밍 처리(큰 파일): `useLines`(값 반환), `forEachLine`(부수 효과)
- 세밀한 제어가 필요하면 `inputStream`/`outputStream`/`bufferedReader` + `use`로 직접 자원을 관리
- 디렉터리 단위 작업은 `copyRecursively`, `deleteRecursively`, `walkTopDown`/`walkBottomUp`
- 거의 모든 텍스트 함수는 기본 문자셋이 UTF-8이며, 자동으로 닫아주지 않는 함수(`Reader.readText()`, `bufferedReader()` 등)는 반드시 `use`로 감싸야 자원 누수를 막을 수 있음
