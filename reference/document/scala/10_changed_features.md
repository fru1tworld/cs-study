# Scala 3 변경된 기능

---

## 목차

1. [개요](#개요)
2. [타입에서의 와일드카드 인수(Wildcard Arguments in Types)](#타입에서의-와일드카드-인수wildcard-arguments-in-types)
3. [임포트(Imports)](#임포트imports)
4. [암시적 변환(Implicit Conversions)](#암시적-변환implicit-conversions)
5. [암시적 해소의 변경 사항(Changes in Implicit Resolution)](#암시적-해소의-변경-사항changes-in-implicit-resolution)
6. [타입 추론의 변경 사항(Changes in Type Inference)](#타입-추론의-변경-사항changes-in-type-inference)
7. [프로그래밍적 구조적 타입(Programmatic Structural Types)](#프로그래밍적-구조적-타입programmatic-structural-types)
8. [연산자 규칙(Rules for Operators)](#연산자-규칙rules-for-operators)
9. [매치 표현식 문법(Match Expressions)](#매치-표현식-문법match-expressions)
10. [Option 없는 패턴 매칭(Option-less Pattern Matching)](#option-없는-패턴-매칭option-less-pattern-matching)
11. [메인 함수(Main Methods)](#메인-함수main-methods)
12. [자동 에타 확장(Automatic Eta Expansion)](#자동-에타-확장automatic-eta-expansion)
13. [참고 자료](#참고-자료)

---

## 개요

이 섹션은 Scala 2 대비 Scala 3에서 **변경된 기능(changed features)**을 다룹니다. 완전히 새로 도입된 기능과 달리, 여기에 정리된 항목들은 Scala 2에도 존재했지만 Scala 3에서 문법(syntax)이나 의미(semantics)가 바뀐 기능들입니다.

> 📘 **처음 배우는 분께**
> 각 항목마다 "Scala 2에서는 이랬다 → Scala 3에서는 이렇게 바뀌었다"를 먼저 정리한 콜아웃을 붙여 두었으니,
> 그 부분을 먼저 읽고 본문으로 들어가면 수월합니다. 모든 항목을 처음부터 외울 필요는 없고,
> 나중에 옛 Scala 코드를 만났을 때 돌아보는 용도로 활용하면 됩니다.

전체 "Other Changed Features" 섹션은 다음과 같은 주제를 포함합니다.

- 수치 리터럴(Numeric Literals) — 수치 값을 표현하는 방식의 변경
- 프로그래밍적 구조적 타입(Programmatic Structural Types) — 구조적 타입 처리 방식의 변경
- 연산자 규칙(Rules for Operators) — 연산자 문법과 동작 규칙의 갱신
- 타입에서의 와일드카드 인수(Wildcard Arguments in Types) — 와일드카드(wildcard) 타입 사용의 변경
- 임포트(Imports) — 임포트 메커니즘의 변경
- 타입 추론의 변경 사항(Changes in Type Inference) — 타입 추론(type inference) 알고리즘의 개정
- 암시적 해소의 변경 사항(Changes in Implicit Resolution) — 암시적 파라미터 처리의 갱신
- 암시적 변환(Implicit Conversions) — 변환 메커니즘의 변경
- 오버로드 해소의 변경 사항(Changes in Overload Resolution) — 메서드 오버로드 선택의 개선
- 매치 표현식(Match Expressions) — 패턴 매칭 문법의 갱신
- 가변 인수 스플라이스(Vararg Splices) — 가변 인수 처리의 변경
- 패턴 바인딩(Pattern Bindings) — 바인딩 패턴의 변경
- Option 없는 패턴 매칭(Option-less pattern matching) — 간소화된 패턴 매칭 접근법
- 자동 에타 확장(Automatic Eta Expansion) — 함수 확장 규칙의 변경
- 컴파일러 플러그인의 변경 사항(Changes in Compiler Plugins) — 플러그인 아키텍처의 변경
- 지연 값 초기화(Lazy Vals Initialization) — `lazy val` 초기화의 갱신
- 메인 메서드(Main Methods) — 진입점(entry point) 정의의 변경
- 보간 내 이스케이프(Escapes in interpolations) — 문자열 보간(string interpolation) 이스케이프의 변경

이 문서는 위 항목 중 핵심적인 변경 기능들을 상세히 다룹니다.

---

## 타입에서의 와일드카드 인수(Wildcard Arguments in Types)

> 📘 **처음 배우는 분께 — Scala 2에서 `_`는 타입 자리에서 "아무 타입이나"였다**
> Scala 2에서는 타입 안에 밑줄 `_`를 써서 "이 자리에 어떤 타입이 와도 상관없다"를 표현했습니다.
> 예를 들어 `List[_]`는 "원소 타입이 무엇인지는 모르지만 아무튼 어떤 `List`"라는 뜻입니다(이런 걸 *와일드카드 타입*이라 부릅니다).
> 문제는 같은 `_`가 `List[_]`(아무 타입)와 `F[_]`(타입 하나를 받는 타입 생성자, 00번 7번 "고차 타입" 참고)에서 서로 다른 뜻으로 쓰여 헷갈렸다는 점입니다.
> Scala 3는 "아무 타입이나"는 `?`로 옮기고, `_`는 다른 용도로 비워두려 합니다.

Scala 3는 타입에서 사용하는 와일드카드(wildcard) 문법을 기존의 `_`에서 `?`로 점진적으로 전환하고 있습니다. 새 문법의 예시는 다음과 같습니다.

```scala
List[?]
Map[? <: AnyRef, ? >: Null]
```

### 동기(Motivation)

> 💡 **왜 필요한가**
> 한 기호(`_`)가 문맥에 따라 두 가지 뜻을 가지면, 사람도 컴파일러도 의도를 추측해야 합니다.
> `?`(와일드카드)와 `_`(자리표시자)로 역할을 나누면 코드만 보고도 뜻이 분명해집니다.
> 또 와일드카드를 `?`로 쓰는 건 Java의 `List<?>`와도 표기가 같아, Java를 아는 사람에게 익숙합니다.

이러한 변경의 동기는 밑줄 기호 `_`를 **익명 타입 파라미터(anonymous type parameter)**에 사용할 수 있게 만드는 것입니다. 이는 값 파라미터 목록(value parameter list)에서 `_`가 쓰이는 방식과 일관성을 맞춥니다.

기존에는 타입 람다(type lambda)로서의 `C[_]`를 `[X] =>> C[X]`로 작성해야 했지만, 새 접근법은 고계 타입(higher-kinded type)에 대한 이 표기를 단순화합니다.

와일드카드 기호로 `?`를 선택한 것은 Java의 기존 와일드카드 타입 문법과 맞추기 위해서입니다. 동시에 `F[_]`가 문맥에 따라 서로 다른 의미(타입 생성자 파라미터(type constructor parameter)이거나 존재 와일드카드 타입(existential wildcard type))를 가질 수 있던 불일치를 제거합니다.

### 마이그레이션 전략(Migration Strategy)

기존 코드베이스와 kind-projector 컴파일러 플러그인을 수용하기 위해 전환은 여러 단계로 진행됩니다.

**1단계 (Scala 3.0–3.3):** `_`와 `?` 모두 와일드카드 문법으로 합법(legal)입니다.

**2단계 (Scala 3.4):** 와일드카드로서의 밑줄 `_`은 더 이상 사용되지 않음(deprecated)으로 표시되며, 자동 변환을 위한 `-rewrite` 옵션이 제공됩니다.

**3단계 (미래):** `_`의 의미는 완전히 **타입 파라미터 자리표시자(type parameter placeholder)**로 전환됩니다.

### kind-projector 호환성(Kind-Projector Compatibility)

`-Ykind-projector` 컴파일러 옵션 하에서의 변화는 다음과 같습니다.

- Scala 3.0: `*`가 타입 파라미터 자리표시자로 사용됩니다.
- Scala 3.2: `*`는 `_` 선호로 인해 사용 중단(deprecated)되며, `-rewrite` 지원이 제공됩니다.
- Scala 3.3: `*`가 제거되고, `_`가 타입 파라미터 자리표시자 문법으로 단독 사용됩니다.

### 교차 컴파일 옵션(Cross-Compilation Options)

`-Ykind-projector:underscores` 옵션은 `_`을 타입 파라미터 자리표시자로 취급하고 `?`을 와일드카드 전용으로 예약할 수 있게 합니다. Scala 2 호환을 위해서는, kind-projector 0.13 이상 및 Scala 2.13.5+/2.12.14+ 환경에서 `-Xsource:3 -P:kind-projector:underscore-placeholders` 옵션을 사용합니다.

---

## 임포트(Imports)

> 📘 **처음 배우는 분께 — Scala 2의 임포트 문법**
> Scala 2에서는 "패키지 안의 모든 것을 가져오기"를 `import scala.annotation._` 처럼 밑줄 `_`로,
> "이름 바꿔 가져오기"를 `import A.{min => minimum}` 처럼 화살표 `=>`로 썼습니다.
> 그런데 `_`는 와일드카드 타입에도, `=>`는 함수 타입에도 쓰이는 기호라 임포트에서만 또 다른 뜻으로 겹쳤습니다.
> Scala 3는 이를 각각 `*`(모두)와 `as`(이름 바꾸기)라는 전용 표현으로 바꿔 헷갈림을 줄였습니다.

Scala 3는 임포트(import)와 익스포트(export) 문법을 현대화하여, 밑줄(`_`)과 화살표(`=>`)를 더 명시적인 키워드로 대체합니다.

### 와일드카드 임포트(Wildcard Imports)

새 문법은 와일드카드 임포트에 밑줄 대신 `*`를 사용합니다.

```scala
import scala.annotation.*  // annotation 패키지의 모든 것을 임포트한다
```

`*`라는 이름의 멤버를 명시적으로 임포트하려면 백틱(backtick)을 사용합니다.

```scala
object A:
  def * = ...
  def min = ...

object B:
  import A.`*`   // `*`만 임포트한다

object C:
  import A.*     // A의 모든 것을 임포트한다
```

### 임포트 이름 변경(Renaming Imports)

`as` 키워드가 이름 변경(rename) 또는 제외(exclude)를 위해 `=>` 연산자를 대체합니다. 단일 이름 변경에는 더 이상 중괄호가 필요하지 않습니다.

```scala
import A.{min as minimum, `*` as multiply}
import Predef.{augmentString as _, *}     // augmentString을 제외한 모든 것을 임포트한다
import scala.annotation as ann
import java as j
```

### 마이그레이션 지원(Migration Support)

교차 빌드(cross-building) 호환성을 위해 Scala 3.0은 새 문법과 더불어 와일드카드용 `_`와 이름 변경용 `=>`를 사용하는 기존 임포트 문법도 지원합니다. 레거시 문법은 향후 버전에서 제거될 예정입니다. `-source 3.1-migration -rewrite` 설정으로 자동 변환을 수행할 수 있습니다.

### 형식 문법(Formal Syntax)

```
Import            ::=  'import' ImportExpr {',' ImportExpr}
ImportExpr        ::= SimpleRef {'.' id} '.' ImportSpec
                    | SimpleRef `as` id
ImportSpec        ::=  NamedSelector
                    |  WildcardSelector
                    | '{' ImportSelectors) '}'
NamedSelector     ::=  id ['as' (id | '_')]
WildCardSelector  ::=  '*' | 'given' [InfixType]
ImportSelectors   ::=  NamedSelector [',' ImportSelectors]
                    |  WildCardSelector {',' WildCardSelector}
```

---

## 암시적 변환(Implicit Conversions)

> 📘 **처음 배우는 분께 — Scala 2에서는 `implicit def` 한 줄이면 타입이 몰래 바뀌었다**
> 00번 9번의 (2)번에서 봤듯, Scala 2에서는 `implicit def intToStr(n: Int): String = ...` 처럼
> "A를 받아 B를 돌려주는 implicit 메서드"를 하나 정의해 두면, B 자리에 A를 써도 컴파일러가 알아서 변환해 넣었습니다.
> 편하지만 "어디서 무엇이 자동으로 바뀌었는지" 추적이 어려워 버그의 원인이 되곤 했습니다.
> Scala 3는 이 변환을 `scala.Conversion`이라는 전용 타입으로 모으고, 켜려면 명시적으로 임포트하게 해서 "마법"을 줄였습니다.

암시적 변환(implicit conversion)은 "뷰(view)"라고도 불리며, 컴파일러가 두 가지 핵심 상황에서 표현식을 자동으로 변환할 수 있게 합니다.

1. **타입 불일치(Type Mismatch):** 타입 `T`의 표현식이 주어졌으나 타입 `S`가 요구될 때
2. **누락된 멤버(Missing Members):** 타입 `T`가 정의하지 않은 멤버 `m`에 접근할 때

이때 컴파일러는 암시적 스코프(implicit scope)에서 적절한 변환을 탐색합니다.

### 변환 메커니즘(Conversion Mechanisms)

암시적 변환을 정의하는 두 가지 방법이 있습니다.

- 시그니처가 `T => S` 또는 `(=> T) => S`인 `implicit def`
- `scala.Conversion[T, S]` 타입의 암시적 값(implicit value)

**중요:** 암시적 변환을 정의할 때는 `scala.language.implicitConversions` 임포트 또는 `-language:implicitConversions` 컴파일러 플래그가 필요합니다. 없으면 경고가 발생합니다.

> 💡 **왜 필요한가**
> "임포트를 해야만 정의가 허용된다"는 건 일종의 안전장치입니다.
> 암시적 변환은 강력하지만 코드를 추적하기 어렵게 만들기 때문에, "정말 이 위험을 감수하겠다"고 한 번 더 선언하게 한 것입니다.
> 이렇게 하면 무심코 변환이 켜지는 일을 막고, 코드를 읽는 사람도 그 파일에 변환이 있음을 임포트 줄에서 바로 알 수 있습니다.

### 코드 예시

#### 예시 1: 타입 변환(Type Conversion)

```scala
import scala.language.implicitConversions
implicit def int2Integer(x: Int): java.lang.Integer =
  x.asInstanceOf[java.lang.Integer]
```

이는 `scala.Int` 값을 `java.lang.Integer`를 기대하는 Java 메서드에 전달할 수 있게 합니다.

#### 예시 2: 변환을 통한 Ordering(Ordering via Conversion)

```scala
import scala.language.implicitConversions
implicit def ordT[T, S](
    implicit conv: Conversion[T, S],
             ordS: Ordering[S]
   ): Ordering[T] =
  (x: T, y: T) => ordS.compare(x, y)

class A(val x: Int)

implicit val AToInt: Conversion[A, Int] = _.x

implicitly[Ordering[Int]]  // 표준 라이브러리에서 사용 가능
implicitly[Ordering[A]]    // A를 Int로 변환한 뒤 Int의 Ordering을 사용한다
```

이 예시는 암시적 변환을 통해 기존의 `Ordering` 인스턴스를 새로운 타입에 활용하는 방법을 보여줍니다.

---

## 암시적 해소의 변경 사항(Changes in Implicit Resolution)

> 📘 **처음 배우는 분께 — "암시적 해소"가 뭔가요**
> 00번 9·10번에서 본 "컴파일러가 범위 안에서 알맞은 값을 알아서 찾아 채워주는 것"을 떠올리세요.
> 그 "알맞은 후보를 어디서, 어떤 순서로, 어느 것을 골라 채울지" 정하는 규칙 전체를 **암시적 해소(implicit resolution)**라고 합니다.
> 후보가 여러 개라 "어느 걸 써야 할지 모르겠다"고 컴파일러가 멈추면 **모호성(ambiguity) 오류**가 납니다.
> Scala 2와 3는 이 "찾는 규칙"이 세부적으로 여러 군데 달라졌고, 아래는 그 차이 목록입니다. 처음엔 큰 그림(규칙이 더 일관적·예측가능해졌다)만 잡아도 충분합니다.

Scala 3는 성능을 위한 더 공격적인 캐싱(more aggressive caching)을 포함하는 새로운 암시적 해소(implicit resolution) 알고리즘을 구현합니다. 새로운 `given` 구문과 레거시 `implicit` 선언 모두에 영향을 주는 언어 수준 변경 사항이 함께 도입되었습니다.

### 1. 명시적 타입 선언 요구

암시적 값(implicit value)과 메서드는 로컬 블록을 제외하고 타입을 명시적으로 선언해야 합니다.

> 💡 **왜 필요한가**
> Scala 2에서는 `implicit val x = ...`처럼 타입을 생략하면 컴파일러가 추론해 줬습니다.
> 그런데 암시값은 "타입을 보고" 후보가 골라지므로, 타입이 눈에 안 보이면 무엇이 끼어들지 읽는 사람이 알기 어렵습니다.
> 그래서 Scala 3는 (잠깐 쓰고 마는 로컬 블록을 빼고) 암시값에 타입을 꼭 적게 했습니다. "공개적으로 쓰일 부품엔 이름표를 붙여라"는 셈입니다.

```scala
class C {
  val ctx: Context = ...        // ok
  /*!*/ implicit val x = ...    // error: 타입을 명시적으로 지정해야 한다
  /*!*/ implicit def y = ...    // error: 타입을 명시적으로 지정해야 한다
}
val y = {
  implicit val ctx = this.ctx // ok
  ...
}
```

### 2. 중첩 고려(Nesting Consideration)

더 깊은 중첩(nesting)이 암시적 선택의 우선순위를 결정하며, 이전에 모호성(ambiguity) 오류로 처리되던 경우를 해소합니다.

```scala
def f(implicit i: C) = {
  def g(implicit j: C) = {
    implicitly[C]  // i가 아닌 j로 해소된다
  }
}
```

### 3. 패키지 프리픽스 제외(Package Prefix Exclusion)

패키지 프리픽스(package prefix)는 더 이상 암시적 탐색 스코프에 기여하지 않습니다. 정의 패키지 밖의 참조는 패키지 수준 암시값(package-level implicit)을 제외합니다.

### 4. 앵커와 암시적 스코프 정의(Anchor and Implicit Scope Definitions)

**앵커(Anchor):** 객체(object), 클래스(class), 트레잇(trait), 추상 타입(abstract type), 불투명 타입 별칭(opaque type alias), 매치 타입 별칭(match type alias)에 대한 참조입니다. 패키지는 `-source:3.0-migration` 하에서만 앵커로 취급됩니다.

**타입 T의 앵커**는 다음을 포함합니다.

- 타입 자체가 앵커를 참조하는 경우 그 타입
- 별칭 타입(aliased type)의 앵커
- 타입 파라미터(type parameter)에 대한 경계 앵커(bound anchor)들의 합집합
- 싱글톤 참조(singleton reference)의 기저 타입(underlying type)의 앵커
- 한정 참조(qualified reference)에 대한 경로 앵커(path anchor)

**타입 T의 암시적 스코프(implicit scope)**는 다음을 포함합니다.

- 클래스에 대한 컴패니언 객체(companion object)
- 객체 참조에 대한 그 객체 자체
- 부모 클래스(parent class)의 스코프
- 타입 별칭(type alias)에 동반되는 객체
- 경로 텀(path term) 참조

### 5. 모호성 전파(Ambiguity Propagation)

재귀적 탐색 단계에서 발생한 모호성(ambiguity)은 호출자(caller)에게 전파됩니다.

```scala
class A
class B extends C
class C
implicit def a1: A
implicit def a2: A
implicit def b(implicit a: A): B
implicit def c: C

implicitly[C]  // 이제 모호하다 (b(a1) vs b(a2) vs c)
```

새로운 `scala.util.NotGiven[Q]` 타입은 부정(negation)을 가능하게 하며, `Q`에 대한 암시적 탐색이 실패할 때 성공합니다.

### 6. 발산을 정상적 실패로 취급(Divergence as Normal Failure)

발산하는 암시값(divergent implicit)이 발생해도 암시적 탐색이 중단되지 않고, 다른 대안(alternative)이 계속 시도됩니다.

### 7. 이름에 의한 호출 동등성(Call-by-Name Parity)

Scala 3는 이름에 의한 호출(call-by-name)과 값에 의한 호출(call-by-value) 암시적 변환 사이의 우선순위 구분을 없앱니다.

> 📘 **처음 배우는 분께 — call-by-value vs call-by-name**
> 00번 11번을 떠올리면, 보통 인자는 `x: Int`처럼 "넘기는 순간 값을 계산"합니다(값에 의한 호출, call-by-value).
> 반대로 `x: => Int`처럼 `=>`를 앞에 붙이면 "넘길 땐 계산 안 하고, 실제로 쓰는 순간 계산"합니다(이름에 의한 호출, call-by-name, 지연 평가).
> 위 예시의 `conv1(x: Int)`과 `conv2(x: => Int)`는 이 둘의 차이입니다.
> Scala 2는 둘 중 값에 의한 호출 쪽을 더 우선했지만, Scala 3는 그 우선순위 구분을 없애서 둘 다 후보면 **모호하다**고 봅니다.

```scala
implicit def conv1(x: Int): A = new A(x)
implicit def conv2(x: => Int): A = new A(x)
def buzz(y: A) = ???
buzz(1)   // error: 모호하다(ambiguous)
```

### 8. 컨텍스트 파라미터 특수성(Context Parameter Specificity)

컨텍스트 파라미터(context parameter)가 없는 대안이 컨텍스트 파라미터가 있는 대안보다 더 특수(specific)한 것으로 간주됩니다. 수정된 오버로드 해소 규칙에 따르면, 대안 A가 B보다 더 특수한 조건은 다음과 같습니다.

- A의 상대 가중치(relative weight)가 더 크거나,
- 가중치가 같고, A는 암시적 파라미터(implicit parameter)를 받지 않지만 B는 받거나,
- 가중치가 같고, 둘 다 암시적 파라미터를 받지만, 암시적 파라미터를 일반 파라미터처럼 취급할 때 A가 더 특수한 경우

### 9. 상속 깊이 추이성(Inheritance Depth Transitivity)

암시적 특수성(specificity) 규칙이 이제 추이성(transitivity)을 보장합니다. `A`의 암시값 `a`가 `B`의 암시값 `b`보다 더 특수한 조건은 다음과 같습니다.

- `A`가 `B`를 확장(extends)하거나,
- `A`가 컴패니언 클래스가 `B`를 확장하는 객체이거나,
- 둘 다 객체이고, `B`가 상속된 암시적 멤버를 갖지 않으며, `A`의 컴패니언이 `B`의 컴패니언을 확장하는 경우

### 10. given 명확화 변경(Given Disambiguation Changes)

Scala 3.5부터, 두 개의 given이 기대 타입(expected type)에 일치할 때, "가장 특수한 것이 아니라 가장 일반적인(most general) 것을 선택합니다." 마이그레이션 모드(migration mode)는 선호 변경에 대해 경고합니다.

> ⚠️ **짚고 넘어가기 — 이건 동작이 바뀌는 변경이라 주의**
> Scala 2에서 같은 코드가 후보 중 "가장 특수한 것"을 골랐다면, Scala 3.5부터는 "가장 일반적인 것"을 고릅니다.
> 즉 같은 코드라도 *실제로 선택되는 given이 달라질 수 있다*는 뜻입니다.
> 그래서 Scala 3는 마이그레이션 모드에서 "여기서 선택이 바뀐다"고 미리 경고해 줍니다. 옛 코드를 옮길 때 이 경고를 흘려보내지 마세요.

### 11. 재귀적 given 회피 (미래 소스)(Recursive Given Avoidance - Future Source)

`-source future` 하에서, 암시적 해소는 무한 루프를 일으키는 재귀적 given 생성을 회피합니다.

```scala
object Prices {
  opaque type Price = BigDecimal
  object Price{
    given Ordering[Price] = summon[Ordering[BigDecimal]]
  }
}
```

규칙: given `G` 내에서 암시적 탐색을 진행하는 동안, 같은 소유자(owner)에서 `G` 또는 그 이후의 given으로 되돌아가는 결과를 폐기합니다.

**타임라인:**

- 3.3: 변경 없음
- 3.4: 경고(warning) 발생
- 3.5: 오류(error) 발생
- 3.6+: 기본으로 활성화됨

---

## 타입 추론의 변경 사항(Changes in Type Inference)

> 📘 **처음 배우는 분께 — "타입 추론"이란**
> `val x = 1 + 2`라고만 써도 우리가 `: Int`를 안 적었는데 컴파일러가 "x는 `Int`구나" 하고 알아내는 것, 이게 **타입 추론(type inference)**입니다.
> Scala 2도 추론을 했지만, Scala 3는 그 알고리즘을 새로 고쳐서 더 똑똑하고 예측 가능하게 만들었습니다(특히 04번에서 다루는 GADT 같은 까다로운 경우).
> 아래 본문은 그 세부 알고리즘을 글로 풀기보다 영상 자료로 안내하는 "목차" 성격이라, 처음엔 "추론이 개선됐다" 정도만 알면 됩니다.

Scala 3의 타입 추론(type inference) 변경 사항은 공식 문서에서 상세한 서술 대신 아래 두 발표 자료로 심화 이해를 안내합니다.

1. **"Scala 3, Type inference and You!"** — Guillaume Martres의 발표 (2019년 9월, YouTube)
2. **"GADTs in Dotty"** — Aleksander Boruch-Gruszecki의 발표 (2019년 7월, YouTube)

이 항목은 인덱스 역할을 하며, 타입 추론 변경에 대한 상세한 설명은 위 영상 자료에서 확인할 수 있습니다.

---

## 프로그래밍적 구조적 타입(Programmatic Structural Types)

> 📘 **처음 배우는 분께 — "구조적 타입"은 이름이 아니라 모양으로 따지는 타입**
> 보통 타입은 "이 클래스(이름)를 상속했는가"로 맞는지 따집니다(명목적 타이핑).
> **구조적 타입**은 이름 대신 "`close()` 메서드가 있는가?"처럼 *모양(구조)*만 맞으면 받아들입니다. (덕 타이핑과 비슷한 발상)
> 예: `type Closeable = { def close(): Unit }`는 "close()를 가진 무엇이든"이라는 타입입니다.
> Scala 2에도 있었지만 동작 방식이 달랐고, Scala 3는 이를 `Selectable` 트레잇 기반으로 더 안전하고 명시적으로 다시 설계했습니다.

Scala 3의 구조적 타입(structural type)은 동적인(dynamic) 문맥에서 필드와 메서드에 점 표기법(dot notation)으로 접근하면서도 정적 타입 안전성(static type safety)을 유지합니다. 가능한 모든 행(row)에 대해 클래스를 만들기 어려운 데이터베이스 접근 같은 시나리오에서 특히 유용합니다.

### 핵심 개념(Core Concept)

구조적 타입은 부모 타입(parent type)에 없는 멤버를 정련(refinement)으로 추가합니다. 예를 들어:

```scala
type Person = Record { val name: String; val age: Int }
```

이는 `Record`를 구조적 멤버(structural member) `name`과 `age`로 확장한 `Person` 타입을 생성합니다.

### Selectable 트레잇(The Selectable Trait)

구조적 타입의 기초는 `scala.Selectable` 마커 트레잇(marker trait)입니다. `Selectable`을 확장하는 클래스는 필드 이름을 값으로 매핑하는 `selectDynamic` 메서드를 정의하며, 구조적 타입에서의 멤버 선택은 이 메서드 호출로 변환됩니다.

```scala
person.name  // 변환됨: person.selectDynamic("name").asInstanceOf[String]
person.age   // 변환됨: person.selectDynamic("age").asInstanceOf[Int]
```

기본적인 `Record` 구현은 다음과 같습니다.

```scala
class Record(elems: (String, Any)*) extends Selectable:
  private val fields = elems.toMap
  def selectDynamic(name: String): Any = fields(name)
```

인스턴스를 생성할 때는 명시적 타입 단언(type assertion)이 필요합니다.

```scala
val person = Record("name" -> "Emma", "age" -> 42).asInstanceOf[Person]
```

### 동적 메서드 호출(Dynamic Method Calls)

`selectDynamic` 외에도, `Selectable` 클래스는 구조적 멤버에 대한 메서드 호출을 처리하기 위해 `applyDynamic`을 구현할 수 있습니다.

```scala
a.f(b, c)  // 변환됨: a.applyDynamic("f")(b, c)
```

### Java 리플렉션 접근법(Java Reflection Approach)

공통 인터페이스가 없는 서로 무관한 클래스들의 경우, Java 리플렉션을 사용하는 구조적 타입이 해법을 제공합니다. `reflectiveSelectable` 암시적 변환이 이를 가능하게 합니다.

```scala
import scala.reflect.Selectable.reflectiveSelectable

type Closeable = { def close(): Unit }

def autoClose(f: Closeable)(op: Closeable => Unit): Unit =
  try op(f) finally f.close()
```

이 임포트는 리플렉션 기반 디스패치(reflection-based dispatch)가 발생함을 명시적으로 드러냅니다. Scala 2에서는 리플렉션이 자동으로 동작했고 `reflectiveCalls` 언어 임포트를 요구했던 것과 다릅니다.

> 💡 **왜 필요한가**
> "모양만 맞으면 받겠다"를 실제로 실행하려면, 런타임에 "이 객체에 정말 그 메서드가 있나?"를 들여다봐야 할 때가 있는데 이게 **리플렉션(reflection)**이고 느립니다.
> Scala 3는 이 느린 리플렉션이 끼어드는 경우엔 `reflectiveSelectable`을 직접 임포트하게 만들어, "여기서 성능 비용이 든다"는 사실이 코드에 드러나게 했습니다. 비용을 숨기지 않는 설계입니다.

### 로컬 클래스와 익명 클래스(Local and Anonymous Classes)

`Selectable`을 확장하는 로컬 클래스(local class)와 익명 클래스(anonymous class)는 정련된 타입(refined type)을 받습니다.

```scala
trait Vehicle extends reflect.Selectable:
  val wheels: Int

val i3 = new Vehicle:
  val wheels = 4
  val range = 240

i3.range  // Vehicle이 Selectable을 확장하므로 적법(well-formed)하다
```

`Selectable`을 확장하지 않으면 정련이 타입에 추가되지 않아 `i3.range`는 컴파일에 실패합니다.

### scala.Dynamic과의 비교(Comparison with scala.Dynamic)

두 접근법 모두 프로그래밍적 멤버 선택(programmatic member selection)을 사용하지만, 구조적 타입은 타입 선언과 기저 값(underlying value) 사이의 대응(correspondence)을 통해 타입 안전성을 유지하는 반면, `scala.Dynamic`은 완전히 동적인 선택을 허용합니다. 둘 다 `selectDynamic`과 `applyDynamic`을 사용하지만, `Selectable`의 `applyDynamic`은 파라미터 타입 전달을 위해 `java.lang.Class` 인수를 받을 수 있습니다.

---

## 연산자 규칙(Rules for Operators)

### infix 수식자(The infix Modifier)

> 📘 **처음 배우는 분께 — "중위(infix)"가 뭔가요, Scala 2에선 어땠나**
> Scala에서는 메서드를 `a.union(b)` 대신 `a union b`처럼 점과 괄호 없이 가운데에 놓고 쓸 수 있습니다. 이걸 **중위 표기(infix)**라 합니다. (`1 + 2`도 사실 `1.+(2)`입니다.)
> Scala 2에서는 *아무 메서드나* 이렇게 중위로 쓸 수 있어서, 의도치 않게 `list filter ...` 같은 글자 메서드까지 연산자처럼 쓰여 가독성이 들쭉날쭉했습니다.
> Scala 3는 "글자로 된 메서드를 중위로 쓰려면 정의에 `infix`를 붙여라"고 정해, 작성자가 의도한 것만 연산자처럼 쓰이게 했습니다. (`+`, `*` 같은 기호 연산자는 예외 없이 그대로 허용)

Scala 3는 메서드를 중위 연산자(infix operator)로 사용하는 방식을 제어하는 `infix` 수식자(modifier)를 도입합니다. 메서드 정의에 `infix` 수식자를 붙이면 해당 메서드를 중위 연산(infix operation)으로 사용할 수 있습니다.

#### 핵심 요구 사항(Key Requirements)

알파벳-숫자 메서드(alphanumeric method)는 정의에 `infix` 수식자가 있는 경우에만 중위 연산자로 사용할 수 있습니다. 예를 들어:

```scala
trait MultiSet[T]:
  infix def union(other: MultiSet[T]): MultiSet[T]
  def difference(other: MultiSet[T]): MultiSet[T]
  @targetName("intersection")
  def *(other: MultiSet[T]): MultiSet[T]
end MultiSet
```

이 정의 하에서:

- `s1 union s2`는 허용됩니다 (`infix` 수식자가 있음).
- `s1 difference s2`는 사용 중단(deprecation) 경고를 발생시킵니다 (`infix` 수식자가 없음).
- `s1 * s2`는 항상 허용됩니다 (기호 연산자(symbolic operator)).

#### 기술적 세부 사항(Technical Details)

명세에 따르면 "`infix`는 소프트 수식자(soft modifier)입니다. 수식자 위치(modifier position)에 있을 때를 제외하면 일반 식별자처럼 취급됩니다." 추가 제약은 다음과 같습니다.

- 메서드가 다른 메서드를 오버라이드(override)하는 경우, 둘 다 `infix` 어노테이션에 대해 일치해야 합니다 (둘 다 가지거나 둘 다 가지지 않음).
- 수신자(receiver)가 아닌 첫 번째 파라미터 목록은 정확히 하나의 파라미터를 포함해야 합니다.
- 확장 메서드(extension method)도 동일한 단일 파라미터 요구 사항으로 `infix` 표시가 가능합니다.

`infix` 수식자는 타입에도 적용됩니다: "`infix` 수식자는 정확히 두 개의 타입 파라미터를 갖는 타입(type), 트레잇(trait), 클래스(class) 정의에도 부여될 수 있습니다."

### @targetName 어노테이션(The @targetName Annotation)

기호 연산자(symbolic operator)에는 `@targetName` 어노테이션을 포함하는 것이 권장됩니다. 이는 "연산자의 알파벳-숫자 이름 인코딩(encoding)"을 제공하며 다음과 같은 이점이 있습니다.

- **상호 운용성(Interoperability):** 다른 언어가 Scala에 정의된 연산자를 알파벳-숫자 이름으로 호출할 수 있습니다.
- **디버깅(Debugging):** 스택 트레이스(stack trace)가 저수준 인코딩 대신 사람이 읽기 좋은 이름을 표시합니다.
- **문서화(Documentation):** 기호 이름에 대한 검색 가능한 대체 이름(alias)을 제공합니다.

### 문법 변경(Syntax Change)

중요한 변경은 다중 행(multi-line) 표현식에서 중위 연산자가 새 줄을 시작할 수 있게 한 것입니다.

```scala
val str = "hello"
  ++ " world"
  ++ "!"
```

이는 세미콜론 추론(semicolon inference) 규칙이 선두 중위 연산자(leading infix operator) 앞에 세미콜론을 삽입하지 않도록 수정되었기 때문입니다. 유효한 선두 중위 연산자는 다음 조건을 만족해야 합니다.

- 기호(symbolic) 식별자 또는 백틱으로 감싼 식별자일 것
- 새 줄을 시작할 것 (빈 줄 다음이 아닐 것)
- 공백과 표현식을 시작하는 토큰(token)이 뒤따를 것
- 자체 줄에 나타나는 경우 일관된 들여쓰기(indentation)를 유지할 것

### 단항 연산자(Unary Operators)

"단항 연산자(unary operator)는 비어 있더라도 명시적 파라미터 목록을 가져서는 안 됩니다." 단항 연산자는 `unary_op` 명명 규칙을 따르며, 여기서 `op`는 `+`, `-`, `!`, `~` 중 하나입니다. 즉 메서드 이름은 `unary_+`, `unary_-`, `unary_!`, `unary_~`가 됩니다.

---

## 매치 표현식 문법(Match Expressions)

> 📘 **처음 배우는 분께 — Scala 2의 `match`는 "통째로 한 덩어리"였다**
> 00번 3번에서 본 `match`는 값을 여러 경우로 나눠 분기하는 도구입니다.
> Scala 2에서 `match`는 비교적 뻣뻣한 문법 덩어리라, 결과에 또 `match`를 이어 붙이거나 `xs.match`처럼 점 뒤에 쓰는 게 자연스럽지 않았습니다.
> Scala 3는 `match`를 (`map`, `filter` 같은) **메서드처럼 다룰 수 있게** 바꿔서, 아래처럼 연쇄·점 표기가 가능해졌습니다.

Scala 3에서 매치 표현식(match expression)의 문법 처리가 재설계되었습니다. `match`는 여전히 키워드(keyword)이지만, 알파벳 연산자(alphabetical operator)처럼 사용됩니다.

### 1. 연쇄 매치 표현식(Chained Match Expressions)

이제 매치 표현식을 연속으로 연결할 수 있습니다. 예를 들어:

```scala
xs match {
  case Nil => "empty"
  case _   => "nonempty"
} match {
  case "empty"    => 0
  case "nonempty" => 1
}
```

중괄호 없이 작성하면 다음과 같습니다.

```scala
xs match
  case Nil => "empty"
  case _   => "nonempty"
match
  case "empty" => 0
  case "nonempty" => 1
```

### 2. 점(.) 뒤의 매치(Match Following a Period)

이제 매치 표현식은 점 연산자(dot operator) 뒤에 올 수 있습니다.

```scala
if xs.match
  case Nil => false
  case _   => true
then "nonempty"
else "empty"
```

### 3. 검사 대상의 타입 표기 변경(Scrutinee Type Ascription Changes)

매치 대상이 되는 값, 즉 검사 대상(scrutinee)에는 더 이상 타입 표기(type annotation)를 직접 붙일 수 없습니다. 기존 문법 `x : T match { ... }`는 이제 `(x: T) match { ... }`로 작성해야 합니다.

> ⚠️ **짚고 넘어가기 — "검사 대상(scrutinee)"은 그냥 "match로 검사하는 그 값"**
> `scrutinee`는 어려운 말이지만 뜻은 단순합니다. `x match { ... }`에서 검사받는 대상인 `x`를 가리킵니다.
> Scala 2처럼 `x: T match ...`라고 쓰면 "`: T`가 x에 붙는지, match 결과에 붙는지" 애매했기에, Scala 3는 괄호로 묶어 `(x: T) match ...`로 명확히 쓰게 했습니다.

### 문법(Grammar)

갱신된 문법 규칙은 다음과 같습니다.

```
InfixExpr    ::=  ...
               |  InfixExpr MatchClause
SimpleExpr   ::=  ...
               |  SimpleExpr '.' MatchClause
MatchClause  ::=  'match' '{' CaseClauses '}'
```

---

## Option 없는 패턴 매칭(Option-less Pattern Matching)

> 📘 **처음 배우는 분께 — "추출자"와 Scala 2의 `Option` 규칙**
> `case Point(x, y) =>`처럼 패턴이 값을 도로 분해할 수 있는 건, 뒤에서 `unapply`라는 메서드가 "분해 결과"를 돌려주기 때문입니다. 이런 분해 장치를 **추출자(extractor)**라고 합니다.
> Scala 2에서는 이 `unapply`가 "성공/실패"를 표현하려고 거의 항상 `Option`(값이 있을 수도/없을 수도)을 돌려줘야 했습니다.
> Scala 3는 그 `Option` 의무를 풀어, `Boolean`이나 `isEmpty`/`get`을 가진 타입 등 더 다양한 반환 타입을 허용합니다. 그래서 제목이 "Option 없는(Option-less)" 패턴 매칭입니다.

Scala 3는 Scala 2 대비 패턴 매칭(pattern matching) 구현을 단순화합니다. 핵심 이점은 생성된 패턴이 디버깅하기 훨씬 쉬워졌다는 점입니다. 변수들이 디버그 모드에서 모두 나타나고 위치(position)가 정확히 보존됩니다.

Scala 3는 `unapply`와 `unapplySeq` 메서드를 통해 Scala 2 추출자(extractor)의 상위 집합(superset)을 지원합니다.

### 추출자(Extractors)

추출자는 다음 시그니처의 메서드를 노출합니다.

```
def unapply(x: T): U
def unapplySeq(x: T): U
```

이 메서드들은 선두 타입 절(type clause), 텀 절(term clause) 앞뒤의 여러 using 절(using clause), 그리고 선택적 암시 절(implicit clause)을 포함할 수 있습니다.

### 고정 항수 추출자(Fixed-Arity Extractors)

고정 항수 추출자(fixed-arity extractor)는 `unapply`를 사용하며, 반환 타입 `U`는 다음 중 하나를 만족해야 합니다.

- 불리언 매치(Boolean match)
- 프로덕트 매치(Product match)
- `isEmpty: Boolean`과 `get: S` 메서드를 갖는 타입 `R`

#### 불리언 매치(Boolean Match)

`U =:= Boolean`이고 패턴이 0개일 때:

```scala
object Even:
  def unapply(s: String): Boolean = s.size % 2 == 0

"even" match
  case s @ Even() => println(s"$s has an even number of characters")
  case s          => println(s"$s has an odd number of characters")
```

#### 프로덕트 매치(Product Match)

`U <: Product`이고 N개의 연속된 `_1: P1`부터 `_N: PN`까지의 멤버가 있을 때:

```scala
class FirstChars(s: String) extends Product:
  def _1 = s.charAt(0)
  def _2 = s.charAt(1)
  def canEqual(that: Any): Boolean = ???
  def productArity: Int = ???
  def productElement(n: Int): Any = ???

object FirstChars:
  def unapply(s: String): FirstChars = new FirstChars(s)

"Hi!" match
  case FirstChars(char1, char2) =>
    println(s"First: $char1; Second: $char2")
```

#### 단일 매치(Single Match)

타입 `S`인 하나의 패턴으로 매칭할 때:

```scala
class Nat(val x: Int):
  def get: Int = x
  def isEmpty = x < 0

object Nat:
  def unapply(x: Int): Nat = new Nat(x)

5 match
  case Nat(n) => println(s"$n is a natural number")
  case _      => ()
```

#### 이름 기반 매치(Name-based Match)

`S`가 `_1`부터 `_N`까지 이름의 멤버를 N > 1개 가질 때:

```scala
object MyPatternMatcher:
  def unapply(s: String) = AlwaysEmpty

object AlwaysEmpty:
  def isEmpty = true
  def get = NameBased

object NameBased:
  def _1: Int = ???
  def _2: String = ???

"" match
  case MyPatternMatcher(_, _) => ???
  case _ => ()
```

### 가변 항수 추출자(Variadic Extractors)

가변 항수 추출자(variadic extractor)는 `unapplySeq`를 사용하며, 반환 타입 `U`는 다음 중 하나를 만족해야 합니다.

- 시퀀스 매치(Sequence match)
- 프로덕트-시퀀스 매치(Product-sequence match)
- `isEmpty`와 `get` 메서드를 갖는 타입 `R`

#### 시퀀스 매치(Sequence Match)

`V <: X`이고 `lengthCompare`, `apply`, `drop`, `toSeq` 메서드를 가질 때:

```scala
object CharList:
  def unapplySeq(s: String): Option[Seq[Char]] = Some(s.toList)

"example" match
  case CharList(c1, c2, c3, c4, _, _, _) =>
    println(s"$c1,$c2,$c3,$c4")
  case _ =>
    println("Expected *exactly* 7 characters!")
```

#### 프로덕트-시퀀스 매치(Product-Sequence Match)

`V <: Product`이고 마지막 멤버가 시퀀스 패턴을 만족하는 N개의 연속된 멤버를 가질 때:

```scala
class Foo(val name: String, val children: Int*)
object Foo:
  def unapplySeq(f: Foo): Option[(String, Seq[Int])] =
    Some((f.name, f.children))

def foo(f: Foo) = f match
  case Foo(name, x, y, ns*) => ">= two children."
  case Foo(name, ns*)       => "< two children."
```

### 타입 테스트(Type Testing)

추상 타입 테스트(abstract type testing)는 `ClassTag`를 `TypeTest` 또는 그 별칭 `Typeable`로 대체합니다. 추상 타입에 대한 패턴 `_: X`는 스코프에 `TypeTest`가 있어야 하며, 추상 타입과 함께 unapply를 사용하는 패턴 `x @ X()` 역시 마찬가지입니다.

---

## 메인 함수(Main Methods)

> 📘 **처음 배우는 분께 — Scala 2에서 프로그램의 시작점을 만들던 방법**
> 프로그램을 실행하려면 "여기서부터 시작"이라는 진입점(main)이 있어야 합니다.
> Scala 2에서는 `object Hello extends App { ... }`처럼 `App`을 상속하거나, `def main(args: Array[String])`을 직접 적었습니다. 명령줄 인자도 `args(0)`을 꺼내 손수 숫자로 바꾸는 식이라 번거로웠습니다.
> Scala 3는 메서드 위에 `@main`만 붙이면 그 메서드가 진입점이 되고, 인자의 타입 변환(예: 문자열 → `Int`)도 자동으로 해줍니다.

Scala 3는 명령줄 실행 가능 프로그램(command-line executable program)을 만드는 새로운 방법으로 `@main` 어노테이션을 도입합니다. 이 어노테이션을 붙이면 전통적인 "object가 App을 확장하는(object-extends-App)" 패턴 없이도 해당 메서드가 실행 가능 프로그램으로 변환됩니다.

### 기본 예시(Basic Example)

```scala
@main def happyBirthday(age: Int, name: String, others: String*) =
  val suffix =
    age % 100 match
    case 11 | 12 | 13 => "th"
    case _ =>
      age % 10 match
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
  val bldr = new StringBuilder(s"Happy $age$suffix birthday, $name")
  for other <- others do bldr.append(" and ").append(other)
  bldr.toString
```

이는 다음과 같이 호출되는 실행 가능 프로그램을 생성합니다: `scala happyBirthday 23 Lisa Peter`. 출력 결과는 "Happy 23rd birthday, Lisa and Peter"입니다.

### 핵심 특징(Key Characteristics)

**배치(Placement):** 메서드는 최상위 수준(top-level)이거나 정적으로 접근 가능한(statically accessible) 객체 내부에 있을 수 있습니다. 프로그램 이름은 객체 프리픽스(prefix) 없이 메서드 이름에서 결정됩니다.

**파라미터 처리(Parameter Handling):** 메인 메서드의 파라미터 목록은 반복 파라미터(repeated parameter)로 끝날 수 있으며, 해당 파라미터가 명령줄에 주어진 나머지 모든 인수를 받습니다.

**타입 변환(Type Conversion):** 각 파라미터는 문자열을 해당 타입으로 변환하기 위해 대응되는 `scala.util.CommandLineParser.FromString[T]` 타입 클래스(type class) 인스턴스를 필요로 합니다.

### 컴파일러 생성(Compiler Generation)

컴파일러는 다음과 동등한 생성 클래스(generated class)를 만듭니다.

```scala
final class happyBirthday:
  import scala.util.CommandLineParser as CLP
  <static> def main(args: Array[String]): Unit =
    try
      happyBirthday(
        CLP.parseArgument[Int](args, 0),
        CLP.parseArgument[String](args, 1),
        CLP.parseRemainingArguments[String](args, 2))
    catch
      case error: CLP.ParseError => CLP.showError(error)
```

### 오류 처리(Error Handling)

생성된 프로그램은 인수 개수와 타입 호환성을 검증합니다.

```
> scala happyBirthday 22
Illegal command line after first argument: more arguments expected

> scala happyBirthday sixty Fred
Illegal command line: java.lang.NumberFormatException
```

### Scala 2와의 비교(Comparison with Scala 2)

Scala 2에서는 `object happyBirthday extends App`을 사용했으며, 인수 벡터를 직접 파싱(by-hand parsing)해야 했습니다.

`App` 클래스는 사용 중단된(deprecated) `DelayedInit` 트레잇에 의존했습니다. `App`은 제한된 형태로 남아 있지만, 교차 버전 호환성을 위해서는 `Array[String]` 인수를 갖는 명시적 `main` 메서드를 사용하는 것이 권장됩니다.

---

## 자동 에타 확장(Automatic Eta Expansion)

> 📘 **처음 배우는 분께 — `def`(메서드)와 함수 값은 다르고, "에타 확장"이 그 다리다**
> 00번 11번에도 나오듯, Scala에서 `def f(x: Int) = ...`로 만든 건 **메서드**고, `val g: Int => Int = ...`처럼 변수에 담아 이리저리 넘길 수 있는 건 **함수 값**입니다. 둘은 별개입니다.
> 메서드를 함수 값이 필요한 자리에 넣으면, 컴파일러가 메서드를 함수 값으로 감싸 주는데 이걸 **에타 확장(eta expansion)**이라 합니다.
> Scala 2에서는 이 변환을 컴파일러가 머뭇거려서 `m _`처럼 끝에 밑줄을 붙여 "함수로 만들어라"고 직접 일러줘야 할 때가 많았습니다. Scala 3는 이를 대부분 자동으로 해줍니다.

Scala 3는 자동 에타 확장(automatic eta-expansion)을 통해 메서드(method)에서 함수(function)로의 변환을 개선합니다. 하나 이상의 파라미터를 갖는 메서드는 자동으로 함수로 변환됩니다.

### 기본 예시(Basic Example)

```scala
def m(x: Boolean, y: String)(z: Int): List[Int]
val f1 = m
val f2 = m(true, "abc")
```

이는 두 개의 함수 값을 생성합니다.

- `f1: (Boolean, String) => Int => List[Int]`
- `f2: Int => List[Int]`

후행 밑줄 문법(trailing underscore syntax)인 `m _`는 더 이상 필요하지 않으며, 향후 사용 중단(deprecated)될 예정입니다.

### 무항 메서드 예외(Nullary Methods Exception)

자동 에타 확장은 빈 파라미터 목록(empty parameter list)을 갖는 무항(nullary) 메서드를 명시적으로 제외합니다.

```scala
def next(): T
```

`next`에 대한 단순 참조는 자동으로 함수로 변환되지 않습니다. 대신, 함수 값을 만들려면 `() => next()`로 작성해야 합니다. 이 방법이 사용 중단된 `next _` 문법보다 권장됩니다.

> ⚠️ **짚고 넘어가기 — "무항(nullary)"은 인자가 없는 `()` 메서드**
> "무항(nullary)"은 `def next(): T`처럼 *빈 괄호* 파라미터 목록을 가진 메서드를 말합니다.
> 이런 메서드는 `next`라고만 써도 Scala가 `()`를 알아서 끼워 넣어 "지금 실행"으로 해석할 때가 있어, "함수 값으로 만들기"와 충돌합니다.
> 그래서 무항 메서드만은 자동 에타 확장에서 빼고, 함수가 필요하면 `() => next()`라고 분명히 적게 했습니다.

이 제외 규칙은 Scala가 암시적으로 `()` 인수를 삽입하기 때문에 에타 확장과 모호성(ambiguity)이 생길 수 있기 때문입니다. Scala 3가 자동 삽입을 제한하더라도 근본적인 충돌이 남아 있어 이 제약이 유지됩니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other Changed Features](https://docs.scala-lang.org/scala3/reference/changed-features/)
