# Oracle JSON 집계 함수에서 SQL FILTER 에뮬레이션하기

> 원문: https://blog.jooq.org/emulating-sql-filter-with-oracle-json-aggregate-functions/

## 개요

SQL:2003 표준의 유용한 기능 중 하나는 집계 `FILTER` 절로, WHERE 절이 아닌 집계 수준에서 필터링을 수행할 수 있게 해준다. ClickHouse, CockroachDB, DuckDB, Firebird, H2, HSQLDB, PostgreSQL, SQLite, Trino, YugabyteDB 등 다양한 데이터베이스 시스템이 이 기능을 네이티브로 지원하고 있다.

## 기본 FILTER 절 사용법

집계 함수는 특정 조건을 만족하는 그룹별로 값을 계산할 수 있다:

```sql
SELECT
  COUNT(*) FILTER (WHERE BOOK.TITLE LIKE 'A%'),
  COUNT(*) FILTER (WHERE BOOK.TITLE LIKE 'B%'),
  ...
FROM BOOK
```

이 접근 방식은 여러 집계 값을 동시에 계산하는 피벗 스타일 쿼리에 유용하다. 표준 집계 함수의 경우, 집계 함수가 일반적으로 `NULL` 값을 무시하기 때문에 `CASE` 표현식을 사용하여 `FILTER` 절을 에뮬레이션할 수 있다:

```sql
SELECT
  COUNT(CASE WHEN BOOK.TITLE LIKE 'A%' THEN 1 END),
  COUNT(CASE WHEN BOOK.TITLE LIKE 'B%' THEN 1 END),
  ...
FROM BOOK
```

## JSON 집계의 문제점

JSON 데이터를 집계할 때 상황은 더 복잡해진다. 책을 단순히 세는 것이 아니라 JSON 배열이나 객체로 집계하는 경우를 생각해 보자:

```sql
SELECT
  JSON_ARRAYAGG(BOOK.TITLE)
    FILTER (WHERE BOOK.LANGUAGE_ID = 1),
  JSON_OBJECTAGG('id-' || BOOK.ID, BOOK.TITLE)
    FILTER (WHERE BOOK.LANGUAGE_ID = 2),
  ...
FROM BOOK
```

JSON 컬렉션 함수에서는 `NULL` 값이 의미를 가진다. 왜냐하면 결과 JSON 문서 내의 실제 데이터를 나타내기 때문이다. 제목이 `NULL`인 책도 출력에 포함되어야 한다.

표준 `CASE` 표현식 접근 방식은 실패하게 되는데, `ABSENT ON NULL`을 사용하면 합법적인 null 항목까지 제거해 버리기 때문이다. "필터 조건에 의해 걸러진 행"과 "실제로 NULL인 합법적인 값"을 구분하는 것이 문제가 된다.

## 해결 방법: 배열로 데이터 감싸기

해결 방법은 세 단계로 구성된다:

### 1단계 - 감싸기 (Wrap)

`JSON_ARRAY(T_BOOK.TITLE NULL ON NULL)`을 사용하여 합법적인 데이터를 배열로 감싼다. 이렇게 하면 필터링을 준비하면서도 null 값을 보존할 수 있다. 각 값을 -- 합법적인 null을 포함하여 -- 배열 구조로 캡슐화함으로써, 다음 단계에서 의도적인 null과 필터로 제거된 행을 구분할 수 있게 된다.

### 2단계 - 필터링 (Apply)

집계 함수에 `ABSENT ON NULL`을 적용하여 FILTER 에뮬레이션으로 인한 NULL을 제거한다. CASE 표현식이 NULL을 반환하면(필터 조건이 충족되지 않은 경우) `ABSENT ON NULL`이 해당 항목을 집계에서 완전히 제거하여, 배열로 감싸진 합법적인 null 값은 보존하면서 거짓 null 값이 결과에 나타나는 것을 방지한다.

### 3단계 - 풀기 (Unwrap)

`JSON_TRANSFORM`과 `NESTED PATH`를 사용하여 데이터를 다시 배열에서 추출한다. `REPLACE '@' = PATH '@[0]'`과 같은 치환을 사용하여 각 감싸진 배열에서 첫 번째(유일한) 요소를 추출한다.

### 구현 예제

```sql
SELECT
  JSON_TRANSFORM(
    JSON_ARRAYAGG(
      CASE
        WHEN T_BOOK.LANGUAGE_ID = 1
        THEN JSON_ARRAY(T_BOOK.TITLE NULL ON NULL)
      END
      ABSENT ON NULL
    ),
    NESTED PATH '$[*]' (REPLACE '@' = PATH '@[0]')
  ),
  JSON_TRANSFORM(
    JSON_OBJECTAGG(
      'id-' || T_BOOK.ID,
      CASE
        WHEN T_BOOK.LANGUAGE_ID = 2
        THEN JSON_ARRAY(T_BOOK.TITLE NULL ON NULL)
      END
      ABSENT ON NULL
    ),
    NESTED PATH '$.*' (REPLACE '@' = PATH '@[0]')
  )
FROM T_BOOK;
```

위 쿼리에서 동작 원리를 살펴보면:

- `JSON_ARRAYAGG`의 경우: `LANGUAGE_ID = 1`인 행만 집계에 포함된다. 각 제목은 먼저 `JSON_ARRAY()`로 감싸지고, 조건을 만족하지 않는 행은 CASE가 NULL을 반환하여 `ABSENT ON NULL`에 의해 제거된다. 마지막으로 `JSON_TRANSFORM`이 `'$[*]'` 경로를 통해 각 배열 요소를 순회하며 `'@[0]'`으로 내부 값을 추출한다.

- `JSON_OBJECTAGG`의 경우: 동일한 원리가 적용되지만, 경로가 `'$.*'`로 객체의 모든 속성을 순회한다.

## jOOQ 구현

jOOQ 3.20 버전은 `JSON_ARRAYAGG`와 `JSON_OBJECTAGG`에 대해 위의 에뮬레이션을 구현하여, Oracle을 포함한 모든 지원 데이터베이스에서 투명하게 `FILTER` 절을 사용할 수 있도록 한다.
