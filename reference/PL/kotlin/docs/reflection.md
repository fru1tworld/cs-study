# Kotlin 리플렉션

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
