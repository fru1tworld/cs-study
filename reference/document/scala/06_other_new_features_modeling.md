# Scala 3 기타 새 기능: 클래스와 트레이트 모델링

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Other New Features" 섹션 일부(트레이트 매개변수, 불투명 타입, export 등)를 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/other-new-features/

---

## 목차

1. [트레이트 매개변수 (Trait Parameters)](#1-트레이트-매개변수-trait-parameters)
2. [트랜스패런트 트레이트 (Transparent Traits)](#2-트랜스패런트-트레이트-transparent-traits)
3. [open 클래스 (Open Classes)](#3-open-클래스-open-classes)
4. [불투명 타입 별칭 (Opaque Type Aliases)](#4-불투명-타입-별칭-opaque-type-aliases)
5. [불투명 타입 별칭: 더 자세히 (Opaque Type Aliases: more details)](#5-불투명-타입-별칭-더-자세히-opaque-type-aliases-more-details)
6. [export 절 (Export Clauses)](#6-export-절-export-clauses)
7. [범용 apply 메서드 / 생성자 앱 (Universal Apply Methods / Creator Applications)](#7-범용-apply-메서드--생성자-앱-universal-apply-methods--creator-applications)

---

## 1. 트레이트 매개변수 (Trait Parameters)

Scala 3는 클래스가 매개변수를 가질 수 있는 것처럼, **트레이트(trait)** 도 매개변수를 가질 수 있도록 허용합니다.

```scala
trait Greeting(val name: String):
  def msg = s"How are you, $name"

class C extends Greeting("Bob"):
  println(msg)
```

매개변수화된 트레이트(parameterized trait)의 인자(argument)는 트레이트가 초기화(initialization)되기 직전에 즉시(immediately) 평가(evaluate)됩니다.

이 기능의 어려운 점 중 하나는, 트레이트가 여러 번 인스턴스화되는 것을 막는 방법을 명시하는 것입니다. 예를 들어, 같은 매개변수화된 트레이트를 서로 다른 인자로 두 번 상속(extend)하려고 하면 모호함(ambiguity)이 생깁니다. 이를 막기 위한 규칙들이 필요합니다.

### 트레이트 매개변수 규칙

트레이트 매개변수의 사용을 규율하는 세 가지 규칙은 다음과 같습니다.

1. **매개변수화되지 않은 상위 클래스(superclass)를 가진 매개변수화된 트레이트를 상속하는 클래스**의 경우: 그 클래스는 반드시 트레이트에 인자를 전달해야 합니다.

2. **매개변수화된 상위 클래스를 가진 매개변수화된 트레이트를 상속하는 클래스**의 경우: 그 클래스는 트레이트에 인자를 전달해서는 안 됩니다(모호함을 방지하기 위함).

3. **트레이트가 부모 트레이트(parent trait)를 참조하는 경우**: 트레이트는 부모 트레이트에 인자를 전달할 수 없습니다.

### 모호함 방지 (Ambiguity Prevention)

매개변수화된 트레이트를 서로 다른 인자로 여러 번 상속하려고 시도하면 컴파일러 오류가 발생합니다.

```scala
class D extends C, Greeting("Bill") // error: parameter passed twice
```

위 예시에서 `D`는 `C`(이미 `Greeting("Bob")`을 통해 `Greeting`을 상속함)를 상속하면서, 동시에 `Greeting("Bill")`을 다시 상속하려 합니다. 그 결과 `Greeting`의 매개변수가 두 번 전달되어(parameter passed twice) 오류가 됩니다.

### 트레이트 상속 예시 (Trait Inheritance Example)

```scala
trait FormalGreeting extends Greeting:
  override def msg = s"How do you do, $name"

class E extends Greeting("Bob"), FormalGreeting
```

여기서 `E`는 `Greeting`과 `FormalGreeting`을 모두 명시적으로 상속해야 합니다. 그 이유는 `FormalGreeting` 자체만으로는 `Greeting`에 인자를 제공하지 않기 때문입니다(규칙 3에 따라 트레이트는 부모 트레이트에 인자를 전달할 수 없음). 따라서 `Greeting`의 매개변수를 채워주는 책임은 구체적인(concrete) 클래스인 `E`에게 있습니다.

### 컨텍스트 매개변수에 대한 예외 (Context Parameters Exception)

명시적으로 부모를 상속해야 한다는 요구사항은, 트레이트가 **오직 컨텍스트 매개변수(context parameter)** 만 가질 때 완화됩니다. 이 경우 컴파일러는 추론된(inferred) 인자와 함께 그 트레이트를 암묵적으로 삽입(implicitly insert)합니다.

```scala
case class ImpliedName(name: String):
  override def toString = name

trait ImpliedGreeting(using val iname: ImpliedName):
  def msg = s"How are you, $iname"

trait ImpliedFormalGreeting extends ImpliedGreeting:
  override def msg = s"How do you do, $iname"

class F(using iname: ImpliedName) extends ImpliedFormalGreeting
```

위 예시에서, 컴파일러는 `F`를 확장(expand)하여 추론된 인자와 함께 부모 트레이트 `ImpliedGreeting`을 암묵적으로 참조하도록 만듭니다. 즉, `ImpliedGreeting`은 `using` 컨텍스트 매개변수만 가지므로, 컴파일러가 `F`의 컨텍스트 범위(context scope)에서 `ImpliedName` 인스턴스를 자동으로 찾아 채워줍니다. 따라서 사용자가 `F`를 작성할 때 `ImpliedGreeting`을 명시적으로 다시 나열할 필요가 없습니다.

---

## 2. 트랜스패런트 트레이트 (Transparent Traits)

트랜스패런트 트레이트(transparent trait)와 클래스는, 특정 트레이트가 추론된 타입(inferred type)을 어지럽히는(clutter) 것을 방지하기 위해 설계된 Scala 3의 기능입니다. 이 기능을 통해 개발자는 독립적인 타입(standalone type)이라기보다는 주로 **믹스인(mixin)** 으로 쓰이는 트레이트를 표시(mark)할 수 있습니다.

### 해결하려는 문제

Scala 2에서는 컴파일러가 자동으로 추가하는 트레이트(예: `Product`)가, 바람직하지 않은 경우에도 추론된 타입에 그대로 나타나곤 했습니다. 다음 예시를 보겠습니다.

```scala
trait Kind
case object Var extends Kind
case object Val extends Kind
val x = Set(if condition then Val else Var)
```

위 코드에서 `x`는 더 단순한 `Set[Kind]`가 아니라, `Set[Kind & Product & Serializable]`로 추론됩니다. 즉 사용자가 신경 쓰지 않는 구현 세부사항(`Product`, `Serializable`)이 타입에 노출됩니다.

### 해결책: 트랜스패런트 선언

트레이트를 `transparent`로 표시하면, 추론된 타입에서 해당 트레이트가 억제(suppress)됩니다.

```scala
transparent trait S
trait Kind
object Var extends Kind, S
object Val extends Kind, S
val x = Set(if condition then Val else Var)
```

이제 `x`는 `Set[Kind]` 타입을 가집니다.

추가로, `Kind` 자체를 `transparent`로 선언하면, 합집합(union)을 넓히지(widening) 않고 `Set[Val | Var]`를 얻게 됩니다.

### 자동으로 트랜스패런트로 취급되는 타입

다음 타입들은 기본적으로 트랜스패런트로 취급됩니다.

- `scala.Any`
- `scala.AnyVal`
- `scala.Matchable`
- `scala.Product`
- `java.lang.Object`
- `java.lang.Comparable`
- `java.io.Serializable`

### 타입 추론 규칙

타입을 추론할 때 시스템은 다음과 같이 동작합니다.

1. **교집합(intersection)에서 가능하면 트랜스패런트 트레이트를 제거**합니다.
2. 결과가 오직 트랜스패런트 상위 타입(supertype)만 남게 되는 경우, **합집합을 넓히는 것을 피합니다.**
3. **단일 트랜스패런트 인스턴스는 보존**합니다(예: `Product` 하나만 있을 때는 `Any`로 넓히지 않고 그대로 둠).
4. 넓혔을 때 트랜스패런트가 아닌 타입이 모두 사라진다면, **원래의 합집합 타입을 유지**합니다.

이러한 규칙은 타입 변수(type variable), `val`, `def`의 반환 타입(return type) 추론에 적용됩니다(고차 종류, higher-kinded type은 제외). 이를 통해 구현 세부사항에 해당하는 트레이트를 억제하면서도 타입을 최대한 구체적으로 유지합니다.

---

## 3. open 클래스 (Open Classes)

`open` 수식어(modifier)는 어떤 클래스가 하위 클래스(subclass)에 의한 **상속(extension)을 위해 설계되었음**을 알립니다. 이 기능은 확장 가능성에 대한 세 가지 의도(intent) 수준을 구별합니다.

### 사용 예시

```scala
// File Writer.scala
package p

open class Writer[T]:

  /** Sends to stdout, can be overridden */
  def send(x: T) = println(x)

  /** Sends all arguments using `send` */
  def sendAll(xs: T*) = xs.foreach(send)
end Writer

// File EncryptedWriter.scala
package p

class EncryptedWriter[T: Encryptable] extends Writer[T]:
  override def send(x: T) = super.send(encrypt(x))
```

`Writer`는 `open`으로 표시되어 있으므로, 다른 파일에 있는 `EncryptedWriter`가 자유롭게 상속할 수 있습니다.

### 상속 규칙 (Extension Rules)

`open`이 아닌 클래스라도 두 가지 조건 하에서는 여전히 상속할 수 있습니다.

1. **같은 파일 내 상속(Same-file extensions)**: 상속하는 클래스가, 상속되는 클래스와 같은 소스 파일(source file) 안에 있는 경우.

2. **기능 게이트 상속(Feature-gated extensions)**: `adhocExtensions` 언어 기능(language feature)이 다음 중 하나의 방법으로 활성화된 경우.
   - 임포트: `import scala.language.adhocExtensions`
   - 컴파일러 플래그: `-language:adhocExtensions`

이 기능이 활성화되어 있지 않으면, 파일을 가로지르는(cross-file) 상속을 시도할 때 경고(warning)가 발생합니다.

### 설계 근거 (Design Rationale)

확장 가능성에 대한 세 가지 기대(expectation)가 존재합니다.

1. **의도된 상속(Intentional extensions)**: `open`으로 표시되며, 문서화된 상속 계약(extension contract)을 동반합니다.
2. **상속 없음(No extensions)**: `final`로 표시되며, 정확성(correctness)/보안(security) 보장을 제공합니다.
3. **임시 상속(Ad-hoc extensions)**: 수식어가 없으며, 허용되지만 지원되지 않고(unsupported) 깨지기 쉽습니다(fragile).

이 설계는 깨지기 쉬운 임시 상속을 권장하지 않으면서도, 테스트나 임시 패치(temporary patch)처럼 명시적으로 동의(opt-in)한 경우에는 그것을 허용합니다.

### 기술적 세부사항 (Technical Details)

- `open`은 **소프트 수식어(soft modifier)** 로 동작합니다(수식어 위치가 아닌 곳에서는 일반 식별자로 사용 가능).
- `open`을 `final`이나 `sealed`와 결합할 수 없습니다.
- 트레이트와 추상 클래스(abstract class)에는 `open`이 불필요합니다(이들은 항상 상속 가능하기 때문).

### 마이그레이션 노트 (Migration Note)

임시 상속에 대한 기능 경고(feature warning)는 현재 `-source future` 하에서만 활성화되며, Scala 3.4부터는 기본값(default)이 됩니다.

---

## 4. 불투명 타입 별칭 (Opaque Type Aliases)

불투명 타입 별칭(opaque type alias)은 **런타임 오버헤드 없이(without any overhead)** 타입 추상화(type abstraction)를 가능하게 합니다. 핵심적인 차이점은, 불투명 타입이 그것이 정의된 스코프(scope) 내부에서는 투명하게(transparent) 보이지만, 외부 코드에는 추상 타입(abstract type)처럼 보인다는 점입니다.

### 핵심 개념

불투명 타입 별칭은 `opaque type` 키워드로 선언하며, 기반(underlying) 구현 타입을 지정합니다. 그 구현 타입은 정의된 스코프 안에서만 보입니다. 외부 코드는 기반 타입에 직접 접근할 수 없습니다.

### 기본 예시: Logarithm 타입

공식 문서가 제공하는 예시는 다음과 같습니다.

```scala
object MyMath:

  opaque type Logarithm = Double

  object Logarithm:

    def apply(d: Double): Logarithm = math.log(d)

    def safe(d: Double): Option[Logarithm] =
      if d > 0.0 then Some(math.log(d)) else None

  end Logarithm

  extension (x: Logarithm)
    def toDouble: Double = math.exp(x)
    def + (y: Logarithm): Logarithm = Logarithm(math.exp(x) + math.exp(y))
    def * (y: Logarithm): Logarithm = x + y

end MyMath
```

공개 API(public API)는 동반 객체(companion object)의 메서드와 확장 메서드(extension method)를 통해 통제됩니다. 스코프 안에서 유효한 연산들은 다음과 같습니다.

```scala
import MyMath.Logarithm

val l = Logarithm(1.0)
val l2 = Logarithm(2.0)
val l3 = l * l2
val l4 = l + l2
```

반면, 추상화를 위반하는 연산은 타입 오류를 발생시킵니다.

```scala
val d: Double = l       // error: found: Logarithm, required: Double
val l2: Logarithm = 1.0 // error: found: Double, required: Logarithm
l * 2                   // error: found: Int(2), required: Logarithm
l / l2                  // error: `/` is not a member of Logarithm
```

`Logarithm`은 외부에서는 `Double`과 호환되지 않는 별개의 타입으로 취급되므로, 위와 같이 `Double`로 직접 대입하거나 정수를 곱하는 등의 연산이 금지됩니다.

### 불투명 타입 별칭의 경계 (Bounds for Opaque Type Aliases)

불투명 타입은 상한(upper bound)이나 하한(lower bound) 경계를 포함할 수 있습니다. 공식 문서는 권한(permission) 시스템 예시로 이를 보여줍니다.

```scala
object Access:

  opaque type Permissions = Int
  opaque type PermissionChoice = Int
  opaque type Permission <: Permissions & PermissionChoice = Int

  extension (x: PermissionChoice)
    def | (y: PermissionChoice): PermissionChoice = x | y
  extension (x: Permissions)
    def & (y: Permissions): Permissions = x | y
  extension (granted: Permissions)
    def is(required: Permissions) = (granted & required) == required
    def isOneOf(required: PermissionChoice) = (granted & required) != 0

  val NoPermission: Permission = 0
  val Read: Permission = 1
  val Write: Permission = 2
  val ReadWrite: Permissions = Read | Write
  val ReadOrWrite: PermissionChoice = Read | Write

end Access
```

경계 `Permission <: Permissions & PermissionChoice`는, 객체 외부에서 `Permission`이 다른 두 타입(`Permissions`, `PermissionChoice`)의 하위 타입(subtype)이 되도록 만듭니다. 이를 통해 다음과 같은 대입이 가능해집니다.

```scala
object User:
  import Access.*

  case class Item(rights: Permissions)
  extension (item: Item)
    def +(other: Item): Item = Item(item.rights & other.rights)

  val roItem = Item(Read)  // OK, since Permission <: Permissions
  val woItem = Item(Write)
  val rwItem = Item(ReadWrite)
  val noItem = Item(NoPermission)

  assert(!roItem.rights.is(ReadWrite))
  assert(roItem.rights.isOneOf(ReadOrWrite))

  assert(rwItem.rights.is(ReadWrite))
  assert(rwItem.rights.isOneOf(ReadOrWrite))

  assert(!noItem.rights.is(ReadWrite))
  assert(!noItem.rights.isOneOf(ReadOrWrite))

  assert((roItem + woItem).rights.is(ReadWrite))
end User
```

`Read`, `Write` 등은 `Permission` 타입이지만, `Permission <: Permissions`이므로 `Permissions`를 요구하는 `Item(...)`에 대입될 수 있습니다.

그러나 서로 관련 없는(unrelated) 불투명 타입은 섞을 수 없습니다. 다음 코드는 오류를 발생시킵니다.

```scala
roItem.rights.isOneOf(ReadWrite) // error
```

`ReadWrite`는 `Permissions` 타입이고 `isOneOf`는 `PermissionChoice`를 요구하는데, `Access` 객체 외부에서는 `Permissions`와 `PermissionChoice`가 서로 관련 없는 별개의 타입이기 때문입니다.

### 클래스의 불투명 타입 멤버 (Opaque Type Members on Classes)

불투명 타입은 클래스에서도 동작합니다.

```scala
class Logarithms:

  opaque type Logarithm = Double

  def apply(d: Double): Logarithm = math.log(d)

  def safe(d: Double): Option[Logarithm] =
    if d > 0.0 then Some(math.log(d)) else None

  def mul(x: Logarithm, y: Logarithm) = x + y
```

중요한 점은, **서로 다른 클래스 인스턴스의 불투명 타입 멤버는 서로 구별되는(distinct) 별개의 타입으로 취급**된다는 것입니다.

```scala
val l1 = new Logarithms
val l2 = new Logarithms
val x = l1(1.5)
val y = l1(2.6)
val z = l2(3.1)
l1.mul(x, y) // type checks
l1.mul(x, z) // error: found l2.Logarithm, required l1.Logarithm
```

위 예시에서 `x`, `y`는 `l1.Logarithm` 타입이고 `z`는 `l2.Logarithm` 타입입니다. 이 둘은 다른 인스턴스에 속하므로 서로 호환되지 않아, `l1.mul(x, z)`는 타입 오류가 됩니다.

클래스 내 불투명 타입의 투명성 스코프(scope of transparency)는 `private[this]` 스코프와 동등합니다. 최상위(top-level) 불투명 타입의 경우, 투명성은 그것이 정의된 파일 안으로 제한됩니다.

---

## 5. 불투명 타입 별칭: 더 자세히 (Opaque Type Aliases: more details)

### 문법 (Syntax)

`opaque` 수식어는 다음과 같이 정의됩니다.

```
Modifier          ::=  ...
                    |  'opaque'
```

`opaque`는 **소프트 수식어(soft modifier)** 이며, 정의 키워드(definition keyword) 앞에 오지 않을 때는 일반 식별자로 사용할 수 있습니다.

불투명 타입 별칭은 위치에 제약이 있습니다. 불투명 타입 별칭은 반드시 클래스, 트레이트, 객체의 멤버이거나, 최상위(top-level)에 정의되어야 합니다. **지역 블록(local block)에서는 정의할 수 없습니다.**

### 타입 검사 (Type Checking)

단형(monomorphic) 불투명 타입 별칭의 일반적인 형태는 다음과 같습니다.

```
opaque type T >: L <: U = R
```

여기서 `L`(하한, lower bound)과 `U`(상한, upper bound)는 선택적(optional)이며, 각각 기본값은 `scala.Nothing`과 `scala.Any`입니다. 시스템은 `L <: R`과 `R <: U`를 강제합니다.

주목할 점은, 불투명 타입 별칭에서는 **F-경계(F-bound)가 지원되지 않는다**는 것입니다. 즉, `T`는 `L`이나 `U`에 나타날 수 없습니다.

#### 스코프 투명성 규칙 (Scope Transparency Rules)

레퍼런스는 이중적(dual) 동작을 설명합니다. 별칭 정의의 스코프 **안에서는** 별칭이 투명(transparent)합니다. 즉 `T`는 `R`의 일반적인 별칭(normal alias)처럼 취급됩니다. 별칭의 스코프 **밖에서는** 별칭이 추상 타입 `type T >: L <: U`처럼 취급됩니다.

객체에 정의된 별칭에는 특수한 경우가 적용됩니다. 객체가 불투명 타입을 포함할 때, 동등성(equality) 규칙이 조합(compose)되어 다음 코드가 유효해집니다.

```scala
object o:
  opaque type T = Int
  val x: Int = id(2)
def id(x: o.T): o.T = x
```

#### 제약 (Restrictions)

핵심 제약은 다음과 같습니다.

- 불투명 타입 별칭은 `private`일 수 없으며, 하위 클래스에서 오버라이드(override)할 수 없습니다.
- 불투명 타입 별칭은 오른쪽 항(right-hand side)으로 컨텍스트 함수 타입(context function type)을 가질 수 없습니다.

### 불투명 타입의 타입 매개변수 (Type Parameters of Opaque Types)

불투명 타입 별칭은 하나의 타입 매개변수 목록(single type parameter list)을 지원합니다. 유효한 예시는 다음과 같습니다.

```scala
opaque type F[T] = (T, T)
opaque type G = [T] =>> List[T]
```

유효하지 않은 구성은 다음과 같습니다.

```scala
opaque type BadF[T] = [U] =>> (T, U)
opaque type BadG = [T] =>> [U] =>> (T, U)
```

### 동등성의 변환 (Translation of Equality)

`==`나 `!=`를 사용하는 동등성 연산은 일반적으로 범용 동등성(universal equality)을 사용합니다. 단, 해당 타입에 대해 오버로딩된 `==`나 `!=` 연산자가 따로 정의된 경우는 예외입니다.

이 연산은 타입 검사(type checking) 이후에, 기반 타입(underlying type)에 정의된 (부)동등성 연산자로 매핑(map)됩니다.

```scala
opaque type T = Int

val x: T
val y: T
x == y    // uses Int equality for the comparison.
```

위 예시에서 `x == y` 비교는 (타입 검사 후) `Int`의 동등성을 사용하게 됩니다.

### 최상위 불투명 타입 (Top-level Opaque Types)

최상위 불투명 타입 별칭은 미묘한 스코핑(scoping)을 보입니다. 별칭이 나타나는 소스 파일 안의 다른 모든 최상위 정의(top-level definition)에서는 투명하지만, **중첩된 객체와 클래스(nested objects and classes), 그리고 다른 모든 소스 파일에서는 불투명**합니다.

`test1.scala`의 예시:

```scala
opaque type A = String
val x: A = "abc"

object obj:
  val y: A = "abc"  // error: found: "abc", required: A
```

`test2.scala`의 예시:

```scala
def z: String = x   // error: found: A, required: String
```

이 동작은 최상위 정의가 각자의 합성 객체(synthetic object)에 배치된다는 사실을 반영합니다. 따라서 `obj` 같은 중첩 객체 안에서는 `A`가 더 이상 투명하지 않으며, 다른 파일에서도 `A`는 추상 타입으로 보입니다.

### 트랜스패런트 인라인 메서드에서의 불투명 타입 (Opaque Types in Transparent Inline Methods)

불투명 타입이 그 정의 컨텍스트(defining context) 내에서 트랜스패런트 인라인 메서드(transparent inline method)로부터 반환될 때, 타입 추론은 세심한 처리를 요구합니다. 레퍼런스는 다음과 같이 설명합니다. 반환된 타입에는 별칭이 풀린(dealiased) 불투명 타입이 포함될 수 있습니다. 일반적으로 이는 그런 트랜스패런트 메서드 호출이 `DECLARED & ACTUAL`(선언된 타입 & 실제 타입)을 반환함을 의미합니다.

이 이슈를 보여주는 포괄적인 예시는 다음과 같습니다.

```scala
object Time:
  opaque type Time = String
  opaque type Seconds <: Time = String

  transparent inline def sec(n: Double): Seconds =
    s"${n}s": Seconds

  transparent inline def testInference(): List[Time] =
    List(sec(5))
  transparent inline def testGuarded(): List[Time] =
    List(sec(5)): List[Seconds]
  transparent inline def testExplicitTime(): List[Time] =
    List[Seconds](sec(5))
  transparent inline def testExplicitString(): List[Time] =
    List[String](sec(5))

end Time

@main def main() =
  val t1: List[String] = Time.testInference()
  val t2: List[Time.Seconds] = Time.testGuarded()
  val t3: List[Time.Seconds] = Time.testExplicitTime()
  val t4: List[String] = Time.testExplicitString()
```

각 메서드는 선언된 반환 타입(`List[Time]`)을 갖지만, 트랜스패런트 인라인이므로 호출 측에서는 더 정밀한 실제 타입이 드러날 수 있습니다. 어떤 타입 어노테이션(`: List[Seconds]`, `List[Seconds](...)`, `List[String](...)`)으로 결과를 안내(guard)하느냐에 따라, 호출 결과 `t1`~`t4`가 서로 다른 정밀한 타입(`List[String]`, `List[Time.Seconds]`)으로 추론되는 것을 볼 수 있습니다.

### SIP 35와의 관계 (Relationship to SIP 35)

레퍼런스는 SIP 35와의 다섯 가지 핵심 차이점을 기록합니다.

1. 불투명 타입 별칭은 더 이상 지역 문장 시퀀스(local statement sequence)에서 정의될 수 없습니다.
2. 불투명 타입 별칭이 보이는 스코프는, 이제 (단지 동반 객체만이 아니라) 그것이 정의된 스코프 전체입니다.
3. 불투명 타입 별칭에 대한 동반 객체(companion object) 개념이 폐기되었습니다.
4. 불투명 타입 별칭은 경계(bound)를 가질 수 있습니다.
5. 불투명 타입 별칭이 관여하는 타입 동등성(type equality)의 개념이 명확해졌습니다. 이는 이전 SIP 35 구현에 비해 강화(strengthen)되었습니다.

---

## 6. export 절 (Export Clauses)

### 핵심 목적 (Core Purpose)

export 절(export clause)은 객체의 선택된 멤버에 대한 **별칭(alias)** 을 생성하여, **상속(inheritance)보다 조합(composition)** 을 촉진합니다. export 절은 장황한 위임 메서드(forwarding method)를 작성하지 않고도 내부 컴포넌트의 멤버를 노출(expose)하는 간결한 구문을 제공합니다.

### 주요 예시 (Primary Example)

공식 문서는 `Printer`와 `Scanner` 컴포넌트를 결합하는 `Copier` 클래스를 보여줍니다.

```scala
class Copier:
  private val printUnit = new Printer { type PrinterType = InkJet }
  private val scanUnit = new Scanner

  export scanUnit.scan
  export printUnit.{status as _, *}

  def status: List[String] = printUnit.status ++ scanUnit.status
```

이는 다음과 같은 별칭을 생성합니다.

- `final def scan(): BitMap = scanUnit.scan()` 같은 메서드 별칭
- 타입 별칭(type alias) 또한 자동으로 생성됨

`export scanUnit.scan`은 `scanUnit`의 `scan` 멤버를 `Copier`에 노출하고, `export printUnit.{status as _, *}`는 `printUnit`의 `status`를 제외(`status as _`)한 나머지 모든 멤버(`*`)를 노출합니다. `status`는 `Copier`가 직접 정의하므로 제외한 것입니다.

### 셀렉터 종류 (Selector Types)

구문은 여러 셀렉터(selector) 패턴을 지원합니다.

- **단순(Simple)** `x`: `x`라는 이름의 멤버에 대해 별칭을 생성합니다.
- **이름 변경(Renaming)** `x as y`: `x`를 `y`라는 이름으로 별칭을 만듭니다.
- **생략(Omitting)** `x as _`: 와일드카드에 의해 `x`가 별칭으로 만들어지는 것을 방지합니다.
- **given 셀렉터(Given selector)** `given x`: `x` 타입에 부합하는 given 인스턴스(given instance)에 대해 별칭을 만듭니다.
- **와일드카드(Wildcard)** `*`: given과 합성(synthetic) 멤버를 제외한 모든 적격(eligible) 멤버에 대해 별칭을 만듭니다.

### 적격 요건 (Eligibility Requirements)

멤버는 다음 조건을 모두 만족할 때만 별칭 대상이 됩니다.

- 포함하는 클래스의 기반 클래스(base class)가 소유한 멤버가 아닐 것
- 기반 클래스의 구체적인(concrete) 정의를 오버라이드하지 않을 것
- export 위치(export location)에서 접근 가능할 것
- 생성자(constructor)나 클래스의 합성 부분(synthetic part)이 아닐 것
- given 인스턴스는 오직 given 셀렉터를 통해서만 별칭이 될 것

### EBNF 문법 (EBNF Grammar)

레퍼런스가 명시하는 형식 구문은 다음과 같습니다.

```
Export            ::=  'export' ImportExpr {',' ImportExpr}
ImportExpr        ::=  SimpleRef {'.' id} '.' ImportSpec
ImportSpec        ::=  NamedSelector
                    |  WildcardSelector
                    | '{' ImportSelectors) '}'
NamedSelector     ::=  id ['as' (id | '_')]
WildCardSelector  ::=  '*' | 'given' [InfixType]
ImportSelectors   ::=  NamedSelector [',' ImportSelectors]
                    |  WildCardSelector {',' WildCardSelector}
```

### 주요 제약 (Key Restrictions)

명세는 다음 제약들을 나열합니다.

1. export 절은 오직 클래스/객체/트레이트 안이나 최상위(top-level)에만 나타날 수 있으며, **블록 문장(block statement)으로는 절대 쓸 수 없습니다.**
2. 와일드카드/given 셀렉터는 패키지(package)를 참조할 수 없습니다(증분 컴파일, incremental compilation의 안전성을 위함).
3. 이름 변경(renaming)은 그 대상 이름(target name)과 일치하는, 이름이 변경되지 않은 export들을 가립니다(hide).
4. 이름 변경의 대상 이름들은 서로 쌍별로 구별되어야(pairwise distinct) 합니다.
5. `export status as stat` 같은 단순 이름 변경(simple renaming)은 현재 지원되지 않습니다.
6. 무인자(nullary) Java 메서드의 별칭은 무인자 Scala 메서드가 되며, 괄호(`()`)를 요구합니다.

### 타입 멤버와 항 멤버의 처리 (Type and Term Member Handling)

타입 멤버(type member)는 타입 정의(type definition)로 별칭이 되고, 항 멤버(term member)는 메서드 정의(method definition)로 별칭이 됩니다. export 별칭의 메서드는 `final`로 유지됩니다. 단, 실험적(experimental) `modularity` 임포트 하에서는 타입 별칭이 `final`이 아닌 형태가 됩니다.

### modularity 기능 노트 (Modularity Feature Note)

실험적 `modularity` 언어 임포트 하에서는, 오직 export된 메서드와 값(value)만 `final`이며, 생성되는 `PrinterType`은 `final` 수식어가 없는 단순 타입 별칭(simple type alias)이 됩니다.

### 확장 지원 (Extensions Support)

export 절은 확장 블록(extension block) 내에서도 동작합니다. 이때 한정자(qualifier)는 같은 절(clause) 안에 있는 매개변수 없는(parameterless) 확장 메서드를 참조해야 합니다.

### 정교화 순서 (Elaboration Order)

export 처리는 클래스 타입 완성(class type completion) 도중에, 부모 타입(parent type)이 결정된 후에 일어납니다. 모든 경로(path)는 어떤 별칭으로 진입하기 전에 타입이 결정되어, 같은 클래스 내의 export들 사이에서 순환 참조(circular reference)가 생기는 것을 방지합니다.

### 동기 (Motivation)

export 절은, 조합(composition)을 상속(inheritance) 관계만큼이나 간결하고 표현하기 쉽게 만들어 줌으로써, 언어 설계의 빈틈을 메웁니다. 또한 멤버 이름 변경과 선택적 노출(selective exposure)을 지원하는데, 이는 상속이 제공하지 못하는 기능입니다.

---

## 7. 범용 apply 메서드 / 생성자 앱 (Universal Apply Methods / Creator Applications)

### 개요 (Overview)

Scala 3는 생성자 프록시(constructor proxy) 기능을 케이스 클래스(case class)를 넘어 **모든 구체 클래스(concrete class)** 로 확장합니다. 이 기능을 통해 `new` 키워드 없이 함수 적용(function application) 구문으로 인스턴스를 생성할 수 있습니다.

### 기본 예시 (Basic Example)

```scala
class StringBuilder(s: String):
  def this() = this("")

StringBuilder("abc")  // old: new StringBuilder("abc")
StringBuilder()       // old: new StringBuilder()
```

### 생성되는 동반 객체 (Generated Companion Object)

컴파일러는 `apply` 메서드를 가진 동반 객체를 자동으로 생성합니다.

```scala
object StringBuilder:
  inline def apply(s: String): StringBuilder = new StringBuilder(s)
  inline def apply(): StringBuilder = new StringBuilder()
```

각 `apply` 메서드는 대응하는 생성자(constructor)와 동일하게 `inline`으로 만들어집니다.

### 생성자 프록시 규칙 (Constructor Proxy Rules)

**규칙 1 — 동반 객체 생성(Companion Object Creation):**

구체 클래스 `C`에 대해, 다음 조건이 모두 성립할 때 생성자 프록시 동반 객체가 생성됩니다.

- 그 클래스에 기존 동반 객체가 없을 것
- 그 스코프에 `C`라는 이름의 다른 값(value)이나 메서드가 정의되거나 상속되지 않았을 것

**규칙 2 — apply 메서드 생성(Apply Method Generation):**

다음 조건이 모두 성립할 때 생성자 프록시 `apply` 메서드가 생성됩니다.

- 그 클래스에 동반 객체가 있을 것(자동 생성된 것일 수도 있음)
- 그 동반 객체가 이미 `apply` 멤버를 정의하고 있지 않을 것

생성되는 각 메서드는 그것이 대응하는 생성자의 타입 매개변수(type parameter), 값 매개변수(value parameter), 접근 제한(access restriction)과 일치합니다. 만약 클래스가 `protected`라면, 동반 객체가 `protected`이거나 `apply` 메서드가 `protected`가 됩니다.

### 제약 (Restrictions)

- 생성자 프록시는 독립적인 값(standalone value)으로 기능할 수 없습니다. 즉, `apply` 선택(selection)이나 암묵적 적용(implicit application)을 요구합니다.
- 또한, 생성자 프록시 식별자가 다른 스코프에서 임포트되거나 정의된 메서드(또는 `apply`를 포함하는 값)로도 해석(resolve)될 때, 모호함(ambiguity) 오류가 발생합니다.

### 동기 (Motivation)

이 접근법은 구현 세부사항(implementation specifics)을 감추고, 코드 가독성(readability)을 높이며, 모든 구체 클래스에 걸쳐 객체 생성 구문을 표준화함으로써 언어의 규칙성(regularity)을 향상시킵니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other New Features](https://docs.scala-lang.org/scala3/reference/other-new-features/)
- [Trait Parameters](https://docs.scala-lang.org/scala3/reference/other-new-features/trait-parameters.html)
- [Transparent Traits](https://docs.scala-lang.org/scala3/reference/other-new-features/transparent-traits.html)
- [Open Classes](https://docs.scala-lang.org/scala3/reference/other-new-features/open-classes.html)
- [Opaque Type Aliases](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques.html)
- [Opaque Type Aliases: more details](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques-details.html)
- [Export Clauses](https://docs.scala-lang.org/scala3/reference/other-new-features/export.html)
- [Universal Apply Methods](https://docs.scala-lang.org/scala3/reference/other-new-features/creator-applications.html)
