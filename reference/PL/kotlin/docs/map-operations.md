# 맵별 연산

[맵](collections-overview.md#map)에서 키와 값의 타입은 사용자가 정의합니다. 맵 엔트리에 대한 키 기반 접근은 키로 값 조회부터 키와 값의 별도 필터링까지 다양한 맵별 처리 기능을 가능하게 합니다. 이 페이지에서는 표준 라이브러리의 맵 처리 함수를 설명합니다.

## 키와 값 조회

맵에서 값을 조회하려면 키를 인수로 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/get.html) 함수에 제공해야 합니다. 단축 `[key]` 구문도 지원됩니다. 주어진 키를 찾지 못하면 `null`을 반환합니다. 또한 맵에서 키를 찾을 수 없을 때 예외를 throw하는 [`getValue()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-value.html) 함수도 있습니다. 또한 누락된 키에 대한 기본값을 생성하는 두 가지 옵션이 더 있습니다:

* [`getOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-else.html)는 `List`와 동일하게 작동합니다: 해당 키 값에 대한 키를 찾을 수 없으면 주어진 람다에서 값을 반환합니다.
* [`getOrDefault()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-default.html)는 키를 찾을 수 없으면 지정된 기본값을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap.get("one"))
    println(numbersMap["one"])
    println(numbersMap.getOrDefault("four", 10))
    println(numbersMap["five"])               // null
    //numbersMap.getValue("six")      // 예외!
//sampleEnd
}
```

맵의 모든 키 또는 모든 값에 대한 연산을 수행하려면 각각 `keys` 및 `values` 속성에서 조회할 수 있습니다. `keys`는 맵 키의 셋이고 `values`는 맵 값의 컬렉션입니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap.keys)
    println(numbersMap.values)
//sampleEnd
}
```

## 필터링

다른 컬렉션과 마찬가지로 [`filter()`](collection-filtering.md) 함수로 맵을 [필터링](collection-filtering.md)할 수 있습니다. 맵에서 `filter()`를 호출할 때 `Pair`를 인수로 하는 술어를 전달합니다. 이를 통해 필터링 술어에서 키와 값을 모두 사용할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
    println(filteredMap)
//sampleEnd
}
```

맵을 필터링하는 두 가지 특정 방법도 있습니다: 키로 필터링과 값으로 필터링. 각각에 대한 함수가 있습니다: [`filterKeys()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-keys.html)와 [`filterValues()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-values.html). 둘 다 주어진 술어와 일치하는 엔트리의 새 맵을 반환합니다. `filterKeys()`의 술어는 요소 키만 확인하고 `filterValues()`의 술어는 값만 확인합니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredKeysMap = numbersMap.filterKeys { it.endsWith("1") }
    val filteredValuesMap = numbersMap.filterValues { it < 10 }

    println(filteredKeysMap)
    println(filteredValuesMap)
//sampleEnd
}
```

## `plus`와 `minus` 연산자

요소에 대한 키 접근 때문에 [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)와 [`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자는 다른 컬렉션과 다르게 맵에서 작동합니다. `plus`는 왼쪽 `Map`의 모든 엔트리와 오른쪽의 `Pair` 또는 다른 `Map` 요소를 포함하는 `Map`을 반환합니다. 오른쪽 피연산자에 왼쪽 `Map`에 있는 키가 있는 엔트리가 포함된 경우 결과 맵에는 오른쪽의 엔트리가 포함됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap + Pair("four", 4))
    println(numbersMap + Pair("one", 10))
    println(numbersMap + mapOf("five" to 5, "one" to 11))
//sampleEnd
}
```

`minus`는 키가 오른쪽 피연산자에서 가져오는지에 따라 왼쪽 `Map`의 엔트리에서 `Map`을 생성합니다. 따라서 오른쪽 피연산자는 단일 키이거나 키 컬렉션(리스트, 셋 등)일 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap - "one")
    println(numbersMap - listOf("two", "four"))
//sampleEnd
}
```

가변 맵에서 [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)과 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 사용에 대한 자세한 내용은 아래 [맵 쓰기 연산](#map-write-operations)을 참조하세요.

## 맵 쓰기 연산 {id="map-write-operations"}

[가변](collections-overview.md#collection-types) 맵은 맵별 쓰기 연산을 제공합니다. 이러한 연산을 사용하면 키 기반 값 접근을 사용하여 맵 내용을 변경할 수 있습니다.

맵 쓰기 연산에 대한 특정 규칙이 있습니다:

* 값은 업데이트될 수 있습니다. 반면, 키는 절대 변경되지 않습니다: 엔트리를 추가하면 키가 상수입니다.
* 각 키에는 항상 하나의 연관된 값이 있습니다. 전체 엔트리를 추가하고 제거할 수 있습니다.

다음은 가변 맵에서 사용할 수 있는 쓰기 연산에 대한 표준 라이브러리 함수 설명입니다.

### 엔트리 추가 및 업데이트

가변 맵에 새 키-값 쌍을 추가하려면 [`put()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/put.html)을 사용합니다. `LinkedHashMap`(기본 맵 구현)에 새 엔트리를 넣으면 맵을 반복할 때 마지막에 오도록 추가됩니다. 정렬된 맵에서 새 요소의 위치는 키 순서에 의해 정의됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap.put("three", 3)
    println(numbersMap)
//sampleEnd
}
```

한 번에 여러 엔트리를 추가하려면 [`putAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/put-all.html)을 사용합니다. 인수는 `Map`이거나 `Pair`의 그룹(`Iterable`, `Sequence` 또는 `Array`)일 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap.putAll(setOf("four" to 4, "five" to 5))
    println(numbersMap)
//sampleEnd
}
```

`put()`과 `putAll()` 모두 주어진 키가 맵에 이미 존재하면 값을 덮어씁니다. 따라서 이를 사용하여 맵 엔트리의 값을 업데이트할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    val previousValue = numbersMap.put("one", 11)
    println("이전에 'one'과 연관된 값: $previousValue, 이후: ${numbersMap["one"]}")
    println(numbersMap)
//sampleEnd
}
```

단축 연산자 형태를 사용하여 맵에 새 엔트리를 추가할 수도 있습니다. 두 가지 방법이 있습니다:

* [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`) 연산자.
* `[]` 연산자 - `set()`의 별칭.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap["three"] = 3     // numbersMap.put("three", 3) 호출
    numbersMap += mapOf("four" to 4, "five" to 5)
    println(numbersMap)
//sampleEnd
}
```

맵에 있는 키로 호출하면 연산자는 해당 엔트리의 값을 덮어씁니다.

### 엔트리 제거

가변 맵에서 엔트리를 제거하려면 [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/remove.html) 함수를 사용합니다. `remove()`를 호출할 때 키나 전체 키-값 쌍을 전달할 수 있습니다. 키와 값을 모두 지정하면 이 키의 값이 두 번째 인수와 일치하는 경우에만 요소가 제거됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap.remove("one")
    println(numbersMap)
    numbersMap.remove("three", 4)            // 아무것도 제거하지 않음
    println(numbersMap)
//sampleEnd
}
```

가변 맵의 `keys` 또는 `values`에서 키나 값을 제거하여 엔트리를 제거할 수도 있습니다. `values`에서 `remove()`를 호출하면 주어진 값과 일치하는 첫 번째 엔트리만 제거됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3, "threeAgain" to 3)
    numbersMap.keys.remove("one")
    println(numbersMap)
    numbersMap.values.remove(3)
    println(numbersMap)
//sampleEnd
}
```

가변 맵에서 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자도 사용할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap -= "two"
    println(numbersMap)
    numbersMap -= "five"             // 아무것도 제거하지 않음
    println(numbersMap)
//sampleEnd
}
```
