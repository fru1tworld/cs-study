# Chapter 47: 서버 프로그래밍 인터페이스 (Server Programming Interface, SPI)

## 목차
1. [개요](#개요)
2. [인터페이스 함수](#인터페이스-함수)
   - [연결 관리](#연결-관리)
   - [쿼리 실행](#쿼리-실행)
   - [준비된 구문](#준비된-구문)
   - [커서 관리](#커서-관리)
3. [인터페이스 지원 함수](#인터페이스-지원-함수)
4. [메모리 관리](#메모리-관리)
5. [트랜잭션 관리](#트랜잭션-관리)
6. [데이터 변경의 가시성](#데이터-변경의-가시성)
7. [예제](#예제)

---

## 개요

서버 프로그래밍 인터페이스(Server Programming Interface, SPI) 는 C 언어로 작성된 사용자 정의 함수가 PostgreSQL 서버 내부에서 SQL 명령을 실행할 수 있도록 해주는 인터페이스입니다.

### SPI의 주요 목적

| 목적 | 설명 |
|------|------|
| SQL 실행 | C 함수 내에서 SQL 명령을 실행 |
| 파서/플래너/실행기 접근 | Parser, Planner, Executor에 대한 간편한 접근 제공 |
| 메모리 관리 | 메모리 할당 및 해제의 단순화 |

### 사용을 위한 필수 조건

SPI를 사용하려면 다음 헤더 파일을 포함해야 합니다:

```c
#include "executor/spi.h"
```

### 반환값 규칙

SPI 함수들은 다음과 같은 반환값 규칙을 따릅니다:

- 성공: 음이 아닌 값 (non-negative value)
- 실패: 음수 값 (negative value) 또는 NULL

### 중요한 특징

> 주의사항: SPI를 통해 호출된 명령이 실패하면, C 함수로 제어가 반환되지 않습니다. C 함수가 실행 중인 트랜잭션 또는 서브트랜잭션이 롤백됩니다. 오류로부터 복구하려면 자신의 서브트랜잭션을 설정해야 합니다.

---

## 인터페이스 함수

### 연결 관리

SPI를 사용하기 전에 먼저 SPI 관리자에 연결해야 하며, 작업이 끝나면 연결을 해제해야 합니다.

#### SPI_connect / SPI_connect_ext

C 함수를 SPI 관리자에 연결합니다.

```c
int SPI_connect(void)
int SPI_connect_ext(int options)
```

설명:
- `SPI_connect()`는 `SPI_connect_ext(0)`과 동일합니다.
- SPI를 통해 명령어를 실행하려면 반드시 호출해야 합니다.

옵션:

| 옵션 | 설명 |
|------|------|
| `SPI_OPT_NONATOMIC` | 비원자적(nonatomic) 연결 설정. `SPI_commit`, `SPI_rollback` 등 트랜잭션 제어 함수 호출 허용 |

반환값:

| 반환값 | 의미 |
|--------|------|
| `SPI_OK_CONNECT` | 연결 성공 |

예제:

```c
// 기본 연결
if (SPI_connect() != SPI_OK_CONNECT)
    ereport(ERROR, (errmsg("SPI connect failed")));

// 비원자적 연결 (트랜잭션 제어 필요 시)
if (SPI_connect_ext(SPI_OPT_NONATOMIC) != SPI_OK_CONNECT)
    ereport(ERROR, (errmsg("SPI connect failed")));
```

#### SPI_finish

C 함수를 SPI 관리자에서 연결 해제합니다.

```c
int SPI_finish(void)
```

설명:
- SPI 작업을 완료한 후 반드시 호출해야 합니다.
- `elog(ERROR)`를 통해 트랜잭션을 중단하는 경우에는 자동으로 SPI가 정리되므로 별도 호출이 필요 없습니다.

반환값:

| 반환값 | 의미 |
|--------|------|
| `SPI_OK_FINISH` | 정상적으로 연결 해제됨 |
| `SPI_ERROR_UNCONNECTED` | 연결되지 않은 C 함수에서 호출됨 |

---

### 쿼리 실행

#### SPI_execute

지정된 SQL 명령을 실행합니다.

```c
int SPI_execute(const char *command, bool read_only, long count)
```

매개변수:

| 매개변수 | 타입 | 설명 |
|---------|------|------|
| `command` | `const char *` | 실행할 SQL 명령 문자열 |
| `read_only` | `bool` | `true`면 읽기 전용 실행 |
| `count` | `long` | 반환할 최대 행 수, 제한 없으면 0 |

read_only 매개변수의 영향:

| 값 | 동작 |
|----|------|
| `false` | 명령 카운터 증가, 새 스냅샷 계산, 데이터베이스 수정 가능 |
| `true` | 스냅샷과 명령 카운터 미업데이트, SELECT 명령만 허용, 더 빠른 실행 |

성공 반환값:

| 반환값 | 설명 |
|--------|------|
| `SPI_OK_SELECT` | SELECT 실행 성공 |
| `SPI_OK_SELINTO` | SELECT INTO 실행 성공 |
| `SPI_OK_INSERT` | INSERT 실행 성공 |
| `SPI_OK_DELETE` | DELETE 실행 성공 |
| `SPI_OK_UPDATE` | UPDATE 실행 성공 |
| `SPI_OK_MERGE` | MERGE 실행 성공 |
| `SPI_OK_INSERT_RETURNING` | INSERT RETURNING 실행 성공 |
| `SPI_OK_DELETE_RETURNING` | DELETE RETURNING 실행 성공 |
| `SPI_OK_UPDATE_RETURNING` | UPDATE RETURNING 실행 성공 |
| `SPI_OK_MERGE_RETURNING` | MERGE RETURNING 실행 성공 |
| `SPI_OK_UTILITY` | 유틸리티 명령(CREATE TABLE 등) 실행 성공 |
| `SPI_OK_REWRITTEN` | 규칙에 의해 재작성된 명령 |

오류 반환값:

| 반환값 | 설명 |
|--------|------|
| `SPI_ERROR_ARGUMENT` | command가 NULL이거나 count < 0 |
| `SPI_ERROR_COPY` | COPY TO stdout/FROM stdin 시도 |
| `SPI_ERROR_TRANSACTION` | 트랜잭션 조작 명령 시도 |
| `SPI_ERROR_OPUNKNOWN` | 알 수 없는 명령 타입 |
| `SPI_ERROR_UNCONNECTED` | 미연결 C 함수에서 호출 |

결과 접근:

```c
// 처리된 행 수
uint64 proc = SPI_processed;

// 결과 행 접근
if (SPI_tuptable != NULL)
{
    SPITupleTable *tuptable = SPI_tuptable;
    TupleDesc tupdesc = tuptable->tupdesc;

    for (uint64 j = 0; j < tuptable->numvals; j++)
    {
        HeapTuple tuple = tuptable->vals[j];
        // 튜플 처리...
    }
}
```

SPITupleTable 구조체:

```c
typedef struct SPITupleTable
{
    TupleDesc   tupdesc;        /* 튜플 디스크립터 */
    HeapTuple  *vals;           /* 튜플 배열 */
    uint64      numvals;        /* 유효한 튜플 개수 */

    /* 내부용 멤버 (외부 호출자용 아님) */
    uint64      alloced;
    MemoryContext tuptabcxt;
    slist_node  next;
    SubTransactionId subid;
} SPITupleTable;
```

#### SPI_exec

읽기/쓰기 명령을 실행합니다. `SPI_execute`의 단순화된 버전입니다.

```c
int SPI_exec(const char *command, long count)
```

설명:
- `SPI_execute(command, false, count)`와 동일합니다.
- `read_only` 매개변수가 항상 `false`로 설정됩니다.

매개변수:

| 매개변수 | 타입 | 설명 |
|---------|------|------|
| `command` | `const char *` | 실행할 명령 문자열 |
| `count` | `long` | 반환할 최대 행 수, 0이면 제한 없음 |

---

### 준비된 구문

동일하거나 유사한 명령을 반복 실행할 때, 파싱 분석을 한 번만 수행하고 실행 계획을 재사용할 수 있습니다.

#### SPI_prepare

SQL 명령을 준비하되 실행하지 않습니다.

```c
SPIPlanPtr SPI_prepare(const char *command, int nargs, Oid *argtypes)
```

매개변수:

| 매개변수 | 설명 |
|---------|------|
| `command` | SQL 명령 문자열 (매개변수는 `$1`, `$2` 등으로 표시) |
| `nargs` | 입력 매개변수 개수 |
| `argtypes` | 매개변수의 데이터 타입 OID 배열 포인터 |

반환값:
- 성공: `SPIPlanPtr` 구조체에 대한 포인터
- 실패: NULL (SPI_result에 오류 코드 설정)

예제:

```c
// 매개변수가 있는 준비된 구문
SPIPlanPtr plan;
Oid argtypes[2] = {INT4OID, TEXTOID};

plan = SPI_prepare("SELECT * FROM table WHERE id = $1 AND name = $2", 2, argtypes);
if (plan == NULL)
    ereport(ERROR, (errmsg("SPI_prepare failed")));
```

실행 계획 최적화:

| 상황 | 동작 |
|------|------|
| 매개변수 없음 | 첫 사용 시 일반(generic) 계획 생성, 이후 계속 사용 |
| 매개변수 있음 | 초기에는 커스텀 계획 생성, 충분한 사용 후 일반 계획으로 전환 |

주의사항:
- `SPI_finish` 호출 시 메모리 해제됨
- 계획을 보존하려면 `SPI_keepplan` 또는 `SPI_saveplan` 사용
- DDL 변경 시 자동으로 재분석 및 재계획

#### SPI_execute_plan

`SPI_prepare`로 준비된 SQL 문을 실행합니다.

```c
int SPI_execute_plan(SPIPlanPtr plan, Datum *values, const char *nulls,
                     bool read_only, long count)
```

매개변수:

| 매개변수 | 설명 |
|---------|------|
| `plan` | `SPI_prepare`로 반환된 준비된 SQL 문 |
| `values` | 실제 매개변수 값의 배열 |
| `nulls` | NULL 매개변수를 설명하는 배열: `' '`(non-null) 또는 `'n'`(null), NULL이면 모든 매개변수가 non-null |
| `read_only` | `true`면 읽기 전용 실행 |
| `count` | 반환할 최대 행 수 (0: 무제한) |

오류 반환값:

| 반환값 | 설명 |
|--------|------|
| `SPI_ERROR_ARGUMENT` | `plan`이 NULL이거나 유효하지 않음, 또는 `count < 0` |
| `SPI_ERROR_PARAM` | `values`가 NULL이고 `plan`에 매개변수가 있음 |

예제:

```c
SPIPlanPtr plan;
Datum values[2];
char nulls[2] = {' ', ' '};  // 둘 다 non-null

plan = SPI_prepare("SELECT * FROM users WHERE id = $1", 1, (Oid[]){INT4OID});

values[0] = Int32GetDatum(42);

int ret = SPI_execute_plan(plan, values, nulls, true, 0);
if (ret > 0 && SPI_tuptable != NULL)
{
    // 결과 처리
}
```

#### SPI_keepplan / SPI_saveplan

준비된 구문을 저장하여 `SPI_finish` 후에도 사용할 수 있게 합니다.

```c
int SPI_keepplan(SPIPlanPtr plan)
SPIPlanPtr SPI_saveplan(SPIPlanPtr plan)
```

---

### 커서 관리

커서를 사용하면 대량의 결과 집합을 메모리 효율적으로 처리할 수 있습니다.

#### SPI_cursor_open

`SPI_prepare`로 생성된 명령문을 사용하여 커서를 설정합니다.

```c
Portal SPI_cursor_open(const char *name, SPIPlanPtr plan,
                       Datum *values, const char *nulls,
                       bool read_only)
```

매개변수:

| 매개변수 | 설명 |
|---------|------|
| `name` | 포탈 이름 (NULL이면 시스템이 자동 선택) |
| `plan` | `SPI_prepare`에서 반환된 준비된 명령문 |
| `values` | 실제 매개변수 값 배열 |
| `nulls` | NULL 매개변수 설명 배열 |
| `read_only` | 읽기 전용 실행 여부 |

반환값:
- 커서를 포함하는 포탈에 대한 포인터
- 오류 시 `elog`를 통해 보고

커서 사용의 이점:

| 이점 | 설명 |
|------|------|
| 메모리 효율성 | 결과 행을 한 번에 몇 개씩 검색하여 메모리 오버런 방지 |
| 생명주기 연장 | 포탈은 현재 트랜잭션이 끝날 때까지 존재 가능 |

#### SPI_cursor_fetch

커서에서 여러 행을 가져옵니다.

```c
void SPI_cursor_fetch(Portal portal, bool forward, long count)
```

매개변수:

| 매개변수 | 설명 |
|---------|------|
| `portal` | 커서를 포함하는 포탈 |
| `forward` | `true`: 앞으로 페치, `false`: 뒤로 페치 |
| `count` | 페치할 최대 행 수 |

결과:
- `SPI_processed`: 처리된 행 수
- `SPI_tuptable`: 결과 튜플 테이블

> 주의: 뒤로 페치(backward fetch)는 커서의 플랜이 `CURSOR_OPT_SCROLL` 옵션으로 생성되지 않았다면 실패할 수 있습니다.

#### SPI_cursor_move

커서를 이동합니다 (행을 페치하지 않음).

```c
void SPI_cursor_move(Portal portal, bool forward, long count)
```

#### SPI_cursor_close

커서를 닫고 포탈 저장소를 해제합니다.

```c
void SPI_cursor_close(Portal portal)
```

설명:
- 트랜잭션 종료 시 모든 열린 커서가 자동으로 닫힙니다.
- 리소스를 더 빨리 해제하려는 경우에만 명시적으로 호출합니다.

예제: 커서를 사용한 대량 데이터 처리:

```c
SPIPlanPtr plan;
Portal portal;

SPI_connect();

plan = SPI_prepare("SELECT * FROM large_table", 0, NULL);
portal = SPI_cursor_open("my_cursor", plan, NULL, NULL, true);

while (true)
{
    SPI_cursor_fetch(portal, true, 100);  // 100행씩 페치

    if (SPI_processed == 0)
        break;  // 더 이상 행이 없음

    // 페치된 행 처리
    for (uint64 i = 0; i < SPI_processed; i++)
    {
        HeapTuple tuple = SPI_tuptable->vals[i];
        // 튜플 처리...
    }

    SPI_freetuptable(SPI_tuptable);
}

SPI_cursor_close(portal);
SPI_finish();
```

---

## 인터페이스 지원 함수

`SPI_execute` 등의 함수에서 반환한 결과 집합에서 정보를 추출하기 위한 함수들입니다.

### 함수 목록

| 함수 | 설명 |
|------|------|
| `SPI_fname` | 지정된 열 번호에 대한 열 이름 반환 |
| `SPI_fnumber` | 지정된 열 이름에 대한 열 번호 반환 |
| `SPI_getvalue` | 지정된 열의 문자열 값 반환 |
| `SPI_getbinval` | 지정된 열의 이진 값 반환 |
| `SPI_gettype` | 지정된 열의 데이터 타입 이름 반환 |
| `SPI_gettypeid` | 지정된 열의 데이터 타입 OID 반환 |
| `SPI_getrelname` | 지정된 관계(relation)의 이름 반환 |
| `SPI_getnspname` | 지정된 관계의 네임스페이스 반환 |
| `SPI_result_code_string` | 오류 코드를 문자열로 반환 |

### SPI_getvalue

지정된 열의 값을 문자열 표현으로 반환합니다.

```c
char *SPI_getvalue(HeapTuple row, TupleDesc rowdesc, int colnumber)
```

매개변수:

| 매개변수 | 타입 | 설명 |
|---------|------|------|
| `row` | `HeapTuple` | 검사할 입력 행 |
| `rowdesc` | `TupleDesc` | 입력 행 설명 |
| `colnumber` | `int` | 열 번호 (1부터 시작) |

반환값:
- 성공: 열 값의 문자열 표현
- NULL: 열이 null이거나 colnumber가 범위를 벗어난 경우

예제:

```c
TupleDesc tupdesc = SPI_tuptable->tupdesc;
HeapTuple tuple = SPI_tuptable->vals[0];

char *name = SPI_getvalue(tuple, tupdesc, 1);  // 첫 번째 열
char *value = SPI_getvalue(tuple, tupdesc, 2);  // 두 번째 열

elog(INFO, "name: %s, value: %s", name, value);
```

### SPI_getbinval

지정된 열의 이진 값을 반환합니다.

```c
Datum SPI_getbinval(HeapTuple row, TupleDesc rowdesc, int colnumber, bool *isnull)
```

매개변수:

| 매개변수 | 설명 |
|---------|------|
| `row` | 검사할 입력 행 |
| `rowdesc` | 입력 행 설명 |
| `colnumber` | 열 번호 (1부터 시작) |
| `isnull` | 열이 null인지 여부를 반환하는 포인터 |

---

## 메모리 관리

PostgreSQL은 메모리 컨텍스트(Memory Context) 내에서 메모리를 할당합니다. 이는 서로 다른 수명을 가진 할당들을 효율적으로 관리하는 방법을 제공합니다.

### 메모리 컨텍스트 개념

```
SPI_connect 호출
    ↓
C 함수의 전용 컨텍스트 생성 (현재 컨텍스트)
    ↓
palloc, repalloc, SPI 유틸리티 함수로 메모리 할당
    ↓
SPI_finish 호출
    ↓
현재 컨텍스트를 상위 실행자 컨텍스트로 복원
C 함수 컨텍스트의 모든 메모리 해제
```

### 메모리 관리 함수

| 함수 | 설명 |
|------|------|
| `SPI_palloc` | 상위 실행자 컨텍스트에서 메모리 할당 |
| `SPI_repalloc` | 상위 실행자 컨텍스트에서 메모리 재할당 |
| `SPI_pfree` | 상위 실행자 컨텍스트에서 메모리 해제 |
| `SPI_copytuple` | 상위 실행자 컨텍스트에서 행의 복사본 생성 |
| `SPI_returntuple` | Datum으로 반환할 튜플 준비 |
| `SPI_modifytuple` | 선택된 필드를 대체하여 행 생성 |
| `SPI_freetuple` | 상위 실행자 컨텍스트에서 할당된 행 해제 |
| `SPI_freetuptable` | `SPI_execute` 등으로 생성된 행 집합 해제 |
| `SPI_freeplan` | 이전에 저장된 준비된 명령문 해제 |

### SPI_palloc

상위 실행자 컨텍스트에서 메모리를 할당합니다.

```c
void *SPI_palloc(Size size)
```

설명:
- C 함수가 메모리에 할당된 객체를 반환해야 하는 경우, `palloc`으로 할당하면 `SPI_finish`에서 해제됩니다.
- `SPI_palloc`을 사용하면 "상위 실행자 컨텍스트"에서 메모리가 할당되어 C 함수 반환 후에도 유지됩니다.

예제:

```c
// SPI 컨텍스트에서 메모리 할당 (SPI_finish 후 해제됨)
char *temp = palloc(100);

// 상위 컨텍스트에서 메모리 할당 (SPI_finish 후에도 유지)
char *result = SPI_palloc(100);
```

### SPI_freetuptable

`SPI_execute` 등으로 생성된 행 집합을 해제합니다.

```c
void SPI_freetuptable(SPITupleTable *tuptable)
```

예제:

```c
SPI_execute("SELECT * FROM large_table", true, 100);

// 결과 처리...

// 메모리 해제
SPI_freetuptable(SPI_tuptable);
```

---

## 트랜잭션 관리

SPI를 통한 트랜잭션 관리 함수들입니다.

### 제한사항

- `COMMIT`, `ROLLBACK` 같은 트랜잭션 제어 명령어는 `SPI_execute`를 통해 직접 실행할 수 없습니다.
- 대신 별도의 인터페이스 함수를 사용해야 합니다.
- 트랜잭션 관리 함수를 사용하려면 `SPI_connect_ext(SPI_OPT_NONATOMIC)`으로 연결해야 합니다.

### SPI_commit

현재 트랜잭션을 커밋합니다.

```c
void SPI_commit(void)
void SPI_commit_and_chain(void)
```

설명:
- SQL 명령어 `COMMIT`을 실행하는 것과 동일합니다.
- 트랜잭션 커밋 후, 기본 트랜잭션 특성을 사용하여 새로운 트랜잭션이 자동으로 시작됩니다.
- `SPI_commit_and_chain`은 `COMMIT AND CHAIN`처럼 이전 트랜잭션과 동일한 특성으로 새 트랜잭션을 시작합니다.

### SPI_rollback

현재 트랜잭션을 롤백합니다.

```c
void SPI_rollback(void)
void SPI_rollback_and_chain(void)
```

설명:
- SQL 명령어 `ROLLBACK`을 실행하는 것과 동일합니다.
- 트랜잭션 롤백 후, 새로운 트랜잭션이 자동으로 시작됩니다.

### 사용 시 주의사항

> 경고: 임의의 사용자 정의 SQL 호출 함수에서 트랜잭션을 시작/종료하는 것은 일반적으로 안전하지 않고 권장되지 않습니다. 복잡한 SQL 표현식 중간에 트랜잭션 경계가 생기면 내부 오류나 크래시가 발생할 수 있습니다.

주요 사용 목적:
- 주로 절차형 언어(procedural language) 구현에서 사용
- `CALL` 명령으로 호출되는 SQL 레벨 프로시저의 트랜잭션 관리 지원

---

## 데이터 변경의 가시성

SPI를 사용하는 함수에서 데이터 변경의 가시성을 관리하는 규칙입니다.

### 핵심 규칙

#### 규칙 1: 명령 실행 중 변경의 불가시성

SQL 명령 실행 중 그 명령이 만든 데이터 변경은 명령 자신에게 보이지 않습니다.

```sql
INSERT INTO a SELECT * FROM a;
```
- `SELECT` 부분은 새로 삽입된 행들을 볼 수 없습니다.

#### 규칙 2: 이후 명령에 대한 가시성

명령 C에 의한 변경사항은 C 이후에 시작된 모든 명령에 보입니다.

### SPI 읽기/쓰기 플래그에 따른 가시성

| 모드 | 동작 | 적용 규칙 |
|------|------|----------|
| 읽기 전용 (Read-only) | 호출 명령의 변경을 볼 수 없음 | 규칙 1 적용 |
| 읽기-쓰기 (Read-write) | 지금까지의 모든 변경을 볼 수 있음 | 규칙 2 적용 |

### 함수 휘발성(Volatility)에 따른 동작

표준 절차형 언어는 함수 휘발성 속성에 따라 SPI 모드를 설정합니다:

| 휘발성 | 모드 | 설명 |
|--------|------|------|
| `STABLE` | 읽기 전용 | 같은 입력에 대해 같은 결과 반환 |
| `IMMUTABLE` | 읽기 전용 | 항상 같은 결과 반환 |
| `VOLATILE` | 읽기-쓰기 | 실행할 때마다 다른 결과 가능 |

> 주의: C 함수 작성자는 이 규칙을 위반할 수 있지만, 일반적으로 권장되지 않습니다.

---

## 예제

### 기본 예제: execq 함수

SQL 명령을 첫 번째 인자로, 행 개수를 두 번째 인자로 받아 실행하는 C 함수입니다.

```c
#include "postgres.h"
#include "executor/spi.h"
#include "utils/builtins.h"

PG_MODULE_MAGIC;

PG_FUNCTION_INFO_V1(execq);

Datum
execq(PG_FUNCTION_ARGS)
{
    char *command;
    int cnt;
    int ret;
    uint64 proc;

    /* 주어진 text 객체를 C 문자열로 변환 */
    command = text_to_cstring(PG_GETARG_TEXT_PP(0));
    cnt = PG_GETARG_INT32(1);

    SPI_connect();
    ret = SPI_exec(command, cnt);
    proc = SPI_processed;

    /* 일부 행이 페치된 경우, elog(INFO)를 통해 출력 */
    if (ret > 0 && SPI_tuptable != NULL)
    {
        SPITupleTable *tuptable = SPI_tuptable;
        TupleDesc tupdesc = tuptable->tupdesc;
        char buf[8192];
        uint64 j;

        for (j = 0; j < tuptable->numvals; j++)
        {
            HeapTuple tuple = tuptable->vals[j];
            int i;

            for (i = 1, buf[0] = 0; i <= tupdesc->natts; i++)
                snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf), " %s%s",
                        SPI_getvalue(tuple, tupdesc, i),
                        (i == tupdesc->natts) ? " " : " |");
            elog(INFO, "EXECQ: %s", buf);
        }
    }

    SPI_finish();
    pfree(command);

    PG_RETURN_INT64(proc);
}
```

### SQL 함수 선언

```sql
CREATE FUNCTION execq(text, integer) RETURNS int8
    AS 'MODULE_PATHNAME'
    LANGUAGE C STRICT;
```

### 샘플 세션

```sql
-- 테이블 생성
=> SELECT execq('CREATE TABLE a (x integer)', 0);
 execq
-------
     0
(1 row)

-- INSERT 실행
=> INSERT INTO a VALUES (execq('INSERT INTO a VALUES (0)', 0));
INSERT 0 1

-- SELECT로 결과 확인
=> SELECT execq('SELECT * FROM a', 0);
INFO:  EXECQ:  0    -- execq에 의해 삽입된 행
INFO:  EXECQ:  1    -- execq가 반환하고 상위 INSERT가 삽입한 행
 execq
-------
     2
(1 row)
```

### 커서를 사용한 대용량 데이터 처리 예제

```c
#include "postgres.h"
#include "executor/spi.h"
#include "utils/builtins.h"

PG_MODULE_MAGIC;

PG_FUNCTION_INFO_V1(process_large_table);

Datum
process_large_table(PG_FUNCTION_ARGS)
{
    char *table_name;
    SPIPlanPtr plan;
    Portal portal;
    uint64 total_rows = 0;

    table_name = text_to_cstring(PG_GETARG_TEXT_PP(0));

    SPI_connect();

    /* 쿼리 준비 */
    char query[256];
    snprintf(query, sizeof(query), "SELECT * FROM %s", table_name);
    plan = SPI_prepare(query, 0, NULL);

    if (plan == NULL)
        ereport(ERROR, (errmsg("SPI_prepare failed")));

    /* 커서 열기 */
    portal = SPI_cursor_open("my_cursor", plan, NULL, NULL, true);

    /* 배치로 행 처리 */
    while (true)
    {
        SPI_cursor_fetch(portal, true, 1000);  /* 1000행씩 페치 */

        if (SPI_processed == 0)
            break;

        total_rows += SPI_processed;

        /* 페치된 행 처리 */
        SPITupleTable *tuptable = SPI_tuptable;
        TupleDesc tupdesc = tuptable->tupdesc;

        for (uint64 i = 0; i < tuptable->numvals; i++)
        {
            HeapTuple tuple = tuptable->vals[i];
            /* 각 행에 대한 처리 로직 */
        }

        /* 메모리 해제 */
        SPI_freetuptable(SPI_tuptable);
    }

    /* 정리 */
    SPI_cursor_close(portal);
    SPI_finish();
    pfree(table_name);

    PG_RETURN_INT64(total_rows);
}
```

### 준비된 구문을 사용한 매개변수화된 쿼리 예제

```c
#include "postgres.h"
#include "executor/spi.h"
#include "utils/builtins.h"

PG_MODULE_MAGIC;

/* 정적 준비된 계획 (세션 간 재사용) */
static SPIPlanPtr saved_plan = NULL;

PG_FUNCTION_INFO_V1(get_user_by_id);

Datum
get_user_by_id(PG_FUNCTION_ARGS)
{
    int32 user_id = PG_GETARG_INT32(0);
    Datum values[1];
    char *result = NULL;

    SPI_connect();

    /* 계획이 없으면 준비 */
    if (saved_plan == NULL)
    {
        SPIPlanPtr plan;
        Oid argtypes[1] = {INT4OID};

        plan = SPI_prepare("SELECT username FROM users WHERE id = $1", 1, argtypes);

        if (plan == NULL)
            ereport(ERROR, (errmsg("SPI_prepare failed")));

        /* 계획 저장 (SPI_finish 후에도 유지) */
        saved_plan = SPI_saveplan(plan);
    }

    /* 매개변수 설정 */
    values[0] = Int32GetDatum(user_id);

    /* 준비된 구문 실행 */
    int ret = SPI_execute_plan(saved_plan, values, NULL, true, 1);

    if (ret > 0 && SPI_tuptable != NULL && SPI_processed > 0)
    {
        TupleDesc tupdesc = SPI_tuptable->tupdesc;
        HeapTuple tuple = SPI_tuptable->vals[0];

        char *username = SPI_getvalue(tuple, tupdesc, 1);
        if (username != NULL)
        {
            /* 상위 컨텍스트에 결과 복사 */
            result = SPI_palloc(strlen(username) + 1);
            strcpy(result, username);
        }
    }

    SPI_finish();

    if (result == NULL)
        PG_RETURN_NULL();

    PG_RETURN_TEXT_P(cstring_to_text(result));
}
```

---

## 요약

### SPI 사용 흐름

```
1. SPI_connect() 또는 SPI_connect_ext() 호출
        ↓
2. SPI_execute(), SPI_prepare(), SPI_cursor_open() 등으로 SQL 실행
        ↓
3. SPI_tuptable, SPI_processed 등으로 결과 접근
        ↓
4. SPI_getvalue(), SPI_getbinval() 등으로 데이터 추출
        ↓
5. SPI_freetuptable()로 메모리 해제 (필요시)
        ↓
6. SPI_finish() 호출
```

### 주요 전역 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `SPI_processed` | `uint64` | 마지막 명령으로 처리된 행 수 |
| `SPI_tuptable` | `SPITupleTable *` | 마지막 명령의 결과 튜플 테이블 |
| `SPI_result` | `int` | 일부 함수의 오류 코드 |

### 참고 자료

더 복잡한 예제는 PostgreSQL 소스 코드의 다음 위치에서 찾을 수 있습니다:
- `src/test/regress/regress.c`
- `contrib/spi` 모듈
