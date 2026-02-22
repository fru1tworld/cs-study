# SQL FILTER 절의 성능 영향

> 원문: https://blog.jooq.org/the-performance-impact-of-sqls-filter-clause/

최근 트위터에서 흥미로운 질문을 발견했습니다. SQL(특히 PostgreSQL)에서 `FILTER`를 사용하는 것이 성능에 영향을 미치는 걸까요, 아니면 집계 함수에서 `CASE` 표현식의 단순한 구문 설탕(syntax sugar)에 불과할까요?

간단히 상기하자면, `FILTER`는 SQL에서 집계하기 전에 값을 필터링할 수 있는 훌륭한 표준 SQL 확장입니다. 이는 단일 쿼리에서 [여러 항목을 집계](https://blog.jooq.org/how-to-calculate-multiple-aggregate-functions-in-a-single-query/)할 때 매우 유용합니다.

[PostgreSQL 9.4의 멋진 SQL2003 FILTER 절](https://blog.jooq.org/the-awesome-postgresql-9-4-sql2003-filter-clause-for-aggregate-functions/)에 대한 이전 블로그 게시물을 참고하세요.

이 두 가지는 동일합니다:

```sql
SELECT
  fa.actor_id,
  SUM(length) FILTER (WHERE rating = 'R'),
  SUM(length) FILTER (WHERE rating = 'PG'),
  SUM(CASE WHEN rating = 'R' THEN length END),
  SUM(CASE WHEN rating = 'PG' THEN length END)
FROM film_actor AS fa
LEFT JOIN film AS f ON f.film_id = fa.film_id
GROUP BY fa.actor_id
```

하지만 본론으로 돌아가겠습니다. 성능 측면에서 정말로 차이가 있을까요? 차이가 있어야 할까요? 당연히, 차이가 있어서는 안 됩니다. 두 가지 유형의 집계 함수 표현식은 정확히 같은 의미임을 증명할 수 있습니다. 그리고 사실, 이것이 바로 다른 SQL 방언에서 `FILTER`를 사용할 때 jOOQ가 하는 일입니다. 위의 쿼리를 우리의 SQL 변환 도구에 넣고, 예를 들어 Oracle로 변환하면 다음과 같은 결과를 얻게 됩니다:

```sql
SELECT fa.actor_id,
  sum(CASE WHEN rating = 'R' THEN length END),
  sum(CASE WHEN rating = 'PG' THEN length END),
  sum(CASE WHEN rating = 'R' THEN length END),
  sum(CASE WHEN rating = 'PG' THEN length END)
FROM film_actor fa
  LEFT JOIN film f ON f.film_id = fa.film_id
GROUP BY fa.actor_id
```

반대 방향의 변환도 옵티마이저에서 가능해야 합니다.

## 성능 차이가 있을까?

하지만 실제로 이것이 수행되고 있을까요? [sakila 데이터베이스](https://www.jooq.org/sakila)에서 PostgreSQL을 대상으로 다음 2개의 쿼리를 비교해 봅시다:

쿼리 1 (FILTER 구문):

```sql
SELECT
  fa.actor_id,
  SUM(length) FILTER (WHERE rating = 'R'),
  SUM(length) FILTER (WHERE rating = 'PG')
FROM film_actor AS fa
LEFT JOIN film AS f
  ON f.film_id = fa.film_id
GROUP BY fa.actor_id
```

쿼리 2 (CASE 구문):

```sql
SELECT
  fa.actor_id,
  SUM(CASE WHEN rating = 'R' THEN length END),
  SUM(CASE WHEN rating = 'PG' THEN length END)
FROM film_actor AS fa
LEFT JOIN film AS f
  ON f.film_id = fa.film_id
GROUP BY fa.actor_id
```

이 [벤치마크 기법](https://www.jooq.org/benchmark)을 사용하며, 벤치마크 코드는 이 블로그 게시물 끝에 게시하겠습니다. 각 쿼리를 500번 실행한 결과는 명확합니다(시간이 적을수록 좋습니다):

```
Run 1, Statement 1: 00:00:00.786621
Run 1, Statement 2: 00:00:00.839966

Run 2, Statement 1: 00:00:00.775477
Run 2, Statement 2: 00:00:00.829746

Run 3, Statement 1: 00:00:00.774942
Run 3, Statement 2: 00:00:00.834745

Run 4, Statement 1: 00:00:00.776973
Run 4, Statement 2: 00:00:00.836655

Run 5, Statement 1: 00:00:00.775871
Run 5, Statement 2: 00:00:00.845209
```

Docker에서 PostgreSQL 15를 실행하는 제 머신에서 `CASE` 구문을 사용하면 `FILTER` 구문에 비해 일관되게 8%의 성능 패널티가 있습니다. 실제 비벤치마크 쿼리에서의 차이는 하드웨어와 데이터 세트에 따라 더 작거나 더 클 수 있습니다. 하지만 분명히, 이 경우 한쪽이 다른 쪽보다 약간 더 낫습니다.

이러한 유형의 구문은 일반적으로 리포팅 컨텍스트에서 사용되기 때문에, 그 차이는 분명히 중요할 수 있습니다.

## 보조 조건자(Auxiliary Predicate)를 통한 추가 최적화

`RATING` 열에 대한 조건자를 중복으로 추가하면 추가적인 최적화 잠재력이 있다고 생각할 수 있습니다. 다음과 같이 말입니다:

쿼리 1 (보조 조건자가 있는 FILTER):

```sql
SELECT
  fa.actor_id,
  SUM(length) FILTER (WHERE rating = 'R'),
  SUM(length) FILTER (WHERE rating = 'PG')
FROM film_actor AS fa
LEFT JOIN film AS f
  ON f.film_id = fa.film_id
  AND rating IN ('R', 'PG')
GROUP BY fa.actor_id
```

쿼리 2 (보조 조건자가 있는 CASE):

```sql
SELECT
  fa.actor_id,
  SUM(CASE WHEN rating = 'R' THEN length END),
  SUM(CASE WHEN rating = 'PG' THEN length END)
FROM film_actor AS fa
LEFT JOIN film AS f
  ON f.film_id = fa.film_id
  AND rating IN ('R', 'PG')
GROUP BY fa.actor_id
```

이것이 결과를 왜곡하지 않으려면 `LEFT JOIN`의 `ON` 절에 배치해야 한다는 점에 유의하세요. 쿼리의 `WHERE` 절에는 배치할 수 없습니다. 이 차이에 대한 설명은 [여기](https://blog.jooq.org/10-cool-sql-optimisations-that-do-not-depend-on-the-cost-model/)에 있습니다. 이제 벤치마크 결과는 어떨까요?

```
Run 1, Statement 1: 00:00:00.701943
Run 1, Statement 2: 00:00:00.747103

Run 2, Statement 1: 00:00:00.69377
Run 2, Statement 2: 00:00:00.746252

Run 3, Statement 1: 00:00:00.684777
Run 3, Statement 2: 00:00:00.745419

Run 4, Statement 1: 00:00:00.688584
Run 4, Statement 2: 00:00:00.740979

Run 5, Statement 1: 00:00:00.688878
Run 5, Statement 2: 00:00:00.742864
```

예상대로, 중복 조건자가 성능을 개선했습니다(완벽한 세상에서는 그럴 필요가 없지만, 현실은 이렇습니다. 옵티마이저가 이것을 최적화할 수 있는 만큼 잘 최적화하지 못합니다). 하지만 여전히, `FILTER` 절이 `CASE` 절 사용보다 더 나은 성능을 보입니다.

## jOOQ의 FILTER 지원

jOOQ 3.17 기준으로, 다음 SQL 방언들이 `FILTER`를 네이티브로 지원하는 것으로 알려져 있습니다: CockroachDB, Firebird, H2, HSQLDB, PostgreSQL, SQLite, YugabyteDB.

## 결론

완벽한 세상에서는 증명 가능하게 동등한 두 SQL 구문이 동일한 성능을 보여야 합니다. 하지만 현실 세계에서는 항상 그런 것은 아닙니다. 옵티마이저는 다음 사이에서 트레이드오프를 합니다:

- 드물게 사용되는 구문을 최적화하는 데 드는 시간
- 쿼리를 실행하는 데 드는 시간

이것은 집계 함수의 간단한 표현식 패턴을 살펴볼 가치가 있는 경우라고 생각합니다. `AGG(CASE ..)`는 매우 인기 있는 관용구이고, 8%는 상당히 의미 있는 개선이므로, PostgreSQL이 이것을 수정해야 한다고 생각합니다. 두고 봐야겠죠. 어쨌든, `FILTER`는 이미:

- 더 나은 성능을 보이고
- 더 보기 좋기 때문에

이 멋진 표준 SQL 구문으로 지금 바로 안전하게 전환할 수 있습니다.

## 벤치마크 코드

```sql
DO $$
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT INT := 500;
  rec RECORD;
BEGIN

  -- 워밍업 패널티를 피하기 위해 전체 벤치마크를 여러 번 반복
  FOR r IN 1..5 LOOP
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT
          fa.actor_id,
          SUM(length) FILTER (WHERE rating = 'R'),
          SUM(length) FILTER (WHERE rating = 'PG')
        FROM film_actor AS fa
        LEFT JOIN film AS f
          ON f.film_id = fa.film_id
        GROUP BY fa.actor_id
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 1: %', r, (clock_timestamp() - v_ts);
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT
          fa.actor_id,
          SUM(CASE WHEN rating = 'R' THEN length END),
          SUM(CASE WHEN rating = 'PG' THEN length END)
        FROM film_actor AS fa
        LEFT JOIN film AS f
          ON f.film_id = fa.film_id
        GROUP BY fa.actor_id
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 2: %', r, (clock_timestamp() - v_ts);
    RAISE INFO '';
  END LOOP;
END$$;
```

보조 조건자가 포함된 벤치마크 코드:

```sql
DO $$
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT INT := 500;
  rec RECORD;
BEGIN

  -- 워밍업 패널티를 피하기 위해 전체 벤치마크를 여러 번 반복
  FOR r IN 1..5 LOOP
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT
          fa.actor_id,
          SUM(length) FILTER (WHERE rating = 'R'),
          SUM(length) FILTER (WHERE rating = 'PG')
        FROM film_actor AS fa
        LEFT JOIN film AS f
          ON f.film_id = fa.film_id
          AND rating IN ('R', 'PG')
        GROUP BY fa.actor_id
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 1: %', r, (clock_timestamp() - v_ts);
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT
          fa.actor_id,
          SUM(CASE WHEN rating = 'R' THEN length END),
          SUM(CASE WHEN rating = 'PG' THEN length END)
        FROM film_actor AS fa
        LEFT JOIN film AS f
          ON f.film_id = fa.film_id
          AND rating IN ('R', 'PG')
        GROUP BY fa.actor_id
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 2: %', r, (clock_timestamp() - v_ts);
    RAISE INFO '';
  END LOOP;
END$$;
```
