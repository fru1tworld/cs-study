# PostgreSQL 개요

> 공식 문서: https://www.postgresql.org/docs/current/intro-whatis.html

## PostgreSQL이란?

PostgreSQL은 ** 객체-관계형 데이터베이스 관리 시스템(ORDBMS)** 으로, UC Berkeley의 POSTGRES 프로젝트를 기반으로 개발되었습니다. 35년 이상의 활발한 개발 역사를 가진 세계에서 가장 진보된 오픈 소스 관계형 데이터베이스입니다.

---

## 핵심 특징

### 1. SQL 표준 준수

PostgreSQL은 SQL 표준을 가장 충실히 준수하는 데이터베이스 중 하나입니다.

```sql
-- SQL 표준 기능 지원 예시
SELECT * FROM users
WHERE created_at BETWEEN '2024-01-01' AND '2024-12-31'
ORDER BY name
FETCH FIRST 10 ROWS ONLY;  -- SQL 표준 LIMIT
```

### 2. ACID 트랜잭션

모든 트랜잭션에서 ACID 속성을 완벽하게 보장합니다.

| 속성 | 설명 |
|------|------|
| **A**tomicity | 트랜잭션은 전체가 성공하거나 전체가 실패 |
| **C**onsistency | 트랜잭션 전후 데이터 일관성 유지 |
| **I**solation | 동시 트랜잭션 간 간섭 방지 |
| **D**urability | 커밋된 트랜잭션은 영구 보존 |

### 3. MVCC (다중 버전 동시성 제어)

읽기 작업이 쓰기 작업을 차단하지 않고, 쓰기 작업이 읽기 작업을 차단하지 않습니다.

```sql
-- 세션 A: 데이터 수정 중
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- 아직 COMMIT하지 않음

-- 세션 B: 동시에 데이터 읽기 가능 (차단되지 않음)
SELECT * FROM accounts WHERE id = 1;
-- 수정 전 데이터를 읽음
```

### 4. 확장성

사용자 정의 확장이 가능합니다:

- ** 데이터 타입**: 커스텀 타입 정의
- ** 함수 및 연산자**: 새로운 함수/연산자 추가
- ** 집계 함수**: 사용자 정의 집계 함수
- ** 인덱스 방법**: 새로운 인덱싱 전략
- ** 절차형 언어**: PL/pgSQL, PL/Python, PL/Perl 등

---

## 주요 기능

### 데이터 무결성

```sql
-- 외래키 제약조건
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
);
```

### 고급 쿼리 기능

```sql
-- CTE (Common Table Expressions)
WITH monthly_sales AS (
    SELECT
        DATE_TRUNC('month', order_date) AS month,
        SUM(amount) AS total
    FROM orders
    GROUP BY DATE_TRUNC('month', order_date)
)
SELECT * FROM monthly_sales WHERE total > 10000;

-- Window Functions
SELECT
    name,
    department,
    salary,
    RANK() OVER (PARTITION BY department ORDER BY salary DESC) as rank
FROM employees;
```

### JSON 지원

```sql
-- JSONB 데이터 저장 및 쿼리
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    data JSONB
);

-- JSON 경로 쿼리
SELECT * FROM products
WHERE data @> '{"category": "electronics"}';
```

### 전문 검색 (Full-Text Search)

```sql
-- 전문 검색 인덱스
CREATE INDEX idx_articles_search
ON articles USING GIN (to_tsvector('english', content));

-- 검색 쿼리
SELECT * FROM articles
WHERE to_tsvector('english', content) @@ to_tsquery('postgresql & database');
```

---

## 버전 정보

| 버전 | 상태 | 출시일 | 지원 종료 |
|------|------|--------|-----------|
| 18 | Current | 2025 | 2030년 예정 |
| 17 | Supported | 2024 | 2029년 예정 |
| 16 | Supported | 2023 | 2028년 예정 |
| 15 | Supported | 2022 | 2027년 예정 |
| 14 | Supported | 2021 | 2026년 예정 |

> PostgreSQL은 메이저 버전 출시 후 **5년간** 지원됩니다.

---

## PostgreSQL 18 주요 기능 (2025)

### 1. 새로운 I/O 서브시스템

스토리지 읽기 성능이 최대 **3배** 향상되었습니다.

### 2. Virtual Generated Columns

```sql
CREATE TABLE products (
    price NUMERIC,
    quantity INTEGER,
    -- 읽기 시 계산되는 가상 컬럼 (저장 공간 불필요)
    total NUMERIC GENERATED ALWAYS AS (price * quantity) VIRTUAL
);
```

### 3. UUIDv7 지원

시간 기반 UUID로 인덱싱 및 읽기 성능이 향상됩니다.

```sql
SELECT uuidv7();
-- 018c1a2b-3c4d-7000-8000-000000000000
```

### 4. OLD/NEW in RETURNING 절

```sql
UPDATE users SET name = 'New Name'
WHERE id = 1
RETURNING OLD.name AS old_name, NEW.name AS new_name;
```

### 5. OAuth 인증 지원

외부 OAuth 제공자를 통한 인증이 가능해졌습니다.

---

## 라이선스

PostgreSQL은 **PostgreSQL License** (BSD/MIT 계열)를 사용합니다.

- 개인, 상업, 학술 목적으로 무료 사용 가능
- 소스코드 수정 및 재배포 가능
- 특허권 무료 사용

---

## 설치 방법

### macOS (Homebrew)

```bash
brew install postgresql@17
brew services start postgresql@17
```

### Ubuntu/Debian

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Docker

```bash
docker run --name postgres \
    -e POSTGRES_PASSWORD=mypassword \
    -p 5432:5432 \
    -d postgres:17
```

---

## 기본 접속

```bash
# 로컬 접속
psql -U postgres

# 원격 접속
psql -h hostname -p 5432 -U username -d dbname
```

### psql 기본 명령어

| 명령어 | 설명 |
|--------|------|
| `\l` | 데이터베이스 목록 |
| `\c dbname` | 데이터베이스 연결 |
| `\dt` | 테이블 목록 |
| `\d tablename` | 테이블 구조 |
| `\du` | 사용자 목록 |
| `\q` | 종료 |

---

## 참고 문서

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [PostgreSQL Wiki](https://wiki.postgresql.org/)
- [PostgreSQL 튜토리얼](https://www.postgresql.org/docs/current/tutorial.html)
