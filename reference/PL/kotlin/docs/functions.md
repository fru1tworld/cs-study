# 함수

## 함수 선언

**기본 문법:**
```kotlin
fun double(x: Int): Int {
    return 2 * x
}

fun main() {
    println(double(5)) // 10
}
```

**핵심 포인트:**
- `fun` 키워드 사용
- 괄호 안에 명시적 타입과 함께 매개변수 지정
- 필요한 경우 반환 타입 포함

## 함수 사용

**표준 함수 호출:**
```kotlin
val result = double(2)
```

**멤버/확장 함수:**
```kotlin
Stream().read()
```

## 매개변수

**선언 (파스칼 표기법):**
```kotlin
fun powerOf(number: Int, exponent: Int): Int { /*...*/ }
```

**핵심 규칙:**
- 매개변수는 읽기 전용(`val`)
- 쉼표로 매개변수 구분
- 쉬운 리팩토링을 위해 후행 쉼표 허용:
```kotlin
fun powerOf(
    number: Int,
    exponent: Int, // 후행 쉼표
) { /*...*/ }
```

## 기본값이 있는 매개변수

**기본 문법:**
```kotlin
fun read(
    b: ByteArray,
    off: Int = 0,
    len: Int = b.size,
) { /*...*/ }
```

**기본값 규칙:**
- 기본값이 있는 매개변수 뒤에는 명명된 인수가 와야 함
- 예외: 후행 람다는 이 규칙을 따르지 않아도 됨

**예제:**
```kotlin
fun greeting(
    userId: Int = 0,
    message: String,
) { /*...*/ }

fun main() {
    greeting(message = "Hello!") // 유효 - 기본 userId 사용
    // greeting("Hello!") // 오류: 'userId'에 값이 전달되지 않음
}
```

**후행 람다 (예외):**
```kotlin
fun greeting(
    userId: Int = 0,
    message: () -> Unit,
) { /*...*/ }

greeting() { println("Hello!") } // 유효
```

**비상수 기본값:**
```kotlin
fun read(
    b: ByteArray,
    off: Int = 0,
    len: Int = b.size, // 다른 매개변수 참조 가능
) { /*...*/ }
```

**참고:** 다른 매개변수를 참조하는 매개변수는 순서상 더 뒤에 선언되어야 합니다.

## 명명된 인수

**사용:**
```kotlin
fun reformat(
    str: String,
    normalizeCase: Boolean = true,
    upperCaseFirstLetter: Boolean = true,
    divideByCamelHumps: Boolean = false,
    wordSeparator: Char = ' ',
) { /*...*/ }

// 명명된 인수 사용
reformat(
    "String!",
    normalizeCase = false,
    upperCaseFirstLetter = false,
    divideByCamelHumps = true,
    '_'
)

// 모든 기본값 건너뛰기
reformat("This is a long String!")

// 일부 기본값 건너뛰기 (후속 인수는 명명해야 함)
reformat(
    "This is a short String!",
    upperCaseFirstLetter = false,
    wordSeparator = '_'
)
```

**명명된 인수와 가변 인수:**
```kotlin
fun mergeStrings(vararg strings: String) { /*...*/ }
mergeStrings(strings = arrayOf("a", "b", "c"))
```

## 반환 타입

**규칙:**
- 블록 본문 함수는 명시적 반환 타입 필요 (`Unit` 제외)
- 단일 표현식 함수는 반환 타입 생략 가능
- 복잡한 제어 흐름에는 추론 없음

## 단일 표현식 함수

**기본 문법:**
```kotlin
fun double(x: Int): Int = x * 2

// 컴파일러가 반환 타입 추론
fun double(x: Int) = x * 2
```

**명시적 타입이 필요한 경우:**
- 재귀 또는 상호 재귀 함수
- 타입 없는 표현식이 있는 함수: `fun empty() = null`

## Unit 반환 함수

**문법:**
```kotlin
fun printHello(name: String?, action: () -> Unit) {
    if (name != null) println("Hello $name")
    else println("Hi there!")
    action()
}

fun main() {
    printHello("Kodee") { println("This runs after the greeting.") }
    // Hello Kodee
    // This runs after the greeting.
}
```

**참고:** `Unit` 반환 타입을 지정하거나 명시적으로 반환할 필요 없음.

**표현식 본문 사용:**
```kotlin
fun getDisplayNameOrDefault(userId: String?): String =
    getDisplayName(userId ?: return "default")
```

## 가변 인수 (Varargs)

**문법:**
```kotlin
fun <T> asList(vararg ts: T): List<T> {
    val result = ArrayList<T>()
    for (t in ts) // ts는 Array
        result.add(t)
    return result
}

val list = asList(1, 2, 3) // [1, 2, 3]
```

**스프레드 연산자:**
```kotlin
val a = arrayOf(1, 2, 3)
val list = asList(-1, 0, *a, 4) // [-1, 0, 1, 2, 3, 4]
```

**기본 타입 배열:**
```kotlin
val a = intArrayOf(1, 2, 3)
val list = asList(-1, 0, *a.toTypedArray(), 4)
```

**규칙:**
- `vararg` 매개변수는 하나만 허용
- 마지막이 아닌 vararg의 경우 후속 매개변수에 명명된 인수 필요

## 중위 표기법

**선언:**
```kotlin
infix fun Int.shl(x: Int): Int { /*...*/ }

// 표준 호출
1.shl(2)

// 중위 호출
1 shl 2
```

**요구사항:**
- 멤버 또는 확장 함수여야 함
- 단일 매개변수만
- 매개변수는 `vararg`이거나 기본값을 가질 수 없음

**우선순위:**
```kotlin
1 shl 2 + 3        // ≡ 1 shl (2 + 3)
0 until n * 2      // ≡ 0 until (n * 2)
xs union ys as Set // ≡ xs union (ys as Set<*>)
```

**명시적 수신자 필요:**
```kotlin
class MyStringCollection {
    val items = mutableListOf<String>()

    infix fun add(s: String) {
        println("Adding: $s")
        items += s
    }

    fun build() {
        add("first")            // 올바름: 일반 호출
        this add "second"       // 올바름: 명시적 수신자와 중위
        // add "third"          // 오류: 명시적 수신자 필요
    }
}
```

## 함수 스코프

함수는 다음에서 선언될 수 있습니다:
- 파일의 최상위 수준
- 지역적으로 (다른 함수 내부)
- 멤버 함수로
- 확장 함수로

## 지역 함수

**깊이 우선 탐색 예제:**
```kotlin
class Person(val name: String) {
    val friends = mutableListOf<Person>()
}

class SocialGraph(val people: List<Person>)

fun dfs(graph: SocialGraph) {
    fun dfs(current: Person, visited: MutableSet<Person>) {
        if (!visited.add(current)) return
        println("Visited ${current.name}")
        for (friend in current.friends)
            dfs(friend, visited)
    }
    dfs(graph.people[0], HashSet())
}
```

**클로저 접근:**
```kotlin
fun dfs(graph: SocialGraph) {
    val visited = HashSet<Person>()
    fun dfs(current: Person) {
        if (!visited.add(current)) return
        println("Visited ${current.name}")
        for (friend in current.friends)
            dfs(friend)
    }
    dfs(graph.people[0])
}
```

## 멤버 함수

**선언:**
```kotlin
class Sample {
    fun foo() {
        print("Foo")
    }
}

Stream().read()
```

## 제네릭 함수

**문법:**
```kotlin
fun <T> singletonList(item: T): List<T> { /*...*/ }
```

## 꼬리 재귀 함수

**문법:**
```kotlin
import kotlin.math.cos
import kotlin.math.abs

val eps = 1E-10

tailrec fun findFixPoint(x: Double = 1.0): Double =
    if (abs(x - cos(x)) < eps) x else findFixPoint(cos(x))
```

**동등한 루프 버전:**
```kotlin
private fun findFixPoint(): Double {
    var x = 1.0
    while (true) {
        val y = cos(x)
        if (abs(x - y) < eps) return x
        x = cos(x)
    }
}
```

**요구사항:**
- 컴파일러가 재귀를 루프로 최적화
- 함수는 마지막 연산으로 자신을 호출해야 함
- 재귀 호출 후 코드를 사용할 수 없음
- `try`/`catch`/`finally` 블록에서 사용할 수 없음
- `open`일 수 없음

## 관련 문서

- [인라인 함수](inline-functions.md)
- [확장 함수](extensions.md)
- [고차 함수와 람다](lambdas.md)
- [클래스](classes.md)
- [상속](inheritance.md)
- [제네릭](generics.md)
