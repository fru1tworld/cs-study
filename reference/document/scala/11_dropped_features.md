# Scala 3 삭제된 기능

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Dropped Features" 섹션을 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/dropped-features/

---

## 목차

1. [개요](#개요)
2. [DelayedInit](#delayedinit)
3. [Scala 2 매크로(Scala 2 Macros)](#scala-2-매크로scala-2-macros)
4. [존재 타입(Existential Types)](#존재-타입existential-types)
5. [일반 타입 프로젝션(General Type Projection)](#일반-타입-프로젝션general-type-projection)
6. [do-while](#do-while)
7. [프로시저 문법(Procedure Syntax)](#프로시저-문법procedure-syntax)
8. [패키지 객체(Package Objects)](#패키지-객체package-objects)
9. [초기화 선행자(Early Initializers)](#초기화-선행자early-initializers)
10. [22 제한(Limit 22)](#22-제한limit-22)
11. [자동 적용(Auto-Application)](#자동-적용auto-application)
12. [비지역 반환(Nonlocal Returns)](#비지역-반환nonlocal-returns)
13. [private[this]와 protected[this]](#privatethis와-protectedthis)
14. [참고 자료](#참고-자료)

---

## 개요

Scala 3 레퍼런스 문서는 Scala 2에 존재했지만 Scala 3에서 더 이상 지원되지 않는 기능들을 정리합니다. 이 목록은 개발자가 Scala 2에서 Scala 3로 업그레이드할 때 발생하는 호환성 변경 사항(compatibility breaks)을 이해하는 데 도움을 줍니다.

문서는 Scala 2에서 제거되었거나 더 이상 권장되지 않는(deprecated) 다음과 같은 기능들을 다룹니다.

1. **DelayedInit** — 트레이트(trait) 초기화 메커니즘
2. **Scala 2 매크로(Scala 2 Macros)** — Scala 3의 새로운 매크로 시스템으로 대체됨
3. **존재 타입(Existential Types)** — 경계(bound)를 가진 타입 와일드카드
4. **일반 타입 프로젝션(General Type Projection)** — `T#A` 형태의 타입 프로젝션
5. **do-while** — 반복문(loop) 구문
6. **프로시저 문법(Procedure Syntax)** — 명시적 반환 타입이 없는 메서드
7. **패키지 객체(Package Objects)** — 패키지 수준의 정의
8. **초기화 선행자(Early Initializers)** — 트레이트 필드 사전 초기화
9. **클래스 섀도잉(Class Shadowing)** — 내부 클래스 오버라이딩
10. **22 제한(Limit 22)** — 튜플/함수 인자 개수 제한
11. **XML 리터럴(XML Literals)** — 코드에 내장된 XML 구문
12. **심볼 리터럴(Symbol Literals)** — `'symbol` 표기법
13. **자동 적용(Auto-Application)** — 암묵적 함수 적용
14. **약한 적합성(Weak Conformance)** — 수치 타입 변환
15. **비지역 반환(Nonlocal Returns)** — 더 이상 권장되지 않음(비지역 제어 흐름)
16. **private[this]와 protected[this]** — 접근 한정자(access modifier)
17. **와일드카드 초기화(Wildcard Initializer)** — 초기화되지 않은 필드 선언

각 기능은 그 변경의 근거(rationale)와 마이그레이션 지침을 설명하는 상세 문서로 연결됩니다. 본 번역에서는 위 작업 목록에서 지정된 핵심 페이지들을 중심으로 상세히 다룹니다.

---

## DelayedInit

`DelayedInit` 트레이트(trait)에 대한 특수 처리는 Scala 3에서 더 이상 지원되지 않습니다.

### App 클래스에 미치는 영향

이전에 `DelayedInit`에 의존하던 `App` 클래스는 이제 부분적으로 동작하지 않습니다. 여전히 간단한 메인 프로그램을 작성하는 방법으로 `App`을 사용할 수는 있습니다.

```scala
object HelloWorld extends App {
  println("Hello, world!")
}
```

그러나 이제 코드는 객체의 초기화자(initializer) 안에서 실행됩니다. 이는 일부 JVM에서 해당 코드가 인터프리터로만 실행됨을 의미합니다. 따라서 이 방식은 벤치마킹(benchmarking) 목적에는 적합하지 않습니다.

### 명령줄 인자 접근

명령줄 인자(command line arguments)에 접근해야 한다면 명시적인 `main` 메서드를 사용해야 합니다.

```scala
object Hello:
  def main(args: Array[String]) =
    println(s"Hello, ${args(0)}")
```

### 권장 대안

Scala 3는 `@main` 메서드를 통해 더 나은 대안을 제공합니다. `@main` 메서드는 "프로그램(program)" 객체를 사용하는 방식보다 프로그램 진입점(entry point)을 생성하는 데 더 편리한 방법을 제공합니다.

**핵심 요약:** `DelayedInit` 트레이트에 대한 특수 처리는 더 이상 지원되지 않으며, 개발자는 새로운 코드에서 `@main` 메서드를 사용하도록 마이그레이션해야 합니다.

---

## Scala 2 매크로(Scala 2 Macros)

이전의 실험적(experimental) 매크로 시스템은 삭제되었습니다.

대신 두 가지 상호 보완적인 개념인 `inline`과 `'{ ... }`/`${ ... }` 코드 생성에 기반한, 더 깔끔하고 더 제한적인 시스템이 제공됩니다. `'{ ... }`는 코드의 컴파일을 지연시키고 해당 코드를 담은 객체(object)를 생성합니다. 이와 쌍대적으로(dually) `${ ... }`는 코드를 생성하는 표현식(expression)을 평가하여 그 결과를 둘러싼 `${ ... }` 위치에 삽입합니다. 이 환경에서, `${ ... }`를 포함하는 인라인(inlined) 표시된 정의는 매크로(macro)이며, `${ ... }` 내부의 코드는 컴파일 타임(compile-time)에 실행되어 `'{ ... }` 형태의 코드를 생성합니다. 추가로, 코드의 내용은 `'{ ... }`/`${ ... }` 프레임워크의 확장인 더 복잡한 리플렉션(reflection) API를 통해 검사하고 생성할 수 있습니다.

- `inline`은 Scala 3에서 [구현](../metaprogramming/inline.html)되었습니다.
- 인용(Quotes) `'{ ... }`과 스플라이스(splices) `${ ... }`는 Scala 3에서 [구현](../metaprogramming/macros.html)되었습니다.
- [TASTy 리플렉션(TASTy reflect)](../metaprogramming/reflection.html)은 인용된 코드(quoted code)를 검사하거나 생성하기 위한, 더 복잡한 트리(tree) 기반 API를 제공합니다.

---

## 존재 타입(Existential Types)

`forSome`를 사용하는 존재 타입(existential types, SLS §3.2.12에 명세됨)은 Scala 3에서 제거되었습니다. 언어 개발자들은 세 가지 주요 근거를 제시했습니다.

**타입 건전성(Type Soundness) 우려**
이 제거는 DOT와 Scala 3의 기반이 되는 근본적인 타입 건전성 원칙(type soundness principle)의 위배를 해소합니다. 이 원칙은, 타입 셀렉션(type selection)의 모든 접두부(prefix)는 런타임에 생성된 값(runtime-constructed value)에서 비롯되거나, 확립된 경계(bound)를 가진 타입을 참조해야 함을 요구합니다.

**기능 간 상호작용 문제**
존재 타입은 다른 언어 구성 요소(construct)와 상호작용할 때 상당한 복잡성을 발생시켰으며, 이는 유지보수와 예측 가능성 측면에서 어려움을 만들어냈습니다.

**제한적인 고유 가치**
존재 타입은 경로 의존 타입(path-dependent types)과 상당 부분 겹치므로, 언어에 대한 고유한 기여가 제한적이었습니다.

### 와일드카드 기반 대안

`forSome` 없이 와일드카드(wildcards)로 표현되는 존재 타입은 여전히 지원되지만 다른 방식으로 처리됩니다. 시스템은 이제 이를 정제 타입(refined types)으로 해석합니다. 예를 들어,

```
Map[_ <: AnyRef, Int]
```

는, 첫 번째 타입 파라미터가 상한(upper bound)으로 `AnyRef`를 가지고 두 번째 타입 파라미터가 `Int`의 별칭(alias)인 `Map` 타입으로 처리됩니다.

### Scala 2 호환성

Scala 2 컴파일 결과로 생성된 클래스 파일(class files)을 처리할 때, Scala 3는 자신의 타입 시스템을 사용해 존재 타입을 최선의 근사(best-effort approximation)로 처리하려 시도하며, 이 변환 방식의 한계에 대해 경고(warning)를 발생시킵니다.

---

## 일반 타입 프로젝션(General Type Projection)

Scala 2는 임의의 타입 `T`와 그 타입 멤버(type member)인 `A`에 대해 일반 타입 프로젝션(general type projection) `T#A`를 허용했습니다. 이 기능은 건전성(soundness) 우려로 인해 Scala 3에서 제거되었습니다.

### Scala 3의 주요 제약

Scala 3는 `T`가 (추상이 아닌) 구체 타입(concrete type)일 때만 타입 프로젝션을 허용합니다. 다음과 같은 경우 타입은 추상(abstract)으로 간주됩니다.

- 추상 타입 멤버(`= SomeType` 없이 선언된 `type T`)
- 타입 파라미터(`[T]`)
- 추상 타입에 대한 별칭(`type T = SomeAbstractType`)

`A`에 대해서는, `T`의 멤버 타입이기만 하면(예: 하위 클래스 `class T { class A }`) 별다른 제약이 없습니다.

### 마이그레이션 지침

추상 타입을 대상으로 한 타입 프로젝션에 의존하는 코드의 경우, 개발자는 다음 방법의 사용을 고려해야 합니다.

- 경로 의존 타입(path-dependent types)
- 암묵적 파라미터(implicit parameters)

### 주목할 만한 영향

이 제약은 Scala 2에서 지원되던 패턴인 "콤비네이터 계산법(combinator calculus)의 타입 수준 인코딩(type-level encoding)"의 구현을 불가능하게 만듭니다.

이 변경은 문서화된 불건전성(unsoundness) 문제를 해소하며, Scala 3의 더 엄격한 타입 시스템 설계와 일관됩니다.

---

## do-while

다음과 같은 구문 구성 요소는

```
do <body> while <cond>
```

더 이상 지원되지 않습니다. 대신 아래의 동등한 `while` 반복문을 사용하는 것이 권장됩니다.

```
while ({ <body> ; <cond> }) ()
```

예를 들어, 다음 코드 대신

```
do
  i += 1
while (f(i) == 0)
```

이렇게 작성합니다.

```
while
  i += 1
  f(i) == 0
do ()
```

`while`의 조건으로 블록(block)을 사용하는 이 아이디어는 "1.5회 반복(loop-and-a-half)" 문제에 대한 해결책도 제공합니다. 다음은 또 다른 예입니다.

```
while
  val x: Int = iterator.next
  x >= 0
do print(".")
```

### 이 구성 요소를 삭제한 이유

- `do-while`은 비교적 드물게 사용되며, `while`만으로도 충실하게(faithfully) 표현할 수 있습니다. 따라서 이를 별도의 구문 구성 요소로 두는 것에는 큰 의의가 없어 보입니다.
- 새로운 구문 규칙(syntax rules) 하에서 `do`는 문장 연속(statement continuation)으로 사용되는데, 이는 문장 도입(statement introduction)으로서의 의미와 충돌하게 됩니다.

---

## 프로시저 문법(Procedure Syntax)

**프로시저 문법(procedure syntax)**은 Scala 3에서 제거되었습니다. 다음과 같은 옛 구문 형식은

```
def f() { ... }
```

더 이상 지원되지 않습니다. 개발자는 다음 대안 중 하나를 사용해야 합니다.

```
def f() = { ... }
def f(): Unit = { ... }
```

### 마이그레이션 지원

Scala 3는 레거시(legacy) 코드를 위한 하위 호환성 옵션을 제공합니다. `-source:3.0-migration` 컴파일러 플래그를 사용하면 옛 구문이 허용됩니다. 추가로, `-migration` 옵션을 활성화하면 컴파일러가 구식 코드를 Scala 3 표준에 맞도록 자동으로 재작성(rewrite)할 수 있습니다.

[Scalafix](https://scalacenter.github.io/scalafix/) 도구 또한 프로시저 문법을 Scala 3 호환 형식으로 변환하는 자동 재작성 기능을 제공하여, 대규모 코드베이스의 마이그레이션 과정을 단순화합니다.

---

## 패키지 객체(Package Objects)

아래와 같은 형태의 패키지 객체(package objects)는 더 이상 권장되지 않으며(deprecated) 결국 제거될 예정입니다.

```scala
package object p {
  val a = ...
  def b = ...
}
```

### 삭제되는 이유

"이제 모든 종류의 정의를 최상위(top-level)에 작성할 수 있으므로, 패키지 객체는 더 이상 필요하지 않습니다."

### 현대적인 대안

패키지 객체 대신, 개발자는 이제 소스 파일 내에서 정의를 패키지 수준(package level)에 직접 배치할 수 있습니다.

```scala
package p
type Labelled[T] = (String, T)
val a: Labelled[Int] = ("count", 1)
def b = a._2

case class C()

extension (x: C) def pair(y: C) = (x, y)
```

같은 패키지에 속한 여러 소스 파일들은 클래스 및 객체와 자유롭게 섞인 최상위 정의를 포함할 수 있습니다.

### 구현 세부 사항

컴파일러는 대상이 되는 최상위 정의들에 대해 합성된 래퍼 객체(synthetic wrapper objects)를 생성합니다. `src.scala`라는 이름의 파일이 그러한 정의들을 포함하고 있다면, 이들은 `src$package`라는 이름의 합성 객체(synthetic object)로 감싸집니다. 이 감싸기 동작은 패키지를 통해 정의에 접근하는 사용자에게는 투명하게(transparent) 처리됩니다.

### 중요한 고려 사항

이 동작에 관한 네 가지 핵심 사항은 다음과 같습니다.

1. 정의가 감싸질 때, 소스 파일 이름은 바이너리 호환성(binary compatibility)에 영향을 줍니다.
2. 메인 메서드(main methods)는 (자동 감싸기에 의존하지 말고) 명시적으로 이름이 붙은 객체에 배치해야 합니다.
3. `private` 한정자는 감싸기 여부와 무관하게 동작합니다.
4. 같은 이름을 공유하는 오버로딩(overloaded) 정의들은 반드시 동일한 소스 파일에서 비롯되어야 합니다.

---

## 초기화 선행자(Early Initializers)

초기화 선행자(early initializers)는 Scala 2의 기능으로, 다음과 같은 특정 구문을 사용하여 클래스 선언 내에서 초기화 코드를 실행할 수 있게 해주었습니다.

```
class C extends { ... } with SuperClass ...
```

이 기능은 Scala 3에서 제거되었습니다. 문서에 따르면 초기화 선행자는 "드물게 사용되었으며, 주로 트레이트 파라미터(trait parameters)의 부재를 보완하기 위해 쓰였는데, 이는 이제 Scala 3에서 직접 지원됩니다."

### 핵심 사항

이 기능을 삭제한 주된 이유는, 그것이 우회책(workaround) 목적을 수행했기 때문입니다. Scala 3는 트레이트 파라미터(trait parameters)를 일급(first-class) 언어 기능으로 도입하므로, 개발자는 이제 상속 시점에 트레이트로 파라미터를 직접 전달할 수 있게 되었고, 따라서 초기화 선행자 패턴이 더 이상 필요하지 않게 되었습니다.

역사적 맥락을 덧붙이면, 초기화 선행자는 본래 Scala 언어 명세(Scala Language Specification) 5.1.6절에 문서화되어 있었습니다.

### 마이그레이션 경로

Scala 2 코드에서 초기화 선행자를 사용하던 개발자는 대신 트레이트 파라미터를 사용하도록 리팩터링해야 합니다. 트레이트 파라미터는 클래스 정의 지점에서 트레이트 동작을 파라미터화하는, 더 깔끔하고 직접적인 접근 방식을 제공합니다.

---

## 22 제한(Limit 22)

함수 타입(function types)의 최대 파라미터 개수에 대한 22 제한과 튜플 타입(tuple types)의 최대 필드 개수에 대한 22 제한이 삭제되었습니다.

**함수(Functions):** 함수는 이제 임의의 개수의 파라미터를 가질 수 있습니다. `scala.Function22`를 넘어서는 함수는 새로운 트레이트인 `scala.runtime.FunctionXXL`로 소거(erase)됩니다.

**튜플(Tuples):** 튜플 또한 임의의 개수의 필드를 가질 수 있습니다. `scala.Tuple22`를 넘어서는 튜플은 새로운 클래스인 `scala.runtime.TupleXXL`(트레이트 `scala.Product`를 확장함)로 소거됩니다. 나아가, 이들은 연결(concatenation)과 인덱싱(indexing) 같은 제네릭 연산(generic operations)을 지원합니다.

두 구현 모두 내부적으로 배열(arrays)을 메커니즘으로 사용합니다.

### 요약

Scala 3는 초과되는 파라미터와 필드를 위한 전용 런타임 타입(runtime types)을 도입하여 기존의 22-파라미터 제약을 없앱니다. 이를 통해 배열 기반 구현으로 호환성을 유지하면서도, 실용적으로 어떤 크기의 함수와 튜플이든 사용할 수 있게 되었습니다.

---

## 자동 적용(Auto-Application)

**개요**

Scala 3는 무항(nullary) 메서드를 인자 없이 호출할 때 빈 인자 목록 `()`을 자동으로 삽입하던 동작을 없앱니다. 이전에는 `next`가 자동으로 `next()`로 확장되었지만, Scala 3는 파라미터 구문과 일치하는 명시적 적용(application) 구문을 요구합니다.

**변경 내용**

이전:
```scala
def next(): T = ...
next     // 암묵적으로 next()로 확장됨
```

Scala 3에서는:
```scala
next
^
missing arguments for method next
```

**예외**

자바(Java) 메서드 안에서 정의되거나 자바 메서드를 오버라이드하는 메서드는 자동 `()` 삽입을 그대로 유지합니다. 이는 균등 접근 원칙(uniform access principle)을 보존하며, 다음과 같은 코드를

```scala
xs.toString().length()
```

다음과 같은 관용적인(idiomatic) Scala 코드로 작성할 수 있게 합니다.

```scala
xs.toString.length
```

하위 호환성을 위해, Scala 3는 Scala 2에서 정의되었거나 Scala 2 메서드를 오버라이드하는 무항 메서드에 대해서는 자동 삽입을 일시적으로 허용하여, 라이브러리 간의 불일치를 수용합니다.

**오버라이딩 규칙**

메서드 오버라이드(override)에 대해 이제 더 엄격한 적합성(conformance)이 적용됩니다. 파라미터 없는 메서드(parameterless method)는 무항 메서드(nullary method)로 오버라이드될 수 없으며, 그 반대도 마찬가지입니다. 둘은 정확히 일치해야 합니다.

```scala
class A:
  def next(): Int

class B extends A:
  def next: Int // error: incompatible type
```

자바 및 Scala 2 메서드 오버라이드는 여전히 이 규칙에서 면제됩니다.

**마이그레이션**

기존 코드는 `-source 3.0-migration` 옵션 하에서 컴파일됩니다. 이를 `-rewrite`와 함께 사용하면 코드가 Scala 3의 더 엄격한 검사를 준수하도록 자동으로 리팩터링됩니다.

---

## 비지역 반환(Nonlocal Returns)

> 참고: 이 기능은 삭제(dropped)가 아니라 더 이상 권장되지 않는(deprecated) 상태입니다.

중첩된 익명 함수(nested anonymous functions)로부터의 반환(return)은 Scala 3.2.0부터 더 이상 권장되지 않습니다(deprecated).

비지역 반환(nonlocal returns)은 `scala.runtime.NonLocalReturnException`을 던지고(throw) 잡는(catch) 방식으로 구현됩니다. 이는 프로그래머가 의도한 바와 일치하지 않는 경우가 많습니다. 예외(exception)를 던지고 잡는 데 드는 숨겨진 성능 비용(hidden performance cost) 때문에 문제가 될 수 있습니다. 또한 이는 누수가 있는(leaky) 구현입니다. 모든 예외를 잡는 핸들러(catch-all exception handler)가 `NonLocalReturnException`을 가로챌 수 있기 때문입니다.

비지역 반환과 `scala.util.control.Breaks` API에 대한 더 나은 대안으로 [`scala.util.boundary`와 `boundary.break`](https://nightly.scala-lang.org/api/scala/util/boundary$.html)가 제공됩니다.

예:

```scala
import scala.util.boundary, boundary.break
def firstIndex[T](xs: List[T], elem: T): Int =
  boundary:
    for (x, i) <- xs.zipWithIndex do
      if x == elem then break(i)
    -1
```

### 요약

이 문서 페이지는 비지역 반환이 Scala 3.2.0에서 더 이상 권장되지 않게 된 이유를 설명합니다. 이 기능은 예외 던지기 메커니즘에 의존했는데, 이는 숨겨진 성능 비용을 부과했고 모든 예외를 잡는 핸들러를 통해 취약성(vulnerability)을 만들어냈습니다. 권장되는 대체 수단은, 중첩된 문맥(nested contexts)에서 더 깔끔하고 효율적인 제어 흐름(control flow)을 위해 현대적인 `scala.util.boundary` API를 사용하는 것입니다.

---

## private[this]와 protected[this]

`private[this]`와 `protected[this]` 접근 한정자(access modifiers)는 더 이상 권장되지 않으며(deprecated), 단계적으로 폐지될 예정입니다.

이전에는 이 한정자들이 다음과 같은 용도로 필요했습니다.

- 게터(getter)와 세터(setter)의 생성을 피하기 위해
- `private[this]` 하위의 코드를 가변성 검사(variance checks)에서 제외하기 위해 (Scala 2는 `protected[this]`도 제외했으나, 이는 불건전(unsound)한 것으로 밝혀져 제거되었습니다)
- `private[this] val`이 클래스의 메서드에서 접근되지 않을 경우, 필드(field)의 생성을 피하기 위해

이제 컴파일러는 `private` 멤버에 대해, 그것이 오직 `this`를 통해서만 접근된다는 사실을 추론합니다. 그러한 멤버는 마치 `private[this]`로 선언된 것처럼 취급됩니다. `protected[this]`는 대체 수단 없이 삭제됩니다.

이 변경은 경우에 따라 Scala 프로그램의 의미(semantics)를 바꿀 수 있습니다. `private val`이 더 이상 필드를 생성한다고 보장되지 않기 때문입니다. 다음의 경우 필드가 생략됩니다.

- `val`이 오직 `this`를 통해서만 접근되고,
- `val`이 현재 클래스의 메서드로부터 접근되지 않는 경우

이는 프로그램이 누락된 private 필드를 리플렉션(reflection)을 통해 접근하려 할 때 문제를 일으킬 수 있습니다. 권장되는 해결책은, 해당 필드를 둘러싼 클래스를 한정자(qualifier)로 하는 한정 private(qualified private)으로 선언하는 것입니다. 예:

```scala
class C(x: Int):
  private[C] val field = x + 1
    // `field`를 리플렉션을 통해 접근하려면 [C]가 필요함
  val retained = field * field
```

클래스 파라미터(class parameters)는 일반적으로 객체-private(object-private)으로 추론되므로, 명시적으로 `val` 또는 `var`로 선언하여 도입한 멤버는 여기서 설명한 규칙에서 면제됩니다.

특히, 다음 필드는 가변성 검사에서 제외되지 않습니다.

```scala
class C[-T](private val t: T) // error
```

그리고 위에서 보인 private 필드와 대조적으로, 다음 필드는 제거되지 않습니다.

```scala
class C(private val c: Int)
```

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Dropped Features](https://docs.scala-lang.org/scala3/reference/dropped-features/)
