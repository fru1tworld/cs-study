> 원본: https://rockthejvm.com/articles/scala-3-given-and-using-clauses

# Scala 3: Given과 Using 절

## 이 글의 대상 독자

Scala 3을 처음 배우는 사람이나, implicit 경험이 많지 않은 Scala 2 개발자를 대상으로 한다. implicit에 대한 사전 지식이 있으면 도움이 되지만 필수는 아니다.

## 소개

이 글에서는 implicit 매개변수에 대한 사전 지식 없이도 Scala 3의 given/using 절을 살펴본다. 이 기능은 Scala 3 New Features 강좌에서 상세히 다루고 있다.

## 작은 문제 하나

인구조사 애플리케이션을 만든다고 해보자. `Person` case class를 사용한다:

```scala
case class Person(surname: String, name: String, age: Int)
```

이 인스턴스들에는 표준 정렬 기준이 필요하다. 보통 성(surname)의 알파벳 순서로 정렬한다:

```scala
val personOrdering: Ordering[Person] = new Ordering[Person] {
  override def compare(x: Person, y: Person): Int =
    x.surname.compareTo(y.surname)
}
```

이 정렬 기준을 모든 메서드에 일일이 넘기는 건 꽤 번거롭다:

```scala
def listPeople(persons: Seq[Person])(ordering: Ordering[Person]) = ...
def someOtherMethodRequiringOrdering(alice: Person, bob: Person)(ordering: Ordering[Person]) = ...
def yetAnotherMethodRequiringOrdering(persons: List[Person])(ordering: Ordering[Person]) = ...
```

이 표준 값을 매번 수동으로 전달하는 대신, 컴파일러가 자동으로 주입하도록 할 수 있다.

## Given/Using 절

해결 방법은 두 가지 수정으로 이뤄진다. 먼저, 표준 정렬 기준을 "given"으로 선언한다:

```scala
given personOrdering: Ordering[Person] with {
  override def compare(x: Person, y: Person): Int =
    x.surname.compareTo(y.surname)
}
```

그다음, 자동 주입이 필요한 메서드 매개변수에 `using`을 붙인다:

```scala
def listPeople(persons: Seq[Person])(using ordering: Ordering[Person]) = ...
```

이제 메서드를 호출할 때 정렬 기준 인자를 명시적으로 넘기지 않아도 된다:

```scala
listPeople(List(Person("Weasley", "Ron", 15), Person("Potter", "Harry", 15)))
```

컴파일러가 자동으로 정렬 인스턴스를 주입한다.

## Given 임포트

대규모 애플리케이션에서는 코드가 여러 파일에 걸쳐 있으므로, given 인스턴스를 임포트하는 방법이 필요하다. given이 object 안에 정의되어 있다고 하자:

```scala
object StandardValues {
  given personOrdering: Ordering[Person] with {
    override def compare(x: Person, y: Person): Int = x.surname.compareTo(y.surname)
  }
}
```

given을 다음과 같이 임포트할 수 있다:

```scala
import StandardValues.personOrdering
```

또는 타입으로 임포트할 수도 있다:

```scala
import StandardValues.{given Ordering[Person]}
```

**팁:** 일반 와일드카드 임포트는 given 인스턴스를 포함하지 않는다. 모든 given을 임포트하려면 다음과 같이 작성한다:

```scala
import StandardValues.{given _}
```

## Given 파생

given 인스턴스는 `using` 절을 통해 다른 given 인스턴스에 의존할 수 있다. 예를 들어 `Ordering[T]`로부터 `Ordering[Option[T]]`를 자동으로 만들 수 있다:

```scala
given optionOrdering[T](using normalOrdering: Ordering[T]): Ordering[Option[T]] with {
  def compare(optionA: Option[T], optionB: Option[T]): Int = (a, b) match {
    case (None, None) => 0
    case (None, _) => -1
    case (_, None) => 1
    case (Some(a), Some(b)) => normalOrdering.compare(a, b)
  }
}
```

이 구조는 컴파일러에게 "`Ordering[T]`가 스코프에 존재하면, `Ordering[Option[T]]`를 자동으로 생성하라"고 알려준다. 다음과 같이 호출하면:

```scala
def sortThings[T](things: List[T])(using ordering: Ordering[T]) = ...

val maybePersons: List[Option[Person]] = ...
sortThings(maybePersons)
```

컴파일러가 기존의 `Ordering[Person]`으로부터 `Ordering[Option[Person]]`을 자동으로 구성한다.

## Given이 유용한 곳

given/using 절은 확장 메서드(extension method)와 결합하면 다음을 구현할 수 있다:

- 타입 클래스(Type class)
- 의존성 주입(Dependency Injection)
- 타입별 코드를 위한 컨텍스트 추상화
- 자동 타입 생성
- 타입 수준 프로그래밍

**핵심 제약 사항:** 같은 스코프에 동일 타입의 given 인스턴스가 둘 이상 존재하면 안 된다. 그렇지 않으면 컴파일러가 어떤 것을 주입해야 할지 판단할 수 없다.

철학적으로 보면, given은 타입의 존재를 증명하는 것이다. 컴파일러는 `using` 절에 의존하는 새로운 given 인스턴스를 구성하며, given/using 패턴을 조합해 컴파일 타임에 타입 관계를 증명할 수 있게 해준다.

## 기타 편의 기능

given 인스턴스의 이름이 필요 없을 때는 익명으로 선언할 수 있다:

```scala
given Ordering[Person] {
  override def compare(x: Person, y: Person): Int =
    x.surname.compareTo(y.surname)
}
```

더 간단하게 만들려면 표현식을 사용할 수 있다:

```scala
given personOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.surname.compareTo(b.surname) < 0)
```

이것도 익명으로 만들 수 있다:

```scala
given Ordering[Person] = Ordering.fromLessThan((a, b) => a.surname.compareTo(b.surname) < 0)
```

## 결론

given 구조는 특정 타입의 인스턴스를 자동으로 구성하고, 해당 타입의 using 절이 있는 곳에 자동으로 삽입될 수 있게 한다.
