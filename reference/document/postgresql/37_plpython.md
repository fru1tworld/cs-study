# PL/Python - Python 절차적 언어 (Python Procedural Language)

## 목차
1. [개요](#1-개요)
2. [PL/Python 함수](#2-plpython-함수-plpython-functions)
3. [데이터 값](#3-데이터-값-data-values)
4. [함수 간 데이터 공유](#4-함수-간-데이터-공유-sharing-data)
5. [익명 코드 블록](#5-익명-코드-블록-anonymous-code-blocks)
6. [트리거 함수](#6-트리거-함수-trigger-functions)
7. [데이터베이스 접근](#7-데이터베이스-접근-database-access)
8. [명시적 서브트랜잭션](#8-명시적-서브트랜잭션-explicit-subtransactions)
9. [트랜잭션 제어](#9-트랜잭션-제어-transaction-control)
10. [유틸리티 함수](#10-유틸리티-함수-utility-functions)
11. [환경 변수](#11-환경-변수-environment-variables)

---

## 1. 개요

PL/Python은 PostgreSQL 함수와 프로시저를 [Python 프로그래밍 언어](https://www.python.org)로 작성할 수 있게 해주는 로드 가능한 절차적 언어(loadable procedural language)입니다.

### 1.1 설치

특정 데이터베이스에 PL/Python을 설치하려면 다음 명령을 사용합니다:

```sql
CREATE EXTENSION plpython3u;
```

팁: `template1`에 언어를 설치하면, 이후에 생성되는 모든 데이터베이스에 해당 언어가 자동으로 설치됩니다.

참고:
- 소스 패키지 사용자는 설치 과정에서 PL/Python 빌드를 특별히 활성화해야 합니다.
- 바이너리 패키지 사용자는 별도의 서브패키지에서 PL/Python을 찾을 수 있습니다.

### 1.2 보안 고려사항

PL/Python은 "비신뢰(untrusted)" 언어입니다:

- 사용자 행동에 대한 제한이 없습니다
- 언어 이름의 "u"는 "untrusted"를 의미합니다 (`plpython3u`)
- 슈퍼유저(superuser)만 비신뢰 언어로 함수를 생성할 수 있습니다
- 데이터베이스 관리자 권한으로 실행되므로 코드가 오용되지 않도록 주의해야 합니다

> 참고: 향후 안전한 실행 메커니즘이 개발되면 신뢰 버전인 `plpython`이 제공될 수 있습니다.

---

## 2. PL/Python 함수 (PL/Python Functions)

### 2.1 기본 구문

PL/Python 함수는 표준 `CREATE FUNCTION` 구문을 사용하여 생성합니다:

```sql
CREATE FUNCTION funcname (argument-list)
  RETURNS return-type
AS $$
  # PL/Python 함수 본문
$$ LANGUAGE plpython3u;
```

### 2.2 인수 처리

- 인수는 `args` 리스트의 요소로 전달됩니다
- 명명된 인수는 일반 변수로 전달됩니다 (가독성을 위해 권장)
- 인수는 함수 범위 내에서 전역 변수로 설정됩니다

### 2.3 반환 값

- 표준 Python `return` 문을 사용합니다
- 결과 집합 문(result-set statements)에는 `yield`를 사용합니다
- 반환 값이 없으면 Python의 `None`이 반환됩니다
- `None`은 SQL `NULL`로 변환됩니다
- 프로시저(Procedures) 는 `None`을 반환해야 합니다 (return 없이 종료하거나 인수 없는 `return` 사용)

### 2.4 기본 예제

```sql
CREATE FUNCTION pymax (a integer, b integer)
  RETURNS integer
AS $$
  if a > b:
    return a
  return b
$$ LANGUAGE plpython3u;
```

### 2.5 스코핑 규칙 주의사항

Python의 스코핑 규칙으로 인해, 인수 변수를 자기 자신을 포함하는 표현식으로 재할당하지 마세요:

#### 작동하지 않는 예:
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  x = x.strip()  -- 오류: x가 로컬 변수가 됨
  return x
$$ LANGUAGE plpython3u;
```

#### 작동하지만 권장하지 않는 방법:
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  global x
  x = x.strip()
  return x
$$ LANGUAGE plpython3u;
```

#### 권장하는 방법 (읽기 전용 매개변수):
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  return x.strip()
$$ LANGUAGE plpython3u;
```

---

## 3. 데이터 값 (Data Values)

### 3.1 데이터 타입 매핑

#### PostgreSQL에서 Python으로 변환

PL/Python 함수가 인수를 받을 때 적용되는 타입 매핑:

| PostgreSQL 타입 | Python 타입 |
|----------------|-------------|
| `boolean` | `bool` |
| `smallint`, `int`, `bigint`, `oid` | `int` |
| `real`, `double precision` | `float` |
| `numeric` | `decimal.Decimal` |
| `bytea` | `bytes` |
| 문자열 타입 및 기타 | `str` (유니코드) |

#### Python에서 PostgreSQL로 반환 변환

- boolean: Python 진리값 규칙에 따라 평가 (0과 빈 문자열은 false, `'f'`는 true)
- bytea: Python 내장 함수를 사용하여 변환
- 기타: `str()` 함수를 통해 문자열로 변환 (float는 정밀도 유지를 위해 `repr()` 사용)
- 문자열: PostgreSQL 서버 인코딩으로 자동 변환

### 3.2 NULL 처리

SQL NULL 값은 Python에서 `None`으로 나타납니다:

```sql
CREATE FUNCTION pymax (a integer, b integer)
  RETURNS integer
AS $$
  if (a is None) or (b is None):
    return None
  if a > b:
    return a
  return b
$$ LANGUAGE plpython3u;
```

### 3.3 배열과 리스트

SQL 배열은 Python 리스트로 매핑됩니다:

```sql
CREATE FUNCTION return_arr()
  RETURNS int[]
AS $$
return [1, 2, 3, 4, 5]
$$ LANGUAGE plpython3u;
```

다차원 배열은 중첩 리스트가 됩니다:

```sql
CREATE FUNCTION test_type_conversion_array_int4(x int4[])
RETURNS int4[] AS $$
return x
$$ LANGUAGE plpython3u;
```

### 3.4 복합 타입 (Composite Types)

#### 복합 타입 인수 수신

복합 타입은 Python 매핑(딕셔너리)으로 전달됩니다:

```sql
CREATE FUNCTION overpaid (e employee)
  RETURNS boolean
AS $$
  if e["salary"] > 200000:
    return True
  if (e["age"] < 30) and (e["salary"] > 100000):
    return True
  return False
$$ LANGUAGE plpython3u;
```

#### 복합 타입 반환

시퀀스(튜플/리스트)로 반환:
```sql
CREATE FUNCTION make_pair (name text, value integer)
  RETURNS named_value
AS $$
  return ( name, value )
$$ LANGUAGE plpython3u;
```

딕셔너리로 반환:
```sql
CREATE FUNCTION make_pair (name text, value integer)
  RETURNS named_value
AS $$
  return { "name": name, "value": value }
$$ LANGUAGE plpython3u;
```

속성을 가진 객체로 반환:
```sql
CREATE FUNCTION make_pair (name text, value integer)
  RETURNS named_value
AS $$
  class nv: pass
  nv.name = name
  nv.value = value
  return nv
$$ LANGUAGE plpython3u;
```

### 3.5 집합 반환 함수 (Set-Returning Functions)

여러 행을 반환하는 세 가지 방법:

시퀀스(튜플/리스트/세트):
```sql
CREATE FUNCTION greet (how text)
  RETURNS SETOF greeting
AS $$
  return ( [ how, "World" ], [ how, "PostgreSQL" ], [ how, "PL/Python" ] )
$$ LANGUAGE plpython3u;
```

반복자(Iterator):
```sql
CREATE FUNCTION greet (how text)
  RETURNS SETOF greeting
AS $$
  class producer:
    def __init__ (self, how, who):
      self.how = how
      self.who = who
      self.ndx = -1
    def __iter__ (self):
      return self
    def __next__(self):
      self.ndx += 1
      if self.ndx == len(self.who):
        raise StopIteration
      return ( self.how, self.who[self.ndx] )
  return producer(how, [ "World", "PostgreSQL", "PL/Python" ])
$$ LANGUAGE plpython3u;
```

제너레이터(Generator) - yield 사용:
```sql
CREATE FUNCTION greet (how text)
  RETURNS SETOF greeting
AS $$
  for who in [ "World", "PostgreSQL", "PL/Python" ]:
    yield ( how, who )
$$ LANGUAGE plpython3u;
```

---

## 4. 함수 간 데이터 공유 (Sharing Data)

PL/Python은 PostgreSQL 세션 내에서 함수 호출 간 데이터를 공유하기 위한 두 개의 전역 딕셔너리를 제공합니다:

### 4.1 SD 딕셔너리 (Private Data)

- 범위: 함수별
- 목적: 동일한 함수 에 대한 반복 호출 간 개인 데이터 저장
- 접근: 정의된 함수 내에서만 사용 가능

### 4.2 GD 딕셔너리 (Public Data)

- 범위: 세션 전체
- 목적: 세션 내 모든 Python 함수 가 접근 가능한 데이터 저장
- 주의: 전역 접근성으로 인해 신중하게 사용

### 4.3 비교표

| 특성 | SD | GD |
|-----|----|----|
| 범위 | 단일 함수 | 세션의 모든 함수 |
| 지속성 | 동일 함수 호출 간 | 전체 세션 |
| 가시성 | 개인적 | 공용 |
| 사용 사례 | 한 함수 내 캐싱 | 세션 전체 상태 |

### 4.4 실행 격리

각 PL/Python 함수는 Python 인터프리터에서 자체 실행 환경을 갖습니다:

- 한 함수(예: `myfunc`)의 전역 데이터와 인수는 다른 함수(예: `myfunc2`)에서 사용할 수 없습니다
- 예외: `GD` 딕셔너리에 저장된 데이터는 세션의 모든 함수에서 공유됩니다

---

## 5. 익명 코드 블록 (Anonymous Code Blocks)

PL/Python은 `DO` 문을 사용하여 익명 코드 블록 을 지원합니다. 이들은 함수로 저장되지 않고 실행되는 이름 없는 코드 블록입니다.

### 5.1 구문

```sql
DO $$
    # PL/Python 코드
$$ LANGUAGE plpython3u;
```

### 5.2 주요 특성

- 인수 없음: 익명 코드 블록은 입력 매개변수를 받지 않습니다
- 반환 값 없음: 블록이 반환할 수 있는 모든 값은 무시됩니다
- 동작: 함수처럼 동작하지만 저장되지 않습니다
- 언어: `LANGUAGE plpython3u`를 지정해야 합니다

### 5.3 사용 사례

- 일회성 데이터 조작 작업
- 관리 작업
- 영구 함수를 만들지 않고 PL/Python 코드 테스트
- 재사용이 필요 없는 절차적 로직 실행

---

## 6. 트리거 함수 (Trigger Functions)

PL/Python에서 함수가 트리거로 사용될 때, `TD` (트리거 데이터) 딕셔너리에 트리거 관련 값이 포함되어 트리거 이벤트에 대한 컨텍스트를 제공합니다.

### 6.1 TD 딕셔너리 내용

| 키 | 값 |
|----|-----|
| `TD["event"]` | 이벤트 타입: `INSERT`, `UPDATE`, `DELETE`, 또는 `TRUNCATE` |
| `TD["when"]` | 트리거 타이밍: `BEFORE`, `AFTER`, 또는 `INSTEAD OF` |
| `TD["level"]` | 트리거 레벨: `ROW` 또는 `STATEMENT` |
| `TD["new"]` | 새 행 (행 수준 트리거용) |
| `TD["old"]` | 이전 행 (행 수준 트리거용) |
| `TD["name"]` | 트리거 이름 |
| `TD["table_name"]` | 트리거가 발생한 테이블 이름 |
| `TD["table_schema"]` | 테이블의 스키마 |
| `TD["relid"]` | 테이블의 OID |
| `TD["args"]` | `CREATE TRIGGER` 명령의 인수 (있는 경우) |

### 6.2 반환 값

BEFORE 또는 INSTEAD OF 행 수준 트리거(`TD["when"]`이 `BEFORE` 또는 `INSTEAD OF`이고 `TD["level"]`이 `ROW`인 경우):

| 반환 값 | 동작 |
|--------|------|
| `None` 또는 `"OK"` | 행이 수정되지 않음 |
| `"SKIP"` | 이벤트 중단 |
| `"MODIFY"` | 행이 수정됨 (`INSERT` 또는 `UPDATE` 이벤트만) |

기타 모든 반환 값은 무시됩니다.

### 6.3 트리거 예제

```sql
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    salary INTEGER,
    created_at TIMESTAMP
);

CREATE FUNCTION validate_employee() RETURNS trigger AS $$
    # salary가 0 이하면 삽입/수정 거부
    if TD["new"]["salary"] is not None and TD["new"]["salary"] <= 0:
        return "SKIP"

    # created_at 자동 설정
    if TD["event"] == "INSERT":
        TD["new"]["created_at"] = "now"
        return "MODIFY"

    return "OK"
$$ LANGUAGE plpython3u;

CREATE TRIGGER validate_employee_trigger
    BEFORE INSERT OR UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION validate_employee();
```

### 6.4 감사 로그 트리거 예제

```sql
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    table_name TEXT,
    operation TEXT,
    old_data TEXT,
    new_data TEXT,
    changed_at TIMESTAMP DEFAULT now()
);

CREATE FUNCTION audit_trigger() RETURNS trigger AS $$
    import json

    plan = plpy.prepare(
        "INSERT INTO audit_log (table_name, operation, old_data, new_data) VALUES ($1, $2, $3, $4)",
        ["text", "text", "text", "text"]
    )

    old_data = json.dumps(TD["old"]) if TD["old"] else None
    new_data = json.dumps(TD["new"]) if TD["new"] else None

    plpy.execute(plan, [TD["table_name"], TD["event"], old_data, new_data])

    return "OK"
$$ LANGUAGE plpython3u;

CREATE TRIGGER employees_audit
    AFTER INSERT OR UPDATE OR DELETE ON employees
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();
```

---

## 7. 데이터베이스 접근 (Database Access)

PL/Python 언어 모듈은 데이터베이스 접근 기능을 제공하는 `plpy` 모듈을 자동으로 가져옵니다.

### 7.1 plpy.execute()

쿼리를 실행하고 결과 객체를 반환합니다.

구문: `plpy.execute(query [, limit])`

```sql
CREATE FUNCTION get_users() RETURNS SETOF text AS $$
    rv = plpy.execute("SELECT * FROM users", 5)
    for row in rv:
        yield row["username"]
$$ LANGUAGE plpython3u;
```

결과 객체 메서드:

| 메서드 | 설명 |
|--------|------|
| `nrows()` | 처리된 행 수 |
| `status()` | SPI_execute() 반환 값 |
| `colnames()` | 컬럼 이름 리스트 |
| `coltypes()` | 컬럼 타입 OID 리스트 |
| `coltypmods()` | 타입별 수정자 리스트 |
| `__str__()` | `plpy.debug(rv)`로 디버깅 가능 |

참고: 전체 결과 집합을 메모리에 로드합니다. 대용량 데이터셋에는 `plpy.cursor()`를 사용하세요.

### 7.2 준비된 쿼리 (Prepared Queries)

매개변수화된 쿼리에 재사용 가능한 실행 계획을 준비합니다.

구문:
- `plpy.prepare(query [, argtypes])`
- `plpy.execute(plan [, arguments [, limit]])`

```sql
CREATE FUNCTION find_user(username text) RETURNS text AS $$
    plan = plpy.prepare(
        "SELECT last_name FROM users WHERE first_name = $1",
        ["text"]
    )
    rv = plpy.execute(plan, [username], 5)
    # 또는: rv = plan.execute([username], 5)

    if rv:
        return rv[0]["last_name"]
    return None
$$ LANGUAGE plpython3u;
```

영구 계획 저장 예제:

```sql
CREATE FUNCTION cached_query() RETURNS trigger AS $$
    if "plan" in SD:
        plan = SD["plan"]
    else:
        plan = plpy.prepare("SELECT 1")
        SD["plan"] = plan
    # plan 사용...
$$ LANGUAGE plpython3u;
```

### 7.3 커서 (Cursors)

대용량 결과 집합을 청크 단위로 처리하기 위한 커서를 반환합니다.

구문:
- `plpy.cursor(query)`
- `plpy.cursor(plan [, arguments])`

반복자 방식:

```sql
CREATE FUNCTION count_odd_iterator() RETURNS integer AS $$
    odd = 0
    for row in plpy.cursor("SELECT num FROM largetable"):
        if row['num'] % 2:
            odd += 1
    return odd
$$ LANGUAGE plpython3u;
```

fetch 방식:

```sql
CREATE FUNCTION count_odd_fetch(batch_size integer) RETURNS integer AS $$
    odd = 0
    cursor = plpy.cursor("SELECT num FROM largetable")
    while True:
        rows = cursor.fetch(batch_size)
        if not rows:
            break
        for row in rows:
            if row['num'] % 2:
                odd += 1
    return odd
$$ LANGUAGE plpython3u;
```

커서 메서드:

| 메서드 | 설명 |
|--------|------|
| `fetch(n)` | 최대 n개의 다음 행 배치 반환 |
| `close()` | 커서 리소스 명시적 해제 |

### 7.4 오류 처리

#### 기본 오류 트래핑

```sql
CREATE FUNCTION try_adding_joe() RETURNS text AS $$
    try:
        plpy.execute("INSERT INTO users(username) VALUES ('joe')")
    except plpy.SPIError:
        return "something went wrong"
    else:
        return "Joe added"
$$ LANGUAGE plpython3u;
```

#### 특정 예외 처리

```sql
CREATE FUNCTION insert_fraction(numerator int, denominator int) RETURNS text AS $$
    from plpy import spiexceptions
    try:
        plan = plpy.prepare(
            "INSERT INTO fractions (frac) VALUES ($1 / $2)",
            ["int", "int"]
        )
        plpy.execute(plan, [numerator, denominator])
    except spiexceptions.DivisionByZero:
        return "denominator cannot equal zero"
    except spiexceptions.UniqueViolation:
        return "already have that fraction"
    except plpy.SPIError as e:
        return "other error, SQLSTATE %s" % e.sqlstate
    else:
        return "fraction inserted"
$$ LANGUAGE plpython3u;
```

핵심 포인트:

- `plpy.SPIError`와 그 하위 클래스는 표준 Python 예외처럼 잡을 수 있습니다
- 예외 클래스 이름은 PostgreSQL 조건 이름에서 파생됩니다 (예: `division_by_zero` -> `DivisionByZero`)
- 예외 객체 내에서 `sqlstate` 속성으로 오류 코드 확인
- 모든 `plpy.spiexceptions` 클래스는 `SPIError`를 상속합니다

---

## 8. 명시적 서브트랜잭션 (Explicit Subtransactions)

명시적 서브트랜잭션은 데이터베이스 작업을 원자적으로 처리하여 모든 작업이 성공하거나 모두 롤백되도록 보장합니다.

### 8.1 문제점

서브트랜잭션 없이, 일련의 데이터베이스 작업 중간에 오류가 발생하면 일부 변경사항은 커밋되고 다른 것들은 실패할 수 있습니다:

```sql
CREATE FUNCTION transfer_funds() RETURNS void AS $$
try:
    plpy.execute("UPDATE accounts SET balance = balance - 100 WHERE account_name = 'joe'")
    plpy.execute("UPDATE accounts SET balance = balance + 100 WHERE account_name = 'mary'")
except plpy.SPIError as e:
    result = "error transferring funds: %s" % e.args
else:
    result = "funds transferred correctly"
plan = plpy.prepare("INSERT INTO operations (result) VALUES ($1)", ["text"])
plpy.execute(plan, [result])
$$ LANGUAGE plpython3u;
```

두 번째 UPDATE가 실패하면 Joe의 계정에서는 출금되지만 Mary의 계정에는 입금되지 않아 데이터가 불일치하게 됩니다.

### 8.2 해결책: 서브트랜잭션 컨텍스트 관리자

`plpy.subtransaction()`을 사용하여 데이터베이스 작업을 원자적 블록으로 래핑합니다:

```sql
CREATE FUNCTION transfer_funds_safe() RETURNS void AS $$
try:
    with plpy.subtransaction():
        plpy.execute("UPDATE accounts SET balance = balance - 100 WHERE account_name = 'joe'")
        plpy.execute("UPDATE accounts SET balance = balance + 100 WHERE account_name = 'mary'")
except plpy.SPIError as e:
    result = "error transferring funds: %s" % e.args
else:
    result = "funds transferred correctly"
plan = plpy.prepare("INSERT INTO operations (result) VALUES ($1)", ["text"])
plpy.execute(plan, [result])
$$ LANGUAGE plpython3u;
```

### 8.3 핵심 포인트

- 컨텍스트 관리자 인터페이스: `plpy.subtransaction()`은 Python의 컨텍스트 관리자 프로토콜을 구현합니다
- 오류 처리: 예외를 처리하려면 여전히 `try`/`except`가 필요합니다
- 원자적 동작: 서브트랜잭션 블록 내의 모든 작업은 함께 성공하거나 함께 실패합니다
- 예외 시 롤백: 서브트랜잭션은 모든 예외 종료 시 롤백됩니다 (데이터베이스 오류뿐만 아니라 일반 Python 예외도 포함)

---

## 9. 트랜잭션 제어 (Transaction Control)

프로시저나 익명 코드 블록에서는 `plpy.commit()`과 `plpy.rollback()`을 사용하여 트랜잭션을 제어할 수 있습니다.

### 9.1 commit과 rollback

```sql
CREATE PROCEDURE batch_insert() AS $$
    for i in range(100):
        plpy.execute(f"INSERT INTO test_table (value) VALUES ({i})")
        if i % 10 == 9:
            plpy.commit()  # 10개마다 커밋
$$ LANGUAGE plpython3u;

CALL batch_insert();
```

### 9.2 조건부 트랜잭션 제어

```sql
CREATE PROCEDURE process_data() AS $$
    try:
        plpy.execute("UPDATE accounts SET balance = balance * 1.05")
        # 검증 로직
        result = plpy.execute("SELECT COUNT(*) FROM accounts WHERE balance < 0")
        if result[0]["count"] > 0:
            plpy.rollback()  # 음수 잔액이 있으면 롤백
            plpy.warning("Transaction rolled back: negative balances detected")
        else:
            plpy.commit()
    except plpy.SPIError as e:
        plpy.rollback()
        plpy.error(f"Error: {e}")
$$ LANGUAGE plpython3u;
```

---

## 10. 유틸리티 함수 (Utility Functions)

### 10.1 로깅 함수

`plpy` 모듈은 다음 로깅 함수를 제공합니다:

| 함수 | 설명 |
|------|------|
| `plpy.debug(msg, kwargs)` | 디버그 메시지 |
| `plpy.log(msg, kwargs)` | 로그 메시지 |
| `plpy.info(msg, kwargs)` | 정보 메시지 |
| `plpy.notice(msg, kwargs)` | 알림 메시지 |
| `plpy.warning(msg, kwargs)` | 경고 메시지 |
| `plpy.error(msg, kwargs)` | 오류 (예외 발생) |
| `plpy.fatal(msg, kwargs)` | 치명적 오류 (예외 발생) |

핵심 포인트:

- `plpy.error`와 `plpy.fatal`은 호출 쿼리로 전파되어 트랜잭션을 중단시키는 Python 예외를 발생시킵니다
- 대체 구문: `raise plpy.Error(msg)` 및 `raise plpy.Fatal(msg)` (키워드 인수 미지원)
- 다른 함수는 다양한 우선순위 레벨에서 메시지를 생성합니다
- 메시지 가시성은 `log_min_messages` 및 `client_min_messages` 구성 변수로 제어됩니다

### 10.2 키워드 인수

로깅 함수는 오류 메시지를 풍부하게 하기 위해 다음 키워드 인수를 허용합니다:

- `detail`
- `hint`
- `sqlstate`
- `schema_name`
- `table_name`
- `column_name`
- `datatype_name`
- `constraint_name`

예제:

```sql
CREATE FUNCTION raise_custom_exception() RETURNS void AS $$
plpy.error("custom exception message",
           detail="some info about exception",
           hint="hint for users")
$$ LANGUAGE plpython3u;
```

### 10.3 문자열 인용 함수

안전한 동적 SQL 쿼리를 구성하기 위한 세 가지 유틸리티 함수:

| 함수 | 설명 |
|------|------|
| `plpy.quote_literal(string)` | 문자열 리터럴 인용 |
| `plpy.quote_nullable(string)` | nullable 값 인용 |
| `plpy.quote_ident(string)` | 식별자 인용 |

예제:

```sql
CREATE FUNCTION safe_update(colname text, newvalue text, keyvalue text) RETURNS void AS $$
    plpy.execute("UPDATE tbl SET %s = %s WHERE key = %s" % (
        plpy.quote_ident(colname),
        plpy.quote_nullable(newvalue),
        plpy.quote_literal(keyvalue)))
$$ LANGUAGE plpython3u;
```

이 함수들은 PostgreSQL의 내장 인용 함수와 동등하며, 동적 쿼리를 구축할 때 SQL 인젝션을 방지합니다.

---

## 11. 환경 변수 (Environment Variables)

다음 환경 변수는 PL/Python 동작에 영향을 줄 수 있습니다. 이 변수들은 메인 PostgreSQL 서버 프로세스 환경에서 설정해야 합니다 (예: 시작 스크립트에서):

### 11.1 지원되는 환경 변수

| 변수 | 설명 |
|------|------|
| `PYTHONHOME` | Python 설치 디렉토리 |
| `PYTHONPATH` | 모듈 검색 경로 |
| `PYTHONOPTIMIZE` | 최적화 수준 |
| `PYTHONDEBUG` | 디버그 모드 |
| `PYTHONVERBOSE` | 상세 출력 |
| `PYTHONDONTWRITEBYTECODE` | 바이트코드 생성 방지 |
| `PYTHONIOENCODING` | I/O 인코딩 지정 |
| `PYTHONUSERBASE` | 사이트 패키지용 사용자 기본 디렉토리 |
| `PYTHONHASHSEED` | 해시 무작위화 시드 |

### 11.2 중요 참고사항

1. 서버 프로세스 요구사항: 이 변수들은 개별 클라이언트 연결이 아닌 PostgreSQL 서버 프로세스 자체에 영향을 미칩니다
2. 버전 종속성: 사용 가능한 변수는 Python 버전에 따라 다릅니다; 자세한 내용은 Python 문서를 참조하세요
3. 임베디드 인터프리터 제한: `python` man 페이지에 나열된 일부 Python 환경 변수는 명령줄 인터프리터에서만 효과적이며, PL/Python이 사용하는 임베디드 Python 인터프리터에서는 작동하지 않을 수 있습니다

가장 일반적인 사용 사례로, PYTHONPATH 는 PL/Python의 검색 경로에 사용자 정의 모듈 디렉토리를 추가하는 데 일반적으로 사용됩니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - PL/Python](https://www.postgresql.org/docs/current/plpython.html)
- [Python 프로그래밍 언어](https://www.python.org)
- [PostgreSQL 확장 프로그래밍](https://www.postgresql.org/docs/current/extend.html)
