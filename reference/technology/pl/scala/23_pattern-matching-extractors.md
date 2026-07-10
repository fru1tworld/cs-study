# 패턴 매칭과 추출자(Extractor) 객체

> **원문:** https://docs.scala-lang.org/tour/pattern-matching.html , https://docs.scala-lang.org/tour/extractor-objects.html

---

## 목차

1. [match 표현식 기초](#1-match-표현식-기초)
2. [케이스 클래스 분해하기](#2-케이스-클래스-분해하기)
3. [와일드카드 · 타입 패턴 · 변수 바인딩(@)](#3-와일드카드--타입-패턴--변수-바인딩)
4. [패턴 가드(guard)](#4-패턴-가드guard)
5. [sealed와 총체성(exhaustiveness) 검사](#5-sealed와-총체성exhaustiveness-검사)
6. [추출자 객체와 unapply](#6-추출자-객체와-unapply)
7. [여러 개/가변 개수를 뽑아내는 unapplySeq](#7-여러-개가변-개수를-뽑아내는-unapplyseq)
8. [val 정의에서 추출자 쓰기](#8-val-정의에서-추출자-쓰기)
9. [정리](#9-정리)

---

## 1. match 표현식 기초

Scala의 `match`는 Java의 `switch`보다 훨씬 강력합니다. 단순히 값을 비교하는 데 그치지 않고, **값의 구조를 검사하면서 동시에 분해**할 수 있기 때문입니다.

```scala
val n = 3
val result = n match
  case 0 => "영"
  case 1 => "하나"
  case _ => "그 외"   // 와일드카드: 남은 모든 경우

println(result)   // "그 외"
```

> 💡 **왜 필요한가 — match는 문(statement)이 아니라 식(expression)**
>
> `if`가 값을 반환하듯 `match`도 값을 반환합니다. 그래서 `val result = n match { ... }`처럼 결과를 바로 변수에 담거나 함수의 반환값으로 쓸 수 있습니다. Java의 `switch`는 (전통적으로는) 분기만 하고 값을 주지 않는 문이라는 점과 대비됩니다.

---

## 2. 케이스 클래스 분해하기

`match`가 진가를 발휘하는 곳은 `case class`를 분해할 때입니다. 필드에 이름을 붙여 한 번에 값을 꺼낼 수 있습니다.

```scala
sealed trait Notification
case class Email(sender: String, title: String) extends Notification
case class SMS(caller: String, message: String) extends Notification

def describe(n: Notification): String = n match
  case Email(sender, title) => s"$sender 님의 메일: $title"
  case SMS(caller, _)       => s"$caller 님의 문자"   // message는 관심 없으면 _
```

각 `case` 패턴은 해당 케이스 클래스의 **생성자 형태를 그대로 흉내 낸 모양**이며, 실제로는 뒤에서 설명할 `unapply`를 통해 필드를 분해합니다.

---

## 3. 와일드카드 · 타입 패턴 · 변수 바인딩(`@`)

| 패턴 | 의미 | 예시 |
|---|---|---|
| `_` | 아무 값이나 매치, 값은 버림 | `case SMS(_, msg) => msg` |
| `x` (소문자 식별자) | 아무 값이나 매치하되 `x`라는 이름으로 바인딩 | `case x => x.toString` |
| `x: T` | 타입 `T`의 인스턴스일 때만 매치, `x`로 바인딩 | `case p: SMS => p.caller` |
| `x @ pattern` | 패턴에 매치시키면서 **전체 값도** `x`로 함께 바인딩 | `case p @ SMS(c, _) => (p, c)` |

```scala
def handle(n: Notification): String = n match
  case p: Email          => s"메일 객체 전체: $p"
  case p @ SMS(c, _)     => s"문자 전체 $p, 발신자만 $c"
```

- 타입 패턴(`x: T`)은 "이 값이 특정 하위 타입일 때만" 분기하고 싶을 때 씁니다.
- `@` 바인딩은 "필드도 꺼내고 싶고, 원본 객체 전체도 유지하고 싶을 때" 동시에 쓰는 절충안입니다.

---

## 4. 패턴 가드(guard)

`case` 뒤에 `if 조건`을 붙이면, 패턴이 구조적으로 맞더라도 **조건까지 참일 때만** 그 분기를 선택합니다.

```scala
def classify(n: Int): String = n match
  case x if x < 0  => "음수"
  case x if x == 0 => "영"
  case _           => "양수"
```

가드는 패턴만으로는 표현할 수 없는 **값 자체에 대한 조건**(범위, 비교, 외부 변수 참조 등)을 추가로 걸 때 사용합니다.

---

## 5. sealed와 총체성(exhaustiveness) 검사

`sealed trait`로 선언된 타입은 하위 타입이 **같은 파일 안에서만** 정의될 수 있습니다. 이 덕분에 컴파일러는 "가능한 모든 하위 타입"을 미리 알 수 있고, `match`에서 빠뜨린 경우가 있으면 경고를 냅니다.

```scala
sealed trait Notification
case class Email(sender: String, title: String) extends Notification
case class SMS(caller: String, message: String) extends Notification

def describe(n: Notification): String = n match
  case Email(sender, _) => s"$sender 님의 메일"
  // SMS 케이스가 빠짐 → 컴파일러가 "match may not be exhaustive" 경고
```

> ⚠️ **짚고 넘어가기 — 경고이지 항상 에러는 아니다**
>
> 기본 설정에서는 경고(warning)로 그치지만, 빌드 설정에 따라 이를 에러로 승격시켜 컴파일을 막을 수 있습니다. `sealed` + `case class`(ADT) 조합을 쓰는 가장 큰 이유가 바로 이 안전성입니다. `00_prerequisites_scala_basics.md`의 ADT 설명과 이어지는 내용입니다.

---

## 6. 추출자 객체와 unapply

지금까지 본 `case class` 패턴은 사실 **추출자(extractor)** 라는 더 일반적인 메커니즘의 한 사례일 뿐입니다. 추출자는 `unapply`라는 메서드를 가진 평범한 `object`입니다.

> 📘 **처음 배우는 분께 — apply의 반대 방향**
>
> `object Foo { def apply(...) = ... }`는 `Foo(...)`로 값을 **만드는** 규칙을 정의합니다.
> `object Foo { def unapply(...) = ... }`는 반대로 `case Foo(...) =>` 패턴이 값을 **분해하는** 규칙을 정의합니다. 이름 그대로 "apply의 반대(un-apply)"입니다.

`case class`는 컴파일러가 `apply`와 `unapply`를 **자동으로 만들어 주기 때문에** 편리했던 것이고, 직접 `unapply`를 정의하면 `case class`가 아닌 임의의 값에도 패턴 매칭을 걸 수 있습니다.

```scala
object CustomerID:
  def apply(name: String): String = s"$name--${name.hashCode}"

  def unapply(customerID: String): Option[String] =
    val name = customerID.split("--").head
    if customerID == apply(name) then Some(name) else None

val id = CustomerID("Kim")   // "Kim--..."

id match
  case CustomerID(name) => println(s"이름: $name")   // unapply가 호출되어 name을 뽑아냄
  case _                => println("형식이 안 맞음")
```

`unapply`의 반환 타입은 "몇 개의 값을 뽑아내려 하는가"에 따라 다릅니다.

| 뽑아낼 값 | `unapply` 반환 타입 | 매치 실패 표현 |
|---|---|---|
| 없음, 참/거짓만 확인 | `Boolean` | `false` |
| 값 1개 | `Option[T]` | `None` |
| 값 여러 개(고정 개수) | `Option[(T1, T2, ...)]` (튜플) | `None` |

```scala
object Even:
  def unapply(n: Int): Boolean = n % 2 == 0

4 match
  case Even() => "짝수"   // Boolean을 반환하는 unapply는 () 형태로 매치
  case _      => "홀수"
```

---

## 7. 여러 개/가변 개수를 뽑아내는 unapplySeq

미리 개수를 정할 수 없는 경우(예: 리스트, 정규식 그룹)에는 `unapply` 대신 `unapplySeq`를 정의합니다. 이때 반환 타입은 `Option[Seq[T]]`입니다.

```scala
object Words:
  def unapplySeq(s: String): Option[Seq[String]] =
    Some(s.split(" ").toSeq)

"scala is fun" match
  case Words(a, b, c) => println(s"$a / $b / $c")   // 개수가 맞을 때만 매치
```

`List(x, y, z)`처럼 컬렉션을 원소 개수와 함께 분해하는 패턴이 바로 이 방식으로 동작합니다.

---

## 8. val 정의에서 추출자 쓰기

패턴 매칭 문법은 `match` 안뿐 아니라 **값 정의**에서도 쓸 수 있습니다.

```scala
val CustomerID(name) = CustomerID("Kim"): @unchecked
```

이 코드는 내부적으로 `unapply` 결과에 `.get`을 호출하는 것과 같습니다. 즉 매치에 실패하면 `None.get` 예외가 발생할 수 있으므로, 매치가 항상 성공한다고 확신할 수 있을 때만 이렇게 씁니다.

> ⚠️ **짚고 넘어가기 — 실패 가능성을 감수하는 문법**
>
> `match`는 실패한 경우(`case _`)를 강제로 다루게 하지만, `val 패턴 = 값` 형태는 실패 시 그대로 런타임 예외로 이어집니다. 튜플 분해(`val (a, b) = pair`)처럼 "절대 실패하지 않는" 패턴에 주로 사용하는 이유입니다.

---

## 9. 정리

- `match`는 값을 반환하는 표현식이며, Java `switch`보다 강력하게 **구조 분해**를 지원합니다.
- `case class`의 패턴 매칭은 컴파일러가 자동 생성한 `unapply` 덕분에 동작하는 것이며, **추출자 패턴의 특수한 경우**입니다.
- 임의의 `object`에 `unapply`(단일/복수 값) 또는 `unapplySeq`(가변 개수)를 정의하면, `case class`가 아닌 값도 `case 패턴(...) =>` 형태로 분해할 수 있습니다.
- `unapply`의 반환 타입 규칙: 참/거짓만 필요하면 `Boolean`, 값 하나면 `Option[T]`, 여러 개(고정 개수)면 `Option[튜플]`, 가변 개수면 `unapplySeq`로 `Option[Seq[T]]`.
- `sealed` 타입과 함께 쓰면 컴파일러가 **총체성(exhaustiveness) 검사**를 해주어, 빠뜨린 경우를 경고로 잡아낼 수 있습니다.

---

## 참고 자료

- [Pattern Matching — Tour of Scala](https://docs.scala-lang.org/tour/pattern-matching.html)
- [Extractor Objects — Tour of Scala](https://docs.scala-lang.org/tour/extractor-objects.html)
