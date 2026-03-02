# Kotlin 투어: 함수

## 개요

Kotlin에서 함수는 `fun` 키워드를 사용하여 선언합니다. 이 페이지는 함수 선언, 매개변수, 반환 타입, 그리고 람다 표현식 같은 고급 개념을 다룹니다.

## 기본 함수 문법

**핵심 규칙:**
- 함수 매개변수는 괄호 `()` 안에 위치
- 각 매개변수에는 쉼표로 구분된 타입이 필요
- 반환 타입은 `()` 뒤에 콜론 `:`과 함께 작성
- 함수 본문은 중괄호 `{}` 안에 위치
- 함수를 종료/반환하려면 `return` 키워드 사용

**예제:**

```kotlin
fun sum(x: Int, y: Int): Int {
    return x + y
}

fun main() {
    println(sum(1, 2)) // 3
}
```

## 명명된 인수

매개변수는 이름으로 호출할 수 있어 어떤 순서도 가능하고 가독성이 향상됩니다:

```kotlin
fun printMessageWithPrefix(message: String, prefix: String) {
    println("[$prefix] $message")
}

fun main() {
    printMessageWithPrefix(prefix = "Log", message = "Hello") // [Log] Hello
}
```

## 기본 매개변수 값

타입 뒤에 `=`를 사용하여 기본값 정의:

```kotlin
fun printMessageWithPrefix(message: String, prefix: String = "Info") {
    println("[$prefix] $message")
}

fun main() {
    printMessageWithPrefix("Hello", "Log")     // [Log] Hello
    printMessageWithPrefix("Hello")            // [Info] Hello
    printMessageWithPrefix(prefix = "Log", message = "Hello") // [Log] Hello
}
```

## 반환 없는 함수

함수가 유용한 값을 반환하지 않으면 반환 타입은 `Unit`입니다. `return` 키워드와 반환 타입은 선택사항입니다:

```kotlin
fun printMessage(message: String) {
    println(message)
}

fun main() {
    printMessage("Hello") // Hello
}
```

## 단일 표현식 함수

`=`를 사용하고 중괄호를 생략하여 함수 단순화:

```kotlin
fun sum(x: Int, y: Int) = x + y

fun main() {
    println(sum(1, 2)) // 3
}
```

## 함수에서의 조기 반환

함수를 일찍 종료하려면 `return` 사용:

```kotlin
val registeredUsernames = mutableListOf("john_doe", "jane_smith")
val registeredEmails = mutableListOf("john@example.com", "jane@example.com")

fun registerUser(username: String, email: String): String {
    if (username in registeredUsernames) {
        return "Username already taken. Please choose a different username."
    }
    if (email in registeredEmails) {
        return "Email already registered. Please use a different email."
    }
    registeredUsernames.add(username)
    registeredEmails.add(email)
    return "User registered successfully: $username"
}
```

## 함수 연습 문제

### 연습 1: 원의 면적

```kotlin
import kotlin.math.PI

fun circleArea(radius: Int): Double {
    return PI * radius * radius
}

fun main() {
    println(circleArea(2)) // 12.566370614359172
}
```

### 연습 2: 단일 표현식 원의 면적

```kotlin
import kotlin.math.PI

fun circleArea(radius: Int): Double = PI * radius * radius

fun main() {
    println(circleArea(2)) // 12.566370614359172
}
```

### 연습 3: 기본값을 가진 시간 간격

```kotlin
fun intervalInSeconds(hours: Int = 0, minutes: Int = 0, seconds: Int = 0) =
    ((hours * 60) + minutes) * 60 + seconds

fun main() {
    println(intervalInSeconds(1, 20, 15))      // 4815
    println(intervalInSeconds(minutes = 1, seconds = 25))  // 85
    println(intervalInSeconds(hours = 2))      // 7200
    println(intervalInSeconds(minutes = 10))   // 600
    println(intervalInSeconds(hours = 1, seconds = 1))  // 3601
}
```

---

## 람다 표현식

람다 표현식은 간결한 함수 작성을 가능하게 합니다. 문법: `{ 매개변수 -> 본문 }`

**예제:**

```kotlin
val upperCaseString = { text: String -> text.uppercase() }

fun main() {
    println(upperCaseString("hello")) // HELLO
}
```

### 다른 함수에 람다 전달

**`.filter()` 사용:**

```kotlin
fun main() {
    val numbers = listOf(1, -2, 3, -4, 5, -6)
    val positives = numbers.filter { x -> x > 0 }
    val isNegative = { x: Int -> x < 0 }
    val negatives = numbers.filter(isNegative)

    println(positives) // [1, 3, 5]
    println(negatives) // [-2, -4, -6]
}
```

**`.map()` 사용:**

```kotlin
fun main() {
    val numbers = listOf(1, -2, 3, -4, 5, -6)
    val doubled = numbers.map { x -> x * 2 }
    val isTripled = { x: Int -> x * 3 }
    val tripled = numbers.map(isTripled)

    println(doubled) // [2, -4, 6, -8, 10, -12]
    println(tripled) // [3, -6, 9, -12, 15, -18]
}
```

### 함수 타입

함수 타입 문법: `(매개변수타입) -> 반환타입`

예제: `(String) -> String`, `(Int, Int) -> Int`, `() -> Unit`

```kotlin
val upperCaseString: (String) -> String = { text -> text.uppercase() }

fun main() {
    println(upperCaseString("hello")) // HELLO
}
```

### 함수에서 람다 반환

```kotlin
fun toSeconds(time: String): (Int) -> Int = when (time) {
    "hour" -> { value -> value * 60 * 60 }
    "minute" -> { value -> value * 60 }
    "second" -> { value -> value }
    else -> { value -> value }
}

fun main() {
    val timesInMinutes = listOf(2, 10, 15, 1)
    val min2sec = toSeconds("minute")
    val totalTimeInSeconds = timesInMinutes.map(min2sec).sum()
    println("Total time is $totalTimeInSeconds secs") // Total time is 1680 secs
}
```

### 람다 별도로 호출

```kotlin
fun main() {
    println({ text: String -> text.uppercase() }("hello")) // HELLO
}
```

### 후행 람다

람다가 마지막 매개변수인 경우 함수 괄호 밖에 작성:

```kotlin
fun main() {
    println(listOf(1, 2, 3).fold(0, { x, item -> x + item })) // 6

    // 후행 람다 형태
    println(listOf(1, 2, 3).fold(0) { x, item -> x + item }) // 6
}
```

---

## 람다 표현식 연습 문제

### 연습 1: 람다를 사용한 URL 생성

```kotlin
fun main() {
    val actions = listOf("title", "year", "author")
    val prefix = "https://example.com/book-info"
    val id = 5
    val urls = actions.map { action -> "$prefix/$id/$action" }
    println(urls)
    // [https://example.com/book-info/5/title, https://example.com/book-info/5/year, https://example.com/book-info/5/author]
}
```

### 연습 2: 동작 반복 함수

```kotlin
fun repeatN(n: Int, action: () -> Unit) {
    for (i in 1..n) {
        action()
    }
}

fun main() {
    repeatN(5) { println("Hello") }
}
```

---

## 다음 단계

[Kotlin의 클래스](kotlin-tour-classes.html)
