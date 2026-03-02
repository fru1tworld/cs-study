# Kotlin에서 Java 호출하기

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
