# 타입 계층: Any, AnyVal, AnyRef와 값 클래스

> **원문:** https://docs.scala-lang.org/tour/unified-types.html , https://docs.scala-lang.org/overviews/core/value-classes.html

---

## 목차

1. [개요: Scala는 왜 "모든 것이 객체"라고 말하는가](#1-개요-scala는-왜-모든-것이-객체라고-말하는가)
2. [타입 계층 한눈에 보기](#2-타입-계층-한눈에-보기)
3. [`AnyVal` — 아홉 가지 값 타입](#3-anyval--아홉-가지-값-타입)
4. [`AnyRef` — 참조 타입의 뿌리](#4-anyref--참조-타입의-뿌리)
5. [바닥의 두 타입: `Nothing`과 `Null`](#5-바닥의-두-타입-nothing과-null)
6. [값 타입 사이의 변환](#6-값-타입-사이의-변환)
7. [값 클래스(Value Class) — 객체처럼 쓰고 원시값처럼 실행되는 타입](#7-값-클래스value-class--객체처럼-쓰고-원시값처럼-실행되는-타입)
   - 7.1 [값 클래스 정의 규칙](#71-값-클래스-정의-규칙)
   - 7.2 [암시적 클래스와 결합 — "확장 메서드" 패턴](#72-암시적-클래스와-결합--확장-메서드-패턴)
   - 7.3 [결국 인스턴스가 만들어지는 경우](#73-결국-인스턴스가-만들어지는-경우)
   - 7.4 [값 클래스가 될 수 없는 경우](#74-값-클래스가-될-수-없는-경우)
8. [Scala 3에서는: opaque type이 값 클래스를 대체](#8-scala-3에서는-opaque-type이-값-클래스를-대체)
9. [요약](#9-요약)

---

## 1. 개요: Scala는 왜 "모든 것이 객체"라고 말하는가

Scala는 Java와 달리 `int`, `boolean` 같은 원시 타입(primitive type)과 클래스 타입을 구분하지 않습니다. **정수, 불리언, 함수까지 포함해 모든 값이 타입을 가진 객체**로 취급됩니다.

```scala
val n: Int = 42
val isReady: Boolean = true
val greet: String => String = name => s"Hi, $name"
// 위 셋 모두 결국 Any의 자손이다
```

> 📘 **처음 배우는 분께 — "통합된 타입 시스템"이 실무에 주는 이득**
>
> Java에서는 `int`(원시 타입)와 `Integer`(래퍼 클래스)가 별개라서 컬렉션에 담으려면 박싱(boxing)이 필요하고, 제네릭에도 원시 타입을 직접 못 넣습니다. Scala는 처음부터 `Int`도 하나의 타입으로 계층에 편입시켜서, `List[Any]`에 정수·문자열·함수를 함께 담는 것 같은 일이 타입 규칙상 자연스럽게 허용됩니다. 다만 실제 실행 시에는 JVM 바이트코드 최적화로 원시 타입 그대로 처리되는 경우가 많아 성능 손해는 없습니다.

이번 문서는 이 통합 타입 계층의 뼈대(`Any`/`AnyVal`/`AnyRef`/`Nothing`/`Null`)와, 그 위에서 "객체처럼 보이지만 실행 시 오버헤드가 없는" 타입을 만드는 **값 클래스(value class)** 를 함께 다룹니다.

---

## 2. 타입 계층 한눈에 보기

```
                     Any
                    /   \
               AnyVal    AnyRef
              /  |  \       \
          Int Boolean ...   (사용자 정의 클래스, String, List, ...)
              \   |   /        \
               Nothing ------ Null
```

| 타입 | 위치 | 의미 |
|---|---|---|
| `Any` | 최상위(top type) | 모든 타입의 조상. `equals`, `hashCode`, `toString` 등 공통 메서드 정의 |
| `AnyVal` | `Any`의 자식 | 아홉 가지 값 타입(숫자·`Boolean`·`Unit`)의 조상 |
| `AnyRef` | `Any`의 자식 | 참조 타입의 조상. Java의 `java.lang.Object`에 대응 |
| `Nothing` | 최하위(bottom type) | 모든 타입의 자손. 값이 존재하지 않는 상황(예외, 무한루프)을 표현 |
| `Null` | `AnyRef` 계열의 바닥 | 참조 타입 전용 바닥 타입. 유일한 값은 `null` |

> 💡 **왜 필요한가 — 계층이 두 갈래(`AnyVal`/`AnyRef`)로 갈라지는 이유**
>
> JVM 위에서 동작해야 하므로 Scala는 JVM의 현실(원시 타입 vs 객체 참조)을 완전히 숨길 수 없습니다. `AnyVal` 자식들은 JVM 바이트코드 수준에서 `int`, `boolean` 같은 원시 타입으로 컴파일되고(성능 목적), `AnyRef` 자식들은 힙에 할당되는 참조로 컴파일됩니다. `Any`라는 공통 조상을 두어 **타입 시스템 위에서는** 둘을 똑같이 다룰 수 있게 해 두었을 뿐, 실행 시에는 여전히 이 구분이 유지됩니다.

---

## 3. `AnyVal` — 아홉 가지 값 타입

`AnyVal`의 자식은 정확히 아홉 개이며, 모두 `null`이 될 수 없습니다.

| 분류 | 타입 |
|---|---|
| 숫자 | `Double`, `Float`, `Long`, `Int`, `Short`, `Byte` |
| 논리값 | `Boolean` |
| 문자 | `Char` |
| "의미 있는 정보 없음" | `Unit` |

- `Unit`은 Java의 `void`에 대응하며, **유일한 값 `()`** 를 가집니다. 반환할 값이 없는 함수(부수 효과만 있는 함수)의 반환 타입으로 쓰입니다.

```scala
def printLine(s: String): Unit = println(s)
val result: Unit = printLine("hi")   // result는 ()
```

---

## 4. `AnyRef` — 참조 타입의 뿌리

- `String`, `List`, 사용자가 정의한 모든 `class`/`case class`/`trait`는 `AnyRef`의 자손입니다.
- JVM 환경에서 `AnyRef`는 **`java.lang.Object`의 별칭**과 같이 동작합니다.

```scala
class Point(val x: Int, val y: Int)   // 암묵적으로 extends AnyRef
```

> 📘 **처음 배우는 분께 — `Any`, `AnyVal`, `AnyRef` 이름이 헷갈릴 때**
>
> - `Any` = "아무거나" (전체 최상위)
> - `AnyVal` = "아무 **값(Val)** 타입" = 원시 타입 계열
> - `AnyRef` = "아무 **참조(Ref)** 타입" = 객체 계열
>
> 이름 그대로 값 타입 쪽과 참조 타입 쪽을 가리키는 이름이라고 생각하면 헷갈리지 않습니다.

---

## 5. 바닥의 두 타입: `Nothing`과 `Null`

| 타입 | 자손 관계 | 용도 |
|---|---|---|
| `Nothing` | 모든 타입(값 타입 포함)의 자손 | 예외를 던지는 표현식, 절대 정상 종료하지 않는 함수의 반환 타입 |
| `Null` | `AnyRef` 계열의 자손(값 타입의 자손은 아님) | `null` 리터럴의 타입. JVM과의 상호운용을 위해 존재하며, 공식 문서도 실제 코드에서는 쓰지 말라고 권고 |

```scala
def fail(msg: String): Nothing = throw new RuntimeException(msg)
// Nothing은 모든 타입의 부분타입이므로, fail(...)의 결과를
// 어떤 타입이 기대되는 자리에도 끼워 넣을 수 있다
val x: Int = if (n > 0) n else fail("음수 불가")
```

> 💡 **왜 필요한가 — `Nothing`이 "모든 타입의 자손"이라는 게 왜 쓸모 있나**
>
> `if`의 두 분기는 같은 타입을 반환해야 타입 검사가 통과합니다. 한쪽 분기가 예외를 던지고 끝난다면 그쪽은 "정상적으로 반환할 값"이 없는데, `Nothing`을 그 분기의 타입으로 주면 **어떤 타입과도 타입 검사상 충돌하지 않으므로** `if` 전체가 자연스럽게 타입을 갖게 됩니다.

`Null`은 값 타입(`AnyVal` 계열)의 자손이 아니므로, `val n: Int = null`은 컴파일되지 않습니다. `null`은 오직 참조 타입 자리에만 들어갈 수 있습니다.

---

## 6. 값 타입 사이의 변환

`AnyVal` 자식끼리는 명시적 변환 메서드(`.toX`)로 단방향 캐스팅이 가능합니다.

```scala
val i: Int = 42
val l: Long = i.toLong      // 안전 (정밀도 손실 없음)
val f: Float = l.toFloat    // 정밀도 손실 가능
```

- 값이 커지는 방향(`Int → Long → Float` 등)은 대체로 안전하지만, `Long → Float`는 정밀도가 떨어질 수 있어 최신 Scala 버전에서는 이 암묵적 변환 자체가 **지원 중단(deprecated)** 되었습니다.
- 반대 방향(`Float`를 `Long` 자리에)은 **자동으로 되돌아가지 않으며**, 항상 명시적으로 `.toLong` 등을 호출해야 합니다.

```scala
val values: List[Any] = List("문자열", 1, true, 'a', (x: Int) => x + 1)
// 서로 다른 타입이지만 모두 Any의 자손이므로 한 리스트에 담을 수 있다
```

---

## 7. 값 클래스(Value Class) — 객체처럼 쓰고 원시값처럼 실행되는 타입

`AnyVal`을 상속하는 사용자 정의 클래스를 **값 클래스(value class)** 라고 부릅니다. Scala 2.10에 도입되었으며, 목적은 하나입니다.

> **타입 안전성을 위해 값을 감싸는 래퍼(wrapper) 클래스를 만들되, 런타임에는 객체 할당(allocation) 비용이 들지 않게 한다.**

```scala
class Meters(val value: Double) extends AnyVal {
  def +(that: Meters): Meters = new Meters(value + that.value)
}

val distance = new Meters(3.5)
```

> 💡 **왜 필요한가 — "그냥 `Double`을 쓰면 안 되나?"**
>
> `def move(distance: Double)`이라고 쓰면 미터인지 킬로미터인지 초인지 타입만 봐서는 알 수 없어서, 실수로 단위를 헷갈린 값을 넘겨도 컴파일러가 잡아 주지 못합니다. `class Meters(val value: Double) extends AnyVal`처럼 감싸면 `move(distance: Meters)`가 되어 **다른 단위를 넘기면 컴파일 에러**가 납니다. 문제는 보통 이런 래퍼 클래스를 쓰면 매번 새 객체를 힙에 할당하는 비용이 붙는다는 점인데, 값 클래스는 (조건이 맞으면) 컴파일 시점에 래퍼를 지우고 **원래의 `Double`을 그대로 사용**하도록 최적화되어 이 비용을 없앱니다.

### 7.1 값 클래스 정의 규칙

값 클래스가 되려면 다음을 모두 지켜야 합니다.

- `AnyVal`을 상속(`extends AnyVal`)한다.
- 생성자 파라미터는 **`val` 하나뿐**이어야 한다(공개(public)여야 함).
- 클래스 본문에는 `def`만 둘 수 있다. `val`, `var`, 중첩 클래스/트레이트/객체는 둘 수 없다.
- 상속하는 트레이트가 있다면 **유니버설 트레이트(universal trait)** — 즉 `Any`를 상속하고 `def`만 갖는 트레이트 — 여야 한다.

```scala
trait Printable extends Any {
  def print(): Unit
}

class Meters(val value: Double) extends AnyVal with Printable {
  def print(): Unit = println(s"$value m")
}
```

### 7.2 암시적 클래스와 결합 — "확장 메서드" 패턴

값 클래스는 `implicit class`와 함께 쓰여 **기존 타입에 메서드를 추가하면서도 할당 비용이 없는 확장(extension)** 을 만드는 데 자주 활용됩니다.

```scala
implicit class RichInt(val self: Int) extends AnyVal {
  def toHexStr: String = Integer.toHexString(self)
}

3.toHexStr   // "3" — RichInt 인스턴스를 실제로 만들지 않고
             // 컴파일러가 정적 메서드 호출로 최적화함
```

> 📘 **처음 배우는 분께 — `implicit class`가 뭐였는지 기억이 안 난다면**
>
> `implicit class`는 기존 타입(여기서는 `Int`)에 새 메서드를 덧붙이는 Scala 2 방식입니다(`00_prerequisites_scala_basics.md` 9번 항목 참고). 여기에 `extends AnyVal`을 얹으면, 컴파일러가 "이 래퍼는 실행 시 굳이 안 만들어도 되겠다"고 판단해 인스턴스 생성을 생략하고 곧바로 안의 메서드 호출로 바꿔치기합니다. Scala 3에서는 이 조합 전체가 `extension` 키워드 하나로 대체됩니다(`03_contextual_extensions_typeclasses.md` 참고).

### 7.3 결국 인스턴스가 만들어지는 경우

값 클래스는 "대부분의 경우" 할당을 생략할 뿐, 다음 상황에서는 실제 객체가 만들어집니다.

- 값 클래스를 **다른 타입이나 유니버설 트레이트로 취급**할 때(업캐스팅)
- **배열의 원소**로 대입할 때
- `isInstanceOf`/`asInstanceOf`나 패턴 매칭처럼 **런타임 타입 검사**를 수행할 때

```scala
def asPrintable(p: Printable): Unit = p.print()
asPrintable(new Meters(3.5))
// Meters를 Printable(트레이트)로 취급하는 순간 실제 객체가 할당된다
```

> ⚠️ **짚고 넘어가기 — "할당 안 함"은 약속이 아니라 최적화**
>
> 값 클래스는 "절대 객체를 안 만든다"는 보장이 아니라, **조건이 맞을 때 컴파일러가 할당을 생략해 주는 최적화**입니다. 위 세 가지 경우처럼 값 클래스의 정체가 드러나야 하는 상황(다형성 호출, 배열, 타입 검사)에서는 결국 진짜 객체가 필요해집니다. 성능이 목적이라면 "이 코드 경로가 저 세 경우에 해당하지 않는지" 확인해야 실제로 이득을 봅니다.

### 7.4 값 클래스가 될 수 없는 경우

다음에 해당하면 값 클래스로 정의할 수 없습니다.

- 생성자 파라미터가 **둘 이상**인 경우
- **보조 생성자(secondary constructor)** 를 두는 경우
- 클래스 내부에 **중첩된 클래스/트레이트/객체**를 두는 경우
- `equals`나 `hashCode`를 **직접 재정의(concrete override)** 하는 경우
- 감싸는 대상(생성자 파라미터의 타입)이 **다른 사용자 정의 값 클래스**인 경우
- 값 클래스 자체가 **최상위(top-level)이거나 정적으로 접근 가능한 객체 안**에 있지 않은 경우(지역 클래스로는 불가)
- 다른 클래스가 이 값 클래스를 **상속**(확장)하는 경우
- `@specialized` **타입 파라미터**를 두는 경우

---

## 8. Scala 3에서는: opaque type이 값 클래스를 대체

- 값 클래스는 Scala 3에서도 **호환을 위해 계속 지원**됩니다.
- 다만 공식 문서는 새 코드에서는 **불투명 타입(opaque type)** 사용을 권장합니다.
- 불투명 타입은 값 클래스처럼 "컴파일 타임에만 구분되고 런타임에는 원래 타입 그대로 처리"되는 성질을 가지면서, 값 클래스의 여러 제약(파라미터 하나만, 유니버설 트레이트만 등)에서 자유롭습니다.

```scala
// Scala 2 스타일
class Meters(val value: Double) extends AnyVal

// Scala 3 스타일
opaque type Meters = Double
```

> 자세한 비교는 `04_new_types.md`(opaque type)와 `00_prerequisites_scala_basics.md` 12번 대응표를 참고하세요.

---

## 9. 요약

- Scala는 원시 타입과 참조 타입을 구분하지 않고, **`Any`를 최상위로 하는 하나의 타입 계층**으로 통합한다.
- `AnyVal`은 아홉 개의 값 타입(숫자·`Boolean`·`Char`·`Unit`)의 조상, `AnyRef`는 모든 참조 타입(사용자 정의 클래스 포함)의 조상이다.
- `Nothing`은 모든 타입의 자손(바닥 타입)으로 예외·무한루프 표현에, `Null`은 참조 타입 전용 바닥 타입으로 `null` 표현에 쓰인다.
- **값 클래스(`extends AnyVal`)** 는 타입 안전한 래퍼를 만들면서도 할당 비용을 없애는 기법이며, 파라미터 하나·`def`만 허용 등 엄격한 규칙을 따른다.
- 업캐스팅·배열 저장·런타임 타입 검사 상황에서는 값 클래스도 결국 실제 객체로 할당된다.
- Scala 3에서는 값 클래스보다 **opaque type**이 권장되는 대안이다.

---

## 참고 자료

- [Unified Types (Tour of Scala)](https://docs.scala-lang.org/tour/unified-types.html)
- [Value Classes and Universal Traits](https://docs.scala-lang.org/overviews/core/value-classes.html)
- `00_prerequisites_scala_basics.md` — `implicit class`, 대응표
- `04_new_types.md` — opaque type
