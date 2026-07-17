> 원본: https://rockthejvm.com/articles/scala-3-type-level-programming

# Scala 3: 타입 레벨 프로그래밍

## 소개

Scala 3에는 타입 레벨 프로그래밍을 단순화하고 강화하기 위한 기능이 다양하게 갖춰져 있습니다. match type, inline, 다양한 컴파일 타임 연산 등 그 목록은 계속 이어집니다. 이렇게 새로운 기능이 많다 보니, 실제 문제를 타입 레벨 기법으로 풀려고 할 때 어디서부터 시작해야 할지 길을 잃기 쉽습니다.

이 글에서는 그 간극을 메우는 것이 목표입니다. Scala 3이 제공하는 새로운 타입 레벨 기능 여러 가지를 활용해 구체적인 문제를 단계별로 풀어 보겠습니다. 바로 시작합시다!

### 셋업

이 글의 셋업은 [이 파일](https://github.com/ncreep/scala3-flat-json-blog/blob/master/setup.scala)에서 확인할 수 있으며, [Scala CLI](https://scala-cli.virtuslab.org/) 도구로 따라 해볼 수 있습니다.

저장소를 클론한 뒤, 저장소 폴더에서 다음 명령어를 실행하면 필요한 의존성이 포함된 REPL이 시작됩니다.

```scala
scala-cli console .
```

### 문제 정의

우리가 풀 문제는 실제로 제가 업무에서 겪었던 사례에서 영감을 받은 것입니다. 그래서 여러분 자신의 작업에도 충분히 실용적일 것입니다.

다음과 같은 데이터 모델[1](#user-content-fn-badmodel)이 있고, 이를 JSON으로 저장해야 한다고 가정해 봅시다.

```scala
import io.circe.*
import io.circe.generic.semiauto

case class User(email: Email, phone: Phone, address: Address)

case class Email(primary: String, secondary: Option[String])

case class Phone(number: String, prefix: Int)

case class Address(country: String, city: String)

object Email:
  given Codec.AsObject[Email] = semiauto.deriveCodec[Email]

object Phone:
  given Codec.AsObject[Phone] = semiauto.deriveCodec[Phone]

object Address:
  given Codec.AsObject[Address] = semiauto.deriveCodec[Address]
```

그런데 이 데이터를 아무 형식으로나 저장할 수는 없고, 반드시 "플랫(flat)"해야 합니다. 구체적으로 말하면, 최상위 클래스의 각 필드가 생성되는 최상위 JSON 안에 인라인되어야 합니다[2](#user-content-fn-whyflat).

다음 데이터가 있다고 가정합시다.

```scala
val user = User(
  Email(
    primary = "bilbo@baggins.com",
    secondary = Some("frodo@baggins.com")),
  Phone(number = "555-555-00", prefix = 88),
  Address(country = "Shire", city = "Hobbiton"))
```

결과 JSON은 다음과 같아야 합니다.

```scala
{
  "primary": "bilbo@baggins.com",
  "secondary": "frodo@baggins.com",
  "number": "555-555-00",
  "prefix": 88,
  "country": "Shire",
  "city": "Hobbiton"
}
```

최상위 필드가 사라지고, 그 안에 담겨 있던 데이터가 최상위 레벨에 인라인된 것을 볼 수 있습니다[3](#user-content-fn-goodidea).

이 문제를 런타임에서 JSON을 조작하는 방식으로 풀기는 어렵지 않을 것입니다. 하지만 그것만으로는 부족한 이유가 최소 두 가지 있습니다. 첫째, 불필요한 중간 JSON 구조를 만드는 데 따른 런타임 비용이 발생합니다. 둘째, 그리고 더 중요한 것은, 우리는 가능한 한 컴파일 타임에 안전하기를 원합니다. 사용자가 프로덕션에 배포한 뒤에야 사소한 실수를 발견하는 일은 피하고 싶습니다. 여기서 안전성이란 다음을 의미합니다.

- 최상위 클래스에 원시 타입 필드가 있으면 - 컴파일 실패
- 인라인되는 클래스들의 필드 이름이 중복되면 - 컴파일 실패

이것이 왜 실제 안전성 문제인지는 나중에 살펴보겠지만, 지금은 이 조건들이 컴파일 타임 기법을 사용하도록 강제한다는 점만 알면 됩니다[4](#user-content-fn-manually).

크게 보면, Scala에는 두 종류의 컴파일 타임 기법이 있습니다. 타입 레벨 프로그래밍과 매크로입니다. 플랫 JSON 문제를 매크로로도 풀 수 있겠지만, 여기서는 그렇게 하지 않겠습니다. 매크로는 강력하지만 매우 "일회성" 솔루션이 되기 쉽습니다. 재사용 가능한 구성 요소로 이루어지기보다는 특정 문제에 맞춤 제작되는 경향이 있습니다. 반면 타입 레벨 기법은 대체로 기존에 있는 재사용 가능한 언어 기능으로 구성됩니다. 우리가 배울 타입 레벨 기능 하나하나가 다른 맥락에서도 재사용할 수 있는 새로운 도구가 될 수 있습니다. 따라서 이 글에서는 매크로로 자체적인 기능을 만들어내는 대신, 기존 언어 기능만 사용하는 타입 레벨 프로그래밍에 집중하겠습니다[5](#user-content-fn-otherlibraries).

한 가지 양해를 구하자면, 이 글을 쓰는 시점에서 Scala 3의 새로운 타입 레벨 기능 중 일부는 아직 거친 부분이 있어서, 모든 것이 순탄하게 진행되지는 않을 것입니다[6](#user-content-fn-issues). 우리의 정신 건강에 최소한의 타격만 입고 무사히 빠져나올 수 있기를 바랍니다. 타입 레벨 프로그래밍과 매크로 사이에서 고민 중이라면, 매크로가 덜 고통스러운 선택일 수 있습니다(역시 글을 쓰는 시점 기준이며, 상황은 계속 개선되고 있습니다).

Scala 2에서는 타입 레벨 프로그래밍이 알아보기 힘든 컴파일 에러를 만들어내는 것으로 악명이 높았습니다. 다행히 Scala 3에서는 커스텀 에러 메시지를 제공할 수 있는 도구가 추가되었습니다. 사용자에게 좋은 사용 경험을 주고 싶으니, 문제에 조건을 하나 더 추가하겠습니다. 컴파일에 실패할 경우 읽기 쉬운 컴파일 에러를 제공하는 것입니다.

정리하면, 우리가 할 일은 다음과 같습니다.

1. 클래스가 주어졌을 때
2. 플랫 형식의 JSON 직렬화기/역직렬화기를 제공한다
3. 클래스가 안전성 기준을 충족하지 못하면 컴파일 실패시킨다
4. 매크로는 사용하지 않는다
5. 합리적으로 유용한 컴파일 에러 메시지를 제공한다

미리 경고하자면, 이 글은 꽤 길고 다양한 부분으로 구성되어 있습니다. 파트별로 천천히 읽어 주세요. 한 번에 다 읽으면 부담스러울 수 있습니다.

그러면 이제 구체적인 클래스의 플랫 직렬화부터 시작하겠습니다. 이렇게 하면 처음부터 깊이 들어가지 않고도 타입 레벨 프로그래밍을 조금씩 맛볼 수 있습니다.

## 구체적인 클래스 직렬화

### 의사 코드

JSON을 다루게 되므로, JSON 작업을 대신해 줄 JSON 라이브러리가 필요합니다. [circe](https://github.com/circe/circe)를 사용하겠지만, 다른 Scala JSON 라이브러리(선택지가 풍부합니다)를 써도 무방합니다. circe의 내부 동작을 상세히 설명하지는 않겠으며, 작성하는 JSON 코드가 충분히 자명하기를 바랍니다.

이 파트의 목표는 `User` 클래스의 인스턴스를 받아 플랫 JSON으로 직렬화하는 것입니다. circe 용어로 말하면, `User` 타입의 `Encoder`가 필요합니다.

대부분의 언어에서 메타프로그래밍은 일반 코드만큼 읽기 쉽지 않기 때문에, 메타프로그래밍 문제를 다룰 때는 먼저 의사 코드로 솔루션을 스케치하는 것이 도움이 됩니다. JSON `Encoder`의 의사 코드를 작성해 봅시다.

1. `User` 인스턴스가 주어지면
2. 클래스의 각 필드에 대해
3. 해당 필드 타입에 맞는 `Encoder`를 찾는다
4. 필드를 JSON 객체로 변환한다
5. 모든 JSON 객체를 하나로 합친다

간결하고 명확합니다.

### 리스트와 튜플

의사 코드에서는 클래스의 필드를 리스트처럼, 즉 순회할 수 있는 것처럼 다루고 있습니다. 물론 이것은 단순화입니다. 클래스의 필드는 각각 다른 타입을 가질 수 있지만, Scala의 리스트는 단일 (정적으로 알려진) 타입만 담을 수 있기 때문입니다.

다음과 같이 시도해 보면,

```scala
val fields = user.productIterator
```

`fields`의 타입은 별로 유용하지 않은 `Iterator[Any]`가 됩니다. 모든 것이 컴파일 타임에 알려져야 하므로 이는 우리 목적에 맞지 않습니다. 그래야만 필드에 맞는 올바른 `Encoder`를 찾을 수 있습니다. `Encoder`(그리고 일반적으로 implicit)는 값의 정적으로 알려진 타입으로 결정된다는 점을 기억하세요.

이 문제를 해결하려면 우리가 사용하는 케이스 클래스 정의를 조금 다른 눈으로 바라보면 됩니다. 케이스 클래스의 필드는 일종의 튜플과 비슷합니다. 즉 다음 클래스는,

```scala
case class User(email: Email, phone: Phone, address: Address)
```

(필드 이름을 제외하면) 다음 튜플 타입과 동등합니다.

```scala
(Email, Phone, Address)
```

한 걸음 더 나아가면, 튜플은 리스트와 비슷하되 원소의 타입이 서로 다를 수 있다(이종heterogeneous)는 점이 핵심적인 차이입니다. 이 모든 것을 깔끔하게 묶어주기 위해, Scala 3은 튜플에 리스트와 유사한 연산을 직접 제공합니다[7](#user-content-fn-hlist). 물론 튜플은 실제 리스트가 아니므로 다루는 방식은 다르겠지만, 앞으로 튜플을 창의적으로 조작할 때 리스트와의 유사성을 염두에 두세요.

`Tuple` 컴패니언 객체에는 케이스 클래스를 대응하는 튜플로 변환해 주는 편리한 함수가 있습니다.

```scala
scala> val fields = Tuple.fromProductTyped(user)
val fields: (Email, Phone, Address) = (
  Email(bilbo@baggins.com,Some(frodo@baggins.com)),
  Phone(555-555-00,88),
  Address(Shire,Hobbiton))
```

`Tuple.fromProductTyped`를 사용하면 `User` 값을 타입 정보를 잃지 않고 대응하는 튜플 타입으로 변환할 수 있습니다. 이제 튜플을 리스트처럼 다루는 방법을 배우기만 하면 됩니다.

### 튜플 순회

그렇다면 튜플을 어떻게 순회할까요?

실제 리스트를 다루고 있었다면 `map` 함수가 우리 문제에 딱 맞았을 것입니다.

Scala 3의 새로운 튜플에도 `map` 함수가 있어서 원하는 바로 그것처럼 보입니다. 제가 쓰고 싶은 코드는 이렇습니다.

```scala
fields.map: field =>
  val encoder = summon[Encoder]

  val json = encoder(field)

  json
```

안타깝게도 이 코드는 여러 이유로 작동하지 않습니다.

이제부터 코드 두더지 잡기 게임을 하게 됩니다. 하나를 고치면 다른 문제가 튀어나옵니다. 결국은 작동하게 만들겠지만요...

먼저, `summon[Encoder]` 줄은 실제로 무엇을 의미할까요? 정확히 어떤 타입을 JSON으로 변환하는 걸까요? `Encoder`는 아무 값이나 받아서 JSON으로 변환하는 것이 아니라, 변환할 타입을 나타내는 타입 파라미터를 받고, 그 특정 타입을 변환합니다. 따라서 `summon[Encoder[X]]`처럼 호출해야 합니다. 여기서 `X`는 현재 처리 중인 필드의 타입입니다.

여기서 `field`의 타입은 무엇일까요? 우리는 튜플을 다루고 있으므로 각 원소의 타입이 고유하며, `map`은 이를 처리할 수 있어야 합니다. 그러려면 `map`이 받는 함수 인자가 어떤 타입이든 처리할 수 있어야 합니다. 실제 `map`의 시그니처는 다음과 같습니다.

```scala
def map[F[_]](f: [t] => t => F[t]): Map[this.type, F]
```

시그니처가 좀 위협적인데, 다형 함수(polymorphic function)를 인자로 사용합니다(반환 타입은 당분간 무시하겠습니다). 이는 `map`에 전달하는 함수가 **어떤** 타입에 대해서든 동일하게 작동해야 한다는 뜻입니다[8](#user-content-fn-uniformtype). 현재 타입에 접근할 수 없으므로 구체적인 타입 인자를 제공할 수 없어 `Encoder`를 소환(summon)할 수 없습니다. 따라서 단순히 `map`을 호출하는 것은 우리에게 통하지 않습니다.

직접 커스텀 `map`을 구현하는 수밖에 없겠군요.

### 튜플 재귀

튜플에 대한 자체 `map` 유사 함수를 구현하기 위해 리스트와 튜플 사이의 유사성을 활용하겠습니다.

`List`가 있고 각 원소를 JSON으로 변환한 뒤 결과를 새 `List`로 모으고 싶다고 해봅시다. 단순화된, 꼬리 재귀가 아닌[9](#user-content-fn-tailrecursion) 풀이는 다음과 같습니다.

```scala
import io.circe.*

def listToJson[A: Encoder](ls: List[A]): List[Json] =
  ls match
    case Nil => Nil
    case h :: t =>
      val encoder = summon[Encoder[A]]

      val json = encoder(h)

      json :: listToJson(t)
```

리스트 인자에 패턴 매치를 걸어서, 빈 경우이거나 head와 tail이 있는 경우[10](#user-content-fn-headtail)를 처리하고 재귀합니다.

Scala 3의 튜플도 비슷한 구조를 노출합니다. `EmptyTuple`이거나 `*:`라는 비어 있지 않은 튜플입니다(`List`의 `::`에 대응합니다). 예를 들어 다음 튜플의 타입은,

```scala
val tuple: (Int, String, Boolean) = (3, "abc", true)
```

다음과 같이 쓸 수도 있습니다[11](#user-content-fn-confusing).

```scala
val tuple: Int *: String *: Boolean *: EmptyTuple = (3, "abc", true)
```

리스트처럼 생겼지만 타입 레벨에서의 리스트입니다. 정밀한 타입 정보가 리스트 형태로 있어서 재귀하며 다양하게 조작할 수 있습니다.

`List`와의 유사성을 따라 다음 코드를 작성할 수 있습니다.

```scala
def tupleToJson(tuple: Tuple): List[Json] = // 1
  tuple match
    case EmptyTuple => Nil // 2
    case ((h: h) *: (t: t)) => // 3
      val encoder = summon[Encoder[h]] // 4

      val json = encoder(h) // 5

      json :: tupleToJson(t) // 6
```

이 코드는 여러모로 깨져 있지만, 좋은 출발점입니다. 한 단계씩 두더지를 잡아 봅시다.

먼저 시그니처(1)를 보면, 제네릭 `Tuple`을 받고 있습니다. 이것은 모든 튜플 타입의 상위 타입입니다. 즉 `EmptyTuple`, `Int *: EmptyTuple`, `String *: Boolean *: EmptyTuple` 등을 포함합니다. 사용자가 전달할 구체적인 타입을 알 수 없으므로, 다음에서 하듯 패턴 매치로 확인해야 합니다.

첫 번째 case(2)는 간단합니다. `EmptyTuple`이면 재귀의 기저 사례이므로 빈 리스트를 반환합니다.

두 번째 case(3)는 비어 있지 않은 튜플 `*:`인 경우입니다. head `h`와 tail `t`로 분해합니다. head에 타입 `h`를, tail에 타입 `t`를 부여한 것에 주목하세요. 소문자로 타입을 쓴 것은 타입 변수임을 나타내기 위함입니다[12](#user-content-fn-patternmatching). 튜플의 head에 어떤 타입이 오는지, tail의 타입이 무엇인지는 실제로 알 수 없습니다.

구체적인 예를 들면, 입력 튜플이 `Int *: String *: EmptyTuple`이라면 재귀의 첫 번째 단계에서 `h = Int`이고 `t = String *: EmptyTuple`입니다. 다음 단계에서는 `h = String`이고 `t = EmptyTuple`입니다. 그다음 단계에서 빈 기저 사례에 도달합니다.

이제 튜플 head의 타입에 이름이 생겼으므로 적절한 `Encoder[h]`를 `summon`(4)합니다. `Encoder`(5)를 사용해 튜플 head의 값을 JSON으로 변환합니다. 마지막으로 tail로 재귀 호출(6)을 하고 현재 JSON을 재귀 호출 결과 앞에 붙입니다.

여기까지는 좋은데, 실제로 이 함수를 컴파일하면 다음과 같은 에러가 나옵니다.

```scala
-- [E006] Not Found Error: -----------------------------------------------------
4 |    case ((h: h) *: (t: t)) => // 3
  |              ^
  |              Not found: type h
-- [E006] Not Found Error: -----------------------------------------------------
4 |    case ((h: h) *: (t: t)) => // 3
  |                        ^
  |                        Not found: type t
-- [E006] Not Found Error: -----------------------------------------------------
5 |      val encoder = summon[Encoder[h]] // 4
  |                                   ^
  |                                   Not found: type h
```

컴파일러는 타입 `h`와 `t`가 패턴 매치와 별개로 독립적으로 존재해야 한다고 생각합니다. 이것을 타입 변수 선언으로 받아들이지 않는 것입니다.

하지만 우리가 추구하는 아이디어 자체는 타당합니다. 컴파일러가 이해할 수 있도록 다른 구문을 사용하면 됩니다.

```scala
 case tup: (h *: t) => ...
```

이 구문에서는 타입을 선언하되 구조 분해는 하지 않습니다. 이제 타입 `h`와 `t`만 선언하고 **값** `h`와 `t`는 선언하지 않으므로, 직접 가져와야 합니다. 튜플에 직접 존재하는 `head`와 `tail` 메서드를 사용하면 됩니다.

```scala
def tupleToJson(tuple: Tuple): List[Json] = // 1
  tuple match
    case EmptyTuple => Nil // 2
    case tup: (h *: t) => // 3
      val encoder = summon[Encoder[h]] // 4

      val json = encoder(tup.head) // 5

      json :: tupleToJson(tup.tail) // 6
```

다시 컴파일해 봅시다.

```scala
-- [E172] Type Error: ----------------------------------------------------------
5 |      val encoder = summon[Encoder[h]] // 4
  |                                      ^
  |No given instance of type io.circe.Encoder[h] was found for parameter x of method summon in object Predef.
```

컴파일러가 이제 타입 변수 선언은 받아들입니다. 하지만 그 타입으로 할 수 있는 일이 없다고 정당하게 불평합니다. `h`는 "새로운" 타입으로, 무엇이 될지 미리 알 수 없습니다. 사용자 입력에 따라 결정되는데, 그 정보는 컴파일 타임에 사용할 수 없습니다. `summon`은 필요한 given 값을 컴파일 타임에 결정하려 하므로 어디서 찾아야 할지 모르는 채 막혀 버립니다. 상황은 더 나빠집니다.

여기서 수행하는 패턴 매치는 런타임에 일어납니다. 그리고 패턴 매치의 대상은 제네릭 타입입니다. 제네릭 타입은 런타임에 지워지므로(erasure), 여기서 유용한 것을 얻을 방법이 없습니다.

summon이 작동하지 않는 것과 erasure 문제 모두, 우리가 뭔가 잘못하고 있음을 가리킵니다. 코드를 "잘못된 시점에" 실행하고 있는 것입니다.

### 컴파일 타임에 코드 실행하기

한 발 물러서서 `tupleToJson` 함수를 어떻게 사용할지 생각해 봅시다. 이 함수의 "사용자 입력"은 외부 IO에서 오는 임의의 값이 아닙니다. 우리가 제공할 정적으로 알려진 튜플 타입이 될 것입니다. 예를 들어 이런 식입니다.

```scala
val tuple: Int *: String *: Boolean *: EmptyTuple = (1, "abc", true)

val list = tupleToJson(tuple)
```

호출 지점에서는 평가되는 정확한 타입을 알고 있습니다. 타입 `Int`, `String`, `Boolean`에 대한 given `Encoder`를 결정하려고 하면 모든 것이 정상적으로 작동할 것입니다. 소환(summoning)을 호출 지점까지 지연시킬 방법이 필요합니다.

다행히 정확히 이 역할을 하는 함수가 있습니다.

```scala
scala.compiletime.summonInline
```

이 함수는 `summon`과 동일하지만, 문서에 명시된 대로 다음과 같이 동작합니다.

> 소환은 호출이 완전히 인라인될 때까지 지연됩니다.

호출이 "완전히 인라인"된다는 것은 무슨 뜻일까요? 바로 Scala의 새로운 [inline](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html) 키워드가 등장하는 대목입니다.

함수를 `inline`으로 표시하면 컴파일러가 항상 해당 함수의 코드를 호출 지점에 직접 삽입합니다. 이것만 들으면 무해한 기능 같지만, 인라인이 재귀적으로 적용될 수 있고, 컴파일러가 인라인하면서 단순한 제어 흐름 표현식(`if`나 `match` 같은)을 평가할 수 있다는 사실과 결합하면, 매크로 수준의 코드 생성 능력이 됩니다. 이것이 컴파일러가 우리를 위해 컴파일 타임에 코드를 평가하게 만드는 방법입니다.

좀 추상적으로 들릴 수 있지만, 실제로는 여기저기에 `inline` 키워드를 뿌리고 잘 되기를 바라면 됩니다. 해봅시다.

```scala
import scala.compiletime.summonInline

def tupleToJson(tuple: Tuple): List[Json] = // 1
  tuple match
    case EmptyTuple => Nil // 2
    case tup: (h *: t) => // 3
      val encoder = summonInline[Encoder[h]] // 4

      val json = encoder(tup.head) // 5

      json :: tupleToJson(tup.tail) // 6
```

여기서는 (4)만 `summonInline`으로 바꿨습니다. 여전히 같은 에러로 실패합니다.

```scala
No given instance of type io.circe.Encoder[h] was found.
```

감싸고 있는 `tupleToJson`이 어디에서도 인라인되지 않고 있으니 당연한 결과입니다. `inline`을 붙여 봅시다.

```scala
import scala.compiletime.summonInline

inline def tupleToJson(tuple: Tuple): List[Json] = // 1
  tuple match
    case EmptyTuple => Nil // 2
    case tup: (h *: t) => // 3
      val encoder = summonInline[Encoder[h]] // 4

      val json = encoder(tup.head) // 5

      json :: tupleToJson(tup.tail) // 6
```

이제 컴파일은 됩니다. 안타깝게도 `inline` 세계에 발을 들인 이상, 컴파일만 되는 것으로는 충분하지 않습니다. 인라인하는 코드, 특히 `summonInline` 호출은 호출 지점을 컴파일할 때에야 비로소 완전히 평가됩니다. 이렇게요.

```scala
val tuple = (1, "abc", true)

val list = tupleToJson(tuple)
```

`tupleToJson` 호출에서 컴파일이 실패합니다.

```scala
-- [E172] Type Error: ----------------------------------------------------------
3 |val list = tupleToJson(tuple)
  |           ^^^^^^^^^^^^^^^^^^
  |No given instance of type io.circe.Encoder[h] was found.
```

이전과 정확히 같은 에러가 나오지만, 호출 지점까지 지연시켰을 뿐입니다. 컴파일러가 `tupleToJson` 호출을 인라인할 때 같은 코드를 다른 위치에 배치하는 것일 뿐이므로 당연합니다. 평가 중인 구체적인 `h` 타입에 대해서는 여전히 아무것도 모릅니다.

`inline`을 좀 더 뿌려 봅시다.

```scala
import scala.compiletime.summonInline

inline def tupleToJson(tuple: Tuple): List[Json] = // 1
  inline tuple match
    case EmptyTuple => Nil // 2
    case tup: (h *: t) => // 3
      val encoder = summonInline[Encoder[h]] // 4

      val json = encoder(tup.head) // 5

      json :: tupleToJson(tup.tail) // 6
```

패턴 매치를 [inline match](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html#inline-matches)로 표시했습니다. 이렇게 하면 컴파일러가 패턴 매치를 컴파일 타임에 실제로 평가하고, 인라인 중에 컴파일 타임에 알려진 정보를 바탕으로 올바른 `case` 분기를 선택하도록 강제합니다. 패턴 매치할 수 있는 대상에 일부 제약이 생기지만, 우리의 경우에는 컴파일러가 평가할 수 있는 패턴 매치입니다. 인라인 지점에서 튜플 타입이 완전히 알려져 있으므로, 컴파일러가 각 인라인 단계에서 `h`와 `t`를 구체적인 타입으로 대체하고 올바른 `case` 분기를 선택할 수 있기 때문입니다. 이렇게요.

```scala
scala> val tuple = (1, "abc", true)

       val list = tupleToJson(tuple)

val tuple: (Int, String, Boolean) = (1,abc,true)
val list: List[Json] = List(1, "abc", true)
```

작동합니다! 튜플을 순회하면서 `summonInline` 호출을 인라인하는 데 성공했습니다. 컴파일러가 우리를 위해 대략 다음과 같은 코드를 생성한 것입니다.

```scala
    val tup = 1 *: "abc" *: true *: EmptyTuple
    val encoder = summon[Encoder[Int]]
    val json = encoder(tup.head)

    json :: {
      val tup = "abc" *: true *: EmptyTuple
      val encoder = summon[Encoder[String]]
      val json = encoder(tup.head)

      json :: {
        val tup = true *: EmptyTuple
        val encoder = summon[Encoder[Boolean]]
        val json = encoder(tup.head)

        json :: {
          Nil
        }
      }
    }
```

바로 우리가 원하던 것입니다. 런타임 문제 없이, 모든 것이 컴파일 타임에 안전하게 처리됩니다.

### 컴파일 타임 관찰 사항

이 접근 방식에서 흥미로운 관찰 몇 가지가 있습니다.

- 이런 식으로 코드를 생성할 수 있게 되면서 매크로와 유사한 능력을 얻게 됩니다. 이는 양날의 검이며, 매크로와 같은 단점도 일부 공유합니다. 가장 큰 단점은
- 에러를 사용자 코드를 컴파일할 때에야 발견한다는 것입니다. 모든 것이 예상대로 작동할지 미리 알 수 있는 방법이 없습니다. 그리고 문제가 발생하면
- 사용자는 자신이 작성하지 않은 코드에서 컴파일 에러를 디버깅해야 합니다. 코드 생성 로직이 정교해질수록 디버깅 과정도 복잡해질 수 있습니다.
- 타입 정보를 보존하는 것이 매우 중요합니다. 타입 정보가 누락되면 컴파일러가 완전히 인라인하지 못할 수 있습니다. 예를 들어,

```scala
scala> val tuple: Tuple = (1, "abc", true)

       val list = tupleToJson(tuple)
-- Error: ----------------------------------------------------------------------
3 |val list = tupleToJson(tuple)
  |           ^^^^^^^^^^^^^^^^^^
  |           cannot reduce inline match with
  |            scrutinee:  tuple : (tuple : Tuple)
  |            patterns :  case EmptyTuple
  |                        case tup @ _:*:[h @ _, t @ _]
  |-----------------------------------------------------------------------------
  |Inline stack trace
  |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  |This location contains code that was inlined from rs$line$6:2
2 |  inline tuple match
  |         ^
3 |    case EmptyTuple => Nil // 2
4 |    case tup: (h *: t) => // 3
5 |      val encoder = summonInline[Encoder[h]] // 4
6 |      val json = encoder(tup.head) // 5
7 |      json :: tupleToJson(tup.tail) // 6
  -----------------------------------------------------------------------------
1 error found
```

여기서는 `tuple: Tuple`로 표시해 의도적으로 타입 정보를 잃었습니다. 컴파일러는 제공된 튜플의 전체 타입을 알 수 없어 인라인 중에 막히고, 다소 알아보기 힘든 컴파일 에러를 내뱉습니다. 매크로가 일으키는 에러가 떠오르는 분위기입니다.

하지만 괜찮습니다. 우리 목적에는 충분히 쓸 만합니다.

### 케이스 클래스로 다시 해보기

`tupleToJson` 구현과 `Tuple.fromProductTyped`에 대한 지식을 갖추었으니, 실제 케이스 클래스 인스턴스를 받아서 JSON 값 묶음으로 변환할 수 있습니다.

```scala
scala> val fields = Tuple.fromProductTyped(user)
       val jsons = tupleToJson(fields)

val fields: (Email, Phone, Address) =
  (Email(bilbo@baggins.com,Some(frodo@baggins.com)),
   Phone(555-555-00,88),
   Address(Shire,Hobbiton))

val jsons: List[Json] = List(
  {"primary":"bilbo@baggins.com","secondary":"frodo@baggins.com"},
  {"number":"555-555-00","prefix":88},
  {"country":"Shire","city":"Hobbiton"})
```

원하던 JSON 값들입니다. 이제 하나의 JSON 객체로 병합해서 반환하기만 하면 됩니다.

여기서 작은 문제가 생깁니다. 리스트의 정적 타입이 `List[Json]`입니다. 임의의 JSON 값을 JSON 객체로 병합할 수는 없습니다. JSON 객체끼리만 단일 객체로 병합할 수 있습니다. 실제 값들이 JSON 객체라는 것은 알고 있지만, `Json` 값을 `JsonObject` 값으로 "캐스트"하려 하는 것은 안전하지 않습니다.

실제로 케이스 클래스에 `Int` 같은 원시 타입 필드가 있으면, JSON 숫자를 JSON 객체에 병합하려는 꼴이 되어 런타임에 터질 것입니다.

안전성 요구 사항 중 하나를 떠올려 봅시다.

> 최상위 클래스에 원시 타입 필드가 있으면 - 컴파일 실패

이 실패 모드를 어떻게 방지할까요?

### JSON 객체

해결책은 꽤 직관적입니다. circe에는 `Encoder.AsObject`라는 타입이 있는데, `Encoder`와 동일하지만 `encodeObject`라는 메서드를 추가로 제공하며, 이 메서드의 정적 반환 타입이 `JsonObject`입니다.

클래스의 각 필드에 `Encoder` 대신 `Encoder.AsObject`를 요구하면, 필드 중 어느 것이든 JSON 객체에 대응하지 않을 경우 컴파일에 실패합니다. 이전 코드를 약간만 수정하면 됩니다.

```scala
inline def tupleToJson(tuple: Tuple): List[JsonObject] = // 1
  inline tuple match
    case EmptyTuple => Nil
    case tup: (h *: t) =>
      val encoder = summonInline[Encoder.AsObject[h]] // 2

      val json = encoder.encodeObject(tup.head) // 3

      json :: tupleToJson(tup.tail)
```

이제 정적 반환 타입(1)이 `List[JsonObject]`입니다. 현재 필드에 대해 `Encoder.AsObject`를 소환(2)하여 이를 달성합니다. 그런 다음 `encodeObject`(3)를 호출해 `JsonObject` 인스턴스를 생성할 수 있습니다.

이전의 케이스 클래스 직렬화 예제에서는 여전히 동일하게 작동합니다. 하지만 클래스에 원시 타입 필드를 추가하면,

```scala
case class User(id: Int, email: Email, phone: Phone, address: Address)
```

컴파일이 실패합니다.

```scala
scala> val fields = Tuple.fromProductTyped(user)
       val jsons = tupleToJson(fields)
-- [E172] Type Error: ----------------------------------------------------------
2 |val jsons = tupleToJson(fields)
  |            ^^^^^^^^^^^^^^^^^^^
  |No given instance of type io.circe.Encoder.AsObject[Int] was found.
  |I found:
  |
  |    io.circe.Encoder.AsObject.importedAsObjectEncoder[Int](
  |      /* missing */summon[io.circe.export.Exported[io.circe.Encoder.AsObject[Int]]])
  |
  |But no implicit values were found that match type io.circe.export.Exported[io.circe.Encoder.AsObject[Int]].
```

`Encoder.AsObject[Int]`이 존재하지 않는다고 정확하게 알려줍니다[13](#user-content-fn-bettererror).

### 마무리

이제 모든 조각을 합쳐서 플랫 `Encoder`를 완성할 준비가 되었습니다.

```scala
val encoder = Encoder.instance[User]: value => // 1
  val fields = Tuple.fromProductTyped(value) // 2
  val jsons = tupleToJson(fields) // 3

  concatObjects(jsons) // 4

def concatObjects(jsons: List[JsonObject]): Json =
  Json.obj(jsons.flatMap(_.toList): _*)
```

`User` 클래스에 대한 새 `Encoder`를 만들고 있습니다(1). 주어진 `User` 값에 대해 수행할 동작을 지정하는 방식입니다.

- `User`를 필드들의 튜플로 변환합니다(2)
- `tupleToJson`(3)으로 필드들을 순회하면서 각 필드를 `JsonObject`로 변환합니다
- `concatObjects`(4)는 `JsonObject` 리스트를 안전하게 단일 객체로 만들어 주는 작은 유틸리티 함수입니다

완성된 `Encoder`를 호출하면 원하는 플랫 JSON이 생성됩니다.

```scala
scala>  val json = encoder(user)
val json: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}
```

마침내 완성입니다!

`Encoder`의 전체 코드는 함께 제공되는 [저장소](https://github.com/ncreep/scala3-flat-json-blog/blob/master/flat_concrete.scala)에서 확인할 수 있습니다.

## 구체 클래스 역직렬화

이제 플랫 JSON을 다시 읽어올 차례입니다.

### 의사 코드

circe에서 역직렬화하려면 `User` 타입의 `Decoder` 인스턴스가 필요합니다.

이전 파트에서처럼 플랫 JSON `Decoder`를 구성하는 의사 코드부터 스케치해 봅시다:

1. `User` 타입이 주어지면
2. 클래스의 각 필드 타입마다
3. 적절한 `Decoder`를 찾고
4. 결과 `Decoder`들을 하나의 `Decoder`로 합치고
5. 합쳐진 `Decoder`로 `User` 인스턴스를 읽어낸다

이 의사 코드는 `Encoder`의 의사 코드와 꽤 비슷하지만, 눈에 띄는 차이가 있습니다. 어떤 값(즉 `User`의 필드들)에서 출발하는 대신, 타입(`User` 필드들의 타입)에서 출발한다는 점입니다. 역직렬화는 JSON에서 시작하고, `Decoder`가 작업을 마친 뒤에야 `User` 인스턴스를 돌려받으니 당연한 일입니다.

여기서 새로운 문제가 생깁니다: `User`의 필드 타입을 어떻게 잡아낼 수 있을까요?

### Mirror

여기서도 Scala 3는 새로운 도구를 제공합니다: [Mirror](https://blog.philipp-martini.de/blog/magic-mirror-scala3/) 타입입니다.

`Mirror`는 [여러 타입](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html#mirror-1), 특히 case class에 메타프로그래밍 인터페이스를 제공하는 특수한 값입니다. 각 `Mirror` 인스턴스는 우리가 관심 있는 클래스에 맞게 컴파일러가 즉석에서 합성합니다[14](#user-content-fn-generic). `User` 타입의 `Mirror`는 다음과 같이 소환할 수 있습니다:

```scala
scala> import scala.deriving.Mirror

       val mirror = summon[Mirror.Of[User]]
val mirror:
  scala.deriving.Mirror.Product{
    type MirroredMonoType = User;
      type MirroredType = User;
      type MirroredLabel = "User";
      type MirroredElemTypes = (Email, Phone, Address);
      type MirroredElemLabels = ("email", "phone", "address")
  } = User
```

결과로 얻은 `Mirror` 값은 다소 밋밋한 기본 `Mirror` 타입을 매우 세밀하게 정제한 것입니다. 우리 목적에서 흥미로운 부분은 `MirroredElemTypes` 타입 멤버입니다:

```scala
type MirroredElemTypes = (Email, Phone, Address)
```

바로 `User` 타입을 필드 타입들의 튜플로 표현한 것입니다. 그러므로 이 튜플 타입이 우리가 순회할 대상입니다. 추가로 `Mirror`는 `User`를 튜플로/튜플에서 변환하는 유틸리티도 제공하는데, 이것은 나중에 사용하겠습니다.

그런데 한 가지 꼬이는 부분이 있습니다. 이전에 튜플을 순회할 때는 튜플 **값**, 즉 런타임에서 실제로 연산할 수 있는 구체적인 무언가가 있었습니다. 반면 여기서 가진 것은 튜플 **타입**뿐입니다. 타입만으로 대체 무엇을 할 수 있을까요?

### Erased Value

사실 꽤 많은 것을 할 수 있습니다. `inline`과 `inline match`로 컴파일 타임 코드 평가를 달성했던 방법을 떠올려 봅시다. 같은 도구를 여기에도 적용할 수 있으며, 타입을 값으로 끌어내는 방법만 있으면 됩니다.

바로 [scala.compiletime.erasedValue](https://www.scala-lang.org/api/3.3.1/scala/compiletime.html#erasedValue-fffff7c4)가 등장하는 지점입니다:

> 타입은 있지만 그 값은 없고, 패턴 매칭은 하고 싶을 때 이 메서드를 사용하세요.

정확히 우리에게 필요한 것입니다.

`erasedValue`는 타입을 (가짜) 값으로 바꿔서 패턴 매칭할 수 있게 해줍니다. 단, 컴파일 타임에 전개되는 `inline` 안에서만 사용해야 한다는 제약이 있습니다. 사용 예시는 다음과 같습니다:

```scala
import scala.compiletime.*

inline def size[T <: Tuple]: Int = // 1
  inline erasedValue[T] match // 2
    case EmptyTuple => 0 // 3
    case _: (h *: t) => 1 + size[t] // 4
```

튜플 타입의 크기를 계산하는 방법을 정의하고 있습니다. 임의의 `Tuple` 타입을 받는 `inline` 함수로 시작합니다 (1). 그런 다음 튜플 타입의 `erasedValue`에 패턴 매칭합니다 (2). 덕분에 일반적인 패턴 매칭 문법으로 튜플의 두 가지 경우를 분기할 수 있습니다. 먼저 빈 경우 (3)에서는 빈 튜플의 크기인 0을 반환합니다. 다음으로 비어 있지 않은 경우 (4)에서는 머리(`h`)와 꼬리(`t`)가 있습니다. 이 경우 크기는 머리의 크기 1에 꼬리의 크기를 더한 것이며, 꼬리의 크기는 `size`를 재귀 호출해서 계산합니다.

테스트해 볼 수 있습니다:

```scala
scala> size[(String, Int, Double)]
val res2: Int = 3
```

예상대로 튜플의 크기는 `3`입니다.

Note

패턴 매칭에서 값에 이름을 붙이는 대신 `_`를 사용하고 있습니다. 이것은 `erasedValue`의 양보 불가능한 제한입니다. 타입만 있으므로 런타임에서 **실제로** 사용할 수 있는 값이 존재하지 않습니다. 따라서 패턴 매칭에서 매칭 대상인 (가짜) 값을 사용하는 것이 허용되지 않습니다. 각 분기에서 만나는 타입에 반응하는 것만 가능합니다[15](#user-content-fn-atruntime).

이제 `inline`과 `erasedValue`로 튜플 타입을 순회할 수 있게 되었습니다.

### 다시 튜플 순회

이 도구들을 갖추었으니 의사 코드를 부분적으로 구현할 수 있습니다. 당장은 `User` 타입을 무시하고, 임의의 튜플에 플랫 `Decoder`를 만드는 문제부터 풀어 봅시다. 이것을 해결한 뒤 `Mirror`로 튜플에서 `User` 인스턴스를 만들면 됩니다.

더 이상 미루지 않고, 코드를 봅시다:

```scala
inline def decodeTuple[T <: Tuple]: Decoder[T] =  // 1
  inline erasedValue[T] match // 2
    case EmptyTuple => Decoder.const(EmptyTuple) // 3
    case _: (h *: t) => // 4
      val decoder = summonInline[Decoder[h]] // 5

      combineDecoders(decoder, decodeTuple[t]) // 6

def combineDecoders[H, T <: Tuple](dh: Decoder[H], dt: Decoder[T]): Decoder[H *: T] = // 7
  dh.product(dt).map(_ *: _)
```

단계별로 따라가 봅시다:

- 튜플의 모든 하위 타입에 동작하며, 해당 튜플 타입의 디코더를 생산하는 `inline` 함수가 있습니다 (1).
- 튜플 타입의 `erasedValue`에 패턴 매칭합니다 (2).
- `EmptyTuple` 경우 (3)에서는 항상 `EmptyTuple`을 반환하는 새 `Decoder`를 만듭니다.
- 비어 있지 않은 경우 (4)에서는 머리 타입 `h`와 꼬리 타입 `t`가 있습니다.
- 머리 타입의 `Decoder`를 `summonInline`으로 소환합니다 (5). `summonInline`은 함수 사용 지점으로 지연되며, 실제 `h` 타입에 `given` `Decoder`가 스코프에 있을 때만 성공한다는 점을 기억하세요.
- 그런 다음 재귀 호출 (6)로 꼬리 타입의 `Decoder`를 만들고, `combineDecoders`로 방금 소환한 `Decoder[h]`와 재귀 호출로 만든 `Decoder[t]`로부터 전체 튜플의 `Decoder`를 구축합니다.
- `combineDecoders` (7)는 튜플의 머리(`H`)용 `Decoder`와 꼬리(`T`)용 `Decoder`를 받아서 머리와 꼬리를 합친 `Decoder[H *: T]`를 만드는 작은 유틸리티 함수입니다. `Decoder`의 `product` 함수로 두 `Decoder`를 같은 JSON에 적용하고 결과를 튜플로 묶습니다. 플랫 JSON에서 튜플을 읽을 때 바로 이것이 필요합니다. `map` 호출은 `product`의 결과에서 최종 튜플 `H *: T`를 구성합니다.

의사 코드의 설명과 거의 일치합니다. 안타깝게도 이 코드는 깨져 있습니다:

```scala
-- [E007] Type Mismatch Error: -------------------------------------------------
3 |    case EmptyTuple => Decoder.const(EmptyTuple) // 3
  |                                     ^^^^^^^^^^
  |       Found:    EmptyTuple.type
  |       Required: T
  |
  |          where:    T is a type in method decodeTuple with bounds <: Tuple
-- [E007] Type Mismatch Error: -------------------------------------------------
7 |      combineDecoders(decoder, decodeTuple[t]) // 6
  |      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  |      Found:    io.circe.Decoder[h *: t]
  |      Required: io.circe.Decoder[T]
  |
  |      where:    T is a type in method decodeTuple with bounds <: Tuple
  |                h is a type in method decodeTuple with bounds
  |                t is a type in method decodeTuple with bounds <: Tuple
```

`EmptyTuple` 분기에 매칭되면 `T = EmptyTuple`이고, `h *: t` 분기에 매칭되면 `T = h *: t`라는 것을 컴파일러가 알아내지 못하는 것 같습니다.

거의 다 왔는데…

### 타입 해킹

여기서 부족한 것은 타입 통합(type-unification)입니다. 컴파일러가 각 case 분기의 타입을 타입 인자 `T`와 통합할 수 있어야 합니다. 컴파일러가 이걸 못 하는 건 버그라고 주장하고 싶지만, 이 문제를 우회하는 해킹 방법이 있습니다.

컴파일러는 실제 타입의 타입 인자를 통합하는 데 더 뛰어난 것 같습니다[16](#user-content-fn-realtypes). 이를 발동시키려면 패턴 매칭에 타입 인자를 포함해야 합니다. 인위적으로 다음과 같이 할 수 있습니다:

```scala
trait Is[A] // 1

inline def decodeTuple[T <: Tuple]: Decoder[T] =
  inline erasedValue[Is[T]] match // 2
    case _: Is[EmptyTuple] => Decoder.const(EmptyTuple) // 3
    case _: Is[h *: t] => // 4
      val decoder = summonInline[Decoder[h]]

      combineDecoders(decoder, decodeTuple[t])

def combineDecoders[H, T <: Tuple](dh: Decoder[H], dt: Decoder[T]): Decoder[H *: T] = // 7
  dh.product(dt).map(_ *: _)
```

타입 인자 하나를 가진 더미 래퍼를 만들었습니다 (1)[17](#user-content-fn-doesntmatter). 이제 `T`에 직접 패턴 매칭하는 대신 `Is[T]`에 패턴 매칭합니다 (2). 각 분기 (3)과 (4)가 `Is`로 감싸져 있고, 이것이 어떻게든 컴파일러가 `T`를 각 분기의 타입과 통합해야 한다고 이해하게 만듭니다.

이 코드는 이제 컴파일됩니다!

플랫 `Encoder`로 만든 플랫 JSON에서 튜플을 디코딩해서 동작을 확인해 볼 수도 있습니다:

```scala
scala>  val json = encoder(user)
val json: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}

scala> decodeTuple[mirror.MirroredElemTypes].decodeJson(json)
val res1:
  Decoder.Result[mirror.MirroredElemTypes] =
    Right(
      (Email(bilbo@baggins.com,Some(frodo@baggins.com)),
      Phone(555-555-00,88),
      Address(Shire,Hobbiton)))
```

타입 인자로 `mirror.MirroredElemTypes`를 사용하고 있는 점에 주목하세요. 이것은 경로 의존 타입으로, `User`의 필드 타입들(`(Email, Phone, Address)`)의 별칭일 뿐이라는 점을 기억하세요.

이전 파트에서 생성한 플랫 JSON에서 `User`의 모든 필드 타입으로 구성된 튜플을 디코딩하는 데 성공했습니다. 거의 다 되었고, 이 코드를 튜플에서 `User` 타입으로 변환하는 것으로 감싸기만 하면 됩니다.

### 타입에서 값으로

퍼즐의 마지막 조각은 `Decoder`의 결과인 튜플 값을 다시 `User` 값으로 변환하는 방법입니다. 여기서도 `Mirror`가 구원해 줍니다: `mirror.fromTuple`은 `User` 타입에 맞는 튜플 값을 받아 `User` 인스턴스를 만들어 냅니다:

```scala
scala> val tuple = (
            Email("bilbo@baggins.com", Some("frodo@baggins.com")),
            Phone("555-555-00", 88),
            Address("Shire", "Hobbiton"))

       val user = mirror.fromTuple(tuple)

val user: User = User(
 Email(bilbo@baggins.com,Some(frodo@baggins.com)),
 Phone(555-555-00,88),
 Address(Shire,Hobbiton))
```

이것을 활용해 최종 `Decoder` 인스턴스를 만들 수 있습니다:

```scala
val mirror = summon[Mirror.Of[User]] // 1

val decoder =
  decodeTuple[mirror.MirroredElemTypes] // 2
    .map(mirror.fromTuple) // 3
```

먼저 `User` 타입의 `Mirror`를 소환합니다 (1). 그런 다음 미러에서 가져온 `User`의 필드 타입들로 플랫 `Decoder`를 만듭니다 (2). 마지막으로 `mirror.fromTuple` 함수로 `Decoder`의 튜플 결과를 `User` 인스턴스로 변환합니다 (3).

### 모든 것을 합치기

최종 `Decoder`를 테스트하기 전에, `Encoder`와 합쳐서 하나의 `Codec`으로 만들어 봅시다:

```scala
val codec = Codec.from(decoder, encoder)
```

이것은 `User` 인스턴스를 플랫 JSON 형식으로 직렬화/역직렬화할 수 있는 완전한 기능의 `Codec`입니다. 쉽게 시연할 수 있습니다:

```scala
scala> val json = codec(user)
       val decodedUser = codec.decodeJson(json)

val json: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}
val decodedUser: Decoder.Result[User] = Right(
  User(
    Email(bilbo@baggins.com,Some(frodo@baggins.com)),
    Phone(555-555-00,88),
    Address(Shire,Hobbiton)))
```

플랫 JSON으로 성공적인 왕복 변환을 달성했습니다!

`Decoder`와 `Codec`의 전체 코드는 함께 제공되는 [저장소](https://github.com/ncreep/scala3-flat-json-blog/blob/master/flat_concrete.scala)에서 찾을 수 있습니다.

## 범용 Codec

지금까지는 구체적인 `User` 클래스 직렬화에 집중했으니, 이제 한 단계 올려서 어떤 case class든 다룰 수 있게 만들 차례입니다.

다행히 지금까지 작성한 코드는 실제로 꽤 범용적입니다. `User` 클래스에 종속된 가정을 코드에 직접 심어 놓은 경우가 많지 않습니다.

지금까지의 코드를 돌아보면:

```scala
val codec: Codec[User] = // 1
  val encoder = Encoder.instance[User]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val mirror = summon[Mirror.Of[User]] // 2

  val decoder =
    decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

`User` 타입을 직접 참조하는 곳은 딱 두 군데뿐입니다. `val` 선언 (1)과 `User`의 `Mirror`를 소환하는 곳 (2)입니다. 나머지 코드는 완전히 범용적이며 `User` 타입을 명시적으로 참조하지 않습니다.

이제 해야 할 일은 `User` 대신 타입 파라미터를 꺼내고 적절한 `Mirror`를 `given` 인자로 전달하는 것뿐입니다. 해봅시다:

```scala
def makeCodec[A]( // 1
    using mirror: Mirror.Of[A]): Codec[A] = // 2

  val encoder = Encoder.instance[A]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val decoder =
    decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

새 함수 `makeCodec`을 만들었습니다. 이제 `User` 대신 타입 파라미터 `A`를 사용하고 (1), `Mirror`를 `using` 인자로 전달합니다 (2). 늘 그렇듯 첫 번째 시도에서 당연히 컴파일에 실패합니다:

```scala
-- [E007] Type Mismatch Error: -------------------------------------------------
5 |    val fields = Tuple.fromProductTyped(value)
  |                                        ^^^^^
  |      Found:    (value : A)
  |      Required: Product
```

case class를 튜플로 변환하는 `Tuple.fromProductTyped`에 타입 바운드가 있는 것 같습니다. `Product`의 하위 타입에만 사용할 수 있습니다. 이치에 맞는데, `enum` 인스턴스에 `fromProductTyped`를 쓰는 것은 의미가 없을 것입니다. 더 일반적으로, 우리가 작성한 코드는 `enum`에 적용하면 큰 의미가 없습니다. 쉽게 고칠 수 있습니다. 타입 바운드를 추가하면 됩니다:

```scala
def makeCodec[A <: Product]( // 1
    using mirror: Mirror.Of[A]): Codec[A] = // 2

  val encoder = Encoder.instance[A]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val decoder =
    decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

또 실패합니다:

```scala
-- [E172] Type Error: ----------------------------------------------------------
5 |    val fields = Tuple.fromProductTyped(value)
  |                                              ^
  |No given instance of type deriving.Mirror.ProductOf[A] was found for parameter m of method fromProductTyped in object Tuple. Failed to synthesize an instance of type deriving.Mirror.ProductOf[A]: trait Product is not a generic product because it is not a case class
  |
  |where:    A is a type in method makeCodec with bounds <: Product
-- [E008] Not Found Error: -----------------------------------------------------
10 |  val decoder = decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)
   |                                                          ^^^^^^^^^^^^^^^^
   |        value fromTuple is not a member of deriving.Mirror.Of[A]
   |
   |        where:    A is a type in method makeCodec with bounds <: Product
```

이제 `Mirror`에 종류가 다르다는 것을 알게 됩니다. `Mirror.ProductOf`가 있고 `Mirror.SumOf`가 있습니다. 곱 타입(product type)이라고도 하는 case class는 `Mirror.ProductOf`와 매칭됩니다. 따라서 case class와 튜플 사이를 오가는 모든 함수는 `Mirror.ProductOf`가 필요합니다. 이제 `Mirror` 인자를 이 더 구체적인 타입으로 특화할 수 있습니다:

```scala
def makeCodec[A <: Product]( // 1
    using mirror: Mirror.ProductOf[A]): Codec[A] = // 2

  val encoder = Encoder.instance[A]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val decoder =
    decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

그런데 또 실패합니다…

```scala
-- Error: ----------------------------------------------------------------------
 6 |    val jsons = tupleToJson(fields)
   |                ^^^^^^^^^^^^^^^^^^^
   |               cannot reduce inline match with
   |                scrutinee:  fields : (fields : mirror.MirroredElemTypes)
   |                patterns :  case EmptyTuple
   |                            case tup @ _:*:[h @ _, t @ _]
   |----------------------------------------------------------------------------
-- Error: ----------------------------------------------------------------------
10 |  val decoder = decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)
   |                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
   |cannot reduce inline match with
   | scrutinee:  compiletime.erasedValue[Is[mirror.MirroredElemTypes]] : Is[mirror.MirroredElemTypes]
   | patterns :  case _:Is[EmptyTuple]
   |             case _:Is[*:[h @ _, t @ _]]
   |----------------------------------------------------------------------------
```

이번 에러는 좀 더 해독하기 어렵습니다[18](#user-content-fn-elided). `inline match`에 문제가 있는 것 같고, 매치를 축소(reduce)하지 못한다는 내용입니다. 흥미롭게도 에러의 일부는 지금 손대지도 않은 코드를 가리키고 있습니다.

다양한 튜플 변환을 작성할 때 `inline`을 사용했다는 점을 기억해야 합니다. 그 결과, 해당 코드가 지금 컴파일하고 있는 호출 지점에 직접 복사/붙여넣기됩니다. 구체적인 `User` 타입으로 작업할 때는 컴파일러가 관련된 정확한 타입, 즉 `User` 타입에 대응하는 적절한 튜플 타입을 쉽게 추론할 수 있어서 문제가 없었습니다. 하지만 추상 타입 `A`로는 컴파일러가 막히고 맙니다. `A`에 매칭되는 튜플이 컴파일 타임에 정확히 어떤 형태인지 알 수 없기 때문입니다. 결과적으로 그 알 수 없는 튜플 타입에 컴파일 타임 패턴 매칭을 수행할 수 없습니다. "cannot reduce inline match"라는 에러 메시지가 의미하는 바가 이것입니다.

다시 한번, 해결책은 타입이 컴파일러에 완전히 알려질 때까지 타입 결정을 지연시키는 것입니다. `makeCodec` 내부에서 코드를 평가하는 대신, `makeCodec` 자체의 평가를 이후 `makeCodec`의 호출 지점으로 미루는 것입니다. 어딘가에서 실제로 `makeCodec[User]`를 호출할 때, 그때서야 컴파일러가 관련된 모든 타입을 알게 되어 우리가 사용한 `inline match`들을 적용할 수 있습니다.

결론적으로 `makeCodec`도 `inline`으로 표시해야 합니다:

```scala
inline def makeCodec[A <: Product]( // 1
    using mirror: Mirror.ProductOf[A]): Codec[A] = // 2

  val encoder = Encoder.instance[A]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val decoder =
    decodeTuple[mirror.MirroredElemTypes].map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

이제 컴파일에 성공합니다.

여기서 배울 수 있는 중요한 교훈은 `inline`이 다소 전파성이 있다는 것입니다. 코드 깊은 곳에서 `inline`을 사용하면, 컴파일러가 분석할 수 있을 만큼 타입이 충분히 구체적인 지점까지 스택 위로 전파해야 합니다.

기억하시겠지만, `inline` 세계에 들어서면 컴파일이 된다고 모든 것이 올바르게 동작하는 것은 아닙니다. 실행될 실제 코드가 호출 지점에서야 완전히 결정되기 때문입니다. 즉, 구체적인 타입으로 `makeCodec`을 호출하는 것도 동작하는지 확인해야 합니다. 이전처럼 `makeCodec`으로 JSON 왕복 변환을 해봅시다:

```scala
scala> val codec = makeCodec[User]

       val json = codec(user)
       val decodedUser = codec.decodeJson(json)
val codec: Codec[User] = io.circe.Codec$$anon$4@da69e23
val json: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}
val decodedUser: Decoder.Result[User] = Right(
  User(
    Email(bilbo@baggins.com,Some(frodo@baggins.com)),
    Phone(555-555-00,88),
    Address(Shire,Hobbiton)))
```

또다시 잘 동작합니다!

이 파트의 전체 코드는 [여기](https://github.com/ncreep/scala3-flat-json-blog/blob/master/flat_generic.scala)에서 찾을 수 있습니다.

이제 어떤 case class든 플랫 JSON 형식을 만들어주는 범용 함수가 생겼습니다. 원래 문제 정의를 거의 완전히 충족합니다. 마지막으로 안전성 요구사항 하나만 남았습니다. 그것으로 넘어가기 전에, 잠깐 곁길로 빠져 봅시다.

## 모듈화

서론에서 매크로 대신 타입 수준 프로그래밍을 사용해야 한다고 설득했습니다. 매크로는 일회성 해결책에 가까운 반면, 타입 수준 프로그래밍은 재사용 가능한 기법으로 이어지기 때문입니다.

### 문제가 뭡니까?

지금까지 꽤 많은 타입 수준 프로그래밍을 해왔는데, 정말로 재사용 가능한 걸까요?

좀 더 고급 함수 하나를 살펴봅시다:

```scala
inline def decodeTuple[T <: Tuple]: Decoder[T] =
  inline erasedValue[Is[T]] match
    case _: Is[EmptyTuple] => Decoder.const(EmptyTuple)
    case _: Is[h *: t] =>
      val decoder = summonInline[Decoder[h]]

      combineDecoders(decoder, decodeTuple[t])
```

이게 정말 재사용 가능한 코드일까요, 아니면 특정 문제에 맞춘 맞춤형 솔루션일까요?

솔직히 말하면, 재사용 가능하지 않습니다. 매우 구체적인 문제를 비모듈적인 방식으로 풀고 있습니다.

비모듈적이라 함은, 서로 다른 두 가지 관심사가 하나의 함수에 뒤섞여 있다는 뜻입니다:

- 튜플 순회
- 각 튜플 구성요소에 맞는 given 소환

다른 문제에서 순회는 필요하지만 소환은 필요 없다면? 소환이 코드의 다른 부분에서 일어나야 한다면?

현재 `decodeTuple`의 구현으로는 이런 변경이 불가능합니다. 하지만 더 나은 방법이 있습니다.

모듈성을 되찾는 한 가지 방법은 로직을 함수와 인자로 분리하는 것입니다. 한 단계 순회한 뒤 `Decoder` 하나를 소환하는 대신, 모든 `Decoder`를 튜플로 전달받아 순회할 수 있습니다. 이렇게 하면 소환과 순회가 완전히 분리됩니다.

### Match Types

좋은 접근법이긴 하지만, 새로운 문제가 생깁니다. 새 함수의 타입은 어떻게 될까요?

지금까지는 이런 형태였습니다:

```scala
def decodeTuple[T <: Tuple]: Decoder[T]
```

임의의 튜플 타입에 대해 해당 타입의 `Decoder`를 만들어낸다는 뜻입니다. 이제 입력을 `Decoder` 튜플로 바꾸고 싶습니다. 첫 번째 시도는 이렇습니다:

```scala
def decodeTuple[T <: Tuple](decoders: T): Decoder[...]
```

순회할 모든 `Decoder`가 담긴 튜플을 입력으로 받겠다는 선언입니다. 이 튜플의 형태는 아마 `(Decoder[A1], Decoder[A2], ..., Decoder[AN])`일 것입니다. 그런데 실제 입력은 임의의 튜플 그 이상의 구조를 가지고 있으므로, 타입 시그니처에 정보가 부족합니다[19](#user-content-fn-patternmatch). 게다가 입력 타입과 출력 타입의 관계를 표현하기도 어렵습니다. 입력이 `T <: Tuple`뿐이라면, 결과 `Decoder`의 타입은 무엇이어야 할까요[20](#user-content-fn-alternativesolution)? 가상의 `(A1, A2, ..., AN)` 튜플에 접근할 방법이 없습니다. 그래서 반환 타입에 타입 매개변수가 빠져 있는 것입니다.

이쯤 되면 이 글에서 제가 던지는 모든 문제에 Scala 3가 해결책을 갖고 있다는 사실에 익숙해졌을 겁니다[21](#user-content-fn-coincidence). 이번에는 `Tuple.Map`이라는 내장 타입으로 입력 타입을 더 정확하게 표현할 수 있습니다.

튜플 `(A1, A2, ..., AN)`을 다른 튜플 타입 `(Decoder[A1], Decoder[A2], ..., Decoder[AN])`과 연결해주는 타입이 필요합니다. 튜플과 리스트의 유추를 떠올리면, "리스트" `(A1, A2, ..., AN)` 위에 "함수" `Decoder`를 map한 것과 같습니다. 원하는 타입은 다음과 같습니다:

```scala
Tuple.Map[(A1, A2, ..., AN), Decoder]
```

`Tuple.Map`은 "[match type](https://docs.scala-lang.org/scala3/reference/new-types/match-types.html)"이라 불리는 특별한 종류의 타입입니다. 패턴과 재귀 호출을 작성하여 타입 간의 복잡한 관계를 명시할 수 있습니다. `Tuple.Map`의 정의는 대략 다음과 같습니다:

```scala
type Map[T <: Tuple, F[_]] <: Tuple = // 1
  T match // 2
    case EmptyTuple => EmptyTuple // 3
    case h *: t => F[h] *: Map[t, F] // 4
```

이렇게 읽을 수 있습니다:

- 어떤 튜플 타입 `T`와 타입 생성자 `F`가 주어지면 (1).
- 타입 `T`에 대해 매칭합니다 (2).
- 빈 튜플이면 결과 타입도 빈 튜플입니다.
- 머리 `h`와 꼬리 `t`로 이루어져 있으면, 결과는 `F[h]`와 꼬리에 대한 `Map` 재귀 호출로 구성된 새 튜플입니다 (4).

구체적인 튜플과 타입 생성자로 `Map`을 "호출"하면, 컴파일러가 컴파일 시점에 이 로직을 수행하여 새로운 결과 타입을 계산합니다[22](#user-content-fn-dayofyore). 이는 `List.map`을 순진한 재귀로 구현하는 것과 매우 비슷합니다. 구체적인 예제로 보면 더 이해하기 쉽습니다.

`T = (String, Int, Double)`이고 `F = Decoder`라고 합시다. 그러면 `Map[(String, Int, Double), Decoder]` 타입은 무엇일까요? 새로운 튜플 문법으로 `Map[String *: Int *: Double *: EmptyTuple, Decoder]`라고 쓸 수 있다는 것을 떠올립시다. 이제 단계별로 축약해 봅시다:

```scala
Map[String *: Int *: Double *: EmptyTuple, Decoder] =
// T is not empty, `h = String` and `t = Int *: Double *: EmptyTuple`
F[String] *: Map[Int *: Double *: EmptyTuple, Decoder] =
// T is not empty, `h = Int` and `t = Double *: EmptyTuple`
F[String] *: F[Int] *: Map[Double *: EmptyTuple, Decoder] =
// T is not empty, `h = Double` and `t = EmptyTuple`
F[String] *: F[Int] *: F[Double] *: Map[EmptyTuple, Decoder] =
// T is empty
F[String] *: F[Int] *: F[Double] *: EmptyTuple =
// With the regular syntax
(F[String], F[Int], F[Double])
```

휴, 꽤 힘들었지만 다행히 이제부터는 컴파일러가 대신 해줍니다. `Map` match type은 예상되는 입력의 형태를 정확하게 지정하면서 동시에 출력도 쉽게 정의할 수 있게 해줍니다:

```scala
def decodeTuple[T <: Tuple](decoders: Map[T, Decoder]): Decoder[T]
```

이렇게 읽습니다: 내부 타입들이 튜플 `T`를 구성하는 `Decoder` 튜플이 주어지면, `T`에 대한 단일 `Decoder`를 생성합니다.

### 또다시 튜플 순회...

타입 시그니처를 잡았으니, `inline match`로 구현할 수 있습니다. 구현은 이전 `decodeTuple` 버전과 비슷하지만, 이제 `summon`할 필요 없이 순회하면서 튜플의 현재 요소를 꺼내기만 하면 됩니다:

```scala
import Tuple.*

inline def decodeTuple[T <: Tuple](decoders: Map[T, Decoder]): Decoder[T] = // 1
  inline decoders match // 2
    case EmptyTuple => Decoder.const(EmptyTuple) // 3
    case ds: (Decoder[h] *: Map[t, Decoder]) => // 4
      combineDecoders(ds.head, decodeTuple(ds.tail)) // 5
```

이제 익숙하게 느껴질 겁니다:

- `Decoder` 튜플이 주어지면 (1).
- `Decoder`들에 대해 inline match를 수행합니다 (2).
- 튜플이 비어 있으면 빈 튜플로 상수 `Decoder`를 생성합니다 (3).
- 비어 있지 않고 머리가 `Decoder[h]`이고 꼬리가 `Map[t, Decoder]`이면, 여기서 `h`와 `t`는 `T` 튜플의 머리와 꼬리입니다 (4).
- `Decoder[h]`와 나머지 `t` `Decoder`들에 대한 `decodeTuple` 호출 결과를 조합합니다 (5).

물론 이건 컴파일되지 않습니다:

```scala
-- [E007] Type Mismatch Error: -------------------------------------------------
3 |    case EmptyTuple => Decoder.const(EmptyTuple) // 3
  |                                     ^^^^^^^^^^
  |       Found:    EmptyTuple.type
  |       Required: T
  |
-- [E007] Type Mismatch Error: -------------------------------------------------
5 |      combineDecoders(ds.head, decodeTuple(ds.tail)) // 5
  |      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  |      Found:    io.circe.Decoder[h *: t]
  |      Required: io.circe.Decoder[T]
```

이전에도 겪었던 타입 통합 실패와 같은 종류의 오류입니다. 패턴 매치의 첫 번째 분기에서 컴파일러는 `T = EmptyTuple`임을 알아야 하고, 두 번째 분기에서는 `T = h *: t`임을 알아야 하지만, 알아내지 못합니다.

안타깝게도 또다시 타입 꼼수에 의존해야 합니다. 이를 위해 컴파일러가 통합을 수행하도록 유도하는 작은 래퍼를 만들겠습니다:

```scala
import Tuple.*

case class IsDecoderMap[T <: Tuple](value: Map[T, Decoder])
```

이것은 단순히 `Map` 값을 감싸면서 `T` 타입 매개변수를 외부에 노출합니다. 타입 매개변수가 클래스에 나타나면 컴파일러가 통합할 수 있다는 아이디어입니다. 사용하려면 `Decoder` 튜플을 `IsDecoderMap`으로 감싸고 그것에 대해 매칭합니다:

```scala
inline def decodeTuple[T <: Tuple](decoders: Map[T, Decoder]): Decoder[T] =
  inline IsDecoderMap(decoders) match // 1
    case _: IsDecoderMap[EmptyTuple] => Decoder.const(EmptyTuple) // 2
    case ds: IsDecoderMap[h *: t] => // 3
      combineDecoders(ds.value.head, decodeTuple(ds.value.tail))
```

(1)에서 튜플을 감싸고 매칭합니다. (2)에서 `EmptyTuple` 케이스를 매칭하여 컴파일러가 `T = EmptyTuple`임을 인정하게 합니다. (3)에서는 비어있지 않은 케이스를 매칭하여 컴파일러가 `T = h *: t`를 추론하게 합니다.

놀랍게도 컴파일이 됩니다!

실제로 시험해 볼 수도 있습니다:

```scala
scala> val decoders = (Decoder[Email], Decoder[Phone], Decoder[Address])
       val decoder = decodeTuple[(Email, Phone, Address)](decoders)

       decoder.decodeJson(json)
val res10:
  Decoder.Result[(Email, Phone, Address)] = Right((
    Email(bilbo@baggins.com,Some(frodo@baggins.com)),
    Phone(555-555-00,88),
    Address(Shire,Hobbiton)))
```

`Decoder` 튜플을 직접 구성하여 `decodeTuple`에 전달합니다. 아쉽게도 Scala의 타입 추론이 올바른 `Map` 타입을 스스로 추론할 만큼 똑똑하지 않아서, `decodeTuple`에 타입 인자를 명시적으로 제공해야 합니다. 그 점만 빼면 완벽하게 작동합니다.

소환과 순회를 성공적으로 분리했으니, 실제로 필요한 `Decoder`를 어떻게 소환할까요?

### 소환의 시간

케이스 클래스용 플랫 `Decoder`를 만들 때 클래스의 `Mirror`에 접근할 수 있고, 클래스의 필드 타입은 `MirroredElemTypes`로 주어진다는 것을 떠올립시다. 이제 필요한 것은 `MirroredElemTypes`의 각 요소에 하나씩 대응하는 `Decoder` 튜플을 소환하는 것입니다.

그 튜플의 타입은 무엇일까요? 바로 `Map`이 해결해 줍니다. 원하는 타입은 다음과 같습니다:

```scala
Map[mirror.MirroredElemTypes, Decoder]
```

이 타입을 순회하면서 `summonInline`으로 `Decoder`를 하나씩 소환할 수도 있지만[23](#user-content-fn-summonexercise), 그럴 필요가 없습니다. 표준 라이브러리가 이미 해결책을 제공합니다:

```scala
scala.compiletime.summonAll
```

설명은 다음과 같습니다:

> Given a tuple `T`, summons each of its member types and returns them in a `Tuple`.

따라서 간단히 이렇게 하면 됩니다:

```scala
val decoders = summonAll[Map[mirror.MirroredElemTypes, Decoder]]
```

이것으로 이전과 같은 방식의 플랫 `Decoder`를 만드는 데 필요한 재료가 모두 갖춰집니다:

```scala
inline def makeDecoder[A <: Product](
    using mirror: Mirror.ProductOf[A]): Decoder[A] =

  val decoders = summonAll[Map[mirror.MirroredElemTypes, Decoder]] // 1

  decodeTuple(decoders).map(mirror.fromTuple) // 2
```

먼저 클래스 필드에 대한 모든 `Decoder`를 `summon`하고 (1), 이를 단일 플랫 `Decoder`로 조합합니다 (2).

### 이제 재사용 가능한가?

이전보다 훨씬 모듈적입니다. `Tuple.Map`과 `summonAll`은 다양한 맥락에서 활용할 수 있는 유용한 도구입니다. 하지만 `decodeTuple` 함수는 아직 최대한 재사용 가능한 상태는 아닙니다.

`decodeTuple` 함수를 더 개선하지는 않겠지만, 그 방향의 연습 문제를 몇 가지 남겨 두겠습니다.

1. `encodeTuple` 함수도 예전 `decodeTuple`과 같은 모듈성 부족 문제를 안고 있습니다. 비슷한 방식으로 재구현할 수 있습니다. 다음 시그니처를 구현해 보세요[24](#user-content-fn-implementedintherepo):

```scala
inline def encodeTuple[T <: Tuple](encoders: Map[T, Encoder.AsObject]): Encoder.AsObject[T]
```

다음 헬퍼 정의가 필요합니다:

```scala
val emptyEncoder: Encoder.AsObject[EmptyTuple]
def combineObjectEncoders[H, T <: Tuple](eh: Encoder.AsObject[H], et: Encoder.AsObject[T]): Encoder.AsObject[H *: T]
```
2. `decodeTuple`은 함수형 프로그래밍에서 유명한 `sequence` 함수와 구조가 매우 비슷합니다[25](#user-content-fn-listanalogy). `decodeTuple`을 일반화하여 다음 시그니처를 구현해 보세요[26](#user-content-fn-cats):

```scala
def sequenceTuple[T <: Tuple, F[_]: cats.Applicative](fs: Map[T, F]): F[T]
```
3. `sequenceTuple`을 이용하여 `decodeTuple`을 재구현해 보세요. `sequenceTuple`의 다른 활용 사례도 생각해 볼 수 있을까요?
4. `encodeTuple`의 새 구현은 `decodeTuple`과 매우 비슷합니다. 안타깝게도 `sequenceTuple`로는 `encodeTuple`을 처리할 수 없습니다[27](#user-content-fn-contravariant). `sequenceTuple`을 더 일반화할 수 있습니다. 다음 시그니처를 구현해 보세요:

```scala
def sequenceInvariantTuple[T <: Tuple, F[_]: cats.InvariantMonoidal](fs: Map[T, F]): F[T]
```
5. `sequenceInvariantTuple`을 이용하여 `encodeTuple`을 재구현해 보세요.
6. `sequenceInvariantTuple`을 이용하여 `sequenceTuple`을 재구현해 보세요.

연습 문제를 모두 풀었다면 축하합니다! 매우 재사용 가능한 함수들을 손에 넣은 것입니다.

이 파트의 전체 코드는 [repo](https://github.com/ncreep/scala3-flat-json-blog/blob/master/flat_modular.scala)에서 확인할 수 있습니다.

## 안전이 최우선

재사용성이라는 주제에서 잠시 벗어나, 다시 평탄한 JSON 문제로 돌아왔습니다. 이제 마지막 안전 요구사항 하나가 남아 있습니다.

> 인라인된 클래스에 중복 필드 이름이 있으면 컴파일에 실패할 것

이것이 왜 안전 문제일까요? 다음과 같은 대안 모델이 있다고 가정해 봅시다.

```scala
case class User2(email: Email2, phone: Phone2, address: Address2)

case class Email2(primary: String, secondary: Option[String],
                  lastUpdate: Long, verified: Boolean)

case class Phone2(number: String, prefix: Int, verified: Boolean)

case class Address2(country: String, city: String, lastUpdate: Date)

case class Date(value: Long)

object Email2:
  given Codec.AsObject[Email2] = semiauto.deriveCodec[Email2]

object Phone2:
  given Codec.AsObject[Phone2] = semiauto.deriveCodec[Phone2]

object Address2:
  given Codec.AsObject[Address2] = semiauto.deriveCodec[Address2]

object Date:
  given Codec.AsObject[Date] = semiauto.deriveCodec[Date]
```

`User2`의 평탄한 JSON 포맷을 만들면 어떤 일이 벌어질까요? 이렇게 됩니다.

```scala
scala> val user2 = User2(
         Email2(primary = "bilbo@baggins.com", secondary = Some("frodo@baggins.com"),
                lastUpdate = 21, verified = true),
         Phone2(number = "555-555-00", prefix = 88, verified = false),
         Address2(country = "Shire", city = "Hobbiton", lastUpdate = Date(55)))
       val codec = makeCodec[User2]
       val json2 = codec(user2)
val json2: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "lastUpdate" : {
    "value" : 55
  },
  "verified" : false,
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}
```

`lastUpdate` 필드가 두 개 있고 값도 각각 다르지만(`21`과 `55`), 최종 JSON에는 하나의 값(`55`)만 남습니다. 중복된 `verified` 필드도 마찬가지입니다. 정보가 조용히 유실된 셈입니다.

더 심각한 건 이 JSON을 다시 읽으려고 하면 다음과 같은 결과가 나온다는 점입니다.

```scala
scala> codec.decodeJson(json2)
val res14: Decoder.Result[User2] = Left(DecodingFailure at .lastUpdate: Long)
```

읽기가 실패합니다. `Email2` 클래스의 첫 번째 `lastUpdate` 필드를 읽으려 하는데, 이 필드는 `Long` 타입이어야 합니다. 그런데 실제로 기록된 값은 `Address2`에서 온 것이고, 호환되지 않는 `Date` 타입입니다. 결국 아무 경고도 없이 런타임 오류가 발생합니다. `verified` 필드는 읽기 자체는 성공하지만, 둘 중 하나에 잘못된 정보가 들어가게 됩니다.

안전하지 않습니다. 전혀요.

이런 오류를 컴파일 타임에 방지할 마지막 안전 요구사항을 구현해 봅시다.

### 수많은 Mirror

주의

이 부분은 글 전체에서 가장 이해하기 어려운 동시에 가장 실험적인 내용입니다. 무사히 빠져나갈 수 있기를 바랍니다...

중복 필드를 검사하려면 먼저 기록할 모든 필드의 목록을 얻어야 합니다. 각 타입의 `Mirror`가 이 정보를 제공할 수 있습니다. 관련된 `Mirror`를 모두 소환해 봅시다.

```scala
scala> val emailMirror = summon[Mirror.Of[Email]]
       val phoneMirror = summon[Mirror.Of[Phone]]
       val addressMirror = summon[Mirror.Of[Address]]
val emailMirror:
  Mirror.Product{
    type MirroredMonoType = Email;
    type MirroredType = Email;
    type MirroredLabel = "Email";
    type MirroredElemTypes = (String, Option[String]);
    type MirroredElemLabels = ("primary", "secondary")
  } = Email$@17504660
val phoneMirror:
  Mirror.Product{
    type MirroredMonoType = Phone;
    type MirroredType = Phone;
    type MirroredLabel = "Phone";
    type MirroredElemTypes = (String, Int);
    type MirroredElemLabels = ("number", "prefix")
  } = Phone$@699053cb
val addressMirror:
  Mirror.Product{
    type MirroredMonoType = Address;
    type MirroredType = Address;
    type MirroredLabel = "Address";
    type MirroredElemTypes = (String, String);
    type MirroredElemLabels = ("country", "city")
  } = Address$@263df135
```

내부 타입 `MirroredElemLabels`에는 필드 이름이 **타입**으로 들어 있습니다. 문자열 리터럴 타입입니다. 타입 이름이 담긴 `MirroredLabel` 타입도 마찬가지입니다.

여기서 주목할 점은 각 케이스마다 정제(refine)된 타입을 다루고 있다는 것입니다. `MirroredElemLabels` 타입은 `Mirror`에서는 추상적이고, 해당 클래스의 given 인스턴스를 소환해야 비로소 완전히 정의됩니다. 따라서 다양한 컴파일 타임 조작을 수행하는 동안 타입 정보를 절대 잃어버리지 않는 것이 핵심입니다. 이후 나오는 코드에서 복잡성의 주된 원인이 바로 이것입니다.

유용한 오류 메시지를 만들려면 필드 이름과 타입 이름 모두 접근할 수 있어야 합니다. 그래야 사용자가 중복 필드의 이름과 출처를 모두 파악할 수 있습니다. 유용한 오류 메시지 제공이 원래 요구사항 중 하나였다는 점을 떠올려 주세요. 이를 위해 `MirroredLabel`과 `MirroredElemLabels`를 함께 모아야 합니다. 헬퍼 래퍼를 정의합시다.

```scala
trait Labelling[Label <: String, ElemLabels <: Tuple]
```

각 `Mirror` 인스턴스에서 `MirroredLabel`[28](#user-content-fn-typebound)과 `MirroredElemLabels`를 추출하여 `Labelling`의 타입 인자로 넣겠다는 아이디어입니다[29](#user-content-fn-designchoice). 우리 목표는 다음과 같은 튜플 타입을 구성하는 것입니다[30](#user-content-fn-notvalue).

```scala
Labelling[emailMirror.MirroredLabel, emailMirror.MirroredElemLabels] *:
  Labelling[phoneMirror.MirroredLabel, phoneMirror.MirroredElemLabels] *:
  Labelling[addressMirror.MirroredLabel, addressMirror.MirroredElemLabels] *:
  EmptyTuple
```

타입 별칭을 풀어 쓰면 다음과 동일합니다.

```scala
Labelling["Email", ("primary", "secondary")] *:
Labelling["Phone", ("number", "prefix")] *:
Labelling["Address", ("country", "city")] *:
EmptyTuple
```

이 타입을 계산할 수 있다면, 이를 처리하여 중복 필드 이름을 검사하고 필요하면 적절한 오류를 발생시킬 수 있습니다.

그런데 여기서 약간의 문제가 생깁니다.

### 값에서 타입으로

`Mirror`는 **값**이지만 우리가 필요한 결과는 **타입**입니다. 지금까지는 (`inline`을 사용하여) **타입**으로부터 **값**을 계산하기만 했습니다. 반대 방향으로는 어떻게 할까요?

[이](https://github.com/lampepfl/dotty/pull/4768/files#diff-c8a7eb6800a0b83897e79c60b4d1364aa27fc962774f9e68d7879d75d3e59690R221) 오래된 설계 문서의 접근 방식에서 영감을 받아, 다음과 같은 헬퍼를 정의합니다.

```scala
class Typed[A]:
  type Value = A
```

`Typed`의 인스턴스는 어떤 **타입**을 타입 인자로 캡처하는 **값**입니다. 예를 들어 앞의 예시를 다음과 같이 `Typed` 값으로 "계산"할 수 있습니다.

```scala
val typedResult = Typed[
  Labelling[emailMirror.MirroredLabel, emailMirror.MirroredElemLabels] *:
  Labelling[phoneMirror.MirroredLabel, phoneMirror.MirroredElemLabels] *:
  Labelling[addressMirror.MirroredLabel, addressMirror.MirroredElemLabels] *:
  EmptyTuple]
```

`typedResult`는 다른 값들로부터 계산되어 함수에서 반환될 수 있는 **값**입니다. 그 과정에서 우리가 필요한 `Labelling` 정보를 담은 타입 인자를 캡처합니다. 계산이 끝나면 타입 멤버 `Value`로 이 타입을 추출하여 오류 검증에 활용할 수 있습니다. 이제 과제는 타입 정보를 잃지 않으면서 적절한 `Typed` 값을 어떻게 계산하느냐입니다.

의사 코드로 해결 방안을 스케치해 봅시다.

- 타입의 튜플이 주어지면,
- 튜플의 각 타입마다 해당 타입의 `Mirror` **값**을 소환합니다.
- `Mirror` 인스턴스를 `Typed` **값**으로 감싼 `Labelling` **타입**으로 변환합니다.
- `Typed` **값**들의 **타입** 매개변수를 하나의 튜플로 합쳐 결합합니다.

값과 타입 사이를 왔다 갔다 해야 하는 모습이 보이시나요? 이것이 바로 우리의 도전 과제입니다.

모듈성에 관한 의문이 생길 수도 있습니다. 다시 한번 반복과 소환을 섞고 있으니까요. 이 경우에는 어쩔 수 없습니다. 제가 아는 한, 각 `Mirror` 인스턴스에 존재하는 정밀한 타입 정보를 잃지 않으면서 `summonAll`(이나 커스텀 소환 함수)을 쓸 방법이 없습니다[31](#user-content-fn-cantdo). 타입의 정밀도를 유지하는 것이 핵심이라는 점을 기억하세요.

무엇을 해야 하는지 알았으니, 첫 단계로 타입 시그니처를 정의합시다.

```scala
inline def makeLabellings[T <: Tuple]: Typed[_ <: Tuple]
```

이것은 `Tuple` 형태의 타입들이 주어지면, 그 안에 어떤 `Tuple`을 타입 인자로 담은 `Typed` 값을 계산하겠다는 뜻입니다. 매우 모호한 타입 시그니처인데, 반환 타입을 더 정밀하게 명시할 수 있을까요?

`Tuple.Map`과 비슷한 것을 만들어서 모든 튜플 원소가 `Labelling[_, _]` 타입이라고 명시할 수는 있습니다. 하지만 크게 개선되지는 않고 정의만 복잡해질 뿐입니다. 진짜 정보는 각 클래스마다 즉석에서 계산되는 문자열 리터럴 타입 안에 있으며, 이 정보는 소환하는 `given` 값에서만 찾을 수 있습니다. 관련 타입이 값 내부에 존재하므로, 타입 `T`와 출력 `Labelled` 타입 사이의 관계를 캡처하는 타입을 정의할 방법은 (제가 아는 한[32](#user-content-fn-letmeknow)) 없습니다.

이 시그니처를 그대로 사용하고 초기 구현을 시도해 봅시다.

```scala
inline def makeLabellings[T <: Tuple]: Typed[_ <: Tuple] =
  inline erasedValue[T] match // 1
    case EmptyTuple => Typed[EmptyTuple] // 2
    case _: (h *: t) => // 3
      val headMirror = summonInline[Mirror.ProductOf[h]] // 4
      type headLabelling = // 5
        Labelling[headMirror.MirroredLabel, headMirror.MirroredElemLabels]

      val tailLabellings = makeLabellings[t] // 6

      Typed[headLabelling *: tailLabellings.Value] // 7
```

하나씩 살펴봅시다.

- `T` 타입에 매치합니다(1). 값이 아닌 타입만 있으므로 `erasedValue`를 다시 사용합니다.
- 빈 경우(2)에는 `EmptyTuple` **타입**을 감싼 `Typed` **값**을 생성합니다.
- 비어 있지 않은 경우(3), 튜플의 head인 `h`의 `Mirror`를 소환합니다(4).
- 그다음 head `Mirror` **값**에서 `MirroredLabel`과 `MirroredElemLabels`를 추출하여 `Labelling`으로 구성한 새 **타입**을 만듭니다(5).
- 튜플의 tail `t`의 labelling을 재귀적으로 계산합니다(6).
- 마지막으로 head `Labelling` **타입**과 tail의 `Labelling` **타입**을 합쳐 새 `Typed` **값**을 만듭니다(7).

다소 놀랍게도, 이 코드가 첫 시도에 컴파일됩니다. 실행해 봅시다.

```scala
scala> makeLabellings[(Email, Phone, Address)]
val res24: Typed[? <: Tuple] = Typed@111494bc
```

이것도 컴파일됩니다. 이상하고 수상합니다... 물론, 결과를 자세히 보면 반환된 타입이 쓸모없다는 걸 알 수 있습니다. 돌아온 건 `Typed[? <: Tuple]`뿐이라, 캡처한 `Mirror`에 관한 세부 정보가 전혀 없습니다. 흠...

곰곰이 생각해 보면 놀라운 결과가 아닙니다. `makeLabellings`의 정적 반환 타입이 `Typed[_ <: Tuple]`이므로, 매 재귀 단계에서 수집한 정밀한 타입 정보를 잃고 익명 `Tuple` 값을 반환하게 됩니다. 막다른 길입니다. `makeLabellings`에 충분히 정밀한 정적 타입을 지정할 수 없는데, 정밀한 타입 없이는 안전 요구사항을 구현할 수 없습니다.

놀랍지 않게도, Scala 3에는 이 문제의 해결책이 있습니다: [transparent inline](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html#transparent-inline-methods-1). 문서에서 인용하면:

> 인라인 메서드를 추가로 `transparent`로 선언할 수 있습니다. 이렇게 하면 인라인 메서드의 반환 타입이 전개 시 더 정밀한 타입으로 특수화될 수 있습니다.

`transparent`가 하는 일을 간단한 예시로 확인해 봅시다.

```scala
scala> transparent inline def stuff: Tuple = (1, "b", true)
       val tuple = stuff
def stuff: Tuple
val tuple: (Int, String, Boolean) = (1,b,true)
```

`stuff`의 정적 타입이 `Tuple`로 선언되었음에도, 실제로 `stuff`를 호출하여 값 `tuple`에 할당하면 컴파일러가 선언된 것보다 더 정밀한 타입 `(Int, String, Boolean)`을 보여줍니다. 컴파일러가 코드를 인라인할 때 선언된 것보다 더 정밀한 타입을 보게 되고, 이 특수화된 타입을 결과 값에 부여합니다. 인라인 과정(컴파일 타임)에서 타입이 보이기만 하면 컴파일러가 그것을 보존합니다.

이제 `makeLabellings`를 `transparent`로 표시하여, 매 재귀 호출에서 얻는 정밀한 정적 타입을 보존할 수 있습니다.

```scala
transparent inline def makeLabellings[T <: Tuple]: Typed[_ <: Tuple] =
  inline erasedValue[T] match
    case EmptyTuple => Typed[EmptyTuple]
    case _: (h *: t) =>
      val headMirror = summonInline[Mirror.ProductOf[h]]
      type headLabelling =
        Labelling[headMirror.MirroredLabel, headMirror.MirroredElemLabels]

      val tailLabellings = makeLabellings[t]

      Typed[headLabelling *: tailLabellings.Value]
```

여전히 컴파일됩니다. 실행해 봅시다.

```scala
scala> makeLabellings[(Email, Phone, Address)]
val res25:
  Typed[? >: Nothing *: Nothing <: Labelling[? <: String, ? <: Tuple] *: Tuple] = Typed@265627ac
```

이건... 이상합니다... 이전보다는 확실히 더 정밀한 타입을 얻었습니다. 노이즈를 무시하면 다음과 같습니다.

```scala
Labeling[_, _] *: Tuple
```

단순한 `Tuple`보다는 확실히 낫습니다. `transparent`가 어느 정도 작동하는 것 같지만, 우리 목적에는 아직 부족합니다.

솔직히, 왜 코드가 기대대로 동작하지 않는지 모르겠습니다. 더 많은 해킹이 필요한 시점입니다.

무엇이 일어났는지 "디버깅"하려고 보니, tail에서 얻은 `Labelling`의 정밀한 타입을 보존하지 못한 것 같습니다. 반환 타입의 `Tuple` 부분이 바로 그것입니다. head의 `Labelling` 타입도 보존되지 않았고, 그것이 `Labelling[_, _]`로 나타난 것입니다.

여기서 제가 가진 일반적인 직관은 컴파일러가 더 정밀하도록 강제해야 한다는 것입니다. 이어지는 코드는 수많은 시행착오 끝에 나온 결과물입니다. 이 코드가 왜 작동하는지 명쾌하게 설명할 수 없는데, 상당 부분이 `transparent inline`의 타이퍼(typer) 코드에 존재하는 문제점들을 우회하기 위한 것이라고 짐작합니다[33](#user-content-fn-transparentissues).

```scala
transparent inline def makeLabellings[T <: Tuple]: Typed[_ <: Tuple] =
  inline erasedValue[T] match
    case EmptyTuple => Typed[EmptyTuple]
    case _: (h *: t) =>
      inline makeLabellings[t] match // 1
        case tailLabellings =>
          inline summonInline[Mirror.Of[h]] match // 2
            case headMirror =>
              type HeadLabelling = Labelling[headMirror.MirroredLabel, headMirror.MirroredElemLabels]

              Typed[Prepend[HeadLabelling, tailLabellings.Value]] // 3

type Prepend[X, +Y <: Tuple] <: Tuple = X match
  case X => X *: Y
```

이 리팩터링을 간략히 설명합니다.

- (1)에서 tail의 `makeLabellings` 결과를 `val`에 직접 대입하는 대신 `inline match`를 사용합니다. 이렇게 하면 컴파일러가 표현식의 정밀한 타입을 유지하게 되는 것 같습니다[34](#user-content-fn-noop). `val`에 대입하면 타입 정보가 소실됩니다.
- (2)에서 head에 소환한 `Mirror`에도 같은 방식을 적용합니다.
- 이유는 불분명하지만 이 match의 순서가 중요합니다. 순서를 바꾸면 타입 정밀도가 다시 소실됩니다.
- (3)에서 최종 결과를 구성하지만, 튜플을 만들 때 `*:` 대신 `Prepend`를 사용합니다. `Prepend`는 본질적으로 (단순한) match type으로 정의된 `*:`의 별칭입니다. 어떤 이유에서인지 `*:`를 직접 사용하면 타입 정밀도가 소실됩니다[35](#user-content-fn-differentissue).

전체가 매우 취약합니다[36](#user-content-fn-complexity). 겉보기에 무해한 코드 수정이 다양하고 예상치 못한 방식으로 깨집니다[37](#user-content-fn-tests). 이런 종류의 계산(즉, 값에서 타입을 계산하는 작업)에는 Scala 2 스타일의 "implicit을 활용한 Prolog"이 실제로 더 견고하며, 아무 문제 없이 동작합니다[38](#user-content-fn-implicitlabelling). Scala 3의 이 새로운 영역은 아직 성숙하지 않았습니다. 시간이 지나면 개선되기를 바랍니다.

어찌 되었든, 동작합니다!

```scala
scala> makeLabellings[(Email, Phone, Address)]
val res0:
  Typed[
   (Labelling["Email", ("primary", "secondary")],
    Labelling["Phone", ("number", "prefix")],
    Labelling["Address", ("country", "city")])] = Typed@40edc64e
```

이 아름답고 정밀한 타입을 보세요! 중복 필드를 검사하고 유용한 오류를 발행하기 위해 필요한 모든 타입 정보가 담겨 있습니다.

사용 중인 mirror에서 이 정보를 직접 가져올 수 있습니다.

```scala
scala> val mirror = summon[Mirror.Of[User]]
       makeLabellings[mirror.MirroredElemTypes]
val res5:
  Typed[(
    Labelling["Email2", ("primary", "secondary")],
    Labelling["Phone2", ("number", "prefix")],
    Labelling["Address2", ("country", "city")])] = Typed@47927528
```

이 부분의 전체 코드는 [저장소](https://github.com/ncreep/scala3-flat-json-blog/blob/master/labelling.scala)에서 확인할 수 있습니다.

이제 진짜 재미있는 부분에 들어갈 준비가 되었습니다.

### 본격적인 타입 레벨 프로그래밍

지금까지 모든 타입 레벨 조작에는 일정한 값 레벨 요소가 관여했습니다. 항상 어딘가에 `inline def`가 숨어 있었습니다. 이제 labelling을 순수 타입으로 얻었으니, 그 위에서 본격적인 타입 레벨 조작을 수행합니다. 순수 타입만 사용하고 다른 것은 아무것도 쓰지 않습니다. 이것이 Scala 3의 타입 레벨 지원이 가장 빛나는 대목입니다.

계획은 다음과 같습니다.

- 대상 타입(예: `User`의 필드)의 전체 `Labelling` 목록이 주어지면,
- 겹치는 필드 이름의 목록을 계산합니다.
- 목록이 비어 있지 않으면 컴파일 오류를 발생시킵니다.

이 문제는 `Mirror`에서 타입 정보를 추출하는 문제보다 어떤 면에서는 더 쉽습니다. 하나의 타입에서 다른 타입을 계산하는 것이니까요. 따라서 앞서 사용한 `Typed` 트릭이 필요 없고, 타입만으로 직접 작업할 수 있습니다. 이런 조작의 핵심 도구는 앞서 만났던 match type입니다[39](#user-content-fn-alternative).

match type이 수행할 수 있는 패턴 매칭과 재귀적 축약 덕분에, match type은 사실상 타입을 다루는 하나의 언어를 형성합니다. 매우 원시적인 언어이긴 합니다. 컴파일 오류 메시지가 부실하고, IDE 지원도 거의 없으며, 리스트 대신 `Tuple`을 쓰고, 그 밖에 구조라 할 만한 것이 별로 없습니다. 초창기 Scala와 비슷한 느낌이랄까... 그럼에도 이 언어는 꽤 표현력이 좋습니다. 곧 확인해 보겠습니다.

중복 목록을 계산하는 구체적인 알고리즘 자체는 그다지 흥미롭지 않습니다. 중요한 것은 순수 타입으로 표현할 수 있다는 점입니다.

알고리즘은 다음과 같습니다.

- labelling 목록이 주어지면, 각 labelling마다:
- 레이블을 출처(해당 클래스)와 짝을 지어, 쌍의 목록을 만듭니다.
- 레이블별로 출처를 그룹화합니다.
- 출처가 둘 이상인 레이블만 필터링합니다.
- 남은 레이블이 곧 중복입니다.

작성할 코드에는 직관적인 값 레벨 대응물이 있으며, [여기](https://github.com/ncreep/scala3-flat-json-blog/blob/master/value_level_find_duplicates.scala)에서 확인할 수 있습니다[40](#user-content-fn-notthebest). 구현할 알고리즘의 더 정확한 감을 잡고 싶다면 살펴보세요.

### 중복 찾기

먼저 각 필드 레이블을 출처(해당 클래스)와 짝짓는 방법이 필요합니다. 간단하지 않은 타입 변환이 필요할 때면 match type을 정의하거나 활용하면 됩니다.

```scala
type ZipWithSource[L] <: Tuple = L match // 1
  case Labelling[label, elemLabels] => // 2
    ZipWithConst[elemLabels, label] // 3

type ZipWithConst[T <: Tuple, A] =
  T Map ([t] =>> (t, A)) // 4
```

여기서:

- `ZipWithSource`(1)는 `Labelling`을 인자로 받습니다. 타입 바운드가 결과가 어떤 `Tuple`이 될 것임을 나타냅니다.
- `Labelling`에 매치하여(2) 클래스 레이블(`label`)과 필드 레이블(`elemLabels`)로 분해합니다.
- 레이블에 `ZipWithConst` 타입 "함수"를 적용합니다(3).
- `ZipWithConst`는 앞서 본 `Map`의 중위 적용에 대한 별칭입니다(4). `Map`은 `List.map`의 `Tuple` 버전입니다. 중위 형태로 적용하니 "컬렉션" 느낌이 납니다.
- `ZipWithConst`는 튜플과 고정 타입 `A`를 인자로 받습니다.
- 튜플의 각 원소에 적용하는 "함수"는 [타입 람다](https://docs.scala-lang.org/scala3/reference/new-types/type-lambdas.html)로, `=>>`로 표기합니다. 타입 람다는 타입 레벨의 익명 함수입니다. 여기서는 원래 튜플의 각 원소를 `A`와 짝지어 줍니다.

`ZipWithSource`를 "실행"해 봅시다.

```scala
scala> type L = Labelling["Email2", ("primary", "secondary", "lastUpdate", "verified")]
       type Res = ZipWithSource[L]
// defined alias type Res
   = (("primary", "Email2"),
      ("secondary", "Email2"),
      ("lastUpdate", "Email2"),
      ("verified", "Email2"))
```

특정 `Labelling`에 `ZipWithSource`를 적용하면 쌍의 `Tuple`이 반환됩니다. 각 원소는 필드 이름과 그 클래스 출처입니다.

이제 `Labelling` 튜플 전체에 `ZipWithSource`를 적용할 수 있습니다.

```scala
type ZipAllWithSource[Labellings <: Tuple] = Labellings FlatMap ZipWithSource
```

`Labelling` 튜플을 받아 각각에 `ZipWithSource`를 적용한 뒤 결과를 평탄화합니다. 여기서 중위 형태로 사용된 `FlatMap`은 `List.flatMap`의 튜플 버전이며, 예상하는 것과 정확히 같은 일을 합니다.

```scala
scala> type Labellings = (
           Labelling["Email2", ("primary", "secondary", "lastUpdate", "verified")],
           Labelling["Phone2", ("number", "prefix", "verified")],
           Labelling["Address2", ("country", "city", "lastUpdate")])

       type Res = ZipAllWithSource[Labellings]
// defined alias type Res
   = (("primary", "Email2"), ("secondary", "Email2"), ("lastUpdate", "Email2"),
      ("verified", "Email2"), ("number", "Phone2"), ("prefix", "Phone2"),
      ("verified", "Phone2"), ("country", "Address2"), ("city", "Address2"),
      ("lastUpdate", "Address2"))
```

꽤 괜찮습니다. 실제 언어로 프로그래밍하는 것 같은 느낌입니다. Scala 2 시절처럼 이상한 implicit 해결 트릭을 쓸 필요가 없습니다.

이제 쌍으로 이루어진 긴 튜플이 있습니다. 각 쌍은 필드 이름과 그 필드가 속한 클래스입니다. 이것들이 모두 **타입**이라는 점을 주목하세요. 겉보기와 달리 여기에는 값이 없고, 문자열 타입 리터럴만 있습니다.

진행하기 전에 유틸리티 함수 몇 가지가 필요합니다.

```scala
import scala.compiletime.ops.boolean.* // 1
import scala.compiletime.ops.any.*

type FindLabel[Label, T <: Tuple] = // 2
  T Filter ([ls] =>> HasLabel[Label, ls]) Map Second // 3

type RemoveLabel[Label, T <: Tuple] = // 4
  T Filter ([ls] =>> ![HasLabel[Label, ls]]) // 5

type HasLabel[Label, LS] <: Boolean = LS match // 6
  case (l, s) => Label == l // 7

type Second[T] = T match // 8
  case (a, b) => b
```

- 먼저 중요한 import(1)가 있습니다. [scala.compiletime](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#the-scalacompiletime-package-1) 패키지에는 타입을 일반 값처럼 다룰 수 있게 해 주는 매우 유용한 유틸리티가 있습니다. 여기서는 불리언 연산과 `any` 연산을 import하는데, 등가 비교에 필요합니다.
- `FindLabel` 타입 "함수"(2)는 레이블과 튜플을 받습니다.
- `List.filter`에 해당하는 `Filter` "함수"(3)를 사용하여 입력 `Label`을 가진 원소만 남깁니다. 여기서도 익명 함수 역할의 타입 람다를 사용합니다. 결과가 쌍의 튜플이므로, `Second` 함수를 `Map`하여 쌍의 두 번째 원소(레이블의 출처)만 추출합니다.
- `RemoveLabel`(4)도 비슷하게 제공된 레이블을 가지지 않는 쌍을 걸러냅니다(5). 불리언 타입 리터럴에 적용한 타입 `!`(`compiletime.ops.boolean`에서 옴)에 주목하세요.
- `HasLabel` 타입 정의(6)는 레이블과 레이블-출처 쌍을 받습니다.
- 쌍을 분해(7)하여 레이블을 추출하고, `==`로 추출된 레이블과 제공된 `Label` 인자를 비교합니다. 이 `==`는 `compiletime.ops.any` 패키지에서 오는 타입 비교이며, 두 타입 리터럴이 같은지 검사하여 타입 레벨 불리언을 생성합니다. 우리의 경우 문자열 타입 리터럴을 비교하는 것입니다.
- `Second` match type(8)은 쌍에 매치하여 두 번째 원소를 추출합니다.

일반적인 값 레벨 컬렉션을 다루는 것과 같은 느낌입니다. 실행해 봅시다.

```scala
scala> type Labels =(("primary", "Email2"), ("secondary", "Email2"), ("lastUpdate", "Email2"),
                     ("verified", "Email2"), ("number", "Phone2"), ("prefix", "Phone2"),
                     ("verified", "Phone2"), ("country", "Address2"), ("city", "Address2"),
                     ("lastUpdate", "Address2"))

       type Res1 = FindLabel["verified", Labels]
       type Res2 = RemoveLabel["verified", Labels]
// defined alias type Res1
   = ("Email2", "Phone2")
// defined alias type Res2
   = (("primary", "Email2"), ("secondary", "Email2"), ("lastUpdate", "Email2"),
      ("number", "Phone2"), ("prefix", "Phone2"), ("country", "Address2"),
      ("city", "Address2"), ("lastUpdate", "Address2"))
```

첫 번째 호출에서는 `verified` 레이블의 모든 출처를 찾고, 두 번째에서는 `verified` 레이블을 포함하는 모든 튜플을 제거합니다[41](#user-content-fn-cheating).

다음은 핵심 기능인 레이블별 그룹화입니다. `List.groupBy`와 비슷하지만, 문자열 리터럴 **타입**으로 작업한다는 제약이 있습니다. 타입으로 할 수 있는 것이 매우 제한적이며, 이 경우에는 등가 비교만 가능합니다[42](#user-content-fn-efficient). 코드는 다음과 같습니다.

```scala
type GroupByLabels[Labels <: Tuple] <: Tuple = Labels match
  case EmptyTuple => EmptyTuple
  case (label, source) *: t => // 1
    ((label, source *: FindLabel[label, t])) *: GroupByLabels[RemoveLabel[label, t]] // 2
```

비어 있지 않은 분기(1)에서 레이블과 출처 쌍에 매치합니다. 그다음 새로운 쌍(2)을 만드는데, 해당 레이블, 현재 출처, 그리고 tail에 있는 해당 레이블의 모든 출처(`FindLabel` 사용)를 합칩니다. 그리고 tail에서 해당 레이블을 제거(`RemoveLabel`)한 뒤, 남은 것으로 재귀적으로 그룹화를 계속합니다.

테스트해 봅시다.

```scala
scala> type Res = GroupByLabels[Labels]
// defined alias type Res
   = (("primary", "Email2" *: EmptyTuple),
      ("secondary", "Email2" *: EmptyTuple),
      ("lastUpdate", ("Email2", "Address2")),
      ("verified", ("Email2", "Phone2")),
      ("number", "Phone2" *: EmptyTuple),
      ("prefix", "Phone2" *: EmptyTuple),
      ("country", "Address2" *: EmptyTuple),
      ("city", "Address2" *: EmptyTuple))
```

결과를 보면, 첫 번째 원소가 레이블이고 두 번째가 출처 튜플인 쌍들이 있습니다. 대부분의 레이블은 출처가 하나뿐이지만, 일부는 두 개입니다. 이것이 바로 안전하지 않은 필드 이름입니다. 거의 다 왔습니다...

```scala
import scala.compiletime.ops.int.* // 1

type OnlyDuplicates[Labels <: Tuple] = Labels Filter ([t] =>> Size[Second[t]] > 1) // 2

type FindDuplicates[Labellings <: Tuple] =
  OnlyDuplicates[GroupByLabels[ZipAllWithSource[Labellings]]] // 3

type Size[T] <: Int = T match // 4
  case EmptyTuple => 0 // 5
  case x *: xs => 1 + Size[xs] // 6
```

여기서:

- `compiletime.ops` 유틸리티를 추가로 import합니다(1). 이번에는 타입 레벨 정수 리터럴을 다루기 위한 것입니다. 타입 레벨에서 정수를 비교할 수 있게 해 줍니다.
- `OnlyDuplicates`(2)는 `GroupLabels`의 결과를 받아 출처 튜플의 크기가 1보다 큰 쌍만 필터링합니다. 이것이 문제가 되는 필드입니다.
- 타입 레벨에서 동작하는 `>`에 주목하세요. 값 레벨과 유사한 분위기를 줍니다.
- `FindDuplicates`(3)는 모든 것을 하나로 묶습니다. `Labelling` 튜플을 받아 `ZipWithSource`, `GroupByLabel`, 마지막으로 `OnlyDuplicates`를 거쳐 중복만 남깁니다.
- `Size` 타입(4)에는 작은 문제가 있습니다. `Tuple.Size`가 이미 존재하지만, 직접 정의해야 합니다. `Tuple.Size`는 입력이 `Tuple`의 하위 타입이어야 한다고 제약하는데, 이는 합리적입니다. 하지만 `Size`를 `Filter` 호출 내부에서 사용합니다. 안타깝게도 `Filter`의 타입이 `Size`의 입력이 실제로 튜플이라는 것을 "알기"에는 충분히 정밀하지 않습니다.
- 그래서 직접 정의한 버전은 더 느슨한 타입을 받습니다. 아무 입력이나 받은 다음 튜플이라고 가정하고 매치합니다.
- 빈 경우(5) 결과는 타입 레벨 `0`입니다.
- 비어 있지 않은 경우 tail에 재귀하고 결과에 `+ 1`합니다. 역시 모든 것이 타입 레벨에서 일어납니다.
- 이것이 예전에 작성했던 튜플의 값 레벨 `size` 정의를 얼마나 깔끔하게 반영하는지 주목하세요. 타입 레벨 함수가 값 레벨 함수를 그대로 반영하는 것은 실제로 매우 흔한 패턴입니다.

이것으로 끝입니다. 이제 `Labelling` 묶음에서 중복 필드를 찾을 수 있습니다[43](#user-content-fn-cheatagain).

```scala
scala> type Res = FindDuplicates[Labellings]
// defined alias type Res
   = (("lastUpdate", ("Email2", "Address2")),
      ("verified", ("Email2", "Phone2")))
```

대성공! 중복 필드와 함께 해당 필드가 속한 클래스 이름까지 찾아냈습니다. 컴파일 타임에 얻어낸 이 정보로 컴파일 오류를 생성할 수 있습니다.

`FindDuplicates`의 전체 코드는 [저장소](https://github.com/ncreep/scala3-flat-json-blog/blob/master/find_duplicates.scala)에서 확인할 수 있습니다.

### 타입 레벨 관찰

계속 진행하기 전에, 지금까지 한 작업을 정리해 봅시다. 중복 필드를 찾는 전체 코드입니다:

```scala
type FindDuplicates[Labellings <: Tuple] = OnlyDuplicates[GroupByLabels[ZipAllWithSource[Labellings]]]

type ZipAllWithSource[Labellings <: Tuple] = Labellings FlatMap ZipWithSource

type GroupByLabels[Labels <: Tuple] <: Tuple = Labels match
  case EmptyTuple => EmptyTuple
  case (label, source) *: t =>
    ((label, source *: FindLabel[label, t])) *: GroupByLabels[RemoveLabel[label, t]]

type OnlyDuplicates[Labels <: Tuple] =
  Labels Filter ([t] =>> Size[Second[t]] > 1)

type FindLabel[Label, T <: Tuple] =
  T Filter ([ls] =>> HasLabel[Label, ls]) Map Second

type RemoveLabel[Label, T <: Tuple] =
  T Filter ([ls] =>> ![HasLabel[Label, ls]])

type HasLabel[Label, LS] <: Boolean = LS match
  case (l, s) => Label == l

type ZipWithSource[L] <: Tuple = L match
  case Labelling[label, elemLabels] => ZipWithConst[elemLabels, label]

type ZipWithConst[T <: Tuple, A] = Map[T, [t] =>> (t, A)]

type Second[T] = T match
  case (a, b) => b

type Size[T] <: Int = T match
  case EmptyTuple => 0
  case x *: xs => 1 + Size[xs]
```

이 코드에서 관찰할 수 있는 점들입니다:

- 실제 함수형 언어로 작성한 것과 거의 비슷해 보입니다!
- 특히 `Map`, `FlatMap`, `Filter` 같은 고수준 함수들이 그렇습니다. 중위(infix) 문법 덕분에 더 친숙하게 느껴집니다.
- `=>>`로 익명 함수를 정의할 수 있다는 점이 매우 깔끔합니다. 재사용 가능한 도구로서 꽤 훌륭하지 않습니까?
- 타입 레벨 코드는 [값 레벨](https://github.com/ncreep/scala3-flat-json-blog/blob/master/value_level_find_duplicates.scala) 코드를 아주 잘 모방합니다. 일반적인 값 레벨 코드에서 출발한다면, 타입 레벨로 변환하는 것은 비교적 직관적입니다[44](#user-content-fn-rules).
- Scala 2에서 implicit을 사용하던 시절, 비슷한 상황에서 겪었던 즉흥적인 Prolog 느낌과 비교하면 크게 개선되었습니다[45](#user-content-fn-prologisgreat).
- 그렇긴 하지만, 우리가 사용하는 언어는 상당히 원시적이고 결과 코드도 매우 "느슨한 타입"으로 작성되어 있어서, 중첩 튜플에 크게 의존하고 남용합니다. 마치 학생이 Lisp 과제에서 작성할 법한 코드 같습니다.
- 튜플과 리스트의 유비(analogy)로 돌아가면, 우리가 작성하는 모든 코드가 `List[Any]`를 사용하고 패턴 매칭으로 런타임 타입을 확인하는 것과 마찬가지입니다.
- `FindDuplicates`의 "시그니처"를 보면, `Tuple`의 임의 서브타입에 동작한다고만 명시하고, 나중에 패턴 매칭으로 구조가 올바른 형태인지 검증합니다. 게다가 패턴 누락 검사(exhaustivity check)도 없습니다. 관련 패턴을 빠뜨리면 전적으로 우리 책임입니다.
- 물론 우리의 경우 "타입을 잘못 사용"해도 오류는 컴파일 타임에 보고됩니다. 하지만 위에서 `inline`을 다룰 때처럼 "클라이언트 측"에서 발생할 가능성이 높습니다. 유비하자면, 이 코드의 "클라이언트 측"은 타입 레벨 계산의 "런타임"에 해당합니다. 따라서 오류가 원하는 시점보다 늦게 보고됩니다.
- 개발 중에 이것은 매우 혼란스러울 수 있습니다. 한 곳에서 "match type reduction failed"라는 컴파일 에러가 뜨지만, 실제 오류는 다른 곳에서 발생한 것이라 에러 트레이스를 파고들어 원인을 찾아야 합니다. 런타임 스택트레이스를 디버깅하는 것과 비슷합니다.
- 타입 레벨에서 Scala는 "동적 타입 언어"라고 할 수 있습니다...
- 이 문제를 완화하려면 `Labelling` 트레이트처럼 타입에 더 많은 구조를 부여할 수 있습니다. 하지만 이런 타입을 활용할 추가 유틸리티 없이는 별로 얻는 것이 없고, 사용성만 나빠집니다.
- 예를 들어, `Labelling`으로 가득 찬 튜플의 타입(`List[Labelling]`에 해당)이나 문자열 리터럴 튜플의 타입(`List[String]`에 해당)을 정의하는 유틸리티가 필요합니다. 그리고 `Tuple.Map` 같은 라이브러리 함수도 이런 상세한 타입 정보를 보존하도록 수정해야 합니다.
- `Tuple.*`과 `compiletime.ops`에 있는 타입들 너머로, 타입 레벨 프로그래밍을 위한 "표준 라이브러리"가 들어설 여지가 많습니다[46](#user-content-fn-notextensible).

좋습니다, 이제 이것을 실제 컴파일 타임 에러로 만들어 봅시다.

### `error` 함수

이제 모든 `Labelling` 사이에서 중복 필드를 실제로 찾아내는 타입 레벨 함수가 있습니다. 이것을 실제 컴파일 에러와 어떻게 연결할까요?

바로 이것이 등장할 차례입니다:

```scala
scala.compiletime.error
```

[공식 문서의 설명](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#error-1)은 다음과 같습니다:

> The `error` method is used to produce user-defined compile errors during inline expansion.
> 
> 
> 
> If an inline expansion results in a call `error(msgStr)` the compiler produces an error message containing the given `msgStr`.

REPL에서 시도해 보면, 커스텀 에러 메시지를 발생시킬 수 있습니다:

```scala
scala> import scala.compiletime.error

       error("my error!")
-- Error: ----------------------------------------------------------------------
3 |error("my error!")
  |^^^^^^^^^^^^^^^^^^
  |my error!
1 error found
```

이것이 커스텀 컴파일 타임 에러를 삽입하여 사용성을 높이는 방법입니다. `FindDuplicates` 타입을 다시 값으로 변환하기만 하면 됩니다...

사실 리터럴로 가득 찬 타입을 다시 값으로 변환하는 것은 그리 복잡하지 않습니다. [constValueTuple](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#constvalue-and-constvalueopt-1) 함수가 바로 이 역할을 합니다:

```scala
scala> import scala.compiletime.constValueTuple

       type Stuff = ("a", "b", "c")
       val tupleValue = constValueTuple[Stuff]
// defined alias type Stuff = ("a", "b", "c")
val tupleValue: Stuff = (a,b,c)
```

`constValueTuple`은 튜플을 순회하며 각 요소에 [constValue](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#constvalue-and-constvalueopt-1)를 호출합니다. 요소가 리터럴이면 해당하는 값으로 변환됩니다.

이렇게 문자열 타입 리터럴이 담긴 **타입** `Stuff`에서 출발하여, 실제 문자열 값이 담긴 **값** `tupleValue`를 얻었습니다.

`error` 함수를 실험해 봅시다. `tupleValue`로 문자열을 만들어 `error`에 전달합니다:

```scala
scala> import scala.compiletime.error
       val listValue: List[String] = tupleValue.toList

       error("The error" + listValue.mkString(","))
-- Error: ----------------------------------------------------------------------
5 |error("The error" + listValue.mkString(","))
  |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  |A literal string is expected as an argument to `compiletime.error`. Got "The error".+(listValue.mkString(","))
1 error found
```

이것은 동작하지 않습니다. `error`는 리터럴 문자열만 받을 수 있는데, 여기서 사용한 문자열은 `mkString` 호출의 결과이기 때문입니다. 사실 이것은 당연합니다. 컴파일 타임 에러가 되려면 `error` 안의 코드가 컴파일 타임에 평가되어야 합니다. 매크로에 의존하지 않는 한, 임의의 코드를 컴파일 타임에 실행할 수는 없습니다.

좀 더 파고들어 보면, `error`는 문자열 리터럴 외에 `+` 연산도 지원하며 컴파일 타임에 평가됩니다:

```scala
scala> import scala.compiletime.error

       error("error1" + " error2")
-- Error: ----------------------------------------------------------------------
1 |error("error1" + " error2")
  |^^^^^^^^^^^^^^^^^^^^^^^^^^^
  |error1 error2
1 error found
```

없는 것보다 낫지만, 데이터에 복잡한 조작을 할 수 없으니 `error` 함수는 우리 목적에 너무 제한적으로 보입니다. 정말 그럴까요?..

`inline`으로 코드를 작성할 때 결과 코드가 컴파일 타임에 재귀적으로 평가되었던 것을 떠올려 봅시다. 이론적으로, 복잡한 데이터를 받아 재귀적으로 문자열 리터럴과 `+` 호출의 연속으로 변환하는 재귀 함수를 만들 수 있습니다.

이 방법은 분명 가능하지만[47](#user-content-fn-exercise), 표준 문자열 유틸리티 함수를 전혀 쓸 수 없으므로 매우 저수준의 문자열 조작이 됩니다. 번거롭고, 값 레벨에서 작업해도 크게 유리할 것이 없습니다. 하지만 대안이 있습니다.

### 컴파일 타임 문자열 구성

모든 정보가 이미 타입으로 인코딩되어 있고, match type 형태로 사용할 수 있는 타입 "언어"가 매우 강력하다는 점을 고려하면, 문자열 구성을 컴파일 타임에 직접 수행할 수 있습니다. 최종 문자열이 준비되면, 그 결과 타입에 `constValue`를 호출하면 됩니다.

이 접근 방식도 여전히 다소 저수준인데, 표준 라이브러리에서 제공하는 문자열 컴파일 타임 연산이 많지 않기 때문입니다. 하지만 `Tuple.Fold` 같은 고수준 함수를 타입 레벨에서 활용하여 작업을 단순화할 수 있습니다.

타입 레벨 코드를 따라가려면, 동일한 값 레벨 코드를 [여기](https://github.com/ncreep/scala3-flat-json-blog/blob/master/value_level_error_generation.scala)에서 확인할 수 있습니다.

먼저 `List.mkString`의 타입 레벨 버전으로 워밍업합시다[48](#user-content-fn-unsafe):

```scala
import scala.compiletime.ops.string.*

type MkString[T <: Tuple] = // 1
  Fold[Init[T], Last[T], [a, b] =>> a ++ ", " ++ b] // 2

type ++[A, B] <: String = (A, B) match // 3
  case (a, b) => a + b
```

이번에는:

- `MkString` (1)을 임의 튜플에 동작하도록 정의합니다.
- 튜플을 `Fold`합니다 (2). 튜플의 `Last`를 초기 누산기로, `Init`(마지막 요소를 제외한 나머지)을 실제 순회 대상으로 사용합니다. 튜플의 요소들을 쉼표로 구분된 하나의 문자열로 합칩니다.
- 문자열을 합칠 때 `compiletime.ops.string.+`를 직접 사용할 수 없는데, 이 연산자는 인수가 문자열이어야 하지만 여기서 사용하는 `Fold`는 컴파일 타임에 그 정보를 갖고 있지 않기 때문입니다.
- 대신 `++` (3)를 정의합니다. 이것은 `+`의 "타입 제한 없는" 버전으로, 인수를 문자열로 "캐스팅"한 뒤 `+`를 적용합니다[49](#user-content-fn-notype).

이것은 `List.mkString`과 동일하게 동작합니다:

```scala
scala> type Strings = ("a", "b", "c")
       type Result = MkString[Strings]
// defined alias type Result
   = "a, b, c"
```

`Result`는 `Strings` 튜플의 모든 문자열을 결합한 단일 타입 레벨 문자열 리터럴입니다.

`MkString`을 사용하면 레이블과 소스의 단일 그룹을 렌더링할 수 있습니다[50](#user-content-fn-typeboundstring):

```scala
type RenderLabelWithSources[LabelWithSources] <: String = LabelWithSources match
  case (label, sources) => "- [" ++ label ++ "] from [" ++ MkString[sources] ++ "]"
```

레이블과 소스의 쌍이 주어지면, 레이블과 소스의 `MkString`을 포함한 단일 문자열을 만듭니다. 이렇게 됩니다:

```scala
scala> type Label = ("lastUpdate", ("Email2", "Address2"))
       type Result  = RenderLabelWithSources[Label]
// defined alias type Result
   = "- [lastUpdate] from [Email2, Address2]"
```

실제 에러 메시지처럼 보이는 조각들을 계산하기 시작했습니다. `lastUpdate` 필드가 `Email2`와 `Address2` 양쪽에서 발견되었다는 의미입니다. 그런데 문자열 보간(string interpolation) 지원이 절실합니다. 마치 [옛날의](https://docs.scala-lang.org/sips/string-interpolation.html) Scala를 쓰는 기분입니다...

`Tuple.Map`을 사용하면 이 로직을 레이블-소스 튜플 전체에 적용할 수 있습니다:

```scala
type RenderLabelsWithSources[LabelsWithSources <: Tuple] =
  LabelsWithSources Map RenderLabelWithSource
```

사용 예시입니다:

```scala
scala> type Labels =
         (("lastUpdate", ("Email2", "Address2")),
          ("verified", ("Email2", "Phone2")))

       type Result = RenderLabelsWithSources[Labels]
// defined alias type Result
   = ("- [lastUpdate] from [Email2, Address2]",
      "- [verified] from [Email2, Phone2]")
```

거의 다 왔습니다... 마지막 단계는 이 튜플을 하나의 문자열로 합치는 것입니다:

```scala
type RenderError[LabelsWithSources <: Tuple] =
  "Duplicate fields found:\n" ++
    Fold[RenderLabelsWithSources[LabelsWithSources], "", [a, b] =>> a ++ "\n" ++ b]
```

여기서 레이블과 소스의 튜플을 렌더링한 다음, 개행 문자로 구분하여 하나의 문자열로 합칩니다. 에러 설명을 담은 고정 문자열을 앞에 붙입니다.

이것으로 최종 에러 메시지가 만들어집니다:

```scala
scala>  type Labels =
                (("lastUpdate", ("Email2", "Address2")),
                 ("verified", ("Email2", "Phone2")))
        type Result = RenderError[Labels]
// defined alias type Result
   =
    "Duplicate fields found:
     - [lastUpdate] from [Email2, Address2]
     - [verified] from [Email2, Phone2]
    "
```

성공입니다! 중복 필드의 원시 데이터로부터 매우 유익하고 사용자 친화적인 에러 메시지를 만들어냈습니다.

마지막 단계는 이것을 `error` 함수에 연결하는 것입니다:

```scala
inline def renderDuplicatesError[Labellings <: Tuple]: Unit = // 1
  type Duplicates = FindDuplicates[Labellings] // 2

  inline erasedValue[Duplicates] match // 3
    case _: EmptyTuple => () // 4
    case _: (h *: t) => error(constValue[RenderError[h *: t]]) // 5

inline def checkDuplicateFields[A](using mirror: Mirror.ProductOf[A]): Unit = // 6
  inline makeLabellings[mirror.MirroredElemTypes] match // 7
    case labels => renderDuplicatesError[labels.Value] //8
```

이 마지막 코드 조각의 설명입니다:

- Labelling의 튜플이 주어지면 (1),
- `FindDuplicates`를 적용하고 (2) 결과를 새로운 타입 `Duplicates`에 할당합니다.
- `Duplicates`는 타입일 뿐 값이 아니므로, `erasedValue`를 사용하여 값처럼 매칭합니다 (3).
- `Duplicates`가 비어 있으면 (4), 할 일이 없으므로 아무 문제 없이 반환합니다.
- 비어 있지 않으면 (5), 결과 튜플[51](#user-content-fn-alias)에 `RenderError`를 적용합니다. 결과는 타입 레벨 문자열 리터럴이므로 [constValue](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#constvalue-and-constvalueopt-1)로 문자열 값으로 변환할 수 있습니다. `constValue`의 결과가 리터럴 문자열이므로, 안전하게 `error` 함수에 전달할 수 있습니다.
- 이 로직을 실제 클래스에 적용하기 위해, 타입과 그 `Mirror`를 받는 마지막 함수 `checkDuplicateFields`를 정의합니다 (6).
- 클래스의 필드 타입을 `makeLabellings`에 전달하여 (7) `A`의 필드에 대한 모든 레이블이 담긴 튜플을 만듭니다.
- 다시 한번 타입 정밀도 손실을 우려합니다. `makeLabellings`의 결과를 `val`에 할당하면 컴파일러가 `makeLabellings`에서 얻은 정확한 타입을 완전히 잊어버리기 때문입니다. 대신 결과에 `inline match` 핵(hack)을 사용하여 타입을 보존합니다.
- 마지막으로, labelling의 타입을 추출하여 (8) `renderDuplicatesError`에 전달합니다.

이것이 전부입니다. 실제로 동작할까요?..

```scala
scala> checkDuplicateFields[User]

scala> checkDuplicateFields[User2]
-- Error: ----------------------------------------------------------------------
1 |checkDuplicateFields[User2]
  |^^^^^^^^^^^^^^^^^^^^^^^^^^^
  |Duplicate fields found:
  |- [lastUpdate] from [Email2, Address2]
  |- [verified] from [Email2, Phone2]
1 error found
```

이 아름다운 컴파일 에러를 보십시오! 코드가 컴파일에 실패해서 이렇게 기뻤던 적은 없었던 것 같습니다.

`checkDuplicateFields`는 중복이 없는 `User`에서는 정상적으로 컴파일되지만, `User2`에서는 상세하고 유익한 에러 메시지와 함께 컴파일이 실패합니다.

이 파트의 전체 코드는 함께 제공되는 [저장소](https://github.com/ncreep/scala3-flat-json-blog/blob/master/error_generation.scala)에서 찾을 수 있습니다.

이것으로 마지막 안전성 요구사항을 모두 충족했습니다.

### 최종 통합

마침내, 모든 부분을 하나로 모아서 flat JSON 문제를 완전히 해결할 수 있습니다.

```scala
inline def makeCodec[A <: Product](
    using mirror: Mirror.ProductOf[A]): Codec[A] =

  checkDuplicateFields[A]

  val encoder = Encoder.instance[A]: value =>
    val fields = Tuple.fromProductTyped(value)
    val jsons = tupleToJson(fields)

    concatObjects(jsons)

  val decoders = summonAll[Map[mirror.MirroredElemTypes, Decoder]]
  val decoder = decodeTuple(decoders).map(mirror.fromTuple)

  Codec.from(decoder, encoder)
```

이전과 동일한 `makeCodec` 코드이지만, 실제로 `Codec`을 생성하기 전에 `checkDuplicateFields`를 호출합니다. 검사를 통과하면 평소대로 진행하고, 그렇지 않으면 컴파일을 중단합니다.

```scala
scala> val codec = makeCodec[User]
       val json = codec(user)
       val fromJson = codec.decodeJson(json)
val codec: Codec[User] = io.circe.Codec$$anon$4@6ff185a3
val json: Json = {
  "primary" : "bilbo@baggins.com",
  "secondary" : "frodo@baggins.com",
  "number" : "555-555-00",
  "prefix" : 88,
  "country" : "Shire",
  "city" : "Hobbiton"
}
val fromJson: Decoder.Result[User] =
  Right(
    User(
      Email(bilbo@baggins.com,Some(frodo@baggins.com)),
      Phone(555-555-00,88),
      Address(Shire,Hobbiton)))
```

이전과 동일하게 동작합니다. 하지만 `User2`에 대해 `Codec`을 만들려고 하면:

```scala
scala> val codec = makeCodec[User2]
-- Error: ----------------------------------------------------------------------
1 |val codec = makeCodec[User2]
  |            ^^^^^^^^^^^^^^^^
  |            Duplicate fields found:
  |            - [lastUpdate] from [Email2, Address2]
  |            - [verified] from [Email2, Phone2]
1 error found
```

`User2`에 대해 `Codec`을 만드는 것이 안전하지 않다는 멋진 컴파일 에러를 얻습니다. 처음부터 목표로 했던 읽기 쉽고 유익한 컴파일 에러라는 기준을 충족합니다.

런타임 위기를 피했습니다!

## 결론

긴 여정이었습니다. 튜플과 `inline`을 사용한 "단순한" 타입 레벨 프로그래밍에서 시작하여, match type 기반의 본격적인 타입 레벨 언어까지 도달했습니다.

거친 부분이나 컴파일러 이슈가 있었지만, Scala의 타입 레벨 기능이 제대로 동작할 때는 정말 빛을 발합니다. Scala 타입 시스템의 표현력은 계속 높아지고 있는 것 같습니다. 그리고 Scala는 정교한 문제를, 컴파일 타임에, 꽤 읽기 좋은 코드로 해결하는 도전에 충분히 대응할 수 있습니다.

이 글이 타입 레벨 계산의 세계로 모험을 떠나고 싶은 호기심을 불러일으키길 바랍니다. 타입 레벨에 사는 사람이 많을수록 즐거운 법입니다.

여기까지 읽어주셔서 감사합니다. 흥미롭거나 유용한 내용이 있었기를 바랍니다. 다음에 또 만납시다!

## Footnotes

1. This is a very loosely-typed model for example purposes only, please use better and more precise types for real code. [↩](#user-content-fnref-badmodel)
2. I'll leave it up to you to imagine why you might have such a requirement. [↩](#user-content-fnref-whyflat)
3. I'm going to side-step the issue of whether tying your internal model to serialization concerns is a good idea (it's not), and just go with the problem as is. [↩](#user-content-fnref-goodidea)
4. Or we could do it "manually" by creating another class with the desired flat format and then convert between the original nested classes and the new flat one. We could, but it would be tedious, and not as magical… [↩](#user-content-fnref-manually)
5. There are libraries that can somewhat simplify meta-programming in Scala, such as [Magnolia](https://github.com/softwaremill/magnolia) and [Shapeless 3](https://github.com/typelevel/shapeless-3). These libraries are useful, but as of writing they are not powerful enough to solve the problem as stated. [↩](#user-content-fnref-otherlibraries)
6. I found myself discovering and reporting a couple of issues while researching the material for this article. And to add some further entertainment, occasionally Metals would refuse to compile on some spurious errors, only after hitting "recompile workspace" the errors would go away. [↩](#user-content-fnref-issues)
7. Similar to the functionality of the `HList` type in the [Shapeless](https://github.com/milessabin/shapeless) library for Scala 2. [↩](#user-content-fnref-hlist)
8. From the point of view of the function argument implementation, the choice of the "current" type parameter at the time of invocation is not within its control. So any implementation of the function argument cannot assume anything about its inputs and cannot do anything special for specific types. This is a form of "[parametricity](https://en.wikipedia.org/wiki/Parametricity)" (we're assuming no cheating with reflection and the like). A further exploration of this concept can be found in the "[It's existential on the inside](https://typelevel.org/blog/2016/01/28/existential-inside.html)" blog by Stephen Compall. [↩](#user-content-fnref-uniformtype)
9. A non-tail-recursive version is much more natural in these circumstances, and thus easier to implement. Although it is not acceptable in Scala for the `List` type, it will be perfectly fine for the tuple code that we are writing. Since tuples are usually not that big. [↩](#user-content-fnref-tailrecursion)
10. I'm using the convention where `h` stands for the head of the structure, and `t` stands for the tail of the structure. This may seem a bit unreadable at first, but will grow on you eventually. [↩](#user-content-fnref-headtail)
11. Confusing though it may be, but we now have two equivalent ways of expressing the same tuple values:

```scala
(3, "abc", true) == 3 *: "abc" *: true *: EmptyTuple
```

With two equivalent type names:

```scala
(Int, String, Boolean) =:= Int *: String *: Boolean *: EmptyTuple
```

[↩](#user-content-fnref-confusing)
12. Similar to the value-level, where lowercase identifiers are fresh variables introduced by the pattern match, and uppercase identifiers are existing values. [↩](#user-content-fnref-patternmatching)
13. This compilation error would be more informative if we could see what field in the class triggered the error. I'll leave this as a (difficult) exercise for the reader… [↩](#user-content-fnref-bettererror)
14. Similar to the functionality of the `Generic` type in the [Shapeless](https://github.com/milessabin/shapeless) library in Scala 2. [↩](#user-content-fnref-generic)
15. You can actually name the branches, instead of using `_`. But if, by mistake, you try to use the named value the compiler will complain and abort compilation. [↩](#user-content-fnref-atruntime)
16. Traits, classes, etc., but not type aliases. [↩](#user-content-fnref-realtypes)
17. The specific name of the wrapper doesn't matter. The only thing that matters is that it has a single type-parameter. [↩](#user-content-fnref-doesntmatter)
18. I elided some of the details from the full error. [↩](#user-content-fnref-elided)
19. This can be fine, as we can pattern match on the tuple to verify the shape with an `inline match` at compile-time. But this leads as into the land of being "dynamically-typed at compile-time". Indeed, a very strange place to be. [↩](#user-content-fnref-patternmatch)
20. This problem actually has a solution. Below we are going to use a match type for the input and a "simple type" for the output. We could flip this around and use a "simple type" for the input and a match type for the output. But this would still leave us with the issue that the input type is missing some information and lead us to the "dynamically typed" approach. [↩](#user-content-fnref-alternativesolution)
21. Coincidence? I think not… [↩](#user-content-fnref-coincidence)
22. In the days of yore computations of this kind would be accomplished with an unholy mix of recursive implicits and type-members. See, e.g., all of [Shapeless](https://github.com/milessabin/shapeless). [↩](#user-content-fnref-dayofyore)
23. Feel free to solve this as an exercise. This is yet another instance of tuple iteration. [↩](#user-content-fnref-summonexercise)
24. You can find the solution to this exercise in the [repo](https://github.com/ncreep/scala3-flat-json-blog/blob/master/flat_modular.scala). [↩](#user-content-fnref-implementedintherepo)
25. Keeping in mind the `List` analogy that we have with tuples. [↩](#user-content-fnref-listanalogy)
26. You don't have to use the Cats library here, any definition of `Applicative` will do. But since circe is already bringing in Cats as a dependency, we just use that. [↩](#user-content-fnref-cats)
27. Because `Encoder.AsObject` is a contravariant functor and cannot have an `Applicative` instance. [↩](#user-content-fnref-contravariant)
28. The type bound `<: String` means that we are aiming at storing string literal types here. [↩](#user-content-fnref-typebound)
29. There's a design choice to be made here, as we could define `Label` and `ElemLabels` as type members rather than type arguments. In some sense both choices are equivalent, and the difference is mainly in ergonomics. [↩](#user-content-fnref-designchoice)
30. Notice that we are only building up a type and not a corresponding value. Although a value might be useful in some contexts, and we could create an equivalent value if we needed to, it won't be necessary for our use case. [↩](#user-content-fnref-notvalue)
31. A more concrete example would be something like `summonAll[Map[T, Mirror.Of]]`, the resulting statically known type will not contain anything beyond `Mirror.Of[X]`, no field names, or anything specific. Let me know if you know otherwise. [↩](#user-content-fnref-cantdo)
32. Let me know if you know otherwise. [↩](#user-content-fnref-letmeknow)
33. Here are a two issues I opened while playing around with this code: [18010](https://github.com/lampepfl/dotty/issues/18010) and [18011](https://github.com/lampepfl/dotty/issues/18011). [↩](#user-content-fnref-transparentissues)
34. One would imagine this transformation to be a noop that does nothing to the typing process, and yet… [↩](#user-content-fnref-noop)
35. This is a somewhat different manifestation of [18011](https://github.com/lampepfl/dotty/issues/18011), where instead of failing to compile we just lose the precise types. [↩](#user-content-fnref-differentissue)
36. [According](https://github.com/lampepfl/dotty/issues/18011#issuecomment-1605394436) to Martin Odersky, the relevant area of the compiler has "crazily complicated algorithms"… [↩](#user-content-fnref-complexity)
37. When writing such code it's very important to have some tests to make sure that it actually does what you think it does. It will be very unpleasant if after some benign refactor code silently stops working. [↩](#user-content-fnref-tests)
38. If you're curious what that would look like, I've implemented the same logic using implicits in the [repo](https://github.com/ncreep/scala3-flat-json-blog). [↩](#user-content-fnref-implicitlabelling)
39. It is actually possible to convert the `Labelling` tuple types into values, but since we want compile-time errors derived from this process, we will be very limited in what can do with these values in a way that the compiler can report about. So sticking with type-level computation is actually more straightforward here. For an alternative approach see, e.g., the [constValue family of functions](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html#constvalue-and-constvalueopt-1). [↩](#user-content-fnref-alternative)
40. This is of course not the way you would solve this problem with regular Scala. The code is written in a style that conforms to the limitations of the type-level implementation. [↩](#user-content-fnref-notthebest)
41. I'm slightly cheating with the REPL output, as for some reason the REPL refuses to fully reduce the last result. To this end I wrap `Res2` with the `Reduce`, which is defined as:

```scala
type Reduce[T <: Tuple] = T match
  case EmptyTuple => EmptyTuple
  case h *: t => h *: Reduce[t]
```

Which should be a noop, but for some reason forces the REPL to fully reduce the type. [↩](#user-content-fnref-cheating)
42. If, for example, we could compare them lexicographically, we could have similar functionality in `O(n*log(n))`, rather than `n^2` we have here. And who knows what magic we could do with some hashing. [↩](#user-content-fnref-efficient)
43. We are cheating again and applying `Reduce` here as well. [↩](#user-content-fnref-cheatagain)
44. Though you would to have to observe some "rules" when writing the value-level code, e.g., use mostly basic lists and recursion. [↩](#user-content-fnref-rules)
45. Don't get me wrong, Prolog is great where it's [applicable](https://www.youtube.com/watch?v=9i06TyYM_lI). But in this case the functional style seems much more readable to me. [↩](#user-content-fnref-prologisgreat)
46. Creating new functions similar to the ones in `Tuple.*` is possible in library code, but the functionality of `compiletime.ops` is wired into the compiler and not extensible outside of it. [↩](#user-content-fnref-notextensible)
47. As per usual, feel free to make into an exercise: given the results of `FindDuplicates` turn it into a value, and then into a single error message that can be passed to `error`. [↩](#user-content-fnref-exercise)
48. This code would be "unsafe" to run on empty tuples, it will fail at compile-time if we pass it an empty tuple. Feel free to make this function safe by correctly handling the empty case. [↩](#user-content-fnref-unsafe)
49. I have no idea how to specify that we are actually trying to match string literals in that match. It seems to work without any further annotations though. [↩](#user-content-fnref-notype)
50. Using a type bound here `<: String` to add a bit more precision to type-checking. Note that type bounds can only be added to match types, not to type aliases. So their usage is a bit inconsistent. [↩](#user-content-fnref-typeboundstring)
51. It would be nice to have some pattern aliases, so that we don't have to destructure `h` and `t` explicitly and restructure them back. Unfortunately `_: t@NonEmptyTuple => ...` is not valid syntax. [↩](#user-content-fnref-alias)
