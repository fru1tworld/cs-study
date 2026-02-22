# PostgreSQL에서 하나를 제외한 모든 컬럼 선택하기

> 원문: https://blog.jooq.org/selecting-all-columns-except-one-in-postgresql/

## 문제 상황

여러 테이블을 프로젝션하는 쿼리를 작성할 때, 불필요한 컬럼이 포함되는 경우가 있습니다. 예를 들어, 다음 쿼리는 각 배우가 출연한 영화 중 가장 긴 영화를 찾습니다:

```sql
SELECT *
FROM (
  SELECT
    a.*,
    f.*,
    RANK() OVER (PARTITION BY actor_id ORDER BY length DESC) rk
  FROM film f
  JOIN film_actor fa USING (film_id)
  JOIN actor a USING (actor_id)
) t
WHERE rk = 1
ORDER BY first_name, last_name
```

결과에 포함되는 `rk` 컬럼은 항상 1이며, 최종 출력에서는 아무런 의미가 없습니다. 이상적으로는 이 컬럼을 프로젝션에서 제외하고 싶을 것입니다.

## BigQuery의 해결책

"Google의 BigQuery에는 매우 흥미로운 SQL 언어 기능이 있습니다" - 바로 `EXCEPT` 키워드입니다: `SELECT * EXCEPT rk FROM (...) t WHERE rk = 1`

안타깝게도 대부분의 인기 있는 SQL 데이터베이스에는 이 문법이 없습니다.

## PostgreSQL 해결 방법: 중첩 레코드

PostgreSQL은 객체-관계형 기능을 지원하여, 테이블 전체를 레코드로 중첩할 수 있습니다:

```sql
SELECT (a).*, (f).*
FROM (
  SELECT
    a,
    f,
    RANK() OVER (PARTITION BY actor_id ORDER BY length DESC) rk
  FROM film f
  JOIN film_actor fa USING (film_id)
  JOIN actor a USING (actor_id)
) t
WHERE rk = 1
ORDER BY (a).first_name, (a).last_name;
```

핵심적인 차이점은: 컬럼을 확장하지 않고 완전한 테이블을 참조하고(`a.*` 대신 `a`), 외부 쿼리에서 `(a).first_name`과 같은 괄호 표기법을 사용하여 이를 펼치는 것입니다.

## 다른 데이터베이스를 위한 대안적 접근법

SQL Server는 `CROSS APPLY`와 `TOP 1 WITH TIES`를 사용합니다:

```sql
SELECT *
FROM actor a
CROSS APPLY (
  SELECT TOP 1 WITH TIES f.*
  FROM film f
  JOIN film_actor fa
    ON f.film_id = fa.film_id
    AND fa.actor_id = a.actor_id
  ORDER BY length DESC
) f
ORDER BY first_name, last_name;
```

유사한 기능을 지원하는 다른 데이터베이스로는 Snowflake(`SELECT * EXCLUDE`), Oracle, Informix 등이 있지만, 구현 세부 사항은 상당히 다릅니다.
