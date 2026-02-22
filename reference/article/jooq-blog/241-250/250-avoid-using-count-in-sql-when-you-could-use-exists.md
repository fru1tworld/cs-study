# EXISTS()를 사용할 수 있을 때 SQL에서 COUNT(*)를 사용하지 마세요

> 원문: https://blog.jooq.org/avoid-using-count-in-sql-when-you-could-use-exists/

이전 글에서 불필요한 `COUNT(*)` 쿼리를 피하고 동등한 `EXISTS` 쿼리로 대체하는 것의 중요성에 대해 논의한 적이 있습니다.

## 핵심 원칙

> "EXISTS가 충분한 경우에는 COUNT(*)를 사용하지 말라"

## 근거

`COUNT(*)`는 일치하는 행의 정확한 개수를 반환해야 하지만, `EXISTS`는 행이 존재하는지 여부만 답하면 됩니다. `EXISTS`는 첫 번째 일치하는 행을 찾은 후 즉시 종료할 수 있어, 결과를 세는 것이 아니라 존재 여부를 확인할 때 더 효율적입니다.

## 쿼리 예제

문제가 있는 COUNT(*) 사용 쿼리:

```sql
SELECT count(*)
FROM actor a
JOIN film_actor fa USING (actor_id)
WHERE a.last_name = 'WAHLBERG'
```

EXISTS()를 사용한 최적화된 쿼리:

```sql
SELECT EXISTS (
  SELECT 1 FROM actor a
  JOIN film_actor fa USING (actor_id)
  WHERE a.last_name = 'WAHLBERG'
)
```

## 실행 계획(Execution Plan)

Oracle 실행 계획:
- COUNT(*): 비용(Cost) = 3
- EXISTS: 비용(Cost) = 2

PostgreSQL 실행 계획:
- COUNT(*): 비용(Cost) ≈ 123
- EXISTS: 비용(Cost) ≈ 3.4

## 성능 벤치마크

### Oracle 벤치마크 (10,000회 반복)

- EXISTS 구문: ~3 시간 단위
- COUNT(*) 구문: ~4 시간 단위
- 개선: EXISTS가 1.3배 빠름

### PostgreSQL 벤치마크 (1,000회 반복)

```
INFO:  Statement 1: 00:00:00.023656  (EXISTS)
INFO:  Statement 2: 00:00:00.7944    (COUNT(*))
```

- 개선: EXISTS가 40배 빠름

## Java/컬렉션 유사 사례

이 원칙은 SQL뿐만 아니라 프로그래밍 언어에도 동일하게 적용됩니다.

좋지 않은 방법:

```java
if (collection.size() == 0)
    doSomething();
```

더 나은 방법:

```java
if (!collection.isEmpty())
    doSomething();
```

`isEmpty()`는 존재 여부만 확인하는 반면, `size()`는 정확한 개수를 불필요하게 계산할 수 있습니다. Hibernate와 같은 일부 ORM 프레임워크는 존재 여부만 필요한 경우에도 정확한 크기를 계산하기 위해 비용이 많이 드는 쿼리를 실행할 수 있습니다.

## Oracle PL/SQL 벤치마크 코드

```sql
SET SERVEROUTPUT ON
DECLARE
  v_ts TIMESTAMP WITH TIME ZONE;
  v_repeat CONSTANT NUMBER := 10000;
BEGIN
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT CASE WHEN EXISTS (
        SELECT 1 FROM actor a
        JOIN film_actor fa USING (actor_id)
        WHERE a.last_name = 'WAHLBERG'
      ) THEN 1 ELSE 0 END
      FROM dual
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Statement 1 : ' || (SYSTIMESTAMP - v_ts));
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT count(*)
      FROM actor a
      JOIN film_actor fa USING (actor_id)
      WHERE a.last_name = 'WAHLBERG'
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Statement 2 : ' || (SYSTIMESTAMP - v_ts));
END;
/
```

## PostgreSQL PL/pgSQL 벤치마크 코드

```sql
DO $$
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT INT := 1000;
  rec RECORD;
BEGIN
  v_ts := clock_timestamp();

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT CASE WHEN EXISTS (
        SELECT 1 FROM actor a
        JOIN film_actor fa USING (actor_id)
        WHERE a.last_name = 'WAHLBERG'
      ) THEN 1 ELSE 0 END
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  RAISE INFO 'Statement 1: %', (clock_timestamp() - v_ts);
  v_ts := clock_timestamp();

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT count(*)
      FROM actor a
      JOIN film_actor fa USING (actor_id)
      WHERE a.last_name = 'WAHLBERG'
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  RAISE INFO 'Statement 2: %', (clock_timestamp() - v_ts);
END$$;
```

## jOOQ 구현

jOOQ에서 EXISTS를 사용하는 방법:

```java
select(field(exists(...)))
```

또는 편의 API 사용:

```java
DSLContext.fetchExists(Select)
```

## 결론

존재 여부만 확인하면 되는 경우, 정확한 개수를 계산하는 것보다 존재 여부를 확인하는 것이 본질적으로 더 빠릅니다. 이 원칙은 데이터베이스(Oracle, PostgreSQL, MySQL)와 프로그래밍 언어 전반에 걸쳐 적용됩니다. 애플리케이션 로직이 "이것이 존재하는가?"만 알면 되고 "몇 개가 있는가?"가 필요하지 않다면, 모든 주요 데이터베이스에서 상당한 성능 향상을 위해 `EXISTS`를 사용하세요.
