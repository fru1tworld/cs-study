# PostgreSQL 레퍼런스와 부록

## 부록 F: 추가 제공 모듈 (Additional Supplied Modules)

### 목차

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

### 1. 개요

PostgreSQL은 핵심 데이터베이스 기능 외에도 다양한 추가 모듈을 제공합니다. 이 모듈들은 `contrib` 디렉토리에 위치하며, 필요에 따라 선택적으로 설치할 수 있습니다.

#### 1.1 특징

- 선택적 설치: 필요한 모듈만 설치하여 시스템 오버헤드 최소화
- 신뢰된 확장(Trusted Extensions): 일부 모듈은 슈퍼유저가 아닌 일반 사용자도 설치 가능
- 표준 SQL 인터페이스: `CREATE EXTENSION` 명령으로 간편하게 설치

#### 1.2 신뢰된 확장 목록

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

### 2. 설치 및 설정

#### 2.1 소스에서 빌드하기

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

#### 2.2 패키지 관리자를 통한 설치

대부분의 Linux 배포판에서는 `postgresql-contrib` 패키지로 제공됩니다:

```bash
# Debian/Ubuntu
sudo apt-get install postgresql-contrib

# RHEL/CentOS
sudo yum install postgresql-contrib

# macOS (Homebrew)
brew install postgresql  # contrib 모듈 포함
```

#### 2.3 확장 등록

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

### 3. 주요 모듈 목록

#### 3.1 성능 모니터링

| 모듈 | 설명 |
|------|------|
| `pg_stat_statements` | SQL 문의 계획 및 실행 통계 추적 |
| `pg_buffercache` | 공유 버퍼 캐시 상태 검사 |
| `pg_freespacemap` | Free Space Map 검사 |
| `pgrowlocks` | 테이블의 행 잠금 정보 표시 |
| `pgstattuple` | 튜플 수준 통계 획득 |
| `auto_explain` | 느린 쿼리의 실행 계획 자동 로깅 |

#### 3.2 데이터 타입

| 모듈 | 설명 |
|------|------|
| `hstore` | 키/값 쌍 데이터 타입 |
| `ltree` | 계층적 트리 구조 데이터 타입 |
| `citext` | 대소문자 구분 없는 문자열 타입 |
| `cube` | 다차원 큐브 데이터 타입 |
| `seg` | 선분 또는 부동소수점 구간 타입 |
| `isn` | ISBN, EAN, UPC 등 국제 표준 번호 |
| `uuid-ossp` | UUID 생성기 |

#### 3.3 인덱스 및 검색

| 모듈 | 설명 |
|------|------|
| `pg_trgm` | 삼중문자 기반 유사도 검색 |
| `bloom` | 블룸 필터 인덱스 |
| `btree_gin` | GIN용 B-tree 연산자 클래스 |
| `btree_gist` | GiST용 B-tree 연산자 클래스 |
| `fuzzystrmatch` | 문자열 유사도 함수 |
| `unaccent` | 발음 기호 제거 |

#### 3.4 외부 데이터 연결

| 모듈 | 설명 |
|------|------|
| `postgres_fdw` | 외부 PostgreSQL 서버 연결 |
| `file_fdw` | 서버 파일 시스템 데이터 접근 |
| `dblink` | 원격 PostgreSQL 데이터베이스 연결 |

#### 3.5 보안

| 모듈 | 설명 |
|------|------|
| `pgcrypto` | 암호화 함수 |
| `passwordcheck` | 패스워드 강도 검증 |
| `sslinfo` | 클라이언트 SSL 정보 획득 |

---

### 4. pg_stat_statements

`pg_stat_statements`는 PostgreSQL에서 실행되는 모든 SQL 문의 계획(Planning) 및 실행(Execution) 통계를 추적하는 모듈로, 성능 분석과 쿼리 최적화에 필수적입니다.

#### 4.1 설치 및 활성화

##### postgresql.conf 설정

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

# 계획 통계 추적 (기본값: off, 활성화 시 성능 영향 있음)
pg_stat_statements.track_planning = off

# 서버 종료 시 통계 저장 여부 (기본값: on)
pg_stat_statements.save = on
```

##### 확장 생성

```sql
-- 각 데이터베이스에서 실행
CREATE EXTENSION pg_stat_statements;
```

#### 4.2 pg_stat_statements 뷰

##### 주요 컬럼

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

##### 블록 I/O 통계

| 컬럼 | 설명 |
|------|------|
| `shared_blks_hit` | 공유 블록 캐시 히트 수 |
| `shared_blks_read` | 디스크에서 읽은 공유 블록 수 |
| `shared_blks_dirtied` | 더티된 공유 블록 수 |
| `shared_blks_written` | 기록된 공유 블록 수 |
| `temp_blks_read` | 읽은 임시 블록 수 |
| `temp_blks_written` | 기록된 임시 블록 수 |

##### WAL 통계

| 컬럼 | 설명 |
|------|------|
| `wal_records` | 생성된 WAL 레코드 수 |
| `wal_fpi` | WAL Full Page Image 수 |
| `wal_bytes` | 생성된 WAL 바이트 수 |

#### 4.3 실용 예제

##### 가장 오래 걸리는 쿼리 찾기

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

##### 가장 자주 호출되는 쿼리

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

##### 캐시 히트율이 낮은 쿼리 (디스크 I/O가 많은 쿼리)

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

##### 평균 행 수가 많은 쿼리

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

##### 특정 사용자의 쿼리 통계

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

#### 4.4 통계 초기화

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

#### 4.5 보안 고려사항

- 슈퍼유저 또는 `pg_read_all_stats` 권한을 가진 사용자만 다른 사용자의 쿼리 텍스트와 `queryid`를 조회할 수 있음
- 일반 사용자는 자신이 실행한 쿼리만 조회 가능
- 민감한 쿼리 데이터가 포함될 수 있으므로 접근 권한 관리 필요

---

### 5. postgres_fdw

`postgres_fdw`는 외부 PostgreSQL 서버에 저장된 데이터에 투명하게 접근할 수 있는 Foreign Data Wrapper(FDW) 모듈입니다. `dblink`에 비해 표준 SQL 인터페이스를 따르며 성능도 우수합니다.

#### 5.1 기본 설정

##### 확장 설치

```sql
CREATE EXTENSION postgres_fdw;
```

##### 외래 서버 생성

```sql
CREATE SERVER remote_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (
        host '192.168.1.100',
        port '5432',
        dbname 'remote_db'
    );
```

##### 사용자 매핑 생성

```sql
CREATE USER MAPPING FOR local_user
    SERVER remote_server
    OPTIONS (
        user 'remote_user',
        password 'remote_password'
    );
```

##### 외래 테이블 생성

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

##### 외래 스키마 가져오기

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

#### 5.2 주요 옵션

##### 서버 옵션

| 옵션 | 설명 |
|------|------|
| `host` | 원격 서버 호스트명 또는 IP |
| `port` | 원격 서버 포트 (기본값: 5432) |
| `dbname` | 원격 데이터베이스명 |
| `connect_timeout` | 연결 타임아웃 (초) |

##### 비용 추정 옵션

```sql
-- 원격 EXPLAIN 사용하여 비용 추정 (더 정확하지만 오버헤드 있음)
ALTER SERVER remote_server OPTIONS (ADD use_remote_estimate 'true');

-- 수동 비용 설정
ALTER SERVER remote_server OPTIONS (
    ADD fdw_startup_cost '100',
    ADD fdw_tuple_cost '0.01'
);
```

##### 성능 관련 옵션

```sql
-- 페치 크기 설정 (한 번에 가져올 행 수, 기본값: 100)
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD fetch_size '1000');

-- 배치 INSERT 크기 (기본값: 1)
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD batch_size '100');

-- 비동기 실행 활성화
ALTER FOREIGN TABLE remote_customers OPTIONS (ADD async_capable 'true');
```

#### 5.3 데이터 조작

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

#### 5.4 조인 푸시다운 (Join Pushdown)

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

#### 5.5 트랜잭션 관리

```sql
-- 병렬 커밋 활성화 (기본값: off)
ALTER SERVER remote_server OPTIONS (ADD parallel_commit 'true');

-- 병렬 롤백 활성화 (기본값: off)
ALTER SERVER remote_server OPTIONS (ADD parallel_abort 'true');
```

#### 5.6 연결 관리

```sql
-- 현재 연결 상태 확인
SELECT * FROM postgres_fdw_get_connections(true);

-- 특정 서버 연결 해제
SELECT postgres_fdw_disconnect('remote_server');

-- 모든 연결 해제
SELECT postgres_fdw_disconnect_all();

-- 트랜잭션 종료 시 연결 해제 설정 (기본값: on)
ALTER SERVER remote_server OPTIONS (ADD keep_connections 'off');
```

#### 5.7 완전한 예제

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

### 6. pg_trgm

`pg_trgm` 모듈은 삼중문자(Trigram) 매칭을 기반으로 텍스트 유사도를 측정하는 함수와 연산자를 제공합니다. 유사 문자열 검색과 LIKE/정규식 검색의 인덱스 지원이 핵심 기능입니다.

#### 6.1 삼중문자(Trigram) 개념

삼중문자는 문자열에서 연속된 3개 문자의 그룹입니다. 두 문자열의 유사도는 공유하는 삼중문자 수로 측정됩니다.

```sql
-- 삼중문자 확인
SELECT show_trgm('cat');
-- 결과: {"  c"," ca","at ","cat"}

SELECT show_trgm('hello');
-- 결과: {"  h"," he","ell","hel","llo","lo "}
```

처리 규칙:
- 비단어 문자(영숫자가 아닌 특수문자)는 무시됨
- 각 단어 앞에 공백 2개, 뒤에 공백 1개를 추가하여 삼중문자를 추출
- 대소문자 구분 없음

#### 6.2 설치

```sql
CREATE EXTENSION pg_trgm;
```

#### 6.3 함수

| 함수 | 반환 타입 | 설명 |
|------|-----------|------|
| `similarity(text, text)` | real | 두 문자열의 유사도 (0~1) |
| `show_trgm(text)` | text[] | 문자열의 삼중문자 배열 |
| `word_similarity(text, text)` | real | 첫 번째 문자열과 두 번째 문자열의 연속 범위 간 최대 유사도 |
| `strict_word_similarity(text, text)` | real | 단어 경계에 맞춘 유사도 |

##### 사용 예제

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

#### 6.4 연산자

| 연산자 | 반환 타입 | 설명 |
|--------|-----------|------|
| `text % text` | boolean | 유사도가 임계값 이상인지 확인 |
| `text <-> text` | real | 거리 (1 - 유사도) |
| `text <% text` | boolean | 단어 유사도가 임계값 이상인지 확인 |
| `text <<-> text` | real | 단어 유사도 기반 거리 |

##### 사용 예제

```sql
-- % 연산자로 유사 문자열 찾기
SELECT * FROM products WHERE name % 'laptop';

-- 거리 기반 정렬
SELECT name, name <-> 'laptop' AS distance
FROM products
ORDER BY distance
LIMIT 5;
```

#### 6.5 설정 파라미터

```sql
-- 유사도 임계값 설정 (기본값: 0.3)
SET pg_trgm.similarity_threshold = 0.4;

-- 단어 유사도 임계값 (기본값: 0.6)
SET pg_trgm.word_similarity_threshold = 0.5;

-- 엄격한 단어 유사도 임계값 (기본값: 0.5)
SET pg_trgm.strict_word_similarity_threshold = 0.4;
```

#### 6.6 인덱스 지원

`pg_trgm`은 GiST와 GIN 인덱스를 지원하여 유사도 검색, LIKE 검색, 정규식 검색을 가속화합니다.

##### GIN 인덱스 (권장)

```sql
-- GIN 인덱스 생성
CREATE INDEX idx_products_name_gin ON products USING GIN (name gin_trgm_ops);
```

##### GiST 인덱스

```sql
-- GiST 인덱스 생성
CREATE INDEX idx_products_name_gist ON products USING GIST (name gist_trgm_ops);

-- 서명 길이 지정 (더 정밀한 검색, 기본값: 12바이트)
CREATE INDEX idx_products_name_gist ON products
    USING GIST (name gist_trgm_ops(siglen=32));
```

#### 6.7 실용 예제

##### 유사 문자열 검색

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

##### LIKE 패턴 검색 가속화

```sql
-- 인덱스를 사용한 LIKE 검색 (앵커 패턴 불필요)
SELECT * FROM products WHERE name LIKE '%laptop%';
SELECT * FROM products WHERE name LIKE '%phone%case%';
```

##### 정규식 검색 가속화

```sql
-- 인덱스를 사용한 정규식 검색
SELECT * FROM products WHERE name ~ '(laptop|notebook)';
SELECT * FROM products WHERE name ~* 'PHONE';  -- 대소문자 무시
```

##### 철자 교정 시스템

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

##### 퍼지 검색 (Fuzzy Search)

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

#### 6.8 GIN vs GiST 비교

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

### 7. hstore

`hstore` 모듈은 단일 PostgreSQL 값에 키/값 쌍 집합을 저장하는 데이터 타입입니다. 반정형(Semi-structured) 데이터나 속성이 많은 행을 저장할 때 유용합니다.

#### 7.1 설치

```sql
CREATE EXTENSION hstore;
```

#### 7.2 기본 문법

```sql
-- hstore 리터럴
SELECT 'name=>John, age=>30, city=>Seoul'::hstore;

-- 공백과 특수문자 포함 시 큰따옴표 사용
SELECT '"first name"=>"John Doe", "e-mail"=>"john@example.com"'::hstore;

-- NULL 값
SELECT 'name=>John, address=>NULL'::hstore;
```

#### 7.3 연산자

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

#### 7.4 함수

##### 생성 함수

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

##### 추출 함수

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

##### 변환 함수

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

#### 7.5 첨자 접근 (Subscript Access)

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

#### 7.6 인덱스

##### GIN 인덱스 (존재 여부 검사)

```sql
CREATE INDEX idx_attrs_gin ON user_attributes USING GIN (attrs);

-- 인덱스를 사용하는 쿼리
SELECT * FROM user_attributes WHERE attrs ? 'role';
SELECT * FROM user_attributes WHERE attrs ?& ARRAY['name', 'role'];
SELECT * FROM user_attributes WHERE attrs @> 'role=>admin';
```

##### GiST 인덱스

```sql
CREATE INDEX idx_attrs_gist ON user_attributes USING GIST (attrs);

-- 서명 길이 지정
CREATE INDEX idx_attrs_gist ON user_attributes
    USING GIST (attrs gist_hstore_ops(siglen=32));
```

##### B-tree 인덱스 (동등 비교)

```sql
CREATE INDEX idx_attrs_btree ON user_attributes USING BTREE (attrs);

-- 정확한 일치 검색에 사용
SELECT * FROM user_attributes WHERE attrs = 'name=>John, role=>admin';
```

#### 7.7 실용 예제

##### 동적 속성 저장

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

##### 레코드와 hstore 변환

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

##### 통계 분석

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

### 8. ltree

`ltree` 모듈은 계층적 트리 구조 데이터를 표현하는 데이터 타입을 구현합니다. 조직도, 카테고리 트리, 파일 시스템 경로 등을 효율적으로 저장하고 검색할 수 있습니다.

#### 8.1 설치

```sql
CREATE EXTENSION ltree;
```

#### 8.2 기본 개념

##### 레이블 (Label)

- 영숫자, 언더스코어(`_`), 하이픈(`-`)으로 구성
- 최대 1000자 제한
- 예: `42`, `Personal_Services`, `Science`

##### 레이블 경로 (Label Path)

- 점(`.`)으로 구분된 레이블 시퀀스
- 최대 65535개 레이블까지 가능
- 예: `Top.Countries.Europe.Korea`

#### 8.3 데이터 타입

##### ltree

레이블 경로를 저장합니다:

```sql
SELECT 'Top.Science.Astronomy'::ltree;
```

##### lquery

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

##### ltxtquery

전문 검색 스타일 패턴:

```sql
-- 경로 내 위치와 무관하게 검색
'Europe & Russia'           -- Europe과 Russia 모두 포함
'Europe | Russia'           -- Europe 또는 Russia 포함
'Europe & !Transportation'  -- Europe 포함, Transportation 제외
'Astro*'                    -- Astro로 시작하는 레이블 포함
```

#### 8.4 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `@>` | 조상인가? | `'Top.Science'::ltree @> 'Top.Science.Astronomy'::ltree` → `t` |
| `<@` | 자손인가? | `'Top.Science.Astronomy' <@ 'Top.Science'` → `t` |
| `~` | lquery 패턴 매칭 | `'Top.Science'::ltree ~ '*.Science'::lquery` → `t` |
| `?` | lquery 배열 중 하나와 매칭 | `'Top'::ltree ? ARRAY['Top', 'Bottom']::lquery[]` |
| `@` | ltxtquery 매칭 | `'Top.Science'::ltree @ 'Science'::ltxtquery` → `t` |
| `||` | 경로 연결 | `'Top'::ltree || 'Science'::ltree` → `Top.Science` |
| `||` | 텍스트 연결 | `'Top'::ltree || 'Science'::text` → `Top.Science` |

#### 8.5 함수

##### 경로 조작

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

##### 타입 변환

```sql
-- 텍스트를 ltree로
SELECT text2ltree('Top.Science');

-- ltree를 텍스트로
SELECT ltree2text('Top.Science'::ltree);
```

#### 8.6 인덱스

##### B-tree 인덱스

```sql
CREATE INDEX idx_path_btree ON categories USING BTREE (path);
```

지원 연산자: `<`, `<=`, `=`, `>=`, `>`

##### GiST 인덱스

```sql
-- 기본 GiST 인덱스
CREATE INDEX idx_path_gist ON categories USING GIST (path);

-- 서명 길이 지정 (더 정밀한 검색)
CREATE INDEX idx_path_gist ON categories
    USING GIST (path gist_ltree_ops(siglen=100));
```

지원 연산자: `<@`, `@>`, `@`, `~`, `?`

##### 배열용 GiST 인덱스

```sql
CREATE INDEX idx_paths_gist ON items USING GIST (paths gist__ltree_ops);
```

#### 8.7 실용 예제

##### 카테고리 트리

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

##### 계층 구조 쿼리

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

##### 경로 패턴 검색

```sql
-- 'Computers'를 포함하는 모든 경로
SELECT * FROM categories WHERE path ~ '*.Computers.*';

-- 'Electronics' 또는 'Clothing'으로 끝나는 경로
SELECT * FROM categories WHERE path ~ '*.(Electronics|Clothing)';

-- 특정 단어를 포함하는 경로 (전문 검색)
SELECT * FROM categories WHERE path @ 'Computers & Laptops';
```

##### 조직도

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

##### 경로 수정

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

### 9. 기타 유용한 모듈

#### 9.1 pgcrypto (암호화)

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

#### 9.2 uuid-ossp (UUID 생성)

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

#### 9.3 citext (대소문자 구분 없는 텍스트)

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

#### 9.4 fuzzystrmatch (문자열 유사도)

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

#### 9.5 tablefunc (크로스탭)

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

#### 9.6 dblink (원격 데이터베이스 연결)

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

#### 9.7 pg_buffercache (버퍼 캐시 모니터링)

공유 버퍼 캐시의 상태를 모니터링합니다.

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

#### 9.8 auto_explain (자동 실행 계획 로깅)

실행 시간이 임계값을 초과하는 쿼리의 실행 계획을 자동으로 로그에 기록합니다.

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

### 참고 자료

- [PostgreSQL 공식 문서 - Additional Supplied Modules](https://www.postgresql.org/docs/current/contrib.html)
- [PostgreSQL 공식 문서 - pg_stat_statements](https://www.postgresql.org/docs/current/pgstatstatements.html)
- [PostgreSQL 공식 문서 - postgres_fdw](https://www.postgresql.org/docs/current/postgres-fdw.html)
- [PostgreSQL 공식 문서 - pg_trgm](https://www.postgresql.org/docs/current/pgtrgm.html)
- [PostgreSQL 공식 문서 - hstore](https://www.postgresql.org/docs/current/hstore.html)
- [PostgreSQL 공식 문서 - ltree](https://www.postgresql.org/docs/current/ltree.html)

---

## PostgreSQL SQL 명령어 참조 (SQL Commands Reference)

### 목차

1. [데이터 조작 명령어 (DML)](#1-데이터-조작-명령어-dml)
2. [데이터 정의 명령어 (DDL)](#2-데이터-정의-명령어-ddl)
3. [권한 관리 명령어 (DCL)](#3-권한-관리-명령어-dcl)
4. [트랜잭션 제어 명령어 (TCL)](#4-트랜잭션-제어-명령어-tcl)
5. [기타 유틸리티 명령어](#5-기타-유틸리티-명령어)

---

### 1. 데이터 조작 명령어 (DML)

데이터 조작 언어(Data Manipulation Language)는 테이블의 데이터를 조회, 삽입, 수정, 삭제하는 명령어입니다.

#### 1.1 SELECT - 데이터 조회

`SELECT`는 테이블이나 뷰에서 행(row)을 조회하는 명령어입니다.

##### 기본 구문 (Syntax)

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

##### 처리 순서

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

##### 주요 절 설명

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

##### 예제

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

#### 1.2 INSERT - 데이터 삽입

`INSERT`는 테이블에 새로운 행을 삽입하는 명령어입니다.

##### 기본 구문

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
INSERT INTO table_name [ AS alias ] [ ( column_name [, ...] ) ]
    [ OVERRIDING { SYSTEM | USER } VALUE ]
    { DEFAULT VALUES | VALUES ( { expression | DEFAULT } [, ...] ) [, ...] | query }
    [ ON CONFLICT [ conflict_target ] conflict_action ]
    [ RETURNING [ WITH ( { OLD | NEW } AS output_alias [, ...] ) ]
                { * | output_expression [ [ AS ] output_name ] } [, ...] ]
```

##### 주요 특징

- 열 이름은 임의의 순서로 나열 가능
- 생략된 열은 기본값(DEFAULT) 또는 NULL로 채워짐
- 데이터 타입이 맞지 않으면 자동 타입 변환 시도
- `RETURNING` 절로 삽입/수정된 행의 계산된 값 반환 가능
- `ON CONFLICT`로 유니크 제약조건 위반 시 대체 동작 지정 가능

##### 예제

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

#### 1.3 UPDATE - 데이터 수정

`UPDATE`는 조건을 만족하는 행의 열 값을 변경하는 명령어입니다.

##### 기본 구문

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

##### 주요 특징

- SET 절에 명시된 열만 수정됨; 다른 열은 이전 값 유지
- RETURNING 절로 수정된 행의 값 반환 가능 (기본적으로 새 값, OLD 키워드로 이전 값)

##### 예제

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

#### 1.4 DELETE - 데이터 삭제

`DELETE`는 테이블에서 조건을 만족하는 행을 삭제하는 명령어입니다.

##### 기본 구문

```sql
[ WITH [ RECURSIVE ] with_query [, ...] ]
DELETE FROM [ ONLY ] table_name [ * ] [ [ AS ] alias ]
    [ USING from_item [, ...] ]
    [ WHERE condition | WHERE CURRENT OF cursor_name ]
    [ RETURNING [ WITH ( { OLD | NEW } AS output_alias [, ...] ) ]
                { * | output_expression [ [ AS ] output_name ] } [, ...] ]
```

##### 주요 특징

- WHERE 절이 없으면 테이블의 모든 행이 삭제됨
- 대상 테이블에 대한 DELETE 권한 필요
- USING 절의 테이블에는 SELECT 권한이 필요
- 모든 행을 빠르게 삭제하려면 `TRUNCATE` 사용 권장

##### 예제

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

#### 1.5 TRUNCATE - 테이블 비우기

`TRUNCATE`는 테이블의 모든 행을 빠르게 삭제하는 명령어입니다.

##### 기본 구문

```sql
TRUNCATE [ TABLE ] [ ONLY ] name [ * ] [, ... ]
    [ RESTART IDENTITY | CONTINUE IDENTITY ] [ CASCADE | RESTRICT ]
```

##### DELETE와의 차이점

| 특성 | TRUNCATE | DELETE |
|------|----------|--------|
| 속도 | 매우 빠름 (행 개수와 무관) | 행 수에 비례 |
| 트랜잭션 안전성 | 지원 | 지원 |
| MVCC 가시성 | 테이블 수준 잠금 | 행 수준 잠금 |
| 트리거 실행 | TRUNCATE 트리거만 | DELETE 트리거 |
| 조건 지정 | 불가능 | WHERE 절 사용 가능 |

##### 예제

```sql
-- 단일 테이블 비우기
TRUNCATE bigtable;

-- 시퀀스 재시작과 함께 비우기
TRUNCATE bigtable RESTART IDENTITY;

-- 여러 테이블을 CASCADE로 비우기
TRUNCATE bigtable, othertable CASCADE;
```

---

#### 1.6 MERGE - 조건부 삽입/수정/삭제

`MERGE`는 조건에 따라 INSERT, UPDATE, DELETE를 수행하는 명령어입니다 (PostgreSQL 15+).

##### 기본 구문

```sql
MERGE INTO target_table [ [ AS ] target_alias ]
USING data_source [ [ AS ] source_alias ]
ON join_condition
when_clause [...]
```

##### 예제

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

### 2. 데이터 정의 명령어 (DDL)

데이터 정의 언어(Data Definition Language)는 데이터베이스 객체를 생성, 수정, 삭제하는 명령어입니다.

#### 2.1 CREATE TABLE - 테이블 생성

`CREATE TABLE`은 새로운 테이블을 생성하는 명령어입니다.

##### 기본 구문

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

##### 주요 제약조건 (Constraints)

| 제약조건 | 설명 |
|----------|------|
| `NOT NULL` | 열에 NULL 값 불허 |
| `UNIQUE` | 열의 모든 값이 고유해야 함 |
| `PRIMARY KEY` | 행의 고유 식별자 (NOT NULL + UNIQUE) |
| `CHECK` | 열 값이 불리언 표현식을 만족해야 함 |
| `DEFAULT` | 열의 기본값 지정 |
| `FOREIGN KEY` | 다른 테이블의 열을 참조 |
| `EXCLUDE` | 지정된 조건이 참이 되는 것을 방지 |

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `TEMPORARY/TEMP` | 세션 또는 트랜잭션 종료 시 자동 삭제 |
| `UNLOGGED` | WAL에 기록하지 않음 (빠르지만 장애 시 데이터 손실 가능) |
| `IF NOT EXISTS` | 테이블이 이미 존재할 경우 오류 방지 |
| `PARTITION BY` | 테이블을 파티션으로 분할 (RANGE, LIST, HASH) |
| `INHERITS` | 부모 테이블에서 열 상속 |
| `TABLESPACE` | 테이블 저장 위치 지정 |

##### 예제

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

#### 2.2 ALTER TABLE - 테이블 수정

`ALTER TABLE`은 기존 테이블의 정의를 변경하는 명령어입니다.

##### 기본 구문

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

##### 주요 액션

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

##### 예제

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

#### 2.3 DROP TABLE - 테이블 삭제

`DROP TABLE`은 테이블을 삭제하는 명령어입니다.

##### 기본 구문

```sql
DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
```

##### 옵션

| 옵션 | 설명 |
|------|------|
| `IF EXISTS` | 테이블이 존재하지 않아도 오류 발생하지 않음 |
| `CASCADE` | 의존하는 객체(뷰, 외래 키 등)도 함께 삭제 |
| `RESTRICT` | 의존하는 객체가 있으면 삭제 거부 (기본값) |

##### 예제

```sql
-- 기본 삭제
DROP TABLE films;

-- 존재할 경우에만 삭제
DROP TABLE IF EXISTS temp_data;

-- 의존 객체와 함께 삭제
DROP TABLE orders CASCADE;
```

---

#### 2.4 CREATE INDEX - 인덱스 생성

`CREATE INDEX`는 테이블의 특정 열에 인덱스를 생성하여 조회 성능을 향상시킵니다.

##### 기본 구문

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

##### 인덱스 메서드

| 메서드 | 설명 | 용도 |
|--------|------|------|
| `B-tree` | 기본값; 균형 트리 | 비교 연산 (<, <=, =, >=, >) |
| `Hash` | 해시 인덱스 | 동등 비교 (=) |
| `GiST` | 일반화된 검색 트리 | 기하학적 데이터, 전문 검색 |
| `SP-GiST` | 공간 분할 GiST | 불균형 데이터 구조 |
| `GIN` | 역인덱스 | 배열, 전문 검색, JSONB |
| `BRIN` | 블록 범위 인덱스 | 대용량 테이블의 순차적 데이터 |

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `UNIQUE` | 고유 값 강제; 중복 항목 방지 |
| `CONCURRENTLY` | 쓰기 잠금 없이 인덱스 생성 (시간은 더 걸림) |
| `INCLUDE` | 인덱스 전용 스캔을 위한 비키 열 추가 |
| `WHERE` | 행의 부분집합에 대한 부분 인덱스 생성 |

##### 예제

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

#### 2.5 CREATE VIEW - 뷰 생성

`CREATE VIEW`는 쿼리에 대한 뷰를 정의합니다. 뷰는 물리적으로 구체화되지 않으며, 뷰가 참조될 때마다 쿼리가 실행됩니다.

##### 기본 구문

```sql
CREATE [ OR REPLACE ] [ TEMP | TEMPORARY ] [ RECURSIVE ] VIEW name [ ( column_name [, ...] ) ]
    [ WITH ( view_option_name [= view_option_value] [, ... ] ) ]
    AS query
    [ WITH [ CASCADED | LOCAL ] CHECK OPTION ]
```

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `OR REPLACE` | 기존 뷰가 있으면 대체 (열 구조가 동일해야 함) |
| `TEMPORARY/TEMP` | 세션 종료 시 자동 삭제되는 임시 뷰 |
| `RECURSIVE` | WITH RECURSIVE 구문을 사용하는 재귀 뷰 |
| `check_option` | 업데이트 가능한 뷰에 대해 `local` 또는 `cascaded` |
| `security_barrier` | 뷰에 대한 행 수준 보안 활성화 |
| `security_invoker` | 뷰 소유자가 아닌 사용자의 권한으로 기본 관계 확인 |
| `CHECK OPTION` | 삽입/수정된 행이 뷰 정의 조건을 만족하는지 확인 |

##### 업데이트 가능한 뷰의 조건

뷰가 자동으로 업데이트 가능하려면 다음 모든 조건을 만족해야 합니다:

- FROM 목록에 정확히 하나의 항목 (테이블 또는 업데이트 가능한 뷰)
- 최상위 수준에서 WITH, DISTINCT, GROUP BY, HAVING, LIMIT, OFFSET 없음
- 최상위 수준에서 집합 연산 (UNION, INTERSECT, EXCEPT) 없음
- SELECT 목록에 집계 함수, 윈도우 함수, 집합 반환 함수 없음

##### 예제

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

#### 2.6 CREATE FUNCTION - 함수 생성

`CREATE FUNCTION`은 새로운 함수를 정의합니다.

##### 기본 구문

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

##### 주요 함수 속성

| 속성 | 설명 |
|------|------|
| `IMMUTABLE` | 데이터베이스 수정 불가; 동일 인수에 항상 동일 결과 |
| `STABLE` | 데이터베이스 수정 불가; 단일 스캔 내에서 일관성 유지 |
| `VOLATILE` | 단일 스캔 내에서도 값이 변경될 수 있음 (기본값) |
| `STRICT` / `RETURNS NULL ON NULL INPUT` | 인수가 NULL이면 NULL 반환 |
| `SECURITY DEFINER` | 소유자의 권한으로 실행 (vs `SECURITY INVOKER` - 기본값) |
| `PARALLEL SAFE` | 병렬 모드에서 실행해도 안전 |

##### 예제

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

#### 2.7 CREATE TRIGGER - 트리거 생성

`CREATE TRIGGER`는 특정 테이블과 연결되어 특정 작업 수행 시 지정된 함수를 실행하는 트리거를 정의합니다.

##### 기본 구문

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

##### 트리거 타이밍

| 타이밍 | 설명 |
|--------|------|
| `BEFORE` | 작업 전에 트리거 실행 |
| `AFTER` | 작업 후에 트리거 실행 |
| `INSTEAD OF` | 작업 대신 트리거 실행 (뷰에서만 사용) |

##### 트리거 레벨

| 레벨 | 설명 |
|------|------|
| `FOR EACH ROW` | 영향 받는 각 행마다 한 번 실행 |
| `FOR EACH STATEMENT` | SQL 문마다 한 번 실행 (기본값) |

##### 예제

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

#### 2.8 기타 CREATE 명령어

##### CREATE DATABASE - 데이터베이스 생성

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

##### CREATE SCHEMA - 스키마 생성

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

##### CREATE SEQUENCE - 시퀀스 생성

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

### 3. 권한 관리 명령어 (DCL)

데이터 제어 언어(Data Control Language)는 데이터베이스 객체에 대한 접근 권한을 관리합니다.

#### 3.1 GRANT - 권한 부여

`GRANT` 명령어는 데이터베이스 객체에 대한 접근 권한을 정의하거나 역할의 멤버십을 부여합니다.

##### 기본 구문

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

##### 주요 권한 유형

| 객체 유형 | 사용 가능한 권한 |
|-----------|------------------|
| 테이블/뷰 | SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, MAINTAIN |
| 시퀀스 | USAGE, SELECT, UPDATE |
| 데이터베이스 | CREATE, CONNECT, TEMPORARY |
| 함수/프로시저 | EXECUTE |
| 스키마 | CREATE, USAGE |

##### 주요 개념

| 개념 | 설명 |
|------|------|
| `PUBLIC` | 모든 역할(현재 및 미래)에 권한 부여 |
| `WITH GRANT OPTION` | 수신자가 다른 사람에게 권한을 부여할 수 있음 |
| `GRANTED BY` | 누가 권한을 부여했는지 기록; 현재 사용자여야 함 |
| 소유자 권한 | 객체 소유자는 기본적으로 모든 권한 보유; 취소 불가 |

##### 예제

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

##### 역할 멤버십 옵션

| 옵션 | 설명 |
|------|------|
| `ADMIN` | 멤버가 멤버십을 부여/취소 가능 (기본값 FALSE) |
| `INHERIT` | 멤버가 역할의 권한을 상속 (기본값은 상속 속성에 따름) |
| `SET` | 멤버가 SET ROLE로 부여된 역할로 변경 가능 (기본값 TRUE) |

---

#### 3.2 REVOKE - 권한 취소

`REVOKE` 명령어는 하나 이상의 역할에서 접근 권한을 제거합니다.

##### 기본 구문

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

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `GRANT OPTION FOR` | 권한 자체가 아닌 부여 옵션만 취소 |
| `CASCADE` | 의존하는 권한도 함께 취소 |
| `RESTRICT` | 의존하는 권한이 있으면 취소 거부 (기본값) |

##### 예제

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

#### 3.3 CREATE ROLE - 역할 생성

`CREATE ROLE`은 새로운 데이터베이스 역할을 정의합니다.

##### 기본 구문

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

##### 주요 역할 속성

| 속성 | 설명 |
|------|------|
| `SUPERUSER` | 슈퍼유저 권한 (모든 접근 검사 우회) |
| `CREATEDB` | 데이터베이스 생성 가능 |
| `CREATEROLE` | 새 역할 생성 가능 |
| `LOGIN` | 로그인 가능 (vs NOLOGIN: 그룹 역할용) |
| `INHERIT` | 멤버인 역할의 권한 상속 |
| `REPLICATION` | 복제 연결 가능 |
| `BYPASSRLS` | 행 수준 보안 우회 |

##### 예제

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

#### 3.4 ALTER DEFAULT PRIVILEGES - 기본 권한 변경

`ALTER DEFAULT PRIVILEGES`는 향후 생성될 객체에 적용될 기본 접근 권한을 정의합니다.

##### 기본 구문

```sql
ALTER DEFAULT PRIVILEGES
    [ FOR { ROLE | USER } target_role [, ...] ]
    [ IN SCHEMA schema_name [, ...] ]
    abbreviated_grant_or_revoke
```

##### 예제

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

### 4. 트랜잭션 제어 명령어 (TCL)

트랜잭션 제어 언어(Transaction Control Language)는 트랜잭션의 시작, 커밋, 롤백을 관리합니다.

#### 4.1 BEGIN - 트랜잭션 시작

`BEGIN`은 트랜잭션 블록을 시작합니다.

```sql
BEGIN [ WORK | TRANSACTION ] [ transaction_mode [, ...] ]

-- transaction_mode:
    ISOLATION LEVEL { SERIALIZABLE | REPEATABLE READ | READ COMMITTED | READ UNCOMMITTED }
    READ WRITE | READ ONLY
    [ NOT ] DEFERRABLE
```

##### 예제

```sql
BEGIN;
-- 또는
BEGIN TRANSACTION;

-- 격리 수준 지정
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

#### 4.2 COMMIT - 트랜잭션 커밋

`COMMIT`은 현재 트랜잭션을 커밋합니다.

```sql
COMMIT [ WORK | TRANSACTION ] [ AND [ NO ] CHAIN ]
```

##### 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

#### 4.3 ROLLBACK - 트랜잭션 롤백

`ROLLBACK`은 현재 트랜잭션을 중단하고 모든 변경 사항을 취소합니다.

```sql
ROLLBACK [ WORK | TRANSACTION ] [ AND [ NO ] CHAIN ]
```

##### 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
-- 문제 발생!
ROLLBACK;  -- 변경 사항 취소
```

#### 4.4 SAVEPOINT - 저장점 정의

`SAVEPOINT`는 현재 트랜잭션 내에서 새로운 저장점을 정의합니다.

```sql
SAVEPOINT savepoint_name
```

#### 4.5 ROLLBACK TO SAVEPOINT - 저장점으로 롤백

```sql
ROLLBACK [ WORK | TRANSACTION ] TO [ SAVEPOINT ] savepoint_name
```

#### 4.6 RELEASE SAVEPOINT - 저장점 해제

```sql
RELEASE [ SAVEPOINT ] savepoint_name
```

##### 저장점 사용 예제

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
SAVEPOINT my_savepoint;
UPDATE accounts SET balance = balance + 100 WHERE id = 999;  -- 존재하지 않는 계정
ROLLBACK TO SAVEPOINT my_savepoint;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

#### 4.7 SET TRANSACTION - 트랜잭션 특성 설정

```sql
SET TRANSACTION transaction_mode [, ...]
SET TRANSACTION SNAPSHOT snapshot_id
SET SESSION CHARACTERISTICS AS TRANSACTION transaction_mode [, ...]
```

##### 격리 수준 (Isolation Levels)

| 격리 수준 | 설명 |
|-----------|------|
| `READ UNCOMMITTED` | 커밋되지 않은 데이터 읽기 가능 (PostgreSQL에서는 READ COMMITTED로 동작) |
| `READ COMMITTED` | 커밋된 데이터만 읽기 (기본값) |
| `REPEATABLE READ` | 트랜잭션 시작 시점의 스냅샷 사용 |
| `SERIALIZABLE` | 가장 엄격한 격리; 직렬화 가능한 트랜잭션 |

##### 예제

```sql
BEGIN;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE READ ONLY;
SELECT * FROM accounts;
COMMIT;
```

---

### 5. 기타 유틸리티 명령어

#### 5.1 EXPLAIN - 실행 계획 표시

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

##### 예제

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

#### 5.2 ANALYZE - 통계 수집

`ANALYZE`는 데이터베이스에 대한 통계를 수집합니다.

```sql
ANALYZE [ ( option [, ...] ) ] [ table_and_columns [, ...] ]
ANALYZE [ VERBOSE ] [ table_and_columns [, ...] ]
```

##### 예제

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

#### 5.3 VACUUM - 가비지 컬렉션

`VACUUM`은 데드 튜플을 정리하고 선택적으로 데이터베이스를 분석합니다.

```sql
VACUUM [ ( option [, ...] ) ] [ table_and_columns [, ...] ]
VACUUM [ FULL ] [ FREEZE ] [ VERBOSE ] [ ANALYZE ] [ table_and_columns [, ...] ]
```

##### 옵션

| 옵션 | 설명 |
|------|------|
| `FULL` | 테이블 전체를 다시 작성하여 공간 회수 (배타적 잠금 필요) |
| `FREEZE` | 트랜잭션 ID 래핑 방지를 위해 행 동결 |
| `VERBOSE` | 상세 진행 상황 출력 |
| `ANALYZE` | VACUUM 후 통계 업데이트 |

##### 예제

```sql
-- 전체 데이터베이스 VACUUM
VACUUM;

-- 특정 테이블 VACUUM 및 분석
VACUUM ANALYZE films;

-- FULL VACUUM (공간 회수)
VACUUM FULL films;
```

#### 5.4 COPY - 데이터 복사

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

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `FORMAT` | csv, text, binary |
| `DELIMITER` | 열 구분자 |
| `HEADER` | 첫 행을 헤더로 처리 |
| `NULL` | NULL 값의 문자열 표현 |
| `ENCODING` | 파일 인코딩 |

##### 예제

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

#### 5.5 COMMENT - 객체에 주석 추가

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

##### 예제

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

#### 5.6 SET - 런타임 매개변수 설정

```sql
SET [ SESSION | LOCAL ] configuration_parameter { TO | = } { value | 'value' | DEFAULT }
SET [ SESSION | LOCAL ] TIME ZONE { value | 'value' | LOCAL | DEFAULT }
```

##### 예제

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

#### 5.7 SHOW - 런타임 매개변수 조회

```sql
SHOW name
SHOW ALL
```

##### 예제

```sql
-- 특정 설정 조회
SHOW client_encoding;
SHOW search_path;
SHOW timezone;

-- 모든 설정 조회
SHOW ALL;
```

#### 5.8 LISTEN/NOTIFY/UNLISTEN - 알림

##### LISTEN - 알림 대기

```sql
LISTEN channel
```

##### NOTIFY - 알림 보내기

```sql
NOTIFY channel [ , 'payload' ]
```

##### UNLISTEN - 알림 대기 중지

```sql
UNLISTEN { channel | * }
```

##### 예제

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

### 부록: SQL 명령어 분류 요약

#### A. 전체 SQL 명령어 목록

##### 데이터 조작 (DML)

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

##### 테이블 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE TABLE` | 새 테이블 정의 |
| `CREATE TABLE AS` | 쿼리 결과로 새 테이블 정의 |
| `ALTER TABLE` | 테이블 정의 변경 |
| `DROP TABLE` | 테이블 삭제 |
| `CLUSTER` | 인덱스에 따라 테이블 클러스터링 |
| `LOCK` | 테이블 잠금 |

##### 인덱스 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE INDEX` | 새 인덱스 정의 |
| `ALTER INDEX` | 인덱스 정의 변경 |
| `DROP INDEX` | 인덱스 삭제 |
| `REINDEX` | 인덱스 재구성 |

##### 뷰 작업

| 명령어 | 설명 |
|--------|------|
| `CREATE VIEW` | 새 뷰 정의 |
| `CREATE MATERIALIZED VIEW` | 새 구체화된 뷰 정의 |
| `ALTER VIEW` | 뷰 정의 변경 |
| `ALTER MATERIALIZED VIEW` | 구체화된 뷰 정의 변경 |
| `DROP VIEW` | 뷰 삭제 |
| `DROP MATERIALIZED VIEW` | 구체화된 뷰 삭제 |
| `REFRESH MATERIALIZED VIEW` | 구체화된 뷰 내용 갱신 |

##### 역할 및 권한 작업

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

##### 트랜잭션 제어

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

##### 함수 및 프로시저

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

##### 트리거 및 규칙

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

##### 유틸리티 명령어

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

### 참고 자료

- [PostgreSQL 공식 문서 - SQL Commands](https://www.postgresql.org/docs/current/sql-commands.html)
- [PostgreSQL 공식 문서 - Data Manipulation](https://www.postgresql.org/docs/current/dml.html)
- [PostgreSQL 공식 문서 - Data Definition](https://www.postgresql.org/docs/current/ddl.html)
- [PostgreSQL 공식 문서 - Queries](https://www.postgresql.org/docs/current/queries.html)

---

## PostgreSQL 클라이언트 애플리케이션 (Client Applications)

클라이언트 애플리케이션은 데이터베이스 서버가 설치된 위치와 관계없이 모든 호스트에서 실행할 수 있습니다.

---

### 목차

1. [클라이언트 애플리케이션 개요](#1-클라이언트-애플리케이션-개요)
2. [psql - 대화형 터미널](#2-psql---대화형-터미널)
3. [pg_dump - 데이터베이스 백업](#3-pg_dump---데이터베이스-백업)
4. [pg_restore - 데이터베이스 복원](#4-pg_restore---데이터베이스-복원)
5. [pg_dumpall - 클러스터 전체 백업](#5-pg_dumpall---클러스터-전체-백업)
6. [pg_basebackup - 기본 백업](#6-pg_basebackup---기본-백업)
7. [createdb - 데이터베이스 생성](#7-createdb---데이터베이스-생성)
8. [dropdb - 데이터베이스 삭제](#8-dropdb---데이터베이스-삭제)
9. [createuser - 사용자 생성](#9-createuser---사용자-생성)
10. [dropuser - 사용자 삭제](#10-dropuser---사용자-삭제)
11. [vacuumdb - 가비지 컬렉션 및 분석](#11-vacuumdb---가비지-컬렉션-및-분석)
12. [reindexdb - 인덱스 재구축](#12-reindexdb---인덱스-재구축)
13. [clusterdb - 테이블 클러스터링](#13-clusterdb---테이블-클러스터링)
14. [기타 유틸리티](#14-기타-유틸리티)

---

### 1. 클라이언트 애플리케이션 개요

PostgreSQL은 다양한 클라이언트 애플리케이션을 제공하며, 이들은 데이터베이스 관리, 백업/복원, 성능 테스트 등 다양한 작업을 수행할 수 있습니다.

#### 1.1 클라이언트 애플리케이션 목록

| 애플리케이션 | 설명 |
|-------------|------|
| psql | PostgreSQL 대화형 터미널 |
| pg_dump | 데이터베이스를 SQL 스크립트 또는 아카이브 형식으로 내보내기 |
| pg_dumpall | 클러스터 전체를 스크립트 파일로 추출 |
| pg_restore | pg_dump로 생성된 아카이브에서 데이터베이스 복원 |
| pg_basebackup | PostgreSQL 클러스터의 기본 백업 수행 |
| createdb | 새 PostgreSQL 데이터베이스 생성 |
| dropdb | PostgreSQL 데이터베이스 삭제 |
| createuser | 새 PostgreSQL 사용자 계정 생성 |
| dropuser | PostgreSQL 사용자 계정 삭제 |
| vacuumdb | 가비지 컬렉션 및 분석 수행 |
| reindexdb | 데이터베이스 인덱스 재구축 |
| clusterdb | PostgreSQL 데이터베이스 클러스터링 |
| pg_isready | PostgreSQL 서버 연결 상태 확인 |
| pgbench | PostgreSQL 벤치마크 테스트 실행 |
| pg_config | 설치된 PostgreSQL 버전 정보 조회 |
| pg_receivewal | PostgreSQL 서버에서 WAL 스트리밍 |
| pg_recvlogical | PostgreSQL 논리적 디코딩 스트림 제어 |
| pg_verifybackup | 기본 백업의 무결성 검증 |
| pg_amcheck | 데이터베이스 손상 검사 |
| ecpg | 임베디드 SQL C 전처리기 |

#### 1.2 공통 연결 옵션

대부분의 클라이언트 애플리케이션은 다음과 같은 공통 연결 옵션을 지원합니다:

| 옵션 | 설명 |
|------|------|
| `-h, --host=HOST` | 데이터베이스 서버 호스트명 |
| `-p, --port=PORT` | 데이터베이스 서버 포트 (기본값: 5432) |
| `-U, --username=USER` | 연결할 사용자명 |
| `-d, --dbname=DBNAME` | 데이터베이스 이름 |
| `-W, --password` | 비밀번호 프롬프트 강제 |
| `-w, --no-password` | 비밀번호 프롬프트 표시 안 함 |

#### 1.3 환경 변수 (Environment Variables)

다음 환경 변수를 사용하여 기본 연결 매개변수를 설정할 수 있습니다:

```bash
# 기본 연결 매개변수
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=mydb
export PGUSER=postgres
export PGPASSWORD=secret  # 보안상 권장하지 않음

# 비밀번호 파일 사용 (권장)
export PGPASSFILE=~/.pgpass
```

---

### 2. psql - 대화형 터미널

psql은 PostgreSQL의 터미널 기반 프론트엔드로, SQL 쿼리를 대화형으로 입력하고 실행 결과를 확인할 수 있습니다.

#### 2.1 기본 구문 (Syntax)

```bash
psql [option]... [dbname [username]]
```

#### 2.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-d dbname` | 연결할 데이터베이스 이름 |
| `-h hostname` | 서버 호스트명 |
| `-p port` | 서버 포트 (기본값: 5432) |
| `-U username` | 데이터베이스 사용자 |
| `-c command` | 명령 문자열 실행 |
| `-f filename` | 파일에서 명령 실행 |
| `-l, --list` | 모든 데이터베이스 목록 표시 후 종료 |
| `-a, --echo-all` | 모든 입력 라인을 stdout으로 출력 |
| `-e, --echo-queries` | 서버로 전송된 SQL 명령 에코 |
| `-q, --quiet` | 정보 메시지 억제 |
| `-t, --tuples-only` | 테이블 데이터만 출력 (헤더 제외) |
| `-A, --no-align` | 정렬되지 않은 출력 모드 |
| `-H, --html` | HTML 출력 형식 |
| `-x, --expanded` | 확장된 테이블 포맷 |

#### 2.3 연결 예제

```bash
# 로컬 데이터베이스에 연결
psql -d mydb

# 특정 호스트와 포트로 연결
psql -h localhost -p 5432 -U postgres -d mydb

# 연결 문자열 사용
psql "host=localhost port=5432 dbname=mydb user=postgres"

# URI 형식 연결 문자열
psql postgresql://user:password@host:5432/dbname?sslmode=require

# 서비스 파일 사용
psql "service=myservice sslmode=require"
```

#### 2.4 SQL 명령 실행

```bash
# 단일 명령 실행
psql -c "SELECT * FROM users;"

# 여러 명령 실행
psql -c '\x' -c 'SELECT * FROM users;'

# 파일에서 명령 실행
psql -f script.sql

# 표준 입력에서 명령 실행
psql < commands.sql

# 모든 데이터베이스 목록 표시
psql -l
```

#### 2.5 메타 명령 (Meta-Commands)

psql에서 백슬래시(`\`)로 시작하는 메타 명령을 사용하여 다양한 작업을 수행할 수 있습니다:

##### 2.5.1 정보 조회 명령

| 명령 | 설명 |
|------|------|
| `\l` | 데이터베이스 목록 |
| `\dt` | 테이블 목록 |
| `\dt+` | 테이블 목록 (상세) |
| `\d tablename` | 테이블 구조 설명 |
| `\d+ tablename` | 테이블 구조 설명 (상세) |
| `\di` | 인덱스 목록 |
| `\dv` | 뷰 목록 |
| `\df` | 함수 목록 |
| `\dn` | 스키마 목록 |
| `\du` | 역할/사용자 목록 |
| `\dp` | 테이블 권한 목록 |
| `\dx` | 설치된 확장 목록 |

##### 2.5.2 연결 및 실행 명령

| 명령 | 설명 |
|------|------|
| `\c dbname` | 다른 데이터베이스에 연결 |
| `\conninfo` | 현재 연결 정보 표시 |
| `\e` | 외부 편집기에서 쿼리 편집 |
| `\i filename` | 파일에서 명령 실행 |
| `\ir filename` | 상대 경로로 파일에서 명령 실행 |
| `\o filename` | 출력을 파일로 리다이렉션 |
| `\copy` | 클라이언트 측 COPY 명령 |
| `\! command` | 쉘 명령 실행 |
| `\q` | psql 종료 |

##### 2.5.3 출력 형식 명령

| 명령 | 설명 |
|------|------|
| `\x` | 확장 모드 토글 |
| `\x on` | 확장 모드 켜기 |
| `\x off` | 확장 모드 끄기 |
| `\x auto` | 확장 모드 자동 |
| `\pset format FORMAT` | 출력 형식 설정 |
| `\pset border N` | 테두리 스타일 설정 |
| `\pset null 'NULL'` | NULL 값 표시 문자열 설정 |
| `\timing` | 쿼리 실행 시간 표시 토글 |

#### 2.6 출력 형식 설정

```sql
-- 정렬된 출력 (기본값)
\pset format aligned

-- CSV 형식
\pset format csv

-- 정렬되지 않은 형식
\pset format unaligned
\pset fieldsep ','

-- HTML 형식
\pset format html

-- 확장 모드 (세로 표시)
\x on

-- LaTeX 형식
\pset format latex
```

#### 2.7 변수와 보간 (Variables and Interpolation)

```sql
-- 변수 설정
\set foo 'my_table'

-- 변수 사용 (인용 없음)
SELECT * FROM :foo;

-- 식별자로 인용
SELECT * FROM :"foo";

-- SQL 리터럴로 인용
SELECT * FROM :'foo';

-- 조건문 사용
\if :myvar
    SELECT 'Variable is set';
\else
    SELECT 'Variable is not set';
\endif

-- 현재 설정된 모든 변수 표시
\set
```

#### 2.8 COPY 명령 사용

```sql
-- 테이블을 CSV 파일로 내보내기
\copy users TO '/tmp/users.csv' WITH CSV HEADER;

-- CSV 파일에서 테이블로 가져오기
\copy users FROM '/tmp/users.csv' WITH CSV HEADER;

-- 특정 컬럼만 내보내기
\copy users(id, name, email) TO '/tmp/users.csv' WITH CSV HEADER;

-- 쿼리 결과 내보내기
\copy (SELECT * FROM users WHERE active = true) TO '/tmp/active_users.csv' WITH CSV HEADER;
```

#### 2.9 종료 코드 (Exit Codes)

| 코드 | 설명 |
|------|------|
| `0` | 성공적 완료 |
| `1` | 치명적 오류 |
| `2` | 연결 실패 (비대화형 모드) |
| `3` | `ON_ERROR_STOP` 설정 시 스크립트 오류 |

#### 2.10 환경 변수

| 변수 | 설명 |
|------|------|
| `PGDATABASE` | 기본 데이터베이스 이름 |
| `PGHOST` | 기본 호스트명 |
| `PGPORT` | 기본 포트 |
| `PGUSER` | 기본 사용자명 |
| `PSQL_EDITOR` | `\e` 명령에 사용할 편집기 |
| `PAGER` | 출력을 위한 페이저 프로그램 |
| `PSQLRC` | 사용자 .psqlrc 파일 위치 |

#### 2.11 실용적인 예제

```bash
# 데이터베이스 정보 빠르게 확인
psql -d mydb -c "\dt" -c "\du"

# 쿼리 결과를 CSV로 저장
psql -d mydb -A -F',' -c "SELECT * FROM users" > users.csv

# 스크립트 실행 및 오류 시 중단
psql -d mydb -v ON_ERROR_STOP=1 -f migration.sql

# 특정 테이블 구조 확인
psql -d mydb -c "\d+ users"

# 대용량 쿼리 결과를 파일로 저장
psql -d mydb -o result.txt -c "SELECT * FROM large_table"
```

---

### 3. pg_dump - 데이터베이스 백업

pg_dump는 PostgreSQL 데이터베이스를 SQL 스크립트 또는 아카이브 파일로 내보내는 유틸리티입니다. 데이터베이스가 활발히 사용되는 중에도 일관된 백업을 생성하며, 다른 사용자의 접근을 차단하지 않습니다.

#### 3.1 기본 구문 (Syntax)

```bash
pg_dump [connection-option...] [option...] [dbname]
```

#### 3.2 주요 기능

- 논블로킹 (Non-blocking): 동시 읽기/쓰기 접근을 방해하지 않음
- 다양한 형식: 일반 텍스트 SQL, 커스텀 아카이브, 디렉토리, tar 형식 지원
- 유연한 복원: 데이터베이스 객체를 선택적으로 복원 가능
- 병렬 덤프: 디렉토리 형식에서 병렬 처리 지원

#### 3.3 출력 형식 옵션

| 형식 | 옵션 | 설명 |
|------|------|------|
| Plain | `-Fp` | SQL 텍스트 파일, 이식성이 좋지만 복원이 느림 |
| Custom | `-Fc` | 압축됨, 선택적 복원, 재정렬 가능 |
| Directory | `-Fd` | 병렬 덤프 및 복원 지원 |
| Tar | `-Ft` | 디렉토리 형식과 호환, 압축 없음 |

#### 3.4 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --data-only` | 데이터만 덤프, 스키마 제외 |
| `-s, --schema-only` | 스키마만 덤프, 데이터 제외 |
| `-c, --clean` | CREATE 문 전에 DROP 명령 추가 |
| `-C, --create` | CREATE DATABASE 명령 포함 |
| `-F format` | 출력 형식: p (plain), c (custom), d (directory), t (tar) |
| `-j njobs` | 병렬 덤프 (디렉토리 형식만) |
| `-t pattern` | 특정 테이블 덤프 |
| `-T pattern` | 특정 테이블 제외 |
| `-n pattern` | 특정 스키마 덤프 |
| `-N pattern` | 특정 스키마 제외 |
| `-v, --verbose` | 상세 출력 |
| `-Z level` | 압축 수준 (0-9) |
| `--inserts` | COPY 대신 INSERT 명령 사용 |
| `--column-inserts` | 컬럼 이름이 포함된 INSERT 명령 사용 |
| `--if-exists` | DROP 명령에 IF EXISTS 추가 |
| `--no-owner` | 소유권 복원 명령 생략 |
| `--no-privileges` | 권한 복원 명령 생략 |

#### 3.5 기본 사용 예제

```bash
# 간단한 SQL 스크립트 덤프
pg_dump mydb > mydb_backup.sql

# 커스텀 아카이브 형식 (압축)
pg_dump -Fc mydb > mydb_backup.dump

# 디렉토리 형식 (병렬 덤프)
pg_dump -Fd -j 4 mydb -f mydb_backup_dir

# tar 형식
pg_dump -Ft mydb > mydb_backup.tar
```

#### 3.6 선택적 덤프 예제

```bash
# 특정 테이블만 덤프
pg_dump -t users mydb > users.sql

# 특정 테이블 제외하고 덤프
pg_dump -T logs mydb > mydb_without_logs.sql

# 패턴과 일치하는 테이블 덤프
pg_dump -t 'emp*' mydb > emp_tables.sql

# 여러 테이블 덤프
pg_dump -t users -t orders -t products mydb > selected_tables.sql

# 특정 스키마 덤프
pg_dump -n public mydb > public_schema.sql

# 여러 스키마 덤프
pg_dump -n 'east*' -n 'west*' mydb > multi_schema.sql

# 데이터만 덤프 (스키마 제외)
pg_dump -a mydb > data_only.sql

# 스키마만 덤프 (데이터 제외)
pg_dump -s mydb > schema_only.sql
```

#### 3.7 고급 사용 예제

```bash
# 압축된 덤프
pg_dump -Z 9 mydb > mydb_compressed.sql.gz

# 병렬 덤프 (5개 작업)
pg_dump -Fd -j 5 mydb -f /backup/mydb

# INSERT 문으로 덤프 (다른 DBMS와 호환성)
pg_dump --inserts mydb > mydb_inserts.sql

# 컬럼 이름 포함 INSERT
pg_dump --column-inserts mydb > mydb_column_inserts.sql

# 소유권 및 권한 제외
pg_dump --no-owner --no-privileges mydb > mydb_portable.sql

# DROP IF EXISTS 포함
pg_dump -c --if-exists mydb > mydb_with_drop.sql

# 필터 파일 사용
pg_dump --filter=filter.txt mydb > filtered.sql
```

#### 3.8 필터 파일 예제

`filter.txt` 파일:
```
include table users*
include table orders
exclude table user_sessions
exclude table user_logs
```

#### 3.9 복원 예제

```bash
# SQL 스크립트에서 복원
psql newdb < mydb_backup.sql

# 커스텀 아카이브에서 복원
pg_restore -d newdb mydb_backup.dump

# 디렉토리 형식에서 복원
pg_restore -d newdb mydb_backup_dir

# tar 형식에서 복원
pg_restore -d newdb mydb_backup.tar
```

#### 3.10 중요 참고사항

- 보안: 일반 텍스트 덤프는 임의의 슈퍼유저 코드를 실행할 수 있습니다. 신뢰할 수 없는 소스의 덤프는 복원 전에 검사하세요.
- 프로덕션 백업: 정기 프로덕션 백업에는 pg_basebackup 또는 WAL 아카이빙을 권장합니다.
- 전역 객체: 역할, 테이블스페이스 등 클러스터 전체 객체는 pg_dumpall을 사용하세요.

---

### 4. pg_restore - 데이터베이스 복원

pg_restore는 pg_dump로 생성된 아카이브 파일(커스텀, 디렉토리, tar 형식)에서 PostgreSQL 데이터베이스를 복원하는 유틸리티입니다.

#### 4.1 기본 구문 (Syntax)

```bash
pg_restore [connection-option...] [option...] [filename]
```

#### 4.2 주요 기능

- 선택적 복원: 특정 스키마, 테이블, 함수, 트리거 선택 가능
- 병렬 처리: `-j` 플래그로 여러 작업 동시 실행
- 형식 지원: 커스텀, 디렉토리, tar 아카이브 형식
- 스크립트 생성: 직접 복원 대신 SQL 스크립트 생성 가능
- 유연한 출력: 내용 목록 표시, 항목 재정렬, 필터링

#### 4.3 핵심 옵션

| 옵션 | 설명 |
|------|------|
| `-d, --dbname=dbname` | 복원할 대상 데이터베이스 |
| `-a, --data-only` | 데이터만 복원, 스키마 제외 |
| `-s, --schema-only` | 스키마만 복원, 데이터 제외 |
| `-c, --clean` | 재생성 전에 객체 삭제 |
| `-C, --create` | 복원 전 데이터베이스 생성 |
| `-1, --single-transaction` | 모든 명령을 BEGIN/COMMIT으로 감싸기 |
| `-j, --jobs=N` | N개의 동시 작업 실행 |
| `-f, --file=filename` | 스크립트를 위한 출력 파일 |
| `-l, --list` | 아카이브 내용 목록 표시 |
| `-L, --use-list=file` | 목록 파일에서 항목 복원 |
| `-v, --verbose` | 상세 출력 |

#### 4.4 필터링 옵션

| 옵션 | 설명 |
|------|------|
| `-n, --schema=schema` | 특정 스키마 복원 |
| `-N, --exclude-schema=schema` | 특정 스키마 제외 |
| `-t, --table=table` | 특정 테이블 복원 |
| `-I, --index=index` | 특정 인덱스 복원 |
| `-P, --function=func(args)` | 특정 함수 복원 |
| `-T, --trigger=trigger` | 특정 트리거 복원 |
| `--filter=filename` | 파일에서 필터 패턴 읽기 |

#### 4.5 기본 복원 예제

```bash
# 커스텀 아카이브에서 복원
pg_restore -d newdb mydb_backup.dump

# 디렉토리 형식에서 복원
pg_restore -d newdb mydb_backup_dir

# tar 형식에서 복원
pg_restore -d newdb mydb_backup.tar
```

#### 4.6 데이터베이스 삭제 후 재생성

```bash
# 데이터베이스 삭제 후 재생성하며 복원
pg_restore -c -C -d postgres mydb_backup.dump
```

#### 4.7 새 데이터베이스로 복원

```bash
# 빈 템플릿에서 새 데이터베이스 생성
createdb -T template0 newdb

# 복원 수행
pg_restore -d newdb mydb_backup.dump
```

#### 4.8 선택적 복원 예제

```bash
# 특정 테이블만 복원
pg_restore -d mydb -t users mydb_backup.dump

# 특정 스키마만 복원
pg_restore -d mydb -n public mydb_backup.dump

# 데이터만 복원 (스키마 제외)
pg_restore -a -d mydb mydb_backup.dump

# 스키마만 복원 (데이터 제외)
pg_restore -s -d mydb mydb_backup.dump
```

#### 4.9 병렬 복원

```bash
# 4개의 병렬 작업으로 복원
pg_restore -j 4 -d mydb mydb_backup.dump

# 디렉토리 형식에서 8개의 병렬 작업
pg_restore -j 8 -d mydb mydb_backup_dir
```

#### 4.10 아카이브 내용 확인 및 재정렬

```bash
# 아카이브 내용 목록 표시
pg_restore -l mydb_backup.dump > contents.txt

# 목록 파일 편집 (주석 처리하거나 순서 변경)
# 편집된 목록으로 복원
pg_restore -L contents.txt -d mydb mydb_backup.dump
```

#### 4.11 SQL 스크립트로 변환

```bash
# 아카이브를 SQL 스크립트로 변환
pg_restore mydb_backup.dump > restore_script.sql

# 특정 파일로 출력
pg_restore -f restore_script.sql mydb_backup.dump
```

#### 4.12 트랜잭션 옵션

```bash
# 단일 트랜잭션으로 복원 (실패 시 전체 롤백)
pg_restore -1 -d mydb mydb_backup.dump

# 트랜잭션 크기 지정 (대용량 복원 시)
pg_restore --transaction-size=1000 -d mydb mydb_backup.dump
```

#### 4.13 모범 사례 (Best Practices)

1. 새 데이터베이스는 `template0`에서 생성 (빈 상태 보장)
2. 원자성을 위해 `--single-transaction` 사용 (대용량에서는 잠금 제한 주의)
3. 대용량 복원 시 `--transaction-size=N` 사용
4. 복원 후 통계가 완전히 복원되지 않았으면 `ANALYZE` 실행
5. 멀티프로세서 시스템에서 복원 속도를 높이려면 `--jobs=N` 사용

#### 4.14 보안 경고

복원은 임의의 코드를 실행합니다. 신뢰할 수 없는 소스의 슈퍼유저가 작성한 덤프라면 `pg_restore --file`을 사용하여 SQL 문을 먼저 검사하세요.

---

### 5. pg_dumpall - 클러스터 전체 백업

pg_dumpall은 PostgreSQL 데이터베이스 클러스터 전체를 SQL 스크립트 파일로 추출합니다. 모든 데이터베이스, 역할, 테이블스페이스 등 클러스터 수준의 객체를 포함합니다.

#### 5.1 기본 구문 (Syntax)

```bash
pg_dumpall [connection-option...] [option...]
```

#### 5.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-g, --globals-only` | 전역 객체만 덤프 (역할, 테이블스페이스) |
| `-r, --roles-only` | 역할만 덤프 |
| `-t, --tablespaces-only` | 테이블스페이스만 덤프 |
| `-c, --clean` | 재생성 전 데이터베이스 객체 삭제 |
| `--if-exists` | DROP 명령에 IF EXISTS 추가 |
| `-s, --schema-only` | 스키마만 덤프, 데이터 제외 |
| `-a, --data-only` | 데이터만 덤프, 스키마 제외 |
| `--no-role-passwords` | 역할 비밀번호 제외 |
| `-v, --verbose` | 상세 출력 |

#### 5.3 사용 예제

```bash
# 전체 클러스터 백업
pg_dumpall > full_backup.sql

# 전역 객체만 백업 (역할, 테이블스페이스)
pg_dumpall -g > globals.sql

# 역할만 백업
pg_dumpall -r > roles.sql

# DROP 문 포함
pg_dumpall -c > full_backup_with_drop.sql

# 원격 서버에서 백업
pg_dumpall -h remotehost -U postgres > remote_backup.sql
```

#### 5.4 복원 예제

```bash
# 전체 복원
psql -f full_backup.sql postgres

# 또는
psql postgres < full_backup.sql
```

#### 5.5 pg_dump와의 차이점

| 항목 | pg_dump | pg_dumpall |
|------|---------|------------|
| 범위 | 단일 데이터베이스 | 전체 클러스터 |
| 역할 백업 | 불가 | 가능 |
| 테이블스페이스 | 불가 | 가능 |
| 출력 형식 | 다양 (plain, custom, directory, tar) | plain만 가능 |
| 병렬 덤프 | 가능 (directory 형식) | 불가 |
| 선택적 복원 | 가능 | 제한적 |

---

### 6. pg_basebackup - 기본 백업

pg_basebackup은 실행 중인 PostgreSQL 데이터베이스 클러스터의 기본 백업을 수행하는 유틸리티입니다. PITR(Point-In-Time Recovery) 및 스탠바이 서버 설정에 필수적입니다.

#### 6.1 기본 구문 (Syntax)

```bash
pg_basebackup [option...]
```

#### 6.2 주요 기능

- 전체 또는 증분 백업: 데이터베이스 클러스터의 정확한 복사본 또는 수정된 블록만 포함하는 증분 버전 생성
- 논블로킹 (Non-disruptive): 다른 데이터베이스 클라이언트에 영향을 주지 않음
- 복제 프로토콜: 복제 권한이 있는 일반 PostgreSQL 연결 사용
- 다양한 형식: 일반 파일 또는 tar 형식 출력 지원

#### 6.3 요구사항

- 사용자는 `REPLICATION` 권한 또는 슈퍼유저여야 함
- `pg_hba.conf`에서 복제 연결을 허용해야 함
- 서버의 `max_wal_senders`가 백업과 WAL 스트리밍을 위해 충분히 설정되어야 함

#### 6.4 주요 옵션

##### 6.4.1 출력 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory` | 대상 디렉토리 (필수) |
| `-F format` | 출력 형식: 'p' (plain) 또는 't' (tar) |
| `-T olddir=newdir` | 테이블스페이스 재배치 |

##### 6.4.2 WAL 처리 옵션

| 옵션 | 설명 |
|------|------|
| `-X method` | WAL 수집: 'none', 'fetch', 또는 'stream' (기본값) |
| `-S slotname` | 특정 복제 슬롯 사용 |
| `--waldir=path` | WAL 디렉토리 위치 지정 |

##### 6.4.3 압축 옵션

| 옵션 | 설명 |
|------|------|
| `-z, --gzip` | gzip 압축 활성화 |
| `-Z method` | 압축 방식 지정: gzip, lz4, zstd, none |

##### 6.4.4 성능 및 보고 옵션

| 옵션 | 설명 |
|------|------|
| `-c {fast\|spread}` | 체크포인트 모드 (기본값: spread) |
| `-P, --progress` | 진행 상황 보고 활성화 |
| `-r rate` | 최대 전송 속도 (KB/s) |
| `-N, --no-sync` | 파일 동기화 건너뛰기 (빠르지만 위험) |

#### 6.5 사용 예제

```bash
# 로컬 디렉토리로 기본 백업
pg_basebackup -D /usr/local/pgsql/backup

# 진행 상황 표시와 함께 압축된 tar 백업
pg_basebackup -D backup -F t -z -P

# 테이블스페이스 재배치와 함께 백업
pg_basebackup -D /backup -T /opt/ts=./backup/ts

# 원격 서버에서 gzip 압축으로 백업
pg_basebackup -h mydbserver -D backup -F t -z

# 빠른 체크포인트로 백업
pg_basebackup -D backup -c fast -P

# 전송 속도 제한
pg_basebackup -D backup -r 10240 -P

# 복제 슬롯 사용
pg_basebackup -D backup -S myslot -P
```

#### 6.6 스탠바이 서버 설정

```bash
# 스탠바이 서버용 백업
pg_basebackup -D /var/lib/postgresql/standby \
    -h primary.example.com \
    -U replicator \
    -P \
    -R  # standby.signal 파일 및 연결 정보 자동 생성
```

#### 6.7 중요 참고사항

- 시작 시 체크포인트가 필요함 (시간이 걸릴 수 있음)
- PostgreSQL 9.1+ 이상에서 동작 (일부 기능은 더 최신 버전 필요)
- WAL 스트리밍 (`-X stream`)은 서버 9.3+ 필요
- tar 형식은 서버 9.5+ 필요
- 증분 백업은 서버 17+ 필요
- 백업 매니페스트가 기본적으로 생성되어 `pg_verifybackup`으로 무결성 검증 가능

---

### 7. createdb - 데이터베이스 생성

createdb는 새 PostgreSQL 데이터베이스를 생성하는 유틸리티입니다. SQL `CREATE DATABASE` 명령의 래퍼입니다.

#### 7.1 기본 구문 (Syntax)

```bash
createdb [connection-option...] [option...] [dbname [description]]
```

#### 7.2 주요 옵션

##### 7.2.1 데이터베이스 구성 옵션

| 옵션 | 설명 |
|------|------|
| `-D, --tablespace=tablespace` | 데이터베이스의 기본 테이블스페이스 설정 |
| `-E, --encoding=encoding` | 문자 인코딩 지정 |
| `-l, --locale=locale` | 로케일 설정 (collate, ctype, icu-locale) |
| `-O, --owner=owner` | 데이터베이스 소유자 지정 |
| `-T, --template=template` | 템플릿 데이터베이스 지정 |
| `-S, --strategy=strategy` | 데이터베이스 생성 전략 설정 |

##### 7.2.2 로케일 옵션

| 옵션 | 설명 |
|------|------|
| `--lc-collate=locale` | LC_COLLATE 설정 |
| `--lc-ctype=locale` | LC_CTYPE 설정 |
| `--builtin-locale=locale` | 내장 프로바이더 로케일 |
| `--icu-locale=locale` | ICU 로케일 ID |
| `--locale-provider={builtin\|libc\|icu}` | 로케일 프로바이더 |

##### 7.2.3 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-V, --version` | 버전 출력 후 종료 |
| `-?, --help` | 도움말 표시 |

#### 7.3 사용 예제

```bash
# 기본 설정으로 데이터베이스 생성
createdb mydb

# 설명과 함께 생성
createdb mydb "My test database"

# 특정 소유자 지정
createdb -O myuser mydb

# 특정 인코딩 지정
createdb -E UTF8 mydb

# 템플릿 지정
createdb -T template0 mydb

# 원격 서버에 생성
createdb -h remotehost -p 5432 -U postgres mydb

# 로케일 지정
createdb -l ko_KR.UTF-8 mydb

# 모든 옵션 함께 사용
createdb -h localhost -p 5432 -U postgres -O myuser -E UTF8 -T template0 mydb "Description"
```

#### 7.4 SQL 동등 명령

```sql
-- createdb mydb와 동등
CREATE DATABASE mydb;

-- createdb -O myuser -E UTF8 mydb와 동등
CREATE DATABASE mydb
    OWNER = myuser
    ENCODING = 'UTF8';

-- 템플릿 지정
CREATE DATABASE mydb
    TEMPLATE = template0
    ENCODING = 'UTF8'
    LC_COLLATE = 'ko_KR.UTF-8'
    LC_CTYPE = 'ko_KR.UTF-8';
```

---

### 8. dropdb - 데이터베이스 삭제

dropdb는 기존 PostgreSQL 데이터베이스를 삭제하는 유틸리티입니다. SQL `DROP DATABASE` 명령의 래퍼입니다.

#### 8.1 기본 구문 (Syntax)

```bash
dropdb [connection-option...] [option...] dbname
```

#### 8.2 요구사항

실행 사용자는 데이터베이스 슈퍼유저이거나 데이터베이스 소유자여야 합니다.

#### 8.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 서버로 전송되는 명령 에코 |
| `-f, --force` | 삭제 전 모든 기존 연결 종료 |
| `-i, --interactive` | 삭제 전 확인 프롬프트 |
| `--if-exists` | 데이터베이스가 없어도 오류 발생하지 않음 |
| `-V, --version` | 버전 정보 표시 |
| `-?, --help` | 도움말 표시 |
| `--maintenance-db=dbname` | 삭제 시 연결할 데이터베이스 (기본값: postgres 또는 template1) |

#### 8.4 사용 예제

```bash
# 간단한 데이터베이스 삭제
dropdb mydb

# 확인 프롬프트와 함께 삭제
dropdb -i mydb

# 에코 및 원격 연결
dropdb -e -h remotehost -p 5432 -U postgres mydb

# 기존 연결 강제 종료 후 삭제
dropdb -f mydb

# 존재하지 않아도 오류 없음
dropdb --if-exists mydb
```

#### 8.5 SQL 동등 명령

```sql
-- dropdb mydb와 동등
DROP DATABASE mydb;

-- dropdb -f mydb와 동등
DROP DATABASE mydb WITH (FORCE);

-- dropdb --if-exists mydb와 동등
DROP DATABASE IF EXISTS mydb;
```

#### 8.6 주의사항

- 삭제된 데이터베이스는 복구할 수 없습니다. 삭제 전 백업을 확인하세요.
- 현재 연결된 데이터베이스는 삭제할 수 없습니다.
- `-f` 옵션은 활성 연결을 강제로 종료합니다.

---

### 9. createuser - 사용자 생성

createuser는 새 PostgreSQL 사용자 계정(역할)을 생성하는 유틸리티입니다. SQL `CREATE ROLE` 명령의 래퍼입니다.

#### 9.1 기본 구문 (Syntax)

```bash
createuser [connection-option...] [option...] [username]
```

#### 9.2 요구사항

- 슈퍼유저 또는 `CREATEROLE` 권한이 있는 사용자만 새 사용자를 생성할 수 있습니다.
- `SUPERUSER`, `REPLICATION`, `BYPASSRLS` 권한을 가진 사용자를 생성하려면 슈퍼유저로 연결해야 합니다.

#### 9.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-d, --createdb` | 사용자가 데이터베이스를 생성할 수 있도록 허용 |
| `-D, --no-createdb` | 데이터베이스 생성 금지 (기본값) |
| `-s, --superuser` | 슈퍼유저 생성 |
| `-S, --no-superuser` | 일반 사용자 생성 (기본값) |
| `-r, --createrole` | 역할 생성 허용 |
| `-R, --no-createrole` | 역할 생성 금지 (기본값) |
| `-l, --login` | 로그인 허용 (기본값) |
| `-L, --no-login` | 로그인 금지 |
| `-P, --pwprompt` | 비밀번호 프롬프트 |
| `-c, --connection-limit=N` | 최대 연결 수 설정 |
| `-v, --valid-until=TIMESTAMP` | 비밀번호 만료 날짜 설정 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `--replication` | 복제 권한 부여 |
| `--no-replication` | 복제 권한 제외 (기본값) |

#### 9.4 사용 예제

```bash
# 기본 사용자 생성
createuser joe

# 비밀번호 프롬프트와 함께 슈퍼유저 생성
createuser -s -P admin

# 데이터베이스 생성 권한을 가진 사용자
createuser -d dbcreator

# 연결 제한이 있는 사용자
createuser -c 10 limited_user

# 로그인이 불가능한 역할 (그룹용)
createuser -L mygroup

# 비밀번호 만료일 설정
createuser -P -v "2025-12-31" temp_user

# 원격 서버에 사용자 생성
createuser -h remotehost -p 5432 -U postgres -P newuser

# 복제 권한이 있는 사용자
createuser --replication replicator
```

#### 9.5 SQL 동등 명령

```sql
-- createuser joe와 동등
CREATE ROLE joe LOGIN;

-- createuser -s -P admin과 동등
CREATE ROLE admin SUPERUSER LOGIN PASSWORD 'password';

-- createuser -d dbcreator와 동등
CREATE ROLE dbcreator CREATEDB LOGIN;

-- createuser -L mygroup과 동등
CREATE ROLE mygroup NOLOGIN;

-- createuser --replication replicator와 동등
CREATE ROLE replicator REPLICATION LOGIN;
```

---

### 10. dropuser - 사용자 삭제

dropuser는 기존 PostgreSQL 사용자 계정을 삭제하는 유틸리티입니다. SQL `DROP ROLE` 명령의 래퍼입니다.

#### 10.1 기본 구문 (Syntax)

```bash
dropuser [connection-option...] [option...] [username]
```

#### 10.2 권한 요구사항

- 슈퍼유저는 모든 역할을 삭제할 수 있습니다.
- 슈퍼유저가 아닌 역할은 `CREATEROLE` 권한 또는 대상 역할에 대한 `ADMIN OPTION`을 가진 사용자만 삭제할 수 있습니다.

#### 10.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 서버로 전송되는 명령 에코 |
| `-i, --interactive` | 삭제 전 확인 프롬프트 |
| `--if-exists` | 사용자가 없어도 오류 발생하지 않음 |
| `-V, --version` | 버전 정보 표시 |
| `-?, --help` | 도움말 표시 |

#### 10.4 사용 예제

```bash
# 기본 사용자 삭제
dropuser joe

# 확인 프롬프트와 함께 삭제
dropuser -i joe

# 에코 및 원격 연결
dropuser -e -h remotehost -p 5432 -U postgres joe

# 존재하지 않아도 오류 없음
dropuser --if-exists joe
```

#### 10.5 SQL 동등 명령

```sql
-- dropuser joe와 동등
DROP ROLE joe;

-- dropuser --if-exists joe와 동등
DROP ROLE IF EXISTS joe;
```

#### 10.6 주의사항

- 객체를 소유한 사용자는 먼저 객체의 소유권을 변경하거나 객체를 삭제해야 삭제할 수 있습니다.
- 권한이 부여된 사용자는 먼저 권한을 취소해야 합니다.

```sql
-- 소유권 변경
REASSIGN OWNED BY joe TO postgres;

-- 소유한 객체 삭제
DROP OWNED BY joe;

-- 사용자 삭제
DROP ROLE joe;
```

---

### 11. vacuumdb - 가비지 컬렉션 및 분석

vacuumdb는 PostgreSQL 데이터베이스에서 가비지 컬렉션을 수행하고 쿼리 최적화를 위한 내부 통계를 생성하는 유틸리티입니다. SQL `VACUUM` 명령의 래퍼입니다.

#### 11.1 기본 구문 (Syntax)

```bash
vacuumdb [connection-option...] [option...] [-t | --table table [(column [...])]] ... [dbname | -a | --all]
vacuumdb [connection-option...] [option...] [-n | --schema schema] ... [dbname | -a | --all]
vacuumdb [connection-option...] [option...] [-N | --exclude-schema schema] ... [dbname | -a | --all]
```

#### 11.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 처리 |
| `-d, --dbname=dbname` | 데이터베이스 이름 |
| `-z, --analyze` | 옵티마이저 통계 계산 |
| `-Z, --analyze-only` | 분석만 수행 (VACUUM 없음) |
| `-f, --full` | 전체 VACUUM 수행 |
| `-F, --freeze` | 튜플을 적극적으로 프리징 |
| `-t, --table=table` | 특정 테이블 처리 |
| `-n, --schema=schema` | 특정 스키마 처리 |
| `-N, --exclude-schema=schema` | 특정 스키마 제외 |
| `-j, --jobs=njobs` | 병렬로 VACUUM/ANALYZE 실행 |
| `-P, --parallel=workers` | VACUUM용 병렬 워커 수 |
| `-v, --verbose` | 상세 정보 출력 |
| `-q, --quiet` | 진행 메시지 억제 |
| `-e, --echo` | 명령 에코 |

#### 11.3 사용 예제

```bash
# 단일 데이터베이스 정리
vacuumdb mydb

# 모든 데이터베이스 정리
vacuumdb --all

# 정리 및 분석
vacuumdb --analyze mydb

# 분석만 수행
vacuumdb --analyze-only mydb

# 전체 VACUUM
vacuumdb --full mydb

# 특정 테이블 처리
vacuumdb -t users mydb

# 특정 컬럼 분석
vacuumdb --analyze-only -t "users(name, email)" mydb

# 특정 스키마 처리
vacuumdb -n public mydb

# 병렬 처리
vacuumdb --jobs=4 mydb

# 여러 스키마 처리
vacuumdb -n foo -n bar mydb
```

#### 11.4 SQL 동등 명령

```sql
-- vacuumdb mydb와 동등
VACUUM;

-- vacuumdb --analyze mydb와 동등
VACUUM ANALYZE;

-- vacuumdb --full mydb와 동등
VACUUM FULL;

-- vacuumdb -t users mydb와 동등
VACUUM users;

-- vacuumdb --analyze-only -t users mydb와 동등
ANALYZE users;
```

#### 11.5 정기적인 유지보수

```bash
# 크론잡으로 야간 VACUUM ANALYZE
0 2 * * * vacuumdb --all --analyze --quiet

# 주간 전체 VACUUM (시스템 다운타임 필요)
0 4 * * 0 vacuumdb --all --full --quiet
```

---

### 12. reindexdb - 인덱스 재구축

reindexdb는 PostgreSQL 데이터베이스의 인덱스를 재구축하는 유틸리티입니다. SQL `REINDEX` 명령의 래퍼입니다.

#### 12.1 기본 구문 (Syntax)

```bash
reindexdb [connection-option...] [option...] [dbname | -a | --all]
```

#### 12.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 재인덱스 |
| `-d, --dbname=dbname` | 재인덱스할 데이터베이스 |
| `-S, --schema=schema` | 특정 스키마 재인덱스 |
| `-t, --table=table` | 특정 테이블 재인덱스 |
| `-i, --index=index` | 특정 인덱스 재인덱스 |
| `-s, --system` | 시스템 카탈로그만 재인덱스 |
| `--concurrently` | 동시 읽기를 허용하며 재인덱스 |
| `-j, --jobs=njobs` | 병렬로 재인덱스 명령 실행 |
| `--tablespace=tablespace` | 특정 테이블스페이스에 인덱스 재구축 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-v, --verbose` | 상세 처리 정보 출력 |
| `-q, --quiet` | 진행 메시지 억제 |

#### 12.3 사용 예제

```bash
# 단일 데이터베이스 재인덱스
reindexdb mydb

# 모든 데이터베이스 재인덱스
reindexdb --all

# 특정 테이블 재인덱스
reindexdb -t users mydb

# 특정 인덱스 재인덱스
reindexdb -i users_pkey mydb

# 특정 테이블과 인덱스 함께
reindexdb -t users -i orders_idx mydb

# 동시 접근 허용하며 재인덱스
reindexdb --concurrently mydb

# 병렬 재인덱스
reindexdb --all --jobs=4

# 시스템 카탈로그만 재인덱스
reindexdb --system mydb
```

#### 12.4 SQL 동등 명령

```sql
-- reindexdb mydb와 동등
REINDEX DATABASE mydb;

-- reindexdb -t users mydb와 동등
REINDEX TABLE users;

-- reindexdb -i users_pkey mydb와 동등
REINDEX INDEX users_pkey;

-- reindexdb --concurrently mydb와 동등
REINDEX DATABASE CONCURRENTLY mydb;

-- reindexdb --system mydb와 동등
REINDEX SYSTEM mydb;
```

#### 12.5 중요 참고사항

- `-j/--jobs` 옵션은 `--system`과 호환되지 않습니다.
- 병렬 작업에는 충분한 `max_connections` 설정이 필요합니다.
- `--concurrently` 옵션은 잠금 없이 재인덱스하지만 더 많은 리소스를 사용합니다.

---

### 13. clusterdb - 테이블 클러스터링

clusterdb는 PostgreSQL 데이터베이스의 테이블을 재클러스터링하는 유틸리티입니다. 이전에 클러스터링된 테이블을 찾아 마지막으로 사용된 인덱스로 다시 클러스터링합니다. SQL `CLUSTER` 명령의 래퍼입니다.

#### 13.1 기본 구문 (Syntax)

```bash
clusterdb [connection-option...] [option...] [--table | -t table] ... [dbname | -a | --all]
```

#### 13.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 클러스터링 |
| `-d dbname, --dbname=dbname` | 클러스터링할 데이터베이스 |
| `-t table, --table=table` | 특정 테이블만 클러스터링 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-q, --quiet` | 진행 메시지 억제 |
| `-v, --verbose` | 상세 정보 출력 |
| `-V, --version` | 버전 표시 후 종료 |
| `-?, --help` | 도움말 표시 |
| `--maintenance-db=dbname` | 모든 데이터베이스 클러스터링 시 사용할 데이터베이스 |

#### 13.3 사용 예제

```bash
# 데이터베이스의 모든 클러스터링된 테이블 재클러스터링
clusterdb mydb

# 특정 테이블만 클러스터링
clusterdb -t users mydb

# 모든 데이터베이스 클러스터링
clusterdb --all

# 상세 정보와 함께
clusterdb -v mydb
```

#### 13.4 SQL 동등 명령

```sql
-- clusterdb mydb와 동등
CLUSTER;

-- clusterdb -t users mydb와 동등
CLUSTER users;

-- 인덱스 지정 클러스터링
CLUSTER users USING users_pkey;
```

#### 13.5 참고사항

- 클러스터링은 테이블을 인덱스 순서대로 물리적으로 재정렬합니다.
- 범위 쿼리 성능을 크게 향상시킬 수 있습니다.
- 클러스터링 중에는 테이블에 배타적 잠금이 설정됩니다.
- 클러스터링되지 않은 테이블은 영향을 받지 않습니다.

---

### 14. 기타 유틸리티

#### 14.1 pg_isready - 서버 연결 상태 확인

서버의 연결 상태를 확인하는 유틸리티입니다.

```bash
# 기본 연결 확인
pg_isready

# 특정 호스트 확인
pg_isready -h localhost -p 5432

# 출력 형식 지정
pg_isready -h localhost -p 5432 -d mydb -U postgres
```

종료 코드:
- `0`: 서버가 연결을 수락 중
- `1`: 서버가 연결을 거부 중
- `2`: 응답 없음
- `3`: 연결 시도 없음 (잘못된 매개변수)

#### 14.2 pgbench - 벤치마크 테스트

PostgreSQL에서 벤치마크 테스트를 실행하는 유틸리티입니다.

```bash
# 벤치마크 데이터베이스 초기화
pgbench -i -s 10 mydb

# 기본 벤치마크 실행
pgbench -c 10 -j 2 -t 1000 mydb

# 60초 동안 실행
pgbench -c 10 -j 2 -T 60 mydb

# 사용자 정의 스크립트 사용
pgbench -f custom_script.sql mydb
```

주요 옵션:
- `-i`: 초기화 모드
- `-s scale`: 스케일 팩터
- `-c clients`: 클라이언트 수
- `-j threads`: 스레드 수
- `-t transactions`: 트랜잭션 수
- `-T seconds`: 실행 시간 (초)
- `-f script`: 사용자 정의 스크립트 파일

#### 14.3 pg_config - 설치 정보 조회

설치된 PostgreSQL 버전 정보를 조회합니다.

```bash
# 모든 정보 표시
pg_config

# 특정 정보만 표시
pg_config --bindir
pg_config --includedir
pg_config --libdir
pg_config --version
```

#### 14.4 pg_receivewal - WAL 스트리밍

서버에서 WAL을 스트리밍하여 저장합니다.

```bash
# WAL 수신 및 저장
pg_receivewal -D /path/to/wal_archive -h primary_host

# 복제 슬롯 사용
pg_receivewal -D /path/to/wal_archive -S myslot -h primary_host
```

#### 14.5 pg_verifybackup - 백업 무결성 검증

pg_basebackup으로 생성된 백업의 무결성을 검증합니다.

```bash
# 백업 검증
pg_verifybackup /path/to/backup

# 상세 출력
pg_verifybackup -e /path/to/backup
```

#### 14.6 pg_amcheck - 데이터베이스 손상 검사

데이터베이스의 손상을 검사합니다.

```bash
# 전체 데이터베이스 검사
pg_amcheck mydb

# 특정 테이블 검사
pg_amcheck -t users mydb

# 상세 출력
pg_amcheck -v mydb
```

---

### 요약

PostgreSQL 클라이언트 애플리케이션은 데이터베이스 관리, 백업/복원, 성능 테스트 등 다양한 작업을 수행할 수 있는 강력한 도구들입니다.

#### 자주 사용되는 명령 요약

| 작업 | 명령 |
|------|------|
| 대화형 SQL 실행 | `psql -d dbname` |
| 데이터베이스 백업 | `pg_dump -Fc dbname > backup.dump` |
| 데이터베이스 복원 | `pg_restore -d dbname backup.dump` |
| 전체 클러스터 백업 | `pg_dumpall > full_backup.sql` |
| 기본 백업 | `pg_basebackup -D /backup -P` |
| 데이터베이스 생성 | `createdb dbname` |
| 데이터베이스 삭제 | `dropdb dbname` |
| 사용자 생성 | `createuser username` |
| 사용자 삭제 | `dropuser username` |
| 가비지 컬렉션 | `vacuumdb --analyze dbname` |
| 인덱스 재구축 | `reindexdb dbname` |
| 테이블 클러스터링 | `clusterdb dbname` |

#### 모범 사례

1. 정기 백업: pg_dump 또는 pg_basebackup을 사용하여 정기적으로 백업
2. 유지보수: vacuumdb와 reindexdb를 정기적으로 실행
3. 모니터링: pg_isready로 서버 상태 확인
4. 보안: 비밀번호 파일(.pgpass) 사용, 환경 변수에 비밀번호 저장 지양
5. 테스트: pgbench로 성능 테스트 수행

---

### 참고 자료

- [PostgreSQL 공식 문서 - Client Applications](https://www.postgresql.org/docs/current/reference-client.html)
- [PostgreSQL 공식 문서 - psql](https://www.postgresql.org/docs/current/app-psql.html)
- [PostgreSQL 공식 문서 - pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html)
- [PostgreSQL 공식 문서 - pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html)
- [PostgreSQL 공식 문서 - pg_basebackup](https://www.postgresql.org/docs/current/app-pgbasebackup.html)

---

## PostgreSQL 서버 애플리케이션 (Server Applications)

PostgreSQL 서버 애플리케이션은 데이터베이스 클러스터의 초기화, 시작, 중지, 업그레이드 및 복구 등을 수행하는 명령줄 도구들입니다.

---

### 목차

1. [initdb - 데이터베이스 클러스터 초기화](#1-initdb---데이터베이스-클러스터-초기화)
2. [pg_ctl - PostgreSQL 서버 제어](#2-pg_ctl---postgresql-서버-제어)
3. [postgres - PostgreSQL 데이터베이스 서버](#3-postgres---postgresql-데이터베이스-서버)
4. [pg_upgrade - PostgreSQL 버전 업그레이드](#4-pg_upgrade---postgresql-버전-업그레이드)
5. [pg_resetwal - WAL 및 제어 정보 재설정](#5-pg_resetwal---wal-및-제어-정보-재설정)
6. [pg_rewind - 데이터 디렉토리 동기화](#6-pg_rewind---데이터-디렉토리-동기화)

---

### 1. initdb - 데이터베이스 클러스터 초기화

#### 개요

`initdb`는 새로운 PostgreSQL 데이터베이스 클러스터(Database Cluster)를 생성하는 도구입니다. 데이터베이스 클러스터는 단일 서버 인스턴스에서 관리하는 데이터베이스들의 모음입니다.

#### 기본 문법

```bash
initdb [option...] [--pgdata | -D] directory
```

#### 주요 기능

`initdb`는 다음 작업을 수행합니다:

- 클러스터 데이터가 저장될 디렉토리 생성
- 공유 카탈로그 테이블(Shared Catalog Tables) 생성
- 기본 데이터베이스 생성:
  - postgres: 사용자, 유틸리티, 서드파티 애플리케이션용 기본 데이터베이스
  - template1: 이후 `CREATE DATABASE` 명령어로 복사될 소스 데이터베이스
  - template0: 수정되면 안 되는 원본 템플릿 데이터베이스

#### 중요 사항

- 소유권(Ownership): `initdb`를 실행한 OS 사용자가 서버 프로세스 소유자가 됨
- 권한(Permission): root로 실행 불가
- 보안(Security): 기본적으로 클러스터 소유자만 접근 가능

#### 주요 옵션

##### 필수 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory`, `--pgdata=directory` | 데이터베이스 클러스터가 저장될 디렉토리 (`PGDATA` 환경변수로도 설정 가능) |

##### 인증 관련 (Authentication)

| 옵션 | 설명 |
|------|------|
| `-A authmethod`, `--auth=authmethod` | 로컬 사용자의 기본 인증 방법 (`pg_hba.conf`에 사용) |
| `--auth-host=authmethod` | TCP/IP 연결의 인증 방법 |
| `--auth-local=authmethod` | Unix 도메인 소켓 연결의 인증 방법 |

##### 인코딩 및 로케일 (Encoding & Locale)

| 옵션 | 설명 |
|------|------|
| `-E encoding`, `--encoding=encoding` | 템플릿 데이터베이스의 인코딩 지정 |
| `--locale=locale` | 클러스터의 기본 로케일 설정 |
| `--lc-collate=locale` | 정렬 순서 로케일 |
| `--lc-ctype=locale` | 문자 분류 로케일 |
| `--lc-messages=locale` | 메시지 로케일 |
| `--locale-provider={builtin|libc|icu}` | 로케일 제공자 설정 (기본값: libc) |
| `--icu-locale=locale` | ICU 제공자 사용 시 ICU 로케일 지정 |
| `--no-locale` | `--locale=C`와 동일 |

##### 슈퍼유저 설정 (Superuser)

| 옵션 | 설명 |
|------|------|
| `-U username`, `--username=username` | 부트스트랩 슈퍼유저 이름 (기본값: OS 사용자명) |
| `-W`, `--pwprompt` | 슈퍼유저 비밀번호 프롬프트 |
| `--pwfile=filename` | 파일에서 슈퍼유저 비밀번호 읽기 |

##### 데이터 보호 (Data Protection)

| 옵션 | 설명 |
|------|------|
| `-k`, `--data-checksums` | I/O 시스템 손상 감지를 위한 체크섬 활성화 (기본값: 활성화) |
| `--no-data-checksums` | 체크섬 비활성화 |

##### WAL 관련 (Write-Ahead Log)

| 옵션 | 설명 |
|------|------|
| `-X directory`, `--waldir=directory` | WAL 저장 디렉토리 지정 |
| `--wal-segsize=size` | WAL 세그먼트 크기 설정 (1~1024 MB, 2의 거듭제곱, 기본값: 16 MB) |

##### 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-g`, `--allow-group-access` | 클러스터 소유자와 같은 그룹의 사용자가 파일 읽기 가능 |
| `-T config`, `--text-search-config=config` | 기본 텍스트 검색 구성 설정 |
| `-c name=value`, `--set name=value` | 서버 파라미터 강제 설정 |
| `-N`, `--no-sync` | 파일 동기화 대기 생략 (테스트용) |
| `-s`, `--show` | 내부 설정 표시 후 종료 |

#### 환경 변수 (Environment Variables)

| 변수 | 설명 |
|------|------|
| `PGDATA` | 데이터베이스 클러스터 저장 디렉토리 (`-D`로 오버라이드 가능) |
| `PG_COLOR` | 진단 메시지 색상 사용 여부 (`always`, `auto`, `never`) |
| `TZ` | 생성된 클러스터의 기본 시간대 |

#### 사용 예제

##### 기본 초기화

```bash
initdb -D /var/lib/postgresql/data
```

##### 슈퍼유저 이름 및 비밀번호 지정

```bash
initdb -D /var/lib/postgresql/data -U postgres -W
```

##### 인코딩 및 로케일 설정 (한국어)

```bash
initdb -D /var/lib/postgresql/data -E UTF8 --locale=ko_KR.UTF-8
```

##### ICU 로케일 제공자 사용

```bash
initdb -D /var/lib/postgresql/data --locale-provider=icu --icu-locale=ko
```

##### WAL 디렉토리 별도 지정

```bash
initdb -D /var/lib/postgresql/data -X /var/lib/postgresql/wal
```

##### 서버 파라미터 설정

```bash
initdb -D /var/lib/postgresql/data -c max_connections=200 -c shared_buffers=1GB
```

---

### 2. pg_ctl - PostgreSQL 서버 제어

#### 개요

`pg_ctl`은 PostgreSQL 데이터베이스 서버를 초기화, 시작, 중지, 제어 및 상태 확인하는 유틸리티입니다.

#### 명령 형식

```bash
pg_ctl init[db] [-D datadir] [-s] [-o initdb-options]
pg_ctl start [-D datadir] [-l filename] [-W] [-t seconds] [-s] [-o options] [-p path] [-c]
pg_ctl stop [-D datadir] [-m s[mart]|f[ast]|i[mmediate]] [-W] [-t seconds] [-s]
pg_ctl restart [-D datadir] [-m s[mart]|f[ast]|i[mmediate]] [-W] [-t seconds] [-s] [-o options] [-c]
pg_ctl reload [-D datadir] [-s]
pg_ctl status [-D datadir]
pg_ctl promote [-D datadir] [-W] [-t seconds] [-s]
pg_ctl logrotate [-D datadir] [-s]
pg_ctl kill signal_name process_id
```

#### 모드 설명

| 모드 | 설명 |
|------|------|
| init/initdb | 새 PostgreSQL 데이터베이스 클러스터 생성 |
| start | 백그라운드에서 서버 시작 |
| stop | 서버 종료 (`smart`/`fast`/`immediate` 모드 선택 가능) |
| restart | 서버 재시작 (stop + start) |
| reload | 설정 파일 다시 읽기 (`SIGHUP` 신호 전송) |
| status | 실행 중인 서버 상태 확인 |
| promote | 대기 서버(Standby)를 읽기-쓰기 모드로 전환 |
| logrotate | 서버 로그 파일 회전(Rotation) |
| kill | 프로세스에 신호 전송 (주로 Windows용) |

#### 종료 모드 (Shutdown Modes)

| 모드 | 설명 |
|------|------|
| Smart | 새 연결 거부, 모든 기존 클라이언트 연결 대기 후 종료 |
| Fast (기본값) | 클라이언트 대기 없음, 활성 트랜잭션 롤백, 강제 연결 해제 후 종료 |
| Immediate | 모든 서버 프로세스 즉시 중단 (다음 시작 시 복구 필요) |

#### 주요 옵션

##### 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-D datadir`, `--pgdata=datadir` | 데이터베이스 설정 파일 위치 |
| `-s`, `--silent` | 에러만 출력, 정보 메시지 없음 |
| `-t seconds`, `--timeout=seconds` | 작업 완료 대기 최대 시간 (기본값: 60초) |
| `-w`, `--wait` | 작업 완료를 기다림 (기본값) |
| `-W`, `--no-wait` | 작업 완료를 기다리지 않음 |

##### start 관련 옵션

| 옵션 | 설명 |
|------|------|
| `-l filename`, `--log=filename` | 서버 로그 출력을 파일에 기록 |
| `-o options`, `--options=options` | `postgres` 명령에 직접 전달할 옵션 |
| `-p path` | `postgres` 실행파일 위치 지정 |
| `-c`, `--core-files` | 서버 크래시 시 코어 파일 생성 허용 |

##### stop 관련 옵션

| 옵션 | 설명 |
|------|------|
| `-m mode`, `--mode=mode` | 종료 모드: `smart`, `fast` (기본값), `immediate` |

#### 환경 변수

| 변수 | 설명 |
|------|------|
| `PGCTLTIMEOUT` | 시작/종료 완료 대기 기본 시간 (초, 기본값: 60) |
| `PGDATA` | 기본 데이터 디렉토리 위치 |

#### 사용 예제

##### 서버 시작

```bash
pg_ctl start -D /usr/local/pgsql/data
```

##### 서버 시작 (포트 및 옵션 지정)

```bash
pg_ctl start -D /usr/local/pgsql/data -o "-p 5433 -c fsync=off"
```

##### 로그 파일과 함께 서버 시작

```bash
pg_ctl start -D /usr/local/pgsql/data -l /var/log/postgresql/server.log
```

##### 서버 종료

```bash
pg_ctl stop -D /usr/local/pgsql/data
```

##### Smart 모드로 종료

```bash
pg_ctl stop -D /usr/local/pgsql/data -m smart
```

##### Immediate 모드로 종료 (긴급 상황)

```bash
pg_ctl stop -D /usr/local/pgsql/data -m immediate
```

##### 서버 재시작

```bash
pg_ctl restart -D /usr/local/pgsql/data
```

##### 서버 상태 확인

```bash
pg_ctl status -D /usr/local/pgsql/data
```

출력 예:

```
pg_ctl: server is running (PID: 13718)
/usr/local/pgsql/bin/postgres "-D" "/usr/local/pgsql/data"
```

##### 설정 파일 다시 읽기 (Reload)

```bash
pg_ctl reload -D /usr/local/pgsql/data
```

##### 대기 서버를 읽기-쓰기 모드로 승격 (Promote)

```bash
pg_ctl promote -D /usr/local/pgsql/data
```

##### 데이터베이스 초기화

```bash
pg_ctl init -D /usr/local/pgsql/data
```

#### 종료 상태 코드 (Exit Status)

| 상태 | 의미 |
|------|------|
| 0 | 정상 완료 |
| 3 | status 모드에서 서버 미실행 |
| 4 | status 모드에서 접근 가능한 데이터 디렉토리 없음 |
| 그 외 | 작업 실패 또는 타임아웃 |

---

### 3. postgres - PostgreSQL 데이터베이스 서버

#### 개요

`postgres`는 PostgreSQL 데이터베이스 서버 프로세스입니다. 클라이언트 애플리케이션이 데이터베이스에 접근하려면 실행 중인 `postgres` 인스턴스에 연결해야 합니다.

#### 기본 특징

- 데이터베이스 클러스터: 하나의 `postgres` 인스턴스는 정확히 하나의 데이터베이스 클러스터를 관리
- 시작 방법: `-D` 옵션 또는 `PGDATA` 환경 변수로 데이터 디렉토리 위치 지정
- 기본 동작: 기본적으로 포그라운드(foreground)에서 실행되며 로그를 stderr로 출력
- 권장 사항: 직접 실행보다 `pg_ctl` 유틸리티 사용을 권장

#### 주요 옵션

##### 일반 목적 옵션 (General Purpose Options)

| 옵션 | 설명 |
|------|------|
| `-B nbuffers` | 공유 버퍼(Shared Buffers) 수 설정 |
| `-c name=value` | 런타임 파라미터 설정 (여러 번 사용 가능) |
| `-C name` | 파라미터 값 출력 후 종료 |
| `-d debug-level` | 디버그 레벨 설정 (1~5) |
| `-D datadir` | 데이터베이스 디렉토리 경로 지정 |
| `-e` | 유럽식 날짜 형식 설정 (DMY) |
| `-F` | fsync 비활성화 (성능 향상, 데이터 손상 위험) |
| `-h hostname` | TCP/IP 바인드 주소 지정 |
| `-k directory` | Unix 소켓 디렉토리 지정 |
| `-l` | SSL 보안 연결 활성화 |
| `-N max-connections` | 최대 클라이언트 연결 수 |
| `-p port` | TCP/IP 포트 또는 소켓 지정 |
| `-s` | 명령어 실행 시간/통계 출력 |
| `-S work-mem` | 정렬/해시 테이블 메모리 설정 |

##### 디버깅 옵션 (Semi-internal Options)

| 옵션 | 설명 |
|------|------|
| `-f {s\|i\|o\|b\|t\|n\|m\|h}` | 스캔/조인 방법 비활성화 |
| `-O` | 시스템 테이블 구조 수정 허용 |
| `-P` | 손상된 시스템 인덱스 복구 |
| `-t pa[rser]\|pl[anner]\|e[xecutor]` | 타이밍 통계 출력 |
| `-T` | 코어 덤프 파일 생성 (`SIGABRT` 사용) |
| `-W seconds` | 새 서버 프로세스 시작 시 지연 |

#### 단일 사용자 모드 (Single-User Mode)

단일 사용자 모드는 부팅, 디버깅, 재해 복구 시 사용합니다.

##### 시작 명령어

```bash
postgres --single -D /usr/local/pgsql/data [other-options] my_database
```

##### 단일 사용자 모드 옵션

| 옵션 | 설명 |
|------|------|
| `--single` | 단일 사용자 모드 선택 (첫 번째 인자) |
| `database` | 접근할 데이터베이스 이름 (마지막 인자) |
| `-E` | 실행 전 모든 명령어 표준 출력 |
| `-j` | 명령어 종료자: 세미콜론 + 2개 개행 |
| `-r filename` | 서버 로그를 파일로 리다이렉트 |

##### 특징

- newline이 명령어 종료자 (기본)
- `-j` 옵션 사용 시: `세미콜론 + 개행 + 개행`이 종료자
- 여러 명령어가 단일 트랜잭션으로 실행
- EOF (`Ctrl+D`)로 종료
- 라인 편집 기능 없음 (명령어 히스토리 불가)

#### 신호 처리 (Signal Handling)

| 신호 | 동작 |
|------|------|
| `SIGTERM` | 정상 종료 (모든 클라이언트 대기) - Smart Shutdown |
| `SIGINT` | 강제 종료 (클라이언트 즉시 연결 끊음) - Fast Shutdown |
| `SIGQUIT` | 즉시 종료 (복구 절차 필요) - Immediate Shutdown |
| `SIGHUP` | 설정 파일 재로드 |
| `SIGKILL` | 사용 금지 (리소스 해제 불가) |

#### 환경 변수

| 변수 | 설명 |
|------|------|
| `PGCLIENTENCODING` | 기본 문자 인코딩 |
| `PGDATA` | 기본 데이터 디렉토리 위치 |
| `PGDATESTYLE` | DateStyle 파라미터 기본값 (deprecated) |
| `PGPORT` | 기본 포트 번호 |

#### 사용 예제

##### 기본 시작

```bash
postgres -D /usr/local/pgsql/data
```

##### 특정 포트 지정

```bash
postgres -p 1234 -D /usr/local/pgsql/data
```

##### 런타임 파라미터 설정

```bash
postgres -c work_mem=256MB -D /usr/local/pgsql/data
postgres --work_mem=256MB -D /usr/local/pgsql/data
```

##### 단일 사용자 모드로 시작

```bash
postgres --single -D /usr/local/pgsql/data postgres
```

---

### 4. pg_upgrade - PostgreSQL 버전 업그레이드

#### 개요

`pg_upgrade`는 PostgreSQL 데이터를 메이저 버전(Major Version) 간에 업그레이드하는 도구입니다. 기존의 dump/restore 과정 없이 빠르게 업그레이드할 수 있습니다.

#### 지원 범위

- 메이저 버전 업그레이드: PostgreSQL 9.2.X 이상에서 현재 메이저 릴리스로 업그레이드 가능
  - 예: 12.14 → 13.10, 14.9 → 15.5
- 마이너 버전 업그레이드: 불필요 (예: 12.7 → 12.8, 14.1 → 14.5)

#### 기본 문법

```bash
pg_upgrade -b oldbindir [-B newbindir] -d oldconfigdir -D newconfigdir [option]...
```

#### 주요 옵션

##### 필수 옵션

| 옵션 | 환경변수 | 설명 |
|------|---------|------|
| `-b`, `--old-bindir` | `PGBINOLD` | 이전 PostgreSQL 실행 파일 디렉토리 |
| `-d`, `--old-datadir` | `PGDATAOLD` | 이전 데이터베이스 클러스터 설정 디렉토리 |
| `-D`, `--new-datadir` | `PGDATANEW` | 새 데이터베이스 클러스터 설정 디렉토리 |
| `-B`, `--new-bindir` | `PGBINNEW` | 새 PostgreSQL 실행 파일 디렉토리 (기본값: pg_upgrade 위치) |

##### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-c`, `--check` | 변경 없이 호환성만 확인 |
| `-j`, `--jobs=njobs` | 병렬 처리 작업 수 |
| `-k`, `--link` | 파일 복사 대신 하드링크 사용 |
| `-N`, `--no-sync` | 동기화 대기 안 함 (테스트용) |
| `-o`, `--old-options options` | 이전 postgres 커맨드 옵션 |
| `-O`, `--new-options options` | 새 postgres 커맨드 옵션 |
| `-p`, `--old-port=port` | 이전 클러스터 포트 |
| `-P`, `--new-port=port` | 새 클러스터 포트 |
| `-r`, `--retain` | 성공 후에도 SQL/로그 파일 유지 |
| `-s`, `--socketdir=dir` | postmaster 소켓 디렉토리 |
| `-U`, `--username=username` | 클러스터 설치 사용자명 |
| `-v`, `--verbose` | 상세 로깅 활성화 |

##### 파일 전송 모드 옵션 (File Transfer Mode)

| 옵션 | 설명 |
|------|------|
| `--copy` | 파일 복사 (기본값) |
| `--link` | 하드링크 사용 (빠르지만 이전 클러스터 사용 불가) |
| `--clone` | 효율적인 파일 클로닝 (Linux Btrfs/XFS, macOS APFS) |
| `--copy-file-range` | `copy_file_range` 시스템 호출 사용 |
| `--swap` | 데이터 디렉토리 이동 (가장 빠름) |

#### 업그레이드 단계

##### 1단계: 준비 작업

```bash
# 이전 클러스터 이동 (필요시)
mv /usr/local/pgsql /usr/local/pgsql.old

# 새 버전 설치 (소스 빌드 시)
./configure --prefix=/usr/local/pgsql.new [호환 옵션들]
make && make install
```

##### 2단계: 새 클러스터 초기화

```bash
initdb -D /path/to/new/cluster [호환 옵션들]
```

##### 3단계: 인증 설정 및 서버 중지

```bash
# pg_hba.conf 수정 (peer 인증으로 변경) 또는 ~/.pgpass 파일 생성

# 양쪽 서버 중지
pg_ctl -D /opt/PostgreSQL/12 stop
pg_ctl -D /opt/PostgreSQL/18 stop
```

##### 4단계: pg_upgrade 실행

```bash
# 기본 실행 (복사 모드)
/usr/local/pgsql.new/bin/pg_upgrade \
  -b /usr/local/pgsql/bin \
  -B /usr/local/pgsql.new/bin \
  -d /var/lib/pgsql/data \
  -D /var/lib/pgsql/new/data

# 하드링크 모드 (빠름)
pg_upgrade \
  -b /usr/local/pgsql/bin \
  -B /usr/local/pgsql.new/bin \
  -d /var/lib/pgsql/data \
  -D /var/lib/pgsql/new/data \
  --link

# 병렬 처리 (CPU 코어 수만큼)
pg_upgrade ... --jobs=8

# 미리 확인만 (실제 업그레이드 없음)
pg_upgrade ... --check
```

##### 5단계: 업그레이드 후 처리

```bash
# 설정 파일 복원
cp /usr/local/pgsql.old/data/pg_hba.conf /usr/local/pgsql.new/data/

# 새 서버 시작
pg_ctl -D /opt/PostgreSQL/18 start

# pg_upgrade가 생성한 스크립트 실행
psql --username=postgres --file=script.sql postgres

# 통계 재생성
vacuumdb --all --analyze-in-stages --missing-stats-only
vacuumdb --all --analyze-only
```

##### 6단계: 이전 클러스터 삭제

```bash
# pg_upgrade 완료 시 제공된 스크립트로 삭제
./delete_old_cluster.sh

# 또는 수동으로
rm -rf /usr/local/pgsql.old/data
```

#### 사용 예제

##### 완전한 업그레이드 시나리오 (PostgreSQL 14 → 18)

```bash
#!/bin/bash

# PostgreSQL 14 → 18 업그레이드

OLD_BIN=/usr/lib/postgresql/14/bin
NEW_BIN=/usr/lib/postgresql/18/bin
OLD_DATA=/var/lib/postgresql/14/main
NEW_DATA=/var/lib/postgresql/18/main

# 1. 미리 확인
sudo -u postgres $NEW_BIN/pg_upgrade \
  -b $OLD_BIN \
  -B $NEW_BIN \
  -d $OLD_DATA \
  -D $NEW_DATA \
  --check

# 2. 서버 중지
sudo systemctl stop postgresql

# 3. 업그레이드 실행
sudo -u postgres $NEW_BIN/pg_upgrade \
  -b $OLD_BIN \
  -B $NEW_BIN \
  -d $OLD_DATA \
  -D $NEW_DATA \
  --jobs=4 \
  --link

# 4. 새 서버 시작
sudo systemctl start postgresql

# 5. 통계 재생성
sudo -u postgres vacuumdb --all --analyze-only

# 6. 정리
sudo rm -rf /usr/lib/postgresql/14
```

##### Windows에서의 업그레이드

```bash
pg_upgrade.exe ^
  --old-datadir "C:/Program Files/PostgreSQL/12/data" ^
  --new-datadir "C:/Program Files/PostgreSQL/18/data" ^
  --old-bindir "C:/Program Files/PostgreSQL/12/bin" ^
  --new-bindir "C:/Program Files/PostgreSQL/18/bin"
```

#### 주요 주의사항

- 경고: 업그레이드 중 이전 슈퍼유저에 의한 임의 코드 실행 가능성 있음
- 링크 모드 제약: 이전 클러스터를 더 이상 사용할 수 없으며, 새 클러스터 시작 후 이전 클러스터가 손상될 위험 있음
- 파일 시스템 요구사항: 링크/클론/스왑 모드에서는 이전 클러스터와 새 클러스터가 같은 파일 시스템에 있어야 함

---

### 5. pg_resetwal - WAL 및 제어 정보 재설정

#### 개요

`pg_resetwal`은 PostgreSQL 데이터베이스 클러스터의 Write-Ahead Log(WAL)와 제어 정보(`pg_control`)를 초기화하는 도구입니다. 파일 손상으로 서버가 기동되지 않을 때 사용하는 최후의 수단입니다.

#### 기본 문법

```bash
pg_resetwal [ -f | --force ] [ -n | --dry-run ] [ option... ] [ -D | --pgdata ] datadir
```

#### 중요 주의사항

이 도구는 매우 위험합니다:

- 서버가 비정상 종료되었거나 제어 파일이 손상된 경우 `-f` 옵션이 필수
- 복구 후 즉시 데이터 덤프 및 복원 필요
- pg_resetwal 실행 후 데이터 수정 작업 금지 (손상 악화 가능)

안전한 사용:

- 정상 종료된 데이터 디렉토리에서는 부작용 없음
- `--wal-segsize` 같은 옵션으로 전역 설정을 안전하게 변경 가능

#### 주요 옵션

##### 제어 옵션

| 옵션 | 설명 |
|------|------|
| `-D datadir`, `--pgdata=datadir` | 데이터 디렉토리 위치 (필수) |
| `-f`, `--force` | 위험한 상황에서도 강제 실행 |
| `-n`, `--dry-run` | 실제 수정 없이 복구 값 출력 |
| `-V`, `--version` | 버전 정보 표시 |
| `-?`, `--help` | 도움말 표시 |

##### 트랜잭션 ID 관련 (Transaction ID)

| 옵션 | 설명 |
|------|------|
| `-x xid`, `--next-transaction-id=xid` | 다음 트랜잭션 ID 설정 |
| `-u xid`, `--oldest-transaction-id=xid` | 가장 오래된 미동결 트랜잭션 ID 설정 |
| `-e xid_epoch`, `--epoch=xid_epoch` | 트랜잭션 ID의 에포크 설정 |

##### 멀티트랜잭션 관련 (Multitransaction)

| 옵션 | 설명 |
|------|------|
| `-m mxid,mxid`, `--multixact-ids=mxid,mxid` | 다음 멀티트랜잭션 ID와 가장 오래된 ID 설정 |
| `-O mxoff`, `--multixact-offset=mxoff` | 멀티트랜잭션 오프셋 설정 |

##### WAL 관련 (Write-Ahead Log)

| 옵션 | 설명 |
|------|------|
| `-l walfile`, `--next-wal-file=walfile` | WAL 시작 위치 설정 |
| `--wal-segsize=wal_segment_size` | WAL 세그먼트 크기 설정 (1~1024 MB) |

##### 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-o oid`, `--next-oid=oid` | 다음 OID 설정 |
| `-c xid,xid`, `--commit-timestamp-ids=xid,xid` | 커밋 시간 조회 가능한 트랜잭션 ID 범위 설정 |

#### 안전값 결정 방법

##### 다음 트랜잭션 ID (`-x`)

```bash
# pg_xact 디렉토리의 가장 큰 파일명 + 1, × 1048576 (0x100000)
# 예: 0011이 최대값이면 -x 0x1200000 사용
ls -la $PGDATA/pg_xact/
```

##### 가장 오래된 트랜잭션 ID (`-u`)

```bash
# pg_xact 디렉토리의 가장 작은 파일명 × 1048576 (0x100000)
# 예: 0007이 최소값이면 -u 0x700000 사용
ls -la $PGDATA/pg_xact/
```

##### 다음 WAL 파일 (`-l`)

```bash
# pg_wal 디렉토리의 기존 파일보다 큰 값 필요
# 예: 00000001000000320000004A가 최대면 -l 00000001000000320000004B 이상 사용
ls -la $PGDATA/pg_wal/
```

#### 실행 조건

| 조건 | 요구사항 |
|------|---------|
| 서버 상태 | 반드시 서버가 종료되어야 함 |
| 사용자 | 서버를 설치한 사용자만 실행 가능 |
| 버전 | 동일 메이저 버전 서버에서만 작동 |

#### 사용 예제

##### 기본 사용 (안전한 상황)

```bash
# 드라이 런 (실제 수정 없음)
pg_resetwal -n -D /var/lib/postgresql/14/main

# 실제 실행
pg_resetwal -D /var/lib/postgresql/14/main
```

##### 위험한 상황에서의 사용

```bash
# 비정상 종료된 서버의 WAL 초기화
pg_resetwal -f -D /var/lib/postgresql/14/main
```

##### 수동 값 설정

```bash
# 트랜잭션 ID와 OID 수동 설정
pg_resetwal -x 0x1200000 -u 0x700000 -o 100000 -D /var/lib/postgresql/14/main
```

##### WAL 세그먼트 크기 변경

```bash
# 64MB WAL 세그먼트로 변경
pg_resetwal --wal-segsize=64 -D /var/lib/postgresql/14/main
```

##### 멀티트랜잭션 정보 설정

```bash
pg_resetwal -m 0x20000,0x10000 -O 0x150000 -D /var/lib/postgresql/14/main
```

#### 데이터 복구 절차

```bash
# 1. pg_resetwal 실행 (필요시)
pg_resetwal -f -D /var/lib/postgresql/data

# 2. 서버 시작 확인
pg_ctl -D /var/lib/postgresql/data start

# 3. 즉시 데이터 덤프
pg_dumpall > backup.sql

# 4. initdb로 초기화
initdb -D /var/lib/postgresql/new_data

# 5. 새 서버 시작
pg_ctl -D /var/lib/postgresql/new_data start

# 6. 데이터 복구
psql < backup.sql

# 7. 일관성 검증
```

---

### 6. pg_rewind - 데이터 디렉토리 동기화

#### 개요

`pg_rewind`는 PostgreSQL 클러스터의 타임라인(Timeline)이 분기된 후, 한 클러스터를 다른 클러스터 사본과 동기화하는 도구입니다.

#### 전형적인 사용 사례

- 페일오버(Failover) 후 이전 주(Primary) 서버를 새로운 주 서버를 따르는 대기(Standby)로 복구
- 복제 구성에서 분기된 서버를 다시 동기화

#### 주요 특징

- 효율성: 변경된 블록만 복사하므로 변경되지 않은 관계 블록을 비교·복사할 필요 없음
- 속도: 대규모 데이터베이스에서 블록 변화가 적을 때 rsync 등보다 훨씬 빠름

#### 요구사항

- `wal_log_hints` 옵션 활성화 또는 데이터 체크섬 활성화
- `full_page_writes = on` (기본값)

#### 기본 문법

```bash
pg_rewind [option...] {-D | --target-pgdata} directory {--source-pgdata=directory | --source-server=connstr}
```

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory`, `--target-pgdata=directory` | 필수 동기화할 대상 데이터 디렉토리 (서버 종료 필수) |
| `--source-pgdata=directory` | 소스 서버 데이터 디렉토리 경로 (서버 깔끔한 종료 필수) |
| `--source-server=connstr` | libpq 연결 문자열로 소스 서버 지정 (서버 실행 중) |
| `-R`, `--write-recovery-conf` | `standby.signal` 생성 및 `postgresql.auto.conf`에 연결 설정 추가 |
| `-n`, `--dry-run` | 실제 수정 없이 모든 작업 실행 (테스트용) |
| `-N`, `--no-sync` | 디스크 동기화 대기 생략 (테스트용) |
| `-P`, `--progress` | 진행 상황 보고 활성화 |
| `-c`, `--restore-target-wal` | WAL 아카이브에서 누락된 WAL 파일 검색 |
| `--config-file=filename` | 대상 클러스터 구성 파일 지정 |
| `--debug` | 상세 디버깅 출력 |
| `--no-ensure-shutdown` | 깔끔한 종료 확인 건너뛰기 |

#### 동작 방식

##### 1단계: WAL 스캔

- 소스 클러스터의 타임라인 분기 지점 이후 마지막 체크포인트부터 대상 클러스터의 WAL 로그를 스캔
- 변경된 모든 데이터 블록을 기록
- 누락된 WAL 파일이 있으면 `-c` 옵션을 추가해 다시 실행

##### 2단계: 변경 블록 복사

```
--source-pgdata (파일 시스템 접근) 또는
--source-server (SQL 접근)
```

- 모든 변경 블록을 소스에서 대상으로 복사
- 관계 파일이 타임라인 분기 직전의 체크포인트 상태로 동기화

##### 3단계: 기타 파일 복사

- 새 관계 파일, WAL 세그먼트, `pg_xact`, 구성 파일 전체 복사
- 제외 디렉토리: `pg_dynshmem/`, `pg_notify/`, `pg_replslot/`, `pg_serial/`, `pg_snapshots/`, `pg_stat_tmp/`, `pg_subtrans/`

##### 4단계: backup_label 생성

- WAL 재생 시작점 구성
- `pg_control` 파일에 최소 일관성 LSN 설정

##### 5단계: WAL 재생

- 대상 서버 시작 시 필요한 WAL 재생
- 일관된 상태의 데이터 디렉토리 완성

#### 사용자 권한 설정

슈퍼유저 대신 필요한 권한만 가진 역할을 생성해 사용할 수 있습니다:

```sql
CREATE USER rewind_user LOGIN;
GRANT EXECUTE ON function pg_catalog.pg_ls_dir(text, boolean, boolean) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_stat_file(text, boolean) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_read_binary_file(text) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_read_binary_file(text, bigint, bigint, boolean) TO rewind_user;
```

#### 사용 예제

##### 파일 시스템 경로로 동기화

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data
```

##### 라이브 서버로 동기화 (복구 설정 자동 생성)

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-server="host=primary_server dbname=postgres user=rewind_user" \
          -R
```

##### 드라이 런 (테스트)

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data \
          -n --progress
```

##### WAL 아카이브에서 누락 파일 복구

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data \
          -c -P
```

#### 페일오버 후 복구 시나리오

```bash
# 1. 이전 Primary 서버 종료 확인
pg_ctl -D /var/lib/postgresql/old_primary stop

# 2. pg_rewind 실행 (새 Primary를 소스로)
pg_rewind -D /var/lib/postgresql/old_primary \
          --source-server="host=new_primary dbname=postgres" \
          -R -P

# 3. 이전 Primary를 Standby로 시작
pg_ctl -D /var/lib/postgresql/old_primary start
```

#### 중요 주의사항

- 실패 시: 대상 데이터 디렉토리가 복구 불가능한 상태가 될 수 있으므로 새 백업 필수
- 읽기 전용 파일: SSL 인증서 등 읽기 전용 파일이 있으면 재생성 전에 미리 제거 권장
- 복구 구성: pg_rewind 후 복구 설정 없이 서버를 재시작하면 주 서버에서 다시 분기될 수 있음

#### 환경 변수

- `--source-server` 사용 시 libpq 환경 변수 지원
- `PG_COLOR`: `always`, `auto`, `never` (진단 메시지 색상)

---

### 요약

| 도구 | 주요 용도 |
|------|----------|
| initdb | 새 데이터베이스 클러스터 초기화 |
| pg_ctl | 서버 시작, 중지, 재시작, 상태 확인 |
| postgres | 데이터베이스 서버 프로세스 (직접 또는 pg_ctl 통해 실행) |
| pg_upgrade | 메이저 버전 업그레이드 |
| pg_resetwal | WAL 및 제어 정보 재설정 (긴급 복구용) |
| pg_rewind | 분기된 클러스터 동기화 (페일오버 복구용) |

---

### 참고 문서

- [PostgreSQL 공식 문서 - Server Applications](https://www.postgresql.org/docs/current/reference-server.html)
- [PostgreSQL 공식 문서 - initdb](https://www.postgresql.org/docs/current/app-initdb.html)
- [PostgreSQL 공식 문서 - pg_ctl](https://www.postgresql.org/docs/current/app-pg-ctl.html)
- [PostgreSQL 공식 문서 - postgres](https://www.postgresql.org/docs/current/app-postgres.html)
- [PostgreSQL 공식 문서 - pg_upgrade](https://www.postgresql.org/docs/current/pgupgrade.html)
- [PostgreSQL 공식 문서 - pg_resetwal](https://www.postgresql.org/docs/current/app-pgresetwal.html)
- [PostgreSQL 공식 문서 - pg_rewind](https://www.postgresql.org/docs/current/app-pgrewind.html)

---

## PostgreSQL 내부 함수와 시스템 관리 함수 (Internal Functions and System Administration Functions)

### 개요

PostgreSQL은 데이터베이스 서버의 관리, 모니터링, 백업, 복제 등을 위한 다양한 시스템 함수를 제공합니다. 이러한 함수들은 DBA(Database Administrator)와 시스템 운영자가 PostgreSQL 클러스터를 효과적으로 관리하는 데 필수적입니다.

---

## 1. 서버 신호 함수 (Server Signaling Functions)

서버 신호 함수는 다른 서버 프로세스의 동작을 제어하는 데 사용됩니다. 모든 함수는 `boolean`을 반환하며, 신호 전송 성공 시 `true`, 실패 시 `false`를 반환합니다.

### pg_cancel_backend()

```sql
pg_cancel_backend(pid integer) → boolean
```

설명: 지정된 백엔드 프로세스의 현재 쿼리를 취소합니다.

권한: 해당 역할의 멤버이거나 `pg_signal_backend` 권한이 있어야 합니다. 슈퍼유저 백엔드는 슈퍼유저만 취소할 수 있습니다.

예제:
```sql
-- PID가 12345인 프로세스의 쿼리 취소
SELECT pg_cancel_backend(12345);

-- 특정 사용자의 모든 쿼리 취소
SELECT pg_cancel_backend(pid)
FROM pg_stat_activity
WHERE usename = 'slow_user' AND state = 'active';
```

### pg_terminate_backend()

```sql
pg_terminate_backend(pid integer, timeout bigint DEFAULT 0) → boolean
```

설명: 지정된 백엔드 프로세스의 세션을 종료합니다.

매개변수:
- `pid`: 종료할 프로세스 ID
- `timeout`: 대기 시간(밀리초). 0이면 즉시 반환(기본값). 양수이면 프로세스 종료 또는 타임아웃까지 대기

반환값: 성공 시 `true`, 타임아웃 시 `false`

예제:
```sql
-- 즉시 종료 요청 (결과 대기 안 함)
SELECT pg_terminate_backend(12345);

-- 5초 대기하며 종료
SELECT pg_terminate_backend(12345, 5000);

-- 1시간 이상 실행 중인 쿼리 종료
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
  AND query_start < now() - interval '1 hour';
```

### pg_log_backend_memory_contexts()

```sql
pg_log_backend_memory_contexts(pid integer) → boolean
```

설명: 지정된 백엔드의 메모리 컨텍스트 정보를 서버 로그에 기록하도록 요청합니다.

권한: 슈퍼유저 전용

예제:
```sql
-- 특정 백엔드의 메모리 사용량 로깅
SELECT pg_log_backend_memory_contexts(pg_backend_pid());
```

### pg_reload_conf()

```sql
pg_reload_conf() → boolean
```

설명: 모든 PostgreSQL 프로세스가 설정 파일을 다시 로드하도록 신호를 보냅니다. postmaster에 SIGHUP을 전달하면 postmaster가 자식 프로세스에 다시 전파합니다.

참고: 리로드 전에 `pg_file_settings`, `pg_hba_file_rules`, `pg_ident_file_mappings` 뷰로 설정 파일을 확인하는 것이 좋습니다.

예제:
```sql
-- 설정 파일 변경 후 리로드
SELECT pg_reload_conf();

-- 리로드 전 설정 확인
SELECT * FROM pg_file_settings WHERE error IS NOT NULL;
```

### pg_rotate_logfile()

```sql
pg_rotate_logfile() → boolean
```

설명: 로그 파일 관리자에게 새 출력 파일로 즉시 전환하도록 신호를 보냅니다.

참고: 내장 로그 수집기(built-in log collector)가 실행 중일 때만 작동합니다.

예제:
```sql
-- 로그 파일 즉시 교체
SELECT pg_rotate_logfile();
```

---

## 2. 백업 제어 함수 (Backup Control Functions)

백업 제어 함수는 온라인 백업 수행에 필요합니다. 복구(recovery) 중에는 실행할 수 없습니다(일부 제외).

### pg_backup_start()

```sql
pg_backup_start(label text [, fast boolean]) → pg_lsn
```

설명: 서버를 온라인 백업 수행 준비 상태로 만듭니다.

매개변수:
- `label`: 사용자 정의 백업 레이블 (일반적으로 백업 덤프 파일명)
- `fast`: `true`이면 즉시 체크포인트 실행 (I/O 스파이크 발생 가능)

반환값: WAL 위치 (pg_lsn)

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 일반 백업 시작
SELECT pg_backup_start('daily_backup_2024');

-- 빠른 백업 시작 (즉시 체크포인트)
SELECT pg_backup_start('emergency_backup', true);
```

### pg_backup_stop()

```sql
pg_backup_stop([wait_for_archive boolean]) → record
  (lsn pg_lsn, labelfile text, spcmapfile text)
```

설명: 온라인 백업을 완료합니다.

매개변수:
- `wait_for_archive`: `true`(기본값)이면 WAL 아카이빙 완료까지 대기. `false`이면 즉시 반환 (권장하지 않음)

반환값:
- `lsn`: 백업 종료 LSN
- `labelfile`: 백업 레이블 파일 내용
- `spcmapfile`: 테이블스페이스 맵 파일 내용

중요: 반환된 파일 내용은 반드시 백업 영역에 기록해야 합니다. 라이브 데이터 디렉터리에는 기록하지 마십시오.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 백업 종료 및 결과 확인
SELECT * FROM pg_backup_stop();

-- 백업 레이블 파일만 가져오기
SELECT labelfile FROM pg_backup_stop();
```

### pg_create_restore_point()

```sql
pg_create_restore_point(name text) → pg_lsn
```

설명: WAL에 복구 대상으로 사용할 수 있는 명명된 마커를 생성합니다.

매개변수:
- `name`: 복구 지점 이름 (`recovery_target_name` 매개변수와 함께 사용)

주의: 중복 이름 사용은 피하는 것이 좋습니다. 복구는 이름이 일치하는 첫 번째 지점에서 중단됩니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 복구 지점 생성
SELECT pg_create_restore_point('before_schema_change');
SELECT pg_create_restore_point('after_data_migration');
```

### WAL 위치 함수

#### pg_current_wal_lsn()

```sql
pg_current_wal_lsn() → pg_lsn
```

설명: 현재 WAL 쓰기 위치를 반환합니다. 아카이빙에 가장 적합합니다.

예제:
```sql
SELECT pg_current_wal_lsn();
-- 결과: 0/1A2B3C4D
```

#### pg_current_wal_insert_lsn()

```sql
pg_current_wal_insert_lsn() → pg_lsn
```

설명: 현재 WAL 삽입 위치(논리적 끝)를 반환합니다. 주로 디버깅용입니다.

#### pg_current_wal_flush_lsn()

```sql
pg_current_wal_flush_lsn() → pg_lsn
```

설명: 영구 저장소에 기록된 것으로 알려진 마지막 WAL 위치를 반환합니다.

#### pg_switch_wal()

```sql
pg_switch_wal() → pg_lsn
```

설명: 아카이빙을 위해 새 WAL 파일로 강제 전환합니다.

반환값: 완료된 파일의 끝 위치 + 1. 마지막 전환 이후 WAL 활동이 없으면 현재 WAL 위치의 시작 부분을 반환합니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- WAL 파일 강제 전환
SELECT pg_switch_wal();
```

#### pg_walfile_name()

```sql
pg_walfile_name(lsn pg_lsn) → text
```

설명: WAL 위치를 WAL 파일명으로 변환합니다.

예제:
```sql
SELECT pg_walfile_name('0/1A2B3C4D');
-- 결과: 000000010000000000000001
```

#### pg_walfile_name_offset()

```sql
pg_walfile_name_offset(lsn pg_lsn) → record
  (file_name text, file_offset integer)
```

설명: LSN을 파일명과 파일 내 바이트 오프셋으로 변환합니다.

예제:
```sql
SELECT * FROM pg_walfile_name_offset(pg_current_wal_lsn());
--        file_name         | file_offset
-- --------------------------+-------------
--  00000001000000000000000D |     4039624
```

#### pg_split_walfile_name()

```sql
pg_split_walfile_name(file_name text) → record
  (segment_number numeric, timeline_id bigint)
```

설명: WAL 파일명에서 시퀀스 번호와 타임라인 ID를 추출합니다.

#### pg_wal_lsn_diff()

```sql
pg_wal_lsn_diff(lsn1 pg_lsn, lsn2 pg_lsn) → numeric
```

설명: 두 WAL 위치 간의 바이트 차이를 계산합니다 (`lsn1 - lsn2`).

용도: 복제 지연(replication lag) 계산에 유용합니다.

예제:
```sql
-- 복제 지연 바이트 계산
SELECT pg_wal_lsn_diff(
    pg_current_wal_lsn(),
    replay_lsn
) AS replication_lag_bytes
FROM pg_stat_replication;
```

---

## 3. 복구 제어 함수 (Recovery Control Functions)

복구 제어 함수는 복구(recovery) 상태를 확인하고 제어하는 데 사용됩니다.

### 복구 정보 함수 (Recovery Information Functions)

복구 중이나 정상 실행 중 모두 사용 가능합니다.

#### pg_is_in_recovery()

```sql
pg_is_in_recovery() → boolean
```

설명: 복구가 진행 중인지 여부를 반환합니다.

예제:
```sql
SELECT pg_is_in_recovery();
-- Primary: false
-- Standby: true
```

#### pg_last_wal_receive_lsn()

```sql
pg_last_wal_receive_lsn() → pg_lsn
```

설명: 스트리밍 복제로 수신 및 동기화된 마지막 WAL 위치를 반환합니다.

반환값: 스트리밍 비활성화 또는 미시작 시 `NULL`

#### pg_last_wal_replay_lsn()

```sql
pg_last_wal_replay_lsn() → pg_lsn
```

설명: 복구 중 재생된 마지막 WAL 위치를 반환합니다.

반환값: 복구 없이 정상 시작된 경우 `NULL`

#### pg_last_xact_replay_timestamp()

```sql
pg_last_xact_replay_timestamp() → timestamp with time zone
```

설명: 복구 중 재생된 마지막 트랜잭션의 타임스탬프를 반환합니다.

참고: 프라이머리에서 커밋/중단 WAL 레코드가 생성된 시간입니다.

예제:
```sql
-- 복제 지연 시간 확인
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

#### pg_get_wal_resource_managers()

```sql
pg_get_wal_resource_managers() → setof record
  (rm_id integer, rm_name text, rm_builtin boolean)
```

설명: 현재 로드된 WAL 리소스 관리자 목록을 반환합니다.

반환 필드:
- `rm_id`: 리소스 관리자 ID
- `rm_name`: 리소스 관리자 이름
- `rm_builtin`: 내장 여부 (`true`: 내장, `false`: 확장으로 로드됨)

### 복구 제어 함수 (Recovery Control Functions)

복구 중에만 실행 가능합니다.

#### pg_is_wal_replay_paused()

```sql
pg_is_wal_replay_paused() → boolean
```

설명: 복구 일시정지가 요청된 상태인지 여부를 반환합니다.

#### pg_get_wal_replay_pause_state()

```sql
pg_get_wal_replay_pause_state() → text
```

설명: 현재 복구 일시정지 상태를 반환합니다.

반환값:
- `not paused`: 일시정지 요청 없음
- `pause requested`: 일시정지 요청됨, 아직 일시정지 안 됨
- `paused`: 복구가 실제로 일시정지됨

#### pg_wal_replay_pause()

```sql
pg_wal_replay_pause() → void
```

설명: 복구 일시정지를 요청합니다.

참고: 일시정지 중에는 데이터베이스 변경이 적용되지 않으며, 쿼리는 일관된 스냅샷을 기준으로 실행됩니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 복구 일시정지
SELECT pg_wal_replay_pause();

-- 상태 확인
SELECT pg_get_wal_replay_pause_state();
```

#### pg_wal_replay_resume()

```sql
pg_wal_replay_resume() → void
```

설명: 일시정지된 복구를 재개합니다.

권한: 슈퍼유저 (권한 부여 가능)

#### pg_promote()

```sql
pg_promote([wait boolean DEFAULT true, wait_seconds integer DEFAULT 60]) → boolean
```

설명: 스탠바이를 프라이머리로 승격합니다.

매개변수:
- `wait`: `true`이면 승격 완료 또는 타임아웃까지 대기 (기본값). `false`이면 SIGUSR1 신호 전송 후 즉시 반환
- `wait_seconds`: 대기 시간(초). 기본값 60초

반환값: 승격 성공 시 `true`, 타임아웃 시 `false`

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 스탠바이 승격 (60초 대기)
SELECT pg_promote();

-- 즉시 승격 요청 (대기 안 함)
SELECT pg_promote(false);

-- 120초 대기하며 승격
SELECT pg_promote(true, 120);
```

주의사항:
- 승격 진행 중에는 `pg_wal_replay_pause()` 또는 `pg_wal_replay_resume()` 실행 불가
- 일시정지 상태에서 승격이 트리거되면 일시정지 상태가 해제됨
- 스트리밍이 활성화된 상태에서 무기한 일시정지하면 디스크 공간이 부족해질 수 있음

---

## 4. 스냅샷 동기화 함수 (Snapshot Synchronization Functions)

스냅샷 동기화 함수는 여러 세션 간에 동일한 데이터베이스 뷰를 공유하는 데 사용됩니다.

### pg_export_snapshot()

```sql
pg_export_snapshot() → text
```

설명: 트랜잭션의 현재 스냅샷을 저장하고 식별 문자열을 반환합니다.

용도: 반환된 문자열을 다른 클라이언트에 전달하면 해당 클라이언트도 동일한 스냅샷을 사용할 수 있습니다.

주의사항:
- 내보낸 트랜잭션이 종료될 때까지만 유효
- 트랜잭션당 여러 번 내보내기 가능 (`READ COMMITTED`에서만 의미 있음)
- 스냅샷 내보내기 후 `PREPARE TRANSACTION` 사용 불가
- 동기화된 트랜잭션은 기존 데이터에 대해 동일한 뷰를 가지지만, 서로의 미커밋 변경 사항은 볼 수 없음

예제:
```sql
-- 세션 1: 스냅샷 내보내기
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT pg_export_snapshot();
-- 결과: '00000003-0000001B-1'

-- 세션 2: 스냅샷 가져오기
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET TRANSACTION SNAPSHOT '00000003-0000001B-1';
-- 이제 세션 1과 동일한 데이터를 볼 수 있음
```

### pg_log_standby_snapshot()

```sql
pg_log_standby_snapshot() → pg_lsn
```

설명: 실행 중인 트랜잭션의 스냅샷을 WAL에 기록합니다.

용도: 스탠바이에서의 논리적 디코딩(logical decoding)에 유용합니다. bgwriter나 checkpointer가 스냅샷을 기록할 때까지 기다릴 필요가 없습니다.

---

## 5. 복제 관리 함수 (Replication Management Functions)

복제 관리 함수는 복제 슬롯, 논리적 디코딩, 복제 원점(replication origin)을 관리합니다.

### 5.1 물리적 복제 슬롯 (Physical Replication Slots)

#### pg_create_physical_replication_slot()

```sql
pg_create_physical_replication_slot(
  slot_name name,
  [immediately_reserve boolean, temporary boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 새 물리적 복제 슬롯을 생성합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `immediately_reserve`: `true`이면 즉시 LSN 예약 (기본값: 첫 연결 시)
- `temporary`: `true`이면 세션 전용, 에러 시 자동 해제

예제:
```sql
-- 물리적 복제 슬롯 생성
SELECT * FROM pg_create_physical_replication_slot('standby_slot');

-- 즉시 예약으로 임시 슬롯 생성
SELECT * FROM pg_create_physical_replication_slot('temp_slot', true, true);
```

#### pg_copy_physical_replication_slot()

```sql
pg_copy_physical_replication_slot(
  src_slot_name name,
  dst_slot_name name,
  [temporary boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 기존 물리적 슬롯을 복사합니다.

참고: 복사된 슬롯은 소스 슬롯의 LSN부터 WAL을 예약합니다. 무효화된 슬롯은 복사할 수 없습니다.

#### pg_drop_replication_slot()

```sql
pg_drop_replication_slot(slot_name name) → void
```

설명: 물리적 또는 논리적 복제 슬롯을 삭제합니다.

예제:
```sql
-- 복제 슬롯 삭제
SELECT pg_drop_replication_slot('standby_slot');
```

### 5.2 논리적 복제 슬롯 (Logical Replication Slots)

#### pg_create_logical_replication_slot()

```sql
pg_create_logical_replication_slot(
  slot_name name,
  plugin name,
  [temporary boolean, twophase boolean, failover boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 새 논리적(디코딩) 복제 슬롯을 생성합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `plugin`: 출력 플러그인 이름 (예: `pgoutput`, `test_decoding`)
- `temporary`: `true`이면 세션 전용
- `twophase`: `true`이면 준비된 트랜잭션(prepared transactions) 디코딩 활성화
- `failover`: `true`이면 페일오버 재개를 위해 스탠바이에 동기화

예제:
```sql
-- 논리적 복제 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot('my_slot', 'pgoutput');

-- 2단계 커밋 지원 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot(
    'two_phase_slot',
    'pgoutput',
    false,  -- temporary
    true    -- twophase
);
```

#### pg_copy_logical_replication_slot()

```sql
pg_copy_logical_replication_slot(
  src_slot_name name,
  dst_slot_name name,
  [temporary boolean, plugin name]
) → record (slot_name name, lsn pg_lsn)
```

설명: 기존 논리적 복제 슬롯을 복사합니다.

참고: 출력 플러그인과 지속성을 변경할 수 있습니다. `failover`는 복사되지 않습니다(기본값 false).

### 5.3 논리적 디코딩 (Logical Decoding)

#### pg_logical_slot_get_changes()

```sql
pg_logical_slot_get_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data text)
```

설명: 마지막 소비 이후의 변경 사항을 반환하고 소비합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `upto_lsn`: `NULL`이면 WAL 끝까지, 아니면 해당 LSN 이전에 커밋된 트랜잭션만 포함
- `upto_nchanges`: `NULL`이면 제한 없음, 양수이면 해당 행 수 초과 시 중단 (소프트 제한, 트랜잭션 단위로 확인)
- `options`: 출력 플러그인에 전달할 옵션

예제:
```sql
-- 모든 변경 사항 가져오기
SELECT * FROM pg_logical_slot_get_changes('my_slot', NULL, NULL);

-- 최대 100개 변경 사항만 가져오기
SELECT * FROM pg_logical_slot_get_changes('my_slot', NULL, 100);

-- 옵션과 함께 가져오기
SELECT * FROM pg_logical_slot_get_changes(
    'my_slot',
    NULL,
    NULL,
    'include-xids', 'true',
    'include-timestamp', 'true'
);
```

#### pg_logical_slot_peek_changes()

```sql
pg_logical_slot_peek_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data text)
```

설명: `pg_logical_slot_get_changes()`와 동일하지만 변경 사항을 소비하지 않습니다. 다음 호출 시 다시 반환됩니다.

#### pg_logical_slot_get_binary_changes()

```sql
pg_logical_slot_get_binary_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data bytea)
```

설명: `pg_logical_slot_get_changes()`와 같지만 `bytea`로 반환합니다.

#### pg_logical_slot_peek_binary_changes()

```sql
pg_logical_slot_peek_binary_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data bytea)
```

설명: `pg_logical_slot_peek_changes()`와 같지만 `bytea`로 반환합니다.

#### pg_replication_slot_advance()

```sql
pg_replication_slot_advance(
  slot_name name,
  upto_lsn pg_lsn
) → record (slot_name name, end_lsn pg_lsn)
```

설명: 복제 슬롯의 확인된 위치를 앞으로 이동합니다.

주의: 슬롯 위치를 뒤로 이동하거나 삽입 위치를 초과하여 이동할 수 없습니다. 변경된 위치는 다음 체크포인트에서 기록됩니다.

예제:
```sql
-- 슬롯 위치 앞으로 이동
SELECT * FROM pg_replication_slot_advance('my_slot', '0/1A2B3C4D');
```

### 5.4 복제 원점 (Replication Origins)

복제 원점은 복제된 데이터의 출처를 추적하는 데 사용됩니다.

#### pg_replication_origin_create()

```sql
pg_replication_origin_create(node_name text) → oid
```

설명: 외부 이름으로 복제 원점을 생성합니다.

반환값: 내부 ID

주의: 최대 이름 길이 512바이트

예제:
```sql
-- 복제 원점 생성
SELECT pg_replication_origin_create('remote_cluster_1');
```

#### pg_replication_origin_drop()

```sql
pg_replication_origin_drop(node_name text) → void
```

설명: 복제 원점과 연관된 재생 진행 상황을 삭제합니다.

#### pg_replication_origin_oid()

```sql
pg_replication_origin_oid(node_name text) → oid
```

설명: 이름으로 복제 원점을 찾아 내부 ID를 반환합니다. 없으면 `NULL` 반환.

#### pg_replication_origin_session_setup()

```sql
pg_replication_origin_session_setup(node_name text) → void
```

설명: 현재 세션을 지정된 원점에서 재생하는 것으로 표시합니다.

주의: 이미 선택된 원점이 없는 경우에만 호출 가능합니다. `pg_replication_origin_session_reset()`으로 해제할 수 있습니다.

#### pg_replication_origin_session_reset()

```sql
pg_replication_origin_session_reset() → void
```

설명: `pg_replication_origin_session_setup()` 효과를 취소합니다.

#### pg_replication_origin_session_is_setup()

```sql
pg_replication_origin_session_is_setup() → boolean
```

설명: 현재 세션에서 복제 원점이 선택되었는지 여부를 반환합니다.

#### pg_replication_origin_session_progress()

```sql
pg_replication_origin_session_progress(flush boolean) → pg_lsn
```

설명: 선택된 원점의 재생 위치를 반환합니다.

매개변수:
- `flush`: 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

#### pg_replication_origin_xact_setup()

```sql
pg_replication_origin_xact_setup(
  origin_lsn pg_lsn,
  origin_timestamp timestamp with time zone
) → void
```

설명: 현재 트랜잭션을 원점에서 재생하는 트랜잭션으로 표시합니다.

주의: `pg_replication_origin_session_setup()`으로 원점이 선택된 경우에만 호출 가능합니다.

#### pg_replication_origin_xact_reset()

```sql
pg_replication_origin_xact_reset() → void
```

설명: `pg_replication_origin_xact_setup()` 효과를 취소합니다.

#### pg_replication_origin_advance()

```sql
pg_replication_origin_advance(node_name text, lsn pg_lsn) → void
```

설명: 원점 노드의 복제 진행 상황을 설정합니다.

용도: 초기 설정이나 구성 변경 후에 사용합니다.

경고: 부주의하게 사용하면 복제 일관성이 깨질 수 있습니다.

#### pg_replication_origin_progress()

```sql
pg_replication_origin_progress(node_name text, flush boolean) → pg_lsn
```

설명: 지정된 원점의 재생 위치를 반환합니다.

매개변수:
- `flush`: 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

### 5.5 논리적 디코딩 메시지 (Logical Decoding Messages)

#### pg_logical_emit_message()

```sql
pg_logical_emit_message(
  transactional boolean,
  prefix text,
  content text | bytea,
  [flush boolean DEFAULT false]
) → pg_lsn
```

설명: 플러그인용 논리적 디코딩 메시지를 발행합니다.

매개변수:
- `transactional`: `true`이면 현재 트랜잭션의 일부, `false`이면 즉시 기록
- `prefix`: 플러그인 인식을 위한 텍스트 접두사
- `content`: 메시지 내용 (텍스트 또는 바이너리)
- `flush`: `false`(기본값)이면 WAL 쓰기 지연 가능 (transactional이면 무시됨)

예제:
```sql
-- 트랜잭션 메시지 발행
SELECT pg_logical_emit_message(true, 'myapp', 'data migration started');

-- 비트랜잭션 메시지 즉시 발행
SELECT pg_logical_emit_message(false, 'myapp', 'checkpoint marker', true);
```

### 5.6 복제 슬롯 동기화 (Replication Slot Synchronization)

#### pg_sync_replication_slots()

```sql
pg_sync_replication_slots() → void
```

설명: 프라이머리에서 스탠바이로 논리적 페일오버 복제 슬롯을 동기화합니다.

주의사항:
- 스탠바이에서만 실행
- 승격 후 임시 동기화 슬롯을 삭제해야 함
- `sync_replication_slots`가 활성화되고 slotsync 워커가 실행 중이면 실행 불가
- `hot_standby_feedback` 비활성화 후 동기화된 슬롯이 무효화될 수 있음

---

## 6. 통계 함수 (Statistics Functions)

### 6.1 데이터베이스 객체 크기 함수

모든 크기 결과는 바이트 단위입니다. 존재하지 않는 OID에 대해서는 `NULL`을 반환합니다.

#### pg_column_size()

```sql
pg_column_size("any") → integer
```

설명: 개별 데이터 값을 저장하는 데 사용된 바이트 수를 반환합니다. 압축이 적용된 경우 이를 반영합니다.

예제:
```sql
SELECT pg_column_size('Hello World');
-- 결과: 12

SELECT pg_column_size(repeat('x', 10000));
-- 압축으로 인해 실제 크기보다 작을 수 있음
```

#### pg_column_compression()

```sql
pg_column_compression("any") → text
```

설명: 가변 길이 값에 사용된 압축 알고리즘을 반환합니다. 압축되지 않은 경우 `NULL` 반환.

#### pg_database_size()

```sql
pg_database_size(name | oid) → bigint
```

설명: 데이터베이스가 사용하는 총 디스크 공간을 반환합니다.

권한: `CONNECT` 권한 또는 `pg_read_all_stats` 역할 필요

예제:
```sql
-- 데이터베이스 크기 확인
SELECT pg_size_pretty(pg_database_size('mydb'));
-- 결과: '1234 MB'

-- 모든 데이터베이스 크기 확인
SELECT datname, pg_size_pretty(pg_database_size(datname))
FROM pg_database
ORDER BY pg_database_size(datname) DESC;
```

#### pg_table_size()

```sql
pg_table_size(regclass) → bigint
```

설명: 테이블이 사용하는 디스크 공간을 반환합니다(인덱스 제외). TOAST 테이블, 자유 공간 맵(FSM), 가시성 맵(VM)을 포함합니다.

#### pg_indexes_size()

```sql
pg_indexes_size(regclass) → bigint
```

설명: 테이블 인덱스가 사용하는 총 디스크 공간을 반환합니다.

#### pg_relation_size()

```sql
pg_relation_size(relation regclass [, fork text]) → bigint
```

설명: 릴레이션 "포크"가 사용하는 디스크 공간을 반환합니다.

fork 옵션:
- `main`: 메인 데이터 포크 (기본값)
- `fsm`: 자유 공간 맵 (Free Space Map)
- `vm`: 가시성 맵 (Visibility Map)
- `init`: 초기화 포크

예제:
```sql
-- 테이블의 메인 데이터 크기
SELECT pg_size_pretty(pg_relation_size('mytable'));

-- FSM 크기
SELECT pg_size_pretty(pg_relation_size('mytable', 'fsm'));
```

#### pg_total_relation_size()

```sql
pg_total_relation_size(regclass) → bigint
```

설명: 총 디스크 공간: `pg_table_size() + pg_indexes_size()`

예제:
```sql
-- 테이블 총 크기 (인덱스 포함)
SELECT pg_size_pretty(pg_total_relation_size('mytable'));
```

#### pg_tablespace_size()

```sql
pg_tablespace_size(name | oid) → bigint
```

설명: 테이블스페이스의 총 디스크 공간을 반환합니다.

권한: `CREATE` 권한 또는 `pg_read_all_stats` 역할 필요 (기본 테이블스페이스 제외)

#### pg_size_pretty()

```sql
pg_size_pretty(bigint | numeric) → text
```

설명: 바이트를 사람이 읽기 쉬운 형식으로 변환합니다.

단위: bytes, kB, MB, GB, TB, PB (2의 거듭제곱)
- 1kB = 1024 bytes
- 1MB = 1048576 bytes

예제:
```sql
SELECT pg_size_pretty(1073741824);
-- 결과: '1024 MB'

SELECT pg_size_pretty(pg_database_size(current_database()));
```

#### pg_size_bytes()

```sql
pg_size_bytes(text) → bigint
```

설명: 사람이 읽기 쉬운 형식을 바이트로 변환합니다.

유효한 단위: bytes, B, kB, MB, GB, TB, PB

예제:
```sql
SELECT pg_size_bytes('1 GB');
-- 결과: 1073741824
```

### 6.2 위치 함수 (Location Functions)

#### pg_relation_filenode()

```sql
pg_relation_filenode(relation regclass) → oid
```

설명: 릴레이션의 파일노드 번호를 반환합니다. 파일 이름의 기본 구성 요소입니다.

참고: 보통 `pg_class.relfilenode`와 동일합니다. 저장소가 없는 릴레이션(뷰)은 `NULL` 반환.

#### pg_relation_filepath()

```sql
pg_relation_filepath(relation regclass) → text
```

설명: PGDATA를 기준으로 한 릴레이션의 전체 파일 경로를 반환합니다.

예제:
```sql
SELECT pg_relation_filepath('mytable');
-- 결과: 'base/16384/16385'
```

#### pg_filenode_relation()

```sql
pg_filenode_relation(tablespace oid, filenode oid) → regclass
```

설명: `pg_relation_filepath()`의 역함수입니다.

매개변수:
- `tablespace`: 테이블스페이스 OID. 기본 테이블스페이스는 `0`
- `filenode`: 파일노드 번호

반환값: 릴레이션이 없거나 임시 릴레이션이면 `NULL`

### 6.3 통계 조작 함수 (Statistics Manipulation Functions)

경고: 변경 사항은 autovacuum 또는 VACUUM/ANALYZE에 의해 덮어씌워질 수 있습니다. 임시 목적으로만 사용하십시오.

복구 중에는 실행할 수 없습니다.

#### pg_restore_relation_stats()

```sql
pg_restore_relation_stats(VARIADIC kwargs "any") → boolean
```

설명: 테이블 수준 통계를 업데이트합니다.

필수 인자:
- `schemaname` (text): 스키마 이름
- `relname` (text): 테이블 이름

선택적 통계:
- `relpages` (integer): 페이지 수
- `reltuples` (real): 튜플 수
- `relallvisible` (integer): 모두 가시 페이지 수
- `relallfrozen` (integer): 모두 동결 페이지 수
- `version` (integer): 소스 버전

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

예제:
```sql
SELECT pg_restore_relation_stats(
    'schemaname', 'myschema',
    'relname',    'mytable',
    'relpages',   173::integer,
    'reltuples',  10000::real
);
```

#### pg_clear_relation_stats()

```sql
pg_clear_relation_stats(schemaname text, relname text) → void
```

설명: 테이블 수준 통계를 새로 생성된 것처럼 지웁니다.

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

#### pg_restore_attribute_stats()

```sql
pg_restore_attribute_stats(VARIADIC kwargs "any") → boolean
```

설명: 컬럼 수준 통계를 생성/업데이트합니다.

필수 인자:
- `schemaname` (text): 스키마 이름
- `relname` (text): 테이블 이름
- `attname` (text) 또는 `attnum` (smallint): 컬럼 이름 또는 번호
- `inherited` (boolean): 상속 여부

선택적 통계 (`pg_stats`에서):
- `avg_width` (integer): 평균 너비
- `null_frac` (real): NULL 비율
- `version` (integer): 소스 버전

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

예제:
```sql
SELECT pg_restore_attribute_stats(
    'schemaname', 'myschema',
    'relname',    'mytable',
    'attname',    'col1',
    'inherited',  false,
    'avg_width',  125::integer,
    'null_frac',  0.5::real
);
```

#### pg_clear_attribute_stats()

```sql
pg_clear_attribute_stats(
  schemaname text,
  relname text,
  attname text,
  inherited boolean
) → void
```

설명: 컬럼 수준 통계를 새로 생성된 것처럼 지웁니다.

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

### 6.4 파티셔닝 정보 함수 (Partitioning Information Functions)

#### pg_partition_tree()

```sql
pg_partition_tree(regclass) → setof record
  (relid regclass, parentrelid regclass, isleaf boolean, level integer)
```

설명: 파티션 트리의 테이블/인덱스 목록을 반환합니다.

반환 필드:
- `relid`: 파티션 OID
- `parentrelid`: 부모 OID
- `isleaf`: 리프 파티션 여부
- `level`: 입력에서 0, 직접 자식은 1, 등

예제:
```sql
-- 파티션 트리 전체 크기
SELECT pg_size_pretty(sum(pg_relation_size(relid))) AS total_size
FROM pg_partition_tree('measurement');

-- 파티션 트리 구조 확인
SELECT * FROM pg_partition_tree('orders');
```

#### pg_partition_ancestors()

```sql
pg_partition_ancestors(regclass) → setof regclass
```

설명: 자기 자신을 포함한 조상 릴레이션 목록을 반환합니다.

#### pg_partition_root()

```sql
pg_partition_root(regclass) → regclass
```

설명: 파티션 트리의 최상위 부모를 반환합니다.

반환값: 파티션/파티션 테이블이 아니면 `NULL`

---

## 7. 인덱스 유지보수 함수 (Index Maintenance Functions)

복구 중에는 실행할 수 없습니다. 슈퍼유저와 인덱스 소유자로 제한됩니다.

### BRIN 인덱스 함수

#### brin_summarize_new_values()

```sql
brin_summarize_new_values(index regclass) → integer
```

설명: BRIN 인덱스에서 요약되지 않은 페이지 범위를 스캔하고 테이블 페이지를 스캔하여 요약을 생성합니다.

반환값: 삽입된 새 요약 수

예제:
```sql
-- 새 값 요약
SELECT brin_summarize_new_values('mytable_brin_idx');
```

#### brin_summarize_range()

```sql
brin_summarize_range(index regclass, blockNumber bigint) → integer
```

설명: 지정된 블록을 포함하는 페이지 범위를 요약합니다. 해당 특정 범위만 처리합니다.

#### brin_desummarize_range()

```sql
brin_desummarize_range(index regclass, blockNumber bigint) → void
```

설명: 페이지 범위를 요약하는 BRIN 인덱스 튜플을 제거합니다.

### GIN 인덱스 함수

#### gin_clean_pending_list()

```sql
gin_clean_pending_list(index regclass) → bigint
```

설명: GIN 인덱스의 "대기 중(pending)" 목록을 정리합니다. 항목을 메인 GIN 구조로 이동합니다.

반환값: 대기 목록에서 제거된 페이지 수. `fastupdate = false`이면 0 반환 (대기 목록 없음)

예제:
```sql
-- 대기 목록 정리
SELECT gin_clean_pending_list('mytable_gin_idx');
```

---

## 8. 일반 파일 접근 함수 (Generic File Access Functions)

클러스터 디렉터리와 `log_directory`로 접근이 제한됩니다. 슈퍼유저 또는 `pg_read_server_files` 역할을 가진 사용자만 사용 가능합니다.

중요: 이러한 함수는 데이터베이스 내부 권한 검사를 우회합니다. 권한 부여 시 주의하십시오.

### 디렉터리 목록 함수

#### pg_ls_dir()

```sql
pg_ls_dir(
  dirname text,
  [missing_ok boolean, include_dot_dirs boolean]
) → setof text
```

설명: 디렉터리의 파일, 디렉터리, 특수 파일 목록을 반환합니다.

매개변수:
- `missing_ok`: `true`이면 없을 때 `NULL` 반환 (기본값: 에러)
- `include_dot_dirs`: `false`(기본값)이면 "."과 ".." 제외

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 데이터 디렉터리 목록
SELECT pg_ls_dir('base');

-- 없어도 에러 안 남
SELECT pg_ls_dir('nonexistent', true, false);
```

#### pg_ls_logdir()

```sql
pg_ls_logdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 서버 로그 디렉터리의 파일 목록을 반환합니다.

참고: 점으로 시작하는 파일, 디렉터리, 특수 파일은 제외됩니다.

권한: 슈퍼유저 및 `pg_monitor` 역할 (권한 부여 가능)

#### pg_ls_waldir()

```sql
pg_ls_waldir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: WAL 디렉터리의 파일 목록을 반환합니다.

권한: 슈퍼유저 및 `pg_monitor` 역할 (권한 부여 가능)

예제:
```sql
-- WAL 파일 목록 및 크기
SELECT name, pg_size_pretty(size) FROM pg_ls_waldir() ORDER BY modification DESC;
```

#### pg_ls_logicalmapdir()

```sql
pg_ls_logicalmapdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: `pg_logical/mappings` 디렉터리의 파일 목록을 반환합니다.

#### pg_ls_logicalsnapdir()

```sql
pg_ls_logicalsnapdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: `pg_logical/snapshots` 디렉터리의 파일 목록을 반환합니다.

#### pg_ls_replslotdir()

```sql
pg_ls_replslotdir(slot_name text) → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 복제 슬롯 디렉터리의 파일 목록을 반환합니다.

#### pg_ls_tmpdir()

```sql
pg_ls_tmpdir([tablespace oid]) → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 테이블스페이스의 임시 디렉터리 파일 목록을 반환합니다.

매개변수:
- `tablespace`: 지정하지 않으면 기본값 `pg_default`

### 파일 읽기 함수

#### pg_read_file()

```sql
pg_read_file(
  filename text,
  [offset bigint, length bigint],
  [missing_ok boolean]
) → text
```

설명: 파일 내용 또는 파일의 일부를 반환합니다.

매개변수:
- `offset`: 음수이면 파일 끝에서부터의 상대 위치
- `missing_ok`: `true`이면 없을 때 `NULL` 반환

참고: 파일 내용을 데이터베이스 인코딩 문자열로 해석합니다. 유효하지 않은 인코딩이면 오류가 발생합니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 전체 파일 읽기
SELECT pg_read_file('postgresql.conf');

-- 처음 100바이트만 읽기
SELECT pg_read_file('postgresql.conf', 0, 100);
```

#### pg_read_binary_file()

```sql
pg_read_binary_file(
  filename text,
  [offset bigint, length bigint],
  [missing_ok boolean]
) → bytea
```

설명: `pg_read_file()`과 같지만 `bytea`로 반환합니다. 인코딩 검사 없음.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- UTF-8 파일을 데이터베이스 인코딩으로 읽기
SELECT convert_from(pg_read_binary_file('file_in_utf8.txt'), 'UTF8');
```

#### pg_stat_file()

```sql
pg_stat_file(filename text [, missing_ok boolean]) → record
  (size bigint, access timestamp with time zone,
   modification timestamp with time zone, change timestamp with time zone,
   creation timestamp with time zone, isdir boolean)
```

설명: 파일 메타데이터를 반환합니다.

반환 필드:
- `size`: 파일 크기
- `access`: 접근 시간
- `modification`: 수정 시간
- `change`: 변경 시간 (Unix)
- `creation`: 생성 시간 (Windows)
- `isdir`: 디렉터리 여부

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
SELECT * FROM pg_stat_file('postgresql.conf');
```

---

## 9. 권고 잠금 함수 (Advisory Lock Functions)

권고 잠금(Advisory Lock)은 애플리케이션 정의 리소스 잠금을 관리합니다. 64비트 키 또는 두 개의 32비트 키(겹치지 않는 공간)로 식별됩니다.

### 잠금 유형

- 배타적 잠금 (Exclusive Lock): 다른 배타적 잠금과 충돌
- 공유 잠금 (Shared Lock): 다른 공유 잠금과 충돌하지 않음, 배타적 잠금만 충돌

### 잠금 수준

- 세션 수준 (Session-level): 해제하거나 세션이 종료될 때까지 유지. 스태킹 지원
- 트랜잭션 수준 (Transaction-level): 트랜잭션 종료 시 자동 해제. 수동 해제 불가

### 배타적 세션 수준 잠금

#### pg_advisory_lock()

```sql
pg_advisory_lock(key bigint | key1 integer, key2 integer) → void
```

설명: 배타적 세션 수준 잠금을 획득합니다. 필요시 대기합니다.

예제:
```sql
-- 64비트 키로 잠금
SELECT pg_advisory_lock(12345);

-- 두 개의 32비트 키로 잠금
SELECT pg_advisory_lock(1, 2);
```

#### pg_try_advisory_lock()

```sql
pg_try_advisory_lock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 배타적 세션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

예제:
```sql
-- 잠금 시도
IF pg_try_advisory_lock(12345) THEN
    -- 잠금 획득 성공, 작업 수행
END IF;
```

### 공유 세션 수준 잠금

#### pg_advisory_lock_shared()

```sql
pg_advisory_lock_shared(key bigint | key1 integer, key2 integer) → void
```

설명: 공유 세션 수준 잠금을 획득합니다. 필요시 대기합니다.

#### pg_try_advisory_lock_shared()

```sql
pg_try_advisory_lock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 공유 세션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

### 세션 수준 잠금 해제

#### pg_advisory_unlock()

```sql
pg_advisory_unlock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 배타적 세션 수준 잠금을 해제합니다.

반환값: 성공적으로 해제되면 `true`, 잠금이 없으면 `false` (SQL 경고 보고)

#### pg_advisory_unlock_shared()

```sql
pg_advisory_unlock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 공유 세션 수준 잠금을 해제합니다.

반환값: 성공적으로 해제되면 `true`, 잠금이 없으면 `false` (SQL 경고 보고)

#### pg_advisory_unlock_all()

```sql
pg_advisory_unlock_all() → void
```

설명: 현재 세션이 보유한 모든 세션 수준 잠금을 해제합니다.

참고: 세션 종료 시 자동으로 호출됩니다 (비정상 종료 포함).

### 트랜잭션 수준 잠금

#### pg_advisory_xact_lock()

```sql
pg_advisory_xact_lock(key bigint | key1 integer, key2 integer) → void
```

설명: 배타적 트랜잭션 수준 잠금을 획득합니다. 필요시 대기합니다. 트랜잭션 종료 시 해제됩니다.

예제:
```sql
BEGIN;
SELECT pg_advisory_xact_lock(12345);
-- 작업 수행
COMMIT; -- 자동으로 잠금 해제
```

#### pg_try_advisory_xact_lock()

```sql
pg_try_advisory_xact_lock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 배타적 트랜잭션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

#### pg_advisory_xact_lock_shared()

```sql
pg_advisory_xact_lock_shared(key bigint | key1 integer, key2 integer) → void
```

설명: 공유 트랜잭션 수준 잠금을 획득합니다. 필요시 대기합니다. 트랜잭션 종료 시 해제됩니다.

#### pg_try_advisory_xact_lock_shared()

```sql
pg_try_advisory_xact_lock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 공유 트랜잭션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

### 권고 잠금 사용 예제

```sql
-- 배치 작업에서 중복 실행 방지
DO $$
DECLARE
    lock_id CONSTANT bigint := 999999;
BEGIN
    IF NOT pg_try_advisory_lock(lock_id) THEN
        RAISE NOTICE '다른 프로세스가 이미 실행 중입니다';
        RETURN;
    END IF;

    -- 배치 작업 수행
    PERFORM process_batch();

    -- 잠금 해제
    PERFORM pg_advisory_unlock(lock_id);
END;
$$;

-- 트랜잭션 수준 잠금으로 동시성 제어
BEGIN;
SELECT pg_advisory_xact_lock(
    ('x' || substr(md5('customer_' || customer_id::text), 1, 16))::bit(64)::bigint
)
FROM customer
WHERE customer_id = 12345;

-- 고객 데이터 처리
UPDATE customer SET ... WHERE customer_id = 12345;

COMMIT; -- 잠금 자동 해제
```

---

## 10. 세션 정보 함수 (Session Information Functions)

세션 및 시스템 정보를 조회하는 함수들입니다.

### 기본 세션 정보

#### current_database() / current_catalog

```sql
current_database() → name
current_catalog → name
```

설명: 현재 데이터베이스 이름을 반환합니다.

#### current_user / session_user / user

```sql
current_user → name
session_user → name
user → name
```

설명:
- `current_user` / `user`: 현재 실행 컨텍스트의 사용자 이름
- `session_user`: 세션 사용자 이름

참고: `current_role`은 `current_user`의 동의어입니다.

#### current_schema / current_schemas()

```sql
current_schema → name
current_schema() → name
current_schemas(boolean) → name[]
```

설명:
- `current_schema`: 검색 경로의 첫 번째 스키마
- `current_schemas(true)`: 암시적 스키마 포함
- `current_schemas(false)`: 명시적 스키마만

예제:
```sql
SELECT current_schema();
-- 결과: 'public'

SELECT current_schemas(true);
-- 결과: '{pg_catalog,public}'
```

### 네트워크 정보

#### inet_client_addr() / inet_client_port()

```sql
inet_client_addr() → inet
inet_client_port() → integer
```

설명: 현재 클라이언트의 IP 주소와 포트를 반환합니다.

#### inet_server_addr() / inet_server_port()

```sql
inet_server_addr() → inet
inet_server_port() → integer
```

설명: 현재 연결에서 서버의 IP 주소와 포트를 반환합니다.

예제:
```sql
SELECT inet_client_addr(), inet_client_port();
-- 결과: '192.168.1.100', 54321

SELECT inet_server_addr(), inet_server_port();
-- 결과: '192.168.1.1', 5432
```

### 프로세스 및 시스템 정보

#### pg_backend_pid()

```sql
pg_backend_pid() → integer
```

설명: 세션에 연결된 서버 프로세스의 PID를 반환합니다.

#### pg_blocking_pids()

```sql
pg_blocking_pids(pid integer) → integer[]
```

설명: 지정된 프로세스를 차단하는 프로세스 ID 배열을 반환합니다.

예제:
```sql
-- 차단된 프로세스와 차단하는 프로세스 확인
SELECT pid, pg_blocking_pids(pid), query
FROM pg_stat_activity
WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

#### pg_conf_load_time()

```sql
pg_conf_load_time() → timestamp with time zone
```

설명: 설정 파일이 마지막으로 로드된 시간을 반환합니다.

#### pg_postmaster_start_time()

```sql
pg_postmaster_start_time() → timestamp with time zone
```

설명: 서버 시작 시간을 반환합니다.

예제:
```sql
-- 서버 가동 시간
SELECT now() - pg_postmaster_start_time() AS uptime;
```

#### pg_current_logfile()

```sql
pg_current_logfile([text]) → text
```

설명: 현재 로그 파일의 경로를 반환합니다.

#### pg_listening_channels()

```sql
pg_listening_channels() → setof text
```

설명: 세션이 수신 대기 중인 알림 채널 집합을 반환합니다.

#### pg_notification_queue_usage()

```sql
pg_notification_queue_usage() → double precision
```

설명: 알림 큐가 차지하는 비율(0-1)을 반환합니다.

---

## 11. 트랜잭션 ID와 스냅샷 (Transaction IDs and Snapshots)

### 트랜잭션 ID 함수

#### pg_current_xact_id()

```sql
pg_current_xact_id() → xid8
```

설명: 현재 트랜잭션 ID를 반환합니다.

#### pg_current_xact_id_if_assigned()

```sql
pg_current_xact_id_if_assigned() → xid8
```

설명: 트랜잭션 ID가 할당된 경우 해당 ID를 반환하고, 없으면 `NULL`을 반환합니다.

#### pg_xact_status()

```sql
pg_xact_status(xid8) → text
```

설명: 커밋 상태를 반환합니다.

반환값: `'in progress'`, `'committed'`, `'aborted'`, 또는 `NULL`

예제:
```sql
SELECT pg_xact_status(pg_current_xact_id());
-- 결과: 'in progress'
```

#### age()

```sql
age(xid) → integer
```

설명: 주어진 XID 이후의 트랜잭션 수를 반환합니다.

예제:
```sql
-- 테이블의 나이 확인 (동결 필요성 판단)
SELECT relname, age(relfrozenxid)
FROM pg_class
WHERE relkind = 'r'
ORDER BY age(relfrozenxid) DESC
LIMIT 10;
```

### 스냅샷 함수

#### pg_current_snapshot()

```sql
pg_current_snapshot() → pg_snapshot
```

설명: 현재 스냅샷을 반환합니다 (`xmin:xmax:xip_list` 형식).

스냅샷 구성 요소:
- `xmin`: 가장 낮은 활성 트랜잭션 ID
- `xmax`: 가장 높은 완료된 트랜잭션 ID + 1
- `xip_list`: 진행 중인 트랜잭션 목록 (형식: "10:20:10,14,15")

#### pg_snapshot_xip()

```sql
pg_snapshot_xip(pg_snapshot) → setof xid8
```

설명: 스냅샷에서 진행 중인 XID들을 반환합니다.

#### pg_snapshot_xmax()

```sql
pg_snapshot_xmax(pg_snapshot) → xid8
```

설명: 스냅샷에서 xmax를 반환합니다.

#### pg_snapshot_xmin()

```sql
pg_snapshot_xmin(pg_snapshot) → xid8
```

설명: 스냅샷에서 xmin을 반환합니다.

#### pg_visible_in_snapshot()

```sql
pg_visible_in_snapshot(xid8, pg_snapshot) → boolean
```

설명: 트랜잭션이 스냅샷에서 보이는지 여부를 반환합니다.

---

## 12. 제어 데이터 함수 (Control Data Functions)

클러스터 전체 정보를 반환합니다(`pg_controldata` 유틸리티와 유사한 정보).

### pg_control_checkpoint()

```sql
pg_control_checkpoint() → record
```

설명: 체크포인트 상태를 반환합니다.

반환 필드:
- `checkpoint_lsn`, `redo_lsn` (pg_lsn)
- `redo_wal_file` (text)
- `timeline_id`, `prev_timeline_id` (integer)
- `full_page_writes` (boolean)
- `next_xid`, `next_oid`, `next_multixact_id`, `next_multi_offset`
- `oldest_xid`, `oldest_xid_dbid`, `oldest_active_xid`
- `oldest_multi_xid`, `oldest_multi_dbid`
- `oldest_commit_ts_xid`, `newest_commit_ts_xid`
- `checkpoint_time` (timestamp with time zone)

예제:
```sql
SELECT * FROM pg_control_checkpoint();
```

### pg_control_system()

```sql
pg_control_system() → record
```

설명: 제어 파일 상태를 반환합니다.

반환 필드:
- `pg_control_version`, `catalog_version_no` (integer)
- `system_identifier` (bigint)
- `pg_control_last_modified` (timestamp with time zone)

### pg_control_init()

```sql
pg_control_init() → record
```

설명: 클러스터 초기화 상태를 반환합니다.

반환 필드:
- `max_data_alignment`, `database_block_size`, `blocks_per_segment` (integer)
- `wal_block_size`, `bytes_per_wal_segment` (integer)
- `max_identifier_length`, `max_index_columns` (integer)
- `max_toast_chunk_size`, `large_object_chunk_size` (integer)
- `float8_pass_by_value` (boolean)
- `data_page_checksum_version` (integer)

### pg_control_recovery()

```sql
pg_control_recovery() → record
```

설명: 복구 상태를 반환합니다.

반환 필드:
- `min_recovery_end_lsn`, `backup_start_lsn`, `backup_end_lsn` (pg_lsn)
- `min_recovery_end_timeline` (integer)
- `end_of_backup_record_required` (boolean)

---

## 13. 버전 정보 함수 (Version Information Functions)

### version()

```sql
version() → text
```

설명: PostgreSQL 서버 버전 문자열을 반환합니다.

예제:
```sql
SELECT version();
-- 결과: 'PostgreSQL 16.0 on x86_64-pc-linux-gnu, compiled by gcc ...'
```

### unicode_version()

```sql
unicode_version() → text
```

설명: PostgreSQL에서 사용하는 Unicode 버전을 반환합니다.

### icu_unicode_version()

```sql
icu_unicode_version() → text
```

설명: ICU Unicode 버전을 반환합니다. ICU 지원이 없으면 `NULL` 반환.

---

## 요약 (Summary)

| 카테고리 | 주요 함수 | 개수 |
|----------|----------|------|
| 서버 신호 (Server Signaling) | `pg_cancel_backend`, `pg_terminate_backend`, `pg_log_backend_memory_contexts`, `pg_reload_conf`, `pg_rotate_logfile` | 5 |
| 백업 제어 (Backup Control) | `pg_backup_start`, `pg_backup_stop`, `pg_create_restore_point`, WAL 위치 함수 | 12 |
| 복구 제어 (Recovery Control) | 복구 정보 (5) + 제어 (5) | 10 |
| 스냅샷 (Snapshots) | `pg_export_snapshot`, `pg_log_standby_snapshot` | 2 |
| 복제 (Replication) | 물리적 슬롯 (3) + 논리적 슬롯 (4) + 디코딩 (4) + 원점 (9) + 메시지 (1) + 동기화 (1) | 22 |
| DB 객체 (DB Objects) | 크기 (9) + 위치 (3) + 통계 (4) + 파티셔닝 (3) | 19 |
| 인덱스 유지보수 (Index Maintenance) | BRIN (3) + GIN (1) | 4 |
| 파일 접근 (File Access) | 디렉터리 (9) + 읽기 (3) | 12 |
| 권고 잠금 (Advisory Locks) | 세션 배타 (2) + 세션 공유 (2) + 해제 (3) + 트랜잭션 배타 (2) + 트랜잭션 공유 (2) | 11 |
| 세션 정보 (Session Info) | 기본 (10) + 네트워크 (4) + 프로세스 (6) | 20 |
| 트랜잭션/스냅샷 (Txn/Snapshot) | 트랜잭션 ID (4) + 스냅샷 (6) | 10 |
| 제어 데이터 (Control Data) | `pg_control_*` 함수들 | 4 |
| 버전 정보 (Version) | `version`, `unicode_version`, `icu_unicode_version` | 3 |
| 총계 | | 134 |

---

## 참고 자료 (References)

- PostgreSQL 공식 문서: [System Administration Functions](https://www.postgresql.org/docs/current/functions-admin.html)
- PostgreSQL 공식 문서: [System Information Functions](https://www.postgresql.org/docs/current/functions-info.html)
- PostgreSQL 공식 문서: [Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- PostgreSQL 공식 문서: [High Availability, Load Balancing, and Replication](https://www.postgresql.org/docs/current/high-availability.html)

---

## PostgreSQL 18 오류 코드 (Error Codes)

### 개요

PostgreSQL 서버가 내보내는 모든 메시지에는 5자리 오류 코드가 부여되며, 이는 SQL 표준의 "SQLSTATE" 코드 규약을 따릅니다. 특정 오류 조건을 감지해야 하는 애플리케이션은 텍스트 오류 메시지 대신 오류 코드를 기준으로 판단해야 합니다. 오류 코드는 PostgreSQL 릴리스 간에 변경될 가능성이 낮고, 서버 오류 메시지의 지역화(localization)에 영향을 받지 않습니다.

---

### SQLSTATE 오류 코드 체계

#### 코드 구조

SQLSTATE 코드는 5자리 문자열로 구성됩니다:

- 처음 2자리: 오류 클래스(Error Class)를 나타냅니다
- 나머지 3자리: 해당 클래스 내의 특정 조건(Specific Condition)을 나타냅니다

예를 들어, `23505` 코드는:
- `23` = 무결성 제약 위반 클래스 (Integrity Constraint Violation)
- `505` = 고유 위반 (Unique Violation)

#### 코드 분류

| 코드 패턴 | 의미 |
|-----------|------|
| `XX000` | 클래스 내 정의되지 않은 오류 |
| `XXPXX` | PostgreSQL 전용 오류 코드 (P 포함) |

```sql
-- 오류 코드 확인 예제
DO $$
BEGIN
    INSERT INTO users(id, email) VALUES (1, 'test@example.com');
EXCEPTION
    WHEN unique_violation THEN  -- SQLSTATE '23505'
        RAISE NOTICE '중복된 키가 존재합니다: %', SQLERRM;
    WHEN foreign_key_violation THEN  -- SQLSTATE '23503'
        RAISE NOTICE '외래 키 제약 위반: %', SQLERRM;
END;
$$;
```

---

### 오류 클래스 및 코드 목록

#### Class 00 - 성공적 완료 (Successful Completion)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `00000` | `successful_completion` | 작업이 성공적으로 완료됨 |

---

#### Class 01 - 경고 (Warning)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `01000` | `warning` | 일반 경고 |
| `0100C` | `dynamic_result_sets_returned` | 동적 결과 집합이 반환됨 |
| `01008` | `implicit_zero_bit_padding` | 암시적 제로 비트 패딩 |
| `01003` | `null_value_eliminated_in_set_function` | 집합 함수에서 NULL 값이 제거됨 |
| `01007` | `privilege_not_granted` | 권한이 부여되지 않음 |
| `01006` | `privilege_not_revoked` | 권한이 취소되지 않음 |
| `01004` | `string_data_right_truncation` | 문자열 데이터 오른쪽 잘림 |
| `01P01` | `deprecated_feature` | 더 이상 사용되지 않는 기능 |

---

#### Class 02 - 데이터 없음 (No Data)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `02000` | `no_data` | 데이터 없음 |
| `02001` | `no_additional_dynamic_result_sets_returned` | 추가 동적 결과 집합이 반환되지 않음 |

---

#### Class 03 - SQL 문 미완료 (SQL Statement Not Yet Complete)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `03000` | `sql_statement_not_yet_complete` | SQL 문이 아직 완료되지 않음 |

---

#### Class 08 - 연결 예외 (Connection Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `08000` | `connection_exception` | 연결 예외 |
| `08003` | `connection_does_not_exist` | 연결이 존재하지 않음 |
| `08006` | `connection_failure` | 연결 실패 |
| `08001` | `sqlclient_unable_to_establish_sqlconnection` | SQL 클라이언트가 SQL 연결을 설정할 수 없음 |
| `08004` | `sqlserver_rejected_establishment_of_sqlconnection` | SQL 서버가 SQL 연결 설정을 거부함 |
| `08007` | `transaction_resolution_unknown` | 트랜잭션 해결 상태 알 수 없음 |
| `08P01` | `protocol_violation` | 프로토콜 위반 |

```sql
-- 연결 오류 처리 예제 (PL/pgSQL)
DO $$
BEGIN
    PERFORM dblink_connect('myconn', 'host=remote_host dbname=mydb');
EXCEPTION
    WHEN connection_exception THEN
        RAISE NOTICE '연결 오류 발생: %', SQLERRM;
    WHEN SQLSTATE '08006' THEN
        RAISE NOTICE '연결 실패: 원격 서버에 연결할 수 없습니다';
END;
$$;
```

---

#### Class 09 - 트리거 동작 예외 (Triggered Action Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `09000` | `triggered_action_exception` | 트리거 동작 예외 |

---

#### Class 0A - 지원되지 않는 기능 (Feature Not Supported)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0A000` | `feature_not_supported` | 기능이 지원되지 않음 |

```sql
-- 지원되지 않는 기능 예제
DO $$
BEGIN
    -- 지원되지 않는 작업 시도
    EXECUTE 'SOME_UNSUPPORTED_FEATURE';
EXCEPTION
    WHEN feature_not_supported THEN
        RAISE NOTICE '이 기능은 현재 PostgreSQL 버전에서 지원되지 않습니다';
END;
$$;
```

---

#### Class 0B - 잘못된 트랜잭션 시작 (Invalid Transaction Initiation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0B000` | `invalid_transaction_initiation` | 잘못된 트랜잭션 시작 |

---

#### Class 0F - 로케이터 예외 (Locator Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0F000` | `locator_exception` | 로케이터 예외 |
| `0F001` | `invalid_locator_specification` | 잘못된 로케이터 지정 |

---

#### Class 0L - 잘못된 권한 부여자 (Invalid Grantor)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0L000` | `invalid_grantor` | 잘못된 권한 부여자 |
| `0LP01` | `invalid_grant_operation` | 잘못된 권한 부여 작업 |

---

#### Class 0P - 잘못된 역할 지정 (Invalid Role Specification)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0P000` | `invalid_role_specification` | 잘못된 역할 지정 |

---

#### Class 0Z - 진단 예외 (Diagnostics Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0Z000` | `diagnostics_exception` | 진단 예외 |
| `0Z002` | `stacked_diagnostics_accessed_without_active_handler` | 활성 핸들러 없이 스택 진단에 접근함 |

---

#### Class 10 - XQuery 오류 (XQuery Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `10608` | `invalid_argument_for_xquery` | XQuery에 대한 잘못된 인수 |

---

#### Class 20 - Case를 찾을 수 없음 (Case Not Found)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `20000` | `case_not_found` | CASE 문에서 일치하는 조건을 찾을 수 없음 |

```sql
-- CASE NOT FOUND 예제
CREATE OR REPLACE FUNCTION get_grade(score INTEGER)
RETURNS TEXT AS $$
BEGIN
    CASE
        WHEN score >= 90 THEN RETURN 'A';
        WHEN score >= 80 THEN RETURN 'B';
        WHEN score >= 70 THEN RETURN 'C';
        -- ELSE 절이 없으면 case_not_found 발생 가능
    END CASE;
EXCEPTION
    WHEN case_not_found THEN
        RETURN 'F';
END;
$$ LANGUAGE plpgsql;
```

---

#### Class 21 - 카디널리티 위반 (Cardinality Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `21000` | `cardinality_violation` | 카디널리티 위반 (서브쿼리가 여러 행 반환) |

```sql
-- 카디널리티 위반 예제
DO $$
DECLARE
    v_name TEXT;
BEGIN
    -- 여러 행을 반환하는 서브쿼리를 단일 값에 할당하려고 할 때
    SELECT name INTO STRICT v_name FROM users;  -- 여러 행이 있으면 오류
EXCEPTION
    WHEN cardinality_violation THEN
        RAISE NOTICE '쿼리가 여러 행을 반환했습니다';
    WHEN too_many_rows THEN
        RAISE NOTICE '결과가 너무 많습니다';
END;
$$;
```

---

#### Class 22 - 데이터 예외 (Data Exception)

데이터 처리 과정에서 발생하는 다양한 오류를 포함합니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `22000` | `data_exception` | 데이터 예외 |
| `2202E` | `array_subscript_error` | 배열 첨자 오류 |
| `22021` | `character_not_in_repertoire` | 문자가 레퍼토리에 없음 |
| `22008` | `datetime_field_overflow` | 날짜/시간 필드 오버플로우 |
| `22012` | `division_by_zero` | 0으로 나누기 |
| `22005` | `error_in_assignment` | 할당 오류 |
| `2200B` | `escape_character_conflict` | 이스케이프 문자 충돌 |
| `22022` | `indicator_overflow` | 지시자 오버플로우 |
| `22015` | `interval_field_overflow` | 간격 필드 오버플로우 |
| `2201E` | `invalid_argument_for_logarithm` | 로그 함수에 대한 잘못된 인수 |
| `22014` | `invalid_argument_for_ntile_function` | NTILE 함수에 대한 잘못된 인수 |
| `22016` | `invalid_argument_for_nth_value_function` | NTH_VALUE 함수에 대한 잘못된 인수 |
| `2201F` | `invalid_argument_for_power_function` | 거듭제곱 함수에 대한 잘못된 인수 |
| `2201G` | `invalid_argument_for_width_bucket_function` | WIDTH_BUCKET 함수에 대한 잘못된 인수 |
| `22018` | `invalid_character_value_for_cast` | 캐스트에 대한 잘못된 문자 값 |
| `22007` | `invalid_datetime_format` | 잘못된 날짜/시간 형식 |
| `22019` | `invalid_escape_character` | 잘못된 이스케이프 문자 |
| `2200D` | `invalid_escape_octet` | 잘못된 이스케이프 옥텟 |
| `22025` | `invalid_escape_sequence` | 잘못된 이스케이프 시퀀스 |
| `22P06` | `nonstandard_use_of_escape_character` | 비표준 이스케이프 문자 사용 |
| `22010` | `invalid_indicator_parameter_value` | 잘못된 지시자 매개변수 값 |
| `22023` | `invalid_parameter_value` | 잘못된 매개변수 값 |
| `22013` | `invalid_preceding_or_following_size` | 잘못된 PRECEDING 또는 FOLLOWING 크기 |
| `2201B` | `invalid_regular_expression` | 잘못된 정규 표현식 |
| `2201W` | `invalid_row_count_in_limit_clause` | LIMIT 절의 잘못된 행 수 |
| `2201X` | `invalid_row_count_in_result_offset_clause` | 결과 오프셋 절의 잘못된 행 수 |
| `2202H` | `invalid_tablesample_argument` | 잘못된 TABLESAMPLE 인수 |
| `2202G` | `invalid_tablesample_repeat` | 잘못된 TABLESAMPLE REPEAT |
| `22009` | `invalid_time_zone_displacement_value` | 잘못된 시간대 변위 값 |
| `2200C` | `invalid_use_of_escape_character` | 잘못된 이스케이프 문자 사용 |
| `2200G` | `most_specific_type_mismatch` | 가장 구체적인 타입 불일치 |
| `22004` | `null_value_not_allowed` | NULL 값이 허용되지 않음 |
| `22002` | `null_value_no_indicator_parameter` | NULL 값에 지시자 매개변수 없음 |
| `22003` | `numeric_value_out_of_range` | 숫자 값이 범위를 벗어남 |
| `2200H` | `sequence_generator_limit_exceeded` | 시퀀스 생성기 한계 초과 |
| `22026` | `string_data_length_mismatch` | 문자열 데이터 길이 불일치 |
| `22001` | `string_data_right_truncation` | 문자열 데이터 오른쪽 잘림 |
| `22011` | `substring_error` | SUBSTRING 오류 |
| `22027` | `trim_error` | TRIM 오류 |
| `22024` | `unterminated_c_string` | 종료되지 않은 C 문자열 |
| `2200F` | `zero_length_character_string` | 길이가 0인 문자열 |
| `22P01` | `floating_point_exception` | 부동 소수점 예외 |
| `22P02` | `invalid_text_representation` | 잘못된 텍스트 표현 |
| `22P03` | `invalid_binary_representation` | 잘못된 바이너리 표현 |
| `22P04` | `bad_copy_file_format` | 잘못된 COPY 파일 형식 |
| `22P05` | `untranslatable_character` | 변환할 수 없는 문자 |
| `2200L` | `not_an_xml_document` | XML 문서가 아님 |
| `2200M` | `invalid_xml_document` | 잘못된 XML 문서 |
| `2200N` | `invalid_xml_content` | 잘못된 XML 내용 |
| `2200S` | `invalid_xml_comment` | 잘못된 XML 주석 |
| `2200T` | `invalid_xml_processing_instruction` | 잘못된 XML 처리 명령 |

##### JSON 관련 오류

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `22030` | `duplicate_json_object_key_value` | 중복된 JSON 객체 키 값 |
| `22031` | `invalid_argument_for_sql_json_datetime_function` | SQL/JSON 날짜/시간 함수에 대한 잘못된 인수 |
| `22032` | `invalid_json_text` | 잘못된 JSON 텍스트 |
| `22033` | `invalid_sql_json_subscript` | 잘못된 SQL/JSON 첨자 |
| `22034` | `more_than_one_sql_json_item` | SQL/JSON 항목이 하나 이상 |
| `22035` | `no_sql_json_item` | SQL/JSON 항목 없음 |
| `22036` | `non_numeric_sql_json_item` | 숫자가 아닌 SQL/JSON 항목 |
| `22037` | `non_unique_keys_in_a_json_object` | JSON 객체에 고유하지 않은 키 |
| `22038` | `singleton_sql_json_item_required` | 단일 SQL/JSON 항목 필요 |
| `22039` | `sql_json_array_not_found` | SQL/JSON 배열을 찾을 수 없음 |
| `2203A` | `sql_json_member_not_found` | SQL/JSON 멤버를 찾을 수 없음 |
| `2203B` | `sql_json_number_not_found` | SQL/JSON 숫자를 찾을 수 없음 |
| `2203C` | `sql_json_object_not_found` | SQL/JSON 객체를 찾을 수 없음 |
| `2203D` | `too_many_json_array_elements` | JSON 배열 요소가 너무 많음 |
| `2203E` | `too_many_json_object_members` | JSON 객체 멤버가 너무 많음 |
| `2203F` | `sql_json_scalar_required` | SQL/JSON 스칼라 필요 |
| `2203G` | `sql_json_item_cannot_be_cast_to_target_type` | SQL/JSON 항목을 대상 타입으로 캐스트할 수 없음 |

```sql
-- 데이터 예외 처리 예제
DO $$
DECLARE
    v_result NUMERIC;
BEGIN
    -- 0으로 나누기
    v_result := 10 / 0;
EXCEPTION
    WHEN division_by_zero THEN
        RAISE NOTICE '0으로 나눌 수 없습니다';
    WHEN numeric_value_out_of_range THEN
        RAISE NOTICE '숫자가 허용 범위를 초과했습니다';
END;
$$;

-- 잘못된 텍스트 표현 예제
DO $$
BEGIN
    PERFORM '123abc'::INTEGER;
EXCEPTION
    WHEN invalid_text_representation THEN
        RAISE NOTICE '문자열을 숫자로 변환할 수 없습니다: %', SQLERRM;
END;
$$;
```

---

#### Class 23 - 무결성 제약 위반 (Integrity Constraint Violation)

데이터 무결성과 관련된 오류 클래스입니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `23000` | `integrity_constraint_violation` | 무결성 제약 위반 |
| `23001` | `restrict_violation` | RESTRICT 위반 |
| `23502` | `not_null_violation` | NOT NULL 위반 |
| `23503` | `foreign_key_violation` | 외래 키 위반 |
| `23505` | `unique_violation` | 고유 제약 위반 |
| `23514` | `check_violation` | CHECK 제약 위반 |
| `23P01` | `exclusion_violation` | 배제 제약 위반 |

```sql
-- 무결성 제약 위반 처리 예제
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price NUMERIC CHECK (price > 0),
    category_id INTEGER REFERENCES categories(id)
);

-- 오류 처리 함수
CREATE OR REPLACE FUNCTION safe_insert_product(
    p_name VARCHAR,
    p_price NUMERIC,
    p_category_id INTEGER
) RETURNS TEXT AS $$
BEGIN
    INSERT INTO products (name, price, category_id)
    VALUES (p_name, p_price, p_category_id);
    RETURN '성공';
EXCEPTION
    WHEN not_null_violation THEN
        RETURN '오류: 필수 필드가 누락되었습니다';
    WHEN unique_violation THEN
        RETURN '오류: 중복된 값이 존재합니다';
    WHEN foreign_key_violation THEN
        RETURN '오류: 참조하는 카테고리가 존재하지 않습니다';
    WHEN check_violation THEN
        RETURN '오류: 가격은 0보다 커야 합니다';
    WHEN exclusion_violation THEN
        RETURN '오류: 배제 제약 조건을 위반했습니다';
END;
$$ LANGUAGE plpgsql;

-- 사용 예제
SELECT safe_insert_product('노트북', 1500000, 1);
SELECT safe_insert_product(NULL, 1000, 1);        -- not_null_violation
SELECT safe_insert_product('키보드', -100, 1);    -- check_violation
SELECT safe_insert_product('마우스', 50000, 999); -- foreign_key_violation
```

---

#### Class 24 - 잘못된 커서 상태 (Invalid Cursor State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `24000` | `invalid_cursor_state` | 잘못된 커서 상태 |

---

#### Class 25 - 잘못된 트랜잭션 상태 (Invalid Transaction State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `25000` | `invalid_transaction_state` | 잘못된 트랜잭션 상태 |
| `25001` | `active_sql_transaction` | 활성 SQL 트랜잭션 |
| `25002` | `branch_transaction_already_active` | 분기 트랜잭션이 이미 활성화됨 |
| `25008` | `held_cursor_requires_same_isolation_level` | 보유 커서가 동일한 격리 수준 필요 |
| `25003` | `inappropriate_access_mode_for_branch_transaction` | 분기 트랜잭션에 부적절한 접근 모드 |
| `25004` | `inappropriate_isolation_level_for_branch_transaction` | 분기 트랜잭션에 부적절한 격리 수준 |
| `25005` | `no_active_sql_transaction_for_branch_transaction` | 분기 트랜잭션을 위한 활성 SQL 트랜잭션 없음 |
| `25006` | `read_only_sql_transaction` | 읽기 전용 SQL 트랜잭션 |
| `25007` | `schema_and_data_statement_mixing_not_supported` | 스키마와 데이터 문 혼합이 지원되지 않음 |
| `25P01` | `no_active_sql_transaction` | 활성 SQL 트랜잭션 없음 |
| `25P02` | `in_failed_sql_transaction` | 실패한 SQL 트랜잭션 내에 있음 |
| `25P03` | `idle_in_transaction_session_timeout` | 유휴 트랜잭션 세션 타임아웃 |
| `25P04` | `transaction_timeout` | 트랜잭션 타임아웃 |

```sql
-- 트랜잭션 상태 오류 처리 예제
DO $$
BEGIN
    -- 읽기 전용 트랜잭션에서 쓰기 시도
    SET TRANSACTION READ ONLY;
    INSERT INTO logs (message) VALUES ('test');
EXCEPTION
    WHEN read_only_sql_transaction THEN
        RAISE NOTICE '읽기 전용 트랜잭션에서는 데이터를 수정할 수 없습니다';
    WHEN in_failed_sql_transaction THEN
        RAISE NOTICE '이전 오류로 인해 트랜잭션이 실패한 상태입니다';
END;
$$;
```

---

#### Class 26 - 잘못된 SQL 문 이름 (Invalid SQL Statement Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `26000` | `invalid_sql_statement_name` | 잘못된 SQL 문 이름 |

---

#### Class 27 - 트리거된 데이터 변경 위반 (Triggered Data Change Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `27000` | `triggered_data_change_violation` | 트리거된 데이터 변경 위반 |

---

#### Class 28 - 잘못된 인증 지정 (Invalid Authorization Specification)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `28000` | `invalid_authorization_specification` | 잘못된 인증 지정 |
| `28P01` | `invalid_password` | 잘못된 비밀번호 |

---

#### Class 2B - 종속 권한 기술자 존재 (Dependent Privilege Descriptors Still Exist)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2B000` | `dependent_privilege_descriptors_still_exist` | 종속 권한 기술자가 여전히 존재함 |
| `2BP01` | `dependent_objects_still_exist` | 종속 객체가 여전히 존재함 |

```sql
-- 종속 객체 존재 오류 처리
DO $$
BEGIN
    DROP TABLE parent_table;  -- 종속된 자식 테이블이 있는 경우
EXCEPTION
    WHEN dependent_objects_still_exist THEN
        RAISE NOTICE '이 테이블을 참조하는 다른 객체가 존재합니다. CASCADE를 사용하세요.';
END;
$$;
```

---

#### Class 2D - 잘못된 트랜잭션 종료 (Invalid Transaction Termination)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2D000` | `invalid_transaction_termination` | 잘못된 트랜잭션 종료 |

---

#### Class 2F - SQL 루틴 예외 (SQL Routine Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2F000` | `sql_routine_exception` | SQL 루틴 예외 |
| `2F005` | `function_executed_no_return_statement` | 함수가 RETURN 문 없이 실행됨 |
| `2F002` | `modifying_sql_data_not_permitted` | SQL 데이터 수정이 허용되지 않음 |
| `2F003` | `prohibited_sql_statement_attempted` | 금지된 SQL 문 시도 |
| `2F004` | `reading_sql_data_not_permitted` | SQL 데이터 읽기가 허용되지 않음 |

---

#### Class 34 - 잘못된 커서 이름 (Invalid Cursor Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `34000` | `invalid_cursor_name` | 잘못된 커서 이름 |

---

#### Class 38 - 외부 루틴 예외 (External Routine Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `38000` | `external_routine_exception` | 외부 루틴 예외 |
| `38001` | `containing_sql_not_permitted` | 포함된 SQL이 허용되지 않음 |
| `38002` | `modifying_sql_data_not_permitted` | SQL 데이터 수정이 허용되지 않음 |
| `38003` | `prohibited_sql_statement_attempted` | 금지된 SQL 문 시도 |
| `38004` | `reading_sql_data_not_permitted` | SQL 데이터 읽기가 허용되지 않음 |

---

#### Class 39 - 외부 루틴 호출 예외 (External Routine Invocation Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `39000` | `external_routine_invocation_exception` | 외부 루틴 호출 예외 |
| `39001` | `invalid_sqlstate_returned` | 잘못된 SQLSTATE 반환됨 |
| `39004` | `null_value_not_allowed` | NULL 값이 허용되지 않음 |
| `39P01` | `trigger_protocol_violated` | 트리거 프로토콜 위반 |
| `39P02` | `srf_protocol_violated` | SRF(Set-Returning Function) 프로토콜 위반 |
| `39P03` | `event_trigger_protocol_violated` | 이벤트 트리거 프로토콜 위반 |

---

#### Class 3B - 세이브포인트 예외 (Savepoint Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3B000` | `savepoint_exception` | 세이브포인트 예외 |
| `3B001` | `invalid_savepoint_specification` | 잘못된 세이브포인트 지정 |

---

#### Class 3D - 잘못된 카탈로그 이름 (Invalid Catalog Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3D000` | `invalid_catalog_name` | 잘못된 카탈로그 이름 |

---

#### Class 3F - 잘못된 스키마 이름 (Invalid Schema Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3F000` | `invalid_schema_name` | 잘못된 스키마 이름 |

---

#### Class 40 - 트랜잭션 롤백 (Transaction Rollback)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `40000` | `transaction_rollback` | 트랜잭션 롤백 |
| `40002` | `transaction_integrity_constraint_violation` | 트랜잭션 무결성 제약 위반 |
| `40001` | `serialization_failure` | 직렬화 실패 |
| `40003` | `statement_completion_unknown` | 문 완료 상태 알 수 없음 |
| `40P01` | `deadlock_detected` | 교착 상태 감지 |

```sql
-- 트랜잭션 롤백 및 교착 상태 처리 예제
CREATE OR REPLACE FUNCTION transfer_funds(
    p_from_account INTEGER,
    p_to_account INTEGER,
    p_amount NUMERIC
) RETURNS TEXT AS $$
DECLARE
    v_retry_count INTEGER := 0;
    v_max_retries INTEGER := 3;
BEGIN
    LOOP
        BEGIN
            -- 자금 이체 로직
            UPDATE accounts
            SET balance = balance - p_amount
            WHERE id = p_from_account;

            UPDATE accounts
            SET balance = balance + p_amount
            WHERE id = p_to_account;

            RETURN '이체 성공';

        EXCEPTION
            WHEN deadlock_detected THEN
                v_retry_count := v_retry_count + 1;
                IF v_retry_count >= v_max_retries THEN
                    RAISE EXCEPTION '교착 상태로 인해 최대 재시도 횟수를 초과했습니다';
                END IF;
                RAISE NOTICE '교착 상태 감지, 재시도 중... (%/%)', v_retry_count, v_max_retries;
                PERFORM pg_sleep(0.1 * v_retry_count);  -- 백오프

            WHEN serialization_failure THEN
                v_retry_count := v_retry_count + 1;
                IF v_retry_count >= v_max_retries THEN
                    RAISE EXCEPTION '직렬화 실패로 인해 최대 재시도 횟수를 초과했습니다';
                END IF;
                RAISE NOTICE '직렬화 실패, 재시도 중... (%/%)', v_retry_count, v_max_retries;
                PERFORM pg_sleep(0.1 * v_retry_count);
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

---

#### Class 42 - 구문 오류 또는 접근 규칙 위반 (Syntax Error or Access Rule Violation)

가장 자주 발생하는 오류 클래스 중 하나입니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `42000` | `syntax_error_or_access_rule_violation` | 구문 오류 또는 접근 규칙 위반 |
| `42601` | `syntax_error` | 구문 오류 |
| `42501` | `insufficient_privilege` | 권한 부족 |
| `42846` | `cannot_coerce` | 변환할 수 없음 |
| `42803` | `grouping_error` | 그룹화 오류 |
| `42P20` | `windowing_error` | 윈도우 함수 오류 |
| `42P19` | `invalid_recursion` | 잘못된 재귀 |
| `42830` | `invalid_foreign_key` | 잘못된 외래 키 |
| `42602` | `invalid_name` | 잘못된 이름 |
| `42622` | `name_too_long` | 이름이 너무 김 |
| `42939` | `reserved_name` | 예약된 이름 |
| `42804` | `datatype_mismatch` | 데이터 타입 불일치 |
| `42P18` | `indeterminate_datatype` | 불확정 데이터 타입 |
| `42P21` | `collation_mismatch` | 정렬 규칙 불일치 |
| `42P22` | `indeterminate_collation` | 불확정 정렬 규칙 |
| `42809` | `wrong_object_type` | 잘못된 객체 타입 |
| `428C9` | `generated_always` | GENERATED ALWAYS 제약 |
| `42703` | `undefined_column` | 정의되지 않은 열 |
| `42883` | `undefined_function` | 정의되지 않은 함수 |
| `42P01` | `undefined_table` | 정의되지 않은 테이블 |
| `42P02` | `undefined_parameter` | 정의되지 않은 매개변수 |
| `42704` | `undefined_object` | 정의되지 않은 객체 |
| `42701` | `duplicate_column` | 중복된 열 |
| `42P03` | `duplicate_cursor` | 중복된 커서 |
| `42P04` | `duplicate_database` | 중복된 데이터베이스 |
| `42723` | `duplicate_function` | 중복된 함수 |
| `42P05` | `duplicate_prepared_statement` | 중복된 준비된 문 |
| `42P06` | `duplicate_schema` | 중복된 스키마 |
| `42P07` | `duplicate_table` | 중복된 테이블 |
| `42712` | `duplicate_alias` | 중복된 별칭 |
| `42710` | `duplicate_object` | 중복된 객체 |
| `42702` | `ambiguous_column` | 모호한 열 |
| `42725` | `ambiguous_function` | 모호한 함수 |
| `42P08` | `ambiguous_parameter` | 모호한 매개변수 |
| `42P09` | `ambiguous_alias` | 모호한 별칭 |
| `42P10` | `invalid_column_reference` | 잘못된 열 참조 |
| `42611` | `invalid_column_definition` | 잘못된 열 정의 |
| `42P11` | `invalid_cursor_definition` | 잘못된 커서 정의 |
| `42P12` | `invalid_database_definition` | 잘못된 데이터베이스 정의 |
| `42P13` | `invalid_function_definition` | 잘못된 함수 정의 |
| `42P14` | `invalid_prepared_statement_definition` | 잘못된 준비된 문 정의 |
| `42P15` | `invalid_schema_definition` | 잘못된 스키마 정의 |
| `42P16` | `invalid_table_definition` | 잘못된 테이블 정의 |
| `42P17` | `invalid_object_definition` | 잘못된 객체 정의 |

```sql
-- 구문 및 접근 오류 처리 예제
DO $$
BEGIN
    -- 존재하지 않는 테이블 참조
    PERFORM * FROM nonexistent_table;
EXCEPTION
    WHEN undefined_table THEN
        RAISE NOTICE '테이블이 존재하지 않습니다: %', SQLERRM;
    WHEN undefined_column THEN
        RAISE NOTICE '열이 존재하지 않습니다: %', SQLERRM;
    WHEN insufficient_privilege THEN
        RAISE NOTICE '이 작업을 수행할 권한이 없습니다';
    WHEN syntax_error THEN
        RAISE NOTICE 'SQL 구문 오류: %', SQLERRM;
END;
$$;

-- 중복 객체 처리 예제
DO $$
BEGIN
    CREATE TABLE existing_table (id INT);
EXCEPTION
    WHEN duplicate_table THEN
        RAISE NOTICE '테이블이 이미 존재합니다. CREATE TABLE IF NOT EXISTS를 사용하세요.';
    WHEN duplicate_object THEN
        RAISE NOTICE '객체가 이미 존재합니다';
END;
$$;
```

---

#### Class 44 - WITH CHECK OPTION 위반 (WITH CHECK OPTION Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `44000` | `with_check_option_violation` | WITH CHECK OPTION 위반 |

```sql
-- WITH CHECK OPTION 위반 예제
CREATE VIEW active_users AS
    SELECT * FROM users WHERE status = 'active'
    WITH CHECK OPTION;

-- 뷰를 통해 비활성 상태로 변경하려고 하면 오류 발생
DO $$
BEGIN
    UPDATE active_users SET status = 'inactive' WHERE id = 1;
EXCEPTION
    WHEN with_check_option_violation THEN
        RAISE NOTICE '뷰의 WITH CHECK OPTION 조건을 위반하는 변경은 허용되지 않습니다';
END;
$$;
```

---

#### Class 53 - 리소스 부족 (Insufficient Resources)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `53000` | `insufficient_resources` | 리소스 부족 |
| `53100` | `disk_full` | 디스크 가득 참 |
| `53200` | `out_of_memory` | 메모리 부족 |
| `53300` | `too_many_connections` | 연결이 너무 많음 |
| `53400` | `configuration_limit_exceeded` | 구성 한계 초과 |

```sql
-- 리소스 부족 오류 처리 (애플리케이션 레벨)
DO $$
BEGIN
    -- 대용량 데이터 처리
    PERFORM process_large_dataset();
EXCEPTION
    WHEN out_of_memory THEN
        RAISE NOTICE '메모리가 부족합니다. work_mem 설정을 확인하세요.';
    WHEN disk_full THEN
        RAISE NOTICE '디스크 공간이 부족합니다. 디스크 정리가 필요합니다.';
    WHEN too_many_connections THEN
        RAISE NOTICE '최대 연결 수를 초과했습니다. max_connections 설정을 확인하세요.';
END;
$$;
```

---

#### Class 54 - 프로그램 한계 초과 (Program Limit Exceeded)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `54000` | `program_limit_exceeded` | 프로그램 한계 초과 |
| `54001` | `statement_too_complex` | 문이 너무 복잡함 |
| `54011` | `too_many_columns` | 열이 너무 많음 |
| `54023` | `too_many_arguments` | 인수가 너무 많음 |

---

#### Class 55 - 객체가 필수 상태가 아님 (Object Not In Prerequisite State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `55000` | `object_not_in_prerequisite_state` | 객체가 필수 상태가 아님 |
| `55006` | `object_in_use` | 객체가 사용 중 |
| `55P02` | `cant_change_runtime_param` | 런타임 매개변수를 변경할 수 없음 |
| `55P03` | `lock_not_available` | 잠금을 사용할 수 없음 |
| `55P04` | `unsafe_new_enum_value_usage` | 안전하지 않은 새 열거형 값 사용 |

```sql
-- 잠금 관련 오류 처리
DO $$
BEGIN
    -- NOWAIT 옵션으로 잠금 시도
    LOCK TABLE critical_table IN ACCESS EXCLUSIVE MODE NOWAIT;
    -- 작업 수행
EXCEPTION
    WHEN lock_not_available THEN
        RAISE NOTICE '테이블이 다른 세션에서 사용 중입니다. 나중에 다시 시도하세요.';
    WHEN object_in_use THEN
        RAISE NOTICE '객체가 현재 사용 중입니다';
END;
$$;
```

---

#### Class 57 - 운영자 개입 (Operator Intervention)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `57000` | `operator_intervention` | 운영자 개입 |
| `57014` | `query_canceled` | 쿼리가 취소됨 |
| `57P01` | `admin_shutdown` | 관리자 종료 |
| `57P02` | `crash_shutdown` | 충돌 종료 |
| `57P03` | `cannot_connect_now` | 현재 연결할 수 없음 |
| `57P04` | `database_dropped` | 데이터베이스가 삭제됨 |
| `57P05` | `idle_session_timeout` | 유휴 세션 타임아웃 |

```sql
-- 쿼리 취소 처리 예제
DO $$
BEGIN
    -- 장시간 실행되는 쿼리
    PERFORM long_running_query();
EXCEPTION
    WHEN query_canceled THEN
        RAISE NOTICE '쿼리가 사용자 또는 타임아웃에 의해 취소되었습니다';
    WHEN admin_shutdown THEN
        RAISE NOTICE '서버가 관리자에 의해 종료 중입니다';
END;
$$;
```

---

#### Class 58 - 시스템 오류 (System Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `58000` | `system_error` | 시스템 오류 |
| `58030` | `io_error` | I/O 오류 |
| `58P01` | `undefined_file` | 정의되지 않은 파일 |
| `58P02` | `duplicate_file` | 중복된 파일 |
| `58P03` | `file_name_too_long` | 파일 이름이 너무 김 |

---

#### Class F0 - 구성 파일 오류 (Configuration File Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `F0000` | `config_file_error` | 구성 파일 오류 |
| `F0001` | `lock_file_exists` | 잠금 파일이 존재함 |

---

#### Class HV - 외부 데이터 래퍼 오류 (Foreign Data Wrapper Error - SQL/MED)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `HV000` | `fdw_error` | FDW 오류 |
| `HV005` | `fdw_column_name_not_found` | FDW 열 이름을 찾을 수 없음 |
| `HV002` | `fdw_dynamic_parameter_value_needed` | FDW 동적 매개변수 값 필요 |
| `HV010` | `fdw_function_sequence_error` | FDW 함수 순서 오류 |
| `HV021` | `fdw_inconsistent_descriptor_information` | FDW 일관성 없는 기술자 정보 |
| `HV024` | `fdw_invalid_attribute_value` | FDW 잘못된 속성 값 |
| `HV007` | `fdw_invalid_column_name` | FDW 잘못된 열 이름 |
| `HV008` | `fdw_invalid_column_number` | FDW 잘못된 열 번호 |
| `HV004` | `fdw_invalid_data_type` | FDW 잘못된 데이터 타입 |
| `HV006` | `fdw_invalid_data_type_descriptors` | FDW 잘못된 데이터 타입 기술자 |
| `HV091` | `fdw_invalid_descriptor_field_identifier` | FDW 잘못된 기술자 필드 식별자 |
| `HV00B` | `fdw_invalid_handle` | FDW 잘못된 핸들 |
| `HV00C` | `fdw_invalid_option_index` | FDW 잘못된 옵션 인덱스 |
| `HV00D` | `fdw_invalid_option_name` | FDW 잘못된 옵션 이름 |
| `HV090` | `fdw_invalid_string_length_or_buffer_length` | FDW 잘못된 문자열 또는 버퍼 길이 |
| `HV00A` | `fdw_invalid_string_format` | FDW 잘못된 문자열 형식 |
| `HV009` | `fdw_invalid_use_of_null_pointer` | FDW NULL 포인터의 잘못된 사용 |
| `HV014` | `fdw_too_many_handles` | FDW 핸들이 너무 많음 |
| `HV001` | `fdw_out_of_memory` | FDW 메모리 부족 |
| `HV00P` | `fdw_no_schemas` | FDW 스키마 없음 |
| `HV00J` | `fdw_option_name_not_found` | FDW 옵션 이름을 찾을 수 없음 |
| `HV00K` | `fdw_reply_handle` | FDW 응답 핸들 |
| `HV00Q` | `fdw_schema_not_found` | FDW 스키마를 찾을 수 없음 |
| `HV00R` | `fdw_table_not_found` | FDW 테이블을 찾을 수 없음 |
| `HV00L` | `fdw_unable_to_create_execution` | FDW 실행을 생성할 수 없음 |
| `HV00M` | `fdw_unable_to_create_reply` | FDW 응답을 생성할 수 없음 |
| `HV00N` | `fdw_unable_to_establish_connection` | FDW 연결을 설정할 수 없음 |

---

#### Class P0 - PL/pgSQL 오류 (PL/pgSQL Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `P0000` | `plpgsql_error` | PL/pgSQL 오류 |
| `P0001` | `raise_exception` | RAISE EXCEPTION |
| `P0002` | `no_data_found` | 데이터를 찾을 수 없음 |
| `P0003` | `too_many_rows` | 행이 너무 많음 |
| `P0004` | `assert_failure` | ASSERT 실패 |

```sql
-- PL/pgSQL 오류 처리 예제
CREATE OR REPLACE FUNCTION get_user_by_id(p_user_id INTEGER)
RETURNS users AS $$
DECLARE
    v_user users;
BEGIN
    SELECT * INTO STRICT v_user
    FROM users
    WHERE id = p_user_id;

    RETURN v_user;
EXCEPTION
    WHEN no_data_found THEN
        RAISE EXCEPTION '사용자 ID %를 찾을 수 없습니다', p_user_id
            USING ERRCODE = 'P0002';
    WHEN too_many_rows THEN
        RAISE EXCEPTION '사용자 ID %에 대해 여러 행이 반환되었습니다', p_user_id
            USING ERRCODE = 'P0003';
END;
$$ LANGUAGE plpgsql;

-- 사용자 정의 예외 발생
CREATE OR REPLACE FUNCTION validate_age(p_age INTEGER)
RETURNS VOID AS $$
BEGIN
    IF p_age < 0 OR p_age > 150 THEN
        RAISE EXCEPTION '잘못된 나이 값: %', p_age
            USING ERRCODE = 'P0001',
                  HINT = '나이는 0에서 150 사이여야 합니다';
    END IF;
END;
$$ LANGUAGE plpgsql;
```

---

#### Class XX - 내부 오류 (Internal Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `XX000` | `internal_error` | 내부 오류 |
| `XX001` | `data_corrupted` | 데이터 손상 |
| `XX002` | `index_corrupted` | 인덱스 손상 |

---

### 오류 코드 활용 가이드

#### 1. PL/pgSQL에서 예외 처리

조건명으로 예외를 처리할 수 있습니다(대소문자 구분 없음):

```sql
CREATE OR REPLACE FUNCTION safe_divide(a NUMERIC, b NUMERIC)
RETURNS NUMERIC AS $$
BEGIN
    RETURN a / b;
EXCEPTION
    WHEN division_by_zero THEN
        RETURN NULL;
    WHEN numeric_value_out_of_range THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

#### 2. SQLSTATE 코드 직접 사용

조건명 대신 SQLSTATE 코드를 직접 지정할 수도 있습니다:

```sql
DO $$
BEGIN
    -- 작업 수행
EXCEPTION
    WHEN SQLSTATE '23505' THEN  -- unique_violation
        RAISE NOTICE '고유 제약 위반';
    WHEN SQLSTATE '23503' THEN  -- foreign_key_violation
        RAISE NOTICE '외래 키 위반';
END;
$$;
```

#### 3. 사용자 정의 SQLSTATE 코드

사용자 정의 오류에 자체 SQLSTATE 코드를 부여할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION custom_validation()
RETURNS VOID AS $$
BEGIN
    -- 사용자 정의 오류 발생
    RAISE EXCEPTION '사용자 정의 유효성 검사 실패'
        USING ERRCODE = 'A0001';  -- 사용자 정의 코드
END;
$$ LANGUAGE plpgsql;
```

#### 4. 애플리케이션에서 오류 코드 처리

애플리케이션은 오류 메시지 텍스트가 아닌 오류 코드를 기준으로 처리해야 합니다:

```python
# Python (psycopg2) 예제
import psycopg2
from psycopg2 import errors

try:
    cursor.execute("INSERT INTO users (email) VALUES ('duplicate@email.com')")
except errors.UniqueViolation:  # SQLSTATE 23505
    print("이메일이 이미 존재합니다")
except errors.ForeignKeyViolation:  # SQLSTATE 23503
    print("참조하는 외래 키가 존재하지 않습니다")
except psycopg2.DatabaseError as e:
    print(f"데이터베이스 오류: {e.pgcode} - {e.pgerror}")
```

```java
// Java (JDBC) 예제
try {
    statement.executeUpdate("INSERT INTO users (email) VALUES ('duplicate@email.com')");
} catch (SQLException e) {
    String sqlState = e.getSQLState();
    switch (sqlState) {
        case "23505":  // unique_violation
            System.out.println("이메일이 이미 존재합니다");
            break;
        case "23503":  // foreign_key_violation
            System.out.println("참조하는 외래 키가 존재하지 않습니다");
            break;
        default:
            System.out.println("데이터베이스 오류: " + sqlState);
    }
}
```

---

### 오류 정보 조회

#### GET DIAGNOSTICS 사용

PL/pgSQL에서 예외가 발생했을 때 상세 정보를 조회할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION detailed_error_handling()
RETURNS TEXT AS $$
DECLARE
    v_sqlstate TEXT;
    v_message TEXT;
    v_detail TEXT;
    v_hint TEXT;
    v_context TEXT;
BEGIN
    -- 오류를 유발하는 작업
    INSERT INTO nonexistent_table VALUES (1);
    RETURN '성공';
EXCEPTION
    WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS
            v_sqlstate = RETURNED_SQLSTATE,
            v_message = MESSAGE_TEXT,
            v_detail = PG_EXCEPTION_DETAIL,
            v_hint = PG_EXCEPTION_HINT,
            v_context = PG_EXCEPTION_CONTEXT;

        RAISE NOTICE 'SQLSTATE: %', v_sqlstate;
        RAISE NOTICE '메시지: %', v_message;
        RAISE NOTICE '상세: %', v_detail;
        RAISE NOTICE '힌트: %', v_hint;
        RAISE NOTICE '컨텍스트: %', v_context;

        RETURN format('오류 발생 - SQLSTATE: %s, 메시지: %s', v_sqlstate, v_message);
END;
$$ LANGUAGE plpgsql;
```

---

### 주요 참고사항

1. 이식성: 오류 코드는 텍스트 오류 메시지보다 PostgreSQL 버전 간 안정성이 높습니다.

2. 지역화 독립성: 오류 코드는 서버의 언어 설정에 관계없이 동일한 값을 유지합니다.

3. SQL 표준 준수: 많은 코드가 SQL 표준에 정의되어 있으며, 일부는 PostgreSQL 전용입니다.

4. PostgreSQL 전용 코드: `P`가 포함된 코드(예: `23P01`, `08P01`)는 PostgreSQL 전용입니다.

5. 오류 클래스 단위 처리: 특정 조건 대신 오류 클래스 전체를 포괄적으로 처리할 수 있습니다:
   ```sql
   EXCEPTION
       WHEN integrity_constraint_violation THEN  -- Class 23 전체
           -- 모든 무결성 제약 위반 처리
   ```

---

### 참고 자료

- [PostgreSQL 공식 문서 - Appendix A. PostgreSQL Error Codes](https://www.postgresql.org/docs/current/errcodes-appendix.html)
- [SQL 표준 SQLSTATE 코드](https://en.wikipedia.org/wiki/SQLSTATE)
- [PL/pgSQL 예외 처리](https://www.postgresql.org/docs/current/plpgsql-control-structures.html#PLPGSQL-ERROR-TRAPPING)

---

## PostgreSQL 문서 색인 (Document Index)

PostgreSQL 18 공식 문서 번역 색인입니다.

---

### 문서 구성 개요

| 분류 | 문서 번호 | 내용 |
|------|----------|------|
| 기초 | 01-05 | SQL 기본, 데이터 정의/조작, 쿼리 |
| 데이터 타입 및 함수 | 06-08 | 데이터 타입, 함수/연산자, 타입 변환 |
| 인덱스 및 검색 | 09-10 | 인덱스, 전문 검색 |
| 동시성 및 성능 | 11-13 | 동시성 제어, 성능 튜닝, 병렬 쿼리 |
| 서버 관리 | 14-17 | 서버 설정, 인증, 역할, DB 관리 |
| 백업 및 복제 | 18-19, 24 | 백업/복원, 고가용성, 논리적 복제 |
| 유지보수 | 20-23 | 지역화, 유지보수, 모니터링, WAL |
| 클라이언트 인터페이스 | 25-28 | libpq, 대용량 객체, ECPG, 정보 스키마 |
| 확장성 | 29-40 | SQL 확장, 트리거, 절차적 언어 등 |
| 인덱스 내부 | 47-52 | 인덱스 접근 메서드, GiST, SP-GiST, GIN, BRIN, Hash |
| 내부 구조 | 53-62 | 저장소, 시스템 카탈로그, 프로토콜, 소스 코드 |
| 참조 | 63-66 | B-Tree, contrib 모듈, SQL 명령어, 클라이언트 앱 |

---

### I. 기초 (Tutorial & SQL Basics)

#### [01. 튜토리얼 (Tutorial)](./01_tutorial.md)
PostgreSQL 입문을 위한 기본 가이드입니다.
- PostgreSQL 소개 및 아키텍처 개념
- SQL 언어 기초
- 고급 기능 개요

#### [02. SQL 구문 (SQL Syntax)](./02_sql_syntax.md)
SQL 구문의 기본 규칙을 다룹니다.
- 어휘 구조 (Lexical Structure)
- 값 표현식 (Value Expressions)
- 함수 호출 방법

#### [03. 데이터 정의 (Data Definition)](./03_data_definition.md)
데이터베이스 객체를 정의하는 방법입니다.
- 테이블 생성 및 수정
- 제약 조건 (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK)
- 스키마 및 상속
- 테이블 파티셔닝

#### [04. 데이터 조작 (Data Manipulation)](./04_data_manipulation.md)
데이터를 삽입, 수정, 삭제하는 방법입니다.
- INSERT 문
- UPDATE 문
- DELETE 문
- RETURNING 절

#### [05. 쿼리 (Queries)](./05_queries.md)
SELECT 문을 활용한 데이터 조회입니다.
- SELECT 기본 구문
- 테이블 표현식 (FROM 절, JOIN)
- 집합 연산 (UNION, INTERSECT, EXCEPT)
- WITH 쿼리 (CTE)
- LIMIT/OFFSET

---

### II. 데이터 타입 및 함수 (Data Types & Functions)

#### [06. 데이터 타입 (Data Types)](./06_data_types.md)
PostgreSQL에서 지원하는 데이터 타입입니다.
- 숫자 타입 (integer, numeric, float 등)
- 문자 타입 (char, varchar, text)
- 날짜/시간 타입
- 불리언, 열거형
- 기하학 타입, 네트워크 타입
- 배열, JSON/JSONB, XML
- 범위 타입, 복합 타입

#### [07. 함수와 연산자 (Functions and Operators)](./07_functions_operators.md)
내장 함수 및 연산자 레퍼런스입니다.
- 논리, 비교, 수학 연산자
- 문자열 함수
- 패턴 매칭 (LIKE, 정규식)
- 날짜/시간 함수
- 배열, JSON 함수
- 집계 함수, 윈도우 함수
- 서브쿼리 표현식

#### [08. 타입 변환 (Type Conversion)](./08_type_conversion.md)
데이터 타입 간 변환 규칙입니다.
- 암시적/명시적 타입 변환
- 연산자 타입 해석
- 함수 타입 해석
- 값 저장 시 타입 변환
- UNION/CASE 구문의 타입 해결

---

### III. 인덱스 및 검색 (Indexes & Search)

#### [09. 인덱스 (Indexes)](./09_indexes.md)
데이터베이스 성능 최적화를 위한 인덱스입니다.
- B-Tree, Hash, GiST, SP-GiST, GIN, BRIN 인덱스
- 다중 컬럼 인덱스
- 표현식 인덱스
- 부분 인덱스
- 커버링 인덱스 (INCLUDE)
- 인덱스 전용 스캔

#### [10. 전문 검색 (Full Text Search)](./10_full_text_search.md)
텍스트 검색 기능입니다.
- 문서와 쿼리 (tsvector, tsquery)
- 텍스트 검색 연산자
- 검색 결과 순위 지정
- 하이라이팅
- GIN/GiST 인덱스 활용

---

### IV. 동시성 및 성능 (Concurrency & Performance)

#### [11. 동시성 제어 (Concurrency Control)](./11_concurrency.md)
다중 사용자 환경에서의 동시성 관리입니다.
- MVCC (다중 버전 동시성 제어)
- 트랜잭션 격리 수준
  - Read Committed (기본값)
  - Repeatable Read
  - Serializable
- 명시적 잠금 (테이블/행 수준)
- 권고 잠금 (Advisory Locks)
- 교착 상태 (Deadlock)

#### [12. 성능 팁 (Performance Tips)](./12_performance.md)
쿼리 성능 최적화 방법입니다.
- EXPLAIN 사용법
  - EXPLAIN 기초
  - EXPLAIN ANALYZE
- 플래너 통계
- 명시적 JOIN 절로 플래너 제어
- 데이터베이스 채우기 최적화
- 비영속적 설정

#### [13. 병렬 쿼리 (Parallel Query)](./13_parallel_query.md)
다중 CPU를 활용한 병렬 처리입니다.
- 병렬 쿼리 작동 방식
- Gather/Gather Merge 노드
- 병렬 스캔, 병렬 조인, 병렬 집계
- 병렬 안전성 (Parallel Safety)
- 관련 설정 파라미터

---

### V. 서버 관리 (Server Administration)

#### [14. 서버 설정 (Server Configuration)](./14_server_config.md)
PostgreSQL 서버 설정 파라미터입니다.
- postgresql.conf 파일
- 파라미터 타입 (Boolean, String, Numeric 등)
- 연결 및 인증 설정
- 리소스 소비 (메모리, 디스크)
- Write Ahead Log (WAL)
- 복제 설정
- 쿼리 계획 설정
- 오류 보고 및 로깅

#### [15. 클라이언트 인증 (Client Authentication)](./15_client_auth.md)
클라이언트 연결 인증 방법입니다.
- pg_hba.conf 파일
- 인증 방법
  - Trust, Password (md5, scram-sha-256)
  - GSSAPI, SSPI
  - Ident, Peer
  - LDAP, RADIUS
  - 인증서 인증
  - PAM, OAuth
- 사용자 이름 맵

#### [16. 데이터베이스 역할 (Database Roles)](./16_database_roles.md)
사용자 및 권한 관리입니다.
- 역할 생성/삭제
- 역할 속성 (LOGIN, SUPERUSER, CREATEDB 등)
- 역할 멤버십
- 사전 정의된 역할
- 함수 보안 (SECURITY DEFINER)

#### [17. 데이터베이스 관리 (Managing Databases)](./17_managing_databases.md)
데이터베이스 생성 및 관리입니다.
- 데이터베이스 생성/삭제
- 템플릿 데이터베이스 (template0, template1)
- 데이터베이스 구성
- 테이블스페이스

---

### VI. 백업 및 복제 (Backup & Replication)

#### [18. 백업 및 복원 (Backup and Restore)](./18_backup_restore.md)
데이터 백업 및 복구 방법입니다.
- SQL 덤프 (pg_dump, pg_dumpall)
- 파일 시스템 레벨 백업
- 연속 아카이빙 (WAL 아카이빙)
- Point-in-Time Recovery (PITR)

#### [19. 고가용성, 로드 밸런싱, 복제 (High Availability)](./19_high_availability.md)
서버 가용성 및 복제 구성입니다.
- 솔루션 비교 (공유 디스크, 파일 시스템 복제, WAL 배송)
- 로그 배송 스탠바이 서버
- 스트리밍 복제
- 핫 스탠바이 (읽기 전용 쿼리)
- 동기/비동기 복제

#### [24. 논리적 복제 (Logical Replication)](./24_logical_replication.md)
테이블 단위 논리적 복제입니다.
- 발행 (Publication)
- 구독 (Subscription)
- 행 필터, 컬럼 목록
- 충돌 처리
- 장애 조치
- 모니터링

---

### VII. 유지보수 (Maintenance)

#### [20. 지역화 (Localization)](./20_localization.md)
다국어 및 문자셋 지원입니다.
- 로케일 지원 (LC_COLLATE, LC_CTYPE 등)
- 콜레이션 (Collation)
- 문자 집합 (Character Set) 지원
- 인코딩 변환

#### [21. 정기적 유지보수 (Routine Maintenance)](./21_maintenance.md)
데이터베이스 유지보수 작업입니다.
- VACUUM
  - 디스크 공간 회수
  - 플래너 통계 업데이트
  - 가시성 맵 업데이트
  - 트랜잭션 ID Wraparound 방지
- Autovacuum 데몬
- 재인덱싱 (REINDEX)
- 로그 파일 유지보수

#### [22. 모니터링 (Monitoring)](./22_monitoring.md)
데이터베이스 활동 모니터링입니다.
- 표준 유닉스 도구
- 누적 통계 시스템
  - pg_stat_activity
  - pg_stat_replication
  - pg_stat_database
  - pg_stat_user_tables
- 잠금 보기 (pg_locks)
- 진행 상황 보고
- 동적 추적

#### [23. WAL (Write-Ahead Log)](./23_wal.md)
WAL의 안정성 및 구성입니다.
- 안정성 원칙
- 데이터 체크섬
- WAL 구성
- 비동기 커밋
- WAL 내부 구조

---

### VIII. 클라이언트 인터페이스 (Client Interfaces)

#### [25. libpq - C 라이브러리](./25_libpq.md)
PostgreSQL C API입니다.
- 연결 제어 함수
- 명령 실행 함수
- 비동기 명령 처리
- 커서를 사용한 결과 검색
- 대용량 객체 함수

#### [26. 대용량 객체 (Large Objects)](./26_large_objects.md)
대용량 바이너리 데이터 처리입니다.
- 대용량 객체 소개
- 클라이언트 인터페이스
- 서버측 함수 (lo_create, lo_import, lo_export 등)

#### [27. ECPG - 내장 SQL](./27_ecpg.md)
C 언어 내장 SQL 프로그래밍입니다.
- ECPG 개념
- 데이터베이스 연결 관리
- SQL 명령 실행
- 호스트 변수
- 동적 SQL
- 오류 처리

#### [28. 정보 스키마 (Information Schema)](./28_information_schema.md)
SQL 표준 메타데이터 뷰입니다.
- 정보 스키마 개요
- 주요 뷰
  - tables, columns
  - table_constraints
  - routines
  - views

---

### IX. 확장성 (Extensibility)

#### [29. SQL 확장하기 (Extending SQL)](./29_extending_sql.md)
PostgreSQL 확장 방법입니다.
- 확장성 작동 원리
- PostgreSQL 타입 시스템
- 사용자 정의 함수
- 사용자 정의 타입
- 사용자 정의 연산자
- 확장 (Extensions)

#### [30. 트리거 (Triggers)](./30_triggers.md)
자동 실행 함수입니다.
- 트리거 개요 및 용도
- 트리거 동작 (BEFORE/AFTER, ROW/STATEMENT)
- 트리거 함수 작성
- 데이터 변경의 가시성

#### [31. 이벤트 트리거 (Event Triggers)](./31_event_triggers.md)
DDL 이벤트 트리거입니다.
- 이벤트 종류 (ddl_command_start, ddl_command_end 등)
- 이벤트 트리거 함수
- DDL 명령 캡처

#### [32. 규칙 시스템 (Rule System)](./32_rule_system.md)
쿼리 재작성 규칙입니다.
- 쿼리 트리 구조
- 뷰와 규칙 시스템
- INSERT/UPDATE/DELETE 규칙
- 규칙 vs 트리거

#### [33. 절차적 언어 (Procedural Languages)](./33_procedural_languages.md)
절차적 언어 개요입니다.
- 절차적 언어란?
- 핸들러 작동 방식
- 신뢰/비신뢰 언어
- 언어 설치

#### [34. PL/pgSQL](./34_plpgsql.md)
PostgreSQL 기본 절차적 언어입니다.
- 변수 및 상수 선언
- 제어 구조 (IF, CASE, LOOP)
- 커서 사용
- 오류 및 예외 처리
- 트리거 함수
- 트랜잭션 제어

#### [35. PL/Tcl](./35_pltcl.md)
Tcl 절차적 언어입니다.
- PL/Tcl 함수와 인수
- 데이터베이스 접근
- 트리거 함수
- 오류 처리

#### [36. PL/Perl](./36_plperl.md)
Perl 절차적 언어입니다.
- PL/Perl 함수
- 내장 함수
- 신뢰/비신뢰 PL/Perl
- 트리거

#### [37. PL/Python](./37_plpython.md)
Python 절차적 언어입니다.
- PL/Python 함수
- 데이터 타입 매핑
- 데이터베이스 접근
- 트리거 함수
- 트랜잭션 제어

#### [38. SPI (Server Programming Interface)](./38_spi.md)
서버 프로그래밍 인터페이스입니다.
- 연결 관리
- 쿼리 실행
- 준비된 구문
- 커서 관리
- 메모리 관리

#### [39. 백그라운드 워커 (Background Workers)](./39_background_workers.md)
백그라운드 프로세스 확장입니다.
- 백그라운드 워커 등록
- 시그널 처리
- 공유 메모리 접근

#### [40. 논리적 디코딩 (Logical Decoding)](./40_logical_decoding.md)
변경 데이터 캡처입니다.
- 논리적 디코딩 개념
- 복제 슬롯
- 출력 플러그인

---

### X. 고급 확장 기능 (Advanced Extension Features)

#### [41. 복제 진행 추적 (Replication Progress)](./41_replication_progress.md)
복제 원본 및 진행 상태 추적입니다.

#### [42. 아카이브 모듈 (Archive Modules)](./42_archive_modules.md)
커스텀 WAL 아카이브 모듈입니다.

#### [43. 외래 데이터 래퍼 (FDW)](./43_fdw.md)
외래 테이블 접근 래퍼 작성입니다.

#### [44. 테이블 샘플링 메서드 (Table Sampling)](./44_tablesample.md)
커스텀 샘플링 메서드입니다.

#### [45. 커스텀 스캔 프로바이더 (Custom Scan)](./45_custom_scan.md)
커스텀 스캔 방법 구현입니다.

#### [46. 일반 WAL 레코드 (Generic WAL)](./46_generic_wal.md)
확장을 위한 일반 WAL 레코드입니다.

---

### XI. 인덱스 내부 구조 (Index Internals)

#### [47. 인덱스 접근 메서드 (Index Access Method)](./47_index_am.md)
새로운 인덱스 유형 개발을 위한 인터페이스입니다.
- 기본 API 구조
- 인덱스 스캔
- 인덱스 잠금 고려사항

#### [48. GiST 인덱스](./48_gist.md)
일반화 검색 트리입니다.
- GiST 소개 및 확장성
- 내장 연산자 클래스
- 구현 세부사항

#### [49. SP-GiST 인덱스](./49_spgist.md)
공간 분할 일반화 검색 트리입니다.
- 쿼드 트리, k-d 트리, 기수 트리
- 확장성

#### [50. GIN 인덱스](./50_gin.md)
일반화 역 인덱스입니다.
- 복합 값 인덱싱
- 전문 검색, 배열 검색
- 성능 팁

#### [51. BRIN 인덱스](./51_brin.md)
블록 범위 인덱스입니다.
- 블록 범위 개념
- 연산자 클래스 (Minmax, Inclusion, Bloom)

#### [52. 해시 인덱스](./52_hash.md)
해시 인덱스 내부입니다.
- 해시 코드 저장
- 동등 비교 전용

---

### XII. 내부 구조 (Internals)

#### [53. 데이터베이스 물리적 저장소 (Storage)](./53_storage.md)
물리적 저장소 구조입니다.
- 파일 레이아웃
- TOAST
- 프리 스페이스 맵
- 가시성 맵
- 페이지 레이아웃
- HOT (Heap-Only Tuples)

#### [54. 시스템 카탈로그 (System Catalogs)](./54_system_catalogs.md)
시스템 메타데이터 테이블입니다.
- pg_class, pg_attribute
- pg_type, pg_proc
- pg_namespace, pg_index
- pg_constraint, pg_database

#### [55. 시스템 뷰 (System Views)](./55_system_views.md)
시스템 상태 뷰입니다.
- pg_stat_activity
- pg_locks
- pg_settings
- pg_tables, pg_indexes

#### [56. Frontend/Backend 프로토콜](./56_protocol.md)
클라이언트-서버 통신 프로토콜입니다.
- 메시지 흐름
- 메시지 형식
- 확장 쿼리 프로토콜
- SASL 인증
- 복제 프로토콜

#### [57. PostgreSQL 소스 코드](./57_source_code.md)
소스 코드 구조입니다.
- 디렉토리 구조
- backend, bin, common 등
- 빌드 시스템

#### [58. 네이티브 언어 지원 (NLS)](./58_nls.md)
메시지 번역 시스템입니다.
- GNU gettext 기반
- 번역자/프로그래머 가이드

#### [59. 플래너 통계 활용 (Planner Statistics)](./59_planner_stats.md)
쿼리 플래너의 통계 활용입니다.
- 단일/확장 통계
- 행 추정 예제

#### [60. 유전 쿼리 최적화기 (GEQO)](./60_geqo.md)
복잡한 조인 최적화입니다.
- 유전 알고리즘 기반
- 설정 파라미터

#### [61. 테이블 접근 메서드 (Table AM)](./61_table_am.md)
테이블 저장 방법 인터페이스입니다.

#### [62. WAL 내부 구현 (WAL Internals)](./62_wal_internals.md)
WAL 시스템 내부입니다.
- WAL 기본 원리
- 레코드 구조

---

### XIII. 참조 (Reference)

#### [63. B-Tree 인덱스](./63_btree.md)
B-Tree 인덱스 상세입니다.
- 연산자 클래스 동작
- 지원 함수
- 구현 세부사항

#### [64. 추가 제공 모듈 (Contrib)](./64_contrib.md)
contrib 확장 모듈입니다.
- pg_stat_statements
- postgres_fdw
- pg_trgm, hstore, ltree

#### [65. SQL 명령어 참조 (SQL Commands)](./65_sql_commands.md)
SQL 명령어 레퍼런스입니다.
- DML (SELECT, INSERT, UPDATE, DELETE)
- DDL (CREATE, ALTER, DROP)
- DCL (GRANT, REVOKE)
- TCL (BEGIN, COMMIT, ROLLBACK)

#### [66. 클라이언트 애플리케이션 (Client Apps)](./66_client_apps.md)
클라이언트 도구입니다.
- psql (대화형 터미널)
- pg_dump, pg_restore
- pg_dumpall
- pg_basebackup
- createdb, dropdb
- createuser, dropuser
- vacuumdb, reindexdb

---

### 주제별 빠른 참조

#### 초보자용
1. [01. 튜토리얼](./01_tutorial.md) - PostgreSQL 시작하기
2. [02. SQL 구문](./02_sql_syntax.md) - SQL 기본 문법
3. [03. 데이터 정의](./03_data_definition.md) - 테이블 생성
4. [04. 데이터 조작](./04_data_manipulation.md) - 데이터 CRUD
5. [05. 쿼리](./05_queries.md) - SELECT 문

#### 성능 최적화
1. [09. 인덱스](./09_indexes.md) - 인덱스 활용
2. [12. 성능 팁](./12_performance.md) - EXPLAIN 및 튜닝
3. [13. 병렬 쿼리](./13_parallel_query.md) - 병렬 처리
4. [21. 유지보수](./21_maintenance.md) - VACUUM, ANALYZE

#### 운영/관리
1. [14. 서버 설정](./14_server_config.md) - 설정 파라미터
2. [15. 클라이언트 인증](./15_client_auth.md) - 인증 설정
3. [16. 역할](./16_database_roles.md) - 권한 관리
4. [18. 백업/복원](./18_backup_restore.md) - 백업 전략
5. [22. 모니터링](./22_monitoring.md) - 상태 모니터링

#### 고가용성/복제
1. [19. 고가용성](./19_high_availability.md) - HA 구성
2. [24. 논리적 복제](./24_logical_replication.md) - 논리적 복제
3. [23. WAL](./23_wal.md) - WAL 이해

#### 개발자용
1. [06. 데이터 타입](./06_data_types.md) - 타입 선택
2. [07. 함수와 연산자](./07_functions_operators.md) - 내장 함수
3. [34. PL/pgSQL](./34_plpgsql.md) - 저장 프로시저
4. [30. 트리거](./30_triggers.md) - 자동화

#### 확장 개발자용
1. [29. SQL 확장](./29_extending_sql.md) - 확장 개발
2. [47. 인덱스 접근 메서드](./47_index_am.md) - 커스텀 인덱스
3. [43. FDW](./43_fdw.md) - 외래 데이터 래퍼

---

### 문서 정보

- 원본: PostgreSQL 18 공식 문서
- 최종 업데이트: 2026-01-15

---

### 참고 자료

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/18/)
- [PostgreSQL 위키](https://wiki.postgresql.org/)
