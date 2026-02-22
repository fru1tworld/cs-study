# Chapter 65: Generic WAL Records (일반 WAL 레코드)

## 목차
1. [개요](#개요)
2. [API 함수](#api-함수)
3. [사용 절차](#사용-절차)
4. [주요 제약 사항 및 동작](#주요-제약-사항-및-동작)
5. [사용 예제](#사용-예제)
6. [추가 고려 사항](#추가-고려-사항)

---

## 개요

Generic WAL Records(일반 WAL 레코드)는 PostgreSQL 확장(Extension)이 커스텀 WAL 리소스 매니저(Custom WAL Resource Manager)를 구현하지 않고도 WAL에 로깅되는 데이터 업데이트를 수행할 수 있도록 하는 내장 메커니즘입니다.

이 기능은 페이지(Page)의 변경 사항을 일반적인 방식으로 기술하여, 확장 개발자가 복잡한 WAL 인프라를 직접 구현할 필요 없이 데이터 무결성과 복구 가능성을 보장받을 수 있게 합니다.

### 중요 사항

> 주의: Generic WAL 레코드는 논리적 디코딩(Logical Decoding) 과정에서 무시됩니다. 만약 논리적 디코딩이 필요한 경우에는 대신 커스텀 WAL 리소스 매니저(Custom WAL Resource Manager) 를 사용해야 합니다.

### API 위치

| 구분 | 경로 |
|------|------|
| 헤더 파일 | `access/generic_xlog.h` |
| 구현 파일 | `access/transam/generic_xlog.c` |

---

## API 함수

Generic WAL을 사용하기 위한 주요 API 함수들은 다음과 같습니다.

### GenericXLogStart

```c
GenericXLogState *GenericXLogStart(Relation relation)
```

설명: 주어진 릴레이션(Relation)에 대한 Generic WAL 레코드 생성을 시작합니다.

매개변수:
- `relation`: WAL 레코드를 생성할 대상 릴레이션

반환값: Generic WAL 상태 객체에 대한 포인터

---

### GenericXLogRegisterBuffer

```c
Page GenericXLogRegisterBuffer(GenericXLogState *state, Buffer buffer, int flags)
```

설명: 수정할 버퍼를 등록하고, 해당 버퍼 페이지의 임시 복사본에 대한 포인터를 반환합니다.

매개변수:
- `state`: `GenericXLogStart()`에서 반환된 상태 객체
- `buffer`: 수정할 버퍼
- `flags`: 동작 플래그

반환값: 버퍼 페이지의 임시 복사본에 대한 포인터

플래그 옵션:

| 플래그 | 설명 |
|--------|------|
| `GENERIC_XLOG_FULL_IMAGE` | 델타(Delta) 업데이트 대신 전체 페이지 이미지(Full-Page Image)를 포함합니다. 새로 생성되거나 완전히 재작성된 페이지에 사용합니다. |

> 중요: 반드시 이 함수가 반환한 복사본을 수정해야 합니다. 원본 버퍼를 직접 수정해서는 안 됩니다!

---

### GenericXLogFinish

```c
XLogRecPtr GenericXLogFinish(GenericXLogState *state)
```

설명: 실제 버퍼에 변경 사항을 적용하고 Generic WAL 레코드를 발행(Emit)합니다.

매개변수:
- `state`: Generic WAL 상태 객체

반환값: 발행된 WAL 레코드의 LSN(Log Sequence Number)

동작:
- 버퍼를 자동으로 dirty로 표시
- LSN을 자동으로 설정
- 별도의 명시적 호출 불필요

---

### GenericXLogAbort

```c
void GenericXLogAbort(GenericXLogState *state)
```

설명: 페이지 이미지 복사본에 대한 모든 변경 사항을 폐기합니다.

매개변수:
- `state`: Generic WAL 상태 객체

사용 시점: `GenericXLogStart()` 이후 어떤 단계에서든 호출 가능

---

## 사용 절차

Generic WAL 레코드를 사용하여 WAL에 로깅되는 데이터 업데이트를 수행하는 절차는 다음과 같습니다.

### 1단계: 레코드 생성 시작

```c
GenericXLogState *state;
state = GenericXLogStart(relation);
```

지정된 릴레이션에 대한 Generic WAL 레코드 생성을 시작합니다.

### 2단계: 버퍼 등록

```c
Page page;
page = GenericXLogRegisterBuffer(state, buffer, flags);
```

수정할 버퍼를 등록합니다. 이 함수는 다중 페이지 수정이 필요한 경우 여러 번 호출할 수 있습니다.

### 3단계: 페이지 수정

`GenericXLogRegisterBuffer()`에서 얻은 페이지 이미지에 원하는 수정을 적용합니다.

```c
// 페이지 복사본에 대한 수정 작업 수행
// 예: 아이템 추가, 데이터 변경 등
```

### 4단계: 완료 및 발행

```c
GenericXLogFinish(state);
```

변경 사항을 실제 버퍼에 적용하고 Generic WAL 레코드를 발행합니다.

### 취소 시

오류 발생 또는 작업 취소가 필요한 경우:

```c
GenericXLogAbort(state);
```

---

## 주요 제약 사항 및 동작

### 버퍼 수정 규칙

| 규칙 | 설명 |
|------|------|
| 직접 수정 금지 | 모든 변경은 `GenericXLogRegisterBuffer()`에서 반환된 복사본을 통해서만 이루어져야 합니다. `BufferGetPage()`를 직접 호출하여 수정하면 안 됩니다. |
| 락 관리 | 호출자가 버퍼의 pin/unpin 및 lock/unlock을 직접 관리해야 합니다. |
| 배타적 락 필요 | `GenericXLogRegisterBuffer()` 호출 전부터 `GenericXLogFinish()` 호출 후까지 배타적 락(Exclusive Lock)을 유지해야 합니다. |

### 단계 순서

- 2단계(버퍼 등록)와 3단계(페이지 수정)는 어떤 순서로든 자유롭게 혼합할 수 있습니다.
- 리플레이(Replay) 시 락이 획득될 순서대로 버퍼를 등록해야 합니다.

### 버퍼 제한

- 하나의 레코드에 등록할 수 있는 최대 버퍼 수는 `MAX_GENERIC_XLOG_PAGES`로 제한됩니다.
- 이 한도를 초과하면 오류가 발생합니다.

### 페이지 레이아웃

Generic WAL은 표준 페이지 레이아웃(Standard Page Layout)을 가정합니다. `pd_lower`와 `pd_upper` 사이에는 유용한 데이터가 없다고 간주합니다.

### 크리티컬 섹션 (Critical Section)

- `GenericXLogStart()`와 `GenericXLogFinish()` 사이에는 크리티컬 섹션이 없습니다.
- 따라서 이 구간에서 메모리 할당이나 오류 발생(Error Throwing)이 안전합니다.
- 유일한 크리티컬 섹션은 `GenericXLogFinish()` 내부에만 존재합니다.

### 버퍼 관리

`GenericXLogFinish()`가 자동으로 처리하는 작업:
- 버퍼를 dirty로 표시
- LSN 설정

별도의 명시적 호출이 필요하지 않습니다.

### 언로그드 릴레이션 (Unlogged Relations)

- 언로그드 릴레이션에서도 동일하게 작동합니다.
- 단, WAL 레코드가 발행되지 않습니다.
- 특별한 검사가 필요하지 않습니다.

### 리두 동작 (Redo Behavior)

- Generic WAL 리두(Redo)는 등록 순서대로 배타적 락을 획득합니다.
- 이후 동일한 순서로 락을 해제합니다.

### 델타 인코딩 (Delta Encoding)

- `GENERIC_XLOG_FULL_IMAGE` 플래그 없이 사용하면, 레코드에는 바이트 단위 델타(Byte-by-Byte Delta)가 포함됩니다.
- 페이지 내 데이터 이동에는 최적화되어 있지 않습니다.

---

## 사용 예제

### 기본 사용 예제

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

### 새 페이지 초기화 예제

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

### 다중 페이지 수정 예제

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

### 오류 처리 예제

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

### 조건부 업데이트 예제

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

## 추가 고려 사항

### Generic WAL vs Custom WAL Resource Manager

| 특성 | Generic WAL | Custom WAL Resource Manager |
|------|-------------|----------------------------|
| 구현 복잡도 | 낮음 | 높음 |
| 논리적 디코딩 지원 | 지원 안 함 | 지원 |
| 유연성 | 제한적 | 높음 |
| 페이지 레이아웃 | 표준 레이아웃 필요 | 자유로움 |
| 적합한 용도 | 간단한 확장 | 복잡한 스토리지 엔진 |

### 성능 고려 사항

1. 전체 페이지 이미지 (Full-Page Image)
   - `GENERIC_XLOG_FULL_IMAGE` 플래그 사용 시 WAL 크기가 증가합니다.
   - 새 페이지나 완전히 재작성된 페이지에만 사용하세요.

2. 델타 인코딩 한계
   - 바이트 단위 델타는 페이지 내 데이터 이동에 비효율적입니다.
   - 대량의 데이터 재배치 시 전체 페이지 이미지가 더 효율적일 수 있습니다.

3. 버퍼 수 제한
   - `MAX_GENERIC_XLOG_PAGES` 제한을 고려하여 설계하세요.
   - 대규모 트랜잭션은 여러 Generic WAL 레코드로 분할해야 할 수 있습니다.

### 디버깅 팁

- WAL 레코드 내용 확인: `pg_waldump` 유틸리티 사용
- 레코드 타입: `XLOG_GENERIC`로 표시됨
- 복구 테스트: 크래시 복구 시나리오를 반드시 테스트하세요

---

## 참고 자료

- [PostgreSQL 공식 문서 - Generic WAL Records](https://www.postgresql.org/docs/current/generic-wal.html)
- [PostgreSQL 공식 문서 - Custom WAL Resource Managers](https://www.postgresql.org/docs/current/custom-rmgr.html)
- [PostgreSQL 소스 코드 - generic_xlog.h](https://github.com/postgres/postgres/blob/master/src/include/access/generic_xlog.h)
- [PostgreSQL 소스 코드 - generic_xlog.c](https://github.com/postgres/postgres/blob/master/src/backend/access/transam/generic_xlog.c)
