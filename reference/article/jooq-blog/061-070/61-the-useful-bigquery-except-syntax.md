# 유용한 BigQuery * EXCEPT 구문

> 원문: https://blog.jooq.org/the-useful-bigquery-except-syntax/

jOOQ를 사용하고 만들면서 가장 멋진 점 중 하나는, 벤더들이 표준 SQL 언어에 추가한 최고의 확장 기능들을 발견하고, 이러한 절들을 에뮬레이션을 통해 jOOQ에서 지원할 수 있다는 것입니다.

## 문제점

`SELECT *`를 사용할 때, `LAST_UPDATE`와 같은 원치 않는 컬럼이 `NATURAL JOIN`과 같은 연산을 방해할 수 있습니다. 다음 쿼리에서 이 문제를 확인할 수 있습니다:

```sql
SELECT actor_id, a.first_name, a.last_name, count(fa.film_id)
FROM actor AS a
NATURAL LEFT JOIN film_actor AS fa
GROUP BY actor_id
```

이 쿼리는 영화가 없는 배우들을 반환하는데, 이는 `LAST_UPDATE` 컬럼이 의도치 않게 조인 조건에 포함되어 의도하지 않은 필터링이 발생하기 때문입니다. `actor` 테이블과 `film_actor` 테이블 모두에 `last_update` 타임스탬프 컬럼이 있어서 `NATURAL JOIN`이 이 컬럼도 조인 조건으로 사용하게 됩니다.

## 해결책: * EXCEPT 구문

BigQuery는 `* EXCEPT`를 사용하여 특정 컬럼을 제외할 수 있습니다:

```sql
SELECT * EXCEPT (last_update) FROM actor
```

## 예제 쿼리

수정된 전체 쿼리는 다음과 같습니다:

```sql
SELECT
  a.actor_id,
  a.first_name,
  a.last_name,
  count(fa.film_id)
FROM (
  SELECT * EXCEPT (last_update) FROM actor
) AS a
NATURAL LEFT JOIN (
  SELECT * EXCEPT (last_update) FROM film_actor
) AS fa
GROUP BY
  a.actor_id,
  a.first_name,
  a.last_name
```

## jOOQ 에뮬레이션

jOOQ는 이 BigQuery 구문을 PostgreSQL과 호환되는 표준 SQL로 변환합니다:

```sql
SELECT
  a.actor_id,
  a.first_name,
  a.last_name,
  count(fa.film_id)
FROM (
  SELECT actor.actor_id, actor.first_name, actor.last_name
  FROM actor
) a
  NATURAL LEFT OUTER JOIN (
    SELECT film_actor.actor_id, film_actor.film_id
    FROM film_actor
  ) fa
GROUP BY
  a.actor_id,
  a.first_name,
  a.last_name
```

## jOOQ를 사용한 Java 구현

```java
Actor a = ACTOR.as("a");
FilmActor fa = FILM_ACTOR.as("fa");

ctx.select(
        a.ACTOR_ID,
        a.FIRST_NAME,
        a.LAST_NAME,
        count(fa.FILM_ID))
   .from(
        select(asterisk().except(a.LAST_UPDATE)).from(a).asTable(a))
   .naturalLeftOuterJoin(
        select(asterisk().except(fa.LAST_UPDATE)).from(fa).asTable(fa))
   .groupBy(a.ACTOR_ID, a.FIRST_NAME, a.LAST_NAME)
   .fetch();
```

## 핵심 요점

`* EXCEPT` 구문은 특히 `NATURAL JOIN` 연산을 복잡하게 만드는 스키마 설계 문제를 다룰 때, 더 깔끔한 임시 SQL 쿼리를 작성하기 위한 "도구 체인에서 유용한 도구"를 제공합니다.
