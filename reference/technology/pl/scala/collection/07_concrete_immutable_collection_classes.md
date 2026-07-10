# 구체적인 불변 컬렉션 클래스 (Concrete Immutable Collection Classes)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html>

---

Scala는 여러분이 골라 쓸 수 있는 다양한 구체적인 불변(immutable) 컬렉션 클래스를 제공합니다. 이 클래스들은 구현하는 트레이트(trait)가 무엇인지(맵, 집합, 시퀀스), 무한할 수 있는지, 그리고 각종 연산의 속도가 어떤지에 따라 서로 다릅니다. 다음은 Scala에서 가장 흔히 사용되는 불변 컬렉션 타입들입니다.

## 리스트 (Lists)

[List](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/List.html)는 유한한 불변 시퀀스(sequence)입니다. 리스트는 첫 번째 원소와 나머지 리스트에 대한 상수 시간(constant-time) 접근을 제공하며, 리스트의 앞에 새 원소를 추가하는 상수 시간 콘스(cons) 연산을 가지고 있습니다. 그 밖의 많은 연산은 선형 시간(linear time)이 걸립니다.

> 📘 **처음 배우는 분께** — 콘스(cons)는 "construct"에서 온 말로, 리스트의 맨 앞에 원소 하나를 붙여 새 리스트를 만드는 연산을 뜻합니다. Scala에서는 `1 :: List(2, 3)`처럼 `::` 연산자로 씁니다. 리스트가 "머리(head) + 나머지(tail)"라는 연결 구조로 되어 있기 때문에, 맨 앞에 붙이는 것은 노드 하나만 만들면 되어 상수 시간이지만, 끝에 붙이거나 중간에 접근하려면 앞에서부터 따라가야 해서 선형 시간이 걸립니다.

## 지연 리스트 (LazyLists)

[LazyList](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/LazyList.html)는 원소가 지연(lazily) 계산된다는 점만 빼면 리스트와 같습니다. 이 덕분에 지연 리스트는 무한히 길 수 있습니다. 요청된 원소만 계산되기 때문입니다. 그 외의 성능 특성은 리스트와 동일합니다.

리스트가 `::` 연산자로 만들어지는 반면, 지연 리스트는 비슷하게 생긴 `#::`로 만들어집니다. 다음은 정수 1, 2, 3을 담은 지연 리스트의 간단한 예입니다.

```scala
scala> val lazyList = 1 #:: 2 #:: 3 #:: LazyList.empty
lazyList: scala.collection.immutable.LazyList[Int] = LazyList(<not computed>)
```

이 지연 리스트의 머리(head)는 1이고, 꼬리(tail)에는 2와 3이 있습니다. 하지만 여기서는 어떤 원소도 출력되지 않습니다. 리스트가 아직 계산되지 않았기 때문입니다! 지연 리스트는 지연 계산되도록 명세되어 있으며, 지연 리스트의 `toString` 메서드는 불필요한 추가 평가를 강제하지 않도록 주의 깊게 만들어져 있습니다.

아래는 좀 더 복잡한 예입니다. 주어진 두 수로 시작하는 피보나치 수열(Fibonacci sequence)을 담은 지연 리스트를 계산합니다. 피보나치 수열이란 각 원소가 바로 앞 두 원소의 합인 수열입니다.

```scala
scala> def fibFrom(a: Int, b: Int): LazyList[Int] = a #:: fibFrom(b, a + b)
fibFrom: (a: Int,b: Int)LazyList[Int]
```

이 함수는 보기보다 단순하지 않습니다. 수열의 첫 원소는 분명히 `a`이고, 나머지 수열은 `b`와 `a + b`로 시작하는 피보나치 수열입니다. 까다로운 부분은 무한 재귀(infinite recursion)를 일으키지 않고 이 수열을 계산하는 것입니다. 만약 이 함수가 `#::` 대신 `::`를 사용했다면, 함수를 호출할 때마다 또 다른 호출이 일어나 무한 재귀에 빠졌을 것입니다. 하지만 `#::`를 사용하기 때문에 우변은 요청되기 전까지 평가되지 않습니다. 다음은 1 두 개로 시작하는 피보나치 수열의 처음 몇 개 원소입니다.

```scala
scala> val fibs = fibFrom(1, 1).take(7)
fibs: scala.collection.immutable.LazyList[Int] = LazyList(<not computed>)
scala> fibs.toList
res9: List[Int] = List(1, 1, 2, 3, 5, 8, 13)
```

> 💡 **왜 필요한가** — "무한한 컬렉션"은 언뜻 쓸모없어 보이지만, 실제로는 "얼마나 필요할지 미리 알 수 없는 데이터"를 자연스럽게 표현하는 도구입니다. 위 예처럼 수열의 생성 규칙만 정의해 두고, 실제로 몇 개가 필요한지는 사용하는 쪽에서 `take(7)`처럼 나중에 결정할 수 있습니다. 생성 로직과 소비 로직이 분리되는 것입니다. 참고로 예전 Scala 버전에 있던 `Stream`이 이 역할을 했는데, 2.13부터는 머리까지 완전히 지연 계산되는 `LazyList`로 대체되었습니다.

## 불변 ArraySeq (Immutable ArraySeqs)

리스트는 알고리즘이 머리 부분만 조심스럽게 처리하는 경우에 매우 효율적입니다. 리스트의 머리에 접근하고, 추가하고, 제거하는 것은 상수 시간밖에 걸리지 않지만, 리스트의 뒤쪽 원소에 접근하거나 수정하는 것은 리스트 안으로 들어간 깊이에 비례하는 선형 시간이 걸립니다.

[ArraySeq](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/ArraySeq.html)는 리스트의 임의 접근(random access) 비효율을 해결하기 위해 Scala 2.13에서 도입된 컬렉션 타입입니다. ArraySeq는 컬렉션의 어떤 원소든 상수 시간에 접근할 수 있게 해 줍니다. 그 결과, ArraySeq를 사용하는 알고리즘은 컬렉션의 머리만 접근하도록 조심할 필요가 없습니다. 임의의 위치에 있는 원소에 접근할 수 있으므로 훨씬 편하게 작성할 수 있습니다.

ArraySeq는 다른 시퀀스와 똑같은 방식으로 만들고 갱신합니다.

```scala
scala> val arr = scala.collection.immutable.ArraySeq(1, 2, 3)
arr: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3)
scala> val arr2 = arr :+ 4
arr2: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3, 4)
scala> arr2(0)
res22: Int = 1
```

ArraySeq는 불변이므로 원소를 제자리에서(in place) 바꿀 수 없습니다. 그 대신 `updated`, `appended`, `prepended` 연산이 주어진 ArraySeq와 원소 하나만 다른 새 ArraySeq를 만들어 냅니다.

```scala
scala> arr.updated(2, 4)
res26: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 4)
scala> arr
res27: scala.collection.immutable.ArraySeq[Int] = ArraySeq(1, 2, 3)
```

위의 마지막 줄이 보여 주듯이, `updated`를 호출해도 원래 ArraySeq인 `arr`에는 아무 영향이 없습니다.

ArraySeq는 원소를 비공개 [배열(Array)](https://docs.scala-lang.org/overviews/collections-2.13/arrays.html)에 저장합니다. 이는 빠른 인덱스 접근을 지원하는 조밀한(compact) 표현이지만, 원소 하나를 갱신하거나 추가하는 데는 선형 시간이 걸립니다. 배열을 새로 만들어 원래 배열의 모든 원소를 복사해야 하기 때문입니다.

## 벡터 (Vectors)

앞 절들에서 보았듯이 `List`와 `ArraySeq`는 특정 사용 사례에서는 효율적인 자료 구조이지만, 다른 사용 사례에서는 비효율적입니다. 예를 들어 원소를 앞에 붙이는 것은 `List`에서는 상수 시간이지만 `ArraySeq`에서는 선형 시간이고, 반대로 인덱스 접근은 `ArraySeq`에서는 상수 시간이지만 `List`에서는 선형 시간입니다.

[Vector](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Vector.html)는 모든 연산에서 좋은 성능을 내는 컬렉션 타입입니다. 벡터는 시퀀스의 어떤 원소든 "사실상"(effectively) 상수 시간에 접근할 수 있게 해 줍니다. List의 머리에 접근하거나 ArraySeq의 원소를 읽는 것보다는 큰 상수이지만, 그래도 상수는 상수입니다. 그 결과, 벡터를 사용하는 알고리즘은 시퀀스의 머리만 접근하도록 조심할 필요가 없습니다. 임의의 위치에 있는 원소에 접근하고 수정할 수 있으므로 훨씬 편하게 작성할 수 있습니다.

벡터는 다른 시퀀스와 똑같은 방식으로 만들고 수정합니다.

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

벡터는 분기 계수(branching factor)가 큰 트리로 표현됩니다(트리나 그래프의 분기 계수란 각 노드가 가진 자식의 수를 말합니다). 이를 구현하는 세부 방식은 Scala 2.13.2에서 [변경되었지만](https://github.com/scala/scala/pull/8534), 기본 아이디어는 다음과 같이 그대로입니다.

모든 트리 노드는 벡터의 원소를 최대 32개 담거나, 다른 트리 노드를 최대 32개 담습니다. 원소가 32개 이하인 벡터는 노드 하나로 표현할 수 있습니다. 원소가 `32 * 32 = 1024`개 이하인 벡터는 한 번의 간접 참조(indirection)로 표현할 수 있습니다. 트리의 루트에서 최종 원소 노드까지 두 번만 이동하면 원소가 2<sup>15</sup>개 이하인 벡터를 다룰 수 있고, 세 번 이동하면 2<sup>20</sup>개, 네 번 이동하면 2<sup>25</sup>개, 다섯 번 이동하면 2<sup>30</sup>개 이하인 벡터를 다룰 수 있습니다. 따라서 합리적인 크기의 모든 벡터에서 원소 선택은 최대 5번의 기본 배열 선택으로 끝납니다. 원소 접근이 "사실상 상수 시간"이라고 쓴 것은 바로 이런 의미였습니다.

선택과 마찬가지로, 함수형 벡터 갱신도 "사실상 상수 시간"입니다. 벡터 중간의 원소를 갱신하려면 그 원소를 담은 노드와, 트리의 루트에서 시작해 그 노드를 가리키는 모든 노드를 복사하면 됩니다. 즉 함수형 갱신 한 번은 각각 최대 32개의 원소나 하위 트리를 담은 노드를 1개에서 5개 사이로 새로 만듭니다. 이는 가변(mutable) 배열의 제자리 갱신보다는 확실히 비싸지만, 벡터 전체를 복사하는 것보다는 훨씬 쌉니다.

> ⚠️ **짚고 넘어가기** — "사실상 상수 시간"(effectively constant time)은 이론적인 상수 시간 O(1)과는 다릅니다. 엄밀히는 O(log₃₂ n)인데, 밑이 32인 로그는 매우 천천히 자라서 현실적인 크기(수십억 개)에서도 5를 넘지 않기 때문에 "상수나 다름없다"고 표현하는 것입니다. 인덱스 접근이 정말로 O(1)인 배열/ArraySeq보다는 느리므로, 극단적인 성능이 필요한 안쪽 루프에서는 이 차이가 체감될 수 있습니다.

벡터는 빠른 임의 선택과 빠른 임의 함수형 갱신 사이에서 좋은 균형을 이루기 때문에, 현재 불변 인덱스 시퀀스(immutable indexed sequence)의 기본 구현입니다.

```scala
scala> collection.immutable.IndexedSeq(1, 2, 3)
res2: scala.collection.immutable.IndexedSeq[Int] = Vector(1, 2, 3)
```

## 불변 큐 (Immutable Queues)

[Queue](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Queue.html)는 선입선출(first-in-first-out, FIFO) 시퀀스입니다. `enqueue`로 큐에 원소를 넣고, `dequeue`로 원소를 꺼냅니다. 이 연산들은 상수 시간입니다.

빈 불변 큐는 다음과 같이 만들 수 있습니다.

```scala
scala> val empty = scala.collection.immutable.Queue[Int]()
empty: scala.collection.immutable.Queue[Int] = Queue()
```

`enqueue`로 불변 큐에 원소를 추가할 수 있습니다.

```scala
scala> val has1 = empty.enqueue(1)
has1: scala.collection.immutable.Queue[Int] = Queue(1)
```

큐에 여러 원소를 추가하려면 컬렉션을 인자로 하여 `enqueueAll`을 호출합니다.

```scala
scala> val has123 = has1.enqueueAll(List(2, 3))
has123: scala.collection.immutable.Queue[Int]
    = Queue(1, 2, 3)
```

큐의 머리에서 원소를 제거하려면 `dequeue`를 사용합니다.

```scala
scala> val (element, has23) = has123.dequeue
element: Int = 1
has23: scala.collection.immutable.Queue[Int] = Queue(2, 3)
```

`dequeue`는 제거된 원소와 나머지 큐로 이루어진 쌍(pair)을 반환한다는 점에 유의하세요. 가변 큐라면 큐 자신을 고치고 원소만 돌려주면 되지만, 불변 큐는 자신을 고칠 수 없으므로 "꺼내고 남은 새 큐"를 함께 돌려주어야 하기 때문입니다.

## 범위 (Ranges)

[Range](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Range.html)는 일정한 간격으로 떨어진 정수들의 순서 있는 시퀀스입니다. 예를 들어 "1, 2, 3"도 범위이고 "5, 8, 11, 14"도 범위입니다. Scala에서 범위를 만들려면 미리 정의된 메서드 `to`와 `by`를 사용합니다.

```scala
scala> 1 to 3
res2: scala.collection.immutable.Range.Inclusive = Range(1, 2, 3)
scala> 5 to 14 by 3
res3: scala.collection.immutable.Range = Range(5, 8, 11, 14)
```

상한을 포함하지 않는 범위를 만들고 싶다면 `to` 대신 편의 메서드 `until`을 사용합니다.

```scala
scala> 1 until 3
res2: scala.collection.immutable.Range = Range(1, 2)
```

범위는 상수 공간(constant space)으로 표현됩니다. 시작값, 끝값, 증가값이라는 숫자 세 개만으로 정의할 수 있기 때문입니다. 이런 표현 덕분에 범위에 대한 대부분의 연산은 매우 빠릅니다.

## 압축 해시-배열 매핑 접두사 트리 (Compressed Hash-Array Mapped Prefix-trees)

해시 [트라이(trie)](https://en.wikipedia.org/wiki/Trie)는 불변 집합과 맵을 효율적으로 구현하는 표준적인 방법입니다. [압축 해시-배열 매핑 접두사 트리](https://github.com/msteindorfer/oopsla15-artifact/)는 JVM에서 해시 트라이의 지역성(locality)을 개선하고 트리가 항상 정규적(canonical)이고 조밀한 표현을 유지하도록 만든 설계입니다. 이는 [immutable.HashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/HashMap.html) 클래스가 지원합니다. 이 자료 구조의 표현은 벡터와 비슷하게 모든 노드가 32개의 원소 또는 32개의 하위 트리를 갖는 트리입니다. 다만 키 선택이 해시 코드에 기반해 이루어진다는 점이 다릅니다. 예를 들어 맵에서 주어진 키를 찾으려면 먼저 그 키의 해시 코드를 구합니다. 그런 다음 해시 코드의 하위 5비트로 첫 번째 하위 트리를 선택하고, 그다음 5비트로 그다음 하위 트리를 선택하는 식으로 진행합니다. 어떤 노드에 저장된 모든 원소의 해시 코드가, 그 수준까지 선택에 사용된 비트에서 서로 달라지면 선택이 멈춥니다.

해시 트라이는 충분히 빠른 조회와 충분히 효율적인 함수형 삽입(`+`) 및 삭제(`-`) 사이에서 좋은 균형을 이룹니다. 그래서 Scala의 불변 맵과 불변 집합의 기본 구현이 해시 트라이에 기반해 있습니다. 사실 Scala에는 원소가 5개 미만인 불변 집합과 맵을 위한 추가 최적화가 있습니다. 원소가 1개에서 4개인 집합과 맵은 원소(맵의 경우 키/값 쌍)를 필드로 직접 담은 단일 객체로 저장됩니다. 빈 불변 집합과 빈 불변 맵은 각각 단 하나의 객체입니다. 빈 불변 집합이나 맵은 영원히 비어 있을 것이므로 저장 공간을 중복해서 만들 필요가 없기 때문입니다.

> 📘 **처음 배우는 분께** — 트라이(trie)는 "retrieval"에서 온 말로, 키를 통째로 비교하는 대신 키를 조각(여기서는 해시 코드의 5비트 묶음)으로 나누어 트리의 각 층에서 한 조각씩 따라 내려가며 찾는 트리 구조입니다. 5비트면 2⁵ = 32가지 값이 나오므로 각 노드가 32갈래로 갈라지는 것입니다. 여러분이 `Map("a" -> 1)`이나 `Set(1, 2, 3)`처럼 평범하게 불변 맵/집합을 만들 때 내부에서 실제로 쓰이는 구조가 바로 이것입니다.

## 레드-블랙 트리 (Red-Black Trees)

레드-블랙 트리는 일부 노드를 "레드", 나머지 노드를 "블랙"으로 지정하는 균형 이진 트리(balanced binary tree)의 한 형태입니다. 다른 균형 이진 트리와 마찬가지로, 레드-블랙 트리의 연산은 트리 크기의 로그(logarithmic) 시간 안에 안정적으로 완료됩니다.

Scala는 내부적으로 레드-블랙 트리를 사용하는 불변 집합과 맵의 구현을 제공합니다. [TreeSet](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/TreeSet.html)과 [TreeMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/TreeMap.html)이라는 이름으로 접근할 수 있습니다.

```scala
scala> scala.collection.immutable.TreeSet.empty[Int]
res11: scala.collection.immutable.TreeSet[Int] = TreeSet()
scala> res11 + 1 + 3 + 3
res12: scala.collection.immutable.TreeSet[Int] = TreeSet(1, 3)
```

레드-블랙 트리는 Scala에서 `SortedSet`의 표준 구현입니다. 모든 원소를 정렬된 순서로 반환하는 효율적인 이터레이터(iterator)를 제공하기 때문입니다.

## 불변 비트 집합 (Immutable BitSets)

[BitSet](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/BitSet.html)은 작은 정수들의 컬렉션을 더 큰 정수의 비트들로 표현합니다. 예를 들어 3, 2, 0을 담은 비트 집합은 이진수로 1101, 십진수로 13인 정수로 표현됩니다.

내부적으로 비트 집합은 64비트 `Long`의 배열을 사용합니다. 배열의 첫 번째 `Long`은 정수 0부터 63까지를, 두 번째는 64부터 127까지를 담당하는 식입니다. 따라서 집합에 든 가장 큰 정수가 수백 정도보다 작기만 하면 비트 집합은 매우 조밀합니다.

비트 집합에 대한 연산은 매우 빠릅니다. 포함 여부 검사는 상수 시간이 걸립니다. 집합에 원소를 추가하는 것은 비트 집합 배열의 `Long` 개수에 비례하는 시간이 걸리는데, 이 개수는 보통 작은 수입니다. 다음은 비트 집합 사용의 간단한 예입니다.

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

## VectorMap (VectorMaps)

[VectorMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/VectorMap.html)은 키의 `Vector`와 `HashMap`을 함께 사용해 맵을 표현합니다. 모든 항목을 삽입 순서대로 반환하는 이터레이터를 제공합니다.

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

처음 몇 줄은 `VectorMap`의 내용이 삽입 순서를 유지한다는 것을 보여 주고, 마지막 줄은 `VectorMap`이 다른 `Map`과 비교 가능하며 이 비교가 원소의 순서를 고려하지 않는다는 것을 보여 줍니다.

## ListMap (ListMaps)

[ListMap](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/ListMap.html)은 맵을 키-값 쌍의 연결 리스트(linked list)로 표현합니다. 일반적으로 리스트 맵에 대한 연산은 리스트 전체를 순회해야 할 수도 있습니다. 따라서 리스트 맵의 연산은 맵 크기에 비례하는 선형 시간이 걸립니다. 사실 Scala에서 리스트 맵이 쓰일 일은 거의 없습니다. 표준 불변 맵이 거의 항상 더 빠르기 때문입니다. 유일하게 예외가 될 수 있는 경우는, 어떤 이유로 리스트의 앞쪽 원소들이 다른 원소들보다 훨씬 자주 선택되도록 맵이 구성되는 상황뿐입니다.

```scala
scala> val map = scala.collection.immutable.ListMap(1->"one", 2->"two")
map: scala.collection.immutable.ListMap[Int,java.lang.String] =
    Map(1 -> one, 2 -> two)
scala> map(2)
res30: String = "two"
```

> ⚠️ **짚고 넘어가기** — "삽입 순서가 필요하면 ListMap"이라고 오해하기 쉽지만, 원문이 말하듯 ListMap은 성능 때문에 실무에서 권장되지 않습니다. 삽입 순서를 유지하는 불변 맵이 필요하다면 바로 위에서 소개한 `VectorMap`이 사실상의 대안입니다. ListMap은 아주 작은 맵이거나 앞쪽 원소만 집중적으로 조회하는 특수한 경우가 아니라면 선택할 이유가 거의 없습니다.
