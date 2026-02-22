# 데이터 정규화를 위한 재귀 SQL

> 원문: https://blog.jooq.org/recursive-sql-for-data-normalisation/

게시일: 2013년 10월 24일
작성자: lukaseder

## 문제 상황

이 글에서는 집계된 데이터를 정규화하기 위해 재귀 SQL을 사용하는 방법을 다룹니다. 여기서 다루는 시나리오는 날짜-이벤트 데이터에서 집계된 건수를 개별 레코드로 "역집계"해야 하는 경우입니다.

초기 데이터:
| 날짜 | 건수 |
|------|-------|
| 2013년 10월 01일 | 2 |
| 2013년 10월 02일 | 1 |
| 2013년 10월 03일 | 3 |
| 2013년 10월 04일 | 4 |
| 2013년 10월 05일 | 2 |
| 2013년 10월 06일 | 0 |
| 2013년 10월 07일 | 2 |

원하는 출력: 각 날짜에 대해 1부터 COUNT까지 번호가 매겨진 개별 레코드 (이벤트가 0인 날짜는 제외).

## SQL 솔루션

이 글에서는 재귀 공통 테이블 표현식(CTE) 접근 방식을 제시합니다:

```sql
with recursive

-- 원본 데이터 정의
data(date, count) as (
  select date '2013-10-01', 2 union all
  select date '2013-10-02', 1 union all
  select date '2013-10-03', 3 union all
  select date '2013-10-04', 4 union all
  select date '2013-10-05', 2 union all
  select date '2013-10-06', 0 union all
  select date '2013-10-07', 2
),

-- 재귀 CTE: count가 0보다 큰 데이터로 시작하여 count가 1이 될 때까지 1씩 감소시킴
recurse(date, count) as (
  select date, count
  from data
  where count > 0
  union all
  select date, count - 1
  from recurse
  where count > 1
)
select date, count event_number from recurse
order by date asc, event_number asc;
```

## 작동 원리

재귀 CTE는 `count > 0`인 모든 레코드로 시작한 다음, 1에 도달할 때까지 각 count에서 1을 재귀적으로 뺍니다. 이렇게 하면 원하는 비정규화된 출력이 생성됩니다.

## PostgreSQL 대안 접근법

저자는 `generate_series`를 사용하는 우아한 PostgreSQL 전용 솔루션을 언급합니다:

```sql
with recursive
-- 원본 데이터 정의
data(date, count) as (
  select date '2013-10-01', 2 union all
  select date '2013-10-02', 1 union all
  select date '2013-10-03', 3 union all
  select date '2013-10-04', 4 union all
  select date '2013-10-05', 2 union all
  select date '2013-10-06', 0 union all
  select date '2013-10-07', 2
)
-- generate_series를 사용하여 1부터 count까지의 시퀀스를 직접 생성
select date, generate_series(1, count) event_number
from data
where count > 0
order by date asc, event_number asc;
```

## 참고 사항

이 글에서는 Oracle의 CONNECT BY 절이 이 특정 사용 사례에는 적합하지 않았다고 언급합니다.
