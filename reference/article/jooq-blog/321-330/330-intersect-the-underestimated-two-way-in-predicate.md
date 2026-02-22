# INTERSECT - 과소평가된 양방향 IN 술어

> 원문: https://blog.jooq.org/intersect-the-underestimated-two-way-in-predicate/

Reddit에서 한 사용자가 흥미로운 질문을 올렸습니다: "Var1과 Var2 두 값 모두 1, 2, 또는 3 중 하나를 반환하는" 술어를 어떻게 표현할 수 있을까요? 이 글에서는 이 문제에 대한 여러 SQL 솔루션을 살펴보겠습니다.

## 정석적인 해결책

가장 직관적인 방법은 모든 조건을 명시적으로 나열하는 것입니다:

```sql
WHERE Var1 = 1 OR Var1 = 2 OR Var1 = 3
OR    Var2 = 1 OR Var2 = 2 OR Var2 = 3
```

이 접근 방식은 상당한 중복을 만들어냅니다.

## IN 술어 사용하기

### 기본 접근 방식

```sql
WHERE Var1 IN (1, 2, 3)
OR    Var2 IN (1, 2, 3)
```

이 방법은 중복을 다소 줄여주지만, 여전히 집합의 크기가 커지면 표현식 길이가 이차적으로 증가합니다.

### 역방향 접근 방식

IN 술어를 뒤집어서 사용할 수도 있습니다:

```sql
WHERE 1 IN (Var1, Var2)
OR    2 IN (Var1, Var2)
OR    3 IN (Var1, Var2)
```

이 방식도 마찬가지로 집합 크기에 따라 표현식이 길어지는 문제가 있습니다.

## EXISTS와 JOIN을 사용한 해결책

```sql
WHERE EXISTS (
    SELECT 1
    FROM (VALUES (Var1), (Var2)) t1(v)
    JOIN (VALUES (1), (2), (3)) t2(v)
    ON t1.v = t2.v
)
```

이 솔루션은 각각 단일 값을 가진 두 개의 테이블을 구성하고, 해당 값을 기준으로 조인하여 교집합 요소를 찾습니다.

## EXISTS와 INTERSECT를 사용한 해결책

```sql
WHERE EXISTS (
    SELECT v
    FROM (VALUES (Var1), (Var2)) t1(v)
    INTERSECT
    SELECT v
    FROM (VALUES (1), (2), (3)) t2(v)
)
```

이것이 가장 깔끔한 해결책입니다. SQL 길이가 O(m * n)이 아닌 O(m + n)으로 증가하기 때문입니다. 여기서 m은 변수의 수이고 n은 비교할 값들의 수입니다.

INTERSECT를 사용하면 두 집합 간의 실제 집합 교집합 연산을 수행합니다. 교집합에 어떤 요소라도 존재하면(EXISTS가 참이면), 조건이 충족됩니다.

## 복잡도 비교

- IN 술어 방식: O(m * n) - 변수 수와 값 수의 곱에 비례
- INTERSECT 방식: O(m + n) - 변수 수와 값 수의 합에 비례

따라서 데이터셋이 커질수록 INTERSECT 방식이 훨씬 더 효율적입니다.

## 데이터베이스 지원 현황

INTERSECT는 다음을 포함한 대부분의 주요 데이터베이스 시스템에서 널리 지원됩니다:

- CUBRID
- DB2
- Derby
- H2
- HANA
- HSQLDB
- Informix
- Ingres
- Oracle
- PostgreSQL
- SQLite
- SQL Server
- Sybase ASE
- Sybase SQL Anywhere

INTERSECT ALL 지원: PostgreSQL과 CUBRID는 덜 일반적인 INTERSECT ALL 변형도 지원합니다.

## 결론

INTERSECT는 여러 변수가 공통 집합의 값과 일치해야 하는 술어를 표현할 때 과소평가된 강력한 도구입니다. 코드가 더 깔끔할 뿐만 아니라 더 나은 확장성을 제공하므로, 복잡한 IN 술어 조합 대신 INTERSECT 사용을 고려해 보세요.
