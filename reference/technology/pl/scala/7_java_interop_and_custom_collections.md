# Java 컬렉션 변환과 커스텀 컬렉션 구현

## Java와 Scala 컬렉션 간 변환

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html

---

### 목차

1. [왜 변환이 필요한가](#1-왜-변환이-필요한가)
2. [`CollectionConverters` — `asJava` / `asScala`](#2-collectionconverters--asjava--asscala)
3. [양방향 변환 쌍](#3-양방향-변환-쌍)
4. [단방향 변환 (Java로만 가는 변환)](#4-단방향-변환-java로만-가는-변환)
5. [변환의 동작 원리 — 래퍼(wrapper)이지 복사가 아니다](#5-변환의-동작-원리--래퍼wrapper이지-복사가-아니다)
6. [주의할 점 — 불변 컬렉션을 Java로 넘길 때](#6-주의할-점--불변-컬렉션을-java로-넘길-때)
7. [정리](#7-정리)

---

### 1. 왜 변환이 필요한가

Scala는 JVM 위에서 동작 → Java 라이브러리와 자유롭게 상호운용(interop) 가능 — 다만 컬렉션만큼은 Java와 Scala가 서로 다른 타입 체계를 가짐.

- Java 컬렉션(`java.util.List`, `java.util.Map` 등)
  - 기본적으로 가변(mutable)
  - 인터페이스 하나에 가변/불변 구분 없음
- Scala 컬렉션
  - `scala.collection.immutable`과 `scala.collection.mutable`로 가변성을 타입으로 구분

→ Java API를 호출하거나 Java 라이브러리에 값을 넘길 때(예: Scala `List`를 Java 메서드가 요구하는 `java.util.List`로 바꿔야 하는 경우) 변환이 자주 필요.
→ 매번 수동으로 순회하며 새 컬렉션을 만드는 건 번거로움 → 표준 라이브러리가 확장 메서드(extension method) 형태로 변환을 제공.

참고 — 확장 메서드로 제공된다는 것:
`asJava`, `asScala`는 Java나 Scala 컬렉션 타입 자체에 원래 있는 메서드가 아님. 특정 객체를 `import`하면 마치 그 타입에 원래 있던 메서드처럼 호출할 수 있게 되는 암시적 확장(implicit extension) — Scala 2의 `implicit class`, Scala 3의 `extension`이 이 메커니즘을 뒷받침(자세한 내용은 `03_contextual_extensions_typeclasses.md` 참고).

---

### 2. `CollectionConverters` — `asJava` / `asScala`

변환 기능은 `scala.jdk.CollectionConverters` 객체에 모여 있음.

```scala
// Scala 2.13
import scala.jdk.CollectionConverters._

// Scala 3
import scala.jdk.CollectionConverters.*
```

이 객체를 임포트하면 두 개의 확장 메서드가 활성화됨.

- `.asJava`
  - 방향: Scala → Java
  - 설명: Scala 컬렉션을 대응하는 Java 컬렉션 인터페이스로 변환
- `.asScala`
  - 방향: Java → Scala
  - 설명: Java 컬렉션을 대응하는 Scala 컬렉션 타입으로 변환

```scala
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

val buf: mutable.Buffer[Int] = mutable.ArrayBuffer(1, 2, 3)
val javaList: java.util.List[Int] = buf.asJava   // Scala -> Java

val fromJava: mutable.Buffer[Int] = javaList.asScala   // Java -> Scala, 다시 원래 타입으로
```

주의 — 옛 이름 `JavaConverters`, `JavaConversions`:
Scala 2.13 이전에는 `scala.collection.JavaConverters`(명시적 `.asJava`/`.asScala` 방식)와, 자동으로 암시적 변환이 걸리던 `scala.collection.JavaConversions`(지금은 제거됨) 두 가지가 존재. 현재는 `scala.jdk.CollectionConverters` 하나로 통합 → 명시적으로 `.asJava`/`.asScala`를 호출하는 방식만 남음. 오래된 코드나 라이브러리에서 `JavaConverters`를 임포트하는 걸 봐도 같은 역할의 옛 이름일 뿐.

---

### 3. 양방향 변환 쌍

다음 타입들은 양쪽 어느 방향으로 변환해도 원래 타입으로 되돌아오는 쌍 — 즉 `x.asJava.asScala`가 다시 원래와 같은 종류의 컬렉션이 됨.

- Scala 타입 · Java 타입
  - `Iterator` · `java.util.Iterator`
  - `Iterator` · `java.util.Enumeration`
  - `Iterable` · `java.lang.Iterable`
  - `Iterable` · `java.util.Collection`
  - `mutable.Buffer` · `java.util.List`
  - `mutable.Set` · `java.util.Set`
  - `mutable.Map` · `java.util.Map`
  - `mutable.ConcurrentMap` · `java.util.concurrent.ConcurrentMap`

공통점 — 왼쪽이 전부 가변(mutable) 컬렉션이거나, 가변성을 따지지 않는 최상위 트레이트(`Iterator`, `Iterable`)임. Java 컬렉션은 원래 가변이 기본 → Scala의 가변 컬렉션과 자연스럽게 짝을 이룸.

```scala
import scala.jdk.CollectionConverters.*

val jMap = new java.util.HashMap[String, Int]()
jMap.put("a", 1)

val sMap: scala.collection.mutable.Map[String, Int] = jMap.asScala
sMap("b") = 2          // Scala 쪽에서 수정하면
jMap.get("b")           // Java 쪽 원본에도 반영됨 (2)
```

---

### 4. 단방향 변환 (Java로만 가는 변환)

아래 타입들은 Java 쪽으로 감쌀 수는 있지만, 대응하는 왕복 쌍이 없어서 한쪽 방향(one-way)으로만 씀.

- `Seq`, `Set`, `Map` 같은 불변(immutable) 컬렉션
  - Java 타입 체계가 "수정 불가"를 표현하지 못함 → 애초에 되돌아오는 쌍을 정의하지 않음
- `mutable.Seq`
  - 가변이긴 하지만 원소 추가/삭제(`add`/`remove`) 연산이 없는 고정 크기 컬렉션
  - → `java.util.List`가 요구하는 연산을 전부 지원하지 못함
  - (반면 3번의 `mutable.Buffer`는 크기 변경까지 지원 → 양방향 쌍이 됨)

- Scala 타입 · Java 타입
  - `Seq` · `java.util.List`
  - `mutable.Seq` · `java.util.List`
  - `Set` · `java.util.Set`
  - `Map` · `java.util.Map`

```scala
import scala.jdk.CollectionConverters.*

val immutableList: List[Int] = List(1, 2, 3)
val jList: java.util.List[Int] = immutableList.asJava   // Scala 불변 -> Java

jList.add(4)   // 컴파일은 되지만 실행 시 예외!
```

왜 필요한가 — 그런데 왜 굳이 `asJava`를 열어두나:
대부분의 Java API는 인자로 `java.util.List` 등 인터페이스 타입만 요구할 뿐, 그 리스트를 수정하지는 않음(읽기 전용 용도로만 순회하는 경우가 대부분). 이런 상황에서는 Scala의 불변 `List`를 감싸서 넘겨주는 것만으로 충분. 문제는 Java 쪽 코드가 실수로(혹은 의도적으로) `add`나 `remove`를 호출할 때 발생 — 아래 6번 항목에서 다룸.

---

### 5. 변환의 동작 원리 — 래퍼(wrapper)이지 복사가 아니다

`asJava`/`asScala`는 데이터를 복사하지 않음 — 대신 원본 컬렉션을 감싸는 래퍼 객체(wrapper object)를 하나 만들어서, 모든 연산을 그 안의 원본 컬렉션으로 그대로 전달(forward)함.

- 래퍼를 통해 원소를 추가/삭제하면 원본 컬렉션에도 즉시 반영됨(3번 예시가 이를 보여줌).
- 같은 이유로, 왕복 변환(round-trip)은 원본 객체를 그대로 돌려줌 — 예를 들어 `mutable.Buffer`를 `.asJava.asScala`로 왕복시키면 매번 새 복사본이 생기는 게 아니라 원래 그 `Buffer` 인스턴스를 다시 얻음.

```scala
import scala.jdk.CollectionConverters.*

val original = scala.collection.mutable.Buffer(1, 2, 3)
val roundTrip = original.asJava.asScala

roundTrip eq original   // true — 새 컬렉션이 아니라 같은 객체
```

참고 — "래퍼"라는 게 뭘 뜻하나:
`asJava`를 호출해서 얻은 `java.util.List`는 사실 원본 Scala 컬렉션을 감싸고 있는 얇은 껍데기. 그 껍데기의 `add`, `get`, `size` 같은 메서드 구현부는 전부 "원본 Scala 컬렉션의 대응 연산을 호출해라"라고만 되어 있음. 그래서 두 "겉모습"(Java 쪽 `List`, Scala 쪽 `Buffer`)은 사실 하나의 데이터를 공유하는 서로 다른 창(window)일 뿐임.

---

### 6. 주의할 점 — 불변 컬렉션을 Java로 넘길 때

Scala의 `scala.collection.immutable.List` 같은 불변 컬렉션을 `.asJava`로 감싸서 Java 코드에 넘기면, 그 Java 쪽 `java.util.List`는 컴파일 시점에는 평범한 가변 리스트처럼 보임 — Java 타입 체계에는 "이건 수정 불가"를 표현할 방법이 없기 때문.

- `add`, `remove`, `set` 같은 변경 연산을 호출하면 컴파일은 통과하지만, 런타임에 `UnsupportedOperationException`이 발생.
- 이는 버그가 아니라 의도된 안전장치 — 원본이 불변이라는 계약을 실제로 어기는 순간 예외로 막아 줌.

```scala
import scala.jdk.CollectionConverters.*

val wrapped: java.util.List[Int] = List(1, 2, 3).asJava

wrapped.get(0)     // 1 — 읽기는 문제 없음
wrapped.add(4)     // java.lang.UnsupportedOperationException
```

주의 — Java 쪽 개발자에게 미리 알려야 하는 이유:
이 예외는 코드를 실행하기 전까지는 드러나지 않음. Scala 쪽에서 불변 컬렉션을 Java API에 넘길 때는, 그 Java 코드가 리스트를 읽기 전용으로만 쓰는지 미리 확인하는 게 안전. 반대로 Java 쪽에서 정말 값을 채워 넣어야 한다면, Scala 쪽 가변 컬렉션(`mutable.Buffer` 등, 3번 참조)으로 넘기거나, `.asJava` 결과를 감싼 뒤 Java 쪽에서 방어적으로 복사(`new ArrayList<>(wrapped)`)해서 쓰는 편이 나음.

---

### 7. 정리

- 변환은 `scala.jdk.CollectionConverters`를 임포트한 뒤 `.asJava` / `.asScala`를 호출하는 것으로 끝.
- `Iterator`, `Iterable`과 가변 컬렉션(`Buffer`, `Set`, `Map`, `ConcurrentMap`)은 양방향으로 자연스럽게 왕복됨.
- 불변 컬렉션(`Seq`, `Set`, `Map`)과 크기 변경이 안 되는 `mutable.Seq`는 Java 쪽으로 가는 단방향만 제공 — 불변 컬렉션을 Java 쪽에서 수정하려 하면 `UnsupportedOperationException` 발생.
- 모든 변환은 복사가 아니라 래퍼 → 한쪽에서 가변 컬렉션을 수정하면 다른 쪽에도 즉시 반영됨.

---

### 참고 자료

- [Conversions Between Java and Scala Collections (공식 문서)](https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html)
- [Scala Collections 개요](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html)

---

## 커스텀 컬렉션 구현 및 연산 추가

---

### 목차

1. [왜 그냥 상속만으로는 부족한가](#1-왜-그냥-상속만으로는-부족한가)
2. [커스텀 컬렉션 만들기: 최소 요건](#2-커스텀-컬렉션-만들기-최소-요건)
3. [예시: 개수 제한이 있는 `Capped` 컬렉션](#3-예시-개수-제한이-있는-capped-컬렉션)
4. [`Seq` 계열: 반환 타입까지 구체화하기 (`RNA` 예시)](#4-seq-계열-반환-타입까지-구체화하기-rna-예시)
5. [가변 컬렉션 만들기: `PrefixMap` 예시](#5-가변-컬렉션-만들기-prefixmap-예시)
6. [기존 컬렉션에 새 연산 추가하기](#6-기존-컬렉션에-새-연산-추가하기)
7. [연산이 컬렉션을 만들어 낼 때: `Factory`와 `BuildFrom`](#7-연산이-컬렉션을-만들어-낼-때-factory와-buildfrom)
8. [정리](#8-정리)

---

### 1. 왜 그냥 상속만으로는 부족한가

> 원문: https://docs.scala-lang.org/overviews/core/custom-collections.html

`List`, `Vector` 같은 표준 컬렉션이 `map`, `filter`, `take` 같은 연산을 호출하면 원본과 같은 종류의 컬렉션을 돌려줌 — 14번 문서에서 다룬 내용. 문제는 이 "같은 종류를 돌려준다"는 성질이 컬렉션 트레이트를 상속하기만 해서는 저절로 따라오지 않음.

```scala
class Capped[A](limit: Int, elems: List[A]) extends Iterable[A]:
  def iterator: Iterator[A] = elems.iterator
```

이렇게만 만들면 `capped.map(_ * 2)`는 `Capped[A]`가 아니라 그냥 `Iterable[Int]`를 돌려줌 → 컴파일러 입장에서는 `Capped`가 "요소를 하나씩 꺼낼 수 있는 것" 이상의 정보를 갖고 있지 않기 때문.

왜 필요한가 — `CanBuildFrom`을 없앤 대신 생긴 새 계약:
Scala 2.12까지는 이 문제를 암시적 `CanBuildFrom` 값으로 해결 — 시그니처가 복잡하고 컴파일 속도에도 부담. 2.13부터는 컬렉션 자신이 "나를 새로 만드는 방법"을 스스로 아는 메서드 몇 개를 구현하도록 바뀜. 이 문서가 다루는 `fromSpecific`, `newSpecificBuilder`, `empty` 등이 바로 그 계약임.

즉 커스텀 컬렉션을 표준 프레임워크에 제대로 편입시키려면, "요소를 순회하는 방법"뿐 아니라 "나 자신과 같은 종류를 새로 만드는 방법"까지 알려줘야 함.

---

### 2. 커스텀 컬렉션 만들기: 최소 요건

새 컬렉션 타입을 만들 때 결정하고 구현해야 할 것을 순서대로 정리:

1. 불변/가변 선택 — `scala.collection.immutable` 또는 `scala.collection.mutable` 중 하나를 상속
2. 어떤 모양인가 선택 — `Iterable`, `Seq`, `Map`, `Set` 중 실제 성격에 맞는 트레이트
3. `~Ops` 트레이트 함께 상속 — 예: `IterableOps`, `SeqOps`, `MapOps`. 이 트레이트가 `map`, `filter` 등의 일반 구현을 제공하고, 그 결과 타입을 커스텀 타입으로 맞춰 줌
4. 필수 메서드 구현
   - `iterator: Iterator[A]` — 요소를 순서대로 꺼내는 방법
   - `fromSpecific(it: IterableOnce[A]): C` — 주어진 요소들로부터 같은 종류의 컬렉션을 새로 만듦
   - `newSpecificBuilder: Builder[A, C]` — 같은 종류의 컬렉션을 채워 나가는 빌더 생성
   - `empty: C` — 빈 컬렉션 인스턴스

보통 이 네 메서드를 컬렉션 인스턴스마다 직접 구현하는 대신, 팩토리 객체(`IterableFactory[Custom]` 등)에 모아두고 `iterableFactory` 필드로 연결하는 편이 코드 중복이 적음. `IterableFactoryDefaults` 트레이트를 섞으면 이 연결을 자동으로 처리해 주기도 함.

주의 — "타입 하나만 상속"이 아니다:
커스텀 컬렉션 선언부에는 보통 트레이트가 두세 개 나란히 붙음(`extends Iterable[A] with IterableOps[...] with IterableFactoryDefaults[...]`). 낯설어 보이지만, 각각 "무엇으로 보일지"(`Iterable`), "연산을 어떻게 구현할지"(`~Ops`), "결과 타입을 어떻게 결정할지"(`~FactoryDefaults`)를 나눠 맡는 것뿐임.

---

### 3. 예시: 개수 제한이 있는 `Capped` 컬렉션

최근 추가된 순서대로 최대 `capacity`개까지만 담고, 넘치면 오래된 것부터 버리는 컬렉션을 만든다고 가정.

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

여기서 `IterableOps[A, Capped, Capped[A]]`가 바로 1절에서 빠져 있던 조각. 이 트레이트가 `map`, `filter` 등의 실제 구현을 제공하고, `IterableFactoryDefaults`는 그 구현이 참조하는 `fromSpecific`, `newSpecificBuilder`, `empty` 세 메서드를 `iterableFactory`를 이용해 자동으로 채워 줌. 즉 `IterableOps`가 "로직"을, `IterableFactoryDefaults`가 "그 로직이 만들어 낼 타입"을 맡는 구조.
그 결과 `capped.filter(_ > 0)`, `capped.map(_ + 1)` 모두 `Iterable[Int]`가 아니라 `Capped[Int]`를 돌려줌.

성능이 중요한 경우 `StrictOptimizedIterableOps`를 함께 섞어서 `map`/`filter` 등이 중간 뷰(view)를 거치지 않고 곧바로 빌더에 쌓도록 최적화할 수도 있음.

---

### 4. `Seq` 계열: 반환 타입까지 구체화하기 (`RNA` 예시)

`Iterable`보다 더 구체적인 `Seq`, `IndexedSeq` 등을 상속할 때는 한 가지 문제가 더 남음. `IterableOps[A, CC, C]`의 두 번째 타입 파라미터 `CC`는 "요소 타입이 바뀌었을 때 무엇을 돌려줄지"를 나타내는데, 기본 시그니처로는 `map(f: A => B): CC[B]`처럼 일반적인 컨테이너만 돌려줄 수 있음.

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

기본 상속만으로는 `rna.map(_.complement)`가 `IndexedSeq[Base]`를 돌려주지만, 위처럼 `Base => Base`를 받는 `map`을 오버로드해 두면 같은 호출이 `RNA`를 돌려주도록 좁힐 수 있음. 공식 문서는 이런 식으로 `map`, `flatMap`, `collect`, `concat`, `appended`, `prepended` 등 컬렉션을 만들어 내는 연산들을 상황에 맞게 오버로드하도록 권장.

흔히 오버로드하는 연산:
- `Iterable` — `map`, `flatMap`, `collect`, `concat`
- `Seq` — `prepended`, `appended`, `patch`
- `Map` — `map`, `flatMap`, `concat`
- `SortedSet` — `map`, `flatMap`, `collect`, `zip`

참고 — "이중 정의"가 아니라 "특수한 경우 우선":
`Base => Base`를 받는 `map`을 따로 정의해도, `Base => String`처럼 다른 타입을 반환하는 함수를 넘기면 상위 트레이트가 제공하는 일반 `map`이 대신 호출됨. 즉 "특수한 입력에 대해서만 더 구체적인 반환 타입을 준다"는 뜻이지, 기존 동작을 없애는 것이 아님.

---

### 5. 가변 컬렉션 만들기: `PrefixMap` 예시

가변 컬렉션도 원리는 같지만, `MapOps` 대신 `mutable.MapOps`를 쓰고 "제자리 수정"을 위한 메서드 두 개(`addOne`, `subtractOne`)를 추가로 구현해야 함.

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

`+=`, `-=`처럼 익숙한 가변 연산자는 내부적으로 `addOne`/`subtractOne`을 호출하도록 표준 라이브러리가 이미 배선해 두었음 → 이 두 메서드만 채우면 됨.

---

### 6. 기존 컬렉션에 새 연산 추가하기

> 원문: https://docs.scala-lang.org/overviews/core/custom-collection-operations.html

새 컬렉션 타입을 만드는 대신, 기존 컬렉션에 메서드를 하나 추가하고 싶은 경우가 훨씬 흔함. 이때 고려할 것은 "이 연산이 컬렉션을 소비만 하는지, 아니면 새 컬렉션을 만들어 내는지".

#### 그냥 훑기만 하는 연산

값을 다 읽어서 하나의 결과(숫자, 불리언 등)로 접는 연산이라면, 굳이 구체 타입을 요구할 필요 없이 `IterableOnce[A]`를 받는 확장 메서드로 충분.

```scala
extension [A](coll: IterableOnce[A])
  def sumBy[B](f: A => B)(using num: Numeric[B]): B =
    val it = coll.iterator
    var acc = num.zero
    while it.hasNext do acc = num.plus(acc, f(it.next()))
    acc

List(1, 2, 3).sumBy(_ * 10)   // 60
```

다만 이 형태는 `String`이나 `Array`처럼 "컬렉션은 아니지만 컬렉션처럼 동작하는" 타입에는 바로 적용되지 않음. 이를 지원하려면 `IsIterable[Repr]`(시퀀스라면 `IsSeq[Repr]`, 맵이라면 `IsMap[Repr]`) 타입 클래스를 함께 받아, `Repr`을 `IterableOps`로 변환해서 사용.

```scala
extension [Repr](coll: Repr)(using it: IsIterable[Repr])
  def sumBy2[B](f: it.A => B)(using num: Numeric[B]): B =
    it(coll).iterator.map(f).foldLeft(num.zero)(num.plus)

"abc".sumBy2(_.toInt)   // String에도 그대로 동작
```

왜 필요한가 — `String`은 `Iterable`을 상속하지 않는데 왜 되는가:
`String`은 성능·Java 상호운용 때문에 실제로 `scala.collection.Iterable`을 상속하지 않음. 대신 암시적 변환으로 `Iterable`처럼 다룰 수 있게 되어 있는데, 파라미터 타입을 `Iterable[A]`로 못 박으면 이 변환이 두 단계로 겹쳐 적용되어야 해서 컴파일러가 포기함. `IsIterable` 타입 클래스는 "컬렉션처럼 보이는 것"을 한 단계 변환으로 처리해 이 문제를 피함.

---

### 7. 연산이 컬렉션을 만들어 낼 때: `Factory`와 `BuildFrom`

#### 결과 타입을 호출자가 정하는 경우: `Factory`

원소들을 모아 어떤 컬렉션이든 만들어 주고 싶다면(호출하는 쪽에서 결과 타입을 지정), `Factory[A, C]`를 받음.

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

#### 입력 컬렉션과 같은 종류로 돌려주고 싶은 경우: `BuildFrom`

반대로 "입력으로 받은 것과 같은 종류의 컬렉션을 돌려주고 싶다"면 `BuildFrom[From, A, C]`를 씀. `Factory`에 "원본이 무엇이었는지" 정보가 하나 더 붙은 버전.

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

`BuildFrom`이 컴파일 시점에 "`List`가 들어오면 `List`를 만드는 빌더를, `String`이 들어오면 `String`을 만드는 빌더를" 자동으로 골라 줌 → `map`이나 `filter`가 그런 것처럼 입력과 같은 모양의 출력이 보장됨.

주의 — `Factory` vs `BuildFrom`, 언제 뭘 쓰나:
- 결과 타입이 호출부의 타입 애너테이션으로 결정돼야 한다 → `Factory`
- 결과 타입이 입력으로 받은 컬렉션과 같아야 한다 → `BuildFrom`

`map`, `filter`, `intersperse`처럼 "받은 것과 같은 종류를 돌려주는" 연산은 거의 항상 `BuildFrom`을 씀. 반면 `List.fill`처럼 입력 컬렉션 자체가 없는 생성 연산은 `Factory`만으로 충분.

---

### 8. 정리

- 컬렉션을 처음부터 새로 설계 → `Iterable`/`Seq`/`Map`/`Set` + 해당 `~Ops` + `iterator`/`fromSpecific`/`newSpecificBuilder`/`empty`
- 반환 타입까지 구체 타입으로 좁히고 싶음 → 관심 있는 입력 타입에 대해 `map`/`flatMap`/`concat` 등을 오버로드
- 가변 컬렉션 → `mutable.~Ops` + `addOne`/`subtractOne`
- 기존 컬렉션을 그냥 훑어 값 하나로 접는 연산 추가 → `IterableOnce[A]` 또는 `IsIterable`/`IsSeq`/`IsMap`을 받는 확장 메서드
- 호출자가 결과 컬렉션 타입을 지정 → `Factory[A, C]`
- 입력과 같은 종류의 컬렉션을 돌려줌 → `BuildFrom[From, A, C]`

핵심 — Scala 컬렉션 프레임워크는 "이 연산이 어떤 컬렉션을 받아 어떤 컬렉션을 돌려주는가"를 타입 클래스(`IsIterable`, `Factory`, `BuildFrom`)로 분리해 두었고, 커스텀 컬렉션이나 커스텀 연산도 이 계약을 따르기만 하면 `map`, `filter` 같은 표준 연산과 똑같은 방식으로 "같은 종류를 돌려준다"는 성질을 얻을 수 있음.

---

### 참고 자료

- [Implementing Custom Collections](https://docs.scala-lang.org/overviews/core/custom-collections.html)
- [Adding New Operations to Collections](https://docs.scala-lang.org/overviews/core/custom-collection-operations.html)
- [14. 컬렉션 개요와 아키텍처](14_collections-overview-architecture.md)
- [17. 컬렉션 변환 연산](17_collection-transformation-operations.md)
