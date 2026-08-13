# PostgreSQL 클라이언트 인터페이스

## Chapter 32: libpq - C 라이브러리

### 개요

- libpq는 PostgreSQL의 C 애플리케이션 프로그래머 인터페이스(API)
- 클라이언트 프로그램이 PostgreSQL 백엔드 서버로 쿼리를 전달하고, 쿼리 결과를 수신·처리할 수 있게 해주는 라이브러리 함수들의 집합을 제공
- C++, Perl, Python, Tcl, ECPG 등 여러 다른 PostgreSQL 애플리케이션 인터페이스의 기반 엔진으로도 사용됨

#### 헤더 파일 포함

- libpq를 사용하는 클라이언트 프로그램은 헤더 파일 `libpq-fe.h`를 포함해야 하며, libpq 라이브러리와 링크 필요

```c
#include <libpq-fe.h>
```

---

### 32.1 데이터베이스 연결 제어 함수 (Database Connection Control Functions)

- 데이터베이스 연결 제어 함수는 애플리케이션이 PostgreSQL 서버와의 연결을 설정·관리할 수 있게 해줌
- 각 연결은 `PGconn` 객체로 표현됨

#### 32.1.1 주요 연결 함수

##### PQconnectdbParams

- 파라미터 배열을 사용하여 새 데이터베이스 연결을 염
- 새 애플리케이션에 권장하는 방법

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

##### PQconnectdb

연결 문자열(connection string)을 사용하여 새 데이터베이스 연결을 염.

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

##### PQsetdbLogin (권장하지 않음)

`PQconnectdb`의 구버전으로, 고정된 파라미터를 사용함.

```c
PGconn *PQsetdbLogin(const char *pghost,
                     const char *pgport,
                     const char *pgoptions,
                     const char *pgtty,
                     const char *dbName,
                     const char *login,
                     const char *pwd);
```

참고: `pgtty`는 더 이상 사용되지 않으며 무시됨.

#### 32.1.2 연결 문자열 형식

##### 키워드/값 형식
```
host=localhost port=5432 dbname=mydb connect_timeout=10
```

##### URI 형식
```
postgresql://user:password@host:port/dbname?option=value
```

##### 다중 호스트
```
postgresql://host1:5432,host2:5432/dbname
```

#### 32.1.3 주요 연결 파라미터

- `host`
  - 설명: 호스트명 또는 Unix 소켓 경로
  - 기본값: `/tmp` (Unix), `localhost` (Windows)
- `port`
  - 설명: 포트 번호
  - 기본값: 5432
- `dbname`
  - 설명: 데이터베이스 이름
  - 기본값: 사용자 이름과 동일
- `user`
  - 설명: PostgreSQL 사용자명
  - 기본값: OS 사용자명
- `password`
  - 설명: 인증 비밀번호
- `connect_timeout`
  - 설명: 연결 타임아웃(초)
  - 기본값: 무한
- `sslmode`
  - 설명: SSL 연결 모드
  - 기본값: `prefer`
- `application_name`
  - 설명: 애플리케이션 식별자
- `options`
  - 설명: 서버 옵션

#### 32.1.4 비차단(Non-blocking) 연결 함수

##### PQconnectStartParams / PQconnectStart

I/O를 차단하지 않고 비동기적으로 연결을 시작함.

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

#### 32.1.5 연결 관리 함수

##### PQfinish

연결을 닫고 메모리를 해제함.

```c
void PQfinish(PGconn *conn);
```

중요: 연결에 실패한 경우에도 반드시 이 함수를 호출 필요.

##### PQreset

서버와의 통신 채널을 재설정함.

```c
void PQreset(PGconn *conn);
```

##### PQresetStart / PQresetPoll

비차단 방식으로 연결을 재설정함.

```c
int PQresetStart(PGconn *conn);
PostgresPollingStatusType PQresetPoll(PGconn *conn);
```

#### 32.1.6 서버 상태 함수

##### PQping / PQpingParams

전체 연결 없이 서버 상태를 확인함.

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

### 32.2 연결 상태 함수 (Connection Status Functions)

연결 상태 함수는 기존 데이터베이스 연결 객체의 상태를 조회하는 함수들임.

#### 32.2.1 연결 파라미터 함수

##### PQdb
연결의 데이터베이스 이름을 반환함.
```c
char *PQdb(const PGconn *conn);
```

##### PQuser
연결의 사용자 이름을 반환함.
```c
char *PQuser(const PGconn *conn);
```

##### PQpass
연결의 비밀번호를 반환함.
```c
char *PQpass(const PGconn *conn);
```

##### PQhost
활성 연결의 서버 호스트명을 반환함.
```c
char *PQhost(const PGconn *conn);
```

##### PQhostaddr
활성 연결의 서버 IP 주소를 반환함.
```c
char *PQhostaddr(const PGconn *conn);
```

##### PQport
활성 연결의 포트를 반환함.
```c
char *PQport(const PGconn *conn);
```

##### PQoptions
연결 요청에서 전달된 명령줄 옵션을 반환함.
```c
char *PQoptions(const PGconn *conn);
```

#### 32.2.2 연결 상태 조회

##### PQstatus
연결의 상태를 반환함.
```c
ConnStatusType PQstatus(const PGconn *conn);
```

반환 값:
- `CONNECTION_OK`: 정상 연결
- `CONNECTION_BAD`: 연결 시도 실패 또는 통신 오류
- 비동기 연결 절차를 위한 기타 값들

##### PQtransactionStatus
서버의 현재 트랜잭션 상태를 반환함.
```c
PGTransactionStatusType PQtransactionStatus(const PGconn *conn);
```

반환 값:
- `PQTRANS_IDLE`: 현재 유휴 상태
- `PQTRANS_ACTIVE`: 명령 진행 중
- `PQTRANS_INTRANS`: 유효한 트랜잭션 블록에서 유휴 상태
- `PQTRANS_INERROR`: 실패한 트랜잭션 블록에서 유휴 상태
- `PQTRANS_UNKNOWN`: 연결이 잘못됨

##### PQparameterStatus
서버의 현재 파라미터 설정을 조회함.
```c
const char *PQparameterStatus(const PGconn *conn, const char *paramName);
```

조회 가능한 파라미터: `application_name`, `client_encoding`, `DateStyle`, `server_encoding`, `server_version`, `TimeZone`, `is_superuser` 등

##### PQprotocolVersion
프론트엔드/백엔드 프로토콜 메이저 버전을 반환함.
```c
int PQprotocolVersion(const PGconn *conn);
```

##### PQserverVersion
서버 버전을 나타내는 정수를 반환함.
```c
int PQserverVersion(const PGconn *conn);
```

버전 계산: major_version × 10000 + minor_version
- 예: 버전 10.1 → `100001`, 버전 11.0 → `110000`

##### PQerrorMessage
가장 최근에 생성된 오류 메시지를 반환함.
```c
char *PQerrorMessage(const PGconn *conn);
```

#### 32.2.3 연결 상태 함수

##### PQsocket
연결 소켓의 파일 디스크립터 번호를 반환함.
```c
int PQsocket(const PGconn *conn);
```
- 유효한 디스크립터는 0 이상, 열린 연결이 없으면 -1 반환

##### PQbackendPID
연결을 처리하는 백엔드의 프로세스 ID를 반환함.
```c
int PQbackendPID(const PGconn *conn);
```

##### PQconnectionNeedsPassword
인증에 비밀번호가 필요했으나 제공되지 않았는지 확인함.
```c
int PQconnectionNeedsPassword(const PGconn *conn);
```

##### PQconnectionUsedPassword
인증에 비밀번호가 사용되었는지 확인함.
```c
int PQconnectionUsedPassword(const PGconn *conn);
```

#### 32.2.4 SSL 관련 함수

##### PQsslInUse
연결에 SSL이 사용 중인지 확인함.
```c
int PQsslInUse(const PGconn *conn);
```
- SSL 사용 시 1, 미사용 시 0 반환

##### PQsslAttribute
연결의 SSL 관련 정보를 반환함.
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

### 32.3 명령 실행 함수 (Command Execution Functions)

#### 32.3.1 주요 함수

##### PQexec

명령을 서버에 제출하고 결과를 기다림.

```c
PGresult *PQexec(PGconn *conn, const char *command);
```

파라미터:
- `conn`: 연결 객체
- `command`: SQL 명령 문자열 (세미콜론으로 구분된 여러 SQL 명령 포함 가능)

주요 특성:
- 명시적인 BEGIN/COMMIT가 없으면 단일 트랜잭션으로 여러 SQL 명령 처리
- 마지막 명령의 결과를 나타내는 `PGresult` 반환
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

##### PQexecParams

별도로 지정된 파라미터로 명령을 제출하며, 바이너리 결과 형식을 지원함.

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
- `paramTypes[]`: 데이터 타입을 지정하는 OID 배열 (서버가 추론하도록 NULL 가능)
- `paramValues[]`: 파라미터 값 배열 (NULL 포인터 = NULL 값)
- `paramLengths[]`: 바이너리 파라미터의 바이트 길이 (텍스트 형식 및 NULL 값에서는 무시)
- `paramFormats[]`: 각 파라미터의 형식 표시기 (0 = 텍스트, 1 = 바이너리)
- `resultFormat`: 결과 형식 (0 = 텍스트, 1 = 바이너리)

주요 장점:
- 파라미터 값이 SQL 명령과 분리됨 (SQL 인젝션 방지)
- 수동 인용 및 이스케이프 불필요
- 호출당 SQL 명령 하나만 지원 (기본 프로토콜의 제한)
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

##### PQprepare

반복 실행을 위한 준비된 문장(prepared statement)을 생성함.

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
- `paramTypes[]`: 파라미터 타입의 OID 배열 (서버가 추론하도록 NULL 가능)

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

##### PQexecPrepared

이전에 준비된 문장을 주어진 파라미터로 실행함.

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

#### 32.3.2 결과 상태 상수

명령 실행 후 결과 상태를 확인함:

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

#### 32.3.3 쿼리 결과 정보 조회

##### PQntuples
결과의 행(튜플) 수를 반환함.
```c
int PQntuples(const PGresult *res);
```

##### PQnfields
결과의 열(필드) 수를 반환함.
```c
int PQnfields(const PGresult *res);
```

##### PQfname
주어진 열 번호와 연관된 열 이름을 반환함.
```c
char *PQfname(const PGresult *res, int field_num);
```

##### PQfnumber
주어진 열 이름과 연관된 열 번호를 반환함.
```c
int PQfnumber(const PGresult *res, const char *field_name);
```

##### PQftype
주어진 열 번호와 연관된 데이터 타입을 반환함.
```c
Oid PQftype(const PGresult *res, int field_num);
```

##### PQfsize
주어진 열 번호와 연관된 열의 내부 저장 크기를 반환함.
```c
int PQfsize(const PGresult *res, int field_num);
```

##### PQgetvalue
단일 필드 값을 반환함.
```c
char *PQgetvalue(const PGresult *res, int tup_num, int field_num);
```

##### PQgetisnull
필드가 NULL 값인지 테스트함.
```c
int PQgetisnull(const PGresult *res, int tup_num, int field_num);
```

##### PQgetlength
필드 값의 실제 길이를 반환함.
```c
int PQgetlength(const PGresult *res, int tup_num, int field_num);
```

#### 32.3.4 메모리 관리

```c
void PQclear(PGresult *res);
```

중요: 결과는 명시적으로 해제 필요. 새 명령이 실행되거나 연결이 닫혀도 자동으로 해제되지 않음.

#### 32.3.5 보안 모범 사례

SQL 인젝션 공격을 방지하기 위해 항상 `PQexecParams()` 또는 `PQexecPrepared()`를 파라미터화된 쿼리와 함께 사용할 것:

```c
// 안전하지 않음 - SQL 인젝션에 취약
PQexec(conn, "SELECT * FROM users WHERE name = 'John';");

// 안전함 - 파라미터가 명령과 분리됨
const char *param = "John";
PQexecParams(conn, "SELECT * FROM users WHERE name = $1;",
             1, NULL, &param, NULL, NULL, 0);
```

---

### 32.4 비동기 명령 처리 (Asynchronous Command Processing)

PostgreSQL libpq의 비동기 명령 처리를 통해 애플리케이션은 결과를 기다리는 동안 차단되지 않고 명령을 제출 가능. 이는 다음과 같은 경우에 유용함:

- 데이터베이스 작업이 진행되는 동안 응답성 유지 (예: UI 업데이트)
- 명령을 더 쉽게 취소
- 여러 SQL 명령을 개별적으로 처리
- 대용량 결과 집합을 점진적으로 처리
- 서버로의 출력 차단 방지

#### 32.4.1 핵심 함수

##### PQsendQuery

결과를 기다리지 않고 명령을 서버에 제출함.

```c
int PQsendQuery(PGconn *conn, const char *command);
```

- 반환 값: 성공적으로 전송되면 1, 실패하면 0
- 오류 정보: 실패 시 `PQerrorMessage()` 사용
- 사용법: NULL이 반환될 때까지 `PQgetResult()`를 반복 호출해야 함
- 제한: `PQgetResult()`가 NULL을 반환하기 전까지 재호출 불가

##### PQsendQueryParams

결과를 기다리지 않고 별도의 파라미터와 함께 명령을 제출함.

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

##### PQsendPrepare

비동기적으로 준비된 문장 생성 요청을 보냄.

```c
int PQsendPrepare(PGconn *conn,
                  const char *stmtName,
                  const char *query,
                  int nParams,
                  const Oid *paramTypes);
```

##### PQsendQueryPrepared

비동기적으로 준비된 문장을 실행함.

```c
int PQsendQueryPrepared(PGconn *conn,
                        const char *stmtName,
                        int nParams,
                        const char * const *paramValues,
                        const int *paramLengths,
                        const int *paramFormats,
                        int resultFormat);
```

##### PQgetResult

이전 send 명령으로부터 다음 결과를 조회함.

```c
PGresult *PQgetResult(PGconn *conn);
```

- 반환 값: 다음 `PGresult` 또는 명령 완료 시 NULL
- 사용법: NULL이 반환될 때까지 반복 호출
- 차단: 명령이 활성화되고 응답 데이터가 아직 읽히지 않은 경우에만 차단
- 메모리: 각 결과를 `PQclear()`로 해제

#### 32.4.2 비차단 I/O 함수

##### PQconsumeInput

서버에서 사용 가능한 데이터를 읽음.

```c
int PQconsumeInput(PGconn *conn);
```

- 반환 값: 성공 시 1, 오류 시 0
- 동작: 수신 데이터를 버퍼링; 실제로 데이터가 도착했는지 여부는 표시하지 않음
- 사용 사례: `PQisBusy()` 또는 `PQnotifies()` 전에 호출

##### PQisBusy

명령이 아직 처리 중인지 확인함.

```c
int PQisBusy(PGconn *conn);
```

- 반환 값: 바쁨(차단됨)이면 1, 준비되면 0
- 요구 사항: 먼저 `PQconsumeInput()` 호출 필요

##### PQsetnonblocking

연결을 비차단 모드로 설정함.

```c
int PQsetnonblocking(PGconn *conn, int arg);
```

- 파라미터: `arg=1`은 비차단, `arg=0`은 차단
- 반환 값: 성공 시 0, 오류 시 -1
- 중요: `PQexec()`는 비차단 모드를 무시하며 항상 차단 방식으로 동작

##### PQisnonblocking

현재 차단 상태를 반환함.

```c
int PQisnonblocking(const PGconn *conn);
```

##### PQflush

대기 중인 출력 데이터를 서버로 플러시함.

```c
int PQflush(PGconn *conn);
```

- 반환 값: 모두 플러시되면 0, 오류 시 -1, 전송 대기 중인 데이터가 남아있으면 1

#### 32.4.3 일반적인 애플리케이션 패턴

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

### 32.5 파이프라인 모드 (Pipeline Mode)

파이프라인 모드를 사용하면 이전 결과를 기다리지 않고 여러 쿼리를 연속으로 전송할 수 있어, 하나의 네트워크 왕복에서 여러 쿼리와 결과를 묶어 처리함으로써 상당한 성능 향상을 얻을 수 있음.

#### 32.5.1 파이프라인 모드가 도움되는 경우

- 높은 네트워크 지연 시간: 왕복 시간이 300ms인 서버에서 100개 문장 작업은 파이프라이닝 없이 30초가 걸릴 수 있지만, 파이프라이닝으로 0.3초가 될 수 있음
- 빠른 소규모 작업: 집합 연산이나 `COPY`로 배치 처리할 수 없는 많은 `INSERT`, `UPDATE`, `DELETE` 작업

#### 32.5.2 모드 진입/종료

##### PQenterPipelineMode

```c
int PQenterPipelineMode(PGconn *conn);
```

- 반환 값: 성공 시 1, 연결이 유휴 상태가 아니면 0
- 제한: 유휴 상태의 연결에서만 호출 가능

##### PQexitPipelineMode

```c
int PQexitPipelineMode(PGconn *conn);
```

- 반환 값: 성공 시 1, 결과가 아직 대기 중이면 0

##### PQpipelineStatus

```c
PGpipelineStatus PQpipelineStatus(const PGconn *conn);
```

반환 값:
- `PQ_PIPELINE_ON`: 현재 파이프라인 모드
- `PQ_PIPELINE_OFF`: 파이프라인 모드 아님
- `PQ_PIPELINE_ABORTED`: 오류가 있는 파이프라인 모드

#### 32.5.3 파이프라인 모드에서 허용되는 함수

- `PQsendQueryParams()` - 파라미터와 함께 쿼리 전송
- `PQsendQueryPrepared()` - 준비된 쿼리 전송
- `PQsendPrepare()` - PREPARE 문장 전송
- `PQsendDescribePrepared()` - 준비된 문장 DESCRIBE 전송
- `PQsendDescribePortal()` - 포털 DESCRIBE 전송

#### 32.5.4 파이프라인 모드에서 금지된 함수

- `PQexec()`, `PQexecParams()`, `PQprepare()`, `PQexecPrepared()`
- `PQsendQuery()` (단순 쿼리 프로토콜 사용)
- 단일 문자열에 여러 SQL 명령
- `COPY` 작업

#### 32.5.5 동기화 및 플러시

##### PQpipelineSync

```c
int PQpipelineSync(PGconn *conn);
```

- 반환 값: 성공 시 1, 파이프라인 모드가 아니거나 전송 실패 시 0
- 효과: 동기화 메시지를 전송하고 전송 버퍼를 플러시
- 목적: 암시적 트랜잭션 구분자 및 오류 복구 지점 설정

##### PQsendFlushRequest

```c
int PQsendFlushRequest(PGconn *conn);
```

- 목적: 동기화 지점을 설정하지 않고 서버가 출력 버퍼를 플러시하도록 요청

#### 32.5.6 결과 처리 패턴

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

### 32.6 결과를 청크로 조회 (Retrieving Query Results in Chunks)

대용량 결과 집합의 경우 `PQsetSingleRowMode`를 사용하면 결과를 한 번에 한 행씩 조회 가능.

```c
int PQsetSingleRowMode(PGconn *conn);
```

이 함수는 `PQsendQuery` 또는 유사한 함수를 호출한 직후, `PQgetResult` 호출 전에 사용 필요.

---

### 32.7 진행 중인 쿼리 취소 (Canceling Queries in Progress)

#### 32.7.1 취소 요청 전송 함수

##### PQcancelCreate

취소 요청 전송을 위한 연결을 준비함.

```c
PGcancelConn *PQcancelCreate(PGconn *conn);
```

- 쿼리 취소에 재사용 가능한 `PGcancelConn` 객체 생성
- 완료 시 메모리 해제를 위해 `PQcancelFinish()` 호출 필요
- 원래 연결의 SSL/GSS 암호화 요구 사항을 따름
- 스레드 안전

##### PQcancelBlocking

동기적으로(차단) 취소 요청을 보냄.

```c
int PQcancelBlocking(PGcancelConn *cancelConn);
```

반환 값:
- 취소 요청이 성공적으로 전송되면 1
- 실패하면 0 (자세한 내용은 `PQcancelErrorMessage()` 사용)

참고: 취소 요청 전송 성공이 쿼리 종료를 보장하지 않음; 서버가 이미 처리를 완료했을 수 있음

##### PQcancelStart / PQcancelPoll

비동기적으로(비차단) 취소 요청을 보냄.

```c
int PQcancelStart(PGcancelConn *cancelConn);
PostgresPollingStatusType PQcancelPoll(PGcancelConn *cancelConn);
```

##### PQcancelFinish

취소 연결을 닫고 메모리를 해제함.

```c
void PQcancelFinish(PGcancelConn *cancelConn);
```

취소 시도가 실패하거나 중단된 경우에도 반드시 호출해야 메모리 누수를 방지 가능.

#### 32.7.2 권장하지 않는 함수 (Obsolete Functions)

##### PQgetCancel (권장하지 않음)
```c
PGcancel *PQgetCancel(PGconn *conn);
```
취소 객체를 생성함. 권장하지 않음. 대신 `PQcancelCreate()` 사용.

##### PQcancel (권장하지 않음)
```c
int PQcancel(PGcancel *cancel, char *errbuf, int errbufsize);
```
권장하지 않음. 대신 `PQcancelBlocking()` 사용.
- 유일한 장점: 시그널 핸들러에서 안전하게 호출 가능
- 보안 취약: 원래 연결이 암호화를 요구하더라도 암호화 없이 취소 요청 전송

#### 32.7.3 보안 고려 사항

최신 함수 (`PQcancelCreate`, `PQcancelBlocking`, `PQcancelStart/Poll`):
- 암호화된 취소 요청 지원 (SSL/GSS)
- 스레드 안전
- 모든 새 코드에 권장

권장하지 않는 함수 (`PQcancel`, `PQrequestCancel`):
- 암호화되지 않은 취소 요청 전송
- 보안 취약, 강력히 권장하지 않음

---

### 32.8 Fast-Path 인터페이스

Fast-Path 인터페이스는 서버로 간단한 함수 호출을 보내는 오래된 메커니즘임. 현재는 대부분의 목적에 준비된 문장이 선호됨.

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

### 32.9 비동기 알림 (Asynchronous Notification)

PostgreSQL은 `LISTEN` 및 `NOTIFY` 명령을 통해 비동기 알림을 제공하여, 클라이언트가 폴링 없이 알림 채널을 구독하고 메시지를 수신할 수 있게 함.

#### 32.9.1 작동 방식

- 클라이언트가 `LISTEN` 명령을 사용하여 알림 채널에 관심을 등록
- 클라이언트는 `UNLISTEN`으로 수신을 중지할 수 있음
- 모든 세션이 선택적 페이로드 데이터와 함께 `NOTIFY` 메시지를 브로드캐스트할 수 있음
- 모든 수신 중인 세션이 비동기적으로 알림을 받음

#### 32.9.2 함수 시그니처

```c
PGnotify *PQnotifies(PGconn *conn);
```

#### 32.9.3 데이터 구조

```c
typedef struct pgNotify
{
    char *relname;              /* 알림 채널 이름 */
    int  be_pid;                /* 알림을 보내는 서버 프로세스의 프로세스 ID */
    char *extra;                /* 알림 페이로드 문자열 */
} PGnotify;
```

#### 32.9.4 사용 패턴

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

항상 다음 호출 후 알림을 확인할 것:
- `PQgetResult()` 호출
- `PQexec()` 호출
- `PQconsumeInput()` 호출

---

### 32.10 COPY 명령 관련 함수

`COPY` 명령을 사용하면 libpq를 통해 네트워크 연결에서 읽거나 쓸 수 있음.

#### 32.10.1 COPY 데이터 전송 함수

##### PQputCopyData

`COPY_IN` 상태에서 서버로 데이터를 전송함.

```c
int PQputCopyData(PGconn *conn,
                  const char *buffer,
                  int nbytes);
```

반환 값:
- `1`: 데이터가 성공적으로 큐에 추가됨
- `0`: 데이터가 큐에 추가되지 않음 (비차단 모드, 버퍼 가득 참)
- `-1`: 오류 발생 (자세한 내용은 `PQerrorMessage()` 사용)

##### PQputCopyEnd

`COPY_IN` 작업을 종료함.

```c
int PQputCopyEnd(PGconn *conn,
                 const char *errormsg);
```

파라미터:
- `errormsg`: 성공적 완료를 위해 `NULL`, 또는 COPY를 실패시킬 오류 메시지 문자열

#### 32.10.2 COPY 데이터 수신 함수

##### PQgetCopyData

`COPY_OUT` 상태에서 서버로부터 데이터를 수신함.

```c
int PQgetCopyData(PGconn *conn,
                  char **buffer,
                  int async);
```

파라미터:
- `buffer`: 포인터-to-포인터(`char **`); 함수가 할당한 메모리 주소를 여기에 저장
- `async`: 비차단 모드는 0이 아닌 값, 차단 모드는 0

반환 값:
- `> 0`: 반환된 데이터 행의 바이트 수
- `0`: COPY 진행 중, 완전한 행 없음 (비동기 모드에서만)
- `-1`: COPY 완료
- `-2`: 오류 발생

#### 32.10.3 일반적인 사용 패턴

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

### 32.11 제어 함수 (Control Functions)

제어 함수는 libpq 동작의 다양한 세부 사항을 관리함.

#### 32.11.1 클라이언트 인코딩 함수

##### PQclientEncoding
클라이언트 인코딩 ID를 반환함.
```c
int PQclientEncoding(const PGconn *conn);
```

##### PQsetClientEncoding
연결의 클라이언트 인코딩을 설정함.
```c
int PQsetClientEncoding(PGconn *conn, const char *encoding);
```
- 성공 시 0, 실패 시 -1 반환

#### 32.11.2 오류 메시지 제어 함수

##### PQsetErrorVerbosity
`PQerrorMessage()` 및 `PQresultErrorMessage()`의 오류 메시지 상세도를 결정함.

```c
typedef enum {
    PQERRORS_TERSE,      // 심각도, 기본 텍스트, 위치만
    PQERRORS_DEFAULT,    // 위 + 세부 정보, 힌트, 컨텍스트 (기본값)
    PQERRORS_VERBOSE,    // 사용 가능한 모든 필드
    PQERRORS_SQLSTATE    // 심각도와 SQLSTATE 코드만
} PGVerbosity;

PGVerbosity PQsetErrorVerbosity(PGconn *conn, PGVerbosity verbosity);
```

##### PQsetErrorContextVisibility
`CONTEXT` 필드가 오류 메시지에 나타나는지 제어함.

```c
typedef enum {
    PQSHOW_CONTEXT_NEVER,    // CONTEXT 포함 안 함
    PQSHOW_CONTEXT_ERRORS,   // 오류에만 포함 (기본값)
    PQSHOW_CONTEXT_ALWAYS    // 사용 가능하면 항상 포함
} PGContextVisibility;

PGContextVisibility PQsetErrorContextVisibility(PGconn *conn,
                                               PGContextVisibility show_context);
```

#### 32.11.3 추적 함수

##### PQtrace
클라이언트/서버 통신 추적을 파일 스트림으로 활성화함.
```c
void PQtrace(PGconn *conn, FILE *stream);
```

##### PQsetTraceFlags
추적 동작을 제어함. `PQtrace()` 후에 호출 필요.
```c
void PQsetTraceFlags(PGconn *conn, int flags);
```

플래그:
- `PQTRACE_SUPPRESS_TIMESTAMPS` - 타임스탬프 생략
- `PQTRACE_REGRESS_MODE` - 테스트를 위해 객체 OID 등의 필드를 수정

##### PQuntrace
추적을 비활성화함.
```c
void PQuntrace(PGconn *conn);
```

---

### 32.12 기타 함수 (Miscellaneous Functions)

#### PQfreemem
libpq가 할당한 메모리를 해제함.
```c
void PQfreemem(void *ptr);
```

Windows에서 중요: DLL과 애플리케이션 간 메모리 할당 호환성 때문에 `free()` 대신 반드시 이 함수를 사용 필요.

#### PQconninfoFree
연결 함수가 할당한 데이터 구조를 해제함.
```c
void PQconninfoFree(PQconninfoOption *connOptions);
```

#### PQencryptPasswordConn
PostgreSQL 비밀번호의 암호화된 형식을 준비함.
```c
char *PQencryptPasswordConn(PGconn *conn, const char *passwd,
                            const char *user, const char *algorithm);
```

파라미터:
- `passwd`: 평문 비밀번호
- `user`: 사용자의 SQL 이름
- `algorithm`: 암호화 방법 (`md5`, `scram-sha-256`, `on`, 또는 `off`)

#### PQlibVersion
libpq 버전 번호를 반환함.
```c
int PQlibVersion(void);
```

버전 계산: (major × 10000) + minor
- 버전 10.1 → `100001`
- 버전 11.0 → `110000`

---

### 32.13 알림 처리 (Notice Processing)

PostgreSQL 서버가 생성한 알림 및 경고 메시지는 쿼리 실패가 아닌 알림 처리 시스템을 통해 전달됨.

#### 32.13.1 2계층 아키텍처

1. 알림 수신기(Notice Receiver): 알림을 형식화하고 메시지 세부 정보 추출
2. 알림 프로세서(Notice Processor): 형식화된 메시지 텍스트 처리 (일반적으로 출력용)

#### 32.13.2 함수 시그니처

##### 알림 수신기

```c
typedef void (*PQnoticeReceiver) (void *arg, const PGresult *res);

PQnoticeReceiver
PQsetNoticeReceiver(PGconn *conn,
                    PQnoticeReceiver proc,
                    void *arg);
```

##### 알림 프로세서

```c
typedef void (*PQnoticeProcessor) (void *arg, const char *message);

PQnoticeProcessor
PQsetNoticeProcessor(PGconn *conn,
                     PQnoticeProcessor proc,
                     void *arg);
```

#### 32.13.3 기본 동작

기본 알림 프로세서 구현:

```c
static void
defaultNoticeProcessor(void *arg, const char *message)
{
    fprintf(stderr, "%s", message);
}
```

메시지를 `stderr`에 출력함.

---

### 32.14 환경 변수 (Environment Variables)

libpq에서 사용하는 다양한 환경 변수들:

#### 연결 파라미터

- `PGHOST`: 데이터베이스 서버 호스트명
- `PGHOSTADDR`: 데이터베이스 서버 IP 주소 (DNS 조회 회피)
- `PGPORT`: 데이터베이스 서버 포트 번호
- `PGDATABASE`: 데이터베이스 이름
- `PGUSER`: 데이터베이스 사용자 이름
- `PGPASSWORD`: 데이터베이스 비밀번호 (보안 위험 - 대신 비밀번호 파일 사용)
- `PGPASSFILE`: 비밀번호 파일 경로
- `PGSERVICE`: 연결용 서비스 이름
- `PGOPTIONS`: 서버로 전달할 명령줄 옵션
- `PGAPPNAME`: 연결용 애플리케이션 이름
- `PGCONNECT_TIMEOUT`: 연결 타임아웃(초)
- `PGCLIENTENCODING`: 클라이언트 문자 인코딩

#### SSL/TLS 파라미터

- `PGSSLMODE`: SSL 연결 모드
- `PGSSLCERT`: 클라이언트 인증서 파일
- `PGSSLKEY`: 클라이언트 키 파일
- `PGSSLROOTCERT`: 루트 인증서 파일
- `PGSSLCRL`: 인증서 폐기 목록

#### GSSAPI 파라미터

- `PGGSSENCMODE`: GSSAPI 암호화 모드
- `PGKRBSRVNAME`: Kerberos 서비스 이름

#### 사용 참고 사항

- 이 변수들은 `PQconnectdb()`, `PQsetdbLogin()`, `PQsetdb()` 함수에서 사용됨
- 직접 연결 파라미터가 환경 변수 값을 재정의
- 보안 경고: `PGPASSWORD` 사용을 피하고 대신 비밀번호 파일 사용

---

### 32.15 SSL 지원 (SSL Support)

PostgreSQL은 SSL/TLS 암호화 클라이언트-서버 통신을 기본 지원함.

#### 32.15.1 SSL 모드 옵션

`sslmode` 파라미터는 SSL 보호 수준을 제어함:

- `disable`
  - 도청 방지: 아니오
  - MITM 보호: 아니오
  - 사용 사례: 보안 불필요
- `allow`
  - 도청 방지: 가능
  - MITM 보호: 아니오
  - 사용 사례: 서버가 요구하면 선택적 암호화
- `prefer`
  - 도청 방지: 가능
  - MITM 보호: 아니오
  - 사용 사례: 암호화 선호하지만 필수 아님 (기본값 - 권장하지 않음)
- `require`
  - 도청 방지: 예
  - MITM 보호: 아니오
  - 사용 사례: 암호화 필수; 네트워크 라우팅 신뢰
- `verify-ca`
  - 도청 방지: 예
  - MITM 보호: CA에 따라 다름
  - 사용 사례: 서버의 CA 인증서 신뢰
- `verify-full`
  - 도청 방지: 예
  - MITM 보호: 예
  - 사용 사례: 권장 - 서버 신원 및 호스트명이 인증서와 일치하는지 확인

#### 32.15.2 서버 인증서 검증

##### 설정

루트 CA 인증서 위치:
- Linux/Unix: `~/.postgresql/root.crt`
- Windows: `%APPDATA%\postgresql\root.crt`

연결 파라미터로 위치 재정의:
```
sslrootcert=/path/to/root.crt
sslcrl=/path/to/root.crl
```

#### 32.15.3 클라이언트 인증서

##### 파일 위치

- Linux/Unix: `~/.postgresql/postgresql.crt` 및 `~/.postgresql/postgresql.key`
- Windows: `%APPDATA%\postgresql\postgresql.crt` 및 `%APPDATA%\postgresql\postgresql.key`

##### 요구 사항

- Unix에서 개인 키 파일 권한: `chmod 0600` (사용자 읽기/쓰기만) - 권장

#### 32.15.4 보안 권장 사항

1. 보안에 민감한 환경에서는 `verify-full` 모드 사용
2. 서버 검증을 위해 클라이언트에 루트 CA 인증서 배치
3. 서버가 인증을 요구할 때 클라이언트 인증서 사용
4. 개인 키에 적절한 파일 권한 설정 (Unix: `0600`)
5. `prefer` 기본값 피하기; 명시적으로 `verify-ca` 또는 `verify-full` 설정

---

### 32.16 예제 프로그램 (Example Programs)

#### 예제 1: 기본 연결 및 쿼리 실행

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

#### 예제 2: 파라미터화된 쿼리

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

#### 예제 3: 비동기 알림 처리

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

### 32.17 libpq 프로그램 빌드 (Building libpq Programs)

#### 컴파일

```bash
gcc -I$(pg_config --includedir) -c myprogram.c
```

#### 링크

```bash
gcc -L$(pg_config --libdir) -lpq myprogram.o -o myprogram
```

#### pkg-config 사용

```bash
gcc $(pkg-config --cflags --libs libpq) myprogram.c -o myprogram
```

---

### 요약

libpq는 PostgreSQL의 핵심 C 라이브러리로서 다음 기능을 제공함:

1. 데이터베이스 연결: `PQconnectdb`, `PQconnectdbParams` 등으로 연결 설정
2. 연결 상태 조회: `PQstatus`, `PQerrorMessage` 등으로 연결 상태 확인
3. 명령 실행: `PQexec`, `PQexecParams`, `PQprepare`, `PQexecPrepared` 등
4. 비동기 처리: `PQsendQuery`, `PQgetResult` 등으로 비차단 작업
5. 파이프라인 모드: 여러 쿼리를 효율적으로 배치 처리
6. 쿼리 취소: `PQcancelBlocking` 등으로 진행 중인 쿼리 취소
7. 알림 처리: `PQnotifies`로 비동기 알림 수신
8. SSL 지원: 암호화된 연결 설정

---

## Chapter 35: Large Objects (대용량 객체)

PostgreSQL은 사용자 데이터에 대한 스트림 방식의 접근을 제공하는 대용량 객체(Large Object) 기능을 지원함. 스트림 방식 접근은 전체 데이터 블록을 한 번에 처리하기 어려운 경우에 유용함.

### 목차

1. [소개 (Introduction)](#1-소개-introduction)
2. [구현 기능 (Implementation Features)](#2-구현-기능-implementation-features)
3. [클라이언트 인터페이스 (Client Interfaces)](#3-클라이언트-인터페이스-client-interfaces)
4. [서버측 함수 (Server-Side Functions)](#4-서버측-함수-server-side-functions)
5. [예제 프로그램 (Example Program)](#5-예제-프로그램-example-program)

---

### 1. 소개 (Introduction)

#### 대용량 객체란?

PostgreSQL의 대용량 객체(Large Object) 기능은 특별히 큰 데이터 값을 저장하고 조작하기 위한 기능임.

- 모든 대용량 객체는 `pg_largeobject` 시스템 테이블에 저장됨.
- 각 대용량 객체는 `pg_largeobject_metadata` 시스템 테이블에 메타데이터 항목을 가짐.
- 파일의 표준 작업(읽기, 쓰기, 탐색 등)과 유사한 읽기/쓰기 API를 사용하여 생성, 수정, 삭제 가능.

#### Large Objects vs TOAST 비교

- 최대 크기
  - Large Objects: 4 TB
  - TOAST: 1 GB
- 부분 읽기/업데이트
  - Large Objects: 효율적으로 지원
  - TOAST: 전체 값을 읽고 써야 함
- 접근 방식
  - Large Objects: 스트림 기반 API
  - TOAST: 일반 SQL 컬럼
- 저장 위치
  - Large Objects: pg_largeobject 테이블
  - TOAST: 자동으로 보조 저장 영역

> 참고: TOAST(The Oversized-Attribute Storage Technique) 저장 시스템의 도입으로 대용량 객체 기능이 부분적으로 구식화되었음. 하지만 매우 큰 데이터(1GB 초과)나 효율적인 부분 업데이트가 필요한 경우에는 여전히 대용량 객체가 유용함.

---

### 2. 구현 기능 (Implementation Features)

#### 기본 구조

대용량 객체는 "청크(chunks)"로 분할되어 데이터베이스의 행(rows)에 저장됨. B-tree 인덱스를 통해 임의 접근(random access) 읽기/쓰기 시 빠른 청크 검색이 보장됨.

#### 희소 할당 (Sparse Allocation)

대용량 객체의 청크는 연속적일 필요가 없음. 이는 Unix 파일 시스템의 "희소 할당(sparsely allocated)" 파일 동작과 동일함.

예시:
```sql
-- 새 대용량 객체를 열고 오프셋 1000000으로 이동한 후 몇 바이트를 쓰면,
-- 1000000 바이트 전체가 할당되지 않고 실제로 작성된 데이터 범위만 할당됩니다.
-- 읽기 작업에서 할당되지 않은 위치는 0(zero)으로 읽힙니다.
```

#### 접근 제어 및 권한 (PostgreSQL 9.0 이상)

대용량 객체는 소유자(owner)와 접근 권한(access permissions)을 가짐.

- 읽기: `SELECT` 권한
- 쓰기/자르기: `UPDATE` 권한
- 삭제/주석 추가/소유자 변경: 대용량 객체 소유자 또는 데이터베이스 슈퍼유저

권한 관리:
```sql
-- 권한 부여
GRANT SELECT ON LARGE OBJECT 12345 TO username;
GRANT UPDATE ON LARGE OBJECT 12345 TO username;

-- 권한 회수
REVOKE SELECT ON LARGE OBJECT 12345 FROM username;
```

> 호환성: `lo_compat_privileges` 런타임 파라미터를 통해 이전 버전과의 호환성을 조정 가능.

---

### 3. 클라이언트 인터페이스 (Client Interfaces)

PostgreSQL의 libpq 클라이언트 라이브러리는 대용량 객체 접근 기능을 제공함. 이 인터페이스는 Unix 파일 시스템 인터페이스(open, read, write, lseek 등)를 모델로 삼아 설계되었음.

#### 중요 사항

- 모든 대용량 객체 조작은 SQL 트랜잭션 블록 내에서 수행되어야 함.
- 헤더 파일: `libpq/libpq-fs.h`
- Pipeline 모드에서는 사용 불가.

#### 3.1 대용량 객체 생성 (Creating a Large Object)

##### lo_create

```c
Oid lo_create(PGconn *conn, Oid lobjId);
```

새로운 대용량 객체를 생성함.

매개변수:
- `conn`: 데이터베이스 연결
- `lobjId`: 할당할 OID (InvalidOid=0이면 시스템이 자동 할당)

반환값: 할당된 OID 또는 InvalidOid(0, 실패 시)

예시:
```c
Oid new_oid = lo_create(conn, InvalidOid);  // 자동 OID 할당
Oid specific_oid = lo_create(conn, 12345);  // 특정 OID 요청
```

##### lo_creat (구 함수)

```c
Oid lo_creat(PGconn *conn, int mode);
```

PostgreSQL 8.0 이전 버전과의 호환성을 위한 함수임.

매개변수:
- `mode`: `INV_READ`, `INV_WRITE`, 또는 둘 다 (`INV_READ | INV_WRITE`)

#### 3.2 대용량 객체 임포트 (Importing a Large Object)

##### lo_import

```c
Oid lo_import(PGconn *conn, const char *filename);
```

운영 체제 파일을 데이터베이스의 대용량 객체로 임포트함.

주의: 파일은 서버가 아닌 클라이언트 측에서 읽음.

예시:
```c
Oid imported_oid = lo_import(conn, "/path/to/local/file.bin");
if (imported_oid == InvalidOid) {
    fprintf(stderr, "Import failed: %s\n", PQerrorMessage(conn));
}
```

##### lo_import_with_oid (PostgreSQL 8.4+)

```c
Oid lo_import_with_oid(PGconn *conn, const char *filename, Oid lobjId);
```

특정 OID를 지정하여 파일을 임포트함.

#### 3.3 대용량 객체 내보내기 (Exporting a Large Object)

##### lo_export

```c
int lo_export(PGconn *conn, Oid lobjId, const char *filename);
```

대용량 객체를 운영 체제 파일로 내보냄.

주의: 파일은 서버가 아닌 클라이언트 측에 씀.

반환값: 1(성공), -1(실패)

예시:
```c
if (lo_export(conn, lobjId, "/path/to/output/file.bin") < 0) {
    fprintf(stderr, "Export failed: %s\n", PQerrorMessage(conn));
}
```

#### 3.4 기존 대용량 객체 열기 (Opening an Existing Large Object)

##### lo_open

```c
int lo_open(PGconn *conn, Oid lobjId, int mode);
```

읽기 또는 쓰기용으로 기존 대용량 객체를 염.

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

#### 3.5 대용량 객체에 데이터 쓰기 (Writing Data to a Large Object)

##### lo_write

```c
int lo_write(PGconn *conn, int fd, const char *buf, size_t len);
```

대용량 객체 디스크립터에 데이터를 씀.

반환값: 실제로 쓴 바이트 수(보통 len과 동일) 또는 -1(실패)

주의: `len`은 `INT_MAX` 이하여야 함.

예시:
```c
char data[] = "Hello, Large Object!";
int nbytes = lo_write(conn, fd, data, strlen(data));
if (nbytes < 0) {
    fprintf(stderr, "Write failed: %s\n", PQerrorMessage(conn));
}
```

#### 3.6 대용량 객체에서 데이터 읽기 (Reading Data from a Large Object)

##### lo_read

```c
int lo_read(PGconn *conn, int fd, char *buf, size_t len);
```

대용량 객체 디스크립터에서 데이터를 읽음.

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

#### 3.7 대용량 객체 내에서 탐색 (Seeking in a Large Object)

##### lo_lseek

```c
int lo_lseek(PGconn *conn, int fd, int offset, int whence);
```

읽기/쓰기 위치를 변경함.

매개변수:
- `whence`: `SEEK_SET`(시작), `SEEK_CUR`(현재 위치), `SEEK_END`(끝)

반환값: 새 위치 또는 -1(실패)

제한: 2GB 이상의 위치를 처리 불가.

예시:
```c
// 시작 위치로 이동
lo_lseek(conn, fd, 0, SEEK_SET);

// 현재 위치에서 100바이트 앞으로
lo_lseek(conn, fd, 100, SEEK_CUR);

// 끝에서 50바이트 앞으로
lo_lseek(conn, fd, -50, SEEK_END);
```

##### lo_lseek64 (PostgreSQL 9.3+)

```c
int64_t lo_lseek64(PGconn *conn, int fd, int64_t offset, int whence);
```

2GB 이상의 대용량 객체를 처리할 수 있는 64비트 버전임.

#### 3.8 현재 위치 조회 (Obtaining the Seek Position)

##### lo_tell

```c
int lo_tell(PGconn *conn, int fd);
```

반환값: 현재 위치 또는 -1(실패)

##### lo_tell64 (PostgreSQL 9.3+)

```c
int64_t lo_tell64(PGconn *conn, int fd);
```

2GB 이상의 위치를 처리할 수 있는 64비트 버전임.

#### 3.9 대용량 객체 자르기 (Truncating a Large Object)

##### lo_truncate

```c
int lo_truncate(PGconn *conn, int fd, size_t len);
```

대용량 객체를 지정된 길이로 자르거나 확장함.

동작:
- `len`이 현재 크기보다 작으면: 해당 길이로 자름
- `len`이 현재 크기보다 크면: null 바이트(0)로 확장

반환값: 0(성공) 또는 -1(실패)

주의: 읽기/쓰기 위치는 변경되지 않음.

예시:
```c
// 대용량 객체를 1024 바이트로 자르기
if (lo_truncate(conn, fd, 1024) < 0) {
    fprintf(stderr, "Truncate failed: %s\n", PQerrorMessage(conn));
}
```

##### lo_truncate64 (PostgreSQL 9.3+)

```c
int lo_truncate64(PGconn *conn, int fd, int64_t len);
```

2GB 이상의 크기를 처리할 수 있는 64비트 버전임.

#### 3.10 대용량 객체 디스크립터 닫기 (Closing a Large Object Descriptor)

##### lo_close

```c
int lo_close(PGconn *conn, int fd);
```

반환값: 0(성공) 또는 -1(실패)

주의: 트랜잭션 종료 시 열린 디스크립터는 자동으로 닫힘.

예시:
```c
if (lo_close(conn, fd) < 0) {
    fprintf(stderr, "Close failed: %s\n", PQerrorMessage(conn));
}
```

#### 3.11 대용량 객체 삭제 (Removing a Large Object)

##### lo_unlink

```c
int lo_unlink(PGconn *conn, Oid lobjId);
```

데이터베이스에서 대용량 객체를 제거함.

반환값: 1(성공) 또는 -1(실패)

예시:
```c
if (lo_unlink(conn, lobjId) < 0) {
    fprintf(stderr, "Delete failed: %s\n", PQerrorMessage(conn));
}
```

#### 오류 처리

오류 발생 시 함수는 유효하지 않은 값(보통 0 또는 -1)을 반환함. 오류 메시지는 연결 객체에 저장되며 `PQerrorMessage()`로 조회 가능.

```c
if (result < 0) {
    fprintf(stderr, "Error: %s\n", PQerrorMessage(conn));
}
```

---

### 4. 서버측 함수 (Server-Side Functions)

SQL 쿼리에서 직접 사용할 수 있는 서버측 함수들임.

#### 4.1 SQL 지향 Large Object 함수

##### lo_from_bytea - bytea 데이터로 대용량 객체 생성

```sql
lo_from_bytea(loid oid, data bytea) -> oid
```

대용량 객체를 생성하고 데이터를 저장함.

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

##### lo_put - 대용량 객체에 데이터 쓰기

```sql
lo_put(loid oid, offset bigint, data bytea) -> void
```

지정된 오프셋부터 데이터를 씀. 필요시 대용량 객체가 자동으로 확대됨.

예시:
```sql
-- 오프셋 1부터 데이터 쓰기
SELECT lo_put(24528, 1, '\xaa'::bytea);

-- 오프셋 100부터 텍스트 데이터 쓰기
SELECT lo_put(24528, 100, 'New data'::bytea);
```

##### lo_get - 대용량 객체에서 데이터 읽기

```sql
lo_get(loid oid [, offset bigint, length integer]) -> bytea
```

대용량 객체의 전체 또는 일부 내용을 추출함.

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

#### 4.2 기본 Large Object 함수

##### lo_creat / lo_create - 대용량 객체 생성

```sql
-- 새로운 빈 대용량 객체 생성 (OID 자동 할당)
SELECT lo_creat(-1);

-- 특정 OID로 대용량 객체 생성
SELECT lo_create(43213);
```

##### lo_unlink - 대용량 객체 삭제

```sql
-- OID 173454인 대용량 객체 삭제
SELECT lo_unlink(173454);
```

##### lo_import - 서버 파일을 대용량 객체로 임포트

```sql
-- 서버의 파일을 대용량 객체로 임포트
SELECT lo_import('/etc/motd');

-- 테이블에 직접 저장하는 예시
INSERT INTO image (name, raster)
VALUES ('beautiful image', lo_import('/etc/motd'));
```

##### lo_export - 대용량 객체를 서버 파일로 내보내기

```sql
-- 대용량 객체를 서버의 파일로 내보내기
SELECT lo_export(image.raster, '/tmp/motd')
FROM image
WHERE name = 'beautiful image';
```

#### 4.3 loread / lowrite - 서버측 읽기/쓰기

서버측에서는 `lo_read`와 `lo_write` 대신 언더스코어 없는 `loread`, `lowrite`를 사용함.

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

#### 4.4 보안 주의사항

lo_import / lo_export 보안 경고:
- 기본적으로 슈퍼유저만 사용 가능함.
- 이 함수들은 데이터베이스 소유자 권한으로 서버 파일 시스템에 접근함.
- 비슈퍼유저에게 권한을 부여할 때는 매우 신중하게 검토 필요.
- 악의적인 사용자가 이 권한을 이용해 슈퍼유저 권한을 획득 가능.

---

### 5. 예제 프로그램 (Example Program)

다음은 libpq로 대용량 객체 인터페이스를 사용하는 C 예제 프로그램임. PostgreSQL 소스 배포의 `src/test/examples/testlo.c`에서도 확인 가능.

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
main(int argc, char **argv)
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

#### 프로그램 컴파일 및 실행

```bash
# 컴파일
gcc -o testlo testlo.c -I$(pg_config --includedir) -L$(pg_config --libdir) -lpq

# 실행
./testlo mydb input.txt output.txt
```

#### 출력 예시

```
파일 "input.txt" 임포트 중...
    대용량 객체 24528로 저장됨.
대용량 객체의 바이트 1000-2000 읽기
>>> [해당 범위의 내용 출력]
대용량 객체의 바이트 1000-2000을 'X'로 덮어쓰기

대용량 객체를 파일 "output.txt"로 내보내기...
```

---

### 요약

- 생성
  - 클라이언트 함수 (libpq): `lo_create()`, `lo_creat()`
  - 서버측 함수 (SQL): `lo_create()`, `lo_creat()`, `lo_from_bytea()`
- 임포트
  - 클라이언트 함수 (libpq): `lo_import()`
  - 서버측 함수 (SQL): `lo_import()`
- 내보내기
  - 클라이언트 함수 (libpq): `lo_export()`
  - 서버측 함수 (SQL): `lo_export()`
- 열기
  - 클라이언트 함수 (libpq): `lo_open()`
  - 서버측 함수 (SQL): `lo_open()`
- 읽기
  - 클라이언트 함수 (libpq): `lo_read()`
  - 서버측 함수 (SQL): `loread()`, `lo_get()`
- 쓰기
  - 클라이언트 함수 (libpq): `lo_write()`
  - 서버측 함수 (SQL): `lowrite()`, `lo_put()`
- 탐색
  - 클라이언트 함수 (libpq): `lo_lseek()`, `lo_lseek64()`
  - 서버측 함수 (SQL): `lo_lseek()`, `lo_lseek64()`
- 위치 조회
  - 클라이언트 함수 (libpq): `lo_tell()`, `lo_tell64()`
  - 서버측 함수 (SQL): `lo_tell()`, `lo_tell64()`
- 자르기
  - 클라이언트 함수 (libpq): `lo_truncate()`, `lo_truncate64()`
  - 서버측 함수 (SQL): `lo_truncate()`, `lo_truncate64()`
- 닫기
  - 클라이언트 함수 (libpq): `lo_close()`
  - 서버측 함수 (SQL): `lo_close()`
- 삭제
  - 클라이언트 함수 (libpq): `lo_unlink()`
  - 서버측 함수 (SQL): `lo_unlink()`

---

### 참고 자료

- [PostgreSQL 공식 문서 - Large Objects](https://www.postgresql.org/docs/current/largeobjects.html)
- [libpq - C 라이브러리](https://www.postgresql.org/docs/current/libpq.html)
- [TOAST 저장 시스템](https://www.postgresql.org/docs/current/storage-toast.html)

---

## Chapter 36: ECPG - C 언어 내장 SQL (Embedded SQL in C)

### 목차

1. [개요](#1-개요)
2. [ECPG 개념](#2-ecpg-개념)
3. [데이터베이스 연결 관리](#3-데이터베이스-연결-관리)
4. [SQL 명령 실행](#4-sql-명령-실행)
5. [호스트 변수](#5-호스트-변수)
6. [동적 SQL](#6-동적-sql)
7. [오류 처리](#7-오류-처리)
8. [전처리기 지시문](#8-전처리기-지시문)
9. [처리 및 컴파일](#9-처리-및-컴파일)

---

### 1. 개요

ECPG(Embedded SQL in C)는 PostgreSQL의 C 언어(및 제한적으로 C++) 내장 SQL 패키지임. Linus Tolke와 Michael Meskes가 개발했으며, SQL 표준을 따라 개발자가 C 코드 내에 SQL을 직접 삽입할 수 있게 함.

#### 주요 특징

- 표준화된 SQL 인터페이스: C 언어에 SQL을 내장
- 트랜잭션 관리: 완전한 트랜잭션 제어 지원
- 다중 연결 지원: 여러 데이터베이스에 동시 연결
- 타입 매핑: SQL과 C 데이터 타입 간 자동 변환
- 콜백을 통한 오류 처리: WHENEVER 문을 사용한 예외 처리
- 동적 SQL 실행: 런타임에 SQL 문 구성 및 실행
- 호환성 모드: Informix, Oracle 호환성 지원
- 대용량 객체 지원: LOB(Large Object) 처리

---

### 2. ECPG 개념

#### 2.1 동작 원리

ECPG 프로그램은 다음 단계를 거쳐 처리됨:

```
1. 소스 코드 작성: *.pgc 파일에 C 코드와 SQL 혼합
2. 전처리: ecpg 전처리기가 *.pgc를 표준 C(*.c) 파일로 변환
3. 컴파일: 일반 C 컴파일러로 처리
4. 실행: 변환된 ECPG 애플리케이션은 ecpglib 라이브러리를 통해
        libpq 함수를 호출하고 PostgreSQL과 통신
```

#### 2.2 SQL 문 구문

모든 내장 SQL 문은 다음 형식을 따름:

```c
EXEC SQL ...;
```

이 구문들은 일반 C 문장과 동일한 위치에 쓸 수 있으며, 전역 수준과 함수 내부 모두에서 사용 가능함.

#### 2.3 ECPG의 장점

1. 단순화된 데이터 처리: C 변수와의 데이터 교환을 자동으로 관리
2. 빌드 타임 검증: SQL 코드의 구문을 컴파일 시점에 검사
3. 표준 준수: SQL 표준에 명시되어 다양한 데이터베이스 시스템에서 지원
4. 이식성: 다른 SQL 데이터베이스용 프로그램을 PostgreSQL로 쉽게 이식 가능

#### 2.4 중요한 구문 규칙

- 대소문자 구분: 내장 SQL은 C가 아닌 SQL 대소문자 규칙을 따름
- 주석: SQL 섹션에서는 SQL 표준에 따라 중첩된 C 스타일 주석 허용
- 문자열/식별자 파싱: C가 아닌 SQL 규칙 사용
- 문자열 상수: ECPG는 `standard_conforming_strings`가 활성화되어 있다고 가정

---

### 3. 데이터베이스 연결 관리

#### 3.1 데이터베이스 서버 연결

##### 기본 구문

```c
EXEC SQL CONNECT TO target [AS connection-name] [USER user-name];
```

##### target 지정 방법

- `dbname[@hostname][:port]`
- `tcp:postgresql://hostname[:port][/dbname][?options]`
- `unix:postgresql://localhost[:port][/dbname][?options]`
- 위 형식 중 하나를 포함하는 SQL 문자열 리터럴
- 문자 변수에 대한 참조
- `DEFAULT` (기본 데이터베이스에 기본 사용자로 연결)

##### 사용자 이름 지정 방법

- `username`
- `username/password`
- `username IDENTIFIED BY password`
- `username USING password`

##### 연결 예제

```c
/* 간단한 연결 */
EXEC SQL CONNECT TO mydb@sql.mydomain.com;

/* 연결 이름과 사용자 지정 */
EXEC SQL CONNECT TO tcp:postgresql://sql.mydomain.com/mydb AS myconnection USER john;

/* 변수를 사용한 연결 */
EXEC SQL BEGIN DECLARE SECTION;
const char *target = "mydb@sql.mydomain.com";
const char *user = "john";
const char *passwd = "secret";
EXEC SQL END DECLARE SECTION;

EXEC SQL CONNECT TO :target USER :user USING :passwd;
```

##### 보안 참고사항

신뢰할 수 없는 사용자가 접근할 수 있는 환경에서는 `search_path`에서 공개 쓰기 가능 스키마를 제거할 것:

```c
EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
```

#### 3.2 연결 선택

여러 연결을 관리하는 방법은 세 가지임:

##### 방법 1: 명시적 AT 절

```c
EXEC SQL AT connection-name SELECT ...;
```

여러 연결을 혼용해야 할 때 적합함.

##### 방법 2: SET CONNECTION 문

```c
EXEC SQL SET CONNECTION connection-name;
```

동일 연결에서 많은 문장을 실행할 때 적합함.

##### 완전한 예제 프로그램

```c
#include <stdio.h>

EXEC SQL BEGIN DECLARE SECTION;
    char dbname[1024];
EXEC SQL END DECLARE SECTION;

int main()
{
    /* 세 개의 데이터베이스에 연결 */
    EXEC SQL CONNECT TO testdb1 AS con1 USER testuser;
    EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
    EXEC SQL COMMIT;

    EXEC SQL CONNECT TO testdb2 AS con2 USER testuser;
    EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
    EXEC SQL COMMIT;

    EXEC SQL CONNECT TO testdb3 AS con3 USER testuser;
    EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
    EXEC SQL COMMIT;

    /* 마지막으로 열린 데이터베이스 "testdb3"에서 쿼리 실행 */
    EXEC SQL SELECT current_database() INTO :dbname;
    printf("current=%s (should be testdb3)\n", dbname);

    /* "AT"을 사용하여 "testdb2"에서 쿼리 실행 */
    EXEC SQL AT con2 SELECT current_database() INTO :dbname;
    printf("current=%s (should be testdb2)\n", dbname);

    /* 현재 연결을 "testdb1"로 전환 */
    EXEC SQL SET CONNECTION con1;

    EXEC SQL SELECT current_database() INTO :dbname;
    printf("current=%s (should be testdb1)\n", dbname);

    EXEC SQL DISCONNECT ALL;
    return 0;
}
```

출력:
```
current=testdb3 (should be testdb3)
current=testdb2 (should be testdb2)
current=testdb1 (should be testdb1)
```

##### 방법 3: 연결과 함께 DECLARE STATEMENT 사용

```c
EXEC SQL AT connection-name DECLARE statement-name STATEMENT;
EXEC SQL PREPARE statement-name FROM :dyn-string;
```

예제:

```c
#include <stdio.h>

EXEC SQL BEGIN DECLARE SECTION;
char dbname[128];
char *dyn_sql = "SELECT current_database()";
EXEC SQL END DECLARE SECTION;

int main(){
    EXEC SQL CONNECT TO postgres AS con1;
    EXEC SQL CONNECT TO testdb AS con2;
    EXEC SQL AT con1 DECLARE stmt STATEMENT;
    EXEC SQL PREPARE stmt FROM :dyn_sql;
    EXEC SQL EXECUTE stmt INTO :dbname;
    printf("%s\n", dbname);

    EXEC SQL DISCONNECT ALL;
    return 0;
}
```

출력: `postgres` (기본 연결에 관계없이 con1에서 실행)

##### 스레딩 참고사항

여러 스레드가 동시에 연결을 공유 불가. 뮤텍스로 접근을 제어하거나 스레드별로 별도의 연결을 사용할 것.

#### 3.3 연결 닫기

##### 구문

```c
EXEC SQL DISCONNECT [connection];
```

##### 연결 지정

- `connection-name` - 지정된 연결 닫기
- `CURRENT` - 현재 연결 닫기
- `ALL` - 모든 연결 닫기
- 생략 - 현재 연결 닫기 (기본값)

##### 모범 사례

리소스 관리를 위해 열린 모든 연결을 명시적으로 해제할 것.

```c
EXEC SQL DISCONNECT ALL;
```

---

### 4. SQL 명령 실행

#### 4.1 SQL 문 실행

SQL 명령은 `EXEC SQL`을 사용하여 직접 실행 가능:

##### 테이블 생성

```c
EXEC SQL CREATE TABLE foo (number integer, ascii char(16));
EXEC SQL CREATE UNIQUE INDEX num1 ON foo(number);
EXEC SQL COMMIT;
```

##### 행 삽입

```c
EXEC SQL INSERT INTO foo (number, ascii) VALUES (9999, 'doodad');
EXEC SQL COMMIT;
```

##### 행 삭제

```c
EXEC SQL DELETE FROM foo WHERE number = 9999;
EXEC SQL COMMIT;
```

##### 행 업데이트

```c
EXEC SQL UPDATE foo
    SET ascii = 'foobar'
    WHERE number = 9999;
EXEC SQL COMMIT;
```

##### 단일 행 SELECT

```c
EXEC SQL SELECT foo INTO :FooBar FROM table1 WHERE ascii = 'doodad';
```

##### 설정 매개변수 조회

```c
EXEC SQL SHOW search_path INTO :var;
```

> 참고: `:something` 형태의 토큰은 C 프로그램 변수를 참조하는 호스트 변수임.

#### 4.2 커서 사용

여러 행을 반환하는 결과 집합에는 커서를 사용함:

```c
/* 커서 선언 */
EXEC SQL DECLARE foo_bar CURSOR FOR
    SELECT number, ascii FROM foo
    ORDER BY ascii;

/* 커서 열기 */
EXEC SQL OPEN foo_bar;

/* 데이터 가져오기 */
EXEC SQL FETCH foo_bar INTO :FooBar, DooDad;
...

/* 커서 닫기 */
EXEC SQL CLOSE foo_bar;
EXEC SQL COMMIT;
```

> 중요: `ECPG DECLARE` 명령은 PostgreSQL 서버에 아무것도 전송하지 않음. 커서는 `OPEN`이 실행될 때 백엔드에서 실제로 열림.

#### 4.3 트랜잭션 관리

- `EXEC SQL COMMIT`: 진행 중인 트랜잭션 커밋
- `EXEC SQL ROLLBACK`: 진행 중인 트랜잭션 롤백
- `EXEC SQL PREPARE TRANSACTION transaction_id`: 2단계 커밋 준비
- `EXEC SQL COMMIT PREPARED transaction_id`: 준비된 트랜잭션 커밋
- `EXEC SQL ROLLBACK PREPARED transaction_id`: 준비된 트랜잭션 롤백
- `EXEC SQL SET AUTOCOMMIT TO ON`: 자동 커밋 모드 활성화
- `EXEC SQL SET AUTOCOMMIT TO OFF`: 자동 커밋 모드 비활성화 (기본값)

기본 모드: 문장은 `EXEC SQL COMMIT`이 실행될 때만 커밋됨.

자동 커밋 모드: `ecpg`에 `-t` 명령줄 옵션을 전달하거나 `EXEC SQL SET AUTOCOMMIT TO ON`으로 활성화 가능. 명시적 트랜잭션 블록 내부가 아닌 한 각 명령이 자동으로 커밋됨.

#### 4.4 준비된 문장 (Prepared Statements)

값을 미리 알 수 없거나 문장을 재사용해야 할 경우 준비된 문장을 사용함:

##### 문장 준비

```c
EXEC SQL PREPARE stmt1 FROM "SELECT oid, datname FROM pg_database WHERE oid = ?";
```

##### 단일 행 결과 실행

```c
EXEC SQL EXECUTE stmt1 INTO :dboid, :dbname USING 1;
```

##### 준비된 문장과 커서 사용

```c
EXEC SQL PREPARE stmt1 FROM "SELECT oid, datname FROM pg_database WHERE oid > ?";
EXEC SQL DECLARE foo_bar CURSOR FOR stmt1;

EXEC SQL WHENEVER NOT FOUND DO BREAK;
EXEC SQL OPEN foo_bar USING 100;

while (1)
{
    EXEC SQL FETCH NEXT FROM foo_bar INTO :dboid, :dbname;
    ...
}
EXEC SQL CLOSE foo_bar;
```

##### 준비된 문장 해제

```c
EXEC SQL DEALLOCATE PREPARE name;
```

미지의 값에는 `?`를 플레이스홀더로 사용하고, `USING` 절로 실제 값을 전달함.

---

### 5. 호스트 변수

#### 5.1 개요

호스트 변수(Host Variables)는 내장 SQL 문에서 C 프로그램과 PostgreSQL 사이의 데이터 교환에 사용되는 C 변수임. SQL 문은 C 프로그램 코드("호스트 언어") 안에 삽입된 손님으로 취급되므로, C 변수를 호스트 변수라고 부름.

##### 기본 구문

```c
EXEC SQL INSERT INTO sometable VALUES (:v1, 'foo', :v2);
```

SQL 문에서 사용될 때 변수 앞에 콜론(`:`)을 붙임.

#### 5.2 선언 섹션 (Declare Sections)

전처리기가 호스트 변수를 인식할 수 있도록, 특별히 표시된 섹션 안에서 선언 필요.

##### 구문

```c
EXEC SQL BEGIN DECLARE SECTION;
    int x = 4;
    char foo[16], bar[16];
EXEC SQL END DECLARE SECTION;
```

##### 대안적 암시적 구문

```c
EXEC SQL int i = 4;
```

##### 핵심 사항

- 변수는 선택적 초기값을 가질 수 있음
- 범위는 프로그램 내 섹션의 위치에 따라 결정됨
- 여러 선언 섹션 허용
- 선언은 일반 C 변수로 출력 파일에 그대로 복사됨
- 구조체와 공용체는 `DECLARE` 섹션 내부에서 선언해야 함

#### 5.3 쿼리 결과 가져오기

##### SELECT (단일 행)

select 목록과 `FROM` 절 사이에 `INTO` 절을 사용함:

```c
EXEC SQL BEGIN DECLARE SECTION;
    int v1;
    VARCHAR v2;
EXEC SQL END DECLARE SECTION;

EXEC SQL SELECT a, b INTO :v1, :v2 FROM test;
```

##### FETCH (커서를 사용한 여러 행)

모든 일반 절 뒤에 `INTO` 절을 사용함:

```c
EXEC SQL BEGIN DECLARE SECTION;
    int v1;
    VARCHAR v2;
EXEC SQL END DECLARE SECTION;

EXEC SQL DECLARE foo CURSOR FOR SELECT a, b FROM test;

do {
    EXEC SQL FETCH NEXT FROM foo INTO :v1, :v2;
} while (...);
```

#### 5.4 타입 매핑

PostgreSQL 데이터 타입은 대응하는 C 변수 타입에 매핑되어야 함:

- `smallint`: `short`
- `integer`: `int`
- `bigint`: `long long int`
- `real`: `float`
- `double precision`: `double`
- `character(n)`, `varchar(n)`, `text`: `char[n+1]`, `VARCHAR[n+1]`
- `boolean`: `bool`
- `timestamp`: `timestamp`*
- `date`: `date`*
- `interval`: `interval`*
- `numeric`, `decimal`: `numeric`, `decimal`*
- `bytea`: `char *`, `bytea[n]`

*pgtypes 라이브러리 함수가 필요한 특수 타입

#### 5.5 문자열 처리

##### char[] 사용

```c
EXEC SQL BEGIN DECLARE SECTION;
    char str[50];
EXEC SQL END DECLARE SECTION;
```

주의: 버퍼 오버플로우를 방지하려면 길이를 직접 관리 필요.

##### VARCHAR 사용

```c
VARCHAR var[180];
```

다음과 같이 변환됨:

```c
struct varchar_var {
    int len;        /* null 종결자 제외 길이 */
    char arr[180];  /* null 종결자가 있는 문자열 */
} var;
```

입력값으로 사용할 때는 `strlen(arr)`과 `len` 중 더 짧은 값이 사용됨.

#### 5.6 특수 데이터 타입

##### Timestamp과 Date

헤더:
```c
#include <pgtypes_timestamp.h>
```

사용법:
```c
EXEC SQL BEGIN DECLARE SECTION;
    timestamp ts;
EXEC SQL END DECLARE SECTION;

EXEC SQL SELECT now()::timestamp INTO :ts;
printf("ts = %s\n", PGTYPEStimestamp_to_asc(ts));
```

출력:
```
ts = 2010-06-27 18:03:56.949343
```

##### Interval (힙 할당)

```c
#include <pgtypes_interval.h>

EXEC SQL BEGIN DECLARE SECTION;
    interval *in;
EXEC SQL END DECLARE SECTION;

in = PGTYPESinterval_new();
EXEC SQL SELECT '1 min'::interval INTO :in;
printf("interval = %s\n", PGTYPESinterval_to_asc(in));
PGTYPESinterval_free(in);
```

##### Numeric과 Decimal

```c
#include <pgtypes_numeric.h>

EXEC SQL BEGIN DECLARE SECTION;
    numeric *num;
    decimal *dec;
EXEC SQL END DECLARE SECTION;

num = PGTYPESnumeric_new();
dec = PGTYPESdecimal_new();

EXEC SQL SELECT 12.345::numeric(4,2), 23.456::decimal(4,2) INTO :num, :dec;

printf("numeric = %s\n", PGTYPESnumeric_to_asc(num, 0));
```

#### 5.7 호스트 변수로서의 배열

##### 배열에 여러 행 가져오기

```c
int main(void) {
EXEC SQL BEGIN DECLARE SECTION;
    int dbid[8];
    char dbname[8][16];
    int i;
EXEC SQL END DECLARE SECTION;

    memset(dbname, 0, sizeof(char) * 16 * 8);
    memset(dbid, 0, sizeof(int) * 8);

    EXEC SQL SELECT oid, datname INTO :dbid, :dbname FROM pg_database;

    for (i = 0; i < 8; i++)
        printf("oid=%d, dbname=%s\n", dbid[i], dbname[i]);
}
```

#### 5.8 호스트 변수로서의 구조체

구조체 멤버에 여러 열을 한 번에 가져오기:

```c
EXEC SQL BEGIN DECLARE SECTION;
    typedef struct {
       int oid;
       char datname[65];
       long long int size;
    } dbinfo_t;

    dbinfo_t dbval;
EXEC SQL END DECLARE SECTION;

EXEC SQL DECLARE cur1 CURSOR FOR
    SELECT oid, datname, pg_database_size(oid) AS size FROM pg_database;
EXEC SQL OPEN cur1;

EXEC SQL WHENEVER NOT FOUND DO BREAK;

while (1) {
    EXEC SQL FETCH FROM cur1 INTO :dbval;
    printf("oid=%d, datname=%s, size=%lld\n",
           dbval.oid, dbval.datname, dbval.size);
}

EXEC SQL CLOSE cur1;
```

#### 5.9 NULL 처리를 위한 인디케이터

인디케이터는 null 값과 잘림 여부를 추적함:

```c
EXEC SQL BEGIN DECLARE SECTION;
    VARCHAR val;
    int val_ind;
EXEC SQL END DECLARE SECTION;

EXEC SQL SELECT b INTO :val :val_ind FROM test1;
```

##### 인디케이터 값

- `0`: 값이 null이 아님
- 음수: 값이 null (실제 호스트 변수 무시됨)
- 양수: 값이 null이 아니지만 잘림

##### no-indicator 모드

전처리기에 `-r no_indicator`를 전달함. null 값은 다음과 같이 표현됨:
- 문자 타입의 경우 빈 문자열
- 정수 타입의 경우 가능한 가장 낮은 값 (예: `INT_MIN`)

#### 5.10 복합 타입 예제

```c
EXEC SQL BEGIN DECLARE SECTION;
    typedef struct {
        int intval;
        varchar textval[33];
    } comp_t;

    comp_t compval;
EXEC SQL END DECLARE SECTION;

EXEC SQL DECLARE cur1 CURSOR FOR
    SELECT (compval).* FROM t4;
EXEC SQL OPEN cur1;

EXEC SQL WHENEVER NOT FOUND DO BREAK;

while (1) {
    EXEC SQL FETCH FROM cur1 INTO :compval;
    printf("intval=%d, textval=%s\n",
           compval.intval, compval.textval.arr);
}
```

---

### 6. 동적 SQL

동적 SQL을 사용하면 런타임에 구성되거나 외부에서 제공된 SQL 문을 실행 가능. ECPG는 동적 SQL 실행을 위해 세 가지 주요 방식을 제공함.

#### 6.1 결과 집합 없이 문장 실행

명령: `EXECUTE IMMEDIATE`

행을 반환하지 않는 SQL 문(DDL, INSERT, UPDATE, DELETE)에 사용함.

```c
EXEC SQL BEGIN DECLARE SECTION;
const char *stmt = "CREATE TABLE test1 (...);";
EXEC SQL END DECLARE SECTION;

EXEC SQL EXECUTE IMMEDIATE :stmt;
```

제한사항: SELECT나 데이터를 조회하는 문장은 실행 불가.

#### 6.2 입력 매개변수가 있는 문장 실행

문장을 한 번 준비한 뒤, 다른 매개변수로 여러 번 실행하는 방식임.

매개변수에 대한 플레이스홀더로 물음표(`?`)를 사용함:

```c
EXEC SQL BEGIN DECLARE SECTION;
const char *stmt = "INSERT INTO test1 VALUES(?, ?);";
EXEC SQL END DECLARE SECTION;

EXEC SQL PREPARE mystmt FROM :stmt;
...
EXEC SQL EXECUTE mystmt USING 42, 'foobar';
```

더 이상 필요하지 않은 준비된 문장은 해제함:

```c
EXEC SQL DEALLOCATE PREPARE name;
```

#### 6.3 결과 집합이 있는 문장 실행

##### 단일 결과 행

`EXECUTE`에 `INTO` 절을 사용하여 결과를 저장함:

```c
EXEC SQL BEGIN DECLARE SECTION;
const char *stmt = "SELECT a, b, c FROM test1 WHERE a > ?";
int v1, v2;
VARCHAR v3[50];
EXEC SQL END DECLARE SECTION;

EXEC SQL PREPARE mystmt FROM :stmt;
...
EXEC SQL EXECUTE mystmt INTO :v1, :v2, :v3 USING 37;
```

##### 여러 결과 행

여러 행을 반환하는 쿼리에는 커서를 사용함:

```c
EXEC SQL BEGIN DECLARE SECTION;
char dbaname[128];
char datname[128];
char *stmt = "SELECT u.usename as dbaname, d.datname "
             "  FROM pg_database d, pg_user u "
             "  WHERE d.datdba = u.usesysid";
EXEC SQL END DECLARE SECTION;

EXEC SQL CONNECT TO testdb AS con1 USER testuser;
EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
EXEC SQL COMMIT;

EXEC SQL PREPARE stmt1 FROM :stmt;
EXEC SQL DECLARE cursor1 CURSOR FOR stmt1;
EXEC SQL OPEN cursor1;

EXEC SQL WHENEVER NOT FOUND DO BREAK;

while (1)
{
    EXEC SQL FETCH cursor1 INTO :dbaname,:datname;
    printf("dbaname=%s, datname=%s\n", dbaname, datname);
}

EXEC SQL CLOSE cursor1;
EXEC SQL COMMIT;
EXEC SQL DISCONNECT ALL;
```

#### 6.4 EXECUTE 절 조합

`EXECUTE` 명령은 다음을 가질 수 있음:
- `INTO` 절만
- `USING` 절만
- `INTO`와 `USING` 절 모두
- 둘 다 없음

---

### 7. 오류 처리

ECPG는 예외와 경고 처리를 위해 서로 독립적으로 사용할 수 있는 두 가지 기능을 제공함:
1. 콜백 - `WHENEVER` 명령 사용
2. 상세 정보 - `sqlca` 변수 접근

#### 7.1 WHENEVER로 콜백 설정

##### 구문

```c
EXEC SQL WHENEVER condition action;
```

##### 조건 (Conditions)

- `SQLERROR`: SQL 문 실행 중 오류 발생 시 호출
- `SQLWARNING`: SQL 문 실행 중 경고 발생 시 호출
- `NOT FOUND`: SQL 문이 0개의 행을 검색하거나 영향을 줄 때 호출

##### 액션 (Actions)

- `CONTINUE`: 조건 무시 (기본값)
- `GOTO label` / `GO TO label`: C `goto`를 사용하여 지정된 레이블로 점프
- `SQLPRINT`: stderr에 메시지 출력
- `STOP`: `exit(1)` 호출하여 프로그램 종료
- `DO BREAK`: C `break` 문 실행 (루프/switch에서만)
- `DO CONTINUE`: C `continue` 문 실행 (루프에서만)
- `CALL name(args)` / `DO name(args)`: 지정된 C 함수 호출

##### 기본 예제

```c
EXEC SQL WHENEVER SQLWARNING SQLPRINT;
EXEC SQL WHENEVER SQLERROR STOP;
```

##### 중요 참고사항

`EXEC SQL WHENEVER`는 C 문이 아니라 전처리기 지시문임. 오류 핸들러는 C 제어 흐름과 무관하게 지시문이 위치한 곳 이후의 모든 내장 SQL 문에 적용됨. 다음 패턴은 동작하지 않음:

```c
/* 잘못됨 - if 블록 내부의 핸들러 */
int main(int argc, char *argv[])
{
    if (verbose) {
        EXEC SQL WHENEVER SQLWARNING SQLPRINT;  /* 조건과 무관하게 항상 적용됨 */
    }
    EXEC SQL SELECT ...;
}

/* 잘못됨 - 호출된 함수에서 설정된 핸들러 */
int main(int argc, char *argv[])
{
    set_error_handler();  /* main()의 SQL 문에 영향 없음 */
    EXEC SQL SELECT ...;
}

static void set_error_handler(void)
{
    EXEC SQL WHENEVER SQLERROR STOP;  /* 이 함수 내에서만 유효 */
}
```

#### 7.2 sqlca 변수

##### 구조체 정의

```c
struct
{
    char sqlcaid[8];
    long sqlabc;
    long sqlcode;
    struct
    {
        int sqlerrml;
        char sqlerrmc[SQLERRMC_LEN];
    } sqlerrm;
    char sqlerrp[8];
    long sqlerrd[6];
    char sqlwarn[8];
    char sqlstate[5];
} sqlca;
```

##### 주요 필드

- `sqlcode`: 오류 코드 (0 = 성공, 음수 = 오류, 양수 = 무해한 조건)
- `sqlstate`: 5문자 오류 코드 (sqlcode보다 선호됨)
- `sqlerrm.sqlerrmc`: 오류 메시지 문자열
- `sqlerrm.sqlerrml`: 오류 메시지 길이
- `sqlerrd[1]`: 처리된 행의 OID
- `sqlerrd[2]`: 처리/반환된 행 수
- `sqlwarn[0]`: 경고가 있으면 'W'로 설정
- `sqlwarn[1]`: 값이 잘리면 'W'로 설정
- `sqlwarn[2]`: 경고에 대해 'W'로 설정

##### 멀티스레딩

멀티스레드 프로그램에서 각 스레드는 자동으로 `sqlca`의 독립적인 복사본을 갖음 (`errno`와 유사).

##### 예제: sqlca 내용 출력

```c
EXEC SQL WHENEVER SQLERROR CALL print_sqlca();

void print_sqlca()
{
    fprintf(stderr, "==== sqlca ====\n");
    fprintf(stderr, "sqlcode: %ld\n", sqlca.sqlcode);
    fprintf(stderr, "sqlerrm.sqlerrml: %d\n", sqlca.sqlerrm.sqlerrml);
    fprintf(stderr, "sqlerrm.sqlerrmc: %s\n", sqlca.sqlerrm.sqlerrmc);
    fprintf(stderr, "sqlerrd: %ld %ld %ld %ld %ld %ld\n",
            sqlca.sqlerrd[0], sqlca.sqlerrd[1], sqlca.sqlerrd[2],
            sqlca.sqlerrd[3], sqlca.sqlerrd[4], sqlca.sqlerrd[5]);
    fprintf(stderr, "sqlwarn: %d %d %d %d %d %d %d %d\n",
            sqlca.sqlwarn[0], sqlca.sqlwarn[1], sqlca.sqlwarn[2],
            sqlca.sqlwarn[3], sqlca.sqlwarn[4], sqlca.sqlwarn[5],
            sqlca.sqlwarn[6], sqlca.sqlwarn[7]);
    fprintf(stderr, "sqlstate: %5s\n", sqlca.sqlstate);
    fprintf(stderr, "===============\n");
}
```

##### 예제 출력

```
==== sqlca ====
sqlcode: -400
sqlerrm.sqlerrml: 49
sqlerrm.sqlerrmc: relation "pg_databasep" does not exist on line 38
sqlerrd: 0 0 0 0 0 0
sqlwarn: 0 0 0 0 0 0 0 0
sqlstate: 42P01
===============
```

#### 7.3 SQLSTATE vs. SQLCODE

##### SQLSTATE (선호됨)

- 5문자 배열 (숫자 또는 대문자)
- 계층적: 처음 2문자 = 일반 클래스, 마지막 3문자 = 서브클래스
- 성공 코드: `00000`
- 표준 기반: SQL 표준에 정의, PostgreSQL에서 기본 지원
- 이식성: 다른 SQL 구현 간 더 나은 호환성

##### SQLCODE (사용 중단됨)

- 단순 정수 값
- 체계: 0 = 성공, 양수 = 정보와 함께 성공, 음수 = 오류
- 제한된 이식성: +100 이상은 표준화되지 않음
- 계층 구조 없음
- 참고: SQL-92 표준에서 사용 중단됨

##### 권장사항

새 애플리케이션에는 SQLSTATE를 사용할 것. 이식성과 일관성이 더 뛰어남.

#### 7.4 SQLCODE 오류 코드 참조

##### 성공

```c
0 (ECPG_NO_ERROR) -> SQLSTATE 00000
100 (ECPG_NOT_FOUND) -> SQLSTATE 02000
```

##### 예제: NOT FOUND로 루프 감지

```c
while (1)
{
    EXEC SQL FETCH ... ;
    if (sqlca.sqlcode == ECPG_NOT_FOUND)
        break;
}

/* 다음과 동일: */
EXEC SQL WHENEVER NOT FOUND DO BREAK;
```

##### 메모리 및 시스템 오류

```c
-12 (ECPG_OUT_OF_MEMORY) -> SQLSTATE YE001
-200 (ECPG_UNSUPPORTED) -> SQLSTATE YE002
```

##### 인수 불일치 오류

```c
-201 (ECPG_TOO_MANY_ARGUMENTS) -> SQLSTATE 07001 또는 07002
-202 (ECPG_TOO_FEW_ARGUMENTS) -> SQLSTATE 07001 또는 07002
```

##### 데이터 타입 변환 오류

```c
-204 (ECPG_INT_FORMAT) -> SQLSTATE 42804
-205 (ECPG_UINT_FORMAT) -> SQLSTATE 42804
-206 (ECPG_FLOAT_FORMAT) -> SQLSTATE 42804
-207 (ECPG_NUMERIC_FORMAT) -> SQLSTATE 42804
-208 (ECPG_INTERVAL_FORMAT) -> SQLSTATE 42804
-209 (ECPG_DATE_FORMAT) -> SQLSTATE 42804
-210 (ECPG_TIMESTAMP_FORMAT) -> SQLSTATE 42804
-211 (ECPG_CONVERT_BOOL) -> SQLSTATE 42804
```

##### Null 처리

```c
-213 (ECPG_MISSING_INDICATOR) -> SQLSTATE 22002
```

##### 배열 오류

```c
-214 (ECPG_NO_ARRAY) -> SQLSTATE 42804
-215 (ECPG_DATA_NOT_ARRAY) -> SQLSTATE 42804
-216 (ECPG_ARRAY_INSERT) -> SQLSTATE 42804
```

##### 연결 오류

```c
-220 (ECPG_NO_CONN) -> SQLSTATE 08003
-221 (ECPG_NOT_CONN) -> SQLSTATE YE002
```

##### 문장 및 디스크립터 오류

```c
-230 (ECPG_INVALID_STMT) -> SQLSTATE 26000
-240 (ECPG_UNKNOWN_DESCRIPTOR) -> SQLSTATE 33000
-241 (ECPG_INVALID_DESCRIPTOR_INDEX) -> SQLSTATE 07009
-242 (ECPG_UNKNOWN_DESCRIPTOR_ITEM) -> SQLSTATE YE002
```

##### 제약 조건 및 쿼리 오류

```c
-239 (ECPG_INFORMIX_DUPLICATE_KEY) -> SQLSTATE 23505
-243 (ECPG_VAR_NOT_NUMERIC) -> SQLSTATE 07006
-244 (ECPG_VAR_NOT_CHAR) -> SQLSTATE 07006
-284 (ECPG_INFORMIX_SUBSELECT_NOT_ONE) -> SQLSTATE 21000
-403 (ECPG_DUPLICATE_KEY) -> SQLSTATE 23505
-404 (ECPG_SUBSELECT_NOT_ONE) -> SQLSTATE 21000
```

##### PostgreSQL 서버 오류

```c
-400 (ECPG_PGSQL) -> PostgreSQL 서버 오류 메시지 포함
-401 (ECPG_TRANS) -> 트랜잭션 오류 -> SQLSTATE 08007
-402 (ECPG_CONNECT) -> 연결 실패 -> SQLSTATE 08001
```

##### 포털/커서 경고

```c
-602 (ECPG_WARNING_UNKNOWN_PORTAL) -> SQLSTATE 34000
-603 (ECPG_WARNING_IN_TRANSACTION) -> SQLSTATE 25001
-604 (ECPG_WARNING_NO_TRANSACTION) -> SQLSTATE 25P01
-605 (ECPG_WARNING_PORTAL_EXISTS) -> SQLSTATE 42P03
```

---

### 8. 전처리기 지시문

PostgreSQL ECPG 전처리기는 파일 파싱 및 처리 방법을 수정하는 여러 지시문을 지원함.

#### 8.1 파일 포함

`EXEC SQL INCLUDE` 지시문을 사용하여 외부 파일을 포함함:

```c
EXEC SQL INCLUDE filename;
EXEC SQL INCLUDE <filename>;
EXEC SQL INCLUDE "filename";
```

##### 주요 동작

- 파일 이름에 `.h`가 없으면 전처리기가 자동으로 추가함
- 검색 순서:
  1. 현재 디렉토리
  2. `/usr/local/include`
  3. PostgreSQL 포함 디렉토리 (예: `/usr/local/pgsql/include`)
  4. `/usr/include`
- `EXEC SQL INCLUDE "filename"` 사용 시 현재 디렉토리만 검색됨
- 포함된 파일도 전처리되므로 내장 SQL 문이 올바르게 처리됨
- C의 `#include`와 동일하지 않음 — C의 `#include`는 SQL 명령을 전처리하지 않음

참고: 포함 파일 이름은 대소문자를 구분함.

#### 8.2 define 및 undef 지시문

내장 SQL에서 사용할 상수를 정의함:

```c
EXEC SQL DEFINE name;
EXEC SQL DEFINE name value;
EXEC SQL DEFINE HAVE_FEATURE;
EXEC SQL DEFINE MYNUMBER 12;
EXEC SQL DEFINE MYSTRING 'abc';
```

`UNDEF`로 정의를 제거함:

```c
EXEC SQL UNDEF MYNUMBER;
```

##### C `#define`과의 주요 차이점

- `EXEC SQL DEFINE` 값은 ecpg 전처리기가 평가하고 컴파일 전에 대체함
- 내장 SQL 쿼리에서 사용하는 상수에는 C의 `#define`을 사용 불가
- `EXEC SQL DEFINE`/`UNDEF`의 효과는 여러 입력 파일에 걸쳐 전파되지 않음; 각 파일은 `-D` 명령줄 심볼만으로 새로 시작함

#### 8.3 조건부 컴파일 지시문

조건부 코드 컴파일을 위해 이러한 지시문을 사용함:

```c
EXEC SQL ifdef name;      /* name이 정의되어 있으면 처리 */
EXEC SQL ifndef name;     /* name이 정의되어 있지 않으면 처리 */
EXEC SQL elif name;       /* 대안 섹션 (여러 개 허용) */
EXEC SQL else;            /* 최종 대안 섹션 */
EXEC SQL endif;           /* 조건부 블록 종료 */
```

##### 특징

- 구조는 최대 127 레벨까지 중첩 가능
- `elif` 섹션은 name이 정의되어 있고 이전 섹션이 처리되지 않은 경우에만 처리됨

##### 예제

```c
EXEC SQL ifdef TZVAR;
EXEC SQL SET TIMEZONE TO TZVAR;
EXEC SQL elif TZNAME;
EXEC SQL SET TIMEZONE TO TZNAME;
EXEC SQL else;
EXEC SQL SET TIMEZONE TO 'GMT';
EXEC SQL endif;
```

---

### 9. 처리 및 컴파일

#### 9.1 개요

ECPG 프로그램은 컴파일 전에 전처리가 필요함. 워크플로우는 다음과 같음:

1. `ecpg` 도구로 SQL 문 전처리
2. 생성된 C 코드 컴파일
3. `libecpg` 라이브러리와 링크

#### 9.2 단계별 프로세스

##### 1. 전처리

`ecpg` 전처리기는 내장 SQL 문을 특수 함수 호출로 변환함.

명령:
```bash
ecpg prog1.pgc
```

입력 파일 `prog1.pgc`에서 `prog1.c`를 생성함. ECPG 프로그램은 일반적으로 `.pgc` 확장자를 사용함.

사용자 지정 출력 파일을 지정하려면:
```bash
ecpg -o output.c input.pgc
```

##### 2. 컴파일

전처리된 C 파일을 일반적인 방법으로 컴파일함:

```bash
cc -c prog1.c
```

중요: PostgreSQL 헤더가 기본 검색 경로에 없는 경우 경로를 명시함:
```bash
cc -c prog1.c -I/usr/local/pgsql/include
```

##### 3. 링크

`libecpg` 라이브러리와 링크함:

```bash
cc -o myprog prog1.o prog2.o ... -lecpg
```

라이브러리가 기본 경로에 없는 경우 경로를 추가함:
```bash
cc -o myprog prog1.o prog2.o ... -lecpg -L/usr/local/pgsql/lib
```

#### 9.3 설치 경로 찾기

PostgreSQL 설치 경로를 확인하려면 `pg_config` 또는 `pkg-config`를 사용함:

```bash
pg_config --includedir
pg_config --libdir
# 또는
pkg-config --cflags --libs libecpg
```

#### 9.4 Make 통합

대규모 프로젝트에서는 Makefile에 다음 암시적 규칙을 추가함:

```makefile
ECPG = ecpg

%.c: %.pgc
        $(ECPG) $<
```

#### 9.5 스레딩 지원

`libecpg` 라이브러리는 기본적으로 스레드 안전하지만, 클라이언트 코드 컴파일 시 스레딩 컴파일러 플래그를 추가해야 가능.

---

### 10. 완전한 예제 프로그램

다음은 ECPG의 여러 기능을 보여주는 완전한 예제임:

```c
/* 파일: example.pgc */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ECPG 호스트 변수 선언 */
EXEC SQL BEGIN DECLARE SECTION;
    char dbname[128];
    char username[64];
    int emp_id;
    char emp_name[100];
    double emp_salary;
    int emp_salary_ind;  /* NULL 인디케이터 */
EXEC SQL END DECLARE SECTION;

/* 오류 처리 함수 */
void print_error(void)
{
    fprintf(stderr, "SQL 오류: %s\n", sqlca.sqlerrm.sqlerrmc);
    fprintf(stderr, "SQLSTATE: %.5s\n", sqlca.sqlstate);
}

int main(int argc, char *argv[])
{
    /* 오류 발생 시 print_error 호출 */
    EXEC SQL WHENEVER SQLERROR CALL print_error();

    /* 데이터베이스에 연결 */
    EXEC SQL CONNECT TO mydb USER postgres;

    if (sqlca.sqlcode != 0) {
        fprintf(stderr, "연결 실패\n");
        return 1;
    }

    printf("데이터베이스에 연결됨\n");

    /* 보안을 위해 search_path 설정 */
    EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
    EXEC SQL COMMIT;

    /* 현재 데이터베이스 이름 조회 */
    EXEC SQL SELECT current_database() INTO :dbname;
    printf("현재 데이터베이스: %s\n", dbname);

    /* 테이블 생성 (존재하지 않는 경우) */
    EXEC SQL CREATE TABLE IF NOT EXISTS employees (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        salary NUMERIC(10,2)
    );
    EXEC SQL COMMIT;

    /* 데이터 삽입 */
    EXEC SQL INSERT INTO employees (name, salary) VALUES ('홍길동', 50000.00);
    EXEC SQL INSERT INTO employees (name, salary) VALUES ('김철수', 60000.00);
    EXEC SQL INSERT INTO employees (name) VALUES ('이영희');  /* salary는 NULL */
    EXEC SQL COMMIT;

    printf("데이터 삽입 완료\n");

    /* 커서를 사용하여 데이터 조회 */
    EXEC SQL DECLARE emp_cursor CURSOR FOR
        SELECT id, name, salary FROM employees ORDER BY id;

    EXEC SQL OPEN emp_cursor;

    /* NOT FOUND 조건에서 루프 탈출 */
    EXEC SQL WHENEVER NOT FOUND DO BREAK;

    printf("\n직원 목록:\n");
    printf("----------------------------------------\n");

    while (1) {
        EXEC SQL FETCH emp_cursor INTO :emp_id, :emp_name, :emp_salary :emp_salary_ind;

        printf("ID: %d, 이름: %s, ", emp_id, emp_name);

        if (emp_salary_ind < 0) {
            printf("급여: (미정)\n");
        } else {
            printf("급여: %.2f\n", emp_salary);
        }
    }

    printf("----------------------------------------\n");

    EXEC SQL CLOSE emp_cursor;

    /* 처리된 행 수 확인 */
    printf("총 %ld 행 처리됨\n", sqlca.sqlerrd[2]);

    /* 정리: 테이블 삭제 (테스트용) */
    EXEC SQL DROP TABLE employees;
    EXEC SQL COMMIT;

    /* 연결 해제 */
    EXEC SQL DISCONNECT ALL;

    printf("\n프로그램 종료\n");

    return 0;
}
```

#### 컴파일 및 실행

```bash
# 전처리
ecpg example.pgc

# 컴파일
cc -c example.c -I$(pg_config --includedir)

# 링크
cc -o example example.o -lecpg -L$(pg_config --libdir)

# 실행
./example
```

#### 예상 출력

```
데이터베이스에 연결됨
현재 데이터베이스: mydb
데이터 삽입 완료

직원 목록:
----------------------------------------
ID: 1, 이름: 홍길동, 급여: 50000.00
ID: 2, 이름: 김철수, 급여: 60000.00
ID: 3, 이름: 이영희, 급여: (미정)
----------------------------------------
총 3 행 처리됨

프로그램 종료
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - ECPG](https://www.postgresql.org/docs/current/ecpg.html)
- [PostgreSQL ECPG 개념](https://www.postgresql.org/docs/current/ecpg-concept.html)
- [PostgreSQL ECPG 연결](https://www.postgresql.org/docs/current/ecpg-connect.html)
- [PostgreSQL ECPG 명령](https://www.postgresql.org/docs/current/ecpg-commands.html)
- [PostgreSQL ECPG 변수](https://www.postgresql.org/docs/current/ecpg-variables.html)
- [PostgreSQL ECPG 동적 SQL](https://www.postgresql.org/docs/current/ecpg-dynamic.html)
- [PostgreSQL ECPG 오류 처리](https://www.postgresql.org/docs/current/ecpg-errors.html)

---

## PostgreSQL 정보 스키마 (Information Schema)

### 개요

정보 스키마(Information Schema)는 현재 데이터베이스에 정의된 객체 정보를 담는 뷰의 집합임. SQL 표준에 정의되어 있으므로 다른 데이터베이스 시스템과의 이식성(portability)과 안정성(stability)이 우수함.

---

### 37.1. 정보 스키마란?

정보 스키마는 `information_schema`라는 이름의 스키마로, 모든 데이터베이스에 자동으로 존재함. 이 스키마의 소유자는 초기 데이터베이스 사용자이며, 삭제하는 것은 권장하지 않음.

#### 시스템 카탈로그와의 차이점

- 표준 준수
  - 정보 스키마 (Information Schema): SQL 표준
  - 시스템 카탈로그 (System Catalog): PostgreSQL 고유
- 이식성
  - 정보 스키마 (Information Schema): 다른 DBMS와 호환
  - 시스템 카탈로그 (System Catalog): PostgreSQL 전용
- 안정성
  - 정보 스키마 (Information Schema): 버전 간 변경이 적음
  - 시스템 카탈로그 (System Catalog): PostgreSQL 내부 변경에 따라 변동 가능
- 상세 정보
  - 정보 스키마 (Information Schema): 표준에 정의된 정보만 제공
  - 시스템 카탈로그 (System Catalog): PostgreSQL 특화 정보까지 제공

#### 사용 방법

정보 스키마의 뷰를 쿼리하려면 `information_schema` 스키마를 명시적으로 지정 필요:

```sql
-- 스키마를 명시적으로 지정
SELECT * FROM information_schema.tables;

-- search_path에 추가하는 방법
SET search_path TO information_schema, public;
SELECT * FROM tables;
```

#### 데이터 타입

정보 스키마는 SQL 표준에 정의된 특별한 데이터 타입을 사용함:

- `sql_identifier`: SQL 식별자를 위한 도메인, `text` 기반
- `character_data`: 문자 데이터를 위한 도메인, `text` 기반
- `cardinal_number`: 음이 아닌 정수를 위한 도메인, `integer` 기반
- `yes_or_no`: 불리언 값을 나타내며, `YES` 또는 `NO` 문자열
- `time_stamp`: 타임스탬프를 위한 도메인

---

### 37.2. 주요 정보 스키마 뷰

#### 37.2.1. information_schema_catalog_name

현재 데이터베이스(카탈로그)의 이름을 포함하는 테이블임. 항상 단일 행만 포함함.

- `catalog_name`
  - 타입: `sql_identifier`
  - 설명: 현재 데이터베이스 이름

```sql
SELECT * FROM information_schema.information_schema_catalog_name;
```

---

### 37.3. schemata - 스키마 정보

`schemata` 뷰는 현재 데이터베이스에 존재하는 모든 스키마에 대한 정보를 제공함. 현재 사용자가 접근 권한을 가진 스키마만 표시됨.

#### 컬럼 정보

- `catalog_name`
  - 타입: `sql_identifier`
  - 설명: 데이터베이스 이름 (항상 현재 데이터베이스)
- `schema_name`
  - 타입: `sql_identifier`
  - 설명: 스키마 이름
- `schema_owner`
  - 타입: `sql_identifier`
  - 설명: 스키마 소유자 이름
- `default_character_set_catalog`
  - 타입: `sql_identifier`
  - 설명: PostgreSQL에서 미지원 (항상 null)
- `default_character_set_schema`
  - 타입: `sql_identifier`
  - 설명: PostgreSQL에서 미지원 (항상 null)
- `default_character_set_name`
  - 타입: `sql_identifier`
  - 설명: PostgreSQL에서 미지원 (항상 null)
- `sql_path`
  - 타입: `character_data`
  - 설명: PostgreSQL에서 미지원 (항상 null)

#### 예제

```sql
-- 모든 스키마 조회
SELECT schema_name, schema_owner
FROM information_schema.schemata
ORDER BY schema_name;

-- 특정 사용자가 소유한 스키마 조회
SELECT schema_name
FROM information_schema.schemata
WHERE schema_owner = 'myuser';
```

---

### 37.4. tables - 테이블 정보

`tables` 뷰는 현재 데이터베이스의 모든 테이블과 뷰에 대한 정보를 제공함. 현재 사용자가 접근 권한을 가진 객체만 표시됨.

#### 컬럼 정보

- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 포함된 데이터베이스 이름
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 포함된 스키마 이름
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `table_type`
  - 타입: `character_data`
  - 설명: 테이블 유형 (아래 표 참조)
- `self_referencing_column_name`
  - 타입: `sql_identifier`
  - 설명: PostgreSQL에서 미지원
- `reference_generation`
  - 타입: `character_data`
  - 설명: PostgreSQL에서 미지원
- `user_defined_type_catalog`
  - 타입: `sql_identifier`
  - 설명: 타입화된 테이블의 기본 타입 데이터베이스
- `user_defined_type_schema`
  - 타입: `sql_identifier`
  - 설명: 타입화된 테이블의 기본 타입 스키마
- `user_defined_type_name`
  - 타입: `sql_identifier`
  - 설명: 타입화된 테이블의 기본 타입 이름
- `is_insertable_into`
  - 타입: `yes_or_no`
  - 설명: 테이블에 삽입 가능 여부
- `is_typed`
  - 타입: `yes_or_no`
  - 설명: 타입화된 테이블 여부
- `commit_action`
  - 타입: `character_data`
  - 설명: 아직 구현되지 않음

#### table_type 값

- `BASE TABLE`: 일반 테이블
- `VIEW`: 뷰
- `FOREIGN`: 외부 테이블 (Foreign Table)
- `LOCAL TEMPORARY`: 임시 테이블

#### 예제

```sql
-- public 스키마의 모든 테이블 조회
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- 일반 테이블만 조회 (뷰 제외)
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_type = 'BASE TABLE'
  AND table_schema NOT IN ('information_schema', 'pg_catalog');

-- 각 스키마별 테이블 개수
SELECT table_schema, COUNT(*) as table_count
FROM information_schema.tables
WHERE table_type = 'BASE TABLE'
GROUP BY table_schema
ORDER BY table_count DESC;
```

---

### 37.5. columns - 컬럼 정보

`columns` 뷰는 데이터베이스의 모든 테이블 및 뷰 컬럼에 대한 상세 정보를 제공함.

#### 기본 컬럼 정보

- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `column_name`
  - 타입: `sql_identifier`
  - 설명: 컬럼 이름
- `ordinal_position`
  - 타입: `cardinal_number`
  - 설명: 테이블 내 컬럼 순서 (1부터 시작)
- `column_default`
  - 타입: `character_data`
  - 설명: 기본값 표현식
- `is_nullable`
  - 타입: `yes_or_no`
  - 설명: NULL 허용 여부
- `data_type`
  - 타입: `character_data`
  - 설명: 데이터 타입 이름

#### 데이터 타입 관련 컬럼

- `character_maximum_length`
  - 타입: `cardinal_number`
  - 설명: 문자/비트 타입의 최대 길이
- `character_octet_length`
  - 타입: `cardinal_number`
  - 설명: 문자 타입의 최대 바이트 수
- `numeric_precision`
  - 타입: `cardinal_number`
  - 설명: 숫자 타입의 정밀도
- `numeric_precision_radix`
  - 타입: `cardinal_number`
  - 설명: 정밀도 기수 (2 또는 10)
- `numeric_scale`
  - 타입: `cardinal_number`
  - 설명: 숫자 타입의 스케일 (소수점 이하 자릿수)
- `datetime_precision`
  - 타입: `cardinal_number`
  - 설명: 날짜/시간 타입의 소수 초 정밀도

#### 도메인 관련 컬럼

- `domain_catalog`
  - 타입: `sql_identifier`
  - 설명: 도메인이 정의된 데이터베이스
- `domain_schema`
  - 타입: `sql_identifier`
  - 설명: 도메인이 정의된 스키마
- `domain_name`
  - 타입: `sql_identifier`
  - 설명: 도메인 이름

#### 식별 컬럼(Identity Column) 관련

- `is_identity`
  - 타입: `yes_or_no`
  - 설명: 식별 컬럼 여부
- `identity_generation`
  - 타입: `character_data`
  - 설명: `ALWAYS` 또는 `BY DEFAULT`
- `identity_start`
  - 타입: `character_data`
  - 설명: 시퀀스 시작값
- `identity_increment`
  - 타입: `character_data`
  - 설명: 시퀀스 증분
- `identity_maximum`
  - 타입: `character_data`
  - 설명: 시퀀스 최대값
- `identity_minimum`
  - 타입: `character_data`
  - 설명: 시퀀스 최소값
- `identity_cycle`
  - 타입: `yes_or_no`
  - 설명: 시퀀스 순환 여부

#### 생성된 컬럼(Generated Column) 관련

- `is_generated`
  - 타입: `character_data`
  - 설명: `ALWAYS` 또는 `NEVER`
- `generation_expression`
  - 타입: `character_data`
  - 설명: 생성 표현식

#### 기타 컬럼

- `udt_catalog`
  - 타입: `sql_identifier`
  - 설명: 컬럼 데이터 타입이 정의된 데이터베이스
- `udt_schema`
  - 타입: `sql_identifier`
  - 설명: 컬럼 데이터 타입이 정의된 스키마
- `udt_name`
  - 타입: `sql_identifier`
  - 설명: 컬럼 데이터 타입 이름
- `is_updatable`
  - 타입: `yes_or_no`
  - 설명: 컬럼 업데이트 가능 여부
- `collation_name`
  - 타입: `sql_identifier`
  - 설명: 콜레이션 이름

#### 예제

```sql
-- 특정 테이블의 컬럼 정보 조회
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'users'
ORDER BY ordinal_position;

-- 모든 TEXT 타입 컬럼 찾기
SELECT
    table_schema,
    table_name,
    column_name
FROM information_schema.columns
WHERE data_type = 'text'
  AND table_schema = 'public';

-- NULL이 허용되지 않는 컬럼 조회
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE is_nullable = 'NO'
  AND table_schema = 'public'
ORDER BY table_name, ordinal_position;

-- 기본값이 있는 컬럼 조회
SELECT
    table_name,
    column_name,
    column_default
FROM information_schema.columns
WHERE column_default IS NOT NULL
  AND table_schema = 'public';

-- 테이블별 컬럼 수 조회
SELECT
    table_name,
    COUNT(*) as column_count
FROM information_schema.columns
WHERE table_schema = 'public'
GROUP BY table_name
ORDER BY column_count DESC;
```

---

### 37.6. views - 뷰 정보

`views` 뷰는 현재 데이터베이스의 모든 뷰에 대한 정보를 제공함.

#### 컬럼 정보

- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 뷰가 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 뷰가 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 뷰 이름
- `view_definition`
  - 타입: `character_data`
  - 설명: 뷰 정의 쿼리 (소유자가 아니면 null)
- `check_option`
  - 타입: `character_data`
  - 설명: CHECK OPTION: `CASCADED`, `LOCAL`, 또는 `NONE`
- `is_updatable`
  - 타입: `yes_or_no`
  - 설명: UPDATE/DELETE 가능 여부
- `is_insertable_into`
  - 타입: `yes_or_no`
  - 설명: INSERT 가능 여부
- `is_trigger_updatable`
  - 타입: `yes_or_no`
  - 설명: INSTEAD OF UPDATE 트리거 존재 여부
- `is_trigger_deletable`
  - 타입: `yes_or_no`
  - 설명: INSTEAD OF DELETE 트리거 존재 여부
- `is_trigger_insertable_into`
  - 타입: `yes_or_no`
  - 설명: INSTEAD OF INSERT 트리거 존재 여부

#### 예제

```sql
-- 모든 뷰 목록 조회
SELECT table_schema, table_name
FROM information_schema.views
WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
ORDER BY table_schema, table_name;

-- 업데이트 가능한 뷰 조회
SELECT table_name, is_updatable, is_insertable_into
FROM information_schema.views
WHERE is_updatable = 'YES'
  AND table_schema = 'public';

-- 뷰 정의 확인 (소유자만 가능)
SELECT table_name, view_definition
FROM information_schema.views
WHERE table_schema = 'public'
  AND view_definition IS NOT NULL;
```

---

### 37.7. 제약조건 관련 뷰

#### 37.7.1. table_constraints - 테이블 제약조건

`table_constraints` 뷰는 테이블에 정의된 모든 제약조건 정보를 제공함.

##### 컬럼 정보

- `constraint_catalog`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 데이터베이스
- `constraint_schema`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 스키마
- `constraint_name`
  - 타입: `sql_identifier`
  - 설명: 제약조건 이름
- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `constraint_type`
  - 타입: `character_data`
  - 설명: 제약조건 유형
- `is_deferrable`
  - 타입: `yes_or_no`
  - 설명: 지연 가능 여부
- `initially_deferred`
  - 타입: `yes_or_no`
  - 설명: 초기 지연 여부
- `enforced`
  - 타입: `yes_or_no`
  - 설명: 제약조건 적용 여부
- `nulls_distinct`
  - 타입: `yes_or_no`
  - 설명: UNIQUE에서 NULL 구분 여부

##### constraint_type 값

- `PRIMARY KEY`: 기본 키 제약조건
- `UNIQUE`: 고유 제약조건
- `FOREIGN KEY`: 외래 키 제약조건
- `CHECK`: 검사 제약조건

##### 예제

```sql
-- 특정 테이블의 모든 제약조건 조회
SELECT
    constraint_name,
    constraint_type,
    is_deferrable,
    initially_deferred
FROM information_schema.table_constraints
WHERE table_schema = 'public'
  AND table_name = 'orders';

-- 모든 기본 키 조회
SELECT
    table_schema,
    table_name,
    constraint_name
FROM information_schema.table_constraints
WHERE constraint_type = 'PRIMARY KEY'
  AND table_schema = 'public';

-- 모든 외래 키 조회
SELECT
    table_name,
    constraint_name
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
  AND table_schema = 'public';
```

#### 37.7.2. check_constraints - CHECK 제약조건

`check_constraints` 뷰는 CHECK 제약조건의 상세 정보를 제공함.

##### 컬럼 정보

- `constraint_catalog`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 데이터베이스
- `constraint_schema`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 스키마
- `constraint_name`
  - 타입: `sql_identifier`
  - 설명: 제약조건 이름
- `check_clause`
  - 타입: `character_data`
  - 설명: CHECK 표현식

> 참고: SQL 표준에서 NOT NULL 제약조건도 CHECK 제약조건으로 간주되어 `CHECK (column_name IS NOT NULL)` 형식으로 이 뷰에 포함됨.

##### 예제

```sql
-- 모든 CHECK 제약조건과 표현식 조회
SELECT
    constraint_name,
    check_clause
FROM information_schema.check_constraints
WHERE constraint_schema = 'public';
```

#### 37.7.3. referential_constraints - 참조 제약조건 (외래 키)

`referential_constraints` 뷰는 외래 키 제약조건의 상세 정보를 제공함.

##### 컬럼 정보

- `constraint_catalog`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 데이터베이스
- `constraint_schema`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 스키마
- `constraint_name`
  - 타입: `sql_identifier`
  - 설명: 제약조건 이름
- `unique_constraint_catalog`
  - 타입: `sql_identifier`
  - 설명: 참조되는 제약조건의 데이터베이스
- `unique_constraint_schema`
  - 타입: `sql_identifier`
  - 설명: 참조되는 제약조건의 스키마
- `unique_constraint_name`
  - 타입: `sql_identifier`
  - 설명: 참조되는 UNIQUE/PK 제약조건 이름
- `match_option`
  - 타입: `character_data`
  - 설명: 매칭 옵션: `FULL`, `PARTIAL`, `NONE`
- `update_rule`
  - 타입: `character_data`
  - 설명: UPDATE 규칙
- `delete_rule`
  - 타입: `character_data`
  - 설명: DELETE 규칙

##### update_rule / delete_rule 값

- `CASCADE`: 참조 행을 함께 변경/삭제
- `SET NULL`: 참조 컬럼을 NULL로 설정
- `SET DEFAULT`: 참조 컬럼을 기본값으로 설정
- `RESTRICT`: 참조하는 행이 있으면 거부
- `NO ACTION`: RESTRICT와 유사하지만 지연 가능

##### 예제

```sql
-- 외래 키와 참조 관계 조회
SELECT
    constraint_name,
    unique_constraint_name,
    update_rule,
    delete_rule
FROM information_schema.referential_constraints
WHERE constraint_schema = 'public';
```

#### 37.7.4. key_column_usage - 키 컬럼 사용

`key_column_usage` 뷰는 PRIMARY KEY, UNIQUE, FOREIGN KEY 제약조건에 사용되는 컬럼을 식별함.

##### 컬럼 정보

- `constraint_catalog`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 데이터베이스
- `constraint_schema`
  - 타입: `sql_identifier`
  - 설명: 제약조건이 속한 스키마
- `constraint_name`
  - 타입: `sql_identifier`
  - 설명: 제약조건 이름
- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `column_name`
  - 타입: `sql_identifier`
  - 설명: 컬럼 이름
- `ordinal_position`
  - 타입: `cardinal_number`
  - 설명: 키 내 컬럼 순서 (1부터 시작)
- `position_in_unique_constraint`
  - 타입: `cardinal_number`
  - 설명: FK의 경우 참조 컬럼의 순서

##### 예제

```sql
-- 기본 키 컬럼 조회
SELECT
    tc.table_name,
    kcu.column_name,
    kcu.ordinal_position
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'PRIMARY KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.ordinal_position;

-- 외래 키 관계 상세 조회
SELECT
    kcu.table_name AS fk_table,
    kcu.column_name AS fk_column,
    ccu.table_name AS ref_table,
    ccu.column_name AS ref_column,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
    ON kcu.constraint_name = rc.constraint_name
    AND kcu.constraint_schema = rc.constraint_schema
JOIN information_schema.constraint_column_usage ccu
    ON rc.unique_constraint_name = ccu.constraint_name
    AND rc.unique_constraint_schema = ccu.constraint_schema
WHERE kcu.table_schema = 'public';
```

---

### 37.8. 권한 관련 뷰

#### 37.8.1. table_privileges - 테이블 권한

`table_privileges` 뷰는 테이블에 부여된 모든 권한을 표시함.

##### 컬럼 정보

- `grantor`
  - 타입: `sql_identifier`
  - 설명: 권한을 부여한 역할
- `grantee`
  - 타입: `sql_identifier`
  - 설명: 권한을 받은 역할
- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `privilege_type`
  - 타입: `character_data`
  - 설명: 권한 유형
- `is_grantable`
  - 타입: `yes_or_no`
  - 설명: 권한 재부여 가능 여부
- `with_hierarchy`
  - 타입: `yes_or_no`
  - 설명: 계층 옵션 포함 여부

##### privilege_type 값

- `SELECT`: 조회 권한
- `INSERT`: 삽입 권한
- `UPDATE`: 수정 권한
- `DELETE`: 삭제 권한
- `TRUNCATE`: 테이블 비우기 권한
- `REFERENCES`: 외래 키 참조 권한
- `TRIGGER`: 트리거 생성 권한

##### 예제

```sql
-- 특정 테이블의 모든 권한 조회
SELECT
    grantee,
    privilege_type,
    is_grantable
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND table_name = 'users'
ORDER BY grantee, privilege_type;

-- 특정 사용자의 모든 권한 조회
SELECT
    table_schema,
    table_name,
    privilege_type
FROM information_schema.table_privileges
WHERE grantee = 'myuser'
ORDER BY table_schema, table_name;
```

#### 37.8.2. column_privileges - 컬럼 권한

`column_privileges` 뷰는 컬럼 수준의 권한을 표시함.

##### 컬럼 정보

- `grantor`
  - 타입: `sql_identifier`
  - 설명: 권한을 부여한 역할
- `grantee`
  - 타입: `sql_identifier`
  - 설명: 권한을 받은 역할
- `table_catalog`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 데이터베이스
- `table_schema`
  - 타입: `sql_identifier`
  - 설명: 테이블이 속한 스키마
- `table_name`
  - 타입: `sql_identifier`
  - 설명: 테이블 이름
- `column_name`
  - 타입: `sql_identifier`
  - 설명: 컬럼 이름
- `privilege_type`
  - 타입: `character_data`
  - 설명: 권한 유형 (`SELECT`, `INSERT`, `UPDATE`, `REFERENCES`)
- `is_grantable`
  - 타입: `yes_or_no`
  - 설명: 권한 재부여 가능 여부

##### 예제

```sql
-- 컬럼 수준 권한 조회
SELECT
    table_name,
    column_name,
    grantee,
    privilege_type
FROM information_schema.column_privileges
WHERE table_schema = 'public'
ORDER BY table_name, column_name;
```

---

### 37.9. routines - 함수/프로시저 정보

`routines` 뷰는 현재 데이터베이스의 모든 함수와 프로시저 정보를 제공함.

#### 주요 컬럼 정보

- `specific_catalog`
  - 타입: `sql_identifier`
  - 설명: 함수가 속한 데이터베이스
- `specific_schema`
  - 타입: `sql_identifier`
  - 설명: 함수가 속한 스키마
- `specific_name`
  - 타입: `sql_identifier`
  - 설명: 함수의 고유 식별자 (오버로딩에도 유일)
- `routine_catalog`
  - 타입: `sql_identifier`
  - 설명: 함수가 속한 데이터베이스
- `routine_schema`
  - 타입: `sql_identifier`
  - 설명: 함수가 속한 스키마
- `routine_name`
  - 타입: `sql_identifier`
  - 설명: 함수 이름 (오버로딩 시 중복 가능)
- `routine_type`
  - 타입: `character_data`
  - 설명: `FUNCTION` 또는 `PROCEDURE`
- `data_type`
  - 타입: `character_data`
  - 설명: 반환 데이터 타입
- `routine_body`
  - 타입: `character_data`
  - 설명: `SQL` 또는 `EXTERNAL`
- `routine_definition`
  - 타입: `character_data`
  - 설명: 함수 소스 코드
- `external_language`
  - 타입: `character_data`
  - 설명: 작성 언어 (예: `plpgsql`, `sql`)
- `is_deterministic`
  - 타입: `yes_or_no`
  - 설명: 불변(IMMUTABLE) 함수 여부
- `security_type`
  - 타입: `character_data`
  - 설명: `INVOKER` 또는 `DEFINER`

#### 예제

```sql
-- 모든 사용자 정의 함수 조회
SELECT
    routine_name,
    routine_type,
    data_type,
    external_language
FROM information_schema.routines
WHERE routine_schema = 'public'
ORDER BY routine_name;

-- 프로시저만 조회
SELECT routine_name, routine_definition
FROM information_schema.routines
WHERE routine_type = 'PROCEDURE'
  AND routine_schema = 'public';

-- 특정 언어로 작성된 함수 조회
SELECT routine_name, external_language
FROM information_schema.routines
WHERE external_language = 'plpgsql'
  AND routine_schema = 'public';
```

---

### 37.10. sequences - 시퀀스 정보

`sequences` 뷰는 현재 데이터베이스의 모든 시퀀스 정보를 제공함.

#### 컬럼 정보

- `sequence_catalog`
  - 타입: `sql_identifier`
  - 설명: 시퀀스가 속한 데이터베이스
- `sequence_schema`
  - 타입: `sql_identifier`
  - 설명: 시퀀스가 속한 스키마
- `sequence_name`
  - 타입: `sql_identifier`
  - 설명: 시퀀스 이름
- `data_type`
  - 타입: `character_data`
  - 설명: 시퀀스 데이터 타입
- `numeric_precision`
  - 타입: `cardinal_number`
  - 설명: 정밀도
- `numeric_precision_radix`
  - 타입: `cardinal_number`
  - 설명: 정밀도 기수
- `numeric_scale`
  - 타입: `cardinal_number`
  - 설명: 스케일
- `start_value`
  - 타입: `character_data`
  - 설명: 시작값
- `minimum_value`
  - 타입: `character_data`
  - 설명: 최소값
- `maximum_value`
  - 타입: `character_data`
  - 설명: 최대값
- `increment`
  - 타입: `character_data`
  - 설명: 증분값
- `cycle_option`
  - 타입: `yes_or_no`
  - 설명: 순환 여부

#### 예제

```sql
-- 모든 시퀀스 조회
SELECT
    sequence_name,
    data_type,
    start_value,
    increment,
    maximum_value
FROM information_schema.sequences
WHERE sequence_schema = 'public';

-- 순환하는 시퀀스 조회
SELECT sequence_name
FROM information_schema.sequences
WHERE cycle_option = 'YES';
```

---

### 37.11. 기타 유용한 뷰

#### 37.11.1. domains - 도메인

사용자 정의 도메인에 대한 정보를 제공함.

```sql
SELECT domain_name, data_type, domain_default
FROM information_schema.domains
WHERE domain_schema = 'public';
```

#### 37.11.2. enabled_roles - 활성화된 역할

현재 세션에서 활성화된 역할을 표시함.

```sql
SELECT * FROM information_schema.enabled_roles;
```

#### 37.11.3. applicable_roles - 적용 가능한 역할

현재 사용자에게 적용 가능한 모든 역할을 표시함.

```sql
SELECT * FROM information_schema.applicable_roles;
```

#### 37.11.4. sql_features - SQL 기능

PostgreSQL에서 지원하는 SQL 표준 기능 목록을 제공함.

```sql
-- 지원되는 SQL 기능 조회
SELECT feature_id, feature_name, is_supported
FROM information_schema.sql_features
WHERE is_supported = 'YES'
LIMIT 20;
```

---

### 37.12. 실용적인 쿼리 예제

#### 테이블 스키마 문서화

```sql
-- 테이블 구조 완전 문서화
SELECT
    c.table_name,
    c.column_name,
    c.ordinal_position,
    c.data_type,
    c.character_maximum_length,
    c.numeric_precision,
    c.is_nullable,
    c.column_default,
    CASE
        WHEN pk.column_name IS NOT NULL THEN 'PK'
        ELSE ''
    END as is_pk
FROM information_schema.columns c
LEFT JOIN (
    SELECT kcu.table_name, kcu.column_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'PRIMARY KEY'
      AND tc.table_schema = 'public'
) pk ON c.table_name = pk.table_name
     AND c.column_name = pk.column_name
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;
```

#### 데이터베이스 객체 요약

```sql
-- 스키마별 객체 수 요약
SELECT
    table_schema,
    SUM(CASE WHEN table_type = 'BASE TABLE' THEN 1 ELSE 0 END) as tables,
    SUM(CASE WHEN table_type = 'VIEW' THEN 1 ELSE 0 END) as views,
    SUM(CASE WHEN table_type = 'FOREIGN' THEN 1 ELSE 0 END) as foreign_tables
FROM information_schema.tables
WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
GROUP BY table_schema
ORDER BY table_schema;
```

#### 외래 키 관계 다이어그램용 데이터

```sql
-- ERD 생성용 외래 키 관계 추출
SELECT DISTINCT
    kcu.table_name AS from_table,
    kcu.column_name AS from_column,
    ccu.table_name AS to_table,
    ccu.column_name AS to_column
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
    ON kcu.constraint_name = rc.constraint_name
    AND kcu.constraint_schema = rc.constraint_schema
JOIN information_schema.constraint_column_usage ccu
    ON rc.unique_constraint_name = ccu.constraint_name
    AND rc.unique_constraint_schema = ccu.constraint_schema
WHERE kcu.table_schema = 'public'
ORDER BY from_table, to_table;
```

#### 권한 감사

```sql
-- 테이블별 권한 요약
SELECT
    table_name,
    grantee,
    STRING_AGG(privilege_type, ', ' ORDER BY privilege_type) as privileges
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND grantee != 'postgres'
GROUP BY table_name, grantee
ORDER BY table_name, grantee;
```

---

### 37.13. 주의사항

#### 제약조건 이름 중복 문제

SQL 표준에서는 스키마 내에서 제약조건 이름이 고유해야 하지만, PostgreSQL은 같은 이름의 제약조건을 허용함. 따라서 다음 뷰에서 같은 이름의 제약조건이 여러 개 반환될 수 있음:

- `check_constraints`
- `domain_constraints`
- `referential_constraints`

#### PostgreSQL 고유 정보

정보 스키마는 SQL 표준에 정의된 정보만 제공함. PostgreSQL 고유 기능(파티션, 상속, OID 등)에 대한 정보가 필요하면 시스템 카탈로그(`pg_catalog`)를 직접 쿼리 필요:

```sql
-- PostgreSQL 시스템 카탈로그 사용 예
SELECT relname, relkind, reltuples
FROM pg_catalog.pg_class
WHERE relnamespace = 'public'::regnamespace;
```

#### 성능 고려사항

정보 스키마 뷰는 시스템 카탈로그를 기반으로 한 복잡한 뷰임. 대규모 데이터베이스에서 자주 쿼리하면 성능에 영향을 줄 수 있으므로, 성능이 중요한 경우에는 시스템 카탈로그를 직접 사용하는 편이 더 효율적임.

---

### 37.14. 정보 스키마 뷰 전체 목록

- `administrable_role_authorizations`: 관리 가능한 역할 권한
- `applicable_roles`: 적용 가능한 역할
- `attributes`: 복합 타입 속성
- `character_sets`: 문자 집합
- `check_constraint_routine_usage`: CHECK 제약조건에서 사용된 루틴
- `check_constraints`: CHECK 제약조건
- `collation_character_set_applicability`: 콜레이션과 문자 집합 관계
- `collations`: 콜레이션
- `column_column_usage`: 생성된 컬럼 종속성
- `column_domain_usage`: 도메인을 사용하는 컬럼
- `column_options`: 외부 테이블 컬럼 옵션
- `column_privileges`: 컬럼 권한
- `column_udt_usage`: UDT를 사용하는 컬럼
- `columns`: 컬럼 정보
- `constraint_column_usage`: 제약조건에 사용된 컬럼
- `constraint_table_usage`: 제약조건에 사용된 테이블
- `data_type_privileges`: 데이터 타입 권한
- `domain_constraints`: 도메인 제약조건
- `domain_udt_usage`: UDT를 사용하는 도메인
- `domains`: 도메인
- `element_types`: 배열 요소 타입
- `enabled_roles`: 활성화된 역할
- `foreign_data_wrapper_options`: FDW 옵션
- `foreign_data_wrappers`: FDW
- `foreign_server_options`: 외부 서버 옵션
- `foreign_servers`: 외부 서버
- `foreign_table_options`: 외부 테이블 옵션
- `foreign_tables`: 외부 테이블
- `information_schema_catalog_name`: 현재 데이터베이스 이름
- `key_column_usage`: 키 컬럼 사용
- `parameters`: 루틴 매개변수
- `referential_constraints`: 참조 제약조건
- `role_column_grants`: 역할별 컬럼 권한
- `role_routine_grants`: 역할별 루틴 권한
- `role_table_grants`: 역할별 테이블 권한
- `role_udt_grants`: 역할별 UDT 권한
- `role_usage_grants`: 역할별 사용 권한
- `routine_column_usage`: 루틴에서 사용된 컬럼
- `routine_privileges`: 루틴 권한
- `routine_routine_usage`: 루틴에서 사용된 루틴
- `routine_sequence_usage`: 루틴에서 사용된 시퀀스
- `routine_table_usage`: 루틴에서 사용된 테이블
- `routines`: 함수/프로시저
- `schemata`: 스키마
- `sequences`: 시퀀스
- `sql_features`: SQL 기능
- `sql_implementation_info`: SQL 구현 정보
- `sql_parts`: SQL 표준 부분
- `sql_sizing`: SQL 크기 제한
- `table_constraints`: 테이블 제약조건
- `table_privileges`: 테이블 권한
- `tables`: 테이블
- `transforms`: 타입 변환
- `triggered_update_columns`: 트리거 업데이트 컬럼
- `triggers`: 트리거
- `udt_privileges`: UDT 권한
- `usage_privileges`: 사용 권한
- `user_defined_types`: 사용자 정의 타입
- `user_mapping_options`: 사용자 매핑 옵션
- `user_mappings`: 사용자 매핑
- `view_column_usage`: 뷰에서 사용된 컬럼
- `view_routine_usage`: 뷰에서 사용된 루틴
- `view_table_usage`: 뷰에서 사용된 테이블
- `views`: 뷰

---

### 참고 자료

- [PostgreSQL 공식 문서 - The Information Schema](https://www.postgresql.org/docs/current/information-schema.html)
- [SQL:2016 표준 - Information Schema](https://www.iso.org/standard/63555.html)
