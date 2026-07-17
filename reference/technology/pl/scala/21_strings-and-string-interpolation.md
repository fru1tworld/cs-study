# 문자열과 문자열 보간법

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/strings.html
> **원문:** https://docs.scala-lang.org/overviews/core/string-interpolation.html

---

## 목차

1. [개요](#1-개요)
2. [문자열은 컬렉션처럼 동작한다](#2-문자열은-컬렉션처럼-동작한다)
3. [컬렉션 연산 활용 예시](#3-컬렉션-연산-활용-예시)
4. [문자열 보간법 개요](#4-문자열-보간법-개요)
5. [`s` 보간자 — 변수와 표현식 삽입](#5-s-보간자--변수와-표현식-삽입)
6. [`f` 보간자 — 타입 안전한 포맷팅](#6-f-보간자--타입-안전한-포맷팅)
7. [`raw` 보간자 — 이스케이프 없는 원본 문자열](#7-raw-보간자--이스케이프-없는-원본-문자열)
8. [내부 동작: `StringContext`로의 디슈가링](#8-내부-동작-stringcontext로의-디슈가링)
9. [커스텀 보간자 만들기](#9-커스텀-보간자-만들기)
10. [패턴 매칭에서의 보간자](#10-패턴-매칭에서의-보간자)
11. [정리](#11-정리)
12. [참고 자료](#12-참고-자료)

---

## 1. 개요

Scala의 `String`은 Java의 `java.lang.String`을 그대로 쓰지만, 두 가지 지점에서 Java 문자열보다 훨씬 편하게 다룰 수 있습니다.

- **컬렉션처럼 다루기**: `map`, `reverse`, `slice` 같은 시퀀스 연산을 문자열에 바로 쓸 수 있습니다.
- **보간법(interpolation)**: `s"...$name..."`처럼 문자열 리터럴 안에 변수·표현식을 직접 끼워 넣을 수 있습니다.

두 기능 모두 "문자열 리터럴을 더 강력한 타입으로 슬쩍 바꿔치기하는" 컴파일러 트릭에 기반합니다. 아래에서 각각을 살펴봅니다.

---

## 2. 문자열은 컬렉션처럼 동작한다

`String`(`java.lang.String`)은 그 자체로는 `Seq`를 상속하지 않습니다. 대신 Scala는 **암시적 변환(implicit conversion) 두 개**를 통해 문자열에 컬렉션 메서드를 얹어 줍니다.

| 변환 | 대상 타입 | 역할 |
|---|---|---|
| 낮은 우선순위 변환 | `WrappedString` (`immutable.IndexedSeq`의 하위 타입) | 문자열을 진짜 시퀀스 값으로 취급하고 싶을 때 사용 |
| 높은 우선순위 변환 | `StringOps` | 불변 시퀀스가 가진 거의 모든 메서드를 문자열에 얹어 줌 (기본으로 적용되는 쪽) |

> 💡 **왜 필요한가**
>
> `String`을 언어 차원에서 컬렉션 클래스로 다시 설계하면 Java와의 상호운용이 깨집니다. 대신 "필요할 때만 컬렉션 메서드가 보이도록" 암시적 변환으로 감싸서, Java 문자열 그대로 두면서도 `map`·`filter`·`reverse` 같은 컬렉션 API를 그대로 쓸 수 있게 한 것입니다. 평소에는 `StringOps`를 통해 메서드가 붙는 것이고, `Seq[Char]` 등 실제 컬렉션 타입이 필요한 자리에서는 `WrappedString`으로 변환됩니다.

---

## 3. 컬렉션 연산 활용 예시

문자열에 그대로 시퀀스 메서드를 호출할 수 있습니다.

```scala
val str = "hello"

str.reverse         // "olleh"
str.map(_.toUpper)  // "HELLO"
str.drop(3)         // "lo"
str.slice(1, 4)     // "ell"

val chars: Seq[Char] = str   // WrappedString으로 변환되어 Seq[Char]에 대입 가능
```

- `reverse`, `map`, `filter`, `drop`, `take`, `slice`처럼 `List`나 `Vector`에서 쓰던 연산을 그대로 문자열에 쓸 수 있습니다.
- `Seq[Char]`가 필요한 위치에 문자열을 넘기면 자동으로 변환됩니다.
- 결과 타입은 대부분 다시 `String`으로 돌아옵니다(`map`의 결과가 `Char`들의 시퀀스이면 `String`으로 재구성됨).

즉 "문자열 전용 메서드(`length`, `substring` 등)"와 "컬렉션이라서 공짜로 딸려오는 메서드(`reverse`, `map` 등)"를 구분 없이 한 객체에서 사용하는 셈입니다.

---

## 4. 문자열 보간법 개요

문자열 보간법(string interpolation)은 문자열 리터럴 앞에 **식별자(interpolator id)** 를 붙여, 그 리터럴을 특별한 방식으로 처리하게 만드는 기능입니다.

```scala
val name = "Kim"
val age = 30

s"이름: $name, 나이: $age"   // "이름: Kim, 나이: 30"
```

Scala가 기본으로 제공하는 보간자는 세 가지입니다.

| 보간자 | 접두어 | 용도 |
|---|---|---|
| `s` | `s"..."` | 변수·표현식을 문자열로 치환 |
| `f` | `f"..."` | `printf` 스타일의 포맷 지정, 컴파일 타임 타입 검사 |
| `raw` | `raw"..."` | `s`와 같되 이스케이프 시퀀스를 해석하지 않음 |

접두어가 없는 일반 문자열 리터럴에서는 `$`가 그냥 평범한 문자일 뿐, 아무 치환도 일어나지 않습니다.

---

## 5. `s` 보간자 — 변수와 표현식 삽입

가장 기본적인 보간자입니다. `$` 뒤에 변수 이름을 쓰면 그 변수의 `toString()` 결과로 치환됩니다.

```scala
val name = "world"
s"Hello, $name!"          // "Hello, world!"
```

단순 변수 이름이 아니라 **임의의 표현식**을 넣고 싶으면 중괄호로 감쌉니다.

```scala
val a = 3
val b = 4
s"합은 ${a + b}입니다"     // "합은 7입니다"
```

주의할 점:

- 문자열 안에서 `$` 문자 자체를 쓰려면 `$$`로 이스케이프합니다.
- 삼중 따옴표(`"""..."""`)와 함께 쓰면 문자열 안에 큰따옴표를 그대로 넣을 수 있습니다.

---

## 6. `f` 보간자 — 타입 안전한 포맷팅

C의 `printf`처럼, 각 변수 뒤에 **형식 지정자(format specifier)** 를 붙여 출력 형태를 제어합니다.

```scala
val name = "Kim"
val height = 1.9d

f"$name%s의 키는 $height%2.2f미터입니다."   // "Kim의 키는 1.90미터입니다."
```

- 변수 뒤에 형식 지정자를 생략하면 `%s`가 기본으로 적용됩니다.
- `%%`로 리터럴 `%` 기호를 표현합니다.
- 가장 큰 특징은 **컴파일 타임 타입 검사**입니다. 예를 들어 `Double` 값에 `%d`(정수용 지정자)를 쓰면 컴파일 오류가 납니다. Java의 `String.format`은 이런 실수를 런타임에야 발견하지만, `f` 보간자는 컴파일 시점에 걸러냅니다.

> ⚠️ **짚고 넘어가기**
>
> "타입 안전(type safe)"하다는 것은 형식 지정자와 값의 타입이 맞는지 컴파일러가 미리 검증해 준다는 뜻이지, 포맷팅 로직 자체가 새로운 것은 아닙니다. 지정자 문법은 Java의 `Formatter` 규칙을 그대로 따릅니다.

---

## 7. `raw` 보간자 — 이스케이프 없는 원본 문자열

`s`와 동일하게 변수 치환은 되지만, **이스케이프 시퀀스를 해석하지 않습니다.**

```scala
s"a\nb"     // 개행이 실제로 들어감 → "a" 다음 줄바꿈 "b"
raw"a\nb"   // \n이 그대로 문자 두 개로 남음 → "a\nb"
```

정규식 패턴이나 Windows 경로처럼 백슬래시가 많이 등장하는 문자열을 다룰 때 유용합니다.

---

## 8. 내부 동작: `StringContext`로의 디슈가링

`id"..."` 형태의 보간 문자열은 컴파일러가 다음과 같이 **`StringContext`에 대한 메서드 호출**로 변환(디슈가링)합니다.

```scala
p"1, $someVar"

// 컴파일러가 실제로 만드는 코드
new StringContext("1, ", "").p(someVar)
```

- 리터럴 사이의 고정된 텍스트 조각들은 `StringContext`의 `parts`로 넘어갑니다.
- `$` 뒤에 있던 표현식들은 그 뒤에 호출되는 메서드(`s`, `f`, `raw`, 혹은 커스텀 이름)의 인자로 전달됩니다.

즉 `s"..."`, `f"..."`, `raw"..."`는 전부 `StringContext`에 정의된(또는 확장으로 추가된) 평범한 메서드일 뿐이며, 언어에 하드코딩된 특수 문법이 아닙니다.

---

## 9. 커스텀 보간자 만들기

`StringContext`에 메서드를 추가하면 나만의 보간자를 만들 수 있습니다. Scala 3에서는 확장 메서드(`extension`)로, Scala 2에서는 `implicit class`로 구현합니다.

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

이 패턴은 SQL 쿼리 조립, ANSI 색상 코드 삽입 같은 도메인 특화 문법(DSL)을 안전하게 만드는 데 자주 쓰입니다.

---

## 10. 패턴 매칭에서의 보간자

보간자는 값을 만드는 것뿐 아니라, `match`의 **추출자(extractor)** 로도 쓸 수 있습니다. `s`와 커스텀 보간자는 기본 추출자를 제공하지만, `f`와 `raw`는 기본으로 추출자가 없어 패턴 매칭에 쓰려면 직접 구현해야 합니다.

```scala
val s"Hello, $name!" = "Hello, world!"
println(name)   // "world"
```

---

## 11. 정리

| 항목 | 핵심 내용 |
|---|---|
| 문자열 = 컬렉션 | `WrappedString`/`StringOps` 암시적 변환으로 `reverse`, `map`, `slice` 등 사용 가능 |
| `s"..."` | `$변수` / `${표현식}` 치환, 가장 기본 |
| `f"..."` | `%d`, `%2.2f` 등 형식 지정, 컴파일 타임 타입 검사 |
| `raw"..."` | `s`와 같지만 이스케이프 시퀀스 미해석 |
| 동작 원리 | 컴파일러가 `StringContext` 메서드 호출로 디슈가링 |
| 확장성 | `extension`(Scala 3) / `implicit class`(Scala 2)로 커스텀 보간자 정의 가능 |
| 패턴 매칭 | `s`와 커스텀 보간자는 추출자로도 사용 가능, `f`/`raw`는 기본 미지원 |

---

## 12. 참고 자료

- [Strings as Collections](https://docs.scala-lang.org/overviews/collections-2.13/strings.html)
- [String Interpolation](https://docs.scala-lang.org/overviews/core/string-interpolation.html)
- [Scala 3 Book — String Interpolation](https://docs.scala-lang.org/scala3/book/string-interpolation.html)
