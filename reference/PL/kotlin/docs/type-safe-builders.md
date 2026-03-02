# 타입 안전 빌더

## 개요

Kotlin의 타입 안전 빌더는 잘 명명된 함수와 **수신자가 있는 함수 리터럴**을 결합하여 **도메인 특화 언어(DSL)**를 만들 수 있게 합니다. 반선언적 방식으로 복잡한 계층적 데이터 구조를 구축할 수 있습니다.

### 사용 사례

- 마크업 생성 (HTML, XML)
- 웹 서버 라우트 구성 (예: Ktor)

## 작동 방식

### 기본 개념

타입 안전 빌더가 활용하는 것:
1. **수신자가 있는 함수 리터럴** - 특정 타입에서 동작하는 람다 전달
2. **확장 함수** - 태그 클래스에 기능 추가
3. **암시적 수신자** - 명시적 `this` 없이 메서드 호출

### 예제: HTML 빌더

```kotlin
val result = html {
    head {
        title { +"HTML encoding with Kotlin" }
    }
    body {
        h1 { +"HTML encoding with Kotlin" }
        p { +"this format can be used as an alternative markup to HTML" }
        a(href = "http://kotlinlang.org") { +"Kotlin" }
        p {
            +"This is some"
            b { +"mixed" }
            +"text. For more see the"
            a(href = "http://kotlinlang.org") { +"Kotlin" }
            +"project"
        }
        p {
            +"some text"
            ul {
                for (i in 1..5) li { +"${i}*2 = ${i*2}" }
            }
        }
    }
}
```

### 핵심 함수 패턴

```kotlin
fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()  // 람다로 초기화
    return html
}
```

매개변수 타입 `HTML.() -> Unit`은 **수신자가 있는 함수 타입**입니다:
- 수신자: `HTML` 인스턴스
- 반환: `Unit`
- 암시적 `this` 또는 생략을 통해 접근

### 자식 태그 함수

```kotlin
protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
    tag.init()
    children.add(tag)
    return tag
}

fun head(init: Head.() -> Unit) = initTag(Head(), init)
fun body(init: Body.() -> Unit) = initTag(Body(), init)
```

### 텍스트 내용 추가

단항 플러스 연산자(`+`)가 문자열을 래핑합니다:

```kotlin
abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}
```

사용:
```kotlin
title { +"XML encoding with Kotlin" }
```

## 스코프 제어: @DslMarker

### 문제

스코프 제어 없이 중첩된 DSL 컨텍스트는 외부 수신자 메서드 호출을 허용합니다:

```kotlin
html {
    head {
        head { }  // 금지되어야 함!
    }
}
```

### 해결책: @DslMarker 어노테이션

마커 어노테이션 생성:

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker
```

기본 클래스에 적용:

```kotlin
@HtmlTagMarker
abstract class Tag(val name: String) : Element { ... }
```

### 효과

컴파일러가 스코프를 가장 가까운 암시적 수신자로 제한합니다:

```kotlin
html {
    head {
        head { }  // 오류: 외부 수신자의 멤버
    }
}
```

### 명시적 접근

외부 수신자에 접근하려면 한정된 `this`를 사용합니다:

```kotlin
html {
    head {
        this@html.head { }  // 허용됨
    }
}
```

### 함수 타입에 적용

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker

fun html(init: @HtmlTagMarker HTML.() -> Unit): HTML { ... }
fun HTML.head(init: @HtmlTagMarker Head.() -> Unit): Head { ... }
```

## 완전한 패키지 정의

```kotlin
package com.example.html

interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker

@HtmlTagMarker
abstract class Tag(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, indent + " ")
        }
        builder.append("$indent</$name>\n")
    }

    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

class HTML : TagWithText("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)
    fun body(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title : TagWithText("title")

abstract class BodyTag(name: String) : TagWithText(name) {
    fun b(init: B.() -> Unit) = initTag(B(), init)
    fun p(init: P.() -> Unit) = initTag(P(), init)
    fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }
}

class Body : BodyTag("body")
class B : BodyTag("b")
class P : BodyTag("p")
class H1 : BodyTag("h1")

class A : BodyTag("a") {
    var href: String
        get() = attributes["href"]!!
        set(value) { attributes["href"] = value }
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}
```

## 관련 주제

- [this 표현식](lambdas.md)
- [빌더 타입 추론과 함께 빌더 사용](lambdas.md)
