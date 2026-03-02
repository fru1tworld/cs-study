# 범위와 진행

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
