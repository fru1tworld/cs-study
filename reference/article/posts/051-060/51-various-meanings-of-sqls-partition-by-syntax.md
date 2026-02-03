# SQL PARTITION BY 구문의 다양한 의미

> 원문: https://blog.jooq.org/various-meanings-of-sqls-partition-by-syntax/

SQL 초보자에게는 `PARTITION BY`라는 다소 난해한 구문이 있는데, SQL 곳곳에서 등장합니다. 이 구문은 항상 비슷한 의미를 가지지만, 꽤 다른 맥락에서 사용됩니다. 그 의미는 `GROUP BY`와 유사한데, 즉 어떤 그룹화/파티셔닝 기준에 따라 데이터 집합을 그룹화/파티셔닝하는 것입니다.

## 예제 데이터셋

예를 들어, Sakila 데이터베이스를 쿼리할 때:

```sql
SELECT actor_id, film_id
FROM film_actor
```

아래와 같은 결과가 나타날 수 있습니다:

| actor_id | film_id |
|----------|---------|
| 1        | 1       |
| 2        | 3       |
| 10       | 1       |
| 20       | 1       |
| 1        | 23      |
| 1        | 25      |
| 30       | 1       |
| 19       | 2       |
| 40       | 1       |
| 3        | 17      |
| 53       | 1       |
| 19       | 3       |
| 2        | 31      |

이 데이터는 actor_id를 기준으로 파티셔닝할 수 있으며, 행들을 겹치지 않는 부분집합으로 분리합니다.

파티션은 데이터 집합을 겹치지 않는 부분집합으로 분리합니다.

## 윈도우 파티션

첫 번째로 할 수 있는 것은 윈도우 함수를 계산할 때 사용하는 윈도우 `PARTITION` 절입니다. 예를 들어, 다음과 같이 계산할 수 있습니다:

```sql
SELECT
  actor_id,
  film_id,
  COUNT(*) OVER (PARTITION BY actor_id)
FROM film_actor
```

전체 데이터셋을 본다고 가정하면(실제 테이블에는 더 많은 행이 있습니다), 다음과 같은 결과가 표시됩니다:

| actor_id | film_id | count |
|----------|---------|-------|
| 1        | 1       | 3     |
| 2        | 3       | 2     |
| 10       | 1       | 1     |
| 20       | 1       | 1     |
| 1        | 23      | 3     |
| 1        | 25      | 3     |
| 30       | 1       | 1     |
| 19       | 2       | 2     |
| 40       | 1       | 1     |
| 3        | 17      | 1     |
| 53       | 1       | 1     |
| 19       | 3       | 2     |
| 2        | 31      | 2     |

다시 말해, 우리는 "파티션에 걸쳐 행을 세는" 것입니다. 이것은 그룹의 행을 세는 `GROUP BY`와 거의 비슷하게 작동하지만, `GROUP BY` 절은 결과 집합과 프로젝션 가능한 컬럼을 변환하여 그룹화되지 않은 컬럼을 사용할 수 없게 만듭니다:

```sql
SELECT actor_id, COUNT(*)
FROM film_actor
GROUP BY actor_id
```

결과:

| actor_id | count |
|----------|-------|
| 1        | 3     |
| 2        | 2     |
| 10       | 1     |
| 20       | 1     |
| 30       | 1     |
| 19       | 2     |
| 40       | 1     |
| 3        | 1     |
| 53       | 1     |

말하자면, 파티션의 내용이 축소되어 각 파티션 키/그룹 키가 결과 집합에 한 번만 나타나게 됩니다. 이 차이가 윈도우 함수를 일반적인 집계 함수와 그룹화보다 훨씬 더 강력하게 만듭니다.

## MATCH_RECOGNIZE 파티션

`MATCH_RECOGNIZE`는 SQL 표준의 일부로, Oracle에 의해 발명되었으며, 다른 모든 RDBMS가 부러워하는 기능입니다(일부는 채택하기 시작했지만). 이 기능은 정규 표현식, 패턴 매칭, 데이터 생성, 그리고 SQL의 강력함을 결합합니다. 지각이 있을지도 모릅니다, 누가 알겠습니까.

예를 들어, 짧은 시간 내에 소액 결제를 하는 고객을 살펴보겠습니다.

```sql
SELECT
  customer_id,
  payment_date,
  payment_id,
  amount
FROM payment
MATCH_RECOGNIZE (

  -- 데이터 집합을 customer_id로 파티셔닝
  PARTITION BY customer_id

  -- 각 파티션을 payment_date로 정렬
  ORDER BY payment_date

  -- 매칭된 모든 행을 반환
  ALL ROWS PER MATCH

  -- 이벤트 "A"가 연속 3회 발생하는 행을 매칭
  PATTERN (A {3})

  -- 이벤트 "A"를 다음과 같이 정의...
  DEFINE A AS

      -- 금액이 1 미만인 결제
      A.amount < 1

      -- 그리고 결제일이 이전 결제로부터 1일 미만인 경우
      AND A.payment_date - prev(A.payment_date) < 1
)
ORDER BY customer_id, payment_date
```

이런! 이 쿼리는 너무 많은 멋진 키워드를 사용해서 이 저렴한 블로그의 구문 하이라이터로는 도저히 따라갈 수 없습니다!

결과는 다음과 같습니다:

| CUSTOMER_ID | PAYMENT_DATE            | PAYMENT_ID | AMOUNT |
|-------------|-------------------------|------------|--------|
| 72          | 2005-08-18 10:59:04.000 | 1961       | 0.99   |
| 72          | 2005-08-18 16:17:54.000 | 1962       | 0.99   |
| 72          | 2005-08-19 12:53:53.000 | 1963       | 0.99   |
| 152         | 2005-08-20 01:16:52.000 | 4152       | 0.99   |
| 152         | 2005-08-20 19:13:23.000 | 4153       | 0.99   |
| 152         | 2005-08-21 03:01:01.000 | 4154       | 0.99   |
| 207         | 2005-07-08 17:14:14.000 | 5607       | 0.99   |
| 207         | 2005-07-09 01:26:22.000 | 5608       | 0.99   |
| 207         | 2005-07-09 13:56:56.000 | 5609       | 0.99   |
| 244         | 2005-08-20 11:54:01.000 | 6615       | 0.99   |
| 244         | 2005-08-20 17:12:28.000 | 6616       | 0.99   |
| 244         | 2005-08-21 09:31:44.000 | 6617       | 0.99   |

3개의 결제 그룹 각각에 대해 다음을 확인할 수 있습니다:
- 금액이 1 미만입니다.
- 연속된 날짜가 1일 미만의 간격입니다.
- 그룹은 _고객별_로 구성되며, 이것이 바로 파티션입니다.

`MATCH_RECOGNIZE`에 대해 더 알고 싶으신가요? [이 글](https://modern-sql.com/caniuse/match-recognize)이 웹에서 찾을 수 있는 어떤 것보다 훨씬 잘 설명해 준다고 생각합니다. Gerald Venzl이 제공하는 Docker 이미지 등을 통해 Oracle XE 21c에서 무료로 실험해 볼 수 있습니다.

## MODEL 파티션

`MATCH_RECOGNIZE`보다 더 난해한 것이 Oracle 전용 `MODEL` 또는 `SPREADSHEET` 절입니다. 모든 복잡한 애플리케이션에는 동료들을 어리둥절하게 만들기 위해 최소 하나의 `MODEL` 쿼리가 있어야 합니다.

간단히 말해, MS Excel과 같은 스프레드시트 소프트웨어에서 할 수 있는 모든 것을 할 수 있습니다. 작동 방식에 대한 깊은 설명 없이 여기에 또 다른 예를 들겠습니다:

```sql
SELECT
  customer_id,
  payment_date,
  payment_id,
  amount
FROM (
  SELECT *
  FROM (
    SELECT p.*, 0 AS s, 0 AS n
    FROM payment p
  )
  MODEL

    -- 다시 데이터 집합을 customer_id로 파티셔닝
    PARTITION BY (customer_id)

    -- "스프레드시트 차원"은 파티션 내에서 payment_date로
    -- 정렬된 행 번호
    DIMENSION BY (
      row_number () OVER (
        PARTITION BY customer_id
        ORDER BY payment_date
      ) AS rn
    )

    -- Measures는 프로젝션하려는 것으로, 다음을 포함:
    -- o 테이블 컬럼
    -- o 추가 계산된 값
    MEASURES (payment_date, payment_id, amount, s, n)

    -- 이 규칙들은 스프레드시트 수식
    RULES (

      -- S는 1 미만의 금액이면서 결제일 간격이 1일 미만인
      -- 이전 금액들의 합계
      s[any] = CASE
          WHEN amount[cv(rn)] < 1
          AND payment_date[cv(rn)] - payment_date[cv(rn) - 1] < 1
          THEN coalesce(s[cv(rn) - 1], 0) + amount[cv(rn)]
          ELSE 0
      END,

      -- N은 이러한 속성을 가진 연속 금액의 수
      n[any] = CASE
          WHEN amount[cv(rn)] < 1
          AND payment_date[cv(rn)] - payment_date[cv(rn) - 1] < 1
          THEN coalesce(n[cv(rn) - 1], 0) + 1
          ELSE 0
      END
    )
) t

-- 연속 이벤트가 3개 이상인 행만 필터링
WHERE n >= 3
ORDER BY customer_id, rn
```

금요일 배포 전에 이런 쿼리를 프로덕션 코드베이스에 하나 넣어 보세요, 모든 사람의 사랑을 받게 될 것이 보장됩니다. 어쨌든, `MATCH_RECOGNIZE`가 좀 더 깔끔했다고 생각합니다.

결과는 다음과 같습니다:

| CUSTOMER_ID | PAYMENT_DATE            | PAYMENT_ID | AMOUNT |
|-------------|-------------------------|------------|--------|
| 72          | 2005-08-19 12:53:53.000 | 1963       | 0.99   |
| 152         | 2005-08-21 03:01:01.000 | 4154       | 0.99   |
| 207         | 2005-07-09 13:56:56.000 | 5609       | 0.99   |
| 244         | 2005-08-21 09:31:44.000 | 6617       | 0.99   |
| 244         | 2005-08-21 19:39:43.000 | 6618       | 0.99   |
| 252         | 2005-07-28 02:44:25.000 | 6800       | 0.99   |
| 377         | 2005-07-07 12:24:37.000 | 10211      | 0.99   |
| 425         | 2005-08-01 12:37:46.000 | 11499      | 0.99   |
| 511         | 2005-07-11 18:50:55.000 | 13769      | 0.99   |

모험을 즐기신다면, `MATCH_RECOGNIZE` 예제처럼 그룹을 구성하는 일반적인 3행을 반환하도록 제 쿼리를 수정해 보시고, 솔루션을 댓글에 남겨주세요. 분명히 가능합니다!

## 파티션 테이블

최소한 Oracle과 PostgreSQL은 저장소 수준에서 테이블 파티셔닝을 지원하며, 아마 다른 데이터베이스도 지원할 것입니다. 이 기능은 데이터를 별도의 _물리적_ 테이블로 분리하면서도, 애플리케이션에서는 하나의 _논리적_ 테이블이 있는 것처럼 투명하게 가장하여 저장소 문제를 다루는 데 도움을 줍니다(다른 종류의 문제를 도입하기도 하지만). 전형적인 예는 날짜 범위별로 데이터셋을 파티셔닝하는 것입니다. 예를 들어 PostgreSQL 문서에 나오는 것처럼:

```sql
CREATE TABLE payment (
  customer_id int not null,
  amount numeric not null,
  payment_date date not null
)
PARTITION BY RANGE (payment_date);
```

이제 이 테이블은 아직 사용할 수 없습니다. _논리적_으로만 존재하기 때문입니다. 데이터를 _물리적_으로 어떻게 저장할지 아직 모릅니다:

```sql
INSERT INTO payment (customer_id, amount, payment_date)
VALUES (1, 10, DATE '2000-01-01');
```

이것은 다음과 같은 오류를 생성합니다:

> SQL Error [23514]: ERROR: no partition of relation "payment" found for row
> Detail: Partition key of the failing row contains (payment_date) = (2000-01-01).

그래서 특정 날짜 범위에 대한 물리적 저장소를 만들어 보겠습니다. 예를 들어:

```sql
CREATE TABLE payment_2000
PARTITION OF payment
FOR VALUES FROM (DATE '2000-01-01') TO (DATE '2000-12-31');
```

이제 삽입이 작동합니다. 이 `PARTITION`의 해석은 다시 윈도우 함수의 것과 일치하는데, 데이터셋을 겹치지 않고 명확하게 분리된 부분집합으로 파티셔닝합니다.

## 특이한 것: 외부 조인 파티션

다음 파티셔닝 기능은 SQL 표준의 일부이지만, 지금까지 Oracle에서만 구현된 것을 봤습니다. Oracle은 이 기능을 오래전부터 가지고 있었습니다: 파티션 외부 조인(Partitioned Outer Join)입니다. 이것은 설명하기가 쉽지 않으며, 안타깝게도 이 파티션은 윈도우 파티션과는 아무 관련이 없습니다. 이것은 `CROSS JOIN`의 문법적 편의(또는 취향에 따라 불편)에 더 가깝습니다.

이렇게 생각해 보세요. 파티션 외부 조인을 사용하면 희소한 데이터에서 빈 부분을 채울 수 있습니다. 예제를 살펴보겠습니다:

```sql
SELECT
  f.film_id,
  f.title,
  c.category_id,
  c.name,
  count(*) OVER ()
FROM film f
  LEFT OUTER JOIN film_category fc
    ON f.film_id = fc.film_id
  LEFT OUTER JOIN category c
    ON fc.category_id = c.category_id
ORDER BY f.film_id, c.category_id
```

이 쿼리는 영화별 카테고리를 생성합니다. 카테고리가 영화에 나타나지 않으면 결과에 레코드가 없습니다:

| FILM_ID | TITLE            | CATEGORY_ID | NAME        | COUNT(*)OVER() |
|---------|------------------|-------------|-------------|----------------|
| 1       | ACADEMY DINOSAUR | 6           | Documentary | 1000           |
| 2       | ACE GOLDFINGER   | 11          | Horror      | 1000           |
| 3       | ADAPTATION HOLES | 6           | Documentary | 1000           |
| 4       | AFFAIR PREJUDICE | 11          | Horror      | 1000           |
| 5       | AFRICAN EGG      | 8           | Family      | 1000           |
| 6       | AGENT TRUMAN     | 9           | Foreign     | 1000           |
| 7       | AIRPLANE SIERRA  | 5           | Comedy      | 1000           |
| 8       | AIRPORT POLLOCK  | 11          | Horror      | 1000           |
| 9       | ALABAMA DEVIL    | 11          | Horror      | 1000           |
| 10      | ALADDIN CALENDAR | 15          | Sports      | 1000           |

보시다시피 1000개의 영화가 있고, Sakila 데이터베이스가 너무 단조로워서 다대다 관계가 둘 이상의 할당을 허용함에도 모든 영화에 카테고리가 1개뿐입니다.

외부 조인 중 하나에 `PARTITION BY` 절을 추가하면 어떻게 될까요?

```sql
SELECT
  f.film_id,
  f.title,
  c.category_id,
  c.name,
  count(*) OVER ()
FROM film f
  LEFT OUTER JOIN film_category fc
    ON f.film_id = fc.film_id
  LEFT OUTER JOIN category c
  PARTITION BY (c.category_id) -- 여기가 마법
    ON fc.category_id = c.category_id
ORDER BY f.film_id, c.category_id
```

전체 결과를 보여주지는 않겠지만, 윈도우 함수 결과에서 볼 수 있듯이 이제 1000행이 아닌 16000행이 됩니다. 1000개의 영화 x 16개의 카테고리이기 때문입니다. 말하자면 매칭이 없는 경우 빈 카테고리 이름(카테고리 ID는 비어있지 않음)을 가진 교차 곱입니다:

| FILM_ID | TITLE            | CATEGORY_ID | NAME        | COUNT(*)OVER() |
|---------|------------------|-------------|-------------|----------------|
| 1       | ACADEMY DINOSAUR | 1           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 2           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 3           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 4           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 5           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 6           | Documentary | 16000          |
| 1       | ACADEMY DINOSAUR | 7           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 8           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 9           |             | 16000          |
| 1       | ACADEMY DINOSAUR | 10          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 11          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 12          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 13          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 14          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 15          |             | 16000          |
| 1       | ACADEMY DINOSAUR | 16          |             | 16000          |
| 2       | ACE GOLDFINGER   | 1           |             | 16000          |
| 2       | ACE GOLDFINGER   | 2           |             | 16000          |
| 2       | ACE GOLDFINGER   | 3           |             | 16000          |
| 2       | ACE GOLDFINGER   | 4           |             | 16000          |
| 2       | ACE GOLDFINGER   | 5           |             | 16000          |
| 2       | ACE GOLDFINGER   | 6           |             | 16000          |
| 2       | ACE GOLDFINGER   | 7           |             | 16000          |
| 2       | ACE GOLDFINGER   | 8           |             | 16000          |
| 2       | ACE GOLDFINGER   | 9           |             | 16000          |
| 2       | ACE GOLDFINGER   | 10          |             | 16000          |
| 2       | ACE GOLDFINGER   | 11          | Horror      | 16000          |
| 2       | ACE GOLDFINGER   | 12          |             | 16000          |
| 2       | ACE GOLDFINGER   | 13          |             | 16000          |
| 2       | ACE GOLDFINGER   | 14          |             | 16000          |
| 2       | ACE GOLDFINGER   | 15          |             | 16000          |
| 2       | ACE GOLDFINGER   | 16          |             | 16000          |

어떤 면에서 이것은 희소한 데이터를 기반으로 보고서를 만들고, 그 빈 부분에 대한 레코드를 생성하고 싶을 때 유용합니다. `PARTITION BY` 없이 유사한 쿼리는 `CROSS JOIN`을 사용하는 것입니다:

```sql
SELECT
  f.film_id,
  f.title,
  c.category_id,
  NVL2(fc.category_id, c.name, NULL) AS name,
  count(*) OVER ()
FROM film f
  CROSS JOIN category c
  LEFT JOIN film_category fc
    ON fc.film_id = f.film_id
    AND fc.category_id = c.category_id
ORDER BY f.film_id, c.category_id;
```

솔직히 말해서, 과거에 이 파티션 외부 조인이 매우 유용하거나 이해하기 쉬운 것을 발견하지 못했고, 표준 SQL임에도 불구하고 다른 RDBMS가 정말로 중요한 기능을 놓치고 있다고 확신하지 않습니다. 지금까지 jOOQ는 다른 RDBMS에서 이 기능을 에뮬레이션하지 않습니다.
