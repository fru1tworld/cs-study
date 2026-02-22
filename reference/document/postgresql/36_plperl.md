# PL/Perl - Perl 절차적 언어 (Perl Procedural Language)

## 목차
1. [개요](#1-개요)
2. [PL/Perl 함수와 인수](#2-plperl-함수와-인수-plperl-functions-and-arguments)
3. [PL/Perl의 데이터 값](#3-plperl의-데이터-값-data-values-in-plperl)
4. [내장 함수](#4-내장-함수-built-in-functions)
5. [전역 값](#5-전역-값-global-values-in-plperl)
6. [신뢰(Trusted)와 비신뢰(Untrusted) PL/Perl](#6-신뢰trusted와-비신뢰untrusted-plperl)
7. [PL/Perl 트리거](#7-plperl-트리거-plperl-triggers)
8. [PL/Perl 이벤트 트리거](#8-plperl-이벤트-트리거-plperl-event-triggers)
9. [PL/Perl 내부 구조](#9-plperl-내부-구조-plperl-under-the-hood)

---

## 1. 개요

PL/Perl은 PostgreSQL 함수와 프로시저를 [Perl 프로그래밍 언어](https://www.perl.org)로 작성할 수 있게 해주는 로드 가능한 절차적 언어(loadable procedural language)입니다.

### 1.1 주요 장점

PL/Perl을 사용하는 주요 장점은 저장 함수(stored functions)와 프로시저 내에서 Perl의 광범위한 "문자열 처리(string munging)" 연산자와 함수를 사용할 수 있다는 것입니다. 복잡한 문자열 파싱(parsing)은 PL/pgSQL에서 제공하는 문자열 함수와 제어 구조보다 Perl을 사용하는 것이 더 쉬울 수 있습니다.

### 1.2 설치

특정 데이터베이스에 PL/Perl을 설치하려면 다음 명령을 사용합니다:

```sql
CREATE EXTENSION plperl;
```

팁: `template1`에 언어를 설치하면, 이후에 생성되는 모든 데이터베이스에 해당 언어가 자동으로 설치됩니다.

참고:
- 소스 패키지 사용자는 설치 과정에서 PL/Perl 빌드를 특별히 활성화해야 합니다 (Chapter 17: 소스 코드로부터 설치 참조).
- 바이너리 패키지 사용자는 별도의 서브패키지에서 PL/Perl을 찾을 수 있습니다.

---

## 2. PL/Perl 함수와 인수 (PL/Perl Functions and Arguments)

### 2.1 기본 함수 생성

PL/Perl 함수는 표준 `CREATE FUNCTION` 구문을 사용하여 생성합니다:

```sql
CREATE FUNCTION funcname (argument-types)
RETURNS return-type
-- 함수 속성을 여기에 지정할 수 있습니다
AS $$
    # PL/Perl 함수 본문이 여기에 들어갑니다
$$ LANGUAGE plperl;
```

핵심 사항:
- 함수 본문은 서브루틴으로 래핑된 일반 Perl 코드입니다
- 함수는 스칼라 컨텍스트(scalar context)에서 동작합니다 (직접 리스트를 반환할 수 없음)
- 비스칼라 값(배열, 레코드, 집합)은 참조(reference)로 반환해야 합니다
- 인수는 `@_`로 전달되고 결과는 `return` 또는 마지막 표현식을 통해 반환됩니다

### 2.2 익명 코드 블록 (Anonymous Code Blocks)

```sql
DO $$
    # PL/Perl 코드
$$ LANGUAGE plperl;
```

익명 블록은 인수를 받지 않으며 반환 값은 무시됩니다.

### 2.3 인수 처리

#### NULL 값 처리

SQL NULL 값은 Perl에서 "undefined"로 나타납니다. `STRICT` 속성을 사용하거나 `defined()`로 확인합니다:

```sql
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

#### 타입 변환 (Type Conversion)

- 인수는 데이터베이스 인코딩에서 UTF-8로 변환됩니다
- 비참조(non-reference) 인수는 PostgreSQL의 외부 텍스트 표현 형식의 문자열입니다
- `bytea` 타입 변환에는 `decode_bytea()`를 사용합니다
- 반환 값은 외부 텍스트 표현 형식이어야 합니다

#### 불리언 값 (Boolean Values)

기본적으로 `bool` 값은 텍스트(`'t'` 또는 `'f'`)입니다. 올바른 처리를 위해 변환(transforms)을 사용합니다:

```sql
CREATE EXTENSION bool_plperl;

CREATE FUNCTION perl_and(bool, bool) RETURNS bool
TRANSFORM FOR TYPE bool
AS $$
  my ($a, $b) = @_;
  return $a && $b;
$$ LANGUAGE plperl;
```

### 2.4 배열 작업 (Working with Arrays)

#### 배열 반환

```sql
CREATE OR REPLACE FUNCTION returns_array()
RETURNS text[][] AS $$
    return [['a"b','c,d'],['e\\f','g']];
$$ LANGUAGE plperl;
```

#### 배열 수신 (Blessed Objects로)

```sql
CREATE OR REPLACE FUNCTION concat_array_elements(text[]) RETURNS TEXT AS $$
    my $arg = shift;
    my $result = "";
    return undef if (!defined $arg);

    # 배열 참조로 사용
    for (@$arg) {
        $result .= $_;
    }

    # 문자열로도 작동
    $result .= $arg;

    return $result;
$$ LANGUAGE plperl;
```

### 2.5 복합 타입 (Composite Types)

#### 복합 타입 인수 수신

```sql
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

#### 복합 타입 결과 반환

```sql
CREATE TYPE testrowperl AS (f1 integer, f2 text, f3 text);

CREATE OR REPLACE FUNCTION perl_row() RETURNS testrowperl AS $$
    return {f2 => 'hello', f1 => 1, f3 => 'world'};
$$ LANGUAGE plperl;
```

### 2.6 집합 반환 (Returning Sets)

#### return_next 사용 (대용량 집합에 권장)

```sql
CREATE OR REPLACE FUNCTION perl_set_int(int)
RETURNS SETOF INTEGER AS $$
    foreach (0..$_[0]) {
        return_next($_);
    }
    return undef;
$$ LANGUAGE plperl;
```

#### 배열 참조 반환 (소규모 집합용)

```sql
CREATE OR REPLACE FUNCTION perl_set_int(int) RETURNS SETOF INTEGER AS $$
    return [0..$_[0]];
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION perl_set() RETURNS SETOF testrowperl AS $$
    return [
        { f1 => 1, f2 => 'Hello', f3 => 'World' },
        { f1 => 2, f2 => 'Hello', f3 => 'PostgreSQL' },
        { f1 => 3, f2 => 'Hello', f3 => 'PL/Perl' }
    ];
$$ LANGUAGE plperl;
```

### 2.7 프로시저의 출력 매개변수 (Output Parameters in Procedures)

```sql
CREATE PROCEDURE perl_triple(INOUT a integer, INOUT b integer) AS $$
    my ($a, $b) = @_;
    return {a => $a * 3, b => $b * 3};
$$ LANGUAGE plperl;
```

### 2.8 Pragma 사용

#### Strict 모드

```sql
-- 함수별 설정
use strict;

-- 전역 설정 (임시)
SET plperl.use_strict to true;

-- 전역 설정 (영구 - postgresql.conf)
plperl.use_strict = true
```

#### Feature Pragma

Perl 5.10.0 이상에서 사용 가능합니다.

### 2.9 중요 참고사항

- 중첩된 명명 서브루틴(nested named subroutines)은 위험합니다; 대신 익명 서브루틴을 사용하세요
- 함수 본문에는 달러 따옴표(`$$...$$`)를 권장합니다
- 인수는 자동으로 UTF-8로/에서 변환됩니다
- 누락된 복합 타입 속성은 NULL로 반환됩니다

---

## 3. PL/Perl의 데이터 값 (Data Values in PL/Perl)

### 3.1 핵심 개념

#### 인수 및 반환 값 처리

- PL/Perl 함수에 제공되는 입력 인수는 텍스트 형식으로 변환됩니다 (`SELECT` 문으로 표시되는 것처럼)
- `return` 및 `return_next` 명령은 함수의 선언된 반환 타입에 대해 허용 가능한 입력 형식의 모든 문자열을 받아들입니다

### 3.2 타입별 변환 규칙

| PostgreSQL 타입 | Perl 표현 |
|----------------|----------|
| `integer`, `numeric` | 숫자 문자열 |
| `text`, `varchar` | 문자열 |
| `boolean` | `'t'` 또는 `'f'` (기본) |
| `bytea` | 이스케이프된 문자열 (decode_bytea 사용) |
| `NULL` | `undef` |
| 배열 | Blessed 배열 참조 |
| 복합 타입 | 해시 참조 |

### 3.3 변환 모듈 (Transform Modules)

텍스트 변환이 불편한 경우를 위해 PostgreSQL은 이 동작을 개선할 수 있는 변환 모듈을 제공합니다.

```sql
-- bool 변환 모듈 예제
CREATE EXTENSION bool_plperl;

CREATE FUNCTION perl_and(bool, bool) RETURNS bool
TRANSFORM FOR TYPE bool
AS $$
  my ($a, $b) = @_;
  return $a && $b;
$$ LANGUAGE plperl;
```

PostgreSQL 배포판에는 여러 예제 변환 모듈이 포함되어 있습니다.

---

## 4. 내장 함수 (Built-in Functions)

### 4.1 데이터베이스 접근 (Database Access from PL/Perl)

#### 4.1.1 spi_exec_query()

SQL 명령을 실행하고 전체 행 집합을 해시 참조의 배열에 대한 참조로 반환합니다.

구문: `spi_exec_query(query [, limit])`

```perl
$rv = spi_exec_query('SELECT * FROM my_table', 5);
$foo = $rv->{rows}[$i]->{my_column};
$nrows = $rv->{processed};
$res = $rv->{status};
```

전체 예제:

```sql
CREATE TABLE test (
    i int,
    v varchar
);

INSERT INTO test (i, v) VALUES (1, 'first line');
INSERT INTO test (i, v) VALUES (2, 'second line');
INSERT INTO test (i, v) VALUES (3, 'third line');
INSERT INTO test (i, v) VALUES (4, 'immortal');

CREATE OR REPLACE FUNCTION test_munge() RETURNS SETOF test AS $$
    my $rv = spi_exec_query('select i, v from test;');
    my $status = $rv->{status};
    my $nrows = $rv->{processed};
    foreach my $rn (0 .. $nrows - 1) {
        my $row = $rv->{rows}[$rn];
        $row->{i} += 200 if defined($row->{i});
        $row->{v} =~ tr/A-Za-z/a-zA-Z/ if (defined($row->{v}));
        return_next($row);
    }
    return undef;
$$ LANGUAGE plperl;

SELECT * FROM test_munge();
```

#### 4.1.2 spi_query()와 spi_fetchrow()

대용량 행 집합을 처리하거나 행이 도착하는 대로 반환하는 데 함께 사용됩니다.

구문:
```perl
spi_query(command)
spi_fetchrow(cursor)
spi_cursor_close(cursor)
```

예제:

```sql
CREATE TYPE foo_type AS (the_num INTEGER, the_text TEXT);

CREATE OR REPLACE FUNCTION lotsa_md5 (INTEGER) RETURNS SETOF foo_type AS $$
    use Digest::MD5 qw(md5_hex);
    my $file = '/usr/share/dict/words';
    my $t = localtime;
    elog(NOTICE, "opening file $file at $t" );
    open my $fh, '<', $file
        or elog(ERROR, "cannot open $file for reading: $!");
    my @words = <$fh>;
    close $fh;
    $t = localtime;
    elog(NOTICE, "closed file $file at $t");
    chomp(@words);
    my $row;
    my $sth = spi_query("SELECT * FROM generate_series(1,$_[0]) AS b(a)");
    while (defined ($row = spi_fetchrow($sth))) {
        return_next({
            the_num => $row->{a},
            the_text => md5_hex($words[rand @words])
        });
    }
    return;
$$ LANGUAGE plperlu;

SELECT * from lotsa_md5(500);
```

참고: `spi_fetchrow`는 더 이상 행이 없을 때 `undef`를 반환합니다. 모든 행을 읽지 않는 경우 `spi_cursor_close()`를 호출하여 커서를 수동으로 해제하세요 (메모리 누수 방지).

#### 4.1.3 준비된 쿼리 (Prepared Queries)

번호가 매겨진 인수 플레이스홀더를 사용하는 준비된 쿼리 기능을 구현합니다.

구문:
```perl
spi_prepare(command, argument_types)
spi_query_prepared(plan, arguments)
spi_exec_prepared(plan [, attributes], arguments)
spi_freeplan(plan)
```

예제 1:
```perl
$plan = spi_prepare('SELECT * FROM test WHERE id > $1 AND name = $2',
                    'INTEGER', 'TEXT');
```

예제 2: 공유 플랜 사용
```sql
CREATE OR REPLACE FUNCTION init() RETURNS VOID AS $$
        $_SHARED{my_plan} = spi_prepare('SELECT (now() + $1)::date AS now',
                                        'INTERVAL');
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION add_time( INTERVAL ) RETURNS TEXT AS $$
        return spi_exec_prepared(
                $_SHARED{my_plan},
                $_[0]
        )->{rows}->[0]->{now};
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION done() RETURNS VOID AS $$
        spi_freeplan( $_SHARED{my_plan});
        undef $_SHARED{my_plan};
$$ LANGUAGE plperl;

SELECT init();
SELECT add_time('1 day'), add_time('2 days'), add_time('3 days');
SELECT done();
```

예제 3: limit 속성 사용
```sql
CREATE TABLE hosts AS SELECT id, ('192.168.1.'||id)::inet AS address
                      FROM generate_series(1,3) AS id;

CREATE OR REPLACE FUNCTION init_hosts_query() RETURNS VOID AS $$
        $_SHARED{plan} = spi_prepare('SELECT * FROM hosts
                                      WHERE address << $1', 'inet');
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION query_hosts(inet) RETURNS SETOF hosts AS $$
        return spi_exec_prepared(
                $_SHARED{plan},
                {limit => 2},
                $_[0]
        )->{rows};
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION release_hosts_query() RETURNS VOID AS $$
        spi_freeplan($_SHARED{plan});
        undef $_SHARED{plan};
$$ LANGUAGE plperl;

SELECT init_hosts_query();
SELECT query_hosts('192.168.1.0/30');
SELECT release_hosts_query();
```

#### 4.1.4 spi_commit()과 spi_rollback()

현재 트랜잭션을 커밋하거나 롤백합니다. 프로시저 또는 익명 코드 블록(`DO` 명령)에서 최상위 레벨에서만 호출 가능합니다.

예제:
```sql
CREATE PROCEDURE transaction_test1()
LANGUAGE plperl
AS $$
foreach my $i (0..9) {
    spi_exec_query("INSERT INTO test1 (a) VALUES ($i)");
    if ($i % 2 == 0) {
        spi_commit();
    } else {
        spi_rollback();
    }
}
$$;

CALL transaction_test1();
```

### 4.2 유틸리티 함수 (Utility Functions in PL/Perl)

#### 4.2.1 elog()

로그 또는 오류 메시지를 출력합니다.

구문: `elog(level, msg)`

레벨: `DEBUG`, `LOG`, `INFO`, `NOTICE`, `WARNING`, `ERROR`

참고: `ERROR`는 오류 조건을 발생시킵니다. 다른 레벨은 `log_min_messages` 및 `client_min_messages` 구성 변수에 의해 제어되는 다양한 우선순위 레벨에서 메시지를 생성합니다.

#### 4.2.2 quote_literal()

SQL에서 문자열 리터럴로 사용하기에 적합하게 주어진 문자열을 따옴표로 묶어 반환합니다.

구문: `quote_literal(string)`

`undef` 입력에 대해 `undef`를 반환합니다. 인수가 `undef`일 수 있는 경우 `quote_nullable()`을 사용하세요.

#### 4.2.3 quote_nullable()

주어진 문자열을 문자열 리터럴로 사용하기에 적합하게 따옴표로 묶어 반환하거나, 인수가 `undef`인 경우 `"NULL"`을 반환합니다.

구문: `quote_nullable(string)`

#### 4.2.4 quote_ident()

SQL에서 식별자로 사용하기에 적합하게 주어진 문자열을 따옴표로 묶어 반환합니다.

구문: `quote_ident(string)`

필요한 경우에만 따옴표가 추가됩니다 (비식별자 문자 또는 대소문자 변환).

#### 4.2.5 decode_bytea()

`bytea` 인코딩된 문자열로 표현된 이스케이프되지 않은 이진 데이터를 반환합니다.

구문: `decode_bytea(string)`

#### 4.2.6 encode_bytea()

이진 데이터의 `bytea` 인코딩된 형태를 반환합니다.

구문: `encode_bytea(string)`

#### 4.2.7 encode_array_literal()

배열 내용을 배열 리터럴 형식의 문자열로 반환합니다.

구문: `encode_array_literal(array [, delimiter])`

지정되지 않거나 `undef`인 경우 기본 구분자는 `", "`입니다.

#### 4.2.8 encode_typed_literal()

Perl 변수를 지정된 데이터 타입으로 변환하고 문자열 표현을 반환합니다.

구문: `encode_typed_literal(value, typename)`

중첩된 배열과 복합 타입을 올바르게 처리합니다.

#### 4.2.9 encode_array_constructor()

배열 내용을 배열 생성자 형식의 문자열로 반환합니다.

구문: `encode_array_constructor(array)`

개별 값은 `quote_nullable()`을 사용하여 따옴표로 묶입니다.

#### 4.2.10 looks_like_number()

문자열 내용이 숫자처럼 보이면 true를, 그렇지 않으면 false를 반환합니다.

구문: `looks_like_number(string)`

인수가 `undef`이면 `undef`를 반환합니다. 선행/후행 공백을 무시합니다. `Inf`와 `Infinity`를 숫자로 취급합니다.

#### 4.2.11 is_array_ref()

인수가 배열 참조로 취급될 수 있으면 true를 반환합니다.

구문: `is_array_ref(argument)`

`ref()`가 `ARRAY` 또는 `PostgreSQL::InServer::ARRAY`이면 true를 반환합니다.

---

## 5. 전역 값 (Global Values in PL/Perl)

### 5.1 개요

전역 해시 `%_SHARED`를 사용하면 현재 세션의 수명 동안 함수 호출 간에 코드 참조를 포함한 데이터를 저장할 수 있습니다.

### 5.2 기본 사용법

#### 단순 데이터 저장 및 검색

```sql
CREATE OR REPLACE FUNCTION set_var(name text, val text) RETURNS text AS $$
    if ($_SHARED{$_[0]} = $_[1]) {
        return 'ok';
    } else {
        return "cannot set shared variable $_[0] to $_[1]";
    }
$$ LANGUAGE plperl;

CREATE OR REPLACE FUNCTION get_var(name text) RETURNS text AS $$
    return $_SHARED{$_[0]};
$$ LANGUAGE plperl;

SELECT set_var('sample', 'Hello, PL/Perl!  How''s tricks?');
SELECT get_var('sample');
```

### 5.3 코드 참조 저장

```sql
CREATE OR REPLACE FUNCTION myfuncs() RETURNS void AS $$
    $_SHARED{myquote} = sub {
        my $arg = shift;
        $arg =~ s/(['\\\])/\\$1/g;
        return "'$arg'";
    };
$$ LANGUAGE plperl;

SELECT myfuncs(); /* 함수 초기화 */

CREATE OR REPLACE FUNCTION use_quote(TEXT) RETURNS text AS $$
    my $text_to_quote = shift;
    my $qfunc = $_SHARED{myquote};
    return &$qfunc($text_to_quote);
$$ LANGUAGE plperl;

/* 또는 한 줄로: */
/* return $_SHARED{myquote}->($_[0]); */
```

### 5.4 보안 및 역할 격리 (Security & Role Isolation)

- 각 SQL 역할(role)은 자체 `%_SHARED` 인스턴스가 있는 별도의 Perl 인터프리터를 얻습니다
- 두 PL/Perl 함수는 동일한 SQL 역할에 의해 실행되는 경우에만 동일한 `%_SHARED`를 공유합니다
- 이는 사용자 간의 우발적이거나 악의적인 간섭을 방지합니다

#### 역할 간 데이터 공유

서로 다른 SQL 역할 간에 데이터를 공유해야 하는 함수의 경우:
1. 함수가 동일한 사용자에 의해 소유되어야 합니다
2. `SECURITY DEFINER`로 표시해야 합니다
3. 의도하지 않은 목적으로 오용될 수 없도록 해야 합니다

---

## 6. 신뢰(Trusted)와 비신뢰(Untrusted) PL/Perl

### 6.1 개요

PostgreSQL의 PL/Perl은 서로 다른 보안 수준을 가진 두 가지 변형으로 제공됩니다:

### 6.2 신뢰 PL/Perl (`plperl`)

- "신뢰" 변형으로 기본 설치됩니다
- 보안을 유지하기 위해 특정 Perl 작업을 제한합니다
- 권한이 없는 데이터베이스 사용자에게 안전합니다
- 모든 데이터베이스 사용자가 생성할 수 있습니다

제한된 작업:
- 파일 핸들 작업
- `require` 및 `use` (외부 모듈용)
- 환경과 상호 작용하는 모든 작업
- 데이터베이스 서버 내부에 접근하거나 OS 수준 접근을 얻을 수 없음

### 6.3 비신뢰 PL/Perl (`plperlu`)

- 제한 없이 전체 Perl 언어를 사용할 수 있습니다
- 파일 작업, 시스템 호출, 외부 모듈 가져오기를 수행할 수 있습니다
- 데이터베이스 슈퍼유저만 이 언어로 함수를 생성할 수 있습니다
- 민감한 사용 사례(예: 메일 전송)에 사용

### 6.4 보안 경고

> 신뢰 PL/Perl은 보안을 유지하기 위해 Perl `Opcode` 모듈에 의존합니다. Perl 문서에 따르면 이 모듈은 신뢰 PL/Perl 사용 사례에 효과적이지 않습니다. 보안 요구 사항이 해당 경고의 불확실성과 양립할 수 없는 경우, `REVOKE USAGE ON LANGUAGE plperl FROM PUBLIC` 실행을 고려하세요.

### 6.5 코드 예제

이 함수는 `plperl`에서 실패합니다 (파일 작업이 금지됨):

```perl
CREATE FUNCTION badfunc() RETURNS integer AS $$
    my $tmpfile = "/tmp/badfile";
    open my $fh, '>', $tmpfile
        or elog(ERROR, qq{could not open the file "$tmpfile": $!});
    print $fh "Testing writing to a file\n";
    close $fh or elog(ERROR, qq{could not close the file "$tmpfile": $!});
    return 1;
$$ LANGUAGE plperl;
```

동일한 함수가 `plperlu`에서는 성공합니다 (슈퍼유저가 생성한 경우):

```perl
CREATE FUNCTION badfunc() RETURNS integer AS $$
    -- 위와 동일한 코드
$$ LANGUAGE plperlu;
```

### 6.6 주요 구현 세부 사항

- 별도의 인터프리터: PL/Perl 함수는 각 SQL 역할에 대해 별도의 Perl 인터프리터에서 실행됩니다
- 공유 상태: 세션의 모든 PL/PerlU 함수는 단일 Perl 인터프리터를 공유합니다 (데이터 공유 가능)
- 교차 통신 없음: PL/Perl과 PL/PerlU 함수는 서로 통신할 수 없습니다
- Perl 빌드 요구 사항: Perl이 `usemultiplicity` 또는 `useithreads`로 컴파일되어야 여러 인터프리터를 지원합니다; 그렇지 않으면 세션당 하나의 인터프리터만 사용 가능합니다

---

## 7. PL/Perl 트리거 (PL/Perl Triggers)

PL/Perl은 트리거 함수를 작성하는 데 사용할 수 있습니다. 해시 참조 `$_TD`는 현재 트리거 이벤트에 대한 정보를 포함하며 각 트리거 호출마다 별도의 로컬 값을 받는 전역 변수입니다.

### 7.1 트리거 변수 (`$_TD`)

| 변수 | 설명 |
|-----|------|
| `$_TD->{new}{foo}` | `foo` 열의 NEW 값 |
| `$_TD->{old}{foo}` | `foo` 열의 OLD 값 |
| `$_TD->{name}` | 호출되는 트리거의 이름 |
| `$_TD->{event}` | 트리거 이벤트: `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, 또는 `UNKNOWN` |
| `$_TD->{when}` | 트리거가 호출된 시점: `BEFORE`, `AFTER`, `INSTEAD OF`, 또는 `UNKNOWN` |
| `$_TD->{level}` | 트리거 레벨: `ROW`, `STATEMENT`, 또는 `UNKNOWN` |
| `$_TD->{relid}` | 트리거가 발동된 테이블의 OID |
| `$_TD->{table_name}` | 트리거가 발동된 테이블의 이름 |
| `$_TD->{table_schema}` | 트리거가 발동된 테이블의 스키마 이름 |
| `$_TD->{argc}` | 트리거 함수의 인수 개수 |
| `@{$_TD->{args}}` | 트리거 함수의 인수 (`argc` > 0인 경우) |

### 7.2 행 수준 트리거 반환 값 (Row-Level Trigger Return Values)

| 반환 값 | 동작 |
|--------|-----|
| `return;` | 작업 실행 |
| `"SKIP"` | 작업을 실행하지 않음 |
| `"MODIFY"` | NEW 행이 트리거 함수에 의해 수정되었음을 나타냄 |

### 7.3 예제

```sql
CREATE TABLE test (
    i int,
    v varchar
);

CREATE OR REPLACE FUNCTION valid_id() RETURNS trigger AS $$
    if (($_TD->{new}{i} >= 100) || ($_TD->{new}{i} <= 0)) {
        return "SKIP";    # INSERT/UPDATE 명령 건너뛰기
    } elsif ($_TD->{new}{v} ne "immortal") {
        $_TD->{new}{v} .= "(modified by trigger)";
        return "MODIFY";  # 행 수정 후 INSERT/UPDATE 명령 실행
    } else {
        return;           # INSERT/UPDATE 명령 실행
    }
$$ LANGUAGE plperl;

CREATE TRIGGER test_valid_id_trig
    BEFORE INSERT OR UPDATE ON test
    FOR EACH ROW EXECUTE FUNCTION valid_id();
```

---

## 8. PL/Perl 이벤트 트리거 (PL/Perl Event Triggers)

PL/Perl은 이벤트 트리거 함수를 작성하는 데 사용할 수 있습니다. 이벤트 트리거 함수는 특별한 해시 참조 `$_TD`를 사용하여 현재 트리거 이벤트에 대한 정보에 접근합니다.

### 8.1 $_TD 해시 참조

`$_TD`는 각 트리거 호출마다 별도의 로컬 값을 받는 전역 변수입니다. 다음 필드를 포함합니다:

| 필드 | 설명 |
|-----|------|
| `$_TD->{event}` | 트리거가 발동된 이벤트의 이름 |
| `$_TD->{tag}` | 트리거가 발동된 명령 태그 |

### 8.2 반환 값

트리거 함수의 반환 값은 무시됩니다.

### 8.3 예제

```sql
CREATE OR REPLACE FUNCTION perlsnitch() RETURNS event_trigger AS $$
  elog(NOTICE, "perlsnitch: " . $_TD->{event} . " " . $_TD->{tag} . " ");
$$ LANGUAGE plperl;

CREATE EVENT TRIGGER perl_a_snitch
    ON ddl_command_start
    EXECUTE FUNCTION perlsnitch();
```

이 예제는 다음을 보여줍니다:
- `event_trigger`를 반환하는 이벤트 트리거 함수 정의
- `$_TD->{event}` 및 `$_TD->{tag}`를 통한 이벤트 정보 접근
- `elog()`를 사용한 트리거 활동 로깅
- `ddl_command_start` 이벤트에서 실행되는 이벤트 트리거 생성

---

## 9. PL/Perl 내부 구조 (PL/Perl Under the Hood)

### 9.1 구성 (Configuration)

#### 9.1.1 `plperl.on_init` (string)

Perl 인터프리터가 처음 초기화될 때 `plperl` 또는 `plperlu`에 대한 특수화 전에 실행되는 Perl 코드를 지정합니다. 이 단계에서는 SPI 함수를 사용할 수 없습니다.

핵심 사항:
- 단일 문자열로 제한됩니다; 더 긴 코드는 모듈에 배치하고 로드해야 합니다
- 초기화가 실패하면 전체 인터프리터 초기화가 중단됩니다
- 로드된 모든 모듈은 `plperl`에서 사용 가능해집니다

예제:
```perl
plperl.on_init = 'require "plperlinit.pl"'
plperl.on_init = 'use lib "/my/app"; use MyApp::PgInit;'
```

로드된 모듈 보기:
```sql
DO 'elog(WARNING, join ", ", sort keys %INC)' LANGUAGE plperl;
```

중요 고려 사항:
- `shared_preload_libraries`에 포함된 경우, 초기화는 postmaster에서 발생합니다 (성능 이점이 있지만 보안 위험)
- Windows에서는 postmaster Perl 인터프리터가 자식 프로세스로 전파되지 않으므로 사전 로드의 이점이 없습니다
- `postgresql.conf` 또는 서버 명령줄에서만 설정할 수 있습니다

#### 9.1.2 `plperl.on_plperl_init` / `plperl.on_plperlu_init` (string)

인터프리터가 `plperl` 또는 `plperlu`에 대해 특수화될 때 각각 실행되는 Perl 코드입니다. 세션에서 첫 번째 함수 실행 시 또는 추가 인터프리터가 생성될 때 발생합니다.

핵심 사항:
- `plperl.on_init` 실행 후에 따릅니다
- 실행 중 SPI 함수를 사용할 수 없습니다
- `plperl.on_plperl_init` 코드는 인터프리터 "잠금" 후에 실행됩니다 (신뢰 작업만)
- 슈퍼유저만 이 설정을 변경할 수 있습니다
- 변경 사항은 현재 세션에서 이미 사용된 인터프리터에 영향을 미치지 않습니다

#### 9.1.3 `plperl.use_strict` (boolean)

true인 경우, 후속 PL/Perl 함수 컴파일에서 `strict` pragma가 활성화됩니다. 현재 세션에서 이미 컴파일된 함수에는 영향을 미치지 않습니다.

### 9.2 제한 사항 및 누락된 기능 (Limitations and Missing Features)

1. 직접 함수 호출: PL/Perl 함수는 서로 직접 호출할 수 없습니다

2. SPI 구현: SPI가 아직 완전히 구현되지 않았습니다

3. 대용량 데이터 집합:
   - `spi_exec_query`는 전체 결과 집합을 메모리에 로드합니다
   - 대용량 데이터셋에는 대신 `spi_query`/`spi_fetchrow`를 사용하세요
   - 집합 반환 함수는 대용량 결과 집합을 반환하는 대신 `return_next`를 사용해야 합니다

4. 세션 종료:
   - `END` 블록은 세션이 정상적으로 종료될 때 실행됩니다 (치명적 오류 시에는 아님)
   - 파일 핸들이 자동으로 플러시되지 않습니다
   - 객체가 자동으로 파괴되지 않습니다

---

## 참고 자료

- [PostgreSQL 공식 문서 - PL/Perl](https://www.postgresql.org/docs/current/plperl.html)
- [Perl 프로그래밍 언어](https://www.perl.org)
- [PostgreSQL 확장 프로그래밍](https://www.postgresql.org/docs/current/extend.html)
