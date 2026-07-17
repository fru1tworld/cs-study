# 커스텀 컬렉션 구현 및 연산 추가

---

## 목차

1. [왜 그냥 상속만으로는 부족한가](#1-왜-그냥-상속만으로는-부족한가)
2. [커스텀 컬렉션 만들기: 최소 요건](#2-커스텀-컬렉션-만들기-최소-요건)
3. [예시: 개수 제한이 있는 `Capped` 컬렉션](#3-예시-개수-제한이-있는-capped-컬렉션)
4. [`Seq` 계열: 반환 타입까지 구체화하기 (`RNA` 예시)](#4-seq-계열-반환-타입까지-구체화하기-rna-예시)
5. [가변 컬렉션 만들기: `PrefixMap` 예시](#5-가변-컬렉션-만들기-prefixmap-예시)
6. [기존 컬렉션에 새 연산 추가하기](#6-기존-컬렉션에-새-연산-추가하기)
7. [연산이 컬렉션을 만들어 낼 때: `Factory`와 `BuildFrom`](#7-연산이-컬렉션을-만들어-낼-때-factory와-buildfrom)
8. [정리](#8-정리)

---

## 1. 왜 그냥 상속만으로는 부족한가

> **원문:** https://docs.scala-lang.org/overviews/core/custom-collections.html

`List`, `Vector` 같은 표준 컬렉션이 `map`, `filter`, `take` 같은 연산을 호출하면
**원본과 같은 종류**의 컬렉션을 돌려준다는 점은 14번 문서에서 다뤘습니다. 문제는
이 "같은 종류를 돌려준다"는 성질이 컬렉션 트레이트를 상속하기만 해서는 저절로 따라오지
않는다는 것입니다.

```scala
class Capped[A](limit: Int, elems: List[A]) extends Iterable[A]:
  def iterator: Iterator[A] = elems.iterator
```

이렇게만 만들면 `capped.map(_ * 2)`는 `Capped[A]`가 아니라 그냥 `Iterable[Int]`를
돌려줍니다. 컴파일러 입장에서는 `Capped`가 "요소를 하나씩 꺼낼 수 있는 것" 이상의
정보를 갖고 있지 않기 때문입니다.

> 💡 **왜 필요한가 — `CanBuildFrom`을 없앤 대신 생긴 새 계약**
>
> Scala 2.12까지는 이 문제를 암시적 `CanBuildFrom` 값으로 해결했는데, 시그니처가
> 복잡하고 컴파일 속도에도 부담을 줬습니다. 2.13부터는 컬렉션 자신이 **"나를 새로
> 만드는 방법"을 스스로 아는 메서드 몇 개**를 구현하도록 바꿨습니다. 이 문서가
> 다루는 `fromSpecific`, `newSpecificBuilder`, `empty` 등이 바로 그 계약입니다.

즉 커스텀 컬렉션을 표준 프레임워크에 제대로 편입시키려면, "요소를 순회하는 방법"뿐
아니라 "**나 자신과 같은 종류를 새로 만드는 방법**"까지 알려줘야 합니다.

---

## 2. 커스텀 컬렉션 만들기: 최소 요건

새 컬렉션 타입을 만들 때 결정하고 구현해야 할 것을 순서대로 정리하면 다음과 같습니다.

1. **불변/가변 선택** — `scala.collection.immutable` 또는 `scala.collection.mutable` 중 하나를 상속
2. **어떤 모양인가 선택** — `Iterable`, `Seq`, `Map`, `Set` 중 실제 성격에 맞는 트레이트
3. **`~Ops` 트레이트 함께 상속** — 예: `IterableOps`, `SeqOps`, `MapOps`. 이 트레이트가
   `map`, `filter` 등의 **일반 구현**을 제공하고, 그 결과 타입을 커스텀 타입으로 맞춰 줌
4. **필수 메서드 구현**

| 메서드 | 역할 |
|---|---|
| `iterator: Iterator[A]` | 요소를 순서대로 꺼내는 방법 |
| `fromSpecific(it: IterableOnce[A]): C` | 주어진 요소들로부터 **같은 종류의** 컬렉션을 새로 만듦 |
| `newSpecificBuilder: Builder[A, C]` | 같은 종류의 컬렉션을 채워 나가는 빌더 생성 |
| `empty: C` | 빈 컬렉션 인스턴스 |

보통 이 네 메서드를 컬렉션 인스턴스마다 직접 구현하는 대신, **팩토리 객체**
(`IterableFactory[Custom]` 등)에 모아두고 `iterableFactory` 필드로 연결하는 편이
코드 중복이 적습니다. `IterableFactoryDefaults` 트레이트를 섞으면 이 연결을
자동으로 처리해 주기도 합니다.

> ⚠️ **짚고 넘어가기 — "타입 하나만 상속"이 아니다**
>
> 커스텀 컬렉션 선언부에는 보통 트레이트가 두세 개 나란히 붙습니다
> (`extends Iterable[A] with IterableOps[...] with IterableFactoryDefaults[...]`).
> 낯설어 보이지만, 각각 "무엇으로 보일지"(`Iterable`), "연산을 어떻게 구현할지"(`~Ops`),
> "결과 타입을 어떻게 결정할지"(`~FactoryDefaults`)를 나눠 맡는 것뿐입니다.

---

## 3. 예시: 개수 제한이 있는 `Capped` 컬렉션

최근 추가된 순서대로 최대 `capacity`개까지만 담고, 넘치면 오래된 것부터 버리는
컬렉션을 만든다고 합시다.

```scala
import scala.collection.{Iterable, IterableFactory, IterableFactoryDefaults, IterableOps}
import scala.collection.mutable.Builder

class Capped[A] private (capacity: Int, elems: Vector[A])
    extends Iterable[A]
    with IterableOps[A, Capped, Capped[A]]
    with IterableFactoryDefaults[A, Capped]:

  def iterator: Iterator[A] = elems.iterator

  def push(a: A): Capped[A] =
    val next = (elems :+ a).takeRight(capacity)
    new Capped(capacity, next)

  override def iterableFactory: IterableFactory[Capped] =
    Capped.factory(capacity)

object Capped:
  private def factory(capacity: Int): IterableFactory[Capped] =
    new IterableFactory[Capped]:
      def empty[A]: Capped[A] = new Capped(capacity, Vector.empty)
      def from[A](source: IterableOnce[A]): Capped[A] =
        new Capped(capacity, source.iterator.toVector.takeRight(capacity))
      def newBuilder[A]: Builder[A, Capped[A]] =
        Vector.newBuilder[A].mapResult(v => new Capped(capacity, v.takeRight(capacity)))

  def apply[A](capacity: Int): Capped[A] = new Capped(capacity, Vector.empty)
```

여기서 `IterableOps[A, Capped, Capped[A]]`가 바로 1절에서 빠져 있던 조각입니다.
이 트레이트가 `map`, `filter` 등의 실제 구현을 제공하고, `IterableFactoryDefaults`는
그 구현이 참조하는 `fromSpecific`, `newSpecificBuilder`, `empty` 세 메서드를
`iterableFactory`를 이용해 **자동으로** 채워 줍니다. 즉 `IterableOps`가 "로직"을,
`IterableFactoryDefaults`가 "그 로직이 만들어 낼 타입"을 맡는 구조입니다.
그 결과 `capped.filter(_ > 0)`, `capped.map(_ + 1)` 모두 `Iterable[Int]`가 아니라
`Capped[Int]`를 돌려줍니다.

성능이 중요한 경우 `StrictOptimizedIterableOps`를 함께 섞어서 `map`/`filter` 등이
중간 뷰(view)를 거치지 않고 곧바로 빌더에 쌓도록 최적화할 수도 있습니다.

---

## 4. `Seq` 계열: 반환 타입까지 구체화하기 (`RNA` 예시)

`Iterable`보다 더 구체적인 `Seq`, `IndexedSeq` 등을 상속할 때는 한 가지 문제가
더 남습니다. `IterableOps[A, CC, C]`의 두 번째 타입 파라미터 `CC`는 "요소 타입이
바뀌었을 때 무엇을 돌려줄지"를 나타내는데, 기본 시그니처로는 `map(f: A => B): CC[B]`처럼
**일반적인 컨테이너**만 돌려줄 수 있습니다.

예를 들어 염기(`Base`) 네 종류만 담는 `RNA` 시퀀스가 있다면:

```scala
enum Base:
  case A, C, G, U

class RNA private (data: IndexedSeq[Base])
    extends IndexedSeq[Base]
    with IndexedSeqOps[Base, IndexedSeq, RNA]:

  def apply(i: Int): Base = data(i)
  def length: Int = data.length

  // Base -> Base 인 경우에 한해 결과를 다시 RNA로 좁혀 줌
  def map(f: Base => Base): RNA = RNA.fromSpecific(view.map(f))
```

기본 상속만으로는 `rna.map(_.complement)`가 `IndexedSeq[Base]`를 돌려주지만,
위처럼 **`Base => Base`를 받는 `map`을 오버로드**해 두면 같은 호출이 `RNA`를
돌려주도록 좁힐 수 있습니다. 공식 문서는 이런 식으로 `map`, `flatMap`, `collect`,
`concat`, `appended`, `prepended` 등 컬렉션을 만들어 내는 연산들을 상황에 맞게
오버로드하도록 권장합니다.

| 트레이트 | 흔히 오버로드하는 연산 |
|---|---|
| `Iterable` | `map`, `flatMap`, `collect`, `concat` |
| `Seq` | `prepended`, `appended`, `patch` |
| `Map` | `map`, `flatMap`, `concat` |
| `SortedSet` | `map`, `flatMap`, `collect`, `zip` |

> 📘 **처음 배우는 분께 — "이중 정의"가 아니라 "특수한 경우 우선"**
>
> `Base => Base`를 받는 `map`을 따로 정의해도, `Base => String`처럼 다른 타입을
> 반환하는 함수를 넘기면 상위 트레이트가 제공하는 일반 `map`이 대신 호출됩니다.
> 즉 "특수한 입력에 대해서만 더 구체적인 반환 타입을 준다"는 뜻이지, 기존 동작을
> 없애는 것이 아닙니다.

---

## 5. 가변 컬렉션 만들기: `PrefixMap` 예시

가변 컬렉션도 원리는 같지만, `MapOps` 대신 `mutable.MapOps`를 쓰고 "제자리 수정"을
위한 메서드 두 개(`addOne`, `subtractOne`)를 추가로 구현해야 합니다.

```scala
import scala.collection.mutable

class PrefixMap[A] extends mutable.Map[String, A]
    with mutable.MapOps[String, A, mutable.Map, PrefixMap[A]]:

  private val inner = mutable.Map.empty[String, A]

  def get(key: String): Option[A] = inner.get(key)
  def iterator: Iterator[(String, A)] = inner.iterator

  def addOne(kv: (String, A)): this.type =
    inner += kv; this

  def subtractOne(key: String): this.type =
    inner -= key; this
```

`+=`, `-=`처럼 익숙한 가변 연산자는 내부적으로 `addOne`/`subtractOne`을 호출하도록
표준 라이브러리가 이미 배선해 두었으므로, 이 두 메서드만 채우면 됩니다.

---

## 6. 기존 컬렉션에 새 연산 추가하기

> **원문:** https://docs.scala-lang.org/overviews/core/custom-collection-operations.html

새 컬렉션 타입을 만드는 대신, **기존 컬렉션에 메서드를 하나 추가**하고 싶은 경우가
훨씬 흔합니다. 이때 고려할 것은 "이 연산이 컬렉션을 **소비만** 하는지, 아니면
**새 컬렉션을 만들어 내는지**"입니다.

### 그냥 훑기만 하는 연산

값을 다 읽어서 하나의 결과(숫자, 불리언 등)로 접는 연산이라면, 굳이 구체 타입을
요구할 필요 없이 `IterableOnce[A]`를 받는 확장 메서드로 충분합니다.

```scala
extension [A](coll: IterableOnce[A])
  def sumBy[B](f: A => B)(using num: Numeric[B]): B =
    val it = coll.iterator
    var acc = num.zero
    while it.hasNext do acc = num.plus(acc, f(it.next()))
    acc

List(1, 2, 3).sumBy(_ * 10)   // 60
```

다만 이 형태는 `String`이나 `Array`처럼 "컬렉션은 아니지만 컬렉션처럼 동작하는"
타입에는 바로 적용되지 않습니다. 이를 지원하려면 `IsIterable[Repr]`(시퀀스라면
`IsSeq[Repr]`, 맵이라면 `IsMap[Repr]`) 타입 클래스를 함께 받아, `Repr`을
`IterableOps`로 변환해서 사용합니다.

```scala
extension [Repr](coll: Repr)(using it: IsIterable[Repr])
  def sumBy2[B](f: it.A => B)(using num: Numeric[B]): B =
    it(coll).iterator.map(f).foldLeft(num.zero)(num.plus)

"abc".sumBy2(_.toInt)   // String에도 그대로 동작
```

> 💡 **왜 필요한가 — `String`은 `Iterable`을 상속하지 않는데 왜 되는가**
>
> `String`은 성능·Java 상호운용 때문에 실제로 `scala.collection.Iterable`을
> 상속하지 않습니다. 대신 암시적 변환으로 `Iterable`처럼 다룰 수 있게 되어 있는데,
> 파라미터 타입을 `Iterable[A]`로 못 박으면 이 변환이 두 단계로 겹쳐 적용되어야 해서
> 컴파일러가 포기합니다. `IsIterable` 타입 클래스는 "컬렉션처럼 보이는 것"을 한 단계
> 변환으로 처리해 이 문제를 피합니다.

---

## 7. 연산이 컬렉션을 만들어 낼 때: `Factory`와 `BuildFrom`

### 결과 타입을 호출자가 정하는 경우: `Factory`

원소들을 모아 **어떤 컬렉션이든** 만들어 주고 싶다면(호출하는 쪽에서 결과 타입을
지정), `Factory[A, C]`를 받습니다.

```scala
trait Factory[-A, +C]:
  def fromSpecific(it: IterableOnce[A]): C
  def newBuilder: Builder[A, C]
```

```scala
def fill[A, C](n: Int)(elem: => A)(using factory: Factory[A, C]): C =
  factory.fromSpecific(Iterator.fill(n)(elem))

val xs: List[Int] = fill(3)(0)      // List(0, 0, 0)
val ys: Vector[Int] = fill(3)(0)    // Vector(0, 0, 0)
```

### 입력 컬렉션과 같은 종류로 돌려주고 싶은 경우: `BuildFrom`

반대로 "입력으로 받은 것과 **같은 종류**의 컬렉션을 돌려주고 싶다"면
`BuildFrom[From, A, C]`를 씁니다. `Factory`에 "원본이 무엇이었는지" 정보가
하나 더 붙은 버전입니다.

```scala
trait BuildFrom[-From, -A, +C]:
  def fromSpecific(from: From)(it: IterableOnce[A]): C
  def newBuilder(from: From): Builder[A, C]
```

예를 들어 원소 사이사이에 구분자를 끼워 넣는 `intersperse` 연산을 만들면:

```scala
extension [Repr](coll: Repr)(using seq: IsSeq[Repr])
  def intersperse[B >: seq.A, That](sep: B)(using bf: BuildFrom[Repr, B, That]): That =
    val elems = seq(coll).iterator
    val builder = bf.newBuilder(coll)
    if elems.hasNext then builder += elems.next()
    while elems.hasNext do
      builder += sep
      builder += elems.next()
    builder.result()

List(1, 2, 3).intersperse(0)   // List(1, 0, 2, 0, 3) — List로 돌아옴
"abc".intersperse(' ')         // "a b c" — String으로 돌아옴
```

`BuildFrom`이 컴파일 시점에 "`List`가 들어오면 `List`를 만드는 빌더를,
`String`이 들어오면 `String`을 만드는 빌더를" 자동으로 골라 주기 때문에,
`map`이나 `filter`가 그런 것처럼 **입력과 같은 모양의 출력**이 보장됩니다.

> ⚠️ **짚고 넘어가기 — `Factory` vs `BuildFrom`, 언제 뭘 쓰나**
>
> - 결과 타입이 **호출부의 타입 애너테이션**으로 결정돼야 한다 → `Factory`
> - 결과 타입이 **입력으로 받은 컬렉션과 같아야** 한다 → `BuildFrom`
>
> `map`, `filter`, `intersperse`처럼 "받은 것과 같은 종류를 돌려주는" 연산은
> 거의 항상 `BuildFrom`을 씁니다. 반면 `List.fill`처럼 입력 컬렉션 자체가 없는
> 생성 연산은 `Factory`만으로 충분합니다.

---

## 8. 정리

| 상황 | 필요한 것 |
|---|---|
| 컬렉션을 처음부터 새로 설계 | `Iterable`/`Seq`/`Map`/`Set` + 해당 `~Ops` + `iterator`/`fromSpecific`/`newSpecificBuilder`/`empty` |
| 반환 타입까지 구체 타입으로 좁히고 싶음 | 관심 있는 입력 타입에 대해 `map`/`flatMap`/`concat` 등을 오버로드 |
| 가변 컬렉션 | `mutable.~Ops` + `addOne`/`subtractOne` |
| 기존 컬렉션을 그냥 훑어 값 하나로 접는 연산 추가 | `IterableOnce[A]` 또는 `IsIterable`/`IsSeq`/`IsMap`을 받는 확장 메서드 |
| 호출자가 결과 컬렉션 타입을 지정 | `Factory[A, C]` |
| 입력과 같은 종류의 컬렉션을 돌려줌 | `BuildFrom[From, A, C]` |

핵심은 한 문장으로 요약됩니다: Scala 컬렉션 프레임워크는 "이 연산이 어떤 컬렉션을
받아 어떤 컬렉션을 돌려주는가"를 타입 클래스(`IsIterable`, `Factory`, `BuildFrom`)로
분리해 두었고, 커스텀 컬렉션이나 커스텀 연산도 이 계약을 따르기만 하면 `map`,
`filter` 같은 표준 연산과 똑같은 방식으로 "같은 종류를 돌려준다"는 성질을 얻을 수
있다는 것입니다.

---

## 참고 자료

- [Implementing Custom Collections](https://docs.scala-lang.org/overviews/core/custom-collections.html)
- [Adding New Operations to Collections](https://docs.scala-lang.org/overviews/core/custom-collection-operations.html)
- [14. 컬렉션 개요와 아키텍처](14_collections-overview-architecture.md)
- [17. 컬렉션 변환 연산](17_collection-transformation-operations.md)
