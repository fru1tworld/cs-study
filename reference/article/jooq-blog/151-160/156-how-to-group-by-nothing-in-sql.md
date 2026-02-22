# SQL에서 "아무것도 아닌 것"으로 GROUP BY하는 방법

> 원문: https://blog.jooq.org/how-to-group-by-nothing-in-sql/

이 글은 SQL의 잘 알려지지 않은 `GROUPING SETS` 기능, 특히 빈 그룹화 집합 `GROUP BY ()`를 사용하여 "아무것도 아닌 것"으로 그룹화하는 방법을 탐구합니다.

## 핵심 개념

`GROUP BY ()`와 `GROUP BY`를 생략하는 것은 미묘하게 다릅니다.

```sql
SELECT count(*)
FROM film
GROUP BY ()
```

결과: 1000

```sql
SELECT count(*)
FROM film
```

이것도 결과: 1000

## 결정적인 차이점

모든 행을 제거하는 WHERE 절을 추가할 때:

```sql
SELECT count(*)
FROM film
WHERE 1 = 0
GROUP BY ()
```

결과: 빈 결과 집합

```sql
SELECT count(*)
FROM film
WHERE 1 = 0
```

결과: 0 (값이 0인 한 행)

첫 번째 쿼리는 아무것도 생성하지 않습니다! 반면 두 번째 쿼리는 항상 정확히 한 행을 반환합니다.

## 데이터베이스 지원

네이티브 GROUPING SETS 지원:
- DB2 LUW, HANA, Oracle, PostgreSQL 9.5+, SQL Server, Sybase SQL Anywhere, Teradata

PostgreSQL 동작: Oracle과 SQL Server와 달리, PostgreSQL은 SQL 표준을 구현하여 일관되게 한 행을 반환합니다.

## 에뮬레이션 전략

GROUPING SETS를 지원하지 않는 데이터베이스의 경우:

방법 1 - 상수 리터럴:

```sql
GROUP BY 'a'
-- 또는
GROUP BY 'a' || 'b'
-- 또는
GROUP BY (SELECT 1)
```

지원 데이터베이스: Firebird, HSQLDB, MariaDB, MySQL, PostgreSQL, Redshift, SQLite, Vertica

방법 2 - 더미 테이블:

```sql
SELECT count(*)
FROM film, (SELECT 1 x) dummy
WHERE 1 = 0
GROUP BY dummy.x
```

지원 데이터베이스: Access, Informix, Ingres, SQL Data Warehouse, Sybase ASE

## 역사적 맥락

이 기능은 SQL:1999에서 `<grand total>`(총계)이라고 불렸으며, 이는 Excel 피벗 테이블의 총계와 유사합니다. 이것이 표준에서 입력 데이터가 비어 있더라도 항상 한 행을 반환하는 것을 선호하는 이유를 설명합니다.
