# Scala 3 개요와 새로운 기능

---

## 목차

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

## Scala 3 소개

Scala 3는 Scala 언어 전반을 완전히 새롭게 정비한(complete overhaul) 버전입니다. Scala 3는 언어의 타입 시스템(type system)에 대한 근본적인 개선과 함께, 새롭고 풍부한 표현력을 가진 기능들을 다수 도입했습니다.

Scala 3는 다음과 같은 여러 측면에서 변화를 가져왔습니다.

- 더 깔끔하고 일관된 **문법(syntax)** 개선
- 더 강력하고 안전해진 **타입 시스템(type system)**
- "implicit"을 여러 의도별 기능으로 분리한 **문맥적 추상화(contextual abstractions)**
- 현대적인 패턴을 지원하는 **객체지향 프로그래밍(object-oriented programming)** 향상
- 원칙에 기반한(principled) 새로운 **메타프로그래밍(metaprogramming)** 도구 모음

> 📘 **처음 배우는 분께 — 이 문서는 "지도"입니다**
> 이 문서는 기능들의 이름만 쭉 나열하는 카탈로그라, 처음 보면 "뭐가 뭔지 모르겠다"고 느끼기 쉽습니다.
> 지금은 세부 사항을 다 이해하려 하지 말고, **"Scala 3에 대략 이런 묶음의 기능들이 있구나"** 하는
> 지도만 머릿속에 그리면 충분합니다. 각 기능의 실제 사용법은 02번 이후 문서에서 하나씩 자세히 다룹니다.

> ⚠️ **짚고 넘어가기 — "implicit을 의도별로 분리"란?**
> 위에서 말한 "implicit을 여러 의도별 기능으로 분리"는 이 문서 전체를 관통하는 핵심입니다.
> Scala 2에는 `implicit`이라는 키워드 하나가 **네 가지 전혀 다른 일**(매개변수 자동 채움, 타입 자동 변환,
> 메서드 덧붙이기, 타입 클래스)에 모두 쓰였습니다. 그래서 "이 implicit이 대체 무슨 일을 하는 건지"
> 알기 어려웠죠. Scala 3는 이걸 용도별로 `given` / `using` / `extension` / `Conversion`으로 쪼갰습니다.
> (자세한 배경은 00번 문서 9번 항목 참고)

---

## 새로운 문법(New Syntax)

Scala 3는 여러 가지 문법적 개선(syntactic enhancements)을 도입했습니다. 이는 코드를 더 읽기 쉽고 간결하게 만들어 줍니다.

- **제어 구조(Control structures)**: `if`, `while`, `for` 문에 대해 간결한(quiet) 문법을 제공합니다. 불필요한 괄호 없이 제어 구조를 작성할 수 있습니다.
- **생성자 적용(Creator applications)**: `new` 키워드가 선택적(optional)이 됩니다. 객체를 생성할 때 `new`를 생략할 수 있습니다.
- **들여쓰기 기반 스타일(Indentation-based style)**: 선택적 중괄호(optional braces)를 통해 `{ }` 없이도 코드를 작성할 수 있어, 집중력을 방해하지 않는(distraction-free) 프로그래밍이 가능합니다.
- **타입 와일드카드(Type wildcards)**: 타입 와일드카드 표기가 `_`에서 `?`로 변경되었습니다.
- **implicit 재설계(Implicits redesign)**: implicit과 관련된 문법과 접근 방식이 대대적으로 개정되었습니다.

이러한 문법적 변화들은 Scala 3 코드를 더 명확하고 표현력 있게 만드는 것을 목표로 합니다.

---

## 새로운 타입 시스템 기능(New Type System Features)

Scala 3는 타입 시스템에 다음과 같은 주목할 만한 기능들을 추가했습니다.

> 📘 **처음 배우는 분께 — "타입 시스템 기능"이 뭔가요**
> 여기 나오는 것들은 대부분 "더 정교하게 타입을 표현하는 새 방법"입니다. 예를 들어 `A & B`는
> "A이면서 동시에 B인 것", `A | B`는 "A 또는 B 중 하나"를 타입으로 적는 방법입니다.
> 지금은 이름과 한 줄 설명만 훑어도 됩니다. 실제로 자주 쓰게 되는 건 맨 위의 **열거형**(enum)이고,
> 이건 00번 문서의 ADT(대수적 데이터 타입)를 짧게 쓰는 문법이라 가장 먼저 익히면 좋습니다.

- **열거형(Enums)**: 대수적 데이터 타입(algebraic data type, ADT)을 자연스럽게 모델링할 수 있도록 케이스 클래스(case class)와 매끄럽게 어우러지도록 재설계되었습니다.
- **불투명 타입(Opaque types)**: 성능 오버헤드 없이 구현 세부 사항을 감출 수 있습니다.
- **교집합 타입(Intersection types)** (`A & B`): 두 타입을 동시에 만족하는 인스턴스를 표현합니다.
- **합집합 타입(Union types)** (`A | B`): 두 타입 중 하나에 해당하는 인스턴스를 표현합니다.
- **의존 함수 타입(Dependent function types)**: 반환 타입이 특정 인자(argument)에 의존할 수 있는 함수 타입입니다.
- **다형 함수 타입(Polymorphic function types)**: 타입 매개변수(type parameter)를 가지는 메서드에 대해 추상화할 수 있는 함수 타입입니다.
- **타입 람다(Type lambdas)**: 타입 수준의 함수를 일급(first-class) 값으로 다룰 수 있게 합니다.
- **매치 타입(Match types)**: 타입 수준 계산(type-level computation)을 언어 차원에서 직접 지원합니다.

> 📘 **처음 배우는 분께 — "타입 람다", "타입 수준 계산"은 고급 주제입니다**
> "타입 람다", "매치 타입", "타입 수준 계산" 같은 표현은 **값이 아니라 타입을 가지고 계산을 한다**는
> 뜻입니다. 보통 우리가 `1 + 1`처럼 값을 계산하듯, 여기서는 컴파일러가 *타입끼리* 조립·계산을 합니다.
> 라이브러리를 만드는 고급 사용자에게 필요한 기능이라, **처음 배울 때는 "이런 게 있다"만 알고 넘어가도
> 전혀 문제없습니다.** (관련 기초는 00번 문서 7번 "고차 타입" 참고)

---

## 문맥적 추상화(Contextual Abstractions)

> 📘 **처음 배우는 분께 — "문맥적 추상화"가 핵심 단어입니다**
> 제목의 "문맥적 추상화(contextual abstraction)"란, **"코드에 일일이 안 적어도 컴파일러가 주변 문맥에서
> 알맞은 것을 찾아 자동으로 채워주는"** 기능들을 묶어 부르는 말입니다. 바로 Scala 2의 `implicit`이 하던
> 일이고, Scala 3에서 가장 중요하면서도 가장 어려운 부분입니다. 이 절은 그 새 기능들의 이름표라고 보면 됩니다.

Scala 2에서는 `implicit` 하나가 여러 용도로 사용되었습니다. Scala 3는 이를 각각의 의도(intent)에 맞춰 세분화된 언어 기능들로 나누어 제공합니다.

> 💡 **왜 필요한가**
> "implicit 하나로 다 되는데 왜 굳이 나눴나?" 싶을 수 있습니다. 하나로 뭉쳐 있으면 코드를 읽을 때
> "이 implicit이 값을 채우려는 건지, 타입을 바꾸려는 건지, 메서드를 더하려는 건지" 의도가 안 보입니다.
> 이름을 용도별로 나누면(예: 값을 정의할 땐 `given`, 매개변수로 받을 땐 `using`) **코드만 봐도 의도가
> 드러나** 읽기 쉽고 오해가 줄어듭니다.

- **using 절(Using clauses)**: 호출하는 문맥(calling context)에서 사용 가능한 정보에 대해 추상화할 수 있습니다.
- **주어진 인스턴스(Given instances)**: 구현 세부 사항이 외부로 새어 나가지 않으면서, 어떤 타입에 대한 표준적(canonical)인 값을 정의할 수 있습니다.
- **확장 메서드(Extension methods)**: 언어에 직접 내장되어 더 나은 오류 메시지를 제공합니다.
- **암시적 변환(Implicit conversions)**: `Conversion` 타입 클래스(type-class)의 인스턴스로 재설계되었습니다.
- **문맥 함수(Context functions)**: 도메인 특화 언어(domain-specific language, DSL)를 가능하게 하는 새로운 기능입니다.
- **컴파일러 제안(Compiler suggestions)**: implicit 해석(resolution)이 실패했을 때 필요한 import를 힌트로 제공합니다.

---

## 객체지향 프로그래밍 향상(Object-Oriented Enhancements)

Scala 3는 현대적인 객체지향(OOP) 패턴을 지원합니다.

> 📘 **처음 배우는 분께 — 여기서 말하는 "트레이트"**
> 아래 "트레이트 매개변수", "투명 트레이트"의 **트레이트**(trait)는 Java의 인터페이스와 비슷하되
> 메서드 구현과 필드까지 가질 수 있는, 여러 개를 섞어 쓸 수 있는 구성 요소입니다. (00번 문서 2번 참고)
> "트레이트 매개변수"는 그 trait가 클래스처럼 생성 시점에 값을 받을 수 있게 된 것을 말합니다.

- **트레이트 매개변수(Trait parameters)**: 트레이트(trait)도 클래스처럼 매개변수를 받을 수 있습니다.
- **open 클래스(Open classes)**: 확장(extension)을 의도한 클래스는 명시적으로 `open`으로 표시해야 합니다.
- **투명 트레이트(Transparent traits)**: 추론된(inferred) 타입에서 구현 세부 사항을 감춥니다.
- **export 절(Export clauses)**: 객체 멤버에 대한 별칭(alias)을 만들어, 상속보다 합성(composition)을 강조합니다.
- **명시적 null(Explicit null)**: `null`을 타입 계층 구조 밖으로 옮기는 선택적(optional) 기능입니다.
- **안전한 초기화(Safe initialization)**: 초기화되지 않은(uninitialized) 객체에 대한 접근을 탐지합니다.

---

## 메타프로그래밍(Metaprogramming)

Scala 3는 메타프로그래밍을 위한 강력한 도구들의 모음(arsenal)을 갖추고 있습니다.

> 📘 **처음 배우는 분께 — "메타프로그래밍"이란**
> 메타프로그래밍은 **"코드를 다루는 코드"**, 즉 컴파일하는 동안(compile-time) 코드를 들여다보거나
> 새로 만들어내는 기법입니다. 보통 반복되는 코드를 자동 생성하거나 성능을 끌어올릴 때 씁니다.
> Scala에서 가장 고급 주제이니, 처음에는 **"있다는 것만" 알고 넘어가도 됩니다.** (00번 문서의 추천
> 학습 순서에서도 맨 마지막입니다.)

- **inline**: 값과 메서드를 컴파일 시점(compile-time)에 축약(reduction)합니다.
- **컴파일타임 연산(Compiletime operations)**: `scala.compiletime`을 통해 추가적인 기능을 제공합니다.
- **준-인용(Quasi-quotation)**: 코드를 구성하기 위한 고수준(high-level) 인터페이스입니다 (예: `'{ 1 + 1 }`).
- **리플렉션 API(Reflection API)**: `quotes.reflect`를 통해 더 고급의 제어(advanced control)를 가능하게 합니다.

---

## Reference 개요: Scala 3의 설계 목표

Scala 3 Reference는 Scala 2로부터의 중요한 언어 변경 사항들을 문서화합니다. 이 재설계(redesign)는 세 가지 핵심 목표를 추구했습니다.

> 📘 **처음 배우는 분께 — "DOT 계산법"은 몰라도 됩니다**
> DOT 계산법(DOT calculus)은 "Scala 같은 언어가 타입 측면에서 모순 없이 잘 작동하는가"를
> 수학적으로 증명하기 위해 만든 **학술적인 작은 이론 모형**입니다. 쉽게 말해 Scala 3의 타입 시스템이
> "**이론적으로 탄탄한 기초 위에 세워졌다**"는 뜻입니다. 언어를 *쓰는* 입장에서는 이 단어를 몰라도
> 전혀 지장이 없으니, "Scala 3는 그냥 만든 게 아니라 검증된 토대가 있구나" 정도로만 받아들이면 됩니다.

1. **이론적 기반의 강화**: DOT 계산법(DOT calculus)에 관한 기초 연구와의 호환성을 확립하여 Scala의 이론적 기반(theoretical foundations)을 강화합니다.
2. **사용의 용이성과 안전성 향상**: implicit과 같은 강력한 기능들을 더 다루기 쉽게(taming) 만들어, 언어를 더 쉽고 안전하게 사용할 수 있도록 합니다. 즉, 더 완만한 학습 곡선(gentler learning curve)을 제공합니다.
3. **일관성과 표현력 향상**: 여러 언어 구성 요소에 걸친 불일치(inconsistencies)를 제거하고 실용적인 유용성(practical utility)을 높여, 일관성과 표현력을 향상시킵니다.

---

## 핵심 기반(Essential Foundations)

다음 구성 요소들은 DOT의 핵심 기능과 고차 타입(higher-kinded types)을 직접적으로 모델링합니다.

> 📘 **처음 배우는 분께 — "고차 타입"이란**
> `List`는 그 자체로는 타입이 아니라 `List[Int]`처럼 **타입을 하나 받아야 비로소 타입이 되는 것**입니다.
> 이렇게 "타입을 받아 타입을 만드는 것"을 다루는 능력을 고차 타입(higher-kinded type)이라 합니다.
> 여러 컨테이너(List, Option 등)에 공통으로 적용되는 추상 코드를 짤 때 쓰이며, 라이브러리 작성자에게
> 주로 필요합니다. (더 자세히는 00번 문서 7번 참고)

- **교집합 타입(Intersection Types)**: 기존의 합성 타입(compound types)을 대체합니다.
- **합집합 타입(Union Types)**: 두 타입 중 하나를 표현하는 타입입니다.
- **타입 람다(Type Lambdas)**: 구조적 타입(structural type) 및 프로젝션(projection) 인코딩을 대체합니다.
- **문맥 함수(Context Functions)**: 주어진 매개변수(given parameter)에 대해 추상화합니다.

---

## 단순화(Simplifications)

다음 개선들은 기존 구성 요소들을 더 안전하고 일관성 있게(uniform) 대체합니다.

- **트레이트 매개변수(Trait Parameters)**: 조기 초기화자(early initializers)를 대체합니다.
- **주어진 인스턴스(Given Instances)**: implicit 객체(implicit object)와 implicit def를 대체합니다.
- **using 절(Using Clauses)**: 암시적 매개변수(implicit parameters)를 대체합니다.
- **확장 메서드(Extension Methods)**: 암시적 클래스(implicit classes)를 대체합니다.
- **불투명 타입 별칭(Opaque Type Aliases)**: 대부분의 값 클래스(value classes)를 대체합니다.
- **최상위 정의(Top-level Definitions)**: 패키지 객체(package objects)를 대체합니다.
- **export 절(Export Clauses)**: 일반적인 집합(aggregation) 기능을 제공합니다.
- **가변 인자 스플라이스(Vararg Splices)**: `xs*` 문법을 사용합니다.
- **범용 apply 메서드(Universal Apply Methods)**: `new` 대신 함수 호출 문법(function call syntax)을 사용합니다.

---

## 제약(Restrictions)

다음 구성 요소들은 언어의 안전성을 높이기 위해 제한되었습니다.

- **암시적 변환(Implicit Conversions)**: 단일 정의 방식(single definition method)으로 제한되며, 언어 import가 필요합니다.
- **주어진 import(Given Imports)**: 특별한 import 형태(special import form)를 요구합니다.
- **타입 프로젝션(Type Projection)**: 접두 부분(prefix)이 클래스(class)로만 제한됩니다.
- **다세계 동등성(Multiversal Equality)**: 옵트인(opt-in) 방식으로, 의미 없는(nonsensical) 비교를 방지합니다.
- **infix**: 균일한 메서드 적용 문법(uniform method application syntax)을 강제합니다.

---

## 제거된 구성 요소(Dropped Constructs)

다음 기능들은 언어를 단순화하기 위해 제거되었습니다.

- **DelayedInit**
- **존재 타입(Existential Types)**
- **프로시저 문법(Procedure Syntax)**
- **클래스 섀도잉(Class Shadowing)**
- **XML 리터럴(XML Literals)**
- **심볼 리터럴(Symbol Literals)**
- **자동 적용(Auto Application)**
- **약한 적합성(Weak Conformance)**
- **합성 타입(Compound Types)**
- **자동 튜플링(Auto Tupling)**

---

## 기존 구성 요소의 변경(Changes to Existing Constructs)

다음 구성 요소들은 일관성과 유용성을 위해 수정되었습니다.

- **구조적 타입(Structural Types)**: 플러그형 구현(pluggable implementations)을 지원하도록 변경되었습니다.
- **이름 기반 패턴 매칭(Name-based Pattern Matching)**: 그 구현이 명문화(codified)되었습니다.
- **자동 에타 확장(Automatic Eta Expansion)**: 범용 적용(universal application)이 가능하도록 변경되었습니다.
- **암시적 해석(Implicit Resolution)**: 규칙이 간소화(streamlined)되고 스코프(scope)가 제한되었습니다.

---

## 새로운 언어 구성 요소(New Language Constructs)

다음은 언어의 강력함과 사용성을 높이기 위해 추가된 구성 요소들입니다.

- **열거형(Enums)**: 열거(enumeration)와 대수적 데이터 타입(algebraic data type)을 위한 간결한 문법을 제공합니다.
- **매개변수 언튜플링(Parameter Untupling)**: 튜플을 수동으로 분해(destructuring)하는 것을 피할 수 있게 합니다.
- **의존 함수 타입(Dependent Function Types)**: 의존 메서드(dependent methods)를 일반화합니다.
- **다형 함수 타입(Polymorphic Function Types)**: 다형 메서드(polymorphic methods)를 확장합니다.
- **종류 다형성(Kind Polymorphism)**: 타입(types)과 타입 생성자(type constructors)에 대한 연산자를 제공합니다.
- **@targetName 어노테이션(@targetName Annotations)**: 상호 운용성(interoperability)을 제공하고 이름 충돌(name clash)을 피하게 합니다.

---

## 메타프로그래밍 프레임워크(Metaprogramming Framework)

Scala 2의 실험적(experimental) 매크로를 대체하는, 원칙에 기반한(principled) 접근 방식입니다.

- **매치 타입(Match Types)**: 타입 수준 계산(type-level computation)을 수행합니다.
- **inline**: 단순한 매크로 구현(simple macro implementation)을 가능하게 합니다.
- **인용과 스플라이스(Quotes and Splices)**: 원칙에 기반한 매크로 및 스테이징(staging) 표현을 제공합니다.
- **타입 클래스 도출(Type Class Derivation)**: 매크로의 견고한(robust) 언어 내(in-language) 대안입니다.

> 📘 **처음 배우는 분께 — "타입 클래스 도출"이란**
> 타입 클래스(00번 문서 10번)는 "기존 타입에 나중에 기능을 덧붙이는" 패턴인데, 타입마다 그 구현을
> 손으로 일일이 써야 했습니다. "도출(derivation)"은 그 구현을 **컴파일러가 자동으로 만들어 주는 것**을
> 말합니다. 예컨대 `case class`의 필드 구성만 보고 "JSON으로 바꾸는 방법"을 컴파일러가 알아서
> 생성해 주는 식입니다.
- **이름에 의한 문맥 매개변수(By-name Context Parameters)**: 지연 평가(lazy evaluation) 패턴을 대체합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala 3 Reference](https://docs.scala-lang.org/scala3/reference/)
