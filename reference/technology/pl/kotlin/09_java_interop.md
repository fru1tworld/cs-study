# 자바 상호운용

# Kotlin에서 Java 호출하기

> **원문:** https://kotlinlang.org/docs/java-interop.html

Kotlin은 원활한 Java 상호운용성을 위해 설계되었습니다. Java 코드는 Kotlin에서 자연스럽게 호출할 수 있으며, 그 반대도 마찬가지입니다.

## 기본 Java 사용

대부분의 Java 코드는 Kotlin에서 문제없이 작동합니다:

```kotlin
import java.util.*

fun demo(source: List<Int>) {
    val list = ArrayList<Int>()
    // for 루프는 Java 컬렉션과 함께 작동
    for (item in source) {
        list.add(item)
    }
    // 연산자 규칙이 작동
    for (i in 0..source.size - 1) {
        list[i] = source[i]  // get과 set이 호출됨
    }
}
```

## Getter와 Setter

명명 규칙을 따르는 Java getter/setter 메서드는 Kotlin 프로퍼티로 표현됩니다:

- `getX()` 및 `setX()` 같은 메서드는 프로퍼티 `x`가 됨
- `isX()` 및 `setX()` 같은 메서드는 프로퍼티 `x`가 됨

```kotlin
import java.util.Calendar

fun calendarDemo() {
    val calendar = Calendar.getInstance()
    if (calendar.firstDayOfWeek == Calendar.SUNDAY) {
        calendar.firstDayOfWeek = Calendar.MONDAY  // setFirstDayOfWeek() 호출
    }
    if (!calendar.isLenient) {
        calendar.isLenient = true  // setLenient() 호출
    }
}
```

### Java 합성 프로퍼티 참조 (Kotlin 1.8.20+)

Java 합성 프로퍼티에 대한 참조를 생성할 수 있습니다:

```kotlin
val persons = listOf(Person("Jack", 11), Person("Sofie", 12), Person("Peter", 11))
persons
    .sortedBy(Person::age)  // 합성 프로퍼티에 대한 참조
    .forEach { person -> println(person.name) }
```

**Gradle에서 활성화:**

```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>()
    .configureEach {
        compilerOptions.languageVersion.set(
            org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
        )
    }
```

## void를 반환하는 메서드

Java `void` 메서드는 Kotlin에서 `Unit`을 반환합니다:

```kotlin
// Java: void method()
// Kotlin: 반환 값이 호출 지점에서 Unit으로 할당됨
```

## Kotlin 키워드 이스케이프

백틱을 사용하여 Kotlin 키워드와 동일한 이름의 메서드를 호출합니다:

```kotlin
foo.`is`(bar)
```

## Null 안전성과 플랫폼 타입

모든 Java 참조는 `null`일 수 있어 Kotlin의 null 안전성과 불일치가 발생합니다. Java 타입은 완화된 null 검사를 가진 **플랫폼 타입**으로 처리됩니다.

### 플랫폼 타입 표기법

- `T!`는 "T 또는 T?"를 의미
- `(Mutable)Collection<T>!`는 "T의 Java 컬렉션 (가변성과 null 가능성 알 수 없음)"을 의미
- `Array<(out) T>!`는 "T 또는 하위 타입의 Java 배열, null 가능성 알 수 없음"을 의미

```kotlin
val list = ArrayList<String>()  // non-null (생성자 결과)
val size = list.size             // non-null (원시 int)
val item = list[0]               // 플랫폼 타입 (추론됨)

item.substring(1)  // 허용됨, item == null이면 예외 발생
```

### 플랫폼 타입의 타입 안전성

플랫폼 값을 할당할 때 예상 타입을 선택할 수 있습니다:

```kotlin
val nullable: String? = item   // 허용됨, 항상 작동
val notNull: String = item     // 허용됨, 런타임에 실패할 수 있음 (assertion 생성됨)
```

## Null 가능성 어노테이션

null 가능성 어노테이션이 있는 Java 타입은 nullable/non-nullable Kotlin 타입으로 처리됩니다:

**지원되는 어노테이션:**
- JetBrains: `@Nullable`, `@NotNull`
- JSpecify: `org.jspecify.annotations`
- Android: `com.android.annotations`, `android.support.annotations`
- JSR-305: `javax.annotation`
- FindBugs: `edu.umd.cs.findbugs.annotations`
- Eclipse: `org.eclipse.jdt.annotation`
- Lombok: `lombok.NonNull`
- RxJava 3: `io.reactivex.rxjava3.annotations`

**컴파일러 옵션으로 구성:**

```
-Xnullability-annotations=@<package-name>:<report-level>
```

보고 수준: `ignore`, `warn`, `strict`

### JSpecify 지원 (권장)

JSpecify는 통합된 null 가능성 어노테이션을 제공합니다:

```java
// Java
@NullMarked
public class InventoryService {
    public String notNull() { return ""; }
    public @Nullable String nullable() { return null; }
}
```

```kotlin
// Kotlin
fun test(inventory: InventoryService) {
    inventory.notNull().length     // OK
    inventory.nullable().length    // 오류: 안전 호출(?.) 또는 non-null assertion(!!) 필요
}
```

**보고 수준 구성:**

```
-Xjspecify-annotations=<report-level>
```

수준: `strict` (기본값), `warn`, `ignore`

## 매핑된 타입

Java 타입은 Kotlin 동등 타입으로 매핑됩니다:

### 원시 타입

| Java | Kotlin |
|------|--------|
| `byte` | `kotlin.Byte` |
| `int` | `kotlin.Int` |
| `long` | `kotlin.Long` |
| `boolean` | `kotlin.Boolean` |
| 기타 | (동일 패턴) |

### 내장 타입

| Java | Kotlin |
|------|--------|
| `java.lang.Object` | `kotlin.Any!` |
| `java.lang.String` | `kotlin.String!` |
| `java.lang.Throwable` | `kotlin.Throwable!` |

### 박싱 타입

| Java | Kotlin |
|------|--------|
| `java.lang.Integer` | `kotlin.Int?` |
| `java.lang.Boolean` | `kotlin.Boolean?` |
| (기타 박싱된 원시 타입) | (nullable Kotlin 동등 타입) |

### 컬렉션

| Java | Kotlin (읽기 전용) | Kotlin (가변) | 플랫폼 |
|------|------------------|-----------------|----------|
| `Collection<T>` | `Collection<T>` | `MutableCollection<T>` | `(Mutable)Collection<T>!` |
| `List<T>` | `List<T>` | `MutableList<T>` | `(Mutable)List<T>!` |
| `Set<T>` | `Set<T>` | `MutableSet<T>` | `(Mutable)Set<T>!` |
| `Map<K,V>` | `Map<K,V>` | `MutableMap<K,V>` | `(Mutable)Map<K,V>!` |

## Kotlin에서의 Java 제네릭

Java 와일드카드는 타입 프로젝션으로 변환됩니다:

```kotlin
// Java: Foo<? extends Bar> → Kotlin: Foo<out Bar!>!
// Java: Foo<? super Bar> → Kotlin: Foo<in Bar!>!
// Java: List (raw 타입) → Kotlin: List<*>! (List<out Any?>!)
```

**제네릭을 사용한 타입 검사:**

```kotlin
if (a is List<Int>)   // 오류: 런타임에 Int의 List인지 확인할 수 없음
if (a is List<*>)     // OK: 스타 프로젝트된 제네릭 타입 허용
```

## Java 배열

Kotlin에서 배열은 불변입니다 (Java와 다름). 특화된 원시 배열 클래스를 사용합니다:

```java
// Java
public class JavaArrayExample {
    public void removeIndices(int[] indices) { }
    public void removeIndicesVarArg(int... indices) { }
}
```

```kotlin
// Kotlin
val javaObj = JavaArrayExample()
val array = intArrayOf(0, 1, 2, 3)

javaObj.removeIndices(array)           // int[] 매개변수
javaObj.removeIndicesVarArg(*array)    // 스프레드 연산자를 사용한 varargs
```

**특화된 배열 타입:** `IntArray`, `DoubleArray`, `CharArray`, `LongArray` 등.

## Java Varargs

스프레드 연산자 `*`를 사용하여 vararg 매개변수에 배열을 전달합니다:

```kotlin
val array = intArrayOf(0, 1, 2, 3)
javaObj.removeIndicesVarArg(*array)
```

## 연산자

올바른 이름과 시그니처를 가진 모든 Java 메서드는 연산자로 사용할 수 있습니다:

```kotlin
// 적절한 시그니처를 가진 Java 메서드
a + b  // Java 메서드에 대한 연산자 호출
a[i]   // Java 메서드에 대한 인덱싱 연산자
```

참고: Java 메서드에는 중위 호출 구문이 허용되지 않습니다.

## 검사 예외

Kotlin에는 검사 예외가 없습니다. Java에서 예외를 catch할 필요가 없습니다:

```kotlin
fun render(list: List<*>, to: Appendable) {
    for (item in list) {
        to.append(item.toString())  // IOException을 catch할 필요 없음
    }
}
```

## Object 메서드

Java의 `java.lang.Object` 메서드는 확장 함수를 통해 접근합니다:

### `wait()` 및 `notify()`

`Any`에서 사용할 수 없습니다. `java.util.concurrent`를 사용하거나 `java.lang.Object`를 통해 접근합니다:

```kotlin
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private val lock = Object()

synchronized(lock) {
    lock.wait()
    lock.notifyAll()
}
```

### `getClass()`

`java` 확장 프로퍼티를 사용합니다:

```kotlin
val fooClass = foo::class.java     // 바운드 클래스 참조
val fooClass = foo.javaClass        // 확장 프로퍼티
```

### `clone()`

`kotlin.Cloneable`을 확장합니다:

```kotlin
class Example : Cloneable {
    override fun clone(): Any { }
}
```

### `finalize()`

`override` 키워드 없이 선언합니다:

```kotlin
class C {
    protected fun finalize() {
        // 종료 로직
    }
}
```

## Java 클래스 상속

- 최대 **하나의** Java 클래스가 상위 타입이 될 수 있음
- **여러** Java 인터페이스가 상위 타입이 될 수 있음

## 정적 멤버 접근

전체 한정 이름을 사용하여 접근합니다:

```kotlin
if (Character.isLetter(a)) { }
java.lang.Integer.bitCount(foo)
```

매핑된 타입의 경우 전체 Java 이름을 사용합니다:

```kotlin
java.lang.Integer.toHexString(foo)
```

## Java 리플렉션

```kotlin
val fooClass = foo::class.java              // 인스턴스에서 클래스로
val fooClass = ClassName::class.java        // 클래스 참조
val fooClass = foo.javaClass                // 확장 프로퍼티

// 원시 타입의 경우:
Int::class.java              // 원시 int
Int::class.javaObjectType    // 래퍼 Integer
```

## SAM 변환

Kotlin 함수 리터럴은 단일 메서드 Java 인터페이스로 자동 변환됩니다:

```kotlin
val runnable = Runnable { println("This runs in a runnable") }

val executor = ThreadPoolExecutor()
executor.execute { println("This runs in a thread pool") }

// 여러 SAM 메서드가 있는 경우 타입을 지정:
executor.execute(Runnable { println("This runs in a thread pool") })
```

## JNI (Java Native Interface)

네이티브 함수를 `external`로 표시합니다:

```kotlin
external fun foo(x: Int): Double

// 프로퍼티의 경우:
var myProperty: String
    external get
    external set
```

## Kotlin에서 Lombok 사용

Lombok이 생성한 선언은 Kotlin에서 사용할 수 있습니다. Java/Kotlin 혼합 모듈의 경우 [Lombok 컴파일러 플러그인](lombok.html)을 사용합니다.

---

**핵심 요점:** Kotlin은 플랫폼 타입, 합성 프로퍼티, null 가능성 어노테이션, 자동 타입 매핑을 통해 원활한 Java 상호운용성을 제공하여 Java와 Kotlin 코드를 자연스럽게 혼합할 수 있게 합니다.

---

# Java에서 Kotlin 호출하기

> **원문:** https://kotlinlang.org/docs/java-to-kotlin-interop.html

이 포괄적인 가이드는 Java에서 Kotlin 코드를 원활하게 호출하는 방법을 설명하며, 상호운용성 기능과 모범 사례를 다룹니다.

## 프로퍼티

Kotlin 프로퍼티는 Java 요소로 컴파일됩니다:
- **Getter 메서드** (`get` 접두사)
- **Setter 메서드** (`var` 프로퍼티의 경우 `set` 접두사)
- **Private 필드** (프로퍼티와 동일한 이름)

예시:

```kotlin
var firstName: String
```

다음으로 컴파일됩니다:

```java
private String firstName;
public String getFirstName() { return firstName; }
public void setFirstName(String firstName) { this.firstName = firstName; }
```

**특수 명명 규칙**: `is`로 시작하는 프로퍼티는 다른 매핑을 사용합니다. `isOpen`의 경우 getter는 `isOpen()`이고 setter는 `setOpen()`입니다.

## 패키지 레벨 함수

파일 `app.kt` (패키지 `org.example`)의 함수와 프로퍼티는 `org.example.AppKt`의 정적 메서드로 컴파일됩니다.

`@JvmName`으로 클래스 이름을 커스터마이즈합니다:

```kotlin
@file:JvmName("DemoUtils")
package org.example

fun getTime() { /*...*/ }
```

Java에서 다음과 같이 호출됩니다: `org.example.DemoUtils.getTime()`

여러 파일을 단일 facade 클래스로 병합하려면 `@JvmMultifileClass`를 사용합니다.

## 인스턴스 필드

`@JvmField`를 사용하여 프로퍼티를 Java 필드로 노출합니다:

```kotlin
class User(id: String) {
    @JvmField val ID = id
}
```

Java에서 다음과 같이 사용됩니다:

```java
return user.ID;  // 직접 필드 접근
```

요구 사항:
- 백킹 필드가 있어야 함
- private이 아니어야 함
- `open`, `override`, `const` 수정자 없음
- 위임 프로퍼티가 아님

## 정적 필드

컴패니언 객체 또는 명명된 객체의 프로퍼티는 정적 필드가 됩니다. 다음으로 노출합니다:

**`@JvmField` 어노테이션:**

```kotlin
companion object {
    @JvmField val COMPARATOR: Comparator<Key> = compareBy<Key> { it.value }
}
```

Java에서 접근: `Key.COMPARATOR`

**`lateinit` 수정자:**

```kotlin
object Singleton {
    lateinit var provider: Provider
}
```

**`const` 수정자:**

```kotlin
const val MAX = 239  // 정적 필드가 됨
```

## 정적 메서드

`@JvmStatic`으로 정적 메서드를 생성합니다:

```kotlin
companion object {
    @JvmStatic fun callStatic() {}
    fun callNonStatic() {}
}
```

Java 사용:

```java
C.callStatic();           // 작동 - 정적
C.callNonStatic();        // 오류 - 정적이 아님
C.Companion.callStatic(); // 인스턴스 메서드
```

명명된 객체의 경우:

```kotlin
object Obj {
    @JvmStatic fun callStatic() {}
}
```

Java 사용:

```java
Obj.callStatic();              // 작동
Obj.INSTANCE.callStatic();     // 역시 작동
```

**참고**: `@JvmStatic`은 인터페이스 컴패니언 객체에서 작동합니다 (Java 1.8+).

## 인터페이스의 기본 메서드

Kotlin 인터페이스 함수는 Java 기본 메서드(구체적인 메서드)로 컴파일됩니다:

```kotlin
interface Robot {
    fun move() { println("~walking~") }  // 기본 메서드
    fun speak(): Unit
}
```

Java 클래스는 암묵적으로 구현을 상속합니다:

```java
public class C3PO implements Robot {
    @Override
    public void speak() { System.out.println("I beg your pardon"); }
}

c3po.move();   // 기본 구현 호출
```

### 기본 메서드를 위한 호환성 모드

`-jvm-default` 컴파일러 옵션으로 제어:

| 모드 | 동작 |
|------|----------|
| `enable` (기본값) | 호환성 브릿지와 `DefaultImpls`를 포함한 기본 구현 생성 |
| `no-compatibility` | 기본 구현만; 호환성 브릿지 없음 |
| `disable` | 호환성 브릿지와 `DefaultImpls`만; 기본 구현 없음 |

## 가시성 매핑

| Kotlin | Java |
|--------|------|
| `private` | `private` |
| `protected` | `protected` |
| `internal` | `public` (이름 맹글링 포함) |
| `public` | `public` |

## KClass

`Class`를 `KClass`로 수동 변환:

```java
kotlin.jvm.JvmClassMappingKt.getKotlinClass(MainView.class)
```

## @JvmName으로 시그니처 충돌 처리

타입 삭제 충돌 해결:

```kotlin
fun List<String>.filterValid(): List<String>
@JvmName("filterValidInt")
fun List<Int>.filterValid(): List<Int>
```

Kotlin에서: 둘 다 `filterValid`로 호출
Java에서: `filterValid`와 `filterValidInt`

**프로퍼티 접근자:**

```kotlin
val x: Int
@JvmName("getX_prop")
get() = 15

fun getX() = 10
```

**커스텀 접근자 이름:**

```kotlin
@get:JvmName("x")
@set:JvmName("changeX")
var x: Int = 23
```

## 오버로드 생성

`@JvmOverloads`를 사용하여 기본 매개변수에서 여러 오버로드를 생성합니다:

```kotlin
class Circle @JvmOverloads constructor(
    centerX: Int,
    centerY: Int,
    radius: Double = 1.0
) {
    @JvmOverloads fun draw(label: String, lineWidth: Int = 1, color: String = "red") { }
}
```

Java에서 생성:

```java
Circle(int centerX, int centerY, double radius)
Circle(int centerX, int centerY)
void draw(String label, int lineWidth, String color) { }
void draw(String label, int lineWidth) { }
void draw(String label) { }
```

## 검사 예외

Kotlin에는 검사 예외가 없습니다. 예외를 선언하려면 `@Throws`를 사용합니다:

```kotlin
@Throws(IOException::class)
fun writeToFile() {
    throw IOException()
}
```

Java 코드에서 이제 catch할 수 있습니다:

```java
try {
    demo.Example.writeToFile();
} catch (IOException e) { }
```

## Null 안전성

Kotlin은 non-nullable 매개변수에 대한 런타임 검사를 생성합니다. `null`을 전달하는 Java 코드는 즉시 `NullPointerException`을 받습니다.

## Variant 제네릭

Kotlin의 선언 지점 변성은 Java에서 사용 지점 변성(와일드카드)으로 컴파일됩니다:

```kotlin
class Box<out T>(val value: T)

fun boxDerived(value: Derived): Box<Derived> = Box(value)
fun unboxBase(box: Box<Base>): Base = box.value
```

다음으로 컴파일됩니다:

```java
Box<Derived> boxDerived(Derived value) { ... }
Base unboxBase(Box<? extends Base> box) { ... }  // 매개변수에 와일드카드
```

**`@JvmWildcard`로 와일드카드 강제:**

```kotlin
fun boxDerived(value: Derived): Box<@JvmWildcard Derived> = Box(value)
// Box<? extends Derived> boxDerived(Derived value) { ... }
```

**`@JvmSuppressWildcards`로 와일드카드 억제:**

```kotlin
fun unboxBase(box: Box<@JvmSuppressWildcards Base>): Base = box.value
// Base unboxBase(Box<Base> box) { ... }
```

## Nothing 타입의 변환

`Nothing` 타입은 Java에 대응하는 것이 없습니다. Kotlin은 raw 타입을 생성합니다:

```kotlin
fun emptyList(): List<Nothing> = listOf()
// 변환: List emptyList() { ... }
```

## 인라인 값 클래스

기본적으로 인라인 값 클래스는 언박싱된 표현을 사용합니다 (Java에서 접근 불가). `@JvmExposeBoxed`를 사용하여 박싱된 버전을 노출합니다:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmInline
value class MyInt(val value: Int)
```

Java에서 이제 호출할 수 있습니다:

```java
MyInt input = new MyInt(5);
```

함수에 적용:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun MyInt.timesTwoBoxed(): MyInt = MyInt(this.value * 2)
```

또는 `-Xjvm-expose-boxed`로 컴파일하여 전역적으로 적용합니다.

### 상속된 함수

`@JvmExposeBoxed`는 상속된 함수의 박싱된 표현을 자동 생성하지 않습니다. 명시적으로 오버라이드하세요:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class DefaultTransformer : IdTransformer {
    override fun transformId(rawId: UInt): UInt = super.transformId(rawId)
}
```

---

**관련 문서**: [Kotlin에서 Java 호출하기](java-interop.html) | [Kotlin에서 Java 레코드 사용하기](jvm-records.html)

---

# Java와의 비교

> **원문:** https://kotlinlang.org/docs/comparison-to-java.html

이 페이지는 Kotlin과 Java를 종합적으로 비교하여 각각의 강점과 차이점을 강조합니다.

## Kotlin에서 해결된 Java의 일부 문제점

Kotlin은 여러 가지 일반적인 Java 문제를 해결합니다:

- **Null 참조** - [null 안전성](null-safety.html)을 통해 타입 시스템에서 제어됨
- **Raw 타입** - [Raw 타입 없음](java-interop.html#java-generics-in-kotlin)
- **배열 공변성** - [배열은 불변](arrays.html)
- **함수 타입** - SAM 변환 대신 적절한 [함수 타입](lambdas.html#function-types)
- **제네릭 변성** - 와일드카드 없이 [사용 지점 변성](generics.html#use-site-variance-type-projections)
- **검사 예외** - Kotlin에서는 필수가 아님
- **컬렉션** - [읽기 전용과 가변 컬렉션을 위한 별도 인터페이스](collections-overview.html)

## Java에는 있지만 Kotlin에는 없는 것

- **검사 예외** - Kotlin 설계의 일부가 아님
- **원시 타입** - 명시적으로 사용 불가 (바이트코드에서는 사용됨)
- **정적 멤버** - [컴패니언 객체](object-declarations.html#companion-objects), [최상위 함수](functions.html), [확장 함수](extensions.html#extension-functions), 또는 [@JvmStatic](java-to-kotlin-interop.html#static-methods)으로 대체
- **와일드카드 타입** - [선언 지점 변성](generics.html#declaration-site-variance) 및 [타입 프로젝션](generics.html#type-projections)으로 대체
- **삼항 연산자** - [if 표현식](control-flow.html#if-expression)으로 대체
- **레코드** - Java 16+ 기능으로 Kotlin에 없음
- **패키지-프라이빗 가시성** - 명시적 [가시성 수정자](visibility-modifiers.html)로 대체

*참고: Kotlin의 [스마트 캐스트](typecasts.html#smart-casts)는 Java의 [패턴 매칭](https://openjdk.org/projects/amber/design-notes/patterns/pattern-matching-for-java)과 유사한 기능을 제공합니다.*

## Kotlin에는 있지만 Java에는 없는 것

### 함수형 기능:

- [람다 표현식](lambdas.html) + [인라인 함수](inline-functions.html)
- [확장 함수](extensions.html)
- [최상위 함수](functions.html)
- [중위 함수](functions.html#infix-notation)

### 타입 시스템 및 안전성:

- [Null 안전성](null-safety.html)
- 변수 및 프로퍼티 타입에 대한 [타입 추론](types-overview.html)
- [선언 지점 변성 및 타입 프로젝션](generics.html)
- [스마트 캐스트](typecasts.html#smart-casts)

### 편의 기능:

- [문자열 템플릿](strings.html)
- [프로퍼티](properties.html)
- [주 생성자](classes.html)
- [데이터 클래스](data-classes.html)
- [기본 값이 있는 매개변수](functions.html#parameters-with-default-values)
- [명명된 매개변수](functions.html#named-arguments)
- [범위 표현식](ranges.html)

### 고급 기능:

- [코루틴](coroutines-overview.html)
- [일급 위임](delegation.html)
- [객체 선언을 통한 싱글톤](object-declarations.html)
- [컴패니언 객체](classes.html#companion-objects)
- [연산자 오버로딩](operator-overloading.html)
- [Expect 및 actual 선언](/docs/multiplatform/multiplatform-expect-actual.html)
- [라이브러리 작성자를 위한 명시적 API 모드](whatsnew14.html#explicit-api-mode-for-library-authors)

## 다음 단계

문서에서는 다음을 학습하도록 제안합니다:
- [Java와 Kotlin에서 문자열을 다루는 일반적인 작업](java-to-kotlin-idioms-strings.html)
- [Java와 Kotlin에서 컬렉션을 다루는 일반적인 작업](java-to-kotlin-collections-guide.html)
- [Java와 Kotlin에서 null 가능성 처리](java-to-kotlin-nullability-guide.html)

자세한 내용은 [JetBrains 공식 Kotlin 채널 동영상](https://www.youtube.com/watch?v=yJDoa42X-wQ)을 시청하세요.

---

# Java 프로젝트에 Kotlin 추가하기 - 튜토리얼

> **원문:** https://kotlinlang.org/docs/mixing-java-kotlin-intellij.html

이 튜토리얼은 두 언어 간의 완전한 상호운용성을 활용하면서 기존 Java 프로젝트에 Kotlin을 점진적으로 도입하는 방법을 보여줍니다.

## 학습 목표

- Java와 Kotlin 코드를 모두 컴파일하도록 Maven 또는 Gradle 설정
- 프로젝트 디렉토리에서 Java 및 Kotlin 소스 파일 구성
- IntelliJ IDEA를 사용하여 Java 파일을 Kotlin으로 변환

## 프로젝트 구성

### Maven 설정

**IntelliJ IDEA 2025.3**+는 Maven 프로젝트에 첫 번째 Kotlin 파일을 추가할 때 `pom.xml`을 자동으로 업데이트합니다.

수동으로 구성하려면:

1. **`<properties>`에 Kotlin 버전 프로퍼티 추가**:

```xml
<properties>
    <kotlin.version>2.3.10</kotlin.version>
</properties>
```

2. **`<dependencies>`에 Kotlin 의존성 추가**:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
</dependency>
```

3. **`<build><plugins>`에 Kotlin Maven 플러그인 구성**:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>default-compile</id>
            <phase>compile</phase>
            <configuration>
                <sourceDirs>
                    <sourceDir>src/main/kotlin</sourceDir>
                    <sourceDir>src/main/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
        <execution>
            <id>default-test-compile</id>
            <phase>test-compile</phase>
            <configuration>
                <sourceDirs>
                    <sourceDir>src/test/kotlin</sourceDir>
                    <sourceDir>src/test/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
    </executions>
</plugin>
```

4. IDE에서 Maven 프로젝트 다시 로드
5. 테스트 실행: `./mvnw clean test`

### Gradle 설정

1. **`plugins {}`에 Kotlin JVM 플러그인 추가**:

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
}
```

2. **JVM 툴체인 설정**:

```kotlin
kotlin {
    jvmToolchain(17)
}
```

3. **`dependencies {}`에 Kotlin 테스트 라이브러리 추가**:

```kotlin
dependencies {
    testImplementation(kotlin("test"))
}
```

4. Gradle 프로젝트 다시 로드
5. 테스트 실행: `./gradlew clean test`

## 프로젝트 구조

```
src/
├── main/
│   ├── java/       # Java 및 Kotlin 프로덕션 코드
│   └── kotlin/     # 추가 Kotlin 프로덕션 코드 (선택 사항)
└── test/
    ├── java/       # Java 및 Kotlin 테스트 코드
    └── kotlin/     # 추가 Kotlin 테스트 코드 (선택 사항)
```

**핵심 포인트:**
- 동일한 디렉토리에서 `.kt`와 `.java` 파일 혼합
- Kotlin 플러그인은 `src/main/java`와 `src/test/java`를 모두 자동으로 인식
- 디렉토리를 수동으로 생성하거나 첫 번째 Kotlin 파일을 추가할 때 IntelliJ IDEA가 생성하도록 함

## Java 파일을 Kotlin으로 변환

IntelliJ IDEA에는 Java에서 Kotlin으로 변환기 (J2K)가 포함되어 있습니다:

1. Java 파일을 마우스 오른쪽 버튼으로 클릭
2. 컨텍스트 메뉴에서 **Convert Java File to Kotlin File** 선택 (또는 Code 메뉴)

**참고:** 변환기는 대부분의 보일러플레이트 코드를 잘 처리하지만 복잡한 로직에는 수동 조정이 필요할 수 있습니다.

## 권장 다음 단계

프로덕션 코드를 즉시 변환하는 대신 먼저 Kotlin 테스트를 추가하는 것으로 시작하세요. 이를 통해 안정적인 코드베이스를 유지하면서 점진적으로 Kotlin을 도입할 수 있습니다.

## 주요 구성 이점

- **원활한 상호운용성**: Java와 Kotlin 코드가 장벽 없이 서로 참조
- **Gradle/Maven 통합**: 두 빌드 도구 모두 두 언어를 적절히 컴파일하고 연결
- **단계적 마이그레이션**: 자신의 속도에 맞춰 점진적으로 Java 파일을 Kotlin으로 변환
