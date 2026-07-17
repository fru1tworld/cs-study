# 정규 표현식

> **원문:** https://docs.scala-lang.org/tour/regular-expression-patterns.html

---

## 목차

1. [`Regex` 만들기](#1-regex-만들기)
2. [첫 번째 일치 찾기](#2-첫-번째-일치-찾기)
3. [그룹으로 부분 추출하기](#3-그룹으로-부분-추출하기)
4. [모든 일치 순회하기](#4-모든-일치-순회하기)
5. [`match`와 결합하기](#5-match와-결합하기)
6. [요약](#6-요약)

---

## 1. `Regex` 만들기

Scala는 별도의 정규식 리터럴 문법 없이, **문자열에 `.r` 메서드를 붙이는 것만으로** 정규 표현식 객체(`scala.util.matching.Regex`)를 만듭니다.

```scala
import scala.util.matching.Regex

val digit: Regex = "[0-9]".r
```

> 💡 **왜 필요한가**
>
> 별도 리터럴(`/regex/` 같은) 없이 문자열 그대로 정규식이 되므로, 문자열 보간(`s"..."`)이나 삼중 따옴표(`"""..."""`)와 자연스럽게 섞어 쓸 수 있습니다. 백슬래시가 많이 들어가는 패턴은 이스케이프를 피하기 위해 삼중 따옴표를 쓰는 것이 관례입니다.

```scala
val phone: Regex = """\d{3}-\d{3}-\d{4}""".r   // 삼중 따옴표: \ 이스케이프 불필요
```

---

## 2. 첫 번째 일치 찾기

`findFirstMatchIn`은 문자열 안에서 패턴과 처음 일치하는 부분을 찾아 `Option[Match]`로 돌려줍니다. 일치가 없으면 `None`이므로, 결과가 있을 수도 없을 수도 있는 상황을 `Option`으로 안전하게 다룰 수 있습니다.

```scala
val digit: Regex = "[0-9]".r

def hasDigit(password: String): Boolean =
  digit.findFirstMatchIn(password) match
    case Some(_) => true
    case None    => false
```

> 📘 **처음 배우는 분께**
>
> 이 패턴은 "일치 여부만 필요하고 실제 매치 값은 필요 없는" 전형적인 상황입니다. 이런 경우 굳이 `match`를 쓰지 않고 `digit.findFirstMatchIn(password).isDefined`처럼 `Option`의 메서드로 줄여 쓸 수도 있습니다.

---

## 3. 그룹으로 부분 추출하기

패턴 일부를 괄호 `( )`로 묶으면 **그룹(group)** 이 되어, 매치 결과에서 해당 부분만 따로 꺼낼 수 있습니다.

```scala
val keyVal: Regex = "([a-zA-Z-]+):\\s*(.+)".r

val line = "color: red"
keyVal.findFirstMatchIn(line).foreach { m =>
  println(s"key=${m.group(1)}, value=${m.group(2)}")   // key=color, value=red
}
```

- `group(0)` : 전체 일치 문자열
- `group(1)`, `group(2)`, ... : 각 괄호에 대응하는 부분 문자열 (왼쪽부터 순서대로)

> ⚠️ **짚고 넘어가기**
>
> 그룹 번호는 여는 괄호 `(`가 나온 순서로 매겨집니다. 중첩된 괄호가 있으면 바깥쪽부터 번호가 붙으므로, 패턴이 복잡해지면 그룹 순서를 착각하기 쉽습니다.

---

## 4. 모든 일치 순회하기

문자열 하나에 여러 번 일치하는 경우, `findAllMatchIn`으로 모든 매치를 순회할 수 있습니다. 이 메서드는 `Iterator[Match]`를 돌려주므로 `for`나 컬렉션 메서드로 처리하기 좋습니다.

```scala
val css: Regex = "([a-zA-Z-]+):\\s*([^;]+);".r

val style = "color: red; margin: 0; padding: 4px;"

for (m <- css.findAllMatchIn(style))
  println(s"${m.group(1)} -> ${m.group(2)}")

// color -> red
// margin -> 0
// padding -> 4px
```

CSS 선언처럼 "키: 값;"이 여러 번 반복되는 텍스트를 한 번에 파싱할 때 유용한 패턴입니다.

---

## 5. `match`와 결합하기

정규식을 `case` 절의 패턴으로 바로 사용할 수 있습니다. 이때 그룹은 자동으로 변수에 바인딩되며, 필요 없는 그룹은 `_`로 무시합니다.

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

> 💡 **왜 필요한가**
>
> `Regex`도 `unapply`를 구현한 추출자(extractor)이기 때문에, `case class`를 패턴 매칭하듯 `case Email(user, domain, _)`처럼 쓸 수 있습니다. 문자열의 형태에 따라 분기하면서 동시에 필요한 부분만 이름 붙여 꺼내는, Scala 패턴 매칭의 전형적인 활용입니다.

---

## 6. 요약

| 하고 싶은 일 | 메서드/문법 |
|---|---|
| 문자열을 정규식으로 만들기 | `"패턴".r` |
| 첫 일치 찾기 (있을 수도, 없을 수도) | `regex.findFirstMatchIn(s): Option[Match]` |
| 모든 일치 순회하기 | `regex.findAllMatchIn(s): Iterator[Match]` |
| 그룹 값 꺼내기 | `m.group(n)` |
| 형태별로 분기하며 값 추출 | `s match { case Regex(a, b) => ... }` |

정규식은 결국 "문자열을 검사하고, 부분을 뽑아내는" 두 가지 목적으로 쓰이며, Scala에서는 이 둘 모두를 `Option`/`Iterator`/패턴 매칭 같은 익숙한 도구로 자연스럽게 표현합니다.

---

## 참고 자료

- [Tour of Scala — Regular Expression Patterns](https://docs.scala-lang.org/tour/regular-expression-patterns.html)
- [`scala.util.matching.Regex` API 문서](https://www.scala-lang.org/api/current/scala/util/matching/Regex.html)
