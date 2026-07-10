> 원본: https://rockthejvm.com/articles/scala-3-macros-comprehensive-guide

# Scala 3 매크로 가이드

Daniel Ciocirlan | 20분 읽기 | 2025년 2월 21일 | 가이드 | 중급

---

## 목차

1. [들어가며](#들어가며)
2. [프로젝트 설정](#프로젝트-설정)
3. [추상 구문 트리(AST)](#추상-구문-트리ast)
4. [첫 번째 Scala 3 매크로](#첫-번째-scala-3-매크로)
5. [표현식 패턴 매칭](#표현식-패턴-매칭)
6. [컴파일 타임 타입 안전 리플렉션](#컴파일-타임-타입-안전-리플렉션)
7. [매크로의 활용 가능성](#매크로의-활용-가능성)
8. [마무리](#마무리)

---

## 들어가며

Scala 3는 이미 상당히 훌륭한 언어다. 그럼에도 불구하고, Scala에서 가장 과소평가된 기능 중 하나는 타입 안전하고 컴파일 타임에 동작하는 **메타프로그래밍**이다. 이를 통해 컴파일 시점에 코드를 생성하거나, 확장하거나, 합성하거나, 그 밖에 다양한 방식으로 코드를 조작할 수 있다. 게다가 언어의 표현력과 타입 시스템의 엄격함을 그대로 유지하면서 말이다.

메타프로그래밍 자체는 새로운 개념이 아니다. Lisp 계열 언어들은 코드 구조와 데이터 구조가 동일하기 때문에, 해석 과정에서 코드를 조작할 수 있다. 다른 언어들도 텍스트 확장기, 텍스트 전처리기, 컴파일러 플러그인, 유사 인용(quasi-quotes) 등 다양한 기법을 제공한다.

하지만 Scala가 독특한 점은 **같은 언어**, 같은 자료 구조, 같은 표준 라이브러리를 사용해서 올바른 코드를 다른 올바른 코드로 변환할 수 있다는 것이다. 타입 시스템의 안전성과 동형(homoiconic) 표현(코드를 데이터로 다루는 방식)을 동시에 원한다면, Scala가 이상에 가장 가까운 언어다.

이 글은 Scala에서 가장 범용적인 메타프로그래밍 형태인 **Scala 3 매크로**를 다룬다. 이전 글인 [인라인에 대한 글](/articles/scala-3-inlines)의 후속이므로, 먼저 읽어보면 좋다. 물론 필수는 아니다. 이 글을 위해 인라인에 대해 알아야 할 것은 다음 두 가지뿐이다.

- inline 메서드는 호출 지점에서 메서드 본문으로 치환된다
- inline 인자는 메서드 본문 내에서 호출 지점에 전달된 표현식으로 치환된다

> **Tip**
>
> 이 글은 [Scala Macros and Metaprogramming 과정](/courses/scala-macros-and-metaprogramming)의 미리보기다. 이 과정을 통해 Scala에 대한 깊은 이해를 쌓고, 자신과 팀의 생산성을 높여줄 라이브러리와 도구를 만들 수 있다.

이 글은 독립적으로 읽을 수 있으며, 코드를 직접 따라 해보길 강력히 권한다.

---

## 프로젝트 설정

코드를 따라 해보려면(적극 권장!) 일반적인 Scala 프로젝트에 `build.sbt`에 다음 컴파일러 플래그만 추가하면 된다.

```scala
ThisBuild / scalacOptions ++= Seq(
  "-Xprint:postInlining",
  "-Xmax-inlines:100000"
)
```

인라인을 많이 다룰 것이므로, 컴파일러가 최종적으로 만들어낸 코드를 추적해야 한다. max-inlines 제한도 넉넉하게 올려 두었다. 재귀적 확장 등 예기치 않은 상황에 대비하기 위해서다.

---

## 추상 구문 트리(AST)

잠시 Scala를 잊고, 훨씬 단순한 "프로그래밍 언어"인 공학용 계산기를 생각해 보자.

공학용 계산기에서는 아무리 복잡한(단, 문법적으로 올바른) 수학 표현식이든 입력하고 "=" (또는 "실행")을 누르면 결과를 얻을 수 있다. 계산기가 최종 값을 반환하려면, 수학 표현식을 수학 법칙에 따라 예측 가능하게 평가할 수 있는 자료 구조로 변환해야 한다. 계산기가 수행할 수 있는 연산을 기술하는 자료 구조를 다음과 같이 상상해 보자.

```scala
trait Expr
case class Num(value: Double) extends Expr
case class Sum(lhs: Expr, rhs: Expr) extends Expr
case class Sub(lhs: Expr, rhs: Expr) extends Expr
case class Mul(lhs: Expr, rhs: Expr) extends Expr
case class Div(lhs: Expr, rhs: Expr) extends Expr
case class Sin(expr: Expr) extends Expr
case class Cos(expr: Expr) extends Expr
// ... 그 밖의 모든 연산
```

이렇게 정의하면, `2 + 3 / 4 + 2 * 8 * sin(30)` 같은 수식을 다음과 같은 Expr 인스턴스로 변환할 수 있다.

```scala
val computation =
    Sum(
        Num(2),
        Sum(
            Div(Num(3), Num(4)),
            Mul(
                Num(2),
                Mul(
                    Num(8),
                    Sin(Num(30))
                )
            )
        )
    )
```

이 자료 구조 덕분에 예측 가능한 처리가 된다. 이 자료 구조를 평가하는 알고리즘도 작성할 수 있다.

```scala
def evaluate(expr: Expr): Double = expr match {
    case Num(v) => v
    case Sum(lhs, rhs) => evaluate(lhs) + evaluate(rhs)
    case Sub(lhs, rhs) => evaluate(lhs) - evaluate(rhs)
    case Mul(lhs, rhs) => evaluate(lhs) * evaluate(rhs)
    case Div(lhs, rhs) => evaluate(lhs) / evaluate(rhs)
    case Sin(expr) => Math.sin(evaluate(expr)) // 또는 저수준 구현이 무엇이든
    // ... 나머지 연산들
}
```

물론 "자연스러운" 수학 표현식을 이 Expr 형태로 파싱하는 것이 핵심이다. 사실 종이에 적는 표기보다 Expr 자료 구조가 수학 표현식의 "더 자연스러운" 형태라고 할 수도 있겠지만, 여기서는 넘어가겠다.

이 Expr 데이터 타입을 **추상 구문 트리**(Abstract Syntax Tree), 줄여서 **AST**라고 부른다. 따라서 수학 표현식의 평가는 두 단계로 이뤄진다.

- 수학 표현식(문법적으로 올바른)을 AST로 변환
- AST를 평가

실제 프로그래밍 언어의 컴파일 과정도 크게 다르지 않다.

- 우리가 작성한 텍스트를 가져온다(여기서 "우리"에는 LLM 어시스턴트도 포함한다)
- 그 텍스트를 AST로 변환한다
- 그 AST를 바이너리로 변환한다

이 고수준 컴파일 과정은 최초의 컴파일러 이후 본질적으로 변하지 않았다. 세부 사항이 차이를 만들 뿐이다. Scala 컴파일러는 이 과정의 세부 단계를 위해 수십 개의 페이즈를 거치지만, 원리는 같다. 프로그래밍 언어가 훨씬 복잡하기 때문에, 사용할 수 있는 AST 노드의 종류도 훨씬 다양하다.

- import 문
- class/interface/enum/object 정의
- 필드, 메서드, 추상 타입, 타입 제약
- 제네릭
- 인자
- 값 정의
- 메서드 호출
- 일반적인 수학 스타일 표현식
- 그 밖의 모든 것

그렇다면 메타프로그래밍이란 무엇인가?

Scala에서 메타프로그래밍이란 AST를 조작하고, 그 AST를 다시 올바른 타입의 코드에 주입하여 결과를 사용하는 것이다. 단순해 보이지만 사실상 거의 불가능한 일이다. 이유는 AST를 얻고 나면 더 이상 "코드의 영역"에 있지 않게 되어 호스트 언어에 접근할 수 없기 때문이다. 소스 코드가 AST를 만들어 내는데, 어떻게 소스 코드에서 AST에 접근할 수 있겠는가?

Scala에서는 이것이 가능하다. 두 가지를 할 수 있다.

- 표현식을 AST로 변환하는 것, 이를 **인용**(quoting)이라 한다
- AST를 다시 표현식으로 변환하는 것, 이를 **접합**(splicing)이라 한다

어떻게 생겼는지 살펴보자.

---

## 첫 번째 Scala 3 매크로

매크로는 항상 두 부분으로 구성된다.

- 코드에서 표현식을 인용하여 AST를 구성한다
- 그 AST를 접합한다

일반 코드에서 표현식을 인용하거나 AST를 다른 자료 구조처럼 사용할 수는 없다. Scala 언어를 최대한 활용해서 클래스, 메서드, 값 등 무엇이든 만들 수 있지만, 최종 목표는 언제나 AST를 접합하여 일반 타입으로 되돌아가는 것이다.

매크로의 "hello world"를 살펴보자.

```scala
import scala.quoted.*

inline def firstMacro(number: Int, string: String): String =
    ${ firstMacroImpl('number, 'string) }

def firstMacroImpl(numExpr: Expr[Int], strExpr: Expr[String])(using Quotes): Expr[String] =
    Expr("This is my first macro")
```

살펴볼 것이 꽤 많다. 하나씩 짚어보자.

첫째, 모든 매크로는 반드시 `inline` 메서드여야 한다. 목표가 컴파일 타임에 AST를 조작하고 코드에 주입하는 것이므로, 컴파일러가 매크로 구현을 실행할 수 있도록 `inline` 키워드가 필수다.

둘째, 매크로의 구현은 항상 앞서 설명한 두 단계의 조합이다. AST를 구성한 다음 접합해야 한다. AST를 구성하는 행위가 `buildAST`이고 접합하는 행위가 가상의 메서드 `splice`라면, 메서드가 반환하는 값은 대략 이런 형태일 것이다.

```scala
splice(buildAST(myParam1, myParam2, ...))
```

실제로는 가상의 `splice` 메서드가 별도의 구문으로 제공된다. `splice(myAST)` 대신 `${ myAST }`라고 쓴다. 따라서 매크로 구현은 다음과 같이 작성된다.

```scala
${ buildAST(myParam1, myParam2, ...) }
```

관례상 `myMacro`라는 매크로를 작성하면, AST를 구성하는 함수는 `myMacroImpl`이라고 부른다. Scala의 매크로 기반 라이브러리 어디에서나 볼 수 있는 명명 규칙이다. 따라서 함수는 이렇게 된다.

```scala
${ firstMacroImpl(myParam1, myParam2, ...) }
```

인자는 어떨까? 매크로 구현의 핵심은 AST를 조작하는 것이다. AST를 처음부터 만들 수도 있지만, 대개 AST를 다른 AST로 변환하는 경우가 많다. 그래서 매크로 구현 함수는 보통 AST를 인자로 받는다. 앞서 말했듯이 일반 코드에서 **인용**을 통해 AST를 만들 수 있다. 표현식을 인용하려면 표현식 앞에 작은따옴표를 붙이면 된다. `firstMacro` 함수의 `number` 매개변수를 인용하려면 `'number`라고 쓰면 된다. 두 인자를 모두 AST로 만들어 매크로 구현 함수에 전달하면 최종 표현식은 다음과 같다.

```scala
${ firstMacroImpl('number, 'string) }
```

이것이 첫 번째 부분이다. 다음 부분, 특히 시그니처를 살펴보자. 타입 A인 표현식을 인용하면 `Expr[A]`로 기술되는 **타입이 있는 AST**를 얻는다. 공학용 계산기의 장난감 프로그래밍 언어와 비슷하지만, 타입을 유지한다는 점이 다르다(Scala니까!). 메서드의 목표는 메인 매크로가 접합에 사용할 또 다른 AST를 얻는 것이므로, `Expr[SomethingElse]`를 반환해야 한다. 메인 매크로가 String을 반환하므로, 매크로 구현은 `Expr[String]`을 반환해야 한다. 따라서 시그니처는 이렇다.

```scala
def firstMacroImpl(numExpr: Expr[Int], strExpr: Expr[String]): Expr[String]
```

`Expr` 타입과 매크로 API를 사용하려면 `scala.quoted.*`를 import해야 한다. 기억해야 할 import는 이것 하나뿐이다.

그렇다면 `Quotes`는 무엇인가? `Quotes`는 AST를 구성하고, 변환하고, 타입과 값을 합성하는 등의 API를 제공하는 객체다. 또한 `Quotes` 인스턴스에는 "리플렉션"이라는 특별한 패키지가 있어서, 표현식이나 타입의 속성을 검사하고 임의의 복잡한 AST를 수동으로 만들 수 있다(잠시 후 살펴볼 것이다). 따라서 완전한 시그니처는 다음과 같다.

```scala
def firstMacroImpl(numExpr: Expr[Int], strExpr: Expr[String])(using Quotes): Expr[String]
```

구현은 간단한 표현식이다. `Expr("This is my first macro")`는 접합되었을 때 **컴파일러**가 "This is my first macro"라는 값을 생성한다는 뜻이다.

그렇다면 매크로를 어떻게 사용할까?

매크로의 컴파일 방식 때문에 **매크로를 정의한 파일과 같은 파일에서 사용할 수 없다**. 보통 라이브러리에 작성된 매크로를 사용하므로 정의와 사용이 자연스럽게 분리되기 때문에 큰 문제는 아니다.

매크로를 다음과 같은 파일에 작성했다면:

```scala
package com.rockthejvm.macros

import scala.quoted.*

class MacrosDemo {
    inline def firstMacro(number: Int, string: String): String =
        ${ firstMacroImpl('number, 'string) }

    def firstMacroImpl(numExpr: Expr[Int], strExpr: Expr[String])(using Quotes): Expr[String] =
        Expr("This is my first macro")
}
```

다른 파일에서 매크로를 사용할 수 있다.

```scala
package com.rockthejvm.macros

import MacrosDemo.*

object MacrosDemoUsage {
    val firstMacroCall = firstMacro(42, "Scala")
}
```

SBT 콘솔에서 `~compile`을 실행하면, 컴파일러가 확장한 결과를 추적할 수 있다. 특히 사용 파일에서 다음과 같은 출력을 볼 수 있다.

```
[info] package com.rockthejvm.macros {
[info]   import com.rockthejvm.macros.MacrosDemo.*
[info]   final lazy module val MacrosDemoUsage: com.rockthejvm.macros.MacrosDemoUsage
[info]      = new com.rockthejvm.macros.MacrosDemoUsage()
[info]   @SourceFile("src/main/scala/com/rockthejvm/macros/MacrosDemoUsage.scala")
[info]     final module class MacrosDemoUsage() extends Object() {
[info]     this: com.rockthejvm.macros.MacrosDemoUsage.type =>
[info]     private def writeReplace(): AnyRef =
[info]       new scala.runtime.ModuleSerializationProxy(
[info]         classOf[com.rockthejvm.macros.MacrosDemoUsage.type])
[info]     val firstMacroUsage: String = "This is my first macro":String
[info]   }
```

마지막 줄을 보자. 컴파일러가 다음을 계산했다.

- `firstMacroUsage`의 값은 매크로 함수가 반환한 "This is my first macro"다
- 그 값의 타입은 `String`이다

즉, **매크로는 컴파일 타임에 계산된다**. 당연히 그래야 한다. AST를 구성하고 일반 코드에 주입하는 것이 매크로의 존재 이유니까.

축하한다. 첫 번째 매크로를 작성하고 사용해 보았다!

매크로 구현 내부에서는 임의의 계산을 실행할 수 있다. 하나 해보자. 숫자가 3보다 크면 문자열을 n번 반복하고, 그렇지 않으면 문자열에서 n / 2개의 문자만 가져오는 것이다. 장난감 같은 예제이지만, 핵심적인 질문을 던진다. Expr의 값을 어떻게 얻을 수 있을까?

`Expr[A]` 타입의 AST는 값을 가질 수도 있고 그렇지 않을 수도 있다. 컴파일러가 (컴파일 시점에) 원래 표현식의 값을 알 수 있는지 여부에 달려 있다. 예를 들어 리터럴 문자열, 숫자, `2 + 3` 같은 단순한 수학 연산은 컴파일러가 알 수 있다. 반면 `getMeaningOfLife()` 같은 표현식은 우리가 42라는 것을 알고 메서드가 그 값을 반환하더라도, 컴파일러는 알 수 없다.

매크로 API를 사용하면 `Expr`의 값을 얻어볼 수 있고, 값을 알 수 없으면 컴파일 오류를 발생시킬 수 있다. 위의 장난감 로직을 구현한 코드는 다음과 같다.

```scala
def firstMacroImpl(numAST: Expr[Int], stringAST: Expr[String])(using Quotes): Expr[String] = {
  val numValue    = numAST.valueOrAbort // 컴파일 타임에 계산할 수 없으면 컴파일 오류 발생
  val stringValue = stringAST.valueOrAbort
  val newString =
    if (numValue > 3) stringValue.repeat(numValue)
    else stringValue.take(numValue / 2)

  Expr("The macro impl is: " + newString)
}
```

이것을 다시 컴파일하면 SBT에서 다음과 같은 출력을 볼 수 있다.

```
[info] [[syntax trees at end of              postInlining]] // /Users/daniel/dev/rockthejvm/blog-projects/scala-macros-demo/src/main/scala/com/rockthejvm/macros/MacrosDemoUsage.scala
[info] package com.rockthejvm.macros {
[info]   import com.rockthejvm.macros.MacrosDemo.*
[info]   final lazy module val MacrosDemoUsage: com.rockthejvm.macros.MacrosDemoUsage
[info]      = new com.rockthejvm.macros.MacrosDemoUsage()
[info]   @SourceFile("src/main/scala/com/rockthejvm/macros/MacrosDemoUsage.scala")
[info]     final module class MacrosDemoUsage() extends Object() {
[info]     this: com.rockthejvm.macros.MacrosDemoUsage.type =>
[info]     private def writeReplace(): AnyRef =
[info]       new scala.runtime.ModuleSerializationProxy(
[info]         classOf[com.rockthejvm.macros.MacrosDemoUsage.type])
[info]     val firstMacroUsage: String =
[info]       "The macro impl is: ScalaScalaScalaScalaScala":String
[info]   }
[info] }
```

마지막 줄이 확인해야 할 부분이다. 컴파일러가 매크로의 로직을 컴파일 타임에 실행했고, 기대한 결과를 얻었다.

다시 강조하자면, 매크로 안에서는 임의의 코드를 실행할 수 있다. 이것은 엄청난 능력이다. 당연한 귀결이 하나 있다. 매크로 내부의 계산이 복잡할수록 컴파일 시간이 길어진다.

---

## 표현식 패턴 매칭

실제 상황에서 처리하려는 표현식과 조작하려는 AST는 대개 꽤 크다. Scala 3 매크로 API를 사용하면 표현식을 패턴 매칭하고 조작할 부분을 추출할 수 있다.

간단한 예제를 살펴보자. Option이 어떻게 만들어졌는지에 따라, 컴파일 타임에 Option을 설명하는 매크로를 만들어 보겠다.

```scala
inline def pmOptions(inline opt: Option[Int]) =
    ${ pmOptionsImpl('opt) }

def pmOptionsImpl(opt: Expr[Option[Int]])(using Quotes): Expr[String] = ???
```

계산기 예제는 표현식을 평가할 때 가능한 값들의 case class를 패턴 매칭하면 되므로 쉬웠다. Scala답게, 훨씬 복잡한 것들도 패턴 매칭할 수 있고 **인용 패턴**(quote pattern)으로 하위 표현식을 추출할 수 있다.

구현을 보기 전에, `opt` 매개변수가 `inline`으로 표시된 것에 주목하자. 매크로 구현에서 전체 표현식에 접근하기 위해서다. 이제 구현을 살펴보자.

`pmOptionsImpl`의 예제는 다음과 같다.

```scala
def pmOptionsImpl(opt: Expr[Option[Int]])(using Quotes): Expr[String] = {
  val result = opt match {
    case '{ Some(42) }                   => "got the meaning of life"
    case '{ Some($x) }                   => s"got a variable: ${x.show}"
    case '{ ($o: Option[a]).map[b]($f) } => "mapping an option"
    case _                               => "got something else"
  }
  Expr(result)
}
```

구문부터 살펴보자. `Expr[A]`를 `'{ }` 형태의 case로 패턴 매칭할 수 있으며, `{}` 안에 해당 표현식을 만든 (거의) 원래 코드를 쓸 수 있다. 첫 번째 case는 `'{ Some(42) }`이므로, `pmOptions(Some(42))`를 사용하면 이 case에 매칭된다.

두 번째 case는 표현식 **변수**를 포함한다. 패턴 매칭 규칙은 일반 값의 패턴 매칭과 거의 동일하지만, 표현식을 캡처할 때 `$variable`을 사용한다. `expression.show`를 호출하면 해당 표현식이 대응하는 코드를 문자열로 보여준다. SBT 기반의 자세한 출력 세션에서 유용하다.

세 번째 case는 꽤 파격적이다. `Option(x).map(f)` 형태의 표현식이 코드에 정확히 그렇게 등장한 경우를 매칭한다. 먼저 `Option[a]` 표현식을 분리하는데, `a`는 컴파일러가 알아낼 수 있는 타입 **변수**다. 그런 다음 `.map`을 "호출"하는데, 이것은 실제 메서드 호출이 아니라 **패턴**이다. 다른 타입 변수 `b`를 추가하고 필요하다면 함수 표현식 `$f`를 추출한다. 인용 case 안에서 할 수 있는 것이 많다. 타입 변수와 제약을 추가하거나, 하위 표현식을 추출하거나, 제네릭 타입을 매칭하거나, 복잡한 체인에서 타입의 동일성을 강제할 수도 있다(예를 들어, 결과 타입이 매번 같은 경우에만 체이닝된 `.map`을 매칭하는 것).

이제 사용해 보자. 사용 파일에 다음을 추가한다.

```scala
val optionDescription   = pmOptions(Some(2))
val optionDescription_2 = pmOptions(Option(2))
val optionDescription_3 = pmOptions(Option(10).map(_ + 1))
```

다음과 같은 출력을 얻는다(이전과 마찬가지로 마지막 줄들이 중요하다).

```
[info] package com.rockthejvm.macros {
[info]   import com.rockthejvm.macros.MacrosDemo.*
[info]   final lazy module val MacrosDemoUsage: com.rockthejvm.macros.MacrosDemoUsage
[info]      = new com.rockthejvm.macros.MacrosDemoUsage()
[info]   @SourceFile("src/main/scala/com/rockthejvm/macros/MacrosDemoUsage.scala")
[info]     final module class MacrosDemoUsage() extends Object() {
[info]     this: com.rockthejvm.macros.MacrosDemoUsage.type =>
[info]     private def writeReplace(): AnyRef =
[info]       new scala.runtime.ModuleSerializationProxy(
[info]         classOf[com.rockthejvm.macros.MacrosDemoUsage.type])
[info]     val firstMacroUsage: String =
[info]       "The macro impl is: ScalaScalaScalaScalaScala":String
[info]     val optionDescription: String = "got a variable: 2":String
[info]     val optionDescription_2: String = "got something else":String
[info]     val optionDescription_3: String = "mapping an option":String
[info]   }
[info] }
```

매크로가 모든 경우에서 컴파일 타임에 평가되었고, map 구조까지 잘 처리된 것을 볼 수 있다. 인용 패턴 매칭은 놀라울 정도로 강력하다.

그런데 한 가지 주목할 점이 있다. `Option(2)`는 어떤 case에도 매칭되지 않았다! 이것은 중요하다. `Some(2)`와 `Option(2)`가 런타임에 동일한 객체를 만들더라도, 같은 표현식이 아니다. 우리가 다루는 것은 코드 자체이고, 이 두 코드 조각은 다르며, 컴파일러(와 패턴 매치)는 그것을 구별한다.

---

## 컴파일 타임 타입 안전 리플렉션

매크로에 대해 이야기할 것이 훨씬 많지만, 한 가지 더 언급할 만한 예제는 임의의 AST를 합성하는 능력이다. 이것이 "리플렉션"의 영역이다.

"리플렉션"이라고 하면 보통 런타임에 필드와 메서드를 검사하는 Java API를 떠올린다. 메서드가 존재하지 않으면 프로그램이 죽고, 애노테이션으로 Spring 스타일의 온갖 마법 같은 의존성 주입을 만들어 내는 그것 말이다. 여기서 "리플렉션"이란 AST를 동적으로 검사하고 조작하되, **컴파일 타임**에 **모든 타입 안전성**을 유지하면서 한다는 뜻이다.

리플렉션의 고전적인 사용 사례를 생각해 보자. 문자열로 된 이름으로 메서드를 호출하는 것이다.

매크로 패턴에 이제 익숙하므로, 매크로 진입점부터 시작할 수 있다.

```scala
inline def callMethodDynamically[A](instance: A, methodName: String, arg: Int): String =
    ${ callMethodDynamicallyImpl('instance, 'methodName, 'arg) }
```

이 메서드를 호출하면, 컴파일러가 `methodName`으로 지정된 메서드를 찾아서 인자 `arg`로 호출하길 원한다. 물론 컴파일 타임에. 다음과 같은 클래스를 만들면(사용 파일에 추가하자):

```scala
case class SimpleWrapper(x: Int) {
    def magicMethod(y: Int) =
        s"This simple wrapper called a magic returning ${x + y}"
}
```

다음과 같은 값을 만들 때:

```scala
val magicCall = callMethodDynamically(SimpleWrapper(2), "magicMethod", 10)
```

컴파일러가 이것을 다음과 같이 변환해 주길 원한다.

```scala
val magicCall = SimpleWrapper(2).magicMethod(10)
```

그리고 당연히, 해당 클래스에 해당 메서드가 존재하지 않으면 컴파일이 되지 않아야 한다.

임의의 AST를 구성하려면 새로운 것을 살펴봐야 한다. `Expr[A]`가 코드에 주입(하고 실행)되었을 때 타입 A의 값을 반환하는 타입이 있는 표현식이라면, 임의의 `Expr[A]`를 수동으로 구성할 수는 없고 코드에서 인용하는 방법밖에 없다. 임의의 AST는 값을 반환하는 표현식일 수도 있지만, 반드시 그런 것은 아니다. 값을 생성하는 표현식이 언어의 전부가 아니기 때문이다. 타입 정의, 필드, 인자, 변수 참조 등은 표현되어야 하지만 그 자체로 값을 생성하지는 않는 AST의 예다. 일반적인 AST의 타입을 `Term`이라 한다.

임의의 AST를 생성한다는 것은 Term을 만들고 조작한다는 뜻이다. 최종 결과는 Term을 신중하게 다룬 후에 만들어지는 `Expr[Something]`이 된다.

"동적" 메서드 호출을 위해, 메서드 호출을 나타내는 Term을 만들고, Term이 완전히 구성되면 이 경우 `Expr[String]`으로 변환해야 한다.

구현을 단계별로 만들어 보자.

```scala
def callMethodDynamicallyImpl[A](instance: Expr[A], methodName: Expr[String], arg: Expr[Int])(using
    q: Quotes
): Expr[String] = {
    import q.reflect.*

    // TODO
}
```

이미 몇 가지가 달라졌다. given `Quotes`에 이름을 붙였는데, `q.reflect.*`를 import해야 하기 때문이다. 이 API가 임의의 AST를 구성하고 검사할 수 있게 해준다. `reflect` 패키지는 Expr과 Term 사이를 변환하는 확장 메서드를 제공한다.

해야 할 일은 다음과 같다.

- 현재 Expr인 `instance`에서 Term을 얻는다
- 메서드 참조를 나타내는 Term을 만든다
- 해당 인스턴스에서 메서드를 호출하는 Term을 만든다
- 그 Term을 다시 Expr로 변환한다

단계별로 보자. `instance`를 Term으로 변환:

```scala
val term = instance.asTerm // Expr를 Term으로 변환
```

그 term에서 메서드를 찾기:

```scala
val method = Select.unique(term, methodName.valueOrAbort)
```

메서드를 호출:

```scala
val invocation = Apply(method, List(arg.asTerm))
```

마지막으로, term을 원하는 Expr 타입으로 반환:

```scala
invocation.asExprOf[String]
```

전체 코드는 다음과 같다.

```scala
inline def callMethodDynamically[A](instance: A, methodName: String, arg: Int): String =
  ${ callMethodDynamicallyImpl('instance, 'methodName, 'arg) }
def callMethodDynamicallyImpl[A](instance: Expr[A], methodName: Expr[String], arg: Expr[Int])(using
    q: Quotes
): Expr[String] = {
  import q.reflect.*
  // Expr를 Term으로 변환
  val term       = instance.asTerm
  // 메서드 찾기
  val method     = Select.unique(term, methodName.valueOrAbort)
  // 메서드 호출 생성
  val invocation = Apply(method, List(arg.asTerm)) // == instance.method(arg)
  // 최종 Expr 반환
  invocation.asExprOf[String]
}
```

사용 파일에 다음을 작성하면:

```scala
val result = callMethodDynamically(SimpleWrapper(10), "magicMethod", 42)
```

다음과 같은 컴파일러 출력을 볼 수 있다.

```
[info]     val result: String =
[info]       {
[info]         val instance$proxy1: com.rockthejvm.macros.MacrosDemoUsage.SimpleWrapper
[info]            = com.rockthejvm.macros.MacrosDemoUsage.SimpleWrapper.apply(10)
[info]         instance$proxy1.magicMethod(42):String
[info]       }
```

이것은 `SimpleWrapper(10).magicMethod(42)`와 동일하다.

당연히, 클래스에 존재하지 않는 메서드 이름을 전달하면 컴파일러가 오류를 보여준다.

```
[error] -- [E008] Not Found Error: /Users/daniel/dev/rockthejvm/blog-projects/scala-macros-demo/src/main/scala/com/rockthejvm/macros/MacrosDemoUsage.scala:18:44
[error] 18 |  val result        = callMethodDynamically(SimpleWrapper(10), "stupidMethod", 42)
[error]    |                                            ^^^^^^^^^^^^^^^^^
[error]    |value stupidMethod is not a member of com.rockthejvm.macros.MacrosDemoUsage.SimpleWrapper
[error] one error found
[error] (Compile / compileIncremental) Compilation failed
```

모든 것이 컴파일 타임에 일어난다!

메서드 호출에 적합한 메서드를 찾는 이 과정은 일반 코드에서 하는 것과 크게 다르지 않다. `SimpleWrapper(10).stupidMethod(42)`라고 직접 호출해도 정확히 같은 오류가 발생한다. 컴파일러가 정확히 같은 탐색을 수행하기 때문이다.

---

## 매크로의 활용 가능성

보다시피 매크로는 놀라울 정도로 강력하다. 매크로로 할 수 있는 것들의 예시는 다음과 같다.

- 타입 합성
- 타입에 대한 패턴 매칭
- 타입 클래스 인스턴스 도출(derivation)
- given 소환 또는 생성(하나씩 또는 일괄)
- 새로운 값, 메서드, 또는 임의의 코드 생성
- 특정 위치에서 컴파일 오류 보고
- 코드 최적화
- 정상적인 코드를 의도적으로 컴파일 실패시키기(예: 모범 사례 강제)
- 더 나은 컴파일을 돕는 임의의 코드 실행(예: 파일 읽기, 스키마 파싱, 데이터베이스 연결까지)

그리고 이 밖에도 훨씬 많다.

---

## 마무리

이 글에서는 매크로의 기본 구조를 살펴보고, 매크로가 유용한 이유를 설명했다. AST, 매크로 패턴, 새로운 구문, 표현식 구성, 값 추출, 인용 및 다양한 코드 구조에 대한 패턴 매칭, 그리고 컴파일 타임 "리플렉션" 패키지를 활용한 직접적인 AST 구성까지 다뤘다.

이 글이 Scala 매크로에 대한 호기심을 불러일으켰으면 한다. Scala 컴파일러 팀과 독학한 소수의 라이브러리 저자를 제외하면, 매크로에 대한 역량을 갖춘 사람은 극히 드물다. Scala 커뮤니티에서 더 많은 사람이 매크로를 배우고, 이를 활용해 훌륭한 도구와 라이브러리를 만들어 주길 바란다.
