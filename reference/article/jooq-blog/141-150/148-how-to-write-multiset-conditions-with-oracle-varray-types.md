# Oracle VARRAY 타입으로 Multiset 조건을 작성하는 방법

> 원문: https://blog.jooq.org/how-to-write-multiset-conditions-with-oracle-varray-types/

Oracle은 중첩 컬렉션을 가능하게 하는 SQL 표준 ORDBMS 확장을 구현하고 있습니다. 두 가지 컬렉션 타입이 존재합니다:

중첩 테이블(Nested tables):

```sql
CREATE TYPE t1 AS TABLE OF VARCHAR2(10);
```

VARRAY:

```sql
CREATE TYPE t2 AS VARRAY(10) OF VARCHAR2(10);
```

중첩 테이블은 임의의 크기를 지원하고, VARRAY는 고정된 최대 크기를 가집니다. 중첩 컬렉션을 저장할 때, VARRAY는 테이블에 인라인으로 저장할 수 있지만, 중첩 테이블은 추가적인 스토리지 절(storage clause)이 필요합니다.

## 핵심 이슈: Multiset 조건

결정적인 차이점은 multiset 조건이 VARRAY에서는 작동하지 않는다는 것입니다. 다음 쿼리들을 비교해 보세요:

```sql
SELECT * FROM t WHERE 'abc' MEMBER OF t1;
SELECT * FROM t WHERE 'abc' MEMBER OF t2;
```

첫 번째 쿼리는 성공하지만, 두 번째 쿼리는 "ORA-00932: inconsistent datatypes" 오류를 발생시킵니다.

## IS A SET 조건

중첩 테이블 버전:

```sql
SELECT * FROM t WHERE t1 IS A SET;
```

VARRAY 동등 구현:

```sql
SELECT *
FROM t
WHERE t2 IS NOT NULL
AND (SELECT count(*) FROM TABLE(t2))
  = (SELECT count(DISTINCT column_value) FROM TABLE(t2));
```

이것은 VARRAY의 전체 값 개수와 고유한(distinct) 값 개수를 비교합니다.

## IS EMPTY 조건

중첩 테이블 버전:

```sql
SELECT * FROM t WHERE t1 IS EMPTY;
```

VARRAY 동등 구현:

```sql
SELECT *
FROM t
WHERE t2 IS NOT NULL
AND NOT EXISTS (
  SELECT * FROM TABLE (t2)
);
```

## MEMBER 조건

중첩 테이블 버전:

```sql
SELECT * FROM t WHERE 'abc' MEMBER OF t1;
```

VARRAY 동등 구현:

```sql
SELECT *
FROM t
WHERE t2 IS NOT NULL
AND EXISTS (
  SELECT 1 FROM TABLE(t2) WHERE column_value = 'abc'
);
```

## SUBMULTISET 조건

집합(set)과 같은 동작을 위한 구현:

중첩 테이블 버전:

```sql
SELECT * FROM t WHERE t1('abc', 'xyz') SUBMULTISET OF t1;
```

VARRAY 동등 구현:

```sql
SELECT *
FROM t
WHERE t2 IS NOT NULL
AND EXISTS (
  SELECT 1 FROM TABLE(t2)
  WHERE column_value = 'abc'
  INTERSECT
  SELECT 1 FROM TABLE(t2)
  WHERE column_value = 'xyz'
);
```

중복을 고려한 진정한 multiset 동작을 위한 구현:

```sql
SELECT *
FROM t
WHERE t2 IS NOT NULL
AND NOT EXISTS (
  SELECT column_value, count(*)
  FROM TABLE (t2('dup', 'dup')) x
  GROUP BY column_value
  HAVING count(*) > (
    SELECT count(*)
    FROM TABLE (t2) y
    WHERE y.column_value = x.column_value
  )
);
```

이것은 왼쪽 피연산자의 어떤 값도 오른쪽 피연산자보다 더 자주 나타나지 않는지 검증합니다.

## MULTISET EXCEPT (DISTINCT 버전)

중첩 테이블 버전:

```sql
SELECT t1 MULTISET EXCEPT DISTINCT t1('aaa', 'abc', 'dup', 'dup') FROM t;
```

VARRAY 동등 구현:

```sql
SELECT
  id,
  CASE
    WHEN t2 IS NULL THEN NULL
    ELSE
      CAST(MULTISET(
        SELECT column_value
        FROM TABLE (t2)
        MINUS
        SELECT column_value
        FROM TABLE (t2('aaa', 'abc', 'dup', 'dup'))
      ) AS t2)
  END r
FROM t;
```

## MULTISET EXCEPT (ALL 버전)

중첩 테이블 버전:

```sql
SELECT t1 MULTISET EXCEPT ALL t1('aaa', 'abc', 'dup', 'dup') FROM t;
```

VARRAY 동등 구현:

```sql
SELECT
  id,
  CASE
    WHEN t2 IS NULL THEN NULL
    ELSE
      CAST(MULTISET(
        SELECT column_value
        FROM (
          SELECT
            column_value,
            row_number() OVER (
              PARTITION BY column_value
              ORDER BY column_value) rn
          FROM TABLE (t2)
          MINUS
          SELECT
            column_value,
            row_number() OVER (
              PARTITION BY column_value
              ORDER BY column_value) rn
          FROM TABLE (t2('aaa', 'abc', 'dup', 'dup'))
        )
      ) AS t2)
  END r
FROM t;
```

윈도우 함수 접근 방식은 중복 값에 고유한 행 번호를 할당하여 multiset을 집합으로 변환함으로써, 적절한 차집합 연산을 가능하게 합니다.

## 결론

중첩 컬렉션은 강력한 Oracle SQL 기능입니다. 중첩 테이블은 명시적인 스토리지 관리가 필요하고, VARRAY는 테이블에 직접 임베드됩니다. 그러나 VARRAY는 기본적으로 multiset 조건과 연산자를 지원하지 않습니다. 이 글에서는 표준 SQL 구문인 EXISTS 절, INTERSECT/MINUS 연산자, 그리고 윈도우 함수를 사용하여 스키마 제약으로 인해 중첩 테이블을 사용할 수 없을 때 VARRAY로 동등한 기능을 구현하는 포괄적인 에뮬레이션 기법을 설명했습니다.
