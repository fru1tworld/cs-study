# Chapter 59: 외래 데이터 래퍼 작성하기 (Writing a Foreign Data Wrapper)

## 목차

1. [개요](#1-개요)
2. [FDW 핸들러 함수](#2-fdw-핸들러-함수)
3. [FDW 콜백 루틴](#3-fdw-콜백-루틴)
4. [헬퍼 함수](#4-헬퍼-함수)
5. [쿼리 계획](#5-쿼리-계획)
6. [행 잠금](#6-행-잠금)
7. [예제 코드](#7-예제-코드)

---

## 1. 개요

### 1.1 외래 데이터 래퍼란?

외래 데이터 래퍼(Foreign Data Wrapper, FDW) 는 PostgreSQL 코어 서버가 외래 테이블에 대한 모든 연산을 처리하기 위해 호출하는 함수들의 집합입니다. FDW는 다음과 같은 책임을 가집니다:

- 원격 데이터 소스에서 데이터 가져오기 (Fetching data from remote sources)
- PostgreSQL 실행기에 데이터 반환하기 (Returning data to the executor)
- 외래 테이블에 대한 업데이트 처리하기 (Handling updates to foreign tables)

### 1.2 아키텍처

외래 테이블에 대한 모든 연산은 해당 외래 데이터 래퍼를 통해 라우팅됩니다. 래퍼는 PostgreSQL이 쿼리 계획(query planning)과 실행(execution) 중에 호출하는 콜백 함수들을 구현해야 합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Core Server                    │
├─────────────────────────────────────────────────────────────┤
│  Query Planner  │     Executor     │   Catalog Manager       │
├─────────────────┴──────────────────┴────────────────────────┤
│                   Foreign Data Wrapper API                   │
├─────────────────────────────────────────────────────────────┤
│   Planning     │   Scanning     │   Modifying    │  Helper   │
│   Callbacks    │   Callbacks    │   Callbacks    │  Functions│
├─────────────────────────────────────────────────────────────┤
│              External Data Source (원격 데이터 소스)           │
│   (PostgreSQL, MySQL, Oracle, File, MongoDB, etc.)          │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 참조 구현

PostgreSQL 표준 배포판의 `contrib` 서브디렉토리에는 자신만의 래퍼를 작성할 때 좋은 참조가 되는 FDW 구현들이 포함되어 있습니다:

- file_fdw: 서버의 파일 시스템에 있는 데이터 파일 접근
- postgres_fdw: 다른 PostgreSQL 서버에 접근
- dblink: 다른 PostgreSQL 데이터베이스에 접근 (레거시)

---

## 2. FDW 핸들러 함수

### 2.1 핸들러 함수 개요

외래 데이터 래퍼는 핸들러 함수(handler function) 를 통해 PostgreSQL에 등록됩니다. 이 함수는 `fdw_handler` 타입을 반환해야 하며, 콜백 함수 포인터들을 포함하는 `FdwRoutine` 구조체를 반환합니다.

### 2.2 FdwRoutine 구조체

`FdwRoutine` 구조체는 `src/include/foreign/fdwapi.h`에 정의되어 있으며, 모든 FDW 콜백 함수 포인터를 포함합니다:

```c
typedef struct FdwRoutine
{
    NodeTag     type;

    /* 스캔 관련 함수 (필수) */
    GetForeignRelSize_function      GetForeignRelSize;
    GetForeignPaths_function        GetForeignPaths;
    GetForeignPlan_function         GetForeignPlan;
    BeginForeignScan_function       BeginForeignScan;
    IterateForeignScan_function     IterateForeignScan;
    ReScanForeignScan_function      ReScanForeignScan;
    EndForeignScan_function         EndForeignScan;

    /* 조인 푸시다운 (선택) */
    GetForeignJoinPaths_function    GetForeignJoinPaths;

    /* 상위 레벨 처리 (선택) */
    GetForeignUpperPaths_function   GetForeignUpperPaths;

    /* 수정 관련 함수 (선택) */
    AddForeignUpdateTargets_function AddForeignUpdateTargets;
    PlanForeignModify_function      PlanForeignModify;
    BeginForeignModify_function     BeginForeignModify;
    ExecForeignInsert_function      ExecForeignInsert;
    ExecForeignUpdate_function      ExecForeignUpdate;
    ExecForeignDelete_function      ExecForeignDelete;
    EndForeignModify_function       EndForeignModify;

    /* 기타 콜백들... */
} FdwRoutine;
```

### 2.3 핸들러 함수 예제

```c
#include "postgres.h"
#include "foreign/fdwapi.h"
#include "foreign/foreign.h"

PG_MODULE_MAGIC;

/* 콜백 함수 선언 */
static void myGetForeignRelSize(PlannerInfo *root,
                                RelOptInfo *baserel,
                                Oid foreigntableid);
static void myGetForeignPaths(PlannerInfo *root,
                              RelOptInfo *baserel,
                              Oid foreigntableid);
/* ... 기타 콜백 함수들 ... */

/* 핸들러 함수 */
PG_FUNCTION_INFO_V1(my_fdw_handler);
Datum
my_fdw_handler(PG_FUNCTION_ARGS)
{
    FdwRoutine *fdwroutine = makeNode(FdwRoutine);

    /* 필수 콜백 설정 */
    fdwroutine->GetForeignRelSize = myGetForeignRelSize;
    fdwroutine->GetForeignPaths = myGetForeignPaths;
    fdwroutine->GetForeignPlan = myGetForeignPlan;
    fdwroutine->BeginForeignScan = myBeginForeignScan;
    fdwroutine->IterateForeignScan = myIterateForeignScan;
    fdwroutine->ReScanForeignScan = myReScanForeignScan;
    fdwroutine->EndForeignScan = myEndForeignScan;

    /* 선택적 콜백 설정 */
    fdwroutine->ExplainForeignScan = myExplainForeignScan;
    fdwroutine->AnalyzeForeignTable = myAnalyzeForeignTable;

    PG_RETURN_POINTER(fdwroutine);
}
```

### 2.4 FDW 등록

```sql
-- 외래 데이터 래퍼 생성
CREATE FOREIGN DATA WRAPPER my_fdw
    HANDLER my_fdw_handler
    VALIDATOR my_fdw_validator;

-- 외래 서버 생성
CREATE SERVER my_server
    FOREIGN DATA WRAPPER my_fdw
    OPTIONS (host 'remote_host', port '5432', dbname 'remote_db');

-- 사용자 매핑 생성
CREATE USER MAPPING FOR current_user
    SERVER my_server
    OPTIONS (user 'remote_user', password 'secret');

-- 외래 테이블 생성
CREATE FOREIGN TABLE remote_table (
    id integer,
    name text,
    value numeric
) SERVER my_server
  OPTIONS (schema_name 'public', table_name 'source_table');
```

---

## 3. FDW 콜백 루틴

### 3.1 스캔 콜백 (Scanning Callbacks)

외래 테이블을 스캔하기 위한 필수 콜백 함수들입니다.

#### GetForeignRelSize

```c
void GetForeignRelSize(PlannerInfo *root,
                       RelOptInfo *baserel,
                       Oid foreigntableid);
```

목적: 쿼리 계획 시 외래 테이블의 크기 추정치를 얻습니다.

설명:
- `baserel->rows`: 예상 행 수 (WHERE 절 적용 후)
- `baserel->width`: 평균 행 너비 (바이트)
- `baserel->tuples`: 테이블의 총 행 수 (선택적)

```c
static void
myGetForeignRelSize(PlannerInfo *root,
                    RelOptInfo *baserel,
                    Oid foreigntableid)
{
    MyFdwPlanState *fpinfo;

    /* 개인 계획 상태 초기화 */
    fpinfo = (MyFdwPlanState *) palloc0(sizeof(MyFdwPlanState));
    baserel->fdw_private = (void *) fpinfo;

    /* 테이블 크기 추정 */
    baserel->rows = 1000;    /* 예상 행 수 */
    baserel->tuples = 1000;  /* 총 행 수 */
}
```

#### GetForeignPaths

```c
void GetForeignPaths(PlannerInfo *root,
                     RelOptInfo *baserel,
                     Oid foreigntableid);
```

목적: 외래 테이블 스캔을 위한 가능한 접근 경로(access paths)를 생성합니다.

설명:
- 최소 하나의 `ForeignPath` 노드를 생성해야 함
- `add_path`를 호출하여 경로를 `baserel->pathlist`에 추가

```c
static void
myGetForeignPaths(PlannerInfo *root,
                  RelOptInfo *baserel,
                  Oid foreigntableid)
{
    MyFdwPlanState *fpinfo = (MyFdwPlanState *) baserel->fdw_private;
    ForeignPath *path;
    Cost        startup_cost;
    Cost        total_cost;

    /* 비용 추정 */
    startup_cost = 10;      /* 연결 설정 비용 */
    total_cost = startup_cost + baserel->rows * 0.01;

    /* ForeignPath 생성 */
    path = create_foreignscan_path(root,
                                   baserel,
                                   NULL,        /* default pathtarget */
                                   baserel->rows,
                                   startup_cost,
                                   total_cost,
                                   NIL,         /* no pathkeys */
                                   NULL,        /* no outer rel */
                                   NULL,        /* no extra plan */
                                   NIL);        /* no fdw_private */

    /* 경로 추가 */
    add_path(baserel, (Path *) path);
}
```

#### GetForeignPlan

```c
ForeignScan *GetForeignPlan(PlannerInfo *root,
                            RelOptInfo *baserel,
                            Oid foreigntableid,
                            ForeignPath *best_path,
                            List *tlist,
                            List *scan_clauses,
                            Plan *outer_plan);
```

목적: 선택된 외래 접근 경로로부터 `ForeignScan` 계획 노드를 생성합니다.

```c
static ForeignScan *
myGetForeignPlan(PlannerInfo *root,
                 RelOptInfo *baserel,
                 Oid foreigntableid,
                 ForeignPath *best_path,
                 List *tlist,
                 List *scan_clauses,
                 Plan *outer_plan)
{
    Index       scan_relid = baserel->relid;
    List       *fdw_private;
    List       *local_exprs = NIL;
    List       *remote_exprs = NIL;

    /* scan_clauses에서 RestrictInfo 노드 추출 */
    scan_clauses = extract_actual_clauses(scan_clauses, false);

    /* 원격 실행 가능한 조건과 로컬 조건 분류 */
    classifyConditions(root, baserel, scan_clauses,
                       &remote_exprs, &local_exprs);

    /* FDW 개인 데이터 준비 */
    fdw_private = list_make2(makeString("SELECT * FROM remote_table"),
                             remote_exprs);

    /* ForeignScan 노드 생성 */
    return make_foreignscan(tlist,
                            local_exprs,     /* 로컬에서 검사할 조건 */
                            scan_relid,
                            NIL,             /* fdw_exprs */
                            fdw_private,     /* FDW 개인 데이터 */
                            NIL,             /* fdw_scan_tlist */
                            NIL,             /* fdw_recheck_quals */
                            outer_plan);
}
```

#### BeginForeignScan

```c
void BeginForeignScan(ForeignScanState *node, int eflags);
```

목적: 실행기 시작 시 스캔을 초기화합니다.

주의: `(eflags & EXEC_FLAG_EXPLAIN_ONLY)`가 참이면 외부에서 볼 수 있는 동작을 수행하지 말아야 합니다 (EXPLAIN 전용).

```c
static void
myBeginForeignScan(ForeignScanState *node, int eflags)
{
    ForeignScan *plan = (ForeignScan *) node->ss.ps.plan;
    MyFdwExecState *festate;

    /* EXPLAIN만을 위한 경우 실제 초기화 건너뛰기 */
    if (eflags & EXEC_FLAG_EXPLAIN_ONLY)
        return;

    /* 실행 상태 초기화 */
    festate = (MyFdwExecState *) palloc0(sizeof(MyFdwExecState));
    node->fdw_state = (void *) festate;

    /* 원격 서버에 연결 */
    festate->conn = GetConnection(node);

    /* 쿼리 준비 */
    festate->query = strVal(linitial(plan->fdw_private));
}
```

#### IterateForeignScan

```c
TupleTableSlot *IterateForeignScan(ForeignScanState *node);
```

목적: 외래 소스에서 한 행을 가져옵니다.

반환값: 행이 더 이상 없으면 NULL을 반환합니다.

```c
static TupleTableSlot *
myIterateForeignScan(ForeignScanState *node)
{
    MyFdwExecState *festate = (MyFdwExecState *) node->fdw_state;
    TupleTableSlot *slot = node->ss.ss_ScanTupleSlot;
    HeapTuple       tuple;

    /* 슬롯 초기화 */
    ExecClearTuple(slot);

    /* 다음 행 가져오기 */
    tuple = FetchNextRow(festate);

    if (tuple != NULL)
    {
        /* 튜플을 슬롯에 저장 */
        ExecStoreHeapTuple(tuple, slot, false);
    }

    return slot;
}
```

#### ReScanForeignScan

```c
void ReScanForeignScan(ForeignScanState *node);
```

목적: 스캔을 처음부터 다시 시작합니다. 매개변수 값이 변경되었을 수 있습니다.

```c
static void
myReScanForeignScan(ForeignScanState *node)
{
    MyFdwExecState *festate = (MyFdwExecState *) node->fdw_state;

    /* 커서를 처음으로 되감기 */
    ResetRemoteCursor(festate);
}
```

#### EndForeignScan

```c
void EndForeignScan(ForeignScanState *node);
```

목적: 스캔을 종료하고 리소스(파일, 연결 등)를 해제합니다.

```c
static void
myEndForeignScan(ForeignScanState *node)
{
    MyFdwExecState *festate = (MyFdwExecState *) node->fdw_state;

    if (festate != NULL)
    {
        /* 연결 해제 */
        ReleaseConnection(festate->conn);

        /* 메모리 해제 */
        pfree(festate);
    }
}
```

### 3.2 조인 스캔 콜백 (Join Scanning Callbacks)

#### GetForeignJoinPaths

```c
void GetForeignJoinPaths(PlannerInfo *root,
                         RelOptInfo *joinrel,
                         RelOptInfo *outerrel,
                         RelOptInfo *innerrel,
                         JoinType jointype,
                         JoinPathExtraData *extra);
```

목적: 동일한 서버에 있는 여러 외래 테이블의 조인을 위한 접근 경로를 생성합니다.

설명:
- 조인을 원격에서 실행할 수 있는지 결정
- `fdw_scan_tlist`를 적절한 `TargetEntry` 노드로 설정해야 함
- 실패해도 괜찮음 (로컬 조인이 항상 가능)

```c
static void
myGetForeignJoinPaths(PlannerInfo *root,
                      RelOptInfo *joinrel,
                      RelOptInfo *outerrel,
                      RelOptInfo *innerrel,
                      JoinType jointype,
                      JoinPathExtraData *extra)
{
    ForeignPath *joinpath;
    Cost         startup_cost;
    Cost         total_cost;

    /* 조인이 원격 실행 가능한지 확인 */
    if (!foreign_join_ok(root, joinrel, jointype, outerrel, innerrel, extra))
        return;

    /* 비용 추정 */
    estimate_join_cost(root, joinrel, outerrel, innerrel,
                       &startup_cost, &total_cost);

    /* 조인 경로 생성 */
    joinpath = create_foreign_join_path(root,
                                        joinrel,
                                        NULL,
                                        joinrel->rows,
                                        startup_cost,
                                        total_cost,
                                        NIL,
                                        NULL,
                                        NULL,
                                        NIL);

    /* 경로 추가 */
    add_path(joinrel, (Path *) joinpath);
}
```

### 3.3 상위 레벨 계획 콜백 (Upper-Level Planning Callbacks)

#### GetForeignUpperPaths

```c
void GetForeignUpperPaths(PlannerInfo *root,
                          UpperRelationKind stage,
                          RelOptInfo *input_rel,
                          RelOptInfo *output_rel,
                          void *extra);
```

목적: 스캔/조인 후의 처리(집계, 정렬 등)를 위한 경로를 생성합니다.

stage 값:
- `UPPERREL_SETOP`: UNION/INTERSECT/EXCEPT 처리
- `UPPERREL_PARTIAL_GROUP_AGG`: 부분 그룹화/집계
- `UPPERREL_GROUP_AGG`: 그룹화/집계
- `UPPERREL_WINDOW`: 윈도우 함수 처리
- `UPPERREL_DISTINCT`: DISTINCT 처리
- `UPPERREL_ORDERED`: ORDER BY 처리
- `UPPERREL_FINAL`: 최종 처리

```c
static void
myGetForeignUpperPaths(PlannerInfo *root,
                       UpperRelationKind stage,
                       RelOptInfo *input_rel,
                       RelOptInfo *output_rel,
                       void *extra)
{
    ForeignPath *path;

    /* GROUP BY만 처리 */
    if (stage != UPPERREL_GROUP_AGG)
        return;

    /* 원격에서 집계 가능한지 확인 */
    if (!foreign_grouping_ok(root, input_rel, output_rel))
        return;

    /* 경로 생성 및 추가 */
    path = create_foreign_upper_path(root,
                                     output_rel,
                                     output_rel->reltarget,
                                     output_rel->rows,
                                     100,  /* startup_cost */
                                     1000, /* total_cost */
                                     NIL,
                                     NULL,
                                     NIL);

    add_path(output_rel, (Path *) path);
}
```

### 3.4 업데이트 콜백 (Updating Callbacks)

외래 테이블에 대한 INSERT, UPDATE, DELETE 연산을 지원하기 위한 콜백들입니다.

#### AddForeignUpdateTargets

```c
void AddForeignUpdateTargets(PlannerInfo *root,
                             Index rtindex,
                             RangeTblEntry *target_rte,
                             Relation target_relation);
```

목적: UPDATE/DELETE 연산에 필요한 숨겨진 "junk" 컬럼을 추가합니다.

```c
static void
myAddForeignUpdateTargets(PlannerInfo *root,
                          Index rtindex,
                          RangeTblEntry *target_rte,
                          Relation target_relation)
{
    Var *var;

    /* 행 식별자 컬럼 추가 (예: ctid와 유사한 역할) */
    var = makeVar(rtindex,
                  SelfItemPointerAttributeNumber,
                  TIDOID,
                  -1,
                  InvalidOid,
                  0);

    add_row_identity_var(root, var, rtindex, "ctid");
}
```

#### PlanForeignModify

```c
List *PlanForeignModify(PlannerInfo *root,
                        ModifyTable *plan,
                        Index resultRelation,
                        int subplan_index);
```

목적: INSERT/UPDATE/DELETE 연산을 위한 FDW 개인 정보를 생성합니다.

```c
static List *
myPlanForeignModify(PlannerInfo *root,
                    ModifyTable *plan,
                    Index resultRelation,
                    int subplan_index)
{
    CmdType     operation = plan->operation;
    RangeTblEntry *rte = planner_rt_fetch(resultRelation, root);
    Relation    rel;
    StringInfoData sql;
    List       *targetAttrs = NIL;

    initStringInfo(&sql);

    /* 테이블 열기 */
    rel = table_open(rte->relid, NoLock);

    /* 연산에 따른 SQL 생성 */
    switch (operation)
    {
        case CMD_INSERT:
            appendStringInfo(&sql, "INSERT INTO %s VALUES (...)",
                             RelationGetRelationName(rel));
            break;
        case CMD_UPDATE:
            appendStringInfo(&sql, "UPDATE %s SET ... WHERE ...",
                             RelationGetRelationName(rel));
            break;
        case CMD_DELETE:
            appendStringInfo(&sql, "DELETE FROM %s WHERE ...",
                             RelationGetRelationName(rel));
            break;
        default:
            elog(ERROR, "unexpected operation: %d", (int) operation);
    }

    table_close(rel, NoLock);

    return list_make1(makeString(sql.data));
}
```

#### BeginForeignModify

```c
void BeginForeignModify(ModifyTableState *mtstate,
                        ResultRelInfo *rinfo,
                        List *fdw_private,
                        int subplan_index,
                        int eflags);
```

목적: 실행기 시작 시 테이블 수정 연산을 초기화합니다.

```c
static void
myBeginForeignModify(ModifyTableState *mtstate,
                     ResultRelInfo *rinfo,
                     List *fdw_private,
                     int subplan_index,
                     int eflags)
{
    MyFdwModifyState *fmstate;

    /* EXPLAIN만을 위한 경우 건너뛰기 */
    if (eflags & EXEC_FLAG_EXPLAIN_ONLY)
        return;

    /* 수정 상태 초기화 */
    fmstate = (MyFdwModifyState *) palloc0(sizeof(MyFdwModifyState));
    rinfo->ri_FdwState = fmstate;

    /* 연결 설정 */
    fmstate->conn = GetConnection(rinfo->ri_RelationDesc);

    /* SQL 문 준비 */
    fmstate->query = strVal(linitial(fdw_private));
}
```

#### ExecForeignInsert

```c
TupleTableSlot *ExecForeignInsert(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에 한 튜플을 삽입합니다.

```c
static TupleTableSlot *
myExecForeignInsert(EState *estate,
                    ResultRelInfo *rinfo,
                    TupleTableSlot *slot,
                    TupleTableSlot *planSlot)
{
    MyFdwModifyState *fmstate = (MyFdwModifyState *) rinfo->ri_FdwState;

    /* 원격 서버에 INSERT 실행 */
    if (!ExecuteRemoteInsert(fmstate, slot))
    {
        /* 삽입 실패 시 NULL 반환 */
        return NULL;
    }

    /* 실제 삽입된 데이터가 있는 슬롯 반환 */
    return slot;
}
```

#### ExecForeignBatchInsert

```c
TupleTableSlot ExecForeignBatchInsert(EState *estate,
                                        ResultRelInfo *rinfo,
                                        TupleTableSlot slots,
                                        TupleTableSlot planSlots,
                                        int *numSlots);
```

목적: 여러 튜플을 일괄 삽입합니다.

```c
static TupleTableSlot 
myExecForeignBatchInsert(EState *estate,
                         ResultRelInfo *rinfo,
                         TupleTableSlot slots,
                         TupleTableSlot planSlots,
                         int *numSlots)
{
    MyFdwModifyState *fmstate = (MyFdwModifyState *) rinfo->ri_FdwState;
    int inserted;

    /* 일괄 INSERT 실행 */
    inserted = ExecuteRemoteBatchInsert(fmstate, slots, *numSlots);

    /* 실제 삽입된 행 수 업데이트 */
    *numSlots = inserted;

    return slots;
}
```

#### GetForeignModifyBatchSize

```c
int GetForeignModifyBatchSize(ResultRelInfo *rinfo);
```

목적: 단일 `ExecForeignBatchInsert` 호출이 처리할 수 있는 최대 튜플 수를 보고합니다.

```c
static int
myGetForeignModifyBatchSize(ResultRelInfo *rinfo)
{
    /* 한 번에 최대 100개 행 삽입 가능 */
    return 100;
}
```

#### ExecForeignUpdate

```c
TupleTableSlot *ExecForeignUpdate(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에서 한 튜플을 업데이트합니다.

```c
static TupleTableSlot *
myExecForeignUpdate(EState *estate,
                    ResultRelInfo *rinfo,
                    TupleTableSlot *slot,
                    TupleTableSlot *planSlot)
{
    MyFdwModifyState *fmstate = (MyFdwModifyState *) rinfo->ri_FdwState;
    Datum       datum;
    bool        isNull;

    /* junk 컬럼에서 행 식별자 추출 */
    datum = ExecGetJunkAttribute(planSlot,
                                 fmstate->ctidAttno,
                                 &isNull);

    /* 원격 UPDATE 실행 */
    if (!ExecuteRemoteUpdate(fmstate, slot, DatumGetItemPointer(datum)))
        return NULL;

    return slot;
}
```

#### ExecForeignDelete

```c
TupleTableSlot *ExecForeignDelete(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에서 한 튜플을 삭제합니다.

```c
static TupleTableSlot *
myExecForeignDelete(EState *estate,
                    ResultRelInfo *rinfo,
                    TupleTableSlot *slot,
                    TupleTableSlot *planSlot)
{
    MyFdwModifyState *fmstate = (MyFdwModifyState *) rinfo->ri_FdwState;
    Datum       datum;
    bool        isNull;

    /* junk 컬럼에서 행 식별자 추출 */
    datum = ExecGetJunkAttribute(planSlot,
                                 fmstate->ctidAttno,
                                 &isNull);

    /* 원격 DELETE 실행 */
    if (!ExecuteRemoteDelete(fmstate, DatumGetItemPointer(datum)))
        return NULL;

    return slot;
}
```

#### EndForeignModify

```c
void EndForeignModify(EState *estate, ResultRelInfo *rinfo);
```

목적: 테이블 수정을 종료하고 리소스를 해제합니다.

```c
static void
myEndForeignModify(EState *estate, ResultRelInfo *rinfo)
{
    MyFdwModifyState *fmstate = (MyFdwModifyState *) rinfo->ri_FdwState;

    if (fmstate != NULL)
    {
        /* 연결 해제 */
        ReleaseConnection(fmstate->conn);
    }
}
```

#### IsForeignRelUpdatable

```c
int IsForeignRelUpdatable(Relation rel);
```

목적: 외래 테이블이 지원하는 업데이트 연산을 비트마스크로 반환합니다.

```c
static int
myIsForeignRelUpdatable(Relation rel)
{
    /* INSERT, UPDATE, DELETE 모두 지원 */
    return (1 << CMD_INSERT) | (1 << CMD_UPDATE) | (1 << CMD_DELETE);
    /* 비트 값: INSERT=8, UPDATE=4, DELETE=16 */
}
```

### 3.5 직접 수정 콜백 (Direct Modification Callbacks)

원격 서버에서 직접 수정을 수행하기 위한 콜백들입니다.

#### PlanDirectModify

```c
bool PlanDirectModify(PlannerInfo *root,
                      ModifyTable *plan,
                      Index resultRelation,
                      int subplan_index);
```

목적: 원격 서버에서 직접 수정이 안전한지 결정합니다.

```c
static bool
myPlanDirectModify(PlannerInfo *root,
                   ModifyTable *plan,
                   Index resultRelation,
                   int subplan_index)
{
    CmdType     operation = plan->operation;

    /* DELETE와 UPDATE만 직접 수정 지원 */
    if (operation != CMD_DELETE && operation != CMD_UPDATE)
        return false;

    /* 조건 검사 및 서브플랜을 ForeignScan으로 재작성 */
    /* ... */

    return true;
}
```

#### BeginDirectModify

```c
void BeginDirectModify(ForeignScanState *node, int eflags);
```

목적: 직접 수정 실행을 준비합니다.

#### IterateDirectModify

```c
TupleTableSlot *IterateDirectModify(ForeignScanState *node);
```

목적: RETURNING 절을 위한 결과 데이터를 가져옵니다.

#### EndDirectModify

```c
void EndDirectModify(ForeignScanState *node);
```

목적: 직접 수정 후 정리합니다.

### 3.6 TRUNCATE 콜백

#### ExecForeignTruncate

```c
void ExecForeignTruncate(List *rels,
                         DropBehavior behavior,
                         bool restart_seqs);
```

목적: 외래 테이블을 TRUNCATE합니다.

매개변수:
- `behavior`: `DROP_RESTRICT` 또는 `DROP_CASCADE`
- `restart_seqs`: `RESTART IDENTITY` 여부

```c
static void
myExecForeignTruncate(List *rels,
                      DropBehavior behavior,
                      bool restart_seqs)
{
    ListCell   *lc;

    foreach(lc, rels)
    {
        Relation rel = lfirst(lc);

        /* 각 테이블에 대해 원격 TRUNCATE 실행 */
        ExecuteRemoteTruncate(rel, behavior, restart_seqs);
    }
}
```

### 3.7 EXPLAIN 콜백

#### ExplainForeignScan

```c
void ExplainForeignScan(ForeignScanState *node, ExplainState *es);
```

목적: 외래 테이블 스캔에 대한 추가 EXPLAIN 출력을 인쇄합니다.

```c
static void
myExplainForeignScan(ForeignScanState *node, ExplainState *es)
{
    MyFdwExecState *festate = (MyFdwExecState *) node->fdw_state;

    /* 원격 쿼리 표시 */
    if (festate && festate->query)
        ExplainPropertyText("Remote SQL", festate->query, es);
}
```

#### ExplainForeignModify

```c
void ExplainForeignModify(ModifyTableState *mtstate,
                          ResultRelInfo *rinfo,
                          List *fdw_private,
                          int subplan_index,
                          struct ExplainState *es);
```

목적: 테이블 업데이트에 대한 추가 EXPLAIN 출력을 인쇄합니다.

### 3.8 ANALYZE 콜백

#### AnalyzeForeignTable

```c
bool AnalyzeForeignTable(Relation relation,
                         AcquireSampleRowsFunc *func,
                         BlockNumber *totalpages);
```

목적: 외래 테이블에 대한 통계를 수집합니다.

```c
static bool
myAnalyzeForeignTable(Relation relation,
                      AcquireSampleRowsFunc *func,
                      BlockNumber *totalpages)
{
    /* 샘플 행 수집 함수 설정 */
    *func = my_acquire_sample_rows;

    /* 총 페이지 수 추정 */
    *totalpages = 100;

    return true;
}

static int
my_acquire_sample_rows(Relation relation,
                       int elevel,
                       HeapTuple *rows,
                       int targrows,
                       double *totalrows,
                       double *totaldeadrows)
{
    int numrows = 0;

    /* 샘플 행 수집 */
    /* ... */

    *totalrows = 10000;
    *totaldeadrows = 0;

    return numrows;
}
```

### 3.9 IMPORT FOREIGN SCHEMA 콜백

#### ImportForeignSchema

```c
List *ImportForeignSchema(ImportForeignSchemaStmt *stmt, Oid serverOid);
```

목적: 원격 스키마에서 테이블 정의를 가져옵니다.

필터 유형:
- `FDW_IMPORT_SCHEMA_ALL`: 모든 테이블
- `FDW_IMPORT_SCHEMA_LIMIT_TO`: 지정된 테이블만
- `FDW_IMPORT_SCHEMA_EXCEPT`: 지정된 테이블 제외

```c
static List *
myImportForeignSchema(ImportForeignSchemaStmt *stmt, Oid serverOid)
{
    List       *commands = NIL;
    StringInfoData cmd;

    /* 원격 스키마에서 테이블 정보 조회 */
    /* ... */

    initStringInfo(&cmd);
    appendStringInfo(&cmd,
                     "CREATE FOREIGN TABLE %s.%s (\n"
                     "    id integer,\n"
                     "    name text\n"
                     ") SERVER %s",
                     stmt->local_schema,
                     "remote_table",
                     get_server_name(serverOid));

    commands = lappend(commands, pstrdup(cmd.data));

    return commands;
}
```

### 3.10 병렬 실행 콜백 (Parallel Execution Callbacks)

#### IsForeignScanParallelSafe

```c
bool IsForeignScanParallelSafe(PlannerInfo *root,
                               RelOptInfo *rel,
                               RangeTblEntry *rte);
```

목적: 스캔이 병렬 워커에서 수행될 수 있는지 테스트합니다.

#### EstimateDSMForeignScan

```c
Size EstimateDSMForeignScan(ForeignScanState *node, ParallelContext *pcxt);
```

목적: 필요한 동적 공유 메모리 크기를 추정합니다 (바이트 단위).

#### InitializeDSMForeignScan

```c
void InitializeDSMForeignScan(ForeignScanState *node,
                              ParallelContext *pcxt,
                              void *coordinate);
```

목적: 병렬 연산을 위한 공유 메모리를 초기화합니다.

#### InitializeWorkerForeignScan

```c
void InitializeWorkerForeignScan(ForeignScanState *node,
                                 shm_toc *toc,
                                 void *coordinate);
```

목적: 리더의 공유 상태에서 병렬 워커의 로컬 상태를 초기화합니다.

#### ShutdownForeignScan

```c
void ShutdownForeignScan(ForeignScanState *node);
```

목적: DSM 세그먼트 파괴 전에 리소스를 해제합니다.

### 3.11 비동기 실행 콜백 (Asynchronous Execution Callbacks)

#### IsForeignPathAsyncCapable

```c
bool IsForeignPathAsyncCapable(ForeignPath *path);
```

목적: 경로가 외래 릴레이션을 비동기적으로 스캔할 수 있는지 테스트합니다.

#### ForeignAsyncRequest

```c
void ForeignAsyncRequest(AsyncRequest *areq);
```

목적: 비동기적으로 한 튜플을 생성합니다.

#### ForeignAsyncConfigureWait

```c
void ForeignAsyncConfigureWait(AsyncRequest *areq);
```

목적: 대기할 파일 디스크립터 이벤트를 구성합니다.

#### ForeignAsyncNotify

```c
void ForeignAsyncNotify(AsyncRequest *areq);
```

목적: 관련 이벤트를 처리하고 비동기적으로 한 튜플을 생성합니다.

---

## 4. 헬퍼 함수

PostgreSQL 코어 서버에서 FDW 개발을 위해 제공하는 유틸리티 함수들입니다.

### 4.1 ForeignDataWrapper 함수

```c
/* OID로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapper(Oid fdwid);

/* 확장 옵션으로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapperExtended(Oid fdwid, bits16 flags);
/* flags: FDW_MISSING_OK - 없으면 에러 대신 NULL 반환 */

/* 이름으로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapperByName(const char *name, bool missing_ok);
```

### 4.2 ForeignServer 함수

```c
/* OID로 서버 객체 가져오기 */
ForeignServer *GetForeignServer(Oid serverid);

/* 확장 옵션으로 서버 객체 가져오기 */
ForeignServer *GetForeignServerExtended(Oid serverid, bits16 flags);
/* flags: FSV_MISSING_OK - 없으면 에러 대신 NULL 반환 */

/* 이름으로 서버 객체 가져오기 */
ForeignServer *GetForeignServerByName(const char *name, bool missing_ok);
```

### 4.3 사용자 매핑 및 테이블 함수

```c
/* 사용자 매핑 가져오기 */
UserMapping *GetUserMapping(Oid userid, Oid serverid);
/* 특정 사용자 매핑이 없으면 PUBLIC 매핑으로 대체 */

/* 외래 테이블 정보 가져오기 */
ForeignTable *GetForeignTable(Oid relid);
```

### 4.4 컬럼 옵션 함수

```c
/* 컬럼별 FDW 옵션 가져오기 */
List *GetForeignColumnOptions(Oid relid, AttrNumber attnum);
/* DefElem 리스트로 반환, 옵션이 없으면 NIL */
```

### 4.5 사용 예제

```c
#include "foreign/foreign.h"

static void
example_helper_usage(Oid foreigntableid)
{
    ForeignTable *table;
    ForeignServer *server;
    ForeignDataWrapper *fdw;
    UserMapping *user;
    ListCell *lc;

    /* 외래 테이블 정보 가져오기 */
    table = GetForeignTable(foreigntableid);

    /* 서버 정보 가져오기 */
    server = GetForeignServer(table->serverid);

    /* FDW 정보 가져오기 */
    fdw = GetForeignDataWrapper(server->fdwid);

    /* 현재 사용자의 매핑 가져오기 */
    user = GetUserMapping(GetUserId(), server->serverid);

    /* 테이블 옵션 처리 */
    foreach(lc, table->options)
    {
        DefElem *def = (DefElem *) lfirst(lc);
        elog(INFO, "Option: %s = %s", def->defname, defGetString(def));
    }

    /* 컬럼 옵션 처리 */
    for (AttrNumber i = 1; i <= 10; i++)
    {
        List *colopts = GetForeignColumnOptions(foreigntableid, i);
        /* colopts 처리... */
    }
}
```

---

## 5. 쿼리 계획

### 5.1 계획 참여 콜백

다음 FDW 콜백 함수들이 PostgreSQL의 쿼리 계획에 참여합니다:

- `GetForeignRelSize`
- `GetForeignPaths`
- `GetForeignPlan`
- `PlanForeignModify`
- `GetForeignJoinPaths`
- `GetForeignUpperPaths`
- `PlanDirectModify`

### 5.2 주요 계획 정보

#### baserestrictinfo와 비용 절감

`root`와 `baserel` 매개변수를 통해 FDW가 데이터 가져오기를 줄일 수 있습니다:

`baserel->baserestrictinfo`: 행을 필터링해야 하는 제한 조건(WHERE 절)을 포함합니다. FDW는 이를 적용하거나 코어 실행기에 맡길 수 있습니다.

`baserel->reltarget->exprs`: ForeignScan 노드가 가져와야 하는 컬럼을 결정합니다 (조건 평가에만 사용되는 컬럼은 제외).

```c
static void
analyze_restrictions(RelOptInfo *baserel)
{
    ListCell *lc;

    foreach(lc, baserel->baserestrictinfo)
    {
        RestrictInfo *rinfo = (RestrictInfo *) lfirst(lc);
        Expr *clause = rinfo->clause;

        /* 조건이 원격 실행 가능한지 확인 */
        if (is_foreign_expr(baserel, clause))
        {
            /* 원격에서 실행할 조건으로 분류 */
        }
        else
        {
            /* 로컬에서 실행할 조건으로 분류 */
        }
    }
}
```

#### 개인 필드 저장

`baserel->fdw_private`: FDW가 외래 테이블에 대한 정보를 저장하기 위한 void 포인터입니다. 계획자가 NULL로 초기화하고 이후 건드리지 않습니다. 계획 단계 간에 정보를 전달하면서 재계산을 피하는 데 유용합니다.

`ForeignPath->fdw_private`: 접근 경로 의미를 저장하기 위한 List 포인터입니다. 디버깅을 위해 `nodeToString`으로 덤프 가능한 표현을 사용하는 것이 좋습니다.

### 5.3 GetForeignPlan 구현

`GetForeignPlan`에서 FDW는 다음을 할 수 있습니다:

1. 타겟 리스트 복사: 그대로 계획 노드에 복사
2. scan_clauses 처리: 실제 조건을 추출하여 qual 리스트에 배치
3. 내부 조건 제거: FDW가 원격에서 처리하면 계획 노드의 qual 리스트에서 제거
4. fdw_exprs에 하위 표현식 추가: 실행 시점 평가를 보장

### 5.4 조건 재검사 요구사항

qual 리스트에서 제거된 조건은 다음 중 하나여야 합니다:
- `fdw_recheck_quals`에 추가되거나
- `RecheckForeignScan`에서 재검사

이는 `READ COMMITTED` 격리 수준에서 동시 업데이트 발생 시 올바른 동작을 보장합니다. 잠재적 NULL 필드가 있는 푸시다운된 외부 조인의 경우 `RecheckForeignScan` 구현이 필요할 수 있습니다.

### 5.5 ForeignScan 출력 설명

`fdw_scan_tlist`: FDW가 반환하는 튜플을 설명합니다:

- NIL: 반환된 튜플이 외래 테이블의 선언된 행 타입과 일치
- Non-NIL: 반환된 컬럼을 나타내는 Var/표현식이 있는 TargetEntry 리스트

사용 사례:
- 쿼리에 필요 없는 생략된 컬럼 문서화
- FDW가 로컬 실행보다 저렴하게 계산하는 표현식 포함
- `GetForeignJoinPaths`에서 생성된 조인 계획에 필수

### 5.6 조인을 위한 매개변수화된 경로

FDW는 다음을 구성해야 합니다:

1. 최소 하나의 경로: 테이블 제한 조건에만 의존
2. 매개변수화된 경로: 조인용 (예: `foreign_variable = local_variable`)
   - 릴레이션의 조인 리스트에서 조인 조건 검색 (`baserestrictinfo`에 없음)
   - `get_baserel_parampathinfo`를 사용하여 `param_info` 설정
   - `fdw_exprs`에 local_variable 부분 추가

### 5.7 원격 조인 지원

`GetForeignJoinPaths`는 `GetForeignPaths`와 유사하게 원격 조인을 위한 `ForeignPath`를 생성합니다:

- 조인 조건이 `extra->restrictlist`로 전달됨 (`baserestrictinfo`가 아님)
- 경로의 `fdw_private`을 통해 `GetForeignPlan`에 정보 전달

### 5.8 상위 레벨 연산

#### GetForeignUpperPaths

스캔/조인 위의 연산 (그룹화, 집계)용:

1. 원격 실행을 위한 경로 생성
2. 적절한 상위 릴레이션에 삽입 (예: `UPPERREL_GROUP_AGG`)
3. `add_path`를 사용하여 비용 기반으로 로컬 처리와 경쟁
4. 승리한 경로를 `GetForeignPlan`을 통해 계획으로 변환

### 5.9 UPDATE/DELETE 계획

`PlanForeignModify`와 `PlanDirectModify`는 다음에 접근할 수 있습니다:
- 외래 테이블의 RelOptInfo 구조체
- 스캔 계획 중 생성된 baserel->fdw_private 데이터

반환된 `List`는 `copyObject`가 복사 방법을 아는 구조체만 포함해야 합니다.

---

## 6. 행 잠금

### 6.1 개요

외래 데이터 래퍼(FDW)는 행 수준 잠금을 구현하여 동시 업데이트를 방지하고, PostgreSQL의 표준 테이블 잠금 의미론을 근사할 수 있습니다.

### 6.2 초기 잠금 vs 지연 잠금

#### 초기 잠금 (Early Locking)

- 기본 저장소에서 처음 검색될 때 행을 잠금
- 더 간단한 구현, 더 적은 원격 왕복
- 단점: 불필요한 행을 잠글 수 있어 동시성 감소 또는 교착 상태 발생 가능

```c
/* 초기 잠금 예제 */
static TupleTableSlot *
myIterateForeignScan_EarlyLock(ForeignScanState *node)
{
    /* SELECT FOR UPDATE 또는 UPDATE/DELETE 타겟인 경우 */
    if (node->ss.ps.state->es_rowmark_map != NULL ||
        ExecRelationIsTargetRelation(node->ss.ps.state,
                                     node->ss.ss_currentRelation->rd_id))
    {
        /* 잠금과 함께 행 가져오기 */
        return FetchRowWithLock(node);
    }
    else
    {
        /* 잠금 없이 행 가져오기 */
        return FetchRow(node);
    }
}
```

#### 지연 잠금 (Late Locking)

- 필요할 때만 행을 잠금
- 더 복잡함; 행을 고유하게 재식별하는 능력 필요
- PostgreSQL TID와 같이 특정 행 버전을 식별하는 행 식별자 필요
- 섹션 58.2.6의 API 함수가 지연 잠금 지원

### 6.3 구현 접근법

#### UPDATE/DELETE 연산용

`ForeignScan` 연산은 타겟 테이블 행에 대해 초기 잠금 을 수행해야 합니다:

```
ForeignScan → SELECT FOR UPDATE (동등)
```

감지 방법:
- 계획 시점: relid를 `root->parse->resultRelation`과 비교
- 실행 시점: `ExecRelationIsTargetRelation()` 사용

대안: `ExecForeignUpdate` 또는 `ExecForeignDelete` 콜백 내에서 지연 잠금 수행

#### SELECT FOR UPDATE/SHARE 명령용

초기 잠금: `SELECT FOR UPDATE/SHARE`와 동등하게 튜플 가져오기

지연 잠금: 섹션 58.2.6의 콜백 구현:

```c
/* GetForeignRowMarkType은 잠금 강도 옵션 반환 */
static RowMarkType
myGetForeignRowMarkType(RangeTblEntry *rte, LockClauseStrength strength)
{
    switch (strength)
    {
        case LCS_FORUPDATE:
            return ROW_MARK_EXCLUSIVE;
        case LCS_FORNOKEYUPDATE:
            return ROW_MARK_NOKEYEXCLUSIVE;
        case LCS_FORSHARE:
            return ROW_MARK_SHARE;
        case LCS_FORKEYSHARE:
            return ROW_MARK_KEYSHARE;
        case LCS_NONE:
            return ROW_MARK_REFERENCE;
        default:
            return ROW_MARK_COPY;
    }
}
```

감지 방법:
- 계획 시점: `get_plan_rowmark` 사용
- 실행 시점: `ExecFindRowMark` 사용 및 `strength` 필드 != `LCS_NONE` 확인

### 6.4 잠금되지 않은 외래 테이블용

UPDATE/DELETE/SELECT FOR UPDATE/SHARE에서 잠금되지 않은 외래 테이블의 경우, `GetForeignRowMarkType`이 다음을 선택하여 기본 전체 행 복사를 재정의:

- `ROW_MARK_REFERENCE` (잠금 강도 = `LCS_NONE`일 때): 새 잠금 획득 없이 재가져오기 위해 `RefetchForeignRow` 호출
- `ROW_MARK_COPY`: 전체 행 복사 (기본 동작)

### 6.5 READ COMMITTED 격리 고려사항

`READ COMMITTED` 모드에서 PostgreSQL은 업데이트된 튜플에 대해 조건을 재검사해야 할 수 있습니다. FDW는 다음을 할 수 있습니다:

1. 효율적인 재가져오기를 위해 프로젝션된 컬럼에 TID 포함 (저렴한 재가져오기 능력 필요)
2. 컬럼 리스트에 전체 행 복사 (기본) - 특별한 요구 없지만 병합/해시 조인 성능 저하

```c
/* RefetchForeignRow 구현 예제 */
static void
myRefetchForeignRow(EState *estate,
                    ExecRowMark *erm,
                    Datum rowid,
                    TupleTableSlot *slot,
                    bool *updated)
{
    /* rowid를 사용하여 행 재가져오기 */
    HeapTuple tuple = FetchRowById(rowid);

    if (tuple != NULL)
    {
        ExecStoreHeapTuple(tuple, slot, false);

        /* 이전 버전과 다른지 확인 */
        *updated = CheckIfRowUpdated(rowid, tuple);
    }
    else
    {
        ExecClearTuple(slot);
        *updated = false;
    }
}
```

---

## 7. 예제 코드

### 7.1 완전한 FDW 구현 예제

다음은 간단한 CSV 파일 FDW의 완전한 구현 예제입니다.

#### 헤더 파일 (my_csv_fdw.h)

```c
#ifndef MY_CSV_FDW_H
#define MY_CSV_FDW_H

#include "postgres.h"
#include "fmgr.h"
#include "foreign/fdwapi.h"
#include "foreign/foreign.h"
#include "optimizer/pathnode.h"
#include "optimizer/planmain.h"
#include "optimizer/restrictinfo.h"
#include "utils/rel.h"

/* 계획 상태 구조체 */
typedef struct MyCsvFdwPlanState
{
    char       *filename;       /* CSV 파일 경로 */
    List       *options;        /* 옵션 리스트 */
    BlockNumber pages;          /* 추정 페이지 수 */
    double      ntuples;        /* 추정 튜플 수 */
} MyCsvFdwPlanState;

/* 실행 상태 구조체 */
typedef struct MyCsvFdwExecState
{
    char       *filename;       /* CSV 파일 경로 */
    FILE       *file;           /* 파일 핸들 */
    char       *buffer;         /* 읽기 버퍼 */
    int         lineno;         /* 현재 줄 번호 */
    AttrNumber  numattrs;       /* 속성 수 */
    FmgrInfo   *in_funcs;       /* 입력 함수 배열 */
    Oid        *typioparams;    /* 타입 IO 매개변수 */
} MyCsvFdwExecState;

#endif /* MY_CSV_FDW_H */
```

#### 소스 파일 (my_csv_fdw.c)

```c
#include "my_csv_fdw.h"
#include "access/htup_details.h"
#include "access/reloptions.h"
#include "catalog/pg_foreign_table.h"
#include "commands/copy.h"
#include "commands/defrem.h"
#include "commands/explain.h"
#include "executor/executor.h"
#include "miscadmin.h"
#include "nodes/makefuncs.h"
#include "optimizer/optimizer.h"
#include "parser/parsetree.h"
#include "utils/builtins.h"
#include "utils/lsyscache.h"
#include "utils/memutils.h"

PG_MODULE_MAGIC;

/* SQL 인터페이스 함수 선언 */
PG_FUNCTION_INFO_V1(my_csv_fdw_handler);
PG_FUNCTION_INFO_V1(my_csv_fdw_validator);

/* 콜백 함수 선언 */
static void myCsvGetForeignRelSize(PlannerInfo *root, RelOptInfo *baserel,
                                   Oid foreigntableid);
static void myCsvGetForeignPaths(PlannerInfo *root, RelOptInfo *baserel,
                                 Oid foreigntableid);
static ForeignScan *myCsvGetForeignPlan(PlannerInfo *root, RelOptInfo *baserel,
                                        Oid foreigntableid,
                                        ForeignPath *best_path,
                                        List *tlist, List *scan_clauses,
                                        Plan *outer_plan);
static void myCsvBeginForeignScan(ForeignScanState *node, int eflags);
static TupleTableSlot *myCsvIterateForeignScan(ForeignScanState *node);
static void myCsvReScanForeignScan(ForeignScanState *node);
static void myCsvEndForeignScan(ForeignScanState *node);
static void myCsvExplainForeignScan(ForeignScanState *node, ExplainState *es);
static bool myCsvAnalyzeForeignTable(Relation relation,
                                     AcquireSampleRowsFunc *func,
                                     BlockNumber *totalpages);

/* 유틸리티 함수 */
static void estimate_file_size(const char *filename, BlockNumber *pages,
                               double *ntuples);
static bool parse_csv_line(MyCsvFdwExecState *festate, TupleTableSlot *slot);

/*
 * FDW 핸들러 함수
 */
Datum
my_csv_fdw_handler(PG_FUNCTION_ARGS)
{
    FdwRoutine *fdwroutine = makeNode(FdwRoutine);

    /* 스캔 콜백 (필수) */
    fdwroutine->GetForeignRelSize = myCsvGetForeignRelSize;
    fdwroutine->GetForeignPaths = myCsvGetForeignPaths;
    fdwroutine->GetForeignPlan = myCsvGetForeignPlan;
    fdwroutine->BeginForeignScan = myCsvBeginForeignScan;
    fdwroutine->IterateForeignScan = myCsvIterateForeignScan;
    fdwroutine->ReScanForeignScan = myCsvReScanForeignScan;
    fdwroutine->EndForeignScan = myCsvEndForeignScan;

    /* EXPLAIN 콜백 (선택) */
    fdwroutine->ExplainForeignScan = myCsvExplainForeignScan;

    /* ANALYZE 콜백 (선택) */
    fdwroutine->AnalyzeForeignTable = myCsvAnalyzeForeignTable;

    PG_RETURN_POINTER(fdwroutine);
}

/*
 * FDW 옵션 유효성 검사 함수
 */
Datum
my_csv_fdw_validator(PG_FUNCTION_ARGS)
{
    List       *options_list = untransformRelOptions(PG_GETARG_DATUM(0));
    Oid         catalog = PG_GETARG_OID(1);
    ListCell   *cell;
    char       *filename = NULL;

    foreach(cell, options_list)
    {
        DefElem *def = (DefElem *) lfirst(cell);

        if (strcmp(def->defname, "filename") == 0)
        {
            if (catalog == ForeignTableRelationId)
            {
                filename = defGetString(def);

                /* 파일 존재 여부 확인 */
                if (access(filename, R_OK) != 0)
                    ereport(ERROR,
                            (errcode(ERRCODE_FDW_INVALID_OPTION_NAME),
                             errmsg("could not access file \"%s\": %m",
                                    filename)));
            }
        }
        else
        {
            ereport(ERROR,
                    (errcode(ERRCODE_FDW_INVALID_OPTION_NAME),
                     errmsg("invalid option \"%s\"", def->defname)));
        }
    }

    /* filename 옵션은 필수 */
    if (catalog == ForeignTableRelationId && filename == NULL)
        ereport(ERROR,
                (errcode(ERRCODE_FDW_OPTION_NAME_NOT_FOUND),
                 errmsg("filename is required for my_csv_fdw foreign tables")));

    PG_RETURN_VOID();
}

/*
 * GetForeignRelSize: 릴레이션 크기 추정
 */
static void
myCsvGetForeignRelSize(PlannerInfo *root, RelOptInfo *baserel,
                       Oid foreigntableid)
{
    MyCsvFdwPlanState *fpinfo;
    ForeignTable *table;
    ListCell   *lc;

    fpinfo = (MyCsvFdwPlanState *) palloc0(sizeof(MyCsvFdwPlanState));
    baserel->fdw_private = (void *) fpinfo;

    /* 테이블 옵션 가져오기 */
    table = GetForeignTable(foreigntableid);
    foreach(lc, table->options)
    {
        DefElem *def = (DefElem *) lfirst(lc);

        if (strcmp(def->defname, "filename") == 0)
            fpinfo->filename = defGetString(def);
    }

    /* 파일 크기 추정 */
    estimate_file_size(fpinfo->filename, &fpinfo->pages, &fpinfo->ntuples);

    /* 기본 릴레이션 정보 설정 */
    baserel->rows = fpinfo->ntuples;
    baserel->tuples = fpinfo->ntuples;
}

/*
 * GetForeignPaths: 접근 경로 생성
 */
static void
myCsvGetForeignPaths(PlannerInfo *root, RelOptInfo *baserel,
                     Oid foreigntableid)
{
    MyCsvFdwPlanState *fpinfo = (MyCsvFdwPlanState *) baserel->fdw_private;
    ForeignPath *path;
    Cost        startup_cost;
    Cost        total_cost;
    double      ntuples;

    /* 비용 계산 */
    ntuples = baserel->rows;
    startup_cost = 10;                      /* 파일 열기 비용 */
    total_cost = startup_cost +
                 ntuples * 0.01 +           /* 튜플 처리 비용 */
                 fpinfo->pages;             /* I/O 비용 */

    /* 기본 스캔 경로 생성 */
    path = create_foreignscan_path(root, baserel,
                                   NULL,    /* default pathtarget */
                                   ntuples,
                                   startup_cost,
                                   total_cost,
                                   NIL,     /* no pathkeys */
                                   NULL,    /* no outer rel */
                                   NULL,    /* no extra plan */
                                   NIL);    /* no fdw_private */

    add_path(baserel, (Path *) path);
}

/*
 * GetForeignPlan: 계획 노드 생성
 */
static ForeignScan *
myCsvGetForeignPlan(PlannerInfo *root, RelOptInfo *baserel,
                    Oid foreigntableid, ForeignPath *best_path,
                    List *tlist, List *scan_clauses, Plan *outer_plan)
{
    MyCsvFdwPlanState *fpinfo = (MyCsvFdwPlanState *) baserel->fdw_private;
    Index       scan_relid = baserel->relid;
    List       *fdw_private;

    /* scan_clauses에서 RestrictInfo 추출 */
    scan_clauses = extract_actual_clauses(scan_clauses, false);

    /* FDW 개인 데이터: 파일명 저장 */
    fdw_private = list_make1(makeString(fpinfo->filename));

    /* ForeignScan 노드 생성 */
    return make_foreignscan(tlist,
                            scan_clauses,   /* 로컬에서 검사할 조건 */
                            scan_relid,
                            NIL,            /* no fdw_exprs */
                            fdw_private,
                            NIL,            /* no fdw_scan_tlist */
                            NIL,            /* no fdw_recheck_quals */
                            outer_plan);
}

/*
 * BeginForeignScan: 스캔 초기화
 */
static void
myCsvBeginForeignScan(ForeignScanState *node, int eflags)
{
    ForeignScan *plan = (ForeignScan *) node->ss.ps.plan;
    MyCsvFdwExecState *festate;
    TupleDesc   tupdesc;
    int         i;

    /* EXPLAIN ONLY인 경우 초기화 건너뛰기 */
    if (eflags & EXEC_FLAG_EXPLAIN_ONLY)
        return;

    /* 실행 상태 할당 */
    festate = (MyCsvFdwExecState *) palloc0(sizeof(MyCsvFdwExecState));
    node->fdw_state = (void *) festate;

    /* 파일명 가져오기 */
    festate->filename = strVal(linitial(plan->fdw_private));

    /* 파일 열기 */
    festate->file = fopen(festate->filename, "r");
    if (festate->file == NULL)
        ereport(ERROR,
                (errcode_for_file_access(),
                 errmsg("could not open file \"%s\": %m", festate->filename)));

    /* 버퍼 할당 */
    festate->buffer = (char *) palloc(65536);
    festate->lineno = 0;

    /* 튜플 디스크립터 정보 저장 */
    tupdesc = node->ss.ss_currentRelation->rd_att;
    festate->numattrs = tupdesc->natts;

    /* 입력 함수 설정 */
    festate->in_funcs = (FmgrInfo *) palloc(festate->numattrs * sizeof(FmgrInfo));
    festate->typioparams = (Oid *) palloc(festate->numattrs * sizeof(Oid));

    for (i = 0; i < festate->numattrs; i++)
    {
        Form_pg_attribute attr = TupleDescAttr(tupdesc, i);
        Oid         typinput;

        getTypeInputInfo(attr->atttypid, &typinput, &festate->typioparams[i]);
        fmgr_info(typinput, &festate->in_funcs[i]);
    }
}

/*
 * IterateForeignScan: 다음 튜플 가져오기
 */
static TupleTableSlot *
myCsvIterateForeignScan(ForeignScanState *node)
{
    MyCsvFdwExecState *festate = (MyCsvFdwExecState *) node->fdw_state;
    TupleTableSlot *slot = node->ss.ss_ScanTupleSlot;

    /* 슬롯 초기화 */
    ExecClearTuple(slot);

    /* 다음 줄 읽기 및 파싱 */
    if (parse_csv_line(festate, slot))
    {
        ExecStoreVirtualTuple(slot);
    }

    return slot;
}

/*
 * ReScanForeignScan: 스캔 재시작
 */
static void
myCsvReScanForeignScan(ForeignScanState *node)
{
    MyCsvFdwExecState *festate = (MyCsvFdwExecState *) node->fdw_state;

    /* 파일 처음으로 되감기 */
    if (fseek(festate->file, 0, SEEK_SET) != 0)
        ereport(ERROR,
                (errcode_for_file_access(),
                 errmsg("could not seek in file \"%s\": %m",
                        festate->filename)));

    festate->lineno = 0;
}

/*
 * EndForeignScan: 스캔 종료
 */
static void
myCsvEndForeignScan(ForeignScanState *node)
{
    MyCsvFdwExecState *festate = (MyCsvFdwExecState *) node->fdw_state;

    if (festate != NULL)
    {
        /* 파일 닫기 */
        if (festate->file != NULL)
            fclose(festate->file);

        /* 메모리 해제 */
        if (festate->buffer != NULL)
            pfree(festate->buffer);
        if (festate->in_funcs != NULL)
            pfree(festate->in_funcs);
        if (festate->typioparams != NULL)
            pfree(festate->typioparams);
    }
}

/*
 * ExplainForeignScan: EXPLAIN 출력
 */
static void
myCsvExplainForeignScan(ForeignScanState *node, ExplainState *es)
{
    ForeignScan *plan = (ForeignScan *) node->ss.ps.plan;
    char       *filename;

    if (plan->fdw_private != NIL)
    {
        filename = strVal(linitial(plan->fdw_private));
        ExplainPropertyText("Foreign File", filename, es);
    }
}

/*
 * AnalyzeForeignTable: 통계 수집 지원
 */
static bool
myCsvAnalyzeForeignTable(Relation relation,
                         AcquireSampleRowsFunc *func,
                         BlockNumber *totalpages)
{
    /* 현재 구현에서는 ANALYZE 지원 안 함 */
    return false;
}

/*
 * estimate_file_size: 파일 크기 추정
 */
static void
estimate_file_size(const char *filename, BlockNumber *pages, double *ntuples)
{
    struct stat stat_buf;

    if (stat(filename, &stat_buf) != 0)
    {
        *pages = 0;
        *ntuples = 0;
        return;
    }

    /* 페이지 수 계산 (8KB 블록 기준) */
    *pages = (stat_buf.st_size + BLCKSZ - 1) / BLCKSZ;

    /* 평균 행 크기 100바이트로 가정하여 튜플 수 추정 */
    *ntuples = stat_buf.st_size / 100.0;
}

/*
 * parse_csv_line: CSV 줄 파싱
 */
static bool
parse_csv_line(MyCsvFdwExecState *festate, TupleTableSlot *slot)
{
    char       *line;
    char       *token;
    char       *saveptr;
    int         attnum;
    Datum      *values = slot->tts_values;
    bool       *nulls = slot->tts_isnull;

    /* 다음 줄 읽기 */
    line = fgets(festate->buffer, 65536, festate->file);
    if (line == NULL)
        return false;   /* EOF */

    festate->lineno++;

    /* 개행 문자 제거 */
    line[strcspn(line, "\r\n")] = '\0';

    /* 모든 속성을 NULL로 초기화 */
    for (attnum = 0; attnum < festate->numattrs; attnum++)
        nulls[attnum] = true;

    /* CSV 파싱 */
    attnum = 0;
    for (token = strtok_r(line, ",", &saveptr);
         token != NULL && attnum < festate->numattrs;
         token = strtok_r(NULL, ",", &saveptr))
    {
        /* 빈 값은 NULL로 처리 */
        if (token[0] == '\0')
        {
            nulls[attnum] = true;
        }
        else
        {
            values[attnum] = InputFunctionCall(&festate->in_funcs[attnum],
                                               token,
                                               festate->typioparams[attnum],
                                               -1);
            nulls[attnum] = false;
        }
        attnum++;
    }

    return true;
}
```

#### Makefile

```makefile
MODULE_big = my_csv_fdw
OBJS = my_csv_fdw.o

EXTENSION = my_csv_fdw
DATA = my_csv_fdw--1.0.sql

PG_CONFIG = pg_config
PGXS := $(shell $(PG_CONFIG) --pgxs)
include $(PGXS)
```

#### 확장 SQL 파일 (my_csv_fdw--1.0.sql)

```sql
-- FDW 핸들러 함수 생성
CREATE FUNCTION my_csv_fdw_handler()
RETURNS fdw_handler
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;

-- FDW 유효성 검사 함수 생성
CREATE FUNCTION my_csv_fdw_validator(text[], oid)
RETURNS void
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;

-- 외래 데이터 래퍼 생성
CREATE FOREIGN DATA WRAPPER my_csv_fdw
    HANDLER my_csv_fdw_handler
    VALIDATOR my_csv_fdw_validator;
```

### 7.2 FDW 사용 예제

```sql
-- 확장 설치
CREATE EXTENSION my_csv_fdw;

-- 외래 서버 생성 (로컬 파일용)
CREATE SERVER csv_server FOREIGN DATA WRAPPER my_csv_fdw;

-- 사용자 매핑 생성 (파일 FDW에는 필수가 아님)
CREATE USER MAPPING FOR CURRENT_USER SERVER csv_server;

-- 외래 테이블 생성
CREATE FOREIGN TABLE employees (
    id integer,
    name text,
    department text,
    salary numeric
) SERVER csv_server
  OPTIONS (filename '/path/to/employees.csv');

-- 외래 테이블 쿼리
SELECT * FROM employees WHERE department = 'Engineering';

-- EXPLAIN으로 실행 계획 확인
EXPLAIN (ANALYZE, VERBOSE) SELECT * FROM employees;

-- 통계 수집 (AnalyzeForeignTable이 구현된 경우)
ANALYZE employees;
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Writing a Foreign Data Wrapper](https://www.postgresql.org/docs/current/fdwhandler.html)
- [PostgreSQL 공식 문서 - CREATE FOREIGN DATA WRAPPER](https://www.postgresql.org/docs/current/sql-createforeigndatawrapper.html)
- [PostgreSQL 공식 문서 - CREATE FOREIGN TABLE](https://www.postgresql.org/docs/current/sql-createforeigntable.html)
- PostgreSQL 소스 코드: `contrib/file_fdw`, `contrib/postgres_fdw`
- 헤더 파일: `src/include/foreign/fdwapi.h`, `src/include/foreign/foreign.h`
