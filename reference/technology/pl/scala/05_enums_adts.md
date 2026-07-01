# Scala 3 열거형과 대수적 데이터 타입

---

## 목차

1. [개요](#개요)
2. [열거형(Enumerations)](#열거형enumerations)
   - [기본 열거형](#기본-열거형)
   - [매개변수를 가지는 열거형](#매개변수를-가지는-열거형)
   - [열거형에 정의되는 메서드](#열거형에-정의되는-메서드)
   - [사용자 정의 멤버](#사용자-정의-멤버)
   - [사용자 정의 컴패니언 객체](#사용자-정의-컴패니언-객체)
   - [열거형 케이스에 대한 제약](#열거형-케이스에-대한-제약)
   - [열거형 케이스의 사용 중단(Deprecation)](#열거형-케이스의-사용-중단deprecation)
   - [자바 열거형과의 호환성](#자바-열거형과의-호환성)
   - [구현 세부 사항](#구현-세부-사항)
3. [대수적 데이터 타입(Algebraic Data Types, ADT)](#대수적-데이터-타입algebraic-data-types-adt)
   - [ADT의 기본 형태](#adt의-기본-형태)
   - [명시적 타입 선언](#명시적-타입-선언)
   - [열거형 케이스에 접근하기](#열거형-케이스에-접근하기)
   - [메서드와 컴패니언 객체](#메서드와-컴패니언-객체)
   - [혼합 열거형(Hybrid Enumerations)](#혼합-열거형hybrid-enumerations)
   - [매개변수 변성(Variance)에 대한 고려](#매개변수-변성variance에-대한-고려)
   - [문법 요약](#문법-요약)
4. [열거형과 ADT의 변환(Desugaring)](#열거형과-adt의-변환desugaring)
   - [핵심 용어](#핵심-용어)
   - [변환 규칙(1~9)](#변환-규칙19)
   - [싱글턴 케이스의 변환](#싱글턴-케이스의-변환)
   - [스코프 규칙](#스코프-규칙)
   - [자바 호환 열거형](#자바-호환-열거형)
   - [그 외 규칙](#그-외-규칙)
5. [참고 자료](#참고-자료)

---

## 개요

Scala 3의 열거형(enum) 섹션은 크게 세 가지 주제를 다룹니다.

1. **열거형(Enumerations)** — 이름이 부여된 값들의 집합으로 구성되는 타입을 정의하는 방법
2. **대수적 데이터 타입(Algebraic Data Types, ADT)** — 열거형(enum) 구문을 활용해 ADT 및 일반화 대수적 데이터 타입(Generalized Algebraic Data Types, GADT)을 표현하는 방법
3. **열거형과 ADT의 변환(Translation of Enums and ADTs)** — 열거형과 ADT가 어떻게 핵심 언어 기능으로 디슈가링(desugaring, 문법 설탕 해제)되는지에 대한 설명

> 📘 **처음 배우는 분께 — enum은 "sealed trait + case class"의 줄임말입니다**
>
> Scala 2에서 "정해진 몇 가지 경우만 가지는 타입"을 만들려면, `sealed trait`를 선언하고
> 그 아래에 `case class`/`case object`를 줄줄이 늘어놓아야 했습니다(00번 4·8번 참고).
> Scala 3의 `enum`은 바로 이 흔한 패턴을 한 단어로 짧게 쓰는 문법입니다.
>
> ```scala
> // Scala 2 스타일 (00번 4번에서 본 그 패턴)
> sealed trait Color
> case object Red   extends Color
> case object Green extends Color
> case object Blue  extends Color
>
> // Scala 3 enum — 위와 사실상 같은 것
> enum Color:
>   case Red, Green, Blue
> ```
>
> 그래서 이 문서는 "enum이라는 새 기능"을 배우는 게 아니라, "이미 있던 패턴을 짧게 쓰는 법"을
> 배우는 것에 가깝습니다. 뒤에서 "디슈가링(desugaring)"이라며 enum을 `sealed class`로 다시
> 풀어내는 이유가 이것입니다 — 컴파일러는 결국 위의 Scala 2 코드와 비슷한 형태로 바꿔 줍니다.

> 💡 **왜 필요한가 — ADT가 무엇이고 왜 중요한가**
>
> "대수적 데이터 타입(ADT)"이라는 말이 어렵게 들리지만, 뜻은 단순합니다.
> **"이 타입이 가질 수 있는 경우의 수가 딱 정해져 있다"**는 데이터를 말합니다(00번 8번).
> 예: 신호등은 빨강/노랑/초록 셋뿐, 결과는 성공/실패 둘뿐.
>
> 경우의 수가 닫혀 있으면 컴파일러가 `match`에서 빠뜨린 경우를 잡아 줍니다.
> "있을 수 없는 상태"를 아예 타입으로 표현하지 못하게 막는 것 — 이것이 ADT의 실용적 가치이고,
> enum은 그 ADT를 가장 쉽게 쓰는 도구입니다.

---

## 열거형(Enumerations)

### 기본 열거형

열거형(enum)은 이름이 붙은 값(named values)들의 집합으로 구성되는 타입을 정의할 때 사용합니다.

```scala
enum Color:
  case Red, Green, Blue
```

이 코드는 봉인된(sealed) 클래스 `Color`를 생성하고, 컴패니언 객체(companion object)를 통해 `Color.Red`, `Color.Green`, `Color.Blue` 세 값에 접근할 수 있습니다.

> 📘 **처음 배우는 분께 — "봉인된(sealed)"과 "컴패니언 객체"가 왜 나오나**
>
> 위 한 줄짜리 `enum Color`가 컴파일러 안에서는 두 가지로 변신합니다.
> - **봉인된(sealed) 클래스 `Color`** — "Color를 상속하는 건 여기 적힌 게 전부"라는 뜻(00번 4번).
>   그래서 나중에 `match`로 색을 분기할 때 컴파일러가 빠진 경우를 잡아 줄 수 있습니다.
> - **컴패니언 객체 `Color`** — 클래스와 같은 이름의 싱글턴 객체(00번 5번).
>   `Color.Red`처럼 점을 찍어 값에 접근하는데, 그 `Red`들이 바로 이 객체 안에 담깁니다.
>
> 즉 `Color`라는 이름은 "타입(클래스)"이면서 동시에 "값들을 담은 상자(객체)"이기도 합니다.

---

### 매개변수를 가지는 열거형

열거형(enum)은 매개변수(parameter)를 지원합니다.

```scala
enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)
```

매개변수는 명시적인 `extends` 절을 통해 지정됩니다. 각 케이스(case)가 부모 열거형 클래스를 명시적으로 확장하면서 생성자 인자를 함께 전달하는 형태입니다.

> 📘 **처음 배우는 분께 — 각 case가 값을 들고 다닐 수 있습니다**
>
> Java의 enum과 비슷하게, Scala의 enum도 각 항목에 데이터를 붙일 수 있습니다.
> `enum Color(val rgb: Int)`는 "모든 Color는 rgb 정수를 하나 가진다"는 뜻이고,
> `case Red extends Color(0xFF0000)`는 "Red는 그 rgb 값으로 빨강 코드를 쓴다"는 뜻입니다.
> `Color.Red.rgb`처럼 꺼내 쓸 수 있습니다. Java로 치면 enum 생성자에 값을 넘기는 것과 같습니다.

---

### 열거형에 정의되는 메서드

각 열거형 값은 `ordinal` 메서드로 조회할 수 있는 고유한 정수(unique integer)와 연관됩니다.

```scala
enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)

val red = Color.Red
val ord = red.ordinal
assert(ord == 0)
```

`ordinal` 값은 케이스가 선언된 순서대로 0부터 시작하여 부여됩니다. 위 예시에서 `Red`는 0, `Green`은 1, `Blue`는 2의 서수(ordinal)를 가집니다.

> 📘 **처음 배우는 분께 — ordinal(서수)이란?**
>
> "서수(ordinal)"는 각 케이스에 매겨진 **순번**입니다. 선언한 순서대로 0, 1, 2... 가 자동으로 붙습니다.
> Java enum의 `ordinal()`과 같은 개념입니다. "지금 이 값이 몇 번째로 선언된 것인가"를 알려 줄 뿐,
> 보통은 직접 쓸 일이 많지 않고, 직렬화나 배열 인덱싱 같은 데서 가끔 씁니다.
>
> ⚠️ 다만 선언 순서를 바꾸면 서수도 바뀝니다. 서수 숫자 자체를 어딘가에 저장해 두면
> 나중에 케이스 순서를 바꿨을 때 의미가 어긋날 수 있으니 주의하세요.

또한 컴패니언 객체(companion object)는 다음 세 가지 유틸리티 메서드를 제공합니다.

- `valueOf(name: String)` — 이름(name)을 기준으로 열거형 값을 가져옵니다.
- `values` — 모든 열거형 값을 배열(Array)로 반환합니다.
- `fromOrdinal(n: Int)` — 서수(ordinal)를 기준으로 열거형 값을 가져옵니다.

> 💡 **왜 필요한가 — 이 세 메서드는 공짜로 따라옵니다**
>
> enum을 선언하기만 하면 `values`, `valueOf`, `fromOrdinal`이 컴패니언 객체(00번 5번)에
> 자동으로 생깁니다. 직접 만들 필요가 없습니다.
> - `values` : 모든 케이스를 한 번에 순회할 때 (예: 모든 색을 화면에 그리기)
> - `valueOf("Blue")` : 문자열을 enum 값으로 되돌릴 때 (예: 설정 파일에서 읽은 문자열 해석)
>
> Java enum이 제공하던 `values()`/`valueOf()`와 같은 편의 기능을 Scala도 그대로 제공하는 셈입니다.

```scala
val blue = Color.valueOf("Blue")
val values = Color.values
val red = Color.fromOrdinal(0)
```

---

### 사용자 정의 멤버

열거형(enum) 본문에 사용자 정의 멤버(custom definitions)를 추가할 수 있습니다. 다음은 행성을 표현하는 열거형에 중력 및 무게 계산 메서드를 추가한 예시입니다.

```scala
enum Planet(mass: Double, radius: Double):
  private final val G = 6.67300E-11
  def surfaceGravity = G * mass / (radius * radius)
  def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity

  case Mercury extends Planet(3.303e+23, 2.4397e6)
  case Venus   extends Planet(4.869e+24, 6.0518e6)
  case Earth   extends Planet(5.976e+24, 6.37814e6)
  case Mars    extends Planet(6.421e+23, 3.3972e6)
  case Jupiter extends Planet(1.9e+27,   7.1492e7)
  case Saturn  extends Planet(5.688e+26, 6.0268e7)
  case Uranus  extends Planet(8.686e+25, 2.5559e7)
  case Neptune extends Planet(1.024e+26, 2.4746e7)
end Planet
```

여기서 `surfaceGravity`(표면 중력)와 `surfaceWeight`(표면 무게)는 모든 열거형 값이 공유하는 메서드이며, 각 케이스(case)는 자신의 질량(mass)과 반지름(radius) 값을 생성자 인자로 전달합니다.

> 📘 **처음 배우는 분께 — enum 안에 메서드를 둘 수 있습니다**
>
> Java enum과 마찬가지로, Scala enum도 본문에 메서드를 정의할 수 있고 그 메서드는 모든 케이스가
> 공유합니다. `Planet.Earth.surfaceWeight(70)`처럼 어떤 행성이든 같은 계산을 쓸 수 있는 식입니다.
> 각 케이스가 넘긴 생성자 인자(질량·반지름)가 메서드 계산에 그대로 쓰입니다.
> 끝의 `end Planet`은 "여기서 Planet 정의가 끝난다"는 선택적 표시로, 긴 블록의 가독성을 위한 것입니다.

---

### 사용자 정의 컴패니언 객체

열거형(enum)에 대해 명시적인 컴패니언 객체(explicit companion object)를 정의할 수 있습니다. 다음은 앞선 `Planet` 열거형에 `main` 메서드를 포함한 컴패니언 객체를 추가한 예시입니다.

```scala
enum Planet(mass: Double, radius: Double):
  private final val G = 6.67300E-11
  def surfaceGravity = G * mass / (radius * radius)
  def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity

  case Mercury extends Planet(3.303e+23, 2.4397e6)
  case Venus   extends Planet(4.869e+24, 6.0518e6)
  case Earth   extends Planet(5.976e+24, 6.37814e6)
  case Mars    extends Planet(6.421e+23, 3.3972e6)
  case Jupiter extends Planet(1.9e+27,   7.1492e7)
  case Saturn  extends Planet(5.688e+26, 6.0268e7)
  case Uranus  extends Planet(8.686e+25, 2.5559e7)
  case Neptune extends Planet(1.024e+26, 2.4746e7)
end Planet

object Planet:
  def main(args: Array[String]) =
    val earthWeight = args(0).toDouble
    val mass = earthWeight / Earth.surfaceGravity
    for p <- values do
      println(s"Your weight on $p is ${p.surfaceWeight(mass)}")
end Planet
```

이 예시는 지구상의 무게를 입력받아 각 행성에서의 무게를 계산해 출력합니다. 컴패니언 객체 안에서도 `values`로 모든 열거형 값을 순회할 수 있습니다.

> 📘 **처음 배우는 분께 — "컴패니언 객체를 직접 정의한다"는 말**
>
> 앞서 enum이 컴패니언 객체를 자동으로 만들어 준다고 했는데(00번 5번), 여기서는 그 객체에
> 내가 원하는 멤버(`main` 메서드 등)를 **추가로** 넣는 것입니다. 같은 이름 `Planet`으로 `object`를
> 직접 쓰면, 컴파일러가 자동 생성하는 부분과 합쳐집니다. 그래서 `object Planet` 안에서도
> 자동 생성된 `values`를 그대로 쓸 수 있습니다.

---

### 열거형 케이스에 대한 제약

> ⚠️ **짚고 넘어가기 — "케이스는 안에 적지만 사실은 바깥에 산다"**
>
> 이 절은 헷갈리기 쉬운 규칙을 다룹니다. `case Mercury ...`를 enum 본문 *안*에 적긴 하지만,
> 컴파일러는 이를 실제로는 **컴패니언 객체 쪽(바깥)**에 두는 것처럼 취급합니다.
> 그래서 케이스는 enum 본문 안의 `private` 변수(`mercuryMass` 등)를 그냥 이름만으로는 못 봅니다.
> "안에 적혀 있으니 안의 것을 다 쓸 수 있겠지"라는 직관이 여기서는 통하지 않는다는 점만 기억하세요.
> 해결책은 아래 예시처럼 `Planet.earthMass`로 **풀네임을 적어 주는 것**입니다.

열거형 케이스 선언(enum case declaration)은 보조 생성자(secondary constructor)와 유사합니다. 즉, 열거형 템플릿(template) 내부에 선언되어 있지만, 실제 스코프(scope)는 열거형 템플릿의 바깥에 위치합니다. 이는 열거형 케이스가 내부 멤버(inner members)에 접근하거나 컴패니언 객체의 멤버를 직접 참조할 수 없음을 의미합니다.

```scala
import Planet.*
enum Planet(mass: Double, radius: Double):
  private final val (mercuryMass, mercuryRadius) = (3.303e+23, 2.4397e6)

  case Mercury extends Planet(mercuryMass, mercuryRadius)             // 찾을 수 없음(Not found)
  case Venus   extends Planet(venusMass, venusRadius)                 // 잘못된 참조(illegal reference)
  case Earth   extends Planet(Planet.earthMass, Planet.earthRadius)   // 정상(ok)
object Planet:
  private final val (venusMass, venusRadius) = (4.869e+24, 6.0518e6)
  private final val (earthMass, earthRadius) = (5.976e+24, 6.37814e6)
end Planet
```

위 예시에서:

- `Mercury`는 열거형 본문 안에 선언된 `mercuryMass`, `mercuryRadius`를 단순 식별자(simple identifier)로 참조하려 하므로 "찾을 수 없음(Not found)" 오류가 발생합니다.
- `Venus`는 컴패니언 객체의 `venusMass`, `venusRadius`를 단순 식별자로 참조하려 하므로 "잘못된 참조(illegal reference)" 오류가 발생합니다.
- `Earth`는 `Planet.earthMass`, `Planet.earthRadius`처럼 컴패니언 객체를 명시적으로 한정(qualify)하여 참조하므로 정상적으로 동작합니다.

개별 열거형 케이스에 대해 컴패니언 객체를 정의할 수는 없습니다. 열거형 케이스(enum case)는 접근 제어자(access modifier)만 허용하며, 열거형 클래스(enum class)는 접근 제어자와 `into` 또는 `infix`만 허용합니다.

---

### 열거형 케이스의 사용 중단(Deprecation)

`scala.deprecated` 애너테이션을 사용하여 특정 케이스를 사용 중단(deprecated)으로 표시할 수 있습니다.

```scala
enum Planet(mass: Double, radius: Double):
  ...
  case Neptune extends Planet(1.024e+26, 2.4746e7)

  @deprecated("refer to IAU definition of planet")
  case Pluto extends Planet(1.309e+22, 1.1883e3)
end Planet
```

열거형의 어휘적 스코프(lexical scope) 내에서는 사용 중단된 케이스라도 인트로스펙션(introspection) 목적으로 접근할 수 있습니다.

```scala
trait Deprecations[T <: reflect.Enum]:
  extension (t: T) def isDeprecatedCase: Boolean

object Planet:
  given Deprecations[Planet]:
    extension (p: Planet)
      def isDeprecatedCase = p == Pluto
```

이처럼 열거형 내부(또는 컴패니언 객체)에서는 사용 중단 경고 없이 `Pluto`를 참조해, 어떤 케이스가 사용 중단되었는지 판별하는 확장 메서드를 정의할 수 있습니다.

---

### 자바 열거형과의 호환성

> 💡 **왜 필요한가 — Scala enum과 Java enum은 기본적으로 다른 것**
>
> 헷갈리기 쉬운 부분입니다. Scala 3의 `enum`은 Java의 `enum`을 그대로 가져온 게 아니라,
> 더 일반적인 도구(ADT까지 표현)입니다. 그래서 기본 Scala enum은 `java.lang.Enum`을 상속하지
> **않습니다**. Java 코드와 섞어 쓰면서 `compareTo`처럼 Java enum 메서드가 필요할 때만,
> 아래처럼 `extends Enum[...]`을 명시해 "이건 Java식 enum이기도 하다"고 알려 주는 것입니다.

Scala 열거형은 자바 상호운용성(Java interoperability)을 위해 `java.lang.Enum`을 확장할 수 있습니다.

```scala
enum Color extends Enum[Color]:
  case Red, Green, Blue
```

이때 타입 매개변수(type parameter)는 반드시 열거형 자신의 타입과 일치해야 합니다. 생성자 인자(constructor arguments)는 컴파일러가 자동으로 생성합니다.

```scala
enum Color extends Enum[Color]:
  case Red, Green, Blue

val cmp = Color.Red.compareTo(Color.Green)
assert(cmp == -1)
```

`java.lang.Enum`을 확장함으로써 `compareTo`와 같은 자바 열거형의 표준 메서드를 사용할 수 있습니다. 위 예시에서 `Red`(서수 0)와 `Green`(서수 1)을 비교하면 -1이 반환됩니다.

---

### 구현 세부 사항

열거형(enum)은 `scala.reflect.Enum` 트레이트(trait)를 확장하는 봉인된(sealed) 클래스로 표현됩니다.

```scala
package scala.reflect

transparent trait Enum extends Any, Product, Serializable:
  def ordinal: Int
```

`extends` 절이 있는 열거형 값은 익명 클래스(anonymous class) 인스턴스로 확장됩니다.

```scala
val Venus: Planet = new Planet(4.869E24, 6051800.0):
  def ordinal: Int = 1
  override def productPrefix: String = "Venus"
  override def toString: String = "Venus"
```

반면 `extends` 절이 없는 값은 태그(tag)와 이름(name)을 인자로 받는 비공개(private) 메서드를 사용합니다.

```scala
val Red: Color = $new(0, "Red")
```

여기서 `$new`는 컴파일러가 생성하는 비공개 헬퍼 메서드로, 단순 케이스(simple case)의 인스턴스를 만들어 냅니다.

---

## 대수적 데이터 타입(Algebraic Data Types, ADT)

Scala 3의 열거형(enum) 구문은 대수적 데이터 타입(Algebraic Data Types, ADT)과 일반화 대수적 데이터 타입(Generalized Algebraic Data Types, GADT)을 지원합니다.

> 📘 **처음 배우는 분께 — 여기서부터는 "값이 데이터를 담는" enum입니다**
>
> 앞 절의 `Color.Red`는 데이터 없는 단순한 이름표였습니다. 이제부터는 케이스가 **값을 품습니다**.
> 대표 예가 `Option`입니다 — "값이 있다(`Some(x)`)" 또는 "값이 없다(`None`)" 두 경우뿐인 타입이죠.
> 이렇게 "경우의 수 + 각 경우가 담는 데이터"를 한 타입으로 묶은 것이 ADT이고(00번 8번),
> enum 문법으로 아주 짧게 쓸 수 있습니다. 사실 이게 enum의 가장 강력한 쓰임새입니다.
>
> GADT는 일단 "ADT의 더 정밀한 버전"이라고만 알아 두세요. 자세한 직관은 변성 절에서 다시 짚습니다.

### ADT의 기본 형태

다음은 `Option` 타입을 표현하는 기본적인 예시입니다.

```scala
enum Option[+T]:
  case Some(x: T)
  case None
```

이 코드는 두 케이스를 가지는 `Option` 열거형을 생성합니다. `Some`은 매개변수가 있는(parameterized) 케이스이고, `None`은 매개변수가 없는(unparameterized) 케이스입니다. `None`은 일반 열거형 값(normal enum value)으로 취급됩니다.

---

### 명시적 타입 선언

부모 타입(parent type)을 명시적으로 지정할 수도 있습니다.

```scala
enum Option[+T]:
  case Some(x: T) extends Option[T]
  case None       extends Option[Nothing]
```

컴파일러는 공변(covariant) 타입 매개변수는 최소화(minimize)하고 반공변(contravariant) 타입 매개변수는 최대화(maximize)하는 방식으로 `None`의 타입을 `Option[Nothing]`으로 자동 추론합니다. 따라서 위처럼 명시하지 않아도 동일한 결과를 얻을 수 있습니다.

> 📘 **처음 배우는 분께 — `+T`와 `Nothing`이 왜 나오나**
>
> `Option[+T]`의 `+`는 **공변(covariant)**을 뜻합니다(00번 6번). 쉽게 말해
> `Dog <: Animal`이면 `Option[Dog] <: Option[Animal]`도 성립한다는 약속입니다.
>
> `Nothing`은 "모든 타입의 가장 밑바닥 자식"인 특수 타입입니다. 어떤 `T`에 대해서도
> `Nothing <: T`라서, `Option[Nothing]`은 모든 `Option[T]`의 하위 타입이 됩니다.
> 그래서 값이 없는 `None`을 `Option[Nothing]`으로 두면, 어느 자리에 와도 자연스럽게 들어맞습니다.
> "공변은 최소화" = "값을 안 담는 None은 가장 작은 타입(Nothing)으로 맞춘다"는 뜻으로 읽으면 됩니다.

---

### 열거형 케이스에 접근하기

열거형 케이스는 컴패니언 객체(companion object) 안에 위치하며, `Option.Some`이나 `Option.None`처럼 접근합니다. 기본적으로 타입 확장(type widening)이 적용되어, 더 구체적인 타입이 요구되지 않는 한 표현식의 타입은 부모 열거형 타입(parent enum type)으로 결정됩니다.

> 📘 **처음 배우는 분께 — "타입 확장(type widening)"이 무슨 뜻인가**
>
> `Option.Some("hello")`를 만들면, 그 값의 타입은 좁은 `Some[String]`이 아니라 넓은
> `Option[String]`으로 잡힙니다. 이렇게 컴파일러가 일부러 **더 넓은(부모) 타입으로 넓혀 잡는 것**이
> 타입 확장입니다.
>
> 왜 그럴까요? 보통 우리가 원하는 건 "Option이다(있을 수도 없을 수도)"이지, "반드시 Some이다"가
> 아니기 때문입니다. 만약 정말로 좁은 `Some[Int]` 타입이 필요하면 아래처럼 `new`를 쓰거나 타입을
> 직접 적어 주면 됩니다.

```scala
val option = Option.Some("hello")  // 타입: Option[String]
val none = Option.None              // 타입: Option[Nothing]
```

구체적인 케이스 타입(specific case type)을 얻으려면 `new`를 사용하거나 명시적인 타입 애너테이션(type annotation)을 지정해야 합니다.

```scala
val some0 = new Option.Some(2)           // 타입: Option.Some[Int]
val some1: Option.Some[Int] = Option.Some(3)  // 타입: Option.Some[Int]
```

---

### 메서드와 컴패니언 객체

ADT에는 메서드(method)를 정의할 수 있습니다. 다음은 `Option`을 좀 더 완성된 형태로 구현한 예시로, 인스턴스 메서드 `isDefined`와 컴패니언 객체의 팩토리 메서드 `apply`를 포함합니다.

```scala
enum Option[+T]:
  case Some(x: T)
  case None

  def isDefined: Boolean = this match
    case None => false
    case _    => true

object Option:
  def apply[T >: Null](x: T): Option[T] =
    if x == null then None else Some(x)

end Option
```

`isDefined`는 패턴 매칭(pattern matching)을 통해 현재 인스턴스가 `None`인지 판별하며, 컴패니언 객체의 `apply`는 `null` 여부에 따라 `None` 또는 `Some`을 반환하는 팩토리(factory) 역할을 합니다.

> 💡 **왜 필요한가 — ADT + 패턴 매칭이 한 세트인 이유**
>
> `this match case None => ... case _ => ...`가 바로 ADT의 진가입니다(00번 3번).
> 경우의 수가 닫혀 있으니, `match`로 모든 경우를 안전하게 갈래 칠 수 있습니다.
> 만약 새 케이스를 추가하고 어딘가의 `match`에서 빠뜨리면 컴파일러가 경고해 줍니다 —
> "값을 담는 enum(ADT)"과 "패턴 매칭"은 늘 함께 쓰이는 한 쌍이라고 생각하면 됩니다.

---

### 혼합 열거형(Hybrid Enumerations)

열거형(enum)은 단순 값(simple values)과 매개변수를 가지는 케이스(parameterized cases)를 함께 조합할 수 있습니다.

```scala
enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)
  case Mix(mix: Int) extends Color(mix)
```

위 예시에서 `Red`, `Green`, `Blue`는 고정된 RGB 값을 가지는 단순 값이고, `Mix`는 임의의 정수 `mix`를 인자로 받아 그 값을 RGB로 사용하는 매개변수화된 케이스입니다.

---

### 매개변수 변성(Variance)에 대한 고려

> 📘 **처음 배우는 분께 — "공변 위치/반공변 위치"가 뭔가요 (어려우면 건너뛰어도 됨)**
>
> 변성(variance)에는 한 가지 추가 규칙이 있습니다(00번 6번). 타입 매개변수 `T`를 `-T`(반공변)로
> 선언했다면, `T`는 함수의 **입력(매개변수) 자리**에만 와야 하고 **출력(반환) 자리**에는 오면 안 됩니다.
> (`+T`는 그 반대.)
>
> 비유하면, `-T`는 "받기만 하는(소비하는) 자리"용입니다. 그런데 `f: T => T`에서 두 번째 `T`는
> 함수가 **돌려주는** 자리(공변 위치)라서, 받기 전용으로 선언한 `T`를 거기 쓰면 규칙 위반입니다.
> 아래 예시의 오류가 바로 이것입니다. 이 규칙이 아직 와닿지 않으면 지금은 넘어가도 enum 사용에는
> 지장이 없습니다.

매개변수가 있는 케이스(parameterized cases)는 부모 열거형으로부터 타입 매개변수(type parameter)와 변성(variance)을 상속받습니다. 반공변(contravariant) 매개변수는 공변 위치(covariant position) 위반이 발생하지 않도록 주의해야 합니다.

**문제가 되는 예시:**

```scala
enum View[-T]:
  case Refl(f: T => T)  // 오류: 반공변 T가 공변 위치에 사용됨
```

위 코드에서 `T`는 반공변으로 선언되었지만, `f: T => T`에서 함수 반환 타입 위치(공변 위치)에 `T`가 등장하므로 변성 검사(variance check)를 통과하지 못합니다.

**수정 방법 — 명시적 타입 매개변수 사용:**

```scala
enum View[-T]:
  case Refl[R](f: R => R) extends View[R]
```

케이스에 독자적인 타입 매개변수 `R`을 도입하면 열거형의 반공변 `T`와 무관하게 함수 타입을 안전하게 표현할 수 있습니다.

**완전한 구현 예시:**

```scala
enum View[-T, +U] extends (T => U):
  case Refl[R](f: R => R) extends View[R, R]

  final def apply(t: T): U = this match
    case refl: Refl[r] => refl.f(t)
```

이 예시에서 `View[-T, +U]`는 함수 타입 `T => U`를 확장하며, `Refl` 케이스는 항등(identity) 형태의 변환을 나타냅니다. `apply` 메서드는 패턴 매칭으로 `Refl`의 함수 `f`를 적용합니다. `case refl: Refl[r]`처럼 소문자 타입 변수 `r`을 사용하여 GADT 스타일의 타입 정련(type refinement)이 이루어집니다.

> 📘 **처음 배우는 분께 — GADT는 "케이스마다 타입이 더 좁혀지는 ADT"**
>
> GADT(일반화 ADT)를 한 문장으로 잡자면: **"패턴 매칭으로 어떤 케이스인지 알아내는 순간,
> 그 자리에서 타입이 더 구체적으로 좁혀지는 ADT"**입니다.
>
> 보통의 ADT는 모든 케이스가 같은 부모 타입을 그대로 씁니다. GADT에서는 케이스마다
> 타입 매개변수를 **자기 사정에 맞게 더 좁게** 고정합니다(여기서는 `Refl[R] extends View[R, R]` —
> 입력과 출력 타입이 같음을 박아 둠). 그래서 `case refl: Refl[r] =>` 안으로 들어가면 컴파일러가
> "여기서는 T와 U가 사실 같은 타입(r)이구나"를 알아내고, 그 정보를 이용해 코드가 타입 검사를
> 통과하게 됩니다. 이 "안으로 들어가니 타입이 더 똑똑해지는 것"이 타입 정련(type refinement)입니다.
>
> GADT는 고급 주제입니다. "이런 게 있다"는 감만 잡고, 당장 enum/ADT를 쓰는 데는 몰라도 됩니다.

---

### 문법 요약

열거형 정의(enum definition)는 다음과 같은 구조를 따릅니다.

- 선택적인 타입 매개변수(type parameters)를 가지는 열거형 클래스 선언(enum class declaration)
- `case` 키워드로 정의되는 케이스 — 매개변수를 가지는 생성자(parameterized constructor) 또는 단순 식별자(simple identifier) 형태
- 부모 타입(parent type)을 지정하는 선택적인 명시적 `extends` 절
- 열거형 본문(enum body) 내부의 메서드 정의(method definitions)

---

## 열거형과 ADT의 변환(Desugaring)

Scala 컴파일러는 열거형(enum)과 대수적 데이터 타입(ADT)을 핵심 언어 기능(core language features)만으로 이루어진 코드로 확장(expand)합니다. 즉, 열거형은 필수 구문 구조라기보다는 "문법 설탕(syntactic sugar)"에 해당하며, 이 변환(디슈가링, desugaring) 과정을 거쳐 일반적인 클래스/객체 정의로 풀어집니다.

> 📘 **처음 배우는 분께 — "디슈가링(desugaring)"이란?**
>
> "문법 설탕(syntactic sugar)"은 똑같은 일을 더 달콤하게(짧고 읽기 좋게) 쓰게 해 주는 문법을
> 말합니다. enum이 바로 그 설탕입니다 — 길게 쓰면 `sealed abstract class` + `object`로 써야 할
> 것을 짧게 줄여 주니까요.
>
> **디슈가링**은 그 설탕을 다시 녹여, 컴파일러가 실제로 다루는 기본 코드로 풀어내는 과정입니다
> (00번 11번). 이 절은 "내가 쓴 enum 한 줄이 사실은 어떤 클래스/객체 코드와 같은가"를 규칙으로
> 보여 줍니다. 맨 앞 📘에서 본 "enum = sealed trait + case class"라는 큰 그림을, 컴파일러가 실제로
> 어떻게 펼치는지 확인하는 부분이라고 보면 됩니다. 세부 규칙은 참고용이니 가볍게 훑어도 됩니다.

### 핵심 용어

- **E**: 열거형(enum)의 이름
- **C**: 열거형 내부 케이스(case)의 이름
- **<...>**: 선택적인 구문 구조(optional syntactic constructs)
- **클래스 케이스(Class cases)**: 타입 매개변수 또는 값 매개변수를 가지는 매개변수화된 케이스
- **단순 케이스(Simple cases)**: 이름만 가지는 비제네릭(non-generic) 열거형 케이스
- **값 케이스(Value cases)**: 매개변수는 없지만 `extends` 절 또는 본문(body)을 가지는 케이스
- **싱글턴 케이스(Singleton cases)**: 단순 케이스와 값 케이스를 합쳐서 부르는 용어

> 📘 **처음 배우는 분께 — "싱글턴 케이스"는 인스턴스가 딱 하나인 케이스**
>
> 용어가 많아 헷갈리니 정리하면, 케이스는 크게 두 부류입니다.
> - **데이터를 담는 케이스**(클래스 케이스): `Some(x: T)`처럼 인자를 받아 만들 때마다 다른 값이 됩니다.
> - **인스턴스가 하나뿐인 케이스**(싱글턴 케이스): `Red`, `None`처럼 인자가 없어 값이 하나로 고정됩니다.
>
> "싱글턴(singleton)"은 "전 세계에 인스턴스가 딱 하나"라는 뜻입니다(00번 1번의 `object`와 같은 결).
> `Color.Red`는 항상 같은 그 하나의 Red이므로, 매번 새로 만들 필요 없이 `val`로 한 번만 만들어
> 재사용합니다. 그래서 이런 케이스들에만 `values`/`valueOf` 같은 메서드가 따라붙는 것입니다.

---

### 변환 규칙(1~9)

**규칙 1 — 열거형 정의의 확장**

다음과 같은 열거형 정의:

```scala
enum E ... { <defs> <cases> }
```

는 아래와 같이 확장됩니다.

```scala
sealed abstract class E ... extends <parents> with scala.reflect.Enum {
  import E.{ <caseIds> }
  <defs>
}
object E { <cases> }
```

열거형 본문의 일반 정의(`<defs>`)는 봉인된 추상 클래스(sealed abstract class)로, 케이스(`<cases>`)는 컴패니언 객체로 각각 분리됩니다.

---

**규칙 2 — 콤마로 구분된 단순 케이스**

한 줄에 작성된 여러 케이스:

```scala
case C_1, ..., C_n
```

는 각각 개별적으로 확장됩니다.

```scala
case C_1; ...; case C_n
```

---

**규칙 3 — 단순 케이스 (비제네릭 열거형)**

```scala
case C
```

는 다음과 같이 확장됩니다.

```scala
val C = $new(n, "C")
```

여기서 `n`은 해당 케이스의 서수(ordinal)이고, `$new`는 단순 케이스 값을 생성하는 컴파일러 생성 메서드입니다.

---

**규칙 4 — 단순 케이스 (제네릭 열거형)**

타입 매개변수에 변성(variance)이 지정된 열거형 `E`에서 단순 케이스는 변성에 따라 적절한 타입 경계(type bounds)가 적용된 형태로 확장됩니다.

---

**규칙 5 — `extends` 없는 클래스 케이스 (비제네릭 열거형)**

```scala
case C <type-params> <value-params>
```

는 다음과 같이 됩니다.

```scala
case C <type-params> <value-params> extends E
```

즉, `extends` 절이 생략된 경우 부모로 열거형 `E`가 자동으로 추가됩니다.

---

**규칙 6 — 클래스 케이스 (제네릭 열거형, 타입 매개변수 없음)**

```scala
case C <value-params>
```

는 다음과 같이 확장됩니다.

```scala
case C[Ts] <value-params> extends E[Ts]
```

여기서 `Ts`는 열거형의 타입 매개변수를 의미하며, 케이스가 이를 상속받습니다.

---

**규칙 7 — 클래스 케이스 (제네릭 열거형, `extends` 있음)**

타입 매개변수는 없지만 `extends` 절을 가지는 케이스의 경우, 케이스의 매개변수 타입이나 부모 타입의 타입 인자(type arguments)에서 열거형의 타입 매개변수가 등장하면, 해당 타입 매개변수를 케이스가 획득(gain)하게 됩니다.

---

**규칙 8 — 값 케이스의 확장**

```scala
case C extends <parents>
```

는 다음과 같이 됩니다.

```scala
val C = new <parents> { <body>; def ordinal = n }
```

여기서 `n`은 0부터 시작하는 서수(ordinal) 위치입니다.

---

**규칙 9 — 클래스 케이스의 확장**

```scala
case C <params> extends <parents>
```

는 다음과 같이 확장됩니다.

```scala
final case class C <params> extends <parents>
```

이때 컴파일러가 생성한 `ordinal` 메서드가 함께 추가됩니다. 또한 `apply`와 `copy` 메서드 호출에는 특별한 처리가 적용되어 부모 타입들의 하부 교차 타입(underlying intersection types)으로 타입이 부여(ascribe)됩니다.

---

### 싱글턴 케이스의 변환

싱글턴 케이스(singleton cases)가 있는 열거형은 다음과 같은 합성 멤버(synthetic members)를 생성합니다.

- `valueOf(name: String): E'` — 식별자(identifier)를 기준으로 싱글턴을 반환합니다.
- `values: Array[E']` — 모든 싱글턴 케이스를 정의된 순서대로 반환합니다.
- `$new` 비공개 메서드 (단순 케이스가 존재하는 경우) — 새로운 단순 케이스 값을 생성합니다.

`$new` 메서드의 시그니처는 다음과 같습니다.

```scala
private def $new(_$ordinal: Int, $name: String) =
  new E with runtime.EnumValue:
    def ordinal = _$ordinal
    override def productPrefix = $name
    override def toString = $name
```

---

### 스코프 규칙

열거형 케이스(enum cases)는 보조 생성자(secondary constructor)처럼 취급됩니다. 따라서 다음 항목에는 접근할 수 없습니다.

- `this`를 사용하여 자신을 감싸는 열거형(enclosing enum)에 접근하는 것
- 단순 식별자(simple identifier)를 통해 열거형의 값 매개변수(value parameters)나 인스턴스 멤버(instance members)에 접근하는 것
- `this` 또는 단순 식별자를 통해 컴패니언 객체(companion object)에 접근하는 것

---

### 자바 호환 열거형

`java.lang.Enum`을 확장하는 열거형은 표준 변환 규칙을 따르되 다음과 같은 제약이 적용됩니다.

- 클래스 케이스(class cases)는 금지됩니다.
- 단순 케이스는 정적 필드(static field) 표현을 위해 `val` 대신 `@static val`로 확장됩니다.
- `ordinal` 메서드는 생성되지 않습니다(`java.lang.Enum`으로부터 상속받습니다).
- `toString`은 재정의(override)되지 않습니다(`java.lang.Enum`의 `name`을 사용합니다).
- `productPrefix`는 `this.name`을 호출합니다.

---

### 그 외 규칙

- 열거형이 아닌 일반 케이스 클래스(non-enum case classes)는 `scala.reflect.Enum`을 확장할 수 없습니다.
- `extends` 절이 있는 열거형 케이스는 반드시 부모 중 하나로 열거형 클래스(enum class) 자신을 포함해야 합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Enumerations](https://docs.scala-lang.org/scala3/reference/enums/)
