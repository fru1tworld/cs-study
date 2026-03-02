# 컬렉션 일부 조회

Kotlin 표준 라이브러리에는 컬렉션의 일부를 조회하기 위한 확장 함수가 포함되어 있습니다. 이러한 함수는 결과 컬렉션에 포함할 요소를 선택하는 다양한 방법을 제공합니다: 위치를 명시적으로 나열하거나, 결과 크기를 지정하거나, 기타 방법을 사용합니다.

## 슬라이스

[`slice()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/slice.html)는 주어진 인덱스에 있는 컬렉션 요소의 리스트를 반환합니다. 인덱스는 [범위](ranges.md)나 정수 값의 컬렉션으로 전달할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.slice(1..3))
    println(numbers.slice(0..4 step 2))
    println(numbers.slice(setOf(3, 5, 0)))
//sampleEnd
}
```

## Take와 drop

처음부터 지정된 수의 요소를 가져오려면 [`take()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take.html) 함수를 사용합니다. 마지막 요소를 가져오려면 [`takeLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-last.html)를 사용합니다. 컬렉션 크기보다 큰 숫자로 호출하면 두 함수 모두 전체 컬렉션을 반환합니다.

처음이나 마지막부터 주어진 수의 요소를 제외한 모든 요소를 가져오려면 [`drop()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop.html)과 [`dropLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-last.html) 함수를 각각 호출합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.take(3))
    println(numbers.takeLast(3))
    println(numbers.drop(1))
    println(numbers.dropLast(5))
//sampleEnd
}
```

술어를 사용하여 take하거나 drop할 요소 수를 정의할 수도 있습니다. 위에서 설명한 함수의 네 가지 변형이 있습니다:

* [`takeWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-while.html)은 술어와 일치하는 요소를 술어와 일치하지 않는 첫 번째 요소까지 가져오는 `take()`입니다. 첫 번째 컬렉션 요소가 술어와 일치하지 않으면 결과는 비어 있습니다.
* [`takeLastWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-last-while.html)은 `takeLast()`와 유사합니다: 컬렉션 끝에서 술어와 일치하는 요소 범위를 가져옵니다. 범위의 첫 번째 요소는 술어와 일치하지 않는 마지막 요소 바로 다음 요소입니다. 마지막 컬렉션 요소가 술어와 일치하지 않으면 결과는 비어 있습니다;
* [`dropWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-while.html)은 같은 술어를 가진 `takeWhile()`의 반대입니다: 술어와 일치하지 않는 첫 번째 요소부터 끝까지 요소를 반환합니다.
* [`dropLastWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-last-while.html)은 같은 술어를 가진 `takeLastWhile()`의 반대입니다: 처음부터 술어와 일치하지 않는 마지막 요소까지 요소를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.takeWhile { !it.startsWith('f') })
    println(numbers.takeLastWhile { it != "three" })
    println(numbers.dropWhile { it.length == 3 })
    println(numbers.dropLastWhile { it.contains('i') })
//sampleEnd
}
```

## 청크

컬렉션을 주어진 크기의 부분으로 나누려면 [`chunked()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/chunked.html) 함수를 사용합니다. `chunked()`는 단일 인수인 청크의 크기를 받아 주어진 크기의 `List`들의 `List`를 반환합니다. 첫 번째 청크는 첫 번째 요소에서 시작하여 `size` 요소를 포함하고, 두 번째 청크는 다음 `size` 요소를 포함하는 식입니다. 마지막 청크는 크기가 더 작을 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.chunked(3))
//sampleEnd
}
```

반환된 청크에 변환을 즉시 적용할 수도 있습니다. 이렇게 하려면 `chunked()`를 호출할 때 변환을 람다 함수로 제공합니다. 람다 인수는 컬렉션의 청크입니다. 변환과 함께 `chunked()`가 호출되면 청크는 해당 람다에 즉시 전달되어야 하는 수명이 짧은 `List`입니다; 해당 청크에서 다른 `List`를 사용하려면 청크를 해당 `List`에 복사합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.chunked(3) { it.sum() })  // `it`은 원래 컬렉션의 청크
//sampleEnd
}
```

## 윈도우

주어진 크기의 컬렉션 요소 범위 _모두_를 조회할 수 있습니다. 이를 가져오는 함수는 [`windowed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/windowed.html)입니다: 주어진 크기의 슬라이딩 윈도우를 사용하여 컬렉션을 볼 때 보이는 요소 범위의 리스트를 반환합니다. `chunked()`와 달리 `windowed()`는 _각_ 컬렉션 요소에서 시작하는 요소 범위(_윈도우_)를 반환합니다. 모든 윈도우는 단일 `List`의 요소로 반환됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.windowed(3))
//sampleEnd
}
```

`windowed()`는 선택적 매개변수로 더 많은 유연성을 제공합니다:

* `step`은 두 인접 윈도우의 첫 번째 요소 사이의 거리를 정의합니다. 기본값은 1이므로 결과에는 모든 요소에서 시작하는 윈도우가 포함됩니다. 스텝을 2로 늘리면 홀수 요소에서 시작하는 윈도우만 받습니다: 첫 번째, 세 번째 등.
* `partialWindows`는 컬렉션 끝에서 더 작은 크기의 윈도우를 포함합니다. 예를 들어, 세 요소의 윈도우를 요청하면 마지막 두 요소에 대해 빌드할 수 없습니다. 이 경우 `partialWindows`를 활성화하면 크기가 2와 1인 두 개의 리스트가 더 포함됩니다.

마지막으로, 반환된 범위에 변환을 즉시 적용할 수 있습니다. 이렇게 하려면 `windowed()`를 호출할 때 변환을 람다 함수로 제공합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = (1..10).toList()
    println(numbers.windowed(3, step = 2, partialWindows = true))
    println(numbers.windowed(3) { it.sum() })
//sampleEnd
}
```

두 요소 윈도우를 빌드하기 위해 별도의 함수인 [`zipWithNext()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/zip-with-next.html)가 있습니다. 이는 수신자 컬렉션의 인접 요소 쌍을 생성합니다. `zipWithNext()`는 컬렉션을 조각으로 나누지 않습니다; 마지막 요소를 제외한 _각_ 요소에 대해 `Pair`를 생성하므로 `[1, 2, 3, 4]`에서의 결과는 `[[1, 2], [2, 3], [3, 4]]`이지 `[[1, 2], [3, 4]]`가 아닙니다. `zipWithNext()`는 변환 함수와 함께 호출할 수도 있습니다; 수신자 컬렉션의 두 요소를 인수로 받아야 합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.zipWithNext())
    println(numbers.zipWithNext() { s1, s2 -> s1.length > s2.length})
//sampleEnd
}
```
