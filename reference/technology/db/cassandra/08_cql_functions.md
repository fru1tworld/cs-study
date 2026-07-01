# CQL 함수와 집계

> 원본: https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html

---

## 목차

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

## 개요

CQL은 데이터를 변환하는 데 사용할 수 있는 여러 종류의 **함수**(function)를 지원합니다. 함수는 크게 두 가지로 나뉩니다.

- **스칼라 함수(scalar functions)**: 입력으로 받은 값을 새로운 값으로 변환합니다. 결과 집합의 각 행(row)마다 평가됩니다.
- **집계 함수(aggregate functions)**: 여러 행에 걸쳐 값을 집계하여 단일 결과 값을 만들어 냅니다.

각 분류에는 Cassandra가 기본으로 제공하는 **내장 함수**(native function)와, 사용자가 직접 코드를 작성하여 정의하는 **사용자 정의 함수(user-defined function, UDF)** 및 **사용자 정의 집계**(user-defined aggregate, UDA)가 존재합니다.

> 참고: 스칼라 함수와 집계 함수는 조정자(coordinator) 노드에서 평가됩니다. 결과 집합이 클 경우, 클라이언트에 데이터를 반환하기 전에 조정자에서 계산이 수행된다는 점에 유의해야 합니다.

---

## 스칼라 함수(Scalar Functions)

스칼라 함수는 결과 집합의 각 행에 대해 평가되며, 하나 이상의 값을 입력받아 단일 값을 반환합니다.

### 내장 함수(Native Functions)

Cassandra는 다양한 내장 스칼라 함수를 제공합니다.

> **결정성(deterministic)과 단조성(monotonicity)에 관한 참고**
> 일부 내장 함수는 **결정적**(deterministic)이지 않습니다. 즉, 동일한 입력에 대해 항상 동일한 결과를 보장하지 않습니다. 대표적으로 `uuid()`, `now()`와 같은 함수가 그렇습니다. 이러한 비결정적 함수를 사용자 정의 집계(UDA) 내부에서 사용하면 집계가 결정적 동작을 전제로 하기 때문에 예측 불가능한 결과가 나올 수 있습니다.

---

#### Cast

`cast` 함수는 한 데이터 타입(native datatype)의 값을 다른 데이터 타입으로 변환하는 데 사용할 수 있습니다.

예를 들어, `int` 타입의 `count` 컬럼 평균을 `double`로 계산하려면 다음과 같이 사용합니다.

```cql
SELECT avg(cast(count as double)) FROM myTable
```

`cast` 함수가 지원하는 변환(원본 타입 → 변환 가능한 대상 타입)은 다음 표와 같습니다.

| 원본 타입(From) | 변환 가능한 대상 타입(To) |
| --- | --- |
| `ascii` | `text`, `varchar` |
| `bigint` | `tinyint`, `smallint`, `int`, `float`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `boolean` | `text`, `varchar` |
| `counter` | `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `date` | `timestamp` |
| `decimal` | `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `varint`, `text`, `varchar` |
| `double` | `tinyint`, `smallint`, `int`, `bigint`, `float`, `decimal`, `varint`, `text`, `varchar` |
| `float` | `tinyint`, `smallint`, `int`, `bigint`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `inet` | `text`, `varchar` |
| `int` | `tinyint`, `smallint`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `smallint` | `tinyint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `time` | `text`, `varchar` |
| `timestamp` | `date`, `text`, `varchar` |
| `timeuuid` | `timestamp`, `date`, `text`, `varchar` |
| `tinyint` | `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `varint`, `text`, `varchar` |
| `uuid` | `text`, `varchar` |
| `varint` | `tinyint`, `smallint`, `int`, `bigint`, `float`, `double`, `decimal`, `text`, `varchar` |

`cast` 함수는 자기 자신과 동일한 타입으로의 변환(예: `int` → `int`)도 허용하지만, 이는 아무런 효과가 없는 동일 변환(no-op)입니다.

---

#### Token

`token` 함수는 특정 파티션 키(partition key)에 대한 **토큰(token)** 값을 계산합니다. 토큰은 클러스터 내에서 데이터가 어떻게 분산되는지를 결정하는 값입니다.

`token` 함수가 반환하는 정확한 타입은 클러스터에 설정된 파티셔너(partitioner)에 따라 달라집니다.

- `Murmur3Partitioner`의 경우 반환 타입은 `bigint` 입니다.
- `RandomPartitioner`의 경우 반환 타입은 `varint` 입니다.
- `ByteOrderedPartitioner`의 경우 반환 타입은 `blob` 입니다.

예를 들어 다음과 같은 테이블이 있다고 가정합니다.

```cql
CREATE TABLE users (
    userid text PRIMARY KEY,
    username text,
);
```

이 경우 `userid` 컬럼에 대해 다음과 같이 토큰을 계산할 수 있습니다.

```cql
SELECT token(userid) FROM users;
```

`token` 함수는 데이터가 클러스터 전반에 어떻게 분산되는지를 이해하거나, 파티션 키를 기준으로 결과를 페이징(paging)할 때 유용합니다.

---

#### Uuid

`uuid` 함수는 인자를 받지 않으며, 호출될 때마다 **랜덤한 타입 4(version 4) UUID**를 생성합니다.

```cql
INSERT INTO users (id, name) VALUES (uuid(), 'Alice');
```

`uuid()`는 결과 집합의 각 행마다, 그리고 호출될 때마다 새로운 무작위 값을 반환합니다.

---

#### Timeuuid 함수

##### now

`now` 함수는 인자를 받지 않으며, 호출 시점의 노드 시간을 기반으로 한 **새로운 타입 1(version 1) timeuuid**를 생성합니다. 단, 단일 구문(statement) 내의 모든 호출은 동일한 값을 생성한다는 점에 유의해야 합니다.

이 함수는 timeuuid 값을 기준으로 데이터를 삽입할 때 유용합니다.

```cql
INSERT INTO myTable (t) VALUES (now());
```

##### min_timeuuid 와 max_timeuuid

`min_timeuuid`(예전 명칭 `minTimeuuid`)와 `max_timeuuid`(예전 명칭 `maxTimeuuid`) 함수는 `timestamp` 또는 `timestamp`로 변환 가능한 `date` 문자열을 입력으로 받아, 해당 타임스탬프에 대응하는 **가짜(fake) timeuuid**를 반환합니다.

- `min_timeuuid`는 해당 타임스탬프에 대해 가능한 가장 작은(가장 이른) timeuuid를 반환합니다.
- `max_timeuuid`는 해당 타임스탬프에 대해 가능한 가장 큰(가장 늦은) timeuuid를 반환합니다.

이 두 함수는 `timeuuid` 컬럼에 대해 특정 타임스탬프 범위를 질의할 때 유용합니다. 예를 들어 다음 쿼리는 `t` 컬럼이 `'2013-01-01 00:05+0000'`(포함) 이상이고 `'2013-02-02 10:00+0000'`(포함) 이하인 모든 행을 조회합니다.

```cql
SELECT * FROM myTable
WHERE t > min_timeuuid('2013-01-01 00:05+0000')
  AND t < max_timeuuid('2013-02-02 10:00+0000')
```

> **주의:** `min_timeuuid`와 `max_timeuuid`가 반환하는 값은 실제 timeuuid가 아니라, 해당 타임스탬프 경계를 표현하기 위한 가짜 값입니다. 따라서 이 값들을 다른 곳에 삽입하거나 일반적인 timeuuid처럼 사용해서는 안 되며, 오직 위와 같은 범위 비교 용도로만 사용해야 합니다. 또한 위 쿼리에서 부등호(`>`, `<`)와 등호를 포함하는 부등호(`>=`, `<=`)의 차이에 주의해야 합니다.

---

#### 날짜/시간(Datetime) 함수

##### 현재 값을 조회하는 함수

다음 함수들은 인자 없이 호출되며, 조정자 노드의 현재 날짜/시간을 다양한 타입으로 반환합니다.

| 함수 | 반환 타입 | 설명 |
| --- | --- | --- |
| `current_timestamp()` | `timestamp` | 현재 타임스탬프(밀리초 정밀도)를 반환합니다. |
| `current_date()` | `date` | 현재 날짜를 반환합니다. |
| `current_time()` | `time` | 현재 시각(자정 이후 경과 시간)을 반환합니다. |
| `current_timeuuid()` | `timeuuid` | 현재 시점의 timeuuid를 반환합니다(`now()`와 동등). |

예를 들어 현재 날짜 이후의 데이터를 조회하려면 다음과 같이 사용합니다.

```cql
SELECT * FROM myTable WHERE date >= current_date();
```

##### 시간/날짜 타입 간 변환 함수

다음 함수들은 `timeuuid`, `timestamp`, `date` 타입 간의 변환을 수행합니다.

| 함수 | 입력 타입 | 반환 타입 |
| --- | --- | --- |
| `to_date(timeuuid)` | `timeuuid` | `date` |
| `to_date(timestamp)` | `timestamp` | `date` |
| `to_timestamp(timeuuid)` | `timeuuid` | `timestamp` |
| `to_timestamp(date)` | `date` | `timestamp` |
| `to_unix_timestamp(timeuuid)` | `timeuuid` | `bigint` |
| `to_unix_timestamp(timestamp)` | `timestamp` | `bigint` |
| `to_unix_timestamp(date)` | `date` | `bigint` |

- `to_date` 함수는 `timeuuid` 또는 `timestamp`를 `date` 값으로 변환합니다.
- `to_timestamp` 함수는 `timeuuid` 또는 `date`를 `timestamp` 값으로 변환합니다.
- `to_unix_timestamp` 함수는 `timeuuid`, `timestamp`, `date`를 유닉스(Unix) 타임스탬프(에포크 기준 밀리초 단위의 `bigint`)로 변환합니다.

> 예전 버전의 함수명(`toDate`, `toTimestamp`, `toUnixTimestamp` 등 카멜 케이스)은 하위 호환을 위해 여전히 사용할 수 있습니다.

예를 들어, `timeuuid` 컬럼 `t`의 값을 사람이 읽기 쉬운 타임스탬프로 변환하려면 다음과 같이 사용합니다.

```cql
SELECT to_timestamp(t) FROM myTable;
```

---

#### Blob 변환 함수

CQL은 임의의 native 타입과 `blob` 사이를 변환하는 다수의 함수를 제공합니다. 이 함수들의 이름은 다음과 같은 규칙을 따릅니다.

- `type_as_blob(value)`: `type` 타입의 값을 받아 `blob`으로 변환합니다.
- `blob_as_type(value)`: `blob`을 받아 `type` 타입의 값으로 변환합니다.

즉, 모든 native 타입 `type`에 대해 `type_as_blob`이라는 함수와 `blob_as_type`이라는 함수가 정의됩니다(`blob` 자신은 제외).

예를 들어 `bigint`의 경우 다음과 같습니다.

```cql
// bigint 값 3을 blob으로 변환
bigint_as_blob(3)        // 결과: 0x0000000000000003

// blob을 다시 bigint로 변환
blob_as_bigint(0x0000000000000003)   // 결과: 3
```

> 예전 버전의 함수명(`bigintAsBlob`, `blobAsBigint` 등 카멜 케이스)도 하위 호환을 위해 사용할 수 있습니다.

---

#### 수학(Math) 함수

CQL은 다음과 같은 수학 함수를 제공합니다.

| 함수 | 설명 |
| --- | --- |
| `abs` | 입력값의 절대값(absolute value)을 반환합니다. |
| `exp` | 입력값의 지수값(자연상수 e의 거듭제곱, exponential)을 반환합니다. |
| `log` | 입력값의 자연로그(natural logarithm)를 반환합니다. |
| `log10` | 입력값의 밑이 10인 로그(base-10 logarithm)를 반환합니다. |
| `round` | 입력값을 가장 가까운 정수로 반올림합니다. 반올림 모드는 `HALF_UP`(반올림)을 사용합니다. |

> 이 함수들의 반환 타입은 항상 입력 타입과 동일합니다.

예시:

```cql
SELECT abs(temperature), round(score) FROM measurements;
```

---

#### 컬렉션(Collection) 함수

다음 함수들은 `map`, `set`, `list` 타입의 컬렉션 컬럼에 대해 동작합니다.

| 함수 | 설명 |
| --- | --- |
| `map_keys(map)` | 맵의 키(key)들로 이루어진 `set`을 반환합니다. |
| `map_values(map)` | 맵의 값(value)들로 이루어진 `list`를 반환합니다. |
| `collection_count(collection)` | 컬렉션에 포함된 원소의 개수를 반환합니다. |
| `collection_min(collection)` | `set` 또는 `list`에서 가장 작은 원소를 반환합니다. |
| `collection_max(collection)` | `set` 또는 `list`에서 가장 큰 원소를 반환합니다. |
| `collection_sum(collection)` | 숫자 컬렉션의 원소 합을 반환합니다. |
| `collection_avg(collection)` | 숫자 컬렉션의 원소 평균을 반환합니다. |

> `collection_sum`과 `collection_avg`는 숫자 타입 컬렉션에 대해서만 동작합니다. 결과는 입력 타입과 동일한 타입으로 반환되므로, 합계가 해당 타입의 최대값을 초과하는 경우 **오버플로**(overflow)가 발생할 수 있다는 점에 유의해야 합니다.

예시:

```cql
SELECT collection_count(tags), collection_max(scores) FROM myTable;
```

---

#### 데이터 마스킹(Data Masking) 함수

데이터 마스킹 함수는 민감한 데이터를 가려서 노출하는 데 사용됩니다. 컬럼 정의에 함께 적용하여 동적 데이터 마스킹(dynamic data masking)에 활용할 수 있습니다.

| 함수 | 설명 |
| --- | --- |
| `mask_null(value)` | 입력값에 관계없이 항상 `null`을 반환합니다. |
| `mask_default(value)` | 입력값을 타입별 고정 기본값으로 대체합니다(예: 텍스트는 `****`). |
| `mask_replace(value, replacement)` | 입력값을 지정한 대체값(`replacement`)으로 치환합니다. |
| `mask_inner(value, begin, end[, padding])` | 앞쪽 `begin`개와 뒤쪽 `end`개 문자를 제외한 가운데 문자들을 마스킹 문자로 가립니다. `padding`(선택)으로 마스킹 문자를 지정할 수 있습니다. |
| `mask_outer(value, begin, end[, padding])` | 앞쪽 `begin`개와 뒤쪽 `end`개의 바깥쪽 문자들을 마스킹하고 가운데는 그대로 둡니다. |
| `mask_hash(value[, algorithm])` | 입력값을 해시(hash)하여 그 결과를 `blob`으로 반환합니다. `algorithm`(선택)으로 해시 알고리즘을 지정할 수 있습니다. |

예시:

```cql
-- 신용카드 번호 컬럼에서 마지막 4자리만 노출
SELECT mask_inner(credit_card, 0, 4) FROM payments;

-- 이메일을 항상 ****로 노출
SELECT mask_default(email) FROM users;
```

---

#### 벡터 유사도(Vector Similarity) 함수

벡터 유사도 함수는 두 벡터 사이의 관계(유사도/거리)를 계산합니다. 주로 벡터 검색(vector search) 및 유사도 기반 질의에 사용됩니다.

| 함수 | 설명 |
| --- | --- |
| `similarity_cosine(vector, vector)` | 두 벡터의 코사인 유사도(cosine similarity)를 반환합니다. |
| `similarity_euclidean(vector, vector)` | 두 벡터 사이의 유클리드 거리(Euclidean distance) 기반 유사도를 반환합니다. |
| `similarity_dot_product(vector, vector)` | 두 벡터의 내적(dot product)을 반환합니다. |

> 이 함수들은 차원(dimension)이 동일한 `float` 벡터에 대해 동작합니다.

예시:

```cql
SELECT similarity_cosine(embedding, [0.1, 0.2, 0.3]) FROM documents;
```

---

### 사용자 정의 함수(User-Defined Functions, UDF)

사용자 정의 함수(UDF)를 사용하면 Cassandra 내부에서 사용자가 작성한 코드를 실행할 수 있습니다. 기본적으로 Cassandra는 **Java**로 작성된 함수를 지원합니다.

UDF는 단일 노드에서, 즉 조정자 노드에서 실행됩니다. 따라서 잘못 작성된 UDF는 노드의 성능에 영향을 줄 수 있으므로 주의해야 합니다.

#### CREATE FUNCTION

UDF는 `CREATE FUNCTION` 구문으로 생성합니다. 문법은 다음과 같습니다.

```
create_function_statement::= CREATE [ OR REPLACE ] FUNCTION [ IF NOT EXISTS ]
    function_name '(' arguments_declaration ')'
    [ CALLED | RETURNS NULL ] ON NULL INPUT
    RETURNS cql_type
    LANGUAGE identifier
    AS string

arguments_declaration: identifier cql_type ( ',' identifier cql_type )*
```

주요 구성 요소는 다음과 같습니다.

- `OR REPLACE`: 같은 이름과 시그니처(signature)의 함수가 이미 존재하면 덮어씁니다. `IF NOT EXISTS`와 함께 사용할 수 없습니다.
- `IF NOT EXISTS`: 같은 함수가 이미 존재하면 아무 작업도 하지 않습니다.
- **null 입력 처리**: 다음 두 가지 중 하나를 반드시 지정해야 합니다.
  - `RETURNS NULL ON NULL INPUT`: 인자 중 하나라도 `null`이면 함수 본문을 실행하지 않고 즉시 `null`을 반환합니다.
  - `CALLED ON NULL INPUT`: 인자에 `null`이 포함되어 있어도 함수 본문이 호출됩니다(함수 내부에서 `null`을 직접 처리해야 합니다).
- `RETURNS cql_type`: 함수의 반환 타입을 지정합니다.
- `LANGUAGE`: 함수 본문이 작성된 언어를 지정합니다(예: `java`).
- `AS string`: 함수 본문 코드입니다. 일반적으로 `$$ ... $$` 구분자로 감쌉니다.

##### 예시

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

가장 단순한 형태의 UDF 예시입니다.

```cql
CREATE FUNCTION some_function ( arg int )
    RETURNS NULL ON NULL INPUT
    RETURNS int
    LANGUAGE java
    AS $$ return arg; $$;
```

사용자 정의 타입(UDT)을 활용하는 예시입니다. UDF 내부에서는 `udfContext` 객체를 통해 UDT나 튜플(tuple) 값을 생성할 수 있습니다.

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

`udfContext` 인스턴스는 UDT 값(`UDTValue`)이나 튜플 값(`TupleValue`)을 생성할 때 사용할 수 있는 다음과 같은 메서드를 제공합니다.

- `UDTValue newArgUDTValue(String argName)`
- `UDTValue newArgUDTValue(int argNum)`
- `UDTValue newReturnUDTValue()`
- `UDTValue newUDTValue(String udtName)`
- `TupleValue newArgTupleValue(String argName)`
- `TupleValue newArgTupleValue(int argNum)`
- `TupleValue newReturnTupleValue()`
- `TupleValue newTupleValue(String cqlDefinition)`

#### DROP FUNCTION

함수는 `DROP FUNCTION` 구문으로 삭제합니다. 문법은 다음과 같습니다.

```
drop_function_statement::= DROP FUNCTION [ IF EXISTS ] function_name [ '(' arguments_signature ')' ]

arguments_signature::= cql_type ( ',' cql_type )*
```

##### 예시

```cql
DROP FUNCTION myfunction;
DROP FUNCTION mykeyspace.afunction;
DROP FUNCTION afunction ( int );
DROP FUNCTION afunction ( text );
```

> 동일한 이름의 함수가 여러 개 **오버로드**(overload)되어 있는 경우, 인자 시그니처(`( int )`, `( text )` 등)를 명시하여 삭제할 함수를 특정해야 합니다.

---

## 집계 함수(Aggregate Functions)

집계 함수는 `SELECT` 구문이 선택한 모든 행에 대해 동작하여, 단일 결과 값을 만들어 냅니다.

> **참고:** 일반 컬럼, 스칼라 함수, UDT 필드, 쓰기 시각(writetime), TTL이 집계 함수와 함께 선택되면, 결과 집합에서 처음으로 반환되는 행의 값만 반환됩니다.

### 내장 집계(Native Aggregates)

Cassandra는 다음과 같은 내장 집계 함수를 제공합니다.

#### Count

`count` 함수는 `SELECT` 구문이 반환하는 행(또는 비-`null` 값)의 개수를 셉니다.

```cql
SELECT COUNT(*) FROM plays;
```

```cql
SELECT COUNT(1) FROM plays;
```

특정 컬럼을 대상으로 `COUNT(column)`을 사용하면 해당 컬럼이 `null`이 아닌 행의 개수만 셉니다.

```cql
SELECT COUNT(scores) FROM plays;
```

#### Min 과 Max

`min`과 `max` 함수는 주어진 컬럼에 대해 각각 최소값과 최대값을 계산합니다.

```cql
SELECT MIN(players), MAX(players) FROM plays;
```

#### Sum

`sum` 함수는 주어진 컬럼이 반환하는 모든 값의 합을 계산합니다.

```cql
SELECT SUM(players) FROM plays;
```

> 합계가 컬럼 타입의 최대값을 초과하는 경우 **오버플로**(overflow)가 발생할 수 있으므로 주의해야 합니다.

#### Avg

`avg` 함수는 주어진 컬럼이 반환하는 모든 값의 평균을 계산합니다.

```cql
SELECT AVG(players) FROM plays;
```

> 빈 결과 집합에 대해서는 `0`을 반환합니다. 또한 정수 타입에 대해서는 평균 계산 시 반올림 또는 절삭(truncation)이 발생할 수 있습니다(앞서 다룬 `cast`를 사용하여 `double`로 변환한 뒤 평균을 구하면 소수점까지 정확한 평균을 얻을 수 있습니다).

---

### 사용자 정의 집계(User-Defined Aggregates, UDA)

사용자 정의 집계(UDA)를 사용하면 사용자 정의 함수(UDF)를 조합하여 커스텀 집계 로직을 구현할 수 있습니다. 가중 평균, 커스텀 통계, 도메인 특화 계산 등 내장 집계로 표현하기 어려운 집계를 만들 수 있습니다.

UDA는 다음 요소로 구성됩니다.

- **상태 함수(state function, `SFUNC`)**: 각 행을 처리하며 누적 상태(state)를 갱신하는 UDF입니다. 첫 번째 인자로 현재 상태를 받고, 나머지 인자로 집계 대상 값을 받아 새로운 상태를 반환합니다.
- **상태 타입(state type, `STYPE`)**: 누적 상태의 데이터 타입입니다.
- **최종 함수(final function, `FINALFUNC`, 선택)**: 모든 행을 처리한 뒤 최종 상태를 받아 최종 결과로 변환하는 UDF입니다. 생략하면 최종 상태가 그대로 결과가 됩니다.
- **초기 조건(initial condition, `INITCOND`, 선택)**: 상태의 초기값입니다. 생략하면 `null`이 사용됩니다.

#### CREATE AGGREGATE

UDA는 `CREATE AGGREGATE` 구문으로 생성합니다. 문법은 다음과 같습니다.

```
create_aggregate_statement ::= CREATE [ OR REPLACE ] AGGREGATE [ IF NOT EXISTS ]
    function_name '(' arguments_signature ')'
    SFUNC function_name
    STYPE cql_type
    [ FINALFUNC function_name ]
    [ INITCOND term ]
```

##### 예시: 평균(average) 집계 구현

다음은 정수 컬럼의 평균을 직접 구현한 UDA 예시입니다. 상태로 `tuple<int, bigint>`(개수, 합계)를 사용합니다.

먼저 상태 함수를 정의합니다. 각 행마다 개수를 1 증가시키고 합계에 값을 더합니다.

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

다음으로 최종 함수를 정의합니다. 누적된 합계를 개수로 나누어 평균(`double`)을 반환합니다.

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

이제 두 함수를 조합하여 집계를 생성합니다. 초기 조건(`INITCOND`)은 `(0, 0)`(개수 0, 합계 0)으로 설정합니다.

```cql
CREATE OR REPLACE AGGREGATE test.average(int)
    SFUNC averageState
    STYPE tuple
    FINALFUNC averageFinal
    INITCOND (0, 0);
```

생성한 집계를 테스트하기 위해 테이블을 만들고 데이터를 삽입한 뒤 집계를 호출합니다.

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

위 쿼리는 `val` 컬럼 값 `1, 2, 3, 4`의 평균인 `2.5`를 반환합니다.

> **주의:** UDA가 참조하는 UDF에서 `now()`나 `uuid()`처럼 비결정적(non-deterministic) 연산을 사용하면, 동일한 입력 데이터에 대해서도 실행할 때마다 결과가 달라질 수 있어 예측 불가능한 결과를 초래합니다. 집계는 결정적 동작을 전제로 하므로 이러한 함수의 사용을 피하는 것이 좋습니다.

#### DROP AGGREGATE

집계는 `DROP AGGREGATE` 구문으로 삭제합니다. 문법은 다음과 같습니다.

```
drop_aggregate_statement::= DROP AGGREGATE [ IF EXISTS ] function_name [ '(' arguments_signature ')' ]
```

##### 예시

```cql
DROP AGGREGATE myAggregate;
DROP AGGREGATE myKeyspace.anAggregate;
DROP AGGREGATE someAggregate ( int );
DROP AGGREGATE someAggregate ( text );
```

> UDF와 마찬가지로, 동일한 이름의 집계가 여러 개 오버로드되어 있는 경우 인자 시그니처를 명시하여 삭제할 집계를 특정해야 합니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL Functions](https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html)
