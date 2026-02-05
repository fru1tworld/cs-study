# Java 8 금요일: Optional은 Java에서 선택 사항으로 남을 것이다

> 원문: https://blog.jooq.org/java-8-friday-optional-will-remain-an-option-in-java/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 우리는 jOOQ의 멋진 Java 8 통합에 대해 정말 흥분하고 있습니다. 우리는 몇 주 전에 Java 8 금요일 시리즈를 시작했습니다. 매주 금요일마다 몇 가지 새로운 튜토리얼 스타일의 Java 8 기능을 보여드리고 있습니다.

## Null: 10억 달러짜리 실수

Java에 대해 말하자면, Java에서는 null이 우리의 운명입니다. 아무리 많이 피하려 해도, null은 항상 따라옵니다. Tony Hoare가 null 참조를 "10억 달러짜리 실수"라고 불렀던 것으로 유명하지만, Java는 이 문제를 완전히 해결할 수 없습니다.

Ceylon이나 Kotlin과 같은 다른 JVM 언어들은 nullable 타입 구문(타입 이름에 `?`를 추가)을 흐름 감지 타이핑(flow-sensitive typing)과 결합하여 NullPointerException을 방지하는 솔루션을 제공합니다. 하지만 Java는 하위 호환성을 유지해야 하기 때문에 이러한 접근 방식을 채택하기 어렵습니다.

## Java 8의 Optional 타입

Java 8은 `Optional` 타입을 도입했습니다. `OptionalInt`, `OptionalLong`, `OptionalDouble` 변형도 함께 도입되었습니다. 주된 목적은 객체를 래핑하여 잠재적인 null 가능성을 처리하기 위한 유창한(fluent) API를 제공하는 것입니다.

```java
Optional<String> stringOrNot = Optional.of("123");

// Optional 값이 없으면 빈 문자열을 반환
String alwaysAString = stringOrNot.orElse("");

// Optional 값을 Integer로 매핑
Optional<Integer> integerOrNot = stringOrNot.map(Integer::parseInt);

// 체이닝하여 사용
int alwaysAnInt = stringOrNot
    .map(s -> Integer.parseInt(s))
    .orElse(0);
```

## Streams API와의 통합

Optional 타입은 Java 8의 Streams API와 효과적으로 작동합니다:

```java
Arrays.asList(1, 2, 3)
      .stream()
      .findAny()
      .ifPresent(System.out::println);
```

이 예제에서 `findAny()`는 `Optional<Integer>`를 반환하고, `ifPresent()`는 값이 존재할 때만 람다를 실행합니다.

## Optional의 핵심적인 한계

### 하위 호환성 문제

기존 JDK API는 `Optional`로 개조되지 않았습니다. Scala와 달리, Java는 표준 라이브러리 전체에서 `Optional`을 사용하지 않습니다. 주로 Streams API에서만 나타납니다.

이것은 `Optional`의 효과를 제한합니다. 오래된 API들은 여전히 null을 반환할 수 있고, 새로운 Optional 기반 코드와 일관되게 사용하기 어렵게 만듭니다.

### 제네릭의 복잡성

`Optional`은 Java의 제네릭 타입 시스템 및 타입 소거(type erasure)와 문제적인 상호작용을 만들어냅니다. Optional 컬렉션(`Collection<Optional<Number>>`)은 변성(variance) 수정자와 함께 추론하기 어려워집니다.

Java의 사용 위치 변성(use-site variance)은 이미 충분히 복잡한데, Optional을 추가하면 상황이 더 복잡해집니다. 간단한 컬렉션이 마주하지 않는 복잡성을 Optional 컬렉션은 마주하게 됩니다.

### 직렬화 불가

`Optional`은 의도적으로 `Serializable`이 아닙니다. Brian Goetz는 무언가를 직렬화 가능하게 만드는 것은 "영원히 표현을 고정"시키고 미래의 발전을 제약한다고 설명했습니다. 이것은 `Optional`을 엔티티 클래스의 필드로 사용하거나 직렬화가 필요한 다른 상황에서 제한됩니다.

## Optional의 주된 정당화

`Optional`을 포함시킨 실제 동기는 객체 스트림과 기본형 스트림 간에 통합된 API 적용 범위를 제공하는 것입니다. Optional 기본형들(`OptionalInt`, `OptionalLong`, `OptionalDouble`)은 개발자가 일관된 API 패턴을 유지하면서 스트림 타입 간에 전환할 수 있게 해줍니다.

예를 들어, `IntStream`의 `findAny()`는 `OptionalInt`를 반환하고, `Stream<T>`의 `findAny()`는 `Optional<T>`를 반환합니다. 이를 통해 유사한 API가 기본형과 참조 타입 모두에서 작동할 수 있습니다.

## 대안적 접근 방식

### Ceylon의 흐름 감지 타이핑

Ceylon은 컴파일러가 null 가능성을 추적하고, null 체크 후에 변수가 자동으로 non-null 타입으로 좁혀지는 더 우아한 솔루션을 제공합니다.

### Groovy의 엘비스 연산자

Groovy는 `?:` 연산자(엘비스 연산자)를 제공하여 null 처리를 더 간결하게 만듭니다.

### 어노테이션 기반 접근

`@Nullable`과 `@NonNull` 어노테이션을 사용하는 것도 하나의 대안입니다. 이러한 어노테이션은 IDE와 정적 분석 도구에서 null 관련 문제를 감지하는 데 도움을 줄 수 있습니다.

## 결론

`Optional`을 광범위하게 활용하는 것은 비례적인 이점 없이 복잡성을 도입합니다. Ceylon의 접근 방식이나 Groovy의 엘비스 연산자와 비교할 때, `Optional`은 null 가능성 관리에 대한 우아한 솔루션이라기보다는 어색한 우회책으로 기능합니다.

`Optional`은 Streams API 컨텍스트에서 가치를 제공하지만, 더 광범위한 채택은 대안들과 비교했을 때 이점보다 더 큰 복잡성을 제시합니다. Java의 하위 호환성 제약으로 인해, `Optional`은 Java에서 선택 사항으로 남을 것이며, null 문제에 대한 완전한 해결책이 되지는 않을 것입니다.
