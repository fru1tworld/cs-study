# PostgreSQL 함수와 연산자 (Functions and Operators)

이 문서는 PostgreSQL 공식 문서의 "Chapter 9. Functions and Operators"를 한국어로 번역한 것입니다.

## 개요

PostgreSQL은 내장 데이터 타입에 대한 다양한 함수와 연산자를 제공합니다. 사용자는 자신만의 함수와 연산자를 정의할 수도 있습니다 (Part V 참조).

### 유용한 psql 명령어

- `\df` - 사용 가능한 모든 함수 목록
- `\do` - 사용 가능한 모든 연산자 목록

### 표기법

함수는 다음과 같은 형식으로 설명됩니다:
```
repeat(text, integer) → text
```
이는 `repeat` 함수가 text와 integer 인자를 받아 text를 반환한다는 의미입니다.

예제 결과는 화살표로 표시됩니다:
```
repeat('Pg', 4) → 'PgPgPgPg'
```

---

# 9.1. 논리 연산자 (Logical Operators)

PostgreSQL은 세 가지 논리 연산자를 지원합니다: `AND`, `OR`, `NOT`. 시스템은 `true`, `false`, `null` (알 수 없음을 나타냄)의 3값 논리를 사용합니다.

## AND 연산자 진리표

| a | b | a AND b |
|---|---|---------|
| TRUE | TRUE | TRUE |
| TRUE | FALSE | FALSE |
| TRUE | NULL | NULL |
| FALSE | FALSE | FALSE |
| FALSE | NULL | FALSE |
| NULL | NULL | NULL |

## OR 연산자 진리표

| a | b | a OR b |
|---|---|--------|
| TRUE | TRUE | TRUE |
| TRUE | FALSE | TRUE |
| TRUE | NULL | TRUE |
| FALSE | FALSE | FALSE |
| FALSE | NULL | NULL |
| NULL | NULL | NULL |

## NOT 연산자 진리표

| a | NOT a |
|---|-------|
| TRUE | FALSE |
| FALSE | TRUE |
| NULL | NULL |

## 주요 특성

1. 교환법칙(Commutativity): AND와 OR 연산자는 교환 가능합니다:
   - `a AND b` = `b AND a`
   - `a OR b` = `b OR a`

2. 평가 순서: 좌우 피연산자의 평가 순서는 보장되지 않습니다.

3. 3값 논리(Three-Valued Logic): NULL은 알 수 없는 값을 나타내며, NULL과의 논리 연산은 위 진리표의 규칙을 따릅니다.

### 예제

```sql
-- AND 연산자
SELECT true AND true;    -- true
SELECT true AND false;   -- false
SELECT true AND null;    -- null

-- OR 연산자
SELECT false OR true;    -- true
SELECT false OR null;    -- null

-- NOT 연산자
SELECT NOT true;         -- false
SELECT NOT null;         -- null
```

---

# 9.2. 비교 함수와 연산자 (Comparison Functions and Operators)

## 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `<` | 작다 (Less than) |
| `>` | 크다 (Greater than) |
| `<=` | 작거나 같다 (Less than or equal to) |
| `>=` | 크거나 같다 (Greater than or equal to) |
| `=` | 같다 (Equal) |
| `<>` | 같지 않다 (Not equal, 표준 SQL) |
| `!=` | 같지 않다 (`<>`의 별칭) |

참고: `<>`는 표준 SQL 표기법입니다. `!=`는 파싱 시 `<>`로 변환됩니다.

## 비교 서술어 (Comparison Predicates)

### BETWEEN

```sql
a BETWEEN x AND y        -- a >= x AND a <= y 와 동일
a NOT BETWEEN x AND y    -- BETWEEN의 부정
a BETWEEN SYMMETRIC x AND y  -- 필요시 끝점을 자동 교환
```

예제:
```sql
SELECT 5 BETWEEN 1 AND 10;           -- true
SELECT 5 BETWEEN 10 AND 1;           -- false
SELECT 5 BETWEEN SYMMETRIC 10 AND 1; -- true (자동 교환)
```

### NULL 처리

```sql
a IS NULL               -- null 여부 검사
a IS NOT NULL           -- null이 아닌지 검사
a ISNULL                -- 비표준 구문
a NOTNULL               -- 비표준 구문
```

중요: `expression = NULL`은 절대 사용하지 마세요. 항상 `IS NULL`을 사용하세요.

### DISTINCT FROM (NULL-안전 비교)

```sql
a IS DISTINCT FROM b        -- NULL을 비교 가능한 값으로 취급하며 같지 않음
a IS NOT DISTINCT FROM b    -- NULL을 비교 가능한 값으로 취급하며 같음
```

예제:
```sql
SELECT 1 IS DISTINCT FROM NULL;          -- true (NULL이 아님)
SELECT NULL IS DISTINCT FROM NULL;       -- false
SELECT NULL IS NOT DISTINCT FROM NULL;   -- true
```

### Boolean 검사

```sql
boolean_expression IS TRUE
boolean_expression IS NOT TRUE
boolean_expression IS FALSE
boolean_expression IS NOT FALSE
boolean_expression IS UNKNOWN
boolean_expression IS NOT UNKNOWN
```

## 비교 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `num_nonnulls(VARIADIC "any")` | null이 아닌 인자 수 반환 | `num_nonnulls(1, NULL, 2)` → `2` |
| `num_nulls(VARIADIC "any")` | null인 인자 수 반환 | `num_nulls(1, NULL, 2)` → `1` |

---

# 9.3. 수학 함수와 연산자 (Mathematical Functions and Operators)

## 산술 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `+` | 덧셈 | `2 + 3` → `5` |
| `+` | 단항 양수 | `+ 3.5` → `3.5` |
| `-` | 뺄셈 | `2 - 3` → `-1` |
| `-` | 부정 | `- (-4)` → `4` |
| `*` | 곱셈 | `2 * 3` → `6` |
| `/` | 나눗셈 | `5.0 / 2` → `2.5`; `5 / 2` → `2` |
| `%` | 나머지 (Modulo) | `5 % 4` → `1` |
| `^` | 거듭제곱 | `2 ^ 3` → `8` |

참고: 정수 나눗셈은 소수점 이하를 버립니다.

## 비트 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `&` | 비트 AND | `91 & 15` → `11` |
| `\|` | 비트 OR | `32 \| 3` → `35` |
| `#` | 비트 XOR | `17 # 5` → `20` |
| `~` | 비트 NOT | `~1` → `-2` |
| `<<` | 왼쪽 시프트 | `1 << 4` → `16` |
| `>>` | 오른쪽 시프트 | `8 >> 2` → `2` |

## 기본 수학 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `abs(numeric_type)` | 절대값 | `abs(-17.4)` → `17.4` |
| `ceil(numeric)` / `ceiling(numeric)` | 올림 | `ceil(42.2)` → `43` |
| `floor(numeric)` | 내림 | `floor(42.8)` → `42` |
| `round(numeric)` | 반올림 | `round(42.4)` → `42` |
| `round(v numeric, s integer)` | 소수점 s자리로 반올림 | `round(42.4382, 2)` → `42.44` |
| `trunc(numeric)` | 0 방향으로 절삭 | `trunc(42.8)` → `42` |
| `sign(numeric)` | 부호 (-1, 0, +1) | `sign(-8.4)` → `-1` |

### 예제

```sql
SELECT abs(-15);           -- 15
SELECT ceil(4.3);          -- 5
SELECT floor(4.7);         -- 4
SELECT round(4.567, 2);    -- 4.57
SELECT trunc(4.567, 2);    -- 4.56
SELECT sign(-5);           -- -1
```

## 제곱근과 거듭제곱 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `sqrt(numeric)` | 제곱근 | `sqrt(2)` → `1.414...` |
| `cbrt(double precision)` | 세제곱근 | `cbrt(64.0)` → `4` |
| `power(a numeric, b numeric)` | a의 b 거듭제곱 | `power(9, 3)` → `729` |
| `\|/` | 제곱근 연산자 | `\|/ 25.0` → `5` |
| `\|\|/` | 세제곱근 연산자 | `\|\|/ 64.0` → `4` |

### 예제

```sql
SELECT sqrt(16);           -- 4
SELECT cbrt(27);           -- 3
SELECT power(2, 10);       -- 1024
SELECT |/ 144;             -- 12
```

## 로그와 지수 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `exp(numeric)` | e의 x 거듭제곱 | `exp(1.0)` → `2.718...` |
| `ln(numeric)` | 자연 로그 | `ln(2.0)` → `0.693...` |
| `log(numeric)` | 밑이 10인 로그 | `log(100)` → `2` |
| `log(b numeric, x numeric)` | 밑이 b인 x의 로그 | `log(2.0, 64.0)` → `6.0` |

### 예제

```sql
SELECT exp(1);             -- 2.718281828...
SELECT ln(exp(1));         -- 1
SELECT log(1000);          -- 3
SELECT log(2, 8);          -- 3
```

## 고급 수학 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `factorial(bigint)` | 팩토리얼 | `factorial(5)` → `120` |
| `gcd(numeric_type, numeric_type)` | 최대공약수 | `gcd(12, 8)` → `4` |
| `lcm(numeric_type, numeric_type)` | 최소공배수 | `lcm(12, 8)` → `24` |
| `div(y numeric, x numeric)` | 정수 나눗셈 | `div(9, 4)` → `2` |
| `mod(y numeric_type, x numeric_type)` | 나머지 | `mod(9, 4)` → `1` |

## 삼각 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `sin(double precision)` | 사인 (라디안) | `sin(1)` → `0.841...` |
| `sind(double precision)` | 사인 (도) | `sind(30)` → `0.5` |
| `cos(double precision)` | 코사인 (라디안) | `cos(0)` → `1` |
| `cosd(double precision)` | 코사인 (도) | `cosd(60)` → `0.5` |
| `tan(double precision)` | 탄젠트 (라디안) | `tan(1)` → `1.557...` |
| `tand(double precision)` | 탄젠트 (도) | `tand(45)` → `1` |
| `asin(double precision)` | 역 사인 (라디안) | `asin(1)` → `1.570...` |
| `acos(double precision)` | 역 코사인 (라디안) | `acos(1)` → `0` |
| `atan(double precision)` | 역 탄젠트 (라디안) | `atan(1)` → `0.785...` |
| `atan2(y, x)` | 2인자 역 탄젠트 | `atan2(1, 0)` → `1.570...` |

### 예제

```sql
SELECT sin(radians(30));   -- 0.5
SELECT cos(radians(60));   -- 0.5
SELECT tan(radians(45));   -- 1
SELECT degrees(asin(0.5)); -- 30
```

## 쌍곡선 함수

| 함수 | 설명 |
|------|------|
| `sinh(double precision)` | 쌍곡선 사인 |
| `cosh(double precision)` | 쌍곡선 코사인 |
| `tanh(double precision)` | 쌍곡선 탄젠트 |
| `asinh(double precision)` | 역 쌍곡선 사인 |
| `acosh(double precision)` | 역 쌍곡선 코사인 |
| `atanh(double precision)` | 역 쌍곡선 탄젠트 |

## 각도 변환 및 특수 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `radians(double precision)` | 도를 라디안으로 변환 | `radians(180)` → `3.141...` |
| `degrees(double precision)` | 라디안을 도로 변환 | `degrees(pi())` → `180` |
| `pi()` | 원주율 값 | `pi()` → `3.141592653...` |

## 난수 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `random()` | 0.0 ≤ x < 1.0 범위의 난수 | `random()` → `0.897...` |
| `random(min integer, max integer)` | 범위 내 정수 난수 | `random(1, 10)` → `7` |
| `random_normal([mean, stddev])` | 정규 분포 난수 | `random_normal(0.0, 1.0)` |
| `setseed(double precision)` | 난수 생성기 시드 설정 | `setseed(0.12345)` |

### 예제

```sql
-- 1에서 100 사이의 난수 정수
SELECT random(1, 100);

-- 0과 1 사이의 난수
SELECT random();

-- 시드 설정 후 재현 가능한 난수
SELECT setseed(0.5);
SELECT random();  -- 항상 동일한 값
```

---

# 9.4. 문자열 함수와 연산자 (String Functions and Operators)

## 문자열 연결 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `\|\|` | 두 문자열 연결 | `'Post' \|\| 'greSQL'` → `PostgreSQL` |
| `\|\|` (비문자열과 함께) | 비문자열을 텍스트로 변환 후 연결 | `'Value: ' \|\| 42` → `Value: 42` |

### 예제

```sql
SELECT 'Hello' || ' ' || 'World';  -- Hello World
SELECT 'Count: ' || 42;            -- Count: 42
```

## 대소문자 변환

| 함수 | 설명 | 예제 |
|------|------|------|
| `lower(text)` | 소문자로 변환 | `lower('TOM')` → `tom` |
| `upper(text)` | 대문자로 변환 | `upper('tom')` → `TOM` |
| `initcap(text)` | 각 단어 첫 글자 대문자 | `initcap('hi THOMAS')` → `Hi Thomas` |

### 예제

```sql
SELECT lower('PostgreSQL');        -- postgresql
SELECT upper('postgresql');        -- POSTGRESQL
SELECT initcap('hello world');     -- Hello World
```

## 문자열 길이 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `length(text)` | 문자 수 | `length('jose')` → `4` |
| `char_length(text)` | 문자 수 (별칭) | `char_length('jose')` → `4` |
| `octet_length(text)` | 바이트 수 | `octet_length('jose')` → `4` |
| `bit_length(text)` | 비트 수 | `bit_length('jose')` → `32` |

### 예제

```sql
SELECT length('PostgreSQL');       -- 10
SELECT octet_length('한글');       -- 6 (UTF-8에서 한글은 3바이트)
SELECT char_length('한글');        -- 2
```

## 부분 문자열 / 위치 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `substring(string FROM start FOR count)` | 부분 문자열 추출 | `substring('Thomas' from 2 for 3)` → `hom` |
| `substring(string FROM pattern)` | 정규식으로 추출 | `substring('Thomas' from '...$')` → `mas` |
| `strpos(string, substring)` | 부분 문자열 첫 위치 | `strpos('high', 'ig')` → `2` |
| `position(substring IN string)` | 첫 위치 (SQL 표준) | `position('om' in 'Thomas')` → `3` |
| `left(string, n)` | 처음 n 문자 | `left('abcde', 2)` → `ab` |
| `right(string, n)` | 마지막 n 문자 | `right('abcde', 2)` → `de` |
| `substr(string, start [, count])` | 부분 문자열 추출 | `substr('alphabet', 3, 2)` → `ph` |

### 예제

```sql
SELECT substring('PostgreSQL' from 1 for 4);  -- Post
SELECT left('PostgreSQL', 4);                  -- Post
SELECT right('PostgreSQL', 3);                 -- SQL
SELECT strpos('PostgreSQL', 'gre');           -- 5
SELECT position('SQL' in 'PostgreSQL');       -- 8
```

## 공백 제거 함수 (Trim)

| 함수 | 설명 | 예제 |
|------|------|------|
| `btrim(string [, characters])` | 양쪽에서 제거 (기본: 공백) | `btrim('xyxtrimyyx', 'xyz')` → `trim` |
| `ltrim(string [, characters])` | 왼쪽에서 제거 | `ltrim('zzzytest', 'xyz')` → `test` |
| `rtrim(string [, characters])` | 오른쪽에서 제거 | `rtrim('testxxzx', 'xyz')` → `test` |
| `trim([LEADING\|TRAILING\|BOTH] [chars] FROM string)` | 지정 문자 제거 | `trim(both 'xyz' from 'yxTomxx')` → `Tom` |

### 예제

```sql
SELECT trim('  hello  ');                     -- hello
SELECT ltrim('   hello');                     -- hello
SELECT rtrim('hello   ');                     -- hello
SELECT trim(both 'x' from 'xxxhelloxxx');    -- hello
```

## 문자열 치환 및 조작

| 함수 | 설명 | 예제 |
|------|------|------|
| `replace(string, from, to)` | 모든 발생 치환 | `replace('abcdef', 'cd', 'XX')` → `abXXef` |
| `translate(string, from, to)` | 각 문자 치환 | `translate('12345', '143', 'ax')` → `a2x5` |
| `reverse(text)` | 문자열 뒤집기 | `reverse('abcde')` → `edcba` |
| `repeat(string, number)` | 문자열 n번 반복 | `repeat('Pg', 4)` → `PgPgPgPg` |
| `overlay(string PLACING new FROM start [FOR count])` | 부분 문자열 교체 | `overlay('Txxxxas' placing 'hom' from 2 for 4)` → `Thomas` |
| `lpad(string, length [, fill])` | 왼쪽 패딩 | `lpad('hi', 5, 'xy')` → `xyxhi` |
| `rpad(string, length [, fill])` | 오른쪽 패딩 | `rpad('hi', 5, 'xy')` → `hixyx` |

### 예제

```sql
SELECT replace('Hello World', 'World', 'PostgreSQL');  -- Hello PostgreSQL
SELECT reverse('PostgreSQL');                           -- LQSergtsoP
SELECT repeat('Ab', 3);                                 -- AbAbAb
SELECT lpad('42', 5, '0');                             -- 00042
SELECT rpad('hello', 10, '.');                         -- hello.....
```

## format 함수

문자열 포맷팅을 위한 강력한 함수입니다.

```sql
format(formatstr text [, formatarg any [, ...] ]) → text
```

포맷 지정자:
- `%s` - 단순 문자열
- `%I` - SQL 식별자 (인용 부호 포함)
- `%L` - SQL 리터럴 (인용 부호 포함)
- `%%` - 리터럴 `%` 문자

### 예제

```sql
SELECT format('Hello %s', 'World');
-- 결과: Hello World

SELECT format('INSERT INTO %I VALUES(%L)', 'Foo bar', E'O\'Reilly');
-- 결과: INSERT INTO "Foo bar" VALUES('O''Reilly')

SELECT format('|%10s|', 'foo');
-- 결과: |       foo|

SELECT format('Testing %3$s, %2$s, %1$s', 'one', 'two', 'three');
-- 결과: Testing three, two, one
```

## 문자열 연결 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `concat(val1 any [, val2 any [, ...] ])` | 모든 인자 연결 (NULL 무시) | `concat('abc', 2, NULL, 22)` → `abc222` |
| `concat_ws(sep, val1 any [, val2 any [, ...] ])` | 구분자로 연결 (NULL 무시) | `concat_ws(',', 'a', 'b', NULL, 'c')` → `a,b,c` |

### 예제

```sql
SELECT concat('Hello', ' ', 'World');          -- Hello World
SELECT concat('Value: ', NULL, 42);            -- Value: 42
SELECT concat_ws(', ', 'apple', 'banana', 'cherry');  -- apple, banana, cherry
SELECT concat_ws('-', 2024, 01, 15);           -- 2024-1-15
```

## 기타 유틸리티 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `starts_with(string, prefix)` | 접두사 확인 | `starts_with('alphabet', 'alph')` → `t` |
| `ascii(text)` | 첫 문자의 ASCII 코드 | `ascii('x')` → `120` |
| `chr(integer)` | 코드에서 문자 반환 | `chr(65)` → `A` |
| `quote_ident(text)` | SQL 식별자 인용 | `quote_ident('Foo bar')` → `"Foo bar"` |
| `quote_literal(text)` | SQL 리터럴 인용 | `quote_literal(E'O\'Reilly')` → `'O''Reilly'` |
| `md5(text)` | MD5 해시 (16진수) | `md5('abc')` → `900150983cd24fb0...` |

---

# 9.5. 이진 문자열 함수와 연산자 (Binary String Functions)

이진 문자열(`bytea`)을 처리하는 함수들입니다.

| 함수 | 설명 | 예제 |
|------|------|------|
| `octet_length(bytea)` | 바이트 수 | `octet_length('\x123456'::bytea)` → `3` |
| `get_byte(bytea, integer)` | 지정 위치의 바이트 추출 | `get_byte('\x1234'::bytea, 0)` → `18` |
| `set_byte(bytea, integer, integer)` | 바이트 설정 | `set_byte('\x1234'::bytea, 0, 16)` |
| `encode(bytea, text)` | 인코딩 (base64, hex 등) | `encode('123'::bytea, 'base64')` → `MTIz` |
| `decode(text, text)` | 디코딩 | `decode('MTIz', 'base64')` → `\x313233` |

---

# 9.6. 비트 문자열 함수와 연산자 (Bit String Functions)

비트 문자열(`bit`, `bit varying`)을 처리하는 함수들입니다.

| 연산자/함수 | 설명 |
|-------------|------|
| `\|\|` | 비트 문자열 연결 |
| `&` | 비트 AND |
| `\|` | 비트 OR |
| `#` | 비트 XOR |
| `~` | 비트 NOT |
| `<<` | 왼쪽 시프트 |
| `>>` | 오른쪽 시프트 |
| `bit_length(bit)` | 비트 수 |
| `length(bit)` | 비트 수 |

---

# 9.7. 패턴 매칭 (Pattern Matching)

PostgreSQL은 세 가지 패턴 매칭 방법을 제공합니다.

## 9.7.1. LIKE 연산자

구문:
```sql
string LIKE pattern [ESCAPE escape-character]
string NOT LIKE pattern [ESCAPE escape-character]
```

와일드카드:
- `_` - 단일 문자 매칭
- `%` - 0개 이상의 문자 시퀀스 매칭

### 예제

```sql
SELECT 'abc' LIKE 'abc';      -- true
SELECT 'abc' LIKE 'a%';       -- true
SELECT 'abc' LIKE '_b_';      -- true
SELECT 'abc' LIKE 'c';        -- false
SELECT 'abc' LIKE '%b%';      -- true
```

### 대소문자 무시: ILIKE

```sql
SELECT 'AbC' ILIKE 'abc';     -- true
SELECT 'ABC' ILIKE 'a%c';     -- true
```

연산자 별칭:
- `~~` = `LIKE`
- `~~*` = `ILIKE`
- `!~~` = `NOT LIKE`
- `!~~*` = `NOT ILIKE`

## 9.7.2. SIMILAR TO (SQL:1999 정규 표현식)

구문:
```sql
string SIMILAR TO pattern [ESCAPE escape-character]
string NOT SIMILAR TO pattern [ESCAPE escape-character]
```

메타문자:
- `|` - 대안 (둘 중 하나)
- `*` - 0회 이상 반복
- `+` - 1회 이상 반복
- `?` - 0회 또는 1회
- `{m}` - 정확히 m회 반복
- `{m,}` - m회 이상 반복
- `{m,n}` - m회에서 n회 반복
- `()` - 그룹화
- `[...]` - 문자 클래스

### 예제

```sql
SELECT 'abc' SIMILAR TO 'abc';          -- true
SELECT 'abc' SIMILAR TO 'a';            -- false
SELECT 'abc' SIMILAR TO '%(b|d)%';      -- true
SELECT 'abc' SIMILAR TO '(b|c)%';       -- false
SELECT 'abc' SIMILAR TO 'a(b|c)+';      -- true
```

## 9.7.3. POSIX 정규 표현식

연산자:
- `~` - 매칭 (대소문자 구분)
- `~*` - 매칭 (대소문자 무시)
- `!~` - 매칭 안 됨 (대소문자 구분)
- `!~*` - 매칭 안 됨 (대소문자 무시)

### 예제

```sql
SELECT 'thomas' ~ 't.*ma';      -- true
SELECT 'thomas' ~* 'T.*ma';     -- true
SELECT 'thomas' !~ 't.*max';    -- true
```

### 정규 표현식 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `regexp_like(string, pattern [, flags])` | 불리언 매칭 | `regexp_like('Hello', 'hello', 'i')` → `true` |
| `regexp_match(string, pattern [, flags])` | 첫 번째 매칭 캡처 | `regexp_match('foobar', '(bar)')` → `{bar}` |
| `regexp_matches(string, pattern [, flags])` | 모든 매칭 캡처 | 여러 행 반환 |
| `regexp_replace(string, pattern, replacement [, flags])` | 매칭 치환 | `regexp_replace('Thomas', '.[mN]a.', 'M')` → `ThM` |
| `regexp_count(string, pattern [, start [, flags]])` | 매칭 수 | `regexp_count('abcabc', 'a')` → `2` |
| `regexp_split_to_array(string, pattern [, flags])` | 배열로 분할 | `regexp_split_to_array('a b c', '\s+')` → `{a,b,c}` |

### 정규 표현식 예제

```sql
-- 이메일 형식 검증
SELECT 'user@example.com' ~ '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$';
-- true

-- 전화번호에서 숫자만 추출
SELECT regexp_replace('010-1234-5678', '[^0-9]', '', 'g');
-- 01012345678

-- 문자열 분할
SELECT regexp_split_to_array('apple,banana;cherry', '[,;]');
-- {apple,banana,cherry}

-- 모든 단어 추출
SELECT regexp_matches('Hello World PostgreSQL', '\w+', 'g');
-- {Hello}
-- {World}
-- {PostgreSQL}
```

---

# 9.8. 데이터 타입 포맷팅 함수 (Data Type Formatting Functions)

PostgreSQL은 다양한 데이터 타입을 문자열로 변환하고, 문자열을 데이터 타입으로 변환하는 함수를 제공합니다.

## 주요 함수

| 함수 | 설명 |
|------|------|
| `to_char(timestamp, text)` | 타임스탬프를 문자열로 |
| `to_char(interval, text)` | 인터벌을 문자열로 |
| `to_char(numeric, text)` | 숫자를 문자열로 |
| `to_date(text, text)` | 문자열을 날짜로 |
| `to_timestamp(text, text)` | 문자열을 타임스탬프로 |
| `to_number(text, text)` | 문자열을 숫자로 |

### 예제

```sql
SELECT to_char(current_timestamp, 'YYYY-MM-DD HH24:MI:SS');
-- 2024-01-15 14:30:00

SELECT to_char(125.8::real, '999D99');
-- 125.80

SELECT to_date('2024-01-15', 'YYYY-MM-DD');
-- 2024-01-15

SELECT to_number('12,454.8-', '99G999D9S');
-- -12454.8
```

---

# 9.9. 날짜/시간 함수와 연산자 (Date/Time Functions and Operators)

## EXTRACT / date_part

날짜/시간 값에서 하위 필드를 추출합니다.

```sql
EXTRACT(field FROM source)
date_part('field', source)
```

유효한 필드:
- `year`, `month`, `day`, `hour`, `minute`, `second`
- `century`, `decade`, `millennium`
- `quarter`, `week`, `isoyear`, `isodow`, `doy`, `dow`
- `epoch`, `julian`, `timezone`, `timezone_hour`, `timezone_minute`
- `microseconds`, `milliseconds`

### 예제

```sql
SELECT EXTRACT(YEAR FROM TIMESTAMP '2024-01-15 20:38:40');
-- 2024

SELECT EXTRACT(HOUR FROM TIMESTAMP '2024-01-15 20:38:40');
-- 20

SELECT EXTRACT(DOW FROM DATE '2024-01-15');  -- 요일 (0=일요일)
-- 1

SELECT date_part('month', TIMESTAMP '2024-01-15 20:38:40');
-- 1

SELECT EXTRACT(EPOCH FROM INTERVAL '1 day');
-- 86400
```

## date_trunc

지정된 정밀도로 절삭합니다.

```sql
date_trunc('field', source [, time_zone])
```

유효한 필드: `microseconds`, `milliseconds`, `second`, `minute`, `hour`, `day`, `week`, `month`, `quarter`, `year`, `decade`, `century`, `millennium`

### 예제

```sql
SELECT date_trunc('hour', TIMESTAMP '2024-01-15 20:38:40');
-- 2024-01-15 20:00:00

SELECT date_trunc('month', TIMESTAMP '2024-01-15 20:38:40');
-- 2024-01-01 00:00:00

SELECT date_trunc('year', TIMESTAMP '2024-01-15 20:38:40');
-- 2024-01-01 00:00:00
```

## date_bin

타임스탬프를 지정된 간격의 빈(bin)으로 분류합니다.

```sql
date_bin('stride', source, origin)
```

### 예제

```sql
SELECT date_bin('15 minutes', TIMESTAMP '2024-01-15 15:44:17', TIMESTAMP '2024-01-01');
-- 2024-01-15 15:30:00

SELECT date_bin('1 hour', TIMESTAMP '2024-01-15 15:44:17', TIMESTAMP '2024-01-01');
-- 2024-01-15 15:00:00
```

## 현재 날짜/시간 함수

SQL 표준 (트랜잭션 시작 시간):

```sql
CURRENT_DATE              -- 현재 날짜
CURRENT_TIME [(precision)]             -- 현재 시간 (시간대 포함)
CURRENT_TIMESTAMP [(precision)]        -- 현재 타임스탬프 (시간대 포함)
LOCALTIME [(precision)]                -- 현재 로컬 시간
LOCALTIMESTAMP [(precision)]           -- 현재 로컬 타임스탬프
```

PostgreSQL 전용:

```sql
now()                     -- CURRENT_TIMESTAMP와 동일
transaction_timestamp()   -- 현재 트랜잭션 시작 시간
statement_timestamp()     -- 현재 문장 시작 시간
clock_timestamp()         -- 실제 현재 시간 (문장 내에서 변함)
timeofday()               -- 현재 시간을 텍스트 문자열로
```

### 예제

```sql
SELECT CURRENT_DATE;                    -- 2024-01-15
SELECT CURRENT_TIME;                    -- 14:30:00.123456+09
SELECT CURRENT_TIMESTAMP;               -- 2024-01-15 14:30:00.123456+09
SELECT now();                           -- 2024-01-15 14:30:00.123456+09
SELECT clock_timestamp();               -- 실제 현재 시간

-- 트랜잭션 내에서 차이 확인
BEGIN;
SELECT now();                           -- 14:30:00
SELECT pg_sleep(2);
SELECT now();                           -- 여전히 14:30:00 (트랜잭션 시작 시간)
SELECT clock_timestamp();               -- 14:30:02 (실제 시간)
COMMIT;
```

## 시간대 함수

### AT TIME ZONE / AT LOCAL

```sql
-- timestamp without time zone → with time zone
TIMESTAMP '2024-01-15 20:38:40' AT TIME ZONE 'America/New_York'

-- timestamp with time zone → without time zone
TIMESTAMPTZ '2024-01-15 20:38:40+09' AT TIME ZONE 'UTC'

-- AT LOCAL은 세션 시간대 사용
TIMESTAMP '2024-01-15 20:38:40' AT LOCAL
```

### 예제

```sql
SELECT TIMESTAMP '2024-01-15 12:00:00' AT TIME ZONE 'UTC';
-- 2024-01-15 12:00:00+00

SELECT TIMESTAMPTZ '2024-01-15 12:00:00+09' AT TIME ZONE 'UTC';
-- 2024-01-15 03:00:00

SELECT TIMESTAMP '2024-01-15 12:00:00' AT TIME ZONE 'Asia/Seoul';
-- 2024-01-15 12:00:00+09
```

## 날짜/시간 연산자

### 덧셈

```sql
date + integer → date                    -- 일 추가
date + interval → timestamp
date + time → timestamp
timestamp + interval → timestamp
time + interval → time
interval + interval → interval
```

### 뺄셈

```sql
date - date → integer                    -- 일 수 차이
date - integer → date
date - interval → timestamp
time - time → interval
time - interval → time
timestamp - interval → timestamp
timestamp - timestamp → interval
interval - interval → interval
- interval → interval                    -- 부정
```

### 곱셈 & 나눗셈

```sql
interval * double precision → interval
interval / double precision → interval
```

### 예제

```sql
SELECT DATE '2024-01-15' + 7;            -- 2024-01-22
SELECT DATE '2024-01-15' + INTERVAL '1 month';  -- 2024-02-15 00:00:00
SELECT DATE '2024-01-15' - DATE '2024-01-01';   -- 14
SELECT INTERVAL '1 hour' * 3.5;          -- 03:30:00
SELECT TIME '05:00' - TIME '03:00';      -- 02:00:00
```

## age 함수

두 타임스탬프 사이의 나이/차이를 계산합니다.

```sql
age(timestamp, timestamp) → interval
age(timestamp) → interval                -- 현재로부터의 나이
```

### 예제

```sql
SELECT age(TIMESTAMP '2024-01-15', TIMESTAMP '2000-01-01');
-- 24 years 14 days

SELECT age(TIMESTAMP '2000-06-15');  -- 현재가 2024-01-15라면
-- 23 years 7 mons

SELECT age(TIMESTAMP '2024-06-15', TIMESTAMP '2024-01-15');
-- 5 mons
```

## 날짜/시간 생성 함수

```sql
make_date(year, month, day)
make_time(hour, min, sec)
make_timestamp(year, month, day, hour, min, sec)
make_timestamptz(year, month, day, hour, min, sec [, timezone])
make_interval([years, months, weeks, days, hours, mins, secs])
```

### 예제

```sql
SELECT make_date(2024, 1, 15);           -- 2024-01-15
SELECT make_time(14, 30, 0);             -- 14:30:00
SELECT make_timestamp(2024, 1, 15, 14, 30, 0);  -- 2024-01-15 14:30:00
SELECT make_interval(days => 10, hours => 5);  -- 10 days 05:00:00
```

## OVERLAPS 연산자

두 기간이 겹치는지 검사합니다.

```sql
(start1, end1) OVERLAPS (start2, end2)
(start1, length1) OVERLAPS (start2, length2)
```

### 예제

```sql
SELECT (DATE '2024-01-01', DATE '2024-06-30') OVERLAPS
       (DATE '2024-03-01', DATE '2024-12-31');
-- true

SELECT (DATE '2024-01-01', DATE '2024-02-01') OVERLAPS
       (DATE '2024-03-01', DATE '2024-04-01');
-- false
```

---

# 9.10. 열거형 지원 함수 (Enum Support Functions)

열거형(Enum) 타입을 위한 함수들입니다.

| 함수 | 설명 |
|------|------|
| `enum_first(anyenum)` | 열거형의 첫 번째 값 |
| `enum_last(anyenum)` | 열거형의 마지막 값 |
| `enum_range(anyenum)` | 열거형의 모든 값 배열 |
| `enum_range(anyenum, anyenum)` | 지정 범위의 값 배열 |

### 예제

```sql
CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');

SELECT enum_first(null::mood);           -- sad
SELECT enum_last(null::mood);            -- happy
SELECT enum_range(null::mood);           -- {sad,ok,happy}
SELECT enum_range('ok'::mood, 'happy'::mood);  -- {ok,happy}
```

---

# 9.18. 조건 표현식 (Conditional Expressions)

## 9.18.1. CASE

if/else 문과 유사한 범용 조건 표현식입니다.

일반 형식:

```sql
CASE WHEN condition THEN result
     [WHEN ...]
     [ELSE result]
END
```

단순 형식:

```sql
CASE expression
    WHEN value THEN result
    [WHEN ...]
    [ELSE result]
END
```

### 예제

```sql
-- 일반 형식
SELECT
    name,
    CASE
        WHEN score >= 90 THEN 'A'
        WHEN score >= 80 THEN 'B'
        WHEN score >= 70 THEN 'C'
        ELSE 'F'
    END AS grade
FROM students;

-- 단순 형식
SELECT
    CASE status
        WHEN 1 THEN '활성'
        WHEN 2 THEN '대기'
        WHEN 3 THEN '삭제'
        ELSE '알 수 없음'
    END AS status_name
FROM orders;

-- 0으로 나누기 방지
SELECT
    CASE WHEN divisor <> 0 THEN dividend / divisor
         ELSE NULL
    END
FROM calculations;
```

## 9.18.2. COALESCE

첫 번째 null이 아닌 인자를 반환합니다. 모든 인자가 null이면 null을 반환합니다.

```sql
COALESCE(arg1, arg2, ...)
```

### 예제

```sql
SELECT COALESCE(NULL, NULL, 'default');    -- default
SELECT COALESCE(NULL, 'second', 'third');  -- second
SELECT COALESCE('first', 'second');        -- first

-- 실용적인 사용
SELECT COALESCE(nickname, username, 'Anonymous') AS display_name
FROM users;

-- NULL 기본값 설정
SELECT product_name, COALESCE(description, '설명 없음') AS description
FROM products;
```

## 9.18.3. NULLIF

두 값이 같으면 null을 반환하고, 그렇지 않으면 첫 번째 값을 반환합니다.

```sql
NULLIF(value1, value2)
```

### 예제

```sql
SELECT NULLIF(1, 1);          -- null
SELECT NULLIF(1, 2);          -- 1
SELECT NULLIF('hello', '');   -- hello
SELECT NULLIF('', '');        -- null

-- 0으로 나누기 방지
SELECT numerator / NULLIF(denominator, 0)
FROM calculations;

-- 빈 문자열을 NULL로 변환
SELECT NULLIF(trim(value), '')
FROM data;
```

## 9.18.4. GREATEST와 LEAST

표현식 목록에서 가장 큰 값 또는 가장 작은 값을 선택합니다.

```sql
GREATEST(expression1, expression2, ...)
LEAST(expression1, expression2, ...)
```

### 예제

```sql
SELECT GREATEST(1, 2, 3);              -- 3
SELECT LEAST(1, 2, 3);                 -- 1
SELECT GREATEST('a', 'b', 'c');        -- c
SELECT LEAST('apple', 'banana');       -- apple

-- NULL 값은 무시됨
SELECT GREATEST(1, NULL, 3);           -- 3
SELECT LEAST(NULL, 2, 3);              -- 2

-- 실용적인 사용: 범위 제한
SELECT LEAST(GREATEST(value, 0), 100) AS clamped_value  -- 0~100 범위로 제한
FROM data;

-- 날짜 비교
SELECT GREATEST(start_date, '2024-01-01'::date) AS effective_start
FROM contracts;
```

---

# 9.19. 배열 함수와 연산자 (Array Functions and Operators)

## 배열 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `@>` | 포함 - 첫 번째 배열이 두 번째를 포함? | `ARRAY[1,4,3] @> ARRAY[3,1]` → `t` |
| `<@` | 포함됨 - 첫 번째 배열이 두 번째에 포함? | `ARRAY[2,7] <@ ARRAY[1,7,4,2,6]` → `t` |
| `&&` | 겹침 - 공통 요소가 있는가? | `ARRAY[1,4,3] && ARRAY[2,1]` → `t` |
| `\|\|` | 배열 연결 또는 요소 추가/앞에 추가 | `ARRAY[1,2] \|\| ARRAY[3,4]` → `{1,2,3,4}` |

### 예제

```sql
SELECT ARRAY[1,2,3] @> ARRAY[2,3];              -- true
SELECT ARRAY[1,2] <@ ARRAY[1,2,3,4];            -- true
SELECT ARRAY[1,2,3] && ARRAY[3,4,5];            -- true (3이 공통)
SELECT ARRAY[1,2] || ARRAY[3,4];                -- {1,2,3,4}
SELECT ARRAY[1,2] || 3;                         -- {1,2,3}
SELECT 0 || ARRAY[1,2];                         -- {0,1,2}
```

## 배열 함수

### 수정 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `array_append(array, element)` | 끝에 요소 추가 | `array_append(ARRAY[1,2], 3)` → `{1,2,3}` |
| `array_prepend(element, array)` | 앞에 요소 추가 | `array_prepend(0, ARRAY[1,2])` → `{0,1,2}` |
| `array_cat(array1, array2)` | 두 배열 연결 | `array_cat(ARRAY[1,2], ARRAY[3,4])` → `{1,2,3,4}` |
| `array_remove(array, element)` | 일치하는 요소 모두 제거 | `array_remove(ARRAY[1,2,3,2], 2)` → `{1,3}` |
| `array_replace(array, from, to)` | 요소 치환 | `array_replace(ARRAY[1,2,5,4], 5, 3)` → `{1,2,3,4}` |
| `array_reverse(array)` | 배열 뒤집기 | `array_reverse(ARRAY[1,2,3])` → `{3,2,1}` |

### 정보 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `array_dims(array)` | 차원 텍스트 표현 | `array_dims(ARRAY[[1,2],[3,4]])` → `[1:2][1:2]` |
| `array_length(array, dim)` | 특정 차원의 길이 | `array_length(ARRAY[1,2,3], 1)` → `3` |
| `array_ndims(array)` | 차원 수 | `array_ndims(ARRAY[[1,2],[3,4]])` → `2` |
| `array_lower(array, dim)` | 차원의 하한 | `array_lower(ARRAY[1,2,3], 1)` → `1` |
| `array_upper(array, dim)` | 차원의 상한 | `array_upper(ARRAY[1,2,3], 1)` → `3` |
| `cardinality(array)` | 전체 요소 수 | `cardinality(ARRAY[[1,2],[3,4]])` → `4` |

### 검색 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `array_position(array, element [, start])` | 첫 번째 발생 위치 | `array_position(ARRAY['a','b','c'], 'b')` → `2` |
| `array_positions(array, element)` | 모든 발생 위치 배열 | `array_positions(ARRAY['A','B','A'], 'A')` → `{1,3}` |

### 생성 및 변환 함수

| 함수 | 설명 | 예제 |
|------|------|------|
| `array_fill(element, dims)` | 값으로 채운 배열 생성 | `array_fill(0, ARRAY[3])` → `{0,0,0}` |
| `array_to_string(array, sep [, null])` | 구분자로 문자열 변환 | `array_to_string(ARRAY[1,2,3], ',')` → `1,2,3` |
| `string_to_array(text, sep [, null])` | 문자열을 배열로 | `string_to_array('1,2,3', ',')` → `{1,2,3}` |

### 집합 연산

| 함수 | 설명 |
|------|------|
| `unnest(array)` | 배열을 행 집합으로 확장 |

### 예제

```sql
-- 배열 생성 및 조작
SELECT array_append(ARRAY[1,2,3], 4);           -- {1,2,3,4}
SELECT array_prepend(0, ARRAY[1,2,3]);          -- {0,1,2,3}
SELECT array_remove(ARRAY[1,2,2,3], 2);         -- {1,3}

-- 배열 정보
SELECT array_length(ARRAY[1,2,3,4,5], 1);       -- 5
SELECT cardinality(ARRAY[[1,2],[3,4]]);         -- 4

-- 검색
SELECT array_position(ARRAY['월','화','수','목','금'], '수');  -- 3

-- unnest로 배열 확장
SELECT unnest(ARRAY['apple', 'banana', 'cherry']);
-- apple
-- banana
-- cherry

-- 문자열 <-> 배열 변환
SELECT array_to_string(ARRAY['Hello', 'World'], ' ');  -- Hello World
SELECT string_to_array('apple,banana,cherry', ',');    -- {apple,banana,cherry}
```

---

# 9.20. 범위/다중범위 함수와 연산자 (Range/Multirange Functions)

범위 타입(`int4range`, `daterange` 등)을 위한 함수들입니다.

## 범위 연산자

| 연산자 | 설명 |
|--------|------|
| `@>` | 범위가 요소/범위를 포함 |
| `<@` | 요소/범위가 범위에 포함 |
| `&&` | 범위가 겹침 |
| `<<` | 범위가 엄격히 왼쪽에 |
| `>>` | 범위가 엄격히 오른쪽에 |
| `&<` | 범위가 오른쪽으로 확장되지 않음 |
| `&>` | 범위가 왼쪽으로 확장되지 않음 |
| `-\|-` | 범위가 인접 |
| `+` | 범위 합집합 |
| `*` | 범위 교집합 |
| `-` | 범위 차집합 |

### 예제

```sql
-- 범위 생성
SELECT int4range(1, 10);                         -- [1,10)
SELECT int4range(1, 10, '[]');                   -- [1,10]
SELECT daterange('2024-01-01', '2024-12-31');    -- [2024-01-01,2024-12-31)

-- 포함 검사
SELECT int4range(1, 10) @> 5;                    -- true
SELECT 5 <@ int4range(1, 10);                    -- true

-- 겹침 검사
SELECT int4range(1, 5) && int4range(3, 8);       -- true

-- 범위 연산
SELECT int4range(1, 5) + int4range(3, 8);        -- [1,8)
SELECT int4range(1, 8) * int4range(3, 10);       -- [3,8)
```

---

# 9.21. 집계 함수 (Aggregate Functions)

집계 함수는 여러 입력 행으로부터 단일 결과를 계산합니다.

## 일반 목적 집계 함수

| 함수 | 설명 | 부분 모드 |
|------|------|----------|
| `count(*)` | 입력 행 수 계산 | Yes |
| `count(any)` | null이 아닌 입력 값 수 | Yes |
| `sum(numeric_type)` | null이 아닌 입력 값의 합 | Yes |
| `avg(numeric_type)` | null이 아닌 입력 값의 평균 | Yes |
| `min(sortable_type)` | null이 아닌 입력 값의 최소 | Yes |
| `max(sortable_type)` | null이 아닌 입력 값의 최대 | Yes |
| `array_agg(anynonarray)` | 입력 값을 배열로 수집 | Yes |
| `string_agg(text, delimiter)` | null이 아닌 값을 구분자로 연결 | Yes |
| `bool_and(boolean)` | 모든 입력이 true면 true | Yes |
| `bool_or(boolean)` | 하나라도 true면 true | Yes |
| `bit_and(integer)` | 모든 null 아닌 입력의 비트 AND | Yes |
| `bit_or(integer)` | 모든 null 아닌 입력의 비트 OR | Yes |
| `every(boolean)` | `bool_and`의 SQL 표준 동의어 | Yes |
| `any_value(anyelement)` | 임의의 null 아닌 값 반환 | Yes |

### 예제

```sql
-- 기본 집계
SELECT COUNT(*) FROM employees;                  -- 전체 행 수
SELECT COUNT(department) FROM employees;         -- NULL 아닌 부서 수
SELECT SUM(salary) FROM employees;               -- 급여 합계
SELECT AVG(salary) FROM employees;               -- 평균 급여
SELECT MIN(hire_date), MAX(hire_date) FROM employees;  -- 입사일 범위

-- 그룹별 집계
SELECT department, COUNT(*), AVG(salary)
FROM employees
GROUP BY department;

-- 배열 집계
SELECT array_agg(name ORDER BY name) FROM employees;
-- {Alice, Bob, Charlie, ...}

-- 문자열 집계
SELECT string_agg(name, ', ' ORDER BY name) FROM employees;
-- Alice, Bob, Charlie, ...

-- 불리언 집계
SELECT bool_and(active) FROM users;              -- 모두 활성화?
SELECT bool_or(is_admin) FROM users;             -- 관리자가 한 명이라도?
```

## 통계 집계 함수

| 함수 | 설명 |
|------|------|
| `stddev(numeric_type)` | 표본 표준편차 |
| `stddev_pop(numeric_type)` | 모집단 표준편차 |
| `stddev_samp(numeric_type)` | 표본 표준편차 |
| `variance(numeric_type)` | 표본 분산 |
| `var_pop(numeric_type)` | 모집단 분산 |
| `var_samp(numeric_type)` | 표본 분산 |
| `corr(Y, X)` | 상관 계수 |
| `covar_pop(Y, X)` | 모집단 공분산 |
| `covar_samp(Y, X)` | 표본 공분산 |
| `regr_slope(Y, X)` | 최소제곱선의 기울기 |
| `regr_intercept(Y, X)` | 최소제곱선의 Y 절편 |
| `regr_r2(Y, X)` | 상관 계수의 제곱 |

### 예제

```sql
-- 통계 함수
SELECT
    stddev(salary) AS salary_stddev,
    variance(salary) AS salary_variance,
    var_pop(salary) AS salary_var_pop
FROM employees;

-- 회귀 분석
SELECT
    corr(salary, experience_years) AS correlation,
    regr_slope(salary, experience_years) AS slope,
    regr_intercept(salary, experience_years) AS intercept
FROM employees;
```

## 순서-집합 집계 함수 (Ordered-Set Aggregate Functions)

| 함수 | 설명 |
|------|------|
| `mode()` | 가장 빈번한 값 |
| `percentile_cont(fraction)` | 보간을 사용한 연속 백분위수 |
| `percentile_disc(fraction)` | 이산 백분위수 |

### 예제

```sql
-- 최빈값
SELECT mode() WITHIN GROUP (ORDER BY category) FROM products;

-- 백분위수
SELECT
    percentile_cont(0.5) WITHIN GROUP (ORDER BY salary) AS median,
    percentile_cont(0.25) WITHIN GROUP (ORDER BY salary) AS q1,
    percentile_cont(0.75) WITHIN GROUP (ORDER BY salary) AS q3
FROM employees;

-- 이산 백분위수
SELECT percentile_disc(0.5) WITHIN GROUP (ORDER BY salary)
FROM employees;
```

## 중요 참고사항

- `count()`를 제외한 집계 함수는 선택된 행이 없을 때 NULL 을 반환합니다.
- `sum()`은 행이 없을 때 0이 아닌 `NULL`을 반환합니다.
- `array_agg()`는 입력 행이 없을 때 빈 배열이 아닌 `NULL`을 반환합니다.
- NULL 대신 0이나 빈 배열을 원하면 `coalesce()`를 사용하세요.

```sql
-- NULL 대신 기본값 사용
SELECT COALESCE(SUM(amount), 0) FROM orders WHERE 1=0;  -- 0
SELECT COALESCE(array_agg(name), '{}') FROM employees WHERE 1=0;  -- {}
```

---

# 9.22. 윈도우 함수 (Window Functions)

윈도우 함수는 현재 쿼리 행과 관련된 행 집합에서 계산을 수행합니다. 집계 함수와 달리 행을 단일 출력 행으로 그룹화하지 않습니다.

필수: 윈도우 함수는 반드시 `OVER` 절과 함께 호출해야 합니다.

## 순위 함수

| 함수 | 반환 타입 | 설명 |
|------|----------|------|
| `row_number()` | `bigint` | 파티션 내 현재 행 번호 (1부터 시작) |
| `rank()` | `bigint` | 갭이 있는 순위 (동순위 다음은 건너뜀) |
| `dense_rank()` | `bigint` | 갭이 없는 순위 (동료 그룹 수) |
| `percent_rank()` | `double precision` | 상대 순위: (rank - 1) / (총 행 - 1), 0~1 범위 |
| `cume_dist()` | `double precision` | 누적 분포: (현재까지 행) / (총 행), 1/N~1 범위 |
| `ntile(num_buckets integer)` | `integer` | 파티션을 버킷으로 나눔, 1~num_buckets 반환 |

### 예제

```sql
-- 순위 함수 비교
SELECT
    name,
    department,
    salary,
    row_number() OVER (ORDER BY salary DESC) AS row_num,
    rank() OVER (ORDER BY salary DESC) AS rank,
    dense_rank() OVER (ORDER BY salary DESC) AS dense_rank
FROM employees;

-- 부서별 급여 순위
SELECT
    name,
    department,
    salary,
    rank() OVER (PARTITION BY department ORDER BY salary DESC) AS dept_rank
FROM employees;

-- 상위 N개 선택 (각 부서별)
SELECT * FROM (
    SELECT
        name,
        department,
        salary,
        row_number() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
) sub
WHERE rn <= 3;

-- ntile로 등급 분류
SELECT
    name,
    salary,
    ntile(4) OVER (ORDER BY salary DESC) AS quartile
FROM employees;
```

## 오프셋 함수

| 함수 | 반환 타입 | 설명 |
|------|----------|------|
| `lag(value [, offset [, default]])` | `anycompatible` | 현재 행 이전 offset 행의 값; 없으면 default (기본 NULL) |
| `lead(value [, offset [, default]])` | `anycompatible` | 현재 행 이후 offset 행의 값; 없으면 default (기본 NULL) |

### 예제

```sql
-- 이전/다음 행 값 비교
SELECT
    date,
    sales,
    lag(sales) OVER (ORDER BY date) AS prev_sales,
    lead(sales) OVER (ORDER BY date) AS next_sales
FROM daily_sales;

-- 전일 대비 변화율
SELECT
    date,
    sales,
    sales - lag(sales) OVER (ORDER BY date) AS daily_change,
    round(100.0 * (sales - lag(sales) OVER (ORDER BY date)) /
          NULLIF(lag(sales) OVER (ORDER BY date), 0), 2) AS change_pct
FROM daily_sales;

-- 기본값 지정
SELECT
    date,
    sales,
    lag(sales, 1, 0) OVER (ORDER BY date) AS prev_sales  -- 없으면 0
FROM daily_sales;
```

## 프레임 함수

| 함수 | 반환 타입 | 설명 |
|------|----------|------|
| `first_value(value)` | `anyelement` | 윈도우 프레임의 첫 번째 행 값 |
| `last_value(value)` | `anyelement` | 윈도우 프레임의 마지막 행 값 |
| `nth_value(value, n)` | `anyelement` | 윈도우 프레임의 n번째 행 값; 없으면 NULL |

### 예제

```sql
-- 첫 번째와 마지막 값
SELECT
    name,
    department,
    salary,
    first_value(name) OVER (PARTITION BY department ORDER BY salary DESC) AS highest_paid,
    last_value(name) OVER (
        PARTITION BY department ORDER BY salary DESC
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS lowest_paid
FROM employees;

-- n번째 값
SELECT
    name,
    department,
    salary,
    nth_value(salary, 2) OVER (
        PARTITION BY department ORDER BY salary DESC
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS second_highest
FROM employees;
```

## 윈도우 함수로서의 집계 함수

모든 집계 함수를 `OVER` 절과 함께 윈도우 함수로 사용할 수 있습니다.

### 예제

```sql
-- 누적 합계 (Running Sum)
SELECT
    date,
    sales,
    SUM(sales) OVER (ORDER BY date) AS cumulative_sales
FROM daily_sales;

-- 이동 평균 (Moving Average)
SELECT
    date,
    sales,
    AVG(sales) OVER (
        ORDER BY date
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS moving_avg_7days
FROM daily_sales;

-- 전체 대비 비율
SELECT
    name,
    department,
    salary,
    round(100.0 * salary / SUM(salary) OVER (), 2) AS pct_of_total,
    round(100.0 * salary / SUM(salary) OVER (PARTITION BY department), 2) AS pct_of_dept
FROM employees;
```

## 명명된 윈도우 (Named Window)

반복을 피하기 위해 재사용 가능한 윈도우 사양을 정의합니다.

```sql
SELECT
    name,
    salary,
    SUM(salary) OVER w AS running_sum,
    AVG(salary) OVER w AS running_avg
FROM employees
WINDOW w AS (ORDER BY salary);
```

## 중요 참고사항

- 허용된 위치: `SELECT` 목록과 `ORDER BY` 절에서만 사용 가능
- 허용되지 않음: `GROUP BY`, `HAVING`, `WHERE` 절에서 사용 불가
- 실행 순서: 윈도우 함수는 비윈도우 집계 함수 이후에 실행됨
- 기본 프레임:
  - `ORDER BY`가 있는 경우: 파티션 시작부터 현재 행까지
  - `ORDER BY`가 없는 경우: 전체 파티션

---

# 9.24. 서브쿼리 표현식 (Subquery Expressions)

서브쿼리 표현식은 Boolean 결과를 반환합니다.

## EXISTS

```sql
EXISTS (subquery)
```

서브쿼리가 최소 하나의 행을 반환하면 `true`, 행이 없으면 `false`를 반환합니다.

### 예제

```sql
SELECT * FROM orders o
WHERE EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = o.id
);
```

## IN

```sql
expression IN (subquery)
```

표현식이 서브쿼리의 어떤 행과 같으면 `true`를 반환합니다.

### 예제

```sql
SELECT * FROM employees
WHERE department_id IN (
    SELECT id FROM departments WHERE location = 'Seoul'
);

-- 값 목록과 함께 사용
SELECT * FROM employees
WHERE status IN ('active', 'pending');
```

## NOT IN

```sql
expression NOT IN (subquery)
```

표현식이 서브쿼리의 모든 행과 다르면 `true`를 반환합니다.

주의: NULL 값이 있으면 예상치 못한 결과가 발생할 수 있습니다.

```sql
SELECT * FROM employees
WHERE department_id NOT IN (
    SELECT id FROM departments WHERE location = 'Busan'
);
```

## ANY / SOME

```sql
expression operator ANY (subquery)
expression operator SOME (subquery)
```

비교 연산이 서브쿼리의 어떤 행에 대해 true이면 `true`를 반환합니다.

### 예제

```sql
SELECT * FROM employees
WHERE salary > ANY (
    SELECT salary FROM employees WHERE department = 'Sales'
);

-- = ANY는 IN과 동일
SELECT * FROM employees
WHERE department = ANY (ARRAY['HR', 'IT', 'Sales']);
```

## ALL

```sql
expression operator ALL (subquery)
```

비교 연산이 서브쿼리의 모든 행에 대해 true이면 `true`를 반환합니다.

### 예제

```sql
SELECT * FROM employees
WHERE salary > ALL (
    SELECT salary FROM employees WHERE department = 'Sales'
);
-- Sales 부서 모든 직원보다 급여가 높은 직원

-- <> ALL은 NOT IN과 동일
SELECT * FROM products
WHERE category <> ALL (ARRAY['Electronics', 'Clothing']);
```

---

# 9.16. JSON 함수와 연산자 (JSON Functions and Operators)

PostgreSQL은 JSON 데이터 처리를 위한 포괄적인 기능을 제공합니다.

## JSON 생성 함수

| 함수 | 설명 |
|------|------|
| `to_json(value)` / `to_jsonb(value)` | SQL 값을 JSON으로 변환 |
| `json_build_array(...)` | 값들로 JSON 배열 생성 |
| `json_build_object(...)` | 키-값 쌍으로 JSON 객체 생성 |
| `json_object(keys, values)` | 키와 값 배열로 JSON 객체 생성 |

### 예제

```sql
SELECT to_json('Hello'::text);                   -- "Hello"
SELECT to_jsonb(ARRAY[1, 2, 3]);                -- [1, 2, 3]

SELECT json_build_array(1, 2, 'foo', true);     -- [1, 2, "foo", true]

SELECT json_build_object('name', 'John', 'age', 30);
-- {"name": "John", "age": 30}

SELECT row_to_json(row(1, 'foo'));              -- {"f1":1,"f2":"foo"}
```

## JSON 연산자

### 기본 추출 연산자

| 연산자 | 반환 타입 | 설명 | 예제 |
|--------|----------|------|------|
| `->` int | json | 배열 요소 추출 | `'[1,2,3]'::json -> 0` → `1` |
| `->` text | json | 객체 필드 추출 | `'{"a":1}'::json -> 'a'` → `1` |
| `->>` int | text | 배열 요소를 텍스트로 | `'[1,2,3]'::json ->> 0` → `'1'` |
| `->>` text | text | 객체 필드를 텍스트로 | `'{"a":"b"}'::json ->> 'a'` → `'b'` |
| `#>` text[] | json | 경로로 요소 추출 | `'{"a":{"b":1}}'::json #> '{a,b}'` → `1` |
| `#>>` text[] | text | 경로로 텍스트 추출 | `'{"a":{"b":1}}'::json #>> '{a,b}'` → `'1'` |

### 예제

```sql
-- 기본 추출
SELECT '{"name": "John", "age": 30}'::json -> 'name';      -- "John"
SELECT '{"name": "John", "age": 30}'::json ->> 'name';     -- John (텍스트)
SELECT '[1, 2, 3]'::json -> 1;                              -- 2
SELECT '[1, 2, 3]'::json ->> 1;                             -- '2'

-- 중첩 경로 추출
SELECT '{"a": {"b": {"c": 1}}}'::json #> '{a,b,c}';        -- 1
SELECT '{"users": [{"name": "John"}, {"name": "Jane"}]}'::json #>> '{users,0,name}';
-- John
```

### JSONB 전용 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `@>` | 포함 | `'{"a":1, "b":2}'::jsonb @> '{"b":2}'` → `true` |
| `<@` | 포함됨 | `'{"b":2}'::jsonb <@ '{"a":1, "b":2}'` → `true` |
| `?` | 키/요소 존재 | `'{"a":1}'::jsonb ? 'a'` → `true` |
| `?\|` | 키 중 하나 존재 | `'{"a":1}'::jsonb ?\| array['a','b']` → `true` |
| `?&` | 모든 키 존재 | `'{"a":1,"b":2}'::jsonb ?& array['a','b']` → `true` |
| `\|\|` | 연결/병합 | `'{"a":1}'::jsonb \|\| '{"b":2}'` → `{"a":1,"b":2}` |
| `-` | 키 또는 요소 삭제 | `'{"a":1,"b":2}'::jsonb - 'a'` → `{"b":2}` |
| `#-` | 경로에서 삭제 | `'{"a":{"b":1}}'::jsonb #- '{a,b}'` → `{"a":{}}` |

### 예제

```sql
-- 포함 검사
SELECT '{"name": "John", "age": 30}'::jsonb @> '{"age": 30}';  -- true

-- 키 존재 검사
SELECT '{"a": 1, "b": 2}'::jsonb ? 'a';                        -- true

-- JSON 병합
SELECT '{"a": 1}'::jsonb || '{"b": 2}'::jsonb;                 -- {"a": 1, "b": 2}
SELECT '{"a": 1}'::jsonb || '{"a": 2}'::jsonb;                 -- {"a": 2} (덮어쓰기)

-- 키 삭제
SELECT '{"a": 1, "b": 2}'::jsonb - 'a';                        -- {"b": 2}
SELECT '["a", "b", "c"]'::jsonb - 1;                           -- ["a", "c"]
```

## JSON 처리 함수

### 배열/객체 확장

| 함수 | 설명 |
|------|------|
| `json_array_elements(json)` | JSON 배열을 값 집합으로 확장 |
| `json_each(json)` | JSON 객체를 키-값 쌍으로 확장 |
| `json_object_keys(json)` | 객체 키 가져오기 |
| `json_array_length(json)` | 배열 요소 수 반환 |

### 예제

```sql
-- 배열 확장
SELECT * FROM json_array_elements('[1, 2, 3]');
-- 1
-- 2
-- 3

-- 객체 확장
SELECT * FROM json_each('{"a": 1, "b": 2}');
-- a | 1
-- b | 2

-- 객체 키
SELECT json_object_keys('{"a": 1, "b": 2, "c": 3}');
-- a
-- b
-- c

-- 배열 길이
SELECT json_array_length('[1, 2, 3, 4, 5]');  -- 5
```

### JSONB 수정 함수

| 함수 | 설명 |
|------|------|
| `jsonb_set(target, path, new_value [, create_missing])` | 경로에 값 설정 |
| `jsonb_insert(target, path, new_value [, insert_after])` | 경로에 값 삽입 |
| `jsonb_strip_nulls(jsonb)` | null 값 제거 |

### 예제

```sql
-- 값 설정
SELECT jsonb_set('{"a": 1, "b": 2}'::jsonb, '{c}', '3');
-- {"a": 1, "b": 2, "c": 3}

SELECT jsonb_set('{"a": {"b": 1}}'::jsonb, '{a,c}', '"new"');
-- {"a": {"b": 1, "c": "new"}}

-- 배열에 삽입
SELECT jsonb_insert('[1, 2, 3]'::jsonb, '{1}', '"new"');
-- [1, "new", 2, 3]

-- null 제거
SELECT jsonb_strip_nulls('{"a": 1, "b": null, "c": 3}');
-- {"a": 1, "c": 3}
```

## SQL/JSON 경로 언어

경로 표현식으로 JSON을 쿼리합니다. `$`로 시작합니다.

### 경로 쿼리 함수

| 함수 | 반환 타입 | 설명 |
|------|----------|------|
| `jsonb_path_exists(target, path)` | boolean | 경로가 매칭되는지 확인 |
| `jsonb_path_query(target, path)` | setof jsonb | 매칭되는 항목 반환 |
| `jsonb_path_query_array(target, path)` | jsonb | 매칭을 배열로 반환 |
| `jsonb_path_query_first(target, path)` | jsonb | 첫 번째 매칭 반환 |

### 예제

```sql
-- 경로 존재 확인
SELECT jsonb_path_exists('{"a": [1, 2, 3]}', '$.a[*] ? (@ > 2)');
-- true

-- 경로로 쿼리
SELECT jsonb_path_query('{"a": [1, 2, 3, 4, 5]}', '$.a[*] ? (@ > 2)');
-- 3
-- 4
-- 5

-- 변수 사용
SELECT jsonb_path_query(
    '{"a": [1, 2, 3, 4, 5]}',
    '$.a[*] ? (@ >= $min && @ <= $max)',
    '{"min": 2, "max": 4}'
);
-- 2
-- 3
-- 4

-- 배열로 반환
SELECT jsonb_path_query_array('{"a": [1, 2, 3]}', '$.a[*] ? (@ > 1)');
-- [2, 3]
```

---

# 9.27. 시스템 정보 함수 (System Information Functions)

PostgreSQL 시스템에 대한 정보를 반환하는 함수들입니다.

## 세션 정보 함수

| 함수 | 설명 |
|------|------|
| `current_catalog` / `current_database()` | 현재 데이터베이스 이름 |
| `current_schema()` | 현재 스키마 이름 |
| `current_schemas(include_implicit)` | 검색 경로의 스키마들 |
| `current_user` | 현재 실행 컨텍스트의 사용자 이름 |
| `session_user` | 세션 사용자 이름 |
| `user` | `current_user`와 동일 |
| `inet_client_addr()` | 원격 연결 주소 |
| `inet_client_port()` | 원격 연결 포트 |
| `inet_server_addr()` | 로컬 연결 주소 |
| `inet_server_port()` | 로컬 연결 포트 |
| `pg_backend_pid()` | 현재 세션의 서버 프로세스 ID |
| `version()` | PostgreSQL 버전 정보 |

### 예제

```sql
SELECT current_database();                       -- mydb
SELECT current_user;                             -- postgres
SELECT version();
-- PostgreSQL 16.1 on x86_64-pc-linux-gnu...

SELECT pg_backend_pid();                         -- 12345
SELECT inet_client_addr();                       -- 192.168.1.100
```

## 접근 권한 확인 함수

| 함수 | 설명 |
|------|------|
| `has_table_privilege(user, table, privilege)` | 테이블 권한 확인 |
| `has_schema_privilege(user, schema, privilege)` | 스키마 권한 확인 |
| `has_database_privilege(user, database, privilege)` | 데이터베이스 권한 확인 |
| `has_column_privilege(user, table, column, privilege)` | 열 권한 확인 |
| `pg_has_role(user, role, privilege)` | 역할 멤버십 확인 |

### 예제

```sql
SELECT has_table_privilege('myuser', 'employees', 'SELECT');
-- true

SELECT has_schema_privilege(current_user, 'public', 'CREATE');
-- true
```

---

# 참고 자료

- [PostgreSQL 공식 문서 - Chapter 9. Functions and Operators](https://www.postgresql.org/docs/current/functions.html)
- [PostgreSQL 공식 문서 - 논리 연산자](https://www.postgresql.org/docs/current/functions-logical.html)
- [PostgreSQL 공식 문서 - 비교 함수와 연산자](https://www.postgresql.org/docs/current/functions-comparison.html)
- [PostgreSQL 공식 문서 - 수학 함수와 연산자](https://www.postgresql.org/docs/current/functions-math.html)
- [PostgreSQL 공식 문서 - 문자열 함수와 연산자](https://www.postgresql.org/docs/current/functions-string.html)
- [PostgreSQL 공식 문서 - 날짜/시간 함수와 연산자](https://www.postgresql.org/docs/current/functions-datetime.html)
- [PostgreSQL 공식 문서 - 패턴 매칭](https://www.postgresql.org/docs/current/functions-matching.html)
- [PostgreSQL 공식 문서 - 조건 표현식](https://www.postgresql.org/docs/current/functions-conditional.html)
- [PostgreSQL 공식 문서 - 배열 함수와 연산자](https://www.postgresql.org/docs/current/functions-array.html)
- [PostgreSQL 공식 문서 - 집계 함수](https://www.postgresql.org/docs/current/functions-aggregate.html)
- [PostgreSQL 공식 문서 - 윈도우 함수](https://www.postgresql.org/docs/current/functions-window.html)
- [PostgreSQL 공식 문서 - JSON 함수와 연산자](https://www.postgresql.org/docs/current/functions-json.html)
- [PostgreSQL 공식 문서 - 서브쿼리 표현식](https://www.postgresql.org/docs/current/functions-subquery.html)
