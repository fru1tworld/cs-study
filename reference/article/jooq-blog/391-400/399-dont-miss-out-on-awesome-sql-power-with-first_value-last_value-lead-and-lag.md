# FIRST_VALUE(), LAST_VALUE(), LEAD(), LAG()의 놀라운 SQL 파워를 놓치지 마라

> 원문: https://blog.jooq.org/dont-miss-out-on-awesome-sql-power-with-first_value-last_value-lead-and-lag/

저자: lukaseder
날짜: 2014년 11월 7일

---

## 소개

이 글에서는 상용 데이터베이스와 PostgreSQL, Firebird, CUBRID 같은 일부 오픈소스 데이터베이스에서 사용할 수 있는 강력한 윈도우 함수들을 살펴봅니다. 특히 현재 행 앞이나 뒤에 위치한 행의 값에 접근하는 함수들에 초점을 맞춥니다.

---

## 테스트 데이터 설정

### 사용된 국가들 (G8)
- 캐나다(CA), 프랑스(FR), 독일(DE), 이탈리아(IT), 일본(JP), 러시아 연방(RU), 영국(GB), 미국(US)

### 데이터 포인트 (2009-2012)

1인당 GDP (현재 미국 달러):
```
        2009    2010    2011    2012
CA      40,764  47,465  51,791  52,409
DE      40,270  40,408  44,355  42,598
FR      40,488  39,448  42,578  39,759
GB      35,455  36,573  38,927  38,649
IT      35,724  34,673  36,988  33,814
JP      39,473  43,118  46,204  46,548
RU       8,616  10,710  13,324  14,091
US      46,999  48,358  49,855  51,755
```

중앙정부 부채, 총액 (GDP 대비 %):
```
        2009    2010    2011    2012
CA        51.3    51.4    52.5    53.5
DE        47.6    55.5    55.1    56.9
FR        85.0    89.2    93.2   103.8
GB        71.7    85.2    99.6   103.2
IT       121.3   119.9   113.0   131.1
JP       166.8   174.8   189.5   196.5
RU         8.7     9.1     9.3     9.4
US        76.3    85.6    90.1    93.8
```

### 데이터베이스 테이블 생성 (PostgreSQL)

```sql
CREATE TABLE countries (
  code CHAR(2) NOT NULL,
  year INT NOT NULL,
  gdp_per_capita DECIMAL(10, 2) NOT NULL,
  govt_debt DECIMAL(10, 2) NOT NULL
);

INSERT INTO countries
VALUES ('CA', 2009, 40764, 51.3),
       ('CA', 2010, 47465, 51.4),
       ('CA', 2011, 51791, 52.5),
       ('CA', 2012, 52409, 53.5),
       ('DE', 2009, 40270, 47.6),
       ('DE', 2010, 40408, 55.5),
       ('DE', 2011, 44355, 55.1),
       ('DE', 2012, 42598, 56.9),
       ('FR', 2009, 40488, 85.0),
       ('FR', 2010, 39448, 89.2),
       ('FR', 2011, 42578, 93.2),
       ('FR', 2012, 39759, 103.8),
       ('GB', 2009, 35455, 121.3),
       ('GB', 2010, 36573, 85.2),
       ('GB', 2011, 38927, 99.6),
       ('GB', 2012, 38649, 103.2),
       ('IT', 2009, 35724, 121.3),
       ('IT', 2010, 34673, 119.9),
       ('IT', 2011, 36988, 113.0),
       ('IT', 2012, 33814, 131.1),
       ('JP', 2009, 39473, 166.8),
       ('JP', 2010, 43118, 174.8),
       ('JP', 2011, 46204, 189.5),
       ('JP', 2012, 46548, 196.5),
       ('RU', 2009, 8616, 8.7),
       ('RU', 2010, 10710, 9.1),
       ('RU', 2011, 13324, 9.3),
       ('RU', 2012, 14091, 9.4),
       ('US', 2009, 46999, 76.3),
       ('US', 2010, 48358, 85.6),
       ('US', 2011, 49855, 90.1),
       ('US', 2012, 51755, 93.8);
```

---

## 쿼리의 재미를 시작하다

### 기본 최대값 쿼리

```sql
SELECT MAX(gdp_per_capita), MAX(govt_debt)
FROM countries;
```

결과:
```
52409.00    196.50
```

### 전통적인 SQL-92 방식 (최고값 찾기)

```sql
SELECT
  'highest gdp per capita' AS what,
  c1.*
FROM countries c1
WHERE NOT EXISTS (
  SELECT 1
  FROM countries c2
  WHERE c1.gdp_per_capita < c2.gdp_per_capita
)
UNION ALL
SELECT
  'highest government debt' AS what,
  c1.*
FROM countries c1
WHERE NOT EXISTS (
  SELECT 1
  FROM countries c2
  WHERE c1.govt_debt < c2.govt_debt
);
```

### 정량화된 비교 술어 사용

```sql
SELECT
  'highest gdp per capita' AS what,
  countries.*
FROM countries
WHERE gdp_per_capita >= ALL (
  SELECT gdp_per_capita FROM countries
)
UNION ALL
SELECT
  'highest government debt' AS what,
  countries.*
FROM countries
WHERE govt_debt >= ALL (
  SELECT govt_debt FROM countries
);
```

### 서브쿼리로 단순화

```sql
SELECT
  'highest gdp per capita' AS what,
  countries.*
FROM countries
WHERE gdp_per_capita = (
  SELECT MAX(gdp_per_capita) FROM countries
)
UNION ALL
SELECT
  'highest government debt' AS what,
  countries.*
FROM countries
WHERE govt_debt = (
  SELECT MAX(govt_debt) FROM countries
);
```

결과:
```
what                     code year       gdp    debt
----------------------------------------------------
highest gdp per capita   CA   2012  52409.00   53.50
highest government debt  JP   2012  46548.00  196.50
```

---

## FIRST_VALUE()와 LAST_VALUE()

### 기본 FIRST_VALUE() 쿼리

```sql
SELECT
  countries.*,
  FIRST_VALUE (code)           OVER (w_gdp) AS max_gdp_code,
  FIRST_VALUE (year)           OVER (w_gdp) AS max_gdp_year,
  FIRST_VALUE (gdp_per_capita) OVER (w_gdp) AS max_gdp_gdp,
  FIRST_VALUE (govt_debt)      OVER (w_gdp) AS max_gdp_debt
FROM
  countries
WINDOW
  w_gdp  AS (ORDER BY gdp_per_capita DESC)
ORDER BY
  code, year;
```

결과 샘플 (확장된 행들 표시):
```
각 국가                   연도별 최고
-----------------------------------------------
CA 2009 40764.00 51.30   CA 2012 52409.00 53.50
CA 2010 47465.00 51.40   CA 2012 52409.00 53.50
CA 2011 51791.00 52.50   CA 2012 52409.00 53.50
CA 2012 52409.00 53.50   CA 2012 52409.00 53.50
```

### 각 행을 최고값과 비교하기 (연도별 파티션)

```sql
SELECT
  countries.*,
  TO_CHAR(100 * gdp_per_capita / FIRST_VALUE (gdp_per_capita)
    OVER (w_gdp) , '999.99 %') gdp_rank,
  TO_CHAR(100 * govt_debt / FIRST_VALUE (govt_debt)
    OVER (w_debt), '999.99 %') debt_rank
FROM
  countries
WINDOW
  w_gdp  AS (PARTITION BY year ORDER BY gdp_per_capita DESC),
  w_debt AS (PARTITION BY year ORDER BY govt_debt DESC)
ORDER BY
  code, year;
```

결과:
```
국가                      퍼센트
------------------------------------------
CA   2009  40764   51.3    86.73%   30.76%
CA   2010  47465   51.4    98.15%   29.41%
CA   2011  51791   52.5   100.00%   27.70%
CA   2012  52409   53.5   100.00%   27.23%
DE   2009  40270   47.6    85.68%   28.54%
DE   2010  40408   55.5    83.56%   31.75%
DE   2011  44355   55.1    85.64%   29.08%
DE   2012  42598   56.9    81.28%   28.96%
FR   2009  40488   85.0    86.15%   50.96%
FR   2010  39448   89.2    81.57%   51.03%
FR   2011  42578   93.2    82.21%   49.18%
FR   2012  39759  103.8    75.86%   52.82%
GB   2009  35455  121.3    75.44%   72.72%
GB   2010  36573   85.2    75.63%   48.74%
GB   2011  38927   99.6    75.16%   52.56%
GB   2012  38649  103.2    73.74%   52.52%
IT   2009  35724  121.3    76.01%   72.72%
IT   2010  34673  119.9    71.70%   68.59%
IT   2011  36988  113.0    71.42%   59.63%
IT   2012  33814  131.1    64.52%   66.72%
JP   2009  39473  166.8    83.99%  100.00%
JP   2010  43118  174.8    89.16%  100.00%
JP   2011  46204  189.5    89.21%  100.00%
JP   2012  46548  196.5    88.82%  100.00%
RU   2009   8616    8.7    18.33%    5.22%
RU   2010  10710    9.1    22.15%    5.21%
RU   2011  13324    9.3    25.73%    4.91%
RU   2012  14091    9.4    26.89%    4.78%
US   2009  46999   76.3   100.00%   45.74%
US   2010  48358   85.6   100.00%   48.97%
US   2011  49855   90.1    96.26%   47.55%
US   2012  51755   93.8    98.75%   47.74%
```

### 국가별 파티션 (최고/최저 연도 찾기)

```sql
SELECT
  countries.*,
  TO_CHAR(100 * gdp_per_capita / FIRST_VALUE (gdp_per_capita)
    OVER (w_gdp), '999.99 %') gdp_rank,
  TO_CHAR(100 * govt_debt / FIRST_VALUE (govt_debt)
    OVER (w_debt), '999.99 %') debt_rank
FROM
  countries
WINDOW
  w_gdp  AS (PARTITION BY code ORDER BY gdp_per_capita DESC),
  w_debt AS (PARTITION BY code ORDER BY govt_debt DESC)
ORDER BY
  code, year;
```

결과:
```
국가                       퍼센트
------------------------------------------
CA   2009  40764   51.3    77.78%   95.89%
CA   2010  47465   51.4    90.57%   96.07%
CA   2011  51791   52.5    98.82%   98.13%
CA   2012  52409   53.5   100.00%  100.00%
DE   2009  40270   47.6    90.79%   83.66%
DE   2010  40408   55.5    91.10%   97.54%
DE   2011  44355   55.1   100.00%   96.84%
DE   2012  42598   56.9    96.04%  100.00%
FR   2009  40488   85.0    95.09%   81.89%
FR   2010  39448   89.2    92.65%   85.93%
FR   2011  42578   93.2   100.00%   89.79%
FR   2012  39759  103.8    93.38%  100.00%
GB   2009  35455  121.3    91.08%  100.00%
GB   2010  36573   85.2    93.95%   70.24%
GB   2011  38927   99.6   100.00%   82.11%
GB   2012  38649  103.2    99.29%   85.08%
IT   2009  35724  121.3    96.58%   92.52%
IT   2010  34673  119.9    93.74%   91.46%
IT   2011  36988  113.0   100.00%   86.19%
IT   2012  33814  131.1    91.42%  100.00%
JP   2009  39473  166.8    84.80%   84.89%
JP   2010  43118  174.8    92.63%   88.96%
JP   2011  46204  189.5    99.26%   96.44%
JP   2012  46548  196.5   100.00%  100.00%
RU   2009   8616    8.7    61.15%   92.55%
RU   2010  10710    9.1    76.01%   96.81%
RU   2011  13324    9.3    94.56%   98.94%
RU   2012  14091    9.4   100.00%  100.00%
US   2009  46999   76.3    90.81%   81.34%
US   2010  48358   85.6    93.44%   91.26%
US   2011  49855   90.1    96.33%   96.06%
US   2012  51755   93.8   100.00%  100.00%
```

### LAST_VALUE() 주의사항

`LAST_VALUE()`를 `ORDER BY`와 함께 사용할 때 중요한 주의사항이 있습니다. `ORDER BY`를 사용하는 윈도우 정의는 암시적으로 프레임 절을 포함합니다:

```sql
-- 전체 데이터 세트에서 "마지막" 연도 찾기
-- 이것은 예상대로 동작하지 않을 수 있으므로, 항상 명시적인
-- ORDER BY 절을 제공하세요
LAST_VALUE (year) OVER()

-- 이 두 가지는 암시적으로 동일합니다. 전체 데이터
-- 세트에서 "마지막" 연도를 찾는 것이 아니라, 현재 행
-- "이전"의 프레임에서만 찾습니다. 다시 말해, 현재 행이
-- 항상 "마지막 값"이 됩니다!
LAST_VALUE (year) OVER(ORDER BY year)
LAST_VALUE (year) OVER(
  ORDER BY year
  ROWS BETWEEN UNBOUNDED PRECEDING
           AND CURRENT ROW
)

-- 명시적 정렬로 전체 데이터 세트에서 "마지막" 연도 찾기
LAST_VALUE (year) OVER(
  ORDER BY year
  ROWS BETWEEN UNBOUNDED PRECEDING
           AND UNBOUNDED FOLLOWING
)
```

---

## LEAD()와 LAG()

### 기본 LEAD()와 LAG() 예제

```sql
-- 모든 고유 연도(2009-2012)를 포함하는
-- 데이터 소스로 이 뷰를 사용합니다
WITH years AS (
  SELECT DISTINCT year
  FROM countries
)
SELECT
  FIRST_VALUE (year)    OVER w_year AS first,
  LEAD        (year, 2) OVER w_year AS lead2,
  LEAD        (year)    OVER w_year AS lead1,
  year,
  LAG         (year)    OVER w_year AS lag1,
  LAG         (year, 2) OVER w_year AS lag2,
  LAST_VALUE  (year)    OVER w_year AS last
FROM
  years
WINDOW
  w_year AS (
    ORDER BY year DESC
    ROWS BETWEEN UNBOUNDED PRECEDING
             AND UNBOUNDED FOLLOWING
  )
ORDER BY year;
```

결과:
```
first  lead2  lead1  year   lag1   lag2   last
----------------------------------------------
2012                 2009   2010   2011   2009
2012          2009   2010   2011   2012   2009
2012   2009   2010   2011   2012          2009
2012   2010   2011   2012                 2009
```

### GDP 기준 이웃 국가 찾기

```sql
SELECT
  year,
  code,
  gdp_per_capita,
  LEAD (code)           OVER w_gdp AS runner_up_code,
  LEAD (gdp_per_capita) OVER w_gdp AS runner_up_gdp,
  LAG  (code)           OVER w_gdp AS leader_code,
  LAG  (gdp_per_capita) OVER w_gdp AS leader_gdp
FROM
  countries
WINDOW
  w_gdp AS (PARTITION BY year ORDER BY gdp_per_capita DESC)
ORDER BY year DESC, gdp_per_capita DESC;
```

결과:
```
year   국가           차순위       선두
------------------------------------------
2012   CA  52409    US  51755
2012   US  51755    JP  46548    CA  52409
2012   JP  46548    DE  42598    US  51755
2012   DE  42598    FR  39759    JP  46548
2012   FR  39759    GB  38649    DE  42598
2012   GB  38649    IT  33814    FR  39759
2012   IT  33814    RU  14091    GB  38649
2012   RU  14091                 IT  33814

2011   CA  51791    US  49855
2011   US  49855    JP  46204    CA  51791
2011   JP  46204    DE  44355    US  49855
2011   DE  44355    FR  42578    JP  46204
2011   FR  42578    GB  38927    DE  44355
2011   GB  38927    IT  36988    FR  42578
2011   IT  36988    RU  13324    GB  38927
2011   RU  13324                 IT  36988

2010   US  48358    CA  47465
2010   CA  47465    JP  43118    US  48358
2010   JP  43118    DE  40408    CA  47465
2010   DE  40408    FR  39448    JP  43118
2010   FR  39448    GB  36573    DE  40408
2010   GB  36573    IT  34673    FR  39448
2010   IT  34673    RU  10710    GB  36573
2010   RU  10710                 IT  34673

2009   US  46999    CA  40764
2009   CA  40764    FR  40488    US  46999
2009   FR  40488    DE  40270    CA  40764
2009   DE  40270    JP  39473    FR  40488
2009   JP  39473    IT  35724    DE  40270
2009   IT  35724    GB  35455    JP  39473
2009   GB  35455    RU   8616    IT  35724
2009   RU   8616                 GB  35455
```

---

## jOOQ 구현

이 글에는 jOOQ를 사용한 Java 구현도 포함되어 있습니다. 전체 코드는 다음과 같습니다:

```java
// 생성된 테이블과 DSL의 모든 jOOQ 함수를 정적 임포트합니다
import static org.jooq.example.db.postgres.Tables.*;
import static org.jooq.impl.DSL.*;

// 별칭을 지정하여 테이블 참조를 짧게 만듭니다
Countries c = COUNTRIES;

// 윈도우 정의를 지정합니다
WindowDefinition w_gdp =
  name("w_gdp").as(
    partitionBy(c.YEAR)
    .orderBy(c.GDP_PER_CAPITA.desc())
  );

// 네이티브 SQL처럼 쿼리를 작성합니다
System.out.println(
    DSL.using(conn)
       .select(
           c.YEAR,
           c.CODE,
           c.GDP_PER_CAPITA,
           lead(c.CODE)          .over(w_gdp).as("runner_up_code"),
           lead(c.GDP_PER_CAPITA).over(w_gdp).as("runner_up_gdp"),
           lag (c.CODE)          .over(w_gdp).as("leader_code"),
           lag (c.GDP_PER_CAPITA).over(w_gdp).as("leader_gdp")
       )
       .from(c)
       .window(w_gdp)
       .orderBy(c.YEAR.desc(), c.GDP_PER_CAPITA.desc())
       .fetch()
);
```

### jOOQ 출력 테이블

```
+----+----+--------------+--------------+-------------+-----------+----------+
|year|code|gdp_per_capita|runner_up_code|runner_up_gdp|leader_code|leader_gdp|
+----+----+--------------+--------------+-------------+-----------+----------+
|2012|CA  |      52409.00|US            |     51755.00|{null}     |    {null}|
|2012|US  |      51755.00|JP            |     46548.00|CA         |  52409.00|
|2012|JP  |      46548.00|DE            |     42598.00|US         |  51755.00|
|2012|DE  |      42598.00|FR            |     39759.00|JP         |  46548.00|
|2012|FR  |      39759.00|GB            |     38649.00|DE         |  42598.00|
|2012|GB  |      38649.00|IT            |     33814.00|FR         |  39759.00|
|2012|IT  |      33814.00|RU            |     14091.00|GB         |  38649.00|
|2012|RU  |      14091.00|{null}        |       {null}|IT         |  33814.00|
|2011|CA  |      51791.00|US            |     49855.00|{null}     |    {null}|
|2011|US  |      49855.00|JP            |     46204.00|CA         |  51791.00|
|2011|JP  |      46204.00|DE            |     44355.00|US         |  49855.00|
|2011|DE  |      44355.00|FR            |     42578.00|JP         |  46204.00|
|2011|FR  |      42578.00|GB            |     38927.00|DE         |  44355.00|
|2011|GB  |      38927.00|IT            |     36988.00|FR         |  42578.00|
|2011|IT  |      36988.00|RU            |     13324.00|GB         |  38927.00|
|2011|RU  |      13324.00|{null}        |       {null}|IT         |  36988.00|
|2010|US  |      48358.00|CA            |     47465.00|{null}     |    {null}|
|2010|CA  |      47465.00|JP            |     43118.00|US         |  48358.00|
|2010|JP  |      43118.00|DE            |     40408.00|CA         |  47465.00|
|2010|DE  |      40408.00|FR            |     39448.00|JP         |  43118.00|
|2010|FR  |      39448.00|GB            |     36573.00|DE         |  40408.00|
|2010|GB  |      36573.00|IT            |     34673.00|FR         |  39448.00|
|2010|IT  |      34673.00|RU            |     10710.00|GB         |  36573.00|
|2010|RU  |      10710.00|{null}        |       {null}|IT         |  34673.00|
|2009|US  |      46999.00|CA            |     40764.00|{null}     |    {null}|
|2009|CA  |      40764.00|FR            |     40488.00|US         |  46999.00|
|2009|FR  |      40488.00|DE            |     40270.00|CA         |  40764.00|
|2009|DE  |      40270.00|JP            |     39473.00|FR         |  40488.00|
|2009|JP  |      39473.00|IT            |     35724.00|DE         |  40270.00|
|2009|IT  |      35724.00|GB            |     35455.00|JP         |  39473.00|
|2009|GB  |      35455.00|RU            |      8616.00|IT         |  35724.00|
|2009|RU  |       8616.00|{null}        |       {null}|GB         |  35455.00|
+----+----+--------------+--------------+-------------+-----------+----------+
```

---

## 결론

윈도우 함수는 모든 주요 상용 데이터베이스에서 사용할 수 있는 믿을 수 없을 정도로 강력한 기능이며, PostgreSQL, Firebird, CUBRID 같은 일부 오픈소스 데이터베이스에서도 사용할 수 있습니다.

윈도우 함수는 SQL 기능의 주요한 진화를 나타냅니다. 이 글은 jOOQ를 사용하든 네이티브 SQL을 사용하든 개발자들이 데이터베이스 작업에서 이러한 함수들을 활용할 것을 권장하며 마무리합니다.
