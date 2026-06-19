# Scala 3 컨텍스트 추상화: 확장 메서드와 타입 클래스

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "Contextual Abstractions" 섹션(확장 메서드/타입 클래스/파생/다중우주 동등성/컨텍스트 함수/암시적 변환)을 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/reference/contextual/

---

## 목차

1. [확장 메서드(Extension Methods)](#1-확장-메서드extension-methods)
   - 1.1. [개요](#11-개요)
   - 1.2. [확장 메서드의 변환(Translation)](#12-확장-메서드의-변환translation)
   - 1.3. [연산자 확장(Operator Extensions)](#13-연산자-확장operator-extensions)
   - 1.4. [제네릭 확장(Generic Extensions)](#14-제네릭-확장generic-extensions)
   - 1.5. [집합 확장(Collective Extensions)](#15-집합-확장collective-extensions)
   - 1.6. [확장 메서드 해석(Resolution)](#16-확장-메서드-해석resolution)
   - 1.7. [호출 해석 규칙](#17-호출-해석-규칙)
   - 1.8. [확장 내부에서의 참조](#18-확장-내부에서의-참조)
   - 1.9. [문법(Syntax)](#19-문법syntax)
2. [타입 클래스(Type Classes)](#2-타입-클래스type-classes)
   - 2.1. [개요](#21-개요)
   - 2.2. [세미그룹(Semigroup)과 모노이드(Monoid)](#22-세미그룹semigroup과-모노이드monoid)
   - 2.3. [펑터(Functor)](#23-펑터functor)
   - 2.4. [모나드(Monad)](#24-모나드monad)
   - 2.5. [요약](#25-요약)
3. [타입 클래스 파생(Type Class Derivation)](#3-타입-클래스-파생type-class-derivation)
   - 3.1. [개요](#31-개요)
   - 3.2. [정확한 동작 메커니즘](#32-정확한-동작-메커니즘)
   - 3.3. [Mirror 타입 클래스](#33-mirror-타입-클래스)
   - 3.4. [derived 메서드 구현](#34-derived-메서드-구현)
   - 3.5. [문법](#35-문법)
   - 3.6. [핵심 설계 원칙](#36-핵심-설계-원칙)
4. [다중우주 동등성(Multiversal Equality)](#4-다중우주-동등성multiversal-equality)
   - 4.1. [개요](#41-개요)
   - 4.2. [핵심 문제](#42-핵심-문제)
   - 4.3. [CanEqual 해법](#43-canequal-해법)
   - 4.4. [여러 CanEqual 인스턴스](#44-여러-canequal-인스턴스)
   - 4.5. [하위 호환성](#45-하위-호환성)
   - 4.6. [CanEqual 인스턴스 파생](#46-canequal-인스턴스-파생)
   - 4.7. [동등성 검사 규칙](#47-동등성-검사-규칙)
   - 4.8. [사전 정의된 인스턴스](#48-사전-정의된-인스턴스)
   - 4.9. [왜 타입 파라미터가 두 개인가](#49-왜-타입-파라미터가-두-개인가)
5. [컨텍스트 함수(Context Functions)](#5-컨텍스트-함수context-functions)
   - 5.1. [개요](#51-개요)
   - 5.2. [핵심 문법](#52-핵심-문법)
   - 5.3. [자동 변환](#53-자동-변환)
   - 5.4. [예제: 빌더 패턴](#54-예제-빌더-패턴)
   - 5.5. [예제: 사후 조건(Postconditions)](#55-예제-사후-조건postconditions)
   - 5.6. [핵심 구분점](#56-핵심-구분점)
6. [암시적 변환(Implicit Conversions)](#6-암시적-변환implicit-conversions)
   - 6.1. [핵심 개념](#61-핵심-개념)
   - 6.2. [정의 예제](#62-정의-예제)
   - 6.3. [변환이 적용되는 시점](#63-변환이-적용되는-시점)
   - 6.4. [실전 예제](#64-실전-예제)

---

## 1. 확장 메서드(Extension Methods)

### 1.1. 개요

확장 메서드(extension method)는 어떤 타입이 최초로 정의된 이후에 그 타입에 새로운 메서드를 추가할 수 있게 해 줍니다. 즉, 타입의 원본 소스 코드를 수정하지 않고도 메서드를 덧붙일 수 있습니다. 공식 문서의 기초 예제는 다음과 같습니다.

```scala
case class Circle(x: Double, y: Double, radius: Double)

extension (c: Circle)
  def circumference: Double = c.radius * math.Pi * 2
```

이렇게 정의한 메서드는 일반적인 점(`.`) 표기법으로 호출할 수 있습니다.

```scala
circle.circumference
```

### 1.2. 확장 메서드의 변환(Translation)

컴파일러는 확장 메서드를, 확장 대상 값(receiver)을 첫 번째 파라미터로 받는, 특별히 표시된(marked) 함수로 변환합니다. 위에서 정의한 `circumference`는 다음과 동등한 형태로 변환됩니다.

```scala
<extension> def circumference(c: Circle): Double = c.radius * math.Pi * 2
```

이는 곧 `circle.circumference`와 `circumference(circle)`가 동일한 결과를 산출함을 의미합니다.

### 1.3. 연산자 확장(Operator Extensions)

확장 메서드 문법은 연산자 정의도 지원합니다.

```scala
extension (x: String)
  def < (y: String): Boolean = ...
extension (x: Elem)
  def +: (xs: Seq[Elem]): Seq[Elem] = ...
extension (x: Number)
  infix def min (y: Number): Number = ...
```

`+:`와 같은 우결합(right-associative) 연산자는, 기대되는 의미(semantics)를 유지하기 위해 변환 과정에서 파라미터 순서가 재배치(reordering)됩니다.

### 1.4. 제네릭 확장(Generic Extensions)

확장에 타입 파라미터(type parameter)를 추가할 수 있습니다.

```scala
extension [T](xs: List[T])
  def second = xs.tail.head

extension [T: Numeric](x: T)
  def + (y: T): T = summon[Numeric[T]].plus(x, y)
```

메서드 수준의 타입 파라미터는 확장 수준의 타입 파라미터와 결합됩니다.

```scala
extension [T](xs: List[T])
  def sumBy[U: Numeric](f: T => U): U = ...
```

확장 파라미터에 대한 타입 인수(type argument)는 확장 문법이 아닌 일반 호출 문법을 사용해 지정해야 합니다.

```scala
sumBy[String](List("a", "bb", "ccc"))(_.length)
```

또한 확장은 컨텍스트 파라미터(contextual parameter)를 위한 `using` 절도 지원합니다.

### 1.5. 집합 확장(Collective Extensions)

동일한 리시버(receiver) 타입을 공유하는 여러 확장 메서드를 하나로 묶을 수 있습니다.

```scala
extension (ss: Seq[String])

  def longestStrings: Seq[String] =
    val maxLength = ss.map(_.length).max
    ss.filter(_.length == maxLength)

  def longestString: String = longestStrings.head
```

명시적으로 중괄호를 사용할 수도 있습니다.

```scala
extension (ss: Seq[String]) {

  def longestStrings: Seq[String] = {
    val maxLength = ss.map(_.length).max
    ss.filter(_.length == maxLength)
  }

  def longestString: String = longestStrings.head
}
```

집합 확장 내부에서 메서드들은 공유된 리시버를 통해 서로를 암시적으로 참조할 수 있습니다.

집합 확장은 타입 파라미터와 `using` 절도 지원합니다.

```scala
extension [T](xs: List[T])(using Ordering[T])
  def smallest(n: Int): List[T] = xs.sorted.take(n)
  def smallestIndices(n: Int): List[Int] =
    val limit = smallest(n).max
    xs.zipWithIndex.collect { case (x, i) if x <= limit => i }
```

### 1.6. 확장 메서드 해석(Resolution)

컴파일러는 다음 네 가지 메커니즘을 통해 확장 메서드가 적용 가능한지를 인식합니다.

1. **스코프 내에서 가시적(Visible in scope)** — 둘러싼 스코프 안에 정의되거나, 상속되거나, 임포트된 경우
2. **주어진 인스턴스의 멤버(Given instance members)** — 가시적인 주어진 인스턴스(given instance) 안에 포함된 경우
3. **리시버의 암시적 스코프(Implicit scope of receiver)** — 리시버 타입의 암시적 스코프(implicit scope)에 정의된 경우
4. **암시적 스코프 내 주어진 인스턴스(Given in implicit scope)** — 리시버의 암시적 스코프에 있는 주어진 인스턴스 안에 포함된 경우

스코프 기반 가용성을 보여 주는 예제입니다.

```scala
trait IntOps:
  extension (i: Int) def isZero: Boolean = i == 0

  extension (i: Int) def safeMod(x: Int): Option[Int] =
    if x.isZero then None
    else Some(i % x)

object IntOpsEx extends IntOps:
  extension (i: Int) def safeDiv(x: Int): Option[Int] =
    if x.isZero then None
    else Some(i / x)

trait SafeDiv:
  import IntOpsEx.*

  extension (i: Int) def divide(d: Int): Option[(Int, Int)] =
    (i.safeDiv(d), i.safeMod(d)) match
      case (Some(d), Some(r)) => Some((d, r))
      case _ => None
```

확장 메서드는 주어진 인스턴스를 통해 가용해집니다.

```scala
given ops1: IntOps()
1.safeMod(2)
```

암시적 스코프를 활용하는 예제입니다.

```scala
class List[T]:
  ...
object List:
  ...
  extension [T](xs: List[List[T]])
    def flatten: List[T] = xs.foldLeft(List.empty[T])(_ ++ _)

  given [T: Ordering] => Ordering[List[T]]:
    extension (xs: List[T])
      def < (ys: List[T]): Boolean = ...
end List

List(List(1, 2), List(3, 4)).flatten
List(1, 2) < List(3)
```

### 1.7. 호출 해석 규칙

컴파일러가 `m`이 직접적인 멤버가 아닌 선택 표현식 `e.m[Ts]`를 만나면, 두 가지 재작성(rewriting) 전략을 순서대로 시도합니다.

**첫 번째**로, 표준 이름 해석(name resolution)을 완화된 임포트 규칙과 함께 적용하여 `m[Ts](e)`로 재작성합니다. 모호한 임포트(ambiguous import)는, 오직 하나만 유효한 타입 검사를 통과할 때 비(非)와일드카드 임포트(non-wildcard import)를 우선함으로써 처리됩니다.

**두 번째**로, 첫 번째 접근이 실패하면, 적격한(eligible) 객체들(리시버 타입의 암시적 스코프에 있거나 가시적인 주어진 인스턴스인 객체) 안에서 확장 메서드 `m`을 탐색하고, 이에 따라 `o.m[Ts](e)`로 재작성합니다.

### 1.8. 확장 내부에서의 참조

확장 메서드가 같은 집합 확장 내의 다른 확장 메서드를 참조할 때, 리시버는 암시적으로 전달됩니다. 재귀 호출의 경우를 봅시다.

```scala
extension (s: String)
  def position(ch: Char, n: Int): Int =
    if n < s.length && s(n) != ch then position(ch, n + 1)
    else n
```

재귀 호출 `position(ch, n + 1)`는 `s.position(ch, n + 1)`로 확장되며, 메서드 전체는 다음과 같이 변환됩니다.

```scala
def position(s: String)(ch: Char, n: Int): Int =
  if n < s.length && s(n) != ch then position(s)(ch, n + 1)
  else n
```

### 1.9. 문법(Syntax)

확장 선언은 다음 패턴을 따릅니다.

```
Extension  ::=  'extension' [DefTypeParamClause] {UsingParamClause}
                '(' DefParam ')' {UsingParamClause} ExtMethods
ExtMethods ::=  ExtMethod | [nl] '{' ExtMethod {semi ExtMethod} '}' 
                | indent ExtMethod {semi ExtMethod} outdent
ExtMethod  ::=  {Annotation [nl]} {Modifier} 'def' DefDef
```

`extension` 키워드는 문맥 민감(context-sensitive)합니다. 즉, 문장(statement)을 시작하면서 그 뒤에 `[` 또는 `(`가 따라올 때에만 키워드로 인식됩니다.

---

## 2. 타입 클래스(Type Classes)

### 2.1. 개요

타입 클래스(type class)는 추상적이고 매개변수화된(parameterized) 타입으로, 닫힌(closed) 데이터 타입에 서브타이핑(subtyping) 없이 새로운 동작을 추가할 수 있게 해 줍니다. Scala 3에서 타입 클래스는 타입 파라미터를 가진 트레이트(trait)이며, 그 구현은 `extends` 키워드 대신 **주어진 인스턴스(given instance)**를 사용합니다.

### 2.2. 세미그룹(Semigroup)과 모노이드(Monoid)

기초적인 타입 클래스 정의는 다음과 같습니다.

```scala
trait SemiGroup[T]:
  extension (x: T) def combine (y: T): T

trait Monoid[T] extends SemiGroup[T]:
  def unit: T
```

`String`에 대한 구현입니다.

```scala
given Monoid[String]:
  extension (x: String) def combine (y: String): String = x.concat(y)
  def unit: String = ""
```

`Int`에 대한 구현입니다.

```scala
given Monoid[Int]:
  extension (x: Int) def combine (y: Int): Int = x + y
  def unit: Int = 0
```

컨텍스트 바운드(context bound)와 함께 사용하는 예입니다.

```scala
def combineAll[T: Monoid as m](xs: List[T]): T =
  xs.foldLeft(m.unit)(_.combine(_))
```

여기서 `[T: Monoid as m]`는 컨텍스트 바운드를 통해 도입된 `Monoid[T]` 인스턴스에 `m`이라는 이름을 부여하는 Scala 3의 문법입니다.

### 2.3. 펑터(Functor)

펑터(Functor)는 값을 "맵핑(map over)"하여, 구조를 보존하면서 변환할 수 있는 능력을 제공합니다. 정의에는 타입 생성자(type constructor) `F[_]`가 사용됩니다.

```scala
trait Functor[F[_]]:
  extension [A](x: F[A])
    def map[B](f: A => B): F[B]
```

`List` 인스턴스는 다음과 같습니다.

```scala
given Functor[List]:
  extension [A](xs: List[A])
    def map[B](f: A => B): List[B] =
      xs.map(f)
```

테스트 메서드 사용 예입니다.

```scala
def assertTransformation[F[_]: Functor, A, B](expected: F[B], original: F[A], mapping: A => B): Unit =
  assert(expected == original.map(mapping))

assertTransformation(List("a1", "b1"), List("a", "b"), elt => s"${elt}1")
```

### 2.4. 모나드(Monad)

모나드(Monad)는 펑터를 확장하여 `flatMap`과 `pure` 연산을 추가합니다.

```scala
trait Monad[F[_]] extends Functor[F]:

  def pure[A](x: A): F[A]

  extension [A](x: F[A])
    def flatMap[B](f: A => F[B]): F[B]
    def map[B](f: A => B) = x.flatMap(f.andThen(pure))

end Monad
```

#### List 구현

```scala
given listMonad: Monad[List]:
  def pure[A](x: A): List[A] =
    List(x)
  extension [A](xs: List[A])
    def flatMap[B](f: A => List[B]): List[B] =
      xs.flatMap(f)
```

#### Option 구현

```scala
given optionMonad: Monad[Option]:
  def pure[A](x: A): Option[A] =
    Option(x)
  extension [A](xo: Option[A])
    def flatMap[B](f: A => Option[B]): Option[B] = xo match
      case Some(x) => f(x)
      case None => None
```

#### Reader 모나드

Reader 모나드는 함수에 대해 동작하며, 동일한 파라미터(설정(configuration), 컨텍스트, 환경(environment))를 필요로 하는 함수들을 결합할 때 유용합니다.

```scala
type ConfigDependent[Result] = Config => Result

given configDependentMonad: Monad[ConfigDependent]:

  def pure[A](x: A): ConfigDependent[A] =
    config => x

  extension [A](x: ConfigDependent[A])
    def flatMap[B](f: A => ConfigDependent[B]): ConfigDependent[B] =
      config => f(x(config))(config)

end configDependentMonad
```

타입 람다(type lambda)를 사용한 버전입니다.

```scala
given configDependentMonad: Monad[[Result] =>> Config => Result]:

  def pure[A](x: A): Config => A =
    config => x

  extension [A](x: Config => A)
    def flatMap[B](f: A => Config => B): Config => B =
      config => f(x(config))(config)

end configDependentMonad
```

임의의 컨텍스트 타입에 대해 매개변수화된 Reader 모나드입니다.

```scala
given readerMonad: [Ctx] => Monad[[X] =>> Ctx => X]:

  def pure[A](x: A): Ctx => A =
    ctx => x

  extension [A](x: Ctx => A)
    def flatMap[B](f: A => Ctx => B): Ctx => B =
      ctx => f(x(ctx))(ctx)

end readerMonad
```

### 2.5. 요약

타입 클래스는 구현 전략 면에서 서브타입 다형성(subtype polymorphism)과 다릅니다. 전통적인 상속(inheritance)에서는 "어떤 클래스에 새로운 인터페이스를 추가하려면 그 클래스의 소스 코드를 변경해야" 하지만, "타입 클래스의 인스턴스는 어디에서나 정의할 수 있어" 더 우수한 확장성(extensibility)을 제공합니다. Scala 3는 트레이트, 주어진 인스턴스, 확장 메서드, 컨텍스트 바운드, 타입 람다를 결합하여 타입 클래스를 자연스럽게 표현합니다.

---

## 3. 타입 클래스 파생(Type Class Derivation)

### 3.1. 개요

타입 클래스 파생(type class derivation)은 특정 조건을 만족하는 타입 클래스에 대해 주어진 인스턴스를 자동으로 생성합니다. 문서에서 설명하듯이, 이 의미에서의 "타입 클래스는, 동작 대상이 되는 타입을 결정하는 단일 타입 파라미터를 가진 임의의 트레이트 또는 클래스"입니다.

예를 들어, `derives` 절(clause)을 가진 enum은 다음과 같습니다.

```scala
enum Tree[T] derives Eq, Ordering, Show:
  case Branch(left: Tree[T], right: Tree[T])
  case Leaf(elem: T)
```

이는 컴패니언 객체(companion object)에 다음 인스턴스들을 생성합니다.

```scala
given [T: Eq]       => Eq[Tree[T]]       = Eq.derived
given [T: Ordering] => Ordering[Tree[T]] = Ordering.derived
given [T: Show]     => Show[Tree[T]]     = Show.derived
```

### 3.2. 정확한 동작 메커니즘

이 프레임워크는 타입 클래스 `TC`가 타입 인수를 어떻게 받는지에 따라 경우를 구분합니다.

#### 경우 1: TC가 파라미터 하나를 받는 경우

모든 인수의 카인드(kind)가 `*`일 때, 생성되는 인스턴스는 다음을 따릅니다.

```scala
given [T_1: TC, ..., T_N: TC] => TC[DerivingType[T_1, ..., T_N]] = TC.derived
```

단순한 케이스 클래스(case class)의 예입니다.

```scala
case class Point(x: Int, y: Int) derives Ordering
```

이는 다음을 생성합니다.

```scala
object Point:
  given Ordering[Point] = Ordering.derived
```

#### 경우 2: CanEqual 타입 클래스

`CanEqual`을 파생할 때, 프레임워크는 양쪽 측면에 "DerivingType의 모든 파라미터를 두 번씩" 갖는 인스턴스를 생성하며, 컨텍스트 바운드는 카인드가 `*`인 파라미터에 대해서만 적용됩니다.

#### 경우 3: 고차(Higher-Kinded) 파라미터

`F`와 `DerivingType`이 일치하는 카인드 파라미터를 가질 때, 생성되는 인스턴스는 타입 람다를 사용합니다.

### 3.3. Mirror 타입 클래스

`Mirror` 기반 구조(infrastructure)는 ADT(대수적 데이터 타입, Algebraic Data Type) 구조에 대한 컴파일 시점(compile-time) 타입 정보를 제공합니다.

```scala
sealed trait Mirror:
  type MirroredType
  type MirroredElemTypes
  type MirroredMonoType
  type MirroredLabel <: String
  type MirroredElemLabels <: Tuple

object Mirror:
  trait Product extends Mirror:
    def fromProduct(p: scala.Product): MirroredMonoType
  
  trait Sum extends Mirror:
    def ordinal(x: MirroredMonoType): Int
```

컴파일러는 다음에 대해 자동으로 `Mirror` 인스턴스를 생성합니다.

- enum 및 enum 케이스
- 케이스 객체(case object)
- 가시적인 생성자(visible constructor)를 가진 케이스 클래스
- 도달 가능한(reachable) 자식 케이스를 가진 봉인된(sealed) 클래스/트레이트

위의 `Tree` 예제에 대해 컴파일러는 다음을 제공합니다.

```scala
// Tree에 대한 Mirror (Sum 타입)
new Mirror.Sum:
  type MirroredType = Tree
  type MirroredElemTypes[T] = (Branch[T], Leaf[T])
  type MirroredMonoType = Tree[_]
  type MirroredLabel = "Tree"
  type MirroredElemLabels = ("Branch", "Leaf")
  def ordinal(x: MirroredMonoType): Int = x match
    case _: Branch[_] => 0
    case _: Leaf[_] => 1

// Branch에 대한 Mirror (Product 타입)
new Mirror.Product:
  type MirroredType = Branch
  type MirroredElemTypes[T] = (Tree[T], Tree[T])
  type MirroredMonoType = Branch[_]
  type MirroredLabel = "Branch"
  type MirroredElemLabels = ("left", "right")
  def fromProduct(p: Product): MirroredMonoType = new Branch(...)
```

### 3.4. derived 메서드 구현

타입 클래스 작성자는 일반적으로 `Mirror` 파라미터를 받는 인라인(inline) 메서드로 `derived`를 구현합니다.

```scala
import scala.deriving.Mirror

inline def derived[T](using Mirror.Of[T]): TC[T] = ...
```

#### 완전한 Eq 예제

인라인 메서드와 패턴 매칭(pattern matching)을 사용하는 완전한 저수준(low-level) 구현입니다.

```scala
import scala.collection.AbstractIterable
import scala.compiletime.{erasedValue, error, summonInline}
import scala.deriving.*

inline def summonInstances[T, Elems <: Tuple]: List[Eq[?]] =
  inline erasedValue[Elems] match
    case _: (elem *: elems) => deriveOrSummon[T, elem] :: summonInstances[T, elems]
    case _: EmptyTuple => Nil

inline def deriveOrSummon[T, Elem]: Eq[Elem] =
  inline erasedValue[Elem] match
    case _: T => deriveRec[T, Elem]
    case _    => summonInline[Eq[Elem]]

inline def deriveRec[T, Elem]: Eq[Elem] =
  inline erasedValue[T] match
    case _: Elem => error("infinite recursive derivation")
    case _       => Eq.derived[Elem](using summonInline[Mirror.Of[Elem]])

trait Eq[T]:
  def eqv(x: T, y: T): Boolean

object Eq:
  given Eq[Int]:
    def eqv(x: Int, y: Int) = x == y

  def check(x: Any, y: Any, elem: Eq[?]): Boolean =
    elem.asInstanceOf[Eq[Any]].eqv(x, y)

  def iterable[T](p: T): Iterable[Any] = new AbstractIterable[Any]:
    def iterator: Iterator[Any] = p.asInstanceOf[Product].productIterator

  def eqSum[T](s: Mirror.SumOf[T], elems: => List[Eq[?]]): Eq[T] =
    new Eq[T]:
      def eqv(x: T, y: T): Boolean =
        val ordx = s.ordinal(x)
        (s.ordinal(y) == ordx) && check(x, y, elems(ordx))

  def eqProduct[T](p: Mirror.ProductOf[T], elems: => List[Eq[?]]): Eq[T] =
    new Eq[T]:
      def eqv(x: T, y: T): Boolean =
        iterable(x).lazyZip(iterable(y)).lazyZip(elems).forall(check)

  inline def derived[T](using m: Mirror.Of[T]): Eq[T] =
    lazy val elemInstances = summonInstances[T, m.MirroredElemTypes]
    inline m match
      case s: Mirror.SumOf[T]     => eqSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => eqProduct(p, elemInstances)
end Eq
```

재귀적 ADT로 테스트하는 예입니다.

```scala
enum Lst[+T] derives Eq:
  case Cns(t: T, ts: Lst[T])
  case Nl

@main def test(): Unit =
  import Lst.*
  val eqoi = summon[Eq[Lst[Int]]]
  assert(eqoi.eqv(23 :: 47 :: Nl, 23 :: 47 :: Nl))
  assert(!eqoi.eqv(23 :: Nl, 7 :: Nl))
```

### 3.5. 문법

```
InheritClauses    ::=  ['extends' ConstrApps] ['derives' QualId {',' QualId}]
```

이제 `extends` 절에서도 여러 타입을 쉼표로 구분할 수 있습니다.

```scala
class A extends B, C { ... }
```

### 3.6. 핵심 설계 원칙

이 프레임워크는 "미러링된 타입의 속성을 인코딩하는 타입 멤버(type member)"와 값 수준(value-level) 메커니즘이 함께 동작하는, 최소한의 저수준 설계를 우선합니다. 속성은 항(term)이 아닌 타입을 통해 인코딩되어 바이트코드(bytecode) 크기를 작게 유지합니다. `Mirror` 기반 구조는 기존의 `Product` 기반 구조를 확장하며, 일반적으로 컴패니언 객체에 의해 구현됩니다.

상위 수준 라이브러리(Shapeless 3, Magnolia 스타일 등)는 더 정교한 파생 패턴을 위해 이 기반 위에 구축될 수 있습니다.

---

## 4. 다중우주 동등성(Multiversal Equality)

### 4.1. 개요

Scala 3는 동등성 비교(equality comparison) 시 타입 안전성을 강화하기 위한 옵트인(opt-in) 메커니즘으로 다중우주 동등성(multiversal equality)을 도입합니다. 임의의 두 값을 비교할 수 있게 하는 일반 동등성(universal equality)과 달리, 이 기능은 `CanEqual`이라는 이항(binary) 타입 클래스를 사용해 비교를 제한합니다.

### 4.2. 핵심 문제

일반 동등성은 안전성 취약점을 만듭니다. 다음을 고려해 봅시다.

```scala
val x = ... // 타입 T
val y = ... // 타입 S 이지만, 사실 T 여야 함
x == y      // 타입 검사를 통과하며, 항상 false 를 반환함
```

타입이 일치하지 않더라도 비교가 성공하므로, 런타임에서야 드러나는 버그를 감출 수 있습니다.

### 4.3. CanEqual 해법

`CanEqual` 타입 클래스는 동등성을 의도된 타입 쌍으로만 제한합니다.

```scala
class T derives CanEqual
```

또는 명시적 인스턴스를 제공할 수도 있습니다.

```scala
given CanEqual[T, T] = CanEqual.derived
```

`CanEqual`의 정의는 다음과 같습니다.

```scala
@implicitNotFound("Values of types ${L} and ${R} cannot be compared with == or !=")
sealed trait CanEqual[-L, -R]

object CanEqual:
  object derived extends CanEqual[Any, Any]
```

### 4.4. 여러 CanEqual 인스턴스

선택적인 비교를 허용하기 위해 여러 인스턴스를 정의할 수 있습니다.

```scala
given CanEqual[A, A] = CanEqual.derived
given CanEqual[B, B] = CanEqual.derived
given CanEqual[A, B] = CanEqual.derived
given CanEqual[B, A] = CanEqual.derived
```

### 4.5. 하위 호환성

"폴백(fallback)" 인스턴스인 `canEqualAny`는 `strictEquality`가 활성화되지 않는 한 하위 호환성을 보존합니다.

```scala
def canEqualAny[L, R]: CanEqual[L, R] = CanEqual.derived
```

엄격 모드(strict mode)는 다음을 통해 활성화합니다.

```scala
import scala.language.strictEquality
```

또는 명령줄 플래그 `-language:strictEquality`를 사용합니다.

### 4.6. CanEqual 인스턴스 파생

타입 클래스 파생을 통해 인스턴스 생성을 단순화할 수 있습니다.

```scala
class Box[T](x: T) derives CanEqual
```

이는 다음을 생성합니다.

```scala
given [T, U] => CanEqual[T, U] => CanEqual[Box[T], Box[U]] =
  CanEqual.derived
```

동작을 보여 주는 예제입니다.

```scala
new Box(1) == new Box(1L)   // OK. CanEqual[Int, Long]이 존재함
new Box(1) == new Box("a")  // 오류: 비교할 수 없음
new Box(1) == 1             // 오류: 비교할 수 없음
```

### 4.7. 동등성 검사 규칙

`strictEquality`가 비활성화된 상태에서, 비교는 다음 경우에 성공합니다.

1. 타입이 정확히 일치하거나,
2. 한 타입이 다른 타입의 리프팅(lifting)된 버전의 서브타입이거나,
3. 두 타입 모두 반사적(reflexive) `CanEqual` 인스턴스를 갖지 않을 때

핵심 정의입니다.

- **리프팅(Lifting)**: 공변(covariant) 위치의 추상 타입을 상한(upper bound)으로 치환하는 것
- **반사적 인스턴스(Reflexive instance)**: `CanEqual[T, T]`에 대한 암시적 탐색(implicit search)이 성공하는 경우

### 4.8. 사전 정의된 인스턴스

`CanEqual` 객체는 다음 타입에 대해 인스턴스를 정의합니다.

- 원시 타입(`Byte`, `Short`, `Char`, `Int`, `Long`, `Float`, `Double`, `Boolean`, `Unit`)
- 자바 타입(`Number`, `Boolean`, `Character`)
- 컬렉션(`Seq`, `Set`)

사전 정의된 비교의 규칙입니다.

- 모든 원시 수치(numeric) 타입은 서로 비교 가능
- 원시 수치 타입은 `java.lang.Number`의 서브타입과 비교 가능
- `Boolean`은 `java.lang.Boolean`과 비교 가능
- `Char`는 `java.lang.Character`와 비교 가능
- 시퀀스는 원소 타입이 비교 가능하면 비교 가능
- 집합(Set)은 원소 타입이 비교 가능하면 비교 가능
- 임의의 `AnyRef` 서브타입은 `Null`과 비교 가능

### 4.9. 왜 타입 파라미터가 두 개인가

두 파라미터 설계는 변성(variance) 안전성을 갖춘 API 설계를 가능하게 합니다. 더 안전한 `contains` 메서드를 생각해 봅시다.

```scala
def contains[U >: T](x: U)(using CanEqual[T, U]): Boolean
```

이는 하위 호환성을 유지하면서 비교를 제한합니다. 단일 파라미터 설계라면 안전성 이점을 훼손하는 일반(universal) 인스턴스가 필요해져, 생태계의 점진적 마이그레이션이 불가능해집니다.

---

## 5. 컨텍스트 함수(Context Functions)

### 5.1. 개요

컨텍스트 함수(context function)는 (오직) 컨텍스트 파라미터(context parameter)만을 포함하는 함수로, `?=>` 화살표 문법으로 구분됩니다. 문서에서 명시하듯, "컨텍스트 함수는 (오직) 컨텍스트 파라미터를 갖는 함수이다. 그 타입을 _컨텍스트 함수 타입(context function type)_ 이라 한다."

### 5.2. 핵심 문법

컨텍스트 함수 타입은 다음과 같이 선언합니다.

```scala
type Executable[T] = ExecutionContext ?=> T
```

이 패턴을 사용하는 함수는 합성된(synthesized) 인수와 함께 호출됩니다.

```scala
def f(x: Int): ExecutionContext ?=> Int = ...

f(2)(using ec)   // 명시적 인수
f(2)             // 인수가 추론됨
```

### 5.3. 자동 변환

어떤 표현식의 기대 타입(expected type)이 컨텍스트 함수 타입 `(T_1, ..., T_n) ?=> U`이고 그 표현식이 아직 컨텍스트 함수 리터럴(literal)이 아닌 경우, 자동으로 다음으로 변환됩니다.

```scala
(x_1: T1, ..., x_n: Tn) ?=> E
```

합성된 파라미터들은 타입 검사 동안 주어진 인스턴스(given)로서 사용 가능해집니다.

### 5.4. 예제: 빌더 패턴

컨텍스트 함수는 컨텍스트 객체를 메서드 호출마다 명시적으로 엮어 전달하지 않고도, 중첩(nested) 구조를 구성하는 깔끔한 DSL 스타일 문법을 가능하게 합니다.

```scala
import scala.collection.mutable.ArrayBuffer

class Table:
  val rows = new ArrayBuffer[Row]
  def add(r: Row): Unit = rows += r
  override def toString = rows.mkString("Table(", ", ", ")")

class Row:
  val cells = new ArrayBuffer[Cell]
  def add(c: Cell): Unit = cells += c
  override def toString = cells.mkString("Row(", ", ", ")")

case class Cell(elem: String)

def table(init: Table ?=> Unit) =
  given t: Table = Table()
  init
  t

def row(init: Row ?=> Unit)(using t: Table) =
  given r: Row = Row()
  init
  t.add(r)

def cell(str: String)(using r: Row) =
  r.add(new Cell(str))
```

여기서 `table`, `row`, `cell`은 각각 컨텍스트 함수를 받아, 현재의 `Table` 또는 `Row`를 주어진 인스턴스로 제공합니다. 이를 통해 사용자는 `using` 인수를 명시적으로 넘기지 않고도 중첩된 빌더 블록을 작성할 수 있습니다.

### 5.5. 예제: 사후 조건(Postconditions)

불투명 타입 별칭(opaque type alias)을 컨텍스트 함수와 함께 사용하면, 단언(assertion) 검사를 위한 무비용(zero-overhead) 추상화를 만들 수 있습니다. 이때 결과 값을 조건식 안에서 박싱(boxing) 부담 없이 참조할 수 있습니다.

```scala
object PostConditions:
  opaque type WrappedResult[T] = T

  def result[T](using r: WrappedResult[T]): T = r

  extension [T](x: T)
    def ensuring(condition: WrappedResult[T] ?=> Boolean): T =
      assert(condition(using x))
      x
end PostConditions

import PostConditions.{ensuring, result}

val s = List(1, 2, 3).sum.ensuring(result == 6)
```

여기서 `ensuring`은 `WrappedResult[T] ?=> Boolean` 타입의 컨텍스트 함수를 조건으로 받습니다. 호출 시 `x`(즉, 현재 결과 값)가 주어진 인스턴스로 전달되며, 조건식 안에서 `result`를 통해 그 값을 참조할 수 있습니다. `WrappedResult`가 불투명 타입이므로 런타임에 별도의 래퍼(wrapper) 객체가 생성되지 않아 오버헤드가 없습니다.

### 5.6. 핵심 구분점

컨텍스트 함수 리터럴은 파라미터와 결과 사이에 `?=>`를 사용하며, 표준 함수 리터럴과는 달리 일반 함수 타입이 아닌 컨텍스트 함수 타입을 갖는다는 점에서 구분됩니다.

---

## 6. 암시적 변환(Implicit Conversions)

### 6.1. 핵심 개념

암시적 변환(implicit conversion)은 `scala.Conversion` 클래스의 주어진 인스턴스를 통해 구현됩니다. 클래스 시그니처는 다음과 같습니다.

```scala
abstract class Conversion[-T, +U] extends (T => U):
  def apply (x: T): U
```

이는 값을 타입 `T`에서 타입 `U`로 자동 변환하는 메커니즘을 확립합니다.

### 6.2. 정의 예제

`String`에서 `Token`으로의 변환은 다음과 같이 정의할 수 있습니다.

```scala
given Conversion[String, Token]:
  def apply(str: String): Token = new KeyWord(str)
```

또는 별칭(alias)을 사용해 더 간결하게 정의할 수 있습니다.

```scala
given Conversion[String, Token] = new KeyWord(_)
```

### 6.3. 변환이 적용되는 시점

컴파일러는 다음 세 가지 상황에서 암시적 변환을 자동으로 호출합니다.

1. 타입 `S`가 기대되는 위치에 타입 `T`의 표현식이 제공되지만, `T`가 `S`에 부합(conform)하지 않을 때
2. `e`가 타입 `T`인 선택 표현식 `e.m`에서, `T`에 멤버 `m`이 없을 때
3. `T`가 `m`을 정의하지만 제공된 인수와 일치하는 버전이 없는 적용 표현식 `e.m(args)`에서

적합한 `scala.Conversion` 인스턴스가 존재하면, 표현식 `e`는 `C.apply(e)`로 대체됩니다.

### 6.4. 실전 예제

**자동 박싱(auto-boxing) 변환:**

```scala
given int2Integer: Conversion[Int, java.lang.Integer] =
  java.lang.Integer.valueOf(_)
```

**enum 기반 라우팅을 활용한 마그넷 패턴(Magnet Pattern):**

마그넷 패턴은 여러 메서드 변형(variant)을, 특화된(specialized) 타입으로의 변환을 통해 하나로 통합할 수 있게 해 줍니다. 오버로딩(overloading)이 불가능하거나 과도한 중복을 만들어 낼 때 유용합니다.

```scala
object Completions:

  enum CompletionArg:
    case Error(s: String)
    case Response(f: Future[HttpResponse])
    case Status(code: Future[StatusCode])

  object CompletionArg:

    given fromString    : Conversion[String, CompletionArg]               = Error(_)
    given fromFuture    : Conversion[Future[HttpResponse], CompletionArg] = Response(_)
    given fromStatusCode: Conversion[Future[StatusCode], CompletionArg]   = Status(_)
  end CompletionArg
  import CompletionArg.*

  def complete[T](arg: CompletionArg) = arg match
    case Error(s) => ...
    case Response(f) => ...
    case Status(code) => ...

end Completions
```

이 패턴에서 `complete` 메서드는 단일 타입인 `CompletionArg`만을 받지만, `String`, `Future[HttpResponse]`, `Future[StatusCode]` 등 다양한 인수 타입을 암시적 변환을 통해 받아들일 수 있습니다.

> **참고:** Scala 3에서는 암시적 변환을 정의하는 권장 방식이 `scala.Conversion`의 주어진 인스턴스입니다. 구(舊) Scala 2 스타일의 `implicit def` 변환과 달리, 이 방식은 변환을 명시적인 타입으로 식별 가능하게 만듭니다. 변환 정의 시점에서 언어 기능 임포트 `import scala.language.implicitConversions`가 필요할 수 있으며, 이를 통해 암시적 변환 사용에 대한 경고를 제어합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/)
- [Extension Methods](https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html)
- [Type Classes](https://docs.scala-lang.org/scala3/reference/contextual/type-classes.html)
- [Type Class Derivation](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html)
- [Multiversal Equality](https://docs.scala-lang.org/scala3/reference/contextual/multiversal-equality.html)
- [Context Functions](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html)
- [Implicit Conversions](https://docs.scala-lang.org/scala3/reference/contextual/conversions.html)
