# Kotlin 함수와 람다

> 공식 문서: https://kotlinlang.org/docs/functions.html

## 함수 선언

### 기본 함수

```kotlin
// 기본 형태
fun sum(a: Int, b: Int): Int {
    return a + b
}

// 단일 표현식 함수 (반환 타입 추론)
fun sum(a: Int, b: Int) = a + b

// Unit 반환 (void와 유사)
fun printSum(a: Int, b: Int): Unit {
    println("sum = ${a + b}")
}

// Unit 생략 가능
fun printSum(a: Int, b: Int) {
    println("sum = ${a + b}")
}
```

### 기본 매개변수

```kotlin
fun greet(
    name: String,
    greeting: String = "Hello",
    punctuation: String = "!"
): String {
    return "$greeting, $name$punctuation"
}

// 사용
greet("John")                          // "Hello, John!"
greet("John", "Hi")                    // "Hi, John!"
greet("John", "Hi", "?")               // "Hi, John?"
```

### 명명된 인자

```kotlin
fun reformat(
    str: String,
    normalizeCase: Boolean = true,
    upperCaseFirstLetter: Boolean = true,
    divideByCamelHumps: Boolean = false,
    wordSeparator: Char = ' '
) { /* ... */ }

// 명명된 인자 사용
reformat(
    "hello world",
    normalizeCase = true,
    upperCaseFirstLetter = false,
    divideByCamelHumps = true,
    wordSeparator = '_'
)

// 일부만 명명
reformat("hello", wordSeparator = '-')
```

### 가변 인자 (Varargs)

```kotlin
fun printAll(vararg messages: String) {
    for (m in messages) println(m)
}

printAll("Hello", "World", "!")

// 배열을 spread operator로 전달
val array = arrayOf("a", "b", "c")
printAll(*array)

// 다른 인자와 함께
fun format(format: String, vararg args: Any): String {
    return String.format(format, *args)
}
```

---

## 함수 타입

### 함수 타입 표기

```kotlin
// 기본 함수 타입
val sum: (Int, Int) -> Int = { a, b -> a + b }

// 파라미터 이름 포함 (문서화 목적)
val sum: (x: Int, y: Int) -> Int = { a, b -> a + b }

// nullable 함수 타입
val nullableSum: ((Int, Int) -> Int)? = null

// Unit 반환
val printer: (String) -> Unit = { println(it) }

// 수신자가 있는 함수 타입
val isEven: Int.() -> Boolean = { this % 2 == 0 }
5.isEven()  // false

// 고차 함수의 함수 타입
val operation: (Int, (Int) -> Int) -> Int = { x, f -> f(x) }
```

### 함수 참조

```kotlin
// 함수 참조
fun double(x: Int) = x * 2
val doubleRef: (Int) -> Int = ::double

// 멤버 참조
class Person(val name: String)
val getName: (Person) -> String = Person::name

// 생성자 참조
val createPerson: (String) -> Person = ::Person

// 바운드 참조
val person = Person("John")
val getPersonName: () -> String = person::name
println(getPersonName())  // "John"

// 확장 함수 참조
fun String.double() = this + this
val stringDouble: String.() -> String = String::double
```

---

## 람다 표현식

### 기본 람다

```kotlin
// 전체 문법
val sum: (Int, Int) -> Int = { a: Int, b: Int -> a + b }

// 타입 추론
val sum = { a: Int, b: Int -> a + b }

// 파라미터 타입 추론 (변수 타입 명시 시)
val sum: (Int, Int) -> Int = { a, b -> a + b }

// 단일 매개변수는 it 사용
val double: (Int) -> Int = { it * 2 }

// 여러 줄 람다 (마지막 표현식이 반환값)
val multiLine = { x: Int ->
    val doubled = x * 2
    val tripled = x * 3
    doubled + tripled  // 반환값
}
```

### 트레일링 람다

```kotlin
// 마지막 파라미터가 함수일 때 괄호 밖으로
val result = listOf(1, 2, 3).map { it * 2 }

// 람다가 유일한 인자일 때 괄호 생략
listOf(1, 2, 3).forEach { println(it) }

// 두 개 이상의 람다는 명명된 인자 권장
buildString {
    append("Hello")
    append(" ")
    append("World")
}
```

### 구조 분해

```kotlin
// Pair, Map.Entry 등에서 구조 분해
mapOf("a" to 1, "b" to 2).forEach { (key, value) ->
    println("$key = $value")
}

// 사용하지 않는 변수는 _
mapOf("a" to 1).forEach { (_, value) ->
    println(value)
}
```

### 클로저

```kotlin
var sum = 0
listOf(1, 2, 3).forEach {
    sum += it  // 외부 변수 캡처 및 수정
}
println(sum)  // 6
```

---

## 고차 함수

### 함수를 인자로

```kotlin
fun <T> List<T>.customFilter(predicate: (T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (predicate(item)) {
            result.add(item)
        }
    }
    return result
}

// 사용
val numbers = listOf(1, 2, 3, 4, 5)
val evens = numbers.customFilter { it % 2 == 0 }  // [2, 4]
```

### 함수를 반환

```kotlin
fun operation(type: String): (Int, Int) -> Int {
    return when (type) {
        "add" -> { a, b -> a + b }
        "subtract" -> { a, b -> a - b }
        "multiply" -> { a, b -> a * b }
        else -> { _, _ -> 0 }
    }
}

val add = operation("add")
println(add(3, 4))  // 7
```

### 인라인 함수

람다로 인한 오버헤드를 제거합니다.

```kotlin
inline fun <T> measure(block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    println("Elapsed: ${System.currentTimeMillis() - start}ms")
    return result
}

// 사용
val result = measure {
    Thread.sleep(100)
    "Done"
}
```

### noinline과 crossinline

```kotlin
// noinline: 특정 람다를 인라인하지 않음
inline fun foo(inlined: () -> Unit, noinline notInlined: () -> Unit) {
    // notInlined는 다른 함수에 전달 가능
    val stored: () -> Unit = notInlined
}

// crossinline: 비지역 반환 금지
inline fun createRunnable(crossinline block: () -> Unit): Runnable {
    return Runnable { block() }
}
```

### reified 타입 파라미터

인라인 함수에서 타입 정보를 런타임에 유지합니다.

```kotlin
inline fun <reified T> isType(value: Any): Boolean {
    return value is T
}

inline fun <reified T> parseJson(json: String): T {
    return Gson().fromJson(json, T::class.java)
}

// 사용
isType<String>("hello")  // true
isType<Int>("hello")     // false

val user: User = parseJson("""{"name": "John"}""")
```

---

## 익명 함수

```kotlin
// 익명 함수 (람다와 유사하지만 반환 타입 명시 가능)
val sum = fun(a: Int, b: Int): Int = a + b

// 블록 본문
val greet = fun(name: String): String {
    return "Hello, $name"
}

// 람다와의 차이: return 동작
fun lookForAlice(people: List<String>) {
    // 람다에서 return은 외부 함수 반환
    people.forEach {
        if (it == "Alice") return  // lookForAlice 함수 종료
    }

    // 익명 함수에서 return은 익명 함수만 반환
    people.forEach(fun(person) {
        if (person == "Alice") return  // forEach만 종료
    })
}
```

---

## 수신자 있는 함수 리터럴

### 수신자 있는 람다

```kotlin
// 수신자 타입.() -> 반환타입
val greet: StringBuilder.() -> Unit = {
    append("Hello, ")
    append("World!")
}

// 사용
val sb = StringBuilder()
sb.greet()
println(sb.toString())  // "Hello, World!"

// 또는
buildString {
    append("Hello")
    append(", ")
    append("World!")
}
```

### DSL 구축

```kotlin
// HTML DSL 예제
class HTML {
    private val children = mutableListOf<String>()

    fun body(init: Body.() -> Unit) {
        val body = Body()
        body.init()
        children.add(body.build())
    }

    fun build() = "<html>${children.joinToString("")}</html>"
}

class Body {
    private val children = mutableListOf<String>()

    fun p(text: String) {
        children.add("<p>$text</p>")
    }

    fun build() = "<body>${children.joinToString("")}</body>"
}

fun html(init: HTML.() -> Unit): String {
    val html = HTML()
    html.init()
    return html.build()
}

// 사용
val document = html {
    body {
        p("Hello")
        p("World")
    }
}
// <html><body><p>Hello</p><p>World</p></body></html>
```

---

## 연산자 오버로딩

### 단항 연산자

```kotlin
data class Point(val x: Int, val y: Int) {
    operator fun unaryMinus() = Point(-x, -y)
    operator fun inc() = Point(x + 1, y + 1)
    operator fun dec() = Point(x - 1, y - 1)
}

val point = Point(1, 2)
println(-point)  // Point(-1, -2)

var p = Point(1, 2)
println(++p)  // Point(2, 3)
```

### 이항 연산자

```kotlin
data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scale: Int) = Point(x * scale, y * scale)
}

val p1 = Point(1, 2)
val p2 = Point(3, 4)
println(p1 + p2)   // Point(4, 6)
println(p1 * 2)    // Point(2, 4)
```

### 비교 연산자

```kotlin
data class Person(val name: String, val age: Int) : Comparable<Person> {
    override operator fun compareTo(other: Person): Int {
        return this.age - other.age
    }
}

val john = Person("John", 25)
val jane = Person("Jane", 30)
println(john < jane)   // true
println(john >= jane)  // false
```

### 인덱스 연산자

```kotlin
class Matrix(private val data: Array<IntArray>) {
    operator fun get(row: Int, col: Int): Int = data[row][col]
    operator fun set(row: Int, col: Int, value: Int) {
        data[row][col] = value
    }
}

val matrix = Matrix(arrayOf(intArrayOf(1, 2), intArrayOf(3, 4)))
println(matrix[0, 1])  // 2
matrix[0, 1] = 10
```

### invoke 연산자

```kotlin
class Greeter(val greeting: String) {
    operator fun invoke(name: String) = "$greeting, $name!"
}

val greet = Greeter("Hello")
println(greet("World"))  // "Hello, World!"
```

### in 연산자

```kotlin
data class IntRange(val start: Int, val end: Int) {
    operator fun contains(value: Int): Boolean {
        return value in start..end
    }
}

val range = IntRange(1, 10)
println(5 in range)   // true
println(15 in range)  // false
```

---

## 중위 함수 (Infix)

```kotlin
infix fun Int.times(str: String) = str.repeat(this)
println(3 times "Hello ")  // "Hello Hello Hello "

infix fun String.onto(other: String) = Pair(this, other)
val pair = "key" onto "value"

// 커스텀 중위 함수
class Person(val name: String) {
    private val likedPeople = mutableListOf<Person>()

    infix fun likes(other: Person) {
        likedPeople.add(other)
    }
}

val john = Person("John")
val jane = Person("Jane")
john likes jane  // john.likes(jane)와 동일
```

---

## 테일 재귀 함수

```kotlin
// tailrec으로 스택 오버플로우 방지
tailrec fun factorial(n: Int, accumulator: Long = 1): Long {
    return if (n <= 1) accumulator
    else factorial(n - 1, n * accumulator)
}

tailrec fun findFixPoint(x: Double = 1.0): Double {
    return if (Math.abs(x - Math.cos(x)) < 1e-10) x
    else findFixPoint(Math.cos(x))
}
```

---

## 지역 함수

```kotlin
fun processUser(user: User) {
    // 지역 함수: 외부 함수의 변수에 접근 가능
    fun validate() {
        if (user.name.isEmpty()) {
            throw IllegalArgumentException("Name is empty for user ${user.id}")
        }
    }

    validate()
    // user 처리 로직
}

// 중복 코드 제거
fun saveUser(user: User) {
    fun validate(value: String, fieldName: String) {
        if (value.isEmpty()) {
            throw IllegalArgumentException("$fieldName is empty")
        }
    }

    validate(user.name, "Name")
    validate(user.email, "Email")

    // 저장 로직
}
```

---

## 참고 문서

- [Functions](https://kotlinlang.org/docs/functions.html)
- [Lambdas](https://kotlinlang.org/docs/lambdas.html)
- [Inline functions](https://kotlinlang.org/docs/inline-functions.html)
- [Operator overloading](https://kotlinlang.org/docs/operator-overloading.html)
