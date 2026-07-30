# Scala 패턴 매칭, 함수형 에러 처리, Future와 Promise

## 패턴 매칭과 추출자(Extractor) 객체

> **원문:** https://docs.scala-lang.org/tour/pattern-matching.html , https://docs.scala-lang.org/tour/extractor-objects.html

---

### 목차

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

### 1. match 표현식 기초

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

### 2. 케이스 클래스 분해하기

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

### 3. 와일드카드 · 타입 패턴 · 변수 바인딩(`@`)

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

### 4. 패턴 가드(guard)

`case` 뒤에 `if 조건`을 붙이면, 패턴이 구조적으로 맞더라도 **조건까지 참일 때만** 그 분기를 선택합니다.

```scala
def classify(n: Int): String = n match
  case x if x < 0  => "음수"
  case x if x == 0 => "영"
  case _           => "양수"
```

가드는 패턴만으로는 표현할 수 없는 **값 자체에 대한 조건**(범위, 비교, 외부 변수 참조 등)을 추가로 걸 때 사용합니다.

---

### 5. sealed와 총체성(exhaustiveness) 검사

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

### 6. 추출자 객체와 unapply

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

### 7. 여러 개/가변 개수를 뽑아내는 unapplySeq

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

### 8. val 정의에서 추출자 쓰기

패턴 매칭 문법은 `match` 안뿐 아니라 **값 정의**에서도 쓸 수 있습니다.

```scala
val CustomerID(name) = CustomerID("Kim"): @unchecked
```

이 코드는 내부적으로 `unapply` 결과에 `.get`을 호출하는 것과 같습니다. 즉 매치에 실패하면 `None.get` 예외가 발생할 수 있으므로, 매치가 항상 성공한다고 확신할 수 있을 때만 이렇게 씁니다.

> ⚠️ **짚고 넘어가기 — 실패 가능성을 감수하는 문법**
>
> `match`는 실패한 경우(`case _`)를 강제로 다루게 하지만, `val 패턴 = 값` 형태는 실패 시 그대로 런타임 예외로 이어집니다. 튜플 분해(`val (a, b) = pair`)처럼 "절대 실패하지 않는" 패턴에 주로 사용하는 이유입니다.

---

### 9. 정리

- `match`는 값을 반환하는 표현식이며, Java `switch`보다 강력하게 **구조 분해**를 지원합니다.
- `case class`의 패턴 매칭은 컴파일러가 자동 생성한 `unapply` 덕분에 동작하는 것이며, **추출자 패턴의 특수한 경우**입니다.
- 임의의 `object`에 `unapply`(단일/복수 값) 또는 `unapplySeq`(가변 개수)를 정의하면, `case class`가 아닌 값도 `case 패턴(...) =>` 형태로 분해할 수 있습니다.
- `unapply`의 반환 타입 규칙: 참/거짓만 필요하면 `Boolean`, 값 하나면 `Option[T]`, 여러 개(고정 개수)면 `Option[튜플]`, 가변 개수면 `unapplySeq`로 `Option[Seq[T]]`.
- `sealed` 타입과 함께 쓰면 컴파일러가 **총체성(exhaustiveness) 검사**를 해주어, 빠뜨린 경우를 경고로 잡아낼 수 있습니다.

---

### 참고 자료

- [Pattern Matching — Tour of Scala](https://docs.scala-lang.org/tour/pattern-matching.html)
- [Extractor Objects — Tour of Scala](https://docs.scala-lang.org/tour/extractor-objects.html)

---

## Option, Either, Try를 이용한 함수형 에러 처리

---

### 목차

1. [null을 쓰지 않는 이유](#1-null을-쓰지-않는-이유)
2. [Option — 값이 있을 수도, 없을 수도](#2-option--값이-있을-수도-없을-수도)
3. [Option 다루기: match, for-컴프리헨션, 메서드](#3-option-다루기-match-for-컴프리헨션-메서드)
4. [Either — 실패 이유까지 담기](#4-either--실패-이유까지-담기)
5. [Try — 예외를 값으로 바꾸기](#5-try--예외를-값으로-바꾸기)
6. [세 타입 비교와 선택 기준](#6-세-타입-비교와-선택-기준)
7. [Option과 컬렉션의 관계](#7-option과-컬렉션의-관계)
8. [참고 자료](#8-참고-자료)

---

### 1. null을 쓰지 않는 이유

> **원문:** https://docs.scala-lang.org/overviews/scala-book/no-null-values.html

Scala가 기반을 두는 함수형 프로그래밍(대수학, algebra)에는 애초에 "값이 없음"을 뜻하는 null이라는 개념이 없습니다. Java 스타일로 실패를 표현하려고 하면 다음과 같은 모호함이 생깁니다.

```java
// Java 스타일: 실패 시 무엇을 반환해야 할지 애매하다
int toInt(String s) {
    try { return Integer.parseInt(s.trim()); }
    catch (Exception e) { return 0; }  // 실패인지, 진짜 0인지 구분 불가
}
```

`0`을 반환하면 "파싱에 실패했다"와 "입력이 정말 0이었다"를 구분할 수 없습니다. Scala는 이 모호함을 없애기 위해 **"성공/실패를 타입으로 표현"** 하는 쪽을 택했고, 그 첫 번째 도구가 `Option`입니다.

> 💡 **왜 필요한가** — `null`은 "참조는 있는데 가리키는 대상이 없다"는 상태를 타입 시스템 밖에서 몰래 허용합니다. 컴파일러는 어떤 값이 null일 수 있는지 알려주지 않으므로, 실수로 null을 사용하면 `NullPointerException`이 런타임에야 터집니다. `Option`은 "값이 없을 수 있다"는 사실 자체를 타입(`Option[A]`)에 새겨 넣어, 컴파일 타임에 처리를 강제합니다.

---

### 2. Option — 값이 있을 수도, 없을 수도

`Option[A]`는 값이 하나 있거나(`Some(a)`) 아예 없는(`None`) 두 가지 경우만 존재하는 컨테이너입니다.

| 타입 | 의미 |
|---|---|
| `Option[A]` | `Some[A]`와 `None`의 부모 타입(sealed) |
| `Some(value)` | 값이 있는 경우 |
| `None` | 값이 없는 경우 |

```scala
def toInt(s: String): Option[Int] =
  try Some(s.trim.toInt)
  catch case _: NumberFormatException => None

toInt("42")   // Some(42)
toInt("foo")  // None
```

`Integer.parseInt` 같은 "실패할 수 있는 연산"을 감싸서, 반환 타입만 보고도 이 함수가 실패할 수 있다는 사실을 알 수 있게 만듭니다.

#### 필드나 매개변수의 선택성 표현

값이 없을 수 있는 필드도 null 대신 `Option`으로 선언합니다.

```scala
case class Address(street1: String, street2: Option[String], city: String)

Address("역삼로 1", None, "서울")             // 상세주소 없음
Address("역삼로 1", Some("3층"), "서울")       // 상세주소 있음
```

`street2: String`이라고만 써두면 나중에 이 필드가 null일 수 있는지 코드만 봐서는 알 수 없지만, `Option[String]`이라고 쓰면 타입 자체가 "없을 수도 있음"을 알려줍니다.

> ⚠️ **짚고 넘어가기** — `Option`은 "예외적인 실패"보다는 "정상적으로 있을 수도 없을 수도 있는 값"을 표현하는 데 씁니다. 왜 예외가 났는지 이유까지 남기고 싶다면 4장의 `Either`나 5장의 `Try`를 씁니다.

---

### 3. Option 다루기: match, for-컴프리헨션, 메서드

> **원문:** https://docs.scala-lang.org/overviews/scala-book/no-null-values.html , https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html

#### 3.1 패턴 매칭으로 분기

```scala
toInt("42") match
  case Some(i) => println(s"성공: $i")
  case None    => println("변환 실패")
```

#### 3.2 for-컴프리헨션으로 여러 개 연결

값이 있을 때만 다음 단계로 넘어가고 싶은 연산이 여러 개면, 매번 `match`를 쓰는 대신 `for`로 엮습니다.

```scala
val sum: Option[Int] =
  for
    a <- toInt("1")
    b <- toInt("2")
    c <- toInt("3")
  yield a + b + c

sum  // Some(6)
```

셋 중 하나라도 `None`이면 전체 결과가 `None`이 됩니다. 내부적으로는 `flatMap`의 연쇄로 변환됩니다.

```scala
toInt("1").flatMap(a => toInt("2").flatMap(b => toInt("3").map(c => a + b + c)))
```

> 📘 **처음 배우는 분께** — `for`가 편해 보이지만 사실은 `map`/`flatMap` 호출을 대신 써 주는 문법 설탕(syntactic sugar)입니다. "값이 있으면 다음으로, 없으면 그대로 멈춘다"는 동작을 `flatMap`이 담당합니다.

#### 3.3 자주 쓰는 메서드

| 메서드 | 동작 |
|---|---|
| `getOrElse(default)` | 값이 있으면 꺼내고, 없으면 기본값 반환 |
| `map(f)` | 값이 있으면 `f` 적용한 `Some`, 없으면 `None` |
| `flatMap(f)` | `f`가 `Option`을 반환할 때 중첩(`Option[Option[A]]`)을 한 겹으로 |
| `foreach(f)` | 값이 있을 때만 `f`를 부수효과로 실행 |
| `isEmpty` / `isDefined` | 없음/있음 여부 확인 |
| `orElse(other)` | 값이 없으면 다른 `Option`으로 대체 |

```scala
toInt("42").getOrElse(0)     // 42
toInt("foo").getOrElse(0)    // 0
toInt("42").foreach(println) // 42 출력
toInt("foo").foreach(println) // 아무 것도 출력 안 함
```

> ⚠️ **짚고 넘어가기** — `Option.get`은 `None`일 때 예외를 던지므로 되도록 쓰지 않습니다. `getOrElse`나 패턴 매칭으로 안전하게 값을 꺼내세요.

---

### 4. Either — 실패 이유까지 담기

`Option`은 "왜 실패했는지"는 알려주지 않습니다. 실패 원인까지 값으로 남기고 싶을 때는 `Either[L, R]`을 씁니다. 관례상 `Left`는 실패, `Right`는 성공을 담습니다("정답(Right)"이라는 말장난).

```scala
def toInt(s: String): Either[String, Int] =
  try Right(s.trim.toInt)
  catch case _: NumberFormatException => Left(s"'$s'는 숫자가 아닙니다")

toInt("42")  // Right(42)
toInt("foo") // Left("'foo'는 숫자가 아닙니다")
```

`Either`도 `map`/`flatMap`이 **오른쪽(Right) 기준으로 편향(right-biased)** 되어 있어, `Option`처럼 for-컴프리헨션으로 엮을 수 있습니다.

```scala
val sum: Either[String, Int] =
  for
    a <- toInt("1")
    b <- toInt("two")   // 여기서 실패
    c <- toInt("3")
  yield a + b + c

sum  // Left("'two'는 숫자가 아닙니다")
```

패턴 매칭으로 두 갈래를 처리합니다.

```scala
toInt("foo") match
  case Right(n)  => println(s"성공: $n")
  case Left(msg) => println(s"실패: $msg")
```

---

### 5. Try — 예외를 값으로 바꾸기

> **원문:** https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html

`Try[A]`는 "예외를 던질 수 있는 코드"를 감싸는 용도로 만들어졌습니다. `Success(value)` 또는 원본 예외를 그대로 담은 `Failure(exception)` 둘 중 하나가 됩니다.

```scala
import scala.util.{Try, Success, Failure}

def toInt(s: String): Try[Int] = Try(s.trim.toInt)

toInt("42")   // Success(42)
toInt("foo")  // Failure(java.lang.NumberFormatException: ...)
```

`Try(...)` 블록은 안에서 던져진 예외를 자동으로 잡아 `Failure`로 감싸 줍니다. 직접 `try`/`catch`를 쓸 필요가 없어, 기존에 예외를 던지는 Java API를 감쌀 때 특히 편합니다.

`Option`과 마찬가지로 `map`/`flatMap`이 있고 for-컴프리헨션으로 엮을 수 있습니다.

```scala
val sum: Try[Int] =
  for
    a <- toInt("1")
    b <- toInt("2")
    c <- toInt("3")
  yield a + b + c

sum  // Success(6)
```

패턴 매칭 또는 `getOrElse`로 결과를 꺼냅니다.

```scala
toInt("foo") match
  case Success(n) => println(s"성공: $n")
  case Failure(e) => println(s"실패: ${e.getMessage}")

toInt("foo").getOrElse(-1)  // -1
```

`Either`로 변환하고 싶으면 `toEither`를 씁니다(`Failure`는 `Left(exception)`, `Success`는 `Right(value)`가 됩니다).

---

### 6. 세 타입 비교와 선택 기준

| 타입 | 실패를 표현 | 실패 이유 | 주 용도 |
|---|---|---|---|
| `Option[A]` | `None` | 없음 | null 대신, "있을 수도 없을 수도" 값 |
| `Either[L, R]` | `Left(L)` | 임의 타입(보통 메시지/에러코드) | 실패 이유를 직접 설계해서 남기고 싶을 때 |
| `Try[A]` | `Failure(Throwable)` | 예외 객체 그대로 | 예외를 던지는 기존 코드(특히 Java API)를 감쌀 때 |

- 애초에 예외가 아니라 "값이 있을 수도 없을 수도"인 경우 → `Option`
- 실패 이유를 직접 정의한 타입(예: 에러 코드, 검증 메시지)으로 남기고 싶은 경우 → `Either`
- `Integer.parseInt`처럼 이미 예외를 던지는 코드를 함수형으로 감싸고 싶은 경우 → `Try`

세 타입 모두 `map`/`flatMap`을 지원하므로 for-컴프리헨션으로 동일한 패턴을 사용할 수 있고, 서로 변환도 가능합니다(`Option.toRight`, `Try.toEither`, `Either.toOption` 등).

---

### 7. Option과 컬렉션의 관계

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/conversion-between-option-and-the-collections.html

`Option`은 원소가 0개 또는 1개뿐인 최소 단위의 컬렉션처럼 동작합니다. 다만 Scala 2.13부터는 컬렉션 전체 인터페이스인 `Iterable`이 아니라, 그보다 훨씬 좁은 **`IterableOnce`** 만 구현합니다.

> 📘 **처음 배우는 분께** — `IterableOnce`는 "적어도 한 번은 순회할 수 있다"는 최소한의 능력만 보장하는 타입입니다. `Option`이 `fromSpecific`(여러 원소로부터 자기 자신을 재구성하는 기능)을 제대로 구현할 수 없기 때문에, 완전한 `Iterable`이 아니라 이 최소 인터페이스만 구현하도록 설계되었습니다.

덕분에 `flatMap`처럼 `IterableOnce`를 받는 컬렉션 연산에 `Option`을 바로 섞어 쓸 수 있습니다.

```scala
for
  a <- Set(1, 2)
  b <- Option(10)   // Option을 컬렉션 for-컴프리헨션에 그대로 사용
yield a + b
// Set(11, 12)
```

리스트 안의 `Option`들을 값만 모으고 싶을 때는 `flatten`이 유용합니다.

```scala
val xs: List[Option[Int]] = List(Some(1), None, Some(3))
xs.flatten   // List(1, 3) — None은 자동으로 걸러짐
```

암시적 변환으로 `Option[A]`를 `Iterable[A]`처럼 다룰 수도 있지만(예: `.drop(1)`), 이 경우 결과 타입이 `Option`이 아니라 `Iterable`로 바뀌어 버립니다. 그래서 `Option`은 `map`, `filter`처럼 자주 쓰는 메서드들을 자체적으로 다시 구현해 두어, 컬렉션으로 자동 변환되지 않고도 `Option` 타입을 그대로 유지할 수 있게 합니다.

---

### 8. 참고 자료

- [Functional Error Handling](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html)
- [No Null Values](https://docs.scala-lang.org/overviews/scala-book/no-null-values.html)
- [Conversion Between Option and the Collections](https://docs.scala-lang.org/overviews/collections-2.13/conversion-between-option-and-the-collections.html)

---

## Future와 Promise를 이용한 비동기 프로그래밍

> **원문:** https://docs.scala-lang.org/overviews/core/futures.html

---

### 목차

1. [개요](#1-개요)
2. [ExecutionContext — Future를 실행하는 스레드 풀](#2-executioncontext--future를-실행하는-스레드-풀)
3. [Future 만들기와 상태 변화](#3-future-만들기와-상태-변화)
4. [콜백: onComplete와 foreach](#4-콜백-oncomplete와-foreach)
5. [함수형 조합: map, flatMap, for, filter](#5-함수형-조합-map-flatmap-for-filter)
6. [실패 처리: recover, recoverWith, fallbackTo, failed](#6-실패-처리-recover-recoverwith-fallbackto-failed)
7. [andThen — 순서가 보장된 부수효과](#7-andthen--순서가-보장된-부수효과)
8. [여러 Future 묶기: zip, firstCompletedOf, sequence/traverse](#8-여러-future-묶기-zip-firstcompletedof-sequencetraverse)
9. [블로킹: Await와 blocking](#9-블로킹-await와-blocking)
10. [Promise — Future를 직접 완료시키는 도구](#10-promise--future를-직접-완료시키는-도구)
11. [예외 처리 규칙](#11-예외-처리-규칙)
12. [Duration 간단히](#12-duration-간단히)
13. [정리](#13-정리)

---

### 1. 개요

Scala의 `Future[T]`는 **아직 존재하지 않을 수도 있는 값을 담는 그릇**입니다. 오래 걸리는 계산이나 I/O를 별도 스레드에 맡기고, 호출한 쪽은 결과를 기다리며 멈추지 않고 다음 일을 계속할 수 있게 해줍니다.

핵심 규칙은 단순합니다.

- `Future[T]`는 **성공(값 T)** 아니면 **실패(예외)** 로 **딱 한 번만** 완료됩니다.
- 한 번 완료되면 그 상태는 **바뀌지 않습니다**(불변).
- Future 자체는 "결과를 읽기만 하는" 쪽이고, 그 결과를 채워 넣는 역할은 짝꿍인 **`Promise`** 가 맡습니다.

> 💡 **왜 필요한가 — 콜백 지옥 대신 값처럼 다루기**
>
> 전통적인 비동기 코드는 콜백 함수를 계속 중첩시켜야 해서("콜백 지옥") 읽기도 조합하기도 어렵습니다. `Future`는 "아직 안 왔지만 곧 올 값"을 `map`, `flatMap` 같은 익숙한 컬렉션 연산으로 조합할 수 있게 해서, 비동기 코드를 동기 코드와 비슷한 느낌으로 작성하게 해줍니다.

---

### 2. ExecutionContext — Future를 실행하는 스레드 풀

`Future { ... }` 블록은 어딘가의 스레드에서 실행되어야 하는데, "어디서 실행할지"를 결정하는 것이 **`ExecutionContext`** 입니다. Java의 `Executor`/스레드 풀과 비슷한 역할입니다.

```scala
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global   // 기본 제공 컨텍스트

val f: Future[Int] = Future {
  Thread.sleep(1000)
  21 * 2
}
```

- `ExecutionContext.global`은 **ForkJoinPool** 기반이며, 기본 병렬도는 **가용 CPU 코어 수**입니다.
- 필요하면 자바 `Executor`를 감싸서 직접 만들 수도 있습니다: `ExecutionContext.fromExecutor(executor)`.
- Scala 3에서는 `given ExecutionContext = ...`로 컨텍스트를 스코프에 두는 것이 관용적입니다(implicit → given 전환은 `02_contextual_givens_using.md` 참고).

VM 옵션으로 스레드 풀 크기를 조절할 수 있습니다.

| 옵션 | 의미 | 기본값 |
|---|---|---|
| `scala.concurrent.context.minThreads` | 최소 스레드 수 | 1 |
| `scala.concurrent.context.numThreads` | 목표 스레드 수 | 코어 수 |
| `scala.concurrent.context.maxThreads` | 최대 스레드 수 | 코어 수 |

---

### 3. Future 만들기와 상태 변화

`Future { 표현식 }`은 표현식을 (암시적으로 주어진) `ExecutionContext` 위에서 즉시 비동기로 실행하기 시작합니다.

```scala
val price: Future[Int] = Future {
  fetchStockPrice("AAPL")   // 오래 걸릴 수 있는 작업
}
```

상태는 다음 세 가지 중 하나입니다.

- **미완료(not yet completed)** — 아직 실행 중
- **성공적으로 완료(completed with a value)**
- **실패로 완료(completed with an exception)**

한 번 완료 상태에 들어가면 되돌아가지 않습니다. "성공했다가 나중에 실패로 바뀐다" 같은 일은 없습니다.

---

### 4. 콜백: onComplete와 foreach

Future가 완료되었을 때 실행할 콜백을 등록할 수 있습니다.

```scala
import scala.util.{Success, Failure}

price.onComplete {
  case Success(v)  => println(s"가격: $v")
  case Failure(ex) => println(s"실패: ${ex.getMessage}")
}

price.foreach(v => println(s"가격: $v"))   // 성공한 경우만 처리
```

- `onComplete`는 성공/실패 **양쪽 모두** 다룹니다.
- `foreach`는 **성공했을 때만** 콜백을 실행하고, 실패하면 조용히 무시합니다.

> ⚠️ **짚고 넘어가기 — 콜백 실행 순서는 보장되지 않는다**
>
> 같은 Future에 콜백을 여러 개 등록해도 **어떤 순서로 실행될지, 어느 스레드에서 실행될지는 정해져 있지 않습니다.** "언젠가는 실행된다"만 보장됩니다. 순서가 필요하면 뒤에 나오는 `andThen`을 쓰세요.

---

### 5. 함수형 조합: map, flatMap, for, filter

Future는 컬렉션처럼 `map`/`flatMap`으로 값을 가공할 수 있고, 이 연산들은 **원본을 바꾸지 않고 새 Future를 반환**합니다.

```scala
val doubled: Future[Int] = price.map(_ * 2)                  // 성공 값만 변환

val withTax: Future[Int] = price.flatMap { p =>
  Future(p + calcTax(p))                                      // Future를 반환하는 다음 단계로 연결
}
```

- `map` : 성공 값을 다른 값으로 바꿈. 실패는 그대로 전파.
- `flatMap` : "이 값을 받아서 또 다른 Future를 만드는" 다음 단계와 연결. 여러 비동기 작업을 순서대로 이어붙일 때 사용.
- `filter`/`withFilter` : 조건을 만족하지 않으면 `NoSuchElementException`으로 실패.

여러 Future를 조합할 때는 **for-컴프리헨션**이 `flatMap` 체인보다 훨씬 읽기 쉽습니다.

```scala
val usd: Future[Int] = Future(price1)
val eur: Future[Int] = Future(price2)

val total: Future[Int] = for {
  p1 <- usd
  p2 <- eur
  if p1 > 0
} yield p1 + p2
```

> ⚠️ **짚고 넘어가기 — for 안의 Future는 이미 각자 실행 중이다**
>
> `usd`, `eur`는 `for` 블록에 들어가기 **전에 이미 만들어졌으므로 병렬로 실행됩니다.** 반대로 `for` 블록 안에서 `Future(...)`를 새로 만들면 앞 줄의 결과가 나온 뒤에야 시작되어 **순차 실행**이 됩니다. "병렬로 돌리고 싶다면 Future를 미리 만들어 두라"는 것이 흔히 하는 실수 포인트입니다.

---

### 6. 실패 처리: recover, recoverWith, fallbackTo, failed

| 메서드 | 역할 |
|---|---|
| `recover { case ... => 값 }` | 특정 예외를 **성공 값**으로 바꿔치기 |
| `recoverWith { case ... => Future(...) }` | 특정 예외를 **다른 Future**로 대체 |
| `fallbackTo(다른Future)` | 실패하면 인자로 준 Future의 결과를 대신 사용 |
| `failed` | 성공/실패를 뒤집는 투영(projection). 원래 실패했을 때만 그 예외를 값으로 받음 |

```scala
val safePrice: Future[Int] =
  price.recover { case _: TimeoutException => -1 }

val withBackup: Future[Int] = price.fallbackTo(cachedPrice)

price.failed.foreach(ex => log(ex))   // price가 실패했을 때만 실행
```

- `recover`는 **동기 값**을 반환(예외 → 값), `recoverWith`는 **또 다른 Future**를 반환(예외 → Future)한다는 점이 `map`/`flatMap`의 차이와 대응됩니다.

---

### 7. andThen — 순서가 보장된 부수효과

`andThen`은 콜백을 실행하지만, **결과는 그대로 다음 `andThen`에 전달**되도록 체이닝할 수 있고 실행 순서도 등록한 순서를 따릅니다. 로깅처럼 "본 결과는 안 바꾸되 순서대로 뭔가 하고 싶을 때" 씁니다.

```scala
price
  .andThen { case Success(v) => log(s"조회 성공: $v") }
  .andThen { case _          => cleanupResources() }
```

일반 `onComplete`를 여러 번 걸면 순서가 뒤섞일 수 있지만, `andThen` 체인은 **등록한 순서대로 실행**된다는 점이 다릅니다.

---

### 8. 여러 Future 묶기: zip, firstCompletedOf, sequence/traverse

| 상황 | 도구 |
|---|---|
| 두 Future를 **모두** 기다려 튜플로 묶기 | `f1.zip(f2)` |
| 여러 Future 중 **가장 먼저** 끝난 것만 취하기 | `Future.firstCompletedOf(Seq(f1, f2, f3))` |
| `List[Future[A]]`를 `Future[List[A]]`로 뒤집기 | `Future.sequence(futures)` |
| 리스트를 순회하며 각각 Future로 변환한 뒤 한 번에 묶기 | `Future.traverse(list)(f)` |

```scala
val f1 = Future(fetchA())
val f2 = Future(fetchB())

val both: Future[(A, B)] = f1.zip(f2)                        // 둘 다 성공해야 성공

val fastest: Future[Int] = Future.firstCompletedOf(Seq(f1, f2))

val many: Future[List[Int]] = Future.sequence(List(f1, f2, f3))
```

`sequence`/`traverse`는 병렬로 실행 중인 여러 Future의 결과를 한꺼번에 모을 때 특히 유용합니다.

---

### 9. 블로킹: Await와 blocking

Future는 원래 "기다리지 않는" 것이 목적이지만, 프로그램의 진입점(`main`)이나 테스트 코드처럼 **어딘가에서는 결과를 꺼내야** 합니다. 이때 `Await`을 씁니다.

```scala
import scala.concurrent.Await
import scala.concurrent.duration._

val value = Await.result(price, 3.seconds)   // 완료까지 블로킹, 실패하면 예외를 던짐
Await.ready(price, 3.seconds)                // 완료만 기다림(결과는 안 꺼냄), 실패해도 예외 안 던짐
```

Future 내부에서 어쩔 수 없이 블로킹 작업(예: 락 대기, 동기 I/O)을 해야 한다면, `blocking { ... }`으로 감싸 스레드 풀에 "지금 이 스레드가 막혀 있다"고 알려줍니다. 실제로 어떻게 반응할지는 `ExecutionContext` 구현에 달려 있는데, `ExecutionContext.global`은 이 신호를 받아 필요하면 스레드를 더 만들어 다른 작업이 굶지 않게 하지만, 스레드 풀을 고정 크기로 쓰는 다른 `ExecutionContext`는 아무 동작도 하지 않을 수 있습니다.

```scala
Future {
  blocking {
    legacyBlockingCall()
  }
}
```

> ⚠️ **짚고 넘어가기 — 블로킹은 최후의 수단**
>
> `Await`이나 `blocking`을 남발하면 애초에 Future를 쓰는 이유(스레드를 놀리지 않기)가 사라집니다. 스레드 풀이 고갈되어 데드락처럼 보이는 현상도 생길 수 있으므로, 가능하면 `map`/`flatMap`/콜백으로 끝까지 비동기로 처리하고 `Await`은 프로그램의 최상단이나 테스트에서만 쓰는 것이 안전합니다.

---

### 10. Promise — Future를 직접 완료시키는 도구

`Future { ... }`는 "무엇을 실행할지"와 "결과가 어떻게 채워질지"가 한 덩어리이지만, **콜백 기반 API처럼 나중에 외부에서 결과를 채워 넣어야 하는 상황**도 있습니다. 이럴 때 `Promise`를 씁니다.

```scala
import scala.concurrent.Promise

val p = Promise[Int]()
val f: Future[Int] = p.future     // 아직 미완료 상태인 Future를 미리 꺼내 둠

// ... 나중에, 어디선가 ...
p.success(42)          // f가 성공(42)으로 완료됨
// 혹은
p.failure(new RuntimeException("실패"))
```

| 메서드 | 동작 |
|---|---|
| `success(v)` / `failure(e)` | 성공/실패로 완료. **이미 완료된 Promise에 다시 호출하면 예외** |
| `complete(Try[T])` | `Success`/`Failure` 값으로 한 번에 완료 |
| `trySuccess` / `tryFailure` / `tryComplete` | 이미 완료돼 있어도 예외 대신 `Boolean`으로 성공 여부만 알려줌 |
| `completeWith(다른Future)` | 다른 Future가 끝나는 대로 그 결과를 그대로 이어받아 완료 |

`Promise`는 "Future를 만드는 공장의 스위치"라고 보면 됩니다 — 콜백 기반 라이브러리(예: 네트워크 클라이언트의 `onResult` 콜백)를 Future 기반 API로 감쌀 때 전형적으로 사용합니다.

```scala
def legacyAsyncCall(onResult: Try[Int] => Unit): Unit = ???

def toFuture: Future[Int] =
  val p = Promise[Int]()
  legacyAsyncCall(p.complete)
  p.future
```

---

### 11. 예외 처리 규칙

`Future { ... }` 블록 안에서 예외가 발생하면 기본적으로 그 예외가 Future의 실패 값으로 담깁니다. 다만 몇 가지는 특별 취급됩니다.

- `scala.runtime.NonLocalReturnControl` (함수 안에서 `return`을 쓸 때 내부적으로 쓰이는 제어 예외) → 그 안에 담긴 반환값을 그대로 꺼내 **성공** 값으로 처리
- `InterruptedException`, `Error`, `ControlThrowable` → `ExecutionException`으로 한 번 감싸서 전달
- `NonFatal`로 분류되지 않는 **치명적(fatal) 예외**(예: `OutOfMemoryError`)는 Future 안에 담기지 않고 실행 중이던 스레드에서 그대로 다시 던져집니다.

즉 "어떤 예외든 조용히 Future 실패로 삼켜진다"고 가정하면 안 되고, 정말 치명적인 오류는 여전히 프로세스를 흔들 수 있다는 뜻입니다.

---

### 12. Duration 간단히

`Await`이나 타임아웃 API에서 쓰는 시간 단위는 `scala.concurrent.duration.Duration`입니다.

```scala
import scala.concurrent.duration._

val d1 = Duration(100, MILLISECONDS)
val d2 = 100.millis          // 리터럴 확장 메서드로 더 간결하게
val d3 = 1.2.seconds

val Duration(length, unit) = d2   // 패턴 매칭으로 분해도 가능
```

덧셈·비교 같은 산술 연산과 단위 변환을 지원해서, 타임아웃 값을 조합하거나 로깅할 때 편리합니다.

---

### 13. 정리

| 개념 | 한 줄 요약 |
|---|---|
| `Future[T]` | 언젠가 성공 또는 실패로 딱 한 번 완료되는 비동기 값 |
| `ExecutionContext` | Future가 실제로 실행되는 스레드 풀 |
| `onComplete` / `foreach` | 완료 후 실행할 콜백 등록 (순서 보장 없음) |
| `map` / `flatMap` / `for` | 값을 변환하거나 여러 Future를 순서대로 엮음 |
| `recover` / `recoverWith` / `fallbackTo` | 실패를 값 또는 대체 Future로 흡수 |
| `andThen` | 순서가 보장된 부수효과 체이닝 |
| `zip` / `firstCompletedOf` / `sequence` | 여러 Future를 동시에 묶어 처리 |
| `Await.result` / `Await.ready` | 결과를 동기적으로 기다림(최후의 수단) |
| `blocking { ... }` | 불가피한 블로킹 구간을 스레드 풀에 알림 |
| `Promise` | 외부에서 직접 Future를 완료시키는 손잡이 |

Future/Promise는 "값을 반환하는 계산"이라는 Scala의 표현식 중심 사고(00번 문서)를 **비동기 세계로 그대로 확장한 것**입니다. 동기 코드에서 `val`에 값을 담듯, 비동기 코드에서는 `Future`에 "곧 담길 값"을 담고 `map`/`flatMap`으로 조립한다고 생각하면 자연스럽습니다.

---

### 참고 자료

- [Scala Futures and Promises (공식 문서)](https://docs.scala-lang.org/overviews/core/futures.html)
- [scala.concurrent.Future API](https://www.scala-lang.org/api/current/scala/concurrent/Future.html)
- [scala.concurrent.Promise API](https://www.scala-lang.org/api/current/scala/concurrent/Promise.html)
