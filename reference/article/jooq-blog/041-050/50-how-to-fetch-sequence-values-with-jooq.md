# jOOQ로 시퀀스 값을 가져오는 방법

> 원문: https://blog.jooq.org/how-to-fetch-sequence-values-with-jooq/

## 개요

이 글에서는 다양한 데이터베이스 시스템에서 jOOQ를 사용하여 SQL 시퀀스의 값을 조회하는 방법을 설명합니다.

## 기본 시퀀스 조회

시퀀스를 생성하는 표준 SQL 구문은 다음과 같습니다:

```sql
CREATE SEQUENCE s;
```

jOOQ의 코드 생성 기능을 사용하면, 다음과 같이 시퀀스 값을 가져올 수 있습니다:

```java
System.out.println(ctx.fetchValue(S.nextval()));
```

## 데이터베이스 방언(Dialect) 변환

시퀀스 표현식은 다양한 데이터베이스 시스템에 맞게 변환됩니다:

- CockroachDB, PostgreSQL, YugabyteDB: `nextval('s')`
- Db2, HANA, Informix, Oracle, Snowflake, Sybase SQL Anywhere, Vertica: `s.nextval`
- Derby, Firebird, H2, HSQLDB, MariaDB, SQL Server: `NEXT VALUE FOR s`

## 고급 사용법

`S.nextval()` 필드 표현식을 SELECT 문에 포함시킬 수 있습니다:

```java
ctx.select(S.nextval()).fetch();
```

여러 시퀀스 값을 동시에 가져오려면 `nextvals()`를 사용합니다:

```java
System.out.println(ctx.fetchValues(S.nextvals(10)));
```

이 코드는 `[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]`과 같은 결과를 출력합니다.

내부적으로 이 쿼리는 jOOQ의 `GENERATE_SERIES` 에뮬레이션을 활용합니다. H2의 경우 다음과 같은 SQL이 생성됩니다:

```sql
SELECT NEXT VALUE FOR s
FROM system_range(1, 10)
```
