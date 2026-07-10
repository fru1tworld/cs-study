> 원본: https://rockthejvm.com/articles/type-level-programming-in-scala-3-part-2-a-quicksort-on-types

# Scala 3 타입 수준 프로그래밍: Part 2 - 타입으로 구현하는 퀵소트

## 도입

타입 수준 프로그래밍 미니 시리즈의 두 번째 글이다. 첫 번째 파트에서는 타입 수준 프로그래밍의 개념과, 컴파일러가 풀 수 있는 타입 관계로 계산 문제를 표현하는 방법을 다뤘다.

이번 글에서는 타입 수준에서 퀵소트 알고리즘을 리스트에 적용하고, 컴파일러가 컴파일 타임에 이를 풀도록 만든다.

## 이전 파트 요약

이전 파트에서는 페아노 산술을 사용해 자연수를 타입으로 표현하는 데 집중했다. 자연수 전체를 0과 후속(succession) 관계만으로 표현할 수 있다.

```scala
trait Nat
class _0 extends Nat
class Succ[N <: Nat] extends Nat

type _1 = Succ[_0]
type _2 = Succ[_1] // Succ[Succ[_0]]
type _3 = Succ[_2]
type _4 = Succ[_3]
type _5 = Succ[_4]

// "LessThan"으로 이름을 짓고, "<"로 리팩토링한다
trait <[A <: Nat, B <: Nat]
object < {
  given basic[B <: Nat]: <[_0, Succ[B]] with {}
  given inductive[A <: Nat, B <: Nat](using lt: <[A, B]): <[Succ[A], Succ[B]] with {}
  def apply[A <: Nat, B <: Nat](using lt: <[A, B]) = lt
}
val comparison = <[_1, _3]
/*
  <.apply[_1, _3] -> <[_1, _3] 필요
  inductive[_0, _2] -> <[_0, _2] 필요
  ltBasic[_1] -> <[_0, Succ[_1]] == <[_0, _2] 생성
 */

trait <=[A <: Nat, B <: Nat]
object <= {
  given lteBasic[B <: Nat]: <=[_0, B] with {}
  given inductive[A <: Nat, B <: Nat](using lte: <=[A, B]): <=[Succ[A], Succ[B]] with {}
  def apply[A <: Nat, B <: Nat](using lte: <=[A, B]) = lte
}
val lteTest: _1 <= _1 = <=[_1, _1]
```

"작다"와 "작거나 같다" 관계를 이 관계를 나타내는 `given` 값의 존재를 증명하는 방식으로 구현했다. 컴파일러는 정의된 규칙에 따라 인스턴스를 합성한다.

## 타입 수준 리스트

shapeless 스타일을 따라, 타입 수준에서 이종 리스트(HList)를 다음과 같이 표현한다:

```scala
trait HList
class HNil extends HList
class ::[H <: Nat, T <: HList] extends HList
```

예를 들어 리스트 `[1,2,3,4]`는 타입 수준에서 `::[_1, ::[_2, ::[_3, ::[_4, HNil]]]]`이 되고, 중위 표기법으로는 `_1 :: _2 :: _3 :: _4 :: HNil`이다.

HList가 "정렬되었는지" 테스트하려면 `Sorted[_2 :: _3 :: _1 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]` 같은 관계를 만들어서, 첫 번째 리스트를 정렬한 결과가 두 번째 리스트와 같은지 검증한다.

일반적인 연결 리스트 퀵소트에는 다음이 필요하다:

- 리스트 head에서 피벗을 선택
- 피벗보다 작은 원소와 크거나 같은 원소로 분할
- 분할된 각 부분을 재귀적으로 정렬
- 정렬된 부분들을 합치기

이 연산들은 세 가지 타입 수준 연산으로 변환된다: 연결(Concatenation), 분할(Partitioning), 정렬(Sorting).

## 타입 수준 퀵소트: 연결 (Concatenation)

`Concat[HA, HB, O]` 타입으로 연결을 테스트한다. 모든 멤버는 HList의 서브타입이다. `[1,2] ++ [3,4] == [1,2,3,4]`라는 연결은 `Concat[_1 :: _2 :: HNil, _3 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]`의 인스턴스가 존재할 때만 참이다.

연결의 공리는 다음과 같다:

1. **기본**: 빈 리스트에 임의의 리스트 L을 연결하면 L이 된다.
2. **귀납/재귀**: 비어 있지 않은 리스트 H :: T에 임의의 리스트 L을 연결하면, head가 H이고 tail이 T와 L의 연결인 새 리스트가 된다.

```scala
trait Concat[HA <: HList, HB <: HList, O <: HList]
object Concat {
  given basicEmpty[L <: HList]: Concat[HNil, L, L] with {}
  given inductive[N <: Nat, HA <: HList, HB <: HList, O <: HList](using Concat[HA, HB, O]): Concat[N :: HA, HB, N :: O] with {}
  def apply[HA <: HList, HB <: HList, O <: HList](using concat: Concat[HA, HB, O]): Concat[HA, HB, O] = concat
}
```

리스트 `[0,1]`과 `[2,3]`의 연결을 테스트하면:

```scala
val concat = Concat[_0 :: _1 :: HNil, _2 :: _3 :: HNil, _0 :: _1 :: _2 :: _3 :: HNil]
```

컴파일러가 이를 성공적으로 컴파일한다. 과정은 다음과 같다:

- `inductive` 규칙을 적용하여 `Concat[_1 :: HNil, _2 :: _3 :: HNil, _1 :: _2 :: _3 :: HNil]`을 요구한다
- 다시 `inductive`를 적용하여 `Concat[HNil, _2 :: _3 :: HNil, _2 :: _3 :: HNil]`을 요구한다
- `basicEmpty`를 타입 `_2 :: _3 :: HNil`에 적용한다
- 역으로 거슬러 올라가며 필요한 모든 중간 `given`을 생성한다

잘못된 연결은 컴파일이 되지 않는다.

## 타입 수준 퀵소트: 분할 (Partitioning)

`Partition[HL <: HList, L <: HList, R <: HList]` 타입은 리스트 HL이 L(피벗보다 작거나 같은 원소, 피벗 포함)과 R(피벗보다 큰 원소)로 성공적으로 분할되었음을 나타낸다.

예를 들어, `[_2 :: _3 :: _1 :: HNil]`을 `[_2 :: _1 :: HNil]`과 `[_3 :: HNil]`로 분할하려면 `Partition[_2 :: _3 :: _1 :: HNil, _2 :: _1 :: HNil, _3 :: HNil]`의 인스턴스가 필요하다.

분할의 공리는 다음과 같다:

1. 빈 리스트를 분할하면 양쪽 모두 빈 리스트다.
2. 원소가 하나인 리스트를 분할하면 "왼쪽"에 그 리스트가, "오른쪽"에 빈 리스트가 온다.
3. 원소가 둘 이상인 리스트 P :: N :: T (피벗, 다음 숫자, tail)의 경우:
   - P <= N이면 N은 "오른쪽"에 남는다
   - N < P이면 N은 "왼쪽"에 남는다

```scala
trait Partition[HL <: HList, L <: HList, R <: HList] // 피벗은 왼쪽에 포함
object Partition {
  given basic: Partition[HNil, HNil, HNil] with {}
  given basic2[N <: Nat]: Partition[N :: HNil, N :: HNil, HNil] with {}

  given inductive[P <: Nat, N <: Nat, T <: HList, L <: HList, R <: HList]
  (using P <= N, Partition[P :: T, P :: L, R]):
    Partition[P :: N :: T, P :: L, N :: R] with {}

  given inductive2[P <: Nat, N <: Nat, T <: HList, L <: HList, R <: HList]
  (using N < P, Partition[P :: T, P :: L, R]):
    Partition[P :: N :: T, P :: N  :: L, R] with {}

  def apply[HL <: HList, L <: HList, R <: HList]
  (using partition: Partition[HL, L, R]): Partition[HL, L, R] =
      partition
}
```

분할 테스트:

```scala
val partition = Partition[_2 :: _3 :: _1 :: HNil, _2 :: _1 :: HNil, _3 :: HNil]
```

컴파일러는 다음과 같이 검증한다:

- `inductive`를 사용하여 `Partition[_2 :: _1 :: HNil, _2 :: _1 :: HNil, HNil]`을 요구한다
- `inductive2`를 사용하여 `Partition[_2 :: HNil, _2 :: HNil, HNil]`을 요구한다
- `basic2[_2]`를 사용하여 `Partition[_2 :: HNil, _2 :: HNil, HNil]`을 만든다
- 역으로 거슬러 올라가며 필요한 given을 생성한다

**참고**: 분할 내 원소의 상대적 순서는 유지된다. 퀵소트가 동작하려면 컴파일러가 하나 이상의 유효한 분할을 검증하면 된다.

## 타입 수준 퀵소트: 정렬 연산

`QSort[HL, R]` 타입은 HL을 정렬한 결과가 R인지를 테스트한다. 컴파일러가 `given` 인스턴스를 생성할 수 있다면, 정렬이 올바른 것이다.

정렬 공리는 다음과 같다:

- 빈 리스트를 정렬하면 빈 리스트가 된다.
- 비어 있지 않은 리스트 N :: T를 정렬하려면:
  - N :: L과 R로 올바르게 분할한다
  - L과 R을 각각 재귀적으로 정렬하여 SL과 SR을 얻는다
  - SL + N + SR로 연결한다

```scala
trait QSort[HL <: HList, Res <: HList]
object QSort {
  given basicEmpty: QSort[HNil, HNil] with {}
  // given basicOne[N <: Nat]: QSort[N :: HNil, N :: HNil] with {}
  given inductive[N <: Nat, T <: HList, L <: HList, R <: HList, SL <: HList, SR <: HList, O <: HList]
    (
      using
      Partition[N :: T, N :: L, R],
      QSort[L, SL],
      QSort[R, SR],
      Concat[SL, N :: SR, O]
    ): QSort[N :: T, O] with {}

  def apply[HL <: HList, Res <: HList](using sort: QSort[HL, Res]): QSort[HL, Res] = sort
}
```

정렬 테스트:

```scala
val sortTest = QSort[_3 :: _2 :: _1 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]
val sortTest2 = QSort[_1 :: _2 :: _3 :: _4 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]
val sortTest3 = QSort[_4 :: _3 :: _2 :: _1 :: HNil, _1 :: _2 :: _3 :: _4 :: HNil]
val sortTest4 = QSort[_4 :: _4 :: _4 :: HNil, _4 :: _4 :: _4 :: HNil]
```

이렇게 컴파일 타임에 타입 위에서 동작하는 퀵소트가 완성된다.

## 타입 수준 퀵소트: 자동 추론

한 발 더 나아가서, 결과 타입을 직접 지정하지 않아도 컴파일러가 올바른 정렬 결과를 자동으로 추론하게 만들 수 있다.

다른 형태의 정렬 타입을 생각해 보자:

```scala
trait Sort[HL <: HList] {
  type Result <: HList
}
```

두 번째 제네릭 인자를 제거하고, 대신 추상 타입 멤버를 추가했다. 컴파일러가 이 추상 타입 멤버를 자동으로 "저장"하게 된다.

컴패니언 객체에 타입 별칭을 정의한다:

```scala
type QSort[HL <: HList, O <: HList] = Sort[HL] { type Result = O }
```

컴파일러는 `given` 해소 과정에서 어차피 타입 추론을 수행하므로, 제네릭 타입을 자동으로 추론하면서 Sort의 추상 타입 인자도 함께 채울 수 있다.

빈 리스트 정렬의 첫 번째 공리:

```scala
given basicEmpty: QSort[HNil, HNil] = new Sort[HNil] { type Result = HNil }
```

귀납 공리:

```scala
given inductive[N <: Nat, T <: HList, L <: HList, R <: HList, SL <: HList, SR <: HList, O <: HList] (
  using
  Partition[N :: T, N :: L, R],
  QSort[L, SL],
  QSort[R, SR],
  Concat[SL, N :: SR, O]
): QSort[N :: T, O] = new Sort[N :: T] { type Result = O }
```

자동 추론 정렬의 전체 코드:

```scala
trait Sort[HL <: HList] {
  type Result <: HList
}
object Sort {
  type QSort[HL <: HList, O <: HList] = Sort[HL] { type Result = O }
  given basicEmpty: QSort[HNil, HNil] = new Sort[HNil] { type Result = HNil }
  given inductive[N <: Nat, T <: HList, L <: HList, R <: HList, SL <: HList, SR <: HList, O <: HList] (
    using
    Partition[N :: T, N :: L, R],
    QSort[L, SL],
    QSort[R, SR],
    Concat[SL, N :: SR, O]
  ): QSort[N :: T, O] = new Sort[N :: T] { type Result = O }

  def apply[L <: HList](using sort: Sort[L]): QSort[L, sort.Result] = sort
}
```

이제 다음과 같이 테스트할 수 있다:

```scala
val qsort = Sort.apply[_4 :: _3 :: _5 :: _1 :: _2 :: HNil]
```

결과 타입이 무엇일까? 컴파일러는 알고 있지만, 런타임에는 타입이 소거된다. 타입 정보를 런타임에 보존하려면 Rob Norris의 typename 라이브러리를 사용할 수 있다.

`build.sbt`에 추가:

```scala
libraryDependencies += "org.tpolecat" %% "typename" % "1.0.0"
```

헬퍼 메서드를 작성한다:

```scala
def printType[A](value: A)(using typename: TypeName[A]): String = typename.value
```

출력해 보면:

```scala
printType(qsort).replaceAll("com.rockthejvm.blog.typelevelprogramming.TypeLevelProgramming.", "") // 완전 한정 타입명 접두사 제거
```

결과는 다음과 같다:

```scala
Sort[::[_4, ::[_3, ::[_5, ::[_1, ::[_2, HNil]]]]]] {
  type Result >:
    ::[Succ[_0], ::[Succ[_1], ::[Succ[_2], ::[Succ[_3], ::[Succ[_4], HNil]]]]]
  <: ::[Succ[_0], ::[Succ[_1], ::[Succ[_2], ::[Succ[_3], ::[Succ[_4], HNil]]]]]
}
```

결과 타입은 `::[Succ[_0], ::[Succ[_1], ::[Succ[_2], ::[Succ[_3], ::[Succ[_4], HNil]]]]]`, 즉 `_1 :: _2 :: _3 :: _4 :: _5 :: HNil`이다. 올바르게 정렬된 리스트다!

## 결론

Scala에서 가장 멋진 퀵소트가 아니냐고 묻는다면, 인정하지 않을 수 없다. 컴파일러가 타입 추론만으로 퀵소트를 수행하고, 잘못된 정렬 결과는 아예 컴파일이 되지 않는다. Scala의 타입 시스템이 얼마나 강력한지 보여주는 좋은 예다.
