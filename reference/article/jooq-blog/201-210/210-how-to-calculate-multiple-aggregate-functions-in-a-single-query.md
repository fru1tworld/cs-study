# 단일 쿼리에서 여러 집계 함수(Aggregate Functions)를 계산하는 방법

> 원문: https://blog.jooq.org/how-to-calculate-multiple-aggregate-functions-in-a-single-query/

이 글에서는 Sakila 데이터베이스를 예시로 사용하여 단일 SQL 쿼리에서 여러 집계 함수를 계산하는 네 가지 접근 방식을 설명합니다.

## 문제 상황

한 개발자가 단일 테이블에서 서로 다른 조건(predicate)으로 레코드 수를 세야 하는 상황이 있었습니다. 8개의 개별 쿼리(또는 모든 조합을 고려하면 32개)를 실행하는 대신, 이를 효율적으로 결합하는 방법을 보여드리겠습니다.

세 가지 조건은 다음과 같습니다:
- length가 120에서 150 사이
- language_id가 1
- rating이 'PG'

## 해결책 1: 필터링된 집계 함수 (모든 데이터베이스에서 사용 가능)

`CASE` 표현식을 사용하여 `NULL` 또는 숫자 값을 생성하고, `COUNT()` 함수가 `NULL`을 무시한다는 특성을 활용하는 방법입니다. 이 방식은 모든 데이터베이스에서 작동합니다:

```sql
SELECT
  count(*),
  count(CASE WHEN length BETWEEN 120 AND 150 THEN 1 END),
  count(CASE WHEN language_id = 1 THEN 1 END),
  count(CASE WHEN rating = 'PG' THEN 1 END)
FROM film
```

여러 조건을 결합해야 할 경우, nullable 컬럼에 대한 산술 연산을 사용하면 모든 조건이 참이어야 합니다:

```sql
count(length + language_id)  -- 둘 다 NULL이 아닐 때만 카운트
```

## 해결책 2: FILTER 절 (PostgreSQL/HSQLDB)

PostgreSQL과 HSQLDB는 SQL 표준에 정의된 전용 구문을 지원합니다:

```sql
SELECT
  count(*) FILTER (WHERE length BETWEEN 120 AND 150),
  count(*) FILTER (WHERE language_id = 1),
  count(*) FILTER (WHERE rating = 'PG')
FROM film
```

이 구문은 조건을 더 명시적으로 표현할 수 있어 가독성이 좋습니다.

## 해결책 3: PIVOT (Oracle/SQL Server)

Oracle의 `PIVOT` 구문은 자동으로 그룹화하고 집계합니다:

```sql
SELECT * FROM (
  SELECT
    CASE WHEN length BETWEEN 120 AND 150 THEN 1 ELSE 0 END length,
    CASE WHEN language_id = 1 THEN 1 ELSE 0 END language_id,
    CASE WHEN rating = 'PG' THEN 1 ELSE 0 END rating
  FROM film
) PIVOT (
  count(*) FOR (length, language_id, rating) IN (
    (0,0,0) AS a, (0,0,1) AS b, (0,1,0) AS c, (0,1,1) AS d,
    (1,0,0) AS e, (1,0,1) AS f, (1,1,0) AS g, (1,1,1) AS h
  )
)
```

그 다음 결과 컬럼들을 합산하여 원하는 카운트를 얻습니다.

## 해결책 4: GROUPING SETS/CUBE (다중 벤더 지원)

DB2, HANA, Oracle, PostgreSQL, SQL Server, Sybase에서 지원됩니다:

```sql
SELECT
  GROUPING_ID(length, language_id, rating),
  length, language_id, rating,
  count(*)
FROM (
  SELECT
    CASE WHEN length BETWEEN 120 AND 150 THEN 1 ELSE 0 END length,
    CASE WHEN language_id = 1 THEN 1 ELSE 0 END language_id,
    CASE WHEN rating = 'PG' THEN 1 ELSE 0 END rating
  FROM film
) CUBE(length, language_id, rating)
HAVING COALESCE(length, 1) != 0
  AND COALESCE(language_id, 1) != 0
  AND COALESCE(rating, 1) != 0
```

`CUBE()` 함수는 모든 가능한 컬럼 조합에 대한 그룹화를 간편하게 생성합니다. `GROUPING_ID` 함수는 어떤 컬럼이 현재 그룹화에 포함되어 있는지를 비트마스크로 알려줍니다.

## 성능 결과

Sakila 데이터셋(1000개 행)을 사용한 Oracle에서 2000회 반복, 5회 실행 평균 결과:

| 방식 | 소요 시간 |
|------|-----------|
| 8개 개별 쿼리 | ~1.65초 |
| 수동 PIVOT (CASE 방식) | ~1.01초 |
| PIVOT 구문 | ~2.56초 (가장 느림) |
| GROUPING SETS/CUBE | ~0.81초 (가장 빠름) |

실제 2억 개 행의 테이블에서 수동 피벗 접근 방식은 개별 쿼리 대비 약 20배의 성능 향상을 제공했습니다.

## 핵심 요점

- `COUNT()` 함수는 `NULL` 값을 무시합니다 - 이 특성을 활용하면 조건별 카운트를 단일 쿼리로 계산할 수 있습니다.
- 필터링된 집계 함수(CASE를 사용한 방식)는 모든 데이터베이스에서 작동하는 범용 솔루션입니다.
- `FILTER` 절은 PostgreSQL과 HSQLDB에서 가장 깔끔한 구문을 제공합니다.
- `GROUPING SETS`와 `CUBE`는 많은 조합이 필요할 때 가장 빠른 성능을 보여줍니다.
- 여러 개의 개별 쿼리를 실행하는 대신 단일 쿼리로 집계를 통합하면 데이터베이스 왕복(round-trip)을 줄이고 성능을 크게 향상시킬 수 있습니다.
