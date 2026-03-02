# 인터페이스

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
