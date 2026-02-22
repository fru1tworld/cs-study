# SQL WINDOW 절로 구문 오버헤드를 줄이는 방법

> 원문: https://blog.jooq.org/how-to-reduce-syntactic-overhead-using-the-sql-window-clause/

게시일: 2018년 9월 20일 | 2018년 9월 26일

작성자: lukaseder

---

SQL은 장황한 언어이며, 가장 장황한 기능 중 하나가 바로 윈도우 함수입니다. 최근 Stack Overflow 질문에서 누군가가 주어진 날짜별로 시계열 데이터의 첫 번째 값과 마지막 값의 차이를 계산하는 방법을 물었습니다.

## 입력 데이터

```
volume  tstamp
---------------------------
29011   2012-12-28 09:00:00
28701   2012-12-28 10:00:00
28830   2012-12-28 11:00:00
28353   2012-12-28 12:00:00
28642   2012-12-28 13:00:00
28583   2012-12-28 14:00:00
28800   2012-12-29 09:00:00
28751   2012-12-29 10:00:00
28670   2012-12-29 11:00:00
28621   2012-12-29 12:00:00
28599   2012-12-29 13:00:00
28278   2012-12-29 14:00:00
```

## 원하는 결과

```
first  last   difference  date
------------------------------------
29011  28583  428         2012-12-28
28800  28278  522         2012-12-29
```

## 쿼리를 어떻게 작성할까요?

값과 타임스탬프의 진행이 서로 상관관계가 없으므로, `Timestamp2 > Timestamp1`이면 `Value2 < Value1`이라는 규칙이 없습니다. PostgreSQL 구문을 사용한 간단한 쿼리는 다음과 같습니다:

```sql
SELECT
  max(volume)               AS first,
  min(volume)               AS last,
  max(volume) - min(volume) AS difference,
  CAST(tstamp AS DATE)      AS date
FROM t
GROUP BY CAST(tstamp AS DATE);
```

그러나 이 접근 방식은 max/min이 시간 순서상 첫 번째/마지막 값을 보장하지 않기 때문에 문제를 해결하지 못합니다.

## 대안적 접근 방법

윈도우 함수 없이 첫 번째와 마지막 값을 찾는 여러 방법이 있습니다:

- Oracle: `some_aggregate_function(...) KEEP (DENSE_RANK FIRST ORDER BY ...)` 구문과 함께 `FIRST`와 `LAST` 함수를 사용합니다.
- PostgreSQL: `ORDER BY`와 `LIMIT`과 함께 `DISTINCT ON` 구문을 사용합니다.

가장 좋은 접근 방식은 `FIRST_VALUE`와 `LAST_VALUE` 윈도우 함수를 사용하는 것입니다:

```sql
SELECT DISTINCT
  first_value(volume) OVER (
    PARTITION BY CAST(tstamp AS DATE)
    ORDER BY tstamp
    ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
  ) AS first,
  last_value(volume) OVER (
    PARTITION BY CAST(tstamp AS DATE)
    ORDER BY tstamp
    ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
  ) AS last,
  first_value(volume) OVER (
    PARTITION BY CAST(tstamp AS DATE)
    ORDER BY tstamp
    ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
  )
  - last_value(volume) OVER (
    PARTITION BY CAST(tstamp AS DATE)
    ORDER BY tstamp
    ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
  ) AS diff,
  CAST(tstamp AS DATE) AS date
FROM t
ORDER BY CAST(tstamp AS DATE)
```

이 쿼리는 윈도우 명세가 반복되기 때문에 읽기 어렵습니다.

## WINDOW 절이 해결책입니다

최소 네 개의 데이터베이스가 SQL 표준 `WINDOW` 절을 구현했습니다:

- MySQL
- PostgreSQL
- SQLite (버전 3.25.0 이상)
- Sybase SQL Anywhere

WINDOW 절을 사용하면 쿼리를 다음과 같이 리팩터링할 수 있습니다:

```sql
SELECT DISTINCT
  first_value(volume) OVER w AS first,
  last_value(volume) OVER w AS last,
  first_value(volume) OVER w
    - last_value(volume) OVER w AS diff,
  CAST(tstamp AS DATE) AS date
FROM t
WINDOW w AS (
  PARTITION BY CAST(tstamp AS DATE)
  ORDER BY tstamp
  ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
)
ORDER BY CAST(tstamp AS DATE)
```

WINDOW 절은 공통 테이블 표현식(CTE)과 유사하게 윈도우 명세를 정의할 수 있게 해줍니다:

```
WINDOW
    <window-name> AS (<window-specification>)
{  ,<window-name> AS (<window-specification>)... }
```

## 부분 명세로부터 명세 구축하기

윈도우 명세는 이전에 정의된 명세를 참조할 수 있습니다:

```sql
SELECT DISTINCT
  first_value(volume) OVER w3 AS first,
  last_value(volume) OVER w3 AS last,
  first_value(volume) OVER w3
    - last_value(volume) OVER w3 AS diff,
  CAST(tstamp AS DATE) AS date
FROM t
WINDOW
  w1 AS (PARTITION BY CAST(tstamp AS DATE)),
  w2 AS (w1 ORDER BY tstamp),
  w3 AS (w2 ROWS BETWEEN UNBOUNDED PRECEDING
                     AND UNBOUNDED FOLLOWING)
ORDER BY CAST(tstamp AS DATE)
```

## 기존 윈도우 정의 수정하기

윈도우 정의는 참조될 때 수정될 수 있습니다:

```sql
SELECT DISTINCT
  first_value(volume) OVER (
    w2 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
  ) AS first,
  last_value(volume) OVER (
    w2 ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
  ) AS last,
  first_value(volume) OVER (
    w2 ROWS UNBOUNDED PRECEDING
  ) - last_value(volume) OVER (
    w2 ROWS BETWEEN 1 PRECEDING AND UNBOUNDED FOLLOWING
  ) AS diff,
  CAST(tstamp AS DATE) AS date
FROM t
WINDOW
  w1 AS (PARTITION BY CAST(tstamp AS DATE)),
  w2 AS (w1 ORDER BY tstamp)
ORDER BY CAST(tstamp AS DATE)
```

## 데이터베이스가 WINDOW 절을 지원하지 않는다면?

WINDOW 절을 지원하지 않는 데이터베이스의 경우, 각 함수에 윈도우 명세를 수동으로 작성하거나 jOOQ와 같은 SQL 빌더를 사용해야 합니다. jOOQ는 WINDOW 절을 에뮬레이션할 수 있습니다. 온라인 변환은 https://www.jooq.org/translate 에서 이용할 수 있습니다.

---

## 댓글 섹션

Dag Wanvik (2018년 9월 26일):
예제에서 BETWEEN 명세가 불완전하며 "AND CURRENT ROW"가 누락되었다고 지적했습니다.

Lukas Eder (2018년 9월 26일):
수정을 확인하고, 의도한 구문이 `w3 ROWS UNBOUNDED PRECEDING`임을 명확히 했습니다.

Lukasz (2018년 9월 26일):
SQLite 3.25.0(2018년 9월 18일 출시)도 WINDOW 절을 지원하여 총 네 개의 데이터베이스가 된다고 언급했습니다.

Lukas Eder (2018년 9월 26일):
SQLite 지원에 대한 수정을 인정했습니다.
