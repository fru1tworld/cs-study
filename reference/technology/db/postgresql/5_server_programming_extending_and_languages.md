# PostgreSQL 서버 프로그래밍: 확장, 트리거, 절차적 언어

## Chapter 36: SQL 확장하기 (Extending SQL)

PostgreSQL의 가장 강력한 특징 중 하나는 확장성(extensibility)입니다. 이 장에서는 PostgreSQL의 SQL을 확장하는 다양한 방법을 살펴봅니다.

---

### 36.1 확장성의 작동 원리 (How Extensibility Works)

PostgreSQL은 카탈로그 주도(catalog-driven) 방식으로 동작하기 때문에 확장 가능합니다. 이는 일반적인 관계형 데이터베이스와 근본적으로 다른 설계입니다.

#### 핵심 원리

1. 카탈로그 주도 아키텍처
   - PostgreSQL은 시스템 카탈로그(메타데이터 테이블)에 광범위한 정보를 저장합니다
   - 데이터베이스, 테이블, 컬럼, 데이터 타입, 함수, 접근 방법 등의 정보 포함
   - 기존 시스템은 이러한 정보를 하드코딩된 프로시저에 저장함

2. 사용자 수정 가능한 카탈로그
   - 시스템 카탈로그 테이블은 일반 테이블처럼 보이며 수정 가능
   - PostgreSQL이 이 카탈로그를 기반으로 동작하므로, 수정을 통해 확장 가능

#### 확장 방법

| 방법 | 설명 |
|------|------|
| 동적 로딩 | 공유 라이브러리로 새 타입, 함수 구현 |
| SQL 코드 | SQL로 작성된 코드를 서버에 즉시 추가 |

이러한 유연성 덕분에 PostgreSQL은 새로운 애플리케이션의 빠른 프로토타이핑, 새로운 저장 구조 실험, 커스텀 확장에 매우 적합합니다.

---

### 36.2 PostgreSQL 타입 시스템 (The PostgreSQL Type System)

PostgreSQL 데이터 타입은 크게 네 가지 범주로 나뉩니다.

#### 36.2.1 기본 타입 (Base Types)

SQL 언어 수준 아래에서 구현되며, 일반적으로 C로 작성됩니다.

- PostgreSQL은 사용자가 제공한 함수를 통해서만 기본 타입을 조작
- 내장 기본 타입은 Chapter 8에 문서화됨
- 열거형 타입(enum types): SQL 명령으로만 생성 가능한 기본 타입의 하위 카테고리

#### 36.2.2 컨테이너 타입 (Container Types)

여러 값을 보유하는 세 종류의 컨테이너 타입이 있습니다.

##### 배열 (Arrays)
```sql
-- 배열은 자동으로 생성됨
CREATE TYPE point AS (x float, y float);
-- point[] 타입이 자동으로 사용 가능
```

- 동일한 타입의 여러 값을 보유
- 각 기본 타입, 복합 타입, 범위 타입, 도메인 타입에 대해 자동 생성
- 배열의 배열은 없음 (다차원 배열은 1차원으로 취급)

##### 복합 타입 (Composite Types / Row Types)
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

##### 범위 타입 (Range Types)
```sql
-- 내장 범위 타입 예시
SELECT '[2023-01-01, 2023-12-31]'::daterange;
SELECT '[1, 10)'::int4range;
```

- 동일 타입의 두 값(하한과 상한)을 보유
- 일부 내장 범위 타입이 존재하며 사용자 정의도 가능

#### 36.2.3 도메인 (Domains)

기본 타입에 제약 조건을 추가하여 유효한 값의 부분집합으로 제한합니다.

```sql
CREATE DOMAIN positive_integer AS integer
    CHECK (VALUE > 0);

CREATE DOMAIN email AS text
    CHECK (VALUE ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
```

- 일반적으로 기본 타입과 상호 교환 가능
- `CREATE DOMAIN` SQL 명령으로 생성

#### 36.2.4 의사 타입 (Pseudo-Types)

특수 목적의 타입으로 제한 사항이 있습니다.

| 특징 | 설명 |
|------|------|
| 테이블 컬럼 불가 | 컨테이너 타입의 구성 요소로 사용 불가 |
| 함수 선언 가능 | 인수 및 반환 타입으로 사용 가능 |
| 특수 함수 식별 | 특별한 함수 클래스 식별에 사용 |

주요 의사 타입: `void`, `trigger`, `event_trigger`, `internal`, `record`, `cstring` 등

#### 36.2.5 다형성 타입 (Polymorphic Types)

단일 함수 정의가 여러 다른 데이터 타입에서 작동할 수 있게 합니다.

##### 단순 계열 (Simple Family)

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

##### 호환 계열 (Common Family)

| 타입 | 설명 |
|------|------|
| `anycompatible` | 자동 승격으로 공통 타입 사용 |
| `anycompatiblearray` | 자동 승격으로 공통 배열 타입 사용 |
| `anycompatiblenonarray` | 자동 승격으로 공통 비배열 타입 사용 |
| `anycompatiblerange` | 자동 승격으로 공통 범위 타입 사용 |
| `anycompatiblemultirange` | 자동 승격으로 공통 다중 범위 타입 사용 |

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

##### 독립적인 타입 변수

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

### 36.3 사용자 정의 함수 (User-Defined Functions)

PostgreSQL은 네 가지 종류의 사용자 정의 함수를 제공합니다.

| 종류 | 설명 |
|------|------|
| SQL 함수 | SQL로 작성된 함수 |
| 절차적 언어 함수 | PL/pgSQL, PL/Python 등으로 작성 |
| 내부 함수 | C로 작성되어 서버에 정적 링크 |
| C 언어 함수 | 동적 로드되는 공유 라이브러리 |

#### 함수 기능

매개변수 타입:
- 기본 타입, 복합 타입, 또는 조합 허용

반환 타입:
- 기본 타입 또는 복합 타입 반환 가능
- 기본 또는 복합 값의 집합(sets) 반환 가능

고급 타입:
- 의사 타입(다형성 타입 포함) 허용
- 사용 가능한 기능은 함수 종류에 따라 다름

---

### 36.5 SQL 함수 (Query Language Functions)

SQL 함수는 임의의 SQL 문 목록을 실행하고 마지막 쿼리의 결과를 반환합니다.

#### 36.5.1 기본 SQL 함수

##### 단순 함수
```sql
CREATE FUNCTION one() RETURNS integer AS $$
    SELECT 1 AS result;
$$ LANGUAGE SQL;

SELECT one();
-- 결과: 1
```

##### 인수가 있는 함수
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

#### 36.5.2 복합 타입에 대한 함수

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

##### 복합 타입 반환
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

#### 36.5.3 출력 매개변수 (Output Parameters)

```sql
-- 단일 출력 매개변수
CREATE FUNCTION add_em(IN x int, IN y int, OUT sum int)
AS 'SELECT x + y'
LANGUAGE SQL;

SELECT add_em(3, 7);
-- 결과: 10
```

##### 다중 출력 매개변수
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

#### 36.5.4 가변 인수 함수 (VARIADIC)

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

#### 36.5.5 기본 매개변수 값

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

#### 36.5.6 집합 반환 함수 (Set-Returning Functions)

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

##### RETURNS TABLE 구문
```sql
CREATE FUNCTION sum_n_product_with_tab(x int)
RETURNS TABLE(sum int, product int) AS $$
    SELECT $1 + tab.y, $1 * tab.y FROM tab;
$$ LANGUAGE SQL;
```

#### 36.5.7 다형성 SQL 함수

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

### 36.6 함수 오버로딩 (Function Overloading)

동일한 SQL 이름으로 여러 함수를 정의할 수 있습니다(인수가 다른 경우).

#### 기본 개념

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

#### 모호성 주의사항

오버로딩된 함수 생성 시 모호성을 피해야 합니다:

```sql
-- 잠재적 모호성
CREATE FUNCTION test(int, real) RETURNS ...;
CREATE FUNCTION test(smallint, double precision) RETURNS ...;

-- test(1, 1.5) 호출 시 어떤 함수가 호출될지 불명확
```

#### 피해야 할 이름 충돌

1. 복합 타입 속성과의 충돌
   - 복합 타입의 단일 인수를 받는 함수는 해당 타입의 속성과 동일한 이름을 피해야 함
   - `attribute(table)`은 `table.attribute`와 동등하게 취급

2. VARIADIC vs 비-VARIADIC 함수
   - `foo(numeric)`과 `foo(VARIADIC numeric[])`를 모두 생성하면 모호성 발생
   - 동일 스키마에서는 비-VARIADIC 함수가 우선

#### C 언어 함수 제약

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

### 36.7 함수 휘발성 범주 (Function Volatility Categories)

모든 PostgreSQL 함수는 최적화기에게 함수의 동작을 알려주는 휘발성 분류를 가집니다.

#### 세 가지 범주

| 범주 | 설명 |
|------|------|
| VOLATILE | 기본값. 모든 작업 가능, 동일 인수에도 다른 결과 반환 가능 |
| STABLE | DB 수정 불가. 단일 문 내에서 동일 인수에 동일 결과 보장 |
| IMMUTABLE | DB 수정 불가. 동일 인수에 영원히 동일 결과 보장 |

#### 상세 설명

##### VOLATILE (휘발성)
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

##### STABLE (안정적)
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

##### IMMUTABLE (불변)
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

#### 비교 표

| 특성 | VOLATILE | STABLE | IMMUTABLE |
|------|----------|--------|-----------|
| 부작용 | 허용 | 불허 | 불허 |
| 다중 호출 최적화 | 불가 | 가능 | 가능 |
| 상수 폴딩 | 불가 | 불가 | 가능 |
| 인덱스 조건 | 무효 | 유효 | 유효 |

#### 중요 규칙

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

### 36.12 사용자 정의 집계 함수 (User-Defined Aggregates)

PostgreSQL의 사용자 정의 집계는 상태 값과 상태 전환 함수를 사용하여 정의됩니다.

#### 기본 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 상태 데이터 타입 | 상태 값의 내부 데이터 타입 |
| 초기 상태 값 | 상태의 시작 값 |
| 상태 전환 함수 | 각 행마다 상태 업데이트 |
| 최종 함수 (선택) | 최종 상태를 결과로 변환 |

#### 기본 구문

```sql
CREATE AGGREGATE aggregate_name (input_type)
(
    sfunc = transition_function,
    stype = state_type,
    initcond = 'initial_value',
    finalfunc = final_function  -- 선택사항
);
```

#### 예제: SUM 집계

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

#### 예제: 평균 집계

```sql
CREATE AGGREGATE avg (float8)
(
    sfunc = float8_accum,
    stype = float8[],
    finalfunc = float8_avg,
    initcond = '{0,0,0}'
);
```

#### NULL 처리

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

#### 36.12.1 이동 집계 모드 (Moving-Aggregate Mode)

윈도우 함수에서 이동 프레임 시작점을 효율적으로 처리하기 위해 역 전환 함수를 사용합니다.

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
- 역함수가 null을 반환하면 처음부터 다시 계산함

#### 36.12.2 다형성 및 VARIADIC 집계

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

#### 36.12.3 정렬 집합 집계 (Ordered-Set Aggregates)

명시적 정렬 순서와 직접 인수를 가지는 집계입니다.

```sql
-- 백분위수 집계 사용
SELECT percentile_disc(0.5) WITHIN GROUP (ORDER BY income)
FROM households;
```

특징:
- 직접 인수: 한 번만 평가 (예: 0.5 백분위수)
- 집계 인수: 정렬 순서와 함께 행마다 평가
- 윈도우 함수로 사용 불가

#### 36.12.4 부분 집계 (Partial Aggregation)

병렬 집계를 위해 서로 다른 데이터 하위 집합의 부분 상태 값을 결합합니다.

```sql
CREATE AGGREGATE max (anyelement)
(
    sfunc = greatest,
    stype = anyelement,
    combinefunc = greatest    -- 병렬 실행에 필요
);
```

요구사항:
- combine 함수는 교환 가능해야 함 (입력 순서가 보장되지 않음)
- 집계는 `PARALLEL SAFE`로 표시해야 함

---

### 36.13 사용자 정의 타입 (User-Defined Types)

PostgreSQL은 SQL 언어 수준 아래에서 정의되는 새로운 기본 타입의 생성을 허용합니다.

#### 핵심 요구사항

모든 사용자 정의 타입은 다음이 필요합니다:

| 함수 | 설명 |
|------|------|
| 입력 함수 | null 종료 C 문자열을 받아 내부 표현 반환 |
| 출력 함수 | 내부 표현을 받아 null 종료 C 문자열 반환 |
| 이진 입력 함수 (선택) | 더 빠르고 덜 이식 가능한 이진 연산용 `recv` |
| 이진 출력 함수 (선택) | 이진 연산용 `send` |

#### 예제: 복소수 타입

##### C 구조체 정의
```c
typedef struct Complex {
    double      x;
    double      y;
} Complex;
```

##### 입력 함수
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

##### 출력 함수
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

##### SQL 정의

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

#### 설계 고려사항

1. 입출력 함수는 서로 역함수 관계여야 하며, 이 관계가 깨지면 데이터 덤프/복원 시 심각한 문제가 발생합니다.

2. 가변 길이 타입:
   - 처음 4바이트는 `char[4]` 필드 (관례적으로 `vl_len_`)
   - `SET_VARSIZE()` 매크로로 총 크기 저장
   - `VARSIZE()` 매크로로 크기 조회

#### 정의 후 가능한 작업

1. 자동 배열 지원: 배열은 타입 이름 앞에 `_`가 붙음 (예: `complex[]`)
2. 추가 함수 정의: 데이터 타입에 대한 연산
3. 연산자 정의: 함수 위에 구축
4. 연산자 클래스 생성: 타입의 인덱싱 지원

---

### 36.14 사용자 정의 연산자 (User-Defined Operators)

사용자 정의 연산자는 함수 호출의 "구문적 설탕(syntactic sugar)"으로, 쿼리 플래너가 쿼리를 최적화하는 데 활용할 수 있는 추가 정보를 담을 수 있습니다.

#### 핵심 요구사항

1. 먼저 기본 함수 생성 후 연산자 생성
2. 함수가 실제 작업 수행
3. 시스템이 피연산자 수와 타입을 기반으로 호출할 연산자 결정

#### 구문

```sql
CREATE OPERATOR operator_name (
    leftarg = type1,           -- 선택 (전위 연산자는 생략)
    rightarg = type2,          -- 필수
    function = function_name,  -- 필수
    commutator = operator_name -- 선택적 최적화 힌트
);
```

#### 완전한 예제

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

#### 연산자 유형

| 유형 | 설명 |
|------|------|
| 이항 (중위) | `leftarg`와 `rightarg` 모두 있음 |
| 전위 (단항) | `leftarg` 매개변수 생략 |

---

### 36.15 연산자 최적화 정보 (Operator Optimization Information)

PostgreSQL 연산자 정의에는 쿼리 실행을 최적화하는 선택적 절이 포함될 수 있습니다.

#### 36.15.1 COMMUTATOR (교환자)

정의되는 연산자의 교환자인 연산자를 식별합니다.

정의: 모든 가능한 입력 값에 대해 `(x A y)`와 `(y B x)`가 동일하면 연산자 A는 연산자 B의 교환자입니다.

```sql
CREATE OPERATOR < (
    leftarg = complex,
    rightarg = complex,
    function = complex_lt,
    commutator = >  -- < 와 > 는 교환자
);
```

사용 사례: 인덱스 및 조인 절 최적화에 중요 — 쿼리 최적화기가 다른 계획 유형을 위해 절의 순서를 뒤집을 수 있습니다.

#### 36.15.2 NEGATOR (부정자)

정의되는 연산자의 부정자인 연산자를 식별합니다.

정의: 모든 가능한 입력에 대해 `(x A y)`와 `NOT (x B y)`가 동일하면 연산자 A는 연산자 B의 부정자입니다.

```sql
CREATE OPERATOR >= (
    ...
    negator = <  -- >= 와 < 는 부정자
);
```

사용 사례: 최적화기가 `NOT (x = y)`를 `x <> y`로 단순화할 수 있습니다.

#### 36.15.3 RESTRICT (제한)

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

#### 36.15.4 JOIN (조인)

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

#### 36.15.5 HASHES

이 연산자를 기반으로 한 조인에 해시 조인 방법을 사용할 수 있음을 시스템에 알립니다.

적용 대상: 동등성을 나타내는 `boolean`을 반환하는 이항 연산자

핵심 요구사항:
- 같은 코드로 해시되는 쌍에 대해서만 조인 연산자가 true 반환
- 해시 인덱스 연산자 패밀리 에 연산자가 나타나야 함
- 동일 연산자 패밀리에 교환자 가 있어야 함
- 기본 함수가 immutable 또는 stable 로 표시 (volatile 불가)

#### 36.15.6 MERGES

이 연산자를 기반으로 한 조인에 병합 조인 방법을 사용할 수 있음을 시스템에 알립니다.

적용 대상: 동등성을 나타내는 `boolean`을 반환하는 이항 연산자

핵심 요구사항:
- 두 데이터 타입 모두 완전히 정렬 가능 해야 함
- 조인 연산자가 동등성처럼 동작
- btree 인덱스 연산자 패밀리 의 동등성 멤버로 나타나야 함

---

### 36.16 인덱스를 위한 연산자 클래스와 연산자 패밀리 (Operator Classes and Operator Families for Indexes)

연산자 클래스는 인덱스 메서드가 특정 데이터 타입과 작동하는 데 필요한 연산 집합을 정의합니다.

#### 핵심 개념

##### 연산자 클래스
- 인덱스 접근 방법(B-Tree, GIN, GiST, SP-GiST, BRIN, Hash)과 연관
- 특정 데이터 타입 및 의미 해석에 대한 연산 식별
- 동일 데이터 타입과 인덱스 방법에 여러 연산자 클래스 존재 가능
- 일반적으로 하나의 클래스가 기본값으로 표시

##### 연산자 패밀리
- 하나 이상의 연산자 클래스 포함
- 패밀리 전체에 대한 인덱싱 가능 연산자 및 지원 함수 포함
- 교차 데이터 타입 연산 가능
- 모든 연산자와 함수가 호환 가능한 의미를 가져야 함

#### 인덱스 메서드 전략

전략은 전략 번호로 식별되는 일반화된 연산자입니다.

##### B-Tree 전략
| 연산 | 전략 번호 |
|------|-----------|
| 미만 | 1 |
| 이하 | 2 |
| 같음 | 3 |
| 이상 | 4 |
| 초과 | 5 |

##### Hash 전략
| 연산 | 전략 번호 |
|------|-----------|
| 같음 | 1 |

#### 지원 함수

##### B-Tree 지원 함수
| 함수 | 지원 번호 |
|------|-----------|
| 두 키 비교 및 정수 반환 | 1 |
| C 호출 가능 정렬 지원 함수 주소 반환 | 2 |
| 테스트 값을 기본값 더하기/빼기 오프셋과 비교 | 3 |
| btree 중복 제거 안전 여부 결정 | 4 |
| 연산자 클래스별 옵션 정의 | 5 |
| C 호출 가능 건너뛰기 지원 함수 주소 반환 | 6 |

#### 예제: 복소수 연산자 클래스

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

#### 연산자 패밀리 예제

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

### 36.17 확장 패키징 (Extension Packaging)

PostgreSQL 확장을 사용하면 관련 SQL 객체를 단일 설치 가능한 단위로 패키징할 수 있습니다.

#### 주요 장점

- 단일 명령 설치/제거: `CREATE EXTENSION` 및 `DROP EXTENSION` 사용
- 덤프 단순화: `pg_dump`가 개별 객체가 아닌 `CREATE EXTENSION` 명령만 포함
- 쉬운 마이그레이션: 새 확장 버전으로 업데이트 간소화
- 종속성 추적: PostgreSQL이 객체 관계 이해

#### 필수 파일

##### 1. 제어 파일 (`extension_name.control`)

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

##### 2. SQL 스크립트 파일

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

##### 3. 제어 파일 예제

```ini
# pair 확장
comment = '키/값 쌍 데이터 타입'
default_version = '1.0'
relocatable = false
```

#### 스키마 재배치

세 가지 수준의 재배치 지원:

##### 1. 완전히 재배치 가능
```ini
relocatable = true
```
- `ALTER EXTENSION SET SCHEMA`로 언제든지 확장을 다른 스키마로 이동 가능
- 모든 객체가 처음에 하나의 스키마에 있어야 함

##### 2. 설치 중에만 재배치 가능
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

##### 3. 재배치 불가능
```ini
relocatable = false
schema = 'myschema'
```
- 지정된 스키마에 고정
- 제어 파일과 일치하지 않으면 `SCHEMA` 옵션 불허

#### 확장 업데이트

##### 업데이트 스크립트

명명 패턴: `extension_name--old_version--target_version.sql`

예: `foo--1.0--1.1.sql`

```sql
-- 1.0에서 1.1로 업그레이드하는 변경 사항
ALTER FUNCTION my_func() ...;
CREATE FUNCTION new_func() ...;
```

##### 업데이트 명령

```sql
ALTER EXTENSION foo UPDATE TO '1.1';
```

PostgreSQL은 필요한 경우 업데이트 스크립트를 자동으로 체인 연결합니다 (예: 1.0 → 1.1 → 2.0).

#### 확장 설치

```sql
-- 기본 설치
CREATE EXTENSION extension_name;

-- 특정 버전
CREATE EXTENSION extension_name VERSION '1.0';

-- 특정 스키마
CREATE EXTENSION extension_name SCHEMA myschema;
```

#### 특수 변수

스크립트 파일에서:
- `@extschema@` → 대상 스키마 이름
- `@extschema:name@` → 참조된 확장의 스키마
- `@extowner@` → `CREATE EXTENSION`을 호출하는 사용자 이름
- `MODULE_PATHNAME` → 공유 라이브러리 경로 (구성된 경우)

---

### 요약

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

### 참고 자료

- [PostgreSQL 공식 문서 - Chapter 36: Extending SQL](https://www.postgresql.org/docs/current/extend.html)
- [CREATE FUNCTION 레퍼런스](https://www.postgresql.org/docs/current/sql-createfunction.html)
- [CREATE OPERATOR 레퍼런스](https://www.postgresql.org/docs/current/sql-createoperator.html)
- [CREATE AGGREGATE 레퍼런스](https://www.postgresql.org/docs/current/sql-createaggregate.html)
- [CREATE TYPE 레퍼런스](https://www.postgresql.org/docs/current/sql-createtype.html)
- [CREATE EXTENSION 레퍼런스](https://www.postgresql.org/docs/current/sql-createextension.html)

---

## Chapter 39: 트리거 (Triggers)

### 목차

1. [트리거 개요](#1-트리거-개요)
2. [트리거 동작의 개요](#2-트리거-동작의-개요)
3. [데이터 변경의 가시성](#3-데이터-변경의-가시성-visibility-of-data-changes)
4. [트리거 함수 작성](#4-트리거-함수-작성)
5. [트리거 생성 및 관리](#5-트리거-생성-및-관리)
6. [완전한 트리거 예제](#6-완전한-트리거-예제)
7. [고급 트리거 기능](#7-고급-트리거-기능)

---

### 1. 트리거 개요

트리거(Trigger)는 특정 유형의 작업이 수행될 때마다 데이터베이스가 자동으로 특정 함수를 실행하도록 하는 명세입니다. 트리거를 연결할 수 있는 객체는 다음과 같습니다:

- 테이블 (파티션 테이블 포함)
- 뷰 (Views)
- 외부 테이블 (Foreign Tables)

#### 1.1 트리거의 용도

트리거는 다음과 같은 용도로 널리 사용됩니다:

- 데이터 무결성 검증: 복잡한 제약 조건 구현
- 감사 로깅 (Audit Logging): 데이터 변경 이력 추적
- 자동 값 계산: 파생 컬럼 자동 갱신
- 복잡한 비즈니스 로직 구현: 자동화된 워크플로우
- 데이터 동기화: 관련 테이블 간 일관성 유지

#### 1.2 지원되는 프로시저 언어

트리거 함수는 다음 언어로 작성할 수 있습니다:

| 언어 | 설명 |
|------|------|
| PL/pgSQL | PostgreSQL의 기본 절차적 언어 |
| PL/Tcl | Tcl 절차적 언어 |
| PL/Perl | Perl 절차적 언어 |
| PL/Python | Python 절차적 언어 |
| C | C 언어 (복잡하지만 가능) |

> 주의: 순수 SQL 함수 언어는 트리거 함수로 사용할 수 없습니다.

---

### 2. 트리거 동작의 개요

#### 2.1 트리거 유형

##### 테이블 및 외부 테이블에서의 트리거

| 작업 | 설명 |
|------|------|
| `INSERT` | 새 행 삽입 시 |
| `UPDATE` | 기존 행 갱신 시 |
| `DELETE` | 행 삭제 시 |
| `TRUNCATE` | 테이블 잘라내기 시 |

##### 뷰에서의 트리거

뷰에서는 `INSTEAD OF` 트리거를 사용하여 `INSERT`, `UPDATE`, `DELETE` 작업을 처리합니다.

#### 2.2 트리거 실행 시점

| 시점 | 설명 |
|------|------|
| BEFORE | 작업 실행 전에 트리거 발동 |
| AFTER | 작업 실행 후에 트리거 발동 |
| INSTEAD OF | 작업 대신 트리거 실행 (뷰 전용, 행 수준만 가능) |

#### 2.3 트리거 실행 수준

| 수준 | 설명 |
|------|------|
| 행 수준 (FOR EACH ROW) | 영향받는 각 행마다 한 번씩 실행 |
| 문장 수준 (FOR EACH STATEMENT) | SQL 문당 한 번만 실행 (영향받는 행 수와 무관) |

#### 2.4 트리거 함수 요구사항

트리거 함수는 반드시:

1. 인자 없이 정의되어야 함
2. 반환 타입이 `trigger`이어야 함
3. 트리거 정의 전에 생성되어야 함

```sql
-- 트리거 함수 기본 구조
CREATE FUNCTION trigger_function_name()
RETURNS trigger AS $$
BEGIN
    -- 트리거 로직
    RETURN NEW;  -- 또는 OLD, NULL
END;
$$ LANGUAGE plpgsql;
```

#### 2.5 트리거 실행 순서

동일한 이벤트에 대해 동일한 테이블에 여러 트리거가 정의된 경우:

- 트리거 이름의 알파벳 순서로 실행됩니다
- 각 트리거에서 수정된 행이 다음 트리거의 입력이 됩니다
- `BEFORE` 또는 `INSTEAD OF` 트리거가 `NULL`을 반환하면 해당 행에 대한 작업이 취소됩니다

---

### 3. 데이터 변경의 가시성 (Visibility of Data Changes)

트리거 내에서 데이터 변경의 가시성은 트리거 유형과 실행 시점에 따라 달라집니다.

#### 3.1 문장 수준 트리거

| 시점 | 가시성 |
|------|--------|
| BEFORE | 해당 문장에 의한 변경 사항을 볼 수 없음 |
| AFTER | 해당 문장에 의한 모든 수정 사항을 볼 수 있음 |

#### 3.2 행 수준 트리거

| 트리거 유형 | 가시성 |
|-------------|--------|
| BEFORE | 트리거를 발동시킨 데이터 변경을 볼 수 없음 (아직 발생하지 않음). 같은 외부 명령에서 이전에 처리된 행의 변경 효과는 볼 수 있음 |
| AFTER | 외부 명령에 의한 모든 데이터 변경을 볼 수 있음 (이미 완료됨) |
| INSTEAD OF | 같은 외부 명령에서 이전 INSTEAD OF 트리거 발동에 의한 데이터 변경 효과를 볼 수 있음 |

#### 3.3 함수 휘발성 (Volatility) 고려사항

표준 절차적 언어로 작성된 트리거 함수에는 다음 규칙이 적용됩니다:

| 휘발성 | 동작 |
|--------|------|
| VOLATILE (기본값) | 위의 가시성 규칙이 적용됨 |
| STABLE 또는 IMMUTABLE | 트리거 유형에 관계없이 호출 명령에 의한 변경을 볼 수 없음 |

> 주의: 행 처리 순서는 명령이 여러 행에 영향을 미칠 때 예측할 수 없으므로, BEFORE 트리거에서 이전에 처리된 행의 변경 효과에 의존하는 것은 권장되지 않습니다.

---

### 4. 트리거 함수 작성

#### 4.1 PL/pgSQL 트리거 함수의 특수 변수

PL/pgSQL 함수가 트리거로 호출될 때 다음 특수 변수들이 자동으로 생성됩니다:

##### 레코드 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `NEW` | record | INSERT/UPDATE 작업에서 새로운 데이터베이스 행. DELETE 작업이나 문장 수준 트리거에서는 NULL |
| `OLD` | record | UPDATE/DELETE 작업에서 이전 데이터베이스 행. INSERT 작업이나 문장 수준 트리거에서는 NULL |

##### 컨텍스트 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `TG_NAME` | name | 발동된 트리거의 이름 |
| `TG_WHEN` | text | BEFORE, AFTER, 또는 INSTEAD OF |
| `TG_LEVEL` | text | ROW 또는 STATEMENT |
| `TG_OP` | text | 작업 유형: INSERT, UPDATE, DELETE, 또는 TRUNCATE |
| `TG_RELID` | oid | 트리거를 발생시킨 테이블의 객체 ID |
| `TG_TABLE_NAME` | name | 트리거를 발생시킨 테이블 이름 |
| `TG_TABLE_SCHEMA` | name | 테이블의 스키마 이름 |
| `TG_NARGS` | integer | CREATE TRIGGER 문에서 제공된 인자 수 |
| `TG_ARGV[]` | text[] | CREATE TRIGGER 문의 인자 배열 (0부터 인덱싱) |

#### 4.2 반환 값 규칙

##### 행 수준 BEFORE 트리거

| 반환 값 | 효과 |
|---------|------|
| `NULL` | 이 행에 대한 작업 건너뜀 |
| 수정된 `NEW` | 변경된 행으로 삽입/갱신 진행 |
| `NEW` 그대로 | 원래 행으로 작업 진행 |

```sql
-- BEFORE 트리거 반환 값 예제
CREATE FUNCTION validate_and_modify() RETURNS trigger AS $$
BEGIN
    -- 조건 검사 후 작업 건너뛰기
    IF NEW.value < 0 THEN
        RETURN NULL;  -- 이 행에 대한 작업 취소
    END IF;

    -- 값 수정
    NEW.modified_at := current_timestamp;
    RETURN NEW;  -- 수정된 행으로 진행
END;
$$ LANGUAGE plpgsql;
```

##### 행 수준 INSTEAD OF 트리거

| 반환 값 | 효과 |
|---------|------|
| `NULL` | 수정이 수행되지 않았음을 신호 |
| `NEW` (INSERT/UPDATE) | RETURNING 절 지원, 데이터 수정됨을 신호 |
| `OLD` (DELETE) | RETURNING 절 지원, 데이터 수정됨을 신호 |

##### 행 수준 AFTER 트리거 및 문장 수준 트리거

반환 값은 무시됩니다 (NULL 반환 가능). 단, 오류를 발생시켜 작업 전체를 중단할 수 있습니다.

#### 4.3 기본 트리거 함수 예제

##### 예제 1: 데이터 검증 및 자동 값 설정

```sql
-- 테이블 정의
CREATE TABLE emp (
    empname           text,
    salary            integer,
    last_date         timestamp,
    last_user         text
);

-- 트리거 함수 정의
CREATE FUNCTION emp_stamp() RETURNS trigger AS $emp_stamp$
BEGIN
    -- 필수 필드 검증
    IF NEW.empname IS NULL THEN
        RAISE EXCEPTION 'empname cannot be null';
    END IF;

    IF NEW.salary IS NULL THEN
        RAISE EXCEPTION '% cannot have null salary', NEW.empname;
    END IF;

    -- 비즈니스 규칙 적용
    IF NEW.salary < 0 THEN
        RAISE EXCEPTION '% cannot have a negative salary', NEW.empname;
    END IF;

    -- 자동으로 변경 이력 기록
    NEW.last_date := current_timestamp;
    NEW.last_user := current_user;

    RETURN NEW;
END;
$emp_stamp$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_stamp
    BEFORE INSERT OR UPDATE ON emp
    FOR EACH ROW
    EXECUTE FUNCTION emp_stamp();
```

##### 예제 2: 감사 로깅 (Audit Logging)

```sql
-- 원본 테이블
CREATE TABLE emp (
    empname           text NOT NULL,
    salary            integer
);

-- 감사 테이블
CREATE TABLE emp_audit (
    operation         char(1)   NOT NULL,
    stamp             timestamp NOT NULL,
    userid            text      NOT NULL,
    empname           text      NOT NULL,
    salary            integer
);

-- 감사 트리거 함수
CREATE OR REPLACE FUNCTION process_emp_audit() RETURNS trigger AS $emp_audit$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO emp_audit
        SELECT 'D', now(), current_user, OLD.*;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO emp_audit
        SELECT 'U', now(), current_user, NEW.*;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp_audit
        SELECT 'I', now(), current_user, NEW.*;
    END IF;

    RETURN NULL;  -- AFTER 트리거이므로 반환값 무시됨
END;
$emp_audit$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_audit
    AFTER INSERT OR UPDATE OR DELETE ON emp
    FOR EACH ROW
    EXECUTE FUNCTION process_emp_audit();
```

##### 예제 3: 뷰에 대한 INSTEAD OF 트리거

```sql
-- 기본 테이블
CREATE TABLE emp (
    empname           text PRIMARY KEY,
    salary            integer
);

-- 뷰 생성
CREATE VIEW emp_view AS
    SELECT e.empname, e.salary
    FROM emp e;

-- INSTEAD OF 트리거 함수
CREATE OR REPLACE FUNCTION update_emp_view() RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        DELETE FROM emp WHERE empname = OLD.empname;
        IF NOT FOUND THEN
            RETURN NULL;
        END IF;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE') THEN
        UPDATE emp SET salary = NEW.salary
        WHERE empname = OLD.empname;
        IF NOT FOUND THEN
            RETURN NULL;
        END IF;
        RETURN NEW;

    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp VALUES(NEW.empname, NEW.salary);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER emp_view_trigger
    INSTEAD OF INSERT OR UPDATE OR DELETE ON emp_view
    FOR EACH ROW
    EXECUTE FUNCTION update_emp_view();
```

---

### 5. 트리거 생성 및 관리

#### 5.1 CREATE TRIGGER 구문

```sql
CREATE [ OR REPLACE ] [ CONSTRAINT ] TRIGGER name
    { BEFORE | AFTER | INSTEAD OF } { event [ OR ... ] }
    ON table_name
    [ FROM referenced_table_name ]
    [ NOT DEFERRABLE | [ DEFERRABLE ] [ INITIALLY IMMEDIATE | INITIALLY DEFERRED ] ]
    [ REFERENCING { { OLD | NEW } TABLE [ AS ] transition_relation_name } [ ... ] ]
    [ FOR [ EACH ] { ROW | STATEMENT } ]
    [ WHEN ( condition ) ]
    EXECUTE { FUNCTION | PROCEDURE } function_name ( arguments )
```

여기서 `event`는 다음 중 하나입니다:

- `INSERT`
- `UPDATE [ OF column_name [, ... ] ]`
- `DELETE`
- `TRUNCATE`

#### 5.2 기본 트리거 생성 예제

```sql
-- BEFORE INSERT 행 수준 트리거
CREATE TRIGGER check_insert
    BEFORE INSERT ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION check_account_insert();

-- AFTER UPDATE 문장 수준 트리거
CREATE TRIGGER summarize_update
    AFTER UPDATE ON sales
    FOR EACH STATEMENT
    EXECUTE FUNCTION summarize_sales();

-- 특정 컬럼 UPDATE 시에만 실행
CREATE TRIGGER update_salary_trigger
    AFTER UPDATE OF salary ON employees
    FOR EACH ROW
    EXECUTE FUNCTION log_salary_change();
```

#### 5.3 조건부 트리거 (WHEN 절)

`WHEN` 절을 사용하여 트리거 발동 조건을 지정할 수 있습니다:

```sql
-- 급여가 변경된 경우에만 트리거 실행
CREATE TRIGGER salary_change_trigger
    AFTER UPDATE ON employees
    FOR EACH ROW
    WHEN (OLD.salary IS DISTINCT FROM NEW.salary)
    EXECUTE FUNCTION log_salary_change();

-- 특정 조건을 만족하는 경우에만 실행
CREATE TRIGGER high_value_insert
    BEFORE INSERT ON orders
    FOR EACH ROW
    WHEN (NEW.amount > 10000)
    EXECUTE FUNCTION validate_high_value_order();
```

> 성능 팁: `WHEN` 조건을 사용하면 불필요한 트리거 실행을 방지하여 성능을 크게 향상시킬 수 있습니다.

#### 5.4 트리거 관리 명령

##### 트리거 비활성화/활성화

```sql
-- 특정 트리거 비활성화
ALTER TABLE table_name DISABLE TRIGGER trigger_name;

-- 특정 트리거 활성화
ALTER TABLE table_name ENABLE TRIGGER trigger_name;

-- 테이블의 모든 트리거 비활성화
ALTER TABLE table_name DISABLE TRIGGER ALL;

-- 테이블의 모든 사용자 트리거 비활성화 (시스템 생성 트리거 제외)
ALTER TABLE table_name DISABLE TRIGGER USER;
```

##### 트리거 삭제

```sql
-- 트리거 삭제
DROP TRIGGER [ IF EXISTS ] trigger_name ON table_name [ CASCADE | RESTRICT ];

-- 예제
DROP TRIGGER emp_stamp ON emp;
DROP TRIGGER IF EXISTS emp_audit ON emp CASCADE;
```

##### 트리거 정보 조회

```sql
-- 테이블의 트리거 목록 조회
SELECT trigger_name, event_manipulation, action_timing, action_statement
FROM information_schema.triggers
WHERE event_object_table = 'emp';

-- pg_trigger 시스템 카탈로그에서 상세 정보 조회
SELECT tgname, tgenabled, tgtype
FROM pg_trigger
WHERE tgrelid = 'emp'::regclass;
```

---

### 6. 완전한 트리거 예제

#### 6.1 재고 관리 시스템 예제

```sql
-- 제품 테이블
CREATE TABLE products (
    product_id    serial PRIMARY KEY,
    product_name  text NOT NULL,
    quantity      integer NOT NULL DEFAULT 0,
    reorder_level integer NOT NULL DEFAULT 10,
    last_updated  timestamp DEFAULT current_timestamp
);

-- 재고 이력 테이블
CREATE TABLE inventory_log (
    log_id        serial PRIMARY KEY,
    product_id    integer REFERENCES products(product_id),
    old_quantity  integer,
    new_quantity  integer,
    change_amount integer,
    change_type   text,
    changed_by    text,
    changed_at    timestamp DEFAULT current_timestamp
);

-- 재주문 알림 테이블
CREATE TABLE reorder_alerts (
    alert_id      serial PRIMARY KEY,
    product_id    integer REFERENCES products(product_id),
    product_name  text,
    current_qty   integer,
    reorder_level integer,
    created_at    timestamp DEFAULT current_timestamp
);

-- 재고 변경 로깅 및 재주문 알림 트리거 함수
CREATE OR REPLACE FUNCTION manage_inventory() RETURNS trigger AS $$
DECLARE
    change_type_val text;
    change_amount_val integer;
BEGIN
    -- 변경 유형 및 수량 결정
    IF TG_OP = 'INSERT' THEN
        change_type_val := 'INITIAL';
        change_amount_val := NEW.quantity;

        -- 이력 기록
        INSERT INTO inventory_log (product_id, old_quantity, new_quantity,
                                   change_amount, change_type, changed_by)
        VALUES (NEW.product_id, 0, NEW.quantity,
                change_amount_val, change_type_val, current_user);

    ELSIF TG_OP = 'UPDATE' THEN
        -- 수량이 변경된 경우에만 처리
        IF OLD.quantity != NEW.quantity THEN
            change_amount_val := NEW.quantity - OLD.quantity;

            IF change_amount_val > 0 THEN
                change_type_val := 'RESTOCK';
            ELSE
                change_type_val := 'SALE';
            END IF;

            -- 이력 기록
            INSERT INTO inventory_log (product_id, old_quantity, new_quantity,
                                       change_amount, change_type, changed_by)
            VALUES (NEW.product_id, OLD.quantity, NEW.quantity,
                    change_amount_val, change_type_val, current_user);

            -- 재주문 수준 이하로 떨어진 경우 알림 생성
            IF NEW.quantity <= NEW.reorder_level AND OLD.quantity > OLD.reorder_level THEN
                INSERT INTO reorder_alerts (product_id, product_name,
                                            current_qty, reorder_level)
                VALUES (NEW.product_id, NEW.product_name,
                        NEW.quantity, NEW.reorder_level);
            END IF;
        END IF;

        -- 마지막 업데이트 시간 자동 갱신
        NEW.last_updated := current_timestamp;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER inventory_management
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION manage_inventory();
```

#### 6.2 사용 예제

```sql
-- 제품 추가
INSERT INTO products (product_name, quantity, reorder_level)
VALUES ('Widget A', 100, 20);

-- 재고 감소 (판매)
UPDATE products SET quantity = quantity - 15 WHERE product_id = 1;

-- 재고 감소 (재주문 수준 이하로)
UPDATE products SET quantity = 18 WHERE product_id = 1;

-- 재고 확인
SELECT * FROM products;

-- 재고 이력 확인
SELECT * FROM inventory_log ORDER BY changed_at DESC;

-- 재주문 알림 확인
SELECT * FROM reorder_alerts;
```

---

### 7. 고급 트리거 기능

#### 7.1 전이 테이블 (Transition Tables)

`AFTER STATEMENT` 트리거에서 전이 테이블을 사용하면 영향받은 모든 행을 한 번에 처리할 수 있습니다. 이는 대량 데이터 처리 시 행 수준 트리거보다 훨씬 효율적입니다.

```sql
-- 전이 테이블을 사용한 감사 트리거
CREATE OR REPLACE FUNCTION process_emp_audit_bulk() RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO emp_audit
            SELECT 'D', now(), current_user, o.*
            FROM old_table o;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO emp_audit
            SELECT 'U', now(), current_user, n.*
            FROM new_table n;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO emp_audit
            SELECT 'I', now(), current_user, n.*
            FROM new_table n;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- INSERT용 트리거 (새 행 전이 테이블)
CREATE TRIGGER emp_audit_ins
    AFTER INSERT ON emp
    REFERENCING NEW TABLE AS new_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();

-- UPDATE용 트리거 (이전/새 행 전이 테이블)
CREATE TRIGGER emp_audit_upd
    AFTER UPDATE ON emp
    REFERENCING OLD TABLE AS old_table NEW TABLE AS new_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();

-- DELETE용 트리거 (이전 행 전이 테이블)
CREATE TRIGGER emp_audit_del
    AFTER DELETE ON emp
    REFERENCING OLD TABLE AS old_table
    FOR EACH STATEMENT
    EXECUTE FUNCTION process_emp_audit_bulk();
```

#### 7.2 제약 트리거 (Constraint Triggers)

제약 트리거는 트랜잭션 끝까지 실행을 지연시킬 수 있습니다:

```sql
-- 제약 트리거 생성
CREATE CONSTRAINT TRIGGER check_foreign_key
    AFTER INSERT OR UPDATE ON child_table
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION check_fk_constraint();
```

#### 7.3 트리거 인자 사용

트리거 함수에 인자를 전달하면 범용으로 재사용 가능한 트리거를 만들 수 있습니다:

```sql
-- 범용 로깅 트리거 함수
CREATE OR REPLACE FUNCTION generic_audit_trigger() RETURNS trigger AS $$
DECLARE
    audit_table text;
BEGIN
    -- 첫 번째 인자로 감사 테이블 이름 받기
    audit_table := TG_ARGV[0];

    -- 동적 SQL로 감사 레코드 삽입
    EXECUTE format('INSERT INTO %I (operation, table_name, changed_at, changed_by)
                    VALUES ($1, $2, $3, $4)', audit_table)
    USING TG_OP, TG_TABLE_NAME, current_timestamp, current_user;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 여러 테이블에 동일한 함수 사용
CREATE TRIGGER orders_audit
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION generic_audit_trigger('orders_audit_log');

CREATE TRIGGER customers_audit
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION generic_audit_trigger('customers_audit_log');
```

#### 7.4 캐스케이딩 트리거

트리거 함수 내에서 다른 트리거를 발동시키는 SQL 문을 실행할 수 있습니다:

```sql
-- 캐스케이딩 삭제를 구현하는 트리거
CREATE OR REPLACE FUNCTION cascade_delete_orders() RETURNS trigger AS $$
BEGIN
    -- 관련 주문 항목 삭제 (order_items 테이블의 트리거도 발동됨)
    DELETE FROM order_items WHERE order_id = OLD.order_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_cascade_delete
    BEFORE DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION cascade_delete_orders();
```

> 주의: 캐스케이딩 트리거 사용 시 무한 재귀가 발생하지 않도록 주의해야 합니다. 무한 루프 방지는 개발자의 책임입니다.

#### 7.5 생성된 컬럼과 트리거

저장된 생성 컬럼(Stored Generated Columns)은 `BEFORE` 트리거 실행 이후, `AFTER` 트리거 실행 이전에 계산됩니다:

- BEFORE 트리거: `OLD` 행에는 이전 생성 값이 포함되지만, `NEW` 행에는 아직 새 생성 값이 없습니다.
- 가상 생성 컬럼(Virtual Generated Columns): 트리거 발동 시점에는 계산되지 않습니다.

---

### 요약

PostgreSQL 트리거는 데이터베이스에서 자동화된 로직을 구현하는 강력한 도구입니다:

| 기능 | 설명 |
|------|------|
| 데이터 무결성 | 복잡한 비즈니스 규칙 자동 적용 |
| 감사 추적 | 데이터 변경 이력 자동 기록 |
| 자동 계산 | 파생 값 자동 갱신 |
| 조건부 실행 | WHEN 절로 선택적 트리거 발동 |
| 전이 테이블 | 대량 데이터 효율적 처리 |
| 지연 실행 | 제약 트리거로 트랜잭션 끝까지 지연 |

트리거를 효과적으로 활용하면 애플리케이션 코드를 단순화하고 데이터베이스 수준에서 일관된 비즈니스 로직을 유지할 수 있습니다.

---

### 참고 자료

- [PostgreSQL 공식 문서 - Triggers](https://www.postgresql.org/docs/current/triggers.html)
- [PostgreSQL 공식 문서 - CREATE TRIGGER](https://www.postgresql.org/docs/current/sql-createtrigger.html)
- [PostgreSQL 공식 문서 - PL/pgSQL Trigger Functions](https://www.postgresql.org/docs/current/plpgsql-trigger.html)

---

## PostgreSQL 이벤트 트리거 (Event Triggers)

### 목차
1. [개요](#1-개요)
2. [이벤트 트리거와 일반 트리거의 차이점](#2-이벤트-트리거와-일반-트리거의-차이점)
3. [이벤트 종류](#3-이벤트-종류)
4. [이벤트 트리거 생성](#4-이벤트-트리거-생성)
5. [이벤트 트리거 지원 함수](#5-이벤트-트리거-지원-함수)
6. [이벤트 트리거 관리](#6-이벤트-트리거-관리)
7. [실용적인 예제](#7-실용적인-예제)
8. [C 언어로 이벤트 트리거 함수 작성](#8-c-언어로-이벤트-트리거-함수-작성)
9. [주의사항 및 제한사항](#9-주의사항-및-제한사항)

---

### 1. 개요

이벤트 트리거(Event Trigger)는 PostgreSQL에서 일반 트리거 메커니즘을 보완하는 기능입니다. DDL(Data Definition Language) 이벤트를 캡처하여 데이터베이스 수준의 스키마 변경 작업을 모니터링하고 제어할 수 있습니다.

#### 주요 특징

- 데이터베이스 전역 범위: 특정 테이블에 연결되지 않고 데이터베이스 전체에서 작동
- DDL 명령 캡처: `CREATE`, `ALTER`, `DROP` 등의 DDL 명령에 반응
- 프로시저 언어 지원: PL/pgSQL, PL/Python, C 등 이벤트 트리거를 지원하는 모든 프로시저 언어로 작성 가능 (순수 SQL은 불가)

---

### 2. 이벤트 트리거와 일반 트리거의 차이점

| 특성 | 일반 트리거 (Regular Trigger) | 이벤트 트리거 (Event Trigger) |
|------|------------------------------|-------------------------------|
| 범위 | 특정 테이블에 연결 | 데이터베이스 전역 |
| 이벤트 유형 | DML (INSERT, UPDATE, DELETE) | DDL (CREATE, ALTER, DROP 등) |
| 작성 언어 | 모든 프로시저 언어 + SQL | 프로시저 언어 또는 C (SQL 제외) |
| 반환 타입 | `trigger` | `event_trigger` |
| 데이터 접근 | 테이블의 행 데이터 | DDL 명령 메타데이터 |

---

### 3. 이벤트 종류

PostgreSQL은 현재 5가지 이벤트 트리거 유형을 지원합니다.

#### 3.1 ddl_command_start

DDL 명령이 실행되기 직전에 발생합니다.

##### 트리거가 발생하는 명령
- `CREATE`, `ALTER`, `DROP`
- `COMMENT`, `GRANT`, `REVOKE`
- `IMPORT FOREIGN SCHEMA`, `REINDEX`
- `REFRESH MATERIALIZED VIEW`, `SECURITY LABEL`
- `SELECT INTO` (`CREATE TABLE AS`와 동등)

##### 트리거가 발생하지 않는 경우
- 공유 객체에 대한 명령 (데이터베이스, 역할, 테이블스페이스, 매개변수 권한)
- `ALTER SYSTEM` 명령
- 이벤트 트리거 자체에 대한 DDL 명령

```sql
-- 예제: DDL 명령 시작 시 로깅
CREATE OR REPLACE FUNCTION log_ddl_start()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE NOTICE 'DDL 명령 시작: %', tg_tag;
END;
$$;

CREATE EVENT TRIGGER ddl_start_logger
    ON ddl_command_start
    EXECUTE FUNCTION log_ddl_start();
```

#### 3.2 ddl_command_end

DDL 명령이 완료된 직후에 발생합니다. 명령의 효과는 반영되었지만 트랜잭션은 아직 커밋되지 않은 상태입니다.

```sql
-- 예제: DDL 명령 완료 시 상세 정보 기록
CREATE OR REPLACE FUNCTION log_ddl_end()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        RAISE NOTICE 'DDL 완료: % - 객체: %.%',
                     obj.command_tag,
                     obj.schema_name,
                     obj.object_identity;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER ddl_end_logger
    ON ddl_command_end
    EXECUTE FUNCTION log_ddl_end();
```

#### 3.3 sql_drop

객체가 시스템 카탈로그에서 삭제된 직후, `ddl_command_end` 이벤트 직전에 발생합니다.

이 이벤트는 `DROP` 명령과 일부 `ALTER` 명령(예: `ALTER TABLE DROP COLUMN`)에서 발생합니다.

```sql
-- 예제: 삭제된 객체 정보 기록
CREATE OR REPLACE FUNCTION log_dropped_objects()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        RAISE NOTICE '삭제됨: % - %.% (%)',
                     obj.object_type,
                     obj.schema_name,
                     obj.object_name,
                     obj.object_identity;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER drop_logger
    ON sql_drop
    EXECUTE FUNCTION log_dropped_objects();
```

#### 3.4 table_rewrite

테이블이 재작성(rewrite)되기 직전에 발생합니다. `ALTER TABLE` 또는 `ALTER TYPE` 명령으로 인해 테이블이 물리적으로 재작성될 때 발생합니다.

참고: `CLUSTER`나 `VACUUM`에 의한 재작성에는 트리거되지 않습니다.

##### 테이블 재작성이 발생하는 경우
- 열의 데이터 타입 변경
- 열의 기본값 변경 (기존 행에 영향을 미치는 경우)
- 테이블의 지속성(persistence) 변경
- 테이블의 접근 방법(access method) 변경

```sql
-- 예제: 테이블 재작성 모니터링
CREATE OR REPLACE FUNCTION monitor_table_rewrite()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE NOTICE '테이블 재작성: %, 이유 코드: %',
                 pg_event_trigger_table_rewrite_oid()::regclass,
                 pg_event_trigger_table_rewrite_reason();
END;
$$;

CREATE EVENT TRIGGER table_rewrite_monitor
    ON table_rewrite
    EXECUTE FUNCTION monitor_table_rewrite();
```

#### 3.5 login

인증된 사용자가 데이터베이스에 로그인할 때 발생합니다.

##### 주의사항
- 스탠바이 서버에서도 실행됩니다
- 버그가 있으면 시스템 로그인이 불가능해질 수 있습니다
- 스탠바이 서버에서는 데이터베이스 쓰기를 피해야 합니다
- 장기 실행 쿼리를 피해야 합니다

##### 문제 발생 시 해결 방법
- 연결 문자열이나 설정 파일에서 `event_triggers = false` 설정
- 단일 사용자 모드로 재시작

```sql
-- 예제: 로그인 감사 로깅
CREATE OR REPLACE FUNCTION audit_login()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO login_audit (username, login_time, client_addr)
    VALUES (current_user, current_timestamp, inet_client_addr());
EXCEPTION
    WHEN OTHERS THEN
        -- 스탠바이에서 쓰기 오류 무시
        NULL;
END;
$$;

CREATE EVENT TRIGGER login_auditor
    ON login
    EXECUTE FUNCTION audit_login();
```

---

### 4. 이벤트 트리거 생성

#### 4.1 기본 구문

```sql
CREATE EVENT TRIGGER trigger_name
    ON event_type
    [ WHEN filter_variable IN (filter_value [, ... ]) [ AND ... ] ]
    EXECUTE FUNCTION function_name();
```

#### 4.2 이벤트 트리거 함수 요구사항

1. 반환 타입이 `event_trigger`인 함수를 먼저 생성해야 합니다
2. 함수는 값을 반환할 필요가 없습니다 (반환 타입은 신호 역할만)
3. 동일 이벤트에 대한 여러 트리거는 트리거 이름의 알파벳 순서로 실행됩니다

#### 4.3 WHEN 조건 사용

특정 조건에서만 트리거를 실행하도록 필터링할 수 있습니다.

```sql
-- 예제: 특정 DDL 명령에만 반응하는 트리거
CREATE EVENT TRIGGER restrict_drop
    ON ddl_command_start
    WHEN TAG IN ('DROP TABLE', 'DROP SCHEMA', 'DROP INDEX')
    EXECUTE FUNCTION prevent_dangerous_drops();
```

#### 4.4 완전한 예제: DDL 제한 트리거

```sql
-- 1. 이벤트 트리거 함수 생성
CREATE OR REPLACE FUNCTION prevent_ddl_on_production()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    current_db text := current_database();
BEGIN
    -- 프로덕션 데이터베이스에서 DDL 차단
    IF current_db LIKE '%_prod' OR current_db LIKE '%_production' THEN
        IF NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
            RAISE EXCEPTION '프로덕션 환경에서 DDL 명령이 차단되었습니다: %', tg_tag;
        END IF;
    END IF;
END;
$$;

-- 2. 이벤트 트리거 생성
CREATE EVENT TRIGGER protect_production
    ON ddl_command_start
    EXECUTE FUNCTION prevent_ddl_on_production();
```

---

### 5. 이벤트 트리거 지원 함수

PostgreSQL은 이벤트 트리거 내에서 정보를 얻기 위한 여러 지원 함수를 제공합니다.

#### 5.1 pg_event_trigger_ddl_commands()

`ddl_command_end` 이벤트에서 실행된 DDL 명령 목록을 반환합니다.

##### 반환 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `classid` | `oid` | 객체가 속한 카탈로그의 OID |
| `objid` | `oid` | 객체 자체의 OID |
| `objsubid` | `integer` | 하위 객체 ID (예: 열의 속성 번호) |
| `command_tag` | `text` | 명령 태그 |
| `object_type` | `text` | 객체 타입 |
| `schema_name` | `text` | 객체가 속한 스키마 이름 (해당되는 경우) |
| `object_identity` | `text` | 스키마 한정 객체 식별자 |
| `in_extension` | `boolean` | 확장 스크립트의 일부인지 여부 |
| `command` | `pg_ddl_command` | 내부 형식의 완전한 명령 표현 |

```sql
-- 예제: DDL 명령 상세 정보 조회
CREATE OR REPLACE FUNCTION track_ddl_commands()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    cmd record;
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        INSERT INTO ddl_audit_log (
            command_tag,
            object_type,
            schema_name,
            object_identity,
            executed_by,
            executed_at
        ) VALUES (
            cmd.command_tag,
            cmd.object_type,
            cmd.schema_name,
            cmd.object_identity,
            current_user,
            current_timestamp
        );
    END LOOP;
END;
$$;
```

#### 5.2 pg_event_trigger_dropped_objects()

`sql_drop` 이벤트에서 삭제된 모든 객체 목록을 반환합니다.

##### 반환 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `classid` | `oid` | 객체가 속했던 카탈로그의 OID |
| `objid` | `oid` | 객체 자체의 OID |
| `objsubid` | `integer` | 하위 객체 ID |
| `original` | `boolean` | 삭제의 루트 객체인지 여부 |
| `normal` | `boolean` | 정상 의존성 관계가 있었는지 여부 |
| `is_temporary` | `boolean` | 임시 객체인지 여부 |
| `object_type` | `text` | 객체 타입 |
| `schema_name` | `text` | 객체가 속했던 스키마 이름 |
| `object_name` | `text` | 객체 이름 |
| `object_identity` | `text` | 스키마 한정 객체 식별자 |
| `address_names` | `text[]` | `pg_get_object_address`와 함께 사용할 배열 |
| `address_args` | `text[]` | `address_names`의 보완 배열 |

```sql
-- 예제: 삭제된 객체 감사
CREATE OR REPLACE FUNCTION audit_dropped_objects()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        -- 루트 객체만 기록 (의존성으로 삭제된 객체 제외)
        IF obj.original THEN
            INSERT INTO drop_audit_log (
                object_type,
                schema_name,
                object_name,
                object_identity,
                dropped_by,
                dropped_at
            ) VALUES (
                obj.object_type,
                obj.schema_name,
                obj.object_name,
                obj.object_identity,
                current_user,
                current_timestamp
            );
        END IF;
    END LOOP;
END;
$$;
```

#### 5.3 pg_event_trigger_table_rewrite_oid()

`table_rewrite` 이벤트에서 재작성될 테이블의 OID를 반환합니다.

```sql
SELECT pg_event_trigger_table_rewrite_oid()::regclass;
-- 결과: public.my_table
```

#### 5.4 pg_event_trigger_table_rewrite_reason()

테이블 재작성 이유를 비트맵 코드로 반환합니다.

| 비트 값 | 의미 |
|---------|------|
| 1 | 테이블의 지속성(persistence) 변경 |
| 2 | 열의 기본값 변경 |
| 4 | 열의 데이터 타입 변경 |
| 8 | 테이블 접근 방법(access method) 변경 |

```sql
-- 예제: 재작성 이유 분석
CREATE OR REPLACE FUNCTION analyze_rewrite_reason()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    reason integer := pg_event_trigger_table_rewrite_reason();
    table_name text := pg_event_trigger_table_rewrite_oid()::regclass::text;
    reasons text := '';
BEGIN
    IF reason & 1 > 0 THEN reasons := reasons || '지속성 변경, '; END IF;
    IF reason & 2 > 0 THEN reasons := reasons || '기본값 변경, '; END IF;
    IF reason & 4 > 0 THEN reasons := reasons || '데이터 타입 변경, '; END IF;
    IF reason & 8 > 0 THEN reasons := reasons || '접근 방법 변경, '; END IF;

    reasons := rtrim(reasons, ', ');

    RAISE NOTICE '테이블 %이(가) 다음 이유로 재작성됩니다: %', table_name, reasons;
END;
$$;
```

---

### 6. 이벤트 트리거 관리

#### 6.1 이벤트 트리거 조회

```sql
-- 모든 이벤트 트리거 조회
SELECT * FROM pg_event_trigger;

-- psql에서 이벤트 트리거 목록 보기
\dy

-- 상세 정보와 함께 조회
SELECT
    evtname AS "트리거 이름",
    evtevent AS "이벤트",
    evtenabled AS "상태",
    pg_get_userbyid(evtowner) AS "소유자",
    evtfoid::regproc AS "함수"
FROM pg_event_trigger
ORDER BY evtname;
```

#### 6.2 이벤트 트리거 활성화/비활성화

```sql
-- 트리거 비활성화
ALTER EVENT TRIGGER trigger_name DISABLE;

-- 트리거 활성화
ALTER EVENT TRIGGER trigger_name ENABLE;

-- 레플리카에서만 활성화
ALTER EVENT TRIGGER trigger_name ENABLE REPLICA;

-- 항상 활성화 (수퍼유저만)
ALTER EVENT TRIGGER trigger_name ENABLE ALWAYS;
```

#### 6.3 이벤트 트리거 이름 변경

```sql
ALTER EVENT TRIGGER old_name RENAME TO new_name;
```

#### 6.4 이벤트 트리거 소유자 변경

```sql
ALTER EVENT TRIGGER trigger_name OWNER TO new_owner;
```

#### 6.5 이벤트 트리거 삭제

```sql
-- 트리거 삭제
DROP EVENT TRIGGER trigger_name;

-- 존재하는 경우에만 삭제
DROP EVENT TRIGGER IF EXISTS trigger_name;

-- 의존 객체와 함께 삭제
DROP EVENT TRIGGER trigger_name CASCADE;
```

---

### 7. 실용적인 예제

#### 7.1 DDL 감사 로깅 시스템

```sql
-- 감사 로그 테이블 생성
CREATE TABLE ddl_audit_log (
    id SERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,
    command_tag TEXT NOT NULL,
    object_type TEXT,
    schema_name TEXT,
    object_identity TEXT,
    executed_by TEXT NOT NULL DEFAULT current_user,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    client_addr INET,
    application_name TEXT
);

-- DDL 감사 함수
CREATE OR REPLACE FUNCTION audit_ddl_changes()
RETURNS event_trigger
LANGUAGE plpgsql
SECURITY DEFINER AS $$
DECLARE
    cmd record;
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        INSERT INTO ddl_audit_log (
            event_type,
            command_tag,
            object_type,
            schema_name,
            object_identity,
            client_addr,
            application_name
        ) VALUES (
            TG_EVENT,
            cmd.command_tag,
            cmd.object_type,
            cmd.schema_name,
            cmd.object_identity,
            inet_client_addr(),
            current_setting('application_name', true)
        );
    END LOOP;
END;
$$;

-- 감사 트리거 생성
CREATE EVENT TRIGGER audit_ddl
    ON ddl_command_end
    EXECUTE FUNCTION audit_ddl_changes();
```

#### 7.2 테이블 재작성 정책 구현

```sql
-- 테이블 재작성 정책 함수
CREATE OR REPLACE FUNCTION enforce_rewrite_policy()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    table_oid oid := pg_event_trigger_table_rewrite_oid();
    table_name text := table_oid::regclass::text;
    current_hour integer := extract('hour' from current_time);
    table_pages integer;
    max_pages integer := 100;
BEGIN
    -- 특정 테이블은 재작성 금지
    IF table_name = 'public.critical_data' THEN
        RAISE EXCEPTION '테이블 %은(는) 재작성이 허용되지 않습니다', table_name;
    END IF;

    -- 테이블 크기 확인
    SELECT relpages INTO table_pages
    FROM pg_class
    WHERE oid = table_oid;

    -- 대용량 테이블은 유지보수 시간에만 재작성 허용
    IF table_pages > max_pages THEN
        IF current_hour NOT BETWEEN 2 AND 5 THEN
            RAISE EXCEPTION '% 페이지 이상의 테이블은 오전 2시~5시 사이에만 재작성 가능합니다 (현재 테이블: % 페이지)',
                            max_pages, table_pages;
        END IF;
    END IF;

    RAISE NOTICE '테이블 % 재작성 허용됨 (% 페이지)', table_name, table_pages;
END;
$$;

CREATE EVENT TRIGGER rewrite_policy
    ON table_rewrite
    EXECUTE FUNCTION enforce_rewrite_policy();
```

#### 7.3 특정 스키마 보호

```sql
-- 보호된 스키마에서 DDL 차단
CREATE OR REPLACE FUNCTION protect_schema()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    cmd record;
    protected_schemas text[] := ARRAY['core', 'audit', 'security'];
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        IF cmd.schema_name = ANY(protected_schemas) THEN
            -- 수퍼유저는 허용
            IF NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
                RAISE EXCEPTION '스키마 %는 보호되어 있습니다. DDL 명령 %이(가) 차단되었습니다.',
                                cmd.schema_name, cmd.command_tag;
            END IF;
        END IF;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER schema_protection
    ON ddl_command_end
    EXECUTE FUNCTION protect_schema();
```

#### 7.4 DROP 작업 방지

```sql
-- DROP 명령 차단 및 경고
CREATE OR REPLACE FUNCTION prevent_drops()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
    critical_objects text[] := ARRAY['users', 'orders', 'products', 'transactions'];
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        -- 특정 중요 테이블 보호
        IF obj.object_type = 'table' AND obj.object_name = ANY(critical_objects) THEN
            RAISE EXCEPTION '중요 테이블 %은(는) 삭제할 수 없습니다. DBA에게 문의하세요.',
                            obj.object_identity;
        END IF;

        -- 프로덕션 스키마의 객체 보호
        IF obj.schema_name = 'production' THEN
            RAISE EXCEPTION '프로덕션 스키마의 객체는 삭제할 수 없습니다: %',
                            obj.object_identity;
        END IF;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER drop_protection
    ON sql_drop
    EXECUTE FUNCTION prevent_drops();
```

---

### 8. C 언어로 이벤트 트리거 함수 작성

#### 8.1 EventTriggerData 구조체

C에서 이벤트 트리거 함수를 작성할 때 `EventTriggerData` 구조체를 사용합니다.

```c
typedef struct EventTriggerData
{
    NodeTag     type;       /* 항상 T_EventTriggerData */
    const char *event;      /* 이벤트 이름 */
    Node       *parsetree;  /* 명령의 파싱 트리 */
    CommandTag  tag;        /* 명령 태그 */
} EventTriggerData;
```

| 멤버 | 설명 |
|------|------|
| `type` | 항상 `T_EventTriggerData` |
| `event` | 이벤트 이름: `"login"`, `"ddl_command_start"`, `"ddl_command_end"`, `"sql_drop"`, `"table_rewrite"` |
| `parsetree` | 명령의 파싱 트리 포인터 (구조는 버전에 따라 변경될 수 있음) |
| `tag` | 이벤트와 연관된 명령 태그 (예: `"CREATE FUNCTION"`) |

#### 8.2 C 함수 예제: 모든 DDL 차단

```c
#include "postgres.h"
#include "commands/event_trigger.h"
#include "fmgr.h"

PG_MODULE_MAGIC;

PG_FUNCTION_INFO_V1(noddl);

Datum
noddl(PG_FUNCTION_ARGS)
{
    EventTriggerData *trigdata;

    /* 이벤트 트리거 관리자에 의해 호출되었는지 확인 */
    if (!CALLED_AS_EVENT_TRIGGER(fcinfo))
        elog(ERROR, "이벤트 트리거 관리자에 의해 호출되지 않았습니다");

    trigdata = (EventTriggerData *) fcinfo->context;

    /* 오류 발생시켜 DDL 차단 */
    ereport(ERROR,
            (errcode(ERRCODE_INSUFFICIENT_PRIVILEGE),
             errmsg("명령 \"%s\"이(가) 거부되었습니다",
                    GetCommandTagName(trigdata->tag))));

    PG_RETURN_NULL();
}
```

#### 8.3 C 함수 등록 및 트리거 생성

```sql
-- 함수 등록
CREATE FUNCTION noddl() RETURNS event_trigger
    AS 'noddl' LANGUAGE C;

-- 이벤트 트리거 생성
CREATE EVENT TRIGGER noddl ON ddl_command_start
    EXECUTE FUNCTION noddl();
```

#### 8.4 테스트

```sql
-- 이벤트 트리거 목록 확인
\dy

-- DDL 시도 (차단됨)
CREATE TABLE test_table(id serial);
-- 오류: 명령 "CREATE TABLE"이(가) 거부되었습니다

-- 트리거 일시 비활성화하여 작업 수행
BEGIN;
ALTER EVENT TRIGGER noddl DISABLE;
CREATE TABLE test_table(id serial);
ALTER EVENT TRIGGER noddl ENABLE;
COMMIT;
```

---

### 9. 주의사항 및 제한사항

#### 9.1 트랜잭션 중단 시 동작

- 이벤트 트리거는 중단된 트랜잭션에서 실행될 수 없습니다
- DDL 명령이 실패하면 `ddl_command_end` 트리거는 실행되지 않습니다
- `ddl_command_start` 트리거가 실패하면:
  - 추가 트리거가 실행되지 않음
  - 명령이 실행되지 않음
- `ddl_command_end` 트리거가 실패하면:
  - DDL 효과가 롤백됨

#### 9.2 트리거가 발생하지 않는 DDL 명령

다음 객체에 대한 DDL 명령은 이벤트 트리거를 발생시키지 않습니다:

- 공유 객체: 데이터베이스, 역할(Role), 테이블스페이스
- 매개변수 권한: `ALTER SYSTEM` 명령
- 이벤트 트리거 자체: 무한 재귀 방지

#### 9.3 성능 고려사항

```sql
-- 비효율적: 모든 DDL에 트리거 실행
CREATE EVENT TRIGGER slow_trigger
    ON ddl_command_end
    EXECUTE FUNCTION heavy_processing();

-- 효율적: WHEN 절로 필터링
CREATE EVENT TRIGGER fast_trigger
    ON ddl_command_end
    WHEN TAG IN ('CREATE TABLE', 'DROP TABLE')
    EXECUTE FUNCTION heavy_processing();
```

#### 9.4 권한 요구사항

- 이벤트 트리거를 생성하려면 수퍼유저 권한이 필요합니다
- 일반 사용자는 이벤트 트리거를 생성, 수정, 삭제할 수 없습니다

#### 9.5 이벤트 트리거 실행 순서

동일한 이벤트에 여러 트리거가 정의된 경우:
- 트리거 이름의 알파벳 순서로 실행됩니다
- 예: `a_trigger`가 `b_trigger`보다 먼저 실행

```sql
-- 실행 순서 제어를 위한 명명 규칙
CREATE EVENT TRIGGER aa_first_trigger ON ddl_command_start
    EXECUTE FUNCTION first_function();

CREATE EVENT TRIGGER zz_last_trigger ON ddl_command_start
    EXECUTE FUNCTION last_function();
```

#### 9.6 디버깅 팁

```sql
-- 이벤트 트리거 디버깅을 위한 로깅
CREATE OR REPLACE FUNCTION debug_event_trigger()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE LOG 'Event Trigger Debug: event=%, tag=%',
              TG_EVENT, TG_TAG;
END;
$$;

-- 로그 레벨 조정
SET client_min_messages = LOG;
```

---

### 참고 자료

- [PostgreSQL 공식 문서 - Event Triggers](https://www.postgresql.org/docs/current/event-triggers.html)
- [PostgreSQL 공식 문서 - Event Trigger Functions](https://www.postgresql.org/docs/current/functions-event-triggers.html)
- [CREATE EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-createeventtrigger.html)
- [ALTER EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-altereventtrigger.html)
- [DROP EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-dropeventtrigger.html)

---

## Chapter 39: 규칙 시스템 (The Rule System)

### 목차

1. [규칙 시스템 개요](#1-규칙-시스템-개요)
2. [쿼리 트리 (The Query Tree)](#2-쿼리-트리-the-query-tree)
3. [뷰와 규칙 시스템](#3-뷰와-규칙-시스템)
4. [구체화된 뷰 (Materialized Views)](#4-구체화된-뷰-materialized-views)
5. [INSERT, UPDATE, DELETE 규칙](#5-insert-update-delete-규칙)
6. [규칙과 권한](#6-규칙과-권한)
7. [규칙과 명령 상태](#7-규칙과-명령-상태)
8. [규칙 vs 트리거](#8-규칙-vs-트리거)

---

### 1. 규칙 시스템 개요

PostgreSQL의 규칙 시스템(Rule System)은 쿼리 재작성 규칙 시스템(Query Rewrite Rule System) 입니다. 이는 다른 데이터베이스 시스템에서 일반적으로 사용하는 저장 프로시저나 트리거와는 근본적으로 다른 접근 방식입니다.

#### 1.1 규칙 시스템의 작동 방식

규칙 시스템은 다음과 같이 작동합니다:

1. 쿼리 수정: 규칙을 반영하여 쿼리를 수정합니다.
2. 수정된 쿼리 전달: 수정된 쿼리를 쿼리 플래너에 전달하여 계획 및 실행합니다.

#### 1.2 규칙 시스템의 용도

규칙 시스템은 다음과 같은 용도로 사용됩니다:

- 쿼리 언어 프로시저: 복잡한 쿼리 변환
- 뷰 (Views): 가상 테이블 구현
- 버전 관리: 데이터 버전 처리
- 복잡한 쿼리 변환: 쿼리 최적화 및 재작성

#### 1.3 핵심 특징

| 특징 | 설명 |
|------|------|
| 쿼리 수준 작동 | 행 수준이 아닌 쿼리 수준에서 작동 |
| 재작성 기반 | 쿼리를 수정하거나 추가 쿼리 생성 |
| 뷰 구현 | PostgreSQL 뷰의 기반 메커니즘 |
| 최적화 가능 | 플래너가 전체 정보를 활용하여 최적화 |

---

### 2. 쿼리 트리 (The Query Tree)

쿼리 트리는 PostgreSQL 규칙 시스템에서 사용하는 SQL 문의 내부 표현입니다. 파서(Parser)와 플래너(Planner) 사이에 위치하며, 파싱된 쿼리와 사용자 정의 재작성 규칙을 0개 이상의 쿼리 트리로 변환합니다.

#### 2.1 쿼리 트리의 주요 구성 요소

##### 명령 유형 (Command Type)

쿼리 트리를 생성한 명령을 나타내는 단순 값입니다:

| 명령 유형 | 설명 |
|----------|------|
| `SELECT` | 데이터 조회 |
| `INSERT` | 데이터 삽입 |
| `UPDATE` | 데이터 갱신 |
| `DELETE` | 데이터 삭제 |

##### 범위 테이블 (Range Table)

쿼리에서 사용되는 릴레이션(테이블/뷰)의 목록입니다:

- `SELECT` 문에서는 `FROM` 키워드 뒤에 나열된 릴레이션
- 각 항목은 테이블/뷰와 쿼리 내 별칭을 식별
- 쿼리 트리 구조에서는 이름이 아닌 번호로 참조

##### 결과 릴레이션 (Result Relation)

쿼리 결과가 기록될 위치를 식별하는 범위 테이블의 인덱스입니다:

| 쿼리 유형 | 결과 릴레이션 |
|----------|--------------|
| `SELECT` | 결과 릴레이션 없음 |
| `INSERT`/`UPDATE`/`DELETE` | 수정되는 테이블 또는 뷰 |

##### 대상 목록 (Target List)

쿼리 결과를 정의하는 표현식 목록입니다:

| 쿼리 유형 | 대상 목록 내용 |
|----------|---------------|
| `SELECT` | `SELECT`와 `FROM` 키워드 사이의 표현식 |
| `INSERT` | `VALUES` 또는 `SELECT` 절의 표현식 |
| `UPDATE` | `SET column = expression` 부분의 표현식 |
| `DELETE` | CTID 항목(일반 테이블) 또는 전체 행 변수(뷰) |

##### 자격 조건 (Qualification)

`WHERE` 절에 해당하는 불리언 표현식으로, 작업 실행 여부를 결정합니다:

- 각 행에 대해 true/false로 평가
- 영향받는 행을 제어

##### 조인 트리 (Join Tree)

`FROM` 절의 구조를 나타냅니다:

- 조인 순서와 관계 표시
- `ON` 또는 `USING` 표현식의 제한 저장
- 최상위 `WHERE` 표현식을 자격 조건으로 포함

#### 2.2 쿼리 트리 확인 방법

쿼리 트리는 다음 설정 매개변수를 사용하여 서버 로그에 표시할 수 있습니다:

```sql
-- 파싱된 쿼리 트리 출력
SET debug_print_parse = on;

-- 재작성된 쿼리 트리 출력
SET debug_print_rewritten = on;

-- 실행 계획 트리 출력
SET debug_print_plan = on;
```

규칙 액션은 `pg_rewrite` 시스템 카탈로그에 쿼리 트리로 저장됩니다.

---

### 3. 뷰와 규칙 시스템

PostgreSQL에서 뷰는 규칙 시스템을 사용하여 구현됩니다. 뷰는 본질적으로 `ON SELECT DO INSTEAD` 규칙(관례적으로 `_RETURN`이라는 이름)이 있는 빈 테이블입니다.

#### 3.1 뷰 생성의 내부 동작

```sql
CREATE VIEW myview AS SELECT * FROM mytab;
```

이는 기능적으로 다음과 동등합니다:

```sql
CREATE TABLE myview (...);
CREATE RULE "_RETURN" AS ON SELECT TO myview DO INSTEAD
    SELECT * FROM mytab;
```

#### 3.2 SELECT 규칙의 작동 방식 (How SELECT Rules Work)

##### 핵심 특성

- 모든 쿼리에 마지막 단계로 적용됨
- 새 쿼리 트리를 생성하는 대신 쿼리 트리를 직접 수정
- `INSTEAD`로 표시된 하나의 무조건적 `SELECT` 액션으로 제한

##### 예제: 뷰 쿼리 재작성

다음과 같은 뷰와 쿼리가 있다고 가정합니다:

```sql
-- 뷰 정의
CREATE VIEW shoelace AS
    SELECT s.sl_name, s.sl_avail, s.sl_color, s.sl_len, s.sl_unit,
           s.sl_len * u.un_fact AS sl_len_cm
    FROM shoelace_data s, unit u
    WHERE s.sl_unit = u.un_name;

-- 뷰 쿼리
SELECT * FROM shoelace;
```

규칙 시스템이 이 쿼리를 처리하면:

1. `shoelace` 뷰의 `_RETURN` 규칙을 찾음
2. 규칙의 액션 쿼리로 서브쿼리 범위 테이블 항목을 생성
3. 원래 뷰 참조를 이 서브쿼리로 대체

재작성된 쿼리는 다음과 같이 됩니다:

```sql
SELECT shoelace.sl_name, shoelace.sl_avail, shoelace.sl_color,
       shoelace.sl_len, shoelace.sl_unit, shoelace.sl_len_cm
FROM (SELECT s.sl_name, s.sl_avail, s.sl_color, s.sl_len, s.sl_unit,
             s.sl_len * u.un_fact AS sl_len_cm
      FROM shoelace_data s, unit u
      WHERE s.sl_unit = u.un_name) shoelace;
```

#### 3.3 비-SELECT 문에서의 뷰 규칙 (View Rules in Non-SELECT Statements)

`INSERT`, `UPDATE`, `DELETE`, `MERGE` 명령의 경우:

- 쿼리 트리는 `SELECT` 트리와 거의 동일
- 결과 릴레이션이 결과가 저장될 위치를 가리킴
- 갱신/삭제되는 행을 식별하기 위한 특수 `CTID` (현재 튜플 ID) 항목 추가
- 실행기는 이 시스템 컬럼을 사용하여 원본 행을 찾음

#### 3.4 뷰의 강력함 (The Power of Views)

규칙 시스템은 쿼리 최적화 측면에서 다음과 같은 이점을 제공합니다:

- 플래너가 테이블, 관계, 자격 조건에 대한 모든 정보를 단일 쿼리 트리에서 받음
- 다른 뷰를 참조하는 복잡한 뷰는 계획 전에 완전히 확장됨
- 플래너가 완전한 정보로 최적의 결정을 내릴 수 있음

```sql
-- 예제: shoe_ready 뷰에 대한 간단한 SELECT
SELECT * FROM shoe_ready;

-- 내부적으로 4개 테이블의 조인으로 확장되어
-- 플래너에게 모든 정보가 제공됨
```

#### 3.5 뷰 업데이트 (Updating a View)

PostgreSQL은 세 가지 메커니즘을 통해 뷰 업데이트를 지원합니다 (평가 순서대로):

##### 1. INSTEAD 규칙 (가장 먼저 평가)

`INSERT`, `UPDATE`, `DELETE` 명령을 기본 테이블에 대한 작업으로 재작성하는 사용자 정의 규칙입니다.

```sql
-- 뷰에 대한 INSERT를 기본 테이블로 리다이렉트하는 규칙
CREATE RULE insert_to_myview AS ON INSERT TO myview
    DO INSTEAD INSERT INTO mytab VALUES (NEW.col1, NEW.col2);
```

##### 2. INSTEAD OF 트리거

- `INSERT`: 뷰가 결과 릴레이션으로 유지됨
- `UPDATE`/`DELETE`/`MERGE`: 뷰가 확장되고, 트리거에 이전 행 값을 제공하기 위해 `wholerow` 항목 추가

```sql
-- INSTEAD OF 트리거 예제
CREATE TRIGGER update_myview_trigger
    INSTEAD OF UPDATE ON myview
    FOR EACH ROW
    EXECUTE FUNCTION update_myview_func();
```

##### 3. 자동 업데이트 가능 뷰 (가장 나중에 평가)

단일 기본 릴레이션에서 선택하는 간단한 뷰는 자동으로 기본 테이블을 업데이트하도록 재작성될 수 있습니다.

중요: 규칙은 트리거보다 먼저 평가되며, 규칙이나 트리거가 존재하면 자동 재작성이 우회됩니다.

오류 처리: 이러한 메커니즘 중 어느 것도 적용되지 않으면 실행기가 뷰를 직접 업데이트할 수 없어 오류가 발생합니다.

---

### 4. 구체화된 뷰 (Materialized Views)

구체화된 뷰는 규칙 시스템을 사용하여 쿼리 결과를 테이블과 유사한 형태로 저장합니다. 가상적인 일반 뷰와 달리 데이터를 물리적으로 저장합니다.

#### 4.1 테이블 및 뷰와의 차이점

```sql
-- 구체화된 뷰 생성
CREATE MATERIALIZED VIEW mymatview AS SELECT * FROM mytab;

-- 일반 테이블 생성 (비교용)
CREATE TABLE mymatview AS SELECT * FROM mytab;
```

##### 주요 차이점

| 특성 | 구체화된 뷰 | 일반 테이블 |
|------|------------|------------|
| 직접 업데이트 | 불가능 | 가능 |
| 쿼리 저장 | 저장됨 | 저장되지 않음 |
| 새로고침 | `REFRESH` 명령으로 가능 | 수동 갱신 필요 |

#### 4.2 작동 방식

- PostgreSQL 시스템 카탈로그에서 구체화된 뷰는 테이블이나 뷰와 같은 릴레이션
- 쿼리 시 구체화된 뷰에서 직접 데이터 반환 (테이블처럼)
- 규칙은 뷰를 채우는 데만 사용되며, 접근에는 사용되지 않음

#### 4.3 실용적인 예제: 판매 요약

```sql
-- 원본 테이블
CREATE TABLE invoice (
    invoice_no    integer        PRIMARY KEY,
    seller_no     integer,
    invoice_date  date,
    invoice_amt   numeric(13,2)
);

-- 구체화된 뷰 생성
CREATE MATERIALIZED VIEW sales_summary AS
  SELECT
      seller_no,
      invoice_date,
      sum(invoice_amt)::numeric(13,2) as sales_amt
    FROM invoice
    WHERE invoice_date < CURRENT_DATE
    GROUP BY seller_no, invoice_date;

-- 인덱스 생성 (구체화된 뷰의 장점)
CREATE UNIQUE INDEX sales_summary_seller
  ON sales_summary (seller_no, invoice_date);

-- 매일 밤 새로고침
REFRESH MATERIALIZED VIEW sales_summary;
```

#### 4.4 사용 사례

| 사용 사례 | 설명 |
|----------|------|
| 과거 데이터 분석 | 현재 완전성 없이 과거 데이터 요약 |
| 원격 데이터 캐싱 | 외부 데이터 래퍼의 데이터에 대한 빠른 로컬 접근 |
| 인덱스 지원 | 일반 뷰와 달리 구체화된 뷰에 인덱스 생성 가능 |

#### 4.5 성능 이점

구체화된 뷰와 인덱스를 사용하면 외부 데이터 래퍼를 통해 원격 데이터에 직접 접근하는 것에 비해 극적인 성능 향상을 얻을 수 있습니다 (예: 188ms vs 0.117ms).

---

### 5. INSERT, UPDATE, DELETE 규칙

`INSERT`, `UPDATE`, `DELETE`에 대한 규칙은 뷰 규칙과 크게 다릅니다.

#### 5.1 뷰 규칙과의 주요 차이점

| 특성 | 뷰 규칙 | UPDATE 규칙 |
|------|--------|-------------|
| 액션 수 | 하나의 `SELECT` 액션 | 없음, 하나, 또는 여러 개 |
| 실행 방식 | `INSTEAD` 전용 | `INSTEAD` 또는 `ALSO` |
| 의사 릴레이션 | 없음 | `NEW`와 `OLD` 지원 |
| 규칙 자격 조건 | 없음 | 지원됨 |
| 쿼리 트리 처리 | 기존 트리 수정 | 새 쿼리 트리 생성 |

#### 5.2 중요 주의사항

> 권장사항: 많은 작업에 대해 규칙 대신 트리거 사용을 권장합니다.

규칙보다 트리거가 권장되는 이유:

- 트리거가 더 단순하고 예측 가능한 의미론을 가짐
- 휘발성 함수(volatile functions)가 있는 규칙은 예상치 못하게 여러 번 실행될 수 있음
- 규칙은 `WITH` 절이나 `UPDATE` 쿼리의 다중 할당 서브-`SELECT`와 같은 일부 구문을 지원하지 않음

#### 5.3 UPDATE 규칙 구문

```sql
CREATE [ OR REPLACE ] RULE name AS ON event
    TO table [ WHERE condition ]
    DO [ ALSO | INSTEAD ] { NOTHING | command | ( command ; ... ) }
```

여기서 `event`는 `INSERT`, `UPDATE`, `DELETE` 중 하나입니다.

#### 5.4 실행 순서

| 규칙 유형 | 실행 순서 |
|----------|----------|
| `ON INSERT` | 원래 쿼리가 규칙 액션 전에 실행 |
| `ON UPDATE`/`ON DELETE` | 규칙 액션이 원래 쿼리 전에 실행 |

이 순서는 액션이 수정되는 행을 볼 수 있도록 보장합니다.

#### 5.5 일반적인 사용 사례

##### 뷰 보호 규칙

```sql
-- 뷰에 대한 INSERT를 차단하는 규칙
CREATE RULE shoe_ins_protect AS ON INSERT TO shoe
    DO INSTEAD NOTHING;

CREATE RULE shoe_upd_protect AS ON UPDATE TO shoe
    DO INSTEAD NOTHING;

CREATE RULE shoe_del_protect AS ON DELETE TO shoe
    DO INSTEAD NOTHING;
```

##### 실제 테이블로 작업 리다이렉트

```sql
-- shoelace 뷰에 대한 INSERT를 shoelace_data 테이블로 리다이렉트
CREATE RULE shoelace_ins AS ON INSERT TO shoelace
    DO INSTEAD
    INSERT INTO shoelace_data VALUES (
        NEW.sl_name,
        NEW.sl_avail,
        NEW.sl_color,
        NEW.sl_len,
        NEW.sl_unit
    );

-- UPDATE 규칙
CREATE RULE shoelace_upd AS ON UPDATE TO shoelace
    DO INSTEAD
    UPDATE shoelace_data
       SET sl_name = NEW.sl_name,
           sl_avail = NEW.sl_avail,
           sl_color = NEW.sl_color,
           sl_len = NEW.sl_len,
           sl_unit = NEW.sl_unit
     WHERE sl_name = OLD.sl_name;

-- DELETE 규칙
CREATE RULE shoelace_del AS ON DELETE TO shoelace
    DO INSTEAD
    DELETE FROM shoelace_data
     WHERE sl_name = OLD.sl_name;
```

##### 조건부 로깅

```sql
-- 재고량이 변경된 경우에만 로그 기록
CREATE TABLE shoelace_log (
    sl_name    text,
    sl_avail   integer,
    log_who    text,
    log_when   timestamp
);

CREATE RULE log_shoelace AS ON UPDATE TO shoelace_data
    WHERE NEW.sl_avail <> OLD.sl_avail
    DO INSERT INTO shoelace_log VALUES (
        NEW.sl_name,
        NEW.sl_avail,
        current_user,
        current_timestamp
    );
```

#### 5.6 컬럼 참조

| 의사 릴레이션 | 설명 |
|--------------|------|
| `NEW` | 새 행의 컬럼 참조 (`INSERT`/`UPDATE`) |
| `OLD` | 이전 행의 컬럼 참조 (`UPDATE`/`DELETE`) |

- 일치하지 않는 `NEW` 참조는 `UPDATE`에서 `OLD`로, `INSERT`에서 NULL로 변환
- 일치하지 않는 `OLD` 참조는 NULL로 변환

#### 5.7 복잡한 규칙 예제

```sql
-- 여러 테이블에 영향을 미치는 규칙
CREATE RULE delete_computer AS ON DELETE TO computer
    DO ALSO
    DELETE FROM software WHERE hostname = OLD.hostname;

-- 조건부 INSTEAD 규칙
CREATE RULE update_special AS ON UPDATE TO products
    WHERE OLD.category = 'special'
    DO INSTEAD
    UPDATE special_products
       SET name = NEW.name, price = NEW.price
     WHERE id = OLD.id;
```

---

### 6. 규칙과 권한

규칙에 의해 쿼리가 재작성되면 원래 쿼리에 명시적으로 지정된 것 외의 테이블/뷰에도 접근하게 됩니다.

#### 6.1 규칙 소유권과 접근 제어

##### 핵심 개념

| 개념 | 설명 |
|------|------|
| 별도 소유자 없음 | 재작성 규칙은 자동으로 릴레이션(테이블/뷰) 소유자가 소유 |
| 권한 검사 | 보안 호출자 뷰의 `SELECT` 규칙을 제외하고, 규칙을 통해 접근되는 모든 릴레이션은 호출 사용자가 아닌 규칙 소유자의 권한으로 검사됨 |
| 사용자 이점 | 사용자는 쿼리에 명시적으로 이름이 지정된 테이블/뷰에 대한 권한만 필요 |

#### 6.2 실용적인 예제

```sql
-- 기본 테이블 생성
CREATE TABLE phone_data (person text, phone text, private boolean);

-- 뷰 생성 (private 번호는 숨김)
CREATE VIEW phone_number AS
    SELECT person, CASE WHEN NOT private THEN phone END AS phone
    FROM phone_data;

-- assistant 역할에 뷰 접근 권한 부여
GRANT SELECT ON phone_number TO assistant;
```

이 예제에서:
- 테이블 소유자만 `phone_data`에 직접 접근 가능
- `assistant`는 권한 부여를 통해 `phone_number` 뷰 쿼리 가능
- 규칙이 쿼리를 재작성하고, 권한 검사는 소유자의 권한 사용
- `assistant`는 `phone_data`에 직접 접근 불가

#### 6.3 보안 고려사항

##### security_barrier 없는 취약점

표준 뷰는 함수 실행 순서 조작을 통해 데이터를 유출할 수 있습니다:

```sql
-- 안전하지 않은 뷰
CREATE VIEW phone_number AS
    SELECT person, phone FROM phone_data WHERE phone NOT LIKE '412%';

-- 공격: 사용자 정의 함수가 WHERE 절 전에 실행
CREATE FUNCTION tricky(text, text) RETURNS bool AS $$
BEGIN
    RAISE NOTICE '% => %', $1, $2;  -- 모든 데이터가 여기서 노출됨
    RETURN true;
END;
$$ LANGUAGE plpgsql COST 0.0000000000000000000001;

SELECT * FROM phone_number WHERE tricky(person, phone);
-- 모든 데이터가 NOTICE 메시지를 통해 노출됨
```

##### 해결책: security_barrier 속성

```sql
-- 보안 뷰
CREATE VIEW phone_number WITH (security_barrier) AS
    SELECT person, phone FROM phone_data WHERE phone NOT LIKE '412%';
```

이점:
- 함수/연산자가 보이지 않는 행에 접근하는 것을 방지
- 행 필터가 함수 호출 전에 실행되도록 보장

주의: 성능에 영향을 미칠 수 있음

##### LEAKPROOF 함수

- 쿼리 플래너는 leakproof 함수(부작용이 없는 함수)에 대해 유연성을 가짐
- 예: 동등 연산자, 단순 내장 함수
- Leakproof 함수는 보안을 손상시키지 않고 어느 시점에서든 안전하게 평가될 수 있음

#### 6.4 중요한 제한사항

`security_barrier`를 사용하더라도 뷰는 다음에 대해 완전히 안전하지 않습니다:

- `EXPLAIN`을 통한 쿼리 계획 분석
- 타이밍 공격 (쿼리 실행 시간 측정)
- 데이터 분포에 대한 통계적 추론
- 은닉 채널 공격

매우 민감한 데이터의 경우 뷰에만 의존하기보다 접근 자체를 제한하는 것을 고려하세요.

---

### 7. 규칙과 명령 상태

PostgreSQL이 쿼리를 실행할 때 명령 상태 문자열(예: `INSERT 149592 1`)을 반환합니다. 규칙은 어떤 명령 상태가 반환되는지에 영향을 미칠 수 있습니다.

#### 7.1 규칙이 명령 상태에 미치는 영향

##### 사례 1: 무조건적 INSTEAD 규칙이 없는 경우

- 원래 쿼리가 실행되고 평소처럼 명령 상태 반환
- 예외: 조건부 `INSTEAD` 규칙이 있으면 부정된 자격 조건이 원래 쿼리에 추가되어 상태에 반영된 행 수가 줄어들 수 있음

##### 사례 2: 무조건적 INSTEAD 규칙이 있는 경우

- 원래 쿼리가 실행되지 않음
- 서버는 다음 조건을 만족하는 마지막 `INSTEAD` 규칙의 명령 상태를 반환:
  - 원래 쿼리와 동일한 명령 유형 (`INSERT`, `UPDATE`, `DELETE`)
  - 규칙에 의해 실제로 삽입/실행됨
- 일치하는 규칙 쿼리가 추가되지 않으면, 상태는 원래 쿼리 유형과 함께 행 수 및 OID 필드에 0을 표시

#### 7.2 예측 가능한 명령 상태를 위한 모범 사례

특정 `INSTEAD` 규칙이 명령 상태를 설정하도록 보장하려면:

> 활성 규칙들 사이에서 알파벳 순으로 마지막이 되는 규칙 이름을 부여하여 마지막에 적용되도록 합니다.

```sql
-- 예: 'z_' 접두사를 사용하여 마지막에 실행되도록
CREATE RULE z_final_insert AS ON INSERT TO myview
    DO INSTEAD
    INSERT INTO mytable VALUES (NEW.col1, NEW.col2);
```

---

### 8. 규칙 vs 트리거

규칙과 트리거는 모두 데이터베이스에서 자동화된 동작을 구현하지만, 서로 다른 수준에서 작동합니다.

#### 8.1 주요 차이점

| 특성 | 트리거 | 규칙 |
|------|--------|------|
| 실행 수준 | 영향받는 각 행마다 한 번 실행 | 쿼리 수준에서 작동 |
| 개념적 복잡도 | 더 단순하고 초보자에게 쉬움 | 더 복잡하고 학습 곡선이 있음 |
| 작동 방식 | 행별 트리거 함수 호출 | 쿼리 수정 또는 추가 쿼리 생성 |
| 효율성 (대량 작업) | 행 수에 비례하여 작업량 증가 | 행 수와 무관하게 하나의 쿼리 |

#### 8.2 사용 시기

##### 트리거 사용이 적합한 경우

| 사용 사례 | 이유 |
|----------|------|
| 제약 조건 및 외래 키 | 규칙은 데이터가 조용히 버려지므로 제약 조건을 제대로 구현할 수 없음 |
| 복잡한 로직 | 명시적 오류 처리 및 유효성 검사가 필요할 때 |
| 단순성 | 로직이 간단할 때 |
| 뷰 업데이트 | `INSTEAD OF` 트리거가 규칙보다 쉬움 |

```sql
-- 트리거가 적합한 예: 데이터 유효성 검사
CREATE FUNCTION validate_order() RETURNS trigger AS $$
BEGIN
    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'Quantity must be positive';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_order
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION validate_order();
```

##### 규칙 사용이 적합한 경우

| 사용 사례 | 이유 |
|----------|------|
| 대량 작업 | 단일 문이 많은 행에 영향을 미칠 때, 하나의 추가 명령을 발행하는 규칙이 일반적으로 더 빠름 |
| 쿼리 수준 변환 | 전체 쿼리를 다른 형태로 변환해야 할 때 |

```sql
-- 규칙이 적합한 예: 로그 테이블로 모든 변경 복제
CREATE RULE log_changes AS ON UPDATE TO products
    DO ALSO
    INSERT INTO products_changelog
    SELECT 'UPDATE', current_timestamp, OLD.*, NEW.*;
```

#### 8.3 성능 비교 예제

`computer`와 `software` 테이블 간의 캐스케이딩 삭제 시나리오:

##### 단일 행 삭제

두 접근 방식 모두 인덱스 스캔으로 비슷하게 수행됩니다.

```sql
-- 트리거 접근
DELETE FROM computer WHERE hostname = 'server1';
-- 트리거가 software 테이블에서 관련 행 삭제

-- 규칙 접근
DELETE FROM computer WHERE hostname = 'server1';
-- 규칙이 software 삭제 쿼리 추가
```

##### 대량 삭제 (예: 2000행)

| 접근 방식 | 동작 | 성능 |
|----------|------|------|
| 트리거 | 2000개의 개별 명령을 실행기를 통해 실행, 각각 인덱스 스캔 수행 | 느림 |
| 규칙 | 조인을 사용하는 하나의 최적화된 명령 생성, 영향받는 행 수와 무관 | 빠름 |

```sql
-- 규칙이 생성하는 최적화된 쿼리 예시
DELETE FROM software
 WHERE hostname IN (SELECT hostname FROM computer WHERE condition);
```

#### 8.4 요약

| 고려사항 | 권장사항 |
|---------|---------|
| 제약 조건 필요 | 트리거 사용 |
| 대량 작업 속도 필요 | 규칙 사용 |
| 뷰 업데이트 | `INSTEAD OF` 트리거 사용 (규칙보다 쉬움) |
| 단순한 로직 | 트리거 사용 |
| 복잡한 쿼리 변환 | 규칙 사용 |

> 규칙이 트리거보다 현저히 느린 경우는, 규칙 액션이 조건이 불명확한 대규모 조인을 생성하여 플래너가 좋은 계획을 수립하지 못할 때뿐입니다.

---

### 요약

PostgreSQL의 규칙 시스템은 쿼리 재작성을 통해 강력한 기능을 제공합니다:

| 기능 | 설명 |
|------|------|
| 쿼리 재작성 | 파서와 플래너 사이에서 쿼리를 변환 |
| 뷰 구현 | 규칙 시스템의 핵심 사용 사례 |
| 구체화된 뷰 | 쿼리 결과를 물리적으로 저장 |
| 데이터 수정 규칙 | INSERT, UPDATE, DELETE에 대한 사용자 정의 동작 |
| 보안 | security_barrier를 통한 행 수준 보안 |
| 성능 최적화 | 대량 작업에서 트리거보다 효율적일 수 있음 |

규칙 시스템을 잘 활용하면 복잡한 데이터베이스 로직을 쿼리 수준에서 처리할 수 있습니다. 그러나 대부분의 일반적인 사용 사례에서는 트리거가 더 단순하고 예측 가능한 선택입니다.

---

### 참고 자료

- [PostgreSQL 공식 문서 - The Rule System](https://www.postgresql.org/docs/current/rules.html)
- [PostgreSQL 공식 문서 - The Query Tree](https://www.postgresql.org/docs/current/querytree.html)
- [PostgreSQL 공식 문서 - Views and the Rule System](https://www.postgresql.org/docs/current/rules-views.html)
- [PostgreSQL 공식 문서 - Rules on INSERT, UPDATE, DELETE](https://www.postgresql.org/docs/current/rules-update.html)
- [PostgreSQL 공식 문서 - Rules vs Triggers](https://www.postgresql.org/docs/current/rules-triggers.html)

---

## PostgreSQL 절차적 언어 (Procedural Languages)

### 목차
1. [개요](#1-개요)
2. [절차적 언어 설치](#2-절차적-언어-설치)
3. [PL/pgSQL](#3-plpgsql)
4. [PL/Tcl](#4-pltcl)
5. [PL/Perl](#5-plperl)
6. [PL/Python](#6-plpython)
7. [절차적 언어 보안](#7-절차적-언어-보안)

---

### 1. 개요

#### 1.1 절차적 언어란?

PostgreSQL은 SQL과 C 외에도 다른 언어로 사용자 정의 함수(User-Defined Functions)를 작성할 수 있습니다. 이러한 언어들을 일반적으로 절차적 언어(Procedural Languages, PL) 라고 합니다.

절차적 언어로 작성된 함수의 경우, 데이터베이스 서버는 함수의 소스 텍스트를 해석하는 방법에 대한 기본 제공 지식이 없습니다. 대신 이 작업은 해당 언어의 세부 사항을 알고 있는 특수한 핸들러(Handler) 에게 전달됩니다.

#### 1.2 핸들러의 작동 방식

핸들러는 다음과 같이 작동합니다:

- 직접 처리: 파싱(Parsing), 구문 분석(Syntax Analysis), 실행(Execution) 등 모든 작업을 수행
- 중개 역할: PostgreSQL과 기존 프로그래밍 언어 구현 간의 "접착제(Glue)" 역할

핸들러 자체는 공유 객체(Shared Object)로 컴파일되고 필요 시 로드되는 C 언어 함수입니다.

#### 1.3 표준 배포판의 절차적 언어

PostgreSQL 표준 배포판에는 현재 4가지 절차적 언어가 포함되어 있습니다:

| 언어 | 설명 | 신뢰 여부 |
|------|------|-----------|
| PL/pgSQL | SQL 절차적 언어 | Trusted |
| PL/Tcl | Tcl 절차적 언어 | Trusted / Untrusted (PL/TclU) |
| PL/Perl | Perl 절차적 언어 | Trusted / Untrusted (PL/PerlU) |
| PL/Python | Python 절차적 언어 | Untrusted (PL/PythonU) |

> 참고: 핵심 배포판에 포함되지 않은 추가 절차적 언어는 PostgreSQL의 External Projects에서 찾을 수 있습니다.

---

### 2. 절차적 언어 설치

#### 2.1 기본 설치 규칙

절차적 언어는 사용할 각 데이터베이스에 설치해야 합니다. `template1` 데이터베이스에 설치된 언어는 이후에 생성되는 모든 데이터베이스에서 자동으로 사용할 수 있습니다.

#### 2.2 간편 설치 방법 (권장)

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

#### 2.3 수동 설치 방법

확장(Extension)으로 패키지되지 않은 언어의 경우, 다음 5단계를 수행해야 합니다. 이 작업은 데이터베이스 슈퍼유저(Superuser)만 수행할 수 있습니다.

##### 단계 1: 공유 객체 컴파일 및 설치

언어 핸들러를 공유 객체로 컴파일하고 적절한 라이브러리 디렉토리에 설치합니다.

##### 단계 2: 핸들러 함수 선언

```sql
CREATE FUNCTION handler_function_name()
    RETURNS language_handler
    AS 'path-to-shared-object'
    LANGUAGE C;
```

##### 단계 3: 인라인 핸들러 선언 (선택 사항)

익명 코드 블록(`DO` 명령)을 실행하기 위해 필요합니다:

```sql
CREATE FUNCTION inline_function_name(internal)
    RETURNS void
    AS 'path-to-shared-object'
    LANGUAGE C;
```

##### 단계 4: 유효성 검사 함수 선언 (선택 사항)

함수 정의를 실행하지 않고 검사하기 위해 필요합니다:

```sql
CREATE FUNCTION validator_function_name(oid)
    RETURNS void
    AS 'path-to-shared-object'
    LANGUAGE C STRICT;
```

##### 단계 5: 언어 생성

```sql
CREATE [TRUSTED] LANGUAGE language_name
    HANDLER handler_function_name
    [INLINE inline_function_name]
    [VALIDATOR validator_function_name];
```

#### 2.4 수동 설치 예제: PL/Perl

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

#### 2.5 기본 설치 상태

| 언어 | 기본 설치 여부 |
|------|----------------|
| PL/pgSQL | 예 (모든 데이터베이스) |
| PL/Tcl, PL/TclU | 아니오 (설정 시) |
| PL/Perl, PL/PerlU | 아니오 (설정 시) |
| PL/PythonU | 아니오 (설정 시) |

---

### 3. PL/pgSQL

#### 3.1 개요

PL/pgSQL은 PostgreSQL 데이터베이스 시스템을 위한 로드 가능한 절차적 언어입니다. PostgreSQL 9.0 이상에서는 기본적으로 설치됩니다.

##### 설계 목표

- 함수(Function), 프로시저(Procedure), 트리거(Trigger) 생성
- SQL 언어에 제어 구조 추가
- 복잡한 계산 수행
- 모든 사용자 정의 타입, 함수, 프로시저, 연산자 상속
- 서버에서 신뢰할 수 있도록 정의
- 사용하기 쉬움

#### 3.2 PL/pgSQL 사용의 장점

##### 성능 개선

문제점: SQL은 각 문장이 개별적으로 데이터베이스 서버에서 실행되어야 합니다. 클라이언트 애플리케이션이 각 쿼리를 전송, 처리 대기, 결과 수신, 처리 후 다시 서버로 쿼리를 보내야 하므로 프로세스 간 통신과 네트워크 오버헤드가 발생합니다.

PL/pgSQL의 해결책:
- 계산 블록과 일련의 쿼리를 데이터베이스 서버 내부에 그룹화
- 클라이언트와 서버 간 추가 왕복(Round-trip) 제거
- 중간 결과의 마샬링/전송 불필요
- 여러 번의 쿼리 파싱 회피

#### 3.3 기본 구조 (Block Structure)

PL/pgSQL은 블록 구조 언어입니다. 함수 본문은 반드시 블록이어야 합니다:

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
END [ label ];
```

##### 기본 예제

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

##### 중첩 블록과 변수 스코핑 예제

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

#### 3.4 변수 선언 (Declarations)

##### 기본 문법

```sql
name [ CONSTANT ] type [ COLLATE collation_name ] [ NOT NULL ] [ { DEFAULT | := | = } expression ];
```

##### 변수 선언 예제

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

##### 함수 매개변수 사용

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

##### %TYPE을 사용한 타입 복사

```sql
DECLARE
    user_id users.user_id%TYPE;
    user_ids users.user_id%TYPE[];
```

##### %ROWTYPE을 사용한 행 타입

```sql
DECLARE
    myrow tablename%ROWTYPE;
```

##### RECORD 타입

```sql
DECLARE
    arow RECORD;
```

#### 3.5 표현식 (Expressions)

모든 PL/pgSQL 표현식은 메인 SQL 실행기를 통해 처리됩니다. 표현식은 내부적으로 `SELECT` 명령으로 변환됩니다.

```sql
-- 예: IF 문의 표현식
IF x < y THEN ...

-- 내부적으로 다음과 같이 변환됨:
-- SELECT $1 < $2
```

#### 3.6 제어 구조 (Control Structures)

##### 조건문 (Conditionals)

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

##### 반복문 (Loops)

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

#### 3.7 오류 처리 (Exception Handling)

##### 기본 문법

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

##### 예외 처리 예제

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

##### 오류 정보 얻기

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
| `PG_EXCEPTION_CONTEXT` | 예외 발생 시점의 콜 스택 |
| `PG_DATATYPE_NAME` | 관련 데이터 타입 |
| `COLUMN_NAME` | 관련 컬럼 |
| `CONSTRAINT_NAME` | 관련 제약 조건 |
| `TABLE_NAME` | 관련 테이블 |
| `SCHEMA_NAME` | 관련 스키마 |

#### 3.8 지원되는 데이터 타입

| 카테고리 | 설명 |
|----------|------|
| 스칼라 타입 | 서버가 지원하는 모든 스칼라 데이터 타입 |
| 배열 타입 | 서버가 지원하는 모든 배열 데이터 타입 |
| 복합 타입 | 이름으로 지정된 모든 복합 타입(행 타입) |
| RECORD | 모든 복합 타입 수용 (입력/출력) |

---

### 4. PL/Tcl

#### 4.1 개요

PL/Tcl 은 PostgreSQL에서 [Tcl 언어](https://www.tcl.tk/)를 사용하여 함수와 프로시저를 작성할 수 있게 해주는 로드 가능한 절차적 언어입니다.

##### 주요 특징

- C 언어 함수 작성자가 가지는 대부분의 기능을 제공
- 몇 가지 제한 사항 존재
- Tcl의 강력한 문자열 처리 라이브러리 사용 가능

##### 안전성과 제한 사항

- 모든 것이 안전한 Tcl 인터프리터 컨텍스트 내에서 실행
- 안전한 Tcl 명령 세트로 제한
- SPI를 통한 데이터베이스 접근과 `elog()`를 통한 메시지 출력만 가능
- 데이터베이스 서버 내부나 OS 수준 접근 불가
- 권한 없는 데이터베이스 사용자도 안전하게 사용 가능

#### 4.2 설치

```sql
-- Trusted PL/Tcl
CREATE EXTENSION pltcl;

-- Untrusted PL/TclU
CREATE EXTENSION pltclu;
```

#### 4.3 함수 생성 및 인수

##### 기본 문법

```tcl
CREATE FUNCTION funcname (argument-types) RETURNS return-type AS $$
    # PL/Tcl function body
$$ LANGUAGE pltcl;
```

##### 예제: STRICT 절이 있는 간단한 함수

```tcl
CREATE FUNCTION tcl_max(integer, integer) RETURNS integer AS $$
    if {$1 > $2} {return $1}
    return $2
$$ LANGUAGE pltcl STRICT;
```

##### 예제: NULL 값 처리

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

##### 예제: 복합 타입 인수

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

##### 예제: 복합 타입 반환

```tcl
CREATE FUNCTION square_cube(in int, out squared int, out cubed int) AS $$
    return [list squared [expr {$1 * $1}] cubed [expr {$1 * $1 * $1}]]
$$ LANGUAGE pltcl;
```

##### 예제: 집합 반환 함수

```tcl
CREATE FUNCTION sequence(int, int) RETURNS SETOF int AS $$
    for {set i $1} {$i < $2} {incr i} {
        return_next $i
    }
$$ LANGUAGE pltcl;
```

#### 4.4 주요 함수

| 함수 | 용도 |
|------|------|
| `argisnull n` | 인수 n이 NULL인지 확인 |
| `return_null` | NULL 값 반환 |
| `return_next value` | 집합 반환 함수에서 행 반환 |

#### 4.5 PL/TclU (Untrusted)

제한 없는 Tcl 함수가 필요한 경우(예: 이메일 전송)에 사용합니다.

- 안전한 Tcl 대신 전체 Tcl 인터프리터 사용
- 신뢰할 수 없는 절차적 언어로 설치해야 함
- 데이터베이스 슈퍼유저만 함수 생성 가능
- 개발자는 함수가 오용되지 않도록 보장해야 함

---

### 5. PL/Perl

#### 5.1 개요

PL/Perl 은 PostgreSQL 함수와 프로시저를 Perl 프로그래밍 언어로 작성할 수 있게 해주는 로드 가능한 절차적 언어입니다.

##### 주요 장점

- Perl의 광범위한 "문자열 처리(String Munging)" 연산자와 함수 사용 가능
- 복잡한 문자열 파싱이 PL/pgSQL보다 훨씬 쉬움

#### 5.2 설치

```sql
-- Trusted PL/Perl
CREATE EXTENSION plperl;

-- Untrusted PL/PerlU
CREATE EXTENSION plperlu;
```

> 참고: 소스 패키지에서는 설치 과정 중 PL/Perl을 특별히 활성화해야 합니다. 바이너리 패키지에서는 별도의 서브패키지에 포함될 수 있습니다.

#### 5.3 함수 생성 문법

```perl
CREATE FUNCTION funcname (argument-types)
RETURNS return-type
-- function attributes can go here
AS $$
    # PL/Perl function body goes here
$$ LANGUAGE plperl;
```

#### 5.4 예제

##### 기본 함수

```perl
CREATE FUNCTION perl_max (integer, integer) RETURNS integer AS $$
    if ($_[0] > $_[1]) { return $_[0]; }
    return $_[1];
$$ LANGUAGE plperl;
```

##### NULL 값 처리

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

##### 익명 코드 블록

```perl
DO $$
    # PL/Perl code
$$ LANGUAGE plperl;
```

##### Boolean 값 처리

```sql
CREATE EXTENSION bool_plperl;

CREATE FUNCTION perl_and(bool, bool) RETURNS bool
TRANSFORM FOR TYPE bool
AS $$
  my ($a, $b) = @_;
  return $a && $b;
$$ LANGUAGE plperl;
```

##### 배열 처리

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

##### 복합 타입

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

##### 복합 타입 반환

```perl
CREATE TYPE testrowperl AS (f1 integer, f2 text, f3 text);

CREATE OR REPLACE FUNCTION perl_row() RETURNS testrowperl AS $$
    return {f2 => 'hello', f1 => 1, f3 => 'world'};
$$ LANGUAGE plperl;
```

##### 집합 반환 함수

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

#### 5.5 Strict 모드

```perl
use strict;
```

또는 전역 설정:

```sql
SET plperl.use_strict = true;
```

#### 5.6 주의 사항

- 명명된 중첩 서브루틴은 위험 - 대신 익명 서브루틴 사용
- 문자열 상수 필요 - 함수 본문에 달러 인용(`$$..$$`) 사용
- 인코딩 자동 - UTF-8 변환이 투명하게 처리됨
- 다차원 배열 - 하위 차원 배열에 대한 참조로 표현

---

### 6. PL/Python

#### 6.1 개요

PL/Python 은 PostgreSQL 함수와 프로시저를 Python으로 작성할 수 있게 해주는 절차적 언어입니다.

##### 중요 사항

- 신뢰할 수 없는(Untrusted) 언어로만 사용 가능 (`plpython3u`)
- 사용자 작업을 제한하지 않으므로 "신뢰할 수 없음"으로 명명
- 데이터베이스 관리자가 사용할 수 있는 시스템 기능에 완전히 접근 가능
- 슈퍼유저만 함수 생성 가능

#### 6.2 설치

```sql
CREATE EXTENSION plpython3u;
```

#### 6.3 기본 문법

```sql
CREATE FUNCTION funcname (argument-list)
  RETURNS return-type
AS $$
  # PL/Python function body
$$ LANGUAGE plpython3u;
```

#### 6.4 예제

##### 간단한 함수

```sql
CREATE FUNCTION pymax (a integer, b integer)
  RETURNS integer
AS $$
  if a > b:
    return a
  return b
$$ LANGUAGE plpython3u;
```

##### 변수 스코핑 주의 사항

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

모범 사례: 함수 매개변수를 읽기 전용으로 취급하여 구현 세부 사항에 의존하지 않도록 합니다.

#### 6.5 데이터 타입 매핑

##### PostgreSQL에서 Python으로 변환

| PostgreSQL 타입 | Python 타입 |
|-----------------|-------------|
| `boolean` | `bool` |
| `smallint`, `int`, `bigint`, `oid` | `int` |
| `real`, `double` | `float` |
| `numeric` | `Decimal` (`cdecimal` 또는 `decimal` 모듈) |
| `bytea` | `bytes` |
| 기타 모든 타입 (문자열 타입 포함) | `str` (Unicode) |

##### Python에서 PostgreSQL로 반환 값 변환

- `boolean`: Python 규칙에 따라 진리값 평가 (0과 빈 문자열은 false, `'f'`는 true)
- `bytea`: Python 내장 함수를 사용하여 `bytes`로 변환
- 기타 타입: Python `str()` 내장 함수를 사용하여 문자열로 변환
  - 예외: `float` 값은 정밀도 손실을 피하기 위해 `str()` 대신 `repr()` 사용
  - 문자열은 자동으로 PostgreSQL 서버 인코딩으로 변환

#### 6.6 주요 기능

- 데이터베이스 접근 기능
- 트리거 함수 지원
- 익명 코드 블록 (DO 문)
- 집합 반환 함수
- 명시적 서브트랜잭션 지원

#### 6.7 보안 고려 사항

함수 작성자는 신뢰할 수 없는 PL/Python 함수가 악의적인 목적으로 악용되지 않도록 보장해야 합니다. PL/Python 함수는 데이터베이스 관리자와 동일한 권한을 가집니다.

---

### 7. 절차적 언어 보안

#### 7.1 TRUSTED vs UNTRUSTED 언어

##### TRUSTED 언어

- 일반 데이터베이스 사용자가 함수를 생성할 수 있음
- 무단 데이터 접근을 허용하지 않음
- 예: PL/pgSQL, PL/Tcl, PL/Perl

##### UNTRUSTED 언어

- 슈퍼유저만 함수를 생성할 수 있음
- 시스템 리소스에 대한 제한 없는 접근 가능
- 예: PL/TclU, PL/PerlU, PL/PythonU

#### 7.2 보안 지침

| 지침 | 설명 |
|------|------|
| 최소 권한 원칙 | 필요한 최소한의 권한만 부여 |
| TRUSTED 언어 우선 | 가능하면 TRUSTED 언어 사용 |
| 코드 검토 | UNTRUSTED 언어 함수는 철저한 검토 필요 |
| 입력 검증 | 모든 사용자 입력에 대해 검증 수행 |

#### 7.3 SECURITY DEFINER vs SECURITY INVOKER

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

#### 7.4 보안 모범 사례

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

### 참고 자료

- [PostgreSQL 공식 문서 - Procedural Languages](https://www.postgresql.org/docs/current/xplang.html)
- [PostgreSQL 공식 문서 - PL/pgSQL](https://www.postgresql.org/docs/current/plpgsql.html)
- [PostgreSQL 공식 문서 - PL/Tcl](https://www.postgresql.org/docs/current/pltcl.html)
- [PostgreSQL 공식 문서 - PL/Perl](https://www.postgresql.org/docs/current/plperl.html)
- [PostgreSQL 공식 문서 - PL/Python](https://www.postgresql.org/docs/current/plpython.html)

---

## Chapter 43: PL/pgSQL - SQL 절차적 언어 (SQL Procedural Language)

PL/pgSQL은 PostgreSQL 데이터베이스 시스템을 위한 로드 가능한 절차적 언어입니다. PL/pgSQL의 설계 목표는 다음과 같습니다:

- 함수(Functions)와 프로시저(Procedures)를 작성하는 데 사용할 수 있는 로드 가능한 절차적 언어 제공
- SQL 언어에 제어 구조 추가
- 복잡한 계산 수행 가능
- 모든 사용자 정의 타입, 함수, 프로시저, 연산자 상속
- 서버에서 신뢰할 수 있는(trusted) 언어로 정의

PL/pgSQL로 작성된 함수는 내장 함수가 사용될 수 있는 모든 곳에서 사용할 수 있습니다. 예를 들어, 복잡한 조건부 계산 함수를 만들고 이를 연산자 정의나 인덱스 표현식에서 사용할 수 있습니다.

PostgreSQL 9.0 이상에서 PL/pgSQL은 기본적으로 설치됩니다. 그러나 여전히 로드 가능한 모듈이므로, 특히 보안에 민감한 관리자는 이를 제거할 수 있습니다.

---

### 43.1 개요 (Overview)

#### 43.1.1 PL/pgSQL 사용의 장점

SQL은 PostgreSQL 및 대부분의 다른 관계형 데이터베이스가 쿼리 언어로 사용하는 언어입니다. 이식 가능하고 배우기 쉽습니다. 그러나 모든 SQL 문은 데이터베이스 서버에서 개별적으로 실행되어야 합니다.

이는 클라이언트 애플리케이션이 각 쿼리를 데이터베이스 서버로 보내고, 처리되기를 기다린 후 결과를 받고, 일부 계산을 수행한 다음 서버에 추가 쿼리를 보내야 함을 의미합니다. 클라이언트가 데이터베이스 서버와 다른 머신에 있는 경우 이 모든 것이 프로세스 간 통신을 수반하며 네트워크 오버헤드를 발생시킵니다.

PL/pgSQL을 사용하면 계산 블록과 일련의 쿼리를 데이터베이스 서버 내에서 그룹화하여 절차적 언어의 강력함과 SQL의 사용 용이성을 결합하면서 상당한 클라이언트/서버 통신 오버헤드를 절약할 수 있습니다.

- 클라이언트와 서버 간의 추가 왕복 제거
- 클라이언트에 필요하지 않거나 단순히 조건을 테스트하기 위해 전송해야 하는 중간 결과를 서버와 클라이언트 간에 전송할 필요 없음
- 여러 번의 쿼리 파싱을 피할 수 있음

이로 인해 순수 SQL을 사용하는 것에 비해 상당한 성능 향상을 얻을 수 있습니다.

또한 PL/pgSQL을 사용하면 SQL에서 사용 가능한 모든 데이터 타입, 연산자, 함수를 사용할 수 있습니다.

#### 43.1.2 지원되는 인자 및 결과 데이터 타입

PL/pgSQL으로 작성된 함수는 서버가 지원하는 모든 스칼라 또는 배열 데이터 타입을 인자로 받아들이고 결과로 반환할 수 있습니다. 또한 지정된 행 타입의 복합 타입(행 타입)을 받아들이거나 반환할 수 있습니다. 함수가 반환 타입으로 `record`를 선언하면 익명의 복합 타입도 반환할 수 있습니다.

PL/pgSQL 함수는 `VARIADIC` 마커를 사용하여 가변 개수의 인자를 받아들이도록 선언할 수 있습니다. 이는 SQL 함수와 정확히 동일한 방식으로 작동합니다.

PL/pgSQL 함수는 다형성 타입 `anyelement`, `anyarray`, `anynonarray`, `anyenum`, `anyrange`, `anycompatible`, `anycompatiblearray`, `anycompatiblenonarray`, `anycompatiblerange`를 사용하여 다형성으로 선언할 수도 있습니다.

PL/pgSQL 함수는 집합(또는 테이블)을 반환하도록 선언할 수도 있습니다. 이러한 함수는 모든 집합을 생성하기 위해 원하는 각 행에 대해 `RETURN NEXT`를 실행하거나 `RETURN QUERY`를 사용하여 쿼리 결과를 출력합니다.

---

### 43.2 PL/pgSQL의 구조 (Structure of PL/pgSQL)

PL/pgSQL로 작성된 함수는 다음과 같은 형식으로 정의됩니다:

```sql
CREATE FUNCTION somefunc(integer, text) RETURNS integer
AS $$
    -- 함수 본문
$$ LANGUAGE plpgsql;
```

함수 본문은 단순히 `CREATE FUNCTION`에 대한 문자열 리터럴입니다. 달러 인용(dollar quoting)을 사용하여 함수 본문을 작성하는 것이 더 도움이 됩니다. 달러 인용이 없으면 함수 본문의 작은따옴표나 백슬래시를 이중으로 사용하여 이스케이프해야 합니다.

#### 43.2.1 블록 구조

PL/pgSQL은 블록 구조 언어입니다. 함수 본문의 전체 텍스트는 블록이어야 합니다. 블록은 다음과 같이 정의됩니다:

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
END [ label ];
```

블록 내의 각 선언과 각 문은 세미콜론으로 종료됩니다. 위에 표시된 대로 다른 블록 내에 나타나는 블록은 세미콜론을 가져야 하지만, 함수 본문을 종료하는 최종 `END`는 세미콜론이 필요하지 않습니다.

> 팁: 일반적인 실수는 `BEGIN`/`END` 바로 앞에 세미콜론을 쓰는 것입니다. 이는 올바르지 않으며 구문 오류가 발생합니다.

레이블(label)은 `EXIT` 문에 의해 종료되거나 블록에서 선언된 변수의 이름을 한정하는 데에만 필요합니다. 레이블이 `END` 뒤에 지정되면 블록의 시작 레이블과 일치해야 합니다.

모든 키워드는 대소문자를 구분하지 않습니다. 식별자는 큰따옴표로 묶이지 않는 한 암묵적으로 소문자로 변환됩니다.

PL/pgSQL 코드에서 주석은 SQL에서와 동일한 방식으로 작동합니다. 이중 대시(`--`)는 줄 끝까지 확장되는 주석을 시작합니다. `/*`는 `*/`가 나타날 때까지 확장되는 블록 주석을 시작합니다. 블록 주석은 중첩됩니다.

블록의 문 섹션에 있는 모든 문은 중첩 블록일 수 있습니다. 블록은 컨트롤 구문의 논리적 그룹화 또는 변수를 작은 그룹의 문에 지역화하는 데 사용할 수 있습니다. 선언 섹션에서 선언된 변수는 블록 내의 문을 처리하기 전에 기본값으로 초기화되며, 매번 블록에 진입할 때마다 초기화됩니다.

```sql
CREATE FUNCTION somefunc() RETURNS integer AS $$
<< outerblock >>
DECLARE
    quantity integer := 30;
BEGIN
    RAISE NOTICE 'Quantity here is %', quantity;  -- 30이 출력됨
    quantity := 50;
    --
    -- quantity에 대한 지역 선언을 가진 서브블록 생성
    --
    DECLARE
        quantity integer := 80;
    BEGIN
        RAISE NOTICE 'Quantity here is %', quantity;  -- 80이 출력됨
        RAISE NOTICE 'Outer quantity here is %', outerblock.quantity;  -- 50이 출력됨
    END;

    RAISE NOTICE 'Quantity here is %', quantity;  -- 50이 출력됨

    RETURN quantity;
END;
$$ LANGUAGE plpgsql;
```

> 참고: `BEGIN`/`END` 쌍과 트랜잭션 제어를 위한 같은 이름의 SQL 명령 사이에는 실제로 혼동이 없습니다. PL/pgSQL의 `BEGIN`/`END`는 그룹화만을 위한 것이며 트랜잭션을 시작하거나 종료하지 않습니다. PL/pgSQL에서 트랜잭션을 관리하는 것은 나중에 설명됩니다.

---

### 43.3 선언 (Declarations)

블록에서 사용되는 모든 변수는 블록의 선언 섹션에서 선언되어야 합니다. (유일한 예외는 `FOR` 루프의 루프 변수로, 정수 범위를 반복하는 경우 자동으로 정수 변수로 선언되고, 커서 결과를 반복하는 경우 레코드 변수로 선언됩니다.)

PL/pgSQL 변수는 `integer`, `varchar`, `char`와 같은 모든 SQL 데이터 타입은 물론, 사용자 정의 타입도 가질 수 있습니다.

변수 선언의 일반적인 구문은 다음과 같습니다:

```sql
name [ CONSTANT ] type [ COLLATE collation_name ] [ NOT NULL ] [ { DEFAULT | := | = } expression ];
```

`DEFAULT` 절이 주어지면 블록에 진입할 때 변수에 할당될 초기값을 지정합니다. `DEFAULT` 절이 주어지지 않으면 변수는 SQL 널 값으로 초기화됩니다. `CONSTANT` 옵션은 변수가 값을 할당받지 못하도록 하여 블록이 지속되는 동안 값이 일정하게 유지되도록 합니다. `COLLATE` 옵션은 변수에 사용할 조합(collation)을 지정합니다. `NOT NULL`이 지정되면 널 값을 할당하면 런타임 오류가 발생합니다. `NOT NULL`로 선언된 모든 변수는 널이 아닌 기본값을 지정해야 합니다. `=`는 PL/SQL 규정을 따르기 위해 `:=` 대신 사용할 수 있습니다.

변수의 기본값은 함수가 호출될 때마다가 아니라 블록에 진입할 때마다 평가되고 변수에 할당됩니다. 예를 들어, `integer` 변수에 `now()`를 할당하면 `now()`가 호출되는 순간의 현재 타임스탬프가 할당됩니다.

```sql
quantity integer DEFAULT 32;
url varchar := 'http://mysite.com';
transaction_time CONSTANT timestamp with time zone := now();
```

변수의 기본값 표현식은 블록에 진입할 때마다 실행됩니다. 최상위 블록 변수라도 함수가 호출될 때마다 기본 표현식이 평가됩니다.

#### 43.3.1 함수 인자 선언

함수에 전달된 인자는 식별자 `$1`, `$2` 등으로 명명됩니다. 선택적으로 가독성 향상을 위해 인자 값에 대한 별칭을 선언할 수 있습니다. 그런 다음 별칭이나 숫자 식별자를 사용하여 인자 값을 참조할 수 있습니다.

별칭을 만드는 두 가지 방법이 있습니다. 선호되는 방법은 `CREATE FUNCTION` 명령에서 인자에 이름을 지정하는 것입니다:

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

다른 방법은 선언 구문으로 별칭을 명시적으로 선언하는 것입니다:

```sql
name ALIAS FOR $n;
```

같은 예제를 이 스타일로 작성하면:

```sql
CREATE FUNCTION sales_tax(real) RETURNS real AS $$
DECLARE
    subtotal ALIAS FOR $1;
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

> 참고: 이 두 예제는 완전히 동일하지 않습니다. 첫 번째 경우, `subtotal`은 `sales_tax.subtotal`로 참조될 수 있지만, 두 번째 경우에는 그렇지 않습니다. (별칭을 외부 블록에 레이블을 붙인 경우 별칭을 한정할 수 있습니다.)

출력 인자는 입력 인자와 동일한 방식으로 PL/pgSQL 함수에서 처리됩니다. 출력 인자는 함수의 시작 부분에서 NULL로 시작되며, 함수 실행 중에 할당되어야 합니다. 최종 출력 인자의 값은 반환되는 값입니다. 예를 들어, 판매세 예제는 다음과 같이 작성할 수도 있습니다:

```sql
CREATE FUNCTION sales_tax(subtotal real, OUT tax real) AS $$
BEGIN
    tax := subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

출력 인자는 함수가 여러 값을 반환할 때 가장 유용합니다.

#### 43.3.2 별칭 (Aliases)

```sql
newname ALIAS FOR oldname;
```

`ALIAS` 구문은 함수 인자에 별칭을 할당하는 것보다 더 일반적입니다: 이전에 선언된 변수에 다른 이름을 선언할 수 있습니다. 이는 주로 트리거 함수와 같이 미리 정의된 이름을 가진 변수에 더 의미 있는 이름을 할당하는 데 유용합니다.

```sql
DECLARE
  prior ALIAS FOR old;
  updated ALIAS FOR new;
```

`ALIAS`는 새 변수를 만듭니다; 같은 변수를 참조하는 두 가지 방법이 아닙니다.

#### 43.3.3 복사된 타입 (Copied Types)

```sql
variable%TYPE
```

`%TYPE`은 변수나 테이블 열의 데이터 타입을 제공합니다. 이를 사용하여 데이터베이스 객체의 데이터 타입을 보유하는 변수를 선언할 수 있습니다. 예를 들어, `users` 테이블에 `user_id`라는 열이 있다고 가정하면 다음과 같이 작성할 수 있습니다:

```sql
user_id users.user_id%TYPE;
```

`%TYPE`을 사용하면 참조하는 데이터베이스 객체의 정의를 알 필요가 없으며, 가장 중요한 것은 참조된 객체의 데이터 타입이 미래에 변경되더라도(예: `user_id`의 타입을 `integer`에서 `real`로 변경) 함수 정의를 변경할 필요가 없다는 것입니다.

`%TYPE`은 함수 인자에도 사용할 수 있습니다:

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
DECLARE
    proportion subtotal%TYPE := 0.06;
BEGIN
    RETURN subtotal * proportion;
END;
$$ LANGUAGE plpgsql;
```

#### 43.3.4 행 타입 (Row Types)

```sql
name table_name%ROWTYPE;
name composite_type_name;
```

복합 타입의 변수를 행 변수(또는 행-타입 변수)라고 합니다. 이러한 변수는 `SELECT` 또는 `FOR` 쿼리 결과의 전체 행을 보유할 수 있습니다. 단, 해당 쿼리의 열 집합이 변수의 선언된 타입과 일치해야 합니다. 행 값의 개별 필드는 일반적인 점 표기법을 사용하여 액세스됩니다(예: `rowvar.field`).

행 변수는 복합 타입의 이름을 사용하여 선언하거나 `table_name%ROWTYPE` 표기법을 사용하여 테이블 또는 뷰의 행과 동일한 타입을 가진 변수를 선언할 수 있습니다. `%ROWTYPE`과 함께 사용되는 테이블 또는 뷰 이름은 스키마로 한정될 수 있습니다.

함수의 인자는 복합 타입(테이블 행 포함)일 수 있습니다. 이 경우 해당 식별자 `$n`은 행 변수가 되며, 필드는 점 표기법을 사용하여 선택할 수 있습니다(예: `$1.user_id`).

```sql
CREATE FUNCTION merge_fields(t_row table1) RETURNS text AS $$
DECLARE
    t2_row table2%ROWTYPE;
BEGIN
    SELECT * INTO t2_row FROM table2 WHERE ... ;
    RETURN t_row.f1 || t2_row.f3 || t_row.f5 || t2_row.f7;
END;
$$ LANGUAGE plpgsql;

SELECT merge_fields(t.*) FROM table1 t WHERE ... ;
```

#### 43.3.5 레코드 타입 (Record Types)

```sql
name RECORD;
```

레코드 변수는 행 타입 변수와 비슷하지만 미리 정의된 구조가 없습니다. 레코드 변수는 `SELECT` 또는 `FOR` 명령 중에 할당된 행의 실제 행 구조를 취합니다. 레코드 변수의 하위 구조는 할당될 때마다 변경될 수 있습니다.

레코드 변수는 처음 할당되기 전까지 하위 구조가 없다는 결과가 있습니다. 할당되지 않은 레코드 변수의 필드에 액세스하려고 하면 런타임 오류가 발생합니다.

`RECORD`는 진정한 데이터 타입이 아니라 플레이스홀더일 뿐입니다. PL/pgSQL 함수가 `record` 타입을 반환하도록 선언된 경우 이는 레코드 변수와 정확히 동일한 개념이 아닙니다.

#### 43.3.6 PL/pgSQL 변수의 데이터 정렬 (Collation of PL/pgSQL Variables)

PL/pgSQL 함수가 데이터 정렬(collation)이 가능한 타입의 인자를 받는 경우, 함수 호출 시 인자로부터 적용할 collation이 결정됩니다. 인자 간에 암묵적 collation 충돌이 없으면, 모든 해당 인자가 그 collation을 암묵적으로 갖는 것으로 처리됩니다.

---

### 43.4 표현식 (Expressions)

PL/pgSQL에서 사용되는 모든 표현식은 PostgreSQL의 메인 SQL 실행기에서 처리됩니다. 예를 들어 다음과 같이 작성할 때:

```sql
IF expression THEN ...
```

PL/pgSQL은 내부적으로 다음과 같은 쿼리를 실행합니다:

```sql
SELECT expression
```

표현식에서 선언된 PL/pgSQL 변수에 대한 참조를 매개변수로 대체합니다. 이렇게 하면 `SELECT`에 대한 쿼리 계획이 한 번만 준비되고 이후 호출에서 재사용됩니다.

이러한 방식으로 표현식을 평가하면 일반적인 SQL 쿼리가 할 수 있는 모든 것을 의미합니다:

- 산술, 문자열, 비교 연산자를 사용하여 복잡한 표현식 계산
- 함수 호출
- 스칼라 서브쿼리 포함

그러나 표현식이 스칼라 값으로 평가되어야 한다는 요구 사항도 있습니다(복합 타입을 반환하는 경우도 단일 값).

---

### 43.5 기본 문 (Basic Statements)

#### 43.5.1 할당 (Assignment)

변수 또는 행/레코드 필드에 대한 할당은 다음과 같이 작성됩니다:

```sql
variable { := | = } expression;
```

앞서 언급했듯이 이러한 문의 표현식은 PostgreSQL 메인 SQL 엔진에 전송된 `SELECT` 명령을 통해 평가됩니다. 표현식은 단일 값을 생성해야 합니다(변수가 행 또는 레코드 변수인 경우 행 값일 수 있음).

대상 변수가 단순 변수(행/레코드 변수가 아님)인 경우 `=`를 `:=` 대신 사용할 수 있습니다.

```sql
tax := subtotal * 0.06;
my_record.user_id := 20;
my_array[1] := 5;
my_array[2:4] := ARRAY[1, 2, 3];
```

#### 43.5.2 단일 행 결과를 갖는 SELECT 실행 (Executing a Command with a Single-Row Result)

단일 행(아마도 여러 열)을 생성하는 SQL 명령의 결과는 레코드 변수, 행 타입 변수 또는 스칼라 변수 목록에 할당될 수 있습니다. 이는 기본 SQL 명령에 `INTO` 절을 작성하여 수행됩니다:

```sql
SELECT select_expressions INTO [STRICT] target FROM ...;
INSERT ... RETURNING expressions INTO [STRICT] target;
UPDATE ... RETURNING expressions INTO [STRICT] target;
DELETE ... RETURNING expressions INTO [STRICT] target;
```

여기서 `target`은 레코드 변수, 행 변수 또는 쉼표로 구분된 단순 변수와 레코드/행 필드 목록일 수 있습니다.

`SELECT`를 사용하는 경우 `INTO` 절은 쿼리의 `select_expressions` 목록 바로 뒤, 또는 `FROM` 절 바로 앞에 작성할 수 있습니다.

`STRICT`가 지정되지 않으면 `target`은 쿼리에서 반환된 첫 번째 행으로 설정되고, 쿼리가 행을 반환하지 않으면 널로 설정됩니다. (첫 번째 행은 `ORDER BY`를 사용하지 않는 한 "정의되지 않음"입니다.) 첫 번째 행 이후의 모든 결과 행은 버려집니다.

```sql
SELECT * INTO myrec FROM emp WHERE empname = myname;
IF NOT FOUND THEN
    RAISE EXCEPTION 'employee % not found', myname;
END IF;
```

`STRICT` 옵션이 지정되면 쿼리가 정확히 하나의 행을 반환해야 합니다. 그렇지 않으면 `NO_DATA_FOUND`(행이 없음) 또는 `TOO_MANY_ROWS`(둘 이상의 행)와 같은 런타임 오류가 발생합니다.

```sql
BEGIN
    SELECT * INTO STRICT myrec FROM emp WHERE empname = myname;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE EXCEPTION 'employee % not found', myname;
        WHEN TOO_MANY_ROWS THEN
            RAISE EXCEPTION 'employee % not unique', myname;
END;
```

`STRICT`가 지정되지 않은 쿼리의 경우 `FOUND`는 행이 반환되면 true로, 반환되지 않으면 false로 설정됩니다.

#### 43.5.3 결과가 없는 명령 또는 동적 명령 실행 (Executing a Command with No Result)

관심 있는 결과를 반환하지 않는 SQL 명령의 경우(예: `INSERT` without `RETURNING`, 또는 결과가 필요 없는 다른 DML), 일반 SQL 명령으로 실행할 수 있습니다:

```sql
INSERT INTO mytable (id, value) VALUES (nextval('myseq'), 'hello');
```

또는 `PERFORM` 문으로 표현식을 평가하고 결과를 버릴 수 있습니다:

```sql
PERFORM query;
```

`SELECT` 대신 `PERFORM` 키워드를 사용하는 것 외에는 일반 `SELECT` 문과 동일하게 작성하며, 실행 결과는 모두 버려집니다.

```sql
PERFORM create_mv('cs_session_page_requests_mv', my_query);
```

#### 43.5.4 동적 명령 실행 (Executing Dynamic Commands)

가변 테이블이나 열에 대해 동적 명령을 생성해야 할 때가 많습니다. PL/pgSQL 함수 내에서 동적 명령을 실행하려면 `EXECUTE` 문을 사용합니다:

```sql
EXECUTE command-string [ INTO [STRICT] target ] [ USING expression [, ... ] ];
```

여기서 `command-string`은 실행할 명령을 포함하는 문자열(텍스트 타입)을 생성하는 표현식입니다.

```sql
EXECUTE 'SELECT count(*) FROM mytable WHERE inserted_by = $1 AND inserted <= $2'
   INTO c
   USING checked_user, checked_date;
```

`USING` 절을 사용하여 명령에 매개변수 값을 삽입합니다. 이는 명령 문자열에 값을 텍스트로 직접 삽입하는 것보다 선호되는 방식으로, 값을 텍스트로 변환하고 다시 되돌리는 런타임 오버헤드를 줄이고 SQL 인젝션 위험도 낮춥니다.

식별자나 리터럴을 동적으로 삽입해야 할 경우 `format` 함수를 함께 사용하면 편리합니다:

```sql
EXECUTE format('SELECT count(*) FROM %I '
   'WHERE inserted_by = $1 AND inserted <= $2', tabname)
   INTO c
   USING checked_user, checked_date;
```

`%I` 지정자는 식별자로 처리되어야 하는 값을 삽입하고, `%L` 지정자는 리터럴로 처리되어야 하는 값을 삽입합니다.

#### 43.5.5 쿼리 결과의 상태 얻기 (Obtaining the Result Status)

다음과 같은 명령을 실행한 후 영향을 받은 행 수를 확인할 수 있습니다:

```sql
GET DIAGNOSTICS variable { = | := } item [ , ... ];
```

현재 사용 가능한 상태 항목은 다음과 같습니다:

| 이름 | 타입 | 설명 |
|------|------|------|
| `ROW_COUNT` | bigint | 가장 최근 SQL 명령에서 처리된 행 수 |
| `PG_CONTEXT` | text | 현재 호출 스택을 설명하는 줄 |
| `PG_ROUTINE_OID` | oid | 현재 함수의 OID |

```sql
GET DIAGNOSTICS integer_var = ROW_COUNT;
```

#### 43.5.6 아무것도 하지 않기 (Doing Nothing At All)

아무 작업도 수행하지 않는 플레이스홀더 문이 필요할 때가 있습니다. 예를 들어, if/then/else 체인의 한 분기를 의도적으로 비워 두고 싶을 때는 다음과 같이 작성합니다:

```sql
NULL;
```

예를 들어, 다음 두 코드 조각은 동일합니다:

```sql
BEGIN
    y := x / 0;
EXCEPTION
    WHEN division_by_zero THEN
        NULL;  -- 오류 무시
END;
```

---

### 43.6 제어 구조 (Control Structures)

제어 구조는 아마도 PL/pgSQL의 가장 유용하고 중요한 부분입니다. PL/pgSQL의 제어 구조를 사용하면 매우 유연하고 강력한 방식으로 PostgreSQL 데이터를 조작할 수 있습니다.

#### 43.6.1 함수에서 반환 (Returning from a Function)

함수에서 데이터를 반환하는 두 가지 명령이 있습니다: `RETURN`과 `RETURN NEXT`(+ `RETURN QUERY`).

##### 43.6.1.1 RETURN

```sql
RETURN expression;
```

`RETURN`과 함께 표현식을 사용하면 함수가 종료되고 `expression`의 값이 호출자에게 반환됩니다. 이 형식은 스칼라 타입을 반환하는 PL/pgSQL 함수에서 사용됩니다.

```sql
RETURN;
```

표현식 없는 `RETURN`을 사용하여 `void`를 반환하도록 선언된 함수를 종료합니다.

프로시저에서 반환할 때는 표현식 없는 `RETURN`을 사용합니다.

복합 타입을 반환하도록 선언된 함수의 경우 표현식은 적절한 복합 값을 생성해야 합니다:

```sql
RETURN composite_expression;
```

또는 출력 인자를 사용한 경우:

```sql
RETURN;  -- 출력 인자가 자동으로 반환됨
```

##### 43.6.1.2 RETURN NEXT 및 RETURN QUERY

```sql
RETURN NEXT expression;
RETURN QUERY query;
RETURN QUERY EXECUTE command-string [ USING expression [, ... ] ];
```

함수가 `SETOF sometype`을 반환하도록 선언된 경우 따라야 할 절차가 약간 다릅니다. 개별 항목은 `RETURN NEXT` 명령의 시퀀스를 통해 반환되고, 마지막에 표현식 없는 `RETURN`이 함수가 실행을 완료했음을 나타내는 데 사용됩니다.

```sql
CREATE FUNCTION get_all_foo() RETURNS SETOF foo AS $$
DECLARE
    r foo%rowtype;
BEGIN
    FOR r IN
        SELECT * FROM foo WHERE fooid > 0
    LOOP
        -- 일부 처리를 수행할 수 있음
        RETURN NEXT r; -- SELECT * FROM foo의 현재 행을 반환
    END LOOP;
    RETURN;
END;
$$ LANGUAGE plpgsql;
```

`RETURN QUERY`는 쿼리 결과의 모든 행을 함수의 결과 집합에 추가합니다:

```sql
CREATE FUNCTION get_available_flightid(date) RETURNS SETOF integer AS $$
BEGIN
    RETURN QUERY SELECT flightid
                   FROM flight
                  WHERE flightdate >= $1
                    AND flightdate < ($1 + 1);

    -- 쿼리가 아무것도 반환하지 않았는지 확인 가능
    IF NOT FOUND THEN
        RAISE NOTICE 'No flights for %', $1;
    END IF;

    RETURN;
END;
$$ LANGUAGE plpgsql;
```

#### 43.6.2 프로시저에서 반환 (Returning from a Procedure)

프로시저에는 반환 값이 없습니다. 따라서 프로시저는 표현식 없는 `RETURN` 문으로 종료할 수 있습니다:

```sql
RETURN;
```

`RETURN` 문은 프로시저가 자연스럽게 끝나기 전에 종료하려는 경우에만 필요합니다.

프로시저의 출력 인자는 프로시저가 정상적으로 종료되거나 `RETURN` 문에 도달하면 호출자에게 반환됩니다.

#### 43.6.3 호출자에 대한 제어 반환 (Returning Control to the Caller)

프로시저 본문 내부에서 `COMMIT` 또는 `ROLLBACK`을 호출하면 트랜잭션이 종료되고 새 트랜잭션이 자동으로 시작됩니다. 이러한 명령 후에 실행이 계속됩니다.

#### 43.6.4 조건문 (Conditionals)

`IF`와 `CASE` 문을 사용하면 특정 조건에 따라 명령을 실행할 수 있습니다. PL/pgSQL에는 세 가지 형태의 `IF`가 있습니다:

- `IF ... THEN ... END IF`
- `IF ... THEN ... ELSE ... END IF`
- `IF ... THEN ... ELSIF ... THEN ... ELSE ... END IF`

그리고 두 가지 형태의 `CASE`가 있습니다:

- `CASE ... WHEN ... THEN ... ELSE ... END CASE`
- `CASE WHEN ... THEN ... ELSE ... END CASE`

##### IF-THEN

```sql
IF boolean-expression THEN
    statements
END IF;
```

`IF-THEN` 문은 가장 간단한 형태의 `IF`입니다. `THEN`과 `END IF` 사이의 문은 조건이 참이면 실행됩니다. 그렇지 않으면 건너뜁니다.

```sql
IF v_user_id <> 0 THEN
    UPDATE users SET email = v_email WHERE user_id = v_user_id;
END IF;
```

##### IF-THEN-ELSE

```sql
IF boolean-expression THEN
    statements
ELSE
    statements
END IF;
```

`IF-THEN-ELSE` 문은 조건이 참이 아닐 때 실행되어야 할 대체 문 집합을 지정할 수 있도록 `IF-THEN`에 추가합니다.

```sql
IF parentid IS NULL OR parentid = ''
THEN
    RETURN fullname;
ELSE
    RETURN hp_true_filename(parentid) || '/' || fullname;
END IF;
```

##### IF-THEN-ELSIF

```sql
IF boolean-expression THEN
    statements
[ ELSIF boolean-expression THEN
    statements
[ ELSIF boolean-expression THEN
    statements
    ...]]
[ ELSE
    statements ]
END IF;
```

가끔 두 가지 이상의 대안이 있습니다. `IF-THEN-ELSIF`는 이를 가능하게 합니다. 조건은 참이 될 때까지 연속적으로 테스트됩니다.

```sql
IF number = 0 THEN
    result := 'zero';
ELSIF number > 0 THEN
    result := 'positive';
ELSIF number < 0 THEN
    result := 'negative';
ELSE
    -- 다른 유일한 가능성은 number가 널인 경우
    result := 'NULL';
END IF;
```

> 참고: `ELSEIF`는 `ELSIF`의 별칭입니다.

##### 간단한 CASE

```sql
CASE search-expression
    WHEN expression [, expression [ ... ]] THEN
      statements
  [ WHEN expression [, expression [ ... ]] THEN
      statements
    ... ]
  [ ELSE
      statements ]
END CASE;
```

간단한 형태의 `CASE`는 검색 표현식의 값을 기반으로 조건부 실행을 제공합니다.

```sql
CASE x
    WHEN 1, 2 THEN
        msg := 'one or two';
    ELSE
        msg := 'other value than one or two';
END CASE;
```

##### 검색된 CASE

```sql
CASE
    WHEN boolean-expression THEN
      statements
  [ WHEN boolean-expression THEN
      statements
    ... ]
  [ ELSE
      statements ]
END CASE;
```

검색된 형태의 `CASE`는 부울 표현식의 진리값을 기반으로 조건부 실행을 제공합니다.

```sql
CASE
    WHEN x BETWEEN 0 AND 10 THEN
        msg := 'value is between zero and ten';
    WHEN x BETWEEN 11 AND 20 THEN
        msg := 'value is between eleven and twenty';
END CASE;
```

#### 43.6.5 단순 루프 (Simple Loops)

`LOOP`, `EXIT`, `CONTINUE`, `WHILE`, `FOR`, `FOREACH` 문을 사용하면 PL/pgSQL 함수가 일련의 명령을 반복하도록 할 수 있습니다.

##### LOOP

```sql
[ <<label>> ]
LOOP
    statements
END LOOP [ label ];
```

`LOOP`는 `EXIT` 또는 `RETURN` 문에 의해 종료될 때까지 무한히 반복되는 무조건적인 루프를 정의합니다. 선택적 레이블은 중첩된 루프 내의 `EXIT` 및 `CONTINUE` 문에서 종료하거나 계속할 루프를 지정하는 데 사용할 수 있습니다.

##### EXIT

```sql
EXIT [ label ] [ WHEN boolean-expression ];
```

레이블이 지정되지 않은 경우 가장 안쪽의 루프가 종료되고 `END LOOP` 다음 문이 실행됩니다. 레이블이 지정되면 해당 레이블이 있는 루프가 종료됩니다.

```sql
LOOP
    -- 일부 계산
    IF count > 0 THEN
        EXIT;  -- 루프 종료
    END IF;
END LOOP;
```

`WHEN`이 있으면 부울 표현식이 참인 경우에만 루프 종료가 발생합니다:

```sql
LOOP
    -- 일부 계산
    EXIT WHEN count > 0;
END LOOP;
```

##### CONTINUE

```sql
CONTINUE [ label ] [ WHEN boolean-expression ];
```

레이블이 지정되지 않은 경우 가장 안쪽 루프의 다음 반복이 시작됩니다. 레이블이 지정되면 해당 레이블이 있는 루프의 다음 반복이 시작됩니다.

```sql
LOOP
    -- 일부 계산
    EXIT WHEN count > 100;
    CONTINUE WHEN count < 50;
    -- count가 50 이상일 때만 관심 있는 일부 계산
END LOOP;
```

##### WHILE

```sql
[ <<label>> ]
WHILE boolean-expression LOOP
    statements
END LOOP [ label ];
```

`WHILE` 문은 부울 표현식이 참으로 평가되는 한 일련의 문을 반복합니다. 각 루프 본문에 진입하기 직전에 표현식이 확인됩니다.

```sql
WHILE amount_owed > 0 AND gift_certificate_balance > 0 LOOP
    -- 일부 계산
END LOOP;

WHILE NOT done LOOP
    -- 일부 계산
END LOOP;
```

##### FOR (정수 변형)

```sql
[ <<label>> ]
FOR name IN [ REVERSE ] expression .. expression [ BY expression ] LOOP
    statements
END LOOP [ label ];
```

이 형태의 `FOR`는 정수 값의 범위를 반복하는 루프를 만듭니다. 변수 `name`은 자동으로 `integer` 타입으로 정의되며 루프 내에서만 존재합니다(기존 변수의 정의는 루프 내에서 무시됨). 범위의 하한과 상한을 지정하는 두 표현식은 루프에 진입할 때 한 번 평가됩니다.

```sql
FOR i IN 1..10 LOOP
    -- i는 루프 내에서 1부터 10까지의 값을 취함
END LOOP;

FOR i IN REVERSE 10..1 LOOP
    -- i는 루프 내에서 10부터 1까지의 값을 취함
END LOOP;

FOR i IN REVERSE 10..1 BY 2 LOOP
    -- i는 루프 내에서 10, 8, 6, 4, 2의 값을 취함
END LOOP;
```

#### 43.6.6 쿼리 결과를 통한 루핑 (Looping Through Query Results)

다른 타입의 `FOR` 루프를 사용하면 쿼리의 결과를 반복하고 해당 데이터를 그에 따라 조작할 수 있습니다.

```sql
[ <<label>> ]
FOR target IN query LOOP
    statements
END LOOP [ label ];
```

`target`은 레코드 변수, 행 변수 또는 쉼표로 구분된 스칼라 변수 목록입니다. `target`은 `query`의 각 행에 연속적으로 할당되고 루프 본문이 각 행에 대해 실행됩니다.

```sql
CREATE FUNCTION refresh_mviews() RETURNS integer AS $$
DECLARE
    mviews RECORD;
BEGIN
    RAISE NOTICE 'Refreshing all materialized views...';

    FOR mviews IN
       SELECT n.nspname AS mv_schema,
              c.relname AS mv_name,
              pg_catalog.pg_get_userbyid(c.relowner) AS owner
         FROM pg_catalog.pg_class c
    LEFT JOIN pg_catalog.pg_namespace n ON (n.oid = c.relnamespace)
        WHERE c.relkind = 'm'
     ORDER BY 1
    LOOP

        -- 이제 "mviews"에는 한 행의 레코드가 있습니다

        RAISE NOTICE 'Refreshing materialized view %.%...',
                     quote_ident(mviews.mv_schema),
                     quote_ident(mviews.mv_name);
        EXECUTE format('REFRESH MATERIALIZED VIEW %I.%I',
                       mviews.mv_schema, mviews.mv_name);
    END LOOP;

    RAISE NOTICE 'Done refreshing materialized views.';
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
```

`FOR-IN-EXECUTE`를 사용하여 동적 쿼리의 결과를 반복할 수도 있습니다:

```sql
[ <<label>> ]
FOR target IN EXECUTE text_expression [ USING expression [, ... ] ] LOOP
    statements
END LOOP [ label ];
```

#### 43.6.7 배열을 통한 루핑 (Looping Through Arrays)

`FOREACH` 루프는 `FOR` 루프와 매우 비슷하지만 쿼리에서 반환된 행을 반복하는 대신 배열 값의 요소를 반복합니다.

```sql
[ <<label>> ]
FOREACH target [ SLICE number ] IN ARRAY expression LOOP
    statements
END LOOP [ label ];
```

`SLICE` 없이(또는 `SLICE 0`) `FOREACH`는 표현식을 평가하여 생성된 배열의 개별 요소를 반복합니다.

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

양수 `SLICE` 값을 사용하면 `FOREACH`는 개별 요소가 아니라 배열의 슬라이스를 반복합니다.

```sql
CREATE FUNCTION scan_rows(int[]) RETURNS void AS $$
DECLARE
  x int[];
BEGIN
  FOREACH x SLICE 1 IN ARRAY $1
  LOOP
    RAISE NOTICE 'row = %', x;
  END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT scan_rows(ARRAY[[1,2,3],[4,5,6],[7,8,9],[10,11,12]]);
-- 출력:
-- NOTICE:  row = {1,2,3}
-- NOTICE:  row = {4,5,6}
-- NOTICE:  row = {7,8,9}
-- NOTICE:  row = {10,11,12}
```

#### 43.6.8 오류 트래핑 (Trapping Errors)

기본적으로 PL/pgSQL 함수에서 발생하는 모든 오류는 함수 실행을 중단하고 실제로 주변 트랜잭션도 중단합니다. `BEGIN` 블록에 `EXCEPTION` 절을 사용하여 오류를 트랩하고 복구할 수 있습니다:

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
          handler_statements
      ... ]
END;
```

`EXCEPTION` 절이 있는 경우, 블록에 진입하기 전에 암묵적인 서브트랜잭션(subtransaction)이 설정됩니다. 블록이 성공적으로 완료되면 서브트랜잭션이 커밋됩니다. 오류가 발생하면 블록에서 수행한 모든 데이터베이스 변경 사항이 롤백되고 적절한 예외 핸들러로 제어가 전달됩니다.

```sql
CREATE TABLE mytab(id int PRIMARY KEY, data text);
INSERT INTO mytab(id, data) VALUES (1, 'hello');

CREATE FUNCTION merge_db(key int, data text) RETURNS void AS $$
BEGIN
    LOOP
        -- 먼저 업데이트 시도
        UPDATE mytab SET data = merge_db.data WHERE id = key;
        IF found THEN
            RETURN;
        END IF;
        -- 없으면 삽입 시도
        -- 다른 세션이 동시에 행을 삽입하면 고유 키 오류가 발생할 수 있음
        BEGIN
            INSERT INTO mytab(id, data) VALUES (key, data);
            RETURN;
        EXCEPTION WHEN unique_violation THEN
            -- 아무것도 하지 않고 UPDATE를 다시 시도
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT merge_db(1, 'david');
SELECT merge_db(1, 'dennis');
```

특수 조건 이름 `OTHERS`는 `QUERY_CANCELED`와 `ASSERT_FAILURE`를 제외한 모든 오류 타입과 일치합니다. `OTHERS` 핸들러 끝에는 일반적으로 `RAISE`를 사용하여 원래 오류를 다시 발생시킵니다.

예외 핸들러 내에서 `GET STACKED DIAGNOSTICS` 명령을 사용하여 현재 예외에 대한 정보를 얻을 수 있습니다:

```sql
GET STACKED DIAGNOSTICS variable { = | := } item [ , ... ];
```

사용 가능한 항목은 다음과 같습니다:

| 이름 | 타입 | 설명 |
|------|------|------|
| `RETURNED_SQLSTATE` | text | 예외의 SQLSTATE 오류 코드 |
| `COLUMN_NAME` | text | 예외와 관련된 열 이름 |
| `CONSTRAINT_NAME` | text | 예외와 관련된 제약 조건 이름 |
| `PG_DATATYPE_NAME` | text | 예외와 관련된 데이터 타입 이름 |
| `MESSAGE_TEXT` | text | 예외의 기본 메시지 텍스트 |
| `TABLE_NAME` | text | 예외와 관련된 테이블 이름 |
| `SCHEMA_NAME` | text | 예외와 관련된 스키마 이름 |
| `PG_EXCEPTION_DETAIL` | text | 예외의 상세 메시지 텍스트 |
| `PG_EXCEPTION_HINT` | text | 예외의 힌트 메시지 텍스트 |
| `PG_EXCEPTION_CONTEXT` | text | 예외 컨텍스트를 설명하는 줄 |

```sql
DECLARE
  text_var1 text;
  text_var2 text;
  text_var3 text;
BEGIN
  -- 일부 처리 수행
  ...
EXCEPTION WHEN OTHERS THEN
  GET STACKED DIAGNOSTICS text_var1 = MESSAGE_TEXT,
                          text_var2 = PG_EXCEPTION_DETAIL,
                          text_var3 = PG_EXCEPTION_HINT;
END;
```

---

### 43.7 커서 (Cursors)

전체 결과 집합을 한꺼번에 처리하는 대신, 커서를 선언하여 쿼리를 캡슐화하고 결과를 몇 행씩 순차적으로 읽을 수 있습니다. 결과 행 수가 매우 많을 때 메모리 오버플로를 방지하는 데 유용합니다.

커서를 함수의 반환값으로 사용하는 방법도 있습니다. 함수에서 대량의 행 집합을 효율적으로 반환할 때 유용하며, 호출자는 반환된 커서를 통해 행을 직접 읽습니다.

#### 43.7.1 커서 변수 선언 (Declaring Cursor Variables)

PL/pgSQL에서 커서에 대한 모든 액세스는 커서 변수를 통해 이루어지며, 이는 항상 특수 데이터 타입 `refcursor`입니다. 커서 변수를 만드는 한 가지 방법은 `refcursor` 타입의 변수로 선언하는 것입니다. 다른 방법은 커서 선언 구문을 사용하는 것입니다:

```sql
name [ [ NO ] SCROLL ] CURSOR [ ( arguments ) ] FOR query;
```

`SCROLL`이 지정되면 커서는 역방향으로 스크롤할 수 있습니다. `NO SCROLL`이 지정되면 역방향 페치가 거부됩니다. 지정되지 않은 경우 쿼리에 따라 역방향 페치가 허용될 수 있습니다.

```sql
DECLARE
    curs1 refcursor;
    curs2 CURSOR FOR SELECT * FROM tenk1;
    curs3 CURSOR (key integer) FOR SELECT * FROM tenk1 WHERE unique1 = key;
```

세 가지 모두 데이터 타입은 `refcursor`이지만, 첫 번째는 모든 쿼리에 사용할 수 있고, 두 번째는 이미 완전히 지정된 쿼리에 바인딩되어 있으며, 세 번째는 매개변수화된 쿼리에 바인딩되어 있습니다.

#### 43.7.2 커서 열기 (Opening Cursors)

커서를 사용하여 행을 검색하기 전에 열어야 합니다. PL/pgSQL에는 세 가지 형태의 `OPEN` 문이 있습니다.

##### OPEN FOR query

```sql
OPEN unbound_cursorvar [ [ NO ] SCROLL ] FOR query;
```

커서 변수가 열리고 지정된 쿼리가 실행됩니다.

```sql
OPEN curs1 FOR SELECT * FROM foo WHERE key = mykey;
```

##### OPEN FOR EXECUTE

```sql
OPEN unbound_cursorvar [ [ NO ] SCROLL ] FOR EXECUTE query_string
                                         [ USING expression [, ... ] ];
```

커서 변수가 열리고 지정된 쿼리가 실행됩니다. 쿼리는 문자열 표현식으로 지정됩니다.

```sql
OPEN curs1 FOR EXECUTE format('SELECT * FROM %I WHERE col1 = $1', tabname) USING keyvalue;
```

##### 바인딩된 커서 열기

```sql
OPEN bound_cursorvar [ ( [ argument_name := ] argument_value [, ...] ) ];
```

이 형태의 `OPEN`은 선언 시 쿼리에 바인딩된 커서 변수에 사용됩니다.

```sql
OPEN curs2;
OPEN curs3(42);
OPEN curs3(key := 42);
```

#### 43.7.3 커서 사용 (Using Cursors)

커서가 열리면 여기에 설명된 문을 사용하여 조작할 수 있습니다.

##### FETCH

```sql
FETCH [ direction { FROM | IN } ] cursor INTO target;
```

`FETCH`는 커서에서 다음 행을 가져와 `target`(행 변수, 레코드 변수 또는 쉼표로 구분된 단순 변수 목록)에 저장합니다.

```sql
FETCH curs1 INTO rowvar;
FETCH curs2 INTO foo, bar, baz;
FETCH LAST FROM curs3 INTO x, y;
FETCH RELATIVE -2 FROM curs4 INTO x;
```

`direction` 절은 SQL의 `FETCH` 명령에서 허용된 모든 변형이 될 수 있습니다:

- `NEXT` (기본값)
- `PRIOR`
- `FIRST`
- `LAST`
- `ABSOLUTE count`
- `RELATIVE count`
- `FORWARD`
- `BACKWARD`

성공적인 페치 후 `FOUND`는 true로 설정되고, 더 이상 행이 없으면 false로 설정됩니다.

##### MOVE

```sql
MOVE [ direction { FROM | IN } ] cursor;
```

`MOVE`는 데이터를 반환하지 않고 커서를 재배치합니다. `FETCH`와 정확히 동일하게 작동하지만 행을 반환하지 않습니다.

```sql
MOVE curs1;
MOVE LAST FROM curs3;
MOVE RELATIVE -2 FROM curs4;
MOVE FORWARD 2 FROM curs4;
```

##### UPDATE/DELETE WHERE CURRENT OF

```sql
UPDATE table SET ... WHERE CURRENT OF cursor;
DELETE FROM table WHERE CURRENT OF cursor;
```

커서가 테이블에 단순히 위치해 있으면 `WHERE CURRENT OF` 조건을 사용하여 가장 최근에 페치된 행을 업데이트하거나 삭제할 수 있습니다.

```sql
UPDATE foo SET dataval = myval WHERE CURRENT OF curs1;
```

##### CLOSE

```sql
CLOSE cursor;
```

`CLOSE`는 열린 커서의 기반이 되는 포털을 닫습니다. 커서가 닫히면 트랜잭션이 끝나기 전에 다시 열 수 있으며, 동일하거나 다른 쿼리에 바인딩될 수 있습니다.

```sql
CLOSE curs1;
```

#### 43.7.4 커서를 통해 결과 반환 (Returning Cursors)

PL/pgSQL 함수는 호출자에게 커서를 반환할 수 있습니다. 대량의 행 집합을 반환할 때 유용하며, 함수는 커서 이름 문자열을 반환합니다.

```sql
CREATE FUNCTION myfunc(refcursor, refcursor) RETURNS SETOF refcursor AS $$
BEGIN
    OPEN $1 FOR SELECT * FROM table_1;
    RETURN NEXT $1;
    OPEN $2 FOR SELECT * FROM table_2;
    RETURN NEXT $2;
END;
$$ LANGUAGE plpgsql;

-- 함수를 호출하려면:
BEGIN;
SELECT * FROM myfunc('a', 'b');
-- 결과:
-- myfunc
--------
-- a
-- b
-- (2 rows)

FETCH ALL FROM a;
FETCH ALL FROM b;
COMMIT;
```

---

### 43.8 프로시저에서의 트랜잭션 관리 (Transaction Management in Procedures)

함수가 아닌 저장 프로시저에서는 `COMMIT` 및 `ROLLBACK` 명령으로 현재 트랜잭션을 종료하고 새 트랜잭션을 시작할 수 있습니다.

```sql
CREATE PROCEDURE transaction_test1()
LANGUAGE plpgsql
AS $$
BEGIN
    FOR i IN 0..9 LOOP
        INSERT INTO test1 (a) VALUES (i);
        IF i % 2 = 0 THEN
            COMMIT;
        ELSE
            ROLLBACK;
        END IF;
    END LOOP;
END;
$$;

CALL transaction_test1();
```

새로운 트랜잭션은 기본 트랜잭션 특성(예: 격리 수준)으로 시작됩니다. `COMMIT AND CHAIN` 및 `ROLLBACK AND CHAIN` 변형을 사용하면 종료된 트랜잭션과 동일한 트랜잭션 특성을 가진 새 트랜잭션을 시작합니다.

특별한 고려 사항:

- 트랜잭션 제어는 최상위 레벨의 `CALL`에서 호출되거나 중첩된 `CALL` 또는 `DO` 호출에서 직접 호출된 프로시저에서만 사용할 수 있습니다.
- 커서 루프는 트랜잭션 종료로 인해 암묵적으로 닫히지만, `WITH HOLD` 커서는 열린 상태로 유지될 수 있습니다.
- `EXCEPTION` 절이 있는 블록에서는 트랜잭션 제어 명령을 사용할 수 없습니다.

---

### 43.9 오류 및 메시지 (Errors and Messages)

`RAISE` 문을 사용하여 메시지를 보고하고 오류를 발생시킬 수 있습니다.

```sql
RAISE [ level ] 'format' [, expression [, ... ]] [ USING option = expression [, ... ] ];
RAISE [ level ] condition_name [ USING option = expression [, ... ] ];
RAISE [ level ] SQLSTATE 'sqlstate' [ USING option = expression [, ... ] ];
RAISE [ level ] USING option = expression [, ... ];
RAISE ;
```

`level` 옵션은 오류 심각도를 지정합니다. 허용되는 수준은 `DEBUG`, `LOG`, `INFO`, `NOTICE`, `WARNING`, `EXCEPTION`이며, 기본값은 `EXCEPTION`입니다. `EXCEPTION`은 오류를 발생시키고(일반적으로 현재 트랜잭션을 중단), 다른 수준은 다양한 우선순위의 메시지만 생성합니다.

`format` 문자열 내에서 `%`는 다음 선택적 인자 값의 문자열 표현으로 대체됩니다. 리터럴 `%`를 내보내려면 `%%`를 작성합니다. 인자 수는 `format` 문자열의 플레이스홀더 수와 일치해야 합니다.

```sql
RAISE NOTICE 'Calling cs_create_job(%)', v_job_id;
RAISE EXCEPTION '존재하지 않는 ID --> %', user_id
      USING HINT = 'user_id가 올바른지 확인하세요';
```

`USING` 다음에 제공되는 옵션은 다음과 같습니다:

| 옵션 | 설명 |
|------|------|
| `MESSAGE` | 오류 메시지 텍스트 설정 |
| `DETAIL` | 오류 상세 메시지 제공 |
| `HINT` | 힌트 메시지 제공 |
| `ERRCODE` | 오류 코드 지정 |
| `COLUMN`, `CONSTRAINT`, `DATATYPE`, `TABLE`, `SCHEMA` | 관련 객체 이름 제공 |

```sql
RAISE EXCEPTION 'Nonexistent ID --> %', user_id
      USING ERRCODE = 'unique_violation';
-- 또는
RAISE 'Duplicate user ID: %', user_id USING ERRCODE = '23505';
```

매개변수 없이 `RAISE;`를 사용하면 현재 처리 중인 예외가 다시 발생합니다(예외 핸들러에서만 의미가 있음):

```sql
EXCEPTION WHEN OTHERS THEN
    -- 로깅 등 처리
    RAISE;  -- 원래 예외 다시 발생
END;
```

#### ASSERT

`ASSERT` 문은 PL/pgSQL 함수에 디버깅 검사를 삽입하는 편리한 방법입니다:

```sql
ASSERT condition [ , message ];
```

`condition`은 항상 true로 평가될 것으로 예상되는 부울 표현식입니다. 그렇다면 `ASSERT` 문은 아무 작업도 수행하지 않습니다. 결과가 false이거나 널이면 `ASSERT_FAILURE` 예외가 발생합니다.

```sql
ASSERT quantity > 0, 'quantity must be positive';
```

> 참고: `ASSERT`는 절대 실패하지 않아야 하는 조건을 검사하기 위한 것입니다; 프로그래머가 실수했음을 나타내며 일반적인 데이터 또는 사용자 오류 보고에는 사용하지 마세요.

---

### 43.10 트리거 함수 (Trigger Functions)

PL/pgSQL은 트리거 함수를 정의하는 데 사용할 수 있습니다. 트리거 함수는 `CREATE FUNCTION` 명령을 사용하여 생성되며, 인자 없이 반환 타입 `trigger`(행 수준 트리거의 경우) 또는 `event_trigger`(이벤트 트리거의 경우)를 반환합니다.

#### 43.10.1 행 수준 트리거에 대한 데이터 변경 트리거

트리거 함수가 실행될 때, PL/pgSQL 런타임 환경에서는 자동으로 여러 특수 변수가 생성됩니다:

| 변수 | 설명 |
|------|------|
| `NEW` | `INSERT`/`UPDATE` 작업에 대한 행의 새 데이터베이스 행 (`RECORD` 타입) |
| `OLD` | `UPDATE`/`DELETE` 작업에 대한 행의 이전 데이터베이스 행 (`RECORD` 타입) |
| `TG_NAME` | 실제로 발생된 트리거의 이름 (`name` 타입) |
| `TG_WHEN` | 트리거 정의에 따라 `BEFORE`, `AFTER`, `INSTEAD OF` 중 하나 (`text` 타입) |
| `TG_LEVEL` | 트리거 정의에 따라 `ROW` 또는 `STATEMENT` (`text` 타입) |
| `TG_OP` | 트리거가 발생된 작업: `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` 중 하나 (`text` 타입) |
| `TG_RELID` | 트리거 호출을 유발한 테이블의 객체 ID (`oid` 타입) |
| `TG_RELNAME` | 트리거 호출을 유발한 테이블의 이름 (`name` 타입, 더 이상 사용되지 않음, `TG_TABLE_NAME` 사용 권장) |
| `TG_TABLE_NAME` | 트리거 호출을 유발한 테이블의 이름 (`name` 타입) |
| `TG_TABLE_SCHEMA` | 트리거 호출을 유발한 테이블의 스키마 (`name` 타입) |
| `TG_NARGS` | `CREATE TRIGGER` 문에서 트리거 함수에 주어진 인자 수 (`integer` 타입) |
| `TG_ARGV[]` | `CREATE TRIGGER` 문의 인자, 인덱스는 0부터 시작 (`text` 배열) |

행 수준 트리거 함수의 반환값은 작업 종류와 트리거 타이밍에 따라 의미가 달라집니다:

- `BEFORE` 트리거: `NULL`을 반환하면 해당 행에 대한 작업이 건너뜁니다. 행 값을 반환하면 그 값이 실제로 삽입되거나 업데이트되는 행으로 사용됩니다.
- `AFTER` 트리거: 반환값은 무시됩니다.
- `INSTEAD OF` 트리거: `NULL`을 반환하면 뷰의 해당 행에 대한 작업이 건너뜁니다.

```sql
-- 예: 직원 급여가 변경되면 emp_audit 테이블에 항목을 삽입하는 트리거
CREATE TABLE emp (
    empname           text NOT NULL,
    salary            integer
);

CREATE TABLE emp_audit(
    operation         char(1)   NOT NULL,
    stamp             timestamp NOT NULL,
    userid            text      NOT NULL,
    empname           text      NOT NULL,
    salary            integer
);

CREATE OR REPLACE FUNCTION process_emp_audit() RETURNS TRIGGER AS $emp_audit$
    BEGIN
        --
        -- emp_audit에 emp에 대한 작업을 반영하는 행을 만듭니다
        -- 작업 타입을 결정하기 위해 특수 변수 TG_OP를 사용합니다
        --
        IF (TG_OP = 'DELETE') THEN
            INSERT INTO emp_audit SELECT 'D', now(), user, OLD.*;
            RETURN OLD;
        ELSIF (TG_OP = 'UPDATE') THEN
            INSERT INTO emp_audit SELECT 'U', now(), user, NEW.*;
            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
            INSERT INTO emp_audit SELECT 'I', now(), user, NEW.*;
            RETURN NEW;
        END IF;
        RETURN NULL; -- AFTER 트리거이므로 결과는 무시됨
    END;
$emp_audit$ LANGUAGE plpgsql;

CREATE TRIGGER emp_audit
AFTER INSERT OR UPDATE OR DELETE ON emp
    FOR EACH ROW EXECUTE FUNCTION process_emp_audit();
```

#### 43.10.2 문 수준 트리거에 대한 데이터 변경 트리거

문 수준 트리거는 `NEW` 또는 `OLD`를 가지지 않지만, `AFTER` 트리거와 관련된 모든 행에 액세스하려면 전환 테이블(transition tables)을 사용할 수 있습니다.

```sql
CREATE OR REPLACE FUNCTION check_account_update() RETURNS TRIGGER AS $$
DECLARE
    new_balance NUMERIC;
BEGIN
    -- 전환 테이블을 쿼리하여 새 잔액의 합계를 계산
    SELECT SUM(balance) INTO new_balance FROM new_table;

    -- 모든 잔액이 0 이상인지 확인
    IF new_balance < 0 THEN
        RAISE EXCEPTION 'Total balance cannot be negative';
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_balance
AFTER UPDATE ON accounts
REFERENCING NEW TABLE AS new_table
FOR EACH STATEMENT EXECUTE FUNCTION check_account_update();
```

#### 43.10.3 이벤트 트리거 (Event Triggers)

이벤트 트리거는 `CREATE TABLE`이나 `DROP INDEX`와 같은 DDL 문이 실행될 때 호출됩니다. 이벤트 트리거 함수의 반환 타입은 `event_trigger`입니다.

이벤트 트리거 함수에서 사용 가능한 특수 변수:

| 변수 | 설명 |
|------|------|
| `TG_EVENT` | 함수가 호출된 이벤트 (`text` 타입) |
| `TG_TAG` | 함수가 호출된 명령 태그 (`text` 타입) |

```sql
CREATE OR REPLACE FUNCTION log_ddl() RETURNS event_trigger AS $$
BEGIN
    RAISE NOTICE 'DDL command: %', TG_TAG;
END;
$$ LANGUAGE plpgsql;

CREATE EVENT TRIGGER log_ddl_trigger
ON ddl_command_end
EXECUTE FUNCTION log_ddl();
```

---

### 43.11 PL/pgSQL 언더 더 후드 (PL/pgSQL Under the Hood)

#### 43.11.1 변수 대체 (Variable Substitution)

PL/pgSQL 인터프리터가 함수를 처리할 때 SQL 문과 표현식에서 PL/pgSQL 변수 이름을 찾아 쿼리 매개변수로 대체합니다.

```sql
-- 이 함수에서:
CREATE FUNCTION logfunc(logtxt text) RETURNS void AS $$
    DECLARE
        curtime timestamp := now();
    BEGIN
        INSERT INTO logtable VALUES (logtxt, curtime);
    END;
$$ LANGUAGE plpgsql;

-- INSERT는 다음과 같이 처리됩니다:
INSERT INTO logtable VALUES ($1, $2);
```

#### 43.11.2 계획 캐싱 (Plan Caching)

PL/pgSQL 인터프리터는 함수 내의 개별 SQL 문에 대한 실행 계획을 캐시합니다. 계획은 함수가 처음 실행될 때 각 문에 대해 준비되고 이후 실행에서 재사용됩니다.

이로 인해 성능이 크게 향상되지만, 기반이 되는 데이터베이스 객체가 변경되면(예: 인덱스 추가, 테이블 변경) 캐시된 계획이 최적이 아닐 수 있습니다. 이런 경우 PostgreSQL은 자동으로 계획을 무효화합니다.

몇 가지 주의점:

1. 같은 세션 내에서 여러 번 호출되는 함수의 경우 처음 실행이 계획 생성으로 인해 약간 느릴 수 있습니다.
2. 매개변수 값에 따라 최적 계획이 달라질 수 있는 경우 일반 계획(generic plan)이 사용됩니다.

동적 SQL(`EXECUTE`)을 사용하면 매번 계획을 새로 만들므로 이러한 문제를 피할 수 있지만 오버헤드가 발생합니다.

---

### 43.12 개발 팁 (Tips for Developing in PL/pgSQL)

#### 디버깅을 위한 인용 사용

PL/pgSQL 코드를 작성할 때 달러 인용(dollar quoting)을 사용하는 것이 좋습니다:

```sql
CREATE OR REPLACE FUNCTION testfunc(integer) RETURNS integer AS $PROC$
....
$PROC$ LANGUAGE plpgsql;
```

`$PROC$`와 같은 태그를 사용하면 중첩된 달러 인용이 가능합니다.

#### 추가 검사 활성화

`plpgsql.extra_warnings` 및 `plpgsql.extra_errors` 구성 매개변수를 사용하여 컴파일 타임에 추가 검사를 활성화할 수 있습니다:

```sql
SET plpgsql.extra_warnings TO 'all';
-- 또는
SET plpgsql.extra_warnings TO 'shadowed_variables';
```

사용 가능한 검사:
- `shadowed_variables`: 외부 블록의 변수를 가리는 내부 블록의 변수 선언 경고
- `strict_multi_assignment`: 복수 값을 단일 변수에 할당하는 경우 경고
- `too_many_rows`: `SELECT INTO`가 둘 이상의 행을 반환하는 경우 경고

#### RAISE로 디버깅

`RAISE NOTICE` 문은 PL/pgSQL 함수를 디버깅하는 간단하지만 효과적인 방법입니다:

```sql
CREATE FUNCTION debug_example(x integer) RETURNS integer AS $$
DECLARE
    result integer;
BEGIN
    RAISE NOTICE 'Input value: %', x;

    result := x * 2;
    RAISE NOTICE 'After multiplication: %', result;

    result := result + 10;
    RAISE NOTICE 'After addition: %', result;

    RETURN result;
END;
$$ LANGUAGE plpgsql;
```

---

### 요약 (Summary)

PL/pgSQL은 PostgreSQL의 강력한 절차적 언어로, 다음과 같은 주요 기능을 제공합니다:

1. 블록 구조: `DECLARE`, `BEGIN`, `EXCEPTION`, `END`를 사용한 명확한 코드 구조화
2. 변수와 타입: SQL 데이터 타입, `%TYPE`, `%ROWTYPE`, `RECORD` 지원
3. 제어 구조: `IF/ELSIF/ELSE`, `CASE`, `LOOP`, `WHILE`, `FOR`, `FOREACH`
4. 쿼리 통합: SQL 문을 직접 포함하고 결과를 변수에 저장
5. 커서: 대량 데이터 집합의 효율적인 처리
6. 오류 처리: `EXCEPTION` 블록과 `RAISE` 문을 통한 견고한 오류 관리
7. 트리거: 데이터 변경 및 DDL 이벤트에 대한 자동화된 응답
8. 트랜잭션 제어: 프로시저에서의 `COMMIT`/`ROLLBACK` 지원

PL/pgSQL을 사용하면 복잡한 비즈니스 로직을 데이터베이스 내에서 직접 구현하여 클라이언트-서버 통신 오버헤드를 줄이고, 데이터 무결성을 보장하며, 재사용 가능한 코드를 작성할 수 있습니다.

---

## PL/Tcl - Tcl 절차적 언어 (PL/Tcl - Tcl Procedural Language)

### 목차
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

### 1. 개요 (Overview)

#### 1.1 PL/Tcl이란?

PL/Tcl 은 PostgreSQL을 위한 로드 가능한 절차적 언어(loadable procedural language)로, PostgreSQL 함수와 프로시저 작성에 [Tcl 언어](https://www.tcl.tk/)를 사용할 수 있게 해줍니다. PL/Tcl은 C 언어 함수의 대부분의 기능을 제공하면서 Tcl의 강력한 문자열 처리 라이브러리의 이점을 추가로 제공합니다.

#### 1.2 주요 특징

보안상의 이점:
- 안전한 Tcl 인터프리터(safe Tcl interpreter) 컨텍스트 내에서 실행됩니다
- 제한된 명령 집합과 제한된 데이터베이스 접근만 가능합니다
- 특정 SPI 명령과 `elog()` 메시지 기능만 사용 가능합니다
- 데이터베이스 서버 내부나 OS 수준 권한에 대한 접근이 없습니다
- 비권한 데이터베이스 사용자도 이 언어를 안전하게 사용할 수 있습니다

#### 1.3 구현 제한사항

1. 새로운 데이터 타입에 대한 입출력 함수(input/output functions) 생성에 사용할 수 없습니다
2. 안전한 Tcl 명령 집합으로 제한됩니다

#### 1.4 PL/TclU (비신뢰 Tcl 변형)

제한 없는 Tcl 기능(예: 이메일 전송)이 필요한 경우 PL/TclU 를 사용합니다:
- 안전 모드 대신 전체 Tcl 인터프리터를 사용합니다
- 비신뢰 언어(untrusted language)로 설치해야 합니다 - 슈퍼유저만 함수를 생성할 수 있습니다
- 전체 시스템 접근이 가능하므로 함수 작성자가 보안을 보장해야 합니다
- 관리자 수준의 권한을 갖습니다

#### 1.5 설치

공유 객체 코드(shared object code)는 설치 시 Tcl 지원이 설정되면 자동으로 빌드됩니다.

데이터베이스에 설치하려면:

```sql
CREATE EXTENSION pltcl;    -- 안전한 PL/Tcl용
CREATE EXTENSION pltclu;   -- 비신뢰 PL/TclU용
```

---

### 2. PL/Tcl 함수와 인수 (PL/Tcl Functions and Arguments)

#### 2.1 기본 함수 생성

PL/Tcl 함수는 표준 SQL 구문을 사용하여 생성합니다:

```sql
CREATE FUNCTION funcname (argument-types) RETURNS return-type AS $$
    # PL/Tcl 함수 본문
$$ LANGUAGE pltcl;
```

PL/TclU의 경우 언어 지정만 다르게 `pltclu`를 사용합니다.

#### 2.2 함수 인수

- 함수 인수는 `$1`, `$2` 등의 이름을 가진 Tcl 변수로 전달됩니다
- 결과는 표준 Tcl `return` 문을 사용하여 반환됩니다
- 프로시저에서는 Tcl 코드의 반환 값이 무시됩니다

#### 2.3 간단한 함수 예제

```sql
CREATE FUNCTION tcl_max(integer, integer) RETURNS integer AS $$
    if {$1 > $2} {return $1}
    return $2
$$ LANGUAGE pltcl STRICT;
```

`STRICT` 절은 null 인수로 함수가 호출되는 것을 방지합니다.

#### 2.4 NULL 값 처리

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

#### 2.5 복합 타입 인수 (Composite-Type Arguments)

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

#### 2.6 복합 타입 반환

예상 결과 타입과 일치하는 열 이름/값 쌍의 리스트를 반환합니다:

```sql
CREATE FUNCTION square_cube(in int, out squared int, out cubed int) AS $$
    return [list squared [expr {$1 * $1}] cubed [expr {$1 * $1 * $1}]]
$$ LANGUAGE pltcl;
```

배열 기반으로 반환할 때는 `array get`을 사용합니다:

```sql
CREATE FUNCTION raise_pay(employee, delta int) RETURNS employee AS $$
    set 1(salary) [expr {$1(salary) + $2}]
    return [array get 1]
$$ LANGUAGE pltcl;
```

#### 2.7 집합 반환 (Returning Sets)

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

### 3. PL/Tcl의 데이터 값 (Data Values in PL/Tcl)

#### 3.1 인수 값

PL/Tcl 함수에 전달되는 입력 인수는 텍스트 형식으로 변환됩니다. 이 변환 결과는 `SELECT` 문으로 값을 출력했을 때와 동일합니다.

#### 3.2 반환 값

`return`과 `return_next` 명령은 다음에 대해 유효한 입력 형식인 모든 문자열을 허용합니다:
- 함수의 선언된 결과 타입
- 또는 복합 결과 타입의 지정된 열

#### 3.3 요약

PL/Tcl에서 데이터 값은 텍스트 변환을 통해 처리됩니다:
- 입력 → 텍스트: 함수 인수는 텍스트 표현으로 변환됩니다
- 텍스트 → 출력: return 문은 예상 출력 타입의 입력 형식에 맞는 텍스트 문자열을 허용합니다

값은 텍스트 문자열로 교환되며, PostgreSQL이 텍스트 표현과 실제 데이터 타입 간의 타입 변환을 처리합니다.

---

### 4. PL/Tcl의 전역 데이터 (Global Data in PL/Tcl)

#### 4.1 개요

PL/Tcl의 전역 데이터를 사용하면 함수 호출 간에 데이터를 유지하거나 여러 함수 간에 공유할 수 있습니다. 단, 보안 및 범위와 관련된 중요한 제약사항이 있습니다.

#### 4.2 주요 보안 제한

SQL 역할(Role)별 인터프리터 분리:
- 보안상의 이유로 각 SQL 역할마다 별도의 Tcl 인터프리터를 갖습니다
- 이는 사용자 PL/Tcl 함수 간의 우발적이거나 악의적인 간섭을 방지합니다
- 전역 Tcl 변수는 동일한 SQL 역할에 의해 실행되는 함수 간에만 공유됩니다

역할 간 데이터 공유:
애플리케이션이 여러 SQL 역할에서 코드를 실행하는 경우(`SECURITY DEFINER` 함수, `SET ROLE` 등을 통해):
- 통신해야 하는 함수가 동일한 사용자가 소유하도록 해야 합니다
- `SECURITY DEFINER`로 표시해야 합니다
- 이러한 함수가 오용되지 않도록 주의해야 합니다

#### 4.3 PL/TclU vs PL/Tcl

PL/TclU 함수:
- 모든 함수가 동일한 Tcl 인터프리터에서 실행됩니다(세션 내에서 공유)
- 전역 데이터가 PL/TclU 함수 간에 자동으로 공유됩니다
- 모든 PL/TclU 함수가 슈퍼유저 신뢰 수준에서 실행되므로 보안 위험으로 간주되지 않습니다

#### 4.4 GD 배열 사용 (권장 방식)

특별한 전역 배열 `GD`가 `upvar` 명령을 통해 각 함수에 제공됩니다:
- 전역 이름: 함수의 내부 이름
- 로컬 이름: `GD`
- 목적: 개별 함수에 대한 영구 프라이빗 데이터 저장
- 범위: 특정 인터프리터 내에서만 전역(보안 제한 준수)

모범 사례:
- 함수의 영구 프라이빗 데이터에는 `GD`를 사용합니다
- 여러 함수 간에 특별히 공유하려는 값에만 일반 Tcl 전역 변수를 사용합니다

---

### 5. PL/Tcl에서 데이터베이스 접근 (Database Access from PL/Tcl)

#### 5.1 사용 가능한 명령

##### 5.1.1 spi_exec - SQL 명령 실행

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

##### 5.1.2 spi_prepare - 쿼리 계획 준비

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

##### 5.1.3 spi_execp - 준비된 계획 실행

구문:
```tcl
spi_execp ?-count n? ?-array name? ?-nulls string? queryid ?value-list? ?loop-body?
```

설명:
- `spi_prepare`로 이전에 준비한 쿼리를 실행합니다
- 쿼리/매개변수 지정을 제외하고 `spi_exec`와 동일하게 작동합니다

옵션:
- `-count n`, `-array name`, `loop-body`: `spi_exec`와 동일
- `-nulls string`: 어떤 매개변수가 null인지 나타내는, 공백과 `n` 문자로 구성된 문자열. `value-list`의 길이와 일치해야 합니다

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

##### 5.1.4 subtransaction - SQL 서브트랜잭션

구문:
```tcl
subtransaction command
```

설명:
- SQL 서브트랜잭션 내에서 Tcl 스크립트를 실행합니다
- 스크립트가 오류를 반환하면 전체 서브트랜잭션이 롤백됩니다
- 오류가 외부 트랜잭션에 영향을 미치는 것을 방지합니다

##### 5.1.5 quote - 문자열 리터럴 이스케이프

구문:
```tcl
quote string
```

설명:
- 모든 작은따옴표와 백슬래시 문자를 두 번 반복합니다
- `spi_exec` 또는 `spi_prepare`의 SQL 명령에서 문자열을 안전하게 따옴표 처리합니다

예제:
```tcl
# quote 없이: "SELECT 'doesn't' AS ret" → 파싱 오류
# quote 사용:
"SELECT '[quote $val]' AS ret"
# 결과: "SELECT 'doesn''t' AS ret"
```

##### 5.1.6 elog - 로깅 및 에러 메시지

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

#### 5.2 주요 기능 요약

| 기능 | 설명 |
|------|------|
| 매개변수 지원 | 매개변수화된 쿼리를 사용하여 SQL 인젝션 방지 |
| 배열 저장 | 배열 인덱스를 통해 결과 열에 접근 |
| 루프 처리 | 여러 결과 행을 반복 |
| NULL 처리 | NULL 열 값의 자동 unset |
| 준비된 계획 | 성능 향상을 위한 쿼리 계획 재사용 |
| 안전한 이스케이프 | SQL 인젝션 방지를 위한 내장 함수 |

---

### 6. PL/Tcl 트리거 함수 (Trigger Functions in PL/Tcl)

#### 6.1 개요

PL/Tcl의 트리거 함수는 인수 없이 선언하고 반환 타입이 `trigger`여야 합니다.

#### 6.2 트리거 변수

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

#### 6.3 반환 값

트리거 함수는 다음 중 하나를 반환해야 합니다:

- `OK`: 작업이 정상적으로 진행됩니다
- `SKIP`: 이 행에 대한 작업을 조용히 억제합니다
- 열 이름/값 쌍의 리스트: 수정된 행을 반환합니다(행 수준 `BEFORE INSERT/UPDATE` 또는 `INSTEAD OF INSERT/UPDATE` 트리거에만 의미 있음)

> 팁: 배열 표현을 리스트 형식으로 변환하려면 `array get`을 사용합니다

#### 6.4 예제: 업데이트 카운터 트리거

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

### 7. PL/Tcl 이벤트 트리거 함수 (Event Trigger Functions in PL/Tcl)

#### 7.1 개요

이벤트 트리거 함수는 PL/Tcl로 작성할 수 있습니다. 이벤트 트리거로 호출될 함수는 다음 조건을 만족하도록 선언해야 합니다:
- 인수 없음
- 반환 타입: `event_trigger`

#### 7.2 사용 가능한 변수

트리거 관리자가 다음 변수를 통해 함수 본문에 정보를 전달합니다:

| 변수 | 설명 |
|------|------|
| `$TG_event` | 트리거가 실행된 이벤트 이름 |
| `$TG_tag` | 트리거가 실행된 명령 태그 |

#### 7.3 반환 값

트리거 함수의 반환 값은 무시됩니다.

#### 7.4 예제

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

### 8. PL/Tcl 에러 처리 (Error Handling in PL/Tcl)

#### 8.1 개요

PL/Tcl 함수 내의 Tcl 코드는 잘못된 작업이나 명시적 오류 명령을 통해 오류를 발생시킬 수 있습니다. 오류는 Tcl의 `catch` 명령으로 잡거나, 잡히지 않으면 SQL 오류로 전파됩니다.

#### 8.2 오류 원인과 전파

Tcl 생성 오류:
- 잘못된 작업
- Tcl `error` 명령
- PL/Tcl `elog` 명령
- Tcl의 `catch` 명령으로 잡을 수 있음

SQL 오류:
- PL/Tcl의 `spi_exec`, `spi_prepare`, `spi_execp` 명령 내에서 발생
- Tcl 오류로 보고됨(`catch`로 잡을 수 있음)
- 각 명령은 오류 시 롤백되는 서브트랜잭션에서 실행됨

잡히지 않은 오류는 최상위 수준으로 전파되어 호출하는 쿼리에서 SQL 오류로 보고됩니다.

#### 8.3 오류 정보: errorCode 변수

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

#### 8.4 예제: errorCode 작업

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

### 9. PL/Tcl의 명시적 서브트랜잭션 (Explicit Subtransactions in PL/Tcl)

#### 9.1 개요

PL/Tcl의 명시적 서브트랜잭션은 여러 데이터베이스 작업이 단일 단위로 함께 성공하거나 실패해야 할 때 데이터 일관성을 유지하는 수단입니다.

#### 9.2 문제점

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

#### 9.3 해결책: `subtransaction` 명령

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

#### 9.4 핵심 사항

- 오류를 처리하고 전파되는 것을 방지하려면 `catch`가 여전히 필요합니다
- 모든 오류에서 롤백 트리거: 데이터베이스 오류와 일반 Tcl 예외 모두 롤백을 유발합니다
- 비오류 종료는 롤백하지 않음: 서브트랜잭션 내에서 `return`을 사용해도 롤백이 트리거되지 않습니다
- subtransaction 명령 자체는 오류를 트랩하지 않습니다 - 원자적 롤백 동작만 보장합니다

---

### 10. 트랜잭션 관리 (Transaction Management)

#### 10.1 개요

최상위 수준에서 호출된 PL/Tcl 프로시저 또는 익명 코드 블록(`DO` 명령)에서 두 가지 명령을 사용하여 트랜잭션을 제어할 수 있습니다:

- `commit`: 현재 트랜잭션을 커밋합니다
- `rollback`: 현재 트랜잭션을 롤백합니다

#### 10.2 중요 참고사항

1. SQL 명령 사용 불가: `spi_exec` 또는 유사한 함수를 통해 SQL `COMMIT` 또는 `ROLLBACK` 명령을 사용할 수 없습니다. 대신 `commit` 및 `rollback` PL/Tcl 명령을 사용해야 합니다.

2. 자동 트랜잭션 시작: 트랜잭션이 종료된 후 새 트랜잭션이 자동으로 시작됩니다. 새 트랜잭션을 시작하는 별도의 명령이 필요하지 않습니다.

3. 서브트랜잭션 제한: 명시적 서브트랜잭션이 활성화되어 있으면 트랜잭션을 종료할 수 없습니다.

#### 10.3 예제

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

### 11. PL/Tcl 설정 (PL/Tcl Configuration)

#### 11.1 설정 매개변수

##### 11.1.1 pltcl.start_proc (문자열)

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

##### 11.1.2 pltclu.start_proc (문자열)

목적: `pltcl.start_proc`과 동일하지만 PL/TclU 에 적용됩니다.

- 참조된 함수는 `pltclu` 언어로 작성되어야 합니다
- 다른 모든 동작과 제한은 `pltcl.start_proc`과 동일합니다

---

### 12. Tcl 프로시저 이름 (Tcl Procedure Names)

#### 12.1 개요

PostgreSQL의 PL/Tcl에서 Tcl 프로시저 이름은 Tcl이 고유한 프로시저 이름을 요구하기 때문에 일반 PostgreSQL 함수와 다르게 관리됩니다.

#### 12.2 핵심 사항

##### 12.2.1 이름 생성 전략

PL/Tcl은 다음과 같이 내부 Tcl 프로시저 이름을 생성합니다:
1. 내부 Tcl 프로시저 이름에 인수 타입 이름 포함
2. 동일한 Tcl 인터프리터에서 이전에 로드된 함수 간의 고유성을 보장하기 위해 필요시 함수의 객체 ID(OID) 추가

##### 12.2.2 함수 오버로딩 처리

- PostgreSQL은 다른 스키마나 다른 인수 타입/개수로 동일한 함수 이름을 허용합니다
- Tcl은 모든 프로시저 이름이 구별되어야 합니다
- PL/Tcl은 이름이 같지만 인수 타입이 다른 PostgreSQL 함수에 대해 다른 Tcl 프로시저를 생성하여 이를 해결합니다

##### 12.2.3 중요 제한: 직접 함수 호출

PL/Tcl 함수는 Tcl 코드 내에서 다른 PL/Tcl 함수를 직접 호출할 수 없습니다.

다른 PL/Tcl 함수를 호출해야 하는 경우 SQL을 통해 다음을 사용해야 합니다:
- `spi_exec` 명령, 또는
- 관련 SPI(서버 프로그래밍 인터페이스) 명령

#### 12.3 실질적 영향

- 내부 Tcl 프로시저 이름 지정은 일반적으로 PL/Tcl 프로그래머에게 투명합니다
- 디버깅 세션 중에 보일 수 있습니다
- 개발자는 함수 간 직접 호출 제한을 인지하고 대신 SQL 중개자를 사용해야 합니다

---

### 참고 자료 (References)

- [PostgreSQL 공식 문서 - PL/Tcl](https://www.postgresql.org/docs/current/pltcl.html)
- [Tcl 공식 웹사이트](https://www.tcl.tk/)
- [PostgreSQL 공식 문서 - 에러 코드](https://www.postgresql.org/docs/current/errcodes-appendix.html)

---

## PL/Perl - Perl 절차적 언어 (Perl Procedural Language)

### 목차
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

### 1. 개요

PL/Perl은 PostgreSQL 함수와 프로시저를 [Perl 프로그래밍 언어](https://www.perl.org)로 작성할 수 있게 해주는 로드 가능한 절차적 언어(loadable procedural language)입니다.

#### 1.1 주요 장점

PL/Perl의 주요 장점은 저장 함수(stored functions)와 프로시저 안에서 Perl의 광범위한 "문자열 처리(string munging)" 연산자와 함수를 활용할 수 있다는 점입니다. 복잡한 문자열 파싱(parsing)은 PL/pgSQL의 문자열 함수와 제어 구조보다 Perl로 처리하는 편이 훨씬 쉬울 수 있습니다.

#### 1.2 설치

특정 데이터베이스에 PL/Perl을 설치하려면 다음 명령을 사용합니다:

```sql
CREATE EXTENSION plperl;
```

팁: `template1`에 언어를 설치하면 이후에 생성되는 모든 데이터베이스에 자동으로 설치됩니다.

참고:
- 소스 패키지 사용자는 설치 과정에서 PL/Perl 빌드를 특별히 활성화해야 합니다 (Chapter 17: 소스 코드로부터 설치 참조).
- 바이너리 패키지 사용자는 별도의 서브패키지에서 PL/Perl을 찾을 수 있습니다.

---

### 2. PL/Perl 함수와 인수 (PL/Perl Functions and Arguments)

#### 2.1 기본 함수 생성

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

#### 2.2 익명 코드 블록 (Anonymous Code Blocks)

```sql
DO $$
    # PL/Perl 코드
$$ LANGUAGE plperl;
```

익명 블록은 인수를 받지 않으며 반환 값은 무시됩니다.

#### 2.3 인수 처리

##### NULL 값 처리

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

##### 타입 변환 (Type Conversion)

- 인수는 데이터베이스 인코딩에서 UTF-8로 변환됩니다
- 비참조(non-reference) 인수는 PostgreSQL 외부 텍스트 표현 형식의 문자열입니다
- `bytea` 타입 변환에는 `decode_bytea()`를 사용합니다
- 반환 값은 외부 텍스트 표현 형식이어야 합니다

##### 불리언 값 (Boolean Values)

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

#### 2.4 배열 작업 (Working with Arrays)

##### 배열 반환

```sql
CREATE OR REPLACE FUNCTION returns_array()
RETURNS text[][] AS $$
    return [['a"b','c,d'],['e\\f','g']];
$$ LANGUAGE plperl;
```

##### 배열 수신 (Blessed Objects로)

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

#### 2.5 복합 타입 (Composite Types)

##### 복합 타입 인수 수신

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

##### 복합 타입 결과 반환

```sql
CREATE TYPE testrowperl AS (f1 integer, f2 text, f3 text);

CREATE OR REPLACE FUNCTION perl_row() RETURNS testrowperl AS $$
    return {f2 => 'hello', f1 => 1, f3 => 'world'};
$$ LANGUAGE plperl;
```

#### 2.6 집합 반환 (Returning Sets)

##### return_next 사용 (대용량 집합에 권장)

```sql
CREATE OR REPLACE FUNCTION perl_set_int(int)
RETURNS SETOF INTEGER AS $$
    foreach (0..$_[0]) {
        return_next($_);
    }
    return undef;
$$ LANGUAGE plperl;
```

##### 배열 참조 반환 (소규모 집합용)

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

#### 2.7 프로시저의 출력 매개변수 (Output Parameters in Procedures)

```sql
CREATE PROCEDURE perl_triple(INOUT a integer, INOUT b integer) AS $$
    my ($a, $b) = @_;
    return {a => $a * 3, b => $b * 3};
$$ LANGUAGE plperl;
```

#### 2.8 Pragma 사용

##### Strict 모드

```sql
-- 함수별 설정
use strict;

-- 전역 설정 (임시)
SET plperl.use_strict to true;

-- 전역 설정 (영구 - postgresql.conf)
plperl.use_strict = true
```

##### Feature Pragma

Perl 5.10.0 이상에서 사용 가능합니다.

#### 2.9 중요 참고사항

- 중첩된 명명 서브루틴(nested named subroutines)은 위험합니다; 대신 익명 서브루틴을 사용하세요
- 함수 본문에는 달러 따옴표(`$$...$$`)를 권장합니다
- 인수는 자동으로 UTF-8로/에서 변환됩니다
- 누락된 복합 타입 속성은 NULL로 반환됩니다

---

### 3. PL/Perl의 데이터 값 (Data Values in PL/Perl)

#### 3.1 핵심 개념

##### 인수 및 반환 값 처리

- PL/Perl 함수에 전달되는 입력 인수는 텍스트 형식으로 변환됩니다 (`SELECT` 문에 표시되는 것과 동일)
- `return` 및 `return_next` 명령은 함수에 선언된 반환 타입의 허용 입력 형식에 해당하는 모든 문자열을 받아들입니다

#### 3.2 타입별 변환 규칙

| PostgreSQL 타입 | Perl 표현 |
|----------------|----------|
| `integer`, `numeric` | 숫자 문자열 |
| `text`, `varchar` | 문자열 |
| `boolean` | `'t'` 또는 `'f'` (기본) |
| `bytea` | 이스케이프된 문자열 (decode_bytea 사용) |
| `NULL` | `undef` |
| 배열 | Blessed 배열 참조 |
| 복합 타입 | 해시 참조 |

#### 3.3 변환 모듈 (Transform Modules)

텍스트 변환이 불편한 경우, PostgreSQL은 이 동작을 개선할 수 있는 변환 모듈을 제공합니다.

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

### 4. 내장 함수 (Built-in Functions)

#### 4.1 데이터베이스 접근 (Database Access from PL/Perl)

##### 4.1.1 spi_exec_query()

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

##### 4.1.2 spi_query()와 spi_fetchrow()

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

##### 4.1.3 준비된 쿼리 (Prepared Queries)

번호가 매겨진 인수 플레이스홀더를 사용하는 준비된 쿼리(prepared query)를 지원합니다.

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

##### 4.1.4 spi_commit()과 spi_rollback()

현재 트랜잭션을 커밋하거나 롤백합니다. 프로시저 또는 익명 코드 블록(`DO` 명령) 내의 최상위 레벨에서만 호출할 수 있습니다.

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

#### 4.2 유틸리티 함수 (Utility Functions in PL/Perl)

##### 4.2.1 elog()

로그 또는 오류 메시지를 출력합니다.

구문: `elog(level, msg)`

레벨: `DEBUG`, `LOG`, `INFO`, `NOTICE`, `WARNING`, `ERROR`

참고: `ERROR`는 오류 조건을 발생시킵니다. 나머지 레벨은 `log_min_messages` 및 `client_min_messages` 설정 변수에 따라 다양한 우선순위로 메시지를 출력합니다.

##### 4.2.2 quote_literal()

주어진 문자열을 SQL 문자열 리터럴로 사용할 수 있도록 따옴표로 묶어 반환합니다.

구문: `quote_literal(string)`

`undef` 입력에 대해 `undef`를 반환합니다. 인수가 `undef`일 수 있는 경우 `quote_nullable()`을 사용하세요.

##### 4.2.3 quote_nullable()

주어진 문자열을 SQL 문자열 리터럴로 사용할 수 있도록 따옴표로 묶어 반환하거나, 인수가 `undef`인 경우 `"NULL"`을 반환합니다.

구문: `quote_nullable(string)`

##### 4.2.4 quote_ident()

주어진 문자열을 SQL 식별자로 사용할 수 있도록 따옴표로 묶어 반환합니다.

구문: `quote_ident(string)`

비식별자 문자가 포함되거나 대소문자 변환이 필요한 경우에만 따옴표가 추가됩니다.

##### 4.2.5 decode_bytea()

`bytea` 인코딩된 문자열을 이스케이프 해제한 원시 이진 데이터로 반환합니다.

구문: `decode_bytea(string)`

##### 4.2.6 encode_bytea()

이진 데이터를 `bytea` 인코딩 형식으로 변환하여 반환합니다.

구문: `encode_bytea(string)`

##### 4.2.7 encode_array_literal()

배열 내용을 배열 리터럴 형식의 문자열로 반환합니다.

구문: `encode_array_literal(array [, delimiter])`

구분자를 지정하지 않거나 `undef`로 지정하면 기본값인 `", "`가 사용됩니다.

##### 4.2.8 encode_typed_literal()

Perl 변수를 지정된 데이터 타입으로 변환해 문자열 표현으로 반환합니다.

구문: `encode_typed_literal(value, typename)`

중첩 배열과 복합 타입을 올바르게 처리합니다.

##### 4.2.9 encode_array_constructor()

배열 내용을 배열 생성자 형식의 문자열로 반환합니다.

구문: `encode_array_constructor(array)`

개별 값은 `quote_nullable()`로 따옴표 처리됩니다.

##### 4.2.10 looks_like_number()

문자열이 숫자처럼 보이면 true, 그렇지 않으면 false를 반환합니다.

구문: `looks_like_number(string)`

인수가 `undef`이면 `undef`를 반환합니다. 앞뒤 공백은 무시하며, `Inf`와 `Infinity`는 숫자로 취급합니다.

##### 4.2.11 is_array_ref()

인수를 배열 참조로 취급할 수 있으면 true를 반환합니다.

구문: `is_array_ref(argument)`

`ref()`의 반환값이 `ARRAY` 또는 `PostgreSQL::InServer::ARRAY`이면 true를 반환합니다.

---

### 5. 전역 값 (Global Values in PL/Perl)

#### 5.1 개요

전역 해시 `%_SHARED`를 사용하면 현재 세션이 유지되는 동안 함수 호출 간에 코드 참조를 포함한 데이터를 저장할 수 있습니다.

#### 5.2 기본 사용법

##### 단순 데이터 저장 및 검색

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

#### 5.3 코드 참조 저장

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

#### 5.4 보안 및 역할 격리 (Security & Role Isolation)

- 각 SQL 역할(role)은 자체 `%_SHARED` 인스턴스를 가진 별도의 Perl 인터프리터에서 실행됩니다
- 두 PL/Perl 함수는 동일한 SQL 역할로 실행될 때만 같은 `%_SHARED`를 공유합니다
- 이를 통해 사용자 간의 의도치 않은 간섭이나 악의적인 접근을 방지합니다

##### 역할 간 데이터 공유

서로 다른 SQL 역할 간에 데이터를 공유해야 하는 경우:
1. 함수들이 동일한 사용자 소유여야 합니다
2. `SECURITY DEFINER`로 표시해야 합니다
3. 의도치 않은 용도로 오용되지 않도록 해야 합니다

---

### 6. 신뢰(Trusted)와 비신뢰(Untrusted) PL/Perl

#### 6.1 개요

PostgreSQL의 PL/Perl은 서로 다른 보안 수준을 가진 두 가지 변형으로 제공됩니다:

#### 6.2 신뢰 PL/Perl (`plperl`)

- 기본 설치되는 "신뢰" 변형입니다
- 보안 유지를 위해 특정 Perl 작업을 제한합니다
- 권한이 없는 데이터베이스 사용자에게도 안전합니다
- 모든 데이터베이스 사용자가 함수를 생성할 수 있습니다

제한된 작업:
- 파일 핸들 작업
- `require` 및 `use` (외부 모듈용)
- 환경과 상호 작용하는 모든 작업
- 데이터베이스 서버 내부에 접근하거나 OS 수준 접근을 얻을 수 없음

#### 6.3 비신뢰 PL/Perl (`plperlu`)

- 제한 없이 Perl 언어를 전부 사용할 수 있습니다
- 파일 작업, 시스템 호출, 외부 모듈 임포트가 가능합니다
- 데이터베이스 슈퍼유저만 이 언어로 함수를 생성할 수 있습니다
- 메일 전송 등 민감한 작업에 활용됩니다

#### 6.4 보안 경고

> 신뢰 PL/Perl은 보안 유지를 위해 Perl `Opcode` 모듈에 의존합니다. Perl 문서에 따르면 이 모듈이 신뢰 PL/Perl 사용 사례에 완전히 효과적이지 않을 수 있습니다. 보안 요구 사항이 해당 경고의 불확실성을 허용할 수 없는 경우 `REVOKE USAGE ON LANGUAGE plperl FROM PUBLIC` 실행을 고려하세요.

#### 6.5 코드 예제

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

#### 6.6 주요 구현 세부 사항

- 별도의 인터프리터: PL/Perl 함수는 SQL 역할마다 별도의 Perl 인터프리터에서 실행됩니다
- 공유 상태: 세션 내의 모든 PL/PerlU 함수는 단일 Perl 인터프리터를 공유합니다 (데이터 공유 가능)
- 교차 통신 없음: PL/Perl과 PL/PerlU 함수는 서로 통신할 수 없습니다
- Perl 빌드 요구 사항: 여러 인터프리터를 사용하려면 Perl이 `usemultiplicity` 또는 `useithreads`로 컴파일되어야 하며, 그렇지 않으면 세션당 하나의 인터프리터만 사용됩니다

---

### 7. PL/Perl 트리거 (PL/Perl Triggers)

PL/Perl로 트리거 함수를 작성할 수 있습니다. 해시 참조 `$_TD`는 현재 트리거 이벤트 정보를 담으며, 각 트리거 호출마다 별도의 로컬 값을 받는 전역 변수입니다.

#### 7.1 트리거 변수 (`$_TD`)

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

#### 7.2 행 수준 트리거 반환 값 (Row-Level Trigger Return Values)

| 반환 값 | 동작 |
|--------|-----|
| `return;` | 작업 실행 |
| `"SKIP"` | 작업을 실행하지 않음 |
| `"MODIFY"` | NEW 행이 트리거 함수에 의해 수정되었음을 나타냄 |

#### 7.3 예제

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

### 8. PL/Perl 이벤트 트리거 (PL/Perl Event Triggers)

PL/Perl로 이벤트 트리거 함수를 작성할 수 있습니다. 이벤트 트리거 함수는 특별한 해시 참조 `$_TD`를 통해 현재 트리거 이벤트 정보에 접근합니다.

#### 8.1 $_TD 해시 참조

`$_TD`는 각 트리거 호출마다 별도의 로컬 값을 받는 전역 변수로, 다음 필드를 포함합니다:

| 필드 | 설명 |
|-----|------|
| `$_TD->{event}` | 트리거가 발동된 이벤트의 이름 |
| `$_TD->{tag}` | 트리거가 발동된 명령 태그 |

#### 8.2 반환 값

트리거 함수의 반환 값은 무시됩니다.

#### 8.3 예제

```sql
CREATE OR REPLACE FUNCTION perlsnitch() RETURNS event_trigger AS $$
  elog(NOTICE, "perlsnitch: " . $_TD->{event} . " " . $_TD->{tag} . " ");
$$ LANGUAGE plperl;

CREATE EVENT TRIGGER perl_a_snitch
    ON ddl_command_start
    EXECUTE FUNCTION perlsnitch();
```


---

### 9. PL/Perl 내부 구조 (PL/Perl Under the Hood)

#### 9.1 구성 (Configuration)

##### 9.1.1 `plperl.on_init` (string)

Perl 인터프리터가 처음 초기화될 때 `plperl` 또는 `plperlu`로 특수화되기 전에 실행할 Perl 코드를 지정합니다. 이 단계에서는 SPI 함수를 사용할 수 없습니다.

핵심 사항:
- 단일 문자열로만 지정할 수 있으며, 긴 코드는 모듈에 작성하고 로드해야 합니다
- 초기화가 실패하면 인터프리터 초기화 전체가 중단됩니다
- 로드된 모든 모듈은 `plperl`에서도 사용 가능해집니다

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
- `shared_preload_libraries`에 포함된 경우 초기화가 postmaster에서 이루어집니다 (성능 이점이 있으나 보안 위험 존재)
- Windows에서는 postmaster의 Perl 인터프리터가 자식 프로세스로 전파되지 않으므로 사전 로드 이점이 없습니다
- `postgresql.conf` 또는 서버 명령줄에서만 설정할 수 있습니다

##### 9.1.2 `plperl.on_plperl_init` / `plperl.on_plperlu_init` (string)

인터프리터가 각각 `plperl` 또는 `plperlu`로 특수화될 때 실행되는 Perl 코드입니다. 세션에서 첫 번째 함수가 실행될 때나 추가 인터프리터가 생성될 때 호출됩니다.

핵심 사항:
- `plperl.on_init` 실행 이후에 실행됩니다
- 실행 중에는 SPI 함수를 사용할 수 없습니다
- `plperl.on_plperl_init` 코드는 인터프리터 "잠금" 이후에 실행됩니다 (신뢰된 작업만 허용)
- 슈퍼유저만 이 설정을 변경할 수 있습니다
- 변경 사항은 현재 세션에서 이미 사용 중인 인터프리터에는 영향을 미치지 않습니다

##### 9.1.3 `plperl.use_strict` (boolean)

true이면 이후 PL/Perl 함수 컴파일 시 `strict` pragma가 활성화됩니다. 현재 세션에서 이미 컴파일된 함수에는 영향을 미치지 않습니다.

#### 9.2 제한 사항 및 누락된 기능 (Limitations and Missing Features)

1. 직접 함수 호출: PL/Perl 함수끼리 직접 호출할 수 없습니다

2. SPI 구현: SPI가 아직 완전히 구현되지 않았습니다

3. 대용량 데이터 집합:
   - `spi_exec_query`는 전체 결과 집합을 메모리에 로드합니다
   - 대용량 데이터셋에는 `spi_query`/`spi_fetchrow`를 사용하세요
   - 집합 반환 함수에서 대용량 결과를 반환할 때는 `return_next`를 사용해야 합니다

4. 세션 종료:
   - `END` 블록은 세션이 정상 종료될 때 실행됩니다 (치명적 오류 시에는 실행되지 않음)
   - 파일 핸들이 자동으로 플러시되지 않습니다
   - 객체가 자동으로 소멸되지 않습니다

---

### 참고 자료

- [PostgreSQL 공식 문서 - PL/Perl](https://www.postgresql.org/docs/current/plperl.html)
- [Perl 프로그래밍 언어](https://www.perl.org)
- [PostgreSQL 확장 프로그래밍](https://www.postgresql.org/docs/current/extend.html)

---

## PL/Python - Python 절차적 언어 (Python Procedural Language)

### 목차
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

### 1. 개요

PL/Python은 PostgreSQL 함수와 프로시저를 [Python 프로그래밍 언어](https://www.python.org)로 작성할 수 있도록 해주는 로드 가능한 절차적 언어(loadable procedural language)입니다.

#### 1.1 설치

특정 데이터베이스에 PL/Python을 설치하려면 다음 명령을 사용합니다:

```sql
CREATE EXTENSION plpython3u;
```

팁: `template1`에 언어를 설치하면, 이후에 생성되는 모든 데이터베이스에 해당 언어가 자동으로 설치됩니다.

참고:
- 소스 패키지 사용자는 설치 과정에서 PL/Python 빌드를 특별히 활성화해야 합니다.
- 바이너리 패키지 사용자는 별도의 서브패키지에서 PL/Python을 찾을 수 있습니다.

#### 1.2 보안 고려사항

PL/Python은 "비신뢰(untrusted)" 언어입니다:

- 사용자 행동에 대한 제한이 없습니다
- 언어 이름의 "u"는 "untrusted"를 의미합니다 (`plpython3u`)
- 슈퍼유저(superuser)만 비신뢰 언어로 함수를 생성할 수 있습니다
- 데이터베이스 관리자 권한으로 실행되므로 코드가 오용되지 않도록 주의해야 합니다

> 참고: 향후 안전한 실행 메커니즘이 개발되면 신뢰 버전인 `plpython`이 제공될 수 있습니다.

---

### 2. PL/Python 함수 (PL/Python Functions)

#### 2.1 기본 구문

PL/Python 함수는 표준 `CREATE FUNCTION` 구문을 사용하여 생성합니다:

```sql
CREATE FUNCTION funcname (argument-list)
  RETURNS return-type
AS $$
  # PL/Python 함수 본문
$$ LANGUAGE plpython3u;
```

#### 2.2 인수 처리

- 인수는 `args` 리스트의 요소로 전달됩니다
- 이름 있는 인수는 일반 변수로도 전달됩니다 (가독성을 위해 권장)
- 인수는 함수 범위 내에서 전역 변수로 설정됩니다

#### 2.3 반환 값

- 표준 Python `return` 문을 사용합니다
- 결과 집합 문(result-set statements)에는 `yield`를 사용합니다
- 반환 값이 없으면 Python의 `None`이 반환됩니다
- `None`은 SQL `NULL`로 변환됩니다
- 프로시저(Procedures)는 `None`을 반환해야 합니다 (return 없이 종료하거나 인수 없는 `return` 사용)

#### 2.4 기본 예제

```sql
CREATE FUNCTION pymax (a integer, b integer)
  RETURNS integer
AS $$
  if a > b:
    return a
  return b
$$ LANGUAGE plpython3u;
```

#### 2.5 스코핑 규칙 주의사항

Python 스코핑 규칙으로 인해, 인수 변수를 자기 자신을 포함하는 표현식으로 재할당하지 마세요:

##### 작동하지 않는 예:
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  x = x.strip()  -- 오류: x가 로컬 변수가 됨
  return x
$$ LANGUAGE plpython3u;
```

##### 작동하지만 권장하지 않는 방법:
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  global x
  x = x.strip()
  return x
$$ LANGUAGE plpython3u;
```

##### 권장하는 방법 (읽기 전용 매개변수):
```sql
CREATE FUNCTION pystrip(x text)
  RETURNS text
AS $$
  return x.strip()
$$ LANGUAGE plpython3u;
```

---

### 3. 데이터 값 (Data Values)

#### 3.1 데이터 타입 매핑

##### PostgreSQL에서 Python으로 변환

PL/Python 함수가 인수를 받을 때 적용되는 타입 매핑:

| PostgreSQL 타입 | Python 타입 |
|----------------|-------------|
| `boolean` | `bool` |
| `smallint`, `int`, `bigint`, `oid` | `int` |
| `real`, `double precision` | `float` |
| `numeric` | `decimal.Decimal` |
| `bytea` | `bytes` |
| 문자열 타입 및 기타 | `str` (유니코드) |

##### Python에서 PostgreSQL로 반환 변환

- boolean: Python 진리값 규칙에 따라 평가 (0과 빈 문자열은 false, `'f'`는 true)
- bytea: Python 내장 함수를 사용하여 변환
- 기타: `str()` 함수를 통해 문자열로 변환 (float는 정밀도 유지를 위해 `repr()` 사용)
- 문자열: PostgreSQL 서버 인코딩으로 자동 변환

#### 3.2 NULL 처리

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

#### 3.3 배열과 리스트

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

#### 3.4 복합 타입 (Composite Types)

##### 복합 타입 인수 수신

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

##### 복합 타입 반환

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

#### 3.5 집합 반환 함수 (Set-Returning Functions)

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

### 4. 함수 간 데이터 공유 (Sharing Data)

PL/Python은 PostgreSQL 세션 내 함수 호출 간 데이터 공유를 위한 전역 딕셔너리 두 개를 제공합니다:

#### 4.1 SD 딕셔너리 (Private Data)

- 범위: 함수별
- 목적: 동일 함수를 반복 호출하는 사이에 전용 데이터를 저장
- 접근: 해당 함수 내에서만 사용 가능

#### 4.2 GD 딕셔너리 (Public Data)

- 범위: 세션 전체
- 목적: 세션 내 모든 Python 함수가 접근할 수 있는 데이터 저장
- 주의: 전역으로 공유되므로 신중하게 사용

#### 4.3 비교표

| 특성 | SD | GD |
|-----|----|----|
| 범위 | 단일 함수 | 세션의 모든 함수 |
| 지속성 | 동일 함수 호출 간 | 전체 세션 |
| 가시성 | 개인적 | 공용 |
| 사용 사례 | 한 함수 내 캐싱 | 세션 전체 상태 |

#### 4.4 실행 격리

각 PL/Python 함수는 Python 인터프리터에서 자체 실행 환경을 갖습니다:

- 한 함수(예: `myfunc`)의 전역 데이터와 인수는 다른 함수(예: `myfunc2`)에서 사용할 수 없습니다
- 예외: `GD` 딕셔너리에 저장된 데이터는 세션의 모든 함수에서 공유됩니다

---

### 5. 익명 코드 블록 (Anonymous Code Blocks)

PL/Python은 `DO` 문을 통한 익명 코드 블록을 지원합니다. 익명 코드 블록은 함수로 저장되지 않고 즉시 실행되는 이름 없는 코드 블록입니다.

#### 5.1 구문

```sql
DO $$
    # PL/Python 코드
$$ LANGUAGE plpython3u;
```

#### 5.2 주요 특성

- 인수 없음: 익명 코드 블록은 입력 매개변수를 받지 않습니다
- 반환 값 없음: 블록이 반환할 수 있는 모든 값은 무시됩니다
- 동작: 함수처럼 동작하지만 저장되지 않습니다
- 언어: `LANGUAGE plpython3u`를 지정해야 합니다

#### 5.3 사용 사례

- 일회성 데이터 조작 작업
- 관리 작업
- 영구 함수를 만들지 않고 PL/Python 코드 테스트
- 재사용이 필요 없는 절차적 로직 실행

---

### 6. 트리거 함수 (Trigger Functions)

PL/Python에서 함수가 트리거로 사용될 때, `TD`(트리거 데이터) 딕셔너리에 트리거 관련 값이 담겨 트리거 이벤트에 대한 컨텍스트를 제공합니다.

#### 6.1 TD 딕셔너리 내용

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
| `TD["args"]` | `CREATE TRIGGER` 명령의 인수 튜플 (있는 경우) |

#### 6.2 반환 값

BEFORE 또는 INSTEAD OF 행 수준 트리거(`TD["when"]`이 `BEFORE` 또는 `INSTEAD OF`이고 `TD["level"]`이 `ROW`인 경우):

| 반환 값 | 동작 |
|--------|------|
| `None` 또는 `"OK"` | 행이 수정되지 않음 |
| `"SKIP"` | 이벤트 중단 |
| `"MODIFY"` | 행이 수정됨 (`INSERT` 또는 `UPDATE` 이벤트만) |

기타 모든 반환 값은 무시됩니다.

#### 6.3 트리거 예제

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

#### 6.4 감사 로그 트리거 예제

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

### 7. 데이터베이스 접근 (Database Access)

PL/Python 언어 모듈은 데이터베이스 접근 기능을 제공하는 `plpy` 모듈을 자동으로 임포트합니다.

#### 7.1 plpy.execute()

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

참고: 전체 결과 집합을 메모리에 올려 놓습니다. 대용량 데이터셋에는 `plpy.cursor()`를 사용하세요.

#### 7.2 준비된 쿼리 (Prepared Queries)

매개변수화된 쿼리를 위해 재사용 가능한 실행 계획을 준비합니다.

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

#### 7.3 커서 (Cursors)

대용량 결과 집합을 청크 단위로 처리할 수 있는 커서를 반환합니다.

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

#### 7.4 오류 처리

##### 기본 오류 트래핑

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

##### 특정 예외 처리

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

### 8. 명시적 서브트랜잭션 (Explicit Subtransactions)

명시적 서브트랜잭션은 데이터베이스 작업을 원자적으로 처리하므로, 모든 작업이 성공하거나 모두 롤백됨을 보장합니다.

#### 8.1 문제점

서브트랜잭션 없이 일련의 데이터베이스 작업 도중 오류가 발생하면, 일부 변경사항은 커밋되고 나머지는 실패할 수 있습니다:

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

두 번째 UPDATE가 실패하면 Joe의 계정에서는 출금됐지만 Mary의 계정에는 입금되지 않아 데이터가 불일치하게 됩니다.

#### 8.2 해결책: 서브트랜잭션 컨텍스트 관리자

`plpy.subtransaction()`을 사용하여 데이터베이스 작업을 원자적 블록으로 묶습니다:

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

#### 8.3 핵심 포인트

- 컨텍스트 관리자 인터페이스: `plpy.subtransaction()`은 Python 컨텍스트 관리자 프로토콜을 구현합니다
- 오류 처리: 예외를 처리하려면 여전히 `try`/`except`가 필요합니다
- 원자적 동작: 서브트랜잭션 블록 내의 모든 작업은 함께 성공하거나 함께 실패합니다
- 예외 시 롤백: 예외가 발생하면 서브트랜잭션이 롤백됩니다 (데이터베이스 오류뿐 아니라 일반 Python 예외도 포함)

---

### 9. 트랜잭션 제어 (Transaction Control)

프로시저 또는 익명 코드 블록에서 `plpy.commit()`과 `plpy.rollback()`으로 트랜잭션을 제어할 수 있습니다.

#### 9.1 commit과 rollback

```sql
CREATE PROCEDURE batch_insert() AS $$
    for i in range(100):
        plpy.execute(f"INSERT INTO test_table (value) VALUES ({i})")
        if i % 10 == 9:
            plpy.commit()  # 10개마다 커밋
$$ LANGUAGE plpython3u;

CALL batch_insert();
```

#### 9.2 조건부 트랜잭션 제어

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

### 10. 유틸리티 함수 (Utility Functions)

#### 10.1 로깅 함수

`plpy` 모듈은 다음 로깅 함수를 제공합니다:

| 함수 | 설명 |
|------|------|
| `plpy.debug(msg, **kwargs)` | 디버그 메시지 |
| `plpy.log(msg, **kwargs)` | 로그 메시지 |
| `plpy.info(msg, **kwargs)` | 정보 메시지 |
| `plpy.notice(msg, **kwargs)` | 알림 메시지 |
| `plpy.warning(msg, **kwargs)` | 경고 메시지 |
| `plpy.error(msg, **kwargs)` | 오류 (예외 발생) |
| `plpy.fatal(msg, **kwargs)` | 치명적 오류 (예외 발생) |

핵심 포인트:

- `plpy.error`와 `plpy.fatal`은 호출 쿼리로 전파되어 트랜잭션을 중단시키는 Python 예외를 발생시킵니다
- 대체 구문: `raise plpy.Error(msg)` 및 `raise plpy.Fatal(msg)` (키워드 인수 미지원)
- 나머지 함수는 다양한 우선순위 레벨로 메시지를 생성합니다
- 메시지 가시성은 `log_min_messages` 및 `client_min_messages` 설정 변수로 제어됩니다

#### 10.2 키워드 인수

로깅 함수는 오류 메시지를 보강하기 위해 다음 키워드 인수를 받습니다:

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

#### 10.3 문자열 인용 함수

안전한 동적 SQL 쿼리 구성을 위한 세 가지 유틸리티 함수:

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

이 함수들은 PostgreSQL 내장 인용 함수와 동일하며, 동적 쿼리 구성 시 SQL 인젝션을 방지합니다.

---

### 11. 환경 변수 (Environment Variables)

다음 환경 변수는 PL/Python 동작에 영향을 줄 수 있습니다. 이 변수들은 PostgreSQL 서버 프로세스 환경에서 설정해야 합니다 (예: 시작 스크립트):

#### 11.1 지원되는 환경 변수

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

#### 11.2 중요 참고사항

1. 서버 프로세스 요구사항: 이 변수들은 개별 클라이언트 연결이 아닌 PostgreSQL 서버 프로세스 자체에 적용됩니다
2. 버전 종속성: 사용 가능한 변수는 Python 버전에 따라 다릅니다. 자세한 내용은 Python 문서를 참조하세요
3. 임베디드 인터프리터 제한: `python` man 페이지에 나열된 일부 환경 변수는 명령줄 인터프리터에서만 유효하며, PL/Python이 사용하는 임베디드 Python 인터프리터에서는 작동하지 않을 수 있습니다

가장 흔한 사용 사례로, `PYTHONPATH`는 PL/Python 검색 경로에 사용자 정의 모듈 디렉토리를 추가하는 데 활용됩니다.

---

### 참고 자료

- [PostgreSQL 공식 문서 - PL/Python](https://www.postgresql.org/docs/current/plpython.html)
- [Python 프로그래밍 언어](https://www.python.org)
- [PostgreSQL 확장 프로그래밍](https://www.postgresql.org/docs/current/extend.html)
