# 제어 흐름과 예외 처리

# Kotlin 조건문과 루프

> **원문:** https://kotlinlang.org/docs/control-flow.html

## 개요

Kotlin은 `if`, `when`, 그리고 다양한 루프 구조를 사용하여 프로그램 흐름을 제어하는 유연한 도구를 제공합니다.

---

## if 표현식

### 기본 사용법

괄호 `()` 안에 조건을, 중괄호 `{}` 안에 동작을 사용합니다:

```kotlin
if (heightAlice < heightBob) {
    taller = heightBob
}
```

### 표현식으로서의 if

`if`는 값을 직접 할당하는 표현식으로 사용할 수 있습니다 (삼항 연산자를 대체):

```kotlin
val taller = if (heightAlice > heightBob) heightAlice else heightBob
```

### else if 체인

```kotlin
val heightOrLimit = if (heightLimit > heightAlice) heightLimit
                    else if (heightAlice > heightBob) heightAlice
                    else heightBob
```

### 블록 표현식

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

## when 표현식과 문

### 주제가 있는 기본 when

```kotlin
val userRole = "Editor"
when (userRole) {
    "Viewer" -> print("User has read-only access")
    "Editor" -> print("User can edit content")
    else -> print("User role is not recognized")
}
```

### 표현식 vs 문으로서의 when

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

### 주제 없는 when

```kotlin
val message = when {
    localFileSize > remoteFileSize -> "Local file is larger"
    localFileSize < remoteFileSize -> "Local file is smaller"
    else -> "Files are the same size"
}
```

### 완전성

- **문**: 모든 경우를 다룰 필요 없음
- **표현식**: 모든 경우를 다뤄야 함 (그렇지 않으면 컴파일러가 오류 발생)
- **예외**: 열거형, Boolean, 또는 sealed 클래스는 모든 경우가 다뤄지면 `else`가 필요 없음

### 여러 조건 (쉼표로 구분)

```kotlin
when (ticketPriority) {
    "Low", "Medium" -> print("Standard response time")
    else -> print("High-priority handling")
}
```

### 조건으로 표현식 사용

```kotlin
when (enteredPin) {
    storedPin.toInt() -> print("PIN is correct")
    else -> print("Incorrect PIN")
}
```

### 범위와 컬렉션 검사

```kotlin
when (x) {
    in 1..10 -> print("x is in the range")
    in validNumbers -> print("x is valid")
    !in 10..20 -> print("x is outside the range")
    else -> print("none of the above")
}
```

### 타입 검사

```kotlin
fun hasPrefix(input: Any): Boolean = when (input) {
    is String -> input.startsWith("ID-")
    else -> false
}
```

### 변수에 주제 캡처

```kotlin
val message = when (val input = "yes") {
    "yes" -> "You said yes"
    "no" -> "You said no"
    else -> "Unrecognized input: $input"
}
```

### 가드 조건

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

## for 루프

### 기본 for 루프

```kotlin
for (item in collection) {
    println(item)
}
```

### 범위

```kotlin
// 닫힌 범위 (포함)
for (i in 1..6) print(i)  // 123456

// 열린 범위 (제외)
for (i in 1..<6) print(i)  // 12345

// 역순과 스텝
for (i in 6 downTo 0 step 2) print(i)  // 6420
```

### 인덱스가 있는 배열

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

### 사용자 정의 반복자

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

## while 루프

### while 루프

먼저 조건을 검사한 후 본문을 실행합니다:

```kotlin
var carsInGarage = 0
val maxCapacity = 3

while (carsInGarage < maxCapacity) {
    println("Car entered. Cars now in garage: ${++carsInGarage}")
}
```

### do-while 루프

먼저 본문을 실행한 후 조건을 검사합니다:

```kotlin
do {
    roll = Random.nextInt(1, 7)
    println("Rolled a $roll")
} while (roll != 6)
```

---

## break와 continue

Kotlin은 루프에서 전통적인 `break`와 `continue` 연산자를 지원합니다. 자세한 내용은 [반환과 점프](returns.html) 문서를 참조하세요.

---

# 반환과 점프

> **원문:** https://kotlinlang.org/docs/returns.html

## 개요

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

## break와 continue 레이블

Kotlin의 모든 표현식은 레이블로 표시될 수 있습니다. 레이블은 식별자 뒤에 `@` 기호가 오는 형태입니다 (예: `abc@` 또는 `fooBar@`).

### 예제:

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

## 레이블로 반환

함수는 함수 리터럴, 지역 함수, 객체 표현식을 사용하여 중첩될 수 있습니다. 한정된 `return`을 사용하면 외부 함수에서 반환할 수 있습니다.

### 명시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        if (it == 3) return@lit // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with explicit label")
}
```

### 암시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach {
        if (it == 3) return@forEach // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with implicit label")
}
```

### 익명 함수 사용:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach(fun(value: Int) {
        if (value == 3) return // 익명 함수에서의 지역 반환
        print(value)
    })
    print(" done with anonymous function")
}
```

### run 람다로 break 시뮬레이션:

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

## 핵심 사항

- 람다에서의 지역 반환은 일반 루프의 `continue`와 유사합니다
- `break`에 대한 직접적인 등가물은 없지만 외부 `run` 람다를 사용하여 시뮬레이션할 수 있습니다
- 값을 반환할 때 파서는 한정된 반환에 우선순위를 부여합니다:
  ```kotlin
  return@a 1  // 레이블 @a에서 1을 반환 (레이블이 지정된 표현식이 아님)
  ```
- 람다에서의 비지역 반환은 람다가 인라인 함수 역할을 할 때 가능합니다

---

# Kotlin 예외

> **원문:** https://kotlinlang.org/docs/exceptions.html

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

---

# 코루틴 예외 처리

> **원문:** https://kotlinlang.org/docs/exception-handling.html

## 개요

이 문서는 Kotlin 코루틴에서의 예외 처리와 취소를 다룹니다. 취소된 코루틴이 일시 중단 지점에서 `CancellationException`을 던지는 방법과 취소 중 예외가 발생하거나 여러 자식이 예외를 던질 때 어떤 일이 발생하는지 설명합니다.

---

## 예외 전파

코루틴 빌더는 두 가지 유형이 있습니다:

1. **예외를 자동으로 전파**: `launch`
2. **예외를 사용자에게 노출**: `async`와 `produce`

루트 코루틴을 생성할 때 사용하면, `launch`는 예외를 처리되지 않은 것으로 취급하고 (Java의 `Thread.uncaughtExceptionHandler`와 유사), `async`와 `produce`는 사용자가 `await()` 또는 `receive()`를 통해 예외를 소비하도록 합니다.

### 예제:
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

## CoroutineExceptionHandler

`CoroutineExceptionHandler`는 루트 코루틴과 그 자식들의 처리되지 않은 예외 처리를 사용자 정의하는 데 사용되는 컨텍스트 요소입니다. 일반적인 `catch` 블록으로 작동합니다.

### 핵심 포인트:
- 핸들러에서 예외를 **복구할 수 없음**
- **처리되지 않은 예외에만 호출됨**
- 자식 코루틴은 예외 처리를 계층 구조를 따라 부모에게 위임
- `async` 빌더는 결과 `Deferred` 객체에서 모든 예외를 잡으므로 핸들러가 효과 없음

### 예제:
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

## 취소와 예외

`CancellationException`은 취소에 내부적으로 사용되며 모든 핸들러에서 무시됩니다.

### 핵심 동작:
- `Job.cancel()`로 코루틴이 취소되면 종료되지만 부모를 취소하지 않음
- 코루틴이 `CancellationException` 이외의 예외를 던지면 해당 예외로 부모를 취소
- 이 동작은 구조적 동시성을 위한 안정적인 코루틴 계층을 보장

### 예제:
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

## 예외 집계

여러 자식이 실패할 때:
- **첫 번째 예외가 우선**하여 처리됨
- **추가 예외**는 억제된 예외로 첨부됨

### 예제:
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

### 취소 예외 투명성:
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

## 슈퍼비전

단방향 취소가 필요한 경우, 슈퍼비전은 다음을 허용합니다:
- 부모 취소가 자식에게 영향
- 자식 실패가 부모에게 영향을 주지 않음
- 자식 실패가 형제를 취소하지 않음

### SupervisorJob

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

### supervisorScope

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

### 슈퍼비전된 코루틴의 예외

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

## 요약

| 기능 | 동작 |
|------|------|
| `launch` | 예외를 자동으로 전파 |
| `async` | `await()`를 통해 예외 노출 |
| `CoroutineExceptionHandler` | 루트 코루틴의 처리되지 않은 예외 처리 |
| `SupervisorJob` | 단방향 취소 (부모 -> 자식만) |
| `supervisorScope` | 단방향 취소를 가진 범위 지정된 동시성 |
| 예외 집계 | 첫 번째 예외가 우선; 나머지는 억제됨 |
