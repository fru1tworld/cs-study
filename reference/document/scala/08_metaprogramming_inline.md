# Scala 3 메타프로그래밍: inline과 컴파일 타임 연산

---

## 목차

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

## 메타프로그래밍 개요

> 📘 **처음 배우는 분께 — 메타프로그래밍이란**
>
> 메타프로그래밍(metaprogramming)은 한마디로 **"프로그램을 다루는 프로그램"**입니다.
> 보통 우리가 짜는 코드는 숫자나 문자열 같은 *데이터*를 다루지만, 메타프로그래밍은
> *코드 그 자체*를 데이터처럼 다뤄서, 컴파일 시점에 **코드를 생성하거나 미리 계산**합니다.
> 예를 들어 "이 함수 호출을 컴파일 때 풀어서 더 빠른 코드로 바꿔라" 같은 일을 시킵니다.
>
> ⚠️ 이 장(08)은 Scala에서 **가장 고급 주제**입니다. 00번 추천 학습 순서에서도 맨 마지막입니다.
> 처음 배우는 분이라면 **지금은 통째로 건너뛰어도 전혀 문제없습니다.** 나중에 라이브러리를
> 직접 만들거나 컴파일 타임 최적화가 필요해질 때 돌아오면 됩니다.

Scala 3의 레퍼런스 문서는 여러 가지 기본 기능 위에 구축된, 새롭게 설계된 메타프로그래밍(metaprogramming) 시스템을 소개합니다.

### 핵심 기능들

**인라인(inline)** 은 정의(definition)가 사용 지점(point of use)에서 인라인될 것임을 보장하는 수정자(modifier)입니다. Scala에서의 인라이닝(inlining)은 컴파일러에 대한 단순한 제안(suggestion)이 아니라, 타이퍼(Typer) 단계에서 실행되는 명령(command)입니다. 이 메커니즘은 타입 수준 프로그래밍(type-level programming), 매크로(macros), 코드 생성(code generation)을 포함한 다운스트림(downstream) 컴파일 타임 연산(compile-time operation)을 가능하게 합니다.

> 📘 **처음 배우는 분께 — 컴파일 시점 vs 런타임**
>
> 이 장을 읽을 때 가장 중요한 구분은 **"언제 일어나느냐"**입니다.
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

## 인라인(Inline)

### 인라인 정의(Inline Definitions)

> 📘 **처음 배우는 분께 — inline의 직관**
>
> `inline`은 **"이 함수 호출을, 호출한 자리에 코드를 그대로 펼쳐 넣어라"**는 지시입니다.
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

### 예제 확장(Example Expansion)

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

### 재귀 인라인 메서드(Recursive Inline Methods)

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

### 인라인 매개변수(Inline Parameters)

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

### 오버라이딩 규칙(Rules for Overriding)

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

### `@inline`과의 관계(Relationship to @inline)

Scala 2는 백엔드 인라이닝 힌트(backend inlining hint)로서 `@inline` 애너테이션을 정의합니다. `inline` 수정자는 이보다 더 강력합니다.

- 확장(expansion)이 최선의 노력(best effort)이 아니라 **보장(guaranteed)** 됩니다.
- 확장이 백엔드(backend)가 아니라 **프런트엔드(frontend)** 에서 일어납니다.
- 확장이 **재귀 메서드(recursive method)** 에도 적용됩니다.

---

### 상수 표현식의 정의(The Definition of Constant Expression)

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

### 투명 인라인 메서드(Transparent Inline Methods)

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

### 투명 인라인과 일반 인라인의 차이(Transparent vs. Non-transparent Inline)

투명 인라인 메서드(transparent inline method)는 타입 검사 중(during type checking)에 확장됩니다. 그 외의 인라인 메서드는 타이핑(typing) 이후에 인라인됩니다.

```scala
inline def f1: T = ...
transparent inline def f2: T = (...): T
```

핵심적인 차이점은 다음과 같습니다. `transparent inline given`에서는 인라이닝 도중 발생한 오류(error)가 암시적 탐색 불일치(implicit search mismatch)로 간주되어 탐색(search)이 계속됩니다. `transparent inline given`의 우변(RHS)에 타입 어스크립션(type ascription)을 추가하면, 특수화된 타입을 피하면서도 탐색 동작은 유지할 수 있습니다. 반면 `inline given`은 타이핑 이후에 인라인되며, 오류는 일반적으로 그대로 보고(emit)됩니다.

---

### 인라인 조건문(Inline Conditionals)

> 💡 **왜 필요한가 — `inline if`는 "약속을 강제"한다**
>
> 그냥 `if`는 조건이 상수이면 컴파일러가 알아서 한쪽 분기만 남길 *수도* 있습니다(될 수도, 안 될 수도).
> `inline if`는 **"이 조건은 반드시 컴파일 시점 상수여야 한다"**고 못 박는 것입니다.
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

### 인라인 매치(Inline Matches)

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

## 컴파일 타임 연산(Compile-time Operations)

### `scala.compiletime` 패키지

> 📘 **처음 배우는 분께 — scala.compiletime 패키지**
>
> `scala.compiletime`은 **오직 컴파일 시점에만 동작하는 도구 상자**입니다.
> 여기 들어 있는 함수들(`constValue`, `erasedValue`, `error`, `summonInline` 등)은 보통의
> 라이브러리 함수처럼 런타임에 실행되는 게 아니라, inline으로 코드를 펼치는 *과정에서* 컴파일러가
> 처리하고 사라집니다. 그래서 대부분 `inline def`, `inline match`와 짝을 지어 쓰입니다.
> "런타임에 직접 부르면 오류"라는 설명이 여러 번 나오는 이유가 이것입니다.

[`scala.compiletime`](https://scala-lang.org/api/3.x/scala/compiletime.html) 패키지는 값(value)에 대한 컴파일 타임 연산(compile-time operation)을 위한 헬퍼 정의(helper definition)들을 제공합니다.

---

### `constValue`와 `constValueOpt`

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

### `erasedValue`

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

### `error`

> 💡 **왜 필요한가 — 직접 만드는 컴파일 오류**
>
> `error`는 **"내가 정한 조건이 안 맞으면 컴파일 자체를 실패시키고 친절한 메시지를 띄워라"**라고
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

### `scala.compiletime.ops` 패키지

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

### 선택적 given 소환(Summoning Givens Selectively)

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

### `summonInline`

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

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Metaprogramming](https://docs.scala-lang.org/scala3/reference/metaprogramming/)
- [Inline](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html)
- [Compile-time Operations](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html)
