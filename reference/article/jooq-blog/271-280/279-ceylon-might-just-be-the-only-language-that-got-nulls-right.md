# Ceylon이 NULL을 올바르게 처리한 유일한 (JVM) 언어일 수 있다

> 원문: https://blog.jooq.org/ceylon-might-just-be-the-only-language-that-got-nulls-right/

지난 몇 년간 null에 대해 많이 논의되어 왔습니다. Tony Hoare는 null을 발명한 것이 [10억 달러짜리 실수](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare)라고 언급했습니다. Java 8에서는 이제 (거의) [쓸모없는 Optional](https://blog.jooq.org/java-8-optional-use-cases/)을 도입했습니다. 함수형 프로그래밍은 null을 피하고 모나딕 개념으로 대체하려고 합니다.

하지만 이것들 중 어느 것도 "올바른" 방법이 아닙니다.

## Ceylon의 접근법: Union Types

Ceylon은 문제를 다른 방식으로 해결합니다. Ceylon에서 `Null`은 Java의 `Void`와 유사한 특별한 타입입니다. 이것은 단일 값 `null`만 포함하는 타입입니다. 차이점은 `null`은 다른 어떤 타입에도 할당될 수 없다는 것입니다.

즉, 아래와 같이 작성할 수 없습니다:

```ceylon
String firstName = null; // 컴파일 에러!
```

nullable 타입을 원한다면 union type을 사용해야 합니다:

```ceylon
String? middleName = null; // OK!
```

`String?` 표기법은 `String|Null`의 문법적 설탕(syntactic sugar)입니다. 이것은 `String` 타입이거나 `Null` 타입인 union type입니다. 이것이 핵심 혁신입니다.

전통적인 접근법들은 부재 값, 알 수 없는 값, 초기화되지 않은 값 등 여러 개념을 단일 `null` 개념으로 혼동합니다. Ceylon은 이를 명확하게 구분합니다.

## Optional 모나드의 문제점

`Optional`이나 다른 모나드를 사용하는 접근법은 연쇄 작업이 필요할 때 지나치게 장황하고 오류가 발생하기 쉽습니다. `map()`과 `flatMap()`을 올바르게 선택하는 인지적 부담이 있습니다.

예를 들어, 다음과 같은 체이닝이 필요할 때:

```java
// Optional을 사용한 Java 방식
Optional<String> name = Optional.ofNullable(bob)
    .flatMap(b -> Optional.ofNullable(b.getDepartment()))
    .flatMap(d -> Optional.ofNullable(d.getHead()))
    .map(h -> h.getName());
```

반면 Ceylon/Kotlin 스타일에서는:

```ceylon
// Ceylon의 null-safe 연산자
String? name = bob?.department?.head?.name;
```

훨씬 직관적으로 읽히며, 중첩된 map/flatMap 호출이 필요하지 않습니다.

## Null-Safe 연산자

Ceylon은 nullable 값에 안전하게 접근하기 위한 직관적인 연산자를 제공합니다:

```ceylon
Integer? length = middleName?.length;
Integer length = middleName?.length else 0;
```

`?.` 연산자는 값이 null이 아닐 때만 멤버에 접근하고, `else`는 기본값을 제공합니다.

## Union Types의 확장성

union type의 진정한 힘은 확장성에 있습니다. 여러 특별한 상태를 구분해야 한다면, Optional 패턴을 강제로 도입하지 않고도 이를 정확하게 모델링할 수 있습니다:

```ceylon
String|Unknown|Uninitialized|Undefined value;
```

이것은 실제로 가능한 값이 무엇인지에 대해 더 큰 의미적 명확성을 제공합니다. SQL에서 NULL의 의미론적 뉘앙스(알 수 없음 vs 존재하지 않음)를 Java의 단일 `null`이 혼동하는 문제를 해결합니다.

## Java의 Union Types

흥미롭게도 Java는 예외 처리에서만 union types를 지원합니다:

```java
catch (IOException | SQLException e) {
    // e는 둘 중 하나의 타입일 수 있음
}
```

하지만 일반적인 프로그래밍에서는 이 기능을 사용할 수 없습니다. 저자는 이러한 메커니즘이 언어 전체로 확장되어야 한다고 주장합니다.

## 결론

Ceylon의 설계는 nullable 타입이 미래 언어에서 어떻게 작동해야 하는지를 보여줍니다. null의 존재에 대해 실용적인 태도를 유지하면서, 라이브러리 기반 솔루션이 아닌 타입 시스템 통합을 통해 언어 네이티브 null 안전성을 구현합니다. null을 숨기려 하기보다는 타입 시스템에서 명시적으로 만드는 것이 핵심입니다.

Union types를 사용하는 것은 `Optional` 모나드의 복잡성 없이 개발자의 직관에 맞는 문법적 설탕을 제공합니다. Ceylon은 이러한 실용적인 모델을 미래 언어들에게 제시하고 있습니다.
