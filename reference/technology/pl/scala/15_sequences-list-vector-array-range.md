# 시퀀스: List, Vector, Array, Range와 구체 컬렉션 클래스

> **원문:**
> - https://docs.scala-lang.org/overviews/collections-2.13/seqs.html
> - https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html
> - https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html
> - https://docs.scala-lang.org/overviews/collections-2.13/arrays.html

---

## 목차

1. [Seq란 무엇인가](#1-seq란-무엇인가)
2. [LinearSeq와 IndexedSeq: 두 가지 접근 최적화 방향](#2-linearseq와-indexedseq-두-가지-접근-최적화-방향)
3. [Seq의 공통 연산](#3-seq의-공통-연산)
4. [불변 시퀀스 1 — List](#4-불변-시퀀스-1--list)
5. [불변 시퀀스 2 — Vector](#5-불변-시퀀스-2--vector)
6. [불변 시퀀스 3 — Range](#6-불변-시퀀스-3--range)
7. [불변 시퀀스 4 — LazyList (간단히)](#7-불변-시퀀스-4--lazylist-간단히)
8. [가변 시퀀스와 버퍼](#8-가변-시퀀스와-버퍼)
9. [Array — Seq이면서 Seq가 아닌 존재](#9-array--seq이면서-seq가-아닌-존재)
10. [무엇을 언제 쓸까 — 선택 가이드](#10-무엇을-언제-쓸까--선택-가이드)
11. [참고 자료](#11-참고-자료)

---

## 1. Seq란 무엇인가

`Seq`는 "순서가 있고, 각 원소가 0부터 시작하는 고정된 인덱스를 가지는" 컬렉션입니다.
`Iterable`을 상속하면서 동시에 `PartialFunction[Int, T]`를 상속하기 때문에,
`seq(i)`처럼 함수 호출 문법으로 인덱스 접근을 할 수 있습니다.

```scala
val xs: Seq[Int] = List(10, 20, 30)
xs(1)          // 20  — Seq가 PartialFunction[Int, T]이기 때문에 가능
xs.length      // 3
```

> 💡 **왜 `PartialFunction`인가**
>
> "부분(partial)"인 이유는 인덱스 범위를 벗어나면 정의되지 않기 때문입니다. `xs(10)`처럼 범위를 넘어서면
> 예외가 발생합니다. 일반 함수라면 모든 입력에 대해 값을 정의해야 하지만, `Seq`는 자신의 길이만큼만 정의된
> "부분 함수"인 셈입니다.

`Seq`는 `mutable.Seq`와 `immutable.Seq`로 갈라지며, 이 문서에서는 그 아래에 있는 구체 클래스 —
`List`, `Vector`, `Range`, `ArrayBuffer` 등 — 와 `Seq`처럼 다루어지는 `Array`를 함께 살펴봅니다.

---

## 2. LinearSeq와 IndexedSeq: 두 가지 접근 최적화 방향

`Seq`는 접근 패턴에 따라 두 하위 트레이트로 나뉩니다. 어느 쪽에 속하느냐에 따라
"무엇이 빠른 연산인가"가 완전히 달라집니다.

| 구분 | 빠른 연산 | 느린 연산 | 대표 구현 |
|---|---|---|---|
| **LinearSeq** | `head`, `tail` (선두부터 순차 접근) | `apply(i)` (임의 인덱스 접근) | `List`, `LazyList` |
| **IndexedSeq** | `apply(i)`, `length`, `update` (임의 접근) | 선두부터 하나씩 분해하는 재귀 | `Array`, `ArrayBuffer`, `Vector`(가까움) |

- **LinearSeq**는 "앞에서부터 하나씩 떼어 가며 처리"하는 재귀적 알고리즘에 유리합니다.
- **IndexedSeq**는 "몇 번째 원소가 필요해"처럼 무작위로 인덱스에 접근할 때 유리합니다.

```scala
def sum(xs: List[Int]): Int = xs match
  case Nil          => 0
  case head :: tail => head + sum(tail)   // List는 head/tail 분해가 O(1)
```

`Vector`는 이 둘 사이의 절충안입니다. `IndexedSeq` 계열에 속하지만, 순차 접근도 충분히 빠르게 동작하도록
설계되어 "어느 쪽 패턴으로 쓰든 무난한" 범용 시퀀스 역할을 합니다.

---

## 3. Seq의 공통 연산

모든 `Seq`(가변/불변 공통)가 제공하는 연산을 목적별로 묶으면 다음과 같습니다.

| 목적 | 대표 메서드 |
|---|---|
| 인덱스/길이 | `apply(i)`, `length`, `indices`, `isDefinedAt(i)` |
| 검색 | `indexOf`, `lastIndexOf`, `indexWhere`, `contains` |
| 추가(새 시퀀스 반환) | `:+`(뒤에 추가), `+:`(앞에 추가), `padTo` |
| 갱신 | `updated(i, v)` — 항상 새 시퀀스를 반환 |
| 정렬 | `sorted`, `sortBy`, `sortWith` |
| 뒤집기 | `reverse`, `reverseIterator` |
| 비교 | `startsWith`, `endsWith`, `sameElements` |
| 집합 연산 | `intersect`, `diff`, `distinct` |

```scala
val a = List(3, 1, 2)
a.updated(0, 9)   // List(9, 1, 2) — 원본 a는 그대로, 새 List가 반환됨
a :+ 4            // List(3, 1, 2, 4)
a.sorted          // List(1, 2, 3)
```

> ⚠️ **`update`와 `updated`를 헷갈리지 말 것**
>
> - `updated(i, v)` — **모든** `Seq`에서 사용 가능. 원본은 그대로 두고 **새 시퀀스**를 반환합니다.
> - `update(i, v)` — **가변(mutable) Seq**에서만 사용 가능. 그 자리에서 값을 바꿉니다.
>   `seq(i) = v`라는 대입 문법은 사실 `seq.update(i, v)`의 축약형입니다.
>
> 이름이 한 글자 차이라 혼동하기 쉬운데, 하나는 "새로 만든다", 하나는 "제자리에서 바꾼다"는 정반대의 의미입니다.

가변 시퀀스 중 `mutable.IndexedSeq`는 제자리에서 바꾸는 연산도 추가로 제공합니다.
`mapInPlace`, `sortInPlace`, `sortInPlaceBy` 등이 그 예입니다.

---

## 4. 불변 시퀀스 1 — List

`List`는 함수형 Scala 코드에서 가장 많이 보이는 시퀀스입니다. 내부 구조가
"머리(head) + 나머지(tail)"를 재귀적으로 이어 붙인 **연결 리스트**이기 때문에, 다음 성질을 가집니다.

- `head`, `tail`, `::`(맨 앞에 원소 추가)는 모두 **O(1)**.
- 반면 `length`나 `xs(i)`처럼 "몇 번째 원소"에 접근하는 연산은 **O(n)** — 앞에서부터 다 세어야 하기 때문.
- 원소를 공유(structural sharing)하기 때문에, 앞에 원소 하나를 추가한 새 리스트를 만들어도
  기존 리스트의 메모리를 그대로 재사용합니다.

```scala
val xs = List(1, 2, 3)
val ys = 0 :: xs        // List(0, 1, 2, 3) — xs는 그대로, ys가 xs를 공유
xs.head                 // 1, O(1)
xs(2)                   // 3이지만, 내부적으로 두 번 tail을 타야 함 — O(n)
```

> 💡 **언제 쓰나**
>
> "선두부터 순서대로 처리한다", "재귀로 분해하며 훑는다"는 패턴에 최적입니다.
> 반대로 "가운데 인덱스에 자주 접근한다"거나 "끝에 원소를 자주 추가한다"면 `Vector`가 더 낫습니다.

---

## 5. 불변 시퀀스 2 — Vector

`Vector`는 "인덱스 접근도, 앞/뒤 추가도 어느 쪽이든 무난히 빠르게" 만들려는 목적으로 설계된
불변 인덱스 시퀀스입니다. 불변 컬렉션에서 인덱스 기반 시퀀스가 필요할 때 기본으로 선택되는 타입입니다.

- 내부적으로 **branching factor(가지 수) 32**인 트리 구조로 표현됩니다.
- `apply(i)`(인덱스 접근)는 트리를 최대 5단 정도만 내려가면 되므로 사실상 **상수 시간**에 가깝습니다.
- `updated(i, v)`도 트리 전체를 복사하지 않고 **바뀐 경로의 노드만** 새로 만들기 때문에 효율적입니다.

```scala
val v = Vector(1, 2, 3, 4, 5)
v(3)             // 4 — 사실상 O(1)
v.updated(0, 99) // Vector(99, 2, 3, 4, 5) — 바뀐 경로만 복사, 나머지는 공유
```

`List`처럼 "선두 재귀 분해"에 극도로 특화되어 있지는 않지만, 순차 접근도 충분히 빠릅니다.
"인덱스 접근과 순차 접근을 둘 다 자주 쓴다"면 `Vector`가 안전한 기본 선택입니다.

---

## 6. 불변 시퀀스 3 — Range

`Range`는 "시작값, 끝값, 간격(step)" 딱 세 개의 숫자만으로 등간격 정수 수열 전체를 표현하는 시퀀스입니다.

- 실제 원소를 메모리에 저장하지 않으므로 **상수 공간**(constant space)을 씁니다. `1 to 1000000`이라 해도
  세 숫자만 들고 있을 뿐입니다.
- 대부분의 연산(`length`, `apply(i)`, `contains`)이 계산으로 즉시 나오므로 매우 빠릅니다.

```scala
val r1 = 1 to 5        // Range(1, 2, 3, 4, 5) — 끝값 포함
val r2 = 1 until 5     // Range(1, 2, 3, 4)     — 끝값 미포함
val r3 = 1 to 10 by 3  // Range(1, 4, 7, 10)    — 간격 지정
```

`for (i <- 1 to n)` 같은 반복문에서 흔히 등장하며, 실제 컬렉션이 필요할 때는 `.toList`, `.toVector` 등으로
변환할 수 있습니다.

---

## 7. 불변 시퀀스 4 — LazyList (간단히)

`LazyList`는 `List`와 거의 같은 API를 갖지만, 원소를 **미리 계산하지 않고 실제로 요청될 때 계산**합니다.
`::` 대신 `#::`로 이어 붙입니다.

```scala
def from(n: Int): LazyList[Int] = n #:: from(n + 1)  // 무한 수열이지만 즉시 평가되지 않음

val nats = from(1)
nats.take(3).toList   // List(1, 2, 3) — 여기서 처음 3개만 실제로 계산됨
```

한 번 계산된 원소는 다음 접근을 위해 캐시되므로, 무한 수열이나 "필요한 만큼만 계산하고 싶은" 상황
(예: 피보나치수열)에 적합합니다. 이미 계산된 부분의 성능 특성은 `List`와 동일합니다.

---

## 8. 가변 시퀀스와 버퍼

**버퍼**(Buffer)는 가변 시퀀스 중에서도 크기를 자유롭게 늘리고 줄일 수 있는 것을 말합니다.
"추가·삽입·삭제가 잦다"면 버퍼를 씁니다.

| 클래스 | 내부 구조 | 특히 빠른 연산 | 용도 |
|---|---|---|---|
| `ArrayBuffer` | 내부 배열 + 크기 카운터 | 끝에 추가(`+=`, `++=`) — 상각 O(1) | 끝에만 계속 추가하며 컬렉션을 만들 때 |
| `ListBuffer` | 연결 리스트 기반 | 양 끝 추가, `List`로의 변환 | 최종적으로 `List`가 필요할 때 |
| `mutable.Queue` | 내부적으로 버퍼와 유사 | `+=`(넣기), `dequeue`(빼기) | FIFO 큐가 필요할 때 |
| `StringBuilder` | 문자 배열 | `+=`, `++=`로 누적 후 `toString` | 문자열을 반복적으로 이어 붙일 때 |

```scala
val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf += 1          // ArrayBuffer(1)
buf += 2 += 3      // ArrayBuffer(1, 2, 3)
buf.toArray        // Array(1, 2, 3)

val q = scala.collection.mutable.Queue(1, 2, 3)
q.dequeue()        // 1을 반환하면서 큐에서 제거 (제자리 변경)
```

> ⚠️ **문자열을 `+`로 계속 이어 붙이지 말 것**
>
> `s1 + s2 + s3 + ...`처럼 반복문 안에서 `String`을 `+`로 이어 붙이면 매번 새 문자열을 통째로 복사합니다.
> 여러 조각을 누적해야 한다면 `StringBuilder`에 `+=`로 쌓았다가 마지막에 한 번만 `toString`을 호출하는
> 편이 훨씬 빠릅니다. `ArrayBuffer`가 정수/객체에 대해 하는 일을, `StringBuilder`는 문자에 대해 합니다.

이 밖에 `ArraySeq`는 "배열만큼 빠르면서도, 원소 타입을 모르는 채로 제네릭하게 다루고 싶을 때" 쓰는
고정 크기 가변 시퀀스입니다. 내부적으로 `Array[Object]`에 값을 담습니다.

---

## 9. Array — Seq이면서 Seq가 아닌 존재

`Array[Int]`, `Array[Double]` 같은 Scala 배열은 런타임에서 **Java의 `int[]`, `double[]`과 완전히 동일한
표현**입니다. 성능을 위해 이 저수준 표현을 그대로 유지하다 보니, 배열은 실제로는 `Seq`를 상속하지
않습니다. 그런데도 `map`, `filter`, `sorted` 같은 `Seq`의 메서드를 배열에 바로 쓸 수 있는 이유는
**두 가지 암시적 변환** 덕분입니다.

| 변환 | 하는 일 | 우선순위 |
|---|---|---|
| `ArrayOps`로 감싸기 | 배열을 임시 래퍼로 감싸 `Seq` 메서드를 빌려 씀. 배열 자체는 그대로 배열 | 높음(기본 적용) |
| `ArraySeq`로 감싸기 | 배열을 진짜 `Seq` 서브타입인 `ArraySeq`로 감쌈 | 낮음(`Seq` 타입이 필요할 때만) |

```scala
val arr = Array(3, 1, 2)
arr.sorted            // Array(1, 2, 3) — ArrayOps로 감싸져 sorted를 빌려 씀, 결과도 Array
val s: Seq[Int] = arr // 여기서는 ArraySeq로 감싸져 진짜 Seq가 됨
```

`ArrayOps`로 감싸는 동작은 대부분 임시 객체라 최신 JVM이 최적화로 제거해 주므로,
실질적으로 배열 자체를 다루는 것과 비슷한 성능을 냅니다.

**제네릭 배열 생성**은 별도 주의가 필요합니다. `Array[T]`처럼 타입 매개변수로 배열을 만들려면
런타임에 "T가 실제로 무슨 타입인지" 정보가 있어야 하는데, JVM은 제네릭 타입을 지워버리기(erasure) 때문에
컴파일러가 `ClassTag[T]`라는 암시적 값을 추가로 요구합니다.

```scala
def makeArray[T: ClassTag](x: T, n: Int): Array[T] = Array.fill(n)(x)
// ClassTag가 없으면: "No ClassTag available for T" 컴파일 오류
```

> ⚠️ **제네릭 배열은 느릴 수 있다**
>
> 타입이 지워진 채로 다루는 제네릭 배열 접근은, `Int`나 객체 전용 배열처럼 구체 타입이 고정된 배열보다
> 3~4배 느릴 수 있습니다. 성능이 중요한 코드라면 가능한 한 구체 타입 배열(`Array[Int]` 등)을 쓰는 편이
> 유리합니다.

---

## 10. 무엇을 언제 쓸까 — 선택 가이드

| 상황 | 추천 |
|---|---|
| 앞에서부터 순서대로 재귀 처리 (함수형 스타일) | `List` |
| 인덱스 접근과 순차 접근을 둘 다 자주 함, 불변이 필요함 | `Vector` |
| 등간격 정수 수열 (반복문 등) | `Range` |
| 무한 수열, 필요한 만큼만 계산 | `LazyList` |
| 끝에 계속 추가하며 컬렉션을 만들고, 나중에 배열로 씀 | `ArrayBuffer` |
| 끝에 계속 추가하며 만들고, 나중에 `List`로 씀 | `ListBuffer` |
| FIFO 큐 (넣고 빼기) | `mutable.Queue` |
| 문자열을 반복적으로 이어 붙임 | `StringBuilder` |
| Java와의 상호운용, 성능이 최우선 | `Array` |

---

## 11. 참고 자료

- [Seq (공식 문서)](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
- [구체 불변 컬렉션 클래스](https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html)
- [구체 가변 컬렉션 클래스](https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html)
- [Arrays](https://docs.scala-lang.org/overviews/collections-2.13/arrays.html)
