# 이터레이터

컬렉션을 순회하기 위해 Kotlin 표준 라이브러리는 _이터레이터_의 일반적으로 사용되는 메커니즘을 지원합니다. 이터레이터는 기본 구조를 노출하지 않고 컬렉션 요소에 순차적으로 접근할 수 있게 해주는 객체입니다. 이터레이터는 모든 요소를 하나씩 처리해야 할 때 유용합니다. 예를 들어, 값을 출력하거나 유사한 업데이트를 수행하는 경우입니다.

이터레이터는 [`Iterator<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html) 인터페이스의 `iterator()` 함수를 호출하여 `Set`과 `List`를 포함한 [`Iterable<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html) 인터페이스의 상속자에 대해 얻을 수 있습니다.

이터레이터를 얻으면 컬렉션의 첫 번째 요소를 가리킵니다; [`next()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/next.html) 함수를 호출하면 이 요소를 반환하고 이터레이터 위치를 다음 요소(존재하는 경우)로 이동합니다.

이터레이터가 마지막 요소를 통과하면 더 이상 요소를 검색하는 데 사용할 수 없습니다; 이전 위치로 다시 재설정할 수도 없습니다. 컬렉션을 다시 반복하려면 새 이터레이터를 생성하세요.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val numbersIterator = numbers.iterator()
    while (numbersIterator.hasNext()) {
        println(numbersIterator.next())
    }
//sampleEnd
}
```

`Iterable` 컬렉션을 순회하는 또 다른 방법은 잘 알려진 `for` 루프입니다. 컬렉션에 `for`를 사용하면 암묵적으로 이터레이터를 얻습니다. 따라서 다음 코드는 위의 예제와 동등합니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    for (item in numbers) {
        println(item)
    }
//sampleEnd
}
```

마지막으로, 컬렉션을 반복하고 각 요소에 대해 주어진 람다를 실행할 수 있는 유용한 `forEach()` 함수가 있습니다. 따라서 동일한 예제는 다음과 같이 됩니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    numbers.forEach {
        println(it)
    }
//sampleEnd
}
```

## 리스트 이터레이터

리스트의 경우, 특별한 이터레이터 구현인 [`ListIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/index.html)가 있습니다. 이는 양방향 반복을 지원합니다: 순방향과 역방향.

역방향 반복은 [`hasPrevious()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/has-previous.html)와 [`previous()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/previous.html) 함수로 구현됩니다. 또한 `ListIterator`는 [`nextIndex()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/next-index.html)와 [`previousIndex()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/previous-index.html) 함수로 요소 인덱스에 대한 정보를 제공합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val listIterator = numbers.listIterator()
    while (listIterator.hasNext()) listIterator.next()
    println("역방향 반복:")
    while (listIterator.hasPrevious()) {
        print("인덱스: ${listIterator.previousIndex()}")
        println(", 값: ${listIterator.previous()}")
    }
//sampleEnd
}
```

양방향으로 반복할 수 있다는 것은 `ListIterator`가 마지막 요소에 도달한 후에도 여전히 사용할 수 있음을 의미합니다.

## 가변 이터레이터

가변 컬렉션을 반복하려면 [`MutableIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)를 사용하세요. 이는 요소 제거 함수인 [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/remove.html)로 `Iterator`를 확장합니다. 따라서 반복 중에 컬렉션에서 요소를 제거할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    val mutableIterator = numbers.iterator()

    mutableIterator.next()
    mutableIterator.remove()
    println("제거 후: $numbers")
//sampleEnd
}
```

[`MutableListIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list-iterator/index.html)는 요소를 삽입하고 교체할 수도 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "four", "four")
    val mutableListIterator = numbers.listIterator()

    mutableListIterator.next()
    mutableListIterator.add("two")
    mutableListIterator.next()
    mutableListIterator.set("three")
    println(numbers)
//sampleEnd
}
```
