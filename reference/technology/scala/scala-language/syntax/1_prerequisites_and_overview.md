# Scala 핵심 개념 선행 학습과 Scala 3 개요

## 시작하기 전에 — Scala 핵심 개념 선행 학습

---

### 콜아웃(인용 블록) 읽는 법

이후 문서들에는 원문 번역 사이사이에 다음 세 종류의 보충 설명이 라벨 형태로 들어감.

- 참고 — 그 자리에서 알아야 할 개념을 짧게 설명 (이게 무엇인가)
- 왜 필요한가 — 이 기능이 실제로 어떤 문제를 푸는지 설명 (이게 왜 있는가)
- 주의 — 직역이라 흐릿한 부분·오해하기 쉬운 부분을 바로잡음

원문 번역은 그대로이며, 이 라벨 붙은 설명만 추가로 삽입된 것.

---

### 1. Scala의 가장 기본적인 사고방식: "거의 모든 것이 표현식"

Scala에서는 `if`, `match`, 블록 `{ ... }` 같은 구문이 값을 반환하는 표현식(expression)임.
Java의 `if`는 문(statement)이라 값이 없음 → Scala의 `if`는 값을 반환함.

```scala
val x = if (n > 0) "양수" else "음수 또는 0"   // if가 문자열을 돌려줌
```

- `val`: 한 번 정해지면 바뀌지 않는 값(불변). 기본적으로 `val`을 씀
- `var`: 바뀔 수 있는 변수(가변). 되도록 피함
- `def`: 메서드(함수) 정의
- `object`: 인스턴스가 딱 하나뿐인 싱글턴 객체 (Java의 `static` 자리에 대신 쓰임)

함수형 언어의 성격이 강함 → "값을 바꾼다"기보다 "새 값을 만들어 반환한다"는 방식으로 코드를 작성함.

---

### 2. `trait` — 인터페이스 + 믹스인

`trait`는 Java의 `interface`와 비슷하지만 구현(메서드 본문)과 필드도 가질 수 있는 더 강력한 개념임.
하나의 클래스가 여러 trait를 함께 상속받을 수 있음 → "믹스인(mixin)"이라고도 부름.

```scala
trait Greeter:
  def greet(name: String): String = s"Hello, $name"   // 기본 구현이 있음

class PoliteGreeter extends Greeter   // 그대로 물려받음
```

참고 — 이후 문서에서 "트레이트 매개변수(trait parameter)", "투명 트레이트(transparent trait)" 같은 표현이 나오는데, 전부 이 `trait`를 확장한 이야기임.

---

### 3. `case class`와 패턴 매칭 — 데이터를 담고 분해하는 도구

`case class`는 "데이터를 담는 용도"의 클래스를 최소한의 코드로 정의할 수 있게 해줌.
일반 클래스와 달리 다음 기능이 자동으로 제공됨.

- `new` 없이 생성: `Point(1, 2)`
- 값 비교(`==`)가 내용 기준으로 동작
- 보기 좋은 `toString`
- 패턴 매칭으로 분해 가능

```scala
case class Point(x: Int, y: Int)

val p = Point(1, 2)
p match
  case Point(0, 0) => "원점"
  case Point(x, y) => s"($x, $y)"   // x, y로 내용을 꺼냄
```

`match`는 다른 언어의 `switch`보다 훨씬 강력함 → 구조를 분해하면서 분기함.
함수형 Scala 코드의 핵심 도구.

---

### 4. `sealed` — "이 타입의 자식은 여기 있는 게 전부다"

`sealed`를 붙이면 해당 타입의 상속이 같은 파일 내로 제한됨.
→ 컴파일러가 "가능한 모든 경우"를 파악할 수 있음 → `match`에서 빠뜨린 경우가 있으면 경고해 줌.

```scala
sealed trait Shape
case class Circle(r: Double) extends Shape
case class Square(s: Double) extends Shape
// 이 파일 밖에서는 Shape를 더 상속할 수 없음
```

이 "sealed trait + case class들" 조합이 곧 아래 8번의 ADT.
Scala 3의 `enum`은 바로 이 패턴을 짧게 쓰기 위한 문법 → 05번 문서의 핵심 배경.

---

### 5. 컴패니언 객체(companion object)

클래스와 같은 이름으로 같은 파일에 선언한 `object`를 컴패니언 객체라 함.
보통 해당 타입과 관련된 유틸리티·생성 함수를 모아두는 용도로 사용 (Java의 static 멤버 역할).

```scala
class Pizza(val toppings: List[String])

object Pizza:                       // Pizza의 컴패니언 객체
  def cheese = new Pizza(List("cheese"))   // 미리 만들어 둔 인스턴스
```

---

### 6. 제네릭(타입 매개변수)과 변성(variance)

`List[A]`처럼 대괄호로 받는 것이 타입 매개변수 (Java의 `<A>` 제네릭과 같은 개념).

변성(variance)은 "`Dog`가 `Animal`의 하위 타입일 때, `List[Dog]`도 `List[Animal]`의 하위 타입으로 취급할 수 있는가?"에 대한 규칙.

- `List[+A]`: 공변(covariant). `Dog <: Animal`이면 `List[Dog] <: List[Animal]` (`+`)
- `Foo[-A]`: 반공변(contravariant). 방향이 반대 (`-`)
- `Bar[A]`: 무공변(invariant). 아무 관계 없음 (기본값)

05번(변성 고려), 04번(타입) 문서에서 `+`, `-` 기호가 이 뜻으로 나옴.

---

### 7. 고차 타입(higher-kinded types)

`List` 자체는 타입이 아니라 "타입을 받아 타입을 만드는 것"임 (`List[Int]`가 되어야 비로소 완전한 타입).
이처럼 "타입을 생성하는 타입"을 다루는 기능을 고차 타입이라 함.

```scala
trait Functor[F[_]]:   // F는 List, Option 처럼 "타입 하나를 받는 타입 생성자"
  def map[A, B](fa: F[A])(f: A => B): F[B]
```

`F[_]`의 밑줄은 "여기에 타입 하나가 들어갈 자리가 있다"는 뜻.
01번 문서의 "타입 람다", "종류 다형성(kind polymorphism)"이 이 주제의 연장선.

---

### 8. 대수적 데이터 타입(ADT)

ADT는 "데이터를 합(OR)과 곱(AND)으로 조합하는 방식".

- 곱(product): 여러 값을 동시에 가짐 → `case class Point(x: Int, y: Int)` (x 그리고 y)
- 합(sum): 여러 모양 중 하나 → `sealed trait Shape` 아래 `Circle` 또는 `Square`

```scala
sealed trait Tree
case class Leaf(value: Int) extends Tree
case class Branch(left: Tree, right: Tree) extends Tree
```

"정해진 몇 가지 경우로만 구성된 데이터"를 표현하는 것이 ADT.
`match`로 모든 경우를 안전하게 처리할 수 있음. 05번 문서의 핵심 주제.

---

### 9. `implicit` — Scala 2의 "컴파일러가 알아서 채워주는 것" (가장 중요)

`implicit`은 이후 문서(특히 02·03번)를 이해하는 데 가장 중요한 개념.
한 문장으로: "코드에 명시하지 않아도 컴파일러가 스코프 내에서 적절한 값을 찾아 자동으로 채워 넣는 장치".

Scala 2에서 `implicit` 키워드는 네 가지 서로 다른 용도로 쓰임 (Scala 3는 이를 의도별로 분리 → 02·03번 문서).

(1) 암시적 매개변수(implicit parameter) — 인자를 생략하면 컴파일러가 채워줌

```scala
def greet(implicit name: String): String = s"Hi, $name"

implicit val me: String = "Kim"   // 범위 안에 implicit 값을 둠
greet   // name을 안 넘겨도 me가 자동으로 들어감 → "Hi, Kim"
```

→ Scala 3에서는 `using`(매개변수)과 `given`(값)으로 분리 (02번 문서).

(2) 암시적 변환(implicit conversion) — 타입을 자동으로 바꿔줌

```scala
implicit def intToStr(n: Int): String = n.toString
val s: String = 42   // Int인데 String 자리에 들어감 → 자동 변환됨
```

→ 지나치게 암묵적이라 위험할 수 있음. Scala 3에서는 `Conversion` 타입으로 명시화 (10번 문서).

(3) 암시적 클래스(implicit class) — 기존 타입에 메서드를 덧붙임

```scala
implicit class IntOps(n: Int):
  def squared: Int = n * n
3.squared   // Int에 없던 메서드를 쓴 것처럼 → 9
```

→ Scala 3에서는 `extension`(확장 메서드)으로 대체 (03번 문서).

(4) 타입 클래스 패턴 — 아래 10번 참고 (03번 문서).

주의 — `implicit`은 "편리하지만 어디서 무엇이 끼어들었는지 추적하기 어렵다"는 비판을 받았음.
Scala 3는 이를 `given` / `using` / `extension` / `Conversion`으로 의도별로 분리해 명확하게 만듦.
02번 문서가 처음부터 "implicit 비판 5가지"로 시작하는 이유가 바로 이것.

---

### 10. 타입 클래스(type class) 패턴

타입 클래스는 "이미 만들어진 타입(수정할 수 없는 타입 포함)에 나중에 기능을 추가하는" 방법.
상속과 달리, 타입 자체를 수정하지 않고 외부에서 기능을 부여함.

생각하는 순서.

1. "이런 능력이 필요하다"는 인터페이스를 `trait`로 정의 (예: 비교할 수 있음 `Ord[T]`)
2. 특정 타입(예: `Int`)에 대한 그 능력의 구현을 implicit/given으로 제공
3. 함수는 "그 능력을 가진 타입이면 받겠다"고 요구

```scala
trait Show[A]:                          // 1. "문자열로 보여줄 수 있다"는 능력
  def show(a: A): String

given Show[Int] with                    // 2. Int에 대한 구현 (Scala 3 문법)
  def show(a: Int) = s"Int($a)"

def print[A](a: A)(using s: Show[A]) =  // 3. Show 능력이 있는 A만 받음
  println(s.show(a))
```

이 패턴이 03번 문서의 전체 주제. Scala 2에서는 구현을 `implicit val/def`로 제공, Scala 3에서는 `given`으로 제공.

---

### 11. 자잘하지만 문서에 나오는 것들

- value class: 런타임 오버헤드 없이 타입을 한 겹 감싸는 클래스(`extends AnyVal`). Scala 3의 opaque type이 많은 경우 이를 대체 (04·06번)
- package object: 패키지 단위로 공용 정의를 모아두던 곳. Scala 3는 파일 최상위에 최상위 정의(top-level definition)를 바로 선언할 수 있어 거의 불필요해짐 (07번)
- by-name 매개변수(`x: => T`): 인자를 전달 시점이 아니라 사용 시점에 평가함(지연 평가) (02·10번)
- 디슈가링(desugaring): 편의 문법(syntactic sugar)을 컴파일러가 더 기본적인 형태로 변환하는 과정. "이 문법은 사실 이런 코드와 동일하다"는 설명에서 자주 등장 (05·06번)
- 에타 확장(eta expansion): 메서드(`def`)를 함수 값(`f: A => B`)으로 자동 변환하는 것 (10번)

---

### 12. Scala 2 → Scala 3 대응표 (한눈에)

이후 문서를 읽다가 "이게 Scala 2의 무엇을 대체한 건지?" 궁금할 때 참고.

- `implicit val` / `implicit object` → `given` (02)
- `implicit` 매개변수 → `using` 절 (02)
- `implicit class` → `extension`(확장 메서드) (03)
- `implicit def`(변환) → `Conversion` 인스턴스 (02, 10)
- `sealed trait` + `case class`(ADT) → `enum` (05)
- value class(`AnyVal`) → opaque type (04, 06)
- package object → 최상위 정의(top-level) (07)
- 합성 타입 `A with B` → 교집합 타입 `A & B` (04, 10)
- 와일드카드 타입 `Foo[_]` → `Foo[?]` (07, 10)
- `def f(x: T) { ... }`(프로시저 문법) → `def f(x: T): Unit = { ... }` (11)
- scala-reflect 매크로(실험적) → inline + quotes/splices (08, 09)

---

### 추천 학습 순서

처음 학습하는 분이라면 다음 순서를 권함.

1. 이 문서(00) — 기초 개념
2. 01 — Scala 3 전체 조감도 (어떤 게 있는지 지도 보기)
3. 05 — enum/ADT (가장 직관적이고 실용적)
4. 02 → 03 — given/using → 확장 메서드/타입 클래스 (Scala 3의 핵심, 가장 어려움)
5. 04, 06, 07 — 타입/모델링/문법 등 나머지 기능
6. 10, 11, 12 — 바뀐 것 / 사라진 것 / 마이그레이션
7. 08, 09 — 메타프로그래밍 (가장 고급, 나중에)

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala Book (입문자용 튜토리얼)](https://docs.scala-lang.org/scala3/book/introduction.html)
- [Scala 3 Reference](https://docs.scala-lang.org/scala3/reference/)

---

## Scala 3 개요와 새로운 기능

---

### 목차

1. [Scala 3 소개](#scala-3-소개)
2. [새로운 문법(New Syntax)](#새로운-문법new-syntax)
3. [새로운 타입 시스템 기능(New Type System Features)](#새로운-타입-시스템-기능new-type-system-features)
4. [문맥적 추상화(Contextual Abstractions)](#문맥적-추상화contextual-abstractions)
5. [객체지향 프로그래밍 향상(Object-Oriented Enhancements)](#객체지향-프로그래밍-향상object-oriented-enhancements)
6. [메타프로그래밍(Metaprogramming)](#메타프로그래밍metaprogramming)
7. [Reference 개요: Scala 3의 설계 목표](#reference-개요-scala-3의-설계-목표)
8. [핵심 기반(Essential Foundations)](#핵심-기반essential-foundations)
9. [단순화(Simplifications)](#단순화simplifications)
10. [제약(Restrictions)](#제약restrictions)
11. [제거된 구성 요소(Dropped Constructs)](#제거된-구성-요소dropped-constructs)
12. [기존 구성 요소의 변경(Changes to Existing Constructs)](#기존-구성-요소의-변경changes-to-existing-constructs)
13. [새로운 언어 구성 요소(New Language Constructs)](#새로운-언어-구성-요소new-language-constructs)
14. [메타프로그래밍 프레임워크(Metaprogramming Framework)](#메타프로그래밍-프레임워크metaprogramming-framework)
15. [참고 자료](#참고-자료)

---

### Scala 3 소개

Scala 3는 Scala 언어 전반을 완전히 새롭게 정비한(complete overhaul) 버전.
타입 시스템(type system)에 대한 근본적인 개선과 함께, 새롭고 풍부한 표현력을 가진 기능들을 다수 도입함.

Scala 3는 다음과 같은 여러 측면에서 변화를 가져옴.

- 더 깔끔하고 일관된 문법(syntax) 개선
- 더 강력하고 안전해진 타입 시스템(type system)
- "implicit"을 여러 의도별 기능으로 분리한 문맥적 추상화(contextual abstractions)
- 현대적인 패턴을 지원하는 객체지향 프로그래밍(object-oriented programming) 향상
- 원칙에 기반한(principled) 새로운 메타프로그래밍(metaprogramming) 도구 모음

참고 — 이 문서는 "지도"임. 기능들의 이름만 쭉 나열하는 카탈로그라, 처음 보면 "뭐가 뭔지 모르겠다"고 느끼기 쉬움.
지금은 세부 사항을 다 이해하려 하지 말고, "Scala 3에 대략 이런 묶음의 기능들이 있구나" 하는 지도만 머릿속에 그리면 충분함. 각 기능의 실제 사용법은 02번 이후 문서에서 하나씩 자세히 다룸.

주의 — "implicit을 의도별로 분리"란? 위에서 말한 "implicit을 여러 의도별 기능으로 분리"는 이 문서 전체를 관통하는 핵심.
Scala 2에는 `implicit`이라는 키워드 하나가 네 가지 전혀 다른 일(매개변수 자동 채움, 타입 자동 변환, 메서드 덧붙이기, 타입 클래스)에 모두 쓰임 → "이 implicit이 대체 무슨 일을 하는 건지" 알기 어려웠음.
Scala 3는 이걸 용도별로 `given` / `using` / `extension` / `Conversion`으로 쪼갬. (자세한 배경은 00번 문서 9번 항목 참고)

---

### 새로운 문법(New Syntax)

Scala 3는 여러 가지 문법적 개선(syntactic enhancements)을 도입함. 코드를 더 읽기 쉽고 간결하게 만들어 줌.

- 제어 구조(Control structures): `if`, `while`, `for` 문에 대해 간결한(quiet) 문법을 제공. 불필요한 괄호 없이 제어 구조를 작성할 수 있음
- 생성자 적용(Creator applications): `new` 키워드가 선택적(optional)이 됨. 객체를 생성할 때 `new`를 생략할 수 있음
- 들여쓰기 기반 스타일(Indentation-based style): 선택적 중괄호(optional braces)를 통해 `{ }` 없이도 코드를 작성할 수 있음 → 집중력을 방해하지 않는(distraction-free) 프로그래밍이 가능
- 타입 와일드카드(Type wildcards): 타입 와일드카드 표기가 `_`에서 `?`로 변경됨
- implicit 재설계(Implicits redesign): implicit과 관련된 문법과 접근 방식이 대대적으로 개정됨

이러한 문법적 변화들은 Scala 3 코드를 더 명확하고 표현력 있게 만드는 것을 목표로 함.

---

### 새로운 타입 시스템 기능(New Type System Features)

Scala 3는 타입 시스템에 다음과 같은 주목할 만한 기능들을 추가함.

참고 — "타입 시스템 기능"이 뭔가요. 여기 나오는 것들은 대부분 "더 정교하게 타입을 표현하는 새 방법".
예를 들어 `A & B`는 "A이면서 동시에 B인 것", `A | B`는 "A 또는 B 중 하나"를 타입으로 적는 방법.
지금은 이름과 한 줄 설명만 훑어도 됨. 실제로 자주 쓰게 되는 건 맨 위의 열거형(enum)이고, 이건 00번 문서의 ADT(대수적 데이터 타입)를 짧게 쓰는 문법이라 가장 먼저 익히면 좋음.

- 열거형(Enums): 대수적 데이터 타입(algebraic data type, ADT)을 자연스럽게 모델링할 수 있도록 케이스 클래스(case class)와 매끄럽게 어우러지도록 재설계됨
- 불투명 타입(Opaque types): 성능 오버헤드 없이 구현 세부 사항을 감출 수 있음
- 교집합 타입(Intersection types)(`A & B`): 두 타입을 동시에 만족하는 인스턴스를 표현
- 합집합 타입(Union types)(`A | B`): 두 타입 중 하나에 해당하는 인스턴스를 표현
- 의존 함수 타입(Dependent function types): 반환 타입이 특정 인자(argument)에 의존할 수 있는 함수 타입
- 다형 함수 타입(Polymorphic function types): 타입 매개변수(type parameter)를 가지는 메서드에 대해 추상화할 수 있는 함수 타입
- 타입 람다(Type lambdas): 타입 수준의 함수를 일급(first-class) 값으로 다룰 수 있게 함
- 매치 타입(Match types): 타입 수준 계산(type-level computation)을 언어 차원에서 직접 지원

참고 — "타입 람다", "타입 수준 계산"은 고급 주제. "값이 아니라 타입을 가지고 계산을 한다"는 뜻.
보통 `1 + 1`처럼 값을 계산하듯, 여기서는 컴파일러가 타입끼리 조립·계산을 함.
라이브러리를 만드는 고급 사용자에게 필요한 기능이라, 처음 배울 때는 "이런 게 있다"만 알고 넘어가도 전혀 문제없음. (관련 기초는 00번 문서 7번 "고차 타입" 참고)

---

### 문맥적 추상화(Contextual Abstractions)

참고 — "문맥적 추상화"가 핵심 단어. 제목의 "문맥적 추상화(contextual abstraction)"란, "코드에 일일이 안 적어도 컴파일러가 주변 문맥에서 알맞은 것을 찾아 자동으로 채워주는" 기능들을 묶어 부르는 말.
바로 Scala 2의 `implicit`이 하던 일이고, Scala 3에서 가장 중요하면서도 가장 어려운 부분. 이 절은 그 새 기능들의 이름표라고 보면 됨.

Scala 2에서는 `implicit` 하나가 여러 용도로 사용됨. Scala 3는 이를 각각의 의도(intent)에 맞춰 세분화된 언어 기능들로 나누어 제공함.

왜 필요한가 — "implicit 하나로 다 되는데 왜 굳이 나눴나?" 싶을 수 있음.
하나로 뭉쳐 있으면 코드를 읽을 때 "이 implicit이 값을 채우려는 건지, 타입을 바꾸려는 건지, 메서드를 더하려는 건지" 의도가 안 보임.
이름을 용도별로 나누면(예: 값을 정의할 땐 `given`, 매개변수로 받을 땐 `using`) 코드만 봐도 의도가 드러나 읽기 쉽고 오해가 줄어듦.

- using 절(Using clauses): 호출하는 문맥(calling context)에서 사용 가능한 정보에 대해 추상화할 수 있음
- 주어진 인스턴스(Given instances): 구현 세부 사항이 외부로 새어 나가지 않으면서, 어떤 타입에 대한 표준적(canonical)인 값을 정의할 수 있음
- 확장 메서드(Extension methods): 언어에 직접 내장되어 더 나은 오류 메시지를 제공
- 암시적 변환(Implicit conversions): `Conversion` 타입 클래스(type-class)의 인스턴스로 재설계됨
- 문맥 함수(Context functions): 도메인 특화 언어(domain-specific language, DSL)를 가능하게 하는 새로운 기능
- 컴파일러 제안(Compiler suggestions): implicit 해석(resolution)이 실패했을 때 필요한 import를 힌트로 제공

---

### 객체지향 프로그래밍 향상(Object-Oriented Enhancements)

Scala 3는 현대적인 객체지향(OOP) 패턴을 지원함.

참고 — 여기서 말하는 "트레이트". 아래 "트레이트 매개변수", "투명 트레이트"의 트레이트(trait)는 Java의 인터페이스와 비슷하되 메서드 구현과 필드까지 가질 수 있는, 여러 개를 섞어 쓸 수 있는 구성 요소 (00번 문서 2번 참고).
"트레이트 매개변수"는 그 trait가 클래스처럼 생성 시점에 값을 받을 수 있게 된 것을 말함.

- 트레이트 매개변수(Trait parameters): 트레이트(trait)도 클래스처럼 매개변수를 받을 수 있음
- open 클래스(Open classes): 확장(extension)을 의도한 클래스는 명시적으로 `open`으로 표시해야 함
- 투명 트레이트(Transparent traits): 추론된(inferred) 타입에서 구현 세부 사항을 감춤
- export 절(Export clauses): 객체 멤버에 대한 별칭(alias)을 만들어, 상속보다 합성(composition)을 강조
- 명시적 null(Explicit null): `null`을 타입 계층 구조 밖으로 옮기는 선택적(optional) 기능
- 안전한 초기화(Safe initialization): 초기화되지 않은(uninitialized) 객체에 대한 접근을 탐지

---

### 메타프로그래밍(Metaprogramming)

Scala 3는 메타프로그래밍을 위한 강력한 도구들의 모음(arsenal)을 갖춤.

참고 — "메타프로그래밍"이란. "코드를 다루는 코드", 즉 컴파일하는 동안(compile-time) 코드를 들여다보거나 새로 만들어내는 기법.
보통 반복되는 코드를 자동 생성하거나 성능을 끌어올릴 때 씀. Scala에서 가장 고급 주제이니, 처음에는 "있다는 것만" 알고 넘어가도 됨. (00번 문서의 추천 학습 순서에서도 맨 마지막)

- inline: 값과 메서드를 컴파일 시점(compile-time)에 축약(reduction)
- 컴파일타임 연산(Compiletime operations): `scala.compiletime`을 통해 추가적인 기능을 제공
- 준-인용(Quasi-quotation): 코드를 구성하기 위한 고수준(high-level) 인터페이스 (예: `'{ 1 + 1 }`)
- 리플렉션 API(Reflection API): `quotes.reflect`를 통해 더 고급의 제어(advanced control)를 가능하게 함

---

### Reference 개요: Scala 3의 설계 목표

Scala 3 Reference는 Scala 2로부터의 중요한 언어 변경 사항들을 문서화함. 이 재설계(redesign)는 세 가지 핵심 목표를 추구함.

참고 — "DOT 계산법"은 몰라도 됨. DOT 계산법(DOT calculus)은 "Scala 같은 언어가 타입 측면에서 모순 없이 잘 작동하는가"를 수학적으로 증명하기 위해 만든 학술적인 작은 이론 모형.
쉽게 말해 Scala 3의 타입 시스템이 "이론적으로 탄탄한 기초 위에 세워졌다"는 뜻. 언어를 쓰는 입장에서는 이 단어를 몰라도 전혀 지장이 없음 → "Scala 3는 그냥 만든 게 아니라 검증된 토대가 있구나" 정도로만 받아들이면 됨.

1. 이론적 기반의 강화: DOT 계산법(DOT calculus)에 관한 기초 연구와의 호환성을 확립하여 Scala의 이론적 기반(theoretical foundations)을 강화
2. 사용의 용이성과 안전성 향상: implicit과 같은 강력한 기능들을 더 다루기 쉽게(taming) 만들어, 언어를 더 쉽고 안전하게 사용할 수 있도록 함 → 더 완만한 학습 곡선(gentler learning curve)을 제공
3. 일관성과 표현력 향상: 여러 언어 구성 요소에 걸친 불일치(inconsistencies)를 제거하고 실용적인 유용성(practical utility)을 높여, 일관성과 표현력을 향상

---

### 핵심 기반(Essential Foundations)

다음 구성 요소들은 DOT의 핵심 기능과 고차 타입(higher-kinded types)을 직접적으로 모델링함.

참고 — "고차 타입"이란. `List`는 그 자체로는 타입이 아니라 `List[Int]`처럼 타입을 하나 받아야 비로소 타입이 되는 것.
이렇게 "타입을 받아 타입을 만드는 것"을 다루는 능력을 고차 타입(higher-kinded type)이라 함.
여러 컨테이너(List, Option 등)에 공통으로 적용되는 추상 코드를 짤 때 쓰이며, 라이브러리 작성자에게 주로 필요함. (더 자세히는 00번 문서 7번 참고)

- 교집합 타입(Intersection Types): 기존의 합성 타입(compound types)을 대체
- 합집합 타입(Union Types): 두 타입 중 하나를 표현하는 타입
- 타입 람다(Type Lambdas): 구조적 타입(structural type) 및 프로젝션(projection) 인코딩을 대체
- 문맥 함수(Context Functions): 주어진 매개변수(given parameter)에 대해 추상화

---

### 단순화(Simplifications)

다음 개선들은 기존 구성 요소들을 더 안전하고 일관성 있게(uniform) 대체함.

- 트레이트 매개변수(Trait Parameters): 조기 초기화자(early initializers)를 대체
- 주어진 인스턴스(Given Instances): implicit 객체(implicit object)와 implicit def를 대체
- using 절(Using Clauses): 암시적 매개변수(implicit parameters)를 대체
- 확장 메서드(Extension Methods): 암시적 클래스(implicit classes)를 대체
- 불투명 타입 별칭(Opaque Type Aliases): 대부분의 값 클래스(value classes)를 대체
- 최상위 정의(Top-level Definitions): 패키지 객체(package objects)를 대체
- export 절(Export Clauses): 일반적인 집합(aggregation) 기능을 제공
- 가변 인자 스플라이스(Vararg Splices): `xs*` 문법을 사용
- 범용 apply 메서드(Universal Apply Methods): `new` 대신 함수 호출 문법(function call syntax)을 사용

---

### 제약(Restrictions)

다음 구성 요소들은 언어의 안전성을 높이기 위해 제한됨.

- 암시적 변환(Implicit Conversions): 단일 정의 방식(single definition method)으로 제한되며, 언어 import가 필요
- 주어진 import(Given Imports): 특별한 import 형태(special import form)를 요구
- 타입 프로젝션(Type Projection): 접두 부분(prefix)이 클래스(class)로만 제한
- 다세계 동등성(Multiversal Equality): 옵트인(opt-in) 방식으로, 의미 없는(nonsensical) 비교를 방지
- infix: 균일한 메서드 적용 문법(uniform method application syntax)을 강제

---

### 제거된 구성 요소(Dropped Constructs)

다음 기능들은 언어를 단순화하기 위해 제거됨.

- DelayedInit
- 존재 타입(Existential Types)
- 프로시저 문법(Procedure Syntax)
- 클래스 섀도잉(Class Shadowing)
- XML 리터럴(XML Literals)
- 심볼 리터럴(Symbol Literals)
- 자동 적용(Auto Application)
- 약한 적합성(Weak Conformance)
- 합성 타입(Compound Types)
- 자동 튜플링(Auto Tupling)

---

### 기존 구성 요소의 변경(Changes to Existing Constructs)

다음 구성 요소들은 일관성과 유용성을 위해 수정됨.

- 구조적 타입(Structural Types): 플러그형 구현(pluggable implementations)을 지원하도록 변경
- 이름 기반 패턴 매칭(Name-based Pattern Matching): 그 구현이 명문화(codified)됨
- 자동 에타 확장(Automatic Eta Expansion): 범용 적용(universal application)이 가능하도록 변경
- 암시적 해석(Implicit Resolution): 규칙이 간소화(streamlined)되고 스코프(scope)가 제한됨

---

### 새로운 언어 구성 요소(New Language Constructs)

다음은 언어의 강력함과 사용성을 높이기 위해 추가된 구성 요소들.

- 열거형(Enums): 열거(enumeration)와 대수적 데이터 타입(algebraic data type)을 위한 간결한 문법을 제공
- 매개변수 언튜플링(Parameter Untupling): 튜플을 수동으로 분해(destructuring)하는 것을 피할 수 있게 함
- 의존 함수 타입(Dependent Function Types): 의존 메서드(dependent methods)를 일반화
- 다형 함수 타입(Polymorphic Function Types): 다형 메서드(polymorphic methods)를 확장
- 종류 다형성(Kind Polymorphism): 타입(types)과 타입 생성자(type constructors)에 대한 연산자를 제공
- @targetName 어노테이션(@targetName Annotations): 상호 운용성(interoperability)을 제공하고 이름 충돌(name clash)을 피하게 함

---

### 메타프로그래밍 프레임워크(Metaprogramming Framework)

Scala 2의 실험적(experimental) 매크로를 대체하는, 원칙에 기반한(principled) 접근 방식.

- 매치 타입(Match Types): 타입 수준 계산(type-level computation)을 수행
- inline: 단순한 매크로 구현(simple macro implementation)을 가능하게 함
- 인용과 스플라이스(Quotes and Splices): 원칙에 기반한 매크로 및 스테이징(staging) 표현을 제공
- 타입 클래스 도출(Type Class Derivation): 매크로의 견고한(robust) 언어 내(in-language) 대안
- 이름에 의한 문맥 매개변수(By-name Context Parameters): 지연 평가(lazy evaluation) 패턴을 대체

참고 — "타입 클래스 도출"이란. 타입 클래스(00번 문서 10번)는 "기존 타입에 나중에 기능을 덧붙이는" 패턴인데, 타입마다 그 구현을 손으로 일일이 써야 했음.
"도출(derivation)"은 그 구현을 컴파일러가 자동으로 만들어 주는 것을 말함.
예컨대 `case class`의 필드 구성만 보고 "JSON으로 바꾸는 방법"을 컴파일러가 알아서 생성해 주는 식.

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala 3 Reference](https://docs.scala-lang.org/scala3/reference/)
