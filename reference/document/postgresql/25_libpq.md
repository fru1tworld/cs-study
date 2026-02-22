# Chapter 32: libpq - C 라이브러리

## 개요

libpq 는 PostgreSQL의 C 애플리케이션 프로그래머 인터페이스(API)입니다. libpq는 클라이언트 프로그램이 PostgreSQL 백엔드 서버로 쿼리를 전달하고, 쿼리 결과를 수신하여 처리할 수 있게 해주는 라이브러리 함수들의 집합을 제공합니다.

libpq는 C++, Perl, Python, Tcl, ECPG 등 여러 다른 PostgreSQL 애플리케이션 인터페이스의 기반 엔진으로도 사용됩니다.

### 헤더 파일 포함

libpq를 사용하는 클라이언트 프로그램은 헤더 파일 `libpq-fe.h`를 포함해야 하며, libpq 라이브러리와 링크해야 합니다.

```c
#include <libpq-fe.h>
```

---

## 32.1 데이터베이스 연결 제어 함수 (Database Connection Control Functions)

데이터베이스 연결 제어 함수는 애플리케이션이 PostgreSQL 서버와의 연결을 설정하고 관리할 수 있게 해줍니다. 각 연결은 `PGconn` 객체로 표현됩니다.

### 32.1.1 주요 연결 함수

#### PQconnectdbParams

파라미터 배열을 사용하여 새 데이터베이스 연결을 엽니다. 새로운 애플리케이션에 권장되는 방법 입니다.

```c
PGconn *PQconnectdbParams(const char * const *keywords,
                          const char * const *values,
                          int expand_dbname);
```

파라미터:
- `keywords`: NULL로 종료되는 파라미터 키워드 배열
- `values`: 해당 값들의 NULL로 종료되는 배열
- `expand_dbname`: 0이 아닌 경우, 첫 번째 `dbname` 값이 `=` 또는 URI 스킴을 포함하면 확장

예제:
```c
const char *keywords[] = {"host", "port", "dbname", "user", NULL};
const char *values[] = {"localhost", "5432", "mydb", "postgres", NULL};

PGconn *conn = PQconnectdbParams(keywords, values, 0);

if (PQstatus(conn) != CONNECTION_OK) {
    fprintf(stderr, "연결 실패: %s", PQerrorMessage(conn));
    PQfinish(conn);
    return NULL;
}
```

#### PQconnectdb

연결 문자열(connection string)을 사용하여 새 데이터베이스 연결을 엽니다.

```c
PGconn *PQconnectdb(const char *conninfo);
```

예제:
```c
PGconn *conn = PQconnectdb("host=localhost port=5432 dbname=mydb connect_timeout=10");

if (PQstatus(conn) != CONNECTION_OK) {
    fprintf(stderr, "연결 실패: %s", PQerrorMessage(conn));
    PQfinish(conn);
    return NULL;
}
```

#### PQsetdbLogin (권장하지 않음)

`PQconnectdb`의 이전 버전으로, 고정된 파라미터들을 사용합니다.

```c
PGconn *PQsetdbLogin(const char *pghost,
                     const char *pgport,
                     const char *pgoptions,
                     const char *pgtty,
                     const char *dbName,
                     const char *login,
                     const char *pwd);
```

참고: `pgtty`는 더 이상 사용되지 않으며 무시됩니다.

### 32.1.2 연결 문자열 형식

#### 키워드/값 형식
```
host=localhost port=5432 dbname=mydb connect_timeout=10
```

#### URI 형식
```
postgresql://user:password@host:port/dbname?option=value
```

#### 다중 호스트
```
postgresql://host1:5432,host2:5432/dbname
```

### 32.1.3 주요 연결 파라미터

| 파라미터 | 설명 | 기본값 |
|---------|------|-------|
| `host` | 호스트명 또는 Unix 소켓 경로 | `/tmp` (Unix), `localhost` (Windows) |
| `port` | 포트 번호 | 5432 |
| `dbname` | 데이터베이스 이름 | 사용자 이름과 동일 |
| `user` | PostgreSQL 사용자명 | OS 사용자명 |
| `password` | 인증 비밀번호 | - |
| `connect_timeout` | 연결 타임아웃(초) | 무한 |
| `sslmode` | SSL 연결 모드 | `prefer` |
| `application_name` | 애플리케이션 식별자 | - |
| `options` | 서버 옵션 | - |

### 32.1.4 비차단(Non-blocking) 연결 함수

#### PQconnectStartParams / PQconnectStart

I/O를 차단하지 않고 비동기적으로 연결을 시작합니다.

```c
PGconn *PQconnectStartParams(const char * const *keywords,
                             const char * const *values,
                             int expand_dbname);

PGconn *PQconnectStart(const char *conninfo);

PostgresPollingStatusType PQconnectPoll(PGconn *conn);
```

사용 패턴:
```c
PGconn *conn = PQconnectStart(conninfo);
if (PQstatus(conn) == CONNECTION_BAD) {
    fprintf(stderr, "연결 실패: %s", PQerrorMessage(conn));
    PQfinish(conn);
    return NULL;
}

PostgresPollingStatusType pollStatus;
while ((pollStatus = PQconnectPoll(conn)) != PGRES_POLLING_OK) {
    if (pollStatus == PGRES_POLLING_FAILED) {
        fprintf(stderr, "폴링 실패: %s", PQerrorMessage(conn));
        PQfinish(conn);
        return NULL;
    }

    int sock = PQsocket(conn);
    if (pollStatus == PGRES_POLLING_READING) {
        // 소켓이 읽기 가능할 때까지 대기
    } else if (pollStatus == PGRES_POLLING_WRITING) {
        // 소켓이 쓰기 가능할 때까지 대기
    }
}
```

### 32.1.5 연결 관리 함수

#### PQfinish

연결을 닫고 메모리를 해제합니다.

```c
void PQfinish(PGconn *conn);
```

중요: 연결이 실패한 경우에도 항상 이 함수를 호출해야 합니다.

#### PQreset

서버와의 통신 채널을 재설정합니다.

```c
void PQreset(PGconn *conn);
```

#### PQresetStart / PQresetPoll

비차단 방식으로 연결을 재설정합니다.

```c
int PQresetStart(PGconn *conn);
PostgresPollingStatusType PQresetPoll(PGconn *conn);
```

### 32.1.6 서버 상태 함수

#### PQping / PQpingParams

전체 연결 없이 서버 상태를 보고합니다.

```c
PGPing PQping(const char *conninfo);
PGPing PQpingParams(const char * const *keywords,
                    const char * const *values,
                    int expand_dbname);
```

반환 값:
- `PQPING_OK`: 서버가 연결을 수락 중
- `PQPING_REJECT`: 서버가 연결을 거부 중
- `PQPING_NO_RESPONSE`: 서버에 도달할 수 없음
- `PQPING_NO_ATTEMPT`: 연결 파라미터가 유효하지 않음

---

## 32.2 연결 상태 함수 (Connection Status Functions)

연결 상태 함수는 기존 데이터베이스 연결 객체의 상태를 조회합니다.

### 32.2.1 연결 파라미터 함수

#### PQdb
연결의 데이터베이스 이름을 반환합니다.
```c
char *PQdb(const PGconn *conn);
```

#### PQuser
연결의 사용자 이름을 반환합니다.
```c
char *PQuser(const PGconn *conn);
```

#### PQpass
연결의 비밀번호를 반환합니다.
```c
char *PQpass(const PGconn *conn);
```

#### PQhost
활성 연결의 서버 호스트명을 반환합니다.
```c
char *PQhost(const PGconn *conn);
```

#### PQhostaddr
활성 연결의 서버 IP 주소를 반환합니다.
```c
char *PQhostaddr(const PGconn *conn);
```

#### PQport
활성 연결의 포트를 반환합니다.
```c
char *PQport(const PGconn *conn);
```

#### PQoptions
연결 요청에서 전달된 명령줄 옵션을 반환합니다.
```c
char *PQoptions(const PGconn *conn);
```

### 32.2.2 연결 상태 조회

#### PQstatus
연결의 상태를 반환합니다.
```c
ConnStatusType PQstatus(const PGconn *conn);
```

반환 값:
- `CONNECTION_OK`: 정상 연결
- `CONNECTION_BAD`: 연결 시도 실패 또는 통신 오류
- 비동기 연결 절차를 위한 기타 값들

#### PQtransactionStatus
서버의 현재 트랜잭션 내 상태를 반환합니다.
```c
PGTransactionStatusType PQtransactionStatus(const PGconn *conn);
```

반환 값:
- `PQTRANS_IDLE`: 현재 유휴 상태
- `PQTRANS_ACTIVE`: 명령 진행 중
- `PQTRANS_INTRANS`: 유효한 트랜잭션 블록에서 유휴 상태
- `PQTRANS_INERROR`: 실패한 트랜잭션 블록에서 유휴 상태
- `PQTRANS_UNKNOWN`: 연결이 잘못됨

#### PQparameterStatus
서버의 현재 파라미터 설정을 조회합니다.
```c
const char *PQparameterStatus(const PGconn *conn, const char *paramName);
```

조회 가능한 파라미터: `application_name`, `client_encoding`, `DateStyle`, `server_encoding`, `server_version`, `TimeZone`, `is_superuser` 등

#### PQprotocolVersion
프론트엔드/백엔드 프로토콜 메이저 버전을 반환합니다.
```c
int PQprotocolVersion(const PGconn *conn);
```

#### PQserverVersion
서버 버전을 나타내는 정수를 반환합니다.
```c
int PQserverVersion(const PGconn *conn);
```

버전 계산: major_version × 10000 + minor_version
- 예: 버전 10.1 → `100001`, 버전 11.0 → `110000`

#### PQerrorMessage
가장 최근에 생성된 오류 메시지를 반환합니다.
```c
char *PQerrorMessage(const PGconn *conn);
```

### 32.2.3 연결 상태 함수

#### PQsocket
연결 소켓의 파일 디스크립터 번호를 얻습니다.
```c
int PQsocket(const PGconn *conn);
```
- 유효한 디스크립터의 경우 ≥ 0, 열린 연결이 없으면 -1 반환

#### PQbackendPID
연결을 처리하는 백엔드의 프로세스 ID를 반환합니다.
```c
int PQbackendPID(const PGconn *conn);
```

#### PQconnectionNeedsPassword
인증에 비밀번호가 필요했지만 사용할 수 없었는지 확인합니다.
```c
int PQconnectionNeedsPassword(const PGconn *conn);
```

#### PQconnectionUsedPassword
인증 방법이 비밀번호를 사용했는지 확인합니다.
```c
int PQconnectionUsedPassword(const PGconn *conn);
```

### 32.2.4 SSL 관련 함수

#### PQsslInUse
연결이 SSL을 사용하는지 확인합니다.
```c
int PQsslInUse(const PGconn *conn);
```
- SSL 사용 시 1, 미사용 시 0 반환

#### PQsslAttribute
연결에 대한 SSL 관련 정보를 반환합니다.
```c
const char *PQsslAttribute(const PGconn *conn, const char *attribute_name);
```

일반적인 속성:
- `library`: SSL 구현 이름 (예: "OpenSSL")
- `protocol`: SSL/TLS 버전 (예: "TLSv1.2")
- `key_bits`: 사용된 키 비트 수
- `cipher`: 암호화 스위트 이름
- `compression`: "on" 또는 "off"

---

## 32.3 명령 실행 함수 (Command Execution Functions)

### 32.3.1 주요 함수

#### PQexec

명령을 서버에 제출하고 결과를 기다립니다.

```c
PGresult *PQexec(PGconn *conn, const char *command);
```

파라미터:
- `conn`: 연결 객체
- `command`: SQL 명령 문자열 (세미콜론으로 구분된 여러 SQL 명령 포함 가능)

주요 특성:
- 명시적인 BEGIN/COMMIT가 없으면 단일 트랜잭션에서 여러 SQL 명령 처리
- 실행된 마지막 명령 의 결과만 설명하는 `PGresult` 반환
- 명령이 실패하면 처리가 중단되고 오류 정보 반환
- 차단 호출(서버 응답을 기다림)

예제:
```c
PGresult *res = PQexec(conn, "SELECT * FROM users;");
if (PQresultStatus(res) != PGRES_TUPLES_OK) {
    fprintf(stderr, "명령 실패: %s\n", PQresultErrorMessage(res));
    PQclear(res);
    return;
}

int nrows = PQntuples(res);
int ncols = PQnfields(res);

// 컬럼 이름 출력
for (int i = 0; i < ncols; i++) {
    printf("%s\t", PQfname(res, i));
}
printf("\n");

// 데이터 행 출력
for (int i = 0; i < nrows; i++) {
    for (int j = 0; j < ncols; j++) {
        printf("%s\t", PQgetvalue(res, i, j));
    }
    printf("\n");
}

PQclear(res);
```

#### PQexecParams

별도로 지정된 파라미터로 명령을 제출하며, 바이너리 결과 형식을 지원합니다.

```c
PGresult *PQexecParams(PGconn *conn,
                       const char *command,
                       int nParams,
                       const Oid *paramTypes,
                       const char * const *paramValues,
                       const int *paramLengths,
                       const int *paramFormats,
                       int resultFormat);
```

파라미터:
- `conn`: 연결 객체
- `command`: 파라미터 플레이스홀더(`$1`, `$2` 등)가 있는 SQL 명령 문자열
- `nParams`: 제공된 파라미터 수
- `paramTypes[]`: 데이터 타입을 지정하는 OID 배열 (서버 추론을 위해 NULL 가능)
- `paramValues[]`: 파라미터 값 배열 (NULL 포인터 = NULL 값)
- `paramLengths[]`: 바이너리 파라미터의 바이트 길이 (텍스트 형식 및 NULL 값에서는 무시)
- `paramFormats[]`: 각 파라미터의 형식 표시기 (0 = 텍스트, 1 = 바이너리)
- `resultFormat`: 결과 형식 (0 = 텍스트, 1 = 바이너리)

주요 장점:
- 파라미터 값이 SQL 명령과 분리됨 (SQL 인젝션 방지)
- 수동 인용 및 이스케이프 불필요
- 호출당 하나의 SQL 명령 만 지원 (기본 프로토콜의 제한)
- 바이너리 형식으로 결과 요청 가능

예제:
```c
const char *paramValues[2];
int paramLengths[2];
int paramFormats[2];

paramValues[0] = "123";
paramLengths[0] = 0;      // 텍스트 형식에서는 무시
paramFormats[0] = 0;      // 텍스트 형식

paramValues[1] = "John";
paramLengths[1] = 0;
paramFormats[1] = 0;

const char *command = "INSERT INTO users (id, name) VALUES ($1, $2);";
PGresult *res = PQexecParams(conn, command, 2, NULL, paramValues,
                             paramLengths, paramFormats, 0);

if (PQresultStatus(res) != PGRES_COMMAND_OK) {
    fprintf(stderr, "삽입 실패: %s\n", PQresultErrorMessage(res));
}
PQclear(res);
```

팁: 바이너리 형식 사용 시 SQL에서 명시적 캐스팅으로 파라미터 타입을 강제:
```sql
SELECT * FROM mytable WHERE x = $1::bigint;
```

#### PQprepare

반복 실행을 위한 준비된 문장(prepared statement)을 생성합니다.

```c
PGresult *PQprepare(PGconn *conn,
                    const char *stmtName,
                    const char *query,
                    int nParams,
                    const Oid *paramTypes);
```

파라미터:
- `conn`: 연결 객체
- `stmtName`: 준비된 문장의 이름 (이름 없는 문장은 빈 문자열 `""`)
- `query`: `$1`, `$2` 플레이스홀더가 있는 단일 SQL 명령 문자열
- `nParams`: 사전 지정된 파라미터 수
- `paramTypes[]`: 파라미터 타입의 OID 배열 (서버 추론을 위해 NULL 가능)

예제:
```c
const Oid paramTypes[1] = {23};  // integer 타입 OID
PGresult *res = PQprepare(conn, "my_stmt",
                          "SELECT * FROM users WHERE id = $1;",
                          1, paramTypes);

if (PQresultStatus(res) != PGRES_COMMAND_OK) {
    fprintf(stderr, "준비 실패: %s\n", PQresultErrorMessage(res));
    PQclear(res);
    return;
}
PQclear(res);
```

#### PQexecPrepared

이전에 준비된 문장을 주어진 파라미터로 실행합니다.

```c
PGresult *PQexecPrepared(PGconn *conn,
                         const char *stmtName,
                         int nParams,
                         const char * const *paramValues,
                         const int *paramLengths,
                         const int *paramFormats,
                         int resultFormat);
```

PQexecParams와의 주요 차이점:
- 쿼리 문자열 대신 이름으로 준비된 문장을 지정
- `paramTypes[]` 불필요 (문장 생성 시 결정됨)
- 이전에 파싱/계획된 문장 재사용
- 반복 실행에 효율적

예제:
```c
// 이전에 준비된 문장 실행
const char *paramValues[1] = {"123"};
const int paramLengths[1] = {0};
const int paramFormats[1] = {0};

PGresult *res = PQexecPrepared(conn, "my_stmt", 1,
                               paramValues, paramLengths,
                               paramFormats, 0);

if (PQresultStatus(res) == PGRES_TUPLES_OK) {
    int rows = PQntuples(res);
    printf("%d개 행 조회됨\n", rows);
}
PQclear(res);
```

### 32.3.2 결과 상태 상수

명령 실행 후 결과 상태를 확인합니다:

```c
ExecStatusType PQresultStatus(const PGresult *res);
```

일반적인 상태 값:
- `PGRES_COMMAND_OK`: 명령 성공, 데이터 반환 없음
- `PGRES_TUPLES_OK`: 쿼리 성공, 데이터 반환됨
- `PGRES_FATAL_ERROR`: 치명적 오류 발생
- `PGRES_EMPTY_QUERY`: 빈 쿼리 문자열 전송됨
- `PGRES_COPY_IN`: 서버가 COPY 데이터 전송을 기다림
- `PGRES_COPY_OUT`: 서버가 COPY 데이터를 전송 중
- `PGRES_NONFATAL_ERROR`: 비치명적 오류(알림 또는 경고)

### 32.3.3 쿼리 결과 정보 조회

#### PQntuples
결과의 행(튜플) 수를 반환합니다.
```c
int PQntuples(const PGresult *res);
```

#### PQnfields
결과의 열(필드) 수를 반환합니다.
```c
int PQnfields(const PGresult *res);
```

#### PQfname
주어진 열 번호와 연관된 열 이름을 반환합니다.
```c
char *PQfname(const PGresult *res, int field_num);
```

#### PQfnumber
주어진 열 이름과 연관된 열 번호를 반환합니다.
```c
int PQfnumber(const PGresult *res, const char *field_name);
```

#### PQftype
주어진 열 번호와 연관된 데이터 타입을 반환합니다.
```c
Oid PQftype(const PGresult *res, int field_num);
```

#### PQfsize
주어진 열 번호와 연관된 열의 내부 저장 크기를 반환합니다.
```c
int PQfsize(const PGresult *res, int field_num);
```

#### PQgetvalue
단일 필드 값을 반환합니다.
```c
char *PQgetvalue(const PGresult *res, int tup_num, int field_num);
```

#### PQgetisnull
필드가 NULL 값인지 테스트합니다.
```c
int PQgetisnull(const PGresult *res, int tup_num, int field_num);
```

#### PQgetlength
필드 값의 실제 길이를 반환합니다.
```c
int PQgetlength(const PGresult *res, int tup_num, int field_num);
```

### 32.3.4 메모리 관리

```c
void PQclear(PGresult *res);
```

중요: 결과는 명시적으로 해제해야 합니다. 새 명령이 실행되거나 연결이 닫힐 때 자동으로 사라지지 않습니다.

### 32.3.5 보안 모범 사례

SQL 인젝션 공격을 방지하기 위해 항상 `PQexecParams()` 또는 `PQexecPrepared()`를 파라미터화된 쿼리와 함께 사용하세요:

```c
// 안전하지 않음 - SQL 인젝션에 취약
PQexec(conn, "SELECT * FROM users WHERE name = 'John';");

// 안전함 - 파라미터가 명령과 분리됨
const char *param = "John";
PQexecParams(conn, "SELECT * FROM users WHERE name = $1;",
             1, NULL, &param, NULL, NULL, 0);
```

---

## 32.4 비동기 명령 처리 (Asynchronous Command Processing)

PostgreSQL libpq의 비동기 명령 처리를 통해 애플리케이션은 결과를 기다리는 동안 차단되지 않고 명령을 제출할 수 있습니다. 이는 다음과 같은 경우에 유용합니다:

- 데이터베이스 작업이 진행되는 동안 응답성 유지 (예: UI 업데이트)
- 명령을 더 쉽게 취소
- 여러 SQL 명령을 개별적으로 처리
- 대용량 결과 집합을 점진적으로 처리
- 서버로의 출력 차단 방지

### 32.4.1 핵심 함수

#### PQsendQuery

결과를 기다리지 않고 명령을 서버에 제출합니다.

```c
int PQsendQuery(PGconn *conn, const char *command);
```

- 반환 값: 성공적으로 전송되면 1, 실패하면 0
- 오류 정보: 실패 시 `PQerrorMessage()` 사용
- 사용법: NULL이 반환될 때까지 `PQgetResult()`를 반복 호출해야 함
- 제한: `PQgetResult()`가 NULL을 반환할 때까지 다시 호출 불가

#### PQsendQueryParams

결과를 기다리지 않고 별도의 파라미터와 함께 명령을 제출합니다.

```c
int PQsendQueryParams(PGconn *conn,
                      const char *command,
                      int nParams,
                      const Oid *paramTypes,
                      const char * const *paramValues,
                      const int *paramLengths,
                      const int *paramFormats,
                      int resultFormat);
```

#### PQsendPrepare

비동기적으로 준비된 문장 생성 요청을 보냅니다.

```c
int PQsendPrepare(PGconn *conn,
                  const char *stmtName,
                  const char *query,
                  int nParams,
                  const Oid *paramTypes);
```

#### PQsendQueryPrepared

비동기적으로 준비된 문장을 실행합니다.

```c
int PQsendQueryPrepared(PGconn *conn,
                        const char *stmtName,
                        int nParams,
                        const char * const *paramValues,
                        const int *paramLengths,
                        const int *paramFormats,
                        int resultFormat);
```

#### PQgetResult

이전 send 명령으로부터 다음 결과를 조회합니다.

```c
PGresult *PQgetResult(PGconn *conn);
```

- 반환 값: 다음 `PGresult` 또는 명령 완료 시 NULL
- 사용법: NULL이 반환될 때까지 반복 호출
- 차단: 명령이 활성화되고 응답 데이터가 아직 읽히지 않은 경우에만 차단
- 메모리: 각 결과를 `PQclear()`로 해제

### 32.4.2 비차단 I/O 함수

#### PQconsumeInput

서버에서 사용 가능한 데이터를 읽습니다.

```c
int PQconsumeInput(PGconn *conn);
```

- 반환 값: 성공 시 1, 오류 시 0
- 동작: 들어오는 데이터를 버퍼링; 실제로 데이터가 수신되었는지 표시하지 않음
- 사용 사례: `PQisBusy()` 또는 `PQnotifies()` 전에 호출

#### PQisBusy

명령이 아직 처리 중인지 확인합니다.

```c
int PQisBusy(PGconn *conn);
```

- 반환 값: 바쁨(차단됨)이면 1, 준비되면 0
- 요구 사항: 먼저 `PQconsumeInput()` 호출 필요

#### PQsetnonblocking

연결을 비차단 모드로 설정합니다.

```c
int PQsetnonblocking(PGconn *conn, int arg);
```

- 파라미터: `arg=1`은 비차단, `arg=0`은 차단
- 반환 값: 성공 시 0, 오류 시 -1
- 중요: `PQexec()`는 비차단 모드를 무시하고 항상 차단

#### PQisnonblocking

현재 차단 상태를 반환합니다.

```c
int PQisnonblocking(const PGconn *conn);
```

#### PQflush

대기 중인 출력 데이터를 서버로 플러시합니다.

```c
int PQflush(PGconn *conn);
```

- 반환 값: 플러시되면 0, 오류 시 -1, 큐에 데이터가 남아있으면 1

### 32.4.3 일반적인 애플리케이션 패턴

```c
// 1. 비동기적으로 쿼리 전송
if (!PQsendQuery(conn, "SELECT * FROM table")) {
    fprintf(stderr, "오류: %s\n", PQerrorMessage(conn));
}

// 2. select() 또는 poll()을 사용하여 소켓 준비 대기
// 조건: PQsocket(conn)으로 식별된 소켓에서 읽기 가능한 데이터

// 3. 입력이 준비되면:
if (!PQconsumeInput(conn)) {
    fprintf(stderr, "오류: %s\n", PQerrorMessage(conn));
}

// 4. 명령 완료 확인
if (!PQisBusy(conn)) {
    // 5. 결과 가져오기
    PGresult *res;
    while ((res = PQgetResult(conn)) != NULL) {
        // 결과 처리
        PQclear(res);
    }
}

// 6. NOTIFY 메시지도 확인
PQnotifies(conn);
```

---

## 32.5 파이프라인 모드 (Pipeline Mode)

파이프라인 모드를 사용하면 애플리케이션이 이전 결과를 기다리지 않고 여러 쿼리를 전송할 수 있어, 단일 네트워크 트랜잭션에서 여러 쿼리/결과를 결합하여 상당한 성능 향상을 가능하게 합니다.

### 32.5.1 파이프라인 모드가 도움되는 경우

- 높은 네트워크 지연 시간: 왕복 시간이 300ms인 서버에서 100개 문장 작업은 파이프라이닝 없이 30초가 걸릴 수 있지만, 파이프라이닝으로 0.3초가 될 수 있음
- 빠른 소규모 작업: 집합 연산이나 `COPY`로 배치 처리할 수 없는 많은 `INSERT`, `UPDATE`, `DELETE` 작업

### 32.5.2 모드 진입/종료

#### PQenterPipelineMode

```c
int PQenterPipelineMode(PGconn *conn);
```

- 반환 값: 성공 시 1, 연결이 유휴 상태가 아니면 0
- 제한: 유휴 연결에서만 호출 가능

#### PQexitPipelineMode

```c
int PQexitPipelineMode(PGconn *conn);
```

- 반환 값: 성공 시 1, 결과가 아직 대기 중이면 0

#### PQpipelineStatus

```c
PGpipelineStatus PQpipelineStatus(const PGconn *conn);
```

반환 값:
- `PQ_PIPELINE_ON`: 현재 파이프라인 모드
- `PQ_PIPELINE_OFF`: 파이프라인 모드 아님
- `PQ_PIPELINE_ABORTED`: 오류가 있는 파이프라인 모드

### 32.5.3 파이프라인 모드에서 허용되는 함수

- `PQsendQueryParams()` - 파라미터와 함께 쿼리 전송
- `PQsendQueryPrepared()` - 준비된 쿼리 전송
- `PQsendPrepare()` - PREPARE 문장 전송
- `PQsendDescribePrepared()` - 준비된 문장에 대한 DESCRIBE 전송
- `PQsendDescribePortal()` - 포털에 대한 DESCRIBE 전송

### 32.5.4 파이프라인 모드에서 금지된 함수

- `PQexec()`, `PQexecParams()`, `PQprepare()`, `PQexecPrepared()`
- `PQsendQuery()` (단순 쿼리 프로토콜 사용)
- 단일 문자열에 여러 SQL 명령
- `COPY` 작업

### 32.5.5 동기화 및 플러시

#### PQpipelineSync

```c
int PQpipelineSync(PGconn *conn);
```

- 반환 값: 성공 시 1, 파이프라인 모드가 아니거나 전송 실패 시 0
- 효과: 동기화 메시지를 전송하고 전송 버퍼를 플러시
- 목적: 암시적 트랜잭션 구분자 및 오류 복구 지점 설정

#### PQsendFlushRequest

```c
int PQsendFlushRequest(PGconn *conn);
```

- 목적: 동기화 지점을 설정하지 않고 서버가 출력 버퍼를 플러시하게 함

### 32.5.6 결과 처리 패턴

```c
// 한 쿼리의 결과 처리
PGresult *result;
while ((result = PQgetResult(conn)) != NULL) {
    ExecStatusType status = PQresultStatus(result);

    if (status == PGRES_PIPELINE_SYNC) {
        // 파이프라인 끝 마커
        PQclear(result);
        break;
    }

    if (status == PGRES_PIPELINE_ABORTED) {
        // 중단된 작업 처리
        PQclear(result);
        continue;
    }

    // 정상 결과 처리
    PQclear(result);
}
```

---

## 32.6 결과를 청크로 조회 (Retrieving Query Results in Chunks)

대용량 결과 집합의 경우, `PQsetSingleRowMode`를 사용하여 결과를 한 번에 한 행씩 조회할 수 있습니다.

```c
int PQsetSingleRowMode(PGconn *conn);
```

이 함수는 `PQsendQuery` 또는 유사한 함수 직후, `PQgetResult` 전에 호출해야 합니다.

---

## 32.7 진행 중인 쿼리 취소 (Canceling Queries in Progress)

### 32.7.1 취소 요청 전송 함수

#### PQcancelCreate

취소 요청 전송을 위한 연결을 준비합니다.

```c
PGcancelConn *PQcancelCreate(PGconn *conn);
```

- 쿼리 취소에 재사용할 수 있는 `PGcancelConn` 객체 생성
- 완료 시 메모리 해제를 위해 `PQcancelFinish()` 호출 필요
- 원래 연결의 SSL/GSS 암호화 요구 사항 준수
- 원래 연결에서 쿼리 취소를 위해 스레드 안전

#### PQcancelBlocking

동기적으로(차단) 취소 요청을 보냅니다.

```c
int PQcancelBlocking(PGcancelConn *cancelConn);
```

반환 값:
- 취소 요청이 성공적으로 전송되면 1
- 실패하면 0 (자세한 내용은 `PQcancelErrorMessage()` 사용)

참고: 전송 성공 ≠ 쿼리 종료; 서버가 이미 처리를 완료했을 수 있음

#### PQcancelStart / PQcancelPoll

비동기적으로(비차단) 취소 요청을 보냅니다.

```c
int PQcancelStart(PGcancelConn *cancelConn);
PostgresPollingStatusType PQcancelPoll(PGcancelConn *cancelConn);
```

#### PQcancelFinish

취소 연결을 닫고 메모리를 해제합니다.

```c
void PQcancelFinish(PGcancelConn *cancelConn);
```

취소 시도가 실패하거나 중단된 경우에도 반드시 호출해야 합니다.

### 32.7.2 권장하지 않는 함수 (Obsolete Functions)

#### PQgetCancel (권장하지 않음)
```c
PGcancel *PQgetCancel(PGconn *conn);
```
취소 객체를 생성합니다. 권장하지 않음: 대신 `PQcancelCreate()` 사용

#### PQcancel (권장하지 않음)
```c
int PQcancel(PGcancel *cancel, char *errbuf, int errbufsize);
```
권장하지 않음: 대신 `PQcancelBlocking()` 사용
- 유일한 장점: 시그널 핸들러에서 안전하게 호출 가능
- 보안 취약: 원래 연결이 암호화를 요구해도 암호화 사용하지 않음

### 32.7.3 보안 고려 사항

최신 함수 (`PQcancelCreate`, `PQcancelBlocking`, `PQcancelStart/Poll`):
- 암호화된 취소 요청 지원 (SSL/GSS)
- 스레드 안전
- 모든 새 코드에 권장

권장하지 않는 함수 (`PQcancel`, `PQrequestCancel`):
- 암호화되지 않은 취소 요청 전송
- 보안 취약, 강력히 권장하지 않음

---

## 32.8 Fast-Path 인터페이스

Fast-Path 인터페이스는 서버로 간단한 함수 호출을 보내는 오래된 메커니즘입니다. 현재는 대부분의 목적에 준비된 문장이 선호됩니다.

```c
PGresult *PQfn(PGconn *conn,
               int fnid,
               int *result_buf,
               int *result_len,
               int result_is_int,
               const PQArgBlock *args,
               int nargs);
```

---

## 32.9 비동기 알림 (Asynchronous Notification)

PostgreSQL은 `LISTEN` 및 `NOTIFY` 명령을 통해 비동기 알림을 제공하여, 클라이언트가 폴링 없이 알림 채널을 구독하고 메시지를 수신할 수 있게 합니다.

### 32.9.1 작동 방식

- 클라이언트가 `LISTEN` 명령을 사용하여 알림 채널에 관심을 등록
- 클라이언트는 `UNLISTEN`으로 수신을 중지할 수 있음
- 모든 세션이 선택적 페이로드 데이터와 함께 `NOTIFY` 메시지를 브로드캐스트할 수 있음
- 모든 수신 중인 세션이 비동기적으로 알림을 받음

### 32.9.2 함수 시그니처

```c
PGnotify *PQnotifies(PGconn *conn);
```

### 32.9.3 데이터 구조

```c
typedef struct pgNotify
{
    char *relname;              /* 알림 채널 이름 */
    int  be_pid;                /* 알림을 보내는 서버 프로세스의 프로세스 ID */
    char *extra;                /* 알림 페이로드 문자열 */
} PGnotify;
```

### 32.9.4 사용 패턴

```c
// 권장 접근 방식:
select();  // PQsocket()의 파일 디스크립터를 사용하여 서버 데이터 대기
PQconsumeInput(conn);  // 서버에서 사용 가능한 데이터 읽기
PGnotify *notify = PQnotifies(conn);  // 알림 확인

if (notify) {
    printf("채널 %s에서 알림 수신: %s\n", notify->relname, notify->extra);
    PQfreemem(notify);
}
```

항상 다음 호출 후 알림을 확인하세요:
- `PQgetResult()` 호출
- `PQexec()` 호출
- `PQconsumeInput()` 호출

---

## 32.10 COPY 명령 관련 함수

`COPY` 명령을 사용하면 libpq를 통해 네트워크 연결에서 읽거나 쓸 수 있습니다.

### 32.10.1 COPY 데이터 전송 함수

#### PQputCopyData

`COPY_IN` 상태에서 서버로 데이터를 전송합니다.

```c
int PQputCopyData(PGconn *conn,
                  const char *buffer,
                  int nbytes);
```

반환 값:
- `1`: 데이터가 성공적으로 큐에 추가됨
- `0`: 데이터가 큐에 추가되지 않음 (비차단 모드, 버퍼 가득 참)
- `-1`: 오류 발생 (자세한 내용은 `PQerrorMessage()` 사용)

#### PQputCopyEnd

`COPY_IN` 작업을 종료합니다.

```c
int PQputCopyEnd(PGconn *conn,
                 const char *errormsg);
```

파라미터:
- `errormsg`: 성공적 완료를 위해 `NULL`, 또는 COPY를 실패시킬 오류 메시지 문자열

### 32.10.2 COPY 데이터 수신 함수

#### PQgetCopyData

`COPY_OUT` 상태에서 서버로부터 데이터를 수신합니다.

```c
int PQgetCopyData(PGconn *conn,
                  char buffer,
                  int async);
```

파라미터:
- `buffer`: NULL이 아닌 포인터; 할당된 메모리 또는 NULL을 가리키도록 설정
- `async`: 비차단 모드는 0이 아닌 값, 차단 모드는 0

반환 값:
- `> 0`: 반환된 데이터 행의 바이트 수
- `0`: COPY 진행 중, 완전한 행 없음 (비동기 모드에서만)
- `-1`: COPY 완료
- `-2`: 오류 발생

### 32.10.3 일반적인 사용 패턴

```c
// COPY FROM STDIN 중 데이터 전송
PQexec(conn, "COPY table_name FROM STDIN");
PGresult *res = PQgetResult(conn);

if (PQresultStatus(res) == PGRES_COPY_IN) {
    PQputCopyData(conn, data_buffer, nbytes);
    PQputCopyEnd(conn, NULL);  // 성공적으로 종료
    res = PQgetResult(conn);
}

// COPY TO STDOUT 중 데이터 수신
PQexec(conn, "COPY table_name TO STDOUT");
res = PQgetResult(conn);

if (PQresultStatus(res) == PGRES_COPY_OUT) {
    char *buffer;
    while (PQgetCopyData(conn, &buffer, 0) > 0) {
        // 버퍼 처리
        PQfreemem(buffer);
    }
    res = PQgetResult(conn);
}
```

---

## 32.11 제어 함수 (Control Functions)

제어 함수는 libpq 동작의 다양한 세부 사항을 관리합니다.

### 32.11.1 클라이언트 인코딩 함수

#### PQclientEncoding
클라이언트 인코딩 ID를 반환합니다.
```c
int PQclientEncoding(const PGconn *conn);
```

#### PQsetClientEncoding
연결의 클라이언트 인코딩을 설정합니다.
```c
int PQsetClientEncoding(PGconn *conn, const char *encoding);
```
- 성공 시 0, 실패 시 -1 반환

### 32.11.2 오류 메시지 제어 함수

#### PQsetErrorVerbosity
`PQerrorMessage()` 및 `PQresultErrorMessage()`의 오류 메시지 상세도를 결정합니다.

```c
typedef enum {
    PQERRORS_TERSE,      // 심각도, 기본 텍스트, 위치만
    PQERRORS_DEFAULT,    // 위 + 세부 정보, 힌트, 컨텍스트 (기본값)
    PQERRORS_VERBOSE,    // 사용 가능한 모든 필드
    PQERRORS_SQLSTATE    // 심각도와 SQLSTATE 코드만
} PGVerbosity;

PGVerbosity PQsetErrorVerbosity(PGconn *conn, PGVerbosity verbosity);
```

#### PQsetErrorContextVisibility
`CONTEXT` 필드가 오류 메시지에 나타나는지 제어합니다.

```c
typedef enum {
    PQSHOW_CONTEXT_NEVER,    // CONTEXT 포함 안 함
    PQSHOW_CONTEXT_ERRORS,   // 오류에만 포함 (기본값)
    PQSHOW_CONTEXT_ALWAYS    // 사용 가능하면 항상 포함
} PGContextVisibility;

PGContextVisibility PQsetErrorContextVisibility(PGconn *conn,
                                               PGContextVisibility show_context);
```

### 32.11.3 추적 함수

#### PQtrace
클라이언트/서버 통신 추적을 파일 스트림으로 활성화합니다.
```c
void PQtrace(PGconn *conn, FILE *stream);
```

#### PQsetTraceFlags
추적 동작을 제어합니다. `PQtrace()` 후에 호출해야 합니다.
```c
void PQsetTraceFlags(PGconn *conn, int flags);
```

플래그:
- `PQTRACE_SUPPRESS_TIMESTAMPS` - 타임스탬프 생략
- `PQTRACE_REGRESS_MODE` - 테스트를 위해 객체 OID 등의 필드를 수정

#### PQuntrace
추적을 비활성화합니다.
```c
void PQuntrace(PGconn *conn);
```

---

## 32.12 기타 함수 (Miscellaneous Functions)

### PQfreemem
libpq가 할당한 메모리를 해제합니다.
```c
void PQfreemem(void *ptr);
```

Windows에서 중요: DLL/애플리케이션 메모리 할당 호환성 요구 사항으로 인해 `free()` 대신 반드시 사용해야 합니다.

### PQconninfoFree
연결 함수가 할당한 데이터 구조를 해제합니다.
```c
void PQconninfoFree(PQconninfoOption *connOptions);
```

### PQencryptPasswordConn
PostgreSQL 비밀번호의 암호화된 형식을 준비합니다.
```c
char *PQencryptPasswordConn(PGconn *conn, const char *passwd,
                            const char *user, const char *algorithm);
```

파라미터:
- `passwd`: 평문 비밀번호
- `user`: 사용자의 SQL 이름
- `algorithm`: 암호화 방법 (`md5`, `scram-sha-256`, `on`, 또는 `off`)

### PQlibVersion
libpq 버전 번호를 반환합니다.
```c
int PQlibVersion(void);
```

버전 계산: (major × 10000) + minor
- 버전 10.1 → `100001`
- 버전 11.0 → `110000`

---

## 32.13 알림 처리 (Notice Processing)

PostgreSQL 서버가 생성한 알림 및 경고 메시지는 쿼리 실패로 반환되지 않고 알림 처리 시스템을 통해 처리됩니다.

### 32.13.1 2계층 아키텍처

1. 알림 수신기(Notice Receiver): 알림을 형식화하고 메시지 세부 정보 추출
2. 알림 프로세서(Notice Processor): 형식화된 메시지 텍스트 처리 (일반적으로 출력용)

### 32.13.2 함수 시그니처

#### 알림 수신기

```c
typedef void (*PQnoticeReceiver) (void *arg, const PGresult *res);

PQnoticeReceiver
PQsetNoticeReceiver(PGconn *conn,
                    PQnoticeReceiver proc,
                    void *arg);
```

#### 알림 프로세서

```c
typedef void (*PQnoticeProcessor) (void *arg, const char *message);

PQnoticeProcessor
PQsetNoticeProcessor(PGconn *conn,
                     PQnoticeProcessor proc,
                     void *arg);
```

### 32.13.3 기본 동작

기본 알림 프로세서 구현:

```c
static void
defaultNoticeProcessor(void *arg, const char *message)
{
    fprintf(stderr, "%s", message);
}
```

메시지를 `stderr`에 출력합니다.

---

## 32.14 환경 변수 (Environment Variables)

libpq에서 사용하는 다양한 환경 변수들:

### 연결 파라미터

| 변수 | 설명 |
|-----|------|
| `PGHOST` | 데이터베이스 서버 호스트명 |
| `PGHOSTADDR` | 데이터베이스 서버 IP 주소 (DNS 조회 회피) |
| `PGPORT` | 데이터베이스 서버 포트 번호 |
| `PGDATABASE` | 데이터베이스 이름 |
| `PGUSER` | 데이터베이스 사용자 이름 |
| `PGPASSWORD` | 데이터베이스 비밀번호 (보안 위험 - 대신 비밀번호 파일 사용) |
| `PGPASSFILE` | 비밀번호 파일 경로 |
| `PGSERVICE` | 연결용 서비스 이름 |
| `PGOPTIONS` | 서버로 전달할 명령줄 옵션 |
| `PGAPPNAME` | 연결용 애플리케이션 이름 |
| `PGCONNECT_TIMEOUT` | 연결 타임아웃(초) |
| `PGCLIENTENCODING` | 클라이언트 문자 인코딩 |

### SSL/TLS 파라미터

| 변수 | 설명 |
|-----|------|
| `PGSSLMODE` | SSL 연결 모드 |
| `PGSSLCERT` | 클라이언트 인증서 파일 |
| `PGSSLKEY` | 클라이언트 키 파일 |
| `PGSSLROOTCERT` | 루트 인증서 파일 |
| `PGSSLCRL` | 인증서 폐기 목록 |

### GSSAPI 파라미터

| 변수 | 설명 |
|-----|------|
| `PGGSSENCMODE` | GSSAPI 암호화 모드 |
| `PGKRBSRVNAME` | Kerberos 서비스 이름 |

### 사용 참고 사항

- 이 변수들은 `PQconnectdb()`, `PQsetdbLogin()`, `PQsetdb()` 함수에서 사용됨
- 직접 연결 파라미터가 환경 변수 값을 재정의
- 보안 경고: `PGPASSWORD` 사용을 피하고 대신 비밀번호 파일 사용

---

## 32.15 SSL 지원 (SSL Support)

PostgreSQL은 SSL/TLS 암호화 클라이언트-서버 통신을 기본 지원합니다.

### 32.15.1 SSL 모드 옵션

`sslmode` 파라미터는 SSL 보호 수준을 제어합니다:

| 모드 | 도청 방지 | MITM 보호 | 사용 사례 |
|-----|---------|-----------|---------|
| `disable` | 아니오 | 아니오 | 보안 불필요 |
| `allow` | 가능 | 아니오 | 서버가 요구하면 선택적 암호화 |
| `prefer` | 가능 | 아니오 | 암호화 선호하지만 필수 아님 (기본값 - 권장하지 않음) |
| `require` | 예 | 아니오 | 암호화 필수; 네트워크 라우팅 신뢰 |
| `verify-ca` | 예 | CA에 따라 다름 | 서버의 CA 인증서 신뢰 |
| `verify-full` | 예 | 예 | 권장 - 서버 신원 및 호스트명이 인증서와 일치하는지 확인 |

### 32.15.2 서버 인증서 검증

#### 설정

루트 CA 인증서 위치:
- Linux/Unix: `~/.postgresql/root.crt`
- Windows: `%APPDATA%\postgresql\root.crt`

연결 파라미터로 위치 재정의:
```
sslrootcert=/path/to/root.crt
sslcrl=/path/to/root.crl
```

### 32.15.3 클라이언트 인증서

#### 파일 위치

- Linux/Unix: `~/.postgresql/postgresql.crt` 및 `~/.postgresql/postgresql.key`
- Windows: `%APPDATA%\postgresql\postgresql.crt` 및 `%APPDATA%\postgresql\postgresql.key`

#### 요구 사항

- Unix에서 개인 키 파일 권한: `chmod 0600` (사용자 읽기/쓰기만) - 권장

### 32.15.4 보안 권장 사항

1. 보안에 민감한 환경에서는 `verify-full` 모드 사용
2. 서버 검증을 위해 클라이언트에 루트 CA 인증서 배치
3. 서버가 인증을 요구할 때 클라이언트 인증서 사용
4. 개인 키에 적절한 파일 권한 설정 (Unix: `0600`)
5. `prefer` 기본값 피하기; 명시적으로 `verify-ca` 또는 `verify-full` 설정

---

## 32.16 예제 프로그램 (Example Programs)

### 예제 1: 기본 연결 및 쿼리 실행

```c
#include <stdio.h>
#include <stdlib.h>
#include <libpq-fe.h>

static void
exit_nicely(PGconn *conn)
{
    PQfinish(conn);
    exit(1);
}

int
main(int argc, char argv)
{
    const char *conninfo;
    PGconn     *conn;
    PGresult   *res;
    int         nFields;
    int         i, j;

    /* 연결 정보 설정 */
    if (argc > 1)
        conninfo = argv[1];
    else
        conninfo = "dbname = postgres";

    /* 데이터베이스에 연결 */
    conn = PQconnectdb(conninfo);

    /* 백엔드 연결이 성공적으로 이루어졌는지 확인 */
    if (PQstatus(conn) != CONNECTION_OK)
    {
        fprintf(stderr, "데이터베이스 연결 실패: %s",
                PQerrorMessage(conn));
        exit_nicely(conn);
    }

    /* 보안 설정을 위해 search_path 설정 */
    res = PQexec(conn, "SELECT pg_catalog.set_config('search_path', '', false)");
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        fprintf(stderr, "SET 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        exit_nicely(conn);
    }
    PQclear(res);

    /* 트랜잭션 블록 시작 */
    res = PQexec(conn, "BEGIN");
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "BEGIN 명령 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        exit_nicely(conn);
    }
    PQclear(res);

    /* 커서로 데이터 가져오기 */
    res = PQexec(conn, "DECLARE myportal CURSOR FOR select * from pg_database");
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "DECLARE CURSOR 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        exit_nicely(conn);
    }
    PQclear(res);

    res = PQexec(conn, "FETCH ALL in myportal");
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        fprintf(stderr, "FETCH ALL 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        exit_nicely(conn);
    }

    /* 먼저 속성 이름 출력 */
    nFields = PQnfields(res);
    for (i = 0; i < nFields; i++)
        printf("%-15s", PQfname(res, i));
    printf("\n\n");

    /* 다음으로 행 출력 */
    for (i = 0; i < PQntuples(res); i++)
    {
        for (j = 0; j < nFields; j++)
            printf("%-15s", PQgetvalue(res, i, j));
        printf("\n");
    }

    PQclear(res);

    /* 포털 닫기 */
    res = PQexec(conn, "CLOSE myportal");
    PQclear(res);

    /* 트랜잭션 종료 */
    res = PQexec(conn, "END");
    PQclear(res);

    /* 데이터베이스 연결 닫기 및 정리 */
    PQfinish(conn);

    return 0;
}
```

### 예제 2: 파라미터화된 쿼리

```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <libpq-fe.h>

int
main(int argc, char argv)
{
    PGconn     *conn;
    PGresult   *res;
    const char *conninfo = "dbname=postgres";

    /* 데이터베이스에 연결 */
    conn = PQconnectdb(conninfo);
    if (PQstatus(conn) != CONNECTION_OK)
    {
        fprintf(stderr, "연결 실패: %s", PQerrorMessage(conn));
        PQfinish(conn);
        return 1;
    }

    /* 파라미터화된 쿼리 예제 */
    const char *paramValues[1];
    paramValues[0] = "postgres";

    res = PQexecParams(conn,
                       "SELECT * FROM pg_database WHERE datname = $1",
                       1,       /* 파라미터 수 */
                       NULL,    /* 파라미터 타입 자동 추론 */
                       paramValues,
                       NULL,    /* 텍스트 파라미터이므로 길이 불필요 */
                       NULL,    /* 기본적으로 모두 텍스트 */
                       0);      /* 텍스트 형식으로 결과 요청 */

    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        fprintf(stderr, "쿼리 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        PQfinish(conn);
        return 1;
    }

    /* 결과 출력 */
    int nrows = PQntuples(res);
    int ncols = PQnfields(res);

    printf("%d개 행 조회됨\n", nrows);

    for (int i = 0; i < nrows; i++)
    {
        printf("행 %d:\n", i);
        for (int j = 0; j < ncols; j++)
        {
            printf("  %s = %s\n", PQfname(res, j), PQgetvalue(res, i, j));
        }
    }

    PQclear(res);
    PQfinish(conn);
    return 0;
}
```

### 예제 3: 비동기 알림 처리

```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/time.h>
#include <sys/types.h>
#include <libpq-fe.h>

int
main(int argc, char argv)
{
    PGconn     *conn;
    PGresult   *res;
    PGnotify   *notify;
    int         sock;

    conn = PQconnectdb("dbname=postgres");
    if (PQstatus(conn) != CONNECTION_OK)
    {
        fprintf(stderr, "연결 실패: %s", PQerrorMessage(conn));
        PQfinish(conn);
        return 1;
    }

    /* LISTEN 명령 실행 */
    res = PQexec(conn, "LISTEN TBL2");
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "LISTEN 실패: %s", PQerrorMessage(conn));
        PQclear(res);
        PQfinish(conn);
        return 1;
    }
    PQclear(res);

    /* 소켓 가져오기 */
    sock = PQsocket(conn);

    printf("알림 대기 중...\n");

    while (1)
    {
        fd_set      input_mask;

        FD_ZERO(&input_mask);
        FD_SET(sock, &input_mask);

        /* select()로 입력 대기 */
        if (select(sock + 1, &input_mask, NULL, NULL, NULL) < 0)
        {
            fprintf(stderr, "select() 실패: %s\n", strerror(errno));
            break;
        }

        /* 서버에서 입력 읽기 */
        PQconsumeInput(conn);

        /* 알림 확인 */
        while ((notify = PQnotifies(conn)) != NULL)
        {
            printf("백엔드 PID %d에서 비동기 알림 '%s' 수신, 페이로드: '%s'\n",
                   notify->be_pid, notify->relname, notify->extra);
            PQfreemem(notify);
        }
    }

    PQfinish(conn);
    return 0;
}
```

---

## 32.17 libpq 프로그램 빌드 (Building libpq Programs)

### 컴파일

```bash
gcc -I$(pg_config --includedir) -c myprogram.c
```

### 링크

```bash
gcc -L$(pg_config --libdir) -lpq myprogram.o -o myprogram
```

### pkg-config 사용

```bash
gcc $(pkg-config --cflags --libs libpq) myprogram.c -o myprogram
```

---

## 요약

libpq는 PostgreSQL의 핵심 C 라이브러리로서 다음 기능을 제공합니다:

1. 데이터베이스 연결: `PQconnectdb`, `PQconnectdbParams` 등으로 연결 설정
2. 연결 상태 조회: `PQstatus`, `PQerrorMessage` 등으로 연결 상태 확인
3. 명령 실행: `PQexec`, `PQexecParams`, `PQprepare`, `PQexecPrepared` 등
4. 비동기 처리: `PQsendQuery`, `PQgetResult` 등으로 비차단 작업
5. 파이프라인 모드: 여러 쿼리를 효율적으로 배치 처리
6. 쿼리 취소: `PQcancelBlocking` 등으로 진행 중인 쿼리 취소
7. 알림 처리: `PQnotifies`로 비동기 알림 수신
8. SSL 지원: 암호화된 연결 설정

libpq를 효과적으로 사용하면 안전하고 효율적인 PostgreSQL 클라이언트 애플리케이션을 개발할 수 있습니다.
