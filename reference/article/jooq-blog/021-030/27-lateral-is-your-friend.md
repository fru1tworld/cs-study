# LATERAL은 SQL에서 로컬 컬럼 변수를 만드는 당신의 친구입니다

> 원문: https://blog.jooq.org/lateral-is-your-friend-to-create-local-column-variables-in-sql/

SQL `WITH` 절은 개발자가 공통 테이블 표현식(CTE)을 사용 전에 선언할 수 있도록 하여 쿼리 구조화를 크게 개선했습니다. 하지만 `LATERAL` 키워드는 특정 쿼리를 더 읽기 쉽고 유지보수하기 쉽게 만들 수 있는 대안적인 접근 방식을 제공합니다.

## 전통적인 접근 방식: 중첩된 파생 테이블

원래 개발자들은 파생 테이블 안에 로직을 중첩해야 했습니다:

```sql
SELECT actor_id, name, COUNT(*)
FROM (
  SELECT actor_id, first_name || ' ' || last_name AS name
  FROM actor
) AS a
JOIN film_actor AS fa USING (actor_id)
GROUP BY actor_id, name
ORDER BY COUNT(*) DESC
LIMIT 5
```

## WITH 절 사용하기

`WITH` 절은 선언을 앞으로 이동시켜 가독성을 개선했습니다:

```sql
WITH a AS (
  SELECT actor_id, first_name || ' ' || last_name AS name
  FROM actor
)
SELECT actor_id, name, COUNT(*)
FROM a
JOIN film_actor AS fa USING (actor_id)
GROUP BY actor_id, name
ORDER BY COUNT(*) DESC
LIMIT 5;
```

두 접근 방식 모두 Sakila 데이터베이스에서 동일한 결과를 반환하며, 영화 출연 횟수 기준 상위 5명의 배우를 보여줍니다.

## LATERAL이 구원하러 왔습니다

SQL:1999 표준은 `<lateral derived table>`을 도입하여, `FROM` 절의 서브쿼리가 앞서 나온 객체를 참조할 수 있게 했습니다. 이 기능은 별도의 파생 테이블을 만들지 않고도 원본 테이블 바로 옆에 컬럼 별칭을 선언할 수 있게 해줍니다.

### 테이블 목록 요소로 사용하기

가장 간단한 접근 방식은 `LATERAL`을 테이블 목록 요소로 사용하는 것입니다:

```sql
SELECT actor_id, name, COUNT(*)
FROM
  actor JOIN film_actor AS fa USING (actor_id),
  LATERAL (SELECT first_name || ' ' || last_name AS name) AS t
GROUP BY actor_id, name
ORDER BY COUNT(*) DESC
LIMIT 5;
```

여러 개의 연쇄적인 `LATERAL` 절을 사용하여 여러 변수를 선언할 수 있습니다:

```sql
SELECT actor_id, name, name_length, COUNT(*)
FROM
  actor JOIN film_actor AS fa USING (actor_id),
  LATERAL (SELECT first_name || ' ' || last_name AS name) AS t1,
  LATERAL (SELECT length(name) AS name_length) AS t2
GROUP BY actor_id, name, name_length
ORDER BY COUNT(*) DESC
LIMIT 5;
```

이렇게 하면 계산된 이름 길이와 함께 배우 정보를 보여주는 결과가 생성됩니다.

### 조인 트리 요소로 사용하기

또 다른 방법으로, `CROSS JOIN`과 `LATERAL`을 함께 사용할 수 있습니다:

```sql
SELECT actor_id, name, COUNT(*)
FROM actor
CROSS JOIN LATERAL (SELECT first_name || ' ' || last_name AS name) AS t
JOIN film_actor AS fa USING (actor_id)
GROUP BY actor_id, name
ORDER BY COUNT(*) DESC
LIMIT 5;
```

여러 개의 연쇄 조인도 유사하게 작동합니다:

```sql
SELECT actor_id, name, name_length, COUNT(*)
FROM actor
CROSS JOIN LATERAL (SELECT first_name || ' ' || last_name AS name) AS t1
CROSS JOIN LATERAL (SELECT length(name) AS name_length) AS t2
JOIN film_actor AS fa USING (actor_id)
GROUP BY actor_id, name, name_length
ORDER BY COUNT(*) DESC
LIMIT 5;
```

## 접근 방식 비교

`WITH` 절 접근 방식은 다른 프로그래밍 언어와 유사하게 사용하기 전에 모든 것을 먼저 선언합니다. 하지만 중첩 구조에 대해 생각해야 합니다. `LATERAL` 접근 방식은 원본 테이블 바로 옆에 변수를 선언하므로, 수정되지 않은 원본 테이블을 쿼리 전체에서 계속 사용할 수 있어 리팩토링과 로직 추론이 간단해집니다.

`FROM` 절이 논리적으로 가장 먼저 실행되기 때문에, 거기서 선언된 모든 것은 `GROUP BY` 및 기타 절을 포함하여 쿼리 전체에서 사용할 수 있게 됩니다.

## T-SQL의 APPLY 사용하기

Oracle과 SQL Server는 `APPLY`라는 대안적인 구문을 제공합니다. 이 구문은 함수나 서브쿼리를 테이블에 명시적으로 적용하므로 일부 개발자들이 더 직관적이라고 느낍니다:

```sql
SELECT actor_id, name, name_length, COUNT(*)
FROM actor
CROSS APPLY (SELECT first_name || ' ' || last_name AS name FROM dual)
CROSS APPLY (SELECT length(name) AS name_length FROM dual)
JOIN film_actor USING (actor_id)
GROUP BY actor_id, name, name_length
ORDER BY COUNT(*) DESC
FETCH FIRST 5 ROWS ONLY;
```

## 데이터베이스별 지원 현황

이러한 기능에 대한 지원은 데이터베이스 시스템마다 다릅니다:

- Db2: `LATERAL`
- Firebird: `LATERAL`
- MySQL: `LATERAL`
- Oracle: `LATERAL`과 `APPLY` 모두 지원
- PostgreSQL: `LATERAL`
- Snowflake: `LATERAL`
- SQL Server: `APPLY`

jOOQ는 두 구문을 모두 지원하며 하나를 다른 것으로 에뮬레이션할 수 있어, 다양한 데이터베이스 플랫폼에서 유연성을 제공합니다.
