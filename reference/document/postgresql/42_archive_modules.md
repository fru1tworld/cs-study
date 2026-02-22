# Chapter 51. 아카이브 모듈 (Archive Modules)

PostgreSQL은 연속 아카이빙(Continuous Archiving)을 위한 커스텀 모듈을 생성할 수 있는 인프라를 제공합니다. 아카이브 모듈은 셸 명령 기반 아카이빙(`archive_command` 사용)보다 더 강력하고 성능이 뛰어난 대안을 제공합니다.

## 51.1. 아카이브 모듈 개요 (Overview)

커스텀 `archive_library`가 구성되면, PostgreSQL은 완료된 WAL(Write-Ahead Log) 파일을 모듈에 제출합니다. 서버는 모듈이 성공적으로 아카이빙되었음을 확인할 때까지 WAL 파일을 재사용하거나 제거하지 않습니다. 모듈은 각 WAL 파일에 대해 무엇을 할지 유연하게 결정할 수 있습니다.

### 아카이브 모듈의 장점

| 특징 | archive_command | archive_library |
|------|-----------------|-----------------|
| 성능 | 매 파일마다 셸 프로세스 생성 | 로드된 라이브러리 직접 호출 |
| 유연성 | 셸 명령으로 제한 | C 코드로 완전한 제어 |
| 오류 처리 | 종료 코드 기반 | 상세한 오류 보고 가능 |
| 상태 관리 | 파일/외부 저장소 필요 | 메모리 내 상태 유지 가능 |

### 아카이브 모듈 구성 요소

아카이브 모듈은 다음 구성 요소를 포함해야 합니다:

1. 초기화 함수 (Initialization Function) - 모듈의 필수 진입점
2. 콜백 함수들 (Callbacks) - 다양한 아카이빙 단계를 처리하는 함수들

또한 아카이브 모듈은 다음과 같은 추가 기능을 수행할 수 있습니다:
- 커스텀 GUC(Grand Unified Configuration) 매개변수 선언
- 백그라운드 워커 등록
- 기본 아카이빙 이상의 커스텀 로직 구현

---

## 51.2. 초기화 함수 (Initialization Functions)

아카이브 라이브러리는 `archive_library` 구성 매개변수를 사용하여 공유 라이브러리를 동적으로 로드함으로써 로드됩니다. 라이브러리는 자신이 유효한 아카이브 모듈임을 나타내는 특정 초기화 함수를 제공해야 합니다.

### 필수 초기화 함수

```c
typedef const ArchiveModuleCallbacks *(*ArchiveModuleInit) (void);
```

함수 이름: `_PG_archive_module_init`

이 초기화 함수는 다음 조건을 충족해야 합니다:

- 매개변수를 받지 않음
- `ArchiveModuleCallbacks` 구조체에 대한 포인터를 반환
- 서버 수명 동안 유효한 값을 반환 (일반적으로 전역 범위에서 `static const`로 정의)

### ArchiveModuleCallbacks 구조체

```c
typedef struct ArchiveModuleCallbacks
{
    ArchiveStartupCB startup_cb;           /* 시작 콜백 (선택) */
    ArchiveCheckConfiguredCB check_configured_cb;  /* 구성 확인 콜백 (선택) */
    ArchiveFileCB archive_file_cb;         /* 파일 아카이브 콜백 (필수) */
    ArchiveShutdownCB shutdown_cb;         /* 종료 콜백 (선택) */
} ArchiveModuleCallbacks;
```

### 콜백 요구사항

| 콜백 | 필수 여부 | 설명 |
|------|----------|------|
| `startup_cb` | 선택 | 모듈 초기화 |
| `check_configured_cb` | 선택 | 구성 유효성 확인 |
| `archive_file_cb` | 필수 | WAL 파일 아카이빙 |
| `shutdown_cb` | 선택 | 정리 및 종료 |

### 초기화 함수 예제

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

## 51.3. 아카이브 모듈 콜백 (Archive Module Callbacks)

아카이브 모듈 콜백은 모듈의 실제 아카이빙 동작을 정의합니다. 서버는 각 개별 WAL 파일을 처리하기 위해 필요에 따라 이들을 호출합니다.

### 51.3.1. 시작 콜백 (Startup Callback)

```c
typedef void (*ArchiveStartupCB) (ArchiveModuleState *state);
```

`startup_cb` 콜백은 모듈이 로드된 직후에 호출됩니다. 이 콜백은 필요한 추가 초기화를 수행하는 데 사용할 수 있습니다. 아카이브 모듈에 상태가 있는 경우 `state->private_data`를 사용하여 저장할 수 있습니다.

#### 시작 콜백 예제

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

### 51.3.2. 구성 확인 콜백 (Check Callback)

```c
typedef bool (*ArchiveCheckConfiguredCB) (ArchiveModuleState *state);
```

`check_configured_cb` 콜백은 모듈이 완전히 구성되어 WAL 파일을 수락할 준비가 되었는지 결정합니다 (예: 구성 매개변수가 유효한 값으로 설정되어 있는지 확인).

#### 반환 값

| 반환 값 | 동작 |
|---------|------|
| `true` | 서버가 `archive_file_cb`를 호출하여 아카이빙 진행 |
| `false` | 아카이빙 진행하지 않음; 서버가 경고를 발생시키고 주기적으로 재시도 |

`check_configured_cb`가 정의되지 않은 경우, 서버는 모듈이 항상 구성된 것으로 간주합니다.

> 참고: `false`를 반환할 때 `arch_module_check_errdetail` 매크로를 사용하여 일반 경고 메시지에 추가 정보를 첨부할 수 있습니다.

#### 구성 확인 콜백 예제

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

### 51.3.3. 아카이브 콜백 (Archive Callback)

```c
typedef bool (*ArchiveFileCB) (ArchiveModuleState *state,
                               const char *file,
                               const char *path);
```

`archive_file_cb` 콜백은 단일 WAL 파일을 아카이브합니다. 이것은 필수 콜백입니다.

#### 매개변수

| 매개변수 | 설명 |
|----------|------|
| `state` | 아카이브 모듈 상태 |
| `file` | 아카이브할 WAL 파일의 파일 이름만 (예: `000000010000000000000001`) |
| `path` | WAL 파일의 전체 경로 (파일 이름 포함, 예: `/var/lib/postgresql/data/pg_wal/000000010000000000000001`) |

#### 반환 값

| 반환 값 | 동작 |
|---------|------|
| `true` | 파일이 성공적으로 아카이브됨; 서버가 원본 WAL 파일을 재사용하거나 제거할 수 있음 |
| `false` 또는 오류 발생 | 서버가 원본 WAL 파일을 유지하고 나중에 재시도 |

> 참고: 이 콜백은 호출 사이에 재설정되는 단기 메모리 컨텍스트에서 실행됩니다. 더 긴 수명의 저장소가 필요한 경우 `startup_cb` 콜백에서 메모리 컨텍스트를 생성하세요.

#### 아카이브 콜백 예제

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

### 51.3.4. 종료 콜백 (Shutdown Callback)

```c
typedef void (*ArchiveShutdownCB) (ArchiveModuleState *state);
```

`shutdown_cb` 콜백은 아카이버 프로세스가 종료될 때 (예: 오류 발생 후) 또는 `archive_library` 값이 변경될 때 호출됩니다.

`shutdown_cb`가 정의되지 않은 경우 특별한 동작이 수행되지 않습니다. 아카이브 모듈에 상태가 있는 경우, 이 콜백에서 메모리 누수를 방지하기 위해 해당 상태를 해제해야 합니다.

#### 종료 콜백 예제

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

## 51.4. 완전한 아카이브 모듈 예제

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

## 51.5. 아카이브 모듈 구성 (Configuration)

아카이브 모듈을 사용하려면 `postgresql.conf`에서 다음과 같이 구성합니다:

```ini
# 아카이브 모드 활성화
archive_mode = on

# archive_command 대신 archive_library 사용
archive_library = 'simple_archive'

# 모듈별 설정 (모듈에 따라 다름)
simple_archive.directory = '/var/lib/postgresql/archive'
```

### 구성 매개변수

| 매개변수 | 설명 |
|----------|------|
| `archive_mode` | `on`으로 설정하여 아카이빙 활성화 |
| `archive_library` | 사용할 아카이브 모듈 라이브러리 이름 |
| `archive_command` | `archive_library`가 설정되면 무시됨 |

> 참고: `archive_library`와 `archive_command`를 동시에 설정하면 `archive_library`가 우선합니다.

### 라이브러리 검색 경로

PostgreSQL은 다음 순서로 아카이브 라이브러리를 검색합니다:

1. `dynamic_library_path`에 지정된 디렉토리
2. PostgreSQL 설치 디렉토리의 `lib` 하위 디렉토리

---

## 51.6. 기본 제공 예제: basic_archive

PostgreSQL 소스 코드의 `contrib/basic_archive` 모듈은 유용한 구현 기술을 보여주는 작동하는 예제를 제공합니다.

### basic_archive 설치

```bash
# PostgreSQL 소스 디렉토리에서
cd contrib/basic_archive
make
make install
```

### basic_archive 사용

```ini
# postgresql.conf
archive_mode = on
archive_library = 'basic_archive'
basic_archive.archive_directory = '/path/to/archive'
```

---

## 51.7. 모범 사례 (Best Practices)

### 멱등성 (Idempotency)

아카이브 콜백은 멱등적이어야 합니다. 동일한 WAL 파일에 대해 여러 번 호출되더라도 올바르게 동작해야 합니다.

```c
/* 이미 아카이브된 파일 확인 */
if (file_already_archived(destination, source_size))
    return true;  /* 이미 완료됨 */
```

### 원자적 쓰기 (Atomic Writes)

임시 파일에 쓴 후 이름 변경을 사용하여 원자적 쓰기를 보장합니다.

```c
/* 임시 파일에 쓰기 */
write_to_temp_file(temp_path, data);

/* fsync 호출 */
fsync(temp_path);

/* 원자적으로 이름 변경 */
rename(temp_path, final_path);
```

### 오류 처리

적절한 오류 보고를 통해 문제 진단을 용이하게 합니다.

```c
ereport(WARNING,
        (errcode_for_file_access(),
         errmsg("could not archive \"%s\": %m", file),
         errdetail("Destination: %s", destination)));
```

### 메모리 관리

장기 상태는 `startup_cb`에서 생성한 별도의 메모리 컨텍스트에 저장하고, `shutdown_cb`에서 적절히 해제합니다.

---

## 51.8. 요약

| 구성 요소 | 설명 |
|-----------|------|
| `_PG_archive_module_init` | 필수 초기화 함수, 콜백 구조체 반환 |
| `startup_cb` | 모듈 로드 시 초기화 (선택) |
| `check_configured_cb` | 구성 유효성 확인 (선택) |
| `archive_file_cb` | WAL 파일 아카이빙 (필수) |
| `shutdown_cb` | 종료 시 정리 (선택) |

아카이브 모듈은 `archive_command`보다 더 강력하고 효율적인 WAL 아카이빙 방법을 제공합니다. 상태 관리, 상세한 오류 보고, 그리고 더 나은 성능을 통해 프로덕션 환경에서 안정적인 연속 아카이빙을 구현할 수 있습니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - Archive Modules](https://www.postgresql.org/docs/current/archive-modules.html)
- [PostgreSQL 공식 문서 - Continuous Archiving and PITR](https://www.postgresql.org/docs/current/continuous-archiving.html)
- PostgreSQL 소스 코드: `contrib/basic_archive`
