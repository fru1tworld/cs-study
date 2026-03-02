# Kotlin 예외

## 개요

Kotlin의 예외는 런타임 오류가 발생할 때 코드가 예측 가능하게 실행되도록 도와줍니다. Kotlin은 **모든 예외를 기본적으로 체크되지 않은 예외로 취급**하여 명시적 선언 없이도 예외 처리를 단순화합니다.

예외 작업에는 두 가지 주요 동작이 포함됩니다:
- **예외 던지기**: 문제가 발생했음을 나타냄
- **예외 잡기**: 예기치 않은 예외를 수동으로 처리

---

## 예외 던지기

### 기본 예외 던지기

```kotlin
throw IllegalArgumentException()
```

### 사용자 정의 메시지와 원인 포함

```kotlin
val cause = IllegalStateException("Original cause: illegal state")
if (userInput < 0) {
    throw IllegalArgumentException("Input must be non-negative", cause)
}
```

---

## 전제 조건 함수로 예외 던지기

Kotlin은 자동 예외 던지기를 위한 세 가지 전제 조건 함수를 제공합니다:

| 함수 | 사용 사례 | 던지는 예외 |
|------|----------|------------|
| `require()` | 사용자 입력 유효성 검사 | `IllegalArgumentException` |
| `check()` | 객체/변수 상태 유효성 검사 | `IllegalStateException` |
| `error()` | 불법 상태 또는 조건 표시 | `IllegalStateException` |

### require() 함수

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

### check() 함수

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

### error() 함수

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

## try-catch 블록으로 예외 처리

### 기본 구조

```kotlin
try {
    // 예외를 던질 수 있는 코드
} catch (e: SomeException) {
    // 예외 처리 코드
}
```

### 표현식으로서의 try-catch

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

### 여러 catch 블록 (가장 구체적인 것 먼저)

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

## finally 블록

`finally` 블록은 성공 또는 예외 여부에 관계없이 **항상 실행**됩니다. 정리 작업에 사용됩니다.

### 기본 구조

```kotlin
try {
    // 예외를 던질 수 있는 코드
} catch (e: YourException) {
    // 예외 핸들러
} finally {
    // 항상 실행됨
}
```

### 완전한 예제

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

### `.use()`를 사용한 리소스 관리

자동으로 리소스 닫기 (`AutoClosable` 구현):

```kotlin
FileWriter("test.txt").use { writer ->
    writer.write("some text")
    // 블록 완료 후 자동으로 닫힘
}
```

### catch 없는 try-finally

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

## 사용자 정의 예외 만들기

### 기본 사용자 정의 예외

```kotlin
class MyException: Exception("My message")
```

### 내장 예외의 서브클래스

```kotlin
class NumberTooLargeException: ArithmeticException("My message")
```

### 사용자 정의 예외 계층

부모를 `open`으로 선언해야 합니다:

```kotlin
open class MyCustomException(message: String): Exception(message)
class SpecificCustomException: MyCustomException("Specific error message")
```

### 사용자 정의 예외 사용

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

### sealed 클래스를 사용한 예외 계층

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

## Nothing 타입

`Nothing`은 **절대 성공적으로 완료되지 않는** 함수/표현식을 나타내는 특별한 타입입니다 (항상 던지거나 무한 루프).

### 기본 사용

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

### TODO() 사용

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

## 예외 클래스

일반적인 예외 타입 (`RuntimeException`의 서브클래스):

### ArithmeticException

```kotlin
val example = 2 / 0 // ArithmeticException 던짐
```

### IndexOutOfBoundsException

```kotlin
val myList = mutableListOf(1, 2, 3)
myList.removeAt(3) // IndexOutOfBoundsException 던짐

// 더 안전한 대안
val element = myList.getOrNull(3)
println("Element at index 3: $element")
```

### NoSuchElementException

```kotlin
val emptyList = listOf<Int>()
val firstElement = emptyList.first() // NoSuchElementException 던짐

// 더 안전한 대안
val firstElement = emptyList.firstOrNull()
println("First element: $firstElement")
```

### NumberFormatException

```kotlin
val string = "This is not a number"
val number = string.toInt() // NumberFormatException 던짐

// 더 안전한 대안
val number = string.toIntOrNull()
println("Converted number: $number")
```

### NullPointerException

```kotlin
val text: String? = null
println(text!!.length) // NullPointerException 던짐
```

---

## 예외 계층

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

## 스택 트레이스

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

## Java, Swift, Objective-C와의 예외 상호 운용성

호출자에게 가능한 예외를 알리려면 `@Throws` 어노테이션을 사용하세요:

```kotlin
@Throws(IOException::class)
fun someFunction() {
    // 구현
}
```

이것은 체크된 예외와 체크되지 않은 예외를 구분하는 언어에서 Kotlin 코드를 호출할 때 도움이 됩니다.

---

## 핵심 사항 요약

- 모든 예외는 Kotlin에서 **체크되지 않음**
- 전제 조건에 `require()`, `check()`, `error()` 사용
- `catch` 블록을 가장 구체적인 것부터 가장 덜 구체적인 순으로 정렬
- `finally` 블록은 항상 실행됨
- 자동 리소스 정리에 `.use()` 사용
- `Exception`을 확장하여 사용자 정의 예외 생성
- `Nothing` 타입은 반환하지 않는 함수를 나타냄
- 예외 방지를 위해 더 안전한 대안 사용 (`getOrNull()`, `firstOrNull()` 등)
