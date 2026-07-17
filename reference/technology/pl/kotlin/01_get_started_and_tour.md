# 시작하기 및 코틀린 투어

# Kotlin 시작하기

> **원문:** https://kotlinlang.org/docs/getting-started.html

## 개요

Kotlin은 다음과 같은 특징을 가진 현대적인 언어입니다:
- 간결함
- 멀티플랫폼
- Java 및 다른 언어들과의 상호 운용성

**시작하기**: 신규 사용자는 브라우저에서 직접 기본 사항을 배울 수 있는 인터랙티브 투어를 이용할 수 있습니다.

---

## Kotlin 설치

- Kotlin은 **IntelliJ IDEA**와 **Android Studio**에 포함되어 있습니다
- 두 IDE 중 하나를 다운로드하고 설치하여 Kotlin 사용을 시작하세요

---

## Kotlin 사용 사례 선택

### JVM/콘솔 애플리케이션
1. IntelliJ IDEA 프로젝트 마법사로 기본 JVM 애플리케이션 만들기
2. 첫 번째 단위 테스트 작성하기

### 백엔드 개발
기존 Java 프로젝트에 Kotlin 도입하기:
- Kotlin과 함께 작동하도록 Java 프로젝트 구성하기
- Java Maven 프로젝트에 Kotlin 테스트 추가하기

처음부터 백엔드 앱 만들기:
- Spring Boot로 RESTful 웹 서비스 만들기
- Ktor로 HTTP API 만들기

### 크로스 플랫폼 개발 (Kotlin Multiplatform)
1. 크로스 플랫폼 개발 환경 설정하기
2. iOS 및 Android 애플리케이션 만들기:
   - 네이티브 UI와 비즈니스 로직 공유
   - 비즈니스 로직과 UI 공유
   - 기존 Android 앱을 iOS로 마이그레이션
   - Ktor와 SQLdelight로 빌드하기
3. 샘플 프로젝트 탐색하기

### Android 개발
Android에서 Kotlin 시작에 대한 [Google의 권장 사항](https://developer.android.com/kotlin/get-started)을 따르세요

### 데이터 분석 및 머신 러닝
1. Kotlin Notebook으로 IDE에서 노트북 생성 및 편집하기
2. 다음 도구로 데이터 탐색:
   - **DataFrame** - 데이터 분석 및 조작 라이브러리
   - **Kandy** - 데이터 시각화 플로팅 도구


---

## 지원 받기
- **Slack**: [초대 받기](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
- **이슈 트래커**: [JetBrains YouTrack](https://youtrack.jetbrains.com/issues/KT)
- **피드백**: [피드백 공유하기](https://surveys.hotjar.com/d82e82b0-00d9-44a7-b793-0611bf6189df)

---

# Kotlin 기본 문법

> **원문:** https://kotlinlang.org/docs/basic-syntax.html

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

---

# Kotlin 패키지와 임포트

> **원문:** https://kotlinlang.org/docs/packages.html

## 개요

소스 파일은 코드를 네임스페이스로 구성하기 위해 패키지 선언으로 시작할 수 있습니다.

## 패키지 선언

```kotlin
package org.example

fun printMessage() { /*...*/ }
class Message { /*...*/ }
```

소스 파일의 모든 내용(클래스, 함수 등)은 선언된 패키지에 속합니다. 위 예제에서:
- `printMessage()`의 전체 이름: `org.example.printMessage`
- `Message`의 전체 이름: `org.example.Message`

패키지가 지정되지 않으면 내용은 이름이 없는 기본 패키지에 속합니다.

---

## 기본 임포트

다음 패키지들은 모든 Kotlin 파일에 자동으로 임포트됩니다:

- `kotlin.*`
- `kotlin.annotation.*`
- `kotlin.collections.*`
- `kotlin.comparisons.*`
- `kotlin.io.*`
- `kotlin.ranges.*`
- `kotlin.sequences.*`
- `kotlin.text.*`

### 플랫폼별 임포트

**JVM:**
- `java.lang.*`
- `kotlin.jvm.*`

**JS:**
- `kotlin.js.*`

---

## 임포트 지시문

### 단일 이름 임포트

```kotlin
import org.example.Message  // Message는 이제 한정자 없이 접근 가능
```

### 와일드카드 임포트

```kotlin
import org.example.*  // 'org.example'의 모든 것이 접근 가능
```

### 이름 충돌 처리

```kotlin
import org.example.Message           // Message에 접근 가능
import org.test.Message as TestMessage  // TestMessage는 'org.test.Message'를 나타냄
```

### 임포트 가능한 선언

- 최상위 함수와 프로퍼티
- 객체 선언에서 선언된 함수와 프로퍼티
- 열거형 상수

---

## 최상위 선언의 가시성

최상위 선언이 `private`으로 표시되면 해당 선언은 선언된 파일 내에서만 private입니다. (자세한 내용은 가시성 수정자를 참조하세요.)

---

# Kotlin 투어에 오신 것을 환영합니다

> **원문:** https://kotlinlang.org/docs/kotlin-tour-welcome.html

## 개요

이것은 공식 Kotlin 문서 투어의 메인 랜딩 페이지입니다. 브라우저 기반 인터랙티브 튜토리얼을 통해 Kotlin을 배울 수 있는 소개를 제공합니다.

**주요 목표:** 설치 없이 브라우저에서 Kotlin을 초급부터 중급 수준까지 배웁니다.

### 각 챕터의 구조

각 챕터에는 다음이 포함됩니다:
- **이론** - 예제와 함께 핵심 언어 개념
- **실습** - 이해도를 테스트하는 연습문제
- **해답** - 참고 솔루션

---

## 초급 투어

**다루는 주제:**
1. 변수
2. 기본 타입
3. 컬렉션
4. 제어 흐름
5. 함수
6. 클래스
7. null 안전성

**시작점:** [kotlin-tour-hello-world.html](kotlin-tour-hello-world.html)

---

## 중급 투어

**다루는 주제:**
1. 확장 함수
2. 스코프 함수
3. 수신자가 있는 람다 표현식
4. 클래스와 인터페이스
5. 객체
6. open 및 특수 클래스
7. 프로퍼티
8. null 안전성 (고급)
9. 라이브러리와 API

**시작점:** [kotlin-tour-intermediate-extension-functions.html](kotlin-tour-intermediate-extension-functions.html)

---

## 주요 특징

- 브라우저 기반 - 설치 불필요
- 점진적 학습 경로 (초급 -> 중급)
- 해답이 있는 실습 연습문제
- 이론 + 실습 조합

---

# Kotlin 투어: Hello World

> **원문:** https://kotlinlang.org/docs/kotlin-tour-hello-world.html

## 개요

이것은 기본 문법, 변수, 문자열 템플릿을 다루는 Kotlin 초보자 가이드입니다.

---

## 핵심 개념

### 함수

```kotlin
fun main() {
    println("Hello, world!")
}
```

**핵심 사항:**
- `fun`은 함수를 선언합니다
- `main()`은 프로그램 진입점입니다
- 함수 본문은 중괄호 `{}`로 묶습니다
- `println()`은 줄 바꿈과 함께 출력합니다
- `print()`는 줄 바꿈 없이 출력합니다

---

## 변수

Kotlin은 두 가지 유형의 변수를 지원합니다:

### 읽기 전용 변수 (`val`)

```kotlin
val popcorn = 5  // 변경할 수 없음
```

### 가변 변수 (`var`)

```kotlin
var customers = 10
customers = 8  // 재할당 가능
```

**모범 사례:** 기본적으로 모든 변수를 `val`로 선언하세요. 필요한 경우에만 `var`를 사용하세요.

---

## 문자열 템플릿

문자열 템플릿을 사용하면 `$`를 사용하여 변수와 표현식을 문자열에 포함할 수 있습니다:

### 단순 변수 접근

```kotlin
val customers = 10
println("There are $customers customers")
// 출력: There are 10 customers
```

### 표현식 평가

```kotlin
val customers = 10
println("There are ${customers + 1} customers")
// 출력: There are 11 customers
```

---

## 연습 문제

**과제:** 코드를 완성하여 "Mary is 20 years old"를 출력하세요

**해답:**

```kotlin
fun main() {
    val name = "Mary"
    val age = 20
    println("$name is $age years old")
}
```

---

## 추가 참고사항

- Kotlin은 **타입 추론**을 사용합니다 - 타입을 명시적으로 선언할 필요가 없습니다
- 위 예제에서 `age`는 `Int` 타입으로 추론됩니다
- 다음 주제: [기본 타입](kotlin-tour-basic-types.html)

---

# Kotlin 투어: 기본 타입

> **원문:** https://kotlinlang.org/docs/kotlin-tour-basic-types.html

## 개요

Kotlin의 모든 변수와 데이터 구조는 타입을 가집니다. 타입은 컴파일러에게 해당 변수나 데이터 구조에서 어떤 연산이 허용되는지 알려줍니다. 즉, 어떤 함수와 프로퍼티가 있는지를 나타냅니다.

## 타입 추론

Kotlin은 **타입 추론**을 사용하여 할당된 값을 기반으로 변수의 타입을 자동으로 결정합니다. 예를 들어:

```kotlin
var customers = 10  // Kotlin이 타입을 Int로 추론
```

## 산술 연산 예제

```kotlin
fun main() {
    var customers = 10
    customers = 8
    customers = customers + 3      // 덧셈: 11
    customers += 7                 // 복합 대입: 18
    customers -= 3                 // 뺄셈: 15
    customers *= 2                 // 곱셈: 30
    customers /= 3                 // 나눗셈: 10
    println(customers)             // 10
}
```

**복합 대입 연산자:** `+=`, `-=`, `*=`, `/=`, `%=`

## Kotlin의 기본 타입

| 범주 | 타입 | 예제 |
|------|------|------|
| **정수** | `Byte`, `Short`, `Int`, `Long` | `val year: Int = 2020` |
| **부호 없는 정수** | `UByte`, `UShort`, `UInt`, `ULong` | `val score: UInt = 100u` |
| **부동 소수점** | `Float`, `Double` | `val temp: Float = 24.5f`, `val price: Double = 19.99` |
| **불리언** | `Boolean` | `val isEnabled: Boolean = true` |
| **문자** | `Char` | `val separator: Char = ','` |
| **문자열** | `String` | `val message: String = "Hello, world!"` |

## 초기화 없이 변수 선언

변수는 즉시 초기화하지 않고 선언할 수 있지만, 첫 번째 사용 전에 반드시 초기화해야 합니다:

```kotlin
fun main() {
    val d: Int          // 초기화 없이 선언
    d = 3               // 초기화
    val e: String = "hello"
    println(d)          // 3
    println(e)          // hello
}
```

## 중요한 규칙

초기화되지 않은 변수를 읽으면 컴파일러 오류가 발생합니다:

```kotlin
val d: Int
println(d)  // 오류: 변수 'd'가 초기화되어야 합니다
```

## 연습 문제

**과제:** 각 변수에 올바른 타입을 명시적으로 선언하세요:

```kotlin
fun main() {
    val a: Int = 1000
    val b: String = "log message"
    val c: Double = 3.14
    val d: Long = 100_000_000_000_000
    val e: Boolean = false
    val f: Char = '\n'
}
```

## 다음 단계

[컬렉션](kotlin-tour-collections.html)

---

# Kotlin 투어: 제어 흐름

> **원문:** https://kotlinlang.org/docs/kotlin-tour-control-flow.html

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
- `while`과 `do-while`은 조건 기반 반복에 유용
- 모든 조건부 구조는 값을 반환하는 표현식으로 사용 가능

---

# Kotlin 투어: 함수

> **원문:** https://kotlinlang.org/docs/kotlin-tour-functions.html

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

---

# Kotlin 투어: 클래스

> **원문:** https://kotlinlang.org/docs/kotlin-tour-classes.html

## 개요

Kotlin은 클래스와 객체를 사용한 객체 지향 프로그래밍을 지원합니다. 클래스를 사용하면 객체의 특성 집합을 선언하여 반복적인 선언을 피함으로써 시간과 노력을 절약할 수 있습니다.

**기본 클래스 선언:**

```kotlin
class Customer
```

---

## 프로퍼티

프로퍼티는 클래스 객체의 특성을 선언합니다. 다음과 같이 선언할 수 있습니다:

1. **클래스 이름 뒤 괄호 안에 (클래스 헤더):**

   ```kotlin
   class Contact(val id: Int, var email: String)
   ```

2. **클래스 본문 내에 (중괄호):**

   ```kotlin
   class Contact(val id: Int, var email: String) {
       val category: String = ""
   }
   ```

**모범 사례:**
- 인스턴스 생성 후 수정이 필요하지 않으면 읽기 전용 프로퍼티(`val`) 사용
- `val`/`var` 없이 괄호 안에 있는 프로퍼티는 인스턴스 생성 후 접근 불가
- 프로퍼티는 기본값을 가질 수 있음

**기본값이 있는 예제:**

```kotlin
class Contact(val id: Int, var email: String = "example@gmail.com") {
    val category: String = "work"
}
```

---

## 인스턴스 생성

객체는 생성자를 사용하여 생성됩니다. Kotlin은 클래스 헤더 매개변수로 기본 생성자를 자동 생성합니다.

```kotlin
class Contact(val id: Int, var email: String)

fun main() {
    val contact = Contact(1, "mary@gmail.com")
}
```

---

## 프로퍼티 접근

점 표기법(`.`)을 사용하여 프로퍼티에 접근:

```kotlin
class Contact(val id: Int, var email: String)

fun main() {
    val contact = Contact(1, "mary@gmail.com")

    // 프로퍼티 읽기
    println(contact.email) // mary@gmail.com

    // 프로퍼티 수정
    contact.email = "jane@gmail.com"
    println(contact.email) // jane@gmail.com
}
```

**문자열 템플릿:**

```kotlin
println("Their email address is: ${contact.email}")
```

---

## 멤버 함수

클래스 본문에 선언된 멤버 함수로 객체 동작 정의:

```kotlin
class Contact(val id: Int, var email: String) {
    fun printId() {
        println(id)
    }
}

fun main() {
    val contact = Contact(1, "mary@gmail.com")
    contact.printId() // 1
}
```

---

## 데이터 클래스

데이터 클래스는 데이터 저장에 최적화되어 있으며 자동 생성된 유틸리티 함수가 함께 제공됩니다.

**선언:**

```kotlin
data class User(val name: String, val id: Int)
```

### 미리 정의된 멤버 함수

| 함수 | 설명 |
|------|------|
| `toString()` | 인스턴스와 프로퍼티의 읽기 쉬운 문자열 출력 |
| `equals()` 또는 `==` | 인스턴스 비교 |
| `copy()` | 선택적으로 다른 프로퍼티로 복사본 생성 |

### 문자열로 출력

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)
    println(user) // User(name=Alex, id=1)
}
```

### 인스턴스 비교

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)
    val secondUser = User("Alex", 1)
    val thirdUser = User("Max", 2)

    println("user == secondUser: ${user == secondUser}") // true
    println("user == thirdUser: ${user == thirdUser}") // false
}
```

### 인스턴스 복사

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)

    // 정확한 복사
    println(user.copy()) // User(name=Alex, id=1)

    // 이름 변경된 복사
    println(user.copy("Max")) // User(name=Max, id=1)

    // id 변경된 복사
    println(user.copy(id = 3)) // User(name=Alex, id=3)
}
```

---

## 연습 문제

### 연습 1: Employee 데이터 클래스

```kotlin
data class Employee(val name: String, var salary: Int)

fun main() {
    val emp = Employee("Mary", 20)
    println(emp) // Employee(name=Mary, salary=20)
    emp.salary += 10
    println(emp) // Employee(name=Mary, salary=30)
}
```

### 연습 2: 중첩 데이터 클래스

```kotlin
data class Person(val name: Name, val address: Address, val ownsAPet: Boolean = true)
data class Name(val first: String, val last: String)
data class Address(val street: String, val city: City)
data class City(val name: String, val countryCode: String)

fun main() {
    val person = Person(
        Name("John", "Smith"),
        Address("123 Fake Street", City("Springfield", "US")),
        ownsAPet = false
    )
}
```

### 연습 3: 랜덤 Employee 생성기

```kotlin
import kotlin.random.Random

data class Employee(val name: String, var salary: Int)

class RandomEmployeeGenerator(var minSalary: Int, var maxSalary: Int) {
    val names = listOf("John", "Mary", "Ann", "Paul", "Jack", "Elizabeth")

    fun generateEmployee() = Employee(
        names.random(),
        Random.nextInt(from = minSalary, until = maxSalary)
    )
}

fun main() {
    val empGen = RandomEmployeeGenerator(10, 30)
    println(empGen.generateEmployee())
    println(empGen.generateEmployee())
    println(empGen.generateEmployee())
    empGen.minSalary = 50
    empGen.maxSalary = 100
    println(empGen.generateEmployee())
}
```

---

## 다음 단계

- [null 안전성](kotlin-tour-null-safety.html) - Kotlin 투어의 마지막 챕터

---

# Kotlin 투어: null 안전성

> **원문:** https://kotlinlang.org/docs/kotlin-tour-null-safety.html

## 개요

Kotlin은 런타임이 아닌 컴파일 시점에 잠재적인 `null` 값 문제를 감지하는 내장 null 안전성 기능이 있습니다. 이는 일반적인 null 참조 오류를 방지합니다.

## null 안전성의 주요 기능

- `null` 값이 허용되는 경우 명시적으로 선언
- `null` 값 확인
- 잠재적으로 null인 프로퍼티/함수에 대한 안전 호출 사용
- 감지된 `null` 값에 대한 대체 동작 선언

---

## Nullable 타입

기본적으로 타입은 `null` 값을 보유할 수 없습니다. `null`을 허용하려면 타입 뒤에 `?`를 추가하세요:

```kotlin
fun main() {
    // Non-nullable String
    var neverNull: String = "This can't be null"
    neverNull = null  // 컴파일러 오류

    // Nullable String
    var nullable: String? = "You can keep a null here"
    nullable = null   // OK

    // 타입 추론은 non-nullable을 가정
    var inferredNonNull = "The compiler assumes non-nullable"
    inferredNonNull = null  // 컴파일러 오류

    fun strLength(notNull: String): Int = notNull.length
    println(strLength(neverNull))  // 18
    println(strLength(nullable))   // 컴파일러 오류
}
```

---

## null 값 확인

조건부 표현식을 사용하여 `null`을 확인:

```kotlin
fun describeString(maybeString: String?): String {
    if (maybeString != null && maybeString.length > 0) {
        return "String of length ${maybeString.length}"
    } else {
        return "Empty or null string"
    }
}

fun main() {
    val nullString: String? = null
    println(describeString(nullString))  // Empty or null string
}
```

---

## 안전 호출 연산자 (`?.`)

객체나 접근된 프로퍼티가 `null`이면 `null`을 반환합니다. 잠재적으로 null인 값에 접근할 때 오류를 방지합니다:

```kotlin
fun lengthString(maybeString: String?): Int? = maybeString?.length

fun main() {
    val nullString: String? = null
    println(lengthString(nullString))  // null
}
```

### 안전 호출 체이닝

```kotlin
person.company?.address?.country
```

### 안전 함수 호출

```kotlin
fun main() {
    val nullString: String? = null
    println(nullString?.uppercase())  // null (함수가 호출되지 않음)
}
```

---

## 엘비스 연산자 (`?:`)

왼쪽이 `null`인 경우 기본값을 제공합니다:

```kotlin
fun main() {
    val nullString: String? = null
    println(nullString?.length ?: 0)  // 0
}
```

**문법:** `표현식 ?: 기본값`

---

## 연습 문제

**과제:** 직원의 급여를 반환하거나 찾지 못하면 `0`을 반환하는 함수를 작성하세요.

```kotlin
data class Employee(val name: String, var salary: Int)

fun employeeById(id: Int) = when(id) {
    1 -> Employee("Mary", 20)
    2 -> null
    3 -> Employee("John", 21)
    4 -> Employee("Ann", 23)
    else -> null
}

fun salaryById(id: Int) = employeeById(id)?.salary ?: 0

fun main() {
    println((1..5).sumOf { id -> salaryById(id) })  // 64
}
```

---

## 요약

| 기능 | 목적 |
|------|------|
| `타입?` | nullable 타입 선언 |
| `!= null` | 조건문에서 null 확인 |
| `?.` | 안전 호출 연산자 |
| `?:` | 엘비스 연산자 (기본값 제공) |

---

# Kotlin 투어: 컬렉션

> **원문:** https://kotlinlang.org/docs/kotlin-tour-collections.html

## 개요

컬렉션은 나중에 처리하기 위해 데이터를 그룹화하는 데 사용되는 구조입니다. Kotlin은 세 가지 주요 컬렉션 타입을 제공합니다: **List**, **Set**, **Map**. 각각은 가변 또는 읽기 전용일 수 있습니다.

| 컬렉션 타입 | 설명 |
|------------|------|
| **List** | 순서가 있는 항목 컬렉션 |
| **Set** | 고유한 비정렬 항목 컬렉션 |
| **Map** | 키가 고유한 키-값 쌍의 집합 |

---

## List

**목적:** 순서대로 항목을 저장하고 중복을 허용합니다.

### 리스트 생성

**읽기 전용 리스트** `listOf()` 사용:

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println(readOnlyShapes) // [triangle, square, circle]
```

**가변 리스트** 명시적 타입과 함께 `mutableListOf()` 사용:

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
println(shapes) // [triangle, square, circle]
```

**가변 리스트의 읽기 전용 뷰:**

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
val shapesLocked: List<String> = shapes
```

### 리스트 항목 접근

**인덱스 접근 연산자 `[]` 사용:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("The first item in the list is: ${readOnlyShapes[0]}")
// The first item in the list is: triangle
```

**`.first()`와 `.last()` 함수 사용:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("The first item in the list is: ${readOnlyShapes.first()}")
// The first item in the list is: triangle
```

### 리스트 연산

**개수 얻기:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("This list has ${readOnlyShapes.count()} items") // This list has 3 items
```

**`in` 연산자로 항목 존재 확인:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("circle" in readOnlyShapes) // true
```

**가변 리스트에서 항목 추가/제거:**

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
shapes.add("pentagon")
println(shapes) // [triangle, square, circle, pentagon]
shapes.remove("pentagon")
println(shapes) // [triangle, square, circle]
```

---

## Set

**목적:** 고유하고 비정렬된 항목을 저장합니다.

### 세트 생성

**읽기 전용 세트** `setOf()` 사용:

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
// 중복은 자동으로 제거됨
println(readOnlyFruit) // [apple, banana, cherry]
```

**명시적 타입을 가진 가변 세트:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
println(fruit) // [apple, banana, cherry]
```

**가변 세트의 읽기 전용 뷰:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
val fruitLocked: Set<String> = fruit
```

### 세트 연산

**개수 얻기:**

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
println("This set has ${readOnlyFruit.count()} items") // This set has 3 items
```

**항목 존재 확인:**

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
println("banana" in readOnlyFruit) // true
```

**가변 세트에서 항목 추가/제거:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
fruit.add("dragonfruit")
println(fruit) // [apple, banana, cherry, dragonfruit]
fruit.remove("dragonfruit")
println(fruit) // [apple, banana, cherry]
```

---

## Map

**목적:** 각 키가 고유하고 정확히 하나의 값에 매핑되는 키-값 쌍을 저장합니다.

**주요 특성:**
- 모든 키는 고유해야 함
- 중복 값은 허용됨
- 키를 참조하여 값에 접근

### 맵 생성

**읽기 전용 맵** `mapOf()` 사용:

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu) // {apple=100, kiwi=190, orange=100}
```

**명시적 타입 선언을 가진 가변 맵:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(juiceMenu) // {apple=100, kiwi=190, orange=100}
```

**가변 맵의 읽기 전용 뷰:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
val juiceMenuLocked: Map<String, Int> = juiceMenu
```

### 맵 값 접근

**키와 함께 인덱스 접근 연산자 사용:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("The value of apple juice is: ${readOnlyJuiceMenu["apple"]}")
// The value of apple juice is: 100
```

**존재하지 않는 키에 접근하면 `null` 반환:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("The value of pineapple juice is: ${readOnlyJuiceMenu["pineapple"]}")
// The value of pineapple juice is: null
```

### 맵 연산

**인덱스 접근 연산자로 항목 추가:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
juiceMenu["coconut"] = 150
println(juiceMenu) // {apple=100, kiwi=190, orange=100, coconut=150}
```

**항목 제거:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
juiceMenu.remove("orange")
println(juiceMenu) // {apple=100, kiwi=190}
```

**개수 얻기:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("This map has ${readOnlyJuiceMenu.count()} key-value pairs") // This map has 3 key-value pairs
```

**`.containsKey()`로 키 존재 확인:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu.containsKey("kiwi")) // true
```

**키와 값 프로퍼티 얻기:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu.keys) // [apple, kiwi, orange]
println(readOnlyJuiceMenu.values) // [100, 190, 100]
```

**`in` 연산자로 키 또는 값 존재 확인:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("orange" in readOnlyJuiceMenu.keys) // true
println("orange" in readOnlyJuiceMenu) // true (대안 구문)
println(200 in readOnlyJuiceMenu.values) // false
```

---

## 연습 문제

### 연습 1: 총 숫자 세기

**과제:** 두 리스트의 총 숫자를 세세요.

```kotlin
// 해답
fun main() {
    val greenNumbers = listOf(1, 4, 23)
    val redNumbers = listOf(17, 2)
    val totalCount = greenNumbers.count() + redNumbers.count()
    println(totalCount)
}
```

### 연습 2: 프로토콜 지원 확인

**과제:** 요청된 프로토콜이 지원되는지 확인하세요 (대소문자 무시).

```kotlin
// 해답
fun main() {
    val SUPPORTED = setOf("HTTP", "HTTPS", "FTP")
    val requested = "smtp"
    val isSupported = requested.uppercase() in SUPPORTED
    println("Support for $requested: $isSupported")
}
```

**힌트:** 확인 전에 `.uppercase()` 함수를 사용하여 대문자로 변환하세요.

### 연습 3: 숫자를 단어로 매핑

**과제:** 숫자 1-3을 그 철자에 연관시키는 맵을 정의하세요.

```kotlin
// 해답
fun main() {
    val number2word = mapOf(1 to "one", 2 to "two", 3 to "three")
    val n = 2
    println("$n is spelt as '${number2word[n]}'")
}
```

---

## 핵심 개념

- **확장 함수:** 점 표기법을 사용하여 객체에서 호출되는 함수 (예: `.first()`, `.count()`)
- **프로퍼티:** 점 표기법을 사용하여 객체에서 접근되는 데이터 (예: `.keys`, `.values`)
- **타입 추론:** Kotlin이 컬렉션 요소 타입을 자동으로 추론
- **null 안전성:** 맵에서 존재하지 않는 키에 접근하면 `null` 반환

---

## 다음 단계

[제어 흐름](kotlin-tour-control-flow.html)

---

# 콘솔 앱 만들기 - Kotlin 튜토리얼

> **원문:** https://kotlinlang.org/docs/jvm-get-started.html

이 튜토리얼은 IntelliJ IDEA와 Kotlin을 사용하여 콘솔 애플리케이션을 만드는 방법을 안내합니다.

## 사전 준비

- 최신 버전의 [IntelliJ IDEA](https://www.jetbrains.com/idea/download/index.html)를 다운로드하고 설치하세요

## 프로젝트 생성

1. **새 프로젝트 대화상자 열기**: File | New | Project
2. 왼쪽 목록에서 **Kotlin** 선택
3. **프로젝트 설정 구성**:
   - 프로젝트 이름 지정 및 위치 설정
   - 버전 관리를 위해 선택적으로 "Create Git repository" 선택
4. **빌드 시스템 선택**:
   - **IntelliJ** (네이티브, 추가 다운로드 불필요)
   - **Maven 또는 Gradle** (복잡한 프로젝트용)
   - Gradle의 경우: 빌드 스크립트 언어로 Kotlin 또는 Groovy 선택
5. **JDK 선택**:
   - 설치된 JDK 목록에서 선택
   - 또는 "Add JDK"를 선택하여 경로 지정
   - 또는 설치되어 있지 않은 경우 "Download JDK" 선택
6. **샘플 코드 활성화** (선택 사항):
   - "Add sample code"는 "Hello World!" 예제를 생성
   - "Generate code with onboarding tips"는 유용한 주석을 추가
7. **Create 클릭**

### Gradle 구성 (선택한 경우)

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
    application
}
```

## 애플리케이션 생성

`src/main/kotlin`에서 `Main.kt`를 열고 사용자 입력을 요청하도록 수정합니다:

```kotlin
fun main() {
    println("What's your name?")
    val name = readln()
    println("Hello, $name!")
}
```

**핵심 개념**:
- `readln()` 함수는 사용자 입력을 읽습니다
- 문자열 템플릿은 보간을 위해 `$variableName` 구문을 사용합니다

## 애플리케이션 실행

1. 거터에서 녹색 **Run** 아이콘을 클릭합니다
2. **Run 'MainKt'** 항목을 선택합니다
3. Run 도구 창에서 출력을 확인합니다
4. 프롬프트가 표시되면 이름을 입력합니다

## 다음 단계

Kotlin 학습을 계속하세요:
- [Kotlin 투어](kotlin-tour-welcome.html) 참여하기
- [JetBrains Academy 플러그인](https://plugins.jetbrains.com/plugin/10081-jetbrains-academy)을 설치하고 [Kotlin Koans 과정](https://plugins.jetbrains.com/plugin/10081-jetbrains-academy/docs/learner-start-guide.html?section=Kotlin%20Koans) 완료하기
