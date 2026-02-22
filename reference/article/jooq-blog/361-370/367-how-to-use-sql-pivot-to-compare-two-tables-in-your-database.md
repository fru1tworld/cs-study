# SQL PIVOT으로 데이터베이스의 두 테이블을 비교하는 방법

> 원문: https://blog.jooq.org/how-to-use-sql-pivot-to-compare-two-tables-in-your-database/

게시일: 2015년 2월 26일
저자: lukaseder

## 문제 상황

스키마 수정은 관련 테이블 간에 불일치를 초래할 수 있습니다. ALTER TABLE 문이 한 테이블에는 컬럼을 추가하고 다른 테이블에는 추가하지 않으면, 이후 작업이 예기치 않게 실패할 수 있습니다:

```sql
ALTER TABLE payments ADD code NUMBER(3);
```

구조가 다른 두 테이블은 대량 작업을 시도할 때 오류를 발생시킵니다:

```sql
CREATE TABLE payments (
  id         NUMBER(18) NOT NULL,
  account_id NUMBER(18) NOT NULL,
  value_date DATE,
  amount     NUMBER(25, 2) NOT NULL
);

CREATE TABLE payments_archive (
  id         NUMBER(18) NOT NULL,
  account_id NUMBER(18) NOT NULL,
  value_date DATE,
  amount     NUMBER(25, 2) NOT NULL
);

INSERT INTO payments_archive
SELECT * FROM payments
WHERE value_date < SYSDATE - 30;
```

이 코드는 다음 오류를 발생시킵니다: `ORA-00913: too many values`

## 해결책: PIVOT 사용하기

딕셔너리 뷰 쿼리로 스키마 차이점을 식별할 수 있지만, 가독성이 떨어집니다:

```sql
SELECT
  table_name,
  column_name
FROM all_tab_cols
WHERE table_name LIKE 'PAYMENTS%'
```

PIVOT 접근 방식은 더 명확한 출력을 생성합니다:

```sql
SELECT *
FROM (
  SELECT
    table_name,
    column_name
  FROM all_tab_cols
  WHERE table_name LIKE 'PAYMENTS%'
)
PIVOT (
  COUNT(*) AS cnt
  FOR (table_name)
  IN (
    'PAYMENTS' AS payments,
    'PAYMENTS_ARCHIVE' AS payments_archive
  )
) t;
```

결과:

```
COLUMN_NAME      PAYMENTS_CNT  PAYMENTS_ARCHIVE_CNT
CODE                       1                     0
ACCOUNT_ID                 1                     1
ID                         1                     1
VALUE_DATE                 1                     1
AMOUNT                     1                     1
```

PAYMENTS_ARCHIVE에서 CODE 컬럼이 누락된 것이 즉시 확인됩니다.

## PIVOT 구문 이해하기

PIVOT 절은 행을 열로 변환합니다. 주요 구성 요소:

- 집계 함수: `COUNT(*)`는 각 피벗 컬럼에 대한 값을 생성합니다
- FOR 절: 피벗할 컬럼을 지정합니다 (`table_name`)
- IN 절: 허용되는 값과 결과 컬럼 이름을 정의합니다

## 데이터 타입을 포함한 향상된 비교

```sql
SELECT *
FROM (
  SELECT
    table_name,
    column_name,
    CAST(data_type AS varchar(6)) data_type
  FROM all_tab_cols
  WHERE table_name LIKE 'PAYMENTS%'
)
PIVOT (
  COUNT(*) AS cnt,
  MAX(data_type) AS type
  FOR (table_name)
  IN (
    'PAYMENTS' AS p,
    'PAYMENTS_ARCHIVE' AS a
  )
) t;
```

결과:

```
COLUMN_NAME    P_CNT  P_TYPE      A_CNT  A_TYPE
CODE               1  NUMBER          0
ACCOUNT_ID         1  NUMBER          1  NUMBER
ID                 1  NUMBER          1  NUMBER
VALUE_DATE         1  DATE            1  TIMESTAMP
AMOUNT             1  NUMBER          1  NUMBER
```

이를 통해 누락된 컬럼과 타입 불일치(DATE vs TIMESTAMP) 모두를 확인할 수 있습니다.

## 데이터베이스 호환성

모든 데이터베이스가 PIVOT을 지원하는 것은 아닙니다. GROUP BY와 CASE를 사용한 대안:

```sql
SELECT
  t.column_name,
  COUNT(CASE table_name
        WHEN 'PAYMENTS' THEN 1 END) p_cnt,
  MAX(CASE table_name
        WHEN 'PAYMENTS' THEN data_type END) p_type,
  COUNT(CASE table_name
        WHEN 'PAYMENTS_ARCHIVE' THEN 1 END) a_cnt,
  MAX(CASE table_name
        WHEN 'PAYMENTS_ARCHIVE' THEN data_type END) a_type
FROM (
  SELECT
    table_name,
    column_name,
    data_type
  FROM all_tab_cols
  WHERE table_name LIKE 'PAYMENTS%'
) t
GROUP BY t.column_name;
```

PostgreSQL 9.4 이상에서는 FILTER 절을 지원합니다:

```sql
SELECT
  t.column_name,
  COUNT(table_name)
    FILTER (WHERE table_name = 'PAYMENTS') p_cnt,
  MAX(data_type)
    FILTER (WHERE table_name = 'PAYMENTS') p_type,
  COUNT(table_name)
    FILTER (WHERE table_name = 'PAYMENTS_ARCHIVE') a_cnt,
  MAX(data_type)
    FILTER (WHERE table_name = 'PAYMENTS_ARCHIVE') a_type
FROM (
  SELECT
    table_name,
    column_name,
    data_type
  FROM information_schema.columns
  WHERE table_name LIKE 'payments%'
) t
GROUP BY t.column_name;
```
