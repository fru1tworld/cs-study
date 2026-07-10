# Set과 Map

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/sets.html , https://docs.scala-lang.org/overviews/collections-2.13/maps.html

---

## 목차

1. [Set — 중복 없는 컬렉션](#1-set--중복-없는-컬렉션)
   - 1.1 [불변 Set과 가변 Set](#11-불변-set과-가변-set)
   - 1.2 [Set의 핵심 연산](#12-set의-핵심-연산)
   - 1.3 [집합 연산: 합집합·교집합·차집합](#13-집합-연산-합집합교집합차집합)
   - 1.4 [SortedSet과 BitSet](#14-sortedset과-bitset)
2. [Map — 키-값 컬렉션](#2-map--키-값-컬렉션)
   - 2.1 [Map 만들기와 `->` 표기법](#21-map-만들기와---표기법)
   - 2.2 [조회 연산: get, apply, getOrElse](#22-조회-연산-get-apply-getorelse)
   - 2.3 [불변 Map 갱신](#23-불변-map-갱신)
   - 2.4 [가변 Map 갱신과 `getOrElseUpdate`](#24-가변-map-갱신과-getorelseupdate)
   - 2.5 [키·값 추출과 변환](#25-키값-추출과-변환)
3. [정리: Set vs Map vs Seq](#3-정리-set-vs-map-vs-seq)

---

## 1. Set — 중복 없는 컬렉션

`Set`은 **같은 원소를 두 번 담지 않는** 컬렉션입니다. `Iterable`을 상속하므로 `map`, `filter`, `foreach` 등 다른 컬렉션과 동일한 API를 그대로 쓸 수 있고, 그 위에 "원소가 있는가"를 묻는 연산과 수학적 집합 연산(합집합·교집합·차집합)이 추가로 붙습니다.

> 📘 **처음 배우는 분께 — List와 뭐가 다른가**
>
> `List`는 순서와 중복을 그대로 유지하지만, `Set`은 **순서를 보장하지 않고 중복을 자동으로 제거**합니다. "이 원소가 들어 있는가"를 자주 물어봐야 하는 상황(예: 방문한 노드 추적, 중복 제거)에 적합합니다.

### 1.1 불변 Set과 가변 Set

Scala는 다른 컬렉션과 마찬가지로 Set도 불변(`scala.collection.immutable.Set`)과 가변(`scala.collection.mutable.Set`) 두 갈래로 나뉘며, 아무 접두어 없이 `Set`을 쓰면 불변 버전이 기본으로 임포트됩니다.

```scala
val fruit = Set("apple", "orange", "peach", "banana")
fruit("peach")   // true  — apply(x)는 contains(x)와 같음
```

- **불변 Set**: 원소를 추가/삭제하면 **새 Set을 반환**하고 원본은 그대로입니다. 내부적으로 원소 0개는 싱글턴, 4개 이하는 필드에 직접 저장, 그 이상은 압축 해시 배열 트라이(hash-array mapped trie)로 저장해 메모리를 아낍니다.
- **가변 Set**: 제자리에서(in place) 원소를 바꾸고 자기 자신을 반환합니다. 기본 구현은 해시 테이블입니다.

> 💡 **왜 필요한가 — `val` 불변 Set ↔ `var` 가변 Set 치환 원칙**
>
> 공식 문서는 "가변 컬렉션을 담은 `val`은 불변 컬렉션을 담은 `var`로 바꿔 쓸 수 있는 경우가 많다"고 설명합니다. 즉 `val s = mutable.Set(...); s += x` 대신 `var s = Set(...); s = s + x`로 써도 같은 효과를 낼 수 있습니다. 이 대체 가능성이 "왜 두 종류나 두는가"에 대한 답입니다 — 팀·상황에 따라 가변성을 어디서 관리할지 선택할 수 있게 하는 것입니다.

### 1.2 Set의 핵심 연산

| 연산 | 의미 |
|---|---|
| `xs.contains(x)` / `xs(x)` | `x`가 원소인지 (`apply`는 `contains`의 별칭) |
| `xs.subsetOf(ys)` | `xs`의 모든 원소가 `ys`에도 있는지 |
| `xs + x` (불변) / `xs incl x` | 원소 `x`를 추가한 새 Set |
| `xs - x` (불변) / `xs excl x` | 원소 `x`를 뺀 새 Set |
| `xs += x` (가변) / `xs.addOne(x)` | `xs`에 `x`를 제자리 추가 |
| `xs -= x` (가변) / `xs.subtractOne(x)` | `xs`에서 `x`를 제자리 제거 |
| `xs.add(x)` / `xs.remove(x)` (가변) | 추가/제거 성공 여부를 `Boolean`으로 반환 |

```scala
import scala.collection.mutable

val s = mutable.Set(1, 2, 3)
s += 4        // Set(1, 2, 3, 4)
s -= 2        // Set(1, 3, 4)
s.add(5)      // true  (이미 있으면 false)
```

### 1.3 집합 연산: 합집합·교집합·차집합

| 연산 | 기호 | 메서드 |
|---|---|---|
| 합집합(union) | `xs \| ys` | `xs union ys` |
| 교집합(intersection) | `xs & ys` | `xs intersect ys` |
| 차집합(difference) | `xs &~ ys` | `xs diff ys` |

```scala
val a = Set(1, 2, 3)
val b = Set(2, 3, 4)

a union b        // Set(1, 2, 3, 4)
a intersect b     // Set(2, 3)
a diff b          // Set(1)
```

> ⚠️ **짚고 넘어가기 — `--`은 차집합이 아니라 "여러 원소 제거"**
>
> `xs -- ys`는 `xs diff ys`와 결과가 같아 보이지만 의미상 목적이 다릅니다. `--`는 "`xs`에서 `ys`에 있는 원소들을 하나씩 지운다"는 **불변 컬렉션의 일괄 삭제 연산**이고, `diff`는 "두 집합의 수학적 차집합을 구한다"는 **집합론 연산**입니다. 결과 값은 같지만 읽는 사람에게 전달하는 의도가 다르므로, 집합 관점의 코드라면 `diff`를 쓰는 것이 더 명확합니다.

### 1.4 SortedSet과 BitSet

- **`SortedSet`**: 원소를 정렬된 순서로 순회할 수 있는 Set. 기본 구현인 `immutable.TreeSet`은 레드-블랙 트리를 사용합니다.

  ```scala
  import scala.collection.immutable.TreeSet

  val ts = TreeSet(3, 1, 2)     // TreeSet(1, 2, 3) — 항상 정렬된 순서
  ts.rangeFrom(2)                // TreeSet(2, 3)
  ```

  커스텀 정렬 기준이 필요하면 `TreeSet.empty(customOrdering)`처럼 `Ordering`을 직접 넘길 수 있습니다.

- **`BitSet`**: 0 이상의 정수만 담는 Set을 `Long` 배열의 비트로 표현해 메모리를 극도로 아끼는 특수 구현. `Long` 하나가 64개 정수(0~63, 64~127, ...)를 표현하므로, 정수 집합에서 포함 여부 검사와 추가/삭제가 매우 빠릅니다.

  ```scala
  import scala.collection.immutable.BitSet

  val bs = BitSet(1, 3, 5, 100)
  bs.contains(3)   // true
  ```

---

## 2. Map — 키-값 컬렉션

`Map`은 **키(key)와 값(value)의 짝**(binding/association)을 모아 둔 `Iterable`입니다. 하나의 키는 하나의 값에만 대응하며, 같은 키로 다시 넣으면 기존 값을 덮어씁니다.

### 2.1 Map 만들기와 `->` 표기법

```scala
val ages = Map("Alice" -> 30, "Bob" -> 25)
```

`"Alice" -> 30`은 튜플 `("Alice", 30)`을 만드는 `Predef`의 편의 문법입니다. `Map(("Alice", 30), ("Bob", 25))`라고 써도 동일하지만, `->` 표기가 "키가 값을 가리킨다"는 의미를 더 잘 드러냅니다.

### 2.2 조회 연산: get, apply, getOrElse

| 연산 | 반환 타입 | 키가 없을 때 |
|---|---|---|
| `m(key)` (`apply`) | `Value` | 예외(`NoSuchElementException`) |
| `m.get(key)` | `Option[Value]` | `None` |
| `m.getOrElse(key, default)` | `Value` | `default` 값 |
| `m.contains(key)` | `Boolean` | `false` |

```scala
val ages = Map("Alice" -> 30)

ages("Alice")                  // 30
ages.get("Bob")                 // None
ages.getOrElse("Bob", 0)        // 0
ages("Bob")                     // 예외 발생!
```

> 💡 **왜 필요한가 — 왜 `apply` 말고 `get`을 기본으로 쓰라고 하는가**
>
> `m(key)`는 간결하지만 키가 없으면 프로그램이 죽습니다. `get`은 "있을 수도, 없을 수도 있다"는 사실을 `Option`이라는 타입으로 코드에 그대로 드러내므로, 호출하는 쪽이 `None` 처리를 빼먹으면 컴파일러가 패턴 매칭 누락 등으로 알려줄 여지가 생깁니다. 키 존재가 보장된 상황(설정값이 항상 있는 경우 등)에서만 `apply`나 `getOrElse`를 쓰는 것이 안전합니다.

### 2.3 불변 Map 갱신

불변 `Map`의 갱신 연산은 전부 **새 Map을 반환**합니다.

| 연산 | 의미 |
|---|---|
| `m.updated(k, v)` / `m + (k -> v)` | `k`에 `v`를 대응시킨 새 Map (있으면 덮어씀) |
| `m ++ other` | 두 Map을 합친 새 Map (겹치는 키는 `other` 값 우선) |
| `m.removed(k)` / `m - k` | `k`를 뺀 새 Map |
| `m.removedAll(ks)` / `m -- ks` | 여러 키를 한 번에 뺀 새 Map |

```scala
val m1 = Map("a" -> 1, "b" -> 2)
val m2 = m1.updated("c", 3)   // Map(a -> 1, b -> 2, c -> 3), m1은 그대로
val m3 = m1 - "a"             // Map(b -> 2)
```

### 2.4 가변 Map 갱신과 `getOrElseUpdate`

가변 `Map`은 값을 제자리에서 바꿉니다.

```scala
import scala.collection.mutable

val m = mutable.Map("a" -> 1)
m("a") = 10          // update(key, value)와 동일
m += ("b" -> 2)       // 제자리 추가
m -= "a"              // 제자리 제거
```

`getOrElseUpdate`는 **캐시(cache)** 를 만들 때 특히 유용합니다. 키가 있으면 그 값을 반환하고, 없으면 두 번째 인자(기본값)를 계산해 **저장까지 한 번에** 해줍니다.

```scala
val cache = mutable.Map.empty[Int, Long]

def expensive(n: Int): Long = { /* 오래 걸리는 계산 */ n.toLong * n }

def cachedExpensive(n: Int): Long =
  cache.getOrElseUpdate(n, expensive(n))
```

> ⚠️ **짚고 넘어가기 — 두 번째 인자는 "이름에 의한 호출(by-name)"**
>
> `getOrElseUpdate(key, default)`의 `default`는 **키가 없을 때만 평가**됩니다. 즉 `expensive(n)`은 캐시에 값이 이미 있으면 아예 실행되지 않습니다. 값을 미리 계산해서 넘기는 게 아니라, "필요할 때만 계산하라"는 지연 평가 덕분에 캐시 본연의 목적(중복 계산 방지)이 성립합니다.

### 2.5 키·값 추출과 변환

| 연산 | 반환 |
|---|---|
| `m.keys` / `m.keySet` | 모든 키 (Iterable / Set) |
| `m.values` | 모든 값 |
| `m.filterKeys(pred)`(2.13에서 지원 중단 예고) | 키가 조건을 만족하는 항목만 남긴 View |
| `m.mapValues(f)`(2.13에서 지원 중단 예고) | 값에 `f`를 적용한 View |

```scala
val m = Map("a" -> 1, "b" -> 2, "c" -> 3)

m.keys                       // Iterable(a, b, c)
m.values                     // Iterable(1, 2, 3)
m.view.filterKeys(_ != "a").toMap   // Map(b -> 2, c -> 3)
m.view.mapValues(_ * 10).toMap      // Map(a -> 10, b -> 20, c -> 30)
```

> ⚠️ **짚고 넘어가기 — `filterKeys`/`mapValues`는 View를 거쳐 쓰는 게 안전**
>
> 과거 `filterKeys`, `mapValues`는 즉시 새 `Map`을 만드는 것처럼 보였지만 실제로는 **지연 평가되는 View**를 반환해 혼란을 줬습니다. Scala 2.13부터는 이 둘이 지원 중단(deprecated) 예고 상태이므로, `m.view.filterKeys(...).toMap`처럼 `view`를 명시하고 마지막에 `toMap`으로 확정 짓는 방식을 권장합니다.

`SortedMap`(예: `immutable.TreeMap`)은 `SortedSet`과 같은 원리로, 키를 정렬된 순서로 순회할 수 있는 Map입니다.

```scala
import scala.collection.immutable.TreeMap

val tm = TreeMap(3 -> "c", 1 -> "a", 2 -> "b")   // 키 순서로 정렬됨
```

---

## 3. 정리: Set vs Map vs Seq

| 컬렉션 | 중복 | 순서 | 접근 방식 |
|---|---|---|---|
| `Seq` | 허용 | 유지 | 인덱스로 접근 |
| `Set` | 불허 | 보장 안 함 | 원소 존재 여부로 접근 |
| `Map` | 키는 불허(값은 중복 가능) | 보장 안 함(SortedMap 제외) | 키로 값에 접근 |

세 컬렉션 모두 `Iterable`을 상속하므로 `map`, `filter`, `foreach`, `foldLeft` 같은 공통 연산은 그대로 통용됩니다. 다만 이번 문서에서 본 것처럼 **Set은 집합 연산(union/intersect/diff)**, **Map은 키 기반 조회·갱신**(get/getOrElse/updated)이라는 그 컬렉션만의 API가 추가로 붙는다는 점이 핵심입니다.

---

## 참고 자료

- [Scala Collections — Sets](https://docs.scala-lang.org/overviews/collections-2.13/sets.html)
- [Scala Collections — Maps](https://docs.scala-lang.org/overviews/collections-2.13/maps.html)
