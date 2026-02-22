# SQL 쿼리 변환의 재미: 행 값 표현식을 사용한 술어

> 원문: https://blog.jooq.org/sql-query-transformation-fun-predicates-with-row-value-expressions/

행 값 표현식(Row Value Expression)은 SQL 표준의 매우 유용한 기능입니다. 이를 통해 여러 열을 동시에 비교하는 복잡한 술어를 작성할 수 있습니다. 하지만 안타깝게도 모든 데이터베이스가 이 기능을 완전히 지원하지는 않습니다.

## 행 값 표현식이란?

행 값 표현식(또는 튜플/레코드라고도 함)은 여러 값을 하나의 단위로 묶어서 비교할 수 있게 해줍니다. 예를 들어:

```sql
SELECT * FROM t
WHERE (t.t1, t.t2) IN (
    SELECT u.u1, u.u2 FROM u
)
```

이 쿼리는 `t` 테이블에서 `(t1, t2)` 튜플이 `u` 테이블의 `(u1, u2)` 튜플 중 하나와 일치하는 모든 행을 선택합니다. 이것은 본질적으로 단일 열이 아닌 튜플 비교를 기반으로 하는 세미 조인(semi-join)입니다.

## 데이터베이스 지원 현황

완전 지원: DB2, HSQLDB, MySQL, Oracle, PostgreSQL

불완전하거나 잘못된 구현: CUBRID, H2

이러한 지원 불균형으로 인해, 행 값 표현식 술어를 모든 데이터베이스에서 작동하는 동등한 쿼리로 변환하는 방법이 필요합니다.

## 변환 전략

핵심 접근 방식은 지원되지 않는 행 값 술어를 파생 테이블을 사용하는 `EXISTS` 절로 변환하는 것입니다.

### 원본 쿼리

```sql
SELECT * FROM t
WHERE (t.t1, t.t2) IN (SELECT u.u1, u.u2 FROM u)
```

### 변환된 쿼리

```sql
SELECT * FROM t
WHERE EXISTS (
    SELECT * FROM (
        SELECT null v1, null v2
        FROM dual
        WHERE 1 = 0
        UNION ALL
        SELECT u.u1, u.u2 FROM u
    ) v
    WHERE t.t1 = v.v1 AND t.t2 = v.v2
)
```

변환에서 `UNION ALL` 구조와 빈 결과 집합(`WHERE 1 = 0`)을 포함하는 파생 테이블을 도입하는 이유는 의미적 동등성을 유지하면서 새로운 행이 추가되는 것을 방지하기 위함입니다. 이 접근 방식은 모든 데이터베이스 플랫폼에서 호환성을 보장합니다.

## 술어 변형

### NOT IN 술어

`NOT IN` 술어는 부정된 `EXISTS` 로직으로 변환됩니다:

```sql
-- 원본
SELECT * FROM t
WHERE (t.t1, t.t2) NOT IN (SELECT u.u1, u.u2 FROM u)

-- 변환
SELECT * FROM t
WHERE NOT EXISTS (
    SELECT * FROM (
        SELECT null v1, null v2
        FROM dual
        WHERE 1 = 0
        UNION ALL
        SELECT u.u1, u.u2 FROM u
    ) v
    WHERE t.t1 = v.v1 AND t.t2 = v.v2
)
```

### 동등/부등 비교

동등 비교는 서브쿼리가 단일 행만 반환하는 스칼라 서브셀렉트에서 `IN` 연산과 유사하게 작동합니다.

### 순서 비교

순서 비교(예: `>`, `<`, `>=`, `<=`)는 튜플 비교를 올바르게 처리하기 위해 확장된 술어가 필요합니다:

```sql
-- 원본
SELECT * FROM t
WHERE (t.t1, t.t2) > (v.v1, v.v2)

-- 변환
SELECT * FROM t
WHERE (t.t1 > v.v1) OR (t.t1 = v.v1 AND t.t2 > v.v2)
```

튜플의 순서 비교는 사전식(lexicographic) 순서를 따릅니다. 즉, 첫 번째 요소를 먼저 비교하고, 같으면 두 번째 요소를 비교하는 식입니다.

### 정량화된 비교 (ANY, ALL)

`ANY`와 `ALL` 정량자를 사용하는 비교는 변환 접근 방식을 적절히 수정해야 합니다.

```sql
-- = ANY (IN과 동일)
SELECT * FROM t
WHERE (t.t1, t.t2) = ANY (SELECT u.u1, u.u2 FROM u)

-- = ALL
SELECT * FROM t
WHERE (t.t1, t.t2) = ALL (SELECT u.u1, u.u2 FROM u)
```

## jOOQ에서의 구현

jOOQ 3.1은 이러한 변환을 타입 안전한 Java API 뒤에서 자동으로 구현합니다. 이를 통해 개발자는 수동으로 쿼리를 다시 작성할 필요 없이 이식 가능한 SQL을 작성할 수 있습니다.

```java
// jOOQ를 사용한 타입 안전한 행 값 표현식
create.selectFrom(T)
      .where(row(T.T1, T.T2).in(
          select(U.U1, U.U2).from(U)
      ))
      .fetch();
```

jOOQ는 대상 데이터베이스가 행 값 표현식을 지원하는지 여부를 자동으로 감지하고, 지원하지 않는 경우 위에서 설명한 변환을 적용합니다.

## 중요한 주의사항

이 글에서 제시한 변환은 "NULL이 관련된 엣지 케이스를 의도적으로 생략"했습니다. 프로덕션 환경에서 사용할 때는 NULL 값이 포함된 경우의 동작을 신중하게 고려해야 합니다.

SQL의 3값 논리(3-valued logic)에서 NULL과의 비교는 항상 UNKNOWN을 반환하므로, NULL 값이 포함된 튜플 비교는 예상치 못한 결과를 초래할 수 있습니다.

## 결론

행 값 표현식은 SQL에서 매우 강력한 기능이지만, 데이터베이스 간 지원 차이로 인해 이식성 문제가 발생할 수 있습니다. jOOQ와 같은 쿼리 빌더를 사용하면 이러한 변환을 자동으로 처리하여 데이터베이스에 관계없이 일관된 결과를 얻을 수 있습니다.
