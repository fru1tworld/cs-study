# jOOQ 3.17의 가상 클라이언트 사이드 계산 컬럼으로 동적 뷰 만들기

> 원문: https://blog.jooq.org/create-dynamic-views-with-jooq-3-17s-new-virtual-client-side-computed-columns/

jOOQ 3.17의 가장 멋진 새 기능 중 하나는 클라이언트 사이드 계산 컬럼입니다. jOOQ 3.16에서 이미 서버 사이드 계산 컬럼 지원이 추가되었으며, 많은 분들이 다양한 이유로 이를 높이 평가하고 있습니다.

## 계산 컬럼이란?

계산 컬럼은 표현식에서 파생(계산)되는 컬럼입니다. 직접 값을 기록할 수 없으며, 뷰의 컬럼처럼 동작합니다. 계산 컬럼에는 두 가지 유형이 있습니다:

- VIRTUAL 계산 컬럼: 읽을 때 계산됨
- STORED 계산 컬럼: 쓸 때 계산됨

일부 SQL 방언에서는 이 두 기능을 구분하기 위해 정확히 이 용어를 사용합니다. 일부 방언은 두 가지 모두 지원하고, 일부는 하나만 지원합니다.

## 서버 사이드 계산 컬럼의 한계

서버 사이드 계산 컬럼은 강력하지만 제약이 있습니다. 모든 SQL 방언이 이를 지원하지 않으며, 일부는 VIRTUAL 또는 STORED 중 하나만 지원합니다(두 접근 방식 모두 각각의 장점이 있습니다). 기능 자체가 SQL에서 상당히 제한적입니다.

예를 들어, PostgreSQL은 VIRTUAL 계산 컬럼을 지원하지 않으며, 컬럼 생성 표현식에서 서브쿼리를 사용하려고 하면 다음과 같은 오류가 발생합니다:

```
SQL Error [0A000]: ERROR: cannot use subquery in column generation expression
```

조인이나 상관 서브쿼리를 포함하는 복잡한 계산은 서버 사이드 계산 컬럼으로는 불가능한 경우가 많습니다.

## 뷰를 사용하는 방법은?

뷰를 생성하여 계산 표현식을 포함할 수 있지만, 이 접근 방식에는 단점이 있습니다:

- 뷰는 테이블이 아닙니다. 각 방언마다 갱신 가능한 뷰에 대한 제약이 있어, 뷰에 데이터를 기록하기 어려울 수 있습니다.
- 뷰는 별도의 데이터베이스 객체로서 버전 관리가 필요합니다.
- 개발자는 뷰를 조회할지 기본 테이블을 조회할지 선택해야 합니다.

## jOOQ의 클라이언트 사이드 솔루션

jOOQ 3.17은 코드 생성 설정을 통해 클라이언트 사이드에서 계산 컬럼을 정의할 수 있게 하여 이러한 제한을 극복합니다. 가상 클라이언트 사이드 계산 컬럼은 SELECT, WHERE, RETURNING 등 비쓰기 위치에서 해당 표현식으로 대체됩니다. 저장 버전은 INSERT, UPDATE, MERGE 등 쓰기 작업 중에 값을 계산합니다.

## Sakila 데이터베이스 예제: full_name과 full_address

### 합성 컬럼 설정

Sakila 데이터베이스에서 Maven 코드 생성기 설정을 사용하여 합성 컬럼을 구성합니다:

```xml
<syntheticObjects>
    <columns>
        <column>
            <tables>customer|staff|store</tables>
            <name>full_address</name>
            <type>text</type>
        </column>
        <column>
            <tables>customer|staff</tables>
            <name>full_name</name>
            <type>text</type>
        </column>
    </columns>
</syntheticObjects>
```

### 계산 표현식 정의

계산 정의는 forcedTypes를 사용하여 구성합니다:

```xml
<forcedTypes>
    <forcedType>
        <generator>ctx -> DSL.concat(
            FIRST_NAME, DSL.inline(" "), LAST_NAME)
        </generator>
        <includeExpression>full_name</includeExpression>
    </forcedType>
    <forcedType>
        <generator>ctx -> DSL.concat(
            address().ADDRESS_,
            DSL.inline(", "),
            address().POSTAL_CODE,
            DSL.inline(", "),
            address().city().CITY_,
            DSL.inline(", "),
            address().city().country().COUNTRY_
        )</generator>
        <includeExpression>full_address</includeExpression>
    </forcedType>
</forcedTypes>
```

이 설정은 성과 이름을 연결하는 `full_name` 합성 컬럼과, 암시적 조인을 통해 주소, 우편번호, 도시, 국가를 결합하는 `full_address` 합성 컬럼을 추가합니다.

### 컬럼 조회

Java 코드로 필드를 선택합니다:

```java
Result<Record2<String, String>> result =
ctx.select(CUSTOMER.FULL_NAME, CUSTOMER.FULL_ADDRESS)
   .from(CUSTOMER)
   .fetch();
```

jOOQ는 자동으로 암시적 조인을 관리하며, 연결된 이름과 전체 주소를 상관 테이블 관계를 통해 계산하는 여러 별칭 테이블 조인이 포함된 쿼리를 생성합니다. 프로젝션에서 불필요한 컬럼을 생략하면, 생성된 쿼리에서 불필요한 조인도 제거됩니다.

## 고급 금융 예제: 통화 변환

통화 변환을 사용하는 거래 시스템은 동적 계산을 보여줍니다.

### 스키마

```sql
CREATE TABLE currency (
  code CHAR(3) NOT NULL,
  PRIMARY KEY (code)
);

CREATE TABLE conversion (
  from_currency CHAR(3) NOT NULL,
  to_currency CHAR(3) NOT NULL,
  rate NUMERIC(18, 2) NOT NULL,
  PRIMARY KEY (from_currency, to_currency),
  FOREIGN KEY (from_currency) REFERENCES currency,
  FOREIGN KEY (to_currency) REFERENCES currency
);

CREATE TABLE transaction (
  id BIGINT NOT NULL,
  amount NUMERIC(18, 2) NOT NULL,
  currency CHAR(3) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (currency) REFERENCES currency
);
```

### 전통적인 뷰 접근 방식

```sql
CREATE VIEW v_transaction AS
SELECT
  id, amount, currency,
  amount * (
    SELECT c.rate
    FROM conversion AS c
    WHERE c.from_currency = t.currency
    AND c.to_currency = 'USD'
  ) AS amount_usd
FROM transaction AS t
```

### jOOQ 설정

Maven 설정에서 합성 컬럼과 강제 타입을 구성합니다:

```xml
<configuration>
    <generator>
        <database>
            <syntheticObjects>
                <columns>
                    <column>
                        <tables>TRANSACTION</tables>
                        <name>AMOUNT_USD</name>
                        <type>NUMERIC</type>
                    </column>
                    <column>
                        <tables>TRANSACTION</tables>
                        <name>AMOUNT_USER_CURRENCY</name>
                        <type>NUMERIC</type>
                    </column>
                </columns>
            </syntheticObjects>

            <forcedTypes>
                <forcedType>
                    <generator>ctx -> AMOUNT.times(DSL.field(
   DSL.select(Conversion.CONVERSION.RATE)
      .from(Conversion.CONVERSION)
      .where(Conversion.CONVERSION.FROM_CURRENCY.eq(CURRENCY))
      .and(Conversion.CONVERSION.TO_CURRENCY.eq(
           DSL.inline("USD")))))
                    </generator>
                    <includeExpression>
                        TRANSACTION\.AMOUNT_USD
                    </includeExpression>
                </forcedType>
                <forcedType>
                    <generator>ctx -> AMOUNT.times(DSL.field(
    DSL.select(Conversion.CONVERSION.RATE)
       .from(Conversion.CONVERSION)
       .where(Conversion.CONVERSION.FROM_CURRENCY.eq(CURRENCY))
       .and(Conversion.CONVERSION.TO_CURRENCY.eq(
           (String) ctx.configuration().data("USER_CURRENCY")))))
                    </generator>
                    <includeExpression>
                        TRANSACTION\.AMOUNT_USER_CURRENCY
                    </includeExpression>
                </forcedType>
            </forcedTypes>
        </database>
    </generator>
</configuration>
```

`AMOUNT_USD` 컬럼은 하드코딩된 "USD" 환율을 사용하여 금액을 변환합니다. `AMOUNT_USER_CURRENCY` 컬럼은 `ctx.configuration().data("USER_CURRENCY")`를 통해 사용자별 통화 변환을 동적으로 적용합니다.

### Java 쿼리 실행

```java
ctx.select(
        TRANSACTION.ID,
        TRANSACTION.AMOUNT,
        TRANSACTION.CURRENCY,
        TRANSACTION.AMOUNT_USD,
        TRANSACTION.AMOUNT_USER_CURRENCY,
        sum(TRANSACTION.AMOUNT_USD).over().as("total_usd"),
        sum(TRANSACTION.AMOUNT_USER_CURRENCY).over()
            .as("total_user_currency"))
   .from(TRANSACTION)
   .orderBy(TRANSACTION.ID)
   .fetch()
```

### 생성된 SQL

jOOQ가 생성하는 SQL은 다음과 같습니다:

```sql
select
  TRANSACTION.ID,
  TRANSACTION.AMOUNT,
  TRANSACTION.CURRENCY,
  (TRANSACTION.AMOUNT * (
    select CONVERSION.RATE
    from CONVERSION
    where (
      CONVERSION.FROM_CURRENCY = TRANSACTION.CURRENCY
      and CONVERSION.TO_CURRENCY = 'USD'
    )
  )) AMOUNT_USD,
  (TRANSACTION.AMOUNT * (
    select CONVERSION.RATE
    from CONVERSION
    where (
      CONVERSION.FROM_CURRENCY = TRANSACTION.CURRENCY
      and CONVERSION.TO_CURRENCY = null
    )
  )) AMOUNT_USER_CURRENCY,
  sum((TRANSACTION.AMOUNT * (...))) over () total_usd,
  sum((TRANSACTION.AMOUNT * (...))) over () total_user_currency
from TRANSACTION
order by TRANSACTION.ID
```

### 동적 사용자 컨텍스트

사용자 통화를 동적으로 설정합니다:

```java
ctx.configuration().data("USER_CURRENCY", "CHF");
```

이 접근 방식을 사용하면 데이터베이스 스키마 수정 없이 컨텍스트 변수에 따라 자동으로 업데이트되는 "임의의 뷰"를 만들 수 있습니다.

## 주요 장점

- 복잡한 로직을 생성된 코드 내에 캡슐화합니다
- 암시적 조인과 상관 서브쿼리를 지원합니다
- 설정 데이터를 통해 컨텍스트 인식 계산이 가능합니다
- 일반적인 변환에 대한 쿼리 보일러플레이트를 제거합니다

## 고급 기능: 중첩 레코드와 컬렉션

클라이언트 사이드 계산 컬럼은 쿼리 실행 중에 확장되는 표현식을 참조하는 변수처럼 작동합니다. 다음과 같은 고급 구성도 지원합니다:

- `ROW` 생성자를 통한 중첩 레코드
- `MULTISET` 생성자를 통한 중첩 컬렉션

## 중요한 주의사항

서버 사이드 가상 계산 컬럼과 달리, 이 컬럼에는 인덱스를 생성할 수 없습니다. 서버가 해당 컬럼이나 표현식에 대해 전혀 알지 못하기 때문입니다. 따라서 이 기능은 필터 조건보다는 프로젝션과 집계에 더 적합합니다.

## 향후 기능

향후 블로그 포스트에서는 클라이언트 사이드 계산 컬럼의 STORED 버전에 대해 다룰 예정이며, 새로운 감사 컬럼(audit column) 기능도 포함됩니다. 합성 컬럼이 아닌 실제 스키마 컬럼에 `Generator`를 구현하면 다른 동작이 발생합니다.
