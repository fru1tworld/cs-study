# SQL에서 테이블을 조인하는 다양한 방법에 대한 (아마도 불완전한) 종합 가이드

> 원문: https://blog.jooq.org/a-probably-incomplete-comprehensive-guide-to-the-many-different-ways-to-join-tables-in-sql/

## 개요

SQL에서 조인(JOIN)은 여러 테이블의 데이터를 결합하는 핵심 연산입니다. 이 글에서는 표준 조인부터 고급 조인 기법까지 다양한 조인 유형을 살펴봅니다.

## CROSS JOIN (교차 조인)

CROSS JOIN은 카테시안 곱(Cartesian Product)을 생성합니다. 한 테이블의 모든 행을 다른 테이블의 모든 행과 결합합니다.

```sql
SELECT *
FROM days
CROSS JOIN departments
```

결과 크기 공식:
```
size(result) = size(days) * size(departments)
```

중요: 어느 한 테이블이라도 비어 있으면 결과도 비어 있습니다.

## INNER JOIN (내부 조인)

INNER JOIN은 ON 절의 조건(Predicate)을 사용하여 카테시안 곱을 필터링합니다.

```sql
SELECT *
FROM days d
INNER JOIN departments dept
  ON d.day = dept.day
```

결과 크기 공식:
```
size(result) <= size(days) * size(departments)
```

조건이 결과를 줄이기 때문에 결과 크기는 카테시안 곱보다 작거나 같습니다.

## EQUI JOIN (등가 조인)

EQUI JOIN은 등호(=) 조건을 사용하는 특수한 INNER JOIN입니다. 일반적으로 기본 키(Primary Key)와 외래 키(Foreign Key) 관계에서 사용됩니다.

### USING 절

양쪽 테이블의 컬럼 이름이 같을 때 사용할 수 있습니다.

```sql
SELECT *
FROM days d
INNER JOIN departments dept
  USING (day)
```

### NATURAL JOIN

공통 컬럼 이름을 기준으로 자동으로 조인합니다.

```sql
SELECT *
FROM days
NATURAL JOIN departments
```

주의: NATURAL JOIN은 일반적으로 권장되지 않습니다. 테이블 구조가 변경되면 예기치 않은 결과를 초래할 수 있습니다.

## OUTER JOIN (외부 조인)

### LEFT OUTER JOIN (왼쪽 외부 조인)

왼쪽 테이블의 모든 행을 유지하고, 오른쪽 테이블에 일치하는 행이 없으면 NULL 값을 추가합니다.

```sql
SELECT *
FROM days d
LEFT OUTER JOIN departments dept
  ON d.day = dept.day
```

### RIGHT OUTER JOIN (오른쪽 외부 조인)

오른쪽 테이블의 모든 행을 유지합니다.

```sql
SELECT *
FROM days d
RIGHT OUTER JOIN departments dept
  ON d.day = dept.day
```

### FULL OUTER JOIN (완전 외부 조인)

양쪽 테이블의 모든 행을 유지합니다.

```sql
SELECT *
FROM days d
FULL OUTER JOIN departments dept
  ON d.day = dept.day
```

## PARTITIONED OUTER JOIN (분할 외부 조인)

Oracle에서 지원하는 특수한 외부 조인으로, 파티션별로 조합을 생성하고 조건부 매칭을 수행합니다.

```sql
SELECT *
FROM days d
LEFT OUTER JOIN departments dept
  PARTITION BY (dept.department_id)
  ON d.day = dept.day
```

이는 각 부서별로 모든 날짜의 데이터를 보고자 할 때 유용합니다.

## SEMI JOIN (세미 조인)

SEMI JOIN은 다른 테이블에 일치하는 행이 존재하는 경우에만 첫 번째 테이블의 행을 반환합니다. 중복 없이 결과를 반환합니다.

일반적으로 EXISTS 또는 IN을 사용하여 구현합니다.

```sql
-- EXISTS 사용
SELECT *
FROM days d
WHERE EXISTS (
  SELECT 1
  FROM departments dept
  WHERE d.day = dept.day
)

-- IN 사용
SELECT *
FROM days d
WHERE d.day IN (
  SELECT dept.day
  FROM departments dept
)
```

## ANTI JOIN (안티 조인)

ANTI JOIN은 다른 테이블에 일치하는 행이 존재하지 않는 경우에만 행을 반환합니다.

```sql
-- NOT EXISTS 사용 (권장)
SELECT *
FROM days d
WHERE NOT EXISTS (
  SELECT 1
  FROM departments dept
  WHERE d.day = dept.day
)

-- NOT IN 사용 (NULL 값이 있을 때 주의 필요)
SELECT *
FROM days d
WHERE d.day NOT IN (
  SELECT dept.day
  FROM departments dept
  WHERE dept.day IS NOT NULL
)
```

주의: NOT IN은 NULL 값이 포함된 컬럼에서 예기치 않은 결과를 초래할 수 있으므로 NOT EXISTS를 사용하는 것이 더 안전합니다.

## LATERAL JOIN (래터럴 조인)

LATERAL JOIN은 PostgreSQL과 Oracle에서 지원하며, 오른쪽 테이블이 왼쪽 테이블의 컬럼을 참조할 수 있게 합니다. 이를 통해 상관 서브쿼리(Correlated Subquery)가 여러 행과 컬럼을 반환할 수 있습니다.

```sql
SELECT *
FROM days d
LEFT JOIN LATERAL (
  SELECT *
  FROM departments dept
  WHERE d.day = dept.day
  ORDER BY dept.created_at
  LIMIT 3
) t ON true
```

### APPLY (SQL Server)

SQL Server에서는 LATERAL 대신 APPLY를 사용합니다.

```sql
-- CROSS APPLY (INNER JOIN과 유사)
SELECT *
FROM days d
CROSS APPLY (
  SELECT TOP 3 *
  FROM departments dept
  WHERE d.day = dept.day
  ORDER BY dept.created_at
) t

-- OUTER APPLY (LEFT JOIN과 유사)
SELECT *
FROM days d
OUTER APPLY (
  SELECT TOP 3 *
  FROM departments dept
  WHERE d.day = dept.day
  ORDER BY dept.created_at
) t
```

## MULTISET

Oracle에서 제공하는 MULTISET은 서브쿼리 결과를 중첩 컬렉션(Nested Collection)으로 집계합니다. SQL과 객체 지향 패러다임을 연결하는 기능입니다.

```sql
SELECT
  d.day,
  CAST(MULTISET(
    SELECT dept.department_name
    FROM departments dept
    WHERE d.day = dept.day
  ) AS department_names_t) AS departments
FROM days d
```

## 실용적인 고려사항

1. 명확한 문법 사용: 유지보수성을 위해 가장 명확한 문법을 선택하세요.
2. NULL 처리 주의: NOT IN 대신 NOT EXISTS를 사용하면 NULL 관련 문제를 피할 수 있습니다.
3. NATURAL JOIN 회피: 테이블 구조 변경에 취약하므로 명시적인 조인 조건을 사용하세요.
4. 성능 고려: 조인 유형에 따라 실행 계획이 달라지므로 적절한 조인을 선택하세요.

## 요약

| 조인 유형 | 설명 |
|----------|------|
| CROSS JOIN | 카테시안 곱 생성 |
| INNER JOIN | 조건을 만족하는 행만 반환 |
| LEFT OUTER JOIN | 왼쪽 테이블의 모든 행 유지 |
| RIGHT OUTER JOIN | 오른쪽 테이블의 모든 행 유지 |
| FULL OUTER JOIN | 양쪽 테이블의 모든 행 유지 |
| SEMI JOIN | 일치하는 행이 존재하면 반환 |
| ANTI JOIN | 일치하는 행이 없으면 반환 |
| LATERAL/APPLY | 상관 서브쿼리에서 여러 행/컬럼 반환 |
