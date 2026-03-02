# 위임

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
