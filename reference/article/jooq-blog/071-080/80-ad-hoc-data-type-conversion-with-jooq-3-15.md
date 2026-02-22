# jOOQ 3.15로 임시 데이터 타입 변환하기

> 원문: https://blog.jooq.org/ad-hoc-data-type-conversion-with-jooq-3-15/

## 개요

이 블로그 글은 Lukas Eder가 2021년 7월 20일에 작성한 것으로, jOOQ 3.15에서 도입된 덜 알려진 기능인 필드에 대한 임시(ad-hoc) 데이터 타입 변환을 설명합니다.

## 문제 상황

전통적으로 jOOQ는 데이터베이스 타입을 의미 있는 Java 타입으로 매핑하기 위해 코드 생성 시점에 컨버터를 연결해야 했습니다. 숫자형 치수를 가진 가구 테이블을 예로 들어보겠습니다:

```sql
CREATE TABLE furniture (
  id INT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  length NUMERIC,
  width NUMERIC,
  height NUMERIC
);
```

`BigDecimal`을 직접 사용하는 대신, 개발자들은 커스텀 래퍼 타입을 선호할 수 있습니다:

```java
record Dimension(BigDecimal value) {}
```

## 코드 생성기 접근 방식

전통적인 방법을 사용하면, 개발자들은 코드 생성기에서 강제 타입(forced type)을 설정해야 합니다:

```xml
<forcedType>
  <userType>com.example.Dimension</userType>
  <converter><![CDATA[
  org.jooq.Converter.ofNullable(
    BigDecimal.class,
    Dimension.class,
    Dimension::new,
    Dimension::value
  )
  ]]></converter>
  <includeExpression>LENGTH|WIDTH|HEIGHT</includeExpression>
</forcedType>
```

이를 통해 타입 안전한 쿼리가 가능해집니다:

```java
Result<Record3<Dimension, Dimension, Dimension>> result =
ctx.select(FURNITURE.LENGTH, FURNITURE.WIDTH, FURNITURE.HEIGHT)
   .from(FURNITURE)
   .fetch();
```

## 코드 생성의 한계

전통적인 접근 방식에는 제약이 있었습니다: 코드 생성에 대한 접근 제한, 런타임에만 스키마를 알 수 있는 시나리오, 또는 쿼리별 변환이 필요한 경우 등이 그렇습니다.

## 동적 스키마 솔루션 (3.15 이전)

코드 생성 없이 동적 스키마를 다루려면, 개발자들은 필드를 수동으로 구성해야 했습니다:

```java
Table<?> furniture = table(name("furniture"));
Field<BigDecimal> length = field(name("furniture", "length"), NUMERIC);
Field<BigDecimal> width  = field(name("furniture", "width"), NUMERIC);
Field<BigDecimal> height = field(name("furniture", "height"), NUMERIC);

Result<Record3<BigDecimal, BigDecimal, BigDecimal>> result =
ctx.select(length, width, height)
   .from(furniture)
   .fetch();
```

`asConvertedDataType()`을 사용하면 영구적인 변환을 적용할 수 있었습니다:

```java
DataType<Dimension> type = NUMERIC.asConvertedDataType(
    Converter.ofNullable(
        BigDecimal.class,
        Dimension.class,
        Dimension::new,
        Dimension::value
    )
);

Table<?> furniture = table(name("furniture"));
Field<Dimension> length = field(name("furniture", "length"), type);
Field<Dimension> width  = field(name("furniture", "width"), type);
Field<Dimension> height = field(name("furniture", "height"), type);

Result<Record3<Dimension, Dimension, Dimension>> result =
ctx.select(length, width, height)
   .from(furniture)
   .fetch();
```

## 임시 변환 (jOOQ 3.15+)

새로운 기능은 필드 재정의 없이 인라인으로 쿼리별 변환을 가능하게 합니다:

```java
Table<?> furniture = table(name("furniture"));
Field<BigDecimal> length = field(name("furniture", "length"), NUMERIC);
Field<BigDecimal> width  = field(name("furniture", "width"), NUMERIC);
Field<BigDecimal> height = field(name("furniture", "height"), NUMERIC);

Result<Record3<BigDecimal, BigDecimal, Dimension>> result =
ctx.select(length, width, height.convertFrom(Dimension::new))
   .from(furniture)
   .fetch();
```

## Converter 인터페이스

기반이 되는 `Converter` 인터페이스는 다음을 필요로 합니다:

```java
public interface Converter<T, U> {
    U from(T databaseObject);
    T to(U userObject);
    Class<T> fromType();
    Class<U> toType();
}
```

여기서 `T`는 JDBC 타입을, `U`는 사용자 타입을 나타냅니다.

## 사용 가능한 메서드

여러 메서드 오버로드가 유연성을 제공합니다:

```java
// 읽기 전용 변환 (SELECT용)
height.convertFrom(Dimension::new);
height.convertFrom(Dimension.class, Dimension::new);

// 쓰기 전용 변환 (WHERE, DML용)
height.convertTo(Dimension::value);
height.convertTo(Dimension.class, Dimension::value);

// 양방향 전체 변환
height.convert(Dimension.class, Dimension::new, Dimension::value);
height.convert(Converter.ofNullable(
    BigDecimal.class,
    Dimension.class,
    Dimension::new,
    Dimension::value
));
```

## 실용적인 예제

프로젝션에서의 읽기 전용:

```java
Result<Record1<Dimension>> result =
ctx.select(height.convertFrom(Dimension::new))
   .from(furniture)
   .fetch();
```

DML에서의 쓰기 전용:

```java
ctx.insertInto(furniture)
   .columns(height.convertTo(Dimension::value))
   .values(new Dimension(BigDecimal.ONE))
   .execute();
```

## 핵심 요점

임시 변환은 사용 시점에 "즉석에서" 타입 변환을 가능하게 하여, 유연성이 필요할 때 필드 정의에 컨버터를 영구적으로 연결하거나 코드 생성을 설정할 필요성을 없애줍니다.
