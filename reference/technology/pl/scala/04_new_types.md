# Scala 3 새로운 타입

---

## 목차

1. [개요](#개요)
2. [교집합 타입(Intersection Types)](#교집합-타입intersection-types)
3. [합집합 타입(Union Types)](#합집합-타입union-types)
4. [타입 람다(Type Lambdas)](#타입-람다type-lambdas)
5. [매치 타입(Match Types)](#매치-타입match-types)
6. [의존 함수 타입(Dependent Function Types)](#의존-함수-타입dependent-function-types)
7. [다형 함수 타입(Polymorphic Function Types)](#다형-함수-타입polymorphic-function-types)
8. [참고 자료](#참고-자료)

---

## 개요

Scala 3 레퍼런스의 이 절은 Scala 3에서 새롭게 사용할 수 있는 타입 구문(type construct)들을 소개합니다. 이 장에서는 언어의 표현력과 타입 검사(type-checking) 능력을 확장하는 여섯 가지 핵심적인 타입 시스템 개선 사항을 다룹니다.

1. **교집합 타입(Intersection Types)** — 여러 타입을 하나의 타입으로 결합하여, 모든 제약을 동시에 만족하는 값을 표현합니다.
2. **합집합 타입(Union Types)** — 지정된 여러 타입 중 어느 하나에 속하는 값을 표현하는 타입을 만듭니다.
3. **타입 람다(Type Lambdas)** — 일급(first-class) 타입 수준 함수(type-level function)와 고차 타입 추상화(higher-order type abstraction)를 가능하게 합니다.
4. **매치 타입(Match Types)** — 입력 타입에 따라 타입을 조건적으로 계산하는 타입 수준 패턴 매칭(type-level pattern matching)을 허용합니다.
5. **의존 함수 타입(Dependent Function Types)** — 반환 타입이 특정 파라미터 값에 의존할 수 있는 함수를 지원합니다.
6. **다형 함수 타입(Polymorphic Function Types)** — 여러 타입 파라미터에 걸쳐 다형성(polymorphism)을 유지하는 함수 타입을 가능하게 합니다.

> 📘 **처음 배우는 분께 — 이 문서 전체 지도**
>
> 위 여섯 가지는 크게 두 묶음으로 보면 편합니다. 앞쪽 둘(**교집합 `&`**, **합집합 `|`**)은 "타입들을 AND/OR로 조립"하는, 가장 실용적이고 자주 만나는 기능입니다. 뒤쪽 넷(**타입 람다, 매치 타입, 의존/다형 함수 타입**)은 "타입을 더 정교하게 다루는" 고급 도구로, 라이브러리를 만들 때 주로 쓰입니다. 처음이라면 앞쪽 둘만 확실히 잡아도 충분합니다.

> ⚠️ **짚고 넘어가기 — opaque type(불투명 타입)은 이 절에 없습니다**
>
> 00번 대응표에서 "value class → opaque type (04, 06)"을 보고 이 문서에서 opaque type을 찾는 분이 있을 텐데, 실제 공식 Reference의 "New Types" 절(=이 문서)에는 opaque type 설명이 들어 있지 않습니다(별도 절로 분리되어 있습니다). 헷갈리지 않도록 핵심만 짚어 둡니다. **opaque type**은 `Int` 같은 기존 타입을 `UserId`처럼 새 이름으로 한 겹 감싸되, 컴파일이 끝나면 그 포장이 사라져 **런타임 비용이 0**인 타입입니다. 예를 들어 `opaque type UserId = Int`로 두면 `UserId`와 `Int`를 실수로 섞어 쓰는 것을 컴파일러가 막아 주면서도, 실제로는 그냥 `Int`처럼 빠르게 동작합니다. 이것이 **Scala 2의 value class(`extends AnyVal`)가 하던 일을 대체**합니다(00번 문서 11번 참고). 구체적인 문법은 06번 문서에서 다룹니다.

---

## 교집합 타입(Intersection Types)

### 개요

Scala 3는 타입에 `&` 연산자를 사용하여 교집합 타입(intersection type)을 도입합니다. 타입 `S & T`는 "**동시에 타입 `S`이면서 타입 `T`인 값들을 표현**"합니다.

> 📘 **처음 배우는 분께 — 교집합 타입이란**
>
> "`S`이면서 동시에 `T`인 값"을 표현하는 타입입니다. 일상적으로 비유하면, "운전면허도 있고(Driver) 의사 자격증도 있는(Doctor) 사람" 같은 것입니다. `Driver & Doctor` 타입의 값은 운전 메서드도, 진료 메서드도 둘 다 호출할 수 있습니다.
>
> 집합으로 생각하면, `S & T`는 두 집합의 교집합(∩)에 들어가는 값들입니다. 그래서 `&` 기호를 씁니다.

> ⚠️ **짚고 넘어가기 — Scala 2의 `A with B`를 대체**
>
> Scala 2에서 쓰던 합성 타입 `A with B`가 Scala 3에서는 교집합 타입 `A & B`로 바뀌었습니다. 다만 둘은 완전히 같지 않습니다. `A with B`는 순서가 의미를 가졌지만(왼쪽/오른쪽에 따라 멤버 해소가 달라짐), `A & B`는 뒤에 나오는 "교환 법칙"처럼 순서가 무의미합니다. 그래서 `A & B`가 더 깔끔합니다.

교집합 타입은 (서로 다른 위치에 동일한 멤버를 갖는 여러 클래스를 혼합할 때 종종 발생하던) 컴파일 타임 클래스 합성(class composition)의 일부 제약을 대체합니다.

### 주요 특징

**멤버 합성(Member Composition)**: 교집합 타입은 두 구성 타입의 모든 멤버를 결합합니다. 예를 들어, `Resettable & Growable[String]` 타입을 갖는 파라미터는 두 트레잇 모두의 메서드에 접근할 수 있습니다.

다음 구조를 보십시오.

```scala
trait Resettable:
  def reset(): Unit

trait Growable[T]:
  def add(t: T): Unit

def f(x: Resettable & Growable[String]) =
  x.reset()
  x.add("first")
```

위 예제에서 함수 `f`의 파라미터 `x`는 `Resettable`과 `Growable[String]`의 교집합 타입입니다. 따라서 `x`에 대해 `Resettable`이 정의하는 `reset()`과 `Growable[String]`이 정의하는 `add(t: String)` 모두를 호출할 수 있습니다.

**교환성(Commutativity)**: 교집합 연산자는 교환 법칙(commutative)을 만족합니다. 즉, `A & B`는 `B & A`와 동일합니다.

**공유 멤버 해소(Shared Member Resolution)**: 두 타입이 동일한 멤버를 정의하는 경우, 컴파일러는 해당 멤버 타입들의 교집합을 사용합니다. `A`와 `B` 모두에 나타나는 `children` 메서드는 결과적으로 `List[A] & List[B]` 타입을 갖게 되며, 이는 공변성(covariance)으로 인해 `List[A & B]`로 단순화됩니다.

예를 들어 다음과 같이 두 트레잇이 같은 이름의 멤버를 가질 수 있습니다.

```scala
trait A:
  def children: List[A]

trait B:
  def children: List[B]

val x: A & B = new C
val ys: List[A & B] = x.children
```

여기서 `x.children`의 타입은 `List[A] & List[B]`인데, 이는 `List`의 공변성 때문에 `List[A & B]`로 정규화(normalize)됩니다.

### 구현 관련 참고 사항

컴파일러는 교집합 타입의 멤버에 대한 구현을 자동으로 생성하지 **않습니다**. 여러 타입을 확장(extends)하는 구체 클래스(concrete class)가 교집합의 타입 요구 사항에 맞는 구현을 명시적으로 제공해야 합니다. 값이 **구성(constructed)되는** 시점에서 상속받은 모든 멤버가 올바르게 정의되었음을 보장해야 합니다.

이 패턴은 전통적인 다중 상속(multiple inheritance)의 복잡성 없이 유연한 타입 합성을 가능하게 합니다.

---

## 합집합 타입(Union Types)

### 개요

합집합 타입(union type)은 Scala 3에서 교집합 타입과 쌍을 이루는(dual) 개념을 표현합니다. 합집합 타입 `A | B`는 타입 `A` 또는 타입 `B`에 속하는 모든 값을 포괄합니다.

> 📘 **처음 배우는 분께 — 합집합 타입이란**
>
> 교집합(`&`)이 "둘 다"였다면, 합집합(`|`)은 "둘 중 하나"입니다. `String | Int` 타입의 값은 String이거나 Int입니다. 함수의 인자로 "문자열을 받든지 숫자를 받든지 둘 다 받겠다"고 할 때 `def f(x: String | Int)`처럼 쓸 수 있습니다.
>
> 예전 같으면 이런 "둘 중 하나"를 표현하려면 `Either[String, Int]`로 감싸거나, 공통 부모 trait를 만들어 상속시켜야 했습니다. 합집합 타입은 그런 포장 없이, 기존 타입들을 손대지 않고 그 자리에서 "또는"으로 묶습니다.

### 기본 문법과 예제

기본 문법은 파이프(`|`) 연산자를 사용합니다.

```scala
type Hash = Int

def lookupName(name: String) = ???
def lookupPassword(hash: Hash) = ???

trait ID
case class UserName(name: String) extends ID
case class Password(hash: Hash) extends ID

def help(id: UserName | Password) =
  val user = id match
    case UserName(name) => lookupName(name)
    case Password(hash) => lookupPassword(hash)
  // ... 생략됨
```

위 예제에서 `help` 함수는 `UserName` 또는 `Password`를 받을 수 있는 합집합 타입 파라미터를 받습니다. 패턴 매칭을 통해 어느 쪽인지에 따라 분기 처리할 수 있습니다.

### 주요 속성

**교환성(Commutativity)**: 합집합 연산자는 교환 법칙을 만족합니다. 즉, `A | B`는 `B | A`와 동등합니다.

### 타입 추론 동작

컴파일러는 특정 조건에서만 합집합 타입을 부여(assign)합니다.

1. 코드에서 명시적인 합집합 타입이 제공된 경우
2. 공통 상위 타입(common supertype)이 투명(transparent)으로 선언된 경우

#### 표준 타입 확장(Standard Type Widening)

명시적인 합집합 타입 지정이 없으면, 컴파일러는 공통 상위 타입으로 확장(widen)합니다.

```scala
val password = Password(123)
val name = UserName("Eve")
if true then name else password
// 결과 타입: ID (UserName | Password가 아님)
```

> ⚠️ **짚고 넘어가기 — 합집합 타입은 "내가 적어줘야" 유지된다**
>
> 위 예제가 헷갈리기 쉬운 부분입니다. `if true then name else password`를 쓰면 컴파일러는 결과 타입을 `UserName | Password`로 두지 않고, 둘의 공통 부모인 `ID`로 **확장**(widen)해 버립니다. 즉, 합집합 타입은 기본적으로 추론되지 않습니다. 정밀한 합집합 타입이 필요하면 `val x: Password | UserName = ...`처럼 타입을 직접 적어주거나, 아래의 `transparent trait`를 써야 합니다. "왜 내 합집합 타입이 사라졌지?" 싶을 때 이 규칙을 떠올리세요.

정밀한 합집합 타입을 얻으려면 명시적인 타입 어노테이션이 필요합니다.

```scala
val either: Password | UserName = if true then name else password
// 결과 타입: UserName | Password
```

#### 투명 트레잇(Transparent Trait)의 영향

공통 부모가 `transparent`로 표시되면, 합집합 타입이 보존됩니다.

```scala
type Hash = Int

transparent trait ID
case class UserName(name: String) extends ID
case class Password(hash: Hash) extends ID

if true then UserName("Eve") else Password(123)
// 결과 타입: UserName | Password (확장되지 않음)
```

#### 암묵적 투명 클래스(Implicit Transparent Classes)

타입에 명시적인 부모가 없는 경우, `Object`가 투명한 것으로 취급되기 때문에 합집합 타입이 자동으로 추론됩니다.

```scala
case class UserName(name: String)
case class Password(hash: Hash)

if true then UserName("Eve") else Password(123)
// 결과 타입: UserName | Password
```

합집합 타입은 정밀한 타입 수준 구분을 가능하게 하면서도, 패턴 매칭과 조건 로직에서의 표현력을 유지합니다.

---

## 타입 람다(Type Lambdas)

### 개요

타입 람다(type lambda)는 별도의 타입 정의 없이도 고차 종류 타입(higher-kinded type)을 직접 표현하는 메커니즘을 제공합니다. 이를 통해 개발자는 코드 안에서 인라인(inline)으로 익명 타입 생성자(anonymous type constructor)를 만들 수 있습니다.

> 📘 **처음 배우는 분께 — 타입 람다란 "타입을 만드는 즉석 함수"**
>
> 값의 세계에 익명 함수(`x => x + 1`)가 있듯이, 타입의 세계에도 익명 함수가 있는데 그게 타입 람다입니다. `[X] =>> List[X]`는 "타입 `X`를 받으면 `List[X]`를 돌려주는" 타입 수준의 함수입니다. (00번 문서 7번의 고차 타입 `F[_]` 이야기의 연장입니다.)
>
> 값 함수의 화살표가 `=>`라면, 타입 람다의 화살표는 `=>>`(꺾쇠 두 개)라고 외우면 구분하기 쉽습니다.

> 💡 **왜 필요한가 — 일회용 타입에 이름을 붙이지 않으려고**
>
> 어떤 함수가 "타입 하나를 받는 타입"(예: `List`, `Option`)을 인자로 받는다고 합시다. 그런데 내가 넘기고 싶은 게 "키가 String으로 고정된 Map", 즉 `[V] =>> Map[String, V]`라면, 예전엔 이걸 위해 `type StringMap[V] = Map[String, V]` 같은 별칭을 따로 선언해야 했습니다. 타입 람다를 쓰면 그 자리에서 바로 `[V] =>> Map[String, V]`라고 적어 넘길 수 있어, 한 번 쓰고 버릴 타입에 굳이 이름을 붙이지 않아도 됩니다.

### 문법과 정의

타입 람다의 기본 문법은 다음 패턴을 따릅니다.

```scala
[X, Y] =>> Map[Y, X]
```

이 예제는 두 개의 타입 파라미터 `X`와 `Y`를 받아 `Map[Y, X]`를 생성하는 이항(binary) 타입 생성자를 보여줍니다. 문법은 대괄호(`[ ]`)를 사용하여 파라미터를 선언하고, 그 뒤에 `=>>`를 두고, 마지막에 결과 타입 표현식을 둡니다.

### 파라미터 제약

타입 람다 내부의 타입 파라미터는 경계(bound)를 지원하지만 다음과 같은 제약이 따릅니다.

- 타입 파라미터에는 경계(bound)를 적용할 수 있습니다.
- 타입 파라미터에는 가변성 어노테이션(variance annotation, `+` 또는 `-`)을 붙일 수 **없습니다**.

이 제약은 타입 람다를 (특정 맥락에서 가변성 어노테이션을 허용할 수 있는) 완전한 타입 별칭(type alias) 정의와 구분 짓는 특징입니다.

### 목적과 사용 사례

타입 람다는 함수형 프로그래밍 패턴과 고차 타입 연산(higher-order type operation)을 용이하게 합니다. 이를 통해 개발자는 중간 타입 별칭을 만들지 않고도 타입 생성자를 인자로 전달할 수 있으며, 이는 상용구(boilerplate)를 줄이고 코드의 표현력을 향상시킵니다.

타입 람다의 의미론(semantics)과 구현에 대한 더 깊은 이해가 필요하다면 명세 문서(specification document)를 참고하십시오.

---

## 매치 타입(Match Types)

### 정의와 기본 문법

> 📘 **처음 배우는 분께 — 매치 타입이란 "타입을 보고 타입을 고르는 if문"**
>
> 00번 문서 3번에서 본 `match`(값을 보고 분기하는 패턴 매칭)를, 값이 아니라 **타입**에 대해 하는 것입니다. 예를 들어 "`String`이 들어오면 그 원소 타입은 `Char`, `Array[Int]`가 들어오면 `Int`"처럼, **타입을 입력받아 다른 타입을 결과로 계산**합니다. 일반 `match`가 런타임에 값을 분기한다면, 매치 타입은 컴파일 타임에 타입을 분기한다고 생각하면 됩니다.

> 💡 **왜 필요한가 — 컨테이너에서 "알맹이 타입"을 꺼낼 때**
>
> "어떤 컬렉션이든 그 안에 든 원소의 타입을 알아내고 싶다"는 상황이 대표적입니다. `List[Float]`를 주면 `Float`, `Array[Int]`를 주면 `Int`를 얻고 싶은 것이죠. 매치 타입을 쓰면 `Elem[List[Float]]`가 `Float`로 자동 계산되어, 컨테이너 종류와 무관하게 원소 타입을 안전하게 다룰 수 있습니다.

매치 타입(match type)은 타입 수준 패턴 매칭(type-level pattern matching)을 가능하게 하여, 스크루티니(scrutinee, 검사 대상) 타입 분석에 기반해 여러 우변(right-hand side) 중 하나로 환원(reduce)되는 타입을 만듭니다. 기본 구조는 다음과 같습니다.

```scala
S match { P1 => T1 ... Pn => Tn }
```

여기서 `S`는 스크루티니 타입을, `P1...Pn`은 타입 패턴(type pattern)을, `T1...Tn`은 결과 타입을 나타냅니다.

> ⚠️ **짚고 넘어가기 — "스크루티니"와 "환원"이라는 말**
>
> 낯선 번역어 두 개만 짚고 갑니다. **스크루티니**(scrutinee)는 "검사 대상", 즉 `match` 앞에 놓인 입력 타입(`S`)을 가리키는 전문 용어입니다. **환원**(reduce)은 "이 매치 타입이 실제로 어떤 타입으로 결정되는가"를 계산해 내는 것을 말합니다. `Elem[String]`이 `Char`로 "환원된다"는 건, 컴파일러가 패턴을 따져 결국 `Char`로 확정한다는 뜻입니다.

### 핵심 예제

```scala
type Elem[X] = X match
  case String => Char
  case Array[t] => t
  case Iterable[t] => t
```

이 매치 타입은 다음과 같은 환원(reduction)을 만들어냅니다.

- `Elem[String] =:= Char`
- `Elem[Array[Int]] =:= Int`
- `Elem[List[Float]] =:= Float`

즉, `Elem[X]`는 `X`가 어떤 타입인지에 따라 그 원소(element) 타입으로 환원됩니다.

### 재귀적 정의(Recursive Definitions)

매치 타입은 선택적 상위 경계(upper bound)와 함께 재귀(recursion)를 지원합니다.

```scala
type LeafElem[X] = X match
  case String => Char
  case Array[t] => LeafElem[t]
  case Iterable[t] => LeafElem[t]
  case AnyVal => X
```

위 `LeafElem`은 중첩된 컨테이너를 재귀적으로 파고들어 최종적으로 가장 안쪽의 원소 타입(leaf element type)으로 환원됩니다.

튜플(Tuple)에 대한 재귀적 매치 타입의 또 다른 예는 다음과 같습니다.

```scala
type Concat[Xs <: Tuple, +Ys <: Tuple] <: Tuple = Xs match
  case EmptyTuple => Ys
  case x *: xs => x *: Concat[xs, Ys]
```

여기서 `Concat`은 두 튜플 타입을 연결(concatenate)하는 매치 타입이며, `<: Tuple`이라는 명시적 상위 경계를 가집니다. 재귀 매치 타입에서는 이렇게 상위 경계를 선언하는 것이 중요한데, 이는 컴파일러가 환원되지 않은 매치 타입에 대해서도 타입 검사를 진행할 수 있도록 해 줍니다.

### 의존 타이핑(Dependent Typing)

매치 타입은 의존적으로 타입이 결정되는(dependently-typed) 메서드를 가능하게 합니다. 메서드가 매치 타입에 의해 특수하게 타입이 부여(typing)되기 위한 조건은 다음과 같습니다.

- 패턴에 가드(guard)가 없어야 합니다.
- 매치 표현식의 스크루티니 타입이 매치 타입의 스크루티니 타입의 부분집합(⊆)이어야 합니다.
- 두 구조(매치 표현식과 매치 타입) 사이의 케이스(case) 개수가 동일해야 합니다.
- 모든 패턴이 대응하는 타입 패턴과 매칭되는 타입 패턴(Typed Pattern)을 사용해야 합니다.

구현 예제는 다음과 같습니다.

```scala
def leafElem[X](x: X): LeafElem[X] = x match
  case x: String      => x.charAt(0)
  case x: Array[t]    => leafElem(x(0))
  case x: Iterable[t] => leafElem(x.head)
  case x: AnyVal      => x
```

위 `leafElem` 메서드의 반환 타입은 매치 타입 `LeafElem[X]`이며, 메서드 본문의 각 매치 케이스가 매치 타입의 각 케이스와 정밀하게 대응하므로 컴파일러가 의존적 타이핑을 수행할 수 있습니다.

### 매치 타입의 내부 표현(Internal Representation)

매치 타입은 내부적으로 `Match(S, C1, ..., Cn) <: B`의 형태로 표현됩니다. 여기서 각 케이스 `Ci`는 다음 중 하나입니다.

- 함수 타입 `P => T` (타입 변수가 바인딩되지 않는 경우), 또는
- 타입 람다 `[Xs] =>> P => T` (타입 변수가 바인딩되는 경우)

`B`는 매치 타입의 상위 경계(upper bound)입니다.

### 환원 알고리즘(Reduction Algorithm)

컴파일러는 다음 순서로 매치 타입을 환원합니다.

1. **빈 스크루티니 검사(Empty scrutinee check)**: 스크루티니가 빈 값 집합(empty value set)을 나타내는 경우(예: `Nothing`, `String & Int`) 환원하지 않습니다.

2. **순차적 패턴 처리(Sequential pattern processing)**: 각 패턴 `Pi`에 대해
   - `S <: Pi`이면 `Ti`로 환원합니다.
   - 그렇지 않으면 `S`와 `Pi` 사이의 서로소성(disjointness)을 증명하려 시도합니다.
   - 서로소(disjoint)임이 증명되면 다음 패턴으로 진행합니다. 그렇지 않으면 환원되지 않은 상태로 남습니다.

서로소성 증명(disjointness proof)은 다음과 같은 근거를 활용합니다.

- 단일 클래스 상속(single class inheritance)
- final 클래스의 불변성(immutability)
- 서로 다른 상수 타입(constant type)의 분리
- 싱글톤 경로(singleton path)의 유일성(uniqueness)

타입 파라미터의 인스턴스화(instantiation)는 **최소**(minimal)로 유지됩니다. 즉, 공변(covariant) 위치에 나타나는 변수는 최대한 축소(shrink)되고, 반공변(contravariant) 위치에 나타나는 변수는 최대한 확장(expand)됩니다.

### 와일드카드 매치 타입의 환원(Reducing Wildcard Match Types)

스크루티니가 와일드카드(예: `Array[?]`)와 같이 추상적인 부분을 포함하는 경우, 컴파일러는 패턴 매칭에서 와일드카드를 적절히 다루어 환원을 시도합니다. 패턴 안의 타입 변수(예: `Array[t]`의 `t`)는 스크루티니에서 추론된 타입으로 인스턴스화되며, 인스턴스화 정책은 위에서 설명한 가변성 기반의 최소 인스턴스화 규칙을 따릅니다.

### 서브타이핑 규칙(Subtyping Rules)

**구조적 비교(Structural comparison)**: 두 매치 타입은 다음 조건을 만족할 때 서브타입 관계를 유지합니다.

- 스크루티니가 동등(equivalent)하고,
- 패턴 개수가 정렬되며(상위 타입 측의 패턴 개수 ≤ 하위 타입 측의 패턴 개수),
- 대응하는 패턴들이 매칭되고,
- 본문(body)들이 서브타입 제약을 만족할 때

**환원 동등성(Reduction equivalence)**: 환원 가능한(reducible) 매치 타입은 자신의 환원 결과(redux)와 상호 서브타입(mutual subtype) 관계를 유지합니다.

**경계 부합(Bound conformance)**: 매치 타입은 선언된 상위 경계(upper bound)에 부합합니다.

### 종료성과 순환 탐지(Termination and Cycle Detection)

컴파일러는 서브타입 테스트 도중 스택 오버플로(stack overflow)를 포착하여, 순환 추적(cycle trace)과 함께 컴파일 타임 에러로 변환합니다.

```scala
type L[X] = X match
  case Int => L[X]  // 재귀 한도 초과(recursion limit exceeded) 에러를 발생시킴
```

위 예제처럼 자기 자신으로 무한히 환원되는 매치 타입은 종료되지 않으므로, 컴파일러가 이를 감지하여 에러를 보고합니다.

### 패턴의 가변성 법칙(Variance Laws for Patterns)

모든 매치 타입의 위치(스크루티니, 패턴, 본문)는 불변(invariant) 상태를 유지합니다. 즉, 매치 타입의 구성 요소에는 공변/반공변 관계가 자유롭게 허용되지 않습니다.

### 겹치는 패턴(Overlapping Patterns)

매치 타입의 패턴들은 위에서 아래로 순차적으로 검사됩니다. 따라서 더 구체적인(specific) 패턴을 먼저 두고, 더 일반적인(general) 패턴을 나중에 두어야 의도한 대로 환원됩니다. 컴파일러는 `S <: Pi`를 만족하는 첫 번째 패턴으로 환원하므로, 패턴 순서가 결과에 영향을 미칩니다.

### 다른 시스템과의 비교

매치 타입은 다른 언어의 유사 기능들과 다음과 같은 차이가 있습니다.

- **Haskell의 닫힌 타입 패밀리(closed type families)와의 차이**: 매치 타입은 등식(equality)이 아니라 서브타이핑(subtyping)을 통해 동작하며, 제약을 더 좁히는(constraint tightening) 방식을 취하지 않습니다.
- **TypeScript의 조건부 타입(conditional types)과의 차이**: 매치 타입은 비기저(non-ground) 타입을 지원하고, 직접적인 재귀(direct recursion)를 지원하며, 합집합 분배(union distribution) 의미론을 갖지 않습니다.

### 에러(Errors)

매치 타입이 환원되지 못하거나, 종료되지 않거나, 의존적 타이핑 조건을 만족하지 못하는 경우 컴파일러는 적절한 컴파일 타임 에러를 보고합니다. 특히 재귀 한도를 초과하면 순환 추적과 함께 에러가 발생합니다.

---

## 의존 함수 타입(Dependent Function Types)

### 개요

의존 함수 타입(dependent function type)은 결과 타입이 함수의 파라미터에 의존하는 함수 값(function value)을 가능하게 합니다. 이는 Scala가 기존에 가지고 있던 의존 메서드(dependent method) 기능을 기반으로 확장한 것입니다.

> 📘 **처음 배우는 분께 — 의존 함수 타입이란 "반환 타입이 넘긴 값에 따라 달라지는 함수"**
>
> 보통 함수는 반환 타입이 고정돼 있습니다(`Int`를 받아 `String`을 반환, 처럼). 의존 함수는 **반환 타입이 "어떤 값을 넘겼느냐"에 따라 달라집니다.** 예제의 `Entry`는 각자 자기만의 `Key` 타입을 품고 있어서, `extractKey(e)`의 반환 타입은 넘긴 `e`가 무엇이냐에 따라 `e.Key`로 결정됩니다. 즉 타입이 특정 값(`e`)에 "의존(dependent)"하는 것입니다.

### 핵심 개념

"**의존 함수 타입이란 결과가 함수의 파라미터에 의존하는 함수 타입**"입니다. 이전에는 Scala가 의존 메서드(dependent method)는 지원했지만, 이를 함수 값(function value)으로 표현하는 문법이 없었습니다. Scala 3는 이 간극을 메웁니다.

> 💡 **왜 필요한가 — 메서드만 되던 걸 "값"으로도 다루려고**
>
> 핵심은 "메서드(`def`)"와 "함수 값"의 차이입니다. 의존적인 반환 타입은 `def extractKey(e: Entry): e.Key`처럼 메서드로는 예전부터 쓸 수 있었지만, 이걸 `val extractor = ...`처럼 변수에 담거나 다른 함수에 인자로 넘길 수는 없었습니다. Scala 3의 의존 함수 타입은 이 메서드를 그대로 함수 값으로 포착해, 변수에 담고 여기저기 전달할 수 있게 해 줍니다.

### 예제

레퍼런스는 다음 예제를 제시합니다.

```scala
trait Entry { type Key; val key: Key }

def extractKey(e: Entry): e.Key = e.key          // 의존 메서드(dependent method)

val extractor: (e: Entry) => e.Key = extractKey  // 의존 함수 값(dependent function value)
```

여기서 메서드 `extractKey`는 자신의 파라미터를 참조하는 결과 타입(`e.Key`)을 가집니다. 문법 `(e: Entry) => e.Key`는 `Entry` 인자를 받아 `e.Key` 타입의 값을 반환하는 함수를 기술합니다. 즉, 반환 타입이 인자 `e`의 경로 의존 타입(path-dependent type)인 `e.Key`로 결정됩니다.

### 내부 표현(Internal Representation)

의존 함수는 리파인먼트(refinement) 문법을 사용합니다. 타입 `(e: Entry) => e.Key`는 다음과 같이 디슈가링(desugaring)됩니다.

```scala
Function1[Entry, Entry#Key]:
  def apply(e: Entry): e.Key
```

이는 의존 함수가, `apply` 메서드의 시그니처가 파라미터에 어떻게 의존하는지를 명시하는 추가 리파인먼트를 갖는 표준 `Function` 트레잇(예: `Function1`)의 인스턴스임을 보여줍니다. 다시 말해, 의존 함수 타입은 기존 함수 트레잇 위에 정제된(refined) `apply` 시그니처를 얹은 것입니다.

### 의의(Significance)

이 기능은 Scala 타입 시스템의 한 가지 간극을 메웁니다. 즉, "**결과 타입이 일부 파라미터를 참조하는 메서드**"를, 변수에 할당하고 인자로 전달할 수 있는 함수 값(function value)으로 포착(capture)할 수 있게 해 줍니다.

---

## 다형 함수 타입(Polymorphic Function Types)

### 개요

다형 함수 타입(polymorphic function type)은 함수가 타입 파라미터(type parameter)를 받을 수 있게 하여, 다형 메서드(polymorphic method)와 일급 함수 값(first-class function value) 사이의 간극을 메웁니다. "**다형 함수 타입이란 타입 파라미터를 받는 함수 타입**"입니다.

> 📘 **처음 배우는 분께 — 다형 함수 타입이란 "타입 파라미터까지 가진 함수 값"**
>
> 제네릭 메서드는 익숙합니다. `def first[A](xs: List[A]): A`처럼 `[A]`를 받는 메서드 말이죠. 그런데 이걸 메서드가 아니라 **변수에 담는 함수 값**으로 만들려고 하면, 예전엔 `[A]`를 함수 값에 붙일 방법이 없었습니다. 다형 함수 타입은 `[A] => List[A] => List[A]`처럼, 함수 값 자체가 타입 파라미터 `[A]`를 받게 해 줍니다. 한마디로 "제네릭한 람다"입니다.

### 문법과 기본 개념

다형 함수 타입의 문법은 타입 파라미터를 나타내기 위해 대괄호(`[ ]`)를 사용합니다.

```scala
val bar: [A] => List[A] => List[A] =
  [A] => (xs: List[A]) => foo[A](xs)
```

이는 다음과 같은 함수를 나타냅니다.

1. 타입 파라미터 `A`를 받고,
2. `List[A]` 인자를 받으며,
3. `List[A]`를 반환합니다.

이전에는 Scala가 다형 메서드(polymorphic method)는 허용했지만, 이를 함수 값(function value)에 직접 할당할 수 없었습니다. 이 기능이 그 제약을 해소합니다.

### 실용 예제: 표현식 매핑(Expression Mapping)

설득력 있는 사용 사례 중 하나는, 연산이 여러 타입에 걸쳐 동작해야 하는 강타입(strongly-typed) 언어 표현식(language expression)입니다. 다음과 같이 표현식을 나타내는 열거형(enum)을 생각해 봅시다.

```scala
enum Expr[A]:
  case Var(name: String)
  case Apply[A, B](fun: Expr[B => A], arg: Expr[B]) extends Expr[A]
```

`mapSubexpressions` 함수는 다형 함수 타입이 실제로 동작하는 모습을 보여줍니다.

```scala
def mapSubexpressions[A](e: Expr[A])(f: [B] => Expr[B] => Expr[B]): Expr[A] =
  e match
    case Apply(fun, arg) => Apply(f(fun), f(arg))
    case Var(n) => Var(n)
```

> 💡 **왜 필요한가 — "타입마다 다른 인자"를 하나의 함수로 처리하려고**
>
> 위 예제에서 `mapSubexpressions`는 하위 표현식들에 함수 `f`를 적용하는데, 그 하위 표현식들은 서로 타입이 제각각입니다(`Expr[B => A]`, `Expr[B]` 등). 만약 `f`가 평범하게 `Expr[Int] => Expr[Int]`로 타입이 고정돼 있으면, 다른 타입의 하위 표현식에는 적용할 수 없습니다. `f`를 다형 함수(`[B] => Expr[B] => Expr[B]`)로 만들어 넘기면, 호출하는 쪽에서 매번 적절한 타입 `B`로 알아서 맞춰 쓰므로, 타입이 다른 여러 하위 표현식을 단 하나의 `f`로 안전하게 처리할 수 있습니다.

파라미터 `f`는 다형성(polymorphism)이 필요합니다. 하위 표현식(subexpression)들이 서로 다른 타입을 갖기 때문에, 각각의 구체적인 타입과 무관하게 모두 균일하게(uniformly) 처리되어야 하기 때문입니다. 위 예제에서 `f(fun)`과 `f(arg)`는 서로 다른 타입 인자로 `f`를 적용하지만, `f`가 다형 함수이므로 두 호출 모두 타입 안전하게 동작합니다.

### 사용 예제

하위 표현식을 감싸는(wrapping) 작업은 그 실용적 이점을 잘 보여줍니다.

```scala
val e0 = Apply(Var("f"), Var("a"))
val e1 = mapSubexpressions(e0)(
  [B] => (se: Expr[B]) => Apply(Var[B => B]("wrap"), se))
```

이 코드는 모든 직접 하위 표현식(immediate subexpression)에 래퍼(wrapper) 함수를 적용하면서, 각 표현식의 개별 타입을 보존합니다.

### 타입 람다(Type Lambdas)와의 구분

다형 함수 타입은 타입 람다(type lambda)와 근본적으로 다릅니다. "**타입 람다는 타입(types) 안에서 적용되는 반면, 다형 함수는 항(terms) 안에서 적용**"됩니다.

> ⚠️ **짚고 넘어가기 — 타입 람다(`=>>`)와 다형 함수(`=>`)를 헷갈리지 말 것**
>
> 둘 다 `[A]`로 시작해서 비슷해 보이지만, 사는 세계가 다릅니다. **타입 람다** `[X] =>> List[X]`는 타입을 받아 타입을 만드는, "타입의 세계"에 사는 도구입니다(화살표 `=>>`). **다형 함수** `[A] => (xs: List[A]) => ...`는 실제로 값을 받아 값을 돌려주는, "값(항, term)의 세계"에 사는 함수입니다(화살표 `=>`). 여기서 "항(term)"이란 타입과 대비되는 말로, 실제로 실행되는 값·식을 가리킵니다. 타입 람다는 타입 표현식 안에서 타입 수준(type level)으로 동작하는 반면, 다형 함수는 메서드 본문 안에서 런타임(runtime)에 호출되며 `bar[Int]`처럼 명시적인 타입 인자(type argument)를 받습니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [New Types](https://docs.scala-lang.org/scala3/reference/new-types/)
