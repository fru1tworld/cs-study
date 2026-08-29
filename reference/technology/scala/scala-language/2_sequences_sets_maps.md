# Scala 시퀀스, Set, Map

## 시퀀스: List, Vector, Array, Range와 구체 컬렉션 클래스

> 원문:
> - https://docs.scala-lang.org/overviews/collections-2.13/seqs.html
> - https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html
> - https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html
> - https://docs.scala-lang.org/overviews/collections-2.13/arrays.html

---

### 목차

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

### 1. Seq란 무엇인가

`Seq` = 순서가 있고, 각 원소가 0부터 시작하는 고정된 인덱스를 가지는 컬렉션.
`Iterable`을 상속하면서 동시에 `PartialFunction[Int, T]`를 상속 → `seq(i)`처럼 함수 호출 문법으로 인덱스 접근 가능.

```scala
val xs: Seq[Int] = List(10, 20, 30)
xs(1)          // 20  — Seq가 PartialFunction[Int, T]이기 때문에 가능
xs.length      // 3
```

- 왜 `PartialFunction`인가
  - "부분(partial)"인 이유: 인덱스 범위를 벗어나면 정의되지 않음
  - `xs(10)`처럼 범위를 넘어서면 예외 발생
  - 일반 함수는 모든 입력에 대해 값을 정의해야 하지만, `Seq`는 자신의 길이만큼만 정의된 "부분 함수"

`Seq`는 `mutable.Seq`와 `immutable.Seq`로 갈라짐 → 이 문서에서는 그 아래에 있는 구체 클래스(`List`, `Vector`, `Range`, `ArrayBuffer` 등)와 `Seq`처럼 다루어지는 `Array`를 함께 다룸.

---

### 2. LinearSeq와 IndexedSeq: 두 가지 접근 최적화 방향

`Seq`는 접근 패턴에 따라 두 하위 트레이트로 나뉨 → 어느 쪽에 속하느냐에 따라 "무엇이 빠른 연산인가"가 완전히 달라짐.

- `LinearSeq`
  - 빠른 연산: `head`, `tail`(선두부터 순차 접근)
  - 느린 연산: `apply(i)`(임의 인덱스 접근)
  - 대표 구현: `List`, `LazyList`
  - "앞에서부터 하나씩 떼어 가며 처리"하는 재귀적 알고리즘에 유리
- `IndexedSeq`
  - 빠른 연산: `apply(i)`, `length`, `update`(임의 접근)
  - 느린 연산: 선두부터 하나씩 분해하는 재귀
  - 대표 구현: `Array`, `ArrayBuffer`, `Vector`(가까움)
  - "몇 번째 원소가 필요해"처럼 무작위로 인덱스에 접근할 때 유리

```scala
def sum(xs: List[Int]): Int = xs match
  case Nil          => 0
  case head :: tail => head + sum(tail)   // List는 head/tail 분해가 O(1)
```

`Vector` = 이 둘 사이의 절충안. `IndexedSeq` 계열에 속하지만 순차 접근도 충분히 빠르게 동작 → "어느 쪽 패턴으로 쓰든 무난한" 범용 시퀀스 역할.

---

### 3. Seq의 공통 연산

모든 `Seq`(가변/불변 공통)가 제공하는 연산을 목적별로 정리:

- 인덱스/길이: `apply(i)`, `length`, `indices`, `isDefinedAt(i)`
- 검색: `indexOf`, `lastIndexOf`, `indexWhere`, `contains`
- 추가(새 시퀀스 반환): `:+`(뒤에 추가), `+:`(앞에 추가), `padTo`
- 갱신: `updated(i, v)` — 항상 새 시퀀스를 반환
- 정렬: `sorted`, `sortBy`, `sortWith`
- 뒤집기: `reverse`, `reverseIterator`
- 비교: `startsWith`, `endsWith`, `sameElements`
- 집합 연산: `intersect`, `diff`, `distinct`

```scala
val a = List(3, 1, 2)
a.updated(0, 9)   // List(9, 1, 2) — 원본 a는 그대로, 새 List가 반환됨
a :+ 4            // List(3, 1, 2, 4)
a.sorted          // List(1, 2, 3)
```

- `update`와 `updated`를 헷갈리지 말 것
  - `updated(i, v)`: 모든 `Seq`에서 사용 가능, 원본은 그대로 두고 새 시퀀스를 반환
  - `update(i, v)`: 가변(mutable) `Seq`에서만 사용 가능, 그 자리에서 값을 바꿈
    - `seq(i) = v`라는 대입 문법 = `seq.update(i, v)`의 축약형
  - 이름이 한 글자 차이라 혼동하기 쉬움 → 하나는 "새로 만든다", 하나는 "제자리에서 바꾼다"는 정반대의 의미

가변 시퀀스 중 `mutable.IndexedSeq`는 제자리에서 바꾸는 연산도 추가 제공 → `mapInPlace`, `sortInPlace`, `sortInPlaceBy` 등.

---

### 4. 불변 시퀀스 1 — List

`List` = 함수형 Scala 코드에서 가장 많이 보이는 시퀀스. 내부 구조가 "머리(head) + 나머지(tail)"를 재귀적으로 이어 붙인 연결 리스트 → 다음 성질을 가짐:

- `head`, `tail`, `::`(맨 앞에 원소 추가)는 모두 O(1)
- `length`나 `xs(i)`처럼 "몇 번째 원소"에 접근하는 연산은 O(n) — 앞에서부터 다 세어야 함
- 원소를 공유(structural sharing) → 앞에 원소 하나를 추가한 새 리스트를 만들어도 기존 리스트의 메모리를 그대로 재사용

```scala
val xs = List(1, 2, 3)
val ys = 0 :: xs        // List(0, 1, 2, 3) — xs는 그대로, ys가 xs를 공유
xs.head                 // 1, O(1)
xs(2)                   // 3이지만, 내부적으로 두 번 tail을 타야 함 — O(n)
```

- 언제 쓰나
  - "선두부터 순서대로 처리한다", "재귀로 분해하며 훑는다"는 패턴에 최적
  - 반대로 "가운데 인덱스에 자주 접근한다"거나 "끝에 원소를 자주 추가한다"면 `Vector`가 더 낫음

---

### 5. 불변 시퀀스 2 — Vector

`Vector` = "인덱스 접근도, 앞/뒤 추가도 어느 쪽이든 무난히 빠르게" 만들려는 목적으로 설계된 불변 인덱스 시퀀스. 불변 컬렉션에서 인덱스 기반 시퀀스가 필요할 때 기본으로 선택되는 타입.

- 내부적으로 branching factor(가지 수) 32인 트리 구조로 표현
- `apply(i)`(인덱스 접근)는 트리를 최대 5단 정도만 내려가면 됨 → 사실상 상수 시간에 가까움
- `updated(i, v)`도 트리 전체를 복사하지 않고 바뀐 경로의 노드만 새로 만듦 → 효율적

```scala
val v = Vector(1, 2, 3, 4, 5)
v(3)             // 4 — 사실상 O(1)
v.updated(0, 99) // Vector(99, 2, 3, 4, 5) — 바뀐 경로만 복사, 나머지는 공유
```

`List`처럼 "선두 재귀 분해"에 극도로 특화되어 있지는 않지만 순차 접근도 충분히 빠름 → "인덱스 접근과 순차 접근을 둘 다 자주 쓴다"면 `Vector`가 안전한 기본 선택.

---

### 6. 불변 시퀀스 3 — Range

`Range` = "시작값, 끝값, 간격(step)" 딱 세 개의 숫자만으로 등간격 정수 수열 전체를 표현하는 시퀀스.

- 실제 원소를 메모리에 저장하지 않음 → 상수 공간(constant space) 사용. `1 to 1000000`이라 해도 세 숫자만 들고 있음
- 대부분의 연산(`length`, `apply(i)`, `contains`)이 계산으로 즉시 나옴 → 매우 빠름

```scala
val r1 = 1 to 5        // Range(1, 2, 3, 4, 5) — 끝값 포함
val r2 = 1 until 5     // Range(1, 2, 3, 4)     — 끝값 미포함
val r3 = 1 to 10 by 3  // Range(1, 4, 7, 10)    — 간격 지정
```

`for (i <- 1 to n)` 같은 반복문에서 흔히 등장 → 실제 컬렉션이 필요할 때는 `.toList`, `.toVector` 등으로 변환 가능.

---

### 7. 불변 시퀀스 4 — LazyList (간단히)

`LazyList` = `List`와 거의 같은 API를 갖지만, 원소를 미리 계산하지 않고 실제로 요청될 때 계산. `::` 대신 `#::`로 이어 붙임.

```scala
def from(n: Int): LazyList[Int] = n #:: from(n + 1)  // 무한 수열이지만 즉시 평가되지 않음

val nats = from(1)
nats.take(3).toList   // List(1, 2, 3) — 여기서 처음 3개만 실제로 계산됨
```

한 번 계산된 원소는 다음 접근을 위해 캐시됨 → 무한 수열이나 "필요한 만큼만 계산하고 싶은" 상황(예: 피보나치수열)에 적합. 이미 계산된 부분의 성능 특성은 `List`와 동일.

---

### 8. 가변 시퀀스와 버퍼

버퍼(Buffer) = 가변 시퀀스 중에서도 크기를 자유롭게 늘리고 줄일 수 있는 것. "추가·삽입·삭제가 잦다"면 버퍼 사용.

- `ArrayBuffer`
  - 내부 구조: 내부 배열 + 크기 카운터
  - 특히 빠른 연산: 끝에 추가(`+=`, `++=`) — 상각 O(1)
  - 용도: 끝에만 계속 추가하며 컬렉션을 만들 때
- `ListBuffer`
  - 내부 구조: 연결 리스트 기반
  - 특히 빠른 연산: 양 끝 추가, `List`로의 변환
  - 용도: 최종적으로 `List`가 필요할 때
- `mutable.Queue`
  - 내부 구조: 내부적으로 버퍼와 유사
  - 특히 빠른 연산: `+=`(넣기), `dequeue`(빼기)
  - 용도: FIFO 큐가 필요할 때
- `StringBuilder`
  - 내부 구조: 문자 배열
  - 특히 빠른 연산: `+=`, `++=`로 누적 후 `toString`
  - 용도: 문자열을 반복적으로 이어 붙일 때

```scala
val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf += 1          // ArrayBuffer(1)
buf += 2 += 3      // ArrayBuffer(1, 2, 3)
buf.toArray        // Array(1, 2, 3)

val q = scala.collection.mutable.Queue(1, 2, 3)
q.dequeue()        // 1을 반환하면서 큐에서 제거 (제자리 변경)
```

- 문자열을 `+`로 계속 이어 붙이지 말 것
  - `s1 + s2 + s3 + ...`처럼 반복문 안에서 `String`을 `+`로 이어 붙이면 매번 새 문자열을 통째로 복사
  - 여러 조각을 누적해야 한다면 `StringBuilder`에 `+=`로 쌓았다가 마지막에 한 번만 `toString`을 호출하는 편이 훨씬 빠름
  - `ArrayBuffer`가 정수/객체에 대해 하는 일을, `StringBuilder`는 문자에 대해 함

이 밖에 `ArraySeq` = "배열만큼 빠르면서도, 원소 타입을 모르는 채로 제네릭하게 다루고 싶을 때" 쓰는 고정 크기 가변 시퀀스. 내부적으로 `Array[Object]`에 값을 담음.

---

### 9. Array — Seq이면서 Seq가 아닌 존재

`Array[Int]`, `Array[Double]` 같은 Scala 배열은 런타임에서 Java의 `int[]`, `double[]`과 완전히 동일한 표현. 성능을 위해 이 저수준 표현을 그대로 유지 → 배열은 실제로는 `Seq`를 상속하지 않음. 그런데도 `map`, `filter`, `sorted` 같은 `Seq`의 메서드를 배열에 바로 쓸 수 있는 이유 = 두 가지 암시적 변환 덕분:

- `ArrayOps`로 감싸기
  - 하는 일: 배열을 임시 래퍼로 감싸 `Seq` 메서드를 빌려 씀. 배열 자체는 그대로 배열
  - 우선순위: 높음(기본 적용)
- `ArraySeq`로 감싸기
  - 하는 일: 배열을 진짜 `Seq` 서브타입인 `ArraySeq`로 감쌈
  - 우선순위: 낮음(`Seq` 타입이 필요할 때만)

```scala
val arr = Array(3, 1, 2)
arr.sorted            // Array(1, 2, 3) — ArrayOps로 감싸져 sorted를 빌려 씀, 결과도 Array
val s: Seq[Int] = arr // 여기서는 ArraySeq로 감싸져 진짜 Seq가 됨
```

`ArrayOps`로 감싸는 동작은 대부분 임시 객체 → 최신 JVM이 최적화로 제거 → 실질적으로 배열 자체를 다루는 것과 비슷한 성능.

제네릭 배열 생성은 별도 주의 필요. `Array[T]`처럼 타입 매개변수로 배열을 만들려면 런타임에 "T가 실제로 무슨 타입인지" 정보가 있어야 함 → JVM은 제네릭 타입을 지워버리기(erasure) 때문에 컴파일러가 `ClassTag[T]`라는 암시적 값을 추가로 요구.

```scala
def makeArray[T: ClassTag](x: T, n: Int): Array[T] = Array.fill(n)(x)
// ClassTag가 없으면: "No ClassTag available for T" 컴파일 오류
```

- 제네릭 배열은 느릴 수 있다
  - 타입이 지워진 채로 다루는 제네릭 배열 접근은, `Int`나 객체 전용 배열처럼 구체 타입이 고정된 배열보다 3~4배 느릴 수 있음
  - 성능이 중요한 코드라면 가능한 한 구체 타입 배열(`Array[Int]` 등) 사용이 유리

---

### 10. 무엇을 언제 쓸까 — 선택 가이드

- 앞에서부터 순서대로 재귀 처리(함수형 스타일) → `List`
- 인덱스 접근과 순차 접근을 둘 다 자주 함, 불변이 필요함 → `Vector`
- 등간격 정수 수열(반복문 등) → `Range`
- 무한 수열, 필요한 만큼만 계산 → `LazyList`
- 끝에 계속 추가하며 컬렉션을 만들고, 나중에 배열로 씀 → `ArrayBuffer`
- 끝에 계속 추가하며 만들고, 나중에 `List`로 씀 → `ListBuffer`
- FIFO 큐(넣고 빼기) → `mutable.Queue`
- 문자열을 반복적으로 이어 붙임 → `StringBuilder`
- Java와의 상호운용, 성능이 최우선 → `Array`

---

### 11. 참고 자료

- [Seq (공식 문서)](https://docs.scala-lang.org/overviews/collections-2.13/seqs.html)
- [구체 불변 컬렉션 클래스](https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html)
- [구체 가변 컬렉션 클래스](https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html)
- [Arrays](https://docs.scala-lang.org/overviews/collections-2.13/arrays.html)

---

## Set과 Map

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/sets.html , https://docs.scala-lang.org/overviews/collections-2.13/maps.html

---

### 목차

1. [Set — 중복 없는 컬렉션](#1-set--중복-없는-컬렉션)
   - 1.1 [불변 Set과 가변 Set](#11-불변-set과-가변-set)
   - 1.2 [Set의 핵심 연산](#12-set의-핵심-연산)
   - 1.3 [집합 연산: 합집합·교집합·차집합](#13-집합-연산-합집합교집합차집합)
   - 1.4 [SortedSet과 BitSet](#14-sortedset과-bitset)
2. [Map — 키-값 컬렉션](#2-map--키-값-컬렉션)
   - 2.1 [Map 만들기와 `->` 표기법](#21-map-만들기와---표기법)
   - 2.2 [조회 연산: get, apply, getOrElse](#22-조회-연산-get-apply-getorelse)
   - 2.3 [불변 Map 갱신](#23-불변-map-갱신)
   - 2.4 [가변 Map 갱신과 `getOrElseUpdate`](#24-가변-map-갱신과-getorelseupdate)
   - 2.5 [키·값 추출과 변환](#25-키값-추출과-변환)
3. [정리: Set vs Map vs Seq](#3-정리-set-vs-map-vs-seq)

---

### 1. Set — 중복 없는 컬렉션

`Set` = 같은 원소를 두 번 담지 않는 컬렉션. `Iterable`을 상속 → `map`, `filter`, `foreach` 등 다른 컬렉션과 동일한 API를 그대로 씀 + 그 위에 "원소가 있는가"를 묻는 연산과 수학적 집합 연산(합집합·교집합·차집합)이 추가로 붙음.

- List와 뭐가 다른가
  - `List`는 순서와 중복을 그대로 유지
  - `Set`은 순서를 보장하지 않고 중복을 자동으로 제거
  - "이 원소가 들어 있는가"를 자주 물어봐야 하는 상황(예: 방문한 노드 추적, 중복 제거)에 적합

#### 1.1 불변 Set과 가변 Set

Scala는 다른 컬렉션과 마찬가지로 Set도 불변(`scala.collection.immutable.Set`)과 가변(`scala.collection.mutable.Set`) 두 갈래로 나뉨 → 아무 접두어 없이 `Set`을 쓰면 불변 버전이 기본으로 임포트됨.

```scala
val fruit = Set("apple", "orange", "peach", "banana")
fruit("peach")   // true  — apply(x)는 contains(x)와 같음
```

- 불변 Set: 원소를 추가/삭제하면 새 Set을 반환하고 원본은 그대로.
  - 내부적으로 원소 0개는 싱글턴, 4개 이하는 필드에 직접 저장, 그 이상은 압축 해시 배열 트라이(hash-array mapped trie)로 저장 → 메모리를 아낌
- 가변 Set: 제자리에서(in place) 원소를 바꾸고 자기 자신을 반환
  - 기본 구현은 해시 테이블

- `val` 불변 Set ↔ `var` 가변 Set 치환 원칙
  - 공식 문서: "가변 컬렉션을 담은 `val`은 불변 컬렉션을 담은 `var`로 바꿔 쓸 수 있는 경우가 많다"
  - 즉 `val s = mutable.Set(...); s += x` 대신 `var s = Set(...); s = s + x`로 써도 같은 효과
  - 이 대체 가능성 → "왜 두 종류나 두는가"에 대한 답: 팀·상황에 따라 가변성을 어디서 관리할지 선택 가능

#### 1.2 Set의 핵심 연산

- `xs.contains(x)` / `xs(x)`: `x`가 원소인지(`apply`는 `contains`의 별칭)
- `xs.subsetOf(ys)`: `xs`의 모든 원소가 `ys`에도 있는지
- `xs + x`(불변) / `xs incl x`: 원소 `x`를 추가한 새 Set
- `xs - x`(불변) / `xs excl x`: 원소 `x`를 뺀 새 Set
- `xs += x`(가변) / `xs.addOne(x)`: `xs`에 `x`를 제자리 추가
- `xs -= x`(가변) / `xs.subtractOne(x)`: `xs`에서 `x`를 제자리 제거
- `xs.add(x)` / `xs.remove(x)`(가변): 추가/제거 성공 여부를 `Boolean`으로 반환

```scala
import scala.collection.mutable

val s = mutable.Set(1, 2, 3)
s += 4        // Set(1, 2, 3, 4)
s -= 2        // Set(1, 3, 4)
s.add(5)      // true  (이미 있으면 false)
```

#### 1.3 집합 연산: 합집합·교집합·차집합

- 합집합(union): `xs | ys` / `xs union ys`
- 교집합(intersection): `xs & ys` / `xs intersect ys`
- 차집합(difference): `xs &~ ys` / `xs diff ys`

```scala
val a = Set(1, 2, 3)
val b = Set(2, 3, 4)

a union b        // Set(1, 2, 3, 4)
a intersect b     // Set(2, 3)
a diff b          // Set(1)
```

- `--`은 차집합이 아니라 "여러 원소 제거"
  - `xs -- ys`는 `xs diff ys`와 결과가 같아 보이지만 의미상 목적이 다름
  - `--`: "`xs`에서 `ys`에 있는 원소들을 하나씩 지운다"는 불변 컬렉션의 일괄 삭제 연산
  - `diff`: "두 집합의 수학적 차집합을 구한다"는 집합론 연산
  - 결과 값은 같지만 읽는 사람에게 전달하는 의도가 다름 → 집합 관점의 코드라면 `diff`를 쓰는 것이 더 명확

#### 1.4 SortedSet과 BitSet

- `SortedSet`: 원소를 정렬된 순서로 순회할 수 있는 Set. 기본 구현인 `immutable.TreeSet`은 레드-블랙 트리 사용

  ```scala
  import scala.collection.immutable.TreeSet

  val ts = TreeSet(3, 1, 2)     // TreeSet(1, 2, 3) — 항상 정렬된 순서
  ts.rangeFrom(2)                // TreeSet(2, 3)
  ```

  커스텀 정렬 기준이 필요하면 `TreeSet.empty(customOrdering)`처럼 `Ordering`을 직접 넘길 수 있음.

- `BitSet`: 0 이상의 정수만 담는 Set을 `Long` 배열의 비트로 표현해 메모리를 극도로 아끼는 특수 구현. `Long` 하나가 64개 정수(0~63, 64~127, ...)를 표현 → 정수 집합에서 포함 여부 검사와 추가/삭제가 매우 빠름

  ```scala
  import scala.collection.immutable.BitSet

  val bs = BitSet(1, 3, 5, 100)
  bs.contains(3)   // true
  ```

---

### 2. Map — 키-값 컬렉션

`Map` = 키(key)와 값(value)의 짝(binding/association)을 모아 둔 `Iterable`. 하나의 키는 하나의 값에만 대응 → 같은 키로 다시 넣으면 기존 값을 덮어씀.

#### 2.1 Map 만들기와 `->` 표기법

```scala
val ages = Map("Alice" -> 30, "Bob" -> 25)
```

`"Alice" -> 30`은 튜플 `("Alice", 30)`을 만드는 `Predef`의 편의 문법. `Map(("Alice", 30), ("Bob", 25))`라고 써도 동일하지만, `->` 표기가 "키가 값을 가리킨다"는 의미를 더 잘 드러냄.

#### 2.2 조회 연산: get, apply, getOrElse

- `m(key)`(`apply`): 반환 타입 `Value`, 키가 없을 때 예외(`NoSuchElementException`)
- `m.get(key)`: 반환 타입 `Option[Value]`, 키가 없을 때 `None`
- `m.getOrElse(key, default)`: 반환 타입 `Value`, 키가 없을 때 `default` 값
- `m.contains(key)`: 반환 타입 `Boolean`, 키가 없을 때 `false`

```scala
val ages = Map("Alice" -> 30)

ages("Alice")                  // 30
ages.get("Bob")                 // None
ages.getOrElse("Bob", 0)        // 0
ages("Bob")                     // 예외 발생!
```

- 왜 `apply` 말고 `get`을 기본으로 쓰라고 하는가
  - `m(key)`는 간결하지만 키가 없으면 프로그램이 죽음
  - `get`은 "있을 수도, 없을 수도 있다"는 사실을 `Option`이라는 타입으로 코드에 그대로 드러냄 → 호출하는 쪽이 `None` 처리를 빼먹으면 컴파일러가 패턴 매칭 누락 등으로 알려줄 여지가 생김
  - 키 존재가 보장된 상황(설정값이 항상 있는 경우 등)에서만 `apply`나 `getOrElse` 사용이 안전

#### 2.3 불변 Map 갱신

불변 `Map`의 갱신 연산은 전부 새 Map을 반환:

- `m.updated(k, v)` / `m + (k -> v)`: `k`에 `v`를 대응시킨 새 Map(있으면 덮어씀)
- `m ++ other`: 두 Map을 합친 새 Map(겹치는 키는 `other` 값 우선)
- `m.removed(k)` / `m - k`: `k`를 뺀 새 Map
- `m.removedAll(ks)` / `m -- ks`: 여러 키를 한 번에 뺀 새 Map

```scala
val m1 = Map("a" -> 1, "b" -> 2)
val m2 = m1.updated("c", 3)   // Map(a -> 1, b -> 2, c -> 3), m1은 그대로
val m3 = m1 - "a"             // Map(b -> 2)
```

#### 2.4 가변 Map 갱신과 `getOrElseUpdate`

가변 `Map`은 값을 제자리에서 바꿈.

```scala
import scala.collection.mutable

val m = mutable.Map("a" -> 1)
m("a") = 10          // update(key, value)와 동일
m += ("b" -> 2)       // 제자리 추가
m -= "a"              // 제자리 제거
```

`getOrElseUpdate`는 캐시(cache)를 만들 때 특히 유용. 키가 있으면 그 값을 반환하고, 없으면 두 번째 인자(기본값)를 계산해 저장까지 한 번에 처리.

```scala
val cache = mutable.Map.empty[Int, Long]

def expensive(n: Int): Long = { /* 오래 걸리는 계산 */ n.toLong * n }

def cachedExpensive(n: Int): Long =
  cache.getOrElseUpdate(n, expensive(n))
```

- 두 번째 인자는 "이름에 의한 호출(by-name)"
  - `getOrElseUpdate(key, default)`의 `default`는 키가 없을 때만 평가됨
  - `expensive(n)`은 캐시에 값이 이미 있으면 아예 실행되지 않음
  - 값을 미리 계산해서 넘기는 게 아니라, "필요할 때만 계산하라"는 지연 평가 → 캐시 본연의 목적(중복 계산 방지) 성립

#### 2.5 키·값 추출과 변환

- `m.keys` / `m.keySet`: 모든 키(Iterable / Set)
- `m.values`: 모든 값
- `m.filterKeys(pred)`(2.13에서 지원 중단 예고): 키가 조건을 만족하는 항목만 남긴 View
- `m.mapValues(f)`(2.13에서 지원 중단 예고): 값에 `f`를 적용한 View

```scala
val m = Map("a" -> 1, "b" -> 2, "c" -> 3)

m.keys                       // Iterable(a, b, c)
m.values                     // Iterable(1, 2, 3)
m.view.filterKeys(_ != "a").toMap   // Map(b -> 2, c -> 3)
m.view.mapValues(_ * 10).toMap      // Map(a -> 10, b -> 20, c -> 30)
```

- `filterKeys`/`mapValues`는 View를 거쳐 쓰는 게 안전
  - 과거 `filterKeys`, `mapValues`는 즉시 새 `Map`을 만드는 것처럼 보였지만 실제로는 지연 평가되는 View를 반환 → 혼란 유발
  - Scala 2.13부터는 이 둘이 지원 중단(deprecated) 예고 상태 → `m.view.filterKeys(...).toMap`처럼 `view`를 명시하고 마지막에 `toMap`으로 확정 짓는 방식 권장

`SortedMap`(예: `immutable.TreeMap`)은 `SortedSet`과 같은 원리로, 키를 정렬된 순서로 순회할 수 있는 Map.

```scala
import scala.collection.immutable.TreeMap

val tm = TreeMap(3 -> "c", 1 -> "a", 2 -> "b")   // 키 순서로 정렬됨
```

---

### 3. 정리: Set vs Map vs Seq

- `Seq`
  - 중복: 허용
  - 순서: 유지
  - 접근 방식: 인덱스로 접근
- `Set`
  - 중복: 불허
  - 순서: 보장 안 함
  - 접근 방식: 원소 존재 여부로 접근
- `Map`
  - 중복: 키는 불허(값은 중복 가능)
  - 순서: 보장 안 함(SortedMap 제외)
  - 접근 방식: 키로 값에 접근

세 컬렉션 모두 `Iterable`을 상속 → `map`, `filter`, `foreach`, `foldLeft` 같은 공통 연산은 그대로 통용. 다만 Set은 집합 연산(union/intersect/diff), Map은 키 기반 조회·갱신(get/getOrElse/updated)이라는 그 컬렉션만의 API가 추가로 붙는다는 점이 핵심.

---

### 참고 자료

- [Scala Collections — Sets](https://docs.scala-lang.org/overviews/collections-2.13/sets.html)
- [Scala Collections — Maps](https://docs.scala-lang.org/overviews/collections-2.13/maps.html)
