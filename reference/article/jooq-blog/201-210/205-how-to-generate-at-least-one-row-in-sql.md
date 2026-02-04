> 원문: https://blog.jooq.org/how-to-generate-at-least-one-row-in-sql/

# SQL에서 최소 한 개의 행을 생성하는 방법

이 글에서는 SQL 쿼리가 빈 결과 집합을 반환할 때도 최소 한 개의 행을 반환하도록 보장하는 기법을 탐구합니다.

## 문제 상황

LEFT JOIN을 사용하여 영화가 없는 배우를 찾을 때, 일치하는 배우가 없으면 빈 결과가 발생합니다:

```sql
SELECT actor_id, first_name, last_name, title
FROM actor
LEFT JOIN film_actor USING (actor_id)
LEFT JOIN film USING (film_id)
WHERE first_name = 'SUSANNE'
```

SUSANNE이라는 이름의 배우가 존재하지 않으면, 쿼리는 하나의 빈 행 대신 0개의 행을 반환합니다.

## 주요 해결책

원본 쿼리를 더미 단일 행 테이블과 LEFT JOIN으로 감쌉니다:

```sql
SELECT actor_id, first_name, last_name, title
FROM (SELECT 1 a) a
LEFT JOIN (
  SELECT *
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  LEFT JOIN film USING (film_id)
  WHERE first_name = 'SUSANNE'
) b ON 1 = 1
ORDER BY 1, 2, 3
```

## 중요한 고려사항

- WHERE 절은 반드시 파생 테이블(Derived Table) 내부에 위치해야 합니다. 그렇지 않으면 A에서 해당 단일 행이 다시 제거됩니다.
- ON 절은 구문상 조건(TRUE 또는 1=1)이 필요합니다.
- 결과에 추가 더미 컬럼이 나타납니다.

## 대안: OUTER APPLY

SQL Server와 Oracle 12c 사용자는 OUTER APPLY를 활용하여 명시적인 ON 절 없이 더 깔끔한 구문을 사용할 수 있습니다.

## 고급 기법

PostgreSQL은 "dee" 테이블(컬럼이 없는 단일 행 테이블)을 사용하는 우아한 접근 방식을 제공하여, 더미 컬럼을 캡처하지 않고도 `SELECT *`를 사용할 수 있습니다.
