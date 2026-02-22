# 숨겨진 PL/SQL에서 SQL로의 컨텍스트 스위치에 주의하라

> 원문: https://blog.jooq.org/beware-of-hidden-pl-sql-to-sql-context-switches/

최근 고객사 시스템에서 다음 쿼리를 발견했습니다.

```sql
SELECT USER FROM SYS.DUAL
```

이 쿼리가 한 달에 수십억 번 실행되고 있었으며, 시스템 부하의 약 0.3%를 차지하고 있었습니다. 어떻게 이런 일이 발생했을까요?

## 문제의 발견

`V$SQL` 뷰를 조회하여 자주 실행되는 SQL을 확인할 수 있습니다.

```sql
SELECT
  sql_id,
  executions,
  elapsed_time,
  ratio_to_report(elapsed_time) over() p,
  sql_text
FROM v$sql
ORDER BY p DESC;
```

이 쿼리를 통해 어떤 SQL 문이 가장 많은 시간을 소비하는지 확인할 수 있습니다. 우리의 경우, `SELECT USER FROM SYS.DUAL` 쿼리가 상위에 있었습니다.

## 원인 분석

이 쿼리는 어디서 오는 것일까요? 조사 결과, Oracle의 `STANDARD.USER()` 함수에서 발생하는 것으로 밝혀졌습니다. 이 함수의 소스 코드를 확인하기 위해 다음 쿼리를 실행했습니다.

```sql
WITH s AS (
  SELECT s.*,
    MIN(CASE
      WHEN upper(text) LIKE '%FUNCTION USER%'
      THEN line END
    ) OVER () s
  FROM all_source s
  WHERE owner = 'SYS'
  AND name = 'STANDARD'
  AND type = 'PACKAGE BODY'
)
SELECT text
FROM s
WHERE line >= s AND line < s + 6;
```

그 결과, `STANDARD.USER` 함수의 구현을 확인할 수 있었습니다.

```sql
function USER return varchar2 is
c varchar2(255);
begin
  select user into c from sys.dual;
  return c;
end;
```

보시다시피, `USER` 함수는 내부적으로 `SELECT USER INTO c FROM SYS.DUAL` 쿼리를 실행합니다. 이것이 바로 PL/SQL에서 SQL로의 컨텍스트 스위치입니다. PL/SQL 코드가 실행 컨텍스트를 벗어나 SQL을 호출한 다음 다시 돌아오는 비용이 발생합니다.

우리 고객사의 경우, 이 `USER` 함수가 수많은 테이블의 감사(audit) 트리거에서 호출되고 있었습니다. 트리거가 실행될 때마다 `USER` 함수가 호출되어 감사 컬럼을 채우는데, 이로 인해 불필요한 컨텍스트 스위치가 수십억 번 발생하고 있었던 것입니다.

## 벤치마크

이 문제가 얼마나 심각한지 확인하기 위해 벤치마크를 수행했습니다. 세 가지 접근 방식을 비교했습니다.

1. `USER` 함수를 직접 사용 (내부적으로 `SELECT USER FROM DUAL` 실행)
2. 명시적으로 `SELECT USER FROM DUAL` 쿼리 실행
3. `SYS_CONTEXT('USERENV', 'CURRENT_USER')` 사용

다음은 벤치마크 코드입니다.

```sql
SET SERVEROUTPUT ON

ALTER SYSTEM FLUSH SHARED_POOL;
ALTER SYSTEM FLUSH BUFFER_CACHE;

CREATE TABLE results (
  run     NUMBER(2),
  stmt    NUMBER(2),
  elapsed NUMBER
);

DECLARE
  v_ts TIMESTAMP WITH TIME ZONE;
  v_repeat CONSTANT NUMBER := 500000;
  v NUMBER;
BEGIN

  FOR r IN 1..5 LOOP
    v_ts := SYSTIMESTAMP;

    -- Statement 1: USER 함수 직접 사용
    FOR i IN 1 .. v_repeat LOOP
      v := v + length(USER);
    END LOOP;

    INSERT INTO results VALUES (r, 1,
      SYSDATE + ((SYSTIMESTAMP - v_ts) * 86400) - SYSDATE);
    v_ts := SYSTIMESTAMP;

    -- Statement 2: 명시적 SELECT FROM DUAL
    FOR i IN 1 .. v_repeat LOOP
      SELECT v + length(USER) INTO v FROM dual;
    END LOOP;

    INSERT INTO results VALUES (r, 2,
      SYSDATE + ((SYSTIMESTAMP - v_ts) * 86400) - SYSDATE);
    v_ts := SYSTIMESTAMP;

    -- Statement 3: SYS_CONTEXT 사용
    FOR i IN 1 .. v_repeat LOOP
      v := v + length(sys_context('USERENV', 'CURRENT_USER'));
    END LOOP;

    INSERT INTO results VALUES (r, 3,
      SYSDATE + ((SYSTIMESTAMP - v_ts) * 86400) - SYSDATE);
  END LOOP;

  FOR rec IN (
    SELECT
      run, stmt,
      CAST(elapsed / MIN(elapsed) OVER() AS NUMBER(10, 5)) ratio,
      CAST(AVG(elapsed) OVER (PARTITION BY stmt) /
           MIN(elapsed) OVER() AS NUMBER(10, 5)) avg_ratio
    FROM results
    ORDER BY run, stmt
  )
  LOOP
    dbms_output.put_line('Run ' || rec.run ||
      ', Statement ' || rec.stmt ||
      ' : ' || rec.ratio || ' (avg : ' || rec.avg_ratio || ')');
  END LOOP;

  dbms_output.put_line('');
  dbms_output.put_line('Copyright Data Geekery GmbH');
  dbms_output.put_line('https://www.jooq.org/benchmark');
END;
/

DROP TABLE results;
```

## 벤치마크 결과

500,000번씩 5회 반복 실행한 결과는 다음과 같습니다.

```
Run 1, Statement 1 : 2.40509 (avg : 2.43158)
Run 1, Statement 2 : 2.13208 (avg : 2.11816)
Run 1, Statement 3 : 1.01452 (avg : 1.02081)

Run 2, Statement 1 : 2.41889 (avg : 2.43158)
Run 2, Statement 2 : 2.09753 (avg : 2.11816)
Run 2, Statement 3 : 1.00203 (avg : 1.02081)

Run 3, Statement 1 : 2.45384 (avg : 2.43158)
Run 3, Statement 2 : 2.09060 (avg : 2.11816)
Run 3, Statement 3 : 1.02239 (avg : 1.02081)

Run 4, Statement 1 : 2.39516 (avg : 2.43158)
Run 4, Statement 2 : 2.14140 (avg : 2.11816)
Run 4, Statement 3 : 1.06512 (avg : 1.02081)

Run 5, Statement 1 : 2.48493 (avg : 2.43158)
Run 5, Statement 2 : 2.12922 (avg : 2.11816)
Run 5, Statement 3 : 1.00000 (avg : 1.02081)
```

결과 분석:

- Statement 1 (`USER` 함수 사용): 평균 비율 약 2.43 (가장 느림)
- Statement 2 (명시적 `SELECT FROM DUAL`): 평균 비율 약 2.12
- Statement 3 (`SYS_CONTEXT` 사용): 평균 비율 약 1.02 (가장 빠름)

가장 빠른 결과(Run 5, Statement 3)를 1.0으로 정규화했습니다. 결과를 보면, 명시적 SQL 쿼리는 `SYS_CONTEXT` 호출보다 약 2배 느리며, `USER` 함수 참조는 그보다 약간 더 느립니다.

## 왜 이런 차이가 발생하는가?

`SYS_CONTEXT` 함수는 순수하게 PL/SQL 엔진 내에서 실행됩니다. SQL 엔진으로의 컨텍스트 스위치가 발생하지 않습니다. 반면, `USER` 함수와 `SELECT FROM DUAL`은 매번 SQL 엔진을 호출해야 하므로 컨텍스트 스위치 오버헤드가 발생합니다.

개별적으로 보면 이 오버헤드는 미미해 보일 수 있습니다. 하지만 프로덕션 시스템에서 수십억 번 반복되면 심각한 성능 병목이 됩니다.

## 권장 사항

성능이 중요한 코드, 특히 자주 실행되는 트리거에서 `USER` 함수 호출을 다음과 같이 대체하십시오.

```sql
-- 변경 전
v_user := USER;

-- 변경 후
v_user := sys_context('USERENV', 'CURRENT_USER');
```

이렇게 하면 불필요한 PL/SQL에서 SQL로의 컨텍스트 스위치를 제거하고 실행 효율성을 크게 향상시킬 수 있습니다. 우리의 벤치마크에서 약 2.4배의 성능 향상을 확인했습니다.

## 결론

숨겨진 성능 병목은 대규모 시스템에서 상당히 누적될 수 있습니다. PL/SQL과 SQL 간의 컨텍스트 스위칭 오버헤드는 개별적으로는 작지만, 프로덕션 시스템에서 수십억 번 반복되면 심각한 문제가 됩니다. Oracle의 표준 함수들이 내부적으로 어떻게 구현되어 있는지 이해하고, 성능이 중요한 경우 대안을 사용하는 것이 좋습니다.
