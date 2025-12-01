# Kotlin 기본 문법

> 공식 문서: https://kotlinlang.org/docs/basic-syntax.html

## 개요

Kotlin은 JetBrains에서 개발한 현대적인 프로그래밍 언어입니다. JVM, Android, JavaScript, Native를 타겟으로 하며, Java와 100% 상호 운용이 가능합니다.

---

## 패키지와 임포트

```kotlin
package org.example

import kotlin.text.*
import kotlin.collections.List as KList  // 별칭 사용
```

- 패키지 선언은 파일 최상단에 위치
- 디렉토리와 패키지가 일치할 필요 없음
- 기본 임포트: `kotlin.*`, `kotlin.annotation.*`, `kotlin.collections.*`, `kotlin.comparisons.*`, `kotlin.io.*`, `kotlin.ranges.*`, `kotlin.sequences.*`, `kotlin.text.*`

---

## 프로그램 진입점

```kotlin
fun main() {
    println("Hello, World!")
}

// 명령줄 인자 받기
fun main(args: Array<String>) {
    println(args.contentToString())
}
```

---

## 변수 선언

### val (Immutable - 읽기 전용)

```kotlin
val name: String = "John"
val age = 25  // 타입 추론
val PI = 3.14159  // 상수처럼 사용

// 지연 초기화
val lazyValue: String by lazy {
    println("computed!")
    "Hello"
}
```

### var (Mutable - 변경 가능)

```kotlin
var count = 0
count += 1

var message: String
message = "Hello"  // 나중에 초기화
```

### const val (컴파일 타임 상수)

```kotlin
// 최상위 레벨 또는 object에서만 사용 가능
const val MAX_SIZE = 100
const val API_URL = "https://api.example.com"

object Config {
    const val TIMEOUT = 5000
}
```

**val vs const val:**
- `val`: 런타임에 값 할당 가능
- `const val`: 컴파일 타임에 값이 결정되어야 함 (기본 타입과 String만 가능)

---

## 주석

```kotlin
// 단일 행 주석

/* 여러 줄
   주석 */

/**
 * KDoc 문서화 주석
 * @param name 사용자 이름
 * @return 인사 메시지
 */
fun greet(name: String): String = "Hello, $name"
```

---

## 문자열 템플릿

```kotlin
val name = "John"
val age = 25

// 변수 참조
println("Name: $name")

// 표현식
println("Age next year: ${age + 1}")
println("Name length: ${name.length}")

// 여러 줄 문자열
val text = """
    |첫 번째 줄
    |두 번째 줄
    |세 번째 줄
""".trimMargin()

// 커스텀 마진
val customMargin = """
    #Line 1
    #Line 2
""".trimMargin("#")
```

---

## 조건문

### if 표현식

```kotlin
// 전통적인 사용
if (a > b) {
    println("a is greater")
} else {
    println("b is greater")
}

// 표현식으로 사용 (삼항 연산자 대체)
val max = if (a > b) a else b

// 블록으로 사용
val max = if (a > b) {
    println("Choose a")
    a  // 마지막 표현식이 반환값
} else {
    println("Choose b")
    b
}
```

### when 표현식

```kotlin
// 기본 사용
when (x) {
    1 -> println("x == 1")
    2 -> println("x == 2")
    else -> println("x is neither 1 nor 2")
}

// 표현식으로 사용
val result = when (x) {
    1 -> "one"
    2 -> "two"
    else -> "other"
}

// 여러 조건
when (x) {
    0, 1 -> println("x == 0 or x == 1")
    in 2..10 -> println("x is between 2 and 10")
    !in 10..20 -> println("x is outside 10..20")
    else -> println("none of the above")
}

// 타입 검사
when (obj) {
    is String -> println("String of length ${obj.length}")
    is Int -> println("Int: $obj")
    is List<*> -> println("List of size ${obj.size}")
    else -> println("Unknown type")
}

// 조건 없이 사용 (if-else 체인 대체)
when {
    x.isOdd() -> println("x is odd")
    y.isEven() -> println("y is even")
    else -> println("x+y is odd")
}
```

---

## 반복문

### for 루프

```kotlin
// 범위
for (i in 1..5) {
    println(i)  // 1, 2, 3, 4, 5
}

// until (마지막 제외)
for (i in 1 until 5) {
    println(i)  // 1, 2, 3, 4
}

// step
for (i in 1..10 step 2) {
    println(i)  // 1, 3, 5, 7, 9
}

// 역순
for (i in 5 downTo 1) {
    println(i)  // 5, 4, 3, 2, 1
}

// 컬렉션 순회
val items = listOf("apple", "banana", "cherry")
for (item in items) {
    println(item)
}

// 인덱스와 함께
for ((index, value) in items.withIndex()) {
    println("$index: $value")
}
```

### while 루프

```kotlin
var x = 5
while (x > 0) {
    println(x)
    x--
}

// do-while
do {
    val input = readLine()
} while (input != "quit")
```

### 반복 제어

```kotlin
// break와 continue
for (i in 1..10) {
    if (i == 3) continue
    if (i == 8) break
    println(i)
}

// 레이블 사용
outer@ for (i in 1..3) {
    inner@ for (j in 1..3) {
        if (i == 2 && j == 2) break@outer
        println("$i, $j")
    }
}

// return with label
listOf(1, 2, 3, 4, 5).forEach {
    if (it == 3) return@forEach  // continue처럼 동작
    println(it)
}
```

---

## 범위 (Ranges)

```kotlin
// Int 범위
val range1 = 1..10        // 1부터 10까지 (10 포함)
val range2 = 1 until 10   // 1부터 9까지 (10 제외)
val range3 = 10 downTo 1  // 10부터 1까지
val range4 = 1..10 step 2 // 1, 3, 5, 7, 9

// Char 범위
val charRange = 'a'..'z'

// 포함 여부 확인
if (5 in range1) println("5 is in range")
if (11 !in range1) println("11 is not in range")

// 범위가 비어있는지 확인
val empty = 10..1  // 비어있는 범위
println(empty.isEmpty())  // true

// 범위 변환
val list = (1..5).toList()  // [1, 2, 3, 4, 5]
```

---

## 예외 처리

```kotlin
// try-catch
try {
    val result = riskyOperation()
} catch (e: NumberFormatException) {
    println("Invalid number: ${e.message}")
} catch (e: Exception) {
    println("Error: ${e.message}")
} finally {
    println("Cleanup")
}

// 표현식으로 사용
val number = try {
    input.toInt()
} catch (e: NumberFormatException) {
    0  // 기본값
}

// 예외 던지기
fun validate(value: Int) {
    if (value < 0) {
        throw IllegalArgumentException("Value must be non-negative")
    }
}

// Nothing 타입 (절대 반환하지 않음)
fun fail(message: String): Nothing {
    throw IllegalStateException(message)
}
```

** 참고:** Kotlin에는 checked exception이 없습니다. 모든 예외는 unchecked입니다.

---

## 연산자

### 산술 연산자

```kotlin
val sum = a + b
val diff = a - b
val product = a * b
val quotient = a / b
val remainder = a % b
```

### 비교 연산자

```kotlin
a == b   // 구조적 동등성 (equals)
a != b   // 구조적 부등성
a === b  // 참조 동등성 (같은 객체)
a !== b  // 참조 부등성
a > b    // 크다
a < b    // 작다
a >= b   // 크거나 같다
a <= b   // 작거나 같다
```

### 논리 연산자

```kotlin
a && b   // AND (지연 평가)
a || b   // OR (지연 평가)
!a       // NOT
```

### 비트 연산자 (함수로 제공)

```kotlin
val x = 0b1010
x shl 2   // 왼쪽 시프트: 0b101000
x shr 2   // 오른쪽 시프트: 0b10
x ushr 2  // 부호 없는 오른쪽 시프트
x and 0b1100  // AND
x or 0b1100   // OR
x xor 0b1100  // XOR
x.inv()       // NOT (비트 반전)
```

---

## 타입 검사와 캐스팅

```kotlin
// 타입 검사
if (obj is String) {
    println(obj.length)  // 스마트 캐스트
}

if (obj !is String) {
    println("Not a String")
}

// 안전한 캐스트
val str: String? = obj as? String  // 실패시 null

// 강제 캐스트 (ClassCastException 가능)
val str: String = obj as String
```

---

## 동등성

```kotlin
// 구조적 동등성 (값 비교)
val a = "hello"
val b = "hello"
println(a == b)   // true
println(a.equals(b))  // true

// 참조 동등성 (객체 비교)
val c = StringBuilder("hello")
val d = StringBuilder("hello")
println(c === d)  // false (다른 객체)

val e = c
println(c === e)  // true (같은 객체)
```

---

## 참고 문서

- [Basic Syntax](https://kotlinlang.org/docs/basic-syntax.html)
- [Idioms](https://kotlinlang.org/docs/idioms.html)
- [Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
