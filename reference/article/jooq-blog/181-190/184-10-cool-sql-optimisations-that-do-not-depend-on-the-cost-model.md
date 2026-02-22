# 비용 모델에 의존하지 않는 10가지 멋진 SQL 최적화

> 원문: https://blog.jooq.org/10-cool-sql-optimisations-that-do-not-depend-on-the-cost-model/

## 소개

비용 기반 최적화(Cost Based Optimization)는 현대 데이터베이스의 표준적인 접근 방식입니다. 동적으로 계산된 실행 계획을 직접 작성한 알고리즘으로 능가하기는 극히 어렵습니다. 하지만 비용 모델이 아닌 메타데이터와 쿼리 구조에만 의존하는 더 단순한 최적화들도 존재합니다.

이러한 최적화들은 인덱스 유무, 데이터 양, 데이터 분포의 편향 정도와 관계없이 항상 더 나은 실행 계획을 제공합니다. 두 가지 주요 활용 사례가 있습니다: 쿼리 실수 수정, 그리고 복잡한 뷰의 전체 로직을 실행하지 않고도 재사용할 수 있게 하는 것입니다.

### 테스트 환경

db-engines 순위에 따른 5개 주요 RDBMS를 분석했습니다:
- Oracle 12.2
- MySQL 8.0.2
- SQL Server 2014
- PostgreSQL 9.6
- DB2 LUW 10.5

모든 쿼리는 이러한 유형의 평가를 위한 표준 참조 데이터셋인 Sakila 데이터베이스를 대상으로 합니다.

---

## 1. 이행적 폐쇄 (Transitive Closure)

이 최적화는 동등 관계가 전파될 수 있음을 인식합니다. 컬럼 A가 상수와 같고 A가 컬럼 B와 같다면, B도 그 상수와 같아야 합니다.

`A = B`이고 `B = C`이면, 옵티마이저는 `A = C`를 추론할 수 있습니다.

### 작동 원리

`WHERE actor_id = 1`을 작성하고 `actor_id = fa.actor_id`로 조인할 때, 옵티마이저는 `fa.actor_id = 1`이라고 추론할 수 있습니다. 이를 통해 복잡한 쿼리에서 더 정확한 카디널리티 추정과 더 나은 인덱스 선택이 가능해집니다.

### 예제 쿼리

```sql
SELECT first_name, last_name, film_id
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
WHERE a.actor_id = 1;
```

```sql
SELECT first_name, last_name, film_id
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
WHERE first_name = 'PENELOPE'
AND last_name = 'GUINESS';
```

### 데이터베이스별 실행 계획

Oracle: 조인의 두 테이블 모두에 `A.ACTOR_ID=1` 조건자를 적용하여, 논리적 동등성을 인식하고 조건을 푸시다운합니다.

DB2: 인덱스 스캔에 `START (Q2.ACTOR_ID = 1) STOP (Q2.ACTOR_ID = 1)`을 표시하여 이행적 폐쇄 적용을 나타냅니다.

MySQL: `REF: const`를 두 번 사용하여, 컬럼 참조가 아닌 상수 값 스캔임을 알립니다.

PostgreSQL: 비트맵 인덱스 스캔에 `Index Cond: (actor_id = 1)`을 명시적으로 표시합니다.

SQL Server: 두 인덱스 탐색 모두에 `SEEK:([a].[actor_id]=(1))`와 `SEEK:([fa].[actor_id]=(1))`를 표시합니다.

### 지원 현황

| 데이터베이스 | 이행적 폐쇄 |
|---|---|
| DB2 LUW 10.5 | ✓ |
| MySQL 8.0.2 | ✓ |
| Oracle 12.2 | ✓ |
| PostgreSQL 9.6 | ✓ |
| SQL Server 2014 | ✓ |

지원: 5개 데이터베이스 모두 지원합니다.

---

## 2. 불가능한 조건자와 불필요한 테이블 접근 (Impossible Predicates and Unneeded Table Accesses)

논리적으로 불가능한 조건을 포함하는 쿼리(`WHERE 1 = 0` 또는 `WHERE NULL = NULL`)는 테이블 접근이 필요 없습니다 - 증명 가능하게 빈 결과를 반환합니다.

### 작동 원리

3값 논리(three-valued logic)에서 `NULL = NULL`은 NULL을 산출하며, WHERE 절에서 FALSE로 처리됩니다. 이를 인식하면 불필요한 I/O 작업을 완전히 제거할 수 있습니다.

### 예제 쿼리

```sql
SELECT * FROM actor WHERE 1 = 0;
SELECT * FROM actor WHERE NULL = NULL;
```

### 데이터베이스별 실행 계획

DB2: `TBSCAN GENROW | 0 of 0 | 0` - 테이블 접근을 완전히 제거합니다.

MySQL: EXTRA 컬럼에 `Impossible WHERE` - 불가능한 조건을 명시적으로 표시합니다.

Oracle: `NULL IS NOT NULL` 조건자와 함께 `FILTER` 연산을 표시합니다. 계획에는 여전히 테이블 접근이 표시되지만, 실제로 실행되지는 않습니다.

PostgreSQL: `One-Time Filter: false` - 테이블 스캔 연산 없음.

SQL Server: `Constant Scan` - 테이블 접근 없이 0개 행을 생성합니다.

### 지원 현황

| 데이터베이스 | 불가능한 조건자 | 불필요한 테이블 접근 제거 |
|---|---|---|
| DB2 LUW 10.5 | ✓ | ✓ |
| MySQL 8.0.2 | ✓ | ✓ |
| Oracle 12.2 | ✓ | ✓ |
| PostgreSQL 9.6 | ✓ | ✓ |
| SQL Server 2014 | ✓ | ✓ |

지원: 모든 데이터베이스가 이를 효율적으로 제거합니다. 단, Oracle의 실행 계획은 단순히 FALSE가 아닌 `NULL IS NOT NULL` 필터를 표시하여 혼란스러울 수 있습니다.

---

## 3. JOIN 제거 (JOIN Elimination)

외래 키 관계는 결과 집합에 영향을 미치지 않는 불필요한 조인을 제거할 기회를 만듭니다. 기본 테이블의 컬럼만 선택한다면, 룩업 테이블로의 INNER JOIN은 중복됩니다.

### 작동 원리

기본 키/외래 키와 유일성 제약 조건에 대한 메타데이터를 통해 옵티마이저는 조인 제거가 쿼리 의미를 보존한다는 것을 증명할 수 있습니다.

다음 경우에 적용됩니다:
- NOT NULL 외래 키가 있는 to-one 관계의 INNER JOIN
- 유일 제약 조건이 있는 테이블로의 OUTER JOIN
- to-many OUTER JOIN이 있는 DISTINCT

### 예제 쿼리

INNER JOIN to-one (NOT NULL 외래 키):
```sql
SELECT first_name, last_name
FROM customer c
JOIN address a ON c.address_id = a.address_id;
```

INNER JOIN to-one (nullable 외래 키):
```sql
SELECT title
FROM film f
JOIN language l ON f.original_language_id = l.language_id;
```

OUTER JOIN to-one:
```sql
SELECT first_name, last_name
FROM customer c
LEFT JOIN address a ON c.address_id = a.address_id;
```

DISTINCT with to-many OUTER JOIN:
```sql
SELECT DISTINCT first_name, last_name
FROM actor a
LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id;
```

### 지원 현황

| 데이터베이스 | INNER JOIN to-one | Nullable to-one | OUTER JOIN to-one | OUTER JOIN DISTINCT to-many |
|---|---|---|---|---|
| DB2 LUW 10.5 | ✓ | ✓ | ✓ | ✓ |
| MySQL 8.0.2 | ✗ | ✗ | ✗ | ✗ |
| Oracle 12.2 | ✓ | ✓ | ✓ | ✗ |
| PostgreSQL 9.6 | ✗ | ✗ | ✓ | ✗ |
| SQL Server 2014 | ✓ | ✗ | ✓ | ✓ |

지원이 크게 다릅니다: DB2가 4가지 제거 유형 모두 처리하며 이 영역에서 뛰어납니다. SQL Server는 nullable 외래 키에서는 어려움을 겪지만 OUTER JOIN to-one과 DISTINCT 시나리오는 처리합니다. Oracle은 to-many 관계가 있는 OUTER JOIN DISTINCT는 제거할 수 없습니다. MySQL과 PostgreSQL은 제한된 JOIN 제거 기능을 보여줍니다.

---

## 4. "어리석은" 조건자 제거 (Removing "Silly" Predicates)

nullable 컬럼에서 `WHERE column = column` 같은 표현식은 논리적으로 `WHERE column IS NOT NULL`이 됩니다. non-nullable 컬럼에서는 이러한 조건자를 완전히 제거할 수 있습니다.

### 작동 원리

옵티마이저는 자기 비교가 절대 FALSE가 될 수 없음을 인식합니다(nullable 컬럼에서는 NULL이 될 수 있지만). 따라서 조건자를 NULL 검사로 대체하거나 완전히 제거합니다.

### 예제 쿼리

```sql
SELECT * FROM actor WHERE 1 = 1;
-- nullable 컬럼의 자기 비교
SELECT * FROM film WHERE release_year = release_year;
-- non-nullable 컬럼의 자기 비교
SELECT * FROM film WHERE film_id = film_id;
```

### 지원 현황

| 데이터베이스 | NULL 의미론 (nullable) | No NULL 의미론 (non-nullable) |
|---|---|---|
| DB2 LUW 10.5 | ✓ | ✓ |
| MySQL 8.0.2 | ✗ | ✓ |
| Oracle 12.2 | ✓ | ✓ |
| PostgreSQL 9.6 | ✗ | ✗ |
| SQL Server 2014 | ✗ | ✗ |

혼합된 결과: DB2와 Oracle이 두 경우 모두 잘 처리합니다. PostgreSQL과 SQL Server는 non-nullable 컬럼에서도 간단한 `column = column` 케이스조차 최적화하지 못합니다.

PostgreSQL은 `release_year = release_year`를 최적화하지 못하여, 1000이 아닌 5의 카디널리티 추정을 보여줍니다.

---

## 5. EXISTS 서브쿼리의 프로젝션 (Projections in EXISTS Subqueries)

EXISTS 서브쿼리에서 `SELECT *`를 사용해도 영향이 없습니다 - 옵티마이저는 프로젝션 목록을 평가하지 않고 적격 행의 존재만 확인합니다.

### 작동 원리

EXISTS는 일치하는 행이 존재한다는 확인만 필요하며, 컬럼 값의 검색은 필요 없습니다. 옵티마이저는 테이블 데이터를 건드리지 않고 인덱스 컬럼만 접근할 수 있습니다.

### 예제 쿼리

```sql
SELECT first_name, last_name
FROM actor a
WHERE EXISTS (
  SELECT *
  FROM film_actor fa
  WHERE a.actor_id = fa.actor_id
);
```

이 경우 `SELECT *`가 컬럼 물리화를 강제하지 않습니다; 데이터베이스는 존재만 확인하므로, 특정 컬럼 선택이 불필요합니다.

### 0으로 나누기 테스트

일부 데이터베이스는 서브쿼리의 프로젝션을 정말로 평가하지 않는지 0으로 나누기로 테스트할 수 있습니다:

```sql
-- DB2
SELECT 1 / 0 FROM sysibm.dual;

-- Oracle
SELECT 1 / 0 FROM dual;

-- PostgreSQL, SQL Server
SELECT 1 / 0;

-- MySQL (0으로 나누기 대신 다른 오류 사용)
SELECT pow(-1, 0.5);
```

### 지원 현황

| 데이터베이스 | EXISTS 프로젝션 제거 |
|---|---|
| DB2 LUW 10.5 | ✓ |
| MySQL 8.0.2 | ✓ |
| Oracle 12.2 | ✓ |
| PostgreSQL 9.6 | ✓ |
| SQL Server 2014 | ✓ |

지원: 5개 데이터베이스 모두 이를 올바르게 최적화합니다.

---

## 6. 조건자 병합 (Predicate Merging)

동일한 컬럼에 대한 여러 조건자를 통합할 수 있습니다. 예를 들어, `WHERE id IN (2,3,4) AND id IN (1,2,3)`은 `WHERE id IN (2,3)`이 됩니다.

### 작동 원리

겹치는 조건은 수학적으로 교집합으로 축소될 수 있어, 카디널리티 추정이 개선되고 전체 테이블 스캔 대신 인덱스 범위 스캔이 가능해집니다.

범위 조건자도 비슷하게 결합될 수 있습니다: `FILM_ID BETWEEN 1 AND 100 AND FILM_ID BETWEEN 99 AND 200`은 `FILM_ID BETWEEN 99 AND 100`이 됩니다.

### 예제 쿼리

IN 조건자 병합:
```sql
SELECT *
FROM actor
WHERE actor_id IN (2, 3, 4)
AND actor_id IN (1, 2, 3);
```

범위 조건자 병합:
```sql
SELECT *
FROM film
WHERE film_id BETWEEN 1 AND 100
AND film_id BETWEEN 99 AND 200;
```

교집합이 없는 범위 (빈 결과):
```sql
SELECT *
FROM film
WHERE film_id BETWEEN 1 AND 2
AND film_id BETWEEN 199 AND 200;
```

### 지원 현황

| 데이터베이스 | IN 병합 | 범위 병합 |
|---|---|---|
| DB2 LUW 10.5 | ✓ | ✓ |
| MySQL 8.0.2 | ✓ | ✓ |
| Oracle 12.2 | ✓ | ✓ |
| PostgreSQL 9.6 | ✗ | ✗ |
| SQL Server 2014 | ✓ | ✗ |

PostgreSQL은 `actor_id IN (2,3,4) AND actor_id IN (1,2,3)` 병합에 실패하여, 계획에 두 조건자가 모두 표시됩니다.

SQL Server는 단일 범위로 최적화하지 않고 SEEK와 WHERE 연산을 모두 사용한 범위 병합을 표시합니다.

---

## 7. 증명 가능하게 빈 집합 (Provably Empty Sets)

파생 테이블이나 서브쿼리가 NOT NULL 제약 조건 위반(예: NOT NULL 컬럼에서 `WHERE film_id IS NULL`)을 포함하면, 전체 쿼리가 NOOP가 됩니다.

### 작동 원리

제약 조건 메타데이터가 특정 결과 집합이 존재할 수 없음을 증명합니다. INNER JOIN이 증명 가능하게 빈 집합을 포함하면, 전체 조인을 제거할 수 있습니다.

### 예제 쿼리

JOIN으로 빈 집합:
```sql
SELECT first_name, last_name
FROM actor a
JOIN (
  SELECT *
  FROM film_actor
  WHERE film_id IS NULL  -- film_id는 NOT NULL이므로 불가능
) fa ON a.actor_id = fa.actor_id;
```

SEMI JOIN (IN)으로 빈 집합:
```sql
SELECT *
FROM actor a
WHERE a.actor_id IN (
  SELECT actor_id
  FROM film_actor
  WHERE actor_id IS NULL  -- actor_id는 NOT NULL이므로 불가능
);
```

### 지원 현황

| 데이터베이스 | JOIN/NULL | JOIN/INTERSECT | SEMI JOIN/NULL | SEMI JOIN/INTERSECT |
|---|---|---|---|---|
| DB2 LUW 10.5 | ✓ | ✓ | ✓ | ✓ |
| MySQL 8.0.2 | ✓ | N/A | ✓ | N/A |
| Oracle 12.2 | ✓ | ✗ | ✓ | ✗ |
| PostgreSQL 9.6 | ✗ | ✗ | ✗ | ✗ |
| SQL Server 2014 | ✓ | ✓ | ✓ | ✓ |

DB2와 SQL Server는 모든 증명 가능하게 빈 시나리오에서 일관되게 `Constant Scan` 또는 `GENROW` 연산을 보여줍니다.

MySQL은 NULL 케이스는 지원하지만 INTERSECT가 없습니다.

Oracle은 단순한 케이스는 처리하지만 INTERSECT에서는 실패합니다.

PostgreSQL은 이 최적화를 제공하지 않습니다.

---

## 8. CHECK 제약 조건 (CHECK Constraints)

CHECK 제약 조건은 불가능한 값 감지를 가능하게 합니다. 특정 값으로 제한된 컬럼에서 `WHERE rating = 'N/A'`는 즉시 제거될 수 있습니다.

### 작동 원리

메타데이터로 저장된 도메인 제약 조건이 쿼리 실행 전에 전체 결과 클래스를 제거합니다.

### 예제 쿼리

CHECK 제약 조건이 있는 테이블:
```sql
CREATE TABLE film (
  RATING varchar(10) DEFAULT 'G',
  CONSTRAINT check_special_rating
    CHECK (rating IN ('G','PG','PG-13','R','NC-17'))
);
```

불가능한 조건자:
```sql
SELECT * FROM film WHERE rating = 'N/A';
```

인덱스와 역 조건자:
```sql
CREATE INDEX idx_film_rating ON film (rating);
SELECT count(*)
FROM film
WHERE rating NOT IN ('G','PG','PG-13','R');
```

### MySQL의 CHECK 제약 조건 미적용

MySQL은 CHECK 제약 조건 구문을 받아들이지만 강제하지 않습니다:

```sql
CREATE TABLE x (a INT CHECK (a != 0));
INSERT INTO x VALUES (0);  -- 성공! (실패해야 함)
SELECT * FROM x;           -- 0 반환
```

### 지원 현황

| 데이터베이스 | 불가능한 조건자 | 역 조건자 | 강제 적용 |
|---|---|---|---|
| DB2 LUW 10.5 | ✓ | ✗ | ✓ |
| MySQL 8.0.2 | ✗ | ✗ | ✗ |
| Oracle 12.2 | ✓ | ✗ | ✓ |
| PostgreSQL 9.6 | ✓ | ✗ | ✓ |
| SQL Server 2014 | ✓ | ✗ | ✓ |

DB2와 Oracle이 불가능한 값을 인식합니다. MySQL은 CHECK 제약 조건을 강제하지 않습니다. PostgreSQL은 일관되지 않은 동작을 보여줍니다.

---

## 9. 불필요한 셀프 조인 (Unneeded Self JOINs)

결과를 의미 있게 필터링하지 않는 셀프 조인(예: 다른 조건으로 제한하지 않고 테이블을 자기 자신과 조인)은 단일 테이블 접근으로 축소될 수 있습니다.

### 작동 원리

유일성에 대한 메타데이터가 조인이 행을 추가하지 않고 단지 데이터를 복제할 뿐임을 보장합니다.

### 지원 현황

데이터베이스 시스템 간에 지원이 크게 다릅니다.

---

## 10. 조건자 푸시다운 (Predicate Pushdown)

서브쿼리나 뷰에 적용 가능한 필터는 물리화 후에 적용되지 않고 내부 쿼리로 "푸시다운"될 수 있습니다.

### 작동 원리

소스에서 필터링하면 중간 결과 집합이 더 일찍 줄어들어, 실행 계획을 통한 데이터 이동이 최소화됩니다.

### 지원 현황

일반적으로 강력한 지원이 있지만, 구현 세부 사항은 다릅니다.

---

## 결론

이러한 최적화는 다음에 대해 매우 가치가 있습니다:
- 재작성 없이 차선의 수동 쿼리 수정
- 불필요한 로직을 실행하지 않는 재사용 가능한 뷰 라이브러리 구축
- 복잡한 쿼리에서 카디널리티 추정 개선

### 데이터베이스별 종합 평가

DB2: 거의 모든 카테고리에서 포괄적인 최적화를 보여주며, 가장 강력한 최적화 기능을 제공합니다.

SQL Server: JOIN 제거와 빈 집합 감지에서 좋은 성능을 보이지만, 범위 조건자 병합에서 뒤처집니다.

Oracle: 대부분의 최적화를 잘 처리하지만, 잔여 연산이 있는 혼란스러운 실행 계획을 표시합니다.

PostgreSQL: 조건자 병합과 증명 가능하게 빈 집합 감지에서 크게 뒤처지며, 비용 모델 독립적 변환 적용에서 가장 약한 성능을 보여줍니다.

MySQL: CHECK 제약 조건 강제 메커니즘이 없고 최적화 감지에서 격차를 보이지만, 기본적인 최적화는 적절히 처리합니다.

### 핵심 통찰

핵심적인 통찰: 휴리스틱과 비용 추정에 의존하기보다는 논리적으로 건전한 쿼리를 작성하고 메타데이터 기반 최적화가 나머지를 처리하게 하세요.

이러한 최적화들은 필수 작업이 아닌 선택적 작업을 제거하며, 여러 레이어를 불필요한 로직 실행 없이 재사용할 수 있는 복잡한 뷰 라이브러리에서 특히 가치가 있습니다.
