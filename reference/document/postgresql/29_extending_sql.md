# Chapter 36: SQL 확장하기 (Extending SQL)

PostgreSQL의 가장 강력한 특징 중 하나는 확장성(extensibility)입니다. 이 장에서는 PostgreSQL의 SQL을 확장하는 다양한 방법을 살펴봅니다.

---

## 36.1 확장성의 작동 원리 (How Extensibility Works)

PostgreSQL은 카탈로그 주도(catalog-driven) 방식으로 동작하기 때문에 확장 가능합니다. 이는 일반적인 관계형 데이터베이스와 근본적으로 다른 설계입니다.

### 핵심 원리

1. 카탈로그 주도 아키텍처
   - PostgreSQL은 시스템 카탈로그(메타데이터 테이블)에 광범위한 정보를 저장합니다
   - 데이터베이스, 테이블, 컬럼, 데이터 타입, 함수, 접근 방법 등의 정보 포함
   - 기존 시스템은 이러한 정보를 하드코딩된 프로시저에 저장

2. 사용자 수정 가능한 카탈로그
   - 시스템 카탈로그 테이블은 일반 테이블처럼 보이며 수정 가능
   - PostgreSQL이 이 카탈로그를 기반으로 동작하므로, 수정을 통해 확장 가능

### 확장 방법

| 방법 | 설명 |
|------|------|
| 동적 로딩 | 공유 라이브러리로 새 타입, 함수 구현 |
| SQL 코드 | SQL로 작성된 코드를 서버에 즉시 추가 |

이러한 유연성 덕분에 PostgreSQL은 새로운 애플리케이션의 빠른 프로토타이핑, 새로운 저장 구조 실험, 커스텀 확장 에 매우 적합합니다.

---

## 36.2 PostgreSQL 타입 시스템 (The PostgreSQL Type System)

PostgreSQL 데이터 타입은 크게 네 가지 범주로 나뉩니다.

### 36.2.1 기본 타입 (Base Types)

SQL 언어 수준 아래에서 구현되며, 일반적으로 C로 작성됩니다.

- PostgreSQL은 사용자가 제공한 함수를 통해서만 기본 타입을 조작
- 내장 기본 타입은 Chapter 8에 문서화됨
- 열거형 타입(enum types): SQL 명령으로만 생성 가능한 기본 타입의 하위 카테고리

### 36.2.2 컨테이너 타입 (Container Types)

여러 값을 보유하는 세 종류의 컨테이너 타입이 있습니다.

#### 배열 (Arrays)
```sql
-- 배열은 자동으로 생성됨
CREATE TYPE point AS (x float, y float);
-- point[] 타입이 자동으로 사용 가능
```

- 동일한 타입의 여러 값을 보유
- 각 기본 타입, 복합 타입, 범위 타입, 도메인 타입에 대해 자동 생성
- 배열의 배열은 없음 (다차원 배열은 1차원으로 취급)

#### 복합 타입 (Composite Types / Row Types)
```sql
-- 테이블 생성 시 자동 생성
CREATE TABLE employee (
    name text,
    salary numeric,
    age integer
);

-- 또는 독립적으로 정의
CREATE TYPE address AS (
    street text,
    city text,
    zip_code text
);
```

- 필드 이름과 연관된 타입들의 목록으로 구성
- 값은 필드 값들의 행(row) 또는 레코드(record)

#### 범위 타입 (Range Types)
```sql
-- 내장 범위 타입 예시
SELECT '[2023-01-01, 2023-12-31]'::daterange;
SELECT '[1, 10)'::int4range;
```

- 동일 타입의 두 값(하한과 상한)을 보유
- 일부 내장 범위 타입이 존재하며 사용자 정의도 가능

### 36.2.3 도메인 (Domains)

기본 타입에 제약 조건을 추가하여 유효한 값의 부분집합으로 제한합니다.

```sql
CREATE DOMAIN positive_integer AS integer
    CHECK (VALUE > 0);

CREATE DOMAIN email AS text
    CHECK (VALUE ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
```

- 일반적으로 기본 타입과 상호 교환 가능
- `CREATE DOMAIN` SQL 명령으로 생성

### 36.2.4 의사 타입 (Pseudo-Types)

특수 목적의 타입으로 제한 사항이 있습니다.

| 특징 | 설명 |
|------|------|
| 테이블 컬럼 불가 | 컨테이너 타입의 구성 요소로 사용 불가 |
| 함수 선언 가능 | 인수 및 반환 타입으로 사용 가능 |
| 특수 함수 식별 | 특별한 함수 클래스 식별에 사용 |

주요 의사 타입: `void`, `trigger`, `event_trigger`, `internal`, `record`, `cstring` 등

### 36.2.5 다형성 타입 (Polymorphic Types)

단일 함수 정의가 여러 다른 데이터 타입에서 작동할 수 있게 합니다.

#### 단순 계열 (Simple Family)

| 타입 | 설명 |
|------|------|
| `anyelement` | 모든 데이터 타입 허용 |
| `anyarray` | 모든 배열 타입 허용 |
| `anynonarray` | 배열이 아닌 모든 타입 허용 |
| `anyenum` | 모든 열거형 타입 허용 |
| `anyrange` | 모든 범위 타입 허용 |
| `anymultirange` | 모든 다중 범위 타입 허용 |

예제:
```sql
CREATE FUNCTION equal(anyelement, anyelement) RETURNS boolean AS $$
    SELECT $1 = $2;
$$ LANGUAGE SQL;

-- 다양한 타입으로 호출 가능
SELECT equal(1, 1);           -- true
SELECT equal('abc', 'abc');   -- true
SELECT equal(1.5, 1.5);       -- true
```

규칙:
- 각 `anyelement` 위치는 모든 사용에서 동일한 실제 타입이어야 함
- `anyarray`와 `anyelement`가 함께 선언되면, 배열의 요소 타입은 `anyelement` 타입과 일치해야 함

#### 호환 계열 (Common Family)

| 타입 | 설명 |
|------|------|
| `anycompatible` | 자동 승격으로 공통 타입 사용 |
| `anycompatiblearray` | 자동 승격으로 공통 배열 타입 사용 |
| `anycompatiblenonarray` | 자동 승격으로 공통 비배열 타입 사용 |
| `anycompatiblerange` | 자동 승격으로 공통 범위 타입 사용 |

예제:
```sql
CREATE FUNCTION make_array2(anycompatible, anycompatible)
RETURNS anycompatiblearray AS $$
    SELECT ARRAY[$1, $2];
$$ LANGUAGE SQL;

-- integer와 numeric을 함께 사용 가능 (자동 승격)
SELECT make_array2(1, 2.5);  -- {1,2.5} (numeric 배열)
```

규칙:
- 인수가 동일할 필요 없이 공통 타입으로 암시적 캐스팅 가능해야 함
- 공통 타입 선택은 `UNION` 규칙을 따름

#### 독립적인 타입 변수

단순 계열과 호환 계열은 독립적입니다:

```sql
CREATE FUNCTION myfunc(
    a anyelement, b anyelement,
    c anycompatible, d anycompatible
) RETURNS anycompatible AS $$
    -- a, b는 정확히 동일한 타입이어야 함
    -- c, d는 공통 타입으로 승격 가능해야 함 (a, b와 무관)
    -- 반환 타입은 c, d의 공통 타입
    SELECT c;
$$ LANGUAGE SQL;
```

---

## 36.3 사용자 정의 함수 (User-Defined Functions)

PostgreSQL은 네 가지 종류의 사용자 정의 함수를 제공합니다.

| 종류 | 설명 |
|------|------|
| SQL 함수 | SQL로 작성된 함수 |
| 절차적 언어 함수 | PL/pgSQL, PL/Python 등으로 작성 |
| 내부 함수 | C로 작성되어 서버에 정적 링크 |
| C 언어 함수 | 동적 로드되는 공유 라이브러리 |

### 함수 기능

매개변수 타입:
- 기본 타입, 복합 타입, 또는 조합 허용

반환 타입:
- 기본 타입 또는 복합 타입 반환 가능
- 기본 또는 복합 값의 집합(sets) 반환 가능

고급 타입:
- 의사 타입(다형성 타입 포함) 허용
- 사용 가능한 기능은 함수 종류에 따라 다름

---

## 36.5 SQL 함수 (Query Language Functions)

SQL 함수는 임의의 SQL 문 목록을 실행하고 마지막 쿼리의 결과를 반환합니다.

### 36.5.1 기본 SQL 함수

#### 단순 함수
```sql
CREATE FUNCTION one() RETURNS integer AS $$
    SELECT 1 AS result;
$$ LANGUAGE SQL;

SELECT one();
-- 결과: 1
```

#### 인수가 있는 함수
```sql
CREATE FUNCTION add_em(x integer, y integer) RETURNS integer AS $$
    SELECT x + y;
$$ LANGUAGE SQL;

SELECT add_em(1, 2) AS answer;
-- 결과: 3
```

숫자 표기법 사용:
```sql
CREATE FUNCTION add_em(integer, integer) RETURNS integer AS $$
    SELECT $1 + $2;
$$ LANGUAGE SQL;
```

### 36.5.2 복합 타입에 대한 함수

```sql
CREATE TABLE emp (
    name        text,
    salary      numeric,
    age         integer,
    cubicle     point
);

INSERT INTO emp VALUES ('Bill', 4200, 45, '(2,1)');

-- 복합 타입을 인수로 받는 함수
CREATE FUNCTION double_salary(emp) RETURNS numeric AS $$
    SELECT $1.salary * 2 AS salary;
$$ LANGUAGE SQL;

SELECT name, double_salary(emp.*) AS dream
FROM emp
WHERE emp.cubicle ~= point '(2,1)';
```

#### 복합 타입 반환
```sql
CREATE FUNCTION new_emp() RETURNS emp AS $$
    SELECT text 'None' AS name,
           1000.0 AS salary,
           25 AS age,
           point '(2,2)' AS cubicle;
$$ LANGUAGE SQL;

-- 값 표현식으로 호출
SELECT new_emp();
-- 결과: (None,1000.0,25,"(2,2)")

-- 테이블 함수로 호출
SELECT * FROM new_emp();
-- name | salary | age | cubicle
-- None | 1000.0 |  25 | (2,2)
```

### 36.5.3 출력 매개변수 (Output Parameters)

```sql
-- 단일 출력 매개변수
CREATE FUNCTION add_em(IN x int, IN y int, OUT sum int)
AS 'SELECT x + y'
LANGUAGE SQL;

SELECT add_em(3, 7);
-- 결과: 10
```

#### 다중 출력 매개변수
```sql
CREATE FUNCTION sum_n_product(x int, y int, OUT sum int, OUT product int)
AS 'SELECT x + y, x * y'
LANGUAGE SQL;

SELECT * FROM sum_n_product(11, 42);
-- sum | product
--  53 |     462
```

매개변수 모드:
- `IN` (기본값): 입력 매개변수
- `OUT`: 출력 매개변수
- `INOUT`: 입력 및 출력 모두
- `VARIADIC`: 가변 인수

### 36.5.4 가변 인수 함수 (VARIADIC)

```sql
CREATE FUNCTION mleast(VARIADIC arr numeric[]) RETURNS numeric AS $$
    SELECT min($1[i]) FROM generate_subscripts($1, 1) g(i);
$$ LANGUAGE SQL;

SELECT mleast(10, -1, 5, 4.4);
-- 결과: -1
```

배열을 VARIADIC 함수에 전달:
```sql
-- VARIADIC 키워드 사용
SELECT mleast(VARIADIC ARRAY[10, -1, 5, 4.4]);

-- 빈 배열 전달
SELECT mleast(VARIADIC ARRAY[]::numeric[]);
```

### 36.5.5 기본 매개변수 값

```sql
CREATE FUNCTION foo(a int, b int DEFAULT 2, c int DEFAULT 3)
RETURNS int
LANGUAGE SQL
AS $$
    SELECT $1 + $2 + $3;
$$;

SELECT foo(10, 20, 30);  -- 60
SELECT foo(10, 20);       -- 33
SELECT foo(10);           -- 15
SELECT foo();             -- 오류: 첫 번째 인수에 기본값 없음
```

### 36.5.6 집합 반환 함수 (Set-Returning Functions)

```sql
CREATE TABLE foo (fooid int, foosubid int, fooname text);
INSERT INTO foo VALUES (1, 1, 'Joe'), (1, 2, 'Ed'), (2, 1, 'Mary');

CREATE FUNCTION getfoo(int) RETURNS SETOF foo AS $$
    SELECT * FROM foo WHERE fooid = $1;
$$ LANGUAGE SQL;

SELECT * FROM getfoo(1) AS t1;
-- fooid | foosubid | fooname
--     1 |        1 | Joe
--     1 |        2 | Ed
```

#### RETURNS TABLE 구문
```sql
CREATE FUNCTION sum_n_product_with_tab(x int)
RETURNS TABLE(sum int, product int) AS $$
    SELECT $1 + tab.y, $1 * tab.y FROM tab;
$$ LANGUAGE SQL;
```

### 36.5.7 다형성 SQL 함수

```sql
-- 기본 다형성
CREATE FUNCTION make_array(anyelement, anyelement) RETURNS anyarray AS $$
    SELECT ARRAY[$1, $2];
$$ LANGUAGE SQL;

SELECT make_array(1, 2) AS intarray;        -- {1,2}
SELECT make_array('a'::text, 'b') AS textarray;  -- {a,b}
```

호환 다형성:
```sql
CREATE FUNCTION make_array2(anycompatible, anycompatible)
RETURNS anycompatiblearray AS $$
    SELECT ARRAY[$1, $2];
$$ LANGUAGE SQL;

SELECT make_array2(1, 2.5) AS numericarray;  -- {1,2.5}
```

다형성 VARIADIC 함수:
```sql
CREATE FUNCTION anyleast(VARIADIC anyarray) RETURNS anyelement AS $$
    SELECT min($1[i]) FROM generate_subscripts($1, 1) g(i);
$$ LANGUAGE SQL;

SELECT anyleast(10, -1, 5, 4);  -- -1
SELECT anyleast('abc'::text, 'ABC');  -- 데이터베이스 콜레이션에 따라 다름
```

---

## 36.6 함수 오버로딩 (Function Overloading)

동일한 SQL 이름으로 여러 함수를 정의할 수 있습니다(인수가 다른 경우).

### 기본 개념

```sql
-- 오버로딩 예제
CREATE FUNCTION add_num(integer, integer) RETURNS integer AS $$
    SELECT $1 + $2;
$$ LANGUAGE SQL;

CREATE FUNCTION add_num(numeric, numeric) RETURNS numeric AS $$
    SELECT $1 + $2;
$$ LANGUAGE SQL;

-- PostgreSQL이 인수 타입에 따라 적절한 함수 선택
SELECT add_num(1, 2);      -- integer 버전 호출
SELECT add_num(1.5, 2.5);  -- numeric 버전 호출
```

### 모호성 주의사항

오버로딩된 함수 생성 시 모호성을 피해야 합니다:

```sql
-- 잠재적 모호성
CREATE FUNCTION test(int, real) RETURNS ...;
CREATE FUNCTION test(smallint, double precision) RETURNS ...;

-- test(1, 1.5) 호출 시 어떤 함수가 호출될지 불명확
```

### 피해야 할 이름 충돌

1. 복합 타입 속성과의 충돌
   - 복합 타입의 단일 인수를 받는 함수는 해당 타입의 속성과 동일한 이름을 피해야 함
   - `attribute(table)`은 `table.attribute`와 동등하게 취급

2. VARIADIC vs 비-VARIADIC 함수
   - `foo(numeric)`과 `foo(VARIADIC numeric[])`를 모두 생성하면 모호성 발생
   - 동일 스키마에서는 비-VARIADIC 함수가 우선

### C 언어 함수 제약

C 언어 함수 오버로딩 시 각 함수의 C 이름이 모두 달라야 합니다:

```sql
-- AS 절로 SQL 이름과 C 이름 분리
CREATE FUNCTION test(int) RETURNS int
    AS 'filename', 'test_1arg'
    LANGUAGE C;

CREATE FUNCTION test(int, int) RETURNS int
    AS 'filename', 'test_2arg'
    LANGUAGE C;
```

---

## 36.7 함수 휘발성 범주 (Function Volatility Categories)

모든 PostgreSQL 함수는 최적화기에게 함수의 동작을 알려주는 휘발성 분류 를 가집니다.

### 세 가지 범주

| 범주 | 설명 |
|------|------|
| VOLATILE | 기본값. 모든 작업 가능, 동일 인수에도 다른 결과 반환 가능 |
| STABLE | DB 수정 불가. 단일 문 내에서 동일 인수에 동일 결과 보장 |
| IMMUTABLE | DB 수정 불가. 동일 인수에 영원히 동일 결과 보장 |

### 상세 설명

#### VOLATILE (휘발성)
```sql
-- 휘발성 함수 예제
CREATE FUNCTION get_random() RETURNS float8 AS $$
    SELECT random();
$$ LANGUAGE SQL VOLATILE;
```

- 데이터베이스 수정 가능
- 연속 호출에서 다른 결과 반환 가능
- 최적화기가 동작에 대해 가정하지 않음
- 값이 필요한 각 행에서 재평가
- 예: `random()`, `currval()`, `timeofday()`

#### STABLE (안정적)
```sql
-- 안정적 함수 예제
CREATE FUNCTION get_config_value(key text) RETURNS text AS $$
    SELECT value FROM config WHERE config_key = key;
$$ LANGUAGE SQL STABLE;
```

- 데이터베이스 수정 불가
- 단일 문 내에서 동일 결과 보장
- 여러 함수 호출을 단일 호출로 최적화 가능
- 인덱스 스캔 조건에서 안전하게 사용
- 예: `current_timestamp` 함수 계열

#### IMMUTABLE (불변)
```sql
-- 불변 함수 예제
CREATE FUNCTION calculate_area(radius float8) RETURNS float8 AS $$
    SELECT 3.14159 * radius * radius;
$$ LANGUAGE SQL IMMUTABLE;
```

- 데이터베이스 수정 불가
- 동일 인수에 영원히 동일 결과 보장
- 상수 인수로 호출 시 사전 평가 가능
- 예: `SELECT ... WHERE x = 2 + 2`는 `SELECT ... WHERE x = 4`로 단순화

### 비교 표

| 특성 | VOLATILE | STABLE | IMMUTABLE |
|------|----------|--------|-----------|
| 부작용 | 허용 | 불허 | 불허 |
| 다중 호출 최적화 | 불가 | 가능 | 가능 |
| 상수 폴딩 | 불가 | 불가 | 가능 |
| 인덱스 조건 | 무효 | 유효 | 유효 |

### 중요 규칙

1. 최적화를 위해 가장 엄격한 유효 범주 사용

2. VOLATILE이어야 하는 경우:
   - 부작용이 있는 경우
   - 단일 쿼리 내에서 값이 변할 수 있는 경우
   - 설정 매개변수(예: `TimeZone`)에 의존하는 경우

3. STABLE/IMMUTABLE 제약:
   - `SELECT` 명령만 포함해야 함 (INSERT, UPDATE, DELETE 불가)
   - IMMUTABLE 함수에서 데이터베이스 테이블 선택은 권장하지 않음

4. 계획 시 주의사항:
   - IMMUTABLE이 아닌 함수를 IMMUTABLE로 잘못 표시하면 준비된 문에서 오래된 값이 캐시될 수 있음

---

## 36.12 사용자 정의 집계 함수 (User-Defined Aggregates)

PostgreSQL의 사용자 정의 집계는 상태 값 과 상태 전환 함수 를 사용하여 정의됩니다.

### 기본 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 상태 데이터 타입 | 상태 값의 내부 데이터 타입 |
| 초기 상태 값 | 상태의 시작 값 |
| 상태 전환 함수 | 각 행마다 상태 업데이트 |
| 최종 함수 (선택) | 최종 상태를 결과로 변환 |

### 기본 구문

```sql
CREATE AGGREGATE aggregate_name (input_type)
(
    sfunc = transition_function,
    stype = state_type,
    initcond = 'initial_value',
    finalfunc = final_function  -- 선택사항
);
```

### 예제: SUM 집계

```sql
-- complex 타입의 합계 집계
CREATE AGGREGATE sum (complex)
(
    sfunc = complex_add,
    stype = complex,
    initcond = '(0,0)'
);

-- 사용
SELECT sum(a) FROM test_complex;
```

### 예제: 평균 집계

```sql
CREATE AGGREGATE avg (float8)
(
    sfunc = float8_accum,
    stype = float8[],
    finalfunc = float8_avg,
    initcond = '{0,0,0}'
);
```

### NULL 처리

```sql
-- strict 전환 함수: null 입력 자동 건너뛰기
CREATE AGGREGATE sum (complex)
(
    sfunc = complex_add,  -- STRICT으로 선언
    stype = complex
    -- initcond 없음 = null로 시작
);
```

- Strict 전환 함수: null 입력을 자동으로 건너뛰고 이전 상태 유지
- initcond 생략: 초기 상태가 null이 되어 입력이 없을 때 null 결과 가능

### 36.12.1 이동 집계 모드 (Moving-Aggregate Mode)

윈도우 함수에서 이동 프레임 시작점을 효율적으로 처리하기 위한 역 전환 함수 사용.

```sql
CREATE AGGREGATE sum (complex)
(
    sfunc = complex_add,
    stype = complex,
    initcond = '(0,0)',
    msfunc = complex_add,      -- 이동 순방향 함수
    minvfunc = complex_sub,    -- 역함수
    mstype = complex,          -- 이동 상태 타입
    minitcond = '(0,0)'        -- 이동 초기 조건
);
```

주의사항:
- 부동소수점 집계는 정밀도 제한으로 인해 역함수 사용이 안전하지 않음
- 역함수가 null을 반환하면 처음부터 다시 계산

### 36.12.2 다형성 및 VARIADIC 집계

```sql
-- 다형성 집계
CREATE AGGREGATE array_accum (anycompatible)
(
    sfunc = array_append,
    stype = anycompatiblearray,
    initcond = '{}'
);

-- 다양한 타입에서 작동
SELECT array_accum(attname) FROM pg_attribute WHERE attnum > 0;
```

### 36.12.3 정렬 집합 집계 (Ordered-Set Aggregates)

명시적 정렬 순서와 직접 인수를 가진 집계.

```sql
-- 백분위수 집계 사용
SELECT percentile_disc(0.5) WITHIN GROUP (ORDER BY income)
FROM households;
```

특징:
- 직접 인수: 한 번만 평가 (예: 0.5 백분위수)
- 집계 인수: 정렬 순서와 함께 행마다 평가
- 윈도우 함수로 사용 불가

### 36.12.4 부분 집계 (Partial Aggregation)

병렬 집계를 위해 다른 데이터 하위 집합의 부분 상태 값을 결합.

```sql
CREATE AGGREGATE max (anyelement)
(
    sfunc = greatest,
    stype = anyelement,
    combinefunc = greatest    -- 병렬 실행에 필요
);
```

요구사항:
- combine 함수는 교환 가능해야 함 (입력 순서 미지정)
- 집계는 `PARALLEL SAFE`로 표시해야 함

---

## 36.13 사용자 정의 타입 (User-Defined Types)

PostgreSQL은 SQL 언어 수준 아래에서 정의되는 새로운 기본 타입의 생성을 허용합니다.

### 핵심 요구사항

모든 사용자 정의 타입은 다음이 필요합니다:

| 함수 | 설명 |
|------|------|
| 입력 함수 | null 종료 C 문자열을 받아 내부 표현 반환 |
| 출력 함수 | 내부 표현을 받아 null 종료 C 문자열 반환 |
| 이진 입력 함수 (선택) | 더 빠르고 덜 이식 가능한 이진 연산용 `recv` |
| 이진 출력 함수 (선택) | 이진 연산용 `send` |

### 예제: 복소수 타입

#### C 구조체 정의
```c
typedef struct Complex {
    double      x;
    double      y;
} Complex;
```

#### 입력 함수
```c
PG_FUNCTION_INFO_V1(complex_in);

Datum
complex_in(PG_FUNCTION_ARGS)
{
    char       *str = PG_GETARG_CSTRING(0);
    double      x, y;
    Complex    *result;

    if (sscanf(str, " ( %lf , %lf )", &x, &y) != 2)
        ereport(ERROR,
                (errcode(ERRCODE_INVALID_TEXT_REPRESENTATION),
                 errmsg("invalid input syntax for type %s: \"%s\"",
                        "complex", str)));

    result = (Complex *) palloc(sizeof(Complex));
    result->x = x;
    result->y = y;
    PG_RETURN_POINTER(result);
}
```

#### 출력 함수
```c
PG_FUNCTION_INFO_V1(complex_out);

Datum
complex_out(PG_FUNCTION_ARGS)
{
    Complex    *complex = (Complex *) PG_GETARG_POINTER(0);
    char       *result;

    result = psprintf("(%g,%g)", complex->x, complex->y);
    PG_RETURN_CSTRING(result);
}
```

#### SQL 정의

```sql
-- 1. 셸 타입 선언
CREATE TYPE complex;

-- 2. I/O 함수 등록
CREATE FUNCTION complex_in(cstring)
    RETURNS complex
    AS 'filename'
    LANGUAGE C IMMUTABLE STRICT;

CREATE FUNCTION complex_out(complex)
    RETURNS cstring
    AS 'filename'
    LANGUAGE C IMMUTABLE STRICT;

-- 3. 타입 정의 생성
CREATE TYPE complex (
   internallength = 16,
   input = complex_in,
   output = complex_out,
   alignment = double
);
```

### 설계 고려사항

1. 입출력 함수는 역함수 관계: 이 관계가 깨지면 데이터 덤프/복원 시 심각한 문제 발생

2. 가변 길이 타입:
   - 처음 4바이트는 `char[4]` 필드 (관례적으로 `vl_len_`)
   - `SET_VARSIZE()` 매크로로 총 크기 저장
   - `VARSIZE()` 매크로로 크기 조회

### 정의 후 가능한 작업

1. 자동 배열 지원: 배열은 타입 이름 앞에 `_`가 붙음 (예: `complex[]`)
2. 추가 함수 정의: 데이터 타입에 대한 연산
3. 연산자 정의: 함수 위에 구축
4. 연산자 클래스 생성: 타입의 인덱싱 지원

---

## 36.14 사용자 정의 연산자 (User-Defined Operators)

사용자 정의 연산자는 쿼리 플래너가 쿼리를 최적화하는 데 도움이 되는 추가 정보를 담은 함수 호출의 "구문적 설탕(syntactic sugar)"입니다.

### 핵심 요구사항

1. 먼저 기본 함수 생성 후 연산자 생성
2. 함수가 실제 작업 수행
3. 시스템이 피연산자 수와 타입을 기반으로 호출할 연산자 결정

### 구문

```sql
CREATE OPERATOR operator_name (
    leftarg = type1,           -- 선택 (전위 연산자는 생략)
    rightarg = type2,          -- 필수
    function = function_name,  -- 필수
    commutator = operator_name -- 선택적 최적화 힌트
);
```

### 완전한 예제

```sql
-- 1단계: 함수 생성
CREATE FUNCTION complex_add(complex, complex)
    RETURNS complex
    AS 'filename', 'complex_add'
    LANGUAGE C IMMUTABLE STRICT;

-- 2단계: 연산자 생성
CREATE OPERATOR + (
    leftarg = complex,
    rightarg = complex,
    function = complex_add,
    commutator = +
);

-- 3단계: 연산자 사용
SELECT (a + b) AS c FROM test_complex;
```

### 연산자 유형

| 유형 | 설명 |
|------|------|
| 이항 (중위) | `leftarg`와 `rightarg` 모두 있음 |
| 전위 (단항) | `leftarg` 매개변수 생략 |

---

## 36.15 연산자 최적화 정보 (Operator Optimization Information)

PostgreSQL 연산자 정의에는 쿼리 실행을 최적화하는 선택적 절이 포함될 수 있습니다.

### 36.15.1 COMMUTATOR (교환자)

정의되는 연산자의 교환자인 연산자를 식별합니다.

정의: 모든 가능한 입력 값에 대해 `(x A y)`가 `(y B x)`와 같으면 연산자 A는 연산자 B의 교환자입니다.

```sql
CREATE OPERATOR < (
    leftarg = complex,
    rightarg = complex,
    function = complex_lt,
    commutator = >  -- < 와 > 는 교환자
);
```

사용 사례: 인덱스 및 조인 절에 중요 - 쿼리 최적화기가 다른 계획 유형을 위해 절을 "뒤집을" 수 있음

### 36.15.2 NEGATOR (부정자)

정의되는 연산자의 부정자인 연산자를 식별합니다.

정의: 모든 가능한 입력에 대해 `(x A y)`가 `NOT (x B y)`와 같으면 연산자 A는 연산자 B의 부정자입니다.

```sql
CREATE OPERATOR >= (
    ...
    negator = <  -- >= 와 < 는 부정자
);
```

사용 사례: 최적화기가 `NOT (x = y)`를 `x <> y`로 단순화하는 데 도움

### 36.15.3 RESTRICT (제한)

연산자에 대한 제한 선택도 추정 함수를 지정합니다.

적용 대상: `boolean`을 반환하는 이항 연산자

기능: `column OP constant` 조건을 만족하는 테이블 행의 비율 추정

표준 제한 추정자:
| 추정자 | 용도 |
|--------|------|
| `eqsel` | `=` |
| `neqsel` | `<>` |
| `scalarltsel` | `<` |
| `scalarlesel` | `<=` |
| `scalargtsel` | `>` |
| `scalargesel` | `>=` |

### 36.15.4 JOIN (조인)

연산자에 대한 조인 선택도 추정 함수를 지정합니다.

적용 대상: `boolean`을 반환하는 이항 연산자

기능: `table1.column1 OP table2.column2` 조건을 만족하는 행 쌍의 비율 추정

표준 조인 선택도 추정자:
| 추정자 | 용도 |
|--------|------|
| `eqjoinsel` | `=` |
| `neqjoinsel` | `<>` |
| `scalarltjoinsel` | `<` |
| `scalargejoinsel` | `>=` |

### 36.15.5 HASHES

이 연산자를 기반으로 한 조인에 해시 조인 방법을 사용할 수 있음을 시스템에 알립니다.

적용 대상: 동등성을 나타내는 `boolean`을 반환하는 이항 연산자

핵심 요구사항:
- 같은 코드로 해시되는 쌍에 대해서만 조인 연산자가 true 반환
- 해시 인덱스 연산자 패밀리 에 연산자가 나타나야 함
- 동일 연산자 패밀리에 교환자 가 있어야 함
- 기본 함수가 immutable 또는 stable 로 표시 (volatile 불가)

### 36.15.6 MERGES

이 연산자를 기반으로 한 조인에 병합 조인 방법을 사용할 수 있음을 시스템에 알립니다.

적용 대상: 동등성을 나타내는 `boolean`을 반환하는 이항 연산자

핵심 요구사항:
- 두 데이터 타입 모두 완전히 정렬 가능 해야 함
- 조인 연산자가 동등성처럼 동작
- btree 인덱스 연산자 패밀리 의 동등성 멤버로 나타나야 함

---

## 36.16 인덱스를 위한 연산자 클래스와 연산자 패밀리 (Operator Classes and Operator Families for Indexes)

연산자 클래스는 인덱스 메서드가 특정 데이터 타입과 작동하는 데 필요한 연산 집합을 정의합니다.

### 핵심 개념

#### 연산자 클래스
- 인덱스 접근 방법(B-Tree, GIN, GiST, SP-GiST, BRIN, Hash)과 연관
- 특정 데이터 타입 및 의미 해석에 대한 연산 식별
- 동일 데이터 타입과 인덱스 방법에 여러 연산자 클래스 존재 가능
- 일반적으로 하나의 클래스가 기본값으로 표시

#### 연산자 패밀리
- 하나 이상의 연산자 클래스 포함
- 패밀리 전체에 대한 인덱싱 가능 연산자 및 지원 함수 포함
- 교차 데이터 타입 연산 가능
- 모든 연산자와 함수가 호환 가능한 의미를 가져야 함

### 인덱스 메서드 전략

전략은 전략 번호로 식별되는 일반화된 연산자입니다.

#### B-Tree 전략
| 연산 | 전략 번호 |
|------|-----------|
| 미만 | 1 |
| 이하 | 2 |
| 같음 | 3 |
| 이상 | 4 |
| 초과 | 5 |

#### Hash 전략
| 연산 | 전략 번호 |
|------|-----------|
| 같음 | 1 |

### 지원 함수

#### B-Tree 지원 함수
| 함수 | 지원 번호 |
|------|-----------|
| 두 키 비교 및 정수 반환 | 1 |
| C 호출 가능 정렬 지원 함수 주소 반환 | 2 |
| 테스트 값을 기본값 더하기/빼기 오프셋과 비교 | 3 |
| btree 중복 제거 안전 여부 결정 | 4 |
| 연산자 클래스별 옵션 정의 | 5 |
| C 호출 가능 건너뛰기 지원 함수 주소 반환 | 6 |

### 예제: 복소수 연산자 클래스

```sql
-- 1단계: 비교 함수 생성
CREATE FUNCTION complex_abs_cmp(complex, complex)
    RETURNS integer
    AS 'filename'
    LANGUAGE C IMMUTABLE STRICT;

-- 2단계: 연산자 함수 생성
CREATE FUNCTION complex_abs_lt(complex, complex) RETURNS bool
    AS 'filename', 'complex_abs_lt'
    LANGUAGE C IMMUTABLE STRICT;

-- 3단계: 연산자 선언
CREATE OPERATOR < (
   leftarg = complex, rightarg = complex,
   procedure = complex_abs_lt,
   commutator = >, negator = >=,
   restrict = scalarltsel, join = scalarltjoinsel
);

-- <=, =, >=, > 연산자에 대해 반복

-- 4단계: 연산자 클래스 생성
CREATE OPERATOR CLASS complex_abs_ops
    DEFAULT FOR TYPE complex USING btree AS
        OPERATOR 1 <,
        OPERATOR 2 <=,
        OPERATOR 3 =,
        OPERATOR 4 >=,
        OPERATOR 5 >,
        FUNCTION 1 complex_abs_cmp(complex, complex);
```

### 연산자 패밀리 예제

```sql
-- 정수 연산자 패밀리
CREATE OPERATOR FAMILY integer_ops USING btree;

CREATE OPERATOR CLASS int8_ops
DEFAULT FOR TYPE int8 USING btree FAMILY integer_ops AS
  OPERATOR 1 <,
  OPERATOR 2 <=,
  OPERATOR 3 =,
  OPERATOR 4 >=,
  OPERATOR 5 >,
  FUNCTION 1 btint8cmp(int8, int8),
  FUNCTION 2 btint8sortsupport(internal);

-- 교차 타입 연산자 추가
ALTER OPERATOR FAMILY integer_ops USING btree ADD
  OPERATOR 1 < (int8, int2),
  OPERATOR 2 <= (int8, int2),
  OPERATOR 3 = (int8, int2),
  FUNCTION 1 btint82cmp(int8, int2);
```

---

## 36.17 확장 패키징 (Extension Packaging)

PostgreSQL 확장을 사용하면 관련 SQL 객체를 단일 설치 가능한 단위로 패키징할 수 있습니다.

### 주요 장점

- 단일 명령 설치/제거: `CREATE EXTENSION` 및 `DROP EXTENSION` 사용
- 덤프 단순화: `pg_dump`가 개별 객체가 아닌 `CREATE EXTENSION` 명령만 포함
- 쉬운 마이그레이션: 새 확장 버전으로 업데이트 간소화
- 종속성 추적: PostgreSQL이 객체 관계 이해

### 필수 파일

#### 1. 제어 파일 (`extension_name.control`)

`SHAREDIR/extension` 디렉토리에 위치. `postgresql.conf`와 유사한 형식.

필수 매개변수:

| 매개변수 | 타입 | 설명 |
|----------|------|------|
| `default_version` | string | `CREATE EXTENSION`으로 설치되는 기본 버전 |
| `relocatable` | boolean | 다른 스키마로 이동 가능 여부 (기본: false) |
| `schema` | string | 재배치 불가능한 확장에 필요한 스키마 |

선택적 매개변수:

```ini
# 기본 메타데이터
comment = '확장 설명'
encoding = 'UTF8'

# 모듈 구성
module_pathname = '$libdir/extension_name'

# 종속성
requires = 'foo, bar'

# 보안
superuser = true    # 슈퍼유저만 설치 가능 (기본: true)
trusted = false     # CREATE 권한이 있는 비슈퍼유저 허용
```

#### 2. SQL 스크립트 파일

명명 패턴: `extension_name--version.sql` (예: `foo--1.0.sql`)

제약사항:
- 트랜잭션 제어 명령 불가 (`BEGIN`, `COMMIT`)
- 트랜잭션 블록에서 실행할 수 없는 명령 불가 (`VACUUM`)
- 트랜잭션 내에서 암시적으로 실행

예제 스크립트 (`pair--1.0.sql`):

```sql
-- 직접 psql 실행 방지
\echo Use "CREATE EXTENSION pair" to load this file. \quit

CREATE TYPE pair AS (k text, v text);

CREATE FUNCTION pair(text, text)
RETURNS pair LANGUAGE SQL AS 'SELECT ROW($1, $2)::@extschema@.pair;';

CREATE OPERATOR ~> (LEFTARG = text, RIGHTARG = text, FUNCTION = pair);
```

#### 3. 제어 파일 예제

```ini
# pair 확장
comment = '키/값 쌍 데이터 타입'
default_version = '1.0'
relocatable = false
```

### 스키마 재배치

세 가지 수준의 재배치 지원:

#### 1. 완전히 재배치 가능
```ini
relocatable = true
```
- `ALTER EXTENSION SET SCHEMA`로 언제든지 확장을 다른 스키마로 이동 가능
- 모든 객체가 처음에 하나의 스키마에 있어야 함

#### 2. 설치 중에만 재배치 가능
```ini
relocatable = false
```
- 스크립트 파일에서 `@extschema@` 플레이스홀더 사용
- 실행 전 실제 대상 스키마 이름으로 대체
- 사용자가 `CREATE EXTENSION`의 `SCHEMA` 옵션으로 스키마 지정

```sql
CREATE FUNCTION my_func() RETURNS text AS
'SELECT $1::@extschema@.my_type'
LANGUAGE SQL;
```

#### 3. 재배치 불가능
```ini
relocatable = false
schema = 'myschema'
```
- 지정된 스키마에 고정
- 제어 파일과 일치하지 않으면 `SCHEMA` 옵션 불허

### 확장 업데이트

#### 업데이트 스크립트

명명 패턴: `extension_name--old_version--target_version.sql`

예: `foo--1.0--1.1.sql`

```sql
-- 1.0에서 1.1로 업그레이드하는 변경 사항
ALTER FUNCTION my_func() ...;
CREATE FUNCTION new_func() ...;
```

#### 업데이트 명령

```sql
ALTER EXTENSION foo UPDATE TO '1.1';
```

PostgreSQL은 필요한 경우 업데이트 스크립트를 자동으로 체인 연결합니다 (예: 1.0 → 1.1 → 2.0).

### 확장 설치

```sql
-- 기본 설치
CREATE EXTENSION extension_name;

-- 특정 버전
CREATE EXTENSION extension_name VERSION '1.0';

-- 특정 스키마
CREATE EXTENSION extension_name SCHEMA myschema;
```

### 특수 변수

스크립트 파일에서:
- `@extschema@` → 대상 스키마 이름
- `@extschema:name@` → 참조된 확장의 스키마
- `@extowner@` → `CREATE EXTENSION`을 호출하는 사용자 이름
- `MODULE_PATHNAME` → 공유 라이브러리 경로 (구성된 경우)

---

## 요약

PostgreSQL의 확장성은 다음을 가능하게 합니다:

| 확장 영역 | 설명 |
|-----------|------|
| 타입 시스템 | 기본 타입, 복합 타입, 도메인, 다형성 타입 |
| 함수 | SQL, 절차적 언어, C, 내부 함수 |
| 연산자 | 사용자 정의 연산자 및 최적화 힌트 |
| 집계 함수 | 상태 전환 함수를 사용한 커스텀 집계 |
| 인덱스 | 연산자 클래스 및 패밀리를 통한 커스텀 인덱싱 |
| 확장 패키징 | SQL 객체를 단일 설치 가능 단위로 번들링 |

이러한 확장성 덕분에 PostgreSQL은 다양한 도메인별 요구사항을 충족하는 유연하고 강력한 데이터베이스 시스템으로 자리 잡았습니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - Chapter 36: Extending SQL](https://www.postgresql.org/docs/current/extend.html)
- [CREATE FUNCTION 레퍼런스](https://www.postgresql.org/docs/current/sql-createfunction.html)
- [CREATE OPERATOR 레퍼런스](https://www.postgresql.org/docs/current/sql-createoperator.html)
- [CREATE AGGREGATE 레퍼런스](https://www.postgresql.org/docs/current/sql-createaggregate.html)
- [CREATE TYPE 레퍼런스](https://www.postgresql.org/docs/current/sql-createtype.html)
- [CREATE EXTENSION 레퍼런스](https://www.postgresql.org/docs/current/sql-createextension.html)
