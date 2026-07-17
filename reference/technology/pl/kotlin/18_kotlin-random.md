# 난수 생성 (kotlin.random)

# Kotlin 난수 생성

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.random/

## 개요

`kotlin.random` 패키지는 플랫폼에 종속되지 않는 난수 생성 API를 제공합니다. JVM에서는 `Random.Default`가 스레드마다 하나씩 만들어지는 `java.util.Random` 인스턴스에 실제 연산을 위임하는 방식으로 동작하지만, 공통 코드(Kotlin Multiplatform)에서도 동일한 API로 난수를 뽑을 수 있다는 점이 핵심입니다. Kotlin 1.3부터 표준 라이브러리에 포함되었습니다.

중심에는 추상 클래스 `Random`이 있고, 정수 범위나 부호 없는 타입에 대한 난수 생성은 대부분 확장 함수로 별도 제공됩니다.

## Random 추상 클래스

```kotlin
abstract class Random
```

난수 생성 알고리즘이 구현해야 하는 기반 타입입니다. 직접 서브클래싱해서 커스텀 생성기를 만들 수도 있고, 대부분은 기본 제공 인스턴스인 `Random.Default`나 시드 기반 팩토리 함수를 사용합니다.

### 반드시 구현해야 하는 것

```kotlin
abstract fun nextBits(bitCount: Int): Int
```

가장 하위 레벨의 연산으로, 지정한 비트 수만큼의 난수 비트를 생성합니다. 나머지 `nextInt`, `nextLong` 등은 결국 이 함수를 조합해서 구현된 `open` 함수들입니다. 커스텀 `Random` 구현체를 만들 때 최소한 이 함수만 오버라이드하면 됩니다.

### 기본 타입별 난수 함수

| 함수 | 반환 범위 |
|---|---|
| `nextInt()` | `Int` 전체 범위 |
| `nextInt(until: Int)` | `0 until until` |
| `nextInt(from: Int, until: Int)` | `from until until` |
| `nextLong()` / `nextLong(until)` / `nextLong(from, until)` | `Int`과 동일한 규칙의 `Long` 버전 |
| `nextDouble()` | `0.0 <= x < 1.0` |
| `nextDouble(until: Double)` / `nextDouble(from, until)` | 지정 범위 |
| `nextFloat()` | `0.0 <= x < 1.0` |
| `nextBoolean()` | `true` / `false` |
| `nextBytes(size: Int)` | 길이 `size`의 새 `ByteArray` |
| `nextBytes(array: ByteArray)` | 기존 배열을 난수로 채움 |
| `nextBytes(array, fromIndex, toIndex)` | 배열의 부분 구간만 채움 |

경계값은 항상 `from` 포함, `until` 미포함(`[from, until)`)입니다. 상한만 주는 오버로드(`nextInt(until)` 등)는 `0`부터 시작하는 음수 아닌 값만 반환합니다.

```kotlin
val rnd = Random.Default
val dice = rnd.nextInt(1, 7)          // 1..6
val ratio = rnd.nextDouble()          // 0.0 이상 1.0 미만
val coin = rnd.nextBoolean()
val buf = rnd.nextBytes(16)           // 16바이트 랜덤 배열
```

### Default 컴패니언 객체

```kotlin
object Default : Random, Serializable
```

별도의 인스턴스를 만들지 않고 바로 쓸 수 있는 전역 난수 생성기입니다. 시드를 고정하지 않았기 때문에 실행할 때마다 다른 결과가 나오며, 대부분의 일반적인 용도에는 이것만으로 충분합니다.

```kotlin
val n = Random.nextInt(100)  // Random.Default에 대한 정적 호출처럼 사용 가능
```

## 시드로 재현 가능한 생성기 만들기

```kotlin
fun Random(seed: Int): Random
fun Random(seed: Long): Random
```

같은 시드를 넣으면 항상 같은 순서의 난수를 뽑는 생성기를 만듭니다. 테스트에서 결과를 고정하거나, 절차적 생성 콘텐츠를 재현해야 할 때 유용합니다.

```kotlin
val a = Random(42)
val b = Random(42)
println(a.nextInt() == b.nextInt())  // true, 시드가 같으면 동일한 시퀀스
```

## 범위(Range)를 받는 확장 함수

`IntRange`, `LongRange`를 그대로 넘겨 난수를 뽑을 수 있는 확장 함수도 제공됩니다. `for` 문에 쓰는 범위 표현을 그대로 재사용할 수 있어 가독성이 좋습니다.

```kotlin
fun Random.nextInt(range: IntRange): Int
fun Random.nextLong(range: LongRange): Long
```

```kotlin
val roll = Random.nextInt(1..6)
val big = Random.nextLong(0L..1_000_000L)
```

`until`을 상한으로 잡는 오버로드와 달리, `IntRange`/`LongRange`를 넘길 때는 `..`로 양 끝을 포함(closed range)한다는 점에 주의해야 합니다.

## 부호 없는(Unsigned) 타입 지원 (Kotlin 1.5+)

`UInt`, `ULong`에 대해서도 대칭적인 확장 함수 세트가 있습니다.

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

부호 없는 바이트 배열을 뽑는 `nextUBytes` 계열도 있으나(`@ExperimentalUnsignedTypes`), 아직 실험적 API이므로 프로덕션 코드에서는 안정화 여부를 확인하고 쓰는 것이 좋습니다.

```kotlin
@ExperimentalUnsignedTypes
fun Random.nextUBytes(size: Int): UByteArray
```

## JVM 상호운용

JVM 타깃에서는 Kotlin `Random`과 Java `java.util.Random` 사이를 오가는 변환 함수를 제공합니다. 기존에 `java.util.Random`을 요구하는 라이브러리(API)와 Kotlin 코드를 함께 쓸 때 유용합니다.

```kotlin
fun Random.asJavaRandom(): java.util.Random
fun java.util.Random.asKotlinRandom(): Random
```

```kotlin
val javaRnd: java.util.Random = Random(1).asJavaRandom()
val kotlinRnd: Random = javaRnd.asKotlinRandom()
```

## 정리

- 시드를 신경 쓰지 않는 일반적인 난수는 `Random.Default`(또는 `Random.nextInt(...)`처럼 정적으로 호출)로 충분합니다.
- 테스트나 재현성이 필요한 경우 `Random(seed)`로 고정된 생성기를 사용합니다.
- 범위 기반 API는 `until` 상한 오버로드와 `IntRange`/`LongRange` 오버로드 두 갈래가 있으며, 후자는 양 끝 포함이라는 차이를 기억해야 합니다.
- 부호 없는 타입, JVM 상호운용 함수는 필요할 때만 선택적으로 사용하면 됩니다.
