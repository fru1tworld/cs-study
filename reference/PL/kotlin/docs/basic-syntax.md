# Kotlin 기본 문법

## 패키지 정의와 임포트

패키지 명세는 소스 파일 상단에 위치해야 합니다:

```kotlin
package my.demo
import kotlin.text.*
// ...
```

소스 파일은 디렉토리 구조와 일치시키지 않고 파일 시스템의 임의 위치에 배치할 수 있습니다.

---

## 프로그램 진입점

```kotlin
fun main() {
    println("Hello world!")
}
```

인수를 받는 대안적 형태:

```kotlin
fun main(args: Array<String>) {
    println(args.contentToString())
}
```

---

## 표준 출력으로 출력

```kotlin
print("Hello ")  // 줄 바꿈 없이 출력
print("world!")

println("Hello world!")  // 줄 바꿈과 함께 출력
println(42)
```

---

## 표준 입력에서 읽기

```kotlin
println("Enter any word: ")
val yourWord = readln()  // 전체 라인을 String으로 읽음
print("You entered the word: ")
print(yourWord)
```

---

## 함수

**명시적 반환 타입 사용:**

```kotlin
fun sum(a: Int, b: Int): Int {
    return a + b
}
```

**추론된 반환 타입 (표현식 본문):**

```kotlin
fun sum(a: Int, b: Int) = a + b
```

**Unit 반환 (의미 있는 값 없음):**

```kotlin
fun printSum(a: Int, b: Int) {
    println("sum of $a and $b is ${a + b}")
}
```

---

## 변수

**불변 (읽기 전용) 변수 - `val`:**

```kotlin
val x: Int = 5  // 명시적 타입
val x = 5       // 타입 추론
```

**가변 변수 - `var`:**

```kotlin
var x: Int = 5
x += 1  // 재할당 가능
```

**지연 초기화:**

```kotlin
val c: Int      // 타입 필수
c = 3           // 나중에 초기화
```

**최상위 변수:**

```kotlin
val PI = 3.14
var x = 0
fun incrementX() { x += 1 }
```

---

## 클래스와 인스턴스 생성

**기본 클래스 정의:**

```kotlin
class Shape

class Rectangle(val height: Double, val length: Double) {
    val perimeter = (height + length) * 2
}
```

**인스턴스 생성:**

```kotlin
val rectangle = Rectangle(5.0, 2.0)
println("The perimeter is ${rectangle.perimeter}")
```

**상속:**

```kotlin
open class Shape
class Rectangle(val height: Double, val length: Double): Shape() {
    val perimeter = (height + length) * 2
}
```

---

## 주석

**한 줄 주석:**

```kotlin
// 이것은 줄 끝 주석입니다
```

**블록 주석 (중첩 가능):**

```kotlin
/* 이것은 여러 줄에 걸친
   블록 주석입니다. */

/* 주석은 여기서 시작하고
   /* 중첩된 주석을 포함하며 */
   여기서 끝납니다.
*/
```

---

## 문자열 템플릿

```kotlin
var a = 1
val s1 = "a is $a"           // 단순 변수
a = 2
val s2 = "${s1.replace("is", "was")}, but now is $a"  // 표현식
```

---

## 조건부 표현식

**if 문:**

```kotlin
fun maxOf(a: Int, b: Int): Int {
    if (a > b) {
        return a
    } else {
        return b
    }
}
```

**표현식으로서의 if:**

```kotlin
fun maxOf(a: Int, b: Int) = if (a > b) a else b
```

---

## for 루프

**컬렉션 순회:**

```kotlin
val items = listOf("apple", "banana", "kiwifruit")
for (item in items) {
    println(item)
}
```

**인덱스와 함께 순회:**

```kotlin
for (index in items.indices) {
    println("item at $index is ${items[index]}")
}
```

---

## while 루프

```kotlin
val items = listOf("apple", "banana", "kiwifruit")
var index = 0
while (index < items.size) {
    println("item at $index is ${items[index]}")
    index++
}
```

---

## when 표현식

```kotlin
fun describe(obj: Any): String = when (obj) {
    1 -> "One"
    "Hello" -> "Greeting"
    is Long -> "Long"
    !is String -> "Not a string"
    else -> "Unknown"
}
```

---

## 범위

**범위 내 확인:**

```kotlin
val x = 10
if (x in 1..y+1) {
    println("fits in range")
}
```

**범위 외 확인:**

```kotlin
if (-1 !in 0..list.lastIndex) {
    println("-1 is out of range")
}
```

**범위 순회:**

```kotlin
for (x in 1..5) {
    print(x)  // 1 2 3 4 5 출력
}
```

**스텝과 함께 순회:**

```kotlin
for (x in 1..10 step 2) {
    print(x)  // 1 3 5 7 9 출력
}
for (x in 9 downTo 0 step 3) {
    print(x)  // 9 6 3 0 출력
}
```

---

## 컬렉션

**컬렉션 순회:**

```kotlin
val items = listOf("apple", "banana", "kiwifruit")
for (item in items) {
    println(item)
}
```

**`in`으로 멤버십 확인:**

```kotlin
val items = setOf("apple", "banana", "kiwifruit")
when {
    "orange" in items -> println("juicy")
    "apple" in items -> println("apple is fine too")
}
```

**필터, 정렬, 맵, forEach:**

```kotlin
val fruits = listOf("banana", "avocado", "apple", "kiwifruit")
fruits
    .filter { it.startsWith("a") }
    .sortedBy { it }
    .map { it.uppercase() }
    .forEach { println(it) }
```

---

## Nullable 값과 null 검사

**Nullable 타입 선언:**

```kotlin
fun parseInt(str: String): Int? {
    return str.toIntOrNull()
}
```

**&&로 null 검사:**

```kotlin
fun printProduct(arg1: String, arg2: String) {
    val x = parseInt(arg1)
    val y = parseInt(arg2)
    if (x != null && y != null) {
        println(x * y)  // 자동으로 non-nullable로 캐스트
    } else {
        println("'$arg1' or '$arg2' is not a number")
    }
}
```

**null일 때 조기 반환:**

```kotlin
if (x == null) {
    println("Wrong number format")
    return
}
println(x * y)  // 여기서 x는 non-nullable
```

---

## 타입 검사와 자동 캐스트

**`is` 연산자 사용:**

```kotlin
fun getStringLength(obj: Any): Int? {
    if (obj is String) {
        return obj.length  // 자동으로 String으로 캐스트
    }
    return null
}
```

**`!is` 연산자 사용:**

```kotlin
if (obj !is String) return null
return obj.length  // 여기서 obj는 String
```

**논리 표현식에서의 스마트 캐스트:**

```kotlin
if (obj is String && obj.length >= 0) {
    return obj.length
}
```
