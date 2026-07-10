> 원본: https://rockthejvm.com/articles/scala-3-general-type-projections

# Scala 3: 일반 타입 프로젝션

## 소개

이 글에서는 Scala 3에서 제거된 일반 타입 프로젝션(general type projection)을 살펴본다. 새로운 기능을 소개하는 대신, 무엇이 제거되었고 왜 제거되었는지에 초점을 맞춘다. "일반 타입 프로젝션은 건전하지 않다(unsound)"가 무슨 뜻인지, 그리고 이 기능이 제거된 이유를 설명한다. 이 내용은 Scala 3 New Features 강좌에서도 상세히 다루고 있다.

## 배경

Scala에서는 클래스, 오브젝트, 트레이트 안에 다른 클래스, 오브젝트, 트레이트를 정의할 수 있다:

```scala
class Outer {
  class Inner
}
```

`Outer`의 각 인스턴스는 서로 다른 `Inner` 타입을 만들어 낸다:

```scala
val o1 = new Outer
val o2 = new Outer
val i1 = new o1.Inner
val i2 = new o2.Inner
```

`o1.Inner`와 `o2.Inner`는 완전히 별개의 타입이다:

```scala
val i3: o1.Inner = new o2.Inner // 컴파일 오류 (타입 불일치)
```

그런데 가능한 모든 `o.Inner` 타입은 `Outer#Inner`라는 일반 타입의 하위 타입이다. 이것이 바로 "타입 프로젝션"이며, Scala의 강력한 기능 중 하나다.

Scala 2에서는 추상 타입을 기반으로 타입 프로젝션을 사용할 수 있었다. 컴파일러가 `A <: Outer`라는 사실만 알고 있는 상황에서도 `A#Inner` 같은 표현이 가능했다. 이런 "일반(general)" 또는 "추상(abstract)" 타입 프로젝션에서 `A`는 구체적인 타입이 아니라 추상 타입이다.

추상 타입 프로젝션은 Scala 2의 타입 수준 프로그래밍에서 컴파일 타임 타입 결정을 강제하는 데 활용되었고, 컴파일 타임 타입 정렬 같은 기법을 구현할 수 있게 해주었다. 하지만 이 기능에는 근본적인 건전성 문제가 있었다.

Martin Odersky가 이 문제를 처음 지적하면서, 일반 타입 프로젝션이 컴파일되어서는 안 되는 코드를 컴파일되게 만들어 런타임 오류를 유발할 수 있다는 예시를 보여주었다. 그 특정 예시는 나중에 2.13에서 수정되었지만, 이 기능을 제거해야 한다는 전반적인 논거는 여전히 유효했다.

## 일반 타입 프로젝션 활용

타입 안전한 데이터베이스 필드 라이브러리를 만든다고 해보자. 식별자를 가진 데이터베이스 항목을 표현하는 일반 타입을 정의한다:

```scala
trait ItemLike {
  type Key
}

trait Item[K] extends ItemLike {
  type Key = K
}
```

`Item`의 `Key` 타입은 제네릭 인자 `K`와 정확히 일치한다. `Key`를 받아서 해당 항목 타입을 반환하는 `get` 메서드를 만든다:

```scala
class StringItem extends Item[String]
class IntItem extends Item[Int]
```

사용 패턴은 다음과 같다:

```scala
get[IntItem](42)       // 정상, IntItem을 반환
get[StringItem]("Scala") // 정상, StringItem을 반환
get[StringItem](55)    // 비정상, 컴파일되면 안 됨
```

이를 위한 시그니처에 일반 타입 프로젝션을 사용한다:

```scala
def get[I <: ItemLike](key: I#Key): I = ???
```

의도한 예시들에 대해 이 코드는 정상적으로 컴파일된다.

## 컴파일되지만 깨지는 코드

여기에 문제가 되는 타입을 추가해 보자:

```scala
trait ItemAll extends ItemLike {
  override type Key >: Any
}

trait ItemNothing extends ItemLike {
  override type Key <: Nothing
}
```

이 바운드는 말이 안 된다. `Any`의 상위 타입도, `Nothing`의 하위 타입도 존재하지 않기 때문이다. 컴파일러는 이런 "잘못된 바운드"를 구체 클래스에서 조건이 맞기만 하면 허용한다. 하지만 실제로 둘 다 확장하는 클래스는 만들 수 없다:

```scala
class ItemWeird extends ItemAll with ItemNothing // 컴파일 안 됨
```

그런데 `ItemAll with ItemNothing`이라는 타입 자체를 쓰는 것은 막을 수 없다. 일반 타입 프로젝션과 결합하면 문제가 되는 구조를 만들 수 있다:

```scala
def funcAll[I <: ItemAll]: Any => I#Key = x => x
```

`I <: ItemAll`이므로 `I#Key >: Any`가 보장되어, 항등 함수가 합법적이 된다. 이 코드는 컴파일된다.

마찬가지로:

```scala
def funcNothing[I <: ItemNothing]: I#Key => Nothing = x => x
```

`I <: ItemNothing`이므로 `I#Key <: Nothing`이라는 사실에서 항등 함수가 성립한다.

이제 위험한 부분이다:

```scala
def funcWeird[I <: ItemAll with ItemNothing]: Any => Nothing =
  funcAll[I].andThen(funcNothing[I])
```

이 코드는 의미가 없는데도 컴파일된다:
- `I <: ItemAll`이므로 `funcAll[I]`을 호출할 수 있다 (타입: `Any => I#Key`)
- `I <: ItemNothing`이므로 `funcNothing[I]`을 호출할 수 있다 (타입: `I#Key => Nothing`)
- 반환 타입과 인자 타입이 일치하므로 체이닝이 가능하다

결과적으로 컴파일은 되지만 의미 없는 코드가 만들어진다. 컴파일러가 바운드 검사를 완전히 건너뛴 것이다.

겉보기에 정상적인 코드를 실행하면:

```scala
val anInt: Int = funcWeird("Scala")
println(anInt + 1)
```

다음과 같은 결과가 나온다:

```
Exception in thread "main" java.lang.ClassCastException: class java.lang.String
cannot be cast to class scala.runtime.Nothing$
(java.lang.String is in module java.base of loader 'bootstrap';
scala.runtime.Nothing$ is in unnamed module of loader 'app')
  at scala.Function1.$anonfun$andThen$1(Function1.scala:85)
```

항등 함수의 타입이 `Any => Nothing`이 되었으므로 String을 `Nothing`으로 변환해야 하는데, 런타임에서는 불가능하다.

## 결론

문제의 핵심은 `I#Key` 같은 일반 타입 프로젝션에 있다. 컴파일러가 추상 타입 프로젝션을 허용하다 보니, "`I`가 추상 타입이기 때문에 `I`의 `Key` 멤버에 대한 바운드 호환성 검사를 수행할 수 없다." 이 때문에 건전하지 않은 코드가 컴파일되고 런타임에 실패하는 코너 케이스가 발생한다.

"일반 타입 프로젝션은 건전하지 않다(unsound)"는 말은, 이 기능이 컴파일되어서는 안 되는 코드를 컴파일되게 허용하여 런타임 오류를 유발하는 상황을 만든다는 뜻이다. Scala 3에서는 이런 건전성 위반과 그에 따른 함정을 없애기 위해 이 기능을 제거했다.
