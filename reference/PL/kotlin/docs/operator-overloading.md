# 연산자 오버로딩

## 개요

Kotlin은 타입에 대해 미리 정의된 연산자의 커스텀 구현을 허용합니다. 연산자는 미리 정의된 기호 표현(예: `+` 또는 `*`)과 우선순위를 가집니다. 연산자를 구현하려면 해당 타입에 특정 이름을 가진 멤버 함수나 확장 함수를 제공합니다.

함수에 `operator` 수정자를 표시합니다:

```kotlin
interface IndexedContainer {
    operator fun get(index: Int)
}
```

연산자 오버로드를 오버라이드할 때 `operator`를 생략할 수 있습니다:

```kotlin
class OrdersList: IndexedContainer {
    override fun get(index: Int) { /*...*/ }
}
```

## 단항 연산

### 단항 전위 연산자

| 표현식 | 변환 |
|--------|------|
| `+a` | `a.unaryPlus()` |
| `-a` | `a.unaryMinus()` |
| `!a` | `a.not()` |

**예제:**
```kotlin
data class Point(val x: Int, val y: Int)

operator fun Point.unaryMinus() = Point(-x, -y)

val point = Point(10, 20)
fun main() {
    println(-point) // "Point(x=-10, y=-20)" 출력
}
```

### 증가와 감소

| 표현식 | 변환 |
|--------|------|
| `a++` | `a.inc()` + 아래 참조 |
| `a--` | `a.dec()` + 아래 참조 |

`inc()`와 `dec()` 함수는 값을 반환해야 하며 객체를 변경해서는 안 됩니다.

**후위 형식 (`a++`):** 초기 값을 반환하고, 증가된 값을 변수에 할당
**전위 형식 (`++a`):** 증가된 값을 할당하고, 새 값을 반환

## 이항 연산

### 산술 연산자

| 표현식 | 변환 |
|--------|------|
| `a + b` | `a.plus(b)` |
| `a - b` | `a.minus(b)` |
| `a * b` | `a.times(b)` |
| `a / b` | `a.div(b)` |
| `a % b` | `a.rem(b)` |
| `a..b` | `a.rangeTo(b)` |
| `a..<b` | `a.rangeUntil(b)` |

**예제:**
```kotlin
data class Counter(val dayIndex: Int) {
    operator fun plus(increment: Int): Counter {
        return Counter(dayIndex + increment)
    }
}
```

### in 연산자

| 표현식 | 변환 |
|--------|------|
| `a in b` | `b.contains(a)` |
| `a !in b` | `!b.contains(a)` |

### 인덱스 접근 연산자

| 표현식 | 변환 |
|--------|------|
| `a[i]` | `a.get(i)` |
| `a[i, j]` | `a.get(i, j)` |
| `a[i] = b` | `a.set(i, b)` |
| `a[i, j] = b` | `a.set(i, j, b)` |

### invoke 연산자

| 표현식 | 변환 |
|--------|------|
| `a()` | `a.invoke()` |
| `a(i)` | `a.invoke(i)` |
| `a(i, j)` | `a.invoke(i, j)` |

### 복합 대입

| 표현식 | 변환 |
|--------|------|
| `a += b` | `a.plusAssign(b)` |
| `a -= b` | `a.minusAssign(b)` |
| `a *= b` | `a.timesAssign(b)` |
| `a /= b` | `a.divAssign(b)` |
| `a %= b` | `a.remAssign(b)` |

**참고:** Kotlin에서 대입은 표현식이 아닙니다.

### 동등성과 부등성 연산자

| 표현식 | 변환 |
|--------|------|
| `a == b` | `a?.equals(b) ?: (b === null)` |
| `a != b` | `!(a?.equals(b) ?: (b === null))` |

- `equals(other: Any?): Boolean`으로만 작동
- `===`와 `!==` (동일성 검사)는 오버로드 불가

### 비교 연산자

| 표현식 | 변환 |
|--------|------|
| `a > b` | `a.compareTo(b) > 0` |
| `a < b` | `a.compareTo(b) < 0` |
| `a >= b` | `a.compareTo(b) >= 0` |
| `a <= b` | `a.compareTo(b) <= 0` |

모든 비교는 `compareTo()`가 `Int`를 반환해야 합니다.

### 프로퍼티 위임 연산자

`provideDelegate`, `getValue`, `setValue` 연산자 함수는 위임된 프로퍼티 섹션에 설명되어 있습니다.

## 명명된 함수를 위한 중위 호출

중위 함수 호출을 사용하여 커스텀 중위 연산을 시뮬레이션할 수 있습니다.
