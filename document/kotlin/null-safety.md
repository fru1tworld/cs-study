# Kotlin Null Safety

> 공식 문서: https://kotlinlang.org/docs/null-safety.html

## 개요

Kotlin의 타입 시스템은 NullPointerException(NPE)의 위험을 제거하기 위해 설계되었습니다. 타입 시스템 수준에서 nullable과 non-nullable 타입을 구분합니다.

---

## Nullable 타입

### 기본 개념

```kotlin
// Non-nullable: null을 가질 수 없음
var name: String = "John"
// name = null  // 컴파일 에러!

// Nullable: null을 가질 수 있음
var nullableName: String? = "John"
nullableName = null  // OK

// 타입 관계
// String은 String?의 하위 타입
val nonNull: String = "Hello"
val nullable: String? = nonNull  // OK
// val backToNonNull: String = nullable  // 에러!
```

### NPE가 발생할 수 있는 유일한 경우

```kotlin
// 1. 명시적 throw
throw NullPointerException()

// 2. !! 연산자 사용
val length = nullableString!!.length

// 3. 초기화 관련 데이터 불일치
// - 생성자에서 초기화되지 않은 this 누출
// - 슈퍼클래스 생성자가 파생 클래스의 open 멤버 호출

// 4. Java 상호 운용
// - null 처리가 안 된 플랫폼 타입
// - 제네릭 타입의 null 관련 문제
```

---

## Null 검사

### 조건문으로 검사

```kotlin
val name: String? = "John"

// if로 검사
if (name != null) {
    println(name.length)  // 스마트 캐스트됨
}

// 복합 조건
if (name != null && name.length > 0) {
    println(name)
}

// when에서
when {
    name == null -> println("Name is null")
    name.isEmpty() -> println("Name is empty")
    else -> println("Name is $name")
}
```

### 스마트 캐스트

컴파일러가 null 검사 후 자동으로 non-nullable로 캐스트합니다.

```kotlin
fun printLength(str: String?) {
    if (str != null) {
        // str은 여기서 String으로 스마트 캐스트됨
        println(str.length)
    }

    // str?.let으로도 가능
    str?.let {
        println(it.length)
    }
}

// 스마트 캐스트가 작동하지 않는 경우
class Example {
    var mutableProperty: String? = "hello"

    fun test() {
        if (mutableProperty != null) {
            // 다른 스레드에서 수정될 수 있으므로 스마트 캐스트 안 됨
            // println(mutableProperty.length)  // 에러!

            // 해결책: 로컬 변수에 복사
            val local = mutableProperty
            if (local != null) {
                println(local.length)  // OK
            }
        }
    }
}
```

---

## Safe Call 연산자 (?.)

```kotlin
val name: String? = null

// Safe call: null이면 null 반환
val length: Int? = name?.length  // null

// 체이닝
val city: String? = person?.address?.city

// 컬렉션에서
val listWithNulls: List<String?> = listOf("A", null, "B")
for (item in listWithNulls) {
    item?.let { println(it) }  // null이 아닌 것만 출력
}

// Safe call과 let 조합
val email: String? = user?.email
email?.let { sendEmail(it) }

// 더 간단하게
user?.email?.let { sendEmail(it) }
```

### Safe call과 할당

```kotlin
// 왼쪽이 null이면 할당이 건너뜀
person?.name = "John"  // person이 null이면 아무 일도 안 함

// 체이닝된 경우
person?.address?.city = "Seoul"
```

---

## Elvis 연산자 (?:)

null일 경우 기본값을 제공합니다.

```kotlin
val name: String? = null

// 기본값 제공
val displayName: String = name ?: "Unknown"

// 체이닝된 호출과 함께
val city = person?.address?.city ?: "Unknown City"

// 표현식 사용
val length = name?.length ?: 0

// throw와 함께
val nonNullName = name ?: throw IllegalArgumentException("Name required")

// return과 함께
fun process(name: String?) {
    val validated = name ?: return  // null이면 함수 종료
    println("Processing: $validated")
}
```

---

## Not-null 단언 연산자 (!!)

```kotlin
val name: String? = "John"

// null이 아님을 단언 (위험!)
val length: Int = name!!.length

// 주의: null이면 NPE 발생
val nullName: String? = null
// val willCrash = nullName!!.length  // NullPointerException!
```

### 언제 !! 를 사용할까

```kotlin
// 1. 절대 null이 아님을 확신할 때 (드물게)
val id = intent.getStringExtra("id")!!

// 2. 테스트 코드에서
@Test
fun testNotNull() {
    val result = service.findById(1)
    assertNotNull(result)
    assertEquals("John", result!!.name)
}

// 권장: 가능하면 다른 방법 사용
// 대신 이렇게:
val id = intent.getStringExtra("id")
    ?: throw IllegalStateException("ID is required")

// 또는
val id = requireNotNull(intent.getStringExtra("id")) { "ID is required" }
```

---

## Safe Cast (as?)

```kotlin
val obj: Any = "Hello"

// 일반 캐스트 (실패 시 ClassCastException)
// val num: Int = obj as Int  // 예외!

// Safe cast (실패 시 null)
val num: Int? = obj as? Int  // null

// 패턴
when (val result = obj as? String) {
    null -> println("Not a String")
    else -> println("String: $result")
}

// is 검사 후 스마트 캐스트
if (obj is String) {
    println(obj.length)  // 이미 String으로 캐스트됨
}
```

---

## Nullable 컬렉션

### Nullable 요소

```kotlin
// 요소가 nullable
val listOfNullable: List<Int?> = listOf(1, null, 2, null, 3)

// null 필터링
val nonNulls: List<Int> = listOfNullable.filterNotNull()  // [1, 2, 3]

// mapNotNull
val lengths = listOfNullable.mapNotNull { it?.let { n -> n * 2 } }  // [2, 4, 6]
```

### Nullable 컬렉션

```kotlin
// 컬렉션 자체가 nullable
val nullableList: List<Int>? = null

// Safe call
val size = nullableList?.size ?: 0

// orEmpty()
val safeList: List<Int> = nullableList.orEmpty()

// isNullOrEmpty()
if (nullableList.isNullOrEmpty()) {
    println("List is null or empty")
}
```

### Map에서 null

```kotlin
val map = mapOf("a" to 1, "b" to 2)

// get은 nullable 반환
val value: Int? = map["c"]  // null

// getOrDefault
val withDefault: Int = map.getOrDefault("c", 0)  // 0

// getOrElse
val withElse: Int = map.getOrElse("c") { computeDefault() }

// getValue (없으면 예외)
// val willThrow = map.getValue("c")  // NoSuchElementException
```

---

## 유틸리티 함수

### requireNotNull

```kotlin
fun process(id: String?) {
    val nonNullId = requireNotNull(id) { "ID cannot be null" }
    // nonNullId는 String 타입
    println("Processing: $nonNullId")
}

// null이면 IllegalArgumentException 발생
```

### checkNotNull

```kotlin
fun verify(value: String?) {
    checkNotNull(value) { "Value should not be null at this point" }
    // value는 String 타입
}

// null이면 IllegalStateException 발생
```

### 차이점

- `requireNotNull`: 입력 검증에 사용 (IllegalArgumentException)
- `checkNotNull`: 상태 검증에 사용 (IllegalStateException)

### isNullOrEmpty / isNullOrBlank

```kotlin
val str1: String? = null
val str2: String? = ""
val str3: String? = "  "
val str4: String? = "hello"

println(str1.isNullOrEmpty())  // true
println(str2.isNullOrEmpty())  // true
println(str3.isNullOrEmpty())  // false
println(str4.isNullOrEmpty())  // false

println(str1.isNullOrBlank())  // true
println(str2.isNullOrBlank())  // true
println(str3.isNullOrBlank())  // true (공백만)
println(str4.isNullOrBlank())  // false
```

---

## 제네릭과 Null

### Nullable 타입 파라미터

```kotlin
// T는 기본적으로 Any?의 하위 타입 (nullable 허용)
fun <T> process(value: T) {
    // value는 nullable일 수 있음
}

// non-nullable로 제한
fun <T : Any> processNonNull(value: T) {
    // value는 null이 될 수 없음
}

// 사용
process(null)           // OK
process("hello")        // OK
// processNonNull(null) // 컴파일 에러!
processNonNull("hello") // OK
```

### Nullable 타입 파라미터와 제약

```kotlin
// Nullable 반환 허용
fun <T> List<T>.firstOrNull(): T? {
    return if (isEmpty()) null else get(0)
}

// Non-null 버전
fun <T : Any> List<T>.firstOrThrow(): T {
    return if (isEmpty()) throw NoSuchElementException() else get(0)
}
```

---

## 플랫폼 타입 (Java 상호 운용)

Java에서 온 타입은 ** 플랫폼 타입** 으로, nullable 여부가 불명확합니다.

```kotlin
// Java 코드
// public String getName() { return name; }

// Kotlin에서 사용
val name = javaObject.getName()  // String! (플랫폼 타입)

// 개발자가 결정해야 함
val safeName: String? = javaObject.getName()  // nullable로 처리
val riskyName: String = javaObject.getName()   // non-null로 가정 (위험!)

// 안전하게 처리
val processedName = javaObject.getName()?.uppercase() ?: "UNKNOWN"
```

### @Nullable / @NotNull 어노테이션

Java에서 어노테이션을 사용하면 Kotlin이 인식합니다.

```java
// Java
@NotNull
public String getName() { return name; }

@Nullable
public String getMiddleName() { return middleName; }
```

```kotlin
// Kotlin
val name: String = javaObject.getName()  // non-null로 인식
val middleName: String? = javaObject.getMiddleName()  // nullable로 인식
```

---

## lateinit과 Nullable

### lateinit

```kotlin
class MyService {
    // non-null이지만 나중에 초기화
    lateinit var repository: Repository

    fun setup() {
        repository = Repository()
    }

    fun isInitialized(): Boolean = ::repository.isInitialized
}
```

### lateinit vs Nullable

```kotlin
// lateinit: 반드시 초기화될 것이 확실할 때
lateinit var config: Config

// Nullable: 초기화되지 않을 수 있을 때
var optionalConfig: Config? = null
```

### by lazy vs lateinit

```kotlin
// lazy: 처음 접근 시 초기화, val만 가능
val heavyObject: HeavyObject by lazy {
    HeavyObject()
}

// lateinit: 명시적으로 나중에 초기화, var만 가능
lateinit var injectedObject: InjectedObject
```

---

## 실전 패턴

### Null Object 패턴

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// null 대신 의미 있는 상태 사용
fun process(result: Result<String>) = when (result) {
    is Result.Success -> println("Data: ${result.data}")
    is Result.Error -> println("Error: ${result.exception}")
    Result.Loading -> println("Loading...")
}
```

### 안전한 API 설계

```kotlin
// 좋은 예: nullable 반환 대신 sealed class
sealed class FindResult<out T> {
    data class Found<T>(val value: T) : FindResult<T>()
    data object NotFound : FindResult<Nothing>()
}

fun findUser(id: Long): FindResult<User>

// 사용
when (val result = findUser(1)) {
    is FindResult.Found -> println(result.value)
    FindResult.NotFound -> println("User not found")
}
```

### 확장 함수로 null 처리

```kotlin
// null-safe 확장 함수
fun String?.orEmpty(): String = this ?: ""

fun <T> T?.orDefault(default: T): T = this ?: default

fun <T> T?.ifNull(action: () -> Unit): T? {
    if (this == null) action()
    return this
}

// 사용
val name: String? = null
println(name.orEmpty())  // ""

val count: Int? = null
println(count.orDefault(0))  // 0

name.ifNull { println("Name is null") }
```

---

## 참고 문서

- [Null safety](https://kotlinlang.org/docs/null-safety.html)
- [Kotlin and Java interoperability](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types)
