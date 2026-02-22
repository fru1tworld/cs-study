# PostgreSQL 절차적 언어 (Procedural Languages)

## 목차
1. [개요](#1-개요)
2. [절차적 언어 설치](#2-절차적-언어-설치)
3. [PL/pgSQL](#3-plpgsql)
4. [PL/Tcl](#4-pltcl)
5. [PL/Perl](#5-plperl)
6. [PL/Python](#6-plpython)
7. [절차적 언어 보안](#7-절차적-언어-보안)

---

## 1. 개요

### 1.1 절차적 언어란?

PostgreSQL은 SQL과 C 외에도 다른 언어로 사용자 정의 함수(User-Defined Functions)를 작성할 수 있습니다. 이러한 언어들을 일반적으로 절차적 언어(Procedural Languages, PL) 라고 합니다.

절차적 언어로 작성된 함수의 경우, 데이터베이스 서버는 함수의 소스 텍스트를 해석하는 방법에 대한 기본 제공 지식이 없습니다. 대신 이 작업은 해당 언어의 세부 사항을 알고 있는 특수한 핸들러(Handler) 에게 전달됩니다.

### 1.2 핸들러의 작동 방식

핸들러는 다음과 같이 작동합니다:

- 직접 처리: 구문 분석(Parsing), 구문 분석(Syntax Analysis), 실행(Execution) 등의 모든 작업을 수행
- 중개 역할: PostgreSQL과 기존 프로그래밍 언어 구현 간의 "접착제(Glue)" 역할

핸들러 자체는 공유 객체(Shared Object)로 컴파일되고 필요 시 로드되는 C 언어 함수입니다.

### 1.3 표준 배포판의 절차적 언어

PostgreSQL 표준 배포판에는 현재 4가지 절차적 언어가 포함되어 있습니다:

| 언어 | 설명 | 신뢰 여부 |
|------|------|-----------|
| PL/pgSQL | SQL 절차적 언어 | Trusted |
| PL/Tcl | Tcl 절차적 언어 | Trusted / Untrusted (PL/TclU) |
| PL/Perl | Perl 절차적 언어 | Trusted / Untrusted (PL/PerlU) |
| PL/Python | Python 절차적 언어 | Untrusted (PL/PythonU) |

> 참고: 핵심 배포판에 포함되지 않은 추가 절차적 언어는 PostgreSQL의 External Projects에서 찾을 수 있습니다.

---

## 2. 절차적 언어 설치

### 2.1 기본 설치 규칙

절차적 언어는 사용할 각 데이터베이스에 설치해야 합니다. `template1` 데이터베이스에 설치된 언어는 이후에 생성되는 모든 데이터베이스에서 자동으로 사용할 수 있습니다.

### 2.2 간편 설치 방법 (권장)

표준 배포판에 포함된 언어의 경우, `CREATE EXTENSION` 명령을 사용합니다:

```sql
-- PL/pgSQL (기본적으로 설치됨)
CREATE EXTENSION plpgsql;

-- PL/Tcl
CREATE EXTENSION pltcl;

-- PL/TclU (Untrusted)
CREATE EXTENSION pltclu;

-- PL/Perl
CREATE EXTENSION plperl;

-- PL/PerlU (Untrusted)
CREATE EXTENSION plperlu;

-- PL/Python
CREATE EXTENSION plpython3u;
```

### 2.3 수동 설치 방법

확장(Extension)으로 패키지되지 않은 언어의 경우, 다음 5단계를 수행해야 합니다. 이 작업은 데이터베이스 슈퍼유저(Superuser)만 수행할 수 있습니다.

#### 단계 1: 공유 객체 컴파일 및 설치

언어 핸들러를 공유 객체로 컴파일하고 적절한 라이브러리 디렉토리에 설치합니다.

#### 단계 2: 핸들러 함수 선언

```sql
CREATE FUNCTION handler_function_name()
    RETURNS language_handler
    AS 'path-to-shared-object'
    LANGUAGE C;
```

#### 단계 3: 인라인 핸들러 선언 (선택 사항)

익명 코드 블록(`DO` 명령)을 실행하기 위해 필요합니다:

```sql
CREATE FUNCTION inline_function_name(internal)
    RETURNS void
    AS 'path-to-shared-object'
    LANGUAGE C;
```

#### 단계 4: 유효성 검사 함수 선언 (선택 사항)

함수 정의를 실행하지 않고 검사하기 위해 필요합니다:

```sql
CREATE FUNCTION validator_function_name(oid)
    RETURNS void
    AS 'path-to-shared-object'
    LANGUAGE C STRICT;
```

#### 단계 5: 언어 생성

```sql
CREATE [TRUSTED] LANGUAGE language_name
    HANDLER handler_function_name
    [INLINE inline_function_name]
    [VALIDATOR validator_function_name];
```

### 2.4 수동 설치 예제: PL/Perl

```sql
-- 핸들러 함수 생성
CREATE FUNCTION plperl_call_handler() RETURNS language_handler AS
    '$libdir/plperl' LANGUAGE C;

-- 인라인 핸들러 생성
CREATE FUNCTION plperl_inline_handler(internal) RETURNS void AS
    '$libdir/plperl' LANGUAGE C STRICT;

-- 유효성 검사기 생성
CREATE FUNCTION plperl_validator(oid) RETURNS void AS
    '$libdir/plperl' LANGUAGE C STRICT;

-- 언어 생성
CREATE TRUSTED LANGUAGE plperl
    HANDLER plperl_call_handler
    INLINE plperl_inline_handler
    VALIDATOR plperl_validator;
```

### 2.5 기본 설치 상태

| 언어 | 기본 설치 여부 |
|------|----------------|
| PL/pgSQL | 예 (모든 데이터베이스) |
| PL/Tcl, PL/TclU | 아니오 (설정 시) |
| PL/Perl, PL/PerlU | 아니오 (설정 시) |
| PL/PythonU | 아니오 (설정 시) |

---

## 3. PL/pgSQL

### 3.1 개요

PL/pgSQL은 PostgreSQL 데이터베이스 시스템을 위한 로드 가능한 절차적 언어입니다. PostgreSQL 9.0 이상에서는 기본적으로 설치됩니다.

#### 설계 목표

- 함수(Function), 프로시저(Procedure), 트리거(Trigger) 생성
- SQL 언어에 제어 구조 추가
- 복잡한 계산 수행
- 모든 사용자 정의 타입, 함수, 프로시저, 연산자 상속
- 서버에서 신뢰할 수 있도록 정의
- 사용하기 쉬움

### 3.2 PL/pgSQL 사용의 장점

#### 성능 개선

문제점: SQL은 각 문장이 개별적으로 데이터베이스 서버에서 실행되어야 합니다. 클라이언트 애플리케이션이 각 쿼리를 전송, 처리 대기, 결과 수신, 처리 후 다시 서버로 쿼리를 보내야 하므로 프로세스 간 통신과 네트워크 오버헤드가 발생합니다.

PL/pgSQL의 해결책:
- 계산 블록과 일련의 쿼리를 데이터베이스 서버 내부 에 그룹화
- 클라이언트와 서버 간 추가 왕복(Round-trip) 제거
- 중간 결과의 마샬링/전송 불필요
- 여러 번의 쿼리 파싱 회피

### 3.3 기본 구조 (Block Structure)

PL/pgSQL은 블록 구조 언어입니다. 함수 본문은 반드시 블록이어야 합니다:

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
END [ label ];
```

#### 기본 예제

```sql
CREATE FUNCTION somefunc() RETURNS integer AS $$
DECLARE
    quantity integer := 30;
BEGIN
    RAISE NOTICE 'Quantity here is %', quantity;  -- 30 출력
    RETURN quantity;
END;
$$ LANGUAGE plpgsql;
```

#### 중첩 블록과 변수 스코핑 예제

```sql
CREATE FUNCTION somefunc() RETURNS integer AS $$
<< outerblock >>
DECLARE
    quantity integer := 30;
BEGIN
    RAISE NOTICE 'Quantity here is %', quantity;  -- 30 출력
    quantity := 50;

    -- 서브블록 생성
    DECLARE
        quantity integer := 80;
    BEGIN
        RAISE NOTICE 'Quantity here is %', quantity;  -- 80 출력
        RAISE NOTICE 'Outer quantity here is %', outerblock.quantity;  -- 50 출력
    END;

    RAISE NOTICE 'Quantity here is %', quantity;  -- 50 출력
    RETURN quantity;
END;
$$ LANGUAGE plpgsql;
```

### 3.4 변수 선언 (Declarations)

#### 기본 문법

```sql
name [ CONSTANT ] type [ COLLATE collation_name ] [ NOT NULL ] [ { DEFAULT | := | = } expression ];
```

#### 변수 선언 예제

```sql
DECLARE
    user_id integer;
    quantity numeric(5);
    url varchar;
    myrow tablename%ROWTYPE;
    myfield tablename.columnname%TYPE;
    arow RECORD;

    -- 기본값이 있는 변수
    quantity integer DEFAULT 32;
    url varchar := 'http://mysite.com';
    transaction_time CONSTANT timestamp with time zone := now();
```

#### 함수 매개변수 사용

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

#### %TYPE을 사용한 타입 복사

```sql
DECLARE
    user_id users.user_id%TYPE;
    user_ids users.user_id%TYPE[];
```

#### %ROWTYPE을 사용한 행 타입

```sql
DECLARE
    myrow tablename%ROWTYPE;
```

#### RECORD 타입

```sql
DECLARE
    arow RECORD;
```

### 3.5 표현식 (Expressions)

모든 PL/pgSQL 표현식은 메인 SQL 실행기를 통해 처리됩니다. 표현식은 내부적으로 `SELECT` 명령으로 변환됩니다.

```sql
-- 예: IF 문의 표현식
IF x < y THEN ...

-- 내부적으로 다음과 같이 변환됨:
-- SELECT $1 < $2
```

### 3.6 제어 구조 (Control Structures)

#### 조건문 (Conditionals)

IF-THEN

```sql
IF v_user_id <> 0 THEN
    UPDATE users SET email = v_email WHERE user_id = v_user_id;
END IF;
```

IF-THEN-ELSE

```sql
IF v_count > 0 THEN
    INSERT INTO users_count (count) VALUES (v_count);
    RETURN 't';
ELSE
    RETURN 'f';
END IF;
```

IF-THEN-ELSIF

```sql
IF number = 0 THEN
    result := 'zero';
ELSIF number > 0 THEN
    result := 'positive';
ELSIF number < 0 THEN
    result := 'negative';
ELSE
    result := 'NULL';
END IF;
```

Simple CASE

```sql
CASE x
    WHEN 1, 2 THEN
        msg := 'one or two';
    ELSE
        msg := 'other value than one or two';
END CASE;
```

Searched CASE

```sql
CASE
    WHEN x BETWEEN 0 AND 10 THEN
        msg := 'value is between zero and ten';
    WHEN x BETWEEN 11 AND 20 THEN
        msg := 'value is between eleven and twenty';
END CASE;
```

#### 반복문 (Loops)

무조건 반복 (LOOP)

```sql
LOOP
    -- some computations
    EXIT WHEN count > 0;
END LOOP;
```

WHILE 반복

```sql
WHILE amount_owed > 0 AND gift_certificate_balance > 0 LOOP
    -- some computations here
END LOOP;
```

FOR 반복 (정수형)

```sql
-- 1부터 10까지
FOR i IN 1..10 LOOP
    -- i: 1,2,3,4,5,6,7,8,9,10
END LOOP;

-- 역순
FOR i IN REVERSE 10..1 LOOP
    -- i: 10,9,8,7,6,5,4,3,2,1
END LOOP;

-- 단계 지정
FOR i IN REVERSE 10..1 BY 2 LOOP
    -- i: 10,8,6,4,2
END LOOP;
```

FOR 반복 (쿼리 결과)

```sql
CREATE FUNCTION refresh_mviews() RETURNS integer AS $$
DECLARE
    mviews RECORD;
BEGIN
    FOR mviews IN
       SELECT n.nspname AS mv_schema,
              c.relname AS mv_name
         FROM pg_catalog.pg_class c
    LEFT JOIN pg_catalog.pg_namespace n ON (n.oid = c.relnamespace)
        WHERE c.relkind = 'm'
    LOOP
        EXECUTE format('REFRESH MATERIALIZED VIEW %I.%I',
                       mviews.mv_schema, mviews.mv_name);
    END LOOP;
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
```

FOREACH 반복 (배열)

```sql
CREATE FUNCTION sum(int[]) RETURNS int8 AS $$
DECLARE
  s int8 := 0;
  x int;
BEGIN
  FOREACH x IN ARRAY $1
  LOOP
    s := s + x;
  END LOOP;
  RETURN s;
END;
$$ LANGUAGE plpgsql;
```

### 3.7 오류 처리 (Exception Handling)

#### 기본 문법

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
EXCEPTION
    WHEN condition [ OR condition ... ] THEN
        handler_statements
    [ WHEN condition [ OR condition ... ] THEN
        handler_statements ... ]
END;
```

#### 예외 처리 예제

```sql
CREATE TABLE db (a INT PRIMARY KEY, b TEXT);

CREATE FUNCTION merge_db(key INT, data TEXT) RETURNS VOID AS $$
BEGIN
    LOOP
        UPDATE db SET b = data WHERE a = key;
        IF found THEN
            RETURN;
        END IF;
        BEGIN
            INSERT INTO db(a,b) VALUES (key, data);
            RETURN;
        EXCEPTION WHEN unique_violation THEN
            -- 아무것도 하지 않고 UPDATE 재시도
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

#### 오류 정보 얻기

```sql
DECLARE
  text_var1 text;
  text_var2 text;
BEGIN
  -- 처리 로직
EXCEPTION WHEN OTHERS THEN
  GET STACKED DIAGNOSTICS text_var1 = MESSAGE_TEXT,
                          text_var2 = PG_EXCEPTION_DETAIL;
END;
```

사용 가능한 진단 항목:

| 항목 | 설명 |
|------|------|
| `RETURNED_SQLSTATE` | 오류 코드 |
| `MESSAGE_TEXT` | 주요 메시지 |
| `PG_EXCEPTION_DETAIL` | 상세 메시지 |
| `PG_EXCEPTION_HINT` | 힌트 메시지 |
| `COLUMN_NAME` | 관련 컬럼 |
| `CONSTRAINT_NAME` | 관련 제약 조건 |
| `TABLE_NAME` | 관련 테이블 |
| `SCHEMA_NAME` | 관련 스키마 |

### 3.8 지원되는 데이터 타입

| 카테고리 | 설명 |
|----------|------|
| 스칼라 타입 | 서버가 지원하는 모든 스칼라 데이터 타입 |
| 배열 타입 | 서버가 지원하는 모든 배열 데이터 타입 |
| 복합 타입 | 이름으로 지정된 모든 복합 타입(행 타입) |
| RECORD | 모든 복합 타입 수용 (입력/출력) |

---

## 4. PL/Tcl

### 4.1 개요

PL/Tcl 은 PostgreSQL에서 [Tcl 언어](https://www.tcl.tk/)를 사용하여 함수와 프로시저를 작성할 수 있게 해주는 로드 가능한 절차적 언어입니다.

#### 주요 특징

- C 언어 함수 작성자가 가지는 대부분의 기능을 제공
- 몇 가지 제한 사항 존재
- Tcl의 강력한 문자열 처리 라이브러리 사용 가능

#### 안전성과 제한 사항

- 모든 것이 안전한 Tcl 인터프리터 컨텍스트 내에서 실행
- 안전한 Tcl 명령 세트로 제한
- SPI를 통한 데이터베이스 접근과 `elog()`를 통한 메시지 출력만 가능
- 데이터베이스 서버 내부나 OS 수준 접근 불가
- 권한 없는 데이터베이스 사용자도 안전하게 사용 가능

### 4.2 설치

```sql
-- Trusted PL/Tcl
CREATE EXTENSION pltcl;

-- Untrusted PL/TclU
CREATE EXTENSION pltclu;
```

### 4.3 함수 생성 및 인수

#### 기본 문법

```tcl
CREATE FUNCTION funcname (argument-types) RETURNS return-type AS $$
    # PL/Tcl function body
$$ LANGUAGE pltcl;
```

#### 예제: STRICT 절이 있는 간단한 함수

```tcl
CREATE FUNCTION tcl_max(integer, integer) RETURNS integer AS $$
    if {$1 > $2} {return $1}
    return $2
$$ LANGUAGE pltcl STRICT;
```

#### 예제: NULL 값 처리

```tcl
CREATE FUNCTION tcl_max(integer, integer) RETURNS integer AS $$
    if {[argisnull 1]} {
        if {[argisnull 2]} { return_null }
        return $2
    }
    if {[argisnull 2]} { return $1 }
    if {$1 > $2} {return $1}
    return $2
$$ LANGUAGE pltcl;
```

#### 예제: 복합 타입 인수

```tcl
CREATE TABLE employee (
    name text,
    salary integer,
    age integer
);

CREATE FUNCTION overpaid(employee) RETURNS boolean AS $$
    if {200000.0 < $1(salary)} {
        return "t"
    }
    if {$1(age) < 30 && 100000.0 < $1(salary)} {
        return "t"
    }
    return "f"
$$ LANGUAGE pltcl;
```

#### 예제: 복합 타입 반환

```tcl
CREATE FUNCTION square_cube(in int, out squared int, out cubed int) AS $$
    return [list squared [expr {$1 * $1}] cubed [expr {$1 * $1 * $1}]]
$$ LANGUAGE pltcl;
```

#### 예제: 집합 반환 함수

```tcl
CREATE FUNCTION sequence(int, int) RETURNS SETOF int AS $$
    for {set i $1} {$i < $2} {incr i} {
        return_next $i
    }
$$ LANGUAGE pltcl;
```

### 4.4 주요 함수

| 함수 | 용도 |
|------|------|
| `argisnull n` | 인수 n이 NULL인지 확인 |
| `return_null` | NULL 값 반환 |
| `return_next value` | 집합 반환 함수에서 행 반환 |

### 4.5 PL/TclU (Untrusted)

제한 없는 Tcl 함수가 필요한 경우(예: 이메일 전송)에 사용합니다.

- 안전한 Tcl 대신 전체 Tcl 인터프리터 사용
- 신뢰할 수 없는 절차적 언어로 설치해야 함
- 데이터베이스 슈퍼유저만 함수 생성 가능
- 개발자는 함수가 오용되지 않도록 보장해야 함

---

## 5. PL/Perl

### 5.1 개요

PL/Perl 은 PostgreSQL 함수와 프로시저를 Perl 프로그래밍 언어로 작성할 수 있게 해주는 로드 가능한 절차적 언어입니다.

#### 주요 장점

- Perl의 광범위한 "문자열 처리(String Munging)" 연산자와 함수 사용 가능
- 복잡한 문자열 파싱이 PL/pgSQL보다 훨씬 쉬움

### 5.2 설치

```sql
-- Trusted PL/Perl
CREATE EXTENSION plperl;

-- Untrusted PL/PerlU
CREATE EXTENSION plperlu;
```

> 참고: 소스 패키지에서는 설치 과정 중 PL/Perl을 특별히 활성화해야 합니다. 바이너리 패키지에서는 별도의 서브패키지에 포함될 수 있습니다.

### 5.3 함수 생성 문법

```perl
CREATE FUNCTION funcname (argument-types)
RETURNS return-type
-- function attributes can go here
AS $$
    # PL/Perl function body goes here
$$ LANGUAGE plperl;
```

### 5.4 예제

#### 기본 함수

```perl
CREATE FUNCTION perl_max (integer, integer) RETURNS integer AS $$
    if ($_[0] > $_[1]) { return $_[0]; }
    return $_[1];
$$ LANGUAGE plperl;
```

#### NULL 값 처리

```perl
CREATE FUNCTION perl_max (integer, integer) RETURNS integer AS $$
    my ($x, $y) = @_;
    if (not defined $x) {
        return undef if not defined $y;
        return $y;
    }
    return $x if not defined $y;
    return $x if $x > $y;
    return $y;
$$ LANGUAGE plperl;
```

#### 익명 코드 블록

```perl
DO $$
    # PL/Perl code
$$ LANGUAGE plperl;
```

#### Boolean 값 처리

```sql
CREATE EXTENSION bool_plperl;

CREATE FUNCTION perl_and(bool, bool) RETURNS bool
TRANSFORM FOR TYPE bool
AS $$
  my ($a, $b) = @_;
  return $a && $b;
$$ LANGUAGE plperl;
```

#### 배열 처리

```perl
CREATE OR REPLACE FUNCTION returns_array()
RETURNS text[][] AS $$
    return [['a"b','c,d'],['e\\f','g']];
$$ LANGUAGE plperl;

-- 배열 입력 처리
CREATE OR REPLACE FUNCTION concat_array_elements(text[]) RETURNS TEXT AS $$
    my $arg = shift;
    my $result = "";
    return undef if (!defined $arg);

    for (@$arg) {
        $result .= $_;
    }
    return $result;
$$ LANGUAGE plperl;
```

#### 복합 타입

```perl
CREATE TABLE employee (
    name text,
    basesalary integer,
    bonus integer
);

CREATE FUNCTION empcomp(employee) RETURNS integer AS $$
    my ($emp) = @_;
    return $emp->{basesalary} + $emp->{bonus};
$$ LANGUAGE plperl;
```

#### 복합 타입 반환

```perl
CREATE TYPE testrowperl AS (f1 integer, f2 text, f3 text);

CREATE OR REPLACE FUNCTION perl_row() RETURNS testrowperl AS $$
    return {f2 => 'hello', f1 => 1, f3 => 'world'};
$$ LANGUAGE plperl;
```

#### 집합 반환 함수

```perl
-- return_next 사용
CREATE OR REPLACE FUNCTION perl_set_int(int)
RETURNS SETOF INTEGER AS $$
    foreach (0..$_[0]) {
        return_next($_);
    }
    return undef;
$$ LANGUAGE plperl;

-- 배열 참조 반환
CREATE OR REPLACE FUNCTION perl_set_int(int) RETURNS SETOF INTEGER AS $$
    return [0..$_[0]];
$$ LANGUAGE plperl;
```

### 5.5 Strict 모드

```perl
use strict;
```

또는 전역 설정:

```sql
SET plperl.use_strict = true;
```

### 5.6 주의 사항

- 명명된 중첩 서브루틴은 위험 - 대신 익명 서브루틴 사용
- 문자열 상수 필요 - 함수 본문에 달러 인용(`$$..$$`) 사용
- 인코딩 자동 - UTF-8 변환이 투명하게 처리됨
- 다차원 배열 - 하위 차원 배열에 대한 참조로 표현

---

## 6. PL/Python

### 6.1 개요

PL/Python 은 PostgreSQL 함수와 프로시저를 Python으로 작성할 수 있게 해주는 절차적 언어입니다.

#### 중요 사항

- 신뢰할 수 없는(Untrusted) 언어로만 사용 가능 (`plpython3u`)
- 사용자 작업을 제한하지 않으므로 "신뢰할 수 없음"으로 명명
- 데이터베이스 관리자가 사용할 수 있는 시스템 기능에 완전히 접근 가능
- 슈퍼유저만 함수 생성 가능

### 6.2 설치

```sql
CREATE EXTENSION plpython3u;
```

### 6.3 기본 문법

```sql
CREATE FUNCTION funcname (argument-list)
  RETURNS return-type
AS $$
  # PL/Python function body
$$ LANGUAGE plpython3u;
```

### 6.4 예제

#### 간단한 함수

```sql
CREATE FUNCTION pymax (a integer, b integer)
  RETURNS integer
AS $$
  if a > b:
    return a
  return b
$$ LANGUAGE plpython3u;
```

#### 변수 스코핑 주의 사항

작동하지 않는 예:

```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  x = x.strip()  # 오류 - 로컬 변수 문제
  return x
$$ LANGUAGE plpython3u;
```

해결책: global 문 사용:

```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  global x
  x = x.strip()  # 이제 정상 작동
  return x
$$ LANGUAGE plpython3u;
```

모범 사례: 함수 매개변수를 읽기 전용 으로 취급하여 구현 세부 사항에 의존하지 않도록 합니다.

### 6.5 데이터 타입 매핑

#### PostgreSQL에서 Python으로 변환

| PostgreSQL 타입 | Python 타입 |
|-----------------|-------------|
| `boolean` | `bool` |
| `smallint`, `int`, `bigint`, `oid` | `int` |
| `real`, `double` | `float` |
| `numeric` | `Decimal` (`cdecimal` 또는 `decimal` 모듈) |
| `bytea` | `bytes` |
| 기타 모든 타입 (문자열 타입 포함) | `str` (Unicode) |

#### Python에서 PostgreSQL로 반환 값 변환

- `boolean`: Python 규칙에 따라 진리값 평가 (0과 빈 문자열은 false, `'f'`는 true)
- `bytea`: Python 내장 함수를 사용하여 `bytes`로 변환
- 기타 타입: Python `str()` 내장 함수를 사용하여 문자열로 변환
  - 예외: `float` 값은 정밀도 손실을 피하기 위해 `str()` 대신 `repr()` 사용
  - 문자열은 자동으로 PostgreSQL 서버 인코딩으로 변환

### 6.6 주요 기능

- 데이터베이스 접근 기능
- 트리거 함수 지원
- 익명 코드 블록 (DO 문)
- 집합 반환 함수
- 명시적 서브트랜잭션 지원

### 6.7 보안 고려 사항

함수 작성자는 신뢰할 수 없는 PL/Python이 악의적인 목적으로 악용될 수 없도록 보장해야 합니다. 데이터베이스 관리자 사용자와 동일한 기능을 가지고 있습니다.

---

## 7. 절차적 언어 보안

### 7.1 TRUSTED vs UNTRUSTED 언어

#### TRUSTED 언어

- 일반 데이터베이스 사용자가 함수를 생성할 수 있음
- 무단 데이터 접근을 허용하지 않음
- 예: PL/pgSQL, PL/Tcl, PL/Perl

#### UNTRUSTED 언어

- 슈퍼유저만 함수를 생성할 수 있음
- 시스템 리소스에 대한 제한 없는 접근 가능
- 예: PL/TclU, PL/PerlU, PL/PythonU

### 7.2 보안 지침

| 지침 | 설명 |
|------|------|
| 최소 권한 원칙 | 필요한 최소한의 권한만 부여 |
| TRUSTED 언어 우선 | 가능하면 TRUSTED 언어 사용 |
| 코드 검토 | UNTRUSTED 언어 함수는 철저한 검토 필요 |
| 입력 검증 | 모든 사용자 입력에 대해 검증 수행 |

### 7.3 SECURITY DEFINER vs SECURITY INVOKER

```sql
-- SECURITY DEFINER: 함수 소유자의 권한으로 실행
CREATE FUNCTION secure_func() RETURNS void
SECURITY DEFINER
AS $$
    -- function body
$$ LANGUAGE plpgsql;

-- SECURITY INVOKER: 호출자의 권한으로 실행 (기본값)
CREATE FUNCTION normal_func() RETURNS void
SECURITY INVOKER
AS $$
    -- function body
$$ LANGUAGE plpgsql;
```

### 7.4 보안 모범 사례

1. SECURITY DEFINER 함수에서 search_path 설정

```sql
CREATE FUNCTION secure_func() RETURNS void
SECURITY DEFINER
SET search_path = pg_catalog, pg_temp
AS $$
    -- function body
$$ LANGUAGE plpgsql;
```

2. 동적 SQL에서 입력 이스케이프

```sql
-- format 함수의 %I (식별자), %L (리터럴) 사용
EXECUTE format('SELECT * FROM %I WHERE id = %L', table_name, id_value);
```

3. UNTRUSTED 언어 함수 최소화

필요한 경우에만 UNTRUSTED 언어 사용하고, 사용 시 철저한 보안 검토 수행

---

## 참고 자료

- [PostgreSQL 공식 문서 - Procedural Languages](https://www.postgresql.org/docs/current/xplang.html)
- [PostgreSQL 공식 문서 - PL/pgSQL](https://www.postgresql.org/docs/current/plpgsql.html)
- [PostgreSQL 공식 문서 - PL/Tcl](https://www.postgresql.org/docs/current/pltcl.html)
- [PostgreSQL 공식 문서 - PL/Perl](https://www.postgresql.org/docs/current/plperl.html)
- [PostgreSQL 공식 문서 - PL/Python](https://www.postgresql.org/docs/current/plpython.html)
