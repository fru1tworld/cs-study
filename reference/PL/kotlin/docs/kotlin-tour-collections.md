# Kotlin 투어: 컬렉션

## 개요

컬렉션은 나중에 처리하기 위해 데이터를 그룹화하는 데 사용되는 구조입니다. Kotlin은 세 가지 주요 컬렉션 타입을 제공합니다: **List**, **Set**, **Map**. 각각은 가변 또는 읽기 전용일 수 있습니다.

| 컬렉션 타입 | 설명 |
|------------|------|
| **List** | 순서가 있는 항목 컬렉션 |
| **Set** | 고유한 비정렬 항목 컬렉션 |
| **Map** | 키가 고유한 키-값 쌍의 집합 |

---

## List

**목적:** 순서대로 항목을 저장하고 중복을 허용합니다.

### 리스트 생성

**읽기 전용 리스트** `listOf()` 사용:

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println(readOnlyShapes) // [triangle, square, circle]
```

**가변 리스트** 명시적 타입과 함께 `mutableListOf()` 사용:

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
println(shapes) // [triangle, square, circle]
```

**가변 리스트의 읽기 전용 뷰:**

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
val shapesLocked: List<String> = shapes
```

### 리스트 항목 접근

**인덱스 접근 연산자 `[]` 사용:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("The first item in the list is: ${readOnlyShapes[0]}")
// The first item in the list is: triangle
```

**`.first()`와 `.last()` 함수 사용:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("The first item in the list is: ${readOnlyShapes.first()}")
// The first item in the list is: triangle
```

### 리스트 연산

**개수 얻기:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("This list has ${readOnlyShapes.count()} items") // This list has 3 items
```

**`in` 연산자로 항목 존재 확인:**

```kotlin
val readOnlyShapes = listOf("triangle", "square", "circle")
println("circle" in readOnlyShapes) // true
```

**가변 리스트에서 항목 추가/제거:**

```kotlin
val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
shapes.add("pentagon")
println(shapes) // [triangle, square, circle, pentagon]
shapes.remove("pentagon")
println(shapes) // [triangle, square, circle]
```

---

## Set

**목적:** 고유하고 비정렬된 항목을 저장합니다.

### 세트 생성

**읽기 전용 세트** `setOf()` 사용:

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
// 중복은 자동으로 제거됨
println(readOnlyFruit) // [apple, banana, cherry]
```

**명시적 타입을 가진 가변 세트:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
println(fruit) // [apple, banana, cherry]
```

**가변 세트의 읽기 전용 뷰:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
val fruitLocked: Set<String> = fruit
```

### 세트 연산

**개수 얻기:**

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
println("This set has ${readOnlyFruit.count()} items") // This set has 3 items
```

**항목 존재 확인:**

```kotlin
val readOnlyFruit = setOf("apple", "banana", "cherry", "cherry")
println("banana" in readOnlyFruit) // true
```

**가변 세트에서 항목 추가/제거:**

```kotlin
val fruit: MutableSet<String> = mutableSetOf("apple", "banana", "cherry", "cherry")
fruit.add("dragonfruit")
println(fruit) // [apple, banana, cherry, dragonfruit]
fruit.remove("dragonfruit")
println(fruit) // [apple, banana, cherry]
```

---

## Map

**목적:** 각 키가 고유하고 정확히 하나의 값에 매핑되는 키-값 쌍을 저장합니다.

**주요 특성:**
- 모든 키는 고유해야 함
- 중복 값은 허용됨
- 키를 참조하여 값에 접근

### 맵 생성

**읽기 전용 맵** `mapOf()` 사용:

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu) // {apple=100, kiwi=190, orange=100}
```

**명시적 타입 선언을 가진 가변 맵:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(juiceMenu) // {apple=100, kiwi=190, orange=100}
```

**가변 맵의 읽기 전용 뷰:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
val juiceMenuLocked: Map<String, Int> = juiceMenu
```

### 맵 값 접근

**키와 함께 인덱스 접근 연산자 사용:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("The value of apple juice is: ${readOnlyJuiceMenu["apple"]}")
// The value of apple juice is: 100
```

**존재하지 않는 키에 접근하면 `null` 반환:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("The value of pineapple juice is: ${readOnlyJuiceMenu["pineapple"]}")
// The value of pineapple juice is: null
```

### 맵 연산

**인덱스 접근 연산자로 항목 추가:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
juiceMenu["coconut"] = 150
println(juiceMenu) // {apple=100, kiwi=190, orange=100, coconut=150}
```

**항목 제거:**

```kotlin
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
juiceMenu.remove("orange")
println(juiceMenu) // {apple=100, kiwi=190}
```

**개수 얻기:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("This map has ${readOnlyJuiceMenu.count()} key-value pairs") // This map has 3 key-value pairs
```

**`.containsKey()`로 키 존재 확인:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu.containsKey("kiwi")) // true
```

**키와 값 프로퍼티 얻기:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println(readOnlyJuiceMenu.keys) // [apple, kiwi, orange]
println(readOnlyJuiceMenu.values) // [100, 190, 100]
```

**`in` 연산자로 키 또는 값 존재 확인:**

```kotlin
val readOnlyJuiceMenu = mapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
println("orange" in readOnlyJuiceMenu.keys) // true
println("orange" in readOnlyJuiceMenu) // true (대안 구문)
println(200 in readOnlyJuiceMenu.values) // false
```

---

## 연습 문제

### 연습 1: 총 숫자 세기

**과제:** 두 리스트의 총 숫자를 세세요.

```kotlin
// 해답
fun main() {
    val greenNumbers = listOf(1, 4, 23)
    val redNumbers = listOf(17, 2)
    val totalCount = greenNumbers.count() + redNumbers.count()
    println(totalCount)
}
```

### 연습 2: 프로토콜 지원 확인

**과제:** 요청된 프로토콜이 지원되는지 확인하세요 (대소문자 무시).

```kotlin
// 해답
fun main() {
    val SUPPORTED = setOf("HTTP", "HTTPS", "FTP")
    val requested = "smtp"
    val isSupported = requested.uppercase() in SUPPORTED
    println("Support for $requested: $isSupported")
}
```

**힌트:** 확인 전에 `.uppercase()` 함수를 사용하여 대문자로 변환하세요.

### 연습 3: 숫자를 단어로 매핑

**과제:** 숫자 1-3을 그 철자에 연관시키는 맵을 정의하세요.

```kotlin
// 해답
fun main() {
    val number2word = mapOf(1 to "one", 2 to "two", 3 to "three")
    val n = 2
    println("$n is spelt as '${number2word[n]}'")
}
```

---

## 핵심 개념

- **확장 함수:** 점 표기법을 사용하여 객체에서 호출되는 함수 (예: `.first()`, `.count()`)
- **프로퍼티:** 점 표기법을 사용하여 객체에서 접근되는 데이터 (예: `.keys`, `.values`)
- **타입 추론:** Kotlin이 컬렉션 요소 타입을 자동으로 추론
- **null 안전성:** 맵에서 존재하지 않는 키에 접근하면 `null` 반환

---

## 다음 단계

[제어 흐름](kotlin-tour-control-flow.html)
