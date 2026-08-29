# Iterable 트레이트와 시퀀스 트레이트

## Iterable 트레이트 (Trait Iterable)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html>

- 컬렉션 계층 구조의 최상위 트레이트(trait) = `Iterable`
- 이 트레이트의 모든 메서드는 하나의 추상 메서드(abstract method)인 `iterator`를 기반으로 정의됨 → 컬렉션의 원소를 하나씩 차례로 내어 줌

```scala
def iterator: Iterator[A]
```

- `Iterable`을 구현하는 컬렉션 클래스는 이 메서드만 정의하면 됨
- 나머지 모든 메서드는 `Iterable`로부터 상속

왜 필요한가 — 메서드 수십 개를 컬렉션마다 일일이 구현하면 새 컬렉션을 만들 때마다 엄청난 중복이 생김
- `Iterable`의 설계: "원소를 하나씩 순회하는 방법(`iterator`)만 알려 주면 `map`, `filter`, `foldLeft` 같은 나머지 연산은 전부 공짜로 제공"
- 덕분에 새로운 컬렉션을 정의하는 비용이 크게 줄고, 모든 컬렉션이 동일한 연산 집합을 일관되게 가짐

`Iterable`은 많은 구체(concrete) 메서드도 정의 → 다음 범주로 분류:

- 덧붙이기 연산 `concat`
  - 두 컬렉션을 이어 붙이거나, 이터레이터(iterator)의 모든 원소를 컬렉션 뒤에 덧붙임
- 맵 연산 `map`, `flatMap`, `collect`
  - 컬렉션의 원소에 어떤 함수를 적용하여 새로운 컬렉션을 만듦
- 변환 연산 `to`, `toList`, `toVector`, `toMap`, `toSet`, `toSeq`, `toIndexedSeq`, `toBuffer`, `toArray`
  - `Iterable` 컬렉션을 더 구체적인 무언가로 변환
  - 변환 대상이 가변(mutable) 컬렉션이면(`to(collection.mutable.X)`, `toArray`, `toBuffer`) 원본 원소를 복사하여 새 컬렉션을 만듦
  - 컬렉션의 런타임 타입이 요구된 컬렉션 타입과 이미 일치하는 경우, 수신자(receiver) 인자를 그대로 반환 → 예: 리스트에 `toList`를 적용하면 그 리스트 자신이 반환됨
- 복사 연산 `copyToArray`
  - 이름 그대로 컬렉션의 원소를 배열(array)로 복사
- 크기 정보 연산 `isEmpty`, `nonEmpty`, `size`, `knownSize`, `sizeIs`
  - 컬렉션의 원소 개수를 구하려면 경우에 따라 순회(traversal)가 필요할 수 있음(예: `List`)
  - 어떤 경우에는 컬렉션이 무한 개의 원소를 가질 수도 있음(예: `LazyList.from(1)`)
- 원소 조회 연산 `head`, `last`, `headOption`, `lastOption`, `find`
  - 컬렉션의 첫 번째 또는 마지막 원소를 선택하거나, 조건을 만족하는 첫 번째 원소를 선택
  - 모든 컬렉션에서 "첫 번째"와 "마지막"의 의미가 잘 정의되어 있는 것은 아님 → 예를 들어 해시 집합(hash set)은 원소를 해시 키에 따라 저장하는데, 이 해시 키는 실행할 때마다 달라질 수 있음 → 그 경우 해시 집합의 "첫 번째" 원소도 프로그램을 실행할 때마다 달라질 수 있음
  - 어떤 컬렉션이 항상 같은 순서로 원소를 내어 주면 그 컬렉션은 순서가 있다(ordered)고 함
  - 대부분의 컬렉션은 순서가 있지만, 일부(예: 해시 집합)는 그렇지 않음 → 순서를 포기하면 약간의 효율을 더 얻기 때문
  - 순서는 재현 가능한 테스트를 작성하고 디버깅을 돕는 데 필수적인 경우가 많음 → 그래서 스칼라 컬렉션은 모든 컬렉션 타입에 대해 순서가 있는 대안을 제공 → 예: `HashSet`의 순서 있는 대안 = `LinkedHashSet`
- 부분 컬렉션 조회 연산 `tail`, `init`, `slice`, `take`, `drop`, `takeWhile`, `dropWhile`, `filter`, `filterNot`, `withFilter`
  - 인덱스 범위나 어떤 술어(predicate)로 식별되는 부분 컬렉션을 반환
- 분할 연산 `splitAt`, `span`, `partition`, `partitionMap`, `groupBy`, `groupMap`, `groupMapReduce`
  - 이 컬렉션의 원소들을 여러 부분 컬렉션으로 나눔
- 원소 검사 연산 `exists`, `forall`, `count`
  - 주어진 술어로 컬렉션의 원소를 검사
- 폴드 연산 `foldLeft`, `foldRight`, `reduceLeft`, `reduceRight`
  - 연속한 원소들에 이항 연산(binary operation)을 적용
- 특정 폴드 연산 `sum`, `product`, `min`, `max`
  - 특정 타입(숫자 타입이거나 비교 가능한 타입)의 컬렉션에서 동작
- 문자열 연산 `mkString`, `addString`
  - 컬렉션을 문자열로 변환하는 다른 방법을 제공
- 뷰 연산
  - 뷰(view)는 지연 평가(lazy evaluation)되는 컬렉션 → 자세한 내용은 [뷰 문서](https://docs.scala-lang.org/overviews/collections-2.13/views.html) 참고

참고 — 술어(predicate)란 원소 하나를 받아 `Boolean`을 돌려주는 함수. 예를 들어 `xs.filter(x => x > 0)`에서 `x => x > 0`이 술어이고, `filter`는 이 술어가 `true`를 돌려주는 원소만 남김. 위 목록에서 `p`로 표기된 인자는 전부 술어.

`Iterable`에는 이터레이터를 반환하는 메서드가 두 개 더 있음 → `grouped`와 `sliding`.

- 이 이터레이터들은 원소를 하나씩 반환하는 것이 아니라, 원본 컬렉션 원소들의 부분 시퀀스(subsequence) 전체를 반환
- 이 부분 시퀀스의 최대 크기는 메서드의 인자로 지정
- `grouped` 메서드 = 원소들을 "덩어리"(chunk) 단위로 잘라 반환
- `sliding` 메서드 = 원소들 위를 미끄러지듯 이동하는 "창"(window)을 내어 줌
- 두 메서드의 차이는 다음 REPL 상호작용을 보면 분명해짐

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

주의 — `grouped(3)`은 원소를 겹치지 않게 3개씩 자름(`List(1,2,3)`, `List(4,5)`). 반면 `sliding(3)`은 창을 한 칸씩 옮기며 겹치는 구간을 내어 줌(`List(1,2,3)`, `List(2,3,4)`, `List(3,4,5)`). 이동 평균처럼 "이웃한 원소끼리 묶어" 계산할 때는 `sliding`, 배치 처리처럼 "몫을 나눠" 처리할 때는 `grouped`가 맞는 도구.

#### Iterable 클래스의 연산 (Operations in Class Iterable)

- 추상 메서드
  - `xs.iterator` — `xs`의 모든 원소를 내어 주는 이터레이터
- 그 밖의 이터레이터
  - `xs.foreach(f)` — `xs`의 모든 원소에 대해 함수 `f`를 실행
  - `xs.grouped(size)` — 이 컬렉션을 고정 크기의 "덩어리"로 내어 주는 이터레이터
  - `xs.sliding(size)` — 이 컬렉션의 원소들 위를 미끄러지는 고정 크기의 창을 내어 주는 이터레이터
- 덧붙이기
  - `xs.concat(ys)` (또는 `xs ++ ys`) — `xs`와 `ys`의 원소를 모두 담은 컬렉션. `ys`는 [IterableOnce](https://www.scala-lang.org/api/2.13.18/scala/collection/IterableOnce.html) 컬렉션, 즉 [Iterable](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterable.html)이거나 [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html)
- 맵
  - `xs.map(f)` — `xs`의 모든 원소에 함수 `f`를 적용하여 얻은 컬렉션
  - `xs.flatMap(f)` — `xs`의 모든 원소에 컬렉션을 반환하는 함수 `f`를 적용한 뒤 그 결과들을 이어 붙여 얻은 컬렉션
  - `xs.collect(f)` — `xs`의 원소 중 부분 함수(partial function) `f`가 정의된 원소마다 `f`를 적용하고 그 결과를 모아 얻은 컬렉션
- 변환
  - `xs.to(SortedSet)` — 컬렉션 팩토리(collection factory)를 매개변수로 받는 범용 변환 연산
  - `xs.toList` — 컬렉션을 리스트로 변환
  - `xs.toVector` — 컬렉션을 벡터(vector)로 변환
  - `xs.toMap` — 키/값 쌍의 컬렉션을 맵(map)으로 변환. 컬렉션의 원소가 쌍이 아니면 이 연산의 호출은 정적 타입 오류
  - `xs.toSet` — 컬렉션을 집합(set)으로 변환
  - `xs.toSeq` — 컬렉션을 시퀀스(sequence)로 변환
  - `xs.toIndexedSeq` — 컬렉션을 인덱스 시퀀스(indexed sequence)로 변환
  - `xs.toBuffer` — 컬렉션을 버퍼(buffer)로 변환
  - `xs.toArray` — 컬렉션을 배열로 변환
- 복사
  - `xs copyToArray(arr, s, n)` — 컬렉션의 원소를 최대 `n`개까지 배열 `arr`의 인덱스 `s`부터 복사. 뒤의 두 인자는 생략 가능
- 크기 정보
  - `xs.isEmpty` — 컬렉션이 비어 있는지 검사
  - `xs.nonEmpty` — 컬렉션에 원소가 있는지 검사
  - `xs.size` — 컬렉션의 원소 개수
  - `xs.knownSize` — 원소 개수를 상수 시간에 계산할 수 있으면 그 개수, 그렇지 않으면 `-1`을 반환
  - `xs.sizeCompare(ys)` — `xs`가 `ys` 컬렉션보다 짧으면 음수, 길면 양수, 크기가 같으면 `0`을 반환. 컬렉션이 무한하더라도 동작 → 예: `LazyList.from(1) sizeCompare List(1, 2)`는 양수를 반환
  - `xs.sizeCompare(n)` — `xs`가 `n`보다 짧으면 음수, 길면 양수, 크기가 `n`이면 `0`을 반환. 컬렉션이 무한하더라도 동작 → 예: `LazyList.from(1) sizeCompare 42`는 양수를 반환
  - `xs.sizeIs < 42`, `xs.sizeIs != 42` 등 — 각각 `xs.sizeCompare(42) < 0`, `xs.sizeCompare(42) != 0` 등을 더 편하게 쓸 수 있는 문법을 제공
- 원소 조회
  - `xs.head` — 컬렉션의 첫 번째 원소(순서가 정의되어 있지 않으면 임의의 원소)
  - `xs.headOption` — `xs`의 첫 번째 원소를 옵션(option) 값으로 감싼 것, `xs`가 비어 있으면 `None`
  - `xs.last` — 컬렉션의 마지막 원소(순서가 정의되어 있지 않으면 임의의 원소)
  - `xs.lastOption` — `xs`의 마지막 원소를 옵션 값으로 감싼 것, `xs`가 비어 있으면 `None`
  - `xs.find(p)` — `xs`에서 `p`를 만족하는 첫 번째 원소를 담은 옵션, 조건에 맞는 원소가 없으면 `None`
- 부분 컬렉션
  - `xs.tail` — `xs.head`를 제외한 컬렉션의 나머지
  - `xs.init` — `xs.last`를 제외한 컬렉션의 나머지
  - `xs.slice(from, to)` — `xs`의 특정 인덱스 범위에 속한 원소들로 이루어진 컬렉션(`from`부터 시작하여 `to` 직전까지, `to`는 포함하지 않음)
  - `xs.take(n)` — `xs`의 처음 `n`개 원소로 이루어진 컬렉션(순서가 정의되어 있지 않으면 임의의 원소 `n`개)
  - `xs.drop(n)` — `xs.take(n)`을 제외한 컬렉션의 나머지
  - `xs.takeWhile(p)` — 컬렉션에서 모든 원소가 `p`를 만족하는 가장 긴 접두 구간(prefix)
  - `xs.dropWhile(p)` — 모든 원소가 `p`를 만족하는 가장 긴 접두 구간을 제거한 컬렉션
  - `xs.takeRight(n)` — `xs`의 마지막 `n`개 원소로 이루어진 컬렉션(순서가 정의되어 있지 않으면 임의의 원소 `n`개)
  - `xs.dropRight(n)` — `xs.takeRight(n)`을 제외한 컬렉션의 나머지
  - `xs.filter(p)` — `xs`의 원소 중 술어 `p`를 만족하는 원소들로 이루어진 컬렉션
  - `xs.withFilter(p)` — 이 컬렉션의 비엄격(non-strict) 필터. 이어지는 `map`, `flatMap`, `foreach`, `withFilter` 호출은 `xs`의 원소 중 조건 `p`가 참인 원소에만 적용됨
  - `xs.filterNot(p)` — `xs`의 원소 중 술어 `p`를 만족하지 않는 원소들로 이루어진 컬렉션
- 분할
  - `xs.splitAt(n)` — `xs`를 특정 위치에서 잘라 컬렉션의 쌍 `(xs take n, xs drop n)`을 만듦
  - `xs.span(p)` — `xs`를 술어에 따라 잘라 컬렉션의 쌍 `(xs takeWhile p, xs.dropWhile p)`을 만듦
  - `xs.partition(p)` — `xs`를 두 컬렉션의 쌍으로 나눔(하나는 술어 `p`를 만족하는 원소들, 다른 하나는 만족하지 않는 원소들) → 결과는 컬렉션의 쌍 `(xs filter p, xs.filterNot p)`
  - `xs.groupBy(f)` — 판별 함수(discriminator function) `f`에 따라 `xs`를 컬렉션들의 맵으로 분할
  - `xs.groupMap(f)(g)` — 판별 함수 `f`에 따라 `xs`를 컬렉션들의 맵으로 분할하고, 각 그룹의 원소마다 변환 함수 `g`를 적용
  - `xs.groupMapReduce(f)(g)(h)` — 판별 함수 `f`에 따라 `xs`를 분할한 뒤, 각 그룹의 원소마다 함수 `g`를 적용한 결과들을 함수 `h`로 결합
- 원소 조건 검사
  - `xs.forall(p)` — 술어 `p`가 `xs`의 모든 원소에 대해 성립하는지 나타내는 불리언(boolean)
  - `xs.exists(p)` — 술어 `p`가 `xs`의 어떤 원소에 대해 성립하는지 나타내는 불리언
  - `xs.count(p)` — `xs`에서 술어 `p`를 만족하는 원소의 개수
- 폴드
  - `xs.foldLeft(z)(op)` — `z`에서 시작하여 왼쪽에서 오른쪽으로 가면서 `xs`의 연속한 원소들 사이에 이항 연산 `op`를 적용
  - `xs.foldRight(z)(op)` — `z`에서 시작하여 오른쪽에서 왼쪽으로 가면서 `xs`의 연속한 원소들 사이에 이항 연산 `op`를 적용
  - `xs.reduceLeft(op)` — 비어 있지 않은 컬렉션 `xs`의 연속한 원소들 사이에 왼쪽에서 오른쪽으로 가면서 이항 연산 `op`를 적용
  - `xs.reduceRight(op)` — 비어 있지 않은 컬렉션 `xs`의 연속한 원소들 사이에 오른쪽에서 왼쪽으로 가면서 이항 연산 `op`를 적용
- 특정 폴드
  - `xs.sum` — 컬렉션 `xs`의 숫자 원소 값들의 합
  - `xs.product` — 컬렉션 `xs`의 숫자 원소 값들의 곱
  - `xs.min` — 컬렉션 `xs`의 순서가 정의된 원소 값들 중 최솟값
  - `xs.max` — 컬렉션 `xs`의 순서가 정의된 원소 값들 중 최댓값
  - `xs.minOption` — `min`과 같지만 `xs`가 비어 있으면 `None`을 반환
  - `xs.maxOption` — `max`와 같지만 `xs`가 비어 있으면 `None`을 반환
- 문자열
  - `xs.addString(b, start, sep, end)` — `xs`의 모든 원소를 구분자 `sep`으로 구분하고 문자열 `start`와 `end`로 감싼 문자열을 `StringBuilder` `b`에 추가. `start`, `sep`, `end`는 모두 생략 가능
  - `xs.mkString(start, sep, end)` — 컬렉션을, `xs`의 모든 원소를 구분자 `sep`으로 구분하고 문자열 `start`와 `end`로 감싼 문자열로 변환. `start`, `sep`, `end`는 모두 생략 가능
- 집퍼(zipper)
  - `xs.zip(ys)` — `xs`와 `ys`에서 서로 대응하는 원소들의 쌍으로 이루어진 컬렉션
  - `xs.zipAll(ys, x, y)` — `xs`와 `ys`에서 서로 대응하는 원소들의 쌍으로 이루어진 컬렉션, 더 짧은 쪽 시퀀스에 원소 `x` 또는 `y`를 덧붙여 더 긴 쪽과 길이를 맞춤
  - `xs.zipWithIndex` — `xs`의 원소와 그 인덱스의 쌍으로 이루어진 컬렉션
- 뷰
  - `xs.view` — `xs`에 대한 뷰를 만듦

주의 — `head`, `last`, `min`, `max`, `reduceLeft`, `reduceRight`는 빈 컬렉션에서 호출하면 예외를 던짐. 컬렉션이 비어 있을 가능성이 있다면 `headOption`, `lastOption`, `minOption`, `maxOption`처럼 `Option`을 돌려주는 안전한 변형을 쓰는 편이 좋음. 또 `size`는 `List`처럼 전체 순회가 필요한 컬렉션에서는 비용이 크므로, "길이가 42보다 큰가?" 같은 비교만 필요하다면 무한 컬렉션에서도 동작하는 `sizeIs`나 `sizeCompare`를 쓰는 것이 낫음.

`Iterable` 아래의 상속 계층에는 세 개의 트레이트가 있음 → [Seq](https://www.scala-lang.org/api/2.13.18/scala/collection/Seq.html), [Set](https://www.scala-lang.org/api/2.13.18/scala/collection/Set.html), [Map](https://www.scala-lang.org/api/2.13.18/scala/collection/Map.html).

- `Seq`와 `Map`은 `apply` 메서드와 `isDefinedAt` 메서드를 가진 [PartialFunction](https://www.scala-lang.org/api/2.13.18/scala/PartialFunction.html) 트레이트를 구현, 각자 다른 방식으로 구현
- `Set`은 [SetOps](https://www.scala-lang.org/api/2.13.18/scala/collection/SetOps.html)로부터 `apply` 메서드를 얻음

시퀀스에서 `apply`는 위치 기반 인덱싱, 원소 번호는 항상 `0`부터 시작 → `Seq(1, 2, 3)(1)`은 `2`를 돌려줌. 집합에서 `apply`는 멤버십 검사(membership test) → 예: `Set('a', 'b', 'c')('b')`는 `true`, `Set()('a')`는 `false`. 맵에서 `apply`는 값의 선택 → 예: `Map('a' -> 1, 'b' -> 10, 'c' -> 100)('b')`는 `10`을 돌려줌.

참고 — 스칼라에서 `xs(1)`처럼 객체를 함수처럼 호출하는 문법은 사실 `xs.apply(1)` 호출의 축약. 따라서 "`Seq`, `Set`, `Map`이 `apply`를 구현한다"는 말은, 세 컬렉션 모두 `컬렉션(인자)` 형태로 쓸 수 있되 그 의미가 각각 인덱싱, 멤버십 검사, 키 조회로 다르다는 뜻.

이어지는 문서들에서 이 세 가지 컬렉션 각각을 더 자세히 다룸.

---

## 시퀀스 트레이트 Seq, IndexedSeq, LinearSeq (The sequence traits)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/seqs.html>

- `Seq` 트레이트(trait) = 시퀀스(sequence)를 나타냄
- 시퀀스는 `length`(길이)를 가지며, 원소들이 `0`부터 시작하는 고정된 인덱스 위치를 갖는 반복 가능(iterable) 컬렉션의 한 종류

참고 — 시퀀스는 "순서가 있고, 몇 번째 원소인지 번호로 가리킬 수 있는" 컬렉션. Java의 `List`, Python의 `list`에 해당하는 개념이 Scala에서는 `Seq`라는 공통 트레이트로 추상화되어 있고, `List`, `Vector`, `ArrayBuffer` 같은 구체 클래스들이 이를 구현.

시퀀스의 연산은 아래 목록에 정리, 다음 범주로 분류:

- 인덱싱과 길이(indexing and length) 연산: `apply`, `isDefinedAt`, `length`, `indices`, `lengthCompare`
  - `Seq`에서 `apply` 연산은 인덱싱을 의미 → `Seq[T]` 타입의 시퀀스는 `Int` 인자(인덱스)를 받아 `T` 타입의 시퀀스 원소를 돌려주는 부분 함수(partial function) → 즉 `Seq[T]`는 `PartialFunction[Int, T]`를 확장
  - 시퀀스의 원소는 0부터 시퀀스의 `length` 빼기 1까지 인덱싱됨
  - 시퀀스의 `length` 메서드는 일반 컬렉션의 `size` 메서드의 별칭
  - `lengthCompare` 메서드를 사용하면 시퀀스가 무한 길이라 하더라도 시퀀스의 길이를 `Int` 또는 `Iterable`과 비교 가능
- 인덱스 검색 연산(index search operations): `indexOf`, `lastIndexOf`, `indexOfSlice`, `lastIndexOfSlice`, `indexWhere`, `lastIndexWhere`, `segmentLength`
  - 주어진 값과 같거나 어떤 술어(predicate)를 만족하는 원소의 인덱스를 반환
- 추가 연산(addition operations): `prepended`, `prependedAll`, `appended`, `appendedAll`, `padTo`
  - 시퀀스의 앞이나 뒤에 원소를 추가하여 얻은 새 시퀀스를 반환
- 갱신 연산(update operations): `updated`, `patch`
  - 원래 시퀀스의 일부 원소를 교체하여 얻은 새 시퀀스를 반환
- 정렬 연산(sorting operations): `sorted`, `sortWith`, `sortBy`
  - 다양한 기준에 따라 시퀀스 원소를 정렬
- 뒤집기 연산(reversal operations): `reverse`, `reverseIterator`
  - 시퀀스 원소를 역순으로 내놓거나 처리
- 비교(comparisons): `startsWith`, `endsWith`, `contains`, `containsSlice`, `corresponds`, `search`
  - 두 시퀀스를 서로 비교하거나 시퀀스에서 원소를 검색
- 다중 집합(multiset) 연산: `intersect`, `diff`, `distinct`, `distinctBy`
  - 두 시퀀스의 원소에 대해 집합과 유사한 연산을 수행하거나 중복을 제거

참고 — 부분 함수(partial function)란 "모든 입력에 대해 정의되지는 않은 함수". 예를 들어 길이 3짜리 시퀀스는 인덱스 0, 1, 2에 대해서만 값이 있음 → 인덱스 5를 넣으면 예외 발생. `isDefinedAt(i)`는 바로 "이 인덱스에 값이 있는가"를 미리 확인하는 메서드.

시퀀스가 가변(mutable)이면, 추가로 부수 효과(side effect)를 일으키는 `update` 메서드를 제공하여 시퀀스 원소를 갱신할 수 있게 함. Scala에서 늘 그렇듯 `seq(idx) = elem` 같은 문법은 `seq.update(idx, elem)`의 단축 표기일 뿐 → `update`를 정의하면 편리한 대입 문법이 공짜로 따라옴. `update`와 `updated`의 차이에 주의:

- `update` — 시퀀스 원소를 제자리에서(in place) 변경, 가변 시퀀스에서만 사용 가능
- `updated` — 모든 시퀀스에서 사용 가능, 원본을 수정하는 대신 항상 새 시퀀스를 반환

주의 — `update`와 `updated`는 이름이 한 글자 차이지만 성격이 완전히 다름. `update`는 가변 컬렉션 전용으로 원본을 직접 바꾸고 아무것도 반환하지 않음(`Unit`). `updated`는 불변(immutable) 컬렉션에서도 쓸 수 있고 "해당 인덱스만 바뀐 복사본"을 새로 만들어 돌려줌. 불변 `List`에 `xs(0) = 1`을 쓰면 컴파일 오류가 나는 이유가 바로 이것.

#### Seq 클래스의 연산 (Operations in Class Seq)

- 인덱싱과 길이
  - `xs(i)` — (풀어 쓰면 `xs.apply(i)`.) `xs`의 인덱스 `i`에 있는 원소
  - `xs.isDefinedAt(i)` — `i`가 `xs.indices`에 포함되는지 검사
  - `xs.length` — 시퀀스의 길이(`size`와 동일)
  - `xs.lengthCompare(n)` — `xs`가 `n`보다 짧으면 `-1`, 길면 `+1`, 길이가 정확히 `n`이면 `0`을 반환. 시퀀스가 무한하더라도 동작 → 예: `LazyList.from(1).lengthCompare(42)`는 양수를 반환
  - `xs.indices` — `xs`의 인덱스 범위. `0`부터 `xs.length - 1`까지
- 인덱스 검색
  - `xs.indexOf(x)` — `xs`에서 `x`와 같은 첫 번째 원소의 인덱스(여러 변형이 존재)
  - `xs.lastIndexOf(x)` — `xs`에서 `x`와 같은 마지막 원소의 인덱스(여러 변형이 존재)
  - `xs.indexOfSlice(ys)` — 해당 인덱스부터 시작하는 연속된 원소들이 시퀀스 `ys`를 이루는, `xs`의 첫 번째 인덱스
  - `xs.lastIndexOfSlice(ys)` — 해당 인덱스부터 시작하는 연속된 원소들이 시퀀스 `ys`를 이루는, `xs`의 마지막 인덱스
  - `xs.indexWhere(p)` — `xs`에서 `p`를 만족하는 첫 번째 원소의 인덱스(여러 변형이 존재)
  - `xs.segmentLength(p, i)` — `xs(i)`에서 시작하여 모두 술어 `p`를 만족하는, `xs` 안에서 끊기지 않는 가장 긴 원소 구간의 길이
- 추가
  - `xs.prepended(x)` 또는 `x +: xs` — `xs` 앞에 `x`를 붙인 새 시퀀스
  - `xs.prependedAll(ys)` 또는 `ys ++: xs` — `xs` 앞에 `ys`의 모든 원소를 붙인 새 시퀀스
  - `xs.appended(x)` 또는 `xs :+ x` — `xs` 뒤에 `x`를 붙인 새 시퀀스
  - `xs.appendedAll(ys)` 또는 `xs :++ ys` — `xs` 뒤에 `ys`의 모든 원소를 붙인 새 시퀀스
  - `xs.padTo(len, x)` — 길이가 `len`이 될 때까지 `xs` 뒤에 값 `x`를 덧붙여 만든 시퀀스
- 갱신
  - `xs.patch(i, ys, r)` — `xs`의 인덱스 `i`부터 시작하는 `r`개의 원소를 패치 `ys`로 교체하여 만든 시퀀스
  - `xs.updated(i, x)` — 인덱스 `i`의 원소를 `x`로 교체한 `xs`의 복사본
  - `xs(i) = x` — (풀어 쓰면 `xs.update(i, x)`. `mutable.Seq`에서만 사용 가능.) `xs`의 인덱스 `i`에 있는 원소를 `x`로 변경
- 정렬
  - `xs.sorted` — `xs`의 원소 타입의 표준 순서(ordering)를 사용해 `xs`의 원소들을 정렬하여 얻은 새 시퀀스
  - `xs.sortWith(lt)` — `lt`를 비교 연산으로 사용해 `xs`의 원소들을 정렬하여 얻은 새 시퀀스
  - `xs.sortBy(f)` — `xs`의 원소들을 정렬하여 얻은 새 시퀀스. 두 원소의 비교는 각각에 함수 `f`를 적용한 뒤 그 결과를 비교하는 방식으로 진행
- 뒤집기
  - `xs.reverse` — `xs`의 원소들을 역순으로 담은 시퀀스
  - `xs.reverseIterator` — `xs`의 모든 원소를 역순으로 내놓는 반복자(iterator)
- 비교
  - `xs.sameElements(ys)` — `xs`와 `ys`가 같은 원소를 같은 순서로 담고 있는지 검사
  - `xs.startsWith(ys)` — `xs`가 시퀀스 `ys`로 시작하는지 검사(여러 변형이 존재)
  - `xs.endsWith(ys)` — `xs`가 시퀀스 `ys`로 끝나는지 검사(여러 변형이 존재)
  - `xs.contains(x)` — `xs`에 `x`와 같은 원소가 있는지 검사
  - `xs.search(x)` — 정렬된 시퀀스 `xs`에 `x`와 같은 원소가 있는지 검사. `xs.contains(x)`보다 더 효율적일 수 있음
  - `xs.containsSlice(ys)` — `xs`에 `ys`와 같은 연속 부분 시퀀스가 있는지 검사
  - `xs.corresponds(ys)(p)` — `xs`와 `ys`의 서로 대응하는 원소들이 이항 술어 `p`를 만족하는지 검사
- 다중 집합 연산
  - `xs.intersect(ys)` — 시퀀스 `xs`와 `ys`의 다중 집합 교집합. `xs`의 원소 순서를 유지
  - `xs.diff(ys)` — 시퀀스 `xs`와 `ys`의 다중 집합 차집합. `xs`의 원소 순서를 유지
  - `xs.distinct` — 중복 원소가 없는 `xs`의 부분 시퀀스
  - `xs.distinctBy(f)` — 변환 함수 `f`를 적용했을 때 중복 원소가 없는 `xs`의 부분 시퀀스. 예: `List("foo", "bar", "quux").distinctBy(_.length) == List("foo", "quux")`

주의 — "다중 집합"(multiset) 연산이라는 표현은, 일반 집합(set)과 달리 같은 원소가 여러 번 나올 수 있음을 뜻함. 예를 들어 `List(1, 1, 2).diff(List(1))`은 `1`을 하나만 제거해 `List(1, 2)`가 됨. 집합처럼 "1을 전부 제거"하지 않는다는 점에 주의.

`Seq` 트레이트에는 `LinearSeq`와 `IndexedSeq`라는 두 하위 트레이트가 있음. 이들은 불변 계열에 새로운 연산을 추가하지는 않지만, 서로 다른 성능 특성을 제공.

- 선형 시퀀스(linear sequence) — 효율적인 `head`와 `tail` 연산을 가짐. 자주 쓰이는 예: `scala.collection.immutable.List`, `scala.collection.immutable.LazyList`
- 인덱스 시퀀스(indexed sequence) — 효율적인 `apply`, `length`, (가변이라면) `update` 연산을 가짐. 자주 쓰이는 예: `scala.Array`, `scala.collection.mutable.ArrayBuffer`
- `Vector` 클래스는 인덱스 접근과 선형 접근 사이의 흥미로운 절충안을 제공 → 사실상 상수 시간의 인덱싱 오버헤드와 상수 시간의 선형 접근 오버헤드를 모두 가짐 → 인덱스 접근과 선형 접근이 함께 쓰이는 혼합 접근 패턴에 좋은 기반이 됨(자세한 내용은 나중에 다룸)

왜 필요한가 — `LinearSeq`와 `IndexedSeq`를 굳이 나누는 이유: 자료 구조마다 잘하는 일이 다름
- 연결 리스트(`List`)는 맨 앞에 원소를 붙이거나 떼는 것은 O(1)이지만 `xs(1000000)` 같은 인덱스 접근은 백만 번 따라가야 함
- 배열 기반 구조는 그 반대
- 이 두 트레이트는 "이 컬렉션이 어떤 접근 패턴에 유리한지"를 타입 수준에서 표현 → 코드를 보는 사람과 라이브러리 모두 알맞은 알고리즘을 선택할 수 있게 함

가변 계열에서는 `IndexedSeq`가 원소를 제자리에서 변환하는 연산들을 추가(루트 `Seq`에서 사용할 수 있는 `map`이나 `sort` 같은 변환 연산이 새 컬렉션 인스턴스를 반환하는 것과 대비됨).

##### mutable.IndexedSeq 클래스의 연산 (Operations in Class mutable.IndexedSeq)

- 변환
  - `xs.mapInPlace(f)` — `xs`의 모든 원소 각각에 함수 `f`를 적용하여 변환
  - `xs.sortInPlace()` — 컬렉션 `xs`를 정렬
  - `xs.sortInPlaceWith(c)` — 주어진 비교 함수 `c`에 따라 컬렉션 `xs`를 정렬
  - `xs.sortInPlaceBy(f)` — 각 원소에 함수 `f`를 적용한 결과에 정의된 순서에 따라 컬렉션 `xs`를 정렬

#### 버퍼 (Buffers)

가변 시퀀스의 중요한 하위 범주로 버퍼(buffer)가 있음. 버퍼는 기존 원소의 갱신뿐 아니라 원소의 추가, 삽입, 제거까지 허용. 버퍼가 지원하는 주요 새 메서드:

- 끝에 원소를 추가하는 `append`와 `appendAll`
- 앞에 추가하는 `prepend`와 `prependAll`
- 원소를 삽입하는 `insert`와 `insertAll`
- 원소를 제거하는 `remove`, `subtractOne`, `subtractAll`

이 연산들은 아래 목록에 정리.

자주 쓰이는 버퍼 구현체 두 가지 = `ListBuffer`와 `ArrayBuffer`.

- `ListBuffer` — `List`를 기반, 원소들을 `List`로 효율적으로 변환 가능
- `ArrayBuffer` — 배열을 기반, 배열로 빠르게 변환 가능

왜 필요한가 — Scala는 기본적으로 불변 컬렉션을 권장하지만, "루프를 돌며 결과를 하나씩 쌓아 올리는" 작업은 불변 컬렉션으로 하면 매번 복사가 일어나 비효율적일 수 있음. 버퍼는 이런 점진적 구축 단계에서만 가변으로 작업한 뒤, 완성되면 `toList` 등으로 불변 컬렉션으로 변환해 외부에 공개하는 패턴에 쓰임. Java의 `StringBuilder`로 문자열을 만들어 마지막에 `toString()`을 호출하는 것과 같은 발상.

##### Buffer 클래스의 연산 (Operations in Class Buffer)

- 추가
  - `buf.append(x)` 또는 `buf += x` — 버퍼 끝에 원소 `x`를 추가하고, 결과로 `buf` 자신을 반환
  - `buf.appendAll(xs)` 또는 `buf ++= xs` — `xs`의 모든 원소를 버퍼 끝에 추가
  - `buf.prepend(x)` 또는 `x +=: buf` — 버퍼 앞에 원소 `x`를 추가
  - `buf.prependAll(xs)` 또는 `xs ++=: buf` — `xs`의 모든 원소를 버퍼 앞에 추가
  - `buf.insert(i, x)` — 버퍼의 인덱스 `i`에 원소 `x`를 삽입
  - `buf.insertAll(i, xs)` — `xs`의 모든 원소를 버퍼의 인덱스 `i`에 삽입
  - `buf.padToInPlace(n, x)` — 버퍼의 원소가 총 `n`개가 될 때까지 원소 `x`를 끝에 추가
- 제거
  - `buf.subtractOne(x)` 또는 `buf -= x` — 버퍼에서 원소 `x`를 제거
  - `buf.subtractAll(xs)` 또는 `buf --= xs` — 버퍼에서 `xs`에 있는 원소들을 제거
  - `buf.remove(i)` — 버퍼에서 인덱스 `i`에 있는 원소를 제거
  - `buf.remove(i, n)` — 버퍼에서 인덱스 `i`부터 시작하는 `n`개의 원소를 제거
  - `buf.trimStart(n)` — 버퍼에서 앞의 `n`개 원소를 제거
  - `buf.trimEnd(n)` — 버퍼에서 뒤의 `n`개 원소를 제거
  - `buf.clear()` — 버퍼의 모든 원소를 제거
- 교체
  - `buf.patchInPlace(i, xs, n)` — 버퍼의 인덱스 `i`부터 시작하여(최대) `n`개의 원소를 `xs`의 원소들로 교체
- 복제
  - `buf.clone()` — `buf`와 같은 원소들을 담은 새 버퍼

주의 — 불변 `Seq`의 추가 연산자(`:+`, `+:`)와 버퍼의 연산자(`+=`, `+=:`)를 혼동하기 쉬움. 콜론(`:`)이 들어간 `:+`, `+:`는 새 시퀀스를 반환하고 원본은 그대로 둠. 등호(`=`)가 들어간 `+=`, `-=`는 버퍼 자신을 직접 변경. 또한 `+:`나 `+=:`처럼 콜론으로 끝나는 연산자는 오른쪽 피연산자의 메서드로 호출된다는 점(`x +: xs`는 `xs.prepended(x)`)도 기억해 두면 좋음.
