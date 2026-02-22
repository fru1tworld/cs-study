# 세미 조인과 안티 조인은 SQL에서 자체 구문을 가져야 한다

> 원문: https://blog.jooq.org/semi-join-and-anti-join-should-have-its-own-syntax-in-sql/

## 서론

관계 대수(relational algebra)는 수십 년 전부터 세미 조인(semi join)과 안티 조인(anti join) 연산을 정의해 왔습니다. 그러나 SQL에는 이 두 가지 중요한 관계 연산자에 대한 네이티브 구문이 없습니다. 대신, 개발자들은 `IN`, `EXISTS`, `NOT IN`, `NOT EXISTS` 술어를 사용하는 우회 방법에 의존해야 합니다.

SQL은 여전히 발전하고 있는 언어입니다. 데이터베이스가 내부적으로 `EXISTS`/`NOT EXISTS`를 세미/안티 조인 연산으로 최적화하더라도, 이 구문을 명시적으로 노출하면 쿼리를 더 간결하고 의도가 명확하게 작성할 수 있습니다.

## 세미 조인 (Semi Join)

세미 조인은 왼쪽 관계(relation)의 속성만 반환하면서, 오른쪽 관계에서 조인 조건이 결과를 생성하는지 확인합니다. "Semi"는 오른쪽을 실제로 조인하지 않고, 조인이 결과를 산출할지만 확인한다는 의미입니다.

### 현재 SQL 우회 방법

현재 SQL에서 세미 조인을 표현하려면 `IN` 또는 `EXISTS` 술어를 사용해야 합니다:

```sql
-- IN을 사용한 세미 조인
SELECT * FROM Employee
WHERE DeptName IN (
  SELECT DeptName FROM Dept
)

-- EXISTS를 사용한 세미 조인
SELECT * FROM Employee e
WHERE EXISTS (
  SELECT 1 FROM Dept d
  WHERE e.DeptName = d.DeptName
)
```

### 제안된 네이티브 구문

더 명확한 구문은 다음과 같을 것입니다:

```sql
SELECT * FROM Employee
LEFT SEMI JOIN Dept USING (DeptName)

-- 또는 ON 절 사용
SELECT * FROM Employee
LEFT SEMI JOIN Dept ON Employee.DeptName = Dept.DeptName
```

## 안티 조인 (Anti Join)

안티 조인은 세미 조인의 반대입니다. 오른쪽 관계에서 일치하는 행이 없을 때만 왼쪽 관계의 속성을 반환합니다. 즉, 조인이 결과를 생성하지 "않는" 튜플을 확인합니다.

### 현재 SQL 우회 방법

표준 SQL에서는 `NOT IN` 또는 `NOT EXISTS`를 사용해야 합니다:

```sql
-- NOT IN을 사용한 안티 조인
SELECT * FROM Employee
WHERE DeptName NOT IN (
  SELECT DeptName FROM Dept
)

-- NOT EXISTS를 사용한 안티 조인
SELECT * FROM Employee e
WHERE NOT EXISTS (
  SELECT 1 FROM Dept d
  WHERE e.DeptName = d.DeptName
)
```

### 제안된 네이티브 구문

더 명확한 구문은 다음과 같을 것입니다:

```sql
SELECT * FROM Employee
LEFT ANTI JOIN Dept USING (DeptName)

-- 또는 ON 절 사용
SELECT * FROM Employee
LEFT ANTI JOIN Dept ON Employee.DeptName = Dept.DeptName
```

## 이미 지원하는 시스템들

몇몇 데이터베이스와 도구들은 이미 이 구문을 지원합니다:

- Cloudera Impala: `LEFT SEMI JOIN` 및 `LEFT ANTI JOIN` 구문을 지원합니다
- U-SQL: Microsoft의 빅데이터 쿼리 언어에서 이 구문을 채택했습니다
- DuckDB: 이 키워드들을 구현했습니다
- jOOQ 3.7: jOOQ에서 이 연산을 지원하며, 기본 데이터베이스 시스템에 관계없이 동등한 `EXISTS` 술어를 생성합니다

jOOQ에서는 다음과 같이 작성할 수 있습니다:

```java
ctx.select()
   .from(Employee)
   .leftSemiJoin(Dept)
   .on(Employee.DeptName.eq(Dept.DeptName))
```

## 결론

세미 조인과 안티 조인은 관계 대수 이론과 SQL 구현 사이의 격차를 메우는 연산자입니다. 주요 데이터베이스 벤더들이 네이티브 `SEMI JOIN` 및 `ANTI JOIN` 구문을 구현하기를 권장합니다. 이러한 구문은 쿼리를 더 읽기 쉽게 만들고, 개발자의 의도를 더 명확하게 표현할 수 있게 해줍니다.

관계 대수에서 수십 년 동안 지원해온 이 연산들을 SQL에서도 일급 시민(first-class citizen)으로 대우해야 할 때입니다.
