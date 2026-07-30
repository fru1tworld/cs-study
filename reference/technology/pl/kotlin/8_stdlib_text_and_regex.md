# 코틀린 표준 라이브러리: 문자열과 정규표현식

## 문자열 검사와 변환 함수 (kotlin.text)

## kotlin.text 패키지 개요

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/

`kotlin.text`는 문자열, `CharSequence`, 정규식을 다루는 함수와 타입을 모아둔 패키지다. 대부분 별도 임포트 없이 바로 쓸 수 있고, 크게 다음 네 갈래로 나눌 수 있다.

- **판별(predicate) 함수** — `isBlank`, `isEmpty`, `all` 처럼 조건을 검사해 `Boolean`을 돌려주는 함수들
- **파싱/변환 함수** — `toIntOrNull`처럼 문자열을 다른 타입으로 바꾸되 실패를 예외 대신 값으로 표현하는 함수들
- **문자 분류 타입** — `CharCategory`, `CharDirectionality` 등 유니코드 문자를 범주화하는 열거형
- **변형·빌드 함수** — `trim`, `padStart`, `buildString` 등 문자열을 가공하거나 새로 만드는 함수들

이 문서는 이 중 실무에서 가장 자주 쓰이는 **판별 함수**와 **파싱 함수**, 그리고 그 기반이 되는 `CharCategory`를 중심으로 정리한다.

### 판별 함수 요약

| 함수 | 설명 |
|---|---|
| `isEmpty()` | 길이가 0인지 확인 |
| `isNotEmpty()` | 길이가 0이 아닌지 확인 |
| `isBlank()` | 비어 있거나 공백 문자로만 구성되어 있는지 확인 |
| `isNotBlank()` | 공백이 아닌 문자를 하나라도 포함하는지 확인 |
| `isNullOrEmpty()` | 수신자가 `null`이거나 빈 문자열인지 확인 |
| `isNullOrBlank()` | 수신자가 `null`이거나 공백뿐인지 확인 |
| `all(predicate)` | 모든 문자가 조건을 만족하는지 확인 |
| `any()` / `any(predicate)` | 비어 있지 않은지 / 조건을 만족하는 문자가 있는지 확인 |
| `contains(...)` | 특정 문자·부분 문자열 포함 여부 확인 |

`isNullOrEmpty()`, `isNullOrBlank()`는 수신자 타입이 `CharSequence?`라서, 널 검사와 빈 값 검사를 한 번에 처리할 수 있다는 점이 특징이다.

### 파싱 함수 요약

| 함수 | 설명 |
|---|---|
| `toInt()` / `toIntOrNull()` | `Int`로 파싱 (실패 시 예외 / `null`) |
| `toLong()` / `toLongOrNull()` | `Long`으로 파싱 |
| `toDouble()` / `toDoubleOrNull()` | `Double`로 파싱 |
| `toFloat()` / `toFloatOrNull()` | `Float`로 파싱 |
| `toByte()` / `toByteOrNull()` | `Byte`로 파싱 |
| `toShort()` / `toShortOrNull()` | `Short`로 파싱 |
| `toBoolean()` | `Boolean`으로 파싱 (`"true"`만 대소문자 무시하고 `true`) |
| `digitToInt()` / `digitToIntOrNull()` | 문자 하나를 숫자값으로 변환 (진법 지정 가능) |
| `digitToChar()` | 숫자값을 문자로 변환 |

이름 규칙이 일관적이다: `toXxx()`는 실패 시 예외를 던지고, `toXxxOrNull()`은 실패 시 `null`을 반환한다. 사용자 입력처럼 형식이 보장되지 않는 문자열을 다룰 때는 `OrNull` 계열을 쓰고 안전 호출·엘비스 연산자와 조합하는 것이 관용적이다.

---

## isBlank / isNotBlank / isNullOrBlank

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/is-blank.html

### 시그니처와 동작

```kotlin
fun CharSequence.isBlank(): Boolean
```

`isBlank()`는 문자열이 비어 있거나(`length == 0`), 모든 문자가 `Char.isWhitespace()`를 만족할 때 `true`를 반환한다. `isEmpty()`는 길이만 보지만 `isBlank()`는 공백·탭·개행까지 "내용 없음"으로 취급한다는 점이 다르다.

```kotlin
"".isBlank()        // true  (비어 있음)
"   ".isBlank()     // true  (공백뿐)
" a ".isBlank()     // false (문자 포함)

"".isEmpty()        // true
"   ".isEmpty()     // false (길이는 0이 아님)
```

### 반대 함수와 널 안전 버전

- `isNotBlank()` — `isBlank()`의 반대
- `isNullOrBlank()` — 수신자가 `CharSequence?`일 때, `null`이거나 `isBlank()`면 `true`

```kotlin
fun requireName(name: String) {
    if (name.isBlank()) throw IllegalArgumentException("이름은 비워둘 수 없습니다")
}

fun greet(name: String?) {
    if (name.isNullOrBlank()) {
        println("이름 없음")
        return
    }
    println("안녕하세요, $name")
}
```

폼 입력값 검증, 옵셔널 텍스트 필드 체크처럼 "값이 실질적으로 비어 있는가"를 판단할 때는 `isEmpty()`보다 `isBlank()` 계열이 더 적합하다.

---

## toIntOrNull과 안전한 숫자 파싱

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/to-int-or-null.html

### 시그니처

```kotlin
fun String.toIntOrNull(): Int?
fun String.toIntOrNull(radix: Int): Int?
```

문자열을 정수로 해석해 `Int`를 반환하고, 유효한 정수 표현이 아니면 `null`을 반환한다. 예외를 던지는 `toInt()`와 달리 실패를 값으로 표현하므로 `try/catch` 없이 파싱할 수 있다.

### 파싱 규칙

- 앞에 `+`/`-` 부호가 올 수 있다
- 숫자만 허용되며, 공백·언더스코어(`_`) 구분자는 허용되지 않는다
- 결과가 `Int` 범위(`Int.MIN_VALUE`..`Int.MAX_VALUE`)를 벗어나면 `null`

```kotlin
"42".toIntOrNull()          // 42
"-7".toIntOrNull()          // -7
"2147483648".toIntOrNull()  // null (Int.MAX_VALUE 초과)
" 42 ".toIntOrNull()        // null (공백 허용 안 됨)
"1_000".toIntOrNull()       // null (언더스코어 허용 안 됨)
```

`radix`를 지정하면 10진법이 아닌 다른 진법으로 파싱할 수 있다. 유효하지 않은 `radix`(2~36 범위 밖)를 넘기면 `IllegalArgumentException`이 발생한다.

```kotlin
"ff".toIntOrNull(radix = 16)   // 255
"101".toIntOrNull(radix = 2)   // 5
```

### 실전 패턴

`toIntOrNull()`은 엘비스 연산자와 짝을 이뤄 기본값 처리나 조기 반환에 자주 쓰인다.

```kotlin
fun parsePage(query: String?): Int {
    return query?.toIntOrNull() ?: 1  // 파싱 실패 시 1페이지로 대체
}

fun readAge(input: String): Int? {
    val age = input.toIntOrNull() ?: return null
    return if (age in 0..150) age else null
}
```

같은 규칙이 `toLongOrNull`, `toDoubleOrNull`, `toFloatOrNull`, `toByteOrNull`, `toShortOrNull`에도 그대로 적용된다(부동소수점 계열은 `radix` 매개변수가 없다).

---

## CharCategory — 유니코드 문자 분류

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-char-category/

### 개요

```kotlin
expect enum class CharCategory : Enum<CharCategory>
```

`CharCategory`는 유니코드 표준의 "일반 카테고리(General Category)"를 나타내는 열거형이다. 각 항목은 유니코드 명세의 두 글자 코드(`code: String`)와 대응한다. 예를 들어 대문자는 `"Lu"`, 공백 구분자는 `"Zs"`다.

핵심 함수는 `contains(char: Char): Boolean`으로, 특정 문자가 해당 카테고리에 속하는지 검사한다.

```kotlin
CharCategory.UPPERCASE_LETTER.contains('A')  // true
CharCategory.DECIMAL_DIGIT_NUMBER.contains('7')  // true
```

### 주요 카테고리 그룹

| 그룹 | 항목 (유니코드 코드) |
|---|---|
| 문자(Letter) | `UPPERCASE_LETTER`(Lu), `LOWERCASE_LETTER`(Ll), `TITLECASE_LETTER`(Lt), `MODIFIER_LETTER`(Lm), `OTHER_LETTER`(Lo) |
| 결합 기호(Mark) | `NON_SPACING_MARK`(Mn), `COMBINING_SPACING_MARK`(Mc), `ENCLOSING_MARK`(Me) |
| 숫자(Number) | `DECIMAL_DIGIT_NUMBER`(Nd), `LETTER_NUMBER`(Nl), `OTHER_NUMBER`(No) |
| 구분자(Separator) | `SPACE_SEPARATOR`(Zs), `LINE_SEPARATOR`(Zl), `PARAGRAPH_SEPARATOR`(Zp) |
| 구두점(Punctuation) | `DASH_PUNCTUATION`(Pd), `START_PUNCTUATION`(Ps), `END_PUNCTUATION`(Pe), `CONNECTOR_PUNCTUATION`(Pc), `OTHER_PUNCTUATION`(Po), `INITIAL_QUOTE_PUNCTUATION`(Pi), `FINAL_QUOTE_PUNCTUATION`(Pf) |
| 기호(Symbol) | `MATH_SYMBOL`(Sm), `CURRENCY_SYMBOL`(Sc), `MODIFIER_SYMBOL`(Sk), `OTHER_SYMBOL`(So) |
| 기타 | `CONTROL`(Cc), `FORMAT`(Cf), `PRIVATE_USE`(Co), `SURROGATE`(Cs), `UNASSIGNED`(Cn) |

### Char 확장 함수와의 관계

`Char.isLetter()`, `Char.isDigit()`, `Char.isWhitespace()` 같은 익숙한 확장 함수들은 내부적으로 `CharCategory` 분류를 활용해 구현되어 있다. 카테고리를 직접 다뤄야 하는 경우는 흔치 않지만, 유니코드 기반의 세밀한 문자 분류(예: 통화 기호만 걸러내기, 특정 문장부호 범주만 허용하기)가 필요할 때 유용하다.

```kotlin
fun isCurrencySymbol(c: Char): Boolean =
    CharCategory.CURRENCY_SYMBOL.contains(c)

isCurrencySymbol('$')  // true
isCurrencySymbol('원')  // false
```

### 정리

- `isBlank()`/`isNullOrBlank()`는 "내용이 실질적으로 비어 있는가"를 검사하는 표준적인 방법이다. `isEmpty()`와 혼동하지 말 것.
- `toIntOrNull()` 계열은 예외 기반 파싱(`toInt()`) 대신 널 기반 파싱을 제공해, 사용자 입력 등 신뢰할 수 없는 문자열을 다룰 때 안전하다.
- `CharCategory`는 유니코드 일반 카테고리를 표현하며, `Char`의 각종 `isXxx()` 판별 함수를 뒷받침하는 저수준 분류 체계다.

---

## 문자열 분할, 결합, 서식 함수

## split - 문자열 분할

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/split.html

### 개요

`split`은 구분자를 기준으로 문자열을 나누어 `List<String>`으로 반환하는 함수입니다. 구분자로 문자열, 문자, 정규식 세 가지를 받을 수 있고 각각 여러 개(`vararg`)를 동시에 지정할 수 있습니다.

```kotlin
fun CharSequence.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String>
fun CharSequence.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String>
fun CharSequence.split(regex: Regex, limit: Int = 0): List<String>
```

- `ignoreCase`: 대소문자 구분 여부 (기본 `false`)
- `limit`: 반환할 최대 조각 수. `0`이면 무제한이며, 초과분은 마지막 조각에 그대로 남습니다.

### 예시

```kotlin
"apple,banana;cherry".split(",", ";")        // [apple, banana, cherry]
"a-b-c-d-e".split("-", limit = 3)            // [a, b, c-d-e]
"apple123banana456cherry".split(Regex("\\d+"))  // [apple, banana, cherry]
```

### 주의할 동작

- 구분자가 여러 개면 **앞에 나열된 것부터 우선 매칭**됩니다.
- 문자열이 구분자로 끝나면 결과 리스트 끝에 빈 문자열이 남습니다.
- 빈 구분자(`""`)를 넘기면 문자 단위로 쪼갭니다.
- Java의 `Pattern.split()`과 달리 뒤쪽 빈 문자열을 자동으로 제거하지 않습니다.

```kotlin
"##a##b##".split("##")   // [, a, b, ]
"abcXYZdef".split("xyz", ignoreCase = true)  // [abc, def]
```

### 관련 함수

여러 줄 문자열을 다룰 때는 `lines()`(즉시 리스트 반환)와 `lineSequence()`(지연 시퀀스 반환)를 사용합니다. 내부적으로는 개행 문자(`\r\n`, `\n`, `\r`) 기준으로 `split`을 수행하는 것과 같습니다.

---

## trim - 앞뒤 문자 제거

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/trim.html

### 개요

`trim`은 문자열 **양 끝**의 문자를 제거합니다. 세 가지 방식으로 "제거할 문자"를 지정할 수 있습니다.

```kotlin
fun String.trim(): String                          // 공백 제거
fun String.trim(vararg chars: Char): String        // 지정한 문자들 제거
fun String.trim(predicate: (Char) -> Boolean): String  // 조건에 맞는 문자 제거
```

앞쪽만 지울 때는 `trimStart`, 뒤쪽만 지울 때는 `trimEnd`를 쓰면 됩니다. 세 함수 모두 동일한 세 가지 오버로드(인자 없음/`vararg Char`/`predicate`) 구조를 갖습니다.

```kotlin
"  hello  ".trim()               // "hello"
"##hello##".trim('#')            // "hello"
"123hello456".trim { it.isDigit() }  // "hello"

"  hello  ".trimStart()          // "hello  "
"  hello  ".trimEnd()            // "  hello"
```

### 여러 줄 문자열과의 차이

`trimIndent()` / `trimMargin()`은 이 문서의 범위 밖이지만, 이름이 비슷해 혼동하기 쉽습니다. `trim` 계열은 문자열 **양 끝**의 개별 문자를 지우는 것이고, `trimIndent`/`trimMargin`은 여러 줄 문자열의 **공통 들여쓰기**를 제거하는 별도 함수입니다.

---

## padStart / padEnd - 문자열 패딩

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/pad-start.html

### 시그니처

```kotlin
fun String.padStart(length: Int, padChar: Char = ' '): String
fun String.padEnd(length: Int, padChar: Char = ' '): String
```

- `length`: 결과 문자열이 최소한 가져야 할 길이
- `padChar`: 채울 문자 (기본값은 공백)

원본 문자열이 이미 `length`보다 길거나 같으면 그대로 반환됩니다. 즉 `padStart`/`padEnd`는 문자열을 **자르지 않고 늘리기만** 합니다.

### 예시

```kotlin
"125".padStart(5)         // "  125"
"a".padStart(5, '.')      // "....a"
"7".padEnd(3, '0')        // "700"
"abcde".padStart(3)       // "abcde" (변화 없음)
```

숫자를 고정 자릿수로 맞추거나(`"7".padStart(2, '0')` → `"07"`), 표 형태로 텍스트를 정렬할 때 자주 사용합니다.

---

## StringBuilder - 가변 문자열

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-string-builder/

### 왜 필요한가

`String`은 불변이라 `+`로 계속 이어붙이면 매번 새 객체가 생성됩니다. 반복문 안에서 문자열을 조립할 때는 `StringBuilder`로 하나의 버퍼에 누적하는 편이 훨씬 효율적입니다. JVM에서는 `java.lang.StringBuilder`의 typealias이고, 다른 플랫폼에서는 별도 구현이 제공되는 `expect`/`actual` 클래스입니다.

### 생성자

```kotlin
StringBuilder()                 // 빈 빌더
StringBuilder(capacity = 16)    // 초기 용량 지정
StringBuilder("초기 내용")       // 초기 문자열 지정
```

### 주요 연산

| 함수 | 역할 |
|---|---|
| `append(value)` | 끝에 추가 (Char, String, Int 등 다양한 타입 지원) |
| `appendLine(value)` | 값 추가 후 개행(`\n`) |
| `insert(offset, value)` | 지정 위치에 삽입 |
| `deleteAt(index)` / `deleteRange(start, end)` | 문자(구간) 삭제 |
| `setCharAt(index, char)` | 특정 위치 문자 교체 |
| `reverse()` | 순서 뒤집기 |
| `clear()` | 내용 비우기 |
| `toString()` | 최종 불변 `String`으로 변환 |

### 예시

```kotlin
val sb = StringBuilder()
for (i in 1..3) sb.append(i).append(", ")
sb.deleteRange(sb.length - 2, sb.length)  // 마지막 ", " 제거
println(sb.toString())  // "1, 2, 3"
```

`append`를 체이닝하면 자기 자신(`StringBuilder`)을 반환하므로 메서드 호출을 이어 붙일 수 있습니다.

---

## replace - 치환

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/replace.html

### 오버로드 정리

```kotlin
fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String
fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String
fun CharSequence.replace(regex: Regex, replacement: String): String
fun CharSequence.replace(regex: Regex, transform: (MatchResult) -> CharSequence): String
```

문자/문자열 버전은 모든 일치 항목을 한 번에 바꾸며, `ignoreCase`로 대소문자 무시 여부를 정할 수 있습니다. 정규식 버전은 매치마다 고정 문자열(그룹 참조 `$1`, `${그룹이름}` 포함)로 바꾸거나, `transform` 람다로 `MatchResult`를 받아 동적으로 치환 결과를 계산할 수 있습니다.

### 예시

```kotlin
"Mississippi".replace('s', 'z')                       // "Mizzizzippi"
"data is missing".replace("data", "info")             // "info is missing"

val dateRegex = Regex("""(\d{2})-(\d{2})-(\d{4})""")
"15-09-2024".replace(dateRegex, "$3-$2-$1")            // "2024-09-15"

"hello world".replace(Regex("\\w+")) { it.value.uppercase() }  // "HELLO WORLD"
```

`$` 문자를 치환 문자열 그대로 쓰고 싶다면 `\\$`로 이스케이프해야 그룹 참조로 해석되지 않습니다.

### 참고: replaceFirst

`replace`와 별개로 `replaceFirst(regex, replacement)`는 **첫 번째 매치**만 바꾸는 정규식 전용 함수입니다. 전체 치환이 아닌 한 곳만 고칠 때 사용합니다.

---

## 정규표현식

## Kotlin 정규표현식 (Regex)

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-regex/

### 개요

Kotlin은 `java.util.regex`를 감싼 `Regex` 클래스를 표준 라이브러리(`kotlin.text` 패키지)로 제공한다. 문자열 리터럴을 매번 컴파일하는 자바 스타일 대신, `Regex` 객체를 한 번 만들어 재사용하는 방식으로 설계되어 있다. JVM뿐 아니라 JS, Native, Wasm 등 모든 Kotlin 플랫폼에서 동일한 시그니처로 동작한다.

### Regex 생성

생성자는 세 가지 형태가 있다.

```kotlin
val r1 = Regex("\\d+")
val r2 = Regex("hello", RegexOption.IGNORE_CASE)
val r3 = Regex("hello", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
```

문자열 확장 함수 `String.toRegex()`로도 만들 수 있다.

```kotlin
val r = "[a-z]+".toRegex()
```

특수문자가 섞인 문자열을 있는 그대로(리터럴로) 매칭하고 싶을 때는 직접 이스케이프하는 대신 아래 companion 함수를 쓴다.

```kotlin
Regex.escape("1+1=2")          // "1\+1\=2" 형태로 이스케이프
Regex.fromLiteral("1+1=2")     // "1+1=2" 문자열 자체를 리터럴로 매칭하는 Regex
Regex.escapeReplacement("$1")  // replace()의 치환 문자열에서 특수문자를 이스케이프
```

`pattern: String`과 `options: Set<RegexOption>` 프로퍼티로 생성 당시의 패턴 문자열과 옵션 집합을 다시 조회할 수 있다.

### 매칭 여부 확인

| 함수 | 의미 |
|---|---|
| `matches(input)` | 입력 문자열 **전체**가 패턴과 일치하는지 |
| `containsMatchIn(input)` | 입력 어딘가에 일치하는 부분이 **하나라도** 있는지 |
| `matchesAt(input, index)` | 지정한 인덱스 위치에서 정확히 매칭되는지 |

```kotlin
val digits = Regex("\\d+")
digits.matches("123")          // true
digits.matches("123abc")       // false, 전체가 아니므로
digits.containsMatchIn("abc123def") // true
```

`matches`는 중위(infix) 함수라 `regex matches "123"`처럼도 쓸 수 있다.

### 매치 결과 얻기

일치 부분의 상세 정보(위치, 캡처 그룹 등)가 필요하면 `MatchResult`를 반환하는 함수를 쓴다.

- `find(input, startIndex = 0)`: 시작 위치 이후 **첫 번째** 매치. 없으면 `null`.
- `findAll(input, startIndex = 0)`: 모든 매치를 담은 `Sequence<MatchResult>`. 지연 계산이라 큰 텍스트에서도 효율적이다.
- `matchEntire(input)`: 입력 **전체**가 패턴과 일치할 때만 `MatchResult`, 아니면 `null`.
- `matchAt(input, index)`: 지정 인덱스에서 시작하는 매치, 없으면 `null`.

```kotlin
val num = Regex("\\d+")
val first = num.find("가격 100원, 세일 20%")
println(first?.value)   // "100"

num.findAll("x=1, y=22, z=333")
    .map { it.value }
    .forEach(::println)  // "1", "22", "333" 순서로 출력
```

### 치환 (replace)

- `replace(input, replacement)`: 매치된 모든 부분을 고정 문자열로 치환. `$1`, `${name}` 형태로 캡처 그룹 참조 가능.
- `replace(input) { matchResult -> ... }`: 매치마다 람다를 호출해 그 결과 문자열로 치환. 매치별로 다른 로직을 적용할 때 유용하다.
- `replaceFirst(input, replacement)`: 첫 번째 매치만 치환.

```kotlin
val phone = Regex("(\\d{3})-(\\d{4})-(\\d{4})")
phone.replace("010-1234-5678", "$1-****-$3") // "010-****-5678"

Regex("\\d+").replace("a1b22c333") { m -> "[${m.value.length}]" }
// "a[1]b[2]c[3]"

Regex("\\d+").replaceFirst("a1b2c3", "X") // "aXb2c3"
```

### 분리 (split)

- `split(input, limit = 0)`: 매치 부분을 구분자 삼아 자른 `List<String>`. `limit`이 0보다 크면 그만큼의 조각으로 제한한다.
- `splitToSequence(input, limit = 0)`: 결과를 지연 시퀀스로 받는 버전.

```kotlin
Regex("\\s+").split("a   b  c") // ["a", "b", "c"]
Regex(",").split("a,b,c,d", limit = 2) // ["a", "b,c,d"]
```

### RegexOption

`Regex`를 만들 때 두 번째 인자로 넘기는 옵션 플래그로, 여러 개를 `Set`으로 조합할 수 있다.

| 옵션 | 설명 | 플랫폼 |
|---|---|---|
| `IGNORE_CASE` | 대소문자 구분 없이 매칭(유니코드 인식) | 전체 |
| `MULTILINE` | `^`, `$`가 각 줄의 시작/끝에도 매칭되는 멀티라인 모드 | 전체 |
| `LITERAL` | 패턴을 정규식이 아닌 리터럴 문자열로 취급 | JVM/Native/Wasm |
| `UNIX_LINES` | `\n`만 줄바꿈으로 인식 | JVM/Native/Wasm |
| `COMMENTS` | 패턴 내 공백과 주석(`#`) 허용 | JVM/Native/Wasm |
| `DOT_MATCHES_ALL` | `.`이 줄바꿈 문자까지 포함해 모든 문자에 매칭 | JVM/Native/Wasm |
| `CANON_EQ` | 유니코드 정준 분해 기준으로 동등성 비교 | JVM/Native/Wasm |

```kotlin
val r = Regex("hello", RegexOption.IGNORE_CASE)
r.matches("HELLO") // true
```

JS 등 일부 플랫폼에는 없는 옵션도 있고 향후 옵션이 추가될 수도 있으므로, `RegexOption`을 다루는 `when` 문에서는 `else`를 생략하지 않는 편이 안전하다.

## MatchResult

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-match-result/

`find`, `findAll`, `matchEntire` 등이 반환하는 `MatchResult`는 한 번의 매치에 대한 상세 정보를 담은 인터페이스다.

### 주요 프로퍼티

| 프로퍼티 | 타입 | 설명 |
|---|---|---|
| `value` | `String` | 매치된 부분 문자열 |
| `range` | `IntRange` | 원본 문자열에서 매치가 차지하는 인덱스 범위 |
| `groups` | `MatchGroupCollection` | 캡처 그룹들의 컬렉션(인덱스로 접근, 0번은 전체 매치) |
| `groupValues` | `List<String>` | 각 그룹의 값만 뽑아낸 리스트 |
| `destructured` | `MatchResult.Destructured` | 구조 분해 선언용 래퍼 |

### 그룹 다루기

```kotlin
val m = Regex("(\\w+)@(\\w+)\\.com").find("문의: kim@example.com")!!
m.value            // "kim@example.com"
m.groupValues[1]   // "kim"
m.groupValues[2]   // "example"
m.groups[1]?.value // "kim" (없는 그룹이면 null)
```

### destructured로 구조 분해

캡처 그룹이 여러 개일 때 `groupValues` 인덱싱 대신 `destructured`를 쓰면 변수 이름으로 바로 받을 수 있다.

```kotlin
val (user, domain) = Regex("(\\w+)@(\\w+)\\.com")
    .find("kim@example.com")!!
    .destructured

println("$user / $domain") // "kim / example"
```

### next()로 다음 매치 순회

`find()`로 얻은 `MatchResult`는 `next()`를 호출해 그다음 매치로 이어갈 수 있다(내부적으로 `findAll`이 이 방식을 활용한다).

```kotlin
var m = Regex("\\d+").find("1 22 333")
while (m != null) {
    println(m.value)
    m = m.next()
}
// 1
// 22
// 333
```

### 실전 팁

- 매칭 여부만 필요하면 `matches`/`containsMatchIn`, 위치·그룹 정보까지 필요하면 `find`/`findAll`을 쓴다.
- 같은 패턴을 반복 사용할 때는 함수 내부에서 `Regex(...)`를 매번 생성하지 말고 최상위나 companion object의 `val`로 캐싱해 재사용한다.
- 사용자 입력을 그대로 정규식 일부로 끼워 넣어야 할 때는 `Regex.escape()`로 특수문자를 이스케이프해 의도치 않은 패턴 해석을 방지한다.
