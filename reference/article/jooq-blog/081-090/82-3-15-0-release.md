# 3.15.0 릴리스 - R2DBC, 중첩 ROW, ARRAY, MULTISET 타입, 5개 새 SQL 방언, CREATE PROCEDURE, FUNCTION, TRIGGER 지원 등

> 원문: https://blog.jooq.org/3-15-0-release-with-support-for-r2dbc-nested-row-array-and-multiset-types-5-new-sql-dialects-create-procedure-function-and-trigger-support-and-much-more/

jOOQ 3.15가 출시되었습니다! 이번 릴리스는 많은 사용자들이 기다려온 주요 기능들을 포함하고 있습니다.

## R2DBC 지원

많은 사용자들이 기다려온 기능입니다: jOOQ 3.15는 새로운 네이티브 R2DBC 통합 덕분에 리액티브하게 되었습니다. 최근 버전들은 이미 리액티브 스트림 Publisher SPI를 구현했지만, 이제는 더 이상 속이지 않습니다. 더 이상 블로킹하지 않습니다.

R2DBC ConnectionFactory로 구성된 jOOQ 쿼리를 Flux(또는 원하는 다른 리액티브 스트림 API)로 감싸기만 하면 됩니다:

```java
Flux.from(ctx.select(BOOK.TITLE).from(BOOK))
```

블로킹(JDBC를 통해)과 논블로킹(R2DBC를 통해) 모두 함께 작동할 수 있어, 쿼리 빌딩 로직을 변경하지 않고도 두 실행 모델 사이를 빠르게 전환할 수 있습니다.

### R2DBC 구성

DSLContext는 R2DBC ConnectionFactory로 구성할 수 있습니다:

```java
ConnectionFactory connectionFactory = ConnectionFactories.get(
    ConnectionFactoryOptions.parse("r2dbc:h2:file://localhost/~/r2dbc-test")
        .mutate()
        .option(ConnectionFactoryOptions.USER, "sa")
        .option(ConnectionFactoryOptions.PASSWORD, "")
        .build()
);
DSLContext ctx = DSL.using(connectionFactory);
```

### 지원되는 R2DBC 드라이버

이 기능은 Oracle, H2, PostgreSQL, MySQL, MariaDB, MSSQL을 포함한 여러 데이터베이스와 통합됩니다.

jOOQ는 자동으로 R2DBC 연결을 획득하고 해제하여, 수동 구현에 비해 리소스 처리를 최적화합니다.

## ROW 타입, ROW 타입의 ARRAY, MULTISET 프로젝션

jOOQ 3.14에서 표준 SQL/XML 및 SQL/JSON 지원을 구현한 후, SQL을 한 단계 더 발전시키는 또 다른 주요 마일스톤이 이제 실험적 기능으로 사용 가능합니다: 표준 SQL MULTISET 연산자를 사용한 컬렉션 중첩.

### MULTISET 연산자

MULTISET 연산자는 현재 SQL/XML 또는 SQL/JSON을 사용하여 에뮬레이션됩니다. 결과 문서는 JDBC에서 가져올 때 다시 파싱됩니다. 향후 버전에서는 네이티브 지원(Informix, Oracle)과 ARRAY를 사용한 에뮬레이션(PostgreSQL을 포함한 다양한 방언)도 제공할 예정입니다.

표준 SQL multiset 값 생성자 연산자를 사용하면 상관 서브쿼리의 데이터를 중첩 데이터 구조인 MULTISET으로 수집할 수 있습니다. SQL의 모든 것은 MULTISET이므로 이 연산자가 그리 놀랍지 않을 수 있습니다. 하지만 중첩이 빛나는 부분입니다.

### 중첩 컬렉션 예제

새로운 연산자는 직관적인 중첩 컬렉션 구문을 허용합니다:

```java
var result = dsl.select(
    FILM.TITLE,
    multiset(
      select(FILM_ACTOR.actor().FIRST_NAME,
             FILM_ACTOR.actor().LAST_NAME)
      .from(FILM_ACTOR)
      .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
    ).as("actors"),
    multiset(
      select(FILM_CATEGORY.category().NAME)
      .from(FILM_CATEGORY)
      .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
    ).as("categories")
  ).from(FILM).fetch();
```

### 결과 구조

쿼리는 다음을 나타내는 타입 안전한 결과를 반환합니다:

```java
Result<Record3<String, Result<Record2<String, String>>, Result<Record1<String>>>>
```

- 영화 제목
- 중첩된 배우 결과
- 중첩된 카테고리 결과

JSON 출력 예제:

```json
{
  "title": "ACADEMY DINOSAUR",
  "actors": [
    {"first_name": "PENELOPE", "last_name": "GUINESS"}
  ],
  "categories": [{"name": "Documentary"}]
}
```

### 주요 장점

- 타입 안전: 제네릭을 통한 완전한 컴파일 타임 안전성
- 데이터베이스 주도: SQL 실행 플래너가 전체 쿼리를 최적화
- N+1 문제 없음: 중첩 결과를 가진 단일 쿼리
- 중복 없음: 각 영화는 중첩 컬렉션과 함께 한 번만 나타남
- 다중 데이터베이스 지원: MySQL, Oracle, PostgreSQL, SQL Server에서 작동

### DTO 매핑

결과는 `convertFrom()`과 `mapping()`을 사용하여 Java 레코드에 깔끔하게 매핑됩니다:

```java
record Actor(String firstName, String lastName) {}
record Film(String title, List<Actor> actors,
            List<String> categories) {}

List<Film> result = dsl.select(...)
  .fetch(mapping(Film::new));
```

### 복잡한 중첩 예제

이 기능은 `MULTISET_AGG`를 통한 집계와 함께 깊은 중첩을 지원합니다:

```java
multiset(
  select(PAYMENT.rental().customer().FIRST_NAME,
         PAYMENT.rental().customer().LAST_NAME,
         multisetAgg(PAYMENT.PAYMENT_DATE,
                     PAYMENT.AMOUNT).as("payments"),
         sum(PAYMENT.AMOUNT).as("total"))
  .from(PAYMENT)
  .where(PAYMENT.rental().inventory().FILM_ID
         .eq(FILM.FILM_ID))
  .groupBy(...)
).as("customers")
```

### 생성되는 SQL

jOOQ는 JSON/XML 집계를 사용하여 표준 SQL로 변환합니다:

```sql
(
  select coalesce(
    jsonb_agg(jsonb_build_object(
      'first_name', t.first_name,
      'last_name', t.last_name
    )),
    jsonb_build_array()
  )
  from (select ...) as t
) as actors
```

### 성능 고려사항

이 접근 방식은 카테시안 곱을 완전히 피하고 상관 서브쿼리만 필요로 합니다. 이는 쿼리 단순성을 유지하면서 효율적인 실행 계획을 보장합니다.

### DataType.row()

jOOQ 3.15에는 중첩 ROW 타입을 추적하고 `Field<?>[]`에 접근할 수 있도록 `DataType.row()`가 추가되었습니다.

## 5개의 새로운 SQL 방언

jOOQ 3.15에서는 5개(!)의 새로운 SQLDialect를 추가했습니다. 이는 이전 마이너 릴리스에서는 전례가 없는 일입니다.

### 새로운 방언

- BIGQUERY: 대중적인 요청에 의해 오랫동안 기다려온 기능
- SNOWFLAKE: 대중적인 요청에 의해 오랫동안 기다려온 기능
- EXASOL: 고객의 후원으로 신속하게 지원
- IGNITE: Apache Ignite 지원
- JAVA: 실험적 방언. 주로 https://www.jooq.org/translate 를 사용하여 네이티브 SQL 쿼리를 jOOQ로 번역하려는 경우에 유용하며, 실행할 수는 없습니다

### 업데이트된 방언

Redshift, HANA, Vertica를 포함한 여러 기존 방언들도 업데이트되었습니다.

### 지원 중단된 방언

Ingres와 Oracle10G 지원은 더 이상 사용되지 않습니다(deprecated).

## Java 버전 변경

jOOQ 3.15에서 Java 6과 7 지원이 제거되었고, Java 8이 상용 에디션의 새로운 기준선이 되었으며, Java 11이 jOOQ 오픈 소스 에디션의 기준선이 되었습니다. 이는 OSS 에디션이 이제 드디어 모듈화되었으며, Flow API(R2DBC 참조)와 `@Deprecated(forRemoval, since)` 같은 작은 것들에 접근할 수 있게 되었음을 의미합니다.

### Java 8로의 업그레이드 이점

Java 8로의 업그레이드를 통해 jOOQ 내부에 흥미로운 새로운 개선 사항이 가능해졌습니다:
- default 메서드
- 람다
- 일반화된 타겟 타입 추론
- 그 외 다양한 기능

### 독 푸딩(Dog Fooding)과 새로운 기능

코드베이스를 개선하면 독 푸딩(자체 제품 사용)으로 이어지고, 이는 다시 사용자를 위한 새로운 기능으로 이어집니다. 예를 들어, 내부를 리팩토링하면서 `ResultQuery.collect()`에 많은 중점을 두었습니다. 더 기능적인 변환 편의를 위한 새로운 보조 타입인 `org.jooq.Rows`와 `org.jooq.Records`가 있습니다.

더 많은 함수는 더 적은 루프를 의미하고, 또한 더 적은 ArrayList 할당을 의미합니다.

## DML 문 개선

위의 모든 Java 8 관련 기능과 jOOQ의 더 기능적인 사용으로, 드디어 DML 문 타입 계층(INSERT, UPDATE, DELETE)을 리팩토링하여 각각의 RETURNING 절이 실제 ResultQuery를 반환하도록 했습니다.

이제 DML 문에 대해 `stream()`, `collect()`, `fetchMap()` 및 `subscribe()`(R2DBC를 통해)를 사용할 수 있으며, PostgreSQL에서는 WITH 절에 넣을 수도 있습니다.

```java
// DML 결과를 스트리밍하고 수집하는 예제
ctx.insertInto(BOOK)
   .columns(BOOK.TITLE, BOOK.AUTHOR_ID)
   .values("New Book", 1)
   .returning(BOOK.ID, BOOK.TITLE)
   .stream()
   .collect(Collectors.toList());
```

## ParsingConnection 개선

ParsingConnection은 더 이상 실험적이지 않습니다. 이제 배치가 가능합니다. 통합 속도를 크게 높이기 위해 입력/출력 SQL 문자열 쌍에 대한 캐시를 추가했습니다.

### 새로운 파서 기능

- 배치 처리: 이제 배치 연산이 지원됩니다
- SQL 캐싱: SQL 쌍에 대한 캐싱 메커니즘으로 SQL 번역 속도 향상
- 바인드 변수 타입 추론 개선: 지연된 바인드 변수 타입 추론
- ParseListener SPI: 커스텀 구문 확장을 위한 새로운 SPI

## CREATE PROCEDURE, FUNCTION, TRIGGER 지원

jOOQ 3.15에서는 벤더 독립적인 방식으로 서버 측 로직을 구현하기 위한 CREATE PROCEDURE, CREATE FUNCTION, CREATE TRIGGER 문 지원이 추가되었습니다.

이를 통해 여러 데이터베이스 벤더에 걸쳐 절차적 언어 로직을 작성하고 관리할 수 있습니다.

## 암시적 조인(Implicit Join) 개선

jOOQ의 매우 유용한 암시적 JOIN 기능을 사용하여 실제 또는 합성 외래 키에 대해 테이블을 조인하는 경로 표기법을 사용할 수 있습니다.

jOOQ는 한동안 JPQL의 가장 멋진 기능 중 하나를 지원해 왔습니다: 암시적 조인. jOOQ를 사용하면 타입 안전한 방식으로 to-one 관계를 탐색하고 LEFT JOIN 연산을 생성할 수 있습니다.

### LEFT JOIN 기본 동작

jOOQ는 암시적 조인에 대해 LEFT JOIN만 생성합니다. 이는 암시적으로 조인된 열이 일치하지 않는 INNER JOIN 때문에 행 수를 줄이지 않도록 보장합니다.

jOOQ 3.14부터 외래 키가 필수/not null인지 여부에 따라 내부 조인도 지원됩니다. 기본 동작은 선택적 외래 키를 암시적으로 조인하는 올바른 방법인 LEFT JOIN을 생성하는 것입니다.

## JDBC 드라이버 의존성

Oracle, PostgreSQL, SQL Server에 대해 리플렉션 대신 명시적인 Maven Central 드라이버 의존성을 사용합니다.

## 기타 개선사항

이 릴리스에는 수많은 버그 수정과 작은 개선 사항이 포함되어 있습니다. 전체 릴리스 노트는 공식 jOOQ 문서를 참조하세요.

---

*이 릴리스는 2021년 7월 6일에 게시되었습니다.*
