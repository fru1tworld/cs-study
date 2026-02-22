# Chapter 62: 테이블 접근 메서드 인터페이스 (Table Access Method Interface Definition)

이 문서는 PostgreSQL 핵심 시스템과 테이블 접근 메서드(Table Access Methods) 간의 인터페이스를 정의합니다. 이 인터페이스를 통해 개발자는 기본 `heap` 구현 외에 완전히 새로운 테이블 저장 방법을 구현할 수 있습니다.

## 목차

1. [테이블 접근 메서드 개요](#1-테이블-접근-메서드-개요)
2. [기본 API 구조](#2-기본-api-구조-basic-api-structure)
3. [TableAmRoutine 콜백 함수](#3-tableamroutine-콜백-함수)
4. [스캔 API](#4-스캔-api-scan-api)
5. [수정 API](#5-수정-api-modification-api)
6. [인덱스 관련 연산](#6-인덱스-관련-연산-index-related-operations)
7. [유지보수 연산](#7-유지보수-연산-maintenance-operations)
8. [구현 요구사항](#8-구현-요구사항-implementation-requirements)

---

## 1. 테이블 접근 메서드 개요

### 1.1 테이블 접근 메서드란?

테이블 접근 메서드(Table Access Method, TAM) 는 PostgreSQL이 테이블 데이터를 저장하고 접근하는 방식을 정의하는 인터페이스입니다. PostgreSQL 12부터 도입된 이 인터페이스를 통해:

- 커스텀 테이블 저장소 구현 가능
- 기본 `heap` 방식 외의 다양한 저장 전략 사용 가능
- 특수한 워크로드에 최적화된 저장소 개발 가능

### 1.2 시스템 카탈로그 등록

테이블 접근 메서드는 `pg_am` 시스템 카탈로그에 등록됩니다:

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

### 1.3 접근 메서드 생성 및 삭제

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

## 2. 기본 API 구조 (Basic API Structure)

### 2.1 핸들러 함수 (Handler Function)

테이블 접근 메서드의 핵심은 핸들러 함수(handler function) 입니다. 이 함수는 다음 요구사항을 충족해야 합니다:

| 항목 | 요구사항 |
|------|----------|
| 입력 타입 | `internal` (SQL 직접 호출 방지용 더미 파라미터) |
| 반환 타입 | `table_am_handler` (의사 타입) |
| 반환 값 | `TableAmRoutine` 구조체에 대한 포인터 |

#### 핸들러 함수 등록 예제

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

#### SQL 등록

```sql
-- 핸들러 함수 생성
CREATE OR REPLACE FUNCTION my_tableam_handler(internal)
    RETURNS table_am_handler
    AS 'my_extension', 'my_tableam_handler'
    LANGUAGE C STRICT;

-- 접근 메서드 등록
CREATE ACCESS METHOD myam TYPE TABLE HANDLER my_tableam_handler;
```

### 2.2 TableAmRoutine 구조체

`TableAmRoutine` 구조체는 테이블 접근 메서드의 모든 동작을 정의하는 콜백 함수 포인터들을 포함합니다. 전체 정의는 `src/include/access/tableam.h`에서 확인할 수 있습니다.

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

## 3. TableAmRoutine 콜백 함수

### 3.1 콜백 함수 분류

| 카테고리 | 설명 | 주요 콜백 |
|----------|------|----------|
| 슬롯 관리 | 튜플 테이블 슬롯 연산 | `slot_callbacks` |
| 스캔 초기화/제어 | 테이블 스캔 시작/종료/재시작 | `scan_begin`, `scan_end`, `scan_rescan` |
| 스캔 실행 | 실제 튜플 가져오기 | `scan_getnextslot`, `scan_getnextslot_tidrange` |
| 병렬 스캔 | 병렬 쿼리 지원 | `parallelscan_estimate`, `parallelscan_initialize` |
| 인덱스 패치 | 인덱스를 통한 튜플 접근 | `index_fetch_begin`, `index_fetch_tuple` |
| 튜플 검색 | 특정 튜플 버전 검색 | `tuple_fetch_row_version`, `tuple_get_latest_tid` |
| 튜플 수정 | INSERT/UPDATE/DELETE | `tuple_insert`, `tuple_update`, `tuple_delete` |
| 튜플 잠금 | 행 수준 잠금 | `tuple_lock` |
| 유지보수 | VACUUM, 크기 계산 등 | `relation_vacuum`, `relation_size` |
| 분석/샘플링 | ANALYZE 지원 | `scan_analyze_next_block`, `scan_sample_next_tuple` |

### 3.2 슬롯 관리 콜백

```c
/*
 * slot_callbacks - 릴레이션에 대한 튜플 테이블 슬롯 연산 반환
 *
 * 접근 메서드별 튜플 처리를 위한 슬롯 타입을 정의합니다.
 */
const TupleTableSlotOps* (*slot_callbacks) (Relation rel);
```

튜플 테이블 슬롯은 실행기(executor)가 튜플에 대한 참조를 유지하고 컬럼 접근 기능을 제공하는 데 사용됩니다. 커스텀 접근 메서드 개발자는 일반적으로 AM 전용 튜플 테이블 슬롯 타입을 만들어야 합니다.

참조 파일: `src/include/executor/tuptable.h`

---

## 4. 스캔 API (Scan API)

### 4.1 스캔 시작 함수

#### table_beginscan

```c
static TableScanDesc table_beginscan(
    Relation rel,           /* 스캔할 릴레이션 */
    Snapshot snapshot,      /* 가시성 판단용 스냅샷 */
    int nkeys,              /* 스캔 키 개수 */
    ScanKeyData *key        /* 스캔 키 배열 */
);
```

기본 옵션(전략 허용, 동기화 허용, 페이지 모드 허용)으로 순차 스캔을 시작합니다.

#### table_beginscan_catalog

```c
TableScanDesc table_beginscan_catalog(
    Relation relation,      /* 스캔할 카탈로그 릴레이션 */
    int nkeys,              /* 스캔 키 개수 */
    ScanKeyData *key        /* 스캔 키 배열 */
);
```

시스템 카탈로그 테이블 스캔을 위한 전용 함수입니다. 스냅샷 등록 및 임시 스냅샷 처리를 자동으로 수행합니다.

#### table_beginscan_strat

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

접근 전략과 동기화 스캔 옵션을 직접 설정할 수 있습니다.

### 4.2 특수 스캔 함수

#### 비트맵 스캔

```c
static TableScanDesc table_beginscan_bm(
    Relation rel,
    Snapshot snapshot,
    int nkeys,
    ScanKeyData *key
);
```

비트맵 인덱스 스캔을 위한 테이블 스캔을 시작합니다.

#### TID 범위 스캔

```c
static TableScanDesc table_beginscan_tidrange(
    Relation rel,
    Snapshot snapshot,
    ItemPointer mintid,     /* 최소 TID */
    ItemPointer maxtid      /* 최대 TID */
);
```

특정 TID 범위 내의 튜플만 스캔합니다.

#### 샘플링 스캔

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

TABLESAMPLE 절을 위한 샘플링 스캔을 시작합니다.

### 4.3 스캔 제어 함수

#### table_endscan

```c
static void table_endscan(TableScanDesc scan);
```

스캔을 종료하고 관련 리소스를 해제합니다.

#### table_rescan

```c
static void table_rescan(
    TableScanDesc scan,     /* 재시작할 스캔 */
    ScanKeyData *key        /* 새로운 스캔 키 (NULL 가능) */
);
```

스캔을 처음부터 다시 시작합니다.

#### table_rescan_set_params

```c
static void table_rescan_set_params(
    TableScanDesc scan,
    ScanKeyData *key,
    bool allow_strat,
    bool allow_sync,
    bool allow_pagemode
);
```

파라미터를 재설정하면서 스캔을 재시작합니다.

### 4.4 튜플 가져오기

#### table_scan_getnextslot

```c
static bool table_scan_getnextslot(
    TableScanDesc sscan,        /* 활성 스캔 디스크립터 */
    ScanDirection direction,    /* 스캔 방향 */
    TupleTableSlot *slot        /* 결과 튜플을 저장할 슬롯 */
);
```

활성 스캔에서 다음 튜플을 가져와 슬롯에 저장합니다.

스캔 방향 (ScanDirection):
- `ForwardScanDirection`: 정방향 스캔
- `BackwardScanDirection`: 역방향 스캔
- `NoMovementScanDirection`: 현재 위치 유지

### 4.5 스캔 사용 예제

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

## 5. 수정 API (Modification API)

### 5.1 튜플 삽입 (Tuple Insert)

#### table_tuple_insert

```c
static void table_tuple_insert(
    Relation rel,               /* 대상 릴레이션 */
    TupleTableSlot *slot,       /* 삽입할 튜플이 담긴 슬롯 */
    CommandId cid,              /* 명령 ID */
    int options,                /* 삽입 옵션 플래그 */
    BulkInsertStateData *bistate /* 대량 삽입 상태 (NULL 가능) */
);
```

단일 튜플을 테이블에 삽입합니다.

삽입 옵션 플래그:

| 플래그 | 설명 |
|--------|------|
| `TABLE_INSERT_SKIP_FSM` | Free Space Map 사용 건너뛰기 |
| `TABLE_INSERT_FROZEN` | 튜플을 frozen 상태로 삽입 |
| `TABLE_INSERT_NO_LOGICAL` | 논리적 디코딩 건너뛰기 |

#### table_tuple_insert_speculative

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

추측적 삽입(speculative insert)을 수행합니다. 이는 `ON CONFLICT` 절을 지원하기 위해 사용됩니다.

#### table_tuple_complete_speculative

```c
static void table_tuple_complete_speculative(
    Relation rel,
    TupleTableSlot *slot,
    uint32 specToken,
    bool succeeded              /* 성공/실패 여부 */
);
```

추측적 삽입을 완료하거나 롤백합니다.

### 5.2 대량 삽입 (Multi Insert)

```c
static void table_multi_insert(
    Relation rel,
    TupleTableSlot slots,     /* 삽입할 튜플 슬롯 배열 */
    int ntuples,                /* 튜플 개수 */
    CommandId cid,
    int options,
    BulkInsertStateData *bistate
);
```

여러 튜플을 한 번에 삽입하여 성능을 최적화합니다. COPY 명령에서 주로 사용됩니다.

### 5.3 튜플 업데이트 (Tuple Update)

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

기존 튜플을 새로운 값으로 업데이트합니다.

반환 값 (TM_Result):

| 값 | 설명 |
|----|------|
| `TM_Ok` | 성공 |
| `TM_Invisible` | 튜플이 보이지 않음 |
| `TM_SelfModified` | 현재 트랜잭션에서 이미 수정됨 |
| `TM_Updated` | 다른 트랜잭션에서 업데이트됨 |
| `TM_Deleted` | 다른 트랜잭션에서 삭제됨 |
| `TM_BeingModified` | 현재 수정 중 |
| `TM_WouldBlock` | 잠금 대기가 필요하지만 wait=false |

### 5.4 튜플 삭제 (Tuple Delete)

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

지정된 튜플을 삭제합니다.

### 5.5 튜플 잠금 (Tuple Lock)

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

튜플에 대한 잠금을 획득합니다.

잠금 모드 (LockTupleMode):

| 모드 | 설명 |
|------|------|
| `LockTupleKeyShare` | 키에 대한 공유 잠금 |
| `LockTupleShare` | 공유 잠금 |
| `LockTupleNoKeyExclusive` | 키 외 배타적 잠금 |
| `LockTupleExclusive` | 배타적 잠금 |

대기 정책 (LockWaitPolicy):

| 정책 | 설명 |
|------|------|
| `LockWaitBlock` | 잠금 획득까지 대기 |
| `LockWaitSkip` | 잠금 불가 시 건너뛰기 |
| `LockWaitError` | 잠금 불가 시 에러 |

### 5.6 수정 API 사용 예제

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

## 6. 인덱스 관련 연산 (Index Related Operations)

### 6.1 인덱스 패치 콜백

인덱스를 통해 테이블 튜플에 접근하기 위한 콜백들입니다.

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

### 6.2 인덱스 패치 사용 예제

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

### 6.3 인덱스 삭제 지원

```c
/* 인덱스 삭제 튜플 처리 */
TransactionId (*index_delete_tuples) (
    Relation rel,
    TM_IndexDeleteOp *delstate  /* 삭제 연산 상태 */
);
```

인덱스 VACUUM 시 죽은 튜플에 대한 처리를 수행합니다.

### 6.4 인덱스 빌드 지원

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

## 7. 유지보수 연산 (Maintenance Operations)

### 7.1 VACUUM 지원

```c
void (*relation_vacuum) (
    Relation rel,
    struct VacuumParams *params,    /* VACUUM 파라미터 */
    BufferAccessStrategy bstrategy  /* 버퍼 접근 전략 */
);
```

테이블에 대한 VACUUM 연산을 수행합니다. 죽은 튜플 정리, 통계 갱신 등을 처리합니다.

### 7.2 릴레이션 크기

```c
uint64 (*relation_size) (
    Relation rel,
    ForkNumber forkNumber   /* 포크 종류 */
);
```

릴레이션의 크기를 바이트 단위로 반환합니다.

포크 종류 (ForkNumber):

| 포크 | 설명 |
|------|------|
| `MAIN_FORKNUM` | 메인 데이터 포크 |
| `FSM_FORKNUM` | Free Space Map |
| `VISIBILITYMAP_FORKNUM` | Visibility Map |
| `INIT_FORKNUM` | 초기화 포크 |

### 7.3 크기 추정

```c
void (*relation_estimate_size) (
    Relation rel,
    int32 *attr_widths,
    BlockNumber *pages,         /* 페이지 수 출력 */
    double *tuples,             /* 튜플 수 출력 */
    double *allvisfrac          /* 모두 가시적인 비율 출력 */
);
```

플래너를 위한 릴레이션 크기 추정 정보를 제공합니다.

### 7.4 TOAST 지원

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

TOAST(The Oversized-Attribute Storage Technique)를 통한 대형 값 처리를 지원합니다.

### 7.5 파일 위치 변경

```c
void (*relation_set_new_filelocator) (
    Relation rel,
    const RelFileLocator *newrlocator,
    char persistence,
    TransactionId *freezeXid,
    MultiXactId *minmulti
);
```

TRUNCATE, CLUSTER 등의 연산 시 새로운 파일 위치를 설정합니다.

### 7.6 비트랜잭션 TRUNCATE

```c
void (*relation_nontransactional_truncate) (Relation rel);
```

트랜잭션 로깅 없이 릴레이션을 잘라냅니다. 주로 초기화 목적으로 사용됩니다.

### 7.7 데이터 복사

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

## 8. 구현 요구사항 (Implementation Requirements)

### 8.1 TID 요구사항

테이블 접근 메서드가 수정 또는 인덱스를 지원하려면, 각 튜플이 튜플 식별자(TID, Tuple Identifier) 를 가져야 합니다.

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

### 8.2 비트맵 스캔 지원

비트맵 스캔을 지원하려면 블록 번호가 지역성(locality) 을 제공해야 합니다. 즉, 인접한 블록 번호는 물리적으로 인접한 저장 위치를 나타내야 합니다.

### 8.3 튜플 테이블 슬롯

커스텀 접근 메서드 개발자는 일반적으로 AM 전용 튜플 테이블 슬롯 타입을 만들어야 합니다.

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

### 8.4 저장소 유연성

테이블 접근 메서드는 저장소 구현에 있어 유연성을 가집니다:

| 항목 | 필수 여부 | 설명 |
|------|-----------|------|
| 공유 버퍼 캐시 | 선택 | PostgreSQL의 공유 버퍼 캐시 사용 가능하지만 필수 아님 |
| 표준 페이지 레이아웃 | 선택 | `src/include/storage/bufpage.h`의 레이아웃 사용 가능 |
| 커스텀 저장소 | 허용 | 완전히 다른 저장 방식 구현 가능 |

### 8.5 충돌 안전성 (Crash Safety)

테이블 접근 메서드는 충돌 안전성을 보장하기 위해 다음 중 하나를 사용할 수 있습니다:

#### 방법 1: PostgreSQL WAL 사용

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

#### 방법 2: 커스텀 WAL 리소스 매니저

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

#### 방법 3: 완전 커스텀 구현

자체적인 충돌 복구 메커니즘을 구현할 수도 있습니다.

### 8.6 트랜잭션 지원

단일 트랜잭션 내에서 여러 접근 메서드 간의 크로스-AM 트랜잭션 지원을 위해 `src/backend/access/transam/xlog.c` 기계와 긴밀하게 통합해야 합니다.

### 8.7 참조 구현

새로운 테이블 접근 메서드 개발자는 기존 `heap` 구현을 참조하는 것이 좋습니다.

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

## 참고 자료

### 공식 문서
- [PostgreSQL Documentation: Table Access Method Interface Definition](https://www.postgresql.org/docs/current/tableam.html)

### 소스 코드
- `src/include/access/tableam.h` - TableAmRoutine 구조체 정의
- `src/include/executor/tuptable.h` - 튜플 테이블 슬롯 정의
- `src/backend/access/heap/heapam_handler.c` - heap 접근 메서드 구현

### 관련 문서
- Chapter 64: 인덱스 접근 메서드 인터페이스 (Index Access Method Interface)
- Chapter 28: WAL (Write-Ahead Logging)
- Chapter 66.6: 데이터베이스 페이지 레이아웃 (Database Page Layout)
