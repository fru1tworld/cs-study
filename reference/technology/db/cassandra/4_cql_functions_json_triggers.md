# CQL 함수와 집계, JSON, 트리거, 보안

## CQL 함수와 집계

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html

---

### 목차

1. [개요](#개요)
2. [스칼라 함수(Scalar Functions)](#스칼라-함수scalar-functions)
   - [내장 함수(Native Functions)](#내장-함수native-functions)
     - [Cast](#cast)
     - [Token](#token)
     - [Uuid](#uuid)
     - [Timeuuid 함수](#timeuuid-함수)
     - [날짜/시간(Datetime) 함수](#날짜시간datetime-함수)
     - [Blob 변환 함수](#blob-변환-함수)
     - [수학(Math) 함수](#수학math-함수)
     - [컬렉션(Collection) 함수](#컬렉션collection-함수)
     - [데이터 마스킹(Data Masking) 함수](#데이터-마스킹data-masking-함수)
     - [벡터 유사도(Vector Similarity) 함수](#벡터-유사도vector-similarity-함수)
   - [사용자 정의 함수(User-Defined Functions, UDF)](#사용자-정의-함수user-defined-functions-udf)
3. [집계 함수(Aggregate Functions)](#집계-함수aggregate-functions)
   - [내장 집계(Native Aggregates)](#내장-집계native-aggregates)
   - [사용자 정의 집계(User-Defined Aggregates, UDA)](#사용자-정의-집계user-defined-aggregates-uda)

---

### 개요

CQL은 데이터를 변환하는 함수를 지원함. 함수는 크게 두 가지로 구분됨.

- 스칼라 함수(scalar functions): 입력값을 새로운 값으로 변환. 결과 집합의 각 행(row)마다 평가됨
- 집계 함수(aggregate functions): 여러 행에 걸쳐 값을 집계해 단일 결과 값을 생성

각 분류마다 Cassandra가 기본 제공하는 내장 함수(native function)와, 사용자가 직접 작성하는 사용자 정의 함수(user-defined function, UDF)·사용자 정의 집계(user-defined aggregate, UDA)가 존재함.

> 참고: 스칼라 함수와 집계 함수는 조정자(coordinator) 노드에서 평가됨. 결과 집합이 클 경우 클라이언트에 데이터를 반환하기 전에 조정자에서 계산이 수행됨에 유의.

---

### 스칼라 함수(Scalar Functions)

스칼라 함수는 결과 집합의 각 행에 대해 평가되며, 하나 이상의 값을 입력받아 단일 값을 반환함.

#### 내장 함수(Native Functions)

Cassandra는 다양한 내장 스칼라 함수를 제공함.

> 결정성(deterministic)과 단조성(monotonicity) 참고
> 일부 내장 함수는 결정적(deterministic)이지 않음 → 동일한 입력에도 항상 동일한 결과를 보장하지 않음. 대표적으로 `uuid()`, `now()`가 여기 해당. 이런 비결정적 함수를 사용자 정의 집계(UDA) 내부에서 사용하면 집계가 결정적 동작을 전제로 하므로 예측 불가능한 결과가 나올 수 있음.

---

##### Cast

`cast` 함수는 한 데이터 타입(native datatype)의 값을 다른 데이터 타입으로 변환하는 데 사용함.

예를 들어 `int` 타입의 `count` 컬럼 평균을 `double`로 계산하려면 다음과 같이 사용.

```cql
SELECT avg(cast(count as double)) FROM myTable
```

`cast` 함수가 지원하는 변환(원본 타입 → 변환 가능한 대상 타입)은 다음과 같음.

- `ascii` → `text`, `varchar`
- `bigint` → `tinyint`, `smallint`, `int`, `float`, `double`, `decimal`, `varint`, `text`, `varchar`
- `boolean` → `text`, `varchar`
- `counter` → `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar`
- `date` → `timestamp`
- `decimal` → `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `varint`, `text`, `varchar`
- `double` → `tinyint`, `smallint`, `int`, `bigint`, `float`, `decimal`, `varint`, `text`, `varchar`
- `float` → `tinyint`, `smallint`, `int`, `bigint`, `double`, `decimal`, `varint`, `text`, `varchar`
- `inet` → `text`, `varchar`
- `int` → `tinyint`, `smallint`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar`
- `smallint` → `tinyint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar`
- `time` → `text`, `varchar`
- `timestamp` → `date`, `text`, `varchar`
- `timeuuid` → `timestamp`, `date`, `text`, `varchar`
- `tinyint` → `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar`
- `uuid` → `text`, `varchar`
- `varint` → `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `text`, `varchar`

`cast` 함수는 자기 자신과 동일한 타입으로의 변환(예: `int` → `int`)도 허용하나, 이는 아무 효과가 없는 동일 변환(no-op)임.

---

##### Token

`token` 함수는 특정 파티션 키(partition key)에 대한 토큰(token) 값을 계산함. 토큰은 클러스터 내에서 데이터가 어떻게 분산되는지를 결정하는 값임.

`token` 함수가 반환하는 정확한 타입은 클러스터에 설정된 파티셔너(partitioner)에 따라 달라짐.

- `Murmur3Partitioner`: 반환 타입 `bigint`
- `RandomPartitioner`: 반환 타입 `varint`
- `ByteOrderedPartitioner`: 반환 타입 `blob`

예를 들어 다음과 같은 테이블이 있다고 가정.

```cql
CREATE TABLE users (
    userid text PRIMARY KEY,
    username text,
);
```

이 경우 `userid` 컬럼에 대해 다음과 같이 토큰을 계산 가능.

```cql
SELECT token(userid) FROM users;
```

`token` 함수는 데이터가 클러스터 전반에 어떻게 분산되는지 이해하거나, 파티션 키를 기준으로 결과를 페이징(paging)할 때 유용.

---

##### Uuid

`uuid` 함수는 인자를 받지 않으며, 호출될 때마다 랜덤한 타입 4(version 4) UUID를 생성함.

```cql
INSERT INTO users (id, name) VALUES (uuid(), 'Alice');
```

`uuid()`는 결과 집합의 각 행마다, 그리고 호출될 때마다 새로운 무작위 값을 반환함.

---

##### Timeuuid 함수

###### now

`now` 함수는 인자를 받지 않으며, 호출 시점의 노드 시간을 기반으로 새로운 타입 1(version 1) timeuuid를 생성함. 단, 단일 구문(statement) 내의 모든 호출은 동일한 값을 생성함에 유의.

timeuuid 값을 기준으로 데이터를 삽입할 때 유용.

```cql
INSERT INTO myTable (t) VALUES (now());
```

###### min_timeuuid 와 max_timeuuid

`min_timeuuid`(예전 명칭 `minTimeuuid`)와 `max_timeuuid`(예전 명칭 `maxTimeuuid`) 함수는 `timestamp` 또는 `timestamp`로 변환 가능한 `date` 문자열을 입력받아, 해당 타임스탬프에 대응하는 가짜(fake) timeuuid를 반환함.

- `min_timeuuid`: 해당 타임스탬프에 대해 가능한 가장 작은(가장 이른) timeuuid를 반환
- `max_timeuuid`: 해당 타임스탬프에 대해 가능한 가장 큰(가장 늦은) timeuuid를 반환

이 두 함수는 `timeuuid` 컬럼에 대해 특정 타임스탬프 범위를 질의할 때 유용. 예를 들어 다음 쿼리는 `t` 컬럼이 `'2013-01-01 00:05+0000'`(포함) 이상이고 `'2013-02-02 10:00+0000'`(포함) 이하인 모든 행을 조회함.

```cql
SELECT * FROM myTable
WHERE t > min_timeuuid('2013-01-01 00:05+0000')
  AND t < max_timeuuid('2013-02-02 10:00+0000')
```

> 주의: `min_timeuuid`와 `max_timeuuid`가 반환하는 값은 실제 timeuuid가 아니라, 해당 타임스탬프 경계를 표현하기 위한 가짜 값임. 따라서 이 값들을 다른 곳에 삽입하거나 일반 timeuuid처럼 사용 금지, 오직 위와 같은 범위 비교 용도로만 사용. 또한 위 쿼리에서 부등호(`>`, `<`)와 등호를 포함하는 부등호(`>=`, `<=`)의 차이에 주의 필요.

---

##### 날짜/시간(Datetime) 함수

###### 현재 값을 조회하는 함수

다음 함수들은 인자 없이 호출되며, 조정자 노드의 현재 날짜/시간을 다양한 타입으로 반환함.

- `current_timestamp()`: 반환 타입 `timestamp` — 현재 타임스탬프(밀리초 정밀도)를 반환
- `current_date()`: 반환 타입 `date` — 현재 날짜를 반환
- `current_time()`: 반환 타입 `time` — 현재 시각(자정 이후 경과 시간)을 반환
- `current_timeuuid()`: 반환 타입 `timeuuid` — 현재 시점의 timeuuid를 반환(`now()`와 동등)

예를 들어 현재 날짜 이후의 데이터를 조회하려면 다음과 같이 사용.

```cql
SELECT * FROM myTable WHERE date >= current_date();
```

###### 시간/날짜 타입 간 변환 함수

다음 함수들은 `timeuuid`, `timestamp`, `date` 타입 간의 변환을 수행함.

- `to_date(timeuuid)`: 입력 `timeuuid` → 반환 `date`
- `to_date(timestamp)`: 입력 `timestamp` → 반환 `date`
- `to_timestamp(timeuuid)`: 입력 `timeuuid` → 반환 `timestamp`
- `to_timestamp(date)`: 입력 `date` → 반환 `timestamp`
- `to_unix_timestamp(timeuuid)`: 입력 `timeuuid` → 반환 `bigint`
- `to_unix_timestamp(timestamp)`: 입력 `timestamp` → 반환 `bigint`
- `to_unix_timestamp(date)`: 입력 `date` → 반환 `bigint`

- `to_date` 함수: `timeuuid` 또는 `timestamp`를 `date` 값으로 변환
- `to_timestamp` 함수: `timeuuid` 또는 `date`를 `timestamp` 값으로 변환
- `to_unix_timestamp` 함수: `timeuuid`, `timestamp`, `date`를 유닉스(Unix) 타임스탬프(에포크 기준 밀리초 단위의 `bigint`)로 변환

> 예전 버전의 함수명(`toDate`, `toTimestamp`, `toUnixTimestamp` 등 카멜 케이스)은 하위 호환을 위해 여전히 사용 가능.

예를 들어 `timeuuid` 컬럼 `t`의 값을 사람이 읽기 쉬운 타임스탬프로 변환하려면 다음과 같이 사용.

```cql
SELECT to_timestamp(t) FROM myTable;
```

---

##### Blob 변환 함수

CQL은 임의의 native 타입과 `blob` 사이를 변환하는 다수의 함수를 제공함. 함수 이름은 다음 규칙을 따름.

- `type_as_blob(value)`: `type` 타입의 값을 받아 `blob`으로 변환
- `blob_as_type(value)`: `blob`을 받아 `type` 타입의 값으로 변환

즉, 모든 native 타입 `type`에 대해 `type_as_blob`이라는 함수와 `blob_as_type`이라는 함수가 정의됨(`blob` 자신은 제외).

예를 들어 `bigint`의 경우 다음과 같음.

```cql
// bigint 값 3을 blob으로 변환
bigint_as_blob(3)        // 결과: 0x0000000000000003

// blob을 다시 bigint로 변환
blob_as_bigint(0x0000000000000003)   // 결과: 3
```

> 예전 버전의 함수명(`bigintAsBlob`, `blobAsBigint` 등 카멜 케이스)도 하위 호환을 위해 사용 가능.

---

##### 수학(Math) 함수

CQL은 다음과 같은 수학 함수를 제공함.

- `abs`: 입력값의 절대값(absolute value)을 반환
- `exp`: 입력값의 지수값(자연상수 e의 거듭제곱, exponential)을 반환
- `log`: 입력값의 자연로그(natural logarithm)를 반환
- `log10`: 입력값의 밑이 10인 로그(base-10 logarithm)를 반환
- `round`: 입력값을 가장 가까운 정수로 반올림. 반올림 모드는 `HALF_UP`(반올림)을 사용

> 이 함수들의 반환 타입은 항상 입력 타입과 동일함.

예시:

```cql
SELECT abs(temperature), round(score) FROM measurements;
```

---

##### 컬렉션(Collection) 함수

다음 함수들은 `map`, `set`, `list` 타입의 컬렉션 컬럼에 대해 동작함.

- `map_keys(map)`: 맵의 키(key)들로 이루어진 `set`을 반환
- `map_values(map)`: 맵의 값(value)들로 이루어진 `list`를 반환
- `collection_count(collection)`: 컬렉션에 포함된 원소의 개수를 반환
- `collection_min(collection)`: `set` 또는 `list`에서 가장 작은 원소를 반환
- `collection_max(collection)`: `set` 또는 `list`에서 가장 큰 원소를 반환
- `collection_sum(collection)`: 숫자 컬렉션의 원소 합을 반환
- `collection_avg(collection)`: 숫자 컬렉션의 원소 평균을 반환

> `collection_sum`과 `collection_avg`는 숫자 타입 컬렉션에 대해서만 동작함. 결과는 입력 타입과 동일한 타입으로 반환되므로, 합계가 해당 타입의 최대값을 초과하면 오버플로(overflow) 발생 가능.

예시:

```cql
SELECT collection_count(tags), collection_max(scores) FROM myTable;
```

---

##### 데이터 마스킹(Data Masking) 함수

데이터 마스킹 함수는 민감한 데이터를 가려서 노출하는 데 사용함. 컬럼 정의에 함께 적용해 동적 데이터 마스킹(dynamic data masking)에 활용 가능.

- `mask_null(value)`: 입력값에 관계없이 항상 `null`을 반환
- `mask_default(value)`: 입력값을 타입별 고정 기본값으로 대체(예: 텍스트는 `****`)
- `mask_replace(value, replacement)`: 입력값을 지정한 대체값(`replacement`)으로 치환
- `mask_inner(value, begin, end[, padding])`: 앞쪽 `begin`개와 뒤쪽 `end`개 문자를 제외한 가운데 문자들을 마스킹 문자로 가림
  - `padding`(선택)으로 마스킹 문자 지정 가능
- `mask_outer(value, begin, end[, padding])`: 앞쪽 `begin`개와 뒤쪽 `end`개의 바깥쪽 문자들을 마스킹하고 가운데는 그대로 둠
- `mask_hash(value[, algorithm])`: 입력값을 해시(hash)하여 그 결과를 `blob`으로 반환
  - `algorithm`(선택)으로 해시 알고리즘 지정 가능

예시:

```cql
-- 신용카드 번호 컬럼에서 마지막 4자리만 노출
SELECT mask_inner(credit_card, 0, 4) FROM payments;

-- 이메일을 항상 ****로 노출
SELECT mask_default(email) FROM users;
```

---

##### 벡터 유사도(Vector Similarity) 함수

벡터 유사도 함수는 두 벡터 사이의 관계(유사도/거리)를 계산함. 주로 벡터 검색(vector search) 및 유사도 기반 질의에 사용.

- `similarity_cosine(vector, vector)`: 두 벡터의 코사인 유사도(cosine similarity)를 반환
- `similarity_euclidean(vector, vector)`: 두 벡터 사이의 유클리드 거리(Euclidean distance) 기반 유사도를 반환
- `similarity_dot_product(vector, vector)`: 두 벡터의 내적(dot product)을 반환

> 이 함수들은 차원(dimension)이 동일한 `float` 벡터에 대해 동작함.

예시:

```cql
SELECT similarity_cosine(embedding, [0.1, 0.2, 0.3]) FROM documents;
```

---

#### 사용자 정의 함수(User-Defined Functions, UDF)

사용자 정의 함수(UDF)를 사용하면 Cassandra 내부에서 사용자가 작성한 코드를 실행 가능. 기본적으로 Cassandra는 Java로 작성된 함수를 지원함.

UDF는 단일 노드, 즉 조정자 노드에서 실행됨. 따라서 잘못 작성된 UDF는 노드 성능에 영향을 줄 수 있어 주의 필요.

##### CREATE FUNCTION

UDF는 `CREATE FUNCTION` 구문으로 생성함. 문법은 다음과 같음.

```
create_function_statement::= CREATE [ OR REPLACE ] FUNCTION [ IF NOT EXISTS ]
    function_name '(' arguments_declaration ')'
    [ CALLED | RETURNS NULL ] ON NULL INPUT
    RETURNS cql_type
    LANGUAGE identifier
    AS string

arguments_declaration: identifier cql_type ( ',' identifier cql_type )*
```

주요 구성 요소는 다음과 같음.

- `OR REPLACE`: 같은 이름과 시그니처(signature)의 함수가 이미 존재하면 덮어씀. `IF NOT EXISTS`와 함께 사용 불가
- `IF NOT EXISTS`: 같은 함수가 이미 존재하면 아무 작업도 하지 않음
- null 입력 처리: 다음 두 가지 중 하나를 반드시 지정
  - `RETURNS NULL ON NULL INPUT`: 인자 중 하나라도 `null`이면 함수 본문을 실행하지 않고 즉시 `null`을 반환
  - `CALLED ON NULL INPUT`: 인자에 `null`이 포함되어 있어도 함수 본문이 호출됨(함수 내부에서 `null`을 직접 처리해야 함)
- `RETURNS cql_type`: 함수의 반환 타입을 지정
- `LANGUAGE`: 함수 본문이 작성된 언어를 지정(예: `java`)
- `AS string`: 함수 본문 코드. 일반적으로 `$$ ... $$` 구분자로 감쌈

###### 예시

```cql
CREATE OR REPLACE FUNCTION somefunction(somearg int, anotherarg text, complexarg frozen<someUDT>, listarg list)
    RETURNS NULL ON NULL INPUT
    RETURNS text
    LANGUAGE java
    AS $$
        // some Java code
    $$;

CREATE FUNCTION IF NOT EXISTS akeyspace.fname(someArg int)
    CALLED ON NULL INPUT
    RETURNS text
    LANGUAGE java
    AS $$
        // some Java code
    $$;
```

가장 단순한 형태의 UDF 예시.

```cql
CREATE FUNCTION some_function ( arg int )
    RETURNS NULL ON NULL INPUT
    RETURNS int
    LANGUAGE java
    AS $$ return arg; $$;
```

사용자 정의 타입(UDT)을 활용하는 예시. UDF 내부에서는 `udfContext` 객체를 통해 UDT나 튜플(tuple) 값을 생성 가능.

```cql
CREATE TYPE custom_type (txt text, i int);

CREATE FUNCTION fct_using_udt ( somearg int )
    RETURNS NULL ON NULL INPUT
    RETURNS custom_type
    LANGUAGE java
    AS $$
        UDTValue udt = udfContext.newReturnUDTValue();
        udt.setString("txt", "some string");
        udt.setInt("i", 42);
        return udt;
    $$;
```

`udfContext` 인스턴스는 UDT 값(`UDTValue`)이나 튜플 값(`TupleValue`)을 생성할 때 사용 가능한 다음 메서드를 제공함.

- `UDTValue newArgUDTValue(String argName)`
- `UDTValue newArgUDTValue(int argNum)`
- `UDTValue newReturnUDTValue()`
- `UDTValue newUDTValue(String udtName)`
- `TupleValue newArgTupleValue(String argName)`
- `TupleValue newArgTupleValue(int argNum)`
- `TupleValue newReturnTupleValue()`
- `TupleValue newTupleValue(String cqlDefinition)`

##### DROP FUNCTION

함수는 `DROP FUNCTION` 구문으로 삭제함. 문법은 다음과 같음.

```
drop_function_statement::= DROP FUNCTION [ IF EXISTS ] function_name [ '(' arguments_signature ')' ]

arguments_signature::= cql_type ( ',' cql_type )*
```

###### 예시

```cql
DROP FUNCTION myfunction;
DROP FUNCTION mykeyspace.afunction;
DROP FUNCTION afunction ( int );
DROP FUNCTION afunction ( text );
```

> 동일한 이름의 함수가 여러 개 오버로드(overload)되어 있는 경우, 인자 시그니처(`( int )`, `( text )` 등)를 명시해 삭제할 함수를 특정해야 함.

---

### 집계 함수(Aggregate Functions)

집계 함수는 `SELECT` 구문이 선택한 모든 행에 대해 동작해, 단일 결과 값을 생성함.

> 참고: 일반 컬럼, 스칼라 함수, UDT 필드, 쓰기 시각(writetime), TTL이 집계 함수와 함께 선택되면, 결과 집합에서 처음으로 반환되는 행의 값만 반환됨.

#### 내장 집계(Native Aggregates)

Cassandra는 다음과 같은 내장 집계 함수를 제공함.

##### Count

`count` 함수는 `SELECT` 구문이 반환하는 행(또는 비-`null` 값)의 개수를 셈.

```cql
SELECT COUNT(*) FROM plays;
```

```cql
SELECT COUNT(1) FROM plays;
```

특정 컬럼을 대상으로 `COUNT(column)`을 사용하면 해당 컬럼이 `null`이 아닌 행의 개수만 셈.

```cql
SELECT COUNT(scores) FROM plays;
```

##### Min 과 Max

`min`과 `max` 함수는 주어진 컬럼에 대해 각각 최소값과 최대값을 계산함.

```cql
SELECT MIN(players), MAX(players) FROM plays;
```

##### Sum

`sum` 함수는 주어진 컬럼이 반환하는 모든 값의 합을 계산함.

```cql
SELECT SUM(players) FROM plays;
```

> 합계가 컬럼 타입의 최대값을 초과하면 오버플로(overflow) 발생 가능하므로 주의 필요.

##### Avg

`avg` 함수는 주어진 컬럼이 반환하는 모든 값의 평균을 계산함.

```cql
SELECT AVG(players) FROM plays;
```

> 빈 결과 집합에 대해서는 `0`을 반환함. 또한 정수 타입에 대해서는 평균 계산 시 반올림 또는 절삭(truncation) 발생 가능(앞서 다룬 `cast`를 사용해 `double`로 변환한 뒤 평균을 구하면 소수점까지 정확한 평균을 얻을 수 있음).

---

#### 사용자 정의 집계(User-Defined Aggregates, UDA)

사용자 정의 집계(UDA)를 사용하면 사용자 정의 함수(UDF)를 조합해 커스텀 집계 로직을 구현 가능. 가중 평균, 커스텀 통계, 도메인 특화 계산 등 내장 집계로 표현하기 어려운 집계를 만들 수 있음.

UDA는 다음 요소로 구성됨.

- 상태 함수(state function, `SFUNC`): 각 행을 처리하며 누적 상태(state)를 갱신하는 UDF. 첫 번째 인자로 현재 상태를 받고, 나머지 인자로 집계 대상 값을 받아 새로운 상태를 반환
- 상태 타입(state type, `STYPE`): 누적 상태의 데이터 타입
- 최종 함수(final function, `FINALFUNC`, 선택): 모든 행을 처리한 뒤 최종 상태를 받아 최종 결과로 변환하는 UDF. 생략하면 최종 상태가 그대로 결과가 됨
- 초기 조건(initial condition, `INITCOND`, 선택): 상태의 초기값. 생략하면 `null`이 사용됨

##### CREATE AGGREGATE

UDA는 `CREATE AGGREGATE` 구문으로 생성함. 문법은 다음과 같음.

```
create_aggregate_statement ::= CREATE [ OR REPLACE ] AGGREGATE [ IF NOT EXISTS ]
    function_name '(' arguments_signature ')'
    SFUNC function_name
    STYPE cql_type
    [ FINALFUNC function_name ]
    [ INITCOND term ]
```

###### 예시: 평균(average) 집계 구현

다음은 정수 컬럼의 평균을 직접 구현한 UDA 예시. 상태로 `tuple<int, bigint>`(개수, 합계)를 사용.

먼저 상태 함수를 정의함. 각 행마다 개수를 1 증가시키고 합계에 값을 더함.

```cql
CREATE OR REPLACE FUNCTION test.averageState(state tuple<int,bigint>, val int)
    CALLED ON NULL INPUT
    RETURNS tuple
    LANGUAGE java
    AS $$
        if (val != null) {
            state.setInt(0, state.getInt(0)+1);
            state.setLong(1, state.getLong(1)+val.intValue());
        }
        return state;
    $$;
```

다음으로 최종 함수를 정의함. 누적된 합계를 개수로 나누어 평균(`double`)을 반환.

```cql
CREATE OR REPLACE FUNCTION test.averageFinal (state tuple<int,bigint>)
    CALLED ON NULL INPUT
    RETURNS double
    LANGUAGE java
    AS $$
        double r = 0;
        if (state.getInt(0) == 0) return null;
        r = state.getLong(1);
        r /= state.getInt(0);
        return Double.valueOf(r);
    $$;
```

이제 두 함수를 조합해 집계를 생성함. 초기 조건(`INITCOND`)은 `(0, 0)`(개수 0, 합계 0)으로 설정.

```cql
CREATE OR REPLACE AGGREGATE test.average(int)
    SFUNC averageState
    STYPE tuple
    FINALFUNC averageFinal
    INITCOND (0, 0);
```

생성한 집계를 테스트하기 위해 테이블을 만들고 데이터를 삽입한 뒤 집계를 호출함.

```cql
CREATE TABLE test.atable (
    pk int PRIMARY KEY,
    val int
);

INSERT INTO test.atable (pk, val) VALUES (1,1);
INSERT INTO test.atable (pk, val) VALUES (2,2);
INSERT INTO test.atable (pk, val) VALUES (3,3);
INSERT INTO test.atable (pk, val) VALUES (4,4);

SELECT test.average(val) FROM atable;
```

위 쿼리는 `val` 컬럼 값 `1, 2, 3, 4`의 평균인 `2.5`를 반환함.

> 주의: UDA가 참조하는 UDF에서 `now()`나 `uuid()`처럼 비결정적(non-deterministic) 연산을 사용하면, 동일한 입력 데이터에 대해서도 실행할 때마다 결과가 달라질 수 있어 예측 불가능한 결과를 초래함. 집계는 결정적 동작을 전제로 하므로 이러한 함수의 사용은 지양.

##### DROP AGGREGATE

집계는 `DROP AGGREGATE` 구문으로 삭제함. 문법은 다음과 같음.

```
drop_aggregate_statement::= DROP AGGREGATE [ IF EXISTS ] function_name [ '(' arguments_signature ')' ]
```

###### 예시

```cql
DROP AGGREGATE myAggregate;
DROP AGGREGATE myKeyspace.anAggregate;
DROP AGGREGATE someAggregate ( int );
DROP AGGREGATE someAggregate ( text );
```

> UDF와 마찬가지로, 동일한 이름의 집계가 여러 개 오버로드되어 있는 경우 인자 시그니처를 명시해 삭제할 집계를 특정해야 함.

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL Functions](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html)

---

## CQL JSON, 트리거, 보안

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/

---

### 목차

1. [JSON 지원(JSON Support)](#json-지원json-support)
   - [SELECT JSON](#select-json)
   - [INSERT JSON](#insert-json)
   - [Cassandra 데이터 타입의 JSON 인코딩](#cassandra-데이터-타입의-json-인코딩)
   - [fromJson() 함수](#fromjson-함수)
   - [toJson() 함수](#tojson-함수)
2. [트리거(Triggers)](#트리거triggers)
   - [CREATE TRIGGER](#create-trigger)
   - [DROP TRIGGER](#drop-trigger)
3. [보안(Security)](#보안security)
   - [데이터베이스 역할(Database Roles)](#데이터베이스-역할database-roles)
     - [CREATE ROLE](#create-role)
     - [ALTER ROLE](#alter-role)
     - [DROP ROLE](#drop-role)
     - [GRANT ROLE](#grant-role)
     - [REVOKE ROLE](#revoke-role)
     - [LIST ROLES](#list-roles)
   - [사용자(Users)](#사용자users)
     - [CREATE USER](#create-user)
     - [ALTER USER](#alter-user)
     - [DROP USER](#drop-user)
     - [LIST USERS](#list-users)
   - [데이터 제어(Data Control)](#데이터-제어data-control)
     - [권한(Permissions)](#권한permissions)
     - [GRANT PERMISSION](#grant-permission)
     - [REVOKE PERMISSION](#revoke-permission)
     - [LIST PERMISSIONS](#list-permissions)

---

### JSON 지원(JSON Support)

Cassandra 2.2부터 `SELECT` 및 `INSERT` 문에 JSON 지원이 추가됨. CQL API를 근본적으로 변경하지는 않으며(스키마는 여전히 강제됨), JSON 문서를 편리하게 다룰 수 있는 수단을 제공함.

---

#### SELECT JSON

`SELECT` 문에서 `JSON` 키워드를 사용하면 각 행(row)을 JSON 인코딩된 맵(map) 하나로 반환함. 결과 맵의 키(key)는 일반 결과 집합(result set)의 컬럼명(column name)과 동일함. 예를 들어 `SELECT JSON a, ttl(b) FROM ...` 쿼리는 `"a"`와 `"ttl(b)"`를 키로 가지는 맵을 반환함.

대문자를 포함하는 대소문자 구분(case-sensitive) 컬럼명은 큰따옴표(double quote)로 둘러싸임. 이는 `INSERT JSON` 동작과의 대칭성(symmetry)을 위함. 예를 들어 `SELECT JSON myColumn FROM ...` 는 이스케이프된 맵 키 `"\"myColumn\""`를 결과로 만듦.

맵의 값(value)은 결과 집합 값의 JSON 인코딩 표현임.

---

#### INSERT JSON

`JSON` 키워드를 사용하면 JSON 인코딩된 맵을 하나의 행으로 삽입 가능. JSON 맵의 형식은 일반적으로 동일한 테이블에 대해 `SELECT JSON`이 반환하는 형식과 일치해야 하며, 대소문자 구분 컬럼명은 큰따옴표로 둘러싸여야 함.

예시:

```
INSERT INTO mytable JSON '{ "\"myKey\"": 0, "value": 0}';
```

기본적으로 생략된(omitted) 컬럼은 `NULL`로 설정되며, 기존 값을 제거하고 툼스톤(tombstone)을 생성함. `DEFAULT UNSET`을 사용하면 생략된 컬럼의 기존 값이 보존(preserve)됨.

---

#### Cassandra 데이터 타입의 JSON 인코딩

Cassandra는 가능한 경우 데이터 타입을 네이티브(native) JSON 표현으로 인코딩하고 받아들임. 단일 필드(single-field) 타입의 경우 CQL 리터럴(literal) 형식과 일치하는 문자열 표현도 허용함. 컬렉션(collection), 튜플(tuple), 사용자 정의 타입(user-defined type) 같은 복합 타입(compound type)은 네이티브 JSON 컬렉션(맵 또는 리스트)이나 JSON 인코딩된 문자열 표현을 사용해야 함.

타입별 허용 입력 형식·반환 형식·비고는 다음과 같음.

- `ascii`: 허용 입력 string · 반환 형식 string · JSON의 `\u` 문자 이스케이프를 사용
- `bigint`: 허용 입력 integer, string · 반환 형식 integer · 문자열은 유효한 64비트 정수여야 함
- `blob`: 허용 입력 string · 반환 형식 string · 문자열은 `0x` 다음에 짝수 개의 16진수(hex) 자리로 구성되어야 함
- `boolean`: 허용 입력 boolean, string · 반환 형식 boolean · 문자열은 `"true"` 또는 `"false"`여야 함
- `date`: 허용 입력 string · 반환 형식 string · 형식은 `YYYY-MM-DD`, 시간대는 UTC
- `decimal`: 허용 입력 integer, float, string · 반환 형식 float · 클라이언트 디코더(decoder)에서 IEEE-754 정밀도를 초과할 수 있음
- `double`: 허용 입력 integer, float, string · 반환 형식 float · 문자열은 유효한 정수 또는 부동소수점(float)이어야 함
- `float`: 허용 입력 integer, float, string · 반환 형식 float · 문자열은 유효한 정수 또는 부동소수점이어야 함
- `inet`: 허용 입력 string · 반환 형식 string · IPv4 또는 IPv6 주소
- `int`: 허용 입력 integer, string · 반환 형식 integer · 문자열은 유효한 32비트 정수여야 함
- `list`: 허용 입력 list, string · 반환 형식 list · JSON의 네이티브 리스트 표현을 사용
- `map`: 허용 입력 map, string · 반환 형식 map · JSON의 네이티브 맵 표현을 사용
- `smallint`: 허용 입력 integer, string · 반환 형식 integer · 문자열은 유효한 16비트 정수여야 함
- `set`: 허용 입력 list, string · 반환 형식 list · JSON의 네이티브 리스트 표현을 사용
- `text`: 허용 입력 string · 반환 형식 string · JSON의 `\u` 문자 이스케이프를 사용
- `time`: 허용 입력 string · 반환 형식 string · 형식은 `HH-MM-SS[.fffffffff]`
- `timestamp`: 허용 입력 integer, string · 반환 형식 string · 문자열은 날짜 형태의 타임스탬프를 허용, 형식은 `YYYY-MM-DD HH:MM:SS.SSS`
- `timeuuid`: 허용 입력 string · 반환 형식 string · 상수(constant) 형식에 따른 Type 1 UUID
- `tinyint`: 허용 입력 integer, string · 반환 형식 integer · 문자열은 유효한 8비트 정수여야 함
- `tuple`: 허용 입력 list, string · 반환 형식 list · JSON의 네이티브 리스트 표현을 사용
- `UDT`: 허용 입력 map, string · 반환 형식 map · 필드명을 키로 사용하는 네이티브 맵 표현
- `uuid`: 허용 입력 string · 반환 형식 string · 상수 형식에 따름
- `varchar`: 허용 입력 string · 반환 형식 string · JSON의 `\u` 문자 이스케이프를 사용
- `varint`: 허용 입력 integer, string · 반환 형식 integer · 가변 길이(variable length), 클라이언트 디코더에서 32/64비트를 초과(overflow)할 수 있음

---

#### fromJson() 함수

`fromJson()` 함수는 `INSERT JSON`과 유사하게 동작하나, 단일 컬럼 값(single column value)에만 적용됨. `INSERT` 문의 `VALUES` 절(clause), 또는 `UPDATE`·`DELETE`·`SELECT` 문의 컬럼 값 위치에서 사용 가능. `SELECT` 문의 셀렉션 절(selection clause)에서는 사용 불가.

---

#### toJson() 함수

`toJson()` 함수는 `SELECT JSON`과 유사하게 동작하나, 개별 컬럼 값(individual column value)에만 적용됨. `SELECT` 문의 셀렉션 절(selection clause)에서만 사용 가능.

---

### 트리거(Triggers)

트리거(trigger)는 표준 식별자(identifier) 구문을 따르는 이름으로 식별됨. 트리거 로직은 Java를 포함한 모든 JVM 언어로 작성 가능하며, 데이터베이스 외부에 위치함.

트리거 코드는 Cassandra 설치 디렉터리의 `lib/triggers` 하위 디렉터리에 배치함. 클러스터(cluster) 시작 시점에 로드되며, 클러스터에 참여하는 모든 노드(node)에 존재해야 함.

테이블에 정의된 트리거는 요청된 DML 문이 실행되기 전(before)에 실행됨 → 트랜잭션(transaction)의 원자성(atomicity)을 보장함.

---

#### CREATE TRIGGER

트리거 생성 구문은 다음과 같음.

```
create_trigger_statement ::= CREATE TRIGGER [ IF NOT EXISTS ] trigger_name
	ON table_name
	USING string
```

예시:

```
CREATE TRIGGER myTrigger ON myTable USING 'org.apache.cassandra.triggers.InvertedIndex';
```

---

#### DROP TRIGGER

트리거 삭제 구문은 다음과 같음.

```
drop_trigger_statement ::= DROP TRIGGER [ IF EXISTS ] trigger_name
	ON table_name
```

예시:

```
DROP TRIGGER myTrigger ON myTable;
```

---

### 보안(Security)

#### 데이터베이스 역할(Database Roles)

CQL은 데이터베이스 역할(database role)을 사용해 개별 사용자(user)와 사용자 그룹(group)을 모두 표현함. 역할 이름의 구문은 다음과 같이 정의됨.

```
role_name ::= identifier | string
```

---

##### CREATE ROLE

`CREATE ROLE` 문은 새로운 역할을 생성함.

```
create_role_statement ::= CREATE ROLE [ IF NOT EXISTS ] role_name
                          [ WITH role_options ]
role_options ::= role_option ( AND role_option )*
role_option ::= PASSWORD '=' string
                | LOGIN '=' boolean
                | SUPERUSER '=' boolean
                | OPTIONS '=' map_literal
                | ACCESS TO DATACENTERS set_literal
                | ACCESS TO ALL DATACENTERS
```

예시:

```
CREATE ROLE new_role;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;
CREATE ROLE bob WITH PASSWORD = 'password_b' AND LOGIN = true AND SUPERUSER = true;
CREATE ROLE carlos WITH OPTIONS = { 'custom_option1' : 'option1_value', 'custom_option2' : 99 };
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND ACCESS TO DATACENTERS {'DC1', 'DC3'};
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND ACCESS TO ALL DATACENTERS;
```

기본적으로 역할은 `LOGIN` 권한이나 `SUPERUSER` 상태를 가지지 않음.

데이터베이스 리소스(resource)에 대한 권한(permission)은 역할에 부여됨. 리소스 타입에는 키스페이스(keyspace), 테이블(table), 함수(function), 그리고 역할 자체가 포함됨. 역할은 다른 역할에 할당되어 계층적(hierarchical) 권한 구조를 형성 가능. 이 계층 구조에서 권한과 `SUPERUSER` 상태는 상속(inherit)되나, `LOGIN` 권한은 상속되지 않음.

역할이 `LOGIN` 권한을 가지면 클라이언트는 접속 시 해당 역할로 인증(identify)될 수 있음. 접속이 유지되는 동안 클라이언트는 해당 역할에 할당된 모든 역할과 권한을 획득함.

데이터베이스 역할 리소스에 대한 `CREATE` 권한을 가진 클라이언트만 `CREATE ROLE` 요청을 실행 가능(단, `SUPERUSER`인 경우는 예외). Cassandra의 역할 관리(role management)는 플러그형(pluggable)이며, 커스텀 구현체는 위에 나열된 옵션의 일부만 지원할 수 있음.

역할명에 영숫자(alphanumeric)가 아닌 문자가 포함되는 경우 따옴표로 묶어야 함.

###### 내부 인증을 위한 자격 증명 설정(Setting credentials for internal authentication)

내부 인증(internal authentication)을 위한 비밀번호를 설정하려면 `WITH PASSWORD` 절을 사용하고, 비밀번호를 작은따옴표로 감싸야 함.

내부 인증이 구성되지 않았거나 역할이 `LOGIN` 권한을 가지지 않는 경우 `WITH PASSWORD` 절은 불필요.

###### 특정 데이터센터로의 접속 제한(Restricting connections to specific datacenters)

`network_authorizer`가 구성된 경우, `ACCESS TO DATACENTERS` 절에 접근 가능한 데이터센터의 집합 리터럴(set literal)을 지정해 로그인 역할(login role)을 특정 데이터센터로 제한 가능. 데이터센터를 생략하면 묵시적으로 모든 데이터센터에 대한 접근이 허용됨. 명확성을 위해 `ACCESS TO ALL DATACENTERS` 절을 사용할 수 있으나, 기능적인 차이는 없음.

###### 조건부 역할 생성(Creating a role conditionally)

이미 존재하는 역할을 생성하려고 하면 유효하지 않은 쿼리(invalid query) 오류가 발생함. `IF NOT EXISTS` 옵션을 사용하면 역할이 이미 존재하는 경우 해당 문은 아무 동작도 하지 않음(no-op).

```
CREATE ROLE other_role;
CREATE ROLE IF NOT EXISTS other_role;
```

---

##### ALTER ROLE

`ALTER ROLE` 문은 역할 옵션을 수정(modify)함.

```
alter_role_statement ::= ALTER ROLE role_name WITH role_options
```

예시:

```
ALTER ROLE bob WITH PASSWORD = 'PASSWORD_B' AND SUPERUSER = false;
```

###### 특정 데이터센터로의 접속 제한(Restricting connections to specific datacenters)

`network_authorizer`가 구성된 경우, `ACCESS TO DATACENTERS` 절에 접근 가능한 데이터센터의 집합 리터럴을 지정해 로그인 역할을 특정 데이터센터로 제한 가능. 데이터센터 제한을 해제하려면 `ACCESS TO ALL DATACENTERS` 절을 사용.

`ALTER ROLE` 문 실행 조건은 다음과 같음.

- 다른 역할의 `SUPERUSER` 상태를 변경하려면 클라이언트가 `SUPERUSER` 상태여야 함
- 클라이언트는 자신이 현재 보유한 역할의 `SUPERUSER` 상태 변경 불가
- 클라이언트는 로그인 시 사용한 역할의 일부 속성(예: `PASSWORD`)만 수정 가능
- 역할 속성을 수정하려면 해당 역할에 대한 `ALTER` 권한 필요

---

##### DROP ROLE

`DROP ROLE` 문은 역할을 제거(remove)함.

```
drop_role_statement ::= DROP ROLE [ IF EXISTS ] role_name
```

`DROP ROLE`을 실행하려면 해당 역할에 대한 `DROP` 권한 필요. 클라이언트는 로그인 시 사용한 자신의 역할을 `DROP` 불가하며, `SUPERUSER` 상태인 클라이언트만 다른 `SUPERUSER` 역할을 `DROP` 가능.

존재하지 않는 역할을 삭제하려고 하면 유효하지 않은 쿼리 오류가 발생함. `IF EXISTS` 옵션을 사용하면 역할이 존재하지 않는 경우 해당 문은 아무 동작도 하지 않음(no-op).

참고: `DROP ROLE`은 의도적으로 열려 있는 사용자 세션을 종료하지 않음. 현재 연결된 세션은 그대로 유지되며, 인가(authorization)가 필요하지 않은 데이터베이스 작업은 계속 수행 가능. 인가가 활성화되어 있으면 삭제된 역할의 권한도 함께 철회되며, 이는 `cassandra.yaml`에 구성된 캐싱(caching) 옵션의 영향을 받음. 삭제된 역할이 새로운 권한이나 역할을 부여받아 다시 생성되면, 여전히 연결되어 있는 클라이언트 세션은 새로 부여된 권한과 역할을 획득함.

---

##### GRANT ROLE

`GRANT ROLE` 문은 한 역할을 다른 역할에 할당함.

```
grant_role_statement ::= GRANT role_name TO role_name
```

예시:

```
GRANT report_writer TO alice;
```

이 문은 `report_writer` 역할을 `alice`에게 부여함. `report_writer`에 부여된 모든 권한은 `alice`도 획득함.

역할은 방향성 비순환 그래프(directed acyclic graph, DAG)로 모델링되므로 순환 부여(circular grant)는 허용되지 않음. 다음 예시들은 오류를 발생시킴.

```
GRANT role_a TO role_b;
GRANT role_b TO role_a;

GRANT role_a TO role_b;
GRANT role_b TO role_c;
GRANT role_c TO role_a;
```

---

##### REVOKE ROLE

`REVOKE ROLE` 문은 역할 할당을 해제함.

```
revoke_role_statement ::= REVOKE role_name FROM role_name
```

예시:

```
REVOKE report_writer FROM alice;
```

이 문은 `alice`에게서 `report_writer` 역할을 철회함. `alice`가 `report_writer` 역할을 통해 획득했던 모든 권한도 함께 철회됨.

---

##### LIST ROLES

시스템에 알려진 모든 역할(또는 특정 역할에 부여된 역할)은 `LIST ROLES` 문으로 조회 가능.

```
list_roles_statement ::= LIST ROLES [ OF role_name ] [ NORECURSIVE ]
```

예시:

```
LIST ROLES;
```

시스템에 알려진 모든 역할을 반환함. 이를 위해서는 데이터베이스 역할 리소스에 대한 `DESCRIBE` 권한이 필요함.

다음 예시는 `alice`에게 부여된 모든 역할을 반환하며, 전이적으로(transitively) 획득한 역할도 포함함.

```
LIST ROLES OF alice;
```

다음 예시는 `bob`에게 직접(directly) 부여된 역할만 반환하며, 전이적으로 획득한 역할은 포함하지 않음.

```
LIST ROLES OF bob NORECURSIVE;
```

---

#### 사용자(Users)

Cassandra 2.2에서 역할(role) 개념이 도입되기 전에는 인증(authentication)과 인가(authorization)가 `USER`라는 개념에 기반함. 하위 호환성(backward compatibility)을 위해 레거시(legacy) 구문이 보존되었으며, `USER` 중심 문들은 `ROLE` 기반의 동등한 문(equivalent)에 대한 동의어(synonym)가 됨. 즉, 사용자를 생성/수정하는 것은 역할을 생성/수정하는 또 다른 구문임.

---

##### CREATE USER

`CREATE USER` 문은 새로운 사용자를 생성함.

```
create_user_statement ::= CREATE USER [ IF NOT EXISTS ] role_name
                          [ WITH PASSWORD string ]
                          [ user_option ]
user_option: SUPERUSER | NOSUPERUSER
```

예시:

```
CREATE USER alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE USER bob WITH PASSWORD 'password_b' NOSUPERUSER;
```

`CREATE USER`는 `LOGIN = true`인 `CREATE ROLE`과 동등함. 다음 문 쌍들은 서로 등가임.

```
CREATE USER alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = true;

CREATE USER IF NOT EXISTS alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE ROLE IF NOT EXISTS alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = true;

CREATE USER alice WITH PASSWORD 'password_a' NOSUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = false;

CREATE USER alice WITH PASSWORD 'password_a' NOSUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;

CREATE USER alice WITH PASSWORD 'password_a';
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;
```

---

##### ALTER USER

`ALTER USER` 문은 사용자 옵션을 수정함.

```
alter_user_statement ::= ALTER USER role_name [ WITH PASSWORD string ] [ user_option ]
```

예시:

```
ALTER USER alice WITH PASSWORD 'PASSWORD_A';
ALTER USER bob SUPERUSER;
```

---

##### DROP USER

`DROP USER` 문은 사용자를 제거함.

```
drop_user_statement ::= DROP USER [ IF EXISTS ] role_name
```

---

##### LIST USERS

기존 사용자는 `LIST USERS` 문으로 열거 가능.

```
list_users_statement ::= LIST USERS
```

이 문은 `LIST ROLES`와 동등하나, 출력에는 `LOGIN` 권한을 가진 역할만 포함됨.

---

#### 데이터 제어(Data Control)

##### 권한(Permissions)

리소스에 대한 권한은 역할에 부여됨. Cassandra에는 여러 종류의 리소스 타입이 있으며, 각각 계층적(hierarchical)으로 모델링됨.

- 데이터 리소스(Data resource), 즉 키스페이스(Keyspace)와 테이블(Table)의 계층: `ALL KEYSPACES` → `KEYSPACE` → `TABLE`
- 함수 리소스(Function resource): `ALL FUNCTIONS` → `KEYSPACE` → `FUNCTION`
- 역할을 표현하는 리소스: `ALL ROLES` → `ROLE`
- JMX ObjectName을 표현하는 리소스(MBean/MXBean 집합에 매핑됨): `ALL MBEANS` → `MBEAN`

권한은 이 계층 구조의 어느 레벨에서든 부여될 수 있으며, 하위로 전파됨(flow downwards). 상위 리소스에 권한을 부여하면 하위의 모든 리소스에 동일한 권한이 자동 적용됨. 예를 들어 `KEYSPACE`에 `SELECT`를 부여하면 해당 키스페이스 내 모든 `TABLE`에 자동으로 부여됨. 마찬가지로 `ALL FUNCTIONS`에 권한을 부여하면 키스페이스 범위에 관계없이 정의된 모든 함수에 부여됨. 특정 키스페이스에 속한 모든 함수에만 권한을 부여하는 것도 가능.

권한 변경 사항은 기존 클라이언트 세션에도 즉시 반영됨. 재접속 불필요.

사용 가능한 권한의 전체 집합은 다음과 같음.

- `CREATE`
- `ALTER`
- `DROP`
- `SELECT`
- `MODIFY`
- `AUTHORIZE`
- `DESCRIBE`
- `EXECUTE`

모든 권한이 모든 리소스 타입에 적용되는 것은 아님. 예를 들어 `EXECUTE`는 함수나 mbean 맥락에서만 의미가 있음. 적용할 수 없는 리소스에 권한을 `GRANT`하려고 하면 오류 응답(error response)이 반환됨.

###### 권한별 적용 리소스와 허용 작업

- `CREATE`
  - `ALL KEYSPACES`: 임의의 키스페이스에서 `CREATE KEYSPACE` 및 `CREATE TABLE`
  - `KEYSPACE`: 지정된 키스페이스에서 `CREATE TABLE`
  - `ALL FUNCTIONS`: 임의의 키스페이스에서 `CREATE FUNCTION` 및 `CREATE AGGREGATE`
  - `ALL FUNCTIONS IN KEYSPACE`: 지정된 키스페이스에서 `CREATE FUNCTION` 및 `CREATE AGGREGATE`
  - `ALL ROLES`: `CREATE ROLE`
- `ALTER`
  - `ALL KEYSPACES`: 임의의 키스페이스에서 `ALTER KEYSPACE` 및 `ALTER TABLE`
  - `KEYSPACE`: 지정된 키스페이스에서 `ALTER KEYSPACE` 및 `ALTER TABLE`
  - `TABLE`: `ALTER TABLE`
  - `ALL FUNCTIONS`: `CREATE FUNCTION` 및 `CREATE AGGREGATE`로 기존 함수 교체(replacing)
  - `ALL FUNCTIONS IN KEYSPACE`: `CREATE FUNCTION` 및 `CREATE AGGREGATE`로 지정된 키스페이스 내 기존 함수 교체
  - `FUNCTION`: `CREATE FUNCTION` 및 `CREATE AGGREGATE`로 기존 함수 교체
  - `ALL ROLES`: 임의의 역할에 대한 `ALTER ROLE`
  - `ROLE`: `ALTER ROLE`
- `DROP`
  - `ALL KEYSPACES`: 임의의 키스페이스에서 `DROP KEYSPACE` 및 `DROP TABLE`
  - `KEYSPACE`: 지정된 키스페이스에서 `DROP TABLE`
  - `TABLE`: `DROP TABLE`
  - `ALL FUNCTIONS`: 임의의 키스페이스에서 `DROP FUNCTION` 및 `DROP AGGREGATE`
  - `ALL FUNCTIONS IN KEYSPACE`: 지정된 키스페이스에서 `DROP FUNCTION` 및 `DROP AGGREGATE`
  - `FUNCTION`: `DROP FUNCTION`
  - `ALL ROLES`: 임의의 역할에 대한 `DROP ROLE`
  - `ROLE`: `DROP ROLE`
- `SELECT`
  - `ALL KEYSPACES`: 임의의 테이블에 대한 `SELECT`
  - `KEYSPACE`: 지정된 키스페이스 내 임의의 테이블에 대한 `SELECT`
  - `TABLE`: 지정된 테이블에 대한 `SELECT`
  - `ALL MBEANS`: 임의의 mbean에 대해 getter 메서드 호출
  - `MBEANS`: 와일드카드 패턴(wildcard pattern)에 일치하는 임의의 mbean에 대해 getter 메서드 호출
  - `MBEAN`: 지정된 mbean에 대해 getter 메서드 호출
- `MODIFY`
  - `ALL KEYSPACES`: 임의의 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`
  - `KEYSPACE`: 지정된 키스페이스 내 임의의 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`
  - `TABLE`: 지정된 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`
  - `ALL MBEANS`: 임의의 mbean에 대해 setter 메서드 호출
  - `MBEANS`: 와일드카드 패턴에 일치하는 임의의 mbean에 대해 setter 메서드 호출
  - `MBEAN`: 지정된 mbean에 대해 setter 메서드 호출
- `AUTHORIZE`
  - `ALL KEYSPACES`: 임의의 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `KEYSPACE`: 지정된 키스페이스 내 임의의 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `TABLE`: 지정된 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `ALL FUNCTIONS`: 임의의 함수에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `ALL FUNCTIONS IN KEYSPACE`: 지정된 키스페이스에서의 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `FUNCTION`: 지정된 함수에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `ALL MBEANS`: 임의의 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `MBEANS`: 와일드카드 패턴에 일치하는 임의의 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `MBEAN`: 지정된 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION`
  - `ALL ROLES`: 임의의 역할에 대한 `GRANT ROLE` 및 `REVOKE ROLE`
  - `ROLES`: 지정된 역할에 대한 `GRANT ROLE` 및 `REVOKE ROLE`
- `DESCRIBE`
  - `ALL ROLES`: 모든 역할 또는 다른 지정된 역할에 부여된 역할에 대한 `LIST ROLES`
  - `ALL MBEANS`: 플랫폼의 MBeanServer로부터 임의의 mbean에 대한 메타데이터 조회
  - `MBEANS`: 플랫폼의 MBeanServer로부터 와일드카드 패턴에 일치하는 임의의 mbean에 대한 메타데이터 조회
  - `MBEAN`: 플랫폼의 MBeanServer로부터 지정된 mbean에 대한 메타데이터 조회
- `EXECUTE`
  - `ALL FUNCTIONS`: 임의의 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 임의의 함수 사용
  - `ALL FUNCTIONS IN KEYSPACE`: 지정된 키스페이스 내 임의의 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 해당 키스페이스 내 임의의 함수 사용
  - `FUNCTION`: 지정된 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 해당 함수 사용
  - `ALL MBEANS`: 임의의 mbean에 대해 작업(operation) 실행
  - `MBEANS`: 와일드카드 패턴에 일치하는 임의의 mbean에 대해 작업 실행
  - `MBEAN`: 지정된 mbean에 대해 작업 실행

---

##### GRANT PERMISSION

`GRANT PERMISSION` 문은 권한을 할당함.

```
grant_permission_statement ::= GRANT permissions ON resource TO role_name
permissions ::= ALL [ PERMISSIONS ] | permission [ PERMISSION ]
permission ::= CREATE | ALTER | DROP | SELECT | MODIFY | AUTHORIZE | DESCRIBE | EXECUTE
resource ::=    ALL KEYSPACES
                | KEYSPACE keyspace_name
                | [ TABLE ] table_name
                | ALL ROLES
                | ROLE role_name
                | ALL FUNCTIONS [ IN KEYSPACE keyspace_name ]
                | FUNCTION function_name '(' [ cql_type ( ',' cql_type )* ] ')'
                | ALL MBEANS
                | ( MBEAN | MBEANS ) string
```

예시:

```
GRANT SELECT ON ALL KEYSPACES TO data_reader;
```

이 예시는 `data_reader` 역할을 가진 사용자에게 모든 키스페이스의 모든 테이블에 대해 `SELECT` 문을 실행할 수 있는 권한을 부여함.

```
GRANT MODIFY ON KEYSPACE keyspace1 TO data_writer;
```

이는 `data_writer` 역할을 가진 사용자에게 `keyspace1` 키스페이스의 모든 테이블에 대해 `UPDATE`, `INSERT`, `DELETE`, `TRUNCATE` 쿼리를 실행할 수 있는 권한을 부여함.

```
GRANT DROP ON keyspace1.table1 TO schema_owner;
```

이는 `schema_owner` 역할을 가진 사용자에게 `keyspace1.table1` 테이블을 `DROP`할 수 있는 권한을 부여함.

```
GRANT EXECUTE ON FUNCTION keyspace1.user_function( int ) TO report_writer;
```

이 명령은 `report_writer` 역할을 가진 사용자에게 `keyspace1.user_function( int )` 함수를 사용하는 `SELECT`, `INSERT`, `UPDATE` 쿼리를 실행할 수 있는 권한을 부여함.

```
GRANT DESCRIBE ON ALL ROLES TO role_admin;
```

이는 `role_admin` 역할을 가진 사용자에게 `LIST ROLES` 문으로 시스템의 모든 역할을 조회할 수 있는 권한을 부여함.

###### GRANT ALL

`GRANT ALL` 형식을 사용하면 대상 리소스에 따라 적절한 권한 집합이 자동으로 결정됨.

###### 자동 부여(Automatic Granting)

`CREATE KEYSPACE`, `CREATE TABLE`, `CREATE FUNCTION`, `CREATE AGGREGATE`, `CREATE ROLE` 문으로 리소스가 생성되면, 해당 문을 실행한 데이터베이스 사용자(식별된 역할)는 새 리소스에 대해 적용 가능한 모든 권한을 자동으로 부여받음.

---

##### REVOKE PERMISSION

`REVOKE PERMISSION` 문은 역할로부터 권한을 제거함.

```
revoke_permission_statement ::= REVOKE permissions ON resource FROM role_name
```

예시:

```
REVOKE SELECT ON ALL KEYSPACES FROM data_reader;
REVOKE MODIFY ON KEYSPACE keyspace1 FROM data_writer;
REVOKE DROP ON keyspace1.table1 FROM schema_owner;
REVOKE EXECUTE ON FUNCTION keyspace1.user_function( int ) FROM report_writer;
REVOKE DESCRIBE ON ALL ROLES FROM role_admin;
```

드라이버(driver)의 정상 동작을 위해 일부 테이블은 `SELECT` 권한을 철회할 수 없음. 다음 테이블들은 할당된 역할과 무관하게 모든 인가된 사용자에게 항상 접근 가능함.

```
* system_schema.keyspaces
* system_schema.columns
* system_schema.tables
* system.local
* system.peers
```

---

##### LIST PERMISSIONS

`LIST PERMISSIONS` 문은 부여된 권한을 표시함.

```
list_permissions_statement ::= LIST permissions [ ON resource ] [ OF role_name [ NORECURSIVE ] ]
```

예시:

```
LIST ALL PERMISSIONS OF alice;
```

`alice`에게 부여된 모든 권한을 표시하며, 다른 역할을 통해 전이적으로 획득한 권한도 포함함.

```
LIST ALL PERMISSIONS ON keyspace1.table1 OF bob;
```

`keyspace1.table1`에 대해 `bob`에게 부여된 모든 권한을 표시하며, 다른 역할을 통해 전이적으로 획득한 권한도 포함함. 또한 `keyspace1.table1`에 적용될 수 있는 리소스 계층 상위의 권한도 포함됨. 예를 들어 `bob`이 `keyspace1`에 대한 `ALTER` 권한을 가지고 있다면 이 권한도 쿼리 결과에 포함됨. `NORECURSIVE` 스위치를 추가하면 결과가 `bob` 또는 `bob`이 보유한 역할에 직접 부여된 권한으로만 제한됨.

```
LIST SELECT PERMISSIONS OF carlos;
```

`carlos` 또는 `carlos`가 보유한 역할에 부여된 권한 중 임의의 리소스에 대한 `SELECT` 권한만 표시함.

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL JSON](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/json.html)
- [CQL Triggers](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/triggers.html)
- [CQL Security](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/security.html)
