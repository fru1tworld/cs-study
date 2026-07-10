# Iterable 트레이트 (Trait Iterable)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html>

컬렉션 계층 구조의 최상위에는 트레이트(trait) `Iterable`이 있습니다. 이 트레이트의 모든 메서드는 하나의 추상 메서드(abstract method)인 `iterator`를 기반으로 정의되어 있으며, 이 메서드는 컬렉션의 원소를 하나씩 차례로 내어 줍니다.

```scala
def iterator: Iterator[A]
```

`Iterable`을 구현하는 컬렉션 클래스는 이 메서드만 정의하면 됩니다. 나머지 모든 메서드는 `Iterable`로부터 상속받을 수 있습니다.

> 💡 **왜 필요한가** — 메서드 수십 개를 컬렉션마다 일일이 구현한다면 새 컬렉션을 만들 때마다 엄청난 중복이 생깁니다. `Iterable`은 "원소를 하나씩 순회하는 방법(`iterator`)만 알려 주면 `map`, `filter`, `foldLeft` 같은 나머지 연산은 전부 공짜로 제공한다"는 설계입니다. 덕분에 새로운 컬렉션을 정의하는 비용이 크게 줄고, 모든 컬렉션이 동일한 연산 집합을 일관되게 갖게 됩니다.

`Iterable`은 또한 많은 구체(concrete) 메서드를 정의하며, 이들은 아래 표에 모두 나열되어 있습니다. 이 메서드들은 다음과 같은 범주로 나눌 수 있습니다.

- **덧붙이기 연산** `concat`. 두 컬렉션을 이어 붙이거나, 이터레이터(iterator)의 모든 원소를 컬렉션 뒤에 덧붙입니다.
- **맵 연산** `map`, `flatMap`, `collect`. 컬렉션의 원소에 어떤 함수를 적용하여 새로운 컬렉션을 만들어 냅니다.
- **변환 연산** `to`, `toList`, `toVector`, `toMap`, `toSet`, `toSeq`, `toIndexedSeq`, `toBuffer`, `toArray`. `Iterable` 컬렉션을 더 구체적인 무언가로 바꿉니다. 변환 대상이 가변(mutable) 컬렉션이라면(`to(collection.mutable.X)`, `toArray`, `toBuffer`) 원본 원소를 복사하여 새 컬렉션을 만듭니다. 이 변환들은 모두, 컬렉션의 런타임 타입이 요구된 컬렉션 타입과 이미 일치하는 경우 수신자(receiver) 인자를 그대로 돌려줍니다. 예를 들어 리스트에 `toList`를 적용하면 그 리스트 자신이 반환됩니다.
- **복사 연산** `copyToArray`. 이름 그대로 컬렉션의 원소를 배열(array)로 복사합니다.
- **크기 정보 연산** `isEmpty`, `nonEmpty`, `size`, `knownSize`, `sizeIs`. 컬렉션의 원소 개수를 구하려면 경우에 따라 순회(traversal)가 필요할 수 있습니다(예: `List`). 또 어떤 경우에는 컬렉션이 무한 개의 원소를 가질 수도 있습니다(예: `LazyList.from(1)`).
- **원소 조회 연산** `head`, `last`, `headOption`, `lastOption`, `find`. 컬렉션의 첫 번째 또는 마지막 원소를 선택하거나, 조건을 만족하는 첫 번째 원소를 선택합니다. 다만 모든 컬렉션에서 "첫 번째"와 "마지막"의 의미가 잘 정의되어 있는 것은 아니라는 점에 유의해야 합니다. 예를 들어 해시 집합(hash set)은 원소를 해시 키에 따라 저장할 수 있는데, 이 해시 키는 실행할 때마다 달라질 수 있습니다. 그 경우 해시 집합의 "첫 번째" 원소도 프로그램을 실행할 때마다 달라질 수 있습니다. 어떤 컬렉션이 항상 같은 순서로 원소를 내어 준다면 그 컬렉션은 *순서가 있다*(ordered)고 합니다. 대부분의 컬렉션은 순서가 있지만, 일부(예: 해시 집합)는 그렇지 않습니다. 순서를 포기하면 약간의 효율을 더 얻을 수 있기 때문입니다. 순서는 재현 가능한 테스트를 작성하고 디버깅을 돕는 데 필수적인 경우가 많습니다. 그래서 스칼라 컬렉션은 모든 컬렉션 타입에 대해 순서가 있는 대안을 제공합니다. 예를 들어 `HashSet`의 순서 있는 대안은 `LinkedHashSet`입니다.
- **부분 컬렉션 조회 연산** `tail`, `init`, `slice`, `take`, `drop`, `takeWhile`, `dropWhile`, `filter`, `filterNot`, `withFilter`. 인덱스 범위나 어떤 술어(predicate)로 식별되는 부분 컬렉션을 반환합니다.
- **분할 연산** `splitAt`, `span`, `partition`, `partitionMap`, `groupBy`, `groupMap`, `groupMapReduce`. 이 컬렉션의 원소들을 여러 부분 컬렉션으로 나눕니다.
- **원소 검사 연산** `exists`, `forall`, `count`. 주어진 술어로 컬렉션의 원소를 검사합니다.
- **폴드 연산** `foldLeft`, `foldRight`, `reduceLeft`, `reduceRight`. 연속한 원소들에 이항 연산(binary operation)을 적용합니다.
- **특정 폴드 연산** `sum`, `product`, `min`, `max`. 특정 타입(숫자 타입이거나 비교 가능한 타입)의 컬렉션에서 동작합니다.
- **문자열 연산** `mkString`, `addString`. 컬렉션을 문자열로 변환하는 다른 방법을 제공합니다.
- **뷰 연산**: 뷰(view)는 지연 평가(lazy evaluation)되는 컬렉션입니다. 뷰에 대해서는 [나중에](https://docs.scala-lang.org/overviews/collections-2.13/views.html) 더 자세히 배우게 됩니다.

> 📘 **처음 배우는 분께** — 술어(predicate)란 원소 하나를 받아 `Boolean`을 돌려주는 함수를 말합니다. 예를 들어 `xs.filter(x => x > 0)`에서 `x => x > 0`이 술어이고, `filter`는 이 술어가 `true`를 돌려주는 원소만 남깁니다. 위 목록에서 `p`로 표기된 인자는 전부 술어입니다.

`Iterable`에는 이터레이터를 반환하는 메서드가 두 개 더 있는데, 바로 `grouped`와 `sliding`입니다. 다만 이 이터레이터들은 원소를 하나씩 반환하는 것이 아니라, 원본 컬렉션 원소들의 부분 시퀀스(subsequence) 전체를 반환합니다. 이 부분 시퀀스의 최대 크기는 메서드의 인자로 지정합니다. `grouped` 메서드는 원소들을 "덩어리"(chunk) 단위로 잘라 반환하는 반면, `sliding`은 원소들 위를 미끄러지듯 이동하는 "창"(window)을 내어 줍니다. 두 메서드의 차이는 다음 REPL 상호작용을 보면 분명해집니다.

```scala
scala> val xs = List(1, 2, 3, 4, 5)
xs: List[Int] = List(1, 2, 3, 4, 5)
scala> val git = xs grouped 3
git: Iterator[List[Int]] = non-empty iterator
scala> git.next()
res3: List[Int] = List(1, 2, 3)
scala> git.next()
res4: List[Int] = List(4, 5)
scala> val sit = xs sliding 3
sit: Iterator[List[Int]] = non-empty iterator
scala> sit.next()
res5: List[Int] = List(1, 2, 3)
scala> sit.next()
res6: List[Int] = List(2, 3, 4)
scala> sit.next()
res7: List[Int] = List(3, 4, 5)
```

> ⚠️ **짚고 넘어가기** — `grouped(3)`은 원소를 겹치지 않게 3개씩 자릅니다(`List(1,2,3)`, `List(4,5)`). 반면 `sliding(3)`은 창을 한 칸씩 옮기며 겹치는 구간을 내어 줍니다(`List(1,2,3)`, `List(2,3,4)`, `List(3,4,5)`). 이동 평균처럼 "이웃한 원소끼리 묶어" 계산할 때는 `sliding`, 배치 처리처럼 "몫을 나눠" 처리할 때는 `grouped`가 맞는 도구입니다.

### Iterable 클래스의 연산 (Operations in Class Iterable)

| 연산 | 하는 일 |
|---|---|
| **추상 메서드:** |  |
| `xs.iterator` | `xs`의 모든 원소를 내어 주는 이터레이터입니다. |
| **그 밖의 이터레이터:** |  |
| `xs.foreach(f)` | `xs`의 모든 원소에 대해 함수 `f`를 실행합니다. |
| `xs.grouped(size)` | 이 컬렉션을 고정 크기의 "덩어리"로 내어 주는 이터레이터입니다. |
| `xs.sliding(size)` | 이 컬렉션의 원소들 위를 미끄러지는 고정 크기의 창을 내어 주는 이터레이터입니다. |
| **덧붙이기:** |  |
| `xs.concat(ys)` (또는 `xs ++ ys`) | `xs`와 `ys`의 원소를 모두 담은 컬렉션입니다. `ys`는 [IterableOnce](https://www.scala-lang.org/api/2.13.18/scala/collection/IterableOnce.html) 컬렉션, 즉 [Iterable](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterable.html)이거나 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)입니다. |
| **맵:** |  |
| `xs.map(f)` | `xs`의 모든 원소에 함수 `f`를 적용하여 얻은 컬렉션입니다. |
| `xs.flatMap(f)` | `xs`의 모든 원소에 컬렉션을 반환하는 함수 `f`를 적용한 뒤 그 결과들을 이어 붙여 얻은 컬렉션입니다. |
| `xs.collect(f)` | `xs`의 원소 중 부분 함수(partial function) `f`가 정의된 원소마다 `f`를 적용하고 그 결과를 모아 얻은 컬렉션입니다. |
| **변환:** |  |
| `xs.to(SortedSet)` | 컬렉션 팩토리(collection factory)를 매개변수로 받는 범용 변환 연산입니다. |
| `xs.toList` | 컬렉션을 리스트로 변환합니다. |
| `xs.toVector` | 컬렉션을 벡터(vector)로 변환합니다. |
| `xs.toMap` | 키/값 쌍의 컬렉션을 맵(map)으로 변환합니다. 컬렉션의 원소가 쌍이 아니라면 이 연산의 호출은 정적 타입 오류가 됩니다. |
| `xs.toSet` | 컬렉션을 집합(set)으로 변환합니다. |
| `xs.toSeq` | 컬렉션을 시퀀스(sequence)로 변환합니다. |
| `xs.toIndexedSeq` | 컬렉션을 인덱스 시퀀스(indexed sequence)로 변환합니다. |
| `xs.toBuffer` | 컬렉션을 버퍼(buffer)로 변환합니다. |
| `xs.toArray` | 컬렉션을 배열로 변환합니다. |
| **복사:** |  |
| `xs copyToArray(arr, s, n)` | 컬렉션의 원소를 최대 `n`개까지 배열 `arr`의 인덱스 `s`부터 복사합니다. 뒤의 두 인자는 생략할 수 있습니다. |
| **크기 정보:** |  |
| `xs.isEmpty` | 컬렉션이 비어 있는지 검사합니다. |
| `xs.nonEmpty` | 컬렉션에 원소가 있는지 검사합니다. |
| `xs.size` | 컬렉션의 원소 개수입니다. |
| `xs.knownSize` | 원소 개수를 상수 시간에 계산할 수 있으면 그 개수를, 그렇지 않으면 `-1`을 반환합니다. |
| `xs.sizeCompare(ys)` | `xs`가 `ys` 컬렉션보다 짧으면 음수를, 길면 양수를, 크기가 같으면 `0`을 반환합니다. 컬렉션이 무한하더라도 동작합니다. 예를 들어 `LazyList.from(1) sizeCompare List(1, 2)`는 양수를 반환합니다. |
| `xs.sizeCompare(n)` | `xs`가 `n`보다 짧으면 음수를, 길면 양수를, 크기가 `n`이면 `0`을 반환합니다. 컬렉션이 무한하더라도 동작합니다. 예를 들어 `LazyList.from(1) sizeCompare 42`는 양수를 반환합니다. |
| `xs.sizeIs < 42`, `xs.sizeIs != 42` 등 | 각각 `xs.sizeCompare(42) < 0`, `xs.sizeCompare(42) != 0` 등을 더 편하게 쓸 수 있는 문법을 제공합니다. |
| **원소 조회:** |  |
| `xs.head` | 컬렉션의 첫 번째 원소입니다(순서가 정의되어 있지 않으면 임의의 원소). |
| `xs.headOption` | `xs`의 첫 번째 원소를 옵션(option) 값으로 감싼 것이며, `xs`가 비어 있으면 `None`입니다. |
| `xs.last` | 컬렉션의 마지막 원소입니다(순서가 정의되어 있지 않으면 임의의 원소). |
| `xs.lastOption` | `xs`의 마지막 원소를 옵션 값으로 감싼 것이며, `xs`가 비어 있으면 `None`입니다. |
| `xs.find(p)` | `xs`에서 `p`를 만족하는 첫 번째 원소를 담은 옵션이며, 조건에 맞는 원소가 없으면 `None`입니다. |
| **부분 컬렉션:** |  |
| `xs.tail` | `xs.head`를 제외한 컬렉션의 나머지입니다. |
| `xs.init` | `xs.last`를 제외한 컬렉션의 나머지입니다. |
| `xs.slice(from, to)` | `xs`의 특정 인덱스 범위에 속한 원소들로 이루어진 컬렉션입니다(`from`부터 시작하여 `to` 직전까지, `to`는 포함하지 않음). |
| `xs.take(n)` | `xs`의 처음 `n`개 원소로 이루어진 컬렉션입니다(순서가 정의되어 있지 않으면 임의의 원소 `n`개). |
| `xs.drop(n)` | `xs.take(n)`을 제외한 컬렉션의 나머지입니다. |
| `xs.takeWhile(p)` | 컬렉션에서 모든 원소가 `p`를 만족하는 가장 긴 접두 구간(prefix)입니다. |
| `xs.dropWhile(p)` | 모든 원소가 `p`를 만족하는 가장 긴 접두 구간을 제거한 컬렉션입니다. |
| `xs.takeRight(n)` | `xs`의 마지막 `n`개 원소로 이루어진 컬렉션입니다(순서가 정의되어 있지 않으면 임의의 원소 `n`개). |
| `xs.dropRight(n)` | `xs.takeRight(n)`을 제외한 컬렉션의 나머지입니다. |
| `xs.filter(p)` | `xs`의 원소 중 술어 `p`를 만족하는 원소들로 이루어진 컬렉션입니다. |
| `xs.withFilter(p)` | 이 컬렉션의 비엄격(non-strict) 필터입니다. 이어지는 `map`, `flatMap`, `foreach`, `withFilter` 호출은 `xs`의 원소 중 조건 `p`가 참인 원소에만 적용됩니다. |
| `xs.filterNot(p)` | `xs`의 원소 중 술어 `p`를 만족하지 않는 원소들로 이루어진 컬렉션입니다. |
| **분할:** |  |
| `xs.splitAt(n)` | `xs`를 특정 위치에서 잘라 컬렉션의 쌍 `(xs take n, xs drop n)`을 만듭니다. |
| `xs.span(p)` | `xs`를 술어에 따라 잘라 컬렉션의 쌍 `(xs takeWhile p, xs.dropWhile p)`을 만듭니다. |
| `xs.partition(p)` | `xs`를 두 컬렉션의 쌍으로 나눕니다. 하나는 술어 `p`를 만족하는 원소들, 다른 하나는 만족하지 않는 원소들로 이루어지며, 결과는 컬렉션의 쌍 `(xs filter p, xs.filterNot p)`입니다. |
| `xs.groupBy(f)` | 판별 함수(discriminator function) `f`에 따라 `xs`를 컬렉션들의 맵으로 분할합니다. |
| `xs.groupMap(f)(g)` | 판별 함수 `f`에 따라 `xs`를 컬렉션들의 맵으로 분할하고, 각 그룹의 원소마다 변환 함수 `g`를 적용합니다. |
| `xs.groupMapReduce(f)(g)(h)` | 판별 함수 `f`에 따라 `xs`를 분할한 뒤, 각 그룹의 원소마다 함수 `g`를 적용한 결과들을 함수 `h`로 결합합니다. |
| **원소 조건 검사:** |  |
| `xs.forall(p)` | 술어 `p`가 `xs`의 모든 원소에 대해 성립하는지 나타내는 불리언(boolean)입니다. |
| `xs.exists(p)` | 술어 `p`가 `xs`의 어떤 원소에 대해 성립하는지 나타내는 불리언입니다. |
| `xs.count(p)` | `xs`에서 술어 `p`를 만족하는 원소의 개수입니다. |
| **폴드:** |  |
| `xs.foldLeft(z)(op)` | `z`에서 시작하여 왼쪽에서 오른쪽으로 가면서 `xs`의 연속한 원소들 사이에 이항 연산 `op`를 적용합니다. |
| `xs.foldRight(z)(op)` | `z`에서 시작하여 오른쪽에서 왼쪽으로 가면서 `xs`의 연속한 원소들 사이에 이항 연산 `op`를 적용합니다. |
| `xs.reduceLeft(op)` | 비어 있지 않은 컬렉션 `xs`의 연속한 원소들 사이에 왼쪽에서 오른쪽으로 가면서 이항 연산 `op`를 적용합니다. |
| `xs.reduceRight(op)` | 비어 있지 않은 컬렉션 `xs`의 연속한 원소들 사이에 오른쪽에서 왼쪽으로 가면서 이항 연산 `op`를 적용합니다. |
| **특정 폴드:** |  |
| `xs.sum` | 컬렉션 `xs`의 숫자 원소 값들의 합입니다. |
| `xs.product` | 컬렉션 `xs`의 숫자 원소 값들의 곱입니다. |
| `xs.min` | 컬렉션 `xs`의 순서가 정의된 원소 값들 중 최솟값입니다. |
| `xs.max` | 컬렉션 `xs`의 순서가 정의된 원소 값들 중 최댓값입니다. |
| `xs.minOption` | `min`과 같지만 `xs`가 비어 있으면 `None`을 반환합니다. |
| `xs.maxOption` | `max`와 같지만 `xs`가 비어 있으면 `None`을 반환합니다. |
| **문자열:** |  |
| `xs.addString(b, start, sep, end)` | `xs`의 모든 원소를 구분자 `sep`으로 구분하고 문자열 `start`와 `end`로 감싼 문자열을 `StringBuilder` `b`에 추가합니다. `start`, `sep`, `end`는 모두 생략할 수 있습니다. |
| `xs.mkString(start, sep, end)` | 컬렉션을, `xs`의 모든 원소를 구분자 `sep`으로 구분하고 문자열 `start`와 `end`로 감싼 문자열로 변환합니다. `start`, `sep`, `end`는 모두 생략할 수 있습니다. |
| **집퍼(zipper):** |  |
| `xs.zip(ys)` | `xs`와 `ys`에서 서로 대응하는 원소들의 쌍으로 이루어진 컬렉션입니다. |
| `xs.zipAll(ys, x, y)` | `xs`와 `ys`에서 서로 대응하는 원소들의 쌍으로 이루어진 컬렉션이며, 더 짧은 쪽 시퀀스에 원소 `x` 또는 `y`를 덧붙여 더 긴 쪽과 길이를 맞춥니다. |
| `xs.zipWithIndex` | `xs`의 원소와 그 인덱스의 쌍으로 이루어진 컬렉션입니다. |
| **뷰:** |  |
| `xs.view` | `xs`에 대한 뷰를 만듭니다. |

> ⚠️ **짚고 넘어가기** — `head`, `last`, `min`, `max`, `reduceLeft`, `reduceRight`는 빈 컬렉션에서 호출하면 예외를 던집니다. 컬렉션이 비어 있을 가능성이 있다면 `headOption`, `lastOption`, `minOption`, `maxOption`처럼 `Option`을 돌려주는 안전한 변형을 쓰는 편이 좋습니다. 또 `size`는 `List`처럼 전체 순회가 필요한 컬렉션에서는 비용이 크므로, "길이가 42보다 큰가?" 같은 비교만 필요하다면 무한 컬렉션에서도 동작하는 `sizeIs`나 `sizeCompare`를 쓰는 것이 낫습니다.

`Iterable` 아래의 상속 계층에는 세 개의 트레이트가 있습니다. 바로 [Seq](https://www.scala-lang.org/api/2.13.18/scala/collection/Seq.html), [Set](https://www.scala-lang.org/api/2.13.18/scala/collection/Set.html), [Map](https://www.scala-lang.org/api/2.13.18/scala/collection/Map.html)입니다. `Seq`와 `Map`은 `apply` 메서드와 `isDefinedAt` 메서드를 가진 [PartialFunction](https://www.scala-lang.org/api/2.13.18/scala/PartialFunction.html) 트레이트를 구현하되, 각자 다른 방식으로 구현합니다. `Set`은 [SetOps](https://www.scala-lang.org/api/2.13.18/scala/collection/SetOps.html)로부터 `apply` 메서드를 얻습니다.

시퀀스에서 `apply`는 위치 기반 인덱싱이며, 원소 번호는 항상 `0`부터 시작합니다. 즉 `Seq(1, 2, 3)(1)`은 `2`를 돌려줍니다. 집합에서 `apply`는 멤버십 검사(membership test)입니다. 예를 들어 `Set('a', 'b', 'c')('b')`는 `true`를, `Set()('a')`는 `false`를 돌려줍니다. 마지막으로 맵에서 `apply`는 값의 선택입니다. 예를 들어 `Map('a' -> 1, 'b' -> 10, 'c' -> 100)('b')`는 `10`을 돌려줍니다.

> 📘 **처음 배우는 분께** — 스칼라에서 `xs(1)`처럼 객체를 함수처럼 호출하는 문법은 사실 `xs.apply(1)` 호출의 축약입니다. 따라서 "`Seq`, `Set`, `Map`이 `apply`를 구현한다"는 말은 세 컬렉션 모두 `컬렉션(인자)` 형태로 쓸 수 있되, 그 의미가 각각 인덱싱, 멤버십 검사, 키 조회로 다르다는 뜻입니다.

이어지는 문서들에서 이 세 가지 컬렉션 각각을 더 자세히 설명하겠습니다.
