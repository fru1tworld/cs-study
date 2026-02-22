# Chapter 35: Large Objects (대용량 객체)

PostgreSQL은 사용자 데이터에 대한 스트림 방식의 접근을 제공하는 대용량 객체(Large Object) 기능을 지원합니다. 스트림 방식 접근은 전체 데이터 블록을 한 번에 처리하기 어려운 경우에 유용합니다.

## 목차

1. [소개 (Introduction)](#1-소개-introduction)
2. [구현 기능 (Implementation Features)](#2-구현-기능-implementation-features)
3. [클라이언트 인터페이스 (Client Interfaces)](#3-클라이언트-인터페이스-client-interfaces)
4. [서버측 함수 (Server-Side Functions)](#4-서버측-함수-server-side-functions)
5. [예제 프로그램 (Example Program)](#5-예제-프로그램-example-program)

---

## 1. 소개 (Introduction)

### 대용량 객체란?

PostgreSQL의 대용량 객체(Large Object) 기능은 특별히 큰 데이터 값을 저장하고 조작하기 위한 기능입니다.

- 모든 대용량 객체는 `pg_largeobject` 시스템 테이블에 저장됩니다.
- 각 대용량 객체는 `pg_largeobject_metadata` 시스템 테이블에 메타데이터 항목을 가집니다.
- 파일의 표준 작업(읽기, 쓰기, 탐색 등)과 유사한 읽기/쓰기 API를 사용하여 생성, 수정, 삭제할 수 있습니다.

### Large Objects vs TOAST 비교

| 특성 | Large Objects | TOAST |
|------|---------------|-------|
| 최대 크기 | 4 TB | 1 GB |
| 부분 읽기/업데이트 | 효율적으로 지원 | 전체 값을 읽고 써야 함 |
| 접근 방식 | 스트림 기반 API | 일반 SQL 컬럼 |
| 저장 위치 | pg_largeobject 테이블 | 자동으로 보조 저장 영역 |

> 참고: TOAST(The Oversized-Attribute Storage Technique) 저장 시스템의 도입으로 대용량 객체 기능이 부분적으로 구식화되었습니다. 하지만 매우 큰 데이터(1GB 초과)나 효율적인 부분 업데이트가 필요한 경우에는 여전히 대용량 객체가 유용합니다.

---

## 2. 구현 기능 (Implementation Features)

### 기본 구조

대용량 객체는 "청크(chunks)"로 분할되어 데이터베이스의 행(rows)에 저장됩니다. B-tree 인덱스를 통해 임의 접근(random access) 읽기/쓰기 시 빠른 청크 검색이 보장됩니다.

### 희소 할당 (Sparse Allocation)

대용량 객체의 청크는 연속적일 필요가 없습니다. 이는 Unix 파일 시스템의 "희소 할당(sparsely allocated)" 파일 동작과 동일합니다.

예시:
```sql
-- 새 대용량 객체를 열고 오프셋 1000000으로 이동한 후 몇 바이트를 쓰면,
-- 1000000 바이트 전체가 할당되지 않고 실제로 작성된 데이터 범위만 할당됩니다.
-- 읽기 작업에서 할당되지 않은 위치는 0(zero)으로 읽힙니다.
```

### 접근 제어 및 권한 (PostgreSQL 9.0 이상)

대용량 객체는 소유자(owner)와 접근 권한(access permissions)을 가집니다.

| 작업 | 필요한 권한 |
|------|------------|
| 읽기 | `SELECT` 권한 |
| 쓰기/자르기 | `UPDATE` 권한 |
| 삭제/주석 추가/소유자 변경 | 대용량 객체 소유자 또는 데이터베이스 슈퍼유저 |

권한 관리:
```sql
-- 권한 부여
GRANT SELECT ON LARGE OBJECT 12345 TO username;
GRANT UPDATE ON LARGE OBJECT 12345 TO username;

-- 권한 회수
REVOKE SELECT ON LARGE OBJECT 12345 FROM username;
```

> 호환성: `lo_compat_privileges` 런타임 파라미터를 통해 이전 버전과의 호환성을 조정할 수 있습니다.

---

## 3. 클라이언트 인터페이스 (Client Interfaces)

PostgreSQL의 libpq 클라이언트 라이브러리는 대용량 객체 접근 기능을 제공합니다. 이 인터페이스는 Unix 파일 시스템 인터페이스(open, read, write, lseek 등)를 모델링했습니다.

### 중요 사항

- 모든 대용량 객체 조작은 SQL 트랜잭션 블록 내에서 수행되어야 합니다.
- 헤더 파일: `libpq/libpq-fs.h`
- Pipeline 모드에서는 사용할 수 없습니다.

### 3.1 대용량 객체 생성 (Creating a Large Object)

#### lo_create

```c
Oid lo_create(PGconn *conn, Oid lobjId);
```

새로운 대용량 객체를 생성합니다.

매개변수:
- `conn`: 데이터베이스 연결
- `lobjId`: 할당할 OID (InvalidOid=0이면 시스템이 자동 할당)

반환값: 할당된 OID 또는 InvalidOid(0, 실패 시)

예시:
```c
Oid new_oid = lo_create(conn, InvalidOid);  // 자동 OID 할당
Oid specific_oid = lo_create(conn, 12345);  // 특정 OID 요청
```

#### lo_creat (구 함수)

```c
Oid lo_creat(PGconn *conn, int mode);
```

PostgreSQL 8.0 이전 버전과의 호환성을 위한 함수입니다.

매개변수:
- `mode`: `INV_READ`, `INV_WRITE`, 또는 둘 다 (`INV_READ | INV_WRITE`)

### 3.2 대용량 객체 임포트 (Importing a Large Object)

#### lo_import

```c
Oid lo_import(PGconn *conn, const char *filename);
```

운영 체제 파일을 데이터베이스의 대용량 객체로 임포트합니다.

주의: 파일은 클라이언트 측에서 읽습니다(서버가 아님).

예시:
```c
Oid imported_oid = lo_import(conn, "/path/to/local/file.bin");
if (imported_oid == InvalidOid) {
    fprintf(stderr, "Import failed: %s\n", PQerrorMessage(conn));
}
```

#### lo_import_with_oid (PostgreSQL 8.4+)

```c
Oid lo_import_with_oid(PGconn *conn, const char *filename, Oid lobjId);
```

특정 OID를 지정하여 파일을 임포트합니다.

### 3.3 대용량 객체 내보내기 (Exporting a Large Object)

#### lo_export

```c
int lo_export(PGconn *conn, Oid lobjId, const char *filename);
```

대용량 객체를 운영 체제 파일로 내보냅니다.

주의: 파일은 클라이언트 측에 씁니다(서버가 아님).

반환값: 1(성공), -1(실패)

예시:
```c
if (lo_export(conn, lobjId, "/path/to/output/file.bin") < 0) {
    fprintf(stderr, "Export failed: %s\n", PQerrorMessage(conn));
}
```

### 3.4 기존 대용량 객체 열기 (Opening an Existing Large Object)

#### lo_open

```c
int lo_open(PGconn *conn, Oid lobjId, int mode);
```

읽기 또는 쓰기용으로 기존 대용량 객체를 엽니다.

매개변수:
- `mode`: `INV_READ`, `INV_WRITE`, 또는 둘 다

반환값: 대용량 객체 디스크립터(Large Object Descriptor, 음이 아닌 정수) 또는 -1(실패)

예시:
```c
int fd = lo_open(conn, lobjId, INV_READ | INV_WRITE);
if (fd < 0) {
    fprintf(stderr, "Cannot open large object: %s\n", PQerrorMessage(conn));
}
```

읽기/쓰기 동작:
- `INV_READ`: 트랜잭션 스냅샷 시점의 데이터만 읽음 (REPEATABLE READ와 유사)
- `INV_WRITE`: 커밋된 다른 트랜잭션의 쓰기도 반영됨 (READ COMMITTED와 유사)

### 3.5 대용량 객체에 데이터 쓰기 (Writing Data to a Large Object)

#### lo_write

```c
int lo_write(PGconn *conn, int fd, const char *buf, size_t len);
```

대용량 객체 디스크립터에 데이터를 씁니다.

반환값: 실제로 쓴 바이트 수(보통 len과 동일) 또는 -1(실패)

주의: `len`은 `INT_MAX` 이하여야 합니다.

예시:
```c
char data[] = "Hello, Large Object!";
int nbytes = lo_write(conn, fd, data, strlen(data));
if (nbytes < 0) {
    fprintf(stderr, "Write failed: %s\n", PQerrorMessage(conn));
}
```

### 3.6 대용량 객체에서 데이터 읽기 (Reading Data from a Large Object)

#### lo_read

```c
int lo_read(PGconn *conn, int fd, char *buf, size_t len);
```

대용량 객체 디스크립터에서 데이터를 읽습니다.

반환값: 실제로 읽은 바이트 수(EOF 시 len보다 작을 수 있음) 또는 -1(실패)

예시:
```c
char buf[1024];
int nbytes = lo_read(conn, fd, buf, sizeof(buf));
if (nbytes > 0) {
    // 데이터 처리
    buf[nbytes] = '\0';  // null 종료
    printf("Read: %s\n", buf);
}
```

### 3.7 대용량 객체 내에서 탐색 (Seeking in a Large Object)

#### lo_lseek

```c
int lo_lseek(PGconn *conn, int fd, int offset, int whence);
```

읽기/쓰기 위치를 변경합니다.

매개변수:
- `whence`: `SEEK_SET`(시작), `SEEK_CUR`(현재 위치), `SEEK_END`(끝)

반환값: 새 위치 또는 -1(실패)

제한: 2GB 이상의 위치를 처리할 수 없습니다.

예시:
```c
// 시작 위치로 이동
lo_lseek(conn, fd, 0, SEEK_SET);

// 현재 위치에서 100바이트 앞으로
lo_lseek(conn, fd, 100, SEEK_CUR);

// 끝에서 50바이트 앞으로
lo_lseek(conn, fd, -50, SEEK_END);
```

#### lo_lseek64 (PostgreSQL 9.3+)

```c
int64_t lo_lseek64(PGconn *conn, int fd, int64_t offset, int whence);
```

2GB 이상의 대용량 객체를 처리할 수 있는 64비트 버전입니다.

### 3.8 현재 위치 조회 (Obtaining the Seek Position)

#### lo_tell

```c
int lo_tell(PGconn *conn, int fd);
```

반환값: 현재 위치 또는 -1(실패)

#### lo_tell64 (PostgreSQL 9.3+)

```c
int64_t lo_tell64(PGconn *conn, int fd);
```

2GB 이상의 위치를 처리할 수 있는 64비트 버전입니다.

### 3.9 대용량 객체 자르기 (Truncating a Large Object)

#### lo_truncate

```c
int lo_truncate(PGconn *conn, int fd, size_t len);
```

대용량 객체를 지정된 길이로 자르거나 확장합니다.

동작:
- `len`이 현재 크기보다 작으면: 해당 길이로 자름
- `len`이 현재 크기보다 크면: null 바이트(0)로 확장

반환값: 0(성공) 또는 -1(실패)

주의: 읽기/쓰기 위치는 변경되지 않습니다.

예시:
```c
// 대용량 객체를 1024 바이트로 자르기
if (lo_truncate(conn, fd, 1024) < 0) {
    fprintf(stderr, "Truncate failed: %s\n", PQerrorMessage(conn));
}
```

#### lo_truncate64 (PostgreSQL 9.3+)

```c
int lo_truncate64(PGconn *conn, int fd, int64_t len);
```

2GB 이상의 크기를 처리할 수 있는 64비트 버전입니다.

### 3.10 대용량 객체 디스크립터 닫기 (Closing a Large Object Descriptor)

#### lo_close

```c
int lo_close(PGconn *conn, int fd);
```

반환값: 0(성공) 또는 -1(실패)

주의: 트랜잭션 종료 시 열린 디스크립터는 자동으로 닫힙니다.

예시:
```c
if (lo_close(conn, fd) < 0) {
    fprintf(stderr, "Close failed: %s\n", PQerrorMessage(conn));
}
```

### 3.11 대용량 객체 삭제 (Removing a Large Object)

#### lo_unlink

```c
int lo_unlink(PGconn *conn, Oid lobjId);
```

데이터베이스에서 대용량 객체를 제거합니다.

반환값: 1(성공) 또는 -1(실패)

예시:
```c
if (lo_unlink(conn, lobjId) < 0) {
    fprintf(stderr, "Delete failed: %s\n", PQerrorMessage(conn));
}
```

### 오류 처리

오류 발생 시 함수는 불가능한 값(보통 0 또는 -1)을 반환합니다. 오류 메시지는 연결 객체에 저장되며, `PQerrorMessage()` 함수를 사용하여 조회할 수 있습니다.

```c
if (result < 0) {
    fprintf(stderr, "Error: %s\n", PQerrorMessage(conn));
}
```

---

## 4. 서버측 함수 (Server-Side Functions)

SQL 쿼리에서 직접 사용할 수 있는 서버측 함수들입니다.

### 4.1 SQL 지향 Large Object 함수

#### lo_from_bytea - bytea 데이터로 대용량 객체 생성

```sql
lo_from_bytea(loid oid, data bytea) -> oid
```

대용량 객체를 생성하고 데이터를 저장합니다.

매개변수:
- `loid`: 0이면 시스템이 자동 할당, 아니면 해당 OID 사용
- `data`: 저장할 바이트 데이터

예시:
```sql
-- 새 대용량 객체 생성 (OID 자동 할당)
SELECT lo_from_bytea(0, '\xffffff00'::bytea);
-- 결과: 24528

-- 특정 OID로 생성
SELECT lo_from_bytea(99999, 'Hello World'::bytea);
```

#### lo_put - 대용량 객체에 데이터 쓰기

```sql
lo_put(loid oid, offset bigint, data bytea) -> void
```

지정된 오프셋부터 데이터를 씁니다. 필요시 대용량 객체가 자동으로 확대됩니다.

예시:
```sql
-- 오프셋 1부터 데이터 쓰기
SELECT lo_put(24528, 1, '\xaa'::bytea);

-- 오프셋 100부터 텍스트 데이터 쓰기
SELECT lo_put(24528, 100, 'New data'::bytea);
```

#### lo_get - 대용량 객체에서 데이터 읽기

```sql
lo_get(loid oid [, offset bigint, length integer]) -> bytea
```

대용량 객체의 전체 또는 일부 내용을 추출합니다.

예시:
```sql
-- 전체 내용 읽기
SELECT lo_get(24528);

-- 오프셋 0부터 3바이트 읽기
SELECT lo_get(24528, 0, 3);
-- 결과: \xffaaff

-- 오프셋 100부터 50바이트 읽기
SELECT lo_get(24528, 100, 50);
```

### 4.2 기본 Large Object 함수

#### lo_creat / lo_create - 대용량 객체 생성

```sql
-- 새로운 빈 대용량 객체 생성 (OID 자동 할당)
SELECT lo_creat(-1);

-- 특정 OID로 대용량 객체 생성
SELECT lo_create(43213);
```

#### lo_unlink - 대용량 객체 삭제

```sql
-- OID 173454인 대용량 객체 삭제
SELECT lo_unlink(173454);
```

#### lo_import - 서버 파일을 대용량 객체로 임포트

```sql
-- 서버의 파일을 대용량 객체로 임포트
SELECT lo_import('/etc/motd');

-- 테이블에 직접 저장하는 예시
INSERT INTO image (name, raster)
VALUES ('beautiful image', lo_import('/etc/motd'));
```

#### lo_export - 대용량 객체를 서버 파일로 내보내기

```sql
-- 대용량 객체를 서버의 파일로 내보내기
SELECT lo_export(image.raster, '/tmp/motd')
FROM image
WHERE name = 'beautiful image';
```

### 4.3 loread / lowrite - 서버측 읽기/쓰기

서버측에서 `lo_read`와 `lo_write`는 언더스코어 없이 loread, lowrite 로 사용됩니다.

```sql
-- 대용량 객체 열기
SELECT lo_open(24528, x'40000'::int);  -- INV_READ

-- 읽기 (loread)
SELECT loread(0, 100);  -- fd 0에서 100바이트 읽기

-- 쓰기 (lowrite)
SELECT lowrite(0, 'data'::bytea);  -- fd 0에 데이터 쓰기

-- 닫기
SELECT lo_close(0);
```

### 4.4 보안 주의사항

lo_import / lo_export 보안 경고:
- 기본적으로 슈퍼유저만 사용 가능합니다.
- 이 함수들은 서버 파일 시스템에 접근합니다(서버 소유자 권한으로).
- 비슈퍼유저에게 권한을 부여할 때는 매우 신중하게 검토해야 합니다.
- 악의적인 사용자가 이 권한을 통해 슈퍼유저 권한을 획득할 수 있습니다.

---

## 5. 예제 프로그램 (Example Program)

다음은 libpq를 사용하여 대용량 객체 인터페이스를 활용하는 방법을 보여주는 C 프로그램입니다.

이 프로그램은 PostgreSQL 소스 배포의 `src/test/examples/testlo.c`에서도 찾을 수 있습니다.

```c
/*-----------------------------------------------------------------
 *
 * testlo.c
 *    libpq를 사용한 대용량 객체 테스트 프로그램
 *
 *-----------------------------------------------------------------
 */
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include "libpq-fe.h"
#include "libpq/libpq-fs.h"

#define BUFSIZE 1024

/*
 * importFile - 파일을 대용량 객체로 임포트
 */
static Oid
importFile(PGconn *conn, char *filename)
{
    Oid         lobjId;
    int         lobj_fd;
    char        buf[BUFSIZE];
    int         nbytes, tmp;
    int         fd;

    /* 읽을 파일 열기 */
    fd = open(filename, O_RDONLY, 0666);
    if (fd < 0) {
        fprintf(stderr, "Unix 파일 \"%s\"을 열 수 없습니다\n", filename);
        return InvalidOid;
    }

    /* 대용량 객체 생성 */
    lobjId = lo_creat(conn, INV_READ | INV_WRITE);
    if (lobjId == InvalidOid) {
        fprintf(stderr, "대용량 객체를 생성할 수 없습니다\n");
        close(fd);
        return InvalidOid;
    }

    lobj_fd = lo_open(conn, lobjId, INV_WRITE);

    /* Unix 파일에서 읽어서 대용량 객체에 쓰기 */
    while ((nbytes = read(fd, buf, BUFSIZE)) > 0) {
        tmp = lo_write(conn, lobj_fd, buf, nbytes);
        if (tmp < nbytes) {
            fprintf(stderr, "\"%s\" 읽기 중 오류 발생\n", filename);
            break;
        }
    }

    close(fd);
    lo_close(conn, lobj_fd);

    return lobjId;
}

/*
 * pickout - 대용량 객체의 특정 범위 읽기
 */
static void
pickout(PGconn *conn, Oid lobjId, int start, int len)
{
    int         lobj_fd;
    char       *buf;
    int         nbytes, nread;

    lobj_fd = lo_open(conn, lobjId, INV_READ);
    if (lobj_fd < 0) {
        fprintf(stderr, "대용량 객체 %u를 열 수 없습니다\n", lobjId);
        return;
    }

    lo_lseek(conn, lobj_fd, start, SEEK_SET);
    buf = malloc(len + 1);

    nread = 0;
    while (len - nread > 0) {
        nbytes = lo_read(conn, lobj_fd, buf, len - nread);
        if (nbytes <= 0)
            break;  /* 더 이상 데이터 없음 */
        buf[nbytes] = '\0';
        fprintf(stderr, ">>> %s", buf);
        nread += nbytes;
    }

    free(buf);
    fprintf(stderr, "\n");
    lo_close(conn, lobj_fd);
}

/*
 * overwrite - 대용량 객체의 특정 범위 덮어쓰기
 */
static void
overwrite(PGconn *conn, Oid lobjId, int start, int len)
{
    int         lobj_fd;
    char       *buf;
    int         nbytes, nwritten, i;

    lobj_fd = lo_open(conn, lobjId, INV_WRITE);
    if (lobj_fd < 0) {
        fprintf(stderr, "대용량 객체 %u를 열 수 없습니다\n", lobjId);
        return;
    }

    lo_lseek(conn, lobj_fd, start, SEEK_SET);
    buf = malloc(len + 1);

    /* 'X' 문자로 채우기 */
    for (i = 0; i < len; i++)
        buf[i] = 'X';
    buf[i] = '\0';

    nwritten = 0;
    while (len - nwritten > 0) {
        nbytes = lo_write(conn, lobj_fd, buf + nwritten, len - nwritten);
        if (nbytes <= 0) {
            fprintf(stderr, "\n쓰기 실패!\n");
            break;
        }
        nwritten += nbytes;
    }

    free(buf);
    fprintf(stderr, "\n");
    lo_close(conn, lobj_fd);
}

/*
 * exportFile - 대용량 객체를 파일로 내보내기
 */
static void
exportFile(PGconn *conn, Oid lobjId, char *filename)
{
    int         lobj_fd;
    char        buf[BUFSIZE];
    int         nbytes, tmp;
    int         fd;

    /* 대용량 객체 열기 */
    lobj_fd = lo_open(conn, lobjId, INV_READ);
    if (lobj_fd < 0) {
        fprintf(stderr, "대용량 객체 %u를 열 수 없습니다\n", lobjId);
        return;
    }

    /* 출력 파일 열기 */
    fd = open(filename, O_CREAT | O_WRONLY | O_TRUNC, 0666);
    if (fd < 0) {
        fprintf(stderr, "Unix 파일 \"%s\"을 열 수 없습니다\n", filename);
        lo_close(conn, lobj_fd);
        return;
    }

    /* 대용량 객체에서 읽어서 Unix 파일에 쓰기 */
    while ((nbytes = lo_read(conn, lobj_fd, buf, BUFSIZE)) > 0) {
        tmp = write(fd, buf, nbytes);
        if (tmp < nbytes) {
            fprintf(stderr, "\"%s\" 쓰기 중 오류 발생\n", filename);
            break;
        }
    }

    lo_close(conn, lobj_fd);
    close(fd);
}

static void
exit_nicely(PGconn *conn)
{
    PQfinish(conn);
    exit(1);
}

int
main(int argc, char argv)
{
    char       *in_filename, *out_filename;
    char       *database;
    Oid         lobjOid;
    PGconn     *conn;
    PGresult   *res;

    if (argc != 4) {
        fprintf(stderr, "사용법: %s database_name in_filename out_filename\n",
                argv[0]);
        exit(1);
    }

    database = argv[1];
    in_filename = argv[2];
    out_filename = argv[3];

    /* 연결 설정 */
    conn = PQsetdb(NULL, NULL, NULL, NULL, database);

    /* 백엔드 연결 확인 */
    if (PQstatus(conn) != CONNECTION_OK) {
        fprintf(stderr, "%s", PQerrorMessage(conn));
        exit_nicely(conn);
    }

    /* 보안을 위한 search_path 설정 */
    res = PQexec(conn,
                 "SELECT pg_catalog.set_config('search_path', '', false)");
    if (PQresultStatus(res) != PGRES_TUPLES_OK) {
        fprintf(stderr, "SET 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        exit_nicely(conn);
    }
    PQclear(res);

    /* 트랜잭션 시작 */
    res = PQexec(conn, "BEGIN");
    PQclear(res);

    printf("파일 \"%s\" 임포트 중...\n", in_filename);
    lobjOid = lo_import(conn, in_filename);

    if (lobjOid == InvalidOid) {
        fprintf(stderr, "%s\n", PQerrorMessage(conn));
    } else {
        printf("\t대용량 객체 %u로 저장됨.\n", lobjOid);

        printf("대용량 객체의 바이트 1000-2000 읽기\n");
        pickout(conn, lobjOid, 1000, 1000);

        printf("대용량 객체의 바이트 1000-2000을 'X'로 덮어쓰기\n");
        overwrite(conn, lobjOid, 1000, 1000);

        printf("대용량 객체를 파일 \"%s\"로 내보내기...\n", out_filename);
        if (lo_export(conn, lobjOid, out_filename) < 0) {
            fprintf(stderr, "%s\n", PQerrorMessage(conn));
        }
    }

    /* 트랜잭션 종료 */
    res = PQexec(conn, "END");
    PQclear(res);

    PQfinish(conn);
    return 0;
}
```

### 프로그램 컴파일 및 실행

```bash
# 컴파일
gcc -o testlo testlo.c -I$(pg_config --includedir) -L$(pg_config --libdir) -lpq

# 실행
./testlo mydb input.txt output.txt
```

### 출력 예시

```
파일 "input.txt" 임포트 중...
    대용량 객체 24528로 저장됨.
대용량 객체의 바이트 1000-2000 읽기
>>> [해당 범위의 내용 출력]
대용량 객체의 바이트 1000-2000을 'X'로 덮어쓰기

대용량 객체를 파일 "output.txt"로 내보내기...
```

---

## 요약

| 기능 | 클라이언트 함수 (libpq) | 서버측 함수 (SQL) |
|------|------------------------|-------------------|
| 생성 | `lo_create()`, `lo_creat()` | `lo_create()`, `lo_creat()`, `lo_from_bytea()` |
| 임포트 | `lo_import()` | `lo_import()` |
| 내보내기 | `lo_export()` | `lo_export()` |
| 열기 | `lo_open()` | `lo_open()` |
| 읽기 | `lo_read()` | `loread()`, `lo_get()` |
| 쓰기 | `lo_write()` | `lowrite()`, `lo_put()` |
| 탐색 | `lo_lseek()`, `lo_lseek64()` | `lo_lseek()`, `lo_lseek64()` |
| 위치 조회 | `lo_tell()`, `lo_tell64()` | `lo_tell()`, `lo_tell64()` |
| 자르기 | `lo_truncate()`, `lo_truncate64()` | `lo_truncate()`, `lo_truncate64()` |
| 닫기 | `lo_close()` | `lo_close()` |
| 삭제 | `lo_unlink()` | `lo_unlink()` |

---

## 참고 자료

- [PostgreSQL 공식 문서 - Large Objects](https://www.postgresql.org/docs/current/largeobjects.html)
- [libpq - C 라이브러리](https://www.postgresql.org/docs/current/libpq.html)
- [TOAST 저장 시스템](https://www.postgresql.org/docs/current/storage-toast.html)
