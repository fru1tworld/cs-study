# PostgreSQL 데이터 타입 (Data Types)

이 문서는 PostgreSQL 공식 문서의 "Chapter 8. Data Types"를 한국어로 번역한 것입니다.

## 목차

1. [개요](#개요)
2. [숫자 타입 (Numeric Types)](#숫자-타입-numeric-types)
3. [문자 타입 (Character Types)](#문자-타입-character-types)
4. [바이너리 데이터 타입 (Binary Data Types)](#바이너리-데이터-타입-binary-data-types)
5. [날짜/시간 타입 (Date/Time Types)](#날짜시간-타입-datetime-types)
6. [불리언 타입 (Boolean Type)](#불리언-타입-boolean-type)
7. [열거형 타입 (Enumerated Types)](#열거형-타입-enumerated-types)
8. [기하학적 타입 (Geometric Types)](#기하학적-타입-geometric-types)
9. [네트워크 주소 타입 (Network Address Types)](#네트워크-주소-타입-network-address-types)
10. [비트 문자열 타입 (Bit String Types)](#비트-문자열-타입-bit-string-types)
11. [텍스트 검색 타입 (Text Search Types)](#텍스트-검색-타입-text-search-types)
12. [UUID 타입](#uuid-타입)
13. [XML 타입](#xml-타입)
14. [JSON 타입](#json-타입)
15. [배열 (Arrays)](#배열-arrays)
16. [범위 타입 (Range Types)](#범위-타입-range-types)
17. [통화 타입 (Monetary Types)](#통화-타입-monetary-types)

---

## 개요

PostgreSQL은 사용자가 직접 새로운 타입을 추가할 수 있는 풍부한 기본 데이터 타입을 제공합니다. 대부분의 내장 데이터 타입은 명확한 외부 형식을 가지며, 많은 타입이 SQL 표준에 정의된 타입들입니다.

### 주요 데이터 타입 분류

| 분류 | 타입 |
|------|------|
| 숫자 타입 | `smallint`, `integer`, `bigint`, `decimal`, `numeric`, `real`, `double precision`, `serial`, `bigserial` |
| 문자 타입 | `character varying(n)`, `varchar(n)`, `character(n)`, `char(n)`, `text` |
| 바이너리 타입 | `bytea` |
| 날짜/시간 타입 | `date`, `time`, `timestamp`, `interval` |
| 불리언 타입 | `boolean` |
| 열거형 타입 | 사용자 정의 `ENUM` |
| 기하학적 타입 | `point`, `line`, `lseg`, `box`, `path`, `polygon`, `circle` |
| 네트워크 타입 | `inet`, `cidr`, `macaddr`, `macaddr8` |
| 비트 문자열 | `bit(n)`, `bit varying(n)` |
| 텍스트 검색 | `tsvector`, `tsquery` |
| UUID | `uuid` |
| XML | `xml` |
| JSON | `json`, `jsonb` |
| 배열 | 모든 타입의 배열 |
| 범위 타입 | `int4range`, `int8range`, `numrange`, `tsrange`, `tstzrange`, `daterange` |
| 통화 타입 | `money` |

---

## 숫자 타입 (Numeric Types)

PostgreSQL은 다양한 숫자 타입을 제공합니다.

### 정수 타입 (Integer Types)

| 타입 | 저장 크기 | 범위 | 설명 |
|------|---------|------|------|
| `smallint` (int2) | 2 bytes | -32,768 ~ +32,767 | 소범위 정수 |
| `integer` (int4) | 4 bytes | -2,147,483,648 ~ +2,147,483,647 | 일반적인 정수 (권장) |
| `bigint` (int8) | 8 bytes | -9,223,372,036,854,775,808 ~ +9,223,372,036,854,775,807 | 대범위 정수 |

사용 권장사항:
- `integer`는 범위, 저장 크기, 성능의 균형이 좋아 가장 일반적으로 사용됩니다.
- `smallint`는 디스크 공간이 제한적일 때 사용합니다.
- `bigint`는 `integer` 범위로 부족할 때 사용합니다.

### 임의 정밀도 숫자 (Arbitrary Precision Numbers)

`numeric` 타입은 아주 많은 자릿수를 저장하고 정확한 계산을 수행할 수 있습니다.

```sql
-- 선언 방식
NUMERIC(precision, scale)    -- 정밀도와 스케일 지정
NUMERIC(precision)           -- 스케일 0으로 설정
NUMERIC                      -- 무제약 숫자 (최대 한계까지)
```

용어 정의:
- Precision: 소수점 양쪽의 전체 유효 자릿수
- Scale: 소수점 오른쪽의 소수 자릿수

예: `23.5141` -> precision=6, scale=4

```sql
-- 예제
CREATE TABLE prices (
    amount NUMERIC(10, 2)  -- 최대 10자리, 소수점 이하 2자리
);

INSERT INTO prices VALUES (12345678.99);  -- OK
INSERT INTO prices VALUES (1234567890.12);  -- 에러: 정밀도 초과
```

특수 값:
```sql
UPDATE table SET x = 'Infinity';
UPDATE table SET x = '-Infinity';
UPDATE table SET x = 'NaN';
```

특징:
- 정확한 계산 (더하기, 빼기, 곱하기)
- 금액, 통화처럼 정확성이 중요한 경우 권장
- 정수 타입보다 연산이 느림

### 부동소수점 타입 (Floating-Point Types)

| 타입 | 저장 크기 | 범위 | 정밀도 |
|------|---------|------|--------|
| `real` (float4) | 4 bytes | 약 1E-37 ~ 1E+37 | 최소 6자리 |
| `double precision` (float8) | 8 bytes | 약 1E-307 ~ 1E+308 | 최소 15자리 |

```sql
-- 선언 방식
FLOAT(1) to FLOAT(24)      -- real 타입
FLOAT(25) to FLOAT(53)     -- double precision 타입
FLOAT                      -- double precision 기본값
```

예제:
```sql
CREATE TABLE measurements (
    temperature REAL,
    pressure DOUBLE PRECISION
);

INSERT INTO measurements VALUES (36.5, 101325.12345678901);
```

반올림 차이:
```sql
SELECT x,
  round(x::numeric) AS num_round,
  round(x::double precision) AS dbl_round
FROM generate_series(-3.5, 3.5, 1) as x;

-- numeric: 0.5 -> 1 (0에서 멀어지는 방향)
-- double precision: 0.5 -> 0 (짝수로 반올림, banker's rounding)
```

특수 값:
```sql
UPDATE table SET x = 'Infinity';
UPDATE table SET x = '-Infinity';
UPDATE table SET x = 'NaN';
```

### 시리얼 타입 (Serial Types - 자동 증가)

시리얼 타입은 자동으로 증가하는 정수 값을 생성하는 편의 기능입니다.

| 타입 | 저장 크기 | 범위 | 기반 타입 |
|------|---------|------|----------|
| `smallserial` | 2 bytes | 1 ~ 32,767 | smallint |
| `serial` | 4 bytes | 1 ~ 2,147,483,647 | integer |
| `bigserial` | 8 bytes | 1 ~ 9,223,372,036,854,775,807 | bigint |

작동 원리:
```sql
CREATE TABLE tablename (
    colname SERIAL
);

-- 위는 다음과 동등:
CREATE SEQUENCE tablename_colname_seq AS integer;
CREATE TABLE tablename (
    colname integer NOT NULL DEFAULT nextval('tablename_colname_seq')
);
ALTER SEQUENCE tablename_colname_seq OWNED BY tablename.colname;
```

예제:
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 삽입 (id는 자동 생성)
INSERT INTO users (name) VALUES ('John');
INSERT INTO users (name) VALUES ('Jane');
-- id: 1, 2 자동 생성
```

주의사항:
- 트랜잭션 롤백 시에도 시퀀스 값은 소진됨 (갭 발생 가능)
- 2^31개 이상의 식별자 필요 시 `bigserial` 사용
- 컬럼 삭제 시 시퀀스도 자동 삭제

### 숫자 타입 선택 가이드

| 상황 | 권장 타입 |
|------|----------|
| 정확한 금액 저장 | `NUMERIC` |
| 일반 정수 | `INTEGER` |
| 대용량 정수 | `BIGINT` |
| 자동증가 ID | `SERIAL` / `BIGSERIAL` |
| 과학 계산 | `DOUBLE PRECISION` |
| 메모리 절약 중심 | `SMALLINT` / `SMALLSERIAL` |

---

## 문자 타입 (Character Types)

PostgreSQL은 다양한 문자열 타입을 제공합니다.

### 문자 타입 종류

| 타입 | 설명 |
|------|------|
| `character varying(n)`, `varchar(n)` | 가변 길이, 최대 n자 |
| `character(n)`, `char(n)` | 고정 길이, 공백 패딩 |
| `text` | 가변 무제한 길이 |

### character(n) - 고정 길이, 공백 패딩

```sql
CREATE TABLE test1 (a character(4));
INSERT INTO test1 VALUES ('ok');
SELECT a, char_length(a) FROM test1;
-- 결과: a='ok  ' (패딩됨), char_length=2
```

특징:
- 지정된 길이까지 공백으로 패딩
- 비교 시 뒤쪽 공백은 무시됨
- 저장 공간 낭비 가능성

### varchar(n) - 가변 길이, 길이 제한

```sql
CREATE TABLE test2 (b varchar(5));
INSERT INTO test2 VALUES ('ok');           -- OK (2자)
INSERT INTO test2 VALUES ('good      ');   -- OK (5자로 인식)
INSERT INTO test2 VALUES ('too long');     -- ERROR: 5자 초과
INSERT INTO test2 VALUES ('too long'::varchar(5)); -- OK (명시적 자르기)

SELECT b, char_length(b) FROM test2;
-- 결과:
--   b   | char_length
-- ------+-------------
--   ok  |           2
--  good |           5
--  too l|           5
```

### text - 가변 무제한 길이

```sql
-- 길이 제한 없음
CREATE TABLE test3 (c text);
INSERT INTO test3 VALUES ('아무리 긴 문자열도 저장 가능합니다...');
```

특징:
- PostgreSQL의 네이티브 문자열 타입
- 길이 제한 없음
- 대부분의 내장 함수가 `text`를 사용

### 성능 및 사용 권장사항

> PostgreSQL 공식 문서 Tip:
> 세 가지 타입 간 성능 차이는 거의 없습니다. 실제로 `character(n)`은 저장 공간 오버헤드로 인해 가장 느립니다. 대부분의 경우 `text` 또는 `varchar` (길이 미지정)을 사용하는 것을 권장합니다.

### 저장 공간 요구사항

- 126바이트 이하: 1바이트 + 실제 문자열
- 126바이트 초과: 4바이트 오버헤드
- 최대 저장 가능 길이: 약 1GB
- `character(n)` 선언에서 n의 최대값: 10,485,760

### 문자 타입 선택 가이드

| 상황 | 추천 타입 |
|------|----------|
| 길이 제한 필요 | `varchar(n)` |
| 길이 제한 없음 | `text` |
| ~~고정 길이~~ | ~~`character(n)`~~ (성능 이유로 비추천) |

---

## 바이너리 데이터 타입 (Binary Data Types)

`bytea` 데이터 타입은 이진 문자열(binary strings)을 저장합니다.

| 항목 | 설명 |
|------|------|
| 타입명 | `bytea` |
| 저장 크기 | 1 또는 4 바이트 + 실제 바이너리 문자열 |
| 용도 | 가변 길이 바이너리 문자열 저장 |

### 바이너리 문자열의 특징

- 0값 옥텟과 "인쇄 불가능" 옥텟 저장 가능
- 로캘 설정에 독립적인 바이트 단위 처리
- "raw bytes"로 간주되는 데이터 저장에 적합

### Hex 형식 (권장)

```sql
SET bytea_output = 'hex';
SELECT '\xDEADBEEF'::bytea;
-- 출력: \xdeadbeef
```

특징:
- 바이트당 2개의 16진수 숫자로 인코딩
- `\x` 프리픽스로 시작
- 대소문자 모두 허용
- 외부 애플리케이션과의 호환성 우수
- 더 빠른 변환 속도

### Escape 형식 (레거시)

```sql
SET bytea_output = 'escape';
SELECT 'abc \153\154\155 \052\251\124'::bytea;
-- 출력: abc klm \*\251T
```

이스케이프 규칙:

| 옥텟값 | 설명 | 입력 형식 | 예제 | Hex |
|-------|------|---------|------|-----|
| 0 | 영 옥텟 | `'\000'` | `'\000'::bytea` | `\x00` |
| 39 | 따옴표 | `''''` 또는 `'\047'` | `''''::bytea` | `\x27` |
| 92 | 백슬래시 | `'\\'` 또는 `'\134'` | `'\\'::bytea` | `\x5c` |
| 0-31, 127-255 | 인쇄 불가능 | `'\xxx'` (8진수) | `'\001'::bytea` | `\x01` |

---

## 날짜/시간 타입 (Date/Time Types)

PostgreSQL은 다음 6가지 날짜/시간 타입을 지원합니다.

### 날짜/시간 타입 개요

| 타입 | 저장크기 | 설명 | 범위 | 해상도 |
|------|---------|------|------|--------|
| `timestamp [without time zone]` | 8 bytes | 날짜와 시간 (시간대 없음) | 4713 BC ~ 294276 AD | 1 microsecond |
| `timestamp with time zone` | 8 bytes | 날짜와 시간 (시간대 포함) | 4713 BC ~ 294276 AD | 1 microsecond |
| `date` | 4 bytes | 날짜만 | 4713 BC ~ 5874897 AD | 1 day |
| `time [without time zone]` | 8 bytes | 시간만 | 00:00:00 ~ 24:00:00 | 1 microsecond |
| `time with time zone` | 12 bytes | 시간과 시간대 | 00:00:00+1559 ~ 24:00:00-1559 | 1 microsecond |
| `interval [fields] [(p)]` | 16 bytes | 시간 간격 | -178000000 ~ 178000000 years | 1 microsecond |

### 정밀도 (Precision)

`time`, `timestamp`, `interval`은 선택적 정밀도 값 `p`를 지원하며, 초(seconds) 필드의 소수 자릿수를 지정합니다:
- 범위: 0 ~ 6
- 기본값: 명시적 제한 없음

```sql
timestamp(2)  -- 소수점 이하 2자리
time(4)       -- 소수점 이하 4자리
```

### Date 타입 입력 예제

| 예제 | 설명 |
|------|------|
| `1999-01-08` | ISO 8601 (모든 모드에서 권장) |
| `January 8, 1999` | 모든 datestyle에서 명확 |
| `1/8/1999` | MDY 모드: 1월 8일 / DMY 모드: 8월 1일 |
| `1999-Jan-08` | 모든 모드에서 지원 |
| `19990108` | ISO 8601 compact format |

### Time 타입 입력 예제

| 예제 | 설명 |
|------|------|
| `04:05:06.789` | ISO 8601 |
| `04:05 PM` | 오후 4시 5분 (16:05와 동일) |
| `04:05:06-08:00` | ISO 8601, UTC offset 포함 |
| `04:05:06 PST` | 시간대 약자 |
| `2003-04-12 04:05:06 America/New_York` | 전체 시간대 이름 |

### Timestamp 입력 예제

```sql
-- 유효한 입력
1999-01-08 04:05:06
1999-01-08 04:05:06 -8:00
January 8 04:05:06 1999 PST

-- 명시적 타입 지정
TIMESTAMP WITHOUT TIME ZONE '2004-10-19 10:23:54'
TIMESTAMP WITH TIME ZONE '2004-10-19 10:23:54+02'
```

### 시간대 (Time Zone) 지정 방식

```sql
-- 1. 전체 이름 (권장)
America/New_York
Europe/London
Asia/Seoul

-- 2. 약자
PST (Pacific Standard Time)
EST (Eastern Standard Time)
KST (Korea Standard Time)

-- 3. POSIX 스타일
PST8PDT

-- 4. UTC offset
-8:00:00  또는  -8:00  또는  -800  또는  -8

-- 5. 군부대 약자
zulu  또는  z  (UTC)
```

### 특수 날짜/시간 값

| 입력값 | 유효 타입 | 설명 |
|--------|----------|------|
| `epoch` | date, timestamp | 1970-01-01 00:00:00+00 (Unix 시스템 시간 0) |
| `infinity` | date, timestamp, interval | 모든 다른 타임스탬프보다 이후 |
| `-infinity` | date, timestamp, interval | 모든 다른 타임스탬프보다 이전 |
| `now` | date, time, timestamp | 현재 트랜잭션 시작 시간 |
| `today` | date, timestamp | 오늘 자정 (00:00) |
| `tomorrow` | date, timestamp | 내일 자정 (00:00) |
| `yesterday` | date, timestamp | 어제 자정 (00:00) |
| `allballs` | time | 00:00:00.00 UTC |

### 출력 형식

```sql
SET datestyle = 'ISO';        -- 1997-12-17 07:37:16-08
SET datestyle = 'SQL';        -- 12/17/1997 07:37:16.00 PST
SET datestyle = 'Postgres';   -- Wed Dec 17 07:37:16 1997 PST
SET datestyle = 'German';     -- 17.12.1997 07:37:16.00 PST
```

### Interval 타입 입력 예제

```sql
-- 전통적 Postgres 형식
'1 year 2 months 3 days 4 hours 5 minutes 6 seconds'
'1-2'                    -- 1년 2개월
'3 4:05:06'              -- 3일 4시간 5분 6초

-- ISO 8601 형식
'P1Y2M3DT4H5M6S'        -- 형식 지정자 포함
'P0001-02-03T04:05:06'  -- 대체 형식

-- 축약 형식
'1 12:59:10'             -- 1 day 12 hours 59 min 10 sec로 해석
'200-10'                 -- 200 years 10 months로 해석
```

### Interval 출력 스타일

```sql
SET intervalstyle = 'sql_standard';   -- 1-2 또는 3 4:05:06
SET intervalstyle = 'postgres';       -- 1 year 2 mons 또는 3 days 04:05:06
SET intervalstyle = 'postgres_verbose'; -- @ 1 year 2 mons
SET intervalstyle = 'iso_8601';       -- P1Y2M 또는 P3DT4H5M6S
```

---

## 불리언 타입 (Boolean Type)

### 기본 정보

| 항목 | 설명 |
|------|------|
| 타입명 | `boolean` |
| 저장 크기 | 1 byte |
| 설명 | true 또는 false 상태를 나타냄 |
| 특징 | SQL null 값으로 표현되는 "unknown" 상태 포함 가능 |

### 유효한 값

True 상태 입력값:
- `true`, `yes`, `on`, `1`
- 이들의 고유 접두사도 허용 (예: `t`, `y`)

False 상태 입력값:
- `false`, `no`, `off`, `0`
- 이들의 고유 접두사도 허용 (예: `f`, `n`)

특징:
- 선행/후행 공백 무시
- 대소문자 구분 안 함
- 출력값: 항상 `t` 또는 `f`로 출력

### 사용 방법

```sql
-- SQL 키워드 (권장)
TRUE, FALSE, NULL

-- 문자열 리터럴
'yes'::boolean
'no'::boolean
```

### 예제

```sql
CREATE TABLE test1 (a boolean, b text);
INSERT INTO test1 VALUES (TRUE, 'sic est');
INSERT INTO test1 VALUES (FALSE, 'non est');

SELECT * FROM test1;
 a |    b
---+---------
 t | sic est
 f | non est

SELECT * FROM test1 WHERE a;
 a |    b
---+---------
 t | sic est

SELECT * FROM test1 WHERE NOT a;
 a |    b
---+---------
 f | non est
```

### 주의사항

- `TRUE`/`FALSE`는 자동으로 boolean으로 인식
- `NULL`은 모든 타입이 가능하므로 명시적 캐스팅 필요: `NULL::boolean`

---

## 열거형 타입 (Enumerated Types)

열거형(Enumerated Types)은 정적이고 순서가 있는 값의 집합으로 구성된 데이터 타입입니다.

### 생성 방법

```sql
CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');
```

### 사용 방법

```sql
CREATE TABLE person (
    name text,
    current_mood mood
);

INSERT INTO person VALUES ('Moe', 'happy');
SELECT * FROM person WHERE current_mood = 'happy';
```

결과:
```
 name | current_mood
------+--------------
 Moe  | happy
(1 row)
```

### 정렬 및 비교 연산

열거형 값의 순서는 생성 시 정의한 순서입니다.

```sql
INSERT INTO person VALUES ('Larry', 'sad');
INSERT INTO person VALUES ('Curly', 'ok');
SELECT * FROM person WHERE current_mood > 'sad' ORDER BY current_mood;
```

결과:
```
 name  | current_mood
-------+--------------
 Curly | ok
 Moe   | happy
(2 rows)
```

### 타입 안전성

각 열거형 타입은 독립적이며 다른 열거형과 비교할 수 없습니다.

```sql
-- 오류 발생
SELECT person.name FROM person, holidays
  WHERE person.current_mood = holidays.happiness;
-- ERROR: operator does not exist: mood = happiness

-- 해결책: 명시적 캐스팅
SELECT person.name FROM person, holidays
  WHERE person.current_mood::text = holidays.happiness::text;
```

### 주요 특성

| 특성 | 설명 |
|------|------|
| 대소문자 구분 | 'happy' != 'HAPPY' |
| 공백 처리 | 라벨의 공백은 유의미함 |
| 디스크 용량 | 4바이트 |
| 라벨 길이 | 최대 63바이트 (NAMEDATALEN) |
| 수정 가능 | 새 값 추가, 이름 변경 가능 |
| 삭제 불가 | 기존 값 제거 불가 |

---

## 기하학적 타입 (Geometric Types)

PostgreSQL의 기하학적 타입은 2차원 공간 객체를 나타내며, 좌표는 `double precision` (float8) 숫자로 저장됩니다.

### 기하학적 타입 목록

| 타입 | 저장 크기 | 설명 | 표현 형식 |
|------|---------|------|---------|
| `point` | 16 bytes | 평면의 점 | (x,y) |
| `line` | 24 bytes | 무한 직선 | {A,B,C} |
| `lseg` | 32 bytes | 유한 선분 | [(x1,y1),(x2,y2)] |
| `box` | 32 bytes | 직사각형 상자 | (x1,y1),(x2,y2) |
| `path` | 16+16n bytes | 열린/닫힌 경로 | [(x1,y1),...] or ((x1,y1),...) |
| `polygon` | 40+16n bytes | 다각형 | ((x1,y1),...) |
| `circle` | 24 bytes | 원 | <(x,y),r> |

### 각 타입의 사용법

Point (점)
```sql
SELECT '(1.0, 2.5)'::point;
SELECT point(1.0, 2.5);
```

Line (직선) - 직선 방정식: `Ax + By + C = 0`
```sql
SELECT '{1,2,3}'::line;  -- 기울기 이용
SELECT '[(0,0), (1,1)]'::line;  -- 두 점 이용
```

Line Segment (선분)
```sql
SELECT '[(0,0), (1,1)]'::lseg;
SELECT '((2,2), (3,3))'::lseg;
```

Box (상자)
```sql
SELECT '((0,0), (1,1))'::box;
SELECT '(0,0), (5,5)'::box;
-- 자동으로 우상단과 좌하단으로 정렬됨
```

Path (경로) - 열린 경로 `[]` vs 닫힌 경로 `()`
```sql
-- 열린 경로 (첫 점과 마지막 점이 연결되지 않음)
SELECT '[(0,0), (1,1), (2,0)]'::path;

-- 닫힌 경로 (첫 점과 마지막 점이 연결됨)
SELECT '((0,0), (1,1), (2,0))'::path;
```

Polygon (다각형)
```sql
SELECT '((0,0), (1,0), (1,1), (0,1))'::polygon;
SELECT '(0,0), (2,0), (2,2), (0,2)'::polygon;
```

Circle (원) - 중심점과 반지름
```sql
SELECT '<(0,0), 5>'::circle;
SELECT '((1,1), 2)'::circle;
SELECT '(3,4), 1.5'::circle;
```

---

## 네트워크 주소 타입 (Network Address Types)

PostgreSQL은 IPv4, IPv6, MAC 주소를 저장하기 위한 전문화된 데이터 타입을 제공합니다.

### 네트워크 타입 개요

| 타입 | 크기 | 설명 |
|------|------|------|
| `inet` | 7 or 19 bytes | IPv4 및 IPv6 호스트 및 네트워크 |
| `cidr` | 7 or 19 bytes | IPv4 및 IPv6 네트워크 |
| `macaddr` | 6 bytes | MAC 주소 |
| `macaddr8` | 8 bytes | MAC 주소 (EUI-64 형식) |

### inet 타입

IPv4 또는 IPv6 호스트 주소와 선택적 서브넷을 저장합니다.

```sql
-- 입력 형식: address/y (y는 넷마스크 비트 수)
192.168.1.5/32      -- 단일 호스트
192.168.1.5/24      -- 192.168.1.0/24 네트워크
2001:4f8:3:ba::/64  -- IPv6 네트워크
::ffff:10.4.3.2     -- IPv4 매핑된 IPv6 주소
```

### cidr 타입

CIDR(Classless Internet Domain Routing) 규칙을 준수하는 네트워크 명세만 저장합니다.

```sql
-- 입력/출력 예제
192.168.100.128/25  -- 192.168.100.128/25
192.168/24          -- 192.168.0.0/24
10                  -- 10.0.0.0/8
2001:4f8:3:ba::/64  -- 2001:4f8:3:ba::/64
```

### inet vs cidr 비교

| 특징 | inet | cidr |
|------|------|------|
| 호스트 저장 | O | X |
| 네트워크 저장 | O | O |
| 넷마스크 우측 비트 허용 | O | X (오류) |

### macaddr 타입

6바이트 MAC 주소를 저장합니다.

```sql
-- 입력 형식 (모두 동일한 주소)
'08:00:2b:01:02:03'    -- 콜론 구분
'08-00-2b-01-02-03'    -- 하이픈 구분
'08002b:010203'
'08002b010203'         -- 구분자 없음
```

### macaddr8 타입

EUI-64 형식 8바이트 MAC 주소를 저장합니다.

```sql
'08:00:2b:01:02:03:04:05'   -- 8바이트
'08002b0102030405'          -- 구분자 없음

-- EUI-48을 EUI-64로 변환
SELECT macaddr8_set7bit('08:00:2b:01:02:03');
-- 결과: 0a:00:2b:ff:fe:01:02:03
```

---

## 비트 문자열 타입 (Bit String Types)

비트 문자열은 1과 0으로 이루어진 문자열로, 비트 마스크를 저장하거나 시각화할 때 사용됩니다.

### 두 가지 SQL 비트 타입

| 타입 | 설명 |
|------|------|
| `bit(n)` | 고정 길이, 정확히 n비트 필요 |
| `bit varying(n)` | 가변 길이, 최대 n비트 |

참고:
- `bit` (길이 미지정) = `bit(1)`과 동일
- `bit varying` (길이 미지정) = 무제한 길이

### 사용 예제

```sql
CREATE TABLE test (a BIT(3), b BIT VARYING(5));

-- 성공
INSERT INTO test VALUES (B'101', B'00');
INSERT INTO test VALUES (B'10'::bit(3), B'101');

-- 에러: bit string length 2 does not match type bit(3)
INSERT INTO test VALUES (B'10', B'101');

-- 조회 결과
SELECT * FROM test;
 a   | b
-----+-----
 101 | 00
 100 | 101
```

### 저장 공간

- 8비트마다 1바이트 필요
- 추가 오버헤드: 길이에 따라 5~8바이트

---

## 텍스트 검색 타입 (Text Search Types)

PostgreSQL은 전문 검색(full text search)을 위해 두 가지 데이터 타입을 제공합니다.

### tsvector 타입

문서를 텍스트 검색에 최적화된 형식으로 표현합니다. 정규화된 어휘(lexemes)의 정렬된 목록입니다.

```sql
SELECT 'a fat cat sat on a mat and ate a fat rat'::tsvector;
-- 결과: 'a' 'and' 'ate' 'cat' 'fat' 'mat' 'on' 'rat' 'sat'
```

위치(Position) 정보:
```sql
SELECT 'a:1 fat:2 cat:3 sat:4 on:5 a:6 mat:7 and:8 ate:9 a:10 fat:11 rat:12'::tsvector;
-- 결과: 'a':1,6,10 'and':8 'ate':9 'cat':3 'fat':2,11 'mat':7 'on':5 'rat':12 'sat':4
```

가중치(Weight) 라벨 (A, B, C, D):
```sql
SELECT 'a:1A fat:2B,4C cat:5D'::tsvector;
-- 결과: 'a':1A 'cat':5 'fat':2B,4C
```

정규화:
```sql
-- tsvector는 정규화를 수행하지 않음
SELECT 'The Fat Rats'::tsvector;
-- 결과: 'Fat' 'Rats' 'The'

-- to_tsvector 함수로 정규화 필요
SELECT to_tsvector('english', 'The Fat Rats');
-- 결과: 'fat':2 'rat':3
```

### tsquery 타입

검색할 어휘를 저장하고 불린 연산자로 결합합니다.

지원 연산자:

| 연산자 | 기능 | 예제 |
|--------|------|------|
| `&` | AND | `'fat' & 'rat'` |
| `\|` | OR | `'fat' \| 'cat'` |
| `!` | NOT | `!'cat'` |
| `<->` | FOLLOWED BY (인접) | `'fat' <-> 'rat'` |
| `<N>` | N 거리로 FOLLOWED BY | `'fat' <3> 'rat'` |

연산자 우선순위 (높음 -> 낮음):
1. `!` (NOT) - 가장 강함
2. `<->` (FOLLOWED BY)
3. `&` (AND)
4. `|` (OR) - 가장 약함

사용 예제:
```sql
-- 기본 AND
SELECT 'fat & rat'::tsquery;
-- 결과: 'fat' & 'rat'

-- 괄호를 이용한 그룹화
SELECT 'fat & (rat | cat)'::tsquery;
-- 결과: 'fat' & ( 'rat' | 'cat' )

-- NOT 연산자
SELECT 'fat & rat & ! cat'::tsquery;
-- 결과: 'fat' & 'rat' & !'cat'

-- 접두사 매칭
SELECT 'super:*'::tsquery;
-- 결과: 'super':*
-- "super"로 시작하는 모든 단어와 매칭
```

---

## UUID 타입

`uuid` 데이터 타입은 RFC 9562, ISO/IEC 9834-8:2005에 정의된 Universally Unique Identifiers (UUID) 를 저장합니다.

### 특징

- 128비트 고유 식별자
- 분산 시스템에서 데이터베이스 시퀀스보다 더 나은 고유성 보장
- GUID(Globally Unique Identifier)라고도 불림

### UUID 형식

표준 형식:
```
a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
```
- 소문자 16진수 32자리
- 8자리-4자리-4자리-4자리-12자리 구조 (하이픈으로 구분)

입력 가능한 대체 형식:
```sql
A0EEBC99-9C0B-4EF8-BB6D-6BB9BD380A11          -- 대문자
{a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11}       -- 중괄호 포함
a0eebc999c0b4ef8bb6d6bb9bd380a11             -- 하이픈 제거
a0ee-bc99-9c0b-4ef8-bb6d-6bb9-bd38-0a11      -- 추가 하이픈
```

출력은 항상 표준 형식입니다.

### UUID 생성 방법

PostgreSQL은 기본적으로 2가지 알고리즘을 지원합니다:
1. UUIDv4 - 난수 기반
2. UUIDv7 - 타임스탬프 기반 (최신)

### 사용 예제

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT
);

INSERT INTO users (name) VALUES ('John Doe');
-- id는 자동으로 UUID가 생성됨
```

---

## XML 타입

PostgreSQL의 `xml` 데이터 타입은 XML 데이터를 저장할 수 있으며, 텍스트 필드와 달리 입력값의 well-formedness를 검사 하고 타입 안전 연산을 지원합니다.

### XML 값 생성 방법

표준 방식: XMLPARSE
```sql
XMLPARSE ( { DOCUMENT | CONTENT } value)
```

예제:
```sql
-- 완전한 XML 문서
XMLPARSE (DOCUMENT '<?xml version="1.0"?><book><title>Manual</title><chapter>...</chapter></book>')

-- XML 컨텐츠 조각
XMLPARSE (CONTENT 'abc<foo>bar</foo><bar>foo</bar>')
```

PostgreSQL 특화 문법:
```sql
xml '<foo>bar</foo>'
'<foo>bar</foo>'::xml
```

### XML 값 직렬화 (출력)

```sql
XMLSERIALIZE ( { DOCUMENT | CONTENT } value AS type [[ NO ] INDENT] )
```

파라미터:
- `type`: `character`, `character varying`, 또는 `text`
- `INDENT`: 결과를 pretty-print (기본값: NO INDENT)

### 주요 특징 및 제한사항

| 특징 | 설명 |
|------|------|
| 비교 연산자 미지원 | XML 데이터에 비교 연산자(`=`, `<` 등) 없음 |
| 인덱싱 불가 | XML 컬럼에 직접 인덱스 생성 불가 |
| DTD 검증 미지원 | 입력값이 DTD를 검증하지 않음 |
| DOCUMENT vs CONTENT | 완전한 문서 vs 컨텐츠 조각 구분 가능 |

---

## JSON 타입

PostgreSQL은 JSON 데이터 저장을 위해 `json` 과 `jsonb` 두 가지 타입을 제공합니다.

### json vs jsonb 비교

| 특성 | json | jsonb |
|------|------|-------|
| 저장 형식 | 입력 텍스트의 정확한 복사본 | 분해된 이진 형식 |
| 입력 속도 | 빠름 | 느림 (변환 오버헤드) |
| 처리 속도 | 느림 (매번 재파싱) | 빠름 (재파싱 불필요) |
| 공백 보존 | 예 | 아니오 |
| 객체 키 순서 | 보존 | 보존 안 함 |
| 중복 키 처리 | 모두 유지 | 마지막 값만 유지 |
| 인덱싱 지원 | 아니오 | 예 |

권장사항: 특수한 요구사항이 없다면 `jsonb` 사용 권장

### 기본 문법

```sql
-- 스칼라 값
SELECT '5'::json;

-- 배열
SELECT '[1, 2, "foo", null]'::json;

-- 객체
SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json;

-- 중첩 구조
SELECT '{"foo": [true, "bar"], "tags": {"a": 1, "b": null}}'::json;
```

### 출력 차이 예시

```sql
SELECT '{"bar": "baz", "balance": 7.77, "active":false}'::json;
-- 결과: {"bar": "baz", "balance": 7.77, "active":false}

SELECT '{"bar": "baz", "balance": 7.77, "active":false}'::jsonb;
-- 결과: {"bar": "baz", "active": false, "balance": 7.77}
-- (공백 제거, 키 순서 변경)
```

### jsonb 주요 기능

포함 관계 검사 (`@>`):
```sql
-- 단순 스칼라
SELECT '"foo"'::jsonb @> '"foo"'::jsonb;  -- true

-- 배열 포함
SELECT '[1, 2, 3]'::jsonb @> '[1, 3]'::jsonb;  -- true

-- 객체 포함
SELECT '{"product": "PostgreSQL", "version": 9.4, "jsonb": true}'::jsonb
       @> '{"version": 9.4}'::jsonb;  -- true
```

존재 여부 확인 (`?`):
```sql
-- 배열 요소 확인
SELECT '["foo", "bar", "baz"]'::jsonb ? 'bar';  -- true

-- 객체 키 확인
SELECT '{"foo": "bar"}'::jsonb ? 'foo';  -- true

-- 값은 확인 불가
SELECT '{"foo": "bar"}'::jsonb ? 'bar';  -- false
```

서브스크립팅 (배열/객체 접근):
```sql
-- 객체 값 추출
SELECT ('{"a": 1}'::jsonb)['a'];  -- 1

-- 중첩 객체 접근
SELECT ('{"a": {"b": {"c": 1}}}'::jsonb)['a']['b']['c'];  -- 1

-- 배열 요소 추출 (0부터 시작)
SELECT ('[1, "2", null]'::jsonb)[1];  -- "2"

-- UPDATE에서 사용
UPDATE table_name SET jsonb_field['key'] = '1';
```

GIN 인덱싱:
```sql
-- 기본 GIN 인덱스 (?, ?|, ?&, @>, @?, @@ 연산자 지원)
CREATE INDEX idxgin ON api USING GIN (jdoc);

-- jsonb_path_ops 인덱스 (@>, @?, @@ 만 지원하지만 더 효율적)
CREATE INDEX idxginp ON api USING GIN (jdoc jsonb_path_ops);

-- 표현식 인덱스
CREATE INDEX idxgintags ON api USING GIN ((jdoc -> 'tags'));
```

### 실제 사용 예제

```sql
-- 테이블 생성
CREATE TABLE websites (
    id SERIAL PRIMARY KEY,
    doc jsonb
);

-- 데이터 삽입
INSERT INTO websites (doc) VALUES
('{"site_name": "Example", "tags": [{"term": "paris"}, {"term": "food"}]}');

-- 포함 관계로 검색 (권장: 유연하고 효율적)
SELECT doc->'site_name' FROM websites
WHERE doc @> '{"tags":[{"term":"paris"}, {"term":"food"}]}';

-- 조건부 검색
SELECT doc->'guid', doc->'name' FROM websites
WHERE doc->>'company' = 'Magnafone';
```

### 주의사항

1. UTF-8 인코딩: JSON 표준을 따르려면 데이터베이스 인코딩이 UTF-8이어야 함
2. 숫자 범위: `jsonb`는 PostgreSQL의 `numeric` 타입 범위로 제한
3. 잠금: JSON 문서 업데이트 시 전체 행에 대한 row-level 잠금 발생
4. 문서 크기: 동시성 문제를 피하기 위해 관리 가능한 크기로 제한 권장

---

## 배열 (Arrays)

PostgreSQL은 테이블의 컬럼을 가변 길이의 다차원 배열로 정의할 수 있습니다.

### 배열 선언 (Array Declaration)

```sql
CREATE TABLE sal_emp (
    name            text,
    pay_by_quarter  integer[],        -- 1차원 배열
    schedule        text[][]           -- 2차원 배열
);
```

크기 지정 문법 (SQL 표준):
```sql
CREATE TABLE tictactoe (
    squares integer[3][3]
);
```

주의: PostgreSQL은 선언된 배열 크기를 무시합니다. 크기는 문서화 목적일 뿐 실행 시간에 영향을 주지 않습니다.

### 배열 값 입력 (Array Value Input)

기본 문법:
```sql
'{ val1, val2, val3 }'
```

INSERT 예제:
```sql
INSERT INTO sal_emp VALUES (
    'Bill',
    '{10000, 10000, 10000, 10000}',
    '{{"meeting", "lunch"}, {"training", "presentation"}}'
);
```

ARRAY 생성자 문법:
```sql
INSERT INTO sal_emp VALUES (
    'Bill',
    ARRAY[10000, 10000, 10000, 10000],
    ARRAY[['meeting', 'lunch'], ['training', 'presentation']]
);
```

### 배열 접근 (Accessing Arrays)

단일 요소 접근 (1-based 인덱싱):
```sql
-- 2분기 급여가 1분기와 다른 직원 찾기
SELECT name FROM sal_emp
WHERE pay_by_quarter[1] <> pay_by_quarter[2];
-- 결과: Carol

-- 3분기 급여 조회
SELECT pay_by_quarter[3] FROM sal_emp;
-- 결과: 10000, 25000
```

배열 슬라이스 접근:
```sql
-- 첫 2일의 일정 조회
SELECT schedule[1:2][1:1] FROM sal_emp
WHERE name = 'Bill';
-- 결과: {{meeting},{training}}

-- 하한 또는 상한 생략
SELECT schedule[:2][2:] FROM sal_emp
WHERE name = 'Bill';
-- 결과: {{lunch},{presentation}}
```

배열 정보 함수:
```sql
-- 배열 차원 조회
SELECT array_dims(schedule) FROM sal_emp
WHERE name = 'Carol';
-- 결과: [1:2][1:2]

-- 상한/하한 조회
SELECT array_upper(schedule, 1) FROM sal_emp
WHERE name = 'Carol';
-- 결과: 2

-- 길이 조회
SELECT array_length(schedule, 1) FROM sal_emp
WHERE name = 'Carol';
-- 결과: 2

-- 전체 요소 개수 조회
SELECT cardinality(schedule) FROM sal_emp
WHERE name = 'Carol';
-- 결과: 4
```

### 배열 수정 (Modifying Arrays)

전체 배열 교체:
```sql
UPDATE sal_emp SET pay_by_quarter = '{25000,25000,27000,27000}'
WHERE name = 'Carol';

-- 또는 ARRAY 생성자 사용
UPDATE sal_emp SET pay_by_quarter = ARRAY[25000,25000,27000,27000]
WHERE name = 'Carol';
```

단일 요소 수정:
```sql
UPDATE sal_emp SET pay_by_quarter[4] = 15000
WHERE name = 'Bill';
```

슬라이스 수정:
```sql
UPDATE sal_emp SET pay_by_quarter[1:2] = '{27000,27000}'
WHERE name = 'Carol';
```

배열 연결 (||):
```sql
SELECT ARRAY[1,2] || ARRAY[3,4];
-- 결과: {1,2,3,4}

SELECT ARRAY[5,6] || ARRAY[[1,2],[3,4]];
-- 결과: {{5,6},{1,2},{3,4}}
```

배열 함수:
```sql
-- 요소 앞에 추가
SELECT array_prepend(1, ARRAY[2,3]);
-- 결과: {1,2,3}

-- 요소 뒤에 추가
SELECT array_append(ARRAY[1,2], 3);
-- 결과: {1,2,3}

-- 배열 연결
SELECT array_cat(ARRAY[1,2], ARRAY[3,4]);
-- 결과: {1,2,3,4}
```

### 배열 검색 (Searching in Arrays)

ANY/ALL 연산자:
```sql
-- 한 값이 배열에 포함되는지 검색
SELECT * FROM sal_emp
WHERE 10000 = ANY (pay_by_quarter);

-- 모든 값이 특정 값인지 검색
SELECT * FROM sal_emp
WHERE 10000 = ALL (pay_by_quarter);
```

겹침 연산자 (&&):
```sql
SELECT * FROM sal_emp
WHERE pay_by_quarter && ARRAY[10000];
```

위치 함수:
```sql
-- 첫 번째 발생 위치
SELECT array_position(
    ARRAY['sun','mon','tue','wed','thu','fri','sat'],
    'mon'
);
-- 결과: 2

-- 모든 발생 위치
SELECT array_positions(ARRAY[1, 4, 3, 1, 3, 4, 2, 1], 1);
-- 결과: {1,4,8}
```

### 배열 주요 특징

| 특징 | 설명 |
|------|------|
| 인덱싱 | 1-based (기본값) |
| NULL 처리 | 요소로 NULL 허용, 범위 외 참조 시 NULL 반환 |
| 다차원 배열 | 모든 차원이 동일한 범위를 가져야 함 |
| 슬라이스 | 하한/상한 생략 가능 |
| 동적 크기 | 선언된 크기는 무시됨 |

---

## 범위 타입 (Range Types)

범위 타입은 어떤 원소 타입(부분타입)의 값 범위를 나타내는 데이터 타입입니다.

### 내장 범위 타입

| 범위 타입 | 부분타입 | 멀티레인지 |
|---------|---------|----------|
| `int4range` | integer | `int4multirange` |
| `int8range` | bigint | `int8multirange` |
| `numrange` | numeric | `nummultirange` |
| `tsrange` | timestamp | `tsmultirange` |
| `tstzrange` | timestamp with timezone | `tstzmultirange` |
| `daterange` | date | `datemultirange` |

### 경계 표기법

포함/제외 표기:
```
[lower, upper)  -- 하한 포함, 상한 제외 (표준 형식)
(lower, upper]  -- 하한 제외, 상한 포함
[lower, upper]  -- 양쪽 포함
(lower, upper)  -- 양쪽 제외
empty           -- 빈 범위
```

### 범위 입출력 형식

범위 리터럴:
```sql
-- 3 포함, 7 미포함, 중간의 모든 값 포함
SELECT '[3,7)'::int4range;

-- 3과 7 모두 미포함
SELECT '(3,7)'::int4range;

-- 4만 포함 (단일 지점)
SELECT '[4,4]'::int4range;

-- 포함점 없음 (empty로 정규화됨)
SELECT '[4,4)'::int4range;
```

멀티레인지 리터럴:
```sql
SELECT '{}'::int4multirange;                      -- 빈 멀티레인지
SELECT '{[3,7)}'::int4multirange;                 -- 단일 범위
SELECT '{[3,7), [8,9)}'::int4multirange;          -- 다중 범위
```

### 무한 범위 (Unbounded Ranges)

```sql
(, 3]      -- 3 이하의 모든 값
[3, )      -- 3 이상의 모든 값
(, )       -- 모든 값
```

### 범위 생성 함수

```sql
-- 3번째 인자: '()', '(]', '[)', '[]'
SELECT numrange(1.0, 14.0, '(]');      -- (1.0, 14.0]
SELECT numrange(1.0, 14.0);            -- [1.0, 14.0) (기본값)
SELECT numrange(NULL, 2.2);            -- (, 2.2) (하한 무한)
SELECT int8range(1, 14, '(]');         -- 정규 형식으로 변환됨
```

### 범위 연산자 예제

```sql
-- 포함 (Containment)
SELECT int4range(10, 20) @> 3;

-- 겹침 (Overlaps)
SELECT numrange(11.1, 22.2) && numrange(20.0, 30.0);

-- 상한 추출
SELECT upper(int8range(15, 25));

-- 교집합
SELECT int4range(10, 20) * int4range(15, 25);

-- 범위가 비어있는지 확인
SELECT isempty(numrange(1, 5));
```

### 범위 테이블 예제

```sql
CREATE TABLE reservation (room int, during tsrange);

INSERT INTO reservation VALUES
    (1108, '[2010-01-01 14:30, 2010-01-01 15:30)');

-- 인덱싱
CREATE INDEX reservation_idx ON reservation USING GIST (during);
```

### 범위 제약 조건 (Exclusion Constraints)

```sql
-- 겹치지 않는 범위만 허용
CREATE TABLE reservation (
    during tsrange,
    EXCLUDE USING GIST (during WITH &&)
);

-- 회의실별 겹치지 않는 예약만 허용
CREATE TABLE room_reservation (
    room text,
    during tsrange,
    EXCLUDE USING GIST (room WITH =, during WITH &&)
);
```

### 사용자 정의 범위 타입

```sql
-- float8 범위 타입 정의
CREATE TYPE floatrange AS RANGE (
    subtype = float8,
    subtype_diff = float8mi
);

SELECT '[1.234, 5.678]'::floatrange;
```

---

## 통화 타입 (Monetary Types)

`money` 타입은 고정된 소수점 정밀도를 가진 통화 금액을 저장합니다.

### 기본 정보

| 항목 | 설명 |
|------|------|
| 저장 크기 | 8 bytes |
| 범위 | -92233720368547758.08 ~ +92233720368547758.07 |
| 소수점 자릿수 | `lc_monetary` 데이터베이스 설정에 따라 결정 (일반적으로 2자리) |

### 입출력 형식

```sql
-- 입력 형식
SELECT '12.34'::money;
SELECT '$1,000.00'::money;

-- 출력 형식은 로케일 설정에 의존
```

### 타입 변환

Money로 변환 (권장):
```sql
-- numeric, int, bigint에서 직접 변환 가능
CAST(value AS money)

-- float8에서 변환 (권장하지 않음)
SELECT '12.34'::float8::numeric::money;
```

Money에서 다른 타입으로 변환:
```sql
-- numeric으로 변환 (정밀도 손실 없음)
SELECT '52093.89'::money::numeric;

-- float8로 변환 (2단계 변환)
SELECT '52093.89'::money::numeric::float8;
```

### 주요 연산

```sql
-- money / integer: 소수 부분은 0으로 향해 절단됨
-- money / double precision: 반올림된 결과 반환
-- money / money: double precision 반환 (순수 숫자)

-- 권장 방법 (정밀도 유지)
SELECT ('100.00'::money::numeric / 3)::money;
```

### 주의사항

- 부동소수점 사용 금지: 부동소수점 숫자는 반올림 오류의 위험이 있으므로 금전 데이터에 사용하면 안 됨
- 로케일 호환성: 다른 `lc_monetary` 설정의 데이터베이스로 dump를 복원하기 전에 설정 값을 일치시켜야 함

---

# 참고 자료

- [PostgreSQL 공식 문서 - Chapter 8. Data Types](https://www.postgresql.org/docs/current/datatype.html)
- [PostgreSQL 공식 문서 - Numeric Types](https://www.postgresql.org/docs/current/datatype-numeric.html)
- [PostgreSQL 공식 문서 - Character Types](https://www.postgresql.org/docs/current/datatype-character.html)
- [PostgreSQL 공식 문서 - Date/Time Types](https://www.postgresql.org/docs/current/datatype-datetime.html)
- [PostgreSQL 공식 문서 - JSON Types](https://www.postgresql.org/docs/current/datatype-json.html)
- [PostgreSQL 공식 문서 - Arrays](https://www.postgresql.org/docs/current/arrays.html)
- [PostgreSQL 공식 문서 - Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
