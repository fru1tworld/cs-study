# Chapter 61: 커스텀 스캔 프로바이더 작성하기 (Writing a Custom Scan Provider)

## 개요 (Overview)

PostgreSQL은 확장 모듈(Extension Module)이 시스템에 새로운 스캔 타입을 추가할 수 있도록 하는 실험적 기능을 제공합니다. 외부 데이터 래퍼(Foreign Data Wrapper)가 자체 외부 테이블만 처리하는 것과 달리, 커스텀 스캔 프로바이더(Custom Scan Provider)는 시스템의 모든 릴레이션 에 대해 대안적인 스캔 방법을 제공할 수 있습니다.

### 외부 데이터 래퍼와의 차이점

| 구분 | 외부 데이터 래퍼 (FDW) | 커스텀 스캔 프로바이더 |
|------|------------------------|------------------------|
| 적용 범위 | 자체 외부 테이블만 스캔 | 시스템의 모든 릴레이션에 대해 스캔 가능 |
| 유연성 | 제한적 | 높음 |

### 주요 사용 사례 (Use Cases)

커스텀 스캔 프로바이더는 코어 시스템에서 지원하지 않는 최적화를 구현해야 할 때 주로 사용됩니다:

- 캐싱 전략 (Caching Strategies): 사용자 정의 캐싱 메커니즘 구현
- 하드웨어 가속 (Hardware Acceleration): GPU나 특수 하드웨어를 활용한 스캔
- 커스텀 인덱싱 방법 (Custom Indexing Methods): 새로운 인덱스 구조 활용

---

## 구현 프로세스 (Implementation Process)

커스텀 스캔 프로바이더 작성은 3단계 프로세스 로 이루어집니다:

```
1. 계획 단계 (Planning Phase)
   └── 제안된 전략을 사용하는 스캔을 나타내는 접근 경로(Access Path) 생성

2. 계획 선택 (Plan Selection)
   └── 플래너가 해당 접근 경로를 최적으로 선택하면 이를 계획(Plan)으로 변환

3. 실행 단계 (Execution Phase)
   └── 계획을 실행하고 동일 릴레이션에 대한 다른 접근 경로와 일치하는 결과 생성
```

---

## 61.1 커스텀 스캔 경로 (Custom Scan Paths)

커스텀 스캔 프로바이더는 코어 시스템의 접근 경로 생성이 완료된 후 호출되는 훅(Hook)을 통해 베이스 릴레이션(Base Relation)에 대한 경로를 추가합니다.

### 베이스 릴레이션 훅 (Base Relation Hook)

```c
typedef void (*set_rel_pathlist_hook_type) (PlannerInfo *root,
                                            RelOptInfo *rel,
                                            Index rti,
                                            RangeTblEntry *rte);
extern PGDLLIMPORT set_rel_pathlist_hook_type set_rel_pathlist_hook;
```

이 훅이 호출되면, 커스텀 스캔 프로바이더는 일반적으로 `CustomPath` 객체를 생성하고 `add_path()` 또는 `add_partial_path()` 함수를 사용하여 추가합니다.

### 조인 릴레이션 훅 (Join Relation Hook)

```c
typedef void (*set_join_pathlist_hook_type) (PlannerInfo *root,
                                             RelOptInfo *joinrel,
                                             RelOptInfo *outerrel,
                                             RelOptInfo *innerrel,
                                             JoinType jointype,
                                             JoinPathExtraData *extra);
extern PGDLLIMPORT set_join_pathlist_hook_type set_join_pathlist_hook;
```

> 중요: `CustomPath`는 `extra->restrictlist`에서 사용하는 조인 절(Join Clauses)의 집합을 포함해야 합니다.

### CustomPath 데이터 구조

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

#### 필드 설명

| 필드 | 설명 |
|------|------|
| `path` | 표준 경로 초기화 (행 수, 비용 추정, 정렬 순서 포함) |
| `flags` | 기능을 지정하는 비트 마스크 |
| `custom_paths` | 이 경로에서 사용하는 `Path` 노드 목록 (나중에 `Plan` 노드로 변환됨) |
| `custom_restrictinfo` | 조인 릴레이션의 조인 절 (베이스 릴레이션의 경우 NIL) |
| `custom_private` | 프로바이더의 프라이빗 데이터 (`nodeToString` 호환 필요) |
| `methods` | `CustomPathMethods` 구현에 대한 포인터 |

#### flags 비트 마스크 옵션

| 플래그 | 설명 |
|--------|------|
| `CUSTOMPATH_SUPPORT_BACKWARD_SCAN` | 역방향 스캔 지원 |
| `CUSTOMPATH_SUPPORT_MARK_RESTORE` | mark/restore 작업 지원 |
| `CUSTOMPATH_SUPPORT_PROJECTION` | 스칼라 표현식 평가 가능 |

### 커스텀 스캔 경로 콜백 (Custom Scan Path Callbacks)

#### PlanCustomPath

```c
Plan *(*PlanCustomPath) (PlannerInfo *root,
                         RelOptInfo *rel,
                         CustomPath *best_path,
                         List *tlist,
                         List *clauses,
                         List *custom_plans);
```

커스텀 경로를 완성된 `CustomScan` 계획으로 변환합니다.

- `root`: 플래너 정보
- `rel`: 릴레이션 최적화 정보
- `best_path`: 선택된 최적 경로
- `tlist`: 타겟 리스트
- `clauses`: 적용할 절(Clauses)
- `custom_plans`: 커스텀 계획 목록

#### ReparameterizeCustomPathByChild

```c
List *(*ReparameterizeCustomPathByChild) (PlannerInfo *root,
                                          List *custom_private,
                                          RelOptInfo *child_rel);
```

부모에서 자식 릴레이션 매개변수화로 변환할 때 경로를 재매개변수화합니다.

- `reparameterize_path_by_child()` 및 `adjust_appendrel_attrs()` 헬퍼 함수 사용 가능

### 구현 시 주요 사항

1. 커스텀 경로는 베이스 릴레이션과 조인 릴레이션 모두에 대해 생성 가능
2. 훅은 코어 시스템 경로 생성 후 호출됨 (Gather/Gather Merge 제외)
3. 조인 훅은 서로 다른 내부/외부 릴레이션 조합으로 여러 번 호출될 수 있음
4. 프로바이더는 훅 콜백에서 중복 작업을 최소화해야 함
5. 프라이빗 데이터는 디버깅을 위해 `nodeToString`으로 직렬화 가능해야 함

---

## 61.2 커스텀 스캔 계획 (Custom Scan Plans)

커스텀 스캔 계획(Custom Scan Plan)은 커스텀 스캔을 위한 완성된 계획 트리 구조를 나타냅니다.

### CustomScan 데이터 구조

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

#### 필드 설명

| 필드 | 용도 |
|------|------|
| `scan` | 표준 스캔 구조 (예상 비용, 타겟 리스트, 자격 조건 포함) |
| `flags` | `CustomPath`와 동일한 의미의 비트 마스크 |
| `custom_plans` | 자식 `Plan` 노드 저장 |
| `custom_exprs` | `setrefs.c`와 `subselect.c`에 의해 수정이 필요한 표현식 트리 |
| `custom_private` | 커스텀 스캔 프로바이더만 사용하는 프라이빗 데이터 |
| `custom_scan_tlist` | 실제 스캔 튜플을 설명하는 타겟 리스트 (베이스 릴레이션 스캔의 경우 NIL, 조인의 경우 필수) |
| `custom_relids` | 이 스캔 노드가 처리하는 릴레이션 집합 (범위 테이블 인덱스) |
| `methods` | 필수 커스텀 스캔 메서드를 구현하는 객체에 대한 포인터 |

### 주요 요구사항

| 상황 | 요구사항 |
|------|----------|
| 단일 릴레이션 스캔 | `scan.scanrelid`는 스캔할 테이블의 범위 테이블 인덱스여야 함 |
| 조인 대체 | `scan.scanrelid`는 0이어야 함 |
| 데이터 복제 | "custom" 필드의 모든 데이터는 `copyObject`가 처리할 수 있는 노드로 구성되어야 함 |

> 주의: `CustomPath`나 `CustomScanState`와 달리, `CustomScan`을 포함하는 더 큰 구조체로 대체할 수 없습니다.

### 커스텀 스캔 계획 콜백 (Custom Scan Plan Callbacks)

#### CreateCustomScanState

```c
Node *(*CreateCustomScanState) (CustomScan *cscan);
```

`CustomScan`에 대한 `CustomScanState`를 할당합니다.

요구사항:
- 할당은 프로바이더별 구조체에 포함시키기 위해 `CustomScanState`보다 클 수 있음
- 노드 태그와 `methods` 필드가 적절히 설정되어야 함
- 다른 필드는 0으로 초기화되어야 함
- `ExecInitCustomScan`이 기본 초기화를 수행한 후, `BeginCustomScan` 콜백이 추가 프로바이더별 설정을 위해 호출됨

---

## 61.3 커스텀 스캔 실행 (Executing Custom Scans)

`CustomScan`이 실행될 때, 실행 상태는 `CustomScanState` 구조로 표현됩니다.

### CustomScanState 데이터 구조

```c
typedef struct CustomScanState
{
    ScanState ss;
    uint32    flags;
    const CustomExecMethods *methods;
} CustomScanState;
```

#### 필드 설명

| 필드 | 설명 |
|------|------|
| `ss` | 표준 스캔 상태 초기화 (베이스 릴레이션용; 조인 스캔의 경우 `ss.ss_currentRelation`은 NULL) |
| `flags` | `CustomPath` 및 `CustomScan`과 동일한 의미의 비트 마스크 |
| `methods` | 커스텀 스캔 상태 메서드를 구현하는 정적으로 할당된 객체에 대한 포인터 |

### 필수 콜백 (Required Callbacks)

#### BeginCustomScan

```c
void (*BeginCustomScan) (CustomScanState *node, EState *estate, int eflags);
```

`CustomScanState`의 초기화를 완료합니다. 프라이빗 필드를 여기서 초기화하세요.

- `node`: 커스텀 스캔 상태
- `estate`: 실행 상태
- `eflags`: 실행 플래그

#### ExecCustomScan

```c
TupleTableSlot *(*ExecCustomScan) (CustomScanState *node);
```

다음 스캔 튜플을 가져옵니다.

- 현재 스캔 방향에서 다음 튜플로 `ps_ResultTupleSlot`을 채움
- 튜플이 더 이상 없으면 NULL 또는 빈 슬롯을 반환

#### EndCustomScan

```c
void (*EndCustomScan) (CustomScanState *node);
```

`CustomScanState`와 연관된 프라이빗 데이터를 정리합니다.

- 필수 콜백이지만, 정리할 내용이 없으면 아무것도 하지 않을 수 있음

#### ReScanCustomScan

```c
void (*ReScanCustomScan) (CustomScanState *node);
```

현재 스캔을 처음으로 되감고 릴레이션을 다시 스캔할 준비를 합니다.

### 선택적 콜백 (Optional Callbacks)

#### Mark/Restore 지원

```c
void (*MarkPosCustomScan) (CustomScanState *node);
void (*RestrPosCustomScan) (CustomScanState *node);
```

스캔 위치를 저장하고 복원합니다.

> 주의: `CUSTOMPATH_SUPPORT_MARK_RESTORE` 플래그가 설정된 경우에만 필요합니다.

#### 병렬 실행 지원 (Parallel Execution Support)

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

| 콜백 | 용도 |
|------|------|
| `EstimateDSMCustomScan` | 동적 공유 메모리(DSM) 크기 추정 |
| `InitializeDSMCustomScan` | DSM 초기화 |
| `ReInitializeDSMCustomScan` | DSM 재초기화 |
| `InitializeWorkerCustomScan` | 워커 프로세스 초기화 |

> 주의: 병렬 실행 지원이 필요한 경우에만 구현합니다.

#### ShutdownCustomScan

```c
void (*ShutdownCustomScan) (CustomScanState *node);
```

노드가 완료되지 않을 때 리소스를 해제합니다.

- DSM 세그먼트 파괴 전 정리에 유용

#### ExplainCustomScan

```c
void (*ExplainCustomScan) (CustomScanState *node,
                            List *ancestors,
                            ExplainState *es);
```

표준 스캔 상태 데이터 외에 추가 `EXPLAIN` 정보를 출력하기 위한 선택적 콜백입니다.

---

## 예제: 간단한 커스텀 스캔 프로바이더 구현

아래는 커스텀 스캔 프로바이더의 기본 구조를 보여주는 예제입니다.

### 1. 헤더 및 구조체 정의

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

### 2. 경로 메서드 정의

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

### 3. 스캔 메서드 정의

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

### 4. 실행 메서드 정의

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

### 5. 훅 등록

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

## 설계 시 고려사항

### 성능 최적화

1. 비용 추정 정확성: 플래너가 올바른 결정을 내리도록 정확한 비용 추정 제공
2. 병렬 처리: 가능한 경우 병렬 실행 지원 구현
3. 메모리 관리: 적절한 메모리 컨텍스트 사용

### 호환성

1. 버전 호환성: PostgreSQL 버전 간 API 변경 사항 확인
2. 직렬화: `custom_private` 데이터는 `nodeToString` 호환 필요
3. 복사 가능성: 모든 커스텀 데이터는 `copyObject` 처리 가능해야 함

### 디버깅

1. EXPLAIN 지원: `ExplainCustomScan` 콜백을 통한 상세 정보 제공
2. 로깅: 적절한 디버그 로깅 구현
3. 오류 처리: 명확한 오류 메시지 제공

---

## 참고 자료

- [PostgreSQL 공식 문서 - Writing a Custom Scan Provider](https://www.postgresql.org/docs/current/custom-scan.html)
- [PostgreSQL 공식 문서 - Custom Scan Paths](https://www.postgresql.org/docs/current/custom-scan-path.html)
- [PostgreSQL 공식 문서 - Custom Scan Plans](https://www.postgresql.org/docs/current/custom-scan-plan.html)
- [PostgreSQL 공식 문서 - Executing Custom Scans](https://www.postgresql.org/docs/current/custom-scan-execution.html)
