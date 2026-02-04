# SQL에서 1부터 10까지 범위를 생성하는 방법

> 원문: https://blog.jooq.org/how-to-create-a-range-from-1-to-10-in-sql/

## 소개

숫자 범위를 생성하는 것은 명령형 프로그래밍 언어에서는 매우 간단한 작업입니다. Java에서는 `for (int i = 1; i <= 10; i++)`로, Scala에서는 `(1 to 10) foreach`로 쉽게 처리할 수 있습니다. 하지만 테이블 데이터를 다루는 SQL에서는 이 간단한 작업이 창의적인 해결책을 필요로 합니다.

## 문제점

SQL에서 1부터 10까지의 단순한 범위를 생성하는 것은 언어 자체에 내장된 기능이 없기 때문에 다양한 우회 방법을 사용해야 합니다. 각 데이터베이스마다 서로 다른 접근 방식을 제공하며, 어떤 것은 우아하고 어떤 것은 그렇지 않습니다.

## 해결 방법들

### 1. 임시 테이블 생성

가장 기본적인 접근 방식은 UNION ALL 문을 반복 사용하여 수동으로 값이 1-10인 테이블을 생성하는 것입니다:

```sql
CREATE TABLE "1 to 10" AS
SELECT 1 value FROM DUAL UNION ALL
SELECT 2 FROM DUAL UNION ALL
SELECT 3 FROM DUAL UNION ALL
SELECT 4 FROM DUAL UNION ALL
SELECT 5 FROM DUAL UNION ALL
SELECT 6 FROM DUAL UNION ALL
SELECT 7 FROM DUAL UNION ALL
SELECT 8 FROM DUAL UNION ALL
SELECT 9 FROM DUAL UNION ALL
SELECT 10 FROM DUAL;
```

이 방법은 가장 직관적이지만 실용적이지 않습니다. 범위가 커지면 유지보수가 어려워집니다.

### 2. VALUES() 테이블 생성자 (SQL Server)

SQL Server에서는 인라인 테이블 생성을 사용하여 조금 더 나은 접근 방식을 제공합니다:

```sql
SELECT V
FROM (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)) [1 to 10](V)
```

이 방법은 임시 테이블을 생성하지 않고도 파생 테이블로 값 목록을 인라인으로 사용할 수 있게 해줍니다.

### 3. CTE를 사용한 셀프 조인과 이진 연산

작은 기본 테이블을 생성하고 이를 여러 번 교차 조인하여 이진 연산을 통해 원하는 범위에 도달하는 창의적인 접근 방식입니다:

```sql
WITH t(v) AS (
    SELECT 0 UNION ALL SELECT 1
)
SELECT
    t1.v * 8 + t2.v * 4 + t3.v * 2 + t4.v + 1 AS value
FROM t t1
CROSS JOIN t t2
CROSS JOIN t t3
CROSS JOIN t t4
WHERE t1.v * 8 + t2.v * 4 + t3.v * 2 + t4.v + 1 <= 10
ORDER BY value;
```

이 방법은 명시적 열거 없이 더 큰 시퀀스를 생성할 수 있습니다.

### 4. GROUPING SETS/CUBE

`GROUP BY CUBE()`를 사용하여 충분한 행을 생성할 수 있지만, 직관적이지 않습니다:

```sql
SELECT ROWNUM value
FROM (SELECT 1 FROM DUAL GROUP BY CUBE(1, 1, 1, 1))
WHERE ROWNUM <= 10;
```

### 5. PostgreSQL의 GENERATE_SERIES()

이 글에서 가장 우아한 해결책으로 칭찬받는 방법입니다:

```sql
SELECT * FROM GENERATE_SERIES(1, 10);
```

이것은 Scala의 범위 표기법만큼 편리합니다. PostgreSQL은 이 내장 함수를 통해 숫자뿐만 아니라 날짜와 타임스탬프 시퀀스도 쉽게 생성할 수 있습니다:

```sql
-- 날짜 범위 생성
SELECT * FROM GENERATE_SERIES('2024-01-01'::date, '2024-01-10'::date, '1 day');

-- 타임스탬프 범위 생성
SELECT * FROM GENERATE_SERIES('2024-01-01'::timestamp, '2024-01-01 10:00'::timestamp, '1 hour');
```

### 6. Oracle의 CONNECT BY

Oracle에서는 거의 PostgreSQL만큼 편리한 방법을 제공합니다:

```sql
SELECT LEVEL FROM DUAL CONNECT BY LEVEL <= 10;
```

이 방법은 계층적 쿼리 문법을 사용하여 시퀀스를 생성합니다. LEVEL은 계층 구조에서 현재 깊이를 나타내는 의사 열(pseudo column)입니다.

### 7. 재귀 CTE (Common Table Expression)

표준 SQL을 준수하지만 장황한 해결책입니다. 대부분의 데이터베이스에서 작동합니다:

```sql
WITH RECURSIVE "1 to 10"(V) AS (
    -- 앵커 멤버: 시작 값
    SELECT 1
    UNION ALL
    -- 재귀 멤버: 조건이 충족될 때까지 증가
    SELECT V + 1 FROM "1 to 10" WHERE V < 10
)
SELECT V FROM "1 to 10";
```

Oracle 구문 (RECURSIVE 키워드 없음):

```sql
WITH "1 to 10"(V) AS (
    SELECT 1 FROM DUAL
    UNION ALL
    SELECT V + 1 FROM "1 to 10" WHERE V < 10
)
SELECT V FROM "1 to 10";
```

SQL Server 구문:

```sql
WITH [1 to 10](V) AS (
    SELECT 1
    UNION ALL
    SELECT V + 1 FROM [1 to 10] WHERE V < 10
)
SELECT V FROM [1 to 10];
```

### 8. Oracle의 MODEL 절

의도적으로 복잡한 스프레드시트와 유사한 접근 방식입니다. 유지보수자를 좌절시키기 위한 목적으로 사용할 수 있습니다:

```sql
SELECT V
FROM DUAL
MODEL
    DIMENSION BY (0 d)
    MEASURES (0 V)
    RULES ITERATE(10) (
        V[ITERATION_NUMBER] = ITERATION_NUMBER + 1
    )
ORDER BY 1;
```

MODEL 절은 스프레드시트처럼 규칙을 반복적으로 처리하는 Oracle의 고급 기능입니다. 강력하지만 가독성이 떨어집니다.

### 9. 시스템 테이블 쿼리

`ALL_OBJECTS`와 같은 기존 시스템 테이블을 `ROWNUM`과 함께 사용하는 방법입니다:

```sql
SELECT ROWNUM value
FROM ALL_OBJECTS
WHERE ROWNUM <= 10;
```

이 방법은 큰 범위에서는 위험할 수 있습니다. 시스템 테이블의 행 수가 요청한 범위보다 작을 경우 예상한 결과를 얻지 못할 수 있기 때문입니다.

### 10. MySQL 8.0+의 재귀 CTE

MySQL 8.0부터 재귀 CTE를 지원합니다:

```sql
WITH RECURSIVE seq AS (
    SELECT 1 AS value
    UNION ALL
    SELECT value + 1 FROM seq WHERE value < 10
)
SELECT * FROM seq;
```

### 11. SQL Server의 시스템 테이블 사용

SQL Server에서는 `master.dbo.spt_values` 테이블을 사용할 수 있습니다:

```sql
SELECT TOP 10 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS value
FROM master.dbo.spt_values;
```

또는 `sys.all_objects`를 사용:

```sql
SELECT TOP 10 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS value
FROM sys.all_objects;
```

## jOOQ에서의 사용

jOOQ를 사용하면 데이터베이스 간 이식 가능한 방식으로 시퀀스를 생성할 수 있습니다:

```java
// jOOQ는 각 데이터베이스에 적합한 SQL을 자동으로 생성합니다
DSL.using(configuration)
   .selectFrom(generateSeries(1, 10))
   .fetch();
```

jOOQ의 `generateSeries()` 함수는 대상 데이터베이스에 따라 적절한 SQL로 변환됩니다.

## 결론

SQL은 이 일반적인 작업에 대해 우아한 내장 솔루션이 부족합니다. 표준 SQL에는 네이티브 범위 생성 기능이 없기 때문에 데이터베이스 설계자들은 시스템 테이블, 재귀 로직 또는 수학적 조합을 활용하여 즉흥적으로 해결해야 합니다.

PostgreSQL의 `GENERATE_SERIES()`와 Oracle의 `CONNECT BY`는 대안들보다 우수한 것으로 평가되며, 함수형 프로그래밍 언어와 비교했을 때 SQL의 한계를 보여줍니다. 단순한 순차적 데이터 생성이 프로그래밍 언어 등가물에 비해 불필요하게 복잡해지는 것입니다.

데이터베이스를 선택할 때 이러한 유틸리티 기능의 가용성도 고려해야 할 요소 중 하나입니다. 범위 생성이 자주 필요한 작업이라면 PostgreSQL의 `GENERATE_SERIES()`가 제공하는 편의성은 상당한 이점이 될 수 있습니다.
