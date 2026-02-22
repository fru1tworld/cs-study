# 불가능하다고 생각했던 10가지 SQL 트릭

> 원문: https://blog.jooq.org/10-sql-tricks-that-you-didnt-think-were-possible/

SQL의 선언적 특성 덕분에 당신은 원하는 것을 기술하면 되고, 어떻게 얻을지는 신경 쓰지 않아도 됩니다. 예를 들어, 데이터베이스에게 user 테이블과 address 테이블을 '연결'(JOIN)하여 스위스에 사는 사용자를 찾고 싶다고 말하면 됩니다. 데이터베이스가 이 정보를 어떻게 검색할지는 신경 쓸 필요가 없습니다.

이러한 추상화가 작동하려면 데이터베이스의 기본을 이해해야 합니다. 외래 키와 인덱스를 설정하여 데이터베이스가 최적의 결정을 내릴 수 있도록 도와줘야 합니다.

이 글에서는 여러 컨퍼런스에서 발표한 고급 SQL 기법들을 소개합니다.

## 1. 모든 것은 테이블이다

SQL에서 가장 기본적인 개념은 모든 결과가 테이블이라는 것입니다. 모든 SELECT 문의 결과는 테이블입니다.

```sql
SELECT *
FROM person
```

이 결과를 다시 쿼리에서 사용할 수 있습니다(파생 테이블):

```sql
SELECT *
FROM (
  SELECT *
FROM person
) t
```

VALUES() 생성자를 사용하면 테이블 리터럴을 만들 수 있습니다:

```sql
SELECT *
FROM (
  VALUES(1),(2),(3)
) t(a)
```

VALUES()를 지원하지 않는 데이터베이스에서는 다음과 같이 작성할 수 있습니다:

```sql
SELECT *
FROM (
  SELECT 1 AS a FROM DUAL UNION ALL
  SELECT 2 AS a FROM DUAL UNION ALL
  SELECT 3 AS a FROM DUAL
) t
```

INSERT 문에서도 동일하게 적용됩니다:

```sql
INSERT INTO my_table(a)
VALUES(1),(2),(3);
```

또는:

```sql
INSERT INTO my_table(a)
SELECT 1 AS a FROM DUAL UNION ALL
SELECT 2 AS a FROM DUAL UNION ALL
SELECT 3 AS a FROM DUAL
```

"모든 것은 테이블이다"라는 원칙은 쿼리의 우아한 조합을 가능하게 합니다.

## 2. 재귀 SQL을 이용한 데이터 생성

Common Table Expression(CTE)을 사용하면 SQL에서 변수를 선언할 수 있습니다:

```sql
WITH
  t1(v1, v2) AS (SELECT 1, 2),
  t2(w1, w2) AS (
    SELECT v1 * 2, v2 * 2
    FROM t1
  )
SELECT *
FROM t1, t2
```

재귀 CTE를 사용하면 시퀀스를 생성할 수 있습니다. 다음 예제는 1부터 5까지의 숫자를 생성합니다:

```sql
WITH RECURSIVE t(v) AS (
  SELECT 1           -- 시드 행
  UNION ALL
  SELECT v + 1       -- 재귀 단계
  FROM t
)
SELECT v
FROM t
LIMIT 5
```

재귀 CTE는 SQL:1999를 튜링 완전하게 만듭니다. 이는 어떤 프로그램이든 SQL로 작성할 수 있다는 것을 의미합니다!

## 3. 누적 합계 계산

윈도우 함수는 결과 집합을 변환하지 않고 행의 부분 집합에 대해 집계를 수행합니다. 구문에는 PARTITION BY(그룹화), ORDER BY(순서 지정), ROWS/RANGE 프레임(윈도우 경계 정의)이 포함됩니다:

```sql
SUM(t.amount) OVER (
  PARTITION BY t.account_id
  ORDER BY     t.value_date DESC,
               t.id         DESC
  ROWS BETWEEN UNBOUNDED PRECEDING
       AND     1         PRECEDING
)
```

이 쿼리는 각 계정에 대해 현재 행 이전의 모든 금액의 합계를 계산합니다. 윈도우 함수는 누적 합계와 순위를 계산하는 데 이상적입니다.

## 4. 간격 없는 연속 시리즈 찾기

사용자의 로그인 날짜에서 연속으로 로그인한 가장 긴 기간을 찾고 싶다고 가정해 봅시다.

먼저 고유한 로그인 날짜를 추출합니다:

```sql
SELECT DISTINCT
  cast(login_time AS DATE) AS login_date
FROM logins
WHERE user_id = :user_id
```

각 날짜에 행 번호를 부여합니다:

```sql
SELECT
  login_date,
  row_number() OVER (ORDER BY login_date)
FROM login_dates
```

핵심 트릭: 날짜에서 행 번호를 빼면 간격이 드러납니다:

```sql
SELECT
  login_date -
  row_number() OVER (ORDER BY login_date)
FROM login_dates
```

연속된 날짜는 동일한 차이 값을 가집니다. 이 값으로 그룹화하면 연속 시리즈를 식별할 수 있습니다:

```sql
SELECT
  min(login_date), max(login_date),
  max(login_date) -
  min(login_date) + 1 AS length
FROM login_date_groups
GROUP BY grp
ORDER BY length DESC
```

전체 쿼리를 CTE로 결합하면:

```sql
WITH
  login_dates AS (
    SELECT DISTINCT cast(login_time AS DATE) login_date
    FROM logins WHERE user_id = :user_id
  ),
  login_date_groups AS (
    SELECT
      login_date,
      login_date - row_number() OVER (ORDER BY login_date) AS grp
    FROM login_dates
  )
SELECT
  min(login_date), max(login_date),
  max(login_date) - min(login_date) + 1 AS length
FROM login_date_groups
GROUP BY grp
ORDER BY length DESC
```

## 5. 시리즈 길이 찾기

금액의 부호(양수/음수) 패턴에 따라 시리즈 길이를 계산하는 방법입니다.

먼저 각 행에 부호와 행 번호를 부여합니다:

```sql
SELECT
  id, amount,
  sign(amount) AS sign,
  row_number()
    OVER (ORDER BY id DESC) AS rn
FROM trx
```

LAG()와 LEAD() 윈도우 함수를 사용하여 부호가 변경되는 지점을 감지합니다:

```sql
SELECT
  trx.*,
  CASE WHEN lag(sign)
       OVER (ORDER BY id DESC) != sign
       THEN rn END AS lo,
  CASE WHEN lead(sign)
       OVER (ORDER BY id DESC) != sign
       THEN rn END AS hi,
FROM trx
```

NULL 처리를 추가하면:

```sql
SELECT -- NULL 처리 포함
  trx.*,
  CASE WHEN coalesce(lag(sign)
       OVER (ORDER BY id DESC), 0) != sign
       THEN rn END AS lo,
  CASE WHEN coalesce(lead(sign)
       OVER (ORDER BY id DESC), 0) != sign
       THEN rn END AS hi,
FROM trx
```

FIRST_VALUE()와 LAST_VALUE()에 IGNORE NULLS를 사용하여 경계 마커를 모든 행에 전파합니다:

```sql
SELECT
  trx.*,
  last_value (lo) IGNORE NULLS OVER (
    ORDER BY id DESC
    ROWS BETWEEN UNBOUNDED PRECEDING
    AND CURRENT ROW) AS lo,
  first_value(hi) IGNORE NULLS OVER (
    ORDER BY id DESC
    ROWS BETWEEN CURRENT ROW
    AND UNBOUNDED FOLLOWING) AS hi
FROM trx
```

NULL 처리를 포함한 완전한 버전:

```sql
SELECT -- NULL 처리 포함
  trx.*,
  coalesce(last_value (lo) IGNORE NULLS OVER (
    ORDER BY id DESC
    ROWS BETWEEN UNBOUNDED PRECEDING
    AND CURRENT ROW), rn) AS lo,
  coalesce(first_value(hi) IGNORE NULLS OVER (
    ORDER BY id DESC
    ROWS BETWEEN CURRENT ROW
    AND UNBOUNDED FOLLOWING), rn) AS hi
FROM trx
```

최종적으로 시리즈 길이를 계산합니다:

```sql
SELECT
  trx.*,
  1 + hi - lo AS length
FROM trx
```

전체 쿼리:

```sql
WITH
  trx1(id, amount, sign, rn) AS (
    SELECT id, amount, sign(amount), row_number() OVER (ORDER BY id DESC)
    FROM trx
  ),
  trx2(id, amount, sign, rn, lo, hi) AS (
    SELECT trx1.*,
    CASE WHEN coalesce(lag(sign)
         OVER (ORDER BY id DESC), 0) != sign
         THEN rn END,
    CASE WHEN coalesce(lead(sign)
         OVER (ORDER BY id DESC), 0) != sign
         THEN rn END
    FROM trx1
  )
SELECT
  trx2.*, 1
  - last_value (lo) IGNORE NULLS OVER (ORDER BY id DESC
    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
  + first_value(hi) IGNORE NULLS OVER (ORDER BY id DESC
    ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)
FROM trx2
```

## 6. 부분 집합 합 문제

부분 집합 합 문제는 주어진 항목들의 부분 집합 중 목표 합계에 가장 가까운 것을 찾는 것입니다. 재귀 CTE를 사용하여 가능한 모든 2^n 조합을 생성한 다음, 목표에 가장 가까운 것을 식별합니다:

```sql
WITH sums(sum, id, calc) AS (
  SELECT item, id, to_char(item) FROM items
  UNION ALL
  SELECT item + sum, items.id, calc || ' + ' || item
  FROM sums JOIN items ON sums.id < items.id
)
SELECT
  totals.id,
  totals.total,
  min (sum) KEEP (
    DENSE_RANK FIRST ORDER BY abs(total - sum)
  ) AS best,
  min (calc) KEEP (
    DENSE_RANK FIRST ORDER BY abs(total - sum)
  ) AS calc
FROM totals
CROSS JOIN sums
GROUP BY totals.id, totals.total
```

이 쿼리는 모든 가능한 합계를 생성하고, 각 목표 합계에 대해 가장 가까운 일치를 찾습니다.

## 7. 상한이 있는 누적 합계

Oracle의 MODEL 절을 사용하면 SQL 내에서 스프레드시트와 같은 공식을 사용할 수 있습니다. 이를 통해 누적 합계가 절대 0 아래로 떨어지지 않도록 하는 로직을 구현할 수 있습니다:

```sql
SELECT *
FROM (
  SELECT date, amount, 0 AS total
  FROM amounts
)
MODEL
  DIMENSION BY (row_number() OVER (ORDER BY date) AS rn)
  MEASURES (date, amount, total)
  RULES (
    total[any] = greatest(0,
  coalesce(total[cv(rn) - 1], 0) + amount[cv(rn)])
  )
```

GREATEST() 함수를 사용하여 합계가 음수가 되지 않도록 합니다.

## 8. 시계열 패턴 인식

Oracle 12c의 MATCH_RECOGNIZE 절은 시계열 데이터에서 복잡한 이벤트 패턴을 감지하는 데 사용됩니다. 이는 사기 탐지 및 실시간 분석에 특히 유용합니다.

기본 구문:

```sql
SELECT * FROM series
MATCH_RECOGNIZE (
  ORDER BY ...           -- 처리 순서
  MEASURES ...           -- 일치에서 출력할 열
  ALL ROWS PER MATCH     -- 반환 사양
  PATTERN (...)          -- 정규 표현식과 같은 패턴
  DEFINE ...             -- 이벤트 정의
)
```

실제 예제 - 패턴이 3회 이상 반복될 때 세 번째 발생에서 경고를 트리거:

```sql
SELECT *
FROM series
MATCH_RECOGNIZE (
  ORDER BY id
  MEASURES classifier() AS trg
  ALL ROWS PER MATCH
  PATTERN (S (R X R+)?)
  DEFINE
    R AS sign(R.amount) = prev(sign(R.amount)),
    X AS sign(X.amount) = prev(sign(X.amount))
)
```

패턴 설명:
- S: 시작 이벤트 (모든 초기 행)
- R: 반복 이벤트 (이전 부호와 일치)
- X: 트리거 이벤트 (패턴과 일치)
- R+: 하나 이상의 추가 반복

패턴 매칭은 선언적 형태로 발생하여 절차적 로직이나 상태 관리 없이 복잡한 이벤트 감지가 가능합니다.

## 9. 피벗과 언피벗

행 기반 데이터를 열 기반으로 변환(피벗)하거나 그 반대(언피벗)로 변환합니다.

PostgreSQL에서 FILTER를 사용한 피벗:

```sql
SELECT
  first_name, last_name,
  count(*) FILTER (WHERE rating = 'NC-17') AS "NC-17",
  count(*) FILTER (WHERE rating = 'PG'   ) AS "PG",
  count(*) FILTER (WHERE rating = 'G'    ) AS "G",
  count(*) FILTER (WHERE rating = 'PG-13') AS "PG-13",
  count(*) FILTER (WHERE rating = 'R'    ) AS "R"
FROM actor AS a
JOIN film_actor AS fa USING (actor_id)
JOIN film AS f USING (film_id)
GROUP BY actor_id
```

표준 SQL에서 CASE를 사용한 동일한 결과:

```sql
SELECT
  first_name, last_name,
  count(CASE rating WHEN 'NC-17' THEN 1 END) AS "NC-17",
  count(CASE rating WHEN 'PG'    THEN 1 END) AS "PG",
  count(CASE rating WHEN 'G'     THEN 1 END) AS "G",
  count(CASE rating WHEN 'PG-13' THEN 1 END) AS "PG-13",
  count(CASE rating WHEN 'R'     THEN 1 END) AS "R"
FROM actor AS a
JOIN film_actor AS fa USING (actor_id)
JOIN film AS f USING (film_id)
GROUP BY actor_id
```

Oracle/SQL Server의 PIVOT 구문:

```sql
SELECT something, something
FROM some_table
PIVOT (
  count(*) FOR rating IN (
    'NC-17' AS "NC-17",
    'PG'    AS "PG",
    'G'     AS "G",
    'PG-13' AS "PG-13",
    'R'     AS "R"
  )
)
```

UNPIVOT 구문:

```sql
SELECT something, something
FROM some_table
UNPIVOT (
  count    FOR rating IN (
    "NC-17" AS 'NC-17',
    "PG"    AS 'PG',
    "G"     AS 'G',
    "PG-13" AS 'PG-13',
    "R"     AS 'R'
  )
)
```

## 10. XML/JSON 처리

PostgreSQL에서 XPath 표현식과 재귀 정규식 패턴 매칭을 사용하여 계층적 데이터 구조를 파싱하고 언네스트합니다:

```sql
WITH RECURSIVE
  x(v) AS (SELECT '...'::xml),
  actors(actor_id, first_name, last_name, films) AS (
    SELECT
      row_number() OVER (),
      (xpath('//first-name/text()', t.v))[1]::TEXT,
      (xpath('//last-name/text()' , t.v))[1]::TEXT,
      (xpath('//films/text()'     , t.v))[1]::TEXT
    FROM unnest(xpath('//actor', (SELECT v FROM x))) t(v)
  ),
  films(actor_id, first_name, last_name, film_id, film) AS (
    SELECT actor_id, first_name, last_name, 1,
      regexp_replace(films, ',.+', '')
    FROM actors
    UNION ALL
    SELECT actor_id, a.first_name, a.last_name, f.film_id + 1,
      regexp_replace(a.films, '.*' || f.film || ', ?(.*?)(,.+)?', '\1')
    FROM films AS f
    JOIN actors AS a USING (actor_id)
    WHERE a.films NOT LIKE '%' || f.film
  )
SELECT *
FROM films
```

이 쿼리는 XML 구조에서 배우 정보를 추출하고, 쉼표로 구분된 영화 목록을 개별 행으로 분리합니다.

## 결론

선언적 SQL 프로그래밍은 다른 사고 방식으로의 연습이 필요하지만, 구현 단계가 아닌 원하는 결과를 기술함으로써 아주 적은 코드로 데이터 간의 매우 복잡한 관계를 표현할 수 있게 해줍니다.

CTE, 윈도우 함수, 그리고 벤더별 확장 기능을 결합하면 현대 SQL은 강력한 선언적 기능을 제공합니다.
