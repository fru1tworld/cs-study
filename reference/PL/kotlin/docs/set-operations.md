# 셋별 연산

Kotlin 컬렉션 패키지에는 셋에서 인기 있는 연산을 위한 확장 함수가 포함되어 있습니다: 교집합, 합집합 또는 컬렉션 간의 차집합을 찾는 것입니다.

두 컬렉션을 하나로 병합하려면 [`union()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/union.html) 함수를 사용합니다. 중위 형태 `a union b`로도 사용할 수 있습니다. 순서가 있는 컬렉션의 경우 피연산자의 순서가 중요합니다: 결과 컬렉션에서 첫 번째 피연산자의 요소가 두 번째 피연산자의 요소 앞에 옵니다.

두 컬렉션 모두에 있는 요소를 찾으려면 [`intersect()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/intersect.html)를 사용합니다. 다른 컬렉션에 없는 컬렉션 요소를 찾으려면 [`subtract()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/subtract.html)를 사용합니다. 두 함수 모두 중위 형태로도 호출할 수 있습니다. 예를 들어 `a intersect b`.

```kotlin
fun main() {
//sampleStart
    val numbers = setOf("one", "two", "three")

    // 출력 순서에 따라 같은 출력
    println(numbers union setOf("four", "five"))
    // [one, two, three, four, five]
    println(setOf("four", "five") union numbers)
    // [four, five, one, two, three]

    println(numbers intersect setOf("two", "one"))
    // [one, two]
    println(numbers subtract setOf("three", "four"))
    // [one, two]
    println(numbers subtract setOf("four", "three"))
    // [one, two]
//sampleEnd
}
```

두 컬렉션 중 하나에는 있지만 교집합에는 없는 요소를 찾으려면(대칭 차집합) 차집합을 계산하고 병합하면 됩니다:

```kotlin
fun main() {
//sampleStart
    val numbers = setOf("one", "two", "three")
    val numbers2 = setOf("three", "four")

    // 차집합 병합
    println((numbers - numbers2) union (numbers2 - numbers))
    // [one, two, four]
//sampleEnd
}
```

`List`에 `union()`, `intersect()`, `subtract()` 함수를 적용할 수도 있습니다. 그러나 결과는 `List`에서도 _항상_ `Set`입니다. 이 결과에서 동일한 요소는 모두 하나로 병합되고 인덱스 접근을 사용할 수 없습니다.

```kotlin
fun main() {
//sampleStart
    val list1 = listOf(1, 1, 2, 3, 5, 8, -1)
    val list2 = listOf(1, 1, 2, 2, 3, 5)

    // 두 리스트의 교집합 결과는 Set임
    println(list1 intersect list2)
    // [1, 2, 3, 5]

    // 동일한 요소는 하나로 병합됨
    println(list1 union list2)
    // [1, 2, 3, 5, 8, -1]
//sampleEnd
}
```
