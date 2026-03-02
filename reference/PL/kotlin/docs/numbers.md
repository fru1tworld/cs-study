# Kotlin 숫자

## 개요

이 페이지는 Kotlin의 내장 숫자 타입, 리터럴 상수, 연산, 그리고 부동 소수점 숫자에 대한 특별한 고려 사항을 다룹니다.

---

## 정수 타입

Kotlin은 서로 다른 크기와 범위를 가진 네 가지 부호 있는 정수 타입을 제공합니다:

| 타입 | 크기 (비트) | 최소값 | 최대값 |
|------|-----------|-------|-------|
| `Byte` | 8 | -128 | 127 |
| `Short` | 16 | -32,768 | 32,767 |
| `Int` | 32 | -2,147,483,648 (-2^31) | 2,147,483,647 (2^31 - 1) |
| `Long` | 64 | -9,223,372,036,854,775,808 (-2^63) | 9,223,372,036,854,775,807 (2^63 - 1) |

**타입 추론 규칙:**
- 기본 정수 타입은 `Int` (값이 맞는 경우)
- `Int` 범위 초과 시 자동으로 `Long`
- 명시적으로 `Long`을 지정하려면 `L` 접미사 추가
- 필요한 경우 `Byte` 또는 `Short`를 명시적으로 선언

```kotlin
val one = 1 // Int
val threeBillion = 3000000000 // Long
val oneLong = 1L // Long
val oneByte: Byte = 1
```

---

## 부동 소수점 타입

Kotlin은 [IEEE 754 표준](https://en.wikipedia.org/wiki/IEEE_754)을 준수하는 `Float`와 `Double`을 제공합니다.

| 타입 | 크기 (비트) | 유효 비트 | 지수 비트 | 십진수 자릿수 |
|------|-----------|----------|----------|-------------|
| `Float` | 32 | 24 | 8 | 6-7 |
| `Double` | 64 | 53 | 11 | 15-16 |

**초기화 규칙:**
- 기본 부동 소수점 타입은 `Double`
- 명시적 `Float`를 위해 `f` 또는 `F` 접미사 추가

```kotlin
val pi = 3.14 // Double
val one: Double = 1 // 오류: Int가 추론됨
val oneDouble = 1.0 // Double
val eFloat = 2.7182818284f // Float, 2.7182817로 반올림
```

**암시적 확장 변환 없음:**

```kotlin
fun printDouble(x: Double) { print(x) }
val x = 1.0
val xInt = 1
val xFloat = 1.0f
printDouble(x) // OK
printDouble(xInt) // 오류: 인수 타입 불일치
printDouble(xFloat) // 오류: 인수 타입 불일치
```

---

## 숫자 리터럴 상수

### 정수 리터럴
- **10진수:** `123`
- **Long (대문자 L 접미사):** `123L`
- **16진수:** `0x0F`
- **2진수:** `0b00001011`
- **8진수:** Kotlin에서 지원하지 않음

### 부동 소수점 리터럴
- **Double (기본):** `123.5`, `123.5e10`
- **Float (f/F 접미사):** `123.5f`

### 가독성을 위한 밑줄

```kotlin
val oneMillion = 1_000_000
val creditCardNumber = 1234_5678_9012_3456L
val socialSecurityNumber = 999_99_9999L
val hexBytes = 0xFF_EC_DE_5E
val bytes = 0b11010010_01101001_10010100_10010010
val bigFractional = 1_234_567.7182818284
```

---

## JVM에서의 박싱과 캐싱

JVM은 **-128에서 127** 사이의 박싱된 정수를 캐시합니다. 이러한 캐시된 값에 대한 참조는 **참조적으로 동일**합니다.

```kotlin
val a: Int = 100
val boxedA: Int? = a
val anotherBoxedA: Int? = a
println(boxedA === anotherBoxedA) // true (캐시됨)
```

이 범위 밖의 숫자는 **구조적으로 동일하지만 참조적으로 동일하지 않습니다:**

```kotlin
val b: Int = 10000
val boxedB: Int? = b
val anotherBoxedB: Int? = b
println(boxedB === anotherBoxedB) // false
println(boxedB == anotherBoxedB) // true
```

**권장 사항:** 숫자 비교에는 참조 동등성(`===`) 대신 구조적 동등성(`==`)을 사용하세요.

---

## 명시적 숫자 변환

숫자 타입은 서로의 하위 타입이 아니므로 명시적 변환이 필요합니다:

```kotlin
val byte: Byte = 1
val intConvertedByte: Int = byte.toInt()
```

**사용 가능한 변환 함수:**
- `toByte(): Byte` (Float/Double에서는 폐기됨)
- `toShort(): Short`
- `toInt(): Int`
- `toLong(): Long`
- `toFloat(): Float`
- `toDouble(): Double`

**연산자를 사용한 자동 변환:**

```kotlin
val l = 1L + 3 // Long + Int => Long
println(l is Long) // true
```

### 암시적 변환에 반대하는 이유

암시적 변환은 예기치 않은 동작과 동등성/동일성 손실로 이어질 수 있습니다:

```kotlin
// 가상 (컴파일되지 않음):
val a: Int? = 1 // java.lang.Integer
val b: Long? = a // java.lang.Long이 될 것
print(b == a) // 예기치 않게 "false"를 출력할 것
```

---

## 숫자 연산

Kotlin은 표준 산술 연산자를 지원합니다: `+`, `-`, `*`, `/`, `%`

```kotlin
println(1 + 2) // 3
println(2_500_000_000L - 1L)
println(3.14 * 2.71)
println(10.0 / 3) // 3.3333...
```

### 정수 나눗셈

정수 나눗셈은 항상 정수를 반환합니다; 소수 부분은 버려집니다:

```kotlin
val x = 5 / 2
println(x == 2) // true
println(x == 2.5) // 오류: Int와 Double을 비교할 수 없음
```

**소수 결과를 위해 명시적으로 변환:**

```kotlin
val x = 5 / 2.toDouble()
println(x == 2.5) // true
```

### 비트 연산

`Int`와 `Long`에서만 사용 가능:

```kotlin
val x = 1
val xShiftedLeft = x shl 2 // 4
val xAnd = x and 0x000FF000 // 0
```

**전체 목록:**
- `shl(bits)` - 부호 있는 왼쪽 시프트
- `shr(bits)` - 부호 있는 오른쪽 시프트
- `ushr(bits)` - 부호 없는 오른쪽 시프트
- `and(bits)` - 비트 AND
- `or(bits)` - 비트 OR
- `xor(bits)` - 비트 XOR
- `inv()` - 비트 반전

---

## 부동 소수점 숫자 비교

### Float/Double로 정적 타입 지정된 경우

[IEEE 754 표준](https://en.wikipedia.org/wiki/IEEE_754)을 따릅니다:
- `NaN == NaN` -> `false`
- `0.0 == -0.0` -> `true`

### 동적으로 타입 지정된 경우 (예: Any, Comparable)

`equals()`와 `compareTo()`를 사용합니다:
- `NaN == NaN` -> `true` (자기 자신과 동일)
- `NaN > POSITIVE_INFINITY` -> `true`
- `-0.0 < 0.0` -> `true`

**예제:**

```kotlin
// Double로 정적 타입 지정
println(Double.NaN == Double.NaN) // false

// 부동 소수점으로 정적 타입 지정되지 않음
println(listOf(Double.NaN) == listOf(Double.NaN)) // true

println(listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -0.0).sorted())
// [-0.0, 0.0, Infinity, NaN]
```

---

## 관련 문서
- [타입 개요](types-overview.html)
- [부호 없는 정수 타입](unsigned-integer-types.html)
- [연산자 오버로딩](operator-overloading.html)
