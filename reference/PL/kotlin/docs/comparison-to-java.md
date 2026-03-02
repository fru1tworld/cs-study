# Java와의 비교

이 페이지는 Kotlin과 Java를 종합적으로 비교하여 각각의 강점과 차이점을 강조합니다.

## Kotlin에서 해결된 Java의 일부 문제점

Kotlin은 여러 가지 일반적인 Java 문제를 해결합니다:

- **Null 참조** - [null 안전성](null-safety.html)을 통해 타입 시스템에서 제어됨
- **Raw 타입** - [Raw 타입 없음](java-interop.html#java-generics-in-kotlin)
- **배열 공변성** - [배열은 불변](arrays.html)
- **함수 타입** - SAM 변환 대신 적절한 [함수 타입](lambdas.html#function-types)
- **제네릭 변성** - 와일드카드 없이 [사용 지점 변성](generics.html#use-site-variance-type-projections)
- **검사 예외** - Kotlin에서는 필수가 아님
- **컬렉션** - [읽기 전용과 가변 컬렉션을 위한 별도 인터페이스](collections-overview.html)

## Java에는 있지만 Kotlin에는 없는 것

- **검사 예외** - Kotlin 설계의 일부가 아님
- **원시 타입** - 명시적으로 사용 불가 (바이트코드에서는 사용됨)
- **정적 멤버** - [컴패니언 객체](object-declarations.html#companion-objects), [최상위 함수](functions.html), [확장 함수](extensions.html#extension-functions), 또는 [@JvmStatic](java-to-kotlin-interop.html#static-methods)으로 대체
- **와일드카드 타입** - [선언 지점 변성](generics.html#declaration-site-variance) 및 [타입 프로젝션](generics.html#type-projections)으로 대체
- **삼항 연산자** - [if 표현식](control-flow.html#if-expression)으로 대체
- **레코드** - Java 16+ 기능으로 Kotlin에 없음
- **패키지-프라이빗 가시성** - 명시적 [가시성 수정자](visibility-modifiers.html)로 대체

*참고: Kotlin의 [스마트 캐스트](typecasts.html#smart-casts)는 Java의 [패턴 매칭](https://openjdk.org/projects/amber/design-notes/patterns/pattern-matching-for-java)과 유사한 기능을 제공합니다.*

## Kotlin에는 있지만 Java에는 없는 것

### 함수형 기능:

- [람다 표현식](lambdas.html) + [인라인 함수](inline-functions.html)
- [확장 함수](extensions.html)
- [최상위 함수](functions.html)
- [중위 함수](functions.html#infix-notation)

### 타입 시스템 및 안전성:

- [Null 안전성](null-safety.html)
- 변수 및 프로퍼티 타입에 대한 [타입 추론](types-overview.html)
- [선언 지점 변성 및 타입 프로젝션](generics.html)
- [스마트 캐스트](typecasts.html#smart-casts)

### 편의 기능:

- [문자열 템플릿](strings.html)
- [프로퍼티](properties.html)
- [주 생성자](classes.html)
- [데이터 클래스](data-classes.html)
- [기본 값이 있는 매개변수](functions.html#parameters-with-default-values)
- [명명된 매개변수](functions.html#named-arguments)
- [범위 표현식](ranges.html)

### 고급 기능:

- [코루틴](coroutines-overview.html)
- [일급 위임](delegation.html)
- [객체 선언을 통한 싱글톤](object-declarations.html)
- [컴패니언 객체](classes.html#companion-objects)
- [연산자 오버로딩](operator-overloading.html)
- [Expect 및 actual 선언](/docs/multiplatform/multiplatform-expect-actual.html)
- [라이브러리 작성자를 위한 명시적 API 모드](whatsnew14.html#explicit-api-mode-for-library-authors)

## 다음 단계

문서에서는 다음을 학습하도록 제안합니다:
- [Java와 Kotlin에서 문자열을 다루는 일반적인 작업](java-to-kotlin-idioms-strings.html)
- [Java와 Kotlin에서 컬렉션을 다루는 일반적인 작업](java-to-kotlin-collections-guide.html)
- [Java와 Kotlin에서 null 가능성 처리](java-to-kotlin-nullability-guide.html)

자세한 내용은 [JetBrains 공식 Kotlin 채널 동영상](https://www.youtube.com/watch?v=yJDoa42X-wQ)을 시청하세요.
