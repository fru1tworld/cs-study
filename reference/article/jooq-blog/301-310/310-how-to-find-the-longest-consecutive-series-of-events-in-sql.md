# SQL에서 가장 긴 연속 이벤트 시리즈를 찾는 방법

> 원문: https://blog.jooq.org/how-to-find-the-longest-consecutive-series-of-events-in-sql/

## 소개

이 글에서는 SQL을 사용하여 시계열 데이터에서 연속적인 이벤트 시리즈를 찾는 방법을 다룹니다. 예시로 Stack Overflow의 배지 시스템을 사용하는데, 사용자들은 연속으로 매일 방문하면 "Enthusiast"와 "Fanatic" 배지를 획득할 수 있습니다.

## 문제 정의

사용자는 중간에 빠지는 날 없이 연속으로 로그인해야 합니다. 하루라도 놓치면 카운터가 초기화됩니다. 이러한 연속 기록(streak)을 SQL에서 프로그래밍 방식으로 식별하는 것이 과제입니다.

## 해결 접근법

Stack Exchange Data Explorer(SQL Server)를 사용하여 사용자가 게시물을 작성한 연속 일수를 쿼리하는 방법을 보여드리겠습니다:

```sql
SELECT DISTINCT CAST(CreationDate AS DATE) AS date
FROM Posts
WHERE OwnerUserId = ##UserId##
ORDER BY 1
```

이 쿼리는 2010-11-26, 2010-11-27과 같은 날짜를 반환하고, 그 다음 2010-11-29로 건너뛰는 것처럼 데이터에 자연스러운 공백이 있음을 보여줍니다.

## 핵심 기법: Tabibitosan 방법

이 솔루션은 `ROW_NUMBER()` 윈도우 함수를 사용합니다. 핵심 통찰은 다음과 같습니다: 행 번호(공백이 없음)를 날짜(공백이 있을 수 있음)에서 빼면 연속된 시퀀스에 대해 일관된 그룹 식별자가 생성됩니다.

```sql
WITH dates(date) AS (
  SELECT DISTINCT CAST(CreationDate AS DATE)
  FROM Posts
  WHERE OwnerUserId = ##UserId##
),
groups AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY date) AS rn,
    -- 날짜에서 행 번호를 빼서 그룹 식별자 생성
    dateadd(day, -ROW_NUMBER() OVER (ORDER BY date), date) AS grp,
    date
  FROM dates
)
SELECT
  COUNT(*) AS consecutiveDates,
  MIN(date) AS minDate,
  MAX(date) AS maxDate
FROM groups
GROUP BY grp
ORDER BY 1 DESC, 2 DESC
```

## 수학적 원리

연속된 날짜의 경우: `date2 - date1 = 1`이고 `rn2 - rn1 = 1`이므로, `date - rn`은 상수로 유지됩니다. 연속되지 않은 날짜의 경우, 이 값이 변경되어 자동으로 새로운 그룹이 생성됩니다.

예를 들어 설명하면:
- 날짜가 1, 2, 3일이고 행 번호가 1, 2, 3이면 → 빼면 모두 0 (같은 그룹)
- 날짜가 1, 2, 5일이고 행 번호가 1, 2, 3이면 → 빼면 0, 0, 2 (두 개의 그룹)

## 확장 예제

연속 주(Week) 쿼리:

주 단위로 연속성을 확인하고 싶다면, 날짜 대신 주 식별자를 사용할 수 있습니다:

```sql
WITH weeks(week) AS (
  SELECT DISTINCT datepart(year, CreationDate) * 100
                + datepart(week, CreationDate)
  FROM Posts
  WHERE OwnerUserId = ##UserId##
),
groups AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY week) AS rn,
    -- 주에서 행 번호를 빼서 그룹 식별자 생성
    week - ROW_NUMBER() OVER (ORDER BY week) AS grp,
    week
  FROM weeks
)
SELECT
  COUNT(*) AS consecutiveWeeks,
  MIN(week) AS minWeek,
  MAX(week) AS maxWeek
FROM groups
GROUP BY grp
ORDER BY 1 DESC, 2 DESC
```

DENSE_RANK()를 사용한 간소화:

`DENSE_RANK()`를 사용하면 DISTINCT와 그룹화를 하나의 표현식으로 결합할 수 있어 쿼리를 더 간결하게 만들 수 있습니다:

```sql
WITH groups(date, grp) AS (
  SELECT DISTINCT
    CAST(CreationDate AS DATE),
    -- DENSE_RANK()를 사용하여 중복 제거와 그룹 식별자 생성을 동시에 수행
    dateadd(day,
      -DENSE_RANK() OVER (ORDER BY CAST(CreationDate AS DATE)),
      CAST(CreationDate AS DATE)) AS grp
  FROM Posts
  WHERE OwnerUserId = ##UserId##
)
SELECT
  COUNT(*) AS consecutiveDates,
  MIN(date) AS minDate,
  MAX(date) AS maxDate
FROM groups
GROUP BY grp
ORDER BY 1 DESC, 2 DESC
```

## 다른 데이터베이스에서의 적용

이 기법은 적절한 날짜 함수 구문 조정을 통해 다양한 SQL 데이터베이스에서 작동합니다. 예를 들어:

- SQL Server: `dateadd(day, -ROW_NUMBER() OVER (...), date)` 사용
- PostgreSQL: `date - interval '1 day' * ROW_NUMBER() OVER (...)` 또는 단순히 `date - ROW_NUMBER() OVER (...)` 사용 (날짜에서 정수를 직접 뺄 수 있음)
- Oracle: `date - ROW_NUMBER() OVER (...)` 사용
- MySQL 8.0+: `date_sub(date, INTERVAL ROW_NUMBER() OVER (...) DAY)` 사용

## 결론

이 방법의 핵심 장점은 복잡한 재귀 쿼리나 셀프 조인을 피하고, 대신 윈도우 함수를 활용하여 효율적인 성능을 얻을 수 있다는 것입니다. Tabibitosan 방법은 연속된 시퀀스를 찾는 SQL 문제에서 매우 유용한 패턴으로, 출석 기록, 연속 로그인, 주가 연속 상승일 등 다양한 비즈니스 시나리오에 적용할 수 있습니다.
