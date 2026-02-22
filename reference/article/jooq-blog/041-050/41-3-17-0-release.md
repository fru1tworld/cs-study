# jOOQ 3.17.0 릴리스: 계산 컬럼, 감사 컬럼, 패턴 매칭, 리액티브 트랜잭션, 코틀린 코루틴 지원

> 원문: https://blog.jooq.org/3-17-0-release-with-computed-columns-audit-columns-pattern-matching-reactive-transactions-and-kotlin-coroutine-support/

이번 릴리스는 이전 릴리스에서 이어온 더욱 정교한 SQL 변환 기능에 대한 작업을 계속하며, 다음과 같은 기능들을 포함합니다:

- 읽기 및 쓰기 작업 모두를 위한 클라이언트 사이드 계산 컬럼
- 감사 컬럼
- 패턴 매칭 SQL 변환
- 더 많은 암묵적 JOIN 기능

## 클라이언트 사이드 계산 컬럼

jOOQ 3.17은 상용 기능으로 "클라이언트 사이드 계산 컬럼"을 도입하여, 데이터베이스 수준의 계산 컬럼을 애플리케이션 레벨에서 에뮬레이션할 수 있도록 쿼리를 변환할 수 있게 합니다. 이 기능은 `STORED`와 `VIRTUAL` 두 가지 변형을 지원합니다.

- STORED: INSERT, UPDATE, DELETE, MERGE 작업에 영향을 줍니다.
- VIRTUAL: SELECT 및 RETURNING 절에 영향을 줍니다.

서버 사이드 계산 컬럼과 달리, 클라이언트 사이드 계산 컬럼은 암묵적 조인, 스칼라 서브쿼리, MULTISET 서브쿼리 등 임의의 표현식을 생성할 수 있습니다. 이들은 본질적으로 jOOQ로 작성된 "뷰"와 같은 역할을 하되, 컬럼 단위로 동작합니다. 선택적으로 가시성 수정자를 사용하여 컬럼을 비공개로 유지할 수도 있습니다.

## 감사 컬럼

STORED 클라이언트 사이드 계산 컬럼의 특수한 경우로 감사 컬럼이 있으며, 가장 기본적인 구현은 다음과 같은 형태로 제공됩니다:

- `CREATED_AT` - 생성 시각
- `CREATED_BY` - 생성자
- `MODIFIED_AT` - 수정 시각
- `MODIFIED_BY` - 수정자

이 상용 전용 편의 기능은 데이터베이스 작업 전반에 걸친 시간 추적을 간소화합니다.

## Java 17 기준선

Java 17은 최신 LTS 버전이며, 다음과 같은 정말 멋진 기능들을 포함하고 있습니다:

- 봉인 타입(sealed types) - 패턴 매칭에 필수적
- 레코드(records)
- instanceof 패턴 매칭
- 텍스트 블록(text blocks)
- switch 표현식(switch expressions)

오픈 소스 에디션은 이제 Java 17을 기준으로 요구하며, 이러한 현대적인 언어 기능들을 활용합니다. 이전 버전(3.14-3.16)은 상용 jOOQ 배포판에서 Java 8 및 11 지원을 계속 받습니다.

## PostgreSQL 확장

`jooq-postgres-extensions` 모듈은 이제 다음과 같은 추가 PostgreSQL 전용 데이터 타입을 지원하도록 확장되었습니다:

- CIDR
- CITEXT
- LTREE
- HSTORE
- INET
- RANGE 타입 (INT4, INT8 등의 특수화 포함)

이러한 타입들은 배열 변형과 함께 자동 코드 생성 통합을 지원합니다.

## 패턴 매칭 SQL 변환

이 상용 전용 기능은 표현식 트리에 패턴 매칭을 적용하여 정교한 SQL 최적화를 수행합니다. 변환 예시는 다음과 같습니다:

- `LTRIM(RTRIM(x))` -> `TRIM(x)`
- `x != a AND x != b` -> `x NOT IN (a, b)`
- `x IN (a, b, c) AND x IN (b, c, d)` -> `x IN (b, c)`

활용 사례는 다음과 같습니다:

- SQL 린팅
- 자동 정리
- 방언(dialect) 마이그레이션
- 기능 패칭

## 리액티브 및 코틀린 코루틴 지원

### R2DBC 지원

R2DBC 0.9.1.RELEASE 버전이 이제 지원됩니다.

### 리액티브 트랜잭션 API

기존 블로킹 트랜잭션 API와 동일한 중첩 트랜잭션 시맨틱을 제공하는 새로운 리액티브 트랜잭션 API가 추가되었습니다.

### 코틀린 코루틴

jOOQ의 `Publisher` SPI를 통한 리액티브 스트림 바인딩은 이제 새로운 `org.jooq:jooq-kotlin-coroutines` 모듈에서 코틀린 코루틴으로 자동 브릿지됩니다. 이는 `org.jetbrains.kotlinx:kotlinx-coroutines-core`와 `org.jetbrains.kotlinx:kotlinx-coroutines-reactor`의 일반적인 유틸리티를 사용합니다.

### MULTISET 확장 함수

`org.jooq:jooq-kotlin` 확장 모듈에는 이제 `MULTISET` 및 기타 중첩 관련 편의를 위한 추가 확장 함수가 포함되어 있습니다.

### @Blocking 어노테이션

전체 블로킹 실행 API에 `org.jetbrains.annotations.Blocking` 어노테이션이 적용되어, 리액티브 사용자가 IntelliJ에서 실수로 블로킹 호출을 하는 것을 방지할 수 있습니다.

## 암묵적 JOIN 개선

이번 릴리스에서 암묵적 JOIN에 대한 여러 개선 사항이 도입되었습니다.

`Table<R>`은 이제 `SelectField<R>`을 확장하고, `Condition`은 `Field<Boolean>`을 확장합니다. 이를 통해 기존의 `Field` 객체와 함께 SELECT 절에서 TableRecord와 Boolean 표현식을 직접 프로젝션할 수 있습니다.

```java
Result<Record3<CustomerRecord, AddressRecord, Boolean>> result =
ctx.select(
  CUSTOMER,
  CUSTOMER.address(),
  exists(selectOne().from(PAYMENT)
    .where(PAYMENT.CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID)))
).from(CUSTOMER).fetch();
```

또한 DML 문에서의 암묵적 JOIN 지원이 추가되어, JOIN이 지원되지 않는 곳에서 상관 서브쿼리를 생성합니다.

`EXISTS(...)` 및 `MULTISET(...)` 서브쿼리를 위한 편의 구문 실험(#13063, #13069)이 있었으나 프로토타입은 채택되지 않았습니다. 향후 jOOQ 버전에서는 더 많은 암묵적 JOIN 기능의 형태로 원하는 편의성을 구현할 예정이며, 암묵적 to-many JOIN으로도 해당 기능을 제공할 것입니다.
