# 뭐가 더 빠를까? COUNT(*) vs COUNT(1)?

> 원문: https://blog.jooq.org/whats-faster-count-or-count1/

하나의 COUNT 구문이 다른 것보다 빠르다는 것은 "불사의 미신"입니다. 사실 하나가 다른 것보다 빨라야 할 이유가 전혀 없습니다. 하지만 정말로 그럴까요? 이 글에서 이 미신을 파헤쳐 보겠습니다.

## COUNT(...)는 어떻게 작동하는가?

먼저 COUNT 함수가 실제로 어떻게 작동하는지 살펴봅시다.

### 표준 집계 함수

집계 함수인 COUNT(...)의 두 가지 형태를 구분해야 합니다:

- `COUNT(*)` 는 그룹 내의 모든 튜플 수를 셉니다
- `COUNT(<expr>)` 는 표현식이 NULL이 아닌 경우의 튜플 수만 셉니다

이 차이가 왜 중요한지 실제 예제로 살펴보겠습니다.

### 실용적인 예제: 외부 조인

Sakila 데이터베이스에서 영화가 없는 배우를 추가해 봅시다:

```sql
INSERT INTO actor (actor_id, first_name, last_name)
VALUES (201, 'SUSAN', 'DAVIS');
```

그런 다음 INNER JOIN을 사용하여 각 배우가 출연한 영화 수를 세어봅시다:

```sql
SELECT actor_id, a.first_name, a.last_name, count(*) AS c
FROM actor AS a
JOIN film_actor AS fa USING (actor_id)
JOIN film AS f USING (film_id)
GROUP BY actor_id
ORDER BY c ASC, actor_id ASC;
```

결과:

```
actor_id|first_name |last_name   |c |
--------|-----------|------------|--|
     148|EMILY      |DEE         |14|
      35|JUDY       |DEAN        |15|
     199|JULIA      |FAWCETT     |15|
     186|JULIA      |ZELLWEGER   |16|
...
```

INNER JOIN을 사용했기 때문에 새로 추가된 SUSAN DAVIS는 보이지 않습니다. 그녀는 어떤 영화에도 출연하지 않았기 때문입니다.

이제 LEFT JOIN을 사용해 봅시다:

```sql
SELECT actor_id, a.first_name, a.last_name, count(*) AS c
FROM actor AS a
LEFT JOIN film_actor AS fa USING (actor_id)
LEFT JOIN film AS f USING (film_id)
GROUP BY actor_id
ORDER BY c ASC, actor_id ASC;
```

결과:

```
actor_id|first_name |last_name   |c |
--------|-----------|------------|--|
     201|SUSAN      |DAVIS       | 1|
     148|EMILY      |DEE         |14|
      35|JUDY       |DEAN        |15|
...
```

이제 SUSAN DAVIS가 나타나지만, 영화 수가 0이 아니라 1입니다! 이것은 잘못된 결과입니다. LEFT JOIN은 매칭되는 행이 없을 때 NULL 튜플을 생성하고, `COUNT(*)`는 이 NULL 튜플도 세기 때문입니다.

올바른 해결책은 `COUNT(film_id)`를 사용하는 것입니다:

```sql
SELECT actor_id, a.first_name, a.last_name, count(film_id) AS c
FROM actor AS a
LEFT JOIN film_actor AS fa USING (actor_id)
LEFT JOIN film AS f USING (film_id)
GROUP BY actor_id
ORDER BY c ASC, actor_id ASC;
```

결과:

```
actor_id|first_name |last_name   |c |
--------|-----------|------------|--|
     201|SUSAN      |DAVIS       | 0|
     148|EMILY      |DEE         |14|
      35|JUDY       |DEAN        |15|
...
```

이제 SUSAN DAVIS의 영화 수가 올바르게 0으로 표시됩니다. `film_id`는 기본 키이므로 절대 NULL이 될 수 없습니다. 따라서 LEFT JOIN에서 생성된 NULL 튜플의 film_id도 NULL이고, `COUNT(film_id)`는 이를 세지 않습니다.

### 부분 집합 세기

`COUNT(<expr>)`의 또 다른 유용한 활용법은 조건부 카운팅입니다:

```sql
SELECT
  count(*),
  count(CASE WHEN first_name LIKE 'A%' THEN 1 END),
  count(CASE WHEN first_name LIKE '%A' THEN 1 END),
  count(CASE WHEN first_name LIKE '%A%' THEN 1 END)
FROM actor;
```

결과:

```
count|count|count|count|
-----|-----|-----|-----|
  201|   13|   30|   105|
```

이 쿼리는:
- 전체 배우 수 (201명)
- 이름이 'A'로 시작하는 배우 수 (13명)
- 이름이 'A'로 끝나는 배우 수 (30명)
- 이름에 'A'가 포함된 배우 수 (105명)

을 한 번의 쿼리로 계산합니다. CASE 표현식이 ELSE 절 없이 조건을 만족하지 않으면 NULL을 반환하고, `COUNT(<expr>)`는 NULL을 세지 않기 때문에 이 방법이 작동합니다.

PostgreSQL에서는 SQL 표준의 `FILTER` 절을 사용하여 더 깔끔하게 작성할 수 있습니다:

```sql
SELECT
  count(*),
  count(*) FILTER (WHERE first_name LIKE 'A%'),
  count(*) FILTER (WHERE first_name LIKE '%A'),
  count(*) FILTER (WHERE first_name LIKE '%A%')
FROM actor;
```

## 다시 COUNT(*) vs COUNT(1)로

이론적으로, `COUNT(*)`와 `COUNT(1)` 사이에는 성능 차이가 있어서는 안 됩니다. 왜냐하면:

- `1`은 상수이며 절대 NULL로 평가되지 않습니다
- 따라서 `COUNT(1)`은 `COUNT(*)`와 동일한 결과를 반환해야 합니다
- 옵티마이저는 이를 인식하고 동일하게 처리해야 합니다

하지만 정말 그럴까요? 벤치마크를 통해 확인해 봅시다.

## 벤치마크 설정

벤치마크는 다음 환경에서 수행되었습니다:

- 테이블에 100만 개의 행
- 각 쿼리를 100번 반복
- 5회의 벤치마크 실행

테스트한 데이터베이스:
- MySQL 8.0.16
- Oracle 18c XE
- PostgreSQL 11.3
- SQL Server 2017 Express

두 개의 문장을 비교했습니다:
- Statement 1: `SELECT COUNT(*) FROM t`
- Statement 2: `SELECT COUNT(1) FROM t`

## 벤치마크 결과

### MySQL

MySQL에서는 의미 있는 차이가 없었습니다.

| 실행 | Statement 1 | Statement 2 |
|------|-------------|-------------|
| RUN 1 | 1.0049 | 1.0618 |
| RUN 2 | 1.0227 | 1.0009 |
| RUN 3 | 1.0095 | 1.0558 |
| RUN 4 | 1.0162 | 1.0364 |
| RUN 5 | 1.0092 | 1.0511 |

결과가 무작위로 변동하며, 어느 쪽이 더 빠르다고 할 수 없습니다.

### Oracle

Oracle에서도 측정 가능한 차이가 없었습니다.

| 실행 | Statement 1 | Statement 2 |
|------|-------------|-------------|
| RUN 1 | 1.01609 | 1.09175 |
| RUN 2 | 1.01055 | 1.02643 |
| RUN 3 | 1.01203 | 1.00308 |
| RUN 4 | 1.00589 | 1.07473 |
| RUN 5 | 1.0125 | 1.05336 |

마찬가지로 무작위 변동입니다.

### PostgreSQL

PostgreSQL에서 놀라운 결과가 나왔습니다. `COUNT(*)`가 일관되게 약 10% 더 빠릅니다:

| 실행 | Statement 1 | Statement 2 |
|------|-------------|-------------|
| RUN 1 | 1.00134 | 1.09538 |
| RUN 2 | 1.00190 | 1.09115 |
| RUN 3 | 1.00000 | 1.09858 |
| RUN 4 | 1.00266 | 1.09260 |
| RUN 5 | 1.00454 | 1.09694 |

5번의 실행 모두에서 `COUNT(1)`이 `COUNT(*)`보다 약 9-10% 느렸습니다. 이것은 통계적으로 유의미한 차이입니다.

### SQL Server

SQL Server에서는 관련 있는 차이가 관찰되지 않았습니다.

| 실행 | Statement 1 | Statement 2 |
|------|-------------|-------------|
| RUN 1 | 1.00000 | 1.00390 |
| RUN 2 | 1.00390 | 1.00000 |
| RUN 3 | 1.00000 | 1.00780 |
| RUN 4 | 1.00390 | 1.00000 |
| RUN 5 | 1.00000 | 1.00390 |

## 왜 PostgreSQL에서 차이가 발생하는가?

Twitter 사용자 Vik Fearing이 지적했듯이, PostgreSQL은 `COUNT(<expr>)`를 처리할 때 각 행에서 표현식이 NULL인지 확인하는 루프를 실행합니다. PostgreSQL 소스 코드를 보면, `COUNT(1)`에서도 이 NULL 체크가 매 행마다 수행됩니다.

이것은 다른 데이터베이스들과 달리 PostgreSQL의 옵티마이저가 상수 표현식에 대한 null 체크 오버헤드를 제거하지 않는다는 것을 의미합니다. 이는 SQL의 근본적인 진리라기보다는 구현 세부사항입니다.

## 결론

벤치마크 결과는 다음을 보여줍니다:

1. MySQL, Oracle, SQL Server: `COUNT(*)`와 `COUNT(1)` 사이에 의미 있는 성능 차이가 없습니다.

2. PostgreSQL: `COUNT(*)`가 `COUNT(1)`보다 약 10% 빠릅니다.

따라서, 모든 측정된 데이터베이스 제품에서 `COUNT(1)` 대신 `COUNT(*)`를 일관되게 사용하는 것이 약간 더 나은 선택입니다.

몇 가지 주의사항:
- 이 벤치마크는 단순한 쿼리만 테스트했습니다
- 조인, 유니온, 윈도우 함수가 포함된 복잡한 쿼리에서는 결과가 다를 수 있습니다
- 미래의 PostgreSQL 버전에서는 이 최적화가 적용되어 차이가 없어질 수 있습니다

하지만 현재로서는 `COUNT(*)`를 사용하는 것이 모든 데이터베이스에서 안전한 선택이며, 적어도 PostgreSQL에서는 약간의 성능 이점을 얻을 수 있습니다.
