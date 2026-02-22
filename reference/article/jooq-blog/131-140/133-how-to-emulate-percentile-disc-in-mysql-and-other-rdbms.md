# MySQL 및 기타 RDBMS에서 PERCENTILE_DISC를 에뮬레이션하는 방법

> 원문: https://blog.jooq.org/how-to-emulate-percentile_disc-in-mysql-and-other-rdbms/

저자: lukaseder
게시일: 2019년 1월 28일

## 소개

백분위수 함수(역분포 함수)는 모든 SQL 데이터베이스에서 보편적으로 사용할 수 있는 것은 아닙니다. jOOQ 3.11 기준으로, 지원 현황은 다음과 같이 다양합니다:

| 데이터베이스 | 집계 함수 | 윈도우 함수 |
|-------------|----------|------------|
| MariaDB 10.3.3 | 아니오 | 예 |
| Oracle 18c | 예 | 예 |
| PostgreSQL 11 | 예 | 아니오 |
| SQL Server 2017 | 아니오 | 예 |
| Teradata 16 | 예 | 아니오 |

Oracle이 정렬 집합 집계(ordered set aggregate)와 윈도우 함수 버전 모두를 제공하여 가장 포괄적인 구현을 갖추고 있습니다.

## 윈도우 함수를 사용한 우회 방법

RDBMS가 윈도우 함수를 지원하기만 하면, `PERCENT_RANK`와 `FIRST_VALUE`를 사용하여 `PERCENTILE_DISC`를 쉽게 에뮬레이션할 수 있습니다.

핵심 접근 방식은 다음과 같습니다:

1. 정렬을 위해 `PERCENT_RANK` 값(0에서 1 사이)을 계산합니다
2. 순위가 원하는 백분위수 이하인 행을 필터링합니다
3. `FIRST_VALUE`를 사용하여 결과를 가져옵니다

중앙값(0.5 백분위수)을 찾을 때, 우리는 실제로 `PERCENT_RANK`가 0.5 이하인 값을 찾는 것입니다.

### 중앙값(0.5 백분위수) 예제 쿼리

```sql
SELECT DISTINCT
  rating,
  first_value(length) OVER (
    ORDER BY CASE WHEN p1 <= 0.5 THEN p1 END DESC NULLS LAST) x1
FROM (
  SELECT
    rating,
    length,
    percent_rank() OVER (ORDER BY length) p1
  FROM film
) t
```

이 솔루션은 다음 패턴을 사용합니다: `PERCENT_RANK` 결과를 `CASE` 표현식으로 필터링하고, 내림차순으로 정렬하며, `NULLS LAST`를 적용한 다음, 첫 번째 일치하는 값을 추출합니다.

## 집계 함수 에뮬레이션

역분포 함수를 지원하지 않는 데이터베이스(예: MySQL)의 경우, 윈도우 함수 접근 방식을 `MAX()`와 같은 집계 함수로 감싸서 행을 다시 집계 동작으로 축소할 수 있습니다:

```sql
SELECT MAX(x1) x1
FROM (
  SELECT first_value(length) OVER (
    ORDER BY CASE WHEN p1 <= 0.5 THEN p1 END DESC NULLS LAST) x1
  FROM (
    SELECT
      length,
      percent_rank() OVER (ORDER BY length) p1
    FROM film
  ) t
) t
```

그룹별로 집계하려면 필요에 따라 `GROUP BY`를 적용할 수 있습니다.

## 핵심 통찰

이 기사는 윈도우 함수가 매우 강력하다는 것을 보여주며, 기존 윈도우 연산의 창의적인 조합을 통해 누락된 분석 함수를 에뮬레이션할 수 있음을 입증합니다.

이 솔루션은 다양한 네이티브 함수 지원에도 불구하고 여러 RDBMS 플랫폼에서 일관된 백분위수 계산을 가능하게 하는 이식 가능한 SQL 패턴을 보여줍니다.
