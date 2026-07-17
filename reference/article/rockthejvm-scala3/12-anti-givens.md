> 원본: https://rockthejvm.com/articles/scala-3-anti-givens

# Scala 3: Anti-Given 빠르게 알아보기

## 시작하며

Scala 3의 given/using 조합은 막강한 표현력을 갖고 있다. 이것만으로도 새로운 타입을 합성하고 타입 간의 관계를 증명할 수 있는데, 이는 Scala 2의 implicit이 하던 역할과 같다.

이 글에서는 대부분의 Scala 개발자가 모르는 기법 하나를 소개한다. 바로 컴파일러가 given 인스턴스의 _부재_를 활용해 타입 관계를 강제하도록 만드는 것이다.

## 사전 지식

이 글은 Scala 3 전용이다. 개발 환경을 준비하고 Scala 3 프로젝트를 새로 만들자(별도 라이브러리는 필요 없다). given에 대한 기본 지식이 있으면 도움이 된다.

## 작은 문제: 타입 동일성 증명

두 리스트를 처리하는 라이브러리 API가 있다고 하자.

```scala
def processLists[A, B](la: List[A], lb: List[B]): List[(A, B)] =
  for {
    a <- la
    b <- lb
  } yield (a, b)
```

이 메서드는 수정할 수 없지만, 타입 A가 반드시 B와 같은 경우에만 사용하고 싶다. 다시 말해 아래 코드는 컴파일되어야 하고,

```scala
val sameKindOfLists = processLists(List(1,2,3), List(4,5))
```

아래 코드는 컴파일되지 _않아야_ 한다.

```scala
val differentKindsOfLists = processLists(List(1,2,3), List("black", "white"))
```

방법은 여러 가지가 있다. 가장 단순한 첫 번째 접근법은, 타입 인자를 하나만 받는 래퍼 메서드를 만들어 두 리스트의 타입을 같게 강제하는 것이다.

```scala
def processListsSameTypeV2[A](la: List[A], lb: List[A]): List[(A, A)] =
  processLists[A,A](la, lb)
```

이렇게 하면 서로 다른 타입의 리스트 두 개를 넘길 수 없다.

하지만 좀 더 복잡하고 덜 알려져 있으면서도 훨씬 강력한 기법이 있다.

Scala 표준 라이브러리에는 잘 알려지지 않은 타입 `=:=[A,B]`(중위 표기로 `A =:= B`)가 있다. 이 타입은 A와 B의 "동일성"을 나타낸다. 컴파일러는 implicit 인자나 `using` 절이 필요한 곳에서 `=:=[A,A]` 인스턴스를 자동으로 합성할 수 있다. 이를 활용하면 다음과 같이 작성할 수 있다.

```scala
def processListsSameTypeV3[A, B](la: List[A], lb: List[B])(using A =:= B): List[(A, B)] =
  processLists(la, lb)
```

이 메서드를 구체적인 타입으로 호출하면, 컴파일러가 해당 타입들에 대한 `=:=` given 인스턴스를 찾는다. 결과는 다음과 같다.

```scala
// 동작함
val sameKindOfLists = processListsSameTypeV3(List(1,2,3), List(4,5))
// 컴파일 안 됨
val differentKindsOfLists = processListsSameTypeV3(List(1,2,3), List("black", "white"))
```

첫 번째 호출은 컴파일러가 `=:=[Int, Int]` 인스턴스를 합성할 수 있으므로 동작한다. 반면 두 번째 호출은 `=:=[Int, String]` 인스턴스를 찾을 수 없어 컴파일에 실패한다.

이 두 번째 해법은 더 복잡하지만, 더 큰 문제를 풀기 위한 발판이 된다.

## 더 큰 문제: 타입이 다름을 증명하기

똑같이 수정할 수 없는 `processList` 라이브러리 API가 있다. 그런데 이번에는 정반대의 제약을 걸어야 한다. _서로 다른_ 타입 인자로만 `processList`를 호출할 수 있게 하려면 어떻게 해야 할까? 어떤 이유에서든 서로 다른 타입의 원소만 조합해야 하므로, 이 제약을 강제해야 한다.

현재로서는 `processList`에 정수 리스트 두 개를 넘기는 것을 막을 수단이 없다. 하지만 앞서 (쉬운) 문제에서 given을 활용한 해법을 뒤집어 쓸 수 있다. 이번에는 원하는 타입에 대해 `=:=` 인스턴스가 _존재하지 않음_을 활용하는 것이다. 방법은 다음과 같다.

먼저 특별한 import를 추가한다.

```scala
import scala.util.NotGiven
```

`NotGiven` 타입은 컴파일러가 특별하게 취급한다. `NotGiven[T]`의 존재를 요구하면, 컴파일러는 `T`의 given 인스턴스를 찾거나 합성할 수 _없을 때에만_ `NotGiven[T]` 인스턴스를 성공적으로 합성한다. 우리의 경우 `A =:= B` 인스턴스가 없어야 하므로, 래퍼 메서드는 다음과 같이 된다.

```scala
def processListsDifferentType[A, B](la: List[A], lb: List[B])(using NotGiven[A =:= B]): List[(A, B)] =
  processLists(la, lb)
```

이렇게 하면 제약이 의도대로 동작한다.

```scala
// 컴파일 안 됨
val sameListType = processListsDifferentType(List(1,2,3), List(4,5))
// 동작함
val differentListTypes = processListsDifferentType(List(1,2,3), List("black", "white"))
```

첫 번째는 컴파일러가 `=:=[Int, Int]` 인스턴스를 합성할 수 있어서 `NotGiven`을 만들 수 없으므로 컴파일에 실패한다. 두 번째는 그 반대이므로 정상 동작한다.

## 마치며

Scala 3 컴파일러를 활용해 컴파일 타임에 타입 관계를 강제하는 또 하나의 기법을 살펴보았다.
