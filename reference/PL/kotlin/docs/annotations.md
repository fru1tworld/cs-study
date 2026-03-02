# Kotlin 어노테이션

## 개요

어노테이션은 코드 요소에 메타데이터를 첨부하는 태그입니다. 도구와 프레임워크는 이 메타데이터를 컴파일 및 런타임에 처리하여 다양한 작업을 수행합니다. 보일러플레이트 코드 생성, 코딩 표준 적용, 문서 작성과 같은 일반적인 작업을 단순화합니다.

---

## 선언

어노테이션은 `annotation` 키워드를 사용하여 선언됩니다:

```kotlin
annotation class Fancy
```

### 메타 어노테이션
추가 속성은 메타 어노테이션을 사용하여 지정할 수 있습니다:

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
