# Scala 3 컨텍스트 추상화: given/using

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Contextual Abstractions" 섹션(given/using)을 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/contextual/

---

## 목차

1. [컨텍스트 추상화 개요](#1-컨텍스트-추상화-개요)
2. [주어진 인스턴스 (Given Instances)](#2-주어진-인스턴스-given-instances)
3. [using 절 (Using Clauses)](#3-using-절-using-clauses)
4. [컨텍스트 바운드 (Context Bounds)](#4-컨텍스트-바운드-context-bounds)
5. [given 임포트 (Given Imports)](#5-given-임포트-given-imports)
6. [이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)](#6-이름에-의한-컨텍스트-매개변수-by-name-context-parameters)
7. [Scala 2 implicit와의 관계](#7-scala-2-implicit와의-관계)
8. [참고 자료](#참고-자료)

---

## 1. 컨텍스트 추상화 개요

컨텍스트 추상화(Contextual Abstraction)는 항(term)을 암시적으로 추론(implicit term inference)하는 메커니즘으로, Scala 언어가 제공하는 가장 강력하면서도 가장 논쟁적인 기능 중 하나입니다. Scala 3는 이 컨텍스트 추상화를 처리하는 방식을 전면적으로 재설계했습니다.

### 1.1 기존 implicit 시스템에 대한 비판

Scala 3 문서는 Scala의 기존 implicit 설계에 대해 다섯 가지 주요 비판점을 제시합니다.

**(1) 과도한 사용과 오용 (Overuse and Misuse)**
`implicit` 수식어(modifier)가 너무 많은 언어 구문에 적용되어, 암시적 값(implicit value)과 암시적 변환(implicit conversion)을 혼동하기 쉽습니다. 다음 두 정의는 구문적으로 매우 유사하지만 근본적으로 다른 목적을 가집니다.

```scala
implicit def i1(implicit x: T): C[T]
implicit def i2(x: T): C[T]
```

`i1`은 암시적 매개변수를 받아 `C[T]`를 합성하는 정의이고, `i2`는 `T`를 `C[T]`로 변환하는 암시적 변환입니다. 그러나 이 둘을 구별할 명확한 표시가 없습니다.

**(2) 숨겨진 의존성 (Hidden Dependencies)**
implicit은 임포트(import) 내 어디에나 등장할 수 있어서, 프로그램이 실제로 어떤 implicit을 사용하는지 추적하기 어렵습니다. 이로 인해 "올바른 임포트 문을 추가하면 신비롭게 해결되는" 혼란스러운 타입 오류가 자주 발생합니다.

**(3) 간접적인 의도 표현 (Indirect Intent Signaling)**
`implicit` 수식어는 의도(intent)가 아니라 메커니즘(mechanism)을 전달합니다. 입문자는 그 기저에 깔린 디슈가링(desugaring) 규칙을 이해하지 못하면 서로 다른 implicit 사용 사례를 구별하기 어렵습니다.

**(4) 매개변수 구문의 한계 (Parameter Syntax Limitations)**
암시적 매개변수(implicit parameter)는 인자(argument) 전달 구문에서 대칭성이 부족합니다. 예를 들어 다음과 같은 메서드는

```scala
def currentMap(implicit ctx: Context): Map[String, Int]
```

암시적 매개변수 구역(section) 뒤에 일반 인자를 받을 수 없습니다. 이 때문에 `.apply()` 호출 같은 어색한 우회 방법을 강제하게 됩니다. 또한 메서드는 단 하나의 암시적 매개변수 구역만 가질 수 있으며 그것은 반드시 마지막에 위치해야 합니다.

**(5) 도구 지원의 어려움 (Tooling Challenges)**
컨텍스트에 의존하는 implicit은 IDE의 자동 완성, Scaladoc 문서 생성, 오류 진단을 복잡하게 만듭니다. 실패한 implicit 검색은 종종 불명확한 오류 메시지를 생성합니다.

### 1.2 재설계된 접근법

이러한 문제를 해결하기 위해 Scala 3는 네 가지 근본적인 변화를 도입했습니다.

- **주어진 인스턴스 (Given Instances)**: 암시적 정의(implicit definition)를 전용 구문으로 대체합니다. 여러 언어 구문에 `implicit`을 흩뿌리는 대신, 합성 가능한(synthesizable) 항을 정의하는 단일 메커니즘을 제공합니다.
- **using 절 (Using Clauses)**: 암시적 매개변수와 인자를 위한 명시적 구문을 제공합니다. 어떤 매개변수가 추론된 값을 받는지에 대한 모호함을 제거하고, 여러 개의 매개변수 구역을 허용합니다.
- **given 임포트 (Given Imports)**: 주어진 인스턴스만을 위한 전용 임포트 범주를 만들어, 일반 임포트 목록 안에 숨어 들어가는 것을 방지합니다.
- **명시적 변환 클래스 (Explicit Conversion Class)**: 암시적 변환을 표준 `Conversion` 클래스로 통합하여, 규율 없는(undisciplined) 변환 구문을 제거합니다.

### 1.3 관련 기능

이 섹션은 다음 기능들도 다룹니다.

- 컨텍스트 바운드(context bound) — 이전 버전과 동일하게 유지
- 확장 메서드(extension method) — 암시적 클래스(implicit class)를 대체
- 타입 클래스(type class) 구현 패턴
- 대수적 데이터 타입(ADT)에 대한 타입 클래스 도출(type class derivation)
- 다중우주 동등성(multiversal equality)
- 컨텍스트 함수(context function)
- 이름에 의한 컨텍스트 매개변수(by-name context parameter)
- Scala 2 implicit으로부터의 마이그레이션 안내

### 1.4 설계 철학

이 재설계는 관심사의 분리(separation of concerns)를 강조합니다. given 정의를 위한 별도 구문, 매개변수를 위한 using 절, 범위 관리를 위한 given 임포트, 그리고 암시적 변환을 위한 변환 클래스를 각각 명확히 구분합니다. 이 접근법은 기능 간 상호작용을 줄이고, 일관성을 향상시키며, 숙련된 개발자와 입문자 모두에게 더 효과적으로 작동하는 직교적(orthogonal)인 언어 구조를 만드는 것을 목표로 합니다.

---

## 2. 주어진 인스턴스 (Given Instances)

### 2.1 개요

주어진 인스턴스(given instance), 줄여서 "givens"는 특정 타입의 정준(canonical) 값을 정의하여, 컨텍스트 매개변수(context parameter)에 대한 인자를 합성(synthesize)하는 데 사용됩니다. 이는 Scala 3 컨텍스트 추상화 시스템의 핵심을 이룹니다.

### 2.2 기본 구조

주어진 인스턴스 선언은 `given` 키워드로 시작합니다. 다음은 대표적인 예입니다.

```scala
trait Ord[T]:
  def compare(x: T, y: T): Int
  extension (x: T)
    def < (y: T) = compare(x, y) < 0
    def > (y: T) = compare(x, y) > 0

given intOrd: Ord[Int]:
  def compare(x: Int, y: Int) =
    if x < y then -1 else if x > y then +1 else 0
```

여기서 `intOrd`는 `Ord[Int]` 타입의 주어진 인스턴스이며, `compare` 메서드를 구현합니다.

조건부(conditional) givens는 컨텍스트 바운드(context bound)를 통해 다른 인스턴스에 의존할 수 있습니다.

```scala
given listOrd: [T: Ord] => Ord[List[T]]:
  def compare(xs: List[T], ys: List[T]): Int = (xs, ys) match
    case (Nil, Nil) => 0
    case (Nil, _) => -1
    case (_, Nil) => +1
    case (x :: xs1, y :: ys1) =>
      val fst = summon[Ord[T]].compare(x, y)
      if fst != 0 then fst else compare(xs1, ys1)
```

위 정의는 "`Ord[T]`에 대한 주어진 인스턴스가 존재한다면, `Ord[List[T]]`에 대한 주어진 인스턴스를 만들 수 있다"는 의미입니다. `[T: Ord] =>` 부분이 조건(즉, 컨텍스트 매개변수 `using Ord[T]`)을 나타냅니다.

### 2.3 익명 givens (Anonymous Givens)

이름은 생략할 수 있으며, 이 경우 컴파일러가 구현된 타입으로부터 식별자를 합성합니다. 예를 들어, 이름이 없는 `Ord[Int]`는 `given_Ord_Int`가 되고, 이름이 없는 `Ord[List[T]]`는 `given_Ord_List`가 됩니다.

### 2.4 별칭 givens (Alias Givens)

별칭 givens는 표현식(expression)을 사용하여 인스턴스를 정의합니다.

```scala
given global: ExecutionContext = ForkJoinPool()
```

이것은 익명으로도 가능합니다.

```scala
given Position = enclosingTree.position
```

이러한 별칭 givens는 첫 접근 시 초기화가 트리거됩니다. 무조건적(unconditional) 인스턴스의 경우 이 초기화는 스레드 안전(thread-safe)하며 캐시됩니다.

### 2.5 초기화 동작 (Initialization Behavior)

- 매개변수가 없는 **무조건적 givens**는 첫 접근 시점에 요청에 따라(on-demand) 초기화됩니다.
- **불변(immutable) 별칭 givens**는 캐시된 필드 오버헤드 없이 단순한 포워더(forwarder)로 동작합니다.
- **조건부 givens**는 참조될 때마다 새로운 인스턴스를 생성합니다.

### 2.6 구문 개요

givens는 다음 패턴을 따릅니다.

- 예약어 `given`
- 선택적 이름과 콜론(`:`)
- 선택적 조건(타입 매개변수, 항 매개변수, 또는 함수 인자 타입)
- 단일 구현 타입과 함께
  - `=` 표현식(별칭 형태, alias form), 또는
  - 생성자 적용(constructor application)과 템플릿 본문(structural form, 구조적 형태)

---

## 3. using 절 (Using Clauses)

### 3.1 개요

함수형 프로그래밍은 대부분의 의존성을 단순한 함수 매개변수화(function parameterization)로 표현하는 경향이 있습니다. 그러나 이는 반복적인 인자 전달로 이어질 수 있습니다. 컨텍스트 매개변수(context parameter)는 컴파일러가 인자를 자동으로 합성하도록 허용하여 이 문제를 해결합니다.

### 3.2 기본 구문

컨텍스트 매개변수는 `using` 키워드로 도입됩니다.

```scala
def max[T](x: T, y: T)(using ord: Ord[T]): T =
  if ord.compare(x, y) < 0 then y else x
```

이 함수는 명시적으로 호출하거나 암시적 해소(implicit resolution)를 통해 호출할 수 있습니다.

```scala
max(2, 3)(using intOrd)  // 명시적
max(2, 3)                // 암시적
```

### 3.3 익명 컨텍스트 매개변수 (Anonymous Context Parameters)

컨텍스트 매개변수가 다른 함수로 단순히 전달(forward)되기만 할 때는 그 이름을 생략할 수 있습니다.

```scala
def maximum[T](xs: List[T])(using Ord[T]): T =
  xs.reduceLeft(max)
```

매개변수는 전체 선언 형태인 `(p_1: T_1, ..., p_n: T_n)` 또는 타입만 지정하는 형태인 `T_1, ..., T_n` 중 하나로 지정할 수 있습니다. 다만 가변 인자(vararg) 매개변수는 `using` 절에서 지원되지 않습니다.

### 3.4 클래스 컨텍스트 매개변수 (Class Context Parameters)

클래스 컨텍스트 매개변수에 `val` 또는 `var`를 추가하면 멤버(member)가 됩니다.

```scala
class GivenIntBox(using val usingParameter: Int):
  def myInt = summon[Int]

val b = GivenIntBox(using 23)
```

이 접근법은 명시적인 `given` 멤버를 만드는 것보다 선호됩니다. 클래스 내부에서 모호성(ambiguity)을 회피하기 때문입니다.

### 3.5 복잡한 인자의 추론 (Inferring Complex Arguments)

컨텍스트 매개변수는 정교한 타입 주도(type-driven) 해소를 가능하게 합니다.

```scala
def descending[T](using asc: Ord[T]): Ord[T] = new Ord[T]:
  def compare(x: T, y: T) = asc.compare(y, x)

def minimum[T](xs: List[T])(using Ord[T]) =
  maximum(xs)(using descending)
```

다음 호출들은 모두 동등하게 정규화(normalize)됩니다.

- `minimum(xs)`
- `maximum(xs)(using descending)`
- `maximum(xs)(using descending(using intOrd))`

### 3.6 다중 using 절 (Multiple using Clauses)

여러 개의 `using` 절이 공존할 수 있으며, 일반 매개변수와 자유롭게 혼합될 수 있습니다.

```scala
def f(u: Universe)(using ctx: u.Context)(using s: ctx.Symbol, k: ctx.Kind) = ...
```

인자는 왼쪽에서 오른쪽으로 매칭됩니다. 따라서 `f(global)(using sym, kind)`는 실패합니다 — `ctx`를 먼저 공급해야 하기 때문입니다.

### 3.7 명시적 using과 암시적 using

인자가 `using`을 통해 명시적으로 공급될 때, 누락된 매개변수는 기본값(default)으로 채워집니다. 암시적으로 공급될 때는 누락된 매개변수가 암시적 값(implicit value)을 사용합니다. 이 구별은 중요합니다.

```scala
def f(using p: Boolean, i: Int, s: String = "hello, world") = ...

f(using p = false, i = 27)  // s는 암시적 String이 아니라 기본값을 사용
```

기본값이 암시적 값을 가릴(shadow) 때 컴파일러는 경고를 발생시킵니다.

### 3.8 인스턴스 소환 (Summoning Instances)

`summon` 메서드는 주어진 인스턴스를 가져옵니다.

```scala
def summon[T](using x: T): x.type = x

summon[Ord[List[Int]]]  // listOrd(using intOrd)로 해소됨
```

### 3.9 구문 문법 (Syntax Grammar)

```
ClsParamClause      ::=  ... | UsingClsParamClause
DefParamClause      ::=  ... | UsingParamClause
UsingClsParamClause ::=  '(' 'using' (ClsParams | Types) ')'
UsingParamClause    ::=  '(' 'using' (DefTermParams | Types) ')'
ParArgumentExprs    ::=  ... | '(' 'using' ExprsInParens ')'
```

`using` 키워드는 소프트 키워드(soft keyword)로, 매개변수/인자 목록의 시작 위치에서만 키워드로 인식됩니다.

---

## 4. 컨텍스트 바운드 (Context Bounds)

### 4.1 핵심 개념

컨텍스트 바운드(context bound)는 흔한 패턴—타입 매개변수에 의존하는 컨텍스트 매개변수—을 위한 약식(shorthand) 구문을 제공합니다. 표기법 `[T: Ord]`는 `[T](using Ord[T])`와 동등합니다.

문서에 명시된 바와 같이: "메서드 또는 클래스의 타입 매개변수 `T`에 붙은 `: Ord` 같은 바운드는, 둘러싸는 메서드의 시그니처에 추가되는 컨텍스트 매개변수 `using Ord[T]`를 나타냅니다."

### 4.2 이름 있는 컨텍스트 바운드 (Named Context Bounds)

Scala 3.6부터 컨텍스트 바운드는 `as` 절을 통해 이름을 가질 수 있습니다.

```scala
def reduce[A: Monoid as m](xs: List[A]): A =
  xs.foldLeft(m.unit)(_ `combine` _)
```

이것은 `def reduce[A](xs: List[A])(using m: Monoid[A]): A`로 확장(expand)되며, 이름이 붙은 증인(witness) `m`을 메서드 본문 안에서 참조할 수 있게 합니다.

### 4.3 집합 컨텍스트 바운드 (Aggregate Context Bounds)

여러 개의 컨텍스트 바운드는 중괄호(`{}`)를 사용하여 지정할 수 있습니다.

```scala
def showMax[X : {Ord, Show}](x: X, y: X): String
```

이름 있는 변형도 마찬가지로 작동합니다.

```scala
def showMax[X : {Ord as ordering, Show as show}](x: X, y: X): String =
  show.asString(ordering.max(x, y))
```

### 4.4 매개변수 배치 규칙 (Parameter Placement Rules)

생성된 컨텍스트 매개변수는 다음 세 가지 규칙을 따릅니다.

1. 바운드가 후속 절(clause)에서 이름으로 참조되면, 컨텍스트 바운드는 그 절보다 앞서는 using 절로 매핑됩니다.
2. 마지막 매개변수 절이 이미 `using` 절이라면, 새 매개변수를 그 앞에 병합(merge)합니다.
3. 그 외의 경우, 끝에 새로운 using 절을 생성합니다.

**규칙 1에 의한 예:**

```scala
def run[P : Parser as p](in: p.Input): p.Result
// 다음으로 확장됨:
def run[P](using p: Parser[P])(in: p.Input): p.Result
```

여기서 `p`는 `in: p.Input`에서 이름으로 참조되므로, `using p: Parser[P]`가 `(in: p.Input)` 절보다 앞에 배치됩니다.

### 4.5 다형 함수 타입 (Polymorphic Function Types)

컨텍스트 바운드는 다형 함수 타입(polymorphic function type)과 함께 작동합니다(Scala 3.6+).

```scala
type Comparer = [X: Ord] => (x: X, y: X) => Boolean
val less: Comparer = [X: Ord as ord] => (x: X, y: X) =>
  ord.compare(x, y) < 0
```

이것은 컨텍스트 함수 타입(context function type, `?=>`)을 사용하여 확장됩니다.

```scala
type Comparer = [X] => (x: X, y: X) => Ord[X] ?=> Boolean
```

### 4.6 추상 타입 멤버 (Abstract Type Members)

추상 타입(abstract type)에 대한 컨텍스트 바운드(Scala 3.6+)는 지연된 givens(deferred givens)로 확장됩니다.

```scala
class Collection:
  type Element: Ord
// 다음으로 확장됨:
class Collection:
  type Element
  given Ord[Element] = deferred
```

### 4.7 구문 (Syntax)

형식 문법은 다음을 포함합니다.

```
ContextBounds     ::=  ContextBound
                    |  '{' ContextBound {',' ContextBound} '}'
ContextBound      ::=  Type ['as' id]
```

---

## 5. given 임포트 (Given Imports)

### 5.1 핵심 개념

Scala 3는 `given` 인스턴스를 위한 전용 임포트 메커니즘을 제공합니다. 핵심적인 구별은 다음과 같습니다.

> 일반 와일드카드 선택자 `*`는 givens나 확장(extension)을 제외한 모든 정의를 범위로 가져오는 반면, `given` 선택자는 모든 givens(확장으로부터 비롯된 것 포함)를 범위로 가져옵니다.

즉, `import A.*`는 일반 정의만 가져오고 주어진 인스턴스는 가져오지 않습니다. 주어진 인스턴스를 명시적으로 가져오려면 `given` 선택자를 사용해야 합니다.

### 5.2 기본 구문

객체로부터 주어진 인스턴스만 임포트하려면 다음을 사용합니다.

```scala
import A.given
```

일반 임포트와 given 임포트를 결합하려면 다음을 사용합니다.

```scala
import A.{given, *}
```

### 5.3 타입에 의한 임포트 (Importing By Type)

와일드카드 임포트에만 의존하는 대신, 타입 선택자(by-type selector)를 사용하여 어떤 given 타입을 임포트할지 명시할 수 있습니다.

```scala
import A.given TC
```

이것은 `A` 안에서 타입 `TC`에 부합하는 모든 given을 임포트합니다. 여러 타입을 지정할 수도 있습니다.

```scala
import A.{given T1, ..., given Tn}
```

매개변수화된 타입(parameterized type)의 경우, 와일드카드 인자(`?`)로 범위를 명확히 합니다.

```scala
import Instances.{given Ordering[?], given ExecutionContext}
```

타입에 의한 임포트와 이름에 의한 임포트는 공존할 수 있으며, 이때 타입 기반 선택자는 절(clause)의 마지막에 위치합니다.

### 5.4 마이그레이션 지원 (Migration Support)

명세는 Scala 2의 implicit 시스템을 인정하는 전환(transition) 규칙을 포함합니다. 처음에는 `given` 선택자가 구식(old-style) implicit도 함께 임포트합니다. 버전 3.1에서 디프리케이션(deprecation) 경고가 도입되었고, 이후 릴리스에서 점진적으로 강제(enforcement)됩니다. 이는 라이브러리의 점진적 마이그레이션을 가능하게 합니다.

### 5.5 구문 문법 (Syntax Grammar)

형식 명세는 `WildCardSelector`를 `'*'` 또는 `'given' [InfixType]`로 정의하여, 더 넓은 임포트 표현식 구조 안에서 이름 없는(unnamed) given 임포트와 타입 제약된(type-constrained) given 임포트를 모두 허용합니다.

---

## 6. 이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)

### 6.1 개요

이름에 의한 컨텍스트 매개변수(by-name context parameter)는 컨텍스트 인자가 지연 평가(lazily evaluated)되도록 허용하여, 발산하는(divergent) 추론 루프를 방지합니다. 문서가 설명하듯이: "컨텍스트 매개변수는 발산하는 추론 확장(divergent inferred expansion)을 피하기 위해 이름에 의한(by-name) 방식으로 선언될 수 있습니다."

### 6.2 핵심 메커니즘

이 기능은 `(ev: => Codec[T])` 같은 구문을 사용하여 이름에 의한 컨텍스트 매개변수를 선언합니다. 이는 인자가 실제로 필요해질 때까지 평가를 지연시키며, Scala의 일반적인 이름에 의한 매개변수(by-name parameter)와 유사합니다.

### 6.3 예시 구조

참고 문서는 `Codec` 트레이트(trait) 예제를 제공하는데, 그 구조는 다음과 같습니다.

- `intCodec` 주어진 인스턴스가 `Codec[Int]`를 처리합니다.
- `optionCodec` 주어진 인스턴스가 이름에 의한 매개변수 `ev: => Codec[T]`를 받아 `Codec[Option[T]]`를 처리합니다.
- `None`이 매칭될 때, 내부 타입에 대한 코덱은 불필요하게 평가되지 않습니다.

이러한 재귀적 정의(`Codec[Option[Option[Int]]]` 같은 경우)에서, 이름에 의한 매개변수를 사용하지 않으면 컴파일러가 무한히 펼쳐지는 발산을 일으킬 수 있습니다. 지연 평가를 통해 이를 방지합니다.

### 6.4 합성 알고리즘 (Synthesis Algorithm)

컴파일러는 다음 단계를 통해 이름에 의한 컨텍스트 매개변수의 인자를 합성합니다.

1. 필요한 타입의 새로운(fresh) given을 생성합니다.
2. 초기 후보 검색(initial candidate search) 동안에는 그것을 사용 불가능하게 유지합니다(루프 방지를 위해).
3. 다른 이름에 의한 매개변수에 대한 중첩 검색(nested search)에서는 그것을 사용 가능하게 만듭니다.
4. 합성된 표현식이 이 지역(local) given을 참조한다면, 그것을 다음과 같이 감쌉니다: `{ given lv: T = E; lv }`.
5. 그렇지 않다면 표현식을 변경 없이 반환합니다.

### 6.5 실질적 이점 (Practical Benefit)

> 합성된 컨텍스트 매개변수의 인자는, 그렇게 하지 않으면 발산하는 확장을 방지하기 위해 필요한 경우 지역 `val`로 뒷받침됩니다.

이 접근법은 `Option[Option[Int]]` 같은 재귀적 제네릭 타입이 무한 루프 없이 올바르게 해소되도록 합니다.

---

## 7. Scala 2 implicit와의 관계

Scala 3 참고 문서는 새로운 컨텍스트 추상화 기능이 Scala 2의 implicit 시스템에 어떻게 매핑되는지 설명합니다.

### 7.1 Scala 3 → Scala 2 매핑

**주어진 인스턴스 (Given Instances)**
implicit 객체(object) 또는 implicit 메서드를 가진 클래스로 변환됩니다. 단순한 주어진 인스턴스는 implicit 객체가 되고, 매개변수화된 givens는 클래스/implicit 메서드 조합이 됩니다.

**using 절 (Using Clauses)**
암시적 매개변수 절(implicit parameter clause)에 대응합니다. 핵심 차이점은, 명시적 인자가 일반 적용(application) 구문이 아니라 `(using ...)` 구문을 요구한다는 것입니다.

**컨텍스트 바운드 (Context Bounds)**
두 버전에서 동일하게 작동하며, 각자의 암시적 매개변수 형태로 확장됩니다.

**확장 메서드 (Extension Methods)**
직접적인 Scala 2 대응물은 없지만, 암시적 클래스(implicit class)를 통해 시뮬레이션할 수 있습니다.

**타입 클래스 도출(Type Class Derivation), 컨텍스트 함수 타입(Context Function Types), 이름에 의한 암시적 매개변수(Implicit By-Name Parameters)**
Scala 2 대응물이 없습니다.

### 7.2 Scala 2 → Scala 3 매핑

**암시적 변환 (Implicit Conversions)**
`scala.Conversion`의 주어진 인스턴스로 표현될 수 있습니다. 예를 들어, 암시적 변환 함수는 given Conversion 인스턴스가 됩니다.

**암시적 클래스 (Implicit Classes)**
확장 메서드(선호됨) 또는 클래스/given Conversion 쌍으로 매핑됩니다.

**암시적 값 (Implicit Values)**
일반 `val`과 별칭 given(alias given)의 쌍으로 표현될 수 있습니다.

**추상 implicit (Abstract Implicits)**
일반 추상 정의(abstract definition)와 별칭 given으로 표현됩니다.

### 7.3 호환성 정책

문서는 Scala 3가 호환성을 위해 두 시스템(구식 implicit과 새로운 given/using)을 모두 구현한다고 명시합니다. 향후 버전에서 구식 implicit이 디프리케이션될 가능성이 있으며, 자동 재작성(automatic rewriting) 도구의 지원을 받습니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/)
