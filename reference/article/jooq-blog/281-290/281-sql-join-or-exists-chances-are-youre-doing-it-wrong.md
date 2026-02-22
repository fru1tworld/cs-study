# SQL JOIN vs EXISTS? 아마 잘못하고 있을 것이다

> 원문: https://blog.jooq.org/sql-join-or-exists-chances-are-youre-doing-it-wrong/

많은 개발자들이 JOIN과 SEMI JOIN의 차이점을 잘못 이해하고 있습니다. 이 글에서는 그 차이점과 올바른 사용법에 대해 알아보겠습니다.

## JOIN과 SEMI JOIN이란?

### (INNER) JOIN의 정의

`(INNER) JOIN`은 본질적으로 필터링된 카테시안 곱(filtered cartesian product)에 불과합니다. 관계 대수에서 카테시안 곱은 한 집합의 모든 요소를 다른 집합의 모든 요소와 결합합니다.

SQL로 표현하면 다음과 같습니다:

```sql
SELECT c.last_name, s.last_name
FROM customer AS c
INNER JOIN staff AS s ON c.last_name = s.last_name
```

### SEMI JOIN의 정의

SEMI JOIN(즉, "절반" 조인)은 관계 대수에서 JOIN 연산의 한쪽 결과만 필요할 때 사용됩니다. 예를 들어, 결과에서 고객 정보만 필요한 경우입니다.

자세히 살펴보면, LEFT SEMI JOIN은 R의 속성만 투영하며, 관계 대수가 (멀티셋이 아닌) 집합에 대한 것이므로 중복이 없습니다. 이것은 SEMI JOIN이 INNER JOIN이 생성하는 카테시안 곱을 생성하지 않는다는 것을 의미합니다. 둘은 정말로 같은 것이 아닙니다.

## INNER JOIN을 잘못 사용하는 경우의 문제점

JOIN은 테이블 간 행을 매칭하는 유용한 도구처럼 보일 수 있습니다. 그러나 한 테이블의 결과만 필요한데 일치 여부를 확인하기 위해 INNER JOIN을 사용하면 문제가 발생합니다.

예를 들어, 매칭되는 직원이 있는 고객을 찾기 위해 INNER JOIN을 선택하면, customer와 staff 사이에 카테시안 곱이 생성된 후 필터가 적용됩니다.

세 명의 고객과 세 명의 직원 데이터가 있을 때, 두 명의 고유한 고객 대신 네 개의 결과 행이 생성될 수 있습니다. 이로 인해 원치 않는 중복이 발생합니다.

### DISTINCT 사용의 문제점

이 중복을 제거하기 위해 `DISTINCT`를 사용하면 비효율적입니다:

- 의도치 않은 카테시안 곱이 디스크에서 너무 많은 레코드를 로드합니다
- 메모리에 너무 많은 레코드를 생성하고, 이것들을 다시 제거해야 합니다
- 해싱 대신 정렬을 통해 구현하는 일부 데이터베이스에서는 `DISTINCT`가 비용이 많이 들 수 있습니다
- `DISTINCT`는 SELECT 절의 의미를 변경할 수 있어, 좋지 않은 부작용을 초래할 수 있습니다
- 종종 중첩 서브쿼리가 필요하게 되어 성능이 더욱 저하됩니다

## 해결책: SEMI JOIN 사용하기

SQL에는 네이티브 `SEMI JOIN` 문법이 없으므로, 개발자는 대신 `EXISTS` 또는 `IN` 술어를 사용해야 합니다:

### EXISTS 사용

```sql
SELECT * FROM customer AS c
WHERE EXISTS (
  SELECT * FROM staff AS s
  WHERE c.last_name = s.last_name
)
```

### IN 사용

```sql
SELECT * FROM customer
WHERE last_name IN (
  SELECT last_name FROM staff
)
```

`IN`과 `EXISTS`는 정확히 동등한 "SEMI" JOIN 에뮬레이션입니다.

## 성능상의 이점

`EXISTS`나 `IN`을 사용하는 것이 더 나은 이유는 성능상의 이점도 있기 때문입니다. 데이터베이스는 일치하는 레코드를 하나라도 찾으면 즉시 검색을 중단할 수 있습니다.

즉, INNER JOIN이 불필요한 카테시안 곱을 생성하는 반면, SEMI JOIN을 사용하면 데이터베이스가 매칭되는 고객이 있는 첫 번째 직원을 만나자마자 검색을 중단할 수 있습니다.

## 핵심 요점

테이블 A와 테이블 B 사이에 일치하는 항목이 있는지 확인해야 하지만, 테이블 A의 결과만 필요한 경우, (INNER) JOIN이 아닌 SEMI JOIN(즉, EXISTS 또는 IN 술어)을 사용하세요.

이것은 정확성 측면에서 최적의 솔루션일 뿐만 아니라, 데이터베이스가 첫 번째 일치 항목을 찾자마자 검색을 중단할 수 있으므로 성능상의 이점도 있습니다.

## jOOQ에서의 지원

jOOQ 라이브러리는 Java 애플리케이션에서 더 깔끔한 코드 구현을 위해 네이티브 semi join과 anti join 연산자를 제공합니다. 이를 통해 SQL의 SEMI JOIN 의미론을 Java 코드에서 명시적으로 표현할 수 있습니다.

## 정리

| 상황 | 올바른 선택 |
|------|------------|
| 두 테이블의 데이터가 모두 필요한 경우 | INNER JOIN |
| 한 테이블의 데이터만 필요하고, 다른 테이블과의 일치 여부만 확인하는 경우 | EXISTS 또는 IN (SEMI JOIN) |
| 한 테이블의 데이터만 필요하고, 다른 테이블과 일치하지 않는 행을 찾는 경우 | NOT EXISTS 또는 NOT IN (ANTI JOIN) |

JOIN과 SEMI JOIN의 구분을 명확히 이해하고 적절히 사용하면, 더 정확하고 효율적인 SQL 쿼리를 작성할 수 있습니다.
