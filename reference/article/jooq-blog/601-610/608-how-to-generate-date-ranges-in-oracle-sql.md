# Oracle SQL에서 날짜 범위를 생성하는 방법

> 원문: https://blog.jooq.org/how-to-generate-date-ranges-in-oracle-sql/

저자: lukaseder

게시일: 2013년 7월 24일

## 요약

이 글에서는 Oracle SQL에서 `CONNECT BY` 절을 사용하여 연속적인 12개월 날짜 범위를 생성하는 방법을 설명합니다.

## 핵심 요구사항

이 솔루션은 다음과 같은 날짜 범위를 생성합니다:
- 각 범위는 정확히 12개월을 포함함
- 첫 번째 범위는 지정된 입력 날짜에서 시작
- 마지막 범위는 현재 날짜를 포함
- 후속 범위들은 동일한 월간 간격 패턴을 따름

## 예시 출력

2010-06-10을 입력으로 사용한 경우:

```
START_DATE   END_DATE
2010-06-10   2011-06-10
2011-06-10   2012-06-10
2012-06-10   2013-06-10
2013-06-10   2014-06-10 (오늘 날짜 포함: 2013-07-24)
```

## SQL 솔루션

이 글에서는 다음과 같은 Oracle SQL 쿼리를 제공합니다:

```sql
SELECT
  add_months(input, (level - 1) * 12) start_date,
  add_months(input, level * 12) end_date
FROM (
  SELECT DATE '2010-06-10' input
  FROM DUAL
)
CONNECT BY
  add_months(input, (level - 1) * 12) < sysdate
```

핵심 메커니즘: `CONNECT BY` 절은 `level`이 반복 횟수를 결정하는 계층적 행을 생성하고, `ADD_MONTHS()` 함수가 날짜 간격을 계산합니다.

저자는 이 솔루션의 실제 작동을 보여주는 SQL Fiddle 예제를 참조하고 있습니다.
