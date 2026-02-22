# SQL에서 곱셈 집계 함수를 작성하는 방법

> 원문: https://blog.jooq.org/how-to-write-a-multiplication-aggregate-function-in-sql/

## 소개

SQL의 `SUM()` 집계 함수는 잘 알려져 있습니다. Sakila 데이터베이스를 사용하여 일별 수익과 누적 수익을 계산하는 방법을 보여드리겠습니다:

```sql
WITH p AS (
  SELECT
    CAST (payment_date AS DATE) AS date,
    amount
  FROM payment
)
SELECT
  date,
  SUM (amount) AS daily_revenue,
  SUM (SUM (amount)) OVER (ORDER BY date) AS cumulative_revenue
FROM p
GROUP BY date
ORDER BY date
```

이 쿼리는 2005년 5월부터 6월까지의 일별 수익과 누적 합계를 보여주는 결과를 생성합니다.

## 곱셈 문제

이제 덜 일반적인 시나리오로 초점을 옮겨보겠습니다: 덧셈이 아닌 곱셈을 통해 값을 집계하는 것입니다. 이 예제는 일별 계수(factor)와 누적 값이 있는 금융 데이터를 포함합니다:

원하는 출력은 1986년 1월부터 2월까지의 날짜와 계수 열, 누적 결과를 보여줍니다. 1000에서 시작하여 다음 공식을 사용해 누적 곱셈 조정을 적용합니다: `accumulated(i) = accumulated(i-1) * (1 + factor)`

## 로그를 사용한 수학적 해결책

사용자 정의 집계 함수를 구현하는 대신, 저자는 로그 항등식을 사용할 것을 제안합니다. 핵심 통찰력은 다음 수학적 원리에서 나옵니다:

"log_b(x * y) = log_b(x) + log_b(y)"는 "x * y = b^(log_b(x) + log_b(y))"를 의미합니다.

자연로그와 지수함수를 사용하여 SQL에 적용하면:

MUL(x) = EXP(SUM(LN(x)))

원래 문제에 대한 해결책은 다음과 같습니다:

```sql
SELECT
  date,
  factor,
  1000 * (EXP(SUM(LN(1 + COALESCE(factor, 1)))
       OVER (ORDER BY date)) - 1) AS accumulated
FROM t
```

참고: 데이터베이스 시스템에 따라 `LN()`을 `LOG()`로 대체해야 할 수 있습니다.

## 에지 케이스 처리: 음수

이 글에서는 로그가 음수 값을 처리할 수 없다는 점을 다룹니다. 음수를 포함하는 데이터셋의 경우 부호 처리가 필요합니다:

```sql
WITH v(i) AS (VALUES (-2), (-3), (-4))
SELECT
  CASE
    WHEN SUM (CASE WHEN i < 0 THEN -1 END) % 2 < 0
    THEN -1
    ELSE 1
  END * EXP(SUM(LN(ABS(i)))) multiplication1
FROM v;
```

이것은 세 개의 음수를 곱한 결과로 대략 -24를 생성합니다.

## 에지 케이스 처리: 0 값

0은 0의 로그가 정의되지 않기 때문에 별도의 도전 과제를 제시합니다. 해결책은 두 단계를 포함합니다: `NULLIF()`를 사용하여 로그 계산에서 0을 제외하고, 피연산자 중 하나라도 0이면 0을 반환하도록 `CASE` 표현식을 추가합니다:

```sql
WITH v(i) AS (VALUES (2), (3), (0))
SELECT
  CASE
    WHEN SUM (CASE WHEN i = 0 THEN 1 END) > 0
    THEN 0
    WHEN SUM (CASE WHEN i < 0 THEN -1 END) % 2 < 0
    THEN -1
    ELSE 1
  END * EXP(SUM(LN(ABS(NULLIF(i, 0))))) multiplication
FROM v;
```

## DISTINCT 확장

`DISTINCT` 값의 곱만 계산하려면 세 가지 집계 구성 요소 중 두 가지에 `DISTINCT` 키워드를 적용합니다:

```sql
WITH v(i) AS (VALUES (2), (3), (3))
SELECT
  CASE
    WHEN SUM (CASE WHEN i = 0 THEN 1 END) > 0
    THEN 0
    WHEN SUM (DISTINCT CASE WHEN i < 0 THEN -1 END) % 2 < 0
    THEN -1
    ELSE 1
  END * EXP(SUM(DISTINCT LN(ABS(NULLIF(i, 0))))) multiplication
FROM v;
```

이것은 6을 반환합니다 (2 × 3, 중복된 3 제외).

## 윈도우 함수 확장

집계 함수를 윈도우 함수로 변환하려면 각 `SUM()`을 해당하는 윈도우 함수로 변환해야 합니다:

```sql
WITH v(i, j) AS (
  VALUES (1, 2), (2, -3), (3, 4),
         (4, -5), (5, 0), (6, 0)
)
SELECT i, j,
  CASE
    WHEN SUM (CASE WHEN j = 0 THEN 1 END)
      OVER (ORDER BY i) > 0
    THEN 0
    WHEN SUM (CASE WHEN j < 0 THEN -1 END)
      OVER (ORDER BY i) % 2 < 0
    THEN -1
    ELSE 1
  END * EXP(SUM(LN(ABS(NULLIF(j, 0))))
    OVER (ORDER BY i)) multiplication
FROM v;
```

결과는 누적 곱이 0에 도달할 때까지 증가하다가 그 후 0으로 유지되는 것을 보여줍니다.

## jOOQ 프레임워크 지원

이 글에서는 jOOQ 3.12가 GitHub 저장소의 이슈 #5939를 참조하여 데이터베이스별 자동 에뮬레이션과 함께 이 패턴을 지원할 것이라고 언급합니다.

## Oracle 성능 고려사항

마지막 참고사항으로, Oracle의 `LN()` 함수는 `number` 타입보다 `binary_double` 타입에서 작동할 때 훨씬 더 빠르게 수행됩니다—저자의 테스트에 따르면 잠재적으로 "100배 더 빠릅니다"—따라서 명시적 타입 캐스팅은 최적화에 가치가 있습니다.
