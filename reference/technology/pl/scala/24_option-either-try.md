# Option, Either, Try를 이용한 함수형 에러 처리

---

## 목차

1. [null을 쓰지 않는 이유](#1-null을-쓰지-않는-이유)
2. [Option — 값이 있을 수도, 없을 수도](#2-option--값이-있을-수도-없을-수도)
3. [Option 다루기: match, for-컴프리헨션, 메서드](#3-option-다루기-match-for-컴프리헨션-메서드)
4. [Either — 실패 이유까지 담기](#4-either--실패-이유까지-담기)
5. [Try — 예외를 값으로 바꾸기](#5-try--예외를-값으로-바꾸기)
6. [세 타입 비교와 선택 기준](#6-세-타입-비교와-선택-기준)
7. [Option과 컬렉션의 관계](#7-option과-컬렉션의-관계)
8. [참고 자료](#8-참고-자료)

---

## 1. null을 쓰지 않는 이유

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

## 2. Option — 값이 있을 수도, 없을 수도

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

### 필드나 매개변수의 선택성 표현

값이 없을 수 있는 필드도 null 대신 `Option`으로 선언합니다.

```scala
case class Address(street1: String, street2: Option[String], city: String)

Address("역삼로 1", None, "서울")             // 상세주소 없음
Address("역삼로 1", Some("3층"), "서울")       // 상세주소 있음
```

`street2: String`이라고만 써두면 나중에 이 필드가 null일 수 있는지 코드만 봐서는 알 수 없지만, `Option[String]`이라고 쓰면 타입 자체가 "없을 수도 있음"을 알려줍니다.

> ⚠️ **짚고 넘어가기** — `Option`은 "예외적인 실패"보다는 "정상적으로 있을 수도 없을 수도 있는 값"을 표현하는 데 씁니다. 왜 예외가 났는지 이유까지 남기고 싶다면 4장의 `Either`나 5장의 `Try`를 씁니다.

---

## 3. Option 다루기: match, for-컴프리헨션, 메서드

> **원문:** https://docs.scala-lang.org/overviews/scala-book/no-null-values.html , https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html

### 3.1 패턴 매칭으로 분기

```scala
toInt("42") match
  case Some(i) => println(s"성공: $i")
  case None    => println("변환 실패")
```

### 3.2 for-컴프리헨션으로 여러 개 연결

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

### 3.3 자주 쓰는 메서드

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

## 4. Either — 실패 이유까지 담기

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

## 5. Try — 예외를 값으로 바꾸기

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

## 6. 세 타입 비교와 선택 기준

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

## 7. Option과 컬렉션의 관계

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

## 8. 참고 자료

- [Functional Error Handling](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html)
- [No Null Values](https://docs.scala-lang.org/overviews/scala-book/no-null-values.html)
- [Conversion Between Option and the Collections](https://docs.scala-lang.org/overviews/collections-2.13/conversion-between-option-and-the-collections.html)
