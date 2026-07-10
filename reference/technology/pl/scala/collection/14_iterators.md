# 이터레이터 (Iterators)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/iterators.html>

이터레이터(iterator)는 컬렉션이 아니라, 컬렉션의 원소에 하나씩 접근하는 방법입니다. 이터레이터 `it`의 두 가지 기본 연산은 `next`와 `hasNext`입니다. `it.next()`를 호출하면 이터레이터의 다음 원소를 반환하면서 이터레이터의 상태를 앞으로 전진시킵니다. 같은 이터레이터에서 `next`를 다시 호출하면, 이전에 반환된 원소의 바로 다음 원소를 내어 줍니다. 더 이상 반환할 원소가 없으면 `next` 호출은 `NoSuchElementException`을 던집니다. 반환할 원소가 더 남아 있는지는 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 `hasNext` 메서드로 알아낼 수 있습니다.

> 📘 **처음 배우는 분께** — 이터레이터는 "지금 어디까지 읽었는가"라는 위치 상태를 가진, 한 방향으로만 흐르는 읽기 커서라고 생각하면 됩니다. 컬렉션처럼 원소를 담고 있는 그릇이 아니라, 원소를 차례로 꺼내 주는 통로입니다. 그래서 한 번 지나간 원소로 되돌아갈 수 없고, 끝까지 읽고 나면 그 이터레이터는 다 쓴 것입니다.

이터레이터 `it`이 반환하는 모든 원소를 "차례로 밟아 가는" 가장 직접적인 방법은 while 반복문입니다.

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

Scala의 이터레이터는 `Iterable`과 `Seq` 클래스에서 볼 수 있는 대부분의 메서드에 대응하는 메서드도 제공합니다. 예를 들어 이터레이터가 반환하는 각 원소에 대해 주어진 프로시저를 실행하는 `foreach` 메서드를 제공합니다. `foreach`를 사용하면 위의 반복문을 다음처럼 줄일 수 있습니다.

```scala
// Scala 2와 3
it.foreach(println)
```

늘 그렇듯이 `foreach`, `map`, `withFilter`, `flatMap`이 들어간 표현식은 for 표현식(for-expression)이라는 대체 문법으로도 쓸 수 있으므로, 이터레이터가 반환하는 모든 원소를 출력하는 또 하나의 방법은 다음과 같습니다.

```scala
// Scala 2
for (elem <- it) println(elem)
```

```scala
// Scala 3
for elem <- it do println(elem)
```

이터레이터의 `foreach` 메서드와 이터러블(iterable) 컬렉션의 같은 메서드 사이에는 중요한 차이가 있습니다. 이터레이터에서 `foreach`를 호출하면, 실행이 끝났을 때 이터레이터가 끝 지점에 놓입니다. 그래서 같은 이터레이터에서 `next`를 다시 호출하면 `NoSuchElementException`이 발생합니다. 이와 대조적으로 컬렉션에서 `foreach`를 호출하면 컬렉션의 원소 개수는 그대로 유지됩니다(전달한 함수가 원소를 추가하거나 제거하는 경우는 예외지만, 뜻밖의 결과를 낳을 수 있으므로 권장하지 않습니다).

`Iterator`가 `Iterable`과 공유하는 다른 연산들도 같은 성질을 가집니다. 예를 들어 이터레이터는 새 이터레이터를 반환하는 `map` 메서드를 제공합니다.

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

보다시피 `it.map`을 호출한 직후에는 이터레이터 `it`이 끝까지 전진하지 않았지만, `res1.foreach` 호출로 결과 이터레이터를 순회하면 `it`도 함께 순회되어 끝까지 전진합니다.

또 다른 예는 `dropWhile` 메서드로, 특정 성질을 가진 이터레이터의 첫 원소를 찾는 데 사용할 수 있습니다. 예를 들어 위의 이터레이터에서 글자가 두 개 이상인 첫 번째 단어를 찾으려면 다음과 같이 쓸 수 있습니다.

```scala
scala> val it = Iterator("a", "number", "of", "words")
val it: Iterator[java.lang.String] = <iterator>

scala> it.dropWhile(_.length < 2)
val res4: Iterator[java.lang.String] = <iterator>

scala> res4.next()
val res5: java.lang.String = number
```

여기서도 `dropWhile` 호출로 `it`이 변경되었다는 점에 주목하세요. 이제 `it`은 목록의 두 번째 단어인 "number"를 가리킵니다. 실제로 `it`과 `dropWhile`이 반환한 결과 `res4`는 정확히 같은 원소 시퀀스를 반환하게 됩니다.

> ⚠️ **짚고 넘어가기** — `map`이나 `dropWhile`이 "새 이터레이터를 반환한다"고 해서 원본이 안전하게 보존되는 것이 아닙니다. 반환된 이터레이터는 대개 원본 위에서 동작하는 얇은 포장이라서, 결과를 소비하면 원본도 함께 소비됩니다. 컬렉션의 `map`이 원본을 건드리지 않는 것과는 정반대의 동작이므로, "이터레이터에 메서드를 한 번 호출했다면 그 원본은 다시 쓰지 않는다"를 철칙으로 삼는 것이 안전합니다.

이 동작을 피해 가는 한 가지 방법은, 이터레이터에 직접 메서드를 호출하는 대신 바탕이 되는 이터레이터를 `duplicate`로 복제하는 것입니다. 그 결과로 얻는 *두 개의* 이터레이터는 각각 바탕 이터레이터 `it`과 정확히 같은 원소들을 반환합니다.

```scala
scala> val (words, ns) = Iterator("a", "number", "of", "words").duplicate
val words: Iterator[String] = <iterator>
val ns: Iterator[String] = <iterator>

scala> val shorts = words.filter(_.length < 3).toList
val shorts: List[String] = List(a, of)

scala> val count = ns.map(_.length).sum
val count: Int = 14
```

두 이터레이터는 서로 독립적으로 동작합니다. 한쪽을 전진시켜도 다른 쪽에는 영향을 주지 않으므로, 각각에 임의의 메서드를 호출해 파괴적으로 변형해도 됩니다. 이렇게 하면 원소들을 두 번 순회하는 듯한 환상을 만들어 내는데, 그 효과는 내부 버퍼링(buffering)을 통해 달성됩니다. 늘 그렇듯이 바탕 이터레이터 `it`은 직접 사용할 수 없으며 버려야 합니다.

요약하면, *이터레이터에 메서드를 호출한 뒤에는 그 이터레이터에 다시는 접근하지 않는다*는 조건을 지키는 한 이터레이터는 컬렉션처럼 동작합니다. Scala 컬렉션 라이브러리는 이를 [IterableOnce](https://www.scala-lang.org/api/2.13.18/scala/collection/IterableOnce.html)라는 추상화로 명시적으로 드러냅니다. `IterableOnce`는 [Iterable](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterable.html)과 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 공통 상위 클래스입니다. `IterableOnce[A]`에는 `iterator: Iterator[A]`와 `knownSize: Int` 두 개의 메서드만 있습니다. `IterableOnce` 객체가 실제로 `Iterator`라면 그 `iterator` 연산은 항상 현재 상태 그대로의 자기 자신을 반환하고, `Iterable`이라면 `iterator` 연산은 항상 새 `Iterator`를 반환합니다. `IterableOnce`의 흔한 사용처는 이터레이터와 컬렉션 어느 쪽이든 인자로 받을 수 있는 메서드의 인자 타입입니다. 한 예가 `Iterable` 클래스의 이어 붙이기 메서드 `concat`입니다. 이 메서드는 `IterableOnce` 매개변수를 받으므로, 이터레이터에서 오는 원소든 컬렉션에서 오는 원소든 이어 붙일 수 있습니다.

> 💡 **왜 필요한가** — `IterableOnce`가 푸는 문제는 API 중복입니다. 이것이 없다면 `concat` 같은 메서드는 "컬렉션을 받는 버전"과 "이터레이터를 받는 버전"을 따로 만들어야 합니다. "적어도 한 번은 순회할 수 있다"는 최소한의 공통 계약만 뽑아낸 덕분에, 하나의 시그니처로 두 세계를 모두 받을 수 있습니다. 이름의 Once가 핵심인데, `Iterator`는 딱 한 번만 순회할 수 있고 `Iterable`은 여러 번 순회할 수 있지만, 둘 다 "한 번"은 보장하기 때문입니다.

이터레이터의 모든 연산을 아래에 정리했습니다.

### Iterator 클래스의 연산 (Operations in class Iterator)

| 연산 | 하는 일 |
|---|---|
| **추상 메서드:** |  |
| `it.next()` | 이터레이터의 다음 원소를 반환하고 그 원소를 지나 전진합니다. |
| `it.hasNext` | `it`이 원소를 하나 더 반환할 수 있으면 `true`를 반환합니다. |
| **변형:** |  |
| `it.buffered` | `it`의 모든 원소를 반환하는 버퍼 이터레이터(buffered iterator)입니다. |
| `it.grouped(size)` | `it`이 반환하는 원소들을 고정 크기 시퀀스 "덩어리(chunk)"로 내어 주는 이터레이터입니다. |
| `it.sliding(size)` | `it`이 반환하는 원소들을 고정 크기로 미끄러지는 창(sliding window)을 나타내는 시퀀스로 내어 주는 이터레이터입니다. |
| **복제:** |  |
| `it.duplicate` | 각각 독립적으로 `it`의 모든 원소를 반환하는 이터레이터 한 쌍입니다. |
| **덧붙이기:** |  |
| `it.concat(jt)` 또는 `it ++ jt` | 이터레이터 `it`이 반환하는 모든 원소에 이어서, 이터레이터 `jt`가 반환하는 모든 원소를 반환하는 이터레이터입니다. |
| `it.padTo(len, x)` | 먼저 `it`의 모든 원소를 반환하고, 그 뒤로 전체 길이가 `len`개가 될 때까지 `x`의 복사본을 이어서 반환하는 이터레이터입니다. |
| **맵:** |  |
| `it.map(f)` | `it`이 반환하는 모든 원소에 함수 `f`를 적용하여 얻은 이터레이터입니다. |
| `it.flatMap(f)` | `it`의 모든 원소에 이터레이터를 반환하는 함수 `f`를 적용한 뒤 그 결과들을 이어 붙여 얻은 이터레이터입니다. |
| `it.collect(f)` | `it`의 원소 중 부분 함수(partial function) `f`가 정의된 원소마다 `f`를 적용하고 그 결과를 모아 얻은 이터레이터입니다. |
| **변환:** |  |
| `it.toArray` | `it`이 반환하는 원소들을 배열(array)에 모읍니다. |
| `it.toList` | `it`이 반환하는 원소들을 리스트(list)에 모읍니다. |
| `it.toIterable` | `it`이 반환하는 원소들을 이터러블에 모읍니다. |
| `it.toSeq` | `it`이 반환하는 원소들을 시퀀스에 모읍니다. |
| `it.toIndexedSeq` | `it`이 반환하는 원소들을 인덱스 시퀀스(indexed sequence)에 모읍니다. |
| `it.toLazyList` | `it`이 반환하는 원소들을 지연 리스트(lazy list)에 모읍니다. |
| `it.toSet` | `it`이 반환하는 원소들을 집합(set)에 모읍니다. |
| `it.toMap` | `it`이 반환하는 키/값 쌍들을 맵(map)에 모읍니다. |
| **복사:** |  |
| `it.copyToArray(arr, s, n)` | `it`이 반환하는 원소를 최대 `n`개까지 배열 `arr`의 인덱스 `s`부터 복사합니다. 마지막 두 인자는 선택 사항입니다. |
| **크기 정보:** |  |
| `it.isEmpty` | 이터레이터가 비어 있는지 검사합니다(`hasNext`의 반대). |
| `it.nonEmpty` | 컬렉션에 원소가 있는지 검사합니다(`hasNext`의 별칭). |
| `it.size` | `it`이 반환하는 원소의 개수입니다. 주의: 이 연산 뒤에 `it`은 끝 지점에 놓입니다! |
| `it.length` | `it.size`와 같습니다. |
| `it.knownSize` | 이터레이터의 상태를 변경하지 않고도 알 수 있는 경우의 원소 개수이고, 그렇지 않으면 `-1`입니다. |
| **원소 조회 · 인덱스 검색:** |  |
| `it.find(p)` | `it`이 반환하는 원소 중 `p`를 만족하는 첫 번째 원소를 담은 옵션(option)이며, 조건에 맞는 원소가 없으면 `None`입니다. 주의: 이터레이터는 그 원소의 다음 위치로, 찾지 못한 경우에는 끝으로 전진합니다. |
| `it.indexOf(x)` | `it`이 반환하는 원소 중 `x`와 같은 첫 번째 원소의 인덱스입니다. 주의: 이터레이터는 이 원소의 위치를 지나 전진합니다. |
| `it.indexWhere(p)` | `it`이 반환하는 원소 중 `p`를 만족하는 첫 번째 원소의 인덱스입니다. 주의: 이터레이터는 이 원소의 위치를 지나 전진합니다. |
| **부분 이터레이터:** |  |
| `it.take(n)` | `it`의 처음 `n`개 원소를 반환하는 이터레이터입니다. 주의: `it`은 `n`번째 원소의 다음 위치로 전진하거나, 원소가 `n`개보다 적으면 끝으로 전진합니다. |
| `it.drop(n)` | `it`의 `(n+1)`번째 원소부터 시작하는 이터레이터입니다. 주의: `it`도 같은 위치로 전진합니다. |
| `it.slice(m,n)` | `it`이 반환하는 원소 중 `m`번째 원소부터 시작해 `n`번째 원소 직전까지의 조각(slice)을 반환하는 이터레이터입니다. |
| `it.takeWhile(p)` | 조건 `p`가 참인 동안 `it`의 원소를 반환하는 이터레이터입니다. |
| `it.dropWhile(p)` | 조건 `p`가 `true`인 동안 `it`의 원소를 건너뛰고, 나머지를 반환하는 이터레이터입니다. |
| `it.filter(p)` | `it`의 원소 중 조건 `p`를 만족하는 모든 원소를 반환하는 이터레이터입니다. |
| `it.withFilter(p)` | `it` filter `p`와 같습니다. 이터레이터를 for 표현식에서 쓸 수 있게 하려고 필요합니다. |
| `it.filterNot(p)` | `it`의 원소 중 조건 `p`를 만족하지 않는 모든 원소를 반환하는 이터레이터입니다. |
| `it.distinct` | `it`의 원소를 중복 없이 반환하는 이터레이터입니다. |
| **분할:** |  |
| `it.partition(p)` | `it`을 이터레이터 한 쌍으로 나눕니다. 하나는 술어(predicate) `p`를 만족하는 `it`의 모든 원소를 반환하고, 다른 하나는 만족하지 않는 모든 원소를 반환합니다. |
| `it.span(p)` | `it`을 이터레이터 한 쌍으로 나눕니다. 하나는 술어 `p`를 만족하는 `it`의 접두(prefix) 원소들을 반환하고, 다른 하나는 `it`의 나머지 원소를 모두 반환합니다. |
| **원소 조건:** |  |
| `it.forall(p)` | `it`이 반환하는 모든 원소에 대해 술어 `p`가 성립하는지 나타내는 불리언(boolean)입니다. |
| `it.exists(p)` | `it`의 어떤 원소에 대해 술어 `p`가 성립하는지 나타내는 불리언입니다. |
| `it.count(p)` | `it`의 원소 중 술어 `p`를 만족하는 원소의 개수입니다. |
| **폴드:** |  |
| `it.foldLeft(z)(op)` | `z`에서 시작해 왼쪽에서 오른쪽으로, `it`이 반환하는 연속한 원소들 사이에 이항 연산 `op`를 적용합니다. |
| `it.foldRight(z)(op)` | `z`에서 시작해 오른쪽에서 왼쪽으로, `it`이 반환하는 연속한 원소들 사이에 이항 연산 `op`를 적용합니다. |
| `it.reduceLeft(op)` | 비어 있지 않은 이터레이터 `it`이 반환하는 연속한 원소들 사이에, 왼쪽에서 오른쪽으로 이항 연산 `op`를 적용합니다. |
| `it.reduceRight(op)` | 비어 있지 않은 이터레이터 `it`이 반환하는 연속한 원소들 사이에, 오른쪽에서 왼쪽으로 이항 연산 `op`를 적용합니다. |
| **특정 폴드:** |  |
| `it.sum` | 이터레이터 `it`이 반환하는 숫자 원소 값들의 합입니다. |
| `it.product` | 이터레이터 `it`이 반환하는 숫자 원소 값들의 곱입니다. |
| `it.min` | 이터레이터 `it`이 반환하는 순서 있는(ordered) 원소 값들의 최솟값입니다. |
| `it.max` | 이터레이터 `it`이 반환하는 순서 있는 원소 값들의 최댓값입니다. |
| **집(zip):** |  |
| `it.zip(jt)` | 이터레이터 `it`과 `jt`가 반환하는 서로 대응하는 원소들의 쌍으로 이루어진 이터레이터입니다. |
| `it.zipAll(jt, x, y)` | 이터레이터 `it`과 `jt`가 반환하는 서로 대응하는 원소들의 쌍으로 이루어진 이터레이터로, 더 짧은 이터레이터를 원소 `x` 또는 `y`를 덧붙여 긴 쪽의 길이에 맞춥니다. |
| `it.zipWithIndex` | `it`이 반환하는 원소와 그 인덱스의 쌍으로 이루어진 이터레이터입니다. |
| **갱신:** |  |
| `it.patch(i, jt, r)` | `it`에서 `i`번째부터 `r`개의 원소를 패치 이터레이터 `jt`로 교체하여 얻은 이터레이터입니다. |
| **비교:** |  |
| `it.sameElements(jt)` | 이터레이터 `it`과 `jt`가 같은 원소들을 같은 순서로 반환하는지 검사합니다. 주의: 이 연산 뒤에 이터레이터들을 사용하는 것은 정의되지 않은 동작이며 바뀔 수 있습니다. |
| **문자열:** |  |
| `it.addString(b, start, sep, end)` | `it`이 반환하는 모든 원소를 구분자 `sep`으로 나누고 문자열 `start`와 `end`로 감싼 문자열을 `StringBuilder` `b`에 추가합니다. `start`, `sep`, `end`는 모두 선택 사항입니다. |
| `it.mkString(start, sep, end)` | 컬렉션을, `it`이 반환하는 모든 원소를 구분자 `sep`으로 나누고 문자열 `start`와 `end`로 감싼 문자열로 변환합니다. `start`, `sep`, `end`는 모두 선택 사항입니다. |

### 지연성 (Laziness)

`List` 같은 구체적인 컬렉션에 직접 적용하는 연산과 달리, `Iterator`의 연산은 지연(lazy) 방식입니다.

지연 연산은 결과 전체를 즉시 계산하지 않습니다. 대신 각 결과가 개별적으로 요청될 때 계산합니다.

그래서 표현식 `(1 to 10).iterator.map(println)`은 화면에 아무것도 출력하지 않습니다. 이 경우 `map` 메서드는 인자로 받은 함수를 범위(range)의 값들에 적용하지 않고, 각 값이 요청될 때마다 그렇게 할 새 `Iterator`를 반환합니다. 이 표현식 끝에 `.toList`를 붙이면 그제야 실제로 원소들이 출력됩니다.

이로 인한 결과 중 하나는, `map`이나 `filter` 같은 메서드가 인자로 받은 함수를 입력 원소 전부에 적용한다는 보장이 없다는 것입니다. 예를 들어 표현식 `(1 to 10).iterator.map(println).take(5).toList`는 `1`부터 `5`까지의 값만 출력합니다. `map`이 반환한 `Iterator`에서 요청되는 것이 그 값들뿐이기 때문입니다.

이것이 `map`, `filter`, `fold` 및 그와 유사한 메서드에는 순수 함수(pure function)만 인자로 사용해야 하는 중요한 이유 중 하나입니다. 순수 함수는 부수 효과(side-effect)가 없다는 점을 기억하세요. 그래서 보통은 `map` 안에서 `println`을 쓰지 않습니다. 여기서 `println`을 쓴 것은 지연성을 눈에 보이게 하기 위해서인데, 순수 함수만 쓰면 지연성은 평소에 드러나지 않기 때문입니다.

지연성은 눈에 잘 띄지 않을 때가 많지만 그래도 가치가 있습니다. 불필요한 계산이 일어나는 것을 막아 주고, 다음처럼 무한 시퀀스를 다루는 것도 가능하게 해 주기 때문입니다.

```scala
def zipWithIndex[A](i: Iterator[A]): Iterator[(Int, A)] =
  Iterator.from(0).zip(i)
```

> 💡 **왜 필요한가** — `Iterator.from(0)`은 0, 1, 2, ...를 끝없이 내어 주는 무한 이터레이터입니다. 연산이 즉시(엄격하게) 평가된다면 무한 시퀀스에 `zip`을 적용하는 순간 프로그램이 멈추지 않겠지만, 지연 평가 덕분에 "요청받은 만큼만" 숫자를 만들어 냅니다. 그래서 상대편 이터레이터 `i`가 끝나면 전체도 자연스럽게 끝납니다. 대용량 파일을 한 줄씩 읽으며 처리하는 것처럼, 전체를 메모리에 올리지 않고 스트림 방식으로 일하는 코드가 모두 이 성질 위에 서 있습니다.

### 버퍼 이터레이터 (Buffered iterators)

때로는 "미리 내다볼" 수 있는 이터레이터가 필요합니다. 다음에 반환될 원소를, 그 원소를 지나쳐 전진하지 않고 들여다보고 싶은 경우입니다. 예를 들어 문자열 시퀀스를 반환하는 이터레이터에서 앞쪽의 빈 문자열들을 건너뛰는 작업을 생각해 봅시다. 다음과 같이 쓰고 싶은 유혹을 느낄 수 있습니다.

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

하지만 이 코드를 자세히 들여다보면 잘못되었다는 것이 분명해집니다. 이 코드는 앞쪽의 빈 문자열들을 건너뛰기는 하지만, 첫 번째 비어 있지 않은 문자열까지 지나쳐 `it`을 전진시켜 버립니다!

이 문제의 해법은 버퍼 이터레이터를 사용하는 것입니다. [BufferedIterator](https://www.scala-lang.org/api/2.13.18/scala/collection/BufferedIterator.html) 클래스는 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)의 하위 클래스로, `head`라는 메서드를 하나 더 제공합니다. 버퍼 이터레이터에서 `head`를 호출하면 첫 번째 원소를 반환하되 이터레이터를 전진시키지 않습니다. 버퍼 이터레이터를 사용하면 빈 단어 건너뛰기를 다음처럼 쓸 수 있습니다.

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

모든 이터레이터는 `buffered` 메서드를 호출해 버퍼 이터레이터로 변환할 수 있습니다. 예를 들면 다음과 같습니다.

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

버퍼 이터레이터 `bit`에서 `head`를 호출해도 `bit`은 전진하지 않는다는 점에 주목하세요. 그래서 이어지는 `bit.next()` 호출은 `bit.head`와 같은 값을 반환합니다.

늘 그렇듯이 바탕이 되는 이터레이터를 직접 사용해서는 안 되며 버려야 합니다.

> 📘 **처음 배우는 분께** — 버퍼(buffer)란 잠시 담아 두는 임시 저장 공간을 말합니다. 버퍼 이터레이터의 `head`는 사실 뒤에서 `next()`를 한 번 호출해 원소를 꺼낸 다음, 그것을 버퍼에 담아 두고 보여 주는 것입니다. 나중에 진짜 `next()`가 호출되면 버퍼에 있던 그 원소를 내어 줍니다. 그래서 겉에서 보기에는 "전진하지 않고 미리 본" 것처럼 동작합니다.

버퍼 이터레이터는 `head`가 호출될 때 다음 원소 하나만 버퍼에 담습니다. `duplicate`나 `partition`이 만들어 내는 것 같은 다른 파생 이터레이터들은 바탕 이터레이터의 임의 길이 부분 시퀀스를 버퍼링할 수 있습니다. 하지만 이터레이터들은 `++`로 이어 붙이는 방식으로 효율적으로 결합할 수 있습니다.

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

`collapse`의 두 번째 버전에서는 아직 소비되지 않은 0들이 내부적으로 버퍼링됩니다. 첫 번째 버전에서는 앞쪽의 0들을 버린 다음, 원하는 결과를 두 구성 이터레이터를 차례로 호출하기만 하는 연결(concatenated) 이터레이터로 구성합니다.

> ⚠️ **짚고 넘어가기** — 이 예제의 요점은 두 `collapse`가 같은 결과를 내지만 메모리 특성이 다르다는 것입니다. `span` 버전은 앞쪽의 0들을 담은 이터레이터(`zeros`)와 나머지(`rest`)를 한 쌍으로 돌려주는데, `rest`를 먼저 소비할 수도 있어야 하므로 라이브러리가 0들을 내부 버퍼에 쌓아 둘 수 있습니다. 반면 첫 번째 버전은 `dropWhile`로 0들을 그냥 흘려 버리고 원소 하나만 기억하므로 버퍼링이 없습니다. 이터레이터를 크게 다룰 때는 이런 파생 연산(`duplicate`, `partition`, `span`)이 보이지 않는 버퍼를 만들 수 있다는 점을 기억해 두는 것이 좋습니다.
