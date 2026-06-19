# Scala 3 열거형과 대수적 데이터 타입

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Enums" 섹션을 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/enums/

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

이 문서는 위 세 페이지의 내용을 모두 한국어로 번역하여 통합한 것입니다.

---

## 열거형(Enumerations)

### 기본 열거형

열거형(enum)은 이름이 부여된 값(named values)들의 집합으로 구성되는 타입을 정의하는 데 사용됩니다.

```scala
enum Color:
  case Red, Green, Blue
```

이 코드는 봉인된(sealed) 클래스 `Color`를 생성하며, 컴패니언 객체(companion object)를 통해 `Color.Red`, `Color.Green`, `Color.Blue`라는 세 개의 값에 접근할 수 있게 합니다.

---

### 매개변수를 가지는 열거형

열거형(enum)은 매개변수(parameter)를 지원합니다.

```scala
enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)
```

매개변수는 명시적인 `extends` 절(explicit extends clause)을 통해 지정됩니다. 즉, 각 케이스(case)가 자신의 부모 열거형 클래스를 명시적으로 확장하면서 생성자 인자를 전달하는 형태가 됩니다.

---

### 열거형에 정의되는 메서드

각 열거형 값은 `ordinal` 메서드를 통해 접근할 수 있는 고유한 정수(unique integer)와 연관되어 있습니다.

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

또한 컴패니언 객체(companion object)는 다음 세 가지 유틸리티 메서드를 제공합니다.

- `valueOf(name: String)` — 이름(name)을 기준으로 열거형 값을 가져옵니다.
- `values` — 모든 열거형 값을 배열(Array)로 반환합니다.
- `fromOrdinal(n: Int)` — 서수(ordinal)를 기준으로 열거형 값을 가져옵니다.

```scala
val blue = Color.valueOf("Blue")
val values = Color.values
val red = Color.fromOrdinal(0)
```

---

### 사용자 정의 멤버

열거형(enum)에는 사용자가 직접 정의한 정의(custom definitions)를 추가할 수 있습니다. 다음은 행성을 표현하는 열거형에 중력 및 무게 계산 메서드를 추가한 예시입니다.

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

이 예시에서는 지구상의 무게를 입력받아 각 행성에서의 무게를 계산하여 출력합니다. 컴패니언 객체 안에서 `values`를 통해 모든 열거형 값을 순회할 수 있다는 점에 주목하십시오.

---

### 열거형 케이스에 대한 제약

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

또한 개별 열거형 케이스에 대해서는 컴패니언 객체를 작성할 수 없습니다. 열거형 케이스(enum case)는 접근 제어자(access modifier)만 받아들이며, 열거형 클래스(enum class)는 접근 제어자와 `into` 또는 `infix`만 받아들입니다.

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

열거형의 어휘적 스코프(lexical scope) 내부에서는, 사용 중단된 케이스라도 인트로스펙션(introspection, 자기 점검)을 위해 여전히 접근할 수 있습니다.

```scala
trait Deprecations[T <: reflect.Enum]:
  extension (t: T) def isDeprecatedCase: Boolean

object Planet:
  given Deprecations[Planet]:
    extension (p: Planet)
      def isDeprecatedCase = p == Pluto
```

이처럼 열거형 내부(또는 컴패니언 객체)에서는 사용 중단 경고 없이 `Pluto`를 참조하여, 어떤 케이스가 사용 중단되었는지 판별하는 확장 메서드를 정의할 수 있습니다.

---

### 자바 열거형과의 호환성

Scala 열거형은 자바 상호운용성(Java interoperability)을 위해 `java.lang.Enum`을 확장할 수 있습니다.

```scala
enum Color extends Enum[Color]:
  case Red, Green, Blue
```

이때 타입 매개변수(type parameter)는 반드시 열거형 자신의 타입과 일치해야 합니다. 생성자 인자(constructor arguments)는 컴파일러에 의해 자동으로 생성됩니다.

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

`extends` 절을 가지는 열거형 값은 익명 클래스(anonymous class) 인스턴스로 확장됩니다.

```scala
val Venus: Planet = new Planet(4.869E24, 6051800.0):
  def ordinal: Int = 1
  override def productPrefix: String = "Venus"
  override def toString: String = "Venus"
```

반면, `extends` 절이 없는 값은 태그(tag)와 이름(name)을 인자로 받는 비공개(private) 메서드를 사용합니다.

```scala
val Red: Color = $new(0, "Red")
```

여기서 `$new`는 컴파일러가 생성하는 비공개 헬퍼 메서드로, 단순 케이스(simple case)의 인스턴스를 만들어 냅니다.

---

## 대수적 데이터 타입(Algebraic Data Types, ADT)

Scala 3의 열거형(enum) 구문은 대수적 데이터 타입(Algebraic Data Types, ADT)과 일반화 대수적 데이터 타입(Generalized Algebraic Data Types, GADT)을 지원합니다.

### ADT의 기본 형태

다음은 `Option` 타입을 표현하는 기본적인 예시입니다.

```scala
enum Option[+T]:
  case Some(x: T)
  case None
```

이 코드는 두 개의 케이스를 가지는 `Option` 열거형을 생성합니다. `Some`은 매개변수가 있는(parameterized) 케이스이고, `None`은 매개변수가 없는(unparameterized) 케이스입니다. `None` 케이스는 일반적인 열거형 값(normal enum value)으로 취급됩니다.

---

### 명시적 타입 선언

부모 타입(parent type)을 명시적으로 지정할 수도 있습니다.

```scala
enum Option[+T]:
  case Some(x: T) extends Option[T]
  case None       extends Option[Nothing]
```

컴파일러는 공변(covariant) 타입 매개변수는 최소화(minimize)하고 반공변(contravariant) 타입 매개변수는 최대화(maximize)하는 방식으로, `None`에 대해 자동으로 `Option[Nothing]`을 추론합니다. 따라서 위처럼 명시하지 않아도 동일한 결과를 얻을 수 있습니다.

---

### 열거형 케이스에 접근하기

열거형 케이스는 컴패니언 객체(companion object) 안에 위치하며, `Option.Some`과 `Option.None`처럼 접근합니다. 기본적으로 타입 확장(type widening)이 적용되어, 표현식은 더 구체적인 타입이 요구되지 않는 한 일반적으로 부모 열거형 타입(parent enum type)으로 해석됩니다.

```scala
val option = Option.Some("hello")  // 타입: Option[String]
val none = Option.None              // 타입: Option[Nothing]
```

구체적인 케이스 타입(specific case type)을 얻으려면 `new`를 사용하거나 명시적인 타입 애너테이션(type annotation)을 제공해야 합니다.

```scala
val some0 = new Option.Some(2)           // 타입: Option.Some[Int]
val some1: Option.Some[Int] = Option.Some(3)  // 타입: Option.Some[Int]
```

---

### 메서드와 컴패니언 객체

ADT에는 메서드(method)를 정의할 수 있습니다. 다음은 `Option`을 좀 더 완성도 있게 구현한 예시로, 인스턴스 메서드 `isDefined`와 컴패니언 객체의 팩토리 메서드 `apply`를 포함합니다.

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

매개변수를 가지는 케이스(parameterized cases)는 부모 열거형으로부터 타입 매개변수(type parameter)와 변성(variance)을 상속받습니다. 반공변(contravariant) 매개변수는 공변 위치(covariant position) 위반을 피하기 위해 신중하게 다루어야 합니다.

**문제가 되는 예시:**

```scala
enum View[-T]:
  case Refl(f: T => T)  // 오류: 반공변 T가 공변 위치에 사용됨
```

위 코드에서 `T`는 반공변으로 선언되었지만, `f: T => T`에서 함수의 반환 타입 위치(공변 위치)에 `T`가 등장하기 때문에 변성 검사(variance check)에 실패합니다.

**수정 방법 — 명시적 타입 매개변수 사용:**

```scala
enum View[-T]:
  case Refl[R](f: R => R) extends View[R]
```

케이스에 독립적인 타입 매개변수 `R`을 도입하면, 열거형의 반공변 `T`와 무관하게 함수 타입을 안전하게 표현할 수 있습니다.

**완전한 구현 예시:**

```scala
enum View[-T, +U] extends (T => U):
  case Refl[R](f: R => R) extends View[R, R]

  final def apply(t: T): U = this match
    case refl: Refl[r] => refl.f(t)
```

이 예시에서 `View[-T, +U]`는 함수 타입 `T => U`를 확장하며, `Refl` 케이스는 항등(identity) 형태의 변환을 나타냅니다. `apply` 메서드는 패턴 매칭을 통해 `Refl`의 함수 `f`를 적용합니다. 여기서 `case refl: Refl[r]`처럼 소문자 타입 변수 `r`을 사용하여 GADT 스타일의 타입 정련(type refinement)이 이루어집니다.

---

### 문법 요약

열거형 정의(enum definition)는 다음과 같은 구조를 따릅니다.

- 선택적인 타입 매개변수(type parameters)를 가지는 열거형 클래스 선언(enum class declaration)
- `case` 키워드로 정의되는 케이스 — 매개변수를 가지는 생성자(parameterized constructor) 또는 단순 식별자(simple identifier) 형태
- 부모 타입(parent type)을 지정하는 선택적인 명시적 `extends` 절
- 열거형 본문(enum body) 내부의 메서드 정의(method definitions)

---

## 열거형과 ADT의 변환(Desugaring)

Scala 컴파일러는 열거형(enum)과 대수적 데이터 타입(ADT)을 핵심 언어 기능(core language features)만을 사용하는 코드로 확장(expand)합니다. 즉, 열거형은 필수 구문 구조라기보다는 "문법 설탕(syntactic sugar)"에 해당하며, 이 변환(디슈가링, desugaring) 과정을 통해 일반적인 클래스/객체 정의로 풀어집니다.

### 핵심 용어

- **E**: 열거형(enum)의 이름
- **C**: 열거형 내부 케이스(case)의 이름
- **<...>**: 선택적인 구문 구조(optional syntactic constructs)
- **클래스 케이스(Class cases)**: 타입 매개변수 또는 값 매개변수를 가지는 매개변수화된 케이스
- **단순 케이스(Simple cases)**: 이름만 가지는 비제네릭(non-generic) 열거형 케이스
- **값 케이스(Value cases)**: 매개변수는 없지만 `extends` 절 또는 본문(body)을 가지는 케이스
- **싱글턴 케이스(Singleton cases)**: 단순 케이스와 값 케이스를 합쳐서 부르는 용어

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

즉, 열거형 본문의 일반 정의(`<defs>`)는 봉인된 추상 클래스(sealed abstract class)로, 케이스(`<cases>`)는 컴패니언 객체로 분리됩니다.

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

타입 매개변수를 가지는 열거형 `E`에서 변성(variance)이 지정된 경우, 단순 케이스는 변성에 따라 적절한 타입 경계(type bounds)가 적용되어 확장됩니다.

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

타입 매개변수는 없지만 `extends` 절을 가지는 케이스의 경우, 그 케이스의 매개변수 타입(parameter types)이나 부모 타입의 타입 인자(type arguments)에서 열거형의 타입 매개변수가 언급되면, 해당 타입 매개변수를 케이스가 획득(gain)하게 됩니다.

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

이때 컴파일러가 생성한 `ordinal` 메서드가 함께 추가됩니다. 또한 `apply`와 `copy` 메서드 호출에 대해서는 특별한 처리가 적용되어, 부모 타입들의 하부 교차 타입(underlying intersection types)으로 타입이 부여(ascribe)됩니다.

---

### 싱글턴 케이스의 변환

싱글턴 케이스(singleton cases)를 가지는 열거형은 다음과 같은 합성 멤버(synthetic members)를 생성합니다.

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

열거형 케이스(enum cases)는 보조 생성자(secondary constructor)처럼 취급됩니다. 따라서 다음 항목들에는 접근할 수 없습니다.

- `this`를 사용하여 자신을 감싸는 열거형(enclosing enum)에 접근하는 것
- 단순 식별자(simple identifier)를 통해 열거형의 값 매개변수(value parameters)나 인스턴스 멤버(instance members)에 접근하는 것
- `this` 또는 단순 식별자를 통해 컴패니언 객체(companion object)에 접근하는 것

---

### 자바 호환 열거형

`java.lang.Enum`을 확장하는 열거형은 표준 규칙을 따르되 다음과 같은 제약이 적용됩니다.

- 클래스 케이스(class cases)는 금지됩니다.
- 단순 케이스는 정적 필드(static field) 표현을 위해 `val` 대신 `@static val`로 확장됩니다.
- `ordinal` 메서드는 생성되지 않습니다(`java.lang.Enum`으로부터 상속받습니다).
- `toString`은 재정의(override)되지 않습니다(`java.lang.Enum`의 `name`을 사용합니다).
- `productPrefix`는 `this.name`을 호출합니다.

---

### 그 외 규칙

- 열거형이 아닌 일반 케이스 클래스(non-enum case classes)는 `scala.reflect.Enum`을 확장할 수 없습니다.
- `extends` 절을 가지는 열거형 케이스는 반드시 그 부모 중 하나로 열거형 클래스(enum class) 자신을 포함해야 합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Enumerations](https://docs.scala-lang.org/scala3/reference/enums/)
