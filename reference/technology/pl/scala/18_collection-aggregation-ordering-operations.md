# 컬렉션 집계와 정렬 연산 — fold, reduce, sortBy, groupBy

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html
> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/seqs.html

---

## 목차

1. [개요](#1-개요)
2. [fold — 초깃값을 가진 누적 연산](#2-fold--초깃값을-가진-누적-연산)
3. [reduce — 초깃값 없는 누적 연산](#3-reduce--초깃값-없는-누적-연산)
4. [fold와 reduce의 차이](#4-fold와-reduce의-차이)
5. [자주 쓰는 특수 집계: sum, product, min, max](#5-자주-쓰는-특수-집계-sum-product-min-max)
6. [정렬: sorted, sortWith, sortBy](#6-정렬-sorted-sortwith-sortby)
7. [그룹화: groupBy, groupMap, groupMapReduce](#7-그룹화-groupby-groupmap-groupmapreduce)
8. [partition — 조건으로 둘로 쪼개기](#8-partition--조건으로-둘로-쪼개기)
9. [정리 — 언제 무엇을 쓰나](#9-정리--언제-무엇을-쓰나)

---

## 1. 개요

Scala의 모든 컬렉션은 `Iterable` trait를 상속하며, 이 trait가 **집계(aggregation)**·**정렬(ordering)**·**그룹화(grouping)** 연산을 공통으로 제공합니다.

- **집계**: 컬렉션 전체를 순회하며 값 하나(또는 새 컬렉션)로 압축 — `fold`, `reduce`, `sum`, `product`, `min`, `max`
- **정렬**: 순서를 가진 시퀀스로 재배열 — `sorted`, `sortWith`, `sortBy`
- **그룹화**: 기준에 따라 여러 묶음으로 재분류 — `groupBy`, `groupMap`, `partition`

이 문서는 이 세 갈래를 순서대로 다룹니다.

---

## 2. fold — 초깃값을 가진 누적 연산

`fold` 계열은 **초깃값(zero)** 에서 시작해, 컬렉션의 각 원소를 하나씩 누적해 나가는 연산입니다.

| 메서드 | 방향 | 시그니처 |
|---|---|---|
| `foldLeft(z)(op)` | 왼쪽 → 오른쪽 | `(B, A) => B` |
| `foldRight(z)(op)` | 오른쪽 → 왼쪽 | `(A, B) => B` |
| `fold(z)(op)` | 방향 무관(결합법칙 전제) | `(A1, A1) => A1` |

```scala
val nums = List(1, 2, 3, 4)

nums.foldLeft(0)(_ + _)          // 10  : ((((0+1)+2)+3)+4)
nums.foldRight(0)(_ + _)         // 10  : (1+(2+(3+(4+0))))

// 누적 타입이 원소 타입과 달라도 됨 — foldLeft만 가능
nums.foldLeft("")((acc, n) => acc + n.toString)   // "1234"
```

> 📘 **처음 배우는 분께 — `fold`는 왜 방향 상관없는 특수판인가**
>
> `foldLeft`/`foldRight`는 누적값 타입(`B`)과 원소 타입(`A`)이 달라도 되지만, `fold`는 **둘 다 같은 타입 `A1`이어야** 합니다. 그 대신 컴파일러나 병렬 컬렉션이 순서를 자유롭게 바꿀 여지를 줍니다(연산이 결합법칙을 만족한다는 전제하에). 타입이 다르면 `fold`를 못 쓰고 `foldLeft`/`foldRight`를 써야 합니다.

`foldLeft`·`foldRight`에는 각각 `/:`, `:\` 라는 기호 연산자 별칭도 있었지만 가독성 문제로 Scala 2.13에서 폐지 수순을 밟았으므로, 지금은 메서드 이름을 그대로 쓰는 것이 표준입니다.

---

## 3. reduce — 초깃값 없는 누적 연산

`reduce`는 `fold`와 동작이 비슷하지만 **초깃값을 받지 않고, 컬렉션의 첫(또는 마지막) 원소를 초깃값 삼아** 누적합니다.

| 메서드 | 방향 |
|---|---|
| `reduceLeft(op)` | 왼쪽 → 오른쪽 |
| `reduceRight(op)` | 오른쪽 → 왼쪽 |
| `reduce(op)` | 방향 무관(결합법칙 전제) |

```scala
val nums = List(1, 2, 3, 4)

nums.reduceLeft(_ + _)     // 10
nums.reduceLeft(_ max _)   // 4 : 원소끼리 비교해 최댓값

List.empty[Int].reduceLeft(_ + _)   // UnsupportedOperationException!
```

> ⚠️ **짚고 넘어가기 — 빈 컬렉션에서 `reduce`는 예외를 던진다**
>
> `fold`는 초깃값이 있어 빈 컬렉션도 처리할 수 있지만(`Nil.foldLeft(0)(_ + _) == 0`), `reduce`는 초깃값이 없어 빈 컬렉션에서는 **더할 첫 원소 자체가 없으므로 예외**가 발생합니다. 빈 컬렉션 가능성이 있다면 `reduceOption`(결과를 `Option`으로 감싸 안전하게 처리)을 쓰는 것이 안전합니다.

```scala
List.empty[Int].reduceLeftOption(_ + _)   // None
List(1, 2, 3).reduceLeftOption(_ + _)     // Some(6)
```

---

## 4. fold와 reduce의 차이

| 구분 | fold | reduce |
|---|---|---|
| 초깃값 | 명시적으로 지정 | 컬렉션의 첫/마지막 원소 사용 |
| 빈 컬렉션 | 초깃값 반환(안전) | 예외 발생(위험) |
| 결과 타입 | 원소 타입과 달라도 됨(`foldLeft`/`foldRight`) | 원소 타입과 동일해야 함 |
| 용도 | 다른 타입으로 변환하며 누적(예: 리스트 → 문자열) | 같은 타입 내에서 합치기(예: 합계, 최댓값) |

```scala
// fold: List[Int] -> String (타입이 바뀜)
List(1, 2, 3).foldLeft("nums:")((acc, n) => s"$acc $n")   // "nums: 1 2 3"

// reduce: List[Int] -> Int (타입이 유지됨)
List(1, 2, 3).reduce(_ + _)   // 6
```

---

## 5. 자주 쓰는 특수 집계: sum, product, min, max

`fold`/`reduce`로 직접 구현할 수 있는 대표적인 집계들은 이미 메서드로 제공되어 더 간결하게 쓸 수 있습니다.

```scala
val nums = List(3, 1, 4, 1, 5)

nums.sum        // 14  : 내부적으로 foldLeft(0)(_ + _)에 대응
nums.product    // 60  : 내부적으로 foldLeft(1)(_ * _)에 대응
nums.min        // 1
nums.max        // 5

List.empty[Int].minOption   // None  : 빈 컬렉션에서도 안전
List.empty[Int].maxOption   // None
```

- `sum`, `product`는 `Numeric` 타입클래스가 필요합니다.
- `min`, `max`는 `Ordering` 타입클래스가 필요합니다.
- 빈 컬렉션에서 `min`/`max`는 예외, `minOption`/`maxOption`은 `None`을 반환합니다(위 `reduceOption`과 같은 이유).

---

## 6. 정렬: sorted, sortWith, sortBy

| 메서드 | 정렬 기준 |
|---|---|
| `sorted` | 원소 타입의 기본 순서(`Ordering[A]`) |
| `sortWith(lt)` | 두 원소를 비교하는 함수 `(A, A) => Boolean` |
| `sortBy(f)` | 각 원소에 `f`를 적용한 값을 기준으로 비교 |

```scala
case class Person(name: String, age: Int)
val people = List(Person("Bob", 30), Person("Alice", 25), Person("Carol", 25))

val nums = List(3, 1, 4, 1, 5)
nums.sorted                       // List(1, 1, 3, 4, 5)
nums.sortWith(_ > _)              // List(5, 4, 3, 1, 1) : 내림차순

people.sortBy(_.age)              // Alice(25), Carol(25), Bob(30)
people.sortBy(p => (p.age, p.name))  // 튜플 기준 다중 정렬: 나이 → 이름 순
```

> 💡 **왜 필요한가 — `sortBy`가 있는데 왜 `sortWith`도 있나**
>
> `sortBy(f)`는 "비교 키를 뽑아내는 함수"만 주면 되어 대부분의 경우 가장 간결합니다. 반면 `sortWith(lt)`는 "두 원소를 직접 비교하는 로직"이 필요할 때(예: 대소문자 무시 비교, 커스텀 우선순위 규칙처럼 키 하나로 뽑아내기 애매한 경우) 씁니다. 실무에서는 `sortBy`로 충분한 경우가 대다수입니다.

`Seq` 계열에서만 의미가 있으며(순서가 있어야 정렬 가능), `Set`이나 `Map`처럼 순서가 없는 컬렉션에는 존재하지 않습니다(단, `sorted`로 변환한 결과는 `Seq`가 됩니다).

가변(mutable) `IndexedSeq`에는 원본을 직접 바꾸는 인플레이스 버전도 있습니다.

```scala
import scala.collection.mutable.ArrayBuffer

val buf = ArrayBuffer(3, 1, 2)
buf.sortInPlace()        // buf 자체가 ArrayBuffer(1, 2, 3)로 바뀜
buf.sortInPlaceBy(-_)    // buf 자체가 ArrayBuffer(3, 2, 1)로 바뀜
```

---

## 7. 그룹화: groupBy, groupMap, groupMapReduce

`groupBy(f)`는 판별 함수(discriminator) `f`의 결과값을 키로 삼아 컬렉션을 `Map[K, Collection[A]]`로 나눕니다.

```scala
val words = List("apple", "banana", "avocado", "blueberry", "cherry")

words.groupBy(_.head)
// Map('a' -> List(apple, avocado), 'b' -> List(banana, blueberry), 'c' -> List(cherry))
```

그룹으로 나눈 뒤 각 원소를 변형하거나, 각 그룹을 하나의 값으로 다시 합치고 싶다면 `groupMap`, `groupMapReduce`를 씁니다. 둘 다 "`groupBy` 후 추가 연산"을 한 번에 처리해 중간 `Map`을 두 번 순회하지 않아도 되는 최적화된 형태입니다.

```scala
// groupMap(f)(g): f로 그룹 나누고, 그룹 안의 원소는 g로 변형
words.groupMap(_.head)(_.length)
// Map('a' -> List(5, 7), 'b' -> List(6, 9), 'c' -> List(6))

// groupMapReduce(f)(g)(h): 위에 더해 그룹을 h로 합쳐 값 하나로 축약
words.groupMapReduce(_.head)(_.length)(_ + _)
// Map('a' -> 12, 'b' -> 15, 'c' -> 6)   : 그룹별 길이 합계
```

> 💡 **왜 필요한가 — `groupBy(...).mapValues(...)` 대신 `groupMap`을 쓰는 이유**
>
> `words.groupBy(_.head).view.mapValues(_.map(_.length))`처럼 짜도 같은 결과가 나오지만, 그룹으로 나눈 결과를 다시 순회하며 변환하는 두 단계 작업이 됩니다. `groupMap`/`groupMapReduce`는 이를 한 번의 순회로 처리하도록 표준 라이브러리가 제공하는 축약형입니다.

---

## 8. partition — 조건으로 둘로 쪼개기

`partition(p)`는 술어(predicate) `p`를 만족하는 원소와 만족하지 않는 원소로 컬렉션을 **정확히 둘로** 나눕니다. "그룹 개수가 정확히 2개로 고정된 `groupBy`"라고 볼 수 있습니다.

```scala
val nums = List(1, 2, 3, 4, 5, 6)

val (even, odd) = nums.partition(_ % 2 == 0)
// even = List(2, 4, 6), odd = List(1, 3, 5)
```

`groupBy(_ % 2 == 0)`로도 비슷한 결과를 얻을 수 있지만, 결과가 `Map[Boolean, List[Int]]`가 되어 값을 꺼낼 때 매번 키로 조회해야 합니다. 이분법이 명확한 경우에는 튜플을 바로 반환하는 `partition`이 더 간결합니다.

---

## 9. 정리 — 언제 무엇을 쓰나

| 하고 싶은 일 | 메서드 |
|---|---|
| 컬렉션을 순회하며 값 하나로 압축, 초깃값·결과 타입 자유 | `foldLeft` / `foldRight` |
| 같은 타입끼리 합치기, 초깃값 없이 | `reduce` / `reduceLeft` |
| 빈 컬렉션일 수도 있는 reduce | `reduceOption` 계열 |
| 합계·곱·최댓값·최솟값 (표준 집계) | `sum` / `product` / `min` / `max` (+ `Option` 버전) |
| 기본 순서로 정렬 | `sorted` |
| 커스텀 비교 함수로 정렬 | `sortWith` |
| 비교 키를 뽑아 정렬 | `sortBy` |
| 기준으로 여러 그룹 나누기 | `groupBy` |
| 그룹 나누며 원소 변형/축약까지 한 번에 | `groupMap` / `groupMapReduce` |
| 조건 하나로 정확히 둘로 나누기 | `partition` |

---

## 참고 자료

- [Trait Iterable — Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [Seqs — Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
