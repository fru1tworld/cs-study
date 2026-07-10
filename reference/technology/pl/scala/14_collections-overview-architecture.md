# 컬렉션 개요와 아키텍처 (Iterable 트레이트)

---

## 목차

1. [컬렉션 프레임워크가 추구하는 것](#1-컬렉션-프레임워크가-추구하는-것)
2. [불변 vs 가변, 패키지 구조](#2-불변-vs-가변-패키지-구조)
3. [컬렉션 계층 구조 한눈에 보기](#3-컬렉션-계층-구조-한눈에-보기)
4. [모든 컬렉션의 뿌리: `Iterable` 트레이트](#4-모든-컬렉션의-뿌리-iterable-트레이트)
5. [`Iterable`이 제공하는 주요 연산](#5-iterable이-제공하는-주요-연산)
6. [내부 아키텍처: 왜 `map`은 항상 "같은 종류"를 돌려주는가](#6-내부-아키텍처-왜-map은-항상-같은-종류를-돌려주는가)
7. [정리](#7-정리)

---

## 1. 컬렉션 프레임워크가 추구하는 것

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/introduction.html

Scala 표준 라이브러리의 컬렉션 프레임워크는 리스트, 집합, 맵 등 서로 다른 자료구조를
**하나의 일관된 어휘**로 다룰 수 있게 설계되었습니다. 공식 문서는 이 설계가 지향하는 가치를
여섯 가지로 정리합니다.

| 가치 | 의미 |
|---|---|
| **사용 편의성(Ease of use)** | 20~50개 정도의 메서드 어휘만으로 대부분의 문제를 해결. 부수효과가 없어 "컬렉션을 실수로 망가뜨릴까" 걱정할 필요가 없음 |
| **간결성(Conciseness)** | 여러 줄의 반복문으로 하던 일을 함수형 문법 한 줄로 표현 |
| **안전성(Safety)** | 정적 타입 + 함수형 스타일 덕분에 오류 대부분을 컴파일 시점에 잡아냄 |
| **성능(Performance)** | 잘 튜닝된 라이브러리 구현을 재사용하므로, 직접 짠 코드보다 크게 못하지 않음 |
| **병렬성(Parallelism)** | `.par`를 호출하면 여러 코어에서 병렬 실행 — 순차 컬렉션과 API가 동일 |
| **범용성(Universality)** | `String`, `Array`처럼 컬렉션이 아닌 것처럼 보이는 타입도 동일한 연산을 지원 |

이 방향성을 보여주는 대표 예시가 `partition`입니다.

```scala
val people: List[Person] = ???
val (minors, adults) = people.partition(_.age < 18)
// minors, adults 모두 List[Person] — 원본과 같은 종류의 컬렉션으로 되돌아옴
```

반복문 없이 한 줄로 분류가 끝나고, `List`가 아니라 `Vector`나 `Set`으로 바꿔도
똑같은 코드가 그대로 동작합니다. "어떤 컬렉션이든 같은 방식으로 다룰 수 있다"는 것이
이 프레임워크 전체를 관통하는 주제입니다.

---

## 2. 불변 vs 가변, 패키지 구조

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/overview.html

Scala 컬렉션은 **불변(immutable)** 과 **가변(mutable)** 두 갈래로 명확히 나뉩니다.

- **불변 컬렉션**: 한 번 만들어지면 절대 바뀌지 않습니다. "추가/삭제"처럼 보이는 연산도
  사실은 **새 컬렉션을 만들어 반환**하는 것입니다.
- **가변 컬렉션**: 제자리(in place)에서 원소를 갱신·추가·삭제할 수 있습니다.

> 📘 **처음 배우는 분께 — 왜 기본값이 불변인가**
>
> Scala는 특별한 이유가 없다면 **불변 컬렉션을 기본으로** 사용하도록 설계되었습니다.
> 여러 스레드가 같은 컬렉션을 동시에 들여다봐도 안전하고, "누가 몰래 내용을 바꿨는지"
> 추적할 필요가 없기 때문입니다. 가변 컬렉션이 필요하면 반드시 명시적으로 가져와야 합니다.

패키지도 이 구분을 그대로 반영합니다.

| 패키지 | 역할 |
|---|---|
| `scala.collection` | 불변/가변 구현이 함께 상속하는 추상 루트 트레이트 |
| `scala.collection.immutable` | 불변임이 보장된 구현체 (기본으로 쓰이는 것들) |
| `scala.collection.mutable` | 가변 구현체 (사용하려면 명시적으로 import) |

```scala
import scala.collection.mutable

val fixed = Set(1, 2, 3)          // scala.collection.immutable.Set (기본)
val grows = mutable.Set(1, 2, 3)  // 명시적으로 가변 버전을 가져옴
grows += 4                        // 제자리에서 변경 가능
```

자주 쓰는 타입(`List`, `Iterable`, `Seq`, `IndexedSeq`, `Vector`, `Range`, `LazyList`,
`Iterator`, `StringBuilder`)은 `scala` 최상위 패키지에도 별칭(alias)이 걸려 있어서,
매번 전체 경로를 안 써도 됩니다.

---

## 3. 컬렉션 계층 구조 한눈에 보기

컬렉션 트레이트는 크게 네 갈래로 나뉘고, 이 문서의 주인공인 `Iterable`이 그 맨 위에 있습니다.

```
Iterable                     ← 모든 컬렉션의 공통 조상
 ├── Seq                     ← 순서가 있는 컬렉션
 │    ├── IndexedSeq         ← 인덱스로 빠르게 접근 (Vector, Array 등)
 │    └── LinearSeq          ← 머리/꼬리로 순회 (List 등)
 ├── Set                     ← 순서 없음, 중복 없음
 └── Map                     ← 키-값 쌍
```

- `Buffer`는 `Seq` 계열이지만 **가변 전용**으로만 존재합니다(예: `ArrayBuffer`, `ListBuffer`).
- `Seq`, `Set`, `Map` 각각은 `Iterable`이 정의한 걸 물려받으면서, 자기만의 `apply`를 추가합니다.

| 트레이트 | `apply`의 의미 |
|---|---|
| `Seq` | 인덱스로 접근 — `seq(2)`는 "세 번째 원소" |
| `Set` | 포함 여부 검사 — `set(x)`는 "x가 들어 있는가"(Boolean) |
| `Map` | 키 조회 — `map(k)`는 "키 k에 대응하는 값" |

**핵심 원칙 — 균일 반환 타입(uniform return type):** `List`에 `map`을 걸면 결과도
`List`, `Set`에 걸면 결과도 `Set`입니다. 이 원칙이 왜 성립하는지는 6절에서 다룹니다.

```scala
List(1, 2, 3).map(_ * 2)   // List(2, 4, 6)   → 여전히 List
Set(1, 2, 3).map(_ * 2)    // Set(2, 4, 6)    → 여전히 Set
```

컬렉션 생성 문법도 종류에 상관없이 동일합니다.

```scala
List(1, 2, 3)
Set(1, 2, 3)
Map("x" -> 24, "y" -> 25)
```

---

## 4. 모든 컬렉션의 뿌리: `Iterable` 트레이트

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html

`Iterable`은 컬렉션 계층의 최상단에 있는 트레이트로, 딱 하나의 추상 메서드만 요구합니다.

```scala
trait Iterable[A] {
  def iterator: Iterator[A]   // 이것 하나만 있으면 나머지는 전부 공짜로 따라옴
  // map, filter, foreach, fold, take, drop ... 수십 개가 이 위에서 구현됨
}
```

> 💡 **왜 하나의 메서드로 충분한가**
>
> `iterator`는 "원소를 하나씩 꺼내는 방법"만 알려줍니다. `Iterable` 트레이트는 이 방법
> 하나만 있으면 `map`, `filter`, `fold`, `take`, `grouped` 같은 수십 개 메서드를
> **공통 구현으로** 만들어 제공합니다. 새 컬렉션 타입을 만드는 사람 입장에서는
> `iterator`만 구현하면 나머지 API 전체를 자동으로 얻는 셈입니다.

`Iterable`은 한 단계 더 기본적인 `IterableOnce[A]`를 상속합니다. `IterableOnce`는
"단 한 번만 순회할 수 있어도 됨"을 뜻하는 최소 계약이고, `Iterable`은 여기에
"몇 번이든 다시 순회할 수 있음"(`iterator`를 다시 호출하면 처음부터 다시 순회)이라는
보장을 얹은 것입니다.

```scala
def sumAll(xs: IterableOnce[Int]): Int =
  xs.iterator.sum   // Iterator든 List든, 한 번 순회 가능한 것이면 다 받음
```

`Seq`, `Set`, `Map` 모두 `Iterable`의 자식이므로, `Iterable`이 제공하는 메서드는
어떤 컬렉션에서도 그대로 쓸 수 있습니다.

---

## 5. `Iterable`이 제공하는 주요 연산

`Iterable`의 메서드는 성격별로 다음과 같이 묶어볼 수 있습니다.

| 분류 | 대표 메서드 | 설명 |
|---|---|---|
| 분할/윈도우 | `grouped(n)`, `sliding(n)` | 아래 예시 참고 |
| 변환 | `map`, `flatMap`, `collect` | 원소마다 함수를 적용해 새 컬렉션 생성 |
| 부분 선택 | `take`, `drop`, `takeWhile`, `dropWhile`, `filter`, `filterNot` | 개수/조건으로 일부만 골라냄 |
| 집계 | `foldLeft`, `foldRight`, `sum`, `product`, `min`, `max` | 원소들을 하나의 값으로 합침 |
| 변환(타입) | `toList`, `toSet`, `toArray`, `toMap` | 다른 컬렉션 종류로 바꿔치기 |

`grouped`와 `sliding`은 이름이 비슷해 헷갈리기 쉬운데, 차이는 **겹치는지 여부**입니다.

```scala
val xs = List(1, 2, 3, 4, 5)

xs.grouped(3).toList   // List(List(1,2,3), List(4,5))       — 겹치지 않고 잘라냄
xs.sliding(3).toList   // List(List(1,2,3), List(2,3,4), List(3,4,5)) — 한 칸씩 밀며 겹쳐서
```

`takeWhile`/`dropWhile`은 "조건이 깨지는 순간까지"를 기준으로 자릅니다.

```scala
List(1, 2, 3, 10, 4).takeWhile(_ < 5)   // List(1, 2, 3)   — 10에서 멈춤 (뒤에 4가 있어도 무시)
List(1, 2, 3, 10, 4).dropWhile(_ < 5)   // List(10, 4)     — 앞부분을 버림
```

> ⚠️ **짚고 넘어가기 — `takeWhile`은 `filter`가 아니다**
>
> `filter(_ < 5)`는 조건을 만족하는 원소를 전부 골라내지만, `takeWhile(_ < 5)`는
> **조건이 처음 깨지는 지점에서 즉시 멈춥니다.** 위 예시에서 `4`는 조건을 만족해도
> `10` 뒤에 있다는 이유만으로 결과에서 빠집니다.

---

## 6. 내부 아키텍처: 왜 `map`은 항상 "같은 종류"를 돌려주는가

> **원문:** https://docs.scala-lang.org/overviews/core/architecture-of-scala-213-collections.html

Scala 2.13 컬렉션 재설계의 목표는 **연산 구현을 중복 없이 한 곳에 모으면서도,
컬렉션마다 딱 맞는 반환 타입을 유지**하는 것이었습니다. 이를 위해 내부적으로
템플릿 트레이트(template trait)를 타입 매개변수 3개로 추상화합니다.

| 타입 매개변수 | 의미 |
|---|---|
| `A` | 원소 타입 |
| `CC[_]` | "어떤 컬렉션 종류인가"를 나타내는 타입 생성자 (`List`, `Set` 등) |
| `C` | 지금 다루는 구체적인 컬렉션 타입 (예: `List[Int]`) |

`filter`처럼 **원소 타입이 안 바뀌는** 연산은 `C`를 반환하고, `map`처럼
**원소 타입이 바뀔 수 있는** 연산은 `CC[B]`를 반환하도록 시그니처가 나뉩니다.

```scala
// IterableOps 안에서 대략 이런 모양
def filter(p: A => Boolean): C          // 원소 타입 A 그대로 → 같은 구체 타입 C
def map[B](f: A => B): CC[B]            // 원소 타입이 B로 바뀜 → CC 껍데기에 B를 담아 재생성
```

`Map`처럼 타입 매개변수가 2개(키, 값)인 컬렉션은 `MapOps`가 따로 담당하며,
`map` 오버로드가 두 벌 존재합니다.

```scala
// MapOps: 결과가 여전히 (키, 값) 쌍이면 Map으로 유지
def map[K2, V2](f: ((K, V)) => (K2, V2)): Map[K2, V2]

// IterableOps: 결과가 쌍이 아니면 그냥 Iterable로 격하
def map[B](f: ((K, V)) => B): Iterable[B]
```

```scala
val m = Map(1 -> "a", 2 -> "b")

m.map { case (k, v) => (k + 1, v) }   // Map(2 -> "a", 3 -> "b")  — 쌍 유지 → Map
m.map { case (k, v) => k + v.length } // Iterable(2, 3)           — 쌍 깨짐 → Iterable
```

컴파일러가 함수의 반환 타입을 보고 **더 구체적인 오버로드를 자동으로 선택**하기 때문에,
개발자는 별다른 타입 표기 없이도 "쌍이면 Map, 아니면 Iterable"이라는 결과를 얻습니다.

> 💡 **`CanBuildFrom`이 사라진 이유**
>
> Scala 2.12까지는 `map`의 반환 타입을 결정하기 위해 암시적 `CanBuildFrom` 매개변수를
> 매 연산마다 끌고 다녔습니다. 컴파일 속도 저하와 이해하기 어려운 타입 오류의 주범이었는데,
> 2.13에서는 위처럼 **오버로드 + 팩토리 트레이트** 조합으로 대체되어 훨씬 단순해졌습니다.

실제 생성 로직은 컬렉션 자신이 들고 있지 않고, `IterableFactory`·`MapFactory` 같은
**팩토리 트레이트**가 담당합니다. 덕분에 템플릿 트레이트는 "결과를 어떻게 만드는지"는
몰라도 되고, "무엇을 만들어야 하는지"만 알면 됩니다.

또한 기본적으로 대부분의 연산은 **즉시 평가하지 않고 `View`로 계산을 지연**시킵니다.
`groupBy`, `partition`처럼 결과를 바로 확정 지어야 하는 연산만 `StrictOptimized` 계열
트레이트가 `Builder`를 이용해 즉시(strict) 계산하도록 별도로 최적화되어 있습니다.

---

## 7. 정리

- Scala 컬렉션은 **사용성·간결성·안전성·성능·병렬성·범용성**이라는 여섯 가지 가치를
  목표로 설계되었습니다.
- 기본은 **불변**이며, 가변 컬렉션은 `scala.collection.mutable`에서 명시적으로 가져옵니다.
- `Iterable` → `Seq`/`Set`/`Map`으로 이어지는 계층에서, `Iterable`은 `iterator` 단
  하나의 추상 메서드로 `map`/`filter`/`fold` 등 방대한 공통 연산을 제공합니다.
- `map`이 항상 "같은 종류"의 컬렉션을 돌려주는 것처럼 보이는 이유는, 내부적으로
  `CC[_]`/`C` 타입 매개변수와 오버로드된 시그니처, 팩토리 트레이트가 조합되어
  동작하기 때문입니다. 이 구조 덕분에 예전 `CanBuildFrom` 방식보다 컴파일이 빠르고
  타입 오류도 이해하기 쉬워졌습니다.

---

## 참고 자료

- [Scala 2.13 Collections — Introduction](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html)
- [Scala 2.13 Collections — Overview](https://docs.scala-lang.org/overviews/collections-2.13/overview.html)
- [Scala 2.13 Collections — The Iterable Trait](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [Architecture of Scala 2.13's Collections](https://docs.scala-lang.org/overviews/core/architecture-of-scala-213-collections.html)
