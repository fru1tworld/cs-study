# 중첩 및 내부 클래스

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

`inner` 키워드로 표시된 중첩 클래스는 **외부 클래스의 멤버에 접근**할 수 있습니다. 내부 클래스는 외부 클래스 객체에 대한 참조를 가지고 있습니다:

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
