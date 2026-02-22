# SQL 연산의 진정한 순서에 대한 초보자 가이드

> 원문: https://blog.jooq.org/a-beginners-guide-to-the-true-order-of-sql-operations/

## 핵심 문제

SQL은 영어처럼 읽히도록 설계되었습니다. `SELECT`로 시작하여 원하는 것을 "선택"하고, `FROM`으로 어디서 가져올지 지정하며, `WHERE`로 조건을 필터링합니다. 하지만 SQL의 구문적 순서(lexical order)와 논리적 순서(logical order)는 완전히 다릅니다.

이것이 바로 초보자들이 SQL, 특히 `GROUP BY` 절과 집계 함수(aggregate function)에서 혼란을 느끼는 근본적인 이유입니다.

## SQL 연산의 논리적 순서

SQL이 실제로 실행되는 순서는 다음과 같습니다:

1. FROM - 테이블에서 모든 행을 로드하고 조인(join)을 수행
2. WHERE - 그룹화 전에 행을 필터링
3. GROUP BY - 행을 그룹으로 조직화
4. 집계 함수(Aggregations) - SUM, COUNT 등의 집계 함수 계산
5. HAVING - 집계 값을 기준으로 그룹 필터링
6. 윈도우 함수(WINDOW) - 윈도우 함수 계산
7. SELECT - 최종 컬럼 투영(projection)
8. DISTINCT - 중복 행 제거
9. UNION/INTERSECT/EXCEPT - 결과 집합 결합
10. ORDER BY - 결과 정렬
11. OFFSET/LIMIT (또는 FETCH) - 반환할 행 수 제한

## GROUP BY가 혼란을 일으키는 이유

`GROUP BY`를 사용하면 그룹화되지 않은 컬럼들은 `SELECT`에서 "보이지 않게" 됩니다. 집계 함수로 감싸지 않는 한 해당 컬럼에 직접 접근할 수 없습니다.

이것은 Java의 `Map<String, List<Row>>`와 비슷하게 생각할 수 있습니다. 그룹화 키로 데이터를 묶으면, 각 그룹 내의 개별 행들은 집계를 통해서만 접근할 수 있습니다.

### 잘못된 쿼리 예시

```sql
SELECT first_name, last_name, count(*)
FROM customer
GROUP BY first_name
```

이 쿼리는 실패합니다. `last_name`이 `GROUP BY`에 포함되지 않았고, 집계 함수로 감싸지도 않았기 때문입니다.

### 올바른 쿼리 예시

```sql
SELECT first_name, MAX(last_name), count(*)
FROM customer
GROUP BY first_name
```

`last_name`을 `MAX()` 집계 함수로 감싸면 쿼리가 정상적으로 동작합니다.

## WHERE vs HAVING

또 다른 일반적인 혼란은 `WHERE`와 `HAVING`의 차이입니다.

### 잘못된 쿼리

```sql
SELECT first_name, count(*)
FROM customer
WHERE count(*) > 1
GROUP BY first_name
```

이 쿼리는 실패합니다. 집계 함수는 `WHERE` 절 이후에 계산되기 때문에, `WHERE`에서는 집계 함수를 사용할 수 없습니다.

### 올바른 쿼리

```sql
SELECT first_name, count(*)
FROM customer
GROUP BY first_name
HAVING count(*) > 1
```

`HAVING`은 집계 *이후*에 실행되므로, 집계 함수의 결과를 필터링할 수 있습니다.

## 윈도우 함수와 집계 함수의 조합

논리적 순서를 이해하면 다음과 같은 고급 쿼리도 이해할 수 있습니다:

```sql
SELECT
  payment_date,
  SUM(SUM(amount)) OVER (ORDER BY payment_date) AS revenue
FROM payment
GROUP BY payment_date
```

이 쿼리가 동작하는 이유는 집계 함수(`SUM(amount)`)가 윈도우 함수(`SUM(...) OVER (...)`)보다 먼저 논리적으로 실행되기 때문입니다. 내부의 `SUM(amount)`가 먼저 각 날짜별로 계산되고, 그 결과에 대해 윈도우 함수가 누적 합계를 계산합니다.

## ORDER BY의 특별한 경우

`ORDER BY`에는 한 가지 예외가 있습니다. `SELECT`에 포함되지 않은 컬럼을 `ORDER BY`에서 참조할 수 있습니다. 하지만 이는 `DISTINCT`나 집합 연산(`UNION`, `INTERSECT`, `EXCEPT`)이 없을 때만 가능합니다.

```sql
-- 가능
SELECT first_name
FROM customer
ORDER BY last_name

-- 불가능 (DISTINCT가 있으면 어떤 last_name으로 정렬해야 할지 모호)
SELECT DISTINCT first_name
FROM customer
ORDER BY last_name
```

## 파생 테이블(Derived Table)을 활용한 해결책

쿼리 로직이 SQL의 구조와 맞지 않을 때, 파생 테이블(서브쿼리)을 사용하여 원하는 논리적 실행 순서를 강제할 수 있습니다.

```sql
SELECT *
FROM (
  SELECT first_name, count(*) AS cnt
  FROM customer
  GROUP BY first_name
) AS grouped_customers
WHERE cnt > 1
```

내부 쿼리에서 먼저 그룹화와 집계가 완료된 후, 외부 쿼리에서 일반적인 `WHERE` 절로 필터링할 수 있습니다.

## 결론

SQL을 작성할 때 구문에 맞서 싸우지 마세요. 대신 눈을 감고 논리적 연산 순서를 기억하세요:

1. FROM (테이블 로드)
2. WHERE (행 필터링)
3. GROUP BY (그룹화)
4. 집계 함수 계산
5. HAVING (그룹 필터링)
6. 윈도우 함수 계산
7. SELECT (컬럼 선택)
8. DISTINCT (중복 제거)
9. 집합 연산
10. ORDER BY (정렬)
11. LIMIT/OFFSET (결과 제한)

이 논리적 순서를 이해하면, SQL이 혼란스러운 것에서 이해할 수 있는 것으로 변화합니다. 특정 쿼리가 왜 실패하는지, 어떻게 수정해야 하는지 명확하게 알 수 있게 됩니다.
