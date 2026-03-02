# 컬렉션 필터링

필터링은 컬렉션 처리에서 가장 인기 있는 작업 중 하나입니다. Kotlin에서 필터링 조건은 _술어(predicate)_로 정의됩니다 - 컬렉션 요소를 받아 불리언 값을 반환하는 람다 함수: `true`는 주어진 요소가 술어와 일치함을 의미하고, `false`는 반대를 의미합니다.

표준 라이브러리에는 단일 호출로 컬렉션을 필터링할 수 있는 확장 함수 그룹이 있습니다. 이러한 함수는 원래 컬렉션을 변경하지 않으므로 [가변 및 읽기 전용](collections-overview.md#collection-types) 컬렉션 모두에서 사용할 수 있습니다. 필터링 결과를 연산하려면 변수에 할당하거나 필터링 후 함수를 체이닝해야 합니다.

## 술어로 필터링

기본 필터링 함수는 [`filter()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html)입니다. 술어와 함께 호출하면 `filter()`는 일치하는 컬렉션 요소를 반환합니다. `List`와 `Set` 모두 결과 컬렉션은 `List`이고, `Map`의 경우에도 `Map`입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val longerThan3 = numbers.filter { it.length > 3 }
    println(longerThan3)

    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
    println(filteredMap)
//sampleEnd
}
```

`filter()`의 술어는 요소의 값만 확인할 수 있습니다. 필터에서 요소 위치를 사용하려면 [`filterIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-indexed.html)를 사용합니다. 이는 두 인수를 받는 술어를 받습니다: 요소의 인덱스와 값.

부정 조건으로 컬렉션을 필터링하려면 [`filterNot()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not.html)을 사용합니다. 이는 술어가 `false`를 반환하는 요소의 리스트를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    val filteredIdx = numbers.filterIndexed { index, s -> (index != 0) && (s.length < 5) }
    val filteredNot = numbers.filterNot { it.length <= 3 }

    println(filteredIdx)
    println(filteredNot)
//sampleEnd
}
```

주어진 타입의 요소를 필터링하는 함수도 있습니다:

* [`filterIsInstance()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-is-instance.html)는 주어진 타입의 컬렉션 요소를 반환합니다. `List<Any>`에서 호출하면 `filterIsInstance<T>()`는 `List<T>`를 반환하므로 `T` 타입의 함수를 항목에서 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(null, 1, "two", 3.0, "four")
    println("대문자로 된 모든 String 요소:")
    numbers.filterIsInstance<String>().forEach {
        println(it.uppercase())
    }
//sampleEnd
}
```

* [`filterNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not-null.html)은 모든 널이 아닌 요소를 반환합니다. `List<T?>`에서 호출하면 `filterNotNull()`은 `List<T: Any>`를 반환하므로 요소를 널이 아닌 객체로 취급할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(null, "one", "two", null)
    numbers.filterNotNull().forEach {
        println(it.length)   // 널이 가능한 String에는 length를 사용할 수 없음
    }
//sampleEnd
}
```

## 분할

또 다른 필터링 함수인 [`partition()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/partition.html)은 술어로 컬렉션을 필터링하고 일치하지 않는 요소를 별도의 리스트에 유지합니다. 따라서 반환 값으로 `List`의 `Pair`를 갖습니다: 첫 번째 리스트는 술어와 일치하는 요소를 포함하고 두 번째 리스트는 원래 컬렉션의 다른 모든 것을 포함합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val (match, rest) = numbers.partition { it.length > 3 }

    println(match)
    println(rest)
//sampleEnd
}
```

## 술어 테스트

마지막으로, 컬렉션 요소에 대해 술어를 단순히 테스트하는 함수가 있습니다:

* [`any()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/any.html)는 최소 하나의 요소가 주어진 술어와 일치하면 `true`를 반환합니다.
* [`none()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/none.html)은 어떤 요소도 주어진 술어와 일치하지 않으면 `true`를 반환합니다.
* [`all()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/all.html)은 모든 요소가 주어진 술어와 일치하면 `true`를 반환합니다. `all()`은 빈 컬렉션에서 유효한 술어와 함께 호출하면 `true`를 반환합니다. 이러한 동작은 논리학에서 _공허한 참(vacuous truth)_으로 알려져 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println(numbers.any { it.endsWith("e") })
    println(numbers.none { it.endsWith("a") })
    println(numbers.all { it.endsWith("e") })

    println(emptyList<Int>().all { it > 5 })   // 공허한 참
//sampleEnd
}
```

`any()`와 `none()`은 술어 없이도 사용할 수 있습니다: 이 경우 컬렉션이 비어 있는지 확인합니다. `any()`는 요소가 있으면 `true`를 반환하고 없으면 `false`를 반환합니다; `none()`은 반대로 합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val empty = emptyList<String>()

    println(numbers.any())
    println(empty.any())

    println(numbers.none())
    println(empty.none())
//sampleEnd
}
```
