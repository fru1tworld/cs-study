# 인라인 값 클래스

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

Kotlin 컴파일러는 각 인라인 클래스에 대해 래퍼를 유지하지만 성능을 위해 언박싱된(기본 타입) 표현을 선호합니다. 인라인 클래스는 다른 타입으로 사용될 때마다 **박싱**됩니다.

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
