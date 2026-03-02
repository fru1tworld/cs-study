# 그룹화

Kotlin 표준 라이브러리는 컬렉션 요소를 그룹화하기 위한 확장 함수를 제공합니다. 기본 함수 [`groupBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/group-by.html)는 람다 함수를 받아 `Map`을 반환합니다. 이 맵에서 각 키는 람다 결과이고 해당 값은 이 결과가 반환되는 요소의 `List`입니다. 이 함수는 예를 들어 `String` 리스트를 첫 글자로 그룹화하는 데 사용할 수 있습니다.

두 번째 람다 인수인 값 변환 함수와 함께 `groupBy()`를 호출할 수도 있습니다. 두 람다가 있는 결과 맵에서 `keySelector` 함수에 의해 생성된 키는 원래 요소 대신 값 변환 함수의 결과에 매핑됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")

    println(numbers.groupBy { it.first().uppercase() })
    println(numbers.groupBy(keySelector = { it.first() }, valueTransform = { it.uppercase() }))
//sampleEnd
}
```

요소를 그룹화한 다음 한 번에 모든 그룹에 연산을 적용하려면 [`groupingBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/grouping-by.html) 함수를 사용합니다. 이는 [`Grouping`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-grouping/index.html) 타입의 인스턴스를 반환합니다. `Grouping` 인스턴스를 사용하면 지연 방식으로 모든 그룹에 연산을 적용할 수 있습니다: 그룹은 연산 실행 직전에 실제로 빌드됩니다.

즉, `Grouping`은 다음 연산을 지원합니다:

* [`eachCount()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/each-count.html)는 각 그룹의 요소를 셉니다.
* [`fold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold.html)와 [`reduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce.html)는 각 그룹에 대해 별도의 컬렉션으로 [fold 및 reduce](collection-aggregate.md#fold-and-reduce) 연산을 수행하고 결과를 반환합니다.
* [`aggregate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/aggregate.html)는 각 그룹의 모든 요소에 주어진 연산을 순차적으로 적용하고 결과를 반환합니다. 이것은 `Grouping`에 대해 모든 연산을 수행하는 일반적인 방법입니다. fold나 reduce가 충분하지 않을 때 사용자 정의 연산을 구현하는 데 사용합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.groupingBy { it.first() }.eachCount())
//sampleEnd
}
```
