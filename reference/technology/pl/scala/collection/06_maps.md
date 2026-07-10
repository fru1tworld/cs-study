# 맵 (Maps)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/maps.html>

---

[Map](https://www.scala-lang.org/api/current/scala/collection/Map.html)은 키(key)와 값(value)의 쌍(*매핑*(mapping) 또는 *연관*(association)이라고도 부릅니다)으로 이루어진 [Iterable](https://www.scala-lang.org/api/current/scala/collection/Iterable.html)입니다. Scala의 [Predef](https://www.scala-lang.org/api/current/scala/Predef$.html) 객체는 쌍 `(key, value)`를 `key -> value`라는 대체 문법으로 쓸 수 있게 해주는 암시적 변환(implicit conversion)을 제공합니다. 예를 들어 `Map("x" -> 24, "y" -> 25, "z" -> 26)`은 `Map(("x", 24), ("y", 25), ("z", 26))`과 정확히 같은 의미이지만, 읽기에 더 좋습니다.

> 📘 **처음 배우는 분께** — 맵은 다른 언어의 딕셔너리(Python의 `dict`), 해시맵(Java의 `HashMap`)에 해당하는 자료구조입니다. 키로 값을 찾는 "키 → 값" 대응표라고 생각하면 됩니다. Scala에서 `->`는 특별한 키워드가 아니라 그냥 튜플 `(key, value)`를 만드는 메서드라는 점이 특징입니다.

맵의 기본 연산은 집합(set)의 연산과 비슷합니다. 이 연산들은 아래 표에 정리되어 있으며, 다음과 같은 범주로 나뉩니다.

- **조회**(lookup) 연산 — `apply`, `get`, `getOrElse`, `contains`, `isDefinedAt`. 이 연산들은 맵을 키에서 값으로 가는 부분 함수(partial function)로 만들어 줍니다. 맵의 가장 기본적인 조회 메서드는 `def get(key): Option[Value]`입니다. `m.get(key)` 연산은 맵이 주어진 `key`에 대한 연관을 담고 있는지 검사합니다. 담고 있다면 연관된 값을 `Some`에 감싸서 반환합니다. 맵에 해당 키가 정의되어 있지 않으면 `get`은 `None`을 반환합니다. 맵은 주어진 키에 연관된 값을 `Option`으로 감싸지 않고 직접 반환하는 `apply` 메서드도 정의합니다. 맵에 그 키가 정의되어 있지 않으면 예외가 발생합니다.
- **추가와 갱신**(additions and updates) — `+`, `++`, `updated`. 맵에 새 바인딩(binding)을 추가하거나 기존 바인딩을 변경할 수 있게 해줍니다.
- **제거**(removals) — `-`, `--`. 맵에서 바인딩을 제거합니다.
- **하위 컬렉션 생성**(subcollection producers) — `keys`, `keySet`, `keysIterator`, `values`, `valuesIterator`. 맵의 키와 값을 다양한 형태로 분리해서 반환합니다.
- **변환**(transformations) — `filterKeys`, `mapValues`. 기존 맵의 바인딩을 필터링하거나 변환하여 새 맵을 만들어 냅니다.

> ⚠️ **짚고 넘어가기** — "맵이 부분 함수가 된다"는 말은 비유가 아니라 문자 그대로입니다. Scala의 `Map[K, V]`는 실제로 `PartialFunction[K, V]`를 구현하므로, 맵 자체를 함수처럼 넘길 수 있습니다(예: `List("x", "y").map(myMap)`). 다만 `apply`(즉 `ms(k)`)는 키가 없으면 `NoSuchElementException`을 던지므로, 키 존재가 보장되지 않는 상황에서는 `get`이나 `getOrElse`를 쓰는 것이 안전합니다.

### Map 클래스의 연산

| 연산 | 하는 일 |
| --- | --- |
| **조회:** |  |
| `ms.get(k)` | 맵 `ms`에서 키 `k`에 연관된 값을 옵션(option)으로 반환하며, 찾지 못하면 `None`을 반환합니다. |
| `ms(k)` | (풀어서 쓰면 `ms.apply(k)`) 맵 `ms`에서 키 `k`에 연관된 값을 반환하며, 찾지 못하면 예외를 던집니다. |
| `ms.getOrElse(k, d)` | 맵 `ms`에서 키 `k`에 연관된 값을 반환하며, 찾지 못하면 기본값 `d`를 반환합니다. |
| `ms.contains(k)` | `ms`가 키 `k`에 대한 매핑을 담고 있는지 검사합니다. |
| `ms.isDefinedAt(k)` | `contains`와 같습니다. |
| **하위 컬렉션:** |  |
| `ms.keys` | `ms`의 각 키를 담은 이터러블(iterable)입니다. |
| `ms.keySet` | `ms`의 각 키를 담은 집합입니다. |
| `ms.keysIterator` | `ms`의 각 키를 내어 주는 이터레이터(iterator)입니다. |
| `ms.values` | `ms`에서 키에 연관된 각 값을 담은 이터러블입니다. |
| `ms.valuesIterator` | `ms`에서 키에 연관된 각 값을 내어 주는 이터레이터입니다. |
| **변환:** |  |
| `ms.view.filterKeys(p)` | `ms`의 매핑 중 키가 술어(predicate) `p`를 만족하는 것만 담은 맵 뷰(view)입니다. |
| `ms.view.mapValues(f)` | `ms`에서 키에 연관된 각 값에 함수 `f`를 적용한 결과로 얻는 맵 뷰입니다. |

> ⚠️ **짚고 넘어가기** — Scala 2.13부터 `filterKeys`와 `mapValues`는 맵에서 직접 호출하는 형태가 폐기(deprecated)되었고, 표에 나온 것처럼 `ms.view.filterKeys(p)`, `ms.view.mapValues(f)`처럼 `.view`를 거쳐 호출해야 합니다. 반환값도 실제 맵이 아니라 **뷰**, 즉 지연 평가되는 래퍼입니다. 원본 맵이 바뀌면 뷰의 결과도 바뀌고, 값을 읽을 때마다 함수가 다시 실행됩니다. 진짜 새 맵이 필요하면 뒤에 `.toMap`을 붙이세요.

불변(immutable) 맵은 여기에 더해, 새 `Map`을 반환하는 방식으로 매핑을 추가하고 제거하는 연산을 지원합니다. 다음 표에 정리되어 있습니다.

### immutable.Map 클래스의 연산

| 연산 | 하는 일 |
| --- | --- |
| **추가와 갱신:** |  |
| `ms.updated(k, v)` 또는 `ms + (k -> v)` | `ms`의 모든 매핑에 더해, 키 `k`에서 값 `v`로 가는 매핑 `k -> v`를 담은 맵입니다. |
| **제거:** |  |
| `ms.removed(k)` 또는 `ms - k` | 키 `k`에 대한 매핑을 제외한 `ms`의 모든 매핑을 담은 맵입니다. |
| `ms.removedAll(ks)` 또는 `ms -- ks` | 키가 `ks`에 속하는 매핑을 제외한 `ms`의 모든 매핑을 담은 맵입니다. |

> 💡 **왜 필요한가** — 불변 맵의 추가/제거 연산이 항상 "새 맵"을 반환하는 이유는 원본을 절대 건드리지 않기 위해서입니다. 덕분에 여러 스레드가 같은 맵을 공유해도 잠금(lock) 없이 안전하고, "추가 전 상태"와 "추가 후 상태"를 동시에 들고 있을 수 있습니다. 매번 전체를 복사하는 것은 아니고, 내부적으로 대부분의 구조를 원본과 공유하기 때문에(구조 공유, structural sharing) 생각보다 비용이 크지 않습니다.

가변(mutable) 맵은 여기에 더해 다음 표에 정리된 연산을 지원합니다.

### mutable.Map 클래스의 연산

| 연산 | 하는 일 |
| --- | --- |
| **추가와 갱신:** |  |
| `ms(k) = v` | (풀어서 쓰면 `ms.update(k, v)`) 부수 효과(side effect)로 맵 `ms`에 키 `k`에서 값 `v`로 가는 매핑을 추가하며, `k`에 대한 기존 매핑이 있으면 덮어씁니다. |
| `ms.addOne(k -> v)` 또는 `ms += (k -> v)` | 부수 효과로 맵 `ms`에 키 `k`에서 값 `v`로 가는 매핑을 추가하고, `ms` 자신을 반환합니다. |
| `ms.addAll(kvs)` 또는 `ms ++= kvs` | 부수 효과로 `kvs`의 모든 매핑을 `ms`에 추가하고, `ms` 자신을 반환합니다. |
| `ms.put(k, v)` | 키 `k`에서 값 `v`로 가는 매핑을 `ms`에 추가하고, 이전에 `k`에 연관되어 있던 값을 옵션으로 반환합니다. |
| `ms.getOrElseUpdate(k, d)` | 맵 `ms`에 키 `k`가 정의되어 있으면 그에 연관된 값을 반환합니다. 그렇지 않으면 `ms`를 매핑 `k -> d`로 갱신하고 `d`를 반환합니다. |
| **제거:** |  |
| `ms.subtractOne(k)` 또는 `ms -= k` | 부수 효과로 `ms`에서 키 `k`에 대한 매핑을 제거하고, `ms` 자신을 반환합니다. |
| `ms.subtractAll(ks)` 또는 `ms --= ks` | 부수 효과로 `ms`에서 `ks`의 모든 키를 제거하고, `ms` 자신을 반환합니다. |
| `ms.remove(k)` | `ms`에서 키 `k`에 대한 매핑을 제거하고, 이전에 `k`에 연관되어 있던 값을 옵션으로 반환합니다. |
| `ms.filterInPlace(p)` | `ms`에서 키가 술어 `p`를 만족하는 매핑만 남깁니다. |
| `ms.clear()` | `ms`에서 모든 매핑을 제거합니다. |
| **변환:** |  |
| `ms.mapValuesInPlace(f)` | 맵 `ms`에서 키에 연관된 모든 값을 함수 `f`로 변환합니다. |
| **복제:** |  |
| `ms.clone` | `ms`와 같은 매핑을 담은 새 가변 맵을 반환합니다. |

맵의 추가·제거 연산은 집합의 연산과 그대로 대응합니다. 가변 맵 `m`은 보통 `m(key) = value` 또는 `m += (key -> value)`라는 두 가지 형태로 제자리에서(in place) 갱신합니다. `m.put(key, value)`라는 형태도 있는데, 이 연산은 이전에 `key`에 연관되어 있던 값을 담은 `Option` 값을 반환하며, 그 키가 맵에 없었다면 `None`을 반환합니다.

`getOrElseUpdate`는 캐시(cache) 역할을 하는 맵에 접근할 때 유용합니다. 함수 `f`를 호출하면 실행되는 비용이 큰 계산이 있다고 해봅시다.

**Scala 2**

```scala
scala> def f(x: String): String = {
          println("taking my time."); Thread.sleep(100)
          x.reverse
        }
f: (x: String)String
```

**Scala 3**

```scala
scala> def f(x: String): String =
         println("taking my time."); Thread.sleep(100)
         x.reverse

def f(x: String): String
```

나아가 `f`에 부수 효과가 없어서, 같은 인자로 다시 호출하면 항상 같은 결과가 나온다고 가정합시다. 이 경우 이전에 계산한 인자와 `f`의 결과 바인딩을 맵에 저장해 두고, 인자에 대한 결과가 맵에서 발견되지 않을 때만 `f`의 결과를 계산하면 시간을 절약할 수 있습니다. 이 맵이 함수 `f`의 계산에 대한 *캐시*라고 말할 수 있습니다.

**Scala 2와 3**

```scala
scala> val cache = collection.mutable.Map[String, String]()
cache: scala.collection.mutable.Map[String,String] = Map()
```

이제 더 효율적인, 캐시를 활용하는 버전의 `f` 함수를 만들 수 있습니다.

**Scala 2와 3**

```scala
scala> def cachedF(s: String): String = cache.getOrElseUpdate(s, f(s))
cachedF: (s: String)String
scala> cachedF("abc")
taking my time.
res3: String = cba
scala> cachedF("abc")
res4: String = cba
```

`getOrElseUpdate`의 두 번째 인자는 이름에 의한(by-name) 전달이라는 점에 주의하세요. 따라서 위의 `f("abc")` 계산은 `getOrElseUpdate`가 두 번째 인자의 값을 실제로 필요로 할 때만, 즉 정확히 첫 번째 인자가 `cache` 맵에서 발견되지 않을 때만 수행됩니다. 기본 맵 연산만으로 `cachedF`를 직접 구현할 수도 있지만, 그렇게 하면 더 많은 코드가 필요합니다.

> 📘 **처음 배우는 분께** — **이름에 의한 전달**(by-name parameter)은 인자를 함수에 넘기기 전에 평가하지 않고, 함수 본문 안에서 그 인자가 실제로 쓰이는 순간에 평가하는 방식입니다(선언은 `d: => B` 형태). 만약 `getOrElseUpdate`의 두 번째 인자가 보통의 값 전달이었다면, 캐시에 키가 있든 없든 `f(s)`가 매번 먼저 실행되어 캐시를 두는 의미가 사라졌을 것입니다.

**Scala 2**

```scala
def cachedF(arg: String): String = cache.get(arg) match {
  case Some(result) => result
  case None =>
    val result = f(x)
    cache(arg) = result
    result
}
```

**Scala 3**

```scala
def cachedF(arg: String): String = cache.get(arg) match
  case Some(result) => result
  case None =>
    val result = f(x)
    cache(arg) = result
    result
```

> 💡 **왜 필요한가** — 이 패턴을 메모이제이션(memoization)이라고 부릅니다. `getOrElseUpdate` 한 줄이 "조회 → 없으면 계산 → 저장 → 반환"이라는 네 단계를 원자적인 표현 하나로 줄여 줍니다. 참고로 위 수동 구현 예제의 `f(x)`는 원문 그대로인데, 문맥상 `f(arg)`를 의도한 원문의 오탈자입니다. 또한 여러 스레드가 동시에 쓰는 캐시라면 `mutable.Map` 대신 `TrieMap` 같은 동시성 맵을 고려해야 합니다.
