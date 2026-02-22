# SQL UNPIVOT으로 동료를 감동시켜라!

> 원문: https://blog.jooq.org/impress-your-coworkers-by-using-sql-unpivot/

이 글에서는 Stack Overflow에 올라온 흥미로운 질문을 다룹니다. 질문자는 Oracle에서 다음과 같은 형식의 결과 테이블을 생성하고자 했습니다:

```
Description   COUNT
TEST1         10
TEST2         15
TEST3         25
TEST4         50
```

여기서 각 COUNT 값은 서로 다른 조건에 해당합니다:
- TEST1: sal < 10000인 직원 수
- TEST2: dept > 10인 직원 수
- TEST3: 최근 60일 이내에 채용된 직원 수
- TEST4: grade = 1인 직원 수

## 비효율적인 접근 방식: 중첩 서브쿼리

가장 단순한 접근 방식은 각 조건에 대해 별도의 `SELECT COUNT(*)` 문을 작성하는 것입니다:

```sql
SELECT 'TEST1' AS description, (SELECT COUNT(*) FROM employees WHERE sal < 10000) AS count FROM dual
UNION ALL
SELECT 'TEST2' AS description, (SELECT COUNT(*) FROM employees WHERE dept > 10) AS count FROM dual
UNION ALL
SELECT 'TEST3' AS description, (SELECT COUNT(*) FROM employees WHERE hiredate > (SYSDATE - 60)) AS count FROM dual
UNION ALL
SELECT 'TEST4' AS description, (SELECT COUNT(*) FROM employees WHERE grade = 1) AS count FROM dual;
```

하지만 이 방식은 employees 테이블에 네 번이나 접근해야 하므로 매우 비효율적입니다. 더 나은 방법이 있습니다!

## 개선된 방법: CASE WHEN과 COUNT 활용

집계 함수는 NULL이 아닌 값만 집계한다는 사실을 활용하면, 단일 테이블 스캔으로 모든 조건을 처리할 수 있습니다:

```sql
SELECT
  COUNT(CASE WHEN sal < 10000 THEN 1 END) AS test1,
  COUNT(CASE WHEN dept > 10 THEN 1 END) AS test2,
  COUNT(CASE WHEN hiredate > (SYSDATE - 60) THEN 1 END) AS test3,
  COUNT(CASE WHEN grade = 1 THEN 1 END) AS test4
FROM employees;
```

이 쿼리의 핵심 원리는 간단합니다. `CASE WHEN` 표현식이 조건을 만족하면 1을 반환하고, 그렇지 않으면 NULL을 반환합니다. `COUNT` 함수는 NULL 값을 무시하므로, 결과적으로 각 조건을 만족하는 행의 개수만 세게 됩니다.

## PostgreSQL의 FILTER 절

PostgreSQL(그리고 SQL 표준)에서는 더 깔끔한 문법인 `FILTER` 절을 제공합니다:

```sql
SELECT
  COUNT(*) FILTER (WHERE sal < 10000) AS test1,
  COUNT(*) FILTER (WHERE dept > 10) AS test2,
  COUNT(*) FILTER (WHERE hiredate > (SYSDATE - 60)) AS test3,
  COUNT(*) FILTER (WHERE grade = 1) AS test4
FROM employees;
```

`FILTER` 절은 필터링 조건과 집계 로직을 더 명확하게 분리해줍니다. 의도를 더 명확하게 표현할 수 있어 가독성이 좋습니다.

## UNPIVOT으로 열을 행으로 변환하기

위의 쿼리들은 결과를 열(column) 형태로 반환합니다:

```
TEST1  TEST2  TEST3  TEST4
2      2      3      3
```

하지만 원래 질문자가 원했던 형식은 행(row) 형태였습니다. 여기서 `UNPIVOT`이 등장합니다!

`PIVOT`은 행을 열로 변환하고, `UNPIVOT`은 열을 행으로 변환합니다. 최적화된 CASE 쿼리를 파생 테이블로 감싸고 `UNPIVOT`을 적용하면 됩니다:

```sql
SELECT *
FROM (
  SELECT
    COUNT(CASE WHEN sal < 10000 THEN 1 END) AS test1,
    COUNT(CASE WHEN dept > 10 THEN 1 END) AS test2,
    COUNT(CASE WHEN hiredate > (SYSDATE - 60) THEN 1 END) AS test3,
    COUNT(CASE WHEN grade = 1 THEN 1 END) AS test4
  FROM employees
) t
UNPIVOT (
  count FOR description IN (
    test1 AS 'TEST1',
    test2 AS 'TEST2',
    test3 AS 'TEST3',
    test4 AS 'TEST4'
  )
);
```

이 쿼리는 다음과 같은 결과를 생성합니다:

```
DESCRIPTION   COUNT
TEST1         2
TEST2         2
TEST3         3
TEST4         3
```

정확히 원했던 형식입니다!

## 핵심 요약

1. 집계 함수는 NULL이 아닌 값만 집계합니다 - 이 특성을 활용하면 `CASE WHEN`으로 조건부 카운팅을 할 수 있습니다.

2. 단일 테이블 스캔 - 여러 개의 서브쿼리 대신 하나의 쿼리로 모든 조건을 처리하면 성능이 크게 향상됩니다.

3. PIVOT과 UNPIVOT은 보고서 작성에 매우 유용합니다 - 데이터를 행과 열 형식 사이에서 자유롭게 변환할 수 있습니다. Entity-Attribute-Value(EAV) 모델에서도 유용하게 활용됩니다.

## 데이터베이스 지원

`PIVOT`과 `UNPIVOT`은 Oracle과 SQL Server에서 지원됩니다. 다른 데이터베이스에서는 이 기능이 표준 SQL에 포함되어 있지 않아 직접 구현해야 할 수도 있습니다.

하지만 jOOQ를 사용하면 다양한 데이터베이스에서 일관된 방식으로 이러한 변환을 수행할 수 있습니다!
