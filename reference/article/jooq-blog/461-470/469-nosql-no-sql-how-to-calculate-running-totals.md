# NoSQL? No, SQL! - 누적 합계를 계산하는 방법

> 원문: https://blog.jooq.org/nosql-no-sql-how-to-calculate-running-totals/

## 소개

다양한 컨퍼런스에서 jOOQ 강연을 하면서 다음과 같은 관찰을 하게 되었습니다:

> "Java 개발자들은 SQL을 모릅니다."

이것은 개발자들의 잘못이 아닙니다. 오히려 SQL에 대한 노출이 제한되어 있기 때문입니다. 조직들은 비싼 RDBMS 시스템에 수백만 달러를 투자하지만, Hibernate와 같은 ORM을 통해 기능의 10%만 사용하면서 SQL:1999부터 SQL:2011까지의 현대 SQL 표준을 무시하고 있습니다.

## 핵심 문제: 누적 합계

은행 계좌 거래 예제를 사용하여 누적 합계를 정의해 보겠습니다.

원본 데이터:

| ID   | VALUE_DATE | AMOUNT |
|------|------------|--------|
| 9997 | 2014-03-18 |  99.17 |
| 9981 | 2014-03-16 |  71.44 |
| 9979 | 2014-03-16 | -94.60 |
| 9977 | 2014-03-16 |  -6.96 |
| 9971 | 2014-03-15 | -65.95 |

필요한 출력 (BALANCE 열 포함):

| ID   | VALUE_DATE | AMOUNT | BALANCE  |
|------|------------|--------|----------|
| 9997 | 2014-03-18 |  99.17 | 19985.81 |
| 9981 | 2014-03-16 |  71.44 | 19886.64 |
| 9979 | 2014-03-16 | -94.60 | 19815.20 |
| 9977 | 2014-03-16 |  -6.96 | 19909.80 |
| 9971 | 2014-03-15 | -65.95 | 19916.76 |

계산 공식: BALANCE(ROWn) = BALANCE(ROWn+1) + AMOUNT(ROWn)

## 해결책 1: 중첩 SELECT

```sql
SELECT
  t1.*,
  t1.current_balance - (
    SELECT NVL(SUM(amount), 0)
    FROM v_transactions t2
    WHERE t2.account_id = t1.account_id
    AND (t2.value_date, t2.id) >
        (t1.value_date, t1.id)
  ) AS balance
FROM     v_transactions t1
WHERE    t1.account_id = 1
ORDER BY t1.value_date DESC, t1.id DESC
```

행 값 표현식 없이 작성한 대안:

```sql
SELECT
  t1.*,
  t1.current_balance - (
    SELECT NVL(SUM(amount), 0)
    FROM v_transactions t2
    WHERE t2.account_id = t1.account_id
    AND ((t2.value_date > t1.value_date) OR
         (t2.value_date = t1.value_date AND
          t2.id         > t1.id))
  ) AS balance
FROM     v_transactions t1
WHERE    t1.account_id = 1
ORDER BY t1.value_date DESC, t1.id DESC
```

성능 문제:
실행 계획을 보면 O(n^2) 복잡도를 보여줍니다. 1,101개의 레코드에 대해 Oracle은 1,212K 행을 실체화하며 실행 시간은 770ms입니다. 이렇게 단순한 작업치고는 너무 느립니다.

## 해결책 2: 재귀 SQL (공통 테이블 표현식)

TRANSACTION_NR 열이 필요합니다:

```sql
WITH ordered_with_balance (
  account_id, value_date, amount,
  balance, transaction_number
)
AS (
  SELECT t1.account_id, t1.value_date, t1.amount,
         t1.current_balance, t1.transaction_number
  FROM   v_transactions_by_time t1
  WHERE  t1.transaction_number = 1

  UNION ALL

  SELECT t1.account_id, t1.value_date, t1.amount,
         t2.balance - t2.amount, t1.transaction_number
  FROM   ordered_with_balance t2
  JOIN   v_transactions_by_time t1
  ON     t1.transaction_number =
         t2.transaction_number + 1
  AND    t1.account_id = t2.account_id
)
SELECT   *
FROM     ordered_with_balance
WHERE    account_id= 1
ORDER BY transaction_number ASC
```

작동 방식:
첫 번째 SELECT는 transaction_number = 1인 현재 잔액으로 초기화합니다. 그 다음 재귀 SELECT는 이전 잔액에서 이전 금액을 뺀 값으로 각 후속 잔액을 계산합니다.

성능 문제:
이 접근 방식은 실제로 중첩 SELECT보다 더 나쁜 성능을 보입니다. 재귀의 계산 오버헤드와 TRANSACTION_NR 열 계산으로 인해 1,101개 레코드에 대해 11M 행을 실체화합니다.

## 해결책 3: 윈도우 함수 (권장)

```sql
SELECT
  t.*,
  t.current_balance - NVL(
    SUM(t.amount) OVER (
      PARTITION BY t.account_id
      ORDER BY     t.value_date DESC,
                   t.id         DESC
      ROWS BETWEEN UNBOUNDED PRECEDING
           AND     1         PRECEDING
    ),
  0) AS balance
FROM     v_transactions t
WHERE    t.account_id = 1
ORDER BY t.value_date DESC,
         t.id         DESC
```

작동 방식:
윈도우 함수는 동일한 파티션(계좌) 내의 모든 금액의 합계를 계산합니다. value_date와 id를 내림차순으로 정렬하고, 현재 행 이전의 행들에 대해서만 계산합니다(ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING).

성능:
이 솔루션은 최적의 O(n) 복잡도와 최소한의 리소스 사용을 보여줍니다. 이전 접근 방식들보다 극적으로 빠릅니다.

## 해결책 4: Oracle MODEL 절

```sql
SELECT account_id, value_date, amount, balance
FROM (
  SELECT id, account_id, value_date, amount,
         current_balance AS balance
  FROM   v_transactions
) t
WHERE account_id = 1
MODEL
  PARTITION BY (account_id)
  DIMENSION BY (
    ROW_NUMBER() OVER (
      ORDER BY value_date DESC, id DESC
    ) AS rn
  )
  MEASURES (value_date, amount, balance)
  RULES (
    balance[rn > 1] = balance[cv(rn) - 1]
                    - amount [cv(rn) - 1]
  )
ORDER BY rn ASC
```

작동 방식:
이 절은 데이터를 파티션, 차원(정렬 순서), 측정값(계산된 열), 규칙이 있는 스프레드시트처럼 취급합니다. 첫 번째 행 이후의 행들에 대해, 잔액은 이전 행의 잔액에서 이전 행의 금액을 뺀 값과 같습니다.

성능:
윈도우 함수와 비슷하게 좋은 성능을 보이지만, 문법이 더 독특합니다.

## 비교 분석

이 글은 윈도우 함수가 SQL 기능에서 근본적인 변화를 나타낸다고 강조합니다. Dimitri Fontaine의 말을 인용하면: "윈도우 함수 이전의 SQL이 있고, 윈도우 함수 이후의 SQL이 있습니다."

## 결론

Java 개발자들은 기업이 RDBMS 기술에 상당한 투자를 했음에도 불구하고 SQL을 충분히 활용하지 못하고 있습니다. 윈도우 함수와 유사한 SQL 기능에 대한 지식이 있으면 개발자들은 수천 줄의 Java 코드를 다섯 줄의 SQL로 대체할 수 있으며, 종종 명령형 구현보다 더 나은 성능을 얻을 수 있습니다. 이 글은 RDBMS의 기능을 활용하기 위해 Java 개발자들 사이에서 SQL 교육이 개선되어야 한다고 주장합니다.
