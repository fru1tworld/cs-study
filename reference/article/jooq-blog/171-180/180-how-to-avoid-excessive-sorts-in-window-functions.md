# 윈도우 함수에서 과도한 정렬을 피하는 방법

> 원문: https://blog.jooq.org/how-to-avoid-excessive-sorts-in-window-functions/

윈도우 함수는 SQL의 강력한 도구이지만, 성능 비용이 따릅니다: 바로 정렬 연산입니다. 이 글에서는 불필요한 정렬을 피하고 조건절 푸시다운(predicate push-down)을 통해 윈도우 함수를 사용하는 쿼리를 최적화하는 방법을 살펴봅니다.

## 핵심 문제

윈도우 함수를 뷰나 파생 테이블에서 사용할 때, 데이터베이스 옵티마이저는 윈도우 함수 계산을 지나서 조건절(WHERE 절)을 푸시다운하는 데 어려움을 겪습니다. 이로 인해 데이터베이스는 필터링 전에 더 큰 데이터셋을 처리해야 하며, 결과적으로 비용이 많이 드는 정렬 연산이 발생합니다.

윈도우 함수는 결과를 계산하기 위해 정렬이 필요하며, 이는 O(n log n) 복잡도를 따릅니다. 윈도우 함수가 뷰나 파생 테이블에 배치되면, 대부분의 데이터베이스는 범위 조건절을 효과적으로 푸시다운하지 못하여 불필요한 전체 데이터 처리를 강제합니다.

## 예제 쿼리

이 글에서는 Sakila 데이터베이스를 사용하여 누적 수익을 계산하는 예제를 보여줍니다:

```sql
SELECT
  customer_id,
  payment_date,
  amount,
  SUM(amount) OVER (
    PARTITION BY customer_id
    ORDER BY payment_date, payment_id
  ) cumulative_amount
FROM payment
ORDER BY customer_id, payment_date, payment_id;
```

## 뷰 생성

누적 수익을 계산하는 뷰를 생성합니다:

```sql
CREATE VIEW payment_with_revenue AS
SELECT
  customer_id,
  payment_date,
  amount,
  SUM(amount) OVER (
    PARTITION BY customer_id
    ORDER BY payment_date, payment_id
  ) cumulative_amount
FROM payment;
```

나중에 이 뷰를 필터링할 때, "데이터베이스는 의미론적으로 가능함에도 불구하고 `PAYMENT_DATE`에 대한 범위 조건절을 윈도우 함수를 지나서 푸시다운할 수 없습니다."

## 주요 발견

"윈도우 함수는 '울타리'처럼 작동하여, 그 너머로 자동으로 푸시될 수 있는 조건절이 거의 없습니다." 이 글은 `CUSTOMER_ID`에 대한 조건절은 때때로 푸시다운되지만, 날짜 범위 조건절은 데이터베이스 쿼리 플래너에 의해 거의 최적화되지 않는다는 것을 보여줍니다.

## 최적화 전략

이 글은 두 가지 방식으로 조건절을 푸시하는 것을 보여줍니다:

1. 단순 조건절 (`CUSTOMER_ID IN (1,2,3)` 같은)은 파티셔닝에 영향을 주므로 윈도우 함수를 지나서 푸시될 수 있습니다
2. 범위 조건절 (`PAYMENT_DATE BETWEEN...` 같은)은 상한을 윈도우 함수 계산에 푸시함으로써 부분적으로 최적화할 수 있습니다

## 벤치마크 결과

여러 데이터베이스에서 벤치마크를 수행한 결과:

테스트한 데이터베이스들에서 최적화되지 않은 쿼리의 성능 패널티가 크게 나타났습니다:

- DB2 LUW 10.5: 수동 최적화로 날짜 조건절을 서브쿼리에 푸시하여 3배 성능 향상
- MySQL 8.0.2: 치명적인 성능 차이—수동으로 최적화된 쿼리가 윈도우 함수를 사용하는 최적화되지 않은 버전보다 약 400배 빠르게 실행
- Oracle 12.2.0.1: 최적화되지 않은 윈도우 함수 쿼리에서 4배 성능 패널티 발생
- PostgreSQL 10: 적절한 조건절 푸시다운 없이 7배 성능 저하 발생
- SQL Server 2014: 최적화된 접근 방식과 최적화되지 않은 접근 방식 사이에 약 29배 성능 차이 발생

## 해결책: 인라인 테이블 반환 함수

인라인 테이블 반환 함수(TVF)를 지원하는 데이터베이스(DB2, SQL Server, PostgreSQL)의 경우, 윈도우 함수 로직을 매개변수화된 함수로 감싸면 더 나은 최적화가 가능합니다:

```sql
CREATE FUNCTION f_payment_with_revenue (
  @customer_id BIGINT,
  @from_date DATE,
  @to_date DATE
)
RETURNS TABLE AS RETURN
SELECT * FROM (
  SELECT
    customer_id,
    payment_date,
    amount,
    SUM(amount) OVER (
      PARTITION BY customer_id
      ORDER BY payment_date, payment_id
    ) cumulative_amount
  FROM payment
  WHERE customer_id = @customer_id
  AND payment_date <= @to_date
) t
WHERE payment_date >= @from_date;
```

`CROSS APPLY` 또는 `LATERAL JOIN` 구문과 함께 인라인 테이블 반환 함수를 사용하면 자동 최적화가 가능합니다.

결과는 특히 DB2와 SQL Server에서 크게 개선되었습니다.

## 제한 사항

- PostgreSQL: SQL 함수가 자동으로 인라인되지 않아 성능이 저하됩니다(최적화된 것보다 25배 느림). PostgreSQL의 인라인되지 않는 함수는 성능이 좋지 않았습니다.
- Oracle: SQL 기반 TVF를 지원하지 않아 PL/SQL 대안이 필요합니다.
- MySQL: TVF 지원이 없습니다.

## 결론

윈도우 함수는 강력하지만 '울타리'처럼 작동하여 조건절 푸시다운을 방해합니다. 대부분의 데이터베이스가 단순 조건절을 윈도우 함수를 지나서 푸시할 수 있지만, 범위 조건절을 효과적으로 자동 최적화하는 데이터베이스는 없습니다.

윈도우 함수는 신중한 최적화가 필요합니다. 수동 쿼리 재작성이나 인라인 TVF가 프로덕션 성능을 위해 여전히 필요합니다. 개발자는 뷰를 통해 윈도우 함수 로직을 여러 쿼리에서 재사용할 때 수동으로 쿼리를 최적화하거나 상당한 성능 비용을 감수해야 합니다.

특정 사용 사례에서 성능 비용이 수용 가능한지 판단하기 위해 신중한 측정이 필요합니다.
