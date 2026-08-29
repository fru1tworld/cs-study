# Scala 3 변경·삭제된 기능

## Scala 3 변경된 기능

---

### 목차

1. [개요](#개요)
2. [타입에서의 와일드카드 인자(Wildcard Arguments in Types)](#타입에서의-와일드카드-인자wildcard-arguments-in-types)
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

### 개요

- 이 섹션은 Scala 2 대비 Scala 3에서 **변경된 기능**(changed features)을 다룸
- 완전히 새로 도입된 기능과 다른 점: 여기 정리된 항목은 Scala 2에도 존재했음 → Scala 3에서 문법(syntax)이나 의미(semantics)가 바뀐 것

- 참고(읽는 법)
  - 각 항목마다 "Scala 2에서는 이랬다 → Scala 3에서는 이렇게 바뀌었다" 콜아웃을 먼저 배치
  - 콜아웃을 먼저 읽고 본문으로 진입 권장
  - 모든 항목을 처음부터 외울 필요 없음 → 옛 Scala 코드를 만났을 때 참고용으로 활용

- 전체 "Other Changed Features" 섹션이 다루는 주제
  - 수치 리터럴(Numeric Literals): 수치 값을 표현하는 방식의 변경
  - 프로그래밍적 구조적 타입(Programmatic Structural Types): 구조적 타입 처리 방식의 변경
  - 연산자 규칙(Rules for Operators): 연산자 문법과 동작 규칙의 갱신
  - 타입에서의 와일드카드 인자(Wildcard Arguments in Types): 와일드카드(wildcard) 타입 사용의 변경
  - 임포트(Imports): 임포트 메커니즘의 변경
  - 타입 추론의 변경 사항(Changes in Type Inference): 타입 추론(type inference) 알고리즘의 개정
  - 암시적 해소의 변경 사항(Changes in Implicit Resolution): 암시적 매개변수 처리의 갱신
  - 암시적 변환(Implicit Conversions): 변환 메커니즘의 변경
  - 오버로드 해소의 변경 사항(Changes in Overload Resolution): 메서드 오버로드 선택의 개선
  - 매치 표현식(Match Expressions): 패턴 매칭 문법의 갱신
  - 가변 인자 스플라이스(Vararg Splices): 가변 인자 처리의 변경
  - 패턴 바인딩(Pattern Bindings): 바인딩 패턴의 변경
  - Option 없는 패턴 매칭(Option-less pattern matching): 간소화된 패턴 매칭 접근법
  - 자동 에타 확장(Automatic Eta Expansion): 함수 확장 규칙의 변경
  - 컴파일러 플러그인의 변경 사항(Changes in Compiler Plugins): 플러그인 아키텍처의 변경
  - 지연 값 초기화(Lazy Vals Initialization): `lazy val` 초기화의 갱신
  - 메인 메서드(Main Methods): 진입점(entry point) 정의의 변경
  - 보간 내 이스케이프(Escapes in interpolations): 문자열 보간(string interpolation) 이스케이프의 변경

- 이 문서는 위 항목 중 핵심 변경 기능만 상세히 다룸

---

### 타입에서의 와일드카드 인자(Wildcard Arguments in Types)

- 배경 — Scala 2에서 `_`는 타입 자리에서 "아무 타입이나"였음
  - Scala 2: 타입 안에 밑줄 `_`를 써서 "이 자리에 어떤 타입이 와도 상관없다"를 표현
  - 예: `List[_]` = "원소 타입이 무엇인지는 모르지만 아무튼 어떤 `List`" (와일드카드 타입)
  - 문제: 같은 `_`가 `List[_]`(아무 타입)와 `F[_]`(타입 하나를 받는 타입 생성자, 고차 타입)에서 서로 다른 뜻으로 쓰여 헷갈림
  - Scala 3 방향: "아무 타입이나"는 `?`로 옮기고 `_`는 다른 용도로 비워둠

- Scala 3는 타입에서 쓰는 와일드카드 문법을 기존의 `_`에서 `?`로 점진 전환
- 새 문법 예시

```scala
List[?]
Map[? <: AnyRef, ? >: Null]
```

#### 동기(Motivation)

- 왜 필요한가
  - 한 기호(`_`)가 문맥에 따라 두 가지 뜻을 가지면 사람도 컴파일러도 의도를 추측해야 함
  - `?`(와일드카드)와 `_`(자리표시자)로 역할 분리 → 코드만 보고도 뜻이 분명해짐
  - `?`로 와일드카드를 쓰는 방식은 Java의 `List<?>`와 표기가 같아 Java 경험자에게 친숙

- 변경 동기: 밑줄 `_`를 **익명 타입 매개변수**(anonymous type parameter)에 쓸 수 있게 함 → 값 매개변수 목록에서 `_`가 쓰이는 방식과 일관성 확보
- 기존: 타입 람다(type lambda)로서의 `C[_]`를 `[X] =>> C[X]`로 작성 필요
- 새 접근: 고계 타입(higher-kinded type)에 대한 이 표기를 단순화
- `?` 선택 이유: Java의 기존 와일드카드 타입 문법과 맞춤
- 동시 효과: `F[_]`가 문맥별로 다른 의미(타입 생성자 매개변수 vs 존재 와일드카드 타입)를 갖던 불일치 제거

#### 마이그레이션 전략(Migration Strategy)

- 기존 코드베이스·kind-projector 컴파일러 플러그인 수용을 위해 전환은 여러 단계로 진행

- 1단계 (Scala 3.0–3.3): `_`와 `?` 모두 와일드카드 문법으로 합법
- 2단계 (Scala 3.4): 와일드카드로서의 `_`는 사용 중단(deprecated) 표시 → `-rewrite` 옵션으로 자동 변환 가능
- 3단계 (미래): `_`의 의미는 완전히 **타입 매개변수 자리표시자**로 전환

#### kind-projector 호환성(Kind-Projector Compatibility)

- `-Ykind-projector` 컴파일러 옵션 하의 변화
  - Scala 3.0: `*`가 타입 매개변수 자리표시자로 사용됨
  - Scala 3.2: `*`는 `_` 선호로 사용 중단 → `-rewrite` 지원 제공
  - Scala 3.3: `*` 제거 → `_`가 타입 매개변수 자리표시자 문법으로 단독 사용

#### 교차 컴파일 옵션(Cross-Compilation Options)

- `-Ykind-projector:underscores` 옵션: `_`를 타입 매개변수 자리표시자로 취급 → `?`는 와일드카드 전용으로 예약
- Scala 2 호환 조건: kind-projector 0.13 이상 + Scala 2.13.5+/2.12.14+ 환경에서 `-Xsource:3 -P:kind-projector:underscore-placeholders` 옵션 사용

---

### 임포트(Imports)

- 배경 — Scala 2의 임포트 문법
  - "패키지 안의 모든 것을 가져오기": `import scala.annotation._` 처럼 밑줄 `_` 사용
  - "이름 바꿔 가져오기": `import A.{min => minimum}` 처럼 화살표 `=>` 사용
  - 문제: `_`는 와일드카드 타입에도, `=>`는 함수 타입에도 쓰이는 기호라 임포트에서만 또 다른 뜻으로 중복
  - Scala 3: 각각 `*`(모두)와 `as`(이름 바꾸기)라는 전용 표현으로 대체 → 헷갈림 감소

- Scala 3는 임포트(import)·익스포트(export) 문법을 현대화 → 밑줄(`_`)과 화살표(`=>`)를 더 명시적인 키워드로 대체

#### 와일드카드 임포트(Wildcard Imports)

- 새 문법: 와일드카드 임포트에 밑줄 대신 `*` 사용

```scala
import scala.annotation.*  // annotation 패키지의 모든 것을 임포트한다
```

- `*`라는 이름의 멤버를 명시적으로 임포트하려면 백틱(backtick) 사용

```scala
object A:
  def * = ...
  def min = ...

object B:
  import A.`*`   // `*`만 임포트한다

object C:
  import A.*     // A의 모든 것을 임포트한다
```

#### 임포트 이름 변경(Renaming Imports)

- `as` 키워드가 이름 변경(rename)·제외(exclude)를 위해 `=>` 연산자를 대체
- 단일 이름 변경에는 더 이상 중괄호 불필요

```scala
import A.{min as minimum, `*` as multiply}
import Predef.{augmentString as _, *}     // augmentString을 제외한 모든 것을 임포트한다
import scala.annotation as ann
import java as j
```

#### 마이그레이션 지원(Migration Support)

- 교차 빌드(cross-building) 호환성을 위해 Scala 3.0은 새 문법과 함께 와일드카드용 `_`, 이름 변경용 `=>`를 쓰는 기존 임포트 문법도 지원
- 레거시 문법은 향후 버전에서 제거 예정
- `-source 3.1-migration -rewrite` 설정으로 자동 변환 가능

#### 형식 문법(Formal Syntax)

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

### 암시적 변환(Implicit Conversions)

- 배경 — Scala 2에서는 `implicit def` 한 줄이면 타입이 자동 변환됨
  - Scala 2: `implicit def intToStr(n: Int): String = ...` 처럼 "A를 받아 B를 돌려주는 implicit 메서드"를 정의 → B 자리에 A를 써도 컴파일러가 자동 변환
  - 문제: "어디서 무엇이 자동으로 바뀌었는지" 추적이 어려워 버그 원인이 됨
  - Scala 3: 이 변환을 `scala.Conversion`이라는 전용 타입으로 모으고, 켜려면 명시적으로 임포트하게 함 → 암묵성 감소

- 암시적 변환(implicit conversion): "뷰(view)"라고도 불림 → 컴파일러가 두 상황에서 표현식을 자동 변환

1. **타입 불일치(Type Mismatch):** 타입 `T`의 표현식이 주어졌으나 타입 `S`가 요구될 때
2. **누락된 멤버(Missing Members):** 타입 `T`가 정의하지 않은 멤버 `m`에 접근할 때

- 이때 컴파일러는 암시적 스코프(implicit scope)에서 적절한 변환을 탐색

#### 변환 메커니즘(Conversion Mechanisms)

- 암시적 변환을 정의하는 두 가지 방법
  - 시그니처가 `T => S` 또는 `(=> T) => S`인 `implicit def`
  - `scala.Conversion[T, S]` 타입의 암시적 값(implicit value)

- 중요: 암시적 변환 정의 시 `scala.language.implicitConversions` 임포트 또는 `-language:implicitConversions` 컴파일러 플래그 필요 → 없으면 경고 발생

- 왜 필요한가
  - "임포트를 해야만 정의가 허용된다"는 일종의 안전장치
  - 암시적 변환은 강력하지만 코드 추적을 어렵게 만듦 → "정말 이 위험을 감수하겠다"는 재선언 요구
  - 효과: 무심코 변환이 켜지는 일 방지, 파일의 임포트 줄만 봐도 변환 존재 여부 확인 가능

#### 코드 예시

##### 예시 1: 타입 변환(Type Conversion)

```scala
import scala.language.implicitConversions
implicit def int2Integer(x: Int): java.lang.Integer =
  x.asInstanceOf[java.lang.Integer]
```

- 효과: `scala.Int` 값을 `java.lang.Integer`를 기대하는 Java 메서드에 전달 가능

##### 예시 2: 변환을 통한 Ordering(Ordering via Conversion)

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

- 이 예시가 보여주는 것: 암시적 변환을 통해 기존 `Ordering` 인스턴스를 새로운 타입에 활용

---

### 암시적 해소의 변경 사항(Changes in Implicit Resolution)

- 배경 — "암시적 해소"의 의미
  - 전제: 컴파일러가 범위 안에서 알맞은 값을 알아서 찾아 채워주는 동작
  - **암시적 해소**(implicit resolution): "알맞은 후보를 어디서, 어떤 순서로, 어느 것을 골라 채울지" 정하는 규칙 전체
  - 후보가 여러 개라 컴파일러가 선택을 못 하면 **모호성(ambiguity) 오류** 발생
  - Scala 2/3는 이 탐색 규칙이 세부적으로 여러 곳에서 달라짐 → 아래는 차이 목록
  - 큰 그림만 잡아도 충분: 규칙이 더 일관적·예측 가능해짐

- Scala 3는 성능을 위한 더 공격적인 캐싱(more aggressive caching)을 포함하는 새로운 암시적 해소 알고리즘을 구현
- 새로운 `given` 구문과 레거시 `implicit` 선언 모두에 영향을 주는 언어 수준 변경 사항 동반

#### 1. 명시적 타입 선언 요구

- 암시적 값(implicit value)·메서드는 로컬 블록을 제외하고 타입을 명시적으로 선언해야 함

- 왜 필요한가
  - Scala 2: `implicit val x = ...`처럼 타입을 생략하면 컴파일러가 추론
  - 문제: 암시값은 "타입을 보고" 후보가 골라지므로, 타입이 안 보이면 무엇이 끼어들지 읽는 사람이 알기 어려움
  - Scala 3: (로컬 블록 제외) 암시값에 타입을 반드시 적게 함 → "공개적으로 쓰일 부품엔 이름표를 붙여라"

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

#### 2. 중첩 고려(Nesting Consideration)

- 더 깊은 중첩(nesting)이 암시적 선택의 우선순위를 결정 → 이전에 모호성 오류로 처리되던 경우를 해소

```scala
def f(implicit i: C) = {
  def g(implicit j: C) = {
    implicitly[C]  // i가 아닌 j로 해소된다
  }
}
```

#### 3. 패키지 프리픽스 제외(Package Prefix Exclusion)

- 패키지 프리픽스(package prefix)는 더 이상 암시적 탐색 스코프에 기여하지 않음
- 결과: 정의 패키지 밖의 참조는 패키지 수준 암시값(package-level implicit)을 제외

#### 4. 앵커와 암시적 스코프 정의(Anchor and Implicit Scope Definitions)

- **앵커(Anchor):** 객체(object)·클래스(class)·트레잇(trait)·추상 타입(abstract type)·불투명 타입 별칭(opaque type alias)·매치 타입 별칭(match type alias)에 대한 참조
  - 패키지는 `-source:3.0-migration` 하에서만 앵커로 취급

- **타입 T의 앵커**에 포함되는 것
  - 타입 자체가 앵커를 참조하는 경우 그 타입
  - 별칭 타입(aliased type)의 앵커
  - 타입 매개변수(type parameter)에 대한 경계 앵커(bound anchor)들의 합집합
  - 싱글톤 참조(singleton reference)의 기저 타입(underlying type)의 앵커
  - 한정 참조(qualified reference)에 대한 경로 앵커(path anchor)

- **타입 T의 암시적 스코프**(implicit scope)에 포함되는 것
  - 클래스에 대한 컴패니언 객체(companion object)
  - 객체 참조에 대한 그 객체 자체
  - 부모 클래스(parent class)의 스코프
  - 타입 별칭(type alias)에 동반되는 객체
  - 경로 텀(path term) 참조

#### 5. 모호성 전파(Ambiguity Propagation)

- 재귀적 탐색 단계에서 발생한 모호성(ambiguity)은 호출자(caller)에게 전파됨

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

- 새로운 `scala.util.NotGiven[Q]` 타입: 부정(negation)을 가능케 함 → `Q`에 대한 암시적 탐색이 실패할 때 성공

#### 6. 발산을 정상적 실패로 취급(Divergence as Normal Failure)

- 발산하는 암시값(divergent implicit)이 발생해도 암시적 탐색이 중단되지 않음 → 다른 대안(alternative)이 계속 시도됨

#### 7. 이름에 의한 호출 동등성(Call-by-Name Parity)

- Scala 3는 이름에 의한 호출(call-by-name)과 값에 의한 호출(call-by-value) 암시적 변환 사이의 우선순위 구분을 제거

- 배경 — call-by-value vs call-by-name
  - 보통 인자는 `x: Int`처럼 "넘기는 순간 값을 계산"(값에 의한 호출, call-by-value)
  - `x: => Int`처럼 `=>`를 앞에 붙이면 "실제로 쓰는 순간 계산"(이름에 의한 호출, call-by-name, 지연 평가)
  - 아래 예시의 `conv1(x: Int)`과 `conv2(x: => Int)`가 이 차이
  - Scala 2: 값에 의한 호출 쪽을 더 우선
  - Scala 3: 그 우선순위 구분을 제거 → 둘 다 후보면 모호하다고 판정

```scala
implicit def conv1(x: Int): A = new A(x)
implicit def conv2(x: => Int): A = new A(x)
def buzz(y: A) = ???
buzz(1)   // error: 모호하다(ambiguous)
```

#### 8. 컨텍스트 매개변수 특수성(Context Parameter Specificity)

- 컨텍스트 매개변수(context parameter)가 없는 대안이 있는 대안보다 더 특수(specific)한 것으로 간주
- 수정된 오버로드 해소 규칙: 대안 A가 B보다 더 특수한 조건
  - A의 상대 가중치(relative weight)가 더 크거나
  - 가중치가 같고, A는 암시적 매개변수(implicit parameter)를 받지 않지만 B는 받거나
  - 가중치가 같고, 둘 다 암시적 매개변수를 받지만 이를 일반 매개변수처럼 취급할 때 A가 더 특수한 경우

#### 9. 상속 깊이 추이성(Inheritance Depth Transitivity)

- 암시적 특수성(specificity) 규칙이 추이성(transitivity)을 보장하도록 변경
- `A`의 암시값 `a`가 `B`의 암시값 `b`보다 더 특수한 조건
  - `A`가 `B`를 확장(extends)하거나
  - `A`가 컴패니언 클래스가 `B`를 확장하는 객체이거나
  - 둘 다 객체이고, `B`가 상속된 암시적 멤버를 갖지 않으며, `A`의 컴패니언이 `B`의 컴패니언을 확장하는 경우

#### 10. given 명확화 변경(Given Disambiguation Changes)

- Scala 3.5부터: 두 given이 기대 타입(expected type)에 일치할 때 "가장 특수한 것이 아니라 가장 일반적인(most general) 것을 선택"
- 마이그레이션 모드(migration mode): 선호 변경에 대해 경고

- 짚고 넘어가기 — 이건 동작이 바뀌는 변경이라 주의
  - Scala 2: 같은 코드가 후보 중 "가장 특수한 것"을 선택
  - Scala 3.5부터: "가장 일반적인 것"을 선택
  - 결과: 같은 코드라도 실제로 선택되는 given이 달라질 수 있음
  - Scala 3는 마이그레이션 모드에서 "여기서 선택이 바뀐다"고 사전 경고 → 옛 코드 이전 시 이 경고를 놓치지 않아야 함

#### 11. 재귀적 given 회피 (미래 소스)(Recursive Given Avoidance - Future Source)

- `-source future` 하에서 암시적 해소는 무한 루프를 일으키는 재귀적 given 생성을 회피

```scala
object Prices {
  opaque type Price = BigDecimal
  object Price{
    given Ordering[Price] = summon[Ordering[BigDecimal]]
  }
}
```

- 규칙: given `G` 내에서 암시적 탐색을 진행하는 동안, 같은 소유자(owner)에서 `G` 또는 그 이후의 given으로 되돌아가는 결과를 폐기

- 타임라인
  - 3.3: 변경 없음
  - 3.4: 경고(warning) 발생
  - 3.5: 오류(error) 발생
  - 3.6+: 기본으로 활성화

---

### 타입 추론의 변경 사항(Changes in Type Inference)

- 배경 — "타입 추론"의 의미
  - `val x = 1 + 2`라고만 써도 `: Int`를 안 적었는데 컴파일러가 "x는 `Int`"라고 알아내는 것 = **타입 추론**(type inference)
  - Scala 2도 추론을 수행했으나, Scala 3는 알고리즘을 새로 고쳐 더 예측 가능하게 함 (특히 GADT 같은 까다로운 경우)
  - 아래 항목은 세부 알고리즘 설명이 아니라 영상 자료로 안내하는 목차 성격 → "추론이 개선됐다" 정도만 파악하면 됨

- Scala 3의 타입 추론(type inference) 변경 사항: 공식 문서는 상세 서술 대신 아래 두 발표 자료로 심화 이해를 안내

1. **"Scala 3, Type inference and You!"** — Guillaume Martres의 발표 (2019년 9월, YouTube)
2. **"GADTs in Dotty"** — Aleksander Boruch-Gruszecki의 발표 (2019년 7월, YouTube)

- 이 항목은 인덱스 역할 → 타입 추론 변경의 상세 설명은 위 영상 자료 참고

---

### 프로그래밍적 구조적 타입(Programmatic Structural Types)

- 배경 — "구조적 타입"은 이름이 아니라 모양으로 따지는 타입
  - 일반 타입: "이 클래스(이름)를 상속했는가"로 판정(명목적 타이핑)
  - **구조적 타입**: 이름 대신 "`close()` 메서드가 있는가?"처럼 모양(구조)만 맞으면 받아들임 (덕 타이핑과 유사)
  - 예: `type Closeable = { def close(): Unit }` = "close()를 가진 무엇이든"이라는 타입
  - Scala 2에도 있었지만 동작 방식이 다름 → Scala 3는 `Selectable` 트레잇 기반으로 더 안전하고 명시적으로 재설계

- Scala 3의 구조적 타입(structural type): 동적인(dynamic) 문맥에서 필드·메서드에 점 표기법(dot notation)으로 접근하면서도 정적 타입 안전성(static type safety) 유지
- 유용한 시나리오: 가능한 모든 행(row)에 대해 클래스를 만들기 어려운 데이터베이스 접근 등

#### 핵심 개념(Core Concept)

- 구조적 타입은 부모 타입(parent type)에 없는 멤버를 정련(refinement)으로 추가

```scala
type Person = Record { val name: String; val age: Int }
```

- 위 코드의 의미: `Record`를 구조적 멤버(structural member) `name`, `age`로 확장한 `Person` 타입 생성

#### Selectable 트레잇(The Selectable Trait)

- 구조적 타입의 기초: `scala.Selectable` 마커 트레잇(marker trait)
- `Selectable`을 확장하는 클래스: 필드 이름을 값으로 매핑하는 `selectDynamic` 메서드 정의
- 구조적 타입에서의 멤버 선택은 이 메서드 호출로 변환됨

```scala
person.name  // 변환됨: person.selectDynamic("name").asInstanceOf[String]
person.age   // 변환됨: person.selectDynamic("age").asInstanceOf[Int]
```

- 기본적인 `Record` 구현

```scala
class Record(elems: (String, Any)*) extends Selectable:
  private val fields = elems.toMap
  def selectDynamic(name: String): Any = fields(name)
```

- 인스턴스 생성 시 명시적 타입 단언(type assertion) 필요

```scala
val person = Record("name" -> "Emma", "age" -> 42).asInstanceOf[Person]
```

#### 동적 메서드 호출(Dynamic Method Calls)

- `selectDynamic` 외: `Selectable` 클래스는 구조적 멤버에 대한 메서드 호출 처리를 위해 `applyDynamic` 구현 가능

```scala
a.f(b, c)  // 변환됨: a.applyDynamic("f")(b, c)
```

#### Java 리플렉션 접근법(Java Reflection Approach)

- 공통 인터페이스가 없는 서로 무관한 클래스들의 경우: Java 리플렉션을 사용하는 구조적 타입이 해법
- `reflectiveSelectable` 암시적 변환이 이를 가능케 함

```scala
import scala.reflect.Selectable.reflectiveSelectable

type Closeable = { def close(): Unit }

def autoClose(f: Closeable)(op: Closeable => Unit): Unit =
  try op(f) finally f.close()
```

- 이 임포트의 의미: 리플렉션 기반 디스패치(reflection-based dispatch)가 발생함을 명시적으로 드러냄
- Scala 2와의 차이: Scala 2는 리플렉션이 자동으로 동작했고 `reflectiveCalls` 언어 임포트를 요구

- 왜 필요한가
  - "모양만 맞으면 받겠다"를 실행하려면 런타임에 "이 객체에 정말 그 메서드가 있나?"를 확인해야 할 때가 있음 = **리플렉션**(reflection), 속도가 느림
  - Scala 3는 이 느린 리플렉션이 끼어드는 경우 `reflectiveSelectable`을 직접 임포트하게 함 → "여기서 성능 비용이 든다"는 사실을 코드에 명시 → 비용을 숨기지 않는 설계

#### 로컬 클래스와 익명 클래스(Local and Anonymous Classes)

- `Selectable`을 확장하는 로컬 클래스(local class)·익명 클래스(anonymous class)는 정련된 타입(refined type)을 받음

```scala
trait Vehicle extends reflect.Selectable:
  val wheels: Int

val i3 = new Vehicle:
  val wheels = 4
  val range = 240

i3.range  // Vehicle이 Selectable을 확장하므로 적법(well-formed)하다
```

- `Selectable`을 확장하지 않으면 정련이 타입에 추가되지 않음 → `i3.range`는 컴파일 실패

#### scala.Dynamic과의 비교(Comparison with scala.Dynamic)

- 공통점: 두 접근법 모두 프로그래밍적 멤버 선택(programmatic member selection) 사용
- 차이점
  - 구조적 타입: 타입 선언과 기저 값(underlying value) 사이의 대응(correspondence)을 통해 타입 안전성 유지
  - `scala.Dynamic`: 완전히 동적인 선택 허용
- 공통 API: 둘 다 `selectDynamic`, `applyDynamic` 사용
  - `Selectable`의 `applyDynamic`은 매개변수 타입 전달을 위해 `java.lang.Class` 인자를 받을 수 있음

---

### 연산자 규칙(Rules for Operators)

#### infix 수식자(The infix Modifier)

- 배경 — "중위(infix)" 표기와 Scala 2의 문제
  - Scala: 메서드를 `a.union(b)` 대신 `a union b`처럼 점·괄호 없이 가운데 놓고 사용 가능 = **중위 표기**(infix) (`1 + 2`도 사실 `1.+(2)`)
  - Scala 2 문제: 아무 메서드나 중위로 쓸 수 있어 `list filter ...` 같은 글자 메서드까지 연산자처럼 쓰이며 가독성이 들쭉날쭉
  - Scala 3: "글자로 된 메서드를 중위로 쓰려면 정의에 `infix`를 붙여라"로 규정 → 작성자가 의도한 것만 연산자처럼 쓰이게 함 (`+`, `*` 같은 기호 연산자는 예외 없이 그대로 허용)

- Scala 3는 메서드를 중위 연산자(infix operator)로 쓰는 방식을 제어하는 `infix` 수식자(modifier)를 도입
- 메서드 정의에 `infix` 수식자를 붙이면 해당 메서드를 중위 연산(infix operation)으로 사용 가능

##### 핵심 요구 사항(Key Requirements)

- 알파벳-숫자 메서드(alphanumeric method): 정의에 `infix` 수식자가 있어야만 중위 연산자로 사용 가능

```scala
trait MultiSet[T]:
  infix def union(other: MultiSet[T]): MultiSet[T]
  def difference(other: MultiSet[T]): MultiSet[T]
  @targetName("intersection")
  def *(other: MultiSet[T]): MultiSet[T]
end MultiSet
```

- 이 정의 하에서
  - `s1 union s2`: 허용 (`infix` 수식자 있음)
  - `s1 difference s2`: 사용 중단(deprecation) 경고 발생 (`infix` 수식자 없음)
  - `s1 * s2`: 항상 허용 (기호 연산자)

##### 기술적 세부 사항(Technical Details)

- 명세 정의: `infix`는 소프트 수식자(soft modifier) — 수식자 위치(modifier position)가 아니면 일반 식별자처럼 취급됨
- 추가 제약
  - 메서드가 다른 메서드를 오버라이드(override)하는 경우: 둘 다 `infix` 어노테이션에 대해 일치해야 함 (둘 다 가지거나 둘 다 가지지 않음)
  - 수신자(receiver)가 아닌 첫 번째 매개변수 목록은 정확히 하나의 매개변수를 포함해야 함
  - 확장 메서드(extension method)도 동일한 단일 매개변수 요구 사항으로 `infix` 표시 가능

- `infix` 수식자는 타입에도 적용 가능: 정확히 두 개의 타입 매개변수를 갖는 타입(type)·트레잇(trait)·클래스(class) 정의에 부여 가능

#### @targetName 어노테이션(The @targetName Annotation)

- 기호 연산자(symbolic operator)에는 `@targetName` 어노테이션 포함 권장 → "연산자의 알파벳-숫자 이름 인코딩(encoding)" 제공
- 이점
  - **상호 운용성(Interoperability):** 다른 언어가 Scala에 정의된 연산자를 알파벳-숫자 이름으로 호출 가능
  - **디버깅(Debugging):** 스택 트레이스(stack trace)가 저수준 인코딩 대신 사람이 읽기 좋은 이름을 표시
  - **문서화(Documentation):** 기호 이름에 대한 검색 가능한 대체 이름(alias) 제공

#### 문법 변경(Syntax Change)

- 중요 변경: 다중 행(multi-line) 표현식에서 중위 연산자가 새 줄을 시작할 수 있게 됨

```scala
val str = "hello"
  ++ " world"
  ++ "!"
```

- 배경: 세미콜론 추론(semicolon inference) 규칙이 선두 중위 연산자(leading infix operator) 앞에 세미콜론을 삽입하지 않도록 수정됨
- 유효한 선두 중위 연산자의 조건
  - 기호(symbolic) 식별자 또는 백틱으로 감싼 식별자일 것
  - 새 줄을 시작할 것 (빈 줄 다음이 아닐 것)
  - 공백과 표현식을 시작하는 토큰(token)이 뒤따를 것
  - 자체 줄에 나타나는 경우 일관된 들여쓰기(indentation) 유지

#### 단항 연산자(Unary Operators)

- 규칙: 단항 연산자(unary operator)는 비어 있더라도 명시적 매개변수 목록을 가져서는 안 됨
- 명명 규칙: `unary_op` (여기서 `op`는 `+`, `-`, `!`, `~` 중 하나)
- 결과 메서드 이름: `unary_+`, `unary_-`, `unary_!`, `unary_~`

---

### 매치 표현식 문법(Match Expressions)

- 배경 — Scala 2의 `match`는 "통째로 한 덩어리"였음
  - `match`는 값을 여러 경우로 나눠 분기하는 도구
  - Scala 2: `match`는 비교적 뻣뻣한 문법 덩어리 → 결과에 또 `match`를 이어 붙이거나 `xs.match`처럼 점 뒤에 쓰는 게 자연스럽지 않았음
  - Scala 3: `match`를 (`map`, `filter` 같은) **메서드처럼 다룰 수 있게** 변경 → 연쇄·점 표기 가능

- Scala 3에서 매치 표현식(match expression)의 문법 처리가 재설계됨
- `match`는 여전히 키워드(keyword)이지만 알파벳 연산자(alphabetical operator)처럼 사용됨

#### 1. 연쇄 매치 표현식(Chained Match Expressions)

- 이제 매치 표현식을 연속으로 연결 가능

```scala
xs match {
  case Nil => "empty"
  case _   => "nonempty"
} match {
  case "empty"    => 0
  case "nonempty" => 1
}
```

- 중괄호 없이 작성한 형태

```scala
xs match
  case Nil => "empty"
  case _   => "nonempty"
match
  case "empty" => 0
  case "nonempty" => 1
```

#### 2. 점(.) 뒤의 매치(Match Following a Period)

- 이제 매치 표현식은 점 연산자(dot operator) 뒤에 올 수 있음

```scala
if xs.match
  case Nil => false
  case _   => true
then "nonempty"
else "empty"
```

#### 3. 검사 대상의 타입 표기 변경(Scrutinee Type Ascription Changes)

- 매치 대상 값(검사 대상, scrutinee)에는 더 이상 타입 표기(type annotation)를 직접 붙일 수 없음
- 기존 문법 `x : T match { ... }` → `(x: T) match { ... }`로 작성 필요

- 짚고 넘어가기 — "검사 대상(scrutinee)"은 그냥 "match로 검사하는 그 값"
  - `scrutinee` = `x match { ... }`에서 검사받는 대상인 `x`
  - Scala 2처럼 `x: T match ...`라고 쓰면 "`: T`가 x에 붙는지, match 결과에 붙는지" 애매
  - Scala 3: 괄호로 묶어 `(x: T) match ...`로 명확히 표기

#### 문법(Grammar)

- 갱신된 문법 규칙

```
InfixExpr    ::=  ...
               |  InfixExpr MatchClause
SimpleExpr   ::=  ...
               |  SimpleExpr '.' MatchClause
MatchClause  ::=  'match' '{' CaseClauses '}'
```

---

### Option 없는 패턴 매칭(Option-less Pattern Matching)

- 배경 — "추출자"와 Scala 2의 `Option` 규칙
  - `case Point(x, y) =>`처럼 패턴이 값을 분해할 수 있는 이유: `unapply`라는 메서드가 "분해 결과"를 돌려줌. 이런 분해 장치를 **추출자**(extractor)라 함
  - Scala 2: `unapply`가 "성공/실패"를 표현하려고 거의 항상 `Option`(값이 있을 수도/없을 수도)을 반환해야 했음
  - Scala 3: 그 `Option` 의무를 풀어 `Boolean`이나 `isEmpty`/`get`을 가진 타입 등 더 다양한 반환 타입 허용 → 제목이 "Option 없는(Option-less)" 패턴 매칭

- Scala 3는 Scala 2 대비 패턴 매칭(pattern matching) 구현을 단순화
- 핵심 이점: 생성된 패턴이 디버깅하기 훨씬 쉬워짐 → 변수들이 디버그 모드에서 모두 나타나고 위치(position)가 정확히 보존됨
- Scala 3는 `unapply`, `unapplySeq` 메서드를 통해 Scala 2 추출자(extractor)의 상위 집합(superset)을 지원

#### 추출자(Extractors)

- 추출자가 노출하는 메서드 시그니처

```
def unapply(x: T): U
def unapplySeq(x: T): U
```

- 이 메서드들은 선두 타입 절(type clause), 텀 절(term clause) 앞뒤의 여러 using 절(using clause), 선택적 암시 절(implicit clause)을 포함 가능

#### 고정 항수 추출자(Fixed-Arity Extractors)

- 고정 항수 추출자(fixed-arity extractor): `unapply` 사용, 반환 타입 `U`는 아래 중 하나 만족 필요
  - 불리언 매치(Boolean match)
  - 프로덕트 매치(Product match)
  - `isEmpty: Boolean`과 `get: S` 메서드를 갖는 타입 `R`

##### 불리언 매치(Boolean Match)

- 조건: `U =:= Boolean`이고 패턴이 0개

```scala
object Even:
  def unapply(s: String): Boolean = s.size % 2 == 0

"even" match
  case s @ Even() => println(s"$s has an even number of characters")
  case s          => println(s"$s has an odd number of characters")
```

##### 프로덕트 매치(Product Match)

- 조건: `U <: Product`이고 N개의 연속된 `_1: P1`부터 `_N: PN`까지의 멤버 존재

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

##### 단일 매치(Single Match)

- 조건: 타입 `S`인 하나의 패턴으로 매칭

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

##### 이름 기반 매치(Name-based Match)

- 조건: `S`가 `_1`부터 `_N`까지 이름의 멤버를 N > 1개 보유

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

#### 가변 항수 추출자(Variadic Extractors)

- 가변 항수 추출자(variadic extractor): `unapplySeq` 사용, 반환 타입 `U`는 아래 중 하나 만족 필요
  - 시퀀스 매치(Sequence match)
  - 프로덕트-시퀀스 매치(Product-sequence match)
  - `isEmpty`와 `get` 메서드를 갖는 타입 `R`

##### 시퀀스 매치(Sequence Match)

- 조건: `V <: X`이고 `lengthCompare`, `apply`, `drop`, `toSeq` 메서드 보유

```scala
object CharList:
  def unapplySeq(s: String): Option[Seq[Char]] = Some(s.toList)

"example" match
  case CharList(c1, c2, c3, c4, _, _, _) =>
    println(s"$c1,$c2,$c3,$c4")
  case _ =>
    println("Expected *exactly* 7 characters!")
```

##### 프로덕트-시퀀스 매치(Product-Sequence Match)

- 조건: `V <: Product`이고 마지막 멤버가 시퀀스 패턴을 만족하는 N개의 연속된 멤버 보유

```scala
class Foo(val name: String, val children: Int*)
object Foo:
  def unapplySeq(f: Foo): Option[(String, Seq[Int])] =
    Some((f.name, f.children))

def foo(f: Foo) = f match
  case Foo(name, x, y, ns*) => ">= two children."
  case Foo(name, ns*)       => "< two children."
```

#### 타입 테스트(Type Testing)

- 추상 타입 테스트(abstract type testing): `ClassTag`를 `TypeTest` 또는 그 별칭 `Typeable`로 대체
- 추상 타입에 대한 패턴 `_: X`는 스코프에 `TypeTest` 필요
- 추상 타입과 함께 unapply를 사용하는 패턴 `x @ X()`도 동일 요구

---

### 메인 함수(Main Methods)

- 배경 — Scala 2에서 프로그램의 시작점을 만들던 방법
  - 프로그램 실행에는 "여기서부터 시작"이라는 진입점(main) 필요
  - Scala 2: `object Hello extends App { ... }`처럼 `App`을 상속하거나 `def main(args: Array[String])`을 직접 작성. 명령줄 인자도 `args(0)`을 꺼내 손수 변환해야 해 번거로웠음
  - Scala 3: 메서드 위에 `@main`만 붙이면 그 메서드가 진입점이 됨. 인자의 타입 변환(예: 문자열 → `Int`)도 자동 처리

- Scala 3는 명령줄 실행 가능 프로그램(command-line executable program)을 만드는 새로운 방법으로 `@main` 어노테이션을 도입
- 효과: 전통적인 "object가 App을 확장하는" 패턴 없이도 해당 메서드가 실행 가능 프로그램으로 변환됨

#### 기본 예시(Basic Example)

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

- 호출 방법: `scala happyBirthday 23 Lisa Peter`
- 출력 결과: "Happy 23rd birthday, Lisa and Peter"

#### 핵심 특징(Key Characteristics)

- **배치(Placement):** 메서드는 최상위 수준(top-level)이거나 정적으로 접근 가능한(statically accessible) 객체 내부에 위치 가능 → 프로그램 이름은 객체 프리픽스(prefix) 없이 메서드 이름에서 결정
- **매개변수 처리(Parameter Handling):** 메인 메서드의 매개변수 목록은 반복 매개변수(repeated parameter)로 끝날 수 있음 → 해당 매개변수가 명령줄에 주어진 나머지 모든 인자를 받음
- **타입 변환(Type Conversion):** 각 매개변수는 문자열을 해당 타입으로 변환하기 위한 `scala.util.CommandLineParser.FromString[T]` 타입 클래스(type class) 인스턴스 필요

#### 컴파일러 생성(Compiler Generation)

- 컴파일러가 만드는 동등한 생성 클래스(generated class)

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

#### 오류 처리(Error Handling)

- 생성된 프로그램은 인자 개수·타입 호환성을 검증

```
> scala happyBirthday 22
Illegal command line after first argument: more arguments expected

> scala happyBirthday sixty Fred
Illegal command line: java.lang.NumberFormatException
```

#### Scala 2와의 비교(Comparison with Scala 2)

- Scala 2: `object happyBirthday extends App` 사용 → 인자 벡터를 직접 파싱(by-hand parsing) 필요
- `App` 클래스는 사용 중단된(deprecated) `DelayedInit` 트레잇에 의존
- `App`은 제한된 형태로 남아 있음 → 교차 버전 호환성을 위해서는 `Array[String]` 인자를 갖는 명시적 `main` 메서드 사용 권장

---

### 자동 에타 확장(Automatic Eta Expansion)

- 배경 — `def`(메서드)와 함수 값은 다르고, "에타 확장"이 그 다리
  - Scala에서 `def f(x: Int) = ...`로 만든 것 = **메서드**, `val g: Int => Int = ...`처럼 변수에 담아 넘길 수 있는 것 = **함수 값**. 둘은 별개
  - 메서드를 함수 값이 필요한 자리에 넣으면 컴파일러가 메서드를 함수 값으로 감쌈 = **에타 확장**(eta expansion)
  - Scala 2: 이 변환을 컴파일러가 머뭇거려서 `m _`처럼 끝에 밑줄을 붙여 명시해야 할 때가 많았음
  - Scala 3: 대부분 자동으로 처리

- Scala 3는 자동 에타 확장(automatic eta-expansion)을 통해 메서드(method)에서 함수(function)로의 변환을 개선
- 하나 이상의 매개변수를 갖는 메서드는 자동으로 함수로 변환됨

#### 기본 예시(Basic Example)

```scala
def m(x: Boolean, y: String)(z: Int): List[Int]
val f1 = m
val f2 = m(true, "abc")
```

- 생성되는 두 함수 값
  - `f1: (Boolean, String) => Int => List[Int]`
  - `f2: Int => List[Int]`

- 후행 밑줄 문법(trailing underscore syntax)인 `m _`는 더 이상 필요 없음 → 향후 사용 중단(deprecated) 예정

#### 무항 메서드 예외(Nullary Methods Exception)

- 자동 에타 확장은 빈 매개변수 목록(empty parameter list)을 갖는 무항(nullary) 메서드를 명시적으로 제외

```scala
def next(): T
```

- `next`에 대한 단순 참조는 자동으로 함수로 변환되지 않음
- 함수 값을 만들려면 `() => next()`로 작성 필요 → 사용 중단된 `next _` 문법보다 권장되는 방식

- 짚고 넘어가기 — "무항(nullary)"은 인자가 없는 `()` 메서드
  - "무항(nullary)" = `def next(): T`처럼 *빈 괄호* 매개변수 목록을 가진 메서드
  - 이런 메서드는 `next`라고만 써도 Scala가 `()`를 자동 삽입해 "지금 실행"으로 해석할 때가 있음 → "함수 값으로 만들기"와 충돌
  - 그래서 무항 메서드만 자동 에타 확장에서 제외, 함수가 필요하면 `() => next()`로 명시

- 제외 이유: Scala가 암시적으로 `()` 인자를 삽입하기 때문에 에타 확장과 모호성(ambiguity)이 생길 수 있음
- Scala 3가 자동 삽입을 제한하더라도 근본적인 충돌이 남아 있어 이 제약은 유지됨

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Other Changed Features](https://docs.scala-lang.org/scala3/reference/changed-features/)

---

## Scala 3 삭제된 기능

---

### 목차

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

### 개요

- 참고(읽는 자세) — 이 문서는 편하게 읽어도 됨
  - 이 문서 = "Scala 2에는 있었지만 Scala 3에서 사라진(또는 권장하지 않게 된) 기능 목록"
  - 즉 옛 Scala에서만 보이던 것들 → Scala 3로 처음 배우는 경우 대부분 몰라도 됨
  - 용도: 옛 코드를 읽다가 "이건 뭐지?" 싶을 때 사전처럼 찾아보기
  - 각 항목의 콜아웃 구성: ① 원래 이게 뭐였나(짧게) → ② 왜 없앴나 → ③ 지금은 뭘 쓰면 되나
  - 외워야 할 새 문법이 아니라 "사라진 옛것"이라는 점만 기억하면 됨

- Scala 2에 존재했지만 Scala 3에서 더 이상 지원되지 않거나 권장되지 않는(deprecated) 기능 목록
- 용도: Scala 2 → Scala 3 업그레이드 시 발생하는 호환성 변경 사항(compatibility breaks) 파악

- 이 문서에서 다루는 기능
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

- 각 기능은 변경 근거(rationale)와 마이그레이션 지침을 담은 상세 문서로 연결됨

- 짚고 넘어가기 — 위 목록 중 일부는 이 문서에 상세 설명이 없음
  - 17개 목록에는 있지만 아래 본문에는 별도 절이 없는 항목을 한 줄씩 짚음
  - **클래스 섀도잉(Class Shadowing)**: 부모 안의 내부 클래스를 자식이 같은 이름의 내부 클래스로 가려 덮어쓰던 동작. Scala 3는 더 엄격히 처리
  - **XML 리터럴(XML Literals)**: 코드 안에 `<a>...</a>` 같은 XML을 직접 적던 기능. 거의 안 쓰여 제외 → 필요시 문자열·라이브러리로 처리
  - **심볼 리터럴(Symbol Literals)**: `'name`처럼 작은따옴표로 "이름표 같은 고유 식별자"를 만들던 표기. Scala 3에서 제외 → 대개 문자열로 대체 가능
  - **약한 적합성(Weak Conformance)**: `List(1, 2.0)`처럼 `Int`와 `Double`을 섞으면 자동으로 `Double`로 맞춰 주던 느슨한 규칙. Scala 3는 더 명확한 규칙으로 변경
  - **와일드카드 초기화(Wildcard Initializer)**: `var x: Int = _`처럼 "일단 기본값으로 비워 두기"를 뜻하던 옛 문법. Scala 3.4부터 `var x: Int = scala.compiletime.uninitialized`로 대체
  - 모두 옛 코드에서나 마주칠 것들 → "이런 게 있었구나" 정도로 충분

---

### DelayedInit

- 배경 — DelayedInit / App이 무엇이었나
  - `App` 트레이트: "메인 메서드를 직접 안 쓰고도 객체 본문에 코드만 적으면 그게 곧 프로그램의 시작점이 되는" 옛 편의 기능
  - 이를 가능케 한 장치가 `DelayedInit`(객체 초기화 코드 실행을 뒤로 미뤄 주던 장치)
  - Scala 3: 이 장치를 없앰 → **대신 `@main`을 함수 앞에 붙이면 그 함수가 프로그램 진입점이 됨**
  - 처음 배우는 경우 `DelayedInit`/`App`은 잊고 `@main`만 기억하면 됨

- `DelayedInit` 트레이트(trait)에 대한 특수 처리는 Scala 3에서 더 이상 지원되지 않음

#### App 클래스에 미치는 영향

- 이전에 `DelayedInit`에 의존하던 `App` 클래스는 이제 부분적으로만 동작
- 여전히 간단한 메인 프로그램 작성 방법으로 `App` 사용 가능

```scala
object HelloWorld extends App {
  println("Hello, world!")
}
```

- 단, 코드는 객체의 초기화자(initializer) 안에서 실행됨
- 의미: 일부 JVM에서 해당 코드가 인터프리터로만 실행됨 → 벤치마킹(benchmarking) 목적에는 부적합

#### 명령줄 인자 접근

- 명령줄 인자(command line arguments)에 접근해야 한다면 명시적인 `main` 메서드 필요

```scala
object Hello:
  def main(args: Array[String]) =
    println(s"Hello, ${args(0)}")
```

#### 권장 대안

- Scala 3의 더 나은 대안: `@main` 메서드
- `@main` 메서드는 "프로그램(program)" 객체를 사용하는 방식보다 프로그램 진입점(entry point) 생성에 더 편리함
- 새 코드에서는 `@main` 메서드 사용으로 마이그레이션 권장

---

### Scala 2 매크로(Scala 2 Macros)

- 배경 — 매크로란
  - 매크로(macro): "컴파일하는 도중에 코드가 스스로 또 다른 코드를 만들어 끼워 넣는" 고급 기능(메타프로그래밍)
  - Scala 2의 옛 매크로: 만들기 까다롭고 깨지기 쉬움
  - Scala 3: 이를 통째로 교체 → `inline` + 인용/스플라이스(`'{ }` / `${ }`)라는 더 안전한 방식으로 대체
  - 고급 주제 → 처음에는 "이런 게 있다" 정도만 인지하면 됨

- 이전의 실험적(experimental) 매크로 시스템은 삭제됨
- 대신 `inline`과 `'{ ... }`/`${ ... }` 코드 생성에 기반한 더 깔끔하고 제한적인 시스템 제공
- 동작 방식
  - `'{ ... }`: 코드의 컴파일을 지연시켜 해당 코드를 담은 객체(object)를 생성
  - `${ ... }`: (쌍대적으로) 코드를 생성하는 표현식(expression)을 평가하고 그 결과를 해당 위치에 삽입
  - `${ ... }`를 포함하는 `inline` 정의 = 매크로(macro) → `${ ... }` 내부 코드는 컴파일 타임(compile-time)에 실행되어 `'{ ... }` 형태의 코드를 생성
  - 코드 내용은 `'{ ... }`/`${ ... }` 프레임워크를 확장한 더 복잡한 리플렉션(reflection) API로 검사·생성 가능

- 관련 구현
  - `inline`은 Scala 3에서 [구현](5_metaprogramming.md)
  - 인용(Quotes) `'{ ... }`과 스플라이스(splices) `${ ... }`는 Scala 3에서 [구현](5_metaprogramming.md)
  - [TASTy 리플렉션(TASTy reflect)](5_metaprogramming.md): 인용된 코드(quoted code)를 검사·생성하기 위한 더 복잡한 트리(tree) 기반 API

---

### 존재 타입(Existential Types)

- 배경 — 존재 타입이 무엇이었나
  - 존재 타입(existential type): "타입 매개변수가 정확히 뭔지는 모르지만, 어쨌든 어떤 타입 하나는 있다"고 말하는 방법
  - 예: `List[T] forSome { type T }` = "원소 타입이 무엇이든 좋은 리스트"
  - 문법이 어렵고 타입 시스템을 불안정하게 만들어 Scala 3는 `forSome`을 제거
  - 대신 와일드카드(`List[?]`처럼 `?`로 "아무 타입"을 표현) 사용 → 아래에서 처리 방식 설명

- `forSome`를 사용하는 존재 타입(existential types, SLS 3.2.12절에 명세)은 Scala 3에서 제거됨
- 언어 개발자들이 제시한 세 가지 주요 근거

- **타입 건전성(Type Soundness) 우려**
  - 제거 효과: DOT와 Scala 3의 근본적인 타입 건전성 원칙(type soundness principle) 위배 해소
  - 원칙 내용: 타입 셀렉션(type selection)의 모든 접두부(prefix)는 런타임에 생성된 값(runtime-constructed value)에서 비롯되거나 확립된 경계(bound)를 가진 타입을 참조해야 함

- **기능 간 상호작용 문제**
  - 존재 타입은 다른 언어 구성 요소(construct)와 상호작용할 때 상당한 복잡성 유발 → 유지보수·예측 가능성 저해

- **제한적인 고유 가치**
  - 존재 타입은 경로 의존 타입(path-dependent types)과 상당 부분 겹침 → 언어에 대한 고유 기여가 제한적

#### 와일드카드 기반 대안

- `forSome` 없이 와일드카드(wildcards)로 표현되는 존재 타입은 여전히 지원되지만 처리 방식이 다름
- 시스템은 이를 정제 타입(refined types)으로 해석. 예:

```
Map[_ <: AnyRef, Int]
```

- 해석: 첫 번째 타입 매개변수가 상한(upper bound)으로 `AnyRef`를 가지고, 두 번째 타입 매개변수가 `Int`의 별칭(alias)인 `Map` 타입

#### Scala 2 호환성

- Scala 2 컴파일 결과로 생성된 클래스 파일(class files) 처리 시: Scala 3는 자신의 타입 시스템으로 존재 타입을 최선의 근사(best-effort approximation)로 처리 시도
- 이 변환 방식의 한계에 대해 경고(warning) 발생

---

### 일반 타입 프로젝션(General Type Projection)

- 배경 — 타입 프로젝션 `T#A`가 무엇이었나
  - 타입은 내부에 또 다른 타입(타입 멤버)을 가질 수 있음. 예: `class Outer { class Inner }`에서 `Inner`는 `Outer`에 속한 타입
  - `T#A` = "타입 `T` 안에 있는 타입 `A`를 바깥에서 직접 가리키는" 표기(`Outer#Inner`)
  - 너무 자유로워 타입 시스템을 불안정하게 만들 수 있음 → Scala 3는 `T`가 "구체적인 타입"일 때만 허용하도록 제한
  - 거의 쓸 일 없는 고급 기능 → 처음에는 넘어가도 됨

- Scala 2는 임의의 타입 `T`와 그 타입 멤버(type member)인 `A`에 대해 일반 타입 프로젝션(general type projection) `T#A`를 허용
- 이 기능은 건전성(soundness) 우려로 Scala 3에서 제거됨

#### Scala 3의 주요 제약

- Scala 3는 `T`가 (추상이 아닌) 구체 타입(concrete type)일 때만 타입 프로젝션을 허용
- 아래에 해당하면 타입이 추상(abstract)으로 간주됨
  - 추상 타입 멤버(`= SomeType` 없이 선언된 `type T`)
  - 타입 매개변수(`[T]`)
  - 추상 타입에 대한 별칭(`type T = SomeAbstractType`)

- `A`에 대한 제약: `T`의 멤버 타입이기만 하면(예: 하위 클래스 `class T { class A }`) 별다른 제약 없음

#### 마이그레이션 지침

- 추상 타입을 대상으로 한 타입 프로젝션에 의존하는 코드는 다음 방법 고려
  - 경로 의존 타입(path-dependent types)
  - 암묵적 매개변수(implicit parameters)

#### 주목할 만한 영향

- 이 제약으로 Scala 2에서 지원되던 "콤비네이터 계산법(combinator calculus)의 타입 수준 인코딩(type-level encoding)" 구현이 불가능해짐
- 이 변경은 문서화된 불건전성(unsoundness) 문제를 해소하며 Scala 3의 더 엄격한 타입 시스템 설계와 일관됨

---

### do-while

- 배경 — do-while이 무엇이었나
  - 다른 언어(Java, C 등)의 `do { ... } while (조건)`과 동일 → "본문을 먼저 한 번 실행하고, 그다음 조건을 검사해 반복"하는 반복문
  - Scala 3는 이 전용 문법을 제거. 이유: 잘 안 쓰이는 데다 `do`라는 단어를 다른 문법(예: `for ... do`)에 쓰기로 했기 때문
  - **`while`로 동일하게 표현 가능** → 아래 변환 예시 참고

- 아래 구문 구성 요소

```
do <body> while <cond>
```

- 더 이상 지원되지 않음 → 대신 아래의 동등한 `while` 반복문 사용 권장

```
while ({ <body> ; <cond> }) ()
```

- 변환 예시. 아래 코드 대신

```
do
  i += 1
while (f(i) == 0)
```

- 이렇게 작성

```
while
  i += 1
  f(i) == 0
do ()
```

- `while`의 조건으로 블록(block)을 사용하는 방식은 "1.5회 반복(loop-and-a-half)" 문제 해결책도 제공. 예:

```
while
  val x: Int = iterator.next
  x >= 0
do print(".")
```

#### 이 구성 요소를 삭제한 이유

- `do-while`은 비교적 드물게 사용됨 → `while`만으로도 충실하게(faithfully) 표현 가능 → 별도 구문 구성 요소로 둘 의의가 낮음
- 새로운 구문 규칙(syntax rules) 하에서 `do`는 문장 연속(statement continuation)으로 사용됨 → 문장 도입(statement introduction)으로서의 의미와 충돌

---

### 프로시저 문법(Procedure Syntax)

- 배경 — 프로시저 문법이 무엇이었나
  - Scala 2: 아무 값도 돌려주지 않는 메서드를 `def f() { ... }`처럼 `=` 없이 중괄호만 붙여 작성 가능. 반환 타입은 자동으로 `Unit`(값이 없음을 뜻하는 타입)이 됨
  - 문제: `=`을 깜빡 빠뜨린 건지 일부러 뺀 건지 헷갈려 실수 유발
  - Scala 3: 이를 제거 → **항상 `=`을 붙여 `def f() = { ... }` 또는 `def f(): Unit = { ... }`로 작성**

- **프로시저 문법**(procedure syntax)은 Scala 3에서 제거됨
- 다음 옛 구문 형식

```
def f() { ... }
```

- 더 이상 지원되지 않음 → 아래 대안 중 하나 사용 필요

```
def f() = { ... }
def f(): Unit = { ... }
```

#### 마이그레이션 지원

- Scala 3는 레거시(legacy) 코드를 위한 하위 호환성 옵션 제공: `-source:3.0-migration` 컴파일러 플래그로 옛 구문 허용
- `-migration` 옵션 활성화 시 컴파일러가 구식 코드를 Scala 3 표준에 맞도록 자동 재작성(rewrite)
- [Scalafix](https://scalacenter.github.io/scalafix/) 도구도 프로시저 문법을 Scala 3 호환 형식으로 변환하는 자동 재작성 기능 제공 → 대규모 코드베이스 마이그레이션 단순화

---

### 패키지 객체(Package Objects)

- 배경 — 패키지 객체가 무엇이었나
  - Scala: 함수(`def`)나 타입 별칭(`type`) 같은 정의는 원래 클래스나 `object` 안에만 둘 수 있었음
  - "이 패키지 전체에서 같이 쓸 공용 함수·상수"를 담을 곳이 마땅치 않아 `package object`라는 특별한 상자를 만들어 모아 둠
  - Scala 3: **파일 아무 곳에나(클래스 밖에) 정의를 바로 둘 수 있게(최상위 정의)** 함 → 이 상자가 불필요해짐

- 아래와 같은 형태의 패키지 객체(package objects)는 더 이상 권장되지 않으며(deprecated) 결국 제거될 예정

```scala
package object p {
  val a = ...
  def b = ...
}
```

#### 삭제되는 이유

- 모든 종류의 정의를 최상위(top-level)에 직접 작성 가능 → 패키지 객체가 더 이상 필요 없음

#### 현대적인 대안

- 패키지 객체 대신 소스 파일 내에서 정의를 패키지 수준(package level)에 직접 배치 가능

```scala
package p
type Labelled[T] = (String, T)
val a: Labelled[Int] = ("count", 1)
def b = a._2

case class C()

extension (x: C) def pair(y: C) = (x, y)
```

- 같은 패키지에 속한 여러 소스 파일은 클래스·객체와 자유롭게 섞인 최상위 정의를 포함 가능

#### 구현 세부 사항

- 컴파일러는 최상위 정의들에 대해 합성 래퍼 객체(synthetic wrapper object)를 생성
- 예: `src.scala` 파일이 그런 정의들을 포함하면 `src$package`라는 합성 객체로 감싸짐
- 이 감싸기 동작은 패키지를 통해 정의에 접근하는 코드에는 투명하게(transparent) 처리됨

#### 중요한 고려 사항

- 이 동작에 관한 네 가지 핵심 사항
  1. 정의가 감싸질 때, 소스 파일 이름은 바이너리 호환성(binary compatibility)에 영향
  2. 메인 메서드(main methods)는 (자동 감싸기에 의존하지 말고) 명시적으로 이름이 붙은 객체에 배치 필요
  3. `private` 한정자는 감싸기 여부와 무관하게 동작
  4. 같은 이름을 공유하는 오버로딩(overloaded) 정의들은 반드시 동일한 소스 파일에서 비롯되어야 함

---

### 초기화 선행자(Early Initializers)

- 배경 — 초기화 선행자가 무엇이었나
  - 클래스를 만들 때, 부모 클래스나 트레이트가 초기화되기 전에 먼저 어떤 값을 정해 두고 싶을 때가 있었음
    - 예: 부모 trait가 동작하려면 어떤 필드 값이 미리 채워져 있어야 하는 경우
  - 이를 위한 어색한 문법이 초기화 선행자 → 사실상 "트레이트에 값을 미리 넘기고 싶다"는 욕구의 우회책
  - **Scala 3는 트레이트에 직접 값을 넘기는 "트레이트 매개변수"를 정식 지원** → 이 우회책이 필요 없어짐

- 초기화 선행자(early initializers): Scala 2의 기능, 클래스 선언 내에서 초기화 코드를 실행하게 해주던 특정 구문

```
class C extends { ... } with SuperClass ...
```

- 이 기능은 Scala 3에서 제거됨
- 사용 빈도가 낮았고, 주로 트레이트 매개변수(trait parameters)의 부재를 보완하는 우회책이었음 → 트레이트 매개변수가 Scala 3에서 직접 지원되며 필요성 소멸

#### 핵심 사항

- Scala 3는 트레이트 매개변수(trait parameters)를 일급(first-class) 언어 기능으로 도입 → 상속 시점에 트레이트로 매개변수를 직접 전달 가능
- 결과: 초기화 선행자 패턴이 더 이상 필요하지 않음
- 참고: 초기화 선행자는 본래 Scala 언어 명세(Scala Language Specification) 5.1.6절에 문서화됨

#### 마이그레이션 경로

- 초기화 선행자를 사용하던 코드는 트레이트 매개변수를 사용하도록 리팩터링 필요
- 트레이트 매개변수는 클래스 정의 시점에서 트레이트 동작을 매개변수화하는 더 깔끔하고 직접적인 방식 제공

---

### 22 제한(Limit 22)

- 배경 — "22 제한"이 무엇이었나
  - Scala 2에는 "함수의 매개변수 개수는 최대 22개, 튜플의 칸도 최대 22개까지"라는 한계가 존재
  - 원인: 내부적으로 `Function1`~`Function22`, `Tuple1`~`Tuple22`처럼 개수별로 타입을 미리 만들어 둔 구조
  - Scala 3: 이 한계를 제거 → **이제 매개변수·필드를 (실용적으로) 몇 개든 사용 가능**
  - 23개짜리 튜플을 쓸 일은 거의 없지만 "더는 22에서 막히지 않는다" 정도만 파악하면 됨

- 함수 타입(function types)의 최대 매개변수 개수 22 제한, 튜플 타입(tuple types)의 최대 필드 개수 22 제한이 삭제됨

- **함수(Functions):** 함수는 이제 임의의 개수의 매개변수를 가질 수 있음
  - `scala.Function22`를 넘어서는 함수는 새로운 트레이트인 `scala.runtime.FunctionXXL`로 소거(erase)됨

- **튜플(Tuples):** 튜플 또한 임의의 개수의 필드를 가질 수 있음
  - `scala.Tuple22`를 넘어서는 튜플은 새로운 클래스인 `scala.runtime.TupleXXL`(트레이트 `scala.Product`를 확장함)로 소거됨
  - 연결(concatenation)·인덱싱(indexing) 같은 제네릭 연산(generic operations)도 지원

- 두 구현 모두 내부적으로 배열(arrays)을 메커니즘으로 사용

#### 요약

- Scala 3는 22개를 초과하는 매개변수·필드를 위한 전용 런타임 타입(runtime types)을 도입해 기존의 22-매개변수 제약을 제거
- 배열 기반 구현으로 호환성을 유지하면서 실용적으로 어떤 크기의 함수·튜플이든 사용 가능

---

### 자동 적용(Auto-Application)

- 배경 — 자동 적용이 무엇이었나
  - 빈 괄호로 정의한 메서드 `def next(): T`를 호출할 때 괄호를 빼고 `next`라고만 써도 Scala 2가 알아서 `next()`로 채워 줌 = "자동 적용"
  - Scala 3: 이 자동 채움을 제거 → **정의에 괄호가 있으면 호출할 때도 괄호(`next()`)를 붙여야 함**

- Scala 3는 무항(nullary) 메서드를 인자 없이 호출할 때 빈 인자 목록 `()`을 자동으로 삽입하던 동작을 제거
- 이전: `next`가 자동으로 `next()`로 확장됨
- Scala 3: 정의의 매개변수 구문과 일치하는 명시적 적용(application) 구문 요구

- 짚고 넘어가기 — "무항"과 "매개변수 없음"은 다름
  - **무항 메서드(nullary)**: 괄호가 있는 `def next(): T` → 호출할 때 `next()`
  - **매개변수 없는 메서드(parameterless)**: 괄호조차 없는 `def size: T` → 호출할 때 `size`
  - Scala 3에서는 이 둘이 서로 다른 것으로 취급 → 섞어서 오버라이드 불가
  - 정의한 모양 그대로 호출하면 됨
  - 관례: "값을 읽기만 하는 것"은 괄호 없이(`size`), "어떤 동작·부수효과를 일으키는 것"은 괄호를 붙여(`next()`) 정의

- 변경 내용. 이전:

```scala
def next(): T = ...
next     // 암묵적으로 next()로 확장됨
```

- Scala 3에서는:

```scala
next
^
missing arguments for method next
```

- 예외
  - 자바(Java) 메서드 안에서 정의되거나 자바 메서드를 오버라이드하는 메서드는 자동 `()` 삽입을 그대로 유지
  - 목적: 균등 접근 원칙(uniform access principle) 보존 → 다음 코드

```scala
xs.toString().length()
```

- 을 아래처럼 관용적인(idiomatic) Scala 코드로 작성 가능

```scala
xs.toString.length
```

- 하위 호환성 처리: Scala 3는 Scala 2에서 정의되었거나 Scala 2 메서드를 오버라이드하는 무항 메서드에 대해 자동 삽입을 일시적으로 허용 → 라이브러리 간 불일치 수용

- 오버라이딩 규칙
  - 메서드 오버라이드(override)에는 더 엄격한 적합성(conformance) 적용
  - 매개변수 없는 메서드(parameterless method)는 무항 메서드(nullary method)로 오버라이드될 수 없고, 그 반대도 마찬가지 → 둘은 정확히 일치해야 함

```scala
class A:
  def next(): Int

class B extends A:
  def next: Int // error: incompatible type
```

- 자바 및 Scala 2 메서드 오버라이드는 여전히 이 규칙에서 면제

- 마이그레이션: 기존 코드는 `-source 3.0-migration` 옵션 하에서 컴파일 가능 → `-rewrite`와 함께 사용하면 Scala 3의 더 엄격한 검사를 준수하도록 자동 리팩터링

---

### 비지역 반환(Nonlocal Returns)

- 참고: 이 기능은 삭제(dropped)가 아니라 더 이상 권장되지 않는(deprecated) 상태

- 배경 — 비지역 반환이 무엇이었나
  - 보통 `return`은 자기가 속한 함수 하나만 빠져나옴
  - "비지역 반환": 그 안쪽의 다른 함수(예: `xs.foreach { x => ... return ... }`의 `{ ... }`) 안에서 `return`을 써서 **한 단계 더 바깥 함수까지 한 번에 탈출**하던 동작
  - 편해 보이지만 내부적으로 예외를 던졌다 잡는 방식이라 느리고 위험 (아래 설명)
  - **대신 `scala.util.boundary` / `break`를 사용** → 같은 일을 더 안전하고 명확하게 처리 가능

- 중첩된 익명 함수(nested anonymous functions)로부터의 반환(return)은 Scala 3.2.0부터 더 이상 권장되지 않음(deprecated)

- 비지역 반환(nonlocal returns)의 구현 방식: `scala.runtime.NonLocalReturnException`을 던지고(throw) 잡는(catch) 방식
  - 문제 1: 프로그래머가 의도한 바와 일치하지 않는 경우가 많음
  - 문제 2: 예외(exception)를 던지고 잡는 데 드는 숨겨진 성능 비용(hidden performance cost)
  - 문제 3: 누수가 있는(leaky) 구현 → 모든 예외를 잡는 핸들러(catch-all exception handler)가 `NonLocalReturnException`을 가로챌 수 있음

- 비지역 반환과 `scala.util.control.Breaks` API에 대한 더 나은 대안: [`scala.util.boundary`와 `boundary.break`](https://nightly.scala-lang.org/api/scala/util/boundary$.html)

- 예:

```scala
import scala.util.boundary, boundary.break
def firstIndex[T](xs: List[T], elem: T): Int =
  boundary:
    for (x, i) <- xs.zipWithIndex do
      if x == elem then break(i)
    -1
```

#### 요약

- 비지역 반환은 예외 던지기 메커니즘에 의존 → 숨겨진 성능 비용 발생 + 모든 예외를 잡는 핸들러를 통한 취약성(vulnerability) 존재
- 중첩된 문맥(nested contexts)에서 더 깔끔하고 효율적인 제어 흐름이 필요하면 `scala.util.boundary` API 사용 권장

---

### private[this]와 protected[this]

- 배경 — `private[this]`가 무엇이었나
  - `private` = "같은 클래스 안에서만 접근 가능"
  - `private[this]` = 거기서 한 발 더 나아가 **"바로 이 인스턴스(this)에서만 접근 가능"**(다른 인스턴스끼리도 못 봄)이라는 더 좁은 한정자 → 주로 성능·변성 검사 같은 세밀한 이유로 사용
  - Scala 3: **컴파일러가 "이 `private` 멤버는 사실 `this`로만 쓰이는구나"를 자동 추론** → `[this]`를 일부러 붙일 필요가 거의 없어짐 → 그냥 `private`만 쓰면 됨

- `private[this]`와 `protected[this]` 접근 한정자(access modifiers)는 더 이상 권장되지 않으며(deprecated), 단계적으로 폐지 예정

- 이전에 이 한정자들이 필요했던 용도
  - 게터(getter)·세터(setter)의 생성을 피하기 위해
  - `private[this]` 하위의 코드를 가변성 검사(variance checks)에서 제외하기 위해 (Scala 2는 `protected[this]`도 제외했으나 불건전(unsound)한 것으로 판정되어 제거됨)
  - `private[this] val`이 클래스의 메서드에서 접근되지 않을 경우 필드(field) 생성을 피하기 위해

- 이제 컴파일러는 `private` 멤버가 오직 `this`를 통해서만 접근된다는 사실을 추론
- 그러한 멤버는 마치 `private[this]`로 선언된 것처럼 취급됨
- `protected[this]`는 대체 수단 없이 삭제됨

- 주의: 이 변경은 경우에 따라 Scala 프로그램의 동작(semantics)을 바꿀 수 있음
  - `private val`이 더 이상 항상 필드를 생성한다고 보장되지 않음
  - 필드가 생략되는 조건
    - `val`이 오직 `this`를 통해서만 접근되고
    - `val`이 현재 클래스의 메서드로부터 접근되지 않는 경우

- 영향: 리플렉션(reflection)으로 해당 private 필드에 접근하려 할 때 문제 발생 가능
- 권장 해결책: 해당 필드를 둘러싼 클래스를 한정자(qualifier)로 하는 한정 private(qualified private)으로 선언. 예:

```scala
class C(x: Int):
  private[C] val field = x + 1
    // `field`를 리플렉션을 통해 접근하려면 [C]가 필요함
  val retained = field * field
```

- 클래스 매개변수(class parameters)는 일반적으로 객체-private(object-private)으로 추론됨 → `val` 또는 `var`로 명시적으로 선언된 멤버는 이 규칙에서 면제

- 가변성 검사에서 제외되지 않는 필드 예시

```scala
class C[-T](private val t: T) // error
```

- 위와 대조적으로 제거되지 않는 필드 예시

```scala
class C(private val c: Int)
```

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Dropped Features](https://docs.scala-lang.org/scala3/reference/dropped-features/)
