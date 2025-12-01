# PostgreSQL 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://www.postgresql.org/docs/
> 최신 버전: PostgreSQL 18 (2025년 출시)

## 개요

PostgreSQL은 세계에서 가장 진보된 오픈 소스 관계형 데이터베이스입니다. 35년 이상의 활발한 개발로 신뢰성, 기능 견고성, 성능에서 강력한 명성을 얻었습니다.

---

## 문서 목차

### 기본

| 번호 | 문서 | 설명 |
|------|------|------|
| 01 | [개요](./01_overview.md) | PostgreSQL 소개, 특징, 버전 정보, 설치 |
| 02 | [데이터 타입](./02_data_types.md) | 숫자, 문자, 날짜/시간, JSON, 배열 등 |
| 03 | [테이블 및 스키마](./03_tables_schema.md) | 테이블 생성, 제약조건, 파티셔닝 |
| 04 | [SQL 기본 문법](./04_sql_basics.md) | SELECT, INSERT, UPDATE, DELETE, JOIN |

### 성능

| 번호 | 문서 | 설명 |
|------|------|------|
| 05 | [인덱스](./05_indexes.md) | B-tree, GIN, GiST, BRIN 등 인덱스 유형 |
| 06 | [트랜잭션](./06_transactions.md) | MVCC, 격리 수준, 락킹 |
| 07 | [쿼리 최적화](./07_performance.md) | EXPLAIN, 실행 계획, 튜닝 |

### 운영

| 번호 | 문서 | 설명 |
|------|------|------|
| 08 | [백업 및 복구](./08_backup_recovery.md) | pg_dump, WAL 아카이빙, PITR |
| 09 | [보안](./09_security.md) | 역할, 권한, 인증, RLS |
| 10 | [복제 및 고가용성](./10_replication.md) | 스트리밍 복제, 논리 복제, Failover |

### 개발

| 번호 | 문서 | 설명 |
|------|------|------|
| 11 | [함수 및 프로시저](./11_functions.md) | PL/pgSQL, 트리거, 사용자 정의 함수 |
| 12 | [JSON 기능](./12_json.md) | JSONB, JSONPath, 연산자 및 함수 |

---

## 버전 정보

| 버전 | 상태 | 지원 종료 |
|------|------|-----------|
| 18 | Current | 2030년 예정 |
| 17 | Supported | 2029년 예정 |
| 16 | Supported | 2028년 예정 |
| 15 | Supported | 2027년 예정 |
| 14 | Supported | 2026년 예정 |

> PostgreSQL은 메이저 버전 출시 후 5년간 지원합니다.

---

## PostgreSQL 18 주요 기능 (2025)

### 성능 향상

- ** 새로운 I/O 서브시스템**: 스토리지 읽기 성능 최대 3배 향상
- ** 인덱스 사용 쿼리 증가**: 더 많은 쿼리에서 인덱스 활용

### 새로운 기능

- **Virtual Generated Columns**: 읽기 시 값을 계산하는 가상 컬럼 (기본값)

```sql
CREATE TABLE products (
  price numeric,
  quantity integer,
  total numeric GENERATED ALWAYS AS (price * quantity) VIRTUAL
);
```

- **UUIDv7 함수**: 시간 기반 UUID로 인덱싱 및 읽기 성능 향상

```sql
SELECT uuidv7();
-- 018c1a2b-3c4d-7000-8000-000000000000
```

- **OAuth 인증 지원**
- **OLD/NEW in RETURNING 절**

```sql
UPDATE users SET name = 'New Name'
WHERE id = 1
RETURNING OLD.name AS old_name, NEW.name AS new_name;
```

---

## 빠른 참조

### 데이터 타입

```sql
-- 숫자
INTEGER, BIGINT, NUMERIC(precision, scale), REAL, DOUBLE PRECISION

-- 문자
VARCHAR(n), TEXT, CHAR(n)

-- 날짜/시간
DATE, TIME, TIMESTAMP, TIMESTAMPTZ, INTERVAL

-- 기타
BOOLEAN, JSON, JSONB, UUID, ARRAY
```

### 기본 명령어

```sql
-- 데이터베이스 생성
CREATE DATABASE mydb;

-- 테이블 생성
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX idx_users_email ON users(email);

-- 트랜잭션
BEGIN;
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

### psql 명령어

| 명령어 | 설명 |
|--------|------|
| `\l` | 데이터베이스 목록 |
| `\c dbname` | 데이터베이스 연결 |
| `\dt` | 테이블 목록 |
| `\d tablename` | 테이블 구조 |
| `\di` | 인덱스 목록 |
| `\du` | 사용자 목록 |
| `\df` | 함수 목록 |
| `\q` | 종료 |

---

## 참고 문서

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [PostgreSQL Wiki](https://wiki.postgresql.org/)
- [PostgreSQL 튜토리얼](https://www.postgresql.org/docs/current/tutorial.html)
- [Release Notes](https://www.postgresql.org/docs/current/release.html)
