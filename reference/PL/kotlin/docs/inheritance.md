# 상속

이 페이지는 Kotlin에서의 상속을 다루며, 클래스가 다른 클래스로부터 어떻게 상속받을 수 있는지와 이 메커니즘을 지배하는 규칙을 설명합니다.

## 개요

모든 Kotlin 클래스는 기본적으로 `Any`를 상속받으며, 이는 세 가지 메서드를 제공합니다:
- `equals()`
- `hashCode()`
- `toString()`

```kotlin
class Example // 암시적으로 Any를 상속
```

## open 키워드

기본적으로 Kotlin 클래스는 **final**이며 상속될 수 없습니다. 클래스를 상속 가능하게 만들려면 `open` 키워드를 사용합니다:

```kotlin
open class Base // 클래스가 상속 가능하도록 열림
```

## 기본 상속

클래스를 상속받으려면 콜론 뒤에 상위 타입을 배치합니다:

```kotlin
open class Base(p: Int)
class Derived(p: Int) : Base(p)
```

## 메서드 오버라이딩

메서드는 오버라이드 가능하도록 명시적으로 `open`으로 표시해야 합니다:

```kotlin
open class Shape {
    open fun draw() { /*...*/ }
    fun fill() { /*...*/ }
}

class Circle() : Shape() {
    override fun draw() { /*...*/ }
}
```

`override`로 표시된 멤버는 기본적으로 자체적으로 open입니다. 추가 오버라이딩을 방지하려면 `final`을 사용합니다:

```kotlin
open class Rectangle() : Shape() {
    final override fun draw() { /*...*/ }
}
```

## 프로퍼티 오버라이딩

프로퍼티도 메서드와 유사하게 작동합니다:

```kotlin
open class Shape {
    open val vertexCount: Int = 0
}

class Rectangle : Shape() {
    override val vertexCount = 4
}
```

`val` 프로퍼티를 `var` 프로퍼티로 오버라이드할 수 있습니다(그 반대는 불가):

```kotlin
interface Shape {
    val vertexCount: Int
}

class Rectangle(override val vertexCount: Int = 4) : Shape
```

## 파생 클래스 초기화 순서

기본 클래스 초기화가 **먼저** 발생하고, 그 다음 파생 클래스 초기화가 이루어집니다. 파생 클래스에서 선언된 프로퍼티는 기본 클래스 생성자가 실행될 때 아직 초기화되지 않았습니다.

```kotlin
open class Base(val name: String) {
    init { println("Initializing base class") }
    open val size: Int = name.length.also {
        println("Initializing size in base class: $it")
    }
}

class Derived(name: String, val lastName: String) : Base(name) {
    init { println("Initializing derived class") }
    override val size: Int = (super.size + lastName.length).also {
        println("Initializing size in derived class: $it")
    }
}
```

## 상위 클래스 구현 호출

상위 클래스 함수와 프로퍼티를 호출하려면 `super` 키워드를 사용합니다:

```kotlin
open class Rectangle {
    open fun draw() { println("Drawing a rectangle") }
    val borderColor: String get() = "black"
}

class FilledRectangle : Rectangle() {
    override fun draw() {
        super.draw()
        println("Filling the rectangle")
    }
}
```

외부 클래스의 상위 클래스에 접근하는 내부 클래스의 경우:

```kotlin
inner class Filler {
    fun drawAndFill() {
        super@FilledRectangle.draw()
    }
}
```

## 오버라이딩 규칙 (다중 상속)

동일한 멤버의 여러 구현을 상속받을 때는 반드시 오버라이드해야 합니다:

```kotlin
open class Rectangle {
    open fun draw() { /*...*/ }
}

interface Polygon {
    fun draw() { /*...*/ }
}

class Square() : Rectangle(), Polygon {
    override fun draw() {
        super<Rectangle>.draw()  // Rectangle의 draw() 호출
        super<Polygon>.draw()    // Polygon의 draw() 호출
    }
}
```

## 부 생성자

주 생성자가 없는 파생 클래스는 부 생성자에서 기본 타입을 초기화해야 합니다:

```kotlin
class MyView : View {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
}
```

## 중요 참고사항

- Kotlin 클래스는 **기본적으로 final**입니다 (안전을 위한 설계 철학)
- 상속/오버라이딩을 허용하려면 명시적으로 `open`을 사용
- `override` 수정자는 오버라이딩 시 **필수**
- 기본 클래스 생성자나 초기화자에서 `open` 멤버 사용을 피해 런타임 오류 방지
