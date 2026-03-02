# 단일 요소 조회

Kotlin 컬렉션은 컬렉션에서 단일 요소를 조회하기 위한 함수 세트를 제공합니다. 이 페이지에서 설명하는 함수는 리스트와 셋 모두에 적용됩니다.

[리스트의 정의](collections-overview.md#list)에서 설명한 것처럼 리스트는 순서가 있는 컬렉션입니다. 따라서 리스트의 모든 요소는 참조할 수 있는 위치를 가집니다. 이 페이지에서 설명하는 함수 외에도 리스트는 인덱스로 요소를 조회하고 검색하기 위한 더 넓은 범위의 방법을 제공합니다. 자세한 내용은 [리스트별 연산](list-operations.md)을 참조하세요.

반면, [정의](collections-overview.md#set)에 따르면 셋은 순서가 있는 컬렉션이 아닙니다. 그러나 Kotlin의 `Set`은 요소를 특정 순서로 저장합니다. 이는 구현 타입(순서를 유지하는 `LinkedHashSet`)이거나 요소 순서(순서가 없는 `HashSet`)일 수 있습니다. 셋 요소의 순서를 알 수 없는 경우에도 위치로 요소를 조회하는 것은 여전히 유용할 수 있습니다. 단지 어떤 특정 요소가 반환되는지 알 수 없을 뿐입니다. 이는 랜덤 요소를 원할 때 유용할 수 있습니다.

## 위치로 조회

정확한 위치에서 요소를 조회하려면 [`elementAt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at.html) 함수가 있습니다. `0`부터 `컬렉션 크기 - 1`까지인 정수 숫자를 인수로 호출하면 해당 위치의 컬렉션 요소를 반환합니다. 첫 번째 요소는 `0` 위치를, 마지막 요소는 `(size - 1)` 위치를 가집니다.

`elementAt()`은 인덱스 접근이나 `get()`을 지원하지 않는 컬렉션에 유용합니다. `List`의 경우 [인덱스 접근 연산자](list-operations.md#retrieve-elements-by-index)(`get()` 또는 `[]`)를 사용하는 것이 더 관용적입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = linkedSetOf("one", "two", "three", "four", "five")
    println(numbers.elementAt(3))

    val numbersSortedSet = sortedSetOf("one", "two", "three", "four")
    println(numbersSortedSet.elementAt(0)) // 요소는 오름차순으로 저장됨
//sampleEnd
}
```

컬렉션의 첫 번째 및 마지막 요소를 조회하는 유용한 별칭도 있습니다: [`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)와 [`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html).

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.first())
    println(numbers.last())
//sampleEnd
}
```

존재하지 않는 위치에서 요소를 조회할 때 예외를 피하려면 `elementAt()`의 안전한 변형을 사용하세요:

* [`elementAtOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-null.html)은 지정된 위치가 컬렉션 범위를 벗어나면 null을 반환합니다.
* [`elementAtOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-else.html)는 추가로 `Int` 인수에 매핑되는 람다 함수를 받습니다. 컬렉션 범위를 벗어난 위치로 호출하면 `elementAtOrElse()`는 주어진 값에 대한 람다 결과를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.elementAtOrNull(5))
    println(numbers.elementAtOrElse(5) { index -> "인덱스 $index의 값이 정의되지 않음"})
//sampleEnd
}
```

## 조건으로 조회

[`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)와 [`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html) 함수는 주어진 술어와 일치하는 요소에 대해 컬렉션을 검색할 수도 있습니다. 컬렉션 요소를 테스트하는 술어와 함께 `first()`를 호출하면 람다가 `true`를 반환하는 첫 번째 요소를 받습니다. 반면, 술어와 함께 `last()`는 일치하는 마지막 요소를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.first { it.length > 3 })
    println(numbers.last { it.startsWith("f") })
//sampleEnd
}
```

술어와 일치하는 요소가 없으면 두 함수 모두 예외를 throw합니다. 이를 피하려면 대신 [`firstOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-or-null.html)과 [`lastOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last-or-null.html)을 사용하세요: 일치하는 요소가 없으면 `null`을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.firstOrNull { it.length > 6 })
//sampleEnd
}
```

이름이 상황에 더 적합한 경우 별칭을 사용하세요:

* `firstOrNull()` 대신 [`find()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find.html)를 사용
* `lastOrNull()` 대신 [`findLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find-last.html)를 사용

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.find { it % 2 == 0 })
    println(numbers.findLast { it % 2 == 0 })
//sampleEnd
}
```

## 선택자로 조회

컬렉션을 매핑한 다음 매핑된 컬렉션의 최소 요소에 해당하는 원래 요소를 조회해야 하는 경우 [`firstNotNullOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-not-null-of.html) 함수를 사용하세요.

```kotlin
fun main() {
//sampleStart
    val list = listOf<Any>(0, "true", false)
    // 각 요소를 문자열로 변환하고 필요한 길이를 가진 것을 반환
    val longEnough = list.firstNotNullOf { item -> item.toString().takeIf { it.length >= 4 } }
    println(longEnough)
//sampleEnd
}
```

## 랜덤 요소

컬렉션의 임의의 요소를 조회해야 하는 경우 [`random()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random.html) 함수를 호출하세요. 인수 없이 호출하거나 [`Random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/index.html) 객체를 무작위 소스로 사용하여 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.random())
//sampleEnd
}
```

빈 컬렉션에서 `random()`은 예외를 throw합니다. 대신 `null`을 받으려면 [`randomOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random-or-null.html)을 사용하세요.

## 존재 확인

컬렉션에 요소가 있는지 확인하려면 [`contains()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains.html) 함수를 사용하세요. 함수 인수와 `equals()`인 컬렉션 요소가 있으면 `true`를 반환합니다. `in` 키워드를 사용하여 연산자 형태로 `contains()`를 호출할 수 있습니다.

여러 인스턴스의 존재를 한 번에 확인하려면 이러한 인스턴스의 컬렉션을 인수로 사용하여 [`containsAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains-all.html)을 호출합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.contains("four"))
    println("zero" in numbers)

    println(numbers.containsAll(listOf("four", "two")))
    println(numbers.containsAll(listOf("one", "zero")))
//sampleEnd
}
```

또한 [`isEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-empty.html)와 [`isNotEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-not-empty.html)를 호출하여 컬렉션에 요소가 있는지 확인할 수 있습니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.isEmpty())
    println(numbers.isNotEmpty())

    val empty = emptyList<String>()
    println(empty.isEmpty())
    println(empty.isNotEmpty())
//sampleEnd
}
```
