# PostgreSQL이 아닌 데이터베이스에서 DISTINCT ON 사용하기

> 원문: https://blog.jooq.org/using-distinct-on-in-non-postgresql-databases/

저자: lukaseder

날짜: 2019년 9월 9일 (2022년 5월 26일 업데이트)

## 전체 글 내용

PostgreSQL은 `DISTINCT ON`이라는 강력하지만 흔하지 않은 SQL 기능을 제공합니다. 이 글에서는 이 절(clause)에 대해 설명하고, 이 기능을 기본적으로 지원하지 않는 데이터베이스에서 어떻게 동일한 기능을 구현할 수 있는지 알아봅니다.

### 핵심 개념

PostgreSQL 문서에 따르면: "SELECT DISTINCT ON은 주어진 표현식이 동일하게 평가되는 각 행 집합에서 첫 번째 행만 유지합니다."

PostgreSQL 문서의 예제:
```sql
SELECT DISTINCT ON (location) location, time, report
    FROM weather_reports
    ORDER BY location, time DESC;
```

이 쿼리는 각 위치(location)별로 가장 최근의 날씨 보고서를 조회합니다.

### 표준 SQL 대안

PostgreSQL의 구문 우선 접근 방식 대신, 저자는 윈도우 함수를 사용하는 더 논리적인 순서를 제안합니다:

```sql
SELECT DISTINCT
  location,
  FIRST_VALUE (time) OVER w AS time,
  FIRST_VALUE (report) OVER w AS report
FROM weather_reports
WINDOW w AS (PARTITION BY location ORDER BY time DESC)
ORDER BY location
```

WINDOW 절을 지원하지 않는 데이터베이스(예: Oracle)의 경우, 다음과 같이 확장합니다:

```sql
SELECT DISTINCT
  location,
  FIRST_VALUE (time) OVER (PARTITION BY location ORDER BY time DESC),
  FIRST_VALUE (report) OVER (PARTITION BY location ORDER BY time DESC)
FROM weather_reports
ORDER BY location
```

### 샘플 데이터와 결과

테스트 테이블:
```sql
create table weather_reports (location text, time date, report text);
insert into weather_reports values ('X', DATE '2000-01-01', 'X1');
insert into weather_reports values ('X', DATE '2000-01-02', 'X2');
insert into weather_reports values ('X', DATE '2000-01-03', 'X3');
insert into weather_reports values ('Y', DATE '2000-01-03', 'Y1');
insert into weather_reports values ('Y', DATE '2000-01-05', 'Y2');
insert into weather_reports values ('Z', DATE '2000-01-04', 'Z1');
```

예상 출력:

| location | time       | report |
|----------|------------|--------|
| X        | 2000-01-03 | X3     |
| Y        | 2000-01-05 | Y2     |
| Z        | 2000-01-04 | Z1     |

### 핵심 요점

jOOQ는 이미 PostgreSQL의 `DISTINCT ON` 구문을 지원하며, 윈도우 함수를 사용하여 다른 데이터베이스에서도 이 기능을 에뮬레이션할 수 있도록 향후 지원을 계획하고 있습니다.
