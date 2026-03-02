# Kotlin 투어: 기본 타입

## 개요

Kotlin의 모든 변수와 데이터 구조는 타입을 가집니다. 타입은 컴파일러에게 해당 변수나 데이터 구조에서 어떤 연산이 허용되는지 알려줍니다. 즉, 어떤 함수와 프로퍼티를 가지고 있는지를 나타냅니다.

## 타입 추론

Kotlin은 **타입 추론**을 사용하여 할당된 값을 기반으로 변수의 타입을 자동으로 결정합니다. 예를 들어:

```kotlin
var customers = 10  // Kotlin이 타입을 Int로 추론
```

## 산술 연산 예제

```kotlin
fun main() {
    var customers = 10
    customers = 8
    customers = customers + 3      // 덧셈: 11
    customers += 7                 // 복합 대입: 18
    customers -= 3                 // 뺄셈: 15
    customers *= 2                 // 곱셈: 30
    customers /= 3                 // 나눗셈: 10
    println(customers)             // 10
}
```

**복합 대입 연산자:** `+=`, `-=`, `*=`, `/=`, `%=`

## Kotlin의 기본 타입

| 범주 | 타입 | 예제 |
|------|------|------|
| **정수** | `Byte`, `Short`, `Int`, `Long` | `val year: Int = 2020` |
| **부호 없는 정수** | `UByte`, `UShort`, `UInt`, `ULong` | `val score: UInt = 100u` |
| **부동 소수점** | `Float`, `Double` | `val temp: Float = 24.5f`, `val price: Double = 19.99` |
| **불리언** | `Boolean` | `val isEnabled: Boolean = true` |
| **문자** | `Char` | `val separator: Char = ','` |
| **문자열** | `String` | `val message: String = "Hello, world!"` |

## 초기화 없이 변수 선언

변수는 즉시 초기화하지 않고 선언할 수 있지만, 첫 번째 사용 전에 반드시 초기화해야 합니다:

```kotlin
fun main() {
    val d: Int          // 초기화 없이 선언
    d = 3               // 초기화
    val e: String = "hello"
    println(d)          // 3
    println(e)          // hello
}
```

## 중요한 규칙

초기화되지 않은 변수를 읽으면 컴파일러 오류가 발생합니다:

```kotlin
val d: Int
println(d)  // 오류: 변수 'd'가 초기화되어야 합니다
```

## 연습 문제

**과제:** 각 변수에 올바른 타입을 명시적으로 선언하세요:

```kotlin
fun main() {
    val a: Int = 1000
    val b: String = "log message"
    val c: Double = 3.14
    val d: Long = 100_000_000_000_000
    val e: Boolean = false
    val f: Char = '\n'
}
```

## 다음 단계

[컬렉션](kotlin-tour-collections.html)
