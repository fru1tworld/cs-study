# PostgreSQL 인덱스

> 공식 문서: https://www.postgresql.org/docs/current/indexes.html

인덱스는 데이터베이스 성능을 향상시키는 핵심 도구입니다. 특정 행을 빠르게 찾을 수 있게 해주지만, 오버헤드도 추가하므로 현명하게 사용해야 합니다.

---

## 인덱스 기본

### 인덱스 생성

```sql
-- 기본 인덱스 생성
CREATE INDEX idx_users_email ON users(email);

-- 고유 인덱스
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- 테이블 생성 시 UNIQUE 제약조건은 자동으로 인덱스 생성
CREATE TABLE users (
    email VARCHAR(255) UNIQUE  -- 자동 인덱스 생성
);
```

### 인덱스 삭제

```sql
DROP INDEX idx_users_email;
DROP INDEX IF EXISTS idx_users_email;

-- 동시 접근 허용하며 삭제
DROP INDEX CONCURRENTLY idx_users_email;
```

### 인덱스 확인

```sql
-- psql 명령어
\di                    -- 인덱스 목록
\di+ idx_users_email   -- 상세 정보

-- SQL로 조회
SELECT * FROM pg_indexes WHERE tablename = 'users';
```

---

## 인덱스 타입

### 1. B-Tree (기본값)

가장 일반적인 인덱스 타입입니다. 정렬된 데이터에서 동등 비교와 범위 검색에 적합합니다.

```sql
-- 기본적으로 B-tree 인덱스 생성
CREATE INDEX idx_users_created ON users(created_at);

-- 명시적 지정
CREATE INDEX idx_users_created ON users USING BTREE(created_at);
```

**지원 연산자:**
- `<`, `<=`, `=`, `>=`, `>`
- `BETWEEN`, `IN`
- `IS NULL`, `IS NOT NULL`
- 패턴 매칭: `LIKE 'abc%'` (접두사 검색만)

```sql
-- B-tree 활용 예시
SELECT * FROM users WHERE created_at > '2024-01-01';
SELECT * FROM users WHERE email = 'test@example.com';
SELECT * FROM users WHERE name LIKE 'John%';  -- 인덱스 사용
SELECT * FROM users WHERE name LIKE '%John';  -- 인덱스 미사용!
```

### 2. Hash

동등 비교(`=`)만 지원하며, B-tree보다 빠를 수 있습니다.

```sql
CREATE INDEX idx_users_email_hash ON users USING HASH(email);
```

**사용 시기:**
- 동등 비교만 필요한 경우
- 범위 검색이 필요 없는 경우

```sql
-- Hash 인덱스 활용
SELECT * FROM users WHERE email = 'test@example.com';  -- 인덱스 사용
SELECT * FROM users WHERE email > 'a';  -- 인덱스 미사용!
```

### 3. GiST (Generalized Search Tree)

다양한 데이터 타입과 검색 전략을 지원하는 범용 인덱스입니다.

```sql
-- 기하학적 데이터
CREATE INDEX idx_locations_pos ON locations USING GIST(position);

-- 범위 타입
CREATE INDEX idx_reservations_period ON reservations USING GIST(period);

-- 전문 검색
CREATE INDEX idx_articles_content ON articles USING GIST(to_tsvector('english', content));
```

**사용 시기:**
- 기하학적 데이터 (point, polygon 등)
- 범위 타입 (tsrange, daterange 등)
- 전문 검색 (tsvector)
- 근접 이웃 검색 (KNN)

```sql
-- 근접 이웃 검색 (KNN)
SELECT * FROM locations
ORDER BY position <-> '(126.978, 37.566)'
LIMIT 10;
```

### 4. SP-GiST (Space-Partitioned GiST)

비균형 데이터 구조를 위한 인덱스입니다.

```sql
CREATE INDEX idx_data ON table_name USING SPGIST(column);
```

**사용 시기:**
- quadtree, k-d tree, radix tree가 효율적인 데이터
- 전화번호, IP 주소 등 계층적 데이터

### 5. GIN (Generalized Inverted Index)

역인덱스로, 하나의 값에 여러 키가 포함된 경우에 적합합니다.

```sql
-- 배열 인덱싱
CREATE INDEX idx_articles_tags ON articles USING GIN(tags);

-- JSONB 인덱싱
CREATE INDEX idx_products_data ON products USING GIN(data);

-- 전문 검색
CREATE INDEX idx_articles_search ON articles
USING GIN(to_tsvector('english', title || ' ' || content));
```

**사용 시기:**
- 배열 컬럼
- JSONB 데이터
- 전문 검색 (tsvector)

```sql
-- GIN 인덱스 활용
SELECT * FROM articles WHERE tags @> ARRAY['database'];
SELECT * FROM products WHERE data @> '{"category": "electronics"}';
SELECT * FROM articles WHERE to_tsvector('english', content) @@ to_tsquery('postgresql');
```

### 6. BRIN (Block Range Index)

물리적으로 연속된 데이터에 대한 요약 정보를 저장합니다.

```sql
CREATE INDEX idx_logs_created ON logs USING BRIN(created_at);
```

**특징:**
- 매우 작은 인덱스 크기
- 시계열 데이터에 최적
- 물리적 순서와 값의 상관관계가 높은 경우 효과적

**사용 시기:**
- 시간순 로그 데이터
- 순차적으로 증가하는 ID
- 대용량 테이블에서 저장 공간 절약이 필요한 경우

```sql
-- 시계열 데이터에 효과적
SELECT * FROM logs
WHERE created_at BETWEEN '2024-01-01' AND '2024-01-31';
```

---

## 인덱스 타입 비교

| 타입 | 용도 | 연산자 | 크기 |
|------|------|--------|------|
| B-tree | 범용, 기본값 | `<`, `<=`, `=`, `>=`, `>` | 중간 |
| Hash | 동등 비교 | `=` | 작음 |
| GiST | 기하학, 범위, FTS | 다양함 | 중간 |
| SP-GiST | 계층적 데이터 | 다양함 | 작음 |
| GIN | 배열, JSONB, FTS | `@>`, `?`, `@@` | 큼 |
| BRIN | 시계열 데이터 | `<`, `<=`, `=`, `>=`, `>` | 매우 작음 |

---

## 다중 컬럼 인덱스

```sql
CREATE INDEX idx_users_name_email ON users(last_name, first_name);
```

**컬럼 순서가 중요:**

```sql
-- 이 인덱스 사용 가능
SELECT * FROM users WHERE last_name = 'Kim';
SELECT * FROM users WHERE last_name = 'Kim' AND first_name = 'John';

-- 이 인덱스 사용 불가 (첫 번째 컬럼 조건 없음)
SELECT * FROM users WHERE first_name = 'John';
```

**순서 결정 기준:**
1. 자주 검색되는 컬럼을 앞에
2. 선택도(cardinality)가 높은 컬럼을 앞에
3. 범위 조건 컬럼은 뒤에

---

## 부분 인덱스 (Partial Index)

테이블의 일부 행만 인덱싱합니다.

```sql
-- 활성 사용자만 인덱싱
CREATE INDEX idx_active_users ON users(email)
WHERE is_active = TRUE;

-- NULL이 아닌 값만 인덱싱
CREATE INDEX idx_users_phone ON users(phone)
WHERE phone IS NOT NULL;

-- 특정 상태만 인덱싱
CREATE INDEX idx_pending_orders ON orders(created_at)
WHERE status = 'pending';
```

**장점:**
- 인덱스 크기 감소
- 인덱스 유지 비용 감소
- 자주 조회되는 데이터에 집중

---

## 표현식 인덱스

계산된 값에 대한 인덱스입니다.

```sql
-- 소문자 변환된 값 인덱싱
CREATE INDEX idx_users_lower_email ON users(LOWER(email));

-- 날짜 추출 값 인덱싱
CREATE INDEX idx_orders_year ON orders(EXTRACT(YEAR FROM created_at));

-- JSON 필드 인덱싱
CREATE INDEX idx_products_category ON products((data->>'category'));
```

**사용 예시:**

```sql
-- 표현식 인덱스 활용
SELECT * FROM users WHERE LOWER(email) = 'test@example.com';
SELECT * FROM orders WHERE EXTRACT(YEAR FROM created_at) = 2024;
```

---

## 커버링 인덱스 (Covering Index)

쿼리에 필요한 모든 컬럼을 인덱스에 포함합니다.

```sql
-- INCLUDE 절 사용 (PostgreSQL 11+)
CREATE INDEX idx_users_email_include ON users(email) INCLUDE (name, created_at);
```

**Index-Only Scan:**

```sql
-- 인덱스만으로 쿼리 처리 가능
SELECT email, name, created_at FROM users WHERE email = 'test@example.com';
```

---

## 고유 인덱스와 제약조건

```sql
-- UNIQUE INDEX
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- 부분 고유 인덱스 (조건부 유니크)
CREATE UNIQUE INDEX idx_active_users_email ON users(email)
WHERE is_deleted = FALSE;

-- 복합 고유 인덱스
CREATE UNIQUE INDEX idx_user_roles ON user_roles(user_id, role_id);
```

---

## 동시성을 고려한 인덱스 생성

```sql
-- CONCURRENTLY: 테이블 락 없이 인덱스 생성
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);

-- 주의사항:
-- - 더 오래 걸림
-- - 트랜잭션 내에서 사용 불가
-- - 실패 시 INVALID 상태의 인덱스가 남을 수 있음
```

**유효하지 않은 인덱스 확인:**

```sql
SELECT indexrelid::regclass, indisvalid
FROM pg_index
WHERE NOT indisvalid;

-- 무효 인덱스 재생성
REINDEX INDEX CONCURRENTLY idx_name;
```

---

## 인덱스 정렬 옵션

```sql
-- 정렬 순서 지정
CREATE INDEX idx_users_created ON users(created_at DESC);

-- NULL 위치 지정
CREATE INDEX idx_users_score ON users(score DESC NULLS LAST);

-- 다중 컬럼 개별 정렬
CREATE INDEX idx_products ON products(category ASC, price DESC);
```

---

## 인덱스 성능 모니터링

### 인덱스 사용 통계

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan AS times_used,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### 미사용 인덱스 찾기

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexrelname NOT LIKE '%_pkey';
```

### 인덱스 크기 확인

```sql
SELECT
    indexrelname AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE relname = 'users'
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## 인덱스 유지보수

### REINDEX

```sql
-- 인덱스 재생성
REINDEX INDEX idx_users_email;

-- 테이블의 모든 인덱스 재생성
REINDEX TABLE users;

-- 데이터베이스의 모든 인덱스 재생성
REINDEX DATABASE mydb;

-- 동시성 유지하며 재생성
REINDEX INDEX CONCURRENTLY idx_users_email;
```

### 인덱스 팽창 (Bloat) 확인

```sql
-- pgstattuple 확장 사용
CREATE EXTENSION pgstattuple;

SELECT * FROM pgstatindex('idx_users_email');
```

---

## 인덱스 사용 팁

### 인덱스가 효과적인 경우

1. WHERE 절에서 자주 사용되는 컬럼
2. JOIN 조건에 사용되는 컬럼
3. ORDER BY에 사용되는 컬럼
4. 선택도(selectivity)가 높은 컬럼

### 인덱스가 비효과적인 경우

1. 테이블 크기가 작은 경우
2. 대부분의 행을 반환하는 쿼리
3. 자주 UPDATE/DELETE되는 컬럼
4. NULL이 많은 컬럼 (부분 인덱스 고려)

### 모범 사례

```sql
-- 1. 복합 인덱스 vs 개별 인덱스
-- 자주 함께 사용되는 조건이면 복합 인덱스
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at);

-- 2. 선택도 확인
SELECT
    COUNT(DISTINCT status) AS unique_values,
    COUNT(*) AS total_rows,
    COUNT(DISTINCT status)::float / COUNT(*) AS selectivity
FROM orders;

-- 3. 쿼리 패턴 분석 후 인덱스 설계
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

---

## 참고 문서

- [인덱스](https://www.postgresql.org/docs/current/indexes.html)
- [인덱스 타입](https://www.postgresql.org/docs/current/indexes-types.html)
- [인덱스와 ORDER BY](https://www.postgresql.org/docs/current/indexes-ordering.html)
- [부분 인덱스](https://www.postgresql.org/docs/current/indexes-partial.html)
