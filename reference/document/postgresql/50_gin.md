# Chapter 70: GIN 인덱스 (GIN Indexes)

## 목차
1. [소개](#1-소개-introduction)
2. [내장 연산자 클래스](#2-내장-연산자-클래스-built-in-operator-classes)
3. [확장성](#3-확장성-extensibility)
4. [구현](#4-구현-implementation)
5. [GIN 팁과 트릭](#5-gin-팁과-트릭-tips-and-tricks)
6. [제한 사항](#6-제한-사항-limitations)
7. [예제](#7-예제-examples)

---

## 1. 소개 (Introduction)

### 1.1 GIN이란?

GIN 은 Generalized Inverted Index(일반화된 역 인덱스) 의 약자입니다. GIN은 복합 값(composite values)을 처리하기 위해 설계되었으며, 쿼리가 해당 항목 내의 요소 값을 검색하는 경우에 적합합니다. 예를 들어, 특정 단어를 포함하는 문서를 검색하는 경우가 이에 해당합니다.

### 1.2 핵심 개념

GIN 인덱스에서 사용되는 주요 용어는 다음과 같습니다:

| 용어 | 설명 |
|------|------|
| 항목 (Item) | 인덱싱될 복합 값 |
| 키 (Key) | 항목 내의 요소 값 |
| 포스팅 리스트 (Posting List) | 특정 키가 발생하는 행 ID(row ID)의 집합 |

### 1.3 데이터 구조

GIN은 (키, 포스팅 리스트) 쌍을 저장합니다:

- 각 키 값은 한 번만 저장 됩니다 (자주 발생하는 키에 대해 컴팩트함)
- 동일한 행 ID가 여러 포스팅 리스트에 나타날 수 있습니다 (항목이 여러 키를 포함하기 때문)
- 검색은 항목이 아닌 키를 대상 으로 수행됩니다

### 1.4 주요 장점

- 특정 연산에 독립적인 일반화된 접근 방법 코드
- 특정 데이터 타입에 대한 사용자 정의 전략 가능
- 도메인 전문가가 적절한 접근 방법으로 사용자 정의 데이터 타입 을 개발할 수 있음

---

## 2. 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL은 다음과 같은 GIN 연산자 클래스를 기본적으로 제공합니다:

### 2.1 연산자 클래스 목록

| 이름 | 인덱싱 가능한 연산자 | 설명 |
|------|---------------------|------|
| `array_ops` | `&&`, `@>`, `<@`, `=` | 배열 연산 |
| `jsonb_ops` | `@>`, `@?`, `@@`, `?`, `?\|`, `?&` | JSONB 기본 연산자 클래스 |
| `jsonb_path_ops` | `@>`, `@?`, `@@` | 더 적은 연산자, 더 나은 성능 |
| `tsvector_ops` | `@@` | 전문 검색(Full-Text Search) |

### 2.2 연산자 설명

#### 배열 연산자 (array_ops)

```sql
-- && : 겹침 (overlap) - 공통 요소가 있는지 확인
SELECT * FROM posts WHERE tags && ARRAY['postgresql', 'database'];

-- @> : 포함 (contains) - 왼쪽이 오른쪽을 포함하는지
SELECT * FROM posts WHERE tags @> ARRAY['postgresql'];

-- <@ : 포함됨 (is contained by) - 왼쪽이 오른쪽에 포함되는지
SELECT * FROM posts WHERE tags <@ ARRAY['postgresql', 'mysql', 'mongodb'];

-- = : 동일 (equals)
SELECT * FROM posts WHERE tags = ARRAY['postgresql', 'database'];
```

#### JSONB 연산자 (jsonb_ops)

```sql
-- @> : 포함 (contains)
SELECT * FROM documents WHERE data @> '{"status": "active"}';

-- ? : 키 존재 여부 확인
SELECT * FROM documents WHERE data ? 'email';

-- ?| : 키 중 하나라도 존재하는지
SELECT * FROM documents WHERE data ?| ARRAY['email', 'phone'];

-- ?& : 모든 키가 존재하는지
SELECT * FROM documents WHERE data ?& ARRAY['email', 'name'];

-- @? : JSON 경로 존재 여부
SELECT * FROM documents WHERE data @? '$.items[*] ? (@.price > 100)';

-- @@ : JSON 경로 술어 일치
SELECT * FROM documents WHERE data @@ '$.items[*].price > 100';
```

### 2.3 jsonb_ops vs jsonb_path_ops

```sql
-- jsonb_ops (기본값) - 더 많은 연산자 지원
CREATE INDEX idx_data ON documents USING GIN (data);

-- jsonb_path_ops - 더 나은 성능, 제한된 연산자
CREATE INDEX idx_data_path ON documents USING GIN (data jsonb_path_ops);
```

차이점:
- `jsonb_ops`: 모든 JSONB 연산자 지원, 더 큰 인덱스 크기
- `jsonb_path_ops`: `@>`, `@?`, `@@` 연산자만 지원, 더 작은 인덱스, 더 빠른 검색

#### 전문 검색 연산자 (tsvector_ops)

```sql
-- 전문 검색 인덱스 생성
CREATE INDEX idx_search ON articles USING GIN (to_tsvector('english', content));

-- 검색 쿼리
SELECT * FROM articles
WHERE to_tsvector('english', content) @@ to_tsquery('english', 'postgresql & indexing');
```

---

## 3. 확장성 (Extensibility)

GIN은 사용자 정의 연산자 클래스 메서드를 구현할 수 있는 확장 인터페이스를 제공합니다. GIN 계층은 동시성, 로깅 및 트리 구조 검색을 처리합니다.

### 3.1 필수 메서드

#### 3.1.1 extractValue()

인덱싱할 항목에서 키를 추출합니다.

```c
Datum *extractValue(Datum itemValue, int32 *nkeys, bool nullFlags)
```

매개변수:
- `itemValue`: 인덱싱할 항목 값
- `nkeys`: 추출된 키의 개수를 저장할 포인터
- `nullFlags`: null 키가 있을 경우 설정할 플래그 배열

반환값:
- palloc'd 키 배열
- 항목에 키가 없으면 `NULL` 반환 가능

#### 3.1.2 extractQuery()

쿼리 값에서 키를 추출합니다.

```c
Datum *extractQuery(Datum query, int32 *nkeys, StrategyNumber n,
                    bool pmatch, Pointer extra_data,
                    bool nullFlags, int32 *searchMode)
```

매개변수:
- `query`: 쿼리 값
- `nkeys`: 추출된 키의 개수
- `n`: 전략 번호 (연산자 식별)
- `pmatch`: 부분 일치 플래그 배열
- `extra_data`: 추가 데이터 배열
- `nullFlags`: null 플래그 배열
- `searchMode`: 검색 모드 출력

searchMode 출력 값:

| 값 | 설명 |
|---|------|
| `GIN_SEARCH_MODE_DEFAULT` | 1개 이상의 키와 일치하는 항목만 후보 |
| `GIN_SEARCH_MODE_INCLUDE_EMPTY` | 키가 없는 항목도 포함 |
| `GIN_SEARCH_MODE_ALL` | 모든 non-null 항목이 후보 (느림, 특수 케이스용) |

### 3.2 항목 일치 메서드 (하나 이상 구현)

#### 3.2.1 consistent() - 불리언 버전

```c
bool consistent(bool check[], StrategyNumber n, Datum query,
                int32 nkeys, Pointer extra_data[], bool *recheck,
                Datum queryKeys[], bool nullFlags[])
```

매개변수:
- `check[]`: i번째 키가 인덱싱된 항목에 있으면 `true`
- `n`: 전략 번호
- `query`: 원본 쿼리 값
- `nkeys`: 쿼리 키 개수
- `extra_data[]`: extractQuery에서 전달된 추가 데이터
- `recheck`: 재확인 필요 여부 플래그
- `queryKeys[]`: 쿼리 키 배열
- `nullFlags[]`: null 플래그 배열

recheck 플래그:
- `false`: 인덱스 테스트가 정확함, 힙 튜플이 확실히 일치하거나 일치하지 않음
- `true`: 힙 튜플이 일치할 수 있음, 실제 데이터 대비 재확인 필요

#### 3.2.2 triConsistent() - 3값 버전

```c
GinTernaryValue triConsistent(GinTernaryValue check[], StrategyNumber n,
                              Datum query, int32 nkeys,
                              Pointer extra_data[], Datum queryKeys[],
                              bool nullFlags[])
```

반환값:
- `GIN_TRUE`: 확실히 일치
- `GIN_FALSE`: 확실히 일치하지 않음
- `GIN_MAYBE`: 키 존재 여부 불확실

`triConsistent`만 제공해도 충분하며, `consistent` 기능을 포함합니다.

### 3.3 키 정렬 메서드

#### compare() (권장)

```c
int compare(Datum a, Datum b)
```

반환값:
- `< 0`: a가 b보다 작음
- `0`: 같음
- `> 0`: a가 b보다 큼

제공하지 않으면 GIN은 키 데이터 타입의 기본 btree 연산자 클래스를 찾습니다.

### 3.4 선택적 메서드

#### 3.4.1 comparePartial()

부분 일치 쿼리 지원을 위한 메서드입니다.

```c
int comparePartial(Datum partial_key, Datum key, StrategyNumber n,
                   Pointer extra_data)
```

반환값:
- `< 0`: 일치하지 않음, 스캔 계속
- `0`: 일치
- `> 0`: 스캔 중지

`extractQuery`가 `pmatch` 매개변수를 설정하는 경우 필수입니다.

#### 3.4.2 options()

연산자 클래스 동작을 제어하는 사용자 표시 매개변수를 정의합니다.

```c
void options(local_relopts *relopts)
```

`PG_HAS_OPCLASS_OPTIONS()` 및 `PG_GET_OPCLASS_OPTIONS()` 매크로로 접근합니다.

### 3.5 메서드 요약 테이블

| 메서드 | 목적 | 입력 | 출력 |
|--------|------|------|------|
| `extractValue` | 항목에서 키 추출 | 항목 datum | 키 배열, 개수, null 플래그 |
| `extractQuery` | 쿼리에서 키 추출 | 쿼리 datum, 전략 | 키 배열, 검색 모드, 부분 일치 플래그 |
| `consistent` | 항목이 쿼리와 일치하는지 확인 | check 배열, 쿼리 | Boolean + recheck 플래그 |
| `triConsistent` | 항목이 쿼리와 일치하는지 확인 | check 배열(3값), 쿼리 | GIN_TRUE/FALSE/MAYBE |
| `compare` | 키 정렬 | 두 개의 키 datum | -1, 0, 또는 1 |
| `comparePartial` | 부분 일치 키 범위 | 부분 키, 인덱스 키 | -1, 0, 또는 1 |
| `options` | 연산자 클래스 옵션 정의 | relopts 구조체 | (구조체 수정) |

---

## 4. 구현 (Implementation)

### 4.1 내부 구조

GIN 인덱스는 다음을 포함합니다:

- 키에 대한 B-tree 인덱스 (인덱싱된 항목의 요소)
- 리프 페이지 튜플:
  - 힙 포인터의 B-tree에 대한 포인터 (포스팅 트리, posting tree)
  - 또는 작은 리스트의 경우 힙 포인터의 간단한 리스트 (포스팅 리스트, posting list)

### 4.2 다중 컬럼 GIN 인덱스

- 복합 값에 대한 단일 B-tree: (컬럼 번호, 키 값)
- 다른 컬럼의 키 값은 다른 타입 일 수 있음
- null 키 값 지원 및 null 항목에 대한 플레이스홀더 null 지원

```sql
-- 다중 컬럼 GIN 인덱스 예제
CREATE INDEX idx_multi ON documents USING GIN (tags, categories);
```

### 4.3 GIN 빠른 업데이트 기법 (Fast Update Technique)

#### 문제점

GIN 인덱스 업데이트는 느립니다. 하나의 힙 행(heap row)이 많은 인덱스 삽입을 유발할 수 있기 때문입니다 (키당 하나씩).

#### 해결책

새 튜플을 임시 보류 항목 리스트 (pending entries list) 에 삽입하여 대부분의 작업을 연기합니다.

#### 정리 시점

- 테이블 VACUUM 또는 ANALYZE 중
- `gin_clean_pending_list` 함수 호출 시
- 보류 리스트가 `gin_pending_list_limit`를 초과할 때
- 이후 항목들은 대량 삽입 기법을 사용하여 메인 GIN 구조로 이동

#### 장점

- GIN 인덱스 업데이트 속도가 크게 향상됨
- 오버헤드는 백그라운드 autovacuum 프로세스가 처리할 수 있음

#### 단점

- 검색 시 보류 항목을 스캔해야 함 (큰 리스트는 검색을 느리게 함)
- "너무 큰" 보류 리스트를 유발하는 업데이트는 즉시 정리됨 (더 느림)

#### 제어

```sql
-- fastupdate 비활성화
CREATE INDEX idx_gin ON documents USING GIN (data) WITH (fastupdate = off);

-- 보류 리스트 제한 설정
ALTER INDEX idx_gin SET (gin_pending_list_limit = 256);
```

### 4.4 부분 일치 알고리즘 (Partial Match Algorithm)

#### 사용 사례

쿼리가 하나 이상의 키에 대해 정확한 일치를 결정하지 않지만, 일치 항목이 좁은 키 값 범위 내에 있는 경우입니다.

#### 프로세스

1. `extractQuery`가 범위의 하한을 반환하고 `pmatch = true` 설정
2. `comparePartial` 메서드를 사용하여 키 범위 스캔
3. `comparePartial` 반환값:
   - `0`: 일치하는 인덱스 키
   - `< 0`: 비일치, 검색 계속
   - `> 0`: 검색 가능한 범위를 벗어남

```sql
-- 부분 일치 예제: 트라이그램 검색
CREATE EXTENSION pg_trgm;
CREATE INDEX idx_trgm ON articles USING GIN (title gin_trgm_ops);

-- 부분 일치 쿼리
SELECT * FROM articles WHERE title LIKE '%postgresql%';
```

---

## 5. GIN 팁과 트릭 (Tips and Tricks)

### 5.1 생성 vs 삽입 (Create vs Insert)

대량 삽입의 경우:
1. GIN 인덱스 삭제
2. 데이터 삽입
3. 인덱스 재생성

이 방법이 기존 인덱스에 삽입하는 것보다 빠릅니다.

```sql
-- 대량 데이터 로드 시
DROP INDEX idx_gin;
-- 대량 INSERT 수행
CREATE INDEX idx_gin ON documents USING GIN (data);
```

참고: fastupdate가 활성화되어 있으면 패널티가 감소하지만, 매우 큰 업데이트의 경우 여전히 삭제/재생성이 이점이 있을 수 있습니다.

### 5.2 maintenance_work_mem

GIN 빌드 시간은 이 설정에 매우 민감 합니다. 인덱스 생성 중 작업 메모리를 아끼지 마세요.

```sql
-- 인덱스 생성 전 임시로 증가
SET maintenance_work_mem = '1GB';
CREATE INDEX idx_gin ON documents USING GIN (data);
RESET maintenance_work_mem;
```

### 5.3 gin_pending_list_limit

보류 항목이 정리되는 시점을 제어합니다.

포그라운드 정리를 피하려면:
- 제한 증가 (단, 발생 시 정리가 더 오래 걸림)
- autovacuum을 더 적극적으로 만들기

```sql
-- 전역 설정
SET gin_pending_list_limit = '64kB';

-- 개별 인덱스에 대한 재정의
ALTER INDEX idx_gin SET (gin_pending_list_limit = 128);
```

개별 인덱스 오버라이드:
자주 업데이트되는 인덱스에는 높은 제한, 다른 인덱스에는 낮은 제한 설정

```sql
-- 자주 업데이트되는 테이블
ALTER INDEX idx_frequently_updated SET (gin_pending_list_limit = 256);

-- 거의 업데이트되지 않는 테이블
ALTER INDEX idx_rarely_updated SET (gin_pending_list_limit = 64);
```

### 5.4 gin_fuzzy_search_limit

전문 검색에서 반환되는 행의 소프트 상한을 설정합니다.

```sql
-- 기본값: 0 (제한 없음)
SET gin_fuzzy_search_limit = 10000;
```

해결되는 문제:
자주 사용되는 단어가 포함된 전문 검색이 거대한 결과 집합을 반환하는 경우

권장 값: 5000-20000

참고: "소프트" 제한이므로 실제 결과는 쿼리 및 RNG 품질에 따라 달라질 수 있습니다.

### 5.5 성능 최적화 체크리스트

```sql
-- 1. 적절한 연산자 클래스 선택
-- JSONB의 경우, 필요한 연산자만 사용한다면 jsonb_path_ops 고려
CREATE INDEX idx_jsonb ON docs USING GIN (data jsonb_path_ops);

-- 2. 워크 메모리 설정 확인
SHOW maintenance_work_mem;

-- 3. 인덱스 크기 확인
SELECT pg_size_pretty(pg_relation_size('idx_jsonb'));

-- 4. 인덱스 사용 여부 확인
EXPLAIN ANALYZE SELECT * FROM docs WHERE data @> '{"status": "active"}';

-- 5. 보류 페이지 확인
SELECT * FROM gin_pending_pages('idx_jsonb');
```

---

## 6. 제한 사항 (Limitations)

### 6.1 엄격한 연산자 요구 사항

인덱싱 가능한 연산자는 반드시 strict 해야 합니다:

- null 항목 값에 대해 `extractValue`가 호출되지 않음 (자동 플레이스홀더 항목 생성)
- null 쿼리 값에 대해 `extractQuery`가 호출되지 않음 (쿼리가 충족 불가능하다고 가정)

### 6.2 예외

non-null 복합 항목/쿼리 내의 null 키 값은 지원됩니다.

```sql
-- 허용: 배열 내 null 값
CREATE INDEX idx_array ON t USING GIN (arr);
INSERT INTO t VALUES (ARRAY[1, NULL, 3]);

-- 쿼리 가능
SELECT * FROM t WHERE arr @> ARRAY[1];
```

---

## 7. 예제 (Examples)

### 7.1 배열 인덱싱

```sql
-- 테이블 생성
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    title TEXT,
    tags TEXT[]
);

-- GIN 인덱스 생성
CREATE INDEX idx_posts_tags ON posts USING GIN (tags);

-- 데이터 삽입
INSERT INTO posts (title, tags) VALUES
    ('PostgreSQL 시작하기', ARRAY['postgresql', 'database', 'tutorial']),
    ('GIN 인덱스 이해하기', ARRAY['postgresql', 'gin', 'index']),
    ('MySQL vs PostgreSQL', ARRAY['mysql', 'postgresql', 'comparison']);

-- 쿼리 예제
-- 'postgresql' 태그를 포함하는 게시물
SELECT * FROM posts WHERE tags @> ARRAY['postgresql'];

-- 'postgresql' 또는 'mysql' 태그를 포함하는 게시물
SELECT * FROM posts WHERE tags && ARRAY['postgresql', 'mysql'];

-- 정확히 해당 태그만 가진 게시물
SELECT * FROM posts WHERE tags <@ ARRAY['postgresql', 'database', 'tutorial'];
```

### 7.2 JSONB 인덱싱

```sql
-- 테이블 생성
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    data JSONB
);

-- 기본 GIN 인덱스
CREATE INDEX idx_docs_data ON documents USING GIN (data);

-- 또는 경로 연산자용 최적화 인덱스
CREATE INDEX idx_docs_data_path ON documents USING GIN (data jsonb_path_ops);

-- 데이터 삽입
INSERT INTO documents (data) VALUES
    ('{"name": "Alice", "age": 30, "skills": ["python", "postgresql"]}'),
    ('{"name": "Bob", "age": 25, "skills": ["java", "mongodb"]}'),
    ('{"name": "Charlie", "age": 35, "department": "Engineering"}');

-- 쿼리 예제
-- 특정 키-값 포함 검색
SELECT * FROM documents WHERE data @> '{"age": 30}';

-- 키 존재 여부 확인
SELECT * FROM documents WHERE data ? 'department';

-- 여러 키 중 하나라도 존재
SELECT * FROM documents WHERE data ?| ARRAY['skills', 'department'];

-- 모든 키가 존재
SELECT * FROM documents WHERE data ?& ARRAY['name', 'age'];

-- JSON 경로 쿼리
SELECT * FROM documents WHERE data @? '$.skills[*] ? (@ == "postgresql")';
```

### 7.3 전문 검색 (Full-Text Search)

```sql
-- 테이블 생성
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title TEXT,
    content TEXT,
    search_vector TSVECTOR
);

-- 트리거로 검색 벡터 자동 업데이트
CREATE FUNCTION update_search_vector() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER articles_search_update
    BEFORE INSERT OR UPDATE ON articles
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- GIN 인덱스 생성
CREATE INDEX idx_articles_search ON articles USING GIN (search_vector);

-- 데이터 삽입
INSERT INTO articles (title, content) VALUES
    ('PostgreSQL Performance Tuning',
     'Learn how to optimize your PostgreSQL database for better performance.'),
    ('Introduction to GIN Indexes',
     'GIN indexes are perfect for full-text search and array operations.'),
    ('Database Indexing Strategies',
     'Different indexing strategies for various database workloads.');

-- 검색 쿼리
SELECT title, ts_rank(search_vector, query) AS rank
FROM articles, to_tsquery('english', 'postgresql & performance') query
WHERE search_vector @@ query
ORDER BY rank DESC;

-- 구문 검색
SELECT * FROM articles
WHERE search_vector @@ phraseto_tsquery('english', 'GIN indexes');
```

### 7.4 Contrib 모듈 사용

#### pg_trgm (트라이그램 일치)

```sql
-- 확장 설치
CREATE EXTENSION pg_trgm;

-- 트라이그램 GIN 인덱스
CREATE INDEX idx_title_trgm ON articles USING GIN (title gin_trgm_ops);

-- LIKE/ILIKE 검색 (인덱스 사용)
SELECT * FROM articles WHERE title ILIKE '%postgresql%';

-- 유사도 검색
SELECT title, similarity(title, 'postgres') AS sim
FROM articles
WHERE title % 'postgres'
ORDER BY sim DESC;
```

#### intarray (정수 배열 확장)

```sql
-- 확장 설치
CREATE EXTENSION intarray;

-- 정수 배열 테이블
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    participant_ids INT[]
);

-- GIN 인덱스
CREATE INDEX idx_participants ON events USING GIN (participant_ids gin__int_ops);

-- 쿼리
SELECT * FROM events WHERE participant_ids @> ARRAY[1, 2];
```

#### hstore (키-값 저장소)

```sql
-- 확장 설치
CREATE EXTENSION hstore;

-- hstore 테이블
CREATE TABLE settings (
    id SERIAL PRIMARY KEY,
    config HSTORE
);

-- GIN 인덱스
CREATE INDEX idx_config ON settings USING GIN (config);

-- 데이터 삽입
INSERT INTO settings (config) VALUES
    ('theme => dark, language => ko'),
    ('theme => light, notifications => on');

-- 쿼리
SELECT * FROM settings WHERE config ? 'notifications';
SELECT * FROM settings WHERE config @> 'theme => dark';
```

---

## 부록: GIN vs GiST 비교

| 특성 | GIN | GiST |
|------|-----|------|
| 검색 속도 | 빠름 (정확한 일치에 최적) | 보통 |
| 인덱스 빌드 속도 | 느림 | 빠름 |
| 인덱스 크기 | 큼 | 작음 |
| 업데이트 속도 | 느림 (fastupdate로 완화) | 빠름 |
| 적합한 사용 사례 | 전문 검색, 배열, JSONB | 기하학적 데이터, 범위 쿼리 |
| 정확도 | 손실 없음 (lossless) | 손실 가능 (lossy) |

```sql
-- GIN: 전문 검색에 최적
CREATE INDEX idx_gin ON docs USING GIN (to_tsvector('english', content));

-- GiST: 기하학적 데이터에 적합
CREATE INDEX idx_gist ON locations USING GiST (coordinates);
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - GIN Indexes](https://www.postgresql.org/docs/current/gin.html)
- [PostgreSQL 공식 문서 - Full Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [PostgreSQL 공식 문서 - pg_trgm](https://www.postgresql.org/docs/current/pgtrgm.html)
