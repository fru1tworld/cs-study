# 집합과 맵

## 집합 (Sets)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/sets.html>

- 집합(`Set`): 중복 원소를 포함하지 않는 반복 가능(`Iterable`) 컬렉션
- 집합에 대한 연산은 일반 집합·불변(immutable) 집합·가변(mutable) 집합별로 정리됨
- 연산 범주
  - 판별 연산: `contains`, `apply`, `subsetOf`
    - `contains`: 집합이 주어진 원소를 포함하는지 판별
    - 집합의 `apply` 메서드 = `contains`와 동일 → `set(elem)`은 `set contains elem`과 동일
    - 즉 집합은 자신이 포함하는 원소에 대해 true를 반환하는 판별 함수로도 사용 가능
  - 추가 연산: `incl`과 `concat`(각각 `+`와 `++`) → 하나 이상의 원소를 집합에 추가해 새 집합 생성
  - 제거 연산: `excl`과 `removedAll`(각각 `-`와 `--`) → 하나 이상의 원소를 집합에서 제거해 새 집합 생성
  - 집합 연산: 합집합·교집합·차집합
    - 문자 형태와 기호 형태 두 가지로 존재
    - 문자 버전: `intersect`, `union`, `diff`
    - 기호 버전: `&`, `|`, `&~`
    - `Set`이 `Iterable`로부터 상속받는 `++`도 `union`/`|`의 별칭으로 볼 수 있음
      - 차이: `++`는 `IterableOnce` 인자를 받음, `union`과 `|`는 집합을 인자로 받음

예:

```scala
scala> val fruit = Set("apple", "orange", "peach", "banana")
fruit: scala.collection.immutable.Set[java.lang.String] = Set(apple, orange, peach, banana)
scala> fruit("peach")
res0: Boolean = true
scala> fruit("potato")
res1: Boolean = false
```

참고 — `fruit("peach")`처럼 집합을 함수처럼 호출할 수 있는 이유: 스칼라에서 `Set[A]`가 `A => Boolean` 타입의 함수이기도 함
- `list.filter(fruit)`처럼 집합 자체를 술어(predicate)로 넘기는 관용구가 자주 쓰임

#### Set 클래스의 연산

- 판별
  - `xs contains x`: `x`가 `xs`의 원소인지 판별
  - `xs(x)`: `xs contains x`와 동일
  - `xs subsetOf ys`: `xs`가 `ys`의 부분집합인지 판별
- 추가
  - `xs concat ys` 또는 `xs ++ ys`: `xs`의 모든 원소와 `ys`의 모든 원소를 포함하는 집합
- 제거
  - `xs.empty`: `xs`와 같은 클래스의 빈 집합
- 이항 연산
  - `xs intersect ys` 또는 `xs & ys`: `xs`와 `ys`의 교집합
  - `xs union ys` 또는 `xs | ys`: `xs`와 `ys`의 합집합
  - `xs diff ys` 또는 `xs &~ ys`: `xs`와 `ys`의 차집합

불변 집합: 새로운 `Set`을 반환하는 방식으로 원소를 추가·제거하는 메서드 제공

#### immutable.Set 클래스의 연산

- 추가
  - `xs incl x` 또는 `xs + x`: `xs`의 모든 원소에 더해 `x`를 포함하는 집합
- 제거
  - `xs excl x` 또는 `xs - x`: `xs`의 모든 원소 중 `x`를 제외한 집합
  - `xs removedAll ys` 또는 `xs -- ys`: `xs`의 모든 원소 중 `ys`의 원소들을 제외한 집합

가변 집합: 여기에 더해 원소를 추가·제거·갱신하는 메서드 제공

#### mutable.Set 클래스의 연산

- 추가
  - `xs addOne x` 또는 `xs += x`: 부수 효과(side effect)로 원소 `x`를 집합 `xs`에 추가하고 `xs` 자신을 반환
  - `xs addAll ys` 또는 `xs ++= ys`: 부수 효과로 `ys`의 모든 원소를 집합 `xs`에 추가하고 `xs` 자신을 반환
  - `xs add x`: 원소 `x`를 `xs`에 추가, `x`가 이전에 집합에 없었으면 `true`, 이미 있었으면 `false` 반환
- 제거
  - `xs subtractOne x` 또는 `xs -= x`: 부수 효과로 원소 `x`를 집합 `xs`에서 제거하고 `xs` 자신을 반환
  - `xs subtractAll ys` 또는 `xs --= ys`: 부수 효과로 `ys`의 모든 원소를 집합 `xs`에서 제거하고 `xs` 자신을 반환
  - `xs remove x`: 원소 `x`를 `xs`에서 제거, 이전에 집합에 있었으면 `true`, 없었으면 `false` 반환
  - `xs filterInPlace p`: `xs`에서 술어 `p`를 만족하는 원소만 남김
  - `xs.clear()`: `xs`의 모든 원소를 제거
- 갱신
  - `xs(x) = b`: (풀어 쓰면 `xs.update(x, b)`) 불리언 인자 `b`가 `true`이면 `x`를 `xs`에 추가, 그렇지 않으면 `x`를 `xs`에서 제거
- 복제
  - `xs.clone`: `xs`와 같은 원소를 가진 새로운 가변 집합

- `s += elem`: 부수 효과로 `elem`을 집합 `s`에 추가, 변경된 집합을 결과로 반환
- `s -= elem`: `elem`을 집합에서 제거, 변경된 집합을 결과로 반환
- `+=`/`-=` 외에도 반복 가능 컬렉션이나 이터레이터(iterator)의 모든 원소를 한꺼번에 추가·제거하는 벌크 연산 `++=`와 `--=` 존재

`+=`와 `-=`라는 메서드 이름 선택 → 매우 비슷한 코드가 가변 집합과 불변 집합 어느 쪽에서든 동작 가능. 먼저 불변 집합 `s`를 사용하는 REPL 예:

```scala
scala> var s = Set(1, 2, 3)
s: scala.collection.immutable.Set[Int] = Set(1, 2, 3)
scala> s += 4
scala> s -= 2
scala> s
res2: scala.collection.immutable.Set[Int] = Set(1, 3, 4)
```

여기서는 `immutable.Set` 타입의 `var`에 대해 `+=`와 `-=`를 사용. `s += 4` 같은 문장은 `s = s + 4`의 축약형 → 집합 `s`에 대해 추가 메서드 `+`를 호출한 뒤 그 결과를 변수 `s`에 다시 대입하는 것. 이번에는 가변 집합으로 같은 상호작용:

```scala
scala> val s = collection.mutable.Set(1, 2, 3)
s: scala.collection.mutable.Set[Int] = Set(1, 2, 3)
scala> s += 4
res3: s.type = Set(1, 4, 2, 3)
scala> s -= 2
res4: s.type = Set(1, 4, 3)
```

최종 결과는 이전 상호작용과 매우 비슷함 — `Set(1, 2, 3)`으로 시작해 `Set(1, 3, 4)`로 끝남. 하지만 문장이 이전과 똑같아 보여도 실제로 하는 일은 다름 → 이제 `s += 4`는 가변 집합 값 `s`의 `+=` 메서드를 호출하여 집합을 제자리에서(in place) 변경. 마찬가지로 `s -= 2`도 같은 집합의 `-=` 메서드를 호출.

주의 — 두 예제의 `s += 4`는 겉보기에 같지만 완전히 다른 코드
- 첫 번째는 `var` + 불변 집합 → "새 집합을 만들어 변수에 재대입"
- 두 번째는 `val` + 가변 집합 → "기존 집합 객체 자체를 수정"
- 불변 집합에는 `+=`라는 메서드가 아예 없음, 스칼라 컴파일러가 `s = s + 4`로 다시 써 주기 때문에 동작
- 첫 번째 예제는 `s`가 `val`이면 컴파일되지 않음

이 두 상호작용을 비교하면 중요한 원칙 하나가 드러남 — `val`에 저장된 가변 컬렉션은 `var`에 저장된 불변 컬렉션으로 대체할 수 있고, 그 반대도 마찬가지인 경우가 많음. 단, 해당 컬렉션에 대한 별칭 참조(alias reference)가 없어서 컬렉션이 제자리에서 갱신되었는지 아니면 새 컬렉션이 생성되었는지를 관찰할 방법이 없는 한 성립.

왜 필요한가 — 이 원칙 덕분에 코드 스타일을 크게 바꾸지 않고도 가변/불변 사이를 오갈 수 있음
- 예: 처음에는 성능 때문에 가변 집합으로 작성했다가, 나중에 스레드 안전성이나 추론 용이성을 위해 불변 집합으로 바꿔도 `+=`/`-=`를 쓰는 코드는 거의 그대로 동작
- 단, 컬렉션을 다른 곳에서도 참조하고 있다면(별칭) 두 방식의 차이가 겉으로 드러나므로 주의 필요

가변 집합은 `+=`와 `-=`의 변형인 `add`와 `remove`도 제공. 차이점: `add`와 `remove`는 해당 연산이 집합에 실제로 효과가 있었는지를 나타내는 불리언 결과를 반환.

- 가변 집합의 기본 구현: 해시테이블(hashtable)을 사용해 원소 저장
- 불변 집합의 기본 구현: 원소 개수에 따라 적응하는 표현 방식
  - 빈 집합: 싱글턴(singleton) 객체 하나로 표현
  - 크기 4 이하: 모든 원소를 필드로 저장하는 단일 객체로 표현
  - 그보다 큰 경우: 압축 해시-배열 매핑 접두사 트리(Compressed Hash-Array Mapped Prefix-tree, CHAMP)로 구현

이러한 표현 방식 선택의 결과 → 크기가 작은 집합(대략 4개 이하)에서는 불변 집합이 보통 가변 집합보다 더 압축적이고 효율적 → 집합의 크기가 작을 것으로 예상된다면 불변 집합 권장

집합의 두 가지 하위 트레이트(trait): `SortedSet`과 `BitSet`

#### 정렬된 집합 (Sorted Sets)

- `SortedSet`: (`iterator`나 `foreach`를 사용할 때) 원소를 정해진 순서로 산출하는 집합
- 순서는 집합을 생성하는 시점에 자유롭게 선택 가능
- `SortedSet`의 기본 표현: 순서 이진 트리(ordered binary tree)
  - 불변 조건(invariant): 어떤 노드의 왼쪽 서브트리에 있는 모든 원소가 오른쪽 서브트리에 있는 모든 원소보다 작음
  - 이 덕분에 단순한 중위 순회(in-order traversal)만으로 트리의 모든 원소를 오름차순으로 반환 가능
- 스칼라의 `immutable.TreeSet` 클래스: 레드-블랙 트리(red-black tree) 구현을 사용
  - 순서 불변 조건을 유지하는 동시에 트리를 균형 잡힌 상태로 유지 → 트리의 루트에서 리프까지의 모든 경로 길이가 최대 원소 하나만큼만 차이

참고 — 일반 `Set`(해시 기반)은 원소를 순회할 때 순서를 보장하지 않음. "원소를 항상 정렬된 순서로 꺼내고 싶다"면 `SortedSet`(구현체는 `TreeSet`) 사용
- 해시 집합의 조회는 평균 O(1), 트리 집합은 O(log n)
- 트리 집합은 그 대가로 정렬 순회와 범위 검색이 가능해짐

빈 `TreeSet`을 생성하려면 먼저 원하는 순서(ordering)를 지정 가능:

```scala
scala> val myOrdering = Ordering.fromLessThan[String](_ > _)
myOrdering: scala.math.Ordering[String] = ...
```

그런 다음 그 순서를 가진 빈 트리 집합을 생성:

```scala
scala> TreeSet.empty(myOrdering)
res1: scala.collection.immutable.TreeSet[String] = TreeSet()
```

또는 순서 인자를 생략하고 빈 집합의 원소 타입만 지정 가능 — 이 경우 해당 원소 타입의 기본 순서가 사용됨:

```scala
scala> TreeSet.empty[String]
res2: scala.collection.immutable.TreeSet[String] = TreeSet()
```

트리 집합으로부터 (예를 들어 연결이나 필터링으로) 새 집합을 만들면, 그 집합들은 원래 집합과 같은 순서를 유지. 예:

```scala
scala> res2 + "one" + "two" + "three" + "four"
res3: scala.collection.immutable.TreeSet[String] = TreeSet(four, one, three, two)
```

정렬된 집합은 원소의 범위(range) 연산도 지원
- `range` 메서드: 시작 원소부터 끝 원소 직전까지(끝 원소는 제외)의 모든 원소를 반환
- `from` 메서드: 집합의 순서 기준으로 시작 원소보다 크거나 같은 모든 원소를 반환
- 두 메서드 호출의 결과는 역시 정렬된 집합

예:

```scala
scala> res3.range("one", "two")
res4: scala.collection.immutable.TreeSet[String] = TreeSet(one, three)
scala> res3 rangeFrom "three"
res5: scala.collection.immutable.TreeSet[String] = TreeSet(three, two)
```

주의 — 위 결과가 이상해 보일 수 있는데, 이 예제의 집합은 알파벳 순이 아니라 앞서 만든 `TreeSet(four, one, three, two)`처럼 문자열의 사전 순서로 정렬됨
- `range("one", "two")`는 사전 순으로 `"one"` 이상 `"two"` 미만인 `one`, `three`를 반환
- 범위 연산의 기준은 항상 그 집합이 생성될 때 정해진 `Ordering`
- 문서 본문에서 `from`이라고 언급된 메서드는 예제에서 보듯 Scala 2.13에서는 `rangeFrom`이라는 이름으로 사용됨

#### 비트 집합 (Bitsets)

- 비트 집합(bitset): 음이 아닌 정수 원소들의 집합, 비트를 빽빽하게 담은 하나 이상의 워드(word)로 구현
- `BitSet`의 내부 표현: `Long` 배열을 사용
  - 첫 번째 `Long`: 0부터 63까지의 원소를 담당
  - 두 번째 `Long`: 64부터 127까지의 원소를 담당하는 식
  - 0부터 127 범위의 원소만 담는 불변 비트 집합은 배열을 아예 없애고 비트를 한두 개의 `Long` 필드에 직접 저장하는 최적화를 적용
- 각 `Long`의 64개 비트: 대응하는 원소가 집합에 포함되어 있으면 1로 설정, 그렇지 않으면 0으로 해제
- 비트 집합의 크기는 저장된 가장 큰 정수(`N`)에 따라 결정 → `N/64`개의 `Long` 워드, 즉 `N/8`바이트에 상태 정보를 위한 약간의 추가 바이트를 더한 값

- 비트 집합은 작은 원소를 많이 포함하는 경우 다른 집합보다 더 압축적
- 또 다른 장점: `contains`를 이용한 소속 판별이나 `+=`와 `-=`를 이용한 원소 추가·제거 같은 연산이 모두 매우 효율적

왜 필요한가 — 예를 들어 "0~10000 범위의 사용자 ID 중 활성 상태인 것들"처럼 원소가 작은 음이 아닌 정수로 한정되는 경우
- 일반 해시 집합은 원소마다 객체와 해시 버킷 비용이 듦
- 비트 집합은 원소 하나당 딱 1비트만 씀
- 소속 판별도 해싱 없이 비트 연산 한 번이면 끝남 → 조밀한 정수 집합에서는 메모리와 속도 모두에서 압도적으로 유리

---

## 맵 (Maps)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/maps.html>

---

- [Map](https://www.scala-lang.org/api/current/scala/collection/Map.html): 키(key)와 값(value)의 쌍(매핑(mapping) 또는 연관(association)이라고도 부름)으로 이루어진 [Iterable](https://www.scala-lang.org/api/current/scala/collection/Iterable.html)
- Scala의 [Predef](https://www.scala-lang.org/api/current/scala/Predef$.html) 객체: 쌍 `(key, value)`를 `key -> value`라는 대체 문법으로 쓸 수 있게 해주는 암시적 변환(implicit conversion) 제공
  - 예: `Map("x" -> 24, "y" -> 25, "z" -> 26)`은 `Map(("x", 24), ("y", 25), ("z", 26))`과 정확히 같은 의미, 읽기에 더 좋음

참고 — 맵은 다른 언어의 딕셔너리(Python의 `dict`), 해시맵(Java의 `HashMap`)에 해당하는 자료구조. 키로 값을 찾는 "키 → 값" 대응표로 이해하면 됨
- Scala에서 `->`는 특별한 키워드가 아니라 그냥 튜플 `(key, value)`를 만드는 메서드

맵의 기본 연산은 집합(set)의 연산과 비슷. 범주:

- 조회(lookup) 연산: `apply`, `get`, `getOrElse`, `contains`, `isDefinedAt`
  - 맵을 키에서 값으로 가는 부분 함수(partial function)로 만들어 줌
  - 가장 기본적인 조회 메서드: `def get(key): Option[Value]`
  - `m.get(key)`: 맵이 주어진 `key`에 대한 연관을 담고 있는지 검사 → 담고 있으면 연관된 값을 `Some`에 감싸서 반환, 정의되어 있지 않으면 `None` 반환
  - `apply` 메서드: 주어진 키에 연관된 값을 `Option`으로 감싸지 않고 직접 반환, 키가 정의되어 있지 않으면 예외 발생
- 추가와 갱신(additions and updates): `+`, `++`, `updated` → 새 바인딩(binding) 추가 또는 기존 바인딩 변경
- 제거(removals): `-`, `--` → 바인딩 제거
- 하위 컬렉션 생성(subcollection producers): `keys`, `keySet`, `keysIterator`, `values`, `valuesIterator` → 맵의 키와 값을 다양한 형태로 분리해서 반환
- 변환(transformations): `filterKeys`, `mapValues` → 기존 맵의 바인딩을 필터링하거나 변환하여 새 맵 생성

주의 — "맵이 부분 함수가 된다"는 말은 비유가 아니라 문자 그대로. Scala의 `Map[K, V]`는 실제로 `PartialFunction[K, V]`를 구현 → 맵 자체를 함수처럼 넘길 수 있음(예: `List("x", "y").map(myMap)`)
- 다만 `apply`(즉 `ms(k)`)는 키가 없으면 `NoSuchElementException`을 던짐 → 키 존재가 보장되지 않는 상황에서는 `get`이나 `getOrElse` 사용 권장

#### Map 클래스의 연산

- 조회
  - `ms.get(k)`: 맵 `ms`에서 키 `k`에 연관된 값을 옵션(option)으로 반환, 찾지 못하면 `None` 반환
  - `ms(k)`: (풀어서 쓰면 `ms.apply(k)`) 맵 `ms`에서 키 `k`에 연관된 값을 반환, 찾지 못하면 예외를 던짐
  - `ms.getOrElse(k, d)`: 맵 `ms`에서 키 `k`에 연관된 값을 반환, 찾지 못하면 기본값 `d` 반환
  - `ms.contains(k)`: `ms`가 키 `k`에 대한 매핑을 담고 있는지 검사
  - `ms.isDefinedAt(k)`: `contains`와 동일
- 하위 컬렉션
  - `ms.keys`: `ms`의 각 키를 담은 이터러블(iterable)
  - `ms.keySet`: `ms`의 각 키를 담은 집합
  - `ms.keysIterator`: `ms`의 각 키를 내어 주는 이터레이터(iterator)
  - `ms.values`: `ms`에서 키에 연관된 각 값을 담은 이터러블
  - `ms.valuesIterator`: `ms`에서 키에 연관된 각 값을 내어 주는 이터레이터
- 변환
  - `ms.view.filterKeys(p)`: `ms`의 매핑 중 키가 술어(predicate) `p`를 만족하는 것만 담은 맵 뷰(view)
  - `ms.view.mapValues(f)`: `ms`에서 키에 연관된 각 값에 함수 `f`를 적용한 결과로 얻는 맵 뷰

주의 — Scala 2.13부터 `filterKeys`와 `mapValues`는 맵에서 직접 호출하는 형태가 폐기(deprecated) → `ms.view.filterKeys(p)`, `ms.view.mapValues(f)`처럼 `.view`를 거쳐 호출해야 함
- 반환값도 실제 맵이 아니라 뷰, 즉 지연 평가되는 래퍼
- 원본 맵이 바뀌면 뷰의 결과도 바뀌고, 값을 읽을 때마다 함수가 다시 실행됨
- 진짜 새 맵이 필요하면 뒤에 `.toMap`을 붙임

불변(immutable) 맵: 여기에 더해 새 `Map`을 반환하는 방식으로 매핑을 추가·제거하는 연산 지원

#### immutable.Map 클래스의 연산

- 추가와 갱신
  - `ms.updated(k, v)` 또는 `ms + (k -> v)`: `ms`의 모든 매핑에 더해, 키 `k`에서 값 `v`로 가는 매핑 `k -> v`를 담은 맵
- 제거
  - `ms.removed(k)` 또는 `ms - k`: 키 `k`에 대한 매핑을 제외한 `ms`의 모든 매핑을 담은 맵
  - `ms.removedAll(ks)` 또는 `ms -- ks`: 키가 `ks`에 속하는 매핑을 제외한 `ms`의 모든 매핑을 담은 맵

왜 필요한가 — 불변 맵의 추가/제거 연산이 항상 "새 맵"을 반환하는 이유: 원본을 절대 건드리지 않기 위함
- 여러 스레드가 같은 맵을 공유해도 잠금(lock) 없이 안전, "추가 전 상태"와 "추가 후 상태"를 동시에 들고 있을 수 있음
- 매번 전체를 복사하는 것은 아니고, 내부적으로 대부분의 구조를 원본과 공유(구조 공유, structural sharing) → 생각보다 비용이 크지 않음

가변(mutable) 맵: 여기에 더해 다음 연산 지원

#### mutable.Map 클래스의 연산

- 추가와 갱신
  - `ms(k) = v`: (풀어서 쓰면 `ms.update(k, v)`) 부수 효과(side effect)로 맵 `ms`에 키 `k`에서 값 `v`로 가는 매핑을 추가, `k`에 대한 기존 매핑이 있으면 덮어씀
  - `ms.addOne(k -> v)` 또는 `ms += (k -> v)`: 부수 효과로 맵 `ms`에 키 `k`에서 값 `v`로 가는 매핑을 추가하고 `ms` 자신을 반환
  - `ms.addAll(kvs)` 또는 `ms ++= kvs`: 부수 효과로 `kvs`의 모든 매핑을 `ms`에 추가하고 `ms` 자신을 반환
  - `ms.put(k, v)`: 키 `k`에서 값 `v`로 가는 매핑을 `ms`에 추가, 이전에 `k`에 연관되어 있던 값을 옵션으로 반환
  - `ms.getOrElseUpdate(k, d)`: 맵 `ms`에 키 `k`가 정의되어 있으면 그에 연관된 값 반환, 그렇지 않으면 `ms`를 매핑 `k -> d`로 갱신하고 `d` 반환
- 제거
  - `ms.subtractOne(k)` 또는 `ms -= k`: 부수 효과로 `ms`에서 키 `k`에 대한 매핑을 제거하고 `ms` 자신을 반환
  - `ms.subtractAll(ks)` 또는 `ms --= ks`: 부수 효과로 `ms`에서 `ks`의 모든 키를 제거하고 `ms` 자신을 반환
  - `ms.remove(k)`: `ms`에서 키 `k`에 대한 매핑을 제거, 이전에 `k`에 연관되어 있던 값을 옵션으로 반환
  - `ms.filterInPlace(p)`: `ms`에서 키가 술어 `p`를 만족하는 매핑만 남김
  - `ms.clear()`: `ms`에서 모든 매핑을 제거
- 변환
  - `ms.mapValuesInPlace(f)`: 맵 `ms`에서 키에 연관된 모든 값을 함수 `f`로 변환
- 복제
  - `ms.clone`: `ms`와 같은 매핑을 담은 새 가변 맵을 반환

맵의 추가·제거 연산은 집합의 연산과 그대로 대응. 가변 맵 `m`은 보통 `m(key) = value` 또는 `m += (key -> value)`라는 두 가지 형태로 제자리에서(in place) 갱신. `m.put(key, value)`라는 형태도 존재 — 이 연산은 이전에 `key`에 연관되어 있던 값을 담은 `Option` 값을 반환, 그 키가 맵에 없었다면 `None` 반환.

`getOrElseUpdate`는 캐시(cache) 역할을 하는 맵에 접근할 때 유용. 함수 `f`를 호출하면 실행 비용이 큰 계산이 있다고 가정.

Scala 2

```scala
scala> def f(x: String): String = {
          println("taking my time."); Thread.sleep(100)
          x.reverse
        }
f: (x: String)String
```

Scala 3

```scala
scala> def f(x: String): String =
         println("taking my time."); Thread.sleep(100)
         x.reverse

def f(x: String): String
```

나아가 `f`에 부수 효과가 없어서, 같은 인자로 다시 호출하면 항상 같은 결과가 나온다고 가정. 이 경우 이전에 계산한 인자와 `f`의 결과 바인딩을 맵에 저장해 두고, 인자에 대한 결과가 맵에서 발견되지 않을 때만 `f`의 결과를 계산 → 시간 절약 가능. 이 맵을 함수 `f`의 계산에 대한 캐시라고 할 수 있음.

Scala 2와 3

```scala
scala> val cache = collection.mutable.Map[String, String]()
cache: scala.collection.mutable.Map[String,String] = Map()
```

이제 더 효율적인, 캐시를 활용하는 버전의 `f` 함수를 만들 수 있음.

Scala 2와 3

```scala
scala> def cachedF(s: String): String = cache.getOrElseUpdate(s, f(s))
cachedF: (s: String)String
scala> cachedF("abc")
taking my time.
res3: String = cba
scala> cachedF("abc")
res4: String = cba
```

`getOrElseUpdate`의 두 번째 인자는 이름에 의한(by-name) 전달이라는 점에 주의 — 따라서 위의 `f("abc")` 계산은 `getOrElseUpdate`가 두 번째 인자의 값을 실제로 필요로 할 때만, 즉 정확히 첫 번째 인자가 `cache` 맵에서 발견되지 않을 때만 수행됨. 기본 맵 연산만으로 `cachedF`를 직접 구현할 수도 있지만, 그렇게 하면 더 많은 코드가 필요함.

참고 — 이름에 의한 전달(by-name parameter): 인자를 함수에 넘기기 전에 평가하지 않고, 함수 본문 안에서 그 인자가 실제로 쓰이는 순간에 평가하는 방식(선언은 `d: => B` 형태)
- 만약 `getOrElseUpdate`의 두 번째 인자가 보통의 값 전달이었다면, 캐시에 키가 있든 없든 `f(s)`가 매번 먼저 실행되어 캐시를 두는 의미가 사라졌을 것

Scala 2

```scala
def cachedF(arg: String): String = cache.get(arg) match {
  case Some(result) => result
  case None =>
    val result = f(x)
    cache(arg) = result
    result
}
```

Scala 3

```scala
def cachedF(arg: String): String = cache.get(arg) match
  case Some(result) => result
  case None =>
    val result = f(x)
    cache(arg) = result
    result
```

왜 필요한가 — 이 패턴을 메모이제이션(memoization)이라 부름
- `getOrElseUpdate` 한 줄이 "조회 → 없으면 계산 → 저장 → 반환"이라는 네 단계를 원자적인 표현 하나로 줄여 줌
- 참고로 위 수동 구현 예제의 `f(x)`는 원문 그대로인데, 문맥상 `f(arg)`를 의도한 원문의 오탈자
- 여러 스레드가 동시에 쓰는 캐시라면 `mutable.Map` 대신 `TrieMap` 같은 동시성 맵을 고려해야 함
