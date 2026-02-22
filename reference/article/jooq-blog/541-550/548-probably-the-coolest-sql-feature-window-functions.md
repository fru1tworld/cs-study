# 아마도 가장 멋진 SQL 기능: 윈도우 함수

> 원문: https://blog.jooq.org/probably-the-coolest-sql-feature-window-functions/

독특한 문법에 익숙해지면, SQL은 선언적 수준에서 놀라운 기능을 제공하는 매우 표현력 있고 풍부한 언어입니다. 가장 멋진 기능 중 하나가 바로 윈도우 함수인데, 그 멋진 정도에 비해 인기가 믿을 수 없을 만큼 낮습니다. 낮은 인기의 이유는 개발자들이 이 멋진 것들에 대해 모르기 때문일 수밖에 없습니다. 일단 윈도우 함수를 알게 되면, 모든 곳에 사용하고 싶은 위험에 빠지게 됩니다.

## 윈도우 함수란 무엇인가?

윈도우 함수는 데이터를 처리하는 동안 데이터의 "윈도우(창)"를 살펴봅니다.

SQL 문법:
```sql
SELECT
  LAG(first_name, 1)
    OVER(ORDER BY first_name) "prev",
  first_name,
  LEAD(first_name, 1)
    OVER(ORDER BY first_name) "next"
FROM people
ORDER BY first_name
```

jOOQ 문법:
```java
select(
  lag(PEOPLE.FIRST_NAME, 1)
    .over().orderBy(PEOPLE.FIRST_NAME).as("prev"),
  PEOPLE.FIRST_NAME,
  lead(PEOPLE.FIRST_NAME, 1)
    .over().orderBy(PEOPLE.FIRST_NAME).as("next"))
.from(PEOPLE)
.orderBy(PEOPLE.FIRST_NAME);
```

위 쿼리를 실행하면, 각 레코드의 FIRST_NAME 값이 이전 및 다음 이름을 참조할 수 있습니다.

## WINDOW 절 사용하기

SQL 문법:
```sql
SELECT
  LAG(first_name, 1) OVER w "prev",
  first_name,
  LEAD(first_name, 1) OVER w "next"
FROM people
WINDOW w AS (ORDER BY first_name)
ORDER BY first_name DESC
```

jOOQ 3.3 문법:
```java
WindowDefinition w = name("w").as(
  orderBy(PEOPLE.FIRST_NAME));

select(
  lag(PEOPLE.FIRST_NAME, 1).over(w).as("prev"),
  PEOPLE.FIRST_NAME,
  lead(PEOPLE.FIRST_NAME, 1).over(w).as("next"))
.from(PEOPLE)
.window(w)
.orderBy(PEOPLE.FIRST_NAME.desc());
```

## 프레임 정의 사용하기

윈도우는 경계가 있는(bounded) 또는 경계가 없는(unbounded) 프레임을 가질 수 있습니다.

SQL 문법:
```sql
SELECT
  FIRST_VALUE(first_name)
    OVER(ORDER BY first_name ASC
         ROWS 1 PRECEDING) "prev",
  first_name,
  FIRST_VALUE(first_name)
    OVER(ORDER BY first_name DESC
         ROWS 1 PRECEDING) "next"
FROM people
ORDER BY first_name ASC
```

jOOQ 문법:
```java
select(
  firstValue(PEOPLE.FIRST_NAME)
    .over().orderBy(PEOPLE.FIRST_NAME.asc())
           .rowsPreceding(1).as("prev"),
  PEOPLE.FIRST_NAME,
  firstValue(PEOPLE.FIRST_NAME)
    .over().orderBy(PEOPLE.FIRST_NAME.desc())
           .rowsPreceding(1).as("next"))
.from(PEOPLE)
.orderBy(FIRST_NAME.asc());
```

## PARTITION BY를 사용하여 여러 윈도우 만들기

종종 전체 데이터셋에 대한 단일 윈도우를 원하지 않을 때가 있습니다. 대신, 데이터셋을 여러 개의 작은 윈도우로 PARTITION(분할)하는 것을 선호할 수 있습니다.

SQL 문법:
```sql
SELECT
  first_name,
  LEFT(first_name, 1),
  COUNT(*) OVER(PARTITION BY LEFT(first_name, 1))
FROM people
ORDER BY first_name
```

jOOQ 3.3 문법:
```java
select(
  PEOPLE.FIRST_NAME,
  left(PEOPLE.FIRST_NAME, 1),
  count().over().partitionBy(
    left(PEOPLE.FIRST_NAME, 1)))
.from(PEOPLE)
.orderBy(FIRST_NAME);
```

## 윈도우 함수 vs. 집계 함수

표준을 준수하는 SQL 데이터베이스에서는 모든 집계 함수(사용자 정의 집계 함수 포함)를 OVER() 절을 추가하여 윈도우 함수로 변환할 수 있습니다. 이러한 함수는 GROUP BY 절 없이, 그리고 집계 함수에 부과되는 다른 제약 조건 없이 사용할 수 있습니다. 대신, 윈도우 함수는 구체화된(materialised) 테이블 소스에서 작동하므로 SELECT 또는 ORDER BY 절에서만 사용할 수 있습니다. 집계 함수를 윈도우 함수로 변환하는 것 외에도, OVER() 절에서만 사용할 수 있는 다양한 순위 함수(ranking functions)와 분석 함수(analytical functions)가 있습니다. 가장 좋은 방법은 CUBRID, DB2, Oracle, PostgreSQL, SQL Server 또는 Sybase SQL Anywhere 데이터베이스를 시작하고 바로 윈도우 함수를 가지고 놀아보는 것입니다!
