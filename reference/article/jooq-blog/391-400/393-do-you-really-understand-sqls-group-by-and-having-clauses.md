# SQL의 GROUP BY와 HAVING 절을 정말로 이해하고 있는가?

> 원문: https://blog.jooq.org/do-you-really-understand-sqls-group-by-and-having-clauses/

우리가 당연하게 여기는 몇 가지 SQL 개념이 있다. 예를 들어, `GROUP BY` 및 `HAVING` 절이 그렇다. 이러한 절들이 정확히 어떻게 작동하는지 제대로 이해하지 못한 채 사용하는 경우가 많다.

## 데이터베이스 스키마

이 글에서 사용할 테이블 구조는 다음과 같다:

```sql
CREATE TABLE countries (
  code CHAR(2) NOT NULL,
  year INT NOT NULL,
  gdp_per_capita DECIMAL(10, 2) NOT NULL,
  govt_debt DECIMAL(10, 2) NOT NULL
);
```

## 비즈니스 질문

다음과 같은 질문에 답해야 한다고 가정해보자: "지난 4년간 매년 1인당 GDP가 40,000달러를 초과한 국가들 중에서, GDP 대비 평균 정부 부채 비율이 가장 높은 상위 3개 국가는?"

## 주석이 포함된 SQL 쿼리

```sql
-- 평균 정부 부채
select code, avg(govt_debt)

-- 해당 국가들에 대해
from countries

-- 지난 4년간
where year > 2010

-- 맞다, 국가별로
group by code

-- 매년 1인당 GDP가 40,000을 초과한
having min(gdp_per_capita) >= 40000

-- 상위 3개
order by 2 desc
limit 3
```

결과:

```
code     avg
------------
JP    193.00
US     91.95
DE     56.00
```

## SQL 완전 이해를 위한 10단계

이전 글인 "SQL 완전 이해를 위한 10단계"에서 SQL의 논리적 실행 순서를 설명한 바 있다:

1. `FROM`은 데이터 세트를 생성한다
2. `WHERE`는 생성된 데이터 세트를 필터링한다
3. `GROUP BY`는 필터링된 데이터 세트를 집계한다
4. `HAVING`은 집계된 데이터 세트를 필터링한다
5. `SELECT`는 필터링된 집계 데이터 세트를 변환한다
6. `ORDER BY`는 변환된 데이터 세트를 정렬한다
7. `LIMIT .. OFFSET`은 정렬된 데이터 세트를 프레이밍한다

## 빈 GROUP BY 절

다음과 같은 질문을 생각해보자: "1인당 GDP가 50,000달러 이상인 국가가 있는가?"

```sql
select true answer
from countries
having max(gdp_per_capita) >= 50000
```

결과:

```
answer
------
t
```

`EXISTS`를 사용한 대안적 방법:

```sql
select exists(
  select 1
  from countries
  where gdp_per_capita >= 50000
);
```

SQL 1992 표준에 따르면, `HAVING`이 `GROUP BY` 없이 나타나면 `GROUP BY ( )`가 암묵적으로 적용된다. 정확한 표준 문구는 다음과 같다:

"7.10 <having clause>

<having clause> ::= HAVING <search condition>

구문 규칙 1) HC를 <having clause>라 하자. TE를 HC를 직접 포함하는 <table expression>이라 하자. TE가 <group by clause>를 직접 포함하지 않으면, GROUP BY ( )가 암묵적으로 적용된다."

표준은 다음을 추가로 정의한다:

```
<group by clause> ::=
    GROUP BY <grouping specification>

<grouping specification> ::=
    <grouping column reference>
  | <rollup list>
  | <cube list>
  | <grouping sets list>
  | <grand total>
  | <concatenated grouping>

<grouping set> ::=
    <ordinary grouping set>
  | <rollup list>
  | <cube list>
  | <grand total>

<grand total> ::= <left paren> <right paren>
```

단순 MAX 쿼리:

```sql
select max(gdp_per_capita)
from countries;
```

결과:

```
     max
--------
52409.00
```

## GROUPING SETS

다음과 같은 질문을 고려해보자: "연도별 _또는_ 국가별 최고 1인당 GDP는?"

```sql
select code, year, max(gdp_per_capita)
from countries
group by grouping sets ((code), (year))
```

결과:

```
code    year    max
------------------------
NULL    2009    46999.00
NULL    2010    48358.00
NULL    2011    51791.00
NULL    2012    52409.00

CA      NULL    52409.00
DE      NULL    44355.00
FR      NULL    42578.00
GB      NULL    38927.00
IT      NULL    36988.00
JP      NULL    46548.00
RU      NULL    14091.00
US      NULL    51755.00
```

동등한 UNION ALL 쿼리:

```sql
select code, null, max(gdp_per_capita)
from countries
group by code
union all
select null, year, max(gdp_per_capita)
from countries
group by year;
```

## CUBE()

GROUPING_ID를 사용한 쿼리:

```sql
select
  code, year, max(gdp_per_capita),
  grouping_id(code, year) grp
from countries
where gdp_per_capita >= 48000
group by grouping sets (
  (),
  (code),
  (year),
  (code, year)
)
order by grp desc;
```

결과:

```
code    year    max         grp
---------------------------------
NULL    NULL    52409.00    3

NULL    2012    52409.00    2
NULL    2010    48358.00    2
NULL    2011    51791.00    2

CA      NULL    52409.00    1
US      NULL    51755.00    1

US      2010    48358.00    0
CA      2012    52409.00    0
US      2012    51755.00    0
CA      2011    51791.00    0
US      2011    49855.00    0
```

CUBE()를 사용한 동등한 쿼리:

```sql
select
  code, year, max(gdp_per_capita),
  grouping_id(code, year) grp
from countries
where gdp_per_capita >= 48000
group by cube(code, year)
order by grp desc;
```

## 호환성

GROUPING SETS는 "jOOQ가 현재 지원하는 17개 RDBMS 중 4개"에서만 작동한다:

- DB2
- Oracle
- SQL Server
- Sybase SQL Anywhere

PostgreSQL은 명시적으로 지원하지 않는다.

## jOOQ 코드 예제

GROUPING SETS 버전:

```java
// Countries는 jOOQ 코드 생성기가
// COUNTRIES 테이블에 대해 생성한 객체이다.
Countries c = COUNTRIES;

ctx.select(
       c.CODE,
       c.YEAR,
       max(c.GDP_PER_CAPITA),
       groupingId(c.CODE, c.YEAR).as("grp"))
   .from(c)
   .where(c.GDP_PER_CAPITA.ge(new BigDecimal("48000")))
   .groupBy(groupingSets(new Field[][] {
       {},
       { c.CODE },
       { c.YEAR },
       { c.CODE, c.YEAR }
   }))
   .orderBy(fieldByName("grp").desc())
   .fetch();
```

CUBE() 버전:

```java
ctx.select(
       c.CODE,
       c.YEAR,
       max(c.GDP_PER_CAPITA),
       groupingId(c.CODE, c.YEAR).as("grp"))
   .from(c)
   .where(c.GDP_PER_CAPITA.ge(new BigDecimal("48000")))
   .groupBy(cube(c.CODE, c.YEAR))
   .orderBy(fieldByName("grp").desc())
   .fetch();
```

향후 버전에서는 지원하지 않는 데이터베이스에 대해 UNION ALL 쿼리를 통해 GROUPING SETS를 에뮬레이션할 예정이다.
