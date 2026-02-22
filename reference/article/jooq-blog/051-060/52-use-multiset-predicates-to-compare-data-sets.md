# MULTISET 술어를 사용하여 데이터 세트 비교하기

> 원문: https://blog.jooq.org/use-multiset-predicates-to-compare-data-sets/

## 문제 제기

"주어진 영화 X와 동일한 배우 세트를 가진 영화는 무엇인가?"라는 흥미로운 SQL 질문이 있습니다. Sakila 데이터베이스를 사용하여 동일한 배우 세트를 공유하는 영화를 식별하는 방법을 살펴보겠습니다.

## 배열 집계를 사용한 초기 접근 방식

첫 번째 해결 방법은 PostgreSQL의 `array_agg` 함수를 사용하여 영화별로 배우를 집계하는 것입니다:

```sql
SELECT
  film_id,
  array_agg(actor_id ORDER BY actor_id) actors
FROM film_actor
GROUP BY film_id
```

이렇게 하면 비교가 가능한 정렬된 배열이 생성됩니다. 배열은 리스트처럼 동작합니다. 즉, 순서를 유지하므로 배열을 명시적으로 정렬하는 것이 중요합니다.

일치하는 영화를 찾으려면 배우 배열 자체로 그룹화한 다음, 빈도순으로 결과를 정렬해야 합니다:

```sql
WITH t AS (
  SELECT film_id, array_agg(actor_id ORDER BY actor_id) actors
  FROM film_actor
  GROUP BY film_id
)
SELECT array_agg(film_id ORDER BY film_id) AS films, actors
FROM t
GROUP BY actors
ORDER BY count(*) DESC, films
```

결과: 영화 97과 556이 동일한 배우 세트를 공유하며, 단 한 명의 배우만 포함하고 있습니다.

## MULTISET 비교의 장점

jOOQ 3.15에서 도입된 덜 알려진 기능 중 하나는 MULTISET 비교 기능입니다. 배열과 달리, SQL 표준 MULTISET 비교에서는 순서가 무관합니다. 이러한 차이점 덕분에 jOOQ 코드에서 명시적인 ORDER BY 없이도 더 깔끔한 술어 로직을 작성할 수 있습니다. 다만 jOOQ는 비교 정확성을 위해 내부적으로 값을 정렬하여 처리합니다.

jOOQ 구현은 상관 서브쿼리를 사용하여 영화 간 배우 멀티셋을 비교하는 것을 보여줍니다:

```java
ctx.select(FILM.FILM_ID, FILM.TITLE)
   .from(FILM)
   .where(
       multiset(
           select(FILM_ACTOR.ACTOR_ID)
           .from(FILM_ACTOR)
           .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
       ).eq(multiset(
           select(FILM_ACTOR.ACTOR_ID)
           .from(FILM_ACTOR)
           .where(FILM_ACTOR.FILM_ID.eq(97L))
       ))
   )
   .orderBy(FILM_ID)
   .fetch();
```

생성된 PostgreSQL은 멀티셋 연산을 위해 JSONB 에뮬레이션을 사용하며, jOOQ 코드에 명시적 ORDER BY가 없음에도 불구하고 비교를 위해 내부적으로 배열을 정렬합니다.

## MULTISET_AGG를 사용한 대안

두 번째 접근 방식은 암시적 조인과 GROUP BY를 MULTISET_AGG와 결합하는 것입니다:

```java
ctx.select(FILM_ACTOR.FILM_ID, FILM_ACTOR.film().TITLE)
   .from(FILM_ACTOR)
   .groupBy(FILM_ACTOR.FILM_ID, FILM_ACTOR.film().TITLE)
   .having(multisetAgg(FILM_ACTOR.ACTOR_ID).eq(multiset(
        select(FILM_ACTOR.ACTOR_ID)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(97L))
    )))
   .orderBy(FILM_ACTOR.FILM_ID)
   .fetch();
```

생성된 SQL은 암시적 조인을 자동으로 확장하면서 HAVING 절에서 MULTISET 비교를 적용합니다.

## 배열 기반 대안

명시적인 ARRAY_AGG를 선호하는 데이터베이스의 경우, 수동 정렬이 필요합니다. jOOQ에서 `arrayAgg()`와 `array()` 메서드를 사용하여 대안적인 접근 방식을 제공합니다:

```java
ctx.select(FILM_ACTOR.FILM_ID, FILM_ACTOR.film().TITLE)
   .from(FILM_ACTOR)
   .groupBy(FILM_ACTOR.FILM_ID, FILM_ACTOR.film().TITLE)
   .having(arrayAgg(FILM_ACTOR.ACTOR_ID)
        .orderBy(FILM_ACTOR.ACTOR_ID).eq(array(
            select(FILM_ACTOR.ACTOR_ID)
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.FILM_ID.eq(97L))
            .orderBy(FILM_ACTOR.ACTOR_ID)
    )))
    .orderBy(FILM_ACTOR.FILM_ID)
    .fetch();
```

여기서는 MULTISET 비교와 달리 양쪽 모두에 명시적 ORDER BY 절이 필요합니다.

## 핵심 요약

MULTISET 술어는 순서를 자동으로 처리하여 복잡한 데이터 세트 비교를 단순화합니다. 비교 로직에서 명시적 ORDER BY 절의 필요성을 줄이면서도 jOOQ를 통해 타입 안전성을 유지할 수 있습니다.
