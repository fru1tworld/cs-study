# 플러스 및 마이너스 연산자

Kotlin에서 [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)와 [`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자는 컬렉션에 대해 정의되어 있습니다. 이들은 컬렉션을 첫 번째 피연산자로 받습니다; 두 번째 피연산자는 요소이거나 다른 컬렉션일 수 있습니다. 반환 값은 새로운 읽기 전용 컬렉션입니다:

* `plus`의 결과는 원래 컬렉션의 요소 _및_ 두 번째 피연산자를 포함합니다.
* `minus`의 결과는 원래 컬렉션의 요소에서 두 번째 피연산자의 요소를 _제외한_ 것을 포함합니다. 두 번째 피연산자가 요소인 경우 `minus`는 그것의 _첫 번째_ 발생을 제거합니다; 컬렉션인 경우 해당 요소의 _모든_ 발생을 제거합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    val plusList = numbers + "five"
    val minusList = numbers - listOf("three", "four")
    println(plusList)
    println(minusList)
//sampleEnd
}
```

맵에서 `plus`와 `minus`의 세부 사항은 [맵별 연산](map-operations.md)을 참조하세요. [증가 대입 연산자](operator-overloading.md#augmented-assignments) [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)과 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`)도 컬렉션에 대해 정의되어 있습니다. 그러나 읽기 전용 컬렉션의 경우 실제로 `plus` 또는 `minus` 연산자를 사용하고 결과를 동일한 변수에 재할당하려고 합니다. 따라서 `var` 읽기 전용 컬렉션에서만 사용할 수 있습니다. 가변 컬렉션의 경우 `val`로 선언된 경우 컬렉션을 제자리에서 수정합니다. 자세한 내용은 [컬렉션 쓰기 연산](collection-write.md)을 참조하세요.
