# 수학 함수와 상수

# Kotlin 수학 함수와 상수 (kotlin.math)

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.math/

## 개요

`kotlin.math` 패키지는 삼각함수, 지수·로그 함수, 반올림, 절댓값, 최솟값·최댓값 등 수치 계산에 필요한 함수와 상수를 모아 둔 표준 라이브러리 패키지입니다. 대부분의 함수는 `Double`과 `Float` 양쪽에 오버로드되어 있고, 일부는 `Int`, `Long`, `UInt`, `ULong`까지 지원합니다. 모든 함수는 공통(Common) 코드에서 사용할 수 있어 JVM, JS, Native, Wasm 어디서든 동일하게 동작합니다.

## 상수: PI, E

원주율과 자연로그의 밑을 `Double` 상수로 제공합니다.

```kotlin
println(kotlin.math.PI)  // 3.141592653589793
println(kotlin.math.E)   // 2.718281828459045

val circumference = 2 * PI * radius
```

## 삼각함수

각도는 라디안 단위입니다. `sin`, `cos`, `tan`과 그 역함수 `asin`, `acos`, `atan`, 그리고 두 인자를 받아 사분면을 구분하는 `atan2`가 있습니다.

```kotlin
val angle = PI / 6
println(sin(angle))       // 0.5
println(atan2(1.0, 1.0))  // PI / 4, 사분면을 고려한 각도
```

- `asin`, `atan`은 `[-PI/2, PI/2]` 범위의 각도를 반환합니다.
- `acos`는 `[0, PI]` 범위를 반환합니다.
- `atan2(y, x)`는 좌표 `(x, y)`가 이루는 각도를 `[-PI, PI]` 범위로 반환하며, `atan(y/x)`와 달리 사분면 정보를 잃지 않습니다.

## 쌍곡선 함수

`sinh`, `cosh`, `tanh`와 역함수 `asinh`, `acosh`, `atanh`가 동일한 패턴으로 제공됩니다.

```kotlin
println(sinh(1.0))   // 1.1752...
println(tanh(0.0))   // 0.0
```

## 지수·로그 함수

```kotlin
println(exp(1.0))          // e^1 = E
println(ln(E))             // 1.0
println(log10(1000.0))     // 3.0
println(log2(8.0))         // 3.0
println(log(8.0, 2.0))     // 임의의 밑(base)에 대한 로그
```

작은 값을 다룰 때는 정밀도 손실을 줄인 전용 함수를 사용합니다.

- `expm1(x)`: `exp(x) - 1`과 같지만 `x`가 0에 가까울 때 오차가 훨씬 작습니다.
- `ln1p(x)`: `ln(x + 1)`과 같지만 마찬가지로 작은 `x`에서 정밀합니다.

## 거듭제곱과 제곱근

`pow`는 `Double`/`Float`의 확장 함수로, 지수가 실수인 버전과 정수인 버전이 모두 있습니다.

```kotlin
println(2.0.pow(10))     // 1024.0, 지수가 Int
println(2.0.pow(0.5))    // 1.414..., 지수가 Double, sqrt(2)와 동일
println(sqrt(16.0))      // 4.0
println(cbrt(27.0))      // 3.0, 세제곱근
```

빗변 길이를 구하는 `hypot(x, y)`는 `sqrt(x*x + y*y)`와 결과가 같지만 중간 계산에서 오버플로가 발생하지 않도록 구현되어 있습니다.

```kotlin
println(hypot(3.0, 4.0))  // 5.0
```

## 절댓값과 부호

`abs`는 `Double`, `Float`, `Int`, `Long`을 모두 지원하는 최상위 함수이고, 같은 역할을 하는 `absoluteValue` 확장 프로퍼티도 있습니다. 둘 중 코드 스타일에 맞는 쪽을 쓰면 됩니다.

```kotlin
println(abs(-5))            // 5
println((-5).absoluteValue) // 5
```

부호만 필요하면 `sign`을 사용합니다. `Double`/`Float`에서는 `1.0`, `-1.0`, `0.0`을, `Int`/`Long`에서는 `1`, `-1`, `0`을 반환합니다.

```kotlin
println((-42.0).sign)  // -1.0
println(0.sign)        // 0
```

## 반올림 함수

용도에 따라 네 가지 반올림 방식이 나뉩니다.

- `round(x)`: 가장 가까운 정수로. 정확히 중간값이면 짝수 쪽으로 반올림합니다(round half to even).
- `ceil(x)`: 양의 무한대 방향으로 올림.
- `floor(x)`: 음의 무한대 방향으로 내림.
- `truncate(x)`: 소수부를 버리고 0 방향으로 자름.

```kotlin
println(round(2.5))     // 2.0, 짝수로 반올림
println(round(3.5))     // 4.0
println(ceil(2.1))      // 3.0
println(floor(2.9))     // 2.0
println(truncate(-2.9)) // -2.0
```

`Int`/`Long` 값이 바로 필요하면 `roundToInt()`, `roundToLong()`을 사용합니다. 이쪽은 `round`와 달리 중간값을 항상 양의 무한대 방향으로 반올림합니다.

```kotlin
println(2.5.roundToInt())  // 3
```

## 최솟값·최댓값

`min`, `max`는 `Double`, `Float`, `Int`, `Long`은 물론 Kotlin 1.5부터 `UInt`, `ULong`까지 지원하는 두 값 비교 함수입니다.

```kotlin
println(min(3, 7))   // 3
println(max(3.0, 7.0))  // 7.0
```

셋 이상을 비교하려면 `maxOf`/`minOf`(가변 인자를 받는 별도 함수) 또는 컬렉션의 `maxOrNull()` 계열을 사용하는 편이 낫습니다.

## 부동소수점 정밀 조작

부동소수점 표현의 미묘한 차이를 다뤄야 할 때 쓰는 함수들입니다. `nextUp`, `nextDown`, `nextTowards`, `ulp`는 `Double`이면 공통(Common) 코드에서도 그대로 쓸 수 있고, `Float` 버전만 플랫폼에 따라 지원 범위가 다릅니다. `IEEErem`은 예외적으로 `Double`/`Float` 모두 JVM과 Native에서만 제공됩니다.

- `IEEErem(divisor)`: IEEE 754 표준의 나머지 연산으로, `%` 연산자와 반올림 방식이 다릅니다. (JVM/Native 전용)
- `nextUp()` / `nextDown()`: 표현 가능한 다음 큰 값 / 작은 값을 반환합니다.
- `nextTowards(to)`: 지정한 값 방향으로 한 걸음 이동한 표현 가능한 값을 반환합니다.
- `ulp`: unit in the last place, 즉 해당 값 바로 다음 표현 가능한 값과의 차이입니다.

```kotlin
println(1.0.nextUp())    // 1.0보다 아주 조금 큰 표현 가능한 값
println(1.0.ulp)         // 1.0 근방에서 표현 가능한 최소 간격
```

이 그룹의 함수는 부동소수점 오차를 정밀하게 다뤄야 하는 수치 알고리즘이 아니라면 실무에서 자주 쓰이지는 않습니다.

## 요약

| 분류 | 대표 함수 |
|---|---|
| 상수 | `PI`, `E` |
| 삼각함수 | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2` |
| 쌍곡선 함수 | `sinh`, `cosh`, `tanh`, `asinh`, `acosh`, `atanh` |
| 지수·로그 | `exp`, `expm1`, `ln`, `ln1p`, `log10`, `log2`, `log` |
| 거듭제곱·제곱근 | `pow`, `sqrt`, `cbrt`, `hypot` |
| 절댓값·부호 | `abs`, `absoluteValue`, `sign` |
| 반올림 | `round`, `ceil`, `floor`, `truncate`, `roundToInt`, `roundToLong` |
| 최솟값·최댓값 | `min`, `max` |
| 부동소수점 정밀 조작 | `IEEErem`(JVM/Native), `nextUp`, `nextDown`, `nextTowards`, `ulp` |
