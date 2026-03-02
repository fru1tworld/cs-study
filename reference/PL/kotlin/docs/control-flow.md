# Kotlin 조건문과 루프

## 개요

Kotlin은 `if`, `when`, 그리고 다양한 루프 구조를 사용하여 프로그램 흐름을 제어하는 유연한 도구를 제공합니다.

---

## if 표현식

### 기본 사용법

괄호 `()` 안에 조건을, 중괄호 `{}` 안에 동작을 사용합니다:

```kotlin
if (heightAlice < heightBob) {
    taller = heightBob
}
```

### 표현식으로서의 if

`if`는 값을 직접 할당하는 표현식으로 사용할 수 있습니다 (삼항 연산자를 대체):

```kotlin
val taller = if (heightAlice > heightBob) heightAlice else heightBob
```

### else if 체인

```kotlin
val heightOrLimit = if (heightLimit > heightAlice) heightLimit
                    else if (heightAlice > heightBob) heightAlice
                    else heightBob
```

### 블록 표현식

블록의 마지막 표현식이 결과가 됩니다:

```kotlin
val taller = if (heightAlice > heightBob) {
    print("Choose Alice\n")
    heightAlice
} else {
    print("Choose Bob\n")
    heightBob
}
```

---

## when 표현식과 문

### 주제가 있는 기본 when

```kotlin
val userRole = "Editor"
when (userRole) {
    "Viewer" -> print("User has read-only access")
    "Editor" -> print("User can edit content")
    else -> print("User role is not recognized")
}
```

### 표현식 vs 문으로서의 when

**표현식** (값 반환):

```kotlin
val text = when (x) {
    1 -> "x == 1"
    2 -> "x == 2"
    else -> "x is neither 1 nor 2"
}
```

**문** (동작 수행):

```kotlin
when (x) {
    1 -> print("x == 1")
    2 -> print("x == 2")
    else -> print("x is neither 1 nor 2")
}
```

### 주제 없는 when

```kotlin
val message = when {
    localFileSize > remoteFileSize -> "Local file is larger"
    localFileSize < remoteFileSize -> "Local file is smaller"
    else -> "Files are the same size"
}
```

### 완전성

- **문**: 모든 경우를 다룰 필요 없음
- **표현식**: 모든 경우를 다뤄야 함 (그렇지 않으면 컴파일러가 오류 발생)
- **예외**: 열거형, Boolean, 또는 sealed 클래스는 모든 경우가 다뤄지면 `else`가 필요 없음

### 여러 조건 (쉼표로 구분)

```kotlin
when (ticketPriority) {
    "Low", "Medium" -> print("Standard response time")
    else -> print("High-priority handling")
}
```

### 조건으로 표현식 사용

```kotlin
when (enteredPin) {
    storedPin.toInt() -> print("PIN is correct")
    else -> print("Incorrect PIN")
}
```

### 범위와 컬렉션 검사

```kotlin
when (x) {
    in 1..10 -> print("x is in the range")
    in validNumbers -> print("x is valid")
    !in 10..20 -> print("x is outside the range")
    else -> print("none of the above")
}
```

### 타입 검사

```kotlin
fun hasPrefix(input: Any): Boolean = when (input) {
    is String -> input.startsWith("ID-")
    else -> false
}
```

### 변수에 주제 캡처

```kotlin
val message = when (val input = "yes") {
    "yes" -> "You said yes"
    "no" -> "You said no"
    else -> "Unrecognized input: $input"
}
```

### 가드 조건

가드 조건은 기본 조건 후에 추가 검사를 추가합니다:

```kotlin
when (animal) {
    is Animal.Dog -> feedDog()
    is Animal.Cat if !animal.mouseHunter -> feedCat()
    else -> println("Unknown animal")
}
```

불리언 연산자를 사용한 여러 조건:

```kotlin
when (animal) {
    is Animal.Cat if (!animal.mouseHunter && animal.hungry) -> feedCat()
}
```

`else if`와 함께:

```kotlin
when (animal) {
    is Animal.Dog -> feedDog()
    is Animal.Cat if !animal.mouseHunter -> feedCat()
    else if animal.eatsPlants -> giveLettuce()
    else -> println("Unknown animal")
}
```

---

## for 루프

### 기본 for 루프

```kotlin
for (item in collection) {
    println(item)
}
```

### 범위

```kotlin
// 닫힌 범위 (포함)
for (i in 1..6) print(i)  // 123456

// 열린 범위 (제외)
for (i in 1..<6) print(i)  // 12345

// 역순과 스텝
for (i in 6 downTo 0 step 2) print(i)  // 6420
```

### 인덱스가 있는 배열

```kotlin
val routineSteps = arrayOf("Wake up", "Brush teeth", "Make coffee")

// indices 사용
for (i in routineSteps.indices) {
    println(routineSteps[i])
}

// withIndex() 사용
for ((index, value) in routineSteps.withIndex()) {
    println("The step at $index is \"$value\"")
}
```

### 사용자 정의 반복자

`Iterable<T>` 인터페이스 구현:

```kotlin
class Booklet(val totalPages: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> {
        return object : Iterator<Int> {
            var current = 1
            override fun hasNext() = current <= totalPages
            override fun next() = current++
        }
    }
}

for (page in booklet) {
    println("Reading page $page")
}
```

---

## while 루프

### while 루프

먼저 조건을 검사한 후 본문을 실행합니다:

```kotlin
var carsInGarage = 0
val maxCapacity = 3

while (carsInGarage < maxCapacity) {
    println("Car entered. Cars now in garage: ${++carsInGarage}")
}
```

### do-while 루프

먼저 본문을 실행한 후 조건을 검사합니다:

```kotlin
do {
    roll = Random.nextInt(1, 7)
    println("Rolled a $roll")
} while (roll != 6)
```

---

## break와 continue

Kotlin은 루프에서 전통적인 `break`와 `continue` 연산자를 지원합니다. 자세한 내용은 [반환과 점프](returns.html) 문서를 참조하세요.
