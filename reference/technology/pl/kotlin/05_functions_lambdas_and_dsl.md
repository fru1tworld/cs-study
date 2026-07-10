# 함수, 람다, DSL

# 함수

> **원문:** https://kotlinlang.org/docs/functions.html

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

---

# 고차 함수와 람다

> **원문:** https://kotlinlang.org/docs/lambdas.html

## 개요

Kotlin 함수는 **일급**입니다. 즉 변수에 저장하고, 인수로 전달하고, 다른 함수에서 반환할 수 있습니다. Kotlin은 **함수 타입** 계열과 **람다 표현식** 같은 언어 구조를 사용하여 이를 용이하게 합니다.

## 고차 함수

**고차 함수**는 함수를 매개변수로 받거나 함수를 반환합니다.

### 예제: `fold`
```kotlin
fun <T, R> Collection<T>.fold(
    initial: R,
    combine: (acc: R, nextElement: T) -> R
): R {
    var accumulator: R = initial
    for (element: T in this) {
        accumulator = combine(accumulator, element)
    }
    return accumulator
}
```

### 사용 예제
```kotlin
fun main() {
    val items = listOf(1, 2, 3, 4, 5)

    // 명시적 타입의 람다
    items.fold(0, { acc: Int, i: Int ->
        print("acc = $acc, i = $i, ")
        val result = acc + i
        println("result = $result")
        result
    })

    // 추론된 타입의 람다
    val joinedToString = items.fold("Elements:", { acc, i ->
        acc + " " + i
    })

    // 함수 참조 사용
    val product = items.fold(1, Int::times)
}
```

## 함수 타입

함수 타입은 매개변수와 반환 값이 있는 함수 시그니처를 나타냅니다.

### 문법

- **기본**: `(A, B) -> C` - 타입 A와 B를 받아 C 반환
- **매개변수 없음**: `() -> A`
- **수신자 포함**: `A.(B) -> C`
- **일시 중단**: `suspend () -> Unit` 또는 `suspend A.(B) -> C`
- **널 가능**: `((Int, Int) -> Int)?`
- **타입 별칭**: `typealias ClickHandler = (Button, ClickEvent) -> Unit`

### 함수 타입 인스턴스화

**1. 람다 표현식:**
```kotlin
{ a, b -> a + b }
```

**2. 익명 함수:**
```kotlin
fun(s: String): Int { return s.toIntOrNull() ?: 0 }
```

**3. 호출 가능 참조:**
```kotlin
::isOdd
String::toInt
List<Int>::size
::Regex
```

**4. 함수 타입을 구현하는 커스텀 클래스:**
```kotlin
class IntTransformer: (Int) -> Int {
    override operator fun invoke(x: Int): Int = TODO()
}
val intFunction: (Int) -> Int = IntTransformer()
```

### 함수 타입 호출

```kotlin
val stringPlus: (String, String) -> String = String::plus
val intPlus: Int.(Int) -> Int = Int::plus

println(stringPlus.invoke("<-", "->"))
println(stringPlus("Hello, ", "world!"))
println(intPlus.invoke(1, 1))
println(intPlus(1, 2))
println(2.intPlus(3))  // 확장과 같은 호출
```

## 람다 표현식과 익명 함수

### 람다 표현식 문법

**전체 형식:**
```kotlin
val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
```

**축약 형식:**
```kotlin
val sum = { x: Int, y: Int -> x + y }
```

핵심 포인트:
- 중괄호로 둘러쌈
- `->` 앞에 선택적 타입 어노테이션이 있는 매개변수
- 마지막 표현식이 반환 값

### 후행 람다

마지막 매개변수가 함수면 람다를 괄호 밖에 배치할 수 있습니다:

```kotlin
val product = items.fold(1) { acc, e -> acc * e }
```

람다가 유일한 인수면 괄호를 생략할 수 있습니다:
```kotlin
run { println("...") }
```

### 암시적 `it` 매개변수

단일 매개변수 람다의 경우 암시적 `it`을 사용합니다:

```kotlin
ints.filter { it > 0 }  // 타입: (it: Int) -> Boolean
```

### 람다에서 반환

**암시적 반환** (마지막 표현식):
```kotlin
ints.filter { val shouldFilter = it > 0; shouldFilter }
```

**명시적 한정된 반환:**
```kotlin
ints.filter { val shouldFilter = it > 0; return@filter shouldFilter }
```

### 사용하지 않는 변수에 밑줄

```kotlin
map.forEach { (_, value) -> println("$value!") }
```

### 익명 함수

반환 타입을 명시적으로 지정해야 할 때 사용합니다:

```kotlin
fun(x: Int, y: Int): Int = x + y

// 또는 블록 본문으로
fun(x: Int, y: Int): Int {
    return x + y
}

// filter와 함께 사용
ints.filter(fun(item) = item > 0)
```

**핵심 차이점:** 익명 함수에서 `return`은 둘러싸는 함수가 아닌 함수 자체에서 반환됩니다.

## 클로저

람다는 외부 스코프의 변수에 접근하고 수정할 수 있습니다:

```kotlin
var sum = 0
ints.filter { it > 0 }.forEach { sum += it }
print(sum)
```

## 수신자가 있는 함수 리터럴

수신자 타입 `A.(B) -> C`가 있는 함수는 수신자를 암시적 `this`로 접근할 수 있습니다:

```kotlin
val sum: Int.(Int) -> Int = { other -> plus(other) }

// 수신자가 있는 익명 함수
val sum = fun Int.(other: Int): Int = this + other
```

### 타입 안전 빌더 예제

```kotlin
class HTML {
    fun body() { ... }
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

html {
    body()  // 수신자의 메서드 호출
}
```

---

# 인라인 함수

> **원문:** https://kotlinlang.org/docs/inline-functions.html

## 개요

인라인 함수는 호출 위치에 람다 표현식을 인라인하여 고차 함수의 런타임 패널티를 제거하고, 함수 객체 생성과 클로저 할당 오버헤드를 피합니다.

## 주요 섹션

### 1. 기본 인라인 함수

`inline` 수정자는 컴파일러에게 함수와 그 람다 매개변수 모두를 호출 위치에 인라인하도록 지시합니다.

**예제:**
```kotlin
inline fun <T> lock(lock: Lock, body: () -> T): T { ... }
```

다음 대신:
```kotlin
lock(l) { foo() }
```

컴파일러가 생성:
```kotlin
l.lock()
try {
    foo()
} finally {
    l.unlock()
}
```

### 2. noinline 수정자

다른 것은 인라인하면서 특정 람다 매개변수의 인라인을 방지합니다:

```kotlin
inline fun foo(inlined: () -> Unit, noinline notInlined: () -> Unit) { ... }
```

`noinline` 람다는 필드에 저장하고 자유롭게 전달할 수 있지만, 인라인 가능한 람다는 그렇지 않습니다.

### 3. 비지역 점프 표현식

#### 반환

인라인 함수는 람다에서 `return` 문을 사용하여 둘러싸는 함수를 빠져나갈 수 있습니다:

```kotlin
inline fun inlined(block: () -> Unit) {
    println("hi!")
}

fun foo() {
    inlined {
        return  // OK: foo()를 빠져나감
    }
}
```

#### Break와 Continue

인라인 함수는 람다에서 `break`와 `continue`를 지원합니다:

```kotlin
fun processList(elements: List<Int>): Boolean {
    for (element in elements) {
        val variable = element.nullableMethod() ?: run {
            log.warning("Element is null...")
            continue  // 인라인 함수에서 OK
        }
        if (variable == 0) return true
    }
    return false
}
```

#### crossinline 수정자

람다가 다른 컨텍스트에서 실행될 때 비지역 반환을 금지합니다:

```kotlin
inline fun f(crossinline body: () -> Unit) {
    val f = object: Runnable {
        override fun run() = body()  // 여기서 return 사용 불가
    }
}
```

### 4. 구체화된 타입 매개변수

인라인 함수는 `reified` 타입 매개변수를 가질 수 있어, 리플렉션 없이 런타임에 타입 정보에 접근할 수 있습니다:

```kotlin
inline fun <reified T> TreeNode.findParentOfType(): T? {
    var p = parent
    while (p != null && p !is T) {
        p = p.parent
    }
    return p as T?
}

// Class 매개변수 없이 호출
treeNode.findParentOfType<MyTreeNode>()
```

리플렉션이 필요한 비인라인 버전과 비교:
```kotlin
fun <T> TreeNode.findParentOfType(clazz: Class<T>): T? {
    var p = parent
    while (p != null && !clazz.isInstance(p)) {
        p = p.parent
    }
    return p as T?
}
// 호출에 Class 필요
treeNode.findParentOfType(MyTreeNode::class.java)
```

### 5. 인라인 프로퍼티

`inline` 수정자는 프로퍼티 접근자에 어노테이션할 수 있습니다:

```kotlin
val foo: Foo
    inline get() = Foo()

var bar: Bar
    get() = ...
    inline set(v) { ... }

// 또는 전체 프로퍼티
inline var bar: Bar
    get() = ...
    set(v) { ... }
```

### 6. 공개 API 인라인 함수 제한

- public/protected 인라인 함수는 본문에서 private/internal 선언을 사용할 수 없음
- 바이너리 비호환성 문제 방지
- `@PublishedApi` 어노테이션은 public 인라인 함수에서 internal 선언을 허용합니다:

```kotlin
@PublishedApi
internal class MyClass { ... }

public inline fun useMyClass() {
    // 여기서 MyClass에 접근 가능
}
```

## 성능 고려사항

- 인라인은 런타임 오버헤드를 제거하지만 생성된 코드 크기를 증가시킴
- 적당한 크기의 함수에 신중하게 사용
- 루프 내부의 다형성 호출 위치에서 가장 유익
- 인라인 함수에 인라인 가능한 매개변수가 없으면 컴파일러가 경고 (`@Suppress("NOTHING_TO_INLINE")`로 억제)

---

# Kotlin 스코프 함수

> **원문:** https://kotlinlang.org/docs/scope-functions.html

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

---

# 타입 안전 빌더

> **원문:** https://kotlinlang.org/docs/type-safe-builders.html

## 개요

Kotlin의 타입 안전 빌더는 잘 명명된 함수와 **수신자가 있는 함수 리터럴**을 결합하여 **도메인 특화 언어**(DSL)를 만들 수 있게 합니다. 반선언적 방식으로 복잡한 계층적 데이터 구조를 구축할 수 있습니다.

### 사용 사례

- 마크업 생성 (HTML, XML)
- 웹 서버 라우트 구성 (예: Ktor)

## 작동 방식

### 기본 개념

타입 안전 빌더가 활용하는 것:
1. **수신자가 있는 함수 리터럴** - 특정 타입에서 동작하는 람다 전달
2. **확장 함수** - 태그 클래스에 기능 추가
3. **암시적 수신자** - 명시적 `this` 없이 메서드 호출

### 예제: HTML 빌더

```kotlin
val result = html {
    head {
        title { +"HTML encoding with Kotlin" }
    }
    body {
        h1 { +"HTML encoding with Kotlin" }
        p { +"this format can be used as an alternative markup to HTML" }
        a(href = "http://kotlinlang.org") { +"Kotlin" }
        p {
            +"This is some"
            b { +"mixed" }
            +"text. For more see the"
            a(href = "http://kotlinlang.org") { +"Kotlin" }
            +"project"
        }
        p {
            +"some text"
            ul {
                for (i in 1..5) li { +"${i}*2 = ${i*2}" }
            }
        }
    }
}
```

### 핵심 함수 패턴

```kotlin
fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()  // 람다로 초기화
    return html
}
```

매개변수 타입 `HTML.() -> Unit`은 **수신자가 있는 함수 타입**입니다:
- 수신자: `HTML` 인스턴스
- 반환: `Unit`
- 암시적 `this` 또는 생략을 통해 접근

### 자식 태그 함수

```kotlin
protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
    tag.init()
    children.add(tag)
    return tag
}

fun head(init: Head.() -> Unit) = initTag(Head(), init)
fun body(init: Body.() -> Unit) = initTag(Body(), init)
```

### 텍스트 내용 추가

단항 플러스 연산자(`+`)가 문자열을 래핑합니다:

```kotlin
abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}
```

사용:
```kotlin
title { +"XML encoding with Kotlin" }
```

## 스코프 제어: @DslMarker

### 문제

스코프 제어 없이 중첩된 DSL 컨텍스트는 외부 수신자 메서드 호출을 허용합니다:

```kotlin
html {
    head {
        head { }  // 금지되어야 함!
    }
}
```

### 해결책: @DslMarker 어노테이션

마커 어노테이션 생성:

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker
```

기본 클래스에 적용:

```kotlin
@HtmlTagMarker
abstract class Tag(val name: String) : Element { ... }
```

### 효과

컴파일러가 스코프를 가장 가까운 암시적 수신자로 제한합니다:

```kotlin
html {
    head {
        head { }  // 오류: 외부 수신자의 멤버
    }
}
```

### 명시적 접근

외부 수신자에 접근하려면 한정된 `this`를 사용합니다:

```kotlin
html {
    head {
        this@html.head { }  // 허용됨
    }
}
```

### 함수 타입에 적용

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker

fun html(init: @HtmlTagMarker HTML.() -> Unit): HTML { ... }
fun HTML.head(init: @HtmlTagMarker Head.() -> Unit): Head { ... }
```

## 완전한 패키지 정의

```kotlin
package com.example.html

interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class HtmlTagMarker

@HtmlTagMarker
abstract class Tag(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, indent + " ")
        }
        builder.append("$indent</$name>\n")
    }

    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

class HTML : TagWithText("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)
    fun body(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title : TagWithText("title")

abstract class BodyTag(name: String) : TagWithText(name) {
    fun b(init: B.() -> Unit) = initTag(B(), init)
    fun p(init: P.() -> Unit) = initTag(P(), init)
    fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }
}

class Body : BodyTag("body")
class B : BodyTag("b")
class P : BodyTag("p")
class H1 : BodyTag("h1")

class A : BodyTag("a") {
    var href: String
        get() = attributes["href"]!!
        set(value) { attributes["href"] = value }
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}
```

## 관련 주제

- [this 표현식](lambdas.md)
- [빌더 타입 추론과 함께 빌더 사용](lambdas.md)
