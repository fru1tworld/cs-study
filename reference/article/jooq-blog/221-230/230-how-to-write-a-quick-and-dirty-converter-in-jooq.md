> 원문: https://blog.jooq.org/how-to-write-a-quick-and-dirty-converter-in-jooq/

# jOOQ에서 간단하고 빠른 컨버터(Converter) 작성하는 방법

jOOQ의 강력한 기능 중 하나는 커스텀 데이터 타입을 도입할 수 있는 컨버터(Converter) 기능입니다. 이 글에서는 JDBC 타입을 더 현대적인 Java 타입으로 변환하는 컨버터를 작성하는 방법을 설명합니다.

## 실용적인 예제: Timestamp를 LocalDateTime으로 변환

일반적인 사용 사례로, SQL `TIMESTAMP` 타입을 오래된 `java.sql.Timestamp` 클래스 대신 JSR-310의 `LocalDateTime` 객체로 변환하는 경우가 있습니다.

### 표준 컨버터 (람다 함수 사용)

```java
Converter<Timestamp, LocalDateTime> converter =
Converter.of(
    Timestamp.class,
    LocalDateTime.class,
    t -> t == null ? null : t.toLocalDateTime(),
    u -> u == null ? null : Timestamp.valueOf(u)
);
```

이 방식에서는 `null` 처리를 직접 해야 합니다.

### 간소화된 Nullable 컨버터

jOOQ는 `null` 처리를 자동으로 해주는 더 간단한 방식을 제공합니다:

```java
Converter<Timestamp, LocalDateTime> converter =
Converter.ofNullable(
    Timestamp.class,
    LocalDateTime.class,
    Timestamp::toLocalDateTime,
    Timestamp::valueOf
);
```

`Converter.ofNullable()` 메서드를 사용하면 `null` 체크 로직을 직접 작성할 필요 없이 메서드 레퍼런스만으로 간결하게 컨버터를 정의할 수 있습니다.

## 실제 적용

코드 생성기(Code Generator)를 사용할 때는 구체적인 컨버터 클래스가 필요하지만, 람다 기반 컨버터는 다른 jOOQ API 컨텍스트에서 잘 동작합니다:

```java
DSL.field(
    "my_table.my_timestamp",
    SQLDataType.TIMESTAMP.asConvertedDataType(
        Converter.ofNullable(...)
    )
);
```

## 핵심 요점

jOOQ 3.9 이후 도입된 이러한 팩토리 메서드 방식은 별도의 컨버터 클래스 구현 없이도 간단하게 컨버터를 생성할 수 있게 해줍니다. 특히 일회성 변환이나 빠른 프로토타이핑에 매우 유용합니다.
