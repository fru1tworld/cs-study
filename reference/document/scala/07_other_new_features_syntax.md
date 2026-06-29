# Scala 3 기타 새 기능: 문법과 언어 편의 기능

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

> 📘 **처음 배우는 분께 — "선택적 중괄호"가 무슨 말인가**
>
> Scala 2에서는 클래스·메서드·블록의 범위를 항상 중괄호 `{ ... }`로 묶었습니다.
> Scala 3에서는 Python처럼 **들여쓰기만으로도** 같은 범위를 표현할 수 있게 되었습니다.
> 즉 "중괄호를 써도 되고, 안 쓰고 들여쓰기로 대신해도 된다"는 뜻이라 "선택적(optional) 중괄호"라고 부릅니다.
> 이 문서가 다루는 "새 문법"은 사실 Scala를 처음 배우는 분이 보게 될 **기본 문법**이기도 합니다.
> 옛 자료에서 본 중괄호 스타일과 요즘 자료의 들여쓰기 스타일이 둘 다 맞다고 알아두면 됩니다.

> 💡 **왜 필요한가**
>
> 중괄호가 깊게 중첩되면 "이 `}`가 어느 `{`를 닫는 거지?" 하고 헷갈리기 쉽습니다.
> 들여쓰기는 사람이 어차피 눈으로 읽는 정보라, 이것만으로 범위를 정하면 코드가 더 간결하고 읽기 좋아집니다.
> 동시에 컴파일러가 들여쓰기를 검사해 "닫는 중괄호를 빠뜨렸다" 같은 흔한 실수를 잡아 줍니다.

Scala 3는 의미 있는 들여쓰기(significant indentation)를 도입합니다. 이를 통해 많은 문맥에서 중괄호(`{}`)를 생략할 수 있으며, 동시에 들여쓰기 규칙을 강제하여 흔히 발생하는 오류를 잡아냅니다. 이 기능은 컴파일러 플래그 `-no-indent`로 비활성화할 수 있습니다.

### 1.1 들여쓰기 규칙(Indentation Rules)

컴파일러는 두 가지 규칙을 강제하며, 위반 시 경고(warning)를 출력합니다.

1. **중괄호로 구분된 영역 규칙(Brace-delimited region rule)**: 중괄호 내부에서, 여는 중괄호 뒤 새 줄의 첫 문장보다 더 왼쪽에서 시작하는 문장은 허용되지 않습니다. 이 규칙은 닫는 중괄호가 누락된 경우를 감지하는 데 도움이 됩니다.

2. **들여쓰기 폭 규칙(Indentation width rule)**: 의미 있는 들여쓰기가 꺼져 있을 때, 표현식의 들여쓰기된 하위 부분이 줄바꿈으로 끝나는 경우, 그 다음 문장은 해당 하위 부분보다 더 얕은 들여쓰기로 시작해야 합니다. 이 규칙은 여는 중괄호를 누락한 경우를 방지합니다.

### 1.2 선택적 중괄호 메커니즘(Optional Braces Mechanism)

컴파일러는 줄바꿈 지점에 `<indent>`와 `<outdent>` 토큰을 삽입합니다. 이 토큰들은 중괄호 쌍 `{...}`와 동등하게 동작하며, 들여쓰기 폭 스택(indentation width stack, `IW`)으로 관리됩니다.

#### 들여쓰기 삽입 규칙(Indent Insertion Rules)

`<indent>` 토큰은 다음 조건이 모두 충족될 때 삽입됩니다.

- 현재 위치에서 들여쓰기 영역(indentation region)이 시작될 수 있고,
- 다음 줄의 첫 토큰이 현재보다 더 깊이 들여쓰기되어 있을 때

들여쓰기 영역은 다음 위치 뒤에서 시작될 수 있습니다.

- 확장(extension)의 선행 매개변수(leading parameters) 뒤
- given 인스턴스에서 `with` 뒤
- 템플릿 본문(template body) 시작 부분의 `:` 뒤
- 다음 토큰들 뒤: `=`, `=>`, `?=>`, `<-`, `catch`, `do`, `else`, `finally`, `for`, `if`, `match`, `return`, `then`, `throw`, `try`, `while`, `yield`
- 구식(old-style) `if`/`while` 조건의 닫는 `)` 뒤
- 구식 `for` 루프 열거자(enumerations)의 닫는 `)` 또는 `}` 뒤

#### 내어쓰기 삽입 규칙(Outdent Insertion Rules)

`<outdent>` 토큰은 다음 조건이 모두 충족될 때 삽입됩니다.

- 다음 줄의 첫 토큰이 현재보다 더 얕게 들여쓰기되어 있고,
- 이전 줄이 다음 연속(continuation) 토큰으로 끝나지 않을 때: `then`, `else`, `do`, `catch`, `finally`, `yield`, `match`

선행 중위 연산자(leading infix operators)에는 올바른 파싱 우선순위를 보장하기 위한 별도 규칙이 적용됩니다.

### 1.3 템플릿 본문 주위의 선택적 중괄호(Optional Braces Around Template Bodies)

> 📘 **처음 배우는 분께 — "템플릿 본문"과 끝의 콜론 `:`**
>
> 여기서 "템플릿 본문"은 `class`/`trait`/`object`의 **중괄호 안에 들어가는 멤버 묶음**을 가리키는 컴파일러 용어입니다.
> 중괄호를 생략할 때는 헤더 끝에 콜론 `:`을 붙이고 다음 줄부터 들여쓰면 됩니다.
> 즉 `trait A:` 다음 줄에 들여쓴 `def f: Int`는 `trait A { def f: Int }`와 똑같은 뜻입니다.
> 이 `:`은 "여기서부터 본문이 들여쓰기로 이어진다"는 신호일 뿐, 타입을 적는 콜론과는 역할이 다릅니다.

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

콜론 토큰(`:`)은 뒤에 들여쓰기된 내용이 따라올 때 중괄호를 대체하며, `{` `<indent>` ... `<outdent>` `}`와 동일하게 동작합니다.

### 1.4 메서드 인자에 대한 선택적 중괄호(Optional Braces for Method Arguments)

> ⚠️ **짚고 넘어가기 — `times(10):` 끝의 콜론에 놀라지 마세요**
>
> 아래 `times(10):` 다음 줄에 들여쓴 두 줄은, `times(10) { println("ah"); println("ha") }`와 같은 뜻입니다.
> 즉 "메서드에 넘기는 인자(블록)를 중괄호 대신 콜론+들여쓰기로 적은 것"입니다.
> `xs.map: x =>` 형태도 마찬가지로 `xs.map { x => ... }`와 같습니다.
> 처음 보면 "메서드 호출 뒤에 웬 콜론?" 싶지만, **블록 인자를 여는 신호**라고 알면 됩니다.

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

들여쓰기 접두부(indentation prefix)는 공백(space)과 탭(tab)으로 구성되며, 문자열 접두 관계(string prefix relation)에 따라 순서가 결정됩니다. 공백과 탭을 혼용하면 오류를 유발하기 쉬우므로 한 파일 안에서는 권장되지 않습니다. 비교 불가능한 들여쓰기 폭(예: 탭 6개 vs 공백 4개)은 컴파일 오류를 발생시킵니다.

### 1.6 들여쓰기와 중괄호의 상호작용(Indentation and Braces Interaction)

중괄호 영역 내에서도 들여쓰기 규칙이 다음 원칙에 따라 적용됩니다.

1. 여러 줄 중괄호 영역의 들여쓰기 폭은 `{` 뒤 새 줄에서 첫 토큰의 들여쓰기입니다.
2. 여러 줄 대괄호/괄호의 들여쓰기 폭은 다음과 같습니다.
   - 대괄호/괄호가 줄을 끝맺는 경우: 다음 토큰의 들여쓰기
   - 그 외의 경우: 감싸는 영역의 들여쓰기
3. 닫는 `}`, `]`, `)`는 중첩된 영역을 닫는 데 필요한 `<outdent>` 토큰을 발생시킵니다.

### 1.7 case 절의 특별 처리(Case Clause Special Treatment)

match 표현식과 catch 절에는 다음과 같은 규칙이 적용됩니다.

- `match` 또는 `catch` 뒤에는, `case`가 `match`와 같은 들여쓰기 위치에 나타나더라도 들여쓰기 영역이 열립니다.
- 이 영역은 해당 들여쓰기에서 `case`가 아닌 첫 토큰이 등장하거나, 더 얕게 들여쓰기된 토큰이 나타날 때 닫힙니다.

이를 통해 들여쓰기되지 않은(unindented) case를 작성할 수 있습니다.

```scala
x match
case 1 => print("I")
case 2 => print("II")
case 3 => print("III")

println(".")
```

### 1.8 들여쓰기를 통한 문장 연속(Statement Continuation via Indentation)

두 번째 줄이 첫 번째 줄보다 더 깊이 들여쓰기되고, 다음 조건 중 하나를 만족하면 가상 세미콜론(virtual semicolon)이 억제됩니다.

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

> 📘 **처음 배우는 분께 — `end` 마커는 "여기서 끝"이라는 표시**
>
> 들여쓰기 문법에는 범위를 닫는 `}`가 없으니, 긴 블록은 어디서 끝나는지 눈으로 찾기 어렵습니다.
> `end largeMethod`, `end if`처럼 적어 두면 "이 메서드/`if`가 여기서 끝난다"를 사람에게 알려 줍니다.
> 어디까지나 **사람을 위한 표시**이고 생략해도 동작은 같습니다(선택 사항). 짧은 코드에는 오히려 군더더기가 됩니다.

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

지정자 토큰(specifier token)은 바로 앞 문장과 대응해야 합니다. 유효한 지정자는 식별자(identifier) 또는 다음 키워드입니다: `if`, `while`, `for`, `match`, `try`, `new`, `this`, `val`, `given`, `extension`.

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

구식 문법에서 들여쓰기 문법으로 변환하려면 두 단계가 필요합니다. 먼저 `-rewrite -new-syntax`를 실행한 뒤, `-rewrite -indent`를 실행합니다.

---

## 2. 새 제어 문법(New Control Syntax)

> 📘 **처음 배우는 분께 — `then` / `do`로 괄호를 대신한다**
>
> Java나 Scala 2에서는 `if (x < 0)`, `while (x >= 0)`처럼 조건을 **괄호로 감쌌습니다.**
> Scala 3 새 문법에서는 괄호를 빼는 대신, 조건이 끝나는 곳에 키워드를 둡니다.
> - `if`는 조건 뒤에 **`then`**: `if x < 0 then ...`
> - `while`은 조건 뒤에 **`do`**: `while x >= 0 do ...`
> - `for`는 마지막에 **`yield`**(값을 모음) 또는 **`do`**(부수효과만)
>
> `then`/`do`는 "조건은 여기까지, 이제부터 본문"이라는 경계 표시 역할을 합니다.
> (옛 괄호 문법도 그대로 쓸 수 있으니, 둘 다 눈에 익혀 두세요.)

Scala 3는 조건식 주위에 괄호를 요구하지 않는 간결한 제어 구조 문법을 도입합니다. 이 "조용한(quiet)" 문법은 완전한 기능을 유지하면서도 코드 가독성을 높입니다.

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

> 📘 **처음 배우는 분께 — `for ... yield`는 "변환해서 모으기"**
>
> Scala의 `for`는 자바의 "단순 반복문"이라기보다 **컬렉션을 훑어 새 컬렉션을 만드는** 도구에 가깝습니다.
> - `x <- xs` 는 "`xs`의 각 원소를 `x`로 꺼낸다"는 뜻입니다(`<-`를 "in"으로 읽으면 편합니다).
> - `if x > 0` 는 거른다(필터), `yield x * x` 는 각 결과를 모아 새 컬렉션으로 돌려준다는 뜻입니다.
> - 값을 모으지 않고 그냥 반복만(예: 출력) 하고 싶으면 `yield` 대신 `do`를 씁니다.
>
> 즉 위 예시는 "`xs`에서 양수만 골라 제곱한 리스트"를 만듭니다. (00번 문서의 "표현식" 개념의 연장입니다.)

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

Scala 3 컴파일러는 양방향 문법 변환을 지원합니다. `-rewrite -new-syntax`는 기존 문법을 새 스타일로 변환하여 조건식의 괄호와 중괄호를 제거하고, `-rewrite -old-syntax`는 역방향 변환으로 전통적인 Scala 2 문법을 복원합니다.

---

## 3. 최상위 정의(Top-Level Definitions)

### 3.1 개요(Overview)

> 📘 **처음 배우는 분께 — "최상위 정의"란**
>
> Java에서는 모든 것이 클래스 안에 들어가야 했습니다. Scala 2도 `val`/`def`/`type`은 보통
> 클래스나 `object`(또는 package object) 안에 넣어야 했습니다.
> Scala 3에서는 이런 정의를 **클래스 밖, 파일의 맨 바깥(최상위)에 바로** 적을 수 있습니다.
> 그래서 작은 유틸리티 함수나 상수를 굳이 객체로 감싸지 않아도 됩니다.

> 💡 **왜 필요한가**
>
> 과거에는 패키지 전체에서 공유할 함수·상수를 모으려고 **package object**라는 특별한 파일을 따로 만들었습니다(00번 11번 참고).
> 최상위 정의가 생기면서 그 번거로움이 사라졌고, package object는 거의 쓸 일이 없어졌습니다.
> 컴파일러는 내부적으로 이것들을 보이지 않는 래퍼 객체로 감싸 처리하지만(아래 3.3), 사용하는 입장에서는 신경 쓸 필요가 없습니다.

Scala 3는 다양한 종류의 정의를 래퍼 객체 없이 패키지 수준에서 직접 작성할 수 있게 합니다. 공식 문서에 따르면 "이제 모든 종류의 정의를 최상위(top-level)에서 작성할 수 있습니다."

### 3.2 사용 예시(Example Usage)

```scala
package p
type Labelled[T] = (String, T)
val a: Labelled[Int] = ("count", 1)
def b = a._2

case class C()

extension (x: C) def pair(y: C) = (x, y)
```

이전에는 타입, 값, 메서드 정의를 패키지 객체(package object) 안에 넣어야 했습니다. 이제는 여러 소스 파일에 이러한 정의를 자유롭게 배치할 수 있으며, 클래스나 객체와 섞어 쓸 수 있습니다.

### 3.3 컴파일러 동작(Compiler Behavior)

컴파일러는 다음 범주의 최상위 정의에 대해 합성 래퍼 객체(synthetic wrapper object)를 생성합니다.

- 패턴(pattern), 값, 메서드, 타입 정의
- 암시적 클래스(implicit class)와 객체
- 불투명 타입 별칭(opaque type alias)의 동반 객체(companion object)

`src.scala` 파일의 경우 이러한 정의들은 `src$package`라는 합성 객체 안에 감싸집니다. 이 래핑은 패키지를 통해 해당 정의에 접근하는 호출자에게는 투명하게 동작합니다.

### 3.4 중요 참고 사항(Important Notes)

1. **이진 호환성(Binary Compatibility)**: 소스 파일 이름은 이진 호환성에 영향을 미칩니다. 파일 이름을 바꾸면 생성되는 객체의 이름이 바뀝니다.

> 📘 **처음 배우는 분께 — 프로그램 시작점과 `@main`**
>
> 자바에서는 `public static void main(String[] args)`가 프로그램의 시작점이었습니다.
> Scala 3에서는 아무 함수 위에 **`@main`**만 붙이면 그 함수가 실행 진입점이 됩니다.
> 예: `@main def run() = println("hi")` → 명령행에서 `run`으로 실행됩니다.
> 아래에서 말하는 "최상위 `main` 메서드"는 `@main`이 아니라 옛 방식대로 `main`이라는 이름으로 직접 정의한 경우를 가리킵니다.

2. **main 메서드(Main Methods)**: 최상위 `main` 메서드도 다른 정의와 마찬가지로 래핑됩니다. `src.scala`의 경우 실행하려면 `scala src$package`가 필요합니다. 공식 문서는 main 메서드를 명시적으로 이름 붙인 객체 안에 두도록 권장합니다.

3. **private 범위(Privacy Scope)**: `private` 접근 제어는 래핑과 독립적으로 유지됩니다. `private` 최상위 정의의 가시성은 해당 패키지 전체로 제한됩니다.

4. **오버로딩 변형(Overloaded Variants)**: 같은 이름을 가진 오버로딩 정의는 모두 동일한 소스 파일에서 정의되어야 합니다.

---

## 4. 매개변수 언튜플링(Parameter Untupling)

### 4.1 개요(Overview)

> 📘 **처음 배우는 분께 — 튜플과 "언튜플링"**
>
> **튜플(tuple)**은 `(1, "a")`처럼 여러 값을 괄호로 묶어 한 덩어리로 만든 것입니다.
> `List[(Int, Int)]`는 "(정수, 정수) 쌍들의 리스트"라는 뜻이죠.
> 이런 리스트를 `map`으로 다룰 때, 원래는 쌍을 한 덩어리(`p`)로 받아 `p._1`, `p._2`로 꺼내거나
> `case (x, y) =>`로 분해해야 했습니다.
> **언튜플링**은 컴파일러가 그 쌍을 알아서 `x`, `y` 두 개로 풀어 주는 것이라, `(x, y) => x + y`라고 바로 쓸 수 있습니다.
> ("튜플로 묶인 것을 푼다"고 해서 un-tupling입니다.)

매개변수 언튜플링(parameter untupling)은 여러 매개변수를 가진 함수 값이 튜플 인자를 받을 때 그 요소들을 개별 매개변수로 자동 분해(decompose)해 주는 Scala 3 기능입니다.

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

기대 타입(expected type)이 맞다면, `n > 1`개의 매개변수를 가진 함수 값은 `((T_1, ..., T_n)) => U` 형태의 함수 타입으로 변환됩니다. 이때 튜플 매개변수가 분해되어 각 요소가 내부 함수로 직접 전달됩니다.

### 4.4 동등한 형태들(Equivalent Forms)

다음 세 가지 접근은 모두 동등합니다.

```scala
xs.map { (x, y) => x + y }
xs.map(_ + _)

def combine(i: Int, j: Int) = i + j
xs.map(combine)
```

### 4.5 중요한 제한(Important Limitation)

이 변환은 함수 값이 직접 제공되는 경우에만 적용됩니다. 다음은 동작하지 않습니다.

```scala
val combiner: (Int, Int) => Int = _ + _
xs.map(combiner)     // 타입 불일치(Type Mismatch)
```

대신, 함수를 명시적으로 튜플화(tuple)해야 합니다.

```scala
xs.map(combiner.tupled)
```

### 4.6 암시적 변환(Implicit Conversions)

권장되지는 않지만, 암시적 변환(implicit conversion)으로 유사한 동작을 구현할 수 있습니다. "매개변수 언튜플링은 변환이 적용되기 전에 먼저 시도되므로, 스코프 내의 변환이 언튜플링을 가로채지 못합니다."

---

## 5. kind 다형성(Kind Polymorphism)

### 5.1 개요(Overview)

> 📘 **처음 배우는 분께 — "kind(종류)"는 타입의 타입**
>
> 값에 타입이 있듯이, 타입에도 "종류(kind)"가 있습니다.
> `Int`, `String`처럼 그 자체로 완성된 타입은 한 종류이고, `List`처럼 `List[Int]`가 되어야 비로소 완성되는 "타입을 받아 타입을 만드는 것"은 또 다른 종류입니다(00번 7번 고차 타입 참고).
> 보통 함수는 한 종류만 받을 수 있는데, **kind 다형성**은 종류가 다른 타입들을 한꺼번에 받을 수 있게 해 주는 고급 기능입니다.
> 매우 드물게 쓰이는 주제라, 처음엔 "그런 게 있구나" 하고 넘어가도 좋습니다.

Scala에서 타입 매개변수는 **kind**에 따라 분류됩니다. 일급 타입(first-level types, `Any`의 서브타입)은 값을 위한 일반 타입을 나타내며, 고차 kind 타입(higher-kinded types)은 `List`나 `Map` 같은 타입 생성자(type constructor)를 나타냅니다.

### 5.2 단일 kind의 문제(The Problem with Single Kinds)

전통적으로 타입 매개변수는 특정 kind의 인자만 받을 수 있습니다. 이 제약으로 인해 서로 다른 kind에 걸쳐 동작하는 제네릭 코드를 작성하기 어렵습니다. 예를 들어, 일반 타입과 타입 생성자를 모두 처리하는 단일 암시적 값을 정의할 수 없습니다.

### 5.3 해결책: AnyKind(The Solution: AnyKind)

Scala 3는 **kind 다형성(kind polymorphism)**을 가능하게 하는 특수 타입 `scala.AnyKind`를 도입합니다. `AnyKind`를 상한(upper bound)으로 지정하면 타입 매개변수가 임의 kind(any-kinded)를 받을 수 있게 됩니다.

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

실제 kind가 컴파일 타임에 확정되지 않으므로, 임의 kind 타입에는 엄격한 제한이 따릅니다.

- 값 타입(value type)으로 사용할 수 없습니다.
- 타입 매개변수로 인스턴스화할 수 없습니다.
- 기본적으로 다른 임의 kind 타입 매개변수에 전달하는 용도로만 사용 가능합니다.

이러한 제약에도 불구하고 고급 암시(advanced implicit) 기법을 통해 정교한 일반화를 구현할 수 있습니다.

### 5.5 기술적 특성(Technical Characteristics)

`AnyKind`는 멤버와 부모 클래스가 없는 합성 추상 final 클래스(synthesized abstract final class)로, Scala 타입 시스템에서 독특한 위치를 차지합니다.

- kind와 무관하게 모든 타입의 상위 타입(supertype) 역할을 합니다.
- 모든 타입과 kind 호환성(kind-compatibility)을 가집니다.
- 고차 kind로 취급되어(값으로 사용 불가) 동시에 타입 매개변수를 갖지 않습니다.

### 5.6 상태(Status)

"이 기능은 이제 안정적(stable)입니다. 컴파일러 플래그 `-Yno-kind-polymorphism`은 3.7.0 기준으로 폐기 예정(deprecated)이며, 아무런 효과가 없고(무시됨), 향후 버전에서 제거될 것입니다."

---

## 6. Matchable 트레이트(The Matchable Trait)

### 6.1 개요(Overview)

Scala 3 표준 라이브러리는 패턴 매칭 가능 여부를 제어하기 위한 트레이트 `Matchable`을 도입합니다. 이 기능은 추상 타입(abstract type)과 불투명 타입(opaque type)에서 발생할 수 있는 추상화 위반 문제를 해결합니다.

> 📘 **처음 배우는 분께 — `Matchable`이 푸는 문제 한 줄 요약**
>
> 패턴 매칭(`match`/`case`)으로 "이 값이 사실은 무슨 타입인가"를 들춰볼 수 있는데,
> 이걸 무제한 허용하면 일부러 숨겨 둔 타입(opaque type 등)의 속을 들여다보고 규칙을 깰 수 있습니다.
> `Matchable`은 "패턴 매칭으로 들춰봐도 괜찮은 타입"에만 붙는 표식 역할을 해서, 그런 우회로를 막습니다.
> 처음에는 "패턴 매칭을 안전하게 제한하려고 생긴 표식 트레이트" 정도로만 이해해도 충분합니다.

### 6.2 문제(The Problem)

문제의 핵심은 불변 배열 타입(immutable array type)인 `IArray`에 있습니다. 이는 다음과 같이 정의됩니다.

> 📘 **처음 배우는 분께 — 와일드카드 타입 `?` (Scala 2의 `_`)**
>
> `Array[? <: T]`의 `?`는 "구체적으로 무슨 타입인지는 모르지만 `T`의 하위 타입인 어떤 것"을 뜻하는 **와일드카드 타입**입니다.
> Scala 2에서는 이 자리에 밑줄을 써서 `Array[_ <: T]`라고 적었습니다.
> Scala 3는 `_`를 다른 용도(예: `F[_]` 고차 타입 자리)와 헷갈리지 않도록, 와일드카드 타입 기호를 **`?`**로 바꾸고 있습니다.
> "물음표 = 아직 정해지지 않은 어떤 타입"이라고 기억하면 됩니다. (00번 12번 대응표 참고.)

```scala
opaque type IArray[+T] = Array[? <: T]
```

> ⚠️ **짚고 넘어가기 — 여기서 "안전성"은 보안이 아니라 "추상화가 깨지지 않음"**
>
> 원문의 *security/safety*는 해킹 같은 보안이 아니라, "타입이 약속한 규칙(여기서는 '읽기 전용 배열은 못 고친다')이 우회로로 무너지지 않음"을 뜻합니다.
> 즉 `IArray`는 못 고치도록 만든 배열인데, 패턴 매칭으로 정체를 들춰 `Array`로 보면 몰래 고칠 수 있게 되는 **허점**을 막자는 이야기입니다.

`IArray`는 `length`와 `apply` 같은 안전한 연산을 위한 확장 메서드를 제공하지만, 변형(mutation)을 방지하기 위해 `update` 메서드는 제공하지 않습니다. 그러나 패턴 매칭이 이 추상화를 깨뜨리는 허점(loophole)이 됩니다.

```scala
val imm: IArray[Int] = ...
imm match
  case a: Array[Int] => a(0) = 1
```

런타임에 이 패턴 매칭은 성공합니다. `IArray` 값이 내부적으로 `Array` 인스턴스로 표현되기 때문에, 불변 외관(immutable facade)에도 불구하고 허가되지 않은 변형이 가능해집니다.

이 취약점은 불투명 타입을 넘어 무경계 타입 매개변수(unbounded type parameter)와 추상 타입에까지 이어집니다. 예를 들어:

```scala
def f[T](x: T) = x match
  case a: Array[Int] => a(0) = 0
f(imm)
```

### 6.3 해결책(The Solution)

Scala 3는 패턴 매칭을 제어하기 위해 `scala.Matchable`을 도입합니다. 이제 생성자 패턴(constructor pattern)과 타입 패턴(type pattern)은 선택자 타입(selector type)이 `Matchable`을 따라야(conform) 합니다. 이를 따르지 않으면 다음과 같은 경고가 발생합니다.

"pattern selector should be an instance of Matchable, but it has unmatchable type IArray[Int] instead"
(패턴 선택자는 Matchable의 인스턴스여야 하지만, 대신 매칭 불가능한 타입 IArray[Int]를 가지고 있습니다)

이 경고는 Scala 2/3 마이그레이션 및 교차 컴파일(cross-compilation)을 지원하기 위해 `-source future-migration` 이상에서 활성화됩니다.

#### 새로운 타입 계층(New Type Hierarchy)

`Matchable`은 `Any`를 부모로 갖는 보편 트레이트(universal trait)로, `AnyVal`과 `AnyRef` 모두에 의해 확장됩니다.

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

현재 `Matchable`은 마커 트레이트(marker trait)이지만, `getClass`와 `isInstanceOf`가 패턴 매칭과 밀접하게 관련되어 있어 향후 이들을 포함할 수 있습니다.

#### 문제가 되는 타입들(Problematic Types)

다음 타입들에 대한 패턴 매칭은 경고를 발생시킵니다.

- 타입 `Any`: 매칭 가능한 선택자를 위해 대신 `Matchable`을 사용하세요.
- 무경계 타입 매개변수와 추상 타입: 상한(upper bound)으로 `Matchable`을 추가하세요.
- 보편 트레이트(universal trait)로만 경계가 지어진 타입 매개변수: 마찬가지로 `Matchable` 경계가 필요합니다.

### 6.4 Matchable과 보편 동등성(Matchable and Universal Equality)

`equals` 메서드는 이 조정이 필요한 대표적인 예입니다. `Any` 매개변수를 `Matchable`로 캐스트한 뒤 패턴 매칭을 수행해야 합니다.

```scala
class C(val x: String):

  override def equals(that: Any): Boolean =
    that.asInstanceOf[Matchable] match
      case that: C => this.x == that.x
      case _ => false
```

이 캐스트는 "추상 타입과 불투명 타입이 존재하는 상황에서 보편 동등성(universal equality)은 안전하지 않으며, 타입의 의미(meaning)와 표현(representation)을 제대로 구별할 수 없다"는 점을 명시적으로 드러냅니다. `Any`와 `Matchable`은 모두 `Object`로 소거(erase)되므로, 이 캐스트는 런타임에 항상 성공합니다.

### 6.5 예시: 불투명 타입 동등성 문제(Example: Opaque Type Equality Problem)

다음을 고려해 봅시다.

```scala
opaque type Meter = Double
def Meter(x: Double): Meter = x

opaque type Second = Double
def Second(x: Double): Second = x
```

보편 `equals`는 수학적으로 거짓임에도 `Meter(10).equals(Second(10))`에 대해 잘못되게 true를 반환합니다. `import scala.language.strictEquality`를 통해 다중 동등성(multiversal equality)을 활성화하면 `Meter(10) == Second(10)`를 컴파일 타임 오류로 전환하여 이러한 문제를 방지할 수 있습니다.

---

## 7. @targetName 애너테이션(The @targetName Annotation)

### 7.1 개요(Overview)

> 📘 **처음 배우는 분께 — `@targetName`은 "바이트코드용 이름표"**
>
> Scala에서는 `++=`, `<*>` 같은 **기호로 된 메서드 이름**을 쓸 수 있습니다.
> 그런데 JVM 바이트코드나 자바에서는 이런 기호 이름을 그대로 부르기 어렵습니다.
> `@targetName("append")`를 붙이면 "Scala 코드에서는 `++=`로 쓰되, 실제 내부 이름은 `append`로 만들어 달라"는 뜻이 됩니다.
> Scala로 코딩하는 입장에서는 평소엔 신경 쓸 일이 없고, **자바와 섞어 쓰거나 기호 이름을 쓸 때** 등장하는 보조 장치입니다.

`@targetName` 애너테이션은 정의에 대한 대체 구현 이름(alternate implementation name)을 지정합니다. 생성되는 바이트코드와 다른 언어에서의 호출 방식에 영향을 미치지만, Scala 코드에서의 사용에는 영향을 주지 않습니다.

### 7.2 기본 예시(Basic Example)

```scala
import scala.annotation.targetName

object VecOps:
  extension [T](xs: Vec[T])
    @targetName("append")
    def ++= [T] (ys: Vec[T]): Vec[T] = ...
```

`++=` 연산은 `append`라는 이름으로 구현됩니다. Java 코드에서는 다음과 같이 호출할 수 있습니다.

```java
VecOps.append(vec1, vec2)
```

Scala 코드에서는 `append`가 아닌 `++=`를 그대로 사용해야 합니다.

### 7.3 핵심 세부 사항(Key Details)

**정의와 범위:**

- `@targetName`은 `scala.annotation` 패키지에 위치합니다.
- 단일 `String` 인자(외부 이름)를 받습니다.
- 최상위 클래스, 트레이트, 객체를 제외한 모든 정의에 적용할 수 있습니다.

**요구 사항:**

- 외부 이름은 호스트 플랫폼에서 유효한 이름이어야 합니다.
- 기호 이름(symbolic name)을 가진 정의에는 이 애너테이션을 붙이도록 권장됩니다.
- 백틱으로 감싼 이름 중 플랫폼에서 유효하지 않은 것에도 이 애너테이션을 붙여야 합니다.

### 7.4 오버라이딩과의 관계(Relationship with Overriding)

두 메서드 정의는 이름, 시그니처, 소거된 이름(erased name)을 공유할 때 일치(match)합니다. _소거된 이름_은 `@targetName`이 지정된 경우 대상 이름(target name)이고, 지정되지 않은 경우 정의된 이름입니다.

#### 충돌하는 메서드의 모호성 해소(Disambiguating Clashing Methods)

```scala
@targetName("f_string")
def f(x: => String): Int = x.length
def f(x: => Int): Int = x + 1  // OK
```

두 메서드 모두 소거된 매개변수 타입이 `Function0`이어서, 애너테이션 없이는 충돌(clash)이 발생합니다. 대상 이름이 이를 해소합니다.

#### 오버라이딩 제약(Overriding Constraints)

"두 멤버는 이름과 시그니처가 동일하고, 같은 소거된 이름을 공유하거나 동일한 타입을 가질 때 서로를 오버라이드할 수 있습니다. 오버라이드 관계가 성립하면 소거된 이름과 타입이 모두 일치해야 합니다."

이는 오버라이딩 관계가 깨지는 것을 방지합니다.

```scala
class A:
  def f(): Int = 1
class B extends A:
  @targetName("g") def f(): Int = 2  // 오류: 오버라이딩과 충돌
```

컴파일러는 이를 통해 상속 계층에서 `@targetName`으로 인한 의도치 않은 이름 충돌을 방지합니다.

---

## 8. 개선된 for(Better Fors)

### 8.1 개요(Overview)

Scala 3.8부터(또는 `-preview` 모드에서 3.7부터) for 내포(for-comprehensions)의 사용성이 개선되었으며, 특히 별칭(alias)으로 시작할 수 있게 된 점이 주요 변화입니다.

### 8.2 핵심 기능: for 내포에서의 별칭(Aliases in For-Comprehensions)

> 📘 **처음 배우는 분께 — "디슈가링"과 for의 정체**
>
> Scala의 `for ... yield`는 사실 그 자체로 특별한 문법이 아니라, 컴파일러가 속으로 `map`/`flatMap`/`withFilter` 호출로 **바꿔치기**합니다.
> 이렇게 편한 문법을 더 기본적인 형태로 풀어내는 과정을 **디슈가링(desugaring)**이라고 합니다(설탕을 벗긴다는 비유, 00번 11번 참고).
> 이 절은 그 변환 과정을 더 매끈하게 다듬어 불필요한 중간 단계를 줄였다는 이야기입니다.
> 사용자 입장에서 가장 눈에 띄는 변화는, 아래처럼 `for` 맨 앞에서 `as = ...`로 **값에 이름을 붙이며 시작**할 수 있다는 점입니다.

사용자 관점에서 가장 눈에 띄는 개선은, 값 별칭(value alias)으로 for 내포를 시작할 수 있게 된 것입니다.

```scala
for
  as = List(1, 2, 3)
  bs = List(4, 5, 6)
  a <- as
  b <- bs
yield a + b
```

이는 for 내포 앞에 명시적으로 `val` 선언을 두는 것과 동일하게 디슈가링(desugar)됩니다.

### 8.3 디슈가링 개선(Desugaring Improvements)

#### 1. 순수 별칭에 대한 단순화된 디슈가링(Simplified Desugaring for Pure Aliases)

별칭에 가드 조건(guard condition)이 없을 때 디슈가링이 더 단순해집니다. 이전에는 다음 코드가:

```scala
for {
  a <- doSth(arg)
  b = a
} yield a + b
```

튜플 래핑을 동반한 중첩 `map` 호출로 디슈가링되었지만, 이제는 더 직접적인 코드를 생성합니다.

```scala
doSth(arg).map { a =>
  val b = a
  a + b
}
```

이를 통해 불필요한 중간 튜플링과 매핑 연산이 제거됩니다.

#### 2. 중복된 map 호출 제거(Eliminating Redundant Map Calls)

최종 `yield` 표현식이 마지막 생성자 패턴(단일 변수이거나 변수들의 튜플)과 구문적으로 일치하면, 컴파일러는 불필요한 `map` 호출을 생략합니다.

```scala
for {
  a <- List(1, 2, 3)
} yield a
```

`List(1, 2, 3).map(a => a)` 대신 `List(1, 2, 3)`를 직접 생성합니다.

### 8.4 기술적 세부 사항(Technical Details)

디슈가링 로직은 컴파일러의 `Desugar.scala#makeFor`에 구현되어 있으며, for 내포 문법을 컬렉션 타입의 메서드 호출로 변환하는 작업을 담당합니다.

---

## 9. 안전한 초기화(Safe Initialization)

### 9.1 개요(Overview)

> 📘 **처음 배우는 분께 — "초기화 순서" 문제가 뭔가**
>
> 객체를 만들 때 필드(멤버 값)들은 위에서부터 차례로 채워집니다.
> 그런데 어떤 필드를 채우는 도중에 **아직 채워지지 않은 다른 필드**를 읽어 버리면, 거기엔 `null`이나 0 같은 엉뚱한 값이 들어 있어 버그가 생깁니다.
> 이 절의 "안전 초기화 검사기"는 그런 "아직 준비 안 된 값을 미리 쓰는" 위험한 코드를 **컴파일 시점에 미리 경고**해 주는 기능입니다.
> 이 절은 그 검사기의 내부 이론을 다루므로, 처음엔 "초기화 순서 실수를 잡아 주는 실험적 검사기" 정도로 알고 넘어가도 됩니다.

Scala 3는 `-Wsafe-init` 컴파일러 옵션으로 활성화되는 실험적 안전 초기화 검사기(safe initialization checker)를 제공합니다. 이 기능의 이론적 기반은 논문 _Safe object initialization, abstractly_에 설명되어 있습니다.

### 9.2 핵심 개념(Core Concept)

이 시스템은 객체 초기화 상태를 추적하기 위해 세 가지 추상화를 사용합니다.

- **Cold(차가움)**: 초기화되지 않은 필드를 포함할 수 있는 객체
- **Warm(따뜻함)**: 모든 필드가 초기화되었지만, cold 객체에 도달할 가능성이 있는 객체
- **Hot(뜨거움)**: 추이적으로 초기화된(transitively initialized) 객체로, warm 객체에만 도달함

검사기는 초기화 중인 현재 객체를 나타내는 `ThisRef`와, 외부 참조(outer references) 및 생성자 인자에 대한 추가 메타데이터를 가진 warm 객체를 추적하는 `Warm[C]`도 도입합니다.

### 9.3 설계 목표(Design Goals)

구현은 여섯 가지 주요 목표를 지향합니다.

1. **건전성(Soundness)**: 검사는 항상 종료되며, 일반적이고 합리적인 사용에 대해 건전(sound)합니다.
2. **표현력(Expressiveness)**: 표준적인 초기화 패턴을 지원합니다.
3. **사용자 친화성(User-friendliness)**: 최소한의 구문적 오버헤드와 유익한 오류 메시지를 제공합니다.
4. **모듈성(Modularity)**: 분석을 프로젝트 경계 내로 한정합니다.
5. **성능(Performance)**: 컴파일 중 즉각적인 피드백을 제공합니다.
6. **단순성(Simplicity)**: 핵심 타입 시스템을 수정하지 않습니다.

### 9.4 근본 원칙(Fundamental Principles)

**적층성(Stackability)**은 모든 클래스 필드가 클래스 본문의 끝에서 초기화되어야 함을 요구합니다. 단, 예외(exception) 같은 제어 효과(control effects)는 이를 방해할 수 있습니다.

**단조성(Monotonicity)**은 초기화 상태가 역행하지 않음을 보장합니다. 필드가 한 번 초기화되면 그 상태가 유지됩니다. 이미 초기화된 참조를 통해 초기화되지 않은 객체를 노출하는 재할당(reassignment)은 금지됩니다.

**범위성(Scopability)**은 초기화 프로토콜을 우회하는 측면 채널(side channels)이 없음을 보장합니다. 정적 필드(static fields)와 특정 제어 효과는 이 속성을 위반할 수 있으므로, 전달되는 값이 추이적으로 초기화되도록 강제할 필요가 있습니다.

**권한(Authority)**은 단조성을 보완하여, 초기화 상태가 진행될 수 있는 위치를 제한합니다. 이는 필수 초기화자(mandatory initializers)가 있는 클래스 본문이나 지역 추론 지점(local reasoning points)에서만 허용됩니다.

### 9.5 동작 규칙(Operational Rules)

검사기는 아홉 가지 핵심 규칙을 적용합니다.

1. cold 값은 필드 접근이나 메서드 호출을 통해 사용할 수 없습니다.
2. ThisRef는 초기화되지 않은 필드에 접근할 수 없습니다.
3. 재할당의 우변(right-hand side)은 실질적으로 hot(effectively hot)이어야 합니다.
4. 메서드 인자는 실질적으로 hot이어야 합니다(생성자와 parametric 메서드에 대한 특정 예외 있음).
5. 실질적으로 hot인 인자를 받는 hot 값은 hot 결과를 생성합니다.
6. ThisRef와 warm 값에 대한 메서드 호출은 정적 해석(static resolution)을 거칩니다.
7. 실질적으로 hot인 인자를 가진 `new` 표현식은 hot 결과를 생성합니다.
8. hot이 아닌 인자를 가진 `new` 표현식은 warm 결과를 생성하며, 재분석(re-analysis)을 유발합니다.
9. 패턴 매치 대상(scrutinee), 반환값, throw 표현식은 실질적으로 hot이어야 합니다.

### 9.6 실용 예시(Practical Examples)

검사기는 부모-자식 상호작용 문제, 예를 들어 초기화되지 않은 자식 필드를 참조하는 오버라이드 메서드에 접근하는 경우를 감지합니다.

```scala
abstract class AbstractFile:
  def name: String
  val extension: String = name.substring(4)

class RemoteFile(url: String) extends AbstractFile:
  val localFile: String = s"${url.##}.tmp"
  def name: String = localFile
```

`extension`의 초기화 중 `name`을 호출하고, 이것이 아직 초기화되지 않은 `localFile`에 접근하므로 경고가 발생합니다.

내부-외부 상호작용(inner-outer interactions)도 마찬가지로 검사됩니다. 시스템은 중첩 클래스 생성 시 외부 필드(outer fields)에 초기화 이전에 접근하는 경우를 감지합니다.

```scala
object Trees:
  class ValDef { counter += 1 }
  class EmptyValDef extends ValDef
  val theEmptyValDef = new EmptyValDef
  private var counter = 0
```

함수 캡처(function captures)는 캡처된 참조를 통해 초기화되지 않은 필드에 접근하지 않도록 분석됩니다.

### 9.7 실질적 hot 상태(Effective Hotness)

다음 조건 중 하나를 만족하면 "실질적으로 hot(effectively hot)"으로 간주됩니다.

- 명시적으로 hot이거나,
- 모든 필드가 할당된 ThisRef이거나,
- 내부 클래스(inner classes)가 없고 모든 필드가 실질적으로 hot인 Warm 값이거나,
- 반환 값이 실질적으로 hot인 함수일 때

검사기는 가능한 경우 hot이 아닌 값을 실질적으로 hot 상태로 승격(promote)하려고 시도합니다.

### 9.8 모듈성과 제약(Modularity and Limitations)

분석은 주 생성자(primary constructors)를 진입점으로 취급하며, TASTy를 사용하여 프로젝트 경계를 넘어 상위 클래스 생성자(superclass constructors)를 추적합니다. 객체 지향 상속에 내재한 클래스 간 결합(coupling) 때문에 경계를 넘나드는 추적이 건전성을 위해 필요합니다.

이 구현은 두 가지 제약을 인정합니다. "이 시스템은 Java나 Scala 2 클래스를 확장할 때 안전성을 보장할 수 없습니다." 그리고 "전역 객체(global objects)의 안전한 초기화는 부분적으로만 검사됩니다."

### 9.9 억제 메커니즘(Suppression Mechanisms)

`@unchecked` 애너테이션을 사용하거나 필드를 `lazy`로 선언하면 경고를 억제할 수 있습니다. 단, 이는 안전하지 않은 패턴을 승인하는 것이 아니라 임시방편(workaround)에 해당합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other New Features](https://docs.scala-lang.org/scala3/reference/other-new-features/)
