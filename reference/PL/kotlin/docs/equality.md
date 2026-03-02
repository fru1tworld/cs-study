# Kotlin 동등성

## 개요

Kotlin에는 **두 가지 유형의 동등성**이 있습니다:
- **구조적 동등성 (`==`)** - `equals()` 함수를 확인
- **참조 동등성 (`===`)** - 두 참조가 동일한 객체를 가리키는지 확인

---

## 구조적 동등성

**정의:** 두 객체가 동일한 내용 또는 구조를 가지고 있는지 확인합니다.

**연산자:** `==`와 그 부정 `!=`

**작동 방식:**
`a == b`와 같은 표현식은 다음과 같이 변환됩니다:
```kotlin
a?.equals(b) ?: (b === null)
```

**예제:**
```kotlin
fun main() {
    var a = "hello"
    var b = "hello"
    var c = null
    var d = null
    var e = d

    println(a == b) // true
    println(a == c) // false
    println(c == e) // true
}
```

**핵심 포인트:**
- `equals()` 함수는 기본적으로 `Any` 클래스에서 상속됨
- 기본 구현은 참조 동등성을 제공
- **값 클래스**와 **데이터 클래스**는 구조적 동등성을 위해 자동으로 `equals()`를 재정의
- 비데이터 클래스는 구조적 동등성을 구현하기 위해 수동 재정의 필요
- `null`과의 명시적 비교 (`a == null`)는 자동으로 `a === null`로 변환됨

**사용자 정의 구현:**
```kotlin
class Point(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Point) return false
        // 구조적 동등성을 위해 속성 비교
        return this.x == other.x && this.y == other.y
    }
}
```

**모범 사례:** `equals()`를 재정의할 때 일관성을 위해 `hashCode()`도 재정의하세요.

---

## 참조 동등성

**정의:** 메모리 주소를 확인하여 두 객체가 동일한 인스턴스인지 확인합니다.

**연산자:** `===`와 그 부정 `!==`

**작동 방식:** `a === b`는 `a`와 `b`가 동일한 객체를 가리키는 경우에만 `true`로 평가됩니다.

**예제:**
```kotlin
fun main() {
    var a = "Hello"
    var b = a
    var c = "world"
    var d = "world"

    println(a === b) // true
    println(a === c) // false
    println(c === d) // true
}
```

**참고:** 런타임에 기본 타입 (예: `Int`)의 경우 `===` 동등성은 `==`와 동일합니다.

---

## 부동 소수점 숫자 동등성

**IEEE 754 동작:** 피연산자가 `Float` 또는 `Double`로 정적으로 타입이 지정되면 IEEE 754 표준을 따릅니다.

**비정적 타이핑:** 부동 소수점으로 정적 타입이 지정되지 않으면 구조적 동등성이 적용됩니다:
- `NaN`은 자기 자신과 같음
- `NaN`은 다른 모든 요소보다 큼 (`POSITIVE_INFINITY` 포함)
- `-0.0`은 `0.0`과 같지 **않음**

---

## 배열 동등성

두 배열이 같은 순서로 같은 요소를 가지고 있는지 비교하려면 다음을 사용합니다:
```kotlin
contentEquals()
```

---

## 관련 주제
- [널 안전성](null-safety.md)
- [제네릭: in, out, where](generics.md)
- [부동 소수점 숫자 비교](numbers.md#floating-point-numbers-comparison)
- [배열 비교](arrays.md#compare-arrays)
