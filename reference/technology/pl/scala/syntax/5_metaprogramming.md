# Scala 3 메타프로그래밍: inline과 매크로

## Scala 3 메타프로그래밍: inline과 컴파일 타임 연산

---

### 목차

1. [메타프로그래밍 개요](#메타프로그래밍-개요)
   - [핵심 기능들](#핵심-기능들)
2. [인라인(Inline)](#인라인inline)
   - [인라인 정의(Inline Definitions)](#인라인-정의inline-definitions)
   - [예제 확장(Example Expansion)](#예제-확장example-expansion)
   - [재귀 인라인 메서드(Recursive Inline Methods)](#재귀-인라인-메서드recursive-inline-methods)
   - [인라인 매개변수(Inline Parameters)](#인라인-매개변수inline-parameters)
   - [오버라이딩 규칙(Rules for Overriding)](#오버라이딩-규칙rules-for-overriding)
   - [`@inline`과의 관계(Relationship to @inline)](#inline과의-관계relationship-to-inline)
   - [상수 표현식의 정의(The Definition of Constant Expression)](#상수-표현식의-정의the-definition-of-constant-expression)
   - [투명 인라인 메서드(Transparent Inline Methods)](#투명-인라인-메서드transparent-inline-methods)
   - [투명 인라인과 일반 인라인의 차이(Transparent vs. Non-transparent Inline)](#투명-인라인과-일반-인라인의-차이transparent-vs-non-transparent-inline)
   - [인라인 조건문(Inline Conditionals)](#인라인-조건문inline-conditionals)
   - [인라인 매치(Inline Matches)](#인라인-매치inline-matches)
3. [컴파일 타임 연산(Compile-time Operations)](#컴파일-타임-연산compile-time-operations)
   - [`scala.compiletime` 패키지](#scalacompiletime-패키지)
   - [`constValue`와 `constValueOpt`](#constvalue와-constvalueopt)
   - [`erasedValue`](#erasedvalue)
   - [`error`](#error)
   - [`scala.compiletime.ops` 패키지](#scalacompiletimeops-패키지)
   - [선택적 given 소환(Summoning Givens Selectively)](#선택적-given-소환summoning-givens-selectively)
   - [`summonInline`](#summoninline)
4. [참고 자료](#참고-자료)

---

### 메타프로그래밍 개요

> 📘 **처음 배우는 분께 — 메타프로그래밍이란**
>
> 메타프로그래밍(metaprogramming)은 한마디로 "**프로그램을 다루는 프로그램**"입니다.
> 보통 우리가 짜는 코드는 숫자나 문자열 같은 *데이터*를 다루지만, 메타프로그래밍은
> *코드 그 자체*를 데이터처럼 다뤄서, 컴파일 시점에 **코드를 생성하거나 미리 계산**합니다.
> 예를 들어 "이 함수 호출을 컴파일 때 풀어서 더 빠른 코드로 바꿔라" 같은 일을 시킵니다.
>
> ⚠️ 이 장(08)은 Scala에서 **가장 고급 주제**입니다. 00번 추천 학습 순서에서도 맨 마지막입니다.
> 처음 배우는 분이라면 **지금은 통째로 건너뛰어도 전혀 문제없습니다.** 나중에 라이브러리를
> 직접 만들거나 컴파일 타임 최적화가 필요해질 때 돌아오면 됩니다.

Scala 3의 레퍼런스 문서는 여러 가지 기본 기능 위에 구축된, 새롭게 설계된 메타프로그래밍(metaprogramming) 시스템을 소개합니다.

#### 핵심 기능들

**인라인(inline)** 은 정의(definition)가 사용 지점(point of use)에서 인라인될 것임을 보장하는 수정자(modifier)입니다. Scala에서의 인라이닝(inlining)은 컴파일러에 대한 단순한 제안(suggestion)이 아니라, 타이퍼(Typer) 단계에서 실행되는 명령(command)입니다. 이 메커니즘은 타입 수준 프로그래밍(type-level programming), 매크로(macros), 코드 생성(code generation)을 포함한 다운스트림(downstream) 컴파일 타임 연산(compile-time operation)을 가능하게 합니다.

> 📘 **처음 배우는 분께 — 컴파일 시점 vs 런타임**
>
> 이 장을 읽을 때 가장 중요한 구분은 "**언제 일어나느냐**"입니다.
> - **컴파일 시점(compile-time)**: 코드를 *빌드*할 때. 프로그램이 아직 돌기 전, 컴파일러가 코드를 검사하고 변환하는 시간.
> - **런타임(runtime)**: 빌드가 끝난 프로그램이 실제로 *실행*되는 시간.
>
> 보통 우리가 짜는 코드는 런타임에 동작합니다. 그런데 `inline`을 비롯한 이 장의 기능들은
> **컴파일 시점에 미리 코드를 펼치거나 값을 계산**해 둡니다. 그래서 정작 프로그램이 돌 때는
> 그 계산이 이미 끝나 있어, 더 빠르거나 더 정밀한 타입을 가질 수 있습니다.

**컴파일 타임 연산(Compile-time Operations)** 은 컴파일 타임 값(value)과 타입(type)의 조작(manipulation)을 지원하는 표준 라이브러리 헬퍼 정의(helper definition)들을 제공합니다.

**매크로(Macros)** 는 인용(quotation)과 스플라이싱(splicing)이라는 두 가지 기본 연산을 활용합니다. 인용은 코드(code)를 데이터 표현(data representation)으로 변환하며(표현식에는 `'{...}`, 타입에는 `'[...]`를 사용), 스플라이싱은 그 표현을 다시 실행 가능한 코드로 변환합니다(`${ ... }` 구문 사용). 이를 `inline`과 결합하면 컴파일 시점에 프로그래밍적으로 코드를 구성(construction)할 수 있습니다.

**런타임 스테이징(Runtime Staging)** 은 매크로의 능력을 확장하여, 컴파일 타임이 아닌 런타임(runtime)에 코드를 생성할 수 있게 합니다. 이 다단계 프로그래밍(multi-stage programming) 접근 방식은 런타임 데이터에 의존하는 생성형 코드(generative code)를 가능하게 하며, `inline` 요구 사항 없이 동일한 인용 및 스플라이싱 메커니즘을 사용합니다.

**리플렉션(Reflection)** 은 TASTy를 통해 인용된 코드(quoted code)의 구조를 분석하는 능력을 제공합니다. 인용을 불투명한(opaque) 대상으로 취급하는 대신, TASTy 리플렉션은 표준화된 API를 통해 타입이 부여된 추상 구문 트리(typed abstract syntax tree)를 노출하여 내부 표현(internal representation)의 세부 사항을 드러냅니다.

**TASTy 검사(TASTy Inspection)** 는 `.tasty` 파일 내에 압축된 이진 형식(compressed binary format)으로 저장된, 직렬화된 타입 부여 추상 구문 트리(serialized typed abstract syntax tree)를 다루며, 트리 구조에 대한 외부 분석(external analysis)을 가능하게 합니다.

이러한 상호 연결된 기능들은 컴파일 타임 및 런타임 메타프로그래밍에 대한 Scala 3의 포괄적인 접근 방식을 구성합니다.

---

### 인라인(Inline)

#### 인라인 정의(Inline Definitions)

> 📘 **처음 배우는 분께 — inline의 직관**
>
> `inline`은 "**이 함수 호출을, 호출한 자리에 코드를 그대로 펼쳐 넣어라**"는 지시입니다.
> 즉 함수를 *부르는* 게 아니라, 함수 본문을 복사해서 부르는 자리에 *붙여넣기* 합니다.
> C 언어의 `#define` 매크로나 `inline` 함수를 떠올리면 비슷합니다.
>
> 예를 들어 `inline def double(x: Int) = x * 2`를 만들고 `double(5)`라고 쓰면,
> 컴파일러는 그 자리를 그냥 `5 * 2`로 바꿔 놓습니다. 함수 호출 비용이 사라지고,
> 더 나아가 컴파일러가 `5 * 2`를 미리 `10`으로 계산해 둘 수도 있습니다.
>
> Scala 2에도 비슷한 `@inline` 힌트가 있었지만 그것은 "되도록 펼쳐줘"라는 *부탁*이었고,
> Scala 3의 `inline`은 "반드시 펼친다"는 *보장*이라는 점이 다릅니다(아래 `@inline`과의 관계 참고).

`inline`은 정의(definition)가 사용 지점에서 인라인될 것임을 보장하는 소프트 수정자(soft modifier)입니다.

```scala
object Config:
  inline val logging = false

object Logger:

  private var indent = 0

  inline def log[T](msg: String, indentMargin: =>Int)(op: => T): T =
    if Config.logging then
      println(s"${"  " * indent}start $msg")
      indent += indentMargin
      val result = op
      indent -= indentMargin
      println(s"${"  " * indent}$msg = $result")
      result
    else op
end Logger
```

> 💡 **왜 필요한가**
>
> 위 예제의 핵심은 `inline val logging = false`입니다. `logging`이 컴파일 시점에 `false`로
> 확정되므로, 컴파일러는 `if Config.logging then ... else op`에서 **`else op`만 남기고
> 나머지 로깅 코드를 통째로 지워버립니다.** 결과적으로 "로깅 기능을 끄면 로깅 관련 코드가
> 실행 파일에서 아예 사라지는" 효과를 얻습니다. 런타임에 매번 `if`를 검사하는 비용조차 없습니다.
> 이렇게 "설정에 따라 코드를 켜고 끄는" 것이 inline의 대표적인 쓰임새입니다.

**인라인 값(inline value)** 은 자신의 우변(right-hand side)을 상수(constant)로 취급하며, 이는 Java 및 Scala 2의 `final`과 동등합니다. 우변은 반드시 상수 표현식(constant expression)이어야 합니다.

**인라인 메서드(inline method)** 는 호출 지점(call site)에서 항상 인라인됩니다. if-then-else의 조건이 상수(constant condition)일 때, 선택된 분기(branch)로 다시 작성(rewrite)됩니다.

위 예제에서 `inline val logging = false`는 `logging`을 자신에게 할당된 값과 동등한 리터럴 상수(literal constant)로 취급합니다.

---

#### 예제 확장(Example Expansion)

```scala
var indentSetting = 2

def factorial(n: BigInt): BigInt =
  log(s"factorial($n)", indentSetting) {
    if n == 0 then 1
    else n * factorial(n - 1)
  }
```

만약 `Config.logging == false` 라면, 위 코드는 다음과 같이 단순화됩니다.

```scala
def factorial(n: BigInt): BigInt =
  if n == 0 then 1
  else n * factorial(n - 1)
```

만약 `Config.logging == true` 라면, 다음과 같이 확장됩니다.

```scala
def factorial(n: BigInt): BigInt =
  val msg = s"factorial($n)"
  println(s"${"  " * indent}start $msg")
  Logger.inline$indent_=(indent.+(indentSetting))
  val result =
    if n == 0 then 1
    else n * factorial(n - 1)
  Logger.inline$indent_=(indent.-(indentSetting))
  println(s"${"  " * indent}$msg = $result")
  result
```

인라인 메서드는 반드시 완전히 적용(fully applied)되어야 합니다. 다음은 잘못된 형식(ill-formed)입니다.

```scala
Logger.log[String]("some op", indentSetting)
```

그러나 와일드카드 인자(wildcard arguments)는 허용됩니다.

```scala
Logger.log[String]("some op", indentSetting)(_)
```

---

#### 재귀 인라인 메서드(Recursive Inline Methods)

인라인 메서드는 재귀적(recursive)일 수 있습니다. 상수 지수(constant exponent)가 주어지면, 다음 메서드는 직선형 인라인 코드(straight inline code)로 확장됩니다.

```scala
inline def power(x: Double, n: Int): Double =
  if n == 0 then 1.0
  else if n == 1 then x
  else
    val y = power(x, n / 2)
    if n % 2 == 0 then y * y else y * y * x

power(expr, 10)
// 다음과 같이 변환됩니다
//
//   val x = expr
//   val y1 = x * x   // ^2
//   val y2 = y1 * y1 // ^4
//   val y3 = y2 * x  // ^5
//   y3 * y3          // ^10
```

> ⚠️ **짚고 넘어가기 — 재귀 inline은 "펼치기"가 끝나야 한다**
>
> 보통의 재귀 함수는 런타임에 자기 자신을 계속 호출합니다. 하지만 inline 재귀는 **컴파일 시점에
> 펼쳐지므로**, 컴파일러가 "이쯤에서 멈춰도 된다"는 것을 알 수 있어야 합니다. 위 `power(expr, 10)`은
> 지수 `10`이 *상수*라서 컴파일러가 곱셈 5번짜리 코드로 다 펼쳐낼 수 있습니다.
> 만약 펼치기가 끝없이 이어지면 컴파일이 멈추지 못하므로, 안전장치로 **최대 깊이 32**가 걸려 있습니다.

최대 인라이닝 깊이(maximum inlining depth)는 32이며, 컴파일러 설정 `-Xmax-inlines`로 변경할 수 있습니다.

---

#### 인라인 매개변수(Inline Parameters)

> 📘 **처음 배우는 분께 — 매개변수의 세 가지 전달 방식**
>
> 이 절은 세 가지를 한 번에 비교합니다. 헷갈리니 한 줄씩 정리합니다.
> - **일반(by-value) `x: Double`**: 부르기 전에 *한 번* 계산해서 그 값을 넘김. (가장 흔한 방식)
> - **이름에 의한(by-name) `x: => Double`**: 안 계산하고 넘긴 뒤, 본문에서 *쓸 때마다* 계산. (02·10번에서 다룸)
> - **inline `inline x: Double`**: 인자로 넘긴 *코드(식) 자체*를 본문의 사용 지점마다 그대로 복사해 넣음.
>
> by-name과 inline은 "쓸 때마다 다시 평가"라는 점은 비슷하지만, inline은 함수 호출조차 없이
> **소스 코드를 붙여넣는다는 점**이 다릅니다. 그래서 넘긴 값이 상수면 그 상수가 본문에 박혀,
> 컴파일러가 더 많은 것을 미리 계산할 수 있습니다(상수 전파).

인라인 메서드의 매개변수(parameter)는 `inline` 수정자를 가질 수 있는데, 이는 실제 인자(actual argument)가 메서드 본문(body)에 인라인된다는 것을 의미합니다. 이는 코드 중복(code duplication)을 허용한다는 점에서 이름에 의한 매개변수(by-name parameter)와 다르며, 상수 전파(constant propagation)에 유용합니다.

```scala
inline def funkyAssertEquals(actual: Double, expected: =>Double, inline delta: Double): Unit =
  if (actual - expected).abs > delta then
    throw new AssertionError(s"difference between ${expected} and ${actual} was larger than ${delta}")

funkyAssertEquals(computeActual(), computeExpected(), computeDelta())
// 다음과 같이 변환됩니다
//
//   val actual = computeActual()
//   def expected = computeExpected()
//   if (actual - expected).abs > computeDelta() then
//     throw new AssertionError(s"difference between ${expected} and ${actual} was larger than ${computeDelta()}")
```

위 예제에서 `actual`은 일반(by-value) 매개변수이므로 한 번만 평가되는 지역 `val`로 변환됩니다. `expected`는 이름에 의한(by-name, `=>Double`) 매개변수이므로 함수 참조에 해당하는 `def`로 변환됩니다. 반면 `inline delta`는 메서드 본문의 사용 지점마다 실제 인자 `computeDelta()`가 직접 인라인되어 코드가 복제(duplicate)됩니다.

---

#### 오버라이딩 규칙(Rules for Overriding)

1. 인라인 메서드는 비(非)인라인 메서드(non-inline method)를 오버라이드(override)할 수 있으며, 런타임에 호출될 수 있습니다.

```scala
abstract class A:
  def f: Int
  def g: Int = f

class B extends A:
  inline def f = 22
  override inline def g = f + 11

val b = new B
val a: A = b
// 인라인된 호출(inlined invocations)
assert(b.f == 22)
assert(b.g == 33)
// 동적 호출(dynamic invocations)
assert(a.f == 22)
assert(a.g == 33)
```

2. 인라인 메서드는 사실상 final(effectively final)입니다.

3. 추상 인라인 메서드(abstract inline method)는 오직 다른 인라인 메서드에 의해서만 구현될 수 있으며, 직접 호출될 수 없습니다.

```scala
abstract class A:
  inline def f: Int

object B extends A:
  inline def f: Int = 22

B.f         // OK
val a: A = B
a.f         // error: cannot inline f in A.
```

---

#### `@inline`과의 관계(Relationship to @inline)

Scala 2는 백엔드 인라이닝 힌트(backend inlining hint)로서 `@inline` 애너테이션을 정의합니다. `inline` 수정자는 이보다 더 강력합니다.

- 확장(expansion)이 최선의 노력(best effort)이 아니라 **보장(guaranteed)** 됩니다.
- 확장이 백엔드(backend)가 아니라 **프런트엔드(frontend)** 에서 일어납니다.
- 확장이 **재귀 메서드(recursive method)** 에도 적용됩니다.

---

#### 상수 표현식의 정의(The Definition of Constant Expression)

인라인 값(inline value)의 우변과 인라인 매개변수 인자(inline parameter argument)는 반드시 Scala 언어 명세(Scala Language Specification) §6.24에 따른 상수 표현식(constant expression)이어야 하며, 여기에는 순수 수치 연산(pure numeric computation)의 상수 폴딩(constant folding)과 같은 플랫폼별 확장(platform-specific extension)도 포함됩니다.

인라인 값은 반드시 리터럴 타입(literal type)을 가져야 합니다.

```scala
inline val four = 4
// 다음과 동등합니다
inline val four: 4 = 4
```

인라인 값은 명시적인 구문 없이도 타입을 가질 수 있습니다.

```scala
trait InlineConstants:
  inline val myShort: Short

object Constants extends InlineConstants:
  inline val myShort/*: Short(4)*/ = 4
```

---

#### 투명 인라인 메서드(Transparent Inline Methods)

> 📘 **처음 배우는 분께 — transparent inline이란**
>
> 보통 함수는 선언한 반환 타입이 *고정*입니다. `def choose(...): A`라고 적으면 결과는 늘 `A` 타입이죠.
> `transparent`를 붙이면, 컴파일러가 인라인으로 펼친 뒤 **"실제로 들어온 값의 더 구체적인 타입"으로
> 반환 타입을 좁혀줍니다.** 아래 예에서 `choose(false)`는 선언상 `A`지만, 펼쳐보니 실제로는 `B`를
> 만들기 때문에 결과 타입이 `B`가 되어 `B`에만 있는 `.m`을 쓸 수 있게 됩니다.
>
> 한 줄 요약: **inline은 "코드를 펼친다", transparent inline은 "펼친 결과에 맞춰 타입까지 더 정밀하게 좁힌다".**
> "구현 내용이 타입에 영향을 준다"고 해서 이를 화이트박스(whitebox)라고 부릅니다.

인라인 메서드는 `transparent`로 선언될 수 있는데, 이는 확장(expansion) 시 반환 타입(return type)이 더 정밀한 타입으로 특수화(specialize)되도록 허용합니다.

```scala
class A
class B extends A:
  def m = true

transparent inline def choose(b: Boolean): A =
  if b then new A else new B

val obj1 = choose(true)  // 정적 타입(static type)은 A
val obj2 = choose(false) // 정적 타입(static type)은 B

// obj1.m // 컴파일 타임 오류: `m`은 `A`에 정의되어 있지 않음
obj2.m    // OK
```

`transparent`가 없으면 결과는 항상 `A` 타입을 가집니다. `transparent`가 있으면 타입 검사(type checking)는 확장된 타입(expanded type)을 사용합니다.

이러한 `transparent` 동작은 종종 화이트박스(whitebox) 동작이라고 불리는데, 구현 세부 사항(implementation detail)이 타입 검사에 영향을 미치기 때문입니다.

```scala
transparent inline def zero: Int = 0

val one: 1 = zero + 1
```

---

#### 투명 인라인과 일반 인라인의 차이(Transparent vs. Non-transparent Inline)

투명 인라인 메서드(transparent inline method)는 타입 검사 중(during type checking)에 확장됩니다. 그 외의 인라인 메서드는 타이핑(typing) 이후에 인라인됩니다.

```scala
inline def f1: T = ...
transparent inline def f2: T = (...): T
```

핵심적인 차이점은 다음과 같습니다. `transparent inline given`에서는 인라이닝 도중 발생한 오류(error)가 암시적 탐색 불일치(implicit search mismatch)로 간주되어 탐색(search)이 계속됩니다. `transparent inline given`의 우변(RHS)에 타입 어스크립션(type ascription)을 추가하면, 특수화된 타입을 피하면서도 탐색 동작은 유지할 수 있습니다. 반면 `inline given`은 타이핑 이후에 인라인되며, 오류는 일반적으로 그대로 보고(emit)됩니다.

---

#### 인라인 조건문(Inline Conditionals)

> 💡 **왜 필요한가 — `inline if`는 "약속을 강제"한다**
>
> 그냥 `if`는 조건이 상수이면 컴파일러가 알아서 한쪽 분기만 남길 *수도* 있습니다(될 수도, 안 될 수도).
> `inline if`는 "**이 조건은 반드시 컴파일 시점 상수여야 한다**"고 못 박는 것입니다.
> 덕분에 단순화가 *보장*되고, 만약 상수가 아닌 값이 들어오면 미루지 않고 곧바로 컴파일 오류로 알려줍니다.
> 즉 "런타임에야 결정되는 분기"가 실수로 끼어드는 것을 컴파일 단계에서 막아줍니다.

상수 조건(constant condition)을 가진 if-then-else 표현식은 선택된 분기(branch)로 단순화됩니다. 앞에 `inline`을 붙이면 조건이 상수임을 강제하여, 단순화가 보장됩니다.

```scala
inline def update(delta: Int) =
  inline if delta >= 0 then increaseBy(delta)
  else decreaseBy(-delta)
```

`update(22)` 호출은 `increaseBy(22)`로 다시 작성됩니다. 상수가 아닌 값으로 호출하면 컴파일 타임 오류가 발생합니다.

```
|  inline if delta >= 0 then ???
|  ^
|  cannot reduce inline if
|   its condition
|     delta >= 0
|   is not a constant value
| This location is in code that was inlined at ...
```

투명 인라인(transparent inline) 내에서, `inline if`는 타입 검사 도중 자신의 조건에 포함된 모든 인라인 정의(inline definition)의 인라이닝을 강제합니다.

---

#### 인라인 매치(Inline Matches)

> 📘 **처음 배우는 분께 — inline match란**
>
> 평소 `match`는 런타임에 값을 보고 분기를 고릅니다. `inline match`는 그 선택을 **컴파일 시점에,
> "값의 타입" 정보만으로** 미리 끝내버립니다. 예를 들어 `g(1.0d)`를 컴파일할 때 컴파일러는
> "이건 `Double`이군"을 알고 `case x: Double` 분기만 남기고 나머지는 지웁니다.
> 그래서 transparent와 결합하면, 입력 타입에 따라 결과 타입이 달라지는 함수를 만들 수 있습니다.
> 컴파일 시점에 어느 분기인지 확정할 수 없으면 그냥 넘어가지 않고 컴파일 오류를 냅니다.

인라인 메서드 본문 내의 `match` 표현식 앞에는 `inline`을 붙일 수 있습니다. 컴파일 타임 타입 정보(compile-time type information)가 분기를 선택할 수 있으면, 표현식은 그 분기로 축소(reduce)됩니다. 그렇지 않으면 컴파일 타임 오류가 발생합니다.

```scala
transparent inline def g(x: Any): Any =
  inline x match
    case x: String => (x, x) // Tuple2[String, String](x, x)
    case x: Double => x

g(1.0d) // Double의 서브타입인 1.0d 타입을 가짐
g("test") // (String, String) 타입을 가짐
```

처치 인코딩(Church-encoded) 숫자를 사용한 예제입니다.

```scala
trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

transparent inline def toInt(n: Nat): Int =
  inline n match
    case Zero     => 0
    case Succ(n1) => toInt(n1) + 1

inline val natTwo = toInt(Succ(Succ(Zero)))
val intTwo: 2 = natTwo
```

> 더 자세한 내용은 "Scala 2020: Semantics-preserving inlining for metaprogramming" 논문을 참고하세요.

---

### 컴파일 타임 연산(Compile-time Operations)

#### `scala.compiletime` 패키지

> 📘 **처음 배우는 분께 — scala.compiletime 패키지**
>
> `scala.compiletime`은 **오직 컴파일 시점에만 동작하는 도구 상자**입니다.
> 여기 들어 있는 함수들(`constValue`, `erasedValue`, `error`, `summonInline` 등)은 보통의
> 라이브러리 함수처럼 런타임에 실행되는 게 아니라, inline으로 코드를 펼치는 *과정에서* 컴파일러가
> 처리하고 사라집니다. 그래서 대부분 `inline def`, `inline match`와 짝을 지어 쓰입니다.
> "런타임에 직접 부르면 오류"라는 설명이 여러 번 나오는 이유가 이것입니다.

[`scala.compiletime`](https://scala-lang.org/api/3.x/scala/compiletime.html) 패키지는 값(value)에 대한 컴파일 타임 연산(compile-time operation)을 위한 헬퍼 정의(helper definition)들을 제공합니다.

---

#### `constValue`와 `constValueOpt`

`constValue`는 타입(type)이 표현하는 상수 값(constant value)을 추출하며, 해당 타입이 상수가 아니면 컴파일 오류를 발생시킵니다.

```scala
import scala.compiletime.constValue
import scala.compiletime.ops.int.S

transparent inline def toIntC[N]: Int =
  inline constValue[N] match
    case 0        => 0
    case _: S[n1] => 1 + toIntC[n1]

inline val ctwo = toIntC[2]
```

`constValueOpt`는 대신 `Option[T]`를 반환합니다. 튜플 타입(tuple type)의 경우, `constValueTuple`이 튜플 구성 요소(constituent)들로부터 상수 값을 추출합니다.

---

#### `erasedValue`

> ⚠️ **짚고 넘어가기 — erasedValue는 "값을 진짜로 만들지 않는다"**
>
> 이름이 헷갈릴 수 있는데, `erasedValue[T]`는 타입 `T`의 *진짜 값을 만들어 돌려주지 않습니다.*
> 단지 "여기 `T` 타입 값이 하나 있다고 *치자*"라고 컴파일러를 속여, **타입만 보고 분기**하게 하려는
> 용도입니다(아래 예제처럼 `inline match`의 대상으로만 씁니다). 펼치기가 끝나면 이 호출 자체가
> 사라지므로, 런타임에 실제로 부르면 오류가 납니다. "타입을 값처럼 들고 다니는 트릭"이라고 보면 됩니다.

`erasedValue` 함수는 컴파일 타임에 타입 `T`의 값을 반환하는 것처럼 동작합니다. 이 호출은 인라이닝 도중 제거(remove)되며, 인라인 확장 없이 직접(즉 런타임에) 호출하면 컴파일 오류가 발생합니다.

```scala
def erasedValue[T]: T
```

이를 통해 타입 기반(type-based) 케이스 구분(case distinction)을 할 수 있습니다. 다음은 `erasedValue`를 사용해 기본값(default value)을 구하는 예제입니다.

```scala
import scala.compiletime.erasedValue

transparent inline def defaultValue[T] =
  inline erasedValue[T] match
    case _: Byte    => Some(0: Byte)
    case _: Char    => Some(0: Char)
    case _: Short   => Some(0: Short)
    case _: Int     => Some(0)
    case _: Long    => Some(0L)
    case _: Float   => Some(0.0f)
    case _: Double  => Some(0.0d)
    case _: Boolean => Some(false)
    case _: Unit    => Some(())
    case _          => None

val dInt: Some[Int] = defaultValue[Int]
val dDouble: Some[Double] = defaultValue[Double]
val dBoolean: Some[Boolean] = defaultValue[Boolean]
val dAny: None.type = defaultValue[Any]
```

페아노 수(Peano numbers)를 사용한 타입 수준(type-level) 예제입니다.

```scala
transparent inline def toIntT[N <: Nat]: Int =
  inline scala.compiletime.erasedValue[N] match
    case _: Zero.type => 0
    case _: Succ[n] => toIntT[n] + 1

inline val two = toIntT[Succ[Succ[Zero.type]]]
```

---

#### `error`

> 💡 **왜 필요한가 — 직접 만드는 컴파일 오류**
>
> `error`는 "**내가 정한 조건이 안 맞으면 컴파일 자체를 실패시키고 친절한 메시지를 띄워라**"라고
> 시키는 도구입니다. 런타임에 예외(`throw`)를 던지는 것과 달리, 잘못된 사용을 **빌드 단계에서**
> 미리 잡아내 개발자에게 알려줄 수 있습니다. 라이브러리를 만들 때 "이렇게 쓰면 안 됩니다"를
> 컴파일 오류로 안내하는 식으로 활용합니다.

`error` 메서드는 인라인 확장(inline expansion) 도중 사용자 정의 컴파일 오류(user-defined compile error)를 생성합니다.

```scala
inline def error(inline msg: String): Nothing
```

사용 예제입니다.

```scala
import scala.compiletime.{error, codeOf}

inline def fail() =
  error("failed for a reason")

fail() // error: failed for a reason
```

또는 코드 참조(code reference)와 함께 사용합니다.

```scala
inline def fail(inline p1: Any) =
  error("failed on: " + codeOf(p1))

fail(identity("foo")) // error: failed on: identity[String]("foo")
```

여기서 `codeOf`는 인자로 전달된 표현식의 소스 코드 표현(source code representation)을 문자열로 반환합니다.

---

#### `scala.compiletime.ops` 패키지

[`scala.compiletime.ops`](https://scala-lang.org/api/3.x/scala/compiletime/ops.html) 패키지는 싱글톤 타입(singleton type)에 대한 원시 연산(primitive operation)을 위한 타입들을 제공합니다. 모든 인자가 싱글톤 타입일 때, 컴파일러는 연산 결과를 평가(evaluate)합니다.

```scala
import scala.compiletime.ops.int.*
import scala.compiletime.ops.boolean.*

val conjunction: true && true = true
val multiplication: 3 * 5 = 15
```

연산은 표준 우선순위 규칙(standard precedence rule)을 따릅니다.

```scala
import scala.compiletime.ops.int.*
val x: 1 + 2 * 3 = 7
```

연산 타입(operation type)들은 좌변 매개변수 타입(left-hand side parameter type)을 기준으로 구성됩니다. 매치 타입(match type)을 사용하여 구현 사이를 디스패치(dispatch)할 수 있습니다.

```scala
import scala.compiletime.ops.*
import scala.annotation.infix

type +[X <: Int | String, Y <: Int | String] = (X, Y) match
  case (Int, Int) => int.+[X, Y]
  case (String, String) => string.+[X, Y]

val concat: "a" + "b" = "ab"
val addition: 1 + 1 = 2
```

이처럼 `scala.compiletime.ops.int`, `scala.compiletime.ops.boolean`, `scala.compiletime.ops.string` 등의 하위 패키지가 각 타입에 대한 연산(예: 정수의 `+`, `*`, 후속자(successor)를 나타내는 `S`, 불리언의 `&&` 등)을 제공합니다.

---

#### 선택적 given 소환(Summoning Givens Selectively)

`summonFrom` 구문은 함수형 컨텍스트(functional context)에서 암시적 탐색(implicit search)을 사용할 수 있게 합니다.

```scala
import scala.compiletime.summonFrom

inline def setFor[T]: Set[T] = summonFrom {
  case ord: Ordering[T] => new TreeSet[T]()(using ord)
  case _                => new HashSet[T]
}
```

패턴(pattern)들은 순차적으로 시도되며, 처음으로 일치하는 패턴이 선택됩니다. 패턴에 바인딩된 given 인스턴스(pattern-bound given instance)를 사용할 수도 있습니다.

```scala
import scala.compiletime.summonFrom

inline def setFor[T]: Set[T] = summonFrom {
  case given Ordering[T] => new TreeSet[T]
  case _                 => new HashSet[T]
}
```

암시적 스코프(implicit scope)에 `Ordering[String]`이 있을 때입니다.

```scala
summon[Ordering[String]]

println(setFor[String].getClass) // class scala.collection.immutable.TreeSet 출력
```

**참고:** `summonFrom`에서 모호한 given(ambiguous givens)은 오류를 발생시킵니다.

```scala
class A
given a1: A = new A
given a2: A = new A

inline def f: Any = summonFrom {
  case given _: A => ???  // error: ambiguous givens
}
```

---

#### `summonInline`

`summonInline`은 `summon`의 축약형(shorthand)으로, 인라이닝이 일어날 때까지 소환(summoning)을 지연(delay)합니다. `summonFrom`과 달리, 대상 타입의 given 인스턴스를 찾지 못하면 암시 미발견 오류(implicit-not-found error)를 발생시킵니다.

```scala
import scala.compiletime.summonInline
import scala.annotation.implicitNotFound

@implicitNotFound("Missing One")
trait Missing1

@implicitNotFound("Missing Two")
trait Missing2

trait NotMissing
given NotMissing = ???

transparent inline def summonInlineCheck[T <: Int](inline t : T) : Any =
  inline t match
    case 1 => summonInline[Missing1]
    case 2 => summonInline[Missing2]
    case _ => summonInline[NotMissing]

val missing1 = summonInlineCheck(1) // error: Missing One
val missing2 = summonInlineCheck(2) // error: Missing Two
val notMissing : NotMissing = summonInlineCheck(3)
```

> 더 자세한 내용은 타입 수준 프로그래밍을 위한 암시적 매치(implicit matches)에 관한 [PR #4768](https://github.com/scala/scala3/pull/4768), 그리고 `summonFrom` 구문에 관한 [PR #7201](https://github.com/scala/scala3/pull/7201)을 참고하세요.

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Metaprogramming](https://docs.scala-lang.org/scala3/reference/metaprogramming/)
- [Inline](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html)
- [Compile-time Operations](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html)

---

## Scala 3 메타프로그래밍: 매크로와 인용/스플라이스

---

### 목차

1. [개요](#개요)
2. [매크로(Macros)](#매크로macros)
   - [다단계 프로그래밍(Multi-Staging)](#다단계-프로그래밍multi-staging)
   - [인용된 값(Quoted Values)과 리프팅(Lifting)](#인용된-값quoted-values과-리프팅lifting)
   - [인용에서 값 추출하기](#인용에서-값-추출하기)
   - [매크로와 다단계 프로그래밍](#매크로와-다단계-프로그래밍)
   - [안전성(Safety)](#안전성safety)
   - [스테이지된 람다(Staged Lambdas)](#스테이지된-람다staged-lambdas)
   - [스테이지된 생성자와 클래스](#스테이지된-생성자와-클래스)
   - [인용 패턴 매칭(Quote Pattern Matching)](#인용-패턴-매칭quote-pattern-matching)
   - [부분식 변환(Sub-Expression Transformation)](#부분식-변환sub-expression-transformation)
   - [스테이지된 암시적 소환(Staged Implicit Summoning)](#스테이지된-암시적-소환staged-implicit-summoning)
3. [대칭 메타프로그래밍의 메타이론(Simple SMP)](#대칭-메타프로그래밍의-메타이론simple-smp)
4. [런타임 다단계 프로그래밍(스테이징, Staging)](#런타임-다단계-프로그래밍스테이징staging)
5. [리플렉션(Reflection)](#리플렉션reflection)
6. [TASTy 검사(TASTy Inspection)](#tasty-검사tasty-inspection)
7. [참고 자료](#참고-자료)

---

### 개요

> 📘 **처음 배우는 분께 — 이 장은 통째로 건너뛰어도 됩니다**
>
> 이 문서(09)는 이 묶음에서 **가장 난도가 높은, 라이브러리 제작자용 주제**입니다.
> 매크로는 "코드를 만들어 내는 코드"를 직접 짜는 기술이라, 보통의 애플리케이션을
> 만드는 데에는 거의 쓸 일이 없습니다. 직접 매크로를 만들 일이 생기기 전까지는
> **이 장 전체를 건너뛰어도 전혀 문제없습니다.** (먼저 08번 `inline` 문서를 읽고 오면
> 이 장이 한결 수월합니다. 매크로는 `inline`의 연장선이기 때문입니다.)
>
> 그래도 분위기만 맛보고 싶다면, 아래 두 비유 하나만 들고 가세요.
> - **인용(quote) `'{ ... }`** = 코드를 *실행하지 않고* "코드 조각(데이터)"으로 들고 다니는 것.
>   따옴표 안에 적은 '**말한 내용**'을 그대로 보관하는 것과 같습니다.
> - **스플라이스(splice) `${ ... }`** = 그렇게 들고 있던 코드 조각을 **"여기에 끼워 넣는 것"**.
>   문장의 빈칸에 값을 채워 넣는 것과 같습니다.

> ⚠️ **짚고 넘어가기 — Scala 2의 매크로와는 완전히 다릅니다**
>
> Scala 2에도 매크로가 있었지만(scala-reflect 기반), **실험적**(experimental)이고
> 컴파일러 내부에 강하게 얽혀 있어 다루기 어렵고 버전마다 잘 깨졌습니다.
> Scala 3의 매크로는 이를 버리고 `inline` + 인용/스플라이스라는 **새로운 토대 위에서 다시
> 설계**한 것입니다. 즉 "옛 매크로의 개선판"이 아니라 **다른 시스템**이라고 생각하세요.
> (00번 문서의 대응표에서도 `scala-reflect 매크로(실험적)` → `inline + quotes/splices`로 정리돼 있습니다.)

메타프로그래밍(metaprogramming)은 프로그램이 다른 프로그램(혹은 자기 자신)을 데이터로 다루어 생성하거나 조작하는 기법입니다. Scala 3는 컴파일 타임 코드 생성을 위한 일관된 시스템을 제공하며, 그 핵심에는 **인용(quote)** `'{..}` 과 **스플라이스(splice)** `${..}` 가 있습니다.

Scala 3의 메타프로그래밍 시스템은 다음과 같은 여러 기능으로 구성됩니다.

- **매크로(macro)**: 컴파일 타임에 평가되어 코드를 생성하는 메서드.
- **인용과 스플라이스(quotes & splices)**: 코드 조각을 타입이 부여된 값(`Expr[T]`)으로 다루는 기본 도구.
- **스테이징(staging)**: 런타임에 코드를 생성·컴파일·실행하는 다단계(multi-stage) 프로그래밍.
- **리플렉션(reflection)**: 인용을 더 낮은 수준의 타입이 부여된 추상 구문 트리(Typed-AST)로 내려가 검사·구성할 수 있는 API.
- **TASTy 검사(TASTy inspection)**: 컴파일된 `.tasty` 파일에 담긴 전체 타입 트리를 로드하여 검사하는 도구.

> **참고:** 매크로를 개발할 때에는 `-Xcheck-macros` scalac 옵션 플래그를 활성화하여 추가적인 런타임 검사를 받는 것이 좋습니다.

---

### 매크로(Macros)

#### 다단계 프로그래밍(Multi-Staging)

##### 인용된 표현식(Quoted Expressions)

> 📘 **처음 배우는 분께 — `Expr[T]`가 무엇인가**
>
> `Expr[T]`는 "**`T` 타입을 돌려주는 코드 조각**"을 담는 상자입니다.
> 예를 들어 `Expr[Int]`는 *정수 그 자체*가 아니라, **실행하면 정수가 나오는 코드**
> (`1 + 1`, `x * x` 같은)를 데이터로 들고 있는 것입니다.
> 매크로는 이런 코드 조각들을 레고 블록처럼 이어 붙여 **최종 코드를 조립**한 뒤,
> 그 결과를 컴파일러에게 "이 자리에 이 코드를 넣어라"라고 건네줍니다.

다단계 프로그래밍(multi-stage programming)은 **인용(quote)** `'{..}` 으로 실행을 지연(delay)시키고, **스플라이스(splice)** `${..}` 로 코드를 평가합니다. 인용된 표현식은 `Expr[T]` 타입을 가집니다.

다음은 $x^n$ 을 계산하는 코드를 생성하는 예입니다.

```scala
import scala.quoted.*
def unrolledPowerCode(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
  if n == 0 then '{ 1.0 }
  else if n == 1 then x
  else '{ $x * ${ unrolledPowerCode(x, n-1) } }

'{
  val x = ...
  ${ unrolledPowerCode('{x}, 3) } // 다음으로 평가됨: x * x * x
}
```

여기서 `unrolledPowerCode`는 정적으로 알려진 지수 `n`에 대해 곱셈을 **언롤(unroll)** 한 코드 조각을 만들어 냅니다. 인용 `'{..}` 안에서는 코드가 곧바로 실행되지 않고, 그 코드를 표현하는 트리(`Expr[Double]`)가 만들어집니다.

인용과 스플라이스 사이에는 다음과 같은 상쇄(cancellation) 관계가 성립합니다. 임의의 표현식 `x`, `e`에 대해

```
${'{x}} = x
'{${e}} = e
```

즉, 인용을 즉시 스플라이스하면 원래의 식으로 되돌아오며, 그 반대도 성립합니다.

> 💡 **왜 필요한가 — 인용과 스플라이스는 한 쌍의 도구**
>
> 인용 `'{..}`은 코드를 "**포장**"하고, 스플라이스 `${..}`는 포장을 **"풀어 끼워 넣는"** 도구입니다.
> 위 `unrolledPowerCode`에서 `'{ $x * ${ unrolledPowerCode(x, n-1) } }`을 보면:
> 큰 `'{ ... }`로 "곱셈 코드"를 만들고, 그 안의 `$x`와 `${ ... }`로 **이미 만들어 둔 더 작은 코드
> 조각을 빈칸에 채워 넣는** 식입니다. 그래서 `n`이 3이면 `x * x * x`라는 코드가 *컴파일 시점에*
> 완성됩니다. 위에 적힌 `${'{x}} = x` 같은 상쇄 규칙은 "포장했다가 바로 풀면 원래대로"라는,
> 직관적으로 당연한 이야기를 형식으로 적은 것입니다.

##### 추상 타입(Abstract Types)

> 📘 **처음 배우는 분께 — `Type[T]`는 "타입 정보를 담은 코드 조각"**
>
> `Expr[T]`가 *값*을 만드는 코드를 들고 다닌다면, `Type[T]`는 *타입* `T`에 대한 정보를
> 들고 다니는 상자입니다. 왜 따로 필요할까요? JVM에서는 실행 시점이 되면 제네릭 타입 정보가
> 지워집니다(**타입 소거, type erasure** — `List[Int]`나 `List[String]`이나 런타임엔 그냥 `List`).
> 그래서 인용 `'{ List[T](...) }` 안에서 `T`가 정확히 무엇인지 알려면, 그 정보를 `Type[T]`라는
> 형태로 따로 들고 들어와야 합니다. `[T: Type]`는 바로 "`Type[T]`를 같이 받겠다"는 줄임 표기입니다.

제네릭/추상 타입을 다루는 인용에는 `Type[T]` 타입 클래스가 필요합니다. 타입 소거(type erasure) 때문에 제네릭 타입 정보를 인용 안으로 전달하려면 해당 타입의 `Type` 인스턴스가 스코프에 있어야 합니다.

```scala
import scala.quoted.*
def singletonListExpr[T: Type](x: Expr[T])(using Quotes): Expr[List[T]] =
  '{ List[T]($x) } // 인용 안에서 사용된 제네릭 T

def emptyListExpr[T](using Type[T], Quotes): Expr[List[T]] =
  '{ List.empty[T] } // 인용 안에서 사용된 제네릭 T
```

다른 인스턴스가 없을 때에는 기본적으로 `Type.of[T]`가 사용됩니다. 컴파일러는 `Type.of[T]`를 특별히 처리하여, `T`가 정적으로 알려져 있거나 그 내부 타입들에 대한 암시적 `Type` 인스턴스가 있는 경우 해당 암시(implicit)를 제공합니다.

`[T: Type]`은 `(using Type[T])`의 단축 표기(컨텍스트 바운드)입니다.

##### 인용 컨텍스트(Quote Context)

인용 컨텍스트는 주어진(given) `Quotes` 인스턴스로 추적됩니다. 최상위(top-level) 스플라이스는 새로운 `Quotes` 컨텍스트를 제공합니다. 개념적으로 인용과 스플라이스의 시그니처는 다음과 같습니다.

```scala
def '[T](x: T): Quotes ?=> Expr[T] // def '[T](x: T)(using Quotes): Expr[T]

def $[T](x: Quotes ?=> Expr[T]): T
```

`?=>` 표기는 **컨텍스트 함수(contextual function)** 를 나타내며, 인수를 암시적으로 받는 람다입니다. 따라서 인용 안에서는 항상 `Quotes` 인스턴스가 암시적으로 제공됩니다.

---

#### 인용된 값(Quoted Values)과 리프팅(Lifting)

##### 리프팅(Lifting)

`Expr.apply` 메서드는 일반 값(value)을 인용된 표현으로 **리프팅(lifting)** 합니다.

```scala
val expr1plus1: Expr[Int] = '{ 1 + 1 }

val expr2: Expr[Int] = Expr(1 + 1) // 2를 '{ 2 } 로 리프팅
```

`'{ 1 + 1 }` 과 달리, `Expr(1 + 1)` 은 `1 + 1` 을 **먼저 평가한 뒤** 그 결과(2)를 리프팅합니다. 따라서 전자는 `'{ 1 + 1 }` 트리를, 후자는 `'{ 2 }` 트리를 만듭니다.

> 💡 **왜 필요한가 — 리프팅: "이미 가진 값"을 "코드 조각"으로 승격시키기**
>
> 매크로 안에서 우리는 보통의 값(예: 계산해 낸 정수 `42`)을 손에 쥐고 있을 때가 많습니다.
> 그런데 인용 `'{ ... }` 안에 그 값을 끼워 넣으려면, 값을 **그 값을 만들어 내는 코드 조각**
> (`Expr`)으로 바꿔 줘야 합니다. 이 변환이 **리프팅**(lifting)이고, `Expr(...)`이 그 일을 합니다.
> "들어 올린다(lift)"는 말 그대로, 평범한 값을 한 단계 위인 *코드의 세계*로 끌어올리는 것입니다.
> 반대 방향(코드 조각에서 실제 값을 꺼내는 것)은 바로 다음 절의 **언리프팅**(unlifting)입니다.

`Expr`은 `ToExpr` 타입 클래스를 통해 사용자가 확장할 수 있습니다.

```scala
trait ToExpr[T]:
  def apply(x: T)(using Quotes): Expr[T]
```

다음은 `Option[T]`에 대한 `ToExpr` 구현 예입니다.

```scala
given OptionToExpr: [T: {Type, ToExpr}] => ToExpr[Option[T]]:
  def apply(opt: Option[T])(using Quotes): Expr[Option[T]] =
    opt match
      case Some(x) => '{ Some[T]( ${Expr(x)} ) }
      case None => '{ None }
```

여기서 `[T: {Type, ToExpr}]`는 `T`에 대해 `Type[T]`와 `ToExpr[T]` 두 인스턴스가 모두 필요함을 의미합니다.

---

#### 인용에서 값 추출하기

`Expr.unapply` 추출자(extractor)는 인용된 상수(constant)를 매칭하여 그 안의 값을 꺼냅니다(이를 **언리프팅(unlifting)** 이라 합니다).

```scala
def powerCode(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] =
  n match
    case Expr(m) => // 상수일 경우: 코드 n='{m} 를 숫자 m 으로 언리프팅
      unrolledPowerCode(x, m)
    case _ => // 알 수 없는 경우: 런타임에 power 호출
      '{ power($x, $n) }
```

또는 `n.value`(타입은 `Option[Int]`)나, 상수가 아닐 때 에러를 발생시키는 `n.valueOrAbort`를 사용할 수 있습니다.

```scala
def powerCode(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] =
  // `n`이 상수가 아니면 에러 메시지를 출력함
  unrolledPowerCode(x, n.valueOrAbort)
```

`FromExpr` 타입 클래스는 값 추출에 다형성(polymorphism)을 제공합니다.

```scala
trait FromExpr[T]:
  def unapply(x: Expr[T])(using Quotes): Option[T]
```

---

#### 매크로와 다단계 프로그래밍

##### 매크로(Macros)

> 📘 **처음 배우는 분께 — 매크로 = "컴파일 도중에 실행되어 코드를 짜 넣는 함수"**
>
> 보통의 함수는 *프로그램이 돌아갈 때(런타임)* 실행됩니다. 매크로는 그보다 한발 빠른
> **컴파일 시점**에 실행됩니다. 그래서 `${ unrolledPowerCode('x, 2) }`라고 쓰면, 컴파일러가
> 이 `${ ... }` 안의 코드를 컴파일 도중 돌려서 `x * x`라는 **코드를 만들어 내고**, 그 결과를
> 원래 자리에 박아 넣습니다. 즉 결과 프로그램에는 `power2(x)`가 아니라 곧장 `x * x`가 들어 있게 됩니다.
> "최상위 스플라이스(top-level splice)"란 인용 `'{..}`으로 둘러싸이지 *않은*, 가장 바깥의
> `${..}`를 말하며, 바로 이것이 매크로의 시작점입니다.

**매크로(macro)** 는 인용 안에 중첩되지 않은 **최상위 스플라이스(top-level splice)** 로 구성되며, 컴파일 중에 평가됩니다.

```scala
def power2(x: Double): Double =
  ${ unrolledPowerCode('x, 2) } // x * x
```

`${ ... }` 안의 코드는 컴파일 타임에 실행되어, 그 결과 트리(`x * x`)가 `power2`의 본문을 대체합니다.

##### 인라인 매크로(Inline Macros)

매크로를 더 편리하게 사용하기 위해, 최상위 스플라이스는 `inline` 메서드 안으로 제한됩니다.

```scala
// 인라인 매크로 정의
inline def powerMacro(x: Double, inline n: Int): Double =
  ${ powerCode('x, 'n) }

// 사용자 코드
def power2(x: Double): Double =
  powerMacro(x, 2) // x * x
```

매크로 평가는 인라이닝(inlining) 과정에서 일어납니다. 그 결과, 사용자에게 노출되는 시그니처에는 `Expr` 타입이 전혀 나타나지 않습니다. `inline n: Int`로 표시된 인라인 파라미터 덕분에 `n`의 값(2)이 인라이닝 시점에 상수로 전달됩니다.

##### 완전한 인터프리터를 피하기(Avoiding a Complete Interpreter)

최상위 스플라이스에 대한 제한은 평가를 단순화하여, 컴파일러가 임의의 코드를 실행하는 완전한 인터프리터가 되지 않아도 됩니다.

- 최상위 스플라이스는 컴파일된 **정적(static) 메서드** 하나에 대한 단일 호출을 포함해야 합니다.
- 인수는 리터럴 상수, 인용된 표현식(파라미터), 타입 파라미터에 대한 `Type.of` 호출, 또는 `Quotes` 참조여야 합니다.

이러한 제한은 스플라이스 안의 스플라이스를 금지합니다.

##### 컴파일 단계(Compilation Stages)

> ⚠️ **짚고 넘어가기 — 매크로 정의와 사용은 "다른 파일/모듈"이어야 합니다**
>
> 이 제약을 처음 보면 의아할 수 있는데, 이유는 단순합니다. 매크로는 컴파일 도중에 **실행**되어야
> 하므로, 그 매크로 코드 자체는 **이미 컴파일이 끝나 있어야** 합니다. 자기 자신을 컴파일하면서
> 동시에 실행할 수는 없으니까요. 그래서 매크로 구현은 별도 모듈에 두고 먼저 컴파일해 두며,
> 그것을 쓰는 코드는 그다음 단계에서 컴파일됩니다. "닭이 먼저냐 달걀이 먼저냐"를 피하기 위한
> 규칙이라고 보면 됩니다.

매크로 구현은 미리 컴파일된(pre-compiled) 라이브러리로 제공되어야 합니다. 즉 매크로를 정의하는 코드와 그것을 사용하는 코드는 서로 다른 컴파일 단위에 있어야 합니다.

```scala
// Macro.scala
def powerCode(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] = ...
inline def powerMacro(x: Double, inline n: Int): Double =
  ${ powerCode('x, 'n) }

// Lib.scala (Macro.scala에 의존)
def power2(x: Double) =
  ${ powerCode('x, '{2}) } // powerMacro(x, 2) 호출로부터 인라인됨

// App.scala  (Lib.scala에 의존)
@main def app() = power2(3.14)
```

컴파일 단계들은 중첩된 인용으로 시각화할 수 있습니다.

```scala
'{ // 매크로 라이브러리 (컴파일 단계 1)
  def powerCode(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] =
    ...
  inline def powerMacro(x: Double, inline n: Int): Double =
    ${ powerCode('x, 'n) }
  '{ // 매크로를 사용하는 라이브러리 (컴파일 단계 2)
    def power2(x: Double) =
      ${ powerCode('x, '{2}) } // powerMacro(x, 2) 호출로부터 인라인됨
    '{ power2(3.14) /* 애플리케이션 (컴파일 단계 3) */ }
  }
}
```

컴파일러는 아직 컴파일되지 않은 매크로 호출을 감지하여 소스 컴파일을 다음 단계로 미룹니다. 매크로 정의와 사용 사이에 순환 의존(cyclic dependency)이 있으면 컴파일이 실패합니다.

##### 런타임 다단계 프로그래밍

런타임에 코드를 생성·실행하는 방법에 대해서는 [런타임 다단계 프로그래밍(스테이징)](#런타임-다단계-프로그래밍스테이징staging) 절을 참고하세요.

---

#### 안전성(Safety)

다단계 프로그래밍은 설계상 **정적으로 안전(statically safe)** 하며 **스테이지 간 안전(cross-stage safe)** 합니다.

##### 정적 안전성(Static Safety)

###### 위생성(Hygiene)

> 📘 **처음 배우는 분께 — 위생성(hygiene): "이름이 엉키지 않게 막아 준다"**
>
> 매크로가 만들어 낸 코드를 사용자의 코드 한가운데에 끼워 넣을 때, 가장 무서운 사고는
> **이름 충돌**입니다. 매크로가 내부에서 임시로 쓴 변수 `x`가, 하필 사용자 코드의 `x`와
> 부딪혀서 엉뚱한 값을 가리키게 되는 일 말입니다. C 언어의 텍스트 치환 매크로(`#define`)가
> 악명 높았던 이유가 바로 이것입니다. Scala 3의 매크로는 인용 안의 이름을 단순한 글자가 아니라
> "**어느 선언을 가리키는지까지 묶인 참조**"로 다뤄서, 이런 충돌이 **자동으로** 방지됩니다.
> 이 성질을 **위생성**(hygiene)이라고 부릅니다 — "남의 이름을 더럽히지 않는다"는 뜻입니다.

인용 컨텍스트 안에서 식별자(identifier) 이름은 심볼릭 참조(symbolic reference)로 해석되므로, 우연한 이름 충돌(재바인딩)이 일어나지 않습니다.

```scala
val x = 1
'{ val x = 2; x } // 인용 안의 x = 2 를 가리키며, 바깥의 x 가 아님
```

###### 잘 타입화됨(Well-typed)

잘 타입화된 인용은 타입 추적을 통해 잘 타입화된 코드를 생성합니다. `T` 타입의 인용에서 만들어진 `Expr[T]`는 `T`를 기대하는 위치에서만 스플라이스될 수 있습니다. `Expr`은 공변(covariant)이므로 하위 타입(subtype) 표현식의 스플라이싱이 허용됩니다.

##### 스테이지 간 안전성(Cross-Stage Safety)

###### 레벨 일관성(Level Consistency)

> 📘 **처음 배우는 분께 — "스테이징 레벨"은 코드가 사는 시간대**
>
> 매크로 세계에는 시간대가 여러 개 겹쳐 있습니다. **지금 매크로를 돌리는 시점**과,
> 그 매크로가 만들어 낸 코드가 **나중에 실제로 실행되는 시점**은 서로 다른 시간대입니다.
> 인용 `'{..}`으로 한 겹 감쌀 때마다 "한 단계 미래로" 가고, 스플라이스 `${..}`로 한 겹 풀 때마다
> "한 단계 현재로" 돌아온다고 보면 됩니다. **스테이징 레벨**은 그 시간대를 숫자로 센 것입니다.
>
> 레벨 일관성 규칙은 한 줄로: **"지금 시점의 평범한 변수를, 미래에 실행될 코드 안에 그냥 끼워
> 넣을 수는 없다."** 위 `badPower`의 `n`은 매크로를 돌리는 *지금*은 아직 값이 정해지지 않은
> 변수라서, 코드를 만드는 데 직접 쓸 수 없어 에러가 납니다. 값을 코드 조각으로 바꾸거나
> (`'n`, 리프팅), 아예 컴파일 시점에 상수로 받아야(`inline n`) 합니다.

**스테이징 레벨(staging level)** 은 (인용의 개수) - (스플라이스의 개수)로 정의됩니다. 지역 변수(local variable)는 정의된 스테이징 레벨과 동일한 레벨에서만 사용되어야 합니다.

```scala
def badPower(x: Double, n: Int): Double =
  ${ unrolledPowerCode('x, n) } // 에러: 값 `n`을 아직 알 수 없음
```

크로스 플랫폼 이식성(portability)을 위해 지역 변수는 스테이지 간 영속성(cross-stage persistence)을 갖지 않습니다.

```scala
def badPowerCode(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
  // 에러: `n`이 다음 실행 환경에서 가용하지 않을 수 있음
  '{ power($x, n) }
```

반면 전역 정의(global definition)는 스테이지 간 참조를 지원합니다.

```scala
'{ power(2, 4) } // 이미 컴파일된 power 를 가리킴
```

요약하면 다음과 같습니다.

- **지역 변수**: 정의된 스테이징 레벨에서만 접근 가능.
- **전역 변수**: 어떤 스테이징 레벨에서도 접근 가능.

###### 타입 일관성(Type Consistency)

타입 소거(type erasure) 때문에, 더 높은 스테이징 레벨에서 제네릭 타입 정보를 보존하려면 스코프에 주어진(given) `Type[T]`가 필요합니다.

```scala
def generic[T: Type](expr: Expr[T])(using Quotes): Expr[List[T]] =
  '{ List[T]($expr) } // Type[T] 가 필요함
```

###### 스코프 누출(Scope Extrusion)

인용은 가변 상태(mutable state)나 예외(exception)를 통해 스플라이스 스코프 바깥으로 누출(extrude)될 수 있습니다.

```scala
var x: Expr[T] = null
'{ (y: T) => ${ x = 'y; 1 } }
x // '{y} 값을 가지지만, 여기서 y 는 스코프에 없음
```

`run` 메서드 역시 변수를 누출시킬 수 있습니다.

```scala
'{ (x: Int) => ${ run('x); ... } }
// 다음으로 평가됨: '{ (x: Int) => ${ x; ... } } 1
```

이러한 스코프 누출은 런타임 검사로 방지됩니다. 각 `Quotes` 인스턴스는 부모 스코프를 가리키는 고유한 스코프 식별자(scope identifier)를 담고 있어 스택 구조를 이룹니다. 모든 `Expr`은 생성된 스코프를 추적하며, 스플라이싱 시에 인용의 스코프가 스플라이스의 스코프와 일치하거나 그 부모인지 검증합니다.

---

#### 스테이지된 람다(Staged Lambdas)

함수형 스테이징에는 두 가지 근본적인 추상화가 있습니다.

- `Expr[T => U]`: **다음 스테이지**에 존재하는 함수.
- `Expr[T] => Expr[U]`: **현재 스테이지**에 존재하는 함수.

이 둘 사이를 변환할 수 있습니다.

```scala
def later[T: Type, U: Type](f: Expr[T] => Expr[U]): Expr[T => U] =
  '{ (x: T) => ${ f('x) } }

def now[T: Type, U: Type](f: Expr[T => U]): Expr[T] => Expr[U] =
  (x: Expr[T]) => '{ $f($x) }
```

람다가 알려진 경우, 베타 환원(beta-reduction) 최적화를 적용할 수 있습니다.

```scala
def now[T: Type, U: Type](f: Expr[T => U]): Expr[T] => Expr[U] =
  (x: Expr[T]) => Expr.betaReduce('{ $f($x) })
```

`Expr.betaReduce`는 가능하면 가장 바깥쪽 적용(application)을 환원하고, 환원할 수 없으면 원래 표현식을 그대로 반환합니다.

---

#### 스테이지된 생성자와 클래스

##### 스테이지된 생성자(Staged Constructors)

인용된 코드는 팩토리 메서드나 `new`로 인스턴스를 생성할 수 있습니다.

```scala
'{ C(...) }     // 팩토리 메서드 호출
'{ new C(...) } // 직접 인스턴스화
```

둘 다 표준 스테이징 규칙을 따릅니다.

##### 스테이지된 클래스(Staged Classes)

인용된 코드는 지역 클래스 정의(local class definition)를 포함할 수 있습니다.

```scala
def mkRunnable(x: Int)(using Quotes): Expr[Runnable] = '{
  class MyRunnable extends Runnable:
    def run(): Unit = ... // `x`를 사용하는 사용자 정의 코드 생성
  new MyRunnable
}
```

지역 클래스는 자신을 둘러싼 인용을 벗어날 수 없으며, 인스턴스는 알려진 인터페이스(여기서는 `Runnable`)를 통해 반환됩니다.

---

#### 인용 패턴 매칭(Quote Pattern Matching)

> 💡 **왜 필요한가 — 코드 조각도 패턴 매칭으로 "뜯어볼" 수 있다**
>
> 지금까지는 코드 조각을 *만들어 내는* 이야기였습니다. 반대로, 넘겨받은 코드 조각이
> **어떤 모양인지 들여다보고** 분기하고 싶을 때가 있습니다. 그럴 때 쓰는 것이 인용 패턴입니다.
> 00번 문서에서 본 `case class`의 패턴 매칭(`case Point(x, y) => ...`)을 떠올리세요.
> 똑같은 발상을 *코드 조각*에 적용한 것이 `case '{ power($y, $m) } => ...`입니다.
> "이 코드 조각이 `power(어떤것, 어떤것)` 모양인가? 그렇다면 그 두 부분을 `y`, `m`으로 꺼내자"는
> 뜻입니다. 패턴 안의 `$y`, `$m`은 매칭된 **부분 코드 조각**을 받아 내는 자리입니다.
> 이렇게 코드를 뜯어보고 다시 조립하는 식으로, 예컨대 불필요한 연산을 최적화할 수 있습니다.

코드 분석은 표현식을 부분식(sub-expression)으로 분해할 수 있습니다.

```scala
def fusedPowCode(x: Expr[Double], n: Expr[Int])(using Quotes): Expr[Double] =
  x match
    case '{ power($y, $m) } => // (y^m)^n 형태인 경우
      fusedPowCode(y, '{ $n * $m }) // y^(n*m) 코드 생성
    case _ =>
      '{ power($x, $n) }
```

##### 부분 패턴(Sub-patterns)

인용 패턴 안의 `${..}` 에서는 일반적인 Scala 패턴을 사용할 수 있습니다.

```scala
def fusedUnrolledPowCode(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
  x match
    case '{ power($y, ${Expr(m)}) } => // (y^m)^n 형태인 경우
      fusedUnrolledPowCode(y, n * m) // y * ... * y 코드 생성
    case _ =>                        //                  ( n*m 번 )
      unrolledPowerCode(x, n)
```

다형적인 값 추출에는 `FromExpr`를 사용합니다.

```scala
given OptionFromExpr: [T: {Type, FromExpr}] => FromExpr[Option[T]]:
  def unapply(x: Expr[Option[T]])(using Quotes): Option[Option[T]] =
    x match
      case '{ Some( ${Expr(x)} ) } => Some(Some(x))
      case '{ None } => Some(None)
      case _ => None
```

##### 닫힌 패턴(Closed Patterns)

패턴은 변수가 스코프 밖으로 누출되는 것을 막습니다.

```scala
'{ (x: Int) => x + 1 } match
  case '{ (y: Int) => $z } =>
    // 매칭되면 안 됨. 그렇지 않으면: z = '{ x + 1 } 이 되어버림
```

매칭이 성립하려면 `${..}` 표현식이 패턴 정의 하에서 닫혀(closed) 있어야 합니다.

##### 고차 추상 구문(HOAS) 패턴

고차 추상 구문(Higher-Order Abstract Syntax, HOAS) 패턴 `$f(y)`(또는 `$f(y1,...,yn)`)는 부분식을 에타 확장(eta-expand)합니다.

```scala
'{ ((x: Int) => x + 1).apply(2) } match
  case '{ ((y: Int) => $f(y): Int).apply($z: Int) } =>
    // f 는 `x`에 대한 참조를 포함할 수 있음 (`$y`로 치환됨)
    // f = '{ (y: Int) => $y + 1 }
    Expr.betaReduce('{ $f($z)}) // '{ 2 + 1 } 생성
```

HOAS 패턴 `$x(y1,...,yn)`은, 표현식이 `y1,...,yn` 바깥에 정의되지 않은 패턴 변수를 포함하지 않을 때 매칭됩니다.

##### 타입 변수(Type Variables)

표현식은 알 수 없는 타입을 포함할 수 있습니다. 모든 가능성을 매칭하려면 무한히 많은 케이스가 필요할 것입니다. 타입 변수(소문자 이름)는 임의의 타입에 매칭됩니다.

```scala
def fuseMapCode(x: Expr[List[Int]]): Expr[List[Int]] =
  x match
    case '{ ($ls: List[t]).map[u]($f).map[Int]($g) } =>
      '{ $ls.map($g.compose($f)) }
    ...

fuseMapCode('{ List(1.2).map(f).map(g) }) // '{ List(1.2).map(g.compose(f)) }
fuseMapCode('{ List('a').map(h).map(i) }) // '{ List('a').map(i.compose(h))  }
```

변수 `f`와 `g`는 각각 `Expr[t => u]`와 `Expr[u => Int]`로 추론됩니다. 타입 변수가 인용 안에서 참조되려면 주어진 `Type[t]`와 `Type[u]`가 필요합니다.

타입 변수는 표현식의 정확한 타입을 복원하는 데에도 쓰입니다.

```scala
def let(x: Expr[Any])(using Quotes): Expr[Any] =
  x match
    case '{ $x: t } =>
      '{ val y: t = $x; y }

let('{1}) // `Expr[Int]`를 담은 `Expr[Any]`를 반환함
```

동일한 타입 변수에 대한 다중 참조도 가능합니다.

```scala
case '{ $x: (t, t) } =>
```

타입 변수는 패턴 시작 부분에서 명시적으로 정의할 수도 있습니다.

```scala
case '{ type t; $x: t } =>
case '{ type t >: List[Int] <: Seq[Int]; $x: t } =>
```

##### 타입 패턴(Type Patterns)

인용된 타입 패턴 `case '[..] =>` 은 타입을 검사합니다.

```scala
def empty[T: Type](using Quotes): Expr[T] =
  Type.of[T] match
    case '[String] => '{ "" }
    case '[List[t]] => '{ List.empty[t] }
    case '[type t <: Option[Int]; List[t]] => '{ List.empty[t] }
    ...
```

`Type.of[T]`는 주어진 `Type[T]` 인스턴스를 소환하며, `summon[Type[T]]`와 동등합니다.

고차 종류(higher-kinded) 타입 매칭은 다음과 같습니다.

```scala
def empty[K <: AnyKind : Type](using Quotes): Type[?] =
  Type.of[K] match
    case '[type f[X]; f] => Type.of[f]
    case '[type f[X <: Int, Y]; f] => Type.of[f]
    case '[type k <: AnyKind; k ] => Type.of[k]
```

##### 타입 검사와 캐스팅(Type Testing and Casting)

표준 `isInstanceOf[Expr[T]]`와 `asInstanceOf[Expr[T]]`는 `Expr` 클래스만 검사하고 `T`는 검사하지 않습니다(컴파일 타임 경고를 발생시킵니다). 대신 다음의 적절한 메서드를 사용해야 합니다.

```scala
expr.isExprOf[T] // Type[T]를 이용한 타입 검사
expr.asExprOf[T] // Type[T]를 이용한 타입 캐스팅
```

---

#### 부분식 변환(Sub-Expression Transformation)

`ExprMap` 메커니즘은 모든 부분식을 변환합니다.

```scala
trait ExprMap:
  def transform[T](e: Expr[T])(using Type[T])(using Quotes): Expr[T]
  def transformChildren[T](e: Expr[T])(using Type[T])(using Quotes): Expr[T] =
    ...
```

다음은 상향식(bottom-up) 변환의 예입니다.

```scala
object OptimizeIdentity extends ExprMap:
  def transform[T](e: Expr[T])(using Type[T])(using Quotes): Expr[T] =
    transformChildren(e) match // 상향식 변환
      case '{ identity($x) } => x
      case _ => e
```

`transformChildren`는 기본 연산(primitive)으로, 모든 직접 부분식에 접근하여 각각에 대해 `transform`을 호출합니다. 전달되는 타입은 해당 컨텍스트에서 부분식에 기대되는 타입(예: `Some[Int]`가 아니라 `Option[Int]`)이므로, `Some(1)`을 `None`으로 변환하는 것과 같은 안전한 변환이 가능합니다.

---

#### 스테이지된 암시적 소환(Staged Implicit Summoning)

암시적 인수(implicit argument)를 스테이지된 `Expr` 타입으로 전달할 수 있습니다.

```scala
inline def treeSetFor[T](using ord: Ordering[T]): Set[T] =
  ${ setExpr[T](using 'ord) }

def setExpr[T:Type](using ord: Expr[Ordering[T]])(using Quotes): Expr[Set[T]] =
  '{ given Ordering[T] = $ord; new TreeSet[T]() }
```

또는 `Expr.summon`을 사용하면 호출 시점의 스코프에서 암시를 소환할 수 있습니다.

```scala
def summon[T: Type](using Quotes): Option[Expr[T]]

inline def setFor[T]: Set[T] =
  ${ setForExpr[T] }

def setForExpr[T: Type]()(using Quotes): Expr[Set[T]] =
  Expr.summon[Ordering[T]] match
    case Some(ord) =>
      '{ new TreeSet[T]()($ord) }
    case _ =>
      '{ new HashSet[T] }
```

---

### 대칭 메타프로그래밍의 메타이론(Simple SMP)

> ⚠️ **짚고 넘어가기 — 이 절은 이론 증명이라 건너뛰어도 됩니다**
>
> 이 절은 "인용/스플라이스 시스템이 정말로 안전한가"를 **수학적으로 증명**하는, 언어 설계자와
> 연구자를 위한 내용이라 매크로를 쓰거나 짜는 데는 필요하지 않습니다. 딱 하나만 챙긴다면:
> 여기서 인용은 `'t`, 스플라이스는 `~t`로 표기가 바뀌고, 핵심 규칙 `~'u ==> u`는 앞에서 본
> "포장했다가 바로 풀면 원래대로(`${'{x}} = x`)"와 **똑같은 이야기**를 형식 기호로 다시 쓴 것입니다.

이 절은 원리에 기반한 메타프로그래밍(principled metaprogramming)의 단순화된 변형 이론을 제시합니다. 이 단순 버전은 두 스테이지 사이의 대화(dialogues between two stages)만을 다룹니다. 즉, 프로그램이 스플라이스를 포함하는 인용이거나 그 반대이되, 최상위에서 인용과 스플라이스를 동시에 갖지는 않도록 제한합니다.

#### 문법(Syntax)

형식 시스템은 네 가지 문법 범주를 정의합니다.

- **항(Terms)**: 변수(variable), 람다 `(x: T) => t`, 적용(application), 인용 `'t`, 스플라이스 `~t`. (이 단순 이론에서는 인용을 `'t`, 스플라이스를 `~t`로 표기합니다.)
- **단순 항(Simple terms)**: 인용이나 스플라이스를 포함하지 않는 항. 변수, 람다, 그리고 단순 항들의 적용.
- **값(Values)**: 람다 추상화(lambda abstraction)와 인용된 단순 항 `'u`.
- **타입(Types)**: 기저 타입(base type), 함수 타입 `T -> T`, 인용된 타입 `'T`.

#### 연산적 의미론(Operational Semantics)

##### 평가 규칙(Evaluation Rules)

이 계산법(calculus)에는 람다 적용에 대한 표준 베타 환원이 있습니다.

```
((x: T) => t) v  -->  [x := v]t
```

평가는 값 우선 호출(call-by-value)이며, 적용과 인용된 항에 대한 합동 규칙(congruence rule)을 가집니다.

```
t1  -->  t2           t1  -->  t2
----------- (app)     ----------- (app-arg)
t1 t  -->  t2 t      v t1  -->  v t2
```

인용된 항은 별도의 평가 관계 `==>` 를 사용하여 환원됩니다.

```
t1  ==>  t2
-----------
't1  -->  't2
```

##### 스플라이싱 규칙(Splicing Rules)

핵심이 되는 스플라이스-인용 상호작용은 인접한 쌍을 소거합니다.

```
~'u  ==>  u
```

스플라이싱은 `==>`를 사용하여 항 생성자들을 통해 전파됩니다.

```
t1  ==>  t2                          t1  ==>  t2
---------------------------------    -----------
(x: T) => t1  ==>  (x: T) => t2      t1 t  ==>  t2 t

t1  ==>  t2
-----------
u t1  ==>  u t2
```

스플라이스는 그 인수를 환원합니다.

```
t1  -->  t2
-----------
~t1  ==>  ~t2
```

#### 타이핑 규칙(Typing Rules)

판단(judgment)은 `E1 * E2 |- t: T` 형태를 가지며, 여기서 `*`는 `~` 또는 `'` 중 하나로 두 개의 서로 다른 타이핑 단계(phase)를 나타냅니다.

**변수 조회(Variable lookup)** 는 두 번째 환경을 사용합니다.

```
x: T in E2
-----------
E1 * E2 |- x: T
```

**람다 추상화(Lambda abstraction)** 는 현재 환경을 확장합니다.

```
E1 * E2, x: T1 |- t: T2
-----------------------------------
E1 * E2 |- (x: T1) => t: T1 -> T2
```

**적용(Application)** 은 표준 함수 타이핑을 따릅니다.

```
E1 * E2 |- t1: T2 -> T    E1 * E2 |- t2: T2
--------------------------------------------
E1 * E2 |- t1 t2: T
```

**인용(Quoting)** 은 위상 표지(phase marker)를 `~`에서 `'`로 반전시킵니다.

```
E2 ' E1 |- t: T
----------------
E1 ~ E2 |- 't: 'T
```

**스플라이싱(Splicing)** 은 `'`에서 `~`로 반전시킵니다.

```
E2 ~ E1 |- t: 'T
----------------
E1 ' E2 |- ~t: T
```

#### 건전성 증명(Soundness Proof)

##### 진행 정리(Progress Theorem)

잘 타입화된 항이 진행(progress)함을 보장하는 두 개의 상보적인 명제가 있습니다.

1. "만약 `E1 ~ |- t: T`라면, `t = v`(어떤 값 `v`에 대해)이거나 `t --> t2`(어떤 항 `t2`에 대해)이다."
2. "만약 `' E2 |- t: T`라면, `t = u`(어떤 단순 항 `u`에 대해)이거나 `t ==> t2`(어떤 항 `t2`에 대해)이다."

증명은 항에 대한 구조적 귀납법(structural induction)으로 진행되며, 변수, 람다, 적용, 그리고 인용·스플라이스의 환원 규칙을 각각 따로 다룹니다.

##### 치환 보조정리(Substitution Lemma)

이 계산법은 두 타이핑 단계에 걸쳐 치환 성질(substitution property)을 유지합니다.

1. "만약 `E1 ~ E2 |- s: S`이고 `E1 ~ E2, x: S |- t: T`라면, `E1 ~ E2 |- [x := s]t: T`이다."
2. "만약 `E1 ~ E2 |- s: S`이고 `E2, x: S ' E1 |- t: T`라면, `E2 ' E1 |- [x := s]t: T`이다."

증명은 인용된 항 `'t1`에 치환하는 것이 그 내부 항에 상보적인 치환 보조정리를 적용하는 것과 같음을 관찰함으로써, 두 단계에 걸친 인용된 항들을 연결합니다.

##### 보존 정리(Preservation Theorem)

타입 안전성(type safety)은 환원을 통해 유지됩니다.

1. "만약 `E1 ~ E2 |- t1: T`이고 `t1 --> t2`라면, `E1 ~ E2 |- t2: T`이다."
2. "만약 `E1 ' E2 |- t1: T`이고 `t1 ==> t2`라면, `E1 ' E2 |- t2: T`이다."

평가 도출(evaluation derivation)에 대한 귀납법으로 진행되는 이 증명은 인용된 항의 환원과, 핵심이 되는 스플라이스-인용 상쇄(`~'u ==> u`)가 내부 단순 항의 타이핑 판단을 보존함을 다룹니다.

---

### 런타임 다단계 프로그래밍(스테이징, Staging)

#### 개요

> 💡 **왜 필요한가 — 스테이징: 매크로를 "런타임"으로 옮긴 것**
>
> 매크로는 코드 조각을 만들어 **컴파일 시점**에 끼워 넣었습니다. 스테이징(staging)은 같은
> 인용/스플라이스 도구를 쓰되, 코드 조각을 **프로그램이 실행되는 중에** 조립하고, `run`으로
> 그 자리에서 **컴파일·실행**합니다. 즉 "프로그램이 돌아가는 도중에, 입력에 맞춰 최적화된
> 새 코드를 즉석에서 만들어 실행"하는 것입니다. 예컨대 사용자가 입력한 수식이나 쿼리를 받아
> 그에 꼭 맞는 빠른 함수를 런타임에 생성하는 식입니다.

스테이징(staging)은 코드를 이후 스테이지에서 실행되도록 준비(stage)할 수 있게 합니다. 이 프레임워크는 `Expr[T]` 값을 런타임에 타입이 부여된 구문 트리(typed syntax tree)로 다룰 수 있게 함으로써, 컴파일 타임 메타프로그래밍과 다단계 프로그래밍을 통합합니다.

#### 핵심 개념

코드가 실행되는 단계(phase)는 인용과 스플라이스 스코프의 개수 차이로 결정됩니다.

> "코드가 실행되는 단계는, 그것이 둘러싸인 스플라이스 스코프의 개수와 인용 스코프의 개수의 차이로 결정된다."

- **스플라이스가 인용보다 많음**: 컴파일 타임에 실행(매크로).
- **스플라이스와 인용이 같음**: 일반적인 컴파일 및 실행.
- **인용이 스플라이스보다 많음**: 런타임 코드 생성(스테이징).

#### API 레퍼런스

주요 API는 `scala.quoted.staging`에 있습니다.

```scala
package scala.quoted.staging

def run[T](expr: Quotes ?=> Expr[T])(using Compiler): T = ...

def withQuotes[T](thunk: Quotes ?=> T)(using Compiler): T = ...
```

핵심 차이점: `$`와 `run`은 모두 `Expr[T]`에서 `T`로 사상하지만, `$`만이 스테이지 간 안전성(Cross-Stage Safety)의 적용을 받으며, `run`은 단지 일반 메서드일 뿐이다.

#### 프로젝트 설정

##### 템플릿 사용

```bash
sbt new scala/scala3-staging.g8
```

##### 수동 설정

`build.sbt`에 다음을 추가합니다.

```scala
libraryDependencies += "org.scala-lang" %% "scala3-staging" % scalaVersion.value
```

직접 컴파일하려면 다음과 같이 합니다.

```bash
scalac -with-compiler -d out Test.scala
scala -with-compiler -classpath out Test
```

#### 실전 예제

```scala
import scala.quoted.*

// 런타임 코드 생성을 위한 컴파일러 제공
given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

val power3: Double => Double = staging.run {
  val stagedPower3: Expr[Double => Double] =
    '{ (x: Double) => ${ unrolledPowerCode('x, 3) } }
  println(stagedPower3.show) // "((x: scala.Double) => x.*(x.*(x)))" 출력
  stagedPower3
}

power3.apply(2.0) // 8.0 반환
```

이 예제는 런타임에 거듭제곱 함수를 생성하고, 그 표현을 출력한 뒤, 실행하는 과정을 보여줍니다.

#### 스플라이스에 대한 제한

프레임워크는 세 가지 제약을 강제합니다.

1. 최상위 스플라이스는 `inline` 메서드 안에 나타나야 합니다(이로써 매크로가 됨).
2. 스플라이스는 이전에 컴파일된 메서드를, 인용된·상수·인라인 인수와 함께 호출해야 합니다.
3. 사이에 인용이 없는 중첩된 스플라이스는 금지됩니다.

#### withQuotes 기능

`withQuotes` 메서드는 표현식을 평가하지 않고 `Quotes` 컨텍스트만 제공합니다. 스테이징 컨텍스트 안에서 표현식을 내성(introspection)하거나 조작할 때 유용합니다.

---

### 리플렉션(Reflection)

#### 개요

> 📘 **처음 배우는 분께 — 리플렉션: "코드의 X-레이 사진"을 다루는 저수준 도구**
>
> 인용/스플라이스(`'{..}`, `${..}`)는 코드를 **눈에 보이는 문법 그대로** 다룹니다. 편하지만,
> 그 문법으로 표현할 수 없는 세밀한 조작(예: 변수 선언을 직접 만들고, 심볼을 뜯어보고, 소스
> 위치를 읽는 일)은 못 합니다. **리플렉션 API**는 코드를 컴파일러가 내부에서 보는 모습 그대로,
> 즉 **추상 구문 트리**(AST)라는 잘게 쪼갠 트리 구조로 내려가 다루게 해 줍니다. 의료로 치면
> 인용은 "겉모습", 리플렉션은 "X-레이 사진"인 셈입니다. `import quotes.reflect.*`로 이 도구함을
> 열며, `asTerm`(인용 → 트리), `asExpr`(트리 → 인용)으로 두 세계를 오갑니다.
>
> **AST**(추상 구문 트리)란? 코드 `val foo: Int = 0`을 컴파일러가 `ValDef(foo, Ident(Int),
> Literal(Constant(0)))`처럼 "이름·타입·초깃값"으로 분해해 트리로 만든 것입니다. 우리가 읽는
> 글자 모양 대신, 기계가 다루기 좋은 부품 단위로 쪼개 놓은 형태라고 보면 됩니다.

리플렉션(reflection)은 타입이 부여된 추상 구문 트리(Typed Abstract Syntax Trees, Typed-AST)를 검사하고 구성할 수 있게 합니다. 인용된 표현식, 인용된 타입, 그리고 완전한 TASTy 파일에 적용됩니다. 이 API는 `scala.quoted.Quotes`를 통해 매크로 컨텍스트 안에서 동작합니다.

#### 리플렉션 API에 접근하기

리플렉션 기능을 활성화하려면 암시적 `Quotes` 파라미터를 추가하고 그 `reflect` 모듈을 임포트합니다.

```scala
import scala.quoted.*

inline def natConst(inline x: Int): Int = ${natConstImpl('{x})}

def natConstImpl(x: Expr[Int])(using Quotes): Expr[Int] =
  import quotes.reflect.*
  // 여기에 리플렉션 코드 작성
```

#### 표현식과 트리 사이의 변환

기본은 세 가지 변환 메서드입니다.

- **`asTerm`**: `Expr[T]`로부터 그 기반이 되는 타입 부여 AST를 추출하여 `Term`을 반환합니다.
- **`asExpr`**: `Term`을 다시 `Expr[Any]`로 변환합니다.
- **`asExprOf[T]`**: `Term`을 `Expr[T]`로 변환하며, 타입이 일치하지 않으면 매크로 확장 시점에 예외를 발생시킵니다.

```scala
val term: Term = x.asTerm
val expr: Expr[Any] = term.asExpr
val typedExpr: Expr[Int] = term.asExprOf[Int]
```

#### 트리 구조(Tree Structures)

##### 타입이 부여된 추상 구문 트리(Typed-AST)

트리는 타이핑 이후의 프로그램 코드를 표현합니다. 두 가지 주요 범주가 있습니다.

- **항(Terms)**: 타입이 연관된 표현식으로, `.asExpr`을 통해 `Expr`로 변환할 수 있습니다. 예: `Literal(Constant(0))`은 정수 0을 표현합니다.
- **트리(Trees)**: 일반적인 AST 노드. 예: `ValDef(foo,Ident(Int),Literal(Constant(0)))`은 `val foo: Int = 0`을 표현합니다.

항과 트리를 함께 결합한 블록은 다음과 같습니다.

```scala
Block(
  List(ValDef(foo,Ident(Int),Literal(Constant(0)))),
  Apply(Select(Ident(foo),+), List(Literal(Constant(1))))
)
```

이는 다음 코드를 표현합니다.

```scala
val foo: Int = 0
foo + 1
```

##### 트리 추출자와 생성자(Tree Extractors and Constructors)

패턴 매칭으로 트리 구조를 추출합니다. `Printer.TreeStructure` 유틸리티는 트리의 구성을 드러냅니다.

```scala
def natConstImpl(x: Expr[Int])(using Quotes): Expr[Int] =
  import quotes.reflect.*
  val tree: Term = x.asTerm
  tree match
    case Inlined(_, _, Literal(IntConstant(n))) =>
      if n <= 0 then
        report.error("Parameter must be natural number")
        '{0}
      else
        tree.asExprOf[Int]
    case _ =>
      report.error("Parameter must be a known constant")
      '{0}
```

트리 구조는 다음과 같이 표시합니다.

```scala
tree.show(using Printer.TreeStructure)
// 또는
Printer.TreeStructure.show(tree)
```

#### 심볼(Symbols)

심볼(Symbol)은 이름이 부여된 선언(declaration)과 정의(definition)를 나타냅니다. `Symbol.newVal`과 같은 팩토리 메서드로 새 심볼을 생성합니다.

```scala
import quotes.reflect._
val fooSym = Symbol.newVal(
  parent = Symbol.spliceOwner,
  name = "foo",
  tpe = TypeRepr.of[Int],
  flags = Flags.EmptyFlags,
  privateWithin = Symbol.noSymbol
)
val tree = ValDef(fooSym, Some(Literal(IntConstant(0))))
```

코드에서 심볼을 참조할 때 값에는 `Ref(fooSym)`을, 타입에는 `TypeIdent`를 사용합니다.

##### 플래그(Flags)

플래그는 심볼의 다양한 속성을 나타냅니다. 접근 제어자(access modifier), 언어 기원(origin), 그리고 `inline`이나 `transparent` 같은 특수 속성이 포함됩니다. 플래그는 비트셋(bit-set) 구현을 사용합니다.

- `.is`: 어떤 플래그 부분집합이 존재하는지 검사.
- `.|`: 플래그 합집합(union).
- `.&`: 플래그 교집합(intersect).

심볼 종류에 따라 허용되는 플래그가 다르며, 자세한 내용은 API 문서를 참고하세요.

#### TypeRepr와 TypeTree

타입 표현은 두 가지 형태로 존재합니다.

- **TypeRepr**: 리플렉션 API의 타입 표현. `Type[T]`로부터 얻습니다.

```scala
typeRepr.asType match
  case '[t] =>
    // 주어진 Type[t] 에 접근
```

- **TypeTree**: 타입의 트리 형태. 트리 안에 타입을 삽입할 때(예: `TypeApply`의 인수) 사용됩니다.

##### 타입 표현 예시

`List[String]` 타입은 다음이 됩니다.

```scala
AppliedType(
  TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class collection)),object immutable),List),
  List(TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class java)),object lang),String))
)
```

주요 구성 요소는 다음과 같습니다.

- **TypeRef(prefix, typeSymbol)**: 접두사(prefix) 내에서의 타입 선택.
- **TermRef(prefix, termSymbol)**: 항 선택. 경로 의존 타입(path-dependent type)에 유용합니다.

##### 심볼에서 TypeRepr 추출하기

심볼은 불완전한 타입 정보를 담고 있으므로, 접두사를 고려한(prefix-aware) 메서드를 사용합니다.

```scala
val prefix = TypeRepr.of[Outer[String]]
val innerSymbol = Symbol.classMember
prefix.memberType(innerSymbol)
// 또는
prefix.select(innerSymbol)
```

#### API 문서 탐색

전체 Quotes 리플렉션 API는 `reflectModule` 트레이트에 있습니다. 메서드는 다음과 같이 나뉩니다.

- **`_Module`**: `apply`, `unapply` 같은 정적 메서드.
- **`_Methods`**: 특정 타입에 대한 인스턴스 메서드.

#### 위치(Positions)

`Position` 객체는 매크로 확장 위치 정보를 제공합니다.

```scala
def macroImpl()(quotes: Quotes): Expr[Unit] =
  import quotes.reflect.*
  val pos = Position.ofMacroExpansion

  val jpath = pos.sourceFile.getJPath.getOrElse(
    report.errorAndAbort("virtual file not supported", pos)
  )
  val path = pos.sourceFile.path
  val start = pos.start
  val end = pos.end
  val startLine = pos.startLine
  val endLine = pos.endLine
  val startColumn = pos.startColumn
  val endColumn = pos.endColumn
  val sourceCode = pos.sourceCode
```

#### 트리 유틸리티(Tree Utilities)

##### TreeAccumulator

타입 `X`의 데이터를 누적하며 트리를 순회합니다.

```scala
def collectPatternVariables(tree: Tree)(using ctx: Context): List[Symbol] =
  val acc = new TreeAccumulator[List[Symbol]]:
    def foldTree(syms: List[Symbol], tree: Tree)(owner: Symbol):
        List[Symbol] = tree match
      case ValDef(_, _, rhs) =>
        val newSyms = tree.symbol :: syms
        foldTree(newSyms, body)(tree.symbol)
      case _ =>
        foldOverTree(syms, tree)(owner)
  acc(Nil, tree)
```

주요 메서드는 다음과 같습니다.

- **`foldTree`**: 누적 로직을 정의하기 위해 오버라이드합니다.
- **`foldOverTree`**: 각 자식에 `foldTree`를 적용합니다.

##### TreeTraverser

`TreeAccumulator[Unit]`을 확장하며, 반환값 없이 부수효과(side-effect)만을 위한 순회를 수행합니다.

##### TreeMap

메서드 오버라이딩을 통해 순회 중에 트리를 변환합니다.

```scala
val mapper = new TreeMap:
  override def transformStatement(tree: Statement): Statement = ???
  override def transformTerm(tree: Term): Term = ???
```

#### ValDef.let

우변(right-hand side) 표현식을 변수에 바인딩하여 본문에서 참조합니다.

```scala
def let(rhs: Term)(body: Ident => Term): Term = ...

def lets(terms: List[Term])(body: List[Term] => Term): Term = ...
```

사용 예:

```scala
ValDef.let(someExpr) { ident =>
  // 본문 전체에서 ident 를 사용
}
```

> **중요한 주의사항:** 인용 리플렉션을 사용하면 이러한 보장이 깨질 수 있고 매크로 확장 시점에 실패할 수 있으므로, 추가적인 명시적 검사를 수행해야 합니다. 리플렉션 API를 사용할 때의 타입 안전성은 프로그래머의 책임입니다.

> ⚠️ **짚고 넘어가기 — 리플렉션은 "안전벨트를 푼" 단계입니다**
>
> 앞에서 인용/스플라이스가 위생성·타입 안전성·레벨 일관성으로 우리를 지켜 준다고 했는데,
> 리플렉션은 그보다 한 단계 아래로 내려가는 만큼 그 보호가 **자동으로 보장되지 않습니다.**
> 잘못된 트리를 만들면 컴파일이 아니라 *매크로를 펼치는 도중*에 터질 수 있습니다. 그래서
> 리플렉션 코드에서는 "이 트리가 정말 기대한 타입/모양인지"를 `isExprOf`·`asExprOf` 같은
> 메서드로 **직접 확인하는 책임이 작성자에게** 있습니다. 가능하면 인용/스플라이스로 해결하고,
> 정 안 될 때만 리플렉션으로 내려가는 것이 안전합니다.

---

### TASTy 검사(TASTy Inspection)

#### 개요

> 📘 **처음 배우는 분께 — TASTy는 "컴파일된 코드의 완전한 설계도"**
>
> 자바에서 `.class` 파일은 바이트코드만 담아, 제네릭 타입 같은 정보가 상당히 지워져 있습니다.
> Scala 3는 그와 별도로 `.tasty` 파일을 남기는데, 여기에는 타입·소스 위치·문서까지 포함한
> **코드의 온전한 트리**(앞서 본 AST)가 그대로 들어 있습니다. 그래서 컴파일이 끝난 라이브러리도
> `.tasty` 파일만 있으면 그 구조를 정확히 들여다볼 수 있습니다. **TASTy 검사**(inspection)는
> 이 파일을 읽어 분석하는 기능으로, 린터·문서 생성기·코드 분석 도구를 만들 때 쓰입니다.
> 일반 매크로 작성과는 다소 별개의, 도구 제작용 주제입니다.

TASTy 파일은 소스 위치(source position)와 문서(documentation)를 포함한 클래스의 완전한 타입 트리(typed tree)를 담고 있습니다. 이 특성 덕분에 TASTy는 의미론적 코드 분석이 필요한 도구에 특히 유용합니다.

TASTy 파일과의 상호작용을 단순화하기 위해 Scala는 `Inspector` API를 제공하며, TASTy 내용을 TASTy 리플렉트 인터페이스를 통해 로드하고 노출합니다.

#### TASTyViz

TASTyViz는 TASTy 파일을 검사하기 위한 시각화 도구입니다. 아직 초기 개발 단계이지만, TASTy 구조를 이해하는 데 유용한 디버깅 자원입니다. 프로젝트는 [github.com/shardulc/tastyviz](https://github.com/shardulc/tastyviz)에서 이용할 수 있습니다.

#### Inspector API

##### 의존성 설정

sbt 빌드에 다음을 추가합니다.

```scala
libraryDependencies += "org.scala-lang" %% "scala3-tasty-inspector" % scalaVersion.value
```

##### Inspector 생성하기

`Inspector` 트레이트를 확장하여 소비자(consumer)를 정의합니다.

```scala
import scala.quoted.*
import scala.tasty.inspector.*

class MyInspector extends Inspector:
   def inspect(using Quotes)(tastys: List[Tasty[quotes.type]]): Unit =
      import quotes.reflect.*
      for tasty <- tastys do
         val tree = tasty.ast
         // 트리로 무언가를 수행
```

##### Inspector 실행하기

인스턴스화하여 실행합니다.

```scala
object Test:
   def main(args: Array[String]): Unit =
      val tastyFiles = List("foo/Bar.tasty")
      TastyInspector.inspectTastyFiles(tastyFiles)(new MyInspector)
```

##### 실행 방법

컴파일 후, 런타임에 컴파일러를 사용할 수 있도록 합니다.

```bash
scalac -d out Test.scala
scala -with-compiler -classpath out Test
```

#### 템플릿 프로젝트

sbt 1.1.5 이상을 사용하여 빠르게 설정하려면 다음과 같이 합니다.

```bash
sbt new scala/scala3-tasty-inspector.g8
```

선택한 디렉터리에 미리 구성된 프로젝트 템플릿을 생성합니다.

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Metaprogramming](https://docs.scala-lang.org/scala3/reference/metaprogramming/)
- [Macros](https://docs.scala-lang.org/scala3/reference/metaprogramming/macros.html)
- [The Meta-theory of Symmetric Metaprogramming](https://docs.scala-lang.org/scala3/reference/metaprogramming/simple-smp.html)
- [Run-Time Multi-Stage Programming (Staging)](https://docs.scala-lang.org/scala3/reference/metaprogramming/staging.html)
- [Reflection](https://docs.scala-lang.org/scala3/reference/metaprogramming/reflection.html)
- [TASTy Inspection](https://docs.scala-lang.org/scala3/reference/metaprogramming/tasty-inspect.html)
