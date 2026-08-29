# 코틀린 자바 상호운용, JS, Native, 멀티플랫폼

## 자바 상호운용

## Kotlin에서 Java 호출하기

> 원문: https://kotlinlang.org/docs/java-interop.html

Kotlin → 원활한 Java 상호운용성을 위해 설계됨. Java 코드는 Kotlin에서 자연스럽게 호출 가능 · 반대도 마찬가지.

### 기본 Java 사용

대부분의 Java 코드는 Kotlin에서 문제없이 작동:

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

### Getter와 Setter

명명 규칙을 따르는 Java getter/setter 메서드 → Kotlin 프로퍼티로 표현됨:

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

#### Java 합성 프로퍼티 참조 (Kotlin 1.8.20+)

Java 합성 프로퍼티 참조 생성 가능:

```kotlin
val persons = listOf(Person("Jack", 11), Person("Sofie", 12), Person("Peter", 11))
persons
    .sortedBy(Person::age)  // 합성 프로퍼티에 대한 참조
    .forEach { person -> println(person.name) }
```

Gradle에서 활성화:

```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>()
    .configureEach {
        compilerOptions.languageVersion.set(
            org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
        )
    }
```

### void를 반환하는 메서드

Java `void` 메서드는 Kotlin에서 `Unit` 반환:

```kotlin
// Java: void method()
// Kotlin: 반환 값이 호출 지점에서 Unit으로 할당됨
```

### Kotlin 키워드 이스케이프

백틱 사용 → Kotlin 키워드와 동일한 이름의 메서드 호출:

```kotlin
foo.`is`(bar)
```

### Null 안전성과 플랫폼 타입

모든 Java 참조는 `null`일 수 있음 → Kotlin의 null 안전성과 불일치 발생. Java 타입은 완화된 null 검사를 가진 플랫폼 타입으로 처리됨.

#### 플랫폼 타입 표기법

- `T!`: "T 또는 T?" 의미
- `(Mutable)Collection<T>!`: "T의 Java 컬렉션(가변성·null 가능성 알 수 없음)" 의미
- `Array<(out) T>!`: "T 또는 하위 타입의 Java 배열, null 가능성 알 수 없음" 의미

```kotlin
val list = ArrayList<String>()  // non-null (생성자 결과)
val size = list.size             // non-null (원시 int)
val item = list[0]               // 플랫폼 타입 (추론됨)

item.substring(1)  // 허용됨, item == null이면 예외 발생
```

#### 플랫폼 타입의 타입 안전성

플랫폼 값 할당 시 예상 타입 선택 가능:

```kotlin
val nullable: String? = item   // 허용됨, 항상 작동
val notNull: String = item     // 허용됨, 런타임에 실패할 수 있음 (assertion 생성됨)
```

### Null 가능성 어노테이션

null 가능성 어노테이션이 있는 Java 타입 → nullable/non-nullable Kotlin 타입으로 처리됨:

지원되는 어노테이션:
- JetBrains: `@Nullable`, `@NotNull`
- JSpecify: `org.jspecify.annotations`
- Android: `com.android.annotations`, `android.support.annotations`
- JSR-305: `javax.annotation`
- FindBugs: `edu.umd.cs.findbugs.annotations`
- Eclipse: `org.eclipse.jdt.annotation`
- Lombok: `lombok.NonNull`
- RxJava 3: `io.reactivex.rxjava3.annotations`

컴파일러 옵션으로 구성:

```
-Xnullability-annotations=@<package-name>:<report-level>
```

보고 수준: `ignore`·`warn`·`strict`

#### JSpecify 지원 (권장)

JSpecify → 통합된 null 가능성 어노테이션 제공:

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

보고 수준 구성:

```
-Xjspecify-annotations=<report-level>
```

수준: `strict`(기본값)·`warn`·`ignore`

### 매핑된 타입

Java 타입 → Kotlin 동등 타입으로 매핑:

#### 원시 타입

- `byte` → `kotlin.Byte`
- `int` → `kotlin.Int`
- `long` → `kotlin.Long`
- `boolean` → `kotlin.Boolean`
- 기타: 동일 패턴

#### 내장 타입

- `java.lang.Object` → `kotlin.Any!`
- `java.lang.String` → `kotlin.String!`
- `java.lang.Throwable` → `kotlin.Throwable!`

#### 박싱 타입

- `java.lang.Integer` → `kotlin.Int?`
- `java.lang.Boolean` → `kotlin.Boolean?`
- 기타 박싱된 원시 타입 → nullable Kotlin 동등 타입

#### 컬렉션

- `Collection<T>`
  - 읽기 전용: `Collection<T>`
  - 가변: `MutableCollection<T>`
  - 플랫폼: `(Mutable)Collection<T>!`
- `List<T>`
  - 읽기 전용: `List<T>`
  - 가변: `MutableList<T>`
  - 플랫폼: `(Mutable)List<T>!`
- `Set<T>`
  - 읽기 전용: `Set<T>`
  - 가변: `MutableSet<T>`
  - 플랫폼: `(Mutable)Set<T>!`
- `Map<K,V>`
  - 읽기 전용: `Map<K,V>`
  - 가변: `MutableMap<K,V>`
  - 플랫폼: `(Mutable)Map<K,V>!`

### Kotlin에서의 Java 제네릭

Java 와일드카드 → 타입 프로젝션으로 변환:

```kotlin
// Java: Foo<? extends Bar> → Kotlin: Foo<out Bar!>!
// Java: Foo<? super Bar> → Kotlin: Foo<in Bar!>!
// Java: List (raw 타입) → Kotlin: List<*>! (List<out Any?>!)
```

제네릭을 사용한 타입 검사:

```kotlin
if (a is List<Int>)   // 오류: 런타임에 Int의 List인지 확인할 수 없음
if (a is List<*>)     // OK: 스타 프로젝트된 제네릭 타입 허용
```

### Java 배열

Kotlin에서 배열은 불변(Java와 다름) → 특화된 원시 배열 클래스 사용:

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

특화된 배열 타입: `IntArray`·`DoubleArray`·`CharArray`·`LongArray` 등

### Java Varargs

스프레드 연산자 `*` 사용 → vararg 매개변수에 배열 전달:

```kotlin
val array = intArrayOf(0, 1, 2, 3)
javaObj.removeIndicesVarArg(*array)
```

### 연산자

올바른 이름과 시그니처를 가진 Java 메서드는 모두 연산자로 사용 가능:

```kotlin
// 적절한 시그니처를 가진 Java 메서드
a + b  // Java 메서드에 대한 연산자 호출
a[i]   // Java 메서드에 대한 인덱싱 연산자
```

참고: Java 메서드에는 중위 호출 구문 허용 안 됨

### 검사 예외

Kotlin에는 검사 예외 없음 → Java 예외를 catch할 필요 없음:

```kotlin
fun render(list: List<*>, to: Appendable) {
    for (item in list) {
        to.append(item.toString())  // IOException을 catch할 필요 없음
    }
}
```

### Object 메서드

Java의 `java.lang.Object` 메서드 → 확장 함수를 통해 접근:

#### `wait()` 및 `notify()`

`Any`에서 사용 불가 → `java.util.concurrent` 사용 또는 `java.lang.Object`를 통해 접근:

```kotlin
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private val lock = Object()

synchronized(lock) {
    lock.wait()
    lock.notifyAll()
}
```

#### `getClass()`

`java` 확장 프로퍼티 사용:

```kotlin
val fooClass = foo::class.java     // 바운드 클래스 참조
val fooClass = foo.javaClass        // 확장 프로퍼티
```

#### `clone()`

`kotlin.Cloneable` 확장:

```kotlin
class Example : Cloneable {
    override fun clone(): Any { }
}
```

#### `finalize()`

`override` 키워드 없이 선언:

```kotlin
class C {
    protected fun finalize() {
        // 종료 로직
    }
}
```

### Java 클래스 상속

- 최대 하나의 Java 클래스만 상위 타입 가능
- 여러 Java 인터페이스가 상위 타입 가능

### 정적 멤버 접근

전체 한정 이름 사용 → 접근:

```kotlin
if (Character.isLetter(a)) { }
java.lang.Integer.bitCount(foo)
```

매핑된 타입은 전체 Java 이름 사용:

```kotlin
java.lang.Integer.toHexString(foo)
```

### Java 리플렉션

```kotlin
val fooClass = foo::class.java              // 인스턴스에서 클래스로
val fooClass = ClassName::class.java        // 클래스 참조
val fooClass = foo.javaClass                // 확장 프로퍼티

// 원시 타입의 경우:
Int::class.java              // 원시 int
Int::class.javaObjectType    // 래퍼 Integer
```

### SAM 변환

Kotlin 함수 리터럴 → 단일 메서드 Java 인터페이스로 자동 변환:

```kotlin
val runnable = Runnable { println("This runs in a runnable") }

val executor = ThreadPoolExecutor()
executor.execute { println("This runs in a thread pool") }

// 여러 SAM 메서드가 있는 경우 타입을 지정:
executor.execute(Runnable { println("This runs in a thread pool") })
```

### JNI (Java Native Interface)

네이티브 함수는 `external`로 표시:

```kotlin
external fun foo(x: Int): Double

// 프로퍼티의 경우:
var myProperty: String
    external get
    external set
```

### Kotlin에서 Lombok 사용

Lombok이 생성한 선언은 Kotlin에서 사용 가능. Java/Kotlin 혼합 모듈은 [Lombok 컴파일러 플러그인](https://kotlinlang.org/docs/lombok.html) 사용.

---

핵심 요점: 플랫폼 타입·합성 프로퍼티·null 가능성 어노테이션·자동 타입 매핑 → Java 상호운용성 제공 → Java·Kotlin 코드 자연스럽게 혼합 가능

---

## Java에서 Kotlin 호출하기

> 원문: https://kotlinlang.org/docs/java-to-kotlin-interop.html

Java에서 Kotlin 코드를 원활하게 호출하는 방법 · 상호운용성 기능과 모범 사례 정리.

### 프로퍼티

Kotlin 프로퍼티는 Java 요소로 컴파일됨:
- Getter 메서드(`get` 접두사)
- Setter 메서드(`var` 프로퍼티는 `set` 접두사)
- Private 필드(프로퍼티와 동일한 이름)

예시:

```kotlin
var firstName: String
```

다음으로 컴파일됨:

```java
private String firstName;
public String getFirstName() { return firstName; }
public void setFirstName(String firstName) { this.firstName = firstName; }
```

특수 명명 규칙: `is`로 시작하는 프로퍼티는 다른 매핑 사용. `isOpen`의 경우 getter는 `isOpen()`, setter는 `setOpen()`.

### 패키지 레벨 함수

파일 `app.kt`(패키지 `org.example`)의 함수와 프로퍼티 → `org.example.AppKt`의 정적 메서드로 컴파일됨.

`@JvmName`으로 클래스 이름 커스터마이즈:

```kotlin
@file:JvmName("DemoUtils")
package org.example

fun getTime() { /*...*/ }
```

Java에서 다음과 같이 호출됨: `org.example.DemoUtils.getTime()`

여러 파일을 단일 facade 클래스로 병합하려면 `@JvmMultifileClass` 사용.

### 인스턴스 필드

`@JvmField` 사용 → 프로퍼티를 Java 필드로 노출:

```kotlin
class User(id: String) {
    @JvmField val ID = id
}
```

Java에서 다음과 같이 사용됨:

```java
return user.ID;  // 직접 필드 접근
```

요구 사항:
- 백킹 필드 있어야 함
- private이 아니어야 함
- `open`·`override`·`const` 수정자 없음
- 위임 프로퍼티가 아님

### 정적 필드

컴패니언 객체 또는 명명된 객체의 프로퍼티 → 정적 필드가 됨. 노출 방법:

`@JvmField` 어노테이션:

```kotlin
companion object {
    @JvmField val COMPARATOR: Comparator<Key> = compareBy<Key> { it.value }
}
```

Java에서 접근: `Key.COMPARATOR`

`lateinit` 수정자:

```kotlin
object Singleton {
    lateinit var provider: Provider
}
```

`const` 수정자:

```kotlin
const val MAX = 239  // 정적 필드가 됨
```

### 정적 메서드

`@JvmStatic`으로 정적 메서드 생성:

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

참고: `@JvmStatic`은 인터페이스 컴패니언 객체에서도 작동(Java 1.8+).

### 인터페이스의 기본 메서드

Kotlin 인터페이스 함수 → Java 기본 메서드(구체적인 메서드)로 컴파일됨:

```kotlin
interface Robot {
    fun move() { println("~walking~") }  // 기본 메서드
    fun speak(): Unit
}
```

Java 클래스는 암묵적으로 구현을 상속함:

```java
public class C3PO implements Robot {
    @Override
    public void speak() { System.out.println("I beg your pardon"); }
}

c3po.move();   // 기본 구현 호출
```

#### 기본 메서드를 위한 호환성 모드

`-jvm-default` 컴파일러 옵션으로 제어:

- `enable`(기본값): 호환성 브릿지와 `DefaultImpls`를 포함한 기본 구현 생성
- `no-compatibility`: 기본 구현만, 호환성 브릿지 없음
- `disable`: 호환성 브릿지와 `DefaultImpls`만, 기본 구현 없음

### 가시성 매핑

- `private` → `private`
- `protected` → `protected`
- `internal` → `public`(이름 맹글링 포함)
- `public` → `public`

### KClass

`Class`를 `KClass`로 수동 변환:

```java
kotlin.jvm.JvmClassMappingKt.getKotlinClass(MainView.class)
```

### @JvmName으로 시그니처 충돌 처리

타입 삭제(erasure) 충돌 해결:

```kotlin
fun List<String>.filterValid(): List<String>
@JvmName("filterValidInt")
fun List<Int>.filterValid(): List<Int>
```

Kotlin에서: 둘 다 `filterValid`로 호출
Java에서: `filterValid`와 `filterValidInt`

프로퍼티 접근자:

```kotlin
val x: Int
@JvmName("getX_prop")
get() = 15

fun getX() = 10
```

커스텀 접근자 이름:

```kotlin
@get:JvmName("x")
@set:JvmName("changeX")
var x: Int = 23
```

### 오버로드 생성

`@JvmOverloads` 사용 → 기본 매개변수에서 여러 오버로드 생성:

```kotlin
class Circle @JvmOverloads constructor(
    centerX: Int,
    centerY: Int,
    radius: Double = 1.0
) {
    @JvmOverloads fun draw(label: String, lineWidth: Int = 1, color: String = "red") { }
}
```

Java에서 생성됨:

```java
Circle(int centerX, int centerY, double radius)
Circle(int centerX, int centerY)
void draw(String label, int lineWidth, String color) { }
void draw(String label, int lineWidth) { }
void draw(String label) { }
```

### 검사 예외

Kotlin에는 검사 예외 없음 → 예외를 선언하려면 `@Throws` 사용:

```kotlin
@Throws(IOException::class)
fun writeToFile() {
    throw IOException()
}
```

Java 코드에서 이제 catch 가능:

```java
try {
    demo.Example.writeToFile();
} catch (IOException e) { }
```

### Null 안전성

Kotlin은 non-nullable 매개변수에 대한 런타임 검사 생성 → `null`을 전달하는 Java 코드는 즉시 `NullPointerException` 발생.

### Variant 제네릭

Kotlin의 선언 지점 변성 → Java에서 사용 지점 변성(와일드카드)으로 컴파일됨:

```kotlin
class Box<out T>(val value: T)

fun boxDerived(value: Derived): Box<Derived> = Box(value)
fun unboxBase(box: Box<Base>): Base = box.value
```

다음으로 컴파일됨:

```java
Box<Derived> boxDerived(Derived value) { ... }
Base unboxBase(Box<? extends Base> box) { ... }  // 매개변수에 와일드카드
```

`@JvmWildcard`로 와일드카드 강제:

```kotlin
fun boxDerived(value: Derived): Box<@JvmWildcard Derived> = Box(value)
// Box<? extends Derived> boxDerived(Derived value) { ... }
```

`@JvmSuppressWildcards`로 와일드카드 억제:

```kotlin
fun unboxBase(box: Box<@JvmSuppressWildcards Base>): Base = box.value
// Base unboxBase(Box<Base> box) { ... }
```

### Nothing 타입의 변환

`Nothing` 타입은 Java에 대응하는 것이 없음 → Kotlin은 raw 타입 생성:

```kotlin
fun emptyList(): List<Nothing> = listOf()
// 변환: List emptyList() { ... }
```

### 인라인 값 클래스

기본적으로 인라인 값 클래스는 언박싱된 표현 사용(Java에서 접근 불가) → `@JvmExposeBoxed`로 박싱된 버전 노출:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmInline
value class MyInt(val value: Int)
```

Java에서 이제 호출 가능:

```java
MyInt input = new MyInt(5);
```

함수에 적용:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun MyInt.timesTwoBoxed(): MyInt = MyInt(this.value * 2)
```

또는 `-Xjvm-expose-boxed`로 컴파일 → 전역적으로 적용.

#### 상속된 함수

`@JvmExposeBoxed`는 상속된 함수의 박싱된 표현을 자동 생성하지 않음 → 명시적으로 오버라이드 필요:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class DefaultTransformer : IdTransformer {
    override fun transformId(rawId: UInt): UInt = super.transformId(rawId)
}
```

---

관련 문서: [Kotlin에서 Java 호출하기](https://kotlinlang.org/docs/java-interop.html) | [Kotlin에서 Java 레코드 사용하기](https://kotlinlang.org/docs/jvm-records.html)

---

## Java와의 비교

> 원문: https://kotlinlang.org/docs/comparison-to-java.html

Kotlin과 Java를 종합적으로 비교 → 각각의 강점과 차이점 정리.

### Kotlin에서 해결된 Java의 일부 문제점

- Null 참조: [null 안전성](https://kotlinlang.org/docs/null-safety.html)을 통해 타입 시스템에서 제어됨
- Raw 타입: [Raw 타입 없음](https://kotlinlang.org/docs/java-interop.html#java-generics-in-kotlin)
- 배열 공변성: [배열은 불변](https://kotlinlang.org/docs/arrays.html)
- 함수 타입: SAM 변환 대신 적절한 [함수 타입](https://kotlinlang.org/docs/lambdas.html#function-types)
- 제네릭 변성: 와일드카드 없이 [사용 지점 변성](https://kotlinlang.org/docs/generics.html#use-site-variance-type-projections)
- 검사 예외: Kotlin에서는 필수가 아님
- 컬렉션: [읽기 전용과 가변 컬렉션을 위한 별도 인터페이스](https://kotlinlang.org/docs/collections-overview.html)

### Java에는 있지만 Kotlin에는 없는 것

- 검사 예외: Kotlin 설계의 일부가 아님
- 원시 타입: 명시적으로 사용 불가(바이트코드에서는 사용됨)
- 정적 멤버: [컴패니언 객체](https://kotlinlang.org/docs/object-declarations.html#companion-objects)·[최상위 함수](https://kotlinlang.org/docs/functions.html)·[확장 함수](https://kotlinlang.org/docs/extensions.html#extension-functions)·[@JvmStatic](https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-methods)으로 대체
- 와일드카드 타입: [선언 지점 변성](https://kotlinlang.org/docs/generics.html#declaration-site-variance) 및 [타입 프로젝션](https://kotlinlang.org/docs/generics.html#type-projections)으로 대체
- 삼항 연산자: [if 표현식](https://kotlinlang.org/docs/control-flow.html#if-expression)으로 대체
- 레코드: Java 16+ 기능으로 Kotlin에 없음
- 패키지-프라이빗 가시성: 명시적 [가시성 수정자](https://kotlinlang.org/docs/visibility-modifiers.html)로 대체

참고: Kotlin의 [스마트 캐스트](https://kotlinlang.org/docs/typecasts.html#smart-casts)는 Java의 [패턴 매칭](https://openjdk.org/projects/amber/design-notes/patterns/pattern-matching-for-java)과 유사한 기능 제공.

### Kotlin에는 있지만 Java에는 없는 것

#### 함수형 기능:

- [람다 표현식](https://kotlinlang.org/docs/lambdas.html) + [인라인 함수](https://kotlinlang.org/docs/inline-functions.html)
- [확장 함수](https://kotlinlang.org/docs/extensions.html)
- [최상위 함수](https://kotlinlang.org/docs/functions.html)
- [중위 함수](https://kotlinlang.org/docs/functions.html#infix-notation)

#### 타입 시스템 및 안전성:

- [Null 안전성](https://kotlinlang.org/docs/null-safety.html)
- 변수 및 프로퍼티 타입에 대한 [타입 추론](https://kotlinlang.org/docs/types-overview.html)
- [선언 지점 변성 및 타입 프로젝션](https://kotlinlang.org/docs/generics.html)
- [스마트 캐스트](https://kotlinlang.org/docs/typecasts.html#smart-casts)

#### 편의 기능:

- [문자열 템플릿](https://kotlinlang.org/docs/strings.html)
- [프로퍼티](https://kotlinlang.org/docs/properties.html)
- [주 생성자](https://kotlinlang.org/docs/classes.html)
- [데이터 클래스](https://kotlinlang.org/docs/data-classes.html)
- [기본 값이 있는 매개변수](https://kotlinlang.org/docs/functions.html#parameters-with-default-values)
- [명명된 매개변수](https://kotlinlang.org/docs/functions.html#named-arguments)
- [범위 표현식](https://kotlinlang.org/docs/ranges.html)

#### 고급 기능:

- [코루틴](https://kotlinlang.org/docs/coroutines-overview.html)
- [일급 위임](https://kotlinlang.org/docs/delegation.html)
- [객체 선언을 통한 싱글톤](https://kotlinlang.org/docs/object-declarations.html)
- [컴패니언 객체](https://kotlinlang.org/docs/classes.html#companion-objects)
- [연산자 오버로딩](https://kotlinlang.org/docs/operator-overloading.html)
- [Expect 및 actual 선언](/docs/multiplatform/multiplatform-expect-actual.html)
- [라이브러리 작성자를 위한 명시적 API 모드](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors)

### 다음 단계

- [Java와 Kotlin에서 문자열을 다루는 일반적인 작업](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html)
- [Java와 Kotlin에서 컬렉션을 다루는 일반적인 작업](https://kotlinlang.org/docs/java-to-kotlin-collections-guide.html)
- [Java와 Kotlin에서 null 가능성 처리](https://kotlinlang.org/docs/java-to-kotlin-nullability-guide.html)

자세한 내용은 [JetBrains 공식 Kotlin 채널 동영상](https://www.youtube.com/watch?v=yJDoa42X-wQ) 참고.

---

## Java 프로젝트에 Kotlin 추가하기 - 튜토리얼

> 원문: https://kotlinlang.org/docs/mixing-java-kotlin-intellij.html

두 언어 간의 완전한 상호운용성을 활용하면서 기존 Java 프로젝트에 Kotlin을 점진적으로 도입하는 방법.

### 학습 목표

- Java와 Kotlin 코드를 모두 컴파일하도록 Maven 또는 Gradle 설정
- 프로젝트 디렉토리에서 Java 및 Kotlin 소스 파일 구성
- IntelliJ IDEA로 Java 파일을 Kotlin으로 변환

### 프로젝트 구성

#### Maven 설정

IntelliJ IDEA 2025.3+는 Maven 프로젝트에 첫 번째 Kotlin 파일을 추가할 때 `pom.xml`을 자동으로 업데이트함.

수동으로 구성하는 경우:

1. `<properties>`에 Kotlin 버전 프로퍼티 추가:

```xml
<properties>
    <kotlin.version>2.3.10</kotlin.version>
</properties>
```

2. `<dependencies>`에 Kotlin 의존성 추가:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
</dependency>
```

3. `<build><plugins>`에 Kotlin Maven 플러그인 구성:

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

#### Gradle 설정

1. `plugins {}`에 Kotlin JVM 플러그인 추가:

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
}
```

2. JVM 툴체인 설정:

```kotlin
kotlin {
    jvmToolchain(17)
}
```

3. `dependencies {}`에 Kotlin 테스트 라이브러리 추가:

```kotlin
dependencies {
    testImplementation(kotlin("test"))
}
```

4. Gradle 프로젝트 다시 로드
5. 테스트 실행: `./gradlew clean test`

### 프로젝트 구조

```
src/
├── main/
│   ├── java/       # Java 및 Kotlin 프로덕션 코드
│   └── kotlin/     # 추가 Kotlin 프로덕션 코드 (선택 사항)
└── test/
    ├── java/       # Java 및 Kotlin 테스트 코드
    └── kotlin/     # 추가 Kotlin 테스트 코드 (선택 사항)
```

핵심 포인트:
- 동일한 디렉토리에서 `.kt`와 `.java` 파일 혼합 가능
- Kotlin 플러그인은 `src/main/java`와 `src/test/java`를 모두 자동으로 인식
- 디렉토리는 수동 생성 또는 첫 Kotlin 파일 추가 시 IntelliJ IDEA가 자동 생성

### Java 파일을 Kotlin으로 변환

IntelliJ IDEA에는 Java에서 Kotlin으로 변환기(J2K) 포함:

1. Java 파일 우클릭
2. 컨텍스트 메뉴에서 Convert Java File to Kotlin File 선택(또는 Code 메뉴)

참고: 변환기는 대부분의 보일러플레이트 코드를 잘 처리 → 복잡한 로직은 수동 조정 필요할 수 있음.

### 권장 다음 단계

프로덕션 코드를 즉시 변환하는 대신 먼저 Kotlin 테스트 추가로 시작 → 안정적인 코드베이스 유지하며 점진적으로 Kotlin 도입 가능.

### 주요 구성 이점

- 원활한 상호운용성: Java와 Kotlin 코드가 장벽 없이 서로 참조
- Gradle/Maven 통합: 두 빌드 도구 모두 두 언어를 적절히 컴파일·연결
- 단계적 마이그레이션: 자신의 속도에 맞춰 점진적으로 Java 파일을 Kotlin으로 변환

---

## 코틀린/JS

## Kotlin/JS 시작하기 - 완전 가이드

> 원문: https://kotlinlang.org/docs/js-get-started.html

### 개요

Kotlin/JavaScript(Kotlin/JS)로 브라우저용 웹 애플리케이션 만드는 방법. 두 가지 접근 방식 가능:

1. IntelliJ IDEA: 버전 관리에서 프로젝트 템플릿 복제
2. Gradle: 더 나은 이해를 위해 빌드 파일을 수동으로 생성

### 방법 1: IntelliJ IDEA에서 애플리케이션 생성

#### 환경 설정

1. 최신 버전의 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드 및 설치
2. Kotlin Multiplatform 개발을 위한 환경 설정

#### 프로젝트 생성

1. File | New | Project from Version Control 선택
2. URL 입력: `https://github.com/Kotlin/kmp-js-wizard`
3. Clone 클릭

#### 프로젝트 구성

1. `kmp-js-wizard/gradle/libs.versions.toml` 열기
2. Kotlin 버전이 Kotlin Multiplatform Gradle 플러그인 버전과 일치하는지 확인:

```
[versions]
kotlin = "2.3.10"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

3. Load Gradle Changes 버튼으로 Gradle 파일 동기화

#### 애플리케이션 빌드 및 실행

1. `src/jsMain/kotlin/Main.kt` 열기
2. `main()` 함수에서 Run 아이콘 클릭
3. 웹 애플리케이션이 자동으로 `http://localhost:8080/`에서 열림

애플리케이션은 [`kotlinx.browser`](https://github.com/Kotlin/kotlinx-browser) API로 페이지에 "Hello, Kotlin/JS!" 표시.

#### 연속 빌드 활성화

1. 실행 구성에서 `jsMain [js]` 선택
2. More Actions | Edit 클릭
3. Run 필드에 입력: `jsBrowserDevelopmentRun --continuous`
4. OK 클릭

이후 변경 사항 저장 시 Gradle이 자동으로 다시 빌드하고 브라우저를 핫 리로드함.

### 애플리케이션 수정 - 글자 수 세기 예제

#### 입력 요소 추가

```kotlin
fun Element.appendInput() {
    val input = document.createElement("input")
    appendChild(input)
}

fun main() {
    document.body?.appendInput()
}
```

#### 입력 이벤트 처리 추가

```kotlin
fun Element.appendInput(onChange: (String) -> Unit = {}) {
    val input = document.createElement("input").apply {
        addEventListener("change") { event ->
            onChange(event.target.unsafeCast<HTMLInputElement>().value)
        }
    }
    appendChild(input)
}

fun main() {
    document.body?.appendInput(onChange = { println(it) })
}
```

#### 출력 요소 추가

```kotlin
fun Element.appendTextContainer(): Element {
    return document.createElement("p").also(::appendChild)
}

fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { println(it) })
}
```

#### 입력 처리하여 글자 수 세기

```kotlin
fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { name ->
        name.replace(" ", "").let {
            output?.textContent = "Your name contains ${it.length} letters"
        }
    })
}
```

#### 고유 글자 수 세기 (고급)

```kotlin
fun String.countDistinctCharacters() =
    lowercase().toList().distinct().count()

fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { name ->
        name.replace(" ", "").let {
            output?.textContent = "Your name contains ${it.countDistinctCharacters()} unique letters"
        }
    })
}
```

사용된 주요 함수:
- `lowercase()`: 소문자로 변환
- `toList()`: 문자열을 문자 리스트로 변환
- `distinct()`: 고유 문자 필터링
- `count()`: 요소 개수 세기
- `replace()`: 공백 제거
- `let{}`: 스코프 함수
- `${}` 사용한 문자열 템플릿

### 방법 2: Gradle을 사용하여 애플리케이션 생성

#### 프로젝트 파일 생성

1. Gradle 호환성 확인: [호환성 표](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin) 참고

2. `build.gradle.kts` 생성:

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser()  // browser() 또는 nodejs() 사용
        binaries.executable()
    }
}
```

또는 Groovy(`build.gradle`):

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '2.3.10'
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
}
```

3. 빈 `settings.gradle.kts` 생성

4. 디렉토리 구조 생성:

```
src/jsMain/kotlin/
```

5. `src/jsMain/kotlin/hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/JS!")
}
```

#### 브라우저 환경 전용

1. `src/jsMain/resources` 디렉토리 생성

2. `src/jsMain/resources/index.html` 생성:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Application title</title>
</head>
<body>
    <script src="$NAME_OF_YOUR_PROJECT_DIRECTORY.js"></script>
</body>
</html>
```

`$NAME_OF_YOUR_PROJECT_DIRECTORY`는 실제 프로젝트 디렉토리 이름으로 교체.

#### 프로젝트 빌드 및 실행

```bash
# 브라우저용
gradle jsBrowserDevelopmentRun

# Node.js용
gradle jsNodeDevelopmentRun
```

브라우저 출력: `index.html`을 열고 브라우저 콘솔에서 확인(Ctrl+Shift+J / Cmd+Option+J)

Node.js 출력: 터미널에 출력

#### IntelliJ IDEA에서 프로젝트 열기

1. File | Open 선택
2. 프로젝트 디렉토리 찾기
3. Open 클릭

IDEA가 Kotlin/JS 프로젝트를 자동으로 감지 → Build 창에 오류 표시.

### 다음 단계

- [Kotlin/JS 프로젝트 설정](https://kotlinlang.org/docs/js-project-setup.html)
- [Kotlin/JS 애플리케이션 디버깅](https://kotlinlang.org/docs/js-debugging.html)
- [테스트 작성 및 실행](https://kotlinlang.org/docs/js-running-tests.html)
- [실제 프로젝트를 위한 Gradle 빌드 스크립트](/docs/multiplatform/multiplatform-dsl-reference.html)
- [Gradle 빌드 시스템에 대해 알아보기](https://kotlinlang.org/docs/gradle.html)

---

## Kotlin/JavaScript 개요

> 원문: https://kotlinlang.org/docs/js-overview.html

### Kotlin/JS란?

Kotlin/JavaScript(Kotlin/JS) → Kotlin 코드·표준 라이브러리·호환 가능한 의존성을 JavaScript로 트랜스파일 → JavaScript를 지원하는 모든 환경에서 Kotlin 애플리케이션 실행 가능.

주요 기능:
- Kotlin Multiplatform Gradle 플러그인(`kotlin.multiplatform`)을 통해 구성 및 관리
- ES5 및 ES2015 JavaScript 표준 지원
- npm 의존성 관리 및 애플리케이션 번들링 지원
- 일반적인 모듈 시스템과 호환: ESM·CommonJS·UMD·AMD

---

### Kotlin/JS 사용 사례

#### 1. 프론트엔드와 JVM 백엔드 간 공통 로직 공유

Kotlin/JVM 백엔드와 웹 애플리케이션 간에 DTO·유효성 검사 규칙·인증 로직·REST API 추상화 공유.

#### 2. Android, iOS, 웹 간 로직 공유

Kotlin으로 비즈니스 로직을 한 번 작성 → 각 플랫폼에서 네이티브 UI 사용하며 코드 중복 회피.

#### 3. 프론트엔드 웹 애플리케이션 구축

여러 접근 방식 가능:

- Compose 기반 프레임워크: Android 개발에 익숙하면 [Kobweb](https://kobweb.varabyte.com/) 또는 [Kilua](https://kilua.dev/) 같은 프레임워크 사용
- React와 Kotlin: React Redux·React Router·styled-components를 지원하는 [JavaScript 라이브러리용 Kotlin 래퍼](https://github.com/JetBrains/kotlin-wrappers)로 타입 안전한 React 애플리케이션 구축
- Kotlin/JS 프레임워크: Kotlin 생태계와 통합되는 전용 프레임워크 사용

#### 4. Compose Multiplatform으로 구형 브라우저 지원

모바일/데스크톱 UI를 재사용하면서 레거시 브라우저 지원 확장.

#### 5. 서버 사이드 및 서버리스 애플리케이션

빠른 시작·낮은 메모리 사용량으로 타입 안전한 서버 사이드 개발 → [`kotlinx-nodejs`](https://github.com/Kotlin/kotlinx-nodejs) 라이브러리와 함께 Node.js 타겟 사용.

---

### 시작하기

#### 사전 준비
- [Kotlin 기본 문법](https://kotlinlang.org/docs/basic-syntax.html)에 익숙해지기
- [Kotlin 투어](https://kotlinlang.org/docs/kotlin-tour-welcome.html) 탐색

#### 다음 단계
1. 설정: [Kotlin/JS 프로젝트 설정 가이드](https://kotlinlang.org/docs/js-project-setup.html) 검토
2. 샘플 프로젝트: 패턴과 모범 사례를 위해 제공된 예제 학습
3. 탐색: 디버깅 및 테스트 같은 고급 주제로 이동

---

### 샘플 프로젝트

- [Spring & Angular를 사용한 Petclinic](https://github.com/Kotlin/kmp-spring-petclinic/#readme): DTO와 유효성 검사를 공유하는 Spring Boot 백엔드 + Angular 프론트엔드
- [풀스택 컨퍼런스 CMS](https://github.com/Kotlin/kmp-fullstack-conference-cms/#readme): Ktor·Jetpack Compose·Vue.js 코드 공유 접근 방식
- [Kobweb의 Todo 앱](https://github.com/varabyte/kobweb-templates/tree/main/examples/todo/#readme): Kobweb를 사용한 Android 익숙한 웹 UI 접근 방식
- [로직 공유: Android/iOS/Web](https://github.com/Kotlin/kmp-logic-sharing-simple-example/#readme): 각 플랫폼의 네이티브 UI와 공통 로직
- [풀스택 Todo 리스트](https://github.com/kotlin-hands-on/jvm-js-fullstack/#readme): Ktor 백엔드 + React 프론트엔드 협업

---

### Kotlin/JS 프레임워크

내장 컴포넌트·라우팅·상태 관리로 웹 개발을 단순화하는 다양한 저자들이 작성한 [사용 가능한 프레임워크](https://kotlinlang.org/docs/js-frameworks.html) 참고.

---

### 커뮤니티 및 지원

커뮤니티 및 Kotlin/JS 팀과 연결하려면 공식 [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)의 [#javascript 채널](https://kotlinlang.slack.com/archives/C0B8L3U69) 참여.

---

### 다음 단계

- [Kotlin/JS 프로젝트 설정](https://kotlinlang.org/docs/js-project-setup.html)
- [Kotlin/JS 프로젝트 실행](https://kotlinlang.org/docs/running-kotlin-js.html)
- [Kotlin/JS 코드 디버깅](https://kotlinlang.org/docs/js-debugging.html)
- [Kotlin/JS에서 테스트 실행](https://kotlinlang.org/docs/js-running-tests.html)

---

## Kotlin/JS 프로젝트 설정

> 원문: https://kotlinlang.org/docs/js-project-setup.html

Gradle과 Kotlin Multiplatform 플러그인으로 Kotlin/JS 프로젝트를 구성·관리하는 방법.

### 개요

Kotlin/JS 프로젝트는 `kotlin.multiplatform` 플러그인과 함께 Gradle을 빌드 시스템으로 사용, 다음 기능 제공:
- npm 또는 Yarn으로 npm 의존성 다운로드
- webpack으로 JavaScript 번들 빌드
- JavaScript 개발을 위한 구성 도구 및 헬퍼 태스크 제공

#### 플러그인 적용

```kotlin
plugins { kotlin("multiplatform") version "2.3.10" }
```

```gradle
plugins { id 'org.jetbrains.kotlin.multiplatform' version '2.3.10' }
```

빌드 스크립트의 `kotlin {}` 블록에서 프로젝트 구성.

### 실행 환경

Kotlin/JS는 두 가지 환경 타겟팅 가능:

#### 브라우저

```kotlin
kotlin {
    js {
        browser { }
        binaries.executable()
    }
}
```

#### Node.js

```kotlin
kotlin {
    js {
        nodejs { }
        binaries.executable()
    }
}
```

`binaries.executable()` → 실행 가능한 `.js` 파일 생성. 생략 시 라이브러리 파일만 생성됨.

### ES2015 기능 지원

Kotlin이 실험적으로 지원하는 기능:
- 모듈: 유지보수성 향상
- 클래스: 더 깔끔한 OOP 코드
- 제너레이터: suspend 함수에 대해 더 나은 번들 크기와 디버깅

ES2015 기능 활성화:

```kotlin
tasks.withType<KotlinJsCompile>().configureEach {
    compilerOptions { target = "es2015" }
}
```

### 출력 세분화

`gradle.properties`에서 컴파일러가 `.js` 파일을 출력하는 방식 구성:

- 모듈별(기본값): 각 모듈마다 별도의 `.js`
- 전체 프로그램: 전체 프로젝트에 단일 `.js` 파일
  ```properties
  kotlin.js.ir.output.granularity=whole-program
  ```
- 파일별: Kotlin 파일마다 하나의 `.js`(ES2015 타겟 필요)
  ```properties
  kotlin.js.ir.output.granularity=per-file
  ```

### TypeScript 선언 파일

자동 완성 및 정적 분석을 위해 Kotlin 코드에서 TypeScript 정의(`.d.ts`) 생성:

```kotlin
kotlin {
    js {
        browser { }
        binaries.executable()
        generateTypeScriptDefinitions()
    }
}
```

포함할 선언은 `@JsExport`로 표시. 파일은 `build/js/packages/<package_name>/kotlin/`에 생성됨.

### 의존성

#### Kotlin 표준 라이브러리

표준 라이브러리 의존성은 플러그인과 동일한 버전으로 자동 추가됨.

멀티플랫폼 테스트는 `kotlin.test` 사용:

```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

#### NPM 의존성

`npm()` 함수로 npm 의존성 선언:

```kotlin
dependencies {
    implementation(npm("react", "> 14.0.0 <=16.9.0"))
}
```

의존성 유형:
- `npm(...)`: 일반 의존성
- `devNpm(...)`: 개발 의존성
- `optionalNpm(...)`: 선택적 의존성
- `peerNpm(...)`: 피어 의존성

기본적으로 Yarn이 의존성 관리. npm을 대신 쓰려면:

```properties
kotlin.js.yarn=false
```

### 실행 및 테스트

#### 실행 태스크

브라우저 프로젝트:

```bash
./gradlew jsBrowserDevelopmentRun
```

연속 빌드:

```bash
./gradlew jsBrowserDevelopmentRun --continuous
# 또는
./gradlew jsBrowserDevelopmentRun -t
```

Node.js 프로젝트:

```bash
./gradlew jsNodeDevelopmentRun
```

#### 테스트 태스크

테스트는 자동으로 구성됨:
- 브라우저: 기본적으로 Headless Chrome과 함께 Karma 테스트 러너
- Node.js: Mocha 테스트 프레임워크

테스트 브라우저 구성:

```kotlin
kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChrome()
                    useFirefox()
                    useSafari()
                }
            }
        }
    }
}
```

또는 `gradle.properties`에서:

```properties
kotlin.js.browser.karma.browsers=firefox,safari
```

테스트 실행:

```bash
./gradlew check
```

테스트 비활성화:

```kotlin
kotlin {
    js {
        browser {
            testTask { enabled = false }
        }
    }
}
```

#### Karma 구성

사용자 정의 구성 파일은 `karma.config.d/` 디렉토리에 배치. 모든 `.js` 파일은 생성된 `karma.conf.js`에 자동으로 병합됨.

### Webpack 번들링

#### 버전

Kotlin Multiplatform 플러그인은 기본적으로 webpack 5 사용. 이전 프로젝트는 webpack 4로 되돌리기:

```properties
kotlin.js.webpack.major.version=4
```

#### Webpack 태스크 구성

```kotlin
webpackTask {
    outputFileName = "mycustomfilename.js"
    output.libraryTarget = "commonjs2"
}
```

번들링·실행·테스트를 위한 공통 설정:

```kotlin
browser {
    commonWebpackConfig {
        // 여기에 구성
    }
}
```

#### Webpack 구성 파일

사용자 정의 webpack 구성은 `webpack.config.d/` 디렉토리에 저장. 모든 `.js` 파일은 `build/js/packages/projectName/webpack.config.js`에 자동으로 병합됨.

예시 - webpack 로더 추가:

```javascript
config.module.rules.push({
    test: /\.extension$/,
    loader: 'loader-name'
});
```

#### 실행 파일 빌드

```bash
# 개발 (더 빠르고, 더 큼)
./gradlew browserDevelopmentWebpack

# 프로덕션 (더 느리고, 더 작고, 최소화됨)
./gradlew browserProductionWebpack
```

출력은 `build/dist`(또는 사용자 정의 [배포 대상 디렉토리](#배포-대상-디렉토리))에 위치.

### CSS 지원

CSS 지원 활성화:

```kotlin
browser {
    commonWebpackConfig {
        cssSupport { enabled.set(true) }
    }
}
```

또는 태스크별로:

```kotlin
browser {
    webpackTask { cssSupport { enabled.set(true) } }
    runTask { cssSupport { enabled.set(true) } }
    testTask {
        useKarma {
            webpackConfig.cssSupport { enabled.set(true) }
        }
    }
}
```

`cssSupport.mode`를 통한 CSS 처리 모드:
- `"inline"`(기본값): 전역 `<style>` 태그에 추가
- `"extract"`: 별도 파일로 추출
- `"import"`: 코드 접근을 위해 문자열로 처리

### Node.js 구성

#### Node.js 버전 구성

특정 하위 프로젝트:

```kotlin
project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version = "18.0.0"
}
```

전체 프로젝트:

```kotlin
allprojects {
    project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
        project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version = "18.0.0"
    }
}
```

#### 사전 설치된 Node.js 사용

```kotlin
project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}
```

### Yarn 구성

플러그인은 기본적으로 자체 Yarn 인스턴스를 관리 → 사전 설치된 Yarn 사용도 가능.

#### 사용자 정의 Yarn 설정(.yarnrc)

프로젝트 루트에 `.yarnrc` 생성:

```
registry "http://my.registry/api/npm/"
```

#### 사전 설치된 Yarn 사용

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
}
```

#### 버전 잠금(kotlin-js-store)

`kotlin-js-store/` 디렉토리에는 버전 잠금을 위한 `yarn.lock` 포함. 버전 관리에 커밋 필요.

디렉토리 및 잠금 파일 사용자 정의:

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().lockFileDirectory =
        project.rootDir.resolve("my-kotlin-js-store")
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().lockFileName = "my-yarn.lock"
}
```

#### yarn.lock 업데이트 보고

`yarn.lock` 변경 사항 보고 방식 제어:

```kotlin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING  // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = false  // true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = false  // true
}
```

#### NPM 라이프사이클 스크립트 보안

기본적으로 npm 라이프사이클 스크립트는 보안을 위해 비활성화됨. 필요 시 활성화:

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
}
```

### 배포 대상 디렉토리

기본값: `/build/dist/<targetName>/<binaryName>`

출력 위치 변경:

```kotlin
kotlin {
    js {
        browser {
            distribution { outputDirectory.set(projectDir.resolve("output")) }
            binaries.executable()
        }
    }
}
```

### 모듈 이름

JavaScript 모듈 이름 구성(`build/js/packages/myModuleName`에 영향):

```kotlin
js { outputModuleName = "myModuleName" }
```

`build/dist`의 webpack 출력에는 영향 없음.

### package.json 사용자 정의

Kotlin Multiplatform 플러그인은 자동으로 `package.json` 생성. `customField()` 함수로 사용자 정의 필드 추가:

```kotlin
kotlin {
    js {
        compilations["main"].packageJson {
            customField("hello", mapOf("one" to 1, "two" to 2))
        }
    }
}
```

다음을 생성함:

```json
{
    "hello": { "one": 1, "two": 2 }
}
```

---

## Kotlin/JS 실행하기

> 원문: https://kotlinlang.org/docs/running-kotlin-js.html

Kotlin Multiplatform Gradle 플러그인으로 Kotlin/JS 프로젝트를 실행하는 방법.

### 초기 설정

`src/jsMain/kotlin/App.kt`에 샘플 파일 생성:

```kotlin
fun main() {
    console.log("Hello, Kotlin/JS!")
}
```

### Node.js 타겟 실행

Node.js를 타겟팅하는 Kotlin/JS 프로젝트는 `jsNodeDevelopmentRun` Gradle 태스크로 실행:

```bash
./gradlew jsNodeDevelopmentRun
```

IntelliJ IDEA에서: Gradle 도구 창에서 `jsNodeDevelopmentRun` 액션 찾기.

프로세스:
1. `kotlin.multiplatform` 플러그인이 첫 실행 시 필요한 의존성 다운로드
2. 빌드 완료 후 프로그램 실행
3. 로깅 출력이 터미널에 나타남

### 브라우저 타겟 실행

#### HTML 설정

`/src/jsMain/resources/index.html` 생성:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>JS Client</title>
  </head>
  <body>
    <script src="js-tutorial.js"></script>
  </body>
</html>
```

참고: 스크립트 파일명은 기본적으로 프로젝트 이름 → 그에 맞게 조정 필요(예: 프로젝트 이름이 `followAlong`이면 `followAlong.js`).

#### 개발 서버 실행

```bash
./gradlew jsBrowserDevelopmentRun
```

IntelliJ IDEA에서: Gradle 도구 창에서 `jsBrowserDevelopmentRun` 액션 찾기.

진행 과정:
1. 프로젝트 빌드
2. Webpack 개발 서버 시작
3. 브라우저 창이 HTML 파일로 열림
4. 브라우저 개발자 도구에서 콘솔 출력 확인(우클릭 → 검사 → Console 탭)

#### 연속 개발

개발 중 연속 컴파일/핫 리로드 설정은 [별도 튜토리얼](https://kotlinlang.org/docs/dev-server-continuous-compilation.html) 참고.

### 요약

- Node.js
  - Gradle 태스크: `jsNodeDevelopmentRun`
  - 출력 위치: 터미널
- 브라우저
  - Gradle 태스크: `jsBrowserDevelopmentRun`
  - 출력 위치: 브라우저 콘솔

### 다음 단계

- [Kotlin/JS 프로젝트 설정](https://kotlinlang.org/docs/js-project-setup.html): 프로젝트 구성 옵션 자세히 알아보기
- [Kotlin/JS 디버깅](https://kotlinlang.org/docs/js-debugging.html): 애플리케이션 디버깅 방법
- [Kotlin/JS 테스트 실행](https://kotlinlang.org/docs/js-running-tests.html): 테스트 작성 및 실행

---

## Kotlin의 Dynamic 타입

> 원문: https://kotlinlang.org/docs/dynamic-type.html

### 개요

dynamic 타입: 정적 타입 검사를 비활성화 → 타입이 없거나 느슨하게 타입이 지정된 환경, 특히 JavaScript와의 상호운용성을 용이하게 하는 Kotlin 기능. 참고: dynamic 타입은 JVM을 타겟팅하는 코드에서는 지원 안 됨.

### 선언

```kotlin
val dyn: dynamic = ...
```

### 타입 검사 동작

`dynamic` 타입은 본질적으로 Kotlin의 타입 검사기를 끔:

- `dynamic` 값은 모든 변수에 할당하거나 어디서든 매개변수로 전달 가능
- 모든 값을 `dynamic` 변수에 할당하거나 `dynamic`을 받는 함수에 전달 가능
- `dynamic` 값의 null 검사가 비활성화됨

### Dynamic 함수 및 프로퍼티 호출

`dynamic` 변수에서 모든 매개변수로 모든 프로퍼티나 함수 호출 가능:

```kotlin
dyn.whatever(1, "foo", dyn)           // 'whatever'는 어디에도 정의되어 있지 않음
dyn.whatever(*arrayOf(1, 2, 3))       // 스프레드 연산자와 함께 작동
```

이 코드는 그대로 JavaScript로 컴파일됨.

### 중요 고려사항

`dynamic` 값에서 Kotlin 함수 호출 시 Kotlin-to-JavaScript 컴파일러의 이름 맹글링 주의 필요. 잘 정의된 이름을 할당하려면 `@JsName` 어노테이션 사용:

```kotlin
@JsName("myFunction")
fun myFunction() { ... }
```

### Dynamic 호출 체이닝

dynamic 호출은 `dynamic` 반환 → 자유로운 체이닝 허용:

```kotlin
dyn.foo().bar.baz()
```

### 람다 매개변수

dynamic 호출의 람다 매개변수는 기본적으로 `dynamic` 타입:

```kotlin
dyn.foo { x -> x.bar() }  // x는 dynamic
```

### 지원되는 연산자

- 이항: `+`·`-`·`*`·`/`·`%`·`>`·`<`·`>=`·`<=`·`==`·`!=`·`===`·`!==`·`&&`·`||`
- 단항 전위: `-`·`+`·`!`
- 전위/후위: `++`·`--`
- 할당: `+=`·`-=`·`*=`·`/=`·`%=`
- 인덱스 접근: `d[a]`(읽기)·`d[a1] = a2`(쓰기)

### 금지된 연산

`dynamic` 타입과 함께 `in`·`!in`·`..` 연산은 허용 안 됨.

### 사용 예시

#### JavaScript 라이브러리와 상호작용

```kotlin
// JavaScript 라이브러리의 객체를 dynamic으로 처리
val jsObject: dynamic = js("{}")
jsObject.name = "Kotlin"
jsObject.version = 2.0
console.log(jsObject.name)  // "Kotlin" 출력
```

#### JSON 데이터 처리

```kotlin
val jsonData: dynamic = JSON.parse("""{"key": "value", "count": 42}""")
println(jsonData.key)    // "value"
println(jsonData.count)  // 42
```

#### DOM 조작

```kotlin
val element: dynamic = document.getElementById("myElement")
element.style.color = "red"
element.innerHTML = "Hello, World!"
element.addEventListener("click") {
    console.log("Element clicked!")
}
```

### 주의사항

- dynamic 타입은 컴파일 타임 타입 안전성을 포기함
- 런타임 오류 발생 가능 → 주의해서 사용 필요
- 가능하면 타입이 지정된 Kotlin 래퍼나 외부 선언 사용 권장

### 다음 단계

- [Kotlin/JS 개요](https://kotlinlang.org/docs/js-overview.html)
- [JavaScript 모듈과 상호작용](https://kotlinlang.org/docs/js-modules.html)
- [JavaScript에서 Kotlin 호출](https://kotlinlang.org/docs/js-to-kotlin-interop.html)

---

## 코틀린/Native

## Kotlin/Native 시작하기 - 완전 가이드

> 원문: https://kotlinlang.org/docs/native-get-started.html

### 개요

Kotlin/Native 애플리케이션을 만드는 세 가지 방법:
1. IntelliJ IDEA(IDE 접근 방식)
2. Gradle(빌드 시스템)
3. 명령줄 컴파일러(직접 컴파일)

Kotlin/Native는 다양한 타겟(Linux·macOS·Windows)으로 컴파일. 크로스 플랫폼 컴파일 가능하지만, 이 튜토리얼은 동일 플랫폼 컴파일에 중점.

Mac 사용자: 먼저 Xcode 명령줄 도구 설치 필요.

---

### 방법 1: IntelliJ IDEA 사용

#### 프로젝트 생성

1. 최신 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드(Community 또는 Ultimate Edition)
2. 프로젝트 템플릿 복제:
   - File | New | Project from Version Control 선택
   - URL: `https://github.com/Kotlin/kmp-native-wizard`
3. `gradle/libs.versions.toml`을 열고 최신 Kotlin 버전 확인:
   ```
   [versions]
   kotlin = "2.3.10"
   ```
4. Gradle 파일 다시 로드 제안을 따름

#### 빌드 및 실행

1. `src/nativeMain/kotlin/Main.kt` 열기
2. 거터에서 녹색 실행 아이콘 클릭
3. IntelliJ IDEA가 Gradle을 통해 코드를 실행 → Run 탭에 출력 표시

#### 애플리케이션 업데이트

기본 이름 입력:

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()
}
```

이름의 글자 수 세기:

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
    }
}
```

Gradle 입력 지원을 위한 `build.gradle.kts` 업데이트:

```kotlin
kotlin {
    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                runTaskProvider?.configure {
                    standardInput = System.`in`
                }
            }
        }
    }
}
```

고유 글자 수 세기:

```kotlin
fun String.countDistinctCharacters() = lowercase().toList().distinct().count()

fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
        println("Your name contains ${it.countDistinctCharacters()} unique letters")
    }
}
```

---

### 방법 2: Gradle 사용

#### 프로젝트 파일 생성

1. 호환되는 [Gradle 버전](https://gradle.org/install/) 설치
2. `build.gradle.kts` 생성:

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
}

repositories {
    mavenCentral()
}

kotlin {
    macosArm64("native") {      // macOS에서
    // linuxArm64("native")      // Linux에서
    // mingwX64("native")        // Windows에서
        binaries {
            executable()
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.BIN
}
```

3. 빈 `settings.gradle(.kts)` 파일 생성
4. `src/nativeMain/kotlin/hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

#### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew nativeBinaries

# 실행 파일 실행
build/bin/native/debugExecutable/<project_name>.kexe
```

이 과정으로 `build/bin/native/debugExecutable` 및 `releaseExecutable` 디렉토리 생성됨.

#### IDE에서 열기

- File | Open 선택
- 프로젝트 디렉토리 선택
- IntelliJ IDEA가 Kotlin/Native 프로젝트를 자동으로 감지

---

### 방법 3: 명령줄 컴파일러 사용

#### 다운로드 및 설치

1. [Kotlin GitHub 릴리스](https://github.com/JetBrains/kotlin/releases/tag/v2.3.10)로 이동
2. `kotlin-native-prebuilt-<os>-<arch>-2.3.10.tar.gz` 다운로드
3. 아카이브 압축 해제
4. PATH에 추가:

```bash
export PATH="/<path to compiler>/kotlin-native/bin:$PATH"
```

요구 사항: Java 1.8 이상

#### 프로그램 생성

`hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

#### 컴파일 및 실행

```bash
# 컴파일
kotlinc-native hello.kt -o hello

# 실행
./hello.kexe    # macOS/Linux
./hello.exe     # Windows
```

`-o` 옵션으로 출력 파일명 지정. 컴파일러는 `.kexe`(macOS/Linux) 또는 `.exe`(Windows) 생성.

---

### 타겟 플랫폼

- macOS(ARM): `macosArm64`
- macOS(x64): `macosX64`
- Linux(ARM): `linuxArm64`
- Linux(x64): `linuxX64`
- Windows: `mingwX64`
- iOS(ARM): `iosArm64`
- iOS 시뮬레이터: `iosSimulatorArm64`

---

### 다음 단계

- [C 상호운용 및 libcurl을 사용한 앱 만들기](https://kotlinlang.org/docs/native-app-with-c-and-libcurl.html) 튜토리얼 완료
- 고급 [Gradle 빌드 스크립트](/docs/multiplatform/multiplatform-dsl-reference.html) 학습
- [Gradle 문서](https://kotlinlang.org/docs/gradle.html) 탐색

---

## Kotlin/Native 개요

> 원문: https://kotlinlang.org/docs/native-overview.html

### Kotlin/Native란?

Kotlin/Native: Kotlin 코드를 가상 머신 없이 실행되는 네이티브 바이너리로 컴파일하는 기술. 특징:
- Kotlin 컴파일러를 위한 LLVM 기반 백엔드
- Kotlin 표준 라이브러리의 네이티브 구현

### Kotlin/Native를 사용하는 이유

Kotlin/Native는 다음을 위해 설계됨:
- VM이 없는 플랫폼: 임베디드 장치·iOS 및 기타 제한된 환경
- 자체 포함 프로그램: 추가 런타임이나 VM 불필요
- 쉬운 통합: C·C++·Swift·Objective-C 및 기타 언어의 기존 프로젝트와 원활하게 작동

### 지원되는 타겟 플랫폼

- Linux
- Windows(MinGW를 통해)
- Android NDK
- Apple 플랫폼: macOS·iOS·tvOS·watchOS
  - Xcode 및 명령줄 도구 필요

### 상호운용성 기능

#### C와의 상호운용성

- Kotlin 코드에서 직접 기존 C 라이브러리 사용
- 제공되는 튜토리얼:
  - C 헤더가 있는 동적 라이브러리 생성
  - C 타입을 Kotlin으로 매핑
  - libcurl로 네이티브 HTTP 클라이언트 빌드

#### Swift/Objective-C와의 상호운용성

- Objective-C를 통한 양방향 상호운용성
- macOS 및 iOS의 Swift/Objective-C 애플리케이션에서 직접 Kotlin 코드 사용
- Kotlin에서 Apple 프레임워크 생성

### 코드 공유

- 플랫폼 라이브러리: POSIX·gzip·OpenGL·Metal·Foundation 및 Apple 프레임워크용 사전 빌드된 라이브러리
- Kotlin Multiplatform: Android·iOS·JVM·웹 및 네이티브 플랫폼 간 코드 공유

### 메모리 관리

- JVM 및 Go와 유사한 자동 메모리 관리자
- 내장 추적 가비지 컬렉터
- Swift/Objective-C의 ARC(Automatic Reference Counting)와 통합
- 사용량을 최적화하고 할당 급증을 방지하는 커스텀 메모리 할당자

### 주요 장점

1. 네이티브 성능: JIT 컴파일 오버헤드 없이 네이티브 속도로 실행
2. 작은 바이너리: VM 없이 자체 포함된 실행 파일
3. 직접 메모리 접근: 시스템 수준 프로그래밍 가능
4. 플랫폼 간 공유: 단일 코드베이스로 여러 플랫폼 지원

### 사용 사례

- iOS 앱 개발: Kotlin Multiplatform으로 Android와 iOS 간 코드 공유
- 임베디드 시스템: 리소스가 제한된 환경에서 실행
- 명령줄 도구: 빠른 시작 시간과 작은 바이너리
- 게임 개발: 네이티브 성능이 필요한 경우
- 시스템 프로그래밍: C 라이브러리와의 통합

### 시작하기

- [Kotlin/Native 시작하기](https://kotlinlang.org/docs/native-get-started.html)
- [C 상호운용 및 libcurl을 사용한 앱 만들기](https://kotlinlang.org/docs/native-app-with-c-and-libcurl.html)
- [Gradle 빌드 스크립트 참조](/docs/multiplatform/multiplatform-dsl-reference.html)

### 다음 단계

- C 라이브러리 사용 방법 알아보기
- Swift/Objective-C와의 상호운용성 탐색
- 메모리 관리 이해하기

---

## C와의 상호운용성

> 원문: https://kotlinlang.org/docs/native-c-interop.html

cinterop 도구를 사용한 Kotlin/Native의 C 라이브러리 상호운용성.

### 개요

- C 라이브러리 임포트는 베타 상태 → `@ExperimentalForeignApi` 어노테이션 필요
- cinterop 도구는 C 헤더를 분석 → Kotlin 바인딩 자동 생성
- 생성된 스텁은 IDE 코드 완성 및 네비게이션 가능하게 함

### 프로젝트 설정

1. 바인딩에 포함할 내용을 설명하는 [정의 파일](https://kotlinlang.org/docs/native-definition-file.html) 생성 및 구성
2. cinterop을 포함하도록 Gradle 빌드 파일 구성
3. 실행 파일 생성을 위해 컴파일 및 실행

실습 경험은 [C 상호운용을 사용한 앱 만들기](https://kotlinlang.org/docs/native-app-with-c-and-libcurl.html) 튜토리얼 참고.

참고: 플랫폼 라이브러리(POSIX·Win32·Apple 프레임워크)는 사용자 정의 구성 불필요.

---

### 바인딩

#### 기본 상호운용 타입

- 정수/부동소수점 → 동일한 너비의 Kotlin 대응
- 포인터 및 배열 → `CPointer<T>?`
- 열거형 → Kotlin 열거형 또는 정수 값
- 구조체/공용체 → 점 표기법 필드 접근이 있는 타입
- `typedef` → `typealias`

Lvalue 표현(메모리 위치 값):
- 구조체: 구조체와 동일한 이름
- Kotlin 열거형: `${type}.Var`
- `CPointer<T>`: `CPointerVar<T>`
- 기타 타입: `${type}Var`

#### 포인터 타입

```kotlin
val path = getenv("PATH")?.toKString() ?: ""

import kotlinx.cinterop.*
@OptIn(ExperimentalForeignApi::class)
fun shift(ptr: CPointer<ByteVar>, length: Int) {
    for (index in 0 .. length - 2) {
        ptr[index] = ptr[index + 1]
    }
}
```

주요 연산:
- `.pointed`: 포인터가 가리키는 lvalue 반환
- `.ptr`: lvalue를 포인터로 변환
- `void*` → `COpaquePointer`로 매핑(모든 포인터의 상위 타입)

포인터 캐스팅:

```kotlin
val intPtr = bytePtr.reinterpret<IntVar>()
val longValue = ptr.toLong()
val originalPtr = longValue.toCPointer<IntVar>()
```

#### 메모리 할당

```kotlin
@file:OptIn(ExperimentalForeignApi::class)
import kotlinx.cinterop.*

// nativeHeap 사용 (수동 free 필요)
val buffer = nativeHeap.allocArray<ByteVar>(size)
nativeHeap.free(buffer)

// memScoped 사용 (자동 free)
val fileSize = memScoped {
    val statBuf = alloc<stat>()
    val error = stat("/", statBuf.ptr)
    statBuf.st_size
}
```

#### 바인딩에 포인터 전달

C 함수 포인터 매개변수 → `CValuesRef<T>`로 매핑. 시퀀스 구성 방법:

```kotlin
// C: void foo(int* elements, int count);
// Kotlin:
foo(cValuesOf(1, 2, 3), 3)

// 다른 방법:
intArrayOf(1, 2, 3).toCValues()
listOf(ptr1, ptr2).toCValues()
```

#### 문자열

`const char*` 타입의 매개변수는 Kotlin `String`으로 매핑됨:

```kotlin
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
memScoped {
    LoadCursorA(null, "cursor.bmp".cstr.ptr)  // UTF-8
    LoadCursorW(null, "cursor.bmp".wcstr.ptr) // UTF-16
}

// 수동 변환:
val cString = kotlinString.cstr.getPointer(nativeHeap)
val kotlinStr = cPointer.toKString()
```

자동 변환 건너뛰려면 `.def` 파일에 추가:

```
noStringConversion = LoadCursorA LoadCursorW
```

#### 스코프 로컬 포인터

```kotlin
memScoped {
    items = arrayOfNulls<CPointer<ITEM>?>(6)
    arrayOf("one", "two").forEachIndexed { index, value ->
        items[index] = value.cstr.ptr
    }
    menu = new_menu("Menu".cstr.ptr, items.toCValues().ptr)
}
```

#### 값으로 구조체 전달 및 수신

C 함수가 값으로 구조체를 받거나 반환할 때 `CValue<T>` 사용:

```kotlin
// 구조체 값에서 필드 읽기
val fieldValue = structValue.useContents { field }

// 구조체 값 생성
val structVal = cValue {
    field1 = 10
    field2 = 20
}

// 수정된 복사본 생성
val newStructVal = structValue.copy {
    field1 = 30
}
```

#### 콜백

```kotlin
// Kotlin 함수를 C 함수 포인터로 변환
staticCFunction(::kotlinFunction)

// StableRef로 사용자 데이터 전달
val stableRef = StableRef.create(kotlinObject)
val voidPtr = stableRef.asCPointer()

// 콜백에서 언래핑
val stableRef = voidPtr.asStableRef<KotlinClass>()
val kotlinRef = stableRef.get()

stableRef.dispose() // 메모리 누수 방지
```

#### 이식성

플랫폼 종속 타입(`long`·`size_t`)은 `.convert()` 사용:

```kotlin
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun zeroMemory(buffer: COpaquePointer, size: Int) {
    memset(buffer, 0, size.convert<size_t>())
}
```

#### 객체 피닝

`.usePinned()` 사용:

```kotlin
val buffer = ByteArray(1024)
buffer.usePinned { pinned ->
    recv(fd, pinned.addressOf(0), buffer.size.convert(), 0)
}
```

`.refTo()` 사용:

```kotlin
val buffer = ByteArray(1024)
recv(fd, buffer.refTo(0), buffer.size.convert(), 0)
```

#### 전방 선언

`cnames` 패키지를 통해 전방 선언 임포트:

```kotlin
import cnames.structs.ForwardDeclaredStruct

fun test() {
    consumeStruct(produceStruct() as CPointer<cnames.structs.ForwardDeclaredStruct>)
}
```

---

### 다음 단계

관련 튜토리얼:
- [C에서 원시 데이터 타입 매핑](https://kotlinlang.org/docs/mapping-primitive-data-types-from-c.html)
- [C에서 구조체 및 공용체 타입 매핑](https://kotlinlang.org/docs/mapping-struct-union-types-from-c.html)
- [C에서 함수 포인터 매핑](https://kotlinlang.org/docs/mapping-function-pointers-from-c.html)
- [C에서 문자열 매핑](https://kotlinlang.org/docs/mapping-strings-from-c.html)

---

## Swift/Objective-C와의 상호운용성

> 원문: https://kotlinlang.org/docs/native-objc-interop.html

### 개요

Kotlin/Native는 Objective-C를 통해 Swift와의 간접적인 상호운용성 제공. Swift/Objective-C 코드에서 Kotlin 선언을 사용하는 것과 그 반대 경우를 다룸.

참고: Objective-C 라이브러리 임포트는 베타 상태. Objective-C 라이브러리에서 cinterop으로 생성된 모든 Kotlin 선언은 `@ExperimentalForeignApi` 어노테이션 필요.

### Swift/Objective-C 라이브러리를 Kotlin으로 임포트

Objective-C 프레임워크 및 시스템 라이브러리는 적절히 임포트하면 Kotlin 코드에서 사용 가능:
- [정의 파일](https://kotlinlang.org/docs/native-definition-file.html)을 통해 라이브러리 임포트 구성
- [네이티브 언어 상호운용 컴파일 구성](/docs/multiplatform/multiplatform-configure-compilations.html)

Swift 지원: 순수 Swift 모듈은 아직 지원 안 됨. Swift 라이브러리는 API가 `@objc`로 Objective-C에 내보내진 경우에만 사용 가능.

### Swift/Objective-C에서 Kotlin 사용

Swift/Objective-C에서 사용하려면 Kotlin 모듈을 프레임워크로 컴파일 필요:
- [바이너리 선언](/docs/multiplatform/multiplatform-build-native-binaries.html#declare-binaries)
- 참조: [Kotlin Multiplatform 샘플 프로젝트](https://github.com/Kotlin/kmm-basic-sample)

#### 가시성 제어

##### `@HiddenFromObjC`로 선언 숨기기

```kotlin
@HiddenFromObjC
fun internalFunction() { }
```

이 어노테이션은 다른 Kotlin 모듈에서는 계속 보이면서 Objective-C와 Swift에서만 선언을 숨김. 또는 `internal` 수정자로 컴파일 모듈 내에서 가시성 제한.

##### `@ShouldRefineInSwift` 사용

생성된 Objective-C 헤더에서 함수나 프로퍼티를 `swift_private`로 표시:

```kotlin
@ShouldRefineInSwift
fun complexOperation() { }
```

이런 선언은 `__` 접두사가 붙어 Swift에서 보이지 않음 → Swift 친화적인 래퍼 제작에 사용 가능.

#### `@ObjCName`으로 선언 이름 변경

```kotlin
@ObjCName(swiftName = "MySwiftArray")
class MyKotlinArray {
    @ObjCName("index")
    fun indexOf(@ObjCName("of") element: String): Int = TODO()
}

// Swift에서 사용
let array = MySwiftArray()
let index = array.index(of: "element")
```

#### KDoc 주석으로 문서화

KDoc 주석 → Objective-C 주석으로 변환되어 생성된 프레임워크에 포함됨:

```kotlin
/**
 * 인수의 합을 출력합니다.
 * 합이 32비트 정수에 맞지 않는 경우를 적절히 처리합니다.
 */
fun printSum(a: Int, b: Int) = println(a.toLong() + b)
```

Objective-C 헤더를 생성함:

```objc
/**
 * 인수의 합을 출력합니다.
 * 합이 32비트 정수에 맞지 않는 경우를 적절히 처리합니다.
 */
+ (void)printSumA:(int32_t)a b:(int32_t)b __attribute__((swift_name("printSum(a:b:)")));
```

KDoc 내보내기 비활성화(`build.gradle.kts`):

```kotlin
kotlin {
    iosArm64 {
        binaries {
            framework {
                baseName = "sdk"
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                exportKdoc.set(false)
            }
        }
    }
}
```

### 타입 매핑

- `class` → Swift `class` → Objective-C `@interface`
- `interface` → Swift `protocol` → Objective-C `@protocol`
- 프로퍼티 → 프로퍼티 → 프로퍼티
- 메서드 → 메서드 → 메서드
- `enum class` → Swift `class` → Objective-C `@interface`
- `suspend` → Swift `async`/`completionHandler:` → Objective-C `completionHandler:`
- `@Throws fun` → Swift `throws` → Objective-C `error:(NSError**)error`
- 확장 → 확장 → 카테고리 멤버
- `companion` 멤버 → 클래스 메서드/프로퍼티 → 클래스 메서드/프로퍼티
- `null` → Swift `nil` → Objective-C `nil`
- 원시 타입 → Swift 원시 타입/`NSNumber`
- `Unit` → Swift `Void` → Objective-C `void`
- `String` → Swift `String` → Objective-C `NSString`
- `List` → Swift `Array` → Objective-C `NSArray`
- `MutableList` → Swift `NSMutableArray` → Objective-C `NSMutableArray`
- `Set` → Swift `Set` → Objective-C `NSSet`
- `Map` → Swift `Dictionary` → Objective-C `NSDictionary`
- 함수 타입 → 함수 타입 → 블록 포인터 타입

#### 클래스

이름 변환: Objective-C 프로토콜은 `Protocol` 접미사가 있는 인터페이스로 임포트됨(예: `@protocol Foo` → `interface FooProtocol`). Kotlin 클래스 이름은 프레임워크 이름에 따라 Objective-C로 임포트될 때 접두사가 붙음.

강한 링킹: Kotlin에서 사용되는 Objective-C 클래스는 강하게 링크됨 → Swift/Objective-C 래퍼로 클래스 가용성을 확인하고 "Symbol not found" 충돌 방지.

#### 이니셜라이저

- Swift/Objective-C 이니셜라이저는 Kotlin 생성자 또는 `create`라는 팩토리 메서드로 임포트됨
- 확장 이니셜라이저는 `create` 팩토리 메서드로 임포트됨(Kotlin에는 확장 생성자가 없음)
- Kotlin 생성자는 Swift/Objective-C에서 이니셜라이저로 임포트됨

예시:

```kotlin
class ViewController : UIViewController {
    @OverrideInit
    constructor(coder: NSCoder) : super(coder)
}
```

#### 최상위 함수 및 프로퍼티

최상위 Kotlin 함수는 생성된 클래스(파일당 하나)를 통해 접근 가능:

```kotlin
// MyLibraryUtils.kt
package my.library
fun foo() {}
```

Swift에서 호출:

```swift
MyLibraryUtilsKt.foo()
```

#### 오류 및 예외

모든 Kotlin 예외는 검사되지 않음 · Swift에는 검사 오류만 있음. 던질 수 있는 메서드를 표시하려면 `@Throws` 사용:

```kotlin
@Throws(IOException::class)
fun readFile(path: String): String { }
```

동작:
- `@Throws`가 있는 비-`suspend` 함수: Objective-C에서 `NSError*` 생성 메서드로, Swift에서 `throws`로 매핑
- `suspend` 함수: 항상 완료 핸들러에 `NSError*/Error` 매개변수 있음
- `@Throws` 클래스와 일치하는 예외는 `NSError`로 전파 · 그 외는 프로그램 종료
- Swift/Objective-C 오류 메서드는 아직 예외 던지기로 Kotlin에 임포트 안 됨

#### 열거형

```kotlin
enum class Colors { RED, GREEN, BLUE }
```

Swift에서 접근:

```swift
Colors.red
Colors.green
Colors.blue

// 기본 케이스와 함께 switch 사용
switch color {
    case .red: print("It's red")
    case .green: print("It's green")
    case .blue: print("It's blue")
    default: fatalError("No such color")
}
```

#### Suspend 함수

Kotlin `suspend` 함수는 다음으로 표현됨:
- Objective-C: 완료 핸들러 콜백이 있는 함수
- Swift 5.5+: `async` 함수(실험적, 제한 있음, [KT-47610](https://youtrack.jetbrains.com/issue/KT-47610) 참고)

#### 확장 및 카테고리 멤버

- Objective-C 카테고리 멤버와 Swift 확장은 Kotlin 확장으로 임포트됨
- Kotlin에서 오버라이드 불가 · 확장 이니셜라이저는 생성자로 사용 불가
- 예외(Kotlin 1.8.20+): NSView(AppKit) 및 UIView(UIKit) 카테고리 멤버는 클래스 멤버로 임포트됨(오버라이드 가능)

다른 타입에 대한 Kotlin 확장은 수신자 매개변수가 있는 최상위 선언으로 처리됨:
- Kotlin `String`
- 컬렉션 타입
- `interface` 타입
- 원시 타입
- `inline` 클래스
- `Any` 타입
- 함수 타입
- Objective-C 클래스 및 프로토콜

#### Kotlin 싱글톤

```kotlin
object MyObject {
    val x = "Some value"
}

class MyClass {
    companion object {
        val x = "Some value"
    }
}
```

Swift/Objective-C에서 접근:

```swift
MyObject.shared
MyObject.shared.x
MyClass.companion
MyClass.Companion.shared
```

#### 원시 타입

Kotlin 원시 타입 박스 → 특수 Swift/Objective-C 클래스로 매핑됨(예: `kotlin.Int` → Swift에서 `KotlinInt`, Objective-C에서 `${prefix}Int`). `NSNumber`에서 파생되지만 `NSNumber`로/에서 수동 캐스팅 필요.

#### 문자열

Kotlin `String`은 Objective-C `NSString`으로 내보낸 후 Swift가 복사함. 오버헤드를 피하려면 Objective-C `NSString`으로 직접 접근:

```swift
let nsString = map as NSString
```

NSMutableString: Kotlin에서 사용 불가 · 모든 인스턴스는 Kotlin으로 전달될 때 복사됨.

#### 컬렉션

Kotlin → Objective-C → Swift:

Swift는 Objective-C 동등물을 통해 Kotlin 컬렉션을 암묵적으로 변환(성능 비용 있음). Objective-C 타입으로 명시적 캐스팅:

```kotlin
val map: Map<String, String>
```

```swift
// 암묵적 변환 피하기
let nsMap: NSDictionary = map as NSDictionary
(nsMap[key] as? NSString)?.length ?? 0

// 대신
map[key]?.count ?? 0  // 비싼 변환
```

Swift → Objective-C → Kotlin:

`NSMutableSet`과 `NSMutableDictionary`는 자동으로 변환 안 됨 → Kotlin 컬렉션을 명시적으로 생성:

```swift
let mutableSet: KotlinMutableSet = /* ... */
```

#### 함수 타입

Kotlin 함수 타입 객체 → Swift에서 클로저, Objective-C에서 블록으로 변환됨. 함수 타입의 원시 타입은 박싱된 표현으로 매핑 · `Unit` 반환 값은 `KotlinUnit` 싱글톤으로 매핑:

```kotlin
fun foo(block: (Int) -> Unit) { }
```

Swift 시그니처:

```swift
func foo(block: (KotlinInt) -> KotlinUnit)

// 다음으로 호출
foo { bar($0 as! Int32)
    return KotlinUnit()
}
```

#### 제네릭

Objective-C는 클래스에 경량 제네릭 지원 · Swift는 타입 정보를 위해 이를 임포트 가능. 제네릭 정보는 변환 중 손실될 수 있음.

제한 사항:
- 클래스에만 제네릭, 프로토콜이나 함수에는 없음
- Null 가능성: 제네릭 타입 매개변수는 Objective-C에서 기본적으로 nullable. 상한으로 non-nullable 강제:

```kotlin
class Sample<T : Any>() {  // Objective-C에서 non-nullable 강제
    fun myVal(): T
}
```

헤더에서 제네릭 비활성화:

```kotlin
binaries.framework {
    freeCompilerArgs += "-Xno-objc-generics"
}
```

#### 전방 선언

`objcnames.classes` 및 `objcnames.protocols` 패키지로 임포트:

```kotlin
import objcnames.protocols.ForwardDeclaredProtocolProtocol

fun test() {
    consumeProtocol(
        produceProtocol() as objcnames.protocols.ForwardDeclaredProtocolProtocol
    )
}
```

### 매핑된 타입 간 캐스팅

`as` 캐스트로 Kotlin과 Swift/Objective-C 타입 간 변환:

```kotlin
@file:Suppress("CAST_NEVER_SUCCEEDS")
import platform.Foundation.*

val nsNumber = 42 as NSNumber
val nsArray = listOf(1, 2, 3) as NSArray
val nsString = "Hello" as NSString
val string = nsString as String
```

불가능해 보이는 캐스트에 대한 IDE 경고를 억제하려면 `@Suppress("CAST_NEVER_SUCCEEDS")` 사용.

### 서브클래싱

#### Swift/Objective-C에서 Kotlin 클래스 및 인터페이스 서브클래싱

Kotlin 클래스와 인터페이스는 Swift/Objective-C에서 자유롭게 서브클래싱 가능.

#### Kotlin에서 Swift/Objective-C 클래스 및 프로토콜 서브클래싱

`final` Kotlin 클래스로 서브클래싱(Swift/Objective-C 타입을 상속하는 비-`final` 클래스는 지원 안 됨):

```kotlin
class ViewController : UIViewController {
    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    override fun viewDidLoad() {
        super.viewDidLoad()
    }
}
```

이니셜라이저 오버라이드: 임포트된 이니셜라이저를 오버라이드하려면 Kotlin 생성자에 `@OverrideInit` 어노테이션 사용.

충돌하는 시그니처: Objective-C에서 충돌하는 오버로드를 무시하려면 `@ObjCSignatureOverride` 어노테이션 사용:

```kotlin
@ObjCSignatureOverride
class MyClass : ObjCBase()
```

지정 이니셜라이저 검사 비활성화(`.def` 파일):

```
disableDesignatedInitializerChecks = true
```

### 지원되지 않는 기능

다음은 생성된 프레임워크 헤더에 제대로 노출되지 않음:

- 인라인 클래스(기본 원시 타입 또는 `id`로 매핑)
- 표준 Kotlin 컬렉션 인터페이스를 구현하는 사용자 정의 클래스
- Objective-C 클래스의 Kotlin 서브클래스

### 관련 리소스

- [Kotlin-Swift interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia): Swift에서 Kotlin 사용 예시
- [Swift/Objective-C ARC와의 통합](https://kotlinlang.org/docs/native-arc-integration.html): GC/ARC 통합 세부 사항
- [C와의 상호운용성](https://kotlinlang.org/docs/native-c-interop.html): C 상호운용 예시

---

## Kotlin/Native 메모리 관리

> 원문: https://kotlinlang.org/docs/native-memory-manager.html

### 개요

Kotlin/Native는 JVM·Go 및 기타 주류 기술과 유사한 현대적인 메모리 관리자 사용, 주요 기능:

- 객체는 모든 스레드에서 접근 가능한 공유 힙에 저장됨
- 주기적인 추적 가비지 컬렉션이 "루트"(로컬 및 전역 변수)에서 도달할 수 없는 객체 수집

### 가비지 컬렉터

#### 작동 방식

GC는 stop-the-world 마크 및 동시 스윕 컬렉터로 동작 · 특성:

- 별도의 스레드에서 실행 → 메모리 압박 휴리스틱 또는 타이머가 트리거
- 여러 스레드(애플리케이션 스레드·GC 스레드·선택적 마커 스레드)에서 마크 큐를 병렬로 처리
- 애플리케이션 스레드는 기본적으로 객체 마킹 중 일시 중지됨
- 약한 참조는 GC 일시 중지 시간을 최소화하기 위해 동시에 처리됨

#### 수동으로 가비지 컬렉션 활성화

```kotlin
kotlin.native.internal.GC.collect()
```

이 호출은 가비지 컬렉션을 강제하고 완료될 때까지 대기.

#### GC 성능 모니터링

`build.gradle.kts`에서 로깅 활성화:

```
-Xruntime-logs=gc=info
```

로그는 `stderr`에 출력됨.

Apple 플랫폼에서는 signpost와 함께 Xcode Instruments 사용:

1. `gradle.properties`에 설정:
   ```
   kotlin.native.binary.enableSafepointSignposts=true
   ```
2. Xcode 열기 → Product | Profile(Cmd + I)
3. `os_signpost` 템플릿 선택
4. 다음으로 구성:
   - Subsystem: `org.kotlinlang.native.runtime`
   - Category: `safepoint`
5. record 클릭 → 그래프에서 GC 일시 중지를 파란색 블롭으로 시각화

#### GC 성능 최적화

GC 일시 중지 시간을 줄이려면 동시 마킹(실험적) 활성화:

```
kotlin.native.binary.gc=cms
```

#### 가비지 컬렉션 비활성화

권장하지 않지만 테스트 또는 단기 프로그램에는 가능:

```
kotlin.native.binary.gc=noop
```

경고: 메모리 소비가 지속적으로 증가 → 시스템 메모리 고갈 위험.

#### 병렬 마킹 비활성화

단일 스레드 마킹(큰 힙에서 일시 중지 시간 증가 가능):

```
kotlin.native.binary.gcMarkSingleThreaded=true
```

### 메모리 소비

#### 메모리 소비 모니터링

메모리 누수 확인:

```kotlin
import kotlin.native.internal.*
import kotlin.test.*

class Resource

val global = mutableListOf<Resource>()

@OptIn(ExperimentalStdlibApi::class)
fun getUsage(): Long {
    GC.collect()
    return GC.lastGCInfo!!.memoryUsageAfter["heap"]!!.totalObjectsSizeBytes
}

fun run() {
    global.add(Resource())
    global.clear()
}

@Test
fun test() {
    val before = getUsage()
    run()
    val after = getUsage()
    assertEquals(before, after)
}
```

Apple 플랫폼에서 메모리 추적: Xcode Instruments의 VM Tracker를 통해(태깅이 활성화된 기본 할당자 필요).

#### 메모리 소비 조정

##### 1. Kotlin 업데이트

메모리 관리자 개선을 위해 Kotlin을 최신 상태로 유지.

##### 2. 할당자 페이징 비활성화

```
kotlin.native.binary.pagedAllocator=false
```

메모리는 페이지 대신 객체별로 예약됨. 엄격한 메모리 제한이나 시작 최적화에 유용.

##### 3. Latin-1 문자열 인코딩 활성화

기본적으로 문자열은 UTF-16 사용(문자당 2바이트). ASCII 문자는 Latin-1 활성화(1바이트):

```
kotlin.native.binary.latin1Strings=true
```

바이너리 크기와 메모리 소비 감소. Latin-1이 아닌 문자는 UTF-16으로 폴백.

참고: 실험적 상태에서 `String.pin()`·`String.usePinned()`·`String.refTo()`가 덜 효율적임.

### 백그라운드에서 단위 테스트

모킹하지 않는 한 단위 테스트에서 `Dispatchers.Main` 사용 금지. `kotlinx-coroutines-test`의 `Dispatchers.setMain()` 사용 또는 백그라운드 테스트 런처 구현:

```kotlin
package testlauncher

import platform.CoreFoundation.*
import kotlin.native.concurrent.*
import kotlin.native.internal.test.*
import kotlin.system.*

fun mainBackground(args: Array<String>) {
    val worker = Worker.start(name = "main-background")
    worker.execute(TransferMode.SAFE, { args.freeze() }) {
        val result = testLauncherEntryPoint(it)
        exitProcess(result)
    }
    CFRunLoopRun()
    error("CFRunLoopRun should never return")
}
```

다음으로 컴파일: `-e testlauncher.mainBackground`

### 메모리 관리 모범 사례

#### 순환 참조 방지

순환 참조는 메모리 누수를 일으킬 수 있음 → 약한 참조로 순환 끊기:

```kotlin
import kotlin.native.ref.WeakReference

class Node {
    var parent: WeakReference<Node>? = null
    var children: MutableList<Node> = mutableListOf()
}
```

#### 대용량 객체 관리

대용량 객체를 오래 유지하면 메모리 압박 증가 → 필요 없을 때 참조를 null로 설정:

```kotlin
var largeData: ByteArray? = loadLargeData()
// 사용 후
largeData = null
// GC가 다음 사이클에서 메모리 회수 가능
```

#### 네이티브 리소스 정리

네이티브 리소스(파일 핸들·소켓 등)는 명시적으로 정리 필요:

```kotlin
val file = fopen("data.txt", "r")
try {
    // 파일 사용
} finally {
    fclose(file)
}
```

### 다음 단계

- [레거시 메모리 관리자에서 마이그레이션](https://kotlinlang.org/docs/native-migration-guide.html)
- [Swift/Objective-C ARC 통합 세부사항 확인](https://kotlinlang.org/docs/native-arc-integration.html)

---

## 멀티플랫폼과 Wasm

## Kotlin Multiplatform

> 원문: https://kotlinlang.org/docs/multiplatform.html

### Kotlin Multiplatform이란?

Kotlin Multiplatform(KMP): JetBrains의 오픈소스 기술 → Android·iOS·데스크톱·웹·서버 간에 코드를 공유하면서도 네이티브 개발의 장점 유지 가능.

Compose Multiplatform 사용 시 여러 플랫폼에서 UI 코드도 공유 → 코드 재사용 극대화.

---

### 주요 이점

#### 1. 비용 효율성과 빠른 출시

- 로직과 UI 코드를 공유해 중복과 유지보수 비용 절감
- 여러 플랫폼에 동시 기능 출시 가능
- KMP 도입 후 55%의 사용자가 협업 개선 보고
- 65%의 팀이 성능과 품질 향상 보고(KMP Survey Q2 2024)

#### 2. 코드 공유의 유연성

- 격리된 모듈(네트워킹·스토리지 등) 공유
- 시간이 지남에 따라 점진적으로 공유 코드 확장
- 다양한 옵션:
  - UI는 네이티브로 유지하면서 모든 비즈니스 로직 공유
  - Compose Multiplatform으로 점진적으로 UI 마이그레이션
  - 네이티브와 공유 UI 코드 혼합

#### 3. iOS에서의 네이티브 느낌

- SwiftUI·UIKit 또는 Compose Multiplatform으로 UI 구축
- 각 플랫폼에서 네이티브처럼 느껴지는 앱 생성

#### 4. 네이티브 성능

- 네이티브 바이너리를 위한 Kotlin/Native 활용
- 플랫폼 API에 직접 접근
- iOS에서 SwiftUI와 비슷한 성능

#### 5. 원활한 툴링

- Kotlin Multiplatform IDE 플러그인과 함께 IntelliJ IDEA 및 Android Studio 지원
- 공통 UI 미리보기
- Compose Multiplatform용 핫 리로드
- 크로스 언어 네비게이션·리팩토링·디버깅

#### 6. AI 기반 개발

- KMP 작업을 위한 Junie(JetBrains의 AI 코딩 에이전트) 통합

---

### 지원 플랫폼

Kotlin Multiplatform의 주요 장점 중 하나는 다양한 플랫폼에 대한 광범위한 지원:

- Android
- iOS
- 데스크톱(Windows·macOS·Linux)
- 웹(JavaScript 및 WebAssembly)
- 서버(Java Virtual Machine)

---

### KMP를 사용하는 기업

Google·Duolingo·Forbes·Philips·McDonald's·Bolt·H&M·Baidu·Kuaishou·Bilibili 등이 프로덕션 애플리케이션에 KMP 도입.

---

### 사용 사례

#### 스타트업 및 MVP

스타트업은 종종 제한된 리소스와 빠듯한 일정으로 운영됨 → 개발 효율성과 비용 효과 극대화를 위해 공유 코드베이스로 여러 플랫폼 타겟팅이 유리 → 특히 시장 출시 시간이 중요한 초기 단계 제품이나 MVP에서 더욱 그러함.

#### 중소기업

중소기업은 종종 작은 팀을 유지하면서도 성숙하고 기능이 풍부한 제품 관리. Kotlin Multiplatform으로 사용자가 기대하는 네이티브 룩앤필을 유지하면서 핵심 로직 공유 가능 → 기존 코드베이스에 의존해 사용자 경험을 손상시키지 않으면서 개발 가속화 가능.

#### 엔터프라이즈 애플리케이션

대규모 애플리케이션은 일반적으로 광범위한 코드베이스가 있고, 새로운 기능이 지속적으로 추가되며, 모든 플랫폼에서 동일하게 작동해야 하는 복잡한 비즈니스 로직 존재. Kotlin Multiplatform은 점진적 통합을 제공 → 팀이 단계적으로 도입 가능.

#### 모바일 코드 공유

Kotlin Multiplatform의 주요 사용 사례 중 하나는 모바일 플랫폼 간 코드 공유. iOS와 Android 앱 간에 애플리케이션 로직을 공유 → 네이티브 UI를 구현하거나 플랫폼 API로 작업해야 할 때만 플랫폼별 코드 작성.

---

### 안정성 및 도입

2023년 11월, JetBrains는 Kotlin Multiplatform이 이제 Stable임을 발표. Google I/O 2024에서 Google은 Android와 iOS 간 비즈니스 로직 공유를 위한 Kotlin Multiplatform 사용에 대한 공식 지원 발표.

Kotlin Multiplatform 사용량은 단 1년 만에 두 배 이상 증가 → 2024년 7%에서 2025년 18%로 증가해 이 기술의 증가하는 모멘텀 강조.

---

### 시작하기

- [퀵스타트 가이드](https://kotlinlang.org/docs/quickstart.html): 환경 설정 및 샘플 애플리케이션 실행
- [케이스 스터디](https://kotlinlang.org/case-studies/?type=multiplatform): 실제 도입 사례 학습
- [샘플 앱](https://kotlinlang.org/docs/multiplatform-samples.html): 엄선된 예제 탐색
- [라이브러리 검색](https://klibs.io/): 멀티플랫폼 라이브러리 찾기

---

### 다음 단계

- Kotlin/JVM·Kotlin/JS·Kotlin/Native 및 Kotlin/Wasm에 대해 자세히 알아보기
- Compose Multiplatform으로 첫 번째 앱 만들기
- Kotlin Multiplatform 프로젝트 구조 이해하기

---

## Kotlin/Wasm 및 Compose Multiplatform 시작하기

> 원문: https://kotlinlang.org/docs/wasm-get-started.html

IntelliJ IDEA에서 Kotlin/Wasm과 함께 Compose Multiplatform 앱을 실행하고 웹 게시용 아티팩트를 생성하는 방법.

### 프로젝트 생성

1. Kotlin Multiplatform 개발을 위한 환경 설정
2. IntelliJ IDEA에서 File | New | Project로 이동
3. 왼쪽 패널에서 Kotlin Multiplatform 선택
   - 대안: [KMP 웹 마법사](https://kmp.jetbrains.com/?web=true&webui=compose&includeTests=true) 사용
4. 새 프로젝트 창 구성:
   - Name: WasmDemo
   - Group: wasm.project.demo
   - Artifact: wasmdemo
5. Web target 및 Share UI 탭 선택(다른 옵션 없음)
6. Create 클릭

### 애플리케이션 실행

1. 프로젝트가 로드된 후 실행 구성에서 composeApp [wasmJs] 선택
2. Run 클릭
3. 앱이 자동으로 `http://localhost:8080/`에서 열림
   - 참고: 포트가 다를 수 있음, 실제 포트는 Gradle 출력 확인
4. "Click me!" 버튼 클릭 → Compose Multiplatform 로고 표시

### 아티팩트 생성

게시용 아티팩트 생성 방법:

1. Gradle 도구 창 열기(View | Tool Windows | Gradle)
2. wasmdemo | Tasks | kotlin browser로 이동
3. wasmJsBrowserDistribution 태스크 실행
   - 요구 사항: Java 11+(Java 17+ 권장)

대체 명령:

```bash
./gradlew wasmJsBrowserDistribution
```

출력 위치: `composeApp/build/dist/wasmJs/productionExecutable`

### 애플리케이션 게시

선호하는 플랫폼으로 배포:
- GitHub Pages: [시작 가이드](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site#creating-your-site)
- Cloudflare: [Workers 문서](https://developers.cloudflare.com/workers/)
- Apache HTTP Server: [시작 가이드](https://httpd.apache.org/docs/2.4/getting-started.html)

### 프로젝트 구조

#### 디렉토리 레이아웃

```
WasmDemo/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/     # 공유 코드
│   │   └── wasmJsMain/     # Wasm 특정 코드
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml  # 버전 카탈로그
├── build.gradle.kts
└── settings.gradle.kts
```

#### build.gradle.kts 예시

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
            }
        }
        val wasmJsMain by getting
    }
}

compose {
    // Compose 구성
}
```

### 간단한 UI 예제

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Count: $count")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { count++ }) {
                Text("Click me!")
            }
        }
    }
}
```

### 개발 서버 실행

개발 중 핫 리로드:

```bash
./gradlew wasmJsBrowserDevelopmentRun --continuous
```

또는 Gradle 도구 창에서 `wasmJsBrowserDevelopmentRun` 태스크 실행.

### 브라우저 호환성

Kotlin/Wasm 애플리케이션 실행에는 WebAssembly GC를 지원하는 브라우저 필요:

- Chrome: 119+
- Firefox: 120+
- Safari: 18.2+
- Edge: 119+

### 문제 해결

#### 일반적인 문제

1. "브라우저에서 열리지 않음"
   - 브라우저가 WebAssembly GC를 지원하는지 확인
   - 최신 Chrome 또는 Firefox 사용
2. "빌드 실패"
   - Java 버전 확인(11+ 필요)
   - Gradle 버전 호환성 확인
3. "느린 초기 로드"
   - 프로덕션 빌드 사용(개발 빌드는 더 큼)
   - 적절한 캐싱 구성

### 다음 단계

- [iOS/Android용 Compose Multiplatform](/docs/multiplatform/compose-multiplatform-create-first-app.html) 알아보기
- Kotlin/Wasm 예제 탐색:
  - [KotlinConf 애플리케이션](https://github.com/JetBrains/kotlinconf-app)
  - [Compose 이미지 뷰어](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer)
  - [Node.js 예제](https://github.com/Kotlin/kotlin-wasm-nodejs-template)
  - [WASI 예제](https://github.com/Kotlin/kotlin-wasm-wasi-template)
- [Kotlin/Wasm Slack 커뮤니티](https://slack-chats.kotlinlang.org/c/webassembly) 참여

---

## Kotlin/Wasm 개요

> 원문: https://kotlinlang.org/docs/wasm-overview.html

### Kotlin/Wasm이란?

Kotlin/Wasm: Kotlin 코드를 WebAssembly(Wasm) 형식으로 컴파일 가능하게 함 → Wasm을 지원하는 다양한 환경과 장치에서 애플리케이션 실행 가능.

WebAssembly는 스택 기반 가상 머신을 위한 바이너리 명령어 형식, 특징:
- 플랫폼 독립적: 자체 가상 머신에서 실행
- 언어 무관: Kotlin 및 기타 언어를 위한 컴파일 타겟 제공

### 주요 사용 사례

#### 1. Compose Multiplatform을 사용한 웹 애플리케이션

- Compose Multiplatform으로 UI를 한 번 빌드하고 플랫폼 간 공유
- `wasm-js` 타겟 사용, 브라우저에서 실행
- 웹 프로젝트에서 모바일 및 데스크톱 UI 재사용
- [온라인 데모 체험](https://zal.im/wasm/jetsnack/)

#### 2. WASI를 사용한 서버 사이드 애플리케이션

- 서버 사이드 애플리케이션을 위한 WebAssembly System Interface(WASI) 사용
- 브라우저 환경 외부에서 Kotlin 애플리케이션 실행 가능
- `wasm-wasi` 타겟 사용
- 다양한 환경에서 안전하고 표준화된 인터페이스 제공

### 브라우저 요구 사항

Kotlin/Wasm 애플리케이션을 브라우저에서 실행하려면 다음 필요:
- WebAssembly의 가비지 컬렉션 제안을 지원하는 브라우저 버전
- 레거시 예외 처리 제안 지원
- 현재 브라우저 지원 현황은 [WebAssembly 로드맵](https://webassembly.org/roadmap/) 확인

### 성능

Kotlin/Wasm(베타 상태)의 성능:
- 실행 속도가 JavaScript를 능가
- JVM 성능 수준에 근접
- 최신 Chrome 버전에서 정기적인 벤치마크 수행

### 브라우저 API 지원

Kotlin/Wasm 표준 라이브러리가 제공하는 선언:
- DOM API 접근 및 조작
- 사용자 정의 선언 없이 Fetch API
- JavaScript 상호운용성 기능

Kotlin/Wasm-JavaScript 상호운용성으로 사용자 정의 선언 정의도 가능.

### 시작하기

- [Kotlin/Wasm 및 Compose Multiplatform 시작하기](https://kotlinlang.org/docs/wasm-get-started.html)
- [Kotlin/Wasm 및 WASI 시작 튜토리얼](https://kotlinlang.org/docs/wasm-wasi.html)
- [GitHub 예제](https://github.com/Kotlin/kotlin-wasm-examples) 탐색
- [YouTube 재생목록](https://kotl.in/wasm-pl) 시청

### 주요 장점

#### 1. 성능

- JavaScript보다 빠른 실행
- 거의 네이티브에 가까운 성능
- 작은 바이너리 크기

#### 2. 보안

- 메모리 안전한 샌드박스 환경
- 플랫폼 간 일관된 동작

#### 3. 이식성

- 브라우저·서버·엣지 컴퓨팅 등 다양한 환경 지원
- WASI를 통한 시스템 접근

#### 4. 코드 공유

- Kotlin Multiplatform과 통합
- 기존 Kotlin 코드 재사용

### 지원되는 타겟

- `wasm-js`
  - 환경: 브라우저
  - 사용 사례: 웹 애플리케이션, Compose Multiplatform 웹
- `wasm-wasi`
  - 환경: 서버/런타임
  - 사용 사례: 서버 사이드, CLI 도구

### 커뮤니티 피드백

Kotlin/Wasm:
- Slack: [#webassembly 채널](https://kotlinlang.slack.com/archives/CDFP59223)
- 이슈: [YouTrack](https://youtrack.jetbrains.com/issue/KT-56492)

Compose Multiplatform:
- Slack: [#compose-web 채널](https://slack-chats.kotlinlang.org/c/compose-web)
- 이슈: [GitHub](https://github.com/JetBrains/compose-multiplatform/issues)

### 다음 단계

- Kotlin/Wasm 프로젝트 설정 방법 알아보기
- Compose Multiplatform과 함께 사용하기
- JavaScript와의 상호운용성 탐색
