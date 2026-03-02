# 구조 분해 선언

## 개요

구조 분해 선언을 사용하면 객체를 한 번에 여러 변수로 분해할 수 있습니다:

```kotlin
val (name, age) = person
```

이것은 독립적으로 사용할 수 있는 두 개의 새 변수 (`name`과 `age`)를 생성합니다.

## 작동 방식

구조 분해 선언은 `componentN()` 함수 호출로 컴파일됩니다:

```kotlin
val name = person.component1()
val age = person.component2()
```

**핵심 포인트:**
- `componentN()` 함수는 `operator` 키워드로 표시되어야 함
- 필요한 component 함수가 있으면 모든 객체를 구조 분해할 수 있음
- `component3()`, `component4()` 등과 함께 작동

## 사용 사례

### 1. 함수에서 여러 값 반환

```kotlin
data class Result(val result: Int, val status: Status)

fun function(...): Result {
    // 계산
    return Result(result, status)
}

// 사용법
val (result, status) = function(...)
```

데이터 클래스는 자동으로 `componentN()` 함수를 선언합니다.

### 2. Map 구조 분해

```kotlin
for ((key, value) in map) {
    // key와 value로 무언가 수행
}
```

표준 라이브러리는 확장을 제공합니다:
```kotlin
operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>>
operator fun <K, V> Map.Entry<K, V>.component1() = getKey()
operator fun <K, V> Map.Entry<K, V>.component2() = getValue()
```

### 3. For 루프

```kotlin
for ((a, b) in collection) { ... }
```

변수들은 각 요소의 `component1()`과 `component2()`에서 값을 얻습니다.

## 사용하지 않는 변수에 밑줄 사용

사용하지 않는 변수는 밑줄로 건너뜁니다:

```kotlin
val (_, status) = getResult()
```

건너뛴 컴포넌트에 대해서는 `componentN()` 함수가 호출되지 않습니다.

## 람다에서 구조 분해

### 기본 구문

```kotlin
// 대신:
map.mapValues { entry -> "${entry.value}!" }

// 구조 분해 사용:
map.mapValues { (key, value) -> "$value!" }
```

### 매개변수 변형

```kotlin
{ a -> ... }           // 하나의 매개변수
{ a, b -> ... }        // 두 매개변수
{ (a, b) -> ... }      // 구조 분해된 쌍
{ (a, b), c -> ... }   // 구조 분해된 쌍 + 다른 매개변수
```

### 람다에서 사용하지 않는 컴포넌트

```kotlin
map.mapValues { (_, value) -> "$value!" }
```

### 타입 명세

```kotlin
// 전체 매개변수에 대한 타입:
map.mapValues { (_,value): Map.Entry<Int, String> -> "$value!" }

// 특정 컴포넌트에 대한 타입:
map.mapValues { (_,value: String) -> "$value!" }
```
