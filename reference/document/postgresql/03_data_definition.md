# PostgreSQL 18 데이터 정의

이 문서는 PostgreSQL 18 공식 문서의 "Chapter 5. Data Definition"을 한국어로 번역한 것입니다.

## 개요

데이터 정의는 데이터베이스에서 데이터를 저장하는 구조를 생성하고 관리하는 것을 다룹니다. PostgreSQL에서 데이터는 테이블에 저장되며, 테이블은 행(row)과 열(column)로 구성됩니다.

---

# 5장. 데이터 정의

## 5.1. 테이블 기초

관계형 데이터베이스에서 데이터는 테이블 에 저장됩니다. 테이블은 명명된 열(column) 의 집합이며, 각 열은 특정 데이터 타입을 가집니다. 각 행(row)은 해당 열에 대한 값들의 집합을 나타냅니다.

### 테이블 생성

```sql
CREATE TABLE my_first_table (
    first_column text,
    second_column integer
);
```

### 테이블 이름 규칙

- 테이블 이름은 문자로 시작해야 합니다
- 이름에는 문자, 숫자, 밑줄을 사용할 수 있습니다
- 예약어는 따옴표로 묶어야 사용할 수 있습니다

### 실용적인 예제

```sql
-- 제품 테이블
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric
);

-- 주문 테이블
CREATE TABLE orders (
    order_id integer,
    product_no integer,
    quantity integer,
    order_date date
);
```

### 테이블 삭제

```sql
DROP TABLE my_first_table;
DROP TABLE products;
```

`DROP TABLE`은 테이블과 그 안의 모든 데이터를 영구적으로 삭제합니다.

---

## 5.2. 기본값 (Default Values)

열에 기본값(default value) 을 지정하면, 삽입 시 해당 열에 값을 지정하지 않을 때 기본값이 사용됩니다.

### 기본값 지정

```sql
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric DEFAULT 9.99
);
```

### 동적 기본값

```sql
CREATE TABLE orders (
    order_id integer,
    product_no integer,
    quantity integer DEFAULT 1,
    order_date date DEFAULT CURRENT_DATE
);
```

### 일반적인 기본값 패턴

| 타입 | 기본값 예제 |
|------|------------|
| 숫자 | `DEFAULT 0` |
| 문자열 | `DEFAULT 'unknown'` |
| 불리언 | `DEFAULT false` |
| 날짜 | `DEFAULT CURRENT_DATE` |
| 타임스탬프 | `DEFAULT CURRENT_TIMESTAMP` |
| 자동 증가 | `DEFAULT nextval('seq_name')` |

---

## 5.3. 생성된 열 (Generated Columns)

생성된 열 은 다른 열들로부터 계산되는 특별한 열입니다. 두 가지 종류가 있습니다:

### 저장된 생성 열 (STORED)

```sql
CREATE TABLE people (
    first_name text,
    last_name text,
    full_name text GENERATED ALWAYS AS (first_name || ' ' || last_name) STORED
);
```

저장된 생성 열은 디스크 공간을 사용하지만 읽을 때 계산이 필요 없습니다.

### 가상 생성 열 (VIRTUAL)

PostgreSQL 17부터 가상 생성 열도 지원됩니다:

```sql
CREATE TABLE rectangles (
    width numeric,
    height numeric,
    area numeric GENERATED ALWAYS AS (width * height) VIRTUAL
);
```

가상 생성 열은 디스크 공간을 사용하지 않지만 읽을 때마다 계산됩니다.

### 생성 열 제약사항

- 생성 열에 직접 값을 삽입하거나 업데이트할 수 없습니다
- 생성 열은 다른 생성 열을 참조할 수 없습니다
- 생성 표현식은 해당 테이블의 열만 참조할 수 있습니다

---

## 5.4. 제약 조건 (Constraints)

제약 조건은 테이블의 데이터에 대한 규칙을 정의합니다. 잘못된 데이터가 입력되는 것을 방지합니다.

### 5.4.1. 검사 제약 조건 (Check Constraints)

```sql
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric CHECK (price > 0)
);
```

#### 이름 있는 검사 제약 조건

```sql
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric CONSTRAINT positive_price CHECK (price > 0)
);
```

#### 여러 열에 걸친 검사 제약 조건

```sql
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric CHECK (price > 0),
    discounted_price numeric CHECK (discounted_price > 0),
    CHECK (price > discounted_price)
);
```

### 5.4.2. NOT NULL 제약 조건

```sql
CREATE TABLE products (
    product_no integer NOT NULL,
    name text NOT NULL,
    price numeric
);
```

여러 제약 조건 결합:

```sql
CREATE TABLE products (
    product_no integer NOT NULL,
    name text NOT NULL,
    price numeric NOT NULL CHECK (price > 0)
);
```

### 5.4.3. 고유 제약 조건 (Unique Constraints)

```sql
CREATE TABLE products (
    product_no integer UNIQUE,
    name text,
    price numeric
);
```

#### 다중 열 고유 제약 조건

```sql
CREATE TABLE example (
    a integer,
    b integer,
    c integer,
    UNIQUE (a, c)
);
```

#### NULLS NOT DISTINCT

PostgreSQL 15부터 NULL 값을 구별하지 않는 고유 제약 조건:

```sql
CREATE TABLE products (
    product_no integer UNIQUE NULLS NOT DISTINCT,
    name text,
    price numeric
);
```

### 5.4.4. 기본 키 (Primary Keys)

```sql
CREATE TABLE products (
    product_no integer PRIMARY KEY,
    name text,
    price numeric
);
```

#### 복합 기본 키

```sql
CREATE TABLE order_items (
    order_id integer,
    product_no integer,
    quantity integer,
    PRIMARY KEY (order_id, product_no)
);
```

기본 키는 `UNIQUE`와 `NOT NULL` 제약 조건을 결합한 것입니다.

### 5.4.5. 외래 키 (Foreign Keys)

```sql
CREATE TABLE products (
    product_no integer PRIMARY KEY,
    name text,
    price numeric
);

CREATE TABLE orders (
    order_id integer PRIMARY KEY,
    product_no integer REFERENCES products (product_no),
    quantity integer
);
```

#### ON DELETE와 ON UPDATE 액션

```sql
CREATE TABLE orders (
    order_id integer PRIMARY KEY,
    product_no integer REFERENCES products ON DELETE CASCADE,
    quantity integer
);
```

| 액션 | 설명 |
|------|------|
| `NO ACTION` | 기본값. 참조 무결성 위반 시 오류 |
| `RESTRICT` | NO ACTION과 유사하지만 지연 불가 |
| `CASCADE` | 참조된 행 삭제 시 참조하는 행도 삭제 |
| `SET NULL` | 참조 열을 NULL로 설정 |
| `SET DEFAULT` | 참조 열을 기본값으로 설정 |

#### 자기 참조 외래 키

```sql
CREATE TABLE employees (
    employee_id integer PRIMARY KEY,
    name text,
    manager_id integer REFERENCES employees (employee_id)
);
```

### 5.4.6. 배제 제약 조건 (Exclusion Constraints)

```sql
CREATE TABLE reservation (
    during tsrange,
    EXCLUDE USING GIST (during WITH &&)
);
```

범위가 겹치지 않도록 보장합니다.

---

## 5.5. 시스템 열 (System Columns)

모든 테이블에는 시스템에서 자동으로 생성되는 여러 열이 있습니다:

| 열 이름 | 설명 |
|---------|------|
| `tableoid` | 테이블의 OID |
| `xmin` | 삽입 트랜잭션의 ID |
| `xmax` | 삭제 트랜잭션의 ID (0이면 삭제되지 않음) |
| `cmin` | 삽입 명령의 ID |
| `cmax` | 삭제 명령의 ID |
| `ctid` | 행의 물리적 위치 |

```sql
SELECT tableoid, xmin, ctid, * FROM products;
```

---

## 5.6. 테이블 수정 (Modifying Tables)

### 열 추가

```sql
ALTER TABLE products ADD COLUMN description text;
```

기본값과 함께 추가:

```sql
ALTER TABLE products ADD COLUMN description text DEFAULT 'No description';
```

### 열 삭제

```sql
ALTER TABLE products DROP COLUMN description;
```

관련 제약 조건도 함께 삭제:

```sql
ALTER TABLE products DROP COLUMN description CASCADE;
```

### 제약 조건 추가

```sql
ALTER TABLE products ADD CHECK (name <> '');
ALTER TABLE products ADD CONSTRAINT some_name UNIQUE (product_no);
ALTER TABLE products ADD FOREIGN KEY (product_group_id) REFERENCES product_groups;
```

NOT NULL 추가:

```sql
ALTER TABLE products ALTER COLUMN product_no SET NOT NULL;
```

### 제약 조건 삭제

```sql
ALTER TABLE products DROP CONSTRAINT some_name;
ALTER TABLE products ALTER COLUMN product_no DROP NOT NULL;
```

### 기본값 변경

```sql
ALTER TABLE products ALTER COLUMN price SET DEFAULT 7.77;
ALTER TABLE products ALTER COLUMN price DROP DEFAULT;
```

### 데이터 타입 변경

```sql
ALTER TABLE products ALTER COLUMN price TYPE numeric(10,2);
```

### 열 이름 변경

```sql
ALTER TABLE products RENAME COLUMN product_no TO product_number;
```

### 테이블 이름 변경

```sql
ALTER TABLE products RENAME TO items;
```

---

## 5.7. 권한 (Privileges)

### 권한 부여

```sql
GRANT SELECT, INSERT, UPDATE ON products TO joe;
GRANT ALL PRIVILEGES ON products TO admin;
```

### 권한 취소

```sql
REVOKE ALL ON products FROM PUBLIC;
REVOKE INSERT ON products FROM joe;
```

### 권한 종류

| 권한 | 설명 |
|------|------|
| `SELECT` | 테이블에서 데이터 읽기 |
| `INSERT` | 테이블에 데이터 삽입 |
| `UPDATE` | 테이블 데이터 수정 |
| `DELETE` | 테이블에서 데이터 삭제 |
| `TRUNCATE` | 테이블의 모든 데이터 삭제 |
| `REFERENCES` | 외래 키 제약 조건 생성 |
| `TRIGGER` | 트리거 생성 |

---

## 5.8. 행 보안 정책 (Row Security Policies)

### 행 수준 보안 활성화

```sql
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
```

### 정책 생성

```sql
CREATE POLICY account_managers ON accounts
    FOR SELECT
    TO managers
    USING (manager = current_user);
```

### 정책 예제

```sql
-- 사용자가 자신의 계정만 볼 수 있도록
CREATE POLICY user_policy ON accounts
    FOR ALL
    USING (user_name = current_user);

-- 모든 행 삽입 허용, 자신의 행만 수정 가능
CREATE POLICY insert_policy ON accounts
    FOR INSERT
    WITH CHECK (true);

CREATE POLICY update_policy ON accounts
    FOR UPDATE
    USING (user_name = current_user)
    WITH CHECK (user_name = current_user);
```

---

## 5.9. 스키마 (Schemas)

### 스키마 생성

```sql
CREATE SCHEMA myschema;
```

### 스키마 내 테이블 생성

```sql
CREATE TABLE myschema.products (
    product_no integer,
    name text,
    price numeric
);
```

### 스키마 삭제

```sql
DROP SCHEMA myschema;
DROP SCHEMA myschema CASCADE;  -- 포함된 모든 객체와 함께 삭제
```

### Public 스키마

기본적으로 테이블은 `public` 스키마에 생성됩니다:

```sql
CREATE TABLE products (...);
-- 다음과 동일:
CREATE TABLE public.products (...);
```

### 검색 경로 (Search Path)

```sql
SHOW search_path;
SET search_path TO myschema, public;
```

### 스키마 소유권과 권한

```sql
CREATE SCHEMA myschema AUTHORIZATION joe;
GRANT USAGE ON SCHEMA myschema TO public;
GRANT CREATE ON SCHEMA myschema TO joe;
```

---

## 5.10. 상속 (Inheritance)

### 테이블 상속

```sql
CREATE TABLE cities (
    name text,
    population real,
    elevation integer
);

CREATE TABLE capitals (
    state char(2)
) INHERITS (cities);
```

### 상속 쿼리

```sql
-- 모든 도시 조회 (수도 포함)
SELECT name, elevation FROM cities WHERE elevation > 500;

-- 수도가 아닌 도시만 조회
SELECT name, elevation FROM ONLY cities WHERE elevation > 500;
```

### 상속의 제한사항

- 기본 키와 외래 키는 상속 계층 전체에서 고유성을 보장하지 않습니다
- 인덱스는 상속되지 않습니다
- 트리거와 제약 조건은 조건부로 상속됩니다

---

## 5.11. 테이블 파티셔닝 (Table Partitioning)

### 파티셔닝 개요

파티셔닝은 논리적으로 하나의 큰 테이블을 더 작은 물리적 조각으로 나누는 것입니다.

### 파티셔닝의 이점

- 특정 조건의 쿼리 성능 향상 (파티션 프루닝)
- 대량 데이터 로드 및 삭제 효율성
- 자주 사용되지 않는 데이터를 저렴한 스토리지로 이동

### 5.11.1. 범위 파티셔닝 (Range Partitioning)

```sql
CREATE TABLE measurement (
    city_id int not null,
    logdate date not null,
    peaktemp int,
    unitsales int
) PARTITION BY RANGE (logdate);

CREATE TABLE measurement_y2020 PARTITION OF measurement
    FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');

CREATE TABLE measurement_y2021 PARTITION OF measurement
    FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');
```

### 5.11.2. 리스트 파티셔닝 (List Partitioning)

```sql
CREATE TABLE cities (
    city_id int not null,
    city_name text not null,
    population bigint
) PARTITION BY LIST (left(lower(city_name), 1));

CREATE TABLE cities_ab PARTITION OF cities
    FOR VALUES IN ('a', 'b');

CREATE TABLE cities_cd PARTITION OF cities
    FOR VALUES IN ('c', 'd');
```

### 5.11.3. 해시 파티셔닝 (Hash Partitioning)

```sql
CREATE TABLE orders (
    order_id int not null,
    order_date date not null,
    customer_id int
) PARTITION BY HASH (order_id);

CREATE TABLE orders_p0 PARTITION OF orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE orders_p1 PARTITION OF orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE orders_p2 PARTITION OF orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE orders_p3 PARTITION OF orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

### 파티션 관리

```sql
-- 파티션 추가
CREATE TABLE measurement_y2022 PARTITION OF measurement
    FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');

-- 파티션 분리
ALTER TABLE measurement DETACH PARTITION measurement_y2020;

-- 파티션 연결
ALTER TABLE measurement ATTACH PARTITION measurement_y2020
    FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');
```

---

## 5.12. 외래 데이터 (Foreign Data)

### 외래 데이터 래퍼 (FDW)

```sql
-- 외래 데이터 래퍼 생성
CREATE EXTENSION postgres_fdw;

-- 외래 서버 정의
CREATE SERVER foreign_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'remote_host', dbname 'remote_db', port '5432');

-- 사용자 매핑
CREATE USER MAPPING FOR local_user
    SERVER foreign_server
    OPTIONS (user 'remote_user', password 'secret');

-- 외래 테이블 생성
CREATE FOREIGN TABLE foreign_products (
    product_no integer,
    name text,
    price numeric
) SERVER foreign_server
  OPTIONS (schema_name 'public', table_name 'products');
```

### 외래 테이블 가져오기

```sql
IMPORT FOREIGN SCHEMA public
    FROM SERVER foreign_server
    INTO local_schema;
```

---

## 5.13. 기타 데이터베이스 객체

### 뷰 (Views)

```sql
CREATE VIEW expensive_products AS
    SELECT * FROM products WHERE price > 100;
```

### 구체화된 뷰 (Materialized Views)

```sql
CREATE MATERIALIZED VIEW mv_sales_summary AS
    SELECT product_no, SUM(quantity) as total_sold
    FROM orders
    GROUP BY product_no;

-- 새로고침
REFRESH MATERIALIZED VIEW mv_sales_summary;
```

### 함수 (Functions)

```sql
CREATE FUNCTION add_numbers(a integer, b integer)
RETURNS integer AS $$
    SELECT a + b;
$$ LANGUAGE SQL;
```

### 시퀀스 (Sequences)

```sql
CREATE SEQUENCE product_no_seq;

-- 사용
SELECT nextval('product_no_seq');

-- 테이블에서 사용
CREATE TABLE products (
    product_no integer DEFAULT nextval('product_no_seq'),
    name text,
    price numeric
);
```

### 트리거 (Triggers)

```sql
CREATE OR REPLACE FUNCTION log_update()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (table_name, operation, changed_at)
    VALUES (TG_TABLE_NAME, TG_OP, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_audit
    AFTER INSERT OR UPDATE OR DELETE ON products
    FOR EACH ROW
    EXECUTE FUNCTION log_update();
```

---

## 5.14. 의존성 추적 (Dependency Tracking)

PostgreSQL은 데이터베이스 객체 간의 의존성을 추적합니다.

### 의존성 확인

```sql
-- 테이블에 의존하는 객체 확인
SELECT * FROM pg_depend WHERE refobjid = 'products'::regclass;
```

### DROP 동작

```sql
-- 의존 객체가 있으면 실패
DROP TABLE products;

-- 의존 객체도 함께 삭제
DROP TABLE products CASCADE;

-- 의존 객체가 있으면 삭제하지 않음 (기본값)
DROP TABLE products RESTRICT;
```

---

# 참고 자료

- [PostgreSQL 18 공식 문서](https://www.postgresql.org/docs/18/)
- [PostgreSQL 18 데이터 정의](https://www.postgresql.org/docs/18/ddl.html)
- [PostgreSQL 18 테이블 파티셔닝](https://www.postgresql.org/docs/18/ddl-partitioning.html)
- [PostgreSQL 18 외래 데이터](https://www.postgresql.org/docs/18/ddl-foreign-data.html)
