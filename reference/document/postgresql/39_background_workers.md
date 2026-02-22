# 백그라운드 워커 프로세스 (Background Worker Processes)

## 목차
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

## 개요

PostgreSQL은 사용자가 제공한 코드를 별도의 프로세스에서 실행할 수 있도록 확장할 수 있습니다. 이러한 프로세스를 백그라운드 워커(Background Worker) 라고 합니다.

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

## 보안 경고

> 주의: 백그라운드 워커 프로세스는 C 언어를 통해 데이터에 무제한으로 접근할 수 있기 때문에 상당한 견고성(robustness) 및 보안 위험을 초래합니다. 신중하게 감사(audit)된 모듈만 백그라운드 워커 프로세스를 실행하도록 허용해야 합니다.

---

## 백그라운드 워커 등록

백그라운드 워커를 등록하는 방법은 두 가지가 있습니다.

### 정적 등록 (Static Registration)

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

### 동적 등록 (Dynamic Registration)

시스템 시작 후에 백그라운드 워커를 등록합니다.

```c
bool RegisterDynamicBackgroundWorker(BackgroundWorker *worker,
                                     BackgroundWorkerHandle handle)
```

동적 등록은 일반 백엔드(regular backend) 또는 다른 백그라운드 워커에서 호출해야 합니다. postmaster에서 직접 호출할 수 없습니다.

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

## BackgroundWorker 구조체

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

### 필드 설명

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

## 플래그 옵션 (bgw_flags)

`bgw_flags` 필드는 백그라운드 워커가 요청하는 기능을 지정합니다.

```c
/* 공유 메모리 접근 요청 - 필수 */
BGWORKER_SHMEM_ACCESS

/* 데이터베이스 연결 기능 요청 */
BGWORKER_BACKEND_DATABASE_CONNECTION
```

### 플래그 조합 규칙

| 플래그 조합 | 설명 |
|------------|------|
| `BGWORKER_SHMEM_ACCESS` | 공유 메모리에만 접근 (필수) |
| `BGWORKER_SHMEM_ACCESS \| BGWORKER_BACKEND_DATABASE_CONNECTION` | 공유 메모리 접근 + 데이터베이스 연결 |

> 중요: `BGWORKER_BACKEND_DATABASE_CONNECTION`을 사용하려면 반드시 `BGWORKER_SHMEM_ACCESS`도 함께 설정해야 합니다. 그렇지 않으면 시작이 실패합니다.

---

## 시작 시점 옵션 (bgw_start_time)

`bgw_start_time`은 백그라운드 워커가 시작되어야 하는 서버 상태를 지정합니다.

| 옵션 | 설명 |
|------|------|
| `BgWorkerStart_PostmasterStart` | `postgres` 초기화 직후 시작. 데이터베이스 연결 불가 |
| `BgWorkerStart_ConsistentState` | 핫 스탠바이(hot standby)에서 일관된 상태 도달 후 시작. 읽기 전용 쿼리 가능 |
| `BgWorkerStart_RecoveryFinished` | 정상적인 읽기-쓰기 상태에서 시작 |

> 참고: 핫 스탠바이가 아닌 서버에서는 `BgWorkerStart_ConsistentState`와 `BgWorkerStart_RecoveryFinished`가 동일하게 동작합니다.

---

## 데이터베이스 연결

백그라운드 워커가 데이터베이스에 연결하려면 다음 함수 중 하나를 사용합니다.

### 이름으로 연결

```c
void BackgroundWorkerInitializeConnection(char *dbname,
                                          char *username,
                                          uint32 flags)
```

### OID로 연결

```c
void BackgroundWorkerInitializeConnectionByOid(Oid dboid,
                                               Oid useroid,
                                               uint32 flags)
```

### 연결 매개변수

| 매개변수 | NULL/InvalidOid 사용 시 |
|----------|------------------------|
| `dbname` / `dboid` | 특정 데이터베이스에 연결하지 않음 (공유 카탈로그만 접근 가능) |
| `username` / `useroid` | `initdb` 시 생성된 슈퍼유저로 실행 |

### 연결 플래그

| 플래그 | 설명 |
|--------|------|
| `BGWORKER_BYPASS_ALLOWCONN` | 데이터베이스의 `allowconn` 제한 우회 |
| `BGWORKER_BYPASS_ROLELOGINCHECK` | 역할(role)의 로그인 검사 우회 |

### 연결 예제

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

## 시그널 처리 (Signal Handling)

백그라운드 워커에서 시그널은 초기에 차단되어 있습니다. 명시적으로 차단을 해제해야 합니다.

```c
/* 시그널 차단 해제 */
void BackgroundWorkerUnblockSignals(void)

/* 시그널 차단 */
void BackgroundWorkerBlockSignals(void)
```

### 시그널 처리 예제

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

## 프로세스 라이프사이클 (Lifecycle)

### 자동 등록 해제 조건

백그라운드 워커는 다음 조건에서 자동으로 등록 해제됩니다:

1. `bgw_restart_time`이 `BGW_NEVER_RESTART`로 설정됨
2. 종료 코드가 0
3. `TerminateBackgroundWorker`에 의해 종료됨

### 자동 재시작

- 워커가 크래시되면 `bgw_restart_time` 초 후에 자동으로 재시작됩니다
- postmaster가 재초기화되면 즉시 재시작됩니다

### 일시 중지

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

## 동적 워커 관리 함수

동적으로 등록된 백그라운드 워커를 관리하기 위한 함수들입니다.

### 워커 상태 조회

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

### 워커 종료

```c
void TerminateBackgroundWorker(BackgroundWorkerHandle *handle)
```

- 워커가 실행 중이면 `SIGTERM`을 전송합니다
- 실행 중이지 않으면 등록을 해제합니다

### 시작 대기

```c
BgwHandleStatus WaitForBackgroundWorkerStartup(BackgroundWorkerHandle *handle,
                                               pid_t *pidp)
```

postmaster가 워커를 시작하려고 시도하거나 postmaster가 종료될 때까지 차단됩니다.

반환 값:

| 상태 | 설명 |
|------|------|
| `BGWH_STARTED` | 워커 실행 중 (PID가 주소에 기록됨) |
| `BGWH_STOPPED` | 시작되지 않음 |
| `BGWH_POSTMASTER_DIED` | postmaster가 종료됨 |

### 종료 대기

```c
BgwHandleStatus WaitForBackgroundWorkerShutdown(BackgroundWorkerHandle *handle)
```

백그라운드 워커가 종료되거나 postmaster가 종료될 때까지 차단됩니다.

반환 값:

| 상태 | 설명 |
|------|------|
| `BGWH_STOPPED` | 워커가 종료됨 |
| `BGWH_POSTMASTER_DIED` | postmaster가 종료됨 |

### 사용 예제

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

## 비동기 알림 (Asynchronous Notifications)

백그라운드 워커는 다음 방법으로 알림을 전송할 수 있습니다:

### SPI를 통한 NOTIFY

```c
SPI_connect();
SPI_execute("NOTIFY my_channel, 'payload'", false, 0);
SPI_finish();
```

### 직접 Async_Notify 호출

```c
#include "commands/async.h"

Async_Notify("my_channel", "payload");
```

> 제한사항: 백그라운드 워커는 `LISTEN` 명령을 사용해서는 안 됩니다. 알림을 소비하기 위한 인프라가 없습니다.

---

## 공유 메모리 접근

백그라운드 워커가 공유 메모리에 접근하려면 `bgw_flags`에 `BGWORKER_SHMEM_ACCESS`를 설정해야 합니다.

### 공유 메모리 구조체 정의

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

### 공유 메모리 초기화 (모듈 로드 시)

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

### 공유 메모리 사용 예제

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

## 예제 코드

### 완전한 백그라운드 워커 예제

다음은 주기적으로 테이블의 레코드 수를 로그에 기록하는 완전한 백그라운드 워커 예제입니다.

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

### Makefile

```makefile
MODULES = my_worker

PG_CONFIG = pg_config
PGXS := $(shell $(PG_CONFIG) --pgxs)
include $(PGXS)
```

### 설치 및 사용

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

## 참고사항

### 전역 변수

- `MyBgworkerEntry`: 등록 시의 `BackgroundWorker` 구조체 복사본을 가리키는 전역 변수

### 최대 워커 수 제한

- `max_worker_processes` 설정 매개변수로 최대 백그라운드 워커 수가 제한됩니다

### Windows/EXEC_BACKEND 주의사항

Windows 및 `EXEC_BACKEND` 환경에서는 `Datum`을 참조(reference)로 전달하는 것이 안전하지 않습니다. 값(value)으로만 전달해야 합니다.

- `int32` 또는 작은 값 사용
- 또는 공유 메모리 배열의 인덱스 사용

```c
/* 안전한 방법 */
worker.bgw_main_arg = Int32GetDatum(42);

/* 또는 공유 메모리 인덱스 사용 */
worker.bgw_main_arg = Int32GetDatum(shared_array_index);
```

### 참고 예제

PostgreSQL 소스 코드의 `src/test/modules/worker_spi`에서 유용한 기법을 보여주는 작동 예제를 확인할 수 있습니다.

---

## 관련 문서

- [서버 프로그래밍 (Server Programming)](https://www.postgresql.org/docs/current/server-programming.html)
- [SPI (Server Programming Interface)](https://www.postgresql.org/docs/current/spi.html)
- [논리적 디코딩 (Logical Decoding)](https://www.postgresql.org/docs/current/logicaldecoding.html)
- [확장 모듈 작성 (Writing Extensions)](https://www.postgresql.org/docs/current/extend-extensions.html)
