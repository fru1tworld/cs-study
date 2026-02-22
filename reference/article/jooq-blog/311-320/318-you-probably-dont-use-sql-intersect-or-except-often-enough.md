# SQL INTERSECT나 EXCEPT를 아마 자주 사용하지 않을 것이다

> 원문: https://blog.jooq.org/you-probably-dont-use-sql-intersect-or-except-often-enough/

모든 사람이 SQL의 집합 연산(set operation)에 대해 어느 정도 알고 있다. 적어도 `UNION`과 `UNION ALL`에 대해서는 알고 있을 것이다. 그리고 (명시적이든 암묵적이든) 다양한 유형의 `JOIN`에 대해서도 익숙할 것이다. 하지만 `INTERSECT`와 `EXCEPT`는 어떤가?

## 벤 다이어그램으로 JOIN을 설명하지 말자

아마도 다음과 같은 그 유명한 벤 다이어그램을 본 적이 있을 것이다:

이러한 벤 다이어그램은 `JOIN`이 아닌 집합 연산을 시각화하는 데 훨씬 더 적합하다. `JOIN`은 두 집합의 요소를 결합하는 데카르트 곱(Cartesian product)이다. 집합 A와 B의 각 요소를 결합하여 새로운 집합을 만들기 때문이다. 반면 집합 연산은 진정한 집합 이론의 논리를 따른다.

## 세 가지 주요 집합 연산

SQL에는 세 가지 주요 집합 연산이 있다:

### UNION

`UNION`은 두 결과 집합을 연결하고 중복을 제거한다. `UNION ALL`은 중복을 유지한다. Sakila 데이터베이스를 사용하여 고객과 직원의 이름을 단일 목록으로 결합하는 예를 살펴보자:

```sql
SELECT first_name, last_name
FROM customer
UNION
SELECT first_name, last_name
FROM staff
```

### INTERSECT

`INTERSECT`는 두 집합 모두에 존재하는 행만 반환한다. 고객이면서 동시에 배우인 사람을 찾으려면 다음과 같이 작성할 수 있다:

```sql
SELECT first_name, last_name
FROM customer
INTERSECT
SELECT first_name, last_name
FROM actor
```

이 쿼리는 Sakila 데이터베이스에서 한 가지 일치하는 이름을 반환한다: Jennifer Davis.

### EXCEPT

`EXCEPT`는 첫 번째 집합에서 두 번째 집합에 존재하는 행을 제거한다. 직원이 아닌 고객을 찾으려면:

```sql
SELECT first_name, last_name
FROM customer
EXCEPT
SELECT first_name, last_name
FROM staff
```

## JOIN을 사용한 대안적 작성 방법

물론 이러한 집합 연산은 `JOIN`을 사용하여 동일한 결과를 얻을 수 있다. 하지만 코드가 더 복잡해진다.

### INTERSECT의 JOIN 대안

`INTERSECT` 연산은 `INNER JOIN`이나 세미 조인 패턴(`IN` 또는 `EXISTS` 사용)으로 재현할 수 있다:

```sql
-- INNER JOIN 사용
SELECT DISTINCT c.first_name, c.last_name
FROM customer c
INNER JOIN actor a
  ON c.first_name = a.first_name
  AND c.last_name = a.last_name

-- EXISTS 사용 (세미 조인)
SELECT DISTINCT c.first_name, c.last_name
FROM customer c
WHERE EXISTS (
  SELECT 1 FROM actor a
  WHERE c.first_name = a.first_name
  AND c.last_name = a.last_name
)

-- IN 사용
SELECT DISTINCT first_name, last_name
FROM customer
WHERE (first_name, last_name) IN (
  SELECT first_name, last_name FROM actor
)
```

### EXCEPT의 JOIN 대안

`EXCEPT` 연산은 `LEFT JOIN`과 `NULL` 검사 또는 안티 조인 패턴으로 재현할 수 있다:

```sql
-- LEFT JOIN과 IS NULL 사용
SELECT c.first_name, c.last_name
FROM customer c
LEFT JOIN staff s
  ON c.first_name = s.first_name
  AND c.last_name = s.last_name
WHERE s.first_name IS NULL

-- NOT EXISTS 사용 (안티 조인)
SELECT first_name, last_name
FROM customer c
WHERE NOT EXISTS (
  SELECT 1 FROM staff s
  WHERE c.first_name = s.first_name
  AND c.last_name = s.last_name
)

-- NOT IN 사용
SELECT first_name, last_name
FROM customer
WHERE (first_name, last_name) NOT IN (
  SELECT first_name, last_name FROM staff
)
```

## 가독성의 이점

집합 연산의 가장 큰 장점은 가독성이다. 의도를 훨씬 더 명확하게 전달한다.

"직원이 아닌 고객을 찾아라"라는 요구사항을 생각해보자:

- `EXCEPT` 사용: 쿼리가 의도를 직접적으로 표현한다. "고객에서 직원을 제외하라."
- `LEFT JOIN`과 `IS NULL` 사용: 여러 절이 필요하고 기본 논리가 모호해진다. "고객과 직원을 조인하고, 직원 측이 NULL인 것을 찾아라." 이것이 왜 "직원이 아닌 고객"을 의미하는지 이해하려면 추가적인 사고가 필요하다.

마찬가지로 "고객이면서 배우인 사람"을 찾을 때:

- `INTERSECT` 사용: "고객과 배우의 교집합을 구하라."
- `INNER JOIN` 사용: "고객과 배우를 조인하고 중복을 제거하라."

## 성능 고려사항

성능은 데이터베이스 벤더에 따라 다르다. 일부 경우, 특히 SQL Server에서 무거운 쿼리의 경우 `JOIN`이 집합 연산보다 더 좋은 성능을 보일 수 있다. 그러나 많은 경우 최적화 엔진이 이러한 쿼리를 동등하게 처리한다.

가장 좋은 방법은 실제 프로덕션 환경에서 테스트하는 것이다. 실행 계획을 확인하고 특정 사용 사례에 대해 어떤 접근 방식이 더 나은지 결정하라.

## 결론

`INTERSECT`와 `EXCEPT`는 SQL 표준에서 강력하지만 과소평가된 도구이다. 많은 개발자들이 익숙함 때문에 `JOIN` 기반 대안을 선택하지만, 집합 연산은 종종 더 깔끔하고 읽기 쉬운 코드를 제공한다.

다음에 "A에는 있지만 B에는 없는 것"을 찾거나 "A와 B 모두에 있는 것"을 찾아야 할 때, `EXCEPT`와 `INTERSECT`를 먼저 고려해보라. 더 명확한 의도 표현과 종종 동등한 성능을 제공할 것이다.
