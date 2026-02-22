# SQL의 JOIN .. ON 절과 WHERE 절의 차이

> 원문: https://blog.jooq.org/the-difference-between-sqls-join-on-clause-and-the-where-clause/

## 소개

SQL 교육 참가자들로부터 자주 받는 질문 중 하나는 `JOIN .. ON` 절에 있는 조건절(predicate)과 `WHERE` 절에 있는 조건절 사이의 실질적인 차이에 관한 것입니다. 처음에는 기능적으로 동일해 보일 수 있지만, 외부 조인(outer join) 시나리오에서는 상당한 차이가 발생합니다.

## SQL 코드 예제

첫 번째 쿼리 (WHERE 절 사용):
```sql
SELECT a.actor_id, a.first_name, a.last_name, count(fa.film_id)
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
WHERE fa.film_id < 10
GROUP BY a.actor_id, a.first_name, a.last_name
ORDER BY count(fa.film_id) DESC;
```

두 번째 쿼리 (ON 절 사용):
```sql
SELECT a.actor_id, a.first_name, a.last_name, count(fa.film_id)
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
  AND fa.film_id < 10
GROUP BY a.actor_id, a.first_name, a.last_name
ORDER BY count(fa.film_id) DESC;
```

결과 (INNER JOIN의 경우 동일):
```
ACTOR_ID  FIRST_NAME  LAST_NAME  COUNT
108       WARREN      NOLTE      3
162       OPRAH       KILMER     3
19        BOB         FAWCETT    2
10        CHRISTIAN   GABLE      2
53        MENA        TEMPLE     2
137       MORGAN      WILLIAMS   1
2         NICK        WAHLBERG   1
```

## LEFT JOIN 비교

첫 번째 LEFT JOIN 쿼리 (WHERE 절 사용):
```sql
SELECT a.actor_id, a.first_name, a.last_name, count(fa.film_id)
FROM actor a
LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id
WHERE fa.film_id < 10
GROUP BY a.actor_id, a.first_name, a.last_name
ORDER BY count(fa.film_id) ASC;
```

결과 (5개 행):
```
ACTOR_ID  FIRST_NAME  LAST_NAME  COUNT
194       MERYL       ALLEN      1
198       MARY        KEITEL     1
30        SANDRA      PECK       1
85        MINNIE      ZELLWEGER  1
123       JULIANNE    DENCH      1
```

두 번째 LEFT JOIN 쿼리 (ON 절 사용):
```sql
SELECT a.actor_id, a.first_name, a.last_name, count(fa.film_id)
FROM actor a
LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id
  AND fa.film_id < 10
GROUP BY a.actor_id, a.first_name, a.last_name
ORDER BY count(fa.film_id) ASC;
```

결과 (영화가 없는 배우도 포함):
```
ACTOR_ID  FIRST_NAME     LAST_NAME      COUNT
3         ED             CHASE          0
4         JENNIFER       DAVIS          0
5         JOHNNY         LOLLOBRIGIDA   0
6         BETTE          NICHOLSON      0
...
1         PENELOPE       GUINESS        1
200       THORA          TEMPLE         1
2         NICK           WAHLBERG       1
198       MARY           KEITEL         1
```

## 핵심 설명

INNER JOIN의 경우, 조건절의 위치는 논리적으로 동등합니다. 필터링은 절의 위치에 관계없이 일치하지 않는 행을 제거합니다. LEFT JOIN의 경우에는 위치가 근본적으로 중요합니다:

- ON 절: 일치하는 행을 결정합니다. 일치하지 않는 왼쪽 테이블의 행도 여전히 나타납니다 (NULL 값과 함께)
- WHERE 절: 최종 결과 집합을 필터링하여, 일치하지 않는 행들을 제거합니다

"WHERE 절은 조인 완료 후 필터를 만족하지 않는 행을 다시 제거합니다", 반면 "일치하는 행은 ON 절에 의해 결정됩니다."

## 결론

조건절은 논리적으로 적합한 위치에 배치하세요: 조인과 관련된 조건은 `ON` 절에 속하고, 전체 결과 집합에 적용되는 필터는 `WHERE` 절에 속합니다. 이러한 접근 방식은 내부 조인과 외부 조인 모두에서 정확성을 보장하고, 외부 조인 시나리오에서 의도치 않은 데이터 제거를 방지합니다.
