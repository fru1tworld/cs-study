# Chapter 39: 트리거 (Triggers)

## 목차

1. [트리거 개요](#1-트리거-개요)
2. [트리거 동작의 개요](#2-트리거-동작의-개요)
3. [데이터 변경의 가시성](#3-데이터-변경의-가시성-visibility-of-data-changes)
4. [트리거 함수 작성](#4-트리거-함수-작성)
5. [트리거 생성 및 관리](#5-트리거-생성-및-관리)
6. [완전한 트리거 예제](#6-완전한-트리거-예제)
7. [고급 트리거 기능](#7-고급-트리거-기능)

---

## 1. 트리거 개요

트리거(Trigger)는 특정 유형의 작업이 수행될 때마다 데이터베이스가 자동으로 특정 함수를 실행하도록 하는 명세입니다. 트리거는 다음 객체에 연결할 수 있습니다:

- 테이블 (파티션 테이블 포함)
- 뷰 (Views)
- 외부 테이블 (Foreign Tables)

### 1.1 트리거의 용도

트리거는 다음과 같은 용도로 널리 사용됩니다:

- 데이터 무결성 검증: 복잡한 제약 조건 구현
- 감사 로깅 (Audit Logging): 데이터 변경 이력 추적
- 자동 값 계산: 파생 컬럼 자동 갱신
- 복잡한 비즈니스 로직 구현: 자동화된 워크플로우
- 데이터 동기화: 관련 테이블 간 일관성 유지

### 1.2 지원되는 프로시저 언어

트리거 함수는 다음 언어로 작성할 수 있습니다:

| 언어 | 설명 |
|------|------|
| PL/pgSQL | PostgreSQL의 기본 절차적 언어 |
| PL/Tcl | Tcl 절차적 언어 |
| PL/Perl | Perl 절차적 언어 |
| PL/Python | Python 절차적 언어 |
| C | C 언어 (복잡하지만 가능) |

> 주의: 순수 SQL 함수 언어는 트리거 함수로 사용할 수 없습니다.

---

## 2. 트리거 동작의 개요

### 2.1 트리거 유형

#### 테이블 및 외부 테이블에서의 트리거

| 작업 | 설명 |
|------|------|
| `INSERT` | 새 행 삽입 시 |
| `UPDATE` | 기존 행 갱신 시 |
| `DELETE` | 행 삭제 시 |
| `TRUNCATE` | 테이블 잘라내기 시 |

#### 뷰에서의 트리거

뷰에서는 `INSTEAD OF` 트리거를 사용하여 `INSERT`, `UPDATE`, `DELETE` 작업을 처리합니다.

### 2.2 트리거 실행 시점

| 시점 | 설명 |
|------|------|
| BEFORE | 작업 실행 전에 트리거 발동 |
| AFTER | 작업 실행 후에 트리거 발동 |
| INSTEAD OF | 작업 대신 트리거 실행 (뷰 전용, 행 수준만 가능) |

### 2.3 트리거 실행 수준

| 수준 | 설명 |
|------|------|
| 행 수준 (FOR EACH ROW) | 영향받는 각 행마다 한 번씩 실행 |
| 문장 수준 (FOR EACH STATEMENT) | SQL 문당 한 번만 실행 (영향받는 행 수와 무관) |

### 2.4 트리거 함수 요구사항

트리거 함수는 반드시:

1. 인자 없이 정의되어야 함
2. 반환 타입이 `trigger`이어야 함
3. 트리거 정의 전에 생성되어야 함

```sql
-- 트리거 함수 기본 구조
CREATE FUNCTION trigger_function_name()
RETURNS trigger AS $$
BEGIN
    -- 트리거 로직
    RETURN NEW;  -- 또는 OLD, NULL
END;
$$ LANGUAGE plpgsql;
```

### 2.5 트리거 실행 순서

동일한 이벤트에 대해 동일한 테이블에 여러 트리거가 정의된 경우:

- 트리거 이름의 알파벳 순서 로 실행됩니다
- 각 트리거에서 수정된 행이 다음 트리거의 입력이 됩니다
- `BEFORE` 또는 `INSTEAD OF` 트리거가 `NULL`을 반환하면 해당 행에 대한 작업이 취소됩니다

---

## 3. 데이터 변경의 가시성 (Visibility of Data Changes)

트리거 내에서 데이터 변경의 가시성은 트리거 유형과 실행 시점에 따라 달라집니다.

### 3.1 문장 수준 트리거

| 시점 | 가시성 |
|------|--------|
| BEFORE | 해당 문장에 의한 변경 사항을 볼 수 없음 |
| AFTER | 해당 문장에 의한 모든 수정 사항을 볼 수 있음 |

### 3.2 행 수준 트리거

| 트리거 유형 | 가시성 |
|-------------|--------|
| BEFORE | 트리거를 발동시킨 데이터 변경을 볼 수 없음 (아직 발생하지 않음). 같은 외부 명령에서 이전에 처리된 행의 변경 효과는 볼 수 있음 |
| AFTER | 외부 명령에 의한 모든 데이터 변경을 볼 수 있음 (이미 완료됨) |
| INSTEAD OF | 같은 외부 명령에서 이전 INSTEAD OF 트리거 발동에 의한 데이터 변경 효과를 볼 수 있음 |

### 3.3 함수 휘발성 (Volatility) 고려사항

표준 절차적 언어로 작성된 트리거 함수의 경우:

| 휘발성 | 동작 |
|--------|------|
| VOLATILE (기본값) | 위의 가시성 규칙이 적용됨 |
| STABLE 또는 IMMUTABLE | 트리거 유형에 관계없이 호출 명령에 의한 변경을 볼 수 없음 |

> 주의: 행 처리 순서는 명령이 여러 행에 영향을 미칠 때 예측할 수 없으므로, BEFORE 트리거에서 이전에 처리된 행의 변경 효과에 의존하는 것은 권장되지 않습니다.

---

## 4. 트리거 함수 작성

### 4.1 PL/pgSQL 트리거 함수의 특수 변수

PL/pgSQL 함수가 트리거로 호출되면 다음 특수 변수들이 자동으로 생성됩니다:

#### 레코드 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `NEW` | record | INSERT/UPDATE 작업에서 새로운 데이터베이스 행. DELETE 작업이나 문장 수준 트리거에서는 NULL |
| `OLD` | record | UPDATE/DELETE 작업에서 이전 데이터베이스 행. INSERT 작업이나 문장 수준 트리거에서는 NULL |

#### 컨텍스트 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `TG_NAME` | name | 발동된 트리거의 이름 |
| `TG_WHEN` | text | BEFORE, AFTER, 또는 INSTEAD OF |
| `TG_LEVEL` | text | ROW 또는 STATEMENT |
| `TG_OP` | text | 작업 유형: INSERT, UPDATE, DELETE, 또는 TRUNCATE |
| `TG_RELID` | oid | 트리거를 발생시킨 테이블의 객체 ID |
| `TG_TABLE_NAME` | name | 트리거를 발생시킨 테이블 이름 |
| `TG_TABLE_SCHEMA` | name | 테이블의 스키마 이름 |
| `TG_NARGS` | integer | CREATE TRIGGER 문에서 제공된 인자 수 |
| `TG_ARGV[]` | text[] | CREATE TRIGGER 문의 인자 배열 (0부터 인덱싱) |

### 4.2 반환 값 규칙

#### 행 수준 BEFORE 트리거

| 반환 값 | 효과 |
|---------|------|
| `NULL` | 이 행에 대한 작업 건너뜀 |
| 수정된 `NEW` | 변경된 행으로 삽입/갱신 진행 |
| `NEW` 그대로 | 원래 행으로 작업 진행 |

```sql
-- BEFORE 트리거 반환 값 예제
CREATE FUNCTION validate_and_modify() RETURNS trigger AS $$
BEGIN
    -- 조건 검사 후 작업 건너뛰기
    IF NEW.value < 0 THEN
        RETURN NULL;  -- 이 행에 대한 작업 취소
    END IF;

    -- 값 수정
    NEW.modified_at := current_timestamp;
    RETURN NEW;  -- 수정된 행으로 진행
END;
$$ LANGUAGE plpgsql;
```

#### 행 수준 INSTEAD OF 트리거

| 반환 값 | 효과 |
|---------|------|
| `NULL` | 수정이 수행되지 않았음을 신호 |
| `NEW` (INSERT/UPDATE) | RETURNING 절 지원, 데이터 수정됨을 신호 |
| `OLD` (DELETE) | RETURNING 절 지원, 데이터 수정됨을 신호 |

#### 행 수준 AFTER 트리거 및 문장 수준 트리거

반환 값은 무시됩니다 (NULL 반환 가능). 그러나 오류를 발생시켜 작업을 중단할 수 있습니다.

### 4.3 기본 트리거 함수 예제

#### 예제 1: 데이터 검증 및 자동 값 설정

```sql
-- 테이블 정의
CREATE TABLE emp (
    empname           text,
    salary            integer,
    last_date         timestamp,
    last_user         text
);

-- 트리거 함수 정의
CREATE FUNCTION emp_stamp() RETURNS trigger AS $emp_stamp$
BEGIN
    -- 필수 필드 검증
    IF NEW.empname IS NULL THEN
        RAISE EXCEPTION 'empname cannot be null';
    END IF;

    IF NEW.salary IS NULL THEN
        RAISE EXCEPTION '% cannot have null salary', NEW.empname;
    END IF;

    -- 비즈니스 규칙 적용
    IF NEW.salary < 0 THEN
        RAISE EXCEPTION '% cannot have a negative salary', NEW.empname;
    END IF;

    -- 자동으로 변경 이력 기록
    NEW.last_date := current_timestamp;
    NEW.last_user := current_user;

    RETURN NEW;
END;
$emp_stamp$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_stamp
    BEFORE INSERT OR UPDATE ON emp
    FOR EACH ROW
    EXECUTE FUNCTION emp_stamp();
```

#### 예제 2: 감사 로깅 (Audit Logging)

```sql
-- 원본 테이블
CREATE TABLE emp (
    empname           text NOT NULL,
    salary            integer
);

-- 감사 테이블
CREATE TABLE emp_audit (
    operation         char(1)   NOT NULL,
    stamp             timestamp NOT NULL,
    userid            text      NOT NULL,
    empname           text      NOT NULL,
    salary            integer
);

-- 감사 트리거 함수
CREATE OR REPLACE FUNCTION process_emp_audit() RETURNS trigger AS $emp_audit$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO emp_audit
        SELECT 'D', now(), current_user, OLD.*;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO emp_audit
        SELECT 'U', now(), current_user, NEW.*;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp_audit
        SELECT 'I', now(), current_user, NEW.*;
    END IF;

    RETURN NULL;  -- AFTER 트리거이므로 반환값 무시됨
END;
$emp_audit$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_audit
    AFTER INSERT OR UPDATE OR DELETE ON emp
    FOR EACH ROW
    EXECUTE FUNCTION process_emp_audit();
```

#### 예제 3: 뷰에 대한 INSTEAD OF 트리거

```sql
-- 기본 테이블
CREATE TABLE emp (
    empname           text PRIMARY KEY,
    salary            integer
);

-- 뷰 생성
CREATE VIEW emp_view AS
    SELECT e.empname, e.salary
    FROM emp e;

-- INSTEAD OF 트리거 함수
CREATE OR REPLACE FUNCTION update_emp_view() RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        DELETE FROM emp WHERE empname = OLD.empname;
        IF NOT FOUND THEN
            RETURN NULL;
        END IF;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE') THEN
        UPDATE emp SET salary = NEW.salary
        WHERE empname = OLD.empname;
        IF NOT FOUND THEN
            RETURN NULL;
        END IF;
        RETURN NEW;

    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp VALUES(NEW.empname, NEW.salary);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_view_trigger
    INSTEAD OF INSERT OR UPDATE OR DELETE ON emp_view
    FOR EACH ROW
    EXECUTE FUNCTION update_emp_view();
```

---

## 5. 트리거 생성 및 관리

### 5.1 CREATE TRIGGER 구문

```sql
CREATE [ OR REPLACE ] [ CONSTRAINT ] TRIGGER name
    { BEFORE | AFTER | INSTEAD OF } { event [ OR ... ] }
    ON table_name
    [ FROM referenced_table_name ]
    [ NOT DEFERRABLE | [ DEFERRABLE ] [ INITIALLY IMMEDIATE | INITIALLY DEFERRED ] ]
    [ REFERENCING { { OLD | NEW } TABLE [ AS ] transition_relation_name } [ ... ] ]
    [ FOR [ EACH ] { ROW | STATEMENT } ]
    [ WHEN ( condition ) ]
    EXECUTE { FUNCTION | PROCEDURE } function_name ( arguments )
```

여기서 `event`는 다음 중 하나입니다:

- `INSERT`
- `UPDATE [ OF column_name [, ... ] ]`
- `DELETE`
- `TRUNCATE`

### 5.2 기본 트리거 생성 예제

```sql
-- BEFORE INSERT 행 수준 트리거
CREATE TRIGGER check_insert
    BEFORE INSERT ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION check_account_insert();

-- AFTER UPDATE 문장 수준 트리거
CREATE TRIGGER summarize_update
    AFTER UPDATE ON sales
    FOR EACH STATEMENT
    EXECUTE FUNCTION summarize_sales();

-- 특정 컬럼 UPDATE 시에만 실행
CREATE TRIGGER update_salary_trigger
    AFTER UPDATE OF salary ON employees
    FOR EACH ROW
    EXECUTE FUNCTION log_salary_change();
```

### 5.3 조건부 트리거 (WHEN 절)

`WHEN` 절을 사용하여 트리거 발동 조건을 지정할 수 있습니다:

```sql
-- 급여가 변경된 경우에만 트리거 실행
CREATE TRIGGER salary_change_trigger
    AFTER UPDATE ON employees
    FOR EACH ROW
    WHEN (OLD.salary IS DISTINCT FROM NEW.salary)
    EXECUTE FUNCTION log_salary_change();

-- 특정 조건을 만족하는 경우에만 실행
CREATE TRIGGER high_value_insert
    BEFORE INSERT ON orders
    FOR EACH ROW
    WHEN (NEW.amount > 10000)
    EXECUTE FUNCTION validate_high_value_order();
```

> 성능 팁: `WHEN` 조건을 사용하면 불필요한 트리거 실행을 방지하여 성능을 크게 향상시킬 수 있습니다.

### 5.4 트리거 관리 명령

#### 트리거 비활성화/활성화

```sql
-- 특정 트리거 비활성화
ALTER TABLE table_name DISABLE TRIGGER trigger_name;

-- 특정 트리거 활성화
ALTER TABLE table_name ENABLE TRIGGER trigger_name;

-- 테이블의 모든 트리거 비활성화
ALTER TABLE table_name DISABLE TRIGGER ALL;

-- 테이블의 모든 사용자 트리거 비활성화 (시스템 생성 트리거 제외)
ALTER TABLE table_name DISABLE TRIGGER USER;
```

#### 트리거 삭제

```sql
-- 트리거 삭제
DROP TRIGGER [ IF EXISTS ] trigger_name ON table_name [ CASCADE | RESTRICT ];

-- 예제
DROP TRIGGER emp_stamp ON emp;
DROP TRIGGER IF EXISTS emp_audit ON emp CASCADE;
```

#### 트리거 정보 조회

```sql
-- 테이블의 트리거 목록 조회
SELECT trigger_name, event_manipulation, action_timing, action_statement
FROM information_schema.triggers
WHERE event_object_table = 'emp';

-- pg_trigger 시스템 카탈로그에서 상세 정보 조회
SELECT tgname, tgenabled, tgtype
FROM pg_trigger
WHERE tgrelid = 'emp'::regclass;
```

---

## 6. 완전한 트리거 예제

### 6.1 재고 관리 시스템 예제

```sql
-- 제품 테이블
CREATE TABLE products (
    product_id    serial PRIMARY KEY,
    product_name  text NOT NULL,
    quantity      integer NOT NULL DEFAULT 0,
    reorder_level integer NOT NULL DEFAULT 10,
    last_updated  timestamp DEFAULT current_timestamp
);

-- 재고 이력 테이블
CREATE TABLE inventory_log (
    log_id        serial PRIMARY KEY,
    product_id    integer REFERENCES products(product_id),
    old_quantity  integer,
    new_quantity  integer,
    change_amount integer,
    change_type   text,
    changed_by    text,
    changed_at    timestamp DEFAULT current_timestamp
);

-- 재주문 알림 테이블
CREATE TABLE reorder_alerts (
    alert_id      serial PRIMARY KEY,
    product_id    integer REFERENCES products(product_id),
    product_name  text,
    current_qty   integer,
    reorder_level integer,
    created_at    timestamp DEFAULT current_timestamp
);

-- 재고 변경 로깅 및 재주문 알림 트리거 함수
CREATE OR REPLACE FUNCTION manage_inventory() RETURNS trigger AS $$
DECLARE
    change_type_val text;
    change_amount_val integer;
BEGIN
    -- 변경 유형 및 수량 결정
    IF TG_OP = 'INSERT' THEN
        change_type_val := 'INITIAL';
        change_amount_val := NEW.quantity;

        -- 이력 기록
        INSERT INTO inventory_log (product_id, old_quantity, new_quantity,
                                   change_amount, change_type, changed_by)
        VALUES (NEW.product_id, 0, NEW.quantity,
                change_amount_val, change_type_val, current_user);

    ELSIF TG_OP = 'UPDATE' THEN
        -- 수량이 변경된 경우에만 처리
        IF OLD.quantity != NEW.quantity THEN
            change_amount_val := NEW.quantity - OLD.quantity;

            IF change_amount_val > 0 THEN
                change_type_val := 'RESTOCK';
            ELSE
                change_type_val := 'SALE';
            END IF;

            -- 이력 기록
            INSERT INTO inventory_log (product_id, old_quantity, new_quantity,
                                       change_amount, change_type, changed_by)
            VALUES (NEW.product_id, OLD.quantity, NEW.quantity,
                    change_amount_val, change_type_val, current_user);

            -- 재주문 수준 이하로 떨어진 경우 알림 생성
            IF NEW.quantity <= NEW.reorder_level AND OLD.quantity > OLD.reorder_level THEN
                INSERT INTO reorder_alerts (product_id, product_name,
                                            current_qty, reorder_level)
                VALUES (NEW.product_id, NEW.product_name,
                        NEW.quantity, NEW.reorder_level);
            END IF;
        END IF;

        -- 마지막 업데이트 시간 자동 갱신
        NEW.last_updated := current_timestamp;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER inventory_management
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION manage_inventory();
```

### 6.2 사용 예제

```sql
-- 제품 추가
INSERT INTO products (product_name, quantity, reorder_level)
VALUES ('Widget A', 100, 20);

-- 재고 감소 (판매)
UPDATE products SET quantity = quantity - 15 WHERE product_id = 1;

-- 재고 감소 (재주문 수준 이하로)
UPDATE products SET quantity = 18 WHERE product_id = 1;

-- 재고 확인
SELECT * FROM products;

-- 재고 이력 확인
SELECT * FROM inventory_log ORDER BY changed_at DESC;

-- 재주문 알림 확인
SELECT * FROM reorder_alerts;
```

---

## 7. 고급 트리거 기능

### 7.1 전이 테이블 (Transition Tables)

`AFTER STATEMENT` 트리거에서 전이 테이블을 사용하면 영향받은 모든 행을 한 번에 처리할 수 있습니다. 이는 대량 데이터 처리 시 행 수준 트리거보다 훨씬 효율적입니다.

```sql
-- 전이 테이블을 사용한 감사 트리거
CREATE OR REPLACE FUNCTION process_emp_audit_bulk() RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO emp_audit
            SELECT 'D', now(), current_user, o.*
            FROM old_table o;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO emp_audit
            SELECT 'U', now(), current_user, n.*
            FROM new_table n;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp_audit
            SELECT 'I', now(), current_user, n.*
            FROM new_table n;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- INSERT용 트리거 (새 행 전이 테이블)
CREATE TRIGGER emp_audit_ins
    AFTER INSERT ON emp
    REFERENCING NEW TABLE AS new_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();

-- UPDATE용 트리거 (이전/새 행 전이 테이블)
CREATE TRIGGER emp_audit_upd
    AFTER UPDATE ON emp
    REFERENCING OLD TABLE AS old_table NEW TABLE AS new_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();

-- DELETE용 트리거 (이전 행 전이 테이블)
CREATE TRIGGER emp_audit_del
    AFTER DELETE ON emp
    REFERENCING OLD TABLE AS old_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();
```

### 7.2 제약 트리거 (Constraint Triggers)

제약 트리거는 트랜잭션 끝까지 실행을 지연시킬 수 있습니다:

```sql
-- 제약 트리거 생성
CREATE CONSTRAINT TRIGGER check_foreign_key
    AFTER INSERT OR UPDATE ON child_table
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION check_fk_constraint();
```

### 7.3 트리거 인자 사용

트리거 함수에 인자를 전달하여 재사용 가능한 범용 트리거를 만들 수 있습니다:

```sql
-- 범용 로깅 트리거 함수
CREATE OR REPLACE FUNCTION generic_audit_trigger() RETURNS trigger AS $$
DECLARE
    audit_table text;
BEGIN
    -- 첫 번째 인자로 감사 테이블 이름 받기
    audit_table := TG_ARGV[0];

    -- 동적 SQL로 감사 레코드 삽입
    EXECUTE format('INSERT INTO %I (operation, table_name, changed_at, changed_by)
                    VALUES ($1, $2, $3, $4)', audit_table)
    USING TG_OP, TG_TABLE_NAME, current_timestamp, current_user;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 여러 테이블에 동일한 함수 사용
CREATE TRIGGER orders_audit
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION generic_audit_trigger('orders_audit_log');

CREATE TRIGGER customers_audit
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION generic_audit_trigger('customers_audit_log');
```

### 7.4 캐스케이딩 트리거

트리거 함수 내에서 다른 트리거를 발동시키는 SQL을 실행할 수 있습니다:

```sql
-- 캐스케이딩 삭제를 구현하는 트리거
CREATE OR REPLACE FUNCTION cascade_delete_orders() RETURNS trigger AS $$
BEGIN
    -- 관련 주문 항목 삭제 (order_items 테이블의 트리거도 발동됨)
    DELETE FROM order_items WHERE order_id = OLD.order_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_cascade_delete
    BEFORE DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION cascade_delete_orders();
```

> 주의: 캐스케이딩 트리거 사용 시 무한 재귀를 방지해야 합니다. 프로그래머가 무한 루프가 발생하지 않도록 책임져야 합니다.

### 7.5 생성된 컬럼과 트리거

저장된 생성 컬럼(Stored Generated Columns)은 `BEFORE` 트리거 이후, `AFTER` 트리거 이전에 계산됩니다:

- BEFORE 트리거: `OLD` 행에는 이전 생성 값이 포함됨, `NEW` 행에는 새 생성 값이 아직 없음
- 가상 생성 컬럼 (Virtual Generated Columns): 트리거가 발동할 때 계산되지 않음

---

## 요약

PostgreSQL 트리거는 데이터베이스에서 자동화된 로직을 구현하는 강력한 도구입니다:

| 기능 | 설명 |
|------|------|
| 데이터 무결성 | 복잡한 비즈니스 규칙 자동 적용 |
| 감사 추적 | 데이터 변경 이력 자동 기록 |
| 자동 계산 | 파생 값 자동 갱신 |
| 조건부 실행 | WHEN 절로 선택적 트리거 발동 |
| 전이 테이블 | 대량 데이터 효율적 처리 |
| 지연 실행 | 제약 트리거로 트랜잭션 끝까지 지연 |

트리거를 효과적으로 사용하면 애플리케이션 코드를 단순화하고, 데이터베이스 수준에서 일관된 비즈니스 로직을 유지할 수 있습니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - Triggers](https://www.postgresql.org/docs/current/triggers.html)
- [PostgreSQL 공식 문서 - CREATE TRIGGER](https://www.postgresql.org/docs/current/sql-createtrigger.html)
- [PostgreSQL 공식 문서 - PL/pgSQL Trigger Functions](https://www.postgresql.org/docs/current/plpgsql-trigger.html)
