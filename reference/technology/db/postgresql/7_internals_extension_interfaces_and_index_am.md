# PostgreSQL 내부: 확장 인터페이스와 인덱스 접근 메서드

## Chapter 59: 외래 데이터 래퍼 작성하기 (Writing a Foreign Data Wrapper)

### 목차

1. [개요](#1-개요)
2. [FDW 핸들러 함수](#2-fdw-핸들러-함수)
3. [FDW 콜백 루틴](#3-fdw-콜백-루틴)
4. [헬퍼 함수](#4-헬퍼-함수)
5. [쿼리 계획](#5-쿼리-계획)
6. [행 잠금](#6-행-잠금)
7. [예제 코드](#7-예제-코드)

---

### 1. 개요

#### 1.1 외래 데이터 래퍼란?

외래 데이터 래퍼(Foreign Data Wrapper, FDW) 는 PostgreSQL 코어 서버가 외래 테이블에 대한 모든 연산을 처리하기 위해 호출하는 함수들의 집합임. FDW는 다음과 같은 책임을 가짐:

- 원격 데이터 소스에서 데이터 가져오기 (Fetching data from remote sources)
- PostgreSQL 실행기에 데이터 반환하기 (Returning data to the executor)
- 외래 테이블에 대한 업데이트 처리하기 (Handling updates to foreign tables)

#### 1.2 아키텍처

외래 테이블에 대한 모든 연산은 해당 외래 데이터 래퍼를 통해 라우팅됨. 래퍼는 PostgreSQL이 쿼리 계획(query planning)과 실행(execution) 중에 호출하는 콜백 함수들을 구현해야 함.

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

#### 1.3 참조 구현

PostgreSQL 표준 배포판의 `contrib` 서브디렉토리에는 자신만의 래퍼를 작성할 때 좋은 참조가 되는 FDW 구현들이 포함되어 있음:

- file_fdw: 서버의 파일 시스템에 있는 데이터 파일 접근
- postgres_fdw: 다른 PostgreSQL 서버에 접근
- dblink: 다른 PostgreSQL 데이터베이스에 접근 (레거시)

---

### 2. FDW 핸들러 함수

#### 2.1 핸들러 함수 개요

외래 데이터 래퍼는 핸들러 함수(handler function) 를 통해 PostgreSQL에 등록됨. 이 함수는 `fdw_handler` 타입을 반환해야 하며, 콜백 함수 포인터들을 포함하는 `FdwRoutine` 구조체를 반환함.

#### 2.2 FdwRoutine 구조체

`FdwRoutine` 구조체는 `src/include/foreign/fdwapi.h`에 정의되어 있으며, 모든 FDW 콜백 함수 포인터를 포함함:

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

#### 2.3 핸들러 함수 예제

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

#### 2.4 FDW 등록

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

### 3. FDW 콜백 루틴

#### 3.1 스캔 콜백 (Scanning Callbacks)

외래 테이블을 스캔하기 위한 필수 콜백 함수들임.

##### GetForeignRelSize

```c
void GetForeignRelSize(PlannerInfo *root,
                       RelOptInfo *baserel,
                       Oid foreigntableid);
```

목적: 쿼리 계획 단계에서 외래 테이블의 크기 추정치를 계산함.

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

##### GetForeignPaths

```c
void GetForeignPaths(PlannerInfo *root,
                     RelOptInfo *baserel,
                     Oid foreigntableid);
```

목적: 외래 테이블 스캔을 위한 가능한 접근 경로(access paths)를 생성함.

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

##### GetForeignPlan

```c
ForeignScan *GetForeignPlan(PlannerInfo *root,
                            RelOptInfo *baserel,
                            Oid foreigntableid,
                            ForeignPath *best_path,
                            List *tlist,
                            List *scan_clauses,
                            Plan *outer_plan);
```

목적: 선택된 외래 접근 경로로부터 `ForeignScan` 계획 노드를 생성함.

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

##### BeginForeignScan

```c
void BeginForeignScan(ForeignScanState *node, int eflags);
```

목적: 실행기 시작 시 스캔을 초기화함.

주의: `(eflags & EXEC_FLAG_EXPLAIN_ONLY)`가 참이면 외부에서 관찰 가능한 부작용을 일으키는 동작을 수행해서는 안 됨 (EXPLAIN 전용 실행).

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

##### IterateForeignScan

```c
TupleTableSlot *IterateForeignScan(ForeignScanState *node);
```

목적: 외래 소스에서 한 행을 가져옴.

반환값: 더 이상 가져올 행이 없으면 빈 슬롯을 반환함.

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

##### ReScanForeignScan

```c
void ReScanForeignScan(ForeignScanState *node);
```

목적: 스캔을 처음부터 다시 시작함. 이 시점에 매개변수 값이 변경되어 있을 수 있음.

```c
static void
myReScanForeignScan(ForeignScanState *node)
{
    MyFdwExecState *festate = (MyFdwExecState *) node->fdw_state;

    /* 커서를 처음으로 되감기 */
    ResetRemoteCursor(festate);
}
```

##### EndForeignScan

```c
void EndForeignScan(ForeignScanState *node);
```

목적: 스캔을 종료하고 리소스(파일, 연결 등)를 해제함.

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

#### 3.2 조인 스캔 콜백 (Join Scanning Callbacks)

##### GetForeignJoinPaths

```c
void GetForeignJoinPaths(PlannerInfo *root,
                         RelOptInfo *joinrel,
                         RelOptInfo *outerrel,
                         RelOptInfo *innerrel,
                         JoinType jointype,
                         JoinPathExtraData *extra);
```

목적: 동일한 서버에 있는 여러 외래 테이블의 조인을 위한 접근 경로를 생성함.

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

#### 3.3 상위 레벨 계획 콜백 (Upper-Level Planning Callbacks)

##### GetForeignUpperPaths

```c
void GetForeignUpperPaths(PlannerInfo *root,
                          UpperRelationKind stage,
                          RelOptInfo *input_rel,
                          RelOptInfo *output_rel,
                          void *extra);
```

목적: 스캔/조인 후의 처리(집계, 정렬 등)를 위한 경로를 생성함.

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

#### 3.4 업데이트 콜백 (Updating Callbacks)

외래 테이블에 대한 INSERT, UPDATE, DELETE 연산을 지원하기 위한 콜백들임.

##### AddForeignUpdateTargets

```c
void AddForeignUpdateTargets(PlannerInfo *root,
                             Index rtindex,
                             RangeTblEntry *target_rte,
                             Relation target_relation);
```

목적: UPDATE/DELETE 연산에 필요한 숨겨진 "junk" 컬럼을 추가함.

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

##### PlanForeignModify

```c
List *PlanForeignModify(PlannerInfo *root,
                        ModifyTable *plan,
                        Index resultRelation,
                        int subplan_index);
```

목적: INSERT/UPDATE/DELETE 연산을 위한 FDW 개인 정보를 생성함.

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

##### BeginForeignModify

```c
void BeginForeignModify(ModifyTableState *mtstate,
                        ResultRelInfo *rinfo,
                        List *fdw_private,
                        int subplan_index,
                        int eflags);
```

목적: 실행기 시작 시 테이블 수정 연산을 초기화함.

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

##### ExecForeignInsert

```c
TupleTableSlot *ExecForeignInsert(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에 한 튜플을 삽입함.

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

##### ExecForeignBatchInsert

```c
TupleTableSlot **ExecForeignBatchInsert(EState *estate,
                                        ResultRelInfo *rinfo,
                                        TupleTableSlot **slots,
                                        TupleTableSlot **planSlots,
                                        int *numSlots);
```

목적: 여러 튜플을 일괄 삽입함.

```c
static TupleTableSlot **
myExecForeignBatchInsert(EState *estate,
                         ResultRelInfo *rinfo,
                         TupleTableSlot **slots,
                         TupleTableSlot **planSlots,
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

##### GetForeignModifyBatchSize

```c
int GetForeignModifyBatchSize(ResultRelInfo *rinfo);
```

목적: 단일 `ExecForeignBatchInsert` 호출이 처리할 수 있는 최대 튜플 수를 보고함.

```c
static int
myGetForeignModifyBatchSize(ResultRelInfo *rinfo)
{
    /* 한 번에 최대 100개 행 삽입 가능 */
    return 100;
}
```

##### ExecForeignUpdate

```c
TupleTableSlot *ExecForeignUpdate(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에서 한 튜플을 업데이트함.

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

##### ExecForeignDelete

```c
TupleTableSlot *ExecForeignDelete(EState *estate,
                                  ResultRelInfo *rinfo,
                                  TupleTableSlot *slot,
                                  TupleTableSlot *planSlot);
```

목적: 외래 테이블에서 한 튜플을 삭제함.

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

##### EndForeignModify

```c
void EndForeignModify(EState *estate, ResultRelInfo *rinfo);
```

목적: 테이블 수정을 종료하고 리소스를 해제함.

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

##### IsForeignRelUpdatable

```c
int IsForeignRelUpdatable(Relation rel);
```

목적: 외래 테이블이 지원하는 업데이트 연산을 비트마스크로 반환함.

```c
static int
myIsForeignRelUpdatable(Relation rel)
{
    /* INSERT, UPDATE, DELETE 모두 지원 */
    return (1 << CMD_INSERT) | (1 << CMD_UPDATE) | (1 << CMD_DELETE);
    /* 비트 값: INSERT=8, UPDATE=4, DELETE=16 */
}
```

#### 3.5 직접 수정 콜백 (Direct Modification Callbacks)

원격 서버에서 직접 수정을 수행하기 위한 콜백들임.

##### PlanDirectModify

```c
bool PlanDirectModify(PlannerInfo *root,
                      ModifyTable *plan,
                      Index resultRelation,
                      int subplan_index);
```

목적: 원격 서버에서 직접 수정이 안전한지 결정함.

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

##### BeginDirectModify

```c
void BeginDirectModify(ForeignScanState *node, int eflags);
```

목적: 직접 수정 실행을 준비함.

##### IterateDirectModify

```c
TupleTableSlot *IterateDirectModify(ForeignScanState *node);
```

목적: RETURNING 절을 위한 결과 데이터를 가져옴.

##### EndDirectModify

```c
void EndDirectModify(ForeignScanState *node);
```

목적: 직접 수정 후 정리함.

#### 3.6 TRUNCATE 콜백

##### ExecForeignTruncate

```c
void ExecForeignTruncate(List *rels,
                         DropBehavior behavior,
                         bool restart_seqs);
```

목적: 외래 테이블을 TRUNCATE함.

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

#### 3.7 EXPLAIN 콜백

##### ExplainForeignScan

```c
void ExplainForeignScan(ForeignScanState *node, ExplainState *es);
```

목적: 외래 테이블 스캔에 대한 추가 EXPLAIN 출력을 인쇄함.

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

##### ExplainForeignModify

```c
void ExplainForeignModify(ModifyTableState *mtstate,
                          ResultRelInfo *rinfo,
                          List *fdw_private,
                          int subplan_index,
                          struct ExplainState *es);
```

목적: 테이블 업데이트에 대한 추가 EXPLAIN 출력을 인쇄함.

#### 3.8 ANALYZE 콜백

##### AnalyzeForeignTable

```c
bool AnalyzeForeignTable(Relation relation,
                         AcquireSampleRowsFunc *func,
                         BlockNumber *totalpages);
```

목적: 외래 테이블에 대한 통계를 수집함.

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

#### 3.9 IMPORT FOREIGN SCHEMA 콜백

##### ImportForeignSchema

```c
List *ImportForeignSchema(ImportForeignSchemaStmt *stmt, Oid serverOid);
```

목적: 원격 스키마에서 테이블 정의를 가져옴.

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

#### 3.10 병렬 실행 콜백 (Parallel Execution Callbacks)

##### IsForeignScanParallelSafe

```c
bool IsForeignScanParallelSafe(PlannerInfo *root,
                               RelOptInfo *rel,
                               RangeTblEntry *rte);
```

목적: 스캔을 병렬 워커에서 수행해도 안전한지 확인함.

##### EstimateDSMForeignScan

```c
Size EstimateDSMForeignScan(ForeignScanState *node, ParallelContext *pcxt);
```

목적: 필요한 동적 공유 메모리 크기(바이트)를 추정함.

##### InitializeDSMForeignScan

```c
void InitializeDSMForeignScan(ForeignScanState *node,
                              ParallelContext *pcxt,
                              void *coordinate);
```

목적: 병렬 연산에 사용할 공유 메모리를 초기화함.

##### InitializeWorkerForeignScan

```c
void InitializeWorkerForeignScan(ForeignScanState *node,
                                 shm_toc *toc,
                                 void *coordinate);
```

목적: 리더의 공유 상태를 기반으로 병렬 워커의 로컬 상태를 초기화함.

##### ShutdownForeignScan

```c
void ShutdownForeignScan(ForeignScanState *node);
```

목적: DSM 세그먼트가 해제되기 전에 리소스를 정리함.

#### 3.11 비동기 실행 콜백 (Asynchronous Execution Callbacks)

##### IsForeignPathAsyncCapable

```c
bool IsForeignPathAsyncCapable(ForeignPath *path);
```

목적: 해당 경로가 외래 릴레이션을 비동기적으로 스캔할 수 있는지 확인함.

##### ForeignAsyncRequest

```c
void ForeignAsyncRequest(AsyncRequest *areq);
```

목적: 비동기적으로 한 튜플을 생성함.

##### ForeignAsyncConfigureWait

```c
void ForeignAsyncConfigureWait(AsyncRequest *areq);
```

목적: 대기할 파일 디스크립터 이벤트를 구성함.

##### ForeignAsyncNotify

```c
void ForeignAsyncNotify(AsyncRequest *areq);
```

목적: 관련 이벤트를 처리하고, 비동기적으로 튜플 하나를 생성함.

---

### 4. 헬퍼 함수

PostgreSQL 코어 서버가 FDW 개발을 위해 제공하는 유틸리티 함수들임.

#### 4.1 ForeignDataWrapper 함수

```c
/* OID로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapper(Oid fdwid);

/* 확장 옵션으로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapperExtended(Oid fdwid, bits16 flags);
/* flags: FDW_MISSING_OK - 없으면 에러 대신 NULL 반환 */

/* 이름으로 FDW 객체 가져오기 */
ForeignDataWrapper *GetForeignDataWrapperByName(const char *name, bool missing_ok);
```

#### 4.2 ForeignServer 함수

```c
/* OID로 서버 객체 가져오기 */
ForeignServer *GetForeignServer(Oid serverid);

/* 확장 옵션으로 서버 객체 가져오기 */
ForeignServer *GetForeignServerExtended(Oid serverid, bits16 flags);
/* flags: FSV_MISSING_OK - 없으면 에러 대신 NULL 반환 */

/* 이름으로 서버 객체 가져오기 */
ForeignServer *GetForeignServerByName(const char *name, bool missing_ok);
```

#### 4.3 사용자 매핑 및 테이블 함수

```c
/* 사용자 매핑 가져오기 */
UserMapping *GetUserMapping(Oid userid, Oid serverid);
/* 특정 사용자 매핑이 없으면 PUBLIC 매핑으로 대체 */

/* 외래 테이블 정보 가져오기 */
ForeignTable *GetForeignTable(Oid relid);
```

#### 4.4 컬럼 옵션 함수

```c
/* 컬럼별 FDW 옵션 가져오기 */
List *GetForeignColumnOptions(Oid relid, AttrNumber attnum);
/* DefElem 리스트로 반환, 옵션이 없으면 NIL */
```

#### 4.5 사용 예제

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

### 5. 쿼리 계획

#### 5.1 계획 참여 콜백

다음 FDW 콜백 함수들이 PostgreSQL의 쿼리 계획에 참여함:

- `GetForeignRelSize`
- `GetForeignPaths`
- `GetForeignPlan`
- `PlanForeignModify`
- `GetForeignJoinPaths`
- `GetForeignUpperPaths`
- `PlanDirectModify`

#### 5.2 주요 계획 정보

##### baserestrictinfo와 비용 절감

`root`와 `baserel` 매개변수를 통해 FDW가 데이터 가져오기를 줄일 수 있음:

`baserel->baserestrictinfo`: 행을 필터링해야 하는 제한 조건(WHERE 절)을 포함함. FDW는 이를 적용하거나 코어 실행기에 맡길 수 있음.

`baserel->reltarget->exprs`: ForeignScan 노드가 가져와야 하는 컬럼을 결정함 (조건 평가에만 사용되는 컬럼은 제외).

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

##### 개인 필드 저장

`baserel->fdw_private`: FDW가 외래 테이블에 대한 정보를 저장하기 위한 void 포인터임. 계획자가 NULL로 초기화하고 이후 건드리지 않음. 계획 단계 간에 정보를 전달하면서 재계산을 피하는 데 유용함.

`ForeignPath->fdw_private`: 접근 경로 의미를 저장하기 위한 List 포인터임. 디버깅을 위해 `nodeToString`으로 덤프 가능한 표현을 사용하는 것이 좋음.

#### 5.3 GetForeignPlan 구현

`GetForeignPlan`에서 FDW는 다음을 할 수 있음:

1. 타겟 리스트 복사: 그대로 계획 노드에 복사
2. scan_clauses 처리: 실제 조건을 추출하여 qual 리스트에 배치
3. 내부 조건 제거: FDW가 원격에서 처리하면 계획 노드의 qual 리스트에서 제거
4. fdw_exprs에 하위 표현식 추가: 실행 시점 평가를 보장

#### 5.4 조건 재검사 요구사항

qual 리스트에서 제거된 조건은 다음 중 하나여야 함:
- `fdw_recheck_quals`에 추가되거나
- `RecheckForeignScan`에서 재검사

이는 `READ COMMITTED` 격리 수준에서 동시 업데이트가 발생했을 때 올바른 동작을 보장하기 위함임. NULL이 발생할 수 있는 필드를 포함한 외부 조인을 푸시다운하는 경우 `RecheckForeignScan` 구현이 필요할 수 있음.

#### 5.5 ForeignScan 출력 설명

`fdw_scan_tlist`: FDW가 반환하는 튜플을 설명함:

- NIL: 반환된 튜플이 외래 테이블의 선언된 행 타입과 일치
- Non-NIL: 반환되는 컬럼을 나타내는 Var/표현식으로 구성된 TargetEntry 리스트

사용 사례:
- 쿼리에 필요 없는 생략된 컬럼 문서화
- FDW가 로컬 실행보다 저렴하게 계산하는 표현식 포함
- `GetForeignJoinPaths`로 생성된 조인 계획에서 필수

#### 5.6 조인을 위한 매개변수화된 경로

FDW는 다음을 구성해야 함:

1. 최소 하나의 경로: 테이블 제한 조건에만 의존
2. 매개변수화된 경로: 조인용 (예: `foreign_variable = local_variable`)
   - 릴레이션의 조인 리스트에서 조인 조건을 검색 (`baserestrictinfo`에는 없음)
   - `get_baserel_parampathinfo`를 사용해 `param_info` 설정
   - `fdw_exprs`에 local_variable 부분을 추가

#### 5.7 원격 조인 지원

`GetForeignJoinPaths`는 `GetForeignPaths`와 유사하게 원격 조인을 위한 `ForeignPath`를 생성함:

- 조인 조건이 `extra->restrictlist`로 전달됨 (`baserestrictinfo`가 아님)
- 경로의 `fdw_private`을 통해 `GetForeignPlan`에 정보 전달

#### 5.8 상위 레벨 연산

##### GetForeignUpperPaths

스캔/조인 위의 연산 (그룹화, 집계)용:

1. 원격 실행을 위한 경로 생성
2. 적절한 상위 릴레이션에 삽입 (예: `UPPERREL_GROUP_AGG`)
3. `add_path`를 사용하여 비용 기반으로 로컬 처리와 경쟁
4. 승리한 경로를 `GetForeignPlan`을 통해 계획으로 변환

#### 5.9 UPDATE/DELETE 계획

`PlanForeignModify`와 `PlanDirectModify`는 다음에 접근할 수 있음:
- 외래 테이블의 RelOptInfo 구조체
- 스캔 계획 중 생성된 baserel->fdw_private 데이터

반환되는 `List`는 `copyObject`가 처리할 수 있는 구조체만 포함해야 함.

---

### 6. 행 잠금

#### 6.1 개요

FDW는 행 수준 잠금을 구현해 동시 업데이트를 방지하고, PostgreSQL 표준 테이블 잠금의 의미론을 근사적으로 재현할 수 있음.

#### 6.2 초기 잠금 vs 지연 잠금

##### 초기 잠금 (Early Locking)

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

##### 지연 잠금 (Late Locking)

- 필요할 때만 행을 잠금
- 구현이 더 복잡하며, 행을 고유하게 재식별하는 기능이 필요
- PostgreSQL TID처럼 특정 행 버전을 가리키는 행 식별자가 필요
- 섹션 58.2.6의 API 함수가 지연 잠금을 지원

#### 6.3 구현 접근법

##### UPDATE/DELETE 연산용

`ForeignScan` 연산은 대상 테이블 행에 대해 초기 잠금을 수행해야 함:

```
ForeignScan → SELECT FOR UPDATE (동등)
```

감지 방법:
- 계획 시점: relid를 `root->parse->resultRelation`과 비교
- 실행 시점: `ExecRelationIsTargetRelation()` 사용

대안: `ExecForeignUpdate` 또는 `ExecForeignDelete` 콜백 내에서 지연 잠금 수행

##### SELECT FOR UPDATE/SHARE 명령용

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

#### 6.4 잠금되지 않은 외래 테이블용

UPDATE/DELETE/SELECT FOR UPDATE/SHARE에서 잠금되지 않은 외래 테이블의 경우, `GetForeignRowMarkType`이 다음을 선택하여 기본 전체 행 복사를 재정의:

- `ROW_MARK_REFERENCE` (잠금 강도 = `LCS_NONE`일 때): 새 잠금 획득 없이 재가져오기 위해 `RefetchForeignRow` 호출
- `ROW_MARK_COPY`: 전체 행 복사 (기본 동작)

#### 6.5 READ COMMITTED 격리 고려사항

`READ COMMITTED` 모드에서 PostgreSQL은 업데이트된 튜플에 대해 조건을 재검사해야 할 수 있음. FDW는 다음 방법 중 하나를 선택할 수 있음:

1. 효율적인 재가져오기를 위해 프로젝션된 컬럼에 TID를 포함 (저비용 재가져오기 기능 필요)
2. 전체 행 복사를 기본으로 사용 — 특별한 요구사항은 없지만 병합/해시 조인 성능이 저하될 수 있음

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

### 7. 예제 코드

#### 7.1 완전한 FDW 구현 예제

다음은 간단한 CSV 파일 FDW의 완전한 구현 예제임.

##### 헤더 파일 (my_csv_fdw.h)

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

##### 소스 파일 (my_csv_fdw.c)

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

##### Makefile

```makefile
MODULE_big = my_csv_fdw
OBJS = my_csv_fdw.o

EXTENSION = my_csv_fdw
DATA = my_csv_fdw--1.0.sql

PG_CONFIG = pg_config
PGXS := $(shell $(PG_CONFIG) --pgxs)
include $(PGXS)
```

##### 확장 SQL 파일 (my_csv_fdw--1.0.sql)

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

#### 7.2 FDW 사용 예제

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

### 참고 자료

- [PostgreSQL 공식 문서 - Writing a Foreign Data Wrapper](https://www.postgresql.org/docs/current/fdwhandler.html)
- [PostgreSQL 공식 문서 - CREATE FOREIGN DATA WRAPPER](https://www.postgresql.org/docs/current/sql-createforeigndatawrapper.html)
- [PostgreSQL 공식 문서 - CREATE FOREIGN TABLE](https://www.postgresql.org/docs/current/sql-createforeigntable.html)
- PostgreSQL 소스 코드: `contrib/file_fdw`, `contrib/postgres_fdw`
- 헤더 파일: `src/include/foreign/fdwapi.h`, `src/include/foreign/foreign.h`

---

## 제60장. 테이블 샘플링 메서드 작성 (Writing a Table Sampling Method)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/tablesample-method.html](https://www.postgresql.org/docs/current/tablesample-method.html)

---

### 목차

- [개요](#개요)
- [60.1. 샘플링 메서드 지원 함수](#601-샘플링-메서드-지원-함수)
  - [60.1.1. SampleScanGetSampleSize](#6011-samplescangetsamplesize)
  - [60.1.2. InitSampleScan](#6012-initsamplescan)
  - [60.1.3. BeginSampleScan](#6013-beginsamplescan)
  - [60.1.4. NextSampleBlock](#6014-nextsampleblock)
  - [60.1.5. NextSampleTuple](#6015-nextsampletuple)
  - [60.1.6. EndSampleScan](#6016-endsamplescan)

---

### 개요

PostgreSQL의 `TABLESAMPLE` 절은 기본 제공되는 `BERNOULLI`와 `SYSTEM` 메서드 외에도 사용자 정의 테이블 샘플링 메서드를 지원함. 샘플링 메서드는 `TABLESAMPLE` 절 사용 시 어떤 행이 선택될지를 결정함.

#### SQL 함수 시그니처

SQL 수준에서 테이블 샘플링 메서드는 다음과 같은 시그니처를 가진 단일 SQL 함수로 표현됨:

```sql
method_name(internal) RETURNS tsm_handler
```

이 함수의 특징은 다음과 같음:

- 함수명: `TABLESAMPLE` 절에서 사용할 메서드 이름과 동일해야 함
- `internal` 인자: SQL에서 직접 호출을 방지하기 위한 더미 인자
- 반환 타입: `palloc`된 `TsmRoutine` 타입 구조체에 대한 포인터

#### TsmRoutine 구조체

반환되는 `TsmRoutine` 구조체는 샘플링 메서드에 필요한 지원 함수들에 대한 포인터를 포함해야 함. 이 지원 함수들은 일반 C 함수로서, SQL에서 직접 호출할 수 없음.

##### 구조체 필드

```c
typedef struct TsmRoutine
{
    NodeTag     type;

    /* 파라미터 타입의 OID 리스트 */
    List       *parameterTypes;

    /* 쿼리 간 반복 가능 여부 */
    bool        repeatable_across_queries;

    /* 스캔 간 반복 가능 여부 */
    bool        repeatable_across_scans;

    /* 지원 함수 포인터들 */
    SampleScanGetSampleSize_function SampleScanGetSampleSize;
    InitSampleScan_function InitSampleScan;            /* 선택 사항, NULL 가능 */
    BeginSampleScan_function BeginSampleScan;
    NextSampleBlock_function NextSampleBlock;          /* 선택 사항, NULL 가능 */
    NextSampleTuple_function NextSampleTuple;
    EndSampleScan_function EndSampleScan;              /* 선택 사항, NULL 가능 */
} TsmRoutine;
```

##### 주요 필드 설명

- `parameterTypes`
  - 타입: `List *`
  - 설명: `TABLESAMPLE` 절에서 받는 파라미터 데이터 타입의 OID 리스트. 내장 메서드들은 샘플링 백분율을 위해 `FLOAT4OID` 사용
- `repeatable_across_queries`
  - 타입: `bool`
  - 설명: `true`인 경우: 동일한 파라미터와 `REPEATABLE` 시드를 사용하면 쿼리마다 동일한 샘플 반환. `false`인 경우: `REPEATABLE` 절을 허용하지 않음
- `repeatable_across_scans`
  - 타입: `bool`
  - 설명: `true`인 경우: 동일 쿼리 내 연속 스캔에서 동일한 샘플 반환. `false`인 경우: 플래너가 다중 스캔을 방지하여 불일치 출력 회피

#### 참조 파일

- `src/include/access/tsmapi.h`: `TsmRoutine` 타입 정의
- `src/backend/access/tablesample/`: 내장 샘플링 메서드 구현 예제
- `contrib/`: 확장 샘플링 메서드 예제 (예: `tsm_system_rows`, `tsm_system_time`)

---

### 60.1. 샘플링 메서드 지원 함수

TSM(Table Sampling Method) 핸들러 함수는 지원 함수에 대한 포인터를 포함하는 `TsmRoutine` 구조체를 반환함. 대부분의 함수는 필수이지만, 일부는 선택 사항으로 `NULL`로 설정할 수 있음.

#### 지원 함수 요약

- `SampleScanGetSampleSize`
  - 필수 여부: 필수
  - 용도: 계획(Planning) 단계에서 스캔 크기 추정
- `InitSampleScan`
  - 필수 여부: 선택
  - 용도: 실행기(Executor) 시작 시 초기화
- `BeginSampleScan`
  - 필수 여부: 필수
  - 용도: 샘플링 스캔 실행 시작
- `NextSampleBlock`
  - 필수 여부: 선택
  - 용도: 다음 스캔 페이지 반환
- `NextSampleTuple`
  - 필수 여부: 필수
  - 용도: 다음 샘플 튜플 반환
- `EndSampleScan`
  - 필수 여부: 선택
  - 용도: 스캔 종료 및 리소스 해제

---

#### 60.1.1. SampleScanGetSampleSize

```c
void
SampleScanGetSampleSize(PlannerInfo *root,
                        RelOptInfo *baserel,
                        List *paramexprs,
                        BlockNumber *pages,
                        double *tuples);
```

##### 목적

계획(Planning) 단계에서 호출되어 스캔 크기를 추정함.

##### 설명

- 샘플 스캔 중 읽을 릴레이션 페이지 수를 추정
- 선택될 튜플 수를 추정
- 일반적으로 `baserel->pages`와 `baserel->tuples`에 샘플링 비율을 곱하여 계산
- 결과는 반드시 정수값으로 반올림해야 함

##### 파라미터

- `root`: 플래너 정보 구조체
- `baserel`: 샘플링 대상 릴레이션의 최적화 정보
- `paramexprs`: `TABLESAMPLE` 절 파라미터를 나타내는 표현식 리스트
- `pages`: 출력 파라미터: 읽을 페이지 수
- `tuples`: 출력 파라미터: 선택할 튜플 수

##### 구현 시 주의사항

- `estimate_expression_value()` 함수를 사용하여 표현식을 상수로 변환 시도
- 값을 상수로 변환할 수 없거나 유효하지 않은 값이라도 반드시 추정치를 제공해야 함
- 합리적인 기본값 사용 권장

##### 예제 코드

```c
static void
my_SampleScanGetSampleSize(PlannerInfo *root,
                           RelOptInfo *baserel,
                           List *paramexprs,
                           BlockNumber *pages,
                           double *tuples)
{
    Node       *pctnode;
    float4      samplefract;

    /* 첫 번째 파라미터(샘플링 백분율)를 상수로 변환 시도 */
    pctnode = (Node *) linitial(paramexprs);
    pctnode = estimate_expression_value(root, pctnode);

    if (IsA(pctnode, Const) &&
        !((Const *) pctnode)->constisnull)
    {
        samplefract = DatumGetFloat4(((Const *) pctnode)->constvalue);
        /* 백분율을 0과 100 사이로 제한 */
        samplefract = Max(0, Min(100, samplefract));
        samplefract /= 100.0f;
    }
    else
    {
        /* 상수로 변환 불가 시 기본값 사용 */
        samplefract = 0.1f;  /* 10% 기본값 */
    }

    /* 페이지 및 튜플 수 추정 */
    *pages = (BlockNumber) ceil(baserel->pages * samplefract);
    *tuples = clamp_row_est(baserel->tuples * samplefract);
}
```

---

#### 60.1.2. InitSampleScan

```c
void
InitSampleScan(SampleScanState *node,
               int eflags);
```

##### 목적

실행기(Executor) 시작 시 샘플 스캔에 필요한 초기화를 수행함.

##### 설명

- 처리 시작 전에 필요한 초기화 작업 수행
- `SampleScanState` 노드는 이미 생성되어 있으나 `tsm_state` 필드는 `NULL`
- 내부 상태 데이터를 `palloc`하여 `node->tsm_state`에 저장 가능
- 테이블 정보는 `SampleScanState`의 다른 필드를 통해 접근 가능
- `node->ss.ss_currentScanDesc` 스캔 디스크립터는 아직 설정되지 않음

##### 파라미터

- `node`: 샘플 스캔 상태 노드
- `eflags`: 실행기 동작 모드 플래그

##### `eflags` 처리

```c
if (eflags & EXEC_FLAG_EXPLAIN_ONLY)
{
    /* EXPLAIN만 수행할 경우, 최소한의 작업만 수행 */
    return;
}
```

##### 선택 사항

이 함수는 선택 사항임. `NULL`로 설정 시 `BeginSampleScan`이 모든 초기화를 수행해야 함.

---

#### 60.1.3. BeginSampleScan

```c
void
BeginSampleScan(SampleScanState *node,
                Datum *params,
                int nparams,
                uint32 seed);
```

##### 목적

샘플링 스캔 실행을 시작함.

##### 설명

- 첫 번째 튜플 가져오기 직전에 호출
- 스캔 재시작 시에도 호출
- 테이블 정보는 `SampleScanState` 필드를 통해 접근 가능

##### 파라미터

- `node`: 샘플 스캔 상태 노드
- `params`: `TABLESAMPLE` 절 파라미터 값 배열
- `nparams`: 파라미터 개수
- `seed`: 난수 시드 (`REPEATABLE` 값의 해시 또는 `random()` 결과)

##### 설정 가능한 동작

- `node->use_bulkread`
  - 기본값: `true`
  - 설명: 버퍼 재활용을 권장. 작은 테이블 비율을 읽을 때는 `false`로 설정
- `node->use_pagemode`
  - 기본값: `true`
  - 설명: 페이지당 단일 패스로 가시성 검사 수행. 작은 튜플 비율만 선택 시 `false`로 설정

##### 반복 가능성 요구사항

`repeatable_across_scans`가 `true`로 설정된 경우:

- 재스캔 시 동일한 튜플 집합을 선택해야 함
- `TABLESAMPLE` 파라미터와 시드가 변경되지 않으면 새 `BeginSampleScan` 호출도 동일한 튜플 선택

##### 예제 코드

```c
static void
my_BeginSampleScan(SampleScanState *node,
                   Datum *params,
                   int nparams,
                   uint32 seed)
{
    MySampleState *state;
    float4         percent = DatumGetFloat4(params[0]);

    /* 상태 구조체 할당 또는 재사용 */
    if (node->tsm_state == NULL)
    {
        state = (MySampleState *) palloc0(sizeof(MySampleState));
        node->tsm_state = (void *) state;
    }
    else
    {
        state = (MySampleState *) node->tsm_state;
    }

    /* 샘플링 상태 초기화 */
    state->percent = percent / 100.0f;
    state->seed = seed;
    state->current_block = 0;

    /* 난수 생성기 초기화 */
    sampler_random_init_state(seed, state->randstate);

    /* 작은 샘플은 벌크리드 비활성화 */
    if (state->percent < 0.01)
        node->use_bulkread = false;
}
```

---

#### 60.1.4. NextSampleBlock

```c
BlockNumber
NextSampleBlock(SampleScanState *node,
                BlockNumber nblocks);
```

##### 목적

스캔할 다음 페이지(블록)를 반환함.

##### 설명

- 스캔할 다음 블록 번호를 반환
- 남은 페이지가 없으면 `InvalidBlockNumber` 반환

##### 파라미터

- `node`: 샘플 스캔 상태 노드
- `nblocks`: 릴레이션의 총 블록 수

##### 선택 사항

이 함수는 선택 사항임. `NULL`로 설정 시:

- 코어 코드가 전체 릴레이션 순차 스캔 수행
- 동기화된 스캔(Synchronized Scanning) 사용
- 스캔 순서가 보장되지 않음

##### 예제 코드

```c
static BlockNumber
my_NextSampleBlock(SampleScanState *node, BlockNumber nblocks)
{
    MySampleState *state = (MySampleState *) node->tsm_state;
    BlockNumber    block;

    /* 모든 블록을 검사했는지 확인 */
    while (state->current_block < nblocks)
    {
        block = state->current_block++;

        /* 난수 기반으로 블록 선택 여부 결정 */
        if (sampler_random_fract(state->randstate) < state->percent)
            return block;
    }

    /* 더 이상 스캔할 블록 없음 */
    return InvalidBlockNumber;
}
```

---

#### 60.1.5. NextSampleTuple

```c
OffsetNumber
NextSampleTuple(SampleScanState *node,
                BlockNumber blockno,
                OffsetNumber maxoffset);
```

##### 목적

페이지에서 샘플링할 다음 튜플을 반환함.

##### 설명

- 지정된 페이지에서 샘플링할 다음 튜플의 오프셋 번호 반환
- 남은 튜플이 없으면 `InvalidOffsetNumber` 반환
- `maxoffset`은 페이지에서 사용 중인 가장 큰 오프셋 번호

##### 파라미터

- `node`: 샘플 스캔 상태 노드
- `blockno`: 현재 스캔 중인 블록 번호
- `maxoffset`: 페이지의 최대 오프셋 번호

##### 중요 참고사항

1. 유효한 튜플 정보 미제공: 1부터 `maxoffset` 사이에서 유효한 튜플을 포함하는 오프셋이 어떤 것인지 명시적으로 알 수 없음
2. 누락/비가시 튜플 처리: 코어 코드가 누락되거나 비가시적인 튜플 요청을 자동으로 무시 (편향 없음)
3. 튜플 유효성 확인: 필요 시 `node->donetuples`로 반환된 튜플의 유효성 검사 가능
4. 블록 번호 가정 금지: `blockno`가 가장 최근 `NextSampleBlock` 호출의 결과라고 가정하면 안 됨 (프리페칭이 발생할 수 있음)
5. 페이지 내 연속성 보장: 특정 페이지 샘플링이 시작된 후 `InvalidOffsetNumber`를 반환하기 전까지 연속된 호출은 동일 페이지를 참조함

##### 예제 코드

```c
static OffsetNumber
my_NextSampleTuple(SampleScanState *node,
                   BlockNumber blockno,
                   OffsetNumber maxoffset)
{
    MySampleState *state = (MySampleState *) node->tsm_state;
    OffsetNumber   offset;

    /* 이전 스캔 상태 확인 및 초기화 */
    if (state->current_blockno != blockno)
    {
        state->current_blockno = blockno;
        state->current_offset = FirstOffsetNumber;
    }

    /* 남은 튜플 검사 */
    while (state->current_offset <= maxoffset)
    {
        offset = state->current_offset++;

        /* 난수 기반으로 튜플 선택 여부 결정 */
        if (sampler_random_fract(state->randstate) < state->percent)
            return offset;
    }

    /* 더 이상 샘플링할 튜플 없음 */
    return InvalidOffsetNumber;
}
```

---

#### 60.1.6. EndSampleScan

```c
void
EndSampleScan(SampleScanState *node);
```

##### 목적

스캔을 종료하고 리소스를 해제함.

##### 설명

- 외부에서 볼 수 있는 리소스 정리
- `palloc`된 메모리 정리는 일반적으로 중요하지 않음 (메모리 컨텍스트에서 자동 정리)
- 파일 핸들, 외부 연결 등의 리소스 해제에 사용

##### 선택 사항

이 함수는 선택 사항임. 외부 리소스가 없다면 `NULL`로 설정 가능함.

##### 예제 코드

```c
static void
my_EndSampleScan(SampleScanState *node)
{
    MySampleState *state = (MySampleState *) node->tsm_state;

    if (state != NULL)
    {
        /* 외부 리소스 정리 (예시) */
        if (state->external_file != NULL)
            fclose(state->external_file);

        /* 선택적: 상태 초기화 */
        node->tsm_state = NULL;
    }
}
```

---

### 완전한 예제: 사용자 정의 샘플링 메서드

다음은 사용자 정의 테이블 샘플링 메서드의 완전한 구현 예제임.

#### SQL 정의

```sql
-- 샘플링 메서드 핸들러 함수 생성
CREATE FUNCTION my_sample_method(internal)
RETURNS tsm_handler
AS 'my_sample_extension', 'my_sample_method_handler'
LANGUAGE C STRICT;

-- 사용 예제
SELECT * FROM my_table TABLESAMPLE my_sample_method(10.0) REPEATABLE (42);
```

#### C 구현

```c
#include "postgres.h"
#include "access/tsmapi.h"
#include "access/relscan.h"
#include "utils/sampling.h"

PG_MODULE_MAGIC;

/* 내부 상태 구조체 */
typedef struct MySampleState
{
    float4          percent;
    uint32          seed;
    SamplerRandomState randstate;
    BlockNumber     current_block;
    BlockNumber     current_blockno;
    OffsetNumber    current_offset;
} MySampleState;

/* 함수 선언 */
PG_FUNCTION_INFO_V1(my_sample_method_handler);

static void my_SampleScanGetSampleSize(PlannerInfo *root,
                                       RelOptInfo *baserel,
                                       List *paramexprs,
                                       BlockNumber *pages,
                                       double *tuples);
static void my_InitSampleScan(SampleScanState *node, int eflags);
static void my_BeginSampleScan(SampleScanState *node,
                               Datum *params,
                               int nparams,
                               uint32 seed);
static BlockNumber my_NextSampleBlock(SampleScanState *node,
                                      BlockNumber nblocks);
static OffsetNumber my_NextSampleTuple(SampleScanState *node,
                                       BlockNumber blockno,
                                       OffsetNumber maxoffset);
static void my_EndSampleScan(SampleScanState *node);

/* 핸들러 함수 */
Datum
my_sample_method_handler(PG_FUNCTION_ARGS)
{
    TsmRoutine *tsm = makeNode(TsmRoutine);

    /* 파라미터 타입: FLOAT4 (샘플링 백분율) */
    tsm->parameterTypes = list_make1_oid(FLOAT4OID);

    /* 반복 가능성 설정 */
    tsm->repeatable_across_queries = true;
    tsm->repeatable_across_scans = true;

    /* 지원 함수 등록 */
    tsm->SampleScanGetSampleSize = my_SampleScanGetSampleSize;
    tsm->InitSampleScan = my_InitSampleScan;
    tsm->BeginSampleScan = my_BeginSampleScan;
    tsm->NextSampleBlock = my_NextSampleBlock;
    tsm->NextSampleTuple = my_NextSampleTuple;
    tsm->EndSampleScan = my_EndSampleScan;

    PG_RETURN_POINTER(tsm);
}
```

---

### 관련 참고 자료

#### 내장 샘플링 메서드

- `BERNOULLI`
  - 설명: 각 튜플을 독립적으로 샘플링
  - 소스 파일: `src/backend/access/tablesample/bernoulli.c`
- `SYSTEM`
  - 설명: 블록 단위로 샘플링
  - 소스 파일: `src/backend/access/tablesample/system.c`

#### 확장 샘플링 메서드 (contrib)

- `tsm_system_rows`: 지정된 행 수만큼 샘플링
- `tsm_system_time`: 지정된 시간 동안 샘플링

#### 주요 헤더 파일

- `src/include/access/tsmapi.h` - TSM API 정의
- `src/include/access/relscan.h` - 릴레이션 스캔 구조체
- `src/include/utils/sampling.h` - 샘플링 유틸리티 함수

---

### 요약

테이블 샘플링 메서드를 작성하려면:

1. TSM 핸들러 함수 작성: `TsmRoutine` 구조체 반환
2. 필수 지원 함수 구현:
   - `SampleScanGetSampleSize`: 계획 단계 크기 추정
   - `BeginSampleScan`: 스캔 시작/재시작 처리
   - `NextSampleTuple`: 페이지 내 튜플 선택
3. 선택적 지원 함수 구현:
   - `InitSampleScan`: 초기화 (NULL 가능)
   - `NextSampleBlock`: 블록 선택 (NULL 시 순차 스캔)
   - `EndSampleScan`: 리소스 정리 (NULL 가능)
4. 반복 가능성 결정: `repeatable_across_queries`, `repeatable_across_scans` 설정
5. SQL 함수 등록: `RETURNS tsm_handler` 함수 생성

---

## Chapter 61: 커스텀 스캔 프로바이더 작성하기 (Writing a Custom Scan Provider)

### 개요 (Overview)

PostgreSQL은 확장 모듈(Extension Module)이 시스템에 새로운 스캔 타입을 추가할 수 있도록 하는 실험적 기능을 제공함. 외부 데이터 래퍼(Foreign Data Wrapper)가 자체 외부 테이블만 처리하는 것과 달리, 커스텀 스캔 프로바이더(Custom Scan Provider)는 시스템의 모든 릴레이션에 대해 대안적인 스캔 방법을 제공할 수 있음.

#### 외부 데이터 래퍼와의 차이점

- 적용 범위
  - 외부 데이터 래퍼 (FDW): 자체 외부 테이블만 스캔
  - 커스텀 스캔 프로바이더: 시스템의 모든 릴레이션에 대해 스캔 가능
- 유연성
  - 외부 데이터 래퍼 (FDW): 제한적
  - 커스텀 스캔 프로바이더: 높음

#### 주요 사용 사례 (Use Cases)

커스텀 스캔 프로바이더는 코어 시스템에서 지원하지 않는 최적화를 구현해야 할 때 주로 사용됨:

- 캐싱 전략 (Caching Strategies): 사용자 정의 캐싱 메커니즘 구현
- 하드웨어 가속 (Hardware Acceleration): GPU나 특수 하드웨어를 활용한 스캔
- 커스텀 인덱싱 방법 (Custom Indexing Methods): 새로운 인덱스 구조 활용

---

### 구현 프로세스 (Implementation Process)

커스텀 스캔 프로바이더 작성은 3단계 프로세스로 이루어짐:

```
1. 계획 단계 (Planning Phase)
   └── 제안된 전략을 사용하는 스캔을 나타내는 접근 경로(Access Path) 생성

2. 계획 선택 (Plan Selection)
   └── 플래너가 해당 접근 경로를 최적으로 선택하면 이를 계획(Plan)으로 변환

3. 실행 단계 (Execution Phase)
   └── 계획을 실행하고 동일 릴레이션에 대한 다른 접근 경로와 일치하는 결과 생성
```

---

### 61.1 커스텀 스캔 경로 (Custom Scan Paths)

커스텀 스캔 프로바이더는 코어 시스템의 접근 경로 생성이 완료된 후 호출되는 훅(Hook)을 통해 베이스 릴레이션(Base Relation)에 대한 경로를 추가함.

#### 베이스 릴레이션 훅 (Base Relation Hook)

```c
typedef void (*set_rel_pathlist_hook_type) (PlannerInfo *root,
                                            RelOptInfo *rel,
                                            Index rti,
                                            RangeTblEntry *rte);
extern PGDLLIMPORT set_rel_pathlist_hook_type set_rel_pathlist_hook;
```

이 훅이 호출되면, 커스텀 스캔 프로바이더는 일반적으로 `CustomPath` 객체를 생성하고 `add_path()` 또는 `add_partial_path()` 함수를 사용하여 추가함.

#### 조인 릴레이션 훅 (Join Relation Hook)

```c
typedef void (*set_join_pathlist_hook_type) (PlannerInfo *root,
                                             RelOptInfo *joinrel,
                                             RelOptInfo *outerrel,
                                             RelOptInfo *innerrel,
                                             JoinType jointype,
                                             JoinPathExtraData *extra);
extern PGDLLIMPORT set_join_pathlist_hook_type set_join_pathlist_hook;
```

> 중요: `CustomPath`는 사용할 조인 절(Join Clauses) 집합을 포함해야 함.

#### CustomPath 데이터 구조

```c
typedef struct CustomPath
{
    Path      path;
    uint32    flags;
    List     *custom_paths;
    List     *custom_restrictinfo;
    List     *custom_private;
    const CustomPathMethods *methods;
} CustomPath;
```

##### 필드 설명

- `path`: 표준 경로 초기화 (행 수, 비용 추정, 정렬 순서 포함)
- `flags`: 기능을 지정하는 비트 마스크
- `custom_paths`: 이 경로에서 사용하는 `Path` 노드 목록 (나중에 `Plan` 노드로 변환됨)
- `custom_restrictinfo`: 조인 릴레이션의 조인 절 (베이스 릴레이션의 경우 NIL)
- `custom_private`: 프로바이더의 프라이빗 데이터 (`nodeToString` 호환 필요)
- `methods`: `CustomPathMethods` 구현에 대한 포인터

##### flags 비트 마스크 옵션

- `CUSTOMPATH_SUPPORT_BACKWARD_SCAN`: 역방향 스캔 지원
- `CUSTOMPATH_SUPPORT_MARK_RESTORE`: mark/restore 작업 지원
- `CUSTOMPATH_SUPPORT_PROJECTION`: 스칼라 표현식 평가 가능

#### 커스텀 스캔 경로 콜백 (Custom Scan Path Callbacks)

##### PlanCustomPath

```c
Plan *(*PlanCustomPath) (PlannerInfo *root,
                         RelOptInfo *rel,
                         CustomPath *best_path,
                         List *tlist,
                         List *clauses,
                         List *custom_plans);
```

커스텀 경로를 완성된 `CustomScan` 계획으로 변환함.

- `root`: 플래너 정보
- `rel`: 릴레이션 최적화 정보
- `best_path`: 선택된 최적 경로
- `tlist`: 타겟 리스트
- `clauses`: 적용할 절(Clauses)
- `custom_plans`: 커스텀 계획 목록

##### ReparameterizeCustomPathByChild

```c
List *(*ReparameterizeCustomPathByChild) (PlannerInfo *root,
                                          List *custom_private,
                                          RelOptInfo *child_rel);
```

부모에서 자식 릴레이션 매개변수화로 변환할 때 경로를 재매개변수화함.

- `reparameterize_path_by_child()` 및 `adjust_appendrel_attrs()` 헬퍼 함수 사용 가능

#### 구현 시 주요 사항

1. 커스텀 경로는 베이스 릴레이션과 조인 릴레이션 모두에 대해 생성 가능
2. 훅은 코어 시스템 경로 생성 후 호출됨 (Gather/Gather Merge 제외)
3. 조인 훅은 서로 다른 내부/외부 릴레이션 조합으로 여러 번 호출될 수 있음
4. 프로바이더는 훅 콜백에서 중복 작업을 최소화해야 함
5. 프라이빗 데이터는 디버깅을 위해 `nodeToString`으로 직렬화 가능해야 함

---

### 61.2 커스텀 스캔 계획 (Custom Scan Plans)

커스텀 스캔 계획(Custom Scan Plan)은 커스텀 스캔을 위한 완성된 계획 트리 구조를 나타냄.

#### CustomScan 데이터 구조

```c
typedef struct CustomScan
{
    Scan      scan;
    uint32    flags;
    List     *custom_plans;
    List     *custom_exprs;
    List     *custom_private;
    List     *custom_scan_tlist;
    Bitmapset *custom_relids;
    const CustomScanMethods *methods;
} CustomScan;
```

##### 필드 설명

- `scan`: 표준 스캔 구조 (예상 비용, 타겟 리스트, 자격 조건 포함)
- `flags`: `CustomPath`와 동일한 의미의 비트 마스크
- `custom_plans`: 자식 `Plan` 노드 저장
- `custom_exprs`: `setrefs.c`와 `subselect.c`에 의해 수정이 필요한 표현식 트리
- `custom_private`: 커스텀 스캔 프로바이더만 사용하는 프라이빗 데이터
- `custom_scan_tlist`: 실제 스캔 튜플을 설명하는 타겟 리스트 (베이스 릴레이션 스캔의 경우 NIL, 조인의 경우 필수)
- `custom_relids`: 이 스캔 노드가 처리하는 릴레이션 집합 (범위 테이블 인덱스)
- `methods`: 필수 커스텀 스캔 메서드를 구현하는 객체에 대한 포인터

#### 주요 요구사항

- 단일 릴레이션 스캔: `scan.scanrelid`는 스캔할 테이블의 범위 테이블 인덱스여야 함
- 조인 대체: `scan.scanrelid`는 0이어야 함
- 데이터 복제: "custom" 필드의 모든 데이터는 `copyObject`가 처리할 수 있는 노드로 구성되어야 함

> 주의: `CustomPath`나 `CustomScanState`와 달리, `CustomScan`을 포함하는 더 큰 구조체로 대체할 수 없음.

#### 커스텀 스캔 계획 콜백 (Custom Scan Plan Callbacks)

##### CreateCustomScanState

```c
Node *(*CreateCustomScanState) (CustomScan *cscan);
```

`CustomScan`에 대한 `CustomScanState`를 할당함.

요구사항:
- `CustomScanState`보다 큰 구조체를 할당하여 프로바이더별 필드를 포함시킬 수 있음
- 노드 태그와 `methods` 필드가 적절히 설정되어야 함
- 다른 필드는 0으로 초기화되어야 함
- `ExecInitCustomScan`이 기본 초기화를 수행한 후, `BeginCustomScan` 콜백이 추가 프로바이더별 설정을 위해 호출됨

---

### 61.3 커스텀 스캔 실행 (Executing Custom Scans)

`CustomScan`이 실행될 때, 실행 상태는 `CustomScanState` 구조로 표현됨.

#### CustomScanState 데이터 구조

```c
typedef struct CustomScanState
{
    ScanState ss;
    uint32    flags;
    const CustomExecMethods *methods;
} CustomScanState;
```

##### 필드 설명

- `ss`: 표준 스캔 상태 초기화 (베이스 릴레이션용; 조인 스캔의 경우 `ss.ss_currentRelation`은 NULL)
- `flags`: `CustomPath` 및 `CustomScan`과 동일한 의미의 비트 마스크
- `methods`: 커스텀 스캔 상태 메서드를 구현하는 정적으로 할당된 객체에 대한 포인터

#### 필수 콜백 (Required Callbacks)

##### BeginCustomScan

```c
void (*BeginCustomScan) (CustomScanState *node, EState *estate, int eflags);
```

`CustomScanState`의 초기화를 완료함. 프라이빗 필드를 여기서 초기화하세요.

- `node`: 커스텀 스캔 상태
- `estate`: 실행 상태
- `eflags`: 실행 플래그

##### ExecCustomScan

```c
TupleTableSlot *(*ExecCustomScan) (CustomScanState *node);
```

다음 스캔 튜플을 가져옴.

- `ps_ResultTupleSlot`에 현재 스캔 방향의 다음 튜플을 채움
- 튜플이 더 이상 없으면 NULL 또는 빈 슬롯을 반환

##### EndCustomScan

```c
void (*EndCustomScan) (CustomScanState *node);
```

`CustomScanState`와 연관된 프라이빗 데이터를 정리함.

- 필수 콜백이지만, 정리할 내용이 없으면 아무것도 하지 않을 수 있음

##### ReScanCustomScan

```c
void (*ReScanCustomScan) (CustomScanState *node);
```

현재 스캔을 처음으로 되감고 릴레이션을 다시 스캔할 준비를 함.

#### 선택적 콜백 (Optional Callbacks)

##### Mark/Restore 지원

```c
void (*MarkPosCustomScan) (CustomScanState *node);
void (*RestrPosCustomScan) (CustomScanState *node);
```

스캔 위치를 저장하고 복원함.

> 주의: `CUSTOMPATH_SUPPORT_MARK_RESTORE` 플래그가 설정된 경우에만 필요함.

##### 병렬 실행 지원 (Parallel Execution Support)

```c
Size (*EstimateDSMCustomScan) (CustomScanState *node,
                               ParallelContext *pcxt);

void (*InitializeDSMCustomScan) (CustomScanState *node,
                                  ParallelContext *pcxt,
                                  void *coordinate);

void (*ReInitializeDSMCustomScan) (CustomScanState *node,
                                    ParallelContext *pcxt,
                                    void *coordinate);

void (*InitializeWorkerCustomScan) (CustomScanState *node,
                                     shm_toc *toc,
                                     void *coordinate);
```

- `EstimateDSMCustomScan`: 동적 공유 메모리(DSM) 크기 추정
- `InitializeDSMCustomScan`: DSM 초기화
- `ReInitializeDSMCustomScan`: DSM 재초기화
- `InitializeWorkerCustomScan`: 워커 프로세스 초기화

> 주의: 병렬 실행 지원이 필요한 경우에만 구현함.

##### ShutdownCustomScan

```c
void (*ShutdownCustomScan) (CustomScanState *node);
```

노드가 완료되지 않을 때 리소스를 해제함.

- DSM 세그먼트 파괴 전 정리에 유용

##### ExplainCustomScan

```c
void (*ExplainCustomScan) (CustomScanState *node,
                            List *ancestors,
                            ExplainState *es);
```

표준 스캔 상태 데이터 외에 추가 `EXPLAIN` 정보를 출력하기 위한 선택적 콜백임.

---

### 예제: 간단한 커스텀 스캔 프로바이더 구현

아래는 커스텀 스캔 프로바이더의 기본 구조를 보여주는 예제임.

#### 1. 헤더 및 구조체 정의

```c
#include "postgres.h"
#include "nodes/extensible.h"
#include "nodes/pathnodes.h"
#include "nodes/plannodes.h"
#include "optimizer/pathnode.h"
#include "optimizer/paths.h"

/* 커스텀 스캔 상태 구조체 */
typedef struct MyCustomScanState
{
    CustomScanState css;    /* 반드시 첫 번째 필드여야 함 */
    /* 프로바이더별 추가 필드 */
    int             current_pos;
    void           *private_data;
} MyCustomScanState;
```

#### 2. 경로 메서드 정의

```c
/* 경로를 계획으로 변환 */
static Plan *
my_plan_custom_path(PlannerInfo *root,
                    RelOptInfo *rel,
                    CustomPath *best_path,
                    List *tlist,
                    List *clauses,
                    List *custom_plans)
{
    CustomScan *cscan = makeNode(CustomScan);

    cscan->scan.plan.targetlist = tlist;
    cscan->scan.plan.qual = clauses;
    cscan->scan.scanrelid = rel->relid;
    cscan->flags = best_path->flags;
    cscan->custom_plans = custom_plans;
    cscan->custom_private = best_path->custom_private;
    cscan->methods = &my_custom_scan_methods;

    return &cscan->scan.plan;
}

static CustomPathMethods my_custom_path_methods = {
    .CustomName = "MyCustomScan",
    .PlanCustomPath = my_plan_custom_path,
};
```

#### 3. 스캔 메서드 정의

```c
/* 스캔 상태 생성 */
static Node *
my_create_custom_scan_state(CustomScan *cscan)
{
    MyCustomScanState *state;

    state = (MyCustomScanState *) palloc0(sizeof(MyCustomScanState));
    NodeSetTag(state, T_CustomScanState);
    state->css.methods = &my_custom_exec_methods;

    return (Node *) state;
}

static CustomScanMethods my_custom_scan_methods = {
    .CustomName = "MyCustomScan",
    .CreateCustomScanState = my_create_custom_scan_state,
};
```

#### 4. 실행 메서드 정의

```c
/* 초기화 */
static void
my_begin_custom_scan(CustomScanState *node, EState *estate, int eflags)
{
    MyCustomScanState *state = (MyCustomScanState *) node;
    state->current_pos = 0;
    /* 추가 초기화 로직 */
}

/* 다음 튜플 가져오기 */
static TupleTableSlot *
my_exec_custom_scan(CustomScanState *node)
{
    MyCustomScanState *state = (MyCustomScanState *) node;
    TupleTableSlot *slot = node->ss.ps.ps_ResultTupleSlot;

    /* 튜플 가져오기 로직 */
    /* 더 이상 튜플이 없으면 빈 슬롯 반환 */

    return slot;
}

/* 종료 */
static void
my_end_custom_scan(CustomScanState *node)
{
    MyCustomScanState *state = (MyCustomScanState *) node;
    /* 정리 로직 */
}

/* 재스캔 */
static void
my_rescan_custom_scan(CustomScanState *node)
{
    MyCustomScanState *state = (MyCustomScanState *) node;
    state->current_pos = 0;
    /* 재스캔 준비 */
}

static CustomExecMethods my_custom_exec_methods = {
    .CustomName = "MyCustomScan",
    .BeginCustomScan = my_begin_custom_scan,
    .ExecCustomScan = my_exec_custom_scan,
    .EndCustomScan = my_end_custom_scan,
    .ReScanCustomScan = my_rescan_custom_scan,
};
```

#### 5. 훅 등록

```c
/* 훅 함수 */
static void
my_set_rel_pathlist(PlannerInfo *root,
                    RelOptInfo *rel,
                    Index rti,
                    RangeTblEntry *rte)
{
    CustomPath *cpath;

    /* 이전 훅 호출 */
    if (prev_set_rel_pathlist_hook)
        prev_set_rel_pathlist_hook(root, rel, rti, rte);

    /* 조건 확인 후 커스텀 경로 추가 */
    if (/* 커스텀 스캔이 유용한 조건 */)
    {
        cpath = makeNode(CustomPath);
        cpath->path.type = T_CustomPath;
        cpath->path.pathtype = T_CustomScan;
        cpath->path.parent = rel;
        cpath->path.pathtarget = rel->reltarget;
        cpath->path.rows = rel->rows;
        /* 비용 설정 */
        cpath->path.startup_cost = 0;
        cpath->path.total_cost = /* 비용 계산 */;
        cpath->flags = 0;
        cpath->methods = &my_custom_path_methods;

        add_path(rel, &cpath->path);
    }
}

/* 모듈 로드 시 */
void
_PG_init(void)
{
    prev_set_rel_pathlist_hook = set_rel_pathlist_hook;
    set_rel_pathlist_hook = my_set_rel_pathlist;
}
```

---

### 설계 시 고려사항

#### 성능 최적화

1. 비용 추정 정확성: 플래너가 올바른 결정을 내리도록 정확한 비용 추정 제공
2. 병렬 처리: 가능한 경우 병렬 실행 지원 구현
3. 메모리 관리: 적절한 메모리 컨텍스트 사용

#### 호환성

1. 버전 호환성: PostgreSQL 버전 간 API 변경 사항 확인
2. 직렬화: `custom_private` 데이터는 `nodeToString` 호환 필요
3. 복사 가능성: 모든 커스텀 데이터는 `copyObject` 처리 가능해야 함

#### 디버깅

1. EXPLAIN 지원: `ExplainCustomScan` 콜백을 통한 상세 정보 제공
2. 로깅: 적절한 디버그 로깅 구현
3. 오류 처리: 명확한 오류 메시지 제공

---

### 참고 자료

- [PostgreSQL 공식 문서 - Writing a Custom Scan Provider](https://www.postgresql.org/docs/current/custom-scan.html)
- [PostgreSQL 공식 문서 - Custom Scan Paths](https://www.postgresql.org/docs/current/custom-scan-path.html)
- [PostgreSQL 공식 문서 - Custom Scan Plans](https://www.postgresql.org/docs/current/custom-scan-plan.html)
- [PostgreSQL 공식 문서 - Executing Custom Scans](https://www.postgresql.org/docs/current/custom-scan-execution.html)

---

## Chapter 65: Generic WAL Records (일반 WAL 레코드)

### 목차
1. [개요](#개요)
2. [API 함수](#api-함수)
3. [사용 절차](#사용-절차)
4. [주요 제약 사항 및 동작](#주요-제약-사항-및-동작)
5. [사용 예제](#사용-예제)
6. [추가 고려 사항](#추가-고려-사항)

---

### 개요

Generic WAL Records(일반 WAL 레코드)는 PostgreSQL 확장(Extension)이 커스텀 WAL 리소스 매니저(Custom WAL Resource Manager)를 별도로 구현하지 않고도 WAL에 기록되는 데이터 업데이트를 수행할 수 있도록 하는 내장 메커니즘임.

페이지(Page)의 변경 사항을 범용적인 방식으로 기술하므로, 확장 개발자가 복잡한 WAL 인프라를 직접 구현하지 않아도 데이터 무결성과 복구 가능성을 보장받을 수 있음.

#### 중요 사항

> 주의: Generic WAL 레코드는 논리적 디코딩(Logical Decoding) 과정에서 무시됨. 만약 논리적 디코딩이 필요한 경우에는 대신 커스텀 WAL 리소스 매니저(Custom WAL Resource Manager) 를 사용해야 함.

#### API 위치

- 헤더 파일: `access/generic_xlog.h`
- 구현 파일: `access/transam/generic_xlog.c`

---

### API 함수

Generic WAL의 주요 API 함수는 다음과 같음.

#### GenericXLogStart

```c
GenericXLogState *GenericXLogStart(Relation relation)
```

설명: 지정한 릴레이션(Relation)에 대한 Generic WAL 레코드 생성을 시작함.

매개변수:
- `relation`: WAL 레코드를 생성할 대상 릴레이션

반환값: Generic WAL 상태 객체에 대한 포인터

---

#### GenericXLogRegisterBuffer

```c
Page GenericXLogRegisterBuffer(GenericXLogState *state, Buffer buffer, int flags)
```

설명: 수정할 버퍼를 등록하고, 해당 버퍼 페이지의 임시 복사본에 대한 포인터를 반환함.

매개변수:
- `state`: `GenericXLogStart()`에서 반환된 상태 객체
- `buffer`: 수정할 버퍼
- `flags`: 동작 플래그

반환값: 버퍼 페이지의 임시 복사본에 대한 포인터

플래그 옵션:

- `GENERIC_XLOG_FULL_IMAGE`: 델타(Delta) 업데이트 대신 전체 페이지 이미지(Full-Page Image)를 포함함. 새로 생성되거나 완전히 재작성된 페이지에 사용함.

> 중요: 반드시 이 함수가 반환한 복사본을 수정해야 함. 원본 버퍼를 직접 수정해서는 안 됨!

---

#### GenericXLogFinish

```c
XLogRecPtr GenericXLogFinish(GenericXLogState *state)
```

설명: 실제 버퍼에 변경 사항을 적용하고 Generic WAL 레코드를 발행(Emit)함.

매개변수:
- `state`: Generic WAL 상태 객체

반환값: 발행된 WAL 레코드의 LSN(Log Sequence Number)

동작:
- 버퍼를 자동으로 dirty로 표시
- LSN을 자동으로 설정
- 별도의 명시적 호출 불필요

---

#### GenericXLogAbort

```c
void GenericXLogAbort(GenericXLogState *state)
```

설명: 페이지 이미지 복사본에 대한 모든 변경 사항을 폐기함.

매개변수:
- `state`: Generic WAL 상태 객체

사용 시점: `GenericXLogStart()` 이후 어떤 단계에서든 호출 가능

---

### 사용 절차

Generic WAL 레코드를 사용해 WAL에 기록되는 데이터 업데이트를 수행하는 절차임.

#### 1단계: 레코드 생성 시작

```c
GenericXLogState *state;
state = GenericXLogStart(relation);
```

지정한 릴레이션에 대한 Generic WAL 레코드 생성을 시작함.

#### 2단계: 버퍼 등록

```c
Page page;
page = GenericXLogRegisterBuffer(state, buffer, flags);
```

수정할 버퍼를 등록함. 여러 페이지를 수정해야 할 경우 이 함수를 여러 번 호출할 수 있음.

#### 3단계: 페이지 수정

`GenericXLogRegisterBuffer()`에서 얻은 페이지 이미지에 원하는 수정을 적용함.

```c
// 페이지 복사본에 대한 수정 작업 수행
// 예: 아이템 추가, 데이터 변경 등
```

#### 4단계: 완료 및 발행

```c
GenericXLogFinish(state);
```

변경 사항을 실제 버퍼에 적용하고 Generic WAL 레코드를 발행함.

#### 취소 시

오류 발생 또는 작업 취소가 필요한 경우:

```c
GenericXLogAbort(state);
```

---

### 주요 제약 사항 및 동작

#### 버퍼 수정 규칙

- 직접 수정 금지: 모든 변경은 `GenericXLogRegisterBuffer()`에서 반환된 복사본을 통해서만 이루어져야 함. `BufferGetPage()`를 직접 호출하여 수정하면 안 됨.
- 락 관리: 호출자가 버퍼의 pin/unpin 및 lock/unlock을 직접 관리해야 함.
- 배타적 락 필요: `GenericXLogRegisterBuffer()` 호출 전부터 `GenericXLogFinish()` 호출 후까지 배타적 락(Exclusive Lock)을 유지해야 함.

#### 단계 순서

- 2단계(버퍼 등록)와 3단계(페이지 수정)는 어떤 순서로든 자유롭게 혼합할 수 있음.
- 리플레이(Replay) 시 락을 획득할 순서대로 버퍼를 등록해야 함.

#### 버퍼 제한

- 하나의 레코드에 등록할 수 있는 최대 버퍼 수는 `MAX_GENERIC_XLOG_PAGES`로 제한됨.
- 이 한도를 초과하면 오류가 발생함.

#### 페이지 레이아웃

Generic WAL은 표준 페이지 레이아웃(Standard Page Layout)을 가정함. `pd_lower`와 `pd_upper` 사이에는 유용한 데이터가 없다고 간주함.

#### 크리티컬 섹션 (Critical Section)

- `GenericXLogStart()`와 `GenericXLogFinish()` 사이에는 크리티컬 섹션이 없음.
- 따라서 이 구간에서 메모리 할당이나 오류 발생(Error Throw)이 허용됨.
- 크리티컬 섹션은 `GenericXLogFinish()` 내부에만 존재함.

#### 버퍼 관리

`GenericXLogFinish()`가 자동으로 처리하는 작업:
- 버퍼를 dirty로 표시
- LSN 설정

별도의 명시적 호출은 필요하지 않음.

#### 언로그드 릴레이션 (Unlogged Relations)

- 언로그드 릴레이션에서도 동일하게 작동함.
- 단, WAL 레코드가 발행되지 않음.
- 특별한 검사가 필요하지 않음.

#### 리두 동작 (Redo Behavior)

- Generic WAL 리두(Redo)는 등록 순서대로 배타적 락을 획득함.
- 이후 동일한 순서로 락을 해제함.

#### 델타 인코딩 (Delta Encoding)

- `GENERIC_XLOG_FULL_IMAGE` 플래그 없이 사용하면, 레코드에는 바이트 단위 델타(Byte-by-Byte Delta)가 포함됨.
- 페이지 내 데이터 이동에는 최적화되어 있지 않음.

---

### 사용 예제

#### 기본 사용 예제

```c
#include "access/generic_xlog.h"
#include "storage/bufmgr.h"

void
my_extension_insert(Relation relation, Buffer buffer, void *data)
{
    GenericXLogState *state;
    Page page;

    /* 버퍼에 배타적 락 획득 */
    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    /* Generic WAL 레코드 생성 시작 */
    state = GenericXLogStart(relation);

    /* 버퍼 등록 및 페이지 복사본 획득 */
    page = GenericXLogRegisterBuffer(state, buffer, 0);

    /* 페이지 복사본 수정 */
    /* 예: 새 아이템 추가 */
    PageAddItem(page, (Item) data, sizeof(MyData), InvalidOffsetNumber,
                false, false);

    /* 변경 사항 적용 및 WAL 레코드 발행 */
    GenericXLogFinish(state);

    /* 버퍼 락 해제 */
    LockBuffer(buffer, BUFFER_LOCK_UNLOCK);
}
```

#### 새 페이지 초기화 예제

```c
void
my_extension_init_page(Relation relation, Buffer buffer)
{
    GenericXLogState *state;
    Page page;

    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    state = GenericXLogStart(relation);

    /* 새 페이지이므로 GENERIC_XLOG_FULL_IMAGE 플래그 사용 */
    page = GenericXLogRegisterBuffer(state, buffer, GENERIC_XLOG_FULL_IMAGE);

    /* 페이지 초기화 */
    PageInit(page, BufferGetPageSize(buffer), 0);

    /* 메타데이터 설정 등 추가 작업 */
    /* ... */

    GenericXLogFinish(state);

    LockBuffer(buffer, BUFFER_LOCK_UNLOCK);
}
```

#### 다중 페이지 수정 예제

```c
void
my_extension_split_page(Relation relation, Buffer leftbuf, Buffer rightbuf)
{
    GenericXLogState *state;
    Page leftpage, rightpage;

    /* 두 버퍼 모두 락 획득 (데드락 방지를 위해 순서 고려) */
    LockBuffer(leftbuf, BUFFER_LOCK_EXCLUSIVE);
    LockBuffer(rightbuf, BUFFER_LOCK_EXCLUSIVE);

    state = GenericXLogStart(relation);

    /* 두 버퍼 모두 등록 */
    leftpage = GenericXLogRegisterBuffer(state, leftbuf, 0);
    rightpage = GenericXLogRegisterBuffer(state, rightbuf, GENERIC_XLOG_FULL_IMAGE);

    /* 왼쪽 페이지에서 일부 데이터를 오른쪽 페이지로 이동 */
    /* ... 페이지 분할 로직 ... */

    GenericXLogFinish(state);

    LockBuffer(rightbuf, BUFFER_LOCK_UNLOCK);
    LockBuffer(leftbuf, BUFFER_LOCK_UNLOCK);
}
```

#### 오류 처리 예제

```c
void
my_extension_update_with_error_handling(Relation relation, Buffer buffer)
{
    GenericXLogState *state;
    Page page;

    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    state = GenericXLogStart(relation);

    PG_TRY();
    {
        page = GenericXLogRegisterBuffer(state, buffer, 0);

        /* 수정 작업 수행 */
        /* 이 과정에서 오류가 발생할 수 있음 */
        perform_modification(page);

        /* 성공 시 완료 */
        GenericXLogFinish(state);
    }
    PG_CATCH();
    {
        /* 오류 발생 시 변경 사항 폐기 */
        GenericXLogAbort(state);

        LockBuffer(buffer, BUFFER_LOCK_UNLOCK);

        /* 오류 재발생 */
        PG_RE_THROW();
    }
    PG_END_TRY();

    LockBuffer(buffer, BUFFER_LOCK_UNLOCK);
}
```

#### 조건부 업데이트 예제

```c
bool
my_extension_conditional_update(Relation relation, Buffer buffer,
                                 ItemPointer tid, void *newdata)
{
    GenericXLogState *state;
    Page page;
    ItemId itemid;
    bool success = false;

    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    state = GenericXLogStart(relation);
    page = GenericXLogRegisterBuffer(state, buffer, 0);

    /* 조건 검사 */
    itemid = PageGetItemId(page, ItemPointerGetOffsetNumber(tid));

    if (ItemIdIsNormal(itemid))
    {
        /* 조건이 충족되면 업데이트 수행 */
        /* ... 업데이트 로직 ... */

        GenericXLogFinish(state);
        success = true;
    }
    else
    {
        /* 조건이 충족되지 않으면 변경 폐기 */
        GenericXLogAbort(state);
        success = false;
    }

    LockBuffer(buffer, BUFFER_LOCK_UNLOCK);

    return success;
}
```

---

### 추가 고려 사항

#### Generic WAL vs Custom WAL Resource Manager

- 구현 복잡도
  - Generic WAL: 낮음
  - Custom WAL Resource Manager: 높음
- 논리적 디코딩 지원
  - Generic WAL: 지원 안 함
  - Custom WAL Resource Manager: 지원
- 유연성
  - Generic WAL: 제한적
  - Custom WAL Resource Manager: 높음
- 페이지 레이아웃
  - Generic WAL: 표준 레이아웃 필요
  - Custom WAL Resource Manager: 자유로움
- 적합한 용도
  - Generic WAL: 간단한 확장
  - Custom WAL Resource Manager: 복잡한 스토리지 엔진

#### 성능 고려 사항

1. 전체 페이지 이미지 (Full-Page Image)
   - `GENERIC_XLOG_FULL_IMAGE` 플래그 사용 시 WAL 크기가 증가함.
   - 새 페이지나 완전히 재작성된 페이지에만 사용하세요.

2. 델타 인코딩 한계
   - 바이트 단위 델타는 페이지 내 데이터 이동에 비효율적임.
   - 대량의 데이터 재배치 시 전체 페이지 이미지가 더 효율적일 수 있음.

3. 버퍼 수 제한
   - `MAX_GENERIC_XLOG_PAGES` 제한을 고려하여 설계하세요.
   - 대규모 트랜잭션은 여러 Generic WAL 레코드로 분할해야 할 수 있음.

#### 디버깅 팁

- WAL 레코드 내용 확인: `pg_waldump` 유틸리티 사용
- 레코드 타입: `XLOG_GENERIC`로 표시됨
- 복구 테스트: 크래시 복구 시나리오를 반드시 테스트하세요

---

### 참고 자료

- [PostgreSQL 공식 문서 - Generic WAL Records](https://www.postgresql.org/docs/current/generic-wal.html)
- [PostgreSQL 공식 문서 - Custom WAL Resource Managers](https://www.postgresql.org/docs/current/custom-rmgr.html)
- [PostgreSQL 소스 코드 - generic_xlog.h](https://github.com/postgres/postgres/blob/master/src/include/access/generic_xlog.h)
- [PostgreSQL 소스 코드 - generic_xlog.c](https://github.com/postgres/postgres/blob/master/src/backend/access/transam/generic_xlog.c)

---

## Chapter 64: 인덱스 접근 메서드 인터페이스 (Index Access Method Interface Definition)

이 문서는 PostgreSQL 핵심 시스템과 인덱스 접근 메서드(Index Access Methods) 간의 인터페이스를 정의함. 핵심 시스템은 이 인터페이스에서 지정한 사항 이외의 인덱스에 대해 아무것도 알지 못하므로, 추가 코드를 작성하여 완전히 새로운 인덱스 유형을 개발할 수 있음.

### 목차

1. [인덱스 접근 메서드 개요](#1-인덱스-접근-메서드-개요)
2. [인덱스 기본 API 구조](#2-인덱스-기본-api-구조-basic-api-structure-for-indexes)
3. [인덱스 접근 메서드 함수](#3-인덱스-접근-메서드-함수-index-access-method-functions)
4. [인덱스 스캔](#4-인덱스-스캔-index-scanning)
5. [인덱스 잠금 고려사항](#5-인덱스-잠금-고려사항-index-locking-considerations)
6. [인덱스 고유성 검사](#6-인덱스-고유성-검사-index-uniqueness-checks)
7. [인덱스 비용 추정 함수](#7-인덱스-비용-추정-함수-index-cost-estimation-functions)

---

### 1. 인덱스 접근 메서드 개요

#### 1.1 보조 인덱스 (Secondary Indexes)

PostgreSQL의 모든 인덱스는 보조 인덱스(secondary indexes) 임. 이는 인덱스가 테이블 파일과 물리적으로 분리되어 있음을 의미함. 각 인덱스는 자체 물리적 릴레이션(relation) 으로 저장되며, `pg_class` 시스템 카탈로그에 별도의 엔트리를 가짐.

#### 1.2 인덱스의 역할

인덱스의 주요 역할은 데이터 키 값에서 TID(Tuple Identifiers, 튜플 식별자) 로의 매핑을 제공하는 것임:

- TID 구성: 블록 번호(block number)와 해당 블록 내의 항목 번호(item number)
- 목적: 테이블에서 특정 행 버전(row version)을 가져오는 데 필요한 정보 제공

#### 1.3 저장소 구조

모든 인덱스 접근 메서드는 다음과 같은 저장소 구조를 따름:

- 인덱스를 표준 크기 페이지(standard-size pages) 로 분할
- 일반 저장소 관리자(storage manager) 및 버퍼 관리자(buffer manager) 사용
- 기존 인덱스 접근 메서드들은 표준 페이지 레이아웃 사용

#### 1.4 MVCC와 인덱스

MVCC(Multi-Version Concurrency Control) 환경에서의 인덱스 동작:

```
동일한 논리적 행 → 여러 물리적 버전 존재 가능
인덱스는 각 튜플을 독립적인 객체로 취급
```

- 행 업데이트 시 키 값이 변경되지 않아도 새로운 인덱스 엔트리 생성
- HOT(Heap-Only Tuples) 예외: HOT 튜플은 인덱스 처리 대상이 아님

---

### 2. 인덱스 기본 API 구조 (Basic API Structure for Indexes)

#### 2.1 카탈로그 엔트리 (Catalog Entries)

인덱스 접근 메서드는 `pg_am` 시스템 카탈로그의 행으로 정의됨. 각 접근 메서드는 이름(name) 과 핸들러 함수(handler function) 를 지정함.

##### 필수 카탈로그 항목

- `pg_am`: 접근 메서드 정의
- `pg_opfamily`: 연산자 족 (Operator Family)
- `pg_opclass`: 연산자 클래스 (Operator Class)
- `pg_amop`: 접근 메서드 연산자
- `pg_amproc`: 접근 메서드 지원 함수

##### 접근 메서드 생성 SQL

```sql
-- 새로운 접근 메서드 생성
CREATE ACCESS METHOD method_name
    TYPE INDEX
    HANDLER handler_function;

-- 접근 메서드 삭제
DROP ACCESS METHOD method_name;
```

#### 2.2 핸들러 함수 요구사항

핸들러 함수의 특성:

- 입력: `internal` 타입의 더미 값 (SQL에서 직접 호출 방지)
- 반환값: `IndexAmRoutine` 타입의 palloc'd 구조체
- 목적: 코어 코드가 인덱스 접근 메서드를 사용하는 데 필요한 모든 정보 제공

#### 2.3 IndexAmRoutine 구조체

```c
typedef struct IndexAmRoutine
{
    NodeTag     type;

    /*
     * 전략(Strategy) 및 지원 함수 수
     */
    uint16      amstrategies;       /* 전략(연산자) 수 */
    uint16      amsupport;          /* 지원 함수 총 수 */
    uint16      amoptsprocnum;      /* opclass 옵션 지원 함수 번호 */

    /*
     * 부울 플래그 필드 - 접근 메서드의 기능 정의
     */
    bool        amcanorder;         /* 정렬된 스캔 지원 */
    bool        amcanorderbyop;     /* 연산자 결과 기준 정렬 지원 */
    bool        amcanhash;          /* 해싱 지원 */
    bool        amconsistentequality;   /* 일관된 동등성 의미론 */
    bool        amconsistentordering;   /* 일관된 정렬 의미론 */
    bool        amcanbackward;      /* 역방향 스캔 지원 */
    bool        amcanunique;        /* 고유 인덱스 지원 */
    bool        amcanmulticol;      /* 다중 열 인덱스 지원 */
    bool        amoptionalkey;      /* 첫 열 제약 없이 스캔 가능 */
    bool        amsearcharray;      /* ScalarArrayOpExpr 지원 */
    bool        amsearchnulls;      /* IS NULL/IS NOT NULL 지원 */
    bool        amstorage;          /* 저장소 타입 차이 허용 */
    bool        amclusterable;      /* 클러스터링 가능 */
    bool        ampredlocks;        /* 세밀한 술어 잠금 처리 */
    bool        amcanparallel;      /* 병렬 스캔 지원 */
    bool        amcanbuildparallel; /* 병렬 빌드 지원 */
    bool        amcaninclude;       /* INCLUDE 절 지원 */
    bool        amusemaintenanceworkmem; /* maintenance_work_mem 사용 */
    bool        amsummarizing;      /* 튜플 요약 수행 (예: BRIN) */

    uint8       amparallelvacuumoptions; /* 병렬 vacuum 플래그 */
    Oid         amkeytype;          /* 인덱스 데이터 타입 */

    /*
     * 인터페이스 함수 포인터
     */
    /* 인덱스 구축 및 유지보수 */
    ambuild_function ambuild;
    ambuildempty_function ambuildempty;
    aminsert_function aminsert;
    aminsertcleanup_function aminsertcleanup;
    ambulkdelete_function ambulkdelete;
    amvacuumcleanup_function amvacuumcleanup;

    /* 인덱스 기능 지원 */
    amcanreturn_function amcanreturn;
    amcostestimate_function amcostestimate;
    amgettreeheight_function amgettreeheight;
    amoptions_function amoptions;
    amproperty_function amproperty;
    ambuildphasename_function ambuildphasename;
    amvalidate_function amvalidate;
    amadjustmembers_function amadjustmembers;

    /* 인덱스 스캔 */
    ambeginscan_function ambeginscan;
    amrescan_function amrescan;
    amgettuple_function amgettuple;
    amgetbitmap_function amgetbitmap;
    amendscan_function amendscan;
    ammarkpos_function ammarkpos;
    amrestrpos_function amrestrpos;

    /* 병렬 스캔 지원 */
    amestimateparallelscan_function amestimateparallelscan;
    aminitparallelscan_function aminitparallelscan;
    amparallelrescan_function amparallelrescan;

    /* 전략 변환 */
    amtranslate_strategy_function amtranslatestrategy;
    amtranslate_cmptype_function amtranslatecmptype;
} IndexAmRoutine;
```

#### 2.4 주요 플래그 필드 설명

##### amcanmulticol과 amoptionalkey

- `amcanmulticol=true`: 다중 열 인덱스 지원
- `amoptionalkey=true`: 첫 번째 열에 대한 제약 조건 없이 스캔 가능

NULL 색인화 규칙: `amoptionalkey=true`인 경우 NULL 값을 반드시 색인화해야 함.

##### amcaninclude

- "포함된 열(included columns)" 지원 (키 열 이외의 추가 열 저장)
- `amcanmulticol=false`, `amcaninclude=true` 조합 가능
- 포함된 열은 `amoptionalkey`와 무관하게 NULL 허용

##### amsummarizing

- 블록 단위 이상의 튜플 요약 수행
- BRIN 같은 접근 메서드가 HOT 최적화 를 계속 허용

---

### 3. 인덱스 접근 메서드 함수 (Index Access Method Functions)

#### 3.1 인덱스 구축 및 유지보수 함수

##### ambuild - 인덱스 구축

```c
IndexBuildResult *ambuild(Relation heapRelation,
                          Relation indexRelation,
                          IndexInfo *indexInfo);
```

- 목적: 새 인덱스 구축
- 동작: 빈 인덱스를 채우고 기존 테이블의 모든 튜플에 대해 엔트리 생성
- 반환: 인덱스 통계 정보를 담은 palloc'd 구조체

##### ambuildempty - 빈 인덱스 초기화

```c
void ambuildempty(Relation indexRelation);
```

- 목적: 빈 인덱스 구축 후 초기화 포크(INIT_FORKNUM)에 작성
- 사용처: 로깅되지 않는(unlogged) 인덱스 에만 호출

##### aminsert - 튜플 삽입

```c
bool aminsert(Relation indexRelation,
              Datum *values,
              bool *isnull,
              ItemPointer heap_tid,
              Relation heapRelation,
              IndexUniqueCheck checkUnique,
              bool indexUnchanged,
              IndexInfo *indexInfo);
```

- `values`, `isnull`: 인덱싱할 키 값
- `heap_tid`: 인덱싱될 TID
- `checkUnique`: 고유성 검사 유형
- `indexUnchanged`: 중복 튜플 여부 힌트

반환값: `UNIQUE_CHECK_PARTIAL`일 때만 의미 있음 (true=고유, false=중복 가능성)

##### aminsertcleanup - 삽입 후 정리

```c
void aminsertcleanup(Relation indexRelation,
                     IndexInfo *indexInfo);
```

연속 삽입 중 `indexInfo->ii_AmCache`에 유지된 상태를 정리함. 메모리 이외의 자원 해제가 필요한 경우 사용됨.

##### ambulkdelete - 일괄 삭제

```c
IndexBulkDeleteResult *ambulkdelete(IndexVacuumInfo *info,
                                    IndexBulkDeleteResult *stats,
                                    IndexBulkDeleteCallback callback,
                                    void *callback_state);
```

- 목적: 인덱스에서 튜플 일괄 삭제
- 동작: 전체 인덱스를 스캔하고 콜백 함수로 삭제 대상 결정
- 콜백: `callback(TID, callback_state)` - bool 반환
- 반환: 삭제 결과 통계 또는 NULL

##### amvacuumcleanup - VACUUM 후 정리

```c
IndexBulkDeleteResult *amvacuumcleanup(IndexVacuumInfo *info,
                                       IndexBulkDeleteResult *stats);
```

VACUUM 작업 완료 후 정리를 수행함. 빈 페이지 회수 등 대량 정리 작업이 가능하며, ANALYZE 완료 시에도 호출될 수 있음.

#### 3.2 인덱스 기능 지원 함수

##### amcanreturn - 인덱스 전용 스캔 지원 확인

```c
bool amcanreturn(Relation indexRelation, int attno);
```

인덱스 전용 스캔(Index-Only Scan) 지원 여부를 확인함. 포함된 컬럼(included columns)은 항상 true를 반환함.

##### amcostestimate - 비용 추정

```c
void amcostestimate(PlannerInfo *root,
                    IndexPath *path,
                    double loop_count,
                    Cost *indexStartupCost,
                    Cost *indexTotalCost,
                    Selectivity *indexSelectivity,
                    double *indexCorrelation,
                    double *indexPages);
```

인덱스 스캔의 비용을 추정함. 자세한 내용은 [7. 인덱스 비용 추정 함수](#7-인덱스-비용-추정-함수-index-cost-estimation-functions)를 참조하세요.

##### amgettreeheight - 트리 높이 계산

```c
int amgettreeheight(Relation rel);
```

트리 형태 인덱스의 높이를 계산함. 비용 추정에 사용됨.

##### amoptions - 옵션 파싱

```c
bytea *amoptions(ArrayType *reloptions, bool validate);
```

인덱스 reloptions를 파싱하고 검증함.

##### amproperty - 속성 조회

```c
bool amproperty(Oid index_oid, int attno,
                IndexAMProperty prop, const char *propname,
                bool *res, bool *isnull);
```

인덱스 속성 조회를 재정의함.

##### amvalidate - 연산자 클래스 검증

```c
bool amvalidate(Oid opclassoid);
```

연산자 클래스 카탈로그 항목의 유효성을 검증함.

#### 3.3 인덱스 스캔 함수

##### ambeginscan - 스캔 시작

```c
IndexScanDesc ambeginscan(Relation indexRelation,
                          int nkeys,
                          int norderbys);
```

- 목적: 인덱스 스캔 준비
- 주의: `RelationGetIndexScan()`으로 구조체 생성 필수
- 반환: palloc'd 구조체

##### amrescan - 스캔 재시작

```c
void amrescan(IndexScanDesc scan,
              ScanKey keys,
              int nkeys,
              ScanKey orderbys,
              int norderbys);
```

인덱스 스캔을 시작하거나 재시작함. 스캔 키 개수는 `ambeginscan`에 전달된 값 이하여야 함.

##### amgettuple - 튜플 페치

```c
bool amgettuple(IndexScanDesc scan,
                ScanDirection direction);
```

- 목적: 스캔에서 다음 튜플 페치
- 반환: true=튜플 획득, false=일치하는 튜플 없음
- 출력: 성공 시 `scan->xs_recheck` 설정 (true=재확인 필요)

Index-Only Scan: `scan->xs_want_itup=true`이면 원본 인덱스 데이터를 반환함.

##### amgetbitmap - 비트맵 스캔

```c
int64 amgetbitmap(IndexScanDesc scan,
                  TIDBitmap *tbm);
```

- 목적: 스캔의 모든 튜플을 TIDBitmap에 추가
- 반환: 페치된 튜플 개수
- 동작: 튜플 ID를 비트맵에 OR 연산

##### amendscan - 스캔 종료

```c
void amendscan(IndexScanDesc scan);
```

스캔을 종료하고 자원을 해제함. scan 구조체 자체는 해제하지 않음.

##### ammarkpos / amrestrpos - 위치 마킹

```c
void ammarkpos(IndexScanDesc scan);   /* 현재 위치 표시 */
void amrestrpos(IndexScanDesc scan);  /* 표시된 위치로 복원 */
```

정렬된 스캔에서 위치를 기억하고 복원함. 스캔당 하나의 기억 위치만 지원됨.

#### 3.4 병렬 스캔 함수

##### amestimateparallelscan

```c
Size amestimateparallelscan(Relation indexRelation,
                            int nkeys,
                            int norderbys);
```

병렬 스캔에 필요한 동적 공유 메모리 바이트 수를 추정함.

##### aminitparallelscan

```c
void aminitparallelscan(void *target);
```

병렬 스캔 시작 시 동적 공유 메모리를 초기화함.

##### amparallelrescan

```c
void amparallelrescan(IndexScanDesc scan);
```

병렬 인덱스 스캔 재시작 시 공유 상태를 초기화함.

#### 3.5 전략 변환 함수

```c
CompareType amtranslatestrategy(StrategyNumber strategy,
                                Oid opfamily, Oid opcintype);

StrategyNumber amtranslatecmptype(CompareType cmptype,
                                  Oid opfamily, Oid opcintype);
```

`CompareType`과 전략 번호 간의 변환을 수행함. btree/hash와 유사한 기능의 접근 메서드에서 사용됨.

---

### 4. 인덱스 스캔 (Index Scanning)

#### 4.1 스캔 키 (Scan Keys)

인덱스 스캔에서 접근 메서드는 스캔 키(scan keys) 와 일치하는 모든 튜플의 TID를 반환해야 함.

```
index_key operator constant
```

- 인덱스 열 중 하나와 해당 인덱스 열의 연산자 패밀리(operator family) 멤버로 구성
- 스캔 키는 암시적으로 AND 연산됨
- 반환되는 튜플은 모든 조건을 만족해야 함

#### 4.2 Lossy 인덱스 스캔

인덱스가 스캔 키를 정확히 만족하는 항목 외에 추가 항목을 반환할 수 있음:

```
Lossy 스캔 결과 = 정확한 일치 항목 + 추가 항목 (false positive)
                  ↓
        코어 시스템이 힙 튜플에서 재확인(recheck)
```

- `xs_recheck` 플래그로 재확인 필요 여부 표시
- 재확인 미지정 시 정확한 일치 항목만 반환해야 함

#### 4.3 정렬된 출력 지원

##### amcanorder = true

```c
/* 항상 자연 정렬 순서로 반환 (예: B-tree) */
/* btree 호환 전략 번호 필수 */
```

##### amcanorderbyop = true

```sql
-- ORDER BY index_key operator constant 지원
SELECT * FROM table ORDER BY point_column <-> '(0,0)'::point;
```

#### 4.4 스캔 방향 (Scan Direction)

`amgettuple` 함수의 direction 인자:

- `ForwardScanDirection`: 정방향 스캔 (앞에서 뒤로)
- `BackwardScanDirection`: 역방향 스캔 (뒤에서 앞으로)

동작 예시:

```c
/* 첫 호출이 BackwardScanDirection인 경우 */
/* → 마지막 일치 항목부터 반환 시작 */

/* 이후 호출에서는 방향 전환 가능 */
/* amcanbackward=false인 경우 첫 호출 방향 유지 */
```

#### 4.5 두 가지 스캔 방식 비교

##### amgettuple 방식

```c
/* 튜플을 반복적으로 하나씩 반환 */
while (amgettuple(scan, ForwardScanDirection)) {
    /* 힙 튜플 처리 */
    process_tuple(scan->xs_ctup);
}
```

특징:
- 방향 제어 가능
- 인덱스 전용 스캔(Index-Only Scan) 지원
- 위치 마킹/복원 지원

##### amgetbitmap 방식

```c
/* 한 번의 호출로 모든 튜플 반환 */
int64 count = amgetbitmap(scan, bitmap);
/* 비트맵에서 TID 추출하여 처리 */
```

특징:
- lock/unlock 사이클 회피로 효율적
- 위치 마킹/복원 불가
- 정렬 순서 없음
- 인덱스 전용 스캔 미지원

#### 4.6 동시성 고려사항

허용되는 상황:
- 스캔 시작 후 삽입된 항목의 미반환
- 스캔 중 삭제된 항목의 반영 여부 불확실

필수 조건:
- 삽입/삭제로 인해 스캔이 항목을 놓치거나 중복 반환하지 않아야 함
- (삽입/삭제 중인 항목 제외)

---

### 5. 인덱스 잠금 고려사항 (Index Locking Considerations)

#### 5.1 잠금 유형

##### PostgreSQL 핵심 시스템의 잠금

- 인덱스 스캔
  - 잠금 유형: `AccessShareLock`
  - 설명: 읽기 잠금
- 인덱스 업데이트
  - 잠금 유형: `RowExclusiveLock`
  - 설명: 일반 VACUUM 포함
- 인덱스 생성/삭제/REINDEX
  - 잠금 유형: `ACCESS EXCLUSIVE`
  - 설명: 배타적 잠금
- CONCURRENTLY 옵션
  - 잠금 유형: `SHARE UPDATE EXCLUSIVE`
  - 설명: 동시 작업 허용

중요: `AccessShareLock`과 `RowExclusiveLock`은 충돌하지 않음 - 접근 메서드가 세밀한 잠금을 책임져야 함.

#### 5.2 동시성 제어 규칙

##### 힙 항목 생성 순서

```
새 힙 항목 생성 → 인덱스 항목 생성
```

- 동시 인덱스 스캔이 힙 항목을 보지 못할 수 있음 (정상 동작)
- 커밋되지 않은 행은 어차피 관심 대상이 아님

##### 힙 항목 삭제 순서

```
모든 인덱스 항목 제거 → 힙 항목 삭제 (VACUUM)
```

#### 5.3 인덱스 페이지 핀(Pin)

인덱스 스캔은 `amgettuple`이 반환한 마지막 항목이 있는 인덱스 페이지에 핀(pin) 을 유지해야 함:

```c
/* 핀 유지 이유: VACUUM이 인덱스 항목을 제거한 후
 * 읽기 프로세스가 대응하는 힙 항목에 도달하는 문제 방지 */
ambulkdelete() // 다른 백엔드가 핀을 유지한 페이지에서 항목 삭제 불가
```

#### 5.4 동기 vs 비동기 스캔

##### 동기 스캔 (Synchronous)

```
인덱스 항목 스캔 → 즉시 힙 튜플 가져오기
```

- 비MVCC 호환 스냅샷에서 필수
- 오버헤드가 크지만 안전

##### 비동기 스캔 (Asynchronous)

```
여러 TID 수집 → 나중에 힙 방문 (비트맵 스캔)
```

- MVCC 호환 스냅샷에서만 사용 가능
- 인덱스 잠금 오버헤드 감소
- 더 효율적인 힙 접근 패턴

#### 5.5 술어 잠금 (Predicate Locking)

##### ampredlocks = false (기본값)

```sql
/* 직렬화 가능 트랜잭션에서 전체 인덱스에 비차단 술어 잠금 획득 */
/* 동시 직렬화 가능 트랜잭션의 삽입과 읽기-쓰기 충돌 발생 가능 */
/* 충돌 패턴 감지 시 일부 트랜잭션이 취소될 수 있음 */
```

##### ampredlocks = true

세밀한 술어 잠금을 구현하여 트랜잭션 취소 빈도를 감소시킴.

---

### 6. 인덱스 고유성 검사 (Index Uniqueness Checks)

#### 6.1 개요

PostgreSQL은 고유 인덱스(unique indexes) 를 사용하여 SQL 고유성 제약 조건을 강제함:

- `amcanunique=true`를 설정하는 접근 메서드만 지원 (현재 B-tree만 지원)
- `INCLUDE` 절의 컬럼은 고유성 검사 시 고려되지 않음

#### 6.2 MVCC와 고유성

MVCC 환경에서는 인덱스에 물리적으로 중복 항목이 존재할 수 있음:

```
강제되어야 할 규칙:
"어떤 MVCC 스냅샷도 동일한 인덱스 키를 가진
두 개의 라이브 행을 포함할 수 없다"
```

#### 6.3 새 행 삽입 시 확인 사항

- 현재 트랜잭션이 충돌 행 삭제: 삽입 허용 (UPDATE 시나리오)
- 미커밋 트랜잭션이 충돌 행 삽입: 해당 트랜잭션 완료 대기
- 미커밋 트랜잭션이 충돌 행 삭제: 트랜잭션 완료 후 재검사

#### 6.4 checkUnique 매개변수

`aminsert`의 `checkUnique` 매개변수 값:

##### UNIQUE_CHECK_NO

```c
/* 고유성 검사를 수행하지 않음 */
/* 고유 인덱스가 아님 */
```

##### UNIQUE_CHECK_YES

```c
/* 비연기(non-deferrable) 고유 인덱스 */
/* 즉시 고유성 검사 수행 */
/* 위반 시 즉시 에러 발생 */
```

##### UNIQUE_CHECK_PARTIAL

```c
/* 연기 가능한(deferrable) 고유 제약 조건 */
/* 각 행의 인덱스 항목을 삽입할 때 사용 */

/* 동작: */
/* - 인덱스에 중복 항목 허용 */
/* - 잠재적 중복 감지 시 false 반환 */
/* - false 반환 행에 대해 연기된 재검사 스케줄링 */

/* 특징: */
/* - 다른 트랜잭션 완료 대기 없이 확인 가능 */
/* - 오탐(false positive) 허용 */
```

##### UNIQUE_CHECK_EXISTING

```c
/* 잠재적 고유성 위반으로 보고된 행의 연기된 재검사 */
/* 주의: 새로운 인덱스 항목을 삽입하지 않음 (이미 존재) */

/* 동작: */
/* - 다른 라이브 인덱스 항목 존재 여부 확인 */
/* - 대상 행도 여전히 라이브인 경우 에러 보고 */
```

#### 6.5 예제: 연기된 고유성 검사

```sql
-- 연기 가능한 고유 제약 조건 생성
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    account_number INTEGER,
    CONSTRAINT unique_account UNIQUE (account_number) DEFERRABLE
);

-- 트랜잭션 내에서 제약 조건 연기
BEGIN;
SET CONSTRAINTS unique_account DEFERRED;

-- 일시적인 중복 허용
UPDATE accounts SET account_number = account_number + 1;

-- 커밋 시점에 고유성 검사 수행
COMMIT;
```

---

### 7. 인덱스 비용 추정 함수 (Index Cost Estimation Functions)

#### 7.1 함수 서명

```c
void amcostestimate(PlannerInfo *root,
                    IndexPath *path,
                    double loop_count,
                    Cost *indexStartupCost,
                    Cost *indexTotalCost,
                    Selectivity *indexSelectivity,
                    double *indexCorrelation,
                    double *indexPages);
```

#### 7.2 입력 매개변수

- `root`: 처리 중인 쿼리에 대한 플래너 정보
- `path`: 고려 중인 인덱스 접근 경로
- `loop_count`: 인덱스 스캔 반복 횟수 (중첩 루프 조인 시 1보다 큼)

#### 7.3 출력 매개변수 (pass-by-reference)

- `*indexStartupCost`: 인덱스 시작 처리 비용
- `*indexTotalCost`: 인덱스 처리 총 비용
- `*indexSelectivity`: 인덱스 선택도 (0~1 사이)
- `*indexCorrelation`: 인덱스 스캔 순서와 테이블 순서 간 상관계수 (-1.0~1.0)
- `*indexPages`: 인덱스 리프 페이지 수

#### 7.4 비용 계산에 사용되는 매개변수

```c
/* 시스템 비용 상수 */
seq_page_cost      /* 순차 디스크 블록 페치 비용 */
random_page_cost   /* 비순차(랜덤) 페치 비용 */
cpu_index_tuple_cost  /* 인덱스 행 처리 비용 */
cpu_operator_cost     /* 비교 연산자 비용 */
```

#### 7.5 비용 추정 절차

##### 1단계: 인덱스 선택도 추정

```c
*indexSelectivity = clauselist_selectivity(root,
                                           path->indexquals,
                                           path->indexinfo->rel->relid,
                                           JOIN_INNER,
                                           NULL);
```

##### 2단계: 방문할 인덱스 행 수 추정

```c
numIndexTuples = indexSelectivity * index->rel->tuples;
```

##### 3단계: 검색할 인덱스 페이지 수 추정

```c
numIndexPages = indexSelectivity * index->pages;
```

##### 4단계: 인덱스 접근 비용 계산

```c
cost_qual_eval(&index_qual_cost, path->indexquals, root);

*indexStartupCost = index_qual_cost.startup;

*indexTotalCost = seq_page_cost * numIndexPages +
    (cpu_index_tuple_cost + index_qual_cost.per_tuple) * numIndexTuples;
```

##### 5단계: 인덱스 상관계수 추정

```c
/* pg_statistic에서 단일 필드 순서 인덱스의 상관계수 조회 */
/* 알려지지 않은 경우: 0 (상관계수 없음) */
```

#### 7.6 비용 추정 예제

```c
/* B-tree 인덱스 비용 추정 예제 (간략화) */
void
btcostestimate(PlannerInfo *root, IndexPath *path,
               double loop_count,
               Cost *indexStartupCost, Cost *indexTotalCost,
               Selectivity *indexSelectivity,
               double *indexCorrelation, double *indexPages)
{
    IndexOptInfo *index = path->indexinfo;
    List *indexQuals = path->indexquals;
    double numIndexTuples;
    double numIndexPages;

    /* 선택도 계산 */
    *indexSelectivity = clauselist_selectivity(root, indexQuals,
                                               index->rel->relid,
                                               JOIN_INNER, NULL);

    /* 튜플 및 페이지 수 추정 */
    numIndexTuples = *indexSelectivity * index->rel->tuples;
    numIndexPages = ceil(numIndexTuples / index->tree_height);

    /* 비용 계산 */
    *indexStartupCost = 0;
    *indexTotalCost = random_page_cost * numIndexPages +
                      cpu_index_tuple_cost * numIndexTuples;

    /* 상관계수 (B-tree는 정렬되어 있으므로 높음) */
    *indexCorrelation = 1.0;  /* 또는 통계에서 조회 */

    *indexPages = numIndexPages;
}
```

#### 7.7 중요 사항

- C 언어 필수: SQL이나 절차형 언어로 작성 불가
- 비용 범위: 인덱스 자체 스캔 비용만 포함, 부모 테이블 행 검색 비용 제외
- loop_count > 1: 단일 스캔의 평균값 반환
- 예제 코드 위치: `src/backend/utils/adt/selfuncs.c`

---

### 부록: 사용자 정의 인덱스 접근 메서드 생성 예제

#### A.1 기본 구조

```c
/* 사용자 정의 인덱스 접근 메서드 핸들러 */
#include "postgres.h"
#include "access/amapi.h"
#include "access/reloptions.h"

PG_MODULE_MAGIC;

PG_FUNCTION_INFO_V1(myam_handler);

Datum
myam_handler(PG_FUNCTION_ARGS)
{
    IndexAmRoutine *amroutine = makeNode(IndexAmRoutine);

    /* 기본 속성 설정 */
    amroutine->amstrategies = 1;
    amroutine->amsupport = 1;
    amroutine->amcanorder = false;
    amroutine->amcanorderbyop = false;
    amroutine->amcanbackward = false;
    amroutine->amcanunique = false;
    amroutine->amcanmulticol = false;
    amroutine->amoptionalkey = true;
    amroutine->amsearcharray = false;
    amroutine->amsearchnulls = false;
    amroutine->amstorage = false;
    amroutine->amclusterable = false;
    amroutine->ampredlocks = false;
    amroutine->amcanparallel = false;
    amroutine->amcaninclude = false;
    amroutine->amusemaintenanceworkmem = false;
    amroutine->amsummarizing = false;

    /* 필수 함수 설정 */
    amroutine->ambuild = myam_build;
    amroutine->ambuildempty = myam_buildempty;
    amroutine->aminsert = myam_insert;
    amroutine->ambulkdelete = myam_bulkdelete;
    amroutine->amvacuumcleanup = myam_vacuumcleanup;
    amroutine->amcostestimate = myam_costestimate;
    amroutine->amoptions = myam_options;
    amroutine->amvalidate = myam_validate;
    amroutine->ambeginscan = myam_beginscan;
    amroutine->amrescan = myam_rescan;
    amroutine->amgettuple = myam_gettuple;
    amroutine->amgetbitmap = NULL;  /* 비트맵 스캔 미지원 */
    amroutine->amendscan = myam_endscan;
    amroutine->ammarkpos = NULL;
    amroutine->amrestrpos = NULL;

    PG_RETURN_POINTER(amroutine);
}
```

#### A.2 SQL 등록

```sql
-- 확장 생성
CREATE EXTENSION myam;

-- 또는 직접 등록
CREATE FUNCTION myam_handler(internal) RETURNS index_am_handler
    AS 'MODULE_PATHNAME', 'myam_handler'
    LANGUAGE C STRICT;

CREATE ACCESS METHOD myam TYPE INDEX HANDLER myam_handler;

-- 연산자 클래스 생성
CREATE OPERATOR CLASS myam_ops
    DEFAULT FOR TYPE int4 USING myam AS
    OPERATOR 1 = ,
    FUNCTION 1 myam_cmp(int4, int4);
```

#### A.3 사용 예제

```sql
-- 사용자 정의 접근 메서드로 인덱스 생성
CREATE INDEX idx_mytable ON mytable USING myam (column1);

-- 인덱스 사용 확인
EXPLAIN SELECT * FROM mytable WHERE column1 = 100;
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - Index Access Method Interface Definition](https://www.postgresql.org/docs/current/indexam.html)
- [PostgreSQL 소스 코드 - B-tree 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/nbtree)
- [PostgreSQL 소스 코드 - Hash 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/hash)
- [PostgreSQL 소스 코드 - GiST 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/gist)
- [PostgreSQL 소스 코드 - GIN 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/gin)
- [PostgreSQL 소스 코드 - BRIN 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/brin)

---

## Chapter 68: GiST 인덱스 (GiST Indexes)

### 68.1 소개 (Introduction)

GiST (Generalized Search Tree, 일반화 검색 트리)는 균형 잡힌 트리 구조의 액세스 메서드로, 다양한 인덱싱 스키마를 구현하기 위한 기본 템플릿 역할을 함. B-트리, R-트리를 비롯한 다양한 인덱싱 스키마를 GiST로 구현할 수 있음.

#### 주요 장점

GiST의 가장 큰 장점은 확장성(Extensibility) 임:

1. 도메인 전문가가 커스텀 데이터 타입 개발 가능: 적절한 액세스 메서드를 갖춘 새로운 데이터 타입을 개발할 수 있음.
2. 높은 수준의 추상화: 구현자는 데이터 타입의 의미론(semantics)만 구현하면 됨.
3. GiST 계층이 관리: 동시성(concurrency), 로깅(logging), 트리 구조 관리는 GiST 계층이 담당함.

#### GiST의 역사

GiST 인덱싱 방법은 Joseph M. Hellerstein, Jeffrey F. Naughton, Avi Pfeffer가 개발했으며, 이들의 논문 "Generalized Search Trees for Database Systems"에서 처음 발표되었음.

---

### 68.2 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 다음 표에 나열된 GiST 연산자 클래스가 포함되어 있음. `contrib` 컬렉션의 많은 애드온 모듈도 추가적인 GiST 연산자 클래스를 제공함.

#### 내장 GiST 연산자 클래스 표

- `box_ops`: `<<` `&<` `&&` `&>` `>>` `~=` `@>` `<@` `&<\|` `<<\|` `\|>>` `\|&>`
- `circle_ops`: `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `\|>>` `<<\|` `&<\|` `\|&>`
- `inet_ops`: `<<` `<<=` `>>` `>>=` `=` `<>` `<` `<=` `>` `>=` `&&`
- `point_ops`: `\|>>` `<<` `>>` `<<\|` `~=` `<@` (box/polygon/circle)
- `poly_ops`: `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|&>` `\|>>`
- `range_ops`: `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-`
- `multirange_ops`: `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-`
- `tsquery_ops`: `<@` `@>`
- `tsvector_ops`: `@@`

#### inet_ops 사용 시 주의사항

역사적인 이유로 `inet_ops` 연산자 클래스는 `inet`과 `cidr` 타입의 기본 클래스가 아님. 이를 사용하려면 명시적으로 지정해야 함:

```sql
CREATE INDEX ON my_table USING GIST (my_inet_column inet_ops);
```

#### 기하학적 연산자 설명

- `<<`: 왼쪽에 있음 (strictly left of)
- `>>`: 오른쪽에 있음 (strictly right of)
- `&<`: 오른쪽으로 확장하지 않음 (does not extend to right of)
- `&>`: 왼쪽으로 확장하지 않음 (does not extend to left of)
- `<<\|`: 아래에 있음 (strictly below)
- `\|>>`: 위에 있음 (strictly above)
- `&<\|`: 위로 확장하지 않음 (does not extend above)
- `\|&>`: 아래로 확장하지 않음 (does not extend below)
- `&&`: 겹침 (overlaps)
- `@>`: 포함함 (contains)
- `<@`: 포함됨 (contained by)
- `~=`: 동일함 (same as)

---

### 68.3 확장성 (Extensibility)

전통적으로 새로운 인덱스 액세스 메서드를 구현하는 것은 매우 어려운 작업이었음. 동시성과 로깅의 내부 동작을 이해해야 했기 때문임. GiST 인터페이스는 높은 수준의 추상화를 제공하므로, 액세스 메서드 구현자는 해당 데이터 타입의 의미론만 구현하면 됨.

GiST 연산자 클래스를 구현하려면 여러 메서드를 제공해야 함.

#### 필수 메서드 (Required Methods) - 5개

##### 1. consistent

인덱스 항목이 쿼리 조건에 부합하는지 확인함.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_consistent(internal, data_type, smallint, oid, internal)
RETURNS bool
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_consistent);

Datum
my_consistent(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    data_type  *query = PG_GETARG_DATA_TYPE_P(1);
    StrategyNumber strategy = (StrategyNumber) PG_GETARG_UINT16(2);
    /* Oid subtype = PG_GETARG_OID(3); */
    bool       *recheck = (bool *) PG_GETARG_POINTER(4);
    data_type  *key = DatumGetDataType(entry->key);
    bool        retval;

    /*
     * strategy, key, query를 기반으로 반환 값 결정
     * recheck가 true면 후보(candidate)이며 검증 필요
     * recheck가 false면 정확한 일치
     */
    *recheck = true;

    PG_RETURN_BOOL(retval);
}
```

매개변수 설명:
- `entry`: 인덱스 항목 (GISTENTRY 구조체)
- `query`: 검색할 쿼리 값
- `strategy`: 적용할 연산자를 식별하는 전략 번호 (Strategy Number)
- `subtype`: 연산자의 서브타입 OID
- `recheck`: 결과가 후보인지(true) 정확한 일치인지(false) 표시

##### 2. union

트리의 정보를 통합하여 주어진 모든 항목을 포괄하는 새 인덱스 항목을 생성함.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_union(internal, internal)
RETURNS storage_type
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_union);

Datum
my_union(PG_FUNCTION_ARGS)
{
    GistEntryVector *entryvec = (GistEntryVector *) PG_GETARG_POINTER(0);
    GISTENTRY  *ent = entryvec->vector;
    data_type  *out, *tmp, *old;
    int         numranges = entryvec->n;
    int         i;

    tmp = DatumGetDataType(ent[0].key);
    out = tmp;

    if (numranges == 1)
    {
        out = data_type_deep_copy(tmp);
        PG_RETURN_DATA_TYPE_P(out);
    }

    for (i = 1; i < numranges; i++)
    {
        old = out;
        tmp = DatumGetDataType(ent[i].key);
        out = my_union_implementation(out, tmp);
    }

    PG_RETURN_DATA_TYPE_P(out);
}
```

중요 사항:
- 반드시 새로 `palloc()`된 메모리를 반환해야 함.
- 결과는 인덱스의 저장 타입이어야 함.

##### 3. penalty

트리의 특정 분기에 항목을 삽입하는 비용을 반환함. 항목은 페널티가 가장 낮은 경로를 따라 삽입됨.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_penalty(internal, internal, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_penalty);

Datum
my_penalty(PG_FUNCTION_ARGS)
{
    GISTENTRY  *origentry = (GISTENTRY *) PG_GETARG_POINTER(0);
    GISTENTRY  *newentry = (GISTENTRY *) PG_GETARG_POINTER(1);
    float      *penalty = (float *) PG_GETARG_POINTER(2);
    data_type  *orig = DatumGetDataType(origentry->key);
    data_type  *new_val = DatumGetDataType(newentry->key);

    *penalty = my_penalty_implementation(orig, new_val);
    PG_RETURN_POINTER(penalty);
}
```

중요 사항:
- 결과는 세 번째 인수 포인터를 통해 저장됨.
- 음수 값은 0으로 처리됨.

##### 4. picksplit

페이지 분할 시 어느 항목을 기존 페이지에 남기고 어느 항목을 새 페이지로 옮길지 결정함.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_picksplit(internal, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_picksplit);

Datum
my_picksplit(PG_FUNCTION_ARGS)
{
    GistEntryVector *entryvec = (GistEntryVector *) PG_GETARG_POINTER(0);
    GIST_SPLITVEC *v = (GIST_SPLITVEC *) PG_GETARG_POINTER(1);
    OffsetNumber maxoff = entryvec->n - 1;
    GISTENTRY  *ent = entryvec->vector;
    int         i;

    /* 왼쪽/오른쪽 분할을 위한 공간 할당 */
    v->spl_left = (OffsetNumber *) palloc((maxoff + 1) * sizeof(OffsetNumber));
    v->spl_right = (OffsetNumber *) palloc((maxoff + 1) * sizeof(OffsetNumber));
    v->spl_nleft = 0;
    v->spl_nright = 0;

    /* 항목 분할 및 유니온 계산 */
    data_type *unionL = NULL;
    data_type *unionR = NULL;

    for (i = FirstOffsetNumber; i <= maxoff; i = OffsetNumberNext(i))
    {
        data_type *tmp = DatumGetDataType(ent[i].key);

        if (my_choice_is_left(unionL, unionR, tmp))
        {
            if (unionL == NULL)
                unionL = data_type_copy(tmp);
            else
                unionL = my_union_implementation(unionL, tmp);

            v->spl_left[v->spl_nleft] = i;
            v->spl_nleft++;
        }
        else
        {
            if (unionR == NULL)
                unionR = data_type_copy(tmp);
            else
                unionR = my_union_implementation(unionR, tmp);

            v->spl_right[v->spl_nright] = i;
            v->spl_nright++;
        }
    }

    v->spl_ldatum = DataTypeGetDatum(unionL);
    v->spl_rdatum = DataTypeGetDatum(unionR);
    PG_RETURN_POINTER(v);
}
```

GIST_SPLITVEC 구조체 필드:
- `spl_left`: 왼쪽 페이지에 남을 항목들의 오프셋 배열
- `spl_right`: 오른쪽 페이지로 이동할 항목들의 오프셋 배열
- `spl_nleft`: 왼쪽 항목 수
- `spl_nright`: 오른쪽 항목 수
- `spl_ldatum`: 왼쪽 항목들의 유니온
- `spl_rdatum`: 오른쪽 항목들의 유니온

##### 5. same

두 인덱스 항목이 동일하면 true를, 그렇지 않으면 false를 반환함.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_same(storage_type, storage_type, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_same);

Datum
my_same(PG_FUNCTION_ARGS)
{
    data_type  *v1 = PG_GETARG_DATA_TYPE_P(0);
    data_type  *v2 = PG_GETARG_DATA_TYPE_P(1);
    bool       *result = (bool *) PG_GETARG_POINTER(2);

    *result = my_eq(v1, v2);
    PG_RETURN_POINTER(result);
}
```

---

#### 선택적 메서드 (Optional Methods) - 7개

##### 1. compress

데이터를 인덱스 페이지의 물리적 저장에 적합한 형식으로 변환함. 생략하면 데이터가 그대로 저장됨.

```c
PG_FUNCTION_INFO_V1(my_compress);

Datum
my_compress(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    GISTENTRY  *retval;

    if (entry->leafkey)  /* 리프 노드인 경우 */
    {
        compressed_data_type *compressed = palloc(sizeof(compressed_data_type));
        /* entry->key로부터 compressed 데이터 채우기 */

        retval = palloc(sizeof(GISTENTRY));
        gistentryinit(*retval, PointerGetDatum(compressed),
                      entry->rel, entry->page, entry->offset, FALSE);
    }
    else
    {
        retval = entry;  /* 비리프 항목은 일반적으로 변경 없음 */
    }

    PG_RETURN_POINTER(retval);
}
```

##### 2. decompress

저장된 표현을 GiST 메서드가 처리할 수 있는 형식으로 복원함.

```c
PG_FUNCTION_INFO_V1(my_decompress);

Datum
my_decompress(PG_FUNCTION_ARGS)
{
    /* 압축 해제가 필요 없으면 no-op */
    PG_RETURN_POINTER(PG_GETARG_POINTER(0));
}
```

##### 3. distance

인덱스 항목과 쿼리 값 사이의 거리를 계산함. 연산자 클래스에 정렬 연산자가 있는 경우 필수임.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_distance(internal, data_type, smallint, oid, internal)
RETURNS float8
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_distance);

Datum
my_distance(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    data_type  *query = PG_GETARG_DATA_TYPE_P(1);
    StrategyNumber strategy = (StrategyNumber) PG_GETARG_UINT16(2);
    /* Oid subtype = PG_GETARG_OID(3); */
    bool       *recheck = (bool *) PG_GETARG_POINTER(4);
    data_type  *key = DatumGetDataType(entry->key);
    double      retval;

    /* strategy, key, query를 기반으로 거리 계산 */
    retval = calculate_distance(key, query, strategy);

    /* 근사치인 경우 recheck 설정 */
    *recheck = false;

    PG_RETURN_FLOAT8(retval);
}
```

중요 사항:
- 내부 노드의 경우, 반환된 거리는 어떤 자식까지의 거리보다 크면 안 됨.
- 근사치인 경우 `*recheck = true`로 설정함.

##### 4. fetch

인덱스 전용 스캔(Index-Only Scan)을 지원하기 위해 압축된 인덱스 표현을 원래 데이터 타입으로 변환함.

```c
PG_FUNCTION_INFO_V1(my_fetch);

Datum
my_fetch(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    input_data_type *in = DatumGetPointer(entry->key);
    fetched_data_type *fetched;
    GISTENTRY  *retval;

    retval = palloc(sizeof(GISTENTRY));
    fetched = palloc(sizeof(fetched_data_type));

    /* 저장된 데이터를 원래 데이터 타입의 Datum으로 변환 */
    /* ... 변환 로직 ... */

    gistentryinit(*retval, PointerGetDatum(fetched),
                  entry->rel, entry->page, entry->offset, FALSE);

    PG_RETURN_POINTER(retval);
}
```

참고: `compress`가 리프 항목에 대해 손실 없이 동작하는 경우에만 필요함.

##### 5. options

연산자 클래스의 동작을 제어하는 사용자 정의 매개변수를 선언함.

```c
typedef struct
{
    int32   vl_len_;       /* varlena 헤더 */
    int     int_param;     /* 정수 매개변수 */
    double  real_param;    /* 실수 매개변수 */
} MyOptionsStruct;

PG_FUNCTION_INFO_V1(my_options);

Datum
my_options(PG_FUNCTION_ARGS)
{
    local_relopts *relopts = (local_relopts *) PG_GETARG_POINTER(0);

    init_local_reloptions(relopts, sizeof(MyOptionsStruct));
    add_local_int_reloption(relopts, "int_param", "정수 매개변수 설명",
                            100, 0, 1000000,
                            offsetof(MyOptionsStruct, int_param));
    add_local_real_reloption(relopts, "real_param", "실수 매개변수 설명",
                             1.0, 0.0, 1000000.0,
                             offsetof(MyOptionsStruct, real_param));

    PG_RETURN_VOID();
}
```

다른 함수에서 옵션 접근:
```c
if (PG_HAS_OPCLASS_OPTIONS())
{
    MyOptionsStruct *options = (MyOptionsStruct *) PG_GET_OPCLASS_OPTIONS();
    int_param = options->int_param;
    real_param = options->real_param;
}
```

##### 6. sortsupport

지역성(locality)을 보존하는 방식으로 데이터를 정렬하는 비교 함수를 제공함. 인덱스 빌드 속도를 높이기 위해 사용됨.

```c
PG_FUNCTION_INFO_V1(my_sortsupport);

static int
my_fastcmp(Datum x, Datum y, SortSupport ssup)
{
    /* 공간 코드(예: Z-order, Hilbert curve) 계산 */
    int z1 = ComputeSpatialCode(x);
    int z2 = ComputeSpatialCode(y);

    if (z1 == z2)
        return 0;
    return (z1 > z2) ? 1 : -1;
}

Datum
my_sortsupport(PG_FUNCTION_ARGS)
{
    SortSupport ssup = (SortSupport) PG_GETARG_POINTER(0);
    ssup->comparator = my_fastcmp;
    PG_RETURN_VOID();
}
```

##### 7. translate_cmptype

`CompareType` 값을 전략 번호(strategy numbers)로 변환함. 시간적(temporal) 인덱스 제약 조건에 사용됨.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_translate_cmptype(integer)
RETURNS smallint
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_translate_cmptype);

Datum
my_translate_cmptype(PG_FUNCTION_ARGS)
{
    CompareType cmptype = PG_GETARG_INT32(0);
    StrategyNumber ret = InvalidStrategy;

    switch (cmptype)
    {
        case COMPARE_EQ:
            ret = BTEqualStrategyNumber;
            break;
        case COMPARE_LT:
            ret = BTLessStrategyNumber;
            break;
        /* ... 기타 케이스 ... */
    }

    PG_RETURN_UINT16(ret);
}
```

연산자 패밀리에 등록:
```sql
ALTER OPERATOR FAMILY my_opfamily USING gist ADD
    FUNCTION 12 ("any", "any") my_translate_cmptype(int);
```

---

### 68.4 구현 상세 (Implementation)

#### GiST 인덱스 빌드 방법

GiST 인덱스 구축에는 여러 가지 방법이 있으며, 상황에 따라 각각의 장단점이 있음.

##### 1. 정렬 방법 (Sorted Method) - 기본값

- 조건: 모든 연산자 클래스가 `sortsupport` 함수를 제공할 때 사용됨.
- 장점: 대부분의 데이터셋에 가장 효율적임.
- 동작: 데이터를 먼저 정렬한 후 트리를 bottom-up 방식으로 구축함.

```sql
-- sortsupport가 있는 경우 자동으로 정렬 방법 사용
CREATE INDEX idx_point ON locations USING GIST (point_column);
```

##### 2. 버퍼링 방법 (Buffered Method)

- 장점: 정렬되지 않은 데이터셋에 대해 무작위 I/O를 크게 줄임.
- 단점: I/O 감소를 위해 CPU를 더 사용하며, 인덱스 크기만큼의 임시 디스크 공간이 필요함.
- 자동 활성화: 인덱스 크기가 `effective_cache_size`에 도달하면 자동으로 전환됨.

```sql
-- 버퍼링 빌드 명시적 활성화
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=on);

-- 버퍼링 빌드 비활성화
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=off);

-- 자동 결정 (기본값)
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=auto);
```

#### 트리 구조

GiST 인덱스는 B-트리와 유사한 균형 트리 구조를 가짐:

```
         [루트 노드]
        /    |    \
   [내부]  [내부]  [내부]
   / | \   / | \   / | \
[리프] ... [리프] ... [리프]
```

- 내부 노드 (Internal Nodes): 하위 항목들의 "바운딩 박스" 또는 유니온을 저장함.
- 리프 노드 (Leaf Nodes): 실제 인덱스 키를 저장함.
- 균형 유지: 모든 리프 노드는 루트에서 동일한 깊이에 위치함.

#### 동시성 제어

GiST는 동시 접근을 위한 정교한 잠금 메커니즘을 사용함:

1. 읽기 작업: 페이지 단위 공유 잠금 사용
2. 쓰기 작업: 페이지 단위 배타 잠금 사용
3. 트리 수정: 필요한 경우에만 부모 노드 잠금

---

### 68.5 예제 (Examples)

#### 기본 GiST 인덱스 생성

```sql
-- 기하학적 데이터에 대한 GiST 인덱스
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    position POINT
);

CREATE INDEX idx_locations_pos ON locations USING GIST (position);

-- 인덱스를 사용한 쿼리
SELECT * FROM locations
WHERE position <@ BOX '((0,0),(10,10))';  -- 박스 내의 점 검색
```

#### 범위 타입에 대한 GiST 인덱스

```sql
-- 범위 타입 테이블
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    room_id INTEGER,
    during TSRANGE
);

CREATE INDEX idx_reservations_during ON reservations USING GIST (during);

-- 겹치는 예약 검색
SELECT * FROM reservations
WHERE during && '[2024-01-01, 2024-01-07)'::tsrange;

-- 제외 제약 조건 (같은 방에 겹치는 예약 방지)
ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_reservations
EXCLUDE USING GIST (room_id WITH =, during WITH &&);
```

#### 전체 텍스트 검색에 대한 GiST 인덱스

```sql
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    body TEXT,
    tsv TSVECTOR
);

-- tsvector 컬럼에 GiST 인덱스 생성
CREATE INDEX idx_documents_tsv ON documents USING GIST (tsv);

-- 전체 텍스트 검색 쿼리
SELECT * FROM documents
WHERE tsv @@ to_tsquery('postgresql & index');
```

#### 네트워크 주소에 대한 GiST 인덱스

```sql
CREATE TABLE network_blocks (
    id SERIAL PRIMARY KEY,
    network INET,
    description TEXT
);

-- inet_ops 명시적 지정 필요
CREATE INDEX idx_network ON network_blocks USING GIST (network inet_ops);

-- 서브넷 포함 검색
SELECT * FROM network_blocks
WHERE network >>= '192.168.1.0/24';
```

---

### 68.6 contrib 모듈의 GiST 지원

PostgreSQL `contrib` 컬렉션에는 GiST를 활용하는 여러 유용한 모듈이 있음:

- `btree_gist`
  - 용도: B-트리 동등 기능을 GiST로 제공
  - 인덱싱 대상: 스칼라 타입 (integer, text 등)
- `cube`
  - 용도: 다차원 큐브 인덱싱
  - 인덱싱 대상: N차원 큐브
- `hstore`
  - 용도: 키-값 쌍 저장 및 검색
  - 인덱싱 대상: 키-값 데이터
- `intarray`
  - 용도: 정수 배열의 RD-Tree
  - 인덱싱 대상: 1차원 int4 배열
- `ltree`
  - 용도: 트리 구조 레이블 경로
  - 인덱싱 대상: 계층적 레이블
- `pg_trgm`
  - 용도: 트라이그램 기반 텍스트 유사도
  - 인덱싱 대상: 텍스트 유사도 검색
- `seg`
  - 용도: 부동소수점 범위 인덱싱
  - 인덱싱 대상: 숫자 범위

#### btree_gist 예제

`btree_gist`를 사용하면 스칼라 타입에 대해 GiST 인덱스를 생성하고 제외 제약 조건에서 사용할 수 있음:

```sql
-- 확장 설치
CREATE EXTENSION btree_gist;

-- 회의실 예약 테이블
CREATE TABLE meetings (
    id SERIAL PRIMARY KEY,
    room_id INTEGER,
    during TSRANGE
);

-- 같은 방에 겹치는 회의 방지
ALTER TABLE meetings
ADD CONSTRAINT no_overlapping_meetings
EXCLUDE USING GIST (room_id WITH =, during WITH &&);
```

#### pg_trgm 예제

```sql
-- 확장 설치
CREATE EXTENSION pg_trgm;

-- 트라이그램 GiST 인덱스 생성
CREATE INDEX idx_trgm ON documents USING GIST (title gist_trgm_ops);

-- 유사도 검색
SELECT * FROM documents
WHERE title % 'PostgreSQL'  -- 유사한 제목 검색
ORDER BY title <-> 'PostgreSQL';  -- 유사도 순 정렬
```

---

### 68.7 메모리 관리 (Memory Management)

GiST 지원 메서드는 일반적으로 각 튜플 처리 후 리셋되는 단기 메모리 컨텍스트 내에서 실행됨. 호출 간에 데이터를 캐시하려면 다음과 같이 함:

```c
/* 더 오래 지속되는 컨텍스트에 할당 */
if (fcinfo->flinfo->fn_extra == NULL)
{
    MemoryContext oldcxt;

    oldcxt = MemoryContextSwitchTo(fcinfo->flinfo->fn_mcxt);
    fcinfo->flinfo->fn_extra = palloc(sizeof(CachedData));
    MemoryContextSwitchTo(oldcxt);

    /* 캐시 데이터 초기화 */
    initialize_cache((CachedData *) fcinfo->flinfo->fn_extra);
}

/* 데이터는 인덱스 작업 기간 동안 유지됨 */
CachedData *cached = (CachedData *) fcinfo->flinfo->fn_extra;

/* fn_extra를 교체할 때는 이전 값을 pfree()하여 메모리 누수 방지 */
```

---

### 68.8 GiST vs 다른 인덱스 타입

- 용도
  - GiST: 범용 확장 가능
  - B-tree: 비교 가능한 데이터
  - GIN: 복합 값 검색
  - SP-GiST: 불균형 구조
- 기하학적 데이터
  - GiST: 우수
  - B-tree: 미지원
  - GIN: 미지원
  - SP-GiST: 지원
- 범위 쿼리
  - GiST: 우수
  - B-tree: 우수
  - GIN: 제한적
  - SP-GiST: 지원
- 전체 텍스트
  - GiST: 지원
  - B-tree: 미지원
  - GIN: 우수
  - SP-GiST: 미지원
- 최근접 이웃
  - GiST: 지원
  - B-tree: 미지원
  - GIN: 미지원
  - SP-GiST: 지원
- 인덱스 크기
  - GiST: 중간
  - B-tree: 작음
  - GIN: 큼
  - SP-GiST: 작음
- 빌드 속도
  - GiST: 보통
  - B-tree: 빠름
  - GIN: 느림
  - SP-GiST: 보통

---

### 68.9 성능 고려사항 (Performance Considerations)

#### 인덱스 선택 시 고려사항

1. 데이터 타입: 기하학적 데이터, 범위, 전체 텍스트에는 GiST가 적합함.
2. 쿼리 패턴: 포함, 겹침, 최근접 이웃 쿼리에 효과적임.
3. 업데이트 빈도: 업데이트가 잦을 경우 B-tree보다 느릴 수 있음.

#### 성능 최적화 팁

```sql
-- 인덱스 통계 확인
SELECT * FROM pg_stat_user_indexes
WHERE indexrelname = 'idx_locations_pos';

-- 인덱스 크기 확인
SELECT pg_size_pretty(pg_relation_size('idx_locations_pos'));

-- EXPLAIN ANALYZE로 인덱스 사용 확인
EXPLAIN ANALYZE
SELECT * FROM locations
WHERE position <@ BOX '((0,0),(10,10))';

-- 인덱스 재구축 (필요시)
REINDEX INDEX idx_locations_pos;
```

#### fillfactor 설정

업데이트가 잦은 테이블에서는 fillfactor를 낮게 설정해 페이지 분할을 줄일 수 있음:

```sql
CREATE INDEX idx_geo ON geo_data USING GIST (geom)
WITH (fillfactor = 90);
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - GiST Indexes](https://www.postgresql.org/docs/current/gist.html)
- [GiST Development Guide](https://www.postgresql.org/docs/current/gist-extensibility.html)
- Hellerstein, J.M., Naughton, J.F., Pfeffer, A. "Generalized Search Trees for Database Systems"

---

## Chapter 69. SP-GiST 인덱스 (SP-GiST Indexes)

PostgreSQL 18 공식 문서 번역

---

### 목차

- [69.1. 소개](#691-소개-introduction)
- [69.2. 내장 연산자 클래스](#692-내장-연산자-클래스-built-in-operator-classes)
- [69.3. 확장성](#693-확장성-extensibility)
- [69.4. 구현](#694-구현-implementation)

---

### 69.1. 소개 (Introduction)

SP-GiST는 Space-Partitioned GiST 의 약자로, 분할 검색 트리(partitioned search trees)를 지원하는 인덱스 접근 방법임. SP-GiST를 사용하면 다음과 같은 다양한 비균형(non-balanced) 디스크 기반 데이터 구조를 구현할 수 있음:

- 쿼드 트리 (Quad-trees): 2차원 공간을 4개의 사분면으로 재귀적으로 분할
- k-d 트리 (K-d trees): k차원 점 데이터를 이진 분할
- 기수 트리 (Radix trees, Tries): 문자열의 공통 접두사를 기반으로 분할

#### SP-GiST의 특징

SP-GiST는 검색 공간을 동일하지 않은 크기의 파티션으로 반복적으로 분할 하는 구조를 지원함. 쿼리가 분할 규칙과 잘 일치하면 매우 빠른 검색이 가능함.

전통적인 메모리 기반 검색 트리의 주요 과제는 노드를 디스크 페이지에 효율적으로 매핑하는 것임. SP-GiST는 이를 해결하기 위해 설계되었음:

- 포인터 기반 구조 대신 디스크 페이지에 최적화된 구조 사용
- 높은 팬아웃(fanout)으로 I/O 연산 최소화
- 많은 노드를 순회하더라도 소수의 디스크 페이지만 접근

#### SP-GiST 트리의 구조

SP-GiST 트리는 두 가지 유형의 튜플로 구성됨:

##### 내부 튜플 (Inner Tuples)

검색 트리의 분기점(branch points)으로, 하나 이상의 노드(nodes) 를 포함함:

```
내부 튜플 구조:
┌─────────────────────────────────────────────────┐
│ [선택적 접두사] + [노드1] + [노드2] + ... + [노드n] │
└─────────────────────────────────────────────────┘
```

각 노드는 다음을 포함함:
- 레이블 (Label): 노드를 설명 (예: 기수 트리에서 다음 문자)
- 다운링크 (Downlink): 하위 내부 튜플이나 리프 튜플 목록을 가리킴
- 접두사 (Prefix): 선택적으로 모든 멤버에 공통적인 값 설명

##### 리프 튜플 (Leaf Tuples)

인덱싱된 컬럼의 실제 값을 포함함:

- 인덱싱된 컬럼과 동일한 데이터 타입의 값 저장
- 손실 표현(lossy representation)이나 부분 값 저장 가능
- INCLUDE 컬럼 값 저장 가능 (연산자 클래스에 투명하게 처리)

#### 쿼드 트리 예제

점(point) 데이터를 저장하는 쿼드 트리의 구조:

```
                    ┌─────────────────┐
                    │   내부 튜플      │
                    │ (중심점: 5,5)   │
                    └────────┬────────┘
           ┌─────────┬───────┴───────┬─────────┐
           │         │               │         │
        ┌──▼──┐   ┌──▼──┐        ┌──▼──┐   ┌──▼──┐
        │ NE  │   │ NW  │        │ SE  │   │ SW  │
        │사분면│   │사분면│        │사분면│   │사분면│
        └─────┘   └─────┘        └─────┘   └─────┘
```

#### 기수 트리 (Radix Tree / Trie) 예제

문자열 데이터를 저장하는 기수 트리:

```
                        ┌─────┐
                        │ 루트│
                        └──┬──┘
              ┌────────────┼────────────┐
              │            │            │
           ┌──▼──┐      ┌──▼──┐      ┌──▼──┐
           │ 'h' │      │ 'w' │      │ 'p' │
           └──┬──┘      └──┬──┘      └──┬──┘
              │            │            │
           ┌──▼───┐     ┌──▼───┐     ┌──▼───┐
           │'ello'│     │'orld'│     │'ost' │
           │      │     │      │     │      │
           │hello │     │world │     │post  │
           └──────┘     └──────┘     └──────┘
```

---

### 69.2. 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 다음 표에 나열된 SP-GiST 연산자 클래스가 포함되어 있음.

#### SP-GiST 내장 연산자 클래스 목록

- `box_ops`
  - 인덱싱 타입: `box`
  - 인덱싱 가능 연산자: `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|&>` `\|>>`
- `inet_ops`
  - 인덱싱 타입: `inet`, `cidr`
  - 인덱싱 가능 연산자: `<<` `<<=` `>>` `>>=` `=` `<>` `<` `<=` `>` `>=` `&&`
- `kd_point_ops`
  - 인덱싱 타입: `point`
  - 인덱싱 가능 연산자: `\|>>` `<->` `<<` `>>` `<<\|` `~=` `<@`
- `quad_point_ops`
  - 인덱싱 타입: `point`
  - 인덱싱 가능 연산자: `\|>>` `<->` `<<` `>>` `<<\|` `~=` `<@`
- `poly_ops`
  - 인덱싱 타입: `polygon`
  - 인덱싱 가능 연산자: `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|>>` `\|&>`
- `range_ops`
  - 인덱싱 타입: 모든 범위 타입
  - 인덱싱 가능 연산자: `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-`
- `text_ops`
  - 인덱싱 타입: `text`
  - 인덱싱 가능 연산자: `=` `<` `<=` `>` `>=` `~<~` `~<=~` `~>=~` `~>~` `^@`

#### 연산자 클래스 상세 설명

##### box_ops (상자 연산자 클래스)

`box` 타입에 대한 SP-GiST 인덱스임. 쿼드 트리를 사용하여 2차원 상자를 인덱싱함.

지원 연산자:
- `<<`: 왼쪽에 엄격히 위치 (strictly left of)
- `&<`: 오른쪽으로 확장되지 않음 (does not extend to the right of)
- `&>`: 왼쪽으로 확장되지 않음 (does not extend to the left of)
- `>>`: 오른쪽에 엄격히 위치 (strictly right of)
- `<@`: 포함됨 (contained by)
- `@>`: 포함 (contains)
- `~=`: 동일 (same as)
- `&&`: 겹침 (overlaps)
- `<<|`: 아래에 엄격히 위치 (strictly below)
- `&<|`: 위로 확장되지 않음 (does not extend above)
- `|&>`: 아래로 확장되지 않음 (does not extend below)
- `|>>`: 위에 엄격히 위치 (strictly above)

##### inet_ops (IP 주소 연산자 클래스)

`inet` 및 `cidr` 타입에 대한 SP-GiST 인덱스임. 기수 트리를 사용함.

지원 연산자:
- `<<`: 서브넷에 포함됨 (is subnet)
- `<<=`: 서브넷이거나 같음 (is subnet or equal)
- `>>`: 슈퍼넷에 포함됨 (is supernet)
- `>>=`: 슈퍼넷이거나 같음 (is supernet or equal)
- `=`, `<>`, `<`, `<=`, `>`, `>=`: 비교 연산자
- `&&`: 겹침 (overlaps)

##### quad_point_ops (쿼드 포인트 연산자 클래스)

`point` 타입의 기본 연산자 클래스임. 쿼드 트리를 사용하여 2차원 점을 인덱싱함.

지원 연산자:
- `|>>`: 위에 엄격히 위치
- `<->`: 거리 (k-NN 검색 지원)
- `<<`: 왼쪽에 엄격히 위치
- `>>`: 오른쪽에 엄격히 위치
- `<<|`: 아래에 엄격히 위치
- `~=`: 동일
- `<@`: 상자/다각형에 포함됨

##### kd_point_ops (k-d 포인트 연산자 클래스)

`point` 타입에 대한 대안적 연산자 클래스임. k-d 트리를 사용함.

quad_point_ops와의 차이점:
- 균형 잡힌 트리 구조로 더 나은 균형 제공
- 특정 데이터 분포에서 더 효율적일 수 있음
- 두 클래스 모두 k-NN(k-Nearest Neighbor) 검색 지원

```sql
-- quad_point_ops 사용 (기본값)
CREATE INDEX idx_points_quad ON geo_table USING spgist (location);

-- kd_point_ops 명시적 사용
CREATE INDEX idx_points_kd ON geo_table USING spgist (location kd_point_ops);
```

##### range_ops (범위 연산자 클래스)

모든 범위 타입에 대한 SP-GiST 인덱스임.

지원 연산자:
- `=`: 같음
- `&&`: 겹침
- `@>`: 포함
- `<@`: 포함됨
- `<<`: 왼쪽에 엄격히 위치
- `>>`: 오른쪽에 엄격히 위치
- `&<`: 오른쪽으로 확장되지 않음
- `&>`: 왼쪽으로 확장되지 않음
- `-|-`: 인접 (adjacent to)

##### text_ops (텍스트 연산자 클래스)

`text` 타입에 대한 SP-GiST 인덱스임. 기수 트리(Trie)를 사용함.

지원 연산자:
- `=`: 같음
- `<`, `<=`, `>`, `>=`: 비교 연산자
- `~<~`, `~<=~`, `~>=~`, `~>~`: 로케일 비의존적 비교
- `^@`: 접두사 검색 (starts with)

#### 사용 예제

```sql
-- 포인트 데이터 인덱싱
CREATE TABLE locations (
    id serial PRIMARY KEY,
    name text,
    position point
);

-- SP-GiST 인덱스 생성
CREATE INDEX idx_location_position ON locations USING spgist (position);

-- k-NN 검색: 가장 가까운 5개 위치 찾기
SELECT name, position, position <-> point(3.5, 2.7) AS distance
FROM locations
ORDER BY position <-> point(3.5, 2.7)
LIMIT 5;

-- 영역 내 점 검색
SELECT name, position
FROM locations
WHERE position <@ box(point(0,0), point(10,10));
```

```sql
-- IP 주소 인덱싱
CREATE TABLE network_hosts (
    id serial PRIMARY KEY,
    hostname text,
    ip_address inet
);

CREATE INDEX idx_ip ON network_hosts USING spgist (ip_address inet_ops);

-- 특정 서브넷 내 호스트 찾기
SELECT hostname, ip_address
FROM network_hosts
WHERE ip_address << '192.168.1.0/24';
```

```sql
-- 텍스트 접두사 검색
CREATE TABLE products (
    id serial PRIMARY KEY,
    name text,
    description text
);

CREATE INDEX idx_product_name ON products USING spgist (name text_ops);

-- 접두사 검색
SELECT name FROM products WHERE name ^@ 'App';

-- 이는 LIKE 'App%' 쿼리보다 효율적입니다
SELECT name FROM products WHERE name LIKE 'App%';
```

---

### 69.3. 확장성 (Extensibility)

SP-GiST는 다양한 유형의 비균형 디스크 기반 데이터 구조를 구현할 수 있도록 확장 가능한 인터페이스를 제공함. 분할 규칙 및 동등성에 대한 높은 수준의 추상화를 제공하며, 데이터를 내부 튜플 간에 이동하거나 값을 압축하는 등의 일반적인 작업은 SP-GiST 코어에서 처리함.

#### 필수 사용자 정의 메서드

모든 SP-GiST 연산자 클래스는 5개의 필수 메서드와 선택적 메서드를 구현해야 함:

##### 1. config 메서드

인덱스 구현에 대한 정적 정보를 반환함.

```c
/* 입력 구조체 */
typedef struct spgConfigIn
{
    Oid         attType;        /* 인덱싱된 컬럼의 데이터 타입 */
} spgConfigIn;

/* 출력 구조체 */
typedef struct spgConfigOut
{
    Oid         prefixType;     /* 내부 튜플 접두사의 데이터 타입 */
    Oid         labelType;      /* 내부 튜플 노드 레이블의 데이터 타입 */
    Oid         leafType;       /* 리프 튜플 값의 데이터 타입 */
    bool        canReturnData;  /* 원본 데이터 재구성 가능 여부 */
    bool        longValuesOK;   /* 1페이지 초과 값 처리 가능 여부 */
} spgConfigOut;
```

설명:
- `prefixType`: 내부 튜플에 저장되는 접두사의 데이터 타입 (필수 아님)
- `labelType`: 노드 레이블의 데이터 타입 (필수 아님)
- `leafType`: 리프 튜플에 저장되는 값의 데이터 타입 (기본값: `attType`)
- `canReturnData`: true이면 인덱스 전용 스캔 지원
- `longValuesOK`: true이면 긴 값에 대한 접두사 제거 지원

##### 2. choose 메서드

새 값을 내부 튜플에 삽입할 방법을 결정함.

```c
/* 입력 구조체 */
typedef struct spgChooseIn
{
    Datum       datum;          /* 삽입할 원본 값 */
    Datum       leafDatum;      /* 리프 튜플에 저장될 현재 값 */
    int         level;          /* 트리에서 현재 레벨 */

    /* 내부 튜플 정보 */
    bool        allTheSame;     /* 모든 노드가 동등한지 */
    bool        hasPrefix;      /* 접두사 있음? */
    Datum       prefixDatum;    /* 접두사 값 */
    int         nNodes;         /* 노드 수 */
    Datum      *nodeLabels;     /* 노드 레이블 배열 (NULL 가능) */
} spgChooseIn;

/* 결과 유형 */
typedef enum spgChooseResultType
{
    spgMatchNode = 1,           /* 기존 노드로 하강 */
    spgAddNode,                 /* 내부 튜플에 노드 추가 */
    spgSplitTuple               /* 내부 튜플 분할 (접두사 변경) */
} spgChooseResultType;

/* 출력 구조체 */
typedef struct spgChooseOut
{
    spgChooseResultType resultType;

    union
    {
        struct                  /* spgMatchNode 결과 */
        {
            int         nodeN;          /* 하강할 노드 인덱스 */
            int         levelAdd;       /* 레벨 증가량 */
            Datum       restDatum;      /* 하위 레벨에 전달할 값 */
        } matchNode;

        struct                  /* spgAddNode 결과 */
        {
            Datum       nodeLabel;      /* 새 노드의 레이블 */
            int         nodeN;          /* 새 노드 삽입 위치 */
        } addNode;

        struct                  /* spgSplitTuple 결과 */
        {
            bool        prefixHasPrefix;    /* 새 상위 튜플에 접두사? */
            Datum       prefixPrefixDatum;  /* 새 접두사 */
            int         prefixNNodes;       /* 상위 튜플 노드 수 */
            Datum      *prefixNodeLabels;   /* 상위 튜플 노드 레이블 */
            int         childNodeN;         /* 하위 튜플 연결 노드 */

            bool        postfixHasPrefix;   /* 새 하위 튜플에 접두사? */
            Datum       postfixPrefixDatum; /* 하위 튜플 접두사 */
        } splitTuple;
    } result;
} spgChooseOut;
```

결과 유형 설명:
- spgMatchNode: 기존 노드를 따라 하강. `nodeN`으로 노드 지정
- spgAddNode: 새 자식 노드 추가. `nodeLabel`과 위치 지정
- spgSplitTuple: 내부 튜플 분할. 더 짧은 접두사로 새 상위 튜플 생성

##### 3. picksplit 메서드

리프 튜플 집합에서 새 내부 튜플을 생성하는 방법을 결정함.

```c
/* 입력 구조체 */
typedef struct spgPickSplitIn
{
    int         nTuples;        /* 분할할 리프 튜플 수 */
    Datum      *datums;         /* 리프 튜플 값 배열 */
    int         level;          /* 트리에서 현재 레벨 */
} spgPickSplitIn;

/* 출력 구조체 */
typedef struct spgPickSplitOut
{
    bool        hasPrefix;          /* 새 내부 튜플에 접두사? */
    Datum       prefixDatum;        /* 접두사 값 (hasPrefix = true인 경우) */

    int         nNodes;             /* 생성할 노드 수 */
    Datum      *nodeLabels;         /* 노드 레이블 배열 (NULL 가능) */

    int        *mapTuplesToNodes;   /* 각 리프 튜플의 노드 할당 */
    Datum      *leafTupleDatums;    /* 각 새 리프 튜플의 값 */
} spgPickSplitOut;
```

중요 요구사항:
- 여러 리프 튜플을 2개 이상의 노드로 분류해야 함
- 모든 튜플이 단일 노드에 할당되면 SP-GiST 코어가 해당 결과를 무시함

##### 4. inner_consistent 메서드

트리 검색 시 탐색할 노드(분기)를 반환함.

```c
/* 입력 구조체 */
typedef struct spgInnerConsistentIn
{
    ScanKey     scankeys;           /* 스캔 키 배열 */
    ScanKey     orderbys;           /* ORDER BY 키 배열 (k-NN 검색) */
    int         nkeys;              /* 스캔 키 수 */
    int         norderbys;          /* ORDER BY 키 수 */

    Datum       reconstructedValue; /* 재구성된 상위 값 */
    void       *traversalValue;     /* 연산자 클래스 전용 순회 값 */
    MemoryContext traversalMemoryContext;
    int         level;              /* 트리에서 현재 레벨 */
    bool        returnData;         /* 원본 데이터 반환 필요? */

    /* 내부 튜플 정보 */
    bool        allTheSame;         /* 모든 노드가 동등? */
    bool        hasPrefix;          /* 접두사 있음? */
    Datum       prefixDatum;        /* 접두사 값 */
    int         nNodes;             /* 노드 수 */
    Datum      *nodeLabels;         /* 노드 레이블 배열 */
} spgInnerConsistentIn;

/* 출력 구조체 */
typedef struct spgInnerConsistentOut
{
    int         nNodes;                 /* 방문할 노드 수 */
    int        *nodeNumbers;            /* 방문할 노드 인덱스 */
    int        *levelAdds;              /* 각 노드별 레벨 증가량 */
    Datum      *reconstructedValues;    /* 재구성된 값 (NULL 가능) */
    void     **traversalValues;        /* 연산자 클래스 전용 순회 값 */
    double   **distances;              /* k-NN 검색용 거리 (NULL 가능) */
} spgInnerConsistentOut;
```

##### 5. leaf_consistent 메서드

리프 튜플이 쿼리를 만족하는지 확인함.

```c
/* 입력 구조체 */
typedef struct spgLeafConsistentIn
{
    ScanKey     scankeys;           /* 스캔 키 배열 */
    ScanKey     orderbys;           /* ORDER BY 키 배열 */
    int         nkeys;              /* 스캔 키 수 */
    int         norderbys;          /* ORDER BY 키 수 */

    Datum       reconstructedValue; /* 재구성된 값 */
    void       *traversalValue;     /* 순회 값 */
    int         level;              /* 트리에서 현재 레벨 */
    bool        returnData;         /* 원본 데이터 반환 필요? */

    Datum       leafDatum;          /* 리프 튜플 값 */
} spgLeafConsistentIn;

/* 출력 구조체 */
typedef struct spgLeafConsistentOut
{
    Datum       leafValue;          /* 재구성된 원본 데이터 */
    bool        recheck;            /* 연산자 재확인 필요? */
    bool        recheckDistances;   /* 거리 재확인 필요? */
    double     *distances;          /* k-NN 검색용 거리 */
} spgLeafConsistentOut;
```

#### 선택적 메서드

##### compress 메서드

데이터 항목을 리프 튜플 저장에 적합한 형식으로 변환함.

```c
Datum compress(Datum in)
```

특징:
- 입력: `spgConfigIn.attType` 타입
- 출력: `spgConfigOut.leafType` 타입
- 출력에 외부 라인 TOAST 포인터 포함 불가

##### options 메서드

연산자 클래스 동작을 제어하는 사용자 정의 매개변수를 정의함.

```sql
CREATE OR REPLACE FUNCTION my_spgist_options(internal)
RETURNS void
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

#### 연산자 클래스 구현 예제

다음은 간단한 정수 범위에 대한 SP-GiST 연산자 클래스 개념을 보여주는 예제임:

```sql
-- 연산자 클래스 생성 예제 (개념적)
CREATE OPERATOR CLASS my_int_ops
    FOR TYPE int4 USING spgist AS
    OPERATOR    1   =  (int4, int4),
    OPERATOR    2   <  (int4, int4),
    OPERATOR    3   <= (int4, int4),
    OPERATOR    4   >= (int4, int4),
    OPERATOR    5   >  (int4, int4),
    FUNCTION    1   my_config(internal, internal),
    FUNCTION    2   my_choose(internal, internal),
    FUNCTION    3   my_picksplit(internal, internal),
    FUNCTION    4   my_inner_consistent(internal, internal),
    FUNCTION    5   my_leaf_consistent(internal, internal);
```

---

### 69.4. 구현 (Implementation)

#### SP-GiST 제한 사항

##### 페이지 크기 제약

1. 개별 리프 및 내부 튜플 크기: 단일 인덱스 페이지에 맞아야 함 (기본값 8kB)
2. 긴 값 처리: `longValuesOK = true`인 경우에만 1페이지 초과 값 지원 가능
   - 연속적인 접두사 제거(suffix stripping)를 통해 구현
   - 예: 기수 트리에서 긴 문자열을 접두사와 나머지로 분할

##### 리프 그룹화 제약

하나의 노드가 가리키는 모든 리프 튜플은 동일한 인덱스 페이지에 있어야 함. 이는 SP-GiST 코어가 자동으로 처리함.

##### 무한 루프 방지

SP-GiST는 리프 데이텀이 10회의 `choose` 호출 내에 축소되지 않으면 오류를 발생시킴.

#### 노드 레이블 없는 SP-GiST

일부 트리 알고리즘은 고정된 노드 집합을 사용함. 예를 들어, 쿼드 트리는 항상 정확히 4개의 노드를 가짐:

```
         NW | NE
        ----+----
         SW | SE
```

이 경우:
- `picksplit`에서 `nodeLabels`를 `NULL`로 반환 가능
- `choose`에서 `prefixNodeLabels`를 `NULL`로 반환 가능
- 공간 절약 및 코드 단순화 효과
- 주의: `choose`는 레이블 없는 튜플에 대해 `spgAddNode`를 반환하면 안 됨

#### "All-the-Same" 내부 튜플

`picksplit`이 리프 값을 여러 카테고리로 분류하지 못할 경우:

1. SP-GiST 코어가 동등한 여러 노드를 가진 내부 튜플 생성
2. `allTheSame` 플래그가 true로 설정됨
3. `choose`가 `spgMatchNode`를 반환하면 어떤 동등 노드든 하강 가능
4. `inner_consistent`는 모든 노드 또는 아무 노드도 반환해야 함

이 메커니즘은:
- 무한 재귀 방지
- 트리의 균형 유지
- 데이터 분포가 분할 규칙과 잘 맞지 않는 경우 처리

```
allTheSame = true 인 내부 튜플 예시:
┌──────────────────────────────────────┐
│     내부 튜플 (allTheSame = true)    │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐    │
│  │노드1│ │노드2│ │노드3│ │노드4│    │
│  │  ≡  │ │  ≡  │ │  ≡  │ │  ≡  │    │
│  └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘    │
└─────┼──────┼──────┼──────┼─────────┘
      │      │      │      │
      ▼      ▼      ▼      ▼
    리프   리프   리프   리프
    튜플   튜플   튜플   튜플
```

#### NULL 처리

SP-GiST 코어는 NULL 항목을 자동으로 처리함:

- NULL 값은 인덱스에 저장되지만 연산자 클래스 코드에서는 숨겨짐
- 연산자 클래스 메서드에 NULL 인덱스 항목이나 검색 조건이 전달되지 않음
- 모든 연산자는 strict로 가정됨

```sql
-- NULL 값도 인덱스에 저장됨
CREATE TABLE test_spgist (
    id serial PRIMARY KEY,
    location point
);

INSERT INTO test_spgist (location) VALUES
    (point(1,2)),
    (NULL),         -- NULL 값도 저장됨
    (point(3,4));

CREATE INDEX idx_test_location ON test_spgist USING spgist (location);

-- IS NULL 조건도 인덱스 사용 가능
SELECT * FROM test_spgist WHERE location IS NULL;
```

#### 메모리 관리

SP-GiST 지원 메서드는 단기 메모리 컨텍스트에서 호출됨:

- `CurrentMemoryContext`는 각 튜플 처리 후 재설정됨
- `config` 메서드는 메모리 누수를 피해야 함
- 출력 구조체는 메서드 호출 전에 0으로 초기화됨

```c
/* config 메서드 예시 - 메모리 누수 주의 */
Datum
my_config(PG_FUNCTION_ARGS)
{
    spgConfigOut *cfg = (spgConfigOut *) PG_GETARG_POINTER(1);

    cfg->prefixType = INT4OID;
    cfg->labelType = INT4OID;
    cfg->leafType = INT4OID;
    cfg->canReturnData = true;
    cfg->longValuesOK = false;

    PG_RETURN_VOID();
}
```

#### 예제 구현 위치

PostgreSQL 소스 배포판에는 다음 위치에 예제 구현이 포함되어 있음:

- `src/backend/access/spgist/`: SP-GiST 코어 코드
  - `spgutils.c`: 유틸리티 함수
  - `spginsert.c`: 삽입 로직
  - `spgscan.c`: 스캔 로직
  - `spgvacuum.c`: VACUUM 로직

- `src/backend/utils/adt/`: 내장 연산자 클래스 구현
  - `geo_spgist.c`: 기하 타입 (point, box, polygon)
  - `rangetypes_spgist.c`: 범위 타입
  - `network_spgist.c`: 네트워크 타입 (inet, cidr)

---

### 성능 고려 사항

#### SP-GiST vs. GiST vs. B-tree

- 데이터 구조
  - SP-GiST: 분할 트리
  - GiST: 균형 트리
  - B-tree: 균형 트리
- 적합한 데이터
  - SP-GiST: 공간, 네트워크, 텍스트
  - GiST: 복잡한 데이터 타입
  - B-tree: 스칼라 값
- k-NN 지원
  - SP-GiST: 예 (일부 타입)
  - GiST: 예
  - B-tree: 아니오
- 접두사 검색
  - SP-GiST: 매우 효율적
  - GiST: 보통
  - B-tree: 효율적
- 메모리 사용
  - SP-GiST: 낮음
  - GiST: 중간
  - B-tree: 낮음

#### 최적 사용 사례

1. 점 데이터의 k-NN 검색
```sql
CREATE INDEX idx_point ON locations USING spgist (position);

SELECT * FROM locations
ORDER BY position <-> point(10, 20)
LIMIT 10;
```

2. IP 주소 서브넷 검색
```sql
CREATE INDEX idx_ip ON hosts USING spgist (ip_address inet_ops);

SELECT * FROM hosts WHERE ip_address <<= '192.168.0.0/16';
```

3. 텍스트 접두사 검색
```sql
CREATE INDEX idx_name ON products USING spgist (name text_ops);

SELECT * FROM products WHERE name ^@ 'App';
```

#### 인덱스 생성 시 고려사항

```sql
-- INCLUDE를 사용한 커버링 인덱스
CREATE INDEX idx_location_covering ON locations
    USING spgist (position)
    INCLUDE (name, category);

-- FILLFACTOR 설정 (업데이트가 빈번한 경우)
CREATE INDEX idx_location_fill ON locations
    USING spgist (position)
    WITH (fillfactor = 80);
```

---

### 요약

SP-GiST는 공간 분할 기반의 유연한 인덱스 구조를 제공함:

1. 다양한 트리 구조 지원: 쿼드 트리, k-d 트리, 기수 트리 등
2. 효율적인 공간 검색: 점, 상자, 다각형 등의 기하 데이터
3. 네트워크 데이터 지원: IP 주소 및 서브넷 검색
4. 텍스트 접두사 검색: 기수 트리 기반의 빠른 접두사 매칭
5. k-NN 검색 지원: 가장 가까운 이웃 검색

SP-GiST는 GiST와 달리 비균형 트리 구조를 지원하여 특정 유형의 데이터에 대해 더 효율적인 검색이 가능함. 데이터 특성에 따라 적절한 인덱스 유형을 선택하는 것이 중요함.

---

## Chapter 70: GIN 인덱스 (GIN Indexes)

### 목차
1. [소개](#1-소개-introduction)
2. [내장 연산자 클래스](#2-내장-연산자-클래스-built-in-operator-classes)
3. [확장성](#3-확장성-extensibility)
4. [구현](#4-구현-implementation)
5. [GIN 팁과 트릭](#5-gin-팁과-트릭-tips-and-tricks)
6. [제한 사항](#6-제한-사항-limitations)
7. [예제](#7-예제-examples)

---

### 1. 소개 (Introduction)

#### 1.1 GIN이란?

GIN 은 Generalized Inverted Index(일반화된 역 인덱스) 의 약자임. GIN은 복합 값(composite values)을 처리하기 위해 설계되었으며, 쿼리가 해당 항목 내의 요소 값을 검색하는 경우에 적합함. 예를 들어, 특정 단어를 포함하는 문서를 검색하는 경우가 이에 해당함.

#### 1.2 핵심 개념

GIN 인덱스에서 사용되는 주요 용어는 다음과 같음:

- 항목 (Item): 인덱싱될 복합 값
- 키 (Key): 항목 내의 요소 값
- 포스팅 리스트 (Posting List): 특정 키가 발생하는 행 ID(row ID)의 집합

#### 1.3 데이터 구조

GIN은 (키, 포스팅 리스트) 쌍을 저장함:

- 각 키 값은 한 번만 저장됨 (자주 등장하는 키에 대해 공간 효율적)
- 동일한 행 ID가 여러 포스팅 리스트에 나타날 수 있음 (항목이 여러 키를 포함하기 때문)
- 검색은 항목이 아닌 키를 대상으로 수행됨

#### 1.4 주요 장점

- 특정 연산에 독립적인 일반화된 접근 방법 코드
- 특정 데이터 타입에 대한 사용자 정의 전략 가능
- 도메인 전문가가 적절한 접근 방법으로 사용자 정의 데이터 타입을 개발할 수 있음

---

### 2. 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL은 다음과 같은 GIN 연산자 클래스를 기본적으로 제공함:

#### 2.1 연산자 클래스 목록

- `array_ops`
  - 인덱싱 가능한 연산자: `&&`, `@>`, `<@`, `=`
  - 설명: 배열 연산
- `jsonb_ops`
  - 인덱싱 가능한 연산자: `@>`, `@?`, `@@`, `?`, `?\|`, `?&`
  - 설명: JSONB 기본 연산자 클래스
- `jsonb_path_ops`
  - 인덱싱 가능한 연산자: `@>`, `@?`, `@@`
  - 설명: 더 적은 연산자, 더 나은 성능
- `tsvector_ops`
  - 인덱싱 가능한 연산자: `@@`
  - 설명: 전문 검색(Full-Text Search)

#### 2.2 연산자 설명

##### 배열 연산자 (array_ops)

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

##### JSONB 연산자 (jsonb_ops)

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

#### 2.3 jsonb_ops vs jsonb_path_ops

```sql
-- jsonb_ops (기본값) - 더 많은 연산자 지원
CREATE INDEX idx_data ON documents USING GIN (data);

-- jsonb_path_ops - 더 나은 성능, 제한된 연산자
CREATE INDEX idx_data_path ON documents USING GIN (data jsonb_path_ops);
```

차이점:
- `jsonb_ops`: 모든 JSONB 연산자 지원, 더 큰 인덱스 크기
- `jsonb_path_ops`: `@>`, `@?`, `@@` 연산자만 지원, 더 작은 인덱스, 더 빠른 검색

##### 전문 검색 연산자 (tsvector_ops)

```sql
-- 전문 검색 인덱스 생성
CREATE INDEX idx_search ON articles USING GIN (to_tsvector('english', content));

-- 검색 쿼리
SELECT * FROM articles
WHERE to_tsvector('english', content) @@ to_tsquery('english', 'postgresql & indexing');
```

---

### 3. 확장성 (Extensibility)

GIN은 사용자 정의 연산자 클래스 메서드를 구현할 수 있는 확장 인터페이스를 제공함. GIN 계층은 동시성, 로깅 및 트리 구조 검색을 처리함.

#### 3.1 필수 메서드

##### 3.1.1 extractValue()

인덱싱할 항목에서 키를 추출함.

```c
Datum *extractValue(Datum itemValue, int32 *nkeys, bool **nullFlags)
```

매개변수:
- `itemValue`: 인덱싱할 항목 값
- `nkeys`: 추출된 키의 개수를 저장할 포인터
- `nullFlags`: null 키가 있을 경우 설정할 플래그 배열

반환값:
- palloc'd 키 배열
- 항목에 키가 없으면 `NULL` 반환 가능

##### 3.1.2 extractQuery()

쿼리 값에서 키를 추출함.

```c
Datum *extractQuery(Datum query, int32 *nkeys, StrategyNumber n,
                    bool **pmatch, Pointer **extra_data,
                    bool **nullFlags, int32 *searchMode)
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

- `GIN_SEARCH_MODE_DEFAULT`: 1개 이상의 키와 일치하는 항목만 후보
- `GIN_SEARCH_MODE_INCLUDE_EMPTY`: 키가 없는 항목도 포함
- `GIN_SEARCH_MODE_ALL`: 모든 non-null 항목이 후보 (느림, 특수 케이스용)

#### 3.2 항목 일치 메서드 (하나 이상 구현)

##### 3.2.1 consistent() - 불리언 버전

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

##### 3.2.2 triConsistent() - 3값 버전

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

`triConsistent`만 제공해도 충분하며, `consistent` 기능을 포함함.

#### 3.3 키 정렬 메서드

##### compare() (권장)

```c
int compare(Datum a, Datum b)
```

반환값:
- `< 0`: a가 b보다 작음
- `0`: 같음
- `> 0`: a가 b보다 큼

제공하지 않으면 GIN은 키 데이터 타입의 기본 btree 연산자 클래스를 찾음.

#### 3.4 선택적 메서드

##### 3.4.1 comparePartial()

부분 일치 쿼리 지원을 위한 메서드임.

```c
int comparePartial(Datum partial_key, Datum key, StrategyNumber n,
                   Pointer extra_data)
```

반환값:
- `< 0`: 일치하지 않음, 스캔 계속
- `0`: 일치
- `> 0`: 스캔 중지

`extractQuery`가 `pmatch` 매개변수를 설정하는 경우 필수임.

##### 3.4.2 options()

연산자 클래스 동작을 제어하는 사용자 표시 매개변수를 정의함.

```c
void options(local_relopts *relopts)
```

`PG_HAS_OPCLASS_OPTIONS()` 및 `PG_GET_OPCLASS_OPTIONS()` 매크로로 접근함.

#### 3.5 메서드 요약 테이블

- `extractValue`
  - 목적: 항목에서 키 추출
  - 입력: 항목 datum
  - 출력: 키 배열, 개수, null 플래그
- `extractQuery`
  - 목적: 쿼리에서 키 추출
  - 입력: 쿼리 datum, 전략
  - 출력: 키 배열, 검색 모드, 부분 일치 플래그
- `consistent`
  - 목적: 항목이 쿼리와 일치하는지 확인
  - 입력: check 배열, 쿼리
  - 출력: Boolean + recheck 플래그
- `triConsistent`
  - 목적: 항목이 쿼리와 일치하는지 확인
  - 입력: check 배열(3값), 쿼리
  - 출력: GIN_TRUE/FALSE/MAYBE
- `compare`
  - 목적: 키 정렬
  - 입력: 두 개의 키 datum
  - 출력: -1, 0, 또는 1
- `comparePartial`
  - 목적: 부분 일치 키 범위
  - 입력: 부분 키, 인덱스 키
  - 출력: -1, 0, 또는 1
- `options`
  - 목적: 연산자 클래스 옵션 정의
  - 입력: relopts 구조체
  - 출력: (구조체 수정)

---

### 4. 구현 (Implementation)

#### 4.1 내부 구조

GIN 인덱스는 다음을 포함함:

- 키에 대한 B-tree 인덱스 (인덱싱된 항목의 요소)
- 리프 페이지 튜플:
  - 힙 포인터의 B-tree에 대한 포인터 (포스팅 트리, posting tree)
  - 또는 작은 리스트의 경우 힙 포인터의 간단한 리스트 (포스팅 리스트, posting list)

#### 4.2 다중 컬럼 GIN 인덱스

- 복합 값에 대한 단일 B-tree: (컬럼 번호, 키 값)
- 다른 컬럼의 키 값은 서로 다른 타입일 수 있음
- null 키 값 지원 및 null 항목에 대한 플레이스홀더 null 지원

```sql
-- 다중 컬럼 GIN 인덱스 예제
CREATE INDEX idx_multi ON documents USING GIN (tags, categories);
```

#### 4.3 GIN 빠른 업데이트 기법 (Fast Update Technique)

##### 문제점

GIN 인덱스 업데이트는 느림. 하나의 힙 행(heap row)이 많은 인덱스 삽입을 유발할 수 있기 때문임(키당 하나씩).

##### 해결책

새 튜플을 임시 보류 항목 리스트 (pending entries list) 에 삽입하여 대부분의 작업을 연기함.

##### 정리 시점

- 테이블 VACUUM 또는 ANALYZE 중
- `gin_clean_pending_list` 함수 호출 시
- 보류 리스트가 `gin_pending_list_limit`를 초과할 때
- 이후 항목들은 대량 삽입 기법을 사용하여 메인 GIN 구조로 이동

##### 장점

- GIN 인덱스 업데이트 속도가 크게 향상됨
- 오버헤드는 백그라운드 autovacuum 프로세스가 처리할 수 있음

##### 단점

- 검색 시 보류 항목을 스캔해야 함 (큰 리스트는 검색을 느리게 함)
- "너무 큰" 보류 리스트를 유발하는 업데이트는 즉시 정리됨 (더 느림)

##### 제어

```sql
-- fastupdate 비활성화
CREATE INDEX idx_gin ON documents USING GIN (data) WITH (fastupdate = off);

-- 보류 리스트 제한 설정
ALTER INDEX idx_gin SET (gin_pending_list_limit = 256);
```

#### 4.4 부분 일치 알고리즘 (Partial Match Algorithm)

##### 사용 사례

쿼리가 하나 이상의 키에 대해 정확한 일치 여부를 판단하기 어렵지만, 일치 항목이 좁은 키 값 범위 안에 있는 경우에 사용함.

##### 프로세스

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

### 5. GIN 팁과 트릭 (Tips and Tricks)

#### 5.1 생성 vs 삽입 (Create vs Insert)

대량 삽입의 경우:
1. GIN 인덱스 삭제
2. 데이터 삽입
3. 인덱스 재생성

이 방법이 기존 인덱스에 계속 삽입하는 것보다 빠름.

```sql
-- 대량 데이터 로드 시
DROP INDEX idx_gin;
-- 대량 INSERT 수행
CREATE INDEX idx_gin ON documents USING GIN (data);
```

참고: fastupdate가 활성화되어 있으면 오버헤드가 줄어들지만, 데이터 규모가 매우 클 경우 인덱스를 삭제하고 재생성하는 방법이 여전히 유리할 수 있음.

#### 5.2 maintenance_work_mem

GIN 빌드 시간은 이 설정에 매우 민감함. 인덱스 생성 중에는 작업 메모리를 충분히 확보하는 것이 좋음.

```sql
-- 인덱스 생성 전 임시로 증가
SET maintenance_work_mem = '1GB';
CREATE INDEX idx_gin ON documents USING GIN (data);
RESET maintenance_work_mem;
```

#### 5.3 gin_pending_list_limit

보류 항목이 정리되는 시점을 제어함.

포그라운드 정리를 피하려면:
- 제한값을 높이거나 (단, 정리가 발생할 때 더 오래 걸림)
- autovacuum을 더 적극적으로 동작하도록 설정

```sql
-- 전역 설정
SET gin_pending_list_limit = '64kB';

-- 개별 인덱스에 대한 재정의
ALTER INDEX idx_gin SET (gin_pending_list_limit = 128);
```

개별 인덱스 오버라이드:
자주 업데이트되는 인덱스에는 높은 제한값을, 그렇지 않은 인덱스에는 낮은 제한값을 설정함.

```sql
-- 자주 업데이트되는 테이블
ALTER INDEX idx_frequently_updated SET (gin_pending_list_limit = 256);

-- 거의 업데이트되지 않는 테이블
ALTER INDEX idx_rarely_updated SET (gin_pending_list_limit = 64);
```

#### 5.4 gin_fuzzy_search_limit

전문 검색에서 반환되는 행의 소프트 상한을 설정함.

```sql
-- 기본값: 0 (제한 없음)
SET gin_fuzzy_search_limit = 10000;
```

해결되는 문제:
자주 사용되는 단어를 포함한 전문 검색이 지나치게 많은 결과를 반환하는 경우

권장 값: 5000-20000

참고: "소프트" 제한이므로 실제 반환되는 행 수는 쿼리 및 난수 생성 결과에 따라 달라질 수 있음.

#### 5.5 성능 최적화 체크리스트

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

### 6. 제한 사항 (Limitations)

#### 6.1 엄격한 연산자 요구 사항

인덱싱 가능한 연산자는 반드시 strict여야 함:

- null 항목 값에 대해서는 `extractValue`가 호출되지 않으며, 대신 플레이스홀더 항목이 자동으로 생성됨.
- null 쿼리 값에 대해서는 `extractQuery`가 호출되지 않으며, 해당 쿼리는 충족 불가능한 것으로 처리됨.

#### 6.2 예외

non-null 복합 항목/쿼리 내의 null 키 값은 지원됨.

```sql
-- 허용: 배열 내 null 값
CREATE INDEX idx_array ON t USING GIN (arr);
INSERT INTO t VALUES (ARRAY[1, NULL, 3]);

-- 쿼리 가능
SELECT * FROM t WHERE arr @> ARRAY[1];
```

---

### 7. 예제 (Examples)

#### 7.1 배열 인덱싱

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

#### 7.2 JSONB 인덱싱

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

#### 7.3 전문 검색 (Full-Text Search)

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

#### 7.4 Contrib 모듈 사용

##### pg_trgm (트라이그램 일치)

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

##### intarray (정수 배열 확장)

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

##### hstore (키-값 저장소)

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

### 부록: GIN vs GiST 비교

- 검색 속도
  - GIN: 빠름 (정확한 일치에 최적)
  - GiST: 보통
- 인덱스 빌드 속도
  - GIN: 느림
  - GiST: 빠름
- 인덱스 크기
  - GIN: 큼
  - GiST: 작음
- 업데이트 속도
  - GIN: 느림 (fastupdate로 완화)
  - GiST: 빠름
- 적합한 사용 사례
  - GIN: 전문 검색, 배열, JSONB
  - GiST: 기하학적 데이터, 범위 쿼리
- 정확도
  - GIN: 손실 없음 (lossless)
  - GiST: 손실 가능 (lossy)

```sql
-- GIN: 전문 검색에 최적
CREATE INDEX idx_gin ON docs USING GIN (to_tsvector('english', content));

-- GiST: 기하학적 데이터에 적합
CREATE INDEX idx_gist ON locations USING GiST (coordinates);
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - GIN Indexes](https://www.postgresql.org/docs/current/gin.html)
- [PostgreSQL 공식 문서 - Full Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [PostgreSQL 공식 문서 - pg_trgm](https://www.postgresql.org/docs/current/pgtrgm.html)

---

## Chapter 71: BRIN 인덱스 (BRIN Indexes)

### 목차
- [71.1 소개 (Introduction)](#711-소개-introduction)
  - [71.1.1 인덱스 유지보수 (Index Maintenance)](#7111-인덱스-유지보수-index-maintenance)
- [71.2 내장 연산자 클래스 (Built-in Operator Classes)](#712-내장-연산자-클래스-built-in-operator-classes)
  - [71.2.1 연산자 클래스 매개변수 (Operator Class Parameters)](#7121-연산자-클래스-매개변수-operator-class-parameters)
- [71.3 확장성 (Extensibility)](#713-확장성-extensibility)
  - [71.3.1 Minmax 연산자 클래스](#7131-minmax-연산자-클래스)
  - [71.3.2 Inclusion 연산자 클래스](#7132-inclusion-연산자-클래스)
  - [71.3.3 Bloom 연산자 클래스](#7133-bloom-연산자-클래스)
  - [71.3.4 Minmax-Multi 연산자 클래스](#7134-minmax-multi-연산자-클래스)

---

### 71.1 소개 (Introduction)

BRIN은 Block Range Index의 약자임. BRIN은 특정 컬럼이 테이블 내 물리적 위치와 자연스러운 상관관계(correlation)를 갖는 매우 큰 테이블을 처리하기 위해 설계되었음.

#### 블록 범위 (Block Range) 개념

BRIN은 블록 범위 (또는 "페이지 범위")를 기준으로 동작함. 블록 범위는 테이블에서 물리적으로 인접한 페이지들의 그룹임. 각 블록 범위에 대해 인덱스는 요약 정보(summary information)를 저장함.

예를 들어, 판매 주문 테이블에 날짜 컬럼이 있고 오래된 주문이 테이블 앞부분에 위치한다면, 해당 컬럼은 물리적 위치와 자연스러운 상관관계를 갖음.

#### 쿼리 실행 방식

BRIN 인덱스는 일반적인 비트맵 인덱스 스캔(bitmap index scan)을 통해 쿼리를 처리함:

1. 요약 정보가 쿼리 조건과 일치하면 해당 범위 내 모든 페이지의 튜플을 반환함
2. BRIN 인덱스는 손실(lossy) 인덱스임 — 쿼리 실행기(query executor)가 튜플을 재확인하여 조건에 맞지 않는 것을 제거해야 함
3. 인덱스 크기가 매우 작아, 순차 스캔에 비해 최소한의 오버헤드로 테이블의 상당 부분을 건너뛸 수 있음

#### 스토리지와 정밀도

`pages_per_range` 스토리지 매개변수는 인덱스 생성 시 블록 범위의 크기를 결정함:

```sql
-- 기본값으로 BRIN 인덱스 생성
CREATE INDEX idx_brin ON sales_orders USING brin (order_date);

-- pages_per_range를 32로 지정
CREATE INDEX idx_brin_custom ON sales_orders USING brin (order_date)
    WITH (pages_per_range = 32);
```

인덱스 항목 수 계산:
```
인덱스 항목 수 = 테이블의 총 페이지 수 / pages_per_range
```

트레이드오프:
- 더 작은 `pages_per_range` 값 → 더 큰 인덱스, 더 정밀한 요약 데이터, 더 많은 데이터 블록 건너뛰기 가능
- 더 큰 `pages_per_range` 값 → 더 작은 인덱스, 덜 정밀한 요약 데이터

---

#### 71.1.1 인덱스 유지보수 (Index Maintenance)

##### 요약화 프로세스 (Summarization Process)

초기 생성 단계:
- 기존의 모든 힙(heap) 페이지가 스캔됨
- 각 범위에 대해 요약 인덱스 튜플이 생성됨
- 끝에 있는 불완전한 범위도 포함됨

지속적인 업데이트:
- 이미 요약된 페이지 범위에 새 데이터가 삽입되면 요약 정보가 갱신됨
- 마지막으로 요약된 범위 이후의 새 페이지는 요약화가 실행될 때까지 요약되지 않은 상태로 남음

##### 요약화 트리거 방법

요약화는 다음 방법으로 트리거될 수 있음:

1. 수동 VACUUM: 테이블을 수동으로 또는 autovacuum을 통해 VACUUM 실행

2. autosummarize 매개변수: 활성화되면 autovacuum이 채워진 페이지 범위를 자동으로 요약

3. 함수 사용:

```sql
-- 모든 요약되지 않은 범위를 요약
SELECT brin_summarize_new_values('idx_brin'::regclass);

-- 특정 페이지를 포함하는 범위만 요약
SELECT brin_summarize_range('idx_brin'::regclass, 128);
```

##### 역요약화 (De-summarization)

인덱스 튜플이 더 이상 해당 범위의 값을 잘 나타내지 못할 때, `brin_desummarize_range` 함수로 범위 요약을 해제할 수 있음:

```sql
-- 특정 범위의 요약 해제
SELECT brin_desummarize_range('idx_brin'::regclass, 128);
```

##### Autosummarize 세부사항

- 기본적으로 비활성화되어 있음
- 활성화하면 다음 블록 범위로의 첫 삽입이 감지될 때 autovacuum이 해당 범위의 요약 요청을 수신함
- 요청 큐가 가득 차면 서버 로그에 메시지가 나타남:

```
LOG: request for BRIN range summarization for index "brin_wi_idx" page 128 was not recorded
```

```sql
-- autosummarize 활성화하여 인덱스 생성
CREATE INDEX idx_brin_auto ON sales_orders USING brin (order_date)
    WITH (autosummarize = on);
```

---

### 71.2 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 네 가지 유형의 BRIN 연산자 클래스가 포함되어 있음:

- minmax
  - 설명: 범위 내 인덱싱된 컬럼의 최솟값과 최댓값 저장
  - 사용 사례: 완전 순서 집합(totally ordered set)
- minmax-multi
  - 설명: 범위 내 값을 나타내는 여러 최솟값/최댓값 저장
  - 사용 사례: 이상치(outlier)가 있는 데이터
- inclusion
  - 설명: 범위 내 값을 포함하는 값 저장
  - 사용 사례: 기하학적 타입, 범위 타입
- bloom
  - 설명: 범위 내 모든 값에 대한 블룸 필터 구축
  - 사용 사례: 등호 비교만 필요한 경우

#### 전체 내장 연산자 클래스 목록

##### 숫자 타입 (Numeric Types)

- `int2`
  - minmax: `int2_minmax_ops`
  - bloom: `int2_bloom_ops`
  - minmax-multi: `int2_minmax_multi_ops`
- `int4`
  - minmax: `int4_minmax_ops`
  - bloom: `int4_bloom_ops`
  - minmax-multi: `int4_minmax_multi_ops`
- `int8`
  - minmax: `int8_minmax_ops`
  - bloom: `int8_bloom_ops`
  - minmax-multi: `int8_minmax_multi_ops`
- `float4`
  - minmax: `float4_minmax_ops`
  - bloom: `float4_bloom_ops`
  - minmax-multi: `float4_minmax_multi_ops`
- `float8`
  - minmax: `float8_minmax_ops`
  - bloom: `float8_bloom_ops`
  - minmax-multi: `float8_minmax_multi_ops`
- `numeric`
  - minmax: `numeric_minmax_ops`
  - bloom: `numeric_bloom_ops`
  - minmax-multi: `numeric_minmax_multi_ops`

##### 문자/문자열 타입 (Character/String Types)

- `char`
  - minmax: `char_minmax_ops`
  - bloom: `char_bloom_ops`
- `bpchar`
  - minmax: `bpchar_minmax_ops`
  - bloom: `bpchar_bloom_ops`
- `text`
  - minmax: `text_minmax_ops`
  - bloom: `text_bloom_ops`
- `name`
  - minmax: `name_minmax_ops`
  - bloom: `name_bloom_ops`
- `bytea`
  - minmax: `bytea_minmax_ops`
  - bloom: `bytea_bloom_ops`

##### 날짜/시간 타입 (Date/Time Types)

- `date`
  - minmax: `date_minmax_ops`
  - bloom: `date_bloom_ops`
  - minmax-multi: `date_minmax_multi_ops`
- `timestamp`
  - minmax: `timestamp_minmax_ops`
  - bloom: `timestamp_bloom_ops`
  - minmax-multi: `timestamp_minmax_multi_ops`
- `timestamptz`
  - minmax: `timestamptz_minmax_ops`
  - bloom: `timestamptz_bloom_ops`
  - minmax-multi: `timestamptz_minmax_multi_ops`
- `time`
  - minmax: `time_minmax_ops`
  - bloom: `time_bloom_ops`
  - minmax-multi: `time_minmax_multi_ops`
- `timetz`
  - minmax: `timetz_minmax_ops`
  - bloom: `timetz_bloom_ops`
  - minmax-multi: `timetz_minmax_multi_ops`
- `interval`
  - minmax: `interval_minmax_ops`
  - bloom: `interval_bloom_ops`
  - minmax-multi: `interval_minmax_multi_ops`

##### 네트워크 타입 (Network Types)

- `inet`
  - minmax: `inet_minmax_ops`
  - bloom: `inet_bloom_ops`
  - minmax-multi: `inet_minmax_multi_ops`
  - inclusion: `inet_inclusion_ops`
- `macaddr`
  - minmax: `macaddr_minmax_ops`
  - bloom: `macaddr_bloom_ops`
  - minmax-multi: `macaddr_minmax_multi_ops`
  - inclusion: 없음
- `macaddr8`
  - minmax: `macaddr8_minmax_ops`
  - bloom: `macaddr8_bloom_ops`
  - minmax-multi: `macaddr8_minmax_multi_ops`
  - inclusion: 없음

##### 특수 타입 (Specialized Types)

- `uuid`: `uuid_minmax_ops`, `uuid_bloom_ops`, `uuid_minmax_multi_ops`
- `bit`: `bit_minmax_ops`
- `varbit`: `varbit_minmax_ops`
- `oid`: `oid_minmax_ops`, `oid_bloom_ops`, `oid_minmax_multi_ops`
- `tid`: `tid_minmax_ops`, `tid_bloom_ops`, `tid_minmax_multi_ops`
- `pg_lsn`: `pg_lsn_minmax_ops`, `pg_lsn_bloom_ops`, `pg_lsn_minmax_multi_ops`
- `box`: `box_inclusion_ops`
- `range`: `range_inclusion_ops`

#### 사용 예제

```sql
-- minmax 연산자 클래스 사용 (기본값)
CREATE INDEX idx_date_minmax ON orders USING brin (order_date);

-- bloom 연산자 클래스 사용
CREATE INDEX idx_customer_bloom ON orders USING brin (customer_id int4_bloom_ops);

-- minmax-multi 연산자 클래스 사용
CREATE INDEX idx_amount_multi ON orders USING brin (amount numeric_minmax_multi_ops);

-- inclusion 연산자 클래스 사용 (기하학적 타입)
CREATE INDEX idx_location ON locations USING brin (bounding_box box_inclusion_ops);
```

---

#### 71.2.1 연산자 클래스 매개변수 (Operator Class Parameters)

bloom 과 minmax-multi 연산자 클래스만 매개변수를 지원함.

##### Bloom 연산자 클래스 매개변수

`n_distinct_per_range`
- 블록 범위 내 예상되는 고유한 non-null 값의 수를 정의함
- 양수 값: 블록 범위에 이 수만큼의 고유 값이 있다고 가정
- 음수 값 (≥ -1): 고유 non-null 값의 수가 블록 범위의 최대 튜플 수에 비례하여 선형적으로 증가 (블록당 약 290개 행)
- 기본값: `-0.1`
- 최솟값: `16`

`false_positive_rate`
- 블룸 필터 크기 조정을 위한 원하는 위양성률(false positive rate)
- 유효 범위: `0.0001` ~ `0.25`
- 기본값: `0.01` (1% 위양성률)

```sql
-- Bloom 연산자 클래스 매개변수 사용 예제
CREATE INDEX idx_bloom_custom ON orders USING brin (
    customer_id int4_bloom_ops (
        n_distinct_per_range = 100,
        false_positive_rate = 0.05
    )
);
```

##### Minmax-Multi 연산자 클래스 매개변수

`values_per_range`
- 블록 범위를 요약하기 위해 저장할 최대 값의 수
- 각 값은 점(point) 또는 구간 경계(interval boundary)를 나타냄
- 유효 범위: `8` ~ `256`
- 기본값: `32`

```sql
-- Minmax-Multi 연산자 클래스 매개변수 사용 예제
CREATE INDEX idx_multi_custom ON orders USING brin (
    amount numeric_minmax_multi_ops (values_per_range = 64)
);
```

---

### 71.3 확장성 (Extensibility)

BRIN 인터페이스는 높은 수준의 추상화를 제공하므로, 액세스 메서드 구현자는 대상 데이터 타입의 의미론(semantics)만 구현하면 됨. 동시성, 로깅, 인덱스 구조 탐색은 BRIN 레이어가 직접 처리함.

#### 필수 메서드 (Required Methods)

BRIN 연산자 클래스는 네 가지 핵심 메서드를 제공해야 함:

##### 1. `opcInfo`

```c
BrinOpcInfo *opcInfo(Oid type_oid)
```

인덱싱된 컬럼의 요약 데이터에 대한 내부 정보를 반환함:

```c
typedef struct BrinOpcInfo
{
    uint16      oi_nstored;           /* 저장된 컬럼 수 */
    void       *oi_opaque;            /* 개인용 불투명 포인터 */
    TypeCacheEntry *oi_typcache[];    /* 타입 캐시 항목 */
} BrinOpcInfo;
```

##### 2. `consistent`

```c
bool consistent(BrinDesc *bdesc, BrinValues *column, ScanKey *keys, int nkeys)
```

모든 ScanKey 항목이 해당 범위의 인덱싱된 값과 일치하는지 검사함. 동일한 속성에 대해 여러 스캔 키를 지원함.

이전 버전과의 호환성을 위한 단일 ScanKey 변형:
```c
bool consistent(BrinDesc *bdesc, BrinValues *column, ScanKey key)
```

##### 3. `addValue`

```c
bool addValue(BrinDesc *bdesc, BrinValues *column, Datum newval, bool isnull)
```

인덱스 튜플이 새 값도 포함하도록 수정함. 튜플이 실제로 수정된 경우 `true`를 반환함.

##### 4. `unionTuples`

```c
bool unionTuples(BrinDesc *bdesc, BrinValues *a, BrinValues *b)
```

두 인덱스 튜플을 병합하여 첫 번째 튜플이 두 튜플 모두를 나타내도록 수정함.

#### 선택적 메서드 (Optional Method)

##### `options`

```c
void options(local_relopts *relopts)
```

연산자 클래스 동작을 제어하는 사용자 노출 매개변수를 정의함. 옵션은 `PG_HAS_OPCLASS_OPTIONS()`와 `PG_GET_OPCLASS_OPTIONS()` 매크로를 통해 접근함.

#### 지원 함수 번호 규칙

- 함수 1-10: BRIN 내부 함수용으로 예약
- 함수 11+: SQL 레벨 사용자 정의 구현용

---

#### 71.3.1 Minmax 연산자 클래스

단일 연속 구간으로 표현 가능한 완전 순서 집합(totally ordered set)을 위한 연산자 클래스임.

##### 필수 멤버

- 지원 함수 1: `brin_minmax_opcinfo()`
- 지원 함수 2: `brin_minmax_add_value()`
- 지원 함수 3: `brin_minmax_consistent()`
- 지원 함수 4: `brin_minmax_union()`
- 연산자 전략 1: 미만 (`<`)
- 연산자 전략 2: 이하 (`<=`)
- 연산자 전략 3: 같음 (`=`)
- 연산자 전략 4: 이상 (`>=`)
- 연산자 전략 5: 초과 (`>`)

##### 사용자 정의 Minmax 연산자 클래스 예제

```sql
-- 사용자 정의 타입을 위한 minmax 연산자 클래스 생성
CREATE OPERATOR CLASS my_type_minmax_ops
    DEFAULT FOR TYPE my_type USING brin AS
    OPERATOR 1 <,
    OPERATOR 2 <=,
    OPERATOR 3 =,
    OPERATOR 4 >=,
    OPERATOR 5 >,
    FUNCTION 1 brin_minmax_opcinfo(internal),
    FUNCTION 2 brin_minmax_add_value(internal, internal, internal, internal),
    FUNCTION 3 brin_minmax_consistent(internal, internal, internal),
    FUNCTION 4 brin_minmax_union(internal, internal, internal);
```

---

#### 71.3.2 Inclusion 연산자 클래스

다른 값에 포함되는(containment) 값을 갖는 복잡한 데이터 타입을 위한 연산자 클래스임.

##### 필수 멤버

- 지원 함수 1
  - 객체: `brin_inclusion_opcinfo()`
  - 설명: 기본 정보
- 지원 함수 2
  - 객체: `brin_inclusion_add_value()`
  - 설명: 값 추가
- 지원 함수 3
  - 객체: `brin_inclusion_consistent()`
  - 설명: 일관성 검사
- 지원 함수 4
  - 객체: `brin_inclusion_union()`
  - 설명: 통합
- 지원 함수 11
  - 객체: 병합 함수 (필수)
  - 설명: 두 요소 병합
- 지원 함수 12
  - 객체: 병합 가능성 검사 (선택)
  - 설명: 요소 병합 가능 여부
- 지원 함수 13
  - 객체: 포함 검사 (선택)
  - 설명: 요소 포함 여부
- 지원 함수 14
  - 객체: 빈 요소 검사 (선택)
  - 설명: 범위 타입용

##### 주요 지원 함수

- 지원 함수 11 (필수): 연산자 클래스와 동일한 데이터 타입의 두 요소를 병합함
- 지원 함수 12: 두 요소가 병합 가능한지 검사함 (네트워크 주소 패밀리 등)
- 지원 함수 13: 한 요소가 다른 요소에 포함되는지 검사함 (성능 향상에 권장)
- 지원 함수 14: 요소가 비어 있는지 검사함 (범위 타입용)

##### Inclusion 연산자 클래스 사용 예제

```sql
-- box 타입을 위한 inclusion 인덱스 생성
CREATE TABLE geometric_data (
    id serial PRIMARY KEY,
    bounding_box box
);

CREATE INDEX idx_box ON geometric_data USING brin (bounding_box box_inclusion_ops);

-- 범위 타입을 위한 inclusion 인덱스 생성
CREATE TABLE reservations (
    id serial PRIMARY KEY,
    during daterange
);

CREATE INDEX idx_during ON reservations USING brin (during range_inclusion_ops);
```

---

#### 71.3.3 Bloom 연산자 클래스

등호 비교만 지원하고 해싱을 지원하는 데이터 타입을 위한 연산자 클래스임.

##### 필수 멤버

- 지원 프로시저 1: `brin_bloom_opcinfo()`
- 지원 프로시저 2: `brin_bloom_add_value()`
- 지원 프로시저 3: `brin_bloom_consistent()`
- 지원 프로시저 4: `brin_bloom_union()`
- 지원 프로시저 5: `brin_bloom_options()`
- 지원 프로시저 11: 해시 계산 함수
- 연산자 전략 1: 같음 (`=`)

지원 프로시저 11: 연산자 클래스와 동일한 데이터 타입의 인수 하나를 받아 해시 값을 반환함.

##### Bloom 연산자 클래스 사용 예제

```sql
-- Bloom 필터를 사용한 인덱스 생성
CREATE TABLE user_sessions (
    id serial PRIMARY KEY,
    session_id uuid,
    user_agent text
);

-- UUID에 bloom 연산자 클래스 사용
CREATE INDEX idx_session_bloom ON user_sessions
    USING brin (session_id uuid_bloom_ops);

-- 매개변수와 함께 사용
CREATE INDEX idx_session_bloom_custom ON user_sessions
    USING brin (
        session_id uuid_bloom_ops (
            n_distinct_per_range = 200,
            false_positive_rate = 0.02
        )
    );
```

---

#### 71.3.4 Minmax-Multi 연산자 클래스

완전 순서 집합을 위한 minmax의 확장판으로, 단일 연속 구간 대신 여러 개의 작은 구간을 저장함. 이상치(outlier) 값이 있는 데이터를 더 효과적으로 처리함.

##### 필수 멤버

- 지원 프로시저 1: `brin_minmax_multi_opcinfo()`
- 지원 프로시저 2: `brin_minmax_multi_add_value()`
- 지원 프로시저 3: `brin_minmax_multi_consistent()`
- 지원 프로시저 4: `brin_minmax_multi_union()`
- 지원 프로시저 5: `brin_minmax_multi_options()`
- 지원 프로시저 11: 거리 계산 함수 (범위 길이)
- 연산자 전략 1-5: `<`, `<=`, `=`, `>=`, `>`

##### Minmax vs Minmax-Multi 비교 예제

```sql
-- 이상치가 있는 데이터를 시뮬레이션
CREATE TABLE sales (
    id serial PRIMARY KEY,
    sale_date date,
    amount numeric
);

-- 대부분의 값은 특정 범위 내에 있지만 일부 이상치 존재
INSERT INTO sales (sale_date, amount)
SELECT
    '2024-01-01'::date + (random() * 30)::int,
    CASE
        WHEN random() < 0.95 THEN random() * 100  -- 일반 값: 0-100
        ELSE random() * 10000                      -- 이상치: 0-10000
    END
FROM generate_series(1, 100000);

-- 일반 minmax 인덱스 (이상치로 인해 효율성 저하)
CREATE INDEX idx_amount_minmax ON sales
    USING brin (amount numeric_minmax_ops);

-- minmax-multi 인덱스 (이상치를 더 잘 처리)
CREATE INDEX idx_amount_multi ON sales
    USING brin (amount numeric_minmax_multi_ops (values_per_range = 64));
```

---

### 교차 데이터 타입 연산자 (Cross-Data-Type Operators)

minmax와 inclusion 연산자 클래스 모두 교차 데이터 타입 연산자를 지원함:

- Minmax: 동일한 데이터 타입에 대한 전체 연산자 세트가 필요하며, 추가 데이터 타입용 연산자 세트를 별도로 정의할 수 있음
- Inclusion: 교차 타입 연산자를 사용하면 의존성이 더 복잡해짐

```sql
-- 예: float4_minmax_ops는 float4 비교 연산자뿐만 아니라
-- float8과의 비교 연산자도 지원할 수 있음
```

---

### BRIN 인덱스 장점과 제한사항

#### 장점

- 매우 작은 인덱스 크기
- 스캔 중 최소한의 오버헤드
- 테이블의 큰 부분 스캔 회피
- 자연적으로 정렬된 대용량 테이블에 이상적
- 사용자 정의 데이터 타입으로 확장 가능
- 자동 및 수동 요약화 옵션

#### 제한사항

- 손실 인덱스 (후보 튜플의 재확인 필요)
- 데이터가 물리적 위치와 상관관계가 있을 때만 효과적
- 무작위 데이터 분포에는 효과가 떨어짐

---

### 실제 사용 예제

#### 시계열 데이터에 BRIN 인덱스 사용

```sql
-- 로그 테이블 생성
CREATE TABLE application_logs (
    id bigserial PRIMARY KEY,
    log_time timestamptz NOT NULL,
    level text,
    message text
);

-- 시간 기반 BRIN 인덱스 생성 (자연스러운 시간순 삽입)
CREATE INDEX idx_log_time ON application_logs
    USING brin (log_time)
    WITH (pages_per_range = 128);

-- 쿼리 예제
EXPLAIN ANALYZE
SELECT * FROM application_logs
WHERE log_time BETWEEN '2024-01-01' AND '2024-01-31';
```

#### 파티션 테이블과 BRIN 인덱스

```sql
-- 파티션 테이블 생성
CREATE TABLE sensor_readings (
    sensor_id int,
    reading_time timestamptz,
    value numeric
) PARTITION BY RANGE (reading_time);

-- 파티션 생성
CREATE TABLE sensor_readings_2024_q1
    PARTITION OF sensor_readings
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

-- 각 파티션에 BRIN 인덱스 생성
CREATE INDEX idx_sensor_time ON sensor_readings_2024_q1
    USING brin (reading_time);
```

#### B-tree 대비 BRIN 인덱스 크기 비교

```sql
-- 테스트 테이블 생성
CREATE TABLE size_comparison (
    id serial PRIMARY KEY,
    created_at timestamptz DEFAULT now()
);

-- 데이터 삽입
INSERT INTO size_comparison (created_at)
SELECT timestamp '2024-01-01' + (i || ' seconds')::interval
FROM generate_series(1, 1000000) i;

-- B-tree 인덱스
CREATE INDEX idx_btree ON size_comparison USING btree (created_at);

-- BRIN 인덱스
CREATE INDEX idx_brin ON size_comparison USING brin (created_at);

-- 인덱스 크기 비교
SELECT
    indexrelid::regclass AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
WHERE relid = 'size_comparison'::regclass;
```

---

### 참고 자료

- [PostgreSQL 공식 문서: BRIN Indexes](https://www.postgresql.org/docs/current/brin.html)
- [PostgreSQL 공식 문서: CREATE INDEX](https://www.postgresql.org/docs/current/sql-createindex.html)

---

## Chapter 72: 해시 인덱스 (Hash Indexes)

PostgreSQL은 영구적이고 충돌 복구 가능한(crash-recoverable) 온디스크 해시 인덱스(hash index) 구현을 포함하고 있음. 해시 인덱스는 선형 순서가 잘 정의되지 않은 데이터 타입을 포함하여 모든 데이터 타입을 인덱싱할 수 있음.

### 72.1 개요 (Overview)

#### 72.1.1 기본 개념

해시 인덱스는 인덱싱된 컬럼 값에서 파생된 32비트 해시 코드를 저장함. 따라서 단순 동등 비교(equality comparison)만 처리할 수 있음.

```sql
-- 해시 인덱스 생성 구문
CREATE INDEX name ON table USING HASH (column);
```

#### 72.1.2 주요 특성

##### 저장 방식

- 저장 데이터: 실제 컬럼 값이 아닌 해시 값만 저장
- 해시 크기: 각 인덱스 튜플은 4바이트 해시 값 저장
- 인덱스 크기: UUID, URL 등 긴 데이터 항목 인덱싱 시 B-tree보다 훨씬 작음
- 스캔 특성: 모든 해시 인덱스 스캔은 손실성(lossy)

##### 지원 기능

- 단일 컬럼 인덱스만 지원
- 유일성 검사(uniqueness checking) 불가
- 동등 연산자(`=`)만 지원 (범위 연산 불가)
- 비트맵 인덱스 스캔(bitmap index scan) 참여 가능
- 역방향 스캔(backward scan) 지원

#### 72.1.3 제한 사항

해시 인덱스는 다음 연산에 사용할 수 없음:

- 범위 쿼리 (`<`, `<=`, `>=`, `>`)
- 패턴 매칭 (`LIKE`, `~`)
- `BETWEEN` 또는 `IN` 절
- `IS NULL` 또는 `IS NOT NULL` 조건
- 정렬/순서 연산
- 최근접 이웃 검색(nearest-neighbor search)

#### 72.1.4 성능 특성

##### B-tree와의 비교

B-tree 인덱스에서는 리프 페이지를 찾을 때까지 트리를 타고 내려가야 함. 수백만 행이 있는 테이블에서 이 하강(descent)은 데이터 접근 시간을 증가시킬 수 있음.

해시 인덱스에서 리프 페이지에 해당하는 것을 버킷 페이지(bucket page)라고 함. 해시 인덱스는 버킷 페이지에 직접 접근할 수 있어, 대용량 테이블에서 인덱스 접근 시간을 잠재적으로 줄일 수 있음.

```
B-tree 인덱스:  루트 -> 내부 노드 -> ... -> 리프 페이지
해시 인덱스:    메타페이지 -> 버킷 페이지 (직접 접근)
```

##### 최적 사용 사례

해시 인덱스는 다음 상황에 가장 적합함:

1. 유일하거나 거의 유일한 데이터
2. 해시 버킷당 행 수가 적은 데이터
3. 동등 스캔이 지배적인 대형 테이블
4. SELECT 및 UPDATE 중심 워크로드

##### 주의 사항

해시 값 분포가 고르지 않으면 오버플로우 페이지를 모두 스캔해야 함. 따라서 불균형 해시 인덱스는 일부 데이터에서 B-tree보다 더 많은 블록 접근이 필요할 수 있음.

완화 방법: 고유하지 않은 값이 많은 경우 부분 인덱스(partial index)를 사용하여 제외할 수 있음.

```sql
-- 부분 인덱스 예제: NULL이 아닌 값만 인덱싱
CREATE INDEX idx_email_hash ON users USING HASH (email)
WHERE email IS NOT NULL;
```

#### 72.1.5 버킷과 오버플로우 관리

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

#### 72.1.6 유지 관리 (Maintenance)

##### 튜플 삭제

B-tree와 마찬가지로 해시 인덱스는 단순 인덱스 튜플 삭제(지연 유지 관리)를 수행함:

- 삭제해도 안전한 것으로 알려진 인덱스 튜플 삭제 (LP_DEAD 비트가 이미 설정된 항목)
- 삽입에 공간이 필요할 때 또는 VACUUM 중에 데드 튜플 제거
- VACUUM은 튜플을 더 적은 페이지로 압축하여 오버플로우 체인 최소화 시도

##### 제한 사항

- 축소 방법 없음: REINDEX 외에는 해시 인덱스를 축소할 방법 없음
- 버킷 감소 불가: 버킷 수를 줄이는 방법 없음

#### 72.1.7 확장 (Expansion)

해시 인덱스는 인덱싱된 행 수가 증가함에 따라 버킷 페이지 수를 확장할 수 있음:

1. 새 버킷이 추가될 때 정확히 하나의 기존 버킷이 "분할(split)"됨
2. 일부 튜플이 업데이트된 키-버킷 번호 매핑에 따라 새 버킷으로 전송됨
3. 확장은 포그라운드에서 발생하여 삽입 실행 시간이 증가할 수 있음

```sql
-- 인덱스 상태 확인
SELECT
    indexrelname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE indexrelname LIKE '%hash%';
```

### 72.2 구현 (Implementation)

#### 72.2.1 페이지 유형

해시 인덱스는 네 가지 유형의 페이지로 구성됨:

- 메타 페이지 (Meta page): 페이지 0, 정적으로 할당된 제어 정보 포함
- 기본 버킷 페이지 (Primary bucket pages): 해시 버킷의 주요 저장소
- 오버플로우 페이지 (Overflow pages): 버킷이 용량을 초과할 때 추가 페이지
- 비트맵 페이지 (Bitmap pages): 재사용 가능한 해제된 오버플로우 페이지 추적

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

#### 72.2.2 캐싱 전략

모든 작업마다 메타페이지를 잠그고 고정(pin)하는 성능 페널티를 피하기 위해:

- 메타페이지 정보가 각 백엔드의 relcache 항목에 캐시됨
- 필요한 데이터: 버킷 카운트, highmask, lowmask
- 대상 버킷이 마지막 캐시 갱신 이후 분할되지 않았다면 올바른 버킷 매핑 유지

#### 72.2.3 튜플 구성

- 인덱싱된 테이블의 각 행은 단일 인덱스 튜플로 표현됨
- 해시 인덱스 튜플은 버킷 및 오버플로우 페이지에 저장됨

##### 페이지 내 정렬

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

#### 72.2.4 저장소 할당

- 기본 버킷 페이지와 오버플로우 페이지는 독립적으로 할당됨
- 이를 통해 오버플로우 페이지와 버킷 비율에 유연성을 제공
- 기본 버킷 페이지를 이동하지 않고 가변적인 오버플로우 페이지를 지원하는 특수 주소 지정 규칙 사용

#### 72.2.5 버킷 분할

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

### 72.3 내장 연산자 클래스 (Built-in Operator Classes)

#### 72.3.1 해시 인덱스 전략

해시 인덱스는 동등 비교만 지원하므로 단일 전략만 정의함:

- equal (`=`): 1

#### 72.3.2 해시 지원 함수

해시 인덱스는 하나의 필수 지원 함수와 두 개의 선택적 함수를 요구함:

- 32비트 해시 값 계산
  - 지원 번호: 1
  - 설명: 필수
- 64비트 솔트로 64비트 해시 값 계산
  - 지원 번호: 2
  - 설명: 선택적; 솔트가 0이면 하위 32비트는 함수 1의 결과와 일치해야 함
- 연산자 클래스별 옵션 정의
  - 지원 번호: 3
  - 설명: 선택적

#### 72.3.3 해시 연산자 클래스 조회

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

#### 72.3.4 주요 내장 해시 연산자 클래스

다음은 PostgreSQL에서 제공하는 주요 해시 연산자 클래스임:

- `int2_ops`
  - 인덱싱 타입: smallint
  - 연산자: `=`
- `int4_ops`
  - 인덱싱 타입: integer
  - 연산자: `=`
- `int8_ops`
  - 인덱싱 타입: bigint
  - 연산자: `=`
- `float4_ops`
  - 인덱싱 타입: real
  - 연산자: `=`
- `float8_ops`
  - 인덱싱 타입: double precision
  - 연산자: `=`
- `numeric_ops`
  - 인덱싱 타입: numeric
  - 연산자: `=`
- `text_ops`
  - 인덱싱 타입: text
  - 연산자: `=`
- `varchar_ops`
  - 인덱싱 타입: varchar
  - 연산자: `=`
- `char_ops`
  - 인덱싱 타입: char
  - 연산자: `=`
- `bpchar_ops`
  - 인덱싱 타입: bpchar
  - 연산자: `=`
- `bytea_ops`
  - 인덱싱 타입: bytea
  - 연산자: `=`
- `date_ops`
  - 인덱싱 타입: date
  - 연산자: `=`
- `time_ops`
  - 인덱싱 타입: time
  - 연산자: `=`
- `timetz_ops`
  - 인덱싱 타입: timetz
  - 연산자: `=`
- `timestamp_ops`
  - 인덱싱 타입: timestamp
  - 연산자: `=`
- `timestamptz_ops`
  - 인덱싱 타입: timestamptz
  - 연산자: `=`
- `interval_ops`
  - 인덱싱 타입: interval
  - 연산자: `=`
- `uuid_ops`
  - 인덱싱 타입: uuid
  - 연산자: `=`
- `oid_ops`
  - 인덱싱 타입: oid
  - 연산자: `=`
- `bool_ops`
  - 인덱싱 타입: boolean
  - 연산자: `=`
- `macaddr_ops`
  - 인덱싱 타입: macaddr
  - 연산자: `=`
- `inet_ops`
  - 인덱싱 타입: inet
  - 연산자: `=`
- `cidr_ops`
  - 인덱싱 타입: cidr
  - 연산자: `=`

#### 72.3.5 다중 데이터 타입 해시 패밀리

여러 데이터 타입으로 해시 연산자 패밀리를 구축할 때:

- 지원되는 각 데이터 타입에 대해 호환 가능한 해시 지원 함수 생성 필요
- 패밀리의 동등 연산자가 같다고 간주하는 값은 타입이 달라도 동일한 해시 코드를 반환해야 함
- 연산자 패밀리 내 데이터 타입 간에 암시적 또는 이진 강제 변환(binary coercion)이 발생해도 해시 값이 변경되면 안 됨

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

### 72.4 실용적인 예제

#### 72.4.1 해시 인덱스 생성 및 사용

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

#### 72.4.2 실행 계획 확인

```sql
-- 해시 인덱스 사용 확인
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users WHERE email = 'user@example.com';

-- 예상 출력:
-- Index Scan using idx_users_email_hash on users
--   Index Cond: ((email)::text = 'user@example.com'::text)
--   Buffers: shared hit=3
```

#### 72.4.3 B-tree vs Hash 인덱스 비교

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

#### 72.4.4 부분 해시 인덱스

```sql
-- 활성 사용자만 인덱싱
CREATE INDEX idx_active_users_hash ON users USING HASH (email)
WHERE is_active = true;

-- 특정 조건의 데이터만 인덱싱
CREATE INDEX idx_premium_sessions ON users USING HASH (session_token)
WHERE subscription_type = 'premium';
```

#### 72.4.5 해시 인덱스 유지 관리

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

### 72.5 해시 인덱스 선택 가이드

#### 72.5.1 해시 인덱스를 사용해야 할 때

- 동등 비교(`=`)만 사용: 해시 인덱스의 유일한 지원 연산
- 긴 값 인덱싱 (UUID, URL 등): 4바이트 해시만 저장하여 공간 절약
- 대용량 테이블: 직접 버킷 접근으로 빠른 조회
- 유일하거나 거의 유일한 데이터: 오버플로우 페이지 최소화

#### 72.5.2 B-tree를 사용해야 할 때

- 범위 쿼리 필요: B-tree만 `<`, `>`, `BETWEEN` 지원
- 정렬 필요: 해시 인덱스는 순서 정보 없음
- 유일성 제약 필요: 해시 인덱스는 유일성 검사 불가
- 다중 컬럼 인덱스 필요: 해시는 단일 컬럼만 지원

#### 72.5.3 의사 결정 플로우차트

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

### 72.6 참고 자료

- PostgreSQL 공식 문서: [Hash Indexes](https://www.postgresql.org/docs/current/hash-index.html)
- PostgreSQL 소스 코드: `src/backend/access/hash/README`
- [Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- [Operator Classes and Operator Families](https://www.postgresql.org/docs/current/indexes-opclass.html)
