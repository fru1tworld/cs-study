# 다른 SQL 쿼리가 결과를 반환하지 않을 때만 SQL 쿼리를 실행하는 방법

> 원문: https://blog.jooq.org/how-to-execute-a-sql-query-only-if-another-sql-query-has-no-results/

## 개요

이 글에서는 일반적인 데이터베이스 문제를 해결하는 방법을 탐구합니다: 첫 번째 쿼리가 결과를 반환하지 않을 때만 두 번째 SQL 쿼리를 실행하는 것, 이상적으로는 단일 쿼리 문장 내에서 처리하는 방법입니다.

## 정석 해결책: 공통 테이블 표현식(CTE, Common Table Expression)

저자는 `WITH` 절을 사용하여 기본 쿼리를 감싼 다음, `UNION ALL`로 결과를 결합하는 방법을 보여줍니다:

```sql
WITH r AS (
  SELECT * FROM film WHERE length = 120
)
SELECT * FROM r
UNION ALL
SELECT * FROM film
WHERE length = 130
AND NOT EXISTS (
  SELECT * FROM r
)
```

이 접근 방식은 120분 길이의 영화를 조회하고, 해당 결과가 없으면 130분 영화를 조회합니다.

## 데이터베이스별 성능 분석

PostgreSQL: 실행 계획을 보면 첫 번째 쿼리가 성공할 때 데이터베이스가 두 번째 쿼리 실행을 피하는 것을 알 수 있습니다. 벤치마크 결과 첫 번째 쿼리만 실행하는 것과 비교하여 약 5-10%의 오버헤드가 발생하는데, 이는 CTE가 "최적화 장벽(optimisation fence)"으로 작용하기 때문입니다.

Oracle: PostgreSQL과 유사한 동작을 보입니다—불필요한 경우 두 번째 서브쿼리가 실행되지 않습니다. 여러 테스트 실행에서 단일 쿼리와 이중 쿼리 간의 성능 차이는 무시할 수 있는 수준이었습니다.

SQL Server 2014: 불필요한 두 번째 쿼리를 건너뛰는 최적화가 부족한 점이 눈에 띕니다. 벤치마크 결과 CTE 솔루션을 사용할 때 두 개의 별도 쿼리를 실행하는 것보다 "30% – 40%의 오버헤드"가 발생했습니다.

## 대안적 접근 방식

Chris Saxon은 윈도우 함수(Window Function)를 사용하는 방법을 제안했습니다:

```sql
WITH r AS (
  SELECT title, length, MIN(length) OVER() AS minlen
  FROM film
  WHERE length IN (120, 130)
)
SELECT title FROM r WHERE length = minlen;
```

Spaghettidba가 제안한 `MIN(length) OVER()`를 사용하는 더 단순한 변형은 SQL Server에서 두 개의 독립적인 쿼리를 실행하는 것과 경쟁력 있는 성능을 보였습니다.

## 핵심 요점

저자는 테스트한 세 가지 데이터베이스 모두에서 "카디널리티 추정(cardinality estimates)이 부정확했다"고 강조하며, 이는 더 복잡한 시나리오에서 쿼리 최적화에 영향을 미칠 수 있습니다. 근본적인 결론은 다음과 같습니다: "측정하라! 측정하라! 측정하라!" 실제 성능은 데이터베이스 버전, 데이터 양, 인덱싱 전략, 하드웨어 조건에 따라 달라지므로—어떤 접근 방식을 선택하기 전에 프로덕션과 유사한 데이터셋으로 벤치마킹하는 것이 필수적입니다.
