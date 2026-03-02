# 컬렉션 쓰기 연산

[가변 컬렉션](collections-overview.md#collection-types)은 컬렉션 내용을 변경하는 연산, 예를 들어 요소 추가 또는 제거를 지원합니다. 이 페이지에서는 `MutableCollection`의 모든 구현에서 사용할 수 있는 쓰기 연산을 설명합니다. `List`와 `Map`에 사용할 수 있는 더 구체적인 연산은 각각 [리스트별 연산](list-operations.md)과 [맵별 연산](map-operations.md)에서 설명합니다.

## 요소 추가

리스트나 셋에 단일 요소를 추가하려면 [`add()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/add.html) 함수를 사용합니다. 지정된 객체가 컬렉션 끝에 추가됩니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.add(5)
    println(numbers)
//sampleEnd
}
```

[`addAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/add-all.html)은 인수 객체의 모든 요소를 리스트나 셋에 추가합니다. 인수는 `Iterable`, `Sequence` 또는 `Array`일 수 있습니다. 수신자와 인수의 타입은 다를 수 있습니다. 예를 들어, `Set`의 모든 항목을 `List`에 추가할 수 있습니다.

리스트에서 호출할 때 `addAll()`은 인수 컬렉션에서와 동일한 순서로 새 요소를 추가합니다. 첫 번째 인수로 요소 위치를 사용하여 `addAll()`을 호출할 수도 있습니다. 인수 컬렉션의 첫 번째 요소가 이 위치에 삽입됩니다. 인수 컬렉션의 다른 요소는 그 뒤를 따르며 수신자 요소를 끝 쪽으로 이동합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 5, 6)
    numbers.addAll(arrayOf(7, 8))
    println(numbers)
    numbers.addAll(2, setOf(3, 4))
    println(numbers)
//sampleEnd
}
```

[`plus` 연산자](collection-plus-minus.md)의 제자리 버전인 [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)을 사용하여 요소를 추가할 수도 있습니다. 가변 컬렉션에 적용하면 `+=`는 두 번째 피연산자(요소 또는 다른 컬렉션)를 컬렉션 끝에 추가합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two")
    numbers += "three"
    println(numbers)
    numbers += listOf("four", "five")
    println(numbers)
//sampleEnd
}
```

## 요소 제거

가변 컬렉션에서 요소를 제거하려면 [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/remove.html) 함수를 사용합니다. `remove()`는 요소 값을 받아 해당 값의 첫 번째 발생을 제거합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4, 3)
    numbers.remove(3)                    // 첫 번째 `3`을 제거
    println(numbers)
    numbers.remove(5)                    // 아무것도 제거하지 않음
    println(numbers)
//sampleEnd
}
```

한 번에 여러 요소를 제거하려면 다음 함수가 있습니다:

* [`removeAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/remove-all.html)은 인수 컬렉션에 있는 모든 요소를 제거합니다. 또는 술어와 함께 호출할 수 있습니다; 이 경우 함수는 술어가 `true`를 반환하는 모든 요소를 제거합니다.
* [`retainAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/retain-all.html)은 `removeAll()`의 반대입니다: 인수 컬렉션의 요소를 제외한 모든 요소를 제거합니다. 술어와 함께 사용하면 일치하는 요소만 유지합니다.
* [`clear()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/clear.html)는 리스트에서 모든 요소를 제거하고 비워둡니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    println(numbers)
    numbers.retainAll { it >= 3 }
    println(numbers)
    numbers.clear()
    println(numbers)

    val numbersSet = mutableSetOf("one", "two", "three", "four")
    numbersSet.removeAll(setOf("one", "two"))
    println(numbersSet)
//sampleEnd
}
```

컬렉션에서 요소를 제거하는 또 다른 방법은 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 - [`minus`](collection-plus-minus.md)의 제자리 버전입니다. 두 번째 인수는 요소 타입의 단일 인스턴스이거나 다른 컬렉션일 수 있습니다. 오른쪽에 단일 요소가 있으면 `-=`는 그것의 _첫 번째_ 발생을 제거합니다. 반면, 컬렉션인 경우 해당 요소의 _모든_ 발생이 제거됩니다. 예를 들어, 리스트에 중복 요소가 있으면 한 번에 모두 제거됩니다. 두 번째 피연산자는 컬렉션에 없는 요소를 포함할 수 있습니다. 이러한 요소는 연산 실행에 영향을 미치지 않습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "three", "four")
    numbers -= "three"
    println(numbers)
    numbers -= listOf("four", "five")
    //numbers -= listOf("four")    // 위와 동일
    println(numbers)
//sampleEnd
}
```

## 요소 업데이트

리스트와 맵은 요소를 업데이트하는 연산도 제공합니다. 이는 [리스트별 연산](list-operations.md)과 [맵별 연산](map-operations.md)에서 설명합니다. 셋의 경우 업데이트는 의미가 없습니다. 실제로 요소를 제거하고 다른 것을 추가하기 때문입니다.
