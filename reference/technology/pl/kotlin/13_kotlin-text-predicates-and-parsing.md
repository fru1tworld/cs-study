# 문자열 검사와 변환 함수 (kotlin.text)

# kotlin.text 패키지 개요

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/

`kotlin.text`는 문자열, `CharSequence`, 정규식을 다루는 함수와 타입을 모아둔 패키지다. 대부분 별도 임포트 없이 바로 쓸 수 있고, 크게 다음 네 갈래로 나눌 수 있다.

- **판별(predicate) 함수** — `isBlank`, `isEmpty`, `all` 처럼 조건을 검사해 `Boolean`을 돌려주는 함수들
- **파싱/변환 함수** — `toIntOrNull`처럼 문자열을 다른 타입으로 바꾸되 실패를 예외 대신 값으로 표현하는 함수들
- **문자 분류 타입** — `CharCategory`, `CharDirectionality` 등 유니코드 문자를 범주화하는 열거형
- **변형·빌드 함수** — `trim`, `padStart`, `buildString` 등 문자열을 가공하거나 새로 만드는 함수들

이 문서는 이 중 실무에서 가장 자주 쓰이는 **판별 함수**와 **파싱 함수**, 그리고 그 기반이 되는 `CharCategory`를 중심으로 정리한다.

## 판별 함수 요약

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

## 파싱 함수 요약

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

# isBlank / isNotBlank / isNullOrBlank

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/is-blank.html

## 시그니처와 동작

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

## 반대 함수와 널 안전 버전

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

# toIntOrNull과 안전한 숫자 파싱

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/to-int-or-null.html

## 시그니처

```kotlin
fun String.toIntOrNull(): Int?
fun String.toIntOrNull(radix: Int): Int?
```

문자열을 정수로 해석해 `Int`를 반환하고, 유효한 정수 표현이 아니면 `null`을 반환한다. 예외를 던지는 `toInt()`와 달리 실패를 값으로 표현하므로 `try/catch` 없이 파싱할 수 있다.

## 파싱 규칙

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

## 실전 패턴

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

# CharCategory — 유니코드 문자 분류

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-char-category/

## 개요

```kotlin
expect enum class CharCategory : Enum<CharCategory>
```

`CharCategory`는 유니코드 표준의 "일반 카테고리(General Category)"를 나타내는 열거형이다. 각 항목은 유니코드 명세의 두 글자 코드(`code: String`)와 대응한다. 예를 들어 대문자는 `"Lu"`, 공백 구분자는 `"Zs"`다.

핵심 함수는 `contains(char: Char): Boolean`으로, 특정 문자가 해당 카테고리에 속하는지 검사한다.

```kotlin
CharCategory.UPPERCASE_LETTER.contains('A')  // true
CharCategory.DECIMAL_DIGIT_NUMBER.contains('7')  // true
```

## 주요 카테고리 그룹

| 그룹 | 항목 (유니코드 코드) |
|---|---|
| 문자(Letter) | `UPPERCASE_LETTER`(Lu), `LOWERCASE_LETTER`(Ll), `TITLECASE_LETTER`(Lt), `MODIFIER_LETTER`(Lm), `OTHER_LETTER`(Lo) |
| 결합 기호(Mark) | `NON_SPACING_MARK`(Mn), `COMBINING_SPACING_MARK`(Mc), `ENCLOSING_MARK`(Me) |
| 숫자(Number) | `DECIMAL_DIGIT_NUMBER`(Nd), `LETTER_NUMBER`(Nl), `OTHER_NUMBER`(No) |
| 구분자(Separator) | `SPACE_SEPARATOR`(Zs), `LINE_SEPARATOR`(Zl), `PARAGRAPH_SEPARATOR`(Zp) |
| 구두점(Punctuation) | `DASH_PUNCTUATION`(Pd), `START_PUNCTUATION`(Ps), `END_PUNCTUATION`(Pe), `CONNECTOR_PUNCTUATION`(Pc), `OTHER_PUNCTUATION`(Po), `INITIAL_QUOTE_PUNCTUATION`(Pi), `FINAL_QUOTE_PUNCTUATION`(Pf) |
| 기호(Symbol) | `MATH_SYMBOL`(Sm), `CURRENCY_SYMBOL`(Sc), `MODIFIER_SYMBOL`(Sk), `OTHER_SYMBOL`(So) |
| 기타 | `CONTROL`(Cc), `FORMAT`(Cf), `PRIVATE_USE`(Co), `SURROGATE`(Cs), `UNASSIGNED`(Cn) |

## Char 확장 함수와의 관계

`Char.isLetter()`, `Char.isDigit()`, `Char.isWhitespace()` 같은 익숙한 확장 함수들은 내부적으로 `CharCategory` 분류를 활용해 구현되어 있다. 카테고리를 직접 다뤄야 하는 경우는 흔치 않지만, 유니코드 기반의 세밀한 문자 분류(예: 통화 기호만 걸러내기, 특정 문장부호 범주만 허용하기)가 필요할 때 유용하다.

```kotlin
fun isCurrencySymbol(c: Char): Boolean =
    CharCategory.CURRENCY_SYMBOL.contains(c)

isCurrencySymbol('$')  // true
isCurrencySymbol('원')  // false
```

## 정리

- `isBlank()`/`isNullOrBlank()`는 "내용이 실질적으로 비어 있는가"를 검사하는 표준적인 방법이다. `isEmpty()`와 혼동하지 말 것.
- `toIntOrNull()` 계열은 예외 기반 파싱(`toInt()`) 대신 널 기반 파싱을 제공해, 사용자 입력 등 신뢰할 수 없는 문자열을 다룰 때 안전하다.
- `CharCategory`는 유니코드 일반 카테고리를 표현하며, `Char`의 각종 `isXxx()` 판별 함수를 뒷받침하는 저수준 분류 체계다.
