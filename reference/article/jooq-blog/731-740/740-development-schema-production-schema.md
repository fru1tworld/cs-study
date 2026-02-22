# 개발 스키마, 프로덕션 스키마

> 원문: https://blog.jooq.org/development-schema-production-schema/

우리 대부분은 개발 데이터를 프로덕션 데이터와 분리합니다. 물리적으로든, 최소한 논리적으로든 말입니다. 데이터베이스를 배치하는 다양한 전략이 있습니다.

## 다중 환경 데이터베이스

많은 조직에서는 동일한 스키마 이름을 사용하지만 서로 다른 서버에 환경을 배치합니다. 하지만 데이터베이스 라이선스가 비싸기 때문에, 동일한 데이터베이스 박스에 여러 환경을 통합해야 하는 경우가 있습니다. 이 경우 스키마를 구분해야 합니다:

- DB_DEV
- DB_TEST
- DB_PROD

## 다중 테넌트 단일 박스

또 다른 시나리오는 여러 독립적인 사용자가 있는 블로깅 서버와 같은 경우입니다. 각 사용자는 격리된 테이블이 필요합니다. 이에 대한 해결책으로는 다음과 같은 방법들이 있습니다:

방법 1 - 다중 스키마:

- DB_USER1
- DB_USER2
- DB_USER3

방법 2 - 접두어가 붙은 테이블:

- DB.USER1_POSTS
- DB.USER1_COMMENTS
- DB.USER2_POSTS
- DB.USER2_COMMENTS

## 문제점

이러한 환경에서는 실행되는 모든 SQL 문에서 관련 환경(DEV, TEST, PROD) 또는 사용자(USER1, USER2, USER3)를 모든 데이터베이스 아티팩트에 패치해야 합니다. 이것은 수작업으로 하기에는 실수가 발생하기 쉬운 작업입니다.

## jOOQ의 해결책

jOOQ는 스키마 매핑(SchemaMapping)을 통해 이 문제를 자동으로 해결합니다. 개발 스키마에서 코드를 생성한 후, 런타임에 쿼리를 다른 환경으로 리다이렉트하는 매핑을 만들 수 있습니다.

### 기본 jOOQ 쿼리 (개발 환경)

다음은 개발 스키마에서 실행되는 기본적인 jOOQ 쿼리입니다. 표준 쿼리는 자동으로 스키마 접두어를 포함합니다:

```java
OracleFactory create = new OracleFactory(connection);
create.select(TEXT)
      .from(POSTS)
      .fetch();

// jOOQ가 생성하는 SQL:
// SELECT "DB_DEV"."POSTS"."TEXT" FROM "DB_DEV"."POSTS"
```

### 프로덕션 환경을 위한 스키마 매핑

DB_DEV 대신 DB_PROD를 스키마로 렌더링하도록 jOOQ에 지시하는 매핑을 생성합니다. SchemaMapping은 생성된 모든 SQL에서 개발 스키마 이름을 프로덕션 스키마 이름으로 대체합니다:

```java
// DB_DEV 대신 jOOQ가 DB_PROD를 스키마로 렌더링하도록
// 매핑을 생성합니다
SchemaMapping mapping = new SchemaMapping();
mapping.add(DB_DEV, "DB_PROD");
OracleFactory create = new OracleFactory(connection, mapping);
create.select(TEXT)
      .from(POSTS)
      .fetch();

// jOOQ가 생성하는 SQL:
// SELECT "DB_PROD"."POSTS"."TEXT" FROM "DB_PROD"."POSTS"
```

### 다중 테넌트를 위한 테이블 접두어 매핑

일부 테이블의 이름을 변경하는 매핑을 생성합니다. 테이블 매핑은 사용자별 데이터 격리를 위해 자동으로 테이블 이름에 접두어를 붙입니다:

```java
// 일부 테이블의 이름을 변경하는 매핑을 생성합니다
SchemaMapping mapping = new SchemaMapping();
mapping.add(POSTS, "USER1_POSTS");
mapping.add(COMMENTS, "USER1_COMMENTS");
OracleFactory create = new OracleFactory(connection, mapping);
create.select(TEXT, COMMENT)
      .from(POSTS)
      .join(COMMENTS)
      .using(POST_ID)
      .fetch();

// jOOQ가 생성하는 SQL:
// SELECT "DB"."USER1_POSTS"."TEXT",
//        "DB"."USER1_COMMENTS"."COMMENT"
//   FROM "DB"."USER1_POSTS"
//   JOIN "DB"."USER1_COMMENTS"
// USING ("POST_ID")
```

## 결론

jOOQ는 SchemaMapping을 통해 이러한 스키마/접두어 패치 작업을 자동으로 처리함으로써, 수동으로 처리할 때 발생할 수 있는 실수를 줄여줍니다. 환경별 설정을 중앙에서 관리할 수 있어, 서로 다른 데이터베이스 인스턴스 간의 배포가 간소화됩니다.
