# 구체적인 불변·가변 컬렉션 클래스

## 구체적인 불변 컬렉션 클래스 (Concrete Immutable Collection Classes)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html>

---

Scala가 골라 쓸 수 있는 다양한 구체적인 불변(immutable) 컬렉션 클래스 제공.
- 구현하는 트레이트(trait, 맵·집합·시퀀스)·무한 가능 여부·각종 연산 속도에 따라 서로 다름
- 아래는 Scala에서 가장 흔히 사용되는 불변 컬렉션 타입 목록

### 리스트 (Lists)

[List](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/List.html) — 유한한 불변 시퀀스(sequence).
- 첫 번째 원소와 나머지 리스트에 대한 상수 시간(constant-time) 접근 제공
- 리스트 앞에 새 원소를 추가하는 상수 시간 콘스(cons) 연산 제공
- 그 밖의 많은 연산은 선형 시간(linear time) 소요

참고 — 콘스(cons)
- "construct"에서 온 말 → 리스트 맨 앞에 원소 하나를 붙여 새 리스트를 만드는 연산
- Scala에서는 `1 :: List(2, 3)`처럼 `::` 연산자로 표기
- 리스트가 "머리(head) + 나머지(tail)"라는 연결 구조 → 맨 앞에 붙이는 것은 노드 하나만 만들면 되어 상수 시간
- 끝에 붙이거나 중간에 접근하려면 앞에서부터 따라가야 함 → 선형 시간

### 지연 리스트 (LazyLists)

[LazyList](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/LazyList.html) — 원소가 지연(lazily) 계산된다는 점만 빼면 리스트와 동일.
- 요청된 원소만 계산됨 → 무한히 길 수 있음
- 그 외 성능 특성은 리스트와 동일

리스트가 `::` 연산자로 만들어지는 것과 달리, 지연 리스트는 비슷하게 생긴 `#::`로 만들어짐. 정수 1, 2, 3을 담은 지연 리스트 예시.

```scala
scala> val lazyList = 1 #:: 2 #:: 3 #:: LazyList.empty
lazyList: scala.collection.immutable.LazyList[Int] = LazyList(<not computed>)
```

- 이 지연 리스트의 머리(head)는 1, 꼬리(tail)에는 2와 3
- 여기서는 어떤 원소도 출력되지 않음 → 리스트가 아직 계산되지 않았기 때문
- 지연 리스트는 지연 계산되도록 명세됨 → `toString` 메서드는 불필요한 추가 평가를 강제하지 않도록 설계

좀 더 복잡한 예. 주어진 두 수로 시작하는 피보나치 수열(Fibonacci sequence, 각 원소가 바로 앞 두 원소의 합인 수열)을 담은 지연 리스트 계산.

```scala
scala> def fibFrom(a: Int, b: Int): LazyList[Int] = a #:: fibFrom(b, a + b)
fibFrom: (a: Int,b: Int)LazyList[Int]
```

- 수열의 첫 원소는 `a`, 나머지 수열은 `b`와 `a + b`로 시작하는 피보나치 수열
- 까다로운 부분: 무한 재귀(infinite recursion) 없이 이 수열을 계산하는 것
  - `#::` 대신 `::`를 사용했다면 → 함수 호출마다 또 다른 호출이 일어나 무한 재귀에 빠짐
  - `#::` 사용 → 우변은 요청되기 전까지 평가되지 않음

1 두 개로 시작하는 피보나치 수열의 처음 몇 개 원소.

```scala
scala> val fibs = fibFrom(1, 1).take(7)
fibs: scala.collection.immutable.LazyList[Int] = LazyList(<not computed>)
scala> fibs.toList
res9: List[Int] = List(1, 1, 2, 3, 5, 8, 13)
```

왜 필요한가 — 무한한 컬렉션의 쓸모
- "얼마나 필요할지 미리 알 수 없는 데이터"를 자연스럽게 표현하는 도구
- 수열의 생성 규칙만 정의해 두면 → 실제로 몇 개가 필요한지는 사용하는 쪽에서 `take(7)`처럼 나중에 결정 가능
- 생성 로직과 소비 로직이 분리됨
- 참고: 예전 Scala 버전의 `Stream`이 이 역할을 담당 → 2.13부터는 머리까지 완전히 지연 계산되는 `LazyList`로 대체됨

### 불변 ArraySeq (Immutable ArraySeqs)

리스트는 알고리즘이 머리 부분만 조심스럽게 처리하는 경우에 매우 효율적.
- 리스트 머리에 접근·추가·제거는 상수 시간
- 리스트 뒤쪽 원소에 접근·수정은 리스트 안으로 들어간 깊이에 비례하는 선형 시간

[ArraySeq](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/ArraySeq.html) — 리스트의 임의 접근(random access) 비효율을 해결하기 위해 Scala 2.13에서 도입된 컬렉션 타입.
- 컬렉션의 어떤 원소든 상수 시간에 접근 가능
- ArraySeq를 사용하는 알고리즘은 컬렉션 머리만 접근하도록 조심할 필요 없음 → 임의 위치의 원소에 접근 가능해 편하게 작성 가능

ArraySeq는 다른 시퀀스와 똑같은 방식으로 만들고 갱신.

```scala
scala> val arr = scala.collection.immutable.ArraySeq(1, 2, 3)
arr: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3)
scala> val arr2 = arr :+ 4
arr2: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3, 4)
scala> arr2(0)
res22: Int = 1
```

ArraySeq는 불변 → 원소를 제자리에서(in place) 바꿀 수 없음. 대신 `updated`, `appended`, `prepended` 연산이 주어진 ArraySeq와 원소 하나만 다른 새 ArraySeq를 생성.

```scala
scala> arr.updated(2, 4)
res26: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 4)
scala> arr
res27: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3)
```

위 마지막 줄처럼 `updated`를 호출해도 원래 ArraySeq인 `arr`에는 영향 없음.

ArraySeq는 원소를 비공개 [배열(Array)](https://docs.scala-lang.org/overviews/collections-2.13/arrays.html)에 저장.
- 빠른 인덱스 접근을 지원하는 조밀한(compact) 표현
- 원소 하나를 갱신·추가하는 데는 선형 시간 소요 → 배열을 새로 만들어 원래 배열의 모든 원소를 복사해야 하기 때문

### 벡터 (Vectors)

앞 절에서 보았듯 `List`와 `ArraySeq`는 특정 사용 사례에서는 효율적이지만 다른 사용 사례에서는 비효율적.
- 원소 앞에 붙이기: `List`는 상수 시간, `ArraySeq`는 선형 시간
- 인덱스 접근: `ArraySeq`는 상수 시간, `List`는 선형 시간

[Vector](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Vector.html) — 모든 연산에서 좋은 성능을 내는 컬렉션 타입.
- 시퀀스의 어떤 원소든 "사실상"(effectively) 상수 시간에 접근 가능
  - List 머리 접근이나 ArraySeq 원소 읽기보다는 큰 상수지만 그래도 상수
- 벡터를 사용하는 알고리즘은 시퀀스 머리만 접근하도록 조심할 필요 없음 → 임의 위치의 원소에 접근·수정 가능해 편하게 작성 가능

벡터는 다른 시퀀스와 똑같은 방식으로 만들고 수정.

```scala
scala> val vec = scala.collection.immutable.Vector.empty
vec: scala.collection.immutable.Vector[Nothing] = Vector()
scala> val vec2 = vec :+ 1 :+ 2
vec2: scala.collection.immutable.Vector[Int] = Vector(1, 2)
scala> val vec3 = 100 +: vec2
vec3: scala.collection.immutable.Vector[Int] = Vector(100, 1, 2)
scala> vec3(0)
res1: Int = 100
```

벡터는 분기 계수(branching factor, 트리·그래프에서 각 노드가 가진 자식의 수)가 큰 트리로 표현.
- 구현 세부 방식은 Scala 2.13.2에서 [변경됨](https://github.com/scala/scala/pull/8534)이나 기본 아이디어는 동일

- 모든 트리 노드는 벡터 원소를 최대 32개 담거나, 다른 트리 노드를 최대 32개 담음
- 원소 32개 이하인 벡터 → 노드 하나로 표현 가능
- 원소 `32 * 32 = 1024`개 이하인 벡터 → 한 번의 간접 참조(indirection)로 표현 가능
- 트리의 루트에서 최종 원소 노드까지
  - 두 번 이동 → 원소 2<sup>15</sup>개 이하 처리
  - 세 번 이동 → 2<sup>20</sup>개
  - 네 번 이동 → 2<sup>25</sup>개
  - 다섯 번 이동 → 2<sup>30</sup>개 이하 처리
- 합리적인 크기의 모든 벡터에서 원소 선택은 최대 5번의 기본 배열 선택으로 끝남 → "사실상 상수 시간"의 의미

선택과 마찬가지로 함수형 벡터 갱신도 "사실상 상수 시간".
- 벡터 중간의 원소를 갱신하려면 그 원소를 담은 노드와, 트리의 루트에서 시작해 그 노드를 가리키는 모든 노드를 복사
- 함수형 갱신 한 번은 각각 최대 32개의 원소·하위 트리를 담은 노드를 1개에서 5개 사이로 새로 생성
- 가변(mutable) 배열의 제자리 갱신보다는 확실히 비싸지만, 벡터 전체 복사보다는 훨씬 저렴

주의 — "사실상 상수 시간"과 이론적 O(1)의 차이
- 엄밀히는 O(log₃₂ n) → 밑이 32인 로그는 매우 천천히 자라 현실적인 크기(수십억 개)에서도 5를 넘지 않음 → "상수나 다름없다"고 표현
- 인덱스 접근이 정말로 O(1)인 배열/ArraySeq보다는 느림 → 극단적인 성능이 필요한 안쪽 루프에서는 이 차이가 체감될 수 있음

벡터는 빠른 임의 선택과 빠른 임의 함수형 갱신 사이에서 좋은 균형 → 현재 불변 인덱스 시퀀스(immutable indexed sequence)의 기본 구현.

```scala
scala> collection.immutable.IndexedSeq(1, 2, 3)
res2: scala.collection.immutable.IndexedSeq[Int] = Vector(1, 2, 3)
```

### 불변 큐 (Immutable Queues)

[Queue](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Queue.html) — 선입선출(first-in-first-out, FIFO) 시퀀스.
- `enqueue`로 큐에 원소 삽입, `dequeue`로 원소 제거
- 두 연산 모두 상수 시간

빈 불변 큐 생성.

```scala
scala> val empty = scala.collection.immutable.Queue[Int]()
empty: scala.collection.immutable.Queue[Int] = Queue()
```

`enqueue`로 불변 큐에 원소 추가.

```scala
scala> val has1 = empty.enqueue(1)
has1: scala.collection.immutable.Queue[Int] = Queue(1)
```

큐에 여러 원소를 추가하려면 컬렉션을 인자로 `enqueueAll` 호출.

```scala
scala> val has123 = has1.enqueueAll(List(2, 3))
has123: scala.collection.immutable.Queue[Int]
    = Queue(1, 2, 3)
```

큐 머리에서 원소를 제거하려면 `dequeue` 사용.

```scala
scala> val (element, has23) = has123.dequeue
element: Int = 1
has23: scala.collection.immutable.Queue[Int] = Queue(2, 3)
```

`dequeue`는 제거된 원소와 나머지 큐로 이루어진 쌍(pair) 반환.
- 가변 큐라면 큐 자신을 고치고 원소만 돌려주면 되지만, 불변 큐는 자신을 고칠 수 없음 → "꺼내고 남은 새 큐"를 함께 반환해야 함

### 범위 (Ranges)

[Range](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Range.html) — 일정한 간격으로 떨어진 정수들의 순서 있는 시퀀스.
- 예: "1, 2, 3"·"5, 8, 11, 14" 모두 범위
- Scala에서 범위를 만들려면 미리 정의된 메서드 `to`와 `by` 사용

```scala
scala> 1 to 3
res2: scala.collection.immutable.Range.Inclusive = Range(1, 2, 3)
scala> 5 to 14 by 3
res3: scala.collection.immutable.Range = Range(5, 8, 11, 14)
```

상한을 포함하지 않는 범위를 만들려면 `to` 대신 편의 메서드 `until` 사용.

```scala
scala> 1 until 3
res2: scala.collection.immutable.Range = Range(1, 2)
```

범위는 상수 공간(constant space)으로 표현 → 시작값·끝값·증가값이라는 숫자 세 개만으로 정의 가능 → 범위에 대한 대부분의 연산이 매우 빠름.

### 압축 해시-배열 매핑 접두사 트리 (Compressed Hash-Array Mapped Prefix-trees)

해시 [트라이(trie)](https://en.wikipedia.org/wiki/Trie) — 불변 집합과 맵을 효율적으로 구현하는 표준적인 방법.
- [압축 해시-배열 매핑 접두사 트리](https://github.com/msteindorfer/oopsla15-artifact/) — JVM에서 해시 트라이의 지역성(locality) 개선, 트리가 항상 정규적(canonical)이고 조밀한 표현을 유지하도록 만든 설계
- [immutable.HashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/HashMap.html) 클래스가 지원
- 이 자료 구조의 표현은 벡터와 비슷하게 모든 노드가 32개의 원소 또는 32개의 하위 트리를 가짐 — 다만 키 선택이 해시 코드에 기반한다는 점이 다름
  - 맵에서 주어진 키를 찾을 때: 먼저 키의 해시 코드를 구함 → 해시 코드의 하위 5비트로 첫 번째 하위 트리 선택 → 그다음 5비트로 그다음 하위 트리 선택 → 반복
  - 어떤 노드에 저장된 모든 원소의 해시 코드가, 그 수준까지 선택에 사용된 비트에서 서로 달라지면 선택 중단

해시 트라이는 충분히 빠른 조회와 충분히 효율적인 함수형 삽입(`+`)·삭제(`-`) 사이에서 좋은 균형 → Scala의 불변 맵·불변 집합 기본 구현이 해시 트라이 기반.
- Scala에는 원소가 5개 미만인 불변 집합·맵을 위한 추가 최적화 존재
  - 원소 1개~4개인 집합·맵: 원소(맵의 경우 키/값 쌍)를 필드로 직접 담은 단일 객체로 저장
  - 빈 불변 집합·빈 불변 맵: 각각 단 하나의 객체 → 영원히 비어 있을 것이므로 저장 공간을 중복해서 만들 필요 없음

참고 — 트라이(trie)의 의미
- "retrieval"에서 온 말 → 키를 통째로 비교하는 대신 키를 조각(여기서는 해시 코드의 5비트 묶음)으로 나누어 트리의 각 층에서 한 조각씩 따라 내려가며 찾는 트리 구조
- 5비트 → 2⁵ = 32가지 값 → 각 노드가 32갈래로 갈라짐
- `Map("a" -> 1)`·`Set(1, 2, 3)`처럼 평범하게 불변 맵/집합을 만들 때 내부에서 실제로 쓰이는 구조

### 레드-블랙 트리 (Red-Black Trees)

레드-블랙 트리 — 일부 노드를 "레드", 나머지 노드를 "블랙"으로 지정하는 균형 이진 트리(balanced binary tree)의 한 형태.
- 다른 균형 이진 트리와 마찬가지로 레드-블랙 트리의 연산은 트리 크기의 로그(logarithmic) 시간 안에 안정적으로 완료

Scala는 내부적으로 레드-블랙 트리를 사용하는 불변 집합·맵 구현 제공. [TreeSet](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/TreeSet.html)과 [TreeMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/TreeMap.html)으로 접근 가능.

```scala
scala> scala.collection.immutable.TreeSet.empty[Int]
res11: scala.collection.immutable.TreeSet[Int] = TreeSet()
scala> res11 + 1 + 3 + 3
res12: scala.collection.immutable.TreeSet[Int] = TreeSet(1, 3)
```

레드-블랙 트리는 Scala에서 `SortedSet`의 표준 구현 → 모든 원소를 정렬된 순서로 반환하는 효율적인 이터레이터(iterator) 제공하기 때문.

### 불변 비트 집합 (Immutable BitSets)

[BitSet](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/BitSet.html) — 작은 정수들의 컬렉션을 더 큰 정수의 비트들로 표현.
- 예: 3, 2, 0을 담은 비트 집합은 이진수로 1101, 십진수로 13인 정수로 표현

내부적으로 비트 집합은 64비트 `Long`의 배열 사용.
- 배열의 첫 번째 `Long`은 정수 0~63, 두 번째는 64~127을 담당하는 식
- 집합에 든 가장 큰 정수가 수백 정도보다 작으면 비트 집합은 매우 조밀

비트 집합에 대한 연산은 매우 빠름.
- 포함 여부 검사: 상수 시간
- 원소 추가: 비트 집합 배열의 `Long` 개수에 비례하는 시간(보통 작은 수)

간단한 사용 예.

```scala
scala> val bits = scala.collection.immutable.BitSet.empty
bits: scala.collection.immutable.BitSet = BitSet()
scala> val moreBits = bits + 3 + 4 + 4
moreBits: scala.collection.immutable.BitSet = BitSet(3, 4)
scala> moreBits(3)
res26: Boolean = true
scala> moreBits(0)
res27: Boolean = false
```

### VectorMap (VectorMaps)

[VectorMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/VectorMap.html) — 키의 `Vector`와 `HashMap`을 함께 사용해 맵을 표현.
- 모든 항목을 삽입 순서대로 반환하는 이터레이터 제공

```scala
scala> val vm = scala.collection.immutable.VectorMap.empty[Int, String]
vm: scala.collection.immutable.VectorMap[Int,String] =
  VectorMap()
scala> val vm1 = vm + (1 -> "one")
vm1: scala.collection.immutable.VectorMap[Int,String] =
  VectorMap(1 -> one)
scala> val vm2 = vm1 + (2 -> "two")
vm2: scala.collection.immutable.VectorMap[Int,String] =
  VectorMap(1 -> one, 2 -> two)
scala> vm2 == Map(2 -> "two", 1 -> "one")
res29: Boolean = true
```

- 처음 몇 줄: `VectorMap`의 내용이 삽입 순서를 유지함을 보여줌
- 마지막 줄: `VectorMap`이 다른 `Map`과 비교 가능하며 이 비교가 원소의 순서를 고려하지 않음을 보여줌

### ListMap (ListMaps)

[ListMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/ListMap.html) — 맵을 키-값 쌍의 연결 리스트(linked list)로 표현.
- 리스트 맵에 대한 연산은 리스트 전체를 순회해야 할 수도 있음 → 맵 크기에 비례하는 선형 시간 소요
- Scala에서 리스트 맵이 쓰일 일은 거의 없음 → 표준 불변 맵이 거의 항상 더 빠르기 때문
- 유일한 예외: 어떤 이유로 리스트 앞쪽 원소들이 다른 원소들보다 훨씬 자주 선택되도록 맵이 구성되는 상황

```scala
scala> val map = scala.collection.immutable.ListMap(1->"one", 2->"two")
map: scala.collection.immutable.ListMap[Int,java.lang.String] =
    Map(1 -> one, 2 -> two)
scala> map(2)
res30: String = "two"
```

주의 — "삽입 순서가 필요하면 ListMap"은 오해
- ListMap은 성능 때문에 실무에서 권장되지 않음
- 삽입 순서를 유지하는 불변 맵이 필요하다면 위에서 소개한 `VectorMap`이 사실상의 대안
- ListMap은 아주 작은 맵이거나 앞쪽 원소만 집중적으로 조회하는 특수한 경우가 아니라면 선택할 이유가 거의 없음

---

## 구체적인 가변 컬렉션 클래스 (Concrete Mutable Collection Classes)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html>

앞서 스칼라 표준 라이브러리가 제공하는, 가장 흔히 쓰이는 불변(immutable) 컬렉션 클래스 확인. 이제 가변(mutable) 컬렉션 클래스 확인.

### 배열 버퍼 (Array Buffers)

[ArrayBuffer](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArrayBuffer.html) — 배열과 크기(size)를 함께 담음.
- 배열 버퍼의 연산 대부분은 배열과 같은 속도로 동작 → 연산이 내부 배열에 접근해 수정하기만 하면 되기 때문
- 끝에 데이터를 효율적으로 추가 가능 → 항목 하나를 덧붙이는 데 분할 상환 상수 시간(amortized constant time) 소요
- 새 항목이 항상 끝에 추가되는 방식으로 큰 컬렉션을 효율적으로 만들 때 유용

```scala
scala> val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf: scala.collection.mutable.ArrayBuffer[Int] = ArrayBuffer()
scala> buf += 1
res32: buf.type = ArrayBuffer(1)
scala> buf += 10
res33: buf.type = ArrayBuffer(1, 10)
scala> buf.toArray
res34: Array[Int] = Array(1, 10)
```

참고 — 분할 상환 상수 시간
- 내부 배열이 가득 차면 더 큰 배열을 새로 만들어 복사하는 비용이 가끔 발생
- 그 비용을 여러 번의 추가 연산에 나누어 평균 내면 한 번의 추가가 상수 시간
- 자바의 `ArrayList`, 파이썬의 `list`와 같은 원리

### 리스트 버퍼 (List Buffers)

[ListBuffer](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ListBuffer.html) — 배열 버퍼와 비슷하지만 내부적으로 배열 대신 연결 리스트(linked list) 사용.
- 버퍼를 다 만든 뒤 리스트로 변환할 계획이라면 배열 버퍼 대신 리스트 버퍼 사용

```scala
scala> val buf = scala.collection.mutable.ListBuffer.empty[Int]
buf: scala.collection.mutable.ListBuffer[Int] = ListBuffer()
scala> buf += 1
res35: buf.type = ListBuffer(1)
scala> buf += 10
res36: buf.type = ListBuffer(1, 10)
scala> buf.to(List)
res37: List[Int] = List(1, 10)
```

왜 필요한가 — ListBuffer가 유용한 이유
- 스칼라의 `List`는 불변 연결 리스트 → 뒤에 요소를 하나씩 붙이며 만들면 매번 새 리스트를 만들어야 해 비효율적
- `ListBuffer`는 내부 구조가 `List`와 같음 → 요소를 다 모은 뒤 `to(List)`를 호출하면 복사 없이(상수 시간에) 리스트로 전환
- `ArrayBuffer`에서 `List`로 변환하면 전체 복사가 일어남 → "최종 목표가 `List`"라면 `ListBuffer`가 정답

### StringBuilder

배열 버퍼가 배열을 만드는 데 유용하고 리스트 버퍼가 리스트를 만드는 데 유용한 것처럼, [StringBuilder](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/StringBuilder.html)는 문자열을 만드는 데 유용.
- 워낙 흔히 쓰여 기본 네임스페이스(namespace)에 이미 임포트됨
- `new StringBuilder`로 간단히 생성

Scala 2:

```scala
scala> val buf = new StringBuilder
buf: StringBuilder =
scala> buf += 'a'
res38: buf.type = a
scala> buf ++= "bcdef"
res39: buf.type = abcdef
scala> buf.toString
res41: String = abcdef
```

Scala 3:

```scala
scala> val buf = StringBuilder()
buf: StringBuilder =
scala> buf += 'a'
res38: buf.type = a
scala> buf ++= "bcdef"
res39: buf.type = abcdef
scala> buf.toString
res41: String = abcdef
```

### ArrayDeque

[ArrayDeque](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArrayDeque.html) — 앞과 뒤 양쪽에 요소를 효율적으로 추가할 수 있는 시퀀스(sequence).
- 내부적으로 크기 조절이 가능한 배열 사용

버퍼에 요소 뒤에 덧붙이기(append)와 앞에 붙이기(prepend)가 모두 필요하면 `ArrayBuffer` 대신 `ArrayDeque` 사용.

### 큐 (Queues)

스칼라는 불변 큐에 더해 가변 큐도 제공.
- 가변 큐는 불변 큐와 비슷하게 사용하지만 `enqueue` 대신 `+=`와 `++=` 연산자로 요소 추가
- 가변 큐의 `dequeue` 메서드는 큐 머리(head) 요소를 그냥 제거하고 그 값 반환

Scala 2:

```scala
scala> val queue = new scala.collection.mutable.Queue[String]
queue: scala.collection.mutable.Queue[String] = Queue()
scala> queue += "a"
res10: queue.type = Queue(a)
scala> queue ++= List("b", "c")
res11: queue.type = Queue(a, b, c)
scala> queue
res12: scala.collection.mutable.Queue[String] = Queue(a, b, c)
scala> queue.dequeue
res13: String = a
scala> queue
res14: scala.collection.mutable.Queue[String] = Queue(b, c)
```

Scala 3:

```scala
scala> val queue = scala.collection.mutable.Queue[String]()
queue: scala.collection.mutable.Queue[String] = Queue()
scala> queue += "a"
res10: queue.type = Queue(a)
scala> queue ++= List("b", "c")
res11: queue.type = Queue(a, b, c)
scala> queue
res12: scala.collection.mutable.Queue[String] = Queue(a, b, c)
scala> queue.dequeue
res13: String = a
scala> queue
res14: scala.collection.mutable.Queue[String] = Queue(b, c)
```

주의 — "mQueue" 표기 관련
- 원문에는 "You use a `mQueue`"라고 되어 있으나 `mQueue`라는 클래스가 따로 있는 것이 아니라 가변(mutable) `Queue`를 줄여 부른 표기(원문의 오기에 가까움)
- 실제 클래스는 `scala.collection.mutable.Queue`
- 불변 큐의 `dequeue`는 "(꺼낸 값, 나머지 큐)" 쌍을 새로 반환, 가변 큐의 `dequeue`는 큐 자체를 제자리에서 수정하고 꺼낸 값만 반환 — 이것이 핵심 차이

### 스택 (Stacks)

스택 — 후입선출(last-in-first-out, LIFO) 방식으로 객체를 저장·꺼낼 수 있는 자료구조. 스칼라에서는 [mutable.Stack](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/Stack.html) 클래스가 지원.

Scala 2:

```scala
scala> val stack = new scala.collection.mutable.Stack[Int]
stack: scala.collection.mutable.Stack[Int] = Stack()
scala> stack.push(1)
res0: stack.type = Stack(1)
scala> stack
res1: scala.collection.mutable.Stack[Int] = Stack(1)
scala> stack.push(2)
res0: stack.type = Stack(1, 2)
scala> stack
res3: scala.collection.mutable.Stack[Int] = Stack(2, 1)
scala> stack.top
res8: Int = 2
scala> stack
res9: scala.collection.mutable.Stack[Int] = Stack(2, 1)
scala> stack.pop
res10: Int = 2
scala> stack
res11: scala.collection.mutable.Stack[Int] = Stack(1)
```

Scala 3:

```scala
scala> val stack = scala.collection.mutable.Stack[Int]()
stack: scala.collection.mutable.Stack[Int] = Stack()
scala> stack.push(1)
res0: stack.type = Stack(1)
scala> stack
res1: scala.collection.mutable.Stack[Int] = Stack(1)
scala> stack.push(2)
res0: stack.type = Stack(1, 2)
scala> stack
res3: scala.collection.mutable.Stack[Int] = Stack(2, 1)
scala> stack.top
res8: Int = 2
scala> stack
res9: scala.collection.mutable.Stack[Int] = Stack(2, 1)
scala> stack.pop
res10: Int = 2
scala> stack
res11: scala.collection.mutable.Stack[Int] = Stack(1)
```

### 가변 ArraySeq (Mutable ArraySeqs)

배열 시퀀스(array sequence) — 크기가 고정된 가변 시퀀스, 요소를 내부적으로 `Array[Object]`에 저장. 스칼라에서는 [ArraySeq](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArraySeq.html) 클래스로 구현됨.

배열의 성능 특성은 원하지만 요소의 타입을 알 수 없고 실행 시점에 제공할 `ClassTag`도 없는 상태에서 시퀀스의 제네릭(generic) 인스턴스를 만들고 싶을 때 보통 `ArraySeq` 사용. 이런 문제는 [배열](09_arrays.md) 절에서 설명.

### 해시 테이블 (Hash Tables)

해시 테이블 — 요소를 내부 배열에 저장, 각 항목의 해시 코드(hash code)로 결정되는 배열 내 위치에 그 항목을 배치.
- 같은 해시 코드를 가진 다른 요소가 배열에 이미 있지 않은 한, 요소 추가는 상수 시간
- 넣는 객체들의 해시 코드가 잘 분포되어 있으면 매우 빠름
- 스칼라의 기본 가변 맵·집합 타입은 해시 테이블 기반 → [mutable.HashSet](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/HashSet.html)과 [mutable.HashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/HashMap.html)으로 직접 접근 가능

해시 집합·해시 맵은 다른 집합·맵과 똑같이 사용. 간단한 예.

```scala
scala> val map = scala.collection.mutable.HashMap.empty[Int,String]
map: scala.collection.mutable.HashMap[Int,String] = Map()
scala> map += (1 -> "make a web site")
res42: map.type = Map(1 -> make a web site)
scala> map += (3 -> "profit!")
res43: map.type = Map(1 -> make a web site, 3 -> profit!)
scala> map(1)
res44: String = make a web site
scala> map contains 2
res46: Boolean = false
```

해시 테이블의 순회(iteration)는 특정한 순서를 보장하지 않음 → 내부 배열을 배열에 놓인 순서 그대로 훑을 뿐.
- 순회 순서를 보장받으려면 일반 해시 맵·집합 대신 연결 해시 맵(linked hash map)이나 연결 해시 집합 사용
- 연결 해시 맵·집합은 일반 해시 맵·집합과 같지만, 요소가 추가된 순서를 기억하는 연결 리스트를 추가로 유지한다는 점이 다름
- 이런 컬렉션의 순회는 항상 요소가 처음 추가된 순서대로 이루어짐

### 약한 해시 맵 (Weak Hash Maps)

약한 해시 맵 — 특수한 종류의 해시 맵, 가비지 컬렉터(garbage collector)가 맵에서 그 안에 저장된 키로 이어지는 참조(link)를 따라가지 않음.
- 어떤 키에 대한 다른 참조가 하나도 없으면 그 키와 연관된 값이 맵에서 사라짐
- 캐싱(caching) 같은 작업에 유용 — 비용이 큰 함수가 같은 키로 다시 호출되었을 때 이전 결과를 재사용하고 싶은 경우
  - 키와 함수 결과를 일반 해시 맵에 저장하면 맵이 한없이 커질 수 있고, 어떤 키도 가비지가 되지 못함
  - 약한 해시 맵을 쓰면 이 문제 회피 → 키 객체가 도달 불가능(unreachable)해지는 즉시 그 항목이 약한 해시 맵에서 제거됨
- 스칼라의 약한 해시 맵은 자바 구현체 `java.util.WeakHashMap`을 감싼 [WeakHashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/WeakHashMap.html) 클래스로 구현됨

참고 — 약한 참조가 필요한 이유
- 가비지 컬렉터는 "어디에서도 참조되지 않는 객체"를 메모리에서 회수
- 일반 맵에 키를 넣어 두면 맵 자체가 그 키를 참조 → 프로그램의 다른 곳에서 키를 다 잊어버려도 키는 영원히 회수되지 않음
- 약한 해시 맵은 이 참조를 "약한 참조(weak reference)"로 들고 있음 → 가비지 컬렉터가 회수 여부를 판단할 때 맵의 참조는 없는 셈으로 취급
- 캐시가 메모리를 무한정 붙들지 않게 됨

### 동시성 맵 (Concurrent Maps)

동시성 맵 — 여러 스레드(thread)가 동시에 접근 가능. 일반적인 [Map](https://www.scala-lang.org/api/2.13.18/scala/collection/Map.html) 연산에 더해 다음과 같은 원자적(atomic) 연산 제공.

#### concurrent.Map 클래스의 연산

- `m.putIfAbsent(k, v)`
  - `m`에 키 `k`가 아직 정의되어 있지 않은 경우에만 키/값 바인딩 `k -> v` 추가
- `m.remove(k, v)`
  - 키 `k`가 현재 값 `v`에 매핑되어 있는 경우에 그 항목 제거
- `m.replace(k, old, new)`
  - 키 `k`에 연관된 값이 이전에 `old`였던 경우에 그 값을 `new`로 교체
- `m.replace(k, v)`
  - 키 `k`가 이전에 어떤 값에든 바인딩되어 있던 경우에 그 값을 `v`로 교체

왜 필요한가 — 원자적 연산이 필요한 이유
- 여러 스레드가 맵을 공유할 때 "키가 없는지 확인한 뒤 넣는다"를 두 단계로 나누어 수행하면, 확인과 삽입 사이에 다른 스레드가 끼어들어 경쟁 조건(race condition) 발생
- `putIfAbsent` 같은 원자적 연산은 확인과 수정을 쪼개질 수 없는 한 동작으로 묶어 이 문제를 제거
- 락(lock)을 직접 잡지 않고도 안전하게 "확인 후 갱신" 패턴 사용 가능

`concurrent.Map`은 스칼라 컬렉션 라이브러리의 트레이트(trait). 현재 두 가지 구현체 존재.
- 자바의 `java.util.concurrent.ConcurrentMap` — [표준 자바/스칼라 컬렉션 변환](16_conversions_between_java_and_scala_collections.md)을 사용해 스칼라 맵으로 자동 변환 가능
- [TrieMap](https://www.scala-lang.org/api/2.13.18/scala/collection/concurrent/TrieMap.html) — 해시 배열 매핑 트라이(hash array mapped trie)의 락 프리(lock-free) 구현체

### 가변 비트 집합 (Mutable Bitsets)

[mutable.BitSet](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/BitSet.html) 타입의 가변 비트 집합 — 불변 비트 집합과 같지만 제자리에서(in place) 수정된다는 점이 다름.
- 갱신 시 불변 비트 집합보다 약간 더 효율적 — 바뀌지 않은 `Long` 값들을 이리저리 복사할 필요가 없기 때문

```scala
scala> val bits = scala.collection.mutable.BitSet.empty
bits: scala.collection.mutable.BitSet = BitSet()
scala> bits += 1
res49: bits.type = BitSet(1)
scala> bits += 3
res50: bits.type = BitSet(1, 3)
scala> bits
res51: scala.collection.mutable.BitSet = BitSet(1, 3)
```

주의 — 원문 표기와 동작 원리
- 이 절의 원문 첫 문장은 "A mutable bit of type ... set is..."로 어순이 뒤엉킨 오타 — 의도는 "`mutable.BitSet` 타입의 가변 비트 집합"
- 비트 집합은 음이 아닌 작은 정수들의 집합을 `Long` 배열의 비트로 표현하는 자료구조
- 불변 버전은 원소 하나를 바꿀 때마다 관련 `Long` 워드를 담은 새 배열을 만들어야 하지만, 가변 버전은 해당 비트만 제자리에서 뒤집으면 됨 — 이것이 "복사할 필요가 없다"는 의미
