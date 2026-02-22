> 원문: https://blog.jooq.org/a-nice-api-design-gem-strategy-pattern-with-lambdas/

# 멋진 API 설계 기법: 람다를 활용한 전략 패턴(Strategy Pattern)

게시일: 2017년 3월 17일, Lukas Eder

## 개요

이 글에서는 Java 8의 람다(Lambda)가 어떻게 고전적인 전략 패턴(Strategy Pattern)의 현대적인 구현을 가능하게 하는지 살펴봅니다. 람다를 활용하면 보일러플레이트(Boilerplate) 코드를 줄이면서 API 사용성을 향상시킬 수 있습니다.

## 전통적인 전략 패턴의 문제점

전통적인 접근 방식은 장황한 인터페이스를 구현해야 합니다. 저자는 jOOQ의 `Converter` 인터페이스를 예로 들어 설명합니다. 이 인터페이스는 일반적으로 네 개의 메서드 구현을 요구합니다:

```java
public interface Converter<T, U> {
    U from(T databaseObject);
    T to(U userObject);
    Class<T> fromType();
    Class<U> toType();
}
```

16진수 문자열을 정수로 변환하는 구체적인 구현체를 만들려면 null 처리와 타입 선언을 포함한 상당한 양의 보일러플레이트 코드가 필요합니다.

## 람다를 활용한 해결책

전체 클래스 구현을 요구하는 대신, API 설계자는 함수형 인터페이스(Functional Interface)를 매개변수로 받는 팩토리 메서드(Factory Method)를 제공할 수 있습니다:

```java
static <T, U> Converter<T, U> ofNullable(
    Class<T> fromType,
    Class<U> toType,
    Function<? super T, ? extends U> from,
    Function<? super U, ? extends T> to
)
```

이 접근 방식을 사용하면 16진수 변환기를 간결한 함수형 선언으로 변환할 수 있습니다:

```java
Converter<String, Integer> converter =
Converter.ofNullable(
    String.class,
    Integer.class,
    s -> Integer.parseInt(s, 16),
    Integer::toHexString
);
```

## 추가 예제

JDK의 `Collector.of()` 생성자도 이 패턴을 보여주는 좋은 예입니다. 이를 통해 번거로운 구현 클래스 없이도 간단하게 스트림(Stream) 연산을 수행할 수 있습니다.

## 핵심 요점

"여러 메서드를 가진 인터페이스가 있다면..." 해당 메서드들에 대응하는 함수형 인터페이스를 매개변수로 받는 정적 팩토리(Static Factory)를 제공하세요. 이렇게 하면 사용자가 "전략(Strategy)"을 람다로 전달할 수 있어, 이름 지정에 따른 부담과 보일러플레이트 코드를 제거하면서도 타입 안전성(Type Safety)을 유지할 수 있습니다.
