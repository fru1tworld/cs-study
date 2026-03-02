# 고차 함수와 람다

## 개요

Kotlin 함수는 **일급**입니다. 즉 변수에 저장하고, 인수로 전달하고, 다른 함수에서 반환할 수 있습니다. Kotlin은 **함수 타입** 계열과 **람다 표현식** 같은 언어 구조를 사용하여 이를 용이하게 합니다.

## 고차 함수

**고차 함수**는 함수를 매개변수로 받거나 함수를 반환합니다.

### 예제: `fold`
```kotlin
fun <T, R> Collection<T>.fold(
    initial: R,
    combine: (acc: R, nextElement: T) -> R
): R {
    var accumulator: R = initial
    for (element: T in this) {
        accumulator = combine(accumulator, element)
    }
    return accumulator
}
```

### 사용 예제
```kotlin
fun main() {
    val items = listOf(1, 2, 3, 4, 5)

    // 명시적 타입의 람다
    items.fold(0, { acc: Int, i: Int ->
        print("acc = $acc, i = $i, ")
        val result = acc + i
        println("result = $result")
        result
    })

    // 추론된 타입의 람다
    val joinedToString = items.fold("Elements:", { acc, i ->
        acc + " " + i
    })

    // 함수 참조 사용
    val product = items.fold(1, Int::times)
}
```

## 함수 타입

함수 타입은 매개변수와 반환 값이 있는 함수 시그니처를 나타냅니다.

### 문법

- **기본**: `(A, B) -> C` - 타입 A와 B를 받아 C 반환
- **매개변수 없음**: `() -> A`
- **수신자 포함**: `A.(B) -> C`
- **일시 중단**: `suspend () -> Unit` 또는 `suspend A.(B) -> C`
- **널 가능**: `((Int, Int) -> Int)?`
- **타입 별칭**: `typealias ClickHandler = (Button, ClickEvent) -> Unit`

### 함수 타입 인스턴스화

**1. 람다 표현식:**
```kotlin
{ a, b -> a + b }
```

**2. 익명 함수:**
```kotlin
fun(s: String): Int { return s.toIntOrNull() ?: 0 }
```

**3. 호출 가능 참조:**
```kotlin
::isOdd
String::toInt
List<Int>::size
::Regex
```

**4. 함수 타입을 구현하는 커스텀 클래스:**
```kotlin
class IntTransformer: (Int) -> Int {
    override operator fun invoke(x: Int): Int = TODO()
}
val intFunction: (Int) -> Int = IntTransformer()
```

### 함수 타입 호출

```kotlin
val stringPlus: (String, String) -> String = String::plus
val intPlus: Int.(Int) -> Int = Int::plus

println(stringPlus.invoke("<-", "->"))
println(stringPlus("Hello, ", "world!"))
println(intPlus.invoke(1, 1))
println(intPlus(1, 2))
println(2.intPlus(3))  // 확장과 같은 호출
```

## 람다 표현식과 익명 함수

### 람다 표현식 문법

**전체 형식:**
```kotlin
val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
```

**축약 형식:**
```kotlin
val sum = { x: Int, y: Int -> x + y }
```

핵심 포인트:
- 중괄호로 둘러쌈
- `->` 앞에 선택적 타입 어노테이션이 있는 매개변수
- 마지막 표현식이 반환 값

### 후행 람다

마지막 매개변수가 함수면 람다를 괄호 밖에 배치할 수 있습니다:

```kotlin
val product = items.fold(1) { acc, e -> acc * e }
```

람다가 유일한 인수면 괄호를 생략할 수 있습니다:
```kotlin
run { println("...") }
```

### 암시적 `it` 매개변수

단일 매개변수 람다의 경우 암시적 `it`을 사용합니다:

```kotlin
ints.filter { it > 0 }  // 타입: (it: Int) -> Boolean
```

### 람다에서 반환

**암시적 반환** (마지막 표현식):
```kotlin
ints.filter { val shouldFilter = it > 0; shouldFilter }
```

**명시적 한정된 반환:**
```kotlin
ints.filter { val shouldFilter = it > 0; return@filter shouldFilter }
```

### 사용하지 않는 변수에 밑줄

```kotlin
map.forEach { (_, value) -> println("$value!") }
```

### 익명 함수

반환 타입을 명시적으로 지정해야 할 때 사용합니다:

```kotlin
fun(x: Int, y: Int): Int = x + y

// 또는 블록 본문으로
fun(x: Int, y: Int): Int {
    return x + y
}

// filter와 함께 사용
ints.filter(fun(item) = item > 0)
```

**핵심 차이점:** 익명 함수에서 `return`은 둘러싸는 함수가 아닌 함수 자체에서 반환됩니다.

## 클로저

람다는 외부 스코프의 변수에 접근하고 수정할 수 있습니다:

```kotlin
var sum = 0
ints.filter { it > 0 }.forEach { sum += it }
print(sum)
```

## 수신자가 있는 함수 리터럴

수신자 타입 `A.(B) -> C`가 있는 함수는 수신자를 암시적 `this`로 접근할 수 있습니다:

```kotlin
val sum: Int.(Int) -> Int = { other -> plus(other) }

// 수신자가 있는 익명 함수
val sum = fun Int.(other: Int): Int = this + other
```

### 타입 안전 빌더 예제

```kotlin
class HTML {
    fun body() { ... }
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

html {
    body()  // 수신자의 메서드 호출
}
```
