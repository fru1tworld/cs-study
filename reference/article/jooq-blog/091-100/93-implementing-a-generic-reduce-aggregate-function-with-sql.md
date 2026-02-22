# SQL로 범용 REDUCE 집계 함수 구현하기

> 원문: https://blog.jooq.org/implementing-a-generic-reduce-aggregate-function-with-sql/

## 도입

이 글에서는 함수형 프로그래밍 패러다임에서 영감을 받아 SQL에서 범용 REDUCE 집계 함수를 구현하는 방법을 다룹니다. Neo4j의 Cypher는 Java의 Stream API와 유사한 임의의 리덕션을 지원합니다.

## Java Stream API 예제

```java
Stream.of(2, 4, 3, 1, 6, 5)
      .reduce((i, j) -> i * j)
      .ifPresent(System.out::println); // 720 출력
```

## PostgreSQL REDUCE 구현

주요 SQL 솔루션은 재귀 CTE를 사용한 상관 서브쿼리를 활용합니다:

```sql
with t(i) as (values (2), (4), (3), (1), (6), (5))
select
  (
    with recursive
      u(i, o) as (
         select i, o
         from unnest(array_agg(t.i)) with ordinality as u(i, o)
      ),
      r(i, o) as (
        select u.i, u.o from u where o = 1
        union all
        select r.i * u.i, u.o from u join r on u.o = r.o + 1
      )
    select i from r
    order by o desc
    limit 1
  )
from t;
```

## 집계 함수 개념

리덕션은 그룹에 대해 작동하는 범용 집계 함수입니다. 따라서 원하는 동작을 달성하기 위해 기존 SQL 집계 함수 메커니즘을 재사용해야 합니다.

## ARRAY_AGG() 기초 예제

UNNEST와 함께 기본 집계를 수행합니다:

```sql
with t(i) as (values (2), (4), (3), (1), (6), (5))
select
  (
    select string_agg(i::text, ', ')
    from unnest(array_agg(t.i)) as u(i)
  )
from t;
```

출력: `2, 4, 3, 1, 6, 5`

## 행 번호 생성 예제

```sql
with t(i) as (values (2), (4), (3), (1), (6), (5))
select
  (
    select string_agg(row(i, o)::text, ', ')
    from unnest(array_agg(t.i)) with ordinality as u(i, o)
  )
from t;
```

출력: `(2,1), (4,2), (3,3), (1,4), (6,5), (5,6)`

## 그룹화된 리덕션 예제

짝수/홀수로 그룹화하는 것을 보여줍니다:

```sql
with t(i) as (values (2), (4), (3), (1), (6), (5))
select
  i % 2,
  (
    select string_agg(row(i, o)::text, ', ')
    from unnest(array_agg(t.i)) with ordinality as u(i, o)
  )
from t
group by i % 2;
```

결과는 짝수(0) 그룹과 홀수(1) 그룹에 대해 재인덱싱된 순서와 함께 별도의 계산을 보여줍니다.

## 여러 집계를 포함한 완전한 GROUP BY

```sql
with t(i) as (values (2), (4), (3), (1), (6), (5))
select
  i % 2,
  (
    with recursive
      u(i, o) as (
         select i, o
         from unnest(array_agg(t.i)) with ordinality as u(i, o)
      ),
      r(i, o) as (
        select u.i, u.o from u where o = 1
        union all
        select r.i * u.i, u.o from u join r on u.o = r.o + 1
      )
    select i from r
    order by o desc
    limit 1
  ),
  string_agg(i::text, ' * ')
from t
group by i % 2
```

결과 테이블은 짝수(48: 2×4×6)와 홀수(15: 3×1×5) 그룹에 대해 별도의 곱을 보여줍니다.

## 재귀 메커니즘

재귀 곱셈 반복 과정을 시각화하면, `r.i`가 각 단계에서 연속적인 `u.i` 값과 곱해지며 누적되어 최종 결과(720)에 도달하는 것을 볼 수 있습니다.

## jOOQ 구현 미리보기

```java
ctx.select(T.I.mod(inline(2)), reduce(T.I, (i1, i2) -> i1.times(i2)))
   .from(T.I)
   .groupBy(T.I.mod(inline(2)))
   .fetch();
```

## 핵심 기술 포인트

PostgreSQL은 종종 올바른 일을 합니다. 대부분의 다른 RDBMS에서는 SQL 구문을 이렇게 우아하게 다루기가 더 어려울 것이므로, 이 접근 방식은 이식성이 좋지 않습니다.

이 솔루션은 `WITH ORDINALITY`를 활용하여 행 번호를 생성하고, 재귀 CTE를 사용하여 집계된 데이터를 반복하면서 각 반복의 결과를 리덕션 연산의 출력으로 대체합니다.

## 구현의 세 가지 핵심 구성 요소

### 1. 배열 집계
이 접근 방식은 `ARRAY_AGG()`를 사용하여 결과를 변환하는 `SUM()`과 같은 다른 집계 함수와 달리 데이터 구조를 유지하면서 값을 수집합니다.

### 2. 행 번호 매기기
구현은 다음 중 하나를 사용하여 순서 값을 생성합니다:
- `ROW_NUMBER()` 윈도우 함수
- `UNNEST()` 연산에 대한 PostgreSQL의 `WITH ORDINALITY` 절

### 3. 재귀 처리
재귀 CTE(공통 테이블 표현식)는 순서 정렬을 사용하여 집계된 배열을 반복합니다. 리덕션 연산(이 예제에서는 곱셈)은 누적된 결과와 각 연속 값을 결합합니다.

## 제한 사항

PostgreSQL은 동일한 쿼리 수준에서 `FROM` 절에 직접 집계 함수를 허용하지 않으므로, 표시된 상관 서브쿼리 패턴이 필요합니다.
