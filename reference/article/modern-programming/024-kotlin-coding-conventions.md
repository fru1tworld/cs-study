# Kotlin 코딩 컨벤션

> 원문: [Coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
> 저자: JetBrains (Kotlin 공식 문서) | 번역일: 2026-07-10

널리 알려져 있고 따르기 쉬운 코딩 컨벤션은 어떤 프로그래밍 언어에서든 필수다.
여기서는 Kotlin을 사용하는 프로젝트의 코드 스타일과 코드 구성에 관한 가이드라인을 제공한다.

## IDE에서 스타일 설정하기

Kotlin에서 가장 인기 있는 두 IDE인 [IntelliJ IDEA](https://www.jetbrains.com/idea/)와 [Android Studio](https://developer.android.com/studio/)는
강력한 코드 스타일 지원을 제공한다. 주어진 코드 스타일에 맞게 코드를 자동으로 포맷하도록 설정할 수 있다.

### 스타일 가이드 적용하기

1. **Settings/Preferences | Editor | Code Style | Kotlin**으로 이동한다.
2. **Set from...** 옵션을 클릭한다.
3. **Kotlin style guide**를 선택한다.

### 코드가 스타일 가이드를 따르는지 검증하기

1. **Settings/Preferences | Editor | Inspections | General**로 이동한다.
2. **Incorrect formatting** 인스펙션을 켠다.
스타일 가이드에 기술된 다른 사항(예: 네이밍 컨벤션)을 검증하는 추가 인스펙션은 기본으로 활성화되어 있다.

자세한 내용은 [IntelliJ IDEA로 Kotlin 코드 스타일로 마이그레이션하기](https://kotlinlang.org/docs/code-style-migration-guide.html) 가이드를 참고한다.

## 소스 코드 구성

### 디렉터리 구조

순수 Kotlin 프로젝트에서 권장하는 디렉터리 구조는 공통 루트 패키지를 생략한 패키지 구조를 따른다.
예를 들어 프로젝트의 모든 코드가 `org.example.kotlin` 패키지와 그 하위 패키지에 있다면,
`org.example.kotlin` 패키지의 파일은 소스 루트 바로 아래에 두고,
`org.example.kotlin.network.socket`의 파일은 소스 루트의 `network/socket` 하위 디렉터리에 두어야 한다.

> JVM에서: Kotlin을 Java와 함께 사용하는 프로젝트에서는 Kotlin 소스 파일을 Java 소스 파일과 같은
> 소스 루트에 두고 같은 디렉터리 구조를 따라야 한다. 각 파일은 각 package 문에 대응하는
> 디렉터리에 저장해야 한다.

### 소스 파일 이름

Kotlin 파일에 단일 클래스나 인터페이스(관련 최상위 선언이 함께 있을 수 있다)가 들어 있다면, 파일 이름은 그 클래스의 이름과 같아야 하고 `.kt` 확장자를 붙인다. 이 규칙은 모든 종류의 클래스와 인터페이스에 적용된다.
파일에 여러 클래스가 있거나 최상위 선언만 있다면, 파일이 담고 있는 내용을 설명하는 이름을 골라 그에 맞게 파일 이름을 정한다.
각 단어의 첫 글자를 대문자로 쓰는 [upper camel case](https://en.wikipedia.org/wiki/Camel_case)를 사용한다.
예: `ProcessDeclarations.kt`.

파일 이름은 파일 안의 코드가 무엇을 하는지 설명해야 한다. 따라서 `Util`처럼 의미 없는 단어는 파일 이름에 쓰지 않는다.

#### 멀티플랫폼 프로젝트

멀티플랫폼 프로젝트에서 플랫폼별 소스 세트에 최상위 선언이 있는 파일은 소스 세트 이름과 연관된 접미사를 붙여야 한다. 예를 들면:

* **jvm**Main/kotlin/Platform.**jvm**.kt
* **android**Main/kotlin/Platform.**android**.kt
* **ios**Main/kotlin/Platform.**ios**.kt

공통 소스 세트의 경우 최상위 선언이 있는 파일에 접미사를 붙이지 않는다. 예: `commonMain/kotlin/Platform.kt`.

##### 기술적 배경

멀티플랫폼 프로젝트에서 이 파일 네이밍 방식을 권장하는 이유는 JVM의 제약 때문이다. JVM은 최상위 멤버(함수, 프로퍼티)를 허용하지 않는다.

이를 우회하기 위해 Kotlin JVM 컴파일러는 최상위 멤버 선언을 담는 래퍼 클래스(이른바 "파일 파사드")를 만든다. 파일 파사드는 파일 이름에서 파생한 내부 이름을 가진다.

한편 JVM은 완전 수식 이름(FQN)이 같은 클래스를 여러 개 허용하지 않는다. 이 때문에 Kotlin 프로젝트가 JVM으로 컴파일되지 못하는 상황이 생길 수 있다.

```none
root
|- commonMain/kotlin/myPackage/Platform.kt // 'fun count() { }' 포함
|- jvmMain/kotlin/myPackage/Platform.kt // 'fun multiply() { }' 포함
```

여기서 두 `Platform.kt` 파일은 같은 패키지에 있으므로 Kotlin JVM 컴파일러는 두 개의 파일 파사드를 생성하는데, 둘 다 FQN이 `myPackage.PlatformKt`가 된다. 그 결과 "Duplicate JVM classes" 오류가 발생한다.

이를 피하는 가장 간단한 방법은 위 가이드라인에 따라 파일 중 하나의 이름을 바꾸는 것이다. 이 네이밍 방식은 코드 가독성을 유지하면서 충돌을 피하는 데 도움이 된다.

> 이 권장 사항이 불필요해 보일 수 있는 두 가지 시나리오가 있지만, 그래도 따르기를 권한다.
>
> * JVM이 아닌 플랫폼에서는 파일 파사드 중복 문제가 없다. 그래도 이 네이밍 방식은 파일 이름을 일관되게 유지하는 데 도움이 된다.
> * JVM에서 소스 파일에 최상위 선언이 없으면 파일 파사드가 생성되지 않으므로 이름 충돌을 겪지 않는다.
>
>   그래도 이 네이밍 방식은 단순한 리팩터링이나 코드 추가로 최상위 함수가 생겨 같은 "Duplicate JVM classes" 오류가 발생하는 상황을 예방해 준다.

### 소스 파일 구성

여러 선언(클래스, 최상위 함수 또는 프로퍼티)을 같은 Kotlin 소스 파일에 두는 것은, 그 선언들이 의미적으로 서로 밀접하게 연관되어 있고 파일 크기가 적당한 수준(수백 줄을 넘지 않는 정도)이라면 권장한다.

특히, 어떤 클래스의 모든 클라이언트에 유효한 확장 함수를 정의할 때는 그 클래스와 같은 파일에 둔다. 특정 클라이언트에만 의미 있는 확장 함수를 정의할 때는 그 클라이언트의 코드 옆에 둔다. 어떤 클래스의 모든 확장을 모아 두기 위한 파일을 만들지 않는다.

### 클래스 레이아웃

클래스의 내용은 다음 순서로 배치한다.

1. 프로퍼티 선언과 초기화 블록
2. 보조 생성자
3. 메서드 선언
4. companion object

메서드 선언을 알파벳순이나 가시성순으로 정렬하지 말고, 일반 메서드와 확장 메서드를 분리하지도 말자. 대신 관련 있는 것끼리 모아 두어, 클래스를 위에서 아래로 읽는 사람이 흐름의 논리를 따라갈 수 있게 한다. 순서를 하나 정해서(상위 수준 내용을 먼저 두든, 그 반대든) 일관되게 유지한다.

중첩 클래스는 그 클래스를 사용하는 코드 옆에 둔다. 클래스가 외부에서 사용될 목적이고 클래스 내부에서 참조되지 않는다면, 맨 끝의 companion object 뒤에 둔다.

### 인터페이스 구현 레이아웃

인터페이스를 구현할 때는 구현하는 멤버들을 인터페이스 멤버와 같은 순서로 유지한다(필요하다면 구현에 사용하는 private 메서드를 사이사이에 끼워 넣는다).

### 오버로드 레이아웃

클래스에서 오버로드는 항상 서로 붙여서 둔다.

## 네이밍 규칙

Kotlin의 패키지와 클래스 네이밍 규칙은 꽤 단순하다.

* 패키지 이름은 항상 소문자이고 밑줄을 쓰지 않는다(`org.example.project`). 여러 단어로 된 이름은 일반적으로 권장하지 않지만, 여러 단어가 꼭 필요하다면 그냥 이어 붙이거나 camel case를 쓸 수 있다(`org.example.myProject`).

* 클래스와 객체 이름은 upper camel case를 사용한다.

```kotlin
open class DeclarationProcessor { /*...*/ }

object EmptyDeclarationProcessor : DeclarationProcessor() { /*...*/ }
```

### 함수 이름

함수, 프로퍼티, 지역 변수 이름은 소문자로 시작하고 밑줄 없는 camel case를 사용한다.

```kotlin
fun processDeclarations() { /*...*/ }
var declarationCount = 1
```

예외: 클래스 인스턴스를 만드는 팩터리 함수는 추상 반환 타입과 같은 이름을 가질 수 있다.

```kotlin
interface Foo { /*...*/ }

class FooImpl : Foo { /*...*/ }

fun Foo(): Foo { return FooImpl() }
```

### 테스트 메서드 이름

테스트에서는(그리고 **오직** 테스트에서만) 백틱으로 감싸 공백이 들어간 메서드 이름을 쓸 수 있다.
이런 메서드 이름은 Android 런타임에서는 API 레벨 30부터만 지원된다는 점에 유의한다.
테스트 코드에서는 메서드 이름에 밑줄을 쓰는 것도 허용된다.

```kotlin
class MyTestCase {
    @Test fun `ensure everything works`() { /*...*/ }

    @Test fun ensureEverythingWorks_onAndroid() { /*...*/ }
}
```

### 프로퍼티 이름

상수(`const`로 표시된 프로퍼티, 또는 커스텀 `get` 함수가 없고 깊이 불변인 데이터를 담는 최상위/object `val` 프로퍼티)의 이름은 [screaming snake case](https://en.wikipedia.org/wiki/Snake_case) 컨벤션에 따라 모두 대문자에 밑줄로 구분한 이름을 사용한다.

```kotlin
const val MAX_COUNT = 8
val USER_NAME_FIELD = "UserName"
```

동작을 가진 객체나 가변 데이터를 담는 최상위/object 프로퍼티 이름은 camel case를 사용한다.

```kotlin
val mutableCollection: MutableSet<String> = HashSet()
```

싱글턴 객체에 대한 참조를 담는 프로퍼티 이름은 `object` 선언과 같은 네이밍 스타일을 쓸 수 있다.

```kotlin
val PersonComparator: Comparator<Person> = /*...*/
```

enum 상수는 용도에 따라 모두 대문자에 밑줄로 구분한([screaming snake case](https://en.wikipedia.org/wiki/Snake_case)) 이름(`enum class Color { RED, GREEN }`)을 써도 되고 upper camel case를 써도 된다.

### backing 프로퍼티 이름

개념적으로는 같지만 하나는 공개 API의 일부이고 다른 하나는 구현 세부 사항인 두 프로퍼티가 클래스에 있다면, private 프로퍼티 이름 앞에 밑줄을 접두사로 붙인다.

```kotlin
class C {
    private val _elementList = mutableListOf<Element>()

    val elementList: List<Element>
        get() = _elementList
}
```

### 좋은 이름 고르기

클래스 이름은 보통 그 클래스가 무엇*인지*를 설명하는 명사 또는 명사구다: `List`, `PersonReader`.

메서드 이름은 보통 그 메서드가 무엇을 *하는지* 말해 주는 동사 또는 동사구다: `close`, `readPersons`.
이름은 메서드가 객체를 변경하는지, 새 객체를 반환하는지도 암시해야 한다. 예를 들어 `sort`는 컬렉션을 제자리에서 정렬하고, `sorted`는 정렬된 사본을 반환한다.

이름은 그 엔터티의 목적이 무엇인지 분명히 드러내야 하므로, 이름에 의미 없는 단어(`Manager`, `Wrapper`)를 쓰지 않는 것이 좋다.

선언 이름에 약어를 쓸 때는 다음 규칙을 따른다.

* 두 글자 약어는 두 글자 모두 대문자로 쓴다. 예: `IOStream`.
* 세 글자 이상의 약어는 첫 글자만 대문자로 쓴다. 예: `XmlFormatter`, `HttpInputStream`.

## 포맷팅

### 들여쓰기

들여쓰기는 공백 네 칸을 사용한다. 탭은 사용하지 않는다.

중괄호의 경우, 여는 중괄호는 구문이 시작되는 줄 끝에 두고, 닫는 중괄호는 여는 구문과 수평으로 정렬된 별도의 줄에 둔다.

```kotlin
if (elements != null) {
    for (element in elements) {
        // ...
    }
}
```

> Kotlin에서는 세미콜론이 선택 사항이므로 줄바꿈이 의미를 가진다. 언어 설계는 Java 스타일 중괄호를
> 전제로 하며, 다른 포맷팅 스타일을 쓰려고 하면 예상치 못한 동작을 겪을 수 있다.

### 수평 공백

* 이항 연산자 앞뒤에 공백을 둔다(`a + b`). 예외: "range to" 연산자 앞뒤에는 공백을 두지 않는다(`0..i`).
* 단항 연산자 앞뒤에는 공백을 두지 않는다(`a++`).
* 흐름 제어 키워드(`if`, `when`, `for`, `while`)와 그에 대응하는 여는 괄호 사이에 공백을 둔다.
* 주 생성자 선언, 메서드 선언, 메서드 호출에서는 여는 괄호 앞에 공백을 두지 않는다.

```kotlin
class A(val x: Int)

fun foo(x: Int) { ... }

fun bar() {
    foo(1)
}
```

* `(`, `[` 뒤나 `]`, `)` 앞에는 절대 공백을 두지 않는다.
* `.`이나 `?.` 앞뒤에는 절대 공백을 두지 않는다: `foo.bar().filter { it > 2 }.joinToString()`, `foo?.bar()`.
* `//` 뒤에는 공백을 둔다: `// This is a comment`.
* 타입 파라미터를 지정하는 홑화살괄호 앞뒤에는 공백을 두지 않는다: `class Map<K, V> { ... }`.
* `::` 앞뒤에는 공백을 두지 않는다: `Foo::class`, `String::length`.
* nullable 타입을 표시하는 `?` 앞에는 공백을 두지 않는다: `String?`.

일반 원칙으로, 어떤 형태의 수평 정렬도 피한다. 식별자를 길이가 다른 이름으로 바꿔도 선언이나 사용처의 포맷팅에 영향을 주지 않아야 한다.

### 콜론

다음 경우에는 `:` 앞에 공백을 둔다.

* 타입과 상위 타입을 구분할 때.
* 상위 클래스 생성자 또는 같은 클래스의 다른 생성자에 위임할 때.
* `object` 키워드 뒤에서.

선언과 그 타입을 구분하는 `:` 앞에는 공백을 두지 않는다.

`:` 뒤에는 항상 공백을 둔다.

```kotlin
abstract class Foo<out T : Any> : IFoo {
    abstract fun foo(a: Int): T
}

class FooImpl : Foo() {
    constructor(x: String) : this(x) { /*...*/ }

    val x = object : IFoo { /*...*/ } 
}
```

### 클래스 헤더

주 생성자 파라미터가 몇 개 없는 클래스는 한 줄로 쓸 수 있다.

```kotlin
class Person(id: Int, name: String)
```

헤더가 더 긴 클래스는 각 주 생성자 파라미터가 들여쓰기와 함께 각각의 줄에 오도록 포맷한다.
또한 닫는 괄호는 새 줄에 둔다. 상속을 사용한다면 상위 클래스 생성자 호출이나 구현 인터페이스 목록을 닫는 괄호와 같은 줄에 둔다.

```kotlin
class Person(
    id: Int,
    name: String,
    surname: String
) : Human(id, name) { /*...*/ }
```

인터페이스가 여러 개라면 상위 클래스 생성자 호출을 먼저 두고, 각 인터페이스는 서로 다른 줄에 둔다.

```kotlin
class Person(
    id: Int,
    name: String,
    surname: String
) : Human(id, name),
    KotlinMaker { /*...*/ }
```

상위 타입 목록이 긴 클래스는 콜론 뒤에서 줄을 바꾸고 모든 상위 타입 이름을 수평으로 정렬한다.

```kotlin
class MyFavouriteVeryLongClassHolder :
    MyLongHolder<MyFavouriteVeryLongClass>(),
    SomeOtherInterface,
    AndAnotherOne {

    fun foo() { /*...*/ }
}
```

클래스 헤더가 길 때 헤더와 본문을 명확히 구분하려면, 클래스 헤더 다음에 빈 줄을 넣거나(위 예제처럼) 여는 중괄호를 별도의 줄에 둔다.

```kotlin
class MyFavouriteVeryLongClassHolder :
    MyLongHolder<MyFavouriteVeryLongClass>(),
    SomeOtherInterface,
    AndAnotherOne 
{
    fun foo() { /*...*/ }
}
```

생성자 파라미터에는 일반 들여쓰기(공백 네 칸)를 사용한다. 이렇게 하면 주 생성자에서 선언한 프로퍼티가 클래스 본문에서 선언한 프로퍼티와 같은 들여쓰기를 가진다.

### 수정자 순서

선언에 수정자가 여러 개라면 항상 다음 순서로 둔다.

```kotlin
public / protected / private / internal
expect / actual
final / open / abstract / sealed / const
external
override
lateinit
tailrec
vararg
suspend
inner
enum / annotation / fun // `fun interface`의 수정자로서
companion
inline / value
infix
operator
data
```

모든 어노테이션은 수정자 앞에 둔다.

```kotlin
@Named("Foo")
private val foo: Foo
```

라이브러리를 만드는 경우가 아니라면 불필요한 수정자(예: `public`)는 생략한다.

### 어노테이션

어노테이션은 그것이 붙는 선언 앞의 별도 줄에, 같은 들여쓰기로 둔다.

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude
```

인자가 없는 어노테이션은 같은 줄에 둘 수 있다.

```kotlin
@JsonExclude @JvmField
var x: String
```

인자가 없는 단일 어노테이션은 대응하는 선언과 같은 줄에 둘 수 있다.

```kotlin
@Test fun foo() { /*...*/ }
```

### 파일 어노테이션

파일 어노테이션은 (있다면) 파일 주석 뒤, `package` 문 앞에 두고, `package`와는 빈 줄로 구분한다(패키지가 아니라 파일을 대상으로 한다는 사실을 강조하기 위해).

```kotlin
/** License, copyright and whatever */
@file:JvmName("FooBar")

package foo.bar
```

### 함수

함수 시그니처가 한 줄에 들어가지 않으면 다음 문법을 사용한다.

```kotlin
fun longMethodName(
    argument: ArgumentType = defaultValue,
    argument2: AnotherArgumentType,
): ReturnType {
    // body
}
```

함수 파라미터에는 일반 들여쓰기(공백 네 칸)를 사용한다. 생성자 파라미터와의 일관성을 지키는 데 도움이 된다.

본문이 단일 식으로 이루어진 함수에는 식 본문을 사용하는 것이 좋다.

```kotlin
fun foo(): Int {     // 나쁨
    return 1 
}

fun foo() = 1        // 좋음
```

### 식 본문

함수의 식 본문 첫 줄이 선언과 같은 줄에 들어가지 않으면, `=` 기호를 첫 줄에 두고 식 본문을 네 칸 들여쓴다.

```kotlin
fun f(x: String, y: String, z: String) =
    veryLongFunctionCallWithManyWords(andLongParametersToo(), x, y, z)
```

### 프로퍼티

아주 단순한 읽기 전용 프로퍼티는 한 줄 포맷팅을 고려한다.

```kotlin
val isEmpty: Boolean get() = size == 0
```

더 복잡한 프로퍼티는 `get`과 `set` 키워드를 항상 별도의 줄에 둔다.

```kotlin
val foo: String
    get() { /*...*/ }
```

초기화 식이 있는 프로퍼티에서 초기화 식이 길다면, `=` 기호 뒤에서 줄을 바꾸고 초기화 식을 네 칸 들여쓴다.

```kotlin
private val defaultCharset: Charset? =
    EncodingRegistry.getInstance().getDefaultCharsetForPropertiesFiles(file)
```

### 흐름 제어문

`if`나 `when` 문의 조건이 여러 줄이면 문의 본문에 항상 중괄호를 쓴다.
조건의 이어지는 각 줄은 문의 시작 기준으로 네 칸 들여쓴다.
조건의 닫는 괄호는 여는 중괄호와 함께 별도의 줄에 둔다.

```kotlin
if (!component.isSyncing &&
    !hasAnyKotlinRuntimeInScope(module)
) {
    return createKotlinNotConfiguredPanel(module)
}
```

이렇게 하면 조건과 문 본문이 정렬된다.

`else`, `catch`, `finally` 키워드와 `do-while` 루프의 `while` 키워드는 앞선 중괄호와 같은 줄에 둔다.

```kotlin
if (condition) {
    // body
} else {
    // else part
}

try {
    // body
} finally {
    // cleanup
}
```

`when` 문에서 브랜치가 한 줄을 넘으면, 인접한 case 블록과 빈 줄로 구분하는 것을 고려한다.

```kotlin
private fun parsePropertyValue(propName: String, token: Token) {
    when (token) {
        is Token.ValueToken ->
            callback.visitValue(propName, token.value)

        Token.LBRACE -> { // ...
        }
    }
}
```

짧은 브랜치는 중괄호 없이 조건과 같은 줄에 둔다.

```kotlin
when (foo) {
    true -> bar() // 좋음
    false -> { baz() } // 나쁨
}
```

### 메서드 호출

인자 목록이 길면 여는 괄호 뒤에서 줄을 바꾼다. 인자는 네 칸 들여쓴다.
밀접하게 연관된 여러 인자는 같은 줄에 묶는다.

```kotlin
drawSquare(
    x = 10, y = 10,
    width = 100, height = 100,
    fill = true
)
```

인자 이름과 값을 구분하는 `=` 기호 앞뒤에 공백을 둔다.

### 체이닝 호출 줄바꿈

체이닝된 호출을 줄바꿈할 때는 `.` 문자나 `?.` 연산자를 다음 줄에 두고 한 단계 들여쓴다.

```kotlin
val anchor = owner
    ?.firstChild!!
    .siblings(forward = true)
    .dropWhile { it is PsiComment || it is PsiWhiteSpace }
```

체인의 첫 호출은 보통 그 앞에서 줄을 바꾸지만, 그렇게 하지 않는 편이 코드가 더 자연스럽다면 생략해도 괜찮다.

### 람다

람다 식에서는 중괄호 앞뒤와, 파라미터와 본문을 구분하는 화살표 앞뒤에 공백을 사용한다. 호출이 단일 람다를 받는다면 가능한 한 괄호 바깥으로 전달한다.

```kotlin
list.filter { it > 10 }
```

람다에 레이블을 붙일 때는 레이블과 여는 중괄호 사이에 공백을 두지 않는다.

```kotlin
fun foo() {
    ints.forEach lit@{
        // ...
    }
}
```

여러 줄 람다에서 파라미터 이름을 선언할 때는 이름을 첫 줄에 두고, 그 뒤에 화살표와 줄바꿈을 둔다.

```kotlin
appendCommaSeparated(properties) { prop ->
    val propertyValue = prop.get(obj)  // ...
}
```

파라미터 목록이 너무 길어 한 줄에 들어가지 않으면 화살표를 별도의 줄에 둔다.

```kotlin
foo {
    context: Context,
    environment: Env
    ->
    context.configureEnv(environment)
}
```

### 후행 쉼표

후행 쉼표(trailing comma)는 일련의 요소에서 마지막 항목 뒤에 오는 쉼표 기호다.

```kotlin
class Person(
    val firstName: String,
    val lastName: String,
    val age: Int, // 후행 쉼표
)
```

후행 쉼표를 사용하면 여러 이점이 있다.

* 버전 관리 diff가 더 깔끔해진다 – 변경된 값에만 초점이 맞춰지기 때문이다.
* 요소를 추가하고 재배열하기 쉬워진다 – 요소를 조작할 때 쉼표를 추가하거나 삭제할 필요가 없다.
* 코드 생성이 단순해진다. 예를 들어 객체 초기화 코드에서, 마지막 요소에도 쉼표를 붙일 수 있다.

후행 쉼표는 전적으로 선택 사항이다 – 없어도 코드는 여전히 동작한다. Kotlin 스타일 가이드는 선언부에서 후행 쉼표 사용을 권장하고, 호출부에서는 각자의 판단에 맡긴다.

IntelliJ IDEA 포매터에서 후행 쉼표를 활성화하려면 **Settings/Preferences | Editor | Code Style | Kotlin**으로 가서 **Other** 탭을 열고 **Use trailing comma** 옵션을 선택한다.

#### 열거형

```kotlin
enum class Direction {
    NORTH,
    SOUTH,
    WEST,
    EAST, // 후행 쉼표
}
```

#### 값 인자

```kotlin
fun shift(x: Int, y: Int) { /*...*/ }
shift(
    25,
    20, // 후행 쉼표
)
val colors = listOf(
    "red",
    "green",
    "blue", // 후행 쉼표
)
```

#### 클래스 프로퍼티와 파라미터

```kotlin
class Customer(
    val name: String,
    val lastName: String, // 후행 쉼표
)
class Customer(
    val name: String,
    lastName: String, // 후행 쉼표
)
```

#### 함수 값 파라미터

```kotlin
fun powerOf(
    number: Int, 
    exponent: Int, // 후행 쉼표
) { /*...*/ }
constructor(
    x: Comparable<Number>,
    y: Iterable<Number>, // 후행 쉼표
) {}
fun print(
    vararg quantity: Int,
    description: String, // 후행 쉼표
) {}
```

#### 타입이 선택적인 파라미터 (setter 포함)

```kotlin
val sum: (Int, Int, Int) -> Int = fun(
    x,
    y,
    z, // 후행 쉼표
): Int {
    return x + y + x
}
println(sum(8, 8, 8))
```

#### 인덱싱 접미사

```kotlin
class Surface {
    operator fun get(x: Int, y: Int) = 2 * x + 4 * y - 10
}
fun getZValue(mySurface: Surface, xValue: Int, yValue: Int) =
    mySurface[
        xValue,
        yValue, // 후행 쉼표
    ]
```

#### 람다의 파라미터

```kotlin
fun main() {
    val x = {
            x: Comparable<Number>,
            y: Iterable<Number>, // 후행 쉼표
        ->
        println("1")
    }
    println(x)
}
```

#### when 항목

```kotlin
fun isReferenceApplicable(myReference: KClass<*>) = when (myReference) {
    Comparable::class,
    Iterable::class,
    String::class, // 후행 쉼표
        -> true
    else -> false
}
```

#### 컬렉션 리터럴 (어노테이션에서)

```kotlin
annotation class ApplicableFor(val services: Array<String>)
@ApplicableFor([
    "serializer",
    "balancer",
    "database",
    "inMemoryCache", // 후행 쉼표
])
fun run() {}
```

#### 타입 인자

```kotlin
fun <T1, T2> foo() {}
fun main() {
    foo<
            Comparable<Number>,
            Iterable<Number>, // 후행 쉼표
            >()
}
```

#### 타입 파라미터

```kotlin
class MyMap<
        MyKey,
        MyValue, // 후행 쉼표
        > {}
```

#### 구조 분해 선언

```kotlin
data class Car(val manufacturer: String, val model: String, val year: Int)
val myCar = Car("Tesla", "Y", 2019)
val (
    manufacturer,
    model,
    year, // 후행 쉼표
) = myCar
val cars = listOf<Car>()
fun printMeanValue() {
    var meanValue: Int = 0
    for ((
        _,
        _,
        year, // 후행 쉼표
    ) in cars) {
        meanValue += year
    }
    println(meanValue/cars.size)
}
printMeanValue()
```

## 문서화 주석

긴 문서화 주석은 여는 `/**`를 별도의 줄에 두고, 이어지는 각 줄을 별표로 시작한다.

```kotlin
/**
 * This is a documentation comment
 * on multiple lines.
 */
```

짧은 주석은 한 줄에 둘 수 있다.

```kotlin
/** This is a short documentation comment. */
```

일반적으로 `@param`과 `@return` 태그는 피한다. 대신 파라미터와 반환 값의 설명을 문서화 주석 본문에 직접 녹여 넣고, 파라미터가 언급되는 곳마다 링크를 추가한다. `@param`과 `@return`은 본문 흐름에 담기 어려운 긴 설명이 필요할 때만 사용한다.

```kotlin
// 이렇게 하지 말 것:

/**
 * Returns the absolute value of the given number.
 * @param number The number to return the absolute value for.
 * @return The absolute value.
 */
fun abs(number: Int): Int { /*...*/ }

// 대신 이렇게 할 것:

/**
 * Returns the absolute value of the given [number].
 */
fun abs(number: Int): Int { /*...*/ }
```

## 불필요한 구문 피하기

일반적으로, Kotlin의 어떤 문법 요소가 선택 사항이고 IDE가 불필요하다고 표시한다면 코드에서 생략해야 한다. 불필요한 문법 요소를 "명확성을 위해서"라며 코드에 남겨 두지 말자.

### Unit 반환 타입

함수가 Unit을 반환하면 반환 타입을 생략한다.

```kotlin
fun foo() { // 여기서 ": Unit"이 생략됐다

}
```

### 세미콜론

세미콜론은 가능한 한 항상 생략한다.

### 문자열 템플릿

단순한 변수를 문자열 템플릿에 넣을 때는 중괄호를 쓰지 않는다. 중괄호는 더 긴 식에만 사용한다.

```kotlin
println("$name has ${children.size} children")
```

달러 기호 문자 `$`를 문자열 리터럴로 취급하려면 [다중 달러 문자열 보간](https://kotlinlang.org/docs/strings.html#multi-dollar-string-interpolation)을 사용한다.

```kotlin
val KClass<*>.jsonSchema : String
    get() = $$"""
        {
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "$id": "https://example.com/product.schema.json",
            "$dynamicAnchor": "meta",
            "title": "$${simpleName ?: qualifiedName ?: "unknown"}",
            "type": "object"
        }
        """
```

## 언어 기능의 관용적 사용

### 불변성

가변 데이터보다 불변 데이터를 우선한다. 초기화 후 변경되지 않는 지역 변수와 프로퍼티는 항상 `var`가 아닌 `val`로 선언한다.

변경되지 않는 컬렉션을 선언할 때는 항상 불변 컬렉션 인터페이스(`Collection`, `List`, `Set`, `Map`)를 사용한다. 팩터리 함수로 컬렉션 인스턴스를 만들 때는 가능하면 항상 불변 컬렉션 타입을 반환하는 함수를 사용한다.

```kotlin
// 나쁨: 변경되지 않을 값에 가변 컬렉션 타입 사용
fun validateValue(actualValue: String, allowedValues: HashSet<String>) { ... }

// 좋음: 대신 불변 컬렉션 타입 사용
fun validateValue(actualValue: String, allowedValues: Set<String>) { ... }

// 나쁨: arrayListOf()는 가변 컬렉션 타입인 ArrayList<T>를 반환한다
val allowedValues = arrayListOf("a", "b", "c")

// 좋음: listOf()는 List<T>를 반환한다
val allowedValues = listOf("a", "b", "c")
```

### 파라미터 기본값

오버로드 함수를 선언하기보다 파라미터 기본값이 있는 함수를 선언하는 편을 택한다.

```kotlin
// 나쁨
fun foo() = foo("a")
fun foo(a: String) { /*...*/ }

// 좋음
fun foo(a: String = "a") { /*...*/ }
```

### 타입 별칭

코드베이스에서 여러 번 사용되는 함수 타입이나 타입 파라미터가 있는 타입이 있다면, 타입 별칭을 정의하는 편이 좋다.

```kotlin
typealias MouseClickHandler = (Any, MouseEvent) -> Unit
typealias PersonIndex = Map<String, Person>
```

이름 충돌을 피하려고 private 또는 internal 타입 별칭을 쓴다면, [패키지와 임포트](https://kotlinlang.org/docs/packages.html)에서 언급하는 `import ... as ...`를 우선 고려한다.

### 람다 파라미터

짧고 중첩되지 않은 람다에서는 파라미터를 명시적으로 선언하는 대신 `it` 컨벤션을 사용하기를 권한다. 파라미터가 있는 중첩 람다에서는 항상 파라미터를 명시적으로 선언한다.

### 람다에서의 return

람다에서 레이블 붙은 return을 여러 개 사용하지 않는다. 람다가 단일 탈출 지점을 갖도록 구조를 바꾸는 것을 고려한다. 그게 불가능하거나 충분히 명확하지 않다면 람다를 익명 함수로 바꾸는 것을 고려한다.

람다의 마지막 문에는 레이블 붙은 return을 쓰지 않는다.

### 이름 있는 인자

메서드가 같은 기본 타입의 파라미터를 여러 개 받을 때, 또는 `Boolean` 타입 파라미터에는, 문맥상 모든 파라미터의 의미가 확실히 분명한 경우가 아니라면 이름 있는 인자 문법을 사용한다.

```kotlin
drawSquare(x = 10, y = 10, width = 100, height = 100, fill = true)
```

### 조건문

`try`, `if`, `when`은 식 형태로 쓰는 편을 택한다.

```kotlin
return if (x) foo() else bar()
```

```kotlin
return when(x) {
    0 -> "zero"
    else -> "nonzero"
}
```

위 방식이 아래보다 낫다.

```kotlin
if (x)
    return foo()
else
    return bar()
```

```kotlin
when(x) {
    0 -> return "zero"
    else -> return "nonzero"
}
```

### if와 when

이분 조건에는 `when`보다 `if`를 우선한다.
예를 들어 `if`로 이렇게 쓴다.

```kotlin
if (x == null) ... else ...
```

`when`으로 이렇게 쓰는 대신에.

```kotlin
when (x) {
    null -> // ...
    else -> // ...
}
```

선택지가 셋 이상이면 `when`을 우선한다.

### when 식의 가드 조건

[가드 조건](https://kotlinlang.org/docs/control-flow.html#guard-conditions-in-when-expressions)이 있는 `when` 식이나 문에서 여러 불리언 식을 결합할 때는 괄호를 사용한다.

```kotlin
when (status) {
    is Status.Ok if (status.info.isEmpty() || status.info.id == null) -> "no information"
}
```

이렇게 쓰는 대신에.

```kotlin
when (status) {
    is Status.Ok if status.info.isEmpty() || status.info.id == null -> "no information"
}
```

### 조건문에서의 nullable Boolean 값

조건문에서 nullable `Boolean`을 써야 한다면 `if (value == true)` 또는 `if (value == false)` 검사를 사용한다.

### 루프

루프보다 고차 함수(`filter`, `map` 등)를 우선한다. 예외: `forEach` (`forEach`의 리시버가 nullable이거나 `forEach`가 더 긴 호출 체인의 일부로 쓰이는 경우가 아니라면 일반 `for` 루프를 우선한다).

여러 고차 함수를 사용하는 복잡한 식과 루프 중에서 선택할 때는, 각 경우에 수행되는 연산의 비용을 이해하고 성능을 염두에 두어야 한다.

### 범위 루프

열린 범위를 순회할 때는 `..<` 연산자를 사용한다.

```kotlin
for (i in 0..n - 1) { /*...*/ }  // 나쁨
for (i in 0..<n) { /*...*/ }  // 좋음
```

### 문자열

문자열 연결보다 문자열 템플릿을 우선한다.

일반 문자열 리터럴에 `\n` 이스케이프 시퀀스를 넣기보다 여러 줄 문자열을 우선한다.

여러 줄 문자열에서 들여쓰기를 유지하려면, 결과 문자열에 내부 들여쓰기가 필요 없으면 `trimIndent`를, 내부 들여쓰기가 필요하면 `trimMargin`을 사용한다.

```kotlin
fun main() {
//sampleStart
    println("""
     Not
     trimmed
     text
     """
    )

    println("""
     Trimmed
     text
     """.trimIndent()
    )

    println()

    val a = """Trimmed to margin text:
            |if(a > 1) {
            |    return a
            |}""".trimMargin()

   println(a)
//sampleEnd
}
```

[Java와 Kotlin의 여러 줄 문자열 차이](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html#use-multiline-strings)를 알아보자.

### 함수 vs 프로퍼티

경우에 따라 인자가 없는 함수는 읽기 전용 프로퍼티와 서로 바꿔 쓸 수 있다.
의미는 비슷하지만, 언제 어느 쪽을 선호할지에 대한 스타일 컨벤션이 있다.

기반 알고리즘이 다음 조건을 만족하면 함수보다 프로퍼티를 우선한다.

* 예외를 던지지 않는다.
* 계산 비용이 싸다(또는 첫 실행 시 캐시된다).
* 객체 상태가 변하지 않았다면 호출할 때마다 같은 결과를 반환한다.

### 확장 함수

확장 함수를 자유롭게 사용하자. 어떤 객체를 주 대상으로 동작하는 함수가 있을 때마다 그 객체를 리시버로 받는 확장 함수로 만드는 것을 고려한다. API 오염을 최소화하기 위해 확장 함수의 가시성을 합리적인 선에서 최대한 제한한다. 필요에 따라 지역 확장 함수, 멤버 확장 함수, private 가시성의 최상위 확장 함수를 사용한다.

### 중위 함수

함수를 `infix`로 선언하는 것은 비슷한 역할을 하는 두 객체에 대해 동작할 때만이다. 좋은 예: `and`, `to`, `zip`.
나쁜 예: `add`.

리시버 객체를 변경하는 메서드는 `infix`로 선언하지 않는다.

### 팩터리 함수

클래스의 팩터리 함수를 선언한다면 클래스와 같은 이름을 붙이지 않는다. 팩터리 함수의 동작이 왜 특별한지 분명히 드러나는 별개의 이름을 우선한다. 정말로 특별한 의미가 없는 경우에만 클래스와 같은 이름을 쓸 수 있다.

```kotlin
class Point(val x: Double, val y: Double) {
    companion object {
        fun fromPolar(angle: Double, radius: Double) = Point(...)
    }
}
```

서로 다른 상위 클래스 생성자를 호출하지 않으며 기본값이 있는 파라미터를 가진 단일 생성자로 줄일 수 없는, 여러 오버로드 생성자를 가진 객체가 있다면, 오버로드 생성자를 팩터리 함수로 바꾸는 편을 택한다.

### 플랫폼 타입

플랫폼 타입의 식을 반환하는 public 함수/메서드는 Kotlin 타입을 명시적으로 선언해야 한다.

```kotlin
fun apiCall(): String = MyJavaApi.getProperty("name")
```

플랫폼 타입의 식으로 초기화되는 모든 프로퍼티(패키지 수준 또는 클래스 수준)는 Kotlin 타입을 명시적으로 선언해야 한다.

```kotlin
class Person {
    val name: String = MyJavaApi.getProperty("name")
}
```

플랫폼 타입의 식으로 초기화되는 지역 값은 타입 선언이 있어도 되고 없어도 된다.

```kotlin
fun main() {
    val name = MyJavaApi.getProperty("name")
    println(name)
}
```

### 스코프 함수 apply/with/run/also/let

Kotlin은 주어진 객체의 컨텍스트에서 코드 블록을 실행하는 함수들을 제공한다: `let`, `run`, `with`, `apply`, `also`.
각 상황에 맞는 스코프 함수를 고르는 지침은 [스코프 함수](https://kotlinlang.org/docs/scope-functions.html)를 참고한다.

## 라이브러리를 위한 코딩 컨벤션

라이브러리를 작성할 때는 API 안정성을 보장하기 위해 다음 규칙을 추가로 따르기를 권한다.

* 멤버 가시성을 항상 명시적으로 지정한다(선언이 의도치 않게 public API로 노출되는 것을 피하기 위해).
* 함수 반환 타입과 프로퍼티 타입을 항상 명시적으로 지정한다(구현이 바뀔 때 반환 타입이 의도치 않게 바뀌는 것을 피하기 위해).
* 새 문서화가 필요 없는 오버라이드를 제외한 모든 public 멤버에 [KDoc](https://kotlinlang.org/docs/kotlin-doc.html) 주석을 제공한다(라이브러리 문서 생성을 지원하기 위해).

라이브러리 API를 작성할 때 고려할 모범 사례와 아이디어는 [라이브러리 작성자 가이드라인](https://kotlinlang.org/docs/api-guidelines-introduction.html)에서 더 알아볼 수 있다.
