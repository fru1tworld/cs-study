# SQL에서 테이블 조인 시 가중 평균 계산하기

> 원문: https://blog.jooq.org/calculating-weighted-averages-when-joining-tables-in-sql/

2019년 3월 15일 lukaseder 작성

## 문제 설명

다음 두 테이블로 구성된 데이터베이스 스키마가 있다고 가정해봅시다:

```sql
create table transactions (
  id     bigint         not null primary key,
  lines  bigint         not null,
  price  numeric(18, 2) not null,
  profit numeric(18, 2) not null
);

create table lines (
  id             bigint         not null primary key,
  transaction_id bigint         not null references transactions,
  total          bigint         not null,
  quantity       bigint         not null,
  profit         numeric(18, 2) not null
);
```

이 스키마는 약간 비정규화되어 있으며, 트랜잭션당 라인 수가 미리 계산되어 저장되어 있습니다. 라인 항목에서 합계를 계산하고 트랜잭션에서 평균을 계산하는 리포트가 필요합니다.

## 별도 쿼리로 실행 (올바른 방법)

라인 항목에서 합계 계산:

```sql
SELECT
  sum(profit)   AS total_profit,
  sum(total)    AS total_sales_amount,
  sum(quantity) AS total_items_sold
FROM lines
```

트랜잭션에서 평균 계산:

```sql
SELECT
  avg(lines)  AS avg_items_p_trx,
  avg(price)  AS avg_price_p_trx,
  avg(profit) AS avg_profit_p_trx
FROM transactions
```

## 쿼리를 결합할 때 발생하는 문제

단순히 조인하면 행 중복으로 인해 잘못된 평균이 계산됩니다:

```sql
SELECT
  sum(l.profit)   AS total_profit,
  sum(l.total)    AS total_sales_amount,
  sum(l.quantity) AS total_items_sold,
  avg(t.lines)    AS avg_items_p_trx,
  avg(t.price)    AS avg_price_p_trx,
  avg(t.profit)   AS avg_profit_p_trx
FROM lines AS l
JOIN transactions AS t ON t.id = l.transaction_id
```

예제 출력을 보면 트랜잭션 행이 각 라인에 대해 반복됩니다:

| LINE_ID | TRANSACTION_ID | LINES | PRICE  |
|---------|----------------|-------|--------|
| 1       | 1              | 3     | 20.00  |
| 2       | 1              | 3     | 20.00  |
| 3       | 1              | 3     | 20.00  |
| 4       | 2              | 5     | 100.00 |
| 5       | 2              | 5     | 100.00 |
| 6       | 2              | 5     | 100.00 |
| 7       | 2              | 5     | 100.00 |
| 8       | 2              | 5     | 100.00 |

트랜잭션당 평균 항목 수는 4가 되어야 하지만 4.25로 계산됩니다. 평균 가격은 60.00이 되어야 하지만 80.00으로 계산됩니다.

## 가중 평균 해결책

각 값을 반복 횟수로 나눈 다음, 고유 트랜잭션 수로 나눕니다:

```sql
SELECT
  sum(l.profit)   AS total_profit,
  sum(l.total)    AS total_sales_amount,
  sum(l.quantity) AS total_items_sold,
  sum(t.lines  / t.lines) / count(DISTINCT t.id) avg_items_p_trx,
  sum(t.price  / t.lines) / count(DISTINCT t.id) avg_price_p_trx,
  sum(t.profit / t.lines) / count(DISTINCT t.id) avg_profit_p_trx
FROM lines AS l
JOIN transactions AS t ON t.id = l.transaction_id
```

라인 수에 대한 단순화된 버전:

```sql
SELECT
  sum(l.profit)   AS total_profit,
  sum(l.total)    AS total_sales_amount,
  sum(l.quantity) AS total_items_sold,
  count(*)                / count(DISTINCT t.id) avg_items_p_trx,
  sum(t.price  / t.lines) / count(DISTINCT t.id) avg_price_p_trx,
  sum(t.profit / t.lines) / count(DISTINCT t.id) avg_profit_p_trx
FROM lines AS l
JOIN transactions AS t ON t.id = l.transaction_id
```

## 사전 집계를 사용한 정규화된 대안

```sql
SELECT
  sum(l.profit_per_transaction)   AS total_profit,
  sum(l.total_per_transaction)    AS total_sales_amount,
  sum(l.quantity_per_transaction) AS total_items_sold,
  avg(l.lines_per_transaction)    AS avg_items_p_trx,
  avg(t.price)                    AS avg_price_p_trx,
  avg(t.profit)                   AS avg_profit_p_trx
FROM (
  SELECT
    l.transaction_id,
    sum(l.profit)   AS profit_per_transaction,
    sum(l.total)    AS total_per_transaction,
    sum(l.quantity) AS quantity_per_transaction,
    count(*)        AS lines_per_transaction
  FROM lines AS l
  GROUP BY l.transaction_id
) AS l
JOIN transactions AS t ON t.id = l.transaction_id
```

이 접근 방식은 조인하기 전에 라인 데이터를 트랜잭션당 하나의 행으로 사전 집계하여 중복을 제거합니다. 이렇게 하면 각 트랜잭션이 라인 수에 관계없이 동일하게 기여하도록 정규화됩니다.
