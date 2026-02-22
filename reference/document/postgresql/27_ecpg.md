# Chapter 36: ECPG - C 언어 내장 SQL (Embedded SQL in C)

## 목차

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

## 1. 개요

ECPG(Embedded SQL in C)는 PostgreSQL의 C 언어(및 제한적으로 C++) 내장 SQL 패키지입니다. Linus Tolke와 Michael Meskes가 개발했으며, SQL 표준을 따라 개발자가 C 코드 내에 SQL을 직접 삽입할 수 있게 합니다.

### 주요 특징

- 표준화된 SQL 인터페이스: C 언어에 SQL을 내장
- 트랜잭션 관리: 완전한 트랜잭션 제어 지원
- 다중 연결 지원: 여러 데이터베이스에 동시 연결
- 타입 매핑: SQL과 C 데이터 타입 간 자동 변환
- 콜백을 통한 오류 처리: WHENEVER 문을 사용한 예외 처리
- 동적 SQL 실행: 런타임에 SQL 문 구성 및 실행
- 호환성 모드: Informix, Oracle 호환성 지원
- 대용량 객체 지원: LOB(Large Object) 처리

---

## 2. ECPG 개념

### 2.1 동작 원리

ECPG 프로그램은 다음 단계를 거쳐 처리됩니다:

```
1. 소스 코드 작성: *.pgc 파일에 C 코드와 SQL 혼합
2. 전처리: ecpg 전처리기가 *.pgc를 표준 C(*.c) 파일로 변환
3. 컴파일: 일반 C 컴파일러로 처리
4. 실행: 변환된 ECPG 애플리케이션은 ecpglib 라이브러리를 통해
        libpq 함수를 호출하고 PostgreSQL과 통신
```

### 2.2 SQL 문 구문

모든 내장 SQL 문은 다음 형식을 따릅니다:

```c
EXEC SQL ...;
```

이 문장들은 구문적으로 일반 C 문장을 대체하며, 전역 수준과 함수 내부 모두에서 사용할 수 있습니다.

### 2.3 ECPG의 장점

1. 단순화된 데이터 처리: C 변수와의 데이터 전달을 자동으로 관리
2. 빌드 타임 검증: SQL 코드가 컴파일 시점에 구문 검사됨
3. 표준 준수: SQL 표준에 명시되어 많은 데이터베이스 시스템에서 지원
4. 이식성: 다른 SQL 데이터베이스용 프로그램을 PostgreSQL로 쉽게 포팅 가능

### 2.4 중요한 구문 규칙

- 대소문자 구분: 내장 SQL은 C가 아닌 SQL 대소문자 규칙을 따름
- 주석: SQL 섹션에서는 SQL 표준에 따라 중첩된 C 스타일 주석 허용
- 문자열/식별자 파싱: C가 아닌 SQL 규칙 사용
- 문자열 상수: ECPG는 `standard_conforming_strings`가 활성화되어 있다고 가정

---

## 3. 데이터베이스 연결 관리

### 3.1 데이터베이스 서버 연결

#### 기본 구문

```c
EXEC SQL CONNECT TO target [AS connection-name] [USER user-name];
```

#### target 지정 방법

- `dbname[@hostname][:port]`
- `tcp:postgresql://hostname[:port][/dbname][?options]`
- `unix:postgresql://localhost[:port][/dbname][?options]`
- 위 형식 중 하나를 포함하는 SQL 문자열 리터럴
- 문자 변수에 대한 참조
- `DEFAULT` (기본 데이터베이스에 기본 사용자로 연결)

#### 사용자 이름 지정 방법

- `username`
- `username/password`
- `username IDENTIFIED BY password`
- `username USING password`

#### 연결 예제

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

#### 보안 참고사항

신뢰할 수 없는 사용자 접근의 경우, `search_path`에서 공개적으로 쓰기 가능한 스키마를 제거하세요:

```c
EXEC SQL SELECT pg_catalog.set_config('search_path', '', false);
```

### 3.2 연결 선택

여러 연결을 관리할 때 세 가지 방법이 있습니다:

#### 방법 1: 명시적 AT 절

```c
EXEC SQL AT connection-name SELECT ...;
```

여러 연결을 혼합하여 사용해야 할 때 가장 적합합니다.

#### 방법 2: SET CONNECTION 문

```c
EXEC SQL SET CONNECTION connection-name;
```

같은 연결에서 많은 문장을 실행할 때 가장 적합합니다.

#### 완전한 예제 프로그램

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

#### 방법 3: 연결과 함께 DECLARE STATEMENT 사용

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

#### 스레딩 참고사항

여러 스레드가 동시에 연결을 공유할 수 없습니다. 접근 제어를 위한 뮤텍스를 사용하거나 스레드당 별도의 연결을 사용하세요.

### 3.3 연결 닫기

#### 구문

```c
EXEC SQL DISCONNECT [connection];
```

#### 연결 지정

- `connection-name` - 지정된 연결 닫기
- `CURRENT` - 현재 연결 닫기
- `ALL` - 모든 연결 닫기
- 생략 - 현재 연결 닫기 (기본값)

#### 모범 사례

적절한 리소스 관리를 위해 항상 열린 모든 연결에서 명시적으로 연결을 해제하세요.

```c
EXEC SQL DISCONNECT ALL;
```

---

## 4. SQL 명령 실행

### 4.1 SQL 문 실행

SQL 명령은 `EXEC SQL`을 사용하여 직접 실행할 수 있습니다:

#### 테이블 생성

```c
EXEC SQL CREATE TABLE foo (number integer, ascii char(16));
EXEC SQL CREATE UNIQUE INDEX num1 ON foo(number);
EXEC SQL COMMIT;
```

#### 행 삽입

```c
EXEC SQL INSERT INTO foo (number, ascii) VALUES (9999, 'doodad');
EXEC SQL COMMIT;
```

#### 행 삭제

```c
EXEC SQL DELETE FROM foo WHERE number = 9999;
EXEC SQL COMMIT;
```

#### 행 업데이트

```c
EXEC SQL UPDATE foo
    SET ascii = 'foobar'
    WHERE number = 9999;
EXEC SQL COMMIT;
```

#### 단일 행 SELECT

```c
EXEC SQL SELECT foo INTO :FooBar FROM table1 WHERE ascii = 'doodad';
```

#### 설정 매개변수 조회

```c
EXEC SQL SHOW search_path INTO :var;
```

> 참고: `:something`과 같은 토큰은 C 프로그램 변수를 참조하는 호스트 변수 입니다.

### 4.2 커서 사용

여러 행을 반환하는 결과 집합에는 커서를 사용합니다:

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

> 중요: `ECPG DECLARE` 명령은 PostgreSQL에 문장을 보내지 않습니다. 커서는 실제로 `OPEN`이 실행될 때 백엔드에서 열립니다.

### 4.3 트랜잭션 관리

| 명령 | 목적 |
|------|------|
| `EXEC SQL COMMIT` | 진행 중인 트랜잭션 커밋 |
| `EXEC SQL ROLLBACK` | 진행 중인 트랜잭션 롤백 |
| `EXEC SQL PREPARE TRANSACTION transaction_id` | 2단계 커밋 준비 |
| `EXEC SQL COMMIT PREPARED transaction_id` | 준비된 트랜잭션 커밋 |
| `EXEC SQL ROLLBACK PREPARED transaction_id` | 준비된 트랜잭션 롤백 |
| `EXEC SQL SET AUTOCOMMIT TO ON` | 자동 커밋 모드 활성화 |
| `EXEC SQL SET AUTOCOMMIT TO OFF` | 자동 커밋 모드 비활성화 (기본값) |

기본 모드: 문장은 `EXEC SQL COMMIT`이 실행될 때만 커밋됩니다.

자동 커밋 모드: `ecpg`에 `-t` 명령줄 옵션을 전달하거나 `EXEC SQL SET AUTOCOMMIT TO ON`으로 활성화할 수 있습니다. 명시적 트랜잭션 블록 내부가 아닌 한 각 명령이 자동으로 커밋됩니다.

### 4.4 준비된 문장 (Prepared Statements)

알 수 없는 값이나 재사용 가능한 문장의 경우 준비된 문장을 사용합니다:

#### 문장 준비

```c
EXEC SQL PREPARE stmt1 FROM "SELECT oid, datname FROM pg_database WHERE oid = ?";
```

#### 단일 행 결과 실행

```c
EXEC SQL EXECUTE stmt1 INTO :dboid, :dbname USING 1;
```

#### 준비된 문장과 커서 사용

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

#### 준비된 문장 해제

```c
EXEC SQL DEALLOCATE PREPARE name;
```

알 수 없는 값에 대해 `?`를 플레이스홀더로 사용하고 `USING` 절을 통해 실제 값을 제공합니다.

---

## 5. 호스트 변수

### 5.1 개요

호스트 변수(Host Variables)는 C 프로그램과 PostgreSQL 간에 데이터를 교환하기 위해 내장 SQL 문에서 사용되는 C 프로그램 변수입니다. SQL 문은 C 프로그램 코드("호스트 언어") 내의 "손님"으로 간주되므로 C 변수를 호스트 변수 라고 합니다.

#### 기본 구문

```c
EXEC SQL INSERT INTO sometable VALUES (:v1, 'foo', :v2);
```

SQL 문에서 사용될 때 변수 앞에 콜론(`:`)을 붙입니다.

### 5.2 선언 섹션 (Declare Sections)

전처리기가 호스트 변수를 인식할 수 있도록 특별히 표시된 섹션에서 선언해야 합니다.

#### 구문

```c
EXEC SQL BEGIN DECLARE SECTION;
    int x = 4;
    char foo[16], bar[16];
EXEC SQL END DECLARE SECTION;
```

#### 대안적 암시적 구문

```c
EXEC SQL int i = 4;
```

#### 핵심 사항

- 변수는 선택적 초기값을 가질 수 있음
- 범위는 프로그램 내 섹션의 위치에 따라 결정됨
- 여러 선언 섹션 허용
- 선언은 일반 C 변수로 출력 파일에 그대로 복사됨
- 구조체와 공용체는 `DECLARE` 섹션 내부에서 선언해야 함

### 5.3 쿼리 결과 가져오기

#### SELECT (단일 행)

select 목록과 `FROM` 절 사이에 `INTO` 절을 사용합니다:

```c
EXEC SQL BEGIN DECLARE SECTION;
    int v1;
    VARCHAR v2;
EXEC SQL END DECLARE SECTION;

EXEC SQL SELECT a, b INTO :v1, :v2 FROM test;
```

#### FETCH (커서를 사용한 여러 행)

모든 일반 절 뒤에 `INTO` 절을 사용합니다:

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

### 5.4 타입 매핑

PostgreSQL 데이터 타입은 해당하는 C 변수 타입에 매핑되어야 합니다:

| PostgreSQL 타입 | C 변수 타입 |
|----------------|-------------|
| `smallint` | `short` |
| `integer` | `int` |
| `bigint` | `long long int` |
| `real` | `float` |
| `double precision` | `double` |
| `character(n)`, `varchar(n)`, `text` | `char[n+1]`, `VARCHAR[n+1]` |
| `boolean` | `bool` |
| `timestamp` | `timestamp`* |
| `date` | `date`* |
| `interval` | `interval`* |
| `numeric`, `decimal` | `numeric`, `decimal`* |
| `bytea` | `char *`, `bytea[n]` |

*pgtypes 라이브러리 함수가 필요한 특수 타입

### 5.5 문자열 처리

#### char[] 사용

```c
EXEC SQL BEGIN DECLARE SECTION;
    char str[50];
EXEC SQL END DECLARE SECTION;
```

주의: 버퍼 오버플로우를 피하기 위해 길이를 수동으로 관리해야 합니다.

#### VARCHAR 사용

```c
VARCHAR var[180];
```

다음과 같이 변환됩니다:

```c
struct varchar_var {
    int len;        /* null 종결자 제외 길이 */
    char arr[180];  /* null 종결자가 있는 문자열 */
} var;
```

입력으로 사용될 때 `strlen(arr)`과 `len` 중 더 짧은 것이 사용됩니다.

### 5.6 특수 데이터 타입

#### Timestamp과 Date

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

#### Interval (힙 할당)

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

#### Numeric과 Decimal

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

### 5.7 호스트 변수로서의 배열

#### 배열에 여러 행 가져오기

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

### 5.8 호스트 변수로서의 구조체

일치하는 구조체 멤버로 여러 열 가져오기:

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

### 5.9 NULL 처리를 위한 인디케이터

인디케이터는 null 값과 잘림을 추적합니다:

```c
EXEC SQL BEGIN DECLARE SECTION;
    VARCHAR val;
    int val_ind;
EXEC SQL END DECLARE SECTION;

EXEC SQL SELECT b INTO :val :val_ind FROM test1;
```

#### 인디케이터 값

| 값 | 의미 |
|----|------|
| `0` | 값이 null이 아님 |
| 음수 | 값이 null (실제 호스트 변수 무시됨) |
| 양수 | 값이 null이 아니지만 잘림 |

#### no-indicator 모드

전처리기에 `-r no_indicator`를 전달합니다. null 값은 다음과 같이 신호됩니다:
- 문자 타입의 경우 빈 문자열
- 정수 타입의 경우 가능한 가장 낮은 값 (예: `INT_MIN`)

### 5.10 복합 타입 예제

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

## 6. 동적 SQL

동적 SQL을 사용하면 애플리케이션이 런타임에 구성되거나 외부 소스에서 제공된 SQL 문을 실행할 수 있습니다. ECPG는 동적 SQL 문을 실행하기 위한 세 가지 주요 접근 방식을 제공합니다.

### 6.1 결과 집합 없이 문장 실행

명령: `EXECUTE IMMEDIATE`

결과를 반환하지 않는 SQL 문(DDL, INSERT, UPDATE, DELETE)에 사용합니다.

```c
EXEC SQL BEGIN DECLARE SECTION;
const char *stmt = "CREATE TABLE test1 (...);";
EXEC SQL END DECLARE SECTION;

EXEC SQL EXECUTE IMMEDIATE :stmt;
```

제한사항: SELECT나 데이터를 검색하는 다른 문장은 실행할 수 없습니다.

### 6.2 입력 매개변수가 있는 문장 실행

접근 방식: 문장을 한 번 준비하고 다른 매개변수로 여러 번 실행합니다.

매개변수에 대한 플레이스홀더로 물음표(`?`)를 사용합니다:

```c
EXEC SQL BEGIN DECLARE SECTION;
const char *stmt = "INSERT INTO test1 VALUES(?, ?);";
EXEC SQL END DECLARE SECTION;

EXEC SQL PREPARE mystmt FROM :stmt;
...
EXEC SQL EXECUTE mystmt USING 42, 'foobar';
```

정리: 더 이상 필요하지 않은 준비된 문장은 해제합니다:

```c
EXEC SQL DEALLOCATE PREPARE name;
```

### 6.3 결과 집합이 있는 문장 실행

#### 단일 결과 행

`EXECUTE`에 `INTO` 절을 사용하여 결과를 저장합니다:

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

#### 여러 결과 행

여러 행을 반환하는 쿼리에는 커서 를 사용합니다:

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

### 6.4 EXECUTE 절 조합

`EXECUTE` 명령은 다음을 가질 수 있습니다:
- `INTO` 절만
- `USING` 절만
- `INTO`와 `USING` 절 모두
- 둘 다 없음

---

## 7. 오류 처리

ECPG는 예외와 경고를 처리하기 위한 두 가지 비배타적 기능을 제공합니다:
1. 콜백 - `WHENEVER` 명령 사용
2. 상세 정보 - `sqlca` 변수 접근

### 7.1 WHENEVER로 콜백 설정

#### 구문

```c
EXEC SQL WHENEVER condition action;
```

#### 조건 (Conditions)

| 조건 | 설명 |
|------|------|
| `SQLERROR` | SQL 문 실행 중 오류 발생 시 호출 |
| `SQLWARNING` | SQL 문 실행 중 경고 발생 시 호출 |
| `NOT FOUND` | SQL 문이 0개의 행을 검색하거나 영향을 줄 때 호출 |

#### 액션 (Actions)

| 액션 | 설명 |
|------|------|
| `CONTINUE` | 조건 무시 (기본값) |
| `GOTO label` / `GO TO label` | C `goto`를 사용하여 지정된 레이블로 점프 |
| `SQLPRINT` | stderr에 메시지 출력 |
| `STOP` | `exit(1)` 호출하여 프로그램 종료 |
| `DO BREAK` | C `break` 문 실행 (루프/switch에서만) |
| `DO CONTINUE` | C `continue` 문 실행 (루프에서만) |
| `CALL name(args)` / `DO name(args)` | 지정된 C 함수 호출 |

#### 기본 예제

```c
EXEC SQL WHENEVER SQLWARNING SQLPRINT;
EXEC SQL WHENEVER SQLERROR STOP;
```

#### 중요 참고사항

`EXEC SQL WHENEVER`는 C 문이 아닌 전처리기 지시문 입니다. 오류 핸들러는 C 제어 흐름에 관계없이 설정된 위치 아래 의 모든 내장 SQL 문에 적용됩니다. 다음 패턴은 작동하지 않습니다:

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

### 7.2 sqlca 변수

#### 구조체 정의

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

#### 주요 필드

| 필드 | 목적 |
|------|------|
| `sqlcode` | 오류 코드 (0 = 성공, 음수 = 오류, 양수 = 무해한 조건) |
| `sqlstate` | 5문자 오류 코드 (sqlcode보다 선호됨) |
| `sqlerrm.sqlerrmc` | 오류 메시지 문자열 |
| `sqlerrm.sqlerrml` | 오류 메시지 길이 |
| `sqlerrd[1]` | 처리된 행의 OID |
| `sqlerrd[2]` | 처리/반환된 행 수 |
| `sqlwarn[0]` | 경고가 있으면 'W'로 설정 |
| `sqlwarn[1]` | 값이 잘리면 'W'로 설정 |
| `sqlwarn[2]` | 경고에 대해 'W'로 설정 |

#### 멀티스레딩

멀티스레드 프로그램에서 각 스레드는 자동으로 `sqlca`의 자체 복사본을 갖습니다 (`errno`와 유사).

#### 예제: sqlca 내용 출력

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

#### 예제 출력

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

### 7.3 SQLSTATE vs. SQLCODE

#### SQLSTATE (선호됨)

- 5문자 배열 (숫자 또는 대문자)
- 계층적: 처음 2문자 = 일반 클래스, 마지막 3문자 = 서브클래스
- 성공 코드: `00000`
- 표준 기반: SQL 표준에 정의, PostgreSQL에서 기본 지원
- 이식성: 다른 SQL 구현 간 더 나은 호환성

#### SQLCODE (사용 중단됨)

- 단순 정수 값
- 체계: 0 = 성공, 양수 = 정보와 함께 성공, 음수 = 오류
- 제한된 이식성: +100 이상은 표준화되지 않음
- 계층 구조 없음
- 참고: SQL-92 표준에서 사용 중단됨

#### 권장사항

새 애플리케이션에는 SQLSTATE 사용 - 더 나은 이식성과 일관성을 제공합니다.

### 7.4 SQLCODE 오류 코드 참조

#### 성공

```c
0 (ECPG_NO_ERROR) -> SQLSTATE 00000
100 (ECPG_NOT_FOUND) -> SQLSTATE 02000
```

#### 예제: NOT FOUND로 루프 감지

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

#### 메모리 및 시스템 오류

```c
-12 (ECPG_OUT_OF_MEMORY) -> SQLSTATE YE001
-200 (ECPG_UNSUPPORTED) -> SQLSTATE YE002
```

#### 인수 불일치 오류

```c
-201 (ECPG_TOO_MANY_ARGUMENTS) -> SQLSTATE 07001 또는 07002
-202 (ECPG_TOO_FEW_ARGUMENTS) -> SQLSTATE 07001 또는 07002
```

#### 데이터 타입 변환 오류

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

#### Null 처리

```c
-213 (ECPG_MISSING_INDICATOR) -> SQLSTATE 22002
```

#### 배열 오류

```c
-214 (ECPG_NO_ARRAY) -> SQLSTATE 42804
-215 (ECPG_DATA_NOT_ARRAY) -> SQLSTATE 42804
-216 (ECPG_ARRAY_INSERT) -> SQLSTATE 42804
```

#### 연결 오류

```c
-220 (ECPG_NO_CONN) -> SQLSTATE 08003
-221 (ECPG_NOT_CONN) -> SQLSTATE YE002
```

#### 문장 및 디스크립터 오류

```c
-230 (ECPG_INVALID_STMT) -> SQLSTATE 26000
-240 (ECPG_UNKNOWN_DESCRIPTOR) -> SQLSTATE 33000
-241 (ECPG_INVALID_DESCRIPTOR_INDEX) -> SQLSTATE 07009
-242 (ECPG_UNKNOWN_DESCRIPTOR_ITEM) -> SQLSTATE YE002
```

#### 제약 조건 및 쿼리 오류

```c
-239 (ECPG_INFORMIX_DUPLICATE_KEY) -> SQLSTATE 23505
-243 (ECPG_VAR_NOT_NUMERIC) -> SQLSTATE 07006
-244 (ECPG_VAR_NOT_CHAR) -> SQLSTATE 07006
-284 (ECPG_INFORMIX_SUBSELECT_NOT_ONE) -> SQLSTATE 21000
-403 (ECPG_DUPLICATE_KEY) -> SQLSTATE 23505
-404 (ECPG_SUBSELECT_NOT_ONE) -> SQLSTATE 21000
```

#### PostgreSQL 서버 오류

```c
-400 (ECPG_PGSQL) -> PostgreSQL 서버 오류 메시지 포함
-401 (ECPG_TRANS) -> 트랜잭션 오류 -> SQLSTATE 08007
-402 (ECPG_CONNECT) -> 연결 실패 -> SQLSTATE 08001
```

#### 포털/커서 경고

```c
-602 (ECPG_WARNING_UNKNOWN_PORTAL) -> SQLSTATE 34000
-603 (ECPG_WARNING_IN_TRANSACTION) -> SQLSTATE 25001
-604 (ECPG_WARNING_NO_TRANSACTION) -> SQLSTATE 25P01
-605 (ECPG_WARNING_PORTAL_EXISTS) -> SQLSTATE 42P03
```

---

## 8. 전처리기 지시문

PostgreSQL ECPG 전처리기는 파일 파싱 및 처리 방법을 수정하는 여러 지시문을 지원합니다.

### 8.1 파일 포함

`EXEC SQL INCLUDE` 지시문을 사용하여 외부 파일을 포함합니다:

```c
EXEC SQL INCLUDE filename;
EXEC SQL INCLUDE <filename>;
EXEC SQL INCLUDE "filename";
```

#### 주요 동작

- 전처리기는 `.h`가 이미 없으면 파일 이름에 추가합니다
- 검색 순서:
  1. 현재 디렉토리
  2. `/usr/local/include`
  3. PostgreSQL 포함 디렉토리 (예: `/usr/local/pgsql/include`)
  4. `/usr/include`
- `EXEC SQL INCLUDE "filename"` 사용 시 현재 디렉토리만 검색됩니다
- 포함된 파일이 전처리되므로 내장 SQL 문이 올바르게 처리됩니다
- C의 `#include`와 동일하지 않음 - C의 #include는 SQL 명령을 전처리하지 않음

참고: 포함 파일 이름은 대소문자를 구분합니다.

### 8.2 define 및 undef 지시문

내장 SQL에서 사용할 상수를 정의합니다:

```c
EXEC SQL DEFINE name;
EXEC SQL DEFINE name value;
EXEC SQL DEFINE HAVE_FEATURE;
EXEC SQL DEFINE MYNUMBER 12;
EXEC SQL DEFINE MYSTRING 'abc';
```

`UNDEF`로 정의를 제거합니다:

```c
EXEC SQL UNDEF MYNUMBER;
```

#### C `#define`과의 주요 차이점

- `EXEC SQL DEFINE` 값은 ecpg 전처리기에 의해 평가되고 컴파일 전에 대체됩니다
- 내장 SQL 쿼리에서 사용되는 상수에 C의 `#define`을 사용할 수 없습니다
- `EXEC SQL DEFINE`/`UNDEF`의 효과는 여러 입력 파일에 걸쳐 전달되지 않습니다; 각 파일은 `-D` 명령줄 심볼만으로 새로 시작합니다

### 8.3 조건부 컴파일 지시문

조건부 코드 컴파일을 위해 이러한 지시문을 사용합니다:

```c
EXEC SQL ifdef name;      /* name이 정의되어 있으면 처리 */
EXEC SQL ifndef name;     /* name이 정의되어 있지 않으면 처리 */
EXEC SQL elif name;       /* 대안 섹션 (여러 개 허용) */
EXEC SQL else;            /* 최종 대안 섹션 */
EXEC SQL endif;           /* 조건부 블록 종료 */
```

#### 특징

- 구조는 최대 127 레벨까지 중첩 가능
- `elif` 섹션은 name이 정의되어 있고 이전 섹션이 처리되지 않은 경우에만 처리됨

#### 예제

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

## 9. 처리 및 컴파일

### 9.1 개요

ECPG 프로그램은 컴파일 전에 전처리가 필요합니다. 워크플로우는 다음과 같습니다:

1. `ecpg` 도구로 SQL 문 전처리
2. 생성된 C 코드 컴파일
3. `libecpg` 라이브러리와 링크

### 9.2 단계별 프로세스

#### 1. 전처리

`ecpg` 전처리기는 내장 SQL 문을 특수 함수 호출로 변환합니다.

명령:
```bash
ecpg prog1.pgc
```

입력 파일 `prog1.pgc`에서 `prog1.c`를 생성합니다. ECPG 프로그램은 일반적으로 `.pgc` 확장자를 사용합니다.

사용자 지정 출력 파일을 지정하려면:
```bash
ecpg -o output.c input.pgc
```

#### 2. 컴파일

전처리된 C 파일을 정상적으로 컴파일합니다:

```bash
cc -c prog1.c
```

중요: 기본 검색 위치에 없는 경우 PostgreSQL 헤더 경로를 포함합니다:
```bash
cc -c prog1.c -I/usr/local/pgsql/include
```

#### 3. 링크

`libecpg` 라이브러리와 링크합니다:

```bash
cc -o myprog prog1.o prog2.o ... -lecpg
```

필요한 경우 라이브러리 경로를 추가합니다:
```bash
cc -o myprog prog1.o prog2.o ... -lecpg -L/usr/local/pgsql/lib
```

### 9.3 설치 경로 찾기

PostgreSQL 설치를 찾으려면 `pg_config` 또는 `pkg-config`를 사용합니다:

```bash
pg_config --includedir
pg_config --libdir
# 또는
pkg-config --cflags --libs libecpg
```

### 9.4 Make 통합

대규모 프로젝트의 경우 Makefile에 이 암시적 규칙을 추가합니다:

```makefile
ECPG = ecpg

%.c: %.pgc
        $(ECPG) $<
```

### 9.5 스레딩 지원

`libecpg` 라이브러리는 기본적으로 스레드 안전 하지만, 클라이언트 코드를 컴파일할 때 스레딩 컴파일러 플래그를 추가해야 할 수 있습니다.

---

## 10. 완전한 예제 프로그램

다음은 ECPG의 여러 기능을 보여주는 완전한 예제입니다:

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

### 컴파일 및 실행

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

### 예상 출력

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

## 참고 자료

- [PostgreSQL 공식 문서 - ECPG](https://www.postgresql.org/docs/current/ecpg.html)
- [PostgreSQL ECPG 개념](https://www.postgresql.org/docs/current/ecpg-concept.html)
- [PostgreSQL ECPG 연결](https://www.postgresql.org/docs/current/ecpg-connect.html)
- [PostgreSQL ECPG 명령](https://www.postgresql.org/docs/current/ecpg-commands.html)
- [PostgreSQL ECPG 변수](https://www.postgresql.org/docs/current/ecpg-variables.html)
- [PostgreSQL ECPG 동적 SQL](https://www.postgresql.org/docs/current/ecpg-dynamic.html)
- [PostgreSQL ECPG 오류 처리](https://www.postgresql.org/docs/current/ecpg-errors.html)
