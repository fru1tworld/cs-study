# 시퀀스 트레이트 Seq, IndexedSeq, LinearSeq (The sequence traits)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/seqs.html>

`Seq` 트레이트(trait)는 시퀀스(sequence)를 나타냅니다. 시퀀스는 `length`(길이)를 가지며, 원소들이 `0`부터 시작하는 고정된 인덱스 위치를 갖는 반복 가능(iterable) 컬렉션의 한 종류입니다.

> 📘 **처음 배우는 분께** — 시퀀스는 "순서가 있고, 몇 번째 원소인지 번호로 가리킬 수 있는" 컬렉션입니다. Java의 `List`, Python의 `list`에 해당하는 개념이 Scala에서는 `Seq`라는 공통 트레이트로 추상화되어 있고, `List`, `Vector`, `ArrayBuffer` 같은 구체 클래스들이 이를 구현합니다.

시퀀스의 연산은 아래 표에 정리되어 있으며, 다음과 같은 범주로 나뉩니다.

- **인덱싱과 길이**(indexing and length) 연산: `apply`, `isDefinedAt`, `length`, `indices`, `lengthCompare`. `Seq`에서 `apply` 연산은 인덱싱을 의미합니다. 따라서 `Seq[T]` 타입의 시퀀스는 `Int` 인자(인덱스)를 받아 `T` 타입의 시퀀스 원소를 돌려주는 부분 함수(partial function)입니다. 다시 말해 `Seq[T]`는 `PartialFunction[Int, T]`를 확장합니다. 시퀀스의 원소는 0부터 시퀀스의 `length` 빼기 1까지 인덱싱됩니다. 시퀀스의 `length` 메서드는 일반 컬렉션의 `size` 메서드의 별칭입니다. `lengthCompare` 메서드를 사용하면 시퀀스가 무한 길이라 하더라도 시퀀스의 길이를 `Int` 또는 `Iterable`과 비교할 수 있습니다.
- **인덱스 검색 연산**(index search operations): `indexOf`, `lastIndexOf`, `indexOfSlice`, `lastIndexOfSlice`, `indexWhere`, `lastIndexWhere`, `segmentLength`. 주어진 값과 같거나 어떤 술어(predicate)를 만족하는 원소의 인덱스를 반환합니다.
- **추가 연산**(addition operations): `prepended`, `prependedAll`, `appended`, `appendedAll`, `padTo`. 시퀀스의 앞이나 뒤에 원소를 추가하여 얻은 새 시퀀스를 반환합니다.
- **갱신 연산**(update operations): `updated`, `patch`. 원래 시퀀스의 일부 원소를 교체하여 얻은 새 시퀀스를 반환합니다.
- **정렬 연산**(sorting operations): `sorted`, `sortWith`, `sortBy`. 다양한 기준에 따라 시퀀스 원소를 정렬합니다.
- **뒤집기 연산**(reversal operations): `reverse`, `reverseIterator`. 시퀀스 원소를 역순으로 내놓거나 처리합니다.
- **비교**(comparisons): `startsWith`, `endsWith`, `contains`, `containsSlice`, `corresponds`, `search`. 두 시퀀스를 서로 비교하거나 시퀀스에서 원소를 검색합니다.
- **다중 집합**(multiset) 연산: `intersect`, `diff`, `distinct`, `distinctBy`. 두 시퀀스의 원소에 대해 집합과 유사한 연산을 수행하거나 중복을 제거합니다.

> 📘 **처음 배우는 분께** — 부분 함수(partial function)란 "모든 입력에 대해 정의되지는 않은 함수"입니다. 예를 들어 길이 3짜리 시퀀스는 인덱스 0, 1, 2에 대해서만 값이 있으므로, 인덱스 5를 넣으면 예외가 발생합니다. `isDefinedAt(i)`는 바로 "이 인덱스에 값이 있는가"를 미리 확인하는 메서드입니다.

시퀀스가 가변(mutable)이라면, 추가로 부수 효과(side effect)를 일으키는 `update` 메서드를 제공하여 시퀀스 원소를 갱신할 수 있게 합니다. Scala에서 늘 그렇듯 `seq(idx) = elem` 같은 문법은 `seq.update(idx, elem)`의 단축 표기일 뿐이므로, `update`를 정의하면 편리한 대입 문법이 공짜로 따라옵니다. `update`와 `updated`의 차이에 주의하세요. `update`는 시퀀스 원소를 제자리에서(in place) 변경하며 가변 시퀀스에서만 사용할 수 있습니다. `updated`는 모든 시퀀스에서 사용할 수 있으며, 원본을 수정하는 대신 항상 새 시퀀스를 반환합니다.

> ⚠️ **짚고 넘어가기** — `update`와 `updated`는 이름이 한 글자 차이지만 성격이 완전히 다릅니다. `update`는 가변 컬렉션 전용으로 원본을 직접 바꾸고 아무것도 반환하지 않습니다(`Unit`). `updated`는 불변(immutable) 컬렉션에서도 쓸 수 있고 "해당 인덱스만 바뀐 복사본"을 새로 만들어 돌려줍니다. 불변 `List`에 `xs(0) = 1`을 쓰면 컴파일 오류가 나는 이유가 바로 이것입니다.

### Seq 클래스의 연산 (Operations in Class Seq)

| 표기 | 하는 일 |
| --- | --- |
| **인덱싱과 길이:** | |
| `xs(i)` | (풀어 쓰면 `xs.apply(i)`.) `xs`의 인덱스 `i`에 있는 원소. |
| `xs.isDefinedAt(i)` | `i`가 `xs.indices`에 포함되는지 검사합니다. |
| `xs.length` | 시퀀스의 길이 (`size`와 동일). |
| `xs.lengthCompare(n)` | `xs`가 `n`보다 짧으면 `-1`, 길면 `+1`, 길이가 정확히 `n`이면 `0`을 반환합니다. 시퀀스가 무한하더라도 동작합니다. 예를 들어 `LazyList.from(1).lengthCompare(42)`는 양수를 반환합니다. |
| `xs.indices` | `xs`의 인덱스 범위. `0`부터 `xs.length - 1`까지입니다. |
| **인덱스 검색:** | |
| `xs.indexOf(x)` | `xs`에서 `x`와 같은 첫 번째 원소의 인덱스 (여러 변형이 존재합니다). |
| `xs.lastIndexOf(x)` | `xs`에서 `x`와 같은 마지막 원소의 인덱스 (여러 변형이 존재합니다). |
| `xs.indexOfSlice(ys)` | 해당 인덱스부터 시작하는 연속된 원소들이 시퀀스 `ys`를 이루는, `xs`의 첫 번째 인덱스. |
| `xs.lastIndexOfSlice(ys)` | 해당 인덱스부터 시작하는 연속된 원소들이 시퀀스 `ys`를 이루는, `xs`의 마지막 인덱스. |
| `xs.indexWhere(p)` | `xs`에서 `p`를 만족하는 첫 번째 원소의 인덱스 (여러 변형이 존재합니다). |
| `xs.segmentLength(p, i)` | `xs(i)`에서 시작하여 모두 술어 `p`를 만족하는, `xs` 안에서 끊기지 않는 가장 긴 원소 구간의 길이. |
| **추가:** | |
| `xs.prepended(x)` 또는 `x +: xs` | `xs` 앞에 `x`를 붙인 새 시퀀스. |
| `xs.prependedAll(ys)` 또는 `ys ++: xs` | `xs` 앞에 `ys`의 모든 원소를 붙인 새 시퀀스. |
| `xs.appended(x)` 또는 `xs :+ x` | `xs` 뒤에 `x`를 붙인 새 시퀀스. |
| `xs.appendedAll(ys)` 또는 `xs :++ ys` | `xs` 뒤에 `ys`의 모든 원소를 붙인 새 시퀀스. |
| `xs.padTo(len, x)` | 길이가 `len`이 될 때까지 `xs` 뒤에 값 `x`를 덧붙여 만든 시퀀스. |
| **갱신:** | |
| `xs.patch(i, ys, r)` | `xs`의 인덱스 `i`부터 시작하는 `r`개의 원소를 패치 `ys`로 교체하여 만든 시퀀스. |
| `xs.updated(i, x)` | 인덱스 `i`의 원소를 `x`로 교체한 `xs`의 복사본. |
| `xs(i) = x` | (풀어 쓰면 `xs.update(i, x)`. `mutable.Seq`에서만 사용 가능.) `xs`의 인덱스 `i`에 있는 원소를 `x`로 변경합니다. |
| **정렬:** | |
| `xs.sorted` | `xs`의 원소 타입의 표준 순서(ordering)를 사용해 `xs`의 원소들을 정렬하여 얻은 새 시퀀스. |
| `xs.sortWith(lt)` | `lt`를 비교 연산으로 사용해 `xs`의 원소들을 정렬하여 얻은 새 시퀀스. |
| `xs.sortBy(f)` | `xs`의 원소들을 정렬하여 얻은 새 시퀀스. 두 원소의 비교는 각각에 함수 `f`를 적용한 뒤 그 결과를 비교하는 방식으로 진행됩니다. |
| **뒤집기:** | |
| `xs.reverse` | `xs`의 원소들을 역순으로 담은 시퀀스. |
| `xs.reverseIterator` | `xs`의 모든 원소를 역순으로 내놓는 반복자(iterator). |
| **비교:** | |
| `xs.sameElements(ys)` | `xs`와 `ys`가 같은 원소를 같은 순서로 담고 있는지 검사합니다. |
| `xs.startsWith(ys)` | `xs`가 시퀀스 `ys`로 시작하는지 검사합니다 (여러 변형이 존재합니다). |
| `xs.endsWith(ys)` | `xs`가 시퀀스 `ys`로 끝나는지 검사합니다 (여러 변형이 존재합니다). |
| `xs.contains(x)` | `xs`에 `x`와 같은 원소가 있는지 검사합니다. |
| `xs.search(x)` | 정렬된 시퀀스 `xs`에 `x`와 같은 원소가 있는지 검사합니다. `xs.contains(x)`보다 더 효율적일 수 있습니다. |
| `xs.containsSlice(ys)` | `xs`에 `ys`와 같은 연속 부분 시퀀스가 있는지 검사합니다. |
| `xs.corresponds(ys)(p)` | `xs`와 `ys`의 서로 대응하는 원소들이 이항 술어 `p`를 만족하는지 검사합니다. |
| **다중 집합 연산:** | |
| `xs.intersect(ys)` | 시퀀스 `xs`와 `ys`의 다중 집합 교집합. `xs`의 원소 순서를 유지합니다. |
| `xs.diff(ys)` | 시퀀스 `xs`와 `ys`의 다중 집합 차집합. `xs`의 원소 순서를 유지합니다. |
| `xs.distinct` | 중복 원소가 없는 `xs`의 부분 시퀀스. |
| `xs.distinctBy(f)` | 변환 함수 `f`를 적용했을 때 중복 원소가 없는 `xs`의 부분 시퀀스. 예: `List("foo", "bar", "quux").distinctBy(_.length) == List("foo", "quux")` |

> ⚠️ **짚고 넘어가기** — "다중 집합"(multiset) 연산이라는 표현은, 일반 집합(set)과 달리 같은 원소가 여러 번 나올 수 있음을 뜻합니다. 예를 들어 `List(1, 1, 2).diff(List(1))`은 `1`을 하나만 제거해 `List(1, 2)`가 됩니다. 집합처럼 "1을 전부 제거"하지 않는다는 점에 주의하세요.

`Seq` 트레이트에는 `LinearSeq`와 `IndexedSeq`라는 두 하위 트레이트가 있습니다. 이들은 불변 계열에 새로운 연산을 추가하지는 않지만, 서로 다른 성능 특성을 제공합니다. 선형 시퀀스(linear sequence)는 효율적인 `head`와 `tail` 연산을 갖는 반면, 인덱스 시퀀스(indexed sequence)는 효율적인 `apply`, `length`, 그리고 (가변이라면) `update` 연산을 갖습니다. 자주 쓰이는 선형 시퀀스로는 `scala.collection.immutable.List`와 `scala.collection.immutable.LazyList`가 있습니다. 자주 쓰이는 인덱스 시퀀스로는 `scala.Array`와 `scala.collection.mutable.ArrayBuffer`가 있습니다. `Vector` 클래스는 인덱스 접근과 선형 접근 사이의 흥미로운 절충안을 제공합니다. 사실상 상수 시간의 인덱싱 오버헤드와 상수 시간의 선형 접근 오버헤드를 모두 갖기 때문입니다. 그래서 벡터는 인덱스 접근과 선형 접근이 함께 쓰이는 혼합 접근 패턴에 좋은 기반이 됩니다. 벡터에 대해서는 나중에 더 자세히 배웁니다.

> 💡 **왜 필요한가** — 왜 `LinearSeq`와 `IndexedSeq`를 굳이 나눌까요? 자료 구조마다 잘하는 일이 다르기 때문입니다. 연결 리스트(`List`)는 맨 앞에 원소를 붙이거나 떼는 것은 O(1)이지만 `xs(1000000)` 같은 인덱스 접근은 백만 번 따라가야 합니다. 배열 기반 구조는 그 반대입니다. 이 두 트레이트는 "이 컬렉션이 어떤 접근 패턴에 유리한지"를 타입 수준에서 표현하여, 코드를 보는 사람과 라이브러리 모두 알맞은 알고리즘을 선택할 수 있게 합니다.

가변 계열에서는 `IndexedSeq`가 원소를 제자리에서 변환하는 연산들을 추가합니다 (루트 `Seq`에서 사용할 수 있는 `map`이나 `sort` 같은 변환 연산이 새 컬렉션 인스턴스를 반환하는 것과 대비됩니다).

#### mutable.IndexedSeq 클래스의 연산 (Operations in Class mutable.IndexedSeq)

| 표기 | 하는 일 |
| --- | --- |
| **변환:** | |
| `xs.mapInPlace(f)` | `xs`의 모든 원소 각각에 함수 `f`를 적용하여 변환합니다. |
| `xs.sortInPlace()` | 컬렉션 `xs`를 정렬합니다. |
| `xs.sortInPlaceWith(c)` | 주어진 비교 함수 `c`에 따라 컬렉션 `xs`를 정렬합니다. |
| `xs.sortInPlaceBy(f)` | 각 원소에 함수 `f`를 적용한 결과에 정의된 순서에 따라 컬렉션 `xs`를 정렬합니다. |

### 버퍼 (Buffers)

가변 시퀀스의 중요한 하위 범주로 **버퍼**(buffer)가 있습니다. 버퍼는 기존 원소의 갱신뿐 아니라 원소의 추가, 삽입, 제거까지 허용합니다. 버퍼가 지원하는 주요 새 메서드로는 끝에 원소를 추가하는 `append`와 `appendAll`, 앞에 추가하는 `prepend`와 `prependAll`, 원소를 삽입하는 `insert`와 `insertAll`, 그리고 원소를 제거하는 `remove`, `subtractOne`, `subtractAll`이 있습니다. 이 연산들은 아래 표에 정리되어 있습니다.

자주 쓰이는 버퍼 구현체 두 가지는 `ListBuffer`와 `ArrayBuffer`입니다. 이름에서 알 수 있듯 `ListBuffer`는 `List`를 기반으로 하며 원소들을 `List`로 효율적으로 변환할 수 있고, `ArrayBuffer`는 배열을 기반으로 하며 배열로 빠르게 변환할 수 있습니다.

> 💡 **왜 필요한가** — Scala는 기본적으로 불변 컬렉션을 권장하지만, "루프를 돌며 결과를 하나씩 쌓아 올리는" 작업은 불변 컬렉션으로 하면 매번 복사가 일어나 비효율적일 수 있습니다. 버퍼는 이런 **점진적 구축** 단계에서만 가변으로 작업한 뒤, 완성되면 `toList` 등으로 불변 컬렉션으로 변환해 외부에 공개하는 패턴에 쓰입니다. Java의 `StringBuilder`로 문자열을 만들어 마지막에 `toString()`을 호출하는 것과 같은 발상입니다.

#### Buffer 클래스의 연산 (Operations in Class Buffer)

| 표기 | 하는 일 |
| --- | --- |
| **추가:** | |
| `buf.append(x)` 또는 `buf += x` | 버퍼 끝에 원소 `x`를 추가하고, 결과로 `buf` 자신을 반환합니다. |
| `buf.appendAll(xs)` 또는 `buf ++= xs` | `xs`의 모든 원소를 버퍼 끝에 추가합니다. |
| `buf.prepend(x)` 또는 `x +=: buf` | 버퍼 앞에 원소 `x`를 추가합니다. |
| `buf.prependAll(xs)` 또는 `xs ++=: buf` | `xs`의 모든 원소를 버퍼 앞에 추가합니다. |
| `buf.insert(i, x)` | 버퍼의 인덱스 `i`에 원소 `x`를 삽입합니다. |
| `buf.insertAll(i, xs)` | `xs`의 모든 원소를 버퍼의 인덱스 `i`에 삽입합니다. |
| `buf.padToInPlace(n, x)` | 버퍼의 원소가 총 `n`개가 될 때까지 원소 `x`를 끝에 추가합니다. |
| **제거:** | |
| `buf.subtractOne(x)` 또는 `buf -= x` | 버퍼에서 원소 `x`를 제거합니다. |
| `buf.subtractAll(xs)` 또는 `buf --= xs` | 버퍼에서 `xs`에 있는 원소들을 제거합니다. |
| `buf.remove(i)` | 버퍼에서 인덱스 `i`에 있는 원소를 제거합니다. |
| `buf.remove(i, n)` | 버퍼에서 인덱스 `i`부터 시작하는 `n`개의 원소를 제거합니다. |
| `buf.trimStart(n)` | 버퍼에서 앞의 `n`개 원소를 제거합니다. |
| `buf.trimEnd(n)` | 버퍼에서 뒤의 `n`개 원소를 제거합니다. |
| `buf.clear()` | 버퍼의 모든 원소를 제거합니다. |
| **교체:** | |
| `buf.patchInPlace(i, xs, n)` | 버퍼의 인덱스 `i`부터 시작하여 (최대) `n`개의 원소를 `xs`의 원소들로 교체합니다. |
| **복제:** | |
| `buf.clone()` | `buf`와 같은 원소들을 담은 새 버퍼. |

> ⚠️ **짚고 넘어가기** — 불변 `Seq`의 추가 연산자(`:+`, `+:`)와 버퍼의 연산자(`+=`, `+=:`)를 혼동하기 쉽습니다. 콜론(`:`)이 들어간 `:+`, `+:`는 **새 시퀀스를 반환**하고 원본은 그대로 둡니다. 등호(`=`)가 들어간 `+=`, `-=`는 **버퍼 자신을 직접 변경**합니다. 또한 `+:`나 `+=:`처럼 콜론으로 끝나는 연산자는 오른쪽 피연산자의 메서드로 호출된다는 점(`x +: xs`는 `xs.prepended(x)`)도 기억해 두면 좋습니다.
