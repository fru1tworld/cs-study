# Scala 이터레이터·뷰·LazyList와 동등성·성능

## 이터레이터, 뷰, LazyList

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/iterators.html , https://docs.scala-lang.org/overviews/collections-2.13/views.html

Scala 컬렉션은 기본적으로 **즉시(strict) 평가**합니다. `map`, `filter` 등을 호출하는 순간 결과 컬렉션이 전부 계산되어 메모리에 만들어집니다. 이 문서에서 다루는 세 가지 — **이터레이터**, **뷰**, **LazyList** — 는 모두 "당장 다 계산하지 않고 필요할 때만 계산한다"는 지연(lazy) 평가와 관련이 있지만, 서로 다른 방식과 목적을 가집니다.

### 목차

1. [세 가지 지연 도구, 한눈에 구분하기](#1-세-가지-지연-도구-한눈에-구분하기)
2. [이터레이터(Iterator)](#2-이터레이터iterator)
   - 2.1 [이터레이터는 컬렉션이 아니다](#21-이터레이터는-컬렉션이-아니다)
   - 2.2 [순회와 소비](#22-순회와-소비)
   - 2.3 [변환 메서드 — 파괴적으로 진행된다](#23-변환-메서드--파괴적으로-진행된다)
   - 2.4 [duplicate로 독립된 사본 만들기](#24-duplicate로-독립된-사본-만들기)
   - 2.5 [BufferedIterator — 미리 엿보기](#25-bufferediterator--미리-엿보기)
   - 2.6 [다른 컬렉션으로 변환하기](#26-다른-컬렉션으로-변환하기)
3. [뷰(View)](#3-뷰view)
   - 3.1 [뷰는 "변환 계획서"다](#31-뷰는-변환-계획서다)
   - 3.2 [view와 force(to)](#32-view와-forceto)
   - 3.3 [중간 컬렉션을 만들지 않는 이유](#33-중간-컬렉션을-만들지-않는-이유)
   - 3.4 [주의: 부작용과 재평가](#34-주의-부작용과-재평가)
4. [LazyList](#4-lazylist)
   - 4.1 [머리도 꼬리도 지연된다](#41-머리도-꼬리도-지연된다)
   - 4.2 [무한열 표현하기](#42-무한열-표현하기)
   - 4.3 [메모이제이션(memoization)에 따른 메모리 누수 주의](#43-메모이제이션memoization에-따른-메모리-누수-주의)
5. [세 도구 비교표](#5-세-도구-비교표)
6. [참고 자료](#6-참고-자료)

---

### 1. 세 가지 지연 도구, 한눈에 구분하기

| 도구 | 정체 | 지연되는 것 | 다시 쓸 수 있나 |
|---|---|---|---|
| **Iterator** | 컬렉션이 아니라 "커서(위치 포인터)" | 원소를 언제 계산할지 | 아니오. 한 번 지나가면 끝(소모성) |
| **View** | 컬렉션을 감싼 "변환 계획서" | `map`/`filter` 같은 변환 실행 시점 | 예. 몇 번이든 다시 순회 가능 |
| **LazyList** | 컬렉션 그 자체(`List`처럼 저장·재사용 가능) | 머리·꼬리 모든 원소의 계산 시점 | 예. 계산한 값은 캐시되어 재사용 |

> 📘 **처음 배우는 분께**
>
> 세 개념을 섞어 생각하기 쉬운데, 질문 하나로 구분하면 됩니다. "이 값, 다시 한번 순회할 수 있는가?" Iterator는 **아니오**(한 번 쓰고 버림), View와 LazyList는 **예**입니다. 그중 View는 "안 쓰면 계산 자체를 안 함"이고, LazyList는 "한 번 계산한 값은 기억해 둠(캐시)"이라는 차이가 있습니다.

---

### 2. 이터레이터(Iterator)

#### 2.1 이터레이터는 컬렉션이 아니다

`Iterator`는 원소를 담아 두는 자료구조가 아니라, **컬렉션의 원소를 하나씩 순서대로 꺼내는 방법**입니다. 핵심 메서드는 딱 두 개입니다.

- `next()` : 다음 원소를 꺼내고 커서를 한 칸 전진시킴. 더 꺼낼 게 없으면 `NoSuchElementException`
- `hasNext` : 꺼낼 원소가 남아 있는지 여부

```scala
val it = Iterator(1, 2, 3)
while it.hasNext do println(it.next())
// 1
// 2
// 3
```

`for (x <- it) ...`, `it.foreach(println)`도 위와 동일하게 동작하는 순회 방법입니다.

#### 2.2 순회와 소비

이터레이터는 "커서"이기 때문에, **한 번 지나간 자리는 되돌아갈 수 없습니다.** 아래처럼 이미 다 꺼낸 이터레이터를 다시 쓰려 하면 예외가 납니다.

```scala
val it = Iterator(1, 2, 3)
it.foreach(println)  // 1 2 3 출력, 커서가 끝에 도달함
it.hasNext            // false — 더 이상 꺼낼 게 없음
```

#### 2.3 변환 메서드 — 파괴적으로 진행된다

`map`, `filter`, `flatMap`, `zip`, `grouped`, `sliding` 같은 메서드는 `List`에서 쓰던 것과 이름은 같지만, 동작 방식이 다릅니다. **원본 이터레이터의 커서를 그대로 소모하며 진행**합니다.

```scala
val it = Iterator(1, 2, 3, 4, 5)
val evens = it.filter(_ % 2 == 0)  // it는 이 시점부터 이미 앞으로 당겨져 있음
evens.toList                       // List(2, 4)
it.hasNext                         // false — it는 이미 다 소모됨
```

계산 자체는 **지연되어** 있습니다. `filter`를 호출한다고 그 자리에서 바로 다 걸러내는 게 아니라, `next()`가 호출될 때마다 필요한 만큼만 검사를 진행합니다. 덕분에 무한한 이터레이터에도 `filter`, `map`을 걸 수 있습니다.

> ⚠️ **짚고 넘어가기 — 이터레이터를 "한 번만" 순회한다는 원칙**
>
> 같은 이터레이터 변수를 여러 메서드에 재사용하면 두 번째부터는 빈 결과만 나옵니다. 이터레이터를 여러 갈래로 처리해야 한다면 아래 `duplicate`를 쓰거나, 애초에 컬렉션(`List` 등)에서 새 이터레이터를 다시 얻어야 합니다.

#### 2.4 duplicate로 독립된 사본 만들기

한 이터레이터를 두 갈래로 나누어 각각 다르게 처리하고 싶다면 `duplicate`를 사용합니다.

```scala
val (a, b) = Iterator(1, 2, 3).duplicate
a.toList  // List(1, 2, 3)
b.toList  // List(1, 2, 3) — a와 독립적으로 소모됨
```

두 이터레이터는 서로 영향을 주지 않고 각자 소모됩니다.

#### 2.5 BufferedIterator — 미리 엿보기

보통의 이터레이터는 "다음 값이 뭔지 미리 보고 싶다"는 요청을 지원하지 않습니다(`next()`를 부르는 순간 커서가 이동해 버리므로). `buffered`를 사용하면 커서를 이동시키지 않고 다음 값을 미리 볼 수 있는 `head` 메서드가 생깁니다.

```scala
val it = Iterator("", "", "hello", "world").buffered
while it.hasNext && it.head.isEmpty do it.next()  // 빈 문자열만 건너뜀
it.next()  // "hello" — 첫 유효한 값을 놓치지 않고 꺼냄
```

`head`로 미리 확인한 값은 커서를 옮기지 않으므로, 조건에 따라 "꺼낼지 말지"를 안전하게 결정할 수 있습니다.

#### 2.6 다른 컬렉션으로 변환하기

이터레이터의 계산 결과를 실제 컬렉션으로 확정하려면 `toList`, `toVector`, `toArray`, `toSet`, `toMap` 등을 사용합니다. 이 시점에 비로소 남은 모든 원소가 계산되어 메모리에 실체화됩니다.

---

### 3. 뷰(View)

#### 3.1 뷰는 "변환 계획서"다

일반 컬렉션에서 `map`, `filter` 같은 변환 메서드는 **즉시(strict)** 실행되어 매번 새 컬렉션을 만듭니다. `.view`를 붙이면 같은 메서드 호출이 실제 계산 없이 "이런 변환을 하겠다"는 **계획만 기록**하고, 최종적으로 결과가 필요할 때(강제할 때)에야 한 번에 계산합니다.

```scala
val v = (1 to 5).toVector

v.map(_ + 1).map(_ * 2)          // 즉시 평가: 중간 Vector가 하나 더 생김
v.view.map(_ + 1).map(_ * 2).toVector  // 지연 평가: 중간 결과 없이 한 번에 계산
```

#### 3.2 view와 force(to)

- `collection.view` : 컬렉션을 뷰로 감쌈(변환 시작)
- `.to(List)`, `.toVector`, `.force` 등 : 뷰에 쌓인 변환을 실제로 실행해 다시 즉시 컬렉션으로 만듦(강제 실행)

```scala
val words = List("apple", "banana", "kiwi", "fig")

val short: List[String] =
  words.view.filter(_.length <= 4).map(_.toUpperCase).to(List)
// List("KIWI", "FIG")
```

`to(List)`를 호출하기 전까지는 `filter`도 `map`도 실제로 아무 원소도 건드리지 않습니다.

#### 3.3 중간 컬렉션을 만들지 않는 이유

`v.map(f).map(g)`처럼 변환을 체이닝하면, 즉시 평가 컬렉션은 **매 단계마다 전체 크기의 새 컬렉션**을 만듭니다. 뷰는 이 단계들을 하나로 합쳐 두었다가, 강제될 때 원소 하나당 `f`와 `g`를 순서대로 한 번씩만 적용합니다. 그래서 뷰의 변환 자체는 원소 개수와 무관하게 O(1)로 취급됩니다(계획만 세우는 것이므로).

큰 컬렉션에서 앞쪽 일부만 필요한 경우에도 뷰가 유리합니다. 예를 들어 백만 개짜리 컬렉션에서 조건에 맞는 원소를 100개만 찾고 싶다면, 즉시 평가로 `filter`부터 하면 전체를 다 훑어 새 컬렉션을 만들지만, 뷰로 `filter`한 뒤 `take(100)`을 하면 조건에 맞는 100개를 찾는 순간 나머지는 건드리지 않습니다.

```scala
words.view.filter(_.length > 3).take(2).toList
```

다만 `sorted`처럼 **전체 원소를 다 봐야만 답이 나오는 연산**은 뷰를 써도 지연의 이점이 없습니다 — 결국 전부 순회해야 하기 때문입니다.

#### 3.4 주의: 부작용과 재평가

뷰는 "언제 실제로 실행되는지"가 코드만 봐서는 눈에 잘 안 띈다는 함정이 있습니다.

```scala
val v = (1 to 3).view.map { i =>
  println(s"계산 중: $i")
  i * 2
}
// 아직 아무것도 출력되지 않음 — map은 계획만 세운 상태

v.toList  // 이제야 "계산 중: 1", "계산 중: 2", "계산 중: 3"이 출력됨
```

또한 뷰를 강제(force)할 때마다 계획된 변환이 **매번 다시 실행**됩니다. 부작용(출력, 액터 생성, DB 호출 등)이 있는 함수를 뷰에 얹으면, 강제하는 횟수만큼 부작용이 반복될 수 있습니다.

> ⚠️ **짚고 넘어가기 — 뷰를 강제하지 않은 채 부작용을 기대하지 말 것**
>
> 뷰는 "결과를 실제로 꺼내 쓸 때"에만 동작이 일어납니다. 부작용을 목적으로 한 코드(로깅, 외부 호출 등)를 뷰 체인 안에 넣었다면, 그 뷰를 `toList`나 `foreach`로 강제하기 전까지는 아무 일도 일어나지 않는다는 점을 꼭 확인해야 합니다.

---

### 4. LazyList

`LazyList`(Scala 2.13에서 옛 `Stream`을 대체)는 `List`와 마찬가지로 **컬렉션 그 자체**입니다. 순서가 있는 원소들의 목록이며, 여러 번 순회할 수 있고 저장해 둘 수도 있습니다. 다만 원소를 **필요한 시점에만 계산**하고, 한 번 계산한 값은 **다시 계산하지 않도록 기억(메모이제이션)** 해 둔다는 점이 `List`와의 차이입니다.

#### 4.1 머리도 꼬리도 지연된다

과거 `Stream`은 `head`를 만드는 시점에 바로 계산해 버려서(꼬리만 지연) 종종 예상 밖의 즉시 평가가 일어났습니다. `LazyList`는 이 문제를 고쳐 **`head`와 `tail` 모두 실제로 접근하기 전까지는 계산하지 않습니다.** `LazyList(1, 2, 3)`을 만드는 것만으로는 아무 원소도 계산되지 않고, `head`나 `tail`을 불러야 그 자리의 값이 비로소 계산됩니다.

```scala
val lz = LazyList(1, 2, 3)  // 아직 아무 원소도 계산되지 않음
lz.head        // 1 — 이 호출 시점에 비로소 계산됨
lz.tail.head   // 2 — 두 번째 원소도 이 시점에 계산됨
```

#### 4.2 무한열 표현하기

계산을 미룰 수 있다는 성질 덕분에 **끝이 없는 목록**도 정의할 수 있습니다. 필요한 만큼만 `take`로 잘라내면 무한 목록이라도 문제없이 다룰 수 있습니다.

```scala
def naturals(n: Int): LazyList[Int] = n #:: naturals(n + 1)

val nats = naturals(1)      // 무한한 자연수 목록
nats.take(5).toList          // List(1, 2, 3, 4, 5) — 앞 5개만 실제로 계산됨
```

`#::`는 `LazyList`에서 `::`(cons) 대신 쓰는 지연 연결 연산자입니다. 오른쪽 항인 `naturals(n + 1)`은 실제로 `tail`에 접근하기 전까지 평가되지 않습니다.

#### 4.3 메모이제이션(memoization)에 따른 메모리 누수 주의

`LazyList`는 한 번 계산한 원소를 캐시에 계속 들고 있습니다. 이 목록을 가리키는 참조를 오래 붙들고 있으면, 이미 다 쓴 앞부분 원소도 메모리에서 해제되지 않고 계속 쌓일 수 있습니다. 무한열이나 아주 긴 `LazyList`를 순회할 때는, 처음 몇 개를 담아 둔 `val` 참조를 계속 유지하지 않도록 주의해야 합니다.

---

### 5. 세 도구 비교표

| 구분 | Iterator | View | LazyList |
|---|---|---|---|
| 재순회 가능 여부 | 불가(1회성) | 가능 | 가능 |
| 계산 결과 캐시 여부 | 해당 없음(순회하며 즉시 소모) | 캐시 안 함(매번 재계산) | 캐시함(메모이제이션) |
| 무한 시퀀스 표현 | 가능 | 원본이 유한하면 유한 | 가능 |
| 대표 사용 시점 | 한 번만 훑고 버릴 대량 데이터 처리 | 중간 컬렉션 없이 변환 체이닝 | 재사용 가능한 지연 목록·무한열 |
| 부작용 함수와 함께 쓸 때 위험 | 낮음(순서대로 한 번씩만 실행) | 높음(강제할 때마다 재실행) | 낮음(한 번 계산되면 캐시됨) |

---

### 6. 참고 자료

- [Scala Collections — Iterators](https://docs.scala-lang.org/overviews/collections-2.13/iterators.html)
- [Scala Collections — Views](https://docs.scala-lang.org/overviews/collections-2.13/views.html)
- [Scala 표준 라이브러리 API — LazyList](https://www.scala-lang.org/api/current/scala/collection/immutable/LazyList.html)

---

## 컬렉션의 동등성과 성능 특성

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/equality.html
> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html

---

### 목차

1. [개요](#1-개요)
2. [컬렉션 동등성(`==`)의 규칙](#2-컬렉션-동등성-의-규칙)
   - 2.1 [규칙 1 — 카테고리가 다르면 무조건 다르다](#21-규칙-1--카테고리가-다르면-무조건-다르다)
   - 2.2 [규칙 2 — 같은 카테고리면 구현이 아니라 내용물로 비교한다](#22-규칙-2--같은-카테고리면-구현이-아니라-내용물로-비교한다)
   - 2.3 [가변 컬렉션과 동등성 — 해시 키로 쓸 때의 함정](#23-가변-컬렉션과-동등성--해시-키로-쓸-때의-함정)
3. [성능 특성 표기법](#3-성능-특성-표기법)
4. [순차 컬렉션(Sequence)의 성능](#4-순차-컬렉션sequence의-성능)
   - 4.1 [불변 시퀀스](#41-불변-시퀀스)
   - 4.2 [가변 시퀀스](#42-가변-시퀀스)
5. [집합/맵(Set/Map)의 성능](#5-집합맵setmap의-성능)
6. [실전 선택 가이드](#6-실전-선택-가이드)

---

### 1. 개요

Scala 컬렉션 라이브러리는 두 가지를 문서 두 페이지로 나누어 설명합니다.

- **동등성(equality)** — `==`로 두 컬렉션을 비교했을 때 언제 같다고 판단되는가.
- **성능 특성(performance characteristics)** — `head`, `apply`, `add` 같은 연산이 각 컬렉션 타입에서 상수 시간인지 선형 시간인지.

둘 다 "컬렉션 타입을 무엇으로 고를지"에 직접 영향을 주는 실전 지식입니다. 동등성 규칙을 모르면 해시맵 키로 가변 컬렉션을 썼다가 원인 모를 버그를 만나고, 성능 특성을 모르면 `List`에 인덱스로 접근하는 코드를 반복문 안에 넣어 놓고 왜 느린지 못 찾게 됩니다.

---

### 2. 컬렉션 동등성(`==`)의 규칙

> 💡 **왜 필요한가**
>
> Java에서는 컬렉션 종류(`ArrayList` vs `LinkedList`)가 다르면 `equals`가 대체로 내용까지 비교해 주지만, 인터페이스가 다르면(`List` vs `Set`) 아예 비교 자체가 어색합니다. Scala는 "컬렉션이 속한 큰 분류(세트/맵/시퀀스)만 같으면, 구체적인 구현 타입은 신경 쓰지 말고 내용물로 비교한다"는 일관된 규칙을 세워 이 혼란을 없앴습니다.

#### 2.1 규칙 1 — 카테고리가 다르면 무조건 다르다

Scala 컬렉션은 크게 세 카테고리로 나뉩니다.

- **Seq** (순서가 있는 시퀀스: `List`, `Vector`, `ArraySeq` ...)
- **Set** (집합: `HashSet`, `TreeSet` ...)
- **Map** (맵: `HashMap`, `TreeMap` ...)

**카테고리가 다르면 원소가 똑같아도 항상** `false`입니다.

```scala
Set(1, 2, 3) == List(1, 2, 3)   // false — Set과 Seq는 카테고리가 다름
List(1, 2, 3) == Seq(1, 2, 3)   // true  — 둘 다 Seq 카테고리
```

#### 2.2 규칙 2 — 같은 카테고리면 구현이 아니라 내용물로 비교한다

같은 카테고리 안에서는 **구체적인 클래스가 달라도 원소만 같으면 동등**합니다. Seq는 순서까지 같아야 하고, Set/Map은 순서와 무관하게 원소 집합만 같으면 됩니다.

```scala
List(1, 2, 3) == Vector(1, 2, 3)       // true  — 둘 다 Seq, 순서도 같음
List(1, 2, 3) == List(3, 2, 1)         // false — Seq는 순서까지 비교
HashSet(1, 2) == TreeSet(2, 1)         // true  — Set은 순서 무관
```

> ⚠️ **짚고 넘어가기**
>
> "구현이 다르면 안 같다"고 생각하기 쉽지만 정반대입니다. `List`와 `Vector`처럼 내부 구조가 완전히 다른 두 타입도 카테고리(Seq)와 내용(원소, 순서)만 맞으면 서로 `==`로 비교했을 때 `true`가 나옵니다. 배열(`Array`)만은 예외로, Scala 2.13 기준 `Array`의 `==`는 참조 동등성(reference equality)을 그대로 물려받으므로 원소가 같아도 다른 배열 인스턴스면 `false`입니다. 배열 내용 비교에는 `sameElements`를 쓰거나 `.toSeq`로 감싸 비교해야 합니다.

#### 2.3 가변 컬렉션과 동등성 — 해시 키로 쓸 때의 함정

가변(mutable) 컬렉션은 **"지금 이 순간 담고 있는 원소"** 기준으로 동등성이 결정됩니다. 즉 컬렉션 내용이 바뀌면 `==` 결과와 해시코드(`hashCode`)도 함께 바뀝니다. 이 때문에 가변 컬렉션을 `HashMap`의 키로 쓰면 다음과 같은 사고가 납니다.

```scala
import scala.collection.mutable.{ArrayBuffer, HashMap}

val buf = ArrayBuffer(1, 2, 3)
val map = HashMap(buf -> "value")

map(buf)          // "value" — 아직은 정상 조회

buf(0) += 1       // buf의 내용이 바뀜 → hashCode도 바뀜
map(buf)          // NoSuchElementException! 버킷 위치가 어긋나 못 찾음
```

`HashMap`은 키를 넣을 때의 `hashCode`로 버킷 위치를 정해 두는데, 이후 그 키(가변 컬렉션)의 내용이 바뀌면 `hashCode`도 달라져 **원래 저장했던 버킷과 지금 계산되는 버킷이 어긋납니다.** 데이터는 그대로 맵 안에 있지만 찾을 수 없게 되는 것입니다.

> **실전 원칙:** 해시 기반 컬렉션(`HashMap`, `HashSet`)의 키·원소로는 **불변 컬렉션**만 쓰세요. 가변 컬렉션을 키로 써야 한다면 넣은 뒤 다시는 그 내용을 바꾸지 않는다는 확신이 있을 때만 허용합니다.

---

### 3. 성능 특성 표기법

공식 문서는 각 연산의 시간 복잡도를 다음 다섯 기호로 요약합니다.

| 기호 | 의미 | 설명 |
|---|---|---|
| **C** | 상수 시간(Constant) | 컬렉션 크기와 무관하게 항상 빠름 |
| **eC** | 사실상 상수 시간(effectively Constant) | 이론적으로는 조건이 있지만(예: 벡터 길이가 현실적 한계 안일 때) 실질적으로 상수로 취급 가능 |
| **aC** | 상환 상수 시간(amortized Constant) | 개별 호출은 느릴 수 있어도, 여러 번 호출한 평균은 상수 시간 (예: 배열이 꽉 차면 가끔 통째로 재할당하지만 자주 있는 일은 아님) |
| **Log** | 로그 시간 | 컬렉션 크기의 로그에 비례 (균형 트리 등) |
| **L** | 선형 시간(Linear) | 컬렉션 크기에 비례 — 커질수록 느려짐 |
| **–** | 미지원 | 해당 연산 자체를 제공하지 않음 |

> 📘 **처음 배우는 분께**
>
> "eC"와 "aC"를 헷갈리기 쉽습니다. **eC**(사실상 상수)는 "이론적 최악의 경우엔 로그 시간이지만 실제 크기 범위에서는 상수와 다름없다"는 뜻(`Vector`가 대표적)이고, **aC**(상환 상수)는 "가끔 한 번 비싼 연산(배열 재할당 등)이 끼어도, 여러 번 호출을 평균 내면 상수"라는 뜻(`ArrayBuffer.append`가 대표적)입니다. 전자는 "구조가 원래 빠르다", 후자는 "가끔 비싸지만 평균은 싸다"는 차이입니다.

---

### 4. 순차 컬렉션(Sequence)의 성능

시퀀스는 `head`(첫 원소), `tail`(첫 원소 제외 나머지), `apply`(인덱스 접근), `update`(특정 위치 변경), `prepend`(앞에 추가), `append`(뒤에 추가) 여섯 연산 기준으로 비교합니다.

#### 4.1 불변 시퀀스

| 타입 | head | tail | apply | update | prepend | append |
|---|---|---|---|---|---|---|
| `List` | C | C | L | L | C | L |
| `LazyList` | C | C | L | L | C | L |
| `Vector` | eC | eC | eC | eC | eC | eC |
| `ArraySeq` | C | L | C | L | L | L |
| `Range` | C | C | C | – | – | – |
| `Queue` | aC | aC | L | L | C | C |
| `String` | C | L | C | L | L | L |

- **`List`**: "머리와 꼬리로 이루어진 연결 리스트"이므로 앞쪽 연산(`head`, `tail`, `prepend`)은 전부 상수 시간이지만, 인덱스로 임의 위치에 접근(`apply`)하거나 끝에 추가(`append`)하려면 리스트를 처음부터 끝까지 훑어야 해 선형 시간입니다.
- **`Vector`**: 내부적으로 갈래가 32개인 트리 구조라서 이론적으로는 `Log`지만, 트리 깊이가 실전 데이터 크기에서 사실상 상수(트리 깊이 ≤ 6~7 정도)라 모든 주요 연산이 `eC`로 균형 잡혀 있습니다. "이것도 저것도 적당히 빠른" 범용 시퀀스가 필요할 때의 기본 선택지입니다.
- **`ArraySeq`(불변)**: 배열 하나를 그대로 감싸므로 인덱스 접근(`apply`)은 `C`지만, 불변이라 원소 하나를 바꾸려면(`update`) 배열 전체를 복사해야 해 `L`입니다.
- **`Range`**: `start`, `end`, `step` 세 숫자만 들고 있는 컬렉션이라 `head`/`apply`가 계산만으로 끝나 `C`입니다. 대신 원소를 하나씩 끼워 넣거나 값을 바꾼다는 개념 자체가 없어 `update`/`prepend`/`append`는 아예 지원하지 않습니다(`–`).
- **`Queue`**: 앞쪽 리스트와 뒤집힌 뒤쪽 리스트 두 개로 구현되어 `prepend`/`append` 모두 어느 한쪽 리스트에 얹기만 하면 되어 `C`지만, `head`/`tail`은 뒤쪽 리스트를 뒤집어야 할 때가 있어 `aC`(상환 상수)입니다.

```scala
// List: 앞에서부터 처리하는 재귀 알고리즘에 적합
def sumList(xs: List[Int]): Int =
  if xs.isEmpty then 0 else xs.head + sumList(xs.tail)   // head/tail 모두 C

// Vector: 인덱스 접근이 잦은 코드에 적합
val v = Vector(1, 2, 3, 4, 5)
v(2)          // eC — List였다면 L
v.updated(2, 99)   // eC — List.updated는 L
```

#### 4.2 가변 시퀀스

| 타입 | head | tail | apply | update | prepend | append |
|---|---|---|---|---|---|---|
| `ArrayBuffer` | C | L | C | C | L | aC |
| `ListBuffer` | C | L | L | L | C | C |
| `Array` | C | L | C | C | – | – |
| `StringBuilder` | C | L | C | C | L | aC |

- **`ArrayBuffer`**: 내부가 배열이라 인덱스 접근·변경(`apply`, `update`)이 `C`이고, 뒤에 추가(`append`)는 배열이 꽉 찼을 때만 가끔 재할당하므로 `aC`입니다. 반면 앞에 추가(`prepend`)는 기존 원소를 전부 한 칸씩 밀어야 해 `L`입니다. **뒤에서만 추가/삭제하는 스택·버퍼 용도**에 최적입니다.
- **`ListBuffer`**: `List`를 뒤에서부터 빠르게 만들기 위한 가변 버퍼로, 양 끝(`prepend`, `append`)이 모두 `C`입니다. 대신 인덱스 접근(`apply`)은 여전히 리스트 순회가 필요해 `L`입니다.

```scala
// ArrayBuffer: 뒤쪽 추가 + 인덱스 접근이 잦을 때
val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf += 1; buf += 2; buf += 3   // append aC
buf(1)                          // apply C
```

---

### 5. 집합/맵(Set/Map)의 성능

Set/Map은 `lookup`(조회), `add`(추가), `remove`(삭제), `min`(최솟값) 네 연산으로 비교합니다.

| 타입 | lookup | add | remove | min |
|---|---|---|---|---|
| 불변 `HashSet` / `HashMap` | eC | eC | eC | L |
| 불변 `TreeSet` / `TreeMap` | Log | Log | Log | Log |
| 불변 `BitSet` | C | L | L | eC |
| 불변 `VectorMap` | eC | eC | aC | L |
| 불변 `ListMap` | L | L | L | L |
| 가변 `HashSet` / `HashMap` | eC | eC | eC | L |
| 가변 `TreeSet` | Log | Log | Log | Log |
| 가변 `BitSet` | C | aC | C | eC |

- **해시 기반(`HashSet`/`HashMap`)**: 해시코드로 버킷을 바로 찾아가므로 조회·추가·삭제가 (가변·불변 모두) 전부 사실상 상수 시간(`eC`)입니다. 단, 최솟값(`min`)은 정렬 정보를 따로 유지하지 않으므로 전체를 훑어야 해 `L`입니다. **순서가 중요하지 않은 대다수 상황의 기본 선택지**입니다.
- **트리 기반(`TreeSet`/`TreeMap`)**: 균형 이진 트리(레드-블랙 트리)로 구현되어 모든 연산이 `Log`로 고르지만, 그 대가로 **원소가 항상 정렬된 순서를 유지**합니다. 정렬된 순회나 범위 조회(`range`, `from`, `until`)가 필요할 때 선택합니다.
- **`BitSet`**: 정수 집합을 비트 배열로 표현하므로, 값의 범위가 촘촘하게 몰려 있을 때(dense) 조회(`C`)와 최솟값 조회(`eC`)가 빠릅니다. 추가(`add`)는 불변은 새 비트 배열을 만들어야 해 `L`, 가변은 배열을 가끔만 늘리면 되어 `aC`입니다. 희소(sparse)하고 값의 범위가 아주 넓으면 이 장점이 사라지므로 주의가 필요합니다.
- **`VectorMap`/`ListMap`**: `VectorMap`은 삽입 순서를 기억하면서도 조회·추가는 해시 기반이라 `eC`를 유지합니다. `ListMap`은 내부가 연결 리스트라 대부분의 연산이 `L`이므로, 삽입 순서 보존이 꼭 필요한 게 아니라면 `VectorMap`이 더 나은 선택입니다.

```scala
import scala.collection.immutable.{HashSet, TreeSet}

val hs = HashSet(3, 1, 2)
hs.contains(2)     // eC — 순서 없는 빠른 조회

val ts = TreeSet(3, 1, 2)
ts.min             // Log — 정렬 구조 덕분에 순서 관련 연산이 안정적
ts.range(1, 3)     // 정렬 순회가 필요하면 TreeSet
```

---

### 6. 실전 선택 가이드

| 상황 | 추천 컬렉션 | 이유 |
|---|---|---|
| 앞에서부터 재귀적으로 처리(패턴 매칭, `head`/`tail`) | `List` | 앞쪽 연산이 전부 `C` |
| 인덱스 접근·갱신이 잦은 불변 컬렉션 | `Vector` | 모든 연산이 균형 있게 `eC` |
| 뒤에서만 추가/삭제하는 누적 버퍼(가변) | `ArrayBuffer` | append `aC`, 인덱스 `C` |
| 해시맵/해시셋 키로 쓸 값 | **불변** 컬렉션 | 가변 컬렉션은 내용 변경 시 해시코드가 바뀌어 조회 실패 위험 |
| 순서 없이 빠른 존재 확인만 필요 | `HashSet`/`HashMap` | lookup `eC` |
| 정렬된 순서 유지·범위 조회 필요 | `TreeSet`/`TreeMap` | 모든 연산 `Log`이지만 정렬 보장 |
| 촘촘한 정수 집합 | `BitSet` | lookup `C`, min `eC` |

핵심은 두 가지로 요약됩니다.

1. **동등성**: 카테고리(Seq/Set/Map)가 같고 내용이 같으면 구현 타입과 무관하게 `==`이다. 단, 가변 컬렉션은 내용이 바뀌면 동등성과 해시코드도 함께 바뀌므로 해시 키로 쓰지 않는다.
2. **성능**: "어떤 연산을 자주 하는가"가 컬렉션 선택을 결정한다. 앞쪽 연산 위주면 `List`, 인덱스 접근 위주면 `Vector`, 뒤쪽 누적이면 `ArrayBuffer`, 정렬 유지가 필요하면 `TreeMap`/`TreeSet`.

---

### 참고 자료

- [Equality (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/equality.html)
- [Performance Characteristics (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html)
