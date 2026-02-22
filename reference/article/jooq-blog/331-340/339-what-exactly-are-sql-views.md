# SQL 뷰란 정확히 무엇인가?

> 원문: https://blog.jooq.org/what-exactly-are-sql-views/

여러분은 이미 "일반 뷰"에 대해 알고 있을 것입니다. 하지만 이 글에서 한두 가지 이전에 생각해보지 않았던 내용을 발견하게 될 것이라고 확신합니다...

SQL 뷰란 정확히 무엇일까요? SQL의 뷰는 복잡한 쿼리를 "일반" 테이블과 동일한 방식으로 다룰 수 있게 해주는 수단입니다. 사실 SQL은 테이블(레코드의 집합)에 관한 것이며, 이는 관계 대수학이 릴레이션(튜플의 집합)에 관한 것과 같습니다. 뷰에는 여러 가지 유형이 있습니다:

## "일반" 뷰

이것은 가장 흔히 "뷰"라고 불리는 것입니다. 대부분의 데이터베이스는 다음과 같은 구문으로 선언할 수 있습니다:

```sql
CREATE VIEW my_view AS
SELECT col1, col2
FROM my_table
WHERE ...
```

이러한 저장된 뷰는 카탈로그의 일부가 되며 테이블처럼 이름으로 참조할 수 있어 재사용에 매우 유용합니다. 그리고 더 좋은 점은 테이블과는 다른 권한 집합을 뷰에 부여할 수 있어, 뷰만 사용하여 완전한 보안 계층을 구현할 수 있다는 것입니다(예: 특정 사용자로부터 일부 열이나 행을 숨기기).

일부 데이터베이스(Oracle, PostgreSQL 포함)는 특정 조건 하에서 뷰 업데이트도 허용합니다 - 대부분 계산이나 비정규화를 생성하지 않는 단일 테이블의 명확한 1:1 매핑인 경우에 해당합니다.

## 구체화된 뷰 (Materialized Views)

위의 "일반 뷰"와 마찬가지로, 구체화된 뷰도 테이블처럼 사용할 수 있습니다. 사실, 이들은 데이터가 디스크에 구체화되어 저장되고, 내용이 업데이트될 때마다 갱신되는 테이블입니다. 이는 드물게 업데이트되는 데이터에 대한 빈번하고 복잡한 쿼리에 유용합니다. `MATERIALIZED` 키워드만 추가하면 됩니다:

```sql
CREATE MATERIALIZED VIEW my_view AS
SELECT col1, col2
FROM my_table
WHERE ...
```

Oracle과 PostgreSQL 등이 구체화된 뷰를 지원합니다. SQL Server와 같은 다른 데이터베이스는 "인덱싱된 뷰"를 알고 있는데, 이는 뷰 데이터를 인덱스에 명시적으로 "구체화"해야 하므로 약간 덜 강력합니다.

## "스냅샷" 뷰

이것들은 실제로 뷰가 아니라 실제 테이블입니다. 그러나 이 블로그 포스트의 맥락에서, 데이터의 영구적으로 구체화된 "스냅샷" 뷰라고 생각할 수 있습니다. 다양한 구문을 사용하여 이러한 뷰를 생성할 수 있습니다:

대부분의 데이터베이스, 예: Oracle

```sql
CREATE TABLE my_view AS
SELECT col1, col2
FROM my_table
WHERE ...
```

일부 데이터베이스, 예: SQL Server

```sql
SELECT col1, col2
INTO my_view
FROM my_table
WHERE ...
```

이 접근 방식의 좋은 점은 구체화된 뷰와 마찬가지로, 이러한 "뷰"가 빈번한 쿼리에 매우 유용할 수 있다는 것입니다 - 데이터를 한 번만 미리 계산하면 됩니다. 하지만 일단 그 데이터를 계산하면 "스냅샷"을 생성하게 되고, 데이터는 뷰와 독립적으로 계속 존재할 수 있습니다 - 마치 스냅샷처럼! (관련 인덱스를 추가하는 것을 잊지 마세요)

DB2와 Oracle을 포함한 일부 데이터베이스는 실제 SQL:2011 표준 "스냅샷"을 지원합니다. 예를 들어 Oracle의 플래시백 쿼리나 DB2의 시간 여행 쿼리가 있습니다. 하지만 그것은 다른 이야기입니다.

## 매개변수화된 뷰 (Parameterized Views)

이러한 뷰를 "뷰"라고 부르는 사람은 거의 없지만, 생각해보면 그것이 바로 뷰의 본질입니다. 테이블 반환 함수는 SQL에서 다시 사용할 수 있는 테이블을 반환하는 저장 프로시저입니다. 예를 들어 (PostgreSQL 구문 사용):

```sql
CREATE FUNCTION my_view (arg1 INTEGER, arg2 INTEGER)
RETURNS TABLE (
    col1 INTEGER
    col2 INTEGER
)
AS $$
BEGIN
    RETURN QUERY
    SELECT col1, col2
    FROM my_table
    WHERE v1 = arg2 AND v2 = arg2;
END
$$ LANGUAGE plpgsql;
```

그리고 이렇게 사용합니다:

```sql
SELECT *
FROM my_view (42, 1337)
WHERE ...
```

꽤 강력하지 않나요? Firebird, HANA, HSQLDB, Oracle, PostgreSQL, SQL Server 등이 테이블 반환 함수를 지원합니다.

## 공통 테이블 표현식 (Common Table Expressions)

일반 뷰와 마찬가지로, 이 뷰들은 이름이 지정되어 있지만 단일 문 - 주로 SELECT 문 - 에서만 유효합니다. 다만 PostgreSQL이나 SQL Server는 공통 테이블 표현식을 다른 DML 문과 함께 사용하는 것도 허용합니다. 이러한 "뷰"는 다음과 같이 작성할 수 있습니다:

```sql
WITH
    my_view_a AS (
        SELECT ...
    ),
    my_view_b AS (
        SELECT ...
    )
-- 문에서 즉시 사용됨
SELECT *
FROM my_view_a, my_view_b
```

공통 테이블 표현식은 코드 구조화에 매우 유용하지만(마치 "테이블 변수"처럼), Oracle이나 PostgreSQL에서는 대가가 따릅니다. 뷰가 대부분 임시로 구체화되어 옵티마이저에서 많은 SQL 변환을 방해하기 때문입니다. 반면에, 공통 테이블 표현식은 재귀적/계층적일 수 있어 그래프/트리 순회에 매우 유용합니다.

## 파생 테이블 (Derived Tables)

가장 흔한 유형의 뷰(비록 거의 "뷰"라고 불리지 않지만)는 파생 테이블입니다. 즉, FROM 절에 넣는 모든 중첩된 select 문입니다. 예를 들어:

```sql
SELECT *
FROM (
    SELECT ...
) my_view_a, (
    SELECT ...
) my_view_b
```

공통 테이블 표현식과 달리, 파생 테이블은 문 내에서 쉽게 재사용할 수 없지만, 더 높은 성능을 가진 다른 문으로 최적화될 가능성이 높습니다.

## 결론

SQL은 테이블과 임시 쿼리에서의 테이블 재구성에 관한 것입니다. 모든 SQL 문에서 가장 중요한 절은 `FROM` 절입니다. 이것은 다양한 방식으로 재구성, 필터링, 그룹화, 프로젝션하려는 튜플의 집합을 지정합니다. 위에서 본 것처럼, 위의 뷰 생성 방법 중 하나를 통해 이러한 테이블 변환을 다른 변환에 쉽게 공급할 수 있습니다.
