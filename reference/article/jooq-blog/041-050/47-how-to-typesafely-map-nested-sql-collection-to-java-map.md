# jOOQ에서 중첩된 SQL 컬렉션을 중첩된 Java Map으로 타입 안전하게 매핑하는 방법

> 원문: https://blog.jooq.org/how-to-typesafely-map-a-nested-sql-collection-into-a-nested-java-map-with-jooq/

## 소개

이 글은 jOOQ를 사용하여 중첩된 컬렉션을 Java `Map` 구조로 매핑하는 방법에 대한 Stack Overflow 질문을 다룹니다. 이전에 `MULTISET` 연산자의 컬렉션 중첩 기능에 대해 논의한 바 있는데, 이 기법이 단순한 `List` 구조를 넘어 보다 복잡한 `Map` 기반 표현으로 어떻게 확장되는지를 설명합니다.

## 목표 데이터 구조

예제에서는 Sakila 데이터베이스의 영화 제목과 일별 수익 데이터를 감싸는 Java 레코드를 사용합니다:

```java
record Film(
    String title,
    Map<LocalDate, BigDecimal> revenue
) {}
```

이 레코드는 영화 제목(`title`)과 날짜별 수익 데이터(`revenue`)를 하나의 구조로 묶습니다.

## 핵심 쿼리 패턴

해결책은 `MULTISET` 연산자와 변환 단계를 결합하여 사용합니다:

```java
List<Film> result =
ctx.select(
        FILM.TITLE,
        multiset(
            select(
                PAYMENT.PAYMENT_DATE.cast(LOCALDATE),
                sum(PAYMENT.AMOUNT))
            .from(PAYMENT)
            .where(PAYMENT.rental().inventory().FILM_ID
                .eq(FILM.FILM_ID))
            .groupBy(PAYMENT.PAYMENT_DATE.cast(LOCALDATE))
            .orderBy(PAYMENT.PAYMENT_DATE.cast(LOCALDATE))
        )
        .convertFrom(r -> r.collect(Records.intoMap()))
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch(Records.mapping(Film::new))
```

이 쿼리는 각 영화에 대해 결제 데이터를 날짜별로 집계하고, 그 결과를 `Map`으로 변환하여 `Film` 레코드에 매핑합니다.

## 타입 변환 메커니즘

핵심적인 변환 단계는 다음과 같은 타입 변환을 수행합니다:

- 변환 전: `Field<Result<Record2<LocalDate, BigDecimal>>>` (MULTISET 필드 타입)
- 변환 후: `Field<Map<LocalDate, BigDecimal>>`

이 변환은 jOOQ 3.15의 임시(ad-hoc) 변환 API에서 제공하는 `Field.convertFrom()`을 사용하며, `Records.intoMap()`을 수집 함수로 적용합니다. 수동 변환 단계 없이 중첩된 컬렉션 결과를 `Map` 구조로 변환할 수 있습니다.

## Collector 시그니처

`Records.intoMap()` 메서드는 다음과 같은 제네릭 제약 조건을 가집니다:

```java
public static final <K, V, R extends Record2<K, V>>
Collector<R, ?, Map<K, V>> intoMap() { ... }
```

이 설계 덕분에 `Record2` 인스턴스를 수집할 때 명시적인 키/값 타입 매개변수를 생략할 수 있습니다.

## 결과 소비

쿼리 결과를 다음과 같이 순회하며 출력할 수 있습니다:

```java
for (Film film : result) {
    System.out.println("Film %s with revenue: "
        .formatted(film.title()));
    film.revenue().forEach((d, r) ->
        System.out.println("  %s: %s".formatted(d, r))
    );
}
```

## 주요 특징

### 삽입 순서 보존

`intoMap()`과 `intoGroups()` 컬렉터는 모두 순서를 보존하는 구현체(예: `LinkedHashMap`)를 생성합니다. 이를 통해 `MULTISET`의 정렬 순서가 최종 결과에서도 유지됩니다.

### 타입 안전성

컴파일 타임 검사가 일관성을 보장합니다. 컬럼 표현식이나 레코드 타입을 변경하면 즉시 컴파일 오류가 발생하므로, 타입 인식이 부족한 다른 대안에서 발생할 수 있는 런타임 오류를 방지할 수 있습니다.

### 유연한 매핑

`Record2` 제네릭 제약 조건을 통해 임의의 키/값 타입을 지원합니다. 이는 다양한 데이터 구조에 유연하게 적용할 수 있음을 의미합니다.

## 결론

jOOQ 3.15 이상에서는 `MULTISET`/`MULTISET_AGG`, 임시 변환기(ad-hoc converter) API, 그리고 `Records` 컬렉터 유틸리티를 조합하여 정교한 중첩 데이터 구조를 구성할 수 있습니다. 이를 통해 SQL 결과를 임의의 제네릭 매개변수를 가진 중첩 `Map` 구현체로 타입 안전하게 변환할 수 있습니다.
