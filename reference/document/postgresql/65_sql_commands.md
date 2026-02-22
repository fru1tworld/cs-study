# PostgreSQL SQL 명령어 참조 (SQL Commands Reference)

이 문서는 PostgreSQL 공식 문서를 기반으로 주요 SQL 명령어를 한국어로 설명합니다.

## 목차

1. [데이터 조작 명령어 (DML)](#1-데이터-조작-명령어-dml)
2. [데이터 정의 명령어 (DDL)](#2-데이터-정의-명령어-ddl)
3. [권한 관리 명령어 (DCL)](#3-권한-관리-명령어-dcl)
4. [트랜잭션 제어 명령어 (TCL)](#4-트랜잭션-제어-명령어-tcl)
5. [기타 유틸리티 명령어](#5-기타-유틸리티-명령어)

---

## 1. 데이터 조작 명령어 (DML)

데이터 조작 언어(Data Manipulation Language)는 테이블의 데이터를 조회, 삽입, 수정, 삭제하는 명령어입니다.

### 1.1 SELECT - 데이터 조회

`SELECT`는 테이블이나 뷰에서 행(row)을 조회하는 명령어입니다.

#### 기본 구문 (Syntax)

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
SELECT [ ALL | DISTINCT [ ON ( expression [, ...] ) ] ]
    [ { * | expression [ [ AS ] output_name ] } [, ...] ]
    [ FROM from_item [, ...] ]
    [ WHERE condition ]
    [ GROUP BY [ ALL | DISTINCT ] grouping_element [, ...] ]
    [ HAVING condition ]
    [ WINDOW window_name AS ( window_definition ) [, ...] ]
    [ { UNION | INTERSECT | EXCEPT } [ ALL | DISTINCT ] select ]
    [ ORDER BY expression [ ASC | DESC | USING operator ] [ NULLS { FIRST | LAST } ] [, ...] ]
    [ LIMIT { count | ALL } ]
    [ OFFSET start [ ROW | ROWS ] ]
    [ FETCH { FIRST | NEXT } [ count ] { ROW | ROWS } { ONLY | WITH TIES } ]
    [ FOR { UPDATE | NO KEY UPDATE | SHARE | KEY SHARE } [ OF from_reference [, ...] ] [ NOWAIT | SKIP LOCKED ] [...] ]
```

#### 처리 순서

SELECT 문의 일반적인 처리 순서는 다음과 같습니다:

1. WITH 절: FROM에서 참조할 수 있는 임시 테이블 계산
2. FROM 절: 모든 소스 테이블을 계산하고 여러 개일 경우 크로스 조인
3. WHERE 절: 조건을 만족하지 않는 행 제거
4. GROUP BY/HAVING: 행을 그룹으로 결합하고 그룹 필터링
5. SELECT 목록: 각 행 또는 그룹에 대한 출력 표현식 계산
6. DISTINCT: 중복 행 제거 (지정된 경우)
7. 집합 연산: UNION, INTERSECT, EXCEPT로 결과 결합
8. ORDER BY: 결과 행 정렬
9. LIMIT/OFFSET: 결과 행의 부분집합 반환
10. 잠금 절: 선택된 행에 대한 동시 업데이트 잠금

#### 주요 절 설명

| 절 (Clause) | 설명 |
|-------------|------|
| `SELECT` | 조회할 열(column) 지정 |
| `FROM` | 데이터를 가져올 테이블 지정 |
| `WHERE` | 행 필터링 조건 |
| `GROUP BY` | 집계를 위한 그룹화 |
| `HAVING` | 그룹 필터링 조건 |
| `ORDER BY` | 결과 정렬 |
| `LIMIT/OFFSET` | 결과 행 수 제한 |
| `DISTINCT` | 중복 제거 |

#### 예제

기본 조인 (Basic Join)
```sql
SELECT f.title, f.did, d.name, f.date_prod, f.kind
FROM distributors d
JOIN films f USING (did);
```

집계 함수와 GROUP BY
```sql
SELECT kind, sum(len) AS total
FROM films
GROUP BY kind;
```

HAVING을 사용한 그룹 필터링
```sql
SELECT kind, sum(len) AS total
FROM films
GROUP BY kind
HAVING sum(len) < interval '5 hours';
```

ORDER BY로 정렬
```sql
-- 열 이름으로 정렬
SELECT * FROM distributors ORDER BY name;

-- 열 순서(ordinal position)로 정렬
SELECT * FROM distributors ORDER BY 2;
```

UNION으로 결과 결합
```sql
SELECT distributors.name FROM distributors
WHERE distributors.name LIKE 'W%'
UNION
SELECT actors.name FROM actors
WHERE actors.name LIKE 'W%';
```

WITH 절 (CTE: Common Table Expression)
```sql
WITH t AS (
    SELECT random() as x FROM generate_series(1, 3)
)
SELECT * FROM t
UNION ALL
SELECT * FROM t;
```

재귀 WITH (Recursive CTE)
```sql
WITH RECURSIVE employee_recursive(distance, employee_name, manager_name) AS (
    SELECT 1, employee_name, manager_name
    FROM employee
    WHERE manager_name = 'Mary'
  UNION ALL
    SELECT er.distance + 1, e.employee_name, e.manager_name
    FROM employee_recursive er, employee e
    WHERE er.employee_name = e.manager_name
)
SELECT distance, employee_name FROM employee_recursive;
```

LATERAL 조인
```sql
SELECT m.name AS mname, pname
FROM manufacturers m, LATERAL get_product_names(m.id) pname;
```

---

### 1.2 INSERT - 데이터 삽입

`INSERT`는 테이블에 새로운 행을 삽입하는 명령어입니다.

#### 기본 구문

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
INSERT INTO table_name [ AS alias ] [ ( column_name [, ...] ) ]
    [ OVERRIDING { SYSTEM | USER } VALUE ]
    { DEFAULT VALUES | VALUES ( { expression | DEFAULT } [, ...] ) [, ...] | query }
    [ ON CONFLICT [ conflict_target ] conflict_action ]
    [ RETURNING [ WITH ( { OLD | NEW } AS output_alias [, ...] ) ]
                { * | output_expression [ [ AS ] output_name ] } [, ...] ]
```

#### 주요 특징

- 열 이름은 임의의 순서로 나열 가능
- 생략된 열은 기본값(DEFAULT) 또는 NULL로 채워짐
- 데이터 타입이 맞지 않으면 자동 타입 변환 시도
- `RETURNING` 절로 삽입/수정된 행의 계산된 값 반환 가능
- `ON CONFLICT`로 유니크 제약조건 위반 시 대체 동작 지정 가능

#### 예제

단일 행 삽입
```sql
INSERT INTO films VALUES
    ('UA502', 'Bananas', 105, '1971-07-13', 'Comedy', '82 minutes');
```

기본값 사용
```sql
INSERT INTO films (code, title, did, date_prod, kind)
    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');

-- 모든 열에 기본값 사용
INSERT INTO films DEFAULT VALUES;
```

다중 행 삽입
```sql
INSERT INTO films (code, title, did, date_prod, kind) VALUES
    ('B6717', 'Tampopo', 110, '1985-02-10', 'Comedy'),
    ('HG120', 'The Dinner Game', 140, DEFAULT, 'Comedy');
```

SELECT 쿼리로 삽입
```sql
INSERT INTO films SELECT * FROM tmp_films WHERE date_prod < '2004-05-07';
```

UPSERT - 충돌 시 업데이트 (ON CONFLICT)
```sql
INSERT INTO distributors (did, dname)
    VALUES (5, 'Gizmo Transglobal'), (6, 'Associated Computing, Inc')
    ON CONFLICT (did) DO UPDATE SET dname = EXCLUDED.dname;
```

충돌 시 무시 (ON CONFLICT DO NOTHING)
```sql
INSERT INTO distributors (did, dname) VALUES (7, 'Redline GmbH')
    ON CONFLICT (did) DO NOTHING;
```

RETURNING 절로 삽입된 값 반환
```sql
INSERT INTO distributors (did, dname) VALUES (DEFAULT, 'XYZ Widgets')
   RETURNING did;
```

---

### 1.3 UPDATE - 데이터 수정

`UPDATE`는 조건을 만족하는 행의 열 값을 변경하는 명령어입니다.

#### 기본 구문

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
UPDATE [ ONLY ] table_name [ * ] [ [ AS ] alias ]
    SET { column_name = { expression | DEFAULT } |
          ( column_name [, ...] ) = [ ROW ] ( { expression | DEFAULT } [, ...] ) |
          ( column_name [, ...] ) = ( sub-SELECT )
        } [, ...]
    [ FROM from_item [, ...] ]
    [ WHERE condition | WHERE CURRENT OF cursor_name ]
    [ RETURNING [ WITH ( { OLD | NEW } AS output_alias [, ...] ) ]
                { * | output_expression [ [ AS ] output_name ] } [, ...] ]
```

#### 주요 특징

- SET 절에 명시된 열만 수정됨; 다른 열은 이전 값 유지
- RETURNING 절로 수정된 행의 값 반환 가능 (기본적으로 새 값, OLD 키워드로 이전 값)

#### 예제

기본 UPDATE
```sql
UPDATE films SET kind = 'Dramatic' WHERE kind = 'Drama';
```

여러 열 수정 및 DEFAULT 사용
```sql
UPDATE weather SET temp_lo = temp_lo+1, temp_hi = temp_lo+15, prcp = DEFAULT
  WHERE city = 'San Francisco' AND date = '2003-07-03';
```

RETURNING 절로 수정 전후 값 반환
```sql
UPDATE weather SET temp_lo = temp_lo+1, temp_hi = temp_lo+15, prcp = DEFAULT
  WHERE city = 'San Francisco' AND date = '2003-07-03'
  RETURNING temp_lo, temp_hi, prcp, old.prcp AS old_prcp;
```

FROM 절을 사용한 조인 UPDATE
```sql
UPDATE employees SET sales_count = sales_count + 1 FROM accounts
  WHERE accounts.name = 'Acme Corporation'
  AND employees.id = accounts.sales_person;
```

서브쿼리 사용
```sql
UPDATE employees SET sales_count = sales_count + 1 WHERE id =
  (SELECT sales_person FROM accounts WHERE name = 'Acme Corporation');
```

CTE와 LIMIT을 사용한 배치 UPDATE
```sql
WITH exceeded_max_retries AS (
  SELECT w.ctid FROM work_item AS w
    WHERE w.status = 'active' AND w.num_retries > 10
    ORDER BY w.retry_timestamp
    FOR UPDATE
    LIMIT 5000
)
UPDATE work_item SET status = 'failed'
  FROM exceeded_max_retries AS emr
  WHERE work_item.ctid = emr.ctid;
```

---

### 1.4 DELETE - 데이터 삭제

`DELETE`는 테이블에서 조건을 만족하는 행을 삭제하는 명령어입니다.

#### 기본 구문

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
DELETE FROM [ ONLY ] table_name [ * ] [ [ AS ] alias ]
    [ USING from_item [, ...] ]
    [ WHERE condition | WHERE CURRENT OF cursor_name ]
    [ RETURNING [ WITH ( { OLD | NEW } AS output_alias [, ...] ) ]
                { * | output_expression [ [ AS ] output_name ] } [, ...] ]
```

#### 주요 특징

- WHERE 절이 없으면 테이블의 모든 행이 삭제됨
- 대상 테이블에 DELETE 권한 필요
- USING 절의 테이블에는 SELECT 권한 필요
- 모든 행을 빠르게 삭제하려면 `TRUNCATE` 사용 권장

#### 예제

조건부 삭제
```sql
DELETE FROM films WHERE kind <> 'Musical';
```

전체 테이블 삭제
```sql
DELETE FROM films;
```

RETURNING 절로 삭제된 행 반환
```sql
DELETE FROM tasks WHERE status = 'DONE' RETURNING *;
```

커서 위치의 행 삭제
```sql
DELETE FROM tasks WHERE CURRENT OF c_tasks;
```

USING 절을 사용한 조인 DELETE
```sql
DELETE FROM films USING producers
  WHERE producer_id = producers.id AND producers.name = 'foo';
```

CTE와 LIMIT을 사용한 배치 DELETE
```sql
WITH delete_batch AS (
  SELECT l.ctid FROM user_logs AS l
    WHERE l.status = 'archived'
    ORDER BY l.creation_date
    FOR UPDATE
    LIMIT 10000
)
DELETE FROM user_logs AS dl
  USING delete_batch AS del
  WHERE dl.ctid = del.ctid;
```

---

### 1.5 TRUNCATE - 테이블 비우기

`TRUNCATE`는 테이블의 모든 행을 빠르게 삭제하는 명령어입니다.

#### 기본 구문

```sql
TRUNCATE [ TABLE ] [ ONLY ] name [ * ] [, ... ]
    [ RESTART IDENTITY | CONTINUE IDENTITY ] [ CASCADE | RESTRICT ]
```

#### DELETE와의 차이점

| 특성 | TRUNCATE | DELETE |
|------|----------|--------|
| 속도 | 매우 빠름 (행 개수와 무관) | 행 수에 비례 |
| 트랜잭션 안전성 | 지원 | 지원 |
| MVCC 가시성 | 테이블 수준 잠금 | 행 수준 잠금 |
| 트리거 실행 | TRUNCATE 트리거만 | DELETE 트리거 |
| 조건 지정 | 불가능 | WHERE 절 사용 가능 |

#### 예제

```sql
-- 단일 테이블 비우기
TRUNCATE bigtable;

-- 시퀀스 재시작과 함께 비우기
TRUNCATE bigtable RESTART IDENTITY;

-- 여러 테이블을 CASCADE로 비우기
TRUNCATE bigtable, othertable CASCADE;
```

---

### 1.6 MERGE - 조건부 삽입/수정/삭제

`MERGE`는 조건에 따라 INSERT, UPDATE, DELETE를 수행하는 명령어입니다 (PostgreSQL 15+).

#### 기본 구문

```sql
MERGE INTO target_table [ [ AS ] target_alias ]
USING data_source [ [ AS ] source_alias ]
ON join_condition
when_clause [...]
```

#### 예제

```sql
MERGE INTO customer_account ca
USING recent_transactions t
ON t.customer_id = ca.customer_id
WHEN MATCHED THEN
  UPDATE SET balance = balance + transaction_value
WHEN NOT MATCHED THEN
  INSERT (customer_id, balance)
  VALUES (t.customer_id, t.transaction_value);
```

---

## 2. 데이터 정의 명령어 (DDL)

데이터 정의 언어(Data Definition Language)는 데이터베이스 객체를 생성, 수정, 삭제하는 명령어입니다.

### 2.1 CREATE TABLE - 테이블 생성

`CREATE TABLE`은 새로운 테이블을 생성하는 명령어입니다.

#### 기본 구문

```sql
CREATE [ [ GLOBAL | LOCAL ] { TEMPORARY | TEMP } | UNLOGGED ] TABLE [ IF NOT EXISTS ] table_name (
  { column_name data_type [ STORAGE { PLAIN | EXTERNAL | EXTENDED | MAIN | DEFAULT } ]
    [ COMPRESSION compression_method ] [ COLLATE collation ] [ column_constraint [ ... ] ]
    | table_constraint
    | LIKE source_table [ like_option ... ] }
    [, ... ]
)
[ INHERITS ( parent_table [, ... ] ) ]
[ PARTITION BY { RANGE | LIST | HASH } ( { column_name | ( expression ) } [ COLLATE collation ] [ opclass ] [, ... ] ) ]
[ USING method ]
[ WITH ( storage_parameter [= value] [, ... ] ) | WITHOUT OIDS ]
[ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
[ TABLESPACE tablespace_name ]
```

#### 주요 제약조건 (Constraints)

| 제약조건 | 설명 |
|----------|------|
| `NOT NULL` | 열에 NULL 값 불허 |
| `UNIQUE` | 열의 모든 값이 고유해야 함 |
| `PRIMARY KEY` | 행의 고유 식별자 (NOT NULL + UNIQUE) |
| `CHECK` | 열 값이 불리언 표현식을 만족해야 함 |
| `DEFAULT` | 열의 기본값 지정 |
| `FOREIGN KEY` | 다른 테이블의 열을 참조 |
| `EXCLUDE` | 지정된 조건이 참이 되는 것을 방지 |

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `TEMPORARY/TEMP` | 세션 또는 트랜잭션 종료 시 자동 삭제 |
| `UNLOGGED` | WAL에 기록하지 않음 (빠르지만 장애 시 데이터 손실 가능) |
| `IF NOT EXISTS` | 테이블이 이미 존재할 경우 오류 방지 |
| `PARTITION BY` | 테이블을 파티션으로 분할 (RANGE, LIST, HASH) |
| `INHERITS` | 부모 테이블에서 열 상속 |
| `TABLESPACE` | 테이블 저장 위치 지정 |

#### 예제

기본 테이블 생성
```sql
CREATE TABLE films (
    code        char(5) CONSTRAINT firstkey PRIMARY KEY,
    title       varchar(40) NOT NULL,
    did         integer NOT NULL,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute
);
```

자동 증가 ID가 있는 테이블
```sql
CREATE TABLE distributors (
    did    integer PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    name   varchar(40) NOT NULL CHECK (name <> '')
);
```

UNIQUE 제약조건
```sql
CREATE TABLE films (
    code        char(5),
    title       varchar(40),
    did         integer,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute,
    CONSTRAINT production UNIQUE(date_prod)
);
```

CHECK 제약조건
```sql
CREATE TABLE distributors (
    did     integer CHECK (did > 100),
    name    varchar(40),
    CONSTRAINT con1 CHECK (did > 100 AND name <> '')
);
```

외래 키 (Foreign Key)
```sql
CREATE TABLE orders (
    order_id    integer PRIMARY KEY,
    customer_id integer REFERENCES customers(id),
    order_date  date NOT NULL
);
```

RANGE 파티션 테이블
```sql
CREATE TABLE measurement (
    logdate         date NOT NULL,
    peaktemp        int,
    unitsales       int
) PARTITION BY RANGE (logdate);

-- 파티션 생성
CREATE TABLE measurement_y2016m07
    PARTITION OF measurement (
    unitsales DEFAULT 0
) FOR VALUES FROM ('2016-07-01') TO ('2016-08-01');
```

HASH 파티션 테이블
```sql
CREATE TABLE orders (
    order_id     bigint NOT NULL,
    cust_id      bigint NOT NULL,
    status       text
) PARTITION BY HASH (order_id);

CREATE TABLE orders_p1 PARTITION OF orders FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE orders_p2 PARTITION OF orders FOR VALUES WITH (MODULUS 4, REMAINDER 1);
```

기본값과 시퀀스
```sql
CREATE TABLE distributors (
    name      varchar(40) DEFAULT 'Luso Films',
    did       integer DEFAULT nextval('distributors_serial'),
    modtime   timestamp DEFAULT current_timestamp
);
```

EXCLUSION 제약조건
```sql
CREATE TABLE circles (
    c circle,
    EXCLUDE USING gist (c WITH &&)
);
```

---

### 2.2 ALTER TABLE - 테이블 수정

`ALTER TABLE`은 기존 테이블의 정의를 변경하는 명령어입니다.

#### 기본 구문

```sql
ALTER TABLE [ IF EXISTS ] [ ONLY ] name [ * ]
    action [, ... ]

ALTER TABLE [ IF EXISTS ] [ ONLY ] name [ * ]
    RENAME [ COLUMN ] column_name TO new_column_name

ALTER TABLE [ IF EXISTS ] [ ONLY ] name [ * ]
    RENAME CONSTRAINT constraint_name TO new_constraint_name

ALTER TABLE [ IF EXISTS ] name
    RENAME TO new_name

ALTER TABLE [ IF EXISTS ] name
    SET SCHEMA new_schema

ALTER TABLE [ IF EXISTS ] name
    ATTACH PARTITION partition_name { FOR VALUES partition_bound_spec | DEFAULT }

ALTER TABLE [ IF EXISTS ] name
    DETACH PARTITION partition_name [ CONCURRENTLY | FINALIZE ]
```

#### 주요 액션

| 액션 | 설명 |
|------|------|
| `ADD COLUMN` | 새 열 추가 |
| `DROP COLUMN` | 열 삭제 |
| `ALTER COLUMN ... SET DATA TYPE` | 열의 데이터 타입 변경 |
| `ALTER COLUMN ... SET/DROP DEFAULT` | 기본값 설정 또는 제거 |
| `ALTER COLUMN ... SET/DROP NOT NULL` | NULL 제약조건 수정 |
| `ADD/DROP CONSTRAINT` | 제약조건 추가 또는 제거 |
| `RENAME` | 테이블, 열, 제약조건 이름 변경 |
| `SET SCHEMA` | 테이블을 다른 스키마로 이동 |
| `SET TABLESPACE` | 테이블의 테이블스페이스 변경 |
| `SET OWNER TO` | 테이블 소유자 변경 |

#### 예제

열 추가
```sql
ALTER TABLE distributors ADD COLUMN address varchar(30);

-- NOT NULL 기본값과 함께 추가
ALTER TABLE measurements
  ADD COLUMN mtime timestamp with time zone DEFAULT now();
```

열 타입 변경
```sql
ALTER TABLE distributors
    ALTER COLUMN address TYPE varchar(80),
    ALTER COLUMN name TYPE varchar(100);

-- 타입 변환이 필요한 경우
ALTER TABLE foo
    ALTER COLUMN foo_timestamp SET DATA TYPE timestamp with time zone
    USING timestamp with time zone 'epoch' + foo_timestamp * interval '1 second';
```

열 삭제
```sql
ALTER TABLE distributors DROP COLUMN address RESTRICT;
```

이름 변경
```sql
-- 열 이름 변경
ALTER TABLE distributors RENAME COLUMN address TO city;

-- 테이블 이름 변경
ALTER TABLE distributors RENAME TO suppliers;

-- 제약조건 이름 변경
ALTER TABLE distributors RENAME CONSTRAINT zipchk TO zip_check;
```

제약조건 추가/제거
```sql
-- NOT NULL 제약조건 추가
ALTER TABLE distributors ALTER COLUMN street SET NOT NULL;

-- NOT NULL 제약조건 제거
ALTER TABLE distributors ALTER COLUMN street DROP NOT NULL;

-- CHECK 제약조건 추가
ALTER TABLE distributors ADD CONSTRAINT zipchk CHECK (char_length(zipcode) = 5);

-- 외래 키 추가 (유효성 검사 지연)
ALTER TABLE distributors ADD CONSTRAINT distfk
    FOREIGN KEY (address) REFERENCES addresses (address) NOT VALID;
ALTER TABLE distributors VALIDATE CONSTRAINT distfk;
```

파티션 관리
```sql
-- 파티션 연결
ALTER TABLE measurement
    ATTACH PARTITION measurement_y2016m07 FOR VALUES FROM ('2016-07-01') TO ('2016-08-01');

-- 파티션 분리
ALTER TABLE measurement
    DETACH PARTITION measurement_y2015m12;
```

스키마 및 테이블스페이스 변경
```sql
-- 다른 테이블스페이스로 이동
ALTER TABLE distributors SET TABLESPACE fasttablespace;

-- 다른 스키마로 이동
ALTER TABLE myschema.distributors SET SCHEMA yourschema;
```

---

### 2.3 DROP TABLE - 테이블 삭제

`DROP TABLE`은 테이블을 삭제하는 명령어입니다.

#### 기본 구문

```sql
DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
```

#### 옵션

| 옵션 | 설명 |
|------|------|
| `IF EXISTS` | 테이블이 존재하지 않아도 오류 발생하지 않음 |
| `CASCADE` | 의존하는 객체(뷰, 외래 키 등)도 함께 삭제 |
| `RESTRICT` | 의존하는 객체가 있으면 삭제 거부 (기본값) |

#### 예제

```sql
-- 기본 삭제
DROP TABLE films;

-- 존재할 경우에만 삭제
DROP TABLE IF EXISTS temp_data;

-- 의존 객체와 함께 삭제
DROP TABLE orders CASCADE;
```

---

### 2.4 CREATE INDEX - 인덱스 생성

`CREATE INDEX`는 테이블의 특정 열에 인덱스를 생성하여 조회 성능을 향상시킵니다.

#### 기본 구문

```sql
CREATE [ UNIQUE ] INDEX [ CONCURRENTLY ] [ [ IF NOT EXISTS ] name ]
    ON [ ONLY ] table_name [ USING method ]
    ( { column_name | ( expression ) } [ COLLATE collation ] [ opclass [ ( opclass_parameter = value [, ... ] ) ] ]
      [ ASC | DESC ] [ NULLS { FIRST | LAST } ] [, ...] )
    [ INCLUDE ( column_name [, ...] ) ]
    [ NULLS [ NOT ] DISTINCT ]
    [ WITH ( storage_parameter [= value] [, ... ] ) ]
    [ TABLESPACE tablespace_name ]
    [ WHERE predicate ]
```

#### 인덱스 메서드

| 메서드 | 설명 | 용도 |
|--------|------|------|
| `B-tree` | 기본값; 균형 트리 | 비교 연산 (<, <=, =, >=, >) |
| `Hash` | 해시 인덱스 | 동등 비교 (=) |
| `GiST` | 일반화된 검색 트리 | 기하학적 데이터, 전문 검색 |
| `SP-GiST` | 공간 분할 GiST | 불균형 데이터 구조 |
| `GIN` | 역인덱스 | 배열, 전문 검색, JSONB |
| `BRIN` | 블록 범위 인덱스 | 대용량 테이블의 순차적 데이터 |

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `UNIQUE` | 고유 값 강제; 중복 항목 방지 |
| `CONCURRENTLY` | 쓰기 잠금 없이 인덱스 생성 (시간은 더 걸림) |
| `INCLUDE` | 인덱스 전용 스캔을 위한 비키 열 추가 |
| `WHERE` | 행의 부분집합에 대한 부분 인덱스 생성 |

#### 예제

기본 UNIQUE 인덱스
```sql
CREATE UNIQUE INDEX title_idx ON films (title);
```

포함 열이 있는 인덱스
```sql
CREATE UNIQUE INDEX title_idx ON films (title) INCLUDE (director, rating);
```

표현식 인덱스 (대소문자 무시 검색)
```sql
CREATE INDEX ON films ((lower(title)));
```

사용자 정의 정렬 순서
```sql
CREATE INDEX title_idx_nulls_low ON films (title NULLS FIRST);
```

채우기 비율(fillfactor) 설정
```sql
CREATE UNIQUE INDEX title_idx ON films (title) WITH (fillfactor = 70);
```

GIN 인덱스
```sql
CREATE INDEX gin_idx ON documents_table USING GIN (locations) WITH (fastupdate = off);
```

동시 인덱스 생성 (Non-blocking)
```sql
CREATE INDEX CONCURRENTLY sales_quantity_index ON sales_table (quantity);
```

부분 인덱스 (Partial Index)
```sql
CREATE INDEX orders_active_idx ON orders (order_date) WHERE status = 'active';
```

특정 테이블스페이스에 인덱스 생성
```sql
CREATE INDEX code_idx ON films (code) TABLESPACE indexspace;
```

---

### 2.5 CREATE VIEW - 뷰 생성

`CREATE VIEW`는 쿼리의 뷰를 정의합니다. 뷰는 물리적으로 구체화되지 않으며, 뷰가 참조될 때마다 쿼리가 실행됩니다.

#### 기본 구문

```sql
CREATE [ OR REPLACE ] [ TEMP | TEMPORARY ] [ RECURSIVE ] VIEW name [ ( column_name [, ...] ) ]
    [ WITH ( view_option_name [= view_option_value] [, ... ] ) ]
    AS query
    [ WITH [ CASCADED | LOCAL ] CHECK OPTION ]
```

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `OR REPLACE` | 기존 뷰가 있으면 대체 (열 구조가 동일해야 함) |
| `TEMPORARY/TEMP` | 세션 종료 시 자동 삭제되는 임시 뷰 |
| `RECURSIVE` | WITH RECURSIVE 구문을 사용하는 재귀 뷰 |
| `check_option` | 업데이트 가능한 뷰에 대해 `local` 또는 `cascaded` |
| `security_barrier` | 뷰에 대한 행 수준 보안 활성화 |
| `security_invoker` | 뷰 소유자가 아닌 사용자의 권한으로 기본 관계 확인 |
| `CHECK OPTION` | 삽입/수정된 행이 뷰 정의 조건을 만족하는지 확인 |

#### 업데이트 가능한 뷰의 조건

뷰가 자동으로 업데이트 가능하려면 다음 모든 조건을 만족해야 합니다:

- FROM 목록에 정확히 하나의 항목 (테이블 또는 업데이트 가능한 뷰)
- 최상위 수준에서 WITH, DISTINCT, GROUP BY, HAVING, LIMIT, OFFSET 없음
- 최상위 수준에서 집합 연산 (UNION, INTERSECT, EXCEPT) 없음
- SELECT 목록에 집계 함수, 윈도우 함수, 집합 반환 함수 없음

#### 예제

기본 뷰
```sql
CREATE VIEW comedies AS
    SELECT *
    FROM films
    WHERE kind = 'Comedy';
```

LOCAL CHECK OPTION이 있는 뷰
```sql
CREATE VIEW universal_comedies AS
    SELECT *
    FROM comedies
    WHERE classification = 'U'
    WITH LOCAL CHECK OPTION;
```

CASCADED CHECK OPTION이 있는 뷰
```sql
CREATE VIEW pg_comedies AS
    SELECT *
    FROM comedies
    WHERE classification = 'PG'
    WITH CASCADED CHECK OPTION;
```

재귀 뷰
```sql
CREATE RECURSIVE VIEW public.nums_1_100 (n) AS
    VALUES (1)
UNION ALL
    SELECT n+1 FROM nums_1_100 WHERE n < 100;
```

---

### 2.6 CREATE FUNCTION - 함수 생성

`CREATE FUNCTION`은 새로운 함수를 정의합니다.

#### 기본 구문

```sql
CREATE [ OR REPLACE ] FUNCTION
    name ( [ [ argmode ] [ argname ] argtype [ { DEFAULT | = } default_expr ] [, ...] ] )
    [ RETURNS rettype
      | RETURNS TABLE ( column_name column_type [, ...] ) ]
  { LANGUAGE lang_name
    | TRANSFORM { FOR TYPE type_name } [, ... ]
    | WINDOW
    | { IMMUTABLE | STABLE | VOLATILE }
    | [ NOT ] LEAKPROOF
    | { CALLED ON NULL INPUT | RETURNS NULL ON NULL INPUT | STRICT }
    | { [ EXTERNAL ] SECURITY INVOKER | [ EXTERNAL ] SECURITY DEFINER }
    | PARALLEL { UNSAFE | RESTRICTED | SAFE }
    | COST execution_cost
    | ROWS result_rows
    | SUPPORT support_function
    | SET configuration_parameter { TO value | = value | FROM CURRENT }
    | AS 'definition'
    | AS 'obj_file', 'link_symbol'
    | sql_body
  } ...
```

#### 주요 함수 속성

| 속성 | 설명 |
|------|------|
| `IMMUTABLE` | 데이터베이스 수정 불가; 동일 인수에 항상 동일 결과 |
| `STABLE` | 데이터베이스 수정 불가; 단일 스캔 내에서 일관성 유지 |
| `VOLATILE` | 단일 스캔 내에서도 값이 변경될 수 있음 (기본값) |
| `STRICT` / `RETURNS NULL ON NULL INPUT` | 인수가 NULL이면 NULL 반환 |
| `SECURITY DEFINER` | 소유자의 권한으로 실행 (vs `SECURITY INVOKER` - 기본값) |
| `PARALLEL SAFE` | 병렬 모드에서 실행해도 안전 |

#### 예제

간단한 SQL 함수
```sql
CREATE FUNCTION add(integer, integer) RETURNS integer
    AS 'select $1 + $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;
```

인수 이름이 있는 함수 (SQL 표준 스타일)
```sql
CREATE FUNCTION add(a integer, b integer) RETURNS integer
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT
    RETURN a + b;
```

PL/pgSQL 함수
```sql
CREATE OR REPLACE FUNCTION increment(i integer) RETURNS integer AS $$
    BEGIN
        RETURN i + 1;
    END;
$$ LANGUAGE plpgsql;
```

다중 출력 매개변수
```sql
CREATE FUNCTION dup(in int, out f1 int, out f2 text)
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;
```

TABLE 반환 함수
```sql
CREATE FUNCTION dup(int) RETURNS TABLE(f1 int, f2 text)
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;
```

---

### 2.7 CREATE TRIGGER - 트리거 생성

`CREATE TRIGGER`는 특정 테이블과 연결되어 특정 작업 수행 시 지정된 함수를 실행하는 트리거를 정의합니다.

#### 기본 구문

```sql
CREATE [ OR REPLACE ] [ CONSTRAINT ] TRIGGER name { BEFORE | AFTER | INSTEAD OF } { event [ OR ... ] }
    ON table_name
    [ FROM referenced_table_name ]
    [ NOT DEFERRABLE | [ DEFERRABLE ] [ INITIALLY IMMEDIATE | INITIALLY DEFERRED ] ]
    [ REFERENCING { { OLD | NEW } TABLE [ AS ] transition_relation_name } [ ... ] ]
    [ FOR [ EACH ] { ROW | STATEMENT } ]
    [ WHEN ( condition ) ]
    EXECUTE { FUNCTION | PROCEDURE } function_name ( arguments )

-- event는 다음 중 하나:
    INSERT
    UPDATE [ OF column_name [, ... ] ]
    DELETE
    TRUNCATE
```

#### 트리거 타이밍

| 타이밍 | 설명 |
|--------|------|
| `BEFORE` | 작업 전에 트리거 실행 |
| `AFTER` | 작업 후에 트리거 실행 |
| `INSTEAD OF` | 작업 대신 트리거 실행 (뷰에서만 사용) |

#### 트리거 레벨

| 레벨 | 설명 |
|------|------|
| `FOR EACH ROW` | 영향 받는 각 행마다 한 번 실행 |
| `FOR EACH STATEMENT` | SQL 문마다 한 번 실행 (기본값) |

#### 예제

기본 트리거 - 업데이트 전 실행
```sql
CREATE TRIGGER check_update
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION check_account_update();
```

특정 열 변경 시 트리거
```sql
CREATE OR REPLACE TRIGGER check_update
    BEFORE UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION check_account_update();
```

WHEN 조건이 있는 트리거
```sql
CREATE TRIGGER check_update
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    WHEN (OLD.balance IS DISTINCT FROM NEW.balance)
    EXECUTE FUNCTION check_account_update();
```

뷰에 대한 INSTEAD OF 트리거
```sql
CREATE TRIGGER view_insert
    INSTEAD OF INSERT ON my_view
    FOR EACH ROW
    EXECUTE FUNCTION view_insert_row();
```

전이 테이블이 있는 문 수준 트리거
```sql
CREATE TRIGGER transfer_insert
    AFTER INSERT ON transfer
    REFERENCING NEW TABLE AS inserted
    FOR EACH STATEMENT
    EXECUTE FUNCTION check_transfer_balances_to_zero();
```

---

### 2.8 기타 CREATE 명령어

#### CREATE DATABASE - 데이터베이스 생성

```sql
CREATE DATABASE name
    [ WITH ] [ OWNER [=] user_name ]
           [ TEMPLATE [=] template ]
           [ ENCODING [=] encoding ]
           [ LOCALE [=] locale ]
           [ LC_COLLATE [=] lc_collate ]
           [ LC_CTYPE [=] lc_ctype ]
           [ TABLESPACE [=] tablespace_name ]
           [ ALLOW_CONNECTIONS [=] allowconn ]
           [ CONNECTION LIMIT [=] connlimit ]
           [ IS_TEMPLATE [=] istemplate ]
```

예제
```sql
CREATE DATABASE sales OWNER salesapp TABLESPACE salesspace;
```

#### CREATE SCHEMA - 스키마 생성

```sql
CREATE SCHEMA schema_name [ AUTHORIZATION role_specification ]
CREATE SCHEMA AUTHORIZATION role_specification
CREATE SCHEMA IF NOT EXISTS schema_name [ AUTHORIZATION role_specification ]
```

예제
```sql
CREATE SCHEMA myschema;
CREATE SCHEMA AUTHORIZATION joe;
```

#### CREATE SEQUENCE - 시퀀스 생성

```sql
CREATE [ TEMPORARY | TEMP ] SEQUENCE [ IF NOT EXISTS ] name
    [ AS data_type ]
    [ INCREMENT [ BY ] increment ]
    [ MINVALUE minvalue | NO MINVALUE ] [ MAXVALUE maxvalue | NO MAXVALUE ]
    [ START [ WITH ] start ] [ CACHE cache ] [ [ NO ] CYCLE ]
    [ OWNED BY { table_name.column_name | NONE } ]
```

예제
```sql
CREATE SEQUENCE serial START 101;
SELECT nextval('serial');
```

---

## 3. 권한 관리 명령어 (DCL)

데이터 제어 언어(Data Control Language)는 데이터베이스 객체에 대한 접근 권한을 관리합니다.

### 3.1 GRANT - 권한 부여

`GRANT` 명령어는 데이터베이스 객체에 대한 접근 권한을 정의하거나 역할의 멤버십을 부여합니다.

#### 기본 구문

데이터베이스 객체에 대한 GRANT
```sql
GRANT { privileges | ALL [ PRIVILEGES ] }
ON object_type object_name
TO role_specification [ WITH GRANT OPTION ]
[ GRANTED BY role_specification ]
```

역할에 대한 GRANT
```sql
GRANT role_name TO role_specification
[ WITH { ADMIN | INHERIT | SET } { OPTION | TRUE | FALSE } ]
[ GRANTED BY role_specification ]
```

#### 주요 권한 유형

| 객체 유형 | 사용 가능한 권한 |
|-----------|------------------|
| 테이블/뷰 | SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, MAINTAIN |
| 시퀀스 | USAGE, SELECT, UPDATE |
| 데이터베이스 | CREATE, CONNECT, TEMPORARY |
| 함수/프로시저 | EXECUTE |
| 스키마 | CREATE, USAGE |

#### 주요 개념

| 개념 | 설명 |
|------|------|
| `PUBLIC` | 모든 역할(현재 및 미래)에 권한 부여 |
| `WITH GRANT OPTION` | 수신자가 다른 사람에게 권한을 부여할 수 있음 |
| `GRANTED BY` | 누가 권한을 부여했는지 기록; 현재 사용자여야 함 |
| 소유자 권한 | 객체 소유자는 기본적으로 모든 권한 보유; 취소 불가 |

#### 예제

테이블에 INSERT 권한을 모든 사용자에게 부여
```sql
GRANT INSERT ON films TO PUBLIC;
```

뷰에 모든 권한 부여
```sql
GRANT ALL PRIVILEGES ON kinds TO manuel;
```

특정 열에 대한 권한 부여
```sql
GRANT SELECT (title, kind), UPDATE (kind) ON films TO manuel;
```

역할 멤버십 부여
```sql
GRANT admins TO joe;
```

관리 옵션과 함께 역할 부여 (다른 사람에게 부여 가능)
```sql
GRANT admins TO joe WITH ADMIN OPTION;
```

스키마에 대한 권한 부여
```sql
GRANT USAGE ON SCHEMA myschema TO joe;
GRANT CREATE ON SCHEMA myschema TO joe;
```

함수에 대한 실행 권한 부여
```sql
GRANT EXECUTE ON FUNCTION my_function(integer) TO joe;
```

#### 역할 멤버십 옵션

| 옵션 | 설명 |
|------|------|
| `ADMIN` | 멤버가 멤버십을 부여/취소 가능 (기본값 FALSE) |
| `INHERIT` | 멤버가 역할의 권한을 상속 (기본값은 상속 속성에 따름) |
| `SET` | 멤버가 SET ROLE로 부여된 역할로 변경 가능 (기본값 TRUE) |

---

### 3.2 REVOKE - 권한 취소

`REVOKE` 명령어는 하나 이상의 역할에서 접근 권한을 제거합니다.

#### 기본 구문

테이블 권한 취소
```sql
REVOKE [ GRANT OPTION FOR ]
    { { SELECT | INSERT | UPDATE | DELETE | TRUNCATE | REFERENCES | TRIGGER | MAINTAIN }
    [, ...] | ALL [ PRIVILEGES ] }
    ON { [ TABLE ] table_name [, ...]
         | ALL TABLES IN SCHEMA schema_name [, ...] }
    FROM role_specification [, ...]
    [ GRANTED BY role_specification ]
    [ CASCADE | RESTRICT ]
```

역할 멤버십 취소
```sql
REVOKE [ { ADMIN | INHERIT | SET } OPTION FOR ]
    role_name [, ...] FROM role_specification [, ...]
    [ GRANTED BY role_specification ]
    [ CASCADE | RESTRICT ]
```

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `GRANT OPTION FOR` | 권한 자체가 아닌 부여 옵션만 취소 |
| `CASCADE` | 의존하는 권한도 함께 취소 |
| `RESTRICT` | 의존하는 권한이 있으면 취소 거부 (기본값) |

#### 예제

테이블에서 INSERT 권한 취소
```sql
REVOKE INSERT ON films FROM PUBLIC;
```

모든 권한 취소
```sql
REVOKE ALL PRIVILEGES ON kinds FROM manuel;
```

역할 멤버십 취소
```sql
REVOKE admins FROM joe;
```

스키마 내 모든 테이블에서 권한 취소
```sql
REVOKE SELECT ON ALL TABLES IN SCHEMA myschema FROM joe;
```

---

### 3.3 CREATE ROLE - 역할 생성

`CREATE ROLE`은 새로운 데이터베이스 역할을 정의합니다.

#### 기본 구문

```sql
CREATE ROLE name [ [ WITH ] option [ ... ] ]

-- 옵션:
      SUPERUSER | NOSUPERUSER
    | CREATEDB | NOCREATEDB
    | CREATEROLE | NOCREATEROLE
    | INHERIT | NOINHERIT
    | LOGIN | NOLOGIN
    | REPLICATION | NOREPLICATION
    | BYPASSRLS | NOBYPASSRLS
    | CONNECTION LIMIT connlimit
    | [ ENCRYPTED ] PASSWORD 'password' | PASSWORD NULL
    | VALID UNTIL 'timestamp'
    | IN ROLE role_name [, ...]
    | IN GROUP role_name [, ...]
    | ROLE role_name [, ...]
    | ADMIN role_name [, ...]
    | USER role_name [, ...]
    | SYSID uid
```

#### 주요 역할 속성

| 속성 | 설명 |
|------|------|
| `SUPERUSER` | 슈퍼유저 권한 (모든 접근 검사 우회) |
| `CREATEDB` | 데이터베이스 생성 가능 |
| `CREATEROLE` | 새 역할 생성 가능 |
| `LOGIN` | 로그인 가능 (vs NOLOGIN: 그룹 역할용) |
| `INHERIT` | 멤버인 역할의 권한 상속 |
| `REPLICATION` | 복제 연결 가능 |
| `BYPASSRLS` | 행 수준 보안 우회 |

#### 예제

로그인 가능한 역할 생성
```sql
CREATE ROLE jonathan LOGIN;
```

비밀번호와 만료일이 있는 역할
```sql
CREATE ROLE admin WITH LOGIN PASSWORD 'jw8s0F4' VALID UNTIL '2025-01-01';
```

데이터베이스 생성 권한이 있는 역할
```sql
CREATE ROLE miriam WITH CREATEDB LOGIN PASSWORD 'jw8s0F4';
```

---

### 3.4 ALTER DEFAULT PRIVILEGES - 기본 권한 변경

`ALTER DEFAULT PRIVILEGES`는 향후 생성될 객체에 적용될 기본 접근 권한을 정의합니다.

#### 기본 구문

```sql
ALTER DEFAULT PRIVILEGES
    [ FOR { ROLE | USER } target_role [, ...] ]
    [ IN SCHEMA schema_name [, ...] ]
    abbreviated_grant_or_revoke
```

#### 예제

스키마 내 모든 새 테이블에 자동으로 SELECT 권한 부여
```sql
ALTER DEFAULT PRIVILEGES IN SCHEMA myschema
    GRANT SELECT ON TABLES TO PUBLIC;
```

특정 사용자가 생성하는 모든 함수에 실행 권한 부여
```sql
ALTER DEFAULT PRIVILEGES FOR ROLE admin
    GRANT EXECUTE ON FUNCTIONS TO PUBLIC;
```

---

## 4. 트랜잭션 제어 명령어 (TCL)

트랜잭션 제어 언어(Transaction Control Language)는 트랜잭션의 시작, 커밋, 롤백을 관리합니다.

### 4.1 BEGIN - 트랜잭션 시작

`BEGIN`은 트랜잭션 블록을 시작합니다.

```sql
BEGIN [ WORK | TRANSACTION ] [ transaction_mode [, ...] ]

-- transaction_mode:
    ISOLATION LEVEL { SERIALIZABLE | REPEATABLE READ | READ COMMITTED | READ UNCOMMITTED }
    READ WRITE | READ ONLY
    [ NOT ] DEFERRABLE
```

#### 예제

```sql
BEGIN;
-- 또는
BEGIN TRANSACTION;

-- 격리 수준 지정
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### 4.2 COMMIT - 트랜잭션 커밋

`COMMIT`은 현재 트랜잭션을 커밋합니다.

```sql
COMMIT [ WORK | TRANSACTION ] [ AND [ NO ] CHAIN ]
```

#### 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

### 4.3 ROLLBACK - 트랜잭션 롤백

`ROLLBACK`은 현재 트랜잭션을 중단하고 모든 변경 사항을 취소합니다.

```sql
ROLLBACK [ WORK | TRANSACTION ] [ AND [ NO ] CHAIN ]
```

#### 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- 문제 발생!
ROLLBACK;  -- 변경 사항 취소
```

### 4.4 SAVEPOINT - 저장점 정의

`SAVEPOINT`는 현재 트랜잭션 내에서 새로운 저장점을 정의합니다.

```sql
SAVEPOINT savepoint_name
```

### 4.5 ROLLBACK TO SAVEPOINT - 저장점으로 롤백

```sql
ROLLBACK [ WORK | TRANSACTION ] TO [ SAVEPOINT ] savepoint_name
```

### 4.6 RELEASE SAVEPOINT - 저장점 해제

```sql
RELEASE [ SAVEPOINT ] savepoint_name
```

#### 저장점 사용 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
SAVEPOINT my_savepoint;
UPDATE accounts SET balance = balance + 100 WHERE id = 999;  -- 존재하지 않는 계정
ROLLBACK TO SAVEPOINT my_savepoint;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

### 4.7 SET TRANSACTION - 트랜잭션 특성 설정

```sql
SET TRANSACTION transaction_mode [, ...]
SET TRANSACTION SNAPSHOT snapshot_id
SET SESSION CHARACTERISTICS AS TRANSACTION transaction_mode [, ...]
```

#### 격리 수준 (Isolation Levels)

| 격리 수준 | 설명 |
|-----------|------|
| `READ UNCOMMITTED` | 커밋되지 않은 데이터 읽기 가능 (PostgreSQL에서는 READ COMMITTED로 동작) |
| `READ COMMITTED` | 커밋된 데이터만 읽기 (기본값) |
| `REPEATABLE READ` | 트랜잭션 시작 시점의 스냅샷 사용 |
| `SERIALIZABLE` | 가장 엄격한 격리; 직렬화 가능한 트랜잭션 |

#### 예제

```sql
BEGIN;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE READ ONLY;
SELECT * FROM accounts;
COMMIT;
```

---

## 5. 기타 유틸리티 명령어

### 5.1 EXPLAIN - 실행 계획 표시

`EXPLAIN`은 쿼리의 실행 계획을 표시합니다.

```sql
EXPLAIN [ ( option [, ...] ) ] statement
EXPLAIN [ ANALYZE ] [ VERBOSE ] statement

-- 옵션:
    ANALYZE [ boolean ]
    VERBOSE [ boolean ]
    COSTS [ boolean ]
    SETTINGS [ boolean ]
    GENERIC_PLAN [ boolean ]
    BUFFERS [ boolean ]
    WAL [ boolean ]
    TIMING [ boolean ]
    SUMMARY [ boolean ]
    FORMAT { TEXT | XML | JSON | YAML }
```

#### 예제

```sql
-- 기본 실행 계획
EXPLAIN SELECT * FROM films WHERE kind = 'Comedy';

-- 실제 실행 통계 포함
EXPLAIN ANALYZE SELECT * FROM films WHERE kind = 'Comedy';

-- 상세 정보 포함
EXPLAIN (ANALYZE, VERBOSE, BUFFERS) SELECT * FROM films WHERE kind = 'Comedy';

-- JSON 형식으로 출력
EXPLAIN (FORMAT JSON) SELECT * FROM films WHERE kind = 'Comedy';
```

### 5.2 ANALYZE - 통계 수집

`ANALYZE`는 데이터베이스에 대한 통계를 수집합니다.

```sql
ANALYZE [ ( option [, ...] ) ] [ table_and_columns [, ...] ]
ANALYZE [ VERBOSE ] [ table_and_columns [, ...] ]
```

#### 예제

```sql
-- 전체 데이터베이스 분석
ANALYZE;

-- 특정 테이블 분석
ANALYZE films;

-- 특정 열 분석
ANALYZE films (kind, title);

-- 상세 출력
ANALYZE VERBOSE films;
```

### 5.3 VACUUM - 가비지 컬렉션

`VACUUM`은 데드 튜플을 정리하고 선택적으로 데이터베이스를 분석합니다.

```sql
VACUUM [ ( option [, ...] ) ] [ table_and_columns [, ...] ]
VACUUM [ FULL ] [ FREEZE ] [ VERBOSE ] [ ANALYZE ] [ table_and_columns [, ...] ]
```

#### 옵션

| 옵션 | 설명 |
|------|------|
| `FULL` | 테이블 전체를 다시 작성하여 공간 회수 (배타적 잠금 필요) |
| `FREEZE` | 트랜잭션 ID 래핑 방지를 위해 행 동결 |
| `VERBOSE` | 상세 진행 상황 출력 |
| `ANALYZE` | VACUUM 후 통계 업데이트 |

#### 예제

```sql
-- 전체 데이터베이스 VACUUM
VACUUM;

-- 특정 테이블 VACUUM 및 분석
VACUUM ANALYZE films;

-- FULL VACUUM (공간 회수)
VACUUM FULL films;
```

### 5.4 COPY - 데이터 복사

`COPY`는 파일과 테이블 간에 데이터를 복사합니다.

```sql
-- 테이블에서 파일로
COPY table_name [ ( column_name [, ...] ) ]
    TO { 'filename' | PROGRAM 'command' | STDOUT }
    [ [ WITH ] ( option [, ...] ) ]

-- 파일에서 테이블로
COPY table_name [ ( column_name [, ...] ) ]
    FROM { 'filename' | PROGRAM 'command' | STDIN }
    [ [ WITH ] ( option [, ...] ) ]
```

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `FORMAT` | csv, text, binary |
| `DELIMITER` | 열 구분자 |
| `HEADER` | 첫 행을 헤더로 처리 |
| `NULL` | NULL 값의 문자열 표현 |
| `ENCODING` | 파일 인코딩 |

#### 예제

```sql
-- 테이블을 CSV 파일로 내보내기
COPY films TO '/tmp/films.csv' WITH (FORMAT csv, HEADER);

-- CSV 파일에서 테이블로 가져오기
COPY films FROM '/tmp/films.csv' WITH (FORMAT csv, HEADER);

-- 특정 열만 내보내기
COPY films (title, kind) TO STDOUT WITH (FORMAT csv);

-- 쿼리 결과 내보내기
COPY (SELECT * FROM films WHERE kind = 'Comedy') TO '/tmp/comedies.csv' WITH (FORMAT csv);
```

### 5.5 COMMENT - 객체에 주석 추가

`COMMENT`는 데이터베이스 객체에 주석을 정의합니다.

```sql
COMMENT ON
{
  AGGREGATE agg_name (agg_type [, ...]) |
  COLUMN relation_name.column_name |
  DATABASE object_name |
  FUNCTION func_name (arg_type [, ...]) |
  INDEX object_name |
  SCHEMA object_name |
  SEQUENCE object_name |
  TABLE object_name |
  TRIGGER trigger_name ON table_name |
  TYPE object_name |
  VIEW object_name |
  ...
} IS 'text'
```

#### 예제

```sql
-- 테이블에 주석 추가
COMMENT ON TABLE films IS '영화 정보 테이블';

-- 열에 주석 추가
COMMENT ON COLUMN films.kind IS '영화 장르';

-- 함수에 주석 추가
COMMENT ON FUNCTION my_function(integer) IS '정수를 증가시키는 함수';

-- 주석 제거
COMMENT ON TABLE films IS NULL;
```

### 5.6 SET - 런타임 매개변수 설정

```sql
SET [ SESSION | LOCAL ] configuration_parameter { TO | = } { value | 'value' | DEFAULT }
SET [ SESSION | LOCAL ] TIME ZONE { value | 'value' | LOCAL | DEFAULT }
```

#### 예제

```sql
-- 클라이언트 인코딩 설정
SET client_encoding TO 'UTF8';

-- 타임존 설정
SET TIME ZONE 'Asia/Seoul';

-- 검색 경로 설정
SET search_path TO myschema, public;

-- 세션 내 트랜잭션에만 적용
SET LOCAL statement_timeout = '5min';
```

### 5.7 SHOW - 런타임 매개변수 조회

```sql
SHOW name
SHOW ALL
```

#### 예제

```sql
-- 특정 설정 조회
SHOW client_encoding;
SHOW search_path;
SHOW timezone;

-- 모든 설정 조회
SHOW ALL;
```

### 5.8 LISTEN/NOTIFY/UNLISTEN - 알림

#### LISTEN - 알림 대기

```sql
LISTEN channel
```

#### NOTIFY - 알림 보내기

```sql
NOTIFY channel [ , 'payload' ]
```

#### UNLISTEN - 알림 대기 중지

```sql
UNLISTEN { channel | * }
```

#### 예제

```sql
-- 세션 1: 알림 대기
LISTEN virtual;

-- 세션 2: 알림 보내기
NOTIFY virtual;
NOTIFY virtual, '{"event": "update", "id": 123}';

-- 세션 1: 알림 대기 중지
UNLISTEN virtual;
-- 또는 모든 채널
UNLISTEN *;
```

---

## 부록: SQL 명령어 분류 요약

### A. 전체 SQL 명령어 목록

#### 데이터 조작 (DML)

| 명령어 | 설명 |
|--------|------|
| `CALL` | 프로시저 호출 |
| `COPY` | 파일과 테이블 간 데이터 복사 |
| `DELETE` | 테이블에서 행 삭제 |
| `INSERT` | 테이블에 새 행 삽입 |
| `MERGE` | 조건부 삽입, 수정, 삭제 |
| `SELECT` | 테이블 또는 뷰에서 행 조회 |
| `SELECT INTO` | 쿼리 결과로 새 테이블 정의 |
| `TRUNCATE` | 테이블 비우기 |
| `UPDATE` | 테이블의 행 수정 |
| `VALUES` | 행 집합 계산 |

#### 테이블 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE TABLE` | 새 테이블 정의 |
| `CREATE TABLE AS` | 쿼리 결과로 새 테이블 정의 |
| `ALTER TABLE` | 테이블 정의 변경 |
| `DROP TABLE` | 테이블 삭제 |
| `CLUSTER` | 인덱스에 따라 테이블 클러스터링 |
| `LOCK` | 테이블 잠금 |

#### 인덱스 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE INDEX` | 새 인덱스 정의 |
| `ALTER INDEX` | 인덱스 정의 변경 |
| `DROP INDEX` | 인덱스 삭제 |
| `REINDEX` | 인덱스 재구성 |

#### 뷰 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE VIEW` | 새 뷰 정의 |
| `CREATE MATERIALIZED VIEW` | 새 구체화된 뷰 정의 |
| `ALTER VIEW` | 뷰 정의 변경 |
| `ALTER MATERIALIZED VIEW` | 구체화된 뷰 정의 변경 |
| `DROP VIEW` | 뷰 삭제 |
| `DROP MATERIALIZED VIEW` | 구체화된 뷰 삭제 |
| `REFRESH MATERIALIZED VIEW` | 구체화된 뷰 내용 갱신 |

#### 역할 및 권한 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE ROLE` | 새 데이터베이스 역할 정의 |
| `CREATE USER` | 새 데이터베이스 역할 정의 |
| `ALTER ROLE` | 데이터베이스 역할 변경 |
| `ALTER USER` | 데이터베이스 역할 변경 |
| `DROP ROLE` | 데이터베이스 역할 삭제 |
| `DROP USER` | 데이터베이스 역할 삭제 |
| `GRANT` | 접근 권한 정의 |
| `REVOKE` | 접근 권한 제거 |
| `SET ROLE` | 현재 세션의 사용자 식별자 설정 |
| `ALTER DEFAULT PRIVILEGES` | 기본 접근 권한 정의 |

#### 트랜잭션 제어

| 명령어 | 설명 |
|--------|------|
| `BEGIN` | 트랜잭션 블록 시작 |
| `START TRANSACTION` | 트랜잭션 블록 시작 |
| `COMMIT` | 현재 트랜잭션 커밋 |
| `END` | 현재 트랜잭션 커밋 |
| `ROLLBACK` | 현재 트랜잭션 중단 |
| `ABORT` | 현재 트랜잭션 중단 |
| `SAVEPOINT` | 새 저장점 정의 |
| `RELEASE SAVEPOINT` | 이전에 정의된 저장점 해제 |
| `ROLLBACK TO SAVEPOINT` | 저장점으로 롤백 |
| `SET TRANSACTION` | 현재 트랜잭션의 특성 설정 |
| `SET CONSTRAINTS` | 현재 트랜잭션의 제약조건 검사 타이밍 설정 |

#### 함수 및 프로시저

| 명령어 | 설명 |
|--------|------|
| `CREATE FUNCTION` | 새 함수 정의 |
| `CREATE PROCEDURE` | 새 프로시저 정의 |
| `CREATE AGGREGATE` | 새 집계 함수 정의 |
| `ALTER FUNCTION` | 함수 정의 변경 |
| `ALTER PROCEDURE` | 프로시저 정의 변경 |
| `ALTER AGGREGATE` | 집계 함수 정의 변경 |
| `DROP FUNCTION` | 함수 삭제 |
| `DROP PROCEDURE` | 프로시저 삭제 |
| `DROP AGGREGATE` | 집계 함수 삭제 |

#### 트리거 및 규칙

| 명령어 | 설명 |
|--------|------|
| `CREATE TRIGGER` | 새 트리거 정의 |
| `CREATE EVENT TRIGGER` | 새 이벤트 트리거 정의 |
| `CREATE RULE` | 새 재작성 규칙 정의 |
| `ALTER TRIGGER` | 트리거 정의 변경 |
| `ALTER EVENT TRIGGER` | 이벤트 트리거 정의 변경 |
| `ALTER RULE` | 규칙 정의 변경 |
| `DROP TRIGGER` | 트리거 삭제 |
| `DROP EVENT TRIGGER` | 이벤트 트리거 삭제 |
| `DROP RULE` | 재작성 규칙 삭제 |

#### 유틸리티 명령어

| 명령어 | 설명 |
|--------|------|
| `ANALYZE` | 데이터베이스 통계 수집 |
| `VACUUM` | 가비지 컬렉션 및 선택적 분석 |
| `EXPLAIN` | 문의 실행 계획 표시 |
| `COMMENT` | 객체의 주석 정의 또는 변경 |
| `SET` | 런타임 매개변수 변경 |
| `SHOW` | 런타임 매개변수 값 표시 |
| `RESET` | 런타임 매개변수를 기본값으로 복원 |
| `LISTEN` | 알림 대기 |
| `NOTIFY` | 알림 생성 |
| `UNLISTEN` | 알림 대기 중지 |

---

## 참고 자료

- [PostgreSQL 공식 문서 - SQL Commands](https://www.postgresql.org/docs/current/sql-commands.html)
- [PostgreSQL 공식 문서 - Data Manipulation](https://www.postgresql.org/docs/current/dml.html)
- [PostgreSQL 공식 문서 - Data Definition](https://www.postgresql.org/docs/current/ddl.html)
- [PostgreSQL 공식 문서 - Queries](https://www.postgresql.org/docs/current/queries.html)
