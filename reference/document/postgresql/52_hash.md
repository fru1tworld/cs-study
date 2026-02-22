# Chapter 72: 해시 인덱스 (Hash Indexes)

PostgreSQL은 영구적이고 충돌 복구 가능한(crash-recoverable) 온디스크 해시 인덱스(hash index) 구현을 포함하고 있습니다. 해시 인덱스는 선형 순서가 잘 정의되지 않은 데이터 타입을 포함하여 모든 데이터 타입을 인덱싱할 수 있습니다.

## 72.1 개요 (Overview)

### 72.1.1 기본 개념

해시 인덱스는 인덱싱된 컬럼 값에서 파생된 32비트 해시 코드 를 저장합니다. 따라서 이러한 인덱스는 단순 동등 비교(equality comparison)만 처리할 수 있습니다.

```sql
-- 해시 인덱스 생성 구문
CREATE INDEX name ON table USING HASH (column);
```

### 72.1.2 주요 특성

#### 저장 방식

| 특성 | 설명 |
|------|------|
| 저장 데이터 | 실제 컬럼 값이 아닌 해시 값 만 저장 |
| 해시 크기 | 각 인덱스 튜플은 4바이트 해시 값 저장 |
| 인덱스 크기 | UUID, URL 등 긴 데이터 항목 인덱싱 시 B-tree보다 훨씬 작음 |
| 스캔 특성 | 모든 해시 인덱스 스캔은 손실성(lossy) |

#### 지원 기능

- 단일 컬럼 인덱스 만 지원
- 유일성 검사(uniqueness checking) 불가
- 동등 연산자(`=`)만 지원 (범위 연산 불가)
- 비트맵 인덱스 스캔(bitmap index scan) 참여 가능
- 역방향 스캔(backward scan) 지원

### 72.1.3 제한 사항

해시 인덱스는 다음 연산에 사용할 수 없습니다:

- 범위 쿼리 (`<`, `<=`, `>=`, `>`)
- 패턴 매칭 (`LIKE`, `~`)
- `BETWEEN` 또는 `IN` 절
- `IS NULL` 또는 `IS NOT NULL` 조건
- 정렬/순서 연산
- 최근접 이웃 검색(nearest-neighbor search)

### 72.1.4 성능 특성

#### B-tree와의 비교

B-tree 인덱스에서는 리프 페이지를 찾을 때까지 트리를 타고 내려가야 합니다. 수백만 행이 있는 테이블에서 이 하강(descent)은 데이터 접근 시간을 증가시킬 수 있습니다.

해시 인덱스에서 리프 페이지에 해당하는 것을 버킷 페이지(bucket page) 라고 합니다. 해시 인덱스는 버킷 페이지에 직접 접근 할 수 있어, 대용량 테이블에서 인덱스 접근 시간을 잠재적으로 줄일 수 있습니다.

```
B-tree 인덱스:  루트 -> 내부 노드 -> ... -> 리프 페이지
해시 인덱스:    메타페이지 -> 버킷 페이지 (직접 접근)
```

#### 최적 사용 사례

해시 인덱스는 다음 상황에 가장 적합합니다:

1. 유일하거나 거의 유일한 데이터
2. 해시 버킷당 행 수가 적은 데이터
3. 동등 스캔이 지배적인 대형 테이블
4. SELECT 및 UPDATE 중심 워크로드

#### 주의 사항

해시 값 분포가 고르지 않으면 오버플로우 페이지를 모두 스캔해야 합니다. 따라서 불균형 해시 인덱스는 일부 데이터에서 B-tree보다 더 많은 블록 접근이 필요할 수 있습니다.

완화 방법: 고유하지 않은 값이 많은 경우 부분 인덱스(partial index)를 사용하여 제외할 수 있습니다.

```sql
-- 부분 인덱스 예제: NULL이 아닌 값만 인덱싱
CREATE INDEX idx_email_hash ON users USING HASH (email)
WHERE email IS NOT NULL;
```

### 72.1.5 버킷과 오버플로우 관리

```
버킷 페이지 구조:
┌─────────────────┐
│   버킷 페이지    │ ──> ┌─────────────────┐
│  (기본 저장소)   │     │ 오버플로우 페이지 │ ──> ...
└─────────────────┘     └─────────────────┘
```

- 버킷 페이지(Bucket pages): B-tree의 리프 페이지에 해당하는 직접 접근 지점
- 오버플로우 페이지(Overflow pages): 버킷 페이지가 가득 찼을 때 연결되는 추가 페이지
- 해시 인덱스는 해시 값의 불균등 분포를 처리하도록 설계됨

### 72.1.6 유지 관리 (Maintenance)

#### 튜플 삭제

B-tree와 마찬가지로 해시 인덱스는 단순 인덱스 튜플 삭제(지연 유지 관리)를 수행합니다:

- 삭제해도 안전한 것으로 알려진 인덱스 튜플 삭제 (LP_DEAD 비트가 이미 설정된 항목)
- 삽입에 공간이 필요할 때 또는 VACUUM 중에 데드 튜플 제거
- VACUUM은 튜플을 더 적은 페이지로 압축하여 오버플로우 체인 최소화 시도

#### 제한 사항

- 축소 방법 없음: REINDEX 외에는 해시 인덱스를 축소할 방법 없음
- 버킷 감소 불가: 버킷 수를 줄이는 방법 없음

### 72.1.7 확장 (Expansion)

해시 인덱스는 인덱싱된 행 수가 증가함에 따라 버킷 페이지 수를 확장할 수 있습니다:

1. 새 버킷이 추가될 때 정확히 하나의 기존 버킷이 "분할(split)"됨
2. 일부 튜플이 업데이트된 키-버킷 번호 매핑에 따라 새 버킷으로 전송됨
3. 확장은 포그라운드에서 발생 하여 삽입 실행 시간이 증가할 수 있음

```sql
-- 인덱스 상태 확인
SELECT
    indexrelname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE indexrelname LIKE '%hash%';
```

## 72.2 구현 (Implementation)

### 72.2.1 페이지 유형

해시 인덱스는 네 가지 유형의 페이지 로 구성됩니다:

| 페이지 유형 | 설명 |
|------------|------|
| 메타 페이지 (Meta page) | 페이지 0, 정적으로 할당된 제어 정보 포함 |
| 기본 버킷 페이지 (Primary bucket pages) | 해시 버킷의 주요 저장소 |
| 오버플로우 페이지 (Overflow pages) | 버킷이 용량을 초과할 때 추가 페이지 |
| 비트맵 페이지 (Bitmap pages) | 재사용 가능한 해제된 오버플로우 페이지 추적 |

```
해시 인덱스 구조:
┌──────────────┐
│  메타 페이지   │  <- 페이지 0 (제어 정보)
│  (Page 0)    │
├──────────────┤
│  버킷 0      │ ──> [오버플로우] ──> [오버플로우]
├──────────────┤
│  버킷 1      │
├──────────────┤
│  버킷 2      │ ──> [오버플로우]
├──────────────┤
│    ...       │
├──────────────┤
│ 비트맵 페이지  │  <- 해제된 오버플로우 페이지 추적
└──────────────┘
```

### 72.2.2 캐싱 전략

모든 작업에 대해 메타페이지를 잠그고 고정(pin)하는 성능 페널티를 피하기 위해:

- 메타페이지 정보가 각 백엔드의 relcache 항목에 캐시 됨
- 필요한 데이터: 버킷 카운트, highmask, lowmask
- 대상 버킷이 마지막 캐시 갱신 이후 분할되지 않았다면 올바른 버킷 매핑 유지

### 72.2.3 튜플 구성

- 인덱싱된 테이블의 각 행은 단일 인덱스 튜플 로 표현됨
- 해시 인덱스 튜플은 버킷 및 오버플로우 페이지에 저장됨

#### 페이지 내 정렬

```
페이지 내부:
┌────────────────────────────────────────┐
│ 해시코드 순으로 정렬된 인덱스 항목        │
│ [hash=1001] [hash=1234] [hash=5678] ...│
│         이진 검색 가능!                 │
└────────────────────────────────────────┘

페이지 간:
┌──────────┐  ┌──────────┐  ┌──────────┐
│ 페이지 A  │  │ 페이지 B  │  │ 페이지 C  │
│ (정렬됨)  │  │ (정렬됨)  │  │ (정렬됨)  │
└──────────┘  └──────────┘  └──────────┘
     ↑              ↑              ↑
     └──────── 페이지 간 순서 보장 없음 ──────┘
```

- 최적화: 단일 페이지 내 인덱스 항목은 해시 코드로 정렬되어 이진 검색 가능
- 주의: 동일 버킷의 다른 인덱스 페이지 간에는 해시 코드 순서 보장 없음

### 72.2.4 저장소 할당

- 기본 버킷 페이지와 오버플로우 페이지는 독립적으로 할당 됨
- 이를 통해 오버플로우 페이지 대 버킷의 비율에 유연성 제공
- 기본 버킷 페이지를 이동하지 않고도 가변적인 오버플로우 페이지를 지원하는 특수 주소 지정 규칙 사용

### 72.2.5 버킷 분할

해시 인덱스를 확장하는 복잡한 분할 알고리즘:

1. 분할 알고리즘은 충돌 안전(crash-safe)
2. 중단되더라도 재시작 가능
3. 자세한 정보는 `src/backend/access/hash/README`에 문서화됨

```sql
-- 해시 인덱스 리인덱싱 (크기 최적화)
REINDEX INDEX index_name;

-- 동시 리인덱싱 (잠금 최소화)
REINDEX INDEX CONCURRENTLY index_name;
```

## 72.3 내장 연산자 클래스 (Built-in Operator Classes)

### 72.3.1 해시 인덱스 전략

해시 인덱스는 동등 비교만 지원하므로 단일 전략만 정의합니다:

| 연산 | 전략 번호 |
|------|----------|
| equal (`=`) | 1 |

### 72.3.2 해시 지원 함수

해시 인덱스는 하나의 필수 지원 함수 와 두 개의 선택적 함수 를 요구합니다:

| 함수 | 지원 번호 | 설명 |
|------|----------|------|
| 32비트 해시 값 계산 | 1 | 필수 |
| 64비트 솔트로 64비트 해시 값 계산 | 2 | 선택적; 솔트가 0이면 하위 32비트는 함수 1의 결과와 일치해야 함 |
| 연산자 클래스별 옵션 정의 | 3 | 선택적 |

### 72.3.3 해시 연산자 클래스 조회

PostgreSQL에서 사용 가능한 해시 연산자 클래스를 조회하려면:

```sql
-- 모든 해시 연산자 클래스 조회
SELECT
    am.amname AS index_method,
    opc.opcname AS opclass_name,
    opc.opcintype::regtype AS indexed_type,
    opc.opcdefault AS is_default
FROM pg_am am
JOIN pg_opclass opc ON opc.opcmethod = am.oid
WHERE am.amname = 'hash'
ORDER BY opclass_name;
```

### 72.3.4 주요 내장 해시 연산자 클래스

다음은 PostgreSQL에서 제공하는 주요 해시 연산자 클래스입니다:

| 연산자 클래스 | 인덱싱 타입 | 연산자 |
|--------------|-----------|--------|
| `int2_ops` | smallint | `=` |
| `int4_ops` | integer | `=` |
| `int8_ops` | bigint | `=` |
| `float4_ops` | real | `=` |
| `float8_ops` | double precision | `=` |
| `numeric_ops` | numeric | `=` |
| `text_ops` | text | `=` |
| `varchar_ops` | varchar | `=` |
| `char_ops` | char | `=` |
| `bpchar_ops` | bpchar | `=` |
| `bytea_ops` | bytea | `=` |
| `date_ops` | date | `=` |
| `time_ops` | time | `=` |
| `timetz_ops` | timetz | `=` |
| `timestamp_ops` | timestamp | `=` |
| `timestamptz_ops` | timestamptz | `=` |
| `interval_ops` | interval | `=` |
| `uuid_ops` | uuid | `=` |
| `oid_ops` | oid | `=` |
| `bool_ops` | boolean | `=` |
| `macaddr_ops` | macaddr | `=` |
| `inet_ops` | inet | `=` |
| `cidr_ops` | cidr | `=` |

### 72.3.5 다중 데이터 타입 해시 패밀리

여러 데이터 타입으로 해시 연산자 패밀리를 구축할 때:

- 지원되는 각 데이터 타입에 대해 호환 가능한 해시 지원 함수 생성 필요
- 패밀리의 동등 연산자가 같다고 간주하는 값에 대해 동일한 해시 코드 반환 필요 (다른 타입이라도)
- 연산자 패밀리 내 데이터 타입 간 암시적 또는 이진 강제 변환 시 해시 값이 변경되면 안 됨

```sql
-- 해시 연산자 패밀리 조회
SELECT
    opf.opfname AS family_name,
    am.amname AS index_method,
    opc.opcname AS opclass_name,
    opc.opcintype::regtype AS type
FROM pg_opfamily opf
JOIN pg_am am ON opf.opfmethod = am.oid
JOIN pg_opclass opc ON opc.opcfamily = opf.oid
WHERE am.amname = 'hash'
ORDER BY family_name, opclass_name;
```

## 72.4 실용적인 예제

### 72.4.1 해시 인덱스 생성 및 사용

```sql
-- 테이블 생성
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    session_token UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 이메일에 해시 인덱스 생성 (정확한 일치 검색에 최적)
CREATE INDEX idx_users_email_hash ON users USING HASH (email);

-- UUID에 해시 인덱스 생성 (긴 값에 효율적)
CREATE INDEX idx_users_session_hash ON users USING HASH (session_token);

-- 사용 예: 해시 인덱스가 사용됨
SELECT * FROM users WHERE email = 'user@example.com';
SELECT * FROM users WHERE session_token = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

-- 주의: 다음 쿼리에서는 해시 인덱스 사용 불가
SELECT * FROM users WHERE email LIKE '%@example.com';  -- 패턴 매칭
SELECT * FROM users WHERE email > 'a@example.com';     -- 범위 쿼리
```

### 72.4.2 실행 계획 확인

```sql
-- 해시 인덱스 사용 확인
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users WHERE email = 'user@example.com';

-- 예상 출력:
-- Index Scan using idx_users_email_hash on users
--   Index Cond: ((email)::text = 'user@example.com'::text)
--   Buffers: shared hit=3
```

### 72.4.3 B-tree vs Hash 인덱스 비교

```sql
-- 테스트 테이블 생성
CREATE TABLE test_index (
    id SERIAL,
    uuid_col UUID DEFAULT gen_random_uuid(),
    text_col TEXT
);

-- 대량 데이터 삽입
INSERT INTO test_index (text_col)
SELECT md5(random()::text)
FROM generate_series(1, 1000000);

-- B-tree 인덱스 생성
CREATE INDEX idx_btree ON test_index USING BTREE (uuid_col);

-- 해시 인덱스 생성
CREATE INDEX idx_hash ON test_index USING HASH (uuid_col);

-- 인덱스 크기 비교
SELECT
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as size
FROM pg_indexes
WHERE tablename = 'test_index';

-- 성능 비교 (동등 검색)
EXPLAIN (ANALYZE, TIMING)
SELECT * FROM test_index WHERE uuid_col = (SELECT uuid_col FROM test_index LIMIT 1);
```

### 72.4.4 부분 해시 인덱스

```sql
-- 활성 사용자만 인덱싱
CREATE INDEX idx_active_users_hash ON users USING HASH (email)
WHERE is_active = true;

-- 특정 조건의 데이터만 인덱싱
CREATE INDEX idx_premium_sessions ON users USING HASH (session_token)
WHERE subscription_type = 'premium';
```

### 72.4.5 해시 인덱스 유지 관리

```sql
-- 인덱스 통계 확인
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE indexname LIKE '%hash%';

-- 인덱스 재구축 (크기 최적화)
REINDEX INDEX idx_users_email_hash;

-- 동시 재구축 (서비스 중단 최소화)
REINDEX INDEX CONCURRENTLY idx_users_email_hash;

-- 인덱스 삭제
DROP INDEX idx_users_email_hash;
```

## 72.5 해시 인덱스 선택 가이드

### 72.5.1 해시 인덱스를 사용해야 할 때

| 조건 | 이유 |
|------|------|
| 동등 비교(`=`)만 사용 | 해시 인덱스의 유일한 지원 연산 |
| 긴 값 인덱싱 (UUID, URL 등) | 4바이트 해시만 저장하여 공간 절약 |
| 대용량 테이블 | 직접 버킷 접근으로 빠른 조회 |
| 유일하거나 거의 유일한 데이터 | 오버플로우 페이지 최소화 |

### 72.5.2 B-tree를 사용해야 할 때

| 조건 | 이유 |
|------|------|
| 범위 쿼리 필요 | B-tree만 `<`, `>`, `BETWEEN` 지원 |
| 정렬 필요 | 해시 인덱스는 순서 정보 없음 |
| 유일성 제약 필요 | 해시 인덱스는 유일성 검사 불가 |
| 다중 컬럼 인덱스 필요 | 해시는 단일 컬럼만 지원 |

### 72.5.3 의사 결정 플로우차트

```
인덱스 선택 가이드:
                    ┌─────────────────────┐
                    │ 범위 쿼리가 필요한가? │
                    └──────────┬──────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
                   예                    아니오
                    │                     │
                    ▼                     ▼
              ┌──────────┐      ┌─────────────────────┐
              │  B-tree  │      │ 유일성 제약이 필요한가? │
              └──────────┘      └──────────┬──────────┘
                                           │
                                ┌──────────┴──────────┐
                                │                     │
                               예                    아니오
                                │                     │
                                ▼                     ▼
                          ┌──────────┐      ┌─────────────────┐
                          │  B-tree  │      │ 긴 값(UUID 등)? │
                          └──────────┘      └────────┬────────┘
                                                     │
                                          ┌──────────┴──────────┐
                                          │                     │
                                         예                    아니오
                                          │                     │
                                          ▼                     ▼
                                    ┌──────────┐          ┌──────────┐
                                    │   Hash   │          │ B-tree   │
                                    └──────────┘          │ (기본값) │
                                                          └──────────┘
```

## 72.6 참고 자료

- PostgreSQL 공식 문서: [Hash Indexes](https://www.postgresql.org/docs/current/hash-index.html)
- PostgreSQL 소스 코드: `src/backend/access/hash/README`
- [Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- [Operator Classes and Operator Families](https://www.postgresql.org/docs/current/indexes-opclass.html)
