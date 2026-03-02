# Kotlin 투어: Hello World

## 개요

이것은 기본 문법, 변수, 문자열 템플릿을 다루는 Kotlin 초보자 가이드입니다.

---

## 핵심 개념

### 함수

```kotlin
fun main() {
    println("Hello, world!")
}
```

**핵심 사항:**
- `fun`은 함수를 선언합니다
- `main()`은 프로그램 진입점입니다
- 함수 본문은 중괄호 `{}`로 묶습니다
- `println()`은 줄 바꿈과 함께 출력합니다
- `print()`는 줄 바꿈 없이 출력합니다

---

## 변수

Kotlin은 두 가지 유형의 변수를 지원합니다:

### 읽기 전용 변수 (`val`)

```kotlin
val popcorn = 5  // 변경할 수 없음
```

### 가변 변수 (`var`)

```kotlin
var customers = 10
customers = 8  // 재할당 가능
```

**모범 사례:** 기본적으로 모든 변수를 `val`로 선언하세요. 필요한 경우에만 `var`를 사용하세요.

---

## 문자열 템플릿

문자열 템플릿을 사용하면 `$`를 사용하여 변수와 표현식을 문자열에 포함할 수 있습니다:

### 단순 변수 접근

```kotlin
val customers = 10
println("There are $customers customers")
// 출력: There are 10 customers
```

### 표현식 평가

```kotlin
val customers = 10
println("There are ${customers + 1} customers")
// 출력: There are 11 customers
```

---

## 연습 문제

**과제:** 코드를 완성하여 "Mary is 20 years old"를 출력하세요

**해답:**

```kotlin
fun main() {
    val name = "Mary"
    val age = 20
    println("$name is $age years old")
}
```

---

## 추가 참고사항

- Kotlin은 **타입 추론**을 사용합니다 - 타입을 명시적으로 선언할 필요가 없습니다
- 위 예제에서 `age`는 `Int` 타입으로 추론됩니다
- 다음 주제: [기본 타입](kotlin-tour-basic-types.html)
