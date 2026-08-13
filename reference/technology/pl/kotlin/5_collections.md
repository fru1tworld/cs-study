# 코틀린 컬렉션

## 컬렉션

## 컬렉션 개요

> 원문: https://kotlinlang.org/docs/collections-overview.html

- Kotlin 표준 라이브러리 → 컬렉션 관리를 위한 포괄적인 도구 제공
- 컬렉션: 해결하고자 하는 문제에 중요하고 일반적으로 함께 연산되는 가변 개수의 항목(0개 포함) 그룹
- 컬렉션은 대부분의 프로그래밍 언어에서 공통된 개념 → Java·Python 컬렉션에 익숙하면 소개 생략 가능
- 컬렉션은 보통 동일한 타입(및 하위 타입)의 여러 객체 포함 → 컬렉션 내 객체를 요소 또는 항목이라 함
  - 예: 학과의 모든 학생 → 평균 나이 계산에 사용 가능한 컬렉션

### Kotlin 관련 컬렉션 타입

- List: 인덱스(위치를 나타내는 정수)로 요소에 접근하는 순서가 있는 컬렉션
  - 요소는 여러 번 나타날 수 있음
  - 예: 전화번호 목록 - 순서가 중요하고 반복 가능
- Set: 고유한 요소들의 컬렉션
  - 수학적 집합 추상화를 반영 → 반복이 없는 객체 그룹
  - 일반적으로 순서는 중요하지 않음
  - 예: 복권 번호 - 고유하고 순서 무관
- Map(딕셔너리): 키-값 쌍의 집합
  - 키는 고유, 각 키는 정확히 하나의 값에 매핑
  - 값은 중복 가능
  - 객체 간 논리적 연결 저장에 유용 → 예: 직원 ID와 직책
- ArrayDeque: 양끝에서 요소를 추가·제거할 수 있는 양방향 큐
  - Kotlin에서 스택·큐 데이터 구조 역할을 모두 수행
  - 내부적으로 필요 시 자동 크기 조정되는 배열로 구현됨

> 배열은 컬렉션 타입이 아님 → 자세한 내용은 [배열](arrays.md) 참조

- Kotlin에서는 저장된 객체의 정확한 타입과 독립적으로 컬렉션 조작 가능
  - `String`을 `String` 리스트에 추가하는 것 = `Int`나 사용자 정의 클래스를 추가하는 것과 동일한 방식
  - 따라서 표준 라이브러리는 모든 타입 컬렉션을 위한 제네릭 인터페이스·클래스·함수 제공
- 컬렉션 인터페이스와 관련 함수는 `kotlin.collections` 패키지에 위치

### 컬렉션 타입

- Kotlin 표준 라이브러리 → 기본 컬렉션 타입(셋·리스트·맵) 구현 제공
- 각 컬렉션 타입을 나타내는 인터페이스 쌍:
  - 읽기 전용 인터페이스: 컬렉션 요소 접근 연산 제공
  - 가변 인터페이스: 읽기 전용 인터페이스를 요소 추가·제거·업데이트 등 쓰기 연산으로 확장

- 가변 컬렉션 변경 시 반드시 [`var`](basic-syntax.md#variables)에 할당해야 하는 것은 아님
  - 쓰기 연산은 동일한 가변 컬렉션 객체를 수정 → 참조는 변경되지 않음
  - `val` 컬렉션 재할당 시도 → 컴파일 오류

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

- 읽기 전용 컬렉션 타입은 [공변](generics.md#variance)
  - `Rectangle`이 `Shape`를 상속 → `List<Shape>`가 필요한 모든 곳에 `List<Rectangle>` 사용 가능
  - 컬렉션 타입은 요소 타입과 동일한 하위 타입 관계
  - 맵은 값 타입에 대해서만 공변, 키 타입에는 비공변
- 가변 컬렉션은 공변이 아님
  - 공변이면 런타임 실패로 이어질 수 있음
  - `MutableList<Rectangle>`이 `MutableList<Shape>`의 하위 타입이라면 → `Circle` 같은 다른 `Shape` 상속자를 삽입해 `Rectangle` 타입 인수 위반 가능

Kotlin 컬렉션 인터페이스 다이어그램:

![컬렉션 인터페이스 계층](collections-diagram.png)

- `Collection`, `List`, `Set`, `Map` 각각의 인터페이스와 구현을 아래에서 설명

#### Collection

- [`Collection<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html): 컬렉션 계층의 루트
  - 읽기 전용 컬렉션의 공통 동작 표현: 크기 조회, 항목 멤버십 확인 등
  - 요소 반복 연산을 정의하는 `Iterable<T>` 인터페이스 상속
  - 다른 컬렉션 타입에 적용되는 함수의 매개변수로 사용 가능
  - 더 구체적인 경우 → 상속자인 [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)·[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html) 사용

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

- [`MutableCollection<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html): `add`·`remove` 등 쓰기 연산이 있는 `Collection`

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

#### List

- [`List<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html): 지정된 순서로 요소를 저장하고 인덱스로 접근
  - 인덱스 범위: `0`(첫 요소)부터 `list.size - 1`인 `lastIndex`까지

```kotlin
fun main() {
    val numbers = listOf("one", "two", "three", "four")
    println("요소 수: ${numbers.size}")
    println("세 번째 요소: ${numbers.get(2)}")
    println("네 번째 요소: ${numbers[3]}")
    println("요소 \"two\"의 인덱스: ${numbers.indexOf("two")}")
}
```

- 리스트 요소(null 포함)는 중복 가능 → 동일 객체 또는 단일 객체의 발생을 원하는 수만큼 포함 가능
- 두 리스트가 같은 크기 + 같은 위치에 [구조적으로 동일한](equality.md#structural-equality) 요소 → 동일한 것으로 간주

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

- [`MutableList<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html): 리스트 전용 쓰기 연산이 있는 `List`
  - 예: 특정 위치에 요소 추가·제거

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.add(5)
    numbers.removeAt(1)
    numbers[0] = 0
    numbers.shuffle()
    println(numbers)
//sampleEnd
}
```

- 리스트는 배열과 유사하지만 차이점 존재
  - 배열: 크기가 초기화 시 정의되고 절대 변경되지 않음
  - 리스트: 미리 정의된 크기 없음 → 요소 추가·업데이트·제거 등 쓰기 연산 결과로 크기 변경 가능
- Kotlin에서 `List`의 기본 구현: [`ArrayList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-list/index.html) - 크기 조절 가능한 배열로 볼 수 있음

#### Set

- [`Set<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html): 고유한 요소를 저장, 순서는 일반적으로 정의되지 않음
  - `null` 요소도 고유 → `Set`은 하나의 `null`만 포함 가능
  - 두 셋이 같은 크기 + 한 셋의 각 요소에 대해 다른 셋에 동일한 요소가 있으면 두 셋은 동일

```kotlin
fun main() {
    val numbers = setOf(1, 2, 3, 4)
    println("요소 수: ${numbers.size}")
    if (numbers.contains(1)) println("1은 셋에 있습니다")

    val numbersBackwards = setOf(4, 3, 2, 1)
    println("셋이 동일합니다: ${numbers == numbersBackwards}")
}
```

- [`MutableSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html): `MutableCollection`의 쓰기 연산이 있는 `Set`
- `MutableSet`의 기본 구현 [`LinkedHashSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-linked-hash-set/index.html): 요소 삽입 순서 유지
  - `first()`·`last()` 등 순서 의존 함수 → 예측 가능한 결과 반환

```kotlin
fun main() {
    val numbers = setOf(1, 2, 3, 4)  // LinkedHashSet이 기본 구현
    val numbersBackwards = setOf(4, 3, 2, 1)

    println(numbers.first() == numbersBackwards.first())
    println(numbers.first() == numbersBackwards.last())
}
```

- 대체 구현 [`HashSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-set/index.html): 요소 순서 보장 안 함 → 호출 시 예측 불가한 결과
  - 대신 동일 요소 수 저장 시 메모리 사용량이 더 적음

#### Map

- [`Map<K, V>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html): `Collection` 인터페이스의 상속자는 아니지만 Kotlin 컬렉션 타입
  - 키-값 쌍(엔트리) 저장 → 키는 고유하지만 서로 다른 키가 동일한 값에 쌍을 이룰 수 있음
  - 키로 값 접근, 키·값 검색 등 특정 함수 제공

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

- 동일한 쌍을 포함하는 두 맵은 쌍 순서와 관계없이 동일

```kotlin
fun main() {
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)
    val anotherMap = mapOf("key2" to 2, "key1" to 1, "key4" to 1, "key3" to 3)

    println("맵이 동일합니다: ${numbersMap == anotherMap}")
}
```

- [`MutableMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html): 맵 쓰기 연산이 있는 `Map`
  - 예: 새 키-값 쌍 추가, 주어진 키의 값 업데이트

```kotlin
fun main() {
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap.put("three", 3)
    numbersMap["one"] = 11

    println(numbersMap)
}
```

- `MutableMap`의 기본 구현 [`LinkedHashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-linked-hash-map/index.html): 반복 시 요소 삽입 순서 유지
- 대체 구현 [`HashMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html): 요소 순서 보장 안 함

#### ArrayDeque

- [`ArrayDeque<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/): 양방향 큐 구현
  - 큐의 시작 또는 끝에서 요소 추가·제거 가능
  - Kotlin에서 스택·큐 데이터 구조 역할을 모두 수행
  - 내부적으로 필요 시 자동 크기 조정되는 배열로 구현됨

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

## 컬렉션 생성하기

> 원문: https://kotlinlang.org/docs/constructing-collections.html

### 요소로부터 생성

- 컬렉션 생성의 가장 일반적인 방법: 표준 라이브러리 함수 [`listOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/list-of.html)·[`setOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/set-of.html)·[`mutableListOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-list-of.html)·[`mutableSetOf<T>()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-set-of.html)
  - 요소를 쉼표로 구분된 인수로 제공 → 컴파일러가 요소 타입 자동 감지
  - 빈 컬렉션 생성 시 타입 명시 필요

```kotlin
val numbersSet = setOf("one", "two", "three", "four")
val emptySet = mutableSetOf<String>()
```

- 맵 생성 함수 [`mapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-of.html)·[`mutableMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-map-of.html)도 동일하게 적용
  - 맵의 키·값은 `Pair` 객체로 전달(보통 `to` 중위 함수로 생성)

```kotlin
val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)
```

- `to` 표기법 → 수명이 짧은 `Pair` 객체 생성 → 성능이 중요하지 않은 경우에만 사용 권장
  - 과도한 메모리 사용 방지 → 가변 맵 생성 후 쓰기 연산으로 채우는 방식 대안
  - [`apply()`](scope-functions.md#apply) 함수가 이러한 초기화를 유연하게 지원

```kotlin
val numbersMap = mutableMapOf<String, String>().apply { this["one"] = "1"; this["two"] = "2" }
```

### 컬렉션 빌더 함수로 생성

- 빌더 함수 [`buildList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-list.html)·[`buildSet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-set.html)·[`buildMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/build-map.html) 호출
  - 해당 타입의 새 가변 컬렉션 생성 → [람다 함수](lambdas.md#function-literals-with-receiver)로 채움 → 동일 요소를 가진 읽기 전용 컬렉션 반환

```kotlin
val map = buildMap { // 이것은 MutableMap<String, Int>, 아래 코드에서 타입이 추론됨
    put("a", 1)
    put("b", 0)
    put("c", 4)
}

println(map) // {a=1, b=0, c=4}
```

### 빈 컬렉션

- 빈 컬렉션 생성 함수: [`emptyList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-list.html)·[`emptySet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-set.html)·[`emptyMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/empty-map.html)
  - 빈 컬렉션 생성 시 요소 타입 지정 필요

```kotlin
val empty = emptyList<String>()
```

### 리스트용 초기화 함수

- 리스트 크기·인덱스 기반 요소 값을 정의하는 초기화 함수를 받는 유사 생성자 함수 존재

```kotlin
fun main() {
//sampleStart
    val doubled = List(3) { it * 2 }  // 또는 MutableList를 원하면 MutableList
    println(doubled)
//sampleEnd
}
```

### 구체적인 타입 생성자

- `ArrayList`·`LinkedList` 등 구체적인 타입의 컬렉션 생성 시 해당 타입 생성자 사용 가능
  - `Set`·`Map` 구현에도 유사한 생성자 존재

```kotlin
val linkedList = LinkedList<String>(listOf("one", "two", "three"))
val presizedSet = HashSet<Int>(32)
```

### 복사

- 기존 컬렉션과 동일한 요소를 가진 컬렉션 생성 → 복사 함수 사용
  - 표준 라이브러리 컬렉션 복사 함수 → 동일한 요소에 대한 참조를 가진 얕은 복사 컬렉션 생성
  - 컬렉션 요소에 대한 변경은 모든 복사본에 반영됨

- [`toList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-list.html)·[`toMutableList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-mutable-list.html)·[`toSet()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-set.html) 등 컬렉션 복사 함수 → 특정 시점의 컬렉션 스냅샷 생성
  - 결과는 동일한 요소를 가진 새 컬렉션
  - 원본 컬렉션에서 요소 추가·제거 → 복사본에 영향 없음
  - 복사본도 원본과 독립적으로 변경 가능

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

- 이러한 함수는 컬렉션을 다른 타입으로 변환하는 데도 사용 가능 → 예: 리스트에서 셋 생성 또는 반대

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

- 또는 동일한 컬렉션 인스턴스에 대한 새로운 참조 생성 가능
  - 기존 컬렉션으로 컬렉션 변수 초기화 → 새로운 참조 생성
  - 참조를 통해 컬렉션 인스턴스가 변경되면 모든 참조에 변경 사항 반영

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

- 컬렉션 초기화는 가변성 제한에 사용 가능 → 예: `MutableList`에 대한 `List` 참조 생성 시 이 참조를 통한 수정 시도 → 컴파일러 오류

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

### 다른 컬렉션에서 함수 호출

- 컬렉션은 다른 컬렉션에 대한 다양한 연산 결과로 생성 가능
- [필터링](collection-filtering.md): 필터와 일치하는 요소의 리스트 생성

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val longerThan3 = numbers.filter { it.length > 3 }
    println(longerThan3)
//sampleEnd
}
```

- [매핑](collection-transformations.md#map): 변환 결과의 리스트 생성

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.map { it * 3 })
    println(numbers.mapIndexed { idx, value -> value * idx })
//sampleEnd
}
```

- [연관](collection-transformations.md#associate): 맵 생성

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.associateWith { it.length })
//sampleEnd
}
```

- Kotlin 컬렉션 연산 관련 자세한 내용 → [컬렉션 연산 개요](collection-operations.md) 참조

---

## 컬렉션 연산 개요

> 원문: https://kotlinlang.org/docs/collection-operations.html

- Kotlin 표준 라이브러리 → 컬렉션 연산을 위한 다양한 함수 제공
  - 요소 조회·추가 같은 간단한 연산
  - 검색·정렬·필터링·변환 등 더 복잡한 연산

### 확장 및 멤버 함수

- 표준 라이브러리의 컬렉션 연산은 두 가지 방식으로 선언
  - 컬렉션 인터페이스의 [멤버 함수](classes.md#class-members)
  - [확장 함수](extensions.md#extension-functions)

- 멤버 함수: 컬렉션 타입에 필수적인 연산 정의
  - 예: [`Collection`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/index.html)의 [`isEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/is-empty.html)
  - 예: [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)의 인덱스 접근용 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/get.html)

- 컬렉션 인터페이스의 자체 구현 작성 시 멤버 함수 구현 필요
  - 표준 라이브러리의 스켈레톤 구현 활용 가능: [`AbstractCollection`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-collection/index.html)·[`AbstractList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-list/index.html)·[`AbstractSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-set/index.html)·[`AbstractMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-abstract-map/index.html) 및 가변 버전

- 필터링·변환·순서화 등 다른 연산은 확장 함수로 선언

### 공통 연산

- 공통 연산은 [읽기 전용 및 가변 컬렉션](collections-overview.md#collection-types) 모두에서 사용 가능
- 다음 그룹에 속함:
  - [변환](collection-transformations.md)
  - [필터링](collection-filtering.md)
  - [`plus` 및 `minus` 연산자](collection-plus-minus.md)
  - [그룹화](collection-grouping.md)
  - [컬렉션 일부 조회](collection-parts.md)
  - [단일 요소 조회](collection-elements.md)
  - [정렬](collection-ordering.md)
  - [집계 연산](collection-aggregate.md)

- 이 페이지에서 설명하는 연산은 원래 컬렉션에 영향을 주지 않고 결과 반환
  - 예: 필터링 연산 → 조건과 일치하는 모든 요소를 포함하는 새 컬렉션 생성
  - 결과는 변수에 저장하거나 다른 방식으로 사용 필요(예: 다른 함수에 전달)

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

- 특정 컬렉션 연산에서는 대상 객체를 지정하는 옵션 존재
  - 대상: 함수가 새 객체 대신 결과 항목을 추가하는 가변 컬렉션
  - 대상이 있는 연산 수행 → 이름에 `To` 접미사가 있는 별도 함수 사용
    - 예: [`filter()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html) 대신 [`filterTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-to.html)
    - 예: [`associate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate.html) 대신 [`associateTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-to.html)
  - 이러한 함수는 대상 컬렉션을 추가 매개변수로 받음

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

- 편의를 위해 이러한 함수는 대상 컬렉션을 다시 반환 → 함수 호출의 해당 인수에서 직접 생성 가능

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

- 대상이 있는 함수는 필터링·연관·그룹화·평탄화 등에 사용 가능
  - 전체 목록은 [Kotlin 컬렉션 참조](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index.html) 참조

### 쓰기 연산

- 가변 컬렉션의 경우 컬렉션 상태를 변경하는 쓰기 연산도 존재
  - 요소 추가·제거·업데이트 포함
  - 관련 섹션: [컬렉션 쓰기 연산](collection-write.md)·[리스트별 연산](list-operations.md#list-write-operations)·[맵별 연산](map-operations.md#map-write-operations)

- 특정 연산의 경우 동일한 연산을 수행하는 함수 쌍 존재
  - 하나는 제자리 적용, 다른 하나는 결과를 별도 컬렉션으로 반환
  - 예: [`sort()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort.html) - 가변 컬렉션 제자리 정렬(상태 변경) / [`sorted()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted.html) - 정렬된 새 컬렉션 생성

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

## 단일 요소 조회

> 원문: https://kotlinlang.org/docs/collection-elements.html

- Kotlin 컬렉션 → 단일 요소 조회를 위한 함수 세트 제공
  - 이 페이지의 함수는 리스트·셋 모두에 적용

- [리스트의 정의](collections-overview.md#list)상 리스트는 순서가 있는 컬렉션 → 모든 요소는 참조할 위치를 가짐
  - 인덱스로 요소 조회·검색하는 더 넓은 방법은 [리스트별 연산](list-operations.md) 참조

- [정의](collections-overview.md#set)상 셋은 순서가 있는 컬렉션이 아님
  - Kotlin의 `Set`은 요소를 특정 순서로 저장(구현 타입에 따라: 순서 유지 `LinkedHashSet` / 순서 없는 `HashSet`)
  - 순서를 몰라도 위치로 요소를 조회하는 것이 유용할 수 있음(어떤 요소가 반환될지 알 수 없을 뿐) → 랜덤 요소가 필요할 때 유용

### 위치로 조회

- 정확한 위치에서 요소 조회: [`elementAt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at.html) 함수
  - `0`부터 `컬렉션 크기 - 1`까지의 정수 인수로 호출 → 해당 위치의 요소 반환
  - 첫 요소는 위치 `0`, 마지막 요소는 위치 `(size - 1)`

- `elementAt()`은 인덱스 접근이나 `get()`을 지원하지 않는 컬렉션에 유용
  - `List`의 경우 [인덱스 접근 연산자](list-operations.md#retrieve-elements-by-index)(`get()` 또는 `[]`)가 더 관용적

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

- 첫 번째·마지막 요소 조회용 별칭: [`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)·[`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html)

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.first())
    println(numbers.last())
//sampleEnd
}
```

- 존재하지 않는 위치 조회 시 예외를 피하려면 `elementAt()`의 안전한 변형 사용:
  - [`elementAtOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-null.html): 위치가 범위를 벗어나면 null 반환
  - [`elementAtOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at-or-else.html): `Int` 인수에 매핑되는 람다를 추가로 받음 → 범위를 벗어난 위치로 호출 시 람다 결과 반환

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.elementAtOrNull(5))
    println(numbers.elementAtOrElse(5) { index -> "인덱스 $index의 값이 정의되지 않음"})
//sampleEnd
}
```

### 조건으로 조회

- [`first()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first.html)·[`last()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html) 함수는 술어와 일치하는 요소 검색에도 사용 가능
  - `first()` + 술어 → 람다가 `true`를 반환하는 첫 번째 요소
  - `last()` + 술어 → 일치하는 마지막 요소

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.first { it.length > 3 })
    println(numbers.last { it.startsWith("f") })
//sampleEnd
}
```

- 일치하는 요소가 없으면 두 함수 모두 예외 throw
  - 대신 [`firstOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-or-null.html)·[`lastOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last-or-null.html) 사용 → 일치 요소 없으면 `null` 반환

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.firstOrNull { it.length > 6 })
//sampleEnd
}
```

- 상황에 더 적합한 이름의 별칭:
  - `firstOrNull()` 대신 [`find()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find.html)
  - `lastOrNull()` 대신 [`findLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/find-last.html)

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.find { it % 2 == 0 })
    println(numbers.findLast { it % 2 == 0 })
//sampleEnd
}
```

### 선택자로 조회

- 컬렉션을 매핑한 다음 매핑된 컬렉션의 최소 요소에 해당하는 원래 요소 조회가 필요한 경우 → [`firstNotNullOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/first-not-null-of.html) 함수 사용

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

### 랜덤 요소

- 컬렉션의 임의 요소 조회 → [`random()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random.html) 함수 호출
  - 인수 없이 호출 또는 [`Random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/index.html) 객체를 무작위 소스로 사용 가능

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4)
    println(numbers.random())
//sampleEnd
}
```

- 빈 컬렉션에서 `random()`은 예외 throw → 대신 `null`을 받으려면 [`randomOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/random-or-null.html) 사용

### 존재 확인

- 컬렉션에 요소가 있는지 확인 → [`contains()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains.html) 함수 사용
  - 인수와 `equals()`인 요소가 있으면 `true` 반환
  - `in` 키워드로 연산자 형태 호출 가능
- 여러 인스턴스 존재를 한 번에 확인 → [`containsAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/contains-all.html)에 인스턴스 컬렉션을 인수로 전달

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

- [`isEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-empty.html)·[`isNotEmpty()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-not-empty.html)로도 요소 존재 여부 확인 가능

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

## 정렬

> 원문: https://kotlinlang.org/docs/collection-ordering.html

- 요소의 순서는 특정 컬렉션 타입에서 중요한 측면 → 동일 요소를 가진 두 리스트도 순서가 다르면 같지 않음
- Kotlin에서 객체의 순서는 여러 방식으로 정의 가능

- 대부분의 내장 타입에는 자연 순서 존재:
  - 숫자 타입: 전통적인 숫자 순서(`1` > `0`, `-3.4f` > `-5f`)
  - `Char`·`String`: [사전순](https://en.wikipedia.org/wiki/Lexicographic_order)(`b` > `a`, `world` > `hello`)

- 사용자 정의 타입에 자연 순서 정의 → 해당 타입을 [`Comparable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparable/index.html) 상속자로 만들고 `compareTo()` 함수 구현
  - `compareTo()`는 동일 타입의 다른 객체를 인수로 받아 어떤 객체가 더 큰지를 나타내는 정수 값 반환
    - 양수: 수신자 객체가 더 큼
    - 음수: 인수보다 작음
    - 0: 객체가 같음

- 다음은 주 버전·부 버전으로 구성된 버전을 정렬하는 데 사용할 수 있는 클래스

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

- 사용자 정의 순서 → 원하는 방식으로 모든 타입 인스턴스 정렬 가능
  - 비교할 수 없는 객체에 대한 순서 정의, `Comparable`이 아닌 타입에 대한 순서 정의 등에 특히 유용
  - 타입에 대한 사용자 정의 순서 정의 → 해당 타입의 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html) 생성
  - `Comparator`에는 두 인스턴스를 비교하는 `compare()` 함수 포함 → 비교 결과를 나타내는 정수 반환

```kotlin
fun main() {
//sampleStart
    val lengthComparator = Comparator { str1: String, str2: String -> str1.length - str2.length }
    println(listOf("aaa", "bb", "c").sortedWith(lengthComparator))
//sampleEnd
}
```

- `lengthComparator` 사용 → 기본 사전순 대신 길이로 문자열 비교

- `Comparator`를 정의하는 더 짧은 방법: 표준 라이브러리의 [`compareBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.comparisons/compare-by.html) 함수
  - 인스턴스에서 `Comparable` 값을 생성하는 람다를 받아 생성된 값의 자연 순서로 사용자 정의 순서 정의

- `compareBy()`를 사용하면 위 길이 비교기는 다음과 같이 됨:

```kotlin
fun main() {
//sampleStart
    println(listOf("aaa", "bb", "c").sortedWith(compareBy { it.length }))
//sampleEnd
}
```

- Kotlin 컬렉션 패키지는 자연 순서·사용자 정의 순서·랜덤 순서로 컬렉션을 정렬하는 함수 제공
  - 이 페이지는 [읽기 전용](collections-overview.md#collection-types) 컬렉션에 적용되는 정렬 함수 설명 → 요청된 순서의 요소를 포함하는 새 컬렉션으로 결과 반환
  - [가변](collections-overview.md#collection-types) 컬렉션을 제자리에서 정렬하는 함수 → [리스트별 연산](list-operations.md#sort) 참조

### 자연 순서

- 기본 함수 [`sorted()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted.html)·[`sortedDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-descending.html): `Comparable` 요소가 있는 컬렉션을 자연 순서로 오름차순·내림차순 시퀀스 반환
  - 모든 `Comparable` 요소 컬렉션에 적용 가능

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println("오름차순 정렬: ${numbers.sorted()}")
    println("내림차순 정렬: ${numbers.sortedDescending()}")
//sampleEnd
}
```

### 사용자 정의 순서

- 사용자 정의 순서로 정렬하거나 비교할 수 없는 객체를 정렬 → [`sortedBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-by.html)·[`sortedByDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-by-descending.html) 함수
  - 요소를 `Comparable` 값에 매핑하는 선택자 함수를 받아 해당 값의 자연 순서로 정렬

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

- 컬렉션 정렬에 사용자 정의 순서를 정의하려면 자체 `Comparator` 제공 가능
  - `Comparator`를 전달하며 [`sortedWith()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sorted-with.html) 함수 호출
  - 예: 문자열을 길이로 정렬

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println("길이로 정렬: ${numbers.sortedWith(compareBy { it.length })}")
//sampleEnd
}
```

### 역순

- 역순으로 컬렉션 조회 → [`reversed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reversed.html) 함수 사용

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.reversed())
//sampleEnd
}
```

- `reversed()`는 요소가 복사된 새 컬렉션 반환 → 나중에 원래 컬렉션을 변경해도 이전에 얻은 `reversed()` 결과에 영향 없음

- 또 다른 역순 함수 [`asReversed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/as-reversed.html): 동일한 컬렉션 인스턴스의 역순 뷰 반환
  - 원래 리스트가 변경되지 않을 경우 `reversed()`보다 가볍고 선호됨

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    val reversedNumbers = numbers.asReversed()
    println(reversedNumbers)
//sampleEnd
}
```

- 원래 리스트가 가변인 경우 모든 변경 사항이 역순 뷰에 영향을 미치고 그 반대도 마찬가지

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

- 리스트의 가변성을 모르거나 소스가 리스트가 아닌 경우 `reversed()`가 더 선호됨 → 결과가 미래에 변경되지 않을 복사본이기 때문

### 랜덤 순서

- [`shuffled()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/shuffled.html) 함수: 요소를 랜덤 순서로 포함하는 새 `List` 반환
  - 인수 없이 호출 또는 [`Random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/index.html) 객체와 함께 호출 가능

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.shuffled())
//sampleEnd
}
```

---

## 집계 연산

> 원문: https://kotlinlang.org/docs/collection-aggregate.html

- Kotlin 컬렉션 → 자주 사용되는 집계 연산(컬렉션 내용 기반 단일 값 반환) 함수 포함
  - [`minOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-or-null.html)·[`maxOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-or-null.html): 가장 작은·가장 큰 요소 반환, 빈 컬렉션에서는 `null` 반환
  - [`average()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/average.html): 숫자 컬렉션 요소의 평균 값 반환
  - [`sum()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sum.html): 숫자 컬렉션 요소의 합계 반환
  - [`count()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/count.html): 컬렉션 요소 수 반환

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

- 특정 선택자 함수·사용자 정의 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html)로 최소·최대 요소 조회 함수:
  - [`maxByOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-by-or-null.html)·[`minByOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-by-or-null.html): 선택자 함수를 받아 최댓값·최솟값 반환 값을 가진 요소 반환
  - [`maxWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-with-or-null.html)·[`minWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-with-or-null.html): `Comparator` 객체를 받아 해당 기준으로 최대·최소 요소 반환
  - [`maxOfOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-of-or-null.html)·[`minOfOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-of-or-null.html): 선택자 함수를 받아 최대·최소 반환 값 자체를 반환
  - [`maxOfWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/max-of-with-or-null.html)·[`minOfWithOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/min-of-with-or-null.html): `Comparator` 객체를 받아 해당 기준의 최대·최소 반환 값 반환

- 이러한 함수는 빈 컬렉션에서 `null` 반환
  - 대안 `maxOf`·`minOf`·`maxOfWith`·`minOfWith`: 동일하게 작동하지만 빈 컬렉션에서 `NoSuchElementException` throw

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

- 일반적인 `sum()` 외에 고급 합산 함수 [`sumOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sum-of.html): 선택자 함수를 받아 모든 요소에 대한 반환 값의 합계 반환
  - 선택자는 `Int`·`Long`·`Double`·`UInt`·`ULong` 등 다양한 숫자 타입 반환 가능(JVM에서는 `BigInteger`·`BigDecimal`도 가능)

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 42, 10, 4)
    println(numbers.sumOf { it * 2 })
    println(numbers.sumOf { it.toDouble() / 2 })
//sampleEnd
}
```

### Fold와 reduce

- 더 구체적인 경우를 위한 [`reduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce.html)·[`fold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold.html) 함수: 제공된 연산을 요소에 순차적으로 적용 → 누적된 결과 반환
  - 연산은 두 인수를 받음: 이전에 누적된 값과 컬렉션 요소

- 두 함수 차이점: `fold()`는 초기 값을 받아 첫 단계에서 누적기로 사용 / `reduce()`의 첫 단계는 첫 번째·두 번째 요소를 첫 단계 연산 인수로 사용

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

- 위 예제는 차이점을 보여줌 → `fold()`는 두 배된 요소의 합계 계산에 사용
  - 동일 함수를 `reduce()`에 전달 → 다른 결과 반환
  - 첫 단계에서 리스트의 첫 번째·두 번째 요소가 인수로 사용됨 → 첫 요소가 두 배되지 않음

- 역순으로 함수를 적용 → [`reduceRight()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right.html)·[`foldRight()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-right.html) 함수 사용
  - `fold()`·`reduce()`와 유사하게 작동하지만 마지막 요소부터 시작해 이전 요소로 계속
  - 오른쪽으로 fold·reduce 시 연산 인수 순서 변경: 요소가 먼저, 누적된 값이 다음

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(5, 2, 10, 4)
    val sumDoubledRight = numbers.foldRight(0) { element, sum -> sum + element * 2 }
    println(sumDoubledRight)
//sampleEnd
}
```

- 요소 인덱스를 매개변수로 사용하는 연산 → [`reduceIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-indexed.html)·[`foldIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-indexed.html) 함수 사용
  - 연산의 첫 인수로 요소 인덱스 전달

- 요소에 이러한 연산을 오른쪽에서 왼쪽으로 적용: [`reduceRightIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-indexed.html)·[`foldRightIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold-right-indexed.html)

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

- 모든 reduce 연산은 빈 컬렉션에서 예외 throw → 대신 `null`을 받으려면 `*OrNull()` 대응물 사용:
  - [`reduceOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-or-null.html)
  - [`reduceRightOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-or-null.html)
  - [`reduceIndexedOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-indexed-or-null.html)
  - [`reduceRightIndexedOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce-right-indexed-or-null.html)

- 중간 누적기 값을 나중에 재사용하려면 [`runningFold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-fold.html)(동의어 [`scan()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/scan.html))·[`runningReduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-reduce.html) 함수 사용

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

- 연산 매개변수에 요소 인덱스가 필요한 경우 [`runningFoldIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-fold-indexed.html) 또는 [`runningReduceIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-reduce-indexed.html) 사용

---

## 컬렉션 일부 조회

> 원문: https://kotlinlang.org/docs/collection-parts.html

- Kotlin 표준 라이브러리 → 컬렉션 일부 조회를 위한 확장 함수 포함
  - 결과 컬렉션에 포함할 요소 선택 방법: 위치 명시적 나열, 결과 크기 지정, 기타 방법

### 슬라이스

- [`slice()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/slice.html): 주어진 인덱스에 있는 요소의 리스트 반환
  - 인덱스는 [범위](ranges.md)나 정수 값 컬렉션으로 전달 가능

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

### Take와 drop

- 처음부터 지정된 수의 요소를 가져오려면 [`take()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take.html), 마지막 요소는 [`takeLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-last.html)
  - 컬렉션 크기보다 큰 숫자로 호출 시 두 함수 모두 전체 컬렉션 반환

- 처음·마지막부터 주어진 수의 요소를 제외한 나머지를 가져오려면 [`drop()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop.html)·[`dropLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-last.html) 함수 호출

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

- 술어로 take·drop할 요소 수를 정의하는 네 가지 변형:
  - [`takeWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-while.html): 술어와 일치하는 요소를 일치하지 않는 첫 요소까지 가져오는 `take()` - 첫 요소가 일치하지 않으면 결과는 비어 있음
  - [`takeLastWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/take-last-while.html): `takeLast()`와 유사 - 컬렉션 끝에서 술어와 일치하는 요소 범위를 가져옴, 범위의 첫 요소는 일치하지 않는 마지막 요소 바로 다음 요소, 마지막 요소가 일치하지 않으면 결과는 비어 있음
  - [`dropWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-while.html): 같은 술어를 가진 `takeWhile()`의 반대 - 일치하지 않는 첫 요소부터 끝까지 요소 반환
  - [`dropLastWhile()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/drop-last-while.html): 같은 술어를 가진 `takeLastWhile()`의 반대 - 처음부터 일치하지 않는 마지막 요소까지 요소 반환

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

### 청크

- 컬렉션을 주어진 크기의 부분으로 나누려면 [`chunked()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/chunked.html) 함수 사용
  - 청크 크기를 단일 인수로 받아 주어진 크기의 `List`들의 `List` 반환
  - 첫 청크는 첫 요소부터 `size` 요소, 두 번째 청크는 다음 `size` 요소... 순
  - 마지막 청크는 크기가 더 작을 수 있음

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.chunked(3))
//sampleEnd
}
```

- 반환된 청크에 변환을 즉시 적용 가능 → `chunked()` 호출 시 변환을 람다 함수로 제공
  - 람다 인수는 컬렉션의 청크
  - 변환과 함께 호출되면 청크는 해당 람다에 즉시 전달되어야 하는 수명이 짧은 `List` → 해당 청크에서 다른 `List`가 필요하면 별도 `List`로 복사

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.chunked(3) { it.sum() })  // `it`은 원래 컬렉션의 청크
//sampleEnd
}
```

### 윈도우

- 주어진 크기의 컬렉션 요소 범위 모두를 조회 가능 → [`windowed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/windowed.html) 함수
  - 주어진 크기의 슬라이딩 윈도우로 컬렉션을 볼 때 보이는 요소 범위의 리스트 반환
  - `chunked()`와 달리 각 요소에서 시작하는 요소 범위(윈도우) 반환 → 모든 윈도우는 단일 `List`의 요소로 반환

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")
    println(numbers.windowed(3))
//sampleEnd
}
```

- `windowed()`의 선택적 매개변수:
  - `step`: 두 인접 윈도우의 첫 요소 사이 거리 정의(기본값 1 → 모든 요소에서 시작하는 윈도우 포함, 2로 늘리면 홀수 요소에서 시작하는 윈도우만)
  - `partialWindows`: 컬렉션 끝에서 더 작은 크기의 윈도우 포함(예: 3요소 윈도우 요청 시 마지막 두 요소는 빌드 불가 → 활성화 시 크기 2·1인 리스트 추가 포함)

- 반환된 범위에 변환을 즉시 적용 가능 → `windowed()` 호출 시 변환을 람다 함수로 제공

```kotlin
fun main() {
//sampleStart
    val numbers = (1..10).toList()
    println(numbers.windowed(3, step = 2, partialWindows = true))
    println(numbers.windowed(3) { it.sum() })
//sampleEnd
}
```

- 두 요소 윈도우 빌드용 별도 함수 [`zipWithNext()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/zip-with-next.html): 수신자 컬렉션의 인접 요소 쌍 생성
  - 컬렉션을 조각으로 나누지 않음 → 마지막 요소를 제외한 각 요소에 대해 `Pair` 생성
    - `[1, 2, 3, 4]` → `[[1, 2], [2, 3], [3, 4]]`(`[[1, 2], [3, 4]]`가 아님)
  - 변환 함수와 함께 호출 가능(수신자 컬렉션의 두 요소를 인수로 받음)

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

## 플러스 및 마이너스 연산자

> 원문: https://kotlinlang.org/docs/collection-plus-minus.html

- Kotlin에서 [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)와 [`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자는 컬렉션에 정의됨
  - 첫 번째 피연산자는 컬렉션, 두 번째는 요소이거나 다른 컬렉션 가능
  - 반환 값은 새로운 읽기 전용 컬렉션:
    - `plus`의 결과: 원래 컬렉션의 요소 및 두 번째 피연산자를 포함
    - `minus`의 결과: 원래 컬렉션의 요소에서 두 번째 피연산자의 요소를 제외
      - 두 번째 피연산자가 요소인 경우: 해당 요소의 첫 번째 발생만 제거
      - 컬렉션인 경우: 해당 요소의 모든 발생 제거

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

- 맵에서 `plus`·`minus`의 세부 사항 → [맵별 연산](map-operations.md) 참조
- [증가 대입 연산자](operator-overloading.md#augmented-assignments) [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)·[`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`)도 컬렉션에 정의됨
  - 읽기 전용 컬렉션의 경우 실제로는 `plus`·`minus` 연산자 사용 후 결과를 동일 변수에 재할당하는 것 → `var` 읽기 전용 컬렉션에서만 사용 가능
  - 가변 컬렉션의 경우 `val`로 선언되어도 제자리 수정
  - 자세한 내용은 [컬렉션 쓰기 연산](collection-write.md) 참조

---

## 그룹화

> 원문: https://kotlinlang.org/docs/collection-grouping.html

- Kotlin 표준 라이브러리 → 요소 그룹화를 위한 확장 함수 제공
- 기본 함수 [`groupBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/group-by.html): 람다 함수를 받아 `Map` 반환
  - 각 키는 람다 결과, 해당 값은 이 결과가 반환되는 요소의 `List`
  - 예: `String` 리스트를 첫 글자로 그룹화

- 두 번째 람다 인수(값 변환 함수)와 함께 `groupBy()` 호출 가능
  - `keySelector` 함수가 생성한 키는 원래 요소 대신 값 변환 함수의 결과에 매핑

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five")

    println(numbers.groupBy { it.first().uppercase() })
    println(numbers.groupBy(keySelector = { it.first() }, valueTransform = { it.uppercase() }))
//sampleEnd
}
```

- 요소를 그룹화한 다음 모든 그룹에 한 번에 연산을 적용 → [`groupingBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/grouping-by.html) 함수 사용
  - [`Grouping`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-grouping/index.html) 타입 인스턴스 반환
  - 지연 방식으로 모든 그룹에 연산 적용 가능 → 그룹은 연산 실행 직전에 실제로 빌드됨

- `Grouping`이 지원하는 연산:
  - [`eachCount()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/each-count.html): 각 그룹의 요소 수 계산
  - [`fold()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold.html)·[`reduce()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reduce.html): 각 그룹에 대해 별도 컬렉션으로 [fold 및 reduce](collection-aggregate.md#fold-and-reduce) 연산 수행, 결과 반환
  - [`aggregate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/aggregate.html): 각 그룹의 모든 요소에 주어진 연산을 순차적으로 적용, 결과 반환 - `Grouping`에 대한 일반적인 연산 수행 방법, fold나 reduce가 부족할 때 사용자 정의 연산 구현에 사용

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four", "five", "six")
    println(numbers.groupingBy { it.first() }.eachCount())
//sampleEnd
}
```

---

## 컬렉션 쓰기 연산

> 원문: https://kotlinlang.org/docs/collection-write.html

- [가변 컬렉션](collections-overview.md#collection-types) → 컬렉션 내용을 변경하는 연산 지원(요소 추가·제거 등)
  - 이 페이지는 `MutableCollection`의 모든 구현에서 사용 가능한 쓰기 연산 설명
  - `List`·`Map`에 사용 가능한 더 구체적인 연산은 각각 [리스트별 연산](list-operations.md)·[맵별 연산](map-operations.md)에서 설명

### 요소 추가

- 리스트·셋에 단일 요소를 추가 → [`add()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/add.html) 함수 사용
  - 지정된 객체가 컬렉션 끝에 추가됨

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.add(5)
    println(numbers)
//sampleEnd
}
```

- [`addAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/add-all.html): 인수 객체의 모든 요소를 리스트·셋에 추가
  - 인수는 `Iterable`·`Sequence`·`Array` 가능, 수신자와 인수 타입은 다를 수 있음(예: `Set`의 모든 항목을 `List`에 추가)

- 리스트에서 호출 시 `addAll()`은 인수 컬렉션과 동일한 순서로 새 요소 추가
  - 첫 인수로 요소 위치를 사용해 호출 가능 → 인수 컬렉션의 첫 요소가 이 위치에 삽입, 다른 요소가 그 뒤를 따르며 기존 요소는 끝 쪽으로 이동

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

- [`plus` 연산자](collection-plus-minus.md)의 제자리 버전 [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)으로도 요소 추가 가능
  - 가변 컬렉션에 적용 시 `+=`는 두 번째 피연산자(요소 또는 다른 컬렉션)를 컬렉션 끝에 추가

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

### 요소 제거

- 가변 컬렉션에서 요소 제거 → [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/remove.html) 함수 사용
  - 요소 값을 받아 해당 값의 첫 번째 발생 제거

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

- 한 번에 여러 요소를 제거하는 함수:
  - [`removeAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/remove-all.html): 인수 컬렉션에 있는 모든 요소 제거, 또는 술어와 함께 호출 시 술어가 `true`인 모든 요소 제거
  - [`retainAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/retain-all.html): `removeAll()`의 반대 - 인수 컬렉션의 요소를 제외한 모든 요소 제거, 술어 사용 시 일치하는 요소만 유지
  - [`clear()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/clear.html): 리스트의 모든 요소 제거, 비워둠

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

- 요소 제거의 또 다른 방법: [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 - [`minus`](collection-plus-minus.md)의 제자리 버전
  - 두 번째 인수는 요소 타입 단일 인스턴스이거나 다른 컬렉션 가능
    - 단일 요소인 경우: 해당 요소의 첫 번째 발생만 제거
    - 컬렉션인 경우: 해당 요소의 모든 발생 제거(중복 요소가 있으면 한 번에 모두 제거)
  - 두 번째 피연산자에 컬렉션에 없는 요소가 포함되어도 연산 실행에 영향 없음

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

### 요소 업데이트

- 리스트·맵은 요소 업데이트 연산도 제공 → [리스트별 연산](list-operations.md)·[맵별 연산](map-operations.md)에서 설명
- 셋의 경우 업데이트는 의미 없음 → 실제로는 요소를 제거하고 다른 것을 추가하는 것이기 때문

---

## 컬렉션 변환 연산

> 원문: https://kotlinlang.org/docs/collection-transformations.html

- Kotlin 표준 라이브러리 → 컬렉션 변환을 위한 확장 함수 세트 제공
  - 제공된 변환 규칙에 따라 기존 컬렉션에서 새 컬렉션 구축
  - 함수 그룹:
    - [Map](#map)
    - [Zip](#zip)
    - [Associate](#associate)
    - [Flatten](#flatten)
    - [문자열 표현](#문자열-표현)

### Map

- 매핑 변환: 다른 컬렉션의 요소에 대한 함수 결과로부터 컬렉션 생성
- 기본 매핑 함수 [`map()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map.html): 주어진 람다를 각 요소에 적용, 람다 결과의 리스트 반환
  - 결과 순서는 요소의 원래 순서와 동일
  - 요소 인덱스를 추가 인수로 사용하는 변환 → [`mapIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-indexed.html) 사용

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.map { it * 3 })
    println(numbers.mapIndexed { idx, value -> value * idx })
//sampleEnd
}
```

- 변환이 특정 요소에서 `null`을 생성하는 경우 → `map()` 대신 [`mapNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-not-null.html), `mapIndexed()` 대신 [`mapIndexedNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-indexed-not-null.html) 호출 → 결과에서 null 필터링

```kotlin
fun main() {
//sampleStart
    val numbers = setOf(1, 2, 3)
    println(numbers.mapNotNull { if ( it == 2) null else it * 3 })
    println(numbers.mapIndexedNotNull { idx, value -> if (idx == 0) null else value * idx })
//sampleEnd
}
```

- 맵 변환 시 두 옵션: 값을 변경하지 않고 키를 변환, 또는 반대
  - [`mapKeys()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-keys.html): 변환을 키에 적용
  - [`mapValues()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-values.html): 값을 변환
  - 두 함수 모두 맵 엔트리를 인수로 사용하는 변환을 사용 → 키·값 모두 연산 가능

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    println(numbersMap.mapKeys { it.key.uppercase() })
    println(numbersMap.mapValues { it.value + it.key.length })
//sampleEnd
}
```

### Zip

- 지핑 변환: 두 컬렉션에서 같은 위치 요소로 쌍을 구축
  - Kotlin 표준 라이브러리에서 [`zip()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/zip.html) 확장 함수로 수행

- 컬렉션·배열에서 다른 컬렉션(또는 배열)을 인수로 호출 → `zip()`은 `Pair` 객체의 `List` 반환
  - 수신자 컬렉션의 요소가 쌍의 첫 번째 요소
- 컬렉션 크기가 다른 경우 결과는 더 작은 크기 → 더 큰 컬렉션의 마지막 요소는 결과에 포함 안 됨
- `zip()`은 중위 형태 `a zip b`로도 호출 가능

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

- 수신자·인수 요소 두 매개변수를 받는 변환 함수와 함께 `zip()` 호출 가능
  - 결과 `List`는 같은 위치의 수신자·인수 요소 쌍에서 호출된 변환 함수의 반환 값 포함

```kotlin
fun main() {
//sampleStart
    val colors = listOf("red", "brown", "grey")
    val animals = listOf("fox", "bear", "wolf")

    println(colors.zip(animals) { color, animal -> "The ${animal.replaceFirstChar { it.uppercase() }} is $color"})
//sampleEnd
}
```

- `Pair`의 `List`가 있을 때 역변환(언지핑) 수행 가능 → 쌍에서 두 개의 리스트 구축:
  - 첫 번째 리스트: 원래 리스트에서 각 `Pair`의 첫 번째 요소
  - 두 번째 리스트: 두 번째 요소
- 쌍의 리스트를 언지핑하려면 [`unzip()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/unzip.html) 호출

```kotlin
fun main() {
//sampleStart
    val numberPairs = listOf("one" to 1, "two" to 2, "three" to 3, "four" to 4)
    println(numberPairs.unzip())
//sampleEnd
}
```

### Associate

- 연관 변환: 컬렉션 요소와 관련된 특정 값에서 맵 구축
  - 연관 타입에 따라 요소는 키 또는 값이 될 수 있음

- 기본 연관 함수 [`associateWith()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-with.html): 원래 요소가 키, 제공된 변환 함수 결과가 값인 `Map` 생성
  - 두 요소가 같으면 마지막 것만 맵에 남음

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.associateWith { it.length })
//sampleEnd
}
```

- 컬렉션 요소를 값으로 하는 맵 구축 → [`associateBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-by.html) 함수
  - 요소 값을 기반으로 키를 반환하는 함수를 받음, 두 요소의 키가 같으면 마지막 것만 남음
  - 값 변환 함수와 함께 호출 가능

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    println(numbers.associateBy { it.first().uppercaseChar() })
    println(numbers.associateBy(keySelector = { it.first().uppercaseChar() }, valueTransform = { it.length }))
//sampleEnd
}
```

- 키·값 모두 컬렉션 요소에서 생성되는 맵 구축 → [`associate()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate.html) 함수
  - `Pair`(해당 맵 엔트리의 키와 값)를 반환하는 람다 함수를 받음
  - 수명이 짧은 `Pair` 객체를 생성 → 성능에 영향 줄 수 있음 → 성능이 중요하지 않거나 다른 옵션보다 선호되는 경우에 사용

- 후자의 예: 키와 해당 값이 요소에서 함께 생성되는 경우

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

- 여기서는 먼저 요소마다 변환 함수를 호출한 다음 해당 결과의 속성에서 쌍을 구축

### Flatten

- 중첩된 컬렉션 처리 시 → 평면 접근을 제공하는 표준 라이브러리 함수가 유용

- [`flatten()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/flatten.html): 컬렉션의 컬렉션(예: `Set`의 `List`)에서 호출 가능
  - 중첩된 컬렉션의 모든 요소를 단일 `List`로 반환

```kotlin
fun main() {
//sampleStart
    val numberSets = listOf(setOf(1, 2, 3), setOf(4, 5, 6), setOf(1, 2))
    println(numberSets.flatten())
//sampleEnd
}
```

- [`flatMap()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/flat-map.html): 중첩된 컬렉션 처리를 위한 유연한 방법
  - 요소를 다른 컬렉션에 매핑하는 함수를 받음 → 모든 요소에 대한 반환 값의 단일 리스트 반환
  - `map()`(매핑 결과로 컬렉션 사용)과 `flatten()`의 순차적 호출처럼 동작

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

### 문자열 표현

- 컬렉션 내용을 읽을 수 있는 형식으로 조회 → 컬렉션을 문자열로 변환하는 함수: [`joinToString()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to-string.html)·[`joinTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to.html)
  - `joinToString()`: 제공된 인수 기반으로 요소에서 단일 `String` 구축
  - `joinTo()`: 동일 작업을 수행하지만 결과를 주어진 [`Appendable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-appendable/index.html) 객체에 추가

- 기본 인수로 호출 시 → 컬렉션의 `toString()` 호출과 유사한 결과: 요소의 문자열 표현이 쉼표·공백으로 구분된 `String`

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

- 사용자 정의 문자열 표현 구축 → 함수 인수 `separator`·`prefix`·`postfix` 지정
  - 결과 문자열은 `prefix`로 시작, `postfix`로 끝남
  - `separator`는 마지막 요소를 제외한 각 요소 뒤에 위치

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.joinToString(separator = " | ", prefix = "start: ", postfix = ": end"))
//sampleEnd
}
```

- 더 큰 컬렉션의 경우 결과에 포함할 요소 수(`limit`) 지정 가능
  - 컬렉션 크기가 `limit`를 초과하면 나머지 모든 요소는 `truncated` 인수의 단일 값으로 대체

```kotlin
fun main() {
//sampleStart
    val numbers = (1..100).toList()
    println(numbers.joinToString(limit = 10, truncated = "<...>"))
//sampleEnd
}
```

- 요소 자체의 표현을 사용자 정의하려면 `transform` 함수 제공

```kotlin
fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")
    println(numbers.joinToString { "Element: ${it.uppercase()}"})
//sampleEnd
}
```

---

## 컬렉션 필터링

> 원문: https://kotlinlang.org/docs/collection-filtering.html

- 필터링은 컬렉션 처리에서 가장 인기 있는 작업 중 하나
- Kotlin에서 필터링 조건은 술어(predicate)로 정의 → 요소를 받아 불리언 값을 반환하는 람다 함수(`true`: 일치, `false`: 불일치)

- 표준 라이브러리 → 단일 호출로 필터링 가능한 확장 함수 그룹 존재
  - 원래 컬렉션을 변경하지 않음 → [가변 및 읽기 전용](collections-overview.md#collection-types) 컬렉션 모두에서 사용 가능
  - 필터링 결과를 연산하려면 변수에 할당하거나 필터링 후 함수를 체이닝 필요

### 술어로 필터링

- 기본 필터링 함수 [`filter()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html): 술어와 함께 호출 시 일치하는 요소 반환
  - `List`·`Set` 모두 결과 컬렉션은 `List`, `Map`의 경우 결과도 `Map`

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

- `filter()`의 술어는 요소 값만 확인 가능
  - 필터에서 요소 위치를 사용하려면 [`filterIndexed()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-indexed.html) 사용(인덱스·값 두 인수를 받는 술어)

- 부정 조건으로 필터링 → [`filterNot()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not.html) 사용(술어가 `false`인 요소의 리스트 반환)

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

- 주어진 타입의 요소를 필터링하는 함수:
  - [`filterIsInstance()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-is-instance.html): 주어진 타입의 요소를 반환 - `List<Any>`에서 호출 시 `filterIsInstance<T>()`는 `List<T>` 반환 → `T` 타입 함수를 항목에서 호출 가능

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

  - [`filterNotNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-not-null.html): 모든 널이 아닌 요소를 반환 - `List<T?>`에서 호출 시 `List<T: Any>` 반환 → 요소를 널이 아닌 객체로 취급 가능

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

### 분할

- 필터링 함수 [`partition()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/partition.html): 술어로 필터링하고 일치하지 않는 요소를 별도 리스트에 유지
  - 반환 값은 `List`의 `Pair` - 첫 번째 리스트는 술어와 일치하는 요소, 두 번째는 나머지 모든 요소

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

### 술어 테스트

- 요소에 대해 술어를 단순히 테스트하는 함수:
  - [`any()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/any.html): 최소 하나의 요소가 술어와 일치하면 `true`
  - [`none()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/none.html): 어떤 요소도 술어와 일치하지 않으면 `true`
  - [`all()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/all.html): 모든 요소가 술어와 일치하면 `true`
    - 빈 컬렉션에서 유효한 술어와 함께 호출하면 `true` 반환(공허한 참, vacuous truth)

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

- `any()`·`none()`은 술어 없이도 사용 가능 → 컬렉션이 비어 있는지 확인
  - `any()`: 요소가 있으면 `true`, 없으면 `false`
  - `none()`: 반대로 동작

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

## 리스트별 연산

> 원문: https://kotlinlang.org/docs/list-operations.html

- [`List`](collections-overview.md#list)는 Kotlin에서 가장 인기 있는 내장 컬렉션 타입
  - 리스트 요소에 대한 인덱스 접근 → 강력한 연산 세트 제공

### 인덱스로 요소 조회

- 리스트는 요소 조회를 위한 모든 일반 연산 지원: `elementAt()`·`first()`·`last()` 등 [단일 요소 조회](collection-elements.md)에 나열된 연산
  - 리스트에 고유한 것: 요소에 대한 인덱스 접근
  - 요소를 읽는 가장 간단한 방법: 인덱스로 조회 - [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/get.html) 함수나 단축 `[index]` 구문

- 리스트 크기보다 큰 인덱스로 접근 시 예외 throw
  - [`getOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-else.html): 인덱스가 없을 때 기본값을 계산하는 함수 제공 가능
  - [`getOrNull()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-null.html): 기본값으로 `null` 반환

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

### 리스트 일부 조회

- [컬렉션 일부 조회](collection-parts.md) 연산 외에 리스트는 [`subList()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/sub-list.html) 함수 제공: 지정된 범위의 요소를 리스트로 반환
  - 원래 컬렉션의 요소가 변경되면 이전에 생성된 하위 리스트에서도 변경, 반대도 마찬가지

```kotlin
fun main() {
//sampleStart
    val numbers = (0..13).toList()
    println(numbers.subList(3, 6))
//sampleEnd
}
```

### 요소 위치 찾기

#### 선형 검색

- 모든 리스트에서 [`indexOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of.html)·[`lastIndexOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last-index-of.html) 함수로 요소 위치 검색 가능
  - 리스트에서 인수와 같은 요소의 첫 번째·마지막 위치 반환
  - 해당 요소가 없으면 `-1` 반환

```kotlin
fun main() {
//sampleStart
    val numbers = listOf(1, 2, 3, 4, 2, 5)
    println(numbers.indexOf(2))
    println(numbers.lastIndexOf(2))
//sampleEnd
}
```

- 술어와 일치하는 요소를 검색하는 함수 쌍:
  - [`indexOfFirst()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of-first.html): 술어와 일치하는 첫 번째 요소의 인덱스 반환, 없으면 `-1`
  - [`indexOfLast()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of-last.html): 술어와 일치하는 마지막 요소의 인덱스 반환, 없으면 `-1`

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    println(numbers.indexOfFirst { it > 2})
    println(numbers.indexOfLast { it % 2 == 1})
//sampleEnd
}
```

#### 정렬된 리스트에서 이진 검색

- 리스트에서 요소를 검색하는 또 다른 방법: [이진 검색](https://en.wikipedia.org/wiki/Binary_search_algorithm)
  - 다른 검색 함수보다 훨씬 빠름
  - 조건: 리스트가 특정 순서(자연 순서 또는 함수 매개변수에 제공된 다른 순서)에 따라 오름차순으로 [정렬](collection-ordering.md)되어야 함 → 그렇지 않으면 결과가 정의되지 않음

- 정렬된 리스트에서 검색 → 요소를 인수로 [`binarySearch()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/binary-search.html) 함수 호출
  - 요소가 존재하면 그 인덱스 반환
  - 존재하지 않으면 `(-insertionPoint - 1)` 반환(`insertionPoint`는 정렬 상태를 유지하며 삽입되어야 할 인덱스)
  - 동일 값을 가진 요소가 여러 개면 그 중 어떤 것의 인덱스도 반환될 수 있음

- 검색할 범위 지정 가능 → 두 제공된 인덱스 사이에서만 검색

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

##### Comparator 이진 검색

- 리스트 요소가 `Comparable`이 아닌 경우 → 이진 검색용 [`Comparator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparator/index.html) 제공 필요
  - 리스트는 이 `Comparator`에 따라 오름차순으로 정렬되어야 함

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

- `String.CASE_INSENSITIVE_ORDER`를 `Comparator`로 사용해 문자열 리스트에서 대소문자를 구분하지 않고 검색하는 예:

```kotlin
fun main() {
//sampleStart
    val colors = listOf("Blue", "green", "ORANGE", "Red", "yellow")
    println(colors.binarySearch("RED", String.CASE_INSENSITIVE_ORDER)) // 3
//sampleEnd
}
```

##### 비교 이진 검색

- 명시적 검색 값 없이 비교 함수를 사용하는 이진 검색 → 자연 순서·`Comparator`가 필요 없는 요소를 찾을 수 있음
  - 요소를 `Int` 값에 매핑하는 비교 함수를 받음
  - 함수는 검색된 요소보다 작은 요소에 양수, 큰 요소에 음수, 같으면 0을 반환해야 함
  - 제공된 비교기에 따라 리스트가 오름차순으로 정렬되어 있어야 함

- 이러한 검색은 전체 순서에 대한 비교 연산을 구현하지 않고도 정렬된 리스트에서 요소를 조회 가능하게 함

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

### 리스트 쓰기 연산

- [컬렉션 쓰기 연산](collection-write.md) 외에 [가변](collections-overview.md#collection-types) 리스트는 특정 쓰기 연산 지원
  - 인덱스를 사용하여 요소에 접근 → 리스트 수정 기능 확장

#### 추가

- 리스트의 특정 위치에 요소 추가 → 위치를 추가 인수로 사용해 [`add()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/add.html)·[`addAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/add-all.html) 호출
  - 해당 위치 뒤의 모든 요소가 오른쪽으로 이동

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

#### 업데이트

- 주어진 위치의 요소를 교체하는 [`set()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/set.html) 함수와 연산자 형태 `[]`
  - `set()`은 다른 요소의 인덱스를 변경하지 않음

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf("one", "five", "three")
    numbers[1] =  "two"
    println(numbers)
//sampleEnd
}
```

- [`fill()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fill.html): 모든 컬렉션 요소를 지정된 값으로 교체

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    numbers.fill(3)
    println(numbers)
//sampleEnd
}
```

#### 제거

- 특정 위치의 요소 제거 → 위치를 인수로 [`removeAt()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/remove-at.html) 함수 호출
  - 제거된 요소 뒤의 모든 요소 인덱스가 1씩 감소

```kotlin
fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4, 3)
    numbers.removeAt(1)
    println(numbers)
//sampleEnd
}
```

#### 정렬

- [컬렉션 정렬](collection-ordering.md)은 정렬된 순서로 요소를 조회하는 연산을 설명
  - 가변 리스트의 경우 표준 라이브러리는 리스트를 제자리에서 정렬하는 유사한 확장 함수 제공
  - 이러한 연산을 적용하면 해당 인스턴스의 요소 순서가 변경됨

- 제자리 정렬 함수는 `sorted*` 대신 `sort*` 이름 사용: [`sort()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort.html)·[`sortDescending()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort-descending.html)·[`sortBy()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/sort-by.html) 등

- 참조를 별도 변수에 복사하지 않고 가변 리스트의 요소 순서를 뒤집는 [`shuffle()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/shuffle.html)·[`reverse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/reverse.html)도 존재

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

## 셋별 연산

> 원문: https://kotlinlang.org/docs/set-operations.html

- Kotlin 컬렉션 패키지 → 셋에서 인기 있는 연산을 위한 확장 함수 포함: 교집합·합집합·차집합 찾기

- 두 컬렉션을 하나로 병합 → [`union()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/union.html) 함수(중위 형태 `a union b`도 가능)
  - 순서가 있는 컬렉션의 경우 피연산자 순서가 중요 → 결과에서 첫 번째 피연산자의 요소가 두 번째 피연산자의 요소 앞에 옴

- 두 컬렉션 모두에 있는 요소 → [`intersect()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/intersect.html)
- 다른 컬렉션에 없는 요소 → [`subtract()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/subtract.html)
  - 두 함수 모두 중위 형태 가능(예: `a intersect b`)

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

- 두 컬렉션 중 하나에는 있지만 교집합에는 없는 요소(대칭 차집합) → 차집합을 계산 후 병합

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

- `List`에도 `union()`·`intersect()`·`subtract()` 함수 적용 가능
  - 단 결과는 `List`에서도 항상 `Set` → 동일한 요소는 모두 하나로 병합, 인덱스 접근 불가

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

## 맵별 연산

> 원문: https://kotlinlang.org/docs/map-operations.html

- [맵](collections-overview.md#map)에서 키·값의 타입은 사용자가 정의
  - 맵 엔트리에 대한 키 기반 접근 → 키로 값 조회부터 키·값 별도 필터링까지 다양한 처리 기능 가능
  - 이 페이지는 표준 라이브러리의 맵 처리 함수 설명

### 키와 값 조회

- 맵에서 값 조회 → 키를 인수로 [`get()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/get.html) 함수 호출(단축 `[key]` 구문 지원)
  - 키를 찾지 못하면 `null` 반환
- [`getValue()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-value.html): 키를 찾을 수 없으면 예외 throw
- 누락된 키에 대한 기본값 생성 옵션:
  - [`getOrElse()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-else.html): `List`와 동일하게 작동 - 키를 찾을 수 없으면 주어진 람다에서 값 반환
  - [`getOrDefault()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/get-or-default.html): 키를 찾을 수 없으면 지정된 기본값 반환

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

- 맵의 모든 키·값에 대한 연산 → 각각 `keys`·`values` 속성에서 조회
  - `keys`: 맵 키의 셋 / `values`: 맵 값의 컬렉션

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap.keys)
    println(numbersMap.values)
//sampleEnd
}
```

### 필터링

- 다른 컬렉션과 마찬가지로 [`filter()`](collection-filtering.md) 함수로 맵 필터링 가능
  - 맵에서 `filter()` 호출 시 `Pair`를 인수로 하는 술어 전달 → 필터링 술어에서 키·값 모두 사용 가능

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
    val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
    println(filteredMap)
//sampleEnd
}
```

- 맵 필터링의 특정 방법 두 가지: 키로 필터링, 값으로 필터링
  - [`filterKeys()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-keys.html): 술어가 키만 확인, 일치하는 엔트리의 새 맵 반환
  - [`filterValues()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-values.html): 술어가 값만 확인, 일치하는 엔트리의 새 맵 반환

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

### `plus`와 `minus` 연산자

- 요소에 대한 키 접근 → [`plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html)(`+`)·[`minus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus.html)(`-`) 연산자가 다른 컬렉션과 다르게 작동
  - `plus`: 왼쪽 `Map`의 모든 엔트리와 오른쪽의 `Pair` 또는 다른 `Map` 요소를 포함하는 `Map` 반환
    - 오른쪽 피연산자에 왼쪽 `Map`과 같은 키가 있으면 결과 맵에는 오른쪽의 엔트리가 포함됨

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

- `minus`: 키가 오른쪽 피연산자에서 가져오는지에 따라 왼쪽 `Map`의 엔트리에서 `Map` 생성
  - 오른쪽 피연산자는 단일 키이거나 키 컬렉션(리스트·셋 등) 가능

```kotlin
fun main() {
//sampleStart
    val numbersMap = mapOf("one" to 1, "two" to 2, "three" to 3)
    println(numbersMap - "one")
    println(numbersMap - listOf("two", "four"))
//sampleEnd
}
```

- 가변 맵에서 [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`)·[`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자 사용 → 아래 [맵 쓰기 연산](#map-write-operations) 참조

### 맵 쓰기 연산 {id="map-write-operations"}

- [가변](collections-overview.md#collection-types) 맵 → 맵별 쓰기 연산 제공
  - 키 기반 값 접근으로 맵 내용 변경 가능

- 맵 쓰기 연산의 특정 규칙:
  - 값은 업데이트 가능, 키는 절대 변경되지 않음 → 엔트리 추가 시 키는 상수
  - 각 키에는 항상 하나의 연관된 값 존재, 전체 엔트리는 추가·제거 가능

#### 엔트리 추가 및 업데이트

- 가변 맵에 새 키-값 쌍 추가 → [`put()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/put.html) 사용
  - `LinkedHashMap`(기본 구현)에 새 엔트리를 넣으면 반복 시 마지막에 오도록 추가됨
  - 정렬된 맵에서는 새 요소 위치가 키 순서에 따라 정해짐

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2)
    numbersMap.put("three", 3)
    println(numbersMap)
//sampleEnd
}
```

- 한 번에 여러 엔트리 추가 → [`putAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/put-all.html)
  - 인수는 `Map`이거나 `Pair`의 그룹(`Iterable`·`Sequence`·`Array`) 가능

```kotlin
fun main() {
//sampleStart
    val numbersMap = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    numbersMap.putAll(setOf("four" to 4, "five" to 5))
    println(numbersMap)
//sampleEnd
}
```

- `put()`·`putAll()` 모두 주어진 키가 이미 존재하면 값을 덮어씀 → 엔트리 값 업데이트에 사용 가능

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

- 단축 연산자 형태로 새 엔트리 추가:
  - [`plusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus-assign.html)(`+=`) 연산자
  - `[]` 연산자 - `set()`의 별칭

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

- 맵에 있는 키로 호출하면 연산자는 해당 엔트리의 값을 덮어씀

#### 엔트리 제거

- 가변 맵에서 엔트리 제거 → [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/remove.html) 함수
  - 키나 전체 키-값 쌍을 전달 가능
  - 키·값을 모두 지정하면 해당 키의 값이 두 번째 인수와 일치하는 경우에만 제거됨

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

- 가변 맵의 `keys`·`values`에서 키·값을 제거해도 엔트리 제거 가능
  - `values`에서 `remove()` 호출 시 주어진 값과 일치하는 첫 번째 엔트리만 제거

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

- 가변 맵에서 [`minusAssign`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/minus-assign.html)(`-=`) 연산자도 사용 가능

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

## 시퀀스

> 원문: https://kotlinlang.org/docs/sequences.html

- Kotlin 표준 라이브러리 → 컬렉션과 별개로 시퀀스([`Sequence<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)) 타입 존재
  - 컬렉션과 달리 요소를 포함하지 않고, 반복하는 동안 요소를 생성
  - [`Iterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)과 동일한 함수 제공, 다단계 컬렉션 처리에 다른 접근 방식 구현

- `Iterable`의 다단계 처리는 즉시(eagerly) 실행 → 각 처리 단계 완료 후 중간 컬렉션 반환, 다음 단계는 이 컬렉션에서 실행
- 시퀀스의 다단계 처리는 가능한 경우 지연(lazily) 실행 → 실제 계산은 전체 처리 체인의 결과가 요청될 때만 발생
- 연산 실행 순서도 다름
  - `Sequence`: 모든 처리 단계를 각 요소에 대해 하나씩 수행
  - `Iterable`: 전체 컬렉션에 대해 각 단계를 완료한 후 다음 단계로 진행

- 시퀀스 → 중간 단계 결과 구축을 피해 전체 처리 체인의 성능 향상
  - 단 지연 특성 → 작은 컬렉션·간단한 계산 처리 시 상당한 오버헤드 추가 가능
  - 따라서 [`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)·[`Iterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html) 모두 고려해 상황에 맞는 것을 선택해야 함

### 생성

#### 요소로부터

- 시퀀스 생성 → [`sequenceOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/sequence-of.html) 함수 호출, 요소를 인수로 나열

```kotlin
val numbersSequence = sequenceOf("four", "three", "two", "one")
```

#### Iterable에서

- 이미 `Iterable` 객체(`List`·`Set` 등)가 있으면 [`asSequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/as-sequence.html) 호출로 시퀀스 생성 가능

```kotlin
val numbers = listOf("one", "two", "three", "four")
val numbersSequence = numbers.asSequence()
```

#### 함수로부터

- 요소를 계산하는 함수로 시퀀스 빌드 → 이 함수를 인수로 [`generateSequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/generate-sequence.html) 호출
  - 선택적으로 명시적 값이나 함수 호출 결과인 첫 요소 지정 가능
  - 제공된 함수가 `null`을 반환하면 시퀀스 생성 중지 → 아래 예제의 시퀀스는 무한

```kotlin
fun main() {
//sampleStart
    val oddNumbers = generateSequence(1) { it + 2 } // `it`은 이전 요소
    println(oddNumbers.take(5).toList())
    //println(oddNumbers.count())     // 오류: 시퀀스가 무한함
//sampleEnd
}
```

- `generateSequence()`로 유한 시퀀스를 생성하려면 필요한 마지막 요소 다음에 `null`을 반환하는 함수 제공

```kotlin
fun main() {
//sampleStart
    val oddNumbersLessThan10 = generateSequence(1) { if (it < 8) it + 2 else null }
    println(oddNumbersLessThan10.count())
//sampleEnd
}
```

#### 청크로부터

- 시퀀스 요소를 하나씩 또는 임의 크기의 청크로 생성 → [`sequence()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/sequence.html) 함수
  - [`yield()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence-scope/yield.html)·[`yieldAll()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence-scope/yield-all.html) 호출을 포함하는 람다를 받음
    - 시퀀스 소비자에게 요소를 반환하고 다음 요소를 요청할 때까지 실행을 일시 중단
    - `yield()`: 단일 요소를 인수로 받음
    - `yieldAll()`: `Iterable`·`Iterator`·다른 `Sequence`를 받을 수 있음(`Sequence` 인수는 무한 가능)
  - 이러한 호출은 마지막이어야 함 → 이후 호출은 절대 실행되지 않음

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

### 시퀀스 연산

- 시퀀스 연산은 상태 요구 사항에 따라 분류 가능:
  - 무상태 연산: 상태가 필요 없고 각 요소를 독립적으로 처리 - 예: [`map()`](collection-transformations.md#map)·[`filter()`](collection-filtering.md)
    - 요소 처리에 상수량의 상태가 필요한 경우도 포함 - 예: [`take()`·`drop()`](collection-parts.md)
  - 상태유지 연산: 일반적으로 시퀀스 요소 수에 비례하는 상당한 양의 상태 필요

- 시퀀스 연산이 다른 시퀀스를 반환하며 지연 생성되면 중간 연산
  - 그렇지 않으면 터미널 연산(예: [`toList()`](constructing-collections.md#copy)·[`sum()`](collection-aggregate.md))
  - 시퀀스 요소는 터미널 연산으로만 검색 가능

- 시퀀스는 여러 번 반복 가능 → 단 일부 구현은 한 번만 반복 가능하도록 제한될 수 있음(문서에 구체적으로 명시)

### 시퀀스 처리 예제

Iterable과 Sequence의 차이점을 예제로 확인.

#### Iterable

- 단어 리스트가 있다고 가정 → 아래 코드는 세 글자보다 긴 단어를 필터링하고 처음 네 단어의 길이를 출력

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

- 이 코드 실행 시 `filter()`·`map()` 함수가 코드에 나타난 순서로 실행됨
  - 모든 요소에 대해 `filter:` 출력 → 필터링 후 남은 요소에 대해 `length:` 출력 → 마지막 두 줄 출력

리스트 처리 흐름:

![리스트 처리](list-processing.png)

#### Sequence

동일한 것을 시퀀스로 작성.

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

- 이 코드의 출력 → `filter()`·`map()` 함수가 결과 리스트가 빌드될 때만 호출됨을 보여줌
  - 먼저 "Lengths of.." 텍스트 줄 출력 → 시퀀스 처리 시작
  - 필터링 후 남은 요소는 다음 요소를 필터링하기 전에 맵이 실행됨
  - 결과 크기가 4에 도달하면(`take(4)`의 최대 크기) 처리 중지

시퀀스 처리 흐름:

![시퀀스 처리](sequence-processing.png)

- 이 예제에서 시퀀스 처리는 23단계가 아닌 18단계 소요

---

## 이터레이터

> 원문: https://kotlinlang.org/docs/iterators.html

- 컬렉션 순회를 위해 Kotlin 표준 라이브러리 → 이터레이터 메커니즘 지원
  - 이터레이터: 기본 구조를 노출하지 않고 요소에 순차적으로 접근하는 객체
  - 모든 요소를 하나씩 처리해야 할 때 유용(값 출력, 유사한 업데이트 등)

- 이터레이터는 [`Iterator<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/index.html) 인터페이스의 `iterator()` 함수 호출로 [`Iterable<T>`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html) 상속자(`Set`·`List` 포함)에서 획득 가능

- 이터레이터를 얻으면 컬렉션의 첫 요소를 가리킴
  - [`next()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/next.html) 호출 → 요소를 반환하고 위치를 다음 요소(존재 시)로 이동

- 이터레이터가 마지막 요소를 통과하면 더 이상 사용 불가, 이전 위치로 재설정도 불가
  - 컬렉션을 다시 반복하려면 새 이터레이터 생성 필요

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

- `Iterable` 컬렉션을 순회하는 또 다른 방법: `for` 루프
  - 컬렉션에 `for` 사용 시 암묵적으로 이터레이터를 얻음 → 다음 코드는 위 예제와 동등

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

- 컬렉션을 반복하며 각 요소에 대해 람다를 실행하는 `forEach()` 함수도 존재

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

### 리스트 이터레이터

- 리스트의 경우 특별한 이터레이터 구현 [`ListIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/index.html) 존재
  - 양방향 반복 지원(순방향·역방향)

- 역방향 반복: [`hasPrevious()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/has-previous.html)·[`previous()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/previous.html) 함수로 구현
- `ListIterator`는 [`nextIndex()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/next-index.html)·[`previousIndex()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list-iterator/previous-index.html) 함수로 요소 인덱스 정보 제공

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

- 양방향 반복 가능 → `ListIterator`가 마지막 요소에 도달한 후에도 여전히 사용 가능

### 가변 이터레이터

- 가변 컬렉션 반복 → [`MutableIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html) 사용
  - 요소 제거 함수 [`remove()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/remove.html)로 `Iterator` 확장
  - 반복 중 컬렉션에서 요소 제거 가능

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

- [`MutableListIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list-iterator/index.html): 요소 삽입·교체도 가능

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

## 범위와 진행

> 원문: https://kotlinlang.org/docs/ranges.html

- Kotlin에서는 `kotlin.ranges` 패키지의 [`.rangeTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-to.html)·[`.rangeUntil()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-until.html) 함수로 값의 범위를 쉽게 생성 가능
  - 예: 다음 범위에는 `1`부터 `4`까지의 숫자가 포함됨

- 대괄호를 사용한 범위 생성 → 두 점 `..`을 가진 [`.rangeTo()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/range-to.html) 함수를 연산자 형태로 호출

```kotlin
fun main() {
//sampleStart
    // 양쪽 끝이 포함되는 범위
    println(4 in 1..4)
    // true
//sampleEnd
}
```

- 열린 끝 범위 생성 → `..<` 연산자 사용(예: `1..<4`는 `1`, `2`, `3`을 나타냄)

```kotlin
fun main() {
//sampleStart
    // 열린 끝 범위
    println(4 in 1..<4)
    // false
//sampleEnd
}
```

- 역순으로 범위를 반복 → `..` 대신 [`downTo`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/down-to.html) 함수 사용

```kotlin
fun main() {
//sampleStart
    for (i in 4 downTo 1) print(i)
    // 4321
//sampleEnd
}
```

- 1이 아닌 임의 스텝으로 범위를 반복 → [`step`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/step.html) 함수 사용

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

### 진행(Progression)

- `Int`·`Long`·`Char` 같은 정수 타입의 범위는 [등차수열](https://en.wikipedia.org/wiki/Arithmetic_progression)로 취급 가능
  - Kotlin에서는 특별한 타입으로 정의: [`IntProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-int-progression/index.html)·[`LongProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-long-progression/index.html)·[`CharProgression`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-char-progression/index.html)

- 진행의 세 가지 필수 속성: `first` 요소, `last` 요소, 0이 아닌 `step`
  - 첫 요소는 `first`, 후속 요소는 이전 요소에 `step`을 더한 값
  - 양수 스텝을 가진 진행에서의 반복은 Java/JavaScript의 인덱스 있는 `for` 루프와 동등:

```java
for (int i = first; i <= last; i += step) {
  // ...
}
```

- 범위를 반복해 암묵적으로 진행을 생성하면 → 이 진행의 `first`·`last` 요소는 범위의 끝점, `step`은 1

```kotlin
fun main() {
//sampleStart
    for (i in 1..10) print(i)
    // 12345678910
//sampleEnd
}
```

- 사용자 정의 진행 스텝 정의 → 범위에서 `step` 함수 사용

```kotlin
fun main() {
//sampleStart
    for (i in 1..8 step 2) print(i)
    // 1357
//sampleEnd
}
```

- 진행의 `last` 요소 계산 방식:
  - 양수 스텝: `(last - first) % step == 0`을 만족하는 끝 값보다 크지 않은 최댓값
  - 음수 스텝: `(last - first) % step == 0`을 만족하는 끝 값보다 작지 않은 최솟값
- 따라서 `last` 요소가 항상 지정된 끝 값과 같지는 않음

```kotlin
fun main() {
//sampleStart
    for (i in 1..9 step 3) print(i) // 마지막 요소는 7
    // 147
//sampleEnd
}
```

- 진행은 `Iterable<N>`을 구현(`N`은 각각 `Int`·`Long`·`Char`) → `map`·`filter` 등 다양한 [컬렉션 함수](collection-operations.md)에서 사용 가능

```kotlin
fun main() {
//sampleStart
    println((1..10).filter { it % 2 == 0 })
    // [2, 4, 6, 8, 10]
//sampleEnd
}
```
