# Scala 3 개요와 새로운 기능

> 이 문서는 Scala 3(3.7.0) 공식 문서의 "New in Scala 3" 및 "Reference Overview" 섹션을 한국어로 번역한 것입니다.
> 원본: https://docs.scala-lang.org/scala3/new-in-scala3.html

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

이 문서에서는 Scala 3에서 새롭게 등장한 기능들을 큰 범주로 나누어 살펴봅니다.

---

## 새로운 문법(New Syntax)

Scala 3는 여러 가지 문법적 개선(syntactic enhancements)을 도입했습니다. 이는 코드를 더 읽기 쉽고 간결하게 만들어 줍니다.

- **제어 구조(Control structures)**: `if`, `while`, `for` 문에 대해 더 조용한(quiet) 문법을 제공합니다. 즉, 불필요한 괄호 없이 더 간결하게 제어 구조를 작성할 수 있습니다.
- **생성자 적용(Creator applications)**: `new` 키워드가 선택적(optional)이 됩니다. 즉, 객체를 생성할 때 `new`를 생략할 수 있습니다.
- **들여쓰기 기반 스타일(Indentation-based style)**: "선택적 중괄호(optional braces)"를 통해 중괄호 `{ }`에 신경 쓰지 않고도 코드를 작성할 수 있어, 방해 없이(distraction-free) 프로그래밍할 수 있습니다.
- **타입 와일드카드(Type wildcards)**: 타입 와일드카드 표기가 `_`에서 `?`로 변경되었습니다.
- **implicit 재설계(Implicits redesign)**: implicit과 관련된 문법과 접근 방식이 대대적으로 개정되었습니다.

이러한 문법적 변화들은 Scala 3 코드를 더 명확하고 표현력 있게 만들어 주는 것을 목표로 합니다.

---

## 새로운 타입 시스템 기능(New Type System Features)

Scala 3는 타입 시스템에 다음과 같은 주목할 만한 기능들을 추가했습니다.

- **열거형(Enums)**: 대수적 데이터 타입(algebraic data type, ADT)을 자연스럽게 모델링할 수 있도록 케이스 클래스(case class)와 매끄럽게 어우러지도록 재설계되었습니다.
- **불투명 타입(Opaque types)**: 성능 오버헤드 없이 구현 세부 사항을 감출 수 있습니다.
- **교집합 타입(Intersection types)** (`A & B`): 두 타입을 동시에 만족하는 인스턴스를 표현합니다.
- **합집합 타입(Union types)** (`A | B`): 두 타입 중 하나에 해당하는 인스턴스를 표현합니다.
- **의존 함수 타입(Dependent function types)**: 반환 타입이 특정 인자(argument)에 의존할 수 있는 함수 타입입니다.
- **다형 함수 타입(Polymorphic function types)**: 타입 매개변수(type parameter)를 가지는 메서드에 대해 추상화할 수 있는 함수 타입입니다.
- **타입 람다(Type lambdas)**: 타입 수준의 함수를 일급(first-class) 값으로 다룰 수 있게 합니다.
- **매치 타입(Match types)**: 타입 수준 계산(type-level computation)을 언어 차원에서 직접 지원합니다.

---

## 문맥적 추상화(Contextual Abstractions)

Scala 2에서는 하나의 강력한 기능인 `implicit`이 여러 용도로 사용되었습니다. Scala 3는 이를 하나의 강력한 기능 대신, 각각의 의도(intent)에 맞춰 세분화된 언어 기능들로 제공합니다.

- **using 절(Using clauses)**: 호출하는 문맥(calling context)에서 사용 가능한 정보에 대해 추상화할 수 있습니다.
- **주어진 인스턴스(Given instances)**: 구현 세부 사항이 외부로 새어 나가지 않으면서, 어떤 타입에 대한 표준적(canonical)인 값을 정의할 수 있습니다.
- **확장 메서드(Extension methods)**: 언어에 직접 내장되어 더 나은 오류 메시지를 제공합니다.
- **암시적 변환(Implicit conversions)**: `Conversion` 타입 클래스(type-class)의 인스턴스로 재설계되었습니다.
- **문맥 함수(Context functions)**: 도메인 특화 언어(domain-specific language, DSL)를 가능하게 하는 새로운 기능입니다.
- **컴파일러 제안(Compiler suggestions)**: implicit 해석(resolution)이 실패할 때 어떤 import가 필요한지 힌트를 제공합니다.

---

## 객체지향 프로그래밍 향상(Object-Oriented Enhancements)

Scala 3는 현대적인 객체지향(OOP) 패턴을 지원합니다.

- **트레이트 매개변수(Trait parameters)**: 트레이트(trait)도 클래스처럼 매개변수를 받을 수 있습니다.
- **open 클래스(Open classes)**: 확장(extension)을 의도한 클래스는 명시적으로 `open`으로 표시해야 합니다.
- **투명 트레이트(Transparent traits)**: 추론된(inferred) 타입에서 구현 세부 사항을 감춥니다.
- **export 절(Export clauses)**: 객체 멤버에 대한 별칭(alias)을 만들어, 상속보다 합성(composition)을 강조합니다.
- **명시적 null(Explicit null)**: `null`을 타입 계층 구조 밖으로 옮기는 선택적(optional) 기능입니다.
- **안전한 초기화(Safe initialization)**: 초기화되지 않은(uninitialized) 객체에 대한 접근을 탐지합니다.

---

## 메타프로그래밍(Metaprogramming)

Scala 3는 메타프로그래밍을 위한 강력한 도구들의 모음(arsenal)을 갖추고 있습니다.

- **inline**: 값과 메서드를 컴파일 시점(compile-time)에 축약(reduction)합니다.
- **컴파일타임 연산(Compiletime operations)**: `scala.compiletime`을 통해 추가적인 기능을 제공합니다.
- **준-인용(Quasi-quotation)**: 코드를 구성하기 위한 고수준(high-level) 인터페이스입니다 (예: `'{ 1 + 1 }`).
- **리플렉션 API(Reflection API)**: `quotes.reflect`를 통해 더 고급의 제어(advanced control)를 가능하게 합니다.

---

## Reference 개요: Scala 3의 설계 목표

Scala 3 Reference는 Scala 2로부터의 중요한 언어 변경 사항들을 문서화합니다. 이 재설계(redesign)는 세 가지 핵심 목표를 추구했습니다.

1. **이론적 기반의 강화**: DOT 계산법(DOT calculus)에 관한 기초 연구와의 호환성을 확립하여 Scala의 이론적 기반(theoretical foundations)을 강화합니다.
2. **사용의 용이성과 안전성 향상**: implicit과 같은 강력한 기능들을 더 다루기 쉽게(taming) 만들어, 언어를 더 쉽고 안전하게 사용할 수 있도록 합니다. 즉, 더 완만한 학습 곡선(gentler learning curve)을 제공합니다.
3. **일관성과 표현력 향상**: 여러 언어 구성 요소에 걸친 불일치(inconsistencies)를 제거하고 실용적인 유용성(practical utility)을 높여, 일관성과 표현력을 향상시킵니다.

---

## 핵심 기반(Essential Foundations)

다음 구성 요소들은 DOT의 핵심 기능과 고차 타입(higher-kinded types)을 직접적으로 모델링합니다.

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
- **이름에 의한 문맥 매개변수(By-name Context Parameters)**: 지연 평가(lazy evaluation)를 대체합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala 3 Reference](https://docs.scala-lang.org/scala3/reference/)
