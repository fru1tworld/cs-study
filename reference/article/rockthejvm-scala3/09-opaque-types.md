> 원본: https://rockthejvm.com/articles/scala-3-opaque-types

# Scala 3: Opaque Type 빠르게 알아보기

## 들어가며

이 글은 Scala 3 시리즈의 일부다. 기본적인 클래스, 메서드, 타입 별칭 정의 등 Scala(버전 2) 기초에 익숙하다고 가정한다.

이 글에서는 작지만 흥미로운 기능인 opaque type을 다룬다.

## 배경과 동기

기존 타입을 감싸서(composition) 새로운 타입을 정의하는 일은 흔하다. 그런데 많은 경우 이런 방식은 필드 접근, 메서드 호출, 타입 조합 과정에서 어느 정도의 오버헤드를 수반한다.

예를 들어보자. 소셜 네트워크를 개발하고 있다고 하자. 사용자 정보는 가장 기본적인 데이터 중 하나인데, 정보가 올바른지 검증하는 규칙을 두고 싶다. 가령 이름이 대문자로 시작해야 한다는 규칙을 적용한다고 하자(모든 언어에 해당하는 건 아니지만, 예시를 위해 이렇게 가정한다).

```scala
case class Name(value: String) {
  // some logic here
}
```

이건 String을 단순히 감싼 래퍼다. refined type 같은 방법을 쓸 수도 있지만, 어떤 방식을 택하든 이 새로운 `Name` 타입은 일정한 오버헤드를 수반한다. 사용자가 수백만 명이면, 이런 사소한 오버헤드도 누적된다.

## Opaque Type 등장

이름은 결국 문자열이다. 하지만 추가 로직을 붙이려면 문자열을 감싸거나, 컴파일러에 추가 검사를 시키는 수밖에 없어서 오버헤드가 생긴다. Opaque type을 쓰면 `Name`을 String "그 자체"로 정의하면서도 기능을 붙일 수 있다.

```scala
object SocialNetwork {
    opaque type Name = String
}
```

타입 별칭을 정의한 것으로, 정의된 스코프 안에서는 String과 자유롭게 교환하여 사용할 수 있다.

## Opaque Type의 API 정의

Opaque type의 장점은 클래스나 트레이트처럼 독립적인 타입으로 취급할 수 있다는 점이다. 즉, 컴패니언 객체를 정의할 수 있다.

```scala
object SocialNetwork {
  opaque type Name = String

  object Name {
    def fromString(s: String): Option[Name] =
    if (s.isEmpty || s.charAt(0).isLower) None else Some(s) // simplified
  }
}
```

핵심은, 정의된 스코프 안에서만 String과 교환할 수 있고 외부에서는 `Name`이 실제로 String이라는 사실을 전혀 알 수 없다는 것이다. 외부 스코프에서 `Name`은 자체 API를 가진(현재는 없지만) 완전히 다른 타입이다. 기존 타입(String)으로 구현되지만 보일러플레이트나 오버헤드 없이 새로운 타입을 만들 수 있다. 반면, 이 새 타입은 구현에 사용된 타입과 아무런 연결이 없는 것으로 취급된다. 아래 코드는 컴파일되지 않는다.

```scala
val name: Name = "Daniel" // expected Name, got String
```

이렇게 되면 좋은 소식과 나쁜 소식이 있다. 나쁜 소식은 이 새 타입이 자체 API가 없다는 점이다. String으로 구현되어 있더라도 String의 메서드에 접근할 수 없다. 좋은 소식은 오버헤드 없는 새로운 타입을 얻었으니, API를 처음부터 직접 만들 수 있다는 점이다.

새 타입의 API는 extension method로 정의해야 한다. extension method는 별도 글에서 다루겠지만, 구조는 다음과 같다.

```scala
// still within the SocialNetwork scope where Name is defined
extension (n: Name) {
  def length: Int = n.length
}
```

기본적인 "정적" API(컴패니언 객체)와 "비정적" API(extension method)를 정의했으니, 이제 새 타입을 사용할 수 있다.

```scala
val name: Option[Name] = Name.fromString("Daniel")
val nameLength = name.map(_.length)
```

## 타입 바운드가 있는 Opaque Type

Opaque type 정의에는 일반 타입 별칭처럼 타입 제한을 걸 수 있다. 그래픽 라이브러리를 개발하면서 색상을 다룬다고 해보자.

```scala
object Graphics {
  opaque type Color = Int // in hex
  opaque type ColorFilter <: Color = Int

  val Red: Color = 0xff000000
  val Green: Color = 0xff0000
  val Blue: Color = 0xff0000
  val halfTransparency: ColorFilter = 0x88
}
```

`Color`와 `ColorFilter`는 클래스 계층에서의 치환(substitution)과 동일한 방식으로 사용할 수 있다.

```scala
import Graphics._
case class Overlay(c: Color)

// ok, because ColorFilter "extends" Color
val fadeLayer = Overlay(halfTransparency)
```

## 마무리

Scala 3의 새로운 도구를 하나 배웠다. 별칭이 정의된 스코프 밖에서 기반 타입의 API를 사용할 수 없다는 단점이 있지만, 기존 타입을 활용해 오버헤드 없이 표현할 수 있는 범위가 훨씬 넓어진다.
