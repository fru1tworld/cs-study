# 표준 입력 읽기

# Kotlin 표준 입력 읽기

> **원문:** https://kotlinlang.org/docs/read-standard-input.html

## 개요

콘솔 애플리케이션에서 사용자 입력을 받으려면 표준 입력(stdin)을 읽어야 합니다. Kotlin은 `readln()`과 `readlnOrNull()` 두 함수로 이를 지원하며, 둘의 차이는 EOF(입력 끝)를 만났을 때 예외를 던지느냐 `null`을 반환하느냐입니다.

## 한 줄 읽기: readln()

`readln()`은 표준 입력에서 한 줄을 문자열로 읽어옵니다. 가장 기본적이고 자주 쓰는 함수입니다.

```kotlin
println("이름을 입력하세요:")
val name = readln()
println("반갑습니다, $name!")
```

주의할 점은 더 이상 읽을 줄이 없을 때(EOF) `RuntimeException`을 던진다는 것입니다. 표준 입력이 파이프나 리다이렉션으로 유한하게 공급되는 환경(예: 파일을 `<` 로 넣거나 파이프로 연결하는 경우)에서 반복문으로 계속 읽다 보면 이 예외를 마주치기 쉽습니다.

## 안전하게 읽기: readlnOrNull()

`readlnOrNull()`은 `readln()`과 동일하게 동작하지만, EOF에 도달하면 예외 대신 `null`을 반환합니다. 입력의 끝을 모르는 상태에서 반복적으로 읽어야 할 때 적합합니다.

```kotlin
val line: String? = readlnOrNull()
if (line == null) {
    println("입력이 더 이상 없습니다.")
}
```

### 여러 줄을 한 번에 모으기

`generateSequence`와 조합하면 EOF까지의 모든 줄을 하나의 시퀀스(또는 문자열)로 모을 수 있습니다. 함수 참조 `::readlnOrNull`을 넘겨서, `null`이 나오는 순간 시퀀스가 자연스럽게 끝나도록 만드는 방식입니다.

```kotlin
val allLines = generateSequence(::readlnOrNull).joinToString("\n")
println(allLines)
```

## 문자열을 숫자·불리언으로 변환하기

`readln()`은 항상 `String`을 반환하므로, 숫자나 불리언으로 다루려면 표준 변환 함수를 사용합니다.

```kotlin
val age: Int = readln().toInt()
val price: Double = readln().toDouble()
val flag: Boolean = readln().toBoolean()
```

`toInt()`, `toDouble()`, `toLong()`, `toFloat()`는 입력값이 형식에 맞지 않으면 `NumberFormatException` 등을 던집니다. 사용자 입력은 언제든 잘못될 수 있으므로, 예외를 원하지 않는다면 `OrNull` 계열 함수를 쓰는 편이 안전합니다.

```kotlin
val maybeAge: Int? = readln().toIntOrNull()
val age = maybeAge ?: run {
    println("숫자가 아닙니다. 기본값 0을 사용합니다.")
    0
}
```

이렇게 `toIntOrNull()`과 엘비스 연산자(`?:`)를 함께 쓰면 예외 처리 없이도 잘못된 입력일 때 쓸 기본값을 깔끔하게 지정할 수 있습니다.

## 한 줄에 여러 값 파싱하기

공백이나 쉼표 등 구분자로 여러 값이 한 줄에 들어오는 경우, `split()`으로 나눈 뒤 `map`으로 원하는 타입으로 변환합니다.

```kotlin
// 입력: "10 20 30"
val numbers = readln().split(' ').map { it.toInt() }
println(numbers)  // [10, 20, 30]

// 입력: "1.5,2.5,3.5"
val doubles = readln().split(',').map { it.toDouble() }
println(doubles)  // [1.5, 2.5, 3.5]
```

연속된 공백까지 처리하려면 정규식을 구분자로 넘길 수도 있습니다.

```kotlin
val tokens = readln().trim().split(Regex("\\s+"))
```

## 입력 방식 정리

| 함수 | EOF에서의 동작 | 안전 여부 |
|---|---|---|
| `readln()` | 예외를 던짐 | 아니오 |
| `readlnOrNull()` | `null` 반환 | 예 |
| `String.toInt()` 등 | 형식 오류 시 예외 | 아니오 |
| `String.toIntOrNull()` 등 | 형식 오류 시 `null` 반환 | 예 |

## 실전 패턴: 줄 수를 모른 채 끝까지 읽기

경쟁 프로그래밍이나 파이프 입력 처리처럼 몇 줄이 들어올지 모르는 상황에서는 `readlnOrNull()`을 루프 조건으로 사용하는 패턴이 유용합니다.

```kotlin
while (true) {
    val line = readlnOrNull() ?: break
    if (line.isEmpty()) continue
    println("처리: $line")
}
```

## 참고

- JVM 환경에서 더 세밀한 입력 제어(토큰 단위 파싱, 버퍼링 등)가 필요하면 `java.util.Scanner`나 `System.\`in\`.bufferedReader()`도 대안이 될 수 있습니다.
- 표준 출력에는 `print()`, `println()`을 사용하며, 서식이 필요하면 문자열 템플릿(`"$name"`, `"${expr}"`)을 활용합니다.
