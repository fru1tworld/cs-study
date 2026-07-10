# 구체적인 가변 컬렉션 클래스 (Concrete Mutable Collection Classes)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/concrete-mutable-collection-classes.html>

지금까지 스칼라가 표준 라이브러리에서 제공하는, 가장 흔히 쓰이는 불변(immutable) 컬렉션 클래스들을 살펴보았습니다. 이제 가변(mutable) 컬렉션 클래스들을 살펴보겠습니다.

## 배열 버퍼 (Array Buffers)

[ArrayBuffer](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArrayBuffer.html)는 배열과 크기(size)를 함께 담고 있습니다. 배열 버퍼의 연산 대부분은 배열과 같은 속도로 동작하는데, 연산이 그저 내부 배열에 접근해 수정하기만 하면 되기 때문입니다. 여기에 더해, 배열 버퍼는 끝에 데이터를 효율적으로 추가할 수 있습니다. 배열 버퍼에 항목 하나를 덧붙이는 데는 분할 상환 상수 시간(amortized constant time)이 걸립니다. 따라서 새 항목이 항상 끝에 추가되는 방식으로 큰 컬렉션을 효율적으로 만들어 나갈 때 배열 버퍼가 유용합니다.

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

> 📘 **처음 배우는 분께** — "분할 상환 상수 시간"이란, 내부 배열이 가득 차면 더 큰 배열을 새로 만들어 복사하는 비용이 가끔 발생하지만, 그 비용을 여러 번의 추가 연산에 나누어 평균 내면 한 번의 추가가 상수 시간이라는 뜻입니다. 자바의 `ArrayList`, 파이썬의 `list`와 같은 원리입니다.

## 리스트 버퍼 (List Buffers)

[ListBuffer](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ListBuffer.html)는 배열 버퍼와 비슷하지만, 내부적으로 배열 대신 연결 리스트(linked list)를 사용한다는 점이 다릅니다. 버퍼를 다 만든 뒤 리스트로 변환할 계획이라면, 배열 버퍼 대신 리스트 버퍼를 사용하세요.

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

> 💡 **왜 필요한가** — 스칼라의 `List`는 불변 연결 리스트라서 뒤에 요소를 하나씩 붙이며 만들면 매번 새 리스트를 만들어야 해 비효율적입니다. `ListBuffer`는 내부 구조가 `List`와 같아서, 요소를 다 모은 뒤 `to(List)`를 호출하면 복사 없이(상수 시간에) 리스트로 바뀝니다. `ArrayBuffer`에서 `List`로 변환하면 전체 복사가 일어나므로, "최종 목표가 `List`"라면 `ListBuffer`가 정답입니다.

## StringBuilder

배열 버퍼가 배열을 만드는 데 유용하고 리스트 버퍼가 리스트를 만드는 데 유용한 것처럼, [StringBuilder](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/StringBuilder.html)는 문자열을 만드는 데 유용합니다. 문자열 빌더는 워낙 흔히 쓰이기 때문에 기본 네임스페이스(namespace)에 이미 임포트되어 있습니다. 다음처럼 간단히 `new StringBuilder`로 생성하면 됩니다.

**Scala 2:**

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

**Scala 3:**

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

## ArrayDeque

[ArrayDeque](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArrayDeque.html)는 앞과 뒤 양쪽에 요소를 효율적으로 추가할 수 있는 시퀀스(sequence)입니다. 내부적으로 크기 조절이 가능한 배열을 사용합니다.

버퍼에 요소를 뒤에 덧붙이는 것(append)과 앞에 붙이는 것(prepend)이 모두 필요하다면, `ArrayBuffer` 대신 `ArrayDeque`를 사용하세요.

## 큐 (Queues)

스칼라는 불변 큐에 더해 가변 큐도 제공합니다. 가변 큐는 불변 큐와 비슷하게 사용하지만, `enqueue` 대신 `+=`와 `++=` 연산자로 요소를 덧붙입니다. 또한 가변 큐에서 `dequeue` 메서드는 큐의 머리(head) 요소를 그냥 제거하고 그 값을 반환합니다. 예를 들면 다음과 같습니다.

**Scala 2:**

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

**Scala 3:**

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

> ⚠️ **짚고 넘어가기** — 원문에는 "You use a `mQueue`"라고 되어 있는데, `mQueue`라는 클래스가 따로 있는 것이 아니라 가변(mutable) `Queue`를 줄여 부른 표기(사실상 원문의 오기에 가깝습니다)입니다. 실제 클래스는 `scala.collection.mutable.Queue`입니다. 또 불변 큐의 `dequeue`는 "(꺼낸 값, 나머지 큐)" 쌍을 새로 반환하는 반면, 가변 큐의 `dequeue`는 큐 자체를 제자리에서 수정하고 꺼낸 값만 반환한다는 점이 핵심 차이입니다.

## 스택 (Stacks)

스택은 후입선출(last-in-first-out, LIFO) 방식으로 객체를 저장하고 꺼낼 수 있는 자료구조를 구현합니다. 스칼라에서는 [mutable.Stack](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/Stack.html) 클래스가 이를 지원합니다.

**Scala 2:**

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

**Scala 3:**

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

## 가변 ArraySeq (Mutable ArraySeqs)

배열 시퀀스(array sequence)는 크기가 고정된 가변 시퀀스로, 요소를 내부적으로 `Array[Object]`에 저장합니다. 스칼라에서는 [ArraySeq](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/ArraySeq.html) 클래스로 구현되어 있습니다.

배열의 성능 특성은 원하지만, 요소의 타입을 알 수 없고 실행 시점에 제공할 `ClassTag`도 없는 상태에서 시퀀스의 제네릭(generic) 인스턴스를 만들고 싶을 때 보통 `ArraySeq`를 사용합니다. 이런 문제들은 [배열](09_arrays.md)에 관한 절에서 설명합니다.

## 해시 테이블 (Hash Tables)

해시 테이블은 요소를 내부 배열에 저장하는데, 각 항목의 해시 코드(hash code)로 결정되는 배열 내 위치에 그 항목을 놓습니다. 같은 해시 코드를 가진 다른 요소가 배열에 이미 있지 않은 한, 해시 테이블에 요소를 추가하는 데는 상수 시간밖에 걸리지 않습니다. 따라서 해시 테이블에 넣는 객체들의 해시 코드가 잘 분포되어 있기만 하면 해시 테이블은 매우 빠릅니다. 그런 이유로 스칼라의 기본 가변 맵과 집합 타입은 해시 테이블에 기반하고 있습니다. [mutable.HashSet](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/HashSet.html)과 [mutable.HashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/HashMap.html)이라는 이름으로 직접 접근할 수도 있습니다.

해시 집합과 해시 맵은 다른 집합이나 맵과 똑같이 사용합니다. 간단한 예를 몇 가지 보겠습니다.

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

해시 테이블의 순회(iteration)는 특정한 순서로 일어난다는 보장이 없습니다. 순회는 내부 배열을 그저 배열에 놓인 순서 그대로 훑을 뿐입니다. 순회 순서를 보장받으려면 일반 해시 맵이나 집합 대신 _연결_ 해시 맵(linked hash map)이나 연결 해시 집합을 사용하세요. 연결 해시 맵과 집합은 일반 해시 맵·집합과 같지만, 요소가 추가된 순서를 기억하는 연결 리스트를 추가로 유지한다는 점이 다릅니다. 이런 컬렉션의 순회는 항상 요소가 처음 추가된 순서대로 이루어집니다.

## 약한 해시 맵 (Weak Hash Maps)

약한 해시 맵은 특수한 종류의 해시 맵으로, 가비지 컬렉터(garbage collector)가 맵에서 그 안에 저장된 키로 이어지는 참조(link)를 따라가지 않습니다. 즉, 어떤 키에 대한 다른 참조가 하나도 없으면 그 키와 연관된 값이 맵에서 사라집니다. 약한 해시 맵은 캐싱(caching) 같은 작업에 유용합니다. 비용이 큰 함수가 같은 키로 다시 호출되었을 때 이전 결과를 재사용하고 싶은 경우가 그렇습니다. 키와 함수 결과를 일반 해시 맵에 저장하면 맵이 한없이 커질 수 있고, 어떤 키도 가비지가 되지 못합니다. 약한 해시 맵을 쓰면 이 문제를 피할 수 있습니다. 키 객체가 도달 불가능(unreachable)해지는 즉시 그 항목이 약한 해시 맵에서 제거됩니다. 스칼라의 약한 해시 맵은 자바 구현체 `java.util.WeakHashMap`을 감싼 [WeakHashMap](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/WeakHashMap.html) 클래스로 구현되어 있습니다.

> 📘 **처음 배우는 분께** — 가비지 컬렉터는 "어디에서도 참조되지 않는 객체"를 메모리에서 회수합니다. 그런데 일반 맵에 키를 넣어 두면 맵 자체가 그 키를 참조하므로, 프로그램의 다른 곳에서 키를 다 잊어버려도 키는 영원히 회수되지 않습니다. 약한 해시 맵은 이 참조를 "약한 참조(weak reference)"로 들고 있어서, 가비지 컬렉터가 회수 여부를 판단할 때 맵의 참조는 없는 셈 칩니다. 그래서 캐시가 메모리를 무한정 붙들지 않게 됩니다.

## 동시성 맵 (Concurrent Maps)

동시성 맵은 여러 스레드(thread)가 동시에 접근할 수 있습니다. 일반적인 [Map](https://www.scala-lang.org/api/2.13.18/scala/collection/Map.html) 연산에 더해, 다음과 같은 원자적(atomic) 연산을 제공합니다.

### concurrent.Map 클래스의 연산

| 연산 | 하는 일 |
| --- | --- |
| `m.putIfAbsent(k, v)` | `m`에 키 `k`가 아직 정의되어 있지 않은 경우에만 키/값 바인딩 `k -> v`를 추가합니다. |
| `m.remove(k, v)` | 키 `k`가 현재 값 `v`에 매핑되어 있는 경우에 그 항목을 제거합니다. |
| `m.replace(k, old, new)` | 키 `k`에 연관된 값이 이전에 `old`였던 경우에 그 값을 `new`로 교체합니다. |
| `m.replace(k, v)` | 키 `k`가 이전에 어떤 값에든 바인딩되어 있던 경우에 그 값을 `v`로 교체합니다. |

> 💡 **왜 필요한가** — 여러 스레드가 맵을 공유할 때 "키가 없는지 확인한 뒤 넣는다"를 두 단계로 나누어 수행하면, 확인과 삽입 사이에 다른 스레드가 끼어들어 경쟁 조건(race condition)이 생깁니다. `putIfAbsent` 같은 원자적 연산은 확인과 수정을 쪼개질 수 없는 한 동작으로 묶어 이 문제를 없애 줍니다. 락(lock)을 직접 잡지 않고도 안전하게 "확인 후 갱신" 패턴을 쓸 수 있게 해 주는 것입니다.

`concurrent.Map`은 스칼라 컬렉션 라이브러리의 트레이트(trait)입니다. 현재 두 가지 구현체가 있습니다. 첫째는 자바의 `java.util.concurrent.ConcurrentMap`으로, [표준 자바/스칼라 컬렉션 변환](16_conversions_between_java_and_scala_collections.md)을 사용해 스칼라 맵으로 자동 변환할 수 있습니다. 둘째는 [TrieMap](https://www.scala-lang.org/api/2.13.18/scala/collection/concurrent/TrieMap.html)으로, 해시 배열 매핑 트라이(hash array mapped trie)의 락 프리(lock-free) 구현체입니다.

## 가변 비트 집합 (Mutable Bitsets)

[mutable.BitSet](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/BitSet.html) 타입의 가변 비트 집합은 불변 비트 집합과 같지만, 제자리에서(in place) 수정된다는 점이 다릅니다. 가변 비트 집합은 갱신 시 불변 비트 집합보다 약간 더 효율적인데, 바뀌지 않은 `Long` 값들을 이리저리 복사할 필요가 없기 때문입니다.

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

> ⚠️ **짚고 넘어가기** — 이 절의 원문 첫 문장은 "A mutable bit of type ... set is..."로 어순이 뒤엉킨 오타인데, 의도는 "`mutable.BitSet` 타입의 가변 비트 집합"입니다. 비트 집합은 음이 아닌 작은 정수들의 집합을 `Long` 배열의 비트로 표현하는 자료구조로, 불변 버전은 원소 하나를 바꿀 때마다 관련 `Long` 워드를 담은 새 배열을 만들어야 하지만 가변 버전은 해당 비트만 제자리에서 뒤집으면 됩니다. 그것이 본문에서 말하는 "복사할 필요가 없다"의 의미입니다.
