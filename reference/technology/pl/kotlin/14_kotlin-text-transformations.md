# 문자열 분할, 결합, 서식 함수

# split - 문자열 분할

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/split.html

## 개요

`split`은 구분자를 기준으로 문자열을 나누어 `List<String>`으로 반환하는 함수입니다. 구분자로 문자열, 문자, 정규식 세 가지를 받을 수 있고 각각 여러 개(`vararg`)를 동시에 지정할 수 있습니다.

```kotlin
fun CharSequence.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String>
fun CharSequence.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String>
fun CharSequence.split(regex: Regex, limit: Int = 0): List<String>
```

- `ignoreCase`: 대소문자 구분 여부 (기본 `false`)
- `limit`: 반환할 최대 조각 수. `0`이면 무제한이며, 초과분은 마지막 조각에 그대로 남습니다.

## 예시

```kotlin
"apple,banana;cherry".split(",", ";")        // [apple, banana, cherry]
"a-b-c-d-e".split("-", limit = 3)            // [a, b, c-d-e]
"apple123banana456cherry".split(Regex("\\d+"))  // [apple, banana, cherry]
```

## 주의할 동작

- 구분자가 여러 개면 **앞에 나열된 것부터 우선 매칭**됩니다.
- 문자열이 구분자로 끝나면 결과 리스트 끝에 빈 문자열이 남습니다.
- 빈 구분자(`""`)를 넘기면 문자 단위로 쪼갭니다.
- Java의 `Pattern.split()`과 달리 뒤쪽 빈 문자열을 자동으로 제거하지 않습니다.

```kotlin
"##a##b##".split("##")   // [, a, b, ]
"abcXYZdef".split("xyz", ignoreCase = true)  // [abc, def]
```

## 관련 함수

여러 줄 문자열을 다룰 때는 `lines()`(즉시 리스트 반환)와 `lineSequence()`(지연 시퀀스 반환)를 사용합니다. 내부적으로는 개행 문자(`\r\n`, `\n`, `\r`) 기준으로 `split`을 수행하는 것과 같습니다.

---

# trim - 앞뒤 문자 제거

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/trim.html

## 개요

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

## 여러 줄 문자열과의 차이

`trimIndent()` / `trimMargin()`은 이 문서의 범위 밖이지만, 이름이 비슷해 혼동하기 쉽습니다. `trim` 계열은 문자열 **양 끝**의 개별 문자를 지우는 것이고, `trimIndent`/`trimMargin`은 여러 줄 문자열의 **공통 들여쓰기**를 제거하는 별도 함수입니다.

---

# padStart / padEnd - 문자열 패딩

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/pad-start.html

## 시그니처

```kotlin
fun String.padStart(length: Int, padChar: Char = ' '): String
fun String.padEnd(length: Int, padChar: Char = ' '): String
```

- `length`: 결과 문자열이 최소한 가져야 할 길이
- `padChar`: 채울 문자 (기본값은 공백)

원본 문자열이 이미 `length`보다 길거나 같으면 그대로 반환됩니다. 즉 `padStart`/`padEnd`는 문자열을 **자르지 않고 늘리기만** 합니다.

## 예시

```kotlin
"125".padStart(5)         // "  125"
"a".padStart(5, '.')      // "....a"
"7".padEnd(3, '0')        // "700"
"abcde".padStart(3)       // "abcde" (변화 없음)
```

숫자를 고정 자릿수로 맞추거나(`"7".padStart(2, '0')` → `"07"`), 표 형태로 텍스트를 정렬할 때 자주 사용합니다.

---

# StringBuilder - 가변 문자열

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-string-builder/

## 왜 필요한가

`String`은 불변이라 `+`로 계속 이어붙이면 매번 새 객체가 생성됩니다. 반복문 안에서 문자열을 조립할 때는 `StringBuilder`로 하나의 버퍼에 누적하는 편이 훨씬 효율적입니다. JVM에서는 `java.lang.StringBuilder`의 typealias이고, 다른 플랫폼에서는 별도 구현이 제공되는 `expect`/`actual` 클래스입니다.

## 생성자

```kotlin
StringBuilder()                 // 빈 빌더
StringBuilder(capacity = 16)    // 초기 용량 지정
StringBuilder("초기 내용")       // 초기 문자열 지정
```

## 주요 연산

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

## 예시

```kotlin
val sb = StringBuilder()
for (i in 1..3) sb.append(i).append(", ")
sb.deleteRange(sb.length - 2, sb.length)  // 마지막 ", " 제거
println(sb.toString())  // "1, 2, 3"
```

`append`를 체이닝하면 자기 자신(`StringBuilder`)을 반환하므로 메서드 호출을 이어 붙일 수 있습니다.

---

# replace - 치환

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/replace.html

## 오버로드 정리

```kotlin
fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String
fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String
fun CharSequence.replace(regex: Regex, replacement: String): String
fun CharSequence.replace(regex: Regex, transform: (MatchResult) -> CharSequence): String
```

문자/문자열 버전은 모든 일치 항목을 한 번에 바꾸며, `ignoreCase`로 대소문자 무시 여부를 정할 수 있습니다. 정규식 버전은 매치마다 고정 문자열(그룹 참조 `$1`, `${그룹이름}` 포함)로 바꾸거나, `transform` 람다로 `MatchResult`를 받아 동적으로 치환 결과를 계산할 수 있습니다.

## 예시

```kotlin
"Mississippi".replace('s', 'z')                       // "Mizzizzippi"
"data is missing".replace("data", "info")             // "info is missing"

val dateRegex = Regex("""(\d{2})-(\d{2})-(\d{4})""")
"15-09-2024".replace(dateRegex, "$3-$2-$1")            // "2024-09-15"

"hello world".replace(Regex("\\w+")) { it.value.uppercase() }  // "HELLO WORLD"
```

`$` 문자를 치환 문자열 그대로 쓰고 싶다면 `\\$`로 이스케이프해야 그룹 참조로 해석되지 않습니다.

## 참고: replaceFirst

`replace`와 별개로 `replaceFirst(regex, replacement)`는 **첫 번째 매치**만 바꾸는 정규식 전용 함수입니다. 전체 치환이 아닌 한 곳만 고칠 때 사용합니다.
