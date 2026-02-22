# PL/Tcl - Tcl 절차적 언어 (PL/Tcl - Tcl Procedural Language)

## 목차
1. [개요](#1-개요-overview)
2. [PL/Tcl 함수와 인수](#2-pltcl-함수와-인수-pltcl-functions-and-arguments)
3. [PL/Tcl의 데이터 값](#3-pltcl의-데이터-값-data-values-in-pltcl)
4. [PL/Tcl의 전역 데이터](#4-pltcl의-전역-데이터-global-data-in-pltcl)
5. [PL/Tcl에서 데이터베이스 접근](#5-pltcl에서-데이터베이스-접근-database-access-from-pltcl)
6. [PL/Tcl 트리거 함수](#6-pltcl-트리거-함수-trigger-functions-in-pltcl)
7. [PL/Tcl 이벤트 트리거 함수](#7-pltcl-이벤트-트리거-함수-event-trigger-functions-in-pltcl)
8. [PL/Tcl 에러 처리](#8-pltcl-에러-처리-error-handling-in-pltcl)
9. [PL/Tcl의 명시적 서브트랜잭션](#9-pltcl의-명시적-서브트랜잭션-explicit-subtransactions-in-pltcl)
10. [트랜잭션 관리](#10-트랜잭션-관리-transaction-management)
11. [PL/Tcl 설정](#11-pltcl-설정-pltcl-configuration)
12. [Tcl 프로시저 이름](#12-tcl-프로시저-이름-tcl-procedure-names)

---

## 1. 개요 (Overview)

### 1.1 PL/Tcl이란?

PL/Tcl 은 PostgreSQL을 위한 로드 가능한 절차적 언어(loadable procedural language)로, PostgreSQL 함수와 프로시저 작성에 [Tcl 언어](https://www.tcl.tk/)를 사용할 수 있게 해줍니다. PL/Tcl은 C 언어 함수의 대부분의 기능을 제공하면서 Tcl의 강력한 문자열 처리 라이브러리의 이점을 추가로 제공합니다.

### 1.2 주요 특징

보안상의 이점:
- 안전한 Tcl 인터프리터(safe Tcl interpreter) 컨텍스트 내에서 실행됩니다
- 제한된 명령 집합과 제한된 데이터베이스 접근만 가능합니다
- 특정 SPI 명령과 `elog()` 메시지 기능만 사용 가능합니다
- 데이터베이스 서버 내부나 OS 수준 권한에 대한 접근이 없습니다
- 비권한 데이터베이스 사용자도 이 언어를 안전하게 사용할 수 있습니다

### 1.3 구현 제한사항

1. 새로운 데이터 타입에 대한 입출력 함수(input/output functions) 생성에 사용할 수 없습니다
2. 안전한 Tcl 명령 집합으로 제한됩니다

### 1.4 PL/TclU (비신뢰 Tcl 변형)

제한 없는 Tcl 기능(예: 이메일 전송)이 필요한 경우 PL/TclU 를 사용합니다:
- 안전 모드 대신 전체 Tcl 인터프리터를 사용합니다
- 비신뢰 언어(untrusted language)로 설치해야 합니다 - 슈퍼유저만 함수를 생성할 수 있습니다
- 전체 시스템 접근이 가능하므로 함수 작성자가 보안을 보장해야 합니다
- 관리자 수준의 권한을 갖습니다

### 1.5 설치

공유 객체 코드(shared object code)는 설치 시 Tcl 지원이 설정되면 자동으로 빌드됩니다.

데이터베이스에 설치하려면:

```sql
CREATE EXTENSION pltcl;    -- 안전한 PL/Tcl용
CREATE EXTENSION pltclu;   -- 비신뢰 PL/TclU용
```

---

## 2. PL/Tcl 함수와 인수 (PL/Tcl Functions and Arguments)

### 2.1 기본 함수 생성

PL/Tcl 함수는 표준 SQL 구문을 사용하여 생성합니다:

```sql
CREATE FUNCTION funcname (argument-types) RETURNS return-type AS $$
    # PL/Tcl 함수 본문
$$ LANGUAGE pltcl;
```

PL/TclU의 경우 언어 지정만 다르게 `pltclu`를 사용합니다.

### 2.2 함수 인수

- 함수 인수는 `$1`, `$2` 등의 이름을 가진 Tcl 변수로 전달됩니다
- 결과는 표준 Tcl `return` 문을 사용하여 반환됩니다
- 프로시저에서는 Tcl 코드의 반환 값이 무시됩니다

### 2.3 간단한 함수 예제

```sql
CREATE FUNCTION tcl_max(integer, integer) RETURNS integer AS $$
    if {$1 > $2} {return $1}
    return $2
$$ LANGUAGE pltcl STRICT;
```

`STRICT` 절은 null 인수로 함수가 호출되는 것을 방지합니다.

### 2.4 NULL 값 처리

비엄격(non-strict) 함수에서 null 인수는 빈 문자열이 됩니다. null을 감지하려면 `argisnull`을 사용합니다:

```sql
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

null 값을 반환하려면 `return_null`을 사용합니다.

### 2.5 복합 타입 인수 (Composite-Type Arguments)

복합 타입은 속성 이름과 일치하는 요소 이름을 가진 Tcl 배열로 전달됩니다. Null 속성은 배열에 나타나지 않습니다:

```sql
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

### 2.6 복합 타입 반환

예상 결과 타입과 일치하는 열 이름/값 쌍의 리스트를 반환합니다:

```sql
CREATE FUNCTION square_cube(in int, out squared int, out cubed int) AS $$
    return [list squared [expr {$1 * $1}] cubed [expr {$1 * $1 * $1}]]
$$ LANGUAGE pltcl;
```

배열 기반 반환에 `array get`을 사용합니다:

```sql
CREATE FUNCTION raise_pay(employee, delta int) RETURNS employee AS $$
    set 1(salary) [expr {$1(salary) + $2}]
    return [array get 1]
$$ LANGUAGE pltcl;
```

### 2.7 집합 반환 (Returning Sets)

각 행에 대해 `return_next`를 사용합니다:

스칼라 타입:
```sql
CREATE FUNCTION sequence(int, int) RETURNS SETOF int AS $$
    for {set i $1} {$i < $2} {incr i} {
        return_next $i
    }
$$ LANGUAGE pltcl;
```

복합 타입:
```sql
CREATE FUNCTION table_of_squares(int, int) RETURNS TABLE (x int, x2 int) AS $$
    for {set i $1} {$i < $2} {incr i} {
        return_next [list x $i x2 [expr {$i * $i}]]
    }
$$ LANGUAGE pltcl;
```

---

## 3. PL/Tcl의 데이터 값 (Data Values in PL/Tcl)

### 3.1 인수 값

PL/Tcl 함수에 제공되는 입력 인수는 텍스트 형식으로 변환됩니다. 이 변환은 `SELECT` 문으로 값을 표시한 것과 동일합니다.

### 3.2 반환 값

`return`과 `return_next` 명령은 다음에 대해 유효한 입력 형식인 모든 문자열을 허용합니다:
- 함수의 선언된 결과 타입
- 또는 복합 결과 타입의 지정된 열

### 3.3 요약

PL/Tcl에서 데이터 값은 텍스트 변환을 통해 처리됩니다:
- 입력 → 텍스트: 함수 인수는 텍스트 표현으로 변환됩니다
- 텍스트 → 출력: return 문은 예상 출력 타입의 입력 형식에 맞는 텍스트 문자열을 허용합니다

개발자는 값이 텍스트 문자열로 교환되며, PostgreSQL이 텍스트 표현과 실제 데이터 타입 간의 타입 변환을 처리한다는 것을 인지해야 합니다.

---

## 4. PL/Tcl의 전역 데이터 (Global Data in PL/Tcl)

### 4.1 개요

PL/Tcl의 전역 데이터를 사용하면 함수 호출 간에 데이터를 유지하거나 다른 함수 간에 공유할 수 있습니다. 다만 중요한 보안 및 범위 고려사항이 있습니다.

### 4.2 주요 보안 제한

SQL 역할(Role)별 인터프리터 분리:
- 보안상의 이유로 각 SQL 역할마다 별도의 Tcl 인터프리터를 갖습니다
- 이는 사용자 PL/Tcl 함수 간의 우발적이거나 악의적인 간섭을 방지합니다
- 전역 Tcl 변수는 동일한 SQL 역할에 의해 실행되는 함수 간에만 공유됩니다

역할 간 데이터 공유:
애플리케이션이 여러 SQL 역할에서 코드를 실행하는 경우(`SECURITY DEFINER` 함수, `SET ROLE` 등을 통해):
- 통신해야 하는 함수가 동일한 사용자가 소유하도록 해야 합니다
- `SECURITY DEFINER`로 표시해야 합니다
- 이러한 함수가 오용되지 않도록 주의해야 합니다

### 4.3 PL/TclU vs PL/Tcl

PL/TclU 함수:
- 모든 함수가 동일한 Tcl 인터프리터에서 실행됩니다(세션 내에서 공유)
- 전역 데이터가 PL/TclU 함수 간에 자동으로 공유됩니다
- 모든 PL/TclU 함수가 슈퍼유저 신뢰 수준에서 실행되므로 보안 위험으로 간주되지 않습니다

### 4.4 GD 배열 사용 (권장 방식)

특별한 전역 배열 `GD`가 `upvar` 명령을 통해 각 함수에 제공됩니다:
- 전역 이름: 함수의 내부 이름
- 로컬 이름: `GD`
- 목적: 개별 함수에 대한 영구 프라이빗 데이터 저장
- 범위: 특정 인터프리터 내에서만 전역(보안 제한 준수)

모범 사례:
- 함수의 영구 프라이빗 데이터에는 `GD`를 사용합니다
- 여러 함수 간에 특별히 공유하려는 값에만 일반 Tcl 전역 변수를 사용합니다

---

## 5. PL/Tcl에서 데이터베이스 접근 (Database Access from PL/Tcl)

### 5.1 사용 가능한 명령

#### 5.1.1 spi_exec - SQL 명령 실행

구문:
```tcl
spi_exec ?-count n? ?-array name? command ?loop-body?
```

설명:
- 문자열로 주어진 SQL 명령을 실행합니다
- 처리된 행 수(선택, 삽입, 업데이트 또는 삭제)를 반환합니다
- 유틸리티 문의 경우 0을 반환합니다
- 명령 실패 시 오류를 발생시킵니다

주요 옵션:
- `-count n`: n개 행을 검색한 후 중지합니다(LIMIT과 유사). n이 0이면 완료까지 실행합니다
- `-array name`: 개별 변수 대신 연관 배열에 열 값을 저장합니다
- `loop-body`: 결과의 각 행에 대해 한 번씩 실행되는 선택적 Tcl 스크립트

동작:
- `loop-body` 없는 SELECT 문: 첫 번째 행만 저장되고 나머지 행은 무시됩니다
- 열 이름을 따라 명명된 Tcl 변수에 열 값이 저장됩니다
- 행 번호는 배열 요소 `.tupno`에 저장됩니다(`-array` 사용 시)
- NULL 열은 "unset" 변수가 됩니다

예제:
```tcl
spi_exec "SELECT count(*) AS cnt FROM pg_proc"
# $cnt를 pg_proc의 행 수로 설정

spi_exec -array C "SELECT * FROM pg_class" {
    elog DEBUG "have table $C(relname)"
}
# pg_class의 모든 행에 대해 로그 메시지 출력
```

#### 5.1.2 spi_prepare - 쿼리 계획 준비

구문:
```tcl
spi_prepare query typelist
```

설명:
- 나중에 실행할 쿼리 계획을 준비하고 저장합니다
- 계획은 현재 세션 기간 동안 유지됩니다
- 플레이스홀더로 `$1`, `$2` 등을 사용하는 매개변수화된 쿼리를 지원합니다
- `spi_execp`와 함께 사용할 쿼리 ID를 반환합니다

매개변수:
- `query`: 매개변수 플레이스홀더가 있는 SQL 쿼리 문자열
- `typelist`: 매개변수 타입 이름의 Tcl 리스트(매개변수가 없으면 빈 리스트)

예제:
```tcl
set plan [spi_prepare \
    "SELECT count(*) AS cnt FROM t1 WHERE num >= $1 AND num <= $2" \
    [list int4 int4]]
```

#### 5.1.3 spi_execp - 준비된 계획 실행

구문:
```tcl
spi_execp ?-count n? ?-array name? ?-nulls string? queryid ?value-list? ?loop-body?
```

설명:
- `spi_prepare`로 이전에 준비한 쿼리를 실행합니다
- 쿼리/매개변수 지정을 제외하고 `spi_exec`와 동일하게 작동합니다

옵션:
- `-count n`, `-array name`, `loop-body`: `spi_exec`와 동일
- `-nulls string`: 어떤 매개변수가 null인지 나타내는 공백과 'n' 문자의 문자열. `value-list`의 길이와 일치해야 합니다

예제:
```tcl
CREATE FUNCTION t1_count(integer, integer) RETURNS integer AS $$
    if {![info exists GD(plan)]} {
        set GD(plan) [spi_prepare \
            "SELECT count(*) AS cnt FROM t1 WHERE num >= \$1 AND num <= \$2" \
            [list int4 int4]]
    }
    spi_execp -count 1 $GD(plan) [list $1 $2]
    return $cnt
$$ LANGUAGE pltcl;
```

장점: `spi_exec`와 달리 매개변수에 수동 따옴표 처리가 필요하지 않습니다

#### 5.1.4 subtransaction - SQL 서브트랜잭션

구문:
```tcl
subtransaction command
```

설명:
- SQL 서브트랜잭션 내에서 Tcl 스크립트를 실행합니다
- 스크립트가 오류를 반환하면 전체 서브트랜잭션이 롤백됩니다
- 오류가 외부 트랜잭션에 영향을 미치는 것을 방지합니다

#### 5.1.5 quote - 문자열 리터럴 이스케이프

구문:
```tcl
quote string
```

설명:
- 모든 작은따옴표와 백슬래시 문자를 두 배로 만듭니다
- `spi_exec` 또는 `spi_prepare`의 SQL 명령에서 문자열을 안전하게 따옴표 처리합니다

예제:
```tcl
# quote 없이: "SELECT 'doesn't' AS ret" → 파싱 오류
# quote 사용:
"SELECT '[quote $val]' AS ret"
# 결과: "SELECT 'doesn''t' AS ret"
```

#### 5.1.6 elog - 로깅 및 에러 메시지

구문:
```tcl
elog level msg
```

로그 수준:
- `DEBUG`, `LOG`, `INFO`, `NOTICE`, `WARNING`: 다른 우선순위 수준에서 메시지 생성
- `ERROR`: 오류 조건 발생; 현재 트랜잭션/서브트랜잭션 중단
- `FATAL`: 트랜잭션 중단 및 현재 세션 종료

설정:
- 출력은 `log_min_messages` 및 `client_min_messages` 설정에 의해 제어됩니다

### 5.2 주요 기능 요약

| 기능 | 설명 |
|------|------|
| 매개변수 지원 | 매개변수화된 쿼리를 사용하여 SQL 인젝션 방지 |
| 배열 저장 | 배열 인덱스를 통해 결과 열에 접근 |
| 루프 처리 | 여러 결과 행을 반복 |
| NULL 처리 | NULL 열 값의 자동 unset |
| 준비된 계획 | 성능 향상을 위한 쿼리 계획 재사용 |
| 안전한 이스케이프 | SQL 인젝션 방지를 위한 내장 함수 |

---

## 6. PL/Tcl 트리거 함수 (Trigger Functions in PL/Tcl)

### 6.1 개요

PL/Tcl의 트리거 함수는 인수 없이 선언하고 반환 타입이 `trigger`여야 합니다.

### 6.2 트리거 변수

트리거 관리자가 다음 변수를 통해 정보를 전달합니다:

| 변수 | 설명 |
|------|------|
| `$TG_name` | `CREATE TRIGGER` 문의 트리거 이름 |
| `$TG_relid` | 트리거를 호출한 테이블의 객체 ID |
| `$TG_table_name` | 트리거를 호출한 테이블 이름 |
| `$TG_table_schema` | 트리거를 호출한 테이블의 스키마 |
| `$TG_relatts` | 열 이름의 Tcl 리스트(빈 요소 접두사 포함; 1-인덱스) |
| `$TG_when` | `BEFORE`, `AFTER`, 또는 `INSTEAD OF` |
| `$TG_level` | `ROW` 또는 `STATEMENT` |
| `$TG_op` | `INSERT`, `UPDATE`, `DELETE`, 또는 `TRUNCATE` |
| `$NEW` | 새 행 값의 연관 배열(INSERT/UPDATE만; 행 수준만) |
| `$OLD` | 이전 행 값의 연관 배열(UPDATE/DELETE만; 행 수준만) |
| `$args` | `CREATE TRIGGER`의 함수 인수 Tcl 리스트(`$1`...`$n`으로 접근 가능) |

### 6.3 반환 값

트리거 함수는 다음 중 하나를 반환해야 합니다:

- `OK`: 작업이 정상적으로 진행됩니다
- `SKIP`: 이 행에 대한 작업을 조용히 억제합니다
- 열 이름/값 쌍의 리스트: 수정된 행을 반환합니다(행 수준 `BEFORE INSERT/UPDATE` 또는 `INSTEAD OF INSERT/UPDATE` 트리거에만 의미 있음)

> 팁: 배열 표현을 리스트 형식으로 변환하려면 `array get`을 사용합니다

### 6.4 예제: 업데이트 카운터 트리거

```sql
CREATE FUNCTION trigfunc_modcount() RETURNS trigger AS $$
    switch $TG_op {
        INSERT {
            set NEW($1) 0
        }
        UPDATE {
            set NEW($1) $OLD($1)
            incr NEW($1)
        }
        default {
            return OK
        }
    }
    return [array get NEW]
$$ LANGUAGE pltcl;

CREATE TABLE mytab (num integer, description text, modcnt integer);

CREATE TRIGGER trig_mytab_modcount BEFORE INSERT OR UPDATE ON mytab
    FOR EACH ROW EXECUTE FUNCTION trigfunc_modcount('modcnt');
```

이 예제는 삽입 시 카운터를 0으로 초기화하고 각 업데이트마다 증가시킵니다. 트리거는 열 이름 인수를 통해 다른 테이블에서 재사용할 수 있습니다.

---

## 7. PL/Tcl 이벤트 트리거 함수 (Event Trigger Functions in PL/Tcl)

### 7.1 개요

이벤트 트리거 함수는 PL/Tcl로 작성할 수 있습니다. PostgreSQL은 이벤트 트리거로 호출될 함수가 다음과 같이 선언되어야 합니다:
- 인수 없음
- 반환 타입: `event_trigger`

### 7.2 사용 가능한 변수

트리거 관리자가 다음 변수를 통해 함수 본문에 정보를 전달합니다:

| 변수 | 설명 |
|------|------|
| `$TG_event` | 트리거가 실행된 이벤트 이름 |
| `$TG_tag` | 트리거가 실행된 명령 태그 |

### 7.3 반환 값

트리거 함수의 반환 값은 무시됩니다.

### 7.4 예제

지원되는 명령이 실행될 때마다 `NOTICE` 메시지를 발생시키는 간단한 이벤트 트리거 함수입니다:

```sql
CREATE OR REPLACE FUNCTION tclsnitch() RETURNS event_trigger AS $$
  elog NOTICE "tclsnitch: $TG_event $TG_tag"
$$ LANGUAGE pltcl;

CREATE EVENT TRIGGER tcl_a_snitch ON ddl_command_start EXECUTE FUNCTION tclsnitch();
```

이 예제는:
1. 인수를 받지 않고 `event_trigger`를 반환하는 `tclsnitch()` 함수를 정의합니다
2. `elog`를 사용하여 이벤트 이름과 명령 태그가 포함된 알림 메시지를 기록합니다
3. DDL 명령 시작 이벤트에서 실행되는 이벤트 트리거를 생성합니다

---

## 8. PL/Tcl 에러 처리 (Error Handling in PL/Tcl)

### 8.1 개요

PL/Tcl 함수 내의 Tcl 코드는 잘못된 작업이나 명시적 오류 명령을 통해 오류를 발생시킬 수 있습니다. 오류는 Tcl의 `catch` 명령으로 잡거나, 잡히지 않으면 SQL 오류로 전파됩니다.

### 8.2 오류 원인과 전파

Tcl 생성 오류:
- 잘못된 작업
- Tcl `error` 명령
- PL/Tcl `elog` 명령
- Tcl의 `catch` 명령으로 잡을 수 있음

SQL 오류:
- PL/Tcl의 `spi_exec`, `spi_prepare`, `spi_execp` 명령 내에서 발생
- Tcl 오류로 보고됨(`catch`로 잡을 수 있음)
- 각 명령은 오류 시 롤백되는 서브트랜잭션에서 실행됨

잡히지 않은 오류 는 최상위 수준으로 전파되어 호출하는 쿼리에서 SQL 오류로 보고됩니다.

### 8.3 오류 정보: errorCode 변수

Tcl의 `errorCode` 변수는 리스트 형식으로 추가 오류 정보를 포함합니다:

데이터베이스 오류 구조:
- 첫 번째 단어: `POSTGRES`
- 두 번째 단어: PostgreSQL 버전 번호
- 나머지 단어: 필드 이름/값 쌍

항상 제공되는 필드:
- `SQLSTATE` - 오류 코드
- `condition` - 조건 이름(부록 A 참조)
- `message` - 오류 메시지

선택적 필드:
`detail`, `hint`, `context`, `schema`, `table`, `column`, `datatype`, `constraint`, `statement`, `cursor_position`, `filename`, `lineno`, `funcname`

### 8.4 예제: errorCode 작업

```tcl
if {[catch { spi_exec $sql_command }]} {
    if {[lindex $::errorCode 0] == "POSTGRES"} {
        array set errorArray $::errorCode
        if {$errorArray(condition) == "undefined_table"} {
            # 누락된 테이블 처리
        } else {
            # 다른 유형의 SQL 오류 처리
        }
    }
}
```

이중 콜론(`::`)은 `errorCode`를 전역 변수로 명시적으로 참조합니다.

---

## 9. PL/Tcl의 명시적 서브트랜잭션 (Explicit Subtransactions in PL/Tcl)

### 9.1 개요

PL/Tcl의 명시적 서브트랜잭션은 여러 데이터베이스 작업이 단일 단위로 함께 성공하거나 실패해야 할 때 데이터 일관성을 유지하는 솔루션을 제공합니다.

### 9.2 문제점

명시적 서브트랜잭션 없이는 한 작업의 오류가 이전 작업을 자동으로 롤백하지 않습니다. 예:

```tcl
CREATE FUNCTION transfer_funds() RETURNS void AS $$
    if [catch {
        spi_exec "UPDATE accounts SET balance = balance - 100 WHERE account_name = 'joe'"
        spi_exec "UPDATE accounts SET balance = balance + 100 WHERE account_name = 'mary'"
    } errormsg] {
        set result [format "error transferring funds: %s" $errormsg]
    } else {
        set result "funds transferred successfully"
    }
    spi_exec "INSERT INTO operations (result) VALUES ('[quote $result]')"
$$ LANGUAGE pltcl;
```

두 번째 UPDATE가 실패하면 첫 번째는 여전히 커밋되어 계좌 잔액이 일관되지 않게 됩니다.

### 9.3 해결책: `subtransaction` 명령

여러 작업을 `subtransaction` 블록으로 감싸서 모두 성공하거나 모두 롤백되도록 합니다:

```tcl
CREATE FUNCTION transfer_funds2() RETURNS void AS $$
    if [catch {
        subtransaction {
            spi_exec "UPDATE accounts SET balance = balance - 100 WHERE account_name = 'joe'"
            spi_exec "UPDATE accounts SET balance = balance + 100 WHERE account_name = 'mary'"
        }
    } errormsg] {
        set result [format "error transferring funds: %s" $errormsg]
    } else {
        set result "funds transferred successfully"
    }
    spi_exec "INSERT INTO operations (result) VALUES ('[quote $result]')"
$$ LANGUAGE pltcl;
```

### 9.4 핵심 사항

- 오류를 처리하고 전파되는 것을 방지하려면 `catch`가 여전히 필요합니다
- 모든 오류에서 롤백 트리거: 데이터베이스 오류와 일반 Tcl 예외 모두 롤백을 유발합니다
- 비오류 종료는 롤백하지 않음: 서브트랜잭션 내에서 `return`을 사용해도 롤백이 트리거되지 않습니다
- subtransaction 명령 자체는 오류를 트랩하지 않습니다 - 원자적 롤백 동작만 보장합니다

---

## 10. 트랜잭션 관리 (Transaction Management)

### 10.1 개요

최상위 수준에서 호출된 PL/Tcl 프로시저 또는 익명 코드 블록(`DO` 명령)에서 두 가지 명령을 사용하여 트랜잭션을 제어할 수 있습니다:

- `commit`: 현재 트랜잭션을 커밋합니다
- `rollback`: 현재 트랜잭션을 롤백합니다

### 10.2 중요 참고사항

1. SQL 명령 사용 불가: `spi_exec` 또는 유사한 함수를 통해 SQL `COMMIT` 또는 `ROLLBACK` 명령을 사용할 수 없습니다. 대신 `commit` 및 `rollback` PL/Tcl 명령을 사용해야 합니다.

2. 자동 트랜잭션 시작: 트랜잭션이 종료된 후 새 트랜잭션이 자동으로 시작됩니다. 새 트랜잭션을 시작하는 별도의 명령이 필요하지 않습니다.

3. 서브트랜잭션 제한: 명시적 서브트랜잭션이 활성화되어 있으면 트랜잭션을 종료할 수 없습니다.

### 10.3 예제

```tcl
CREATE PROCEDURE transaction_test1()
LANGUAGE pltcl
AS $$
for {set i 0} {$i < 10} {incr i} {
    spi_exec "INSERT INTO test1 (a) VALUES ($i)"
    if {$i % 2 == 0} {
        commit
    } else {
        rollback
    }
}
$$;

CALL transaction_test1();
```

이 예제는 `test1`에 10개의 행을 삽입하고, 짝수 반복에서 커밋하고 홀수 반복에서 롤백합니다.

---

## 11. PL/Tcl 설정 (PL/Tcl Configuration)

### 11.1 설정 매개변수

#### 11.1.1 pltcl.start_proc (문자열)

목적: PL/Tcl용 새 Tcl 인터프리터가 생성될 때마다 실행할 매개변수 없는 PL/Tcl 함수의 이름을 지정합니다.

주요 세부사항:
- 세션별 초기화를 활성화합니다(예: 추가 Tcl 코드 로딩)
- 새 인터프리터가 생성되는 시점:
  - 데이터베이스 세션에서 PL/Tcl 함수가 처음 실행될 때
  - 새 SQL 역할에 대해 추가 인터프리터가 필요할 때
- 참조된 함수는:
  - `pltcl` 언어로 작성되어야 합니다
  - `SECURITY DEFINER`로 표시되면 안 됩니다
  - 현재 사용자가 호출할 수 있어야 합니다
- 슈퍼유저만 이 설정을 변경할 수 있습니다
- 초기화 함수가 실패하면 호출 함수를 중단하고 오류를 전파하여 현재 트랜잭션/서브트랜잭션을 중단합니다
- 세션 내 변경은 이미 생성된 인터프리터에 영향을 미치지 않습니다

#### 11.1.2 pltclu.start_proc (문자열)

목적: `pltcl.start_proc`과 동일하지만 PL/TclU 에 적용됩니다.

- 참조된 함수는 `pltclu` 언어로 작성되어야 합니다
- 다른 모든 동작과 제한은 `pltcl.start_proc`과 동일합니다

---

## 12. Tcl 프로시저 이름 (Tcl Procedure Names)

### 12.1 개요

PostgreSQL의 PL/Tcl에서 Tcl 프로시저 이름은 Tcl이 고유한 프로시저 이름을 요구하기 때문에 일반 PostgreSQL 함수와 다르게 관리됩니다.

### 12.2 핵심 사항

#### 12.2.1 이름 생성 전략

PL/Tcl은 다음과 같이 내부 Tcl 프로시저 이름을 생성합니다:
1. 내부 Tcl 프로시저 이름에 인수 타입 이름 포함
2. 동일한 Tcl 인터프리터에서 이전에 로드된 함수 간의 고유성을 보장하기 위해 필요시 함수의 객체 ID(OID) 추가

#### 12.2.2 함수 오버로딩 처리

- PostgreSQL은 다른 스키마나 다른 인수 타입/개수로 동일한 함수 이름을 허용합니다
- Tcl은 모든 프로시저 이름이 구별되어야 합니다
- PL/Tcl은 이름이 같지만 인수 타입이 다른 PostgreSQL 함수에 대해 다른 Tcl 프로시저를 생성하여 이를 해결합니다

#### 12.2.3 중요 제한: 직접 함수 호출

PL/Tcl 함수는 Tcl 코드 내에서 다른 PL/Tcl 함수를 직접 호출할 수 없습니다.

다른 PL/Tcl 함수를 호출해야 하는 경우 SQL을 통해 다음을 사용해야 합니다:
- `spi_exec` 명령, 또는
- 관련 SPI(서버 프로그래밍 인터페이스) 명령

### 12.3 실질적 영향

- 내부 Tcl 프로시저 이름 지정은 일반적으로 PL/Tcl 프로그래머에게 투명합니다
- 디버깅 세션 중에 보일 수 있습니다
- 개발자는 함수 간 직접 호출 제한을 인지하고 대신 SQL 중개자를 사용해야 합니다

---

## 참고 자료 (References)

- [PostgreSQL 공식 문서 - PL/Tcl](https://www.postgresql.org/docs/current/pltcl.html)
- [Tcl 공식 웹사이트](https://www.tcl.tk/)
- [PostgreSQL 공식 문서 - 에러 코드](https://www.postgresql.org/docs/current/errcodes-appendix.html)
