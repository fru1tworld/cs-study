# PostgreSQL 서버 프로그래밍: SPI, 백그라운드 워커, 논리적 디코딩

## Chapter 47: 서버 프로그래밍 인터페이스 (Server Programming Interface, SPI)

### 목차
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

### 개요

서버 프로그래밍 인터페이스(Server Programming Interface, SPI) 는 C 언어로 작성된 사용자 정의 함수가 PostgreSQL 서버 내부에서 SQL 명령을 실행할 수 있도록 해주는 인터페이스입니다.

#### SPI의 주요 목적

| 목적 | 설명 |
|------|------|
| SQL 실행 | C 함수 내에서 SQL 명령을 실행 |
| 파서/플래너/실행기 접근 | Parser, Planner, Executor에 대한 간편한 접근 제공 |
| 메모리 관리 | 메모리 할당 및 해제의 단순화 |

#### 사용을 위한 필수 조건

SPI를 사용하려면 다음 헤더 파일을 포함해야 합니다:

```c
#include "executor/spi.h"
```

#### 반환값 규칙

SPI 함수들은 다음과 같은 반환값 규칙을 따릅니다:

- 성공: 음이 아닌 값 (non-negative value)
- 실패: 음수 값 (negative value) 또는 NULL

#### 중요한 특징

> 주의사항: SPI를 통해 호출된 명령이 실패하면, C 함수로 제어가 반환되지 않습니다. C 함수가 실행 중인 트랜잭션 또는 서브트랜잭션이 롤백됩니다. 오류에서 복구하려면 자신의 서브트랜잭션을 직접 설정해야 합니다.

---

### 인터페이스 함수

#### 연결 관리

SPI를 사용하기 전에 먼저 SPI 관리자에 연결해야 하며, 작업이 끝나면 연결을 해제해야 합니다.

##### SPI_connect / SPI_connect_ext

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

##### SPI_finish

C 함수를 SPI 관리자에서 연결 해제합니다.

```c
int SPI_finish(void)
```

설명:
- SPI 작업을 완료한 후 반드시 호출해야 합니다.
- `elog(ERROR)`로 트랜잭션을 중단하는 경우에는 SPI가 자동으로 정리되므로 별도 호출이 필요 없습니다.

반환값:

| 반환값 | 의미 |
|--------|------|
| `SPI_OK_FINISH` | 정상적으로 연결 해제됨 |
| `SPI_ERROR_UNCONNECTED` | 연결되지 않은 C 함수에서 호출됨 |

---

#### 쿼리 실행

##### SPI_execute

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

read_only 매개변수 동작:

| 값 | 동작 |
|----|------|
| `false` | 명령 카운터를 증가시키고 새 스냅샷을 계산하며, 데이터베이스 수정 가능 |
| `true` | 스냅샷과 명령 카운터를 갱신하지 않음, SELECT 명령만 허용, 실행 속도 향상 |

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

##### SPI_exec

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

#### 준비된 구문

동일하거나 유사한 명령을 반복 실행할 때 파싱과 분석을 한 번만 수행하고 실행 계획을 재사용할 수 있습니다.

##### SPI_prepare

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

##### SPI_execute_plan

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

##### SPI_keepplan / SPI_saveplan

준비된 구문을 저장하여 `SPI_finish` 후에도 사용할 수 있게 합니다.

```c
int SPI_keepplan(SPIPlanPtr plan)
SPIPlanPtr SPI_saveplan(SPIPlanPtr plan)
```

---

#### 커서 관리

커서를 사용하면 대량의 결과 집합을 메모리 효율적으로 처리할 수 있습니다.

##### SPI_cursor_open

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
- 오류 시 `elog`를 통해 보고됨

커서 사용의 이점:

| 이점 | 설명 |
|------|------|
| 메모리 효율성 | 결과 행을 한 번에 몇 개씩 검색하여 메모리 오버런 방지 |
| 생명주기 연장 | 포탈은 현재 트랜잭션이 끝날 때까지 존재 가능 |

##### SPI_cursor_fetch

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

##### SPI_cursor_move

커서를 이동합니다 (행을 페치하지 않음).

```c
void SPI_cursor_move(Portal portal, bool forward, long count)
```

##### SPI_cursor_close

커서를 닫고 포탈 저장소를 해제합니다.

```c
void SPI_cursor_close(Portal portal)
```

설명:
- 트랜잭션이 종료되면 열린 모든 커서가 자동으로 닫힙니다.
- 리소스를 더 일찍 해제하려는 경우에만 명시적으로 호출합니다.

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

### 인터페이스 지원 함수

`SPI_execute` 등의 함수에서 반환한 결과 집합에서 정보를 추출하기 위한 함수들입니다.

#### 함수 목록

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

#### SPI_getvalue

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

#### SPI_getbinval

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

### 메모리 관리

PostgreSQL은 메모리 컨텍스트(Memory Context) 내에서 메모리를 할당합니다. 이는 서로 다른 수명을 가진 할당들을 효율적으로 관리하는 방법을 제공합니다.

#### 메모리 컨텍스트 개념

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

#### 메모리 관리 함수

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

#### SPI_palloc

상위 실행자 컨텍스트에서 메모리를 할당합니다.

```c
void *SPI_palloc(Size size)
```

설명:
- C 함수가 메모리에 할당된 객체를 반환해야 할 때, `palloc`으로 할당하면 `SPI_finish` 시 해제됩니다.
- `SPI_palloc`을 사용하면 "상위 실행자 컨텍스트"에서 메모리가 할당되어 C 함수가 반환된 후에도 유지됩니다.

예제:

```c
// SPI 전용 컨텍스트에서 메모리 할당 (SPI_finish 시 해제됨)
char *temp = palloc(100);

// 상위 실행자 컨텍스트에서 메모리 할당 (SPI_finish 후에도 유지됨)
char *result = SPI_palloc(100);
```

#### SPI_freetuptable

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

### 트랜잭션 관리

SPI를 통한 트랜잭션 관리 함수들입니다.

#### 제한사항

- `COMMIT`, `ROLLBACK` 같은 트랜잭션 제어 명령어는 `SPI_execute`를 통해 직접 실행할 수 없습니다.
- 대신 별도의 인터페이스 함수를 사용해야 합니다.
- 트랜잭션 관리 함수를 사용하려면 `SPI_connect_ext(SPI_OPT_NONATOMIC)`으로 연결해야 합니다.

#### SPI_commit

현재 트랜잭션을 커밋합니다.

```c
void SPI_commit(void)
void SPI_commit_and_chain(void)
```

설명:
- SQL 명령어 `COMMIT`을 실행하는 것과 동일합니다.
- 트랜잭션 커밋 후, 기본 트랜잭션 특성을 사용하여 새로운 트랜잭션이 자동으로 시작됩니다.
- `SPI_commit_and_chain`은 `COMMIT AND CHAIN`처럼 이전 트랜잭션과 동일한 특성으로 새 트랜잭션을 시작합니다.

#### SPI_rollback

현재 트랜잭션을 롤백합니다.

```c
void SPI_rollback(void)
void SPI_rollback_and_chain(void)
```

설명:
- SQL 명령어 `ROLLBACK`을 실행하는 것과 동일합니다.
- 트랜잭션 롤백 후, 새로운 트랜잭션이 자동으로 시작됩니다.

#### 사용 시 주의사항

> 경고: 임의의 사용자 정의 SQL 호출 함수에서 트랜잭션을 시작/종료하는 것은 일반적으로 안전하지 않고 권장되지 않습니다. 복잡한 SQL 표현식 중간에 트랜잭션 경계가 생기면 내부 오류나 크래시가 발생할 수 있습니다.

주요 사용 목적:
- 주로 절차형 언어(procedural language) 구현에서 사용
- `CALL` 명령으로 호출되는 SQL 레벨 프로시저의 트랜잭션 관리 지원

---

### 데이터 변경의 가시성

SPI를 사용하는 함수에서 데이터 변경의 가시성을 관리하는 규칙입니다.

#### 핵심 규칙

##### 규칙 1: 명령 실행 중 변경의 불가시성

SQL 명령 실행 중, 해당 명령이 만든 데이터 변경은 그 명령 자신에게는 보이지 않습니다.

```sql
INSERT INTO a SELECT * FROM a;
```
- `SELECT` 부분은 새로 삽입된 행들을 볼 수 없습니다.

##### 규칙 2: 이후 명령에 대한 가시성

명령 C에 의한 변경사항은 C 이후에 시작된 모든 명령에 보입니다.

#### SPI 읽기/쓰기 플래그에 따른 가시성

| 모드 | 동작 | 적용 규칙 |
|------|------|----------|
| 읽기 전용 (Read-only) | 호출 명령의 변경을 볼 수 없음 | 규칙 1 적용 |
| 읽기-쓰기 (Read-write) | 지금까지의 모든 변경을 볼 수 있음 | 규칙 2 적용 |

#### 함수 휘발성(Volatility)에 따른 동작

표준 절차형 언어는 함수의 휘발성 속성에 따라 SPI 모드를 결정합니다:

| 휘발성 | 모드 | 설명 |
|--------|------|------|
| `STABLE` | 읽기 전용 | 같은 입력에 대해 같은 결과 반환 |
| `IMMUTABLE` | 읽기 전용 | 항상 같은 결과 반환 |
| `VOLATILE` | 읽기-쓰기 | 실행할 때마다 다른 결과 가능 |

> 주의: C 함수 작성자는 이 규칙을 어길 수 있지만, 일반적으로 권장하지 않습니다.

---

### 예제

#### 기본 예제: execq 함수

SQL 명령을 첫 번째 인수로, 행 개수를 두 번째 인수로 받아 실행하는 C 함수입니다.

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

#### SQL 함수 선언

```sql
CREATE FUNCTION execq(text, integer) RETURNS int8
    AS 'MODULE_PATHNAME'
    LANGUAGE C STRICT;
```

#### 샘플 세션

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

#### 커서를 사용한 대용량 데이터 처리 예제

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

#### 준비된 구문을 사용한 매개변수화된 쿼리 예제

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

### 요약

#### SPI 사용 흐름

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

#### 주요 전역 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `SPI_processed` | `uint64` | 마지막 명령으로 처리된 행 수 |
| `SPI_tuptable` | `SPITupleTable *` | 마지막 명령의 결과 튜플 테이블 |
| `SPI_result` | `int` | 일부 함수의 오류 코드 |

#### 참고 자료

더 복잡한 예제는 PostgreSQL 소스 코드의 다음 경로에서 찾을 수 있습니다:
- `src/test/regress/regress.c`
- `contrib/spi` 모듈

---

## 백그라운드 워커 프로세스 (Background Worker Processes)

### 목차
1. [개요](#개요)
2. [보안 경고](#보안-경고)
3. [백그라운드 워커 등록](#백그라운드-워커-등록)
4. [BackgroundWorker 구조체](#backgroundworker-구조체)
5. [플래그 옵션](#플래그-옵션-bgw_flags)
6. [시작 시점 옵션](#시작-시점-옵션-bgw_start_time)
7. [데이터베이스 연결](#데이터베이스-연결)
8. [시그널 처리](#시그널-처리-signal-handling)
9. [프로세스 라이프사이클](#프로세스-라이프사이클-lifecycle)
10. [동적 워커 관리 함수](#동적-워커-관리-함수)
11. [비동기 알림](#비동기-알림-asynchronous-notifications)
12. [공유 메모리 접근](#공유-메모리-접근)
13. [예제 코드](#예제-코드)
14. [참고사항](#참고사항)

---

### 개요

PostgreSQL은 사용자가 제공한 코드를 별도의 프로세스에서 실행하는 방식으로 확장할 수 있습니다. 이러한 프로세스를 백그라운드 워커(Background Worker)라고 합니다.

백그라운드 워커의 특징:

- `postgres` (postmaster)에 의해 시작, 중지 및 모니터링됨
- PostgreSQL의 공유 메모리 영역(shared memory area)에 연결 가능
- 내부적으로 데이터베이스에 연결 가능
- 여러 트랜잭션을 순차적으로 실행 가능
- libpq 연결을 통해 선택적으로 클라이언트 역할 수행 가능

백그라운드 워커는 다음과 같은 용도로 활용됩니다:

- 주기적인 유지보수 작업
- 비동기 작업 처리
- 외부 시스템과의 연동
- 커스텀 모니터링
- 병렬 처리 구현

---

### 보안 경고

> 주의: 백그라운드 워커 프로세스는 C 언어를 통해 데이터에 무제한으로 접근할 수 있기 때문에 상당한 견고성(robustness) 및 보안 위험을 초래합니다. 신중하게 감사(audit)된 모듈만 백그라운드 워커 프로세스를 실행하도록 허용해야 합니다.

---

### 백그라운드 워커 등록

백그라운드 워커를 등록하는 방법은 두 가지가 있습니다.

#### 정적 등록 (Static Registration)

서버 시작 시 `shared_preload_libraries` 설정에 모듈 이름을 포함시켜 등록합니다.

```c
void RegisterBackgroundWorker(BackgroundWorker *worker)
```

이 함수는 모듈의 `_PG_init()` 함수에서 호출해야 합니다.

```c
void _PG_init(void)
{
    BackgroundWorker worker;

    /* 워커 구조체 초기화 */
    memset(&worker, 0, sizeof(worker));

    /* 워커 설정 */
    strcpy(worker.bgw_name, "my_worker");
    strcpy(worker.bgw_type, "my_worker_type");
    worker.bgw_flags = BGWORKER_SHMEM_ACCESS | BGWORKER_BACKEND_DATABASE_CONNECTION;
    worker.bgw_start_time = BgWorkerStart_RecoveryFinished;
    worker.bgw_restart_time = 60;  /* 60초 후 재시작 */
    strcpy(worker.bgw_library_name, "my_extension");
    strcpy(worker.bgw_function_name, "my_worker_main");
    worker.bgw_main_arg = Int32GetDatum(0);

    RegisterBackgroundWorker(&worker);
}
```

#### 동적 등록 (Dynamic Registration)

시스템 시작 후에 백그라운드 워커를 등록합니다.

```c
bool RegisterDynamicBackgroundWorker(BackgroundWorker *worker,
                                     BackgroundWorkerHandle **handle)
```

동적 등록은 일반 백엔드(regular backend) 또는 다른 백그라운드 워커에서 호출해야 합니다. postmaster에서 직접 호출하는 것은 불가능합니다.

```c
BackgroundWorker worker;
BackgroundWorkerHandle *handle;

memset(&worker, 0, sizeof(worker));
snprintf(worker.bgw_name, BGW_MAXLEN, "dynamic worker %d", MyProcPid);
strcpy(worker.bgw_type, "dynamic_worker");
worker.bgw_flags = BGWORKER_SHMEM_ACCESS | BGWORKER_BACKEND_DATABASE_CONNECTION;
worker.bgw_start_time = BgWorkerStart_RecoveryFinished;
worker.bgw_restart_time = BGW_NEVER_RESTART;
strcpy(worker.bgw_library_name, "my_extension");
strcpy(worker.bgw_function_name, "dynamic_worker_main");
worker.bgw_main_arg = Int32GetDatum(42);
worker.bgw_notify_pid = MyProcPid;  /* 현재 프로세스에 알림 */

if (!RegisterDynamicBackgroundWorker(&worker, &handle))
    ereport(ERROR,
            (errmsg("could not register background worker")));
```

---

### BackgroundWorker 구조체

백그라운드 워커는 `BackgroundWorker` 구조체를 통해 정의됩니다.

```c
typedef void (*bgworker_main_type)(Datum main_arg);

typedef struct BackgroundWorker
{
    char        bgw_name[BGW_MAXLEN];
    char        bgw_type[BGW_MAXLEN];
    int         bgw_flags;
    BgWorkerStartTime bgw_start_time;
    int         bgw_restart_time;        /* 초 단위, 또는 BGW_NEVER_RESTART */
    char        bgw_library_name[MAXPGPATH];
    char        bgw_function_name[BGW_MAXLEN];
    Datum       bgw_main_arg;
    char        bgw_extra[BGW_EXTRALEN];
    pid_t       bgw_notify_pid;
} BackgroundWorker;
```

#### 필드 설명

| 필드 | 설명 |
|------|------|
| `bgw_name` | 로그 메시지와 프로세스 목록에 표시되는 문자열. 프로세스별 고유 정보를 포함해야 함 |
| `bgw_type` | 로그 메시지에 표시되는 문자열. 같은 유형의 모든 워커는 동일한 값을 사용하여 그룹화 |
| `bgw_flags` | 비트 OR 연산으로 결합된 기능 마스크 |
| `bgw_start_time` | 프로세스가 시작되어야 하는 서버 상태 |
| `bgw_restart_time` | 크래시 후 재시작까지 대기 시간(초). 양수 값 또는 `BGW_NEVER_RESTART` |
| `bgw_library_name` | 진입점을 포함하는 라이브러리 이름. 코어 코드인 경우 `"postgres"` 사용 |
| `bgw_function_name` | 초기 진입점 함수 이름. 동적 라이브러리인 경우 `PGDLLEXPORT`로 표시 필요 |
| `bgw_main_arg` | 워커의 메인 함수에 전달되는 `Datum` 인자 |
| `bgw_extra` | `MyBgworkerEntry`를 통해 접근 가능한 추가 데이터 (함수 인자로 전달되지 않음) |
| `bgw_notify_pid` | 시작/종료 시 `SIGUSR1` 알림을 받을 PID. 시작 시 등록된 워커는 0 |

---

### 플래그 옵션 (bgw_flags)

`bgw_flags` 필드는 백그라운드 워커가 요청하는 기능을 지정합니다.

```c
/* 공유 메모리 접근 요청 - 필수 */
BGWORKER_SHMEM_ACCESS

/* 데이터베이스 연결 기능 요청 */
BGWORKER_BACKEND_DATABASE_CONNECTION
```

#### 플래그 조합 규칙

| 플래그 조합 | 설명 |
|------------|------|
| `BGWORKER_SHMEM_ACCESS` | 공유 메모리에만 접근 (필수) |
| `BGWORKER_SHMEM_ACCESS \| BGWORKER_BACKEND_DATABASE_CONNECTION` | 공유 메모리 접근 + 데이터베이스 연결 |

> 중요: `BGWORKER_BACKEND_DATABASE_CONNECTION`을 사용하려면 반드시 `BGWORKER_SHMEM_ACCESS`도 함께 설정해야 합니다. 그렇지 않으면 시작이 실패합니다.

---

### 시작 시점 옵션 (bgw_start_time)

`bgw_start_time`은 백그라운드 워커가 시작되어야 하는 서버 상태를 지정합니다.

| 옵션 | 설명 |
|------|------|
| `BgWorkerStart_PostmasterStart` | `postgres` 초기화 직후 시작. 데이터베이스 연결 불가 |
| `BgWorkerStart_ConsistentState` | 핫 스탠바이(hot standby)에서 일관된 상태 도달 후 시작. 읽기 전용 쿼리 가능 |
| `BgWorkerStart_RecoveryFinished` | 정상적인 읽기-쓰기 상태에서 시작 |

> 참고: 핫 스탠바이가 아닌 서버에서는 `BgWorkerStart_ConsistentState`와 `BgWorkerStart_RecoveryFinished`가 동일하게 동작합니다.

---

### 데이터베이스 연결

백그라운드 워커가 데이터베이스에 연결하려면 다음 함수 중 하나를 사용합니다.

#### 이름으로 연결

```c
void BackgroundWorkerInitializeConnection(char *dbname,
                                          char *username,
                                          uint32 flags)
```

#### OID로 연결

```c
void BackgroundWorkerInitializeConnectionByOid(Oid dboid,
                                               Oid useroid,
                                               uint32 flags)
```

#### 연결 매개변수

| 매개변수 | NULL/InvalidOid 사용 시 |
|----------|------------------------|
| `dbname` / `dboid` | 특정 데이터베이스에 연결하지 않음 (공유 카탈로그만 접근 가능) |
| `username` / `useroid` | `initdb` 시 생성된 슈퍼유저로 실행 |

#### 연결 플래그

| 플래그 | 설명 |
|--------|------|
| `BGWORKER_BYPASS_ALLOWCONN` | 데이터베이스의 `allowconn` 제한 우회 |
| `BGWORKER_BYPASS_ROLELOGINCHECK` | 역할(role)의 로그인 검사 우회 |

#### 연결 예제

```c
void my_worker_main(Datum main_arg)
{
    /* 시그널 핸들러 설정 */
    pqsignal(SIGTERM, my_sigterm_handler);
    BackgroundWorkerUnblockSignals();

    /* 데이터베이스 연결 */
    BackgroundWorkerInitializeConnection("mydb", NULL, 0);

    /* 이제 데이터베이스 작업 수행 가능 */
    SetCurrentStatementStartTimestamp();
    StartTransactionCommand();
    SPI_connect();

    /* SQL 실행 */
    SPI_execute("SELECT count(*) FROM my_table", true, 0);

    SPI_finish();
    CommitTransactionCommand();
}
```

> 제한사항: 연결 함수는 한 번만 호출할 수 있으며, 데이터베이스 전환은 허용되지 않습니다.

---

### 시그널 처리 (Signal Handling)

백그라운드 워커에서 시그널은 초기에 차단되어 있습니다. 명시적으로 차단을 해제해야 합니다.

```c
/* 시그널 차단 해제 */
void BackgroundWorkerUnblockSignals(void)

/* 시그널 차단 */
void BackgroundWorkerBlockSignals(void)
```

#### 시그널 처리 예제

```c
static volatile sig_atomic_t got_sigterm = false;
static volatile sig_atomic_t got_sighup = false;

/* SIGTERM 핸들러 */
static void
my_sigterm_handler(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_sigterm = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

/* SIGHUP 핸들러 (설정 재로드) */
static void
my_sighup_handler(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_sighup = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

void my_worker_main(Datum main_arg)
{
    /* 시그널 핸들러 등록 */
    pqsignal(SIGTERM, my_sigterm_handler);
    pqsignal(SIGHUP, my_sighup_handler);

    /* 시그널 차단 해제 */
    BackgroundWorkerUnblockSignals();

    /* 메인 루프 */
    while (!got_sigterm)
    {
        int rc;

        /* SIGHUP 처리 - 설정 재로드 */
        if (got_sighup)
        {
            got_sighup = false;
            ProcessConfigFile(PGC_SIGHUP);
        }

        /* 대기 */
        rc = WaitLatch(MyLatch,
                       WL_LATCH_SET | WL_TIMEOUT | WL_POSTMASTER_DEATH,
                       1000L,  /* 1초 타임아웃 */
                       PG_WAIT_EXTENSION);
        ResetLatch(MyLatch);

        /* postmaster 종료 확인 */
        if (rc & WL_POSTMASTER_DEATH)
            proc_exit(1);

        /* 작업 수행 */
        do_work();
    }

    proc_exit(0);
}
```

---

### 프로세스 라이프사이클 (Lifecycle)

#### 자동 등록 해제 조건

백그라운드 워커는 다음 조건에서 자동으로 등록 해제됩니다:

1. `bgw_restart_time`이 `BGW_NEVER_RESTART`로 설정됨
2. 종료 코드가 0
3. `TerminateBackgroundWorker`에 의해 종료됨

#### 자동 재시작

- 워커가 크래시되면 `bgw_restart_time` 초 후에 자동으로 재시작됩니다
- postmaster가 재초기화되면 즉시 재시작됩니다

#### 일시 중지

워커를 일시적으로 중지하려면 `WaitLatch()`에 `WL_POSTMASTER_DEATH` 플래그를 사용합니다.

```c
int rc = WaitLatch(MyLatch,
                   WL_LATCH_SET | WL_TIMEOUT | WL_POSTMASTER_DEATH,
                   timeout_ms,
                   PG_WAIT_EXTENSION);

if (rc & WL_POSTMASTER_DEATH)
{
    /* postmaster가 종료됨 - 워커도 종료해야 함 */
    proc_exit(1);
}
```

---

### 동적 워커 관리 함수

동적으로 등록된 백그라운드 워커를 관리하기 위한 함수들입니다.

#### 워커 상태 조회

```c
BgwHandleStatus GetBackgroundWorkerPid(BackgroundWorkerHandle *handle,
                                       pid_t *pidp)
```

반환 값:

| 상태 | 설명 |
|------|------|
| `BGWH_NOT_YET_STARTED` | 아직 시작되지 않음 |
| `BGWH_STOPPED` | 시작되었지만 더 이상 실행 중이지 않음 |
| `BGWH_STARTED` | 현재 실행 중 (두 번째 인자를 통해 PID 반환) |

#### 워커 종료

```c
void TerminateBackgroundWorker(BackgroundWorkerHandle *handle)
```

- 워커가 실행 중이면 `SIGTERM`을 전송합니다.
- 실행 중이지 않으면 등록을 해제합니다.

#### 시작 대기

```c
BgwHandleStatus WaitForBackgroundWorkerStartup(BackgroundWorkerHandle *handle,
                                               pid_t *pidp)
```

postmaster가 워커를 시작하거나 postmaster가 종료될 때까지 호출을 차단합니다.

반환 값:

| 상태 | 설명 |
|------|------|
| `BGWH_STARTED` | 워커 실행 중 (PID가 주소에 기록됨) |
| `BGWH_STOPPED` | 시작되지 않음 |
| `BGWH_POSTMASTER_DIED` | postmaster가 종료됨 |

#### 종료 대기

```c
BgwHandleStatus WaitForBackgroundWorkerShutdown(BackgroundWorkerHandle *handle)
```

백그라운드 워커가 종료되거나 postmaster가 종료될 때까지 호출을 차단합니다.

반환 값:

| 상태 | 설명 |
|------|------|
| `BGWH_STOPPED` | 워커가 종료됨 |
| `BGWH_POSTMASTER_DIED` | postmaster가 종료됨 |

#### 사용 예제

```c
BackgroundWorker worker;
BackgroundWorkerHandle *handle;
pid_t pid;
BgwHandleStatus status;

/* 워커 등록 */
memset(&worker, 0, sizeof(worker));
/* ... 워커 설정 ... */
worker.bgw_notify_pid = MyProcPid;

if (!RegisterDynamicBackgroundWorker(&worker, &handle))
    ereport(ERROR, (errmsg("could not register background worker")));

/* 시작 대기 */
status = WaitForBackgroundWorkerStartup(handle, &pid);

if (status == BGWH_STARTED)
{
    elog(LOG, "background worker started with PID %d", pid);

    /* 작업 완료까지 대기 */
    status = WaitForBackgroundWorkerShutdown(handle);

    if (status == BGWH_STOPPED)
        elog(LOG, "background worker finished");
}
else if (status == BGWH_STOPPED)
{
    ereport(ERROR, (errmsg("background worker failed to start")));
}
else if (status == BGWH_POSTMASTER_DIED)
{
    ereport(FATAL, (errmsg("postmaster died")));
}
```

---

### 비동기 알림 (Asynchronous Notifications)

백그라운드 워커에서 알림을 전송하는 방법은 다음과 같습니다:

#### SPI를 통한 NOTIFY

```c
SPI_connect();
SPI_execute("NOTIFY my_channel, 'payload'", false, 0);
SPI_finish();
```

#### 직접 Async_Notify 호출

```c
#include "commands/async.h"

Async_Notify("my_channel", "payload");
```

> 제한사항: 백그라운드 워커는 `LISTEN` 명령을 사용해서는 안 됩니다. 알림을 소비하기 위한 인프라가 없습니다.

---

### 공유 메모리 접근

백그라운드 워커가 공유 메모리에 접근하려면 `bgw_flags`에 `BGWORKER_SHMEM_ACCESS`를 설정해야 합니다.

#### 공유 메모리 구조체 정의

```c
/* 공유 메모리에 저장될 데이터 구조 */
typedef struct MyWorkerSharedData
{
    LWLock     *lock;           /* 동시성 제어를 위한 경량 락 */
    int         counter;        /* 공유 카운터 */
    bool        worker_active;  /* 워커 활성 상태 */
    char        message[256];   /* 공유 메시지 버퍼 */
} MyWorkerSharedData;

static MyWorkerSharedData *shared_data = NULL;
```

#### 공유 메모리 초기화 (모듈 로드 시)

```c
/* 공유 메모리 훅 */
static shmem_request_hook_type prev_shmem_request_hook = NULL;
static shmem_startup_hook_type prev_shmem_startup_hook = NULL;

static void
my_shmem_request(void)
{
    if (prev_shmem_request_hook)
        prev_shmem_request_hook();

    /* 공유 메모리 크기 요청 */
    RequestAddinShmemSpace(MAXALIGN(sizeof(MyWorkerSharedData)));
    RequestNamedLWLockTranche("my_worker", 1);
}

static void
my_shmem_startup(void)
{
    bool found;

    if (prev_shmem_startup_hook)
        prev_shmem_startup_hook();

    LWLockAcquire(AddinShmemInitLock, LW_EXCLUSIVE);

    shared_data = ShmemInitStruct("my_worker_data",
                                  sizeof(MyWorkerSharedData),
                                  &found);

    if (!found)
    {
        /* 첫 번째 초기화 */
        shared_data->lock = &(GetNamedLWLockTranche("my_worker"))->lock;
        shared_data->counter = 0;
        shared_data->worker_active = false;
        shared_data->message[0] = '\0';
    }

    LWLockRelease(AddinShmemInitLock);
}

void _PG_init(void)
{
    /* 훅 설치 */
    prev_shmem_request_hook = shmem_request_hook;
    shmem_request_hook = my_shmem_request;

    prev_shmem_startup_hook = shmem_startup_hook;
    shmem_startup_hook = my_shmem_startup;

    /* 백그라운드 워커 등록 */
    /* ... */
}
```

#### 공유 메모리 사용 예제

```c
void my_worker_main(Datum main_arg)
{
    /* 시그널 설정 */
    pqsignal(SIGTERM, my_sigterm_handler);
    BackgroundWorkerUnblockSignals();

    /* 워커 시작 표시 */
    LWLockAcquire(shared_data->lock, LW_EXCLUSIVE);
    shared_data->worker_active = true;
    LWLockRelease(shared_data->lock);

    while (!got_sigterm)
    {
        /* 카운터 증가 */
        LWLockAcquire(shared_data->lock, LW_EXCLUSIVE);
        shared_data->counter++;
        snprintf(shared_data->message, sizeof(shared_data->message),
                 "Counter: %d at %ld",
                 shared_data->counter,
                 (long)time(NULL));
        LWLockRelease(shared_data->lock);

        /* 대기 */
        WaitLatch(MyLatch,
                  WL_LATCH_SET | WL_TIMEOUT | WL_POSTMASTER_DEATH,
                  1000L,
                  PG_WAIT_EXTENSION);
        ResetLatch(MyLatch);
    }

    /* 워커 종료 표시 */
    LWLockAcquire(shared_data->lock, LW_EXCLUSIVE);
    shared_data->worker_active = false;
    LWLockRelease(shared_data->lock);

    proc_exit(0);
}
```

---

### 예제 코드

#### 완전한 백그라운드 워커 예제

주기적으로 테이블의 레코드 수를 로그에 기록하는 백그라운드 워커 예제입니다.

```c
/* my_worker.c */

#include "postgres.h"
#include "fmgr.h"
#include "miscadmin.h"
#include "pgstat.h"
#include "postmaster/bgworker.h"
#include "storage/ipc.h"
#include "storage/latch.h"
#include "storage/lwlock.h"
#include "storage/proc.h"
#include "storage/shmem.h"
#include "utils/builtins.h"
#include "executor/spi.h"
#include "access/xact.h"

PG_MODULE_MAGIC;

/* 설정 변수 */
static int worker_interval = 10;  /* 기본 10초 */
static char *worker_database = "postgres";

/* 시그널 플래그 */
static volatile sig_atomic_t got_sigterm = false;
static volatile sig_atomic_t got_sighup = false;

/* 함수 프로토타입 */
void _PG_init(void);
PGDLLEXPORT void my_worker_main(Datum main_arg);

/* SIGTERM 핸들러 */
static void
my_worker_sigterm(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_sigterm = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

/* SIGHUP 핸들러 */
static void
my_worker_sighup(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_sighup = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

/* 워커 메인 함수 */
void
my_worker_main(Datum main_arg)
{
    StringInfoData buf;

    /* 시그널 핸들러 등록 */
    pqsignal(SIGTERM, my_worker_sigterm);
    pqsignal(SIGHUP, my_worker_sighup);

    /* 시그널 차단 해제 */
    BackgroundWorkerUnblockSignals();

    /* 데이터베이스 연결 */
    BackgroundWorkerInitializeConnection(worker_database, NULL, 0);

    elog(LOG, "my_worker started, connected to database '%s'",
         worker_database);

    initStringInfo(&buf);

    /* 메인 루프 */
    while (!got_sigterm)
    {
        int ret;
        int rc;

        /* SIGHUP 처리 */
        if (got_sighup)
        {
            got_sighup = false;
            ProcessConfigFile(PGC_SIGHUP);
            elog(LOG, "my_worker configuration reloaded");
        }

        /* 대기 */
        rc = WaitLatch(MyLatch,
                       WL_LATCH_SET | WL_TIMEOUT | WL_POSTMASTER_DEATH,
                       worker_interval * 1000L,
                       PG_WAIT_EXTENSION);
        ResetLatch(MyLatch);

        /* postmaster 종료 확인 */
        if (rc & WL_POSTMASTER_DEATH)
            proc_exit(1);

        /* 타임아웃 시 작업 수행 */
        if (rc & WL_TIMEOUT)
        {
            SetCurrentStatementStartTimestamp();
            StartTransactionCommand();
            SPI_connect();
            PushActiveSnapshot(GetTransactionSnapshot());

            pgstat_report_activity(STATE_RUNNING,
                                   "querying table counts");

            /* 쿼리 실행 */
            resetStringInfo(&buf);
            appendStringInfo(&buf,
                "SELECT schemaname, relname, n_live_tup "
                "FROM pg_stat_user_tables "
                "ORDER BY n_live_tup DESC LIMIT 5");

            ret = SPI_execute(buf.data, true, 0);

            if (ret == SPI_OK_SELECT && SPI_processed > 0)
            {
                int i;
                elog(LOG, "Top 5 tables by row count:");

                for (i = 0; i < SPI_processed; i++)
                {
                    char *schema = SPI_getvalue(SPI_tuptable->vals[i],
                                               SPI_tuptable->tupdesc, 1);
                    char *table = SPI_getvalue(SPI_tuptable->vals[i],
                                              SPI_tuptable->tupdesc, 2);
                    char *count = SPI_getvalue(SPI_tuptable->vals[i],
                                              SPI_tuptable->tupdesc, 3);

                    elog(LOG, "  %s.%s: %s rows",
                         schema ? schema : "(null)",
                         table ? table : "(null)",
                         count ? count : "0");
                }
            }

            SPI_finish();
            PopActiveSnapshot();
            CommitTransactionCommand();

            pgstat_report_activity(STATE_IDLE, NULL);
        }
    }

    elog(LOG, "my_worker shutting down");
    proc_exit(0);
}

/* 모듈 초기화 */
void
_PG_init(void)
{
    BackgroundWorker worker;

    if (!process_shared_preload_libraries_in_progress)
        return;

    /* GUC 변수 정의 */
    DefineCustomIntVariable("my_worker.interval",
                           "Interval between checks in seconds.",
                           NULL,
                           &worker_interval,
                           10,
                           1,
                           3600,
                           PGC_SIGHUP,
                           0,
                           NULL,
                           NULL,
                           NULL);

    DefineCustomStringVariable("my_worker.database",
                              "Database to connect to.",
                              NULL,
                              &worker_database,
                              "postgres",
                              PGC_POSTMASTER,
                              0,
                              NULL,
                              NULL,
                              NULL);

    /* 워커 구조체 초기화 */
    memset(&worker, 0, sizeof(worker));

    worker.bgw_flags = BGWORKER_SHMEM_ACCESS |
                       BGWORKER_BACKEND_DATABASE_CONNECTION;
    worker.bgw_start_time = BgWorkerStart_RecoveryFinished;
    worker.bgw_restart_time = 10;
    snprintf(worker.bgw_library_name, BGW_MAXLEN, "my_worker");
    snprintf(worker.bgw_function_name, BGW_MAXLEN, "my_worker_main");
    snprintf(worker.bgw_name, BGW_MAXLEN, "my_worker");
    snprintf(worker.bgw_type, BGW_MAXLEN, "my_worker");
    worker.bgw_main_arg = (Datum) 0;
    worker.bgw_notify_pid = 0;

    RegisterBackgroundWorker(&worker);
}
```

#### Makefile

```makefile
MODULES = my_worker

PG_CONFIG = pg_config
PGXS := $(shell $(PG_CONFIG) --pgxs)
include $(PGXS)
```

#### 설치 및 사용

1. 모듈 빌드:
```bash
make
make install
```

2. `postgresql.conf` 수정:
```
shared_preload_libraries = 'my_worker'
my_worker.interval = 60
my_worker.database = 'mydb'
```

3. PostgreSQL 재시작:
```bash
pg_ctl restart
```

---

### 참고사항

#### 전역 변수

- `MyBgworkerEntry`: 등록 시의 `BackgroundWorker` 구조체 복사본을 가리키는 전역 변수

#### 최대 워커 수 제한

- `max_worker_processes` 설정 매개변수로 최대 백그라운드 워커 수가 제한됩니다

#### Windows/EXEC_BACKEND 주의사항

Windows 및 `EXEC_BACKEND` 환경에서는 `Datum`을 참조(reference)로 전달하는 것이 안전하지 않습니다. 값(value)으로만 전달해야 합니다.

- `int32` 또는 작은 값 사용
- 또는 공유 메모리 배열의 인덱스 사용

```c
/* 안전한 방법 */
worker.bgw_main_arg = Int32GetDatum(42);

/* 또는 공유 메모리 인덱스 사용 */
worker.bgw_main_arg = Int32GetDatum(shared_array_index);
```

#### 참고 예제

PostgreSQL 소스 코드의 `src/test/modules/worker_spi`에서 유용한 기법을 보여주는 작동 예제를 확인할 수 있습니다.

---

### 관련 문서

- [서버 프로그래밍 (Server Programming)](https://www.postgresql.org/docs/current/server-programming.html)
- [SPI (Server Programming Interface)](https://www.postgresql.org/docs/current/spi.html)
- [논리적 디코딩 (Logical Decoding)](https://www.postgresql.org/docs/current/logicaldecoding.html)
- [확장 모듈 작성 (Writing Extensions)](https://www.postgresql.org/docs/current/extend-extensions.html)

---

## 제49장. 논리적 디코딩 (Logical Decoding)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/logicaldecoding.html](https://www.postgresql.org/docs/current/logicaldecoding.html)

---

### 목차

- [개요](#개요)
- [49.1. 논리적 디코딩 예제](#491-논리적-디코딩-예제)
  - [49.1.1. SQL 인터페이스 예제](#4911-sql-인터페이스-예제)
  - [49.1.2. 스트리밍 복제 프로토콜 예제](#4912-스트리밍-복제-프로토콜-예제)
  - [49.1.3. 2단계 커밋 예제](#4913-2단계-커밋-예제)
- [49.2. 논리적 디코딩 개념](#492-논리적-디코딩-개념)
  - [49.2.1. 논리적 디코딩](#4921-논리적-디코딩)
  - [49.2.2. 복제 슬롯 (Replication Slots)](#4922-복제-슬롯-replication-slots)
  - [49.2.3. 복제 슬롯 동기화](#4923-복제-슬롯-동기화)
  - [49.2.4. 출력 플러그인 (Output Plugins)](#4924-출력-플러그인-output-plugins)
  - [49.2.5. 내보낸 스냅샷 (Exported Snapshots)](#4925-내보낸-스냅샷-exported-snapshots)
- [49.3. 스트리밍 복제 프로토콜 인터페이스](#493-스트리밍-복제-프로토콜-인터페이스)
- [49.4. SQL 인터페이스](#494-sql-인터페이스)
- [49.5. 시스템 카탈로그](#495-시스템-카탈로그)
- [49.6. 출력 플러그인](#496-출력-플러그인)
  - [49.6.1. 초기화 함수](#4961-초기화-함수)
  - [49.6.2. 콜백 함수](#4962-콜백-함수)
  - [49.6.3. 출력 모드](#4963-출력-모드)
  - [49.6.4. 출력 생성 함수](#4964-출력-생성-함수)
- [49.7. 출력 라이터 (Output Writers)](#497-출력-라이터-output-writers)
- [49.8. 동기식 복제 지원](#498-동기식-복제-지원)
- [49.9. 대규모 트랜잭션 스트리밍](#499-대규모-트랜잭션-스트리밍)
- [49.10. 2단계 커밋 지원](#4910-2단계-커밋-지원)

---

### 개요

논리적 디코딩(Logical Decoding) 은 PostgreSQL이 SQL을 통해 수행된 데이터베이스 수정 사항을 외부 소비자에게 스트리밍할 수 있게 해주는 인프라입니다. 이 기능은 다음을 지원합니다:

- 복제 솔루션: 데이터베이스 간 동기화
- 감사(Auditing): 변경 사항 추적
- 커스텀 데이터 통합 파이프라인: ETL 프로세스 등

변경 사항은 논리적 복제 슬롯(Logical Replication Slots) 으로 식별되는 스트림으로 전송됩니다.

#### 핵심 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 복제 슬롯 (Replication Slot) | 변경 사항 스트림을 식별하고 소비자가 처리한 수정 사항을 추적 |
| 출력 플러그인 (Output Plugin) | 변경 사항이 스트리밍되는 형식을 결정 |
| 변경 소비 방법 | 스트리밍 복제 프로토콜, SQL 인터페이스, 커스텀 출력 라이터 |

#### 출력 플러그인 접근 가능 데이터

모든 출력 플러그인은 다음에 접근할 수 있습니다:

- `INSERT`로 생성된 새 행
- `UPDATE`로 생성된 새 행 버전
- `UPDATE` 및 `DELETE`의 이전 행 버전 (구성된 `REPLICA IDENTITY`에 따라)

---

### 49.1. 논리적 디코딩 예제

이 섹션에서는 SQL 인터페이스와 스트리밍 복제 프로토콜을 사용하여 논리적 디코딩을 제어하는 방법을 보여줍니다.

#### 사전 요구 사항

논리적 디코딩을 사용하기 전에 다음 설정이 필요합니다:

```sql
-- postgresql.conf 설정
wal_level = logical
max_replication_slots = 1  -- 최소 1 이상

-- 2단계 트랜잭션 사용 시
max_prepared_transactions = 1  -- 최소 1 이상
```

> 참고: 슈퍼유저 권한으로 연결해야 합니다.

#### 49.1.1. SQL 인터페이스 예제

##### 논리적 복제 슬롯 생성

```sql
-- 논리적 복제 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot('regression_slot', 'test_decoding', false, true);

-- 결과
    slot_name    |    lsn
-----------------+-----------
 regression_slot | 0/16B1970
(1 row)
```

##### 복제 슬롯 정보 조회

```sql
SELECT slot_name, plugin, slot_type, database, active, restart_lsn, confirmed_flush_lsn
FROM pg_replication_slots;
```

##### 테스트 데이터 생성

```sql
-- 테이블 생성 및 데이터 삽입
CREATE TABLE data(id serial primary key, data text);
INSERT INTO data(data) VALUES('1');
INSERT INTO data(data) VALUES('2');
UPDATE data SET data = 'updated' WHERE id = 1;
DELETE FROM data WHERE id = 2;
```

##### 변경 사항 조회 (소비)

```sql
-- 변경 사항 가져오기 (한 번 읽으면 소비됨)
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);

-- 결과
    lsn    | xid |                          data
-----------+-----+--------------------------------------------------------
 0/16B19A8 | 688 | BEGIN 688
 0/16B19A8 | 688 | table public.data: INSERT: id[integer]:1 data[text]:'1'
 0/16B1A38 | 688 | COMMIT 688
 0/16B1A38 | 689 | BEGIN 689
 0/16B1A38 | 689 | table public.data: INSERT: id[integer]:2 data[text]:'2'
 0/16B1AD0 | 689 | COMMIT 689
 ...
```

##### 변경 사항 미리보기 (소비하지 않음)

```sql
-- 변경 사항 미리보기 (소비되지 않음)
SELECT * FROM pg_logical_slot_peek_changes('regression_slot', NULL, NULL);
```

##### 출력 플러그인 옵션 사용

```sql
-- 타임스탬프 포함 옵션
SELECT * FROM pg_logical_slot_peek_changes('regression_slot', NULL, NULL, 'include-timestamp', 'on');

-- 결과 (타임스탬프 포함)
COMMIT 10299 (at 2017-05-10 12:07:21.272494-04)
```

##### 복제 슬롯 삭제

```sql
-- 더 이상 사용하지 않는 슬롯 삭제
SELECT pg_drop_replication_slot('regression_slot');
```

#### 49.1.2. 스트리밍 복제 프로토콜 예제

`pg_recvlogical` 유틸리티를 사용한 예제입니다:

```bash
# 슬롯 생성
$ pg_recvlogical -d postgres --slot=test --create-slot

# 변경 사항 스트리밍 시작
$ pg_recvlogical -d postgres --slot=test --start -f -
# Ctrl+Z로 백그라운드 전환

# 다른 터미널에서 데이터 삽입
$ psql -d postgres -c "INSERT INTO data(data) VALUES('4');"

# fg로 포그라운드 복귀
$ fg
BEGIN 693
table public.data: INSERT: id[integer]:4 data[text]:'4'
COMMIT 693
# Ctrl+C로 종료

# 슬롯 삭제
$ pg_recvlogical -d postgres --slot=test --drop-slot
```

#### 49.1.3. 2단계 커밋 예제

##### 스트리밍 프로토콜 사용

```bash
# 2단계 커밋 활성화하여 슬롯 생성
$ pg_recvlogical -d postgres --slot=test --create-slot --enable-two-phase

# 스트리밍 시작
$ pg_recvlogical -d postgres --slot=test --start -f -
# Ctrl+Z

# PREPARE TRANSACTION 실행
$ psql -d postgres -c "BEGIN;INSERT INTO data(data) VALUES('5');PREPARE TRANSACTION 'test';"

$ fg
BEGIN 694
table public.data: INSERT: id[integer]:5 data[text]:'5'
PREPARE TRANSACTION 'test', txid 694
# Ctrl+Z

# COMMIT PREPARED 실행
$ psql -d postgres -c "COMMIT PREPARED 'test';"

$ fg
COMMIT PREPARED 'test', txid 694
# Ctrl+C

# 슬롯 삭제
$ pg_recvlogical -d postgres --slot=test --drop-slot
```

##### SQL 인터페이스 사용

```sql
-- PREPARE TRANSACTION
BEGIN;
INSERT INTO data(data) VALUES('5');
PREPARE TRANSACTION 'test_prepared1';

-- 변경 사항 확인
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: PREPARE TRANSACTION 정보 출력

-- COMMIT PREPARED
COMMIT PREPARED 'test_prepared1';
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: COMMIT PREPARED 정보 출력

-- ROLLBACK PREPARED 예제
BEGIN;
INSERT INTO data(data) VALUES('6');
PREPARE TRANSACTION 'test_prepared2';

ROLLBACK PREPARED 'test_prepared2';
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: ROLLBACK PREPARED 정보 출력
```

> 중요: `pg_logical_slot_get_changes()`로 읽은 변경 사항은 소비되어 후속 호출에서 나타나지 않습니다. 더 이상 사용하지 않는 슬롯은 삭제하여 서버 리소스를 확보하세요.

---

### 49.2. 논리적 디코딩 개념

#### 49.2.1. 논리적 디코딩

논리적 디코딩(Logical Decoding) 은 데이터베이스 테이블에 대한 모든 영구적 변경 사항을, 데이터베이스 내부 상태에 대한 세부 지식 없이도 해석 가능한 일관된 형식으로 추출하는 과정입니다.

PostgreSQL은 저장소 수준에서 변경 사항을 기술하는 WAL(Write-Ahead Log) 내용을 디코딩하여 다음과 같은 애플리케이션별 형식으로 변환합니다:

- 튜플 스트림: 행 단위 데이터
- SQL 문: 재실행 가능한 명령어

#### 49.2.2. 복제 슬롯 (Replication Slots)

복제 슬롯(Replication Slot) 은 원본 서버에서 생성된 순서대로 클라이언트에 재생할 수 있는 변경 사항 스트림을 나타냅니다. 각 슬롯은 단일 데이터베이스의 변경 사항 시퀀스를 스트리밍합니다.

##### 주요 특성

| 특성 | 설명 |
|------|------|
| 고유 식별자 | PostgreSQL 클러스터 내 모든 데이터베이스에서 고유 |
| 영속성 | 연결과 독립적으로 유지되며 충돌에 안전 |
| 정확히 한 번 전송 | 정상 운영 시 각 변경 사항을 정확히 한 번만 전송 |
| 위치 저장 | 현재 위치는 체크포인트 시에만 저장됨 |

##### 중요 고려 사항

- 다중 슬롯: 단일 데이터베이스에 대해 각각 고유한 상태를 가진 여러 독립 슬롯이 존재할 수 있음
- 단일 소비자: 주어진 시점에 하나의 수신자만 슬롯의 변경 사항을 소비할 수 있음
- Hot Standby 지원: Hot Standby에서 논리적 복제 슬롯을 생성할 수 있지만 추가 구성 필요

##### Hot Standby 요구 사항

Hot Standby에서 논리적 슬롯을 사용하려면:

- 스탠바이에서 `hot_standby_feedback` 활성화
- 프라이머리와 스탠바이 간 물리적 슬롯 권장
- 프라이머리에서 `wal_level`을 `logical`로 설정

> 주의: 복제 슬롯은 필요한 WAL과 시스템 카탈로그 행의 제거를 방지합니다. 이로 인해 상당한 저장 공간이 소비될 수 있으며, 극단적인 경우 트랜잭션 ID 랩어라운드를 방지하기 위해 데이터베이스가 종료될 수 있습니다. 더 이상 필요하지 않은 슬롯은 반드시 삭제하세요.

#### 49.2.3. 복제 슬롯 동기화

프라이머리의 논리적 복제 슬롯을 Hot Standby에 동기화하여 장애 조치 기능을 제공할 수 있습니다.

##### 구성 요구 사항

```sql
-- 장애 조치 가능한 슬롯 생성
SELECT pg_create_logical_replication_slot('my_slot', 'test_decoding', false, true, 'failover');

-- 또는 CREATE SUBSCRIPTION의 failover 옵션 사용
CREATE SUBSCRIPTION my_sub CONNECTION '...' PUBLICATION my_pub WITH (failover = true);
```

스탠바이에서 필요한 설정:

- `sync_replication_slots` 활성화
- `primary_slot_name` 구성 (물리적 복제 슬롯)
- `hot_standby_feedback` 활성화
- `primary_conninfo`에 유효한 `dbname` 지정

##### 동기화 방법

| 방법 | 설명 |
|------|------|
| 자동 (권장) | `sync_replication_slots`를 통해 slotsync worker가 주기적으로 업데이트 |
| 수동 | `pg_sync_replication_slots()` 함수 사용 (테스트/디버깅용) |

##### 장애 조치 후 재개

```sql
-- 구독의 연결 정보를 새 프라이머리로 업데이트
ALTER SUBSCRIPTION my_sub CONNECTION 'host=new_primary ...';
```

> 주의: 스탠바이를 승격하기 전에 구독을 비활성화하세요. 그렇지 않으면 논리적 구독자가 이전 프라이머리에서 계속 데이터를 받아 데이터 불일치가 발생할 수 있습니다.

#### 49.2.4. 출력 플러그인 (Output Plugins)

출력 플러그인(Output Plugin) 은 WAL의 내부 표현을 복제 슬롯 소비자가 원하는 형식으로 변환합니다.

출력 플러그인은 공유 라이브러리로 구현되며, 코어 코드를 수정하지 않고도 확장 가능합니다. 예제 플러그인은 `contrib/test_decoding`에서 확인할 수 있습니다.

#### 49.2.5. 내보낸 스냅샷 (Exported Snapshots)

스트리밍 복제 인터페이스를 사용하여 새 복제 슬롯을 생성하면, 이후의 모든 변경 사항이 포함되는 시점의 정확한 데이터베이스 상태를 보여주는 스냅샷이 내보내집니다.

##### 사용법

```sql
-- 슬롯 생성 시점의 데이터베이스 상태 읽기
SET TRANSACTION SNAPSHOT 'exported_snapshot_id';
```

이를 통해 다음이 가능합니다:

- 해당 시점의 데이터베이스 상태 덤프
- 변경 사항 손실 없이 슬롯 내용을 사용한 상태 업데이트

##### 스냅샷 내보내기 억제

스냅샷 내보내기가 필요 없는 애플리케이션의 경우:

```sql
SNAPSHOT 'nothing'
```

---

### 49.3. 스트리밍 복제 프로토콜 인터페이스

스트리밍 복제 프로토콜 인터페이스는 복제 연결을 통해서만 사용할 수 있는 논리적 디코딩 관리 명령을 제공합니다. 이 명령들은 표준 SQL로는 사용할 수 없습니다.

#### 핵심 명령어

##### CREATE_REPLICATION_SLOT

```
CREATE_REPLICATION_SLOT slot_name LOGICAL output_plugin
```

논리적 디코딩을 위한 새 복제 슬롯을 생성합니다. 변경 사항을 디코딩할 출력 플러그인을 지정해야 합니다.

##### DROP_REPLICATION_SLOT

```
DROP_REPLICATION_SLOT slot_name [WAIT]
```

기존 복제 슬롯을 제거합니다. 선택적 `WAIT` 절로 블로킹 여부를 제어할 수 있습니다.

##### START_REPLICATION

```
START_REPLICATION SLOT slot_name LOGICAL ...
```

복제 슬롯에서 변경 사항 스트리밍을 시작합니다. 디코딩된 논리적 변경 사항을 클라이언트로 스트리밍합니다.

#### pg_recvlogical 유틸리티

`pg_recvlogical`은 스트리밍 복제 연결을 통해 논리적 디코딩을 제어하는 주요 명령줄 유틸리티입니다. 위 명령을 내부적으로 사용하여 복제 슬롯을 관리하고 변경 사항을 수신합니다.

```bash
# 주요 옵션
pg_recvlogical -d database --slot=slot_name --create-slot [--enable-two-phase]
pg_recvlogical -d database --slot=slot_name --start -f output_file
pg_recvlogical -d database --slot=slot_name --drop-slot
```

---

### 49.4. SQL 인터페이스

PostgreSQL은 논리적 디코딩과 상호작용하기 위한 SQL 수준 API를 제공합니다. 자세한 함수 정보는 섹션 9.28.6 "복제 관리 함수"를 참조하세요.

#### 주요 함수

| 함수 | 설명 |
|------|------|
| `pg_create_logical_replication_slot()` | 논리적 복제 슬롯 생성 |
| `pg_drop_replication_slot()` | 복제 슬롯 삭제 |
| `pg_logical_slot_get_changes()` | 변경 사항 가져오기 (소비) |
| `pg_logical_slot_peek_changes()` | 변경 사항 미리보기 (비소비) |
| `pg_logical_slot_get_binary_changes()` | 바이너리 형식으로 변경 사항 가져오기 |
| `pg_logical_slot_peek_binary_changes()` | 바이너리 형식으로 변경 사항 미리보기 |
| `pg_replication_slot_advance()` | 슬롯 위치 전진 |

#### 제한 사항

> 중요: 동기식 복제(섹션 26.2.8 참조)는 스트리밍 복제 인터페이스를 통해 사용되는 복제 슬롯에서만 지원됩니다. 함수 인터페이스 및 추가 비코어 인터페이스는 동기식 복제를 지원하지 않습니다.

---

### 49.5. 시스템 카탈로그

논리적 디코딩에 관련된 정보를 제공하는 시스템 카탈로그와 뷰입니다.

#### 주요 뷰

| 뷰 | 설명 |
|----|------|
| `pg_replication_slots` | 복제 슬롯의 현재 상태 표시 (물리적 및 논리적) |
| `pg_stat_replication` | 스트리밍 복제 연결 정보 표시 |
| `pg_stat_replication_slots` | 논리적 복제 슬롯의 통계 정보 제공 |

#### 예제 쿼리

```sql
-- 모든 복제 슬롯 정보 조회
SELECT
    slot_name,
    plugin,
    slot_type,
    database,
    active,
    restart_lsn,
    confirmed_flush_lsn
FROM pg_replication_slots;

-- 활성 복제 연결 조회
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn
FROM pg_stat_replication;

-- 논리적 슬롯 통계 조회
SELECT
    slot_name,
    spill_txns,
    spill_count,
    spill_bytes,
    stream_txns,
    stream_count,
    stream_bytes
FROM pg_stat_replication_slots;
```

---

### 49.6. 출력 플러그인

출력 플러그인은 데이터베이스 변경 사항을 임의의 형식으로 디코딩할 수 있게 해주는 공유 라이브러리입니다.

#### 49.6.1. 초기화 함수

출력 플러그인은 `_PG_output_plugin_init()` 함수를 제공해야 하며, 이 함수는 `OutputPluginCallbacks` 구조체를 채웁니다:

```c
typedef struct OutputPluginCallbacks {
    LogicalDecodeStartupCB startup_cb;
    LogicalDecodeBeginCB begin_cb;
    LogicalDecodeChangeCB change_cb;
    LogicalDecodeTruncateCB truncate_cb;
    LogicalDecodeCommitCB commit_cb;
    LogicalDecodeMessageCB message_cb;
    LogicalDecodeFilterByOriginCB filter_by_origin_cb;
    LogicalDecodeShutdownCB shutdown_cb;

    /* 2단계 커밋 콜백 */
    LogicalDecodeFilterPrepareCB filter_prepare_cb;
    LogicalDecodeBeginPrepareCB begin_prepare_cb;
    LogicalDecodePrepareCB prepare_cb;
    LogicalDecodeCommitPreparedCB commit_prepared_cb;
    LogicalDecodeRollbackPreparedCB rollback_prepared_cb;

    /* 스트리밍 콜백 */
    LogicalDecodeStreamStartCB stream_start_cb;
    LogicalDecodeStreamStopCB stream_stop_cb;
    LogicalDecodeStreamAbortCB stream_abort_cb;
    LogicalDecodeStreamPrepareCB stream_prepare_cb;
    LogicalDecodeStreamCommitCB stream_commit_cb;
    LogicalDecodeStreamChangeCB stream_change_cb;
    LogicalDecodeStreamMessageCB stream_message_cb;
    LogicalDecodeStreamTruncateCB stream_truncate_cb;
} OutputPluginCallbacks;
```

#### 49.6.2. 콜백 함수

##### 필수 콜백

| 콜백 | 설명 |
|------|------|
| `begin_cb` | 트랜잭션 시작 시 호출 |
| `change_cb` | 행 수정 시 호출 |
| `commit_cb` | 트랜잭션 종료 시 호출 |

##### 선택적 콜백

| 콜백 | 설명 |
|------|------|
| `startup_cb` | 복제 슬롯 생성 또는 스트리밍 시작 시 호출 |
| `shutdown_cb` | 종료 시 호출 |
| `truncate_cb` | TRUNCATE 시 호출 |
| `message_cb` | 논리적 디코딩 메시지 수신 시 호출 |
| `filter_by_origin_cb` | 원본 기반 필터링 |

##### 스트리밍 지원 콜백 (활성화 시 필수)

- `stream_start_cb`, `stream_stop_cb` - 스트림 블록 경계
- `stream_abort_cb` - 스트리밍 트랜잭션 중단
- `stream_commit_cb` - 스트리밍 트랜잭션 커밋
- `stream_change_cb` - 스트리밍 변경 사항

##### 콜백 함수 시그니처

```c
// 시작 콜백
typedef void (*LogicalDecodeStartupCB) (
    struct LogicalDecodingContext *ctx,
    OutputPluginOptions *options,
    bool is_init
);

// 트랜잭션 시작 콜백
typedef void (*LogicalDecodeBeginCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn
);

// 변경 콜백
typedef void (*LogicalDecodeChangeCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    Relation relation,
    ReorderBufferChange *change
);

// 커밋 콜백
typedef void (*LogicalDecodeCommitCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    XLogRecPtr commit_lsn
);

// TRUNCATE 콜백
typedef void (*LogicalDecodeTruncateCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    int nrelations,
    Relation relations[],
    ReorderBufferChange *change
);
```

#### 49.6.3. 출력 모드

플러그인은 `OutputPluginOptions`를 통해 출력 형식을 선언합니다:

```c
typedef struct OutputPluginOptions {
    OutputPluginOutputType output_type;
    bool receive_rewrites;
} OutputPluginOptions;
```

| 출력 유형 | 설명 |
|-----------|------|
| `OUTPUT_PLUGIN_TEXTUAL_OUTPUT` | 서버 인코딩의 텍스트 데이터 |
| `OUTPUT_PLUGIN_BINARY_OUTPUT` | 바이너리 데이터 |

`receive_rewrites`가 true이면 DDL 중 힙 재작성으로 인한 변경 사항을 수신합니다.

#### 49.6.4. 출력 생성 함수

콜백 내에서 출력 버퍼에 쓰기:

```c
// 출력 작성 예제
OutputPluginPrepareWrite(ctx, last_write);
appendStringInfo(ctx->out, "BEGIN %u", txn->xid);
OutputPluginWrite(ctx, last_write);
```

`last_write` 매개변수는 이것이 콜백의 마지막 쓰기인지 나타냅니다.

#### 플러그인 기능 및 제한

##### 허용됨

- 백엔드 출력 함수 호출
- `pg_catalog` 테이블 및 사용자 정의 카탈로그 테이블에 대한 읽기 전용 접근
- `systable_*` API를 통한 카탈로그 테이블 접근

##### 금지됨

- 트랜잭션 ID 할당
- 테이블에 쓰기
- DDL 변경
- `pg_current_xact_id()` 호출

#### 트랜잭션 의미론

- 동시 트랜잭션은 커밋 순서로 디코딩됨
- 중단된 트랜잭션은 디코딩되지 않음
- 세이브포인트는 부모 트랜잭션에 통합됨
- 디스크에 플러시된 트랜잭션만 디코딩됨 (`synchronous_commit`의 영향을 받음)

---

### 49.7. 출력 라이터 (Output Writers)

출력 라이터를 사용하면 논리적 디코딩에 추가 출력 방법을 구현할 수 있습니다.

#### 필요한 함수

커스텀 출력 라이터를 구현하려면 세 가지 함수가 필요합니다:

1. WAL 읽기 함수 - Write-Ahead Log에서 읽기
2. 출력 준비 함수 - 출력 형식 준비
3. 출력 쓰기 함수 - 실제로 디코딩된 변경 사항 쓰기

구현 세부 사항은 `src/backend/replication/logical/logicalfuncs.c`를 참조하세요.

---

### 49.8. 동기식 복제 지원

논리적 디코딩을 활용하면 스트리밍 복제와 동일한 사용자 인터페이스로 동기식 복제 솔루션을 구축할 수 있습니다.

#### 구현 요구 사항

1. 데이터를 스트리밍하기 위해 스트리밍 복제 인터페이스 (섹션 49.3)를 사용해야 합니다
2. 클라이언트는 스트리밍 복제 클라이언트와 마찬가지로 `Standby status update (F)` 메시지(섹션 54.4)를 보내야 합니다

#### 중요 제한 사항

논리적 디코딩을 통해 변경 사항을 수신하는 동기식 복제본은 단일 데이터베이스 범위 내에서만 동작합니다. `synchronous_standby_names`는 서버 전체 설정이므로, 둘 이상의 데이터베이스를 활발하게 사용하는 환경에서는 이 방식이 올바르게 동작하지 않습니다.

#### 주의 사항: 카탈로그 테이블 데드락

동기식 복제 설정에서 트랜잭션이 사용자 카탈로그 테이블을 배타적으로 잠글 때 데드락이 발생할 수 있습니다.

##### 피해야 할 작업

데드락을 방지하려면 다음을 통해 사용자 카탈로그 테이블에 배타적 잠금을 설정하지 마세요:

| 작업 | 설명 |
|------|------|
| `LOCK` | 트랜잭션에서 `pg_class`에 대한 `LOCK` 발행 |
| `CLUSTER` | 트랜잭션에서 `pg_class`에 대한 `CLUSTER` 수행 |
| 2단계 커밋 | 2단계 트랜잭션 논리적 디코딩이 활성화된 상태에서 `pg_class`에 대한 `LOCK` 후 `PREPARE TRANSACTION` |
| 트리거 | 발행된 테이블에 트리거가 있는 경우 `pg_trigger`에 대한 `CLUSTER` 후 `PREPARE TRANSACTION` |
| `TRUNCATE` | 트랜잭션에서 사용자 카탈로그 테이블에 대한 `TRUNCATE` 실행 |

> 참고: 이러한 명령은 나열된 시스템 카탈로그 테이블뿐만 아니라 다른 카탈로그 테이블에서도 데드락을 유발할 수 있습니다.

---

### 49.9. 대규모 트랜잭션 스트리밍

PostgreSQL의 논리적 디코딩은 적용 지연을 줄이기 위해 대규모 트랜잭션의 스트리밍을 지원합니다.

#### 기본 동작 vs 스트리밍

스트리밍 없이는 트랜잭션의 모든 디코딩된 변경 사항이 커밋 시점에만 전송되므로, 대규모 트랜잭션 복제에 상당한 지연이 발생할 수 있습니다.

##### 기본 콜백 (커밋 시에만 호출)

- `begin_cb`
- `change_cb`
- `commit_cb`
- `message_cb`

##### 스트리밍 콜백 (점진적으로 호출)

필수:
- `stream_start_cb`, `stream_stop_cb`
- `stream_abort_cb`, `stream_commit_cb`
- `stream_change_cb`

선택적:
- `stream_message_cb`, `stream_truncate_cb`

2단계 커밋용:
- `stream_prepare_cb`, `commit_prepared_cb`, `rollback_prepared_cb`

#### 스트리밍 작동 방식

변경 사항은 `stream_start_cb`와 `stream_stop_cb` 콜백으로 구분된 블록 단위로 스트리밍됩니다:

```
stream_start_cb(...);         // 첫 번째 블록 시작
  stream_change_cb(...);
  stream_change_cb(...);
  stream_message_cb(...);
  ...
stream_stop_cb(...);          // 첫 번째 블록 끝

stream_start_cb(...);         // 두 번째 블록 시작
  stream_change_cb(...);
  ...
stream_stop_cb(...);          // 두 번째 블록 끝

// 트랜잭션 커밋
stream_commit_cb(...);        // 일반 커밋
// 또는
stream_prepare_cb(...);       // 2단계 커밋 준비
commit_prepared_cb(...);      // 2단계 커밋 완료
```

#### 스트리밍 트리거 조건

WAL에서 디코딩된 변경 사항의 총량(모든 진행 중인 트랜잭션 합산)이 `logical_decoding_work_mem` 제한을 초과하면 스트리밍이 자동으로 시작됩니다. 이때 가장 큰 최상위 트랜잭션이 스트리밍 대상으로 선택됩니다.

> 참고: 스트리밍이 활성화되어 있어도 불완전한 튜플(예: 해당 메인 테이블 삽입 없이 TOAST 테이블 삽입)이 발생하면 디스크로의 스필이 여전히 발생할 수 있습니다.

#### 주요 보장 사항

- 변경 사항은 커밋 순서로 적용되어 비스트리밍 모드와 동일한 보장 유지
- 일관성을 희생하지 않고 대규모 트랜잭션의 적용 지연 감소

---

### 49.10. 2단계 커밋 지원

PostgreSQL의 논리적 디코딩은 추가 출력 플러그인 콜백을 통해 2단계 커밋 명령(`PREPARE TRANSACTION`, `COMMIT PREPARED`, `ROLLBACK PREPARED`)을 지원합니다.

#### 기본 vs 2단계 디코딩

##### 2단계 지원 없이 (기본 콜백)

- `PREPARE TRANSACTION`은 무시됨
- `COMMIT PREPARED`는 일반 `COMMIT`으로 디코딩됨
- `ROLLBACK PREPARED`는 일반 `ROLLBACK`으로 디코딩됨

##### 2단계 지원 포함

- 변경 사항은 `PREPARE TRANSACTION` 시점에 디코딩되어 출력 플러그인에 전달됨
- 이는 트랜잭션이 커밋될 때만 변경 사항이 전달되는 기본 디코딩과 다름

#### 필수 콜백

2단계 커밋 디코딩을 지원하려면 출력 플러그인이 다음 콜백을 제공해야 합니다:

| 콜백 | 설명 |
|------|------|
| `begin_prepare_cb` | 준비된 트랜잭션 시작 표시 |
| `prepare_cb` | 트랜잭션이 준비될 때 호출 |
| `commit_prepared_cb` | 준비된 트랜잭션이 커밋될 때 호출 |
| `rollback_prepared_cb` | 준비된 트랜잭션이 롤백될 때 호출 |
| `stream_prepare_cb` | 스트리밍되는 준비된 트랜잭션용 |

#### 선택적 콜백

| 콜백 | 설명 |
|------|------|
| `filter_prepare_cb` | `gid`(전역 ID) 패턴 매칭 또는 `xid`(트랜잭션 ID) 조회를 통해 특정 트랜잭션의 2단계 디코딩을 필터링 |

#### 중요 주의 사항

1. 블로킹 위험: 준비된 트랜잭션이 카탈로그 테이블을 배타적으로 잠근 경우, 주 트랜잭션이 커밋될 때까지 디코딩 준비 작업이 차단될 수 있습니다.

2. 데드락 위험: 이 기능을 기반으로 구축된 분산 2단계 커밋 솔루션은 준비된 트랜잭션이 카탈로그 테이블을 잠그면 데드락이 발생할 수 있습니다. 사용자는 이러한 트랜잭션에서 카탈로그 테이블에 대한 명시적 `LOCK` 명령을 피해야 합니다.

---

### 부록: 빠른 참조

#### 구성 매개변수

| 매개변수 | 설명 | 기본값 |
|----------|------|--------|
| `wal_level` | WAL 기록 수준 (논리적 디코딩에는 `logical` 필요) | `replica` |
| `max_replication_slots` | 최대 복제 슬롯 수 | `10` |
| `max_prepared_transactions` | 최대 준비된 트랜잭션 수 (2단계 커밋용) | `0` |
| `logical_decoding_work_mem` | 논리적 디코딩 작업 메모리 | `64MB` |

#### 주요 SQL 함수

```sql
-- 슬롯 관리
pg_create_logical_replication_slot(slot_name, plugin [, temporary, twophase, failover])
pg_drop_replication_slot(slot_name)

-- 변경 사항 조회
pg_logical_slot_get_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_peek_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_get_binary_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_peek_binary_changes(slot_name, upto_lsn, upto_nchanges [, options])

-- 슬롯 제어
pg_replication_slot_advance(slot_name, upto_lsn)
```

#### pg_recvlogical 주요 옵션

```bash
# 슬롯 생성
--create-slot          # 슬롯 생성
--enable-two-phase     # 2단계 커밋 활성화

# 스트리밍
--start                # 스트리밍 시작
-f filename            # 출력 파일 (-f - 는 stdout)

# 슬롯 관리
--drop-slot            # 슬롯 삭제

# 연결
-d database            # 데이터베이스 지정
--slot=name            # 슬롯 이름
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - Logical Decoding](https://www.postgresql.org/docs/current/logicaldecoding.html)
- [PostgreSQL 공식 문서 - Streaming Replication Protocol](https://www.postgresql.org/docs/current/protocol-replication.html)
- [PostgreSQL 공식 문서 - Replication Management Functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-REPLICATION)
- [contrib/test_decoding](https://www.postgresql.org/docs/current/test-decoding.html) - 예제 출력 플러그인

---

## Chapter 50: 복제 진행 추적 (Replication Progress Tracking)

### 목차

1. [개요](#개요)
2. [복제 원본의 개념](#복제-원본의-개념)
3. [복제 원본 등록 및 관리](#복제-원본-등록-및-관리)
4. [진행 상태 관리](#진행-상태-관리)
5. [세션 및 트랜잭션 설정](#세션-및-트랜잭션-설정)
6. [시스템 카탈로그와 뷰](#시스템-카탈로그와-뷰)
7. [복제 원본 활용 예제](#복제-원본-활용-예제)
8. [고급 사용 사례](#고급-사용-사례)

---

### 개요

복제 원본(Replication Origins)은 논리적 디코딩(Logical Decoding)을 기반으로 구축된 논리적 복제 솔루션을 용이하게 하기 위해 설계된 인프라 구성 요소입니다. 복제 원본은 다음 두 가지 주요 과제를 해결합니다.

#### 주요 해결 과제

1. 복제 진행 상태의 안전한 추적
   - 복제 솔루션을 구축할 때 가장 어려운 부분 중 하나는 재생(replay) 진행 상태를 안전하게 추적하는 것입니다.
   - 적용 프로세스(applying process)나 전체 클러스터가 중단될 경우, 데이터가 어디까지 성공적으로 복제되었는지 파악할 수 있어야 합니다.
   - 단순한 해결책(예: 복제된 각 트랜잭션마다 테이블의 행을 업데이트하는 방식)은 런타임 오버헤드와 데이터베이스 비대화(bloat) 문제를 야기합니다.

2. 복제된 행의 재복제 방지
   - 단일 시스템에서 다른 단일 시스템으로의 복제보다 복잡한 복제 토폴로지에서는 이미 복제된 행이 다시 복제되는 것을 방지하기 어렵습니다.
   - 이로 인해 복제 사이클과 비효율성이 발생할 수 있습니다.
   - 복제 원본은 이를 인식하고 방지하는 선택적 메커니즘을 제공합니다.

#### 복제 원본 인프라의 장점

복제 원본 인프라를 사용하면:

- 충돌 안전한 진행 추적: 복제 진행 상태가 충돌에 안전한 방식으로 지속됩니다.
- 오버헤드 최소화: 단순한 솔루션(각 트랜잭션마다 행을 업데이트하는 방식)의 런타임 오버헤드와 데이터베이스 비대화 문제를 제거합니다.
- 복구 용이성: 적용 프로세스 또는 클러스터 장애 시 복구 지점을 쉽게 결정할 수 있습니다.

---

### 복제 원본의 개념

#### 복제 원본의 속성

각 복제 원본에는 두 가지 속성이 있습니다.

| 속성 | 설명 |
|------|------|
| 이름(Name) | 시스템 간에 사용되는 자유 형식 텍스트 식별자입니다. 충돌을 피하기 위해 복제 솔루션 이름을 접두사로 사용해야 합니다. |
| ID | 공간 효율적인 저장을 위한 숫자 식별자입니다. 시스템 간에 공유되지 않습니다. |

#### 복제 원본의 역할

1. 진행 상태 추적: 원격 노드에서 재생 중임을 표시하고 진행 상태를 추적합니다.
2. 원본 태깅: 변경 사항에 생성 세션의 복제 원본을 태그하여 원본에 따라 다르게 처리할 수 있습니다.
3. 필터링 지원: `filter_by_origin_cb` 콜백을 통해 논리적 디코딩 변경 스트림을 원본별로 효율적으로 필터링합니다.

---

### 복제 원본 등록 및 관리

#### 복제 원본 생성

```sql
pg_replication_origin_create(node_name text) → oid
```

복제 원본을 생성하고 내부 ID를 반환합니다.

매개변수:
- `node_name`: 외부 이름 (최대 512바이트)

권한: 기본적으로 슈퍼유저만 사용 가능 (GRANT로 권한 부여 가능)

예제:

```sql
-- 복제 원본 생성
SELECT pg_replication_origin_create('pg_cluster_node1');
-- 결과: 1 (내부 OID 반환)

-- 다른 노드의 복제 원본 생성
SELECT pg_replication_origin_create('pg_cluster_node2');
-- 결과: 2
```

#### 복제 원본 삭제

```sql
pg_replication_origin_drop(node_name text) → void
```

이전에 생성된 복제 원본과 관련된 모든 재생 진행 상태를 삭제합니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 복제 원본 삭제
SELECT pg_replication_origin_drop('pg_cluster_node1');
```

#### 복제 원본 조회

```sql
pg_replication_origin_oid(node_name text) → oid
```

복제 원본 이름으로 내부 ID를 조회합니다. 원본이 없으면 `NULL`을 반환합니다.

예제:

```sql
-- 복제 원본 OID 조회
SELECT pg_replication_origin_oid('pg_cluster_node1');
-- 결과: 1 (또는 NULL)
```

---

### 진행 상태 관리

#### 복제 진행 상태 조회

```sql
pg_replication_origin_progress(node_name text, flush boolean) → pg_lsn
```

지정된 복제 원본의 재생 위치를 반환합니다.

매개변수:
- `node_name`: 복제 원본 이름
- `flush`: 해당 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

예제:

```sql
-- 진행 상태 조회 (플러시 보장)
SELECT pg_replication_origin_progress('pg_cluster_node1', true);
-- 결과: 0/1A4B8C0

-- 진행 상태 조회 (플러시 미보장)
SELECT pg_replication_origin_progress('pg_cluster_node1', false);
-- 결과: 0/1A4B8C0
```

#### 복제 진행 상태 수동 설정

```sql
pg_replication_origin_advance(node_name text, lsn pg_lsn) → void
```

지정된 노드의 복제 진행 상태를 지정된 위치로 설정합니다. 주로 초기 위치 설정이나 구성 변경 후 새 위치 설정에 사용됩니다.

주의: 부주의한 사용은 일관성 없이 복제된 데이터를 초래할 수 있습니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 초기 복제 위치 설정
SELECT pg_replication_origin_advance('pg_cluster_node1', '0/1A4B8C0');

-- 특정 LSN으로 진행 상태 이동
SELECT pg_replication_origin_advance('pg_cluster_node1', '0/2B5C9D1');
```

---

### 세션 및 트랜잭션 설정

#### 세션에 복제 원본 설정

```sql
pg_replication_origin_session_setup(node_name text) → void
```

현재 세션을 지정된 원본에서 재생 중으로 표시하여 재생 진행 상태를 추적할 수 있게 합니다. 이미 원본이 선택된 경우에는 사용할 수 없습니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 세션에 복제 원본 설정
SELECT pg_replication_origin_session_setup('pg_cluster_node1');
```

#### 세션 복제 원본 해제

```sql
pg_replication_origin_session_reset() → void
```

`pg_replication_origin_session_setup()`의 효과를 취소합니다.

예제:

```sql
-- 세션 복제 원본 해제
SELECT pg_replication_origin_session_reset();
```

#### 세션 설정 확인

```sql
pg_replication_origin_session_is_setup() → boolean
```

현재 세션에 복제 원본이 선택되어 있으면 `true`를 반환합니다.

예제:

```sql
-- 세션에 복제 원본이 설정되어 있는지 확인
SELECT pg_replication_origin_session_is_setup();
-- 결과: t (true) 또는 f (false)
```

#### 세션 진행 상태 조회

```sql
pg_replication_origin_session_progress(flush boolean) → pg_lsn
```

현재 세션에서 선택된 복제 원본의 재생 위치를 반환합니다.

매개변수:
- `flush`: 해당 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

예제:

```sql
-- 세션 진행 상태 조회
SELECT pg_replication_origin_session_progress(true);
-- 결과: 0/1A4B8C0
```

#### 트랜잭션 원본 정보 설정

```sql
pg_replication_origin_xact_setup(origin_lsn pg_lsn, origin_timestamp timestamp with time zone) → void
```

현재 트랜잭션을 지정된 LSN과 타임스탬프에서 커밋된 트랜잭션을 재생 중으로 표시합니다. `pg_replication_origin_session_setup()`으로 복제 원본이 선택된 경우에만 호출할 수 있습니다.

매개변수:
- `origin_lsn`: 원본에서 트랜잭션이 커밋된 LSN
- `origin_timestamp`: 트랜잭션이 커밋된 타임스탬프

예제:

```sql
-- 세션 설정 후 트랜잭션 원본 정보 설정
SELECT pg_replication_origin_session_setup('pg_cluster_node1');
SELECT pg_replication_origin_xact_setup('0/1A4B8C0', '2024-01-15 10:30:00+09');

-- 데이터 변경 작업 수행
INSERT INTO my_table (col1, col2) VALUES ('value1', 'value2');
COMMIT;
```

#### 트랜잭션 원본 정보 해제

```sql
pg_replication_origin_xact_reset() → void
```

`pg_replication_origin_xact_setup()`의 효과를 취소합니다.

예제:

```sql
-- 트랜잭션 원본 정보 해제
SELECT pg_replication_origin_xact_reset();
```

---

### 시스템 카탈로그와 뷰

#### pg_replication_origin 시스템 카탈로그

`pg_replication_origin` 카탈로그는 클러스터에 생성된 모든 복제 원본을 포함합니다. 이것은 공유 시스템 카탈로그로, 대부분의 다른 시스템 카탈로그와 달리 데이터베이스별이 아닌 클러스터 전체에 하나만 존재합니다.

| 열(Column) | 타입(Type) | 설명 |
|------------|-----------|------|
| `roident` | `oid` | 복제 원본의 고유한 클러스터 전체 식별자입니다. 시스템 외부로 노출되지 않아야 합니다. |
| `roname` | `text` | 복제 원본의 외부 사용자 정의 이름입니다. |

조회 예제:

```sql
-- 모든 복제 원본 조회
SELECT roident, roname FROM pg_replication_origin;

-- 결과 예시:
--  roident |       roname
-- ---------+--------------------
--        1 | pg_cluster_node1
--        2 | pg_cluster_node2
```

#### pg_replication_origin_status 뷰

`pg_replication_origin_status` 뷰는 모든 복제 원본의 재생 진행 상태를 표시합니다.

| 열(Column) | 타입(Type) | 설명 |
|------------|-----------|------|
| `local_id` | `oid` | 내부 노드 식별자 (`pg_replication_origin.roident` 참조) |
| `external_id` | `text` | 외부 노드 식별자 (`pg_replication_origin.roname` 참조) |
| `remote_lsn` | `pg_lsn` | 데이터가 복제된 원본 노드의 LSN |
| `local_lsn` | `pg_lsn` | `remote_lsn`이 복제된 이 노드의 LSN. 비동기 커밋 사용 시 데이터를 디스크에 저장하기 전에 커밋 레코드를 플러시하는 데 사용됩니다. |

조회 예제:

```sql
-- 모든 복제 원본의 진행 상태 조회
SELECT
    local_id,
    external_id,
    remote_lsn,
    local_lsn
FROM pg_replication_origin_status;

-- 결과 예시:
--  local_id |    external_id     | remote_lsn | local_lsn
-- ----------+--------------------+------------+------------
--         1 | pg_cluster_node1   | 0/1A4B8C0  | 0/2B5C9D1
--         2 | pg_cluster_node2   | 0/3C6D0E2  | 0/4D7E1F3
```

---

### 복제 원본 활용 예제

#### 예제 1: 기본적인 복제 원본 설정

```sql
-- 1. 복제 원본 생성
SELECT pg_replication_origin_create('upstream_node');

-- 2. 생성된 원본 확인
SELECT * FROM pg_replication_origin;

-- 3. 세션에 복제 원본 설정
SELECT pg_replication_origin_session_setup('upstream_node');

-- 4. 세션 설정 확인
SELECT pg_replication_origin_session_is_setup();

-- 5. 트랜잭션 시작 및 원본 정보 설정
BEGIN;
SELECT pg_replication_origin_xact_setup('0/1000000', NOW());

-- 6. 복제할 데이터 적용
INSERT INTO replicated_table (id, data) VALUES (1, 'replicated data');

-- 7. 커밋
COMMIT;

-- 8. 진행 상태 확인
SELECT pg_replication_origin_session_progress(true);

-- 9. 세션 해제
SELECT pg_replication_origin_session_reset();
```

#### 예제 2: 복제 진행 상태 모니터링

```sql
-- 모든 복제 원본의 상세 진행 상태 조회
SELECT
    ros.external_id AS origin_name,
    ros.remote_lsn AS replicated_up_to,
    ros.local_lsn AS local_position,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), ros.local_lsn)
    ) AS lag_size
FROM pg_replication_origin_status ros
JOIN pg_replication_origin ro ON ros.local_id = ro.roident;
```

#### 예제 3: 복제 재개 시 시작점 결정

```sql
-- 복제를 재개할 때 시작점 결정
DO $$
DECLARE
    v_origin_name TEXT := 'upstream_node';
    v_resume_lsn pg_lsn;
BEGIN
    -- 마지막으로 복제된 LSN 조회
    SELECT pg_replication_origin_progress(v_origin_name, true)
    INTO v_resume_lsn;

    IF v_resume_lsn IS NULL THEN
        RAISE NOTICE 'No previous replication progress found. Starting from scratch.';
    ELSE
        RAISE NOTICE 'Resuming replication from LSN: %', v_resume_lsn;
    END IF;
END $$;
```

#### 예제 4: 양방향 복제에서 루프 방지

```sql
-- 양방향 복제 설정에서 각 노드의 원본 등록

-- Node A에서 실행
SELECT pg_replication_origin_create('node_b');

-- Node B에서 실행
SELECT pg_replication_origin_create('node_a');

-- 데이터 적용 시 원본 표시 (Node A에서 Node B의 데이터 적용)
SELECT pg_replication_origin_session_setup('node_b');

BEGIN;
SELECT pg_replication_origin_xact_setup('0/1234567', NOW());
-- 이 변경 사항은 node_b에서 온 것으로 표시됨
INSERT INTO shared_table (id, value) VALUES (1, 'from node_b');
COMMIT;

SELECT pg_replication_origin_session_reset();
```

---

### 고급 사용 사례

#### 복잡한 복제 토폴로지에서의 원본 필터링

복잡한 복제 시나리오에서 변경 사항은 생성 세션의 복제 원본으로 태그됩니다. 이를 통해:

1. 출력 플러그인이 원본에 따라 변경 사항을 다르게 처리할 수 있습니다.
2. `filter_by_origin_cb` 콜백을 통해 논리적 디코딩 변경 스트림을 원본별로 효율적으로 필터링할 수 있습니다.
3. 출력 플러그인 내에서 직접 필터링하는 것보다 더 효율적입니다.

#### 충돌 안전한 진행 추적 구현

```sql
-- 복제 원본을 사용한 안전한 진행 추적 예제
CREATE OR REPLACE FUNCTION safe_apply_transaction(
    p_origin_name TEXT,
    p_origin_lsn pg_lsn,
    p_origin_timestamp TIMESTAMPTZ,
    p_sql_commands TEXT[]
) RETURNS void AS $$
DECLARE
    v_cmd TEXT;
    v_last_applied_lsn pg_lsn;
BEGIN
    -- 이미 적용된 트랜잭션인지 확인
    SELECT pg_replication_origin_progress(p_origin_name, true)
    INTO v_last_applied_lsn;

    IF v_last_applied_lsn >= p_origin_lsn THEN
        RAISE NOTICE 'Transaction at LSN % already applied. Skipping.', p_origin_lsn;
        RETURN;
    END IF;

    -- 세션 설정
    PERFORM pg_replication_origin_session_setup(p_origin_name);

    -- 트랜잭션 원본 정보 설정
    PERFORM pg_replication_origin_xact_setup(p_origin_lsn, p_origin_timestamp);

    -- SQL 명령 실행
    FOREACH v_cmd IN ARRAY p_sql_commands
    LOOP
        EXECUTE v_cmd;
    END LOOP;

    -- 세션 해제
    PERFORM pg_replication_origin_session_reset();

EXCEPTION WHEN OTHERS THEN
    -- 오류 발생 시 세션 해제
    PERFORM pg_replication_origin_session_reset();
    RAISE;
END;
$$ LANGUAGE plpgsql;
```

#### 복제 원본 관리 유틸리티

```sql
-- 복제 원본 상태 요약 뷰 생성
CREATE OR REPLACE VIEW replication_origin_summary AS
SELECT
    ro.roident AS origin_id,
    ro.roname AS origin_name,
    ros.remote_lsn,
    ros.local_lsn,
    CASE
        WHEN ros.remote_lsn IS NULL THEN 'Not Started'
        ELSE 'Active'
    END AS status
FROM pg_replication_origin ro
LEFT JOIN pg_replication_origin_status ros ON ro.roident = ros.local_id;

-- 사용
SELECT * FROM replication_origin_summary;
```

---

### 함수 요약 표

| 함수 | 설명 | 권한 |
|------|------|------|
| `pg_replication_origin_create(text)` | 복제 원본 생성 | 슈퍼유저 |
| `pg_replication_origin_drop(text)` | 복제 원본 삭제 | 슈퍼유저 |
| `pg_replication_origin_oid(text)` | 복제 원본 OID 조회 | 모든 사용자 |
| `pg_replication_origin_session_setup(text)` | 세션에 복제 원본 설정 | 슈퍼유저 |
| `pg_replication_origin_session_reset()` | 세션 복제 원본 해제 | 슈퍼유저 |
| `pg_replication_origin_session_is_setup()` | 세션 설정 여부 확인 | 슈퍼유저 |
| `pg_replication_origin_session_progress(boolean)` | 세션 진행 상태 조회 | 슈퍼유저 |
| `pg_replication_origin_xact_setup(pg_lsn, timestamptz)` | 트랜잭션 원본 정보 설정 | 슈퍼유저 |
| `pg_replication_origin_xact_reset()` | 트랜잭션 원본 정보 해제 | 슈퍼유저 |
| `pg_replication_origin_advance(text, pg_lsn)` | 진행 상태 수동 설정 | 슈퍼유저 |
| `pg_replication_origin_progress(text, boolean)` | 특정 원본 진행 상태 조회 | 슈퍼유저 |

---

### 관련 문서

- [Chapter 47: 논리적 디코딩 (Logical Decoding)](40_logical_decoding.md)
- [Chapter 27: 고가용성, 로드 밸런싱, 복제 (High Availability)](19_high_availability.md)
- [Chapter 31: 논리적 복제 (Logical Replication)](24_logical_replication.md)

---

### 참고 자료

- [PostgreSQL 공식 문서: Replication Progress Tracking](https://www.postgresql.org/docs/current/replication-origins.html)
- [PostgreSQL 공식 문서: pg_replication_origin 카탈로그](https://www.postgresql.org/docs/current/catalog-pg-replication-origin.html)
- [PostgreSQL 공식 문서: pg_replication_origin_status 뷰](https://www.postgresql.org/docs/current/view-pg-replication-origin-status.html)
- [PostgreSQL 공식 문서: 시스템 관리 함수](https://www.postgresql.org/docs/current/functions-admin.html)

---

## Chapter 51. 아카이브 모듈 (Archive Modules)

PostgreSQL은 연속 아카이빙(Continuous Archiving)을 위한 커스텀 모듈을 생성할 수 있는 인프라를 제공합니다. 아카이브 모듈은 셸 명령 기반 아카이빙(`archive_command` 사용)보다 더 강력하고 성능이 뛰어난 대안을 제공합니다.

### 51.1. 아카이브 모듈 개요 (Overview)

커스텀 `archive_library`가 구성되면, PostgreSQL은 완료된 WAL(Write-Ahead Log) 파일을 모듈에 제출합니다. 서버는 모듈이 아카이빙 성공을 확인할 때까지 WAL 파일을 재사용하거나 삭제하지 않습니다. 모듈은 각 WAL 파일에 대해 무엇을 할지 유연하게 결정할 수 있습니다.

#### 아카이브 모듈의 장점

| 특징 | archive_command | archive_library |
|------|-----------------|-----------------|
| 성능 | 매 파일마다 셸 프로세스 생성 | 로드된 라이브러리 직접 호출 |
| 유연성 | 셸 명령으로 제한 | C 코드로 완전한 제어 |
| 오류 처리 | 종료 코드 기반 | 상세한 오류 보고 가능 |
| 상태 관리 | 파일/외부 저장소 필요 | 메모리 내 상태 유지 가능 |

#### 아카이브 모듈 구성 요소

아카이브 모듈은 다음 구성 요소를 포함해야 합니다:

1. 초기화 함수 (Initialization Function) - 모듈의 필수 진입점
2. 콜백 함수들 (Callbacks) - 다양한 아카이빙 단계를 처리하는 함수들

또한 아카이브 모듈은 다음과 같은 추가 기능을 수행할 수 있습니다:
- 커스텀 GUC(Grand Unified Configuration) 매개변수 선언
- 백그라운드 워커 등록
- 기본 아카이빙 이상의 커스텀 로직 구현

---

### 51.2. 초기화 함수 (Initialization Functions)

아카이브 라이브러리는 `archive_library` 구성 매개변수를 사용하여 공유 라이브러리를 동적으로 로드함으로써 로드됩니다. 라이브러리는 자신이 유효한 아카이브 모듈임을 나타내는 특정 초기화 함수를 제공해야 합니다.

#### 필수 초기화 함수

```c
typedef const ArchiveModuleCallbacks *(*ArchiveModuleInit) (void);
```

함수 이름: `_PG_archive_module_init`

이 초기화 함수는 다음 조건을 충족해야 합니다:

- 매개변수를 받지 않음
- `ArchiveModuleCallbacks` 구조체에 대한 포인터를 반환
- 서버 수명 동안 유효한 값을 반환 (일반적으로 전역 범위에서 `static const`로 정의)

#### ArchiveModuleCallbacks 구조체

```c
typedef struct ArchiveModuleCallbacks
{
    ArchiveStartupCB startup_cb;           /* 시작 콜백 (선택) */
    ArchiveCheckConfiguredCB check_configured_cb;  /* 구성 확인 콜백 (선택) */
    ArchiveFileCB archive_file_cb;         /* 파일 아카이브 콜백 (필수) */
    ArchiveShutdownCB shutdown_cb;         /* 종료 콜백 (선택) */
} ArchiveModuleCallbacks;
```

#### 콜백 요구사항

| 콜백 | 필수 여부 | 설명 |
|------|----------|------|
| `startup_cb` | 선택 | 모듈 초기화 |
| `check_configured_cb` | 선택 | 구성 유효성 확인 |
| `archive_file_cb` | 필수 | WAL 파일 아카이빙 |
| `shutdown_cb` | 선택 | 정리 및 종료 |

#### 초기화 함수 예제

```c
#include "postgres.h"
#include "archive/archive_module.h"
#include "fmgr.h"

PG_MODULE_MAGIC;

/* 함수 프로토타입 선언 */
static void my_archive_startup(ArchiveModuleState *state);
static bool my_archive_check_configured(ArchiveModuleState *state);
static bool my_archive_file(ArchiveModuleState *state,
                            const char *file, const char *path);
static void my_archive_shutdown(ArchiveModuleState *state);

/* 콜백 구조체 정의 (static const로 서버 수명 동안 유효) */
static const ArchiveModuleCallbacks my_archive_callbacks = {
    .startup_cb = my_archive_startup,
    .check_configured_cb = my_archive_check_configured,
    .archive_file_cb = my_archive_file,
    .shutdown_cb = my_archive_shutdown
};

/*
 * _PG_archive_module_init
 *
 * 아카이브 모듈 초기화 함수
 * PostgreSQL이 모듈 로드 시 자동으로 호출합니다.
 */
const ArchiveModuleCallbacks *
_PG_archive_module_init(void)
{
    return &my_archive_callbacks;
}
```

---

### 51.3. 아카이브 모듈 콜백 (Archive Module Callbacks)

아카이브 모듈 콜백은 모듈의 실제 아카이빙 동작을 정의합니다. 서버는 각 개별 WAL 파일을 처리하기 위해 필요에 따라 이들을 호출합니다.

#### 51.3.1. 시작 콜백 (Startup Callback)

```c
typedef void (*ArchiveStartupCB) (ArchiveModuleState *state);
```

`startup_cb` 콜백은 모듈이 로드된 직후에 호출됩니다. 이 콜백은 필요한 추가 초기화를 수행하는 데 사용할 수 있습니다. 아카이브 모듈에 상태가 있는 경우 `state->private_data`를 사용하여 저장할 수 있습니다.

##### 시작 콜백 예제

```c
/* 모듈 상태를 저장하기 위한 구조체 */
typedef struct MyArchiveState
{
    MemoryContext archive_context;  /* 장기 메모리 컨텍스트 */
    int files_archived;             /* 아카이브된 파일 수 */
    char *destination_dir;          /* 대상 디렉토리 */
} MyArchiveState;

static void
my_archive_startup(ArchiveModuleState *state)
{
    MyArchiveState *mystate;
    MemoryContext old_context;

    /*
     * TopMemoryContext에서 장기 메모리 컨텍스트 생성
     * 이 컨텍스트는 아카이버 프로세스 수명 동안 유지됩니다.
     */
    mystate = (MyArchiveState *)
        MemoryContextAllocZero(TopMemoryContext, sizeof(MyArchiveState));

    mystate->archive_context = AllocSetContextCreate(TopMemoryContext,
                                                      "MyArchiveContext",
                                                      ALLOCSET_DEFAULT_SIZES);
    mystate->files_archived = 0;

    /* 구성 매개변수에서 대상 디렉토리 복사 */
    old_context = MemoryContextSwitchTo(mystate->archive_context);
    mystate->destination_dir = pstrdup(my_archive_directory);
    MemoryContextSwitchTo(old_context);

    /* 상태를 private_data에 저장 */
    state->private_data = mystate;

    elog(LOG, "my_archive module initialized");
}
```

#### 51.3.2. 구성 확인 콜백 (Check Callback)

```c
typedef bool (*ArchiveCheckConfiguredCB) (ArchiveModuleState *state);
```

`check_configured_cb` 콜백은 모듈이 완전히 구성되어 WAL 파일을 받아들일 준비가 되었는지 판단합니다 (예: 구성 매개변수가 유효한 값으로 설정되어 있는지 확인).

##### 반환 값

| 반환 값 | 동작 |
|---------|------|
| `true` | 서버가 `archive_file_cb`를 호출하여 아카이빙 진행 |
| `false` | 아카이빙 진행하지 않음; 서버가 경고를 발생시키고 주기적으로 재시도 |

`check_configured_cb`가 정의되지 않은 경우, 서버는 모듈이 항상 구성된 것으로 간주합니다.

> 참고: `false`를 반환하기 전에 `arch_module_check_errdetail` 매크로를 사용하여 경고 메시지에 상세 정보를 추가할 수 있습니다.

##### 구성 확인 콜백 예제

```c
/* GUC 변수 - postgresql.conf에서 설정 */
static char *my_archive_directory = NULL;

static bool
my_archive_check_configured(ArchiveModuleState *state)
{
    /* 아카이브 디렉토리가 설정되었는지 확인 */
    if (my_archive_directory == NULL || my_archive_directory[0] == '\0')
    {
        arch_module_check_errdetail("my_archive.directory is not set.");
        return false;
    }

    /* 디렉토리가 존재하고 쓰기 가능한지 확인 */
    if (access(my_archive_directory, W_OK) != 0)
    {
        arch_module_check_errdetail("my_archive.directory '%s' is not "
                                    "accessible: %m",
                                    my_archive_directory);
        return false;
    }

    return true;
}
```

#### 51.3.3. 아카이브 콜백 (Archive Callback)

```c
typedef bool (*ArchiveFileCB) (ArchiveModuleState *state,
                               const char *file,
                               const char *path);
```

`archive_file_cb` 콜백은 단일 WAL 파일을 아카이브합니다. 이것은 필수 콜백입니다.

##### 매개변수

| 매개변수 | 설명 |
|----------|------|
| `state` | 아카이브 모듈 상태 |
| `file` | 아카이브할 WAL 파일의 파일 이름만 (예: `000000010000000000000001`) |
| `path` | WAL 파일의 전체 경로 (파일 이름 포함, 예: `/var/lib/postgresql/data/pg_wal/000000010000000000000001`) |

##### 반환 값

| 반환 값 | 동작 |
|---------|------|
| `true` | 파일이 성공적으로 아카이브됨; 서버가 원본 WAL 파일을 재사용하거나 제거할 수 있음 |
| `false` 또는 오류 발생 | 서버가 원본 WAL 파일을 유지하고 나중에 재시도 |

> 참고: 이 콜백은 호출마다 재설정되는 단기 메모리 컨텍스트에서 실행됩니다. 수명이 긴 저장소가 필요한 경우 `startup_cb` 콜백에서 별도의 메모리 컨텍스트를 생성하세요.

##### 아카이브 콜백 예제

```c
static bool
my_archive_file(ArchiveModuleState *state,
                const char *file,
                const char *path)
{
    MyArchiveState *mystate = (MyArchiveState *) state->private_data;
    char destination[MAXPGPATH];
    struct stat st;

    /* 대상 파일 경로 생성 */
    snprintf(destination, MAXPGPATH, "%s/%s",
             mystate->destination_dir, file);

    /*
     * 대상에 동일한 파일이 이미 존재하는지 확인
     * (멱등성 보장을 위해)
     */
    if (stat(destination, &st) == 0)
    {
        struct stat src_st;

        if (stat(path, &src_st) == 0 && st.st_size == src_st.st_size)
        {
            elog(DEBUG1, "file %s already exists with same size, skipping",
                 file);
            return true;
        }
    }

    /*
     * 파일 복사
     * 실제 구현에서는 copy_file() 함수나 적절한 방법 사용
     */
    if (!copy_file_to_destination(path, destination))
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not archive file \"%s\": %m", file)));
        return false;
    }

    /* fsync 호출로 내구성 보장 */
    if (fsync(destination) != 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not fsync file \"%s\": %m", destination)));
        return false;
    }

    mystate->files_archived++;
    elog(LOG, "archived %s to %s (total: %d files)",
         file, destination, mystate->files_archived);

    return true;
}
```

#### 51.3.4. 종료 콜백 (Shutdown Callback)

```c
typedef void (*ArchiveShutdownCB) (ArchiveModuleState *state);
```

`shutdown_cb` 콜백은 아카이버 프로세스가 종료될 때 (예: 오류 발생 후) 또는 `archive_library` 값이 변경될 때 호출됩니다.

`shutdown_cb`가 정의되지 않으면 특별한 동작이 수행되지 않습니다. 아카이브 모듈에 상태가 있는 경우, 메모리 누수를 방지하기 위해 이 콜백에서 해당 상태를 해제해야 합니다.

##### 종료 콜백 예제

```c
static void
my_archive_shutdown(ArchiveModuleState *state)
{
    MyArchiveState *mystate = (MyArchiveState *) state->private_data;

    if (mystate != NULL)
    {
        elog(LOG, "my_archive shutting down after archiving %d files",
             mystate->files_archived);

        /* 메모리 컨텍스트 해제 */
        if (mystate->archive_context != NULL)
            MemoryContextDelete(mystate->archive_context);

        /* 상태 구조체 해제 */
        pfree(mystate);
        state->private_data = NULL;
    }
}
```

---

### 51.4. 완전한 아카이브 모듈 예제

다음은 WAL 파일을 지정된 디렉토리에 복사하는 간단하지만 완전한 아카이브 모듈의 예제입니다.

```c
/*
 * simple_archive.c
 *
 * WAL 파일을 지정된 디렉토리에 복사하는 간단한 아카이브 모듈
 *
 * 빌드: gcc -shared -fPIC -o simple_archive.so simple_archive.c \
 *       $(pg_config --cflags) -I$(pg_config --includedir-server)
 *
 * 설정:
 *   archive_mode = on
 *   archive_library = 'simple_archive'
 *   simple_archive.directory = '/path/to/archive'
 */

#include "postgres.h"

#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#include "archive/archive_module.h"
#include "common/file_perm.h"
#include "miscadmin.h"
#include "storage/copydir.h"
#include "storage/fd.h"
#include "utils/guc.h"
#include "utils/memutils.h"

PG_MODULE_MAGIC;

/* GUC 변수 */
static char *archive_directory = NULL;

/* 모듈 상태 */
typedef struct SimpleArchiveState
{
    MemoryContext context;
    int archived_count;
} SimpleArchiveState;

/* 함수 프로토타입 */
static void simple_archive_startup(ArchiveModuleState *state);
static bool simple_archive_configured(ArchiveModuleState *state);
static bool simple_archive_file(ArchiveModuleState *state,
                                const char *file, const char *path);
static void simple_archive_shutdown(ArchiveModuleState *state);

/* 콜백 구조체 */
static const ArchiveModuleCallbacks simple_archive_callbacks = {
    .startup_cb = simple_archive_startup,
    .check_configured_cb = simple_archive_configured,
    .archive_file_cb = simple_archive_file,
    .shutdown_cb = simple_archive_shutdown
};

/*
 * _PG_archive_module_init
 * 모듈 초기화 - GUC 정의 및 콜백 반환
 */
const ArchiveModuleCallbacks *
_PG_archive_module_init(void)
{
    /* GUC 매개변수 정의 */
    DefineCustomStringVariable("simple_archive.directory",
                               "Directory where WAL files are archived.",
                               NULL,
                               &archive_directory,
                               "",
                               PGC_SIGHUP,
                               0,
                               NULL, NULL, NULL);

    MarkGUCPrefixReserved("simple_archive");

    return &simple_archive_callbacks;
}

/*
 * simple_archive_startup
 * 모듈 시작 시 상태 초기화
 */
static void
simple_archive_startup(ArchiveModuleState *state)
{
    SimpleArchiveState *mystate;

    mystate = (SimpleArchiveState *)
        MemoryContextAllocZero(TopMemoryContext,
                               sizeof(SimpleArchiveState));

    mystate->context = AllocSetContextCreate(TopMemoryContext,
                                              "SimpleArchiveContext",
                                              ALLOCSET_DEFAULT_SIZES);
    mystate->archived_count = 0;

    state->private_data = mystate;

    ereport(LOG,
            (errmsg("simple_archive module initialized")));
}

/*
 * simple_archive_configured
 * 모듈이 올바르게 구성되었는지 확인
 */
static bool
simple_archive_configured(ArchiveModuleState *state)
{
    if (archive_directory == NULL || archive_directory[0] == '\0')
    {
        arch_module_check_errdetail("simple_archive.directory is not set.");
        return false;
    }

    /* 디렉토리 존재 및 쓰기 권한 확인 */
    if (access(archive_directory, W_OK) != 0)
    {
        arch_module_check_errdetail("simple_archive.directory '%s' does not "
                                    "exist or is not writable.",
                                    archive_directory);
        return false;
    }

    return true;
}

/*
 * simple_archive_file
 * WAL 파일을 아카이브 디렉토리에 복사
 */
static bool
simple_archive_file(ArchiveModuleState *state,
                    const char *file,
                    const char *path)
{
    SimpleArchiveState *mystate = (SimpleArchiveState *) state->private_data;
    char destination[MAXPGPATH];
    char temp_destination[MAXPGPATH];
    int src_fd = -1;
    int dest_fd = -1;
    char *buffer = NULL;
    struct stat st;
    bool success = false;

    /* 대상 경로 생성 */
    snprintf(destination, MAXPGPATH, "%s/%s", archive_directory, file);
    snprintf(temp_destination, MAXPGPATH, "%s/%s.tmp",
             archive_directory, file);

    /* 이미 아카이브된 파일인지 확인 (멱등성) */
    if (stat(destination, &st) == 0)
    {
        ereport(DEBUG1,
                (errmsg("file \"%s\" already archived", file)));
        return true;
    }

    /* 소스 파일 열기 */
    src_fd = OpenTransientFile(path, O_RDONLY | PG_BINARY);
    if (src_fd < 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not open WAL file \"%s\": %m", path)));
        return false;
    }

    /* 소스 파일 크기 확인 */
    if (fstat(src_fd, &st) != 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not stat WAL file \"%s\": %m", path)));
        goto cleanup;
    }

    /* 임시 파일 생성 */
    dest_fd = OpenTransientFile(temp_destination,
                                O_WRONLY | O_CREAT | O_TRUNC | PG_BINARY);
    if (dest_fd < 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not create archive file \"%s\": %m",
                        temp_destination)));
        goto cleanup;
    }

    /* 버퍼 할당 및 복사 */
    buffer = palloc(BLCKSZ);

    for (;;)
    {
        ssize_t bytes_read;
        ssize_t bytes_written;

        bytes_read = read(src_fd, buffer, BLCKSZ);
        if (bytes_read < 0)
        {
            ereport(WARNING,
                    (errcode_for_file_access(),
                     errmsg("could not read WAL file \"%s\": %m", path)));
            goto cleanup;
        }

        if (bytes_read == 0)
            break;  /* EOF */

        bytes_written = write(dest_fd, buffer, bytes_read);
        if (bytes_written != bytes_read)
        {
            ereport(WARNING,
                    (errcode_for_file_access(),
                     errmsg("could not write to archive file \"%s\": %m",
                            temp_destination)));
            goto cleanup;
        }
    }

    /* fsync 호출 */
    if (pg_fsync(dest_fd) != 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not fsync archive file \"%s\": %m",
                        temp_destination)));
        goto cleanup;
    }

    /* 임시 파일을 최종 파일로 이름 변경 (원자적 연산) */
    if (rename(temp_destination, destination) != 0)
    {
        ereport(WARNING,
                (errcode_for_file_access(),
                 errmsg("could not rename \"%s\" to \"%s\": %m",
                        temp_destination, destination)));
        goto cleanup;
    }

    mystate->archived_count++;

    ereport(DEBUG1,
            (errmsg("archived \"%s\" to \"%s\"", file, destination)));

    success = true;

cleanup:
    if (buffer != NULL)
        pfree(buffer);
    if (dest_fd >= 0)
        CloseTransientFile(dest_fd);
    if (src_fd >= 0)
        CloseTransientFile(src_fd);

    /* 실패 시 임시 파일 정리 */
    if (!success)
        unlink(temp_destination);

    return success;
}

/*
 * simple_archive_shutdown
 * 모듈 종료 시 정리
 */
static void
simple_archive_shutdown(ArchiveModuleState *state)
{
    SimpleArchiveState *mystate = (SimpleArchiveState *) state->private_data;

    if (mystate != NULL)
    {
        ereport(LOG,
                (errmsg("simple_archive shutting down, archived %d files",
                        mystate->archived_count)));

        if (mystate->context != NULL)
            MemoryContextDelete(mystate->context);

        pfree(mystate);
        state->private_data = NULL;
    }
}
```

---

### 51.5. 아카이브 모듈 구성 (Configuration)

아카이브 모듈을 사용하려면 `postgresql.conf`에서 다음과 같이 구성합니다:

```ini
# 아카이브 모드 활성화
archive_mode = on

# archive_command 대신 archive_library 사용
archive_library = 'simple_archive'

# 모듈별 설정 (모듈에 따라 다름)
simple_archive.directory = '/var/lib/postgresql/archive'
```

#### 구성 매개변수

| 매개변수 | 설명 |
|----------|------|
| `archive_mode` | `on`으로 설정하여 아카이빙 활성화 |
| `archive_library` | 사용할 아카이브 모듈 라이브러리 이름 |
| `archive_command` | `archive_library`가 설정되면 무시됨 |

> 참고: `archive_library`와 `archive_command`를 동시에 설정하면 `archive_library`가 우선합니다.

#### 라이브러리 검색 경로

PostgreSQL은 다음 순서로 아카이브 라이브러리를 검색합니다:

1. `dynamic_library_path`에 지정된 디렉토리
2. PostgreSQL 설치 디렉토리의 `lib` 하위 디렉토리

---

### 51.6. 기본 제공 예제: basic_archive

PostgreSQL 소스 코드의 `contrib/basic_archive` 모듈은 유용한 구현 기술을 보여주는 작동하는 예제를 제공합니다.

#### basic_archive 설치

```bash
# PostgreSQL 소스 디렉토리에서
cd contrib/basic_archive
make
make install
```

#### basic_archive 사용

```ini
# postgresql.conf
archive_mode = on
archive_library = 'basic_archive'
basic_archive.archive_directory = '/path/to/archive'
```

---

### 51.7. 모범 사례 (Best Practices)

#### 멱등성 (Idempotency)

아카이브 콜백은 멱등적이어야 합니다. 동일한 WAL 파일에 대해 여러 번 호출되더라도 올바르게 동작해야 합니다.

```c
/* 이미 아카이브된 파일 확인 */
if (file_already_archived(destination, source_size))
    return true;  /* 이미 완료됨 */
```

#### 원자적 쓰기 (Atomic Writes)

임시 파일에 쓴 후 이름 변경을 사용하여 원자적 쓰기를 보장합니다.

```c
/* 임시 파일에 쓰기 */
write_to_temp_file(temp_path, data);

/* fsync 호출 */
fsync(temp_path);

/* 원자적으로 이름 변경 */
rename(temp_path, final_path);
```

#### 오류 처리

적절한 오류 보고로 문제 진단을 쉽게 합니다.

```c
ereport(WARNING,
        (errcode_for_file_access(),
         errmsg("could not archive \"%s\": %m", file),
         errdetail("Destination: %s", destination)));
```

#### 메모리 관리

장기 상태는 `startup_cb`에서 생성한 별도의 메모리 컨텍스트에 저장하고, `shutdown_cb`에서 적절히 해제합니다.

---

### 51.8. 요약

| 구성 요소 | 설명 |
|-----------|------|
| `_PG_archive_module_init` | 필수 초기화 함수, 콜백 구조체 반환 |
| `startup_cb` | 모듈 로드 시 초기화 (선택) |
| `check_configured_cb` | 구성 유효성 확인 (선택) |
| `archive_file_cb` | WAL 파일 아카이빙 (필수) |
| `shutdown_cb` | 종료 시 정리 (선택) |

아카이브 모듈은 `archive_command`보다 더 강력하고 효율적인 WAL 아카이빙 방법을 제공합니다. 상태 관리, 상세한 오류 보고, 그리고 더 나은 성능을 통해 프로덕션 환경에서 안정적인 연속 아카이빙을 구현할 수 있습니다.

---

### 참고 자료

- [PostgreSQL 공식 문서 - Archive Modules](https://www.postgresql.org/docs/current/archive-modules.html)
- [PostgreSQL 공식 문서 - Continuous Archiving and PITR](https://www.postgresql.org/docs/current/continuous-archiving.html)
- PostgreSQL 소스 코드: `contrib/basic_archive`
