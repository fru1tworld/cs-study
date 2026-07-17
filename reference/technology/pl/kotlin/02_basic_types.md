# 기본 타입

# Kotlin 기본 타입 개요

> **원문:** https://kotlinlang.org/docs/basic-types.html

Kotlin의 기본 타입은 다음과 같은 범주로 구성됩니다:

## 숫자 타입

### 정수 타입
- `Byte` - 8비트
- `Short` - 16비트
- `Int` - 32비트
- `Long` - 64비트

### 부호 없는 정수 타입
- `UByte` - 8비트
- `UShort` - 16비트
- `UInt` - 32비트
- `ULong` - 64비트

### 부동 소수점 타입
- `Float` - 32비트
- `Double` - 64비트

## 불리언 타입
- `Boolean` - `true` 또는 `false`

## 문자 타입
- `Char` - 단일 문자

## 문자열 타입
- `String` - 문자 시퀀스

---

자세한 내용은 각 타입의 문서를 참조하세요:
- [숫자](numbers.md)
- [불리언](booleans.md)
- [문자](characters.md)
- [문자열](strings.md)
- [배열](arrays.md)
- [부호 없는 정수 타입](unsigned-integer-types.md)
- [타입 캐스트](typecasts.md)

---

# Kotlin 불리언

> **원문:** https://kotlinlang.org/docs/booleans.html

## 개요

Kotlin의 `Boolean` 타입은 두 가지 가능한 값을 가진 불리언 객체를 나타냅니다: `true`와 `false`. 또한 nullable 대응 타입인 `Boolean?`도 있습니다.

**JVM 참고:** JVM에서 불리언은 일반적으로 8비트를 사용하는 원시 `boolean` 타입으로 저장됩니다.

## 내장 연산

Kotlin은 세 가지 주요 불리언 연산을 제공합니다:

| 연산자 | 이름 | 설명 |
|-------|------|------|
| `\|\|` | 논리합 | 논리 OR |
| `&&` | 논리곱 | 논리 AND |
| `!` | 부정 | 논리 NOT |

## 코드 예제

```kotlin
fun main() {
    val myTrue: Boolean = true
    val myFalse: Boolean = false
    val boolNull: Boolean? = null

    println(myTrue || myFalse)  // true
    println(myTrue && myFalse)  // false
    println(!myTrue)             // false
    println(boolNull)            // null
}
```

## 지연 평가

`||`와 `&&` 연산자 모두 **지연 평가**를 사용합니다:

- **`||` (OR):** 첫 번째 피연산자가 `true`이면 두 번째 피연산자는 평가되지 **않습니다**
- **`&&` (AND):** 첫 번째 피연산자가 `false`이면 두 번째 피연산자는 평가되지 **않습니다**

## Nullable 불리언

JVM에서 불리언 객체에 대한 nullable 참조(`Boolean?`)는 [숫자](numbers.html#boxing-and-caching-numbers-on-the-java-virtual-machine)가 처리되는 것과 유사하게 Java 클래스로 박싱됩니다.

---

# Kotlin 숫자

> **원문:** https://kotlinlang.org/docs/numbers.html

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

---

# 부호 없는 정수 타입

> **원문:** https://kotlinlang.org/docs/unsigned-integer-types.html

## 개요

Kotlin은 표준 정수 타입 외에 네 가지 부호 없는 정수 타입을 제공합니다:

| 타입 | 크기 (비트) | 최소값 | 최대값 |
|------|-----------|-------|-------|
| `UByte` | 8 | 0 | 255 |
| `UShort` | 16 | 0 | 65,535 |
| `UInt` | 32 | 0 | 4,294,967,295 (2^32 - 1) |
| `ULong` | 64 | 0 | 18,446,744,073,709,551,615 (2^64 - 1) |

부호 없는 타입은 해당 부호 있는 타입의 대부분의 연산을 지원하며, 해당 부호 있는 타입을 포함하는 단일 저장 프로퍼티를 가진 인라인 클래스로 구현됩니다.

---

## 부호 없는 배열과 범위

**참고:** 부호 없는 배열은 베타 버전이며 `@ExperimentalUnsignedTypes` 어노테이션을 통한 옵트인이 필요합니다.

### 배열 타입
- `UByteArray` - 부호 없는 바이트 배열
- `UShortArray` - 부호 없는 short 배열
- `UIntArray` - 부호 없는 int 배열
- `ULongArray` - 부호 없는 long 배열

### 범위 지원

`UInt`와 `ULong`에도 범위와 진행이 지원됩니다:
- `UIntRange`
- `UIntProgression`
- `ULongRange`
- `ULongProgression`

이것들은 안정적인 기능입니다.

---

## 부호 없는 정수 리터럴

### 접미사 표기법

접미사를 사용하여 부호 없는 리터럴을 지정합니다:

```kotlin
// 'u' 또는 'U' - 컨텍스트에 따라 타입 추론
val b: UByte = 1u          // UByte (예상 타입 제공)
val s: UShort = 1u         // UShort (예상 타입 제공)
val l: ULong = 1u          // ULong (예상 타입 제공)
val a1 = 42u               // UInt (예상 타입 없음, UInt에 맞음)
val a2 = 0xFFFF_FFFF_FFFFu // ULong (UInt에 맞지 않음)

// 'uL' 또는 'UL' - 명시적으로 부호 없는 long
val a = 1UL // ULong
```

---

## 사용 사례

### 16진수 색상 표현

```kotlin
data class Color(val representation: UInt)
val yellow = Color(0xFFCC00CCu)
```

### 바이트 배열 초기화

```kotlin
val byteOrderMarkUtf8 = ubyteArrayOf(0xEFu, 0xBBu, 0xBFu)
```

### 네이티브 API 상호 운용성

Kotlin 함수 시그니처의 부호 없는 타입은 대체 없이 네이티브 선언에 직접 매핑되어 의미를 보존합니다.

---

## 비목표

부호 없는 정수는 다음 용도로 **의도되지 않았습니다:**
- 컬렉션 크기나 인덱스 (부호 있는 정수 사용)
- 음이 아닌 도메인 값 표현

**이유:**
- 부호 있는 정수는 우발적인 오버플로를 감지하고 오류 조건을 신호하는 데 도움이 됩니다 (예: 빈 리스트의 경우 `List.lastIndex == -1`)
- 부호 없는 정수는 부호 있는 정수의 범위 제한 버전으로 취급할 수 없습니다; 어느 타입도 다른 타입의 하위 타입이 아닙니다

---

# Kotlin 문자

> **원문:** https://kotlinlang.org/docs/characters.html

## 개요

문자는 `Char` 타입으로 표현됩니다. 문자 리터럴은 작은따옴표로 작성합니다: `'1'`.

## 저장

JVM에서 문자는 16비트 유니코드 문자를 나타내는 원시 타입 `char`로 저장됩니다.

## 이스케이프 시퀀스

특수 문자는 백슬래시 `\`로 시작합니다. 다음 이스케이프 시퀀스가 지원됩니다:

| 이스케이프 시퀀스 | 의미 |
|------------------|------|
| `\t` | 탭 |
| `\b` | 백스페이스 |
| `\n` | 새 줄 (LF) |
| `\r` | 캐리지 리턴 (CR) |
| `\'` | 작은따옴표 |
| `\"` | 큰따옴표 |
| `\\` | 백슬래시 |
| `\$` | 달러 기호 |

다른 문자의 경우 유니코드 이스케이프 시퀀스 구문을 사용합니다: `'\uFF00'`

## 코드 예제

```kotlin
fun main() {
    val aChar: Char = 'a'
    println(aChar)           // 출력: a
    println('\n')            // 추가 줄 바꿈 문자 출력
    println('\uFF00')        // 유니코드 문자 출력
}
```

## 문자 변환

문자 변수가 숫자 값을 보유하는 경우 [`digitToInt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/digit-to-int.html) 함수를 사용하여 `Int`로 변환할 수 있습니다.

## JVM에서의 박싱

JVM에서 문자는 nullable 참조가 필요할 때 Java 클래스로 박싱됩니다 (숫자와 유사). 박싱 연산은 동일성을 보존하지 않습니다.

---

# Kotlin 문자열

> **원문:** https://kotlinlang.org/docs/strings.html

## 개요

Kotlin에서 문자열은 `String` 타입으로 표현됩니다. JVM에서 문자열은 문자당 약 2바이트의 UTF-16 인코딩을 사용합니다. 문자열은 큰따옴표로 묶인 불변의 문자 시퀀스입니다.

```kotlin
val str = "abcd 123"
```

## 기본 문자열 연산

### 문자 접근

인덱싱을 통해 요소에 접근하고 `for` 루프로 순회할 수 있습니다:

```kotlin
val str = "abcd"
for (c in str) {
    println(c)
}
```

### 문자열 불변성

일단 초기화되면 문자열은 변경할 수 없습니다. 모든 변환은 새로운 `String` 객체를 반환합니다:

```kotlin
val str = "abcd"
println(str.uppercase()) // ABCD
println(str)              // abcd (변경되지 않음)
```

### 문자열 연결

`+` 연산자를 사용합니다:

```kotlin
val s = "abc" + 1
println(s + "def") // abc1def
```

---

## 문자열 리터럴

### 이스케이프된 문자열

백슬래시(`\`)를 사용한 전통적인 이스케이프 시퀀스를 지원합니다:

```kotlin
val s = "Hello, world!\n"
```

### 여러 줄 문자열

삼중 따옴표(`"""`)로 구분되며, 이스케이프가 필요 없습니다:

```kotlin
val text = """
    for (c in "foo") print(c)
"""
```

`trimMargin()`을 사용하여 선행 공백 제거:

```kotlin
val text = """
    |Tell me and I forget.
    |Teach me and I remember.
    |Involve me and I learn.
    |(Benjamin Franklin)
""".trimMargin()
```

사용자 정의 마진 접두사: `trimMargin(">")`

---

## 문자열 템플릿

템플릿 표현식은 `$`를 사용하여 코드를 평가하고 결과를 문자열에 삽입합니다:

### 단순 변수 보간

```kotlin
val i = 10
println("i = $i") // i = 10

val letters = listOf("a","b","c","d","e")
println("Letters: $letters") // Letters: [a, b, c, d, e]
```

### 표현식 보간

```kotlin
val s = "abc"
println("$s.length is ${s.length}") // abc.length is 3
```

### 여러 줄 문자열에서 리터럴 달러 기호 삽입

```kotlin
val price = """${'$'}9.99"""
```

---

## 다중 달러 문자열 보간

보간을 트리거하기 위해 여러 개의 연속 달러 기호를 지정하여 단일 `$`를 리터럴로 취급합니다:

### 이중 달러 예제 (`$$`)

```kotlin
val KClass<*>.jsonSchema : String get() = $$"""
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/product.schema.json",
    "$dynamicAnchor": "meta",
    "title": "$${simpleName ?: qualifiedName ?: "unknown"}",
    "type": "object"
}
"""
```

### 삼중 달러 예제 (`$$$`)

```kotlin
val productName = "carrot"
val requestedData = $$$"""{
    "currency": "$",
    "enteredAmount": "42.45 $$",
    "$$serviceField": "none",
    "product": "$$$productName"
}"""
```

출력:

```
{
    "currency": "$",
    "enteredAmount": "42.45 $$",
    "$$serviceField": "none",
    "product": "carrot"
}
```

---

## 문자열 포맷팅

Kotlin/JVM에서만 사용 가능합니다. 형식 지정자와 함께 `String.format()`을 사용합니다:

**일반적인 지정자:**
- `%d` - 정수
- `%f` - 부동 소수점 숫자
- `%s` - 문자열

```kotlin
// 선행 0이 있는 정수
val integerNumber = String.format("%07d", 31416)
println(integerNumber) // 0031416

// + 기호와 소수점 4자리를 가진 부동 소수점
val floatNumber = String.format("%+.4f", 3.141592)
println(floatNumber) // +3.1416

// 대문자 문자열
val helloString = String.format("%S %S", "hello", "world")
println(helloString) // HELLO WORLD

// argument_index$로 인수 반복
val negativeNumberInParentheses = String.format("%(d means %1$d", -31416)
println(negativeNumberInParentheses) // (31416) means -31416
```

**장점:** 템플릿보다 더 다재다능합니다; 포맷 문자열은 변수에서 할당할 수 있습니다 (현지화에 유용).

---

# Kotlin 배열

> **원문:** https://kotlinlang.org/docs/arrays.html

## 개요

배열은 같은 타입 또는 그 하위 타입의 고정된 수의 값을 보유하는 데이터 구조입니다. 가장 일반적인 타입은 `Array` 클래스입니다. 객체 타입 배열의 원시 값은 객체로 박싱되어 성능에 영향을 미칩니다. 박싱 오버헤드를 피하려면 원시 타입 배열을 대신 사용하세요.

---

## 배열을 사용해야 할 때

**배열을 사용하는 경우:**
- 특수한 저수준 요구 사항이 있는 경우
- 일반 애플리케이션을 넘어서는 성능 요구 사항이 있는 경우
- 사용자 정의 데이터 구조를 구축해야 하는 경우

**그 외의 경우 컬렉션을 사용하세요.** 컬렉션은 다음을 제공합니다:
- 견고하고 의도가 명확한 코드를 위한 읽기 전용 변형
- 쉬운 요소 추가/제거 (배열은 고정 크기)
- `==` 연산자로 구조적 동등성 검사

**배열 수정의 비효율성 예제:**

```kotlin
fun main() {
    var riversArray = arrayOf("Nile", "Amazon", "Yangtze")
    // 새 배열을 생성하고, 요소를 복사하고, "Mississippi"를 추가
    riversArray += "Mississippi"
    println(riversArray.joinToString()) // Nile, Amazon, Yangtze, Mississippi
}
```

---

## 배열 생성

### 생성 함수

**`arrayOf()`** - 값으로 배열 생성:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    println(simpleArray.joinToString()) // 1, 2, 3
}
```

**`arrayOfNulls()`** - null로 채워진 배열 생성:

```kotlin
fun main() {
    val nullArray: Array<Int?> = arrayOfNulls(3)
    println(nullArray.joinToString()) // null, null, null
}
```

**`emptyArray()`** - 빈 배열 생성:

```kotlin
var exampleArray = emptyArray<String>()
// 타입은 어느 쪽에서든 지정할 수 있음
var exampleArray: Array<String> = emptyArray()
```

### 배열 생성자

크기와 초기화 함수를 받습니다:

```kotlin
fun main() {
    // 0으로 채워진 Array<Int> 생성 [0, 0, 0]
    val initArray = Array<Int>(3) { 0 }
    println(initArray.joinToString()) // 0, 0, 0

    // ["0", "1", "4", "9", "16"]의 Array<String> 생성
    val asc = Array(5) { i -> (i * i).toString() }
    asc.forEach { print(it) } // 014916
}
```

**참고:** Kotlin에서 인덱스는 0부터 시작합니다.

### 중첩 배열

배열은 다차원일 수 있습니다:

```kotlin
fun main() {
    // 2차원 배열
    val twoDArray = Array(2) { Array<Int>(2) { 0 } }
    println(twoDArray.contentDeepToString()) // [[0, 0], [0, 0]]

    // 3차원 배열
    val threeDArray = Array(3) { Array(3) { Array<Int>(3) { 0 } } }
    println(threeDArray.contentDeepToString())
    // [[[0, 0, 0], [0, 0, 0], [0, 0, 0]], [[0, 0, 0], [0, 0, 0], [0, 0, 0]], [[0, 0, 0], [0, 0, 0], [0, 0, 0]]]
}
```

**참고:** 중첩 배열은 같은 타입이나 크기일 필요가 없습니다.

---

## 요소 접근 및 수정

인덱스 접근 연산자 `[]`를 사용합니다:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    val twoDArray = Array(2) { Array<Int>(2) { 0 } }

    // 요소 수정
    simpleArray[0] = 10
    twoDArray[0][0] = 2

    println(simpleArray[0].toString()) // 10
    println(twoDArray[0][0].toString()) // 2
}
```

**불변성:** Kotlin은 런타임 실패를 방지하기 위해 `Array<String>`을 `Array<Any>`에 할당하는 것을 허용하지 않습니다. 대신 `Array<out Any>`를 사용하세요 (타입 프로젝션).

---

## 배열 작업

### 가변 인수 전달 (varargs)

스프레드 연산자 `*`를 사용합니다:

```kotlin
fun main() {
    val lettersArray = arrayOf("c", "d")
    printAllStrings("a", "b", *lettersArray) // abcd
}

fun printAllStrings(vararg strings: String) {
    for (string in strings) {
        print(string)
    }
}
```

### 배열 비교

`.contentEquals()`와 `.contentDeepEquals()`를 사용합니다:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    val anotherArray = arrayOf(1, 2, 3)

    println(simpleArray.contentEquals(anotherArray)) // true

    simpleArray[0] = 10
    println(simpleArray contentEquals anotherArray) // false (중위 표기법)
}
```

**주의: `==`나 `!=`를 사용하지 마세요** - 내용 동등성이 아닌 객체 동일성을 검사합니다.

### 배열 변환

#### 합계

```kotlin
fun main() {
    val sumArray = arrayOf(1, 2, 3)
    println(sumArray.sum()) // 6
}
```

**참고:** 숫자 데이터 타입(Int 등)에서만 작동합니다.

#### 셔플

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [3, 2, 1] (무작위)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [2, 3, 1] (무작위)
}
```

### 배열을 컬렉션으로 변환

#### 리스트 또는 세트로 변환

```kotlin
fun main() {
    val simpleArray = arrayOf("a", "b", "c", "c")

    println(simpleArray.toSet()) // [a, b, c]
    println(simpleArray.toList()) // [a, b, c, c]
}
```

#### 맵으로 변환

`.toMap()`을 사용하여 `Array<Pair<K,V>>`만 맵으로 변환할 수 있습니다:

```kotlin
fun main() {
    val pairArray = arrayOf(
        "apple" to 120,
        "banana" to 150,
        "cherry" to 90,
        "apple" to 140
    )

    println(pairArray.toMap())
    // {apple=140, banana=150, cherry=90}
    // 참고: 중복 키는 최신 값을 사용
}
```

---

## 원시 타입 배열

박싱 오버헤드를 피하려면 원시값과 함께 `Array` 대신 원시 타입 배열을 사용하세요:

| Kotlin 타입 | Java 동등물 |
|------------|------------|
| `BooleanArray` | `boolean[]` |
| `ByteArray` | `byte[]` |
| `CharArray` | `char[]` |
| `DoubleArray` | `double[]` |
| `FloatArray` | `float[]` |
| `IntArray` | `int[]` |
| `LongArray` | `long[]` |
| `ShortArray` | `short[]` |

**예제:**

```kotlin
fun main() {
    val exampleArray = IntArray(5)
    println(exampleArray.joinToString()) // 0, 0, 0, 0, 0
}
```

### 변환

- **원시에서 객체로:** `.toTypedArray()`
- **객체에서 원시로:** `.toBooleanArray()`, `.toByteArray()`, `.toCharArray()` 등

---

## 다음 단계

- [컬렉션 개요](collections-overview.html) - 컬렉션이 권장되는 이유 알아보기
- [기본 타입](types-overview.html) - 다른 기본 타입 탐색
- [Java에서 Kotlin으로 컬렉션 가이드](java-to-kotlin-collections-guide.html) - Java 개발자용
