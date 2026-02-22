# jOOQ에 구현된 SQL 방언 에뮬레이션 Top 10

> 원문: https://blog.jooq.org/top-10-sql-dialect-emulations-implemented-in-jooq/

SQL 표준은 SQL 기능을 구현하는 방법에 대한 좋은 가이드라인을 제공합니다. 하지만 대부분의 방언은 어떤 식으로든 표준에서 벗어납니다(때로는 MySQL처럼 극단적으로 벗어나기도 합니다).

하지만 이러한 이탈이 반드시 나쁜 것은 아닙니다. 혁신은 표준에 의해 주도되는 것이 아니라, 개별 벤더들이 다른 관점에서 문제를 해결하려는 시도에서 비롯됩니다. 그리고 때때로 그 혁신이 표준이 되기도 합니다.

한 가지 예로 Oracle의 MATCH_RECOGNIZE 기능이 있습니다. 다른 기능들은 아직 표준화되지 않았는데, 예를 들어 Oracle/SQL Server의 PIVOT과 UNPIVOT이 있습니다.

많은 경우, 벤더별 기능은 동등한 표준 SQL로 변환되거나 다른 벤더별 SQL로 변환될 수 있습니다.

jOOQ는 21개의 SQL 방언을 하나의 통합된 Java API로 표준화합니다. jOOQ 버전 3.9부터는 jOOQ에 내장된 파서를 통해 이러한 방언 변환을 실험해볼 수 있습니다. https://www.jooq.org/translate 에서 온라인으로 사용 가능합니다.

## 1. 빈 FROM 절

많은 데이터베이스는 FROM 절을 요구하지만(Access, CUBRID, DB2, Derby, Firebird, HANA, HSQLDB, Informix, Ingres, MariaDB, MySQL, Oracle, Sybase SQL Anywhere), 다른 데이터베이스들은 그렇지 않습니다(H2, PostgreSQL, Redshift, SQL Server, SQLite, Sybase ASE, Vertica).

예제 쿼리:

```sql
SELECT current_timestamp
```

Oracle 변환:

```sql
SELECT current_timestamp FROM dual
```

jOOQ의 파서를 사용하여 이를 모든 방언에서 어떻게 변환하는지 보여주는 Java 코드 예제가 있습니다. 출력 결과는 Oracle이 DUAL을 필요로 하는 반면, PostgreSQL은 FROM 절이 필요 없음을 보여줍니다.

## 2. LIMIT .. OFFSET

SQL:2016 표준 구문은 OFFSET과 FETCH 절을 포함하며 ROW/ROWS와 ONLY/WITH TIES 옵션을 제공합니다. 저자는 성능상의 이유로 OFFSET을 사용하지 말 것을 강조합니다.

예제 쿼리 (OFFSET 없이 FETCH):

```sql
SELECT film_id, title, length
FROM film
ORDER BY length DESC
FETCH NEXT 3 ROWS ONLY
```

결과 (Sakila 데이터베이스에서 가장 긴 영화 3편):

```
FILM_ID  TITLE           LENGTH
-------------------------------
212      DARN FORRESTER  185
182      CONTROL ANTHEM  185
141      CHICAGO NORTH   185
```

변환 예시:

- ACCESS/ASE/SQL Server: `select top 3 film_id, title, length from film order by length desc`
- MySQL/PostgreSQL/H2: `select film_id, title, length from film order by length desc limit 3`
- Oracle: `select film_id, title, length from film order by length desc fetch next 3 rows only`
- DB2: `select film_id, title, length from film order by length desc fetch first 3 rows only`
- Informix: `select first 3 film_id, title, length from film order by length desc`

OFFSET 포함 예제:

```sql
SELECT film_id, title, length
FROM film
ORDER BY length DESC
OFFSET 3 ROWS FETCH NEXT 3 ROWS ONLY
```

복잡한 에뮬레이션 (DB2):

```sql
select "v0" film_id, "v1" title, "v2" length from (
  select
    film_id "v0", title "v1", length "v2",
    row_number() over (order by length desc) "rn"
  from film order by "v2" desc
) "x"
where "rn" > 3 and "rn" <= (3 + 3)
order by "rn"
```

주요 관찰 사항으로 세 가지 계열이 있습니다: 표준 FETCH 계열(DB2, Derby, Ingres, Oracle), MySQL LIMIT 계열(CUBRID, H2, HANA, HSQLDB, MariaDB, MySQL, PostgreSQL, Redshift, SQLite, Vertica), 그리고 T-SQL TOP 계열(Access, ASE, SQL Server, Sybase)입니다.

## 3. WITH TIES

WITH TIES 절은 상위 N개 행과 함께 경계에서 동점인 모든 행을 검색합니다.

예제 쿼리:

```sql
SELECT film_id, title, length
FROM film
ORDER BY length DESC
FETCH NEXT 3 ROWS WITH TIES
```

결과 (모두 길이가 185인 영화 10편):

```
FILM_ID  TITLE               LENGTH
-----------------------------------
212      DARN FORRESTER      185
872      SWEET BROTHERHOOD   185
817      SOLDIERS EVOLUTION  185
991      WORST BANGER        185
690      POND SEATTLE        185
609      MUSCLE BRIGHT       185
349      GANGS PRIDE         185
426      HOME PITY           185
182      CONTROL ANTHEM      185
141      CHICAGO NORTH       185
```

구현 방식:

- Oracle/PostgreSQL: `FETCH NEXT 3 ROWS WITH TIES`로 네이티브 지원
- SQL Server: 벤더별 구문 `select top 3 with ties film_id, title, length from film order by length desc`
- DB2/MySQL/기타: `rank() over (order by length desc)` 윈도우 함수를 사용한 에뮬레이션

## 4. 중첩된 집합 연산

이 섹션에서는 UNION, INTERSECT, EXCEPT 연산과 ALL 변형을 탐구합니다.

예제 쿼리:

```sql
SELECT first_name, last_name
FROM actor
UNION
SELECT first_name, last_name
FROM customer
EXCEPT
SELECT 'ADAM', 'GRANT'
ORDER BY 1, 2
```

PostgreSQL (자연스럽게 동작):

```sql
(
  SELECT first_name, last_name
  FROM actor
  UNION
  SELECT first_name, last_name
  FROM customer
)
EXCEPT
SELECT 'ADAM', 'GRANT'
ORDER BY 1, 2
```

MySQL (래핑 필요):

```sql
select * from (
  select * from (
    select first_name, last_name from actor
  ) x
  union
  select * from (
    select first_name, last_name from customer
  ) x
) x
except
select * from (
  select 'ADAM', 'GRANT' from dual
)
x order by 1, 2
```

Derby (버그로 인해 복잡함):

```sql
select first_name, last_name from (
  select first_name, last_name from (
    select first_name, last_name from actor
  ) x
  union
  select first_name, last_name from (
    select first_name, last_name from customer
  ) x
) x
except
select "ADAM", "GRANT" from (
  select 'ADAM', 'GRANT' from "SYSIBM"."SYSDUMMY1"
)
x order by 1, 2
```

주요 관찰 사항: Access는 EXCEPT를 지원하지 않습니다. Firebird는 파서 문제가 있습니다. MySQL은 파서 제한으로 인해 추가 서브쿼리 래핑이 필요합니다. Derby는 특정 버그(DERBY-6983, DERBY-6984)로 인해 문제가 있습니다.

## 5. 파생 컬럼 리스트

파생 테이블에서 테이블과 컬럼의 이름을 동시에 변경할 수 있는 SQL 표준 기능입니다.

PostgreSQL 예제:

```sql
SELECT a, b
FROM (
  SELECT first_name, last_name
  FROM actor
) t(a, b)
WHERE a LIKE 'Z%'
```

결과:

```
A     B
----------
ZERO  CAGE
```

H2/MySQL/SQLite (네이티브 지원 없음):

```sql
select a, b from (
  (select null a, null b where 1 = 0)
   union all
  (select first_name, last_name from actor)
) t
where a like 'Z%'
```

이 우회 방법은 0행 UNION ALL 서브쿼리를 사용하여 컬럼 이름을 정의합니다. 집합 연산은 첫 번째 서브쿼리의 컬럼 이름을 사용하기 때문입니다.

## 6. VALUES 절

VALUES 절은 INSERT 문 외부에서도 인라인 파생 테이블을 생성하는 데 사용될 수 있습니다.

PostgreSQL 예제:

```sql
VALUES ('Hello', 'World'), ('Cool', 'eh?')
```

결과:

```
column1  column2
----------------
Hello    World
Cool     eh?
```

파생 테이블 컨텍스트에서 (PostgreSQL):

```sql
SELECT *
FROM (
  VALUES ('Hello', 'World'), ('Cool', 'eh?')
) AS t(a, b)
```

Oracle (VALUES 지원 없음):

```sql
select "v"."c1", "v"."c2" from (
  (select null "c1", null "c2" from dual where 1 = 0)
   union all
  (select * from (
     (select 'Hello', 'World' from dual)
      union all
     (select 'Cool', 'eh?' from dual)
   ) "v")
) "v"
```

Sybase SQL Anywhere:

```sql
select [v].[c1], [v].[c2] from (
  (select 'Hello', 'World' from [SYS].[DUMMY])
   union all
  (select 'Cool', 'eh?' from [SYS].[DUMMY])
) [v]([c1], [c2])
```

네 가지 지원 유형이 있습니다: PostgreSQL과 기타(VALUES와 파생 컬럼 리스트 모두 지원), H2와 기타(VALUES만 지원), Sybase SQL Anywhere와 기타(파생 컬럼 리스트만 지원), Oracle과 기타(둘 다 지원하지 않음).

## 7. 행 값 표현식을 사용한 술어

튜플 표현식은 여러 컬럼을 동시에 비교할 수 있게 해줍니다.

동등 술어:

```sql
SELECT *
FROM customer
WHERE (first_name, last_name)
    = ('MARY', 'SMITH')
```

PostgreSQL:

```sql
select * from customer
where (first_name, last_name) = ('MARY', 'SMITH')
```

SQL Server (확장 필요):

```sql
select * from customer
where (first_name = 'MARY' and last_name = 'SMITH')
```

Oracle (고유 구문):

```sql
select * from customer
where (first_name, last_name) = (('MARY', 'SMITH'))
```

IN 술어 예제:

```sql
SELECT *
FROM customer
WHERE (first_name, last_name) IN (
  SELECT first_name, last_name
  FROM actor
)
```

PostgreSQL/MySQL/MariaDB:

```sql
select * from customer where (first_name, last_name) in (
  select first_name, last_name from actor
)
```

SQL Server (EXISTS 변환):

```sql
select * from customer where exists (
  select x.c1, x.c2
  from (select first_name, last_name from actor) x(c1, c2)
  where (first_name = x.c1 and last_name = x.c2)
)
```

비동등 술어 예제:

```sql
SELECT *
FROM customer
WHERE (first_name, last_name)
    > ('JENNIFER', 'DAVIS')
```

PostgreSQL:

```sql
select * from customer
where (first_name, last_name) > ('JENNIFER', 'DAVIS')
```

Oracle (확장된 논리):

```sql
select * from customer where (
  first_name >= 'JENNIFER' and (
    first_name > 'JENNIFER' or (
      first_name = 'JENNIFER' and last_name > 'DAVIS'
    )
  )
)
```

## 8. DISTINCT 술어

IS DISTINCT FROM 술어는 표준 동등 비교와 다르게 NULL 비교를 처리합니다.

PostgreSQL 진리표 예제:

```sql
WITH t(v) AS (
  VALUES (1),(2),(null)
)
SELECT v1, v2, v1 IS DISTINCT FROM v2
FROM t t1(v1), t t2(v2)
```

결과 (NULL 처리 표시):

```
v1  v2  d
-----------------
1   1   false
1   2   true
1       true
2   1   true
2   2   false
2       true
    1   true
    2   true
        false
```

에뮬레이션할 쿼리:

```sql
SELECT *
FROM customer
WHERE (first_name, last_name)
  IS DISTINCT FROM ('JENNIFER', 'DAVIS')
```

PostgreSQL (네이티브):

```sql
select * from customer where (first_name, last_name)
                is distinct from ('JENNIFER', 'DAVIS')
```

MySQL (비표준 연산자):

```sql
select * from customer where (not((first_name, last_name)
                                 <=> ('JENNIFER', 'DAVIS')))
```

SQLite (비표준 연산자):

```sql
select * from customer where ((first_name, last_name)
                       is not ('JENNIFER', 'DAVIS'))
```

SQL Server (INTERSECT 접근법):

```sql
select * from customer where not exists (
  (select first_name, last_name)
   intersect
  (select 'JENNIFER', 'DAVIS')
)
```

대안적 진리표 (INTERSECT 사용):

```sql
WITH t(v) AS (
  VALUES (1),(2),(null)
)
SELECT v1, v2, NOT EXISTS (
  SELECT v1 INTERSECT SELECT v2
)
FROM t t1(v1), t t2(v2)
```

## 9. DDL 문

데이터 정의 언어 에뮬레이션은 벤더에 구애받지 않는 마이그레이션 스크립트를 가능하게 합니다.

테이블 구조 복사 (데이터 없이):

```sql
CREATE TABLE x AS
SELECT 1 AS one
WITH NO DATA
```

PostgreSQL:

```sql
create table x as select 1 as one with no data
```

Oracle (WITH NO DATA 지원 없음):

```sql
create table x as select 1 one from dual where 1 = 0
```

DB2:

```sql
create table x as (select 1 one from "SYSIBM"."DUAL")
with no data
```

SQL Server (벤더별 구문):

```sql
select 1 one into x where 1 = 0
```

테이블 구조 복사 (데이터 포함):

```sql
CREATE TABLE x AS
SELECT 1 AS one
WITH DATA
```

Oracle:

```sql
create table x as select 1 one from dual
```

PostgreSQL:

```sql
create table x as select 1 as one with data
```

SQL Server:

```sql
select 1 one into x
```

DB2 (여러 문 필요):

```sql
begin
  execute immediate '
    create table x as (select 1 one from "SYSIBM"."DUAL")
    with no data
  ';
  execute immediate '
    insert into x select 1 one from "SYSIBM"."DUAL"
  ';
end
```

주요 관찰 사항: DB2는 단일 문으로 테이블 구조와 함께 데이터를 복사할 수 없습니다.

## 10. 내장 함수

함수는 데이터베이스마다 크게 다르므로 이식성을 위해 에뮬레이션이 필요합니다.

LPAD 예제:

```sql
SELECT lpad('abc', ' ', 5)
```

네이티브 지원 (대부분의 데이터베이스):

CUBRID, DB2, Derby, Firebird, H2, HANA, HSQLDB, Informix, Ingres, MariaDB, MySQL, Oracle, PostgreSQL, Redshift, Vertica 모두 `lpad('abc', ' ', 5)`를 지원합니다.

SQL Server:

```sql
(replicate(5, (' ' - len('abc'))) + 'abc')
```

Sybase:

```sql
(repeat(5, (' ' - length('abc'))) || 'abc')
```

ASE:

```sql
(replicate(5, (' ' - char_length('abc'))) || 'abc')
```

Access:

```sql
replace(space(' ' - len('abc')), ' ', 5) & 'abc'
```

SQLite (복잡한 우회 방법):

```sql
substr(replace(replace(substr(quote(zeroblob(((' ' - length('abc') - 1 + length("5")) / length("5") + 1) / 2)), 3), '''', ''), '0', "5"), 1, (' ' - length('abc'))) || 'abc'
```

## 결론

jOOQ는 SQL을 타입 안전한 내장형 Java DSL로 표준화합니다. 버전 3.9 이상부터는 https://www.jooq.org/translate 에서 공개적으로 사용 가능한 파서를 통해 방언 차이를 시각화할 수 있습니다. 이 목록은 50개 이상의 항목으로 확장될 수 있지만, 직접 실험해보는 것을 권장합니다. 이슈와 기능 요청은 https://github.com/jOOQ/jOOQ/issues/new 에 보고해 주세요. 저자는 향후 Flyway와 같은 도구와의 통합을 통해 마이그레이션 스크립트에서 벤더에 구애받지 않는 표준화된 SQL을 사용할 수 있게 될 것을 제안합니다.
