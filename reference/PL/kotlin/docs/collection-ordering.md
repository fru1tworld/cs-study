# 정렬

요소의 순서는 특정 컬렉션 타입에서 중요한 측면입니다. 예를 들어, 동일한 요소를 가진 두 리스트는 요소가 다르게 정렬되면 같지 않습니다.

Kotlin에서 객체의 순서는 여러 방식으로 정의할 수 있습니다.

먼저, 대부분의 내장 타입에 대해 _자연_ 순서가 있습니다:

* 숫자 타입은 전통적인 숫자 순서를 사용합니다: `1`은 `0`보다 크고; `-3.4f`는 `-5f`보다 큽니다.
* `Char`와 `String`은 [사전순](https://en.wikipedia.org/wiki/Lexicographic_order)을 사용합니다: `b`는 `a`보다 크고; `world`는 `hello`보다 큽니다.

사용자 정의 타입에 자연 순서를 정의하려면 해당 타입을 [`Comparable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparable/index.html)의 상속자로 만듭니다. 이를 위해 `compareTo()` 함수를 구현해야 합니다. `compareTo()`는 동일한 타입의 다른 객체를 인수로 받아 어떤 객체가 더 큰지를 나타내는 정수 값을 반환해야 합니다:

* 양수는 수신자 객체가 더 크다는 것을 나타냅니다.
* 음수는 인수보다 작다는 것을 나타냅니다.
* 0은 객체가 같다는 것을 나타냅니다.

다음은 주 버전과 부 버전으로 구성된 버전을 정렬하는 데 사용할 수 있는 클래스입니다.

```kotlin
class Version(val major: Int, val minor: Int): Comparable<Version> {
    override fun compareTo(other: Version): Int = when {
        this.major != other.major -> this.major compareTo other.major
        this.minor != other.minor -> this.minor compareTo other.minor
        else -> 0
    }
}

fun main() {
    println(Version(1, 2) > Version(1, 3))
    println(Version(2, 0) > Version(1, 5))
}
```

_사용자 정의_ 순서를 사용하면 원하는 방식으로 모든 타입의 인스턴스를 정렬할 수 있습니다. 특히, 비교할 수 없는 객체에 대한 순서를 정의하거나 `Comparable`이 아닌 타입에 대한 자연 순서가 아닌 순서를 정의할 수 있습니다. 타입에 대한 사용자 정의 순서를 정의하려면 그에 대한 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html)를 만듭니다. `Comparator`에는 두 인스턴스를 비교하는 `compare()` 함수가 포함되어 있습니다: 비교 결과를 나타내는 정수를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val lengthComparator = Comparator { str1: String, str2: String -> str1.length - str2.length }
    println(listOf("aaa", "bb", "c").sortedWith(lengthComparator))
//sampleEnd
}
```

`lengthComparator`를 사용하면 기본 사전순 대신 길이로 문자열을 비교할 수 있습니다.

`Comparator`를 정의하는 더 짧은 방법은 표준 라이브러리의 [`compareBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.comparisons/compare-by.html) 함수입니다. `compareBy()`는 인스턴스에서 `Comparable` 값을 생성하는 람다 함수를 받아 사용자 정의 순서를 생성된 값의 자연 순서로 정의합니다.

`compareBy()`를 사용하면 위 예제의 길이 비교기는 다음과 같이 됩니다:

```kotlin
fun main() {
//sampleStart
    println(listOf("aaa", "bb", "c").sortedWith(compareBy { it.length }))
//sampleEnd
}
```

Kotlin 컬렉션 패키지는 자연 순서, 사용자 정의 순서 또는 랜덤 순서로 컬렉션을 정렬하는 함수를 제공합니다. 이 페이지에서는 [읽기 전용](collections-overview.md#collection-types) 컬렉션에 적용되는 정렬 함수를 설명합니다. 이러한 함수는 요청된 순서로 요소를 포함하는 새 컬렉션으로 결과를 반환합니다. [가변](collections-overview.md#collection-types) 컬렉션을 제자리에서 정렬하는 함수에 대해 알아보려면 [리스트별 연산](list-operations.md#sort)을 참조하세요.

## 자연 순서

기본 함수 [`sorted()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted.html)와 [`sortedDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-descending.html)은 `Comparable`을 구현하는 요소가 있는 컬렉션의 요소를 자연 순서에 따라 오름차순과 내림차순 시퀀스로 정렬하여 반환합니다. 이러한 함수는 모든 `Comparable` 요소의 컬렉션에 적용됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println("오름차순 정렬: ${numbers.sorted()}")
    println("내림차순 정렬: ${numbers.sortedDescending()}")
//sampleEnd
}
```

## 사용자 정의 순서

사용자 정의 순서로 정렬하거나 비교할 수 없는 객체를 정렬하려면 [`sortedBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-by.html)와 [`sortedByDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-by-descending.html) 함수가 있습니다. 이들은 컬렉션 요소를 `Comparable` 값에 매핑하는 선택자 함수를 받아 해당 값의 자연 순서로 컬렉션을 정렬합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    val sortedNumbers = numbers.sortedBy { it.length }
    println("길이로 오름차순 정렬: $sortedNumbers")
    val sortedByLast = numbers.sortedByDescending { it.last() }
    println("마지막 글자로 내림차순 정렬: $sortedByLast")
//sampleEnd
}
```

컬렉션 정렬에 사용자 정의 순서를 정의하려면 자체 `Comparator`를 제공할 수 있습니다. 이렇게 하려면 `Comparator`를 전달하면서 [`sortedWith()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-with.html) 함수를 호출합니다. 이 함수를 사용하면 문자열을 길이로 정렬하는 것은 다음과 같습니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println("길이로 정렬: ${numbers.sortedWith(compareBy { it.length })}")
//sampleEnd
}
```

## 역순

[`reversed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reversed.html) 함수를 사용하여 역순으로 컬렉션을 조회할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.reversed())
//sampleEnd
}
```

`reversed()`는 요소가 복사된 새 컬렉션을 반환합니다. 따라서 나중에 원래 컬렉션을 변경해도 이전에 얻은 `reversed()` 결과에 영향을 미치지 않습니다.

또 다른 역순 함수인 [`asReversed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/as-reversed.html)는 동일한 컬렉션 인스턴스의 역순 뷰를 반환하므로 원래 리스트가 변경되지 않을 경우 `reversed()`보다 가볍고 선호됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val reversedNumbers = numbers.asReversed()
    println(reversedNumbers)
//sampleEnd
}
```

원래 리스트가 가변인 경우 모든 변경 사항이 역순 뷰에 영향을 미치고 그 반대도 마찬가지입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    val reversedNumbers = numbers.asReversed()
    println(reversedNumbers)
    numbers.add("five")
    println(reversedNumbers)
//sampleEnd
}
```

그러나 리스트의 가변성을 모르거나 소스가 리스트가 아닌 경우 `reversed()`가 더 선호됩니다 - 결과가 미래에 변경되지 않을 복사본이기 때문입니다.

## 랜덤 순서

마지막으로, [`shuffled()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/shuffled.html) 함수는 컬렉션 요소를 랜덤 순서로 포함하는 새 `List`를 반환합니다. 인수 없이 호출하거나 [`Random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/index.html) 객체와 함께 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.shuffled())
//sampleEnd
}
```
