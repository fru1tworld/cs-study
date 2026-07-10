> 원본: https://rockthejvm.com/articles/scala-3-extension-methods

# Scala 3: Extension Methods 빠르게 알아보기

## 목차

- [대상 독자](#대상-독자)
- [소개](#소개)
- [사전 지식](#사전-지식)
- [배경](#배경)
- [본격적인 Extension](#본격적인-extension)
- [제네릭 Extension](#제네릭-extension)
- [Given과 함께 쓰는 Extension](#given과-함께-쓰는-extension)
- [결론](#결론)

## 대상 독자

Scala 2에서 Scala 3으로 넘어가려는 분들을 위한 글이다.

## 소개

이번 글에서는 차기 버전 언어의 가장 흥미로운 기능 중 하나인 extension method를 살펴본다.

## 사전 지식

선행 조건으로 두 가지가 중요하다.

- implicit이 어떻게 동작하는지
- [given/using 조합이 어떻게 동작하는지](https://rockthejvm.com/articles/scala-3-given-and-using-clauses)

이 기능은 (수십 가지 다른 변경 사항과 함께) [Scala 3 New Features](https://rockthejvm.com/courses/scala-3-new-features) 과정에서 깊이 있게 다룬다.

## 배경

Scala 2에는 이미 다른 곳에서 정의되어 수정할 수 없는 타입(String이나 Int 같은)에 메서드를 추가하는 개념이 있었다. 이 기법을 "type enrichment"라고 불렀는데, 좀 밋밋해서 사람들이 좀 더 재치 있게 "pimping"이라는 표현을 만들어냈다. 다만 이것도 속어에 가까워서 보편적으로 쓰이는 용어는 "extension methods"다. 실제로 하는 일을 그대로 표현하기 때문이다.

Scala 2에서는 implicit class를 통해 기존 타입에 extension method를 추가할 수 있다. 다음과 같은 case class가 있다고 하자.

```scala
case class Person(name: String) {
    def greet: String = s"Hi, I'm $name, nice to meet you."
}
```

이 경우 다음 implicit class를 작성하면

```scala
implicit class PersonLike(string: String) {
    def greet: String = Person(string).greet
}
```

String에 `greet` 메서드를 호출할 수 있게 된다.

```scala
"Daniel".greet
```

이 코드가 컴파일되는 이유는 컴파일러가 위 라인을 내부적으로 다음과 같이 변환하기 때문이다.

```scala
new PersonLike("Daniel").greet
```

다시 말해 `greet` 메서드는 String 타입 자체를 전혀 건드리지 않으면서도 String 타입의 확장이 되는 셈이다.

[Cats](https://typelevel.org/cats) 같은 라이브러리(사이트에서 [강의](https://rockthejvm.com/courses/cats)도 하고 있다)가 이 패턴을 상시 활용한다.

## 본격적인 Extension

Scala 3에서 `implicit` 키워드는 완전히 지원되기는 하지만(이번 버전 한정) 점차 대체되고 있다.

- `implicit` 값과 인자는 [`given`/`using`](https://rockthejvm.com/articles/scala-3-given-and-using-clauses) 절로 대체된다. [implicit과의 비교](https://rockthejvm.com/articles/scala-3-givens-vs-implicits)와 [implicit과의 병행 사용법](https://rockthejvm.com/articles/scala-3-givens-and-implicits)도 참고하자.
- `implicit` def(변환용)는 [명시적 변환으로 대체](https://rockthejvm.com/articles/scala-3-givens-vs-implicits/#implicit-conversions)된다.
- `implicit` class는 본격적인 extension method로 대체된다. 이 글의 핵심 주제다.

그러면 extension method는 어떻게 선언할까?

문자열에 `greet`라는 (사람처럼 행동하는) 추가 메서드를 붙이는 시나리오에서, 명시적으로 `extension` 절을 작성할 수 있다.

```scala
extension (str: String)
    def greet: String = Person(str).greet
```

이제 이전과 동일하게 `greet` 메서드를 호출할 수 있다.

```scala
"Daniel".greet
```

## 제네릭 Extension

implicit class와 마찬가지로 extension method도 제네릭하게 쓸 수 있다. 누군가 다음과 같은 이진 트리 자료 구조를 작성했다고 하자.

```scala
sealed abstract class Tree[+A]
case class Leaf[+A](value: A) extends Tree[A]
case class Branch[+A](left: Tree[A], right: Tree[A]) extends Tree[A]
```

소스 코드에 접근할 수 없다고 가정하자. 한편 리스트에서 흔히 쓰는 메서드 몇 가지를 추가하고 싶다. 예를 들어 `filter`가 있으면 좋겠다. 다음과 같이 작성할 수 있다.

```scala
extension [A](tree: Tree[A])
  def filter(predicate: A => Boolean): Boolean = tree match {
    case Leaf(value) => predicate(value)
    case Branch(left, right) => left.filter(predicate) || right.filter(predicate)
  }
```

이 메서드는 제네릭이므로 어떤 `Tree[T]`에든 "붙일" 수 있다.

더 나아가 메서드 자체도 제네릭할 수 있다. 트리에 `map` 메서드를 작성하는 방법을 보자.

```scala
extension [A](tree: Tree[A])
  def map[B](func: A => B): Tree[B] = tree match {
    case Leaf(value) => Leaf(func(value))
    case Branch(left, right) => Branch(left.map(func), right.map(func))
  }
```

참고로 두 extension method를 하나의 `extension` 절 아래 묶을 수도 있다.

```scala
extension [A] (tree: Tree[A]) {
  def filter(predicate: A => Boolean): Boolean = tree match {
    case Leaf(value) => predicate(value)
    case Branch(left, right) => left.filter(predicate) || right.filter(predicate)
  }

  def map[B](func: A => B): Tree[B] = tree match {
    case Leaf(value) => Leaf(func(value))
    case Branch(left, right) => Branch(left.map(func), right.map(func))
  }
}
```

(여기서는 중괄호를 사용했지만, [들여쓰기 영역](https://rockthejvm.com/articles/scala-3-indentation)도 똑같이 동작한다.)

## Given과 함께 쓰는 Extension

좀 더 정확히 말하면 `using` 절과 함께 쓰는 경우다.

타입 인자가 수치형(numeric)일 때, 즉 스코프에 `given Numeric[A]`가 있을 때 이진 트리 자료 구조에 `sum` 메서드를 붙이는 방법을 살펴보자.

```scala
extension [A](tree: Tree[A])(using numeric: Numeric[A]) {
  def sum: A = tree match {
    case Leaf(value) => value
    case Branch(left, right) => numeric.plus(left.sum, right.sum)
  }
}
```

이제 `Tree[Int]`를 만들고 `sum` 메서드를 안전하게 호출할 수 있다.

```scala
val tree = Branch(Leaf(1), Leaf(2))
val three = tree.sum
```

`using` 절은 `extension` 절에 둘 수도 있고, 메서드 시그니처 자체에 둘 수도 있다.

```scala
// 동일하게 동작한다
extension [A](tree: Tree[A]) {
  def sum(using numeric: Numeric[A]): A = tree match {
    case Leaf(value) => value
    case Branch(left, right) => numeric.plus(left.sum, right.sum)
  }
}
```

양쪽 모두에 두는 것도 가능하다.

## 결론

이 글에서는 `extension` 메서드의 메커니즘을 분석해 보았다. 이 기능은 given/using 조합과 결합하면 [타입 클래스](https://rockthejvm.com/articles/why-are-scala-type-classes-useful), DSL 등 강력한 추상화를 가능하게 한다.

Scala 3은 정말 잘 만들어질 거라고 확신한다. [여기저기](https://rockthejvm.com/articles/scala-3-indentation) 논란이 있을 수는 있고, 3칸 들여쓰기가 "관례"가 된다면 절대 따르지 않겠지만, 전체적으로 Scala는 더 성숙하고 표현력 있으며, 읽고 쓰기 쉽고 재미있는 언어가 되어가고 있다. 언어라면 마땅히 그래야 한다.
