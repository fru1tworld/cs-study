> 원본: https://rockthejvm.com/articles/scala-3-match-types

# Scala 3: Match Types 빠르게 알아보기

## 목차

1. [대상 독자](#대상-독자)
2. [배경: 사소하지만 성가신 문제](#배경-사소하지만-성가신-문제)
3. [Match Types 등장](#match-types-등장)
4. [Match Types를 활용한 의존 메서드](#match-types를-활용한-의존-메서드)
5. [Match Types가 필요한 미묘한 이유](#match-types가-필요한-미묘한-이유)
6. [Match Types의 추가 기능](#match-types의-추가-기능)
7. [Match Types의 제약 사항](#match-types의-제약-사항)
8. [결론](#결론)

---

## 대상 독자

이 글은 Scala 3의 새로운 기능이 궁금한 Scala 프로그래머를 위한 글이다. Scala 2의 일부 기능(제네릭 등)은 알고 있다고 가정한다. 타입 수준의 미묘한 차이도 다루기 때문에, "이게 나한테 어떤 쓸모가 있지?"라는 질문의 답은 다소 미묘할 수 있다.

이 기능은 (수십 가지 다른 변경 사항과 함께) Scala 3 New Features 과정에서 자세히 설명한다.

---

## 배경: 사소하지만 성가신 문제

기능을 바로 설명하기보다, 먼저 왜 필요한지부터 시작하겠다. 표준 데이터 타입(Int, String, List 등)을 위한 라이브러리를 개발하고 있고, 더 큰 값에서 마지막 구성 요소를 추출하는 코드를 작성하고 싶다고 하자.

- BigInt이 숫자(digit)로 이루어져 있다고 보면, 마지막 부분은 마지막 자릿수다
- String의 마지막 부분은 Char다
- 리스트의 마지막 부분은 마지막 위치의 원소다

이를 위해 다음과 같은 메서드를 만들 수 있다:

```scala
def lastDigitOf(number: BigInt): Int = (number % 10).toInt

def lastCharOf(string: String): Char =
  if string.isEmpty then throw new NoSuchElementException
  else string.charAt(string.length - 1)

def lastElemOf[T](list: List[T]): T =
  if list.isEmpty then throw new NoSuchElementException
  else list.last
```

DRY 원칙에 집착하는 당신은 이 메서드들의 시그니처가 비슷하고 모두 "마지막 조각"이라는 의미를 공유한다는 걸 알아챈다. 그래서 모든 타입에 동작하는 하나의 통합 API로 줄이고 싶다. 게다가 미래를 생각하면, 아직 관련 없는 타입에도 이 로직을 확장할 수 있으면 좋겠다.

현재 Scala 2에서 어떻게 할 수 있을까?

---

## Match Types 등장

좋은 소식과 나쁜 소식이 있다. 나쁜 소식부터: Scala 2에서는 이 꿈을 이룰 수 없다. 좋은 소식은 Scala 3에서 가능하다는 것이다.

Scala 3에서는 전달하는 타입 인자에 따라 다른 형태, 즉 서로 다른 구체 타입으로 축소되는 타입 멤버를 정의할 수 있다:

```scala
type ConstituentPartOf[T] = T match
  case BigInt => Int
  case String => Char
  case List[t] => t
```

이것이 바로 match type이다. 컴파일러가 타입에 대해 패턴 매칭을 수행한다고 생각하면 된다. 다음 표현식은 모두 유효하다:

```scala
val aNumber: ConstituentPartOf[BigInt] = 2
val aCharacter: ConstituentPartOf[String] = 'a'
val anElement: ConstituentPartOf[List[String]] = "Scala"
```

`case List[t] => t` 부분은 타입에 대한 패턴으로, 변수 타입 인자에 대해 평가된다. 컴파일러가 타입 `t`가 무엇인지 파악한 뒤, 추상적인 `ConstituentPartOf[List[t]]`를 그 `t`로 축소한다. 모두 컴파일 타임에 일어난다.

---

## Match Types를 활용한 의존 메서드

이제 match type이 우리의 DRY 문제를 어떻게 해결하는지 살펴보자. 앞선 메서드들이 모두 "큰 것에서 마지막 부분을 추출한다"는 의미를 갖고 있으므로, 방금 만든 match type으로 다음과 같은 만능 API를 작성할 수 있다:

```scala
def lastComponentOf[T](thing: T): ConstituentPartOf[T]
```

이 메서드는 이론상 `T`와 `ConstituentPartOf[T]` 사이의 관계를 컴파일러가 성공적으로 확인할 수 있는 모든 타입에 동작한다. 구현만 완성하면 관심 있는 모든 타입에 _같은 방식으로_ 사용할 수 있다:

```scala
val lastDigit = lastComponentOf(BigInt(53728573)) // 3
val lastChar = lastComponentOf("Scala") // 'a'
val lastElement = lastComponentOf((1 to 10).toList) // 10
```

구현에 대해 이야기하자면, match type을 반환하는 메서드에는 특별한 컴파일러 규칙이 있다. 타입 수준 패턴 매칭과 정확히 같은 구조의 값 수준 패턴 매칭을 사용해야 한다. 따라서 메서드는 다음과 같다:

```scala
def lastComponentOf[T](thing: T): ConstituentPartOf[T] = thing match
  case b: BigInt => (b % 10).toInt
  case s: String =>
    if (s.isEmpty) throw new NoSuchElementException
    else s.charAt(s.length - 1)
  case l: List[_] =>
    if (l.isEmpty) throw new NoSuchElementException
    else l.last
```

이로써 통합을 통해 문제를 해결했다.

---

## Match Types가 필요한 미묘한 이유

왜 match type이 필요한 걸까?

일반적인 답은: **입력 타입에 의존하면서 서로 관련 없을 수 있는 타입을 반환하는 메서드를, 컴파일 타임에 올바르게 타입 검사되는 통합된 방식으로 표현하기 위해서**다. 한 문장이지만 내용이 무겁다. 이미 알고 있는 것들과 어떻게 다른지 비교해 보자.

**일반적인 상속 기반 OOP와 뭐가 다른가?** 인터페이스에 대해 코드를 작성하면, 예를 들어

```scala
def returnConstituentPartOf(thing: Any): ConstituentPart = ... // 패턴 매치
```

실제 인스턴스가 런타임에 반환되므로 API의 타입 안전성을 잃는다. 동시에, 반환 타입이 모두 하나의 상위 트레이트에서 파생되어야 하므로 서로 관련이 있어야 한다.

**일반 제네릭과 뭐가 다른가?** 제네릭이 정확히 이 용도, 즉 서로 관련 없는 타입에 대해 코드와 로직을 재사용하기 위한 것이기 때문에 훨씬 미묘한 질문이다. 예를 들어, 리스트의 로직은 String 리스트든 숫자 리스트든 동일하다.

다음 메서드를 보자:

```scala
def listHead[T](l: List[T]): T = l.head
```

이 메서드 시그니처에서 컴파일러는 반환 타입이 인자로 받은 리스트의 타입과 정확히 같아야 한다고 파악할 수 있다. 타입이 조금이라도 다르면 안 된다. 따라서 반환 타입과 인자 타입 사이에 직접적인 연결이 있으며, 그 관계는 정확히 "`List[T]`를 받으면 `T`만 반환하고 다른 건 없다"이다.

반면 `lastComponentOf` 메서드는 타입 정의에 따라 반환 타입이 유연하게 달라질 수 있게 한다:

- String 인자를 받으면 Char를 반환한다
- BigInt 인자를 받으면 Int를 반환한다
- `List[T]` 인자를 받으면 `T`를 반환한다

이렇게 표현하면, 인자 타입과 반환 타입 사이의 연결을 느슨하게 만들면서도 여전히 제대로 커버하고 있음을 알 수 있다.

---

## Match Types의 추가 기능

Match type은 재귀적으로 정의할 수 있다. 예를 들어 중첩된 리스트를 다루고 싶다면 다음과 같이 쓸 수 있다:

```scala
type LowestLevelPartOf[T] = T match
  case List[t] => LowestLevelPartOf[t]
  case _ => T

val lastElementOfNestedList: LowestLevelPartOf[List[List[List[Int]]]] = 2 // ok
```

그러나 컴파일러는 정의 내 순환을 감지한다:

```scala
// 컴파일되지 않는다
type AnnoyingMatchType[T] = T match
  case _ => AnnoyingMatchType[T]
```

무한 재귀 가능성도 감지한다:

```scala
type InfiniteRecursiveType[T] = T match
  case Int => InfiniteRecursiveType[T]

def aNaiveMethod[T]: InfiniteRecursiveType[T] = ???
val illegal: Int = aNaiveMethod[Int] // <-- 여기서 에러: 컴파일러가 타입을 찾다가 스택 오버플로
```

---

## Match Types의 제약 사항

내가 아는 한 -- 틀리면 알려주길 바란다 -- 의존 메서드에서 match type의 활용은 메서드의 정확한 시그니처에 의해 제약된다. 현재까지는 다음 시그니처를 가진 메서드만 허용된다:

```scala
def method[T](argument: T): MyMatchType[T]
```

`ConstituentPartOf`의 경우, 컨테이너에 값을 더하는 것 같은 좀 더 유용한 API를 만들고 싶다면:

```scala
def accumulate[T](accumulator: T, value: ConstituentPartOf[T]): T
```

이는 정의는 가능하지만 구현은 불가능하다. `accumulator`에 대한 패턴 매치 구조가 타입 수준 패턴 매치와 동일하더라도, 컴파일러는 `accumulator`가 BigInt로 판별되었을 때 `+` 연산자를 가질 수 있다는 것을 알아내지 못한다. 이런 패턴 매치는 _flow typing_과 동치인데, 이는 아마도 미래 Scala 타입 시스템의 성배가 될 것이다.

---

## 결론

매우 유연한 API 통합 문제를 해결할 수 있는 match type에 대해 알아보았다. 이 문제 자체의 심각성에 의문을 제기하는 사람도 있겠지만, 자체 API나 라이브러리를 정의할 때 타입 수준 도구 상자에 넣어둘 만한 강력한 도구다.
