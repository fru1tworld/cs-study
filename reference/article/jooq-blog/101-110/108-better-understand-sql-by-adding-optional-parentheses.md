# 선택적 괄호 추가로 SQL을 더 잘 이해하는 5가지 방법

> 원문: https://blog.jooq.org/better-understand-sql-by-adding-optional-parentheses/

게시일: 2020년 3월 3일, Lukas Eder 작성

## 소개

저자는 SQL 구문 이해에 관한 여러 인기 있는 초보자용 SQL 글들을 참조합니다. 여기에는 연산 순서, JOIN 유형, DISTINCT 동작에 관한 가이드들이 포함됩니다. 그는 주요 논지를 소개합니다: 괄호를 전략적으로 추가하면—종종 선택 사항이지만—SQL의 구문 구조를 명확히 하고 개발자가 쿼리 로직을 더 잘 이해하는 데 도움이 됩니다.

이 글은 "SQL 구문은 키워드, 특수 문자, 그리고 많은 다중 의미를 가진 둘의 지저분한 혼합물"이라고 언급하며, 이로 인해 경험 많은 실무자들도 키워드와 식별자를 구분하기 어렵다고 설명합니다.

## 1. 행 값 표현식 (Row Value Expressions)

기본 예제는 이름으로 배우를 검색합니다:

```sql
SELECT *
FROM actor
WHERE first_name = 'SUSAN'
AND last_name = 'DAVIS';
```

이것은 개별 컬럼 주위에 명시적 괄호를 사용하여 다시 작성할 수 있습니다:

```sql
SELECT *
FROM actor
WHERE (first_name) = ('SUSAN')
AND (last_name) = ('DAVIS');
```

또는 PostgreSQL에서 ROW 생성자를 사용하여:

```sql
SELECT *
FROM actor
WHERE ROW (first_name) = ROW ('SUSAN')
AND ROW (last_name) = ROW ('DAVIS');
```

SQL 표준은 여러 컬럼을 행 값 표현식(튜플)으로 결합하는 것을 허용합니다. 이를 통해 더 깔끔한 다중 컬럼 비교가 가능합니다:

```sql
SELECT *
FROM actor
WHERE (first_name, last_name) = ('SUSAN', 'DAVIS');
```

IN과 함께 행 값 표현식 사용:

```sql
SELECT *
FROM actor
WHERE (first_name, last_name) IN (
  ('SUSAN', 'DAVIS'),
  ('NICK' , 'WAHLBERG')
)
```

결과:

| actor_id | first_name | last_name |
|----------|-----------|----------|
| 2 | NICK | WAHLBERG |
| 101 | SUSAN | DAVIS |
| 110 | SUSAN | DAVIS |

서브쿼리와 함께 행 값 표현식 사용:

```sql
-- 고객과 같은 이름을 가진 배우들
SELECT *
FROM actor
WHERE (first_name, last_name) IN (
  SELECT first_name, last_name
  FROM customer
)
```

결과:

| actor_id | first_name | last_name |
|----------|-----------|----------|
| 4 | JENNIFER | DAVIS |

## 2. JOIN

기본 다중 테이블 조인:

```sql
SELECT a.first_name, a.last_name, f.title
FROM actor AS a
JOIN film_actor AS fa USING (actor_id)
JOIN film AS f USING (film_id)
```

결과 (샘플):

| first_name | last_name | title |
|----------|----------|-----|
| PENELOPE | GUINESS | SPLASH GUMP |
| PENELOPE | GUINESS | VERTIGO NORTHWEST |
| PENELOPE | GUINESS | WESTWARD SEABISCUIT |
| PENELOPE | GUINESS | WIZARD COLDBLOODED |
| NICK | WAHLBERG | ADAPTATION HOLES |

저자는 JOIN이 (+ 또는 AND 같은) 연산자이며 결합 규칙을 가진다고 설명합니다. 대부분의 SQL은 좌측 결합을 구현합니다, 이는 다음을 의미합니다:

```sql
SELECT a.first_name, a.last_name, f.title
FROM (
  actor AS a
    JOIN film_actor AS fa
      USING (actor_id)
)
JOIN film AS f
  USING (film_id)
```

그러나 INNER JOIN은 수학적으로 결합적이어서 재정렬이 가능합니다:

```sql
SELECT a.first_name, a.last_name, f.title
FROM actor AS a
  JOIN (
    film_actor
      JOIN film AS f
        USING (film_id)
  )
USING (actor_id)
```

더 복잡한 조인 트리 예제:

```sql
SELECT a.first_name, a.last_name, sum(p.amount)
FROM (
  actor AS a
    JOIN film_actor AS fa
      USING (actor_id)
)
JOIN (
  film AS f
    JOIN (
      inventory AS i
        JOIN rental AS r
          USING (inventory_id)
    ) USING (film_id)
) USING (film_id)
JOIN payment AS p
  USING (rental_id)
GROUP BY a.actor_id
```

단순화된 (동등한) 버전:

```sql
SELECT a.first_name, a.last_name, sum(p.amount)
FROM actor AS a
JOIN film_actor AS fa USING (actor_id)
JOIN film AS f USING (film_id)
JOIN inventory AS i USING (film_id)
JOIN rental AS r USING (inventory_id)
JOIN payment AS p USING (rental_id)
GROUP BY a.actor_id
```

결과 (샘플):

| first_name | last_name | sum |
|----------|----------|------|
| ADAM | GRANT | 974.19 |
| ADAM | HOPPER | 1532.21 |
| AL | GARLAND | 1525.87 |
| ALAN | DREYFUSS | 1850.29 |
| ALBERT | JOHANSSON | 2202.78 |
| ALBERT | NOLTE | 2183.75 |

## 3. DISTINCT

DISTINCT를 사용하는 쿼리:

```sql
SELECT DISTINCT (actor_id), first_name, last_name
FROM actor
```

괄호는 선택 사항이며 순전히 외관상입니다:

```sql
SELECT DISTINCT actor_id, first_name, last_name
FROM actor
```

둘 다 동일한 결과를 생성합니다. 그러나 여러 컬럼을 오해의 소지가 있게 감싸면:

```sql
SELECT DISTINCT (actor_id, first_name), last_name
FROM actor
```

PostgreSQL에서(중첩 레코드를 지원하는) 이것은 다음을 생성합니다:

| row | last_name |
|----------|----------|
| (1,PENELOPE) | GUINESS |
| (2,NICK) | WAHLBERG |
| (3,ED) | CHASE |
| (4,JENNIFER) | DAVIS |

이것은 의도한 동작이 아닙니다.

## 4. UNION, INTERSECT, EXCEPT

집합 연산자들은 서로 다른 우선순위 수준을 가집니다. SQL 표준에 따르면:
- INTERSECT는 더 높은 우선순위를 가집니다
- UNION과 EXCEPT는 같은 우선순위를 공유합니다

우선순위를 보여주는 예제 (PostgreSQL):

```sql
SELECT 2 AS a, 3 AS b
UNION
SELECT 1 AS a, 2 AS b
INTERSECT
SELECT 1 AS a, 2 AS b
```

결과:

| a | b |
|---|---|
| 1 | 2 |
| 2 | 3 |

이것은 실제로 다음과 같이 실행됩니다:

```sql
SELECT 2 AS a, 3 AS b
UNION
(
  SELECT 1 AS a, 2 AS b
  INTERSECT
  SELECT 1 AS a, 2 AS b
)
```

명시적 괄호로 반대로:

```sql
(
  SELECT 2 AS a, 3 AS b
  UNION
  SELECT 1 AS a, 2 AS b
)
INTERSECT
SELECT 1 AS a, 2 AS b
```

결과:

| a | b |
|---|---|
| 1 | 2 |

좌측 결합을 가진 UNION 대 UNION ALL:

```sql
SELECT 2 AS a, 3 AS b
UNION
SELECT 1 AS a, 2 AS b
UNION ALL
SELECT 1 AS a, 2 AS b
```

다음과 동등합니다:

```sql
(
  SELECT 2 AS a, 3 AS b
  UNION
  SELECT 1 AS a, 2 AS b
)
UNION ALL
SELECT 1 AS a, 2 AS b
```

결과:

| a | b |
|---|---|
| 1 | 2 |
| 2 | 3 |
| 1 | 2 |

다른 괄호화를 사용한 대안:

```sql
SELECT 2 AS a, 3 AS b
UNION
(
  SELECT 1 AS a, 2 AS b
  UNION ALL
  SELECT 1 AS a, 2 AS b
)
```

결과:

| a | b |
|---|---|
| 1 | 2 |
| 2 | 3 |

집합 연산에서의 ORDER BY:

```sql
SELECT 2 AS a, 3 AS b
UNION
SELECT 1 AS a, 2 AS b
INTERSECT
SELECT 1 AS a, 2 AS b
ORDER BY a DESC
```

결과:

| a | b |
|---|---|
| 2 | 3 |
| 1 | 2 |

## 5. 서브쿼리

서브쿼리는 필수적인 괄호가 필요합니다. 상관 스칼라 서브쿼리 예제:

```sql
SELECT
  first_name,
  last_name, (
    -- 여기에 상관 서브쿼리
    SELECT count(*)
    FROM film_actor AS fa
    WHERE fa.actor_id = a.actor_id
  ) c
FROM actor AS a
```

괄호가 있는 파생 테이블:

```sql
SELECT
  first_name,
  last_name,
  c
FROM actor AS a
JOIN (
  -- 여기에 파생 테이블
  SELECT actor_id, count(*) AS c
  FROM film_actor
  GROUP BY actor_id
) fa USING (actor_id)
```

## 결론

저자는 괄호가 SQL 이해를 향상시키는 다섯 가지 핵심 영역을 요약합니다:

1. 차수 1의 행 값 표현식은 실무에서 거의 사용되지 않는 선택적 괄호를 가집니다
2. JOIN은 결합성을 제어하기 위해 조정 가능한 중첩을 가진 트리로 기능합니다
3. DISTINCT는 일반적인 오해에도 불구하고 그 인자 주위에 괄호가 없습니다
4. 집합 연산은 두 가지 우선순위 수준을 따릅니다: INTERSECT (더 높음)와 UNION/EXCEPT (더 낮음)
5. 서브쿼리는 집합 연산 컨텍스트 외부에서 괄호가 필수입니다

이 글은 독자들에게 댓글에서 자신만의 SQL 구문 혼란을 공유하도록 초대하며 마무리됩니다.
