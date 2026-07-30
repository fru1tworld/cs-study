# 코틀린 기본 타입, 제어 흐름, 예외 처리

## 기본 타입

## Kotlin 기본 타입 개요

> **원문:** https://kotlinlang.org/docs/basic-types.html

Kotlin의 기본 타입은 다음과 같은 범주로 구성됩니다:

### 숫자 타입

#### 정수 타입
- `Byte` - 8비트
- `Short` - 16비트
- `Int` - 32비트
- `Long` - 64비트

#### 부호 없는 정수 타입
- `UByte` - 8비트
- `UShort` - 16비트
- `UInt` - 32비트
- `ULong` - 64비트

#### 부동 소수점 타입
- `Float` - 32비트
- `Double` - 64비트

### 불리언 타입
- `Boolean` - `true` 또는 `false`

### 문자 타입
- `Char` - 단일 문자

### 문자열 타입
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

## Kotlin 불리언

> **원문:** https://kotlinlang.org/docs/booleans.html

### 개요

Kotlin의 `Boolean` 타입은 두 가지 가능한 값을 가진 불리언 객체를 나타냅니다: `true`와 `false`. 또한 nullable 대응 타입인 `Boolean?`도 있습니다.

**JVM 참고:** JVM에서 불리언은 일반적으로 8비트를 사용하는 원시 `boolean` 타입으로 저장됩니다.

### 내장 연산

Kotlin은 세 가지 주요 불리언 연산을 제공합니다:

| 연산자 | 이름 | 설명 |
|-------|------|------|
| `\|\|` | 논리합 | 논리 OR |
| `&&` | 논리곱 | 논리 AND |
| `!` | 부정 | 논리 NOT |

### 코드 예제

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

### 지연 평가

`||`와 `&&` 연산자 모두 **지연 평가**를 사용합니다:

- **`||` (OR):** 첫 번째 피연산자가 `true`이면 두 번째 피연산자는 평가되지 **않습니다**
- **`&&` (AND):** 첫 번째 피연산자가 `false`이면 두 번째 피연산자는 평가되지 **않습니다**

### Nullable 불리언

JVM에서 불리언 객체에 대한 nullable 참조(`Boolean?`)는 [숫자](numbers.html#boxing-and-caching-numbers-on-the-java-virtual-machine)가 처리되는 것과 유사하게 Java 클래스로 박싱됩니다.

---

## Kotlin 숫자

> **원문:** https://kotlinlang.org/docs/numbers.html

### 개요

이 페이지는 Kotlin의 내장 숫자 타입, 리터럴 상수, 연산, 그리고 부동 소수점 숫자에 대한 특별한 고려 사항을 다룹니다.

---

### 정수 타입

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

### 부동 소수점 타입

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

### 숫자 리터럴 상수

#### 정수 리터럴
- **10진수:** `123`
- **Long (대문자 L 접미사):** `123L`
- **16진수:** `0x0F`
- **2진수:** `0b00001011`
- **8진수:** Kotlin에서 지원하지 않음

#### 부동 소수점 리터럴
- **Double (기본):** `123.5`, `123.5e10`
- **Float (f/F 접미사):** `123.5f`

#### 가독성을 위한 밑줄

```kotlin
val oneMillion = 1_000_000
val creditCardNumber = 1234_5678_9012_3456L
val socialSecurityNumber = 999_99_9999L
val hexBytes = 0xFF_EC_DE_5E
val bytes = 0b11010010_01101001_10010100_10010010
val bigFractional = 1_234_567.7182818284
```

---

### JVM에서의 박싱과 캐싱

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

### 명시적 숫자 변환

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

#### 암시적 변환에 반대하는 이유

암시적 변환은 예기치 않은 동작과 동등성/동일성 손실로 이어질 수 있습니다:

```kotlin
// 가상 (컴파일되지 않음):
val a: Int? = 1 // java.lang.Integer
val b: Long? = a // java.lang.Long이 될 것
print(b == a) // 예기치 않게 "false"를 출력할 것
```

---

### 숫자 연산

Kotlin은 표준 산술 연산자를 지원합니다: `+`, `-`, `*`, `/`, `%`

```kotlin
println(1 + 2) // 3
println(2_500_000_000L - 1L)
println(3.14 * 2.71)
println(10.0 / 3) // 3.3333...
```

#### 정수 나눗셈

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

#### 비트 연산

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

### 부동 소수점 숫자 비교

#### Float/Double로 정적 타입 지정된 경우

[IEEE 754 표준](https://en.wikipedia.org/wiki/IEEE_754)을 따릅니다:
- `NaN == NaN` -> `false`
- `0.0 == -0.0` -> `true`

#### 동적으로 타입 지정된 경우 (예: Any, Comparable)

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

### 관련 문서
- [타입 개요](types-overview.html)
- [부호 없는 정수 타입](unsigned-integer-types.html)
- [연산자 오버로딩](operator-overloading.html)

---

## 부호 없는 정수 타입

> **원문:** https://kotlinlang.org/docs/unsigned-integer-types.html

### 개요

Kotlin은 표준 정수 타입 외에 네 가지 부호 없는 정수 타입을 제공합니다:

| 타입 | 크기 (비트) | 최소값 | 최대값 |
|------|-----------|-------|-------|
| `UByte` | 8 | 0 | 255 |
| `UShort` | 16 | 0 | 65,535 |
| `UInt` | 32 | 0 | 4,294,967,295 (2^32 - 1) |
| `ULong` | 64 | 0 | 18,446,744,073,709,551,615 (2^64 - 1) |

부호 없는 타입은 해당 부호 있는 타입의 대부분의 연산을 지원하며, 해당 부호 있는 타입을 포함하는 단일 저장 프로퍼티를 가진 인라인 클래스로 구현됩니다.

---

### 부호 없는 배열과 범위

**참고:** 부호 없는 배열은 베타 버전이며 `@ExperimentalUnsignedTypes` 어노테이션을 통한 옵트인이 필요합니다.

#### 배열 타입
- `UByteArray` - 부호 없는 바이트 배열
- `UShortArray` - 부호 없는 short 배열
- `UIntArray` - 부호 없는 int 배열
- `ULongArray` - 부호 없는 long 배열

#### 범위 지원

`UInt`와 `ULong`에도 범위와 진행이 지원됩니다:
- `UIntRange`
- `UIntProgression`
- `ULongRange`
- `ULongProgression`

이것들은 안정적인 기능입니다.

---

### 부호 없는 정수 리터럴

#### 접미사 표기법

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

### 사용 사례

#### 16진수 색상 표현

```kotlin
data class Color(val representation: UInt)
val yellow = Color(0xFFCC00CCu)
```

#### 바이트 배열 초기화

```kotlin
val byteOrderMarkUtf8 = ubyteArrayOf(0xEFu, 0xBBu, 0xBFu)
```

#### 네이티브 API 상호 운용성

Kotlin 함수 시그니처의 부호 없는 타입은 대체 없이 네이티브 선언에 직접 매핑되어 의미를 보존합니다.

---

### 비목표

부호 없는 정수는 다음 용도로 **의도되지 않았습니다:**
- 컬렉션 크기나 인덱스 (부호 있는 정수 사용)
- 음이 아닌 도메인 값 표현

**이유:**
- 부호 있는 정수는 우발적인 오버플로를 감지하고 오류 조건을 신호하는 데 도움이 됩니다 (예: 빈 리스트의 경우 `List.lastIndex == -1`)
- 부호 없는 정수는 부호 있는 정수의 범위 제한 버전으로 취급할 수 없습니다; 어느 타입도 다른 타입의 하위 타입이 아닙니다

---

## Kotlin 문자

> **원문:** https://kotlinlang.org/docs/characters.html

### 개요

문자는 `Char` 타입으로 표현됩니다. 문자 리터럴은 작은따옴표로 작성합니다: `'1'`.

### 저장

JVM에서 문자는 16비트 유니코드 문자를 나타내는 원시 타입 `char`로 저장됩니다.

### 이스케이프 시퀀스

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

### 코드 예제

```kotlin
fun main() {
    val aChar: Char = 'a'
    println(aChar)           // 출력: a
    println('\n')            // 추가 줄 바꿈 문자 출력
    println('\uFF00')        // 유니코드 문자 출력
}
```

### 문자 변환

문자 변수가 숫자 값을 보유하는 경우 [`digitToInt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/digit-to-int.html) 함수를 사용하여 `Int`로 변환할 수 있습니다.

### JVM에서의 박싱

JVM에서 문자는 nullable 참조가 필요할 때 Java 클래스로 박싱됩니다 (숫자와 유사). 박싱 연산은 동일성을 보존하지 않습니다.

---

## Kotlin 문자열

> **원문:** https://kotlinlang.org/docs/strings.html

### 개요

Kotlin에서 문자열은 `String` 타입으로 표현됩니다. JVM에서 문자열은 문자당 약 2바이트의 UTF-16 인코딩을 사용합니다. 문자열은 큰따옴표로 묶인 불변의 문자 시퀀스입니다.

```kotlin
val str = "abcd 123"
```

### 기본 문자열 연산

#### 문자 접근

인덱싱을 통해 요소에 접근하고 `for` 루프로 순회할 수 있습니다:

```kotlin
val str = "abcd"
for (c in str) {
    println(c)
}
```

#### 문자열 불변성

일단 초기화되면 문자열은 변경할 수 없습니다. 모든 변환은 새로운 `String` 객체를 반환합니다:

```kotlin
val str = "abcd"
println(str.uppercase()) // ABCD
println(str)              // abcd (변경되지 않음)
```

#### 문자열 연결

`+` 연산자를 사용합니다:

```kotlin
val s = "abc" + 1
println(s + "def") // abc1def
```

---

### 문자열 리터럴

#### 이스케이프된 문자열

백슬래시(`\`)를 사용한 전통적인 이스케이프 시퀀스를 지원합니다:

```kotlin
val s = "Hello, world!\n"
```

#### 여러 줄 문자열

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

### 문자열 템플릿

템플릿 표현식은 `$`를 사용하여 코드를 평가하고 결과를 문자열에 삽입합니다:

#### 단순 변수 보간

```kotlin
val i = 10
println("i = $i") // i = 10

val letters = listOf("a","b","c","d","e")
println("Letters: $letters") // Letters: [a, b, c, d, e]
```

#### 표현식 보간

```kotlin
val s = "abc"
println("$s.length is ${s.length}") // abc.length is 3
```

#### 여러 줄 문자열에서 리터럴 달러 기호 삽입

```kotlin
val price = """${'$'}9.99"""
```

---

### 다중 달러 문자열 보간

보간을 트리거하기 위해 여러 개의 연속 달러 기호를 지정하여 단일 `$`를 리터럴로 취급합니다:

#### 이중 달러 예제 (`$$`)

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

#### 삼중 달러 예제 (`$$$`)

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

### 문자열 포맷팅

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

## Kotlin 배열

> **원문:** https://kotlinlang.org/docs/arrays.html

### 개요

배열은 같은 타입 또는 그 하위 타입의 고정된 수의 값을 보유하는 데이터 구조입니다. 가장 일반적인 타입은 `Array` 클래스입니다. 객체 타입 배열의 원시 값은 객체로 박싱되어 성능에 영향을 미칩니다. 박싱 오버헤드를 피하려면 원시 타입 배열을 대신 사용하세요.

---

### 배열을 사용해야 할 때

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

### 배열 생성

#### 생성 함수

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

#### 배열 생성자

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

#### 중첩 배열

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

### 요소 접근 및 수정

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

### 배열 작업

#### 가변 인수 전달 (varargs)

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

#### 배열 비교

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

#### 배열 변환

##### 합계

```kotlin
fun main() {
    val sumArray = arrayOf(1, 2, 3)
    println(sumArray.sum()) // 6
}
```

**참고:** 숫자 데이터 타입(Int 등)에서만 작동합니다.

##### 셔플

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [3, 2, 1] (무작위)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [2, 3, 1] (무작위)
}
```

#### 배열을 컬렉션으로 변환

##### 리스트 또는 세트로 변환

```kotlin
fun main() {
    val simpleArray = arrayOf("a", "b", "c", "c")

    println(simpleArray.toSet()) // [a, b, c]
    println(simpleArray.toList()) // [a, b, c, c]
}
```

##### 맵으로 변환

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

### 원시 타입 배열

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

#### 변환

- **원시에서 객체로:** `.toTypedArray()`
- **객체에서 원시로:** `.toBooleanArray()`, `.toByteArray()`, `.toCharArray()` 등

---

### 다음 단계

- [컬렉션 개요](collections-overview.html) - 컬렉션이 권장되는 이유 알아보기
- [기본 타입](types-overview.html) - 다른 기본 타입 탐색
- [Java에서 Kotlin으로 컬렉션 가이드](java-to-kotlin-collections-guide.html) - Java 개발자용

---

## 제어 흐름과 예외 처리

## Kotlin 조건문과 루프

> **원문:** https://kotlinlang.org/docs/control-flow.html

### 개요

Kotlin은 `if`, `when`, 그리고 다양한 루프 구조를 사용하여 프로그램 흐름을 제어하는 유연한 도구를 제공합니다.

---

### if 표현식

#### 기본 사용법

괄호 `()` 안에 조건을, 중괄호 `{}` 안에 동작을 사용합니다:

```kotlin
if (heightAlice < heightBob) {
    taller = heightBob
}
```

#### 표현식으로서의 if

`if`는 값을 직접 할당하는 표현식으로 사용할 수 있습니다 (삼항 연산자를 대체):

```kotlin
val taller = if (heightAlice > heightBob) heightAlice else heightBob
```

#### else if 체인

```kotlin
val heightOrLimit = if (heightLimit > heightAlice) heightLimit
                    else if (heightAlice > heightBob) heightAlice
                    else heightBob
```

#### 블록 표현식

블록의 마지막 표현식이 결과가 됩니다:

```kotlin
val taller = if (heightAlice > heightBob) {
    print("Choose Alice\n")
    heightAlice
} else {
    print("Choose Bob\n")
    heightBob
}
```

---

### when 표현식과 문

#### 주제가 있는 기본 when

```kotlin
val userRole = "Editor"
when (userRole) {
    "Viewer" -> print("User has read-only access")
    "Editor" -> print("User can edit content")
    else -> print("User role is not recognized")
}
```

#### 표현식 vs 문으로서의 when

**표현식** (값 반환):

```kotlin
val text = when (x) {
    1 -> "x == 1"
    2 -> "x == 2"
    else -> "x is neither 1 nor 2"
}
```

**문** (동작 수행):

```kotlin
when (x) {
    1 -> print("x == 1")
    2 -> print("x == 2")
    else -> print("x is neither 1 nor 2")
}
```

#### 주제 없는 when

```kotlin
val message = when {
    localFileSize > remoteFileSize -> "Local file is larger"
    localFileSize < remoteFileSize -> "Local file is smaller"
    else -> "Files are the same size"
}
```

#### 완전성

- **문**: 모든 경우를 다룰 필요 없음
- **표현식**: 모든 경우를 다뤄야 함 (그렇지 않으면 컴파일러가 오류 발생)
- **예외**: 열거형, Boolean, 또는 sealed 클래스는 모든 경우가 다뤄지면 `else`가 필요 없음

#### 여러 조건 (쉼표로 구분)

```kotlin
when (ticketPriority) {
    "Low", "Medium" -> print("Standard response time")
    else -> print("High-priority handling")
}
```

#### 조건으로 표현식 사용

```kotlin
when (enteredPin) {
    storedPin.toInt() -> print("PIN is correct")
    else -> print("Incorrect PIN")
}
```

#### 범위와 컬렉션 검사

```kotlin
when (x) {
    in 1..10 -> print("x is in the range")
    in validNumbers -> print("x is valid")
    !in 10..20 -> print("x is outside the range")
    else -> print("none of the above")
}
```

#### 타입 검사

```kotlin
fun hasPrefix(input: Any): Boolean = when (input) {
    is String -> input.startsWith("ID-")
    else -> false
}
```

#### 변수에 주제 캡처

```kotlin
val message = when (val input = "yes") {
    "yes" -> "You said yes"
    "no" -> "You said no"
    else -> "Unrecognized input: $input"
}
```

#### 가드 조건

가드 조건은 기본 조건 후에 추가 검사를 추가합니다:

```kotlin
when (animal) {
    is Animal.Dog -> feedDog()
    is Animal.Cat if !animal.mouseHunter -> feedCat()
    else -> println("Unknown animal")
}
```

불리언 연산자를 사용한 여러 조건:

```kotlin
when (animal) {
    is Animal.Cat if (!animal.mouseHunter && animal.hungry) -> feedCat()
}
```

`else if`와 함께:

```kotlin
when (animal) {
    is Animal.Dog -> feedDog()
    is Animal.Cat if !animal.mouseHunter -> feedCat()
    else if animal.eatsPlants -> giveLettuce()
    else -> println("Unknown animal")
}
```

---

### for 루프

#### 기본 for 루프

```kotlin
for (item in collection) {
    println(item)
}
```

#### 범위

```kotlin
// 닫힌 범위 (포함)
for (i in 1..6) print(i)  // 123456

// 열린 범위 (제외)
for (i in 1..<6) print(i)  // 12345

// 역순과 스텝
for (i in 6 downTo 0 step 2) print(i)  // 6420
```

#### 인덱스가 있는 배열

```kotlin
val routineSteps = arrayOf("Wake up", "Brush teeth", "Make coffee")

// indices 사용
for (i in routineSteps.indices) {
    println(routineSteps[i])
}

// withIndex() 사용
for ((index, value) in routineSteps.withIndex()) {
    println("The step at $index is \"$value\"")
}
```

#### 사용자 정의 반복자

`Iterable<T>` 인터페이스 구현:

```kotlin
class Booklet(val totalPages: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> {
        return object : Iterator<Int> {
            var current = 1
            override fun hasNext() = current <= totalPages
            override fun next() = current++
        }
    }
}

for (page in booklet) {
    println("Reading page $page")
}
```

---

### while 루프

#### while 루프

먼저 조건을 검사한 후 본문을 실행합니다:

```kotlin
var carsInGarage = 0
val maxCapacity = 3

while (carsInGarage < maxCapacity) {
    println("Car entered. Cars now in garage: ${++carsInGarage}")
}
```

#### do-while 루프

먼저 본문을 실행한 후 조건을 검사합니다:

```kotlin
do {
    roll = Random.nextInt(1, 7)
    println("Rolled a $roll")
} while (roll != 6)
```

---

### break와 continue

Kotlin은 루프에서 전통적인 `break`와 `continue` 연산자를 지원합니다. 자세한 내용은 [반환과 점프](returns.html) 문서를 참조하세요.

---

## 반환과 점프

> **원문:** https://kotlinlang.org/docs/returns.html

### 개요

Kotlin에는 세 가지 구조적 점프 표현식이 있습니다:

- **`return`** - 가장 가까운 둘러싸는 함수나 익명 함수에서 반환
- **`break`** - 가장 가까운 둘러싸는 루프를 종료
- **`continue`** - 가장 가까운 둘러싸는 루프의 다음 단계로 진행

이 모든 표현식은 더 큰 표현식의 일부로 사용할 수 있습니다:

```kotlin
val s = person.name ?: return
```

이 표현식들의 타입은 `Nothing` 타입입니다.

---

### break와 continue 레이블

Kotlin의 모든 표현식은 레이블로 표시될 수 있습니다. 레이블은 식별자 뒤에 `@` 기호가 오는 형태입니다 (예: `abc@` 또는 `fooBar@`).

#### 예제:

```kotlin
loop@ for (i in 1..100) { // ... }
```

`break`나 `continue`를 레이블로 한정할 수 있습니다:

```kotlin
loop@ for (i in 1..100) {
    for (j in 1..100) {
        if (...) break@loop
    }
}
```

- 레이블로 한정된 `break`는 해당 레이블이 표시된 루프 바로 다음 실행 지점으로 점프합니다
- `continue`는 해당 루프의 다음 반복으로 진행합니다

**참고:** 비지역 사용은 둘러싸는 인라인 함수에서 사용되는 람다 표현식에서 유효합니다.

---

### 레이블로 반환

함수는 함수 리터럴, 지역 함수, 객체 표현식을 사용하여 중첩될 수 있습니다. 한정된 `return`을 사용하면 외부 함수에서 반환할 수 있습니다.

#### 명시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        if (it == 3) return@lit // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with explicit label")
}
```

#### 암시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach {
        if (it == 3) return@forEach // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with implicit label")
}
```

#### 익명 함수 사용:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach(fun(value: Int) {
        if (value == 3) return // 익명 함수에서의 지역 반환
        print(value)
    })
    print(" done with anonymous function")
}
```

#### run 람다로 break 시뮬레이션:

```kotlin
fun foo() {
    run loop@{
        listOf(1, 2, 3, 4, 5).forEach {
            if (it == 3) return@loop // run에 전달된 람다에서의 비지역 반환
            print(it)
        }
    }
    print(" done with nested loop")
}
```

---

### 핵심 사항

- 람다에서의 지역 반환은 일반 루프의 `continue`와 유사합니다
- `break`에 대한 직접적인 등가물은 없지만 외부 `run` 람다를 사용하여 시뮬레이션할 수 있습니다
- 값을 반환할 때 파서는 한정된 반환에 우선순위를 부여합니다:
  ```kotlin
  return@a 1  // 레이블 @a에서 1을 반환 (레이블이 지정된 표현식이 아님)
  ```
- 람다에서의 비지역 반환은 람다가 인라인 함수 역할을 할 때 가능합니다

---

## Kotlin 예외

> **원문:** https://kotlinlang.org/docs/exceptions.html

### 개요

Kotlin의 예외는 런타임 오류가 발생할 때 코드가 예측 가능하게 실행되도록 도와줍니다. Kotlin은 **모든 예외를 기본적으로 체크되지 않은 예외로 취급**하여 명시적 선언 없이도 예외 처리를 단순화합니다.

예외 작업에는 두 가지 주요 동작이 포함됩니다:
- **예외 던지기**: 문제가 발생했음을 나타냄
- **예외 잡기**: 예기치 않은 예외를 수동으로 처리

---

### 예외 던지기

#### 기본 예외 던지기

```kotlin
throw IllegalArgumentException()
```

#### 사용자 정의 메시지와 원인 포함

```kotlin
val cause = IllegalStateException("Original cause: illegal state")
if (userInput < 0) {
    throw IllegalArgumentException("Input must be non-negative", cause)
}
```

---

### 전제 조건 함수로 예외 던지기

Kotlin은 자동 예외 던지기를 위한 세 가지 전제 조건 함수를 제공합니다:

| 함수 | 사용 사례 | 던지는 예외 |
|------|----------|------------|
| `require()` | 사용자 입력 유효성 검사 | `IllegalArgumentException` |
| `check()` | 객체/변수 상태 유효성 검사 | `IllegalStateException` |
| `error()` | 불법 상태 또는 조건 표시 | `IllegalStateException` |

#### require() 함수

입력 인수 유효성 검사:

```kotlin
fun getIndices(count: Int): List<Int> {
    require(count >= 0) { "Count must be non-negative. You set count to $count." }
    return List(count) { it + 1 }
}

fun printNonNullString(str: String?) {
    require(str != null)
    println(str.length) // non-null로 스마트 캐스트
}
```

#### check() 함수

객체/변수 상태 유효성 검사:

```kotlin
fun main() {
    var someState: String? = null
    fun getStateValue(): String {
        val state = checkNotNull(someState) { "State must be set beforehand!" }
        check(state.isNotEmpty()) { "State must be non-empty!" }
        return state
    }
    someState = "non-empty-state"
    println(getStateValue()) // "non-empty-state"
}
```

#### error() 함수

불법 상태 신호 (`when` 표현식에서 유용):

```kotlin
class User(val name: String, val role: String)

fun processUserRole(user: User) {
    when (user.role) {
        "admin" -> println("${user.name} is an admin.")
        "editor" -> println("${user.name} is an editor.")
        "viewer" -> println("${user.name} is a viewer.")
        else -> error("Undefined role: ${user.role}")
    }
}
```

---

### try-catch 블록으로 예외 처리

#### 기본 구조

```kotlin
try {
    // 예외를 던질 수 있는 코드
} catch (e: SomeException) {
    // 예외 처리 코드
}
```

#### 표현식으로서의 try-catch

```kotlin
fun main() {
    val num: Int = try {
        count()
    } catch (e: ArithmeticException) {
        -1
    }
    println("Result: $num")
}

fun count(): Int {
    val a = 0
    return 10 / a
}
```

#### 여러 catch 블록 (가장 구체적인 것 먼저)

```kotlin
open class WithdrawalException(message: String) : Exception(message)
class InsufficientFundsException(message: String) : WithdrawalException(message)

fun processWithdrawal(amount: Double, availableFunds: Double) {
    if (amount > availableFunds) {
        throw InsufficientFundsException("Insufficient funds for the withdrawal.")
    }
    if (amount < 1 || amount % 1 != 0.0) {
        throw WithdrawalException("Invalid withdrawal amount.")
    }
    println("Withdrawal processed")
}

fun main() {
    try {
        processWithdrawal(500.5, 500.0)
    } catch (e: InsufficientFundsException) {
        println("Caught: ${e.message}")
    } catch (e: WithdrawalException) {
        println("Caught: ${e.message}")
    }
}
```

---

### finally 블록

`finally` 블록은 성공 또는 예외 여부에 관계없이 **항상 실행**됩니다. 정리 작업에 사용됩니다.

#### 기본 구조

```kotlin
try {
    // 예외를 던질 수 있는 코드
} catch (e: YourException) {
    // 예외 핸들러
} finally {
    // 항상 실행됨
}
```

#### 완전한 예제

```kotlin
fun divideOrNull(a: Int): Int {
    try {
        val b = 44 / a
        println("try block: Executing division: $b")
        return b
    } catch (e: ArithmeticException) {
        println("catch block: Encountered ArithmeticException $e")
        return -1
    } finally {
        println("finally block: Always executed")
    }
}

fun main() {
    divideOrNull(0)
}
```

#### `.use()`를 사용한 리소스 관리

자동으로 리소스 닫기 (`AutoClosable` 구현):

```kotlin
FileWriter("test.txt").use { writer ->
    writer.write("some text")
    // 블록 완료 후 자동으로 닫힘
}
```

#### catch 없는 try-finally

```kotlin
class MockResource {
    fun use() {
        println("Resource being used")
        val result = 100 / 0
    }
    fun close() {
        println("Resource closed")
    }
}

fun main() {
    val resource = MockResource()
    try {
        resource.use()
    } finally {
        resource.close()
    }
}
```

---

### 사용자 정의 예외 만들기

#### 기본 사용자 정의 예외

```kotlin
class MyException: Exception("My message")
```

#### 내장 예외의 서브클래스

```kotlin
class NumberTooLargeException: ArithmeticException("My message")
```

#### 사용자 정의 예외 계층

부모를 `open`으로 선언해야 합니다:

```kotlin
open class MyCustomException(message: String): Exception(message)
class SpecificCustomException: MyCustomException("Specific error message")
```

#### 사용자 정의 예외 사용

```kotlin
class NegativeNumberException: Exception("Parameter is less than zero.")
class NonNegativeNumberException: Exception("Parameter is a non-negative number.")

fun myFunction(number: Int) {
    if (number < 0) throw NegativeNumberException()
    else if (number >= 0) throw NonNegativeNumberException()
}

fun main() {
    myFunction(1)
}
```

#### sealed 클래스를 사용한 예외 계층

```kotlin
sealed class AccountException(message: String, cause: Throwable? = null):
    Exception(message, cause)

class InvalidAccountCredentialsException :
    AccountException("Invalid account credentials detected")

class APIKeyExpiredException(
    message: String = "API key expired",
    cause: Throwable? = null
) : AccountException(message, cause)

fun validateAccount() {
    if (!areCredentialsValid()) throw InvalidAccountCredentialsException()
    if (isAPIKeyExpired()) {
        val cause = RuntimeException("API key validation failed due to network error")
        throw APIKeyExpiredException(cause = cause)
    }
}

fun main() {
    try {
        validateAccount()
        println("Operation successful")
    } catch (e: AccountException) {
        println("Error: ${e.message}")
        e.cause?.let { println("Caused by: ${it.message}") }
    }
}
```

---

### Nothing 타입

`Nothing`은 **절대 성공적으로 완료되지 않는** 함수/표현식을 나타내는 특별한 타입입니다 (항상 던지거나 무한 루프).

#### 기본 사용

```kotlin
class Person(val name: String?)

fun fail(message: String): Nothing {
    throw IllegalArgumentException(message)
}

fun main() {
    val person = Person(name = null)
    val s: String = person.name ?: fail("Name required")
    println(s)
}
```

#### TODO() 사용

```kotlin
fun notImplementedFunction(): Int {
    TODO("This function is not yet implemented")
}

fun main() {
    val result = notImplementedFunction() // NotImplementedError 던짐
    println(result)
}
```

---

### 예외 클래스

일반적인 예외 타입 (`RuntimeException`의 서브클래스):

#### ArithmeticException

```kotlin
val example = 2 / 0 // ArithmeticException 던짐
```

#### IndexOutOfBoundsException

```kotlin
val myList = mutableListOf(1, 2, 3)
myList.removeAt(3) // IndexOutOfBoundsException 던짐

// 더 안전한 대안
val element = myList.getOrNull(3)
println("Element at index 3: $element")
```

#### NoSuchElementException

```kotlin
val emptyList = listOf<Int>()
val firstElement = emptyList.first() // NoSuchElementException 던짐

// 더 안전한 대안
val firstElement = emptyList.firstOrNull()
println("First element: $firstElement")
```

#### NumberFormatException

```kotlin
val string = "This is not a number"
val number = string.toInt() // NumberFormatException 던짐

// 더 안전한 대안
val number = string.toIntOrNull()
println("Converted number: $number")
```

#### NullPointerException

```kotlin
val text: String? = null
println(text!!.length) // NullPointerException 던짐
```

---

### 예외 계층

루트: **Throwable**
- **Error**: 심각한 문제 (예: `OutOfMemoryError`, `StackOverflowError`)
- **Exception**: 처리할 조건
  - **RuntimeException**: 코드 검사를 통해 방지 가능
    - `ArithmeticException`
    - `IndexOutOfBoundsException`
    - `NullPointerException`
    - `NumberFormatException`
    - 등
  - `IOException` 및 기타 체크된 예외

---

### 스택 트레이스

오류로 이어지는 함수 호출 순서를 보여주는 보고서:

```kotlin
fun main() {
    throw ArithmeticException("This is an arithmetic exception!")
}
```

**출력:**

```
Exception in thread "main" java.lang.ArithmeticException: This is an arithmetic exception!
at MainKt.main(Main.kt:3)
```

요소:
- 예외 타입: `java.lang.ArithmeticException`
- 스레드: `main`
- 메시지: `"This is an arithmetic exception!"`
- 스택 프레임: 메서드 이름과 파일 위치 표시

---

### Java, Swift, Objective-C와의 예외 상호 운용성

호출자에게 가능한 예외를 알리려면 `@Throws` 어노테이션을 사용하세요:

```kotlin
@Throws(IOException::class)
fun someFunction() {
    // 구현
}
```

이것은 체크된 예외와 체크되지 않은 예외를 구분하는 언어에서 Kotlin 코드를 호출할 때 도움이 됩니다.

---

### 핵심 사항 요약

- 모든 예외는 Kotlin에서 **체크되지 않음**
- 전제 조건에 `require()`, `check()`, `error()` 사용
- `catch` 블록을 가장 구체적인 것부터 가장 덜 구체적인 순으로 정렬
- `finally` 블록은 항상 실행됨
- 자동 리소스 정리에 `.use()` 사용
- `Exception`을 확장하여 사용자 정의 예외 생성
- `Nothing` 타입은 반환하지 않는 함수를 나타냄
- 예외 방지를 위해 더 안전한 대안 사용 (`getOrNull()`, `firstOrNull()` 등)

---

## 코루틴 예외 처리

> **원문:** https://kotlinlang.org/docs/exception-handling.html

### 개요

이 문서는 Kotlin 코루틴에서의 예외 처리와 취소를 다룹니다. 취소된 코루틴이 일시 중단 지점에서 `CancellationException`을 던지는 방법과 취소 중 예외가 발생하거나 여러 자식이 예외를 던질 때 어떤 일이 발생하는지 설명합니다.

---

### 예외 전파

코루틴 빌더는 두 가지 유형이 있습니다:

1. **예외를 자동으로 전파**: `launch`
2. **예외를 사용자에게 노출**: `async`와 `produce`

루트 코루틴을 생성할 때 사용하면, `launch`는 예외를 처리되지 않은 것으로 취급하고 (Java의 `Thread.uncaughtExceptionHandler`와 유사), `async`와 `produce`는 사용자가 `await()` 또는 `receive()`를 통해 예외를 소비하도록 합니다.

#### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val job = GlobalScope.launch { // launch를 사용한 루트 코루틴
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException() // 콘솔에 출력됨
    }
    job.join()
    println("Joined failed job")

    val deferred = GlobalScope.async { // async를 사용한 루트 코루틴
        println("Throwing exception from async")
        throw ArithmeticException() // 아무것도 출력되지 않음, 사용자가 await 호출해야 함
    }
    try {
        deferred.await()
        println("Unreached")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}
```

**출력:**
```
Throwing exception from launch
Exception in thread "DefaultDispatcher-worker-1 @coroutine#2" java.lang.IndexOutOfBoundsException
Joined failed job
Throwing exception from async
Caught ArithmeticException
```

---

### CoroutineExceptionHandler

`CoroutineExceptionHandler`는 루트 코루틴과 그 자식들의 처리되지 않은 예외 처리를 사용자 정의하는 데 사용되는 컨텍스트 요소입니다. 일반적인 `catch` 블록으로 작동합니다.

#### 핵심 포인트:
- 핸들러에서 예외를 **복구할 수 없음**
- **처리되지 않은 예외에만 호출됨**
- 자식 코루틴은 예외 처리를 계층 구조를 따라 부모에게 위임
- `async` 빌더는 결과 `Deferred` 객체에서 모든 예외를 잡으므로 핸들러가 효과 없음

#### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    val job = GlobalScope.launch(handler) { // 루트 코루틴
        throw AssertionError()
    }

    val deferred = GlobalScope.async(handler) { // 루트이지만 async
        throw ArithmeticException() // 아무것도 출력되지 않음
    }

    joinAll(job, deferred)
}
```

**출력:**
```
CoroutineExceptionHandler got java.lang.AssertionError
```

---

### 취소와 예외

`CancellationException`은 취소에 내부적으로 사용되며 모든 핸들러에서 무시됩니다.

#### 핵심 동작:
- `Job.cancel()`로 코루틴이 취소되면 종료되지만 부모를 취소하지 않음
- 코루틴이 `CancellationException` 이외의 예외를 던지면 해당 예외로 부모를 취소
- 이 동작은 구조적 동시성을 위한 안정적인 코루틴 계층을 보장

#### 예제:
```kotlin
fun main() = runBlocking {
    val job = launch {
        val child = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child is cancelled")
            }
        }
        yield()
        println("Cancelling child")
        child.cancel()
        child.join()
        yield()
        println("Parent is not cancelled")
    }
    job.join()
}
```

**출력:**
```
Cancelling child
Child is cancelled
Parent is not cancelled
```

---

### 예외 집계

여러 자식이 실패할 때:
- **첫 번째 예외가 우선**하여 처리됨
- **추가 예외**는 억제된 예외로 첨부됨

#### 예제:
```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception with suppressed ${exception.suppressed.contentToString()}")
    }

    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw ArithmeticException() // 두 번째 예외
            }
        }
        launch {
            delay(100)
            throw IOException() // 첫 번째 예외
        }
        delay(Long.MAX_VALUE)
    }
    job.join()
}
```

**출력:**
```
CoroutineExceptionHandler got java.io.IOException with suppressed [java.lang.ArithmeticException]
```

#### 취소 예외 투명성:
취소 예외는 기본적으로 언래핑되어 원래 예외가 핸들러에 도달할 수 있습니다:

```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    val job = GlobalScope.launch(handler) {
        val innerJob = launch {
            launch {
                launch {
                    throw IOException()
                }
            }
        }
        try {
            innerJob.join()
        } catch (e: CancellationException) {
            println("Rethrowing CancellationException with original cause")
            throw e
        }
    }
    job.join()
}
```

**출력:**
```
Rethrowing CancellationException with original cause
CoroutineExceptionHandler got java.io.IOException
```

---

### 슈퍼비전

단방향 취소가 필요한 경우, 슈퍼비전은 다음을 허용합니다:
- 부모 취소가 자식에게 영향
- 자식 실패가 부모에게 영향을 주지 않음
- 자식 실패가 형제를 취소하지 않음

#### SupervisorJob

`SupervisorJob`은 일반 `Job`과 유사하지만 취소가 아래쪽으로만 전파됩니다.

```kotlin
fun main() = runBlocking {
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
            println("The first child is failing")
            throw AssertionError("The first child is cancelled")
        }

        val secondChild = launch {
            firstChild.join()
            println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("The second child is cancelled because the supervisor was cancelled")
            }
        }

        firstChild.join()
        println("Cancelling the supervisor")
        supervisor.cancel()
        secondChild.join()
    }
}
```

**출력:**
```
The first child is failing
The first child is cancelled: true, but the second one is still active
Cancelling the supervisor
The second child is cancelled because the supervisor was cancelled
```

#### supervisorScope

`supervisorScope`는 단방향 취소로 범위 지정된 동시성을 제공합니다:

```kotlin
fun main() = runBlocking {
    try {
        supervisorScope {
            val child = launch {
                try {
                    println("The child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("The child is cancelled")
                }
            }
            yield()
            println("Throwing an exception from the scope")
            throw AssertionError()
        }
    } catch(e: AssertionError) {
        println("Caught an assertion error")
    }
}
```

**출력:**
```
The child is sleeping
Throwing an exception from the scope
The child is cancelled
Caught an assertion error
```

#### 슈퍼비전된 코루틴의 예외

각 자식은 `CoroutineExceptionHandler`를 통해 독립적으로 예외를 처리합니다. 자식 실패는 부모에게 전파되지 않습니다.

```kotlin
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    supervisorScope {
        val child = launch(handler) {
            println("The child throws an exception")
            throw AssertionError()
        }
        println("The scope is completing")
    }
    println("The scope is completed")
}
```

**출력:**
```
The scope is completing
The child throws an exception
CoroutineExceptionHandler got java.lang.AssertionError
The scope is completed
```

---

### 요약

| 기능 | 동작 |
|------|------|
| `launch` | 예외를 자동으로 전파 |
| `async` | `await()`를 통해 예외 노출 |
| `CoroutineExceptionHandler` | 루트 코루틴의 처리되지 않은 예외 처리 |
| `SupervisorJob` | 단방향 취소 (부모 -> 자식만) |
| `supervisorScope` | 단방향 취소를 가진 범위 지정된 동시성 |
| 예외 집계 | 첫 번째 예외가 우선; 나머지는 억제됨 |
