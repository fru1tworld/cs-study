# 컬렉션

# 컬렉션 개요

> **원문:** https://kotlinlang.org/docs/collections-overview.html

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

대체 구현인 [`HashSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-set/index.html)은 요소 순서를 전혀 보장하지 않으므로 호출하면 예측할 수 없는 결과를 반환합니다. 그러나 `HashSet`은 동일한 수의 요소를 저장하는 데 더 적은 메모리를 필요로 합니다.

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

`MutableMap`의 기본 구현인 [`LinkedHashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-linked-hash-map/index.html)은 맵을 반복할 때 요소 삽입 순서를 유지합니다. 반면, 대체 구현인 [`HashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)은 요소 순서를 전혀 보장하지 않습니다.

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

---

# 컬렉션 생성하기

> **원문:** https://kotlinlang.org/docs/constructing-collections.html

## 요소로부터 생성

컬렉션을 생성하는 가장 일반적인 방법은 표준 라이브러리 함수 [`listOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/list-of.html), [`setOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/set-of.html), [`mutableListOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-list-of.html), [`mutableSetOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-set-of.html)를 사용하는 것입니다. 컬렉션 요소를 쉼표로 구분된 인수로 제공하면 컴파일러가 요소 타입을 자동으로 감지합니다. 빈 컬렉션을 생성할 때는 타입을 명시적으로 지정해야 합니다.

```kotlin
val numbersSet = setOf("one", "two", "three", "four")
val emptySet = mutableSetOf<String>()
```

맵 생성 함수인 [`mapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-of.html)와 [`mutableMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-map-of.html)도 동일하게 적용됩니다. 맵의 키와 값은 `Pair` 객체로 전달됩니다(보통 `to` 중위 함수로 생성).

```kotlin
val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)
```

`to` 표기법은 수명이 짧은 `Pair` 객체를 생성하므로 성능이 중요하지 않은 경우에만 사용하는 것이 좋습니다. 과도한 메모리 사용을 피하려면 다른 방법을 사용하세요. 예를 들어, 가변 맵을 생성하고 쓰기 연산으로 채울 수 있습니다. [`apply()`](scope-functions.md#apply) 함수가 이러한 초기화를 유연하게 수행하는 데 도움이 됩니다.

```kotlin
val numbersMap = mutableMapOf<String, String>().apply { this["one"] = "1"; this["two"] = "2" }
```

## 컬렉션 빌더 함수로 생성

컬렉션을 생성하는 또 다른 방법은 빌더 함수인 [`buildList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-list.html), [`buildSet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-set.html), [`buildMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-map.html)을 호출하는 것입니다. 이들은 해당하는 타입의 새로운 가변 컬렉션을 생성하고, [람다 함수](lambdas.md#function-literals-with-receiver)를 사용하여 채운 다음 동일한 요소를 가진 읽기 전용 컬렉션을 반환합니다:

```kotlin
val map = buildMap { // 이것은 MutableMap<String, Int>, 아래 코드에서 타입이 추론됨
    put("a", 1)
    put("b", 0)
    put("c", 4)
}

println(map) // {a=1, b=0, c=4}
```

## 빈 컬렉션

빈 컬렉션을 생성하는 함수도 있습니다: [`emptyList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-list.html), [`emptySet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-set.html), [`emptyMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-map.html). 빈 컬렉션을 생성할 때는 보유할 요소의 타입을 지정해야 합니다.

```kotlin
val empty = emptyList<String>()
```

## 리스트용 초기화 함수

리스트의 경우, 리스트 크기와 인덱스를 기반으로 요소 값을 정의하는 초기화 함수를 받는 생성자가 있는 유사 생성자 함수가 있습니다.

```kotlin
fun main() {
//sampleStart
    val doubled = List(3) { it * 2 }  // 또는 MutableList를 원하면 MutableList
    println(doubled)
//sampleEnd
}
```

## 구체적인 타입 생성자

`ArrayList`나 `LinkedList`와 같은 구체적인 타입의 컬렉션을 생성하려면 이러한 타입의 생성자를 사용할 수 있습니다. `Set`과 `Map` 구현에도 유사한 생성자가 있습니다.

```kotlin
val linkedList = LinkedList<String>(listOf("one", "two", "three"))
val presizedSet = HashSet<Int>(32)
```

## 복사

기존 컬렉션과 동일한 요소를 가진 컬렉션을 생성하려면 복사 함수를 사용할 수 있습니다. 표준 라이브러리의 컬렉션 복사 함수는 동일한 요소에 대한 참조를 가진 _얕은 복사_ 컬렉션을 생성합니다. 따라서 컬렉션 요소에 대한 변경은 모든 복사본에 반영됩니다.

[`toList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-list.html), [`toMutableList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-mutable-list.html), [`toSet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-set.html) 등과 같은 컬렉션 복사 함수는 특정 시점의 컬렉션 스냅샷을 생성합니다. 결과는 동일한 요소를 가진 새 컬렉션입니다. 원본 컬렉션에서 요소를 추가하거나 제거해도 복사본에는 영향을 미치지 않습니다. 복사본도 원본 컬렉션과 독립적으로 변경될 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val sourceList = mutableListOf(1, 2, 3)
    val copyList = sourceList.toMutableList()
    val readOnlyCopyList = sourceList.toList()
    sourceList.add(4)
    println("Copy size: ${copyList.size}")

    //readOnlyCopyList.add(4)             // 컴파일 오류
    println("Read-only copy size: ${readOnlyCopyList.size}")
//sampleEnd
}
```

이러한 함수는 컬렉션을 다른 타입으로 변환하는 데도 사용할 수 있습니다. 예를 들어, 리스트에서 셋을 만들거나 그 반대로 할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val sourceList = mutableListOf(1, 2, 3)
    val copySet = sourceList.toMutableSet()
    copySet.add(3)
    copySet.add(4)
    println(copySet)
//sampleEnd
}
```

또는 동일한 컬렉션 인스턴스에 대한 새로운 참조를 생성할 수 있습니다. 기존 컬렉션으로 컬렉션 변수를 초기화하면 새로운 참조가 생성됩니다. 따라서 참조를 통해 컬렉션 인스턴스가 변경되면 모든 참조에 변경 사항이 반영됩니다.

```kotlin
fun main() {
//sampleStart
    val sourceList = mutableListOf(1, 2, 3)
    val referenceList = sourceList
    referenceList.add(4)
    println("Source size: ${sourceList.size}")
//sampleEnd
}
```

컬렉션 초기화는 가변성을 제한하는 데 사용될 수 있습니다. 예를 들어, `MutableList`에 대한 `List` 참조를 생성하면 이 참조를 통해 컬렉션을 수정하려고 할 때 컴파일러가 오류를 발생시킵니다.

```kotlin
fun main() {
//sampleStart
    val sourceList = mutableListOf(1, 2, 3)
    val referenceList: List<Int> = sourceList
    //referenceList.add(4)            // 컴파일 오류
    sourceList.add(4)
    println(referenceList) // sourceList의 현재 상태를 보여줌
//sampleEnd
}
```

## 다른 컬렉션에서 함수 호출

컬렉션은 다른 컬렉션에 대한 다양한 연산의 결과로 생성될 수 있습니다. 예를 들어, [필터링](collection-filtering.md)은 필터와 일치하는 요소의 리스트를 생성합니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val longerThan3 = numbers.filter { it.length > 3 }
    println(longerThan3)
//sampleEnd
}
```

[매핑](collection-transformations.md#map)은 변환 결과의 리스트를 생성합니다:

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.map { it * 3 })
    println(numbers.mapIndexed { idx, value -> value * idx })
//sampleEnd
}
```

[연관](collection-transformations.md#associate)은 맵을 생성합니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.associateWith { it.length })
//sampleEnd
}
```

Kotlin의 컬렉션 연산에 대한 자세한 내용은 [컬렉션 연산 개요](collection-operations.md)를 참조하세요.

---

# 컬렉션 연산 개요

> **원문:** https://kotlinlang.org/docs/collection-operations.html

Kotlin 표준 라이브러리는 컬렉션에 대한 연산을 수행하기 위한 다양한 함수를 제공합니다. 여기에는 요소를 가져오거나 추가하는 것과 같은 간단한 연산과 검색, 정렬, 필터링, 변환 등과 같은 더 복잡한 연산이 포함됩니다.

## 확장 및 멤버 함수

표준 라이브러리의 컬렉션 연산은 두 가지 방식으로 선언됩니다: 컬렉션 인터페이스의 [멤버 함수](classes.md#class-members)와 [확장 함수](extensions.md#extension-functions).

멤버 함수는 컬렉션 타입에 필수적인 연산을 정의합니다. 예를 들어, [`Collection`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)에는 비어 있는지 확인하는 [`isEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/is-empty.html) 함수가 포함되어 있고; [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)에는 요소에 대한 인덱스 접근을 위한 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/get.html) 등이 포함되어 있습니다.

컬렉션 인터페이스의 자체 구현을 만들 때 멤버 함수를 구현해야 합니다. 새 구현 작성을 더 쉽게 하기 위해 표준 라이브러리의 컬렉션 인터페이스의 스켈레톤 구현을 사용하세요: [`AbstractCollection`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-collection/index.html), [`AbstractList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-list/index.html), [`AbstractSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-set/index.html), [`AbstractMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-map/index.html) 및 그에 대응하는 가변 버전.

필터링, 변환, 순서화 등과 같은 다른 컬렉션 연산은 확장 함수로 선언됩니다.

## 공통 연산

공통 연산은 [읽기 전용 및 가변 컬렉션](collections-overview.md#collection-types) 모두에서 사용할 수 있습니다. 공통 연산은 다음 그룹에 속합니다:

* [변환](collection-transformations.md)
* [필터링](collection-filtering.md)
* [`plus` 및 `minus` 연산자](collection-plus-minus.md)
* [그룹화](collection-grouping.md)
* [컬렉션 일부 조회](collection-parts.md)
* [단일 요소 조회](collection-elements.md)
* [정렬](collection-ordering.md)
* [집계 연산](collection-aggregate.md)

이 페이지에서 설명하는 연산은 원래 컬렉션에 영향을 주지 않고 결과를 반환합니다. 예를 들어, 필터링 연산은 필터링 조건과 일치하는 모든 요소를 포함하는 _새 컬렉션_을 생성합니다. 이러한 연산의 결과는 변수에 저장하거나 다른 방식으로 사용해야 합니다. 예를 들어, 다른 함수에 전달할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    numbers.filter { it.length > 3 }  // `numbers`에 아무 일도 일어나지 않고, 결과가 손실됨
    println("numbers are still $numbers")
    val longerThan3 = numbers.filter { it.length > 3 } // 결과가 `longerThan3`에 저장됨
    println("numbers longer than 3 chars are $longerThan3")
//sampleEnd
}
```

특정 컬렉션 연산의 경우, _대상_ 객체를 지정하는 옵션이 있습니다. 대상은 함수가 새 객체에 결과 항목을 반환하는 대신 결과 항목을 추가하는 가변 컬렉션입니다. 대상이 있는 연산을 수행하기 위해 이름에 `To` 접미사가 있는 별도의 함수가 있습니다. 예를 들어, [`filter()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html) 대신 [`filterTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-to.html), [`associate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate.html) 대신 [`associateTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-to.html). 이러한 함수는 대상 컬렉션을 추가 매개변수로 받습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val filterResults = mutableListOf<String>()  // 대상 객체
    numbers.filterTo(filterResults) { it.length > 3 }
    numbers.filterIndexedTo(filterResults) { index, _ -> index == 0 }
    println(filterResults) // 두 연산의 결과를 포함
//sampleEnd
}
```

편의를 위해 이러한 함수는 대상 컬렉션을 다시 반환하므로 함수 호출의 해당 인수에서 직접 생성할 수 있습니다:

```kotlin
fun main() {
    val numbers = listOf("one", "two", "three", "four")
//sampleStart
    // 숫자를 새 HashSet으로 직접 필터링,
    // 따라서 결과에서 중복 제거
    val result = numbers.mapTo(HashSet()) { it.length }
    println("distinct item lengths are $result")
//sampleEnd
}
```

대상이 있는 함수는 필터링, 연관, 그룹화, 평탄화 및 기타 연산에 사용할 수 있습니다. 대상 연산의 전체 목록은 [Kotlin 컬렉션 참조](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index.html)를 참조하세요.

## 쓰기 연산

가변 컬렉션의 경우, 컬렉션 상태를 변경하는 _쓰기 연산_도 있습니다. 이러한 연산에는 요소 추가, 제거 및 업데이트가 포함됩니다. 쓰기 연산은 [컬렉션 쓰기 연산](collection-write.md)과 [리스트별 연산](list-operations.md#list-write-operations) 및 [맵별 연산](map-operations.md#map-write-operations)의 해당 섹션에 나열되어 있습니다.

특정 연산의 경우, 동일한 연산을 수행하는 함수 쌍이 있습니다: 하나는 연산을 제자리에서 적용하고 다른 하나는 결과를 별도의 컬렉션으로 반환합니다. 예를 들어, [`sort()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort.html)는 가변 컬렉션을 제자리에서 정렬하므로 상태가 변경됩니다; [`sorted()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted.html)는 동일한 요소를 정렬된 순서로 포함하는 새 컬렉션을 생성합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    val sortedNumbers = numbers.sorted()
    println(numbers == sortedNumbers)  // false
    numbers.sort()
    println(numbers == sortedNumbers)  // true
//sampleEnd
}
```

---

# 단일 요소 조회

> **원문:** https://kotlinlang.org/docs/collection-elements.html

Kotlin 컬렉션은 컬렉션에서 단일 요소를 조회하기 위한 함수 세트를 제공합니다. 이 페이지에서 설명하는 함수는 리스트와 셋 모두에 적용됩니다.

[리스트의 정의](collections-overview.md#list)에서 설명한 것처럼 리스트는 순서가 있는 컬렉션입니다. 따라서 리스트의 모든 요소는 참조할 수 있는 위치를 가집니다. 이 페이지에서 설명하는 함수 외에도 리스트는 인덱스로 요소를 조회하고 검색하기 위한 더 넓은 범위의 방법을 제공합니다. 자세한 내용은 [리스트별 연산](list-operations.md)을 참조하세요.

반면, [정의](collections-overview.md#set)에 따르면 셋은 순서가 있는 컬렉션이 아닙니다. 그러나 Kotlin의 `Set`은 요소를 특정 순서로 저장합니다. 이는 구현 타입(순서를 유지하는 `LinkedHashSet`)이거나 요소 순서(순서가 없는 `HashSet`)일 수 있습니다. 셋 요소의 순서를 알 수 없는 경우에도 위치로 요소를 조회하는 것은 여전히 유용할 수 있습니다. 단지 어떤 특정 요소가 반환되는지 알 수 없을 뿐입니다. 이는 랜덤 요소를 원할 때 유용할 수 있습니다.

## 위치로 조회

정확한 위치에서 요소를 조회하려면 [`elementAt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at.html) 함수가 있습니다. `0`부터 `컬렉션 크기 - 1`까지인 정수 숫자를 인수로 호출하면 해당 위치의 컬렉션 요소를 반환합니다. 첫 번째 요소는 `0` 위치를, 마지막 요소는 `(size - 1)` 위치를 가집니다.

`elementAt()`은 인덱스 접근이나 `get()`을 지원하지 않는 컬렉션에 유용합니다. `List`의 경우 [인덱스 접근 연산자](list-operations.md#retrieve-elements-by-index)(`get()` 또는 `[]`)를 사용하는 것이 더 관용적입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = linkedSetOf("one", "two", "three", "four", "five")
    println(numbers.elementAt(3))

    val numbersSortedSet = sortedSetOf("one", "two", "three", "four")
    println(numbersSortedSet.elementAt(0)) // 요소는 오름차순으로 저장됨
//sampleEnd
}
```

컬렉션의 첫 번째 및 마지막 요소를 조회하는 유용한 별칭도 있습니다: [`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)와 [`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html).

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.first())
    println(numbers.last())
//sampleEnd
}
```

존재하지 않는 위치에서 요소를 조회할 때 예외를 피하려면 `elementAt()`의 안전한 변형을 사용하세요:

* [`elementAtOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-null.html)은 지정된 위치가 컬렉션 범위를 벗어나면 null을 반환합니다.
* [`elementAtOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-else.html)는 추가로 `Int` 인수에 매핑되는 람다 함수를 받습니다. 컬렉션 범위를 벗어난 위치로 호출하면 `elementAtOrElse()`는 주어진 값에 대한 람다 결과를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.elementAtOrNull(5))
    println(numbers.elementAtOrElse(5) { index -> "인덱스 $index의 값이 정의되지 않음"})
//sampleEnd
}
```

## 조건으로 조회

[`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)와 [`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html) 함수는 주어진 술어와 일치하는 요소를 찾기 위해 컬렉션을 검색할 수도 있습니다. 컬렉션 요소를 테스트하는 술어와 함께 `first()`를 호출하면 람다가 `true`를 반환하는 첫 번째 요소를 받습니다. 반면, 술어와 함께 `last()`는 일치하는 마지막 요소를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.first { it.length > 3 })
    println(numbers.last { it.startsWith("f") })
//sampleEnd
}
```

술어와 일치하는 요소가 없으면 두 함수 모두 예외를 throw합니다. 이를 피하려면 대신 [`firstOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-or-null.html)과 [`lastOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last-or-null.html)을 사용하세요: 일치하는 요소가 없으면 `null`을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.firstOrNull { it.length > 6 })
//sampleEnd
}
```

이름이 상황에 더 적합한 경우 별칭을 사용하세요:

* `firstOrNull()` 대신 [`find()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find.html)를 사용
* `lastOrNull()` 대신 [`findLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find-last.html)를 사용

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.find { it % 2 == 0 })
    println(numbers.findLast { it % 2 == 0 })
//sampleEnd
}
```

## 선택자로 조회

컬렉션을 매핑한 다음 매핑된 컬렉션의 최소 요소에 해당하는 원래 요소를 조회해야 하는 경우 [`firstNotNullOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-not-null-of.html) 함수를 사용하세요.

```kotlin
fun main() {
//sampleStart
    val list = listOf<Any>(0, "true", false)
    // 각 요소를 문자열로 변환하고 필요한 길이를 가진 것을 반환
    val longEnough = list.firstNotNullOf { item -> item.toString().takeIf { it.length >= 4 } }
    println(longEnough)
//sampleEnd
}
```

## 랜덤 요소

컬렉션의 임의의 요소를 조회해야 하는 경우 [`random()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random.html) 함수를 호출하세요. 인수 없이 호출하거나 [`Random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/index.html) 객체를 무작위 소스로 사용하여 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.random())
//sampleEnd
}
```

빈 컬렉션에서 `random()`은 예외를 throw합니다. 대신 `null`을 받으려면 [`randomOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random-or-null.html)을 사용하세요.

## 존재 확인

컬렉션에 요소가 있는지 확인하려면 [`contains()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains.html) 함수를 사용하세요. 함수 인수와 `equals()`인 컬렉션 요소가 있으면 `true`를 반환합니다. `in` 키워드를 사용하여 연산자 형태로 `contains()`를 호출할 수 있습니다.

여러 인스턴스의 존재를 한 번에 확인하려면 이러한 인스턴스의 컬렉션을 인수로 사용하여 [`containsAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains-all.html)을 호출합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.contains("four"))
    println("zero" in numbers)

    println(numbers.containsAll(listOf("four", "two")))
    println(numbers.containsAll(listOf("one", "zero")))
//sampleEnd
}
```

또한 [`isEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-empty.html)와 [`isNotEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-not-empty.html)를 호출하여 컬렉션에 요소가 있는지 확인할 수 있습니다:

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.isEmpty())
    println(numbers.isNotEmpty())

    val empty = emptyList<String>()
    println(empty.isEmpty())
    println(empty.isNotEmpty())
//sampleEnd
}
```

---

# 정렬

> **원문:** https://kotlinlang.org/docs/collection-ordering.html

요소의 순서는 특정 컬렉션 타입에서 중요한 측면입니다. 예를 들어, 동일한 요소를 가진 두 리스트는 요소가 다르게 정렬되면 같지 않습니다.

Kotlin에서 객체의 순서는 여러 방식으로 정의할 수 있습니다.

먼저, 대부분의 내장 타입에는 _자연_ 순서가 있습니다:

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

---

# 집계 연산

> **원문:** https://kotlinlang.org/docs/collection-aggregate.html

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

---

# 컬렉션 일부 조회

> **원문:** https://kotlinlang.org/docs/collection-parts.html

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

---

# 플러스 및 마이너스 연산자

> **원문:** https://kotlinlang.org/docs/collection-plus-minus.html

Kotlin에서 [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)와 [`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자는 컬렉션에 정의되어 있습니다. 이들은 컬렉션을 첫 번째 피연산자로 받습니다; 두 번째 피연산자는 요소이거나 다른 컬렉션일 수 있습니다. 반환 값은 새로운 읽기 전용 컬렉션입니다:

* `plus`의 결과는 원래 컬렉션의 요소 _및_ 두 번째 피연산자를 포함합니다.
* `minus`의 결과는 원래 컬렉션의 요소에서 두 번째 피연산자의 요소를 _제외한_ 것을 포함합니다. 두 번째 피연산자가 요소인 경우 `minus`는 해당 요소의 _첫 번째_ 발생을 제거합니다; 컬렉션인 경우 해당 요소의 _모든_ 발생을 제거합니다.

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

맵에서 `plus`와 `minus`의 세부 사항은 [맵별 연산](map-operations.md)을 참조하세요. [증가 대입 연산자](operator-overloading.md#augmented-assignments) [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)과 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`)도 컬렉션에 정의되어 있습니다. 그러나 읽기 전용 컬렉션의 경우 실제로 `plus` 또는 `minus` 연산자를 사용하고 결과를 동일한 변수에 재할당하려고 합니다. 따라서 `var` 읽기 전용 컬렉션에서만 사용할 수 있습니다. 가변 컬렉션의 경우 `val`로 선언된 경우 컬렉션을 제자리에서 수정합니다. 자세한 내용은 [컬렉션 쓰기 연산](collection-write.md)을 참조하세요.

---

# 그룹화

> **원문:** https://kotlinlang.org/docs/collection-grouping.html

Kotlin 표준 라이브러리는 컬렉션 요소를 그룹화하기 위한 확장 함수를 제공합니다. 기본 함수 [`groupBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/group-by.html)는 람다 함수를 받아 `Map`을 반환합니다. 이 맵에서 각 키는 람다 결과이고 해당 값은 이 결과가 반환되는 요소의 `List`입니다. 이 함수는 예를 들어 `String` 리스트를 첫 글자로 그룹화하는 데 사용할 수 있습니다.

두 번째 람다 인수인 값 변환 함수와 함께 `groupBy()`를 호출할 수도 있습니다. 두 람다가 있는 결과 맵에서 `keySelector` 함수가 생성한 키는 원래 요소 대신 값 변환 함수의 결과에 매핑됩니다.

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

---

# 컬렉션 쓰기 연산

> **원문:** https://kotlinlang.org/docs/collection-write.html

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

컬렉션에서 요소를 제거하는 또 다른 방법은 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 - [`minus`](collection-plus-minus.md)의 제자리 버전입니다. 두 번째 인수는 요소 타입의 단일 인스턴스이거나 다른 컬렉션일 수 있습니다. 오른쪽에 단일 요소가 있으면 `-=`는 해당 요소의 _첫 번째_ 발생을 제거합니다. 반면, 컬렉션인 경우 해당 요소의 _모든_ 발생이 제거됩니다. 예를 들어, 리스트에 중복 요소가 있으면 한 번에 모두 제거됩니다. 두 번째 피연산자는 컬렉션에 없는 요소를 포함할 수 있습니다. 이러한 요소는 연산 실행에 영향을 미치지 않습니다.

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

---

# 컬렉션 변환 연산

> **원문:** https://kotlinlang.org/docs/collection-transformations.html

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

여기서 먼저 요소마다 변환 함수를 호출한 다음 해당 함수 결과의 속성에서 쌍을 구축합니다.

## Flatten

중첩된 컬렉션을 다루는 경우 중첩된 컬렉션 요소에 대한 평면 접근을 제공하는 표준 라이브러리 함수가 유용할 수 있습니다.

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

---

# 컬렉션 필터링

> **원문:** https://kotlinlang.org/docs/collection-filtering.html

필터링은 컬렉션 처리에서 가장 인기 있는 작업 중 하나입니다. Kotlin에서 필터링 조건은 _술어(predicate)_로 정의됩니다 - 컬렉션 요소를 받아 불리언 값을 반환하는 람다 함수: `true`는 주어진 요소가 술어와 일치함을 의미하고, `false`는 반대를 의미합니다.

표준 라이브러리에는 단일 호출로 컬렉션을 필터링할 수 있는 확장 함수 그룹이 있습니다. 이러한 함수는 원래 컬렉션을 변경하지 않으므로 [가변 및 읽기 전용](collections-overview.md#collection-types) 컬렉션 모두에서 사용할 수 있습니다. 필터링 결과를 연산하려면 변수에 할당하거나 필터링 후 함수를 체이닝해야 합니다.

## 술어로 필터링

기본 필터링 함수는 [`filter()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html)입니다. 술어와 함께 호출하면 `filter()`는 일치하는 컬렉션 요소를 반환합니다. `List`와 `Set` 모두 결과 컬렉션은 `List`이고, `Map`의 경우에도 `Map`입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val longerThan3 = numbers.filter { it.length > 3 }
    println(longerThan3)

    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
    println(filteredMap)
//sampleEnd
}
```

`filter()`의 술어는 요소의 값만 확인할 수 있습니다. 필터에서 요소 위치를 사용하려면 [`filterIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-indexed.html)를 사용합니다. 이는 두 인수를 받는 술어를 받습니다: 요소의 인덱스와 값.

부정 조건으로 컬렉션을 필터링하려면 [`filterNot()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not.html)을 사용합니다. 이는 술어가 `false`를 반환하는 요소의 리스트를 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    val filteredIdx = numbers.filterIndexed { index, s -> (index != 0) && (s.length < 5) }
    val filteredNot = numbers.filterNot { it.length <= 3 }

    println(filteredIdx)
    println(filteredNot)
//sampleEnd
}
```

주어진 타입의 요소를 필터링하는 함수도 있습니다:

* [`filterIsInstance()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-is-instance.html)는 주어진 타입의 컬렉션 요소를 반환합니다. `List<Any>`에서 호출하면 `filterIsInstance<T>()`는 `List<T>`를 반환하므로 `T` 타입의 함수를 항목에서 호출할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(null, 1, "two", 3.0, "four")
    println("대문자로 된 모든 String 요소:")
    numbers.filterIsInstance<String>().forEach {
        println(it.uppercase())
    }
//sampleEnd
}
```

* [`filterNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not-null.html)은 모든 널이 아닌 요소를 반환합니다. `List<T?>`에서 호출하면 `filterNotNull()`은 `List<T: Any>`를 반환하므로 요소를 널이 아닌 객체로 취급할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(null, "one", "two", null)
    numbers.filterNotNull().forEach {
        println(it.length)   // 널이 가능한 String에는 length를 사용할 수 없음
    }
//sampleEnd
}
```

## 분할

또 다른 필터링 함수인 [`partition()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/partition.html)은 술어로 컬렉션을 필터링하고 일치하지 않는 요소를 별도의 리스트에 유지합니다. 따라서 반환 값으로 `List`의 `Pair`를 갖습니다: 첫 번째 리스트는 술어와 일치하는 요소를 포함하고 두 번째 리스트는 원래 컬렉션의 다른 모든 것을 포함합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val (match, rest) = numbers.partition { it.length > 3 }

    println(match)
    println(rest)
//sampleEnd
}
```

## 술어 테스트

마지막으로, 컬렉션 요소에 대해 술어를 단순히 테스트하는 함수가 있습니다:

* [`any()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/any.html)는 최소 하나의 요소가 주어진 술어와 일치하면 `true`를 반환합니다.
* [`none()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/none.html)은 어떤 요소도 주어진 술어와 일치하지 않으면 `true`를 반환합니다.
* [`all()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/all.html)은 모든 요소가 주어진 술어와 일치하면 `true`를 반환합니다. `all()`은 빈 컬렉션에서 유효한 술어와 함께 호출하면 `true`를 반환합니다. 이러한 동작은 논리학에서 _공허한 참(vacuous truth)_으로 알려져 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println(numbers.any { it.endsWith("e") })
    println(numbers.none { it.endsWith("a") })
    println(numbers.all { it.endsWith("e") })

    println(emptyList<Int>().all { it > 5 })   // 공허한 참
//sampleEnd
}
```

`any()`와 `none()`은 술어 없이도 사용할 수 있습니다: 이 경우 컬렉션이 비어 있는지 확인합니다. `any()`는 요소가 있으면 `true`를 반환하고 없으면 `false`를 반환합니다; `none()`은 반대로 합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val empty = emptyList<String>()

    println(numbers.any())
    println(empty.any())

    println(numbers.none())
    println(empty.none())
//sampleEnd
}
```

---

# 리스트별 연산

> **원문:** https://kotlinlang.org/docs/list-operations.html

[`List`](collections-overview.md#list)는 Kotlin에서 가장 인기 있는 내장 컬렉션 타입입니다. 리스트 요소에 대한 인덱스 접근은 리스트에 강력한 연산 세트를 제공합니다.

## 인덱스로 요소 조회

리스트는 요소 조회를 위한 모든 일반 연산을 지원합니다: `elementAt()`, `first()`, `last()` 및 [단일 요소 조회](collection-elements.md)에 나열된 기타 연산. 리스트에 고유한 것은 요소에 대한 인덱스 접근이므로 요소를 읽는 가장 간단한 방법은 인덱스로 조회하는 것입니다. 이는 인수로 인덱스를 전달하는 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/get.html) 함수나 단축 `[index]` 구문으로 수행됩니다.

리스트 크기보다 작은 인덱스로 접근하면 예외가 throw됩니다. 이를 피하기 위한 두 가지 다른 함수가 있습니다:

* [`getOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-else.html)를 사용하면 인덱스가 컬렉션에 없을 때 기본값을 계산하는 함수를 제공할 수 있습니다.
* [`getOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-null.html)은 기본값으로 `null`을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.get(0))
    println(numbers[0])
    //numbers.get(5)                         // 예외!
    println(numbers.getOrNull(5))             // null
    println(numbers.getOrElse(5, {it}))        // 5
//sampleEnd
}
```

## 리스트 일부 조회

[컬렉션 일부 조회](collection-parts.md) 연산 외에도 리스트는 지정된 범위의 요소를 리스트로 반환하는 [`subList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/sub-list.html) 함수를 제공합니다. 따라서 원래 컬렉션의 요소가 변경되면 이전에 생성된 하위 리스트에서도 변경되고 그 반대도 마찬가지입니다.

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.subList(3, 6))
//sampleEnd
}
```

## 요소 위치 찾기

### 선형 검색

모든 리스트에서 [`indexOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of.html)와 [`lastIndexOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last-index-of.html) 함수를 사용하여 요소의 위치를 찾을 수 있습니다. 이들은 리스트에서 인수와 같은 요소의 첫 번째와 마지막 위치를 반환합니다. 두 함수 모두 해당 요소가 없으면 `-1`을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4, 2, 5)
    println(numbers.indexOf(2))
    println(numbers.lastIndexOf(2))
//sampleEnd
}
```

주어진 술어와 일치하는 요소를 검색하는 함수 쌍도 있습니다:

* [`indexOfFirst()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of-first.html)는 술어와 일치하는 _첫 번째_ 요소의 인덱스를 반환하거나 해당 요소가 없으면 `-1`을 반환합니다.
* [`indexOfLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of-last.html)는 술어와 일치하는 _마지막_ 요소의 인덱스를 반환하거나 해당 요소가 없으면 `-1`을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    println(numbers.indexOfFirst { it > 2})
    println(numbers.indexOfLast { it % 2 == 1})
//sampleEnd
}
```

### 정렬된 리스트에서 이진 검색

리스트에서 요소를 검색하는 또 다른 방법이 있습니다 - [이진 검색](https://en.wikipedia.org/wiki/Binary_search_algorithm). 이는 다른 검색 함수보다 훨씬 빠르게 작동하지만 _리스트가 특정 순서(자연 순서 또는 함수 매개변수에 제공된 다른 순서)에 따라 오름차순으로 [정렬](collection-ordering.md)되어야 합니다_. 그렇지 않으면 결과가 정의되지 않습니다.

정렬된 리스트에서 요소를 검색하려면 요소를 인수로 전달하면서 [`binarySearch()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/binary-search.html) 함수를 호출합니다. 해당 요소가 존재하면 함수는 그 인덱스를 반환합니다; 그렇지 않으면 `(-insertionPoint - 1)`을 반환합니다. 여기서 `insertionPoint`는 이 요소가 삽입되어야 리스트가 정렬된 상태를 유지하는 인덱스입니다. 주어진 값을 가진 요소가 여러 개인 경우 검색은 그 중 어떤 것의 인덱스도 반환할 수 있습니다.

검색할 범위를 지정할 수도 있습니다: 이 경우 함수는 두 제공된 인덱스 사이에서만 검색합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    numbers.sort()
    println(numbers)
    println(numbers.binarySearch("two"))  // 3
    println(numbers.binarySearch("z")) // -5
    println(numbers.binarySearch("two", 0, 2))  // -3
//sampleEnd
}
```

#### Comparator 이진 검색

리스트 요소가 `Comparable`이 아닌 경우 이진 검색에 사용할 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html)를 제공해야 합니다. 리스트는 이 `Comparator`에 따라 오름차순으로 정렬되어야 합니다. 예를 살펴보겠습니다:

```kotlin
data class Product(val name: String, val price: Double)

fun main() {
//sampleStart
    val productList = listOf(
        Product("WebStorm", 49.0),
        Product("AppCode", 99.0),
        Product("DotTrace", 129.0),
        Product("ReSharper", 149.0))

    println(productList.binarySearch(Product("AppCode", 99.0), compareBy<Product> { it.price }.thenBy { it.name }))
//sampleEnd
}
```

다음은 `Comparator`로 `String.CASE_INSENSITIVE_ORDER`를 사용하여 문자열 리스트에서 대소문자를 구분하지 않고 검색하는 예입니다:

```kotlin
fun main() {
//sampleStart
    val colors = listOf("Blue", "green", "ORANGE", "Red", "yellow")
    println(colors.binarySearch("RED", String.CASE_INSENSITIVE_ORDER)) // 3
//sampleEnd
}
```

#### 비교 이진 검색

명시적 검색 값 없이 _비교_ 함수를 사용하는 이진 검색을 사용하면 자연 순서나 `Comparator`가 필요하지 않은 요소를 찾을 수 있습니다. 대신 요소를 `Int` 값에 매핑하는 비교 함수를 받습니다. 함수는 검색된 요소보다 작은 요소에 대해 양수를 반환하고, 검색된 요소보다 큰 요소에 대해 음수를 반환하며, 요소가 같으면 0을 반환해야 합니다. 제공된 비교기에 따라 리스트가 오름차순으로 정렬되어 있어야 합니다.

이러한 검색은 전체 순서에 대한 비교 연산을 구현하지 않고도 정렬된 리스트에서 요소를 조회할 수 있게 합니다.

```kotlin
import kotlin.math.sign
//sampleStart
data class Product(val name: String, val price: Double)

fun priceComparison(product: Product, price: Double) = sign(product.price - price).toInt()

fun main() {
    val productList = listOf(
        Product("WebStorm", 49.0),
        Product("AppCode", 99.0),
        Product("DotTrace", 129.0),
        Product("ReSharper", 149.0))

    println(productList.binarySearch { priceComparison(it, 99.0) })
}
//sampleEnd
```

## 리스트 쓰기 연산

[컬렉션 쓰기 연산](collection-write.md) 외에도 [가변](collections-overview.md#collection-types) 리스트는 특정 쓰기 연산을 지원합니다. 이러한 연산은 인덱스를 사용하여 요소에 접근하여 리스트 수정 기능을 확장합니다.

### 추가

리스트의 특정 위치에 요소를 추가하려면 요소 위치를 추가 인수로 사용하여 [`add()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/add.html)와 [`addAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/add-all.html)을 호출합니다. 해당 위치 뒤의 모든 요소가 오른쪽으로 이동합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "five", "six")
    numbers.add(1, "two")
    numbers.addAll(2, listOf("three", "four"))
    println(numbers)
//sampleEnd
}
```

### 업데이트

리스트는 주어진 위치에서 요소를 교체하는 함수인 [`set()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/set.html)과 그 연산자 형태 `[]`도 제공합니다. `set()`은 다른 요소의 인덱스를 변경하지 않습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "five", "three")
    numbers[1] =  "two"
    println(numbers)
//sampleEnd
}
```

[`fill()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fill.html)은 모든 컬렉션 요소를 지정된 값으로 간단히 교체합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.fill(3)
    println(numbers)
//sampleEnd
}
```

### 제거

리스트에서 특정 위치의 요소를 제거하려면 위치를 인수로 사용하여 [`removeAt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/remove-at.html) 함수를 호출합니다. 제거된 요소 뒤의 모든 요소 인덱스가 1씩 감소합니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4, 3)
    numbers.removeAt(1)
    println(numbers)
//sampleEnd
}
```

### 정렬

[컬렉션 정렬](collection-ordering.md)에서 정렬된 순서로 컬렉션 요소를 조회하는 연산을 설명합니다. 가변 리스트의 경우 표준 라이브러리는 리스트를 제자리에서 정렬하는 유사한 확장 함수를 제공합니다. 이러한 연산을 리스트 인스턴스에 적용하면 해당 인스턴스의 요소 순서가 변경됩니다.

제자리 정렬 함수는 `sorted*` 대신 `sort*` 이름을 사용합니다: [`sort()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort.html), [`sortDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort-descending.html), [`sortBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort-by.html) 등.

또한 참조를 별도의 변수에 복사하지 않고 가변 리스트의 요소 순서를 뒤집는 [`shuffle()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/shuffle.html)과 [`reverse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reverse.html)도 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")

    numbers.sort()
    println("오름차순 정렬: $numbers")
    numbers.sortDescending()
    println("내림차순 정렬: $numbers")

    numbers.sortBy { it.length }
    println("길이로 오름차순 정렬: $numbers")
    numbers.sortByDescending { it.last() }
    println("마지막 글자로 내림차순 정렬: $numbers")

    numbers.sortWith(compareBy<String> { it.length }.thenBy { it })
    println("Comparator로 정렬: $numbers")

    numbers.shuffle()
    println("섞기: $numbers")

    numbers.reverse()
    println("뒤집기: $numbers")
//sampleEnd
}
```

---

# 셋별 연산

> **원문:** https://kotlinlang.org/docs/set-operations.html

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

---

# 맵별 연산

> **원문:** https://kotlinlang.org/docs/map-operations.html

[맵](collections-overview.md#map)에서 키와 값의 타입은 사용자가 정의합니다. 맵 엔트리에 대한 키 기반 접근은 키로 값 조회부터 키와 값의 별도 필터링까지 다양한 맵별 처리 기능을 가능하게 합니다. 이 페이지에서는 표준 라이브러리의 맵 처리 함수를 설명합니다.

## 키와 값 조회

맵에서 값을 조회하려면 키를 인수로 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/get.html) 함수에 제공해야 합니다. 단축 `[key]` 구문도 지원됩니다. 주어진 키를 찾지 못하면 `null`을 반환합니다. 또한 맵에서 키를 찾을 수 없을 때 예외를 throw하는 [`getValue()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-value.html) 함수도 있습니다. 또한 누락된 키에 대한 기본값을 생성하는 두 가지 옵션이 더 있습니다:

* [`getOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-else.html)는 `List`와 동일하게 작동합니다: 해당 키 값에 대한 키를 찾을 수 없으면 주어진 람다에서 값을 반환합니다.
* [`getOrDefault()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-default.html)는 키를 찾을 수 없으면 지정된 기본값을 반환합니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap.get("one"))
    println(numbersMap["one"])
    println(numbersMap.getOrDefault("four", 10))
    println(numbersMap["five"])               // null
    //numbersMap.getValue("six")      // 예외!
//sampleEnd
}
```

맵의 모든 키 또는 모든 값에 대한 연산을 수행하려면 각각 `keys` 및 `values` 속성에서 조회할 수 있습니다. `keys`는 맵 키의 셋이고 `values`는 맵 값의 컬렉션입니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap.keys)
    println(numbersMap.values)
//sampleEnd
}
```

## 필터링

다른 컬렉션과 마찬가지로 [`filter()`](collection-filtering.md) 함수로 맵을 [필터링](collection-filtering.md)할 수 있습니다. 맵에서 `filter()`를 호출할 때 `Pair`를 인수로 하는 술어를 전달합니다. 이를 통해 필터링 술어에서 키와 값을 모두 사용할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
    println(filteredMap)
//sampleEnd
}
```

맵을 필터링하는 두 가지 특정 방법도 있습니다: 키로 필터링과 값으로 필터링. 각각에 대한 함수가 있습니다: [`filterKeys()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-keys.html)와 [`filterValues()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-values.html). 둘 다 주어진 술어와 일치하는 엔트리의 새 맵을 반환합니다. `filterKeys()`의 술어는 요소 키만 확인하고 `filterValues()`의 술어는 값만 확인합니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredKeysMap = numbersMap.filterKeys { it.endsWith("1") }
    val filteredValuesMap = numbersMap.filterValues { it < 10 }

    println(filteredKeysMap)
    println(filteredValuesMap)
//sampleEnd
}
```

## `plus`와 `minus` 연산자

요소에 대한 키 접근 때문에 [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)와 [`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자는 다른 컬렉션과 다르게 맵에서 작동합니다. `plus`는 왼쪽 `Map`의 모든 엔트리와 오른쪽의 `Pair` 또는 다른 `Map` 요소를 포함하는 `Map`을 반환합니다. 오른쪽 피연산자에 왼쪽 `Map`에 있는 키가 있는 엔트리가 포함된 경우 결과 맵에는 오른쪽의 엔트리가 포함됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap + Pair("four", 4))
    println(numbersMap + Pair("one", 10))
    println(numbersMap + mapOf("five" to 5, "one" to 11))
//sampleEnd
}
```

`minus`는 키가 오른쪽 피연산자에서 가져오는지에 따라 왼쪽 `Map`의 엔트리에서 `Map`을 생성합니다. 따라서 오른쪽 피연산자는 단일 키이거나 키 컬렉션(리스트, 셋 등)일 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap - "one")
    println(numbersMap - listOf("two", "four"))
//sampleEnd
}
```

가변 맵에서 [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)과 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 사용에 대한 자세한 내용은 아래 [맵 쓰기 연산](#map-write-operations)을 참조하세요.

## 맵 쓰기 연산 {id="map-write-operations"}

[가변](collections-overview.md#collection-types) 맵은 맵별 쓰기 연산을 제공합니다. 이러한 연산을 사용하면 키 기반 값 접근을 사용하여 맵 내용을 변경할 수 있습니다.

맵 쓰기 연산에 대한 특정 규칙이 있습니다:

* 값은 업데이트될 수 있습니다. 반면, 키는 절대 변경되지 않습니다: 엔트리를 추가하면 키가 상수입니다.
* 각 키에는 항상 하나의 연관된 값이 있습니다. 전체 엔트리를 추가하고 제거할 수 있습니다.

다음은 가변 맵에서 사용할 수 있는 쓰기 연산에 대한 표준 라이브러리 함수 설명입니다.

### 엔트리 추가 및 업데이트

가변 맵에 새 키-값 쌍을 추가하려면 [`put()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/put.html)을 사용합니다. `LinkedHashMap`(기본 맵 구현)에 새 엔트리를 넣으면 맵을 반복할 때 마지막에 오도록 추가됩니다. 정렬된 맵에서 새 요소의 위치는 키 순서에 따라 정해집니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap.put("three", 3)
    println(numbersMap)
//sampleEnd
}
```

한 번에 여러 엔트리를 추가하려면 [`putAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/put-all.html)을 사용합니다. 인수는 `Map`이거나 `Pair`의 그룹(`Iterable`, `Sequence` 또는 `Array`)일 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap.putAll(setOf("four" to 4, "five" to 5))
    println(numbersMap)
//sampleEnd
}
```

`put()`과 `putAll()` 모두 주어진 키가 맵에 이미 존재하면 값을 덮어씁니다. 따라서 이를 사용하여 맵 엔트리의 값을 업데이트할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    val previousValue = numbersMap.put("one", 11)
    println("이전에 'one'과 연관된 값: $previousValue, 이후: ${numbersMap["one"]}")
    println(numbersMap)
//sampleEnd
}
```

단축 연산자 형태를 사용하여 맵에 새 엔트리를 추가할 수도 있습니다. 두 가지 방법이 있습니다:

* [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`) 연산자.
* `[]` 연산자 - `set()`의 별칭.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap["three"] = 3     // numbersMap.put("three", 3) 호출
    numbersMap += mapOf("four" to 4, "five" to 5)
    println(numbersMap)
//sampleEnd
}
```

맵에 있는 키로 호출하면 연산자는 해당 엔트리의 값을 덮어씁니다.

### 엔트리 제거

가변 맵에서 엔트리를 제거하려면 [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/remove.html) 함수를 사용합니다. `remove()`를 호출할 때 키나 전체 키-값 쌍을 전달할 수 있습니다. 키와 값을 모두 지정하면 이 키의 값이 두 번째 인수와 일치하는 경우에만 요소가 제거됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap.remove("one")
    println(numbersMap)
    numbersMap.remove("three", 4)            // 아무것도 제거하지 않음
    println(numbersMap)
//sampleEnd
}
```

가변 맵의 `keys` 또는 `values`에서 키나 값을 제거하여 엔트리를 제거할 수도 있습니다. `values`에서 `remove()`를 호출하면 주어진 값과 일치하는 첫 번째 엔트리만 제거됩니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3, "threeAgain" to 3)
    numbersMap.keys.remove("one")
    println(numbersMap)
    numbersMap.values.remove(3)
    println(numbersMap)
//sampleEnd
}
```

가변 맵에서 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자도 사용할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap -= "two"
    println(numbersMap)
    numbersMap -= "five"             // 아무것도 제거하지 않음
    println(numbersMap)
//sampleEnd
}
```

---

# 시퀀스

> **원문:** https://kotlinlang.org/docs/sequences.html

컬렉션과 함께, Kotlin 표준 라이브러리에는 또 다른 타입인 _시퀀스_([`Sequence<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html))가 있습니다. 컬렉션과 달리 시퀀스는 요소를 포함하지 않고, 반복하는 동안 요소를 생성합니다. 시퀀스는 [`Iterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)과 동일한 함수를 제공하지만 다단계 컬렉션 처리를 위한 다른 접근 방식을 구현합니다.

`Iterable`의 처리가 여러 단계를 포함할 때, 즉시(eagerly) 실행됩니다: 각 처리 단계가 완료되고 그 결과인 중간 컬렉션을 반환합니다. 다음 단계는 이 컬렉션에서 실행됩니다. 반면, 시퀀스의 다단계 처리는 가능한 경우 지연(lazily) 실행됩니다: 실제 계산은 전체 처리 체인의 결과가 요청될 때만 발생합니다.

연산 실행 순서도 다릅니다: `Sequence`는 모든 처리 단계를 각 요소에 대해 하나씩 수행합니다. 반면, `Iterable`은 전체 컬렉션에 대해 각 단계를 완료한 후 다음 단계로 진행합니다.

따라서 시퀀스는 중간 단계의 결과를 구축하는 것을 피하여 전체 컬렉션 처리 체인의 성능을 향상시킵니다. 그러나 시퀀스의 지연 특성은 작은 컬렉션을 처리하거나 더 간단한 계산을 수행할 때 상당한 오버헤드를 추가합니다. 따라서 [`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)와 [`Iterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)을 모두 고려하고 어떤 것이 특정 상황에 적합한지 결정해야 합니다.

## 생성

### 요소로부터

시퀀스를 생성하려면 [`sequenceOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/sequence-of.html) 함수를 호출하고 요소를 인수로 나열합니다.

```kotlin
val numbersSequence = sequenceOf("four", "three", "two", "one")
```

### Iterable에서

이미 `Iterable` 객체(예: `List` 또는 `Set`)가 있는 경우 [`asSequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/as-sequence.html)를 호출하여 시퀀스를 생성할 수 있습니다.

```kotlin
val numbers = listOf("one", "two", "three", "four")
val numbersSequence = numbers.asSequence()
```

### 함수로부터

시퀀스를 생성하는 또 다른 방법은 요소를 계산하는 함수로 빌드하는 것입니다. 함수를 기반으로 시퀀스를 빌드하려면 이 함수를 인수로 사용하여 [`generateSequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/generate-sequence.html)를 호출합니다. 선택적으로 명시적 값이나 함수 호출의 결과인 첫 번째 요소를 지정할 수 있습니다. 제공된 함수가 `null`을 반환하면 시퀀스 생성이 중지됩니다. 따라서 아래 예제의 시퀀스는 무한합니다.

```kotlin
fun main() {
//sampleStart
    val oddNumbers = generateSequence(1) { it + 2 } // `it`은 이전 요소
    println(oddNumbers.take(5).toList())
    //println(oddNumbers.count())     // 오류: 시퀀스가 무한함
//sampleEnd
}
```

`generateSequence()`로 유한 시퀀스를 생성하려면 필요한 마지막 요소 다음에 `null`을 반환하는 함수를 제공합니다.

```kotlin
fun main() {
//sampleStart
    val oddNumbersLessThan10 = generateSequence(1) { if (it < 8) it + 2 else null }
    println(oddNumbersLessThan10.count())
//sampleEnd
}
```

### 청크로부터

마지막으로, 시퀀스 요소를 하나씩 또는 임의 크기의 청크로 생성할 수 있는 함수가 있습니다 - [`sequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/sequence.html) 함수입니다. 이 함수는 [`yield()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence-scope/yield.html) 및 [`yieldAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence-scope/yield-all.html) 함수 호출을 포함하는 람다 표현식을 받습니다. 이들은 시퀀스 소비자에게 요소를 반환하고 소비자가 다음 요소를 요청할 때까지 `sequence()`의 실행을 일시 중단합니다. `yield()`는 단일 요소를 인수로 받고; `yieldAll()`은 `Iterable` 객체, `Iterator`, 또는 다른 `Sequence`를 받을 수 있습니다. `yieldAll()`의 `Sequence` 인수는 무한할 수 있습니다. 그러나 이러한 호출은 마지막이어야 합니다: 모든 후속 호출은 절대 실행되지 않습니다.

```kotlin
fun main() {
//sampleStart
    val oddNumbers = sequence {
        yield(1)
        yieldAll(listOf(3, 5))
        yieldAll(generateSequence(7) { it + 2 })
    }
    println(oddNumbers.take(5).toList())
//sampleEnd
}
```

## 시퀀스 연산

시퀀스 연산은 상태 요구 사항에 따라 다음 그룹으로 분류할 수 있습니다:

* _무상태_ 연산은 상태가 필요 없으며 각 요소를 독립적으로 처리합니다. 예: [`map()`](collection-transformations.md#map) 또는 [`filter()`](collection-filtering.md). 무상태 연산은 요소를 처리하기 위해 상수 양의 상태가 필요할 수도 있습니다. 예: [`take()` 또는 `drop()`](collection-parts.md).
* _상태유지_ 연산은 일반적으로 시퀀스의 요소 수에 비례하는 상당한 양의 상태를 필요로 합니다.

시퀀스 연산이 다른 시퀀스를 반환하고 지연 생성되는 경우 _중간_ 연산이라고 합니다. 그렇지 않으면 연산은 _터미널_입니다. 터미널 연산의 예로는 [`toList()`](constructing-collections.md#copy) 또는 [`sum()`](collection-aggregate.md)이 있습니다. 시퀀스 요소는 터미널 연산으로만 검색할 수 있습니다.

시퀀스는 여러 번 반복할 수 있습니다; 그러나 일부 시퀀스 구현은 한 번만 반복할 수 있도록 제한할 수 있습니다. 이는 문서에 구체적으로 명시되어 있습니다.

## 시퀀스 처리 예제

Iterable과 Sequence의 차이점을 예제를 통해 살펴보겠습니다.

### Iterable

단어 리스트가 있다고 가정해 보겠습니다. 아래 코드는 세 글자보다 긴 단어를 필터링하고 그러한 처음 네 단어의 길이를 출력합니다.

```kotlin
fun main() {
//sampleStart
    val words = "The quick brown fox jumps over the lazy dog".split(" ")
    val lengthsList = words.filter { println("filter: $it"); it.length > 3 }
        .map { println("length: ${it.length}"); it.length }
        .take(4)

    println("세 글자보다 긴 처음 4개 단어의 길이:")
    println(lengthsList)
//sampleEnd
}
```

이 코드를 실행하면 `filter()` 및 `map()` 함수가 코드에 나타나는 것과 동일한 순서로 실행되는 것을 볼 수 있습니다. 먼저 모든 요소에 대해 `filter:`를 보고, 그 다음 필터링 후 남은 요소에 대해 `length:`를 보고, 마지막으로 마지막 두 줄의 출력을 봅니다.

리스트 처리가 이렇게 진행됩니다:

![리스트 처리](list-processing.png)

### Sequence

이제 시퀀스로 동일한 것을 작성해 보겠습니다:

```kotlin
fun main() {
//sampleStart
    val words = "The quick brown fox jumps over the lazy dog".split(" ")
    // 리스트를 시퀀스로 변환
    val wordsSequence = words.asSequence()

    val lengthsSequence = wordsSequence.filter { println("filter: $it"); it.length > 3 }
        .map { println("length: ${it.length}"); it.length }
        .take(4)

    println("세 글자보다 긴 처음 4개 단어의 길이")
    // 터미널 연산: 결과를 List로 얻기
    println(lengthsSequence.toList())
//sampleEnd
}
```

이 코드의 출력은 `filter()` 및 `map()` 함수가 결과 리스트가 빌드될 때만 호출됨을 보여줍니다. 따라서 먼저 "Lengths of.."라는 텍스트 줄을 보고, 그 다음 시퀀스 처리가 시작됩니다. 필터링 후 남은 요소의 경우, 다음 요소를 필터링하기 전에 맵이 실행됩니다. 결과 크기가 4에 도달하면 `take(4)`가 반환할 수 있는 최대 크기이므로 처리가 중지됩니다.

시퀀스 처리가 이렇게 진행됩니다:

![시퀀스 처리](sequence-processing.png)

이 예제에서 시퀀스 처리는 23단계가 아닌 18단계가 필요합니다.

---

# 이터레이터

> **원문:** https://kotlinlang.org/docs/iterators.html

컬렉션을 순회하기 위해 Kotlin 표준 라이브러리는 _이터레이터_의 일반적으로 사용되는 메커니즘을 지원합니다. 이터레이터는 기본 구조를 노출하지 않고 컬렉션 요소에 순차적으로 접근할 수 있게 해주는 객체입니다. 이터레이터는 모든 요소를 하나씩 처리해야 할 때 유용합니다. 예를 들어, 값을 출력하거나 유사한 업데이트를 수행하는 경우입니다.

이터레이터는 [`Iterator<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html) 인터페이스의 `iterator()` 함수를 호출하여 `Set`과 `List`를 포함한 [`Iterable<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html) 인터페이스의 상속자에서 얻을 수 있습니다.

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

---

# 범위와 진행

> **원문:** https://kotlinlang.org/docs/ranges.html

Kotlin에서는 `kotlin.ranges` 패키지의 [`.rangeTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-to.html)와 [`.rangeUntil()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-until.html) 함수를 사용하여 값의 범위를 쉽게 생성할 수 있습니다.

예를 들어, 다음 범위에는 `1`부터 `4`까지의 숫자가 포함됩니다.

대괄호를 사용하여 범위를 생성하려면 두 개의 점 `..`을 가진 [`.rangeTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-to.html) 함수를 연산자 형태로 호출합니다:

```kotlin
fun main() {
//sampleStart
    // 양쪽 끝이 포함되는 범위
    println(4 in 1..4)
    // true
//sampleEnd
}
```

열린 끝 범위를 생성하려면 `..<` 연산자를 사용하세요. 예를 들어 `1..<4`는 `1`, `2`, `3`을 나타냅니다.

```kotlin
fun main() {
//sampleStart
    // 열린 끝 범위
    println(4 in 1..<4)
    // false
//sampleEnd
}
```

반대로 역순으로 범위를 반복하려면 `..` 대신 [`downTo`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/down-to.html) 함수를 사용합니다.

```kotlin
fun main() {
//sampleStart
    for (i in 4 downTo 1) print(i)
    // 4321
//sampleEnd
}
```

1이 아닌 임의의 스텝으로 범위를 반복하는 것도 가능합니다. 이는 [`step`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/step.html) 함수로 수행합니다.

```kotlin
fun main() {
//sampleStart
    for (i in 0..8 step 2) print(i)
    println()
    // 02468

    for (i in 0..<8 step 2) print(i)
    println()
    // 0246

    for (i in 8 downTo 0 step 2) print(i)
    // 86420
//sampleEnd
}
```

## 진행(Progression)

`Int`, `Long`, `Char`와 같은 정수 타입의 범위는 [등차수열](https://en.wikipedia.org/wiki/Arithmetic_progression)로 취급될 수 있습니다. Kotlin에서 이러한 진행은 특별한 타입으로 정의됩니다: [`IntProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-int-progression/index.html), [`LongProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-long-progression/index.html), [`CharProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-char-progression/index.html).

진행에는 세 가지 필수 속성이 있습니다: `first` 요소, `last` 요소, 0이 아닌 `step`. 첫 번째 요소는 `first`이고, 후속 요소는 이전 요소에 `step`을 더한 값입니다. 양수 스텝을 가진 진행에서의 반복은 Java/JavaScript의 인덱스가 있는 `for` 루프와 동등합니다:

```java
for (int i = first; i <= last; i += step) {
  // ...
}
```

범위를 반복하여 암묵적으로 진행을 생성하면 이 진행의 `first`와 `last` 요소는 범위의 끝점이고 `step`은 1입니다.

```kotlin
fun main() {
//sampleStart
    for (i in 1..10) print(i)
    // 12345678910
//sampleEnd
}
```

사용자 정의 진행 스텝을 정의하려면 범위에서 `step` 함수를 사용합니다.

```kotlin
fun main() {
//sampleStart
    for (i in 1..8 step 2) print(i)
    // 1357
//sampleEnd
}
```

진행의 `last` 요소는 다음과 같이 계산됩니다:
* 양수 스텝의 경우: `(last - first) % step == 0`을 만족하는 끝 값보다 크지 않은 최대값.
* 음수 스텝의 경우: `(last - first) % step == 0`을 만족하는 끝 값보다 작지 않은 최소값.

따라서 `last` 요소가 항상 지정된 끝 값과 같지는 않습니다.

```kotlin
fun main() {
//sampleStart
    for (i in 1..9 step 3) print(i) // 마지막 요소는 7
    // 147
//sampleEnd
}
```

진행은 `Iterable<N>`을 구현합니다. 여기서 `N`은 각각 `Int`, `Long`, `Char`이므로 `map`, `filter` 등 다양한 [컬렉션 함수](collection-operations.md)에서 사용할 수 있습니다.

```kotlin
fun main() {
//sampleStart
    println((1..10).filter { it % 2 == 0 })
    // [2, 4, 6, 8, 10]
//sampleEnd
}
```
