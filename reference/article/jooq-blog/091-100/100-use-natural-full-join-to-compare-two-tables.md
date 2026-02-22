# SQL에서 NATURAL FULL JOIN으로 두 테이블 비교하기

> 원문: https://blog.jooq.org/use-natural-full-join-to-compare-two-tables-in-sql/

## 개요

이 글에서는 전통적인 `UNION`과 `EXCEPT` 방식의 대안으로 `NATURAL FULL JOIN`을 사용하여 두 유사한 SQL 테이블을 비교하는 방법을 설명합니다.

## 문제 설정

동일한 스키마를 가진 두 테이블이 있습니다:

```sql
CREATE TABLE t1 (a INT, b INT, c INT);
CREATE TABLE t2 (a INT, b INT, c INT);
INSERT INTO t1 VALUES (1, 2, 3), (4, 5, 6), (7, 8, 9);
INSERT INTO t2 VALUES            (4, 5, 6), (7, 8, 9), (10, 11, 12);
```

목표는 한 테이블에는 존재하지만 다른 테이블에는 존재하지 않는 행을 찾는 것입니다.

## 전통적인 방식: UNION과 EXCEPT 사용

```sql
(TABLE t1 EXCEPT TABLE t2)
UNION
(TABLE t2 EXCEPT TABLE t1)
ORDER BY a, b, c
```

이 방식은 각 테이블에 고유한 행을 반환하지만 "각 테이블에 두 번씩 접근해야 합니다."

## NATURAL FULL JOIN 해결책

권장되는 방식은 마커 컬럼 전략을 사용합니다:

```sql
SELECT *
FROM (
  SELECT 't1' AS t1, t1.* FROM t1
) t1 NATURAL FULL JOIN (
  SELECT 't2' AS t2, t2.* FROM t2
) t2
WHERE NOT (t1, t2) IS NOT NULL;
```

## 대안적인 구문들

JOIN...USING 사용:

```sql
SELECT *
FROM (
  SELECT 't1' AS t1, t1.* FROM t1
) t1 FULL JOIN (
  SELECT 't2' AS t2, t2.* FROM t2
) t2 USING (a, b, c)
WHERE NOT (t1, t2) IS NOT NULL;
```

COALESCE와 함께 JOIN...ON 사용:

```sql
SELECT
  coalesce(t1.a, t2.a) AS a,
  coalesce(t1.b, t2.b) AS b,
  coalesce(t1.c, t2.c) AS c,
  t1.t1,
  t2.t2
FROM (
  SELECT 't1' AS t1, t1.* FROM t1
) t1 FULL JOIN (
  SELECT 't2' AS t2, t2.* FROM t2
) t2 ON (t1.a, t1.b, t1.c) = (t2.a, t2.b, t2.c)
WHERE NOT (t1, t2) IS NOT NULL;
```

## NULL 값 처리

NULL 값이 존재할 때는 `IS NOT DISTINCT FROM` 술어를 사용합니다:

```sql
SELECT
  coalesce(t1.a, t2.a) AS a,
  coalesce(t1.b, t2.b) AS b,
  coalesce(t1.c, t2.c) AS c,
  t1.t1,
  t2.t2
FROM (
  SELECT 't1' AS t1, t1.* FROM t1
) t1 FULL JOIN (
  SELECT 't2' AS t2, t2.* FROM t2
) t2 ON (t1.a, t1.b, t1.c) IS NOT DISTINCT FROM (t2.a, t2.b, t2.c)
WHERE NOT (t1, t2) IS NOT NULL;
```

## 행 값 표현식의 NULL 술어

SQL에서 흥미로운 특징이 있습니다: 다중 차수 표현식에서 `NOT R IS NOT NULL`은 `R IS NULL`과 다릅니다. 특히 일부 값이 NULL인 경우에 그렇습니다.

- `R IS NULL`: 모든 값이 NULL일 때만 참
- `R IS NOT NULL`: 모든 값이 NULL이 아닐 때만 참
- `NOT R IS NOT NULL`: 하나 이상의 값이 NULL일 때 참

이 특성을 활용하여 FULL JOIN 결과에서 매칭되지 않은 행(한쪽이 NULL인 행)을 필터링할 수 있습니다.

## 주요 장점과 한계

장점:
- 각 테이블에 한 번만 접근
- 컬럼 인덱스 기반이 아닌 이름 기반 비교

한계:
- 중복 데이터가 있으면 카테시안 곱이 발생
- NULL 처리가 UNION/EXCEPT와 다름
- PostgreSQL 12는 특정 조건에서 FULL JOIN을 지원하지 않음
