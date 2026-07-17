# 컬렉션 변환 연산: map, filter, flatMap, collect

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html , https://docs.scala-lang.org/overviews/collections-2.13/seqs.html

---

## 목차

1. [개요: 왜 이 네 개를 묶어서 보는가](#1-개요-왜-이-네-개를-묶어서-보는가)
2. [`map` — 하나를 하나로 바꾼다](#2-map--하나를-하나로-바꾼다)
3. [`filter` — 조건에 맞는 것만 남긴다](#3-filter--조건에-맞는-것만-남긴다)
4. [`flatMap` — 바꾸고 나서 한 겹 펼친다](#4-flatmap--바꾸고-나서-한-겹-펼친다)
5. [`collect` — 걸러내면서 동시에 바꾼다](#5-collect--걸러내면서-동시에-바꾼다)
6. [네 연산 비교표](#6-네-연산-비교표)
7. [컬렉션 종류에 따른 결과 타입](#7-컬렉션-종류에-따른-결과-타입)
8. [참고 자료](#8-참고-자료)

---

## 1. 개요: 왜 이 네 개를 묶어서 보는가

Scala의 모든 컬렉션은 `Iterable` 트레이트를 최상위로 두고, 이 트레이트가 원소를 하나씩 꺼내 처리하는 공통 연산들을 정의합니다. `map`, `filter`, `flatMap`, `collect`는 그중에서도 **"컬렉션 하나를 받아 새 컬렉션 하나를 만들어 내는"** 대표 연산입니다.

> 💡 **왜 필요한가 — 반복문 대신 이 연산들을 쓰는 이유**
>
> `for` 루프로 빈 리스트를 만들고 하나씩 채워 넣는 방식은 "무엇을 할지"보다 "어떻게 반복할지"에 코드가 집중됩니다. `map`/`filter`/`flatMap`/`collect`는 반복의 기계적인 부분(순회, 누적)을 감춰 주고, **어떤 변환을 적용할지**만 표현하게 해 줍니다. 그래서 함수형 스타일 코드에서는 `for` 루프보다 이 연산들이 훨씬 자주 등장합니다.

네 연산 모두 인자로 **함수**를 받고, 원본 컬렉션은 그대로 둔 채 **새 컬렉션을 반환**합니다(불변성 유지). 차이는 "함수가 무엇을 반환하는가"와 "원소 개수가 어떻게 바뀌는가"에 있습니다.

```scala
val xs = List(1, 2, 3, 4, 5)

xs.map(_ * 2)              // List(2, 4, 6, 8, 10)      — 개수 그대로, 값만 변환
xs.filter(_ % 2 == 0)      // List(2, 4)                — 개수 줄어듦, 값은 그대로
xs.flatMap(n => List(n, -n))  // List(1, -1, 2, -2, ...) — map 후 한 겹 펼침
xs.collect { case n if n % 2 == 0 => n * 10 }  // List(20, 40) — filter + map 동시에
```

---

## 2. `map` — 하나를 하나로 바꾼다

`map(f)`는 컬렉션의 **모든 원소 각각에 함수 `f`를 적용**해서, 같은 개수의 원소를 가진 새 컬렉션을 만듭니다. 원문의 정의를 그대로 옮기면 "함수 `f`를 `xs`의 모든 원소에 적용해서 얻은 컬렉션"입니다.

```scala
val names = List("alice", "bob", "carol")
names.map(_.capitalize)      // List("Alice", "Bob", "Carol")
names.map(_.length)          // List(5, 3, 5)
```

- 입력 원소 개수와 출력 원소 개수가 **항상 같습니다**.
- `f`의 반환 타입이 바뀌면 컬렉션의 타입 매개변수도 함께 바뀝니다(`List[String]` → `List[Int]`처럼).

> 📘 **처음 배우는 분께 — `map`은 "형태를 유지한 채 내용만 바꾼다"**
>
> `List`든 `Set`이든 `Option`이든, `map`을 적용하면 **컨테이너의 껍데기는 그대로 두고 안의 값만** 바뀝니다. `Option[Int]`에 `map`을 쓰면 `Option[String]`이 되지, 갑자기 `List`로 바뀌지는 않습니다.

---

## 3. `filter` — 조건에 맞는 것만 남긴다

`filter(p)`는 **불리언을 반환하는 조건 함수(predicate)** `p`를 받아, `p`가 `true`를 반환하는 원소만 남긴 새 컬렉션을 만듭니다.

```scala
val nums = List(1, 2, 3, 4, 5, 6)
nums.filter(_ % 2 == 0)     // List(2, 4, 6)
nums.filterNot(_ % 2 == 0)  // List(1, 3, 5)  — 조건의 반대
```

- 원소의 **값 자체는 바뀌지 않고**, 개수만 줄어들 수 있습니다(그대로 남거나 전부 사라질 수도 있음).
- `partition(p)`을 쓰면 `filter(p)`와 `filterNot(p)`를 한 번에 얻을 수 있습니다. `trait-iterable.html`이 `splitAt`, `span`, `groupBy` 등과 함께 묶어 소개하는 "분할(subdivision)" 연산군에 속합니다.

```scala
val (evens, odds) = nums.partition(_ % 2 == 0)
// evens: List(2, 4, 6), odds: List(1, 3, 5)
```

---

## 4. `flatMap` — 바꾸고 나서 한 겹 펼친다

`flatMap(f)`는 `map`과 비슷하게 각 원소에 함수를 적용하지만, 이때 `f`는 **컬렉션(또는 컬렉션처럼 취급되는 값)을 반환**합니다. `map`만 쓰면 결과가 "컬렉션의 컬렉션"(중첩 구조)이 되는데, `flatMap`은 이 중첩을 **한 겹 평평하게(flatten)** 만들어 줍니다.

```scala
val words = List("hello", "world")

words.map(_.toList)          // List(List('h','e','l','l','o'), List('w','o','r','l','d'))
words.flatMap(_.toList)      // List('h','e','l','l','o','w','o','r','l','d')  — 한 겹 펼침
```

즉 `xs.flatMap(f)`는 개념적으로 `xs.map(f).flatten`과 같습니다.

> 💡 **왜 필요한가 — `Option`을 다룰 때 `flatMap`이 특히 유용한 이유**
>
> `Option`도 컬렉션처럼 취급되므로(원소가 0개 또는 1개인 컬렉션), 실패할 수 있는 연산을 연쇄할 때 `flatMap`이 자주 쓰입니다.
>
> ```scala
> def parse(s: String): Option[Int] = s.toIntOption
>
> val strs = List("1", "x", "3")
> strs.flatMap(parse)   // List(1, 3)  — 파싱 실패(None)는 자동으로 사라짐
> ```
>
> `map`을 썼다면 `List(Some(1), None, Some(3))`처럼 `Option`이 그대로 남아 다루기 번거로웠을 것입니다.

`for` 컴프리헨션(`for ... yield`)은 내부적으로 `map`/`flatMap`/`filter`의 조합으로 디슈가링(desugaring)되므로, 이 세 연산의 동작을 알아 두면 `for` 문법이 실제로 무엇을 하는지 이해하기 쉬워집니다.

---

## 5. `collect` — 걸러내면서 동시에 바꾼다

`collect(f)`는 인자로 **부분 함수**(`PartialFunction`, 모든 입력이 아니라 일부 입력에 대해서만 정의된 함수)를 받습니다. 각 원소에 대해 `f`가 정의되어 있는(`isDefinedAt`이 참인) 경우에만 `f`를 적용한 결과를 모으고, 정의되지 않은 원소는 건너뜁니다.

```scala
val xs = List(1, "two", 3, "four", 5)

xs.collect { case n: Int => n * 10 }
// List(10, 30, 50)  — Int인 것만 골라 10을 곱함
```

이는 사실상 **"패턴이 맞는 것만 골라(filter) + 동시에 변환(map)"** 을 한 번에 처리하는 것과 같습니다. 즉 `xs.collect(pf)`는 `xs.filter(pf.isDefinedAt).map(pf)`를 한 단계로 압축한 표현입니다.

```scala
sealed trait Shape
case class Circle(r: Double) extends Shape
case class Square(s: Double) extends Shape

val shapes = List(Circle(2.0), Square(3.0), Circle(1.5))

shapes.collect { case Circle(r) => r }   // List(2.0, 1.5) — Circle만 골라 반지름 추출
```

> ⚠️ **짚고 넘어가기 — `map` 안에서 `match`를 쓰는 것과의 차이**
>
> `xs.map { case Circle(r) => r }`처럼 쓰면, `Square`처럼 매치되지 않는 원소를 만났을 때 `MatchError`가 발생하며 런타임에 예외로 터집니다. 반면 `collect`는 애초에 정의되지 않은 원소를 **조용히 건너뛰므로** 예외 없이 안전합니다. "일부 케이스만 처리하고 나머지는 버리고 싶다"면 `map` + `match`가 아니라 `collect`를 써야 합니다.

---

## 6. 네 연산 비교표

| 연산 | 인자 | 원소 개수 변화 | 한 줄 요약 |
|---|---|---|---|
| `map(f)` | `A => B` | 그대로 | 모든 원소를 하나씩 변환 |
| `filter(p)` | `A => Boolean` | 같거나 줄어듦 | 조건에 맞는 원소만 남김 |
| `flatMap(f)` | `A => Iterable[B]` | 늘어나거나 줄어들 수 있음 | 변환 후 중첩을 한 겹 펼침 |
| `collect(pf)` | `PartialFunction[A, B]` | 같거나 줄어듦 | 조건에 맞는 원소만 걸러 동시에 변환 |

핵심 감별법: **함수가 무엇을 반환하는가**로 구분하면 됩니다.

- 값 하나 → `map`
- `Boolean` → `filter`
- 컬렉션 → `flatMap`
- 일부 입력에만 정의된 함수(보통 `{ case ... }`) → `collect`

---

## 7. 컬렉션 종류에 따른 결과 타입

`Iterable` 계열의 이 연산들은 **원본과 같은 종류의 컬렉션**을 결과로 돌려줍니다. 예를 들어 `List`에 `map`을 적용하면 `List`가, `Vector`에 적용하면 `Vector`가 나옵니다.

```scala
List(1, 2, 3).map(_ * 2)    // List(2, 4, 6)
Vector(1, 2, 3).map(_ * 2)  // Vector(2, 4, 6)
Set(1, 2, 3).map(_ * 2)     // Set(2, 4, 6)
"abc".map(_.toUpper)        // "ABC" (String도 컬렉션처럼 동작)
```

다만 `Set`처럼 **중복을 허용하지 않는** 컬렉션에서는 `map` 이후 원소 개수가 줄어들 수 있습니다(서로 다른 원소가 같은 값으로 매핑되는 경우).

```scala
Set(1, 2, 3).map(_ % 2)   // Set(1, 0)  — 3개였지만 결과는 2개
```

또한 `seqs.html`이 설명하는 `Seq`(순서와 인덱스가 있는 컬렉션)에서는 `map`/`filter`/`flatMap`/`collect`의 결과도 **원소 순서가 보존된 시퀀스**로 나옵니다. `updated`(특정 인덱스의 값만 바꿔 새 시퀀스를 반환)나 `patch`(구간을 통째로 교체)처럼 인덱스 기반으로 값을 바꾸는 연산과는 구분됩니다. `updated`는 위치 하나를 지정해 바꾸는 것이고, `map`은 조건 없이 전체 원소에 함수를 적용하는 것이라는 차이를 기억해 두면 됩니다.

---

## 8. 참고 자료

- [The Architecture of Scala 2.13 Collections — Iterable](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [The Architecture of Scala 2.13 Collections — Seqs](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
- `00_prerequisites_scala_basics.md` — `case class`/패턴 매칭에 대한 선행 지식 (5번 항목)
