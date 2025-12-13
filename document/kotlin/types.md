# Kotlin 타입 시스템

> 공식 문서: https://kotlinlang.org/docs/basic-types.html

## 개요

Kotlin에서 모든 것은 객체입니다. 모든 변수에서 멤버 함수와 프로퍼티를 호출할 수 있습니다. 일부 타입은 특별한 내부 표현을 가지지만 (숫자, 문자, 불리언 등은 런타임에 기본 값으로 표현됨) 사용자에게는 일반 클래스처럼 보입니다.

---

## 숫자 타입

### 정수 타입

| 타입 | 크기 (비트) | 최솟값 | 최댓값 |
|------|-------------|--------|--------|
| Byte | 8 | -128 | 127 |
| Short | 16 | -32,768 | 32,767 |
| Int | 32 | -2,147,483,648 | 2,147,483,647 |
| Long | 64 | -9,223,372,036,854,775,808 | 9,223,372,036,854,775,807 |

```kotlin
val byte: Byte = 127
val short: Short = 32767
val int: Int = 2147483647
val long: Long = 9223372036854775807L  // L 접미사

// 타입 추론
val inferredInt = 100        // Int
val inferredLong = 3000000000  // Long (Int 범위 초과)
```

### 부호 없는 정수 타입

| 타입 | 크기 (비트) | 최솟값 | 최댓값 |
|------|-------------|--------|--------|
| UByte | 8 | 0 | 255 |
| UShort | 16 | 0 | 65,535 |
| UInt | 32 | 0 | 4,294,967,295 |
| ULong | 64 | 0 | 18,446,744,073,709,551,615 |

```kotlin
val uByte: UByte = 255u
val uShort: UShort = 65535u
val uInt: UInt = 4294967295u
val uLong: ULong = 18446744073709551615uL
```

### 부동소수점 타입

| 타입 | 크기 (비트) | 유효 자릿수 | 지수 비트 | 소수 비트 |
|------|-------------|-------------|-----------|-----------|
| Float | 32 | 6-7 | 8 | 23 |
| Double | 64 | 15-16 | 11 | 52 |

```kotlin
val float: Float = 3.14f    // f 또는 F 접미사 필수
val double: Double = 3.14   // 기본값

// 과학적 표기법
val scientific = 1.5e10     // 1.5 × 10^10

// 특수 값
val inf = Double.POSITIVE_INFINITY
val negInf = Double.NEGATIVE_INFINITY
val nan = Double.NaN
```

### 숫자 리터럴

```kotlin
// 10진수
val decimal = 123

// 16진수
val hexadecimal = 0x0F  // 15

// 2진수
val binary = 0b00001011  // 11

// 8진수는 지원하지 않음

// 밑줄로 가독성 향상
val million = 1_000_000
val creditCard = 1234_5678_9012_3456L
val hexBytes = 0xFF_EC_DE_5E
```

### 숫자 변환

Kotlin에서 작은 타입은 큰 타입의 하위 타입이 아닙니다. 명시적 변환이 필요합니다.

```kotlin
val intValue: Int = 100

// 명시적 변환 필요
val longValue: Long = intValue.toLong()
val doubleValue: Double = intValue.toDouble()

// 변환 함수
val b: Byte = 1
val i: Int = b.toInt()
val l: Long = b.toLong()
val f: Float = b.toFloat()
val d: Double = b.toDouble()
val c: Char = i.toChar()
val s: Short = i.toShort()

// 산술 연산에서는 자동 확장
val result = 1L + 3  // Long + Int -> Long
```

### 숫자 연산

```kotlin
// 정수 나눗셈
println(5 / 2)        // 2 (정수)
println(5 / 2.0)      // 2.5 (Double)
println(5.0 / 2)      // 2.5 (Double)

// 비트 연산 (Int, Long만)
val x = 0b1010
println(x shl 1)      // 20 (왼쪽 시프트)
println(x shr 1)      // 5 (오른쪽 시프트)
println(x ushr 1)     // 5 (부호 없는 오른쪽 시프트)
println(x and 0b1100) // 8 (AND)
println(x or 0b0001)  // 11 (OR)
println(x xor 0b1111) // 5 (XOR)
println(x.inv())      // -11 (NOT)
```

---

## Boolean 타입

```kotlin
val isTrue: Boolean = true
val isFalse: Boolean = false

// 논리 연산
val and = true && false  // false (지연 평가)
val or = true || false   // true (지연 평가)
val not = !true          // false

// nullable Boolean
val nullableBoolean: Boolean? = null
if (nullableBoolean == true) {
    println("true")
} else {
    println("false or null")
}
```

---

## 문자 타입

```kotlin
val char: Char = 'A'

// 특수 문자
val tab = '\t'
val newline = '\n'
val carriage = '\r'
val backslash = '\\'
val quote = '\''
val doubleQuote = '\"'
val dollar = '\$'

// 유니코드
val unicode = '\u0041'  // 'A'

// 문자 연산
val next = 'A' + 1      // 'B'
val code = 'A'.code     // 65
val digit = '5'.digitToInt()  // 5
```

---

## 문자열 타입

### 기본 문자열

```kotlin
val str: String = "Hello, World!"

// 문자 접근
println(str[0])       // 'H'
println(str.first())  // 'H'
println(str.last())   // '!'

// 순회
for (c in str) {
    println(c)
}

// 불변성
// str[0] = 'h'  // 컴파일 에러
```

### 문자열 템플릿

```kotlin
val name = "Kotlin"
val version = 2.0

// 변수 참조
println("Hello, $name!")

// 표현식
println("Version: ${version + 0.1}")
println("Length: ${name.length}")

// 이스케이프
println("\$name = $name")  // $name = Kotlin
```

### 여러 줄 문자열 (Raw String)

```kotlin
val text = """
    첫 번째 줄
    두 번째 줄
    세 번째 줄
"""

// 여백 제거
val trimmed = """
    |첫 번째 줄
    |두 번째 줄
    |세 번째 줄
""".trimMargin()

// 커스텀 마진 문자
val custom = """
    #Line 1
    #Line 2
""".trimMargin("#")

// trimIndent
val indented = """
    Line 1
    Line 2
    Line 3
""".trimIndent()
```

### 문자열 연산

```kotlin
val s1 = "Hello"
val s2 = "World"

// 연결
val concat = s1 + " " + s2  // "Hello World"

// 비교
println(s1 == "Hello")   // true (내용 비교)
println(s1 === "Hello")  // true (문자열 풀)

// 유용한 함수들
println(s1.length)              // 5
println(s1.uppercase())         // "HELLO"
println(s1.lowercase())         // "hello"
println(s1.reversed())          // "olleH"
println(s1.startsWith("He"))    // true
println(s1.endsWith("lo"))      // true
println(s1.contains("ell"))     // true
println(s1.indexOf("l"))        // 2
println(s1.substring(1, 4))     // "ell"
println(s1.replace("l", "L"))   // "HeLLo"
println("  hello  ".trim())     // "hello"
println(s1.repeat(3))           // "HelloHelloHello"

// 분할과 결합
val parts = "a,b,c".split(",")  // ["a", "b", "c"]
val joined = parts.joinToString("-")  // "a-b-c"
```

---

## 배열

### 기본 배열

```kotlin
// arrayOf로 생성
val array = arrayOf(1, 2, 3, 4, 5)

// 크기와 초기화 함수로 생성
val squares = Array(5) { i -> (i + 1) * (i + 1) }
// [1, 4, 9, 16, 25]

// 빈 배열
val empty = emptyArray<Int>()

// 접근
println(array[0])      // 1
println(array.get(0))  // 1

// 수정
array[0] = 10
array.set(1, 20)

// 크기
println(array.size)    // 5

// 순회
for (item in array) {
    println(item)
}

for ((index, value) in array.withIndex()) {
    println("$index: $value")
}
```

### 원시 타입 배열

박싱 오버헤드 없이 원시 타입을 저장합니다.

```kotlin
// IntArray, ByteArray, ShortArray, LongArray
// FloatArray, DoubleArray, CharArray, BooleanArray

val intArray: IntArray = intArrayOf(1, 2, 3)
val byteArray: ByteArray = byteArrayOf(1, 2, 3)
val charArray: CharArray = charArrayOf('a', 'b', 'c')

// 크기로 생성 (0으로 초기화)
val zeros = IntArray(5)  // [0, 0, 0, 0, 0]

// 람다로 초기화
val cubes = IntArray(5) { i -> i * i * i }
// [0, 1, 8, 27, 64]

// 변환
val boxed: Array<Int> = intArray.toTypedArray()
val primitive: IntArray = boxed.toIntArray()
```

### 배열 연산

```kotlin
val arr = arrayOf(1, 2, 3, 4, 5)

// 포함 여부
println(3 in arr)           // true
println(arr.contains(3))    // true

// 정렬
arr.sort()                  // 제자리 정렬
val sorted = arr.sortedArray()  // 새 배열 반환

// 비교
val arr1 = arrayOf(1, 2, 3)
val arr2 = arrayOf(1, 2, 3)
println(arr1 contentEquals arr2)  // true

// 복사
val copy = arr.copyOf()
val partial = arr.copyOfRange(1, 3)

// 채우기
arr.fill(0)  // 모든 요소를 0으로

// 변환
val list = arr.toList()
val set = arr.toSet()
```

---

## Any, Unit, Nothing

### Any

모든 non-nullable 타입의 최상위 타입입니다.

```kotlin
val anything: Any = "Hello"
val anyNumber: Any = 42

// 기본 메서드
println(anything.toString())
println(anything.hashCode())
println(anything.equals("Hello"))

// Any?는 nullable 타입 포함
val nullable: Any? = null
```

### Unit

반환 값이 없는 함수의 반환 타입입니다. Java의 `void`와 유사하지만, `Unit`은 실제 객체입니다.

```kotlin
fun printHello(): Unit {
    println("Hello")
}

// Unit 생략 가능
fun printWorld() {
    println("World")
}

// Unit은 싱글톤
val unit: Unit = Unit
println(unit)  // kotlin.Unit
```

### Nothing

절대 값을 반환하지 않는 함수의 반환 타입입니다. 항상 예외를 던지거나 무한 루프인 함수에 사용됩니다.

```kotlin
fun fail(message: String): Nothing {
    throw IllegalStateException(message)
}

fun infiniteLoop(): Nothing {
    while (true) {
        // 무한 루프
    }
}

// Nothing은 모든 타입의 하위 타입
val x: String = throw Exception()  // 컴파일 OK

// Elvis 연산자와 함께 사용
val name: String = nullableName ?: fail("Name required")

// Nothing?의 유일한 값은 null
val nothing: Nothing? = null
```

---

## 타입 별칭 (Type Aliases)

기존 타입에 대한 대체 이름을 제공합니다.

```kotlin
// 긴 제네릭 타입에 별칭
typealias UserMap = Map<String, List<User>>
typealias Predicate<T> = (T) -> Boolean

// 함수 타입에 별칭
typealias ClickHandler = (Button, Event) -> Unit

// 내부 클래스에 별칭
typealias MyInner = Outer.Inner

// 사용
fun filter(predicate: Predicate<String>) {
    // ...
}

val userMap: UserMap = mapOf(
    "admins" to listOf(User("admin")),
    "users" to listOf(User("user1"), User("user2"))
)
```

---

## 타입 추론

Kotlin은 강력한 타입 추론을 제공합니다.

```kotlin
// 변수
val name = "Kotlin"        // String
val count = 42             // Int
val price = 19.99          // Double
val items = listOf(1, 2)   // List<Int>

// 함수 반환 타입
fun double(x: Int) = x * 2  // 반환 타입: Int

// 람다
val sum = { a: Int, b: Int -> a + b }  // (Int, Int) -> Int

// 제네릭
val list = mutableListOf<String>()  // 타입 인자 필요한 경우
val numbers = listOf(1, 2, 3)        // List<Int> 추론
```

---

## 참고 문서

- [Basic Types](https://kotlinlang.org/docs/basic-types.html)
- [Numbers](https://kotlinlang.org/docs/numbers.html)
- [Strings](https://kotlinlang.org/docs/strings.html)
- [Arrays](https://kotlinlang.org/docs/arrays.html)
- [Type aliases](https://kotlinlang.org/docs/type-aliases.html)
