# PostgreSQL 함수 및 프로시저

> 공식 문서: https://www.postgresql.org/docs/current/plpgsql.html

---

## SQL 함수

### 기본 SQL 함수

```sql
CREATE FUNCTION add_numbers(a INTEGER, b INTEGER)
RETURNS INTEGER
LANGUAGE SQL
AS $$
    SELECT a + b;
$$;

-- 사용
SELECT add_numbers(10, 20);  -- 30
```

### 테이블 반환 함수

```sql
CREATE FUNCTION get_active_users()
RETURNS TABLE(id INTEGER, name VARCHAR, email VARCHAR)
LANGUAGE SQL
AS $$
    SELECT id, name, email FROM users WHERE is_active = TRUE;
$$;

-- 사용
SELECT * FROM get_active_users();
```

### SETOF 반환

```sql
CREATE FUNCTION get_user_orders(user_id INTEGER)
RETURNS SETOF orders
LANGUAGE SQL
AS $$
    SELECT * FROM orders WHERE orders.user_id = get_user_orders.user_id;
$$;
```

---

## PL/pgSQL 함수

### 기본 구조

```sql
CREATE OR REPLACE FUNCTION function_name(parameters)
RETURNS return_type
LANGUAGE plpgsql
AS $$
DECLARE
    -- 변수 선언
BEGIN
    -- 실행 코드
    RETURN value;
END;
$$;
```

### 변수 선언

```sql
CREATE FUNCTION example_variables()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    user_id INTEGER;
    user_name VARCHAR(100);
    total NUMERIC(10, 2) := 0;
    today DATE := CURRENT_DATE;
    user_record users%ROWTYPE;
    custom_record RECORD;
BEGIN
    -- ...
END;
$$;
```

### 조건문

```sql
CREATE FUNCTION get_grade(score INTEGER)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
BEGIN
    IF score >= 90 THEN
        RETURN 'A';
    ELSIF score >= 80 THEN
        RETURN 'B';
    ELSIF score >= 70 THEN
        RETURN 'C';
    ELSE
        RETURN 'F';
    END IF;
END;
$$;
```

### CASE 표현식

```sql
CREATE FUNCTION get_status_text(status_code INTEGER)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN CASE status_code
        WHEN 1 THEN 'Active'
        WHEN 2 THEN 'Pending'
        WHEN 3 THEN 'Closed'
        ELSE 'Unknown'
    END;
END;
$$;
```

### 반복문

```sql
-- LOOP
CREATE FUNCTION count_to(n INTEGER)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    i INTEGER := 0;
    total INTEGER := 0;
BEGIN
    LOOP
        i := i + 1;
        total := total + i;
        EXIT WHEN i >= n;
    END LOOP;
    RETURN total;
END;
$$;

-- WHILE
CREATE FUNCTION factorial(n INTEGER)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    result BIGINT := 1;
    i INTEGER := n;
BEGIN
    WHILE i > 1 LOOP
        result := result * i;
        i := i - 1;
    END LOOP;
    RETURN result;
END;
$$;

-- FOR (정수 범위)
CREATE FUNCTION sum_range(start_val INTEGER, end_val INTEGER)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    total INTEGER := 0;
BEGIN
    FOR i IN start_val..end_val LOOP
        total := total + i;
    END LOOP;
    RETURN total;
END;
$$;

-- FOR (쿼리 결과)
CREATE FUNCTION process_users()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    user_rec RECORD;
BEGIN
    FOR user_rec IN SELECT * FROM users LOOP
        -- user_rec.id, user_rec.name 등 사용
        RAISE NOTICE 'Processing user: %', user_rec.name;
    END LOOP;
END;
$$;

-- FOREACH (배열)
CREATE FUNCTION array_sum(arr INTEGER[])
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    total INTEGER := 0;
    elem INTEGER;
BEGIN
    FOREACH elem IN ARRAY arr LOOP
        total := total + elem;
    END LOOP;
    RETURN total;
END;
$$;
```

---

## 프로시저 (PostgreSQL 11+)

프로시저는 값을 반환하지 않고 트랜잭션을 제어할 수 있습니다.

### 프로시저 생성

```sql
CREATE OR REPLACE PROCEDURE transfer_funds(
    sender_id INTEGER,
    receiver_id INTEGER,
    amount NUMERIC
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- 송금자 잔액 감소
    UPDATE accounts SET balance = balance - amount WHERE id = sender_id;

    -- 수신자 잔액 증가
    UPDATE accounts SET balance = balance + amount WHERE id = receiver_id;

    -- 명시적 커밋
    COMMIT;
END;
$$;
```

### 프로시저 호출

```sql
CALL transfer_funds(1, 2, 100.00);
```

### 트랜잭션 제어

```sql
CREATE PROCEDURE batch_process()
LANGUAGE plpgsql
AS $$
DECLARE
    batch_size INTEGER := 1000;
    processed INTEGER := 0;
BEGIN
    LOOP
        UPDATE large_table
        SET processed = TRUE
        WHERE id IN (
            SELECT id FROM large_table
            WHERE processed = FALSE
            LIMIT batch_size
        );

        GET DIAGNOSTICS processed = ROW_COUNT;

        IF processed = 0 THEN
            EXIT;
        END IF;

        COMMIT;  -- 배치마다 커밋
        RAISE NOTICE 'Processed % rows', processed;
    END LOOP;
END;
$$;
```

---

## 예외 처리

### 기본 예외 처리

```sql
CREATE FUNCTION safe_divide(a NUMERIC, b NUMERIC)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN a / b;
EXCEPTION
    WHEN division_by_zero THEN
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE NOTICE 'Error: %', SQLERRM;
        RETURN NULL;
END;
$$;
```

### 사용자 정의 예외

```sql
CREATE FUNCTION check_balance(account_id INTEGER, amount NUMERIC)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    current_balance NUMERIC;
BEGIN
    SELECT balance INTO current_balance FROM accounts WHERE id = account_id;

    IF current_balance < amount THEN
        RAISE EXCEPTION 'Insufficient funds: balance is %, requested %',
            current_balance, amount
            USING ERRCODE = 'P0001';
    END IF;
END;
$$;
```

### 예외 정보 조회

```sql
CREATE FUNCTION detailed_error_handling()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_state TEXT;
    v_msg TEXT;
    v_detail TEXT;
    v_hint TEXT;
    v_context TEXT;
BEGIN
    -- 위험한 작업
EXCEPTION
    WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS
            v_state   = RETURNED_SQLSTATE,
            v_msg     = MESSAGE_TEXT,
            v_detail  = PG_EXCEPTION_DETAIL,
            v_hint    = PG_EXCEPTION_HINT,
            v_context = PG_EXCEPTION_CONTEXT;

        RAISE NOTICE 'Error state: %', v_state;
        RAISE NOTICE 'Error message: %', v_msg;
END;
$$;
```

---

## 동적 SQL

### EXECUTE 사용

```sql
CREATE FUNCTION dynamic_query(table_name TEXT, id INTEGER)
RETURNS SETOF RECORD
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY EXECUTE format('SELECT * FROM %I WHERE id = $1', table_name)
    USING id;
END;
$$;
```

### format() 함수

```sql
-- %s: 문자열 (따옴표 없음)
-- %I: 식별자 (필요시 따옴표)
-- %L: 리터럴 (따옴표 포함)

CREATE FUNCTION build_query(schema_name TEXT, table_name TEXT, column_name TEXT, value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN format(
        'SELECT * FROM %I.%I WHERE %I = %L',
        schema_name, table_name, column_name, value
    );
END;
$$;
```

---

## 트리거 함수

### 트리거 함수 생성

```sql
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- 트리거 생성
CREATE TRIGGER users_update_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();
```

### 트리거 특수 변수

| 변수 | 설명 |
|------|------|
| `NEW` | 새 행 데이터 (INSERT/UPDATE) |
| `OLD` | 이전 행 데이터 (UPDATE/DELETE) |
| `TG_OP` | 작업 유형 (INSERT/UPDATE/DELETE) |
| `TG_TABLE_NAME` | 테이블 이름 |
| `TG_WHEN` | BEFORE/AFTER/INSTEAD OF |

### 조건부 트리거

```sql
CREATE FUNCTION audit_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (operation, old_data)
        VALUES ('DELETE', row_to_json(OLD));
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (operation, old_data, new_data)
        VALUES ('UPDATE', row_to_json(OLD), row_to_json(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (operation, new_data)
        VALUES ('INSERT', row_to_json(NEW));
        RETURN NEW;
    END IF;
END;
$$;

CREATE TRIGGER users_audit
AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW
EXECUTE FUNCTION audit_changes();
```

---

## 집계 함수 (Aggregate)

### 사용자 정의 집계

```sql
-- 상태 전이 함수
CREATE FUNCTION array_append_agg(acc TEXT[], val TEXT)
RETURNS TEXT[]
LANGUAGE SQL
AS $$
    SELECT acc || val;
$$;

-- 집계 함수 생성
CREATE AGGREGATE string_agg_custom(TEXT) (
    SFUNC = array_append_agg,
    STYPE = TEXT[],
    INITCOND = '{}'
);
```

---

## 함수 속성

### VOLATILITY

```sql
-- IMMUTABLE: 같은 입력에 항상 같은 결과
CREATE FUNCTION double_value(x INTEGER)
RETURNS INTEGER
LANGUAGE SQL
IMMUTABLE
AS $$ SELECT x * 2; $$;

-- STABLE: 같은 트랜잭션 내 같은 결과
CREATE FUNCTION current_user_id()
RETURNS INTEGER
LANGUAGE SQL
STABLE
AS $$ SELECT current_setting('app.user_id')::INTEGER; $$;

-- VOLATILE: 매번 다른 결과 가능 (기본값)
CREATE FUNCTION random_value()
RETURNS DOUBLE PRECISION
LANGUAGE SQL
VOLATILE
AS $$ SELECT random(); $$;
```

### SECURITY

```sql
-- SECURITY INVOKER: 호출자 권한 (기본값)
CREATE FUNCTION get_my_data()
RETURNS TABLE(...)
LANGUAGE SQL
SECURITY INVOKER
AS $$ SELECT * FROM users WHERE user_id = current_user_id(); $$;

-- SECURITY DEFINER: 함수 정의자 권한
CREATE FUNCTION admin_only_function()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- admin 권한으로 실행
END;
$$;
```

### 병렬 처리

```sql
CREATE FUNCTION parallel_safe_func(x INTEGER)
RETURNS INTEGER
LANGUAGE SQL
PARALLEL SAFE  -- UNSAFE, RESTRICTED, SAFE
AS $$ SELECT x * 2; $$;
```

---

## 함수 관리

### 함수 조회

```sql
-- psql 명령어
\df                    -- 함수 목록
\df+ function_name     -- 상세 정보

-- SQL로 조회
SELECT routine_name, routine_type, data_type
FROM information_schema.routines
WHERE routine_schema = 'public';

-- 함수 정의 보기
SELECT pg_get_functiondef('function_name'::regproc);
```

### 함수 삭제

```sql
DROP FUNCTION function_name(parameter_types);
DROP FUNCTION IF EXISTS function_name(INTEGER, VARCHAR);

-- CASCADE: 의존 객체도 삭제
DROP FUNCTION function_name CASCADE;
```

### 함수 이름 변경

```sql
ALTER FUNCTION old_name(INTEGER) RENAME TO new_name;
```

---

## 유용한 패턴

### 기본값 매개변수

```sql
CREATE FUNCTION get_users(limit_count INTEGER DEFAULT 10, offset_count INTEGER DEFAULT 0)
RETURNS TABLE(id INTEGER, name VARCHAR)
LANGUAGE SQL
AS $$
    SELECT id, name FROM users
    ORDER BY id
    LIMIT limit_count OFFSET offset_count;
$$;

-- 사용
SELECT * FROM get_users();         -- 기본값 사용
SELECT * FROM get_users(20);       -- limit만 지정
SELECT * FROM get_users(20, 10);   -- 둘 다 지정
```

### OUT 매개변수

```sql
CREATE FUNCTION get_user_stats(
    user_id INTEGER,
    OUT total_orders INTEGER,
    OUT total_amount NUMERIC
)
LANGUAGE SQL
AS $$
    SELECT COUNT(*), SUM(amount)
    FROM orders
    WHERE orders.user_id = get_user_stats.user_id;
$$;

-- 사용
SELECT * FROM get_user_stats(1);
```

### VARIADIC 매개변수

```sql
CREATE FUNCTION sum_all(VARIADIC numbers INTEGER[])
RETURNS INTEGER
LANGUAGE SQL
AS $$
    SELECT SUM(n) FROM unnest(numbers) AS n;
$$;

-- 사용
SELECT sum_all(1, 2, 3, 4, 5);
```

---

## 참고 문서

- [PL/pgSQL](https://www.postgresql.org/docs/current/plpgsql.html)
- [SQL 함수](https://www.postgresql.org/docs/current/xfunc-sql.html)
- [트리거 함수](https://www.postgresql.org/docs/current/plpgsql-trigger.html)
- [집계 함수](https://www.postgresql.org/docs/current/xaggr.html)
