# Kotlin 배열

## 개요

배열은 같은 타입 또는 그 하위 타입의 고정된 수의 값을 보유하는 데이터 구조입니다. 가장 일반적인 타입은 `Array` 클래스입니다. 객체 타입 배열의 원시 값은 객체로 박싱되어 성능에 영향을 미칩니다. 박싱 오버헤드를 피하려면 원시 타입 배열을 대신 사용하세요.

---

## 배열을 사용해야 할 때

**배열을 사용하는 경우:**
- 특수한 저수준 요구 사항이 있는 경우
- 일반 애플리케이션을 넘어서는 성능 요구 사항이 있는 경우
- 사용자 정의 데이터 구조를 구축해야 하는 경우

**그 외의 경우 컬렉션을 사용하세요.** 컬렉션은 다음을 제공합니다:
- 견고하고 의도가 명확한 코드를 위한 읽기 전용 변형
- 쉬운 요소 추가/제거 (배열은 고정 크기)
- `==` 연산자로 구조적 동등성 검사

**배열 수정의 비효율성 예제:**

```kotlin
fun main() {
    var riversArray = arrayOf("Nile", "Amazon", "Yangtze")
    // 새 배열을 생성하고, 요소를 복사하고, "Mississippi"를 추가
    riversArray += "Mississippi"
    println(riversArray.joinToString()) // Nile, Amazon, Yangtze, Mississippi
}
```

---

## 배열 생성

### 생성 함수

**`arrayOf()`** - 값으로 배열 생성:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    println(simpleArray.joinToString()) // 1, 2, 3
}
```

**`arrayOfNulls()`** - null로 채워진 배열 생성:

```kotlin
fun main() {
    val nullArray: Array<Int?> = arrayOfNulls(3)
    println(nullArray.joinToString()) // null, null, null
}
```

**`emptyArray()`** - 빈 배열 생성:

```kotlin
var exampleArray = emptyArray<String>()
// 타입은 어느 쪽에서든 지정할 수 있음
var exampleArray: Array<String> = emptyArray()
```

### 배열 생성자

크기와 초기화 함수를 받습니다:

```kotlin
fun main() {
    // 0으로 채워진 Array<Int> 생성 [0, 0, 0]
    val initArray = Array<Int>(3) { 0 }
    println(initArray.joinToString()) // 0, 0, 0

    // ["0", "1", "4", "9", "16"]의 Array<String> 생성
    val asc = Array(5) { i -> (i * i).toString() }
    asc.forEach { print(it) } // 014916
}
```

**참고:** Kotlin에서 인덱스는 0부터 시작합니다.

### 중첩 배열

배열은 다차원일 수 있습니다:

```kotlin
fun main() {
    // 2차원 배열
    val twoDArray = Array(2) { Array<Int>(2) { 0 } }
    println(twoDArray.contentDeepToString()) // [[0, 0], [0, 0]]

    // 3차원 배열
    val threeDArray = Array(3) { Array(3) { Array<Int>(3) { 0 } } }
    println(threeDArray.contentDeepToString())
    // [[[0, 0, 0], [0, 0, 0], [0, 0, 0]], [[0, 0, 0], [0, 0, 0], [0, 0, 0]], [[0, 0, 0], [0, 0, 0], [0, 0, 0]]]
}
```

**참고:** 중첩 배열은 같은 타입이나 크기일 필요가 없습니다.

---

## 요소 접근 및 수정

인덱스 접근 연산자 `[]`를 사용합니다:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    val twoDArray = Array(2) { Array<Int>(2) { 0 } }

    // 요소 수정
    simpleArray[0] = 10
    twoDArray[0][0] = 2

    println(simpleArray[0].toString()) // 10
    println(twoDArray[0][0].toString()) // 2
}
```

**불변성:** Kotlin은 런타임 실패를 방지하기 위해 `Array<String>`을 `Array<Any>`에 할당하는 것을 허용하지 않습니다. 대신 `Array<out Any>`를 사용하세요 (타입 프로젝션).

---

## 배열 작업

### 가변 인수 전달 (varargs)

스프레드 연산자 `*`를 사용합니다:

```kotlin
fun main() {
    val lettersArray = arrayOf("c", "d")
    printAllStrings("a", "b", *lettersArray) // abcd
}

fun printAllStrings(vararg strings: String) {
    for (string in strings) {
        print(string)
    }
}
```

### 배열 비교

`.contentEquals()`와 `.contentDeepEquals()`를 사용합니다:

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    val anotherArray = arrayOf(1, 2, 3)

    println(simpleArray.contentEquals(anotherArray)) // true

    simpleArray[0] = 10
    println(simpleArray contentEquals anotherArray) // false (중위 표기법)
}
```

**주의: `==`나 `!=`를 사용하지 마세요** - 내용 동등성이 아닌 객체 동일성을 검사합니다.

### 배열 변환

#### 합계

```kotlin
fun main() {
    val sumArray = arrayOf(1, 2, 3)
    println(sumArray.sum()) // 6
}
```

**참고:** 숫자 데이터 타입(Int 등)에서만 작동합니다.

#### 셔플

```kotlin
fun main() {
    val simpleArray = arrayOf(1, 2, 3)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [3, 2, 1] (무작위)
    simpleArray.shuffle()
    println(simpleArray.joinToString()) // [2, 3, 1] (무작위)
}
```

### 배열을 컬렉션으로 변환

#### 리스트 또는 세트로 변환

```kotlin
fun main() {
    val simpleArray = arrayOf("a", "b", "c", "c")

    println(simpleArray.toSet()) // [a, b, c]
    println(simpleArray.toList()) // [a, b, c, c]
}
```

#### 맵으로 변환

`.toMap()`을 사용하여 `Array<Pair<K,V>>`만 맵으로 변환할 수 있습니다:

```kotlin
fun main() {
    val pairArray = arrayOf(
        "apple" to 120,
        "banana" to 150,
        "cherry" to 90,
        "apple" to 140
    )

    println(pairArray.toMap())
    // {apple=140, banana=150, cherry=90}
    // 참고: 중복 키는 최신 값을 사용
}
```

---

## 원시 타입 배열

박싱 오버헤드를 피하려면 원시값과 함께 `Array` 대신 원시 타입 배열을 사용하세요:

| Kotlin 타입 | Java 동등물 |
|------------|------------|
| `BooleanArray` | `boolean[]` |
| `ByteArray` | `byte[]` |
| `CharArray` | `char[]` |
| `DoubleArray` | `double[]` |
| `FloatArray` | `float[]` |
| `IntArray` | `int[]` |
| `LongArray` | `long[]` |
| `ShortArray` | `short[]` |

**예제:**

```kotlin
fun main() {
    val exampleArray = IntArray(5)
    println(exampleArray.joinToString()) // 0, 0, 0, 0, 0
}
```

### 변환

- **원시에서 객체로:** `.toTypedArray()`
- **객체에서 원시로:** `.toBooleanArray()`, `.toByteArray()`, `.toCharArray()` 등

---

## 다음 단계

- [컬렉션 개요](collections-overview.html) - 컬렉션이 권장되는 이유 알아보기
- [기본 타입](types-overview.html) - 다른 기본 타입 탐색
- [Java에서 Kotlin으로 컬렉션 가이드](java-to-kotlin-collections-guide.html) - Java 개발자용
