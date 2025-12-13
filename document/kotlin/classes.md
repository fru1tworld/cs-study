# Kotlin 클래스와 객체

> 공식 문서: https://kotlinlang.org/docs/classes.html

## 클래스 선언

### 기본 클래스

```kotlin
// 빈 클래스
class Empty

// 기본 생성자와 프로퍼티
class Person(val name: String, var age: Int)

// 사용
val person = Person("John", 25)
println(person.name)  // "John"
person.age = 26
```

### 생성자

```kotlin
// 주 생성자 (Primary Constructor)
class Person(val name: String) {
    // 초기화 블록
    init {
        println("Person created: $name")
    }
}

// 주 생성자에 어노테이션이나 가시성 수정자
class Customer public @Inject constructor(name: String) { }

// 부 생성자 (Secondary Constructor)
class Person(val name: String) {
    var children: MutableList<Person> = mutableListOf()

    // 부 생성자는 주 생성자에 위임해야 함
    constructor(name: String, parent: Person) : this(name) {
        parent.children.add(this)
    }
}

// 주 생성자 없이 부 생성자만
class Person {
    val name: String

    constructor(name: String) {
        this.name = name
    }
}
```

### 기본값과 명명된 인자

```kotlin
class Person(
    val name: String,
    val age: Int = 0,
    val email: String = ""
)

// 다양한 방식으로 생성
val p1 = Person("John")
val p2 = Person("Jane", 25)
val p3 = Person("Bob", email = "bob@example.com")
val p4 = Person(name = "Alice", age = 30, email = "alice@example.com")
```

---

## 프로퍼티

### 기본 프로퍼티

```kotlin
class Person {
    var name: String = ""
    val birthYear: Int = 1990
}

// 타입 추론
class Example {
    var count = 0  // Int
}
```

### Getter와 Setter

```kotlin
class Rectangle(val width: Int, val height: Int) {
    // 커스텀 getter
    val area: Int
        get() = width * height

    // 커스텀 getter (표현식)
    val isSquare get() = width == height
}

class Counter {
    var count: Int = 0
        private set  // setter를 private으로

    fun increment() {
        count++
    }
}

class User {
    var name: String = ""
        set(value) {
            field = value.trim()  // backing field 접근
        }

    var age: Int = 0
        set(value) {
            if (value >= 0) {
                field = value
            }
        }
}
```

### 지연 초기화

```kotlin
// lateinit (var만 가능, non-null 타입)
class MyService {
    lateinit var repository: Repository

    fun setup() {
        repository = Repository()
    }

    fun isInitialized(): Boolean = ::repository.isInitialized
}

// by lazy (val, 처음 접근 시 초기화)
class Config {
    val settings: Map<String, String> by lazy {
        loadSettings()  // 비용이 큰 연산
    }
}
```

### 컴파일 타임 상수

```kotlin
const val MAX_COUNT = 100  // 최상위 레벨

object Config {
    const val API_VERSION = "v1"  // object 내부
}

class MyClass {
    companion object {
        const val DEFAULT_NAME = "Unknown"  // companion object 내부
    }
}
```

---

## 상속

### 클래스 상속

```kotlin
// 기본적으로 클래스는 final
open class Base(val name: String) {
    open fun greet() = "Hello, $name"
}

class Derived(name: String) : Base(name) {
    override fun greet() = "Hi, $name!"
}

// 상속 막기
open class Shape {
    open fun draw() { }
}

class Circle : Shape() {
    final override fun draw() { }  // 더 이상 오버라이드 불가
}
```

### 추상 클래스

```kotlin
abstract class Animal {
    abstract val name: String
    abstract fun makeSound(): String

    // 구현된 메서드
    fun describe() = "$name says ${makeSound()}"
}

class Dog(override val name: String) : Animal() {
    override fun makeSound() = "Woof!"
}

class Cat(override val name: String) : Animal() {
    override fun makeSound() = "Meow!"
}
```

### 생성자 호출

```kotlin
open class Base(val x: Int) {
    open val y: Int = x
}

class Derived : Base {
    // 주 생성자에서 호출
    constructor(x: Int) : super(x)

    override val y = 42
}

// 또는
class Derived(x: Int) : Base(x) {
    override val y = 42
}
```

### super 호출

```kotlin
open class Rectangle {
    open fun draw() = println("Drawing rectangle")
}

class FilledRectangle : Rectangle() {
    override fun draw() {
        super.draw()  // 부모 메서드 호출
        println("Filling rectangle")
    }

    inner class Filler {
        fun fill() {
            super@FilledRectangle.draw()  // 외부 클래스의 부모 메서드
        }
    }
}
```

---

## 인터페이스

### 기본 인터페이스

```kotlin
interface Clickable {
    fun click()
    fun showOff() = println("I'm clickable!")  // 기본 구현
}

interface Focusable {
    fun setFocus(b: Boolean) = println("Focus: $b")
    fun showOff() = println("I'm focusable!")
}

class Button : Clickable, Focusable {
    override fun click() = println("Button clicked")

    // 충돌하는 기본 구현 해결
    override fun showOff() {
        super<Clickable>.showOff()
        super<Focusable>.showOff()
    }
}
```

### 프로퍼티가 있는 인터페이스

```kotlin
interface User {
    val name: String
    val email: String
        get() = "$name@example.com"  // 기본 구현
}

class PrivateUser(override val name: String) : User

class SubscribingUser(override val name: String) : User {
    override val email: String
        get() = "$name@subscribed.com"
}
```

---

## 가시성 수정자

| 수정자 | 클래스 멤버 | 최상위 선언 |
|--------|-------------|-------------|
| public (기본값) | 어디서나 접근 | 어디서나 접근 |
| private | 클래스 내부에서만 | 같은 파일에서만 |
| protected | 클래스 및 하위 클래스 | 최상위에 사용 불가 |
| internal | 같은 모듈 내 | 같은 모듈 내 |

```kotlin
open class Outer {
    private val a = 1
    protected val b = 2
    internal val c = 3
    val d = 4  // public

    protected class Nested {
        public val e = 5
    }
}

class Subclass : Outer() {
    // a는 접근 불가
    // b, c, d는 접근 가능
}

class Unrelated(o: Outer) {
    // o.a, o.b는 접근 불가
    // o.c는 같은 모듈이면 접근 가능
    // o.d는 접근 가능
}
```

---

## 데이터 클래스

```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// 자동 생성:
// - equals() / hashCode()
// - toString() -> "User(id=1, name=John, email=john@example.com)"
// - copy()
// - componentN() 함수

val user = User(1, "John", "john@example.com")

// copy
val updated = user.copy(name = "Jane")

// 구조 분해
val (id, name, email) = user
println("$id: $name ($email)")

// 컬렉션에서 사용
val users = listOf(user1, user2)
val found = users.find { it.id == 1L }
```

### 요구사항

- 주 생성자에 최소 하나의 파라미터
- 모든 주 생성자 파라미터는 `val` 또는 `var`
- `abstract`, `open`, `sealed`, `inner`가 될 수 없음

---

## Sealed 클래스

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// when에서 else 불필요 (모든 케이스 처리 시)
fun handleResult(result: Result<String>) = when (result) {
    is Result.Success -> println("Data: ${result.data}")
    is Result.Error -> println("Error: ${result.message}")
    Result.Loading -> println("Loading...")
}

// Sealed interface
sealed interface Error {
    data class NetworkError(val code: Int) : Error
    data class DatabaseError(val message: String) : Error
    data object UnknownError : Error
}
```

---

## 중첩 클래스와 내부 클래스

### 중첩 클래스 (Nested)

```kotlin
class Outer {
    private val bar: Int = 1

    class Nested {
        fun foo() = 2
        // bar에 접근 불가
    }
}

val nested = Outer.Nested()
println(nested.foo())  // 2
```

### 내부 클래스 (Inner)

```kotlin
class Outer {
    private val bar: Int = 1

    inner class Inner {
        fun foo() = bar  // 외부 클래스 멤버 접근 가능
        fun getOuterReference(): Outer = this@Outer
    }
}

val inner = Outer().Inner()
println(inner.foo())  // 1
```

---

## 객체 선언 (Object Declaration)

### 싱글톤

```kotlin
object DatabaseConfig {
    val url = "jdbc:postgresql://localhost:5432/db"
    val maxConnections = 10

    fun connect(): Connection {
        // ...
    }
}

// 사용
val url = DatabaseConfig.url
DatabaseConfig.connect()
```

### Companion Object

```kotlin
class MyClass {
    companion object Factory {
        fun create(): MyClass = MyClass()

        const val MAX_SIZE = 100
    }
}

// 사용
val instance = MyClass.create()
val size = MyClass.MAX_SIZE

// 이름 생략 가능
class MyClass {
    companion object {
        fun create(): MyClass = MyClass()
    }
}

val instance = MyClass.Companion.create()  // 또는
val instance = MyClass.create()
```

### 인터페이스 구현

```kotlin
interface Factory<T> {
    fun create(): T
}

class MyClass {
    companion object : Factory<MyClass> {
        override fun create(): MyClass = MyClass()
    }
}
```

### 객체 표현식 (익명 객체)

```kotlin
// 인터페이스 구현
val listener = object : ClickListener {
    override fun onClick() {
        println("Clicked!")
    }
}

// 여러 인터페이스
val combined = object : Clickable, Focusable {
    override fun click() { }
    override fun showOff() { }
}

// 슈퍼타입 없는 익명 객체
val adHoc = object {
    var x: Int = 0
    var y: Int = 0
}
println(adHoc.x + adHoc.y)
```

---

## Enum 클래스

```kotlin
enum class Direction {
    NORTH, SOUTH, EAST, WEST
}

// 프로퍼티와 메서드
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF);

    fun containsRed() = (rgb and 0xFF0000) != 0
}

// 추상 멤버
enum class Operation {
    PLUS {
        override fun apply(x: Int, y: Int) = x + y
    },
    MINUS {
        override fun apply(x: Int, y: Int) = x - y
    };

    abstract fun apply(x: Int, y: Int): Int
}

// 인터페이스 구현
enum class PaymentStatus : Describable {
    PENDING {
        override fun describe() = "Payment is pending"
    },
    COMPLETED {
        override fun describe() = "Payment is completed"
    }
}

// 사용
val direction = Direction.NORTH
println(direction.name)     // "NORTH"
println(direction.ordinal)  // 0

val allDirections = Direction.values()
val east = Direction.valueOf("EAST")

// entries 프로퍼티 (Kotlin 1.9+)
for (color in Color.entries) {
    println("${color.name}: ${color.rgb}")
}
```

---

## Value 클래스 (Inline Classes)

래퍼 클래스의 오버헤드 없이 타입 안전성을 제공합니다.

```kotlin
@JvmInline
value class Password(private val s: String)

@JvmInline
value class UserId(val id: Long)

fun login(userId: UserId, password: Password) {
    // userId.id로 접근
}

// 런타임에 박싱 없이 원시 타입으로 표현
val userId = UserId(123L)  // 런타임에는 Long 123
```

### 제약사항

- 정확히 하나의 프로퍼티만 가능
- `===` 비교 금지
- var 프로퍼티 불가
- 다른 클래스 상속 불가

---

## 위임 (Delegation)

### 클래스 위임

```kotlin
interface Printer {
    fun print(message: String)
}

class ConsolePrinter : Printer {
    override fun print(message: String) = println(message)
}

// by 키워드로 위임
class PrefixPrinter(printer: Printer) : Printer by printer {
    override fun print(message: String) {
        println("[PREFIX] $message")
    }
}

// 또는 완전 위임
class LoggingPrinter(private val printer: Printer) : Printer by printer
```

### 프로퍼티 위임

```kotlin
import kotlin.properties.Delegates

class User {
    // lazy
    val lazyValue: String by lazy {
        println("Computed!")
        "Hello"
    }

    // observable
    var name: String by Delegates.observable("초기값") { prop, old, new ->
        println("$old -> $new")
    }

    // vetoable
    var age: Int by Delegates.vetoable(0) { _, _, new ->
        new >= 0  // false면 변경 거부
    }

    // notNull (lateinit 대안)
    var email: String by Delegates.notNull()
}

// 커스텀 위임
class LoggingDelegate<T>(private var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        println("Getting ${property.name}")
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        println("Setting ${property.name} to $value")
        this.value = value
    }
}

class Example {
    var logged: String by LoggingDelegate("initial")
}
```

### Map 위임

```kotlin
class User(map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
}

val user = User(mapOf(
    "name" to "John",
    "age" to 25
))
println(user.name)  // "John"
println(user.age)   // 25

// MutableMap으로 수정 가능
class MutableUser(map: MutableMap<String, Any?>) {
    var name: String by map
    var age: Int by map
}
```

---

## 확장 (Extensions)

### 확장 함수

```kotlin
fun String.addExclamation(): String = "$this!"

fun MutableList<Int>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

// 사용
println("Hello".addExclamation())  // "Hello!"

val list = mutableListOf(1, 2, 3)
list.swap(0, 2)  // [3, 2, 1]
```

### 확장 프로퍼티

```kotlin
val String.lastChar: Char
    get() = this[length - 1]

var StringBuilder.lastChar: Char
    get() = this[length - 1]
    set(value) {
        this.setCharAt(length - 1, value)
    }

// 사용
println("Hello".lastChar)  // 'o'
```

### Nullable 수신자

```kotlin
fun Any?.toString(): String {
    if (this == null) return "null"
    return toString()
}

// 사용
val nullableString: String? = null
println(nullableString.toString())  // "null"
```

---

## 참고 문서

- [Classes](https://kotlinlang.org/docs/classes.html)
- [Inheritance](https://kotlinlang.org/docs/inheritance.html)
- [Interfaces](https://kotlinlang.org/docs/interfaces.html)
- [Data classes](https://kotlinlang.org/docs/data-classes.html)
- [Sealed classes](https://kotlinlang.org/docs/sealed-classes.html)
- [Object declarations](https://kotlinlang.org/docs/object-declarations.html)
- [Delegation](https://kotlinlang.org/docs/delegation.html)
- [Extensions](https://kotlinlang.org/docs/extensions.html)
