# PostgreSQL 내부: 저장소, 카탈로그, 프로토콜, 플래너

## Chapter 73: 데이터베이스 물리적 저장소 (Database Physical Storage)

이 장에서는 PostgreSQL 데이터베이스에서 사용하는 물리적 저장소 형식의 개요를 다룸.

### 목차

1. [데이터베이스 파일 레이아웃 (Database File Layout)](#1-데이터베이스-파일-레이아웃-database-file-layout)
2. [TOAST](#2-toast-the-oversized-attribute-storage-technique)
3. [프리 스페이스 맵 (Free Space Map)](#3-프리-스페이스-맵-free-space-map)
4. [가시성 맵 (Visibility Map)](#4-가시성-맵-visibility-map)
5. [초기화 포크 (The Initialization Fork)](#5-초기화-포크-the-initialization-fork)
6. [데이터베이스 페이지 레이아웃 (Database Page Layout)](#6-데이터베이스-페이지-레이아웃-database-page-layout)
7. [힙 전용 튜플 (Heap-Only Tuples, HOT)](#7-힙-전용-튜플-heap-only-tuples-hot)

---

### 1. 데이터베이스 파일 레이아웃 (Database File Layout)

PostgreSQL은 데이터를 파일 시스템에 특정 구조로 저장함.

#### 1.1 클러스터 디렉토리 구조 (PGDATA)

`PGDATA` 디렉토리는 메인 클러스터 데이터 디렉토리(일반적으로 `/var/lib/pgsql/data`). 이 디렉토리에는 다음과 같은 파일과 하위 디렉토리가 포함됨.

##### 제어 파일 (Control Files)

- `PG_VERSION`: PostgreSQL의 메이저 버전 번호
- `postmaster.pid`: 현재 postmaster PID, 데이터 디렉토리 경로, 시작 타임스탬프, 포트, 소켓 디렉토리가 포함된 락 파일
- `postmaster.opts`: 마지막 서버 시작 시 사용된 명령줄 옵션
- `postgresql.auto.conf`: `ALTER SYSTEM`을 통해 설정된 구성 파라미터
- `current_logfiles`: 현재 작성 중인 로그 파일

##### 주요 하위 디렉토리

- `base/`: 데이터베이스별 하위 디렉토리
- `global/`: 클러스터 전체 테이블(예: `pg_database`)
- `pg_wal/`: Write Ahead Log (WAL) 파일
- `pg_xact/`: 트랜잭션 커밋 상태 데이터
- `pg_commit_ts/`: 트랜잭션 커밋 타임스탬프
- `pg_subtrans/`: 서브트랜잭션 상태 데이터
- `pg_multixact/`: 멀티트랜잭션 상태(공유 행 락)
- `pg_tblspc/`: 테이블스페이스에 대한 심볼릭 링크
- `pg_logical/`: 논리적 디코딩 상태
- `pg_replslot/`: 복제 슬롯 데이터
- `pg_stat/`: 영구 통계 파일
- `pg_stat_tmp/`: 임시 통계 파일
- `pg_notify/`: LISTEN/NOTIFY 데이터
- `pg_serial/`: 커밋된 직렬화 가능 트랜잭션
- `pg_snapshots/`: 내보낸 스냅샷
- `pg_twophase/`: 준비된 트랜잭션 상태
- `pg_dynshmem/`: 동적 공유 메모리 파일

#### 1.2 데이터베이스별 구조 (Per-Database Structure)

각 데이터베이스는 `PGDATA/base/` 디렉토리 내에 OID(Object ID)를 이름으로 하는 하위 디렉토리를 가짐:

```
PGDATA/base/[DATABASE_OID]/
```

이 디렉토리에는 다음이 저장됨:
- 시스템 카탈로그
- 사용자 테이블 및 인덱스
- 관련 메타데이터 파일

#### 1.3 테이블 및 인덱스 파일 명명 규칙

##### 표준 릴레이션

파일은 filenode 번호(`pg_class.relfilenode`에서 확인 가능)를 기반으로 명명됨:

- 메인 포크(Main fork): `[filenode]` — 실제 테이블/인덱스 데이터
- 프리 스페이스 맵(Free Space Map): `[filenode]_fsm` — 사용 가능한 빈 공간 추적
- 가시성 맵(Visibility Map): `[filenode]_vm` — 죽은 튜플이 없는 페이지 추적
- 초기화 포크(Initialization fork): `[filenode]_init` — 언로그 테이블/인덱스 전용

##### 임시 릴레이션

형식: `t_[BBB]_[FFF]`
- `BBB` = 백엔드 프로세스 번호
- `FFF` = Filenode 번호

##### 대용량 파일 (1 GB 초과)

파일은 1 GB 청크로 분할됨(`--with-segsize`로 구성 가능):
- 첫 번째 세그먼트: `[filenode]`
- 후속 세그먼트: `[filenode].1`, `[filenode].2` 등

#### 1.4 중요 참고사항

Filenode != OID: 테이블의 filenode가 종종 OID와 일치하지만, `TRUNCATE`, `REINDEX`, `CLUSTER`, `ALTER TABLE` 같은 작업은 OID를 유지하면서 filenode를 변경 가능.

시스템 카탈로그의 실제 filenode를 얻으려면 `pg_relation_filenode()` 함수 사용.

#### 1.5 테이블스페이스 (Tablespaces)

사용자 정의 테이블스페이스는 `PGDATA/pg_tblspc/`에 심볼릭 링크를 사용함:

```
PGDATA/pg_tblspc/[TABLESPACE_OID] → /path/to/physical/tablespace
  └── PG_[VERSION]_[CATALOG_VERSION]/
      └── [DATABASE_OID]/
          └── [filenode]  (테이블/인덱스 저장)
```

기본 테이블스페이스:
- `pg_default` → `PGDATA/base/`
- `pg_global` → `PGDATA/global/`

#### 1.6 임시 파일 (Temporary Files)

`PGDATA/base/pgsql_tmp/`(또는 테이블스페이스별 `pgsql_tmp/`)에 생성됨:

형식: `pgsql_tmp_[PPP]_[NNN]`
- `PPP` = 백엔드 프로세스 ID (PID)
- `NNN` = 다른 임시 파일을 구별하는 번호

#### 1.7 유용한 함수

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

### 2. TOAST (The Oversized-Attribute Storage Technique)

TOAST는 PostgreSQL이 고정 페이지 크기(일반적으로 8 kB)를 초과하는 대용량 필드 값을 처리하는 기술. PostgreSQL은 튜플이 여러 페이지에 걸쳐 저장되는 것을 허용하지 않음 → 대용량 값은 투명하게 압축되거나 여러 물리적 행으로 분할됨.

#### 2.1 TOAST 작동 원리

##### 기본 메커니즘

- 가변 길이(varlena) 표현을 가진 데이터 타입만 TOAST 지원
- 처음 4바이트는 일반적으로 값의 전체 길이를 포함
- TOAST는 길이 워드의 2비트를 사용해 특수 표현을 인코딩
- 최대 논리적 크기: 비트 할당으로 인해 1 GB (2^30 - 1 바이트)

##### 길이 워드 인코딩

- 두 비트 모두 0: 일반적인 TOAST되지 않은 값
- 상위/하위 비트 설정: 단일 바이트 헤더 (127바이트 미만 값용)
- 인접 비트 설정: 데이터가 압축됨, 사용 전 압축 해제 필요
- 특수 케이스(단일 바이트 헤더의 모든 비트 0): 외부 저장 데이터에 대한 포인터

#### 2.2 TOAST 활성화 조건

TOAST 관리 코드는 다음 조건에서 작동함:
- 행 값이 `TOAST_TUPLE_THRESHOLD`(기본값: 2 kB)를 초과할 때
- 행이 `TOAST_TUPLE_TARGET`(기본값: 2 kB, 조정 가능)보다 작아질 때까지 압축 및/또는 외부 저장 수행

```sql
-- 테이블의 TOAST 타겟 조정
ALTER TABLE 테이블명 SET (toast_tuple_target = N);
```

#### 2.3 네 가지 TOAST 저장 전략

- PLAIN
  - 압축: 불가
  - 외부 저장: 불가
  - 사용 사례: TOAST 불가능한 데이터 타입 전용
- EXTENDED
  - 압축: 가능
  - 외부 저장: 가능
  - 사용 사례: 대부분의 TOAST 가능 타입의 기본값
- EXTERNAL
  - 압축: 불가
  - 외부 저장: 가능
  - 사용 사례: `text`/`bytea`의 빠른 부분 문자열 연산(저장 비용 증가)
- MAIN
  - 압축: 가능
  - 외부 저장: 불가(행이 여전히 너무 큰 경우에만 최후의 수단으로 사용)

```sql
-- 저장 전략 설정
ALTER TABLE 테이블명 ALTER COLUMN 컬럼명 SET STORAGE EXTENDED;
-- 또는 PLAIN, EXTERNAL, MAIN
```

#### 2.4 압축 구성

```sql
-- 테이블 생성 시 압축 방법 지정
CREATE TABLE 테이블명 (
    컬럼명 TEXT COMPRESSION pglz  -- 또는 'default'
);

-- 기본 압축 방법 확인
SHOW default_toast_compression;
```

#### 2.5 디스크 상의 TOAST 저장소 (Out-of-Line, On-Disk)

##### 구조

- TOAST 가능 컬럼이 있는 각 테이블에는 연관된 TOAST 테이블이 생성됨
- TOAST 테이블 OID는 `pg_class.reltoastrelid`에 저장됨
- 외부 저장 값은 최대 `TOAST_MAX_CHUNK_SIZE`(기본값 약 2000바이트) 청크로 분할됨

##### TOAST 테이블 컬럼

```
chunk_id     - TOAST된 값을 식별하는 OID
chunk_seq    - 값 내의 순서 번호
chunk_data   - 실제 데이터 청크
```

##### TOAST 포인터 데이텀

저장 내용:
- TOAST 테이블 OID
- 값 OID (`chunk_id`)
- 논리적 크기
- 물리적 크기
- 압축 방법

총 크기: 실제 값 크기에 관계없이 18바이트 (varlena 헤더 포함)

##### UPDATE 최적화

- UPDATE 중 변경되지 않은 필드 값은 그대로 유지됨
- 외부 저장 값이 변경되지 않으면 TOAST 오버헤드 없음

#### 2.6 메모리 상의 TOAST 저장소 (Out-of-Line, In-Memory)

##### 간접 TOAST 포인터 (Indirect TOAST Pointers)

- 서버 프로세스 메모리의 비간접 varlena 값을 가리킴
- 단기간만 사용(디스크에 지속 불가)
- 1 GB 물리적 튜플 제한을 피하기 위해 논리적 디코딩 중 사용됨
- 생성자가 데이터 생존에 책임

##### 확장 TOAST 포인터 (Expanded TOAST Pointers)

- 복잡한 데이터 타입(예: 배열)에 대한 최적화된 표현
- 예: PostgreSQL 배열은 더 빠른 계산을 위해 인덱싱된 요소 위치로 분해됨
- 읽기-쓰기 vs 읽기 전용 변형:
  - 읽기-쓰기: 함수가 값을 제자리에서 수정 가능
  - 읽기 전용: 불필요한 복사를 피하기 위해 수정 전 복사 필요

#### 2.7 TOAST의 주요 이점

1. 더 작은 메인 테이블: 메인 테이블에는 키 값만 포함 · 대용량 값은 클라이언트로 전송할 때만 가져옴
2. 더 나은 버퍼 캐시 활용: 공유 버퍼 캐시에 더 많은 행이 들어감
3. 효율적인 정렬: 정렬 세트 축소 → 더 많은 정렬이 완전히 메모리에서 수행됨
4. 공간 효율성: 실제 예로 HTML 페이지 테이블이 원시 데이터 크기의 약 50%에 저장, 메인 테이블은 전체 데이터의 약 10%

#### 2.8 TOAST 예제

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

### 3. 프리 스페이스 맵 (Free Space Map)

프리 스페이스 맵(FSM)은 PostgreSQL이 힙(heap)과 인덱스 릴레이션(해시 인덱스 제외)에서 사용 가능한 공간을 추적하는 데 사용하는 데이터 구조. 새 행을 삽입할 때 충분한 빈 공간이 있는 페이지를 효율적으로 찾을 수 있게 함.

#### 3.1 저장 위치

- 파일 명명 규칙: 릴레이션의 filenode 번호에 `_fsm` 접미사를 붙인 별도의 릴레이션 포크에 저장됨
- 예: 릴레이션의 filenode가 `12345`면 FSM은 메인 릴레이션 파일과 같은 디렉토리의 `12345_fsm` 파일에 저장됨

#### 3.2 구조

FSM은 여러 레벨의 FSM 페이지 트리로 구성됨:

##### 리프 레벨 (Leaf Level)

- 힙 또는 인덱스 페이지의 빈 공간 정보를 저장
- 페이지당 1바이트를 사용해 사용 가능한 공간을 표시
- 실제 페이지 정보를 포함하는 최하위 레벨

##### 상위 레벨 (Upper Levels)

- 하위 레벨의 정보를 집계
- 계층적 구조를 형성

#### 3.3 내부 구성

- 각 FSM 페이지는 배열로 저장된 이진 트리를 포함
- 노드당 1바이트
- 리프 노드: 힙 페이지 또는 하위 레벨 FSM 페이지를 나타냄
- 비리프 노드: 자식 노드 값 중 더 높은 값을 저장
- 루트 노드: 모든 리프 노드의 최대값을 포함(사용 가능한 가장 큰 빈 공간을 나타냄)

#### 3.4 주요 특징

1. 효율적인 공간 추적: 페이지당 1바이트의 최소 저장소로 빈 공간 표현
2. 계층적 집계: 상위 레벨이 하위 레벨 정보를 효율적으로 요약
3. 빠른 공간 조회: 루트의 최대값으로 사용 가능한 공간이 있는 페이지를 빠르게 식별

#### 3.5 FSM 확인 예제

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

### 4. 가시성 맵 (Visibility Map)

가시성 맵(VM)은 PostgreSQL에서 어떤 힙 페이지가 모든 활성 트랜잭션에 보이는 튜플만 포함하는지, 그리고 어떤 페이지가 동결된(frozen) 튜플만 포함하는지를 추적하는 메커니즘.

#### 4.1 저장 위치

- 메인 릴레이션 데이터와 함께 별도의 릴레이션 포크로 저장됨
- 릴레이션의 filenode 번호에 `_vm` 접미사를 붙여 명명됨
- 예: filenode가 `12345`면 VM 파일은 같은 디렉토리의 `12345_vm`
- 참고: 인덱스는 가시성 맵 없음

#### 4.2 비트 구조

가시성 맵은 힙 페이지당 2비트를 저장함:

##### 비트 1: 모두 가시 플래그 (All-Visible Flag)

- 설정 시: 페이지가 모두 가시 상태(VACUUM이 필요한 튜플이 없음)
- 사용 사례: 힙에 접근하지 않고 인덱스 튜플만으로 쿼리에 응답하는 인덱스 전용 스캔(Index-Only Scan) 가능

##### 비트 2: 동결 플래그 (Frozen Flag)

- 설정 시: 페이지의 모든 튜플이 동결됨
- 이점: 래핑 방지 VACUUM이 해당 페이지를 다시 방문할 필요 없음

#### 4.3 보수적 설계

가시성 맵은 보수적으로 작동함:
- 비트 설정됨: 조건이 참임을 보장
- 비트 해제됨: 조건이 참일 수도 있고 아닐 수도 있음

#### 4.4 연산

- VACUUM: 비트 설정 → VM 비트를 설정하는 유일한 연산
- 데이터 수정: 비트 해제 → INSERT, UPDATE, DELETE가 관련 비트를 해제

#### 4.5 가시성 맵 확인 예제

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

### 5. 초기화 포크 (The Initialization Fork)

#### 5.1 개요

각 언로그 테이블(unlogged table)과 언로그 테이블의 인덱스에는 초기화 포크가 있음.

#### 5.2 정의

초기화 포크는 적절한 타입의 빈 테이블 또는 인덱스.

#### 5.3 목적과 동작

크래시로 인해 언로그 테이블을 빈 상태로 재설정해야 할 때:
1. 초기화 포크가 메인 포크 위에 복사됨
2. 다른 포크는 삭제됨(필요에 따라 자동으로 재생성)

#### 5.4 핵심 포인트

- 초기화 포크는 언로그 테이블의 템플릿/백업 역할
- 크래시 후 언로그 테이블을 깨끗한 상태로 자동 복구 가능
- 이 메커니즘은 수동 개입 없이 언로그 테이블을 안정적으로 재설정할 수 있도록 보장

#### 5.5 언로그 테이블 예제

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

### 6. 데이터베이스 페이지 레이아웃 (Database Page Layout)

PostgreSQL은 모든 테이블과 인덱스 데이터를 고정 크기 페이지(일반적으로 8 kB)로 저장함.

#### 6.1 전체 페이지 구조

PostgreSQL 페이지는 다섯 부분으로 구성됨:

- PageHeaderData: 24 바이트, 빈 공간 포인터를 포함한 일반 페이지 정보
- ItemIdData: 항목당 4 바이트, 아이템 식별자 배열(오프셋, 길이 쌍)
- Free Space: 가변, 할당되지 않은 공간(아이템 ID는 시작부터, 아이템은 끝부터 할당)
- Items: 가변, 실제 데이터(테이블의 행, 인덱스의 항목)
- Special Space: 가변, 인덱스 접근 방법별 데이터(일반 테이블에서는 비어 있음)

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

#### 6.2 PageHeaderData 레이아웃 (24 바이트)

- `pd_lsn` (PageXLogRecPtr, 8 바이트): LSN — 페이지에 대한 마지막 변경의 마지막 WAL 레코드 다음 바이트
- `pd_checksum` (uint16, 2 바이트): 페이지 체크섬(`-k` 플래그로 활성화된 경우)
- `pd_flags` (uint16, 2 바이트): 플래그 비트
- `pd_lower` (LocationIndex, 2 바이트): 빈 공간 시작 오프셋
- `pd_upper` (LocationIndex, 2 바이트): 빈 공간 끝 오프셋
- `pd_special` (LocationIndex, 2 바이트): 특수 공간 시작 오프셋
- `pd_pagesize_version` (uint16, 2 바이트): 페이지 크기 및 레이아웃 버전 번호
- `pd_prune_xid` (TransactionId, 4 바이트): 페이지에서 가장 오래된 정리되지 않은 XMAX, 또는 0

현재 버전: 4 (PostgreSQL 8.3 이후)

#### 6.3 ItemIdData 구조

- 각 아이템 식별자는 4 바이트 필요
- 포함 내용:
  - 아이템 시작에 대한 바이트 오프셋
  - 바이트 단위 길이
  - 해석에 영향을 주는 속성 비트
- 필요에 따라 빈 공간 시작부터 할당됨
- 해제될 때까지 이동하지 않아 인덱스로 장기 참조 가능

#### 6.4 테이블 행 레이아웃 (HeapTupleHeaderData)

고정 헤더 구조 뒤에 선택적 널 비트맵, 선택적 객체 ID, 사용자 데이터가 위치.

##### HeapTupleHeaderData 레이아웃 (대부분 플랫폼에서 23 바이트)

- `t_xmin` (TransactionId, 4 바이트): 삽입 XID 스탬프
- `t_xmax` (TransactionId, 4 바이트): 삭제 XID 스탬프
- `t_cid` (CommandId, 4 바이트): 삽입 및/또는 삭제 CID 스탬프(`t_xvac`과 오버레이)
- `t_xvac` (TransactionId, 4 바이트): 행 버전을 이동하는 VACUUM 연산의 XID
- `t_ctid` (ItemPointerData, 6 바이트): 이 또는 새로운 행 버전의 현재 TID
- `t_infomask2` (uint16, 2 바이트): 속성 수 + 플래그 비트
- `t_infomask` (uint16, 2 바이트): 다양한 플래그 비트
- `t_hoff` (uint8, 1 바이트): 사용자 데이터에 대한 오프셋(MAXALIGN 배수)

#### 6.5 선택적 구성 요소

##### 1. 널 비트맵 (Null Bitmap)

- `t_infomask`에 `HEAP_HASNULL` 비트가 설정되면 존재
- 데이터 컬럼당 1비트
- 1 = not-null, 0 = null
- 고정 헤더 직후에 시작
- 모든 컬럼이 not-null이면 생략

##### 2. 객체 ID 필드 (Object ID Field)

- `t_infomask`에 `HEAP_HASOID_OLD` 비트가 설정되면 존재
- `t_hoff` 경계 직전에 나타남
- 적절히 정렬됨

##### 3. 사용자 데이터 (User Data)

- `t_hoff`가 가리키는 오프셋에서 시작
- MAXALIGN 배수여야 함

#### 6.6 가변 길이 데이터

가변 길이 필드(`attlen = -1`인 경우)는 `struct varlena`라는 공통 헤더 구조를 공유함:
- 저장된 값의 전체 길이
- 다음을 나타내는 플래그 비트:
  - 인라인 vs TOAST 테이블 저장
  - 압축 상태(2절 참고)

#### 6.7 페이지 레이아웃 확인 예제

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

#### 6.8 주요 참조

- 헤더 세부 정보: `src/include/storage/bufpage.h`
- 튜플 세부 정보: `src/include/access/htup_details.h`
- 헬퍼 함수: `heap_getattr()`, `fastgetattr()`, `heap_getsysattr()`
- 속성 메타데이터: `pg_attribute` 테이블 (`attlen`, `attalign`)

---

### 7. 힙 전용 튜플 (Heap-Only Tuples, HOT)

힙 전용 튜플(HOT)은 UPDATE 연산의 오버헤드를 줄이는 PostgreSQL 최적화 기법. MVCC(다중 버전 동시성 제어)는 높은 동시성을 가능하게 하지만, 모든 행 업데이트마다 새 인덱스 항목을 생성해야 하므로 비용이 높을 수 있음. HOT은 이 문제를 완화함.

#### 7.1 HOT 적격 조건

HOT 최적화는 다음 두 조건이 모두 충족될 때 적용됨:

1. 업데이트가 인덱싱된 컬럼을 수정하지 않음(BRIN 요약 인덱스 제외)
2. 이전 행이 포함된 페이지에 업데이트된 행을 위한 충분한 빈 공간이 존재

#### 7.2 HOT이 제공하는 주요 최적화

##### 1. 새 인덱스 항목 불필요

- 업데이트된 행은 일반 인덱스에 새 인덱스 항목 불필요
- 요약 인덱스(BRIN 등)는 여전히 업데이트가 필요할 수 있음

##### 2. 효율적인 행 버전 정리

- 행이 여러 번 업데이트될 때 중간 버전은 일반 연산(`SELECT` 포함) 중에 완전히 제거 가능
- 주기적 VACUUM 연산을 기다릴 필요 없음
- 프로세스 세부 정보:
  - 인덱스는 계속 원래 행의 페이지 아이템 식별자를 가리킴
  - 이전 버전의 튜플 데이터가 제거됨
  - 아이템 식별자는 아직 가시적인 가장 오래된 버전을 가리키는 리다이렉트로 변환됨
  - 어떤 트랜잭션에도 더 이상 보이지 않는 중간 버전은 완전히 제거됨
  - 아이템 식별자는 재사용 가능해짐

#### 7.3 HOT 체인 (HOT Chain)

HOT 업데이트는 같은 페이지 내에서 튜플 버전들의 체인을 형성함:

```
인덱스 → [원래 아이템 ID] → 튜플 v1 → 튜플 v2 → 튜플 v3 (최신)
          (리다이렉트)
```

#### 7.4 최적화 권장 사항

HOT 가능성 증가 방법:
- 테이블의 `fillfactor` 파라미터를 낮춰 더 많은 빈 공간 예약
- → 업데이트된 행이 같은 페이지에 들어갈 확률 증가

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

참고: 새 행이 새 페이지로 옮겨지고 기존 페이지에 빈 공간이 축적되면 HOT 업데이트는 자연스럽게 발생함.

#### 7.5 HOT 활동 모니터링

시스템 뷰 `pg_stat_all_tables`로 모니터링:
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

#### 7.6 HOT 프루닝 (HOT Pruning)

HOT 프루닝은 페이지 내에서 더 이상 필요하지 않은 튜플 버전을 정리하는 과정.

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

#### 7.7 HOT의 이점 요약

1. 인덱스 유지 관리 감소: 인덱싱되지 않은 컬럼 업데이트 시 인덱스 업데이트 불필요
2. 디스크 I/O 감소: 같은 페이지 내 업데이트로 새 페이지 할당 불필요
3. 자동 정리: SELECT 쿼리 중에도 오래된 버전 정리 가능
4. VACUUM 부하 감소: 일부 정리 작업이 자동으로 수행됨

---

### 요약

PostgreSQL의 물리적 저장소 구조를 이해하면 데이터베이스의 성능 최적화와 문제 진단에 도움:

- 파일 레이아웃: 데이터베이스 파일 구조와 위치 이해
- TOAST: 대용량 데이터의 효율적 저장
- FSM: 빈 공간 추적으로 효율적인 행 삽입
- VM: 가시성 정보로 인덱스 전용 스캔 최적화
- 초기화 포크: 언로그 테이블의 크래시 복구
- 페이지 레이아웃: 행 데이터의 물리적 구조
- HOT: 업데이트 성능 최적화

이러한 내부 구조를 이해하면 `VACUUM`, `ANALYZE`, 인덱스 관리 등의 유지 관리 작업을 더 효과적으로 수행 가능.

---

## Chapter 53: 시스템 카탈로그 (System Catalogs)

### 목차
1. [개요](#개요)
2. [시스템 카탈로그 목록](#시스템-카탈로그-목록)
3. [주요 시스템 카탈로그](#주요-시스템-카탈로그)
   - [pg_class](#pg_class)
   - [pg_attribute](#pg_attribute)
   - [pg_type](#pg_type)
   - [pg_proc](#pg_proc)
   - [pg_namespace](#pg_namespace)
   - [pg_index](#pg_index)
   - [pg_constraint](#pg_constraint)
   - [pg_database](#pg_database)
4. [시스템 카탈로그 활용 예제](#시스템-카탈로그-활용-예제)

---

### 개요

시스템 카탈로그(System Catalogs)는 관계형 데이터베이스 관리 시스템이 스키마 메타데이터를 저장하는 장소. 테이블과 컬럼에 대한 정보, 그리고 내부 관리 정보가 여기에 저장됨.

#### 핵심 특징

PostgreSQL의 시스템 카탈로그는 일반 테이블. 기술적으로는 삭제·재생성·수정이 가능하지만, 정상적인 상황에서는 직접 수정하지 않는 것을 권장.

> 주의사항: 시스템 카탈로그에 컬럼을 추가하거나 값을 직접 삽입/수정하면 시스템에 심각한 문제가 발생할 수 있음.

#### 권장 사항

시스템 카탈로그를 직접 조작하는 대신 표준 SQL 명령어 사용 필요:

```sql
-- 직접 카탈로그 조작 (권장하지 않음)
-- INSERT INTO pg_database (...) VALUES (...);

-- 대신 표준 SQL 명령어 사용 (권장)
CREATE DATABASE mydb;
```

예를 들어, `CREATE DATABASE` 명령은 내부적으로 `pg_database` 카탈로그에 행을 삽입하고 디스크에 데이터베이스를 생성함.

---

### 시스템 카탈로그 목록

PostgreSQL은 50개 이상의 시스템 카탈로그를 제공. 주요 카탈로그 목록:

- `pg_aggregate`: 집계 함수(Aggregate functions)
- `pg_am`: 접근 방법(Access methods)
- `pg_amop`: 접근 방법 연산자(Access method operators)
- `pg_amproc`: 접근 방법 프로시저(Access method procedures)
- `pg_attrdef`: 속성 기본값(Attribute defaults)
- `pg_attribute`: 테이블 컬럼(Table columns)
- `pg_authid`: 인증 식별자(Authentication identifiers)
- `pg_auth_members`: 역할 멤버십(Role membership)
- `pg_cast`: 타입 캐스트(Type casts)
- `pg_class`: 테이블, 인덱스, 시퀀스 등(Tables, indexes, sequences)
- `pg_collation`: 콜레이션(Collations)
- `pg_constraint`: 제약 조건(Constraints)
- `pg_conversion`: 인코딩 변환(Encoding conversions)
- `pg_database`: 데이터베이스(Databases)
- `pg_depend`: 의존성 정보(Dependency information)
- `pg_description`: 객체 주석(Object comments)
- `pg_enum`: 열거형 타입(Enumeration types)
- `pg_event_trigger`: 이벤트 트리거(Event triggers)
- `pg_extension`: 확장(Extensions)
- `pg_foreign_data_wrapper`: 외부 데이터 래퍼(Foreign data wrappers)
- `pg_foreign_server`: 외부 서버(Foreign servers)
- `pg_foreign_table`: 외부 테이블(Foreign tables)
- `pg_index`: 인덱스(Indexes)
- `pg_inherits`: 테이블 상속(Table inheritance)
- `pg_language`: 언어(Languages)
- `pg_largeobject`: 대용량 객체(Large objects)
- `pg_namespace`: 스키마(Schemas/Namespaces)
- `pg_opclass`: 연산자 클래스(Operator classes)
- `pg_operator`: 연산자(Operators)
- `pg_opfamily`: 연산자 패밀리(Operator families)
- `pg_partitioned_table`: 파티션 테이블(Partitioned tables)
- `pg_policy`: 행 수준 보안 정책(Row-level security policies)
- `pg_proc`: 함수 및 프로시저(Functions and procedures)
- `pg_publication`: 논리적 복제 발행(Logical replication publications)
- `pg_range`: 범위 타입(Range types)
- `pg_rewrite`: 규칙(Rules)
- `pg_sequence`: 시퀀스(Sequences)
- `pg_statistic`: 통계(Statistics)
- `pg_subscription`: 논리적 복제 구독(Logical replication subscriptions)
- `pg_tablespace`: 테이블스페이스(Tablespaces)
- `pg_trigger`: 트리거(Triggers)
- `pg_ts_config`: 텍스트 검색 구성(Text search configurations)
- `pg_ts_dict`: 텍스트 검색 사전(Text search dictionaries)
- `pg_type`: 데이터 타입(Data types)
- `pg_user_mapping`: 사용자 매핑(User mappings)

---

### 주요 시스템 카탈로그

#### pg_class

`pg_class` 카탈로그는 테이블 및 테이블과 유사한 구조를 가진 객체들에 대한 정보를 저장함.

##### 저장되는 객체 유형
- 테이블 (Tables)
- 인덱스 (Indexes)
- 시퀀스 (Sequences)
- 뷰 (Views)
- 구체화된 뷰 (Materialized views)
- 복합 타입 (Composite types)
- TOAST 테이블

##### 컬럼 정의

- `oid` (`oid`): 행 식별자(Row identifier)
- `relname` (`name`): 테이블, 인덱스, 뷰 등의 이름
- `relnamespace` (`oid`): 이 릴레이션을 포함하는 네임스페이스의 OID(`pg_namespace.oid` 참조)
- `reltype` (`oid`): 테이블의 행 타입에 해당하는 데이터 타입의 OID, 인덱스·시퀀스·TOAST 테이블의 경우 0(`pg_type.oid` 참조)
- `reloftype` (`oid`): 타입이 지정된 테이블의 경우 기본 복합 타입의 OID, 그 외의 경우 0
- `relowner` (`oid`): 릴레이션 소유자(`pg_authid.oid` 참조)
- `relam` (`oid`): 테이블이나 인덱스에 접근하는 데 사용되는 접근 방법(`pg_am.oid` 참조)
- `relfilenode` (`oid`): 디스크 파일의 이름, 0은 "매핑된" 릴레이션을 의미
- `reltablespace` (`oid`): 릴레이션이 저장된 테이블스페이스, 0은 기본 테이블스페이스를 의미
- `relpages` (`int4`): 페이지(BLCKSZ) 단위의 디스크 표현 크기, VACUUM·ANALYZE·CREATE INDEX에 의해 갱신되는 추정값
- `reltuples` (`float4`): 살아있는 행의 수, 플래너가 사용하는 추정값(-1은 VACUUM/ANALYZE가 실행되지 않았음을 의미)
- `relallvisible` (`int4`): 가시성 맵에서 all-visible로 표시된 페이지 수
- `relallfrozen` (`int4`): 가시성 맵에서 all-frozen으로 표시된 페이지 수
- `reltoastrelid` (`oid`): 연결된 TOAST 테이블의 OID, 없으면 0
- `relhasindex` (`bool`): 테이블에 인덱스가 있으면 true
- `relisshared` (`bool`): 클러스터 내 모든 데이터베이스에서 공유되는 테이블이면 true
- `relpersistence` (`char`): `p` = 영구(permanent), `u` = 비로그(unlogged), `t` = 임시(temporary)
- `relkind` (`char`): 릴레이션 종류(아래 목록 참고)
- `relnatts` (`int2`): 사용자 컬럼 수(시스템 컬럼 제외)
- `relchecks` (`int2`): CHECK 제약 조건 수
- `relhasrules` (`bool`): 테이블에 규칙이 있으면 true
- `relhastriggers` (`bool`): 테이블에 트리거가 있으면 true
- `relhassubclass` (`bool`): 테이블/인덱스에 상속 자식이나 파티션이 있으면 true
- `relrowsecurity` (`bool`): 행 수준 보안이 활성화되면 true
- `relforcerowsecurity` (`bool`): RLS가 테이블 소유자에게도 적용되면 true
- `relispopulated` (`bool`): 릴레이션이 채워져 있으면 true
- `relreplident` (`char`): 복제 아이덴티티 — `d` = 기본(primary key), `n` = 없음, `f` = 모든 컬럼, `i` = 인덱스
- `relispartition` (`bool`): 테이블이나 인덱스가 파티션이면 true
- `relrewrite` (`oid`): DDL 재작성 중 원본 릴레이션의 OID, 그 외에는 0
- `relfrozenxid` (`xid`): 동결된 행의 트랜잭션 ID 임계값
- `relminmxid` (`xid`): 동결된 멀티트랜잭션의 멀티트랜잭션 ID 임계값
- `relacl` (`aclitem[]`): 접근 권한
- `reloptions` (`text[]`): 접근 방법별 옵션("keyword=value" 형식의 문자열)
- `relpartbound` (`pg_node_tree`): 파티션인 경우 파티션 경계의 내부 표현

##### relkind 값

- `r`: 일반 테이블(ordinary table)
- `i`: 인덱스(index)
- `S`: 시퀀스(sequence)
- `t`: TOAST 테이블
- `v`: 뷰(view)
- `m`: 구체화된 뷰(materialized view)
- `c`: 복합 타입(composite type)
- `f`: 외부 테이블(foreign table)
- `p`: 파티션 테이블(partitioned table)
- `I`: 파티션 인덱스(partitioned index)

---

#### pg_attribute

`pg_attribute` 카탈로그는 테이블 컬럼에 대한 정보를 저장함. 데이터베이스의 모든 테이블의 모든 컬럼에 대해 정확히 하나의 `pg_attribute` 행이 존재.

> 참고: "속성(Attribute)"이라는 용어는 "컬럼(Column)"과 동일한 의미로, 역사적인 이유로 사용됨.

##### 컬럼 정의

- `attrelid` (`oid`): 이 컬럼이 속한 테이블(`pg_class.oid` 참조)
- `attname` (`name`): 컬럼 이름
- `atttypid` (`oid`): 데이터 타입 OID(`pg_type.oid` 참조), 삭제된 컬럼의 경우 0
- `attlen` (`int2`): 이 컬럼 타입의 `pg_type.typlen` 복사본
- `attnum` (`int2`): 컬럼 번호(일반 컬럼: 1+, `ctid` 같은 시스템 컬럼: 음수)
- `atttypmod` (`int4`): 타입별 데이터(예: varchar의 최대 길이), 불필요한 경우 -1
- `attndims` (`int2`): 배열 차원 수, 배열이 아니면 0
- `attbyval` (`bool`): `pg_type.typbyval` 복사본
- `attalign` (`char`): `pg_type.typalign` 복사본
- `attstorage` (`char`): `pg_type.typstorage` 복사본, TOAST 가능 타입의 경우 변경 가능
- `attcompression` (`char`): 압축 방법 — `'\0'`(기본값), `'p'`(pglz), `'l'`(LZ4)
- `attnotnull` (`bool`): NOT NULL 제약 조건 보유 여부
- `atthasdef` (`bool`): 기본값이나 생성 표현식 보유 여부(`pg_attrdef` 참조)
- `atthasmissing` (`bool`): 행에서 컬럼이 완전히 누락된 경우 사용되는 누락 값 보유 여부
- `attidentity` (`char`): 아이덴티티 컬럼 — `''`(없음), `'a'`(always), `'d'`(by default)
- `attgenerated` (`char`): 생성된 컬럼 — `''`(없음), `'s'`(stored), `'v'`(virtual)
- `attisdropped` (`bool`): 삭제된 컬럼, 물리적으로 존재하지만 SQL로 접근 불가
- `attislocal` (`bool`): 릴레이션에서 로컬로 정의됨(상속도 가능)
- `attinhcount` (`int2`): 직접 조상의 수, 0이 아니면 삭제/이름 변경 방지
- `attcollation` (`oid`): 콜레이션 OID(`pg_collation.oid` 참조), 콜레이션 불가 타입이면 0
- `attstattarget` (`int2`): ANALYZE를 위한 통계 상세 수준(0=없음, null=기본값, 양수=타입별)
- `attacl` (`aclitem[]`): 컬럼 수준 접근 권한
- `attoptions` (`text[]`): 속성 옵션("keyword=value" 형식의 문자열)
- `attfdwoptions` (`text[]`): 외부 데이터 래퍼 옵션
- `attmissingval` (`anyarray`): 누락 값이 있는 경우 하나의 요소를 가진 배열

---

#### pg_type

`pg_type` 카탈로그는 PostgreSQL의 모든 데이터 타입에 대한 정보를 저장함. 기본 타입, 열거형 타입, 도메인, 복합 타입(각 테이블에 대해 자동 생성)이 포함됨.

##### 컬럼 정의

- `oid` (`oid`): 행 식별자
- `typname` (`name`): 데이터 타입 이름
- `typnamespace` (`oid`): 이 타입을 포함하는 네임스페이스의 OID
- `typowner` (`oid`): 타입 소유자
- `typlen` (`int2`): 고정 크기 타입의 바이트 크기, 가변 길이의 경우 음수(-1: varlena, -2: C 문자열)
- `typbyval` (`bool`): 값이 값으로 전달되는지 참조로 전달되는지 여부
- `typtype` (`char`): 타입 분류자 — `b`(기본), `c`(복합), `d`(도메인), `e`(열거형), `p`(의사), `r`(범위), `m`(다중범위)
- `typcategory` (`char`): 암시적 캐스트 결정을 위한 타입 분류
- `typispreferred` (`bool`): 카테고리 내에서 선호되는 캐스트 대상
- `typisdefined` (`bool`): 타입이 정의되어 있으면 true
- `typdelim` (`char`): 배열 요소 구분 문자
- `typrelid` (`oid`): 복합 타입의 경우 `pg_class` 참조
- `typsubscript` (`regproc`): 첨자 처리기 함수 OID
- `typelem` (`oid`): 첨자를 위한 요소 타입
- `typarray` (`oid`): 이것을 요소로 하는 "진정한" 배열 타입
- `typinput` (`regproc`): 입력 변환 함수(텍스트)
- `typoutput` (`regproc`): 출력 변환 함수(텍스트)
- `typreceive` (`regproc`): 입력 변환 함수(바이너리)
- `typsend` (`regproc`): 출력 변환 함수(바이너리)
- `typmodin` (`regproc`): 타입 수정자 입력 함수
- `typmodout` (`regproc`): 타입 수정자 출력 함수
- `typanalyze` (`regproc`): 사용자 정의 ANALYZE 함수
- `typalign` (`char`): 저장 정렬 — `c`(char), `s`(short), `i`(int), `d`(double)
- `typstorage` (`char`): TOAST 전략 — `p`(plain), `e`(external), `m`(main), `x`(extended)
- `typnotnull` (`bool`): NOT NULL 제약 조건(도메인 전용)
- `typbasetype` (`oid`): 도메인의 기본 타입
- `typtypmod` (`int4`): 도메인 기본 타입의 타입 수정자
- `typndims` (`int4`): 배열에 대한 도메인의 배열 차원
- `typcollation` (`oid`): 콜레이션 OID
- `typdefaultbin` (`pg_node_tree`): 기본 표현식의 바이너리 표현
- `typdefault` (`text`): 사람이 읽을 수 있는 기본값
- `typacl` (`aclitem[]`): 접근 권한

##### 타입 카테고리 (typcategory)

- `A`: 배열 타입(Array types)
- `B`: 불리언 타입(Boolean types)
- `C`: 복합 타입(Composite types)
- `D`: 날짜/시간 타입(Date/time types)
- `E`: 열거형 타입(Enum types)
- `G`: 기하 타입(Geometric types)
- `I`: 네트워크 주소 타입(Network address types)
- `N`: 숫자 타입(Numeric types)
- `P`: 의사 타입(Pseudo-types)
- `R`: 범위 타입(Range types)
- `S`: 문자열 타입(String types)
- `T`: 시간 간격 타입(Timespan types)
- `U`: 사용자 정의 타입(User-defined types)
- `V`: 비트 문자열 타입(Bit-string types)
- `X`: `unknown` 타입
- `Z`: 내부 사용 타입(Internal-use types)

---

#### pg_proc

`pg_proc` 시스템 카탈로그는 함수, 프로시저, 집계 함수, 윈도우 함수(통칭하여 루틴)에 대한 정보를 저장함.

##### 컬럼 정의

- `oid` (`oid`): 행 식별자
- `proname` (`name`): 함수 이름
- `pronamespace` (`oid`): 이 함수를 포함하는 네임스페이스의 OID
- `proowner` (`oid`): 함수 소유자
- `prolang` (`oid`): 구현 언어 또는 호출 인터페이스
- `procost` (`float4`): 예상 실행 비용(cpu_operator_cost 단위)
- `prorows` (`float4`): 예상 결과 행 수
- `provariadic` (`oid`): 가변 인자 배열 매개변수 요소의 데이터 타입(없으면 0)
- `prosupport` (`regproc`): 플래너 지원 함수 OID(없으면 0)
- `prokind` (`char`): 함수 유형 — `f`(일반), `p`(프로시저), `a`(집계), `w`(윈도우)
- `prosecdef` (`bool`): 보안 정의자 함수 여부(SECURITY DEFINER)
- `proleakproof` (`bool`): 인자 정보가 반환값 외의 경로로 노출되지 않는 함수 여부(행 수준 보안과 함께 사용)
- `proisstrict` (`bool`): 인자가 NULL이면 NULL 반환 여부
- `proretset` (`bool`): 함수가 집합(여러 값)을 반환하는지 여부
- `provolatile` (`char`): 휘발성 — `i`(immutable/불변), `s`(stable/안정), `v`(volatile/휘발)
- `proparallel` (`char`): 병렬 안전성 — `s`(safe/안전), `r`(restricted/제한), `u`(unsafe/안전하지 않음)
- `pronargs` (`int2`): 입력 인자 수
- `pronargdefaults` (`int2`): 기본값이 있는 인자 수
- `prorettype` (`oid`): 반환 값의 데이터 타입
- `proargtypes` (`oidvector`): 함수 인자의 데이터 타입(호출 시그니처)
- `proallargtypes` (`oid[]`): 모든 인자의 데이터 타입(OUT/INOUT 포함)
- `proargmodes` (`char[]`): 인자 모드 — `i`(IN), `o`(OUT), `b`(INOUT), `v`(VARIADIC), `t`(TABLE)
- `proargnames` (`text[]`): 함수 인자의 이름
- `proargdefaults` (`pg_node_tree`): 마지막 N개 입력 인자의 기본값 표현식
- `protrftypes` (`oid[]`): TRANSFORM 절에 대한 인자/결과 타입
- `prosrc` (`text`): 함수 호출 세부 정보(언어에 따라 다름)
- `probin` (`text`): 추가 호출 정보(동적 로드된 C 함수용)
- `prosqlbody` (`pg_node_tree`): 사전 파싱된 SQL 함수 본문
- `proconfig` (`text[]`): 로컬 런타임 구성 설정
- `proacl` (`aclitem[]`): 접근 권한

##### prokind 값

- `f`: 일반 함수(normal function)
- `p`: 프로시저(procedure)
- `a`: 집계 함수(aggregate function)
- `w`: 윈도우 함수(window function)

---

#### pg_namespace

`pg_namespace` 카탈로그는 네임스페이스(Namespaces)를 저장함. 네임스페이스는 SQL 스키마의 기반이 되는 구조.

##### 핵심 개념

각 네임스페이스는 이름 충돌 없이 릴레이션, 타입, 기타 객체를 별도 컬렉션으로 보유 가능.

##### 컬럼 정의

- `oid` (`oid`): 행 식별자
- `nspname` (`name`): 네임스페이스 이름
- `nspowner` (`oid`): 네임스페이스 소유자(`pg_authid.oid` 참조)
- `nspacl` (`aclitem[]`): 접근 권한

---

#### pg_index

`pg_index` 카탈로그는 PostgreSQL의 인덱스에 대한 메타데이터를 포함함. 나머지 인덱스 정보는 `pg_class` 카탈로그에 저장됨.

##### 컬럼 정의

- `indexrelid` (`oid`): 이 인덱스의 `pg_class` 항목 OID
- `indrelid` (`oid`): 이 인덱스가 속한 테이블의 `pg_class` 항목 OID
- `indnatts` (`int2`): 인덱스의 총 컬럼 수(키 + 포함 컬럼)
- `indnkeyatts` (`int2`): 키 컬럼만의 수(포함 컬럼 제외)
- `indisunique` (`bool`): 유니크 인덱스이면 true
- `indnullsnotdistinct` (`bool`): 유니크 인덱스에서 true면 NULL은 동일하게 취급, false(기본)면 NULL은 구별됨
- `indisprimary` (`bool`): 인덱스가 기본 키를 나타내면 true
- `indisexclusion` (`bool`): 인덱스가 배제 제약 조건을 지원하면 true
- `indimmediate` (`bool`): 삽입 시 유니크 검사가 즉시 수행되면 true
- `indisclustered` (`bool`): 테이블이 마지막으로 이 인덱스로 클러스터링되었으면 true
- `indisvalid` (`bool`): 인덱스가 쿼리에 유효하면 true, false면 불완전
- `indcheckxmin` (`bool`): 쿼리가 `xmin`이 `TransactionXmin` 아래가 될 때까지 기다려야 하면 true
- `indisready` (`bool`): 인덱스가 삽입 준비가 되면 true
- `indislive` (`bool`): 인덱스가 삭제 중이면 false
- `indisreplident` (`bool`): 인덱스가 복제 아이덴티티로 선택되면 true
- `indkey` (`int2vector`): 인덱싱된 테이블 컬럼 번호의 배열, 0은 표현식을 나타냄
- `indcollation` (`oidvector`): 각 키 컬럼의 콜레이션 OID
- `indclass` (`oidvector`): 각 키 컬럼의 연산자 클래스 OID
- `indoption` (`int2vector`): 컬럼별 플래그 비트(접근 방법에 의해 의미가 정의됨)
- `indexprs` (`pg_node_tree`): 단순 컬럼 참조가 아닌 속성에 대한 표현식 트리
- `indpred` (`pg_node_tree`): 부분 인덱스 술어에 대한 표현식 트리(부분 인덱스가 아니면 NULL)

---

#### pg_constraint

`pg_constraint` 카탈로그는 PostgreSQL 테이블과 도메인에 대한 제약 조건 정의를 저장함.

##### 저장되는 제약 조건 유형
- CHECK 제약 조건
- NOT NULL 제약 조건
- PRIMARY KEY (기본 키) 제약 조건
- UNIQUE (유니크) 제약 조건
- FOREIGN KEY (외래 키) 제약 조건
- EXCLUSION (배제) 제약 조건
- 사용자 정의 제약 조건 트리거 (`CREATE CONSTRAINT TRIGGER`로 생성)
- 도메인에 대한 CHECK 제약 조건

##### 컬럼 정의

- `oid` (`oid`): 행 식별자
- `conname` (`name`): 제약 조건 이름(반드시 유니크할 필요 없음)
- `connamespace` (`oid`): 이 제약 조건을 포함하는 네임스페이스의 OID
- `contype` (`char`): 제약 조건 유형(아래 목록 참고)
- `condeferrable` (`bool`): 제약 조건이 지연 가능한지 여부
- `condeferred` (`bool`): 제약 조건이 기본적으로 지연되는지 여부
- `conenforced` (`bool`): 제약 조건이 강제되는지 여부
- `convalidated` (`bool`): 제약 조건이 검증되었는지 여부
- `conrelid` (`oid`): 테이블 OID(테이블 제약 조건이 아니면 0)
- `contypid` (`oid`): 도메인 OID(도메인 제약 조건이 아니면 0)
- `conindid` (`oid`): 유니크/기본 키/외래 키/배제 제약 조건을 지원하는 인덱스 OID
- `conparentid` (`oid`): 부모 파티션 테이블 제약 조건 OID(파티션 제약 조건이 아니면 0)
- `confrelid` (`oid`): 외래 키의 참조 테이블 OID
- `confupdtype` (`char`): 외래 키 갱신 동작
- `confdeltype` (`char`): 외래 키 삭제 동작
- `confmatchtype` (`char`): 외래 키 매치 유형 — `f`(full), `p`(partial), `s`(simple)
- `conislocal` (`bool`): 제약 조건이 로컬로 정의됨(동시에 상속될 수 있음)
- `coninhcount` (`int2`): 직접 상속 조상의 수
- `connoinherit` (`bool`): 상속 불가 제약 조건
- `conperiod` (`bool`): `WITHOUT OVERLAPS` 또는 `PERIOD`로 정의된 제약 조건
- `conkey` (`int2[]`): 제약된 컬럼 속성 번호 목록
- `confkey` (`int2[]`): 참조된 컬럼 속성 번호 목록(외래 키)
- `conpfeqop` (`oid[]`): PK = FK 비교를 위한 동등 연산자
- `conppeqop` (`oid[]`): PK = PK 비교를 위한 동등 연산자
- `conffeqop` (`oid[]`): FK = FK 비교를 위한 동등 연산자
- `confdelsetcols` (`int2[]`): `SET NULL`/`SET DEFAULT` 삭제 동작에 의해 갱신되는 컬럼
- `conexclop` (`oid[]`): 배제 제약 조건을 위한 컬럼별 배제 연산자
- `conbin` (`pg_node_tree`): CHECK 제약 조건 표현식의 내부 표현(`pg_get_constraintdef()`로 추출)

##### contype 값

- `c`: CHECK 제약 조건
- `f`: FOREIGN KEY (외래 키) 제약 조건
- `n`: NOT NULL 제약 조건
- `p`: PRIMARY KEY (기본 키) 제약 조건
- `u`: UNIQUE (유니크) 제약 조건
- `t`: 제약 조건 트리거(Constraint trigger)
- `x`: EXCLUSION (배제) 제약 조건

##### 외래 키 동작 코드 (confupdtype, confdeltype)

- `a`: NO ACTION (아무 동작 없음)
- `r`: RESTRICT (제한)
- `c`: CASCADE (연쇄)
- `n`: SET NULL (NULL로 설정)
- `d`: SET DEFAULT (기본값으로 설정)

---

#### pg_database

`pg_database` 카탈로그는 PostgreSQL 클러스터에서 사용 가능한 데이터베이스에 대한 정보를 저장함. 이것은 공유 카탈로그(Shared Catalog) — 데이터베이스마다 하나씩 존재하는 것이 아니라 클러스터당 하나의 복사본만 존재.

##### 컬럼 정의

- `oid` (`oid`): 행 식별자
- `datname` (`name`): 데이터베이스 이름
- `datdba` (`oid`): 데이터베이스 소유자(`pg_authid.oid` 참조)
- `encoding` (`int4`): 문자 인코딩(`pg_encoding_to_char()`로 변환)
- `datlocprovider` (`char`): 로케일 제공자 — `b` = builtin, `c` = libc, `i` = icu
- `datistemplate` (`bool`): true이면 `CREATEDB` 권한을 가진 사용자가 복제 가능
- `datallowconn` (`bool`): false이면 아무도 연결 불가(`template0` 보호용)
- `dathasloginevt` (`bool`): 로그인 이벤트 트리거 존재 여부(내부 사용 전용)
- `datconnlimit` (`int4`): 최대 동시 연결 수(-1 = 제한 없음, -2 = 유효하지 않음)
- `datfrozenxid` (`xid`): VACUUM 추적을 위한 최소 동결 트랜잭션 ID
- `datminmxid` (`xid`): VACUUM 추적을 위한 최소 멀티트랜잭션 ID
- `dattablespace` (`oid`): 기본 테이블스페이스(`pg_tablespace.oid` 참조)
- `datcollate` (`text`): LC_COLLATE 설정
- `datctype` (`text`): LC_CTYPE 설정
- `datlocale` (`text`): 콜레이션 제공자 로케일 이름(libc 제공자의 경우 NULL)
- `daticurules` (`text`): ICU 콜레이션 규칙
- `datcollversion` (`text`): 제공자별 콜레이션 버전
- `datacl` (`aclitem[]`): 접근 권한

---

### 시스템 카탈로그 활용 예제

#### 1. 테이블 정보 조회

```sql
-- 특정 스키마의 모든 테이블 조회
SELECT
    c.relname AS table_name,
    n.nspname AS schema_name,
    c.reltuples AS estimated_rows,
    c.relpages AS pages
FROM pg_class c
JOIN pg_namespace n ON c.relnamespace = n.oid
WHERE c.relkind = 'r'  -- 일반 테이블만
  AND n.nspname = 'public'
ORDER BY c.relname;
```

#### 2. 컬럼 정보 조회

```sql
-- 특정 테이블의 모든 컬럼 정보 조회
SELECT
    a.attname AS column_name,
    t.typname AS data_type,
    a.attlen AS length,
    a.attnotnull AS not_null,
    a.attnum AS column_position
FROM pg_attribute a
JOIN pg_class c ON a.attrelid = c.oid
JOIN pg_type t ON a.atttypid = t.oid
WHERE c.relname = 'your_table_name'
  AND a.attnum > 0  -- 시스템 컬럼 제외
  AND NOT a.attisdropped  -- 삭제된 컬럼 제외
ORDER BY a.attnum;
```

#### 3. 인덱스 정보 조회

```sql
-- 테이블의 인덱스 정보 조회
SELECT
    i.relname AS index_name,
    t.relname AS table_name,
    ix.indisunique AS is_unique,
    ix.indisprimary AS is_primary,
    array_agg(a.attname ORDER BY k.ord) AS indexed_columns
FROM pg_index ix
JOIN pg_class i ON i.oid = ix.indexrelid
JOIN pg_class t ON t.oid = ix.indrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
JOIN LATERAL unnest(ix.indkey) WITH ORDINALITY AS k(attnum, ord) ON true
JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
WHERE n.nspname = 'public'
  AND t.relname = 'your_table_name'
GROUP BY i.relname, t.relname, ix.indisunique, ix.indisprimary;
```

#### 4. 제약 조건 정보 조회

```sql
-- 테이블의 모든 제약 조건 조회
SELECT
    con.conname AS constraint_name,
    CASE con.contype
        WHEN 'c' THEN 'CHECK'
        WHEN 'f' THEN 'FOREIGN KEY'
        WHEN 'n' THEN 'NOT NULL'
        WHEN 'p' THEN 'PRIMARY KEY'
        WHEN 'u' THEN 'UNIQUE'
        WHEN 'x' THEN 'EXCLUSION'
    END AS constraint_type,
    pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con
JOIN pg_class c ON c.oid = con.conrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname = 'your_table_name';
```

#### 5. 함수 정보 조회

```sql
-- 특정 스키마의 사용자 정의 함수 조회
SELECT
    p.proname AS function_name,
    n.nspname AS schema_name,
    l.lanname AS language,
    CASE p.prokind
        WHEN 'f' THEN 'function'
        WHEN 'p' THEN 'procedure'
        WHEN 'a' THEN 'aggregate'
        WHEN 'w' THEN 'window'
    END AS kind,
    pg_get_function_arguments(p.oid) AS arguments,
    t.typname AS return_type
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
JOIN pg_language l ON p.prolang = l.oid
JOIN pg_type t ON p.prorettype = t.oid
WHERE n.nspname = 'public'
ORDER BY p.proname;
```

#### 6. 데이터 타입 정보 조회

```sql
-- 사용자 정의 타입 조회
SELECT
    t.typname AS type_name,
    n.nspname AS schema_name,
    CASE t.typtype
        WHEN 'b' THEN 'base'
        WHEN 'c' THEN 'composite'
        WHEN 'd' THEN 'domain'
        WHEN 'e' THEN 'enum'
        WHEN 'r' THEN 'range'
    END AS type_kind,
    t.typlen AS length
FROM pg_type t
JOIN pg_namespace n ON t.typnamespace = n.oid
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND t.typtype IN ('b', 'c', 'd', 'e', 'r')
ORDER BY t.typname;
```

#### 7. 테이블과 인덱스 크기 조회

```sql
-- 테이블과 관련 인덱스의 크기 조회
SELECT
    c.relname AS name,
    CASE c.relkind
        WHEN 'r' THEN 'table'
        WHEN 'i' THEN 'index'
    END AS type,
    pg_size_pretty(pg_relation_size(c.oid)) AS size,
    c.relpages AS pages,
    c.reltuples AS estimated_rows
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind IN ('r', 'i')
ORDER BY pg_relation_size(c.oid) DESC;
```

#### 8. 외래 키 관계 조회

```sql
-- 모든 외래 키 관계 조회
SELECT
    tc.relname AS child_table,
    STRING_AGG(ac.attname, ', ' ORDER BY x.n) AS child_columns,
    tp.relname AS parent_table,
    STRING_AGG(ap.attname, ', ' ORDER BY x.n) AS parent_columns,
    c.conname AS constraint_name
FROM pg_constraint c
JOIN pg_class tc ON c.conrelid = tc.oid
JOIN pg_class tp ON c.confrelid = tp.oid
JOIN pg_namespace n ON tc.relnamespace = n.oid
CROSS JOIN LATERAL unnest(c.conkey, c.confkey) WITH ORDINALITY AS x(child_attnum, parent_attnum, n)
JOIN pg_attribute ac ON ac.attrelid = tc.oid AND ac.attnum = x.child_attnum
JOIN pg_attribute ap ON ap.attrelid = tp.oid AND ap.attnum = x.parent_attnum
WHERE c.contype = 'f'
  AND n.nspname = 'public'
GROUP BY tc.relname, tp.relname, c.conname;
```

#### 9. 스키마별 객체 수 조회

```sql
-- 각 스키마의 객체 수 조회
SELECT
    n.nspname AS schema_name,
    COUNT(CASE WHEN c.relkind = 'r' THEN 1 END) AS tables,
    COUNT(CASE WHEN c.relkind = 'i' THEN 1 END) AS indexes,
    COUNT(CASE WHEN c.relkind = 'v' THEN 1 END) AS views,
    COUNT(CASE WHEN c.relkind = 'S' THEN 1 END) AS sequences
FROM pg_namespace n
LEFT JOIN pg_class c ON c.relnamespace = n.oid
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
GROUP BY n.nspname
ORDER BY n.nspname;
```

#### 10. 데이터베이스 정보 조회

```sql
-- 모든 데이터베이스 정보 조회
SELECT
    datname AS database_name,
    pg_encoding_to_char(encoding) AS encoding,
    datcollate AS collation,
    datconnlimit AS connection_limit,
    pg_size_pretty(pg_database_size(datname)) AS size
FROM pg_database
WHERE datistemplate = false
ORDER BY pg_database_size(datname) DESC;
```

---

### 주의사항

1. 직접 수정 금지: 시스템 카탈로그를 직접 INSERT, UPDATE, DELETE하지 말 것. 대신 표준 DDL 명령어 사용
2. 지연된 플래그 갱신: `relhasindex`, `relhasrules`, `relhastriggers`, `relhassubclass` 같은 불리언 플래그는 지연되어 갱신됨. 조건이 충족되면 true로 설정되지만, 조건이 해제되어도 즉시 false로 재설정되지 않을 수 있음
3. 추정값 이해: `relpages`, `reltuples`, `relallvisible`, `relallfrozen`은 VACUUM, ANALYZE, 특정 DDL 명령에 의해 갱신되는 추정값
4. 공유 카탈로그: `pg_database`, `pg_authid`, `pg_tablespace` 같은 일부 카탈로그는 클러스터 전체에서 공유됨
5. information_schema 활용: 시스템 카탈로그에 직접 접근하는 대신, SQL 표준인 `information_schema` 사용이 이식성 면에서 유리

---

### 참고 자료

- [PostgreSQL 공식 문서 - System Catalogs](https://www.postgresql.org/docs/current/catalogs.html)
- [PostgreSQL 공식 문서 - pg_class](https://www.postgresql.org/docs/current/catalog-pg-class.html)
- [PostgreSQL 공식 문서 - pg_attribute](https://www.postgresql.org/docs/current/catalog-pg-attribute.html)
- [PostgreSQL 공식 문서 - pg_type](https://www.postgresql.org/docs/current/catalog-pg-type.html)
- [PostgreSQL 공식 문서 - pg_proc](https://www.postgresql.org/docs/current/catalog-pg-proc.html)
- [PostgreSQL 공식 문서 - pg_namespace](https://www.postgresql.org/docs/current/catalog-pg-namespace.html)

---

## 제54장. 시스템 뷰 (System Views)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/views.html

PostgreSQL은 시스템 카탈로그와 내부 서버 상태에 편리하게 접근하도록 내장 시스템 뷰(System Views)를 제공함. 이 장에서는 PostgreSQL이 제공하는 다양한 시스템 뷰의 개요와 주요 뷰들의 상세 정보를 설명함.

---

### 목차

- [54.1 개요](#541-개요)
- [54.2 시스템 뷰 목록](#542-시스템-뷰-목록)
- [54.3 주요 시스템 뷰](#543-주요-시스템-뷰)
  - [54.3.1 pg_stat_activity](#5431-pg_stat_activity)
  - [54.3.2 pg_locks](#5432-pg_locks)
  - [54.3.3 pg_settings](#5433-pg_settings)
  - [54.3.4 pg_tables](#5434-pg_tables)
  - [54.3.5 pg_indexes](#5435-pg_indexes)
  - [54.3.6 pg_views](#5436-pg_views)
  - [54.3.7 pg_roles](#5437-pg_roles)
  - [54.3.8 pg_stats](#5438-pg_stats)
  - [54.3.9 pg_cursors](#5439-pg_cursors)
  - [54.3.10 pg_prepared_statements](#54310-pg_prepared_statements)
  - [54.3.11 pg_replication_slots](#54311-pg_replication_slots)
  - [54.3.12 pg_sequences](#54312-pg_sequences)
  - [54.3.13 pg_matviews](#54313-pg_matviews)
  - [54.3.14 pg_policies](#54314-pg_policies)
  - [54.3.15 pg_rules](#54315-pg_rules)
  - [54.3.16 pg_available_extensions](#54316-pg_available_extensions)
- [54.4 통계 뷰](#544-통계-뷰)
- [54.5 유용한 쿼리 예제](#545-유용한-쿼리-예제)

---

### 54.1 개요

PostgreSQL 시스템 뷰는 두 가지 범주로 나뉨.

#### 1. 카탈로그 접근 뷰 (Catalog Access Views)

시스템 카탈로그를 자주 사용하는 쿼리에 편리하게 접근하도록 함.

- `pg_tables` - 테이블 정보
- `pg_indexes` - 인덱스 정보
- `pg_views` - 뷰 정의
- `pg_roles` - 데이터베이스 역할
- `pg_sequences` - 시퀀스 정보
- `pg_matviews` - 구체화된 뷰 정보

#### 2. 서버 상태 뷰 (Server State Views)

내부 서버 상태 정보 접근을 제공함.

- `pg_stat_activity` - 현재 활동 중인 세션
- `pg_locks` - 현재 잠금 상태
- `pg_settings` - 서버 설정
- `pg_cursors` - 현재 커서
- `pg_prepared_statements` - 준비된 문장

#### 정보 스키마 (Information Schema)와의 비교

PostgreSQL은 정보 스키마(Information Schema)도 제공함. 정보 스키마는 SQL 표준을 따르는 뷰를 제공 → 필요한 정보를 모두 얻을 수 있는 경우 PostgreSQL 전용 시스템 뷰 대신 정보 스키마 사용을 권장함. 다른 데이터베이스 시스템과의 이식성이 보장되기 때문.

---

### 54.2 시스템 뷰 목록

PostgreSQL은 다음과 같은 시스템 뷰들을 제공함.

- `pg_aios`: 비동기 I/O 작업 (Asynchronous I/O operations)
- `pg_available_extensions`: 설치 가능한 확장 모듈
- `pg_available_extension_versions`: 설치 가능한 확장 모듈 버전
- `pg_backend_memory_contexts`: 백엔드 메모리 컨텍스트
- `pg_config`: 설정 정보
- `pg_cursors`: 현재 활성 커서
- `pg_file_settings`: 파일 기반 설정
- `pg_group`: 사용자 그룹 (호환성용)
- `pg_hba_file_rules`: HBA 파일 규칙
- `pg_ident_file_mappings`: Ident 파일 매핑
- `pg_indexes`: 인덱스 정보
- `pg_locks`: 현재 잠금 상태
- `pg_matviews`: 구체화된 뷰
- `pg_policies`: 행 수준 보안 정책
- `pg_prepared_statements`: 준비된 문장
- `pg_prepared_xacts`: 준비된 트랜잭션
- `pg_publication_tables`: 게시 테이블
- `pg_replication_origin_status`: 복제 원본 상태
- `pg_replication_slots`: 복제 슬롯
- `pg_roles`: 데이터베이스 역할
- `pg_rules`: 규칙 정의
- `pg_seclabels`: 보안 레이블
- `pg_sequences`: 시퀀스 정보
- `pg_settings`: 서버 설정(GUC)
- `pg_shadow`: 사용자 비밀번호 정보 (구식)
- `pg_shmem_allocations`: 공유 메모리 할당
- `pg_shmem_allocations_numa`: NUMA 공유 메모리 할당
- `pg_stats`: 테이블 통계
- `pg_stats_ext`: 확장 통계
- `pg_stats_ext_exprs`: 확장 통계 표현식
- `pg_tables`: 테이블 정보
- `pg_timezone_abbrevs`: 시간대 약어
- `pg_timezone_names`: 시간대 이름
- `pg_user`: 데이터베이스 사용자
- `pg_user_mappings`: 외부 서버 사용자 매핑
- `pg_views`: 뷰 정의
- `pg_wait_events`: 대기 이벤트 정보

---

### 54.3 주요 시스템 뷰

#### 54.3.1 pg_stat_activity

`pg_stat_activity` 뷰는 서버 프로세스와 현재 활동에 대한 실시간 정보를 제공함. 각 서버 프로세스당 하나의 행이 포함됨.

##### 컬럼 정의

- `datid` (`oid`): 백엔드가 연결된 데이터베이스 OID
- `datname` (`name`): 백엔드가 연결된 데이터베이스 이름
- `pid` (`integer`): 백엔드 프로세스 ID
- `leader_pid` (`integer`): 병렬 그룹 리더의 프로세스 ID
- `usesysid` (`oid`): 이 백엔드에 로그인한 사용자의 OID
- `usename` (`name`): 이 백엔드에 로그인한 사용자 이름
- `application_name` (`text`): 연결된 애플리케이션 이름
- `client_addr` (`inet`): 연결된 클라이언트의 IP 주소
- `client_hostname` (`text`): 연결된 클라이언트의 호스트 이름
- `client_port` (`integer`): 클라이언트가 사용하는 TCP 포트 번호
- `backend_start` (`timestamptz`): 이 프로세스가 시작된 시간
- `xact_start` (`timestamptz`): 현재 트랜잭션이 시작된 시간
- `query_start` (`timestamptz`): 현재 활성 쿼리가 시작된 시간
- `state_change` (`timestamptz`): 상태가 마지막으로 변경된 시간
- `wait_event_type` (`text`): 백엔드가 대기 중인 이벤트 유형
- `wait_event` (`text`): 대기 중인 특정 이벤트 이름
- `state` (`text`): 현재 백엔드 상태
- `backend_xid` (`xid`): 최상위 트랜잭션 식별자
- `backend_xmin` (`xid`): 현재 백엔드의 xmin 호라이즌
- `query_id` (`bigint`): 현재 쿼리의 식별자
- `query` (`text`): 가장 최근에 실행된 쿼리 텍스트
- `backend_type` (`text`): 백엔드 유형

##### state 컬럼 값

- `starting`: 초기 시작 중, 클라이언트 인증 진행 중
- `active`: 백엔드가 쿼리를 실행 중
- `idle`: 백엔드가 새 클라이언트 명령을 대기 중
- `idle in transaction`: 트랜잭션 내에서 명령을 대기 중
- `idle in transaction (aborted)`: 실패한 트랜잭션 내에서 대기 중
- `fastpath function call`: 빠른 경로 함수를 실행 중
- `disabled`: track_activities가 비활성화됨

##### 예제

```sql
-- 현재 활성 쿼리 확인
SELECT pid, usename, datname, state, query
FROM pg_stat_activity
WHERE state = 'active';

-- 특정 시간 이상 실행 중인 쿼리 찾기
SELECT pid, usename, datname,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > interval '5 minutes';

-- 대기 중인 프로세스 확인
SELECT pid, usename, wait_event_type, wait_event, query
FROM pg_stat_activity
WHERE wait_event IS NOT NULL;

-- 데이터베이스별 연결 수 확인
SELECT datname, count(*) AS connections
FROM pg_stat_activity
GROUP BY datname;
```

---

#### 54.3.2 pg_locks

`pg_locks` 뷰는 PostgreSQL 데이터베이스 서버 내에서 활성 프로세스가 보유한 잠금에 대한 정보를 제공함.

##### 컬럼 정의

- `locktype` (`text`): 잠금 가능한 객체 유형
- `database` (`oid`): 잠금 대상이 포함된 데이터베이스 OID
- `relation` (`oid`): 잠금 대상 릴레이션의 OID
- `page` (`int4`): 릴레이션 내 페이지 번호
- `tuple` (`int2`): 페이지 내 튜플 번호
- `virtualxid` (`text`): 잠금 대상 트랜잭션의 가상 ID
- `transactionid` (`xid`): 잠금 대상 트랜잭션 ID
- `classid` (`oid`): 잠금 대상이 포함된 시스템 카탈로그 OID
- `objid` (`oid`): 시스템 카탈로그 내 잠금 대상 OID
- `objsubid` (`int2`): 잠금 대상 컬럼 번호
- `virtualtransaction` (`text`): 잠금을 보유/대기 중인 트랜잭션의 가상 ID
- `pid` (`int4`): 잠금을 보유/대기 중인 프로세스 ID
- `mode` (`text`): 보유 또는 요청 중인 잠금 모드 이름
- `granted` (`bool`): 잠금 보유 여부 (true: 보유, false: 대기)
- `fastpath` (`bool`): 빠른 경로를 통해 획득한 잠금 여부
- `waitstart` (`timestamptz`): 프로세스가 잠금 대기를 시작한 시간

##### locktype 값

- `relation`: 테이블 전체에 대한 잠금
- `extend`: 릴레이션 확장 권한
- `frozenid`: pg_database.datfrozenxid 업데이트 권한
- `page`: 릴레이션의 개별 페이지
- `tuple`: 릴레이션의 개별 튜플
- `transactionid`: 트랜잭션 ID
- `virtualxid`: 가상 트랜잭션 ID
- `spectoken`: 투기적 삽입 토큰
- `object`: 일반 데이터베이스 객체
- `userlock`: 사용자 정의 잠금
- `advisory`: 자문 잠금
- `applytransaction`: 트랜잭션 적용

##### 예제

```sql
-- 모든 잠금 정보 확인
SELECT * FROM pg_locks;

-- pg_stat_activity와 조인하여 세션 정보와 함께 확인
SELECT pl.locktype, pl.relation::regclass, pl.mode, pl.granted,
       psa.pid, psa.usename, psa.query
FROM pg_locks pl
LEFT JOIN pg_stat_activity psa ON pl.pid = psa.pid
WHERE pl.relation IS NOT NULL;

-- 대기 중인 잠금 확인
SELECT pl.pid, pl.virtualtransaction, pl.locktype,
       pl.mode, pl.relation::regclass, psa.query
FROM pg_locks pl
JOIN pg_stat_activity psa ON pl.pid = psa.pid
WHERE NOT pl.granted;

-- 차단 중인 프로세스 식별
SELECT blocked.pid AS blocked_pid,
       blocked.query AS blocked_query,
       blocking.pid AS blocking_pid,
       blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid
JOIN pg_locks blocking_locks ON blocked_locks.relation = blocking_locks.relation
                            AND blocked_locks.pid != blocking_locks.pid
JOIN pg_stat_activity blocking ON blocking_locks.pid = blocking.pid
WHERE NOT blocked_locks.granted;

-- pg_blocking_pids() 함수를 사용한 차단 프로세스 확인
SELECT pid, pg_blocking_pids(pid) AS blocked_by, query
FROM pg_stat_activity
WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

---

#### 54.3.3 pg_settings

`pg_settings` 뷰는 서버의 런타임 설정 매개변수에 접근하는 인터페이스를 제공함. `SHOW`와 `SET` 명령의 대안으로, 추가적인 메타데이터를 함께 제공함.

##### 컬럼 정의

- `name` (`text`): 런타임 설정 매개변수 이름
- `setting` (`text`): 매개변수의 현재 값
- `unit` (`text`): 매개변수의 암묵적 단위
- `category` (`text`): 매개변수의 논리적 그룹
- `short_desc` (`text`): 매개변수에 대한 간략한 설명
- `extra_desc` (`text`): 추가 상세 설명
- `context` (`text`): 매개변수 설정에 필요한 컨텍스트
- `vartype` (`text`): 매개변수 유형
- `source` (`text`): 현재 매개변수 값의 출처
- `min_val` (`text`): 최소 허용 값
- `max_val` (`text`): 최대 허용 값
- `enumvals` (`text[]`): enum 매개변수의 허용 값들
- `boot_val` (`text`): 서버 시작 시 가정되는 값
- `reset_val` (`text`): RESET이 재설정할 값
- `sourcefile` (`text`): 값이 설정된 설정 파일
- `sourceline` (`int4`): 설정 파일 내 줄 번호
- `pending_restart` (`bool`): 변경되었지만 재시작이 필요한지 여부

##### context 컬럼 값 (변경 난이도 순)

- `internal`: 직접 변경 불가, 내부적으로 결정된 값
- `postmaster`: 서버 시작 시에만 설정 가능, 재시작 필요
- `sighup`: postgresql.conf에서 SIGHUP 신호로 변경 가능
- `superuser-backend`: 슈퍼유저만 세션 시작 시 설정 가능
- `backend`: 모든 사용자가 세션 시작 시 설정 가능
- `superuser`: 슈퍼유저만 SET으로 변경 가능
- `user`: 모든 사용자가 SET으로 변경 가능

##### 예제

```sql
-- 모든 설정 확인
SELECT name, setting, unit, category
FROM pg_settings;

-- 메모리 관련 설정 확인
SELECT name, setting, unit, short_desc
FROM pg_settings
WHERE category LIKE '%Memory%';

-- 재시작이 필요한 변경된 설정 확인
SELECT name, setting, boot_val
FROM pg_settings
WHERE pending_restart = true;

-- 특정 설정 변경 (pg_settings 업데이트)
UPDATE pg_settings
SET setting = '100'
WHERE name = 'work_mem';
-- 이는 SET work_mem = '100'과 동일

-- 설정 파일에서 설정된 값 확인
SELECT name, setting, sourcefile, sourceline
FROM pg_settings
WHERE sourcefile IS NOT NULL;

-- 기본값과 다른 설정 확인
SELECT name, setting, boot_val, source
FROM pg_settings
WHERE setting != boot_val;
```

---

#### 54.3.4 pg_tables

`pg_tables` 뷰는 데이터베이스의 각 테이블에 대한 유용한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 테이블을 포함하는 스키마 이름
- `tablename` (`name`): 테이블 이름
- `tableowner` (`name`): 테이블 소유자 이름
- `tablespace` (`name`): 테이블이 포함된 테이블스페이스 이름 (기본값인 경우 null)
- `hasindexes` (`bool`): 테이블에 인덱스가 있는지 여부
- `hasrules` (`bool`): 테이블에 규칙이 있는지 여부
- `hastriggers` (`bool`): 테이블에 트리거가 있는지 여부
- `rowsecurity` (`bool`): 행 수준 보안이 활성화되어 있는지 여부

##### 예제

```sql
-- 모든 사용자 테이블 확인
SELECT schemaname, tablename, tableowner
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');

-- 인덱스가 없는 테이블 확인
SELECT schemaname, tablename
FROM pg_tables
WHERE hasindexes = false
  AND schemaname NOT IN ('pg_catalog', 'information_schema');

-- 행 수준 보안이 활성화된 테이블 확인
SELECT schemaname, tablename
FROM pg_tables
WHERE rowsecurity = true;

-- 특정 스키마의 테이블 목록
SELECT tablename, tableowner
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
```

---

#### 54.3.5 pg_indexes

`pg_indexes` 뷰는 데이터베이스의 각 인덱스에 대한 유용한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 테이블과 인덱스를 포함하는 스키마 이름
- `tablename` (`name`): 인덱스가 적용된 테이블 이름
- `indexname` (`name`): 인덱스 이름
- `tablespace` (`name`): 인덱스가 포함된 테이블스페이스 이름
- `indexdef` (`text`): 인덱스 정의 (재구성된 CREATE INDEX 명령)

##### 예제

```sql
-- 특정 테이블의 인덱스 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users';

-- 스키마별 인덱스 수 확인
SELECT schemaname, count(*) AS index_count
FROM pg_indexes
GROUP BY schemaname;

-- 모든 사용자 정의 인덱스와 정의 확인
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY schemaname, tablename;

-- B-tree 인덱스만 확인
SELECT indexname, tablename, indexdef
FROM pg_indexes
WHERE indexdef LIKE '%btree%';
```

---

#### 54.3.6 pg_views

`pg_views` 뷰는 데이터베이스의 각 뷰에 대한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 뷰를 포함하는 스키마 이름
- `viewname` (`name`): 뷰 이름
- `viewowner` (`name`): 뷰 소유자 이름
- `definition` (`text`): 뷰 정의 (재구성된 SELECT 쿼리)

##### 예제

```sql
-- 모든 사용자 정의 뷰 확인
SELECT schemaname, viewname, viewowner, definition
FROM pg_views
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');

-- 특정 뷰의 정의 확인
SELECT definition
FROM pg_views
WHERE viewname = 'my_view';

-- 특정 테이블을 참조하는 뷰 찾기
SELECT viewname, definition
FROM pg_views
WHERE definition LIKE '%users%';
```

---

#### 54.3.7 pg_roles

`pg_roles` 뷰는 데이터베이스 역할에 대한 정보를 제공함. `pg_authid` 카탈로그 테이블을 누구나 읽을 수 있도록 공개한 뷰이며, 보안을 위해 비밀번호 필드는 숨겨져 있음.

##### 컬럼 정의

- `rolname` (`name`): 역할 이름
- `rolsuper` (`bool`): 슈퍼유저 권한 여부
- `rolinherit` (`bool`): 멤버인 역할의 권한을 자동으로 상속하는지 여부
- `rolcreaterole` (`bool`): 역할 생성 가능 여부
- `rolcreatedb` (`bool`): 데이터베이스 생성 가능 여부
- `rolcanlogin` (`bool`): 로그인 가능 여부
- `rolreplication` (`bool`): 복제 역할 여부
- `rolconnlimit` (`int4`): 최대 동시 연결 수 (-1은 제한 없음)
- `rolpassword` (`text`): 비밀번호 (항상 `********`로 표시)
- `rolvaliduntil` (`timestamptz`): 비밀번호 만료 시간
- `rolbypassrls` (`bool`): 행 수준 보안 정책 우회 여부
- `rolconfig` (`text[]`): 역할별 런타임 설정 기본값
- `oid` (`oid`): 역할 ID

##### 예제

```sql
-- 모든 역할 확인
SELECT rolname, rolsuper, rolcreatedb, rolcanlogin
FROM pg_roles;

-- 로그인 가능한 역할만 확인
SELECT rolname, rolconnlimit, rolvaliduntil
FROM pg_roles
WHERE rolcanlogin = true;

-- 슈퍼유저 역할 확인
SELECT rolname
FROM pg_roles
WHERE rolsuper = true;

-- 연결 제한이 있는 역할 확인
SELECT rolname, rolconnlimit
FROM pg_roles
WHERE rolconnlimit > 0;
```

---

#### 54.3.8 pg_stats

`pg_stats` 뷰는 `pg_statistic` 카탈로그에 저장된 정보를 더 읽기 쉬운 형태로 제공함. 사용자는 읽기 권한이 있는 테이블의 통계에만 접근 가능.

##### 컬럼 정의

- `schemaname` (`name`): 테이블을 포함하는 스키마 이름
- `tablename` (`name`): 테이블 이름
- `attname` (`name`): 컬럼 이름
- `inherited` (`bool`): 자식 테이블의 값도 포함하는지 여부
- `null_frac` (`float4`): null인 항목의 비율
- `avg_width` (`int4`): 컬럼 항목의 평균 바이트 너비
- `n_distinct` (`float4`): 고유 값의 추정 개수
- `most_common_vals` (`anyarray`): 가장 흔한 값 목록
- `most_common_freqs` (`float4[]`): 가장 흔한 값의 빈도
- `histogram_bounds` (`anyarray`): 히스토그램 경계 값
- `correlation` (`float4`): 물리적 행 순서와 논리적 순서 간의 상관관계
- `most_common_elems` (`anyarray`): 가장 자주 나타나는 요소 값 (배열 유형)
- `most_common_elem_freqs` (`float4[]`): 가장 흔한 요소 값의 빈도
- `elem_count_histogram` (`float4[]`): 고유 요소 개수 히스토그램
- `range_length_histogram` (`anyarray`): 범위 값 길이 히스토그램 (range 타입)
- `range_empty_frac` (`float4`): 빈 범위 항목의 비율 (range 타입)
- `range_bounds_histogram` (`anyarray`): 범위 경계 히스토그램 (range 타입)

##### 예제

```sql
-- 특정 테이블의 컬럼 통계 확인
SELECT attname, null_frac, n_distinct, avg_width
FROM pg_stats
WHERE tablename = 'users';

-- null 비율이 높은 컬럼 찾기
SELECT schemaname, tablename, attname, null_frac
FROM pg_stats
WHERE null_frac > 0.5
ORDER BY null_frac DESC;

-- 가장 흔한 값 확인
SELECT attname, most_common_vals, most_common_freqs
FROM pg_stats
WHERE tablename = 'orders'
  AND most_common_vals IS NOT NULL;
```

---

#### 54.3.9 pg_cursors

`pg_cursors` 뷰는 현재 사용 가능한 모든 커서를 나열함.

##### 컬럼 정의

- `name` (`text`): 커서 이름
- `statement` (`text`): 커서를 선언하기 위해 제출된 쿼리 문자열
- `is_holdable` (`bool`): 트랜잭션 커밋 후에도 접근 가능한지 여부
- `is_binary` (`bool`): BINARY로 선언되었는지 여부
- `is_scrollable` (`bool`): 비순차적 행 검색이 가능한지 여부
- `creation_time` (`timestamptz`): 커서가 선언된 시간

##### 예제

```sql
-- 현재 열린 커서 확인
SELECT name, statement, is_holdable, is_scrollable, creation_time
FROM pg_cursors;

-- holdable 커서만 확인
SELECT name, statement
FROM pg_cursors
WHERE is_holdable = true;
```

---

#### 54.3.10 pg_prepared_statements

`pg_prepared_statements` 뷰는 현재 세션에서 사용 가능한 모든 준비된 문장을 표시함.

##### 컬럼 정의

- `name` (`text`): 준비된 문장의 식별자
- `statement` (`text`): 클라이언트가 제출한 쿼리 문자열
- `prepare_time` (`timestamptz`): 준비된 문장이 생성된 시간
- `parameter_types` (`regtype[]`): 예상 매개변수 유형 배열
- `result_types` (`regtype[]`): 반환되는 컬럼 유형 배열
- `from_sql` (`bool`): SQL PREPARE 명령으로 생성되었는지 여부
- `generic_plans` (`int8`): 제네릭 계획이 선택된 횟수
- `custom_plans` (`int8`): 커스텀 계획이 선택된 횟수

##### 예제

```sql
-- 현재 세션의 준비된 문장 확인
SELECT name, statement, prepare_time
FROM pg_prepared_statements;

-- 매개변수 정보와 함께 확인
SELECT name, statement, parameter_types, result_types
FROM pg_prepared_statements;

-- 계획 통계 확인
SELECT name, generic_plans, custom_plans
FROM pg_prepared_statements;
```

---

#### 54.3.11 pg_replication_slots

`pg_replication_slots` 뷰는 데이터베이스 클러스터에 현재 존재하는 모든 복제 슬롯과 상태 정보를 제공함.

##### 컬럼 정의

- `slot_name` (`name`): 복제 슬롯의 고유 식별자
- `plugin` (`name`): 논리 슬롯이 사용하는 출력 플러그인 이름
- `slot_type` (`text`): 슬롯 유형: `physical` 또는 `logical`
- `datoid` (`oid`): 연결된 데이터베이스 OID
- `database` (`name`): 연결된 데이터베이스 이름
- `temporary` (`bool`): 임시 복제 슬롯 여부
- `active` (`bool`): 현재 스트리밍 중인지 여부
- `active_pid` (`int4`): 스트리밍 세션의 프로세스 ID
- `xmin` (`xid`): 유지해야 하는 가장 오래된 트랜잭션
- `catalog_xmin` (`xid`): 시스템 카탈로그에 영향을 미치는 가장 오래된 트랜잭션
- `restart_lsn` (`pg_lsn`): 필요할 수 있는 가장 오래된 WAL의 주소
- `confirmed_flush_lsn` (`pg_lsn`): 수신 확인된 데이터까지의 주소
- `wal_status` (`text`): WAL 파일 가용성 상태
- `safe_wal_size` (`int8`): 안전하게 쓸 수 있는 WAL 바이트 수
- `two_phase` (`bool`): 준비된 트랜잭션 디코딩 활성화 여부
- `two_phase_at` (`pg_lsn`): 준비된 트랜잭션 디코딩이 활성화된 LSN
- `inactive_since` (`timestamptz`): 슬롯이 비활성화된 시간
- `conflicting` (`bool`): 복구와 충돌하여 무효화되었는지 여부
- `invalidation_reason` (`text`): 슬롯 무효화 이유
- `failover` (`bool`): 장애 조치를 위해 스탠바이로 동기화 활성화 여부
- `synced` (`bool`): 기본 서버에서 동기화되었는지 여부

##### 예제

```sql
-- 모든 복제 슬롯 확인
SELECT slot_name, slot_type, active, restart_lsn
FROM pg_replication_slots;

-- 활성 복제 슬롯 확인
SELECT slot_name, active_pid, database
FROM pg_replication_slots
WHERE active = true;

-- 비활성 슬롯과 비활성 시간 확인
SELECT slot_name, inactive_since, wal_status
FROM pg_replication_slots
WHERE active = false;
```

---

#### 54.3.12 pg_sequences

`pg_sequences` 뷰는 데이터베이스의 각 시퀀스에 대한 유용한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 시퀀스를 포함하는 스키마 이름
- `sequencename` (`name`): 시퀀스 이름
- `sequenceowner` (`name`): 시퀀스 소유자 이름
- `data_type` (`regtype`): 시퀀스의 데이터 유형
- `start_value` (`int8`): 시퀀스 시작 값
- `min_value` (`int8`): 시퀀스 최소값
- `max_value` (`int8`): 시퀀스 최대값
- `increment_by` (`int8`): 시퀀스 증가값
- `cycle` (`bool`): 시퀀스 순환 여부
- `cache_size` (`int8`): 시퀀스 캐시 크기
- `last_value` (`int8`): 디스크에 기록된 마지막 시퀀스 값

##### 예제

```sql
-- 모든 시퀀스 정보 확인
SELECT schemaname, sequencename, start_value, increment_by, last_value
FROM pg_sequences;

-- 순환하는 시퀀스 확인
SELECT sequencename, min_value, max_value
FROM pg_sequences
WHERE cycle = true;

-- 캐시 크기가 1보다 큰 시퀀스 확인
SELECT sequencename, cache_size
FROM pg_sequences
WHERE cache_size > 1;
```

---

#### 54.3.13 pg_matviews

`pg_matviews` 뷰는 데이터베이스의 각 구체화된 뷰(Materialized View)에 대한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 구체화된 뷰를 포함하는 스키마 이름
- `matviewname` (`name`): 구체화된 뷰 이름
- `matviewowner` (`name`): 구체화된 뷰 소유자 이름
- `tablespace` (`name`): 구체화된 뷰가 포함된 테이블스페이스 이름
- `hasindexes` (`bool`): 인덱스가 있는지 여부
- `ispopulated` (`bool`): 현재 데이터가 채워져 있는지 여부
- `definition` (`text`): 구체화된 뷰 정의 (재구성된 SELECT 쿼리)

##### 예제

```sql
-- 모든 구체화된 뷰 확인
SELECT schemaname, matviewname, ispopulated
FROM pg_matviews;

-- 데이터가 채워지지 않은 구체화된 뷰 확인
SELECT matviewname, definition
FROM pg_matviews
WHERE ispopulated = false;

-- 인덱스가 없는 구체화된 뷰 확인
SELECT matviewname
FROM pg_matviews
WHERE hasindexes = false;
```

---

#### 54.3.14 pg_policies

`pg_policies` 뷰는 데이터베이스의 각 행 수준 보안(Row Level Security, RLS) 정책에 대한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 정책이 적용된 테이블의 스키마 이름
- `tablename` (`name`): 정책이 적용된 테이블 이름
- `policyname` (`name`): 정책 이름
- `permissive` (`text`): 정책이 허용적(permissive)인지 제한적(restrictive)인지
- `roles` (`name[]`): 정책이 적용되는 역할
- `cmd` (`text`): 정책이 적용되는 명령 유형
- `qual` (`text`): 쿼리에 추가되는 보안 장벽 조건 표현식
- `with_check` (`text`): 행 추가 시 WITH CHECK 조건 표현식

##### 예제

```sql
-- 모든 RLS 정책 확인
SELECT schemaname, tablename, policyname, permissive, roles, cmd
FROM pg_policies;

-- 특정 테이블의 정책 확인
SELECT policyname, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'documents';

-- 제한적 정책만 확인
SELECT tablename, policyname, qual
FROM pg_policies
WHERE permissive = 'RESTRICTIVE';
```

---

#### 54.3.15 pg_rules

`pg_rules` 뷰는 쿼리 재작성 규칙에 대한 정보를 제공함.

##### 컬럼 정의

- `schemaname` (`name`): 테이블을 포함하는 스키마 이름
- `tablename` (`name`): 규칙이 적용된 테이블 이름
- `rulename` (`name`): 규칙 이름
- `definition` (`text`): 규칙 정의 (재구성된 생성 명령)

> 참고: `pg_rules` 뷰는 뷰와 구체화된 뷰의 `ON SELECT` 규칙을 제외함. 뷰의 `ON SELECT` 규칙은 `pg_views`에서, 구체화된 뷰의 것은 `pg_matviews`에서 확인 가능.

##### 예제

```sql
-- 모든 규칙 확인
SELECT schemaname, tablename, rulename, definition
FROM pg_rules;

-- 특정 테이블의 규칙 확인
SELECT rulename, definition
FROM pg_rules
WHERE tablename = 'orders';
```

---

#### 54.3.16 pg_available_extensions

`pg_available_extensions` 뷰는 PostgreSQL에서 설치할 수 있는 확장 모듈을 나열함.

##### 컬럼 정의

- `name` (`name`): 확장 모듈 이름
- `default_version` (`text`): 기본 버전 이름
- `installed_version` (`text`): 현재 설치된 버전 (설치되지 않은 경우 NULL)
- `comment` (`text`): 확장 모듈의 제어 파일에서 가져온 설명

##### 예제

```sql
-- 사용 가능한 모든 확장 모듈 확인
SELECT name, default_version, installed_version, comment
FROM pg_available_extensions
ORDER BY name;

-- 설치된 확장 모듈만 확인
SELECT name, installed_version
FROM pg_available_extensions
WHERE installed_version IS NOT NULL;

-- 설치되지 않은 확장 모듈 확인
SELECT name, default_version, comment
FROM pg_available_extensions
WHERE installed_version IS NULL;
```

---

### 54.4 통계 뷰

PostgreSQL은 데이터베이스 활동을 모니터링하기 위한 다양한 통계 뷰를 제공함. 이 뷰들은 누적 통계 시스템(Cumulative Statistics System)의 일부임.

#### 주요 통계 뷰 목록

- `pg_stat_activity`: 서버 프로세스당 하나의 행, 현재 활동 정보
- `pg_stat_replication`: WAL 전송자 프로세스당 하나의 행
- `pg_stat_replication_slots`: 복제 슬롯당 하나의 행
- `pg_stat_wal_receiver`: WAL 수신자당 하나의 행
- `pg_stat_subscription`: 구독당 하나의 행
- `pg_stat_ssl`: SSL 연결 정보
- `pg_stat_gssapi`: GSSAPI 인증 정보
- `pg_stat_archiver`: 아카이버 프로세스 통계
- `pg_stat_io`: I/O 통계
- `pg_stat_bgwriter`: 백그라운드 작성자 통계
- `pg_stat_checkpointer`: 체크포인터 통계
- `pg_stat_wal`: WAL 활동 통계
- `pg_stat_database`: 데이터베이스별 통계
- `pg_stat_database_conflicts`: 데이터베이스 충돌 통계
- `pg_stat_all_tables`: 모든 테이블 접근 통계
- `pg_stat_sys_tables`: 시스템 테이블 접근 통계
- `pg_stat_user_tables`: 사용자 테이블 접근 통계
- `pg_stat_all_indexes`: 모든 인덱스 접근 통계
- `pg_stat_sys_indexes`: 시스템 인덱스 접근 통계
- `pg_stat_user_indexes`: 사용자 인덱스 접근 통계
- `pg_statio_all_tables`: 모든 테이블 I/O 통계
- `pg_statio_all_indexes`: 모든 인덱스 I/O 통계
- `pg_statio_all_sequences`: 모든 시퀀스 I/O 통계
- `pg_stat_user_functions`: 사용자 함수 통계
- `pg_stat_slru`: SLRU 통계

#### 통계 수집 구성

통계 수집을 활성화하려면 `postgresql.conf`에서 다음 매개변수를 설정.

```sql
-- 현재 명령 모니터링
track_activities = on

-- 테이블/인덱스 접근 통계 수집
track_counts = on

-- 사용자 정의 함수 추적
track_functions = 'all'  -- 또는 'pl' 또는 'none'

-- 블록 I/O 시간 모니터링
track_io_timing = on

-- WAL I/O 시간 모니터링
track_wal_io_timing = on
```

#### 통계 재설정 함수

```sql
-- 현재 데이터베이스의 모든 통계 재설정
SELECT pg_stat_reset();

-- 특정 공유 통계 재설정
SELECT pg_stat_reset_shared('archiver');  -- 또는 'bgwriter', 'wal', 'io' 등

-- 단일 테이블 통계 재설정
SELECT pg_stat_reset_single_table_counters(table_oid);

-- 현재 트랜잭션의 캐시된 값 지우기
SELECT pg_stat_clear_snapshot();
```

---

### 54.5 유용한 쿼리 예제

#### 현재 데이터베이스 활동 모니터링

```sql
-- 현재 실행 중인 쿼리 확인
SELECT pid, usename, datname, state,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC;
```

#### 잠금 대기 확인

```sql
-- 잠금을 대기 중인 쿼리 확인
SELECT blocked.pid AS blocked_pid,
       blocked.usename AS blocked_user,
       blocked.query AS blocked_query,
       blocking.pid AS blocking_pid,
       blocking.usename AS blocking_user,
       blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_stat_activity blocking
  ON blocking.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE cardinality(pg_blocking_pids(blocked.pid)) > 0;
```

#### 테이블 사용 통계

```sql
-- 테이블별 순차 스캔 vs 인덱스 스캔
SELECT schemaname, relname,
       seq_scan, seq_tup_read,
       idx_scan, idx_tup_fetch,
       n_live_tup, n_dead_tup
FROM pg_stat_all_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY seq_scan DESC;
```

#### 인덱스 사용 현황

```sql
-- 사용되지 않는 인덱스 찾기
SELECT schemaname, relname, indexrelname, idx_scan
FROM pg_stat_all_indexes
WHERE idx_scan = 0
  AND schemaname NOT IN ('pg_catalog', 'information_schema');
```

#### 데이터베이스 연결 현황

```sql
-- 데이터베이스별 연결 수와 상태
SELECT datname,
       count(*) AS total,
       count(*) FILTER (WHERE state = 'active') AS active,
       count(*) FILTER (WHERE state = 'idle') AS idle,
       count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_txn
FROM pg_stat_activity
WHERE datname IS NOT NULL
GROUP BY datname;
```

#### 캐시 적중률 확인

```sql
-- 테이블 캐시 적중률
SELECT schemaname, relname,
       heap_blks_read, heap_blks_hit,
       CASE WHEN heap_blks_read + heap_blks_hit > 0
            THEN round(100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit), 2)
            ELSE 0
       END AS cache_hit_ratio
FROM pg_statio_all_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY heap_blks_read DESC;
```

#### 오래 실행 중인 트랜잭션

```sql
-- 5분 이상 실행 중인 트랜잭션
SELECT pid, usename, datname, state,
       now() - xact_start AS txn_duration,
       now() - query_start AS query_duration,
       query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
  AND now() - xact_start > interval '5 minutes'
ORDER BY txn_duration DESC;
```

#### 설정 변경 이력

```sql
-- 기본값에서 변경된 설정
SELECT name, setting, boot_val, source, sourcefile, sourceline
FROM pg_settings
WHERE setting != boot_val
  AND source NOT IN ('default', 'override')
ORDER BY name;
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - System Views](https://www.postgresql.org/docs/current/views.html)
- [PostgreSQL 공식 문서 - Monitoring Database Activity](https://www.postgresql.org/docs/current/monitoring.html)
- [PostgreSQL 공식 문서 - Information Schema](https://www.postgresql.org/docs/current/information-schema.html)

---

## Chapter 55: Frontend/Backend Protocol

PostgreSQL은 클라이언트(Frontend)와 서버(Backend) 간의 통신을 위해 메시지 기반 프로토콜을 사용함. 이 장에서는 프로토콜의 구조, 메시지 흐름, 형식, 오류 처리를 다룸.

### 목차

1. [프로토콜 개요 (Protocol Overview)](#1-프로토콜-개요-protocol-overview)
2. [메시지 흐름 (Message Flow)](#2-메시지-흐름-message-flow)
3. [메시지 데이터 타입 (Message Data Types)](#3-메시지-데이터-타입-message-data-types)
4. [메시지 형식 (Message Formats)](#4-메시지-형식-message-formats)
5. [오류 및 경고 메시지 (Error and Notice Messages)](#5-오류-및-경고-메시지-error-and-notice-messages)
6. [확장 쿼리 프로토콜 (Extended Query Protocol)](#6-확장-쿼리-프로토콜-extended-query-protocol)
7. [SASL 인증 (SASL Authentication)](#7-sasl-인증-sasl-authentication)
8. [복제 프로토콜 (Replication Protocol)](#8-복제-프로토콜-replication-protocol)

---

### 1. 프로토콜 개요 (Protocol Overview)

#### 1.1 기본 구조

PostgreSQL의 Frontend/Backend 프로토콜은 두 가지 주요 단계로 구성됨.

1. 시작 단계 (Startup Phase): Frontend가 서버에 연결 → 인증 수행
2. 정상 운영 단계 (Normal Operation): 쿼리 실행 → 결과 반환

```
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Protocol                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐                      ┌─────────────┐       │
│  │  Frontend   │  ←─── Messages ───→  │  Backend    │       │
│  │  (Client)   │                      │  (Server)   │       │
│  └─────────────┘                      └─────────────┘       │
│                                                              │
│  Phase 1: Startup (인증, 설정)                               │
│  Phase 2: Normal Operation (쿼리, 결과)                      │
└─────────────────────────────────────────────────────────────┘
```

#### 1.2 메시징 개요 (Messaging Overview)

모든 통신은 메시지 스트림으로 이루어짐. 각 메시지의 기본 구조:

```
┌──────────────────┬────────────────────┬──────────────────────┐
│ Message Type (1) │ Length (4 bytes)   │ Message Contents     │
│ (1 byte)         │ (includes self)    │ (variable)           │
└──────────────────┴────────────────────┴──────────────────────┘
```

메시지 구조 상세:
- 첫 번째 바이트: 메시지 타입 식별자 (예: 'Q' = Query, 'R' = Authentication)
- 다음 4바이트: 메시지 길이 (길이 필드 자신 포함, 타입 바이트 제외)
- 나머지: 메시지 내용

> 참고: 초기 시작 메시지(StartupMessage)는 예외적으로 메시지 타입 바이트가 없음.

#### 1.3 프로토콜 버전 (Protocol Versions)

- 3.2: 지원 PostgreSQL 18+, 최신 버전 · 가변 길이 취소 키 지원
- 3.1: 지원 대상 없음, 예약됨(pgbouncer 버그로 스킵)
- 3.0: 지원 PostgreSQL 7.4+, 표준 버전
- 2.0: 지원 PostgreSQL 13까지, 레거시(더 이상 권장하지 않음)

버전 협상:
```
Frontend → Backend: StartupMessage (프로토콜 버전 지정)
                           ↓
Backend 응답:
  - 주 버전 불일치 → 연결 거부
  - 부 버전 불일치 → NegotiateProtocolVersion 메시지
```

#### 1.4 형식 코드 (Format Codes)

PostgreSQL 프로토콜은 두 가지 데이터 형식을 지원함.

- Text: 코드 0, 텍스트 표현(기본값)
- Binary: 코드 1, 바이너리 표현(네트워크 바이트 순서)

```c
// Text 형식 예시
"12345"          // 정수 12345의 텍스트 표현

// Binary 형식 예시 (네트워크 바이트 순서)
0x00 0x00 0x30 0x39  // 정수 12345의 바이너리 표현
```

---

### 2. 메시지 흐름 (Message Flow)

#### 2.1 시작 단계 (Start-up)

연결 초기화 과정:

```
Frontend                              Backend
   │                                     │
   │─────── StartupMessage ──────────────>│
   │    (user, database, options)        │
   │                                     │
   │<────── AuthenticationXXX ───────────│
   │    (인증 요청)                       │
   │                                     │
   │─────── PasswordMessage ─────────────>│
   │    (필요시)                          │
   │                                     │
   │<────── AuthenticationOk ────────────│
   │<────── ParameterStatus (여러개) ────│
   │<────── BackendKeyData ──────────────│
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

인증 메시지 종류:

- `AuthenticationOk`: 인증 성공
- `AuthenticationCleartextPassword`: 평문 비밀번호 요청
- `AuthenticationMD5Password`: MD5 암호화 비밀번호 요청
- `AuthenticationSASL`: SASL 인증 시작
- `AuthenticationGSS`: GSSAPI 인증

MD5 비밀번호 계산:
```python
# MD5 비밀번호 계산 공식
import hashlib

def md5_password(password, username, salt):
    # 1단계: password + username의 MD5
    step1 = hashlib.md5((password + username).encode()).hexdigest()
    # 2단계: step1 + salt의 MD5
    step2 = hashlib.md5((step1 + salt.hex()).encode()).hexdigest()
    return 'md5' + step2
```

#### 2.2 단순 쿼리 (Simple Query)

가장 기본적인 쿼리 실행 방식임.

```
Frontend                              Backend
   │                                     │
   │─────── Query ───────────────────────>│
   │    "SELECT * FROM users"            │
   │                                     │
   │<────── RowDescription ──────────────│
   │    (열 정보)                         │
   │<────── DataRow ─────────────────────│
   │    (행 데이터)                       │
   │<────── DataRow ─────────────────────│
   │<────── CommandComplete ─────────────│
   │    "SELECT 2"                       │
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

응답 메시지 종류:

- `RowDescription`: 반환될 열의 구조 설명
- `DataRow`: 실제 행 데이터
- `CommandComplete`: 명령 완료(영향받은 행 수 포함)
- `EmptyQueryResponse`: 빈 쿼리 문자열
- `ErrorResponse`: 오류 발생
- `ReadyForQuery`: 다음 명령 준비 완료

예시: SELECT 쿼리 결과
```
Frontend: Query ("SELECT id, name FROM users LIMIT 2;")
Backend:  RowDescription (id: int4, name: text)
Backend:  DataRow (1, 'Alice')
Backend:  DataRow (2, 'Bob')
Backend:  CommandComplete ("SELECT 2")
Backend:  ReadyForQuery ('I')  // 'I' = Idle
```

#### 2.3 다중 문장 처리

단순 쿼리는 세미콜론으로 구분된 여러 SQL 문을 포함 가능.

```sql
-- 암시적 트랜잭션 블록
INSERT INTO mytable VALUES(1);
SELECT 1/0;           -- 에러 발생
INSERT INTO mytable VALUES(2);  -- 실행되지 않음
```

결과: 첫 번째 INSERT도 롤백됨 (암시적 트랜잭션)

```sql
-- 명시적 트랜잭션
BEGIN;
INSERT INTO mytable VALUES(1);
COMMIT;              -- 첫 INSERT 커밋됨
INSERT INTO mytable VALUES(2);
SELECT 1/0;          -- 에러 발생
-- 두 번째 INSERT만 롤백
```

#### 2.4 COPY 작업

##### Copy-In (Frontend → Backend)
```
Backend: CopyInResponse
   ↓
Frontend → Backend: CopyData (반복)
Frontend → Backend: CopyDone
   ↓
Backend: CommandComplete
```

##### Copy-Out (Backend → Frontend)
```
Backend: CopyOutResponse
   ↓
Backend → Frontend: CopyData (반복)
Backend → Frontend: CopyDone
   ↓
Backend: CommandComplete
```

#### 2.5 비동기 작업 (Asynchronous Operations)

Backend에서 언제든지 전송될 수 있는 메시지:

1. NoticeResponse: 경고 메시지
2. ParameterStatus: 파라미터 변경 알림
3. NotificationResponse: LISTEN/NOTIFY 알림

```sql
-- 알림 예시
LISTEN my_channel;

-- 다른 세션에서
NOTIFY my_channel, 'Hello!';

-- 수신 측에서 NotificationResponse 메시지 수신
```

#### 2.6 요청 취소 (Canceling Requests)

실행 중인 쿼리 취소 절차:

```
Frontend: 새 연결 생성
   ↓
Frontend → Backend: CancelRequest (PID + Secret Key)
   ↓
Backend: 기존 쿼리 취소 시도
Backend: 새 연결 즉시 종료
```

> 주의: 취소 성공 여부는 보장되지 않음 → Frontend는 원래 연결에서 계속 대기 필요.

#### 2.7 연결 종료 (Termination)

정상 종료:
```
Frontend → Backend: Terminate
Frontend: 연결 닫기
```

비정상 종료:
- Backend 오류 시: ErrorResponse 전송 후 연결 종료
- 진행 중인 트랜잭션은 자동으로 롤백

---

### 3. 메시지 데이터 타입 (Message Data Types)

프로토콜에서 사용되는 기본 데이터 타입.

#### 3.1 정수형 (Integer)

- `Int8`: 8비트 부호 있는 정수
- `Int16`: 16비트 부호 있는 정수
- `Int32`: 32비트 부호 있는 정수
- `Int32(i)`: 특정 값 i를 가진 32비트 정수, 예시 `Int32(0)` = 0

바이트 순서: 네트워크 바이트 순서(빅엔디안, 최상위 바이트 우선)

```c
// Int32를 네트워크 바이트 순서로 변환
uint32_t value = 12345;
uint8_t bytes[4];
bytes[0] = (value >> 24) & 0xFF;  // 0x00
bytes[1] = (value >> 16) & 0xFF;  // 0x00
bytes[2] = (value >> 8) & 0xFF;   // 0x30
bytes[3] = value & 0xFF;          // 0x39
```

#### 3.2 문자열 (String)

- C 스타일 null 종료 문자열
- 길이 제한 없음 (메모리 범위 내)
- 문자열 내부에 null 문자 포함 불가

```
String("user") = 'u' 's' 'e' 'r' '\0'
```

#### 3.3 바이트 수열 (Byte Sequence)

- `Byte1`: 단일 바이트
- `Byte1('c')`: 특정 문자 c
- `Byten`: n바이트 수열
- `Byte[n]`: n개 바이트 배열

---

### 4. 메시지 형식 (Message Formats)

#### 4.1 시작 메시지

##### StartupMessage (F)
```
┌─────────────────────────────────────────────────────┐
│ Int32     │ 메시지 길이 (자신 포함)                  │
│ Int32     │ 프로토콜 버전 (196610 = 3.2)            │
│ String    │ 파라미터 이름 (예: "user")              │
│ String    │ 파라미터 값                             │
│ ...       │ 추가 파라미터 쌍                        │
│ Byte1(0)  │ 종료자                                  │
└─────────────────────────────────────────────────────┘
```

필수 파라미터:
- `user`: 연결할 사용자명

선택 파라미터:
- `database`: 연결할 데이터베이스 (기본값: user명과 동일)
- `options`: 런타임 파라미터
- `replication`: 복제 모드

```python
# Python 예시: StartupMessage 생성
def create_startup_message(user, database):
    params = f"user\x00{user}\x00database\x00{database}\x00\x00"
    # 프로토콜 버전 3.0 = 196608 (0x00030000)
    version = struct.pack('>I', 196608)
    length = struct.pack('>I', 4 + len(version) + len(params))
    return length + version + params.encode()
```

##### CancelRequest (F)
```
┌─────────────────────────────────────────────────────┐
│ Int32     │ 메시지 길이 (16)                        │
│ Int32     │ 취소 요청 코드 (80877102)               │
│ Int32     │ 대상 Backend 프로세스 ID                │
│ Int32     │ 비밀 키                                  │
└─────────────────────────────────────────────────────┘
```

#### 4.2 인증 메시지

##### AuthenticationOk (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32(8)   │ 메시지 길이                            │
│ Int32(0)   │ 인증 성공 코드                         │
└─────────────────────────────────────────────────────┘
```

##### AuthenticationMD5Password (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32(12)  │ 메시지 길이                            │
│ Int32(5)   │ MD5 인증 코드                          │
│ Byte4      │ Salt (암호화용)                        │
└─────────────────────────────────────────────────────┘
```

##### AuthenticationSASL (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int32(10)  │ SASL 인증 코드                         │
│ String[]   │ SASL 메커니즘 목록 (null 종료)         │
└─────────────────────────────────────────────────────┘
```

##### PasswordMessage (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('p') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 비밀번호 (평문 또는 MD5)               │
└─────────────────────────────────────────────────────┘
```

#### 4.3 쿼리 메시지

##### Query (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('Q') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ SQL 쿼리 문자열                        │
└─────────────────────────────────────────────────────┘
```

```python
# Python 예시: Query 메시지 생성
def create_query_message(sql):
    query = sql.encode() + b'\x00'
    length = struct.pack('>I', 4 + len(query))
    return b'Q' + length + query
```

##### Parse (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('P') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 준비된 문(Prepared Statement) 이름     │
│ String     │ 쿼리 문자열                            │
│ Int16      │ 파라미터 타입 개수                     │
│ Int32[]    │ 각 파라미터의 타입 OID                 │
└─────────────────────────────────────────────────────┘
```

##### Bind (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('B') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 대상 포털 이름                         │
│ String     │ 소스 준비된 문 이름                    │
│ Int16      │ 파라미터 형식 코드 개수                │
│ Int16[]    │ 파라미터 형식 코드 (0=text, 1=binary)  │
│ Int16      │ 파라미터 값 개수                       │
│ [Int32 + Byte[]]│ 각 파라미터 (길이 + 값)           │
│ Int16      │ 결과 열 형식 코드 개수                 │
│ Int16[]    │ 결과 형식 코드                         │
└─────────────────────────────────────────────────────┘
```

##### Execute (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('E') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 포털 이름                              │
│ Int32      │ 최대 반환 행 수 (0 = 무제한)           │
└─────────────────────────────────────────────────────┘
```

##### Describe (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('D') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte1      │ 'S' = Statement, 'P' = Portal          │
│ String     │ 이름                                   │
└─────────────────────────────────────────────────────┘
```

##### Sync (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('S') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

#### 4.4 응답 메시지

##### RowDescription (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('T') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int16      │ 필드 개수                              │
│ [          │ 각 필드에 대해:                        │
│   String   │   필드 이름                            │
│   Int32    │   테이블 OID (0 = 테이블 없음)         │
│   Int16    │   열 번호 (0 = 열 없음)                │
│   Int32    │   데이터 타입 OID                      │
│   Int16    │   데이터 타입 크기                     │
│   Int32    │   타입 수정자 (modifier)               │
│   Int16    │   형식 코드 (0=text, 1=binary)         │
│ ]          │                                        │
└─────────────────────────────────────────────────────┘
```

##### DataRow (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('D') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int16      │ 열 값 개수                             │
│ [          │ 각 열에 대해:                          │
│   Int32    │   값 길이 (-1 = NULL)                  │
│   Byte[]   │   열 값 (길이가 -1이면 없음)           │
│ ]          │                                        │
└─────────────────────────────────────────────────────┘
```

##### CommandComplete (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('C') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 명령 태그                              │
└─────────────────────────────────────────────────────┘
```

명령 태그 형식:
- `SELECT rows` - 예: "SELECT 5"
- `INSERT oid rows` - 예: "INSERT 0 3"
- `UPDATE rows` - 예: "UPDATE 10"
- `DELETE rows` - 예: "DELETE 5"

##### ReadyForQuery (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('Z') │ 메시지 타입                            │
│ Int32(5)   │ 메시지 길이                            │
│ Byte1      │ 트랜잭션 상태:                         │
│            │   'I' = Idle (유휴)                    │
│            │   'T' = Transaction (트랜잭션 중)      │
│            │   'E' = Error (실패한 트랜잭션)        │
└─────────────────────────────────────────────────────┘
```

##### BackendKeyData (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('K') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int32      │ Backend 프로세스 ID                    │
│ Byte[n]    │ 비밀 키 (메시지 끝까지, 프로토콜 3.0은 │
│            │ 4바이트 고정, 3.2+는 최대 256바이트)   │
└─────────────────────────────────────────────────────┘
```

##### ParameterStatus (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('S') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 파라미터 이름                          │
│ String     │ 파라미터 값                            │
└─────────────────────────────────────────────────────┘
```

보고되는 주요 파라미터:
- `server_version`
- `client_encoding`
- `server_encoding`
- `DateStyle`
- `TimeZone`
- `integer_datetimes`
- `standard_conforming_strings`

#### 4.5 COPY 메시지

##### CopyInResponse (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('G') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int8       │ 전체 형식 (0=text, 1=binary)           │
│ Int16      │ 열 개수                                │
│ Int16[]    │ 각 열의 형식 코드                      │
└─────────────────────────────────────────────────────┘
```

##### CopyOutResponse (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('H') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int8       │ 전체 형식 (0=text, 1=binary)           │
│ Int16      │ 열 개수                                │
│ Int16[]    │ 각 열의 형식 코드                      │
└─────────────────────────────────────────────────────┘
```

##### CopyData (F & B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('d') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte[]     │ 데이터                                 │
└─────────────────────────────────────────────────────┘
```

##### CopyDone (F & B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('c') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

##### CopyFail (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('f') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 오류 메시지                            │
└─────────────────────────────────────────────────────┘
```

---

### 5. 오류 및 경고 메시지 (Error and Notice Messages)

#### 5.1 ErrorResponse (B)

```
┌─────────────────────────────────────────────────────┐
│ Byte1('E') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ [          │ 필드 반복:                             │
│   Byte1    │   필드 타입 코드                       │
│   String   │   필드 값                              │
│ ]          │                                        │
│ Byte1(0)   │ 종료자                                 │
└─────────────────────────────────────────────────────┘
```

#### 5.2 NoticeResponse (B)

ErrorResponse와 동일한 구조이지만 메시지 타입이 'N':

```
┌─────────────────────────────────────────────────────┐
│ Byte1('N') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ [필드들]   │ ErrorResponse와 동일                   │
│ Byte1(0)   │ 종료자                                 │
└─────────────────────────────────────────────────────┘
```

#### 5.3 필드 타입 코드

- `S` (Severity): 심각도(ERROR, FATAL, PANIC, WARNING, NOTICE 등) - 지역화됨, 항상 존재
- `V` (Severity, Non-localized): 심각도 - 지역화되지 않음(9.6+)
- `C` (Code): SQLSTATE 오류 코드(5자리), 항상 존재
- `M` (Message): 주요 오류 메시지, 항상 존재
- `D` (Detail): 상세 정보
- `H` (Hint): 해결 방법 제안
- `P` (Position): 쿼리 문자열 내 오류 위치(1부터 시작)
- `p` (Internal Position): 내부 명령어 내 오류 위치
- `q` (Internal Query): 실패한 내부 명령어
- `W` (Where): 오류 발생 컨텍스트(콜 스택)
- `s` (Schema Name): 관련 스키마 이름
- `t` (Table Name): 관련 테이블 이름
- `c` (Column Name): 관련 열 이름
- `d` (Data Type Name): 관련 데이터 타입 이름
- `n` (Constraint Name): 관련 제약 조건 이름
- `F` (File): 소스 파일명
- `L` (Line): 소스 라인 번호
- `R` (Routine): 소스 함수명

#### 5.4 SQLSTATE 오류 코드 예시

- `00000`: 성공
- `23505`: unique_violation(고유 제약 조건 위반)
- `42601`: syntax_error(구문 오류)
- `42P01`: undefined_table(테이블 없음)
- `42703`: undefined_column(열 없음)
- `22012`: division_by_zero(0으로 나눔)
- `40001`: serialization_failure(직렬화 실패)

```python
# Python 예시: 오류 메시지 파싱
def parse_error_response(data):
    fields = {}
    i = 0
    while data[i] != 0:
        field_type = chr(data[i])
        i += 1
        # null 종료 문자열 읽기
        end = data.index(0, i)
        value = data[i:end].decode('utf-8')
        fields[field_type] = value
        i = end + 1
    return fields

# 결과 예시:
# {
#     'S': 'ERROR',
#     'V': 'ERROR',
#     'C': '42P01',
#     'M': 'relation "nonexistent" does not exist',
#     'P': '15',
#     'F': 'parse_relation.c',
#     'L': '1180',
#     'R': 'parserOpenTable'
# }
```

---

### 6. 확장 쿼리 프로토콜 (Extended Query Protocol)

#### 6.1 개요

확장 쿼리 프로토콜은 SQL 명령 실행을 여러 단계로 분리 → 더 세밀한 제어와 성능 향상 제공.

```
┌─────────────────────────────────────────────────────────────┐
│              Extended Query Protocol 흐름                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Parse ──→ Bind ──→ Execute                                 │
│    ↓         ↓         ↓                                    │
│  준비된 문  포털     결과                                    │
│  생성      생성     반환                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 6.2 핵심 개념

##### Prepared Statement (준비된 문)
- 파싱 및 의미 분석이 완료된 쿼리
- 파라미터 값이 아직 바인딩되지 않은 상태
- 세션 내에서 재사용 가능

##### Portal (포털)
- 준비된 문에 파라미터 값이 결합된 객체
- 즉시 실행 가능한 상태
- SELECT의 경우 열린 커서에 해당

#### 6.3 상세 흐름

```
Frontend                              Backend
   │                                     │
   │─────── Parse ───────────────────────>│
   │    (쿼리, 파라미터 타입)             │
   │<────── ParseComplete ───────────────│
   │                                     │
   │─────── Bind ────────────────────────>│
   │    (준비된 문 → 포털, 파라미터 값)   │
   │<────── BindComplete ────────────────│
   │                                     │
   │─────── Describe ────────────────────>│
   │    (포털 정보 요청)                  │
   │<────── RowDescription ──────────────│
   │                                     │
   │─────── Execute ─────────────────────>│
   │    (포털 실행)                       │
   │<────── DataRow (여러 개) ───────────│
   │<────── CommandComplete ─────────────│
   │                                     │
   │─────── Sync ────────────────────────>│
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

#### 6.4 예시: 파라미터화된 쿼리

```python
# Python/psycopg2 스타일의 쿼리
cursor.execute("SELECT * FROM users WHERE id = %s", (42,))
```

프로토콜 레벨:

```
1. Parse 메시지:
   - Statement: ""  (unnamed)
   - Query: "SELECT * FROM users WHERE id = $1"
   - Parameter Types: [INT4 OID]

2. Bind 메시지:
   - Portal: ""  (unnamed)
   - Statement: ""
   - Parameter Values: ["42"]
   - Result Formats: [0]  (text)

3. Execute 메시지:
   - Portal: ""
   - Max Rows: 0  (unlimited)

4. Sync 메시지
```

#### 6.5 명명된 vs 비명명 객체

- 수명: 명명된(Named)은 세션 종료까지 · 비명명(Unnamed)은 다음 Parse/Bind까지
- 용도: 명명된은 다중 사용 · 비명명은 1회성 실행
- 이름: 명명된은 비어있지 않은 문자열 · 비명명은 빈 문자열("")

#### 6.6 부분 실행 (Partial Execution)

Execute 메시지의 `max_rows` 파라미터를 사용하여 부분적으로 결과 조회 가능:

```
Frontend: Execute (max_rows = 10)
Backend:  DataRow × 10
Backend:  PortalSuspended

Frontend: Execute (max_rows = 10)
Backend:  DataRow × 5
Backend:  CommandComplete ("SELECT 15")

Frontend: Sync
Backend:  ReadyForQuery
```

#### 6.7 파이프라이닝 (Pipelining)

여러 쿼리를 응답을 기다리지 않고 연속 전송:

```
Frontend: Parse (Query 1)
Frontend: Bind (Query 1)
Frontend: Execute (Query 1)
Frontend: Parse (Query 2)
Frontend: Bind (Query 2)
Frontend: Execute (Query 2)
Frontend: Sync

Backend:  ParseComplete
Backend:  BindComplete
Backend:  DataRow...
Backend:  CommandComplete
Backend:  ParseComplete
Backend:  BindComplete
Backend:  DataRow...
Backend:  CommandComplete
Backend:  ReadyForQuery
```

오류 처리:
- 오류 발생 시 다음 Sync까지 이후 메시지를 모두 무시
- Sync 이후 ReadyForQuery로 동기화

#### 6.8 Close 메시지

##### Close (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('C') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte1      │ 'S' = Statement, 'P' = Portal          │
│ String     │ 이름                                   │
└─────────────────────────────────────────────────────┘
```

##### CloseComplete (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('3') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

---

### 7. SASL 인증 (SASL Authentication)

#### 7.1 SCRAM-SHA-256

PostgreSQL 10부터 지원되는 가장 안전한 비밀번호 기반 인증 방식.

```
Frontend                              Backend
   │                                     │
   │<────── AuthenticationSASL ──────────│
   │    (메커니즘: SCRAM-SHA-256)        │
   │                                     │
   │─────── SASLInitialResponse ─────────>│
   │    (client-first-message)           │
   │                                     │
   │<────── AuthenticationSASLContinue ──│
   │    (server-first-message)           │
   │                                     │
   │─────── SASLResponse ────────────────>│
   │    (client-final-message)           │
   │                                     │
   │<────── AuthenticationSASLFinal ─────│
   │    (server-final-message)           │
   │                                     │
   │<────── AuthenticationOk ────────────│
   │                                     │
```

#### 7.2 SASLInitialResponse (F)

```
┌─────────────────────────────────────────────────────┐
│ Byte1('p') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ SASL 메커니즘 이름                     │
│ Int32      │ 초기 응답 길이 (-1 = 없음)             │
│ Byte[]     │ 초기 응답 데이터                       │
└─────────────────────────────────────────────────────┘
```

#### 7.3 SCRAM 메시지 예시

```
# client-first-message
n,,n=user,r=rOprNGfwEbeRWgbNEkqO

# server-first-message
r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa,s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096

# client-final-message
c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa,p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=

# server-final-message
v=6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=
```

---

### 8. 복제 프로토콜 (Replication Protocol)

#### 8.1 스트리밍 복제 (Streaming Replication)

물리적 복제를 위한 프로토콜.

연결 설정:
```sql
-- StartupMessage에 replication=true 포함
replication = true
```

주요 명령:
- `IDENTIFY_SYSTEM`: 시스템 정보 요청
- `START_REPLICATION`: WAL 스트리밍 시작

#### 8.2 논리적 복제 (Logical Replication)

논리적 변경 스트림을 위한 프로토콜.

```sql
-- 논리적 복제 시작
START_REPLICATION SLOT myslot LOGICAL 0/0
    (proto_version '1', publication_names 'mypub');
```

#### 8.3 복제 메시지

##### XLogData (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('w') │ 메시지 타입                            │
│ Int64      │ WAL 시작 위치                          │
│ Int64      │ 현재 WAL 끝 위치                       │
│ Int64      │ 서버 시간                              │
│ Byte[]     │ WAL 데이터                             │
└─────────────────────────────────────────────────────┘
```

##### Primary Keepalive (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('k') │ 메시지 타입                            │
│ Int64      │ 현재 WAL 끝 위치                       │
│ Int64      │ 서버 시간                              │
│ Byte1      │ 응답 필요 여부                         │
└─────────────────────────────────────────────────────┘
```

##### Standby Status Update (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('r') │ 메시지 타입                            │
│ Int64      │ 마지막으로 받은 WAL                    │
│ Int64      │ 마지막으로 디스크에 플러시한 WAL       │
│ Int64      │ 마지막으로 적용한 WAL                  │
│ Int64      │ 클라이언트 시간                        │
│ Byte1      │ 즉시 응답 요청                         │
└─────────────────────────────────────────────────────┘
```

---

### 9. SSL/TLS 암호화

#### 9.1 SSL 연결 설정

```
Frontend                              Backend
   │                                     │
   │─────── SSLRequest ──────────────────>│
   │    (8바이트 메시지)                  │
   │                                     │
   │<────── 'S' 또는 'N' ────────────────│
   │    (1바이트 응답)                    │
   │                                     │
   │  [SSL 핸드셰이크 - 'S'인 경우]       │
   │                                     │
   │─────── StartupMessage ──────────────>│
   │    (암호화된 채널로)                 │
   │                                     │
```

##### SSLRequest
```
┌─────────────────────────────────────────────────────┐
│ Int32(8)       │ 메시지 길이                        │
│ Int32(80877103)│ SSL 요청 코드                      │
└─────────────────────────────────────────────────────┘
```

#### 9.2 GSSAPI 암호화

```
Frontend                              Backend
   │                                     │
   │─────── GSSENCRequest ───────────────>│
   │                                     │
   │<────── 'G' 또는 'N' ────────────────│
   │                                     │
   │  [GSSAPI 핸드셰이크 - 'G'인 경우]    │
   │                                     │
```

---

### 10. 프로토콜 구현 예제

#### 10.1 간단한 연결 예제 (Python)

```python
import socket
import struct
import hashlib

class PGConnection:
    def __init__(self, host, port, user, database, password):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))
        self.user = user
        self.database = database
        self.password = password

    def startup(self):
        """StartupMessage 전송"""
        # 파라미터 구성
        params = (
            f"user\x00{self.user}\x00"
            f"database\x00{self.database}\x00"
            "\x00"  # 종료자
        ).encode()

        # 프로토콜 버전 3.0
        version = struct.pack('>I', 196608)
        length = struct.pack('>I', 4 + len(version) + len(params))

        self.sock.sendall(length + version + params)

    def read_message(self):
        """메시지 읽기"""
        msg_type = self.sock.recv(1)
        if not msg_type:
            return None, None

        length_bytes = self.sock.recv(4)
        length = struct.unpack('>I', length_bytes)[0]

        content = self.sock.recv(length - 4)
        return msg_type.decode(), content

    def handle_auth(self, content):
        """인증 처리"""
        auth_type = struct.unpack('>I', content[:4])[0]

        if auth_type == 0:
            print("인증 성공!")
            return True
        elif auth_type == 3:
            # 평문 비밀번호
            self.send_password(self.password)
        elif auth_type == 5:
            # MD5 비밀번호
            salt = content[4:8]
            self.send_md5_password(salt)
        return False

    def send_password(self, password):
        """PasswordMessage 전송"""
        msg = password.encode() + b'\x00'
        length = struct.pack('>I', 4 + len(msg))
        self.sock.sendall(b'p' + length + msg)

    def send_md5_password(self, salt):
        """MD5 암호화 비밀번호 전송"""
        # concat('md5', md5(concat(md5(password+user), salt)))
        step1 = hashlib.md5(
            (self.password + self.user).encode()
        ).hexdigest()
        step2 = hashlib.md5(
            (step1.encode() + salt)
        ).hexdigest()
        self.send_password('md5' + step2)

    def query(self, sql):
        """단순 쿼리 실행"""
        msg = sql.encode() + b'\x00'
        length = struct.pack('>I', 4 + len(msg))
        self.sock.sendall(b'Q' + length + msg)

        results = []
        while True:
            msg_type, content = self.read_message()

            if msg_type == 'T':
                # RowDescription
                columns = self.parse_row_description(content)
                print(f"열: {columns}")
            elif msg_type == 'D':
                # DataRow
                row = self.parse_data_row(content)
                results.append(row)
            elif msg_type == 'C':
                # CommandComplete
                tag = content[:-1].decode()
                print(f"명령 완료: {tag}")
            elif msg_type == 'Z':
                # ReadyForQuery
                status = chr(content[0])
                print(f"준비 완료 (상태: {status})")
                break
            elif msg_type == 'E':
                # ErrorResponse
                error = self.parse_error(content)
                print(f"오류: {error}")

        return results

    def parse_row_description(self, content):
        """RowDescription 파싱"""
        num_fields = struct.unpack('>H', content[:2])[0]
        columns = []
        offset = 2

        for _ in range(num_fields):
            # null 종료 문자열 찾기
            end = content.index(0, offset)
            name = content[offset:end].decode()
            columns.append(name)
            # 나머지 필드 건너뛰기 (18바이트)
            offset = end + 1 + 18

        return columns

    def parse_data_row(self, content):
        """DataRow 파싱"""
        num_columns = struct.unpack('>H', content[:2])[0]
        values = []
        offset = 2

        for _ in range(num_columns):
            length = struct.unpack('>i', content[offset:offset+4])[0]
            offset += 4

            if length == -1:
                values.append(None)
            else:
                value = content[offset:offset+length].decode()
                values.append(value)
                offset += length

        return values

    def parse_error(self, content):
        """ErrorResponse 파싱"""
        fields = {}
        offset = 0

        while content[offset] != 0:
            field_type = chr(content[offset])
            offset += 1
            end = content.index(0, offset)
            value = content[offset:end].decode()
            fields[field_type] = value
            offset = end + 1

        return fields

    def close(self):
        """연결 종료"""
        self.sock.sendall(b'X\x00\x00\x00\x04')
        self.sock.close()


# 사용 예시
if __name__ == '__main__':
    conn = PGConnection(
        host='localhost',
        port=5432,
        user='postgres',
        database='testdb',
        password='password'
    )

    conn.startup()

    # 인증 처리
    while True:
        msg_type, content = conn.read_message()
        if msg_type == 'R':
            if conn.handle_auth(content):
                break
        elif msg_type == 'S':
            # ParameterStatus
            pass
        elif msg_type == 'K':
            # BackendKeyData
            pass
        elif msg_type == 'Z':
            # ReadyForQuery
            break

    # 쿼리 실행
    results = conn.query("SELECT id, name FROM users LIMIT 5;")
    for row in results:
        print(row)

    conn.close()
```

#### 10.2 확장 쿼리 예제

```python
def extended_query(self, sql, params):
    """확장 쿼리 프로토콜 사용"""

    # 1. Parse 메시지
    stmt_name = b'\x00'  # unnamed
    query = sql.encode() + b'\x00'
    num_params = struct.pack('>H', 0)  # 타입 추론

    parse_msg = stmt_name + query + num_params
    length = struct.pack('>I', 4 + len(parse_msg))
    self.sock.sendall(b'P' + length + parse_msg)

    # 2. Bind 메시지
    portal_name = b'\x00'  # unnamed
    stmt_name = b'\x00'

    # 파라미터 형식 (모두 텍스트)
    param_formats = struct.pack('>H', 0)

    # 파라미터 값
    num_params = struct.pack('>H', len(params))
    param_values = b''
    for p in params:
        if p is None:
            param_values += struct.pack('>i', -1)
        else:
            val = str(p).encode()
            param_values += struct.pack('>i', len(val)) + val

    # 결과 형식 (모두 텍스트)
    result_formats = struct.pack('>H', 0)

    bind_msg = (portal_name + stmt_name + param_formats +
                num_params + param_values + result_formats)
    length = struct.pack('>I', 4 + len(bind_msg))
    self.sock.sendall(b'B' + length + bind_msg)

    # 3. Describe 메시지 (포털)
    describe_msg = b'P' + b'\x00'
    length = struct.pack('>I', 4 + len(describe_msg))
    self.sock.sendall(b'D' + length + describe_msg)

    # 4. Execute 메시지
    portal_name = b'\x00'
    max_rows = struct.pack('>I', 0)  # 무제한
    execute_msg = portal_name + max_rows
    length = struct.pack('>I', 4 + len(execute_msg))
    self.sock.sendall(b'E' + length + execute_msg)

    # 5. Sync 메시지
    self.sock.sendall(b'S\x00\x00\x00\x04')

    # 응답 처리
    return self._process_response()

# 사용 예시
results = conn.extended_query(
    "SELECT * FROM users WHERE id = $1 AND status = $2",
    [42, 'active']
)
```

---

### 11. 문제 해결 (Troubleshooting)

#### 11.1 일반적인 오류

- 28000 (invalid_authorization_specification): 인증 실패 → 사용자명/비밀번호 확인
- 28P01 (invalid_password): 잘못된 비밀번호 → 비밀번호 확인
- 3D000 (invalid_catalog_name): 데이터베이스 없음 → 데이터베이스 이름 확인
- 08006 (connection_failure): 연결 실패 → 호스트/포트 확인
- 57P03 (cannot_connect_now): 서버 시작 중 → 잠시 후 재시도

#### 11.2 프로토콜 디버깅

```bash
# PostgreSQL 로그 설정
log_connections = on
log_disconnections = on
log_statement = 'all'

# 네트워크 트래픽 캡처
tcpdump -i lo0 -w pg_traffic.pcap port 5432

# Wireshark에서 분석
# 필터: pgsql
```

#### 11.3 성능 최적화

1. Prepared Statements 사용: 반복 쿼리의 파싱 오버헤드 제거
2. 파이프라이닝: 네트워크 왕복 최소화
3. Binary 형식: 큰 데이터의 직렬화/역직렬화 비용 감소
4. 연결 풀링: pgbouncer, pgpool-II 사용

---

### 요약

PostgreSQL Frontend/Backend 프로토콜의 핵심 요소:

1. 메시지 기반 통신: 모든 상호작용은 정형화된 메시지를 통해 이루어짐
2. 두 단계 프로토콜: 시작 단계(인증)와 정상 운영 단계
3. 두 가지 쿼리 모드:
   - 단순 쿼리: 간단하고 직관적
   - 확장 쿼리: 파라미터화, 준비된 문, 성능 최적화
4. 강력한 오류 처리: 상세한 오류 정보와 SQLSTATE 코드
5. 보안: MD5, SCRAM-SHA-256, SSL/TLS, GSSAPI 지원
6. 확장성: 복제 프로토콜, COPY 작업, 비동기 알림

프로토콜을 이해하면 PostgreSQL 클라이언트 라이브러리 개발, 성능 최적화, 장애 분석에 도움됨.

---

## PostgreSQL 소스 코드 (Source Code)

### 개요

PostgreSQL은 오픈 소스 관계형 데이터베이스 관리 시스템 → 소스 코드 기반이 잘 구조화됨.

---

### 1. 소스 코드 디렉토리 구조 (Directory Structure)

#### 1.1 최상위 `src` 디렉토리

PostgreSQL 소스 코드의 핵심은 `src` 디렉토리 → 다음과 같은 주요 하위 디렉토리로 구성:

- `backend`: 핵심 데이터베이스 엔진 및 서버 기능
- `bin`: 명령줄 유틸리티 및 실행 파일
- `common`: 프론트엔드와 백엔드에서 공유하는 공통 코드
- `fe_utils`: 클라이언트 애플리케이션용 프론트엔드 유틸리티
- `include`: 헤더 파일 및 선언
- `interfaces`: 데이터베이스 인터페이스 구현 (예: libpq)
- `pl`: 절차적 언어 지원 (PL/pgSQL, PL/Perl 등)
- `port`: 플랫폼 특화 이식성 코드
- `test`: 테스트 유틸리티 및 테스트 코드
- `timezone`: 시간대 처리 및 데이터
- `tools`: 개발 및 유지보수 도구
- `tutorial`: 교육 자료 및 예제

#### 1.2 `src/backend` 디렉토리 상세

백엔드 디렉토리는 PostgreSQL 서버의 핵심을 구성 → 25개 이상의 하위 디렉토리를 포함:

- `access`: 데이터베이스 액세스 방법 (힙, 인덱스 등)
- `archive`: 아카이브 기능
- `backup`: 백업 작업
- `bootstrap`: 데이터베이스 초기화 절차
- `catalog`: 시스템 카탈로그 관리
- `commands`: SQL 명령어 처리
- `executor`: 쿼리 실행 엔진
- `foreign`: 외부 데이터 래퍼(FDW) 지원
- `jit`: JIT(Just-In-Time) 컴파일
- `lib`: 라이브러리 유틸리티
- `libpq`: PostgreSQL 클라이언트 라이브러리
- `main`: 주 서버 기능
- `nodes`: 파스 트리 노드 구조
- `optimizer`: 쿼리 최적화
- `parser`: SQL 파싱
- `partitioning`: 테이블 파티셔닝
- `port`: 플랫폼 특화 코드
- `postmaster`: 서버 프로세스 관리
- `regex`: 정규 표현식 지원
- `replication`: 복제 기능
- `rewrite`: 쿼리 재작성 규칙
- `snowball`: Snowball 스테머 지원
- `statistics`: 쿼리 통계
- `storage`: 데이터 저장소 관리
- `tcop`: 최상위 명령 처리
- `tsearch`: 전문 검색(Full-Text Search)
- `utils`: 유틸리티 함수

#### 1.3 프론트엔드와 백엔드 코드 분리

PostgreSQL에서 프론트엔드와 백엔드 코드는 명확히 분리됨. 프론트엔드 변경사항은 주로 `src/bin` 또는 `src/fe_utils` 디렉토리에 위치.

공유 코드의 경우 `#ifdef FRONTEND` 전처리기 지시문으로 프론트엔드와 백엔드 로직을 분리:

```c
#ifndef FRONTEND
static inline MemoryContext
MemoryContextSwitchTo(MemoryContext context)
{
    MemoryContext old = CurrentMemoryContext;
    CurrentMemoryContext = context;
    return old;
}
#endif   /* FRONTEND */
```

---

### 2. 코딩 규칙 (Coding Conventions)

#### 2.1 포맷팅 (Formatting)

PostgreSQL은 BSD 스타일의 코드 포맷팅을 따름:

- 탭 간격: 4열 탭 간격 사용 (탭을 공백으로 확장하지 않음)
- 들여쓰기: 논리적 들여쓰기 수준마다 탭 스탑 하나씩 추가
- 중괄호: 제어 블록(`if`, `while`, `switch` 등)의 중괄호는 별도 줄에 배치
- 줄 길이: 80열 너비 기준 가독성을 위해 줄 길이 제한

```c
/* 올바른 포맷팅 예시 */
if (condition)
{
    /* 코드 블록 */
    do_something();
}
else
{
    /* 다른 코드 블록 */
    do_something_else();
}
```

#### 2.2 주석 스타일 (Comment Style)

C++ 스타일 주석(`//`)은 사용하지 않음 → `pgindent`가 이를 `/* ... */` 형태로 변환함.

단일 줄 주석:
```c
/* 이것은 단일 줄 주석입니다 */
```

여러 줄 주석:
```c
/*
 * 주석 텍스트가 여기서 시작하고
 * 여기서 계속됩니다
 */
```

줄 바꿈 보존이 필요한 들여쓰기된 주석:
```c
/*----------
 * 주석 텍스트가 여기서 시작하고
 * 여기서 계속됩니다
 *----------
 */
```

#### 2.3 C 표준 (C Standard)

PostgreSQL 코드는 C99 표준 기능만을 사용해야 함.

금지된 C99 기능:
- 가변 길이 배열 (Variable Length Arrays)
- 선언과 코드의 혼합
- `//` 주석
- 유니버설 문자 이름

폴백(fallback)과 함께 허용되는 비-C99 기능:
- `_Static_assert()` - C11 기능; C99 호환 대체로 폴백
- `__builtin_constant_p` - GCC 확장; 사용 가능 여부 확인 후 폴백

#### 2.4 함수형 매크로와 인라인 함수

`static inline` 함수를 선호하는 경우:
- 매크로 형태로 작성하면 다중 평가 위험이 있을 때
- 매크로가 매우 길어질 때

```c
/* 인라인 함수 예시 */
static inline int
Max(int a, int b)
{
    return (a > b) ? a : b;
}
```

매크로가 필요하거나 더 적합한 경우:
- 다양한 타입의 표현식을 전달해야 할 때
- 표현식 다형성이 필요할 때

#### 2.5 시그널 핸들러 (Signal Handlers)

시그널 핸들러는 인터럽트 위험이 있으므로 매우 신중하게 작성해야 함:

- async-signal-safe 함수만 호출 가능 (POSIX 정의)
- `volatile sig_atomic_t` 변수에만 접근 가능
- `SetLatch()`는 PostgreSQL에서 signal-safe로 간주됨

권장 방식: 최소한의 작업만 수행 → 시그널 수신을 기록하고 래치로 외부 코드를 깨움:

```c
static void
handle_sighup(SIGNAL_ARGS)
{
    got_SIGHUP = true;
    SetLatch(MyLatch);
}
```

#### 2.6 함수 포인터 호출

단순 변수의 경우: `(*pointer)()` 표기법으로 명시적 역참조:
```c
(*emit_log_hook)(edata);
```

구조체 멤버의 경우: 추가 구두점 없이 직접 호출:
```c
paramInfo->paramFetch(paramInfo, paramId);
```

---

### 3. 서버 내 오류 보고 (Reporting Errors Within the Server)

#### 3.1 ereport 함수

서버 코드 내 오류, 경고, 로그 메시지는 `ereport` 또는 레거시 함수인 `elog`를 사용해 생성함.

필수 요소:
1. 심각도 레벨 (Severity Level): `DEBUG`부터 `PANIC`까지 (`src/include/utils/elog.h`에 정의)
2. 기본 메시지 텍스트 (Primary Message Text): 주요 오류 설명
3. 선택적 요소: 오류 식별자 코드 (SQLSTATE), 힌트, 세부 정보 등

#### 3.2 기본 사용법

간단한 예시:
```c
ereport(ERROR,
        errcode(ERRCODE_DIVISION_BY_ZERO),
        errmsg("division by zero"));
```

복잡한 예시:
```c
ereport(ERROR,
        errcode(ERRCODE_AMBIGUOUS_FUNCTION),
        errmsg("function %s is not unique",
               func_signature_string(funcname, nargs,
                                     NIL, actual_arg_types)),
        errhint("Unable to choose a best candidate function. "
                "You might need to add explicit typecasts."));
```

#### 3.3 심각도에 따른 동작

- ERROR 이상의 심각도: 현재 쿼리 실행을 중단하며 반환하지 않음
- ERROR 미만: 정상적으로 반환

#### 3.4 보조 함수 (Auxiliary Functions)

- `errcode(sqlerrcode)`: SQLSTATE 오류 식별자 코드 지정
- `errmsg(msg, ...)`: sprintf 스타일 형식 코드를 사용한 기본 오류 메시지; `%m`은 `strerror()` 지원
- `errmsg_internal(msg, ...)`: 내부 오류용 비번역 메시지
- `errmsg_plural(fmt_singular, fmt_plural, n, ...)`: 복수형 지원 메시지
- `errdetail(msg, ...)`: 선택적 추가 세부 정보
- `errdetail_internal(msg, ...)`: 비번역 세부 메시지
- `errdetail_log(msg, ...)`: 서버 로그에만 전송되는 세부 정보 (클라이언트 제외)
- `errhint(msg, ...)`: 문제 해결을 위한 제안
- `errcontext(msg, ...)`: 컨텍스트 정보 (콜백 함수에서 사용)
- `errposition(int)`: 쿼리 문자열에서 오류의 텍스트 위치
- `errtable(Relation)`: 오류와 릴레이션 이름/스키마 연결
- `errtablecol(Relation, attnum)`: 오류와 컬럼 연결
- `errtableconstraint(Relation, conname)`: 오류와 제약조건 연결
- `errdatatype(Oid)`: 오류와 데이터 타입 연결
- `errdomainconstraint(Oid, conname)`: 오류와 도메인 제약조건 연결
- `errcode_for_file_access()`: 파일 시스템 오류에 적합한 SQLSTATE 선택
- `errcode_for_socket_access()`: 소켓 오류에 적합한 SQLSTATE 선택
- `errhidestmt(bool)`: 로그에서 STATEMENT 부분 숨김
- `errhidecontext(bool)`: 로그에서 CONTEXT 부분 숨김

#### 3.5 기본 SQLSTATE 코드

`errcode()`가 생략된 경우:
- ERROR 이상: `ERRCODE_INTERNAL_ERROR`
- WARNING: `ERRCODE_WARNING`
- NOTICE 이하: `ERRCODE_SUCCESSFUL_COMPLETION`

#### 3.6 elog() - 레거시 함수

```c
elog(level, "format string", ...);
```

다음과 동일:
```c
ereport(level, errmsg_internal("format string", ...));
```

특성:
- SQLSTATE 코드는 항상 기본값 사용
- 메시지를 번역하지 않음
- 내부 오류 및 저수준 디버그 로깅에만 사용
- 표기가 간결 → "발생할 수 없는" 오류 체크에 주로 사용됨

---

### 4. 오류 메시지 스타일 가이드 (Error Message Style Guide)

#### 4.1 메시지 구조

- Primary (기본): 짧고, 사실적이며, 한 줄; 구현 세부사항 회피
- Detail (세부): 구현 세부사항, 시스템 호출, 기술 정보
- Hint (힌트): 문제 해결을 위한 제안

예시:
```
잘못된 방식:
IpcMemoryCreate: shmget(key=%d, size=%u, 0%o) failed: %m
(기본적으로 힌트인 긴 부록 포함)

올바른 방식:
Primary:    could not create shared memory segment: %m
Detail:     Failed syscall was shmget(key=%d, size=%u, 0%o).
Hint:       부록을 완전한 문장으로 작성.
```

#### 4.2 문법 및 구두점

- Primary: 대문자화 없음, 마침표 없음, 느낌표 없음
- Detail/Hint: 완전한 문장, 대문자화, 끝에 마침표, 문장 사이 공백 두 개
- Context: 대문자화 없음, 마침표 없음

#### 4.3 시제 사용

- 과거 시제 ("could not"): 나중에 성공할 수 있는 일시적 실패
- 현재 시제 ("cannot"): 영구적이거나 지속적인 조건

```c
could not open file "%s": %m     /* 다음에는 작동할 수 있음 */
cannot open file "%s"            /* 불가능한 작업 */
```

#### 4.4 피해야 할 단어

- `Unable`: 수동태에 가까움 → "cannot" 또는 "could not"
- `Bad`: 모호함 → 이유 명시 (예: "invalid format")
- `Illegal`: 잘못된 법률 용어 → "invalid" + 설명
- `Unknown`: 모호함 → "unrecognized" + 값 표시
- `Contractions`: 비격식적 → "can't" 대신 "cannot"
- `Non-negative`: 모호함 → "greater than zero" 또는 "≥ zero"

---

### 5. 빌드 시스템 (Build System)

#### 5.1 Meson 빌드 시스템

PostgreSQL 16 이상에서 Meson 빌드 시스템을 지원함. Meson은 Ninja를 기본 백엔드로 사용하는 현대적인 빌드 시스템임.

빠른 시작:
```bash
# 빌드 디렉토리 설정
meson setup build --prefix=/usr/local/pgsql

# 빌드
cd build
ninja

# 설치
su
ninja install

# 사용자 추가 및 데이터 디렉토리 생성
adduser postgres
mkdir -p /usr/local/pgsql/data
chown postgres /usr/local/pgsql/data

# 데이터베이스 초기화 및 시작
su - postgres
/usr/local/pgsql/bin/initdb -D /usr/local/pgsql/data
/usr/local/pgsql/bin/pg_ctl -D /usr/local/pgsql/data -l logfile start
```

#### 5.2 설치 절차

1. 구성:
```bash
# 기본 설정
meson setup build

# 사용자 정의 접두사
meson setup build --prefix=/home/user/pg-install

# 디버그 빌드
meson setup build --buildtype=debug

# OpenSSL 지원
meson setup build -Dssl=openssl

# 초기 설정 후 재구성
meson configure -Dcassert=true
```

2. 빌드:
```bash
ninja
# 자동으로 병렬 빌드 지원; -j 플래그로 오버라이드 가능
```

3. 회귀 테스트:
```bash
meson test
# 실행 중인 인스턴스에 대해:
meson test --setup running
```

4. 설치:
```bash
ninja install
# 또는 더 많은 옵션과 함께:
meson install
```

유지보수 명령:
```bash
ninja uninstall    # 설치 제거
ninja clean        # 빌드 산출물 제거
```

#### 5.3 구성 옵션

##### 설치 위치
- `--prefix=PREFIX` - 기본 설치 디렉토리 (기본값: `/usr/local/pgsql`)
- `--bindir=DIRECTORY` - 실행 파일 (기본값: `PREFIX/bin`)
- `--libdir=DIRECTORY` - 라이브러리 (기본값: `PREFIX/lib`)
- `--includedir=DIRECTORY` - 헤더 (기본값: `PREFIX/include`)
- `--datadir=DIRECTORY` - 데이터 파일 (기본값: `PREFIX/share`)
- `--sysconfdir=DIRECTORY` - 구성 파일 (기본값: `PREFIX/etc`)
- `--mandir=DIRECTORY` - Man 페이지 (기본값: `DATADIR/man`)
- `--localedir=DIRECTORY` - 로케일 데이터 (기본값: `DATADIR/locale`)

##### PostgreSQL 기능
- `-Dnls={auto|enabled|disabled}`: 네이티브 언어 지원
- `-Dplperl={auto|enabled|disabled}`: PL/Perl 언어
- `-Dplpython={auto|enabled|disabled}`: PL/Python 언어
- `-Dpltcl={auto|enabled|disabled}`: PL/Tcl 언어
- `-Dicu={auto|enabled|disabled}`: ICU 콜레이션 지원
- `-Dllvm={auto|enabled|disabled}`: LLVM JIT 컴파일
- `-Dlz4={auto|enabled|disabled}`: LZ4 압축
- `-Dzstd={auto|enabled|disabled}`: Zstandard 압축
- `-Dssl={auto|openssl}`: SSL/TLS 지원
- `-Dgssapi={auto|enabled|disabled}`: GSSAPI 인증
- `-Dldap={auto|enabled|disabled}`: LDAP 지원
- `-Dpam={auto|enabled|disabled}`: PAM 인증
- `-Dsystemd={auto|enabled|disabled}`: systemd 통합
- `-Dbonjour={auto|enabled|disabled}`: Bonjour 검색
- `-Duuid=LIBRARY`: UUID 지원 (`none|bsd|e2fs|ossp`)
- `-Dlibxml={auto|enabled|disabled}`: XML 지원
- `-Dlibxslt={auto|enabled|disabled}`: XSLT 변환

##### 서버 튜닝
- `-Dpgport=NUMBER`: 기본 포트 (기본값: 5432)
- `-Dblocksize=BLOCKSIZE`: 블록 크기 KB (기본값: 8)
- `-Dwal_blocksize=BLOCKSIZE`: WAL 블록 크기 KB (기본값: 8)
- `-Dsegsize=SEGSIZE`: 세그먼트 크기 GB (기본값: 1)

##### 개발자 옵션
- `--buildtype=BUILDTYPE`: 빌드 타입 (`plain|debug|debugoptimized|release`)
- `--debug`: 디버깅 심볼 포함
- `--optimization=LEVEL`: 최적화 레벨 (0,g,1,2,3,s)
- `-Dcassert={true|false}`: 어서션 체크
- `-Ddtrace={auto|enabled|disabled}`: DTrace 지원
- `-Dtap_tests={auto|enabled|disabled}`: TAP 테스트 도구

#### 5.4 Meson 빌드 타겟

코드 타겟:
```bash
ninja all        # 문서를 제외한 모든 것
ninja backend    # 백엔드와 모듈
ninja bin        # 프론트엔드 바이너리
ninja contrib    # Contrib 모듈
ninja pl         # 절차적 언어
```

문서 타겟:
```bash
ninja html       # 멀티페이지 HTML
ninja man        # Man 페이지
ninja docs       # HTML과 man 페이지
ninja alldocs    # 모든 형식
```

설치 타겟:
```bash
ninja install           # 문서 제외
ninja install-docs      # 문서만
ninja install-world     # 모든 것
ninja install-quiet     # 조용한 설치
ninja uninstall         # 파일 제거
```

기타 타겟:
```bash
ninja clean    # 빌드 산출물 제거
ninja test     # 모든 테스트 실행
ninja world    # 문서 포함 모든 것
```

#### 5.5 Autoconf 빌드 시스템 (레거시)

PostgreSQL은 여전히 전통적인 GNU Autoconf 기반 빌드 시스템도 지원함:

```bash
# 구성
./configure --prefix=/usr/local/pgsql

# 빌드
make

# 테스트
make check

# 설치
make install
```

주요 사항:
- 생성된 `configure` 대신 `configure.in`을 직접 편집
- 변경 후 `autoconf` 실행
- `make distclean`으로 모든 파생 파일 제거
- 헤더 의존성 자동 추적을 위해 `--enable-depend` 플래그 사용

---

### 6. 개발 도구 (Development Tools)

#### 6.1 pgindent

`pgindent`는 PostgreSQL 코딩 표준에 맞게 코드를 자동으로 재포맷팅하는 도구임.

위치: `src/tools/pgindent`

사용 목적:
- 코드 스타일 일관성 유지
- 패치 제출 전 코드 정리
- 릴리스 전 자동 포맷팅

#### 6.2 에디터 설정

`src/tools/editors` 디렉토리에는 PostgreSQL 코딩 표준을 준수하는 데 도움이 되는 샘플 에디터 설정이 있음:
- Emacs
- XEmacs
- Vim

#### 6.3 기타 개발 도구

- `make_ctags` / `make_etags`: 태그 파일 생성
- `pginclude`: 인클루드 파일 관리 스크립트
- `find_static`: 정적 분석 도구
- `find_typedef`: typedef 찾기 도구
- `find_badmacros`: 잘못된 매크로 검출

#### 6.4 OID 관리 도구

`src/include/catalog` 디렉토리에는 OID 관리를 위한 도구가 있음:
- `unused_oids` - 사용 가능한 OID 할당 식별
- `duplicate_oids` - OID 충돌 감지

---

### 7. 파서 컴포넌트 (Parser Components)

PostgreSQL 파서는 `src/backend/parser` 디렉토리에 위치함:

- `scan.l`: SQL을 토큰화하는 렉서
- `gram.y`: BNF 표기법의 문법 정의

이들은 flex와 bison 도구에 의해 생성됨.

---

### 8. 회귀 테스트 (Regression Testing)

PostgreSQL은 SQL 구현을 검증하고 시스템 발전에 따른 호환성을 보장하는 내장 회귀 테스트 프레임워크를 제공함.

```bash
# Meson으로 테스트 실행
meson test

# make로 테스트 실행
make check

# 병렬 테스트
make check PROVE_FLAGS=-j4
```

테스트 디렉토리: `src/test`

---

### 9. 설치된 디렉토리 구조

PostgreSQL을 빌드하고 설치한 후의 디렉토리 구조:

- `bin`: PostgreSQL 실행 파일 (psql, initdb, pg_ctl, 서버 바이너리)
- `include`: 확장이나 애플리케이션 컴파일에 필요한 C 헤더 파일
- `lib`: PostgreSQL과 클라이언트 애플리케이션이 사용하는 공유 라이브러리
- `share`: 시간대 데이터, 로케일 데이터, SQL 스크립트 등

---

### 10. 예제 코드

#### 10.1 간단한 오류 보고

```c
#include "postgres.h"
#include "utils/elog.h"

void
example_function(int value)
{
    if (value < 0)
    {
        ereport(ERROR,
                errcode(ERRCODE_INVALID_PARAMETER_VALUE),
                errmsg("value must be non-negative"),
                errdetail("Received value: %d", value),
                errhint("Provide a value greater than or equal to zero."));
    }

    /* 정상 처리 */
    elog(DEBUG1, "processing value: %d", value);
}
```

#### 10.2 파일 접근 오류 처리

```c
#include "postgres.h"
#include <fcntl.h>

void
open_data_file(const char *filename)
{
    int fd;

    fd = open(filename, O_RDONLY);
    if (fd < 0)
    {
        ereport(ERROR,
                errcode_for_file_access(),
                errmsg("could not open file \"%s\": %m", filename));
    }

    /* 파일 처리... */
    close(fd);
}
```

#### 10.3 시그널 핸들러 예시

```c
#include "postgres.h"
#include "miscadmin.h"
#include "storage/latch.h"

static volatile sig_atomic_t got_SIGHUP = false;
static volatile sig_atomic_t got_SIGTERM = false;

static void
handle_sighup(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_SIGHUP = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

static void
handle_sigterm(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_SIGTERM = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

void
setup_signal_handlers(void)
{
    pqsignal(SIGHUP, handle_sighup);
    pqsignal(SIGTERM, handle_sigterm);
}
```

#### 10.4 메모리 컨텍스트 사용

```c
#include "postgres.h"
#include "utils/memutils.h"

void
example_memory_usage(void)
{
    MemoryContext oldcontext;
    MemoryContext mycontext;
    char *data;

    /* 새 메모리 컨텍스트 생성 */
    mycontext = AllocSetContextCreate(CurrentMemoryContext,
                                      "MyContext",
                                      ALLOCSET_DEFAULT_SIZES);

    /* 새 컨텍스트로 전환 */
    oldcontext = MemoryContextSwitchTo(mycontext);

    /* 메모리 할당 */
    data = palloc(1024);
    /* 데이터 처리... */

    /* 원래 컨텍스트로 복원 */
    MemoryContextSwitchTo(oldcontext);

    /* 컨텍스트와 모든 할당된 메모리 해제 */
    MemoryContextDelete(mycontext);
}
```

---

### 11. 참고 자료

- [PostgreSQL 공식 문서 - Internals](https://www.postgresql.org/docs/current/internals.html)
- [PostgreSQL Developer FAQ](https://wiki.postgresql.org/wiki/Developer_FAQ)
- [PostgreSQL Source Code Doxygen](https://doxygen.postgresql.org/)
- [PostgreSQL Meson 빌드 문서](https://www.postgresql.org/docs/current/install-meson.html)
- [PostgreSQL 오류 보고 문서](https://www.postgresql.org/docs/current/error-message-reporting.html)

---

### 요약

PostgreSQL 소스 코드는 체계적으로 구성된 디렉토리 구조를 가지며, 명확한 코딩 규칙을 따름. 주요 포인트:

1. 디렉토리 구조: `src` 디렉토리 아래에 backend, bin, include 등의 핵심 디렉토리가 있음
2. 코딩 규칙: BSD 스타일 포맷팅, C99 표준 준수, 특정 주석 스타일 사용
3. 오류 보고: `ereport`/`elog` 함수를 통한 일관된 오류 메시지 생성
4. 빌드 시스템: Meson (현대적) 및 Autoconf (레거시) 지원
5. 개발 도구: pgindent, 에디터 설정, 다양한 분석 도구 제공

---

## Chapter 57. 네이티브 언어 지원 (Native Language Support)

### 목차
1. [개요](#개요)
2. [번역자 가이드](#번역자-가이드-for-the-translator)
   - [요구사항](#요구사항-requirements)
   - [개념](#개념-concepts)
   - [메시지 카탈로그 생성 및 유지](#메시지-카탈로그-생성-및-유지-creating-and-maintaining-message-catalogs)
   - [PO 파일 편집](#po-파일-편집-editing-the-po-files)
3. [프로그래머 가이드](#프로그래머-가이드-for-the-programmer)
   - [메커니즘](#메커니즘-mechanics)
   - [메시지 작성 가이드라인](#메시지-작성-가이드라인-message-writing-guidelines)

---

### 개요

PostgreSQL은 네이티브 언어 지원(Native Language Support, NLS)을 통해 사용자에게 친숙한 언어로 메시지 제공 → 오류 메시지·경고·정보 메시지 등이 사용자의 모국어로 표시 가능.

PostgreSQL의 NLS는 GNU gettext 라이브러리 기반으로 구현 → 표준화된 접근 방식으로 다양한 언어로의 번역 가능.

#### NLS의 주요 특징

- gettext 기반: 널리 사용되는 GNU gettext 국제화 프레임워크 사용
- 메시지 카탈로그: PO(Portable Object) 및 MO(Machine Object) 파일 형식 지원
- 복수형 처리: 언어별 복수형 규칙 지원
- 동적 번역: 런타임에 적절한 번역 선택

---

### 번역자 가이드 (For the Translator)

#### 요구사항 (Requirements)


##### 기본 요구사항

- 텍스트 편집기
  - 설명: PO 파일 편집용
  - 필수 여부: 필수
- `msgfmt`
  - 설명: MO 파일 생성
  - 필수 여부: 필수
- `libintl`
  - 설명: gettext 라이브러리
  - 필수 여부: 필수

##### 새로운 번역 시작 또는 병합 시 추가 요구사항

- `xgettext`
  - 설명: 소스에서 메시지 추출
  - 버전 요구사항: GNU Gettext 0.10.36+
- `msgmerge`
  - 설명: PO 파일 병합
  - 버전 요구사항: GNU Gettext 0.10.36+

##### PostgreSQL 소스 빌드 요구사항

```bash
# NLS 지원을 활성화하여 PostgreSQL 빌드
./configure --enable-nls
make
```

#### 개념 (Concepts)

##### 메시지 카탈로그 파일 형식

NLS는 두 가지 파일 형식 사용:

- PO (Portable Object)
  - 확장자: `.po`
  - 용도: 원본-번역 쌍 저장
  - 특징: 텍스트 형식·번역자가 직접 편집
- MO (Machine Object)
  - 확장자: `.mo`
  - 용도: 런타임에 사용
  - 특징: 바이너리 형식·자동 생성

##### 파일 명명 규칙

```
progname.pot     # 템플릿 파일 (번역되지 않은 원본)
fr.po            # 프랑스어 PO 파일
de.po            # 독일어 PO 파일
ja.po            # 일본어 PO 파일
pt_BR.po         # 브라질 포르투갈어 PO 파일
ko.po            # 한국어 PO 파일
```

##### 언어 코드 규칙

- ISO 639-1 두 글자 코드 (소문자): `fr`, `de`, `ja`, `ko`
- 지역 변형 포함 시: `pt_BR` (브라질 포르투갈어), `zh_CN` (중국어 간체)

#### 메시지 카탈로그 생성 및 유지 (Creating and Maintaining Message Catalogs)

##### 새로운 번역 시작하기

1단계: 프로그램 디렉토리 확인

해당 프로그램 디렉토리에 `nls.mk` 파일이 있으면 번역을 지원하는 프로그램임.

```bash
# 예: psql 디렉토리 확인
ls src/bin/psql/nls.mk
```

2단계: 기본 카탈로그(POT) 생성

```bash
cd src/bin/psql
make init-po
```

`progname.pot` 템플릿 파일 생성됨.

3단계: 언어별 PO 파일 생성

```bash
# 한국어 번역 파일 생성
cp psql.pot ko.po
```

4단계: 언어 등록

`po/LINGUAS` 파일에 언어 코드 추가:

```
# po/LINGUAS
de fr ja ko pt_BR zh_CN
```

##### 기존 번역 업데이트

프로그램 소스가 변경되면 번역 업데이트 필요:

```bash
# 메시지 병합
make update-po
```

이 명령 실행 시:
1. 새 POT 파일 생성
2. 기존 PO 파일과 병합
3. 불확실한 메시지를 "fuzzy"로 표시
4. 결과를 `.po.new` 확장자로 저장

#### PO 파일 편집 (Editing the PO Files)

##### PO 파일 기본 구조

```po
# 번역자 주석
#. 자동 추출된 주석 (소스 코드에서)
#: filename.c:1023
#, c-format
msgid "Cannot open file \"%s\": %m"
msgstr "파일 \"%s\"을(를) 열 수 없습니다: %m"

msgid "Connection refused"
msgstr "연결이 거부되었습니다"

# 복수형 메시지
msgid "copied %d file"
msgid_plural "copied %d files"
msgstr[0] "%d개 파일을 복사했습니다"
```

##### 주석 유형

- `#`
  - 의미: 번역자 주석
  - 설명: 번역자가 작성하는 메모
- `#.`
  - 의미: 자동 주석
  - 설명: 소스 코드에서 자동 추출된 주석
- `#:`
  - 의미: 위치 정보
  - 설명: 메시지가 사용되는 소스 파일과 라인 번호
- `#,`
  - 의미: 플래그
  - 설명: 메시지 특성(예: c-format, fuzzy)

##### 플래그 설명

- `fuzzy`: 소스 변경으로 인해 번역이 오래되었을 수 있음. 번역자가 확인 후 제거해야 함
  - 중요: Fuzzy 메시지는 최종 사용자에게 제공되지 않음!
- `c-format`: printf 형식 문자열임을 나타냄. 번역도 동일한 플레이스홀더를 포함해야 함

##### 번역 시 주의사항

1. 형식 지정자 보존

원본의 형식 지정자(`%s`, `%d`, `%m` 등) 반드시 보존 필요:

```po
# 올바른 예
msgid "File %s has %d lines"
msgstr "파일 %s에 %d개의 줄이 있습니다"
```

2. 순서 변경이 필요한 경우

언어에 따라 인자 순서를 변경해야 할 때는 위치 지정자 사용:

```po
msgid "File %s has %d characters."
msgstr "%2$d개의 문자가 파일 %1$s에 있습니다."
```

- `%1$s`: 첫 번째 인자 (문자열)
- `%2$d`: 두 번째 인자 (정수)

3. 줄바꿈 및 특수 문자 유지

```po
# 원본이 \n으로 끝나면 번역도 동일하게
msgid "Operation completed.\n"
msgstr "작업이 완료되었습니다.\n"
```

4. 스타일 일관성

- 완전한 문장이 아닌 메시지는 대문자로 시작하지 않음
- 마침표로 끝내지 않음 (문장이 아닌 경우)
- 예: `cannot open file %s` → `파일 %s을(를) 열 수 없음`

##### 편집 도구 추천

- Emacs PO 모드: PO 파일 편집에 최적화
- Poedit: GUI 기반 PO 편집기
- Lokalize: KDE 번역 도구
- 일반 텍스트 편집기: 기본적인 편집 가능

---

### 프로그래머 가이드 (For the Programmer)

#### 메커니즘 (Mechanics)

##### 1단계: 프로그램 시작 시퀀스에 초기화 코드 추가

```c
#ifdef ENABLE_NLS
#include <locale.h>
#endif

...

#ifdef ENABLE_NLS
setlocale(LC_ALL, "");
bindtextdomain("progname", LOCALEDIR);
textdomain("progname");
#endif
```

함수 설명:

- `setlocale(LC_ALL, "")`: 환경 변수에서 로케일 설정 로드
- `bindtextdomain()`: 메시지 카탈로그 디렉토리 지정
- `textdomain()`: 현재 텍스트 도메인(프로그램 이름) 설정

##### 2단계: 번역 대상 메시지에 gettext() 호출 추가

변경 전:

```c
fprintf(stderr, "panic level %d\n", lvl);
```

변경 후:

```c
fprintf(stderr, gettext("panic level %d\n"), lvl);
```

단축 매크로 사용 (권장):

```c
#define _(x) gettext(x)

// 사용 예
fprintf(stderr, _("panic level %d\n"), lvl);
```

##### 3단계: nls.mk 파일 작성

프로그램 소스 디렉토리에 `nls.mk` 파일 생성:

```makefile
CATALOG_NAME = psql
GETTEXT_FILES = command.c common.c copy.c help.c input.c \
                large_obj.c mainloop.c print.c startup.c \
                describe.c sql_help.h tab-complete.c \
                variables.c
GETTEXT_TRIGGERS = psql_error simple_prompt write_msg:2
```

변수 설명:

- `CATALOG_NAME`: `textdomain()` 호출에서 사용된 프로그램 이름
- `GETTEXT_FILES`: 번역 가능한 문자열을 포함하는 소스 파일 목록
- `GETTEXT_TRIGGERS`: 번역 문자열을 포함하는 함수·매크로 이름

GETTEXT_TRIGGERS 형식:

- `gettext` - 첫 번째 인자가 번역 대상
- `_` - 첫 번째 인자가 번역 대상 (매크로)
- `func:2` - 두 번째 인자가 번역 대상
- `ereport:2` - 두 번째 인자가 번역 대상

##### 4단계: po/LINGUAS 파일 생성

지원할 번역 목록 작성. 초기에는 비워 둘 수 있음:

```
# po/LINGUAS
de fr ja ko pt_BR zh_CN
```

#### 메시지 작성 가이드라인 (Message-Writing Guidelines)

##### 1. 런타임에 문장을 구성하지 말 것

잘못된 예:

```c
printf("Files were %s.\n", flag ? "copied" : "removed");
```

문제점: 다른 언어에서는 어순이 다를 수 있어 번역 불가능.

올바른 예:

```c
if (flag)
    printf(_("Files were copied.\n"));
else
    printf(_("Files were removed.\n"));
```

##### 2. 복수형 처리

잘못된 예 1:

```c
printf("copied %d file%s", n, n != 1 ? "s" : "");
```

잘못된 예 2:

```c
if (n == 1)
    printf("copied 1 file");
else
    printf("copied %d files", n);
```

문제점: 많은 언어에서 복수형 규칙이 영어와 다름 (예: 러시아어는 3가지 복수형).

권장 방법 1 - 복수형 회피:

```c
printf(_("number of copied files: %d"), n);
```

권장 방법 2 - errmsg_plural() 사용:

```c
ereport(INFO,
    errmsg_plural("copied %d file",
                  "copied %d files",
                  n,
                  n));
```

errmsg_plural() 인자 설명:

- 첫 번째: 단수형 형식 문자열(영어)
- 두 번째: 복수형 형식 문자열(영어)
- 세 번째: 복수형 결정 제어값(n)
- 네 번째 이후: 형식 문자열에 따른 인자들

ngettext() 직접 사용:

`ereport`/`errmsg` 외부에서는 `ngettext()`를 직접 사용:

```c
printf(ngettext("Processed %d row", "Processed %d rows", count), count);
```

##### 3. 번역자를 위한 주석 추가

복잡하거나 모호한 메시지에는 번역자를 위한 주석 추가:

```c
/* translator: %s is the name of a data type */
_("cannot cast to %s")

/* translator: This message appears when connection is lost */
_("lost connection")
```

이 주석은 메시지 카탈로그 파일에 복사되어 번역자가 문맥을 파악하는 데 활용됨.

##### 4. 플레이스홀더 설명

플레이스홀더가 여러 개인 경우, 각각이 무엇을 나타내는지 설명:

```c
/*
 * translator: first %s is the column name,
 * second %s is the table name
 */
errmsg("column %s of table %s does not exist",
       colname, tablename)
```

##### 5. 일관된 용어 사용

동일한 개념에는 일관된 용어 사용:

```c
// 좋은 예 - 일관된 용어
_("cannot open file")
_("cannot read file")
_("cannot write file")

// 나쁜 예 - 불일관된 용어
_("cannot open file")
_("unable to read file")
_("failed to write file")
```

---

### 예제: 완전한 NLS 지원 프로그램

#### 소스 코드 (myprogram.c)

```c
#include "postgres.h"

#ifdef ENABLE_NLS
#include <locale.h>
#include <libintl.h>
#define _(x) gettext(x)
#else
#define _(x) (x)
#endif

#include "utils/elog.h"

void
initialize_nls(void)
{
#ifdef ENABLE_NLS
    setlocale(LC_ALL, "");
    bindtextdomain("myprogram", LOCALEDIR);
    textdomain("myprogram");
#endif
}

void
process_files(int count)
{
    if (count < 0)
    {
        ereport(ERROR,
            errmsg(_("invalid file count: %d"), count));
        return;
    }

    /* 복수형 처리 예제 */
    ereport(INFO,
        errmsg_plural("processed %d file successfully",
                      "processed %d files successfully",
                      count,
                      count));
}

void
connect_database(const char *dbname, const char *host)
{
    if (dbname == NULL)
    {
        /* translator: 데이터베이스 이름이 제공되지 않았을 때 표시됨 */
        ereport(ERROR,
            errmsg(_("database name is required")));
        return;
    }

    /*
     * translator: first %s is database name,
     * second %s is host name
     */
    ereport(INFO,
        errmsg(_("connecting to database \"%s\" on host \"%s\""),
               dbname, host));
}
```

#### nls.mk 파일

```makefile
CATALOG_NAME = myprogram
GETTEXT_FILES = myprogram.c utils.c commands.c
GETTEXT_TRIGGERS = ereport:2 errmsg errmsg_plural:1,2
```

#### 한국어 번역 파일 (ko.po)

```po
# Korean translation for myprogram
# Copyright (C) 2024 PostgreSQL Global Development Group
# This file is distributed under the same license as PostgreSQL.
#
msgid ""
msgstr ""
"Project-Id-Version: myprogram 1.0\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 2024-01-15 10:00+0900\n"
"PO-Revision-Date: 2024-01-15 11:00+0900\n"
"Last-Translator: 번역자 이름 <translator@example.com>\n"
"Language-Team: Korean\n"
"Language: ko\n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=UTF-8\n"
"Content-Transfer-Encoding: 8bit\n"
"Plural-Forms: nplurals=1; plural=0;\n"

#: myprogram.c:25
#, c-format
msgid "invalid file count: %d"
msgstr "잘못된 파일 개수: %d"

#: myprogram.c:32
#, c-format
msgid "processed %d file successfully"
msgid_plural "processed %d files successfully"
msgstr[0] "%d개 파일을 성공적으로 처리했습니다"

#. translator: 데이터베이스 이름이 제공되지 않았을 때 표시됨
#: myprogram.c:42
msgid "database name is required"
msgstr "데이터베이스 이름이 필요합니다"

#. translator: first %s is database name, second %s is host name
#: myprogram.c:50
#, c-format
msgid "connecting to database \"%s\" on host \"%s\""
msgstr "호스트 \"%2$s\"의 데이터베이스 \"%1$s\"에 연결 중"
```

#### po/LINGUAS 파일

```
de fr ja ko pt_BR zh_CN
```

---

### 번역 워크플로우 요약

```
┌─────────────────────────────────────────────────────────────┐
│                    새로운 번역 시작                           │
├─────────────────────────────────────────────────────────────┤
│  1. make init-po         → progname.pot 생성                 │
│  2. cp progname.pot ko.po → 언어별 PO 파일 생성              │
│  3. po/LINGUAS에 ko 추가  → 언어 등록                        │
│  4. ko.po 편집            → msgstr 작성                      │
│  5. make                  → MO 파일 생성 및 설치              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    기존 번역 업데이트                          │
├─────────────────────────────────────────────────────────────┤
│  1. make update-po        → 새 POT와 기존 PO 병합            │
│  2. fuzzy 항목 확인        → 변경된 메시지 검토               │
│  3. 새 메시지 번역         → 빈 msgstr 채우기                 │
│  4. fuzzy 플래그 제거      → 검토 완료 표시                   │
│  5. make                  → MO 파일 재생성                   │
└─────────────────────────────────────────────────────────────┘
```

---

### 자주 발생하는 문제와 해결책

#### 1. Fuzzy 메시지가 표시되지 않음

문제: 번역했는데도 원본 영어 메시지가 표시됨

원인: `#, fuzzy` 플래그가 남아 있음

해결: PO 파일에서 fuzzy 플래그를 제거

```po
# 변경 전
#, fuzzy, c-format
msgid "cannot open file"
msgstr "파일을 열 수 없음"

# 변경 후
#, c-format
msgid "cannot open file"
msgstr "파일을 열 수 없음"
```

#### 2. 형식 지정자 불일치 오류

문제: `msgfmt` 실행 시 오류 발생

원인: 원본과 번역의 형식 지정자가 다름

해결: 형식 지정자를 정확히 일치시킴

```po
# 잘못된 예
msgid "Error: %s (code %d)"
msgstr "오류: %s"  # %d 누락!

# 올바른 예
msgid "Error: %s (code %d)"
msgstr "오류: %s (코드 %d)"
```

#### 3. 인코딩 문제

문제: 한글이 깨져서 표시됨

원인: PO 파일 인코딩이 올바르지 않음

해결: UTF-8 인코딩 사용 및 헤더 확인

```po
msgid ""
msgstr ""
"Content-Type: text/plain; charset=UTF-8\n"
```

---

### 참고 자료

- [GNU gettext 매뉴얼](https://www.gnu.org/software/gettext/manual/)
- [PostgreSQL 공식 문서 - Native Language Support](https://www.postgresql.org/docs/current/nls.html)
- [ISO 639-1 언어 코드](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
- [PostgreSQL Error Message Style Guide](https://www.postgresql.org/docs/current/error-style-guide.html)

---

## Chapter 75: 플래너가 통계를 사용하는 방법 (How the Planner Uses Statistics)

### 목차

1. [개요](#개요)
2. [단일 컬럼 통계](#단일-컬럼-통계)
3. [행 추정 예제](#행-추정-예제)
4. [확장 통계](#확장-통계)
5. [모범 사례](#모범-사례)

---

### 개요

쿼리 플래너는 쿼리가 검색할 행의 수를 추정 → 최적의 실행 계획 선택. 이러한 통계 정보는 시스템 카탈로그에 저장되며, `VACUUM`, `ANALYZE`, 그리고 DDL 명령에 의해 업데이트됨.

플래너가 사용하는 주요 통계 정보:
- 행 수(Row Count): 테이블에 포함된 총 행의 수
- 디스크 페이지 수(Disk Page Count): 테이블이 차지하는 디스크 페이지 수
- 선택도(Selectivity): WHERE 조건을 만족하는 행의 비율
- 가장 빈번한 값(Most Common Values, MCV): 컬럼에서 가장 자주 나타나는 값들
- 히스토그램(Histogram): 값의 분포를 나타내는 버킷 경계값들

---

### 단일 컬럼 통계

#### 기본 통계 저장

행 수와 디스크 블록 수는 `pg_class` 시스템 카탈로그의 `reltuples`와 `relpages` 컬럼에 저장됨.

```sql
SELECT relname, relkind, reltuples, relpages
FROM pg_class
WHERE relname LIKE 'tenk1%';
```

결과 예시:
```
    relname     | relkind | reltuples | relpages
----------------+---------+-----------+----------
 tenk1          | r       |     10000 |      345
 tenk1_hundred  | i       |     10000 |       11
```

주요 사항:
- `reltuples`와 `relpages`는 실시간으로 업데이트되지 않음
- `VACUUM`, `ANALYZE`, DDL 명령에 의해 업데이트됨
- 플래너는 현재 물리적 테이블 크기에 맞게 이 값들을 스케일링 → 더 나은 근사치 확보

#### 선택도 통계 (Selectivity Statistics)

플래너는 `pg_statistic` 시스템 카탈로그에 저장된 데이터를 사용하여 WHERE 조건과 일치하는 행의 비율인 선택도(Selectivity)를 추정함. 이 데이터는 `ANALYZE`와 `VACUUM ANALYZE`에 의해 업데이트됨.

슈퍼유저 권한이 필요 없고 더 읽기 쉬운 `pg_stats` 뷰를 대신 사용 가능.

```sql
SELECT attname, inherited, n_distinct,
       array_to_string(most_common_vals, E'\n') as most_common_vals
FROM pg_stats
WHERE tablename = 'road';
```

#### 통계 구성

컬럼별로 수집되는 통계의 양을 제어 가능:

```sql
ALTER TABLE table_name ALTER COLUMN column_name SET STATISTICS value;
```

또는 `default_statistics_target` 구성 변수를 통해 전역적으로 설정 가능 (기본값: 100개 항목).

---

### 행 추정 예제

#### 예제 1: 범위 조건 (Range Conditions)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 1000;
```

과정:
1. 플래너가 `<` 연산자의 선택도 함수(`scalarltsel`)를 조회함
2. `pg_stats`에서 히스토그램을 검색함:

```sql
SELECT histogram_bounds FROM pg_stats
WHERE tablename='tenk1' AND attname='unique1';

-- 결과: {0,993,1997,3050,4040,5036,5957,7057,8029,9016,9995}
```

3. 히스토그램 버킷 내에서 선형 분포를 사용하여 선택도를 계산함:

```
선택도 = (1 + (1000 - 993)/(1997 - 993))/10
       = 0.100697

추정 행 수 = 10000 * 0.100697 = 1007
```

#### 예제 2: MCV를 사용한 등호 조건 (Equality with Common Values)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 = 'CRAAAA';
```

과정:
가장 빈번한 값(MCV)과 빈도를 사용함:

```sql
SELECT null_frac, n_distinct, most_common_vals, most_common_freqs
FROM pg_stats
WHERE tablename='tenk1' AND attname='stringu1';
```

결과:
```
most_common_vals  | {EJAAAA,BBAAAA,CRAAAA,...}
most_common_freqs | {0.00333333,0.003,0.003,...}

선택도 = 0.003
추정 행 수 = 10000 * 0.003 = 30
```

#### 예제 3: MCV에 없는 값의 등호 조건

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 = 'xxx';
```

과정:
값이 MCV 목록에 없으므로 다음 공식을 사용함:

```
선택도 = (1 - sum(mcv_freqs))/(num_distinct - num_mcv)
       = (1 - 0.03033333)/(676 - 10)
       = 0.0014559

추정 행 수 = 10000 * 0.0014559 = 15
```

#### 예제 4: MCV와 히스토그램을 사용한 범위 조건

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 < 'IAAAAA';
```

과정:
MCV와 히스토그램 추정을 결합함:

```
selectivity_mcv = 0.01833333 (일치하는 MCV 빈도의 합)
selectivity_histogram = 0.298387 * 0.96966667
선택도 = 0.01833333 + 0.298387 * 0.96966667 = 0.307669

추정 행 수 = 10000 * 0.307669 = 3077
```

#### 예제 5: 다중 조건 (Multiple Conditions)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1
WHERE unique1 < 1000 AND stringu1 = 'xxx';
```

과정:
독립성을 가정하고 선택도를 곱함:

```
선택도 = 0.100697 * 0.0014559 = 0.0001466
추정 행 수 = 10000 * 0.0001466 = 1
```

#### 예제 6: 조인 연산 (Join Operations)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 50 AND t1.unique2 = t2.unique2;
```

tenk1에 대한 필터:
```
선택도 = (0 + (50 - 0)/(993 - 0))/10 = 0.005035
행 수 = 10000 * 0.005035 = 50
```

조인 선택도 (`eqjoinsel` 사용):
```sql
SELECT tablename, null_frac, n_distinct, most_common_vals
FROM pg_stats
WHERE tablename IN ('tenk1', 'tenk2') AND attname='unique2';
```

유니크 컬럼의 경우:
```
선택도 = (1 - 0) * (1 - 0) / max(10000, 10000) = 0.0001

행 수 = (50 * 10000) * 0.0001 = 50
```

---

### 확장 통계

확장 통계(Extended Statistics)는 단일 컬럼 통계로는 파악할 수 없는 컬럼 간 상관관계를 수집함. `CREATE STATISTICS` 명령으로 정의하며, 실제 데이터 수집은 `ANALYZE` 실행 시 이루어짐.

#### 1. 함수적 종속성 (Functional Dependencies)

컬럼 `b`가 컬럼 `a`에 함수적으로 종속된 경우를 추적함 (`a` 값이 결정되면 `b` 값도 결정됨).

##### 문제 상황

확장 통계가 없으면 플래너는 컬럼 조건이 서로 독립적이라고 가정하므로, 상관관계가 있는 컬럼에 대해 행 수를 심각하게 과소 추정함.

예제 설정:
```sql
CREATE TABLE t (a INT, b INT);
INSERT INTO t SELECT i % 100, i % 100 FROM generate_series(1, 10000) s(i);
ANALYZE t;
```

통계 없이 실행:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 1;

-- 추정: 1행 (0.01% 선택도 - 부정확)
-- 실제: 100행
```

플래너는 컬럼 간 함수적 종속성을 인식하지 못하고 개별 선택도를 단순히 곱함 (1% × 1% = 0.01%).

##### 해결책

```sql
CREATE STATISTICS stts (dependencies) ON a, b FROM t;
ANALYZE t;

EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 1;

-- 추정: 100행 (정확!)
```

통계 확인:
```sql
SELECT stxname, stxkeys, stxddependencies
FROM pg_statistic_ext JOIN pg_statistic_ext_data ON (oid = stxoid)
WHERE stxname = 'stts';
```

출력:
```
stxname | stxkeys |             stxddependencies
--------+---------+--------------------------------------------
stts    | 1 5     | {"1 => 5": 1.000000, "5 => 1": 0.423130}
```

컬럼 1(ZIP 코드)이 컬럼 5(도시)를 계수 1.0으로 완전히 결정하고, 반대 방향(도시 → ZIP 코드)은 42.3% 수준임을 나타냄.

제한사항:
- 컬럼과 상수를 비교하는 단순 등호 조건과 `IN` 절에만 적용
- 컬럼 간 비교, 범위 절, `LIKE`, 기타 조건 유형에는 미적용
- 실제로 호환되지 않는 조건 감지 불가(예: `city='San Francisco' AND zip='90210'`)

#### 2. 다변량 N-Distinct 수 (Multivariate N-Distinct Counts)

컬럼 조합의 고유 값 수를 수집 → 여러 컬럼을 사용하는 GROUP BY 쿼리의 추정 정확도 향상.

단일 컬럼 (정확):
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a;

-- 추정 행 수: 100 (정확)
```

다중 컬럼 (통계 없이):
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a, b;

-- 추정: 1000행 (실제: 100 - 10배 차이)
```

해결책:
```sql
CREATE STATISTICS stts (dependencies, ndistinct) ON a, b FROM t;
ANALYZE t;

EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a, b;

-- 추정: 100행 (정확!)
```

통계 확인:
```sql
SELECT stxkeys AS k, stxdndistinct AS nd
FROM pg_statistic_ext JOIN pg_statistic_ext_data ON (oid = stxoid)
WHERE stxname = 'stts';
```

출력:
```
   k   |                                   nd
-------+------------------------------------------------------------------------
 1 2 5 | {"1, 2": 33178, "1, 5": 33178, "2, 5": 27435, "1, 2, 5": 33178}
```

#### 3. 다변량 MCV 목록 (Multivariate MCV Lists)

컬럼 조합의 가장 빈번한 값 목록을 수집 → 다중 컬럼 조건에 대한 추정 정확도를 크게 향상.

MCV 통계 생성:
```sql
CREATE STATISTICS stts2 (mcv) ON a, b FROM t;
ANALYZE t;
```

MCV 목록 검사:
```sql
SELECT m.* FROM pg_statistic_ext
JOIN pg_statistic_ext_data ON (oid = stxoid),
     pg_mcv_list_items(stxdmcv) m
WHERE stxname = 'stts2';
```

출력:
```
 index |  values  | nulls | frequency | base_frequency
-------+----------+-------+-----------+----------------
     0 | {0, 0}   | {f,f} |      0.01 |         0.0001
     1 | {1, 1}   | {f,f} |      0.01 |         0.0001
   ...
    99 | {99, 99} | {f,f} |      0.01 |         0.0001
```

##### MCV 목록의 장점

1. 호환되지 않는 값 조합 감지:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 10;

-- 추정: 1행 (정확 - 일치하는 조합이 없음)
-- 실제: 0행
-- (함수적 종속성은 여전히 1%로 추정할 것임)
```

2. 비등호 절 처리:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a <= 49 AND b > 49;

-- 추정: 1행 (정확 - 일치하는 조합 없음)
-- 실제: 0행
-- (함수적 종속성은 등호 조건에서만 작동)
```

#### 확장 통계 비교

- 함수적 종속성(Functional Dependencies)
  - 비용: 매우 저렴
  - 저장 공간: 최소
  - 절 유형: 등호만
  - 세분화: 컬럼 수준만
  - 호환되지 않는 값: 감지 불가
- MCV 목록(MCV Lists)
  - 비용: 더 비쌈
  - 저장 공간: 더 큼
  - 절 유형: 모든 유형(범위, 부등호 등)
  - 세분화: 개별 값
  - 호환되지 않는 값: 감지 가능

---

### 모범 사례

1. 필요한 경우에만 확장 통계 생성: 쿼리에서 실제로 사용되는 컬럼 그룹에 대해서만 확장 통계 생성 권장
2. 함수적 종속성 사용: 강하게 상관된 컬럼에 함수적 종속성 사용 권장
3. N-Distinct 통계 적용: GROUP BY에서 사용되는 컬럼 조합에 ndistinct 통계 적용 권장
4. MCV 통계 신중히 생성: 잘못된 추정이 나쁜 계획을 유발할 때만 MCV 통계 생성 권장
5. 통계 타겟 조정: 불규칙한 데이터 분포를 가진 컬럼에 대해 통계 타겟을 증가 → 더 나은 정확도 확보

```sql
-- 특정 컬럼의 통계 타겟 증가
ALTER TABLE my_table ALTER COLUMN my_column SET STATISTICS 500;

-- 분석 실행
ANALYZE my_table;
```

6. 정기적인 ANALYZE 실행: 데이터 분포가 크게 변경된 후에는 `ANALYZE`를 실행 → 통계를 최신 상태로 유지 권장

```sql
-- 특정 테이블 분석
ANALYZE my_table;

-- 특정 컬럼만 분석
ANALYZE my_table (column1, column2);
```

---

### 소스 코드 참조

플래너 통계와 관련된 PostgreSQL 소스 코드:

- 테이블 크기 추정: `src/backend/optimizer/util/plancat.c`
- 절 선택도 로직: `src/backend/optimizer/path/clausesel.c`
- 연산자별 함수: `src/backend/utils/adt/selfuncs.c`

---

### 참고 자료

- [PostgreSQL 공식 문서: Using EXPLAIN](https://www.postgresql.org/docs/current/using-explain.html)
- [PostgreSQL 공식 문서: pg_stats 뷰](https://www.postgresql.org/docs/current/view-pg-stats.html)
- [PostgreSQL 공식 문서: CREATE STATISTICS](https://www.postgresql.org/docs/current/sql-createstatistics.html)
- [PostgreSQL 공식 문서: ANALYZE](https://www.postgresql.org/docs/current/sql-analyze.html)

---

## Chapter 62. 유전 쿼리 최적화기 (Genetic Query Optimizer)

PostgreSQL의 유전 쿼리 최적화기(GEQO, Genetic Query Optimizer)는 복잡한 조인 쿼리에 대한 효율적인 실행 계획을 탐색하기 위해 유전 알고리즘을 사용하는 쿼리 최적화 모듈.

---

### 목차

1. [복잡한 최적화 문제로서의 쿼리 처리](#1-복잡한-최적화-문제로서의-쿼리-처리)
2. [유전 알고리즘](#2-유전-알고리즘)
3. [PostgreSQL에서의 유전 쿼리 최적화](#3-postgresql에서의-유전-쿼리-최적화)
4. [GEQO 설정 파라미터](#4-geqo-설정-파라미터)
5. [예제](#5-예제)
6. [참고 문헌](#6-참고-문헌)

---

### 1. 복잡한 최적화 문제로서의 쿼리 처리

#### 1.1 조인 최적화의 어려움

관계형 데이터베이스에서 처리와 최적화가 가장 어려운 연산자는 조인(Join). 조인 최적화가 복잡한 이유:

- 지수적 증가(Exponential Growth): 쿼리에 포함된 조인의 수가 증가할수록 가능한 쿼리 계획의 수는 기하급수적으로 증가
- 다양한 조인 방법: PostgreSQL은 중첩 루프 조인(Nested Loop Join), 해시 조인(Hash Join), 병합 조인(Merge Join) 지원
- 다양한 인덱스 유형: B-tree, Hash, GiST, GIN 인덱스 등이 서로 다른 접근 경로 제공
- 탐색 복잡도: 전통적인 쿼리 최적화는 모든 대안 전략에 대한 철저한 탐색 필요

#### 1.2 전통적인 PostgreSQL 쿼리 최적화기

PostgreSQL의 표준 쿼리 최적화기는 대안 전략들에 대해 거의 완전한 탐색(Near-Exhaustive Search)을 수행함:

- 기원: IBM의 System R 데이터베이스에서 처음 도입
- 결과: 거의 최적에 가까운 조인 순서를 생성
- 한계: 조인 수가 많을 경우 지수적인 시간과 메모리 요구로 인해 실행 불가능

#### 1.3 유전 알고리즘의 필요성

전통적인 접근 방식은 다음과 같은 경우에 부적합:

- 대규모 조인 쿼리 (많은 테이블 포함)
- 복잡한 추론이 필요한 의사 결정 지원 시스템
- 상당한 쿼리 부하가 있는 지식 기반 시스템

해결책: 많은 수의 조인을 포함하는 쿼리에서 조인 순서 문제를 효율적으로 해결하기 위한 유전 알고리즘 구현

조인 가능한 테이블 수에 따른 가능한 조인 순서의 수:

- 2개 테이블: 2가지
- 3개 테이블: 12가지
- 4개 테이블: 120가지
- 5개 테이블: 1,680가지
- 6개 테이블: 30,240가지
- 7개 테이블: 665,280가지
- 10개 테이블: 17,643,225,600가지
- 12개 테이블: 약 1.76 × 10^13가지

---

### 2. 유전 알고리즘

#### 2.1 개요

유전 알고리즘(Genetic Algorithm, GA)은 무작위 탐색 기반의 휴리스틱 최적화 방법(Heuristic Optimization Method). PostgreSQL의 GEQO는 복잡한 쿼리 최적화 문제에서 최적 솔루션을 찾기 위해 이 알고리즘을 활용함.

#### 2.2 핵심 개념

##### 개체군과 적합도 (Population and Fitness)

- 가능한 솔루션의 집합은 개체(Individual)들의 개체군(Population)으로 간주됨
- 각 개체가 환경에 얼마나 잘 적응했는지는 적합도(Fitness)로 지정됨

##### 유전적 구조 (Genetic Structure)

- 염색체(Chromosome): 탐색 공간에서 개체의 좌표를 나타냄, 본질적으로 문자열의 집합
- 유전자(Gene): 최적화되는 단일 파라미터의 값을 인코딩하는 염색체의 하위 섹션
- 일반적인 인코딩: 이진(Binary) 또는 정수(Integer) 표현

#### 2.3 진화 연산 (Evolutionary Operations)

알고리즘은 세 가지 주요 연산으로 새로운 세대의 탐색 지점을 생성함:

1. 재조합(Recombination): 여러 개체의 유전 물질을 결합
2. 돌연변이(Mutation): 유전 물질에 무작위 변화를 적용
3. 선택(Selection): 더 높은 적합도를 가진 개체를 선택하여 번식

#### 2.4 중요한 구분

comp.ai.genetic FAQ에 따르면, GA는 순수한 무작위 탐색이 아님:

> "GA는 확률적 과정을 사용하지만, 결과는 명백히 비무작위적임(무작위보다 더 나음)."

즉, 무작위성이 개입하지만 유전 알고리즘은 세대를 거듭할수록 단순 무작위 탐색보다 체계적으로 더 나은 솔루션을 생성함.

#### 2.5 유전 알고리즘의 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│                    초기 개체군 생성                          │
│              (무작위 조인 순서 생성)                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    적합도 평가                               │
│           (각 조인 순서의 실행 비용 계산)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  종료 조건 충족? │
                    └─────────────────┘
                      │           │
                     예          아니오
                      │           │
                      ▼           ▼
            ┌──────────────┐   ┌─────────────────────────────┐
            │ 최적 계획 반환 │   │         선택               │
            └──────────────┘   │  (높은 적합도 개체 선택)     │
                               └─────────────────────────────┘
                                              │
                                              ▼
                               ┌─────────────────────────────┐
                               │         재조합              │
                               │  (에지 재조합 교차 사용)     │
                               └─────────────────────────────┘
                                              │
                                              ▼
                               ┌─────────────────────────────┐
                               │       새로운 세대 생성       │
                               └─────────────────────────────┘
                                              │
                                              └──────────────┐
                                                             │
                              ┌───────────────────────────────┘
                              │
                              ▼
                    (적합도 평가로 돌아감)
```

---

### 3. PostgreSQL에서의 유전 쿼리 최적화

#### 3.1 개요

GEQO(Genetic Query Optimization)는 쿼리 최적화를 외판원 문제(Traveling Salesman Problem, TSP)로 접근하는 PostgreSQL 모듈. 가능한 쿼리 계획을 조인 순서를 나타내는 정수 문자열로 인코딩함.

#### 3.2 조인 순서 인코딩

쿼리 계획은 각 숫자가 릴레이션 ID를 나타내는 정수 문자열로 표현됨:

```
예제 조인 트리:
      /\
     /\ 2
    /\ 3
   4  1

인코딩: '4-1-3-2'
(릴레이션 4를 1과 조인, 그 다음 3, 그 다음 2)
```

#### 3.3 PostgreSQL GEQO의 주요 특성

1. 정상 상태 GA (Steady State GA): 전체 세대를 교체하는 대신 적합도가 가장 낮은 개체만 교체 → 빠른 수렴 달성
2. 에지 재조합 교차 (Edge Recombination Crossover): 최소한의 에지 손실로 TSP 솔루션에 특히 적합함
3. 돌연변이 연산자 없음: 유효한 투어를 생성하기 위한 복구 메커니즘 불필요

#### 3.4 계획 생성 과정

1. 표준 플래너 코드를 사용하여 개별 릴레이션 스캔 생성
2. 초기 무작위 조인 순서 생성
3. 각 순서에 대해 표준 플래너를 호출하여 실행 비용 추정
4. 각 단계에서 세 가지 가능한 조인 전략을 모두 평가
5. 가장 적합도가 낮은 후보 폐기
6. 저비용 순서의 일부를 결합하여 새로운 후보 생성
7. 사전 설정된 수의 순서가 평가될 때까지 반복
8. 발견된 최적의 계획 사용

#### 3.5 GEQO 소스 코드 구조

주요 루틴은 `src/backend/optimizer/geqo/` 디렉토리에 위치:

- `geqo_main.c`: GEQO 메인 루틴
- `geqo_pool.c`: 개체군 풀 관리
- `geqo_selection.c`: 선택 연산
- `geqo_recombination.c`: 재조합 연산
- `geqo_erx.c`: 에지 재조합 교차
- `geqo_ox1.c`, `geqo_ox2.c`: 순서 교차 연산
- `geqo_pmx.c`: 부분 매핑 교차
- `geqo_random.c`: 난수 생성

#### 3.6 알려진 제한 사항

1. 비용 재계산: 각 후보마다 비용 추정을 다시 계산해야 하므로 반복 작업 발생
2. 메모리 문제: 하위 조인의 비용 추정을 캐싱할 때 메모리 문제 발생 가능
3. TSP 적합성: TSP 알고리즘이 쿼리 최적화에 항상 이상적이지는 않음(하위 시퀀스의 비용이 TSP와 달리 문맥에 의존적)
4. 에지 재조합 효과: 쿼리 최적화에서 에지 재조합 교차의 효과에 대해서는 의문 제기됨

---

### 4. GEQO 설정 파라미터

#### 4.1 geqo (boolean)

유전 쿼리 최적화를 활성화하거나 비활성화.

```sql
-- GEQO 비활성화
SET geqo = off;

-- GEQO 활성화 (기본값)
SET geqo = on;
```

- 기본값: `on`
- 참고: 프로덕션 환경에서는 일반적으로 비활성화하지 않는 편이 좋음. 더 세밀한 제어가 필요하면 `geqo_threshold` 활용 권장

#### 4.2 geqo_threshold (integer)

이 수 이상의 FROM 항목이 포함된 쿼리에 대해 유전 쿼리 최적화 사용.

```sql
-- 8개 이상의 테이블 조인에 GEQO 사용
SET geqo_threshold = 8;

-- 기본값: 12개 이상의 테이블에서 GEQO 사용
SET geqo_threshold = 12;
```

- 기본값: `12`
- 참고: `FULL OUTER JOIN`은 FROM 항목 하나로 계산됨. 단순한 쿼리에는 일반적인 완전 탐색 플래너가 더 유리하며, 테이블이 많은 복잡한 쿼리에서는 GEQO가 과도한 계획 시간 방지

#### 4.3 geqo_effort (integer)

계획 시간과 쿼리 계획 품질 간의 균형 제어.

```sql
-- 최소 노력 (빠른 계획, 품질 저하 가능)
SET geqo_effort = 1;

-- 기본값
SET geqo_effort = 5;

-- 최대 노력 (느린 계획, 더 나은 품질)
SET geqo_effort = 10;
```

- 범위: 1 ~ 10
- 기본값: `5`
- 참고: 값이 클수록 계획 시간은 늘어나지만 효율적인 계획이 선택될 가능성도 상승. 이 값은 다른 GEQO 변수의 기본값을 산출하는 데만 사용되며, 동작에 직접 영향을 주지는 않음

#### 4.4 geqo_pool_size (integer)

유전 개체군 크기(개체 수) 제어.

```sql
-- 작은 풀 크기 (빠른 실행)
SET geqo_pool_size = 128;

-- 큰 풀 크기 (더 나은 탐색)
SET geqo_pool_size = 1024;

-- 자동 결정 (기본값)
SET geqo_pool_size = 0;
```

- 최소값: 2
- 일반적인 범위: 100 ~ 1000
- 기본값: `0` (geqo_effort와 테이블 수에 따라 자동 선택)

#### 4.5 geqo_generations (integer)

알고리즘의 반복 횟수 제어.

```sql
-- 더 많은 세대 (더 나은 결과, 더 오래 걸림)
SET geqo_generations = 500;

-- 자동 결정 (기본값)
SET geqo_generations = 0;
```

- 최소값: 1
- 일반적인 범위: 풀 크기와 동일
- 기본값: `0` (geqo_pool_size에 따라 자동 선택)

#### 4.6 geqo_selection_bias (floating point)

개체군 내 선택 압력 제어.

```sql
-- 낮은 선택 압력
SET geqo_selection_bias = 1.5;

-- 높은 선택 압력 (기본값)
SET geqo_selection_bias = 2.0;
```

- 범위: 1.50 ~ 2.00
- 기본값: `2.0`

#### 4.7 geqo_seed (floating point)

조인 순서 탐색 공간을 통한 무작위 경로를 선택하는 데 사용되는 난수 생성기의 초기값.

```sql
-- 특정 시드 설정 (재현 가능한 결과)
SET geqo_seed = 0.5;

-- 기본값
SET geqo_seed = 0;
```

- 범위: 0 ~ 1
- 기본값: `0`
- 참고: 값을 바꾸면 탐색하는 조인 경로 집합이 달라져 더 나은 계획 또는 더 나쁜 계획으로 이어질 수 있음. 동일한 시드와 동일한 GEQO 파라미터를 사용하면 같은 쿼리에 대해 항상 동일한 계획 생성됨

#### 4.8 설정 예제

```sql
-- postgresql.conf 또는 세션에서 설정
-- 복잡한 쿼리에 대해 GEQO를 더 공격적으로 사용
SET geqo = on;
SET geqo_threshold = 8;
SET geqo_effort = 7;
SET geqo_pool_size = 256;
SET geqo_generations = 256;
SET geqo_selection_bias = 1.75;
SET geqo_seed = 0.5;
```

---

### 5. 예제

#### 5.1 GEQO 동작 확인

```sql
-- GEQO 활성화 상태 확인
SHOW geqo;
SHOW geqo_threshold;

-- 현재 GEQO 설정 확인
SELECT name, setting, unit, short_desc
FROM pg_settings
WHERE name LIKE 'geqo%';
```

결과 예시:
```
         name          | setting |  unit   |                      short_desc
-----------------------+---------+---------+-----------------------------------------------------
 geqo                  | on      |         | Enables genetic query optimization.
 geqo_effort           | 5       |         | GEQO: effort is used to set the default for ...
 geqo_generations      | 0       |         | GEQO: number of iterations of the algorithm.
 geqo_pool_size        | 0       |         | GEQO: number of individuals in the population.
 geqo_seed             | 0       |         | GEQO: seed for random path selection.
 geqo_selection_bias   | 2       |         | GEQO: selective pressure within the population.
 geqo_threshold        | 12      |         | Sets the threshold of FROM items beyond ...
```

#### 5.2 GEQO가 활성화되는 쿼리 예제

```sql
-- 12개 이상의 테이블을 조인하는 복잡한 쿼리
-- GEQO가 기본적으로 활성화됨 (geqo_threshold = 12)
EXPLAIN ANALYZE
SELECT *
FROM table1 t1
JOIN table2 t2 ON t1.id = t2.t1_id
JOIN table3 t3 ON t2.id = t3.t2_id
JOIN table4 t4 ON t3.id = t4.t3_id
JOIN table5 t5 ON t4.id = t5.t4_id
JOIN table6 t6 ON t5.id = t6.t5_id
JOIN table7 t7 ON t6.id = t7.t6_id
JOIN table8 t8 ON t7.id = t8.t7_id
JOIN table9 t9 ON t8.id = t9.t8_id
JOIN table10 t10 ON t9.id = t10.t9_id
JOIN table11 t11 ON t10.id = t11.t10_id
JOIN table12 t12 ON t11.id = t12.t11_id;
```

#### 5.3 GEQO 성능 비교 테스트

```sql
-- 테스트용 테이블 생성
CREATE TABLE test_geqo AS
SELECT generate_series(1, 10000) AS id,
       random() AS value;

-- 인덱스 생성
CREATE INDEX ON test_geqo(id);

-- GEQO 비활성화 상태에서 계획 시간 측정
SET geqo = off;
EXPLAIN ANALYZE
SELECT * FROM test_geqo t1, test_geqo t2, test_geqo t3,
              test_geqo t4, test_geqo t5, test_geqo t6,
              test_geqo t7, test_geqo t8, test_geqo t9,
              test_geqo t10, test_geqo t11, test_geqo t12
WHERE t1.id = t2.id AND t2.id = t3.id AND t3.id = t4.id
  AND t4.id = t5.id AND t5.id = t6.id AND t6.id = t7.id
  AND t7.id = t8.id AND t8.id = t9.id AND t9.id = t10.id
  AND t10.id = t11.id AND t11.id = t12.id
LIMIT 10;

-- GEQO 활성화 상태에서 계획 시간 측정
SET geqo = on;
SET geqo_threshold = 12;
EXPLAIN ANALYZE
SELECT * FROM test_geqo t1, test_geqo t2, test_geqo t3,
              test_geqo t4, test_geqo t5, test_geqo t6,
              test_geqo t7, test_geqo t8, test_geqo t9,
              test_geqo t10, test_geqo t11, test_geqo t12
WHERE t1.id = t2.id AND t2.id = t3.id AND t3.id = t4.id
  AND t4.id = t5.id AND t5.id = t6.id AND t6.id = t7.id
  AND t7.id = t8.id AND t8.id = t9.id AND t9.id = t10.id
  AND t10.id = t11.id AND t11.id = t12.id
LIMIT 10;
```

#### 5.4 GEQO 시드를 이용한 계획 다양성 테스트

```sql
-- 시드 값을 변경하여 다른 실행 계획 탐색
SET geqo_seed = 0.0;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.25;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.5;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.75;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;
```

#### 5.5 특정 쿼리에서 GEQO 임시 비활성화

```sql
-- 트랜잭션 내에서 GEQO 임시 비활성화
BEGIN;
SET LOCAL geqo = off;
-- 완전한 탐색이 필요한 중요한 쿼리 실행
SELECT ...;
COMMIT;
-- 트랜잭션 종료 후 원래 설정으로 복원
```

#### 5.6 GEQO 디버깅을 위한 로깅

```sql
-- 쿼리 계획 시간을 로그에 기록하도록 설정
SET log_statement = 'all';
SET log_duration = on;

-- 또는 자동 설명 확장 사용
LOAD 'auto_explain';
SET auto_explain.log_min_duration = 0;
SET auto_explain.log_analyze = true;
```

---

### 6. 참고 문헌

#### 6.1 온라인 리소스

1. The Hitch-Hiker's Guide to Evolutionary Computation
   - URL: http://www.faqs.org/faqs/ai-faq/genetic/part1/
   - 출처: news://comp.ai.genetic FAQ

2. Evolutionary Computation and its application to art and design
   - 저자: Craig Reynolds
   - URL: https://www.red3d.com/cwr/evolve.html

#### 6.2 참고 문헌

3. Fundamentals of Database Systems
   - 저자: Elmasri, R. and Navathe, S.B.
   - 데이터베이스 시스템의 기초를 다루는 표준 교과서

4. The design and implementation of the POSTGRES query optimizer
   - 저자: Fong, Z.
   - University of California, Berkeley
   - POSTGRES 쿼리 최적화기의 설계와 구현에 대한 논문

#### 6.3 GEQO 개발 이력

GEQO 모듈은 Martin Utesch가 독일 프라이베르크 광업 기술 대학교(University of Mining and Technology in Freiberg, Germany) 자동 제어 연구소(Institute of Automatic Control)를 위해 개발함.

---

### 요약

- 목적: 많은 테이블이 포함된 복잡한 조인 쿼리의 효율적인 계획 탐색
- 알고리즘: 유전 알고리즘 (Genetic Algorithm)
- 인코딩: 조인 순서를 정수 문자열로 표현
- 기본 임계값: 12개 이상의 FROM 항목
- 주요 장점: 지수적 탐색 공간에서 합리적인 시간 내에 좋은 계획 발견
- 주요 단점: 최적 계획을 보장하지 않음, 일부 오버헤드 발생

GEQO는 PostgreSQL이 매우 복잡한 쿼리를 처리할 수 있게 해주는 중요한 구성 요소. 기본 설정은 대부분의 워크로드에 적합하지만, 특정 사용 사례에 맞게 파라미터를 조정 → 성능 최적화 가능.

---

## Chapter 62: 테이블 접근 메서드 인터페이스 (Table Access Method Interface Definition)

### 목차

1. [테이블 접근 메서드 개요](#1-테이블-접근-메서드-개요)
2. [기본 API 구조](#2-기본-api-구조-basic-api-structure)
3. [TableAmRoutine 콜백 함수](#3-tableamroutine-콜백-함수)
4. [스캔 API](#4-스캔-api-scan-api)
5. [수정 API](#5-수정-api-modification-api)
6. [인덱스 관련 연산](#6-인덱스-관련-연산-index-related-operations)
7. [유지보수 연산](#7-유지보수-연산-maintenance-operations)
8. [구현 요구사항](#8-구현-요구사항-implementation-requirements)

---

### 1. 테이블 접근 메서드 개요

#### 1.1 테이블 접근 메서드란?

테이블 접근 메서드(Table Access Method, TAM)는 PostgreSQL이 테이블 데이터를 저장하고 접근하는 방식을 정의하는 인터페이스. PostgreSQL 12부터 도입된 이 인터페이스를 통해:

- 커스텀 테이블 저장소 구현 가능
- 기본 `heap` 방식 외의 다양한 저장 전략 사용 가능
- 특수한 워크로드에 최적화된 저장소 개발 가능

#### 1.2 시스템 카탈로그 등록

테이블 접근 메서드는 `pg_am` 시스템 카탈로그에 등록됨:

```sql
-- 테이블 접근 메서드 조회
SELECT amname, amhandler, amtype
FROM pg_am
WHERE amtype = 't';

-- 결과 예시
--  amname | amhandler     | amtype
-- --------+---------------+--------
--  heap   | heap_tableam_handler | t
```

#### 1.3 접근 메서드 생성 및 삭제

```sql
-- 새로운 테이블 접근 메서드 생성
CREATE ACCESS METHOD myam TYPE TABLE HANDLER my_tableam_handler;

-- 테이블 접근 메서드 삭제
DROP ACCESS METHOD myam;

-- 특정 접근 메서드를 사용하는 테이블 생성
CREATE TABLE my_table (
    id serial PRIMARY KEY,
    data text
) USING myam;
```

---

### 2. 기본 API 구조 (Basic API Structure)

#### 2.1 핸들러 함수 (Handler Function)

테이블 접근 메서드의 핵심은 핸들러 함수(handler function). 이 함수는 다음 요구사항을 충족해야 함:

- 입력 타입: `internal` (SQL 직접 호출 방지용 더미 파라미터)
- 반환 타입: `table_am_handler` (의사 타입)
- 반환 값: `TableAmRoutine` 구조체에 대한 포인터

##### 핸들러 함수 등록 예제

```c
#include "postgres.h"
#include "access/tableam.h"
#include "fmgr.h"

PG_MODULE_MAGIC;

/* TableAmRoutine 구조체 정의 */
static const TableAmRoutine my_tableam_methods = {
    .type = T_TableAmRoutine,

    /* 슬롯 콜백 */
    .slot_callbacks = my_slot_callbacks,

    /* 스캔 콜백 */
    .scan_begin = my_scan_begin,
    .scan_end = my_scan_end,
    .scan_rescan = my_scan_rescan,
    .scan_getnextslot = my_scan_getnextslot,

    /* 튜플 수정 콜백 */
    .tuple_insert = my_tuple_insert,
    .tuple_update = my_tuple_update,
    .tuple_delete = my_tuple_delete,

    /* 기타 필수 콜백들... */
};

/* 핸들러 함수 정의 */
PG_FUNCTION_INFO_V1(my_tableam_handler);

Datum
my_tableam_handler(PG_FUNCTION_ARGS)
{
    PG_RETURN_POINTER(&my_tableam_methods);
}
```

##### SQL 등록

```sql
-- 핸들러 함수 생성
CREATE OR REPLACE FUNCTION my_tableam_handler(internal)
    RETURNS table_am_handler
    AS 'my_extension', 'my_tableam_handler'
    LANGUAGE C STRICT;

-- 접근 메서드 등록
CREATE ACCESS METHOD myam TYPE TABLE HANDLER my_tableam_handler;
```

#### 2.2 TableAmRoutine 구조체

`TableAmRoutine` 구조체는 테이블 접근 메서드의 모든 동작을 정의하는 콜백 함수 포인터들을 포함. 전체 정의는 `src/include/access/tableam.h`에서 확인 가능.

```c
typedef struct TableAmRoutine
{
    NodeTag     type;

    /* === 슬롯 관리 === */
    const TupleTableSlotOps* (*slot_callbacks) (Relation rel);

    /* === 스캔 콜백 === */
    TableScanDesc (*scan_begin) (Relation rel, Snapshot snapshot,
                                 int nkeys, ScanKey key,
                                 ParallelTableScanDesc pscan,
                                 uint32 flags);
    void (*scan_end) (TableScanDesc scan);
    void (*scan_rescan) (TableScanDesc scan, ScanKey key,
                         bool set_params, bool allow_strat,
                         bool allow_sync, bool allow_pagemode);
    bool (*scan_getnextslot) (TableScanDesc scan,
                              ScanDirection direction,
                              TupleTableSlot *slot);

    /* === 병렬 스캔 === */
    Size (*parallelscan_estimate) (Relation rel);
    Size (*parallelscan_initialize) (Relation rel,
                                     ParallelTableScanDesc pscan);
    void (*parallelscan_reinitialize) (Relation rel,
                                       ParallelTableScanDesc pscan);

    /* === 인덱스 패치 === */
    IndexFetchTableData* (*index_fetch_begin) (Relation rel);
    void (*index_fetch_reset) (IndexFetchTableData *data);
    void (*index_fetch_end) (IndexFetchTableData *data);
    bool (*index_fetch_tuple) (struct IndexFetchTableData *scan,
                               ItemPointer tid, Snapshot snapshot,
                               TupleTableSlot *slot, bool *call_again,
                               bool *all_dead);

    /* === 튜플 수정 === */
    void (*tuple_insert) (Relation rel, TupleTableSlot *slot,
                          CommandId cid, int options,
                          BulkInsertState bistate);
    void (*tuple_insert_speculative) (Relation rel, TupleTableSlot *slot,
                                      CommandId cid, int options,
                                      BulkInsertState bistate,
                                      uint32 specToken);
    void (*tuple_complete_speculative) (Relation rel, TupleTableSlot *slot,
                                        uint32 specToken, bool succeeded);
    TM_Result (*tuple_delete) (Relation rel, ItemPointer tid,
                               CommandId cid, Snapshot snapshot,
                               Snapshot crosscheck, bool wait,
                               TM_FailureData *tmfd, bool changingPart);
    TM_Result (*tuple_update) (Relation rel, ItemPointer otid,
                               TupleTableSlot *slot, CommandId cid,
                               Snapshot snapshot, Snapshot crosscheck,
                               bool wait, TM_FailureData *tmfd,
                               LockTupleMode *lockmode,
                               TU_UpdateIndexes *update_indexes);
    TM_Result (*tuple_lock) (Relation rel, ItemPointer tid,
                             Snapshot snapshot, TupleTableSlot *slot,
                             CommandId cid, LockTupleMode mode,
                             LockWaitPolicy wait_policy, uint8 flags,
                             TM_FailureData *tmfd);

    /* === 유지보수 연산 === */
    void (*relation_vacuum) (Relation rel,
                             struct VacuumParams *params,
                             BufferAccessStrategy bstrategy);
    /* ... 추가 콜백들 ... */

} TableAmRoutine;
```

---

### 3. TableAmRoutine 콜백 함수

#### 3.1 콜백 함수 분류

- 슬롯 관리: 튜플 테이블 슬롯 연산 — `slot_callbacks`
- 스캔 초기화/제어: 테이블 스캔 시작/종료/재시작 — `scan_begin`, `scan_end`, `scan_rescan`
- 스캔 실행: 실제 튜플 가져오기 — `scan_getnextslot`, `scan_getnextslot_tidrange`
- 병렬 스캔: 병렬 쿼리 지원 — `parallelscan_estimate`, `parallelscan_initialize`
- 인덱스 패치: 인덱스를 통한 튜플 접근 — `index_fetch_begin`, `index_fetch_tuple`
- 튜플 검색: 특정 튜플 버전 검색 — `tuple_fetch_row_version`, `tuple_get_latest_tid`
- 튜플 수정: INSERT/UPDATE/DELETE — `tuple_insert`, `tuple_update`, `tuple_delete`
- 튜플 잠금: 행 수준 잠금 — `tuple_lock`
- 유지보수: VACUUM, 크기 계산 등 — `relation_vacuum`, `relation_size`
- 분석/샘플링: ANALYZE 지원 — `scan_analyze_next_block`, `scan_sample_next_tuple`

#### 3.2 슬롯 관리 콜백

```c
/*
 * slot_callbacks - 릴레이션에 대한 튜플 테이블 슬롯 연산 반환
 *
 * 접근 메서드별 튜플 처리를 위한 슬롯 타입을 정의합니다.
 */
const TupleTableSlotOps* (*slot_callbacks) (Relation rel);
```

튜플 테이블 슬롯은 실행기(executor)가 튜플에 대한 참조를 유지하고 컬럼 접근 기능을 제공하는 데 사용됨. 커스텀 접근 메서드 개발자는 일반적으로 AM 전용 튜플 테이블 슬롯 타입을 별도로 구현해야 함.

참조 파일: `src/include/executor/tuptable.h`

---

### 4. 스캔 API (Scan API)

#### 4.1 스캔 시작 함수

##### table_beginscan

```c
static TableScanDesc table_beginscan(
    Relation rel,           /* 스캔할 릴레이션 */
    Snapshot snapshot,      /* 가시성 판단용 스냅샷 */
    int nkeys,              /* 스캔 키 개수 */
    ScanKeyData *key        /* 스캔 키 배열 */
);
```

기본 옵션(전략 허용, 동기화 허용, 페이지 모드 허용)으로 순차 스캔 시작.

##### table_beginscan_catalog

```c
TableScanDesc table_beginscan_catalog(
    Relation relation,      /* 스캔할 카탈로그 릴레이션 */
    int nkeys,              /* 스캔 키 개수 */
    ScanKeyData *key        /* 스캔 키 배열 */
);
```

시스템 카탈로그 테이블 스캔을 위한 전용 함수. 스냅샷 등록 및 임시 스냅샷 처리를 자동으로 수행.

##### table_beginscan_strat

```c
static TableScanDesc table_beginscan_strat(
    Relation rel,           /* 스캔할 릴레이션 */
    Snapshot snapshot,      /* 가시성 판단용 스냅샷 */
    int nkeys,              /* 스캔 키 개수 */
    ScanKeyData *key,       /* 스캔 키 배열 */
    bool allow_strat,       /* 접근 전략 허용 여부 */
    bool allow_sync         /* 동기화 스캔 허용 여부 */
);
```

접근 전략과 동기화 스캔 옵션 직접 설정 가능.

#### 4.2 특수 스캔 함수

##### 비트맵 스캔

```c
static TableScanDesc table_beginscan_bm(
    Relation rel,
    Snapshot snapshot,
    int nkeys,
    ScanKeyData *key
);
```

비트맵 인덱스 스캔을 위한 테이블 스캔 시작.

##### TID 범위 스캔

```c
static TableScanDesc table_beginscan_tidrange(
    Relation rel,
    Snapshot snapshot,
    ItemPointer mintid,     /* 최소 TID */
    ItemPointer maxtid      /* 최대 TID */
);
```

특정 TID 범위 내의 튜플만 스캔.

##### 샘플링 스캔

```c
static TableScanDesc table_beginscan_sampling(
    Relation rel,
    Snapshot snapshot,
    int nkeys,
    ScanKeyData *key,
    bool allow_strat,       /* 접근 전략 허용 */
    bool allow_sync,        /* 동기화 스캔 허용 */
    bool allow_pagemode     /* 페이지 모드 허용 */
);
```

TABLESAMPLE 절을 위한 샘플링 스캔 시작.

#### 4.3 스캔 제어 함수

##### table_endscan

```c
static void table_endscan(TableScanDesc scan);
```

스캔을 종료하고 관련 리소스 해제.

##### table_rescan

```c
static void table_rescan(
    TableScanDesc scan,     /* 재시작할 스캔 */
    ScanKeyData *key        /* 새로운 스캔 키 (NULL 가능) */
);
```

스캔을 처음부터 다시 시작.

##### table_rescan_set_params

```c
static void table_rescan_set_params(
    TableScanDesc scan,
    ScanKeyData *key,
    bool allow_strat,
    bool allow_sync,
    bool allow_pagemode
);
```

파라미터를 재설정하면서 스캔 재시작.

#### 4.4 튜플 가져오기

##### table_scan_getnextslot

```c
static bool table_scan_getnextslot(
    TableScanDesc sscan,        /* 활성 스캔 디스크립터 */
    ScanDirection direction,    /* 스캔 방향 */
    TupleTableSlot *slot        /* 결과 튜플을 저장할 슬롯 */
);
```

활성 스캔에서 다음 튜플을 가져와 슬롯에 저장.

스캔 방향 (ScanDirection):
- `ForwardScanDirection`: 정방향 스캔
- `BackwardScanDirection`: 역방향 스캔
- `NoMovementScanDirection`: 현재 위치 유지

#### 4.5 스캔 사용 예제

```c
/* 전체 테이블 스캔 예제 */
void
scan_entire_table(Relation rel)
{
    TableScanDesc scan;
    TupleTableSlot *slot;
    Snapshot snapshot;

    /* 현재 스냅샷 가져오기 */
    snapshot = GetActiveSnapshot();

    /* 튜플 슬롯 생성 */
    slot = table_slot_create(rel, NULL);

    /* 스캔 시작 */
    scan = table_beginscan(rel, snapshot, 0, NULL);

    /* 모든 튜플 순회 */
    while (table_scan_getnextslot(scan, ForwardScanDirection, slot))
    {
        /* 튜플 처리 */
        bool isnull;
        Datum value = slot_getattr(slot, 1, &isnull);

        if (!isnull)
        {
            /* 값 처리 로직 */
        }
    }

    /* 스캔 종료 */
    table_endscan(scan);

    /* 슬롯 해제 */
    ExecDropSingleTupleTableSlot(slot);
}
```

---

### 5. 수정 API (Modification API)

#### 5.1 튜플 삽입 (Tuple Insert)

##### table_tuple_insert

```c
static void table_tuple_insert(
    Relation rel,               /* 대상 릴레이션 */
    TupleTableSlot *slot,       /* 삽입할 튜플이 담긴 슬롯 */
    CommandId cid,              /* 명령 ID */
    int options,                /* 삽입 옵션 플래그 */
    BulkInsertStateData *bistate /* 대량 삽입 상태 (NULL 가능) */
);
```

단일 튜플을 테이블에 삽입.

삽입 옵션 플래그:

- `TABLE_INSERT_SKIP_FSM`: Free Space Map 사용 건너뛰기
- `TABLE_INSERT_FROZEN`: 튜플을 frozen 상태로 삽입
- `TABLE_INSERT_NO_LOGICAL`: 논리적 디코딩 건너뛰기

##### table_tuple_insert_speculative

```c
static void table_tuple_insert_speculative(
    Relation rel,
    TupleTableSlot *slot,
    CommandId cid,
    int options,
    BulkInsertStateData *bistate,
    uint32 specToken           /* 추측적 삽입 토큰 */
);
```

추측적 삽입(speculative insert) 수행. `ON CONFLICT` 절을 지원하기 위해 사용됨.

##### table_tuple_complete_speculative

```c
static void table_tuple_complete_speculative(
    Relation rel,
    TupleTableSlot *slot,
    uint32 specToken,
    bool succeeded              /* 성공/실패 여부 */
);
```

추측적 삽입을 완료하거나 롤백.

#### 5.2 대량 삽입 (Multi Insert)

```c
static void table_multi_insert(
    Relation rel,
    TupleTableSlot **slots,   /* 삽입할 튜플 슬롯 배열 */
    int ntuples,                /* 튜플 개수 */
    CommandId cid,
    int options,
    BulkInsertStateData *bistate
);
```

여러 튜플을 한 번에 삽입 → 성능 최적화. COPY 명령에서 주로 사용됨.

#### 5.3 튜플 업데이트 (Tuple Update)

```c
static TM_Result table_tuple_update(
    Relation rel,
    ItemPointer otid,           /* 원본 튜플의 TID */
    TupleTableSlot *slot,       /* 새로운 튜플 데이터 */
    CommandId cid,
    Snapshot snapshot,          /* 가시성 스냅샷 */
    Snapshot crosscheck,        /* 교차 검증 스냅샷 */
    bool wait,                  /* 잠금 대기 여부 */
    TM_FailureData *tmfd,       /* 실패 정보 출력 */
    LockTupleMode *lockmode,    /* 잠금 모드 출력 */
    TU_UpdateIndexes *update_indexes /* 인덱스 업데이트 정보 출력 */
);
```

기존 튜플을 새로운 값으로 업데이트.

반환 값 (TM_Result):

- `TM_Ok`: 성공
- `TM_Invisible`: 튜플이 보이지 않음
- `TM_SelfModified`: 현재 트랜잭션에서 이미 수정됨
- `TM_Updated`: 다른 트랜잭션에서 업데이트됨
- `TM_Deleted`: 다른 트랜잭션에서 삭제됨
- `TM_BeingModified`: 현재 수정 중
- `TM_WouldBlock`: 잠금 대기가 필요하지만 wait=false

#### 5.4 튜플 삭제 (Tuple Delete)

```c
static TM_Result table_tuple_delete(
    Relation rel,
    ItemPointer tid,            /* 삭제할 튜플의 TID */
    CommandId cid,
    Snapshot snapshot,
    Snapshot crosscheck,
    bool wait,
    TM_FailureData *tmfd,
    bool changingPart           /* 파티션 이동 여부 */
);
```

지정된 튜플을 삭제.

#### 5.5 튜플 잠금 (Tuple Lock)

```c
static TM_Result table_tuple_lock(
    Relation rel,
    ItemPointer tid,
    Snapshot snapshot,
    TupleTableSlot *slot,
    CommandId cid,
    LockTupleMode mode,         /* 잠금 모드 */
    LockWaitPolicy wait_policy, /* 대기 정책 */
    uint8 flags,
    TM_FailureData *tmfd
);
```

튜플에 대한 잠금 획득.

잠금 모드 (LockTupleMode):

- `LockTupleKeyShare`: 키에 대한 공유 잠금
- `LockTupleShare`: 공유 잠금
- `LockTupleNoKeyExclusive`: 키 외 배타적 잠금
- `LockTupleExclusive`: 배타적 잠금

대기 정책 (LockWaitPolicy):

- `LockWaitBlock`: 잠금 획득까지 대기
- `LockWaitSkip`: 잠금 불가 시 건너뛰기
- `LockWaitError`: 잠금 불가 시 에러

#### 5.6 수정 API 사용 예제

```c
/* 단순 INSERT 예제 */
void
simple_insert_example(Relation rel, Datum *values, bool *nulls, int natts)
{
    TupleTableSlot *slot;
    TupleDesc tupdesc = RelationGetDescr(rel);

    /* 슬롯 생성 및 값 설정 */
    slot = table_slot_create(rel, NULL);
    ExecClearTuple(slot);

    for (int i = 0; i < natts; i++)
    {
        slot->tts_values[i] = values[i];
        slot->tts_isnull[i] = nulls[i];
    }

    ExecStoreVirtualTuple(slot);

    /* 튜플 삽입 */
    table_tuple_insert(rel,
                       slot,
                       GetCurrentCommandId(true),
                       0,           /* 옵션 없음 */
                       NULL);       /* 대량 삽입 상태 없음 */

    /* 슬롯 해제 */
    ExecDropSingleTupleTableSlot(slot);
}

/* 단순 UPDATE 예제 */
TM_Result
simple_update_example(Relation rel, ItemPointer tid, TupleTableSlot *newslot)
{
    TM_FailureData tmfd;
    LockTupleMode lockmode;
    TU_UpdateIndexes update_indexes;

    return table_tuple_update(rel,
                              tid,
                              newslot,
                              GetCurrentCommandId(true),
                              GetActiveSnapshot(),
                              InvalidSnapshot,
                              true,         /* 대기 허용 */
                              &tmfd,
                              &lockmode,
                              &update_indexes);
}

/* 단순 DELETE 예제 */
TM_Result
simple_delete_example(Relation rel, ItemPointer tid)
{
    TM_FailureData tmfd;

    return table_tuple_delete(rel,
                              tid,
                              GetCurrentCommandId(true),
                              GetActiveSnapshot(),
                              InvalidSnapshot,
                              true,         /* 대기 허용 */
                              &tmfd,
                              false);       /* 파티션 이동 아님 */
}
```

---

### 6. 인덱스 관련 연산 (Index Related Operations)

#### 6.1 인덱스 패치 콜백

인덱스를 통해 테이블 튜플에 접근하기 위한 콜백들.

```c
/* 인덱스 패치 시작 */
IndexFetchTableData* (*index_fetch_begin) (Relation rel);

/* 인덱스 패치 리셋 */
void (*index_fetch_reset) (IndexFetchTableData *data);

/* 인덱스 패치 종료 */
void (*index_fetch_end) (IndexFetchTableData *data);

/* 인덱스를 통한 튜플 패치 */
bool (*index_fetch_tuple) (
    struct IndexFetchTableData *scan,
    ItemPointer tid,            /* 인덱스에서 얻은 TID */
    Snapshot snapshot,
    TupleTableSlot *slot,
    bool *call_again,           /* 다시 호출 필요 여부 */
    bool *all_dead              /* 모든 버전이 죽었는지 여부 */
);
```

#### 6.2 인덱스 패치 사용 예제

```c
/* 인덱스를 통한 튜플 조회 */
bool
fetch_tuple_via_index(Relation rel, ItemPointer tid, TupleTableSlot *slot)
{
    IndexFetchTableData *fetch;
    bool found;
    bool call_again = false;
    bool all_dead = false;
    Snapshot snapshot = GetActiveSnapshot();

    /* 인덱스 패치 초기화 */
    fetch = table_index_fetch_begin(rel);

    /* 튜플 패치 */
    found = table_index_fetch_tuple(fetch,
                                    tid,
                                    snapshot,
                                    slot,
                                    &call_again,
                                    &all_dead);

    /* 인덱스 패치 종료 */
    table_index_fetch_end(fetch);

    return found;
}
```

#### 6.3 인덱스 삭제 지원

```c
/* 인덱스 삭제 튜플 처리 */
TransactionId (*index_delete_tuples) (
    Relation rel,
    TM_IndexDeleteOp *delstate  /* 삭제 연산 상태 */
);
```

인덱스 VACUUM 시 죽은 튜플 처리.

#### 6.4 인덱스 빌드 지원

```c
/* 인덱스 생성을 위한 테이블 스캔 */
double (*index_build_range_scan) (
    Relation table_rel,
    Relation index_rel,
    IndexInfo *index_info,
    bool allow_sync,
    bool anyvisible,
    bool progress,
    BlockNumber start_blockno,
    BlockNumber numblocks,
    IndexBuildCallback callback,
    void *callback_state,
    TableScanDesc scan
);

/* 인덱스 유효성 검증 스캔 */
void (*index_validate_scan) (
    Relation table_rel,
    Relation index_rel,
    IndexInfo *index_info,
    Snapshot snapshot,
    ValidateIndexState *state
);
```

---

### 7. 유지보수 연산 (Maintenance Operations)

#### 7.1 VACUUM 지원

```c
void (*relation_vacuum) (
    Relation rel,
    struct VacuumParams *params,    /* VACUUM 파라미터 */
    BufferAccessStrategy bstrategy  /* 버퍼 접근 전략 */
);
```

테이블에 대한 VACUUM 연산 수행. 죽은 튜플 정리, 통계 갱신 등을 처리.

#### 7.2 릴레이션 크기

```c
uint64 (*relation_size) (
    Relation rel,
    ForkNumber forkNumber   /* 포크 종류 */
);
```

릴레이션의 크기를 바이트 단위로 반환.

포크 종류 (ForkNumber):

- `MAIN_FORKNUM`: 메인 데이터 포크
- `FSM_FORKNUM`: Free Space Map
- `VISIBILITYMAP_FORKNUM`: Visibility Map
- `INIT_FORKNUM`: 초기화 포크

#### 7.3 크기 추정

```c
void (*relation_estimate_size) (
    Relation rel,
    int32 *attr_widths,
    BlockNumber *pages,         /* 페이지 수 출력 */
    double *tuples,             /* 튜플 수 출력 */
    double *allvisfrac          /* 모두 가시적인 비율 출력 */
);
```

플래너를 위한 릴레이션 크기 추정 정보 제공.

#### 7.4 TOAST 지원

```c
/* TOAST 테이블 필요 여부 */
bool (*relation_needs_toast_table) (Relation rel);

/* TOAST 접근 메서드 */
Oid (*relation_toast_am) (Relation rel);

/* TOAST 데이터 조각 패치 */
void (*relation_fetch_toast_slice) (
    Relation toastrel,
    Oid valueid,
    int32 attrsize,
    int32 sliceoffset,
    int32 slicelength,
    struct varlena *result
);
```

TOAST(The Oversized-Attribute Storage Technique)를 통한 대형 값 처리 지원.

#### 7.5 파일 위치 변경

```c
void (*relation_set_new_filelocator) (
    Relation rel,
    const RelFileLocator *newrlocator,
    char persistence,
    TransactionId *freezeXid,
    MultiXactId *minmulti
);
```

TRUNCATE, CLUSTER 등의 연산 시 새로운 파일 위치 설정.

#### 7.6 비트랜잭션 TRUNCATE

```c
void (*relation_nontransactional_truncate) (Relation rel);
```

트랜잭션 로깅 없이 릴레이션을 잘라냄. 주로 초기화 목적으로 사용됨.

#### 7.7 데이터 복사

```c
/* 릴레이션 데이터 복사 */
void (*relation_copy_data) (
    Relation rel,
    const RelFileLocator *newrlocator
);

/* 클러스터링을 위한 데이터 복사 */
void (*relation_copy_for_cluster) (
    Relation OldTable,
    Relation NewTable,
    Relation OldIndex,
    bool use_sort,
    TransactionId OldestXmin,
    TransactionId *xid_cutoff,
    MultiXactId *multi_cutoff,
    double *num_tuples,
    double *tups_vacuumed,
    double *tups_recently_dead
);
```

---

### 8. 구현 요구사항 (Implementation Requirements)

#### 8.1 TID 요구사항

테이블 접근 메서드가 수정 또는 인덱스를 지원하려면, 각 튜플이 튜플 식별자(TID, Tuple Identifier)를 가져야 함.

TID 구성:
- 블록 번호(Block Number): 튜플이 저장된 페이지 번호
- 항목 번호(Item Number): 페이지 내 튜플의 위치

```c
typedef struct ItemPointerData
{
    BlockIdData ip_blkid;   /* 블록 번호 */
    OffsetNumber ip_posid;  /* 항목 번호 */
} ItemPointerData;

typedef ItemPointerData *ItemPointer;
```

#### 8.2 비트맵 스캔 지원

비트맵 스캔을 지원하려면 블록 번호가 지역성(locality)을 제공해야 함. 즉, 인접한 블록 번호는 물리적으로 인접한 저장 위치를 나타내야 함.

#### 8.3 튜플 테이블 슬롯

커스텀 접근 메서드 개발자는 일반적으로 AM 전용 튜플 테이블 슬롯 타입을 별도로 구현해야 함.

참조 파일: `src/include/executor/tuptable.h`

```c
/* 튜플 테이블 슬롯 연산 구조체 */
typedef struct TupleTableSlotOps
{
    /* 슬롯 초기화 */
    void (*init) (TupleTableSlot *slot);
    /* 슬롯 해제 */
    void (*release) (TupleTableSlot *slot);
    /* 튜플 지우기 */
    void (*clear) (TupleTableSlot *slot);
    /* 속성 값 가져오기 */
    void (*getsomeattrs) (TupleTableSlot *slot, int natts);
    /* NULL 비트맵 가져오기 */
    void (*getsysattr) (TupleTableSlot *slot, int attnum,
                        bool *isnull);
    /* 최소 튜플 복사 */
    void (*materialize) (TupleTableSlot *slot);
    /* HeapTuple 복사본 생성 */
    HeapTuple (*copy_heap_tuple) (TupleTableSlot *slot);
    /* MinimalTuple 복사본 생성 */
    MinimalTuple (*copy_minimal_tuple) (TupleTableSlot *slot);
} TupleTableSlotOps;
```

#### 8.4 저장소 유연성

테이블 접근 메서드는 저장소 구현을 유연하게 가져갈 수 있음:

- 공유 버퍼 캐시(선택): PostgreSQL의 공유 버퍼 캐시 사용 가능하지만 필수 아님
- 표준 페이지 레이아웃(선택): `src/include/storage/bufpage.h`의 레이아웃 사용 가능
- 커스텀 저장소(허용): 완전히 다른 저장 방식 구현 가능

#### 8.5 충돌 안전성 (Crash Safety)

테이블 접근 메서드는 충돌 안전성을 보장하기 위해 다음 중 하나를 사용 가능:

##### 방법 1: PostgreSQL WAL 사용

```c
/* Generic WAL Records 사용 예제 */
#include "access/generic_xlog.h"

void
my_am_insert_with_wal(Relation rel, Buffer buffer, ...)
{
    GenericXLogState *state;
    Page page;

    /* Generic WAL 상태 시작 */
    state = GenericXLogStart(rel);

    /* 버퍼 등록 및 페이지 수정 */
    page = GenericXLogRegisterBuffer(state, buffer, 0);

    /* 페이지 수정 작업 수행 */
    /* ... */

    /* WAL 레코드 기록 및 완료 */
    GenericXLogFinish(state);
}
```

##### 방법 2: 커스텀 WAL 리소스 매니저

```c
/* 커스텀 WAL 리소스 매니저 등록 */
#include "access/xlog.h"
#include "access/xlog_internal.h"

static const RmgrData my_rmgr = {
    .rm_name = "my_tableam",
    .rm_redo = my_redo,
    .rm_desc = my_desc,
    .rm_identify = my_identify,
    .rm_startup = NULL,
    .rm_cleanup = NULL,
    .rm_mask = my_mask,
    .rm_decode = NULL
};

void
_PG_init(void)
{
    RegisterCustomRmgr(MY_RMGR_ID, &my_rmgr);
}
```

##### 방법 3: 완전 커스텀 구현

자체적인 충돌 복구 메커니즘 구현도 가능.

#### 8.6 트랜잭션 지원

단일 트랜잭션 내에서 여러 접근 메서드 간의 크로스-AM 트랜잭션을 지원하려면 `src/backend/access/transam/xlog.c`의 메커니즘과 긴밀하게 통합 필요.

#### 8.7 참조 구현

새로운 테이블 접근 메서드를 개발할 때는 기존 `heap` 구현 참고 권장.

참조 파일: `src/backend/access/heap/heapam_handler.c`

```c
/* heap 접근 메서드 핸들러 예제 (간략화) */
const TableAmRoutine heapam_methods = {
    .type = T_TableAmRoutine,

    .slot_callbacks = heapam_slot_callbacks,

    .scan_begin = heap_beginscan,
    .scan_end = heap_endscan,
    .scan_rescan = heap_rescan,
    .scan_getnextslot = heap_getnextslot,

    .parallelscan_estimate = table_block_parallelscan_estimate,
    .parallelscan_initialize = table_block_parallelscan_initialize,
    .parallelscan_reinitialize = table_block_parallelscan_reinitialize,

    .index_fetch_begin = heapam_index_fetch_begin,
    .index_fetch_reset = heapam_index_fetch_reset,
    .index_fetch_end = heapam_index_fetch_end,
    .index_fetch_tuple = heapam_index_fetch_tuple,

    .tuple_insert = heapam_tuple_insert,
    .tuple_insert_speculative = heapam_tuple_insert_speculative,
    .tuple_complete_speculative = heapam_tuple_complete_speculative,
    .multi_insert = heap_multi_insert,
    .tuple_delete = heapam_tuple_delete,
    .tuple_update = heapam_tuple_update,
    .tuple_lock = heapam_tuple_lock,

    .tuple_fetch_row_version = heapam_fetch_row_version,
    .tuple_get_latest_tid = heap_get_latest_tid,
    .tuple_tid_valid = heapam_tuple_tid_valid,
    .tuple_satisfies_snapshot = heapam_tuple_satisfies_snapshot,

    .relation_set_new_filelocator = heapam_relation_set_new_filelocator,
    .relation_nontransactional_truncate = heapam_relation_nontransactional_truncate,
    .relation_copy_data = heapam_relation_copy_data,
    .relation_copy_for_cluster = heapam_relation_copy_for_cluster,
    .relation_vacuum = heap_vacuum_rel,
    .relation_size = table_block_relation_size,
    .relation_needs_toast_table = heapam_relation_needs_toast_table,
    .relation_estimate_size = heapam_estimate_rel_size,

    /* ... 추가 콜백들 ... */
};
```

---

### 참고 자료

#### 공식 문서
- [PostgreSQL Documentation: Table Access Method Interface Definition](https://www.postgresql.org/docs/current/tableam.html)

#### 소스 코드
- `src/include/access/tableam.h` - TableAmRoutine 구조체 정의
- `src/include/executor/tuptable.h` - 튜플 테이블 슬롯 정의
- `src/backend/access/heap/heapam_handler.c` - heap 접근 메서드 구현

#### 관련 문서
- Chapter 64: 인덱스 접근 메서드 인터페이스 (Index Access Method Interface)
- Chapter 28: WAL (Write-Ahead Logging)
- Chapter 66.6: 데이터베이스 페이지 레이아웃 (Database Page Layout)

---

## PostgreSQL WAL 내부 구현 (WAL Internals)

### 개요

WAL은 PostgreSQL의 데이터 무결성과 복구 메커니즘의 핵심.

---

### 1. WAL 기본 원리

#### 1.1 Write-Ahead Logging 개념

WAL의 핵심 원칙:

> 데이터 파일(테이블과 인덱스가 저장되는 곳)에 대한 변경은 해당 변경을 설명하는 WAL 레코드가 영구 저장소에 플러시된 후에만 기록 가능.

이 원칙을 통해 다음과 같은 이점을 얻음:

- 트랜잭션 커밋 시 데이터 페이지를 플러시할 필요가 없음: WAL 레코드가 먼저 기록되므로 데이터 페이지를 즉시 플러시하지 않아도 됨
- 충돌 복구(Crash Recovery): 충돌 시 WAL 레코드를 재생하여 미적용 변경사항을 복구 (roll-forward recovery 또는 REDO)

#### 1.2 성능상의 이점

```
[기존 방식]
트랜잭션 커밋 → 모든 수정된 데이터 페이지 디스크에 플러시 → 느림

[WAL 방식]
트랜잭션 커밋 → WAL 파일만 디스크에 플러시 → 빠름
                 (순차 쓰기, 단일 fsync)
```

주요 이점:
- 디스크 쓰기 횟수 대폭 감소
- 순차 쓰기로 sync 작업 속도 향상
- 여러 작은 트랜잭션을 단일 `fsync`로 커밋 가능

---

### 2. WAL 저장 구조

#### 2.1 WAL 세그먼트 파일

WAL 파일은 데이터 디렉토리 아래 `pg_wal` 디렉토리에 저장됨.

```
$PGDATA/pg_wal/
├── 000000010000000000000001
├── 000000010000000000000002
├── 000000010000000000000003
└── ...
```

세그먼트 파일 특성:
- 기본 크기: 16MB (initdb의 `--wal-segsize` 옵션으로 구성 가능)
- 순차적으로 번호가 매겨짐
- 번호는 순환하지 않음 (고갈되기까지 매우 오랜 시간이 걸림)

#### 2.2 WAL 페이지 구조

각 세그먼트 파일은 페이지로 나뉨:

```
┌─────────────────────────────────────────────────────────────────┐
│                    WAL 세그먼트 파일 (16MB)                      │
├─────────────────────────────────────────────────────────────────┤
│  페이지 0    │  페이지 1    │  페이지 2    │ ... │  페이지 N    │
│  (8KB)       │  (8KB)       │  (8KB)       │     │  (8KB)       │
└─────────────────────────────────────────────────────────────────┘
```

- 페이지 크기: 기본 8KB (`--with-wal-blocksize` configure 옵션으로 구성)

#### 2.3 페이지 헤더 구조

첫 번째 페이지: XLogLongPageHeaderData

```c
typedef struct XLogLongPageHeaderData
{
    XLogPageHeaderData std;        /* 표준 헤더 필드 */
    uint64      xlp_sysid;         /* pg_control의 시스템 식별자 */
    uint32      xlp_seg_size;      /* 교차 확인 값 */
    uint32      xlp_xlog_blcksz;   /* 교차 확인 값 */
} XLogLongPageHeaderData;
```

이후 페이지: XLogPageHeaderData

```c
typedef struct XLogPageHeaderData
{
    uint16      xlp_magic;         /* 매직 값 (0xD113) - 정확성 검증용 */
    uint16      xlp_info;          /* 플래그 비트 */
    TimeLineID  xlp_tli;           /* 페이지의 첫 레코드의 TimeLineID */
    XLogRecPtr  xlp_pageaddr;      /* 현재 페이지의 XLOG 주소 */
    uint32      xlp_rem_len;       /* 이전 페이지에서 이어지는 바이트 수 */
} XLogPageHeaderData;
```

---

### 3. Log Sequence Number (LSN)

#### 3.1 LSN 개념

LSN은 WAL의 바이트 오프셋을 나타내는 단조 증가하는 값.

```c
typedef uint64 XLogRecPtr;  /* LSN을 나타내는 타입 */
```

LSN 용도:
- WAL 위치 간의 데이터 양 계산
- 복제 진행 상황 측정
- 복구 위치 추적

#### 3.2 LSN 데이터 타입

PostgreSQL에서 LSN은 `pg_lsn` 데이터 타입으로 표현됨:

```sql
-- 현재 WAL 위치 확인
SELECT pg_current_wal_lsn();

-- 두 LSN 간의 차이 계산 (바이트 단위)
SELECT pg_wal_lsn_diff('0/1A2B3C4D', '0/1A2B3C00');

-- 페이지의 LSN 확인
SELECT lsn FROM page_header(get_raw_page('mytable', 0));
```

---

### 4. XLOG 레코드 형식 (XLOG Record Format)

#### 4.1 전체 레코드 레이아웃

XLOG 레코드의 전체 구조는 `access/xlogrecord.h`에 정의됨:

```
┌─────────────────────────────────────────────────────────────────┐
│                      XLOG 레코드 전체 구조                        │
├─────────────────────────────────────────────────────────────────┤
│  고정 크기 헤더 (XLogRecord)                                     │
├─────────────────────────────────────────────────────────────────┤
│  XLogRecordBlockHeader (0개 이상)                                │
├─────────────────────────────────────────────────────────────────┤
│  XLogRecordDataHeader[Short|Long] (0개 또는 1개)                 │
├─────────────────────────────────────────────────────────────────┤
│  블록 데이터 (block data)                                        │
├─────────────────────────────────────────────────────────────────┤
│  메인 데이터 (main data)                                         │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.2 XLogRecord 구조체

모든 XLOG 레코드의 헤더를 정의하는 핵심 구조체:

```c
typedef struct XLogRecord
{
    uint32      xl_tot_len;    /* 전체 레코드 길이 */
    TransactionId xl_xid;      /* 트랜잭션 ID */
    XLogRecPtr  xl_prev;       /* 이전 레코드에 대한 포인터 */
    uint8       xl_info;       /* 플래그 비트 (리소스 관리자별 정보) */
    RmgrId      xl_rmid;       /* 리소스 관리자 식별자 */
    /* 2바이트 패딩 */
    pg_crc32c   xl_crc;        /* CRC-32C 체크섬 */
} XLogRecord;
```

필드 설명:

- `xl_tot_len`: 레코드 전체 길이(헤더 + 데이터)
- `xl_xid`: 레코드를 생성한 트랜잭션 ID
- `xl_prev`: 이전 WAL 레코드의 위치(연결 리스트 형태)
- `xl_info`: 작업 유형을 나타내는 플래그 비트
- `xl_rmid`: 리소스 관리자 식별자
- `xl_crc`: 데이터 무결성을 위한 CRC-32C 체크섬

#### 4.3 XLogRecordBlockHeader 구조체

블록 참조를 설명하는 구조체:

```c
typedef struct XLogRecordBlockHeader
{
    uint8       id;             /* 블록 참조 ID */
    uint8       fork_flags;     /* 포크 번호 및 플래그 */
    uint16      data_length;    /* 페이지 이미지를 제외한 페이로드 크기 */
    /* BKPBLOCK_HAS_IMAGE인 경우 XLogRecordBlockImageHeader가 뒤따름 */
    /* BKPBLOCK_SAME_REL이 아닌 경우 RelFileLocator가 뒤따름 */
    /* BlockNumber가 항상 뒤따름 */
} XLogRecordBlockHeader;
```

플래그 정의:

```c
#define BKPBLOCK_FORK_MASK   0x0F  /* 포크 비트 마스크 */
#define BKPBLOCK_FLAG_MASK   0xF0  /* 플래그 비트 마스크 */
#define BKPBLOCK_HAS_IMAGE   0x10  /* 전체 페이지 이미지 포함 */
#define BKPBLOCK_HAS_DATA    0x20  /* 데이터 포함 */
#define BKPBLOCK_WILL_INIT   0x40  /* 복구 시 페이지 재초기화 */
#define BKPBLOCK_SAME_REL    0x80  /* 이전 블록과 동일한 릴레이션 */
```

#### 4.4 XLogRecordBlockImageHeader 구조체

전체 페이지 이미지(Full Page Image, FPI)를 저장할 때 사용:

```c
typedef struct XLogRecordBlockImageHeader
{
    uint16      length;        /* 페이지 이미지 바이트 수 */
    uint16      hole_offset;   /* "hole" 이전의 바이트 수 */
    uint8       bimg_info;     /* 압축 및 상태 플래그 */
} XLogRecordBlockImageHeader;
```

bimg_info 플래그:

```c
#define BKPIMAGE_HAS_HOLE     0x01  /* 페이지에 hole이 있음 */
#define BKPIMAGE_IS_COMPRESSED 0x02  /* 이미지가 압축됨 */
#define BKPIMAGE_APPLY        0x04  /* 복구 시 적용해야 함 */
```

#### 4.5 압축 헤더 구조체

압축이 적용된 경우:

```c
typedef struct XLogRecordBlockCompressHeader
{
    uint16      hole_length;   /* 사용되지 않는 섹션의 바이트 수 */
} XLogRecordBlockCompressHeader;
```

지원되는 압축 방식:
- PGLZ (PostgreSQL 기본 압축)
- LZ4
- ZSTD

#### 4.6 데이터 헤더 구조체

짧은 형식 (Short):

```c
typedef struct XLogRecordDataHeaderShort
{
    uint8       id;           /* XLR_BLOCK_ID_DATA_SHORT */
    uint8       data_length;  /* 1바이트 길이 */
} XLogRecordDataHeaderShort;
```

긴 형식 (Long):

```c
typedef struct XLogRecordDataHeaderLong
{
    uint8       id;           /* XLR_BLOCK_ID_DATA_LONG */
    /* 4바이트 길이가 뒤따름 */
} XLogRecordDataHeaderLong;
```

---

### 5. WAL 리소스 관리자 (Resource Managers)

#### 5.1 리소스 관리자 개념

리소스 관리자(Resource Manager, rmgr)는 WAL 기능과 관련된 작업 집합. 각 리소스 관리자는 특정 유형의 WAL 레코드 작성과 재생을 담당.

#### 5.2 내장 리소스 관리자

PostgreSQL에 포함된 주요 리소스 관리자:

- `RM_XLOG`: XLOG 자체 관리(체크포인트 등)
- `RM_XACT`: 트랜잭션 관리
- `RM_SMGR`: 스토리지 관리자
- `RM_CLOG`: 커밋 로그
- `RM_DBASE`: 데이터베이스 작업
- `RM_TBLSPC`: 테이블스페이스
- `RM_MULTIXACT`: 다중 트랜잭션
- `RM_RELMAP`: 릴레이션 맵
- `RM_STANDBY`: 스탠바이 관련
- `RM_HEAP`: 힙 테이블 작업(INSERT, UPDATE, DELETE)
- `RM_HEAP2`: 힙 테이블 추가 작업(VACUUM 등)
- `RM_BTREE`: B-tree 인덱스
- `RM_HASH`: 해시 인덱스
- `RM_GIN`: GIN 인덱스
- `RM_GIST`: GiST 인덱스
- `RM_SEQ`: 시퀀스
- `RM_SPGIST`: SP-GiST 인덱스
- `RM_BRIN`: BRIN 인덱스

#### 5.3 RmgrData 구조체

각 리소스 관리자는 `RmgrData` 구조체로 정의됨:

```c
typedef struct RmgrData
{
    const char *rm_name;                                    /* 리소스 관리자 이름 */
    void        (*rm_redo) (XLogReaderState *record);       /* REDO 콜백 함수 */
    void        (*rm_desc) (StringInfo buf,
                            XLogReaderState *record);       /* 레코드 설명 */
    const char *(*rm_identify) (uint8 info);                /* 레코드 이름 반환 */
    void        (*rm_startup) (void);                       /* 시작 시 호출 */
    void        (*rm_cleanup) (void);                       /* 정리 함수 */
    void        (*rm_mask) (char *pagedata,
                            BlockNumber blkno);             /* 페이지 마스킹 */
    void        (*rm_decode) (struct LogicalDecodingContext *ctx,
                              struct XLogRecordBuffer *buf); /* 논리적 디코딩 */
} RmgrData;
```

주요 콜백 함수:

- `rm_redo`: 복구 시 WAL 레코드 적용
- `rm_desc`: 레코드에 대한 추가 세부 정보 제공
- `rm_identify`: xl_info 기반으로 레코드 이름 반환
- `rm_startup`: 시작 시 초기화
- `rm_cleanup`: 정리 작업
- `rm_mask`: `wal_consistency_checking`에서 플래그 제외할 비트 마스킹
- `rm_decode`: 사용자 정의 WAL 레코드의 논리적 디코딩 처리

#### 5.4 사용자 정의 리소스 관리자

PostgreSQL 15부터 확장은 자체 사용자 정의 리소스 관리자 등록 가능:

```c
/* 사용자 정의 리소스 관리자 등록 */
extern void RegisterCustomRmgr(RmgrId rmid, const RmgrData *rmgr);
```

등록 요구사항:

1. 확장의 `_PG_init()` 함수에서 호출해야 함
2. 고유한 리소스 관리자 ID 사용 (개발 시 `RM_EXPERIMENTAL_ID` 사용)
3. `shared_preload_libraries`에 추가하여 서버 시작 시 조기 로딩

예제 코드:

```c
#include "access/xlog.h"
#include "access/xlog_internal.h"

/* 리소스 관리자 정의 */
static const RmgrData my_rmgr = {
    .rm_name = "my_extension",
    .rm_redo = my_redo,
    .rm_desc = my_desc,
    .rm_identify = my_identify,
    .rm_startup = NULL,
    .rm_cleanup = NULL,
    .rm_mask = NULL,
    .rm_decode = my_decode
};

/* REDO 콜백 구현 */
static void
my_redo(XLogReaderState *record)
{
    uint8 info = XLogRecGetInfo(record) & ~XLR_INFO_MASK;

    switch (info)
    {
        case MY_XLOG_INSERT:
            /* INSERT 작업 재생 */
            break;
        case MY_XLOG_UPDATE:
            /* UPDATE 작업 재생 */
            break;
        /* ... */
    }
}

/* 확장 초기화 */
void
_PG_init(void)
{
    RegisterCustomRmgr(MY_RMGR_ID, &my_rmgr);
}
```

---

### 6. WAL 레코드 작성 (Writing XLOG Records)

#### 6.1 XLogInsert 프로세스

WAL 레코드 작성 과정:

```
┌─────────────────────────────────────────────────────────────────┐
│                    WAL 레코드 작성 흐름                           │
├─────────────────────────────────────────────────────────────────┤
│  1. heap_insert() 호출                                           │
│          ↓                                                       │
│  2. XLogBeginInsert() - 레코드 구성 시작                         │
│          ↓                                                       │
│  3. XLogRegisterData() - 메인 데이터 등록                        │
│          ↓                                                       │
│  4. XLogRegisterBuffer() - 버퍼 등록                             │
│          ↓                                                       │
│  5. XLogInsert() - WAL 버퍼에 레코드 작성                        │
│          ↓                                                       │
│  6. 페이지의 pd_lsn 업데이트                                     │
└─────────────────────────────────────────────────────────────────┘
```

#### 6.2 WAL 버퍼

WAL 레코드는 먼저 공유 메모리의 WAL 버퍼에 기록됨:

```c
/* WAL 버퍼 크기 설정 */
wal_buffers = 64MB  /* 기본값: -1 (자동 계산) */
```

WAL 버퍼 플러시 조건:
- 트랜잭션 커밋 또는 중단
- WAL 버퍼가 가득 참
- WAL writer 프로세스가 주기적으로 실행

#### 6.3 XLogFlush 프로세스

```c
/* WAL 플러시 메소드 */
wal_sync_method = fdatasync  /* 기본값 */
```

지원되는 동기화 메소드:

- `open_datasync`: O_DSYNC로 WAL 파일 열기
- `fdatasync`: 커밋마다 fdatasync() 호출
- `fsync`: 커밋마다 fsync() 호출
- `fsync_writethrough`: 커밋마다 fsync() 호출(디스크 캐시 우회)
- `open_sync`: O_SYNC로 WAL 파일 열기

#### 6.4 INSERT 예제 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  INSERT INTO mytable VALUES (1, 'test');                         │
├─────────────────────────────────────────────────────────────────┤
│  1. ExtendCLOG()                                                 │
│     └─ 트랜잭션 상태를 "IN_PROGRESS"로 표시                      │
│                                                                  │
│  2. heap_insert()                                                │
│     └─ XLOG 레코드 생성                                          │
│     └─ XLogInsert()로 WAL 버퍼에 기록 (LSN_1)                    │
│     └─ 페이지의 pd_lsn을 LSN_1으로 업데이트                      │
│                                                                  │
│  3. finish_xact_command()                                        │
│     └─ 커밋 레코드 생성                                          │
│     └─ XLogInsert()로 WAL 버퍼에 기록 (LSN_2)                    │
│                                                                  │
│  4. XLogFlush()                                                  │
│     └─ WAL 버퍼의 모든 레코드를 WAL 세그먼트 파일에 플러시        │
│                                                                  │
│  5. TransactionIdCommitTree()                                    │
│     └─ CLOG에서 트랜잭션 상태를 "COMMITTED"로 변경               │
└─────────────────────────────────────────────────────────────────┘
```

---

### 7. 전체 페이지 쓰기 (Full Page Writes)

#### 7.1 개념

전체 페이지 쓰기(Full Page Writes, FPW)는 체크포인트 이후 페이지가 처음 변경될 때 전체 페이지 이미지를 WAL에 기록하는 기능.

```c
/* 전체 페이지 쓰기 활성화 (기본값: on) */
full_page_writes = on
```

#### 7.2 부분 페이지 쓰기 문제

PostgreSQL은 일반적으로 8KB(16개의 512바이트 섹터) 페이지를 한 번에 씀. 전원 손실 시:

```
┌─────────────────────────────────────────────────────────────────┐
│  8KB 페이지 (16 섹터)                                            │
├────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┤
│ S1 │ S2 │ S3 │ S4 │ S5 │ S6 │ S7 │ S8 │ S9 │... │S15 │S16 │    │
├────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┤
│  ✓    ✓    ✓    ✓    ✓    ✗    ✗    ✗    ✗        ✗    ✗       │
│  ^^^^^^^^^^^^^^^^^^^^^^^^^                                       │
│  쓰기 완료                   전원 손실로 쓰기 실패                │
└─────────────────────────────────────────────────────────────────┘
```

해결책: WAL에 전체 페이지 이미지를 저장하여 복구 시 부분적으로 기록된 페이지를 복원

#### 7.3 백업 블록 (Backup Block)

체크포인트 후 첫 번째 변경 시:

```c
/* WAL 레코드에 전체 페이지 이미지 포함 */
typedef struct XLogRecordBlockImageHeader
{
    uint16      length;        /* 페이지 이미지 바이트 수 */
    uint16      hole_offset;   /* "hole" 이전 바이트 수 */
    uint8       bimg_info;     /* 플래그 */
} XLogRecordBlockImageHeader;
```

---

### 8. 체크포인트 처리 (Checkpoint Processing)

#### 8.1 체크포인트 발생 조건

체크포인터(checkpointer) 백그라운드 프로세스가 체크포인트를 시작하는 경우:

1. `checkpoint_timeout` 간격 만료 (기본값: 5분)
2. WAL 파일 총 크기가 `max_wal_size` 초과 (기본값: 1GB)
3. 서버가 smart 또는 fast 모드로 중지
4. 슈퍼유저가 수동으로 `CHECKPOINT` 명령 실행

#### 8.2 체크포인트 처리 단계

```
┌─────────────────────────────────────────────────────────────────┐
│                     체크포인트 처리 단계                          │
├─────────────────────────────────────────────────────────────────┤
│  1. REDO 포인트 저장                                             │
│     └─ 체크포인트 시작 시점의 XLOG 레코드 위치                    │
│                                                                  │
│  2. 공유 메모리 플러시                                           │
│     └─ CLOG 등 모든 데이터를 스토리지에 전송                      │
│                                                                  │
│  3. 더티 페이지 쓰기                                             │
│     └─ 버퍼의 더티 페이지를 점진적으로 디스크에 기록              │
│                                                                  │
│  4. 체크포인트 레코드 생성                                       │
│     └─ CheckPoint 구조체를 WAL 버퍼에 기록                       │
│                                                                  │
│  5. pg_control 업데이트                                          │
│     └─ 체크포인트 위치 및 기본 정보 기록                          │
└─────────────────────────────────────────────────────────────────┘
```

#### 8.3 CheckPoint 구조체

```c
typedef struct CheckPoint
{
    XLogRecPtr  redo;              /* REDO 시작 위치 */
    TimeLineID  ThisTimeLineID;   /* 현재 타임라인 ID */
    TimeLineID  PrevTimeLineID;   /* 이전 타임라인 ID */
    bool        fullPageWrites;   /* 전체 페이지 쓰기 활성화 여부 */
    FullTransactionId nextXid;    /* 다음 트랜잭션 ID */
    Oid         nextOid;          /* 다음 OID */
    MultiXactId nextMulti;        /* 다음 MultiXactId */
    MultiXactOffset nextMultiOffset;
    TransactionId oldestXid;      /* 가장 오래된 트랜잭션 ID */
    Oid         oldestXidDB;      /* oldestXid가 속한 DB */
    MultiXactId oldestMulti;      /* 가장 오래된 MultiXactId */
    Oid         oldestMultiDB;    /* oldestMulti가 속한 DB */
    pg_time_t   time;             /* 체크포인트 시간 */
    TransactionId oldestCommitTsXid;
    TransactionId newestCommitTsXid;
    TransactionId oldestActiveXid;
} CheckPoint;
```

#### 8.4 pg_control 파일

`pg_control` 파일은 데이터베이스 복구에 필수적인 정보를 담고 있음:

```bash
# pg_controldata로 확인
$ pg_controldata $PGDATA

pg_control version number:            1300
Catalog version number:               202107181
Database system identifier:           7123456789012345678
Database cluster state:               in production
Latest checkpoint location:           0/1A2B3C4D
Latest checkpoint's REDO location:    0/1A2B3C00
Latest checkpoint's TimeLineID:       1
...
```

---

### 9. 데이터베이스 복구 (Database Recovery)

#### 9.1 복구 프로세스 개요

PostgreSQL은 REDO 로그 기반 복구를 사용함:

```
┌─────────────────────────────────────────────────────────────────┐
│                      복구 프로세스 흐름                           │
├─────────────────────────────────────────────────────────────────┤
│  1. pg_control 파일 읽기                                         │
│     └─ 상태가 "in production"이면 복구 모드 활성화               │
│     └─ 상태가 "shut down"이면 정상 시작                          │
│                                                                  │
│  2. 최신 체크포인트 레코드 읽기                                  │
│     └─ REDO 포인트 위치 추출                                     │
│     └─ 손상된 경우 이전 체크포인트 시도                           │
│                                                                  │
│  3. XLOG 재생                                                    │
│     └─ REDO 포인트부터 순차적으로 XLOG 레코드 읽기 및 재생       │
│     └─ 최신 WAL 세그먼트 끝까지 계속                              │
│                                                                  │
│  4. 복구 완료                                                    │
│     └─ 데이터베이스 일관된 상태로 복원                           │
└─────────────────────────────────────────────────────────────────┘
```

#### 9.2 LSN 비교 로직

복구 시 두 가지 레코드 유형을 구분:

백업 블록 (Backup Blocks):
- 해당 테이블 페이지에 무조건 덮어쓰기
- 멱등성(Idempotent) 작업 - 반복 재생 가능

비백업 블록 (Non-Backup Blocks):
- 레코드의 LSN이 페이지의 `pd_lsn`보다 큰 경우에만 재생
- 비멱등성(Non-idempotent) 작업 - 잘못된 재생 시 데이터 불일치 위험

```
┌─────────────────────────────────────────────────────────────────┐
│                      LSN 비교 로직                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  if (레코드가 백업 블록) {                                       │
│      페이지에 무조건 덮어쓰기;                                   │
│  } else {                                                        │
│      if (레코드의 LSN > 페이지의 pd_lsn) {                       │
│          레코드 재생;                                            │
│      } else {                                                    │
│          스킵 (이미 적용됨);                                     │
│      }                                                           │
│  }                                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 9.3 복구 예제

```
REDO 포인트: LSN_1

WAL 레코드들:
┌────────┬─────────┬─────────────┬────────────────────────────┐
│ LSN    │ 유형    │ 페이지 LSN  │ 동작                        │
├────────┼─────────┼─────────────┼────────────────────────────┤
│ LSN_1  │ INSERT  │ LSN_0       │ 재생 (LSN_1 > LSN_0)       │
│ LSN_2  │ UPDATE  │ LSN_1       │ 재생 (LSN_2 > LSN_1)       │
│ LSN_3  │ FPI     │ -           │ 무조건 적용 (백업 블록)     │
│ LSN_4  │ DELETE  │ LSN_3       │ 재생 (LSN_4 > LSN_3)       │
└────────┴─────────┴─────────────┴────────────────────────────┘
```

---

### 10. Generic WAL 레코드

#### 10.1 개념

Generic WAL은 확장이 페이지 변경을 WAL에 기록할 수 있도록 PostgreSQL이 제공하는 내장 메커니즘. `access/generic_xlog.h`에 정의됨.

제한사항: Generic WAL 레코드는 논리적 디코딩(Logical Decoding) 중에 무시됨.

#### 10.2 API 함수

```c
/* 1. Generic WAL 레코드 구성 시작 */
GenericXLogState *state = GenericXLogStart(relation);

/* 2. 버퍼 등록 */
Page page = GenericXLogRegisterBuffer(state, buffer, flags);

/* 3. 페이지 수정 */
/* 반환된 페이지 복사본만 수정 */

/* 4. 변경 적용 및 WAL 레코드 발행 */
GenericXLogFinish(state);

/* 또는 변경 취소 */
GenericXLogAbort(state);
```

#### 10.3 사용 예제

```c
#include "access/generic_xlog.h"
#include "storage/bufmgr.h"

void
my_extension_insert(Relation rel, ItemPointer tid, Datum value)
{
    Buffer      buffer;
    Page        page;
    GenericXLogState *state;

    /* 버퍼 잠금 획득 */
    buffer = ReadBuffer(rel, P_NEW);
    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    /* Generic WAL 시작 */
    state = GenericXLogStart(rel);

    /* 버퍼 등록 (새 페이지이므로 FULL_IMAGE 플래그) */
    page = GenericXLogRegisterBuffer(state, buffer,
                                      GENERIC_XLOG_FULL_IMAGE);

    /* 페이지 초기화 및 데이터 삽입 */
    PageInit(page, BufferGetPageSize(buffer), 0);
    /* ... 데이터 삽입 로직 ... */

    /* WAL 레코드 발행 및 버퍼 해제 */
    GenericXLogFinish(state);
    UnlockReleaseBuffer(buffer);
}
```

#### 10.4 플래그

```c
/* 전체 페이지 이미지 포함 (델타 업데이트 대신) */
#define GENERIC_XLOG_FULL_IMAGE    0x01
```

#### 10.5 주요 제약사항

- 버퍼 수정: `GenericXLogRegisterBuffer()`에서 반환된 복사본만 수정
- 잠금: 등록부터 `GenericXLogFinish()` 이후까지 배타적 잠금 유지
- 최대 버퍼: `MAX_GENERIC_XLOG_PAGES` 제한
- 페이지 레이아웃: `pd_lower`와 `pd_upper` 사이에 유용한 데이터가 없다고 가정
- 더티 마킹: `GenericXLogFinish()`가 자동으로 버퍼를 더티로 표시

---

### 11. WAL 관련 설정 파라미터

#### 11.1 체크포인트 관련

```ini
# 체크포인트 간격 (기본값: 5분)
checkpoint_timeout = 5min

# WAL 파일 최대 크기 (기본값: 1GB)
max_wal_size = 1GB

# WAL 파일 최소 크기
min_wal_size = 80MB

# 체크포인트 완료 목표 (기본값: 0.9)
checkpoint_completion_target = 0.9
```

#### 11.2 WAL 버퍼 관련

```ini
# WAL 버퍼 크기 (기본값: -1, 자동 계산)
wal_buffers = -1

# 전체 페이지 쓰기 (기본값: on)
full_page_writes = on

# WAL 압축 (기본값: off)
wal_compression = off
```

#### 11.3 커밋 관련

```ini
# 커밋 지연 (마이크로초)
commit_delay = 0

# 커밋 시블링 임계값
commit_siblings = 5

# fsync 활성화 (기본값: on)
fsync = on

# WAL 동기화 메소드
wal_sync_method = fdatasync
```

#### 11.4 WAL 레벨

```ini
# WAL 레벨 (기본값: replica)
# minimal: 충돌 복구만 지원
# replica: WAL 아카이빙 및 복제 지원
# logical: 논리적 복제 지원
wal_level = replica
```

---

### 12. 데이터 무결성 메커니즘

#### 12.1 체크섬

PostgreSQL은 여러 체크섬 메커니즘으로 데이터 무결성을 보호함:

- WAL 레코드: CRC-32C (32비트) 체크
- 데이터 페이지: 기본적으로 체크섬 적용
- 전체 페이지 이미지: WAL 레코드에서 항상 체크섬 보호
- 2단계 파일: `pg_twophase`의 상태 파일에 CRC-32C 적용

#### 12.2 스토리지 하드웨어 고려사항

Linux에서 쓰기 캐시 관리:

```bash
# IDE/SATA 드라이브
hdparm -I           # 쓰기 캐시 활성화 여부 확인
hdparm -W 0         # 쓰기 캐시 비활성화

# SCSI 드라이브
sdparm --get=WCE    # 쓰기 캐시 상태 확인
sdparm --clear=WCE  # 쓰기 캐시 비활성화
```

#### 12.3 pg_test_fsync

I/O 서브시스템 성능 측정:

```bash
$ pg_test_fsync

5 seconds per test
O_DIRECT supported on this platform for open_datasync and open_sync.

Compare file sync methods using one 8kB write:
(in wal_sync_method preference order, except fdatasync is Linux's default)
        open_datasync                      4535.775 ops/sec     220 usecs/op
        fdatasync                          4467.020 ops/sec     224 usecs/op
        fsync                              4393.283 ops/sec     228 usecs/op
        fsync_writethrough                              n/a
        open_sync                          4419.012 ops/sec     226 usecs/op
```

---

### 13. 모니터링 및 디버깅

#### 13.1 WAL 관련 시스템 뷰

```sql
-- 현재 WAL 상태 확인
SELECT pg_current_wal_lsn(),
       pg_current_wal_insert_lsn(),
       pg_current_wal_flush_lsn();

-- WAL 통계 확인
SELECT * FROM pg_stat_wal;

-- 체크포인터 통계
SELECT * FROM pg_stat_checkpointer;

-- 복제 슬롯 정보
SELECT * FROM pg_replication_slots;
```

#### 13.2 pg_waldump 유틸리티

WAL 레코드 분석:

```bash
# 특정 WAL 세그먼트 분석
$ pg_waldump /path/to/pg_wal/000000010000000000000001

# 특정 LSN 범위 분석
$ pg_waldump -s 0/1A2B3C00 -e 0/1A2B3CFF /path/to/pg_wal/*

# 특정 리소스 관리자의 레코드만 표시
$ pg_waldump -r Heap /path/to/pg_wal/*
```

출력 예제:

```
rmgr: Heap        len (rec/tot):     59/    59, tx:        488, lsn: 0/01A2B3C0,
      prev 0/01A2B3A0, desc: INSERT off 2 flags 0x00,
      blkref #0: rel 1663/16384/16385 blk 0

rmgr: Transaction len (rec/tot):     34/    34, tx:        488, lsn: 0/01A2B400,
      prev 0/01A2B3C0, desc: COMMIT 2024-01-15 10:30:00.000000 KST
```

#### 13.3 WAL 일관성 검사

```ini
# WAL 일관성 검사 활성화
wal_consistency_checking = all  # 또는 특정 리소스 관리자 지정
```

---

### 14. 참고 자료

#### 14.1 PostgreSQL 소스 코드

- `src/include/access/xlogrecord.h` - WAL 레코드 형식 정의
- `src/include/access/xlog_internal.h` - WAL 내부 구조체
- `src/backend/access/transam/xlog.c` - WAL 핵심 구현
- `src/backend/access/transam/generic_xlog.c` - Generic WAL 구현
- `src/test/modules/test_custom_rmgrs/` - 사용자 정의 리소스 관리자 예제

#### 14.2 공식 문서

- [PostgreSQL WAL Internals](https://www.postgresql.org/docs/current/wal-internals.html)
- [Custom WAL Resource Managers](https://www.postgresql.org/docs/current/custom-rmgr.html)
- [Generic WAL Records](https://www.postgresql.org/docs/current/generic-wal.html)
- [WAL Configuration](https://www.postgresql.org/docs/current/wal-configuration.html)
- [WAL Reliability](https://www.postgresql.org/docs/current/wal-reliability.html)

#### 14.3 추가 참고 자료

- [The Internals of PostgreSQL - Chapter 9: Write Ahead Logging](http://www.interdb.jp/pg/pgsql09.html)
- [Custom WAL Resource Managers Wiki](https://wiki.postgresql.org/wiki/CustomWALResourceManagers)

---

### 요약

PostgreSQL의 WAL 시스템은 데이터 무결성과 복구를 보장하는 핵심 메커니즘:

1. WAL 원칙: 데이터 변경 전에 WAL 레코드를 먼저 기록
2. LSN: 모든 WAL 레코드의 고유 위치 식별자
3. 리소스 관리자: 특정 유형의 WAL 레코드 쓰기 및 재생 담당
4. 체크포인트: 복구 시작점을 정의하고 더티 페이지를 디스크에 플러시
5. 복구: REDO 포인트부터 WAL 레코드를 순차적으로 재생하여 일관된 상태 복원
6. 전체 페이지 쓰기: 부분 페이지 쓰기 문제 해결

이러한 구성요소들이 함께 작동 → PostgreSQL의 신뢰성 있는 트랜잭션 처리와 충돌 복구를 가능하게 함.

---

## Chapter 67. B-Tree 인덱스 (B-Tree Indexes)

PostgreSQL 18 공식 문서 번역

---

### 목차

- [67.1. 소개](#671-소개-introduction)
- [67.2. B-Tree 연산자 클래스의 동작](#672-b-tree-연산자-클래스의-동작-behavior-of-b-tree-operator-classes)
- [67.3. B-Tree 지원 함수](#673-b-tree-지원-함수-b-tree-support-functions)
- [67.4. 구현](#674-구현-implementation)

---

### 67.1. 소개 (Introduction)

PostgreSQL은 표준 B-tree(다중 경로 균형 트리, multi-way balanced tree) 인덱스 구현을 포함함. 명확한 선형 순서로 정렬 가능한 모든 데이터 타입을 B-tree로 인덱싱 가능.

#### B-tree 인덱스의 기본 특성

- 정렬 가능한 모든 데이터 타입에 대해 인덱스 생성 가능
- PostgreSQL의 기본 인덱스 유형 → `CREATE INDEX` 시 별도 지정이 없으면 B-tree가 생성됨
- 제한사항: 인덱스 엔트리는 페이지의 약 1/3을 초과할 수 없음(TOAST 압축 후 기준)

#### B-tree의 역할

B-tree 연산자 클래스는 단순한 인덱싱을 넘어 PostgreSQL 시스템 전반에서 정렬 의미론(sorting semantics)을 표현하는 데 사용됨.

#### 지원 연산자

B-tree 인덱스는 다음 연산자를 사용하는 비교에서 활용됨:

```
<     <=     =     >=     >
```

또한 다음 구성에서도 사용 가능:

- `BETWEEN`
- `IN`
- `IS NULL`
- `IS NOT NULL`
- `LIKE` (패턴이 상수이고 시작 부분에 고정된 경우)
- `~` (정규식, 패턴이 상수이고 시작 부분에 고정된 경우)

#### 기본 예제

```sql
-- B-tree 인덱스 생성 (기본값)
CREATE INDEX idx_name ON table_name (column_name);

-- 명시적으로 B-tree 지정
CREATE INDEX idx_name ON table_name USING BTREE (column_name);

-- 다중 컬럼 B-tree 인덱스
CREATE INDEX idx_multi ON table_name (col1, col2, col3);

-- 고유 B-tree 인덱스
CREATE UNIQUE INDEX idx_unique ON table_name (column_name);
```

---

### 67.2. B-Tree 연산자 클래스의 동작 (Behavior of B-Tree Operator Classes)

B-tree 연산자 클래스가 올바르게 작동하려면 특정 규칙과 동작을 따라야 함.

#### 필수 연산자 (5개)

B-tree 연산자 클래스는 다음 5개의 비교 연산자를 제공해야 함:

- `<` (전략 번호 1): 미만 (Less Than)
- `<=` (전략 번호 2): 이하 (Less Than or Equal)
- `=` (전략 번호 3): 동등 (Equal)
- `>=` (전략 번호 4): 이상 (Greater Than or Equal)
- `>` (전략 번호 5): 초과 (Greater Than)

참고: `<>` (부등호) 연산자는 포함되지 않음. B-tree 인덱스 검색에서 유용하지 않기 때문.

#### 연산자 패밀리 (Operator Families)

유사한 정렬 의미론을 가진 여러 데이터 타입은 연산자 패밀리(Operator Family)로 그룹화됨:

- 단일 타입 연산자: 각 연산자 클래스에 포함
- 교차 타입 연산자(Cross-type Operators): 패밀리 전체에서 느슨하게 포함
- 플래너가 교차 타입 비교에 대해 추론 가능

#### 필수 수학적 원칙

B-tree 연산자 클래스가 올바르게 작동하려면 다음 수학적 원칙을 만족해야 함:

##### 동등 연산자 `=` (동치 관계, Equivalence Relation)

```
반사성(Reflexive):    A = A
대칭성(Symmetric):    A = B  =>  B = A
이행성(Transitive):   A = B AND B = C  =>  A = C
```

##### 미만 연산자 `<` (강한 순서 관계, Strict Total Order)

```
비반사성(Irreflexive):   NOT (A < A)
이행성(Transitive):      A < B AND B < C  =>  A < C
삼분법(Trichotomy):      정확히 하나만 참: A < B, A = B, B < A
```

#### 다중 데이터 타입 패밀리의 제약사항

다중 데이터 타입을 포함하는 연산자 패밀리에서는 데이터 타입 간 암시적/이진 강제 변환(coercion)이 정렬 순서를 바꾸면 금지.

반례 - float8과 numeric:

`float8`과 `numeric`은 같은 연산자 패밀리에 포함 불가:
- `float8`의 제한된 정확도로 인해 서로 다른 `numeric` 값이 같은 `float8` 값과 비교될 수 있음
- 이행성 법칙 위반

```sql
-- 예시: 이행성 위반
-- numeric: 1.0000000000000001, 1.0000000000000002가 서로 다름
-- 하지만 float8로 변환 시 둘 다 동일한 값이 될 수 있음
```

#### 다른 연산자들과의 관계

5개의 기본 연산자로부터 다음 관계가 유도됨:

```sql
A <= B  ==  A < B OR A = B
A >= B  ==  A > B OR A = B
A > B   ==  B < A
```

---

### 67.3. B-Tree 지원 함수 (B-Tree Support Functions)

B-tree 연산자 클래스는 여러 지원 함수를 제공 가능. 일부는 필수, 일부는 선택.

#### 지원 함수 요약

- 1번 `order`: 필수, 값 비교(-1/0/+1 반환)
- 2번 `sortsupport`: 선택, 정렬 최적화
- 3번 `in_range`: 선택, 윈도우 함수 RANGE OFFSET 지원
- 4번 `equalimage`: 선택, 중복 제거(Deduplication) 안전성 판단
- 5번 `options`: 선택, 사용자 정의 파라미터
- 6번 `skipsupport`: 선택, Skip scan 최적화

#### 67.3.1. order 함수 (Support Function #1) - 필수

```c
int32 order(A type, B type)
```

기능:
- 두 값을 비교하여 정수를 반환
- 반환 값:
  - `< 0`: A < B
  - `0`: A = B
  - `> 0`: A > B

예시 구현 위치: `src/backend/access/nbtree/nbtcompare.c`

```c
// 정수 비교 예시
Datum
btint4cmp(PG_FUNCTION_ARGS)
{
    int32 a = PG_GETARG_INT32(0);
    int32 b = PG_GETARG_INT32(1);

    if (a > b)
        PG_RETURN_INT32(1);
    else if (a < b)
        PG_RETURN_INT32(-1);
    else
        PG_RETURN_INT32(0);
}
```

중요 규칙:
- 모든 값은 비교 가능해야 함
- NULL 결과 반환 불허
- Collation 지원 시 `PG_GET_COLLATION()` 메커니즘 사용

#### 67.3.2. sortsupport 함수 (Support Function #2) - 선택

목적: 정렬 목적의 비교를 기본 비교 함수보다 효율적으로 구현

API 정의 위치: `src/include/utils/sortsupport.h`

```c
// sortsupport 사용 예시 구조
typedef struct SortSupportData
{
    MemoryContext ssup_cxt;
    Oid ssup_collation;
    bool ssup_reverse;
    bool ssup_nulls_first;

    // 비교 함수 포인터
    int (*comparator) (Datum x, Datum y, SortSupport ssup);

    // 추가 최적화를 위한 필드들...
} SortSupportData;
```

#### 67.3.3. in_range 함수 (Support Function #3) - 선택

```c
bool in_range(val type1, base type1, offset type2, sub bool, less bool)
```

사용 사례: 윈도우 함수의 `RANGE OFFSET PRECEDING/FOLLOWING` 프레임 경계 지원

의미론:

- sub=false, less=false: val >= (base + offset)
- sub=false, less=true: val <= (base + offset)
- sub=true, less=false: val >= (base - offset)
- sub=true, less=true: val <= (base - offset)

예시 쿼리:

```sql
-- RANGE BETWEEN 사용 예시
SELECT
    product_id,
    sale_date,
    amount,
    SUM(amount) OVER (
        ORDER BY sale_date
        RANGE BETWEEN INTERVAL '7 days' PRECEDING AND CURRENT ROW
    ) as weekly_sum
FROM sales;
```

필수 검증:
- offset이 음수이면 에러 발생: `ERRCODE_INVALID_PRECEDING_OR_FOLLOWING_SIZE (22013)`
- 가능하면 오버플로우 회피 시도

#### 67.3.4. equalimage 함수 (Support Function #4) - 선택

```c
bool equalimage(opcintype oid)
```

목적: B-tree 중복 제거(Deduplication) 최적화의 안전성 판단

반환 값 의미:
- `true`: `order` 함수가 `0`을 반환할 때 두 인수를 의미 정보 손실 없이 교환 가능
- `false` 또는 미등록: 해당 조건을 보장할 수 없음

핵심 개념 - Image Equality:

```
datum_image_eq() C 함수와 order() 함수의 결과가 항상 일치하면 true 반환
```

데이터 타입별 지원 현황:

- 대부분 기본 타입: `btequalimage()` 사용, 안전
- `text`, `varchar`, `char`: `btvarstrequalimage()` 사용, 결정적(deterministic) collation만 안전
- `numeric`: 전용 함수 없음, 불안전(표시 스케일 보존 필요)
- `jsonb`: 전용 함수 없음, 불안전(내부적으로 numeric 사용)
- `float4`, `float8`: 전용 함수 없음, 불안전(`-0`과 `0` 구별 필요)

#### 67.3.5. options 함수 (Support Function #5) - 선택

```c
void options(local_relopts *relopts)
```

목적: 연산자 클래스별 사용자 정의 파라미터 정의

현재 상태: B-tree에는 현재 구현된 options 함수가 없으며, 향후 확장을 위해 예약된 번호임

#### 67.3.6. skipsupport 함수 (Support Function #6) - 선택

API 정의 위치: `src/include/utils/skipsupport.h`

목적: Skip scan 최적화 지원

특징:
- 등록하지 않아도 skip scan은 동작하지만 최적화 효과가 제한됨
- 연속적인 타입에는 일반적으로 불필요
- 교차 타입 함수는 지원되지 않음

Skip Scan 예시:

```sql
-- (x, y) 인덱스에서 y만으로 검색
-- Skip scan이 모든 가능한 x 값에 대해 검색 수행
SELECT * FROM table WHERE y = 100;
```

---

### 67.4. 구현 (Implementation)

#### 67.4.1. B-Tree 구조

B-tree 인덱스는 계층적 구조로 구성됨:

```
메타페이지 (Meta Page)
    - 고정 위치 (첫 번째 세그먼트 파일)
    - 인덱스 메타데이터 저장
       │
       ▼
루트 페이지 (Root Page)
    - 트리의 최상위 레벨
       │
       ▼
내부 페이지 (Internal Pages)
    - 중간 레벨들
    - 다운링크(downlink) 포함
       │
       ▼
리프 페이지 (Leaf Pages)
    - 최하단 레벨
    - 전체 인덱스의 99% 이상 차지
    - 테이블 행(TID)을 가리키는 튜플 저장
```

#### 페이지 분할 (Page Split)

인덱스 항목 삽입 시 페이지가 가득 차면 분할이 발생함:

```sql
-- 페이지 분할 과정
1. 기존 리프 페이지가 크기 초과
2. 일부 항목을 새 페이지로 이동
3. 부모 페이지에 새 다운링크 추가
4. 부모도 분할 필요 시 재귀적 분할
5. 루트 페이지 분할 시 트리 높이 증가
```

#### 67.4.2. 상향식 인덱스 삭제 (Bottom-up Index Deletion)

배경: MVCC 환경에서 UPDATE로 인한 "version churn" 누적 문제

- 한 컬럼만 변경해도 해당 행의 모든 인덱스에 새 튜플이 삽입됨
- HOT(Heap Only Tuple) 최적화가 불가능한 경우 특히 심각

메커니즘:

```
1. version churn으로 인한 페이지 분할이 임박했음을 감지
2. Bottom-up 삭제 패스 실행
3. 단일 리프 페이지에서 가비지로 추정되는 튜플 삭제
4. 페이지 분할 회피 또는 성능 개선
```

Simple Index Deletion과의 비교:

- Bottom-up Deletion
  - 동작: version churn 튜플 대상
  - 트리거: 페이지 분할 예상 시
  - 기반: 정성적 판단
  - 도입: PostgreSQL 14+
- Simple Deletion
  - 동작: LP_DEAD 비트가 설정된 튜플 삭제
  - 트리거: 페이지 분할 예상 시
  - 기반: LP_DEAD 비트 설정
  - 도입: 14 이전

#### 67.4.3. 중복 제거 (Deduplication)

정의: 리프 페이지 튜플들 중 모든 인덱스 키 컬럼 값이 동일한 경우를 중복으로 취급

작동 방식:

```
중복 튜플 그룹
    │
    ▼
단일 posting list 튜플로 병합
    │
    ▼
키 값 1회 저장 + 정렬된 TID 배열
```

예시:

```sql
-- 중복이 많은 컬럼에 인덱스 생성
CREATE INDEX idx_status ON orders (status);

-- status 값이 'pending', 'completed', 'cancelled' 등 소수의 값만 가지면
-- 동일한 status 값을 가진 여러 행의 TID가 하나의 posting list로 병합됨
```

효과:
- 저장 공간 대폭 감소
- 쿼리 지연 시간 단축
- 처리량 증가
- VACUUM 오버헤드 감소

#### Deduplication 제어

```sql
-- 인덱스별 중복 제거 비활성화
CREATE INDEX idx_name ON table_name (col)
  WITH (deduplicate_items = off);

-- 기본값은 on
CREATE INDEX idx_name ON table_name (col)
  WITH (deduplicate_items = on);
```

#### Deduplication 불가능한 경우

의미론적 차이로 인한 제한:

- `text`, `varchar`, `char`: 비결정적(nondeterministic) collation 사용 시 대소문자·악센트 차이 보존 필요
- `numeric`: 표시 스케일(display scale) 보존 필요
- `jsonb`: 내부적으로 numeric 사용
- `float4`, `float8`: `-0`과 `0`의 구분 필요(동일하지만 다른 표현)

구현 제한:

- 컨테이너 타입(복합, 배열, 범위): 구현 복잡성 → 개선 가능성 있음
- INCLUDE 인덱스: 설계상 제한 → 영구적 제한

---

### 실용적인 B-Tree 인덱스 사용 예제

#### 기본 인덱스 생성

```sql
-- 단일 컬럼 인덱스
CREATE INDEX idx_user_email ON users (email);

-- 다중 컬럼 인덱스
CREATE INDEX idx_order_status_date ON orders (status, created_at);

-- 고유 인덱스
CREATE UNIQUE INDEX idx_user_username ON users (username);
```

#### 정렬 순서 지정

```sql
-- 내림차순 인덱스
CREATE INDEX idx_created_desc ON posts (created_at DESC);

-- NULL 처리 지정
CREATE INDEX idx_score ON scores (score DESC NULLS LAST);

-- 복합 정렬 순서
CREATE INDEX idx_multi_sort ON products (category ASC, price DESC);
```

#### 부분 인덱스 (Partial Index)

```sql
-- 활성 사용자만 인덱싱
CREATE INDEX idx_active_users ON users (email)
  WHERE is_active = true;

-- 특정 상태만 인덱싱
CREATE INDEX idx_pending_orders ON orders (order_id)
  WHERE status = 'pending';
```

#### 표현식 인덱스 (Expression Index)

```sql
-- 대소문자 구분 없는 검색용
CREATE INDEX idx_email_lower ON users (lower(email));

-- 날짜 일부 추출
CREATE INDEX idx_order_year ON orders (EXTRACT(YEAR FROM created_at));

-- JSON 필드 인덱싱
CREATE INDEX idx_data_name ON documents ((data->>'name'));
```

#### 커버링 인덱스 (Covering Index)

```sql
-- INCLUDE를 사용한 커버링 인덱스
CREATE INDEX idx_user_covering ON users (email) INCLUDE (name, created_at);

-- 이 쿼리는 인덱스 전용 스캔 가능
SELECT name, created_at FROM users WHERE email = 'test@example.com';
```

#### 인덱스 사용 확인

```sql
-- EXPLAIN으로 인덱스 사용 확인
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- 예상 출력:
-- Index Scan using idx_user_email on users  (cost=0.29..8.31 rows=1 width=100)
--   Index Cond: (email = 'test@example.com'::text)
```

---

### B-Tree 인덱스 성능 최적화 가이드

#### 인덱스가 효과적인 경우

1. 높은 선택성(Selectivity): 조건이 전체 행의 작은 비율만 선택
2. 범위 쿼리: `BETWEEN`, `>`, `<` 등의 범위 조건
3. 정렬된 출력: `ORDER BY` 절과 일치하는 경우
4. 조인 조건: 외래 키 컬럼

#### 인덱스가 비효율적인 경우

1. 낮은 선택성: 대부분의 행을 반환하는 조건
2. 매우 작은 테이블: 순차 스캔이 더 빠름
3. 빈번한 UPDATE가 있는 컬럼: 인덱스 유지 비용 증가

#### 권장 사항

```sql
-- 통계 업데이트로 플래너 정확도 향상
ANALYZE table_name;

-- 인덱스 팽창(bloat) 확인 및 재구축
REINDEX INDEX idx_name;

-- 인덱스 사용 통계 확인
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'your_table';
```

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 67. B-Tree Indexes](https://www.postgresql.org/docs/current/btree.html)
- [PostgreSQL 소스 코드 - nbtree](https://github.com/postgres/postgres/tree/master/src/backend/access/nbtree)
- [CREATE INDEX 문서](https://www.postgresql.org/docs/current/sql-createindex.html)
- [인덱스 유지보수](https://www.postgresql.org/docs/current/routine-reindex.html)
