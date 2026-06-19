# Scala 3 기타 새 기능: 문법과 언어 편의 기능

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Other New Features" 섹션 일부(들여쓰기, 제어 문법, 최상위 정의 등)를 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/other-new-features/

---

## 목차

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

## 1. 선택적 중괄호 — 들여쓰기 문법(Optional Braces / Significant Indentation)

Scala 3는 의미 있는 들여쓰기(significant indentation) 처리를 도입합니다. 이를 통해 많은 문맥에서 중괄호(`{}`)를 선택적으로 사용할 수 있게 되며, 동시에 들여쓰기 규칙을 강제하여 흔히 발생하는 오류를 잡아냅니다. 이 기능은 컴파일러 플래그 `-no-indent`로 비활성화할 수 있습니다.

### 1.1 들여쓰기 규칙(Indentation Rules)

컴파일러는 두 가지 규칙을 강제하며, 규칙을 위반하면 경고(warning)로 표시합니다.

1. **중괄호로 구분된 영역 규칙(Brace-delimited region rule)**: 중괄호 내부에서, 여는 중괄호 뒤의 첫 줄바꿈 문(new-line statement)보다 더 왼쪽에서 시작하는 문장은 존재할 수 없습니다. 이 규칙은 닫는 중괄호가 빠진 경우를 감지하는 데 도움이 됩니다.

2. **들여쓰기 폭 규칙(Indentation width rule)**: 의미 있는 들여쓰기가 꺼져 있을 때, 표현식의 들여쓰기된 하위 부분이 줄바꿈으로 끝나는 경우, 그 다음 문장은 해당 하위 부분보다 더 작은 들여쓰기 폭에서 시작해야 합니다. 이 규칙은 여는 중괄호를 잊어버린 경우를 방지합니다.

### 1.2 선택적 중괄호 메커니즘(Optional Braces Mechanism)

컴파일러는 줄바꿈(line break) 지점에 `<indent>`와 `<outdent>` 토큰을 삽입합니다. 이 토큰들은 중괄호 쌍 `{...}`와 동등하게 동작합니다. 이 토큰들은 들여쓰기 폭 스택(indentation width stack, `IW`)을 통해 관리됩니다.

#### 들여쓰기 삽입 규칙(Indent Insertion Rules)

`<indent>` 토큰은 다음 조건이 모두 만족될 때 삽입됩니다.

- 현재 위치에서 들여쓰기 영역(indentation region)이 시작될 수 있고,
- 다음 줄의 첫 토큰이 현재보다 더 깊은 들여쓰기를 가질 때

들여쓰기 영역은 다음 위치 뒤에서 시작될 수 있습니다.

- 확장(extension)의 선행 매개변수(leading parameters) 뒤
- given 인스턴스에서 `with` 뒤
- 템플릿 본문(template body) 시작 부분의 `:` 뒤
- 다음 토큰들 뒤: `=`, `=>`, `?=>`, `<-`, `catch`, `do`, `else`, `finally`, `for`, `if`, `match`, `return`, `then`, `throw`, `try`, `while`, `yield`
- 구식(old-style) `if`/`while` 조건의 닫는 `)` 뒤
- 구식 `for` 루프 열거자(enumerations)의 닫는 `)` 또는 `}` 뒤

#### 내어쓰기 삽입 규칙(Outdent Insertion Rules)

`<outdent>` 토큰은 다음 조건이 모두 만족될 때 삽입됩니다.

- 다음 줄의 첫 토큰이 현재보다 더 얕은 들여쓰기를 가지고,
- 이전 줄이 다음과 같은 연속(continuation) 토큰으로 끝나지 않을 때: `then`, `else`, `do`, `catch`, `finally`, `yield`, `match`

선행 중위 연산자(leading infix operators)의 경우, 올바른 파싱 우선순위를 보장하기 위한 특별한 규칙이 적용됩니다.

### 1.3 템플릿 본문 주위의 선택적 중괄호(Optional Braces Around Template Bodies)

템플릿 본문(클래스, 트레이트, 객체 정의)은 콜론 기반(colon-based) 문법을 사용하여 중괄호를 생략할 수 있습니다.

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

`<colon>` 토큰(`:`)은 뒤에 들여쓰기된 내용이 따라올 때 중괄호를 대체하며, `{` `<indent>` ... `<outdent>` `}`와 같이 동작합니다.

### 1.4 메서드 인자에 대한 선택적 중괄호(Optional Braces for Method Arguments)

Scala 3.3부터, 콜론은 들여쓰기된 블록으로 함수 인자(function arguments)를 도입할 수 있습니다.

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

람다 매개변수(lambda parameters)는 콜론과 같은 줄에 위치할 수 있습니다.

```scala
xs.map: x =>
  val y = x - 1
  y * y

xs.foldLeft(0): (x, y) =>
  x + y
```

### 1.5 공백과 탭(Spaces vs Tabs)

들여쓰기 접두부(indentation prefix)는 공백(space) 및/또는 탭(tab)으로 구성되며, 문자열 접두 관계(string prefix relation)에 따라 순서가 정해집니다. 공백과 탭을 혼용하는 것은 오류를 유발하기 쉬우므로 한 소스 파일 내에서는 권장되지 않습니다. 비교 불가능한(incomparable) 들여쓰기 폭(예: "탭 6개" vs "공백 4개")은 오류(error)를 발생시킵니다.

### 1.6 들여쓰기와 중괄호의 상호작용(Indentation and Braces Interaction)

중괄호 영역 내에서도 들여쓰기 규칙이 다음 원칙에 따라 적용됩니다.

1. 여러 줄 중괄호 영역(multiline brace region)의 들여쓰기 폭은, `{` 뒤 새 줄에서 첫 토큰의 들여쓰기입니다.
2. 여러 줄 대괄호/괄호(brackets/parentheses)의 들여쓰기 폭은 다음과 같습니다.
   - 대괄호/괄호가 줄을 끝맺는 경우: 그 다음에 오는 토큰의 들여쓰기
   - 그 외의 경우: 감싸는 영역(enclosing region)의 들여쓰기
3. 닫는 `}`, `]`, `)`는 중첩된 영역을 닫기 위해 필요한 `<outdent>` 토큰을 발생시킵니다.

### 1.7 case 절의 특별 처리(Case Clause Special Treatment)

match 표현식과 catch 절에는 정교한 규칙이 적용됩니다.

- `match` 또는 `catch` 뒤에는, `case`가 `match`의 현재 들여쓰기와 같은 위치에 나타나더라도 들여쓰기 영역이 열립니다.
- 이 영역은 해당 들여쓰기에서 `case`가 아닌 첫 토큰, 또는 더 얕게 들여쓰기된 임의의 토큰에서 닫힙니다.

이를 통해 들여쓰기되지 않은(unindented) case를 작성할 수 있습니다.

```scala
x match
case 1 => print("I")
case 2 => print("II")
case 3 => print("III")

println(".")
```

### 1.8 들여쓰기를 통한 문장 연속(Statement Continuation via Indentation)

가상 세미콜론(virtual semicolons)은 두 번째 줄이 첫 번째 줄에 비해 더 깊이 들여쓰기되고, 다음 중 하나가 성립할 때 억제(suppress)됩니다.

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

### 1.9 end 마커(End Markers)

선택적인 `end` 마커는 들여쓰기 영역의 경계를 명확히 합니다.

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

지정자 토큰(specifier token)은 그 앞에 오는 문장에 대응해야 합니다. 유효한 지정자에는 식별자(identifier)나 키워드가 포함됩니다: `if`, `while`, `for`, `match`, `try`, `new`, `this`, `val`, `given`, `extension`.

#### end 마커를 언제 사용하는가(When to Use End Markers)

코드가 다음과 같은 경우 end 마커는 명확성을 높여줍니다.

- 구조물 내부에 빈 줄(blank line)이 포함된 경우
- 15~20줄을 초과하는 경우
- 깊은 들여쓰기(4단계 이상)로 끝나는 경우

더 짧고 단순한 코드의 경우, end 마커는 오히려 가독성을 떨어뜨릴 수 있습니다.

### 1.10 컴파일러 설정과 재작성(Compiler Settings and Rewrites)

- **활성화(기본값)**: 의미 있는 들여쓰기가 기본적으로 활성화되어 있습니다.
- **비활성화**: `-no-indent`, `-old-syntax`, 또는 `-source 3.0-migration`
- **들여쓰기 문법으로 재작성**: `-rewrite -indent`
- **중괄호 문법으로 재작성**: `-rewrite -no-indent`

구식 문법에서 들여쓰기 문법으로 변환하려면 두 번의 패스(pass)가 필요합니다. 먼저 `-rewrite -new-syntax`를 수행한 뒤, `-rewrite -indent`를 수행합니다.

---

## 2. 새 제어 문법(New Control Syntax)

Scala 3는 조건(condition) 주위에 괄호를 요구하지 않는, 간결한 제어 구조 작성 방식을 도입합니다. 이 "조용한(quiet)" 문법은 완전한 기능을 유지하면서도 코드를 더 읽기 쉽게 만듭니다.

### 2.1 핵심 기능(Core Features)

새 문법은 네 가지 주요 제어 구조에 적용됩니다.

**if 표현식(If Expressions):**

```scala
if x < 0 then
  "negative"
else if x == 0 then
  "zero"
else
  "positive"

if x < 0 then -x else x
```

**while 루프(While Loops):**

```scala
while x >= 0 do x = f(x)
```

**for 표현식(For Expressions):**

```scala
for x <- xs if x > 0
yield x * x

for
  x <- xs
  y <- ys
do
  println(x + y)
```

**try-catch:**

```scala
try body
catch case ex: IOException => handle
```

### 2.2 문법 규칙(Syntax Rules)

공식 레퍼런스에 따르면 상세 규칙은 다음과 같습니다.

- `if` 표현식의 조건은 뒤에 `then`이 따라오면 감싸는 괄호 없이 작성할 수 있습니다.
- `while` 루프의 조건은 뒤에 `do`가 따라오면 감싸는 괄호 없이 작성할 수 있습니다.
- `for` 표현식의 열거자(enumerators)는 뒤에 `yield` 또는 `do`가 따라오면 감싸는 괄호나 중괄호 없이 작성할 수 있습니다.
- `for` 표현식에서 `do`는 for 루프(for-loop)를 표현합니다.
- `catch` 뒤에는 같은 줄에 단일 case를 둘 수 있습니다. case가 여러 개라면, (Scala 2와 마찬가지로) 중괄호 안에 있거나 들여쓰기된 블록 안에 있어야 합니다.

### 2.3 컴파일러 지원(Compiler Support)

Scala 3 컴파일러는 양방향 재작성(bidirectional rewriting) 기능을 제공합니다. `-rewrite -new-syntax`를 사용하면 기존 문법을 새 스타일로 변환하여 괄호와 중괄호를 제거합니다. `-rewrite -old-syntax`를 사용하면 역방향 변환을 수행하여, 전통적인 Scala 2 문법 관례를 복원합니다.

---

## 3. 최상위 정의(Top-Level Definitions)

### 3.1 개요(Overview)

Scala 3는 다양한 종류의 정의를 래퍼 객체(wrapper object) 없이 패키지 수준(package level)에서 작성할 수 있게 합니다. 공식 문서가 명시하듯, "이제 모든 종류의 정의를 최상위(top-level)에서 작성할 수 있습니다."

### 3.2 사용 예시(Example Usage)

```scala
package p
type Labelled[T] = (String, T)
val a: Labelled[Int] = ("count", 1)
def b = a._2

case class C()

extension (x: C) def pair(y: C) = (x, y)
```

이전에는 타입(type), 값(value), 메서드(method) 정의를 패키지 객체(package object) 안에 넣어야 했습니다. 이제는 여러 소스 파일이 그러한 정의를 포함할 수 있으며, 클래스나 객체와 자유롭게 섞어서 사용할 수 있습니다.

### 3.3 컴파일러 동작(Compiler Behavior)

컴파일러는 다음 범주의 최상위 정의에 대해 합성된 래퍼 객체(synthetic wrapper object)를 생성합니다.

- 패턴(pattern), 값, 메서드, 타입 정의
- 암시적 클래스(implicit class)와 객체
- 불투명 타입 별칭(opaque type alias)의 동반 객체(companion object)

`src.scala`라는 이름의 파일의 경우, 이러한 정의들은 `src$package`라는 합성 객체 안에 감싸집니다. 이 래핑은 패키지를 통해 이러한 정의에 접근하는 사용자에게는 투명하게(transparently) 동작합니다.

### 3.4 중요 참고 사항(Important Notes)

1. **이진 호환성(Binary Compatibility)**: 소스 파일 이름은 이진 호환성에 영향을 미칩니다. 파일 이름을 바꾸면 생성되는 객체의 이름이 바뀝니다.

2. **main 메서드(Main Methods)**: 최상위 `main` 메서드는 다른 정의와 마찬가지로 래핑됩니다. `src.scala`의 경우, 실행하려면 `scala src$package`가 필요합니다. 문서는 main 메서드를 명시적으로 이름이 붙은 객체 안에 두는 것을 권장합니다.

3. **private 범위(Privacy Scope)**: `private` 개념은 래핑과 독립적으로 유지됩니다. `private`인 최상위 정의는 감싸는 패키지 전체에 걸쳐 가시성(visibility)을 유지합니다.

4. **오버로딩 변형(Overloaded Variants)**: 같은 이름을 공유하는 여러 오버로딩 정의는 동일한 소스 파일에서 비롯되어야 합니다.

---

## 4. 매개변수 언튜플링(Parameter Untupling)

### 4.1 개요(Overview)

매개변수 언튜플링(parameter untupling)은, 여러 매개변수를 가진 함수 값(function value)이 튜플 인자(tupled argument)를 받을 수 있게 하여, 그 튜플의 요소들을 개별 매개변수로 자동 분해(decompose)해 주는 Scala 3 기능입니다.

### 4.2 기본 예시(Basic Example)

쌍(pair)의 리스트를 다룰 때, 이제 다음과 같이 작성할 수 있습니다.

```scala
val xs: List[(Int, Int)]
xs.map {
  (x, y) => x + y
}
```

이전의 패턴 매칭(pattern-matching) 방식을 사용하는 대신 말입니다.

```scala
xs map {
  case (x, y) => x + y
}
```

### 4.3 동작 방식(How It Works)

"기대 타입(expected type)이 그러하다면, `n > 1`개의 매개변수를 가진 함수 값은 `((T_1, ..., T_n)) => U` 형태의 함수 타입으로 감싸집니다." 그러면 그 튜플 매개변수가 분해되어, 그 요소들이 내부 함수로 직접 전달됩니다.

### 4.4 동등한 형태들(Equivalent Forms)

다음 세 가지 접근은 모두 동등합니다.

```scala
xs.map { (x, y) => x + y }
xs.map(_ + _)

def combine(i: Int, j: Int) = i + j
xs.map(combine)
```

### 4.5 중요한 제한(Important Limitation)

이 적응(adaptation)은 함수 값이 직접 제공되는 경우에만 적용됩니다. 다음은 동작하지 않습니다.

```scala
val combiner: (Int, Int) => Int = _ + _
xs.map(combiner)     // 타입 불일치(Type Mismatch)
```

대신, 함수를 명시적으로 튜플화(tuple)해야 합니다.

```scala
xs.map(combiner.tupled)
```

### 4.6 암시적 변환(Implicit Conversions)

권장되지는 않지만, 암시적 변환(implicit conversion)을 사용하여 유사한 동작을 달성할 수 있습니다. "매개변수 언튜플링은 변환(conversion)이 적용되기 전에 시도되므로, 범위 내(in scope)의 변환이 언튜플링을 가로채지(subvert) 못합니다."

---

## 5. kind 다형성(Kind Polymorphism)

### 5.1 개요(Overview)

Scala에서 타입 매개변수(type parameter)는 **kind**에 따라 구성됩니다. 일급 타입(first-level types, `Any`의 서브타입)은 값(value)을 위한 일반 타입을 나타내며, 고차 kind 타입(higher-kinded types)은 `List`나 `Map` 같은 타입 생성자(type constructor)를 나타냅니다.

### 5.2 단일 kind의 문제(The Problem with Single Kinds)

전통적으로, 타입 매개변수는 특정 kind의 인자만 받을 수 있습니다. 이 제약은 서로 다른 kind에 걸쳐 동작하는 제네릭 코드를 작성하는 것을 막습니다. 예를 들어, 일반 타입과 타입 생성자를 모두 처리하는 단일 암시적 값(implicit value)을 만들 수 없습니다.

### 5.3 해결책: AnyKind(The Solution: AnyKind)

Scala 3는 **kind 다형성(kind polymorphism)**을 가능하게 하는 특별한 타입 `scala.AnyKind`를 도입합니다. `AnyKind`를 상한(upper bound)으로 사용하면, 타입 매개변수가 "임의 kind(any-kinded)"가 됩니다.

```scala
def f[T <: AnyKind] = ...
```

이를 통해 임의 kind의 타입 인자를 받을 수 있습니다.

```scala
f[Int]           // 일반 타입
f[List]          // 일급 타입 생성자(first-order type constructor)
f[Map]           // 다중 매개변수 타입 생성자(multi-parameter type constructor)
f[[X] =>> String]  // 고차 kind 타입(higher-kinded type)
```

### 5.4 임의 kind 타입의 제약(Restrictions on Any-Kinded Types)

실제 kind가 컴파일 타임에 알려지지 않으므로, 임의 kind 타입(any-kinded types)에는 엄격한 제한이 적용됩니다.

- 값 타입(value type)으로 사용할 수 없습니다.
- 타입 매개변수로 인스턴스화(instantiate)할 수 없습니다.
- 기본적으로 다른 임의 kind 타입 매개변수에 전달하는 용도로만 사용할 수 있습니다.

이러한 제약에도 불구하고, 이 기능은 고급 암시(advanced implicit) 기법을 통해 정교한 일반화를 가능하게 합니다.

### 5.5 기술적 특성(Technical Characteristics)

`AnyKind`는 멤버가 없고 부모 클래스(parent class)가 없는, 합성된(synthesized) 추상 final 클래스(abstract final class)입니다. 이는 Scala 타입 시스템에서 독특한 위치를 차지합니다.

- kind와 무관하게 모든 타입의 상위 타입(supertype) 역할을 합니다.
- 모든 타입과 kind 호환성(kind-compatibility)을 보입니다.
- 고차 kind(higher-kinded)로 취급되어(값으로 사용할 수 없음) 동시에 타입 매개변수를 갖지 않습니다.

### 5.6 상태(Status)

"이 기능은 이제 안정적(stable)입니다. 컴파일러 플래그 `-Yno-kind-polymorphism`은 3.7.0 기준으로 폐기 예정(deprecated)이며, 아무런 효과가 없고(무시됨), 향후 버전에서 제거될 것입니다."

---

## 6. Matchable 트레이트(The Matchable Trait)

### 6.1 개요(Overview)

Scala 3 표준 라이브러리는 패턴 매칭(pattern matching) 능력을 제어하기 위한 새로운 트레이트 `Matchable`을 도입합니다. 이 기능은 추상 타입(abstract type)과 불투명 타입(opaque type)에서의 안전성(security) 우려를 해결합니다.

### 6.2 문제(The Problem)

문제의 핵심은 불변 배열 타입(immutable array type)인 `IArray`에 있습니다. 이는 다음과 같이 정의됩니다.

```scala
opaque type IArray[+T] = Array[? <: T]
```

`IArray`는 `length`와 `apply` 같은 안전한 연산을 위한 확장 메서드(extension method)를 제공하지만, 변형(mutation)을 방지하기 위해 `update` 메서드는 제공하지 않습니다. 그러나 패턴 매칭이 추상화를 깨뜨리는 허점(loophole)을 만듭니다.

```scala
val imm: IArray[Int] = ...
imm match
  case a: Array[Int] => a(0) = 1
```

런타임에 이 패턴 매칭은 성공합니다. 왜냐하면 `IArray` 값은 `Array` 인스턴스로 표현되기 때문이며, 불변 외관(immutable facade)에도 불구하고 허가되지 않은 변형을 허용하게 됩니다.

이 취약점은 불투명 타입을 넘어 무경계 타입 매개변수(unbounded type parameter)와 추상 타입에까지 확장됩니다. 예를 들어:

```scala
def f[T](x: T) = x match
  case a: Array[Int] => a(0) = 0
f(imm)
```

### 6.3 해결책(The Solution)

Scala 3는 패턴 매칭을 규제하기 위해 `scala.Matchable`을 도입합니다. 이제 생성자 패턴(constructor pattern)과 타입 패턴(type pattern)은 선택자 타입(selector type)이 `Matchable`을 따를(conform) 것을 요구합니다. 이를 따르지 않는 코드는 다음과 같은 경고 메시지를 발생시킵니다.

"pattern selector should be an instance of Matchable, but it has unmatchable type IArray[Int] instead"
(패턴 선택자는 Matchable의 인스턴스여야 하지만, 대신 매칭 불가능한 타입 IArray[Int]를 가지고 있습니다)

이 경고는 마이그레이션과 Scala 2/3 교차 컴파일(cross-compilation)을 지원하기 위해 `-source future-migration` 이상에서 활성화됩니다.

#### 새로운 타입 계층(New Type Hierarchy)

`Matchable`은 `Any`를 부모로 갖는 보편 트레이트(universal trait)이며, `AnyVal`과 `AnyRef` 양쪽 모두에 의해 확장됩니다.

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

현재 `Matchable`은 마커 트레이트(marker trait)이지만, `getClass`와 `isInstanceOf`가 패턴 매칭과 관련이 있기 때문에 향후 이들을 포함할 수도 있습니다.

#### 문제가 되는 타입들(Problematic Types)

다음 타입들에 대한 패턴 매칭은 경고를 발생시킵니다.

- 타입 `Any`: 매칭 가능한 선택자를 위해 대신 `Matchable`을 사용하세요.
- 무경계 타입 매개변수와 추상 타입: 상한(upper bound)으로 `Matchable`을 추가하세요.
- 보편 트레이트(universal trait)로만 경계가 지어진 타입 매개변수: 마찬가지로 `Matchable` 경계가 필요합니다.

### 6.4 Matchable과 보편 동등성(Matchable and Universal Equality)

`equals` 메서드는 필요한 조정을 잘 보여줍니다. 이 메서드는 `Any` 매개변수를 `Matchable`로 캐스트해야 합니다.

```scala
class C(val x: String):

  override def equals(that: Any): Boolean =
    that.asInstanceOf[Matchable] match
      case that: C => this.x == that.x
      case _ => false
```

이 캐스트는 "추상 타입과 불투명 타입이 존재할 때 보편 동등성(universal equality)은 안전하지 않다. 왜냐하면 타입의 의미(meaning)와 그 표현(representation)을 제대로 구별할 수 없기 때문이다"라는 점을 신호합니다. `Any`와 `Matchable`은 모두 `Object`로 소거(erase)되므로, 이 캐스트는 런타임에 항상 성공합니다.

### 6.5 예시: 불투명 타입 동등성 문제(Example: Opaque Type Equality Problem)

다음을 고려해 봅시다.

```scala
opaque type Meter = Double
def Meter(x: Double): Meter = x

opaque type Second = Double
def Second(x: Double): Second = x
```

보편 `equals`는 수학적으로 거짓임에도 불구하고 `Meter(10).equals(Second(10))`에 대해 잘못되게 true를 반환합니다. `import scala.language.strictEquality`를 통한 다중 동등성(multiversal equality)을 사용하면, `Meter(10) == Second(10)`를 컴파일 타임 오류로 변환하여 이러한 문제를 완화할 수 있습니다.

---

## 7. @targetName 애너테이션(The @targetName Annotation)

### 7.1 개요(Overview)

`@targetName` 애너테이션은 정의에 대한 대체 구현 이름(alternate implementation name)을 정의합니다. 이는 생성되는 바이트코드(bytecode)와, 다른 언어의 코드가 해당 메서드를 호출하는 방식에 영향을 미치지만, Scala에서의 사용에는 아무런 영향을 주지 않습니다.

### 7.2 기본 예시(Basic Example)

```scala
import scala.annotation.targetName

object VecOps:
  extension [T](xs: Vec[T])
    @targetName("append")
    def ++= [T] (ys: Vec[T]): Vec[T] = ...
```

이 경우, `++=` 연산은 `append`라는 이름으로 구현됩니다. 그러면 Java 코드는 다음과 같이 이를 호출할 수 있습니다.

```java
VecOps.append(vec1, vec2)
```

Scala 코드는 계속해서 `append`가 아닌 `++=`를 사용해야 합니다.

### 7.3 핵심 세부 사항(Key Details)

**정의와 범위:**

- `@targetName`은 `scala.annotation` 패키지에 위치합니다.
- 단일 `String` 인자(외부 이름)를 받습니다.
- 최상위 클래스, 트레이트, 객체를 제외한 모든 정의에 적용 가능합니다.

**요구 사항:**

- 외부 이름은 호스트 플랫폼에서 합법적(legal)이어야 합니다.
- 기호 이름(symbolic name)을 가진 정의에는 이 애너테이션을 포함해야 합니다.
- 백틱(backtick)으로 감싼 이름 중 플랫폼에서 합법적이지 않은 것에도 이 애너테이션을 붙여야 합니다.

### 7.4 오버라이딩과의 관계(Relationship with Overriding)

두 메서드 정의는 이름(name), 시그니처(signature), 그리고 소거된 이름(erased name)을 공유할 때 일치(match)합니다. _소거된 이름_은, `@targetName`이 지정되어 있으면 대상 이름(target name)이고, 그렇지 않으면 정의된 이름입니다.

#### 충돌하는 메서드의 모호성 해소(Disambiguating Clashing Methods)

```scala
@targetName("f_string")
def f(x: => String): Int = x.length
def f(x: => Int): Int = x + 1  // OK
```

두 메서드 모두 소거된 매개변수 타입이 `Function0`이어서, 애너테이션이 없으면 충돌(clash)이 발생합니다. 대상 이름이 이를 해소합니다.

#### 오버라이딩 제약(Overriding Constraints)

"두 멤버는 그들의 이름과 시그니처가 동일하고, 같은 소거된 이름을 공유하거나 동일한 타입(identical types)을 가질 때 서로를 오버라이드할 수 있습니다. 두 멤버가 오버라이드한다면, 소거된 이름과 타입이 모두 일치해야 합니다."

이는 오버라이딩 관계가 깨지는 것을 방지합니다.

```scala
class A:
  def f(): Int = 1
class B extends A:
  @targetName("g") def f(): Int = 2  // 오류: 오버라이딩과 충돌
```

컴파일러는 `@targetName`을 사용하여 상속 계층(inheritance hierarchy)에서 의도치 않은 이름 충돌을 만드는 것을 방지합니다.

---

## 8. 개선된 for(Better Fors)

### 8.1 개요(Overview)

Scala 3.8부터(또는 `-preview` 모드에서 3.7부터), for 내포(for-comprehensions)의 사용성이 개선되었으며, 특히 별칭(alias)으로 시작할 수 있는 능력을 통해 향상되었습니다.

### 8.2 핵심 기능: for 내포에서의 별칭(Aliases in For-Comprehensions)

가장 중요한 사용자 측면의 개선은, 값 별칭(value alias)으로 for 내포를 시작할 수 있게 한 것입니다.

```scala
for
  as = List(1, 2, 3)
  bs = List(4, 5, 6)
  a <- as
  b <- bs
yield a + b
```

이는 for 내포 앞에서 명시적으로 `val` 선언을 사용하는 것과 동일하게 디슈가링(desugar)됩니다.

### 8.3 디슈가링 개선(Desugaring Improvements)

#### 1. 순수 별칭에 대한 단순화된 디슈가링(Simplified Desugaring for Pure Aliases)

별칭에 가드 조건(guard condition)이 없을 때, 디슈가링이 더 직관적으로 이루어집니다. 이전에는 다음과 같은 코드가:

```scala
for {
  a <- doSth(arg)
  b = a
} yield a + b
```

튜플 래핑(tuple wrapping)을 동반한 중첩 `map` 호출로 디슈가링되었습니다. 이제는 더 직접적인 코드를 생성합니다.

```scala
doSth(arg).map { a =>
  val b = a
  a + b
}
```

이는 불필요한 중간 튜플링(tupling)과 매핑(mapping) 연산을 제거합니다.

#### 2. 중복된 map 호출 제거(Eliminating Redundant Map Calls)

최종적으로 yield되는 표현식이 구문적으로(syntactically) 마지막 생성자 패턴(generator pattern)(단일 변수이거나 변수들의 튜플)과 일치할 때, 컴파일러는 불필요한 `map` 호출을 피합니다.

```scala
for {
  a <- List(1, 2, 3)
} yield a
```

이제 `List(1, 2, 3).map(a => a)` 대신 직접 `List(1, 2, 3)`를 생성합니다.

### 8.4 기술적 세부 사항(Technical Details)

디슈가링 로직은 컴파일러의 `Desugar.scala#makeFor` 구현에 존재하며, 이는 for 내포 문법에서 컬렉션 타입에 대한 메서드 호출로의 변환을 처리합니다.

---

## 9. 안전한 초기화(Safe Initialization)

### 9.1 개요(Overview)

Scala 3는 `-Wsafe-init` 컴파일러 옵션을 통해 활성화되는 실험적인 안전 초기화 검사기(safe initialization checker)를 포함합니다. 문서에 따르면, 이 기능은 논문 _Safe object initialization, abstractly_에 설명되어 있습니다.

### 9.2 핵심 개념(Core Concept)

이 시스템은 객체 초기화 상태(initialization state)를 추적하기 위해 세 가지 근본적인 추상화(abstraction)를 사용합니다.

- **Cold(차가움)**: 초기화되지 않은 필드를 포함할 수 있는 객체
- **Warm(따뜻함)**: 모든 필드가 초기화되었지만, cold 객체에 도달(reach)할 가능성이 있는 객체
- **Hot(뜨거움)**: 추이적으로 초기화된(transitively initialized) 객체로, warm 객체에만 도달함

검사기는 또한 초기화 중인 현재 객체를 나타내는 `ThisRef`와, 외부 참조(outer references) 및 생성자 인자(constructor arguments)에 대한 추가 메타데이터를 갖는 warm 객체를 추적하는 `Warm[C]`를 도입합니다.

### 9.3 설계 목표(Design Goals)

구현은 여섯 가지 주요 목표를 지향합니다.

1. **건전성(Soundness)**: "검사는 항상 종료(terminate)되며, 일반적이고 합리적인 사용에 대해 건전(sound)합니다."
2. **표현력(Expressiveness)**: 표준적인 초기화 패턴을 지원합니다.
3. **사용자 친화성(User-friendliness)**: 최소한의 구문적 오버헤드(syntactic overhead)와 유익한 메시지를 제공합니다.
4. **모듈성(Modularity)**: 분석을 프로젝트 경계(project boundaries) 내로 한정합니다.
5. **성능(Performance)**: 컴파일 중 즉각적인 피드백을 제공합니다.
6. **단순성(Simplicity)**: 핵심 타입 시스템(core type system)을 수정하지 않습니다.

### 9.4 근본 원칙(Fundamental Principles)

**적층성(Stackability)**은 모든 클래스 필드가 클래스 본문(class body)의 끝에서 초기화되어야 함을 요구합니다. 단, 예외(exception) 같은 제어 효과(control effects)는 이를 방해할 수 있습니다.

**단조성(Monotonicity)**은 초기화 상태가 역행(regress)하는 것을 방지합니다. 일단 필드가 초기화되면, 계속 초기화된 상태로 유지됩니다. 이 시스템은 이미 초기화된 참조를 통해 초기화되지 않은 객체를 노출하게 되는 재할당(reassignment)을 금지합니다.

**범위성(Scopability)**은 초기화 프로토콜을 우회하는 측면 채널(side channels)이 없음을 보장합니다. 정적 필드(static fields)와 특정 제어 효과는 이 속성을 위반할 수 있으므로, 전달되는 값(transported values)이 추이적으로 초기화되도록 강제할 필요가 있습니다.

**권한(Authority)**은 단조성을 보완하여, 초기화 상태가 진행(advance)될 수 있는 위치를 제한합니다. 이는 필수 초기화자(mandatory initializers)가 있는 클래스 본문이나 지역 추론 지점(local reasoning points)에서만 가능합니다.

### 9.5 동작 규칙(Operational Rules)

검사기는 아홉 가지 핵심 규칙을 강제합니다.

1. cold 값은 필드 접근(field access)이나 메서드 호출(method invocation)을 통해 접근할 수 없습니다.
2. ThisRef는 초기화되지 않은 필드에 접근할 수 없습니다.
3. 재할당은 실질적으로 hot(effectively hot)인 우변(right-hand side) 표현식을 요구합니다.
4. 메서드 인자는 실질적으로 hot이어야 합니다(생성자와 매개변수적 메서드(parametric methods)에 대한 특정 예외 있음).
5. 실질적으로 hot인 인자를 가진 hot 값은 hot 결과를 생성합니다.
6. ThisRef와 warm 값에 대한 메서드 호출은 정적 해석(static resolution)을 거칩니다.
7. 실질적으로 hot인 인자를 가진 new 표현식은 hot 결과를 생성합니다.
8. hot이 아닌 인자를 가진 new 표현식은 warm 결과를 생성하며, 재분석(re-analysis)을 유발합니다.
9. 패턴 매치 검사 대상(scrutinees), 반환 값(return values), throw 표현식은 실질적으로 hot이어야 합니다.

### 9.6 실용 예시(Practical Examples)

검사기는 부모-자식 상호작용(parent-child interaction) 문제, 예를 들어 초기화되지 않은 자식 필드를 참조하는 오버라이드된 메서드에 접근하는 경우를 감지합니다.

```scala
abstract class AbstractFile:
  def name: String
  val extension: String = name.substring(4)

class RemoteFile(url: String) extends AbstractFile:
  val localFile: String = s"${url.##}.tmp"
  def name: String = localFile
```

이는 경고를 발생시킵니다. 왜냐하면 `extension`의 초기화가 `name`을 호출하고, 이는 초기화되지 않은 `localFile`에 접근하기 때문입니다.

내부-외부 상호작용(inner-outer interactions)도 마찬가지로 검사됩니다. 시스템은 중첩 클래스 생성(nested class construction)이 초기화 이전에 외부 필드(outer fields)에 접근하는 경우를 감지합니다.

```scala
object Trees:
  class ValDef { counter += 1 }
  class EmptyValDef extends ValDef
  val theEmptyValDef = new EmptyValDef
  private var counter = 0
```

함수 캡처(function captures)는 캡처된 참조를 통해 초기화되지 않은 필드에 접근하는 것을 방지하기 위해 분석됩니다.

### 9.7 실질적 hot 상태(Effective Hotness)

다음의 경우 어떤 값은 "실질적으로 hot(effectively hot)"으로 간주됩니다.

- 명시적으로 hot이거나,
- 모든 필드가 할당된 ThisRef이거나,
- 클래스가 내부 클래스(inner classes)를 갖지 않고 모든 필드가 실질적으로 hot인 Warm 값이거나,
- 반환 값이 실질적으로 hot인 함수일 때

검사기는 가능한 경우 언제나 hot이 아닌 값을 실질적으로 hot 상태로 승격(promote)하려고 시도합니다.

### 9.8 모듈성과 제약(Modularity and Limitations)

분석은 주 생성자(primary constructors)를 진입점(entry points)으로 취급하며, TASTy를 사용하여 프로젝트 경계를 넘어 상위 클래스 생성자(superclass constructors)를 추적합니다. 이러한 경계 넘기(crossing)는, 객체 지향 상속(object-oriented inheritance)에 내재한 클래스 간 결합(coupling) 때문에 건전성을 위해 필요합니다.

이 구현은 두 가지 주의 사항을 인정합니다. "이 시스템은 Java나 Scala 2 클래스를 확장할 때 안전성을 보장할 수 없습니다." 그리고 "전역 객체(global objects)의 안전한 초기화는 부분적으로만 검사됩니다."

### 9.9 억제 메커니즘(Suppression Mechanisms)

사용자는 `@unchecked` 애너테이션을 사용하거나 필드를 lazy로 표시하여 경고를 억제할 수 있습니다. 다만 이들은 안전하지 않은 패턴을 승인하는 것이 아니라 임시방편(workaround)에 해당합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other New Features](https://docs.scala-lang.org/scala3/reference/other-new-features/)
