# Kotlin 스코프 함수

> 공식 문서: https://kotlinlang.org/docs/scope-functions.html

## 개요

스코프 함수는 객체의 컨텍스트 내에서 코드 블록을 실행하는 함수입니다. 람다 표현식이 있는 객체에서 이러한 함수를 호출하면 임시 스코프가 형성됩니다. 이 스코프 내에서는 이름 없이 객체에 접근할 수 있습니다.

---

## 스코프 함수 비교

| 함수 | 객체 참조 | 반환 값 | 확장 함수 여부 |
|------|----------|--------|--------------|
| `let` | it | 람다 결과 | Yes |
| `run` | this | 람다 결과 | Yes |
| `with` | this | 람다 결과 | No (컨텍스트 객체를 인자로) |
| `apply` | this | 컨텍스트 객체 | Yes |
| `also` | it | 컨텍스트 객체 | Yes |

### 선택 가이드

```
객체 설정/변경 후 객체 반환?
├── Yes → apply 또는 also
│   ├── 객체 멤버 접근 위주 → apply (this)
│   └── 객체를 인자로 사용 → also (it)
└── No → let, run, 또는 with
    ├── null 검사 필요 → let (?.let)
    ├── 지역 변수 스코프 제한 → let 또는 run
    └── 그룹화된 함수 호출 → with 또는 run
```

---

## let

### 기본 사용

```kotlin
val numbers = mutableListOf("one", "two", "three")

// 람다 결과 반환
val resultList = numbers.map { it.length }.filter { it > 3 }
println(resultList)

// let으로 체이닝
numbers.map { it.length }
    .filter { it > 3 }
    .let { println(it) }
```

### Null 검사와 함께

```kotlin
val name: String? = "John"

// null이 아닐 때만 실행
name?.let {
    println("Name is $it")
    println("Length is ${it.length}")
}

// Elvis와 함께
val displayName = name?.let {
    "Hello, $it"
} ?: "Hello, Guest"
```

### 지역 변수 스코프 제한

```kotlin
val numbers = listOf("one", "two", "three")

// 임시 변수 스코프 제한
numbers.first().let { firstItem ->
    if (firstItem.length > 3) {
        println("Long first item: $firstItem")
    } else {
        println("Short first item: $firstItem")
    }
}
// firstItem은 여기서 접근 불가
```

### 변환 및 매핑

```kotlin
val person = Person("John", 25)

// 객체 변환
val introduction = person.let {
    "My name is ${it.name} and I'm ${it.age} years old"
}

// null-safe 변환
val nullablePerson: Person? = getPerson()
val greeting = nullablePerson?.let { "${it.name}님 안녕하세요" } ?: "손님 안녕하세요"
```

---

## run

### 기본 사용 (확장 함수)

```kotlin
val service = MultiportService("https://example.com", 80)

val result = service.run {
    port = 8080
    query(prepareRequest() + " to port $port")
}
```

### 비확장 함수로서의 run

```kotlin
// 표현식이 필요한 곳에서 문장들 실행
val hexNumberRegex = run {
    val digits = "0-9"
    val hexDigits = "A-Fa-f"
    val sign = "+-"
    Regex("[$sign]?[$digits$hexDigits]+")
}

for (match in hexNumberRegex.findAll("+123 -FFFF !%diffAB")) {
    println(match.value)
}
```

### with와의 차이

```kotlin
// run: 확장 함수, nullable에 안전
person?.run {
    println(name)
    println(age)
}

// with: 일반 함수, null 검사 필요
with(person) {
    println(name)
    println(age)
}
```

---

## with

### 기본 사용

```kotlin
val numbers = mutableListOf("one", "two", "three")

// 그룹화된 함수 호출
with(numbers) {
    println("'with' is called with argument $this")
    println("It contains $size elements")
}

// 결과 반환
val result = with(numbers) {
    "The first element is ${first()}, the last is ${last()}"
}
```

### 객체 설정

```kotlin
val person = Person()

with(person) {
    name = "John"
    age = 25
    email = "john@example.com"
}
```

### 프로퍼티 계산

```kotlin
val user = User(name = "John", birthYear = 1990)

val description = with(user) {
    """
    Name: $name
    Birth Year: $birthYear
    Age: ${2024 - birthYear}
    """.trimIndent()
}
```

---

## apply

### 기본 사용

```kotlin
val person = Person("John").apply {
    age = 25
    city = "Seoul"
}

// 객체 설정에 이상적
val textView = TextView(context).apply {
    text = "Hello"
    textSize = 16f
    setTextColor(Color.BLACK)
}
```

### 빌더 패턴 대체

```kotlin
// apply로 빌더처럼 사용
val request = Request.Builder().apply {
    url("https://api.example.com")
    header("Authorization", "Bearer token")
    method("GET", null)
}.build()

// 체이닝
val dialog = AlertDialog.Builder(context).apply {
    setTitle("제목")
    setMessage("메시지")
    setPositiveButton("확인") { _, _ -> }
    setNegativeButton("취소") { _, _ -> }
}.create().apply {
    show()
}
```

### 객체 초기화

```kotlin
data class Person(
    var name: String = "",
    var age: Int = 0,
    var email: String = ""
)

val person = Person().apply {
    name = "John"
    age = 25
    email = "john@example.com"
}
```

---

## also

### 기본 사용

```kotlin
val numbers = mutableListOf("one", "two", "three")

numbers
    .also { println("Before adding: $it") }
    .add("four")

// 체인 중간에 부수 효과
val result = numbers
    .filter { it.length > 3 }
    .also { println("After filtering: $it") }
    .map { it.uppercase() }
    .also { println("After mapping: $it") }
```

### 디버깅/로깅

```kotlin
fun processData(data: Data): Result {
    return data
        .also { println("Input: $it") }
        .validate()
        .also { println("After validation: $it") }
        .transform()
        .also { println("After transform: $it") }
}
```

### 추가 동작

```kotlin
// 객체 반환하면서 추가 작업
fun createUser(name: String): User {
    return User(name).also {
        saveToDatabase(it)
        sendWelcomeEmail(it)
    }
}

// null 검사와 함께
user?.also {
    println("Processing user: ${it.name}")
    analyticsService.track("user_accessed", it.id)
}
```

---

## takeIf와 takeUnless

### takeIf

조건이 true면 객체 반환, false면 null 반환

```kotlin
val number = 42

// 조건 만족 시 객체 반환
val evenNumber = number.takeIf { it % 2 == 0 }  // 42
val oddNumber = number.takeIf { it % 2 != 0 }   // null

// 체이닝
val displayName = user.name
    .takeIf { it.isNotBlank() }
    ?: "Anonymous"

// 필터링처럼 사용
fun processOnlyValid(data: Data?) {
    data?.takeIf { it.isValid }
        ?.let { process(it) }
}
```

### takeUnless

조건이 false면 객체 반환, true면 null 반환 (takeIf의 반대)

```kotlin
val number = 42

val notEven = number.takeUnless { it % 2 == 0 }  // null
val notOdd = number.takeUnless { it % 2 != 0 }   // 42

// 빈 문자열 제외
val validName = name.takeUnless { it.isBlank() }

// null이 아닌 경우만
fun fetchIfNotCached(key: String): Data? {
    return cache[key].takeUnless { it == null }
        ?: fetchFromNetwork(key)
}
```

---

## 스코프 함수 조합

### let + apply

```kotlin
val person = Person().apply {
    name = "John"
    age = 25
}.let {
    "Created person: ${it.name}, ${it.age}"
}
```

### also + let

```kotlin
val numbers = mutableListOf<Int>()
    .also { println("Empty list created") }
    .also { it.add(1) }
    .also { it.add(2) }
    .let { it.sum() }
```

### 중첩 스코프

```kotlin
// 중첩을 피하기 위해 명시적 이름 사용
person?.let { p ->
    p.address?.let { addr ->
        println("${p.name} lives in ${addr.city}")
    }
}

// 더 나은 방법
person?.address?.let { address ->
    println("${person.name} lives in ${address.city}")
}

// 또는 run 사용
person?.run {
    address?.run {
        println("${this@run.name} lives in $city")
    }
}
```

---

## 실전 패턴

### 객체 생성 및 설정

```kotlin
// 안 좋은 예
val textView = TextView(context)
textView.text = "Hello"
textView.textSize = 16f
textView.visibility = View.VISIBLE

// 좋은 예 - apply
val textView = TextView(context).apply {
    text = "Hello"
    textSize = 16f
    visibility = View.VISIBLE
}
```

### Nullable 처리

```kotlin
// 안 좋은 예
if (user != null) {
    sendEmail(user.email)
    logActivity(user.id)
}

// 좋은 예 - let
user?.let {
    sendEmail(it.email)
    logActivity(it.id)
}

// 더 나은 예 - also (부수 효과 명확)
user?.also {
    sendEmail(it.email)
    logActivity(it.id)
}
```

### 임시 변수 제거

```kotlin
// 안 좋은 예
val temp = calculateResult()
if (temp.isValid) {
    processResult(temp)
}

// 좋은 예 - let
calculateResult().let {
    if (it.isValid) {
        processResult(it)
    }
}

// takeIf로 더 간결하게
calculateResult()
    .takeIf { it.isValid }
    ?.let { processResult(it) }
```

### API 응답 처리

```kotlin
suspend fun fetchUser(id: Long): User? {
    return api.getUser(id)
        .takeIf { it.isSuccessful }
        ?.body()
        ?.also { cache.put(id, it) }
}

// 또는
suspend fun fetchAndProcess(id: Long) {
    api.getUser(id).run {
        if (isSuccessful) {
            body()?.let { user ->
                processUser(user)
            }
        } else {
            handleError(errorBody())
        }
    }
}
```

### 로깅/디버깅

```kotlin
fun processOrder(order: Order): Receipt {
    return order
        .also { logger.info("Processing order: ${it.id}") }
        .validate()
        .also { logger.debug("Validation passed") }
        .calculateTotal()
        .also { logger.info("Total calculated: ${it.amount}") }
        .generateReceipt()
        .also { logger.info("Receipt generated: ${it.number}") }
}
```

---

## 성능 고려사항

스코프 함수는 인라인 함수이므로 런타임 오버헤드가 없습니다.

```kotlin
// 모든 스코프 함수는 inline으로 정의됨
public inline fun <T, R> T.let(block: (T) -> R): R {
    return block(this)
}

public inline fun <T> T.apply(block: T.() -> Unit): T {
    block()
    return this
}
```

---

## 참고 문서

- [Scope functions](https://kotlinlang.org/docs/scope-functions.html)
- [Kotlin Idioms](https://kotlinlang.org/docs/idioms.html)
