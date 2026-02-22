> 원문: https://blog.jooq.org/how-to-fetch-multiple-oracle-execution-plans-in-one-nice-query/

# 하나의 깔끔한 쿼리로 여러 Oracle 실행 계획을 가져오는 방법

Oracle에서 실행 계획(Execution Plan)을 조회할 때, 일반적으로 SQL ID를 미리 알아야 합니다. 하지만 LATERAL 언네스팅(unnesting)을 사용하면 여러 실행 계획을 한 번에 조회할 수 있습니다.

## 핵심 기법

LATERAL 언네스팅을 사용하면 `DBMS_XPLAN.DISPLAY_CURSOR`가 반환하는 중첩 테이블(nested table)을 호출 쿼리에서 언네스트할 수 있으며, `V$SQL` 테이블의 컬럼 값을 각 함수 호출에 전달할 수 있습니다.

## 기본 쿼리 방식

암시적 LATERAL 언네스팅을 사용하는 기본 방법입니다:

```sql
SELECT s.sql_id, p.*
FROM v$sql s, TABLE (
  dbms_xplan.display_cursor (
    s.sql_id, s.child_number, 'ALLSTATS LAST'
  )
) p
WHERE s.sql_text LIKE '%/*+GATHER_PLAN_STATISTICS*/%';
```

## 대안 구문 옵션

더 명시적인 두 가지 대안이 있습니다:

명시적 LATERAL 사용:

```sql
SELECT s.sql_id, p.*
FROM v$sql s CROSS JOIN LATERAL (SELECT * FROM TABLE (
  dbms_xplan.display_cursor (
    s.sql_id, s.child_number, 'ALLSTATS LAST'
  )
)) p
WHERE s.sql_text LIKE '%/*+GATHER_PLAN_STATISTICS*/%';
```

CROSS APPLY 사용 (SQL Server 스타일):

```sql
SELECT s.sql_id, p.*
FROM v$sql s CROSS APPLY TABLE (
  dbms_xplan.display_cursor (
    s.sql_id, s.child_number, 'ALLSTATS LAST'
  )
) p
WHERE s.sql_text LIKE '%/*+GATHER_PLAN_STATISTICS*/%';
```

## 실용적인 예제

이 기법은 서로 다른 쿼리 접근 방식을 비교할 때 유용합니다. 예를 들어, 성이 'A'로 시작하는 배우를 찾고 출연 영화 수를 세는 두 가지 방식(LEFT JOIN 사용 vs 서브쿼리 사용)의 실행 계획을 한 번의 쿼리 실행으로 모두 조회할 수 있습니다.

## 요약

- `LATERAL` 언네스팅을 활용하면 SQL ID를 미리 확인할 필요 없이 여러 실행 계획을 동시에 조회할 수 있습니다
- Oracle에서는 암시적 LATERAL, 명시적 LATERAL, CROSS APPLY 세 가지 구문을 모두 지원합니다
- `/*+GATHER_PLAN_STATISTICS*/` 힌트를 사용한 쿼리들의 실행 계획을 필터링하여 조회할 수 있습니다
