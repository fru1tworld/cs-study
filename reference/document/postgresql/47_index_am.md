# Chapter 64: 인덱스 접근 메서드 인터페이스 (Index Access Method Interface Definition)

이 문서는 PostgreSQL 핵심 시스템과 인덱스 접근 메서드(Index Access Methods) 간의 인터페이스를 정의합니다. 핵심 시스템은 이 인터페이스에서 지정한 사항 이외의 인덱스에 대해 아무것도 알지 못하므로, 추가 코드를 작성하여 완전히 새로운 인덱스 유형을 개발할 수 있습니다.

## 목차

1. [인덱스 접근 메서드 개요](#1-인덱스-접근-메서드-개요)
2. [인덱스 기본 API 구조](#2-인덱스-기본-api-구조-basic-api-structure-for-indexes)
3. [인덱스 접근 메서드 함수](#3-인덱스-접근-메서드-함수-index-access-method-functions)
4. [인덱스 스캔](#4-인덱스-스캔-index-scanning)
5. [인덱스 잠금 고려사항](#5-인덱스-잠금-고려사항-index-locking-considerations)
6. [인덱스 고유성 검사](#6-인덱스-고유성-검사-index-uniqueness-checks)
7. [인덱스 비용 추정 함수](#7-인덱스-비용-추정-함수-index-cost-estimation-functions)

---

## 1. 인덱스 접근 메서드 개요

### 1.1 보조 인덱스 (Secondary Indexes)

PostgreSQL의 모든 인덱스는 보조 인덱스(secondary indexes) 입니다. 이는 인덱스가 테이블 파일과 물리적으로 분리되어 있음을 의미합니다. 각 인덱스는 자체 물리적 릴레이션(relation) 으로 저장되며, `pg_class` 시스템 카탈로그에 별도의 엔트리를 가집니다.

### 1.2 인덱스의 역할

인덱스의 주요 역할은 데이터 키 값에서 TID(Tuple Identifiers, 튜플 식별자) 로의 매핑을 제공하는 것입니다:

- TID 구성: 블록 번호(block number)와 해당 블록 내의 항목 번호(item number)
- 목적: 테이블에서 특정 행 버전(row version)을 가져오는 데 필요한 정보 제공

### 1.3 저장소 구조

모든 인덱스 접근 메서드는 다음과 같은 저장소 구조를 따릅니다:

- 인덱스를 표준 크기 페이지(standard-size pages) 로 분할
- 일반 저장소 관리자(storage manager) 및 버퍼 관리자(buffer manager) 사용
- 기존 인덱스 접근 메서드들은 표준 페이지 레이아웃 사용

### 1.4 MVCC와 인덱스

MVCC(Multi-Version Concurrency Control) 환경에서의 인덱스 동작:

```
동일한 논리적 행 → 여러 물리적 버전 존재 가능
인덱스는 각 튜플을 독립적인 객체로 취급
```

- 행 업데이트 시 키 값이 변경되지 않아도 새로운 인덱스 엔트리 생성
- HOT(Heap-Only Tuples) 예외: HOT 튜플은 인덱스 처리 대상이 아님

---

## 2. 인덱스 기본 API 구조 (Basic API Structure for Indexes)

### 2.1 카탈로그 엔트리 (Catalog Entries)

인덱스 접근 메서드는 `pg_am` 시스템 카탈로그의 행으로 정의됩니다. 각 접근 메서드는 이름(name) 과 핸들러 함수(handler function) 를 지정합니다.

#### 필수 카탈로그 항목

| 카탈로그 | 설명 |
|----------|------|
| `pg_am` | 접근 메서드 정의 |
| `pg_opfamily` | 연산자 족 (Operator Family) |
| `pg_opclass` | 연산자 클래스 (Operator Class) |
| `pg_amop` | 접근 메서드 연산자 |
| `pg_amproc` | 접근 메서드 지원 함수 |

#### 접근 메서드 생성 SQL

```sql
-- 새로운 접근 메서드 생성
CREATE ACCESS METHOD method_name
    TYPE INDEX
    HANDLER handler_function;

-- 접근 메서드 삭제
DROP ACCESS METHOD method_name;
```

### 2.2 핸들러 함수 요구사항

핸들러 함수의 특성:

- 입력: `internal` 타입의 더미 값 (SQL에서 직접 호출 방지)
- 반환값: `IndexAmRoutine` 타입의 palloc'd 구조체
- 목적: 코어 코드가 인덱스 접근 메서드를 사용하는 데 필요한 모든 정보 제공

### 2.3 IndexAmRoutine 구조체

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

### 2.4 주요 플래그 필드 설명

#### amcanmulticol과 amoptionalkey

| 플래그 | 설명 |
|--------|------|
| `amcanmulticol=true` | 다중 열 인덱스 지원 |
| `amoptionalkey=true` | 첫 번째 열에 대한 제약 조건 없이 스캔 가능 |

NULL 색인화 규칙: `amoptionalkey=true`인 경우 NULL 값을 반드시 색인화해야 합니다.

#### amcaninclude

- "포함된 열(included columns)" 지원 (키 열 이외의 추가 열 저장)
- `amcanmulticol=false`, `amcaninclude=true` 조합 가능
- 포함된 열은 `amoptionalkey`와 무관하게 NULL 허용

#### amsummarizing

- 블록 단위 이상의 튜플 요약 수행
- BRIN 같은 접근 메서드가 HOT 최적화 를 계속 허용

---

## 3. 인덱스 접근 메서드 함수 (Index Access Method Functions)

### 3.1 인덱스 구축 및 유지보수 함수

#### ambuild - 인덱스 구축

```c
IndexBuildResult *ambuild(Relation heapRelation,
                          Relation indexRelation,
                          IndexInfo *indexInfo);
```

| 항목 | 설명 |
|------|------|
| 목적 | 새 인덱스 구축 |
| 동작 | 빈 인덱스를 채우고 기존 테이블의 모든 튜플에 대해 엔트리 생성 |
| 반환 | 인덱스 통계 정보를 담은 palloc'd 구조체 |

#### ambuildempty - 빈 인덱스 초기화

```c
void ambuildempty(Relation indexRelation);
```

| 항목 | 설명 |
|------|------|
| 목적 | 빈 인덱스 구축 후 초기화 포크(INIT_FORKNUM)에 작성 |
| 사용처 | 로깅되지 않는(unlogged) 인덱스 에만 호출 |

#### aminsert - 튜플 삽입

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

| 매개변수 | 설명 |
|----------|------|
| `values`, `isnull` | 인덱싱할 키 값 |
| `heap_tid` | 인덱싱될 TID |
| `checkUnique` | 고유성 검사 유형 |
| `indexUnchanged` | 중복 튜플 여부 힌트 |

반환값: `UNIQUE_CHECK_PARTIAL`일 때만 의미 있음 (true=고유, false=중복 가능성)

#### aminsertcleanup - 삽입 후 정리

```c
void aminsertcleanup(Relation indexRelation,
                     IndexInfo *indexInfo);
```

연속 삽입 중 유지된 상태를 정리합니다. 메모리 이외의 자원 해제가 필요한 경우 사용됩니다.

#### ambulkdelete - 일괄 삭제

```c
IndexBulkDeleteResult *ambulkdelete(IndexVacuumInfo *info,
                                    IndexBulkDeleteResult *stats,
                                    IndexBulkDeleteCallback callback,
                                    void *callback_state);
```

| 항목 | 설명 |
|------|------|
| 목적 | 인덱스에서 튜플 일괄 삭제 |
| 동작 | 전체 인덱스를 스캔하고 콜백 함수로 삭제 대상 결정 |
| 콜백 | `callback(TID, callback_state)` - bool 반환 |
| 반환 | 삭제 결과 통계 또는 NULL |

#### amvacuumcleanup - VACUUM 후 정리

```c
IndexBulkDeleteResult *amvacuumcleanup(IndexVacuumInfo *info,
                                       IndexBulkDeleteResult *stats);
```

VACUUM 작업 후 정리를 수행합니다. 빈 페이지 회수 등 대량 정리 작업이 가능합니다.

### 3.2 인덱스 기능 지원 함수

#### amcanreturn - 인덱스 전용 스캔 지원 확인

```c
bool amcanreturn(Relation indexRelation, int attno);
```

인덱스 전용 스캔(Index-Only Scan) 지원 여부를 확인합니다. 포함된 컬럼(included columns)은 항상 true를 반환합니다.

#### amcostestimate - 비용 추정

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

인덱스 스캔의 비용을 추정합니다. 자세한 내용은 [7. 인덱스 비용 추정 함수](#7-인덱스-비용-추정-함수-index-cost-estimation-functions)를 참조하세요.

#### amgettreeheight - 트리 높이 계산

```c
int amgettreeheight(Relation rel);
```

트리 형태 인덱스의 높이를 계산합니다. 비용 추정에 사용됩니다.

#### amoptions - 옵션 파싱

```c
bytea *amoptions(ArrayType *reloptions, bool validate);
```

인덱스 reloptions를 파싱하고 검증합니다.

#### amproperty - 속성 조회

```c
bool amproperty(Oid index_oid, int attno,
                IndexAMProperty prop, const char *propname,
                bool *res, bool *isnull);
```

인덱스 속성 조회를 재정의합니다.

#### amvalidate - 연산자 클래스 검증

```c
bool amvalidate(Oid opclassoid);
```

연산자 클래스 카탈로그 항목의 유효성을 검증합니다.

### 3.3 인덱스 스캔 함수

#### ambeginscan - 스캔 시작

```c
IndexScanDesc ambeginscan(Relation indexRelation,
                          int nkeys,
                          int norderbys);
```

| 항목 | 설명 |
|------|------|
| 목적 | 인덱스 스캔 준비 |
| 주의 | `RelationGetIndexScan()`으로 구조체 생성 필수 |
| 반환 | palloc'd 구조체 |

#### amrescan - 스캔 재시작

```c
void amrescan(IndexScanDesc scan,
              ScanKey keys,
              int nkeys,
              ScanKey orderbys,
              int norderbys);
```

인덱스 스캔을 시작하거나 재시작합니다. 스캔 키 개수는 `ambeginscan`에 전달된 값 이하여야 합니다.

#### amgettuple - 튜플 페치

```c
bool amgettuple(IndexScanDesc scan,
                ScanDirection direction);
```

| 항목 | 설명 |
|------|------|
| 목적 | 스캔에서 다음 튜플 페치 |
| 반환 | true=튜플 획득, false=일치하는 튜플 없음 |
| 출력 | 성공 시 `scan->xs_recheck` 설정 (true=재확인 필요) |

Index-Only Scan: `scan->xs_want_itup=true`이면 원본 인덱스 데이터를 반환합니다.

#### amgetbitmap - 비트맵 스캔

```c
int64 amgetbitmap(IndexScanDesc scan,
                  TIDBitmap *tbm);
```

| 항목 | 설명 |
|------|------|
| 목적 | 스캔의 모든 튜플을 TIDBitmap에 추가 |
| 반환 | 페치된 튜플 개수 |
| 동작 | 튜플 ID를 비트맵에 OR 연산 |

#### amendscan - 스캔 종료

```c
void amendscan(IndexScanDesc scan);
```

스캔을 종료하고 자원을 해제합니다. scan 구조체 자체는 해제하지 않습니다.

#### ammarkpos / amrestrpos - 위치 마킹

```c
void ammarkpos(IndexScanDesc scan);   /* 현재 위치 표시 */
void amrestrpos(IndexScanDesc scan);  /* 표시된 위치로 복원 */
```

정렬된 스캔에서 위치를 기억하고 복원합니다. 스캔당 하나의 기억 위치만 지원됩니다.

### 3.4 병렬 스캔 함수

#### amestimateparallelscan

```c
Size amestimateparallelscan(Relation indexRelation,
                            int nkeys,
                            int norderbys);
```

병렬 스캔에 필요한 동적 공유 메모리 바이트 수를 추정합니다.

#### aminitparallelscan

```c
void aminitparallelscan(void *target);
```

병렬 스캔 시작 시 동적 공유 메모리를 초기화합니다.

#### amparallelrescan

```c
void amparallelrescan(IndexScanDesc scan);
```

병렬 인덱스 스캔 재시작 시 공유 상태를 초기화합니다.

### 3.5 전략 변환 함수

```c
CompareType amtranslatestrategy(StrategyNumber strategy,
                                Oid opfamily, Oid opcintype);

StrategyNumber amtranslatecmptype(CompareType cmptype,
                                  Oid opfamily, Oid opcintype);
```

`CompareType`과 전략 번호 간의 변환을 수행합니다. btree/hash와 유사한 기능의 접근 메서드에서 사용됩니다.

---

## 4. 인덱스 스캔 (Index Scanning)

### 4.1 스캔 키 (Scan Keys)

인덱스 스캔에서 접근 메서드는 스캔 키(scan keys) 와 일치하는 모든 튜플의 TID를 반환해야 합니다.

```
index_key operator constant
```

- 인덱스 열 중 하나와 해당 인덱스 열의 연산자 패밀리(operator family) 멤버로 구성
- 스캔 키는 암시적으로 AND 연산됨
- 반환되는 튜플은 모든 조건을 만족 해야 함

### 4.2 Lossy 인덱스 스캔

인덱스가 스캔 키를 정확히 만족하는 항목 외에 추가 항목을 반환 할 수 있습니다:

```
Lossy 스캔 결과 = 정확한 일치 항목 + 추가 항목 (false positive)
                  ↓
        코어 시스템이 힙 튜플에서 재확인(recheck)
```

- `xs_recheck` 플래그로 재확인 필요 여부 표시
- 재확인 미지정 시 정확한 일치 항목만 반환해야 함

### 4.3 정렬된 출력 지원

#### amcanorder = true

```c
/* 항상 자연 정렬 순서로 반환 (예: B-tree) */
/* btree 호환 전략 번호 필수 */
```

#### amcanorderbyop = true

```sql
-- ORDER BY index_key operator constant 지원
SELECT * FROM table ORDER BY point_column <-> '(0,0)'::point;
```

### 4.4 스캔 방향 (Scan Direction)

`amgettuple` 함수의 direction 인자:

| 방향 | 설명 |
|------|------|
| `ForwardScanDirection` | 정방향 스캔 (앞에서 뒤로) |
| `BackwardScanDirection` | 역방향 스캔 (뒤에서 앞으로) |

동작 예시:

```c
/* 첫 호출이 BackwardScanDirection인 경우 */
/* → 마지막 일치 항목부터 반환 시작 */

/* 이후 호출에서는 방향 전환 가능 */
/* amcanbackward=false인 경우 첫 호출 방향 유지 */
```

### 4.5 두 가지 스캔 방식 비교

#### amgettuple 방식

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

#### amgetbitmap 방식

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

### 4.6 동시성 고려사항

허용되는 상황:
- 스캔 시작 후 삽입된 항목의 미반환
- 스캔 중 삭제된 항목의 반영 여부 불확실

필수 조건:
- 삽입/삭제로 인해 스캔이 항목을 놓치거나 중복 반환하지 않아야 함
- (삽입/삭제 중인 항목 제외)

---

## 5. 인덱스 잠금 고려사항 (Index Locking Considerations)

### 5.1 잠금 유형

#### PostgreSQL 핵심 시스템의 잠금

| 작업 | 잠금 유형 | 설명 |
|------|-----------|------|
| 인덱스 스캔 | `AccessShareLock` | 읽기 잠금 |
| 인덱스 업데이트 | `RowExclusiveLock` | 일반 VACUUM 포함 |
| 인덱스 생성/삭제/REINDEX | `ACCESS EXCLUSIVE` | 배타적 잠금 |
| CONCURRENTLY 옵션 | `SHARE UPDATE EXCLUSIVE` | 동시 작업 허용 |

중요: `AccessShareLock`과 `RowExclusiveLock`은 충돌하지 않음 - 접근 메서드가 세밀한 잠금을 책임져야 합니다.

### 5.2 동시성 제어 규칙

#### 힙 항목 생성 순서

```
새 힙 항목 생성 → 인덱스 항목 생성
```

- 동시 인덱스 스캔이 힙 항목을 보지 못할 수 있음 (정상 동작)
- 커밋되지 않은 행은 어차피 관심 대상이 아님

#### 힙 항목 삭제 순서

```
모든 인덱스 항목 제거 → 힙 항목 삭제 (VACUUM)
```

### 5.3 인덱스 페이지 핀(Pin)

인덱스 스캔은 `amgettuple`이 반환한 마지막 항목이 있는 인덱스 페이지에 핀(pin) 을 유지해야 합니다:

```c
/* 핀 유지 이유: VACUUM이 인덱스 항목을 제거한 후
 * 읽기 프로세스가 대응하는 힙 항목에 도달하는 문제 방지 */
ambulkdelete() // 다른 백엔드가 핀을 유지한 페이지에서 항목 삭제 불가
```

### 5.4 동기 vs 비동기 스캔

#### 동기 스캔 (Synchronous)

```
인덱스 항목 스캔 → 즉시 힙 튜플 가져오기
```

- 비MVCC 호환 스냅샷에서 필수
- 오버헤드가 크지만 안전

#### 비동기 스캔 (Asynchronous)

```
여러 TID 수집 → 나중에 힙 방문 (비트맵 스캔)
```

- MVCC 호환 스냅샷에서만 사용 가능
- 인덱스 잠금 오버헤드 감소
- 더 효율적인 힙 접근 패턴

### 5.5 술어 잠금 (Predicate Locking)

#### ampredlocks = false (기본값)

```sql
/* 직렬화 가능 트랜잭션에서 전체 인덱스에 비차단 술어 잠금 획득 */
/* 동시 직렬화 가능 트랜잭션의 삽입과 읽기-쓰기 충돌 발생 가능 */
/* 충돌 패턴 감지 시 일부 트랜잭션이 취소될 수 있음 */
```

#### ampredlocks = true

세밀한 술어 잠금을 구현하여 트랜잭션 취소 빈도를 감소시킵니다.

---

## 6. 인덱스 고유성 검사 (Index Uniqueness Checks)

### 6.1 개요

PostgreSQL은 고유 인덱스(unique indexes) 를 사용하여 SQL 고유성 제약 조건을 강제합니다:

- `amcanunique=true`를 설정하는 접근 메서드만 지원 (현재 B-tree만 지원)
- `INCLUDE` 절의 컬럼은 고유성 검사 시 고려되지 않음

### 6.2 MVCC와 고유성

MVCC 환경에서는 인덱스에 물리적으로 중복 항목이 존재 할 수 있습니다:

```
강제되어야 할 규칙:
"어떤 MVCC 스냅샷도 동일한 인덱스 키를 가진
두 개의 라이브 행을 포함할 수 없다"
```

### 6.3 새 행 삽입 시 확인 사항

| 상황 | 동작 |
|------|------|
| 현재 트랜잭션이 충돌 행 삭제 | 삽입 허용 (UPDATE 시나리오) |
| 미커밋 트랜잭션이 충돌 행 삽입 | 해당 트랜잭션 완료 대기 |
| 미커밋 트랜잭션이 충돌 행 삭제 | 트랜잭션 완료 후 재검사 |

### 6.4 checkUnique 매개변수

`aminsert`의 `checkUnique` 매개변수 값:

#### UNIQUE_CHECK_NO

```c
/* 고유성 검사를 수행하지 않음 */
/* 고유 인덱스가 아님 */
```

#### UNIQUE_CHECK_YES

```c
/* 비연기(non-deferrable) 고유 인덱스 */
/* 즉시 고유성 검사 수행 */
/* 위반 시 즉시 에러 발생 */
```

#### UNIQUE_CHECK_PARTIAL

```c
/* 연기 가능한(deferrable) 고유 제약 조건 */
/* 각 행의 인덱스 항목 삽입 시 사용 */

/* 동작: */
/* - 인덱스에 중복 항목 허용 */
/* - 잠재적 중복 감지 시 false 반환 */
/* - false 반환 행에 대해 연기된 재검사 스케줄링 */

/* 특징: */
/* - 다른 트랜잭션 완료 대기 없이 확인 가능 */
/* - 오탐(false positive) 허용 */
```

#### UNIQUE_CHECK_EXISTING

```c
/* 잠재적 고유성 위반으로 보고된 행의 연기된 재검사 */
/* 주의: 새로운 인덱스 항목을 삽입하지 않음 (이미 존재) */

/* 동작: */
/* - 다른 라이브 인덱스 항목 존재 여부 확인 */
/* - 대상 행도 여전히 라이브인 경우 에러 보고 */
```

### 6.5 예제: 연기된 고유성 검사

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

## 7. 인덱스 비용 추정 함수 (Index Cost Estimation Functions)

### 7.1 함수 서명

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

### 7.2 입력 매개변수

| 매개변수 | 설명 |
|----------|------|
| `root` | 처리 중인 쿼리에 대한 플래너 정보 |
| `path` | 고려 중인 인덱스 접근 경로 |
| `loop_count` | 인덱스 스캔 반복 횟수 (중첩 루프 조인 시 1보다 큼) |

### 7.3 출력 매개변수 (pass-by-reference)

| 매개변수 | 설명 |
|----------|------|
| `*indexStartupCost` | 인덱스 시작 처리 비용 |
| `*indexTotalCost` | 인덱스 처리 총 비용 |
| `*indexSelectivity` | 인덱스 선택도 (0~1 사이) |
| `*indexCorrelation` | 인덱스 스캔 순서와 테이블 순서 간 상관계수 (-1.0~1.0) |
| `*indexPages` | 인덱스 리프 페이지 수 |

### 7.4 비용 계산에 사용되는 매개변수

```c
/* 시스템 비용 상수 */
seq_page_cost      /* 순차 디스크 블록 페치 비용 */
random_page_cost   /* 비순차(랜덤) 페치 비용 */
cpu_index_tuple_cost  /* 인덱스 행 처리 비용 */
cpu_operator_cost     /* 비교 연산자 비용 */
```

### 7.5 비용 추정 절차

#### 1단계: 인덱스 선택도 추정

```c
*indexSelectivity = clauselist_selectivity(root,
                                           path->indexquals,
                                           path->indexinfo->rel->relid,
                                           JOIN_INNER,
                                           NULL);
```

#### 2단계: 방문할 인덱스 행 수 추정

```c
numIndexTuples = indexSelectivity * index->rel->tuples;
```

#### 3단계: 검색할 인덱스 페이지 수 추정

```c
numIndexPages = indexSelectivity * index->pages;
```

#### 4단계: 인덱스 접근 비용 계산

```c
cost_qual_eval(&index_qual_cost, path->indexquals, root);

*indexStartupCost = index_qual_cost.startup;

*indexTotalCost = seq_page_cost * numIndexPages +
    (cpu_index_tuple_cost + index_qual_cost.per_tuple) * numIndexTuples;
```

#### 5단계: 인덱스 상관계수 추정

```c
/* pg_statistic에서 단일 필드 순서 인덱스의 상관계수 조회 */
/* 알려지지 않은 경우: 0 (상관계수 없음) */
```

### 7.6 비용 추정 예제

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

### 7.7 중요 사항

- C 언어 필수: SQL이나 절차형 언어로 작성 불가
- 비용 범위: 인덱스 자체 스캔 비용만 포함, 부모 테이블 행 검색 비용 제외
- loop_count > 1: 단일 스캔의 평균값 반환
- 예제 코드 위치: `src/backend/utils/adt/selfuncs.c`

---

## 부록: 사용자 정의 인덱스 접근 메서드 생성 예제

### A.1 기본 구조

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

### A.2 SQL 등록

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

### A.3 사용 예제

```sql
-- 사용자 정의 접근 메서드로 인덱스 생성
CREATE INDEX idx_mytable ON mytable USING myam (column1);

-- 인덱스 사용 확인
EXPLAIN SELECT * FROM mytable WHERE column1 = 100;
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Index Access Method Interface Definition](https://www.postgresql.org/docs/current/indexam.html)
- [PostgreSQL 소스 코드 - B-tree 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/nbtree)
- [PostgreSQL 소스 코드 - Hash 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/hash)
- [PostgreSQL 소스 코드 - GiST 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/gist)
- [PostgreSQL 소스 코드 - GIN 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/gin)
- [PostgreSQL 소스 코드 - BRIN 구현](https://github.com/postgres/postgres/tree/master/src/backend/access/brin)
