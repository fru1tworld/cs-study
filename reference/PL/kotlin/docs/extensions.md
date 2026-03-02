# 확장

Kotlin의 확장을 사용하면 상속이나 데코레이터와 같은 디자인 패턴을 사용하지 않고도 클래스나 인터페이스에 새로운 기능을 추가할 수 있습니다. 직접 수정할 수 없는 서드파티 라이브러리 작업 시 특히 유용합니다.

**핵심 포인트:** 확장은 원본 클래스를 수정하지 않습니다; 멤버와 동일한 문법을 사용하여 새 함수를 호출할 수 있게 합니다.

## 수신자

확장은 항상 **수신자**(확장된 타입의 인스턴스)에서 호출됩니다. 수신자 뒤에 `.`이 붙고 그 뒤에 함수나 프로퍼티 이름이 옵니다.

**예제:**
```kotlin
fun main() {
    val builder = StringBuilder()
    builder
        .appendLine("Hello")
        .appendLine()
        .appendLine("World")
    println(builder.toString())
    // Hello
    //
    // World
}
```

## 확장 함수

### 기본 확장 함수

수신자 타입 뒤에 `.`을 붙이고 함수 이름을 접두어로 사용하여 확장 함수를 만듭니다.

**예제 - String 확장:**
```kotlin
fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) this
           else take(maxLength - 3) + "..."
}

fun main() {
    val shortUsername = "KotlinFan42"
    val longUsername = "JetBrainsLoverForever"
    println("Short username: ${shortUsername.truncate(15)}")  // KotlinFan42
    println("Long username: ${longUsername.truncate(15)}")    // JetBrainsLov...
}
```

**예제 - 인터페이스 확장:**
```kotlin
interface User {
    val name: String
    val email: String
}

fun User.displayInfo(): String = "User(name=$name, email=$email)"

class RegularUser(override val name: String, override val email: String) : User

fun main() {
    val user = RegularUser("Alice", "alice@example.com")
    println(user.displayInfo())  // User(name=Alice, email=alice@example.com)
}
```

**예제 - 제네릭 타입 확장:**
```kotlin
fun Map<String, Int>.mostVoted(): String? {
    return maxByOrNull { (key, value) -> value }?.key
}

fun main() {
    val poll = mapOf("Cats" to 37, "Dogs" to 58, "Birds" to 22)
    println("Top choice: ${poll.mostVoted()}")  // Dogs
}
```

### 제네릭 확장 함수

함수 이름 앞에 제네릭 타입 매개변수를 선언합니다:

```kotlin
fun <T> List<T>.endpoints(): Pair<T, T> {
    return first() to last()
}

fun main() {
    val cities = listOf("Paris", "London", "Berlin", "Prague")
    val temperatures = listOf(21.0, 19.5, 22.3)

    val cityEndpoints = cities.endpoints()
    val tempEndpoints = temperatures.endpoints()

    println("First and last cities: $cityEndpoints")           // (Paris, Prague)
    println("First and last temperatures: $tempEndpoints")     // (21.0, 22.3)
}
```

### 널 가능 수신자

확장 함수는 널 가능 수신자 타입을 가질 수 있어, 값이 `null`일 때도 호출할 수 있습니다:

```kotlin
fun Any?.toString(): String {
    if (this == null) return "null"
    return toString()
}

fun main() {
    val number: Int? = 42
    val nothing: Any? = null
    println(number.toString())   // 42
    println(nothing.toString())  // null
}
```

### 확장 vs 멤버 함수

**해석 우선순위:**
1. **멤버 함수가 우선** - 동일한 이름과 시그니처를 가진 확장 함수보다 우선
2. **확장 함수는 정적으로 해석** - 컴파일 시 선언된 타입을 기준으로
3. **확장 함수로 오버로드 가능** - 다른 시그니처로

**예제 - 정적 해석:**
```kotlin
open class Shape
class Rectangle: Shape()

fun Shape.getName() = "Shape"
fun Rectangle.getName() = "Rectangle"

fun printClassName(shape: Shape) {
    println(shape.getName())  // 선언된 타입을 기준으로 Shape.getName() 호출
}

printClassName(Rectangle())  // 출력: Shape
```

**예제 - 멤버 함수 우선:**
```kotlin
class Example {
    fun printFunctionType() { println("Member function") }
}

fun Example.printFunctionType() { println("Extension function") }

Example().printFunctionType()  // 출력: Member function
```

**예제 - 다른 시그니처로 오버로드:**
```kotlin
class Example {
    fun printFunctionType() { println("Member function") }
}

fun Example.printFunctionType(index: Int) {
    println("Extension function #$index")
}

Example().printFunctionType(1)  // 출력: Extension function #1
```

### 익명 확장 함수

네임스페이스 혼잡을 피하기 위해 이름 없는 확장 함수를 정의합니다:

```kotlin
data class Order(val weight: Double)

val calculateShipping = fun Order.(rate: Double): Double = this.weight * rate

fun main() {
    val order = Order(2.5)
    val cost = order.calculateShipping(3.0)
    println("Shipping cost: $cost")  // Shipping cost: 7.5
}
```

**람다 표현식 사용:**
```kotlin
val isInRange: Int.(min: Int, max: Int) -> Boolean = { min, max ->
    this in min..max
}

println(5.isInRange(1, 10))   // true
println(20.isInRange(1, 10))  // false
```

## 확장 프로퍼티

지원 필드 없이 확장 프로퍼티를 만듭니다. 명시적인 게터와 세터가 필요합니다.

**예제 - 읽기 전용 프로퍼티:**
```kotlin
data class User(val firstName: String, val lastName: String)

val User.emailUsername: String
    get() = "${firstName.lowercase()}.${lastName.lowercase()}"

fun main() {
    val user = User("Mickey", "Mouse")
    println("Generated email username: ${user.emailUsername}")  // mickey.mouse
}
```

**예제 - 커스텀 게터/세터가 있는 가변 프로퍼티:**
```kotlin
data class House(val streetName: String)

val houseNumbers = mutableMapOf<House, Int>()

var House.number: Int
    get() = houseNumbers[this] ?: 1
    set(value) {
        println("Setting house number for ${this.streetName} to $value")
        houseNumbers[this] = value
    }

fun main() {
    val house = House("Maple Street")
    println("Default number: ${house.number} ${house.streetName}")  // 1 Maple Street
    house.number = 99  // Setting house number for Maple Street to 99
    println("Updated number: ${house.number} ${house.streetName}") // 99 Maple Street
}
```

## 동반 객체 확장

클래스 이름을 한정자로 사용하여 동반 객체에 대한 확장을 정의합니다:

```kotlin
class Logger {
    companion object { }
}

fun Logger.Companion.logStartupMessage() {
    println("Application started.")
}

fun main() {
    Logger.logStartupMessage()  // Application started.
}
```

## 멤버로서의 확장 선언

다른 클래스 내부에 선언된 확장은 **두 개의 암시적 수신자**를 가집니다:
- **디스패치 수신자:** 확장이 선언된 클래스
- **확장 수신자:** 확장 함수의 수신자 타입

```kotlin
class Host(val hostname: String) {
    fun printHostname() { print(hostname) }
}

class Connection(val host: Host, val port: Int) {
    fun printPort() { print(port) }

    fun Host.printConnectionString() {
        printHostname()      // Host.printHostname() 호출
        print(":")
        printPort()          // Connection.printPort() 호출
    }

    fun connect() {
        host.printConnectionString()
    }
}

fun main() {
    Connection(Host("kotl.in"), 443).connect()  // kotl.in:443
    // Host("kotl.in").printConnectionString()  // 오류: Connection 외부에서 사용 불가
}
```

### 명시적 수신자 접근

두 수신자 모두 동일한 이름의 멤버를 가질 때 한정된 `this` 문법을 사용합니다:

```kotlin
class Connection {
    fun Host.getConnectionString() {
        toString()                    // Host.toString() 호출
        this@Connection.toString()    // Connection.toString() 호출
    }
}
```

### 멤버 확장 오버라이딩

멤버 확장은 `open`일 수 있으며 서브클래스에서 오버라이드할 수 있습니다:

```kotlin
open class User
class Admin : User()

open class NotificationSender {
    open fun User.sendNotification() {
        println("Sending user notification from normal sender")
    }
    open fun Admin.sendNotification() {
        println("Sending admin notification from normal sender")
    }
    fun notify(user: User) {
        user.sendNotification()
    }
}

class SpecialNotificationSender : NotificationSender() {
    override fun User.sendNotification() {
        println("Sending user notification from special sender")
    }
    override fun Admin.sendNotification() {
        println("Sending admin notification from special sender")
    }
}

fun main() {
    NotificationSender().notify(User())
    // 출력: Sending user notification from normal sender

    SpecialNotificationSender().notify(User())
    // 출력: Sending user notification from special sender

    SpecialNotificationSender().notify(Admin())
    // 출력: Sending user notification from special sender
    // (notify() 매개변수가 User로 선언되어 User.sendNotification() 사용)
}
```

**핵심 포인트:**
- **디스패치 수신자**는 **런타임**에 해석됨 (가상 디스패치)
- **확장 수신자**는 **컴파일 시**에 해석됨 (정적 디스패치)

## 확장과 가시성 수정자

확장은 일반 함수와 동일한 가시성 수정자를 사용합니다:

**예제 - private 멤버 접근:**
```kotlin
// 파일: StringUtils.kt
private fun removeWhitespace(input: String): String {
    return input.replace("\\s".toRegex(), "")
}

fun String.cleaned(): String {
    return removeWhitespace(this)
}

fun main() {
    val rawEmail = " user @example. com "
    val cleaned = rawEmail.cleaned()
    println("Raw: '$rawEmail'")        // Raw: ' user @example. com '
    println("Cleaned: '$cleaned'")     // Cleaned: 'user@example.com'
}
```

**예제 - 클래스 외부의 확장은 private 멤버에 접근 불가:**
```kotlin
class User(private val password: String) {
    fun isLoggedIn(): Boolean = true
    fun passwordLength(): Int = password.length
}

fun User.isSecure(): Boolean {
    // password에 접근 불가 (private)
    return passwordLength() >= 8 && isLoggedIn()
}

fun main() {
    val user = User("supersecret")
    println("Is user secure: ${user.isSecure()}")  // true
}
```

**Internal 확장:**
```kotlin
// 같은 모듈 내에서만 접근 가능
internal fun String.parseJson(): Map<String, Any> {
    return mapOf("fakeKey" to "fakeValue")
}
```

## 확장의 스코프

확장은 일반적으로 패키지 아래 최상위 수준에서 정의됩니다:

```kotlin
// org/example/declarations/StringUtils.kt
package org.example.declarations

fun List<String>.getLongestString() { /*...*/ }
```

**다른 패키지의 확장 사용:**
```kotlin
// org/example/usage/Main.kt
package org.example.usage

import org.example.declarations.getLongestString

fun main() {
    val list = listOf("red", "green", "blue")
    list.getLongestString()
}
```

## 표준 라이브러리 확장

Kotlin 표준 라이브러리는 많은 유용한 확장을 제공합니다:

- **컬렉션 연산:** `.map()`, `.filter()`, `.reduce()`, `.fold()`, `.groupBy()`
- **문자열 변환:** `.joinToString()`
- **널 처리:** `.filterNotNull()`

## 요약

| 특징 | 세부사항 |
|------|----------|
| **목적** | 상속 없이 클래스/인터페이스 확장 |
| **문법** | `fun ReceiverType.functionName() { }` |
| **해석** | 확장 수신자는 정적, 디스패치 수신자는 가상 |
| **멤버 우선** | 멤버 함수가 확장 함수를 오버라이드 |
| **널 가능 수신자** | 적절한 널 처리로 지원 |
| **스코프** | 최상위 또는 클래스 멤버로 |
| **가시성** | 일반 함수와 동일한 규칙 따름 |
