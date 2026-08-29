# Scala 컬렉션 변환·집계·정렬 연산

## 컬렉션 변환 연산: map, filter, flatMap, collect

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html , https://docs.scala-lang.org/overviews/collections-2.13/seqs.html

---

### 목차

1. [개요: 왜 이 네 개를 묶어서 보는가](#1-개요-왜-이-네-개를-묶어서-보는가)
2. [`map` — 하나를 하나로 바꾼다](#2-map--하나를-하나로-바꾼다)
3. [`filter` — 조건에 맞는 것만 남긴다](#3-filter--조건에-맞는-것만-남긴다)
4. [`flatMap` — 바꾸고 나서 한 겹 펼친다](#4-flatmap--바꾸고-나서-한-겹-펼친다)
5. [`collect` — 걸러내면서 동시에 바꾼다](#5-collect--걸러내면서-동시에-바꾼다)
6. [네 연산 비교표](#6-네-연산-비교표)
7. [컬렉션 종류에 따른 결과 타입](#7-컬렉션-종류에-따른-결과-타입)
8. [참고 자료](#8-참고-자료)

---

### 1. 개요: 왜 이 네 개를 묶어서 보는가

- Scala의 모든 컬렉션은 `Iterable` 트레이트를 최상위로 둠
  - 이 트레이트가 원소를 하나씩 꺼내 처리하는 공통 연산들을 정의
- `map`, `filter`, `flatMap`, `collect` — "컬렉션 하나를 받아 새 컬렉션 하나를 만들어 내는" 대표 연산

왜 필요한가 — 반복문 대신 이 연산들을 쓰는 이유:
- `for` 루프로 빈 리스트를 만들고 하나씩 채워 넣는 방식 → "무엇을 할지"보다 "어떻게 반복할지"에 코드가 집중됨
- `map`/`filter`/`flatMap`/`collect` → 반복의 기계적인 부분(순회, 누적)을 감추고 어떤 변환을 적용할지만 표현
- 함수형 스타일 코드에서는 `for` 루프보다 이 연산들이 훨씬 자주 등장

네 연산 공통 특성:
- 인자로 함수를 받음
- 원본 컬렉션은 그대로 두고 새 컬렉션을 반환(불변성 유지)
- 차이는 두 가지 — 함수가 무엇을 반환하는가 · 원소 개수가 어떻게 바뀌는가

```scala
val xs = List(1, 2, 3, 4, 5)

xs.map(_ * 2)              // List(2, 4, 6, 8, 10)      — 개수 그대로, 값만 변환
xs.filter(_ % 2 == 0)      // List(2, 4)                — 개수 줄어듦, 값은 그대로
xs.flatMap(n => List(n, -n))  // List(1, -1, 2, -2, ...) — map 후 한 겹 펼침
xs.collect { case n if n % 2 == 0 => n * 10 }  // List(20, 40) — filter + map 동시에
```

---

### 2. `map` — 하나를 하나로 바꾼다

- `map(f)`: 컬렉션의 모든 원소 각각에 함수 `f`를 적용 → 같은 개수의 원소를 가진 새 컬렉션 생성
- 원문 정의: "함수 `f`를 `xs`의 모든 원소에 적용해서 얻은 컬렉션"

```scala
val names = List("alice", "bob", "carol")
names.map(_.capitalize)      // List("Alice", "Bob", "Carol")
names.map(_.length)          // List(5, 3, 5)
```

- 입력 원소 개수와 출력 원소 개수 항상 동일
- `f`의 반환 타입이 바뀌면 컬렉션의 타입 매개변수도 함께 바뀜(`List[String]` → `List[Int]`처럼)

처음 배우는 분께 — `map`은 "형태를 유지한 채 내용만 바꾼다":
- `List`든 `Set`이든 `Option`이든, `map` 적용 시 컨테이너의 껍데기는 그대로 두고 안의 값만 바뀜
- `Option[Int]`에 `map`을 쓰면 `Option[String]`이 됨 — 갑자기 `List`로 바뀌지 않음

---

### 3. `filter` — 조건에 맞는 것만 남긴다

- `filter(p)`: 불리언을 반환하는 조건 함수(predicate) `p`를 받아, `p`가 `true`를 반환하는 원소만 남긴 새 컬렉션 생성

```scala
val nums = List(1, 2, 3, 4, 5, 6)
nums.filter(_ % 2 == 0)     // List(2, 4, 6)
nums.filterNot(_ % 2 == 0)  // List(1, 3, 5)  — 조건의 반대
```

- 원소의 값 자체는 바뀌지 않고, 개수만 줄어들 수 있음(그대로 남거나 전부 사라질 수도 있음)
- `partition(p)` — `filter(p)`와 `filterNot(p)`를 한 번에 얻음
  - `trait-iterable.html`이 `splitAt`, `span`, `groupBy` 등과 함께 묶어 소개하는 "분할(subdivision)" 연산군에 속함

```scala
val (evens, odds) = nums.partition(_ % 2 == 0)
// evens: List(2, 4, 6), odds: List(1, 3, 5)
```

---

### 4. `flatMap` — 바꾸고 나서 한 겹 펼친다

- `flatMap(f)`: `map`과 비슷하게 각 원소에 함수를 적용하지만, `f`는 컬렉션(또는 컬렉션처럼 취급되는 값)을 반환
- `map`만 쓰면 결과가 "컬렉션의 컬렉션"(중첩 구조) → `flatMap`은 이 중첩을 한 겹 평평하게(flatten) 만듦

```scala
val words = List("hello", "world")

words.map(_.toList)          // List(List('h','e','l','l','o'), List('w','o','r','l','d'))
words.flatMap(_.toList)      // List('h','e','l','l','o','w','o','r','l','d')  — 한 겹 펼침
```

- 즉 `xs.flatMap(f)`는 개념적으로 `xs.map(f).flatten`과 동일

왜 필요한가 — `Option`을 다룰 때 `flatMap`이 특히 유용한 이유:
- `Option`도 컬렉션처럼 취급됨(원소가 0개 또는 1개인 컬렉션) → 실패할 수 있는 연산을 연쇄할 때 `flatMap`이 자주 쓰임

```scala
def parse(s: String): Option[Int] = s.toIntOption

val strs = List("1", "x", "3")
strs.flatMap(parse)   // List(1, 3)  — 파싱 실패(None)는 자동으로 사라짐
```

- `map`을 썼다면 `List(Some(1), None, Some(3))`처럼 `Option`이 그대로 남아 다루기 번거로움

- `for` 컴프리헨션(`for ... yield`)은 내부적으로 `map`/`flatMap`/`filter`의 조합으로 디슈가링(desugaring)됨
  - → 이 세 연산의 동작을 알아 두면 `for` 문법이 실제로 무엇을 하는지 이해하기 쉬움

---

### 5. `collect` — 걸러내면서 동시에 바꾼다

- `collect(f)`: 인자로 부분 함수(`PartialFunction`, 모든 입력이 아니라 일부 입력에 대해서만 정의된 함수)를 받음
- 각 원소에 대해 `f`가 정의되어 있는(`isDefinedAt`이 참인) 경우에만 `f`를 적용한 결과를 모으고, 정의되지 않은 원소는 건너뜀

```scala
val xs = List(1, "two", 3, "four", 5)

xs.collect { case n: Int => n * 10 }
// List(10, 30, 50)  — Int인 것만 골라 10을 곱함
```

- 사실상 "패턴이 맞는 것만 골라(filter) + 동시에 변환(map)"을 한 번에 처리
- 즉 `xs.collect(pf)`는 `xs.filter(pf.isDefinedAt).map(pf)`를 한 단계로 압축한 표현

```scala
sealed trait Shape
case class Circle(r: Double) extends Shape
case class Square(s: Double) extends Shape

val shapes = List(Circle(2.0), Square(3.0), Circle(1.5))

shapes.collect { case Circle(r) => r }   // List(2.0, 1.5) — Circle만 골라 반지름 추출
```

짚고 넘어가기 — `map` 안에서 `match`를 쓰는 것과의 차이:
- `xs.map { case Circle(r) => r }`처럼 쓰면 `Square`처럼 매치되지 않는 원소를 만났을 때 `MatchError` 발생 → 런타임 예외
- 반면 `collect`는 애초에 정의되지 않은 원소를 조용히 건너뜀 → 예외 없이 안전
- "일부 케이스만 처리하고 나머지는 버리고 싶다" → `map` + `match`가 아니라 `collect` 사용

---

### 6. 네 연산 비교표

- `map(f)`
  - 인자: `A => B`
  - 원소 개수 변화: 그대로
  - 한 줄 요약: 모든 원소를 하나씩 변환
- `filter(p)`
  - 인자: `A => Boolean`
  - 원소 개수 변화: 같거나 줄어듦
  - 한 줄 요약: 조건에 맞는 원소만 남김
- `flatMap(f)`
  - 인자: `A => Iterable[B]`
  - 원소 개수 변화: 늘어나거나 줄어들 수 있음
  - 한 줄 요약: 변환 후 중첩을 한 겹 펼침
- `collect(pf)`
  - 인자: `PartialFunction[A, B]`
  - 원소 개수 변화: 같거나 줄어듦
  - 한 줄 요약: 조건에 맞는 원소만 걸러 동시에 변환

핵심 감별법: 함수가 무엇을 반환하는가로 구분

- 값 하나 → `map`
- `Boolean` → `filter`
- 컬렉션 → `flatMap`
- 일부 입력에만 정의된 함수(보통 `{ case ... }`) → `collect`

---

### 7. 컬렉션 종류에 따른 결과 타입

- `Iterable` 계열의 이 연산들은 원본과 같은 종류의 컬렉션을 결과로 반환
  - 예: `List`에 `map` 적용 → `List` · `Vector`에 적용 → `Vector`

```scala
List(1, 2, 3).map(_ * 2)    // List(2, 4, 6)
Vector(1, 2, 3).map(_ * 2)  // Vector(2, 4, 6)
Set(1, 2, 3).map(_ * 2)     // Set(2, 4, 6)
"abc".map(_.toUpper)        // "ABC" (String도 컬렉션처럼 동작)
```

- 다만 `Set`처럼 중복을 허용하지 않는 컬렉션에서는 `map` 이후 원소 개수가 줄어들 수 있음(서로 다른 원소가 같은 값으로 매핑되는 경우)

```scala
Set(1, 2, 3).map(_ % 2)   // Set(1, 0)  — 3개였지만 결과는 2개
```

- `seqs.html`이 설명하는 `Seq`(순서와 인덱스가 있는 컬렉션)에서는 `map`/`filter`/`flatMap`/`collect`의 결과도 원소 순서가 보존된 시퀀스로 나옴
  - `updated`(특정 인덱스의 값만 바꿔 새 시퀀스를 반환)나 `patch`(구간을 통째로 교체)처럼 인덱스 기반으로 값을 바꾸는 연산과는 구분
  - `updated`는 위치 하나를 지정해 바꾸는 것 · `map`은 조건 없이 전체 원소에 함수를 적용하는 것 — 이 차이를 기억

---

### 8. 참고 자료

- [The Architecture of Scala 2.13 Collections — Iterable](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [The Architecture of Scala 2.13 Collections — Seqs](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
- `00_prerequisites_scala_basics.md` — `case class`/패턴 매칭 선행 지식(5번 항목)

---

## 컬렉션 집계와 정렬 연산 — fold, reduce, sortBy, groupBy

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html
> 원문: https://docs.scala-lang.org/overviews/collections-2.13/seqs.html

---

### 목차

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

### 1. 개요

- Scala의 모든 컬렉션은 `Iterable` trait를 상속
  - 이 trait가 집계(aggregation)·정렬(ordering)·그룹화(grouping) 연산을 공통으로 제공
- 집계: 컬렉션 전체를 순회하며 값 하나(또는 새 컬렉션)로 압축 — `fold`, `reduce`, `sum`, `product`, `min`, `max`
- 정렬: 순서를 가진 시퀀스로 재배열 — `sorted`, `sortWith`, `sortBy`
- 그룹화: 기준에 따라 여러 묶음으로 재분류 — `groupBy`, `groupMap`, `partition`

이 문서는 이 세 갈래를 순서대로 다룸.

---

### 2. fold — 초깃값을 가진 누적 연산

- `fold` 계열: 초깃값(zero)에서 시작해, 컬렉션의 각 원소를 하나씩 누적해 나가는 연산

- `foldLeft(z)(op)`
  - 방향: 왼쪽 → 오른쪽
  - 시그니처: `(B, A) => B`
- `foldRight(z)(op)`
  - 방향: 오른쪽 → 왼쪽
  - 시그니처: `(A, B) => B`
- `fold(z)(op)`
  - 방향: 무관(결합법칙 전제)
  - 시그니처: `(A1, A1) => A1`

```scala
val nums = List(1, 2, 3, 4)

nums.foldLeft(0)(_ + _)          // 10  : ((((0+1)+2)+3)+4)
nums.foldRight(0)(_ + _)         // 10  : (1+(2+(3+(4+0))))

// 누적 타입이 원소 타입과 달라도 됨 — foldLeft만 가능
nums.foldLeft("")((acc, n) => acc + n.toString)   // "1234"
```

처음 배우는 분께 — `fold`는 왜 방향 상관없는 특수판인가:
- `foldLeft`/`foldRight`는 누적값 타입(`B`)과 원소 타입(`A`)이 달라도 됨
- `fold`는 둘 다 같은 타입 `A1`이어야 함 → 대신 컴파일러나 병렬 컬렉션이 순서를 자유롭게 바꿀 여지 제공(연산이 결합법칙을 만족한다는 전제)
- 타입이 다르면 `fold`를 못 쓰고 `foldLeft`/`foldRight`를 써야 함

- `foldLeft`·`foldRight`에는 각각 `/:`, `:\`라는 기호 연산자 별칭도 있었으나 가독성 문제로 Scala 2.13에서 폐지 수순 → 지금은 메서드 이름을 그대로 쓰는 것이 표준

---

### 3. reduce — 초깃값 없는 누적 연산

- `reduce`: `fold`와 동작이 비슷하지만 초깃값을 받지 않고, 컬렉션의 첫(또는 마지막) 원소를 초깃값 삼아 누적

- `reduceLeft(op)` — 왼쪽 → 오른쪽
- `reduceRight(op)` — 오른쪽 → 왼쪽
- `reduce(op)` — 방향 무관(결합법칙 전제)

```scala
val nums = List(1, 2, 3, 4)

nums.reduceLeft(_ + _)     // 10
nums.reduceLeft(_ max _)   // 4 : 원소끼리 비교해 최댓값

List.empty[Int].reduceLeft(_ + _)   // UnsupportedOperationException!
```

짚고 넘어가기 — 빈 컬렉션에서 `reduce`는 예외를 던짐:
- `fold`는 초깃값이 있어 빈 컬렉션도 처리 가능(`Nil.foldLeft(0)(_ + _) == 0`)
- `reduce`는 초깃값이 없어 빈 컬렉션에서는 더할 첫 원소 자체가 없음 → 예외 발생
- 빈 컬렉션 가능성이 있다면 `reduceOption`(결과를 `Option`으로 감싸 안전하게 처리)을 쓰는 것이 안전

```scala
List.empty[Int].reduceLeftOption(_ + _)   // None
List(1, 2, 3).reduceLeftOption(_ + _)     // Some(6)
```

---

### 4. fold와 reduce의 차이

- 초깃값
  - fold: 명시적으로 지정
  - reduce: 컬렉션의 첫/마지막 원소 사용
- 빈 컬렉션
  - fold: 초깃값 반환(안전)
  - reduce: 예외 발생(위험)
- 결과 타입
  - fold: 원소 타입과 달라도 됨(`foldLeft`/`foldRight`)
  - reduce: 원소 타입과 동일해야 함
- 용도
  - fold: 다른 타입으로 변환하며 누적(예: 리스트 → 문자열)
  - reduce: 같은 타입 내에서 합치기(예: 합계, 최댓값)

```scala
// fold: List[Int] -> String (타입이 바뀜)
List(1, 2, 3).foldLeft("nums:")((acc, n) => s"$acc $n")   // "nums: 1 2 3"

// reduce: List[Int] -> Int (타입이 유지됨)
List(1, 2, 3).reduce(_ + _)   // 6
```

---

### 5. 자주 쓰는 특수 집계: sum, product, min, max

- `fold`/`reduce`로 직접 구현할 수 있는 대표적인 집계들은 이미 메서드로 제공되어 더 간결하게 사용 가능

```scala
val nums = List(3, 1, 4, 1, 5)

nums.sum        // 14  : 내부적으로 foldLeft(0)(_ + _)에 대응
nums.product    // 60  : 내부적으로 foldLeft(1)(_ * _)에 대응
nums.min        // 1
nums.max        // 5

List.empty[Int].minOption   // None  : 빈 컬렉션에서도 안전
List.empty[Int].maxOption   // None
```

- `sum`, `product`는 `Numeric` 타입클래스 필요
- `min`, `max`는 `Ordering` 타입클래스 필요
- 빈 컬렉션에서 `min`/`max`는 예외 · `minOption`/`maxOption`은 `None` 반환(위 `reduceOption`과 같은 이유)

---

### 6. 정렬: sorted, sortWith, sortBy

- `sorted` — 원소 타입의 기본 순서(`Ordering[A]`)
- `sortWith(lt)` — 두 원소를 비교하는 함수 `(A, A) => Boolean`
- `sortBy(f)` — 각 원소에 `f`를 적용한 값을 기준으로 비교

```scala
case class Person(name: String, age: Int)
val people = List(Person("Bob", 30), Person("Alice", 25), Person("Carol", 25))

val nums = List(3, 1, 4, 1, 5)
nums.sorted                       // List(1, 1, 3, 4, 5)
nums.sortWith(_ > _)              // List(5, 4, 3, 1, 1) : 내림차순

people.sortBy(_.age)              // Alice(25), Carol(25), Bob(30)
people.sortBy(p => (p.age, p.name))  // 튜플 기준 다중 정렬: 나이 → 이름 순
```

왜 필요한가 — `sortBy`가 있는데 왜 `sortWith`도 있나:
- `sortBy(f)`는 "비교 키를 뽑아내는 함수"만 주면 되어 대부분의 경우 가장 간결
- `sortWith(lt)`는 "두 원소를 직접 비교하는 로직"이 필요할 때(예: 대소문자 무시 비교, 커스텀 우선순위 규칙처럼 키 하나로 뽑아내기 애매한 경우) 사용
- 실무에서는 `sortBy`로 충분한 경우가 대다수

- `Seq` 계열에서만 의미 있음(순서가 있어야 정렬 가능) — `Set`이나 `Map`처럼 순서가 없는 컬렉션에는 존재하지 않음(단, `sorted`로 변환한 결과는 `Seq`가 됨)
- 가변(mutable) `IndexedSeq`에는 원본을 직접 바꾸는 인플레이스 버전도 있음

```scala
import scala.collection.mutable.ArrayBuffer

val buf = ArrayBuffer(3, 1, 2)
buf.sortInPlace()        // buf 자체가 ArrayBuffer(1, 2, 3)로 바뀜
buf.sortInPlaceBy(-_)    // buf 자체가 ArrayBuffer(3, 2, 1)로 바뀜
```

---

### 7. 그룹화: groupBy, groupMap, groupMapReduce

- `groupBy(f)`: 판별 함수(discriminator) `f`의 결과값을 키로 삼아 컬렉션을 `Map[K, Collection[A]]`로 나눔

```scala
val words = List("apple", "banana", "avocado", "blueberry", "cherry")

words.groupBy(_.head)
// Map('a' -> List(apple, avocado), 'b' -> List(banana, blueberry), 'c' -> List(cherry))
```

- 그룹으로 나눈 뒤 각 원소를 변형하거나, 각 그룹을 하나의 값으로 다시 합치고 싶다면 `groupMap`, `groupMapReduce` 사용
  - 둘 다 "`groupBy` 후 추가 연산"을 한 번에 처리해 중간 `Map`을 두 번 순회하지 않아도 되는 최적화된 형태

```scala
// groupMap(f)(g): f로 그룹 나누고, 그룹 안의 원소는 g로 변형
words.groupMap(_.head)(_.length)
// Map('a' -> List(5, 7), 'b' -> List(6, 9), 'c' -> List(6))

// groupMapReduce(f)(g)(h): 위에 더해 그룹을 h로 합쳐 값 하나로 축약
words.groupMapReduce(_.head)(_.length)(_ + _)
// Map('a' -> 12, 'b' -> 15, 'c' -> 6)   : 그룹별 길이 합계
```

왜 필요한가 — `groupBy(...).mapValues(...)` 대신 `groupMap`을 쓰는 이유:
- `words.groupBy(_.head).view.mapValues(_.map(_.length))`처럼 짜도 같은 결과가 나오지만, 그룹으로 나눈 결과를 다시 순회하며 변환하는 두 단계 작업이 됨
- `groupMap`/`groupMapReduce`는 이를 한 번의 순회로 처리하도록 표준 라이브러리가 제공하는 축약형

---

### 8. partition — 조건으로 둘로 쪼개기

- `partition(p)`: 술어(predicate) `p`를 만족하는 원소와 만족하지 않는 원소로 컬렉션을 정확히 둘로 나눔
- "그룹 개수가 정확히 2개로 고정된 `groupBy`"라고 볼 수 있음

```scala
val nums = List(1, 2, 3, 4, 5, 6)

val (even, odd) = nums.partition(_ % 2 == 0)
// even = List(2, 4, 6), odd = List(1, 3, 5)
```

- `groupBy(_ % 2 == 0)`로도 비슷한 결과를 얻을 수 있으나, 결과가 `Map[Boolean, List[Int]]`가 되어 값을 꺼낼 때 매번 키로 조회해야 함
- 이분법이 명확한 경우에는 튜플을 바로 반환하는 `partition`이 더 간결

---

### 9. 정리 — 언제 무엇을 쓰나

- 컬렉션을 순회하며 값 하나로 압축, 초깃값·결과 타입 자유 → `foldLeft` / `foldRight`
- 같은 타입끼리 합치기, 초깃값 없이 → `reduce` / `reduceLeft`
- 빈 컬렉션일 수도 있는 reduce → `reduceOption` 계열
- 합계·곱·최댓값·최솟값(표준 집계) → `sum` / `product` / `min` / `max` (+ `Option` 버전)
- 기본 순서로 정렬 → `sorted`
- 커스텀 비교 함수로 정렬 → `sortWith`
- 비교 키를 뽑아 정렬 → `sortBy`
- 기준으로 여러 그룹 나누기 → `groupBy`
- 그룹 나누며 원소 변형/축약까지 한 번에 → `groupMap` / `groupMapReduce`
- 조건 하나로 정확히 둘로 나누기 → `partition`

---

### 참고 자료

- [Trait Iterable — Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [Seqs — Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
