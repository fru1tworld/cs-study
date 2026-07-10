> 원본: https://rockthejvm.com/articles/scala-3-traits

# Scala 3: Trait 빠르게 알아보기

## 소개

이 글에서는 이전 Scala 3 탐구에 이어 Scala 3에서 trait의 새로운 기능을 다룬다.

이 기능은 (그 외 수십 가지 변경사항과 함께) Scala 3 New Features 강좌에서 상세히 다루고 있다.

## 배경

Scala의 trait는 원래 Java의 인터페이스에 대응하는 개념으로 만들어졌다. 본질적으로 trait는 추상 필드와 메서드의 묶음을 정의하는 타입이었다. 시간이 지나면서 trait에는 비추상 필드와 메서드 같은 기능이 추가되었다. 이 때문에 추상 클래스와 trait의 경계가 어디인지에 대한 합리적인 의문이 생겼다.

Scala 3이 등장하면서 그 경계는 더욱 모호해진다.

## Trait 인자

Scala 2에서 추상 클래스와 trait의 실질적인 차이 중 하나는 trait가 생성자 인자를 받을 수 없다는 것이었다. 간단히 말해, 이제는 가능하다:

```scala
trait Talker(subject: String) {
    def talkWith(another: Talker): String
}
```

이런 trait를 확장하는 것은 일반 클래스를 확장하는 것과 똑같이 생겼다:

```scala
class Person(name: String) extends Talker("rock")
```

trait에 매개변수를 추가할 수 있게 된 것은 분명 장점이 있다. 하지만 몇 가지 문제가 생길 수 있다. 첫 번째 문제는, 큰 코드베이스에서(그리고 그런 곳이 아니더라도) 같은 trait를 여러 번 확장하는 일이 드물지 않다는 것이다. 한 곳에서는 어떤 인자로, 다른 곳에서는 다른 인자로 같은 trait를 믹스인하면 어떻게 될까?

짧게 답하면, 컴파일이 안 된다. 규칙은 이렇다: 상위 클래스가 이미 trait에 인자를 전달했다면, 다시 믹스인할 때 해당 trait에 인자를 전달해서는 안 된다.

```scala
class RockFan extends Talker("rock")
class RockFanatic extends RockFan with Talker // 여기서 인자를 전달하면 안 된다
```

다음 문제는, trait 계층을 정의하면 어떻게 되느냐는 것이다. 파생 trait에서 인자를 어떻게 전달해야 할까?

역시 짧게 답하면, 파생 trait는 부모 trait에 인자를 전달하지 않는다:

```scala
trait BrokenRecord extends Talker
```

이것이 규칙이다. 부모 trait에 인자를 전달하면 컴파일되지 않는다.

좋다. 그런데 이 trait를 클래스에 어떻게 믹스인할까? 가족이나 친구 중에 있는, 안색이 창백해질 때까지 말을 멈추지 않는 사람을 나타내는 클래스를 만들고 싶다고 해보자.

```scala
class AnnoyingFriend extends BrokenRecord("politics")
```

이건 안 된다. `BrokenRecord` trait는 인자를 받지 않기 때문이다. 그러면 `Talker` trait에 올바른 인자를 어떻게 전달할까?

답은 다시 믹스인하는 것이다:

```scala
class AnnoyingFriend extends BrokenRecord with Talker("politics")
```

좀 투박하긴 하지만, trait의 이 새로운 기능에서 타입 시스템의 건전성을 보장하려면 이 방법밖에 없다.

## Transparent Trait

Scala 컴파일러의 타입 추론은 가장 강력한 기능 중 하나다. 하지만 정보가 충분하지 않으면 컴파일러의 타입 추론으로도 한계가 있을 수 있다. 예를 들어보자:

```scala
trait Color
case object Red extends Color
case object Green extends Color
case object Blue extends Color

val color = if (43 > 2) Red else Blue
```

`color`의 추론된 타입이 무엇일지 맞춰보자. 스포일러: `Color`가 아니다.

이상하지 않은가? 추론된 타입이 `Red`와 `Blue`의 최소 공통 조상이 될 것이라 기대할 텐데 말이다. 실제 추론된 타입은 `Color with Product with Serializable`다. `Red`와 `Blue` 모두 `Color`를 상속하지만, `case object`이기 때문에 `Product`(Scala)와 `Serializable`(Java) trait를 자동으로 구현한다. 그래서 최소 공통 조상이 세 가지의 조합이 되는 것이다.

문제는 `Product`이나 `Serializable`을 값에 부여하는 독립적인 타입으로 쓰는 경우가 거의 없다는 것이다. 그래서 Scala 3에서는 이런 종류의 trait를 `transparent` trait로 만들어 타입 추론에서 무시할 수 있게 했다. 예를 들어보자. 그래픽 라이브러리에 다음과 같은 정의가 있다고 가정한다:

```scala
trait Paintable
trait Color
object Red extends Color with Paintable
object Green extends Color with Paintable
object Blue extends Color with Paintable
```

> **참고**: 간결함을 위해 `case object`로 만들지 않았다. 이에 대해서는 뒤에서 다시 다룬다.

`Paintable` trait가 독립적인 타입으로는 거의 쓰이지 않고, 라이브러리 정의에서 보조 trait로만 사용된다고 해보자. 이 경우 다음과 같이 쓰면

```scala
val color = if (43 > 2) Red else Blue
```

`color`의 타입이 `Color with Paintable`이 아니라 `Color`로 추론되길 원할 것이다. `Paintable`을 타입 추론에서 제외하려면 `transparent`로 표시하면 된다:

```scala
transparent trait Paintable
```

그러면 변수 `color`의 타입이 `Color`로 표시되는 것을 확인할 수 있다.

Scala 3이 출시되면 `Product`, `Comparable`(Java), `Serializable`(Java) trait는 Scala 컴파일러에서 자동으로 transparent trait로 취급된다. 물론 특정 타입을 명시적으로 지정하면 transparent trait가 타입 검사에 영향을 주지 않는다.

## 결론

Scala 3의 trait에 관한 두 가지 새 기능을 살펴보았다. Scala 3이 나오면 잘 활용해보자!
