# 상관 서브쿼리는 악하고 느리다. 정말 그런가?

> 원문: https://blog.jooq.org/correlated-subqueries-are-evil-and-slow-or-are-they/

상관 서브쿼리는 "악하고 느리다"는 것이 SQL에서 널리 알려진 통념이다. 정말 그럴까? 다음 쿼리를 살펴보자:

```sql
SELECT
  first_name, last_name,
  (SELECT count(*)
   FROM film_actor fa
   WHERE fa.actor_id = a.actor_id)
FROM actor a
```

이 쿼리는 각 배우(actor)에 대해 해당 배우가 출연한 영화의 수를 세는 상관 서브쿼리를 사용한다.

## 상관 서브쿼리의 개념적 동작 방식

상관 서브쿼리는 개념적으로 중첩 루프(nested loop)로 실행된다고 볼 수 있다:

```java
for (Actor a : actor) {
  output(a.first_name, a.last_name,
    film_actor.where(fa -> fa.actor_id == a.actor_id).size()
  )
}
```

외부 쿼리의 각 행에 대해, 내부 쿼리가 실행되어 해당하는 film_actor 레코드의 수를 센다.

## 일반적인 대안: 벌크 집계(Bulk Aggregation)

통상적인 지혜에 따르면, 이러한 중첩 쿼리 대신 JOIN과 GROUP BY를 사용한 벌크 집계가 더 성능이 좋다고 한다:

```sql
SELECT
  first_name, last_name, count(fa.actor_id)
FROM actor a
LEFT JOIN film_actor fa USING (actor_id)
GROUP BY actor_id, first_name, last_name
```

이 접근법은 모든 데이터를 한 번에 조인한 후 그룹화하여 집계한다.

## 알고리즘 복잡도 분석

두 접근법의 알고리즘 복잡도를 분석해보자. M을 배우 수, N을 film_actor 레코드 수라고 하면:

벌크 집계(GROUP BY 방식)의 복잡도: O(M + N)
- 모든 actor 테이블을 스캔하고 (M)
- 모든 film_actor 테이블을 스캔한 후 (N)
- 해시 조인과 해시 그룹화를 수행

상관 서브쿼리(중첩 선택)의 복잡도: O(M log N)
- 각 배우에 대해 (M번)
- 인덱스 범위 스캔을 통해 해당 레코드를 찾음 (log N)

흥미롭게도, 데이터셋이 작거나 중간 크기일 때 O(M log N)이 O(M + N)보다 더 효율적일 수 있다. 이는 인덱스 기반 접근이 메모리 소비가 적고 캐시 효율성이 높기 때문이다.

## Oracle 데이터베이스에서의 성능 테스트

Sakila 샘플 데이터베이스(200명의 배우, 5,462개의 film_actor 레코드)를 사용하여 Oracle에서 1,000번의 반복 테스트를 수행했다.

### 상관 서브쿼리(중첩 선택) 결과:

```
Run 1: 00:00:01.122000000
Run 2: 00:00:01.116000000
Run 3: 00:00:01.122000000
```

### 벌크 GROUP BY 결과:

```
Run 1: 00:00:03.191000000
Run 2: 00:00:03.104000000
Run 3: 00:00:03.228000000
```

놀랍게도 상관 서브쿼리가 약 3배 더 빠른 성능을 보였다!

## 실행 계획 비교

### 상관 서브쿼리의 실행 계획:

```
SORT AGGREGATE (200회 실행)
  INDEX RANGE SCAN (200번 시작, 5462개 행)
TABLE ACCESS FULL (1번 시작, 200개 행)
```

이 계획에서는 actor 테이블을 한 번 전체 스캔한 후, 각 배우에 대해 인덱스 범위 스캔을 수행한다.

### 벌크 GROUP BY의 실행 계획:

```
HASH GROUP BY
  HASH JOIN (OUTER)
    TABLE ACCESS FULL (200개 행)
    INDEX FAST FULL SCAN (5462개 행)
```

이 계획에서는 두 테이블을 모두 스캔한 후 해시 조인과 해시 그룹화를 수행한다.

## 최적화 전략: 파생 테이블(Derived Table) 접근법

세 번째 접근법으로, 파생 테이블을 사용한 서브쿼리 팩토링이 있다:

```sql
SELECT
  first_name, last_name, c
FROM actor a
JOIN (
  SELECT actor_id, count(*) c
  FROM film_actor
  GROUP BY actor_id
) USING (actor_id)
```

이 방식은 먼저 film_actor를 집계한 후 결과를 actor와 조인한다.

### 파생 테이블 접근법의 성능 결과:

```
Run 1: 00:00:01.592000000
Run 2: 00:00:01.595000000
```

이 접근법은 상관 서브쿼리와 거의 동등한 성능을 보였으며, 전통적인 벌크 집계보다 약 1.7배 빠른 결과를 보여주었다.

## 핵심 통찰

1. 데이터 볼륨 의존성: 쿼리 최적화는 데이터의 양과 분포에 크게 의존한다.

2. 옵티마이저 동작: 데이터베이스 옵티마이저는 의미적으로 동등한 쿼리를 다르게 처리할 수 있다.

3. 소규모 데이터셋: 작거나 중간 크기의 데이터셋에서는 인덱스 기반 중첩 접근법이 유리하다.

4. 대규모 데이터셋: 더 큰 데이터셋에서는 일반적으로 벌크 집계 전략이 유리하다.

5. 파생 테이블 최적화: 파생 테이블을 사용하면 두 접근법 간의 성능 격차를 줄일 수 있다.

## 결론

이 글의 핵심 메시지는 경험적 측정이 이론적 가정보다 중요하다는 것이다. 논리적으로 동등한 쿼리 구조 간의 성능 차이는 각 데이터베이스 시스템과 데이터셋 특성에 따른 구현 세부사항을 반영한다.

"어떤 초기 판단도 믿지 마라. 측정하라."

상관 서브쿼리가 항상 느린 것은 아니다. 때로는 놀랍게도 더 빠를 수 있다. 중요한 것은 자신의 환경에서 실제로 테스트해보고, 데이터와 쿼리 패턴에 맞는 최적의 접근법을 선택하는 것이다.
