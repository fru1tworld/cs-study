# SQL에서 누적 백분율을 계산하는 방법

> 원문: https://blog.jooq.org/how-to-calculate-a-cumulative-percentage-in-sql/

## 개요

이 블로그 글에서는 윈도우 함수를 사용하여 SQL에서 누적 백분율을 계산하는 방법을 설명합니다. Sakila 데이터베이스를 사용하여 시간이 지남에 따라 100%까지 누적되는 일별 수익 백분율을 보여주는 예제를 다룹니다.

## 문제

특정 날짜까지 전체 수익의 몇 퍼센트가 달성되었는지 알고 싶습니다. 예를 들어:
- 2005년 5월 24일: 전체 수익의 0.04%
- 2005년 8월 24일: 전체 수익의 100.00%

## 해결책: 2단계 접근법

### 1단계: 날짜별 집계

먼저 결제 거래를 날짜별로 그룹화합니다:

```sql
SELECT
  CAST(payment_date AS DATE),
  sum(amount) AS amount
FROM payment
GROUP BY CAST(payment_date AS DATE)
ORDER BY CAST(payment_date AS DATE);
```

이렇게 하면 개별 거래가 아닌 일별 합계가 생성됩니다.

### 2단계: 윈도우 함수 적용

윈도우 함수를 사용하여 누적 백분율을 계산합니다:

```sql
SELECT
  payment_date,
  amount,
  CAST(100 * sum(amount) OVER (ORDER BY payment_date)
           / sum(amount) OVER () AS numeric(10, 2)) percentage
FROM (
  SELECT
    CAST(payment_date AS DATE),
    sum(amount) AS amount
  FROM payment
  GROUP BY CAST(payment_date AS DATE)
) p
ORDER BY payment_date;
```

핵심 구성요소:
- `SUM(amount) OVER (ORDER BY payment_date)`: 현재 행까지의 누적 합계
- `SUM(amount) OVER ()`: 전체 행의 총합
- 나눗셈으로 백분율 산출

## 대안: 중첩 집계 함수

윈도우 함수 내에 집계 함수를 중첩하여 단순화할 수 있습니다:

```sql
SELECT
  CAST(payment_date AS DATE) AS payment_date,
  sum(amount) AS amount,
  CAST(100 * sum(sum(amount)) OVER (ORDER BY CAST(payment_date AS DATE))
           / sum(sum(amount)) OVER () AS numeric(10, 2)) percentage
FROM payment
GROUP BY CAST(payment_date AS DATE)
ORDER BY CAST(payment_date AS DATE);
```

`sum(sum(amount))` 구문은 먼저 집계를 계산한 다음 윈도우 함수를 적용합니다. 이렇게 하면 더 간결한 코드로 동일한 결과를 얻을 수 있습니다.
