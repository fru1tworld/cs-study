# ROW_NUMBER(), RANK(), DENSE_RANK()의 차이

> 원문: https://blog.jooq.org/the-difference-between-row_number-rank-and-dense_rank/

게시일: 2014년 8월 12일 | 저자: Lukas Eder

## 개요

윈도우 함수는 강력한 SQL 기능입니다. Dimitri Fontaine에 따르면: "윈도우 함수 이전의 SQL과 윈도우 함수 이후의 SQL이 있다."

세 가지 주요 순위 함수는 다음과 같습니다:
- `ROW_NUMBER()`
- `RANK()`
- `DENSE_RANK()`

## 샘플 데이터

```sql
CREATE TABLE t(v) AS
SELECT * FROM (
  VALUES('a'),('a'),('a'),('b'),
        ('c'),('c'),('d'),('e')
) t(v)
```

## ROW_NUMBER()

파티션 내의 각 행에 지정된 기준에 따라 순서대로 고유한 순차 번호를 할당합니다:

```sql
SELECT v, ROW_NUMBER() OVER(ORDER BY v)
FROM t
```

결과:
| V | ROW_NUMBER |
|---|------------|
| a | 1 |
| a | 2 |
| a | 3 |
| b | 4 |
| c | 5 |
| c | 6 |
| d | 7 |
| e | 8 |

## RANK()

`ROW_NUMBER()`와 비슷하지만, 동일한 값에 동일한 순위를 할당하여 번호에 간격이 생깁니다:

```sql
SELECT v, RANK() OVER(ORDER BY v)
FROM t
```

결과:
| V | RANK |
|---|------|
| a | 1 |
| a | 1 |
| a | 1 |
| b | 4 |
| c | 5 |
| c | 5 |
| d | 7 |
| e | 8 |

## DENSE_RANK()

연속적인 순위 번호를 할당하여 간격을 제거합니다:

```sql
SELECT v, DENSE_RANK() OVER(ORDER BY v)
FROM t
```

결과:
| V | DENSE_RANK |
|---|------------|
| a | 1 |
| a | 1 |
| a | 1 |
| b | 2 |
| c | 3 |
| c | 3 |
| d | 4 |
| e | 5 |

## 나란히 비교

```sql
SELECT
  v,
  ROW_NUMBER() OVER(w),
  RANK() OVER(w),
  DENSE_RANK() OVER(w)
FROM t
WINDOW w AS (ORDER BY v)
```

결과:
| V | ROW_NUMBER | RANK | DENSE_RANK |
|---|------------|------|------------|
| a | 1 | 1 | 1 |
| a | 2 | 1 | 1 |
| a | 3 | 1 | 1 |
| b | 4 | 4 | 2 |
| c | 5 | 5 | 3 |
| c | 6 | 5 | 3 |
| d | 7 | 7 | 4 |
| e | 8 | 8 | 5 |

## 핵심 통찰

`DENSE_RANK()`와 `DISTINCT`를 함께 사용하면 `DISTINCT` 없이 `ROW_NUMBER()`를 사용한 것과 유사하게 동작하여, 중복 값이 있는 순위 시나리오를 처리하는 대안적인 접근 방식을 제공합니다.
