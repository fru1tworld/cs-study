# 컬렉션 개요

Kotlin 표준 라이브러리는 컬렉션을 관리하기 위한 포괄적인 도구를 제공합니다. 컬렉션은 해결하고자 하는 문제에 중요하고 일반적으로 함께 연산되는 가변 개수의 항목(0개일 수도 있음) 그룹입니다.

컬렉션은 대부분의 프로그래밍 언어에서 공통적인 개념이므로, Java나 Python 컬렉션에 익숙하다면 이 소개를 건너뛰고 상세 섹션으로 진행해도 됩니다.

컬렉션은 일반적으로 동일한 타입(및 그 하위 타입)의 여러 객체를 포함합니다. 컬렉션 내의 객체를 _요소_ 또는 _항목_이라고 합니다. 예를 들어, 학과의 모든 학생은 평균 나이를 계산하는 데 사용될 수 있는 컬렉션을 형성합니다.

다음 컬렉션 타입들이 Kotlin과 관련이 있습니다:

* _List_는 인덱스(위치를 나타내는 정수 숫자)로 요소에 접근할 수 있는 순서가 있는 컬렉션입니다. 요소는 리스트에서 여러 번 나타날 수 있습니다. 리스트의 예로는 전화번호가 있습니다: 숫자들의 그룹이고, 순서가 중요하며, 반복될 수 있습니다.
* _Set_은 고유한 요소들의 컬렉션입니다. 이는 반복이 없는 객체들의 그룹인 수학적 집합 추상화를 반영합니다. 일반적으로 집합 요소의 순서는 중요하지 않습니다. 예를 들어, 복권 번호는 집합을 형성합니다: 고유하고, 순서가 중요하지 않습니다.
* _Map_(또는 _딕셔너리_)은 키-값 쌍의 집합입니다. 키는 고유하며, 각 키는 정확히 하나의 값에 매핑됩니다. 값은 중복될 수 있습니다. 맵은 객체 간의 논리적 연결을 저장하는 데 유용합니다. 예를 들어, 직원의 ID와 직책 같은 것입니다.
* _ArrayDeque_는 양끝에서 요소를 추가하거나 제거할 수 있는 양방향 큐입니다. _ArrayDeque_는 Kotlin에서 스택과 큐 데이터 구조의 역할을 모두 수행합니다. 내부적으로 ArrayDeque는 필요할 때 자동으로 크기가 조정되는 크기 조절 가능한 배열을 사용하여 구현됩니다.

> 배열은 컬렉션 타입이 아닙니다. 자세한 내용은 [배열](arrays.md)을 참조하세요.
>

Kotlin을 사용하면 컬렉션에 저장된 객체의 정확한 타입과 독립적으로 컬렉션을 조작할 수 있습니다. 즉, `String`을 `String` 리스트에 추가하는 것은 `Int`나 사용자 정의 클래스를 추가하는 것과 동일한 방식으로 수행됩니다. 따라서 Kotlin 표준 라이브러리는 모든 타입의 컬렉션을 생성, 채우기 및 관리하기 위한 제네릭 인터페이스, 클래스 및 함수를 제공합니다.

컬렉션 인터페이스와 관련 함수는 `kotlin.collections` 패키지에 있습니다. 그 내용에 대한 개요를 살펴보겠습니다.

## 컬렉션 타입

Kotlin 표준 라이브러리는 기본 컬렉션 타입인 셋, 리스트, 맵의 구현을 제공합니다. 인터페이스 쌍이 각 컬렉션 타입을 나타냅니다:

* 컬렉션 요소에 접근하기 위한 연산을 제공하는 _읽기 전용_ 인터페이스.
* 요소 추가, 제거 및 업데이트와 같은 쓰기 연산으로 해당 읽기 전용 인터페이스를 확장하는 _가변_ 인터페이스.

가변 컬렉션을 변경하는 것은 반드시 [`var`](basic-syntax.md#variables)에 할당해야 하는 것은 아닙니다: 쓰기 연산은 동일한 가변 컬렉션 객체를 수정하므로 참조는 변경되지 않습니다. 하지만 `val` 컬렉션을 재할당하려고 하면 컴파일 오류가 발생합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    numbers.add("five")   // 이것은 가능합니다
    println(numbers)
    //numbers = mutableListOf("six", "seven")      // 컴파일 오류
//sampleEnd
}
```

읽기 전용 컬렉션 타입은 [공변](generics.md#variance)입니다. 이는 `Rectangle` 클래스가 `Shape`를 상속하는 경우, `List<Shape>`가 필요한 모든 곳에서 `List<Rectangle>`을 사용할 수 있음을 의미합니다. 즉, 컬렉션 타입은 요소 타입과 동일한 하위 타입 관계를 갖습니다. 맵은 값 타입에 대해 공변이지만 키 타입에 대해서는 그렇지 않습니다.

반면, 가변 컬렉션은 공변이 아닙니다. 그렇지 않으면 런타임 실패로 이어질 수 있습니다. `MutableList<Rectangle>`이 `MutableList<Shape>`의 하위 타입이라면, 다른 `Shape` 상속자(예: `Circle`)를 삽입하여 `Rectangle` 타입 인수를 위반할 수 있습니다.

다음은 Kotlin 컬렉션 인터페이스의 다이어그램입니다:

![컬렉션 인터페이스 계층](collections-diagram.png)

인터페이스와 그 구현에 대해 살펴보겠습니다. `Collection`에 대해 알아보려면 아래 섹션을 읽으세요. `List`, `Set`, `Map`에 대해 알아보려면 해당 섹션을 읽거나 Kotlin 개발자 옹호자 Sebastian Aigner의 비디오를 시청할 수 있습니다:

<video src="https://www.youtube.com/v/F8jj7e-_jFA" title="Kotlin Collections Overview"/>

### Collection

[`Collection<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)은 컬렉션 계층의 루트입니다. 이 인터페이스는 읽기 전용 컬렉션의 공통 동작을 나타냅니다: 크기 조회, 항목 멤버십 확인 등. `Collection`은 요소를 반복하기 위한 연산을 정의하는 `Iterable<T>` 인터페이스를 상속합니다. 다른 컬렉션 타입에 적용되는 함수의 매개변수로 `Collection`을 사용할 수 있습니다. 더 구체적인 경우에는 `Collection`의 상속자인 [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)와 [`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)을 사용합니다.

```kotlin
fun printAll(strings: Collection<String>) {
    for(s in strings) print("$s ")
    println()
}

fun main() {
    val stringList = listOf("one", "two", "one")
    printAll(stringList)

    val stringSet = setOf("one", "two", "three")
    printAll(stringSet)
}
```

[`MutableCollection<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html)은 `add` 및 `remove`와 같은 쓰기 연산이 있는 `Collection`입니다.

```kotlin
fun List<String>.getShortWordsTo(shortWords: MutableList<String>, maxLength: Int) {
    this.filterTo(shortWords) { it.length <= maxLength }
    // 관사 던지기
    val articles = setOf("a", "A", "an", "An", "the", "The")
    shortWords -= articles
}

fun main() {
    val words = "A long time ago in a galaxy far far away".split(" ")
    val shortWords = mutableListOf<String>()
    words.getShortWordsTo(shortWords, 3)
    println(shortWords)
}
```

### List

[`List<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)는 지정된 순서로 요소를 저장하고 인덱스로 접근할 수 있게 합니다. 인덱스는 0(첫 번째 요소의 인덱스)부터 `list.size - 1`인 `lastIndex`까지입니다.

```kotlin
fun main() {
    val numbers = listOf("one", "two", "three", "four")
    println("요소 수: ${numbers.size}")
    println("세 번째 요소: ${numbers.get(2)}")
    println("네 번째 요소: ${numbers[3]}")
    println("요소 \"two\"의 인덱스: ${numbers.indexOf("two")}")
}
```

리스트 요소(null 포함)는 중복될 수 있습니다: 리스트는 동일한 객체 또는 단일 객체의 발생을 원하는 수만큼 포함할 수 있습니다. 두 리스트가 같은 크기를 가지고 같은 위치에 [구조적으로 동일한](equality.md#structural-equality) 요소가 있으면 동일한 것으로 간주됩니다.

```kotlin
data class Person(var name: String, var age: Int)

fun main() {
    val bob = Person("Bob", 31)
    val people = listOf(Person("Adam", 20), bob, bob)
    val people2 = listOf(Person("Adam", 20), Person("Bob", 31), bob)
    println(people == people2)
    bob.age = 32
    println(people == people2)
}
```

[`MutableList<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)는 리스트 전용 쓰기 연산이 있는 `List`입니다. 예를 들어 특정 위치에 요소를 추가하거나 제거하는 것입니다.

```kotlin
fun main() {
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.add(5)
    numbers.removeAt(1)
    numbers[0] = 0
    numbers.shuffle()
    println(numbers)
}
```

보시다시피, 어떤 면에서 리스트는 배열과 매우 유사합니다. 그러나 한 가지 중요한 차이점이 있습니다: 배열의 크기는 초기화 시 정의되며 절대 변경되지 않습니다; 반면 리스트는 미리 정의된 크기가 없습니다; 리스트의 크기는 요소 추가, 업데이트 또는 제거와 같은 쓰기 연산의 결과로 변경될 수 있습니다.

Kotlin에서 `List`의 기본 구현은 크기 조절 가능한 배열로 볼 수 있는 [`ArrayList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-list/index.html)입니다.

### Set

[`Set<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)은 고유한 요소를 저장합니다; 순서는 일반적으로 정의되지 않습니다. `null` 요소도 고유합니다: `Set`은 하나의 `null`만 포함할 수 있습니다. 두 셋이 같은 크기를 가지고 한 셋의 각 요소에 대해 다른 셋에 동일한 요소가 있으면 두 셋은 동일합니다.

```kotlin
fun main() {
    val numbers = setOf(1, 2, 3, 4)
    println("요소 수: ${numbers.size}")
    if (numbers.contains(1)) println("1은 셋에 있습니다")

    val numbersBackwards = setOf(4, 3, 2, 1)
    println("셋이 동일합니다: ${numbers == numbersBackwards}")
}
```

[`MutableSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)은 `MutableCollection`의 쓰기 연산이 있는 `Set`입니다.

`MutableSet`의 기본 구현인 [`LinkedHashSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-linked-hash-set/index.html)은 요소 삽입 순서를 유지합니다. 따라서 `first()` 또는 `last()`와 같이 순서에 의존하는 함수는 이러한 셋에서 예측 가능한 결과를 반환합니다.

```kotlin
fun main() {
    val numbers = setOf(1, 2, 3, 4)  // LinkedHashSet이 기본 구현
    val numbersBackwards = setOf(4, 3, 2, 1)

    println(numbers.first() == numbersBackwards.first())
    println(numbers.first() == numbersBackwards.last())
}
```

대체 구현인 [`HashSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-set/index.html)은 요소 순서에 대해 아무것도 보장하지 않으므로 호출하면 예측할 수 없는 결과를 반환합니다. 그러나 `HashSet`은 동일한 수의 요소를 저장하는 데 더 적은 메모리를 필요로 합니다.

### Map

[`Map<K, V>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)은 `Collection` 인터페이스의 상속자가 아닙니다; 그러나 이것도 Kotlin 컬렉션 타입입니다. `Map`은 _키-값_ 쌍(또는 _엔트리_)을 저장합니다; 키는 고유하지만 서로 다른 키가 동일한 값에 쌍을 이룰 수 있습니다. `Map` 인터페이스는 키로 값에 접근하거나 키와 값을 검색하는 등의 특정 함수를 제공합니다.

```kotlin
fun main() {
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)

    println("모든 키: ${numbersMap.keys}")
    println("모든 값: ${numbersMap.values}")
    if ("key2" in numbersMap) println("키 \"key2\"의 값: ${numbersMap["key2"]}")
    if (1 in numbersMap.values) println("값 1이 맵에 있습니다")
    if (numbersMap.containsValue(1)) println("값 1이 맵에 있습니다") // 위와 동일
}
```

동일한 쌍을 포함하는 두 맵은 쌍 순서와 관계없이 동일합니다.

```kotlin
fun main() {
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)
    val anotherMap = mapOf("key2" to 2, "key1" to 1, "key4" to 1, "key3" to 3)

    println("맵이 동일합니다: ${numbersMap == anotherMap}")
}
```

[`MutableMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html)은 맵 쓰기 연산이 있는 `Map`입니다. 예를 들어 새로운 키-값 쌍을 추가하거나 주어진 키와 연관된 값을 업데이트할 수 있습니다.

```kotlin
fun main() {
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap.put("three", 3)
    numbersMap["one"] = 11

    println(numbersMap)
}
```

`MutableMap`의 기본 구현인 [`LinkedHashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-linked-hash-map/index.html)은 맵을 반복할 때 요소 삽입 순서를 유지합니다. 반면, 대체 구현인 [`HashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)은 요소 순서에 대해 아무것도 보장하지 않습니다.

### ArrayDeque

[`ArrayDeque<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/)는 양방향 큐의 구현으로, 큐의 시작 또는 끝에서 요소를 추가하거나 제거할 수 있습니다. 따라서 `ArrayDeque`는 Kotlin에서 스택과 큐 데이터 구조의 역할을 모두 수행합니다. 내부적으로 `ArrayDeque`는 필요할 때 자동으로 크기가 조정되는 크기 조절 가능한 배열을 사용하여 구현됩니다:

```kotlin
fun main() {
    val deque = ArrayDeque(listOf(1, 2, 3))

    deque.addFirst(0)
    deque.addLast(4)
    println(deque) // [0, 1, 2, 3, 4]

    println(deque.first()) // 0
    println(deque.last()) // 4

    deque.removeFirst()
    deque.removeLast()
    println(deque) // [1, 2, 3]
}
```
