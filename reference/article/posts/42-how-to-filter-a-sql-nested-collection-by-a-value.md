# SQL 중첩 컬렉션을 값으로 필터링하는 방법

> 원문: https://blog.jooq.org/how-to-filter-a-sql-nested-collection-by-a-value/

Stack Overflow에서 jOOQ의 `MULTISET` 연산자를 사용하여 컬렉션을 중첩한 다음, 해당 중첩 컬렉션에 특정 값이 포함되어 있는지 여부로 결과를 필터링하는 방법에 대한 매우 흥미로운 질문을 발견했습니다.

이 질문은 jOOQ에 한정된 것이지만, PostgreSQL에서 JSON을 사용하여 컬렉션을 중첩하는 쿼리가 있다고 가정해 봅시다. 항상 그렇듯이 Sakila 데이터베이스를 사용합니다. PostgreSQL은 SQL 표준 `MULTISET` 연산자를 지원하지 않지만, 거의 동일하게 작동하는 `ARRAY`를 사용할 수 있습니다.

```sql
SELECT
  f.title,
  ARRAY(
    SELECT ROW(
      a.actor_id,
      a.first_name,
      a.last_name
    )
    FROM actor AS a
    JOIN film_actor AS fa USING (actor_id)
    WHERE fa.film_id = f.film_id
    ORDER BY a.actor_id
  )
FROM film AS f
ORDER BY f.title
```

이 쿼리는 모든 영화와 해당 배우를 다음과 같이 생성합니다(가독성을 위해 배열을 잘랐습니다. 요지는 이해하실 겁니다):

| title | array |
|-------|-------|
| ACADEMY DINOSAUR | {"(1,PENELOPE,GUINESS)","(10,CHRISTIAN,GABLE)","(20,LUCILLE,TRACY)","(30,SANDRA,PECK)" |
| ACE GOLDFINGER | {"(19,BOB,FAWCETT)","(85,MINNIE,ZELLWEGER)","(90,SEAN,GUINESS)","(160,CHRIS,DEPP)"} |
| ADAPTATION HOLES | {"(2,NICK,WAHLBERG)","(19,BOB,FAWCETT)","(24,CAMERON,STREEP)","(64,RAY,JOHANSSON)","(1 |
| AFFAIR PREJUDICE | {"(41,JODIE,DEGENERES)","(81,SCARLETT,DAMON)","(88,KENNETH,PESCI)","(147,FAY,WINSLET)" |
| AFRICAN EGG | {"(51,GARY,PHOENIX)","(59,DUSTIN,TAUTOU)","(103,MATTHEW,LEIGH)","(181,MATTHEW,CARREY)" |
| AGENT TRUMAN | {"(21,KIRSTEN,PALTROW)","(23,SANDRA,KILMER)","(62,JAYNE,NEESON)","(108,WARREN,NOLTE)", |
| AIRPLANE SIERRA | {"(99,JIM,MOSTEL)","(133,RICHARD,PENN)","(162,OPRAH,KILMER)","(170,MENA,HOPPER)","(185 |
| AIRPORT POLLOCK | {"(55,FAY,KILMER)","(96,GENE,WILLIS)","(110,SUSAN,DAVIS)","(138,LUCILLE,DEE)"} |
| ALABAMA DEVIL | {"(10,CHRISTIAN,GABLE)","(22,ELVIS,MARX)","(26,RIP,CRAWFORD)","(53,MENA,TEMPLE)","(68, |

이제 Stack Overflow에서의 질문은, 이 결과를 `ARRAY`(또는 `MULTISET`)에 특정 값이 포함되어 있는지 여부로 어떻게 필터링하느냐는 것이었습니다.

## ARRAY 필터링

쿼리에 `WHERE` 절을 그냥 추가할 수는 없습니다. SQL의 논리적 연산 순서 때문에 `WHERE` 절은 `SELECT` 절보다 "먼저 실행"되므로, `ARRAY`는 아직 `WHERE`에서 사용할 수 없습니다. 하지만 모든 것을 파생 테이블로 감싸면 다음과 같이 할 수 있습니다:

```sql
SELECT *
FROM (
  SELECT
    f.title,
    ARRAY(
      SELECT ROW(
        a.actor_id,
        a.first_name,
        a.last_name
      )
      FROM actor AS a
      JOIN film_actor AS fa USING (actor_id)
      WHERE fa.film_id = f.film_id
      ORDER BY a.actor_id
    ) AS actors
  FROM film AS f
) AS f
WHERE actors @> ARRAY[(
  SELECT ROW(a.actor_id, a.first_name, a.last_name)
  FROM actor AS a
  WHERE a.actor_id = 1
)]
ORDER BY f.title
```

다소 번거로운 `ARRAY @> ARRAY` 연산자를 양해해 주세요. 명목형 타입(`CREATE TYPE ...`)을 사용하지 않는 한 PostgreSQL에서 구조적으로 타입이 지정된 `RECORD[]` 배열을 unnest하기 어렵기 때문에, 더 나은 방법을 알지 못합니다. 더 좋은 필터링 방법을 알고 계시다면 댓글로 알려주세요. 다음은 더 나은 버전입니다:

```sql
SELECT *
FROM (
  SELECT
    f.title,
    ARRAY(
      SELECT ROW(
        a.actor_id,
        a.first_name,
        a.last_name
      )
      FROM actor AS a
      JOIN film_actor AS fa USING (actor_id)
      WHERE fa.film_id = f.film_id
      ORDER BY a.actor_id
    ) AS actors
  FROM film AS f
) AS f
WHERE EXISTS (
  SELECT 1
  FROM unnest(actors) AS t (a bigint, b text, c text)
  WHERE a = 1
)
ORDER BY f.title
```

어쨌든, 이것은 원하는 결과를 생성합니다:

| title | actors |
|-------|--------|
| ACADEMY DINOSAUR | {"(1,PENELOPE,GUINESS)","(10,CHRISTIAN,GABLE)","(20,LUCILLE,TRACY)","(30,SANDRA,PECK)","(40,JOHNN |
| ANACONDA CONFESSIONS | {"(1,PENELOPE,GUINESS)","(4,JENNIFER,DAVIS)","(22,ELVIS,MARX)","(150,JAYNE,NOLTE)","(164,HUMPHREY |
| ANGELS LIFE | {"(1,PENELOPE,GUINESS)","(4,JENNIFER,DAVIS)","(7,GRACE,MOSTEL)","(47,JULIA,BARRYMORE)","(91,CHRIS |
| BULWORTH COMMANDMENTS | {"(1,PENELOPE,GUINESS)","(65,ANGELA,HUDSON)","(124,SCARLETT,BENING)","(173,ALAN,DREYFUSS)"} |
| CHEAPER CLYDE | {"(1,PENELOPE,GUINESS)","(20,LUCILLE,TRACY)"} |
| COLOR PHILADELPHIA | {"(1,PENELOPE,GUINESS)","(106,GROUCHO,DUNST)","(122,SALMA,NOLTE)","(129,DARYL,CRAWFORD)","(163,CH |
| ELEPHANT TROJAN | {"(1,PENELOPE,GUINESS)","(24,CAMERON,STREEP)","(37,VAL,BOLGER)","(107,GINA,DEGENERES)","(115,HARR |
| GLEAMING JAWBREAKER | {"(1,PENELOPE,GUINESS)","(66,MARY,TANDY)","(125,ALBERT,NOLTE)","(143,RIVER,DEAN)","(155,IAN,TANDY |

이제 모든 결과는 `'PENELOPE GUINESS'`가 `ACTOR`로 출연한 영화임이 보장됩니다. 하지만 더 나은 해결책이 있을까요?

## 대신 ARRAY_AGG 사용하기

네이티브 PostgreSQL에서는 (이 경우) `ARRAY_AGG`를 사용하는 것이 더 낫다고 생각합니다:

```sql
SELECT
  f.title,
  ARRAY_AGG(ROW(
    a.actor_id,
    a.first_name,
    a.last_name
  ) ORDER BY a.actor_id) AS actors
FROM film AS f
JOIN film_actor AS fa USING (film_id)
JOIN actor AS a USING (actor_id)
GROUP BY f.title
HAVING bool_or(true) FILTER (WHERE a.actor_id = 1)
ORDER BY f.title
```

이것은 정확히 동일한 결과를 생성합니다:

| title | actors |
|-------|--------|
| ACADEMY DINOSAUR | {"(1,PENELOPE,GUINESS)","(10,CHRISTIAN,GABLE)","(20,LUCILLE,TRACY)","(30,SANDRA,PECK)","(40,JOHN |
| ANACONDA CONFESSIONS | {"(1,PENELOPE,GUINESS)","(4,JENNIFER,DAVIS)","(22,ELVIS,MARX)","(150,JAYNE,NOLTE)","(164,HUMPHRE |
| ANGELS LIFE | {"(1,PENELOPE,GUINESS)","(4,JENNIFER,DAVIS)","(7,GRACE,MOSTEL)","(47,JULIA,BARRYMORE)","(91,CHRI |
| BULWORTH COMMANDMENTS | {"(1,PENELOPE,GUINESS)","(65,ANGELA,HUDSON)","(124,SCARLETT,BENING)","(173,ALAN,DREYFUSS)"} |
| CHEAPER CLYDE | {"(1,PENELOPE,GUINESS)","(20,LUCILLE,TRACY)"} |
| COLOR PHILADELPHIA | {"(1,PENELOPE,GUINESS)","(106,GROUCHO,DUNST)","(122,SALMA,NOLTE)","(129,DARYL,CRAWFORD)","(163,C |
| ELEPHANT TROJAN | {"(1,PENELOPE,GUINESS)","(24,CAMERON,STREEP)","(37,VAL,BOLGER)","(107,GINA,DEGENERES)","(115,HAR |
| GLEAMING JAWBREAKER | {"(1,PENELOPE,GUINESS)","(66,MARY,TANDY)","(125,ALBERT,NOLTE)","(143,RIVER,DEAN)","(155,IAN,TAND |

어떻게 작동하는 걸까요?

* `FILM`별로 그룹화하고 _영화별_ 내용을 중첩 컬렉션으로 집계합니다.
* 이제 `HAVING`을 사용하여 그룹을 필터링할 수 있습니다.
* `BOOL_OR(TRUE)`는 `GROUP`이 비어 있지 않으면 `TRUE`가 됩니다.
* `FILTER (WHERE a.actor_id = 1)`은 그룹에 배치하는 필터 조건입니다.

따라서 `HAVING` 조건은 `ACTOR_ID = 1`인 항목이 하나라도 있으면 `TRUE`이고, 그렇지 않으면 `NULL`이 되며, 이는 `FALSE`와 동일한 효과를 가집니다. 엄밀하게 하고 싶다면 조건을 `COALESCE(BOOL_OR(...), FALSE)`로 감싸면 됩니다.

영리한 건가요, 깔끔한 건가요, 아니면 둘 다일까요?

## jOOQ로 이것을 구현하기

다음은 `MULTISET_AGG`를 지원하는 모든 RDBMS에서 작동하는 jOOQ 버전입니다(`ARRAY_AGG` 에뮬레이션은 아직 개발 중입니다):

```java
ctx.select(
        FILM_ACTOR.film().TITLE,
        multisetAgg(
            FILM_ACTOR.actor().ACTOR_ID,
            FILM_ACTOR.actor().FIRST_NAME,
            FILM_ACTOR.actor().LAST_NAME))
   .from(FILM_ACTOR)
   .groupBy(FILM_ACTOR.film().TITLE)
   .having(boolOr(trueCondition())
       .filterWhere(FILM_ACTOR.actor().ACTOR_ID.eq(1)))
   .orderBy(FILM_ACTOR.film().TITLE)
   .fetch();
```

강력한 `MULTISET` 값 생성자가 jOOQ 사용자들 사이에서 대부분의 주목을 받고 있지만, 약간 덜 강력하지만 때때로 정말 유용한 `MULTISET_AGG` 집계 함수도 있다는 것을 잊지 마세요. 이 함수는 집계 또는 윈도우 함수로 사용할 수 있습니다!
