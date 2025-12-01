# Kotlin 컬렉션

> 공식 문서: https://kotlinlang.org/docs/collections-overview.html

## 개요

Kotlin 표준 라이브러리는 컬렉션 관리를 위한 포괄적인 도구 세트를 제공합니다. 컬렉션 타입은 **불변(read-only)**과 **가변(mutable)** 두 종류로 나뉩니다.

---

## 컬렉션 타입

### 계층 구조

```
Iterable
  └── Collection
        ├── List
        ├── Set
        └── MutableCollection
              ├── MutableList
              └── MutableSet

Map (별도 계층)
  └── MutableMap
```

### 컬렉션 인터페이스

| 인터페이스 | 특성 | 가변 버전 |
|-----------|------|----------|
| List | 순서 있음, 인덱스 접근, 중복 허용 | MutableList |
| Set | 순서 없음, 중복 불허 | MutableSet |
| Map | 키-값 쌍, 키는 고유 | MutableMap |

---

## List

### 생성

```kotlin
// 불변 리스트
val numbers = listOf(1, 2, 3, 4, 5)
val empty = emptyList<String>()
val nullableList = listOfNotNull(1, null, 2, null, 3)  // [1, 2, 3]

// 가변 리스트
val mutableNumbers = mutableListOf(1, 2, 3)
val arrayList = arrayListOf(1, 2, 3)

// 빌더
val list = buildList {
    add(1)
    add(2)
    addAll(listOf(3, 4, 5))
}
```

### 접근

```kotlin
val list = listOf("a", "b", "c", "d")

// 인덱스 접근
println(list[0])          // "a"
println(list.get(0))      // "a"
println(list.getOrNull(10))   // null
println(list.getOrElse(10) { "default" })  // "default"

// 첫 번째/마지막
println(list.first())     // "a"
println(list.last())      // "d"
println(list.firstOrNull())   // "a"
println(list.lastOrNull())    // "d"

// 조건부
println(list.first { it > "a" })  // "b"
println(list.last { it < "d" })   // "c"

// 부분 리스트
println(list.subList(1, 3))  // ["b", "c"]
println(list.take(2))        // ["a", "b"]
println(list.drop(2))        // ["c", "d"]
println(list.takeLast(2))    // ["c", "d"]
println(list.dropLast(2))    // ["a", "b"]
```

### 수정 (MutableList)

```kotlin
val list = mutableListOf(1, 2, 3)

// 추가
list.add(4)              // [1, 2, 3, 4]
list.add(0, 0)           // [0, 1, 2, 3, 4]
list.addAll(listOf(5, 6))  // [0, 1, 2, 3, 4, 5, 6]

// 제거
list.remove(0)           // [1, 2, 3, 4, 5, 6]
list.removeAt(0)         // [2, 3, 4, 5, 6]
list.removeAll { it > 4 }  // [2, 3, 4]
list.retainAll { it > 2 }  // [3, 4]
list.clear()             // []

// 수정
list.add(1)
list.add(2)
list[0] = 10             // [10, 2]
list.set(1, 20)          // [10, 20]
```

---

## Set

### 생성

```kotlin
// 불변 Set (LinkedHashSet - 삽입 순서 유지)
val numbers = setOf(1, 2, 3, 3, 4)  // [1, 2, 3, 4]
val empty = emptySet<String>()

// 가변 Set
val mutableSet = mutableSetOf(1, 2, 3)
val hashSet = hashSetOf(1, 2, 3)       // HashSet
val linkedSet = linkedSetOf(1, 2, 3)   // LinkedHashSet
val sortedSet = sortedSetOf(3, 1, 2)   // TreeSet: [1, 2, 3]

// 빌더
val set = buildSet {
    add(1)
    add(2)
    addAll(listOf(3, 4))
}
```

### 연산

```kotlin
val set1 = setOf(1, 2, 3)
val set2 = setOf(2, 3, 4)

// 합집합
println(set1 union set2)      // [1, 2, 3, 4]
println(set1 + set2)          // [1, 2, 3, 4]

// 교집합
println(set1 intersect set2)  // [2, 3]

// 차집합
println(set1 subtract set2)   // [1]
println(set1 - set2)          // [1]

// 포함 여부
println(2 in set1)            // true
println(set1.contains(2))     // true
println(set1.containsAll(listOf(1, 2)))  // true
```

---

## Map

### 생성

```kotlin
// 불변 Map (LinkedHashMap - 삽입 순서 유지)
val map = mapOf("a" to 1, "b" to 2, "c" to 3)
val empty = emptyMap<String, Int>()

// 가변 Map
val mutableMap = mutableMapOf("a" to 1, "b" to 2)
val hashMap = hashMapOf("a" to 1)     // HashMap
val linkedMap = linkedMapOf("a" to 1) // LinkedHashMap
val sortedMap = sortedMapOf("b" to 2, "a" to 1)  // TreeMap

// Pair로 생성
val map2 = mapOf(Pair("a", 1), Pair("b", 2))

// 빌더
val map3 = buildMap {
    put("a", 1)
    put("b", 2)
    putAll(mapOf("c" to 3, "d" to 4))
}
```

### 접근

```kotlin
val map = mapOf("a" to 1, "b" to 2, "c" to 3)

// 값 접근
println(map["a"])                    // 1
println(map.get("a"))                // 1
println(map.getOrDefault("d", 0))    // 0
println(map.getOrElse("d") { 0 })    // 0

// 키, 값, 엔트리
println(map.keys)       // [a, b, c]
println(map.values)     // [1, 2, 3]
println(map.entries)    // [a=1, b=2, c=3]

// 포함 여부
println("a" in map)              // true
println(map.containsKey("a"))    // true
println(map.containsValue(1))    // true
```

### 수정 (MutableMap)

```kotlin
val map = mutableMapOf("a" to 1, "b" to 2)

// 추가/수정
map["c"] = 3                    // {a=1, b=2, c=3}
map.put("d", 4)                 // {a=1, b=2, c=3, d=4}
map.putAll(mapOf("e" to 5))     // {a=1, b=2, c=3, d=4, e=5}

// 조건부 추가
map.putIfAbsent("a", 10)        // 이미 존재하면 무시
map.getOrPut("f") { 6 }         // 없으면 추가하고 반환

// 제거
map.remove("a")                  // {b=2, c=3, d=4, e=5, f=6}
map.remove("b", 2)               // 키와 값 모두 일치할 때만 제거
map.clear()                      // {}
```

### 순회

```kotlin
val map = mapOf("a" to 1, "b" to 2, "c" to 3)

// 엔트리 순회
for ((key, value) in map) {
    println("$key = $value")
}

// forEach
map.forEach { (key, value) ->
    println("$key = $value")
}

// 키만 순회
for (key in map.keys) {
    println(key)
}

// 값만 순회
for (value in map.values) {
    println(value)
}
```

---

## 컬렉션 변환

### map

```kotlin
val numbers = listOf(1, 2, 3)

// 변환
val doubled = numbers.map { it * 2 }  // [2, 4, 6]

// 인덱스와 함께
val indexed = numbers.mapIndexed { index, value ->
    "[$index] = $value"
}

// null이 아닌 결과만
val notNull = numbers.mapNotNull {
    if (it > 1) it * 2 else null
}  // [4, 6]

// Map에서 키/값 변환
val map = mapOf("a" to 1, "b" to 2)
val mappedKeys = map.mapKeys { it.key.uppercase() }  // {A=1, B=2}
val mappedValues = map.mapValues { it.value * 10 }   // {a=10, b=20}
```

### flatMap

```kotlin
val containers = listOf(
    listOf(1, 2, 3),
    listOf(4, 5),
    listOf(6)
)

// 중첩 컬렉션 평탄화 및 변환
val flat = containers.flatMap { it }  // [1, 2, 3, 4, 5, 6]

val doubled = containers.flatMap { list ->
    list.map { it * 2 }
}  // [2, 4, 6, 8, 10, 12]

// flatten (단순 평탄화)
val flattened = containers.flatten()  // [1, 2, 3, 4, 5, 6]
```

### filter

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6)

// 조건 필터링
val evens = numbers.filter { it % 2 == 0 }     // [2, 4, 6]
val odds = numbers.filterNot { it % 2 == 0 }   // [1, 3, 5]

// 인덱스와 함께
val filtered = numbers.filterIndexed { index, _ ->
    index % 2 == 0
}  // [1, 3, 5] (인덱스 0, 2, 4)

// 타입으로 필터링
val mixed = listOf(1, "two", 3, "four", 5)
val strings = mixed.filterIsInstance<String>()  // ["two", "four"]

// null이 아닌 것만
val nullable = listOf(1, null, 2, null, 3)
val notNull = nullable.filterNotNull()  // [1, 2, 3]

// partition (분할)
val (evens2, odds2) = numbers.partition { it % 2 == 0 }
// evens2: [2, 4, 6], odds2: [1, 3, 5]

// Map 필터링
val map = mapOf("a" to 1, "b" to 2, "c" to 3)
val filtered2 = map.filter { it.value > 1 }  // {b=2, c=3}
val filteredKeys = map.filterKeys { it > "a" }  // {b=2, c=3}
val filteredValues = map.filterValues { it < 3 }  // {a=1, b=2}
```

### groupBy

```kotlin
val words = listOf("apple", "banana", "cherry", "apricot", "blueberry")

// 첫 글자로 그룹화
val grouped = words.groupBy { it.first() }
// {a=[apple, apricot], b=[banana, blueberry], c=[cherry]}

// 변환과 함께
val groupedLength = words.groupBy(
    keySelector = { it.first() },
    valueTransform = { it.length }
)
// {a=[5, 7], b=[6, 9], c=[6]}

// groupingBy (지연 그룹화)
val counts = words.groupingBy { it.first() }.eachCount()
// {a=2, b=2, c=1}
```

### associate

```kotlin
val words = listOf("one", "two", "three")

// 키-값 쌍으로 변환
val map = words.associateWith { it.length }
// {one=3, two=3, three=5}

val map2 = words.associateBy { it.first() }
// {o=one, t=three}

val map3 = words.associate { it to it.length }
// {one=3, two=3, three=5}
```

### zip

```kotlin
val list1 = listOf(1, 2, 3)
val list2 = listOf("a", "b", "c", "d")

// 결합
val zipped = list1.zip(list2)
// [(1, a), (2, b), (3, c)]

// 변환과 함께
val zipped2 = list1.zip(list2) { a, b -> "$a$b" }
// ["1a", "2b", "3c"]

// 분리
val pairs = listOf(1 to "a", 2 to "b", 3 to "c")
val (numbers, letters) = pairs.unzip()
// numbers: [1, 2, 3], letters: ["a", "b", "c"]
```

---

## 컬렉션 집계

### 기본 집계

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// 크기
println(numbers.count())                    // 5
println(numbers.count { it > 2 })           // 3

// 합계/평균
println(numbers.sum())                      // 15
println(numbers.average())                  // 3.0

// 최대/최소
println(numbers.max())                      // 5
println(numbers.min())                      // 1
println(numbers.maxOrNull())                // 5 (빈 컬렉션시 null)
println(numbers.minOrNull())                // 1

// 조건부 최대/최소
val words = listOf("one", "two", "three")
println(words.maxBy { it.length })          // "three"
println(words.minBy { it.length })          // "one"
println(words.maxWith(compareBy { it.length }))  // "three"

// 범위
println(numbers.first())                    // 1
println(numbers.last())                     // 5
```

### reduce와 fold

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// reduce: 첫 번째 요소가 초기값
val sum = numbers.reduce { acc, n -> acc + n }  // 15
val product = numbers.reduce { acc, n -> acc * n }  // 120

// fold: 초기값 지정
val sumFrom10 = numbers.fold(10) { acc, n -> acc + n }  // 25

// 오른쪽에서 왼쪽으로
val reversed = numbers.foldRight("") { n, acc -> "$acc$n" }  // "54321"

// 인덱스와 함께
val indexed = numbers.foldIndexed(0) { index, acc, n ->
    acc + index * n
}  // 0*1 + 1*2 + 2*3 + 3*4 + 4*5 = 40
```

### 조건 검사

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// any: 하나라도 만족
println(numbers.any { it > 3 })      // true
println(numbers.any())               // true (비어있지 않음)

// all: 모두 만족
println(numbers.all { it > 0 })      // true
println(numbers.all { it > 3 })      // false

// none: 아무것도 만족하지 않음
println(numbers.none { it > 5 })     // true
println(numbers.none())              // false (비어있지 않음)

// contains
println(3 in numbers)                // true
println(numbers.contains(3))         // true
println(numbers.containsAll(listOf(1, 2)))  // true
```

---

## 컬렉션 순서

### 정렬

```kotlin
val numbers = listOf(3, 1, 4, 1, 5, 9, 2, 6)

// 정렬 (새 리스트 반환)
println(numbers.sorted())            // [1, 1, 2, 3, 4, 5, 6, 9]
println(numbers.sortedDescending())  // [9, 6, 5, 4, 3, 2, 1, 1]

// 조건 정렬
val words = listOf("cherry", "apple", "banana")
println(words.sortedBy { it.length })  // [apple, cherry, banana]
println(words.sortedByDescending { it.length })

// Comparator로 정렬
println(words.sortedWith(compareBy { it.length }))

// 역순
println(numbers.reversed())  // 새 리스트
println(numbers.asReversed())  // 뷰 (원본 변경 시 함께 변경)

// 무작위 섞기
println(numbers.shuffled())

// MutableList 제자리 정렬
val mutable = numbers.toMutableList()
mutable.sort()
mutable.sortDescending()
mutable.shuffle()
mutable.reverse()
```

### 사용자 정의 정렬

```kotlin
data class Person(val name: String, val age: Int)

val people = listOf(
    Person("Alice", 30),
    Person("Bob", 25),
    Person("Charlie", 30)
)

// 여러 기준 정렬
val sorted = people.sortedWith(
    compareBy({ it.age }, { it.name })
)
// [Bob(25), Alice(30), Charlie(30)]

// 복합 비교
val sorted2 = people.sortedWith(
    compareBy<Person> { it.age }.thenBy { it.name }
)
```

---

## Sequence

지연 평가(lazy evaluation)를 사용하여 대용량 컬렉션을 효율적으로 처리합니다.

### 생성

```kotlin
// 컬렉션에서
val sequence = listOf(1, 2, 3).asSequence()

// sequenceOf
val seq = sequenceOf(1, 2, 3)

// generateSequence
val naturalNumbers = generateSequence(1) { it + 1 }  // 무한 시퀀스
val limitedNaturals = naturalNumbers.take(10).toList()

// sequence 빌더
val fibonacciSequence = sequence {
    var a = 0
    var b = 1
    yield(a)
    yield(b)
    while (true) {
        val next = a + b
        yield(next)
        a = b
        b = next
    }
}
println(fibonacciSequence.take(10).toList())
// [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
```

### 컬렉션 vs 시퀀스

```kotlin
// 컬렉션: 각 단계마다 중간 컬렉션 생성
val result = listOf(1, 2, 3, 4, 5)
    .map { it * 2 }      // [2, 4, 6, 8, 10]
    .filter { it > 5 }   // [6, 8, 10]
    .take(2)             // [6, 8]

// 시퀀스: 요소별로 전체 체인 처리 (중간 컬렉션 없음)
val result2 = listOf(1, 2, 3, 4, 5)
    .asSequence()
    .map { it * 2 }
    .filter { it > 5 }
    .take(2)
    .toList()  // [6, 8]

// 큰 데이터셋에서 효율적
val largeList = (1..1_000_000).toList()

// 비효율적: 모든 요소 처리
val first = largeList
    .map { it * 2 }
    .first { it > 100 }

// 효율적: 조건 만족하면 즉시 종료
val first2 = largeList.asSequence()
    .map { it * 2 }
    .first { it > 100 }
```

---

## 컬렉션 연산자 (+, -)

```kotlin
val list = listOf(1, 2, 3)
val set = setOf(1, 2, 3)

// 추가
val plusOne = list + 4           // [1, 2, 3, 4]
val plusMany = list + listOf(4, 5)  // [1, 2, 3, 4, 5]

// 제거
val minusOne = list - 2          // [1, 3]
val minusMany = list - listOf(1, 2)  // [3]

// MutableList에서
val mutableList = mutableListOf(1, 2, 3)
mutableList += 4           // [1, 2, 3, 4]
mutableList -= 1           // [2, 3, 4]
mutableList += listOf(5, 6)  // [2, 3, 4, 5, 6]

// Map에서
val map = mapOf("a" to 1, "b" to 2)
val plusMap = map + ("c" to 3)    // {a=1, b=2, c=3}
val minusMap = map - "a"          // {b=2}
```

---

## 유용한 함수들

### 변환

```kotlin
val list = listOf(1, 2, 3)

// 다른 타입으로
list.toSet()
list.toMutableList()
list.toTypedArray()

// Map으로
list.associate { it to it.toString() }
list.associateWith { it.toString() }
list.associateBy { it.toString() }

// 문자열로
list.joinToString()                    // "1, 2, 3"
list.joinToString("-")                 // "1-2-3"
list.joinToString(prefix = "[", postfix = "]")  // "[1, 2, 3]"
```

### 검색

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// 찾기
println(numbers.find { it > 2 })           // 3
println(numbers.findLast { it < 4 })       // 3
println(numbers.indexOf(3))                 // 2
println(numbers.lastIndexOf(3))             // 2
println(numbers.indexOfFirst { it > 2 })   // 2
println(numbers.indexOfLast { it < 5 })    // 3

// 이진 검색 (정렬된 리스트)
val sorted = listOf(1, 2, 3, 4, 5)
println(sorted.binarySearch(3))            // 2
```

### 기타

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// distinct
listOf(1, 1, 2, 2, 3).distinct()  // [1, 2, 3]
listOf(1, 1, 2, 2, 3).distinctBy { it % 2 }  // [1, 2]

// chunked (청크로 분할)
numbers.chunked(2)  // [[1, 2], [3, 4], [5]]

// windowed (슬라이딩 윈도우)
numbers.windowed(3)  // [[1, 2, 3], [2, 3, 4], [3, 4, 5]]
numbers.windowed(3, step = 2)  // [[1, 2, 3], [3, 4, 5]]

// onEach (부수 효과)
numbers.onEach { println(it) }.map { it * 2 }
```

---

## 참고 문서

- [Collections overview](https://kotlinlang.org/docs/collections-overview.html)
- [Constructing collections](https://kotlinlang.org/docs/constructing-collections.html)
- [Collection operations](https://kotlinlang.org/docs/collection-operations.html)
- [Sequences](https://kotlinlang.org/docs/sequences.html)
