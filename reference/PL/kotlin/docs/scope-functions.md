# Kotlin 스코프 함수

## 개요

스코프 함수는 객체의 컨텍스트 내에서 코드 블록을 실행하는 Kotlin 표준 라이브러리의 함수입니다. 이름을 직접 사용하지 않고도 객체에 접근할 수 있게 해줍니다. **5가지 스코프 함수**가 있습니다: `let`, `run`, `with`, `apply`, `also`.

---

## 함수 선택 가이드

### 비교 표

| 함수 | 객체 참조 | 반환 값 | 확장 함수 여부 |
|------|----------|---------|---------------|
| `let` | `it` | 람다 결과 | 예 |
| `run` | `this` | 람다 결과 | 예 |
| `run` (비확장) | — | 람다 결과 | 아니오 |
| `with` | `this` | 람다 결과 | 아니오 |
| `apply` | `this` | 컨텍스트 객체 | 예 |
| `also` | `it` | 컨텍스트 객체 | 예 |

### 빠른 선택 가이드

- **널이 아닌 객체**: `let`
- **표현식을 지역 변수로 도입**: `let`
- **객체 구성**: `apply`
- **구성 + 결과 계산**: `run`
- **표현식이 필요한 곳에서 문장**: 비확장 `run`
- **추가 효과**: `also`
- **객체에 대한 함수 호출 그룹화**: `with`

---

## 주요 구분

### 1. 컨텍스트 객체 참조: `this` vs `it`

#### `this` (수신자 참조)
사용: `run`, `with`, `apply`

```kotlin
val adam = Person("Adam").apply {
    age = 20      // this.age = 20과 동일
    city = "London"
}
```

**장점**: 더 깔끔한 구문, 멤버에 접근할 때 `this` 생략 가능
**적합한 용도**: 객체의 멤버와 함수에 대한 작업

#### `it` (인자 참조)
사용: `let`, `also`

```kotlin
fun getRandomInt(): Int {
    return Random.nextInt(100).also { value ->
        writeToLog("getRandomInt() generated value $value")
    }
}
```

**장점**: 더 짧고 읽기 쉬움, 여러 변수에 더 좋음
**적합한 용도**: 객체를 함수 호출의 인자로 사용

### 2. 반환 값

#### 컨텍스트 객체 반환
`apply`와 `also` — 호출 체이닝 가능

```kotlin
val numberList = mutableListOf<Double>()
numberList.also { println("Populating the list") }
    .apply {
        add(2.71)
        add(3.14)
        add(1.0)
    }
    .also { println("Sorting the list") }
    .sort()
```

#### 람다 결과 반환
`let`, `run`, `with` — 변환용

```kotlin
val countEndsWithE = numbers.run {
    add("four")
    add("five")
    count { it.endsWith("e") }
}
```

---

## 상세 함수 설명

### `let`

**컨텍스트 객체**: `it` (인자)
**반환**: 람다 결과

**사용 사례**:
- 호출 체인 결과에 함수 호출
- 널 가능 객체에 안전 호출
- 제한된 스코프로 지역 변수 도입

```kotlin
// 널 확인과 함께 안전 호출
val str: String? = "Hello"
val length = str?.let {
    println("let() called on $it")
    processNonNullString(it)
    it.length
}

// 사용자 정의 변수 이름
val modifiedFirstItem = numbers.first().let { firstItem ->
    if (firstItem.length >= 5) firstItem else "!" + firstItem + "!"
}.uppercase()

// 메서드 참조
numbers.map { it.length }.filter { it > 3 }.let(::println)
```

### `with`

**컨텍스트 객체**: `this` (수신자)
**반환**: 람다 결과
**참고**: 확장 함수가 아님

**사용 사례**:
- 결과가 필요 없이 함수 호출
- 헬퍼 객체 도입

```kotlin
val numbers = mutableListOf("one", "two", "three")
with(numbers) {
    println("'with' is called with argument $this")
    println("It contains $size elements")
}

val firstAndLast = with(numbers) {
    "The first element is ${first()}," +
    " the last element is ${last()}"
}
```

### `run`

**컨텍스트 객체**: `this` (수신자)
**반환**: 람다 결과
**참고**: 확장 함수 (비확장 버전도 있음)

**사용 사례**:
- 객체 초기화와 반환 값 계산
- 표현식이 필요한 곳에서 문장 실행

```kotlin
val result = service.run {
    port = 8080
    query(prepareRequest() + " to port $port")
}

// 비확장 변형
val hexNumberRegex = run {
    val digits = "0-9"
    val hexDigits = "A-Fa-f"
    val sign = "+-"
    Regex("[$sign]?[$digits$hexDigits]+")
}
```

### `apply`

**컨텍스트 객체**: `this` (수신자)
**반환**: 컨텍스트 객체 자체

**사용 사례**:
- 객체 구성
- 값을 반환하지 않는 코드 블록
- 복잡한 처리 체인

```kotlin
val adam = Person("Adam").apply {
    age = 32
    city = "London"
}
```

### `also`

**컨텍스트 객체**: `it` (인자)
**반환**: 컨텍스트 객체 자체

**사용 사례**:
- 객체를 인자로 사용하는 작업
- 외부 `this`를 가리지 않으려 할 때
- 체이닝 중 부수 효과

```kotlin
val numbers = mutableListOf("one", "two", "three")
numbers
    .also { println("The list elements before adding new one: $it") }
    .add("four")
```

---

## `takeIf`와 `takeUnless`

호출 체인에 상태 확인을 포함합니다.

**`takeIf`**: 조건이 true이면 객체 반환, 아니면 `null`

**`takeUnless`**: 조건이 false이면 객체 반환, 아니면 `null`

```kotlin
val number = Random.nextInt(100)
val evenOrNull = number.takeIf { it % 2 == 0 }
val oddOrNull = number.takeUnless { it % 2 == 0 }

// 안전 호출 필요 (널 가능 반환)
val caps = str.takeIf { it.isNotEmpty() }?.uppercase()

// let과 결합
fun displaySubstringPosition(input: String, sub: String) {
    input.indexOf(sub).takeIf { it >= 0 }?.let {
        println("The substring $sub is found in $input.")
        println("Its start position is $it.")
    }
}
```

---

## 모범 사례

**남용 피하기**: 스코프 함수는 코드를 읽기 어렵게 만들 수 있음
**중첩 피하기**: 컨텍스트 객체와 `this`/`it`에 대해 혼란스러워지기 쉬움
**프로젝트 규칙 따르기**: 팀 일관성에 기반하여 선택
