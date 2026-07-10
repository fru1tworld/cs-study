# 클래스와 객체

# 클래스

> **원문:** https://kotlinlang.org/docs/classes.html

Kotlin에서 클래스는 데이터(프로퍼티)와 동작(함수)을 캡슐화하는 객체의 청사진 또는 템플릿입니다. 클래스는 재사용 가능하고 구조화된 코드를 선언하기 위한 간결한 문법을 제공합니다.

## 클래스 선언

클래스는 `class` 키워드를 사용하여 선언합니다:

```kotlin
class Person { /*...*/ }
```

클래스 선언은 다음으로 구성됩니다:

### 클래스 헤더
- `class` 키워드
- 클래스 이름
- 타입 매개변수 (선택사항)
- 주 생성자 (선택사항)

### 클래스 본문 (선택사항)
- 부 생성자
- 초기화 블록
- 함수
- 프로퍼티
- 중첩 및 내부 클래스
- 객체 선언

**최소 문법:**
```kotlin
// 본문이 없는 클래스
class Person(val name: String, var age: Int)
```

## 인스턴스 생성

인스턴스는 클래스 이름 뒤에 괄호 `()`를 사용하여 함수 호출과 유사하게 생성됩니다:

```kotlin
val anonymousUser = Person()
val namedUser = Person("Joe")
```

**핵심 포인트:**
- `new` 키워드가 필요 없음
- 기본값을 사용하거나 특정 인수를 전달할 수 있음
- 가변(`var`) 또는 읽기 전용(`val`) 변수에 할당

**예제:**
```kotlin
class Person(val name: String = "Sebastian")

fun main() {
    val anonymousUser = Person()           // 기본값 사용
    val namedUser = Person("Joe")          // 특정 값 사용
    println(anonymousUser.name)            // Sebastian
    println(namedUser.name)                // Joe
}
```

## 생성자와 초기화 블록

### 주 생성자

주 생성자는 클래스 헤더에서 선언되며 초기 상태를 설정합니다:

```kotlin
class Person(name: String) { /*...*/ }

// 어노테이션이나 수정자가 없으면 'constructor' 키워드는 선택사항:
class Person(name: String) { /*...*/ }
```

**생성자 매개변수 프로퍼티:**

읽기 전용에는 `val`을, 가변에는 `var`를 사용합니다:

```kotlin
class Person(val name: String, var age: Int) { /*...*/ }
```

**프로퍼티 선언 없는 매개변수:**

```kotlin
class PersonWithAssignment(name: String) {
    val displayName: String = name
}
```

**기본값:**

```kotlin
class Person(val name: String = "John", var age: Int = 30) { /*...*/ }

fun main() {
    val person = Person()
    println("Name: ${person.name}, Age: ${person.age}")  // Name: John, Age: 30
}
```

**클래스 본문에서 생성자 매개변수 사용:**

```kotlin
class Person(
    val name: String = "John",
    var age: Int = 30
) {
    val description: String = "Name: $name, Age: $age"
}

fun main() {
    val person = Person()
    println(person.description)  // Name: John, Age: 30
}
```

### 초기화 블록

초기화 블록(`init {}`)은 인스턴스가 생성될 때 실행되며 복잡한 초기화 로직을 포함할 수 있습니다:

```kotlin
class Person(val name: String, var age: Int) {
    init {
        println("Person created: $name, age $age.")
    }
}

fun main() {
    Person("John", 30)  // Person created: John, age 30.
}
```

**다중 초기화 블록:**

```kotlin
class Person(val name: String, var age: Int) {
    init {
        println("Person created: $name, age $age.")
    }

    init {
        if (age < 18) {
            println("$name is a minor.")
        } else {
            println("$name is an adult.")
        }
    }
}

fun main() {
    Person("John", 30)
    // Person created: John, age 30.
    // John is an adult.
}
```

**`init`을 사용한 데이터 유효성 검사:**

```kotlin
class Person(val age: Int) {
    init {
        require(age > 0) { "age must be positive" }
    }
}
```

### 부 생성자

부 생성자는 클래스를 초기화하는 추가적인 방법을 제공합니다. 반드시 주 생성자에게 위임해야 합니다:

```kotlin
class Person(val name: String, var age: Int) {
    constructor(name: String, age: String) : this(name, age.toIntOrNull() ?: 0) {
        println("$name created with converted age: ${this.age}")
    }
}

fun main() {
    Person("Bob", "8")  // Bob created with converted age: 8
}
```

**직접 및 간접 위임:**

```kotlin
class Person(val name: String, var age: Int) {
    // 주 생성자에 직접 위임
    constructor(name: String) : this(name, 0) {
        println("Person created with default age: $age and name: $name.")
    }

    // 간접 위임: this("Bob") -> constructor(name: String) -> 주 생성자
    constructor() : this("Bob") {
        println("New person created with default age: $age and name: $name.")
    }
}

fun main() {
    Person("Alice")  // Person created with default age: 0 and name: Alice.
    Person()         // Person created with default age: 0 and name: Bob.
                     // New person created with default age: 0 and name: Bob.
}
```

**초기화 블록 실행 순서:**

```kotlin
class Person {
    init {
        println("1. First initializer block runs")
    }

    constructor(i: Int) {
        println("2. Person $i is created")
    }
}

fun main() {
    Person(1)
    // 1. First initializer block runs
    // 2. Person 1 is created
}
```

### 생성자가 없는 클래스

명시적 생성자가 없는 클래스는 암시적인 public 매개변수 없는 주 생성자를 가집니다:

```kotlin
class Person { }

fun main() {
    val person = Person()  // 암시적 생성자 사용
}
```

**생성자 가시성 제한:**

```kotlin
class Person private constructor() { /*...*/ }
```

**기본값이 있는 암시적 매개변수 없는 생성자:**

```kotlin
class Person(val personName: String = "")
// Kotlin이 암시적으로 제공: Person()
```

## 상속

클래스 상속을 통해 기존 기본 클래스에서 파생 클래스를 만들고, 프로퍼티와 함수를 상속받으면서 동작을 추가하거나 수정할 수 있습니다.

*자세한 내용은 상속 섹션을 참조하세요.*

## 추상 클래스

추상 클래스는 직접 인스턴스화할 수 없습니다. 상속되도록 설계되었으며 서브클래스가 구현해야 하는 추상 멤버를 제공합니다:

```kotlin
abstract class Person(val name: String, val age: Int) {
    // 추상 멤버 (구현 없음)
    abstract fun introduce()

    // 비추상 멤버 (구현 있음)
    fun greet() {
        println("Hello, my name is $name.")
    }
}

class Student(
    name: String,
    age: Int,
    val school: String
) : Person(name, age) {
    override fun introduce() {
        println("I am $name, $age years old, and I study at $school.")
    }
}

fun main() {
    val student = Student("Alice", 20, "Engineering University")
    student.greet()      // Hello, my name is Alice.
    student.introduce()  // I am Alice, 20 years old, and I study at Engineering University.
}
```

**핵심 포인트:**
- `abstract` 키워드를 사용하여 추상 클래스 선언
- 추상 멤버는 `open` 키워드가 필요 없음 (암시적으로 상속 가능)
- 서브클래스는 `override`를 사용하여 추상 멤버 구현

## 동반 객체

동반 객체를 사용하면 인스턴스를 만들지 않고도 클래스 이름을 사용하여 멤버에 접근할 수 있습니다:

```kotlin
class Person(val name: String) {
    companion object {
        fun createAnonymous() = Person("Anonymous")
    }
}

fun main() {
    val anonymous = Person.createAnonymous()
    println(anonymous.name)  // Anonymous
}
```

이것은 팩토리 함수와 논리적으로 관련된 정적과 같은 기능에 유용합니다.

---

# 상속

> **원문:** https://kotlinlang.org/docs/inheritance.html

이 페이지는 Kotlin에서의 상속을 다루며, 클래스가 다른 클래스로부터 어떻게 상속받을 수 있는지와 이 메커니즘을 지배하는 규칙을 설명합니다.

## 개요

모든 Kotlin 클래스는 기본적으로 `Any`를 상속받으며, 이는 세 가지 메서드를 제공합니다:
- `equals()`
- `hashCode()`
- `toString()`

```kotlin
class Example // 암시적으로 Any를 상속
```

## open 키워드

기본적으로 Kotlin 클래스는 **final**이며 상속될 수 없습니다. 클래스를 상속 가능하게 만들려면 `open` 키워드를 사용합니다:

```kotlin
open class Base // 클래스가 상속 가능하도록 열림
```

## 기본 상속

클래스를 상속받으려면 콜론 뒤에 상위 타입을 배치합니다:

```kotlin
open class Base(p: Int)
class Derived(p: Int) : Base(p)
```

## 메서드 오버라이딩

메서드는 오버라이드 가능하도록 명시적으로 `open`으로 표시해야 합니다:

```kotlin
open class Shape {
    open fun draw() { /*...*/ }
    fun fill() { /*...*/ }
}

class Circle() : Shape() {
    override fun draw() { /*...*/ }
}
```

`override`로 표시된 멤버는 기본적으로 자체적으로 open입니다. 추가 오버라이딩을 방지하려면 `final`을 사용합니다:

```kotlin
open class Rectangle() : Shape() {
    final override fun draw() { /*...*/ }
}
```

## 프로퍼티 오버라이딩

프로퍼티도 메서드와 유사하게 작동합니다:

```kotlin
open class Shape {
    open val vertexCount: Int = 0
}

class Rectangle : Shape() {
    override val vertexCount = 4
}
```

`val` 프로퍼티를 `var` 프로퍼티로 오버라이드할 수 있습니다(그 반대는 불가):

```kotlin
interface Shape {
    val vertexCount: Int
}

class Rectangle(override val vertexCount: Int = 4) : Shape
```

## 파생 클래스 초기화 순서

기본 클래스 초기화가 **먼저** 발생하고, 그 다음 파생 클래스 초기화가 이루어집니다. 파생 클래스에서 선언된 프로퍼티는 기본 클래스 생성자가 실행될 때 아직 초기화되지 않았습니다.

```kotlin
open class Base(val name: String) {
    init { println("Initializing base class") }
    open val size: Int = name.length.also {
        println("Initializing size in base class: $it")
    }
}

class Derived(name: String, val lastName: String) : Base(name) {
    init { println("Initializing derived class") }
    override val size: Int = (super.size + lastName.length).also {
        println("Initializing size in derived class: $it")
    }
}
```

## 상위 클래스 구현 호출

상위 클래스 함수와 프로퍼티를 호출하려면 `super` 키워드를 사용합니다:

```kotlin
open class Rectangle {
    open fun draw() { println("Drawing a rectangle") }
    val borderColor: String get() = "black"
}

class FilledRectangle : Rectangle() {
    override fun draw() {
        super.draw()
        println("Filling the rectangle")
    }
}
```

외부 클래스의 상위 클래스에 접근하는 내부 클래스의 경우:

```kotlin
inner class Filler {
    fun drawAndFill() {
        super@FilledRectangle.draw()
    }
}
```

## 오버라이딩 규칙 (다중 상속)

동일한 멤버의 여러 구현을 상속받을 때는 반드시 오버라이드해야 합니다:

```kotlin
open class Rectangle {
    open fun draw() { /*...*/ }
}

interface Polygon {
    fun draw() { /*...*/ }
}

class Square() : Rectangle(), Polygon {
    override fun draw() {
        super<Rectangle>.draw()  // Rectangle의 draw() 호출
        super<Polygon>.draw()    // Polygon의 draw() 호출
    }
}
```

## 부 생성자

주 생성자가 없는 파생 클래스는 부 생성자에서 기본 타입을 초기화해야 합니다:

```kotlin
class MyView : View {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
}
```

## 중요 참고사항

- Kotlin 클래스는 **기본적으로 final**입니다 (안전을 위한 설계 철학)
- 상속/오버라이딩을 허용하려면 명시적으로 `open`을 사용
- `override` 수정자는 오버라이딩 시 **필수**
- 기본 클래스 생성자나 초기화자에서 `open` 멤버 사용을 피해 런타임 오류 방지

---

# Kotlin 프로퍼티

> **원문:** https://kotlinlang.org/docs/properties.html

## 개요

Kotlin의 프로퍼티를 사용하면 데이터에 접근하거나 변경하기 위한 함수를 작성하지 않고도 데이터를 저장하고 관리할 수 있습니다. 모든 프로퍼티는 이름, 타입, 그리고 자동 생성된 `get()` 함수 (getter)를 가집니다. 가변 프로퍼티는 `set()` 함수 (setter)도 가집니다. getter와 setter를 접근자라고 합니다.

---

## 프로퍼티 선언

프로퍼티는 **가변** (`var`) 또는 **읽기 전용** (`val`)일 수 있습니다.

### 최상위 프로퍼티
```kotlin
// File: Constants.kt
package my.app
val pi = 3.14159
var counter = 0
```

### 클래스, 인터페이스, 객체의 프로퍼티
```kotlin
class Address {
    var name: String = "Holmes, Sherlock"
    var street: String = "Baker"
    var city: String = "London"
}

interface ContactInfo {
    val email: String
}

object Company {
    var name: String = "Detective Inc."
    val country: String = "UK"
}

class PersonContact : ContactInfo {
    override val email: String = "sherlock@example.com"
}
```

### 프로퍼티 접근
```kotlin
fun copyAddress(address: Address): Address {
    val result = Address()
    result.name = address.name
    result.street = address.street
    result.city = address.city
    return result
}
```

---

## 커스텀 Getter와 Setter

### 커스텀 Getter
프로퍼티에 접근할 때마다 실행됩니다:
```kotlin
class Rectangle(val width: Int, val height: Int) {
    val area: Int get() = this.width * this.height
}
```

### 커스텀 Setter
값이 할당될 때마다 실행됩니다 (초기화 중 제외):
```kotlin
class Point(var x: Int, var y: Int) {
    var coordinates: String
        get() = "$x,$y"
        set(value) {
            val parts = value.split(",")
            x = parts[0].toInt()
            y = parts[1].toInt()
        }
}
```

### 가시성 변경 또는 어노테이션 추가

**접근자 가시성 변경:**
```kotlin
class BankAccount(initialBalance: Int) {
    var balance: Int = initialBalance
        private set

    fun deposit(amount: Int) {
        if (amount > 0) balance += amount
    }
}
```

**접근자에 어노테이션 추가:**
```kotlin
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Inject

class Service {
    var dependency: String = "Default Service"
        @Inject get
}
```

---

## 백킹 필드

접근자는 메모리에 프로퍼티 값을 저장하기 위해 백킹 필드를 사용합니다. Kotlin은 필요할 때 자동으로 생성합니다. `field` 키워드를 사용하여 백킹 필드를 참조합니다.

```kotlin
class Scoreboard {
    var score: Int = 0
        set(value) {
            field = value
            println("Score updated to $field")
        }
}
```

**참고:** Kotlin은 기본 getter/setter를 사용하거나 커스텀 접근자에서 `field`를 사용할 때만 백킹 필드를 생성합니다.

---

## 백킹 프로퍼티

외부에서 읽기 전용으로 내부에서 수정하는 패턴:

```kotlin
class ShoppingCart {
    private val _items = mutableListOf<String>()
    val items: List<String> get() = _items

    fun addItem(item: String) {
        _items.add(item)
    }

    fun removeItem(item: String) {
        _items.remove(item)
    }
}
```

**상태를 공유하는 여러 프로퍼티:**
```kotlin
class Temperature {
    private var _celsius: Double = 0.0

    var celsius: Double
        get() = _celsius
        set(value) { _celsius = value }

    var fahrenheit: Double
        get() = _celsius * 9 / 5 + 32
        set(value) { _celsius = (value - 32) * 5 / 9 }
}
```

---

## 컴파일 타임 상수

`const`를 사용하여 읽기 전용 프로퍼티를 컴파일 타임 상수로 표시합니다:

```kotlin
const val MAX_LOGIN_ATTEMPTS = 3
```

**요구 사항:**
- 최상위 프로퍼티, 또는 `object`나 companion object의 멤버
- `String` 또는 기본 타입으로 초기화해야 함
- 커스텀 getter를 가질 수 없음

**어노테이션에서 사용:**
```kotlin
const val SUBSYSTEM_DEPRECATED: String = "This subsystem is deprecated"

@Deprecated(SUBSYSTEM_DEPRECATED)
fun processLegacyOrders() { ... }
```

---

## 늦은 초기화 프로퍼티

생성 후에 초기화되는 프로퍼티에 `lateinit` 수정자를 사용합니다:

```kotlin
public class OrderServiceTest {
    lateinit var orderService: OrderService

    @SetUp
    fun setup() {
        orderService = OrderService()
    }
}
```

**초기화 여부 확인:**
```kotlin
class WeatherStation {
    lateinit var latestReading: String

    fun printReading() {
        if (this::latestReading.isInitialized) {
            println("Latest reading: $latestReading")
        } else {
            println("No reading available")
        }
    }
}
```

**초기화 전 접근 시 예외:**
```
kotlin.UninitializedPropertyAccessException: lateinit property has not been initialized
```

---

## 위임된 프로퍼티

로직을 재사용하기 위해 프로퍼티 책임을 별도의 객체에 위임합니다:

**일반적인 사용 사례:**
- 값을 지연 계산
- 키로 맵에서 읽기
- 데이터베이스 접근
- 접근 시 리스너에 알림

구현 세부 사항은 [위임된 프로퍼티 문서](delegated-properties.md)를 참조하세요.

---

# 인터페이스

> **원문:** https://kotlinlang.org/docs/interfaces.html

Kotlin의 인터페이스는 추상 메서드의 선언과 메서드 구현을 모두 포함할 수 있습니다. 추상 클래스와 달리 인터페이스는 상태를 저장할 수 없지만, 추상이거나 접근자 구현을 제공하는 프로퍼티를 가질 수 있습니다.

## 기본 문법

```kotlin
interface MyInterface {
    fun bar()
    fun foo() {
        // 선택적 본문
    }
}
```

## 인터페이스 구현

클래스나 객체는 하나 이상의 인터페이스를 구현할 수 있습니다:

```kotlin
class Child : MyInterface {
    override fun bar() {
        // 본문
    }
}
```

## 인터페이스의 프로퍼티

인터페이스의 프로퍼티는 추상이거나 접근자 구현을 제공할 수 있습니다. **중요**: 프로퍼티는 지원 필드를 가질 수 없으므로 접근자에서 이를 참조할 수 없습니다:

```kotlin
interface MyInterface {
    val prop: Int // 추상
    val propertyWithImplementation: String
        get() = "foo"

    fun foo() {
        print(prop)
    }
}

class Child : MyInterface {
    override val prop: Int = 29
}
```

## 인터페이스 상속

인터페이스는 다른 인터페이스로부터 파생될 수 있으며, 멤버에 대한 구현을 제공하고 새로운 함수와 프로퍼티를 선언할 수 있습니다. 이러한 인터페이스를 구현하는 클래스는 누락된 구현만 정의하면 됩니다:

```kotlin
interface Named {
    val name: String
}

interface Person : Named {
    val firstName: String
    val lastName: String
    override val name: String
        get() = "$firstName $lastName"
}

data class Employee(
    override val firstName: String,
    override val lastName: String,
    val position: Position
) : Person
```

## 오버라이딩 충돌 해결

동일한 메서드를 가진 여러 인터페이스로부터 상속받을 때는 구현을 명시적으로 지정해야 합니다:

```kotlin
interface A {
    fun foo() { print("A") }
    fun bar()
}

interface B {
    fun foo() { print("B") }
    fun bar() { print("bar") }
}

class D : A, B {
    override fun foo() {
        super<A>.foo()
        super<B>.foo()
    }
    override fun bar() {
        super<B>.bar()
    }
}
```

## JVM 기본 메서드 생성

`-jvm-default` 컴파일러 옵션으로 인터페이스 함수 컴파일 동작을 제어합니다:

| 옵션 | 동작 |
|------|------|
| `enable` (기본값) | 기본 구현 생성; 바이너리 호환성을 위한 브릿지 함수 포함 |
| `no-compatibility` | 기본 구현만; 브릿지 생략 (새 코드용) |
| `disable` | 호환성 브릿지와 `DefaultImpls` 클래스만 |

**설정 (Gradle Kotlin DSL):**
```kotlin
kotlin {
    compilerOptions {
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }
}
```

---

# 가시성 수정자

> **원문:** https://kotlinlang.org/docs/visibility-modifiers.html

클래스, 객체, 인터페이스, 생성자, 함수, 프로퍼티 및 그 세터는 가시성 수정자를 가질 수 있습니다. 게터는 항상 해당 프로퍼티와 동일한 가시성을 갖습니다.

## Kotlin의 네 가지 가시성 수정자

- `private`
- `protected`
- `internal`
- `public` (기본값)

## 패키지

함수, 프로퍼티, 클래스, 객체, 인터페이스는 패키지 내부에 최상위 수준에서 직접 선언될 수 있습니다.

```kotlin
// 파일명: example.kt
package foo

fun baz() { ... }
class Bar { ... }
```

### 패키지 수준 선언의 가시성 규칙

| 수정자 | 가시성 |
|--------|--------|
| **public** (기본값) | 어디서나 볼 수 있음 |
| **private** | 선언을 포함하는 파일 내부에서만 볼 수 있음 |
| **internal** | 같은 모듈 내 어디서나 볼 수 있음 |
| **protected** | 최상위 선언에는 사용 불가 |

### 예제

```kotlin
// 파일명: example.kt
package foo

private fun foo() { ... }          // example.kt 내부에서만 볼 수 있음
public var bar: Int = 5            // 어디서나 볼 수 있음
    private set                    // 세터는 example.kt에서만 볼 수 있음
internal val baz = 6               // 같은 모듈 내에서 볼 수 있음
```

## 클래스 멤버

클래스 내부에 선언된 멤버의 경우:

| 수정자 | 가시성 |
|--------|--------|
| **private** | 이 클래스 내부에서만 볼 수 있음 (모든 멤버 포함) |
| **protected** | private과 동일, 추가로 서브클래스에서 볼 수 있음 |
| **internal** | 선언하는 클래스를 볼 수 있는 같은 모듈 내 모든 클라이언트에서 볼 수 있음 |
| **public** | 선언하는 클래스를 볼 수 있는 모든 클라이언트에서 볼 수 있음 |

**중요:** 외부 클래스는 내부 클래스의 private 멤버를 볼 수 없습니다.

**오버라이드 동작:** 가시성을 명시적으로 지정하지 않고 `protected` 또는 `internal` 멤버를 오버라이드하면 원본과 동일한 가시성을 유지합니다.

### 예제

```kotlin
open class Outer {
    private val a = 1
    protected open val b = 2
    internal open val c = 3
    val d = 4  // 기본적으로 public

    protected class Nested {
        public val e: Int = 5
    }
}

class Subclass : Outer() {
    // a는 볼 수 없음
    // b, c, d는 볼 수 있음
    // Nested와 e는 볼 수 있음
    override val b = 5        // 'b'는 protected
    override val c = 7        // 'c'는 internal
}

class Unrelated(o: Outer) {
    // o.a, o.b는 볼 수 없음
    // o.c와 o.d는 볼 수 있음 (같은 모듈)
    // Outer.Nested는 볼 수 없고, Nested::e도 볼 수 없음
}
```

## 생성자

### 주 생성자 가시성 문법

```kotlin
class C private constructor(a: Int) { ... }
```

- **기본값:** 모든 생성자는 기본적으로 `public`
- **봉인 클래스:** 생성자는 기본적으로 `protected`
- **명시적 키워드:** 가시성을 지정하려면 `constructor` 키워드 사용

## 지역 선언

지역 변수, 함수, 클래스는 **가시성 수정자를 가질 수 없습니다**.

## 모듈

`internal` 가시성 수정자는 멤버가 같은 모듈 내에서 볼 수 있음을 의미합니다.

### 모듈을 구성하는 것

- IntelliJ IDEA 모듈
- Maven 프로젝트
- Gradle 소스 세트 (참고: `test` 소스 세트는 `main`의 internal 선언에 접근 가능)

---

# 확장

> **원문:** https://kotlinlang.org/docs/extensions.html

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

---

# 데이터 클래스

> **원문:** https://kotlinlang.org/docs/data-classes.html

Kotlin의 데이터 클래스는 주로 데이터를 보관하는 데 사용됩니다. 컴파일러는 인스턴스를 출력, 비교, 복사하는 등의 추가 멤버 함수를 자동으로 생성합니다. `data` 키워드로 표시됩니다.

```kotlin
data class User(val name: String, val age: Int)
```

## 자동 생성 멤버

컴파일러는 주 생성자에 선언된 모든 프로퍼티로부터 다음 멤버들을 파생합니다:

- **`equals()`/`hashCode()` 쌍** - 인스턴스 비교용
- **`toString()`** - `"User(name=John, age=42)"` 형식의 출력 반환
- **`componentN()` 함수** - 선언 순서대로 프로퍼티에 대응 (구조 분해용)
- **`copy()` 함수** - 수정된 프로퍼티로 복사본 생성

## 데이터 클래스 요구사항

- 주 생성자는 **최소 하나의 매개변수**를 가져야 함
- 모든 주 생성자 매개변수는 `val` 또는 `var`로 표시해야 함
- 데이터 클래스는 abstract, open, sealed, inner가 **될 수 없음**

## 클래스 본문에 선언된 프로퍼티

클래스 본문 내부에 선언된 프로퍼티(주 생성자가 아닌)는 생성된 함수에서 **제외**됩니다:

```kotlin
data class Person(val name: String) {
    var age: Int = 0
}

fun main() {
    val person1 = Person("John")
    val person2 = Person("John")
    person1.age = 10
    person2.age = 20
    println("person1 == person2: ${person1 == person2}") // true
    // age는 equals()에서 사용되지 않음, name만 사용
}
```

## 복사

`copy()` 함수는 얕은 복사본을 생성하며 프로퍼티 수정이 가능합니다:

```kotlin
val jack = User(name = "Jack", age = 1)
val olderJack = jack.copy(age = 2)
```

**중요:** `copy()`는 얕은 복사를 수행합니다 - 가변 참조는 공유됩니다:

```kotlin
data class Employee(val name: String, val roles: MutableList<String>)

val original = Employee("Jamie", mutableListOf("developer"))
val duplicate = original.copy()
duplicate.roles.add("team lead")
println(original)  // Employee(name=Jamie, roles=[developer, team lead])
// 둘 다 같은 리스트를 참조!
```

## 구조 분해 선언

데이터 클래스 컴포넌트 함수는 구조 분해를 가능하게 합니다:

```kotlin
val jane = User("Jane", 35)
val (name, age) = jane
println("$name, $age years of age") // Jane, 35 years of age
```

## JVM 매개변수 없는 생성자

JVM에서 매개변수 없는 생성자를 생성하려면 기본값을 제공합니다:

```kotlin
data class User(val name: String = "", val age: Int = 0)
```

## 표준 데이터 클래스

표준 라이브러리는 `Pair`와 `Triple` 클래스를 제공하지만, 코드 가독성을 위해 일반적으로 명명된 데이터 클래스가 선호됩니다.

---

# 봉인 클래스와 인터페이스

> **원문:** https://kotlinlang.org/docs/sealed-classes.html

## 개요

봉인 클래스와 인터페이스는 클래스 계층의 제어된 상속을 제공합니다. 봉인 클래스의 모든 직접 서브클래스는 컴파일 시점에 알려져 있습니다. 봉인 클래스가 정의된 모듈과 패키지 외부에서는 다른 서브클래스가 나타날 수 없습니다.

**주요 이점:**
- 미리 정의된 유한한 서브클래스로 클래스 상속 제한
- 패턴 매칭과 상태 관리를 위한 타입 안전 설계
- 라이브러리를 위한 견고한 공개 API

## 봉인 클래스 또는 인터페이스 선언

`sealed` 수정자를 사용합니다:

```kotlin
// 봉인 인터페이스 생성
sealed interface Error

// 봉인 인터페이스 Error를 구현하는 봉인 클래스 생성
sealed class IOError(): Error

// 봉인 클래스 'IOError'를 확장하는 서브클래스 정의
class FileReadError(val file: File): IOError()
class DatabaseError(val source: DataSource): IOError()

// 'Error' 봉인 인터페이스를 구현하는 싱글톤 객체 생성
object RuntimeError : Error
```

### 생성자

봉인 클래스는 항상 추상이며 직접 인스턴스화할 수 없습니다. 서브클래스를 위한 생성자를 포함할 수 있습니다:

```kotlin
sealed class Error(val message: String) {
    class NetworkError : Error("Network failure")
    class DatabaseError : Error("Database cannot be reached")
    class UnknownError : Error("An unknown error has occurred")
}

fun main() {
    val errors = listOf(Error.NetworkError(), Error.DatabaseError(), Error.UnknownError())
    errors.forEach { println(it.message) }
}
// 출력:
// Network failure
// Database cannot be reached
// An unknown error has occurred
```

생성자는 `protected` (기본값) 또는 `private` 가시성만 가질 수 있습니다:

```kotlin
sealed class IOError {
    constructor() { /*...*/ }  // 기본적으로 protected
    private constructor(description: String): this() { /*...*/ }
    // public/internal 생성자는 허용되지 않음
}
```

## 상속 규칙

**직접 서브클래스**는 반드시:
- 같은 패키지에 선언
- 최상위 또는 명명된 클래스, 인터페이스, 객체 내부에 중첩
- 적절한 한정 이름 (지역이나 익명 아님)

**제한사항:**
- `enum` 클래스는 봉인 클래스를 확장할 수 없지만 봉인 인터페이스는 구현 가능
- 간접 서브클래스에는 제한 없음

```kotlin
sealed interface Error

// 봉인 인터페이스를 구현하는 enum (허용)
enum class ErrorType : Error {
    FILE_ERROR, DATABASE_ERROR
}

// 봉인 인터페이스를 확장하는 봉인 클래스 (같은 패키지 내에서만 확장 가능)
sealed class IOError(): Error

// 봉인 인터페이스를 확장하는 open 클래스 (어디서든 확장 가능)
open class CustomError(): Error
```

### 멀티플랫폼 프로젝트

직접 서브클래스는 같은 소스 세트에 있어야 합니다. 봉인 클래스가 `expect`와 `actual` 수정자를 사용하면 두 버전 모두 각각의 소스 세트에서 서브클래스를 가질 수 있습니다.

## when 표현식과 봉인 클래스 사용

핵심 이점: **`else` 절 없이 완전한 타입 검사**

```kotlin
sealed class Error {
    class FileReadError(val file: String): Error()
    class DatabaseError(val source: String): Error()
    object RuntimeError : Error()
}

fun log(e: Error) = when(e) {
    is Error.FileReadError -> println("Error while reading file ${e.file}")
    is Error.DatabaseError -> println("Error while reading from database ${e.source}")
    Error.RuntimeError -> println("Runtime error")
    // else 절이 필요 없음 - 모든 케이스 커버!
}

fun main() {
    val errors = listOf(
        Error.FileReadError("example.txt"),
        Error.DatabaseError("usersDatabase"),
        Error.RuntimeError
    )
    errors.forEach { log(it) }
}
```

## 사용 사례 시나리오

### UI 애플리케이션의 상태 관리

```kotlin
sealed class UIState {
    data object Loading : UIState()
    data class Success(val data: String) : UIState()
    data class Error(val exception: Exception) : UIState()
}

fun updateUI(state: UIState) {
    when (state) {
        is UIState.Loading -> showLoadingIndicator()
        is UIState.Success -> showData(state.data)
        is UIState.Error -> showError(state.exception)
    }
}
```

### 결제 방법 처리

```kotlin
sealed class Payment {
    data class CreditCard(val number: String, val expiryDate: String) : Payment()
    data class PayPal(val email: String) : Payment()
    data object Cash : Payment()
}

fun processPayment(payment: Payment) {
    when (payment) {
        is Payment.CreditCard -> processCreditCardPayment(payment.number, payment.expiryDate)
        is Payment.PayPal -> processPayPalPayment(payment.email)
        is Payment.Cash -> processCashPayment()
    }
}
```

### API 요청-응답 처리

```kotlin
sealed interface ApiRequest

data class LoginRequest(val username: String, val password: String) : ApiRequest
object LogoutRequest : ApiRequest

sealed class ApiResponse {
    data class UserSuccess(val user: UserData) : ApiResponse()
    data object UserNotFound : ApiResponse()
    data class Error(val message: String) : ApiResponse()
}

data class UserData(val userId: String, val name: String, val email: String)

fun handleRequest(request: ApiRequest): ApiResponse {
    return when (request) {
        is LoginRequest -> {
            if (isValidUser(request.username, request.password)) {
                ApiResponse.UserSuccess(UserData("userId", "userName", "userEmail"))
            } else {
                ApiResponse.Error("Invalid username or password")
            }
        }
        is LogoutRequest -> ApiResponse.UserSuccess(UserData("userId", "userName", "userEmail"))
    }
}
```

## 요약

봉인 클래스가 제공하는 것:
- 제어된 타입 안전 상속
- 완전한 `when` 표현식 검사
- 명확한 API 계약
- 더 나은 코드 유지보수성

---

# 중첩 및 내부 클래스

> **원문:** https://kotlinlang.org/docs/nested-classes.html

## 개요

Kotlin에서 클래스는 다른 클래스 내부에 중첩될 수 있습니다. 클래스와 인터페이스의 모든 조합이 중첩에 가능합니다.

## 중첩 클래스

기본 중첩 클래스 예제:

```kotlin
class Outer {
    private val bar: Int = 1
    class Nested {
        fun foo() = 2
    }
}

val demo = Outer.Nested().foo() // == 2
```

### 중첩 조합

다음을 중첩할 수 있습니다:
- 클래스 내의 인터페이스
- 인터페이스 내의 클래스
- 인터페이스 내의 인터페이스

예제:
```kotlin
interface OuterInterface {
    class InnerClass
    interface InnerInterface
}

class OuterClass {
    class InnerClass
    interface InnerInterface
}
```

## 내부 클래스

`inner` 키워드로 표시된 중첩 클래스는 **외부 클래스의 멤버에 접근**할 수 있습니다. 내부 클래스는 외부 클래스 객체에 대한 참조가 있습니다:

```kotlin
class Outer {
    private val bar: Int = 1
    inner class Inner {
        fun foo() = bar
    }
}

val demo = Outer().Inner().foo() // == 1
```

**참고:** 내부 클래스에서 `this` 명확화에 대해서는 한정된 `this` 표현식을 참조하세요.

## 익명 내부 클래스

익명 내부 클래스 인스턴스는 객체 표현식을 사용하여 생성됩니다:

```kotlin
window.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) { ... }
    override fun mouseEntered(e: MouseEvent) { ... }
})
```

### 함수형 인터페이스를 위한 람다 표현식 (JVM)

Java 함수형 인터페이스(단일 추상 메서드)의 경우 람다 표현식을 사용합니다:

```kotlin
val listener = ActionListener { println("clicked") }
```

---

# 열거형 클래스

> **원문:** https://kotlinlang.org/docs/enum-classes.html

## 개요

Kotlin의 열거형 클래스는 상수 집합을 정의하는 타입 안전한 방법을 제공합니다. 각 열거형 상수는 열거형 클래스의 객체 인스턴스입니다.

## 기본 열거형 선언

```kotlin
enum class Direction { NORTH, SOUTH, WEST, EAST }
```

열거형 상수는 쉼표로 구분됩니다.

## 프로퍼티가 있는 열거형

열거형은 프로퍼티를 가지고 초기화될 수 있습니다:

```kotlin
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}
```

## 익명 클래스

열거형 상수는 해당 메서드를 가진 자체 익명 클래스를 선언하고 기본 메서드를 오버라이드할 수 있습니다:

```kotlin
enum class ProtocolState {
    WAITING { override fun signal() = TALKING },
    TALKING { override fun signal() = WAITING };
    abstract fun signal(): ProtocolState
}
```

**참고:** 상수 정의와 멤버 정의를 구분하려면 세미콜론을 사용합니다.

## 인터페이스 구현

열거형 클래스는 인터페이스를 구현할 수 있습니다(클래스에서 파생은 불가):

```kotlin
import java.util.function.BinaryOperator
import java.util.function.IntBinaryOperator

enum class IntArithmetics : BinaryOperator<Int>, IntBinaryOperator {
    PLUS {
        override fun apply(t: Int, u: Int): Int = t + u
    },
    TIMES {
        override fun apply(t: Int, u: Int): Int = t * u
    };

    override fun applyAsInt(t: Int, u: Int) = apply(t, u)
}
```

모든 열거형 클래스는 기본적으로 `Comparable`을 구현하며 상수들은 자연적인 순서로 정렬됩니다.

## 열거형 상수 작업

### 프로퍼티와 메서드

```kotlin
EnumClass.valueOf(value: String): EnumClass
EnumClass.entries: EnumEntries<EnumClass>  // 특수화된 List<EnumClass>
```

### 사용 예제

```kotlin
enum class RGB { RED, GREEN, BLUE }

fun main() {
    // entries를 통해 순회
    for (color in RGB.entries) {
        println(color.toString())  // RED, GREEN, BLUE
    }

    // 이름으로 가져오기
    println(RGB.valueOf("RED"))  // "The first color is: RED" 출력

    // name과 ordinal 접근
    println(RGB.RED.name)      // RED
    println(RGB.RED.ordinal)   // 0
}
```

### 제네릭 접근

제네릭 열거형 접근을 위해 인라인 구체화 타입 매개변수를 사용합니다:

```kotlin
enum class RGB { RED, GREEN, BLUE }

inline fun <reified T : Enum<T>> printAllValues() {
    println(enumEntries<T>().joinToString { it.name })
}

printAllValues<RGB>()  // RED, GREEN, BLUE
```

## 주요 함수

- **`valueOf(name: String)`** - 이름으로 열거형 상수 가져오기 (찾지 못하면 `IllegalArgumentException` 발생)
- **`entries`** - `EnumEntries<T>` 반환 (모든 상수의 특수화된 리스트) [Kotlin 1.9.0 이상 권장]
- **`enumEntries<T>()`** - 모든 열거형 항목을 반환하는 제네릭 함수 (deprecated된 `enumValues<T>()`보다 효율적)
- **`name`** 프로퍼티 - 상수의 이름 가져오기
- **`ordinal`** 프로퍼티 - 상수의 위치 가져오기 (0부터 시작)

## 성능 참고

deprecated된 `enumValues<T>()` 대신 `enumEntries<T>()`를 사용하세요 - 매번 동일한 리스트 인스턴스를 반환하여 불필요한 배열 생성을 피합니다.

---

# 객체 선언과 표현식

> **원문:** https://kotlinlang.org/docs/object-declarations.html

## 개요

Kotlin에서 객체는 클래스를 정의하고 인스턴스를 한 단계로 생성할 수 있게 합니다. 이는 싱글톤과 일회성 객체에 유용합니다. Kotlin은 두 가지 접근 방식을 제공합니다:
- **객체 선언** - 싱글톤 생성용
- **객체 표현식** - 익명 일회성 객체 생성용

## 사용 사례

- 공유 리소스(예: 데이터베이스 연결 풀)를 위한 싱글톤 사용
- 동반 객체를 통한 팩토리 메서드 생성
- 기존 클래스 동작을 일시적으로 수정
- 인터페이스나 추상 클래스의 타입 안전 구현

## 객체 선언

`object` 키워드를 사용하여 단일 인스턴스를 생성합니다:

```kotlin
object DataProviderManager {
    private val providers = mutableListOf<DataProvider>()

    fun registerDataProvider(provider: DataProvider) {
        providers.add(provider)
    }

    val allDataProviders: Collection<DataProvider>
        get() = providers
}

// 사용 - 객체 이름으로 직접 참조
DataProviderManager.registerDataProvider(exampleProvider)
```

**핵심 포인트:**
- 첫 접근 시 스레드 안전 초기화
- 상위 타입을 가질 수 있음 (인터페이스 구현 또는 클래스 상속)
- 지역적일 수 없음 (함수 내부에 중첩 불가)
- 할당의 오른쪽에 사용할 수 없음

### 데이터 객체

`data` 수정자로 표시하여 컴파일러 생성 함수를 얻습니다:

```kotlin
data object MyDataObject {
    val number: Int = 3
}

fun main() {
    println(MyDataObject) // MyDataObject (MyDataObject@hashcode가 아님)
}
```

**생성되는 함수:**
- `toString()` - 객체 이름 반환
- `equals()`/`hashCode()` - 동등성 검사 가능
- `copy()` 함수 없음 (싱글톤 개념 위반)
- `componentN()` 함수 없음 (데이터 프로퍼티 없음)

**중요:** 데이터 객체는 참조(`===`)가 아닌 구조적(`==`)으로 비교

### 봉인 계층의 데이터 객체

```kotlin
sealed interface ReadResult
data class Number(val number: Int) : ReadResult
data class Text(val text: String) : ReadResult
data object EndOfFile : ReadResult

fun main() {
    println(Number(7))  // Number(number=7)
    println(EndOfFile)  // EndOfFile
}
```

## 동반 객체

클래스 수준 함수와 프로퍼티를 정의합니다:

```kotlin
class User(val name: String) {
    companion object Factory {
        fun create(name: String): User = User(name)
    }
}

// 클래스 이름으로 호출
val user = User.create("John Doe")
```

**특징:**
- 이름은 선택사항 (기본값은 `Companion`)
- 클래스의 `private` 멤버에 접근 가능
- 인터페이스 구현 가능
- 클래스 이름이 동반 객체에 대한 참조 역할

```kotlin
// 인터페이스를 구현하는 동반 객체
interface Factory<T> {
    fun create(name: String): T
}

class User(val name: String) {
    companion object : Factory<User> {
        override fun create(name: String): User = User(name)
    }
}

val userFactory: Factory<User> = User
val newUser = userFactory.create("Example User")
```

## 객체 표현식

이름을 지정하지 않고 익명 클래스를 선언하고 인스턴스를 생성합니다:

### 처음부터 익명 객체 생성

```kotlin
val helloWorld = object {
    val hello = "Hello"
    val world = "World"
    override fun toString() = "$hello $world"
}
print(helloWorld) // Hello World
```

### 상위 타입으로부터 상속

```kotlin
window.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) { /*...*/ }
    override fun mouseEntered(e: MouseEvent) { /*...*/ }
})
```

다중 상위 타입:

```kotlin
open class BankAccount(initialBalance: Int) {
    open val balance: Int = initialBalance
}

interface Transaction {
    fun execute()
}

val temporaryAccount = object : BankAccount(1000), Transaction {
    override val balance = 1500
    override fun execute() {
        println("Executing special transaction. New balance is $balance.")
    }
}
```

### 익명 객체의 반환 타입

`public`/`protected`/`internal` 함수에서 반환될 때:
- **상위 타입 없음** -> 반환 타입은 `Any`
- **하나의 상위 타입** -> 반환 타입은 그 상위 타입
- **다중 상위 타입** -> 반환 타입을 명시적으로 선언해야 함

실제 타입에 선언된 멤버만 접근 가능합니다.

### 둘러싸는 스코프 변수 접근

```kotlin
fun countClicks(window: JComponent) {
    var clickCount = 0
    var enterCount = 0

    window.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            clickCount++
        }
        override fun mouseEntered(e: MouseEvent) {
            enterCount++
        }
    })
}
```

## 초기화 동작 차이

| 타입 | 초기화 |
|------|--------|
| **객체 표현식** | 사용되는 위치에서 즉시 실행 |
| **객체 선언** | 첫 접근 시 지연 초기화 |
| **동반 객체** | 클래스가 로드될 때 초기화 (Java 정적 초기화자와 유사) |

---

# 위임

> **원문:** https://kotlinlang.org/docs/delegation.html

## 개요

**위임 패턴**은 구현 상속의 좋은 대안이며, Kotlin은 보일러플레이트 코드 없이 기본적으로 이를 지원합니다.

## 기본 위임

클래스는 `by` 키워드를 사용하여 모든 public 멤버를 지정된 객체에 위임하여 인터페이스를 구현할 수 있습니다:

```kotlin
interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() { print(x) }
}

class Derived(b: Base) : Base by b

fun main() {
    val base = BaseImpl(10)
    Derived(base).print()
}
```

상위 타입 목록의 `by` 절은 `b`가 내부적으로 저장되고, 컴파일러가 `b`로 전달하는 `Base`의 모든 메서드를 생성함을 나타냅니다.

## 위임된 인터페이스의 멤버 오버라이딩

위임하는 클래스에서 인터페이스 멤버를 오버라이드할 수 있습니다. 컴파일러는 위임 객체의 구현 대신 여러분의 `override` 구현을 사용합니다:

```kotlin
interface Base {
    fun printMessage()
    fun printMessageLine()
}

class BaseImpl(val x: Int) : Base {
    override fun printMessage() { print(x) }
    override fun printMessageLine() { println(x) }
}

class Derived(b: Base) : Base by b {
    override fun printMessage() { print("abc") }
}

fun main() {
    val base = BaseImpl(10)
    Derived(base).printMessage()      // 출력: abc
    Derived(base).printMessageLine()  // 출력: 10
}
```

## 중요 참고: 위임 멤버 접근

오버라이드된 멤버는 위임 객체 자체의 구현에서 호출되지 **않습니다**. 위임은 자체 인터페이스 멤버 구현만 접근할 수 있습니다:

```kotlin
interface Base {
    val message: String
    fun print()
}

class BaseImpl(x: Int) : Base {
    override val message = "BaseImpl: x = $x"
    override fun print() { println(message) }
}

class Derived(b: Base) : Base by b {
    override val message = "Message of Derived"
}

fun main() {
    val b = BaseImpl(10)
    val derived = Derived(b)
    derived.print()        // 출력: BaseImpl: x = 10
    println(derived.message) // 출력: Message of Derived
}
```

## 관련 주제

- [인터페이스](interfaces.md)
- [상속](inheritance.md)
- [위임된 프로퍼티](delegated-properties.md)

---

# 위임된 프로퍼티

> **원문:** https://kotlinlang.org/docs/delegated-properties.html

## 개요

위임된 프로퍼티를 사용하면 일반적인 프로퍼티 패턴을 한 번 구현하고 재사용할 수 있습니다. 문법:
```kotlin
val/var <프로퍼티 이름>: <타입> by <표현식>
```

`by` 뒤의 표현식은 `getValue()`와 `setValue()` 연산을 처리하는 위임입니다.

## 기본 위임 구현

위임 클래스는 `getValue()`와 `setValue()` 메서드가 필요합니다:

```kotlin
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$thisRef, thank you for delegating '${property.name}' to me!"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in $thisRef.")
    }
}

class Example {
    var p: String by Delegate()
}

val e = Example()
println(e.p)  // Example@33a17727, thank you for delegating 'p' to me!
e.p = "NEW"   // NEW has been assigned to 'p' in Example@33a17727.
```

## 표준 위임

### 지연 프로퍼티

`lazy()`는 첫 접근 시에만 값을 계산합니다:

```kotlin
val lazyValue: String by lazy {
    println("computed!")
    "Hello"
}

fun main() {
    println(lazyValue)  // "computed!" 출력 후 "Hello" 출력
    println(lazyValue)  // "Hello"만 출력
}
```

**스레드 안전 옵션:**
- 기본값: synchronized (스레드 안전)
- `LazyThreadSafetyMode.PUBLICATION`: 다중 스레드 허용
- `LazyThreadSafetyMode.NONE`: 스레드 안전 없음

### 관찰 가능한 프로퍼티

`Delegates.observable()`은 변경 시 알립니다:

```kotlin
import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("<no name>") { prop, old, new ->
        println("$old -> $new")
    }
}

fun main() {
    val user = User()
    user.name = "first"   // <no name> -> first
    user.name = "second"  // first -> second
}
```

**Vetoable 변형:** 할당을 가로채서 거부하려면 `Delegates.vetoable()`을 사용합니다.

## 다른 프로퍼티에 위임

프로퍼티는 `::` 문법을 사용하여 다른 프로퍼티에 위임할 수 있습니다:

```kotlin
var topLevelInt: Int = 0

class ClassWithDelegate(val anotherClassInt: Int)

class MyClass(var memberInt: Int, val anotherClassInstance: ClassWithDelegate) {
    var delegatedToMember: Int by this::memberInt
    var delegatedToTopLevel: Int by ::topLevelInt
    val delegatedToAnotherClass: Int by anotherClassInstance::anotherClassInt
}

var MyClass.extDelegated: Int by ::topLevelInt
```

**사용 사례 - 하위 호환성:**
```kotlin
class MyClass {
    var newName: Int = 0

    @Deprecated("Use 'newName' instead", ReplaceWith("newName"))
    var oldName: Int by this::newName
}
```

## Map에 프로퍼티 저장

동적 프로퍼티를 위해 map을 위임으로 사용합니다:

```kotlin
class User(val map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
}

fun main() {
    val user = User(mapOf(
        "name" to "John Doe",
        "age" to 25
    ))
    println(user.name)  // John Doe
    println(user.age)   // 25
}
```

가변 프로퍼티의 경우 `MutableMap`을 사용합니다:
```kotlin
class MutableUser(val map: MutableMap<String, Any?>) {
    var name: String by map
    var age: Int by map
}
```

## 지역 위임된 프로퍼티

위임은 지역 스코프에서도 작동합니다:

```kotlin
fun example(computeFoo: () -> Foo) {
    val memoizedFoo by lazy(computeFoo)
    if (someCondition && memoizedFoo.isValid()) {
        memoizedFoo.doSomething()
    }
}
```

## 프로퍼티 위임 요구사항

### 읽기 전용 프로퍼티용 (`val`)
```kotlin
class ResourceDelegate {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Resource {
        return Resource()
    }
}

class Owner {
    val valResource: Resource by ResourceDelegate()
}
```

### 가변 프로퍼티용 (`var`)
```kotlin
class ResourceDelegate(private var resource: Resource = Resource()) {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Resource {
        return resource
    }

    operator fun setValue(thisRef: Owner, property: KProperty<*>, value: Any?) {
        if (value is Resource) {
            resource = value
        }
    }
}

class Owner {
    var varResource: Resource by ResourceDelegate()
}
```

### 표준 라이브러리 인터페이스 사용

```kotlin
fun resourceDelegate(resource: Resource = Resource()): ReadWriteProperty<Any?, Resource> =
    object : ReadWriteProperty<Any?, Resource> {
        var curValue = resource

        override fun getValue(thisRef: Any?, property: KProperty<*>): Resource = curValue
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Resource) {
            curValue = value
        }
    }

val readOnlyResource: Resource by resourceDelegate()
var readWriteResource: Resource by resourceDelegate()
```

## 변환 규칙

### 기본 변환

컴파일러가 보조 프로퍼티를 생성합니다:

```kotlin
class C {
    var prop: Type by MyDelegate()
}

// 컴파일러가 생성:
class C {
    private val prop$delegate = MyDelegate()
    var prop: Type
        get() = prop$delegate.getValue(this, this::prop)
        set(value: Type) = prop$delegate.setValue(this, this::prop, value)
}
```

### 최적화된 경우 (`$delegate` 필드 없음)

- 참조된 프로퍼티: `var prop: Type by ::impl`
- 명명된 객체: `val s: String by NamedObject`
- 지원 필드가 있는 final `val` 프로퍼티
- 상수 표현식, 열거형 항목, `this`, `null`

### 다른 프로퍼티에 위임

직접 접근 (`$delegate` 필드 없음):

```kotlin
class C<Type> {
    private var impl: Type = ...
    var prop: Type by ::impl
}

// 컴파일러가 생성:
class C<Type> {
    private var impl: Type = ...
    var prop: Type
        get() = impl
        set(value) { impl = value }
}
```

## 위임 제공

위임 생성을 커스터마이즈하려면 `provideDelegate`를 사용합니다:

```kotlin
class ResourceDelegate<T> : ReadOnlyProperty<MyUI, T> {
    override fun getValue(thisRef: MyUI, property: KProperty<*>): T { ... }
}

class ResourceLoader<T>(id: ResourceID<T>) {
    operator fun provideDelegate(
        thisRef: MyUI,
        prop: KProperty<*>
    ): ReadOnlyProperty<MyUI, T> {
        checkProperty(thisRef, prop.name)
        return ResourceDelegate()
    }

    private fun checkProperty(thisRef: MyUI, name: String) { ... }
}

class MyUI {
    fun <T> bindResource(id: ResourceID<T>): ResourceLoader<T> { ... }
    val image by bindResource(ResourceID.image_id)
    val text by bindResource(ResourceID.text_id)
}
```

### PropertyDelegateProvider 사용
```kotlin
val provider = PropertyDelegateProvider { thisRef: Any?, property ->
    ReadOnlyProperty<Any?, Int> { _, property -> 42 }
}
val delegate: Int by provider
```

## 요약

위임된 프로퍼티는 다음을 위한 강력한 메커니즘을 제공합니다:
- **지연 초기화** - 첫 접근 시 계산
- **관찰 가능한 프로퍼티** - 변경에 반응
- **Map 저장** - 동적 프로퍼티 바인딩
- **코드 재사용** - 패턴을 한 번 구현
- **하위 호환성** - 이전 이름을 우아하게 deprecated

---

# 인라인 값 클래스

> **원문:** https://kotlinlang.org/docs/inline-classes.html

## 개요

인라인 값 클래스는 런타임 오버헤드 없이 값을 래핑하도록 설계된 Kotlin의 특수한 종류의 클래스입니다. 런타임에 힙의 객체가 아닌 기본 타입으로 표현되어 기본 타입 래핑의 성능 문제를 해결합니다.

### 핵심 개념

인라인 클래스의 데이터는 인라인 함수가 호출 위치에 인라인되는 것과 유사하게 사용 위치에 **인라인**됩니다.

## 선언

**기본 문법:**
```kotlin
value class Password(private val s: String)
```

**JVM 백엔드용 (필수):**
```kotlin
@JvmInline value class Password(private val s: String)
```

### 요구사항

- 주 생성자에서 초기화된 **단일 프로퍼티**가 있어야 함
- 런타임에 인스턴스는 이 단일 프로퍼티만 사용하여 표현됨

**예제:**
```kotlin
val securePassword = Password("Don't try this in production")
// 실제 인스턴스화가 발생하지 않음; 'securePassword'는 단지 String만 포함
```

## 멤버

인라인 클래스가 지원하는 것:
- 프로퍼티와 함수
- `init` 블록
- 부 생성자

**완전한 예제:**
```kotlin
@JvmInline value class Person(private val fullName: String) {
    init {
        require(fullName.isNotEmpty()) { "Full name shouldn't be empty" }
    }

    constructor(firstName: String, lastName: String) :
        this("$firstName $lastName") {
        require(lastName.isNotBlank()) { "Last name shouldn't be empty" }
    }

    val length: Int get() = fullName.length

    fun greet() {
        println("Hello, $fullName")
    }
}

fun main() {
    val name1 = Person("Kotlin", "Mascot")
    val name2 = Person("Kodee")
    name1.greet() // 정적 메서드로 호출됨
    println(name2.length) // 게터가 정적 메서드로 호출됨
}
```

### 프로퍼티 제약

- 지원 필드를 가질 수 없음
- 단순 계산 가능한 프로퍼티만 허용
- `lateinit`이나 위임된 프로퍼티 없음

## 상속

### 인터페이스 (허용)

인라인 클래스는 인터페이스로부터 상속받을 수 **있습니다**:

```kotlin
interface Printable {
    fun prettyPrint(): String
}

@JvmInline value class Name(val s: String) : Printable {
    override fun prettyPrint(): String = "Let's $s!"
}

fun main() {
    val name = Name("Kotlin")
    println(name.prettyPrint()) // 여전히 정적 메서드로 호출됨
}
```

### 클래스 계층 (금지)

- 인라인 클래스는 다른 클래스를 확장할 수 **없음**
- 인라인 클래스는 항상 `final`

## 표현

### 런타임 동작

Kotlin 컴파일러는 각 인라인 클래스마다 래퍼를 유지하지만 성능을 위해 언박싱된(기본 타입) 표현을 선호합니다. 인라인 클래스는 다른 타입으로 사용될 때마다 **박싱**됩니다.

**예제:**
```kotlin
interface I

@JvmInline value class Foo(val i: Int) : I

fun asInline(f: Foo) {}           // 언박싱: Foo 자체로 사용
fun <T> asGeneric(x: T) {}        // 박싱: 제네릭 T로 사용
fun asInterface(i: I) {}          // 박싱: 타입 I로 사용
fun asNullable(i: Foo?) {}        // 박싱: Foo?로 사용
fun <T> id(x: T): T = x

fun main() {
    val f = Foo(42)
    asInline(f)      // 언박싱
    asGeneric(f)     // 박싱
    asInterface(f)   // 박싱
    asNullable(f)    // 박싱
    val c = id(f)    // 박싱 후 언박싱 -> 언박싱 결과
}
```

### 제네릭 타입 매개변수

인라인 클래스가 제네릭 타입 매개변수를 가지면 컴파일러는 이를 `Any?` 또는 상한으로 매핑합니다:

```kotlin
@JvmInline value class UserId<T>(val value: T)

fun compute(s: UserId<String>) {}
// 컴파일러가 생성: fun compute-<hashcode>(s: Any?)
```

### 참조 동등성

인라인 클래스는 값과 래퍼 모두로 표현될 수 있으므로 **허용되지 않습니다**.

## 맹글링

플랫폼 시그니처 충돌을 방지하기 위해 함수 이름이 맹글링됩니다:

**문제:**
```kotlin
@JvmInline value class UInt(val x: Int)

fun compute(x: Int) { }      // public final void compute(int x)
fun compute(x: UInt) { }     // 또한: public final void compute(int x)! 충돌
```

**해결책:**
두 번째 함수는 `public final void compute-<hashcode>(int x)`로 표현되어 충돌을 해결합니다.

### Java 코드에서 호출

맹글링을 수동으로 비활성화하려면 `@JvmName`을 사용합니다:

```kotlin
@JvmInline value class UInt(val x: Int)

fun compute(x: Int) { }

@JvmName("computeUInt")
fun compute(x: UInt) { }
```

**참고:** 기본적으로 인라인 클래스는 언박싱된 표현을 사용하여 Java에서 접근하기 어렵습니다. 박싱된 표현에 대해서는 상호운용성 가이드를 참조하세요.

## 인라인 클래스 vs 타입 별칭

| 측면 | 인라인 클래스 | 타입 별칭 |
|------|-------------|----------|
| **타입 안전성** | 새 타입 도입 | 기존 타입의 별칭일 뿐 |
| **할당 호환성** | 아니오 | 예 (기본 타입과) |

**비교:**
```kotlin
typealias NameTypeAlias = String
@JvmInline value class NameInlineClass(val s: String)

fun acceptString(s: String) {}
fun acceptNameTypeAlias(n: NameTypeAlias) {}
fun acceptNameInlineClass(p: NameInlineClass) {}

fun main() {
    val nameAlias: NameTypeAlias = ""
    val nameInlineClass: NameInlineClass = NameInlineClass("")
    val string: String = ""

    acceptString(nameAlias)              // OK: 별칭은 호환됨
    acceptString(nameInlineClass)        // 불가: 인라인 클래스는 호환되지 않음

    acceptNameTypeAlias(string)          // OK
    acceptNameInlineClass(string)        // 불가
}
```

## 인라인 클래스와 위임

인터페이스와 함께 인라인된 값에 대한 구현 위임이 허용됩니다:

```kotlin
interface MyInterface {
    fun bar()
    fun foo() = "foo"
}

@JvmInline value class MyInterfaceWrapper(val myInterface: MyInterface) : MyInterface by myInterface

fun main() {
    val my = MyInterfaceWrapper(object : MyInterface {
        override fun bar() { /* 본문 */ }
    })
    println(my.foo()) // "foo" 출력
}
```

## 관련 주제

- [열거형 클래스](enum-classes.md)
- [중첩 및 내부 클래스](nested-classes.md)

---

# 함수형 (SAM) 인터페이스

> **원문:** https://kotlinlang.org/docs/fun-interfaces.html

## 개요

**함수형 인터페이스**(**단일 추상 메서드(SAM) 인터페이스**라고도 함)는 단 하나의 추상 멤버 함수만 가진 인터페이스입니다. 여러 개의 비추상 멤버 함수를 가질 수 있지만 정확히 하나의 추상 멤버만 있어야 합니다.

## 선언

함수형 인터페이스를 선언하려면 `fun` 수정자를 사용합니다:

```kotlin
fun interface KRunnable {
    fun invoke()
}
```

## SAM 변환

SAM 변환을 사용하면 명시적인 클래스 구현을 생성하는 대신 람다 표현식을 사용할 수 있어 코드가 더 간결하고 읽기 쉬워집니다.

### 예제

**SAM 변환 없이** (장황함):
```kotlin
fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

val isEven = object : IntPredicate {
    override fun accept(i: Int): Boolean {
        return i % 2 == 0
    }
}
```

**SAM 변환 사용** (간결함):
```kotlin
fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

val isEven = IntPredicate { it % 2 == 0 }

fun main() {
    println("Is 7 even? - ${isEven.accept(7)}")
}
```

## 생성자 함수에서 마이그레이션

Kotlin 1.6.20부터 함수형 인터페이스 생성자에 대한 호출 가능 참조를 사용할 수 있습니다:

**이전** (생성자 함수 사용):
```kotlin
interface Printer {
    fun print()
}

fun Printer(block: () -> Unit): Printer = object : Printer {
    override fun print() = block()
}
```

**이후** (함수형 인터페이스):
```kotlin
fun interface Printer {
    fun print()
}

// 호출 가능 참조 사용
documentsStorage.addPrinter(::Printer)
```

바이너리 호환성을 유지하려면 이전 함수를 deprecated로 표시합니다:
```kotlin
@Deprecated(message = "Your message", level = DeprecationLevel.HIDDEN)
fun Printer(...) {...}
```

## 함수형 인터페이스 vs 타입 별칭

### 타입 별칭 예제
```kotlin
typealias IntPredicate = (i: Int) -> Boolean
val isEven: IntPredicate = { it % 2 == 0 }

fun main() {
    println("Is 7 even? - ${isEven(7)}")
}
```

### 주요 차이점

| 특징 | 타입 별칭 | 함수형 인터페이스 |
|------|----------|-----------------|
| 타입 생성 | 단지 이름일 뿐 (새 타입 없음) | 새 타입 생성 |
| 멤버 | 하나만 | 여러 비추상 + 하나의 추상 |
| 확장 | 인터페이스 특정 확장 제공 불가 | 특정 확장 제공 가능 |
| 상속 | 인터페이스 구현/확장 불가 | 다른 인터페이스 구현/확장 가능 |
| 런타임 비용 | 더 낮음 | 잠재적으로 더 높음 (변환 필요) |

### 각각 언제 사용할까

- **타입 별칭**: API가 특정 매개변수와 반환 타입을 가진 어떤 함수든 받아들일 때 사용
- **함수형 인터페이스**: 함수 타입 시그니처로 표현할 수 없는 복잡한 계약과 연산을 가진 엔티티에 사용

---

# Kotlin this 표현식

> **원문:** https://kotlinlang.org/docs/this-expressions.html

## 개요

this 표현식은 Kotlin에서 현재 수신자를 나타냅니다. `this` 키워드는 컨텍스트에 따라 다른 스코프를 참조하는 데 사용됩니다.

## 사용 컨텍스트

### 클래스에서
클래스의 멤버에서 `this`는 해당 클래스의 현재 객체를 참조합니다.

### 확장 함수와 함수 리터럴에서
확장 함수나 수신자가 있는 함수 리터럴에서 `this`는 점의 왼쪽에 전달된 수신자 매개변수를 나타냅니다.

### 기본 동작
`this`에 한정자가 없으면 가장 안쪽 둘러싸는 스코프를 참조합니다.

---

## 한정된 this

외부 스코프 (클래스, 확장 함수, 또는 레이블이 지정된 수신자가 있는 함수 리터럴)에서 `this`에 접근하려면 `this@label` 구문을 사용합니다. 여기서 `@label`은 스코프의 레이블입니다.

### 예제:
```kotlin
class A { // 암시적 레이블 @A
    inner class B { // 암시적 레이블 @B
        fun Int.foo() { // 암시적 레이블 @foo
            val a = this@A // A의 this
            val b = this@B // B의 this
            val c = this // foo()의 수신자, Int
            val c1 = this@foo // foo()의 수신자, Int

            val funLit = lambda@ fun String.() {
                val d = this // funLit의 수신자, String
            }

            val funLit2 = { s: String ->
                // 둘러싸는 람다 표현식에 수신자가 없으므로
                // foo()의 수신자
                val d1 = this
            }
        }
    }
}
```

---

## 암시적 this

`this`에서 멤버 함수를 호출할 때 `this.` 부분을 생략할 수 있습니다. 그러나 같은 이름의 비멤버 함수가 존재하면 대신 호출될 수 있으므로 주의하세요.

### 예제:
```kotlin
fun main() {
    fun printLine() {
        println("Local function")
    }

    class A {
        fun printLine() {
            println("Member function")
        }

        fun invokePrintLine(omitThis: Boolean = false) {
            if (omitThis) printLine() else this.printLine()
        }
    }

    A().invokePrintLine() // Member function
    A().invokePrintLine(omitThis = true) // Local function
}
```

---

# 타입 검사와 캐스트

> **원문:** https://kotlinlang.org/docs/typecasts.html

## 개요

Kotlin에서는 런타임에 객체가 특정 타입인지 확인하고 다른 타입으로 변환할 수 있습니다. 타입 검사는 다루고 있는 객체의 종류를 확인하고, 타입 캐스트는 객체를 다른 타입으로 변환하려고 시도합니다.

---

## `is`와 `!is` 연산자로 검사

`is` 연산자 (또는 부정을 위한 `!is`)를 사용하여 런타임에 객체가 타입과 일치하는지 확인합니다:

```kotlin
fun main() {
    val input: Any = "Hello, Kotlin"

    if (input is String) {
        println("Message length: ${input.length}") // Message length: 13
    }

    if (input !is String) {
        println("Input is not a valid message")
    } else {
        println("Processing message: ${input.length} characters") // Processing message: 13 characters
    }
}
```

### 하위 타입 검사

`is`와 `!is`를 사용하여 객체가 하위 타입과 일치하는지도 확인할 수 있습니다:

```kotlin
interface Animal {
    val name: String
    fun speak()
}

class Dog(override val name: String) : Animal {
    override fun speak() = println("$name says: Woof!")
}

class Cat(override val name: String) : Animal {
    override fun speak() = println("$name says: Meow!")
}

fun handleAnimal(animal: Animal) {
    println("Handling animal: ${animal.name}")
    animal.speak()

    if (animal is Dog) {
        println("Special care instructions: This is a dog.")
    } else if (animal is Cat) {
        println("Special care instructions: This is a cat.")
    }
}

fun main() {
    val pets: List<Animal> = listOf(
        Dog("Buddy"), Cat("Whiskers"), Dog("Rex")
    )

    for (pet in pets) {
        handleAnimal(pet)
        println("---")
    }
}
```

---

## 스마트 캐스트

컴파일러는 불변 값이면 객체를 자동으로 캐스트하고 암시적(안전한) 캐스트를 자동으로 삽입합니다:

```kotlin
fun logMessage(data: Any) {
    // data는 자동으로 String으로 캐스트됨
    if (data is String) {
        println("Received text: ${data.length} characters")
    }
}

fun main() {
    logMessage("Server started") // Received text: 14 characters
    logMessage(404)
}
```

### 부정 검사를 사용한 스마트 캐스트

부정 검사가 반환으로 이어지면 컴파일러는 캐스트가 안전하다는 것을 알고 있습니다:

```kotlin
fun logMessage(data: Any) {
    // data는 자동으로 String으로 캐스트됨
    if (data !is String) return
    println("Received text: ${data.length} characters")
}

fun main() {
    logMessage("User signed in") // Received text: 14 characters
    logMessage(true)
}
```

### 제어 흐름 (when 표현식)

스마트 캐스트는 `when` 표현식과 함께 작동합니다:

```kotlin
fun processInput(data: Any) {
    when (data) {
        // data는 자동으로 Int로 캐스트됨
        is Int -> println("Log: Assigned new ID ${data + 1}")
        // data는 자동으로 String으로 캐스트됨
        is String -> println("Log: Received message \"$data\"")
        // data는 자동으로 IntArray로 캐스트됨
        is IntArray -> println("Log: Processed scores, total = ${data.sum()}")
    }
}

fun main() {
    processInput(1001) // Log: Assigned new ID 1002
    processInput("System rebooted") // Log: Received message "System rebooted"
    processInput(intArrayOf(10, 20, 30)) // Log: Processed scores, total = 60
}
```

### 제어 흐름 (while 루프)

```kotlin
sealed interface Status
data class Ok(val currentRoom: String) : Status
data object Error : Status

class RobotVacuum(val rooms: List<String>) {
    var index = 0

    fun status(): Status = if (index < rooms.size) Ok(rooms[index]) else Error

    fun clean(): Status {
        println("Finished cleaning ${rooms[index]}")
        index++
        return status()
    }
}

fun main() {
    val robo = RobotVacuum(listOf("Living Room", "Kitchen", "Hallway"))
    var status: Status = robo.status()

    while (status is Ok) {
        // 컴파일러가 status를 Ok 타입으로 스마트 캐스트
        println("Cleaning ${status.currentRoom}...")
        status = robo.clean()
    }
}
```

### Boolean 변수를 사용한 스마트 캐스트

```kotlin
class Cat {
    fun purr() {
        println("Purr purr")
    }
}

fun petAnimal(animal: Any) {
    val isCat = animal is Cat
    if (isCat) {
        // 컴파일러가 animal이 Cat으로 스마트 캐스트되었음을 알고 있음
        animal.purr()
    }
}

fun main() {
    val kitty = Cat()
    petAnimal(kitty) // Purr purr
}
```

### 논리 연산자

컴파일러는 `&&`와 `||` 연산자로 스마트 캐스트를 수행합니다:

```kotlin
// x는 ||의 오른쪽에서 자동으로 String으로 캐스트됨
if (x !is String || x.length == 0) return

// x는 &&의 오른쪽에서 자동으로 String으로 캐스트됨
if (x is String && x.length > 0) {
    print(x.length) // x는 자동으로 String으로 캐스트됨
}
```

다른 타입에 대한 `or` 연산자를 사용하면 공통 상위 타입이 사용됩니다:

```kotlin
interface Status {
    fun signal() {}
}

interface Ok : Status
interface Postponed : Status
interface Declined : Status

fun signalCheck(signalStatus: Any) {
    if (signalStatus is Postponed || signalStatus is Declined) {
        // signalStatus는 공통 상위 타입 Status로 스마트 캐스트됨
        signalStatus.signal()
    }
}
```

### 인라인 함수

```kotlin
interface Processor {
    fun process()
}

inline fun inlineAction(f: () -> Unit) = f()

fun nextProcessor(): Processor? = null

fun runProcessor(): Processor? {
    var processor: Processor? = null

    inlineAction {
        // 컴파일러가 processor가 지역 변수임을 알고 스마트 캐스트 가능
        if (processor != null) {
            processor.process()
        }
        processor = nextProcessor()
    }
    return processor
}
```

### 예외 처리

스마트 캐스트 정보는 `catch`와 `finally` 블록으로 전달됩니다:

```kotlin
fun testString() {
    var stringInput: String? = null
    stringInput = ""

    try {
        // 컴파일러가 stringInput이 null이 아님을 알고 있음
        println(stringInput.length) // 0
        stringInput = null
        if (2 > 1) throw Exception()
        stringInput = ""
    } catch (exception: Exception) {
        // 컴파일러가 stringInput이 null일 수 있음을 알고 있음
        println(stringInput?.length) // null
    }
}

fun main() {
    testString()
}
```

### 스마트 캐스트 전제 조건

스마트 캐스트는 컴파일러가 변수가 변경되지 않을 것임을 보장할 수 있을 때만 작동합니다:

| 변수 타입 | 조건 |
|----------|------|
| `val` 지역 변수 | 항상 (위임된 프로퍼티 제외) |
| `val` 프로퍼티 | `private`, `internal`이거나 검사가 같은 모듈에 있는 경우 |
| `var` 지역 변수 | 수정되지 않고, 수정하는 람다에서 캡처되지 않고, 위임되지 않은 경우 |
| `var` 프로퍼티 | 절대 불가 |

---

## `as`와 `as?` 캐스트 연산자

### 안전하지 않은 캐스트 연산자 (`as`)

`as` 연산자로 캐스트가 실패하면 `ClassCastException`이 발생합니다:

```kotlin
fun main() {
    val rawInput: Any = "user-1234"

    // String으로 성공적으로 캐스트
    val userId = rawInput as String
    println("Logging in user with ID: $userId") // Logging in user with ID: user-1234

    // ClassCastException 발생
    val wrongCast = rawInput as Int
    println("wrongCast contains: $wrongCast") // Exception in thread "main" java.lang.ClassCastException
}
```

### 안전 캐스트 연산자 (`as?`)

`as?` 연산자로 캐스트가 실패하면 `null`을 반환합니다:

```kotlin
fun main() {
    val rawInput: Any = "user-1234"

    // String으로 성공적으로 캐스트
    val userId = rawInput as? String
    println("Logging in user with ID: $userId") // Logging in user with ID: user-1234

    // wrongCast에 null 값 할당
    val wrongCast = rawInput as? Int
    println("wrongCast contains: $wrongCast") // wrongCast contains: null
}
```

### 캐스트 연산자와 Nullable 타입

```kotlin
fun main() {
    val config: Map<String, Any?> = mapOf(
        "username" to "kodee",
        "alias" to null,
        "loginAttempts" to 3
    )

    // nullable String으로 안전하지 않게 캐스트
    val username: String? = config["username"] as String?
    println("Username: $username") // Username: kodee

    // null 값을 nullable String으로 안전하지 않게 캐스트
    val alias: String? = config["alias"] as String?
    println("Alias: $alias") // Alias: null

    // as?로 안전하게 캐스트하여 예외 대신 null 반환
    val safeAttempts: String? = config["loginAttempts"] as? String
    println("Login attempts (safe): $safeAttempts") // Login attempts (safe): null
}
```

---

## 업캐스팅과 다운캐스팅

### 업캐스팅 (상위 타입으로)

업캐스팅은 특별한 구문이 필요 없습니다:

```kotlin
interface Animal {
    fun makeSound()
}

class Dog : Animal {
    override fun makeSound() {
        println("Dog says woof!")
    }
}

fun printAnimalInfo(animal: Animal) {
    animal.makeSound()
}

fun main() {
    val dog = Dog()
    // Dog 인스턴스를 Animal로 업캐스트
    printAnimalInfo(dog) // Dog says woof!
}
```

### 다운캐스팅 (하위 타입으로)

다운캐스팅은 명시적 캐스트 연산자가 필요합니다. 실패 시 안전하게 `null`을 반환하려면 `as?`를 사용하세요:

```kotlin
interface Animal {
    fun makeSound()
}

class Dog : Animal {
    override fun makeSound() {
        println("Dog says woof!")
    }

    fun bark() {
        println("BARK!")
    }
}

fun main() {
    // Animal 타입으로 Dog 인스턴스 생성
    val animal: Animal = Dog()

    // animal을 Dog 타입으로 안전하게 다운캐스트
    val dog: Dog? = animal as? Dog

    // dog가 null이 아니면 bark() 호출을 위해 안전 호출 사용
    dog?.bark() // "BARK!"
}
```

---

## 관련 주제
- [제네릭 타입 검사와 캐스트](generics.html#generics-type-checks-and-casts)
- [리플렉션](reflection.html)
- [제어 흐름](control-flow.html)
- [null 안전성](null-safety.html)
- [인라인 함수](inline-functions.html)
- [위임된 프로퍼티](delegated-properties.html)
- [가시성 수정자](visibility-modifiers.html)

---

# Kotlin 동등성

> **원문:** https://kotlinlang.org/docs/equality.html

## 개요

Kotlin에는 **두 가지 유형의 동등성**이 있습니다:
- **구조적 동등성 (`==`)** - `equals()` 함수를 확인
- **참조 동등성 (`===`)** - 두 참조가 동일한 객체를 가리키는지 확인

---

## 구조적 동등성

**정의:** 두 객체가 동일한 내용 또는 구조가 있는지 확인합니다.

**연산자:** `==`와 그 부정 `!=`

**작동 방식:**
`a == b`와 같은 표현식은 다음과 같이 변환됩니다:
```kotlin
a?.equals(b) ?: (b === null)
```

**예제:**
```kotlin
fun main() {
    var a = "hello"
    var b = "hello"
    var c = null
    var d = null
    var e = d

    println(a == b) // true
    println(a == c) // false
    println(c == e) // true
}
```

**핵심 포인트:**
- `equals()` 함수는 기본적으로 `Any` 클래스에서 상속됨
- 기본 구현은 참조 동등성을 제공
- **값 클래스**와 **데이터 클래스**는 구조적 동등성을 위해 자동으로 `equals()`를 재정의
- 비데이터 클래스는 구조적 동등성을 구현하기 위해 수동 재정의 필요
- `null`과의 명시적 비교 (`a == null`)는 자동으로 `a === null`로 변환됨

**사용자 정의 구현:**
```kotlin
class Point(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Point) return false
        // 구조적 동등성을 위해 속성 비교
        return this.x == other.x && this.y == other.y
    }
}
```

**모범 사례:** `equals()`를 재정의할 때 일관성을 위해 `hashCode()`도 재정의하세요.

---

## 참조 동등성

**정의:** 메모리 주소를 확인하여 두 객체가 동일한 인스턴스인지 확인합니다.

**연산자:** `===`와 그 부정 `!==`

**작동 방식:** `a === b`는 `a`와 `b`가 동일한 객체를 가리키는 경우에만 `true`로 평가됩니다.

**예제:**
```kotlin
fun main() {
    var a = "Hello"
    var b = a
    var c = "world"
    var d = "world"

    println(a === b) // true
    println(a === c) // false
    println(c === d) // true
}
```

**참고:** 런타임에 기본 타입 (예: `Int`)의 경우 `===` 동등성은 `==`와 동일합니다.

---

## 부동 소수점 숫자 동등성

**IEEE 754 동작:** 피연산자가 `Float` 또는 `Double`로 정적으로 타입이 지정되면 IEEE 754 표준을 따릅니다.

**비정적 타이핑:** 부동 소수점으로 정적 타입이 지정되지 않으면 구조적 동등성이 적용됩니다:
- `NaN`은 자기 자신과 같음
- `NaN`은 다른 모든 요소보다 큼 (`POSITIVE_INFINITY` 포함)
- `-0.0`은 `0.0`과 같지 **않음**

---

## 배열 동등성

두 배열이 같은 순서로 같은 요소가 있는지 비교하려면 다음을 사용합니다:
```kotlin
contentEquals()
```

---

## 관련 주제
- [널 안전성](null-safety.md)
- [제네릭: in, out, where](generics.md)
- [부동 소수점 숫자 비교](numbers.md#floating-point-numbers-comparison)
- [배열 비교](arrays.md#compare-arrays)

---

# 연산자 오버로딩

> **원문:** https://kotlinlang.org/docs/operator-overloading.html

## 개요

Kotlin은 타입에 미리 정의된 연산자의 커스텀 구현을 허용합니다. 연산자는 미리 정의된 기호 표현(예: `+` 또는 `*`)과 우선순위를 가집니다. 연산자를 구현하려면 해당 타입에 특정 이름을 가진 멤버 함수나 확장 함수를 제공합니다.

함수에 `operator` 수정자를 표시합니다:

```kotlin
interface IndexedContainer {
    operator fun get(index: Int)
}
```

연산자 오버로드를 오버라이드할 때 `operator`를 생략할 수 있습니다:

```kotlin
class OrdersList: IndexedContainer {
    override fun get(index: Int) { /*...*/ }
}
```

## 단항 연산

### 단항 전위 연산자

| 표현식 | 변환 |
|--------|------|
| `+a` | `a.unaryPlus()` |
| `-a` | `a.unaryMinus()` |
| `!a` | `a.not()` |

**예제:**
```kotlin
data class Point(val x: Int, val y: Int)

operator fun Point.unaryMinus() = Point(-x, -y)

val point = Point(10, 20)
fun main() {
    println(-point) // "Point(x=-10, y=-20)" 출력
}
```

### 증가와 감소

| 표현식 | 변환 |
|--------|------|
| `a++` | `a.inc()` + 아래 참조 |
| `a--` | `a.dec()` + 아래 참조 |

`inc()`와 `dec()` 함수는 값을 반환해야 하며 객체를 변경해서는 안 됩니다.

**후위 형식 (`a++`):** 초기 값을 반환하고, 증가된 값을 변수에 할당
**전위 형식 (`++a`):** 증가된 값을 할당하고, 새 값을 반환

## 이항 연산

### 산술 연산자

| 표현식 | 변환 |
|--------|------|
| `a + b` | `a.plus(b)` |
| `a - b` | `a.minus(b)` |
| `a * b` | `a.times(b)` |
| `a / b` | `a.div(b)` |
| `a % b` | `a.rem(b)` |
| `a..b` | `a.rangeTo(b)` |
| `a..<b` | `a.rangeUntil(b)` |

**예제:**
```kotlin
data class Counter(val dayIndex: Int) {
    operator fun plus(increment: Int): Counter {
        return Counter(dayIndex + increment)
    }
}
```

### in 연산자

| 표현식 | 변환 |
|--------|------|
| `a in b` | `b.contains(a)` |
| `a !in b` | `!b.contains(a)` |

### 인덱스 접근 연산자

| 표현식 | 변환 |
|--------|------|
| `a[i]` | `a.get(i)` |
| `a[i, j]` | `a.get(i, j)` |
| `a[i] = b` | `a.set(i, b)` |
| `a[i, j] = b` | `a.set(i, j, b)` |

### invoke 연산자

| 표현식 | 변환 |
|--------|------|
| `a()` | `a.invoke()` |
| `a(i)` | `a.invoke(i)` |
| `a(i, j)` | `a.invoke(i, j)` |

### 복합 대입

| 표현식 | 변환 |
|--------|------|
| `a += b` | `a.plusAssign(b)` |
| `a -= b` | `a.minusAssign(b)` |
| `a *= b` | `a.timesAssign(b)` |
| `a /= b` | `a.divAssign(b)` |
| `a %= b` | `a.remAssign(b)` |

**참고:** Kotlin에서 대입은 표현식이 아닙니다.

### 동등성과 부등성 연산자

| 표현식 | 변환 |
|--------|------|
| `a == b` | `a?.equals(b) ?: (b === null)` |
| `a != b` | `!(a?.equals(b) ?: (b === null))` |

- `equals(other: Any?): Boolean`으로만 작동
- `===`와 `!==` (동일성 검사)는 오버로드 불가

### 비교 연산자

| 표현식 | 변환 |
|--------|------|
| `a > b` | `a.compareTo(b) > 0` |
| `a < b` | `a.compareTo(b) < 0` |
| `a >= b` | `a.compareTo(b) >= 0` |
| `a <= b` | `a.compareTo(b) <= 0` |

모든 비교는 `compareTo()`가 `Int`를 반환해야 합니다.

### 프로퍼티 위임 연산자

`provideDelegate`, `getValue`, `setValue` 연산자 함수는 위임된 프로퍼티 섹션에 설명되어 있습니다.

## 명명된 함수를 위한 중위 호출

중위 함수 호출을 사용하여 커스텀 중위 연산을 시뮬레이션할 수 있습니다.

---

# 구조 분해 선언

> **원문:** https://kotlinlang.org/docs/destructuring-declarations.html

## 개요

구조 분해 선언을 사용하면 객체를 한 번에 여러 변수로 분해할 수 있습니다:

```kotlin
val (name, age) = person
```

이것은 독립적으로 사용할 수 있는 두 개의 새 변수 (`name`과 `age`)를 생성합니다.

## 작동 방식

구조 분해 선언은 `componentN()` 함수 호출로 컴파일됩니다:

```kotlin
val name = person.component1()
val age = person.component2()
```

**핵심 포인트:**
- `componentN()` 함수는 `operator` 키워드로 표시되어야 함
- 필요한 component 함수가 있으면 모든 객체를 구조 분해할 수 있음
- `component3()`, `component4()` 등과 함께 작동

## 사용 사례

### 1. 함수에서 여러 값 반환

```kotlin
data class Result(val result: Int, val status: Status)

fun function(...): Result {
    // 계산
    return Result(result, status)
}

// 사용법
val (result, status) = function(...)
```

데이터 클래스는 자동으로 `componentN()` 함수를 선언합니다.

### 2. Map 구조 분해

```kotlin
for ((key, value) in map) {
    // key와 value로 무언가 수행
}
```

표준 라이브러리는 확장을 제공합니다:
```kotlin
operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>>
operator fun <K, V> Map.Entry<K, V>.component1() = getKey()
operator fun <K, V> Map.Entry<K, V>.component2() = getValue()
```

### 3. For 루프

```kotlin
for ((a, b) in collection) { ... }
```

변수들은 각 요소의 `component1()`과 `component2()`에서 값을 얻습니다.

## 사용하지 않는 변수에 밑줄 사용

사용하지 않는 변수는 밑줄로 건너뜁니다:

```kotlin
val (_, status) = getResult()
```

건너뛴 컴포넌트에 대해서는 `componentN()` 함수가 호출되지 않습니다.

## 람다에서 구조 분해

### 기본 구문

```kotlin
// 대신:
map.mapValues { entry -> "${entry.value}!" }

// 구조 분해 사용:
map.mapValues { (key, value) -> "$value!" }
```

### 매개변수 변형

```kotlin
{ a -> ... }           // 하나의 매개변수
{ a, b -> ... }        // 두 매개변수
{ (a, b) -> ... }      // 구조 분해된 쌍
{ (a, b), c -> ... }   // 구조 분해된 쌍 + 다른 매개변수
```

### 람다에서 사용하지 않는 컴포넌트

```kotlin
map.mapValues { (_, value) -> "$value!" }
```

### 타입 명세

```kotlin
// 전체 매개변수에 대한 타입:
map.mapValues { (_,value): Map.Entry<Int, String> -> "$value!" }

// 특정 컴포넌트에 대한 타입:
map.mapValues { (_,value: String) -> "$value!" }
```

---

# 타입 별칭

> **원문:** https://kotlinlang.org/docs/type-aliases.html

## 개요

타입 별칭은 기존 타입에 대한 대체 이름을 제공합니다. 새 타입을 도입하지 않고 긴 타입 이름, 특히 제네릭 타입을 줄이는 데 유용합니다.

## 기본 사용법

### 컬렉션 타입
```kotlin
typealias NodeSet = Set<Network.Node>
typealias FileTable<K> = MutableMap<K, MutableList<File>>
```

### 함수 타입
```kotlin
typealias MyHandler = (Int, String, Any) -> Unit
typealias Predicate<T> = (T) -> Boolean
```

### 내부 및 중첩 클래스
```kotlin
class A { inner class Inner }
class B { inner class Inner }
typealias AInner = A.Inner
typealias BInner = B.Inner
```

## 타입 별칭 작동 방식

타입 별칭은 새 타입을 생성하지 않습니다 - 기본 타입과 동등합니다. Kotlin 컴파일러는 컴파일 중에 이를 확장합니다:

```kotlin
typealias Predicate<T> = (T) -> Boolean

fun foo(p: Predicate<Int>) = p(42)

fun main() {
    val f: (Int) -> Boolean = { it > 0 }
    println(foo(f)) // "true" 출력

    val p: Predicate<Int> = { it > 0 }
    println(listOf(1, -2).filter(p)) // "[1]" 출력
}
```

## 중첩된 타입 별칭

다른 선언 내부에 타입 별칭을 정의할 수 있지만 중요한 제한이 있습니다:

### 올바른 사용법 (비캡처)
```kotlin
class Dijkstra {
    typealias VisitedNodes = Set<Node>
    private fun step(visited: VisitedNodes, ...) = ...
}
```

### 잘못된 사용법 (타입 매개변수 캡처)
```kotlin
class Graph<Node> {
    // Node를 캡처하므로 잘못됨
    typealias Path = List<Node>
}
```

### 수정된 버전
```kotlin
class Graph<Node> {
    // Node가 타입 별칭 매개변수이므로 올바름
    typealias Path<Node> = List<Node>
}
```

## 중첩된 타입 별칭 규칙

- 기존의 모든 타입 별칭 규칙을 따라야 함
- 참조된 타입이 허용하는 것보다 더 많은 가시성을 노출할 수 없음
- 중첩 클래스와 동일한 스코프를 가짐 (클래스 내부에 정의, 동일한 이름의 부모 별칭 숨김)
- 가시성 제어를 위해 `internal` 또는 `private`으로 표시 가능
- Kotlin 멀티플랫폼의 `expect/actual` 선언에서는 **지원되지 않음**

---

# Kotlin 어노테이션

> **원문:** https://kotlinlang.org/docs/annotations.html

## 개요

어노테이션은 코드 요소에 메타데이터를 첨부하는 태그입니다. 도구와 프레임워크는 이 메타데이터를 컴파일 및 런타임에 처리하여 다양한 작업을 수행합니다. 보일러플레이트 코드 생성, 코딩 표준 적용, 문서 작성과 같은 일반적인 작업을 단순화합니다.

---

## 선언

어노테이션은 `annotation` 키워드를 사용하여 선언됩니다:

```kotlin
annotation class Fancy
```

### 메타 어노테이션
추가 프로퍼티는 메타 어노테이션을 사용하여 지정할 수 있습니다:

- **`@Target`** - 어노테이션할 수 있는 요소를 지정 (클래스, 함수, 프로퍼티, 표현식)
- **`@Retention`** - 어노테이션이 컴파일된 파일에 저장되고 런타임에 보이는지 지정
- **`@Repeatable`** - 단일 요소에 동일한 어노테이션을 여러 번 사용할 수 있게 함
- **`@MustBeDocumented`** - 어노테이션이 공개 API의 일부이며 생성된 문서에 나타나야 함을 표시

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE_PARAMETER,
         AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Fancy
```

---

## 사용

```kotlin
@Fancy class Foo {
    @Fancy fun baz(@Fancy foo: Int): Int {
        return (@Fancy 1)
    }
}
```

### 생성자 어노테이션
주 생성자의 경우 `constructor` 키워드를 추가합니다:

```kotlin
class Foo @Inject constructor(dependency: MyDependency) { ... }
```

### 프로퍼티 접근자 어노테이션
```kotlin
class Foo {
    var x: MyDependency? = null
    @Inject set
}
```

---

## 생성자

어노테이션은 매개변수가 있는 생성자를 가질 수 있습니다:

```kotlin
annotation class Special(val why: String)

@Special("example")
class Foo {}
```

### 허용된 매개변수 타입
- Java 기본 타입 (Int, Long 등)
- 문자열
- 클래스 (`Foo::class`)
- 열거형
- 다른 어노테이션
- 위 타입들의 배열

**참고:** 어노테이션 매개변수는 널 가능이 될 수 없습니다 (JVM 제한).

### 어노테이션을 매개변수로 사용
다른 어노테이션의 매개변수로 어노테이션을 사용할 때 `@` 문자를 생략합니다:

```kotlin
annotation class ReplaceWith(val expression: String)

annotation class Deprecated(
    val message: String,
    val replaceWith: ReplaceWith = ReplaceWith("")
)

@Deprecated("This function is deprecated, use === instead",
            ReplaceWith("this === other"))
```

### 클래스를 인자로 사용
```kotlin
import kotlin.reflect.KClass

annotation class Ann(val arg1: KClass<*>, val arg2: KClass<out Any>)

@Ann(String::class, Int::class)
class MyClass
```

---

## 인스턴스화

Java와 달리 Kotlin은 임의의 코드에서 어노테이션 생성자를 호출할 수 있습니다:

```kotlin
annotation class InfoMarker(val info: String)

fun processInfo(marker: InfoMarker): Unit = TODO()

fun main(args: Array<String>) {
    if (args.isNotEmpty())
        processInfo(getAnnotationReflective(args))
    else
        processInfo(InfoMarker("default"))
}
```

---

## 람다

어노테이션은 람다에 사용할 수 있으며, 생성된 `invoke()` 메서드에 적용됩니다:

```kotlin
annotation class Suspendable

val f = @Suspendable { Fiber.sleep(10) }
```

---

## 어노테이션 사용 위치 대상

프로퍼티나 주 생성자 매개변수를 어노테이션할 때 어떤 Java 요소가 어노테이션을 받는지 지정하는 구문을 사용합니다:

```kotlin
class Example(
    @field:Ann val foo,      // Java 필드만 어노테이션
    @get:Ann val bar,        // Java getter만 어노테이션
    @param:Ann val quux      // Java 생성자 매개변수만 어노테이션
)
```

### 파일 수준 어노테이션
파일 맨 위 package 지시자 전에 어노테이션을 배치합니다:

```kotlin
@file:JvmName("Foo")
package org.jetbrains.demo
```

### 동일 대상에 여러 어노테이션
```kotlin
class Example {
    @set:[Inject VisibleForTesting] var collaborator: Collaborator
}
```

### 지원되는 사용 위치 대상
- `file`
- `field`
- `property` (Java에서 보이지 않음)
- `get` (프로퍼티 getter)
- `set` (프로퍼티 setter)
- `all` (프로퍼티용 실험적 메타 대상)
- `receiver` (확장 함수/프로퍼티 수신자)
  ```kotlin
  fun @receiver:Fancy String.myExtension() { ... }
  ```
- `param` (생성자 매개변수)
- `setparam` (프로퍼티 setter 매개변수)
- `delegate` (위임된 프로퍼티 필드)

### 기본 대상 (지정하지 않을 때)
기본 선택 순서: `param` -> `property` -> `field`

`@Email` 어노테이션 예제:
```kotlin
data class User(
    val username: String,
    @Email  // @param:Email과 동일
    val email: String
) {
    @Email  // @field:Email과 동일
    val secondaryEmail: String? = null
}
```

### `all` 메타 대상
매개변수, 프로퍼티, 필드, getter, setter 매개변수, RECORD_COMPONENT (해당되는 경우)에 어노테이션을 적용합니다:

```kotlin
data class User(
    val username: String,
    @all:Email val email: String,           // param, field, get
    @all:Email var name: String,            // param, field, get, setparam
) {
    @all:Email val secondaryEmail: String? = null  // field, get
}
```

**활성화:** `-Xannotation-target-all`

#### 제한사항
- 타입, 확장 수신자, 컨텍스트 수신자에 전파되지 않음
- 여러 어노테이션과 함께 사용 불가: `@all:[A B]` (`@all:A @all:B` 대신 사용)
- 위임된 프로퍼티와 함께 사용 불가

---

## Java 어노테이션

Kotlin은 Java 어노테이션과 100% 호환됩니다:

```kotlin
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.*

class Tests {
    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun simple() {
        val f = tempFolder.newFile()
        assertEquals(42, getTheAnswer())
    }
}
```

### Java 어노테이션의 명명된 인자
명명된 인자 구문을 사용합니다 (Java에서 매개변수 순서가 정의되지 않음):

```kotlin
// Java
public @interface Ann {
    int intValue();
    String stringValue();
}

// Kotlin
@Ann(intValue = 1, stringValue = "abc")
class C
```

### 특별한 `value` 매개변수
명시적 이름 없이 지정할 수 있습니다:

```kotlin
// Java
public @interface AnnWithValue {
    String value();
}

// Kotlin
@AnnWithValue("abc")
class C
```

### 어노테이션 매개변수로서의 배열
배열 타입의 `value` 인자 (Kotlin에서 `vararg`가 됨):

```kotlin
// Java
public @interface AnnWithArrayValue {
    String[] value();
}

// Kotlin
@AnnWithArrayValue("abc", "foo", "bar")
class C
```

다른 배열 인자의 경우 배열 리터럴 구문을 사용합니다:

```kotlin
@AnnWithArrayMethod(names = ["abc", "foo", "bar"])
class C
```

### 어노테이션 프로퍼티 접근
```kotlin
fun foo(ann: Ann) {
    val i = ann.value
}
```

### JVM 1.8+ 어노테이션 대상 비활성화
Android API 레벨 < 26의 경우: `-Xno-new-java-annotation-targets`

---

## 반복 가능한 어노테이션

단일 요소에 여러 번 사용하려면 `@kotlin.annotation.Repeatable`로 어노테이션을 표시합니다:

```kotlin
@Repeatable
annotation class Tag(val name: String)
// 컴파일러가 @Tag.Container 포함 어노테이션 생성
```

### 사용자 정의 포함 어노테이션
`@kotlin.jvm.JvmRepeatable`을 사용합니다:

```kotlin
@JvmRepeatable(Tags::class)
annotation class Tag(val name: String)

annotation class Tags(val value: Array<Tag>)
```

### 반복 가능한 어노테이션 접근
리플렉션에는 `KAnnotatedElement.findAnnotations()` 함수를 사용합니다.

---

# Kotlin 리플렉션

> **원문:** https://kotlinlang.org/docs/reflection.html

## 개요

리플렉션은 런타임에 프로그램의 구조를 검사할 수 있게 해주는 언어 및 라이브러리 기능의 집합입니다. 함수와 프로퍼티는 Kotlin에서 일급 시민이며, 이들을 검사하는 능력은 함수형 또는 반응형 스타일을 사용할 때 필수적입니다.

**참고:** Kotlin/JS는 리플렉션 기능에 대한 제한적인 지원을 제공합니다.

---

## JVM 의존성

JVM 플랫폼에서 Kotlin 컴파일러 배포판은 리플렉션을 사용하지 않는 애플리케이션의 런타임 라이브러리 크기를 줄이기 위해 `kotlin-reflect.jar`를 별도의 아티팩트로 포함합니다.

### 의존성 추가

**Gradle:**
```kotlin
dependencies { implementation(kotlin("reflect")) }
```
또는
```groovy
dependencies { implementation "org.jetbrains.kotlin:kotlin-reflect:2.3.10" }
```

**Maven:**
```xml
<dependencies>
  <dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-reflect</artifactId>
  </dependency>
</dependencies>
```

다른 설정의 경우 `kotlin-reflect.jar`가 프로젝트의 클래스패스에 있는지 확인하세요. 필요 없다면 제외하려면 `-no-reflect` 컴파일러 옵션을 사용하세요.

---

## 클래스 참조

### 기본 클래스 참조

클래스 리터럴 구문을 사용하여 Kotlin 클래스에 대한 런타임 참조를 얻습니다:

```kotlin
val c = MyClass::class
```

이것은 `KClass` 타입 값을 반환합니다.

**JVM 참고:** Kotlin 클래스 참조는 Java 클래스 참조와 다릅니다. Java 클래스 참조를 얻으려면 `.java` 프로퍼티를 사용하세요:

```kotlin
val javaClass = myKotlinClass.java
```

### 바운드 클래스 참조

특정 객체의 클래스에 대한 참조를 얻습니다:

```kotlin
val widget: Widget = ...
assert(widget is GoodWidget) { "Bad widget: ${widget::class.qualifiedName}" }
```

수신자 타입에 관계없이 객체의 정확한 런타임 클래스를 얻습니다.

---

## 호출 가능 참조

함수, 프로퍼티, 생성자에 대한 참조는 호출하거나 함수 타입 인스턴스로 사용할 수 있습니다. 공통 슈퍼타입은 `KCallable<out R>`이며, 여기서 `R`은 반환 타입입니다.

### 함수 참조

명명된 함수를 직접 호출하거나 함수 타입 값으로 사용합니다:

```kotlin
fun isOdd(x: Int) = x % 2 != 0

fun main() {
    val numbers = listOf(1, 2, 3)
    println(numbers.filter(::isOdd))
}
```

여기서 `::isOdd`는 `(Int) -> Boolean` 타입의 값입니다.

함수 참조는 매개변수 수에 따라 `KFunction<out R>` 하위 타입에 속합니다 (예: `KFunction3<T1, T2, T3, R>`).

#### 오버로드된 함수

오버로드된 함수를 사용할 때 컨텍스트에서 예상 타입이 알려져야 합니다:

```kotlin
fun main() {
    fun isOdd(x: Int) = x % 2 != 0
    fun isOdd(s: String) = s == "brillig" || s == "slithy" || s == "tove"

    val numbers = listOf(1, 2, 3)
    println(numbers.filter(::isOdd)) // isOdd(x: Int) 참조
}
```

또는 타입을 명시적으로 지정합니다:

```kotlin
val predicate: (String) -> Boolean = ::isOdd // isOdd(x: String) 참조
```

#### 멤버 및 확장 함수

클래스 멤버나 확장 함수의 경우 한정된 구문을 사용합니다: `String::toCharArray`

```kotlin
val isEmptyStringList: List<String>.() -> Boolean = List<String>::isEmpty
```

#### 예제: 함수 합성

```kotlin
fun <A, B, C> compose(f: (B) -> C, g: (A) -> B): (A) -> C {
    return { x -> f(g(x)) }
}

fun isOdd(x: Int) = x % 2 != 0
fun length(s: String) = s.length

fun main() {
    val oddLength = compose(::isOdd, ::length)
    val strings = listOf("a", "ab", "abc")
    println(strings.filter(oddLength))
}
```

### 프로퍼티 참조

프로퍼티를 일급 객체로 접근합니다:

```kotlin
val x = 1
fun main() {
    println(::x.get())
    println(::x.name)
}
```

`KProperty0<Int>`를 반환합니다. 값을 읽으려면 `get()`을, 프로퍼티 이름을 가져오려면 `name`을 사용합니다.

#### 가변 프로퍼티

가변 프로퍼티의 경우 `::y`는 `set()` 메서드가 있는 `KMutableProperty0<Int>`를 반환합니다:

```kotlin
var y = 1
fun main() {
    ::y.set(2)
    println(y) // 2 출력
}
```

#### 프로퍼티 참조를 함수로 사용

```kotlin
fun main() {
    val strs = listOf("a", "bc", "def")
    println(strs.map(String::length))
}
```

#### 클래스 멤버 프로퍼티

```kotlin
fun main() {
    class A(val p: Int)
    val prop = A::p
    println(prop.get(A(1)))
}
```

#### 확장 프로퍼티

```kotlin
val String.lastChar: Char get() = this[length - 1]

fun main() {
    println(String::lastChar.get("abc"))
}
```

### Java 리플렉션과의 상호운용성

표준 라이브러리는 Kotlin과 Java 리플렉션 객체 간 매핑을 위한 확장을 `kotlin.reflect.jvm` 패키지에 포함합니다:

```kotlin
import kotlin.reflect.jvm.*

class A(val p: Int)

fun main() {
    println(A::p.javaGetter) // "public final int A.getP()" 출력
    println(A::p.javaField)  // "private final int A.p" 출력
}
```

Java 클래스에 해당하는 Kotlin 클래스를 얻습니다:

```kotlin
fun getKClass(o: Any): KClass<Any> = o.javaClass.kotlin
```

### 생성자 참조

클래스 이름과 함께 `::` 연산자를 사용하여 메서드와 프로퍼티처럼 생성자를 참조합니다:

```kotlin
class Foo

fun function(factory: () -> Foo) {
    val x: Foo = factory()
}

function(::Foo)
```

생성자 참조는 매개변수 수에 따라 `KFunction<out R>` 하위 타입으로 타입이 지정됩니다.

### 바운드 함수와 프로퍼티 참조

특정 객체의 인스턴스 메서드를 참조합니다:

```kotlin
fun main() {
    val numberRegex = "\\d+".toRegex()
    println(numberRegex.matches("29"))

    val isNumber = numberRegex::matches
    println(isNumber("29"))
}
```

함수 타입 표현식에서 사용:

```kotlin
fun main() {
    val numberRegex = "\\d+".toRegex()
    val strings = listOf("abc", "124", "a70")
    println(strings.filter(numberRegex::matches))
}
```

#### 타입 비교

바운드 참조는 수신자가 첨부되어 매개변수 목록에서 제거됩니다:

```kotlin
val isNumber: (CharSequence) -> Boolean = numberRegex::matches
val matches: (Regex, CharSequence) -> Boolean = Regex::matches
```

#### 바운드 프로퍼티 참조

```kotlin
fun main() {
    val prop = "abc"::length
    println(prop.get()) // 3 출력
}
```

### 바운드 생성자 참조

외부 클래스 인스턴스를 제공하여 내부 클래스 생성자에 대한 바운드 참조를 얻습니다:

```kotlin
class Outer {
    inner class Inner
}

val o = Outer()
val boundInnerCtor = o::Inner
```

---

**관련 주제:** [코루틴](coroutines-overview.md) | [구조 분해 선언](destructuring-declarations.md)

---

# 제네릭: in, out, where

> **원문:** https://kotlinlang.org/docs/generics.html

## 개요

Kotlin의 클래스는 Java와 유사하게 타입 매개변수를 가질 수 있습니다. 타입 인수는 문맥에서 추론될 수 있어 더 깔끔한 문법이 가능합니다.

```kotlin
class Box<T>(t: T) { var value = t }

val box: Box<Int> = Box<Int>(1)
val box = Box(1) // 타입 추론: Box<Int>
```

## 변성

### Java에서의 변성과 와일드카드

Java의 제네릭 타입은 **불변**입니다. 즉 `List<String>`은 `List<Object>`의 서브타입이 아닙니다. 이는 런타임 오류를 방지하지만 유연성이 떨어집니다.

**문제 예제:**
```java
List<String> strs = new ArrayList<String>();
List<Object> objs = strs; // Java에서 컴파일 오류
objs.add(1); // 런타임에 ClassCastException 발생 가능
```

**해결책 - 와일드카드:**
- **`? extends E` (공변성 - 생산자)**: 읽기 전용, 서브타입 전달 안전
- **`? super E` (반공변성 - 소비자)**: 쓰기 전용, 상위 타입 받기 안전

**PECS 기억법**: Producer-Extends, Consumer-Super

### 선언 위치 변성

Kotlin은 **타입 매개변수 선언 위치**에서 변성 어노테이션으로 이 문제를 해결합니다:

#### `out` 수정자 (공변성)

타입 매개변수를 **생산 전용**(반환만, 소비 안 함)으로 만듭니다:

```kotlin
interface Source<out T> {
    fun nextT(): T
}

fun demo(strs: Source<String>) {
    val objects: Source<Any> = strs // OK! T가 공변
}
```

#### `in` 수정자 (반공변성)

타입 매개변수를 **소비 전용**(받기만, 반환 안 함)으로 만듭니다:

```kotlin
interface Comparable<in T> {
    operator fun compareTo(other: T): Int
}

fun demo(x: Comparable<Number>) {
    val y: Comparable<Double> = x // OK! 서브타입 할당 가능
}
```

**기억법**: "Consumer in, Producer out!"

## 타입 프로젝션

### 사용 위치 변성

생산과 소비를 모두 해야 하는 클래스(예: `Array`)의 경우 호출 위치에서 **타입 프로젝션**을 사용합니다:

```kotlin
class Array<T>(val size: Int) {
    operator fun get(index: Int): T { ... }
    operator fun set(index: Int, value: T) { ... }
}

// 문제: Array<Int>는 Array<Any>의 서브타입이 아님
fun copy(from: Array<Any>, to: Array<Any>) { ... }

val ints: Array<Int> = arrayOf(1, 2, 3)
val any = Array<Any>(3) { "" }
copy(ints, any) // 오류: 타입 불일치
```

**해결책 - `out`으로 타입 프로젝션:**
```kotlin
fun copy(from: Array<out Any>, to: Array<Any>) {
    for (i in from.indices) {
        to[i] = from[i] // 안전: from은 읽기 전용
    }
}
```

**`in`으로 타입 프로젝션:**
```kotlin
fun fill(dest: Array<in String>, value: String) { ... }
// 받을 수 있음: Array<String>, Array<CharSequence>, Array<Object>
```

### 스타 프로젝션

타입 인수를 모르지만 타입 안전 연산을 원할 때 `*`를 사용합니다:

- **`Foo<out T : TUpper>` -> `Foo<*>`**: `Foo<out TUpper>`와 동일 (읽기 안전)
- **`Foo<in T>` -> `Foo<*>`**: `Foo<in Nothing>`과 동일 (쓰기 불안전)
- **`Foo<T : TUpper>` (불변) -> `Foo<*>`**: `Foo<out TUpper>`로 읽기, `Foo<in Nothing>`으로 쓰기

```kotlin
interface Function<in T, out U>

Function<*, String>      // Function<in Nothing, String>
Function<Int, *>         // Function<Int, out Any?>
Function<*, *>           // Function<in Nothing, out Any?>
```

## 제네릭 함수

타입 매개변수는 함수 이름 앞에 배치합니다:

```kotlin
fun <T> singletonList(item: T): List<T> { ... }
fun <T> T.basicToString(): String { ... }

val l = singletonList<Int>(1)
val l = singletonList(1) // 타입 추론
```

## 제네릭 제약

### 상한

`extends` 문법을 사용하여 타입 매개변수를 제한합니다:

```kotlin
fun <T : Comparable<T>> sort(list: List<T>) { ... }

sort(listOf(1, 2, 3)) // OK: Int는 Comparable<Int>
sort(listOf(HashMap<Int, String>())) // 오류
```

**기본 상한**: `Any?`

### where를 사용한 다중 상한

```kotlin
fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
    where T : CharSequence, T : Comparable<T> {
    return list.filter { it > threshold }.map { it.toString() }
}
```

## 명확히 널이 아닌 타입

Java 상호운용성을 위해 `T & Any`를 사용하여 제네릭 타입을 명확히 널이 아닌 것으로 선언합니다:

```kotlin
interface ArcadeGame<T1> : Game<T1> {
    override fun load(x: T1 & Any): T1 & Any
}
```

## 타입 소거

타입 정보는 **런타임에 소거**됩니다. `Foo<Bar>`와 `Foo<Baz?>`의 인스턴스는 모두 `Foo<*>`가 됩니다.

### 타입 검사와 캐스트

**허용되지 않음:**
```kotlin
if (something is List<Int>) { } // 오류: 타입 인수 소거됨
```

**허용됨 - 스타 프로젝션 검사:**
```kotlin
if (something is List<*>) {
    something.forEach { println(it) } // 아이템은 Any? 타입
}
```

**비제네릭 부분 검사:**
```kotlin
fun handleStrings(list: MutableList<String>) {
    if (list is ArrayList) { // OK! 비제네릭 검사
        // list가 ArrayList<String>으로 스마트 캐스트
    }
}
```

### 구체화된 타입 매개변수

`inline` 함수만 런타임 검사를 위해 구체화된 타입을 지원합니다:

```kotlin
inline fun <reified A, reified B> Pair<*, *>.asPairOf(): Pair<A, B>? {
    if (first !is A || second !is B) return null
    return first as A to second as B
}

val somePair: Pair<Any?, Any?> = "items" to listOf(1, 2, 3)
val stringToList = somePair.asPairOf<String, List<*>>() // 작동!
```

### 검사되지 않은 캐스트

`foo as List<String>`과 같은 구체적인 제네릭 타입으로의 캐스트는 런타임에 검사되지 않습니다:

```kotlin
fun readDictionary(file: File): Map<String, *> = ...

val intsFile = File("ints.dictionary")
// 경고: 검사되지 않은 캐스트
val intsDictionary: Map<String, Int> = readDictionary(intsFile) as Map<String, Int>
```

**경고 억제:**
```kotlin
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<*>.asListOfType(): List<T>? =
    if (all { it is T }) this as List<T> else null
```

## 타입 인수를 위한 밑줄 연산자

다른 타입이 명시적으로 지정될 때 타입 인수를 자동으로 추론하려면 `_`를 사용합니다:

```kotlin
abstract class SomeClass<T> { abstract fun execute(): T }
class SomeImplementation : SomeClass<String>() { override fun execute(): String = "Test" }

object Runner {
    inline fun <reified S: SomeClass<T>, T> run(): T {
        return S::class.java.getDeclaredConstructor().newInstance().execute()
    }
}

fun main() {
    val s = Runner.run<SomeImplementation, _>() // T가 String으로 추론됨
    assert(s == "Test")
}
```

## 핵심 정리

| 개념 | 목적 | 예제 |
|------|------|------|
| `out T` | 공변성 (생산 전용) | `Source<out T>` |
| `in T` | 반공변성 (소비 전용) | `Comparable<in T>` |
| 타입 프로젝션 | 사용 위치 변성 | `Array<out Any>` |
| 스타 프로젝션 | 알 수 없는 타입 인수 | `List<*>` |
| 구체화 | 인라인 함수의 런타임 타입 정보 | `inline fun <reified T>` |
| 상한 | 타입 매개변수 제한 | `<T : Comparable<T>>` |
| `T & Any` | 명확히 널이 아님 | Java 상호운용 |
