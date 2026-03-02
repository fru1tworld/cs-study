# Kotlin 불리언

## 개요

Kotlin의 `Boolean` 타입은 두 가지 가능한 값을 가진 불리언 객체를 나타냅니다: `true`와 `false`. 또한 nullable 대응 타입인 `Boolean?`도 있습니다.

**JVM 참고:** JVM에서 불리언은 일반적으로 8비트를 사용하는 원시 `boolean` 타입으로 저장됩니다.

## 내장 연산

Kotlin은 세 가지 주요 불리언 연산을 제공합니다:

| 연산자 | 이름 | 설명 |
|-------|------|------|
| `\|\|` | 논리합 | 논리 OR |
| `&&` | 논리곱 | 논리 AND |
| `!` | 부정 | 논리 NOT |

## 코드 예제

```kotlin
fun main() {
    val myTrue: Boolean = true
    val myFalse: Boolean = false
    val boolNull: Boolean? = null

    println(myTrue || myFalse)  // true
    println(myTrue && myFalse)  // false
    println(!myTrue)             // false
    println(boolNull)            // null
}
```

## 지연 평가

`||`와 `&&` 연산자 모두 **지연 평가**를 사용합니다:

- **`||` (OR):** 첫 번째 피연산자가 `true`이면 두 번째 피연산자는 평가되지 **않습니다**
- **`&&` (AND):** 첫 번째 피연산자가 `false`이면 두 번째 피연산자는 평가되지 **않습니다**

## Nullable 불리언

JVM에서 불리언 객체에 대한 nullable 참조(`Boolean?`)는 [숫자](numbers.html#boxing-and-caching-numbers-on-the-java-virtual-machine)가 처리되는 것과 유사하게 Java 클래스로 박싱됩니다.
