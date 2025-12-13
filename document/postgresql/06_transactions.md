# PostgreSQL 트랜잭션 및 동시성 제어

> 공식 문서: https://www.postgresql.org/docs/current/mvcc.html

---

## 트랜잭션 기초

### 트랜잭션이란?

트랜잭션은 하나의 논리적 작업 단위로 처리되는 SQL 명령문의 집합입니다. ACID 속성을 보장합니다.

### 기본 구문

```sql
BEGIN;  -- 또는 START TRANSACTION;
    -- SQL 명령들
COMMIT;  -- 변경사항 저장

-- 또는

BEGIN;
    -- SQL 명령들
ROLLBACK;  -- 변경사항 취소
```

### 예제

```sql
BEGIN;
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;

-- 오류 발생 시 롤백
BEGIN;
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    -- 잔액 부족 등 오류 발생
ROLLBACK;
```

---

## 자동 커밋 (Autocommit)

PostgreSQL은 기본적으로 각 SQL 문을 자동으로 커밋합니다.

```sql
-- 자동 커밋 모드에서는 각 문장이 개별 트랜잭션
UPDATE users SET name = 'John';  -- 즉시 커밋됨

-- 명시적 트랜잭션으로 자동 커밋 비활성화
BEGIN;
    UPDATE users SET name = 'John';
    -- 아직 커밋되지 않음
COMMIT;
```

---

## SAVEPOINT

트랜잭션 내에서 부분 롤백 지점을 설정합니다.

```sql
BEGIN;
    INSERT INTO orders (user_id, amount) VALUES (1, 100);
    SAVEPOINT sp1;

    INSERT INTO order_items (order_id, product_id) VALUES (1, 100);
    -- 오류 발생!
    ROLLBACK TO SAVEPOINT sp1;

    -- sp1 이후 작업만 취소됨
    INSERT INTO order_items (order_id, product_id) VALUES (1, 200);
COMMIT;
```

```sql
-- SAVEPOINT 해제
RELEASE SAVEPOINT sp1;
```

---

## MVCC (다중 버전 동시성 제어)

PostgreSQL은 MVCC를 사용하여 동시성을 관리합니다.

### MVCC 원리

- 각 트랜잭션은 데이터의 ** 스냅샷** 을 봅니다
- 읽기가 쓰기를 차단하지 않습니다
- 쓰기가 읽기를 차단하지 않습니다
- 데이터 수정 시 새 버전을 생성합니다

### 튜플 버전 관리

```sql
-- 각 행은 숨겨진 시스템 컬럼을 가짐
SELECT xmin, xmax, ctid, * FROM users;

-- xmin: 행을 생성한 트랜잭션 ID
-- xmax: 행을 삭제/업데이트한 트랜잭션 ID (0이면 활성)
-- ctid: 행의 물리적 위치
```

---

## 트랜잭션 격리 수준

### 격리 수준 개요

| 격리 수준 | Dirty Read | Non-repeatable Read | Phantom Read | Serialization Anomaly |
|----------|------------|--------------------|--------------|-----------------------|
| Read Uncommitted* | 불가능 | 가능 | 가능 | 가능 |
| Read Committed (기본) | 불가능 | 가능 | 가능 | 가능 |
| Repeatable Read | 불가능 | 불가능 | 불가능** | 가능 |
| Serializable | 불가능 | 불가능 | 불가능 | 불가능 |

> *PostgreSQL에서 Read Uncommitted는 Read Committed로 동작
> **PostgreSQL의 Repeatable Read는 Phantom Read도 방지

### 격리 수준 설정

```sql
-- 트랜잭션 시작 시 설정
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 또는
BEGIN;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- 세션 기본값 설정
SET default_transaction_isolation = 'repeatable read';
```

### Read Committed (기본값)

각 쿼리가 실행 시점의 커밋된 데이터만 봅니다.

```sql
-- 세션 A
BEGIN;
SELECT balance FROM accounts WHERE id = 1;  -- 1000

-- 세션 B
BEGIN;
UPDATE accounts SET balance = 500 WHERE id = 1;
COMMIT;

-- 세션 A
SELECT balance FROM accounts WHERE id = 1;  -- 500 (변경된 값!)
COMMIT;
```

### Repeatable Read

트랜잭션 시작 시점의 스냅샷을 일관되게 봅니다.

```sql
-- 세션 A
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM accounts WHERE id = 1;  -- 1000

-- 세션 B
BEGIN;
UPDATE accounts SET balance = 500 WHERE id = 1;
COMMIT;

-- 세션 A
SELECT balance FROM accounts WHERE id = 1;  -- 1000 (여전히 동일!)
COMMIT;
```

** 직렬화 실패:**

```sql
-- 세션 A
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
UPDATE accounts SET balance = balance + 100 WHERE id = 1;

-- 세션 B
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
UPDATE accounts SET balance = balance + 50 WHERE id = 1;  -- 대기

-- 세션 A
COMMIT;

-- 세션 B
-- ERROR: could not serialize access due to concurrent update
ROLLBACK;
```

### Serializable

가장 엄격한 격리 수준으로, 직렬 실행과 동일한 결과를 보장합니다.

```sql
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
-- 트랜잭션이 직렬로 실행된 것처럼 동작
-- 충돌 시 serialization_failure 오류 발생
COMMIT;
```

---

## 락 (Locking)

### 테이블 레벨 락

```sql
-- 명시적 테이블 락
LOCK TABLE users IN ACCESS EXCLUSIVE MODE;

-- 락 모드
ACCESS SHARE          -- SELECT
ROW SHARE             -- SELECT FOR UPDATE/SHARE
ROW EXCLUSIVE         -- UPDATE, DELETE, INSERT
SHARE UPDATE EXCLUSIVE -- VACUUM, CREATE INDEX CONCURRENTLY
SHARE                 -- CREATE INDEX
SHARE ROW EXCLUSIVE   --
EXCLUSIVE             --
ACCESS EXCLUSIVE      -- ALTER TABLE, DROP TABLE, TRUNCATE
```

### 행 레벨 락

```sql
-- SELECT FOR UPDATE: 수정을 위해 행 잠금
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;

-- SELECT FOR SHARE: 읽기 공유, 수정 방지
SELECT * FROM accounts WHERE id = 1 FOR SHARE;

-- NOWAIT: 락을 얻지 못하면 즉시 오류
SELECT * FROM accounts WHERE id = 1 FOR UPDATE NOWAIT;

-- SKIP LOCKED: 잠긴 행은 건너뜀
SELECT * FROM jobs WHERE status = 'pending'
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

### 락 대기 시간 설정

```sql
-- 락 대기 시간 (밀리초)
SET lock_timeout = '5s';

-- 문 실행 시간 제한
SET statement_timeout = '30s';
```

---

## 데드락 (Deadlock)

### 데드락 발생 예시

```sql
-- 세션 A
BEGIN;
UPDATE accounts SET balance = 100 WHERE id = 1;

-- 세션 B
BEGIN;
UPDATE accounts SET balance = 100 WHERE id = 2;

-- 세션 A (대기)
UPDATE accounts SET balance = 200 WHERE id = 2;

-- 세션 B (데드락!)
UPDATE accounts SET balance = 200 WHERE id = 1;
-- ERROR: deadlock detected
```

### 데드락 방지 전략

1. ** 일관된 순서로 접근**
```sql
-- 항상 낮은 ID 먼저 잠금
BEGIN;
SELECT * FROM accounts WHERE id IN (1, 2) ORDER BY id FOR UPDATE;
UPDATE accounts SET balance = 100 WHERE id = 1;
UPDATE accounts SET balance = 200 WHERE id = 2;
COMMIT;
```

2. ** 락 타임아웃 설정**
```sql
SET deadlock_timeout = '1s';
```

---

## Advisory Locks

애플리케이션 레벨에서 사용하는 커스텀 락입니다.

```sql
-- 세션 레벨 락 (세션 종료까지 유지)
SELECT pg_advisory_lock(12345);
SELECT pg_advisory_unlock(12345);

-- 트랜잭션 레벨 락 (트랜잭션 종료 시 해제)
SELECT pg_advisory_xact_lock(12345);

-- 논블로킹 시도
SELECT pg_try_advisory_lock(12345);  -- true/false 반환

-- 키 2개 사용
SELECT pg_advisory_lock(1, 100);  -- (namespace, key)
```

** 사용 예시:**

```sql
-- 작업 큐에서 단일 작업자만 처리하도록
DO $$
DECLARE
    job_id INTEGER;
BEGIN
    -- 작업 ID로 락 획득 시도
    SELECT id INTO job_id FROM jobs WHERE status = 'pending' LIMIT 1;

    IF pg_try_advisory_xact_lock(job_id) THEN
        -- 작업 처리
        UPDATE jobs SET status = 'processing' WHERE id = job_id;
        -- ...
        UPDATE jobs SET status = 'completed' WHERE id = job_id;
    END IF;
END $$;
```

---

## 락 모니터링

### 현재 락 확인

```sql
SELECT
    pg_locks.pid,
    pg_class.relname,
    pg_locks.mode,
    pg_locks.granted,
    pg_stat_activity.query
FROM pg_locks
JOIN pg_class ON pg_locks.relation = pg_class.oid
JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid
WHERE pg_class.relname NOT LIKE 'pg_%';
```

### 블로킹 쿼리 찾기

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid
JOIN pg_locks blocking_locks ON blocked_locks.locktype = blocking_locks.locktype
    AND blocked_locks.relation = blocking_locks.relation
    AND blocked_locks.pid != blocking_locks.pid
JOIN pg_stat_activity blocking ON blocking_locks.pid = blocking.pid
WHERE NOT blocked_locks.granted;
```

### 장기 트랜잭션 확인

```sql
SELECT
    pid,
    now() - xact_start AS duration,
    query,
    state
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY duration DESC;
```

---

## 동시성 패턴

### 낙관적 잠금 (Optimistic Locking)

```sql
-- 버전 컬럼 사용
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    price NUMERIC,
    version INTEGER DEFAULT 1
);

-- 업데이트 시 버전 확인
UPDATE products
SET name = 'New Name', version = version + 1
WHERE id = 1 AND version = 5;

-- 영향받은 행이 0이면 충돌 발생
```

### 비관적 잠금 (Pessimistic Locking)

```sql
BEGIN;
SELECT * FROM products WHERE id = 1 FOR UPDATE;
-- 다른 트랜잭션은 이 행을 수정할 수 없음
UPDATE products SET price = 100 WHERE id = 1;
COMMIT;
```

### 작업 큐 패턴

```sql
-- 대기 중인 작업 하나 가져오기 (락 획득)
WITH next_job AS (
    SELECT id FROM jobs
    WHERE status = 'pending'
    ORDER BY created_at
    LIMIT 1
    FOR UPDATE SKIP LOCKED
)
UPDATE jobs
SET status = 'processing', started_at = NOW()
FROM next_job
WHERE jobs.id = next_job.id
RETURNING jobs.*;
```

---

## 트랜잭션 관련 설정

```sql
-- 기본 격리 수준
SET default_transaction_isolation = 'read committed';

-- 읽기 전용 트랜잭션
BEGIN READ ONLY;

-- 지연 가능 트랜잭션 (Serializable에서 직렬화 실패 방지)
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE READ ONLY DEFERRABLE;
```

---

## 참고 문서

- [동시성 제어](https://www.postgresql.org/docs/current/mvcc.html)
- [트랜잭션 격리](https://www.postgresql.org/docs/current/transaction-iso.html)
- [명시적 잠금](https://www.postgresql.org/docs/current/explicit-locking.html)
- [Advisory Locks](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS)
