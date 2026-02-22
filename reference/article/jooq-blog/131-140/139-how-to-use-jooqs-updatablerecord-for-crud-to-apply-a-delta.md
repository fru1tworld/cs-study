# jOOQ의 UpdatableRecord로 CRUD를 사용하여 델타 적용하기

> 원문: https://blog.jooq.org/how-to-use-jooqs-updatablerecord-for-crud-to-apply-a-delta/

게시일: 2018년 11월 5일, lukaseder 작성

## 개요

jOOQ는 일상적인 CRUD 작업을 위한 편의 레이어로 `UpdatableRecord` API를 제공합니다. 이것은 Hibernate와 같은 완전한 객체 그래프 ORM과는 다르지만, 여러 가지 유용한 기능을 포함하고 있습니다.

## 기반 테이블과의 1:1 매핑

각 `UpdatableRecord`는 데이터베이스 테이블이나 뷰에 직접 매핑됩니다. PostgreSQL 고객 테이블의 경우:

```sql
CREATE TABLE customer (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  vip BOOLEAN DEFAULT FALSE
);
```

코드 생성된 레코드를 사용하면 간단한 작업이 가능합니다:

```java
CustomerRecord customer = ctx.newRecord(CUSTOMER);
customer.setFirstName("John");
customer.setLastName("Doe");
customer.store();
```

이 코드는 다음과 같은 SQL을 생성합니다:

```sql
INSERT INTO customer (first_name, last_name)
VALUES (?, ?)
```

## SQL DEFAULT 표현식

명시적으로 설정된 컬럼만 생성된 SQL 구문에 포함되므로, 데이터베이스 기본값이 적용될 수 있습니다. 결과 레코드는 다음과 같이 포함됩니다:

```
id     first_name   last_name   vip
-------------------------------------
1337   John         Doe         false
```

기존 레코드를 업데이트할 때는 수정된 컬럼만 포함됩니다:

```java
CustomerRecord customer = ctx.fetchOne(
  CUSTOMER, CUSTOMER.ID.eq(1337));
customer.setVip(null);
customer.store();
```

다음과 같은 SQL이 생성됩니다:

```sql
UPDATE customer SET vip = ? WHERE id = ?
```

장점: 불완전한 레코드 페치도 잘 작동합니다. 명시적인 NULL과 DEFAULT 적용을 구분할 수 있습니다.

단점: 여러 가지 가능한 쿼리가 데이터베이스 실행 계획 캐싱에 영향을 미칠 수 있습니다.

## Record.changed() 추적

jOOQ는 `Record.changed()` 플래그를 사용하여 명시적으로 설정된 NULL 값과 데이터베이스 기본값을 사용해야 하는 설정되지 않은 컬럼을 구분합니다. 이는 JavaScript의 `null`과 `undefined` 구분과 유사합니다.

## POJO 대 레코드

Plain Old Java Object(POJO)는 "undefined"(정의되지 않음)와 명시적인 null 값의 차이를 인코딩할 수 없습니다. POJO를 레코드에 로드하면 모든 `Record.changed()` 플래그가 true로 설정되어 불필요한 컬럼 업데이트가 발생합니다.

다음 JSON을 고려해 보세요:

```json
{
  "id": 1337,
  "lastName": "Smith"
}
```

이상적으로는 다음과 같은 SQL이 생성되어야 합니다:

```sql
UPDATE customer SET last_name = ? WHERE id = ?
```

하지만 POJO는 모든 값을 강제로 업데이트합니다. 델타 의미론을 보존하려면 중간 POJO를 사용하는 대신 JSON을 레코드에 직접 로드하세요.

## 핵심 장점

이 접근 방식은 대상 SQL 구문을 생성하여 실행 계획 캐시 효율성을 유지하면서 데이터베이스 수준의 기본값을 존중하는 선택적 업데이트를 가능하게 합니다.
