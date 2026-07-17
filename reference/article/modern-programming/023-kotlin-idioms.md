# Kotlin 관용구(Idioms)

> 원문: [Idioms](https://kotlinlang.org/docs/idioms.html)
> 저자: JetBrains (Kotlin 공식 문서) | 번역일: 2026-07-10

Kotlin에서 자주 쓰이는 여러 관용구를 모았다. 즐겨 쓰는 관용구가 있다면 풀 리퀘스트를 보내 기여할 수 있다.

## DTO(POJO/POCO) 만들기

```kotlin
data class Customer(val name: String, val email: String)
```

이렇게 하면 다음 기능을 갖춘 `Customer` 클래스가 만들어진다.

* 모든 프로퍼티의 getter(`var`인 경우 setter도)
* `equals()`
* `hashCode()`
* `toString()`
* `copy()`
* 모든 프로퍼티에 대응하는 `component1()`, `component2()`, ... ([데이터 클래스](https://kotlinlang.org/docs/data-classes.html) 참고)

## 함수 파라미터 기본값

```kotlin
fun foo(a: Int = 0, b: String = "") { ... }
```

## 리스트 필터링

```kotlin
val positives = list.filter { x -> x > 0 }
```

더 짧게 쓸 수도 있다.

```kotlin
val positives = list.filter { it > 0 }
```

[Java와 Kotlin의 필터링 차이](https://kotlinlang.org/docs/java-to-kotlin-collections-guide.html#filter-elements)를 알아보자.

## 컬렉션에 요소가 있는지 확인

```kotlin
if ("john@example.com" in emailsList) { ... }

if ("jane@example.com" !in emailsList) { ... }
```

## 문자열 보간

```kotlin
println("Name $name")
```

[Java와 Kotlin의 문자열 연결 차이](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html#concatenate-strings)를 알아보자.

## 표준 입력을 안전하게 읽기

```kotlin
// 문자열을 읽고, 입력을 정수로 변환할 수 없으면 null을 반환한다. 예: Hi there!
val wrongInt = readln().toIntOrNull()
println(wrongInt)
// null

// 정수로 변환할 수 있는 문자열을 읽어 정수를 반환한다. 예: 13
val correctInt = readln().toIntOrNull()
println(correctInt)
// 13
```

자세한 내용은 [표준 입력 읽기](https://kotlinlang.org/docs/read-standard-input.html)를 참고한다.

## 인스턴스 검사

```kotlin
when (x) {
    is Foo -> ...
    is Bar -> ...
    else   -> ...
}
```

## 읽기 전용 리스트

```kotlin
val list = listOf("a", "b", "c")
```

## 읽기 전용 맵

```kotlin
val map = mapOf("a" to 1, "b" to 2, "c" to 3)
```

## 맵 항목 접근

```kotlin
println(map["key"])
map["key"] = value
```

## 맵 또는 pair 리스트 순회

```kotlin
for ((k, v) in map) {
    println("$k -> $v")
}
```

`k`와 `v`는 `name`, `age`처럼 편한 이름 아무거나 써도 된다.

## 범위 순회

```kotlin
for (i in 1..100) { ... }  // 닫힌 범위: 100을 포함한다
for (i in 1..<100) { ... } // 열린 범위: 100을 포함하지 않는다
for (x in 2..10 step 2) { ... }
for (x in 10 downTo 1) { ... }
(1..10).forEach { ... }
```

## 지연 초기화 프로퍼티

```kotlin
val p: String by lazy { // 값은 첫 접근 시에만 계산된다
    // 문자열 계산
}
```

## 확장 함수

```kotlin
fun String.spaceToCamelCase() { ... }

"Convert this to camelcase".spaceToCamelCase()
```

## 싱글턴 만들기

```kotlin
object Resource {
    val name = "Name"
}
```

## 타입 안전한 값을 위한 인라인 value 클래스 사용

```kotlin
@JvmInline
value class EmployeeId(private val id: String)

@JvmInline
value class CustomerId(private val id: String)
```

`EmployeeId`와 `CustomerId`를 실수로 섞어 쓰면 컴파일 오류가 발생한다.

> `@JvmInline` 어노테이션은 JVM 백엔드에서만 필요하다.

## 추상 클래스 인스턴스화

```kotlin
abstract class MyAbstractClass {
    abstract fun doSomething()
    abstract fun sleep()
}

fun main() {
    val myObject = object : MyAbstractClass() {
        override fun doSomething() {
            // ...
        }

        override fun sleep() { // ...
        }
    }
    myObject.doSomething()
}
```

## if-not-null 축약

```kotlin
val files = File("Test").listFiles()

println(files?.size) // files가 null이 아니면 size가 출력된다
```

## if-not-null-else 축약

```kotlin
val files = File("Test").listFiles()

// 단순한 대체 값이라면:
println(files?.size ?: "empty") // files가 null이면 "empty"를 출력한다

// 더 복잡한 대체 값을 코드 블록에서 계산하려면 `run`을 사용한다
val filesSize = files?.size ?: run { 
    val someSize = getSomeSize()
    someSize * 2
}
println(filesSize)
```

## null이면 식 실행

```kotlin
val values = ...
val email = values["email"] ?: throw IllegalStateException("Email is missing!")
```

## 비어 있을 수 있는 컬렉션의 첫 요소 가져오기

```kotlin
val emails = ... // 비어 있을 수 있다
val mainEmail = emails.firstOrNull() ?: ""
```

[Java와 Kotlin의 첫 요소 가져오기 차이](https://kotlinlang.org/docs/java-to-kotlin-collections-guide.html#get-the-first-and-the-last-items-of-a-possibly-empty-collection)를 알아보자.

## null이 아니면 실행

```kotlin
val value = ...

value?.let {
    ... // null이 아니면 이 블록을 실행한다
}
```

## null이 아니면 nullable 값 변환

```kotlin
val value = ...

val mapped = value?.let { transformValue(it) } ?: defaultValue 
// value 또는 변환 결과가 null이면 defaultValue가 반환된다.
```

## when 문에서 반환

```kotlin
fun transform(color: String): Int {
    return when (color) {
        "Red" -> 0
        "Green" -> 1
        "Blue" -> 2
        else -> throw IllegalArgumentException("Invalid color param value")
    }
}
```

## try-catch 식

```kotlin
fun test() {
    val result = try {
        count()
    } catch (e: ArithmeticException) {
        throw IllegalStateException(e)
    }

    // result로 작업
}
```

## if 식

```kotlin
val y = if (x == 1) {
    "one"
} else if (x == 2) {
    "two"
} else {
    "other"
}
```

## Unit을 반환하는 메서드를 빌더 스타일로 사용

```kotlin
fun arrayOfMinusOnes(size: Int): IntArray {
    return IntArray(size).apply { fill(-1) }
}
```

## 단일 식 함수

```kotlin
fun theAnswer() = 42
```

이는 다음과 동일하다.

```kotlin
fun theAnswer(): Int {
    return 42
}
```

다른 관용구와 효과적으로 조합하면 코드가 더 짧아진다. 예를 들어 `when` 식과 함께 쓰면:

```kotlin
fun transform(color: String): Int = when (color) {
    "Red" -> 0
    "Green" -> 1
    "Blue" -> 2
    else -> throw IllegalArgumentException("Invalid color param value")
}
```

## 객체 인스턴스에서 여러 메서드 호출 (with)

```kotlin
class Turtle {
    fun penDown()
    fun penUp()
    fun turn(degrees: Double)
    fun forward(pixels: Double)
}

val myTurtle = Turtle()
with(myTurtle) { // 100픽셀 정사각형 그리기
    penDown()
    for (i in 1..4) {
        forward(100.0)
        turn(90.0)
    }
    penUp()
}
```

## 객체 프로퍼티 설정 (apply)

```kotlin
val myRectangle = Rectangle().apply {
    length = 4
    breadth = 5
    color = 0xFAFAFA
}
```

객체 생성자에 없는 프로퍼티를 설정할 때 유용하다.

## Java 7의 try-with-resources

```kotlin
val stream = Files.newInputStream(Paths.get("/some/file.txt"))
stream.buffered().reader().use { reader ->
    println(reader.readText())
}
```

## 제네릭 타입 정보가 필요한 제네릭 함수

```kotlin
//  public final class Gson {
//     ...
//     public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
//     ...

inline fun <reified T: Any> Gson.fromJson(json: JsonElement): T = this.fromJson(json, T::class.java)
```

## 두 변수 교환

```kotlin
var a = 1
var b = 2
a = b.also { b = a }
```

## 미완성 코드 표시 (TODO)

Kotlin 표준 라이브러리에는 항상 `NotImplementedError`를 던지는 `TODO()` 함수가 있다.
반환 타입이 `Nothing`이라서 기대되는 타입이 무엇이든 사용할 수 있다.
이유를 파라미터로 받는 오버로드도 있다.

```kotlin
fun calcTaxes(): BigDecimal = TODO("Waiting for feedback from accounting")
```

IntelliJ IDEA의 Kotlin 플러그인은 `TODO()`의 의미를 이해해서 TODO 도구 창에 코드 포인터를 자동으로 추가한다.

## 다음 단계

* 관용적인 Kotlin 스타일로 [Advent of Code 퍼즐](https://kotlinlang.org/docs/advent-of-code.html)을 풀어 보자.
* [Java와 Kotlin에서 문자열을 다루는 일반적인 작업](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html)을 알아보자.
* [Java와 Kotlin에서 컬렉션을 다루는 일반적인 작업](https://kotlinlang.org/docs/java-to-kotlin-collections-guide.html)을 알아보자.
* [Java와 Kotlin에서 nullability를 다루는 방법](https://kotlinlang.org/docs/java-to-kotlin-nullability-guide.html)을 알아보자.
