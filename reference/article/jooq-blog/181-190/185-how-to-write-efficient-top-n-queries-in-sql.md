# SQL에서 효율적인 TOP N 쿼리를 작성하는 방법

> 원문: https://blog.jooq.org/how-to-write-efficient-top-n-queries-in-sql/

매우 일반적인 SQL 쿼리 유형 중 하나는 TOP-N 쿼리입니다. 이 쿼리에서는 특정 값으로 정렬된 "TOP N" 레코드가 필요하며, 때로는 카테고리별로 필요할 수도 있습니다.

이 블로그 게시물에서는 이 문제의 다양한 측면과 표준 및 비표준 SQL로 이를 해결하는 방법을 살펴보겠습니다.

## Top 값 가져오기

Sakila 데이터베이스를 살펴볼 때, 가장 많은 영화에 출연한 배우를 찾고 싶을 수 있습니다. 가장 간단한 해결책은 배우당 영화 수를 찾기 위해 GROUP BY를 사용한 다음, "TOP 1" 배우를 찾기 위해 ORDER BY와 LIMIT를 사용하는 것입니다.

PostgreSQL에서:

```sql
SELECT actor_id, first_name, last_name, count(film_id)
FROM actor
LEFT JOIN film_actor USING (actor_id)
GROUP BY actor_id, first_name, last_name
ORDER BY count(film_id) DESC
LIMIT 1;
```

Oracle 12c에서:

```sql
SELECT actor_id, first_name, last_name, count(film_id)
FROM actor
LEFT JOIN film_actor USING (actor_id)
GROUP BY actor_id, first_name, last_name
ORDER BY count(*) DESC
FETCH FIRST ROW ONLY;
```

SQL Server에서:

```sql
SELECT TOP 1 a.actor_id, first_name, last_name, count(film_id)
FROM actor a
LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id
GROUP BY a.actor_id, first_name, last_name
ORDER BY count(*) DESC;
```

### 파생 테이블 대안

때때로 쿼리를 파생 테이블(FROM 절의 서브쿼리)로 작성하는 것이 더 읽기 쉽거나 성능이 좋을 수 있습니다:

```sql
SELECT actor_id, first_name, last_name, COALESCE(c, 0)
FROM actor
LEFT JOIN (
  SELECT actor_id, count(*) AS c
  FROM film_actor
  GROUP BY actor_id
) fa USING (actor_id)
ORDER BY COALESCE(c, 0) DESC
LIMIT 1;
```

### 상관 서브쿼리 대안

또 다른 대안은 SELECT 절에서 상관 서브쿼리를 사용하는 것입니다:

```sql
SELECT actor_id, first_name, last_name, (
  SELECT count(*)
  FROM film_actor fa
  WHERE fa.actor_id = a.actor_id
) AS c
FROM actor a
ORDER BY c DESC
LIMIT 1;
```

### 윈도우 함수를 사용한 대안

ROW_NUMBER() 윈도우 함수를 사용하여 이 쿼리를 작성할 수도 있습니다:

```sql
SELECT *
FROM (
  SELECT
    actor_id, first_name, last_name, count(film_id),
    row_number() OVER (ORDER BY count(film_id) DESC) rn
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  GROUP BY actor_id, first_name, last_name
) t
WHERE rn <= 5
ORDER BY rn;
```

### Oracle KEEP 구문

Oracle은 집계 함수에 사용할 수 있는 특별한 KEEP 구문을 가지고 있습니다:

```sql
SELECT
  max(actor_id)   KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(first_name) KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(last_name)  KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(c)          KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id)
FROM (
  SELECT actor_id, first_name, last_name, count(film_id) c
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  GROUP BY actor_id, first_name, last_name
) t;
```

## WITH TIES 시맨틱스를 사용한 Top 값

여러 행이 동일한 최상위 값을 공유하는 경우 어떻게 해야 할까요? SQL 표준은 모든 동률 결과를 반환하기 위한 `FETCH FIRST ROWS WITH TIES` 구문을 제공합니다.

Oracle 12c (표준 SQL):

```sql
SELECT actor_id, first_name, last_name, count(film_id)
FROM actor
LEFT JOIN film_actor USING (actor_id)
GROUP BY actor_id, first_name, last_name
ORDER BY count(film_id) DESC
FETCH FIRST ROWS WITH TIES;
```

SQL Server:

```sql
SELECT TOP 1 WITH TIES
  a.actor_id, first_name, last_name, count(*)
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
GROUP BY a.actor_id, first_name, last_name
ORDER BY count(*) DESC;
```

### RANK() 윈도우 함수를 사용한 WITH TIES

WITH TIES를 기본적으로 지원하지 않는 다른 데이터베이스의 경우, RANK() 윈도우 함수를 사용하여 동등한 기능을 제공할 수 있습니다:

```sql
SELECT *
FROM (
  SELECT
    actor_id, first_name, last_name, count(film_id),
    rank() OVER (ORDER BY count(film_id) DESC) rk
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  GROUP BY actor_id, first_name, last_name
) t
WHERE rk = 1;
```

TOP 5와 함께 동률을 포함하는 변형:

```sql
SELECT *
FROM (
  SELECT
    actor_id, first_name, last_name, count(film_id),
    rank() OVER (ORDER BY count(film_id) DESC) rk
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  GROUP BY actor_id, first_name, last_name
) t
WHERE rk <= 5
ORDER BY rk;
```

## 카테고리별 Top N 값

배우당 TOP N 무언가를 얻고 싶다고 가정해 봅시다. 예를 들어, 배우가 출연한 가장 성공적인 TOP 3 영화를 찾고 싶습니다. 단순하게 유지하기 위해, 배우당 가장 긴 제목을 가진 TOP 3 영화를 찾는 것을 사용하겠습니다. 이것은 다시 LIMIT를 사용하지만, 이번에는 서브쿼리에서 사용합니다.

파생 테이블(FROM 절의 서브쿼리)은 적어도 LIMIT로는 배우별 시맨틱스를 쉽게 구현할 수 없습니다. 윈도우 함수로는 가능합니다. 상관 서브쿼리(SELECT 또는 WHERE 절의 서브쿼리)는 하나의 행과 하나의 열만 반환할 수 있어서, TOP 3 행을 반환하고 싶을 때는 유용하지 않습니다.

다행히도, SQL 표준은 LATERAL(Oracle 12c, PostgreSQL, DB2에서 구현됨)을 지정하고, SQL Server는 항상 APPLY를 가지고 있었습니다.

### APPLY를 사용한 카테고리별 Top N

SQL Server/Oracle:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
OUTER APPLY (
  SELECT title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE fa.actor_id = a.actor_id
  ORDER BY length(title) DESC
  FETCH FIRST 3 ROWS ONLY
) t
ORDER BY actor_id, length(title) DESC;
```

### LATERAL을 사용한 카테고리별 Top N

PostgreSQL:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
LEFT JOIN LATERAL (
  SELECT title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE fa.actor_id = a.actor_id
  ORDER BY length(title) DESC
  LIMIT 3
) t ON true
ORDER BY actor_id, length(title) DESC;
```

### 윈도우 함수를 사용한 카테고리별 Top N

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
LEFT JOIN (
  SELECT
    actor_id,
    title,
    rank() OVER (
      PARTITION BY actor_id
      ORDER BY length(title) DESC
    ) rk
  FROM film f
  JOIN film_actor fa USING (film_id)
) t USING (actor_id)
WHERE rk <= 3
ORDER BY actor_id, rk;
```

### 테이블 반환 함수 예제 (PostgreSQL)

LATERAL의 장점 중 하나는 함수화된 서브쿼리처럼 작동한다는 것입니다. 실제로 PostgreSQL에서는 테이블 반환 함수로 만들 수 있습니다:

```sql
CREATE FUNCTION top_3_films_per_actor(p_actor_id IN INTEGER)
RETURNS TABLE (
  title VARCHAR(50)
)
AS $
  SELECT title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE fa.actor_id = p_actor_id
  ORDER BY length(title) DESC
  LIMIT 3
$ LANGUAGE sql;

SELECT actor_id, first_name, last_name, title
FROM actor a
LEFT JOIN LATERAL top_3_films_per_actor(a.actor_id) t
ON 1 = 1
ORDER BY actor_id, length(title) DESC;
```

### WITH TIES를 사용한 카테고리별 Top 3

Oracle에서:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
OUTER APPLY (
  SELECT title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE fa.actor_id = a.actor_id
  ORDER BY length(title) DESC
  FETCH FIRST 3 ROWS WITH TIES
) t
ORDER BY actor_id, length(title) DESC;
```

SQL Server에서:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
OUTER APPLY (
  SELECT TOP 3 WITH TIES title
  FROM film f
  JOIN film_actor fa ON f.film_id = fa.film_id
  WHERE fa.actor_id = a.actor_id
  ORDER BY len(title) DESC
) t
ORDER BY actor_id, len(title) DESC;
```

## 성능 벤치마킹

저자는 여러 데이터베이스에서 APPLY/LATERAL 접근 방식과 윈도우 함수를 비교하는 벤치마크를 수행했습니다.

### Oracle 12.2.0.1.0 결과

카테고리별 Top 값 비교 (APPLY vs. 윈도우 함수):

| 실행 | Statement 1 (APPLY) | Statement 2 (윈도우 함수) |
|-----|-------------------|------------------------------|
| 1 | 9.20 | 1.07 |
| 2 | 12.86 | 1.04 |
| 3 | 13.81 | 1.10 |
| 4 | 13.92 | 1.16 |
| 5 | 9.37 | 1.00 |

윈도우 함수가 APPLY보다 약 9-13배 더 빠른 성능을 보였습니다.

### SQL Server 2014 결과

| 실행 | Statement 1 (APPLY) | Statement 2 (윈도우 함수) |
|-----|-------------------|------------------------------|
| 1 | 5.07 | 1.12 |
| 2 | 5.49 | 1.21 |
| 3 | 5.09 | 1.31 |
| 4 | 5.32 | 1.00 |
| 5 | 5.07 | 1.21 |

윈도우 함수가 4-5배의 성능 이점을 보여주었습니다.

### PostgreSQL 9.6 결과

| 실행 | Statement 1 (LATERAL) | Statement 2 (윈도우 함수) |
|-----|----------------------|------------------------------|
| 1 | 0.76 | 0.998 |
| 2 | 0.76 | 0.952 |
| 3 | 0.82 | 0.948 |
| 4 | 0.96 | 0.962 |
| 5 | 0.83 | 1.00 |

두 접근 방식 간의 성능이 비교적 유사했습니다.

### DB2 10.5 결과

| 실행 | Statement 1 (LATERAL) | Statement 2 (윈도우 함수) |
|-----|----------------------|------------------------------|
| 1 | 6.04 | 1.08 |
| 2 | 6.25 | 1.01 |
| 3 | 6.11 | 1.00 |
| 4 | 6.20 | 1.01 |

윈도우 함수가 LATERAL 조인보다 6배 더 나은 성능을 보여주었습니다.

### Oracle 힌트를 사용한 쿼리 최적화

쿼리 최적화 힌트가 추가된 경우:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
OUTER APPLY (
  SELECT /*+LEADING(fa f) USE_NL(fa f)*/ title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE actor_id = a.actor_id
  ORDER BY length(title) DESC
  FETCH FIRST 3 ROWS WITH TIES
) t
ORDER BY actor_id, length(title) DESC;
```

힌트 적용 결과:

- 윈도우 함수: 1.00 (기준)
- 힌트 없는 APPLY: 9.17
- LEADING 힌트가 있는 APPLY: 4.89
- LEADING과 USE_NL 힌트가 있는 APPLY: 1.65

## 주요 결론

TOP N 쿼리는 매우 일반적입니다. 가장 간단한 방법은 ORDER BY 절과 LIMIT 절을 사용하는 것입니다. 때때로 WITH TIES 시맨틱스가 필요하며, Oracle 12c와 SQL Server는 표준 또는 벤더 특정 구문으로 이를 기본적으로 제공할 수 있습니다. 다른 데이터베이스는 윈도우 함수를 사용하여 WITH TIES를 에뮬레이트할 수 있습니다.

카테고리별 TOP N이 필요한 경우, APPLY 또는 LATERAL 구문이 유용합니다. 그러나 벤치마크 결과에 따르면 이들은 동등한 윈도우 함수 대응물보다 훨씬 느릴 수 있습니다. 이는 데이터 세트의 크기에 따라 달라집니다.

윈도우 함수에서 발생하는 O(N log N) 정렬 비용을 과소평가하지 마세요. 항상 그렇듯이: 측정하세요, 절대 추측하지 마세요.
