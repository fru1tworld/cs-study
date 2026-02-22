# 놀라운 PostgreSQL 9.4 / SQL:2003 집계 함수용 FILTER 절

> 원문: https://blog.jooq.org/the-awesome-postgresql-9-4-sql2003-filter-clause-for-aggregate-functions/

게시일: 2014년 12월 30일
작성자: Lukas Eder
카테고리: Java, SQL and jOOQ

---

제가 최근에 가장 좋아하는 SQL 기능은 집계 함수를 위한 `FILTER` 절입니다. 이 기능은 PostgreSQL 9.4에서 도입되었으며, SQL:2003 표준을 준수합니다.

## 예제를 통한 설명

세계은행의 국가별 1인당 GDP 데이터가 있다고 가정해봅시다. 2009년부터 2012년까지 8개 국가(캐나다, 독일, 프랑스, 영국, 이탈리아, 일본, 러시아, 미국)의 데이터입니다. 테이블 구조는 다음과 같습니다:

```sql
CREATE TABLE countries (
  code CHAR(2) NOT NULL,
  year INT NOT NULL,
  gdp_per_capita DECIMAL(10, 2) NOT NULL
);
```

데이터는 다음과 같이 보일 수 있습니다:

| code | year | gdp_per_capita |
|------|------|----------------|
| CA   | 2012 | 52409.00       |
| CA   | 2011 | 51791.00       |
| CA   | 2010 | 47465.00       |
| CA   | 2009 | 40764.00       |
| DE   | 2012 | 42598.00       |
| DE   | 2011 | 44355.00       |
| DE   | 2010 | 40408.00       |
| DE   | 2009 | 40270.00       |
| FR   | 2012 | 39759.00       |
| FR   | 2011 | 42578.00       |
| FR   | 2010 | 39186.00       |
| FR   | 2009 | 40488.00       |
| GB   | 2012 | 38649.00       |
| GB   | 2011 | 38927.00       |
| GB   | 2010 | 36573.00       |
| GB   | 2009 | 35455.00       |
| IT   | 2012 | 33814.00       |
| IT   | 2011 | 36988.00       |
| IT   | 2010 | 34673.00       |
| IT   | 2009 | 35724.00       |
| JP   | 2012 | 46548.00       |
| JP   | 2011 | 46204.00       |
| JP   | 2010 | 43118.00       |
| JP   | 2009 | 39473.00       |
| RU   | 2012 | 14091.00       |
| RU   | 2011 | 13324.00       |
| RU   | 2010 | 10710.00       |
| RU   | 2009 | 8616.00        |
| US   | 2012 | 51755.00       |
| US   | 2011 | 49855.00       |
| US   | 2010 | 48358.00       |
| US   | 2009 | 46999.00       |

## FILTER 절을 사용한 쿼리

이제 각 연도별로 1인당 GDP가 40,000달러 이상인 국가의 수를 세고 싶다고 가정해봅시다. 기존 방식으로는 `CASE` 표현식을 사용해야 했지만, `FILTER` 절을 사용하면 훨씬 더 직관적으로 작성할 수 있습니다:

```sql
SELECT
  year,
  count(*) FILTER (WHERE gdp_per_capita >= 40000)
FROM
  countries
GROUP BY
  year
```

결과는 다음과 같습니다:

| year | count |
|------|-------|
| 2012 | 4     |
| 2011 | 5     |
| 2010 | 4     |
| 2009 | 4     |

## 윈도우 함수와 함께 사용

`FILTER` 절의 진정한 강력함은 윈도우 함수와 결합할 때 드러납니다. `OVER()` 절과 함께 사용하면 각 행에 대해 필터링된 집계 값을 계산할 수 있습니다:

```sql
SELECT
  year,
  code,
  gdp_per_capita,
  count(*)
    FILTER (WHERE gdp_per_capita >= 40000)
    OVER   (PARTITION BY year)
FROM
  countries
```

이 쿼리는 각 국가의 데이터와 함께 해당 연도에 1인당 GDP가 40,000달러 이상인 국가의 수를 표시합니다.

결과는 다음과 같습니다:

| year | code | gdp_per_capita | count |
|------|------|----------------|-------|
| 2009 | CA   | 40764.00       | 4     |
| 2009 | DE   | 40270.00       | 4     |
| 2009 | FR   | 40488.00       | 4     |
| 2009 | GB   | 35455.00       | 4     |
| 2009 | IT   | 35724.00       | 4     |
| 2009 | JP   | 39473.00       | 4     |
| 2009 | RU   | 8616.00        | 4     |
| 2009 | US   | 46999.00       | 4     |
| ...  | ...  | ...            | ...   |

## jOOQ에서의 지원

jOOQ 3.6은 `FILTER` 절을 위한 직관적인 API를 제공합니다. `filterWhere()` 메서드를 사용하여 다음과 같이 작성할 수 있습니다:

```java
// jOOQ를 사용한 FILTER 절
count().filterWhere(COUNTRIES.GDP_PER_CAPITA.ge(40000))
```

## PostgreSQL 외 데이터베이스에서의 에뮬레이션

PostgreSQL을 사용하지 않는 데이터베이스의 경우, jOOQ는 자동으로 `CASE` 표현식을 사용하여 동일한 기능을 에뮬레이션합니다:

```sql
SELECT
  year,
  count(CASE WHEN gdp_per_capita >= 40000 THEN 1 END)
FROM
  countries
GROUP BY
  year
```

이 에뮬레이션된 쿼리는 `FILTER` 절과 동일한 결과를 생성하므로, 어떤 데이터베이스를 사용하든 jOOQ를 통해 일관된 방식으로 필터링된 집계를 수행할 수 있습니다.

## FILTER 절의 장점

`FILTER` 절의 주요 장점은 다음과 같습니다:

1. 가독성: `CASE` 표현식보다 훨씬 더 직관적이고 읽기 쉽습니다.

2. 표준 준수: SQL:2003 표준을 따르므로 표준 SQL을 사용하는 것입니다.

3. 다중 필터링된 집계: 단일 쿼리에서 서로 다른 조건을 가진 여러 집계를 쉽게 수행할 수 있습니다. 예를 들어:

```sql
SELECT
  year,
  count(*) FILTER (WHERE gdp_per_capita >= 40000) AS high_gdp_count,
  count(*) FILTER (WHERE gdp_per_capita < 40000) AS low_gdp_count,
  avg(gdp_per_capita) FILTER (WHERE gdp_per_capita >= 40000) AS high_gdp_avg
FROM
  countries
GROUP BY
  year
```

4. 그룹 보존: 일반 `WHERE` 절과 달리, `FILTER` 절은 조건에 맞는 행이 없는 그룹도 결과에 포함시킵니다(카운트가 0인 상태로). 이는 모든 연도를 표시하면서 조건에 맞는 국가의 수를 세고 싶을 때 유용합니다.

## 댓글에서의 토론

한 독자가 일반 `WHERE` 절로도 같은 결과를 얻을 수 있지 않느냐고 질문했습니다. 저자는 두 가지 중요한 차이점을 설명했습니다:

1. `FILTER` 절은 조건에 맞는 행이 없어도 해당 그룹(연도)을 결과에 포함시킵니다. 일반 `WHERE` 절을 사용하면 조건에 맞는 행이 없는 그룹은 결과에서 완전히 제외됩니다.

2. `FILTER` 절을 사용하면 단일 쿼리에서 서로 다른 조건을 가진 여러 집계를 수행할 수 있습니다. 일반 `WHERE` 절로는 이것이 불가능합니다.

---

Happy querying!
