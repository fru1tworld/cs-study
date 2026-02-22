# 부록 F: 추가 제공 모듈 (Additional Supplied Modules)

이 문서는 PostgreSQL 배포판의 `contrib` 디렉토리에서 제공되는 선택적 구성 요소들을 설명합니다. 이들은 핵심 PostgreSQL 시스템의 일부가 아닌 확장 기능(Extension) 및 유틸리티입니다.

## 목차

1. [개요](#1-개요)
2. [설치 및 설정](#2-설치-및-설정)
3. [주요 모듈 목록](#3-주요-모듈-목록)
4. [pg_stat_statements](#4-pg_stat_statements)
5. [postgres_fdw](#5-postgres_fdw)
6. [pg_trgm](#6-pg_trgm)
7. [hstore](#7-hstore)
8. [ltree](#8-ltree)
9. [기타 유용한 모듈](#9-기타-유용한-모듈)

---

## 1. 개요

PostgreSQL은 핵심 데이터베이스 기능 외에도 다양한 추가 모듈을 제공합니다. 이러한 모듈들은 `contrib` 디렉토리에 위치하며, 필요에 따라 선택적으로 설치할 수 있습니다.

### 1.1 특징

- 선택적 설치: 필요한 모듈만 설치하여 시스템 오버헤드 최소화
- 신뢰된 확장(Trusted Extensions): 일부 모듈은 슈퍼유저가 아닌 일반 사용자도 설치 가능
- 표준 SQL 인터페이스: `CREATE EXTENSION` 명령으로 간편하게 설치

### 1.2 신뢰된 확장 목록

다음 확장들은 데이터베이스에 대한 `CREATE` 권한이 있는 모든 사용자가 설치할 수 있습니다:

| 확장명 | 설명 |
|--------|------|
| `btree_gin` | B-tree 동작을 하는 GIN 연산자 클래스 |
| `btree_gist` | B-tree 동작을 하는 GiST 연산자 클래스 |
| `citext` | 대소문자 구분 없는 문자열 타입 |
| `cube` | 다차원 큐브 데이터 타입 |
| `fuzzystrmatch` | 문자열 유사도 및 거리 측정 |
| `hstore` | 키/값 데이터 타입 |
| `intarray` | 정수 배열 조작 |
| `ltree` | 계층적 트리형 데이터 타입 |
| `pg_trgm` | 삼중문자(Trigram) 기반 유사도 검색 |
| `pgcrypto` | 암호화 함수 |
| `tablefunc` | crosstab 등 테이블 반환 함수 |
| `uuid-ossp` | UUID 생성기 |

---

## 2. 설치 및 설정

### 2.1 소스에서 빌드하기

```bash
# contrib 디렉토리에서 모든 모듈 빌드 및 설치
cd contrib
make
make install

# 특정 모듈만 빌드
cd contrib/module_name
make
make install

# 회귀 테스트 실행
make check          # 설치 전
make installcheck   # 설치 후
```

### 2.2 패키지 관리자를 통한 설치

대부분의 Linux 배포판에서는 `postgresql-contrib` 패키지로 제공됩니다:

```bash
# Debian/Ubuntu
sudo apt-get install postgresql-contrib

# RHEL/CentOS
sudo yum install postgresql-contrib

# macOS (Homebrew)
brew install postgresql  # contrib 모듈 포함
```

### 2.3 확장 등록

설치 후 각 데이터베이스에서 확장을 등록해야 합니다:

```sql
-- 기본 설치
CREATE EXTENSION extension_name;

-- 특정 스키마에 설치
CREATE EXTENSION extension_name SCHEMA schema_name;

-- 버전 지정 설치
CREATE EXTENSION extension_name VERSION '1.0';

-- 확장 업그레이드
ALTER EXTENSION extension_name UPDATE TO '2.0';

-- 확장 제거
DROP EXTENSION extension_name;

-- 설치된 확장 확인
SELECT * FROM pg_extension;
```

---

## 3. 주요 모듈 목록

### 3.1 성능 모니터링

| 모듈 | 설명 |
|------|------|
| `pg_stat_statements` | SQL 문의 계획 및 실행 통계 추적 |
| `pg_buffercache` | 공유 버퍼 캐시 상태 검사 |
| `pg_freespacemap` | Free Space Map 검사 |
| `pgrowlocks` | 테이블의 행 잠금 정보 표시 |
| `pgstattuple` | 튜플 수준 통계 획득 |
| `auto_explain` | 느린 쿼리의 실행 계획 자동 로깅 |

### 3.2 데이터 타입

| 모듈 | 설명 |
|------|------|
| `hstore` | 키/값 쌍 데이터 타입 |
| `ltree` | 계층적 트리 구조 데이터 타입 |
| `citext` | 대소문자 구분 없는 문자열 타입 |
| `cube` | 다차원 큐브 데이터 타입 |
| `seg` | 선분 또는 부동소수점 구간 타입 |
| `isn` | ISBN, EAN, UPC 등 국제 표준 번호 |
| `uuid-ossp` | UUID 생성기 |

### 3.3 인덱스 및 검색

| 모듈 | 설명 |
|------|------|
| `pg_trgm` | 삼중문자 기반 유사도 검색 |
| `bloom` | 블룸 필터 인덱스 |
| `btree_gin` | GIN용 B-tree 연산자 클래스 |
| `btree_gist` | GiST용 B-tree 연산자 클래스 |
| `fuzzystrmatch` | 문자열 유사도 함수 |
| `unaccent` | 발음 기호 제거 |

### 3.4 외부 데이터 연결

| 모듈 | 설명 |
|------|------|
| `postgres_fdw` | 외부 PostgreSQL 서버 연결 |
| `file_fdw` | 서버 파일 시스템 데이터 접근 |
| `dblink` | 원격 PostgreSQL 데이터베이스 연결 |

### 3.5 보안

| 모듈 | 설명 |
|------|------|
| `pgcrypto` | 암호화 함수 |
| `passwordcheck` | 패스워드 강도 검증 |
| `sslinfo` | 클라이언트 SSL 정보 획득 |

---

## 4. pg_stat_statements

`pg_stat_statements`는 PostgreSQL에서 실행되는 모든 SQL 문의 계획(Planning) 및 실행(Execution) 통계를 추적하는 모듈입니다. 성능 분석 및 쿼리 최적화에 필수적인 도구입니다.

### 4.1 설치 및 활성화

#### postgresql.conf 설정

```ini
# 서버 시작 시 모듈 로드 (서버 재시작 필요)
shared_preload_libraries = 'pg_stat_statements'

# 쿼리 ID 계산 활성화
compute_query_id = on

# 추적할 최대 문장 수 (기본값: 5000)
pg_stat_statements.max = 10000

# 추적 대상 설정
# top: 최상위 문장만 (기본값)
# all: 중첩 문장 포함
# none: 비활성화
pg_stat_statements.track = all

# UTILITY 명령어 추적 여부 (기본값: on)
pg_stat_statements.track_utility = on

# 계획 통계 추적 (성능 영향 있음, 기본값: off)
pg_stat_statements.track_planning = off

# 서버 종료 시 통계 저장 여부 (기본값: on)
pg_stat_statements.save = on
```

#### 확장 생성

```sql
-- 각 데이터베이스에서 실행
CREATE EXTENSION pg_stat_statements;
```

### 4.2 pg_stat_statements 뷰

#### 주요 컬럼

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `userid` | oid | 문장을 실행한 사용자의 OID |
| `dbid` | oid | 문장이 실행된 데이터베이스의 OID |
| `queryid` | bigint | 정규화된 쿼리의 해시 코드 |
| `query` | text | 대표 문장 텍스트 |
| `calls` | bigint | 실행 횟수 |
| `total_exec_time` | double precision | 총 실행 시간 (밀리초) |
| `min_exec_time` | double precision | 최소 실행 시간 |
| `max_exec_time` | double precision | 최대 실행 시간 |
| `mean_exec_time` | double precision | 평균 실행 시간 |
| `stddev_exec_time` | double precision | 실행 시간 표준 편차 |
| `rows` | bigint | 검색/영향받은 총 행 수 |

#### 블록 I/O 통계

| 컬럼 | 설명 |
|------|------|
| `shared_blks_hit` | 공유 블록 캐시 히트 수 |
| `shared_blks_read` | 디스크에서 읽은 공유 블록 수 |
| `shared_blks_dirtied` | 더티된 공유 블록 수 |
| `shared_blks_written` | 기록된 공유 블록 수 |
| `temp_blks_read` | 읽은 임시 블록 수 |
| `temp_blks_written` | 기록된 임시 블록 수 |

#### WAL 통계

| 컬럼 | 설명 |
|------|------|
| `wal_records` | 생성된 WAL 레코드 수 |
| `wal_fpi` | WAL Full Page Image 수 |
| `wal_bytes` | 생성된 WAL 바이트 수 |

### 4.3 실용 예제

#### 가장 오래 걸리는 쿼리 찾기

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

#### 가장 자주 호출되는 쿼리

```sql
SELECT
    query,
    calls,
    mean_exec_time,
    total_exec_time
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 10;
```

#### 캐시 히트율이 낮은 쿼리 (디스크 I/O가 많은 쿼리)

```sql
SELECT
    query,
    calls,
    shared_blks_hit,
    shared_blks_read,
    CASE
        WHEN shared_blks_hit + shared_blks_read = 0 THEN 0
        ELSE round(100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read), 2)
    END AS hit_ratio
FROM pg_stat_statements
WHERE shared_blks_hit + shared_blks_read > 0
ORDER BY hit_ratio ASC
LIMIT 10;
```

#### 평균 행 수가 많은 쿼리

```sql
SELECT
    query,
    calls,
    rows,
    CASE WHEN calls = 0 THEN 0 ELSE rows / calls END AS avg_rows
FROM pg_stat_statements
ORDER BY avg_rows DESC
LIMIT 10;
```

#### 특정 사용자의 쿼리 통계

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time
FROM pg_stat_statements
WHERE userid = (SELECT usesysid FROM pg_user WHERE usename = 'myuser')
ORDER BY total_exec_time DESC
LIMIT 10;
```

### 4.4 통계 초기화

```sql
-- 모든 통계 초기화
SELECT pg_stat_statements_reset();

-- 특정 쿼리의 통계만 초기화
SELECT pg_stat_statements_reset(0, 0, queryid)
FROM pg_stat_statements
WHERE query LIKE 'UPDATE users%';

-- 최소/최대값만 초기화
SELECT pg_stat_statements_reset(0, 0, 0, true);
```

### 4.5 보안 고려사항

- 슈퍼유저 또는 `pg_read_all_stats` 역할을 가진 사용자만 다른 사용자의 쿼리 텍스트를 볼 수 있음
- 일반 사용자는 자신이 실행한 쿼리만 조회 가능
- 민감한 쿼리 데이터가 포함될 수 있으므로 접근 권한 관리 필요

---

## 5. postgres_fdw

`postgres_fdw`는 외부 PostgreSQL 서버에 저장된 데이터에 투명하게 접근할 수 있는 Foreign Data Wrapper(FDW) 모듈입니다. `dblink`보다 더 표준적이고 성능이 우수합니다.

### 5.1 기본 설정

#### 확장 설치

```sql
CREATE EXTENSION postgres_fdw;
```

#### 외래 서버 생성

```sql
CREATE SERVER remote_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (
        host '192.168.1.100',
        port '5432',
        dbname 'remote_db'
    );
```

#### 사용자 매핑 생성

```sql
CREATE USER MAPPING FOR local_user
    SERVER remote_server
    OPTIONS (
        user 'remote_user',
        password 'remote_password'
    );
```

#### 외래 테이블 생성

```sql
-- 수동으로 외래 테이블 정의
CREATE FOREIGN TABLE remote_customers (
    id integer NOT NULL,
    name text,
    email text,
    created_at timestamp
)
    SERVER remote_server
    OPTIONS (
        schema_name 'public',
        table_name 'customers'
    );
```

#### 외래 스키마 가져오기

```sql
-- 원격 스키마의 모든 테이블을 한 번에 가져오기
IMPORT FOREIGN SCHEMA public
    FROM SERVER remote_server
    INTO local_schema;

-- 특정 테이블만 가져오기
IMPORT FOREIGN SCHEMA public
    LIMIT TO (customers, orders, products)
    FROM SERVER remote_server
    INTO local_schema;

-- 특정 테이블 제외하고 가져오기
IMPORT FOREIGN SCHEMA public
    EXCEPT (logs, temp_data)
    FROM SERVER remote_server
    INTO local_schema;
```

### 5.2 주요 옵션

#### 서버 옵션

| 옵션 | 설명 |
|------|------|
| `host` | 원격 서버 호스트명 또는 IP |
| `port` | 원격 서버 포트 (기본값: 5432) |
| `dbname` | 원격 데이터베이스명 |
| `connect_timeout` | 연결 타임아웃 (초) |

#### 비용 추정 옵션

```sql
-- 원격 EXPLAIN 사용하여 비용 추정 (더 정확하지만 오버헤드 있음)
ALTER SERVER remote_server OPTIONS (ADD use_remote_estimate 'true');

-- 수동 비용 설정
ALTER SERVER remote_server OPTIONS (
    ADD fdw_startup_cost '100',
    ADD fdw_tuple_cost '0.01'
);
```

#### 성능 관련 옵션

```sql
-- 페치 크기 설정 (한 번에 가져올 행 수, 기본값: 100)
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD fetch_size '1000');

-- 배치 INSERT 크기 (기본값: 1)
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD batch_size '100');

-- 비동기 실행 활성화
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD async_capable 'true');
```

### 5.3 데이터 조작

```sql
-- SELECT (원격 쿼리 실행)
SELECT * FROM remote_customers WHERE id > 1000;

-- INSERT
INSERT INTO remote_customers (name, email)
VALUES ('John Doe', 'john@example.com');

-- UPDATE
UPDATE remote_customers
SET email = 'newemail@example.com'
WHERE id = 1;

-- DELETE
DELETE FROM remote_customers WHERE id = 1;

-- COPY
COPY remote_customers FROM '/path/to/data.csv' WITH CSV;

-- TRUNCATE
TRUNCATE remote_customers;
```

### 5.4 조인 푸시다운 (Join Pushdown)

동일한 원격 서버의 테이블 간 조인은 원격 서버에서 실행됩니다:

```sql
-- 이 쿼리는 원격 서버에서 조인이 실행됨
SELECT c.name, o.order_date, o.total
FROM remote_customers c
JOIN remote_orders o ON c.id = o.customer_id
WHERE o.total > 100;
```

실행 계획 확인:

```sql
EXPLAIN VERBOSE
SELECT c.name, o.order_date
FROM remote_customers c
JOIN remote_orders o ON c.id = o.customer_id;
```

### 5.5 트랜잭션 관리

```sql
-- 병렬 커밋 활성화 (기본값: false)
ALTER SERVER remote_server OPTIONS (ADD parallel_commit 'true');

-- 병렬 롤백 활성화
ALTER SERVER remote_server OPTIONS (ADD parallel_abort 'true');
```

### 5.6 연결 관리

```sql
-- 현재 연결 상태 확인
SELECT * FROM postgres_fdw_get_connections(true);

-- 특정 서버 연결 해제
SELECT postgres_fdw_disconnect('remote_server');

-- 모든 연결 해제
SELECT postgres_fdw_disconnect_all();

-- 세션 종료 시 연결 유지 여부 설정
ALTER SERVER remote_server OPTIONS (ADD keep_connections 'off');
```

### 5.7 완전한 예제

```sql
-- 1. 확장 설치
CREATE EXTENSION postgres_fdw;

-- 2. 외래 서버 설정
CREATE SERVER production_db
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (
        host 'prod.example.com',
        port '5432',
        dbname 'production',
        fetch_size '500'
    );

-- 3. 사용자 매핑
CREATE USER MAPPING FOR CURRENT_USER
    SERVER production_db
    OPTIONS (user 'readonly_user', password 'secure_password');

-- 4. 스키마 가져오기
CREATE SCHEMA remote;
IMPORT FOREIGN SCHEMA public
    LIMIT TO (users, orders, products)
    FROM SERVER production_db
    INTO remote;

-- 5. 데이터 조회
SELECT
    u.name,
    COUNT(o.id) as order_count,
    SUM(o.total) as total_spent
FROM remote.users u
LEFT JOIN remote.orders o ON u.id = o.user_id
GROUP BY u.id, u.name
HAVING SUM(o.total) > 1000
ORDER BY total_spent DESC;
```

---

## 6. pg_trgm

`pg_trgm` 모듈은 삼중문자(Trigram) 매칭을 기반으로 텍스트 유사도를 결정하는 함수와 연산자를 제공합니다. 빠른 유사 문자열 검색과 LIKE/정규식 검색을 위한 인덱스 지원이 핵심 기능입니다.

### 6.1 삼중문자(Trigram) 개념

삼중문자는 문자열에서 연속된 3개 문자의 그룹입니다. 두 문자열의 유사도는 공유하는 삼중문자의 개수로 측정됩니다.

```sql
-- 삼중문자 확인
SELECT show_trgm('cat');
-- 결과: {"  c"," ca","at ","cat"}

SELECT show_trgm('hello');
-- 결과: {"  h"," he","ell","hel","llo","lo "}
```

처리 규칙:
- 비단어 문자(숫자가 아닌 특수문자)는 무시됨
- 각 단어 앞에 2개 공백, 뒤에 1개 공백 추가
- 대소문자 구분 없음

### 6.2 설치

```sql
CREATE EXTENSION pg_trgm;
```

### 6.3 함수

| 함수 | 반환 타입 | 설명 |
|------|-----------|------|
| `similarity(text, text)` | real | 두 문자열의 유사도 (0~1) |
| `show_trgm(text)` | text[] | 문자열의 삼중문자 배열 |
| `word_similarity(text, text)` | real | 첫 번째 문자열과 두 번째 문자열의 연속 범위 간 최대 유사도 |
| `strict_word_similarity(text, text)` | real | 단어 경계에 맞춘 유사도 |

#### 사용 예제

```sql
-- 기본 유사도
SELECT similarity('hello', 'hallo');
-- 결과: 0.5

SELECT similarity('PostgreSQL', 'Postgres');
-- 결과: 0.6666667

-- 단어 유사도 (부분 매칭에 유용)
SELECT word_similarity('word', 'two words');
-- 결과: 0.8

SELECT word_similarity('pg', 'PostgreSQL is great');
-- 결과: 0.5
```

### 6.4 연산자

| 연산자 | 반환 타입 | 설명 |
|--------|-----------|------|
| `text % text` | boolean | 유사도가 임계값 이상인지 확인 |
| `text <-> text` | real | 거리 (1 - 유사도) |
| `text <% text` | boolean | 단어 유사도가 임계값 이상인지 확인 |
| `text <<-> text` | real | 단어 유사도 기반 거리 |

#### 사용 예제

```sql
-- % 연산자로 유사 문자열 찾기
SELECT * FROM products WHERE name % 'laptop';

-- 거리 기반 정렬
SELECT name, name <-> 'laptop' AS distance
FROM products
ORDER BY distance
LIMIT 5;
```

### 6.5 설정 파라미터

```sql
-- 유사도 임계값 설정 (기본값: 0.3)
SET pg_trgm.similarity_threshold = 0.4;

-- 단어 유사도 임계값 (기본값: 0.6)
SET pg_trgm.word_similarity_threshold = 0.5;

-- 엄격한 단어 유사도 임계값 (기본값: 0.5)
SET pg_trgm.strict_word_similarity_threshold = 0.4;
```

### 6.6 인덱스 지원

`pg_trgm`은 GiST와 GIN 인덱스를 지원하여 유사도 검색, LIKE, 정규식 검색을 가속화합니다.

#### GIN 인덱스 (권장)

```sql
-- GIN 인덱스 생성
CREATE INDEX idx_products_name_gin ON products USING GIN (name gin_trgm_ops);
```

#### GiST 인덱스

```sql
-- GiST 인덱스 생성
CREATE INDEX idx_products_name_gist ON products USING GIST (name gist_trgm_ops);

-- 서명 길이 지정 (더 정밀한 검색, 기본값: 12바이트)
CREATE INDEX idx_products_name_gist ON products
    USING GIST (name gist_trgm_ops(siglen=32));
```

### 6.7 실용 예제

#### 유사 문자열 검색

```sql
-- 테이블 생성 및 데이터 삽입
CREATE TABLE words (word text);
INSERT INTO words VALUES
    ('PostgreSQL'), ('MySQL'), ('Oracle'), ('MongoDB'),
    ('Postgres'), ('MariaDB'), ('SQLite'), ('SQL Server');

-- 인덱스 생성
CREATE INDEX idx_words ON words USING GIN (word gin_trgm_ops);

-- 유사 문자열 검색
SELECT word, similarity(word, 'Postgres') AS sim
FROM words
WHERE word % 'Postgres'
ORDER BY sim DESC;
```

#### LIKE 패턴 검색 가속화

```sql
-- 인덱스를 사용한 LIKE 검색 (앵커 패턴 불필요)
SELECT * FROM products WHERE name LIKE '%laptop%';
SELECT * FROM products WHERE name LIKE '%phone%case%';
```

#### 정규식 검색 가속화

```sql
-- 인덱스를 사용한 정규식 검색
SELECT * FROM products WHERE name ~ '(laptop|notebook)';
SELECT * FROM products WHERE name ~* 'PHONE';  -- 대소문자 무시
```

#### 철자 교정 시스템

```sql
-- 단어 사전 테이블
CREATE TABLE dictionary (word text PRIMARY KEY);
CREATE INDEX idx_dictionary ON dictionary USING GIN (word gin_trgm_ops);

-- 철자 교정 제안
CREATE OR REPLACE FUNCTION suggest_spelling(input_word text, max_results int DEFAULT 5)
RETURNS TABLE(suggestion text, similarity real) AS $$
    SELECT word, similarity(word, input_word) AS sim
    FROM dictionary
    WHERE word % input_word
      AND length(word) BETWEEN length(input_word) - 2 AND length(input_word) + 2
    ORDER BY sim DESC
    LIMIT max_results;
$$ LANGUAGE SQL;

-- 사용 예
SELECT * FROM suggest_spelling('databse');
-- 결과: database, 0.6
```

#### 퍼지 검색 (Fuzzy Search)

```sql
-- 검색어와 유사한 모든 제품 찾기
CREATE OR REPLACE FUNCTION fuzzy_search(search_term text)
RETURNS TABLE(id int, name text, score real) AS $$
    SELECT id, name, similarity(name, search_term) AS score
    FROM products
    WHERE name % search_term
    ORDER BY score DESC
    LIMIT 20;
$$ LANGUAGE SQL;

-- 사용
SELECT * FROM fuzzy_search('iphone');
```

### 6.8 GIN vs GiST 비교

| 특성 | GIN | GiST |
|------|-----|------|
| 인덱스 크기 | 더 큼 | 더 작음 |
| 빌드 시간 | 더 느림 | 더 빠름 |
| 검색 성능 | 더 빠름 | 보통 |
| 거리 기반 정렬 | 비효율적 | 효율적 |
| 업데이트 성능 | 느림 | 빠름 |

권장 사항:
- 읽기 위주: GIN
- 거리 기반 상위 N개 검색: GiST
- 업데이트 빈번: GiST

---

## 7. hstore

`hstore` 모듈은 단일 PostgreSQL 값 내에 키/값 쌍 집합을 저장하는 데이터 타입입니다. 반정형(Semi-structured) 데이터나 많은 속성을 가진 행을 저장할 때 유용합니다.

### 7.1 설치

```sql
CREATE EXTENSION hstore;
```

### 7.2 기본 문법

```sql
-- hstore 리터럴
SELECT 'name=>John, age=>30, city=>Seoul'::hstore;

-- 공백과 특수문자 포함 시 큰따옴표 사용
SELECT '"first name"=>"John Doe", "e-mail"=>"john@example.com"'::hstore;

-- NULL 값
SELECT 'name=>John, address=>NULL'::hstore;
```

### 7.3 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `->` | 키로 값 추출 | `'a=>x, b=>y'::hstore -> 'a'` → `x` |
| `->` | 여러 키로 값 추출 | `'a=>x, b=>y'::hstore -> ARRAY['a','b']` → `{x,y}` |
| `?` | 키 존재 여부 | `'a=>1'::hstore ? 'a'` → `t` |
| `?&` | 모든 키 존재 여부 | `'a=>1,b=>2'::hstore ?& ARRAY['a','b']` → `t` |
| `?|` | 하나 이상의 키 존재 여부 | `'a=>1'::hstore ?| ARRAY['a','c']` → `t` |
| `@>` | 포함 여부 (왼쪽이 오른쪽 포함) | `'a=>1,b=>2'::hstore @> 'a=>1'` → `t` |
| `<@` | 포함 여부 (왼쪽이 오른쪽에 포함) | `'a=>1'::hstore <@ 'a=>1,b=>2'` → `t` |
| `||` | 두 hstore 병합 | `'a=>1'::hstore || 'b=>2'` → `"a"=>"1","b"=>"2"` |
| `-` | 키 삭제 | `'a=>1,b=>2'::hstore - 'a'` → `"b"=>"2"` |
| `-` | 여러 키 삭제 | `'a=>1,b=>2,c=>3'::hstore - ARRAY['a','b']` → `"c"=>"3"` |

### 7.4 함수

#### 생성 함수

```sql
-- 키-값 쌍으로 생성
SELECT hstore('name', 'John');
-- 결과: "name"=>"John"

-- 배열로 생성
SELECT hstore(ARRAY['name', 'age'], ARRAY['John', '30']);
-- 결과: "age"=>"30", "name"=>"John"

-- 레코드에서 생성
SELECT hstore(ROW('John', 30, 'Seoul'));
```

#### 추출 함수

```sql
-- 모든 키를 배열로
SELECT akeys('name=>John, age=>30'::hstore);
-- 결과: {name,age}

-- 모든 키를 집합으로
SELECT skeys('name=>John, age=>30'::hstore);
-- 결과: 행으로 반환

-- 모든 값을 배열로
SELECT avals('name=>John, age=>30'::hstore);
-- 결과: {John,30}

-- 키-값 쌍을 레코드 집합으로
SELECT (each('name=>John, age=>30'::hstore)).*;
```

#### 변환 함수

```sql
-- JSON으로 변환
SELECT hstore_to_json('name=>John, age=>30'::hstore);
-- 결과: {"age": "30", "name": "John"}

-- JSONB로 변환
SELECT hstore_to_jsonb('name=>John, age=>30'::hstore);

-- 숫자/불린 자동 변환
SELECT hstore_to_json_loose('name=>John, age=>30, active=>true'::hstore);
-- 결과: {"age": 30, "name": "John", "active": true}
```

### 7.5 첨자 접근 (Subscript Access)

PostgreSQL 14 이상에서 지원:

```sql
-- 테이블 생성
CREATE TABLE user_attributes (
    id serial PRIMARY KEY,
    attrs hstore
);

-- 데이터 삽입
INSERT INTO user_attributes (attrs) VALUES ('name=>John, role=>admin');

-- 첨자로 값 조회
SELECT attrs['name'] FROM user_attributes;

-- 첨자로 값 업데이트
UPDATE user_attributes SET attrs['role'] = 'user' WHERE id = 1;

-- 새 키 추가
UPDATE user_attributes SET attrs['email'] = 'john@example.com' WHERE id = 1;
```

### 7.6 인덱스

#### GIN 인덱스 (존재 여부 검사)

```sql
CREATE INDEX idx_attrs_gin ON user_attributes USING GIN (attrs);

-- 인덱스를 사용하는 쿼리
SELECT * FROM user_attributes WHERE attrs ? 'role';
SELECT * FROM user_attributes WHERE attrs ?& ARRAY['name', 'role'];
SELECT * FROM user_attributes WHERE attrs @> 'role=>admin';
```

#### GiST 인덱스

```sql
CREATE INDEX idx_attrs_gist ON user_attributes USING GIST (attrs);

-- 서명 길이 지정
CREATE INDEX idx_attrs_gist ON user_attributes
    USING GIST (attrs gist_hstore_ops(siglen=32));
```

#### B-tree 인덱스 (동등 비교)

```sql
CREATE INDEX idx_attrs_btree ON user_attributes USING BTREE (attrs);

-- 정확한 일치 검색에 사용
SELECT * FROM user_attributes WHERE attrs = 'name=>John, role=>admin';
```

### 7.7 실용 예제

#### 동적 속성 저장

```sql
-- 제품 테이블 (동적 속성 포함)
CREATE TABLE products (
    id serial PRIMARY KEY,
    name text NOT NULL,
    category text,
    attributes hstore
);

-- 인덱스 생성
CREATE INDEX idx_products_attrs ON products USING GIN (attributes);

-- 다양한 속성을 가진 제품 삽입
INSERT INTO products (name, category, attributes) VALUES
    ('iPhone 15', 'electronics', 'brand=>Apple, storage=>128GB, color=>black'),
    ('Galaxy S24', 'electronics', 'brand=>Samsung, storage=>256GB, color=>white'),
    ('MacBook Pro', 'electronics', 'brand=>Apple, ram=>16GB, storage=>512GB'),
    ('Nike Air Max', 'shoes', 'brand=>Nike, size=>10, color=>red');

-- 특정 속성으로 검색
SELECT name, attributes->'brand' AS brand
FROM products
WHERE attributes ? 'storage';

-- 속성 값으로 필터링
SELECT name, attributes
FROM products
WHERE attributes @> 'brand=>Apple';

-- 여러 조건 조합
SELECT name, attributes
FROM products
WHERE attributes @> 'brand=>Apple'
  AND attributes ? 'ram';
```

#### 레코드와 hstore 변환

```sql
-- 테이블 정의
CREATE TABLE employees (
    id int,
    name text,
    department text,
    salary numeric
);

-- 레코드를 hstore로 변환
INSERT INTO employees VALUES (1, 'John', 'IT', 50000);
SELECT hstore(e) FROM employees e WHERE id = 1;
-- 결과: "id"=>"1", "name"=>"John", "department"=>"IT", "salary"=>"50000"

-- hstore를 레코드 필드에 적용
SELECT (e #= 'salary=>60000, department=>HR').*
FROM employees e WHERE id = 1;
-- 결과: id=1, name=John, department=HR, salary=60000
```

#### 통계 분석

```sql
-- 가장 많이 사용되는 키 찾기
SELECT key, count(*) AS cnt
FROM (
    SELECT (each(attributes)).key FROM products
) AS keys
GROUP BY key
ORDER BY cnt DESC;

-- 특정 키의 값 분포
SELECT attributes->'brand' AS brand, count(*)
FROM products
WHERE attributes ? 'brand'
GROUP BY attributes->'brand'
ORDER BY count DESC;
```

---

## 8. ltree

`ltree` 모듈은 계층적 트리 구조의 데이터를 나타내는 데이터 타입을 구현합니다. 조직도, 카테고리 트리, 파일 시스템 경로 등을 효율적으로 저장하고 검색할 수 있습니다.

### 8.1 설치

```sql
CREATE EXTENSION ltree;
```

### 8.2 기본 개념

#### 레이블 (Label)

- 영숫자, 언더스코어(`_`), 하이픈(`-`)으로 구성
- 최대 1000자 제한
- 예: `42`, `Personal_Services`, `Science`

#### 레이블 경로 (Label Path)

- 점(`.`)으로 구분된 레이블 시퀀스
- 최대 65535개 레이블까지 가능
- 예: `Top.Countries.Europe.Korea`

### 8.3 데이터 타입

#### ltree

레이블 경로를 저장합니다:

```sql
SELECT 'Top.Science.Astronomy'::ltree;
```

#### lquery

정규식과 유사한 패턴 매칭을 위한 타입:

```sql
-- 기본 패턴
'foo'           -- 정확히 'foo'만 매칭
'*.foo.*'       -- 'foo'를 포함하는 모든 경로
'*.foo'         -- 마지막이 'foo'인 경로
'foo.*'         -- 'foo'로 시작하는 경로

-- 수량자
'*{n}'          -- 정확히 n개 레이블
'*{n,}'         -- n개 이상 레이블
'*{n,m}'        -- n개 이상 m개 이하 레이블
'*{,m}'         -- m개 이하 레이블

-- 수정자
'foo*'          -- 'foo'로 시작하는 레이블 (접두어 매칭)
'foo@'          -- 대소문자 무시
'foo%'          -- 언더스코어로 구분된 단어 매칭

-- 논리 연산
'foo|bar'       -- 'foo' 또는 'bar'
'!foo'          -- 'foo'가 아닌 것
```

#### ltxtquery

전문 검색 스타일 패턴:

```sql
-- 경로 내 위치와 무관하게 검색
'Europe & Russia'           -- Europe과 Russia 모두 포함
'Europe | Russia'           -- Europe 또는 Russia 포함
'Europe & !Transportation'  -- Europe 포함, Transportation 제외
'Astro*'                    -- Astro로 시작하는 레이블 포함
```

### 8.4 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `@>` | 조상인가? | `'Top.Science'::ltree @> 'Top.Science.Astronomy'::ltree` → `t` |
| `<@` | 자손인가? | `'Top.Science.Astronomy' <@ 'Top.Science'` → `t` |
| `~` | lquery 패턴 매칭 | `'Top.Science'::ltree ~ '*.Science'::lquery` → `t` |
| `?` | lquery 배열 중 하나와 매칭 | `'Top'::ltree ? ARRAY['Top', 'Bottom']::lquery[]` |
| `@` | ltxtquery 매칭 | `'Top.Science'::ltree @ 'Science'::ltxtquery` → `t` |
| `||` | 경로 연결 | `'Top'::ltree || 'Science'::ltree` → `Top.Science` |
| `||` | 텍스트 연결 | `'Top'::ltree || 'Science'::text` → `Top.Science` |

### 8.5 함수

#### 경로 조작

```sql
-- 부분 경로 추출 (시작 위치, 끝 위치)
SELECT subltree('Top.Science.Astronomy', 1, 2);
-- 결과: Science

-- 부분 경로 추출 (오프셋, 길이)
SELECT subpath('Top.Science.Astronomy', 0, 2);
-- 결과: Top.Science

SELECT subpath('Top.Science.Astronomy', 1);
-- 결과: Science.Astronomy

-- 레이블 개수
SELECT nlevel('Top.Science.Astronomy');
-- 결과: 3

-- 특정 위치의 레이블
SELECT index('Top.Science.Astronomy', 'Science');
-- 결과: 1 (0-based index)

-- 최대 공통 조상 (LCA)
SELECT lca('Top.Science.Astronomy', 'Top.Science.Physics');
-- 결과: Top.Science

SELECT lca(ARRAY['Top.A.B', 'Top.A.C', 'Top.A.D']::ltree[]);
-- 결과: Top.A
```

#### 타입 변환

```sql
-- 텍스트를 ltree로
SELECT text2ltree('Top.Science');

-- ltree를 텍스트로
SELECT ltree2text('Top.Science'::ltree);
```

### 8.6 인덱스

#### B-tree 인덱스

```sql
CREATE INDEX idx_path_btree ON categories USING BTREE (path);
```

지원 연산자: `<`, `<=`, `=`, `>=`, `>`

#### GiST 인덱스

```sql
-- 기본 GiST 인덱스
CREATE INDEX idx_path_gist ON categories USING GIST (path);

-- 서명 길이 지정 (더 정밀한 검색)
CREATE INDEX idx_path_gist ON categories
    USING GIST (path gist_ltree_ops(siglen=100));
```

지원 연산자: `<@`, `@>`, `@`, `~`, `?`

#### 배열용 GiST 인덱스

```sql
CREATE INDEX idx_paths_gist ON items USING GIST (paths gist__ltree_ops);
```

### 8.7 실용 예제

#### 카테고리 트리

```sql
-- 카테고리 테이블 생성
CREATE TABLE categories (
    id serial PRIMARY KEY,
    name text NOT NULL,
    path ltree NOT NULL
);

-- 인덱스 생성
CREATE INDEX idx_categories_path ON categories USING GIST (path);
CREATE INDEX idx_categories_path_btree ON categories USING BTREE (path);

-- 데이터 삽입
INSERT INTO categories (name, path) VALUES
    ('전자제품', 'Products.Electronics'),
    ('컴퓨터', 'Products.Electronics.Computers'),
    ('노트북', 'Products.Electronics.Computers.Laptops'),
    ('데스크톱', 'Products.Electronics.Computers.Desktops'),
    ('스마트폰', 'Products.Electronics.Phones.Smartphones'),
    ('의류', 'Products.Clothing'),
    ('남성의류', 'Products.Clothing.Men'),
    ('여성의류', 'Products.Clothing.Women');
```

#### 계층 구조 쿼리

```sql
-- 특정 카테고리의 모든 하위 카테고리
SELECT * FROM categories WHERE path <@ 'Products.Electronics';

-- 특정 카테고리의 직계 자식만
SELECT * FROM categories
WHERE path ~ 'Products.Electronics.*{1}'::lquery;

-- 특정 깊이의 카테고리
SELECT * FROM categories WHERE nlevel(path) = 3;

-- 형제 카테고리 찾기
SELECT * FROM categories
WHERE path ~ (
    SELECT subpath(path, 0, nlevel(path)-1)::text || '.*{1}'
    FROM categories WHERE name = '노트북'
)::lquery
AND name != '노트북';
```

#### 경로 패턴 검색

```sql
-- 'Computers'를 포함하는 모든 경로
SELECT * FROM categories WHERE path ~ '*.Computers.*';

-- 'Electronics' 또는 'Clothing'으로 끝나는 경로
SELECT * FROM categories WHERE path ~ '*.(Electronics|Clothing)';

-- 특정 단어를 포함하는 경로 (전문 검색)
SELECT * FROM categories WHERE path @ 'Computers & Laptops';
```

#### 조직도

```sql
-- 조직도 테이블
CREATE TABLE organization (
    id serial PRIMARY KEY,
    employee_name text NOT NULL,
    position text,
    path ltree NOT NULL
);

CREATE INDEX idx_org_path ON organization USING GIST (path);

-- 데이터
INSERT INTO organization (employee_name, position, path) VALUES
    ('김철수', 'CEO', 'Company'),
    ('이영희', 'CTO', 'Company.Engineering'),
    ('박민수', 'Backend Lead', 'Company.Engineering.Backend'),
    ('정수진', 'Frontend Lead', 'Company.Engineering.Frontend'),
    ('최동욱', 'CFO', 'Company.Finance'),
    ('강미란', 'HR Director', 'Company.HR');

-- 특정 부서의 모든 직원
SELECT employee_name, position, path
FROM organization
WHERE path <@ 'Company.Engineering';

-- 조직도 계층 표시
SELECT
    repeat('  ', nlevel(path) - 1) || employee_name AS org_chart,
    position
FROM organization
ORDER BY path;

-- 공통 상위 조직 찾기
SELECT lca(
    (SELECT path FROM organization WHERE employee_name = '박민수'),
    (SELECT path FROM organization WHERE employee_name = '정수진')
);
-- 결과: Company.Engineering
```

#### 경로 수정

```sql
-- 경로 중간에 레이블 삽입
CREATE OR REPLACE FUNCTION insert_label(p ltree, pos int, label text)
RETURNS ltree AS $$
    SELECT subpath(p, 0, pos) || label::ltree || subpath(p, pos);
$$ LANGUAGE SQL IMMUTABLE;

-- 사용 예
SELECT insert_label('A.B.C'::ltree, 1, 'X');
-- 결과: A.X.B.C

-- 부서 이동 (경로 업데이트)
UPDATE organization
SET path = 'Company.Finance' || subpath(path, nlevel('Company.Engineering'))
WHERE path <@ 'Company.Engineering.Backend';
```

---

## 9. 기타 유용한 모듈

### 9.1 pgcrypto (암호화)

데이터 암호화 및 해싱 함수를 제공합니다.

```sql
CREATE EXTENSION pgcrypto;

-- 일반 해싱
SELECT digest('hello', 'sha256');
SELECT encode(digest('hello', 'sha256'), 'hex');

-- 패스워드 해싱 (bcrypt)
SELECT crypt('mypassword', gen_salt('bf'));

-- 패스워드 검증
SELECT crypt('mypassword', stored_hash) = stored_hash;

-- 대칭 암호화 (AES)
SELECT encrypt('sensitive data', 'secret_key', 'aes');
SELECT convert_from(decrypt(encrypted_data, 'secret_key', 'aes'), 'UTF8');

-- PGP 암호화
SELECT pgp_sym_encrypt('secret message', 'password');
SELECT pgp_sym_decrypt(encrypted_data, 'password');

-- 난수 생성
SELECT gen_random_bytes(16);
SELECT gen_random_uuid();
```

### 9.2 uuid-ossp (UUID 생성)

다양한 UUID 생성 알고리즘을 제공합니다.

```sql
CREATE EXTENSION "uuid-ossp";

-- UUID v1 (타임스탬프 기반)
SELECT uuid_generate_v1();

-- UUID v4 (랜덤)
SELECT uuid_generate_v4();

-- UUID v5 (네임스페이스 기반)
SELECT uuid_generate_v5(uuid_ns_dns(), 'example.com');

-- 네임스페이스 상수
SELECT uuid_ns_dns();   -- DNS 네임스페이스
SELECT uuid_ns_url();   -- URL 네임스페이스
SELECT uuid_ns_oid();   -- OID 네임스페이스
SELECT uuid_ns_x500();  -- X.500 네임스페이스
```

### 9.3 citext (대소문자 구분 없는 텍스트)

대소문자를 구분하지 않는 문자열 비교를 지원합니다.

```sql
CREATE EXTENSION citext;

-- 테이블 생성
CREATE TABLE users (
    id serial PRIMARY KEY,
    email citext UNIQUE,
    name text
);

-- 대소문자 무시 검색
INSERT INTO users (email, name) VALUES ('John@Example.com', 'John');

-- 모두 같은 행을 찾음
SELECT * FROM users WHERE email = 'john@example.com';
SELECT * FROM users WHERE email = 'JOHN@EXAMPLE.COM';
SELECT * FROM users WHERE email = 'John@Example.COM';
```

### 9.4 fuzzystrmatch (문자열 유사도)

다양한 문자열 유사도 및 거리 함수를 제공합니다.

```sql
CREATE EXTENSION fuzzystrmatch;

-- Soundex (발음 유사도)
SELECT soundex('Robert'), soundex('Rupert');

-- Levenshtein 거리 (편집 거리)
SELECT levenshtein('hello', 'hallo');
-- 결과: 1

-- Metaphone
SELECT metaphone('PostgreSQL', 10);

-- Double Metaphone
SELECT dmetaphone('PostgreSQL');
```

### 9.5 tablefunc (크로스탭)

행을 열로 변환하는 피벗 테이블 기능을 제공합니다.

```sql
CREATE EXTENSION tablefunc;

-- 크로스탭 예제
CREATE TABLE sales (
    product text,
    quarter text,
    amount numeric
);

INSERT INTO sales VALUES
    ('A', 'Q1', 100), ('A', 'Q2', 150), ('A', 'Q3', 200),
    ('B', 'Q1', 80), ('B', 'Q2', 120), ('B', 'Q3', 160);

-- 피벗 테이블 생성
SELECT * FROM crosstab(
    'SELECT product, quarter, amount FROM sales ORDER BY 1, 2',
    'SELECT DISTINCT quarter FROM sales ORDER BY 1'
) AS ct(product text, q1 numeric, q2 numeric, q3 numeric);
```

### 9.6 dblink (원격 데이터베이스 연결)

다른 PostgreSQL 데이터베이스에 직접 쿼리를 실행합니다.

```sql
CREATE EXTENSION dblink;

-- 연결 설정
SELECT dblink_connect('myconn', 'host=remote.server dbname=otherdb user=myuser');

-- 쿼리 실행
SELECT * FROM dblink('myconn', 'SELECT id, name FROM users')
    AS t(id int, name text);

-- 연결 해제
SELECT dblink_disconnect('myconn');

-- 단일 쿼리 (연결 자동 관리)
SELECT * FROM dblink(
    'host=remote.server dbname=otherdb user=myuser',
    'SELECT id, name FROM users'
) AS t(id int, name text);
```

### 9.7 pg_buffercache (버퍼 캐시 모니터링)

공유 버퍼 캐시의 상태를 실시간으로 모니터링합니다.

```sql
CREATE EXTENSION pg_buffercache;

-- 테이블별 버퍼 사용량
SELECT
    c.relname,
    count(*) AS buffers,
    pg_size_pretty(count(*) * 8192) AS buffer_size
FROM pg_buffercache b
JOIN pg_class c ON b.relfilenode = pg_relation_filenode(c.oid)
    AND b.reldatabase IN (0, (SELECT oid FROM pg_database WHERE datname = current_database()))
GROUP BY c.relname
ORDER BY buffers DESC
LIMIT 10;

-- 버퍼 캐시 요약
SELECT * FROM pg_buffercache_summary();

-- 사용 횟수별 분포
SELECT * FROM pg_buffercache_usage_counts();
```

### 9.8 auto_explain (자동 실행 계획 로깅)

느린 쿼리의 실행 계획을 자동으로 로그에 기록합니다.

```ini
# postgresql.conf
shared_preload_libraries = 'auto_explain'

# 100ms 이상 걸리는 쿼리의 실행 계획 로깅
auto_explain.log_min_duration = '100ms'

# ANALYZE 정보 포함
auto_explain.log_analyze = on

# 버퍼 사용량 포함
auto_explain.log_buffers = on

# 타이밍 정보 포함
auto_explain.log_timing = on

# 중첩 문장 포함
auto_explain.log_nested_statements = on

# JSON 형식으로 출력
auto_explain.log_format = 'json'
```

세션 레벨에서 활성화:

```sql
LOAD 'auto_explain';
SET auto_explain.log_min_duration = '100ms';
SET auto_explain.log_analyze = on;
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Additional Supplied Modules](https://www.postgresql.org/docs/current/contrib.html)
- [PostgreSQL 공식 문서 - pg_stat_statements](https://www.postgresql.org/docs/current/pgstatstatements.html)
- [PostgreSQL 공식 문서 - postgres_fdw](https://www.postgresql.org/docs/current/postgres-fdw.html)
- [PostgreSQL 공식 문서 - pg_trgm](https://www.postgresql.org/docs/current/pgtrgm.html)
- [PostgreSQL 공식 문서 - hstore](https://www.postgresql.org/docs/current/hstore.html)
- [PostgreSQL 공식 문서 - ltree](https://www.postgresql.org/docs/current/ltree.html)
