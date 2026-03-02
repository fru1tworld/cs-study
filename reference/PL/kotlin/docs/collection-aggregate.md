# 집계 연산

Kotlin 컬렉션은 일반적으로 사용되는 _집계 연산_을 위한 함수를 포함합니다 - 컬렉션 내용을 기반으로 단일 값을 반환하는 연산입니다. 대부분은 잘 알려져 있으며 다른 언어에서와 동일하게 작동합니다:

* [`minOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-or-null.html)과 [`maxOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-or-null.html)은 각각 가장 작은 요소와 가장 큰 요소를 반환합니다. 빈 컬렉션에서는 `null`을 반환합니다.
* [`average()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/average.html)는 숫자 컬렉션에서 요소의 평균 값을 반환합니다.
* [`sum()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sum.html)은 숫자 컬렉션에서 요소의 합계를 반환합니다.
* [`count()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/count.html)는 컬렉션의 요소 수를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(6, 42, 10, 4)

    println("개수: ${numbers.count()}")
    println("최대: ${numbers.maxOrNull()}")
    println("최소: ${numbers.minOrNull()}")
    println("평균: ${numbers.average()}")
    println("합계: ${numbers.sum()}")
//sampleEnd
}
```

특정 선택자 함수나 사용자 정의 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html)로 가장 작은 요소와 가장 큰 요소를 조회하는 함수도 있습니다:

* [`maxByOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-by-or-null.html)과 [`minByOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-by-or-null.html)은 선택자 함수를 받아 가장 큰 또는 가장 작은 선택자 반환 값을 가진 요소를 반환합니다.
* [`maxWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-with-or-null.html)과 [`minWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-with-or-null.html)은 `Comparator` 객체를 받아 해당 `Comparator`에 따라 가장 큰 또는 가장 작은 요소를 반환합니다.
* [`maxOfOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-of-or-null.html)과 [`minOfOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-of-or-null.html)은 선택자 함수를 받아 가장 큰 또는 가장 작은 선택자 반환 값 자체를 반환합니다.
* [`maxOfWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-of-with-or-null.html)과 [`minOfWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-of-with-or-null.html)은 `Comparator` 객체를 받아 해당 `Comparator`에 따라 가장 큰 또는 가장 작은 선택자 반환 값을 반환합니다.

이러한 함수는 빈 컬렉션에서 `null`을 반환합니다. `maxOf`, `minOf`, `maxOfWith`, `minOfWith`와 같은 대안도 있습니다 - 이들은 동일하게 작동하지만 빈 컬렉션에서 `NoSuchElementException`을 throw합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 42, 10, 4)
    val min3Remainder = numbers.minByOrNull { it % 3 }
    println(min3Remainder)

    val strings = listOf("one", "two", "three", "four")
    val longestString = strings.maxWithOrNull(compareBy { it.length })
    println(longestString)
//sampleEnd
}
```

일반적인 `sum()` 외에도 선택자 함수를 받아 모든 컬렉션 요소에 대한 반환 값의 합계를 반환하는 고급 합산 함수 [`sumOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sum-of.html)가 있습니다. 선택자는 `Int`, `Long`, `Double`, `UInt`, `ULong` 같은 다양한 숫자 타입을 반환할 수 있습니다(JVM에서는 `BigInteger`와 `BigDecimal`도 가능).

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 42, 10, 4)
    println(numbers.sumOf { it * 2 })
    println(numbers.sumOf { it.toDouble() / 2 })
//sampleEnd
}
```

## Fold와 reduce

더 구체적인 경우를 위해 제공된 연산을 컬렉션 요소에 순차적으로 적용하고 누적된 결과를 반환하는 [`reduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce.html)와 [`fold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold.html) 함수가 있습니다. 연산은 두 인수를 받습니다: 이전에 누적된 값과 컬렉션 요소.

이 두 함수의 차이점은 `fold()`가 초기 값을 받아 첫 번째 단계에서 누적기로 사용하는 반면, `reduce()`의 첫 번째 단계는 첫 번째 및 두 번째 요소를 첫 번째 단계의 연산 인수로 사용한다는 것입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 2, 10, 4)

    val simpleSum = numbers.reduce { sum, element -> sum + element }
    println(simpleSum)
    val sumDoubled = numbers.fold(0) { sum, element -> sum + element * 2 }
    println(sumDoubled)

    //val sumDoubledReduce = numbers.reduce { sum, element -> sum + element * 2 } // 잘못됨: 첫 번째 요소가 결과에서 두 배가 되지 않음
    //println(sumDoubledReduce)
//sampleEnd
}
```

위의 예제는 차이점을 보여줍니다: `fold()`는 두 배된 요소의 합계를 계산하는 데 사용됩니다. 동일한 함수를 `reduce()`에 전달하면 다른 결과를 반환합니다. 첫 번째 단계에서 리스트의 첫 번째 및 두 번째 요소가 인수로 사용되기 때문에 첫 번째 요소가 두 배되지 않습니다.

역순으로 함수를 적용하려면 [`reduceRight()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right.html)와 [`foldRight()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-right.html) 함수를 사용합니다. 이들은 `fold()`와 `reduce()`와 유사하게 작동하지만 마지막 요소부터 시작한 다음 이전 요소로 계속합니다. 오른쪽으로 fold하거나 reduce할 때 연산 인수의 순서가 변경됩니다: 먼저 요소가 오고 그 다음 누적된 값이 옵니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 2, 10, 4)
    val sumDoubledRight = numbers.foldRight(0) { element, sum -> sum + element * 2 }
    println(sumDoubledRight)
//sampleEnd
}
```

요소 인덱스를 매개변수로 사용하는 연산을 적용할 수도 있습니다. 이를 위해 [`reduceIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-indexed.html)와 [`foldIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-indexed.html) 함수를 사용합니다. 이들은 연산의 첫 번째 인수로 요소 인덱스를 전달합니다.

마지막으로, 컬렉션 요소에 이러한 연산을 오른쪽에서 왼쪽으로 적용하는 함수인 [`reduceRightIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-indexed.html)와 [`foldRightIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-right-indexed.html)가 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 2, 10, 4)
    val sumEven = numbers.foldIndexed(0) { idx, sum, element -> if (idx % 2 == 0) sum + element else sum }
    println(sumEven)

    val sumEvenRight = numbers.foldRightIndexed(0) { idx, element, sum -> if (idx % 2 == 0) sum + element else sum }
    println(sumEvenRight)
//sampleEnd
}
```

모든 reduce 연산은 빈 컬렉션에서 예외를 throw합니다. 대신 `null`을 받으려면 `*OrNull()` 대응물을 사용하세요:
* [`reduceOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-or-null.html)
* [`reduceRightOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-or-null.html)
* [`reduceIndexedOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-indexed-or-null.html)
* [`reduceRightIndexedOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-indexed-or-null.html)

나중에 재사용하기 위해 중간 누적기 값을 저장하려는 경우 [`runningFold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-fold.html)(또는 동의어인 [`scan()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/scan.html))과 [`runningReduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-reduce.html) 함수가 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(0, 1, 2, 3, 4, 5)
    val runningReduceSum = numbers.runningReduce { sum, item -> sum + item }
    val runningFoldSum = numbers.runningFold(10) { sum, item -> sum + item }
//sampleEnd
    val transform = { index: Int, element: Int -> "N = ${index + 1}: $element" }
    println(runningReduceSum.mapIndexed(transform).joinToString("\n", "runningReduce 합계:\n"))
    println(runningFoldSum.mapIndexed(transform).joinToString("\n", "runningFold 합계:\n"))
}
```

연산 매개변수에 요소 인덱스가 필요한 경우 [`runningFoldIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-fold-indexed.html) 또는 [`runningReduceIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-reduce-indexed.html)를 사용합니다.
