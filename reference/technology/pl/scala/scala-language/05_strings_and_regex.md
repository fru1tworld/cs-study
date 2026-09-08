# Scala 문자열과 정규 표현식

## 문자열과 문자열 보간법

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/strings.html

> 원문: https://docs.scala-lang.org/overviews/core/string-interpolation.html

<a id="1-개요"></a>
### 1. 개요

Scala의 `String`은 Java의 `java.lang.String`을 그대로 사용하면서 두 가지 기능을 더한다.

- 컬렉션처럼 다루기: `map`, `reverse`, `slice` 같은 시퀀스 연산을 문자열에 바로 사용 가능
- 보간법(interpolation): `s"...$name..."`처럼 문자열 리터럴 안에 변수, 표현식을 직접 삽입 가능

두 기능을 이해하려면 문자열 자체의 기능과 컴파일러가 변환해 주는 코드를 함께 봐야 한다.

<a id="2-문자열은-컬렉션처럼-동작한다"></a>

### 2. 문자열은 컬렉션처럼 동작함

`String`(`java.lang.String`)은 `Seq`를 상속하지 않는다. Scala는 두 가지 암시적 변환(implicit conversion)으로 문자열을 컬렉션처럼 다룰 수 있게 한다.

- 낮은 우선순위 변환
  - 대상 타입: `WrappedString`(`immutable.IndexedSeq`의 하위 타입)
  - 역할: 문자열을 진짜 시퀀스 값으로 취급하고 싶을 때 사용
- 높은 우선순위 변환 (기본으로 적용되는 쪽)
  - 대상 타입: `StringOps`
  - 역할: 불변 시퀀스가 가진 거의 모든 메서드를 문자열에 부여

`String` 자체를 컬렉션 클래스로 다시 설계하면 Java와의 상호운용이 깨진다. 그래서 Java 문자열을 유지한 채 암시적 변환으로 감싸 `map`, `filter`, `reverse` 같은 API를 제공한다. 메서드를 호출할 때는 `StringOps`를 거치고, `Seq[Char]` 같은 컬렉션 타입을 요구하는 자리에서는 `WrappedString`으로 변환한다.

<a id="3-컬렉션-연산-활용-예시"></a>

### 3. 컬렉션 연산 활용 예시

- 문자열에 시퀀스 메서드를 그대로 호출 가능

```scala
val str = "hello"

str.reverse         // "olleh"
str.map(_.toUpper)  // "HELLO"
str.drop(3)         // "lo"
str.slice(1, 4)     // "ell"

val chars: Seq[Char] = str   // WrappedString으로 변환되어 Seq[Char]에 대입 가능
```

- `reverse`, `map`, `filter`, `drop`, `take`, `slice`처럼 `List`나 `Vector`에서 쓰던 연산을 문자열에도 그대로 사용
- `Seq[Char]`가 필요한 위치에 문자열을 넘기면 자동 변환
- 결과 타입은 대부분 다시 `String`으로 환원 (`map`의 결과가 `Char`들의 시퀀스이면 `String`으로 재구성)

- 즉 "문자열 전용 메서드(`length`, `substring` 등)"와 "컬렉션이라서 딸려오는 메서드(`reverse`, `map` 등)"를 구분 없이 한 객체에서 사용

<a id="4-문자열-보간법-개요"></a>

### 4. 문자열 보간법 개요

- 문자열 보간법(string interpolation): 문자열 리터럴 앞에 식별자(interpolator id)를 붙여 리터럴을 특별한 방식으로 처리

```scala
val name = "Kim"
val age = 30

s"이름: $name, 나이: $age"   // "이름: Kim, 나이: 30"
```

- Scala가 기본으로 제공하는 보간자:

- `s`
  - 접두어: `s"..."`
  - 용도: 변수, 표현식을 문자열로 치환
- `f`
  - 접두어: `f"..."`
  - 용도: `printf` 스타일의 포맷 지정, 컴파일 타임 타입 검사
- `raw`
  - 접두어: `raw"..."`
  - 용도: `s`와 같되 이스케이프 시퀀스를 해석하지 않음

- 접두어가 없는 일반 문자열 리터럴에서는 `$`가 평범한 문자 → 치환 없음

<a id="5-s-보간자--변수와-표현식-삽입"></a>

### 5. `s` 보간자: 변수와 표현식 삽입

- 가장 기본적인 보간자
  - `$` 뒤에 변수 이름을 쓰면 그 변수의 `toString()` 결과로 치환

```scala
val name = "world"
s"Hello, $name!"          // "Hello, world!"
```

- 단순 변수 이름이 아니라 임의의 표현식을 넣으려면 중괄호로 감쌈

```scala
val a = 3
val b = 4
s"합은 ${a + b}입니다"     // "합은 7입니다"
```

- 주의할 점:

- 문자열 안에서 `$` 문자 자체를 쓰려면 `$$`로 이스케이프
- 삼중 따옴표(`"""..."""`)와 함께 쓰면 문자열 안에 큰따옴표를 그대로 포함 가능

<a id="6-f-보간자--타입-안전한-포맷팅"></a>

### 6. `f` 보간자: 타입 안전한 포맷팅

- C의 `printf`처럼, 각 변수 뒤에 형식 지정자(format specifier)를 붙여 출력 형태를 제어

```scala
val name = "Kim"
val height = 1.9d

f"$name%s의 키는 $height%2.2f미터입니다."   // "Kim의 키는 1.90미터입니다."
```

형식 지정자를 생략하면 `%s`가 적용되며, `%` 자체를 출력하려면 `%%`를 쓴다. 지정자 문법은 Java의 `Formatter` 규칙을 따른다.

`f` 보간자는 지정자와 값의 타입이 맞는지 컴파일할 때 검사한다. 예를 들어 `Double`에 정수용 지정자 `%d`를 쓰면 컴파일 오류가 난다. Java의 `String.format`에서는 런타임에 발견하는 오류를 미리 잡는다는 의미에서 타입 안전(type safe)하다고 부른다.

<a id="7-raw-보간자--이스케이프-없는-원본-문자열"></a>

### 7. `raw` 보간자: 이스케이프 없는 원본 문자열

- `s`와 동일하게 변수 치환은 되지만, 이스케이프 시퀀스를 해석하지 않음

```scala
s"a\nb"     // 개행이 실제로 들어감 → "a" 다음 줄바꿈 "b"
raw"a\nb"   // \n이 그대로 문자 두 개로 남음 → "a\nb"
```

- 정규식 패턴이나 Windows 경로처럼 백슬래시가 많이 등장하는 문자열을 다룰 때 유용

<a id="8-내부-동작-stringcontext로의-디슈가링"></a>

### 8. 내부 동작: `StringContext`로의 디슈가링

- `id"..."` 형태의 보간 문자열은 컴파일러가 다음과 같이 `StringContext`에 대한 메서드 호출로 변환(디슈가링).

```scala
p"1, $someVar"

// 컴파일러가 실제로 만드는 코드
new StringContext("1, ", "").p(someVar)
```

고정된 텍스트 조각은 `StringContext`의 `parts`로 전달되고, `$` 뒤의 표현식은 이어서 호출하는 메서드의 인자가 된다. 위 예제에서는 `p`가 그 메서드다. `s`, `f`, `raw`도 같은 방식으로 `StringContext`에 정의되거나 확장으로 추가된 메서드를 호출한다.

<a id="9-커스텀-보간자-만들기"></a>

### 9. 커스텀 보간자 만들기

- `StringContext`에 메서드를 추가하면 커스텀 보간자를 만들 수 있음
  - Scala 3에서는 확장 메서드(`extension`)로, Scala 2에서는 `implicit class`로 구현

```scala
// Scala 3
extension (sc: StringContext)
  def hi(args: Any*): String =
    sc.parts.mkString("") + "!!! (" + args.mkString(", ") + ")"

val user = "Kim"
hi"안녕 $user"   // StringContext("안녕 ", "").hi(user) 로 디슈가링
```

- `sc.parts`: 텍스트 조각들 (위 예시에서는 `"안녕 "`과 `""`)
- `args`: `$` 뒤에 있던 표현식들의 값

- 이 패턴은 SQL 쿼리 조립, ANSI 색상 코드 삽입 같은 도메인 특화 문법(DSL)을 안전하게 만드는 데 자주 사용

<a id="10-패턴-매칭에서의-보간자"></a>

### 10. 패턴 매칭에서의 보간자

- 보간자는 값을 만드는 것뿐 아니라, `match`의 추출자(extractor)로도 사용 가능
  - `s`와 커스텀 보간자는 기본 추출자를 제공 → `f`와 `raw`는 기본으로 추출자가 없어 패턴 매칭에 쓰려면 직접 구현 필요

```scala
val s"Hello, $name!" = "Hello, world!"
println(name)   // "world"
```

<a id="11-정리"></a>

<a id="12-참고-자료"></a>

### 12. 참고 자료

- [Strings as Collections](https://docs.scala-lang.org/overviews/collections-2.13/strings.html)
- [String Interpolation](https://docs.scala-lang.org/overviews/core/string-interpolation.html)
- [Scala 3 Book: String Interpolation](https://docs.scala-lang.org/scala3/book/string-interpolation.html)

## 정규 표현식

> 원문: https://docs.scala-lang.org/tour/regular-expression-patterns.html

<a id="1-regex-만들기"></a>
### 1. `Regex` 만들기

- Scala는 별도의 정규식 리터럴 문법 없이, 문자열에 `.r` 메서드를 붙이는 것만으로 정규 표현식 객체(`scala.util.matching.Regex`)를 생성

```scala
import scala.util.matching.Regex

val digit: Regex = "[0-9]".r
```

`/regex/` 같은 별도 리터럴 대신 문자열에서 정규식을 만들기 때문에 문자열 보간(`s"..."`)이나 삼중 따옴표(`"""..."""`)도 함께 사용할 수 있다. 특히 백슬래시가 많은 패턴은 삼중 따옴표로 감싸면 이스케이프를 피할 수 있다.

```scala
val phone: Regex = """\d{3}-\d{3}-\d{4}""".r   // 삼중 따옴표: \ 이스케이프 불필요
```

<a id="2-첫-번째-일치-찾기"></a>

### 2. 첫 번째 일치 찾기

- `findFirstMatchIn`: 문자열 안에서 패턴과 처음 일치하는 부분을 찾아 `Option[Match]`로 반환
  - 일치가 없으면 `None` → 결과가 있을 수도 없을 수도 있는 상황을 `Option`으로 안전하게 처리

```scala
val digit: Regex = "[0-9]".r

def hasDigit(password: String): Boolean =
  digit.findFirstMatchIn(password) match
    case Some(_) => true
    case None    => false
```

- 참고: "일치 여부만 필요하고 실제 매치 값은 필요 없는" 상황에서는 굳이 `match`를 쓰지 않고 `digit.findFirstMatchIn(password).isDefined`처럼 `Option`의 메서드로 축약 가능

<a id="3-그룹으로-부분-추출하기"></a>

### 3. 그룹으로 부분 추출하기

- 패턴 일부를 괄호 `( )`로 묶으면 그룹(group)이 되어, 매치 결과에서 해당 부분만 따로 추출 가능

```scala
val keyVal: Regex = "([a-zA-Z-]+):\\s*(.+)".r

val line = "color: red"
keyVal.findFirstMatchIn(line).foreach { m =>
  println(s"key=${m.group(1)}, value=${m.group(2)}")   // key=color, value=red
}
```

- `group(0)`: 전체 일치 문자열
- `group(1)`, `group(2)`, ...: 각 괄호에 대응하는 부분 문자열 (왼쪽부터 순서대로)

- 주의: 그룹 번호는 여는 괄호 `(`가 나온 순서로 매겨짐 → 중첩된 괄호가 있으면 바깥쪽부터 번호가 붙으므로, 패턴이 복잡해지면 그룹 순서를 착각하기 쉬움

<a id="4-모든-일치-순회하기"></a>

### 4. 모든 일치 순회하기

- 문자열 하나에 여러 번 일치하는 경우, `findAllMatchIn`으로 모든 매치를 순회 가능
  - `Iterator[Match]`를 반환 → `for`나 컬렉션 메서드로 처리하기 좋음

```scala
val css: Regex = "([a-zA-Z-]+):\\s*([^;]+);".r

val style = "color: red; margin: 0; padding: 4px;"

for (m <- css.findAllMatchIn(style))
  println(s"${m.group(1)} -> ${m.group(2)}")

// color -> red
// margin -> 0
// padding -> 4px
```

- CSS 선언처럼 "키: 값;"이 여러 번 반복되는 텍스트를 한 번에 파싱할 때 유용한 패턴

<a id="5-match와-결합하기"></a>

### 5. `match`와 결합하기

- 정규식을 `case` 절의 패턴으로 바로 사용 가능
  - 그룹은 자동으로 변수에 바인딩 → 필요 없는 그룹은 `_`로 무시

```scala
val Email: Regex = """^(\w+)@(\w+)\.(\w+)$""".r
val Phone: Regex = """^(\d{3}-\d{3}-\d{4})$""".r

def classify(input: String): String = input match
  case Email(user, domain, _) => s"이메일 (사용자=$user, 도메인=$domain)"
  case Phone(number)          => s"전화번호 ($number)"
  case _                      => "알 수 없는 형식"

classify("kim@example.com")   // 이메일 (사용자=kim, 도메인=example)
classify("010-1234-5678")     // 전화번호 (010-1234-5678)
```

`Regex`는 `unapply`를 구현한 추출자(extractor)이므로 `case class`처럼 패턴 매칭에 사용할 수 있다. `case Email(user, domain, _)`는 문자열이 이메일 형태인지 검사하면서 사용자와 도메인을 추출하고, 필요 없는 마지막 그룹은 `_`로 무시한다.

<a id="6-요약"></a>

### 참고 자료

- [Tour of Scala: Regular Expression Patterns](https://docs.scala-lang.org/tour/regular-expression-patterns.html)
- [`scala.util.matching.Regex` API 문서](https://www.scala-lang.org/api/current/scala/util/matching/Regex.html)
