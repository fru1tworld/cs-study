# SQL 도구 체인에 LATERAL 조인이나 CROSS APPLY를 추가하라

> 원문: https://blog.jooq.org/add-lateral-joins-or-cross-apply-to-your-sql-tool-chain/

T-SQL은 오랫동안 매우 강력한 `CROSS APPLY`와 `OUTER APPLY` 조인 구문을 지원해왔다. SQL:1999 표준에서는 동일한 기능을 하는 "lateral 파생 테이블(lateral derived tables)"이 도입되었으며, 이는 PostgreSQL 9.3과 Oracle 12c에서 지원되기 시작했다. Oracle은 SQL 표준의 `LATERAL` 구문과 T-SQL 벤더 고유의 변형 구문 모두를 채택했다.

## CROSS APPLY란 무엇인가?

`CROSS APPLY`는 두 테이블 간의 `CROSS JOIN`을 수행하는데, 조인 표현식의 오른쪽에서 왼쪽의 컬럼을 참조할 수 있다는 점이 특별하다. Martin Smith가 Stack Overflow에서 설명한 훌륭한 예제를 살펴보자. 이 예제는 컬럼 별칭을 재사용하는 방법을 보여준다:

```sql
SELECT number,
       doubled_number,
       doubled_number_plus_one
FROM master..spt_values
CROSS APPLY (
  SELECT 2 * CAST(number AS BIGINT)
) CA1(doubled_number)
CROSS APPLY (
  SELECT doubled_number + 1
) CA2(doubled_number_plus_one)
```

이 방식을 사용하면 중첩된 서브쿼리 없이도 이전에 계산된 컬럼으로부터 `doubled_number_plus_one`을 계산할 수 있다.

## 테이블 반환 함수(Table-Valued Function) 예제

다음은 테이블 반환 함수를 적용하는 실용적인 T-SQL 예제이다:

```sql
SELECT *
FROM sys.dm_exec_query_stats AS qs
CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle)
```

## PostgreSQL에서의 구현

PostgreSQL에서는 유사한 작업을 두 가지 방식으로 처리할 수 있다.

첫 번째 방법 - 암시적 접근:

```sql
SELECT x, GENERATE_SERIES(0, x)
FROM (VALUES(0), (1), (2)) t(x)
```

두 번째 방법 - 명시적 LATERAL 구문 (PostgreSQL 9.3 이상):

```sql
SELECT x, y
FROM (VALUES(0), (1), (2)) t(x),
LATERAL GENERATE_SERIES(0, t.x) u(y)
```

두 방식 모두 생성된 시리즈 값에 따라 행이 확장되는 동일한 결과를 생성한다.

## jOOQ 3.3에서의 지원

jOOQ 3.3 버전에서는 이 구문을 지원한다. `crossApply`를 사용하는 예제:

```java
DSL.using(configuration)
   .select()
   .from(AUTHOR)
   .crossApply(
        select(count().as("c"))
       .from(BOOK)
       .where(BOOK.AUTHOR_ID.eq(AUTHOR.ID)))
   .fetch();
```

lateral 조인을 사용하는 예제:

```java
DSL.using(configuration)
   .select()
   .from(
        values(row(0), row(1), row(2))
            .as("t", "x"),
        lateral(generateSeries(0,
                fieldByName("t", "x"))
            .as("u", "y")))
   .fetch();
```

## 핵심 요점

lateral 파생 테이블이나 `CROSS APPLY`는 jOOQ를 사용하든 네이티브 SQL을 사용하든 관계없이 여러분의 멋진 SQL 도구 체인의 일부가 되어야 한다.

---

- 게시일: 2013년 12월 18일
- 업데이트: 2024년 5월 24일
- 저자: Lukas Eder (jOOQ 창시자)
- 태그: CROSS APPLY, jOOQ, Lateral Derived Table, Oracle 12c, OUTER APPLY, SQL, SQL Server, T-SQL
