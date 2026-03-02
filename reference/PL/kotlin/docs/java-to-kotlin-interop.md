# Java에서 Kotlin 호출하기

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

`@JvmExposeBoxed`는 상속된 함수에 대해 박싱된 표현을 자동 생성하지 않습니다. 명시적으로 오버라이드하세요:

```kotlin
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class DefaultTransformer : IdTransformer {
    override fun transformId(rawId: UInt): UInt = super.transformId(rawId)
}
```

---

**관련 문서**: [Kotlin에서 Java 호출하기](java-interop.html) | [Kotlin에서 Java 레코드 사용하기](jvm-records.html)
