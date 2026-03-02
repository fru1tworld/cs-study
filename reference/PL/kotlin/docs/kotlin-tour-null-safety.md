# Kotlin 투어: null 안전성

## 개요

Kotlin은 런타임이 아닌 컴파일 시점에 잠재적인 `null` 값 문제를 감지하는 내장 null 안전성 기능을 가지고 있습니다. 이는 일반적인 null 참조 오류를 방지합니다.

## null 안전성의 주요 기능

- `null` 값이 허용되는 경우 명시적으로 선언
- `null` 값 확인
- 잠재적으로 null인 프로퍼티/함수에 대한 안전 호출 사용
- 감지된 `null` 값에 대한 대체 동작 선언

---

## Nullable 타입

기본적으로 타입은 `null` 값을 보유할 수 없습니다. `null`을 허용하려면 타입 뒤에 `?`를 추가하세요:

```kotlin
fun main() {
    // Non-nullable String
    var neverNull: String = "This can't be null"
    neverNull = null  // 컴파일러 오류

    // Nullable String
    var nullable: String? = "You can keep a null here"
    nullable = null   // OK

    // 타입 추론은 non-nullable을 가정
    var inferredNonNull = "The compiler assumes non-nullable"
    inferredNonNull = null  // 컴파일러 오류

    fun strLength(notNull: String): Int = notNull.length
    println(strLength(neverNull))  // 18
    println(strLength(nullable))   // 컴파일러 오류
}
```

---

## null 값 확인

조건부 표현식을 사용하여 `null`을 확인:

```kotlin
fun describeString(maybeString: String?): String {
    if (maybeString != null && maybeString.length > 0) {
        return "String of length ${maybeString.length}"
    } else {
        return "Empty or null string"
    }
}

fun main() {
    val nullString: String? = null
    println(describeString(nullString))  // Empty or null string
}
```

---

## 안전 호출 연산자 (`?.`)

객체나 접근된 프로퍼티가 `null`이면 `null`을 반환합니다. 잠재적으로 null인 값에 접근할 때 오류를 방지합니다:

```kotlin
fun lengthString(maybeString: String?): Int? = maybeString?.length

fun main() {
    val nullString: String? = null
    println(lengthString(nullString))  // null
}
```

### 안전 호출 체이닝

```kotlin
person.company?.address?.country
```

### 안전 함수 호출

```kotlin
fun main() {
    val nullString: String? = null
    println(nullString?.uppercase())  // null (함수가 호출되지 않음)
}
```

---

## 엘비스 연산자 (`?:`)

왼쪽이 `null`인 경우 기본값을 제공합니다:

```kotlin
fun main() {
    val nullString: String? = null
    println(nullString?.length ?: 0)  // 0
}
```

**문법:** `표현식 ?: 기본값`

---

## 연습 문제

**과제:** 직원의 급여를 반환하거나 찾지 못하면 `0`을 반환하는 함수를 작성하세요.

```kotlin
data class Employee(val name: String, var salary: Int)

fun employeeById(id: Int) = when(id) {
    1 -> Employee("Mary", 20)
    2 -> null
    3 -> Employee("John", 21)
    4 -> Employee("Ann", 23)
    else -> null
}

fun salaryById(id: Int) = employeeById(id)?.salary ?: 0

fun main() {
    println((1..5).sumOf { id -> salaryById(id) })  // 64
}
```

---

## 요약

| 기능 | 목적 |
|------|------|
| `타입?` | nullable 타입 선언 |
| `!= null` | 조건문에서 null 확인 |
| `?.` | 안전 호출 연산자 |
| `?:` | 엘비스 연산자 (기본값 제공) |
