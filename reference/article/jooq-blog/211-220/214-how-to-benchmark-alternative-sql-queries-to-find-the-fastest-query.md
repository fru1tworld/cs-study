> 원문: https://blog.jooq.org/how-to-benchmark-alternative-sql-queries-to-find-the-fastest-query/

# 가장 빠른 쿼리를 찾기 위해 대안 SQL 쿼리를 벤치마크하는 방법

## 개요

이 글에서는 실행 계획(Execution Plan)이나 과거의 블로그 글, 또는 가정에 의존하는 대신 SQL 쿼리 성능을 직접 측정하는 것의 중요성을 다룹니다. 데이터베이스마다 쿼리를 다르게 최적화하기 때문에, 여러분의 특정 시스템에서 가장 빠른 접근 방식을 찾으려면 벤치마킹이 필수적입니다.

## 가정의 문제점

상관 서브쿼리(Correlated Subquery)가 LEFT JOIN보다 느린지, 또는 `COUNT(*)`가 `COUNT(1)`보다 성능이 좋은지와 같은 일반적인 SQL 미신에 의문을 제기할 필요가 있습니다. "SQL은 4세대 언어(4GL)이며... 프로그래머들은 실제로 수정 구슬을 들여다보고 있는 것과 같다"는 말처럼, SQL의 선언적 특성은 실제 성능에 대한 불확실성을 만들어냅니다.

## 왜 측정해야 하는가

실행 계획만 신뢰하기보다는 벤치마킹을 통한 직접 측정을 권장합니다. 벤치마킹의 장점은 다음과 같습니다:

- 여러 환경에서 쉽게 재현할 수 있음
- 성능 차이의 규모를 빠르게 파악할 수 있음

그러나 벤치마킹에는 한계도 있습니다. 여러 쿼리가 동시에 실행되는 운영 환경의 조건을 반영하지 못하며, 반복 실행되는 쿼리는 비현실적인 캐싱 효과의 혜택을 받을 수 있습니다.

## 비교 분석

Sakila 샘플 데이터베이스를 사용하여 세 개의 데이터베이스에서 의미적으로 동등한 두 쿼리를 비교한 벤치마크 결과입니다:

PostgreSQL 결과:
상관 서브쿼리 버전이 다섯 번의 테스트 실행에서 LEFT JOIN 접근 방식보다 약 60% 더 빠르게 수행되었습니다.

Oracle 결과:
상관 서브쿼리가 "Oracle에서 LEFT JOIN을 크게 능가했으며", 서브쿼리가 약 10배 더 빠르게 실행되었습니다.

SQL Server 결과:
LEFT JOIN이 "상관 서브쿼리를 크게 능가했으며", 두 쿼리가 동일한 실행 계획을 생성했음에도 불구하고 JOIN이 약 5배 더 빠르게 완료되었습니다.

## 코드 예제

PostgreSQL 벤치마크:
```sql
DO $$
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT INT := 10000;
  rec RECORD;
BEGIN
  FOR i IN 1..5 LOOP
    v_ts := clock_timestamp();
    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT first_name, last_name, count(fa.actor_id) AS c
        FROM actor a
        LEFT JOIN film_actor fa
        ON a.actor_id = fa.actor_id
        WHERE last_name LIKE 'A%'
        GROUP BY a.actor_id, first_name, last_name
        ORDER BY c DESC
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;
    RAISE INFO 'Run %, Statement 1: %', i, (clock_timestamp() - v_ts);
  END LOOP;
END$$;
```

Oracle 벤치마크:
```sql
SET SERVEROUTPUT ON
DECLARE
  v_ts TIMESTAMP WITH TIME ZONE;
  v_repeat CONSTANT NUMBER := 10000;
BEGIN
  FOR r IN 1..5 LOOP
    v_ts := SYSTIMESTAMP;
    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT first_name, last_name, count(fa.actor_id) AS c
        FROM actor a
        LEFT JOIN film_actor fa
        ON a.actor_id = fa.actor_id
        WHERE last_name LIKE 'A%'
        GROUP BY a.actor_id, first_name, last_name
        ORDER BY c DESC
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;
    dbms_output.put_line('Run ' || r || ', Statement 1 : ' || (SYSTIMESTAMP - v_ts));
  END LOOP;
END;
/
```

SQL Server 벤치마크:
```sql
DECLARE @ts DATETIME;
DECLARE @repeat INT = 10000;
DECLARE @r INT;

DECLARE @s1 CURSOR;

SET @r = 0;
WHILE @r < 5
BEGIN
  SET @r = @r + 1
  SET @s1 = CURSOR FOR
    SELECT first_name, last_name, count(fa.actor_id) AS c
    FROM actor a
    LEFT JOIN film_actor fa
    ON a.actor_id = fa.actor_id
    WHERE last_name LIKE 'A%'
    GROUP BY a.actor_id, first_name, last_name
    ORDER BY c DESC

  SET @ts = current_timestamp;
  SET @i = 0;
  WHILE @i < @repeat
  BEGIN
    SET @i = @i + 1
    OPEN @s1;
    FETCH NEXT FROM @s1 INTO @dummy1, @dummy2, @dummy3;
    WHILE @@FETCH_STATUS = 0
    BEGIN
      FETCH NEXT FROM @s1 INTO @dummy1, @dummy2, @dummy3;
    END;
    CLOSE @s1;
  END;
  DEALLOCATE @s1;
  PRINT 'Run ' + CAST(@r AS VARCHAR) + ', Statement 1: ' +
    CAST(DATEDIFF(ms, @ts, current_timestamp) AS VARCHAR) + 'ms';
END;
```

## 핵심 결론

"이렇게 간단한 쿼리에서조차 모든 데이터베이스에 최적인 쿼리는 없다"는 점을 강조합니다. 주요 시사점은 다음과 같습니다:

- 직관이나 과거의 성능 주장에 의존하지 마세요
- 실행 계획은 실제 런타임 동작에 대해 제한적인 통찰만 제공합니다
- 실제 데이터베이스 시스템에서 특정 쿼리를 벤치마크하세요
- 결과는 데이터베이스 플랫폼마다 크게 다릅니다
- 운영 환경의 성능은 벤치마크 조건과 다를 수 있습니다

근본적인 메시지: 가정이나 일반적인 규칙을 기반으로 최적화하기보다는 경험적으로 측정하세요.
