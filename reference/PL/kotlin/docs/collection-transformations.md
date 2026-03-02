# 컬렉션 변환 연산

Kotlin 표준 라이브러리는 컬렉션 _변환_을 위한 확장 함수 세트를 제공합니다. 이러한 함수는 제공된 변환 규칙에 따라 기존 컬렉션에서 새 컬렉션을 구축합니다. 이 페이지에서는 사용 가능한 컬렉션 변환 함수의 개요를 제공합니다. 이러한 함수는 다음 그룹에 속합니다:

* [Map](#map)
* [Zip](#zip)
* [Associate](#associate)
* [Flatten](#flatten)
* [문자열 표현](#문자열-표현)

## Map

_매핑_ 변환은 다른 컬렉션의 요소에 대한 함수 결과로부터 컬렉션을 생성합니다. 기본 매핑 함수는 [`map()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map.html)입니다. 이는 주어진 람다 함수를 각 후속 요소에 적용하고 람다 결과의 리스트를 반환합니다. 결과의 순서는 요소의 원래 순서와 동일합니다. 요소 인덱스를 추가로 인수로 사용하는 변환을 적용하려면 [`mapIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-indexed.html)를 사용합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.map { it * 3 })
    println(numbers.mapIndexed { idx, value -> value * idx })
//sampleEnd
}
```

변환이 특정 요소에서 `null`을 생성하는 경우 `map()` 대신 [`mapNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-not-null.html) 함수 또는 `mapIndexed()` 대신 [`mapIndexedNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-indexed-not-null.html)을 호출하여 결과 컬렉션에서 null을 필터링할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.mapNotNull { if ( it == 2) null else it * 3 })
    println(numbers.mapIndexedNotNull { idx, value -> if (idx == 0) null else value * idx })
//sampleEnd
}
```

맵을 변환할 때 두 가지 옵션이 있습니다: 값을 변경하지 않고 키를 변환하거나 그 반대입니다. 주어진 변환을 키에 적용하려면 [`mapKeys()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-keys.html)를 사용하고; 반면 [`mapValues()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-values.html)는 값을 변환합니다. 두 함수 모두 맵 엔트리를 인수로 사용하는 변환을 사용하므로 키와 값 모두를 연산할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    println(numbersMap.mapKeys { it.key.uppercase() })
    println(numbersMap.mapValues { it.value + it.key.length })
//sampleEnd
}
```

## Zip

_지핑_ 변환은 두 컬렉션에서 같은 위치에 있는 요소로 쌍을 구축하는 것입니다. Kotlin 표준 라이브러리에서 이는 [`zip()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/zip.html) 확장 함수로 수행됩니다.

컬렉션이나 배열에서 다른 컬렉션(또는 배열)을 인수로 호출하면 `zip()`은 `Pair` 객체의 `List`를 반환합니다. 수신자 컬렉션의 요소가 이 쌍의 첫 번째 요소입니다.

컬렉션의 크기가 다른 경우 `zip()`의 결과는 더 작은 크기입니다; 더 큰 컬렉션의 마지막 요소는 결과에 포함되지 않습니다.

`zip()`은 중위 형태 `a zip b`로도 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val colors = listOf("red", "brown", "grey")
    val animals = listOf("fox", "bear", "wolf")
    println(colors zip animals)

    val twoAnimals = listOf("fox", "bear")
    println(colors.zip(twoAnimals))
//sampleEnd
}
```

수신자 요소와 인수 요소의 두 매개변수를 받는 변환 함수와 함께 `zip()`을 호출할 수도 있습니다. 이 경우 결과 `List`는 같은 위치에 있는 수신자와 인수 요소 쌍에서 호출된 변환 함수의 반환 값을 포함합니다.

```kotlin
fun main() {
//sampleStart
    val colors = listOf("red", "brown", "grey")
    val animals = listOf("fox", "bear", "wolf")

    println(colors.zip(animals) { color, animal -> "The ${animal.replaceFirstChar { it.uppercase() }} is $color"})
//sampleEnd
}
```

`Pair`의 `List`가 있을 때 역변환인 _언지핑_을 수행할 수 있습니다 - 이 쌍에서 두 개의 리스트를 구축합니다:

* 첫 번째 리스트는 원래 리스트에서 각 `Pair`의 첫 번째 요소를 포함합니다.
* 두 번째 리스트는 두 번째 요소를 포함합니다.

쌍의 리스트를 언지핑하려면 [`unzip()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/unzip.html)을 호출합니다.

```kotlin
fun main() {
//sampleStart
    val numberPairs = listOf("one" to 1, "two" to 2, "three" to 3, "four" to 4)
    println(numberPairs.unzip())
//sampleEnd
}
```

## Associate

_연관_ 변환은 컬렉션 요소와 그와 관련된 특정 값에서 맵을 구축할 수 있게 합니다. 다른 연관 타입에서 요소는 연관 맵의 키 또는 값이 될 수 있습니다.

기본 연관 함수 [`associateWith()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-with.html)는 원래 컬렉션의 요소가 키이고 제공된 변환 함수에서 값이 생성되는 `Map`을 생성합니다. 두 요소가 같으면 마지막 것만 맵에 남습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.associateWith { it.length })
//sampleEnd
}
```

컬렉션 요소를 값으로 하는 맵을 구축하려면 [`associateBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-by.html) 함수가 있습니다. 이는 요소의 값을 기반으로 키를 반환하는 함수를 받습니다. 두 요소의 키가 같으면 마지막 것만 맵에 남습니다.

`associateBy()`는 값 변환 함수와 함께 호출할 수도 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println(numbers.associateBy { it.first().uppercaseChar() })
    println(numbers.associateBy(keySelector = { it.first().uppercaseChar() }, valueTransform = { it.length }))
//sampleEnd
}
```

키와 값 모두 컬렉션 요소에서 어떻게든 생성되는 맵을 구축하는 또 다른 방법은 [`associate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate.html) 함수입니다. 이는 `Pair`를 반환하는 람다 함수를 받습니다: 해당 맵 엔트리의 키와 값.

`associate()`는 수명이 짧은 `Pair` 객체를 생성하여 성능에 영향을 줄 수 있습니다. 따라서 `associate()`는 성능이 중요하지 않거나 다른 옵션보다 선호되는 경우에 사용해야 합니다.

후자의 예는 키와 해당 값이 요소에서 함께 생성되는 경우입니다.

```kotlin
fun main() {
    data class FullName (val firstName: String, val lastName: String)

    fun parseFullName(fullName: String): FullName {
        val nameParts = fullName.split(" ")
        if (nameParts.size == 2) {
            return FullName(nameParts[0], nameParts[1])
        } else throw Exception("Wrong name format")
    }

//sampleStart
    val names = listOf("Alice Adams", "Brian Brown", "Clara Campbell")
    println(names.associate { name -> parseFullName(name).let { it.lastName to it.firstName } })
//sampleEnd
}
```

여기서 먼저 요소에 대해 변환 함수를 호출한 다음 해당 함수 결과의 속성에서 쌍을 구축합니다.

## Flatten

중첩된 컬렉션에 대해 연산하는 경우 중첩된 컬렉션 요소에 대한 평면 접근을 제공하는 표준 라이브러리 함수가 유용할 수 있습니다.

첫 번째 함수는 [`flatten()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/flatten.html)입니다. 컬렉션의 컬렉션, 예를 들어 `Set`의 `List`에서 호출할 수 있습니다. 이 함수는 중첩된 컬렉션의 모든 요소의 단일 `List`를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numberSets = listOf(setOf(1, 2, 3), setOf(4, 5, 6), setOf(1, 2))
    println(numberSets.flatten())
//sampleEnd
}
```

또 다른 함수인 [`flatMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/flat-map.html)은 중첩된 컬렉션을 처리하는 유연한 방법을 제공합니다. 이는 컬렉션 요소를 다른 컬렉션에 매핑하는 함수를 받습니다. 결과적으로 `flatMap()`은 모든 요소에 대한 반환 값의 단일 리스트를 반환합니다. 따라서 `flatMap()`은 `map()`(매핑 결과로 컬렉션을 사용)과 `flatten()`의 순차적 호출처럼 동작합니다.

```kotlin
data class StringContainer(val values: List<String>)

fun main() {
//sampleStart
    val containers = listOf(
        StringContainer(listOf("one", "two", "three")),
        StringContainer(listOf("four", "five", "six")),
        StringContainer(listOf("seven", "eight"))
    )
    println(containers.flatMap { it.values })
//sampleEnd
}
```

## 문자열 표현

컬렉션 내용을 읽을 수 있는 형식으로 조회해야 하는 경우 컬렉션을 문자열로 변환하는 함수를 사용하세요: [`joinToString()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to-string.html) 및 [`joinTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to.html).

`joinToString()`은 제공된 인수를 기반으로 컬렉션 요소에서 단일 `String`을 구축합니다. `joinTo()`는 동일한 작업을 수행하지만 결과를 주어진 [`Appendable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-appendable/index.html) 객체에 추가합니다.

기본 인수로 호출하면 함수는 컬렉션에서 `toString()`을 호출한 것과 유사한 결과를 반환합니다: 요소의 문자열 표현이 쉼표와 공백으로 구분된 `String`.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println(numbers)
    println(numbers.joinToString())

    val listString = StringBuffer("The list of numbers: ")
    numbers.joinTo(listString)
    println(listString)
//sampleEnd
}
```

사용자 정의 문자열 표현을 구축하려면 함수 인수 `separator`, `prefix`, `postfix`에서 매개변수를 지정할 수 있습니다. 결과 문자열은 `prefix`로 시작하고 `postfix`로 끝납니다. `separator`는 마지막 요소를 제외한 각 요소 뒤에 옵니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.joinToString(separator = " | ", prefix = "start: ", postfix = ": end"))
//sampleEnd
}
```

더 큰 컬렉션의 경우 결과에 포함할 요소 수인 `limit`를 지정할 수 있습니다. 컬렉션 크기가 `limit`를 초과하면 다른 모든 요소는 `truncated` 인수의 단일 값으로 대체됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = (1..100).toList()
    println(numbers.joinToString(limit = 10, truncated = "<...>"))
//sampleEnd
}
```

마지막으로, 요소 자체의 표현을 사용자 정의하려면 `transform` 함수를 제공합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.joinToString { "Element: ${it.uppercase()}"})
//sampleEnd
}
```
