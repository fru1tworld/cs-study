# Chapter 73: 데이터베이스 물리적 저장소 (Database Physical Storage)

이 장에서는 PostgreSQL 데이터베이스에서 사용하는 물리적 저장소 형식에 대한 개요를 제공합니다.

## 목차

1. [데이터베이스 파일 레이아웃 (Database File Layout)](#1-데이터베이스-파일-레이아웃-database-file-layout)
2. [TOAST](#2-toast-the-oversized-attribute-storage-technique)
3. [프리 스페이스 맵 (Free Space Map)](#3-프리-스페이스-맵-free-space-map)
4. [가시성 맵 (Visibility Map)](#4-가시성-맵-visibility-map)
5. [초기화 포크 (The Initialization Fork)](#5-초기화-포크-the-initialization-fork)
6. [데이터베이스 페이지 레이아웃 (Database Page Layout)](#6-데이터베이스-페이지-레이아웃-database-page-layout)
7. [힙 전용 튜플 (Heap-Only Tuples, HOT)](#7-힙-전용-튜플-heap-only-tuples-hot)

---

## 1. 데이터베이스 파일 레이아웃 (Database File Layout)

PostgreSQL은 데이터를 파일 시스템에 특정 구조로 저장합니다. 이 섹션에서는 클러스터 디렉토리 구조, 데이터베이스별 하위 디렉토리, 테이블스페이스, 파일 명명 규칙에 대해 설명합니다.

### 1.1 클러스터 디렉토리 구조 (PGDATA)

`PGDATA` 디렉토리는 메인 클러스터 데이터 디렉토리입니다 (일반적으로 `/var/lib/pgsql/data`). 이 디렉토리에는 다음과 같은 파일과 하위 디렉토리가 포함됩니다.

#### 제어 파일 (Control Files)

| 파일명 | 설명 |
|--------|------|
| `PG_VERSION` | PostgreSQL의 메이저 버전 번호 |
| `postmaster.pid` | 현재 postmaster PID, 데이터 디렉토리 경로, 시작 타임스탬프, 포트, 소켓 디렉토리가 포함된 락 파일 |
| `postmaster.opts` | 마지막 서버 시작 시 사용된 명령줄 옵션 |
| `postgresql.auto.conf` | `ALTER SYSTEM`을 통해 설정된 구성 파라미터 |
| `current_logfiles` | 현재 작성 중인 로그 파일 |

#### 주요 하위 디렉토리

| 디렉토리 | 목적 |
|----------|------|
| `base/` | 데이터베이스별 하위 디렉토리 |
| `global/` | 클러스터 전체 테이블 (예: `pg_database`) |
| `pg_wal/` | Write Ahead Log (WAL) 파일 |
| `pg_xact/` | 트랜잭션 커밋 상태 데이터 |
| `pg_commit_ts/` | 트랜잭션 커밋 타임스탬프 |
| `pg_subtrans/` | 서브트랜잭션 상태 데이터 |
| `pg_multixact/` | 멀티트랜잭션 상태 (공유 행 락) |
| `pg_tblspc/` | 테이블스페이스에 대한 심볼릭 링크 |
| `pg_logical/` | 논리적 디코딩 상태 |
| `pg_replslot/` | 복제 슬롯 데이터 |
| `pg_stat/` | 영구 통계 파일 |
| `pg_stat_tmp/` | 임시 통계 파일 |
| `pg_notify/` | LISTEN/NOTIFY 데이터 |
| `pg_serial/` | 커밋된 직렬화 가능 트랜잭션 |
| `pg_snapshots/` | 내보낸 스냅샷 |
| `pg_twophase/` | 준비된 트랜잭션 상태 |
| `pg_dynshmem/` | 동적 공유 메모리 파일 |

### 1.2 데이터베이스별 구조 (Per-Database Structure)

각 데이터베이스는 `PGDATA/base/` 디렉토리 내에 OID(Object ID)를 이름으로 하는 하위 디렉토리를 가집니다:

```
PGDATA/base/[DATABASE_OID]/
```

이 디렉토리에는 다음이 저장됩니다:
- 시스템 카탈로그
- 사용자 테이블 및 인덱스
- 관련 메타데이터 파일

### 1.3 테이블 및 인덱스 파일 명명 규칙

#### 표준 릴레이션

파일은 filenode 번호(`pg_class.relfilenode`에서 찾을 수 있음)를 기반으로 명명됩니다:

| 포크 유형 | 파일명 | 설명 |
|-----------|--------|------|
| 메인 포크 (Main fork) | `[filenode]` | 실제 테이블/인덱스 데이터 |
| 프리 스페이스 맵 (Free Space Map) | `[filenode]_fsm` | 사용 가능한 빈 공간 추적 |
| 가시성 맵 (Visibility Map) | `[filenode]_vm` | 죽은 튜플이 없는 페이지 추적 |
| 초기화 포크 (Initialization fork) | `[filenode]_init` | 언로그 테이블/인덱스 전용 |

#### 임시 릴레이션

형식: `t_[BBB]_[FFF]`
- `BBB` = 백엔드 프로세스 번호
- `FFF` = Filenode 번호

#### 대용량 파일 (1 GB 초과)

파일은 1 GB 청크로 분할됩니다 (`--with-segsize`로 구성 가능):
- 첫 번째 세그먼트: `[filenode]`
- 후속 세그먼트: `[filenode].1`, `[filenode].2` 등

### 1.4 중요 참고사항

Filenode != OID: 테이블의 filenode가 종종 OID와 일치하지만, `TRUNCATE`, `REINDEX`, `CLUSTER`, `ALTER TABLE`과 같은 작업은 OID를 유지하면서 filenode를 변경할 수 있습니다.

시스템 카탈로그의 실제 filenode를 얻으려면 `pg_relation_filenode()` 함수를 사용하세요.

### 1.5 테이블스페이스 (Tablespaces)

사용자 정의 테이블스페이스는 `PGDATA/pg_tblspc/`에 심볼릭 링크를 사용합니다:

```
PGDATA/pg_tblspc/[TABLESPACE_OID] → /path/to/physical/tablespace
  └── PG_[VERSION]_[CATALOG_VERSION]/
      └── [DATABASE_OID]/
          └── [filenode]  (테이블/인덱스 저장)
```

기본 테이블스페이스:
- `pg_default` → `PGDATA/base/`
- `pg_global` → `PGDATA/global/`

### 1.6 임시 파일 (Temporary Files)

`PGDATA/base/pgsql_tmp/` (또는 테이블스페이스별 `pgsql_tmp/`)에 생성됩니다:

형식: `pgsql_tmp_[PPP]_[NNN]`
- `PPP` = 백엔드 프로세스 ID (PID)
- `NNN` = 다른 임시 파일을 구별하는 번호

### 1.7 유용한 함수

```sql
-- 릴레이션의 전체 경로 표시 (PGDATA에 상대적)
SELECT pg_relation_filepath('테이블명');

-- 실제 filenode 번호 반환
SELECT pg_relation_filenode('테이블명');
```

예제:

```sql
-- 테이블의 파일 경로 확인
SELECT pg_relation_filepath('users');
-- 결과: base/16384/16385

-- filenode 확인
SELECT relfilenode, relname FROM pg_class WHERE relname = 'users';

-- 데이터베이스 OID 확인
SELECT oid, datname FROM pg_database;
```

---

## 2. TOAST (The Oversized-Attribute Storage Technique)

TOAST는 PostgreSQL이 고정 페이지 크기(일반적으로 8 kB)를 초과하는 대용량 필드 값을 처리하는 기술입니다. PostgreSQL은 튜플이 여러 페이지에 걸쳐 저장되는 것을 허용하지 않으므로, 대용량 값은 투명하게 압축되거나 여러 물리적 행으로 분할됩니다.

### 2.1 TOAST 작동 원리

#### 기본 메커니즘

- 가변 길이(varlena) 표현 을 가진 데이터 타입만 TOAST를 지원합니다
- 처음 4바이트는 일반적으로 값의 전체 길이를 포함합니다
- TOAST는 길이 워드의 2비트를 사용하여 특수 표현을 인코딩합니다
- 최대 논리적 크기: 비트 할당으로 인해 1 GB (2^30 - 1 바이트)

#### 길이 워드 인코딩

| 비트 상태 | 의미 |
|-----------|------|
| 두 비트 모두 0 | 일반적인 TOAST되지 않은 값 |
| 상위/하위 비트 설정 | 단일 바이트 헤더 (127바이트 미만 값용) |
| 인접 비트 설정 | 데이터가 압축됨, 사용 전 압축 해제 필요 |
| 특수 케이스 (단일 바이트 헤더의 모든 비트 0) | 외부 저장 데이터에 대한 포인터 |

### 2.2 TOAST 활성화 조건

TOAST 관리 코드는 다음 조건에서 트리거됩니다:
- 행 값이 `TOAST_TUPLE_THRESHOLD`(기본값: 2 kB)를 초과할 때
- 행이 `TOAST_TUPLE_TARGET`(기본값: 2 kB, 조정 가능)보다 작아질 때까지 압축 및/또는 외부 저장 수행

```sql
-- 테이블의 TOAST 타겟 조정
ALTER TABLE 테이블명 SET (toast_tuple_target = N);
```

### 2.3 네 가지 TOAST 저장 전략

| 전략 | 압축 | 외부 저장 | 사용 사례 |
|------|------|-----------|-----------|
| PLAIN | 불가 | 불가 | TOAST 불가능한 데이터 타입 전용 |
| EXTENDED | 가능 | 가능 | 대부분의 TOAST 가능 타입의 기본값 |
| EXTERNAL | 불가 | 가능 | `text`/`bytea`의 빠른 부분 문자열 연산 (저장 비용 증가) |
| MAIN | 가능 | 불가* | *행이 여전히 너무 큰 경우에만 최후의 수단으로 사용 |

```sql
-- 저장 전략 설정
ALTER TABLE 테이블명 ALTER COLUMN 컬럼명 SET STORAGE EXTENDED;
-- 또는 PLAIN, EXTERNAL, MAIN
```

### 2.4 압축 구성

```sql
-- 테이블 생성 시 압축 방법 지정
CREATE TABLE 테이블명 (
    컬럼명 TEXT COMPRESSION pglz  -- 또는 'default'
);

-- 기본 압축 방법 확인
SHOW default_toast_compression;
```

### 2.5 디스크 상의 TOAST 저장소 (Out-of-Line, On-Disk)

#### 구조

- TOAST 가능 컬럼이 있는 각 테이블에 대해 연관된 TOAST 테이블 이 생성됩니다
- TOAST 테이블 OID는 `pg_class.reltoastrelid`에 저장됩니다
- 외부 저장 값은 최대 `TOAST_MAX_CHUNK_SIZE`(기본값 약 2000바이트) 청크로 분할됩니다

#### TOAST 테이블 컬럼

```
chunk_id     - TOAST된 값을 식별하는 OID
chunk_seq    - 값 내의 순서 번호
chunk_data   - 실제 데이터 청크
```

#### TOAST 포인터 데이텀

저장 내용:
- TOAST 테이블 OID
- 값 OID (`chunk_id`)
- 논리적 크기
- 물리적 크기
- 압축 방법

총 크기: 실제 값 크기에 관계없이 18바이트 (varlena 헤더 포함)

#### UPDATE 최적화

- UPDATE 중 변경되지 않은 필드 값은 그대로 유지됩니다
- 외부 저장 값이 변경되지 않으면 TOAST 오버헤드가 없습니다

### 2.6 메모리 상의 TOAST 저장소 (Out-of-Line, In-Memory)

#### 간접 TOAST 포인터 (Indirect TOAST Pointers)

- 서버 프로세스 메모리의 비간접 varlena 값을 가리킵니다
- 단기간만 사용 (디스크에 지속 불가)
- 1 GB 물리적 튜플 제한을 피하기 위해 논리적 디코딩 중 사용됩니다
- 생성자가 데이터 생존에 책임

#### 확장 TOAST 포인터 (Expanded TOAST Pointers)

- 복잡한 데이터 타입(예: 배열)에 대한 최적화된 표현
- 예: PostgreSQL 배열은 더 빠른 계산을 위해 인덱싱된 요소 위치로 분해됩니다
- 읽기-쓰기 vs 읽기 전용 변형:
  - 읽기-쓰기: 함수가 값을 제자리에서 수정 가능
  - 읽기 전용: 불필요한 복사를 피하기 위해 수정 전 복사 필요

### 2.7 TOAST의 주요 이점

1. 더 작은 메인 테이블 - 메인 테이블에는 키 값만 포함; 대용량 값은 클라이언트로 전송할 때만 가져옴
2. 더 나은 버퍼 캐시 활용 - 공유 버퍼 캐시에 더 많은 행이 들어감
3. 효율적인 정렬 - 정렬 세트 축소; 더 많은 정렬이 완전히 메모리에서 수행됨
4. 공간 효율성 - 실제 예: HTML 페이지 테이블이 원시 데이터 크기의 약 50%에 저장, 메인 테이블은 전체 데이터의 약 10%

### 2.8 TOAST 예제

```sql
-- TOAST 테이블 확인
SELECT
    c.relname AS 테이블명,
    t.relname AS toast_테이블명,
    c.reltoastrelid AS toast_oid
FROM pg_class c
JOIN pg_class t ON c.reltoastrelid = t.oid
WHERE c.relkind = 'r' AND c.relname = 'my_table';

-- 컬럼의 저장 전략 확인
SELECT
    attname AS 컬럼명,
    attstorage AS 저장전략,
    CASE attstorage
        WHEN 'p' THEN 'PLAIN'
        WHEN 'e' THEN 'EXTERNAL'
        WHEN 'm' THEN 'MAIN'
        WHEN 'x' THEN 'EXTENDED'
    END AS 저장전략_설명
FROM pg_attribute
WHERE attrelid = 'my_table'::regclass
AND attnum > 0;

-- TOAST 테이블 크기 확인
SELECT
    pg_size_pretty(pg_relation_size('my_table')) AS 테이블_크기,
    pg_size_pretty(pg_relation_size('my_table', 'toast')) AS toast_크기;

-- 대용량 데이터 삽입 예제
CREATE TABLE large_text_table (
    id SERIAL PRIMARY KEY,
    content TEXT
);

-- EXTENDED 전략 (기본값) - 압축 후 외부 저장
INSERT INTO large_text_table (content)
VALUES (repeat('PostgreSQL TOAST 테스트 ', 10000));

-- EXTERNAL 전략으로 변경 - 압축 없이 외부 저장
ALTER TABLE large_text_table ALTER COLUMN content SET STORAGE EXTERNAL;
```

---

## 3. 프리 스페이스 맵 (Free Space Map)

프리 스페이스 맵(FSM)은 PostgreSQL이 힙(heap)과 인덱스 릴레이션(해시 인덱스 제외)에서 사용 가능한 공간을 추적하는 데 사용하는 데이터 구조입니다. 새 행을 삽입할 때 충분한 빈 공간이 있는 페이지를 효율적으로 찾을 수 있게 합니다.

### 3.1 저장 위치

- 파일 명명 규칙: 릴레이션의 filenode 번호에 `_fsm` 접미사를 붙인 별도의 릴레이션 포크에 저장됩니다
- 예: 릴레이션의 filenode가 `12345`면, FSM은 메인 릴레이션 파일과 같은 디렉토리의 `12345_fsm` 파일에 저장됩니다

### 3.2 구조

FSM은 여러 레벨의 FSM 페이지 트리 로 구성됩니다:

#### 리프 레벨 (Leaf Level)

- 힙 또는 인덱스 페이지의 빈 공간 정보를 저장합니다
- 페이지당 1바이트 를 사용하여 사용 가능한 공간을 나타냅니다
- 실제 페이지 정보를 포함하는 최하위 레벨입니다

#### 상위 레벨 (Upper Levels)

- 하위 레벨의 정보를 집계합니다
- 계층적 구조를 형성합니다

### 3.3 내부 구성

- 각 FSM 페이지는 배열로 저장된 이진 트리 를 포함합니다
- 노드당 1바이트
- 리프 노드: 힙 페이지 또는 하위 레벨 FSM 페이지를 나타냅니다
- 비리프 노드: 자식 노드 값 중 더 높은 값을 저장합니다
- 루트 노드: 모든 리프 노드의 최대값을 포함합니다 (사용 가능한 가장 큰 빈 공간을 나타냄)

### 3.4 주요 특징

1. 효율적인 공간 추적: 페이지당 1바이트의 최소 저장소를 사용하여 빈 공간 표현
2. 계층적 집계: 상위 레벨이 하위 레벨 정보를 효율적으로 요약
3. 빠른 공간 조회: 루트의 최대값으로 사용 가능한 공간이 있는 페이지를 빠르게 식별

### 3.5 FSM 확인 예제

```sql
-- pg_freespacemap 확장 설치
CREATE EXTENSION IF NOT EXISTS pg_freespacemap;

-- 테이블의 각 페이지별 빈 공간 확인
SELECT
    blkno AS 페이지번호,
    avail AS 사용가능바이트
FROM pg_freespace('테이블명')
ORDER BY blkno
LIMIT 20;

-- 테이블의 평균 빈 공간 확인
SELECT
    AVG(avail) AS 평균_빈공간,
    MAX(avail) AS 최대_빈공간,
    MIN(avail) AS 최소_빈공간
FROM pg_freespace('테이블명');

-- FSM 파일 크기 확인
SELECT pg_size_pretty(pg_relation_size('테이블명', 'fsm')) AS fsm_크기;
```

---

## 4. 가시성 맵 (Visibility Map)

가시성 맵(VM)은 PostgreSQL에서 어떤 힙 페이지가 모든 활성 트랜잭션에 보이는 튜플만 포함하고 있는지, 그리고 어떤 페이지가 동결된(frozen) 튜플만 포함하고 있는지를 추적하는 메커니즘입니다.

### 4.1 저장 위치

- 메인 릴레이션 데이터와 함께 별도의 릴레이션 포크로 저장됩니다
- 릴레이션의 filenode 번호에 `_vm` 접미사를 붙여 명명됩니다
- 예: filenode가 `12345`면, VM 파일은 같은 디렉토리의 `12345_vm`입니다
- 참고: 인덱스는 가시성 맵이 없습니다

### 4.2 비트 구조

가시성 맵은 힙 페이지당 2비트 를 저장합니다:

#### 비트 1: 모두 가시 플래그 (All-Visible Flag)

- 설정 시: 페이지가 모두 가시 상태 (VACUUM이 필요한 튜플이 없음)
- 사용 사례: 힙에 접근하지 않고 인덱스 튜플만으로 쿼리에 응답하는 인덱스 전용 스캔(Index-Only Scan) 가능

#### 비트 2: 동결 플래그 (Frozen Flag)

- 설정 시: 페이지의 모든 튜플이 동결됨
- 이점: 래핑 방지 VACUUM이 해당 페이지를 다시 방문할 필요 없음

### 4.3 보수적 설계

가시성 맵은 보수적으로 작동합니다:
- 비트 설정됨: 조건이 참임을 보장
- 비트 해제됨: 조건이 참일 수도 있고 아닐 수도 있음

### 4.4 연산

| 연산 | 동작 | 상세 |
|------|------|------|
| VACUUM | 비트 설정 | VM 비트를 설정하는 유일한 연산 |
| 데이터 수정 | 비트 해제 | INSERT, UPDATE, DELETE가 관련 비트를 해제 |

### 4.5 가시성 맵 확인 예제

```sql
-- pg_visibility 확장 설치
CREATE EXTENSION IF NOT EXISTS pg_visibility;

-- 테이블의 가시성 맵 정보 확인
SELECT
    blkno AS 페이지번호,
    all_visible AS 모두가시,
    all_frozen AS 모두동결
FROM pg_visibility('테이블명')
ORDER BY blkno
LIMIT 20;

-- 가시성 맵 요약
SELECT
    COUNT(*) AS 총_페이지수,
    COUNT(*) FILTER (WHERE all_visible) AS 모두가시_페이지수,
    COUNT(*) FILTER (WHERE all_frozen) AS 모두동결_페이지수,
    ROUND(100.0 * COUNT(*) FILTER (WHERE all_visible) / COUNT(*), 2) AS 가시_비율
FROM pg_visibility('테이블명');

-- VM 파일 크기 확인
SELECT pg_size_pretty(pg_relation_size('테이블명', 'vm')) AS vm_크기;

-- 인덱스 전용 스캔 가능 여부 확인 (힙 접근 없이)
EXPLAIN (ANALYZE, BUFFERS)
SELECT id FROM 테이블명 WHERE id = 100;
```

---

## 5. 초기화 포크 (The Initialization Fork)

### 5.1 개요

각 언로그 테이블(unlogged table) 과 언로그 테이블의 인덱스 에는 초기화 포크가 있습니다.

### 5.2 정의

초기화 포크는 적절한 타입의 빈 테이블 또는 인덱스 입니다.

### 5.3 목적과 동작

크래시로 인해 언로그 테이블을 빈 상태로 재설정해야 할 때:
1. 초기화 포크가 메인 포크 위에 복사 됩니다
2. 다른 포크는 삭제 됩니다 (필요에 따라 자동으로 재생성됨)

### 5.4 핵심 포인트

- 초기화 포크는 언로그 테이블의 템플릿/백업 역할을 합니다
- 크래시 후 언로그 테이블을 깨끗한 상태로 자동 복구할 수 있게 합니다
- 이 메커니즘은 수동 개입 없이 언로그 테이블을 안정적으로 재설정할 수 있게 보장합니다

### 5.5 언로그 테이블 예제

```sql
-- 언로그 테이블 생성
CREATE UNLOGGED TABLE session_data (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100),
    data JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 초기화 포크 파일 확인
SELECT
    pg_relation_filepath('session_data') AS 메인파일,
    pg_relation_filepath('session_data') || '_init' AS 초기화포크
;

-- 언로그 테이블의 인덱스도 초기화 포크를 가짐
CREATE INDEX idx_session ON session_data(session_id);

-- 참고: 크래시 후 언로그 테이블은 자동으로 비워짐
-- WAL에 기록되지 않으므로 복구 불가
```

---

## 6. 데이터베이스 페이지 레이아웃 (Database Page Layout)

PostgreSQL은 모든 테이블과 인덱스 데이터를 고정 크기 페이지(일반적으로 8 kB)로 저장합니다. 이 섹션에서는 이러한 페이지의 물리적 구조를 설명합니다.

### 6.1 전체 페이지 구조

PostgreSQL 페이지는 다섯 부분 으로 구성됩니다:

| 구성 요소 | 크기 | 설명 |
|-----------|------|------|
| PageHeaderData | 24 바이트 | 빈 공간 포인터를 포함한 일반 페이지 정보 |
| ItemIdData | 항목당 4 바이트 | 아이템 식별자 배열 (오프셋, 길이 쌍) |
| Free Space | 가변 | 할당되지 않은 공간; 아이템 ID는 시작부터, 아이템은 끝부터 할당 |
| Items | 가변 | 실제 데이터 (테이블의 행, 인덱스의 항목) |
| Special Space | 가변 | 인덱스 접근 방법별 데이터; 일반 테이블에서는 비어 있음 |

```
+------------------+
|  PageHeaderData  |  ← 24 바이트
+------------------+
|    ItemIdData    |  ← 아이템당 4 바이트
|       ...        |
+------------------+
|                  |
|   Free Space     |  ← 빈 공간
|                  |
+------------------+
|                  |
|     Items        |  ← 실제 데이터 (행/항목)
|       ...        |
+------------------+
|  Special Space   |  ← 인덱스 전용 (테이블에서는 비어 있음)
+------------------+
```

### 6.2 PageHeaderData 레이아웃 (24 바이트)

| 필드 | 타입 | 길이 | 설명 |
|------|------|------|------|
| `pd_lsn` | PageXLogRecPtr | 8 바이트 | LSN: 페이지에 대한 마지막 변경의 마지막 WAL 레코드 다음 바이트 |
| `pd_checksum` | uint16 | 2 바이트 | 페이지 체크섬 (`-k` 플래그로 활성화된 경우) |
| `pd_flags` | uint16 | 2 바이트 | 플래그 비트 |
| `pd_lower` | LocationIndex | 2 바이트 | 빈 공간 시작 오프셋 |
| `pd_upper` | LocationIndex | 2 바이트 | 빈 공간 끝 오프셋 |
| `pd_special` | LocationIndex | 2 바이트 | 특수 공간 시작 오프셋 |
| `pd_pagesize_version` | uint16 | 2 바이트 | 페이지 크기 및 레이아웃 버전 번호 |
| `pd_prune_xid` | TransactionId | 4 바이트 | 페이지에서 가장 오래된 정리되지 않은 XMAX, 또는 0 |

현재 버전: 4 (PostgreSQL 8.3 이후)

### 6.3 ItemIdData 구조

- 각 아이템 식별자는 4 바이트 필요
- 포함 내용:
  - 아이템 시작에 대한 바이트 오프셋
  - 바이트 단위 길이
  - 해석에 영향을 주는 속성 비트
- 필요에 따라 빈 공간 시작부터 할당됨
- 해제될 때까지 이동하지 않아 인덱스로 장기 참조 가능

### 6.4 테이블 행 레이아웃 (HeapTupleHeaderData)

고정 헤더 구조 다음에 선택적 널 비트맵, 선택적 객체 ID, 사용자 데이터가 옵니다.

#### HeapTupleHeaderData 레이아웃 (대부분 플랫폼에서 23 바이트)

| 필드 | 타입 | 길이 | 설명 |
|------|------|------|------|
| `t_xmin` | TransactionId | 4 바이트 | 삽입 XID 스탬프 |
| `t_xmax` | TransactionId | 4 바이트 | 삭제 XID 스탬프 |
| `t_cid` | CommandId | 4 바이트 | 삽입 및/또는 삭제 CID 스탬프 (`t_xvac`과 오버레이) |
| `t_xvac` | TransactionId | 4 바이트 | 행 버전을 이동하는 VACUUM 연산의 XID |
| `t_ctid` | ItemPointerData | 6 바이트 | 이 또는 새로운 행 버전의 현재 TID |
| `t_infomask2` | uint16 | 2 바이트 | 속성 수 + 플래그 비트 |
| `t_infomask` | uint16 | 2 바이트 | 다양한 플래그 비트 |
| `t_hoff` | uint8 | 1 바이트 | 사용자 데이터에 대한 오프셋 (MAXALIGN 배수) |

### 6.5 선택적 구성 요소

#### 1. 널 비트맵 (Null Bitmap)

- `t_infomask`에 `HEAP_HASNULL` 비트가 설정되면 존재
- 데이터 컬럼당 1비트
- 1 = not-null, 0 = null
- 고정 헤더 직후에 시작
- 모든 컬럼이 not-null이면 생략

#### 2. 객체 ID 필드 (Object ID Field)

- `t_infomask`에 `HEAP_HASOID_OLD` 비트가 설정되면 존재
- `t_hoff` 경계 직전에 나타남
- 적절히 정렬됨

#### 3. 사용자 데이터 (User Data)

- `t_hoff`가 가리키는 오프셋에서 시작
- MAXALIGN 배수여야 함

### 6.6 가변 길이 데이터

가변 길이 필드(`attlen = -1`인 경우)는 `struct varlena`라는 공통 헤더 구조를 공유합니다:
- 저장된 값의 전체 길이
- 다음을 나타내는 플래그 비트:
  - 인라인 vs TOAST 테이블 저장
  - 압축 상태 (섹션 2 참조)

### 6.7 페이지 레이아웃 확인 예제

```sql
-- pageinspect 확장 설치
CREATE EXTENSION IF NOT EXISTS pageinspect;

-- 페이지 헤더 정보 확인
SELECT * FROM page_header(get_raw_page('테이블명', 0));

-- 튜플 정보 확인
SELECT
    lp AS 라인포인터,
    lp_off AS 오프셋,
    lp_flags AS 플래그,
    lp_len AS 길이,
    t_xmin,
    t_xmax,
    t_ctid,
    t_infomask2,
    t_infomask,
    t_hoff
FROM heap_page_items(get_raw_page('테이블명', 0))
LIMIT 10;

-- 페이지 크기와 사용량 확인
SELECT
    pg_relation_size('테이블명') / 8192 AS 총_페이지수,
    pg_relation_size('테이블명') AS 총_바이트;

-- 튜플의 상세 정보 (t_infomask 비트 해석)
SELECT
    lp,
    t_xmin,
    t_xmax,
    CASE WHEN (t_infomask & 256) > 0 THEN 'HEAP_HASNULL' ELSE '' END AS has_null,
    CASE WHEN (t_infomask & 2048) > 0 THEN 'HEAP_XMIN_COMMITTED' ELSE '' END AS xmin_committed,
    CASE WHEN (t_infomask & 4096) > 0 THEN 'HEAP_XMAX_COMMITTED' ELSE '' END AS xmax_committed
FROM heap_page_items(get_raw_page('테이블명', 0))
WHERE lp_len > 0;
```

### 6.8 주요 참조

- 헤더 세부 정보: `src/include/storage/bufpage.h`
- 튜플 세부 정보: `src/include/access/htup_details.h`
- 헬퍼 함수: `heap_getattr()`, `fastgetattr()`, `heap_getsysattr()`
- 속성 메타데이터: `pg_attribute` 테이블 (`attlen`, `attalign`)

---

## 7. 힙 전용 튜플 (Heap-Only Tuples, HOT)

힙 전용 튜플(HOT)은 MVCC(다중 버전 동시성 제어)를 활용하여 UPDATE 연산의 오버헤드를 줄이는 PostgreSQL의 최적화 기법입니다. MVCC는 높은 동시성을 가능하게 하지만, 일반적으로 모든 행 업데이트에 대해 새 인덱스 항목을 생성해야 하므로 비용이 많이 들 수 있습니다.

### 7.1 HOT 적격 조건

HOT 최적화는 다음 두 조건이 모두 충족될 때 적용됩니다:

1. 업데이트가 인덱싱된 컬럼을 수정하지 않음 (BRIN 요약 인덱스 제외)
2. 이전 행이 포함된 페이지에 업데이트된 행을 위한 충분한 빈 공간이 존재

### 7.2 HOT이 제공하는 주요 최적화

#### 1. 새 인덱스 항목 불필요

- 업데이트된 행은 일반 인덱스에 새 인덱스 항목이 필요하지 않음
- 요약 인덱스(BRIN 등)는 여전히 업데이트가 필요할 수 있음

#### 2. 효율적인 행 버전 정리

- 행이 여러 번 업데이트될 때, 중간 버전은 일반 연산(`SELECT` 포함) 중에 완전히 제거 가능
- 주기적 VACUUM 연산을 기다릴 필요 없음
- 프로세스 세부 정보:
  - 인덱스는 계속 원래 행의 페이지 아이템 식별자를 가리킴
  - 이전 버전의 튜플 데이터가 제거됨
  - 아이템 식별자는 가장 오래된 아직 가시적인 버전을 가리키는 리다이렉트 로 변환됨
  - 더 이상 어떤 트랜잭션에도 보이지 않는 중간 버전은 완전히 제거됨
  - 아이템 식별자는 재사용 가능해짐

### 7.3 HOT 체인 (HOT Chain)

HOT 업데이트는 같은 페이지 내에서 튜플 버전들의 체인을 형성합니다:

```
인덱스 → [원래 아이템 ID] → 튜플 v1 → 튜플 v2 → 튜플 v3 (최신)
          (리다이렉트)
```

### 7.4 최적화 권장 사항

HOT 가능성 증가 방법:
- 테이블의 `fillfactor` 파라미터를 낮춰 더 많은 빈 공간 예약
- 이렇게 하면 업데이트된 행이 같은 페이지에 들어갈 확률이 증가

```sql
-- fillfactor를 90%로 설정하여 10%의 빈 공간 예약
CREATE TABLE hot_optimized (
    id SERIAL PRIMARY KEY,
    data VARCHAR(100),
    updated_at TIMESTAMP
) WITH (fillfactor = 90);

-- 기존 테이블의 fillfactor 변경
ALTER TABLE existing_table SET (fillfactor = 80);

-- VACUUM FULL 또는 CLUSTER로 적용
VACUUM FULL existing_table;
```

참고: 새 행이 새 페이지로 마이그레이션되고 기존 페이지에 빈 공간이 축적되면 HOT 업데이트는 자연스럽게 발생합니다.

### 7.5 HOT 활동 모니터링

시스템 뷰 `pg_stat_all_tables` 를 사용하여 모니터링:
- HOT 업데이트 빈도
- 비-HOT 업데이트 빈도

```sql
-- HOT 업데이트 통계 확인
SELECT
    relname AS 테이블명,
    n_tup_upd AS 총_업데이트,
    n_tup_hot_upd AS HOT_업데이트,
    CASE
        WHEN n_tup_upd > 0
        THEN ROUND(100.0 * n_tup_hot_upd / n_tup_upd, 2)
        ELSE 0
    END AS HOT_비율
FROM pg_stat_user_tables
WHERE n_tup_upd > 0
ORDER BY n_tup_upd DESC;

-- 특정 테이블의 HOT 효율성 확인
SELECT
    relname,
    n_tup_upd AS 업데이트_수,
    n_tup_hot_upd AS HOT_업데이트_수,
    n_live_tup AS 라이브_튜플,
    n_dead_tup AS 데드_튜플
FROM pg_stat_user_tables
WHERE relname = '테이블명';
```

### 7.6 HOT 프루닝 (HOT Pruning)

HOT 프루닝은 페이지 내에서 더 이상 필요하지 않은 튜플 버전을 정리하는 과정입니다:

```sql
-- pageinspect로 HOT 체인 확인
SELECT
    lp AS 라인포인터,
    lp_flags AS 플래그,
    t_ctid,
    CASE lp_flags
        WHEN 0 THEN 'LP_UNUSED'
        WHEN 1 THEN 'LP_NORMAL'
        WHEN 2 THEN 'LP_REDIRECT'
        WHEN 3 THEN 'LP_DEAD'
    END AS 플래그_설명
FROM heap_page_items(get_raw_page('테이블명', 0))
ORDER BY lp;
```

### 7.7 HOT의 이점 요약

1. 인덱스 유지 관리 감소: 인덱싱되지 않은 컬럼 업데이트 시 인덱스 업데이트 불필요
2. 디스크 I/O 감소: 같은 페이지 내 업데이트로 새 페이지 할당 불필요
3. 자동 정리: SELECT 쿼리 중에도 오래된 버전 정리 가능
4. VACUUM 부하 감소: 일부 정리 작업이 자동으로 수행됨

---

## 요약

PostgreSQL의 물리적 저장소 구조를 이해하면 데이터베이스의 성능을 최적화하고 문제를 진단하는 데 도움이 됩니다:

| 구성 요소 | 주요 목적 |
|-----------|-----------|
| 파일 레이아웃 | 데이터베이스 파일 구조와 위치 이해 |
| TOAST | 대용량 데이터의 효율적 저장 |
| FSM | 빈 공간 추적으로 효율적인 행 삽입 |
| VM | 가시성 정보로 인덱스 전용 스캔 최적화 |
| 초기화 포크 | 언로그 테이블의 크래시 복구 |
| 페이지 레이아웃 | 행 데이터의 물리적 구조 |
| HOT | 업데이트 성능 최적화 |

이러한 내부 구조에 대한 이해는 `VACUUM`, `ANALYZE`, 인덱스 관리 등의 유지 관리 작업을 더 효과적으로 수행하는 데 필수적입니다.
