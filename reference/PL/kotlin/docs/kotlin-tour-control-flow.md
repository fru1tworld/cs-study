# Kotlin 투어: 제어 흐름

## 개요

Kotlin은 조건부 표현식에 기반한 결정을 내리고 루프를 생성하고 순회하는 메커니즘을 제공합니다. 이 페이지는 `if`/`when` 표현식, 범위, `for`/`while` 루프를 다룹니다.

---

## 조건부 표현식

### if 문

괄호와 중괄호를 사용한 기본 구문:

```kotlin
val d: Int
val check = true
if (check) {
    d = 1
} else {
    d = 2
}
println(d) // 1
```

삼항 연산자 없이 표현식으로 사용 가능:

```kotlin
val a = 1
val b = 2
println(if (a > b) a else b) // 2
```

### when 표현식

**권장 사항:** 더 나은 가독성과 적은 실수를 위해 `if` 대신 `when`을 사용하세요.

**문으로서:**

```kotlin
val obj = "Hello"
when (obj) {
    "1" -> println("One")
    "Hello" -> println("Greeting")
    else -> println("Unknown")
}
// 출력: Greeting
```

**표현식으로 (값 반환):**

```kotlin
val obj = "Hello"
val result = when (obj) {
    "1" -> "One"
    "Hello" -> "Greeting"
    else -> "Unknown"
}
println(result) // Greeting
```

**주제 없이 (불리언 표현식):**

```kotlin
val trafficLightState = "Red"
val trafficAction = when {
    trafficLightState == "Green" -> "Go"
    trafficLightState == "Yellow" -> "Slow down"
    trafficLightState == "Red" -> "Stop"
    else -> "Malfunction"
}
```

---

## 범위

| 연산자 | 예제 | 동등한 값 |
|-------|------|----------|
| `..` | `1..4` | `1, 2, 3, 4` |
| `..<` | `1..<4` | `1, 2, 3` |
| `downTo` | `4 downTo 1` | `4, 3, 2, 1` |
| `step` | `1..5 step 2` | `1, 3, 5` |

**문자 범위:**

```kotlin
'a'..'d'           // 'a', 'b', 'c', 'd'
'z' downTo 's' step 2  // 'z', 'x', 'v', 't'
```

---

## 루프

### for 루프

범위와 컬렉션을 순회:

```kotlin
// 범위 순회
for (number in 1..5) {
    print(number)
}
// 출력: 12345

// 컬렉션 순회
val cakes = listOf("carrot", "cheese", "chocolate")
for (cake in cakes) {
    println("Yummy, it's a $cake cake!")
}
```

### while 루프

**표준 while:**

```kotlin
var cakesEaten = 0
while (cakesEaten < 3) {
    println("Eat a cake")
    cakesEaten++
}
```

**do-while (먼저 실행, 그 다음 검사):**

```kotlin
var cakesBaked = 0
do {
    println("Bake a cake")
    cakesBaked++
} while (cakesBaked < 3)
```

---

## 해답이 있는 연습 문제

### 연습 1: 주사위 게임

```kotlin
import kotlin.random.Random

fun main() {
    val firstResult = Random.nextInt(6)
    val secondResult = Random.nextInt(6)
    if (firstResult == secondResult)
        println("You win :)")
    else
        println("You lose :(")
}
```

### 연습 2: 게임 콘솔 버튼

```kotlin
fun main() {
    val button = "A"
    println(
        when (button) {
            "A" -> "Yes"
            "B" -> "No"
            "X" -> "Menu"
            "Y" -> "Nothing"
            else -> "There is no such button"
        }
    )
}
```

### 연습 3: 피자 조각 (while 루프)

```kotlin
fun main() {
    var pizzaSlices = 0
    while (pizzaSlices < 7) {
        pizzaSlices++
        println("There's only $pizzaSlices slice/s of pizza :(")
    }
    pizzaSlices++
    println("There are $pizzaSlices slices of pizza. Hooray! We have a whole pizza! :D")
}
```

### 연습 4: Fizz Buzz

```kotlin
fun main() {
    for (number in 1..100) {
        println(
            when {
                number % 15 == 0 -> "fizzbuzz"
                number % 3 == 0 -> "fizz"
                number % 5 == 0 -> "buzz"
                else -> "$number"
            }
        )
    }
}
```

### 연습 5: 'l'로 시작하는 단어 필터링

```kotlin
fun main() {
    val words = listOf("dinosaur", "limousine", "magazine", "language")
    for (w in words) {
        if (w.startsWith("l"))
            println(w)
    }
}
// 출력: limousine, language
```

---

## 핵심 요점

- 여러 조건에는 **`when`이 선호됨**
- **범위**는 `..`, `..<`, `downTo`, `step` 같은 연산자로 루프 순회를 단순화
- **`for` 루프**는 범위와 컬렉션 순회에 이상적
- **`while`과 `do-while`**은 조건 기반 반복에 유용
- 모든 조건부 구조는 값을 반환하는 표현식으로 사용 가능
