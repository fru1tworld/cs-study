# 정규표현식

# Kotlin 정규표현식 (Regex)

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-regex/

## 개요

Kotlin은 `java.util.regex`를 감싼 `Regex` 클래스를 표준 라이브러리(`kotlin.text` 패키지)로 제공한다. 문자열 리터럴을 매번 컴파일하는 자바 스타일 대신, `Regex` 객체를 한 번 만들어 재사용하는 방식으로 설계되어 있다. JVM뿐 아니라 JS, Native, Wasm 등 모든 Kotlin 플랫폼에서 동일한 시그니처로 동작한다.

## Regex 생성

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

## 매칭 여부 확인

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

## 매치 결과 얻기

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

## 치환 (replace)

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

## 분리 (split)

- `split(input, limit = 0)`: 매치 부분을 구분자 삼아 자른 `List<String>`. `limit`이 0보다 크면 그만큼의 조각으로 제한한다.
- `splitToSequence(input, limit = 0)`: 결과를 지연 시퀀스로 받는 버전.

```kotlin
Regex("\\s+").split("a   b  c") // ["a", "b", "c"]
Regex(",").split("a,b,c,d", limit = 2) // ["a", "b,c,d"]
```

## RegexOption

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

# MatchResult

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/-match-result/

`find`, `findAll`, `matchEntire` 등이 반환하는 `MatchResult`는 한 번의 매치에 대한 상세 정보를 담은 인터페이스다.

## 주요 프로퍼티

| 프로퍼티 | 타입 | 설명 |
|---|---|---|
| `value` | `String` | 매치된 부분 문자열 |
| `range` | `IntRange` | 원본 문자열에서 매치가 차지하는 인덱스 범위 |
| `groups` | `MatchGroupCollection` | 캡처 그룹들의 컬렉션(인덱스로 접근, 0번은 전체 매치) |
| `groupValues` | `List<String>` | 각 그룹의 값만 뽑아낸 리스트 |
| `destructured` | `MatchResult.Destructured` | 구조 분해 선언용 래퍼 |

## 그룹 다루기

```kotlin
val m = Regex("(\\w+)@(\\w+)\\.com").find("문의: kim@example.com")!!
m.value            // "kim@example.com"
m.groupValues[1]   // "kim"
m.groupValues[2]   // "example"
m.groups[1]?.value // "kim" (없는 그룹이면 null)
```

## destructured로 구조 분해

캡처 그룹이 여러 개일 때 `groupValues` 인덱싱 대신 `destructured`를 쓰면 변수 이름으로 바로 받을 수 있다.

```kotlin
val (user, domain) = Regex("(\\w+)@(\\w+)\\.com")
    .find("kim@example.com")!!
    .destructured

println("$user / $domain") // "kim / example"
```

## next()로 다음 매치 순회

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

## 실전 팁

- 매칭 여부만 필요하면 `matches`/`containsMatchIn`, 위치·그룹 정보까지 필요하면 `find`/`findAll`을 쓴다.
- 같은 패턴을 반복 사용할 때는 함수 내부에서 `Regex(...)`를 매번 생성하지 말고 최상위나 companion object의 `val`로 캐싱해 재사용한다.
- 사용자 입력을 그대로 정규식 일부로 끼워 넣어야 할 때는 `Regex.escape()`로 특수문자를 이스케이프해 의도치 않은 패턴 해석을 방지한다.
