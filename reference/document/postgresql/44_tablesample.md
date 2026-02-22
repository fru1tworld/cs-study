# 제60장. 테이블 샘플링 메서드 작성 (Writing a Table Sampling Method)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/tablesample-method.html](https://www.postgresql.org/docs/current/tablesample-method.html)

---

## 목차

- [개요](#개요)
- [60.1. 샘플링 메서드 지원 함수](#601-샘플링-메서드-지원-함수)
  - [60.1.1. SampleScanGetSampleSize](#6011-samplescangetsamplesize)
  - [60.1.2. InitSampleScan](#6012-initsamplescan)
  - [60.1.3. BeginSampleScan](#6013-beginsamplescan)
  - [60.1.4. NextSampleBlock](#6014-nextsampleblock)
  - [60.1.5. NextSampleTuple](#6015-nextsampletuple)
  - [60.1.6. EndSampleScan](#6016-endsamplescan)

---

## 개요

PostgreSQL의 `TABLESAMPLE` 절은 표준으로 제공되는 `BERNOULLI`와 `SYSTEM` 메서드 외에도 사용자 정의 테이블 샘플링 메서드 를 지원합니다. 샘플링 메서드는 `TABLESAMPLE` 절을 사용할 때 어떤 행이 선택될지를 결정합니다.

### SQL 함수 시그니처

SQL 수준에서 테이블 샘플링 메서드는 다음과 같은 시그니처를 가진 단일 SQL 함수로 표현됩니다:

```sql
method_name(internal) RETURNS tsm_handler
```

이 함수의 특징은 다음과 같습니다:

| 특성 | 설명 |
|------|------|
| 함수명 | `TABLESAMPLE` 절에서 사용할 메서드 이름과 동일해야 함 |
| `internal` 인자 | SQL에서 직접 호출을 방지하기 위한 더미 인자 (항상 0) |
| 반환 타입 | `palloc`된 `TsmRoutine` 타입 구조체에 대한 포인터 |

### TsmRoutine 구조체

반환되는 `TsmRoutine` 구조체는 샘플링 메서드에 필요한 지원 함수들에 대한 포인터를 포함해야 합니다. 이 지원 함수들은 일반 C 함수로서, SQL에서 직접 호출할 수 없습니다.

#### 구조체 필드

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

#### 주요 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `parameterTypes` | `List *` | `TABLESAMPLE` 절에서 받는 파라미터 데이터 타입의 OID 리스트. 내장 메서드들은 샘플링 백분율을 위해 `FLOAT4OID` 사용 |
| `repeatable_across_queries` | `bool` | `true`인 경우: 동일한 파라미터와 `REPEATABLE` 시드를 사용하면 쿼리마다 동일한 샘플 반환. `false`인 경우: `REPEATABLE` 절을 허용하지 않음 |
| `repeatable_across_scans` | `bool` | `true`인 경우: 동일 쿼리 내 연속 스캔에서 동일한 샘플 반환. `false`인 경우: 플래너가 다중 스캔을 방지하여 불일치 출력 회피 |

### 참조 파일

| 파일 경로 | 설명 |
|-----------|------|
| `src/include/access/tsmapi.h` | `TsmRoutine` 타입 정의 |
| `src/backend/access/tablesample/` | 내장 샘플링 메서드 구현 예제 |
| `contrib/` | 확장 샘플링 메서드 예제 (예: `tsm_system_rows`, `tsm_system_time`) |

---

## 60.1. 샘플링 메서드 지원 함수

TSM(Table Sampling Method) 핸들러 함수는 지원 함수에 대한 포인터를 포함하는 `TsmRoutine` 구조체를 반환합니다. 대부분의 함수는 필수이지만, 일부는 선택 사항으로 `NULL`로 설정할 수 있습니다.

### 지원 함수 요약

| 함수 | 필수 여부 | 용도 |
|------|-----------|------|
| `SampleScanGetSampleSize` | 필수 | 계획(Planning) 단계에서 스캔 크기 추정 |
| `InitSampleScan` | 선택 | 실행기(Executor) 시작 시 초기화 |
| `BeginSampleScan` | 필수 | 샘플링 스캔 실행 시작 |
| `NextSampleBlock` | 선택 | 다음 스캔 페이지 반환 |
| `NextSampleTuple` | 필수 | 다음 샘플 튜플 반환 |
| `EndSampleScan` | 선택 | 스캔 종료 및 리소스 해제 |

---

### 60.1.1. SampleScanGetSampleSize

```c
void
SampleScanGetSampleSize(PlannerInfo *root,
                        RelOptInfo *baserel,
                        List *paramexprs,
                        BlockNumber *pages,
                        double *tuples);
```

#### 목적

계획(Planning) 단계에서 호출되어 스캔 크기를 추정합니다.

#### 설명

- 샘플 스캔 중 읽을 릴레이션 페이지 수 를 추정
- 선택될 튜플 수 를 추정
- 일반적으로 `baserel->pages`와 `baserel->tuples`에 샘플링 비율을 곱하여 계산
- 결과는 반드시 정수값으로 반올림 해야 함

#### 파라미터

| 파라미터 | 설명 |
|----------|------|
| `root` | 플래너 정보 구조체 |
| `baserel` | 샘플링 대상 릴레이션의 최적화 정보 |
| `paramexprs` | `TABLESAMPLE` 절 파라미터를 나타내는 표현식 리스트 |
| `pages` | 출력 파라미터: 읽을 페이지 수 |
| `tuples` | 출력 파라미터: 선택할 튜플 수 |

#### 구현 시 주의사항

- `estimate_expression_value()` 함수를 사용하여 표현식을 상수로 변환 시도
- 값을 상수로 변환할 수 없거나 유효하지 않은 값이라도 반드시 추정치를 제공 해야 함
- 합리적인 기본값 사용 권장

#### 예제 코드

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

### 60.1.2. InitSampleScan

```c
void
InitSampleScan(SampleScanState *node,
               int eflags);
```

#### 목적

실행기(Executor) 시작 시 샘플 스캔에 필요한 초기화를 수행합니다.

#### 설명

- 처리 시작 전에 필요한 초기화 작업 수행
- `SampleScanState` 노드는 이미 생성되어 있으나, `tsm_state` 필드는 `NULL`
- 내부 상태 데이터를 `palloc`하여 `node->tsm_state`에 저장 가능
- 테이블 정보는 `SampleScanState`의 다른 필드를 통해 접근 가능
- `node->ss.ss_currentScanDesc` 스캔 디스크립터는 아직 설정되지 않음

#### 파라미터

| 파라미터 | 설명 |
|----------|------|
| `node` | 샘플 스캔 상태 노드 |
| `eflags` | 실행기 동작 모드 플래그 |

#### `eflags` 처리

```c
if (eflags & EXEC_FLAG_EXPLAIN_ONLY)
{
    /* EXPLAIN만 수행할 경우, 최소한의 작업만 수행 */
    return;
}
```

#### 선택 사항

이 함수는 선택 사항 입니다. `NULL`로 설정 시 `BeginSampleScan`이 모든 초기화를 수행해야 합니다.

---

### 60.1.3. BeginSampleScan

```c
void
BeginSampleScan(SampleScanState *node,
                Datum *params,
                int nparams,
                uint32 seed);
```

#### 목적

샘플링 스캔 실행을 시작합니다.

#### 설명

- 첫 번째 튜플 가져오기 직전 에 호출
- 스캔 재시작 시 에도 호출
- 테이블 정보는 `SampleScanState` 필드를 통해 접근 가능

#### 파라미터

| 파라미터 | 설명 |
|----------|------|
| `node` | 샘플 스캔 상태 노드 |
| `params` | `TABLESAMPLE` 절 파라미터 값 배열 |
| `nparams` | 파라미터 개수 |
| `seed` | 난수 시드 (`REPEATABLE` 값의 해시 또는 `random()` 결과) |

#### 설정 가능한 동작

| 필드 | 기본값 | 설명 |
|------|--------|------|
| `node->use_bulkread` | `true` | 버퍼 재활용을 권장. 작은 테이블 비율을 읽을 때는 `false`로 설정 |
| `node->use_pagemode` | `true` | 페이지당 단일 패스로 가시성 검사 수행. 작은 튜플 비율만 선택 시 `false`로 설정 |

#### 반복 가능성 요구사항

`repeatable_across_scans`가 `true`로 설정된 경우:

- 재스캔 시 동일한 튜플 집합 을 선택해야 함
- `TABLESAMPLE` 파라미터와 시드가 변경되지 않으면 새 `BeginSampleScan` 호출도 동일한 튜플 선택

#### 예제 코드

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

### 60.1.4. NextSampleBlock

```c
BlockNumber
NextSampleBlock(SampleScanState *node,
                BlockNumber nblocks);
```

#### 목적

스캔할 다음 페이지(블록)를 반환합니다.

#### 설명

- 스캔할 다음 블록 번호 를 반환
- 남은 페이지가 없으면 `InvalidBlockNumber` 반환

#### 파라미터

| 파라미터 | 설명 |
|----------|------|
| `node` | 샘플 스캔 상태 노드 |
| `nblocks` | 릴레이션의 총 블록 수 |

#### 선택 사항

이 함수는 선택 사항 입니다. `NULL`로 설정 시:

- 코어 코드가 전체 릴레이션 순차 스캔 수행
- 동기화된 스캔(Synchronized Scanning) 사용
- 스캔 순서가 보장되지 않음

#### 예제 코드

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

### 60.1.5. NextSampleTuple

```c
OffsetNumber
NextSampleTuple(SampleScanState *node,
                BlockNumber blockno,
                OffsetNumber maxoffset);
```

#### 목적

페이지에서 샘플링할 다음 튜플을 반환합니다.

#### 설명

- 지정된 페이지에서 샘플링할 다음 튜플의 오프셋 번호 반환
- 남은 튜플이 없으면 `InvalidOffsetNumber` 반환
- `maxoffset`은 페이지에서 사용 중인 가장 큰 오프셋 번호

#### 파라미터

| 파라미터 | 설명 |
|----------|------|
| `node` | 샘플 스캔 상태 노드 |
| `blockno` | 현재 스캔 중인 블록 번호 |
| `maxoffset` | 페이지의 최대 오프셋 번호 |

#### 중요 참고사항

1. 유효한 튜플 정보 미제공: 1부터 `maxoffset` 사이의 어떤 오프셋이 유효한 튜플을 포함하는지 명시적으로 알려주지 않음
2. 누락/비가시 튜플 처리: 코어 코드가 누락되거나 비가시적인 튜플 요청을 무시 (편향 없음)
3. 튜플 유효성 확인: 필요시 `node->donetuples`로 반환된 튜플의 유효성 검사 가능
4. 블록 번호 가정 금지: `blockno`가 가장 최근 `NextSampleBlock` 호출의 결과라고 가정하면 안 됨 (프리페칭 발생 가능)
5. 페이지 내 연속성 보장: 페이지 샘플링 시작 후 `InvalidOffsetNumber` 반환 전까지 연속 호출은 동일 페이지 참조

#### 예제 코드

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

### 60.1.6. EndSampleScan

```c
void
EndSampleScan(SampleScanState *node);
```

#### 목적

스캔을 종료하고 리소스를 해제합니다.

#### 설명

- 외부에서 볼 수 있는 리소스 정리
- `palloc`된 메모리 정리는 일반적으로 중요하지 않음 (메모리 컨텍스트에서 자동 정리)
- 파일 핸들, 외부 연결 등의 리소스 해제에 사용

#### 선택 사항

이 함수는 선택 사항 입니다. 외부 리소스가 없다면 `NULL`로 설정 가능합니다.

#### 예제 코드

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

## 완전한 예제: 사용자 정의 샘플링 메서드

다음은 사용자 정의 테이블 샘플링 메서드의 완전한 구현 예제입니다.

### SQL 정의

```sql
-- 샘플링 메서드 핸들러 함수 생성
CREATE FUNCTION my_sample_method(internal)
RETURNS tsm_handler
AS 'my_sample_extension', 'my_sample_method_handler'
LANGUAGE C STRICT;

-- 사용 예제
SELECT * FROM my_table TABLESAMPLE my_sample_method(10.0) REPEATABLE (42);
```

### C 구현

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

## 관련 참고 자료

### 내장 샘플링 메서드

| 메서드 | 설명 | 소스 파일 |
|--------|------|-----------|
| `BERNOULLI` | 각 튜플을 독립적으로 샘플링 | `src/backend/access/tablesample/bernoulli.c` |
| `SYSTEM` | 블록 단위로 샘플링 | `src/backend/access/tablesample/system.c` |

### 확장 샘플링 메서드 (contrib)

| 확장 | 설명 |
|------|------|
| `tsm_system_rows` | 지정된 행 수만큼 샘플링 |
| `tsm_system_time` | 지정된 시간 동안 샘플링 |

### 주요 헤더 파일

- `src/include/access/tsmapi.h` - TSM API 정의
- `src/include/access/relscan.h` - 릴레이션 스캔 구조체
- `src/include/utils/sampling.h` - 샘플링 유틸리티 함수

---

## 요약

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
