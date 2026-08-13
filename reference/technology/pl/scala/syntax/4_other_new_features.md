# Scala 3 기타 새 기능: 모델링과 문법

## Scala 3 기타 새 기능: 클래스와 트레이트 모델링

---

### 목차

1. [트레이트 매개변수 (Trait Parameters)](#1-트레이트-매개변수-trait-parameters)
2. [트랜스패런트 트레이트 (Transparent Traits)](#2-트랜스패런트-트레이트-transparent-traits)
3. [open 클래스 (Open Classes)](#3-open-클래스-open-classes)
4. [불투명 타입 별칭 (Opaque Type Aliases)](#4-불투명-타입-별칭-opaque-type-aliases)
5. [불투명 타입 별칭: 더 자세히 (Opaque Type Aliases: more details)](#5-불투명-타입-별칭-더-자세히-opaque-type-aliases-more-details)
6. [export 절 (Export Clauses)](#6-export-절-export-clauses)
7. [범용 apply 메서드 / 생성자 앱 (Universal Apply Methods / Creator Applications)](#7-범용-apply-메서드--생성자-앱-universal-apply-methods--creator-applications)

---

### 1. 트레이트 매개변수 (Trait Parameters)

- Scala 3: 클래스처럼 트레이트(trait)도 매개변수를 가질 수 있도록 허용

참고 — 트레이트 매개변수가 뭔가
- `trait`는 인터페이스에 구현·필드까지 더한 것
- Scala 2의 트레이트: 클래스와 달리 생성자 매개변수를 받을 수 없었음
  - → "외부에서 받은 값으로 초기화해야 하는 트레이트"를 만들 수 없었음
  - → 그 자리에 추상 멤버(`def name: String`)를 두고 상속하는 클래스가 채우게 하는 우회법을 써야 했음
- Scala 3: 클래스처럼 `trait Greeting(val name: String)`으로 트레이트에도 매개변수를 줄 수 있게 함

왜 필요한가
- 트레이트는 여러 개를 동시에 믹스인할 수 있음 → 매개변수를 허용하면 "같은 트레이트가 서로 다른 값으로 두 번 섞이면 무슨 값을 써야 하나" 하는 모호함 문제가 생김
- 아래의 까다로운 세 가지 규칙은 전부 이 모호함을 원천 차단하기 위한 것
- 클래스는 단일 상속이라 이런 고민이 없었지만, 트레이트는 다중 믹스인이므로 규칙이 더 엄격함

```scala
trait Greeting(val name: String):
  def msg = s"How are you, $name"

class C extends Greeting("Bob"):
  println(msg)
```

- 매개변수화된 트레이트(parameterized trait)의 인자(argument): 트레이트가 초기화(initialization)되기 직전에 즉시 평가됨
- 같은 매개변수화된 트레이트를 서로 다른 인자로 두 번 상속(extend)하면 모호함(ambiguity) 발생 → 이를 방지하는 규칙 필요

#### 트레이트 매개변수 규칙

트레이트 매개변수의 사용을 규율하는 세 가지 규칙.

1. 클래스 `C`가 매개변수화된 트레이트 `T`를 상속하고, `C`의 상위 클래스는 `T`를 상속하지 않는 경우: `C`는 반드시 `T`에 인자를 전달해야 함
2. 클래스 `C`가 매개변수화된 트레이트 `T`를 상속하고, `C`의 상위 클래스도 `T`를 상속하는 경우: `C`는 `T`에 인자를 전달해서는 안 됨(모호함 방지)
3. 트레이트는 부모 트레이트에 인자를 전달할 수 없음

#### 모호함 방지 (Ambiguity Prevention)

- 매개변수화된 트레이트를 서로 다른 인자로 여러 번 상속하려고 시도 → 컴파일러 오류 발생

```scala
class D extends C, Greeting("Bill") // error: parameter passed twice
```

- 위 예시: `D`는 `C`(이미 `Greeting("Bob")`을 통해 `Greeting`을 상속함)를 상속하면서, 동시에 `Greeting("Bill")`을 다시 상속하려 함
- 그 결과 `Greeting`의 매개변수가 두 번 전달되어(parameter passed twice) 오류가 됨

#### 트레이트 상속 예시 (Trait Inheritance Example)

```scala
trait FormalGreeting extends Greeting:
  override def msg = s"How do you do, $name"

class E extends Greeting("Bob"), FormalGreeting
```

- `E`는 `Greeting`과 `FormalGreeting`을 모두 명시적으로 상속해야 함
- 이유: `FormalGreeting` 자체만으로는 `Greeting`에 인자를 제공하지 않음(규칙 3에 따라 트레이트는 부모 트레이트에 인자를 전달할 수 없음)
- → `Greeting`의 매개변수를 채워주는 책임은 구체적인(concrete) 클래스인 `E`에게 있음

#### 컨텍스트 매개변수에 대한 예외 (Context Parameters Exception)

- 이 "명시적으로 부모를 상속해야 한다"는 요구사항: 트레이트가 오직 컨텍스트 매개변수(context parameter)만 가질 때 완화됨
- 이 경우 컴파일러가 추론된 인자와 함께 해당 트레이트를 자동으로 삽입

참고 — 컨텍스트 매개변수(`using`)
- `using`: "이 값은 내가 직접 안 넘겨도, 컴파일러가 범위 안에서 알맞은 것을 찾아 자동으로 채워라"라는 매개변수
  - implicit 매개변수가 Scala 3에서 `using`/`given`으로 갈라진 것
- 트레이트의 매개변수가 전부 이런 `using`뿐이라면, 어차피 값을 컴파일러가 알아서 찾아주므로 사람이 부모 트레이트를 일일이 다시 적어줄 필요가 없음
- → 이 경우에만 규칙이 느슨해짐

```scala
case class ImpliedName(name: String):
  override def toString = name

trait ImpliedGreeting(using val iname: ImpliedName):
  def msg = s"How are you, $iname"

trait ImpliedFormalGreeting extends ImpliedGreeting:
  override def msg = s"How do you do, $iname"

class F(using iname: ImpliedName) extends ImpliedFormalGreeting
```

- `ImpliedGreeting`은 `using` 컨텍스트 매개변수만 가짐 → 컴파일러가 `F`의 컨텍스트 범위에서 `ImpliedName` 인스턴스를 자동으로 찾아 채움
- → `F`를 작성할 때 `ImpliedGreeting`을 명시적으로 다시 나열할 필요가 없음

---

### 2. 트랜스패런트 트레이트 (Transparent Traits)

왜 필요한가
- Scala에서는 `val x = ...`처럼 타입을 안 적으면 컴파일러가 알아서 타입을 추론함
- 서로 다른 값을 한데 모으면(예: `Set(a, b)`) 컴파일러는 둘의 공통 조상 타입을 찾음 → 이때 사람은 신경 쓰지 않는 `Product`나 `Serializable` 같은 "내부 사정 트레이트"까지 공통점이라며 타입에 끌어다 붙임
- 그러면 깔끔한 `Set[Kind]` 대신 `Set[Kind & Product & Serializable]` 같은 지저분한 타입이 추론됨
- `transparent`: "이 트레이트는 추론 결과에 표시하지 마라"라고 알려, 추론된 타입을 깔끔하게 유지

- 트랜스패런트 트레이트(transparent trait): 특정 트레이트가 추론된 타입(inferred type)을 어지럽히는 것을 방지하기 위한 Scala 3 기능
- 독립적인 타입(standalone type)이라기보다 주로 믹스인(mixin)으로 쓰이는 트레이트에 표시

#### 해결하려는 문제

- Scala 2: 컴파일러가 자동으로 추가하는 트레이트(예: `Product`)가 원치 않는 경우에도 추론된 타입에 그대로 나타나는 문제

```scala
trait Kind
case object Var extends Kind
case object Val extends Kind
val x = Set(if condition then Val else Var)
```

- 위 코드: `x`는 더 단순한 `Set[Kind]`가 아니라, `Set[Kind & Product & Serializable]`로 추론됨
- 즉 사용자가 신경 쓰지 않는 구현 세부사항(`Product`, `Serializable`)이 타입에 노출됨

참고 — `A & B`(교집합 타입)
- `Kind & Product & Serializable`의 `&`: 이 셋을 동시에 만족하는 타입이라는 뜻의 교집합 타입
  - Scala 2의 `A with B`에 대응
- `case object`로 선언하면 컴파일러가 `Product`와 `Serializable`을 자동으로 끼워 넣음 → 여기서 원치 않게 `& Product & Serializable`이 따라붙은 것

#### 해결책: 트랜스패런트 선언

- 트레이트를 `transparent`로 표시 → 추론된 타입에서 해당 트레이트가 억제(suppress)됨

```scala
transparent trait S
trait Kind
object Var extends Kind, S
object Val extends Kind, S
val x = Set(if condition then Val else Var)
```

- 이제 `x`는 `Set[Kind]` 타입을 가짐
- `Kind` 자체를 `transparent`로 선언하면, 합집합(union)을 넓히지 않고 `Set[Val | Var]`를 얻을 수 있음

#### 자동으로 트랜스패런트로 취급되는 타입

- `scala.Any`
- `scala.AnyVal`
- `scala.Matchable`
- `scala.Product`
- `java.lang.Object`
- `java.lang.Comparable`
- `java.io.Serializable`

#### 타입 추론 규칙

타입 추론 시 동작.

1. 교집합(intersection)에서 가능하면 트랜스패런트 트레이트를 제거
2. 결과가 트랜스패런트 상위 타입(supertype)만 남게 된다면, 합집합을 넓히지 않음
3. 단일 트랜스패런트 인스턴스는 보존(예: `Product` 하나만 있을 때는 `Any`로 넓히지 않고 그대로 둠)
4. 넓혔을 때 트랜스패런트가 아닌 타입이 모두 사라진다면, 원래의 합집합 타입을 유지

- 이 규칙은 타입 변수(type variable), `val`, `def`의 반환 타입 추론에 적용됨(고차 종류(higher-kinded type)는 제외)
- 구현 세부사항에 해당하는 트레이트를 억제하면서도 타입을 최대한 구체적으로 유지

---

### 3. open 클래스 (Open Classes)

왜 필요한가 — 취약한 기반 클래스 문제(fragile base class problem)
- Scala 2(와 Java): `final`만 안 붙이면 아무 클래스나 상속할 수 있었음 → 편해 보이지만 문제 있음
- 라이브러리 저자가 "이 클래스가 상속될 줄 몰랐는데" 나중에 내부 구현을 살짝 바꾸면, 그 클래스를 멋대로 상속해서 일부 메서드만 오버라이드해 쓰던 외부 코드가 소리 없이 깨짐 → 이것이 취약한 기반 클래스 문제
- `open`은 이 흐름을 뒤집음 → "상속해도 된다"가 기본이 아니라, 저자가 `open`을 붙여 명시적으로 상속을 허락한 클래스만 다른 파일에서 마음 놓고 상속하게 함
- 상속 가능성을 우연이 아니라 설계자의 의도로 만드는 것이 핵심

- `open` 수식어(modifier): 해당 클래스가 하위 클래스(subclass)에 의한 상속(extension)을 위해 설계되었음을 알림
- 확장 가능성에 대한 세 가지 의도(intent) 수준을 구별

#### 사용 예시

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

- `Writer`는 `open`으로 표시됨 → 다른 파일에 있는 `EncryptedWriter`가 자유롭게 상속 가능

#### 상속 규칙 (Extension Rules)

`open`이 아닌 클래스라도 두 가지 조건 하에서는 여전히 상속 가능.

1. 같은 파일 내 상속(Same-file extensions): 상속하는 클래스가, 상속되는 클래스와 같은 소스 파일(source file) 안에 있는 경우
2. 기능 게이트 상속(Feature-gated extensions): `adhocExtensions` 언어 기능(language feature)이 다음 중 하나의 방법으로 활성화된 경우
   - 임포트: `import scala.language.adhocExtensions`
   - 컴파일러 플래그: `-language:adhocExtensions`

- 이 기능이 활성화되어 있지 않으면, 파일을 넘나드는(cross-file) 상속을 시도할 때 경고(warning) 발생

#### 설계 근거 (Design Rationale)

확장 가능성에 대한 세 가지 기대(expectation).

1. 의도된 상속(Intentional extensions): `open`으로 표시되며, 문서화된 상속 계약(extension contract)을 동반
2. 상속 없음(No extensions): `final`로 표시되며, 정확성(correctness)/보안(security) 보장을 제공
3. 임시 상속(Ad-hoc extensions): 수식어가 없으며, 허용되지만 지원되지 않고(unsupported) 깨지기 쉬움(fragile)

- 이 설계는 깨지기 쉬운 임시 상속을 권장하지 않으면서도, 테스트나 임시 패치(temporary patch)처럼 명시적으로 동의(opt-in)한 경우에는 그것을 허용

#### 기술적 세부사항 (Technical Details)

- `open`은 소프트 수식어(soft modifier)로 동작(수식어 위치가 아닌 곳에서는 일반 식별자로 사용 가능)
- `open`을 `final`이나 `sealed`와 결합할 수 없음
- 트레이트와 추상 클래스(abstract class)에는 `open`이 불필요(이들은 항상 상속 가능하기 때문)

주의 — `open`이 아니면 상속이 막힌다는 오해
- "`open`이 아니면 상속이 막힌다"고 오해하기 쉽지만, 그렇지 않음
- `open` 없는 클래스도 같은 파일 안에서는 자유롭게 상속할 수 있고, 다른 파일에서 상속하면 막히는 게 아니라 경고만 뜸
- `adhocExtensions` 기능을 켜면 그 경고도 사라짐
- 즉 `open`은 강제 차단(`final`)이 아니라 "이 클래스를 다른 파일에서 상속하는 건 의도된 것이 아니다"라는 신호이자 권장 사항에 가까움
- 완전히 막고 싶으면 `final`을 씀

#### 마이그레이션 노트 (Migration Note)

- 임시 상속에 대한 기능 경고(feature warning): 현재 `-source future` 하에서만 활성화되며, Scala 3.4부터는 기본값(default)이 됨

---

### 4. 불투명 타입 별칭 (Opaque Type Aliases)

왜 필요한가 — value class가 풀지 못한 것
- "센티미터를 담는 `Double`"과 "인치를 담는 `Double`"을 타입으로 구분하고 싶다고 가정 → 그냥 `Double`을 쓰면 둘이 섞여 버그가 남
- 한 겹 감싸는데, 보통 클래스로 감싸면 객체를 새로 만드는 비용이 듦
- Scala 2의 value class(`extends AnyVal`)가 이 비용을 없애려 했지만, 배열에 담기거나 다른 타입과 섞이는 등 여러 상황에서 결국 박싱(boxing, 객체로 다시 감싸짐)이 일어나 한계가 있었음
- opaque type은 컴파일 시점에만 별개의 타입인 척하고, 컴파일이 끝나면 그냥 원래 `Double`로 사라짐
- 즉 타입 안전성은 얻으면서 런타임 비용은 0

참고 — "불투명(opaque)"이라는 이름
- 유리가 "투명(transparent)"하면 속이 다 비치고, "불투명(opaque)"하면 안 보임
- `opaque type Logarithm = Double`: 정의된 안쪽에서는 `Logarithm`이 사실 `Double`임이 다 보이지만(투명), 바깥에서는 속이 안 보여서(불투명) 그냥 `Logarithm`이라는 별개 타입으로만 다뤄진다는 뜻
- 안과 밖에서 다르게 보이는 것이 이 기능의 핵심

- 불투명 타입 별칭(opaque type alias): 런타임 오버헤드 없이 타입 추상화(type abstraction)를 가능하게 함
- 핵심: 불투명 타입이 정의된 스코프 내부에서는 투명하게 보이지만, 외부 코드에서는 추상 타입(abstract type)처럼 보임

#### 핵심 개념

- 불투명 타입 별칭: `opaque type` 키워드로 선언, 기반(underlying) 구현 타입을 지정
- 구현 타입은 정의된 스코프 안에서만 보이며, 외부 코드는 기반 타입에 직접 접근할 수 없음

#### 기본 예시: Logarithm 타입

공식 문서가 제공하는 예시.

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

참고 — 여기서 쓰인 동반 객체와 확장 메서드
- 안쪽에서만 `Logarithm`이 `Double`임을 알기 때문에, "`Double`로 `Logarithm`을 만드는" 통로(`Logarithm.apply`)와 "`Logarithm`에 쓸 수 있는 연산"(`extension`으로 붙인 `+`, `*` 등)을 안쪽에서 미리 다 만들어 둬야 함
- `object Logarithm`: 그 타입의 생성·도구를 모아두는 동반 객체
- `extension (x: Logarithm)`: 기존 타입에 메서드를 덧붙이는 확장 메서드
- 바깥 코드는 이렇게 공개된 통로로만 `Logarithm`을 다룰 수 있고, 속이 `Double`이라는 사실은 이용할 수 없음

- 공개 API: 동반 객체(companion object)의 메서드와 확장 메서드(extension method)를 통해 통제
- 스코프 안에서 유효한 연산들

```scala
import MyMath.Logarithm

val l = Logarithm(1.0)
val l2 = Logarithm(2.0)
val l3 = l * l2
val l4 = l + l2
```

- 반면, 추상화를 위반하는 연산은 타입 오류를 발생시킴

```scala
val d: Double = l       // error: found: Logarithm, required: Double
val l2: Logarithm = 1.0 // error: found: Double, required: Logarithm
l * 2                   // error: found: Int(2), required: Logarithm
l / l2                  // error: `/` is not a member of Logarithm
```

- `Logarithm`은 외부에서는 `Double`과 호환되지 않는 별개의 타입으로 취급 → 위와 같이 `Double`로 직접 대입하거나 정수를 곱하는 등의 연산이 금지됨

#### 불투명 타입 별칭의 경계 (Bounds for Opaque Type Aliases)

- 불투명 타입은 상한(upper bound)이나 하한(lower bound) 경계를 포함할 수 있음
- 공식 문서는 권한(permission) 시스템 예시로 이를 보여줌

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

- 경계 `Permission <: Permissions & PermissionChoice`: 객체 외부에서 `Permission`이 `Permissions`와 `PermissionChoice`의 하위 타입(subtype)이 되도록 만듦
- 이를 통해 다음과 같은 대입이 가능

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

- `Read`, `Write` 등은 `Permission` 타입이지만, `Permission <: Permissions`이므로 `Permissions`를 요구하는 `Item(...)`에 대입될 수 있음
- 그러나 서로 관련 없는(unrelated) 불투명 타입은 섞을 수 없음 — 다음 코드는 오류 발생

```scala
roItem.rights.isOneOf(ReadWrite) // error
```

- `ReadWrite`는 `Permissions` 타입이고 `isOneOf`는 `PermissionChoice`를 요구함 → `Access` 객체 외부에서는 `Permissions`와 `PermissionChoice`가 서로 관련 없는 별개의 타입이기 때문

#### 클래스의 불투명 타입 멤버 (Opaque Type Members on Classes)

- 불투명 타입은 클래스에서도 동작

```scala
class Logarithms:

  opaque type Logarithm = Double

  def apply(d: Double): Logarithm = math.log(d)

  def safe(d: Double): Option[Logarithm] =
    if d > 0.0 then Some(math.log(d)) else None

  def mul(x: Logarithm, y: Logarithm) = x + y
```

- 중요한 점: 서로 다른 클래스 인스턴스의 불투명 타입 멤버는 각각 별개의 타입으로 취급됨

주의 — `l1.Logarithm`과 `l2.Logarithm`은 다른 타입
- 불투명 타입을 `object`가 아니라 `class` 안에 두면, 인스턴스마다 타입이 따로 생김
- 즉 `l1`이 만든 `Logarithm`과 `l2`가 만든 `Logarithm`은 이름은 같아도 서로 다른 타입으로 취급되어 섞을 수 없음
  - 이를 "경로 의존 타입(path-dependent type)"이라 부름 — 어떤 인스턴스에 속했느냐가 타입의 일부가 됨
- 인스턴스와 무관하게 하나의 타입을 쓰고 싶다면 `object`나 최상위에 두면 됨

```scala
val l1 = new Logarithms
val l2 = new Logarithms
val x = l1(1.5)
val y = l1(2.6)
val z = l2(3.1)
l1.mul(x, y) // type checks
l1.mul(x, z) // error: found l2.Logarithm, required l1.Logarithm
```

- 위 예시: `x`, `y`는 `l1.Logarithm` 타입이고 `z`는 `l2.Logarithm` 타입
- 이 둘은 다른 인스턴스에 속하므로 서로 호환되지 않아, `l1.mul(x, z)`는 타입 오류가 됨

- 클래스 내 불투명 타입의 투명성 범위: `private[this]` 스코프와 동등
- 최상위(top-level) 불투명 타입의 경우, 투명성은 정의된 파일 안으로 제한됨

---

### 5. 불투명 타입 별칭: 더 자세히 (Opaque Type Aliases: more details)

#### 문법 (Syntax)

`opaque` 수식어는 다음과 같이 정의됨.

```
Modifier          ::=  ...
                    |  'opaque'
```

- `opaque`는 소프트 수식어(soft modifier)이며, 정의 키워드 앞에 오지 않을 때는 일반 식별자로 사용 가능
- 불투명 타입 별칭은 위치에 제약이 있음 → 반드시 클래스, 트레이트, 객체의 멤버이거나, 최상위(top-level)에 정의되어야 함
- 지역 블록(local block)에서는 정의할 수 없음

#### 타입 검사 (Type Checking)

단형(monomorphic) 불투명 타입 별칭의 일반적인 형태.

```
opaque type T >: L <: U = R
```

- 여기서 `L`(하한, lower bound)과 `U`(상한, upper bound)는 선택적이며, 각각 기본값은 `scala.Nothing`과 `scala.Any`
- 시스템은 `L <: R`과 `R <: U`를 강제

- 주목할 점: 불투명 타입 별칭에서는 F-경계(F-bound)가 지원되지 않음 → `T`는 `L`이나 `U`에 나타날 수 없음

##### 스코프 투명성 규칙 (Scope Transparency Rules)

- 별칭 정의의 스코프 안에서는 별칭이 투명 → `T`는 `R`의 일반 별칭(normal alias)처럼 취급됨
- 스코프 밖에서는 별칭이 추상 타입 `type T >: L <: U`처럼 취급됨
- 객체에 정의된 별칭에는 특수한 경우가 적용 — 객체가 불투명 타입을 포함할 때, 동등성(equality) 규칙이 합성(compose)되어 다음 코드가 유효해짐

```scala
object o:
  opaque type T = Int
  val x: Int = id(2)
def id(x: o.T): o.T = x
```

##### 제약 (Restrictions)

핵심 제약.

- 불투명 타입 별칭은 `private`일 수 없으며, 하위 클래스에서 오버라이드(override)할 수 없음
- 불투명 타입 별칭은 오른쪽 항(right-hand side)으로 컨텍스트 함수 타입(context function type)을 가질 수 없음

#### 불투명 타입의 타입 매개변수 (Type Parameters of Opaque Types)

- 불투명 타입 별칭은 하나의 타입 매개변수 목록(single type parameter list)을 지원

유효한 예시.

```scala
opaque type F[T] = (T, T)
opaque type G = [T] =>> List[T]
```

유효하지 않은 구성.

```scala
opaque type BadF[T] = [U] =>> (T, U)
opaque type BadG = [T] =>> [U] =>> (T, U)
```

#### 동등성의 변환 (Translation of Equality)

- `==`나 `!=`를 사용하는 동등성 연산: 일반적으로 범용 동등성(universal equality)을 사용
  - 단, 해당 타입에 대해 오버로딩된 `==`나 `!=` 연산자가 따로 정의된 경우는 예외
- 이 연산은 타입 검사(type checking) 이후에, 기반 타입(underlying type)에 정의된 (부)동등성 연산자로 매핑(map)됨

```scala
opaque type T = Int

val x: T
val y: T
x == y    // uses Int equality for the comparison.
```

- 위 예시: `x == y` 비교는 (타입 검사 후) `Int`의 동등성을 사용

#### 최상위 불투명 타입 (Top-level Opaque Types)

- 최상위 불투명 타입 별칭: 미묘한 스코핑 동작을 보임
- 별칭이 위치한 소스 파일 내의 다른 최상위 정의에서는 투명하지만, 중첩된 객체와 클래스, 그리고 다른 모든 소스 파일에서는 불투명함

`test1.scala`의 예시.

```scala
opaque type A = String
val x: A = "abc"

object obj:
  val y: A = "abc"  // error: found: "abc", required: A
```

`test2.scala`의 예시.

```scala
def z: String = x   // error: found: A, required: String
```

- 이는 최상위 정의가 각자의 합성 객체(synthetic object)에 배치된다는 사실을 반영
- `obj` 같은 중첩 객체 안에서는 `A`가 더 이상 투명하지 않으며, 다른 파일에서도 `A`는 추상 타입으로 보임

#### 트랜스패런트 인라인 메서드에서의 불투명 타입 (Opaque Types in Transparent Inline Methods)

- 불투명 타입이 그 정의 컨텍스트 내에서 트랜스패런트 인라인 메서드(transparent inline method)로부터 반환될 때, 타입 추론은 세심한 처리를 요구함
- 반환된 타입에는 별칭이 풀린(dealiased) 불투명 타입이 포함될 수 있으며, 이러한 트랜스패런트 메서드 호출은 `DECLARED & ACTUAL`(선언된 타입 & 실제 타입)을 반환함

이 이슈를 보여주는 포괄적인 예시.

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

- 각 메서드는 선언된 반환 타입(`List[Time]`)을 갖지만, 트랜스패런트 인라인이므로 호출 측에서는 더 정밀한 실제 타입이 드러날 수 있음
- 어떤 타입 어노테이션(`: List[Seconds]`, `List[Seconds](...)`, `List[String](...)`)으로 결과를 안내하느냐에 따라, `t1`~`t4`가 서로 다른 정밀한 타입(`List[String]`, `List[Time.Seconds]`)으로 추론됨

#### SIP 35와의 관계 (Relationship to SIP 35)

SIP 35와의 다섯 가지 핵심 차이점.

1. 불투명 타입 별칭은 더 이상 지역 문장 시퀀스(local statement sequence)에서 정의될 수 없음
2. 불투명 타입 별칭이 보이는 스코프는, 이제(단지 동반 객체만이 아니라) 그것이 정의된 스코프 전체
3. 불투명 타입 별칭에 대한 동반 객체(companion object) 개념이 폐기됨
4. 불투명 타입 별칭은 경계(bound)를 가질 수 있음
5. 불투명 타입 별칭이 관여하는 타입 동등성(type equality)의 개념이 명확해짐(이전 SIP 35 구현에 비해 강화됨)

---

### 6. export 절 (Export Clauses)

#### 핵심 목적 (Core Purpose)

왜 필요한가 — 상속 대신 합성(위임)
- 어떤 클래스에 다른 객체의 기능을 빌려오는 방법은 둘
  1. 상속: 그 클래스를 물려받음 — 하지만 위의 `open` 절에서 봤듯 상속은 깨지기 쉽고, 한 부모만 고를 수 있는 등 제약이 많음
  2. 합성(위임): 그 객체를 필드로 가지고 있다가, 호출이 오면 "그건 네가 해"라며 안쪽 객체에게 떠넘김
- 합성이 더 안전하고 유연하지만, Scala 2에서는 `def scan() = scanUnit.scan()`처럼 떠넘기는 메서드(forwarding method)를 일일이 손으로 써야 했음 → 멤버가 수십 개면 그만큼 다 써야 함
- `export`는 이 지루한 위임 메서드를 컴파일러가 자동으로 만들어 줘서, 합성을 상속만큼이나 간편하게 쓰도록 해 줌

참고 — `export` 한 줄이 하는 일
- `export scanUnit.scan`: "`scanUnit`의 `scan`을 내(`Copier`) 멤버인 것처럼 쓸 수 있게 별칭을 만들어라"라는 뜻
- 그러면 바깥에서 `copier.scan()`이라고 부를 수 있고, 실제로는 `copier`가 자기 안의 `scanUnit`에게 넘김
- `import`가 "이름을 내 코드 안으로 끌어와 쓰는 것"이라면, `export`는 "내 멤버로 내보내(공개해) 남도 쓰게 하는 것" — 방향이 반대

- export 절(export clause): 객체의 선택된 멤버에 대한 별칭(alias)을 생성하여, 상속(inheritance)보다 조합(composition)을 촉진
- 장황한 위임 메서드(forwarding method)를 직접 작성하지 않고도 내부 컴포넌트의 멤버를 노출하는 간결한 구문을 제공

#### 주요 예시 (Primary Example)

공식 문서는 `Printer`와 `Scanner` 컴포넌트를 합성하는 `Copier` 클래스를 보여줌.

```scala
class Copier:
  private val printUnit = new Printer { type PrinterType = InkJet }
  private val scanUnit = new Scanner

  export scanUnit.scan
  export printUnit.{status as _, *}

  def status: List[String] = printUnit.status ++ scanUnit.status
```

이는 다음과 같은 별칭을 생성함.

- `final def scan(): BitMap = scanUnit.scan()` 같은 메서드 별칭
- 타입 별칭(type alias) 또한 자동으로 생성됨

- `export scanUnit.scan`: `scanUnit`의 `scan` 멤버를 `Copier`에 노출
- `export printUnit.{status as _, *}`: `printUnit`의 `status`를 제외(`status as _`)한 나머지 모든 멤버(`*`)를 노출
- `status`는 `Copier`가 직접 정의하므로 제외한 것

#### 셀렉터 종류 (Selector Types)

구문은 여러 셀렉터(selector) 패턴을 지원.

- 단순(Simple) `x`: `x`라는 이름의 멤버에 대해 별칭을 생성
- 이름 변경(Renaming) `x as y`: `x`를 `y`라는 이름으로 별칭을 만듦
- 생략(Omitting) `x as _`: 와일드카드에 의해 `x`가 별칭으로 만들어지는 것을 방지
- given 셀렉터(Given selector) `given x`: `x` 타입에 부합하는 given 인스턴스(given instance)에 대해 별칭을 만듦
  - `x`를 생략하면 `Any`에 부합하는 모든 given 인스턴스가 대상이 됨
- 와일드카드(Wildcard) `*`: given과 합성(synthetic) 멤버를 제외한 모든 적격(eligible) 멤버에 대해 별칭을 만듦

참고 — `as`와 `*`, 그리고 `given`
- `x as y`: "`x`를 `y`라는 새 이름으로 내보낸다"(이름 바꾸기)
- `x as _`: "`x`만은 내보내지 않는다"(제외)
- `*`: "나머지 전부"
- → `export printUnit.{status as _, *}`는 "`status` 빼고 나머지 다 내보내라"가 됨(`status`를 뺀 이유는 `Copier`가 `status`를 직접 따로 정의하기 때문)
- 한편 `given`은 자동으로 채워지는 값이라 와일드카드 `*`에 휩쓸리지 않고, 굳이 내보내려면 `given`으로 콕 집어야 함

#### 적격 요건 (Eligibility Requirements)

멤버는 다음 조건을 모두 만족할 때만 별칭 대상이 됨.

- 포함하는 클래스의 기반 클래스(base class)가 소유한 멤버가 아닐 것
- 기반 클래스의 구체적인(concrete) 정의를 오버라이드하지 않을 것
- export 위치(export location)에서 접근 가능할 것
- 생성자(constructor)나 클래스의 합성 부분(synthetic part)이 아닐 것
- given 인스턴스는 오직 given 셀렉터를 통해서만 별칭이 될 것

#### EBNF 문법 (EBNF Grammar)

레퍼런스가 명시하는 형식 구문.

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

#### 주요 제약 (Key Restrictions)

제약.

1. export 절은 클래스/객체/트레이트 안이나 최상위(top-level)에만 나타날 수 있으며, 블록 문장(block statement)으로는 쓸 수 없음
2. 와일드카드/given 셀렉터는 패키지(package)를 참조할 수 없음(증분 컴파일 안전성을 위함)
3. 이름 변경(renaming)은 그 대상 이름(target name)과 일치하는, 이름이 변경되지 않은 export를 가림(hide)
4. 이름 변경의 대상 이름들은 서로 겹치지 않아야(pairwise distinct) 함
5. `export status as stat` 같은 단순 이름 변경(simple renaming)은 현재 지원되지 않음
6. 무인자(nullary) Java 메서드의 별칭은 무인자 Scala 메서드가 되며, 괄호(`()`)를 요구함

#### 타입 멤버와 항 멤버의 처리 (Type and Term Member Handling)

- 타입 멤버(type member): 타입 정의로 별칭이 됨
- 항 멤버(term member): 메서드 정의로 별칭이 됨
- export 별칭 메서드는 `final`
  - 단, 실험적(experimental) `modularity` 임포트 하에서는 타입 별칭이 `final`이 아닌 형태가 됨

#### modularity 기능 노트 (Modularity Feature Note)

- 실험적 `modularity` 언어 임포트 하에서는, 오직 export된 메서드와 값(value)만 `final`이며, 생성되는 `PrinterType`은 `final` 수식어가 없는 단순 타입 별칭(simple type alias)이 됨

#### 확장 지원 (Extensions Support)

- export 절은 확장 블록(extension block) 내에서도 동작
- 이때 한정자(qualifier)는 같은 절 안에 있는 매개변수 없는(parameterless) 확장 메서드를 참조해야 함

#### 정교화 순서 (Elaboration Order)

- export 처리는 클래스 타입 완성(class type completion) 도중, 부모 타입이 결정된 후에 일어남
- 모든 경로(path)는 별칭으로 진입하기 전에 타입이 결정되어, 같은 클래스 내 export 사이에서 순환 참조(circular reference)가 생기는 것을 방지

#### 동기 (Motivation)

- export 절은 조합(composition)을 상속만큼이나 간결하게 표현할 수 있게 해 줌으로써, 언어 설계의 빈틈을 메움
- 멤버 이름 변경과 선택적 노출(selective exposure)도 지원 — 이는 상속이 제공하지 못하는 기능

---

### 7. 범용 apply 메서드 / 생성자 앱 (Universal Apply Methods / Creator Applications)

#### 개요 (Overview)

왜 필요한가 — `new`를 굳이 써야 했던 불편함
- Scala 2에서 `case class Point(...)`는 `Point(1, 2)`처럼 `new` 없이 만들 수 있었지만, 일반 클래스는 꼭 `new StringBuilder("abc")`라고 `new`를 붙여야 했음
- 같은 "객체 만들기"인데 클래스 종류에 따라 문법이 달랐던 것
- Scala 3는 컴파일러가 일반 클래스에도 `case class`처럼 `apply`를 자동으로 만들어 줘서, 모든 구체 클래스를 `new` 없이 동일하게 생성할 수 있게 통일함 — 일관성과 가독성을 위한 변화

참고 — `apply`가 곧 `()` 호출
- Scala에서 `객체(...)`라고 쓰면 컴파일러가 자동으로 `객체.apply(...)`로 풀어 줌(디슈가링)
- 그래서 동반 객체에 `apply`만 정의해 두면 `StringBuilder("abc")`가 `StringBuilder.apply("abc")`, 결국 `new StringBuilder("abc")`로 이어짐
- `new`가 사라진 게 아니라, 컴파일러가 자동 생성한 `apply` 뒤로 숨은 것뿐

- Scala 3는 생성자 프록시(constructor proxy) 기능을 케이스 클래스를 넘어 모든 구체 클래스(concrete class)로 확장
- 이를 통해 `new` 키워드 없이 함수 적용(function application) 구문으로 인스턴스를 생성할 수 있음

#### 기본 예시 (Basic Example)

```scala
class StringBuilder(s: String):
  def this() = this("")

StringBuilder("abc")  // old: new StringBuilder("abc")
StringBuilder()       // old: new StringBuilder()
```

#### 생성되는 동반 객체 (Generated Companion Object)

- 컴파일러는 `apply` 메서드를 가진 동반 객체를 자동으로 생성

```scala
object StringBuilder:
  inline def apply(s: String): StringBuilder = new StringBuilder(s)
  inline def apply(): StringBuilder = new StringBuilder()
```

- 각 `apply` 메서드는 대응하는 생성자와 동일하게 `inline`으로 만들어짐

#### 생성자 프록시 규칙 (Constructor Proxy Rules)

규칙 1 — 동반 객체 생성(Companion Object Creation)

- 구체 클래스 `C`에 대해, 다음 조건이 모두 성립할 때 생성자 프록시 동반 객체가 생성됨
  - 그 클래스에 기존 동반 객체가 없을 것
  - 그 스코프에 `C`라는 이름의 다른 값(value)이나 메서드가 정의되거나 상속되지 않았을 것

규칙 2 — apply 메서드 생성(Apply Method Generation)

- 다음 조건이 모두 성립할 때 생성자 프록시 `apply` 메서드가 생성됨
  - 그 클래스에 동반 객체가 있을 것(자동 생성된 것일 수도 있음)
  - 그 동반 객체가 이미 `apply` 멤버를 정의하고 있지 않을 것

- 생성되는 각 메서드는 대응하는 생성자의 타입 매개변수(type parameter), 값 매개변수(value parameter), 접근 제한(access restriction)과 일치
- 클래스가 `protected`라면, 동반 객체가 `protected`이거나 `apply` 메서드가 `protected`가 됨

#### 제약 (Restrictions)

- 생성자 프록시는 독립적인 값(standalone value)으로 사용할 수 없음 — `apply` 선택이나 암묵적 적용(implicit application)을 요구
- 생성자 프록시 식별자가 다른 스코프에서 임포트되거나 정의된 메서드(또는 `apply`를 포함하는 값)로도 해석될 때, 모호함(ambiguity) 오류 발생

#### 동기 (Motivation)

- 이 접근법은 구현 세부사항을 감추고, 코드 가독성을 높이며, 모든 구체 클래스에 걸쳐 객체 생성 구문을 표준화함으로써 언어의 규칙성(regularity)을 향상시킴

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other New Features](https://docs.scala-lang.org/scala3/reference/other-new-features/)
- [Trait Parameters](https://docs.scala-lang.org/scala3/reference/other-new-features/trait-parameters.html)
- [Transparent Traits](https://docs.scala-lang.org/scala3/reference/other-new-features/transparent-traits.html)
- [Open Classes](https://docs.scala-lang.org/scala3/reference/other-new-features/open-classes.html)
- [Opaque Type Aliases](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques.html)
- [Opaque Type Aliases: more details](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques-details.html)
- [Export Clauses](https://docs.scala-lang.org/scala3/reference/other-new-features/export.html)
- [Universal Apply Methods](https://docs.scala-lang.org/scala3/reference/other-new-features/creator-applications.html)

---

## Scala 3 기타 새 기능: 문법과 언어 편의 기능

---

### 목차

1. [선택적 중괄호 — 들여쓰기 문법(Optional Braces / Significant Indentation)](#1-선택적-중괄호--들여쓰기-문법optional-braces--significant-indentation)
2. [새 제어 문법(New Control Syntax)](#2-새-제어-문법new-control-syntax)
3. [최상위 정의(Top-Level Definitions)](#3-최상위-정의top-level-definitions)
4. [매개변수 언튜플링(Parameter Untupling)](#4-매개변수-언튜플링parameter-untupling)
5. [kind 다형성(Kind Polymorphism)](#5-kind-다형성kind-polymorphism)
6. [Matchable 트레이트(The Matchable Trait)](#6-matchable-트레이트the-matchable-trait)
7. [@targetName 애너테이션(The @targetName Annotation)](#7-targetname-애너테이션the-targetname-annotation)
8. [개선된 for(Better Fors)](#8-개선된-forbetter-fors)
9. [안전한 초기화(Safe Initialization)](#9-안전한-초기화safe-initialization)
10. [참고 자료](#참고-자료)

---

### 1. 선택적 중괄호 — 들여쓰기 문법(Optional Braces / Significant Indentation)

참고 — "선택적 중괄호"가 무슨 말인가
- Scala 2에서는 클래스·메서드·블록의 범위를 항상 중괄호 `{ ... }`로 묶었음
- Scala 3에서는 Python처럼 들여쓰기만으로도 같은 범위를 표현할 수 있게 됨
- 즉 "중괄호를 써도 되고, 안 쓰고 들여쓰기로 대신해도 된다"는 뜻이라 "선택적(optional) 중괄호"라고 부름
- 이 문서가 다루는 "새 문법"은 사실 Scala를 처음 배우는 사람이 보게 될 기본 문법이기도 함
- 옛 자료에서 본 중괄호 스타일과 요즘 자료의 들여쓰기 스타일이 둘 다 맞다고 알아두면 됨

왜 필요한가
- 중괄호가 깊게 중첩되면 "이 `}`가 어느 `{`를 닫는 거지?" 하고 헷갈리기 쉬움
- 들여쓰기는 사람이 어차피 눈으로 읽는 정보 → 이것만으로 범위를 정하면 코드가 더 간결하고 읽기 좋아짐
- 동시에 컴파일러가 들여쓰기를 검사해 "닫는 중괄호를 빠뜨렸다" 같은 흔한 실수를 잡아 줌

- Scala 3는 의미 있는 들여쓰기(significant indentation)를 도입
- 이를 통해 많은 문맥에서 중괄호(`{}`)를 생략할 수 있으며, 동시에 들여쓰기 규칙을 강제하여 흔히 발생하는 오류를 잡아냄
- 이 기능은 컴파일러 플래그 `-no-indent`로 비활성화 가능

#### 1.1 들여쓰기 규칙(Indentation Rules)

컴파일러는 두 가지 규칙을 강제하며, 위반 시 경고(warning)를 출력.

1. 중괄호로 구분된 영역 규칙(Brace-delimited region rule): 중괄호 내부에서, 여는 중괄호 뒤 새 줄의 첫 문장보다 더 왼쪽에서 시작하는 문장은 허용되지 않음 → 닫는 중괄호가 누락된 경우를 감지하는 데 도움
2. 들여쓰기 폭 규칙(Indentation width rule): 의미 있는 들여쓰기가 꺼져 있을 때, 표현식의 들여쓰기된 하위 부분이 줄바꿈으로 끝나는 경우, 그 다음 문장은 해당 하위 부분보다 더 얕은 들여쓰기로 시작해야 함 → 여는 중괄호를 누락한 경우를 방지

#### 1.2 선택적 중괄호 메커니즘(Optional Braces Mechanism)

- 컴파일러는 줄바꿈 지점에 `<indent>`와 `<outdent>` 토큰을 삽입
- 이 토큰들은 중괄호 쌍 `{...}`와 동등하게 동작하며, 들여쓰기 폭 스택(indentation width stack, `IW`)으로 관리됨

##### 들여쓰기 삽입 규칙(Indent Insertion Rules)

`<indent>` 토큰은 다음 조건이 모두 충족될 때 삽입됨.

- 현재 위치에서 들여쓰기 영역(indentation region)이 시작될 수 있고,
- 다음 줄의 첫 토큰이 현재보다 더 깊이 들여쓰기되어 있을 때

들여쓰기 영역은 다음 위치 뒤에서 시작될 수 있음.

- 확장(extension)의 선행 매개변수(leading parameters) 뒤
- given 인스턴스에서 `with` 뒤
- 템플릿 본문(template body) 시작 부분의 `:` 뒤
- 다음 토큰들 뒤: `=`, `=>`, `?=>`, `<-`, `catch`, `do`, `else`, `finally`, `for`, `if`, `match`, `return`, `then`, `throw`, `try`, `while`, `yield`
- 구식(old-style) `if`/`while` 조건의 닫는 `)` 뒤
- 구식 `for` 루프 열거자(enumerations)의 닫는 `)` 또는 `}` 뒤

##### 내어쓰기 삽입 규칙(Outdent Insertion Rules)

`<outdent>` 토큰은 다음 조건이 모두 충족될 때 삽입됨.

- 다음 줄의 첫 토큰이 현재보다 더 얕게 들여쓰기되어 있고,
- 이전 줄이 다음 연속(continuation) 토큰으로 끝나지 않을 때: `then`, `else`, `do`, `catch`, `finally`, `yield`, `match`

- 선행 중위 연산자(leading infix operators)에는 올바른 파싱 우선순위를 보장하기 위한 별도 규칙이 적용됨

#### 1.3 템플릿 본문 주위의 선택적 중괄호(Optional Braces Around Template Bodies)

참고 — "템플릿 본문"과 끝의 콜론 `:`
- 여기서 "템플릿 본문"은 `class`/`trait`/`object`의 중괄호 안에 들어가는 멤버 묶음을 가리키는 컴파일러 용어
- 중괄호를 생략할 때는 헤더 끝에 콜론 `:`을 붙이고 다음 줄부터 들여쓰면 됨
- 즉 `trait A:` 다음 줄에 들여쓴 `def f: Int`는 `trait A { def f: Int }`와 똑같은 뜻
- 이 `:`은 "여기서부터 본문이 들여쓰기로 이어진다"는 신호일 뿐, 타입을 적는 콜론과는 역할이 다름

- 템플릿 본문(클래스, 트레이트, 객체 정의)은 콜론 기반(colon-based) 문법을 사용하여 중괄호를 생략할 수 있음

```scala
trait A:
  def f: Int

class C(x: Int) extends A:
  def f = x

object O:
  def f = 3

enum Color:
  case Red, Green, Blue

new A:
  def f = 3

package p:
  def a = 1
```

- 콜론 토큰(`:`): 뒤에 들여쓰기된 내용이 따라올 때 중괄호를 대체하며, `{` `<indent>` ... `<outdent>` `}`와 동일하게 동작

#### 1.4 메서드 인자에 대한 선택적 중괄호(Optional Braces for Method Arguments)

주의 — `times(10):` 끝의 콜론에 놀라지 말 것
- 아래 `times(10):` 다음 줄에 들여쓴 두 줄은, `times(10) { println("ah"); println("ha") }`와 같은 뜻
- 즉 "메서드에 넘기는 인자(블록)를 중괄호 대신 콜론+들여쓰기로 적은 것"
- `xs.map: x =>` 형태도 마찬가지로 `xs.map { x => ... }`와 같음
- 처음 보면 "메서드 호출 뒤에 웬 콜론?" 싶지만, 블록 인자를 여는 신호라고 알면 됨

- Scala 3.3부터, 콜론은 들여쓰기된 블록으로 함수 인자(function arguments)를 도입할 수 있음

```scala
times(10):
  println("ah")
  println("ha")

credentials `++`:
  val file = Path.userHome / ".credentials"
  if file.exists
  then Seq(Credentials(file))
  else Seq()
xs.map:
  x =>
    val y = x - 1
    y * y
```

- 람다 매개변수(lambda parameters)는 콜론과 같은 줄에 위치할 수 있음

```scala
xs.map: x =>
  val y = x - 1
  y * y

xs.foldLeft(0): (x, y) =>
  x + y
```

#### 1.5 공백과 탭(Spaces vs Tabs)

- 들여쓰기 접두부(indentation prefix): 공백(space)과 탭(tab)으로 구성되며, 문자열 접두 관계(string prefix relation)에 따라 순서가 결정됨
- 공백과 탭을 혼용하면 오류를 유발하기 쉬우므로 한 파일 안에서는 권장되지 않음
- 비교 불가능한 들여쓰기 폭(예: 탭 6개 vs 공백 4개)은 컴파일 오류를 발생시킴

#### 1.6 들여쓰기와 중괄호의 상호작용(Indentation and Braces Interaction)

중괄호 영역 내에서도 들여쓰기 규칙이 다음 원칙에 따라 적용됨.

1. 여러 줄 중괄호 영역의 들여쓰기 폭은 `{` 뒤 새 줄에서 첫 토큰의 들여쓰기
2. 여러 줄 대괄호/괄호의 들여쓰기 폭
   - 대괄호/괄호가 줄을 끝맺는 경우: 다음 토큰의 들여쓰기
   - 그 외의 경우: 감싸는 영역의 들여쓰기
3. 닫는 `}`, `]`, `)`는 중첩된 영역을 닫는 데 필요한 `<outdent>` 토큰을 발생시킴

#### 1.7 case 절의 특별 처리(Case Clause Special Treatment)

match 표현식과 catch 절에는 다음과 같은 규칙이 적용됨.

- `match` 또는 `catch` 뒤에는, `case`가 `match`와 같은 들여쓰기 위치에 나타나더라도 들여쓰기 영역이 열림
- 이 영역은 해당 들여쓰기에서 `case`가 아닌 첫 토큰이 등장하거나, 더 얕게 들여쓰기된 토큰이 나타날 때 닫힘

- 이를 통해 들여쓰기되지 않은(unindented) case를 작성할 수 있음

```scala
x match
case 1 => print("I")
case 2 => print("II")
case 3 => print("III")

println(".")
```

#### 1.8 들여쓰기를 통한 문장 연속(Statement Continuation via Indentation)

두 번째 줄이 첫 번째 줄보다 더 깊이 들여쓰기되고, 다음 조건 중 하나를 만족하면 가상 세미콜론(virtual semicolon)이 억제됨.

- 두 번째 줄이 `(`, `[`, 또는 `{`로 시작하거나,
- 첫 번째 줄이 `return`으로 끝날 때

```scala
f(x + 1)
  (2, 3)        // f(x + 1)(2, 3)와 동등

g(x + 1)
(2, 3)          // g(x + 1); (2, 3)와 동등

if x < 0 then return
  a + b         // if x < 0 then return a + b와 동등
```

#### 1.9 end 마커(End Markers)

참고 — `end` 마커는 "여기서 끝"이라는 표시
- 들여쓰기 문법에는 범위를 닫는 `}`가 없으니, 긴 블록은 어디서 끝나는지 눈으로 찾기 어려움
- `end largeMethod`, `end if`처럼 적어 두면 "이 메서드/`if`가 여기서 끝난다"를 사람에게 알려 줌
- 어디까지나 사람을 위한 표시이고 생략해도 동작은 같음(선택 사항)
- 짧은 코드에는 오히려 군더더기가 됨

- 선택적인 `end` 마커는 들여쓰기 영역의 경계를 명확히 함

```scala
def largeMethod(...) =
  ...
  if ... then ...
  else
    ... // 큰 블록
  end if
  ... // 더 많은 코드
end largeMethod
```

- 지정자 토큰(specifier token): 바로 앞 문장과 대응해야 함
- 유효한 지정자는 식별자(identifier) 또는 다음 키워드: `if`, `while`, `for`, `match`, `try`, `new`, `this`, `val`, `given`, `extension`

##### end 마커를 언제 사용하는가(When to Use End Markers)

코드가 다음과 같은 경우 end 마커는 명확성을 높여줌.

- 구조물 내부에 빈 줄(blank line)이 포함된 경우
- 15~20줄을 초과하는 경우
- 깊은 들여쓰기(4단계 이상)로 끝나는 경우

- 더 짧고 단순한 코드의 경우, end 마커는 오히려 가독성을 떨어뜨릴 수 있음

#### 1.10 컴파일러 설정과 재작성(Compiler Settings and Rewrites)

- 활성화(기본값): 의미 있는 들여쓰기가 기본적으로 활성화되어 있음
- 비활성화: `-no-indent`, `-old-syntax`, 또는 `-source 3.0-migration`
- 들여쓰기 문법으로 재작성: `-rewrite -indent`
- 중괄호 문법으로 재작성: `-rewrite -no-indent`

- 구식 문법에서 들여쓰기 문법으로 변환하려면 두 단계가 필요 — 먼저 `-rewrite -new-syntax`를 실행한 뒤, `-rewrite -indent`를 실행

---

### 2. 새 제어 문법(New Control Syntax)

참고 — `then` / `do`로 괄호를 대신한다
- Java나 Scala 2에서는 `if (x < 0)`, `while (x >= 0)`처럼 조건을 괄호로 감쌌음
- Scala 3 새 문법에서는 괄호를 빼는 대신, 조건이 끝나는 곳에 키워드를 둠
  - `if`는 조건 뒤에 `then`: `if x < 0 then ...`
  - `while`은 조건 뒤에 `do`: `while x >= 0 do ...`
  - `for`는 마지막에 `yield`(값을 모음) 또는 `do`(부수효과만)
- `then`/`do`는 "조건은 여기까지, 이제부터 본문"이라는 경계 표시 역할
- 옛 괄호 문법도 그대로 쓸 수 있으니, 둘 다 눈에 익혀 둘 것

- Scala 3는 조건식 주위에 괄호를 요구하지 않는 간결한 제어 구조 문법을 도입
- 이 "조용한(quiet)" 문법은 완전한 기능을 유지하면서도 코드 가독성을 높임

#### 2.1 핵심 기능(Core Features)

새 문법은 네 가지 주요 제어 구조에 적용됨.

if 표현식(If Expressions)

```scala
if x < 0 then
  "negative"
else if x == 0 then
  "zero"
else
  "positive"

if x < 0 then -x else x
```

while 루프(While Loops)

```scala
while x >= 0 do x = f(x)
```

참고 — `for ... yield`는 "변환해서 모으기"
- Scala의 `for`는 자바의 "단순 반복문"이라기보다 컬렉션을 훑어 새 컬렉션을 만드는 도구에 가까움
- `x <- xs`: "`xs`의 각 원소를 `x`로 꺼낸다"는 뜻(`<-`를 "in"으로 읽으면 편함)
- `if x > 0`: 거른다(필터)
- `yield x * x`: 각 결과를 모아 새 컬렉션으로 돌려줌
- 값을 모으지 않고 그냥 반복만(예: 출력) 하고 싶으면 `yield` 대신 `do`를 씀
- 즉 아래 예시는 "`xs`에서 양수만 골라 제곱한 리스트"를 만듦

for 표현식(For Expressions)

```scala
for x <- xs if x > 0
yield x * x

for
  x <- xs
  y <- ys
do
  println(x + y)
```

try-catch

```scala
try body
catch case ex: IOException => handle
```

#### 2.2 문법 규칙(Syntax Rules)

공식 레퍼런스에 따르면 상세 규칙은 다음과 같음.

- `if` 표현식의 조건은 뒤에 `then`이 따라오면 감싸는 괄호 없이 작성할 수 있음
- `while` 루프의 조건은 뒤에 `do`가 따라오면 감싸는 괄호 없이 작성할 수 있음
- `for` 표현식의 열거자(enumerators)는 뒤에 `yield` 또는 `do`가 따라오면 감싸는 괄호나 중괄호 없이 작성할 수 있음
- `for` 표현식에서 `do`는 for 루프(for-loop)를 표현
- `catch` 뒤에는 같은 줄에 단일 case를 둘 수 있음 — case가 여러 개라면, (Scala 2와 마찬가지로) 중괄호 안에 있거나 들여쓰기된 블록 안에 있어야 함

#### 2.3 컴파일러 지원(Compiler Support)

- Scala 3 컴파일러는 양방향 문법 변환을 지원
- `-rewrite -new-syntax`: 기존 문법을 새 스타일로 변환하여 조건식의 괄호와 중괄호를 제거
- `-rewrite -old-syntax`: 역방향 변환으로 전통적인 Scala 2 문법을 복원

---

### 3. 최상위 정의(Top-Level Definitions)

#### 3.1 개요(Overview)

참고 — "최상위 정의"란
- Java에서는 모든 것이 클래스 안에 들어가야 했음
- Scala 2도 `val`/`def`/`type`은 보통 클래스나 `object`(또는 package object) 안에 넣어야 했음
- Scala 3에서는 이런 정의를 클래스 밖, 파일의 맨 바깥(최상위)에 바로 적을 수 있음
- 그래서 작은 유틸리티 함수나 상수를 굳이 객체로 감싸지 않아도 됨

왜 필요한가
- 과거에는 패키지 전체에서 공유할 함수·상수를 모으려고 package object라는 특별한 파일을 따로 만들었음
- 최상위 정의가 생기면서 그 번거로움이 사라졌고, package object는 거의 쓸 일이 없어짐
- 컴파일러는 내부적으로 이것들을 보이지 않는 래퍼 객체로 감싸 처리하지만(아래 3.3), 사용하는 입장에서는 신경 쓸 필요가 없음

- Scala 3는 다양한 종류의 정의를 래퍼 객체 없이 패키지 수준에서 직접 작성할 수 있게 함
- 공식 문서: "이제 모든 종류의 정의를 최상위(top-level)에서 작성할 수 있습니다"

#### 3.2 사용 예시(Example Usage)

```scala
package p
type Labelled[T] = (String, T)
val a: Labelled[Int] = ("count", 1)
def b = a._2

case class C()

extension (x: C) def pair(y: C) = (x, y)
```

- 이전에는 타입, 값, 메서드 정의를 패키지 객체(package object) 안에 넣어야 했음
- 이제는 여러 소스 파일에 이러한 정의를 자유롭게 배치할 수 있으며, 클래스나 객체와 섞어 쓸 수 있음

#### 3.3 컴파일러 동작(Compiler Behavior)

컴파일러는 다음 범주의 최상위 정의에 대해 합성 래퍼 객체(synthetic wrapper object)를 생성함.

- 패턴(pattern), 값, 메서드, 타입 정의
- 암시적 클래스(implicit class)와 객체
- 불투명 타입 별칭(opaque type alias)의 동반 객체(companion object)

- `src.scala` 파일의 경우 이러한 정의들은 `src$package`라는 합성 객체 안에 감싸짐
- 이 래핑은 패키지를 통해 해당 정의에 접근하는 호출자에게는 투명하게 동작함

#### 3.4 중요 참고 사항(Important Notes)

1. 이진 호환성(Binary Compatibility): 소스 파일 이름은 이진 호환성에 영향을 미침 — 파일 이름을 바꾸면 생성되는 객체의 이름이 바뀜

참고 — 프로그램 시작점과 `@main`
- 자바에서는 `public static void main(String[] args)`가 프로그램의 시작점이었음
- Scala 3에서는 아무 함수 위에 `@main`만 붙이면 그 함수가 실행 진입점이 됨
  - 예: `@main def run() = println("hi")` → 명령행에서 `run`으로 실행됨
- 아래에서 말하는 "최상위 `main` 메서드"는 `@main`이 아니라 옛 방식대로 `main`이라는 이름으로 직접 정의한 경우를 가리킴

2. main 메서드(Main Methods): 최상위 `main` 메서드도 다른 정의와 마찬가지로 래핑됨
   - `src.scala`의 경우 실행하려면 `scala src$package`가 필요
   - 공식 문서는 main 메서드를 명시적으로 이름 붙인 객체 안에 두도록 권장
3. private 범위(Privacy Scope): `private` 접근 제어는 래핑과 독립적으로 유지됨 — `private` 최상위 정의의 가시성은 해당 패키지 전체로 제한됨
4. 오버로딩 변형(Overloaded Variants): 같은 이름을 가진 오버로딩 정의는 모두 동일한 소스 파일에서 정의되어야 함

---

### 4. 매개변수 언튜플링(Parameter Untupling)

#### 4.1 개요(Overview)

참고 — 튜플과 "언튜플링"
- 튜플(tuple): `(1, "a")`처럼 여러 값을 괄호로 묶어 한 덩어리로 만든 것
- `List[(Int, Int)]`: "(정수, 정수) 쌍들의 리스트"라는 뜻
- 이런 리스트를 `map`으로 다룰 때, 원래는 쌍을 한 덩어리(`p`)로 받아 `p._1`, `p._2`로 꺼내거나 `case (x, y) =>`로 분해해야 했음
- 언튜플링: 컴파일러가 그 쌍을 알아서 `x`, `y` 두 개로 풀어 주는 것 → `(x, y) => x + y`라고 바로 쓸 수 있음
  - ("튜플로 묶인 것을 푼다"고 해서 un-tupling)

- 매개변수 언튜플링(parameter untupling): 여러 매개변수를 가진 함수 값이 튜플 인자를 받을 때 그 요소들을 개별 매개변수로 자동 분해(decompose)해 주는 Scala 3 기능

#### 4.2 기본 예시(Basic Example)

쌍(pair)의 리스트를 다룰 때, 이제 다음과 같이 작성할 수 있음.

```scala
val xs: List[(Int, Int)]
xs.map {
  (x, y) => x + y
}
```

이전의 패턴 매칭(pattern-matching) 방식을 사용하는 대신.

```scala
xs map {
  case (x, y) => x + y
}
```

#### 4.3 동작 방식(How It Works)

- 기대 타입(expected type)이 맞다면, `n > 1`개의 매개변수를 가진 함수 값은 `((T_1, ..., T_n)) => U` 형태의 함수 타입으로 변환됨
- 이때 튜플 매개변수가 분해되어 각 요소가 내부 함수로 직접 전달됨

#### 4.4 동등한 형태들(Equivalent Forms)

다음 세 가지 접근은 모두 동등함.

```scala
xs.map { (x, y) => x + y }
xs.map(_ + _)

def combine(i: Int, j: Int) = i + j
xs.map(combine)
```

#### 4.5 중요한 제한(Important Limitation)

- 이 변환은 함수 값이 직접 제공되는 경우에만 적용됨. 다음은 동작하지 않음.

```scala
val combiner: (Int, Int) => Int = _ + _
xs.map(combiner)     // 타입 불일치(Type Mismatch)
```

- 대신, 함수를 명시적으로 튜플화(tuple)해야 함.

```scala
xs.map(combiner.tupled)
```

#### 4.6 암시적 변환(Implicit Conversions)

- 권장되지는 않지만, 암시적 변환(implicit conversion)으로 유사한 동작을 구현할 수 있음
- "매개변수 언튜플링은 변환이 적용되기 전에 먼저 시도되므로, 스코프 내의 변환이 언튜플링을 가로채지 못합니다"

---

### 5. kind 다형성(Kind Polymorphism)

#### 5.1 개요(Overview)

참고 — "kind(종류)"는 타입의 타입
- 값에 타입이 있듯이, 타입에도 "종류(kind)"가 있음
- `Int`, `String`처럼 그 자체로 완성된 타입은 한 종류
- `List`처럼 `List[Int]`가 되어야 비로소 완성되는 "타입을 받아 타입을 만드는 것"은 또 다른 종류(고차 타입)
- 보통 함수는 한 종류만 받을 수 있는데, kind 다형성은 종류가 다른 타입들을 한꺼번에 받을 수 있게 해 주는 고급 기능
- 매우 드물게 쓰이는 주제라, 처음엔 "그런 게 있구나" 하고 넘어가도 됨

- Scala에서 타입 매개변수는 kind에 따라 분류됨
- 일급 타입(first-level types, `Any`의 서브타입): 값을 위한 일반 타입
- 고차 kind 타입(higher-kinded types): `List`나 `Map` 같은 타입 생성자(type constructor)

#### 5.2 단일 kind의 문제(The Problem with Single Kinds)

- 전통적으로 타입 매개변수는 특정 kind의 인자만 받을 수 있음
- 이 제약으로 인해 서로 다른 kind에 걸쳐 동작하는 제네릭 코드를 작성하기 어려움
- 예: 일반 타입과 타입 생성자를 모두 처리하는 단일 암시적 값을 정의할 수 없음

#### 5.3 해결책: AnyKind(The Solution: AnyKind)

- Scala 3는 kind 다형성(kind polymorphism)을 가능하게 하는 특수 타입 `scala.AnyKind`를 도입
- `AnyKind`를 상한(upper bound)으로 지정하면 타입 매개변수가 임의 kind(any-kinded)를 받을 수 있게 됨

```scala
def f[T <: AnyKind] = ...
```

이를 통해 임의 kind의 타입 인자를 받을 수 있음.

```scala
f[Int]           // 일반 타입
f[List]          // 일급 타입 생성자(first-order type constructor)
f[Map]           // 다중 매개변수 타입 생성자(multi-parameter type constructor)
f[[X] =>> String]  // 고차 kind 타입(higher-kinded type)
```

#### 5.4 임의 kind 타입의 제약(Restrictions on Any-Kinded Types)

실제 kind가 컴파일 타임에 확정되지 않으므로, 임의 kind 타입에는 엄격한 제한이 따름.

- 값 타입(value type)으로 사용할 수 없음
- 타입 매개변수로 인스턴스화할 수 없음
- 기본적으로 다른 임의 kind 타입 매개변수에 전달하는 용도로만 사용 가능

- 이러한 제약에도 불구하고 고급 암시(advanced implicit) 기법을 통해 정교한 일반화를 구현할 수 있음

#### 5.5 기술적 특성(Technical Characteristics)

- `AnyKind`는 멤버와 부모 클래스가 없는 합성 추상 final 클래스(synthesized abstract final class)로, Scala 타입 시스템에서 독특한 위치를 차지함
- kind와 무관하게 모든 타입의 상위 타입(supertype) 역할을 함
- 모든 타입과 kind 호환성(kind-compatibility)을 가짐
- 고차 kind로 취급되어(값으로 사용 불가) 동시에 타입 매개변수를 갖지 않음

#### 5.6 상태(Status)

- "이 기능은 이제 안정적(stable)입니다. 컴파일러 플래그 `-Yno-kind-polymorphism`은 3.7.0 기준으로 폐기 예정(deprecated)이며, 아무런 효과가 없고(무시됨), 향후 버전에서 제거될 것입니다."

---

### 6. Matchable 트레이트(The Matchable Trait)

#### 6.1 개요(Overview)

- Scala 3 표준 라이브러리는 패턴 매칭 가능 여부를 제어하기 위한 트레이트 `Matchable`을 도입
- 이 기능은 추상 타입(abstract type)과 불투명 타입(opaque type)에서 발생할 수 있는 추상화 위반 문제를 해결

참고 — `Matchable`이 푸는 문제 한 줄 요약
- 패턴 매칭(`match`/`case`)으로 "이 값이 사실은 무슨 타입인가"를 들춰볼 수 있음
- 이걸 무제한 허용하면 일부러 숨겨 둔 타입(opaque type 등)의 속을 들여다보고 규칙을 깰 수 있음
- `Matchable`: "패턴 매칭으로 들춰봐도 괜찮은 타입"에만 붙는 표식 역할을 해서, 그런 우회로를 막음
- 처음에는 "패턴 매칭을 안전하게 제한하려고 생긴 표식 트레이트" 정도로만 이해해도 충분함

#### 6.2 문제(The Problem)

- 문제의 핵심: 불변 배열 타입(immutable array type)인 `IArray`

참고 — 와일드카드 타입 `?` (Scala 2의 `_`)
- `Array[? <: T]`의 `?`: "구체적으로 무슨 타입인지는 모르지만 `T`의 하위 타입인 어떤 것"을 뜻하는 와일드카드 타입
- Scala 2에서는 이 자리에 밑줄을 써서 `Array[_ <: T]`라고 적었음
- Scala 3는 `_`를 다른 용도(예: `F[_]` 고차 타입 자리)와 헷갈리지 않도록, 와일드카드 타입 기호를 `?`로 바꾸고 있음
- "물음표 = 아직 정해지지 않은 어떤 타입"이라고 기억하면 됨

```scala
opaque type IArray[+T] = Array[? <: T]
```

주의 — 여기서 "안전성"은 보안이 아니라 "추상화가 깨지지 않음"
- 원문의 security/safety는 해킹 같은 보안이 아니라, "타입이 약속한 규칙(여기서는 '읽기 전용 배열은 못 고친다')이 우회로로 무너지지 않음"을 뜻함
- 즉 `IArray`는 못 고치도록 만든 배열인데, 패턴 매칭으로 정체를 들춰 `Array`로 보면 몰래 고칠 수 있게 되는 허점을 막자는 이야기

- `IArray`는 `length`와 `apply` 같은 안전한 연산을 위한 확장 메서드를 제공하지만, 변형(mutation)을 방지하기 위해 `update` 메서드는 제공하지 않음
- 그러나 패턴 매칭이 이 추상화를 깨뜨리는 허점(loophole)이 됨

```scala
val imm: IArray[Int] = ...
imm match
  case a: Array[Int] => a(0) = 1
```

- 런타임에 이 패턴 매칭은 성공함
- `IArray` 값이 내부적으로 `Array` 인스턴스로 표현되기 때문에, 불변 외관(immutable facade)에도 불구하고 허가되지 않은 변형이 가능해짐

- 이 취약점은 불투명 타입을 넘어 무경계 타입 매개변수(unbounded type parameter)와 추상 타입에까지 이어짐. 예:

```scala
def f[T](x: T) = x match
  case a: Array[Int] => a(0) = 0
f(imm)
```

#### 6.3 해결책(The Solution)

- Scala 3는 패턴 매칭을 제어하기 위해 `scala.Matchable`을 도입
- 이제 생성자 패턴(constructor pattern)과 타입 패턴(type pattern)은 선택자 타입(selector type)이 `Matchable`을 따라야(conform) 함
- 이를 따르지 않으면 다음과 같은 경고가 발생

"pattern selector should be an instance of Matchable, but it has unmatchable type IArray[Int] instead"
(패턴 선택자는 Matchable의 인스턴스여야 하지만, 대신 매칭 불가능한 타입 IArray[Int]입니다)

- 이 경고는 Scala 2/3 마이그레이션 및 교차 컴파일(cross-compilation)을 지원하기 위해 `-source future-migration` 이상에서 활성화됨

##### 새로운 타입 계층(New Type Hierarchy)

- `Matchable`은 `Any`를 부모로 갖는 보편 트레이트(universal trait)로, `AnyVal`과 `AnyRef` 모두에 의해 확장됨

```scala
abstract class Any:
  def getClass
  def isInstanceOf
  def asInstanceOf
  def ==
  def !=
  def ##
  def equals
  def hashCode
  def toString

trait Matchable extends Any

class AnyVal extends Any, Matchable
class Object extends Any, Matchable
```

- 현재 `Matchable`은 마커 트레이트(marker trait)이지만, `getClass`와 `isInstanceOf`가 패턴 매칭과 밀접하게 관련되어 있어 향후 이들을 포함할 수 있음

##### 문제가 되는 타입들(Problematic Types)

다음 타입들에 대한 패턴 매칭은 경고를 발생시킴.

- 타입 `Any`: 매칭 가능한 선택자를 위해 대신 `Matchable`을 사용
- 무경계 타입 매개변수와 추상 타입: 상한(upper bound)으로 `Matchable`을 추가
- 보편 트레이트(universal trait)로만 경계가 지어진 타입 매개변수: 마찬가지로 `Matchable` 경계가 필요

#### 6.4 Matchable과 보편 동등성(Matchable and Universal Equality)

- `equals` 메서드는 이 조정이 필요한 대표적인 예 — `Any` 매개변수를 `Matchable`로 캐스트한 뒤 패턴 매칭을 수행해야 함

```scala
class C(val x: String):

  override def equals(that: Any): Boolean =
    that.asInstanceOf[Matchable] match
      case that: C => this.x == that.x
      case _ => false
```

- 이 캐스트는 "추상 타입과 불투명 타입이 존재하는 상황에서 보편 동등성(universal equality)은 안전하지 않으며, 타입의 의미(meaning)와 표현(representation)을 제대로 구별할 수 없다"는 점을 명시적으로 드러냄
- `Any`와 `Matchable`은 모두 `Object`로 소거(erase)되므로, 이 캐스트는 런타임에 항상 성공함

#### 6.5 예시: 불투명 타입 동등성 문제(Example: Opaque Type Equality Problem)

다음을 고려.

```scala
opaque type Meter = Double
def Meter(x: Double): Meter = x

opaque type Second = Double
def Second(x: Double): Second = x
```

- 보편 `equals`는 수학적으로 거짓임에도 `Meter(10).equals(Second(10))`에 대해 잘못되게 true를 반환함
- `import scala.language.strictEquality`를 통해 다중 동등성(multiversal equality)을 활성화하면 `Meter(10) == Second(10)`를 컴파일 타임 오류로 전환하여 이러한 문제를 방지할 수 있음

---

### 7. @targetName 애너테이션(The @targetName Annotation)

#### 7.1 개요(Overview)

참고 — `@targetName`은 "바이트코드용 이름표"
- Scala에서는 `++=`, `<*>` 같은 기호로 된 메서드 이름을 쓸 수 있음
- 그런데 JVM 바이트코드나 자바에서는 이런 기호 이름을 그대로 부르기 어려움
- `@targetName("append")`를 붙이면 "Scala 코드에서는 `++=`로 쓰되, 실제 내부 이름은 `append`로 만들어 달라"는 뜻이 됨
- Scala로 코딩하는 입장에서는 평소엔 신경 쓸 일이 없고, 자바와 섞어 쓰거나 기호 이름을 쓸 때 등장하는 보조 장치

- `@targetName` 애너테이션: 정의에 대한 대체 구현 이름(alternate implementation name)을 지정
- 생성되는 바이트코드와 다른 언어에서의 호출 방식에 영향을 미치지만, Scala 코드에서의 사용에는 영향을 주지 않음

#### 7.2 기본 예시(Basic Example)

```scala
import scala.annotation.targetName

object VecOps:
  extension [T](xs: Vec[T])
    @targetName("append")
    def ++= [T] (ys: Vec[T]): Vec[T] = ...
```

- `++=` 연산은 `append`라는 이름으로 구현됨. Java 코드에서는 다음과 같이 호출할 수 있음.

```java
VecOps.append(vec1, vec2)
```

- Scala 코드에서는 `append`가 아닌 `++=`를 그대로 사용해야 함

#### 7.3 핵심 세부 사항(Key Details)

정의와 범위

- `@targetName`은 `scala.annotation` 패키지에 위치
- 단일 `String` 인자(외부 이름)를 받음
- 최상위 클래스, 트레이트, 객체를 제외한 모든 정의에 적용할 수 있음

요구 사항

- 외부 이름은 호스트 플랫폼에서 유효한 이름이어야 함
- 기호 이름(symbolic name)을 가진 정의에는 이 애너테이션을 붙이도록 권장됨
- 백틱으로 감싼 이름 중 플랫폼에서 유효하지 않은 것에도 이 애너테이션을 붙여야 함

#### 7.4 오버라이딩과의 관계(Relationship with Overriding)

- 두 메서드 정의는 이름, 시그니처, 소거된 이름(erased name)을 공유할 때 일치(match)함
- 소거된 이름은 `@targetName`이 지정된 경우 대상 이름(target name)이고, 지정되지 않은 경우 정의된 이름

##### 충돌하는 메서드의 모호성 해소(Disambiguating Clashing Methods)

```scala
@targetName("f_string")
def f(x: => String): Int = x.length
def f(x: => Int): Int = x + 1  // OK
```

- 두 메서드 모두 소거된 매개변수 타입이 `Function0`이어서, 애너테이션 없이는 충돌(clash) 발생
- 대상 이름이 이를 해소함

##### 오버라이딩 제약(Overriding Constraints)

- "두 멤버는 이름과 시그니처가 동일하고, 같은 소거된 이름을 공유하거나 동일한 타입을 가질 때 서로를 오버라이드할 수 있습니다. 오버라이드 관계가 성립하면 소거된 이름과 타입이 모두 일치해야 합니다."
- 이는 오버라이딩 관계가 깨지는 것을 방지함

```scala
class A:
  def f(): Int = 1
class B extends A:
  @targetName("g") def f(): Int = 2  // 오류: 오버라이딩과 충돌
```

- 컴파일러는 이를 통해 상속 계층에서 `@targetName`으로 인한 의도치 않은 이름 충돌을 방지함

---

### 8. 개선된 for(Better Fors)

#### 8.1 개요(Overview)

- Scala 3.8부터(또는 `-preview` 모드에서 3.7부터) for 내포(for-comprehensions)의 사용성이 개선됨
- 특히 별칭(alias)으로 시작할 수 있게 된 점이 주요 변화

#### 8.2 핵심 기능: for 내포에서의 별칭(Aliases in For-Comprehensions)

참고 — "디슈가링"과 for의 정체
- Scala의 `for ... yield`는 사실 그 자체로 특별한 문법이 아니라, 컴파일러가 속으로 `map`/`flatMap`/`withFilter` 호출로 바꿔치기함
- 이렇게 편한 문법을 더 기본적인 형태로 풀어내는 과정을 디슈가링(desugaring)이라고 함(설탕을 벗긴다는 비유)
- 이 절은 그 변환 과정을 더 매끈하게 다듬어 불필요한 중간 단계를 줄였다는 이야기
- 사용자 입장에서 가장 눈에 띄는 변화는, 아래처럼 `for` 맨 앞에서 `as = ...`로 값에 이름을 붙이며 시작할 수 있다는 점

- 사용자 관점에서 가장 눈에 띄는 개선: 값 별칭(value alias)으로 for 내포를 시작할 수 있게 된 것

```scala
for
  as = List(1, 2, 3)
  bs = List(4, 5, 6)
  a <- as
  b <- bs
yield a + b
```

- 이는 for 내포 앞에 명시적으로 `val` 선언을 두는 것과 동일하게 디슈가링(desugar)됨

#### 8.3 디슈가링 개선(Desugaring Improvements)

##### 1. 순수 별칭에 대한 단순화된 디슈가링(Simplified Desugaring for Pure Aliases)

- 별칭에 가드 조건(guard condition)이 없을 때 디슈가링이 더 단순해짐. 이전에는 다음 코드가:

```scala
for {
  a <- doSth(arg)
  b = a
} yield a + b
```

튜플 래핑을 동반한 중첩 `map` 호출로 디슈가링되었지만, 이제는 더 직접적인 코드를 생성함.

```scala
doSth(arg).map { a =>
  val b = a
  a + b
}
```

- 이를 통해 불필요한 중간 튜플링과 매핑 연산이 제거됨

##### 2. 중복된 map 호출 제거(Eliminating Redundant Map Calls)

- 최종 `yield` 표현식이 마지막 생성자 패턴(단일 변수이거나 변수들의 튜플)과 구문적으로 일치하면, 컴파일러는 불필요한 `map` 호출을 생략함

```scala
for {
  a <- List(1, 2, 3)
} yield a
```

- `List(1, 2, 3).map(a => a)` 대신 `List(1, 2, 3)`를 직접 생성

#### 8.4 기술적 세부 사항(Technical Details)

- 디슈가링 로직은 컴파일러의 `Desugar.scala#makeFor`에 구현되어 있으며, for 내포 문법을 컬렉션 타입의 메서드 호출로 변환하는 작업을 담당함

---

### 9. 안전한 초기화(Safe Initialization)

#### 9.1 개요(Overview)

참고 — "초기화 순서" 문제가 뭔가
- 객체를 만들 때 필드(멤버 값)들은 위에서부터 차례로 채워짐
- 그런데 어떤 필드를 채우는 도중에 아직 채워지지 않은 다른 필드를 읽어 버리면, 거기엔 `null`이나 0 같은 엉뚱한 값이 들어 있어 버그가 생김
- 이 절의 "안전 초기화 검사기"는 그런 "아직 준비 안 된 값을 미리 쓰는" 위험한 코드를 컴파일 시점에 미리 경고해 주는 기능
- 이 절은 그 검사기의 내부 이론을 다루므로, 처음엔 "초기화 순서 실수를 잡아 주는 실험적 검사기" 정도로 알고 넘어가도 됨

- Scala 3는 `-Wsafe-init` 컴파일러 옵션으로 활성화되는 실험적 안전 초기화 검사기(safe initialization checker)를 제공
- 이 기능의 이론적 기반은 논문 _Safe object initialization, abstractly_에 설명되어 있음

#### 9.2 핵심 개념(Core Concept)

이 시스템은 객체 초기화 상태를 추적하기 위해 세 가지 추상화를 사용함.

- Cold(차가움): 초기화되지 않은 필드를 포함할 수 있는 객체
- Warm(따뜻함): 모든 필드가 초기화되었지만, cold 객체에 도달할 가능성이 있는 객체
- Hot(뜨거움): 추이적으로 초기화된(transitively initialized) 객체로, warm 객체에만 도달함

- 검사기는 초기화 중인 현재 객체를 나타내는 `ThisRef`와, 외부 참조(outer references) 및 생성자 인자에 대한 추가 메타데이터를 가진 warm 객체를 추적하는 `Warm[C]`도 도입함

#### 9.3 설계 목표(Design Goals)

구현은 여섯 가지 주요 목표를 지향함.

1. 건전성(Soundness): 검사는 항상 종료되며, 일반적이고 합리적인 사용에 대해 건전(sound)함
2. 표현력(Expressiveness): 표준적인 초기화 패턴을 지원
3. 사용자 친화성(User-friendliness): 최소한의 구문적 오버헤드와 유익한 오류 메시지를 제공
4. 모듈성(Modularity): 분석을 프로젝트 경계 내로 한정
5. 성능(Performance): 컴파일 중 즉각적인 피드백을 제공
6. 단순성(Simplicity): 핵심 타입 시스템을 수정하지 않음

#### 9.4 근본 원칙(Fundamental Principles)

- 적층성(Stackability): 모든 클래스 필드가 클래스 본문의 끝에서 초기화되어야 함을 요구. 단, 예외(exception) 같은 제어 효과(control effects)는 이를 방해할 수 있음
- 단조성(Monotonicity): 초기화 상태가 역행하지 않음을 보장 → 필드가 한 번 초기화되면 그 상태가 유지됨. 이미 초기화된 참조를 통해 초기화되지 않은 객체를 노출하는 재할당(reassignment)은 금지됨
- 범위성(Scopability): 초기화 프로토콜을 우회하는 측면 채널(side channels)이 없음을 보장. 정적 필드(static fields)와 특정 제어 효과는 이 속성을 위반할 수 있으므로, 전달되는 값이 추이적으로 초기화되도록 강제할 필요가 있음
- 권한(Authority): 단조성을 보완하여, 초기화 상태가 진행될 수 있는 위치를 제한 → 필수 초기화자(mandatory initializers)가 있는 클래스 본문이나 지역 추론 지점(local reasoning points)에서만 허용됨

#### 9.5 동작 규칙(Operational Rules)

검사기는 아홉 가지 핵심 규칙을 적용함.

1. cold 값은 필드 접근이나 메서드 호출을 통해 사용할 수 없음
2. ThisRef는 초기화되지 않은 필드에 접근할 수 없음
3. 재할당의 우변(right-hand side)은 실질적으로 hot(effectively hot)이어야 함
4. 메서드 인자는 실질적으로 hot이어야 함(생성자와 parametric 메서드에 대한 특정 예외 있음)
5. 실질적으로 hot인 인자를 받는 hot 값은 hot 결과를 생성함
6. ThisRef와 warm 값에 대한 메서드 호출은 정적 해석(static resolution)을 거침
7. 실질적으로 hot인 인자를 가진 `new` 표현식은 hot 결과를 생성함
8. hot이 아닌 인자를 가진 `new` 표현식은 warm 결과를 생성하며, 재분석(re-analysis)을 유발함
9. 패턴 매치 대상(scrutinee), 반환값, throw 표현식은 실질적으로 hot이어야 함

#### 9.6 실용 예시(Practical Examples)

- 검사기는 부모-자식 상호작용 문제, 예를 들어 초기화되지 않은 자식 필드를 참조하는 오버라이드 메서드에 접근하는 경우를 감지함

```scala
abstract class AbstractFile:
  def name: String
  val extension: String = name.substring(4)

class RemoteFile(url: String) extends AbstractFile:
  val localFile: String = s"${url.##}.tmp"
  def name: String = localFile
```

- `extension`의 초기화 중 `name`을 호출하고, 이것이 아직 초기화되지 않은 `localFile`에 접근하므로 경고가 발생함

- 내부-외부 상호작용(inner-outer interactions)도 마찬가지로 검사됨. 시스템은 중첩 클래스 생성 시 외부 필드(outer fields)에 초기화 이전에 접근하는 경우를 감지함

```scala
object Trees:
  class ValDef { counter += 1 }
  class EmptyValDef extends ValDef
  val theEmptyValDef = new EmptyValDef
  private var counter = 0
```

- 함수 캡처(function captures)는 캡처된 참조를 통해 초기화되지 않은 필드에 접근하지 않도록 분석됨

#### 9.7 실질적 hot 상태(Effective Hotness)

다음 조건 중 하나를 만족하면 "실질적으로 hot(effectively hot)"으로 간주됨.

- 명시적으로 hot이거나,
- 모든 필드가 할당된 ThisRef이거나,
- 내부 클래스(inner classes)가 없고 모든 필드가 실질적으로 hot인 Warm 값이거나,
- 반환 값이 실질적으로 hot인 함수일 때

- 검사기는 가능한 경우 hot이 아닌 값을 실질적으로 hot 상태로 승격(promote)하려고 시도함

#### 9.8 모듈성과 제약(Modularity and Limitations)

- 분석은 주 생성자(primary constructors)를 진입점으로 취급하며, TASTy를 사용하여 프로젝트 경계를 넘어 상위 클래스 생성자(superclass constructors)를 추적함
- 객체 지향 상속에 내재한 클래스 간 결합(coupling) 때문에 경계를 넘나드는 추적이 건전성을 위해 필요함

- 이 구현은 두 가지 제약을 인정함
  - "이 시스템은 Java나 Scala 2 클래스를 확장할 때 안전성을 보장할 수 없습니다."
  - "전역 객체(global objects)의 안전한 초기화는 부분적으로만 검사됩니다."

#### 9.9 억제 메커니즘(Suppression Mechanisms)

- `@unchecked` 애너테이션을 사용하거나 필드를 `lazy`로 선언하면 경고를 억제할 수 있음
- 단, 이는 안전하지 않은 패턴을 승인하는 것이 아니라 임시방편(workaround)에 해당함

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other New Features](https://docs.scala-lang.org/scala3/reference/other-new-features/)
