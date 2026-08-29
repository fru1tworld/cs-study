# 뷰와 이터레이터

## 뷰 (Views)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/views.html>

- 컬렉션에는 새로운 컬렉션을 만들어 내는 메서드가 다수 존재 — `map`, `filter`, `++` 등
- 이런 메서드를 변환자(transformer)라 부름 → 적어도 하나의 컬렉션을 수신 객체(receiver object)로 받아 그 결과로 또 다른 컬렉션을 생성

변환자 구현 방식은 두 가지.

- 엄격한(strict) 방식: 변환자의 결과로 모든 요소를 갖춘 새 컬렉션을 즉시 생성
- 비엄격(non-strict)·지연(lazy) 방식: 결과 컬렉션에 대한 프록시(proxy)만 만들고, 실제 요소는 요청될 때 비로소 생성

비엄격 변환자의 예로 지연 map 연산 구현을 살펴봄.

```scala
// Scala 2
def lazyMap[T, U](iter: Iterable[T], f: T => U) = new Iterable[U] {
  def iterator = iter.iterator.map(f)
}
```

```scala
// Scala 3
def lazyMap[T, U](iter: Iterable[T], f: T => U) = new Iterable[U]:
  def iterator = iter.iterator.map(f)
```

- `lazyMap`은 주어진 컬렉션 `iter`의 모든 요소를 순회하지 않고 새 `Iterable`을 생성
- 주어진 함수 `f`는 새 컬렉션의 `iterator`에서 요소가 요청될 때 그 요소에 적용됨

참고 — 지연 평가
- 지연 평가(lazy evaluation): 계산을 미리 해 두지 않고, 결과가 실제로 필요해지는 순간까지 미루는 실행 전략
- 위 `lazyMap`이 반환하는 `Iterable`은 "나중에 요소를 달라고 하면 그때 `f`를 적용해서 주겠다"는 약속만 담음 → 호출 시점에는 계산이 일어나지 않음

- 스칼라 컬렉션은 기본적으로 모든 변환자가 엄격하게 동작
- 유일한 예외는 `LazyList` → 모든 변환자 메서드를 지연 방식으로 구현
- 어떤 컬렉션이든 지연 컬렉션으로 바꾸고 다시 되돌리는 체계적인 방법이 존재 → 컬렉션 뷰(view)에 기반
- 뷰는 어떤 기반 컬렉션(base collection)을 대표하되 모든 변환자를 지연 방식으로 구현하는 특별한 컬렉션

- 컬렉션에서 그 뷰로 전환 → `view` 메서드 사용. `xs`가 컬렉션이면 `xs.view`는 같은 컬렉션이되 모든 변환자가 지연 방식으로 구현된 것
- 뷰에서 엄격한 컬렉션으로 되돌아가려면 → 엄격한 컬렉션 팩토리(factory)를 인자로 하는 `to` 변환 연산 사용(예: `xs.view.to(List)`)

Int의 벡터에 두 함수를 연달아 map 하는 예.

```scala
// Scala 2
scala> val v = Vector(1 to 10: _*)
val v: scala.collection.immutable.Vector[Int] =
  Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

scala> v.map(_ + 1).map(_ * 2)
val res5: scala.collection.immutable.Vector[Int] =
  Vector(4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
```

```scala
// Scala 3
scala> val v = Vector((1 to 10)*)
val v: scala.collection.immutable.Vector[Int] =
  Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

scala> v.map(_ + 1).map(_ * 2)
val res5: scala.collection.immutable.Vector[Int] =
  Vector(4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
```

- 마지막 문장에서 `v map (_ + 1)`은 새 벡터를 하나 생성하고, 이 벡터는 두 번째 `map (_ * 2)` 호출로 다시 세 번째 벡터로 변환됨
- 많은 경우 첫 번째 map 호출에서 중간 결과를 만드는 것은 다소 낭비 → 두 함수 `(_ + 1)`과 `(_ * 2)`의 합성 함수로 map을 한 번만 수행하는 편이 더 빠름
- 두 함수를 같은 곳에서 쓸 수 있다면 직접 합칠 수 있으나, 자료 구조에 대한 연속적인 변환이 서로 다른 프로그램 모듈에서 이루어지는 경우가 흔함 → 그 상황에서 변환들을 하나로 합치면 모듈성(modularity)이 훼손됨
- 중간 결과를 피하는 더 일반적인 방법: 먼저 벡터를 뷰로 바꾸고, 모든 변환을 뷰에 적용한 다음, 마지막에 뷰를 벡터로 강제(force)

```scala
scala> val w = v.view.map(_ + 1).map(_ * 2).to(Vector)
val w: scala.collection.immutable.Vector[Int] =
  Vector(4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
```

이 연산 순서를 하나씩 나누어 확인.

```scala
scala> val vv = v.view
val vv: scala.collection.IndexedSeqView[Int] = IndexedSeqView(<not computed>)
```

- `v.view`를 적용하면 `IndexedSeqView[Int]`, 즉 지연 평가되는 `IndexedSeq[Int]`를 얻음
- `LazyList`와 마찬가지로 뷰의 `toString` 연산은 뷰의 요소를 강제하지 않음 → `vv`의 내용이 `IndexedSeqView(<not computed>)`로 표시됨

뷰에 첫 번째 `map`을 적용.

```scala
scala> vv.map(_ + 1)
val res13: scala.collection.IndexedSeqView[Int] = IndexedSeqView(<not computed>)
```

- `map`의 결과는 또 다른 `IndexedSeqView[Int]` 값
- 이는 본질적으로 "벡터 `v`에 함수 `(_ + 1)`로 `map`을 적용해야 한다"는 사실을 기록해 두는 래퍼(wrapper) → 뷰가 강제되기 전까지는 그 map을 실제로 적용하지 않음

마지막 결과에 두 번째 `map`을 적용.

```scala
scala> res13.map(_ * 2)
val res14: scala.collection.IndexedSeqView[Int] = IndexedSeqView(<not computed>)
```

이 결과를 강제하면 다음과 같음.

```scala
scala> res14.to(Vector)
val res15: scala.collection.immutable.Vector[Int] =
    Vector(4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
```

- 저장되어 있던 두 함수는 `to` 연산이 실행되는 과정에서 함께 적용되고, 새 벡터가 생성됨
- 중간 자료 구조가 전혀 필요 없음

왜 필요한가 — 중간 컬렉션 생성 비용
- 중간 컬렉션 생성은 단순히 "조금 느린" 문제가 아님
- 요소가 수백만 개인 컬렉션에 변환을 세 번 연결하면 → 최종 결과 하나를 얻기 위해 수백만 개짜리 임시 컬렉션을 두 번이나 만들고 버림
- 뷰는 변환 파이프라인을 "기록"만 해 두었다가 마지막에 한 번의 순회로 처리 → 코드를 모듈별로 나누어 쓰면서도 합성 함수 하나로 map 한 번만 한 것과 같은 효율을 얻음

- 일반적으로 뷰에 적용된 변환 연산은 결코 새 자료 구조를 만들지 않음
- 뷰의 요소에 접근할 때는 기저 자료 구조(underlying data structure)의 요소를 사실상 가능한 한 최소한으로만 순회
- 따라서 뷰는 다음 성질을 가짐
  - 변환자는 `O(1)` 복잡도를 가짐
  - 요소 접근 연산은 기저 자료 구조와 같은 복잡도를 가짐(예: `IndexedSeqView`에서 인덱스 접근은 상수 시간, 그 외에는 선형 시간)

- 이 규칙에는 예외가 존재 — 예를 들어 `sorted` 연산은 두 성질을 동시에 만족할 수 없음
  - 최솟값 요소를 찾으려면 기저 컬렉션 전체를 순회해야 함
  - 그 순회가 `sorted` 호출 시점에 일어나면 첫 번째 성질이 깨짐(`sorted`가 뷰에서 지연 동작하지 않게 됨)
  - 반대로 결과 뷰의 요소에 접근하는 시점에 일어나면 두 번째 성질이 깨짐
- 이런 연산에 대해서는 첫 번째 성질을 포기하기로 결정 → "항상 컬렉션 요소를 강제한다(always forcing the collection elements)"라고 문서화됨

- 뷰를 사용하는 주된 이유는 성능 — 컬렉션을 뷰로 전환하면 중간 결과 생성을 피할 수 있음(절약 효과가 상당히 클 수 있음)
- 또 다른 예: 단어 목록에서 첫 번째 회문(palindrome, 거꾸로 읽어도 같은 단어)을 찾는 문제

```scala
def isPalindrome(x: String) = x == x.reverse
def findPalindrome(s: Seq[String]) = s.find(isPalindrome)
```

- 아주 긴 단어 시퀀스 `words`에서 처음 백만 개 단어 안에서 회문을 찾고자 함
- `findPalindrome` 정의를 재사용하는 방법

```scala
val palindromes = findPalindrome(words.take(1000000))
```

- 이 코드는 "시퀀스의 처음 백만 개 단어를 취한다"와 "그 안에서 회문을 찾는다"라는 두 관심사를 깔끔하게 분리
- 단점: 시퀀스의 첫 단어가 이미 회문이라 해도, 항상 백만 개 단어로 이루어진 중간 시퀀스를 생성 → 최악의 경우 999,999개의 단어가 이후에 전혀 들여다보지도 않을 중간 결과로 복사될 수 있음
- 많은 프로그래머가 여기서 특화 버전을 따로 작성 → 뷰가 있으면 불필요

```scala
val palindromes = findPalindrome(words.view.take(1000000))
```

- 이 코드는 앞서와 똑같이 관심사를 분리하면서도, 백만 개 요소짜리 시퀀스 대신 가벼운 뷰 객체 하나만 생성
- 성능과 모듈성 사이에서 하나를 고를 필요가 없어짐

- 그렇다면 엄격한 컬렉션이 왜 있는지 질문이 생김
- 이유 1: 성능 비교에서 지연 컬렉션이 엄격한 컬렉션보다 항상 유리한 것은 아님 → 컬렉션 크기가 작을 때는 뷰에서 클로저(closure)를 만들고 적용하는 데 드는 추가 오버헤드가 중간 자료 구조를 피해서 얻는 이득보다 큰 경우가 많음
- 이유 2(더 중요): 지연된 연산에 부수 효과(side effect)가 있으면 뷰의 평가가 매우 혼란스러워질 수 있음

주의 — 뷰가 무조건 빠르다는 전제는 위험
- 뷰는 요소에 접근할 때마다 저장해 둔 함수들을 다시 적용 → 같은 뷰를 여러 번 순회하면 변환 계산도 그만큼 반복됨
- 중간 컬렉션 생성 비용이 클 때(큰 컬렉션·긴 변환 체인·일부 요소만 소비)는 뷰가 유리하지만, 작은 컬렉션이나 결과를 여러 번 재사용하는 경우에는 엄격한 컬렉션이 오히려 나음

- 2.8 이전 버전의 스칼라를 쓰던 일부 사용자들이 실제로 겪었던 예 — 그 버전들에서는 `Range` 타입이 지연 방식이어서 사실상 뷰처럼 동작
- 사람들은 이런 식으로 여러 개의 액터(actor)를 만들려 했음

```scala
// Scala 2
val actors = for (i <- 1 to 10) yield actor { ... }
```

```scala
// Scala 3
val actors = for i <- 1 to 10 yield actor { ... }
```

- 그런데 이후에 어느 액터도 실행되지 않아 당황함 — `actor` 메서드는 뒤따르는 중괄호 안의 코드로 액터를 만들어 시작시켜야 함에도 그러함
- 왜 아무 일도 일어나지 않았는지 설명 → 위 for 표현식이 map 적용과 동등하다는 사실이 핵심

```scala
val actors = (1 to 10).map(i => actor { ... })
```

- 이전 버전에서는 `(1 to 10)`이 만드는 범위(range)가 뷰처럼 동작 → map의 결과 역시 뷰
- 즉 어떤 요소도 계산되지 않았고, 그 결과 어떤 액터도 만들어지지 않음
- 표현식 전체의 범위를 강제했다면 액터가 만들어졌겠으나, 액터를 일하게 만들기 위해 그렇게 해야 한다는 것은 직관적이지 않음

- 이런 뜻밖의 상황을 피하기 위해, 현재의 스칼라 컬렉션 라이브러리는 더 일관된 규칙을 따름
- 지연 리스트(lazy list)와 뷰를 제외한 모든 컬렉션은 엄격
- 엄격한 컬렉션에서 지연 컬렉션으로 가는 유일한 방법은 `view` 메서드, 되돌아오는 유일한 방법은 `to`
- 따라서 위 `actors` 정의는 이제 기대한 대로 동작 → 액터 10개를 만들고 시작
- 예전의 그 뜻밖의 동작을 다시 얻으려면 명시적으로 `view` 메서드 호출을 추가해야 함

```scala
// Scala 2
val actors = for (i <- (1 to 10).view) yield actor { ... }
```

```scala
// Scala 3
val actors = for i <- (1 to 10).view yield actor { ... }
```

- 요약: 뷰는 효율성에 대한 관심사와 모듈성에 대한 관심사를 조화시키는 강력한 도구
- 다만 지연 평가의 여러 측면에 얽혀 들지 않으려면, 컬렉션 변환에 부수 효과가 없는 순수 함수형(purely functional) 코드에서만 뷰를 사용하도록 제한하는 것이 좋음
- 가장 피해야 할 것: 새 컬렉션을 만들면서 동시에 부수 효과까지 갖는 연산과 뷰를 뒤섞어 쓰는 것

---

## 이터레이터 (Iterators)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/iterators.html>

- 이터레이터(iterator)는 컬렉션이 아니라, 컬렉션의 원소에 하나씩 접근하는 방법
- 이터레이터 `it`의 두 가지 기본 연산은 `next`와 `hasNext`
  - `it.next()`를 호출하면 이터레이터의 다음 원소를 반환하면서 이터레이터의 상태를 앞으로 전진
  - 같은 이터레이터에서 `next`를 다시 호출하면, 이전에 반환된 원소의 바로 다음 원소를 내어 줌
  - 더 이상 반환할 원소가 없으면 `next` 호출은 `NoSuchElementException`을 던짐
  - 반환할 원소가 더 남아 있는지는 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 `hasNext` 메서드로 확인

참고 — 이터레이터의 정체
- 이터레이터는 "지금 어디까지 읽었는가"라는 위치 상태를 가진, 한 방향으로만 흐르는 읽기 커서
- 컬렉션처럼 원소를 담고 있는 그릇이 아니라, 원소를 차례로 꺼내 주는 통로
- 한 번 지나간 원소로 되돌아갈 수 없고, 끝까지 읽고 나면 그 이터레이터는 다 쓴 것임

이터레이터 `it`이 반환하는 모든 원소를 차례로 밟아 가는 가장 직접적인 방법은 while 반복문.

```scala
// Scala 2
while (it.hasNext)
  println(it.next())
```

```scala
// Scala 3
while it.hasNext do
  println(it.next())
```

- Scala의 이터레이터는 `Iterable`과 `Seq` 클래스에서 볼 수 있는 대부분의 메서드에 대응하는 메서드도 제공
- 예: 이터레이터가 반환하는 각 원소에 대해 주어진 프로시저를 실행하는 `foreach` 메서드 → 위 반복문을 다음처럼 줄일 수 있음

```scala
// Scala 2와 3
it.foreach(println)
```

- `foreach`, `map`, `withFilter`, `flatMap`이 들어간 표현식은 for 표현식(for-expression)이라는 대체 문법으로도 쓸 수 있음 → 이터레이터가 반환하는 모든 원소를 출력하는 또 다른 방법

```scala
// Scala 2
for (elem <- it) println(elem)
```

```scala
// Scala 3
for elem <- it do println(elem)
```

- 이터레이터의 `foreach` 메서드와 이터러블(iterable) 컬렉션의 같은 메서드 사이에는 중요한 차이가 있음
  - 이터레이터에서 `foreach`를 호출하면, 실행이 끝났을 때 이터레이터가 끝 지점에 놓임 → 같은 이터레이터에서 `next`를 다시 호출하면 `NoSuchElementException` 발생
  - 이와 대조적으로 컬렉션에서 `foreach`를 호출하면 컬렉션의 원소 개수는 그대로 유지됨(전달한 함수가 원소를 추가·제거하는 경우는 예외지만, 뜻밖의 결과를 낳을 수 있어 권장하지 않음)

- `Iterator`가 `Iterable`과 공유하는 다른 연산들도 같은 성질을 가짐 — 예: 이터레이터는 새 이터레이터를 반환하는 `map` 메서드를 제공

```scala
scala> val it = Iterator("a", "number", "of", "words")
val it: Iterator[java.lang.String] = <iterator>

scala> it.map(_.length)
val res1: Iterator[Int] = <iterator>

scala> it.hasNext
val res2: Boolean = true

scala> res1.foreach(println)
1
6
2
5

scala> it.hasNext
val res4: Boolean = false
```

- `it.map`을 호출한 직후에는 이터레이터 `it`이 끝까지 전진하지 않았지만, `res1.foreach` 호출로 결과 이터레이터를 순회하면 `it`도 함께 순회되어 끝까지 전진

- 또 다른 예는 `dropWhile` 메서드 — 특정 성질을 가진 이터레이터의 첫 원소를 찾는 데 사용 가능
- 위 이터레이터에서 글자가 두 개 이상인 첫 번째 단어를 찾는 예

```scala
scala> val it = Iterator("a", "number", "of", "words")
val it: Iterator[java.lang.String] = <iterator>

scala> it.dropWhile(_.length < 2)
val res4: Iterator[java.lang.String] = <iterator>

scala> res4.next()
val res5: java.lang.String = number
```

- 여기서도 `dropWhile` 호출로 `it`이 변경됨에 주목 — 이제 `it`은 목록의 두 번째 단어인 "number"를 가리킴
- 실제로 `it`과 `dropWhile`이 반환한 결과 `res4`는 정확히 같은 원소 시퀀스를 반환함

주의 — 새 이터레이터를 반환해도 원본이 보존되지 않음
- `map`이나 `dropWhile`이 "새 이터레이터를 반환한다"고 해서 원본이 안전하게 보존되는 것이 아님
- 반환된 이터레이터는 대개 원본 위에서 동작하는 얇은 포장 → 결과를 소비하면 원본도 함께 소비됨
- 컬렉션의 `map`이 원본을 건드리지 않는 것과는 정반대의 동작 → "이터레이터에 메서드를 한 번 호출했다면 그 원본은 다시 쓰지 않는다"를 철칙으로 삼는 것이 안전

- 이 동작을 피해 가는 한 가지 방법: 이터레이터에 직접 메서드를 호출하는 대신 바탕이 되는 이터레이터를 `duplicate`로 복제
- 그 결과로 얻는 두 개의 이터레이터는 각각 바탕 이터레이터 `it`과 정확히 같은 원소들을 반환

```scala
scala> val (words, ns) = Iterator("a", "number", "of", "words").duplicate
val words: Iterator[String] = <iterator>
val ns: Iterator[String] = <iterator>

scala> val shorts = words.filter(_.length < 3).toList
val shorts: List[String] = List(a, of)

scala> val count = ns.map(_.length).sum
val count: Int = 14
```

- 두 이터레이터는 서로 독립적으로 동작 — 한쪽을 전진시켜도 다른 쪽에는 영향을 주지 않음 → 각각에 임의의 메서드를 호출해 파괴적으로 변형해도 됨
- 이렇게 하면 원소들을 두 번 순회하는 듯한 효과를 만들어 냄 — 내부 버퍼링(buffering)을 통해 달성
- 바탕 이터레이터 `it`은 직접 사용할 수 없으며 버려야 함

- 요약: 이터레이터에 메서드를 호출한 뒤에는 그 이터레이터에 다시는 접근하지 않는다는 조건을 지키는 한 이터레이터는 컬렉션처럼 동작
- Scala 컬렉션 라이브러리는 이를 [IterableOnce](https://www.scala-lang.org/api/2.13.18/scala/collection/IterableOnce.html)라는 추상화로 명시적으로 드러냄
  - `IterableOnce`는 [Iterable](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterable.html)과 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 공통 상위 클래스
  - `IterableOnce[A]`에는 `iterator: Iterator[A]`와 `knownSize: Int` 두 개의 메서드만 있음
  - `IterableOnce` 객체가 실제로 `Iterator`라면 그 `iterator` 연산은 항상 현재 상태 그대로의 자기 자신을 반환, `Iterable`이라면 `iterator` 연산은 항상 새 `Iterator`를 반환
  - `IterableOnce`의 흔한 사용처는 이터레이터와 컬렉션 어느 쪽이든 인자로 받을 수 있는 메서드의 인자 타입 — 예: `Iterable` 클래스의 이어 붙이기 메서드 `concat`. 이 메서드는 `IterableOnce` 매개변수를 받으므로 이터레이터에서 오는 원소든 컬렉션에서 오는 원소든 이어 붙일 수 있음

왜 필요한가 — `IterableOnce`가 푸는 문제
- API 중복 문제를 해결 — 이것이 없다면 `concat` 같은 메서드는 "컬렉션을 받는 버전"과 "이터레이터를 받는 버전"을 따로 만들어야 함
- "적어도 한 번은 순회할 수 있다"는 최소한의 공통 계약만 뽑아낸 덕분에, 하나의 시그니처로 두 세계를 모두 받을 수 있음
- 이름의 Once가 핵심 — `Iterator`는 딱 한 번만 순회할 수 있고 `Iterable`은 여러 번 순회할 수 있으나, 둘 다 "한 번"은 보장하기 때문

이터레이터의 모든 연산을 아래에 정리.

#### Iterator 클래스의 연산 (Operations in class Iterator)

- 추상 메서드
  - `it.next()` — 이터레이터의 다음 원소를 반환하고 그 원소를 지나 전진
  - `it.hasNext` — `it`이 원소를 하나 더 반환할 수 있으면 `true`를 반환
- 변형
  - `it.buffered` — `it`의 모든 원소를 반환하는 버퍼 이터레이터(buffered iterator)
  - `it.grouped(size)` — `it`이 반환하는 원소들을 고정 크기 시퀀스 "덩어리(chunk)"로 내어 주는 이터레이터
  - `it.sliding(size)` — `it`이 반환하는 원소들을 고정 크기로 미끄러지는 창(sliding window)을 나타내는 시퀀스로 내어 주는 이터레이터
- 복제
  - `it.duplicate` — 각각 독립적으로 `it`의 모든 원소를 반환하는 이터레이터 한 쌍
- 덧붙이기
  - `it.concat(jt)` 또는 `it ++ jt` — 이터레이터 `it`이 반환하는 모든 원소에 이어서, 이터레이터 `jt`가 반환하는 모든 원소를 반환하는 이터레이터
  - `it.padTo(len, x)` — 먼저 `it`의 모든 원소를 반환하고, 그 뒤로 전체 길이가 `len`개가 될 때까지 `x`의 복사본을 이어서 반환하는 이터레이터
- 맵
  - `it.map(f)` — `it`이 반환하는 모든 원소에 함수 `f`를 적용하여 얻은 이터레이터
  - `it.flatMap(f)` — `it`의 모든 원소에 이터레이터를 반환하는 함수 `f`를 적용한 뒤 그 결과들을 이어 붙여 얻은 이터레이터
  - `it.collect(f)` — `it`의 원소 중 부분 함수(partial function) `f`가 정의된 원소마다 `f`를 적용하고 그 결과를 모아 얻은 이터레이터
- 변환
  - `it.toArray` — `it`이 반환하는 원소들을 배열(array)에 모음
  - `it.toList` — `it`이 반환하는 원소들을 리스트(list)에 모음
  - `it.toIterable` — `it`이 반환하는 원소들을 이터러블에 모음
  - `it.toSeq` — `it`이 반환하는 원소들을 시퀀스에 모음
  - `it.toIndexedSeq` — `it`이 반환하는 원소들을 인덱스 시퀀스(indexed sequence)에 모음
  - `it.toLazyList` — `it`이 반환하는 원소들을 지연 리스트(lazy list)에 모음
  - `it.toSet` — `it`이 반환하는 원소들을 집합(set)에 모음
  - `it.toMap` — `it`이 반환하는 키/값 쌍들을 맵(map)에 모음
- 복사
  - `it.copyToArray(arr, s, n)` — `it`이 반환하는 원소를 최대 `n`개까지 배열 `arr`의 인덱스 `s`부터 복사. 마지막 두 인자는 선택 사항
- 크기 정보
  - `it.isEmpty` — 이터레이터가 비어 있는지 검사(`hasNext`의 반대)
  - `it.nonEmpty` — 컬렉션에 원소가 있는지 검사(`hasNext`의 별칭)
  - `it.size` — `it`이 반환하는 원소의 개수. 주의 — 이 연산 뒤에 `it`은 끝 지점에 놓임
  - `it.length` — `it.size`와 같음
  - `it.knownSize` — 이터레이터의 상태를 변경하지 않고도 알 수 있는 경우의 원소 개수, 그렇지 않으면 `-1`
- 원소 조회·인덱스 검색
  - `it.find(p)` — `it`이 반환하는 원소 중 `p`를 만족하는 첫 번째 원소를 담은 옵션(option), 조건에 맞는 원소가 없으면 `None`. 주의 — 이터레이터는 그 원소의 다음 위치로, 찾지 못한 경우에는 끝으로 전진
  - `it.indexOf(x)` — `it`이 반환하는 원소 중 `x`와 같은 첫 번째 원소의 인덱스. 주의 — 이터레이터는 이 원소의 위치를 지나 전진
  - `it.indexWhere(p)` — `it`이 반환하는 원소 중 `p`를 만족하는 첫 번째 원소의 인덱스. 주의 — 이터레이터는 이 원소의 위치를 지나 전진
- 부분 이터레이터
  - `it.take(n)` — `it`의 처음 `n`개 원소를 반환하는 이터레이터. 주의 — `it`은 `n`번째 원소의 다음 위치로 전진하거나, 원소가 `n`개보다 적으면 끝으로 전진
  - `it.drop(n)` — `it`의 `(n+1)`번째 원소부터 시작하는 이터레이터. 주의 — `it`도 같은 위치로 전진
  - `it.slice(m,n)` — `it`이 반환하는 원소 중 `m`번째 원소부터 시작해 `n`번째 원소 직전까지의 조각(slice)을 반환하는 이터레이터
  - `it.takeWhile(p)` — 조건 `p`가 참인 동안 `it`의 원소를 반환하는 이터레이터
  - `it.dropWhile(p)` — 조건 `p`가 `true`인 동안 `it`의 원소를 건너뛰고, 나머지를 반환하는 이터레이터
  - `it.filter(p)` — `it`의 원소 중 조건 `p`를 만족하는 모든 원소를 반환하는 이터레이터
  - `it.withFilter(p)` — `it` filter `p`와 같음. 이터레이터를 for 표현식에서 쓸 수 있게 하려고 필요
  - `it.filterNot(p)` — `it`의 원소 중 조건 `p`를 만족하지 않는 모든 원소를 반환하는 이터레이터
  - `it.distinct` — `it`의 원소를 중복 없이 반환하는 이터레이터
- 분할
  - `it.partition(p)` — `it`을 이터레이터 한 쌍으로 나눔. 하나는 술어(predicate) `p`를 만족하는 `it`의 모든 원소를 반환하고, 다른 하나는 만족하지 않는 모든 원소를 반환
  - `it.span(p)` — `it`을 이터레이터 한 쌍으로 나눔. 하나는 술어 `p`를 만족하는 `it`의 접두(prefix) 원소들을 반환하고, 다른 하나는 `it`의 나머지 원소를 모두 반환
- 원소 조건
  - `it.forall(p)` — `it`이 반환하는 모든 원소에 대해 술어 `p`가 성립하는지 나타내는 불리언(boolean)
  - `it.exists(p)` — `it`의 어떤 원소에 대해 술어 `p`가 성립하는지 나타내는 불리언
  - `it.count(p)` — `it`의 원소 중 술어 `p`를 만족하는 원소의 개수
- 폴드
  - `it.foldLeft(z)(op)` — `z`에서 시작해 왼쪽에서 오른쪽으로, `it`이 반환하는 연속한 원소들 사이에 이항 연산 `op`를 적용
  - `it.foldRight(z)(op)` — `z`에서 시작해 오른쪽에서 왼쪽으로, `it`이 반환하는 연속한 원소들 사이에 이항 연산 `op`를 적용
  - `it.reduceLeft(op)` — 비어 있지 않은 이터레이터 `it`이 반환하는 연속한 원소들 사이에, 왼쪽에서 오른쪽으로 이항 연산 `op`를 적용
  - `it.reduceRight(op)` — 비어 있지 않은 이터레이터 `it`이 반환하는 연속한 원소들 사이에, 오른쪽에서 왼쪽으로 이항 연산 `op`를 적용
- 특정 폴드
  - `it.sum` — 이터레이터 `it`이 반환하는 숫자 원소 값들의 합
  - `it.product` — 이터레이터 `it`이 반환하는 숫자 원소 값들의 곱
  - `it.min` — 이터레이터 `it`이 반환하는 순서 있는(ordered) 원소 값들의 최솟값
  - `it.max` — 이터레이터 `it`이 반환하는 순서 있는 원소 값들의 최댓값
- 집(zip)
  - `it.zip(jt)` — 이터레이터 `it`과 `jt`가 반환하는 서로 대응하는 원소들의 쌍으로 이루어진 이터레이터
  - `it.zipAll(jt, x, y)` — 이터레이터 `it`과 `jt`가 반환하는 서로 대응하는 원소들의 쌍으로 이루어진 이터레이터로, 더 짧은 이터레이터를 원소 `x` 또는 `y`를 덧붙여 긴 쪽의 길이에 맞춤
  - `it.zipWithIndex` — `it`이 반환하는 원소와 그 인덱스의 쌍으로 이루어진 이터레이터
- 갱신
  - `it.patch(i, jt, r)` — `it`에서 `i`번째부터 `r`개의 원소를 패치 이터레이터 `jt`로 교체하여 얻은 이터레이터
- 비교
  - `it.sameElements(jt)` — 이터레이터 `it`과 `jt`가 같은 원소들을 같은 순서로 반환하는지 검사. 주의 — 이 연산 뒤에 이터레이터들을 사용하는 것은 정의되지 않은 동작이며 바뀔 수 있음
- 문자열
  - `it.addString(b, start, sep, end)` — `it`이 반환하는 모든 원소를 구분자 `sep`으로 나누고 문자열 `start`와 `end`로 감싼 문자열을 `StringBuilder` `b`에 추가. `start`, `sep`, `end`는 모두 선택 사항
  - `it.mkString(start, sep, end)` — 컬렉션을, `it`이 반환하는 모든 원소를 구분자 `sep`으로 나누고 문자열 `start`와 `end`로 감싼 문자열로 변환. `start`, `sep`, `end`는 모두 선택 사항

#### 지연성 (Laziness)

- `List` 같은 구체적인 컬렉션에 직접 적용하는 연산과 달리, `Iterator`의 연산은 지연(lazy) 방식
- 지연 연산은 결과 전체를 즉시 계산하지 않음 → 각 결과가 개별적으로 요청될 때 계산

- 표현식 `(1 to 10).iterator.map(println)`은 화면에 아무것도 출력하지 않음
  - 이 경우 `map` 메서드는 인자로 받은 함수를 범위(range)의 값들에 적용하지 않고, 각 값이 요청될 때마다 그렇게 할 새 `Iterator`를 반환
  - 이 표현식 끝에 `.toList`를 붙이면 그제야 실제로 원소들이 출력됨

- 이로 인한 결과 중 하나: `map`이나 `filter` 같은 메서드가 인자로 받은 함수를 입력 원소 전부에 적용한다는 보장이 없음
  - 예: 표현식 `(1 to 10).iterator.map(println).take(5).toList`는 `1`부터 `5`까지의 값만 출력 → `map`이 반환한 `Iterator`에서 요청되는 것이 그 값들뿐이기 때문

- 이것이 `map`, `filter`, `fold` 및 그와 유사한 메서드에는 순수 함수(pure function)만 인자로 사용해야 하는 중요한 이유 중 하나
- 순수 함수는 부수 효과(side-effect)가 없음 → 보통은 `map` 안에서 `println`을 쓰지 않음. 여기서 `println`을 쓴 것은 지연성을 눈에 보이게 하기 위함(순수 함수만 쓰면 지연성은 평소에 드러나지 않음)

- 지연성은 눈에 잘 띄지 않을 때가 많지만 그래도 가치가 있음 → 불필요한 계산이 일어나는 것을 막아 주고, 다음처럼 무한 시퀀스를 다루는 것도 가능하게 함

```scala
def zipWithIndex[A](i: Iterator[A]): Iterator[(Int, A)] =
  Iterator.from(0).zip(i)
```

왜 필요한가 — 무한 시퀀스와 지연 평가
- `Iterator.from(0)`은 0, 1, 2, ...를 끝없이 내어 주는 무한 이터레이터
- 연산이 즉시(엄격하게) 평가된다면 무한 시퀀스에 `zip`을 적용하는 순간 프로그램이 멈추지 않음 → 지연 평가 덕분에 "요청받은 만큼만" 숫자를 생성
- 상대편 이터레이터 `i`가 끝나면 전체도 자연스럽게 끝남
- 대용량 파일을 한 줄씩 읽으며 처리하는 것처럼, 전체를 메모리에 올리지 않고 스트림 방식으로 일하는 코드가 모두 이 성질 위에 서 있음

#### 버퍼 이터레이터 (Buffered iterators)

- 때로는 "미리 내다볼" 수 있는 이터레이터가 필요 — 다음에 반환될 원소를, 그 원소를 지나쳐 전진하지 않고 들여다보고 싶은 경우
- 예: 문자열 시퀀스를 반환하는 이터레이터에서 앞쪽의 빈 문자열들을 건너뛰는 작업. 다음과 같이 쓰고 싶은 유혹을 느낄 수 있음

```scala
// Scala 2
def skipEmptyWordsNOT(it: Iterator[String]) =
  while (it.next().isEmpty) {}
```

```scala
// Scala 3
def skipEmptyWordsNOT(it: Iterator[String]) =
  while it.next().isEmpty do ()
```

- 이 코드는 앞쪽의 빈 문자열들을 건너뛰기는 하지만, 첫 번째 비어 있지 않은 문자열까지 지나쳐 `it`을 전진시켜 버림 → 잘못된 코드

- 이 문제의 해법은 버퍼 이터레이터를 사용하는 것
- [BufferedIterator](https://www.scala-lang.org/api/2.13.18/scala/collection/BufferedIterator.html) 클래스는 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 하위 클래스로, `head`라는 메서드를 하나 더 제공
- 버퍼 이터레이터에서 `head`를 호출하면 첫 번째 원소를 반환하되 이터레이터를 전진시키지 않음
- 버퍼 이터레이터를 사용하면 빈 단어 건너뛰기를 다음처럼 쓸 수 있음

```scala
// Scala 2
def skipEmptyWords(it: BufferedIterator[String]) =
  while (it.head.isEmpty) { it.next() }
```

```scala
// Scala 3
def skipEmptyWords(it: BufferedIterator[String]) =
  while it.head.isEmpty do it.next()
```

모든 이터레이터는 `buffered` 메서드를 호출해 버퍼 이터레이터로 변환 가능.

```scala
scala> val it = Iterator(1, 2, 3, 4)
val it: Iterator[Int] = <iterator>

scala> val bit = it.buffered
val bit: scala.collection.BufferedIterator[Int] = <iterator>

scala> bit.head
val res10: Int = 1

scala> bit.next()
val res11: Int = 1

scala> bit.next()
val res12: Int = 2

scala> bit.headOption
val res13: Option[Int] = Some(3)
```

- 버퍼 이터레이터 `bit`에서 `head`를 호출해도 `bit`은 전진하지 않음 → 이어지는 `bit.next()` 호출은 `bit.head`와 같은 값을 반환
- 바탕이 되는 이터레이터를 직접 사용해서는 안 되며 버려야 함

참고 — 버퍼의 동작 원리
- 버퍼(buffer)란 잠시 담아 두는 임시 저장 공간
- 버퍼 이터레이터의 `head`는 사실 뒤에서 `next()`를 한 번 호출해 원소를 꺼낸 다음, 그것을 버퍼에 담아 두고 보여 주는 것
- 나중에 진짜 `next()`가 호출되면 버퍼에 있던 그 원소를 내어 줌
- 겉에서 보기에는 "전진하지 않고 미리 본" 것처럼 동작

- 버퍼 이터레이터는 `head`가 호출될 때 다음 원소 하나만 버퍼에 담음
- `duplicate`나 `partition`이 만들어 내는 것 같은 다른 파생 이터레이터들은 바탕 이터레이터의 임의 길이 부분 시퀀스를 버퍼링할 수 있음
- 이터레이터들은 `++`로 이어 붙이는 방식으로 효율적으로 결합 가능

```scala
// Scala 2
scala> def collapse(it: Iterator[Int]) = if (!it.hasNext) Iterator.empty else {
      |  var head = it.next
      |  val rest = if (head == 0) it.dropWhile(_ == 0) else it
      |  Iterator.single(head) ++ rest
      |}
def collapse(it: Iterator[Int]): Iterator[Int]

scala> def collapse(it: Iterator[Int]) = {
      |  val (zeros, rest) = it.span(_ == 0)
      |  zeros.take(1) ++ rest
      |}
def collapse(it: Iterator[Int]): Iterator[Int]

scala> collapse(Iterator(0, 0, 0, 1, 2, 3, 4)).toList
val res14: List[Int] = List(0, 1, 2, 3, 4)
```

```scala
// Scala 3
scala> def collapse(it: Iterator[Int]) = if !it.hasNext then Iterator.empty else
      |  var head = it.next
      |  val rest = if head == 0 then it.dropWhile(_ == 0) else it
      |  Iterator.single(head) ++ rest
      |
def collapse(it: Iterator[Int]): Iterator[Int]

scala> def collapse(it: Iterator[Int]) =
      |  val (zeros, rest) = it.span(_ == 0)
      |  zeros.take(1) ++ rest
      |
def collapse(it: Iterator[Int]): Iterator[Int]

scala> collapse(Iterator(0, 0, 0, 1, 2, 3, 4)).toList
val res14: List[Int] = List(0, 1, 2, 3, 4)
```

- `collapse`의 두 번째 버전에서는 아직 소비되지 않은 0들이 내부적으로 버퍼링됨
- 첫 번째 버전에서는 앞쪽의 0들을 버린 다음, 원하는 결과를 두 구성 이터레이터를 차례로 호출하기만 하는 연결(concatenated) 이터레이터로 구성

주의 — 같은 결과, 다른 메모리 특성
- 이 예제의 요점: 두 `collapse`가 같은 결과를 내지만 메모리 특성이 다름
- `span` 버전은 앞쪽의 0들을 담은 이터레이터(`zeros`)와 나머지(`rest`)를 한 쌍으로 돌려줌 → `rest`를 먼저 소비할 수도 있어야 하므로 라이브러리가 0들을 내부 버퍼에 쌓아 둠
- 첫 번째 버전은 `dropWhile`로 0들을 그냥 흘려 버리고 원소 하나만 기억 → 버퍼링이 없음
- 이터레이터를 크게 다룰 때는 이런 파생 연산(`duplicate`, `partition`, `span`)이 보이지 않는 버퍼를 만들 수 있다는 점을 기억해 두는 것이 좋음
