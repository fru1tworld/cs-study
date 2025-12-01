# PostgreSQL 쿼리 최적화

> 공식 문서: https://www.postgresql.org/docs/current/performance-tips.html

---

## EXPLAIN 사용법

### 기본 사용

```sql
-- 실행 계획 확인
EXPLAIN SELECT * FROM users WHERE id = 1;

-- 실제 실행 + 통계
EXPLAIN ANALYZE SELECT * FROM users WHERE id = 1;

-- 버퍼 사용량 포함
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM users WHERE id = 1;

-- 상세 출력
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) SELECT * FROM users WHERE id = 1;

-- JSON 형식 출력
EXPLAIN (ANALYZE, FORMAT JSON) SELECT * FROM users WHERE id = 1;
```

### 출력 해석

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- 결과 예시:
-- Index Scan using idx_users_email on users  (cost=0.42..8.44 rows=1 width=100) (actual time=0.025..0.027 rows=1 loops=1)
--   Index Cond: ((email)::text = 'test@example.com'::text)
-- Planning Time: 0.150 ms
-- Execution Time: 0.050 ms
```

**주요 지표:**

| 항목 | 설명 |
|------|------|
| cost | 예상 비용 (시작..총) |
| rows | 예상 반환 행 수 |
| width | 행당 평균 바이트 |
| actual time | 실제 소요 시간 (ms) |
| rows | 실제 반환 행 수 |
| loops | 반복 횟수 |

---

## 스캔 타입

### Sequential Scan (Seq Scan)

테이블 전체를 순차적으로 읽습니다.

```sql
-- 인덱스가 없거나 대부분의 행을 반환할 때
EXPLAIN SELECT * FROM users;
-- Seq Scan on users  (cost=0.00..15.00 rows=500 width=100)
```

### Index Scan

인덱스를 사용하여 행을 찾고 테이블에서 데이터를 가져옵니다.

```sql
EXPLAIN SELECT * FROM users WHERE id = 1;
-- Index Scan using users_pkey on users  (cost=0.28..8.29 rows=1 width=100)
--   Index Cond: (id = 1)
```

### Index Only Scan

인덱스만으로 쿼리를 처리합니다 (커버링 인덱스).

```sql
-- 인덱스에 포함된 컬럼만 조회
CREATE INDEX idx_users_email ON users(email) INCLUDE (name);
EXPLAIN SELECT email, name FROM users WHERE email = 'test@example.com';
-- Index Only Scan using idx_users_email on users
```

### Bitmap Scan

여러 조건을 비트맵으로 결합하여 처리합니다.

```sql
EXPLAIN SELECT * FROM users WHERE age > 30 AND status = 'active';
-- Bitmap Heap Scan on users
--   Recheck Cond: ((age > 30) AND (status = 'active'))
--   -> BitmapAnd
--        -> Bitmap Index Scan on idx_users_age
--        -> Bitmap Index Scan on idx_users_status
```

---

## 조인 방법

### Nested Loop

작은 테이블을 반복하며 조인합니다.

```sql
-- 한쪽 테이블이 작을 때 효율적
EXPLAIN SELECT * FROM users u JOIN orders o ON u.id = o.user_id WHERE u.id = 1;
-- Nested Loop
--   -> Index Scan using users_pkey on users u
--   -> Index Scan using idx_orders_user_id on orders o
```

### Hash Join

해시 테이블을 만들어 조인합니다.

```sql
-- 중간 크기 테이블 조인에 효율적
EXPLAIN SELECT * FROM users u JOIN orders o ON u.id = o.user_id;
-- Hash Join
--   Hash Cond: (o.user_id = u.id)
--   -> Seq Scan on orders o
--   -> Hash
--        -> Seq Scan on users u
```

### Merge Join

정렬된 데이터를 병합합니다.

```sql
-- 이미 정렬되어 있거나 큰 테이블에 효율적
EXPLAIN SELECT * FROM users u JOIN orders o ON u.id = o.user_id ORDER BY u.id;
-- Merge Join
--   Merge Cond: (u.id = o.user_id)
--   -> Index Scan using users_pkey on users u
--   -> Index Scan using idx_orders_user_id on orders o
```

---

## 통계 정보

### ANALYZE

테이블 통계를 수집합니다.

```sql
-- 특정 테이블 분석
ANALYZE users;

-- 특정 컬럼만 분석
ANALYZE users(email, created_at);

-- 모든 테이블 분석
ANALYZE;
```

### 통계 확인

```sql
-- 테이블 통계
SELECT
    relname,
    reltuples AS estimated_rows,
    relpages AS pages
FROM pg_class
WHERE relname = 'users';

-- 컬럼 통계
SELECT
    attname,
    n_distinct,
    most_common_vals,
    most_common_freqs,
    correlation
FROM pg_stats
WHERE tablename = 'users';
```

### 통계 정밀도 조정

```sql
-- 기본값: 100
ALTER TABLE users ALTER COLUMN email SET STATISTICS 500;
ANALYZE users;
```

---

## 플래너 힌트

PostgreSQL은 직접적인 힌트를 지원하지 않지만, 플래너 설정을 조정할 수 있습니다.

```sql
-- 특정 플랜 방법 비활성화 (디버깅용)
SET enable_seqscan = off;
SET enable_indexscan = off;
SET enable_hashjoin = off;
SET enable_mergejoin = off;
SET enable_nestloop = off;

-- 비용 매개변수 조정
SET random_page_cost = 1.1;  -- SSD인 경우 낮춤
SET seq_page_cost = 1.0;
SET cpu_tuple_cost = 0.01;
SET cpu_index_tuple_cost = 0.005;
```

---

## 쿼리 최적화 팁

### 1. 적절한 인덱스 사용

```sql
-- WHERE 절에 사용되는 컬럼
CREATE INDEX idx_users_status ON users(status);

-- 복합 조건
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at);

-- 정렬과 함께
CREATE INDEX idx_users_created ON users(created_at DESC);
```

### 2. 선택적 조건 먼저

```sql
-- 좋음: 선택도 높은 조건이 앞에
SELECT * FROM orders
WHERE user_id = 1 AND status = 'pending';

-- 인덱스: (user_id, status) 순서가 효율적
```

### 3. LIMIT 활용

```sql
-- 필요한 행만 가져오기
SELECT * FROM logs
WHERE level = 'ERROR'
ORDER BY created_at DESC
LIMIT 100;
```

### 4. EXISTS vs IN

```sql
-- 큰 서브쿼리에서는 EXISTS가 효율적
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- 작은 목록에서는 IN이 효율적
SELECT * FROM users WHERE id IN (1, 2, 3, 4, 5);
```

### 5. 불필요한 컬럼 제외

```sql
-- 나쁨
SELECT * FROM users;

-- 좋음
SELECT id, name, email FROM users;
```

### 6. 함수 사용 주의

```sql
-- 나쁨: 인덱스 사용 불가
SELECT * FROM users WHERE LOWER(email) = 'test@example.com';

-- 좋음: 표현식 인덱스 또는 저장 시 소문자로
CREATE INDEX idx_users_lower_email ON users(LOWER(email));
```

---

## VACUUM과 성능

### VACUUM

죽은 튜플을 정리하고 공간을 재사용 가능하게 합니다.

```sql
-- 기본 VACUUM
VACUUM users;

-- VACUUM + ANALYZE
VACUUM ANALYZE users;

-- FULL VACUUM (테이블 잠금, 공간 반환)
VACUUM FULL users;

-- 상세 출력
VACUUM VERBOSE users;
```

### Auto-VACUUM

자동으로 VACUUM을 실행합니다.

```sql
-- 설정 확인
SHOW autovacuum;
SHOW autovacuum_vacuum_threshold;
SHOW autovacuum_vacuum_scale_factor;

-- 테이블별 설정
ALTER TABLE users SET (
    autovacuum_vacuum_threshold = 50,
    autovacuum_vacuum_scale_factor = 0.1
);
```

### VACUUM 모니터링

```sql
-- 마지막 VACUUM 시간
SELECT
    relname,
    last_vacuum,
    last_autovacuum,
    n_dead_tup,
    n_live_tup
FROM pg_stat_user_tables;
```

---

## 병렬 쿼리

PostgreSQL은 대용량 쿼리를 병렬로 처리할 수 있습니다.

### 병렬 처리 설정

```sql
-- 병렬 워커 수
SET max_parallel_workers_per_gather = 4;

-- 병렬 처리 임계값
SET min_parallel_table_scan_size = '8MB';
SET min_parallel_index_scan_size = '512kB';

-- 테이블별 설정
ALTER TABLE large_table SET (parallel_workers = 4);
```

### 병렬 쿼리 확인

```sql
EXPLAIN ANALYZE SELECT COUNT(*) FROM large_table;
-- Finalize Aggregate
--   -> Gather
--        Workers Planned: 2
--        Workers Launched: 2
--        -> Partial Aggregate
--             -> Parallel Seq Scan on large_table
```

---

## 쿼리 캐싱

### Prepared Statements

```sql
-- 준비된 문장 생성
PREPARE user_by_id (int) AS
SELECT * FROM users WHERE id = $1;

-- 실행
EXECUTE user_by_id(1);

-- 삭제
DEALLOCATE user_by_id;
```

### 공유 버퍼

```sql
-- 버퍼 사용 확인
SELECT
    c.relname,
    count(*) AS buffers
FROM pg_buffercache b
JOIN pg_class c ON b.relfilenode = c.relfilenode
GROUP BY c.relname
ORDER BY buffers DESC
LIMIT 10;
```

---

## 성능 모니터링

### pg_stat_statements

쿼리 통계를 수집합니다.

```sql
-- 확장 설치
CREATE EXTENSION pg_stat_statements;

-- 느린 쿼리 확인
SELECT
    query,
    calls,
    total_exec_time / 1000 AS total_seconds,
    mean_exec_time AS avg_ms,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;

-- 통계 초기화
SELECT pg_stat_statements_reset();
```

### 활성 쿼리 모니터링

```sql
SELECT
    pid,
    now() - query_start AS duration,
    state,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

### 테이블 I/O 통계

```sql
SELECT
    relname,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del
FROM pg_stat_user_tables
ORDER BY seq_tup_read DESC;
```

---

## 설정 튜닝

### 메모리 설정

```sql
-- 공유 버퍼 (RAM의 25%)
shared_buffers = '4GB'

-- 작업 메모리 (정렬, 해시 조인용)
work_mem = '256MB'

-- 유지보수 작업 메모리
maintenance_work_mem = '1GB'

-- 유효 캐시 크기 (RAM의 50-75%)
effective_cache_size = '12GB'
```

### I/O 설정

```sql
-- SSD인 경우
random_page_cost = 1.1

-- 체크포인트 설정
checkpoint_completion_target = 0.9
wal_buffers = '64MB'
```

### 연결 설정

```sql
-- 최대 연결 수
max_connections = 200
```

---

## 참고 문서

- [성능 팁](https://www.postgresql.org/docs/current/performance-tips.html)
- [EXPLAIN 사용](https://www.postgresql.org/docs/current/using-explain.html)
- [플래너 통계](https://www.postgresql.org/docs/current/planner-stats.html)
- [병렬 쿼리](https://www.postgresql.org/docs/current/parallel-query.html)
