# 역분포 함수를 사용하여 MEDIAN() 집계 함수를 에뮬레이션하는 방법

> 원문: https://blog.jooq.org/how-to-emulate-the-median-aggregate-function-using-inverse-distribution-functions/

게시일: 2015년 1월 6일
작성자: Lukas Eder

---

`MEDIAN()`은 `MEAN()`이나 `AVG()`와 많이 다릅니다. `AVG()`는 단순히 `SUM(exp) / COUNT(exp)`와 같습니다. 반면 `MEDIAN()`은 표본의 모든 값 중 50%가 `MEDIAN()`보다 높고, 나머지 50%가 `MEDIAN()`보다 낮다는 것을 알려줍니다.

다음의 예를 살펴보겠습니다:

```sql
WITH t(value) AS (
  SELECT 1   FROM DUAL UNION ALL
  SELECT 2   FROM DUAL UNION ALL
  SELECT 3   FROM DUAL
)
SELECT
  avg(value),
  median(value)
FROM
  t;
```

위 쿼리는 다음과 같은 결과를 반환합니다:

```
AVG(VALUE)  MEDIAN(VALUE)
---------- -------------
         2             2
```

지금까지는 값들이 균형 잡혀 있기 때문에 동일합니다. 하지만 값이 치우쳐 있으면 어떨까요?

```sql
WITH t(value) AS (
  SELECT 1   FROM DUAL UNION ALL
  SELECT 2   FROM DUAL UNION ALL
  SELECT 100 FROM DUAL
)
SELECT
  avg(value),
  median(value)
FROM
  t;
```

위 쿼리는 이제 다음과 같은 결과를 반환합니다:

```
AVG(VALUE)  MEDIAN(VALUE)
---------- -------------
34.3333333             2
```

이것이 흥미로운 부분입니다. `MEDIAN()`은 위의 예에서 여전히 `2`를 반환합니다. 왜냐하면 `2`는 정확히 전체 분포의 중간에 있기 때문입니다. 하지만 `AVG()`는 아주 약간의 상승으로도 빠르게 치우칠 수 있습니다(우리 샘플에서 값 `100`처럼).

## 왜 이것이 중요한가요?

이것이 통계에서 중요한 이유는 무엇일까요? 음, [미국의 평균 소득 vs 중위 소득에 관한 훌륭한 분석](https://www.advisorperspectives.com/dshort/updates/2014/09/16/median-household-income-growth-deflating-the-american-dream)을 생각해 보세요. 이 분석에 따르면, 미국의 평균 소득은 상승했지만 중위 소득은 지난 10년간 하락했습니다. 왜 그럴까요? 부의 집중 때문입니다. 일부 사람들(예: 월스트리트 종사자들)이 여전히 더 많은 돈을 벌고 있어서, 상위 0.1%의 인구가 많은 부를 축적하고 있습니다. 그 결과 평균 소득이 상승합니다. 하지만 "보통" 사람들의 실제 부는 감소했습니다.

다시 말해, 분포가 치우치면 `AVG()`는 유용하지 않고 `MEDIAN()`이 더 의미 있는 값을 제공합니다.

## 백분위수가 무엇인가요?

`MEDIAN()`은 본질적으로 표본을 두 개의 동일한 크기 그룹으로 나눕니다. 이렇게 보면, `MEDIAN(exp)`는 50번째 백분위수, 즉 0.5 백분위수입니다. 더 일반적인 용어로:

- `MIN(exp)`: 0-백분위수
- `MEDIAN(exp)`: 50-백분위수(또는 0.5-백분위수)
- `MAX(exp)`: 100-백분위수(또는 1.0-백분위수)

## SQL의 MEDIAN()

안타깝게도 `MEDIAN()`은 SQL 표준에 포함되어 있지 않으며 다음 데이터베이스에서만 지원됩니다:

- CUBRID
- HSQLDB
- Oracle
- Sybase SQL Anywhere

## 역분포 함수를 사용한 대안

하지만 해결책이 있습니다! SQL:2003에서 새롭게 도입된 "역분포 함수"라고도 불리는 "순서 집합 집계 함수"가 있습니다. 순서 집합 집계 함수는 `ORDER BY` 절을 지정하는 추가적인 `WITHIN GROUP` 절을 받습니다. 이 순서 집합 집계 함수의 예로는 PostgreSQL 9.4+의 `percentile_cont()`가 있습니다:

```sql
WITH t(value) AS (
  SELECT 1   UNION ALL
  SELECT 2   UNION ALL
  SELECT 100
)
SELECT
  avg(value),
  percentile_cont(0.5) WITHIN GROUP (ORDER BY value)
FROM
  t;
```

이것이 바로 우리가 찾던 것입니다. `WITHIN GROUP (ORDER BY ...)` 구문을 사용하여 분포 내에서 특정 백분위수(예: 50번째 백분위수 또는 `0.5`)가 정확히 어디에 있는지 찾을 수 있습니다.

`WITHIN GROUP` 구문을 인식하셨을 수도 있습니다. Oracle에서 사용하는 완전히 쓸모없는 `LISTAGG()` 함수에서도 사용되기 때문입니다(Oracle에서 `VARCHAR2`의 최대 길이가 4000이기 때문에 `VARCHAR2`를 반환하므로 완전히 쓸모없습니다):

```sql
WITH t(value) AS (
  SELECT 1   FROM DUAL UNION ALL
  SELECT 2   FROM DUAL UNION ALL
  SELECT 100 FROM DUAL
)
SELECT
  listagg(value, ', ') WITHIN GROUP (ORDER BY value)
FROM
  t;
```

위 쿼리는 다음을 반환합니다:

```
1, 2, 100
```

어쨌든. `PERCENTILE_CONT()` / `PERCENTILE_DISC()` 및 `MEDIAN()` 함수에 대한 추가적인 데이터베이스 지원은 [jOOQ 매뉴얼에서 확인](https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/aggregate-functions/percentile-cont-function/)할 수 있습니다.

## jOOQ에서의 사용

jOOQ는 `DSL.median()` 또는 `DSL.percentileCont()`를 사용하여 기본 에뮬레이션을 제공합니다:

```java
DSL.using(configuration)
   .select(
       median(T.VALUE),
       percentileCont(0.5).withinGroupOrderBy(T.VALUE)
   )
   .from(T)
   .fetch();
```

jOOQ는 사용 중인 데이터베이스에 적합한 SQL을 자동으로 생성하여, 다양한 플랫폼에서 이러한 함수들을 쉽게 사용할 수 있도록 합니다.
