# 리스트별 연산

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
