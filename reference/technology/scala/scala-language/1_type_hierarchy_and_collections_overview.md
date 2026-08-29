# Scala 타입 계층과 컬렉션 아키텍처

## 타입 계층: Any, AnyVal, AnyRef와 값 클래스

> 원문: https://docs.scala-lang.org/tour/unified-types.html , https://docs.scala-lang.org/overviews/core/value-classes.html

---

### 목차

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

### 1. 개요: Scala는 왜 "모든 것이 객체"라고 말하는가

- Scala는 Java와 달리 `int`, `boolean` 같은 원시 타입(primitive type)과 클래스 타입을 구분하지 않음
- 정수·불리언·함수까지 포함해 모든 값이 타입을 가진 객체로 취급됨

```scala
val n: Int = 42
val isReady: Boolean = true
val greet: String => String = name => s"Hi, $name"
// 위 셋 모두 결국 Any의 자손이다
```

처음 배우는 분께 — "통합된 타입 시스템"이 실무에 주는 이득
- Java에서는 `int`(원시 타입)와 `Integer`(래퍼 클래스)가 별개 → 컬렉션에 담으려면 박싱(boxing) 필요, 제네릭에도 원시 타입을 직접 못 넣음
- Scala는 처음부터 `Int`도 하나의 타입으로 계층에 편입 → `List[Any]`에 정수·문자열·함수를 함께 담는 것이 타입 규칙상 자연스럽게 허용됨
- 다만 실제 실행 시에는 JVM 바이트코드 최적화로 원시 타입 그대로 처리되는 경우가 많음 → 성능 손해는 없음

이번 문서는 이 통합 타입 계층의 뼈대(`Any`/`AnyVal`/`AnyRef`/`Nothing`/`Null`)와, 그 위에서 "객체처럼 보이지만 실행 시 오버헤드가 없는" 타입을 만드는 값 클래스(value class)를 함께 다룸.

---

### 2. 타입 계층 한눈에 보기

```
                     Any
                    /   \
               AnyVal    AnyRef
              /  |  \       \
          Int Boolean ...   (사용자 정의 클래스, String, List, ...)
              \   |   /        \
               Nothing ------ Null
```

- `Any`
  - 위치: 최상위(top type)
  - 의미: 모든 타입의 조상. `equals`, `hashCode`, `toString` 등 공통 메서드 정의
- `AnyVal`
  - 위치: `Any`의 자식
  - 의미: 아홉 가지 값 타입(숫자·`Boolean`·`Unit`)의 조상
- `AnyRef`
  - 위치: `Any`의 자식
  - 의미: 참조 타입의 조상. Java의 `java.lang.Object`에 대응
- `Nothing`
  - 위치: 최하위(bottom type)
  - 의미: 모든 타입의 자손. 값이 존재하지 않는 상황(예외, 무한루프)을 표현
- `Null`
  - 위치: `AnyRef` 계열의 바닥
  - 의미: 참조 타입 전용 바닥 타입. 유일한 값은 `null`

왜 필요한가 — 계층이 두 갈래(`AnyVal`/`AnyRef`)로 갈라지는 이유
- JVM 위에서 동작해야 함 → JVM의 현실(원시 타입 vs 객체 참조)을 완전히 숨길 수 없음
- `AnyVal` 자식들은 JVM 바이트코드 수준에서 `int`, `boolean` 같은 원시 타입으로 컴파일됨(성능 목적)
- `AnyRef` 자식들은 힙에 할당되는 참조로 컴파일됨
- `Any`라는 공통 조상 → 타입 시스템 위에서는 둘을 똑같이 다룰 수 있음, 실행 시에는 여전히 이 구분이 유지됨

---

### 3. `AnyVal` — 아홉 가지 값 타입

- `AnyVal`의 자식은 정확히 아홉 개, 모두 `null`이 될 수 없음
- 숫자: `Double`, `Float`, `Long`, `Int`, `Short`, `Byte`
- 논리값: `Boolean`
- 문자: `Char`
- "의미 있는 정보 없음": `Unit`
- `Unit`은 Java의 `void`에 대응, 유일한 값 `()`를 가짐 → 반환할 값이 없는 함수(부수 효과만 있는 함수)의 반환 타입으로 쓰임

```scala
def printLine(s: String): Unit = println(s)
val result: Unit = printLine("hi")   // result는 ()
```

---

### 4. `AnyRef` — 참조 타입의 뿌리

- `String`, `List`, 사용자가 정의한 모든 `class`/`case class`/`trait`는 `AnyRef`의 자손
- JVM 환경에서 `AnyRef`는 `java.lang.Object`의 별칭과 같이 동작

```scala
class Point(val x: Int, val y: Int)   // 암묵적으로 extends AnyRef
```

처음 배우는 분께 — `Any`, `AnyVal`, `AnyRef` 이름이 헷갈릴 때
- `Any` = "아무거나" (전체 최상위)
- `AnyVal` = "아무 값(Val) 타입" = 원시 타입 계열
- `AnyRef` = "아무 참조(Ref) 타입" = 객체 계열
- 이름 그대로 값 타입 쪽과 참조 타입 쪽을 가리키는 이름으로 이해하면 헷갈리지 않음

---

### 5. 바닥의 두 타입: `Nothing`과 `Null`

- `Nothing`
  - 자손 관계: 모든 타입(값 타입 포함)의 자손
  - 용도: 예외를 던지는 표현식, 절대 정상 종료하지 않는 함수의 반환 타입
- `Null`
  - 자손 관계: `AnyRef` 계열의 자손(값 타입의 자손은 아님)
  - 용도: `null` 리터럴의 타입. JVM과의 상호운용을 위해 존재하며, 공식 문서도 실제 코드에서는 쓰지 말라고 권고

```scala
def fail(msg: String): Nothing = throw new RuntimeException(msg)
// Nothing은 모든 타입의 부분타입이므로, fail(...)의 결과를
// 어떤 타입이 기대되는 자리에도 끼워 넣을 수 있다
val x: Int = if (n > 0) n else fail("음수 불가")
```

왜 필요한가 — `Nothing`이 "모든 타입의 자손"이라는 게 왜 쓸모 있나
- `if`의 두 분기는 같은 타입을 반환해야 타입 검사가 통과됨
- 한쪽 분기가 예외를 던지고 끝나면 "정상적으로 반환할 값"이 없음 → 그 분기의 타입을 `Nothing`으로 두면 어떤 타입과도 타입 검사상 충돌하지 않음 → `if` 전체가 자연스럽게 타입을 가짐

`Null`은 값 타입(`AnyVal` 계열)의 자손이 아님 → `val n: Int = null`은 컴파일되지 않음. `null`은 오직 참조 타입 자리에만 들어갈 수 있음.

---

### 6. 값 타입 사이의 변환

- `AnyVal` 자식끼리는 명시적 변환 메서드(`.toX`)로 단방향 캐스팅 가능

```scala
val i: Int = 42
val l: Long = i.toLong      // 안전 (정밀도 손실 없음)
val f: Float = l.toFloat    // 정밀도 손실 가능
```

- 값이 커지는 방향(`Int → Long → Float` 등)은 대체로 안전
  - 단, `Long → Float`는 정밀도가 떨어질 수 있음 → 최신 Scala 버전에서는 이 암묵적 변환 자체가 지원 중단(deprecated)
- 반대 방향(`Float`를 `Long` 자리에)은 자동으로 되돌아가지 않음 → 항상 명시적으로 `.toLong` 등을 호출해야 함

```scala
val values: List[Any] = List("문자열", 1, true, 'a', (x: Int) => x + 1)
// 서로 다른 타입이지만 모두 Any의 자손이므로 한 리스트에 담을 수 있다
```

---

### 7. 값 클래스(Value Class) — 객체처럼 쓰고 원시값처럼 실행되는 타입

- `AnyVal`을 상속하는 사용자 정의 클래스를 값 클래스(value class)라 부름
- Scala 2.10에 도입, 목적은 하나

목적: 타입 안전성을 위해 값을 감싸는 래퍼(wrapper) 클래스를 만들되, 런타임에는 객체 할당(allocation) 비용이 들지 않게 함.

```scala
class Meters(val value: Double) extends AnyVal {
  def +(that: Meters): Meters = new Meters(value + that.value)
}

val distance = new Meters(3.5)
```

왜 필요한가 — "그냥 `Double`을 쓰면 안 되나?"
- `def move(distance: Double)`이라고 쓰면 미터인지 킬로미터인지 초인지 타입만 봐서는 알 수 없음 → 실수로 단위를 헷갈린 값을 넘겨도 컴파일러가 잡아 주지 못함
- `class Meters(val value: Double) extends AnyVal`처럼 감싸면 `move(distance: Meters)`가 됨 → 다른 단위를 넘기면 컴파일 에러
- 문제는 보통 이런 래퍼 클래스를 쓰면 매번 새 객체를 힙에 할당하는 비용이 붙는다는 점 → 값 클래스는 (조건이 맞으면) 컴파일 시점에 래퍼를 지우고 원래의 `Double`을 그대로 사용하도록 최적화되어 이 비용을 없앰

#### 7.1 값 클래스 정의 규칙

값 클래스가 되려면 다음을 모두 지켜야 함.

- `AnyVal`을 상속(`extends AnyVal`)
- 생성자 파라미터는 `val` 하나뿐이어야 함(공개(public)여야 함)
- 클래스 본문에는 `def`만 둘 수 있음 → `val`, `var`, 중첩 클래스/트레이트/객체는 둘 수 없음
- 상속하는 트레이트가 있다면 유니버설 트레이트(universal trait) — 즉 `Any`를 상속하고 `def`만 갖는 트레이트 — 여야 함

```scala
trait Printable extends Any {
  def print(): Unit
}

class Meters(val value: Double) extends AnyVal with Printable {
  def print(): Unit = println(s"$value m")
}
```

#### 7.2 암시적 클래스와 결합 — "확장 메서드" 패턴

- 값 클래스는 `implicit class`와 함께 쓰여 기존 타입에 메서드를 추가하면서도 할당 비용이 없는 확장(extension)을 만드는 데 자주 활용됨

```scala
implicit class RichInt(val self: Int) extends AnyVal {
  def toHexStr: String = Integer.toHexString(self)
}

3.toHexStr   // "3" — RichInt 인스턴스를 실제로 만들지 않고
             // 컴파일러가 정적 메서드 호출로 최적화함
```

처음 배우는 분께 — `implicit class`가 뭐였는지 기억이 안 난다면
- `implicit class`는 기존 타입(여기서는 `Int`)에 새 메서드를 덧붙이는 Scala 2 방식(`00_prerequisites_scala_basics.md` 9번 항목 참고)
- 여기에 `extends AnyVal`을 얹으면 → 컴파일러가 "이 래퍼는 실행 시 굳이 안 만들어도 되겠다"고 판단해 인스턴스 생성을 생략하고 곧바로 안의 메서드 호출로 바꿔치기함
- Scala 3에서는 이 조합 전체가 `extension` 키워드 하나로 대체됨(`03_contextual_extensions_typeclasses.md` 참고)

#### 7.3 결국 인스턴스가 만들어지는 경우

값 클래스는 "대부분의 경우" 할당을 생략할 뿐, 다음 상황에서는 실제 객체가 만들어짐.

- 값 클래스를 다른 타입이나 유니버설 트레이트로 취급할 때(업캐스팅)
- 배열의 원소로 대입할 때
- `isInstanceOf`/`asInstanceOf`나 패턴 매칭처럼 런타임 타입 검사를 수행할 때

```scala
def asPrintable(p: Printable): Unit = p.print()
asPrintable(new Meters(3.5))
// Meters를 Printable(트레이트)로 취급하는 순간 실제 객체가 할당된다
```

짚고 넘어가기 — "할당 안 함"은 약속이 아니라 최적화
- 값 클래스는 "절대 객체를 안 만든다"는 보장이 아니라, 조건이 맞을 때 컴파일러가 할당을 생략해 주는 최적화
- 위 세 가지 경우처럼 값 클래스의 정체가 드러나야 하는 상황(다형성 호출, 배열, 타입 검사)에서는 결국 진짜 객체가 필요해짐
- 성능이 목적이라면 "이 코드 경로가 저 세 경우에 해당하지 않는지" 확인해야 실제로 이득을 봄

#### 7.4 값 클래스가 될 수 없는 경우

다음에 해당하면 값 클래스로 정의할 수 없음.

- 생성자 파라미터가 둘 이상인 경우
- 보조 생성자(secondary constructor)를 두는 경우
- 클래스 내부에 중첩된 클래스/트레이트/객체를 두는 경우
- `equals`나 `hashCode`를 직접 재정의(concrete override)하는 경우
- 감싸는 대상(생성자 파라미터의 타입)이 다른 사용자 정의 값 클래스인 경우
- 값 클래스 자체가 최상위(top-level)이거나 정적으로 접근 가능한 객체 안에 있지 않은 경우(지역 클래스로는 불가)
- 다른 클래스가 이 값 클래스를 상속(확장)하는 경우
- `@specialized` 타입 파라미터를 두는 경우

---

### 8. Scala 3에서는: opaque type이 값 클래스를 대체

- 값 클래스는 Scala 3에서도 호환을 위해 계속 지원됨
- 다만 공식 문서는 새 코드에서는 불투명 타입(opaque type) 사용을 권장
- 불투명 타입은 값 클래스처럼 "컴파일 타임에만 구분되고 런타임에는 원래 타입 그대로 처리"되는 성질을 가지면서, 값 클래스의 여러 제약(파라미터 하나만, 유니버설 트레이트만 등)에서 자유로움

```scala
// Scala 2 스타일
class Meters(val value: Double) extends AnyVal

// Scala 3 스타일
opaque type Meters = Double
```

자세한 비교는 `04_new_types.md`(opaque type)와 `00_prerequisites_scala_basics.md` 12번 대응표 참고.

---

### 9. 요약

- Scala는 원시 타입과 참조 타입을 구분하지 않고, `Any`를 최상위로 하는 하나의 타입 계층으로 통합함
- `AnyVal`은 아홉 개의 값 타입(숫자·`Boolean`·`Char`·`Unit`)의 조상, `AnyRef`는 모든 참조 타입(사용자 정의 클래스 포함)의 조상
- `Nothing`은 모든 타입의 자손(바닥 타입)으로 예외·무한루프 표현에, `Null`은 참조 타입 전용 바닥 타입으로 `null` 표현에 쓰임
- 값 클래스(`extends AnyVal`)는 타입 안전한 래퍼를 만들면서도 할당 비용을 없애는 기법 → 파라미터 하나·`def`만 허용 등 엄격한 규칙을 따름
- 업캐스팅·배열 저장·런타임 타입 검사 상황에서는 값 클래스도 결국 실제 객체로 할당됨
- Scala 3에서는 값 클래스보다 opaque type이 권장되는 대안

---

### 참고 자료

- [Unified Types (Tour of Scala)](https://docs.scala-lang.org/tour/unified-types.html)
- [Value Classes and Universal Traits](https://docs.scala-lang.org/overviews/core/value-classes.html)
- `00_prerequisites_scala_basics.md` — `implicit class`, 대응표
- `04_new_types.md` — opaque type

---

## 컬렉션 개요와 아키텍처 (Iterable 트레이트)

---

### 목차

1. [컬렉션 프레임워크가 추구하는 것](#1-컬렉션-프레임워크가-추구하는-것)
2. [불변 vs 가변, 패키지 구조](#2-불변-vs-가변-패키지-구조)
3. [컬렉션 계층 구조 한눈에 보기](#3-컬렉션-계층-구조-한눈에-보기)
4. [모든 컬렉션의 뿌리: `Iterable` 트레이트](#4-모든-컬렉션의-뿌리-iterable-트레이트)
5. [`Iterable`이 제공하는 주요 연산](#5-iterable이-제공하는-주요-연산)
6. [내부 아키텍처: 왜 `map`은 항상 "같은 종류"를 돌려주는가](#6-내부-아키텍처-왜-map은-항상-같은-종류를-돌려주는가)
7. [정리](#7-정리)

---

### 1. 컬렉션 프레임워크가 추구하는 것

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/introduction.html

- Scala 표준 라이브러리의 컬렉션 프레임워크는 리스트, 집합, 맵 등 서로 다른 자료구조를 하나의 일관된 어휘로 다룰 수 있게 설계됨
- 공식 문서는 이 설계가 지향하는 가치를 여섯 가지로 정리
  - 사용 편의성(Ease of use): 20~50개 정도의 메서드 어휘만으로 대부분의 문제를 해결. 부수효과가 없어 "컬렉션을 실수로 망가뜨릴까" 걱정할 필요가 없음
  - 간결성(Conciseness): 여러 줄의 반복문으로 하던 일을 함수형 문법 한 줄로 표현
  - 안전성(Safety): 정적 타입 + 함수형 스타일 덕분에 오류 대부분을 컴파일 시점에 잡아냄
  - 성능(Performance): 잘 튜닝된 라이브러리 구현을 재사용하므로, 직접 짠 코드보다 크게 못하지 않음
  - 병렬성(Parallelism): `.par`를 호출하면 여러 코어에서 병렬 실행 — 순차 컬렉션과 API가 동일
  - 범용성(Universality): `String`, `Array`처럼 컬렉션이 아닌 것처럼 보이는 타입도 동일한 연산을 지원

이 방향성을 보여주는 대표 예시가 `partition`.

```scala
val people: List[Person] = ???
val (minors, adults) = people.partition(_.age < 18)
// minors, adults 모두 List[Person] — 원본과 같은 종류의 컬렉션으로 되돌아옴
```

- 반복문 없이 한 줄로 분류가 끝남
- `List`가 아니라 `Vector`나 `Set`으로 바꿔도 똑같은 코드가 그대로 동작
- "어떤 컬렉션이든 같은 방식으로 다룰 수 있다"는 것이 이 프레임워크 전체를 관통하는 주제

---

### 2. 불변 vs 가변, 패키지 구조

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/overview.html

Scala 컬렉션은 불변(immutable)과 가변(mutable) 두 갈래로 명확히 나뉨.

- 불변 컬렉션: 한 번 만들어지면 절대 바뀌지 않음. "추가/삭제"처럼 보이는 연산도 사실은 새 컬렉션을 만들어 반환하는 것
- 가변 컬렉션: 제자리(in place)에서 원소를 갱신·추가·삭제할 수 있음

처음 배우는 분께 — 왜 기본값이 불변인가
- Scala는 특별한 이유가 없다면 불변 컬렉션을 기본으로 사용하도록 설계됨
- 여러 스레드가 같은 컬렉션을 동시에 들여다봐도 안전함 → "누가 몰래 내용을 바꿨는지" 추적할 필요가 없음
- 가변 컬렉션이 필요하면 반드시 명시적으로 가져와야 함

패키지도 이 구분을 그대로 반영.

- `scala.collection`: 불변/가변 구현이 함께 상속하는 추상 루트 트레이트
- `scala.collection.immutable`: 불변임이 보장된 구현체 (기본으로 쓰이는 것들)
- `scala.collection.mutable`: 가변 구현체 (사용하려면 명시적으로 import)

```scala
import scala.collection.mutable

val fixed = Set(1, 2, 3)          // scala.collection.immutable.Set (기본)
val grows = mutable.Set(1, 2, 3)  // 명시적으로 가변 버전을 가져옴
grows += 4                        // 제자리에서 변경 가능
```

자주 쓰는 타입(`List`, `Iterable`, `Seq`, `IndexedSeq`, `Vector`, `Range`, `LazyList`, `Iterator`, `StringBuilder`)은 `scala` 최상위 패키지에도 별칭(alias)이 걸려 있음 → 매번 전체 경로를 안 써도 됨.

---

### 3. 컬렉션 계층 구조 한눈에 보기

컬렉션 트레이트는 크게 네 갈래로 나뉘고, 이 문서의 주인공인 `Iterable`이 그 맨 위에 있음.

```
Iterable                     ← 모든 컬렉션의 공통 조상
 ├── Seq                     ← 순서가 있는 컬렉션
 │    ├── IndexedSeq         ← 인덱스로 빠르게 접근 (Vector, Array 등)
 │    └── LinearSeq          ← 머리/꼬리로 순회 (List 등)
 ├── Set                     ← 순서 없음, 중복 없음
 └── Map                     ← 키-값 쌍
```

- `Buffer`는 `Seq` 계열이지만 가변 전용으로만 존재함(예: `ArrayBuffer`, `ListBuffer`)
- `Seq`, `Set`, `Map` 각각은 `Iterable`이 정의한 걸 물려받으면서, 자기만의 `apply`를 추가함
  - `Seq`의 `apply`: 인덱스로 접근 — `seq(2)`는 "세 번째 원소"
  - `Set`의 `apply`: 포함 여부 검사 — `set(x)`는 "x가 들어 있는가"(Boolean)
  - `Map`의 `apply`: 키 조회 — `map(k)`는 "키 k에 대응하는 값"

핵심 원칙 — 균일 반환 타입(uniform return type): `List`에 `map`을 걸면 결과도 `List`, `Set`에 걸면 결과도 `Set`. 이 원칙이 왜 성립하는지는 6절에서 다룸.

```scala
List(1, 2, 3).map(_ * 2)   // List(2, 4, 6)   → 여전히 List
Set(1, 2, 3).map(_ * 2)    // Set(2, 4, 6)    → 여전히 Set
```

컬렉션 생성 문법도 종류에 상관없이 동일.

```scala
List(1, 2, 3)
Set(1, 2, 3)
Map("x" -> 24, "y" -> 25)
```

---

### 4. 모든 컬렉션의 뿌리: `Iterable` 트레이트

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html

`Iterable`은 컬렉션 계층의 최상단에 있는 트레이트로, 딱 하나의 추상 메서드만 요구함.

```scala
trait Iterable[A] {
  def iterator: Iterator[A]   // 이것 하나만 있으면 나머지는 전부 공짜로 따라옴
  // map, filter, foreach, fold, take, drop ... 수십 개가 이 위에서 구현됨
}
```

왜 하나의 메서드로 충분한가
- `iterator`는 "원소를 하나씩 꺼내는 방법"만 알려줌
- `Iterable` 트레이트는 이 방법 하나만 있으면 `map`, `filter`, `fold`, `take`, `grouped` 같은 수십 개 메서드를 공통 구현으로 만들어 제공함
- 새 컬렉션 타입을 만드는 사람 입장에서는 `iterator`만 구현하면 나머지 API 전체를 자동으로 얻는 셈

`Iterable`은 한 단계 더 기본적인 `IterableOnce[A]`를 상속함. `IterableOnce`는 "단 한 번만 순회할 수 있어도 됨"을 뜻하는 최소 계약이고, `Iterable`은 여기에 "몇 번이든 다시 순회할 수 있음"(`iterator`를 다시 호출하면 처음부터 다시 순회)이라는 보장을 얹은 것.

```scala
def sumAll(xs: IterableOnce[Int]): Int =
  xs.iterator.sum   // Iterator든 List든, 한 번 순회 가능한 것이면 다 받음
```

`Seq`, `Set`, `Map` 모두 `Iterable`의 자식 → `Iterable`이 제공하는 메서드는 어떤 컬렉션에서도 그대로 쓸 수 있음.

---

### 5. `Iterable`이 제공하는 주요 연산

`Iterable`의 메서드는 성격별로 다음과 같이 묶어볼 수 있음.

- 분할/윈도우: `grouped(n)`, `sliding(n)` — 아래 예시 참고
- 변환: `map`, `flatMap`, `collect` — 원소마다 함수를 적용해 새 컬렉션 생성
- 부분 선택: `take`, `drop`, `takeWhile`, `dropWhile`, `filter`, `filterNot` — 개수/조건으로 일부만 골라냄
- 집계: `foldLeft`, `foldRight`, `sum`, `product`, `min`, `max` — 원소들을 하나의 값으로 합침
- 변환(타입): `toList`, `toSet`, `toArray`, `toMap` — 다른 컬렉션 종류로 바꿔치기

`grouped`와 `sliding`은 이름이 비슷해 헷갈리기 쉬운데, 차이는 겹치는지 여부.

```scala
val xs = List(1, 2, 3, 4, 5)

xs.grouped(3).toList   // List(List(1,2,3), List(4,5))       — 겹치지 않고 잘라냄
xs.sliding(3).toList   // List(List(1,2,3), List(2,3,4), List(3,4,5)) — 한 칸씩 밀며 겹쳐서
```

`takeWhile`/`dropWhile`은 "조건이 깨지는 순간까지"를 기준으로 자름.

```scala
List(1, 2, 3, 10, 4).takeWhile(_ < 5)   // List(1, 2, 3)   — 10에서 멈춤 (뒤에 4가 있어도 무시)
List(1, 2, 3, 10, 4).dropWhile(_ < 5)   // List(10, 4)     — 앞부분을 버림
```

짚고 넘어가기 — `takeWhile`은 `filter`가 아니다
- `filter(_ < 5)`는 조건을 만족하는 원소를 전부 골라냄
- `takeWhile(_ < 5)`는 조건이 처음 깨지는 지점에서 즉시 멈춤
- 위 예시에서 `4`는 조건을 만족해도 `10` 뒤에 있다는 이유만으로 결과에서 빠짐

---

### 6. 내부 아키텍처: 왜 `map`은 항상 "같은 종류"를 돌려주는가

> 원문: https://docs.scala-lang.org/overviews/core/architecture-of-scala-213-collections.html

- Scala 2.13 컬렉션 재설계의 목표: 연산 구현을 중복 없이 한 곳에 모으면서도, 컬렉션마다 딱 맞는 반환 타입을 유지
- 이를 위해 내부적으로 템플릿 트레이트(template trait)를 타입 매개변수 3개로 추상화
  - `A`: 원소 타입
  - `CC[_]`: "어떤 컬렉션 종류인가"를 나타내는 타입 생성자 (`List`, `Set` 등)
  - `C`: 지금 다루는 구체적인 컬렉션 타입 (예: `List[Int]`)

`filter`처럼 원소 타입이 안 바뀌는 연산은 `C`를 반환하고, `map`처럼 원소 타입이 바뀔 수 있는 연산은 `CC[B]`를 반환하도록 시그니처가 나뉨.

```scala
// IterableOps 안에서 대략 이런 모양
def filter(p: A => Boolean): C          // 원소 타입 A 그대로 → 같은 구체 타입 C
def map[B](f: A => B): CC[B]            // 원소 타입이 B로 바뀜 → CC 껍데기에 B를 담아 재생성
```

`Map`처럼 타입 매개변수가 2개(키, 값)인 컬렉션은 `MapOps`가 따로 담당, `map` 오버로드가 두 벌 존재함.

```scala
// MapOps: 결과가 여전히 (키, 값) 쌍이면 Map으로 유지
def map[K2, V2](f: ((K, V)) => (K2, V2)): Map[K2, V2]

// IterableOps: 결과가 쌍이 아니면 그냥 Iterable로 격하
def map[B](f: ((K, V)) => B): Iterable[B]
```

```scala
val m = Map(1 -> "a", 2 -> "b")

m.map { case (k, v) => (k + 1, v) }   // Map(2 -> "a", 3 -> "b")  — 쌍 유지 → Map
m.map { case (k, v) => k + v.length } // Iterable(2, 3)           — 쌍 깨짐 → Iterable
```

컴파일러가 함수의 반환 타입을 보고 더 구체적인 오버로드를 자동으로 선택함 → 개발자는 별다른 타입 표기 없이도 "쌍이면 Map, 아니면 Iterable"이라는 결과를 얻음.

`CanBuildFrom`이 사라진 이유
- Scala 2.12까지는 `map`의 반환 타입을 결정하기 위해 암시적 `CanBuildFrom` 매개변수를 매 연산마다 끌고 다녔음
- 컴파일 속도 저하와 이해하기 어려운 타입 오류의 주범
- 2.13에서는 위처럼 오버로드 + 팩토리 트레이트 조합으로 대체되어 훨씬 단순해짐

- 실제 생성 로직은 컬렉션 자신이 들고 있지 않고, `IterableFactory`·`MapFactory` 같은 팩토리 트레이트가 담당 → 템플릿 트레이트는 "결과를 어떻게 만드는지"는 몰라도 되고, "무엇을 만들어야 하는지"만 알면 됨
- 기본적으로 대부분의 연산은 즉시 평가하지 않고 `View`로 계산을 지연시킴
- `groupBy`, `partition`처럼 결과를 바로 확정 지어야 하는 연산만 `StrictOptimized` 계열 트레이트가 `Builder`를 이용해 즉시(strict) 계산하도록 별도로 최적화되어 있음

---

### 7. 정리

- Scala 컬렉션은 사용성·간결성·안전성·성능·병렬성·범용성이라는 여섯 가지 가치를 목표로 설계됨
- 기본은 불변이며, 가변 컬렉션은 `scala.collection.mutable`에서 명시적으로 가져옴
- `Iterable` → `Seq`/`Set`/`Map`으로 이어지는 계층에서, `Iterable`은 `iterator` 단 하나의 추상 메서드로 `map`/`filter`/`fold` 등 방대한 공통 연산을 제공함
- `map`이 항상 "같은 종류"의 컬렉션을 돌려주는 것처럼 보이는 이유는, 내부적으로 `CC[_]`/`C` 타입 매개변수와 오버로드된 시그니처, 팩토리 트레이트가 조합되어 동작하기 때문 → 이 구조 덕분에 예전 `CanBuildFrom` 방식보다 컴파일이 빠르고 타입 오류도 이해하기 쉬워짐

---

### 참고 자료

- [Scala 2.13 Collections — Introduction](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html)
- [Scala 2.13 Collections — Overview](https://docs.scala-lang.org/overviews/collections-2.13/overview.html)
- [Scala 2.13 Collections — The Iterable Trait](https://docs.scala-lang.org/overviews/collections-2.13/trait-iterable.html)
- [Architecture of Scala 2.13's Collections](https://docs.scala-lang.org/overviews/core/architecture-of-scala-213-collections.html)
