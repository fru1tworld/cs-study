# CQL 데이터 조작, 보조 인덱스, 구체화된 뷰

## CQL 데이터 조작 (DML)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html

---

### 목차

1. [개요](#개요)
2. [SELECT — 데이터 조회](#select--데이터-조회)
   - [선택 절(Selection Clause)](#선택-절selection-clause)
   - [별칭(Aliases)](#별칭aliases)
   - [WRITETIME, MAXWRITETIME, TTL 함수](#writetime-maxwritetime-ttl-함수)
   - [WHERE 절](#where-절)
   - [GROUP BY 절](#group-by-절)
   - [ORDER BY 절](#order-by-절)
   - [PER PARTITION LIMIT와 LIMIT](#per-partition-limit와-limit)
   - [ALLOW FILTERING](#allow-filtering)
   - [token 함수](#token-함수)
3. [INSERT — 데이터 삽입](#insert--데이터-삽입)
4. [UPDATE — 데이터 수정](#update--데이터-수정)
5. [DELETE — 데이터 삭제](#delete--데이터-삭제)
6. [BATCH — 일괄 처리](#batch--일괄-처리)
7. [경량 트랜잭션(Lightweight Transactions, LWT)](#경량-트랜잭션lightweight-transactions-lwt)
8. [업데이트 파라미터: TTL과 TIMESTAMP](#업데이트-파라미터-ttl과-timestamp)
9. [연산자(Operators)](#연산자operators)
   - [산술 연산자(Arithmetic Operators)](#산술-연산자arithmetic-operators)
   - [숫자 연산(Number Arithmetic)](#숫자-연산number-arithmetic)
   - [연산자 우선순위(Precedence)](#연산자-우선순위precedence)
   - [날짜/시간 연산(Datetime Arithmetic)](#날짜시간-연산datetime-arithmetic)
10. [참고 자료](#참고-자료)

---

### 개요

CQL(Cassandra Query Language)은 데이터를 삽입(insert)·수정(update)·삭제(delete)·조회(query)하기 위한 여러 문장(statement)을 제공함. 이 문서에서 다루는 데이터 조작(Data Manipulation, DML) 문장은 다음과 같음.

- `SELECT` — 데이터 조회
- `INSERT` — 데이터 삽입
- `UPDATE` — 데이터 수정
- `DELETE` — 데이터 삭제
- `BATCH` — 여러 변경 문장을 묶어서 처리

중요한 제약 사항으로, CQL은 조인(join)이나 서브쿼리(sub-query)를 실행하지 않음 → 조회는 항상 단일 테이블(single table)에 대해서만 수행됨.

---

### SELECT — 데이터 조회

`SELECT` 문은 하나의 Cassandra 테이블에서 데이터를 조회(query)함. `SELECT` 문은 최소한 하나의 선택 절(selection clause)과 조회 대상 테이블의 이름을 포함함.

문법(grammar)은 다음과 같음.

```
select_statement::= SELECT [ JSON | DISTINCT ] ( select_clause | '*' )
	FROM `table_name`
	[ WHERE `where_clause` ]
	[ GROUP BY `group_by_clause` ]
	[ ORDER BY `ordering_clause` ]
	[ PER PARTITION LIMIT (`integer` | `bind_marker`) ]
	[ LIMIT (`integer` | `bind_marker`) ]
	[ ALLOW FILTERING ]
select_clause::= `selector` [ AS `identifier` ] ( ',' `selector` [ AS `identifier` ] )
selector::== `column_name`
	| `term`
	| CAST '(' `selector` AS `cql_type` ')'
	| `function_name` '(' [ `selector` ( ',' `selector` )_ ] ')'
	| COUNT '(' '_' ')'
where_clause::= `relation` ( AND `relation` )*
relation::= column_name operator term
	'(' column_name ( ',' column_name )* ')' operator tuple_literal
	TOKEN '(' column_name# ( ',' column_name )* ')' operator term
operator::= '=' | '<' | '>' | '<=' | '>=' | '!=' | IN | CONTAINS | CONTAINS KEY
group_by_clause::= column_name ( ',' column_name )*
ordering_clause::= column_name [ ASC | DESC ] ( ',' column_name [ ASC | DESC ] )*
```

예시:

```sql
SELECT name, occupation FROM users WHERE userid IN (199, 200, 207);
SELECT JSON name, occupation FROM users WHERE userid = 199;
SELECT name AS user_name, occupation AS user_occupation FROM users;

SELECT time, value
FROM events
WHERE event_type = 'myEvent'
  AND time > '2011-02-03'
  AND time <= '2012-01-01'

SELECT COUNT (*) AS user_count FROM users;
```

위 예시에서 알 수 있듯이 `SELECT` 문은 다음을 포함할 수 있음.

- 선택 절(`select_clause`): 조회할 컬럼이나 표현식을 지정
- `WHERE` 절: 조회 결과를 좁히는 조건을 지정
- `ORDER BY` / `LIMIT` 등의 부가 절: 결과를 정렬하거나 제한

---

#### 선택 절(Selection Clause)

선택 절은 어떤 결과를 반환할지를 결정함. 선택 절은 셀렉터(selector) 또는 와일드카드 `*`(전체 컬럼 반환)로 구성됨.

셀렉터(selector)는 다음 중 하나일 수 있음.

- `column_name` — 테이블에 정의된 컬럼 이름. 해당 컬럼의 값을 선택
- `term` — 리터럴 값(literal). 모든 결과 행에 대해 해당 값을 반환
- `CAST` — 어떤 셀렉터의 결과를 지정한 CQL 타입으로 변환
- `function_name(...)` — 함수에 다른 셀렉터를 인자로 적용한 결과. 기본 함수에는 집계 함수(aggregate)도 포함됨
- `COUNT(*)` — 조회된 행의 개수를 셈

`DISTINCT` 키워드를 사용하면 파티션 키(partition key)의 정적(static) 부분만 조회하면서 중복 제거 가능.

---

#### 별칭(Aliases)

`AS` 키워드를 사용하면 셀렉터에 별칭(alias)을 부여하여 결과 집합의 컬럼 이름 변경 가능.

```sql
-- 컬럼 이름 변경
SELECT name AS user_name, occupation AS user_occupation FROM users;

-- 함수 결과에 별칭 부여
SELECT COUNT (*) AS user_count FROM users;
```

---

#### WRITETIME, MAXWRITETIME, TTL 함수

특정 컬럼에 대한 메타데이터를 조회하기 위해 다음 특수 함수 사용 가능.

```sql
WRITETIME(column_name)
MAXWRITETIME(column_name)
TTL(column_name)
WRITETIME(phones[2..4])
WRITETIME(user.name)
```

- `WRITETIME(column_name)` — 해당 컬럼 값이 기록된 시점의 타임스탬프(timestamp, 마이크로초 단위)를 반환
- `MAXWRITETIME(column_name)` — 해당 컬럼에 대한 가장 최근의 쓰기 타임스탬프를 반환
- `TTL(column_name)` — 해당 컬럼 값이 만료되기까지 남은 시간(초 단위, Time To Live)을 반환

이 함수들은 컬렉션(collection)의 일부 요소(`phones[2..4]`)나 UDT(User-Defined Type)의 필드(`user.name`)에도 적용 가능.

---

#### WHERE 절

`WHERE` 절은 조회 대상 행(row)을 좁히는 조건을 지정함. 조건은 관계식(relation)들을 `AND`로 연결하여 표현함.

```
where_clause::= `relation` ( AND `relation` )*
relation::= column_name operator term
	'(' column_name ( ',' column_name )* ')' operator tuple_literal
	TOKEN '(' column_name ( ',' column_name )* ')' operator term
operator::= '=' | '<' | '>' | '<=' | '>=' | '!=' | IN | CONTAINS | CONTAINS KEY
```

`WHERE` 절에서 사용 가능한 연산자(operator):

- `=` — 등호
- `<`, `>`, `<=`, `>=` — 범위 비교
- `!=` — 부등호
- `IN` — 여러 값 중 하나와 일치
- `CONTAINS` — 컬렉션이 특정 값을 포함하는지
- `CONTAINS KEY` — 맵(map)이 특정 키를 포함하는지

WHERE 절 사용에 대한 규칙은 다음과 같음.

- 파티션 키(partition key)의 컬럼들은 `=` 또는 `IN` 연산자로 제한해야 함
- 클러스터링(clustering) 컬럼에는 범위(`<`, `>`, `<=`, `>=`) 제약 사용 가능
- 인덱스(index)가 있는 컬럼에 대해서도 조건 부여 가능

`IN` 연산자 사용 예시:

```sql
SELECT name, occupation FROM users WHERE userid IN (199, 200, 207);
```

범위 조건 사용 예시:

```sql
SELECT time, value
FROM events
WHERE event_type = 'myEvent'
  AND time > '2011-02-03'
  AND time <= '2012-01-01'
```

---

#### GROUP BY 절

`GROUP BY` 절은 동일한 값을 가진 행들을 하나의 그룹으로 묶어(condense) 처리함. 그룹화는 파티션 키(partition key)와 클러스터링 컬럼(clustering column)으로만 수행 가능.

```
group_by_clause::= column_name ( ',' column_name )*
```

`GROUP BY`는 일반적으로 집계 함수(aggregate function, 예: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`)와 함께 사용되어 각 그룹별 집계 결과를 얻음.

---

#### ORDER BY 절

`ORDER BY` 절은 클러스터링 순서(clustering order) 정의에 따라 결과를 정렬함.

```
ordering_clause::= column_name [ ASC | DESC ] ( ',' column_name [ ASC | DESC ] )*
```

- `ASC` — 오름차순(ascending)
- `DESC` — 내림차순(descending)

정렬은 클러스터링 컬럼을 기준으로만 가능하며, 테이블 정의 시 지정된 클러스터링 순서를 따르거나 그 역순으로만 정렬 가능.

---

#### PER PARTITION LIMIT와 LIMIT

`LIMIT` 절은 쿼리가 반환하는 전체 행의 개수를 제한함.

`PER PARTITION LIMIT` 절은 각 파티션(partition)별로 반환되는 행의 개수를 제한함. 여러 파티션에 걸쳐 조회할 때 파티션마다 상위 N개의 행만 가져오고 싶은 경우에 유용함.

```sql
-- 전체 결과를 최대 10개로 제한
SELECT * FROM events LIMIT 10;

-- 각 파티션마다 최대 2개의 행만 반환
SELECT * FROM events PER PARTITION LIMIT 2;
```

`integer` 값 대신 바인드 마커(bind marker, `?`) 사용 가능.

---

#### ALLOW FILTERING

기본적으로 CQL은 예측 불가능한 성능을 야기할 수 있는 쿼리를 허용하지 않음. 즉, 명시적인 인덱스나 파티션 키 제약 없이 전체 데이터를 스캔(scan)해야 하는 쿼리는 거부됨.

`ALLOW FILTERING` 키워드를 추가하면 Cassandra가 필요한 데이터를 가져온 뒤 결과를 추가로 필터링(filtering)하도록 허용함. 다만 이 방식은 클러스터 전체를 스캔(full cluster scan)할 수 있어 성능이 크게 저하될 수 있으므로 주의해서 사용해야 함.

```sql
SELECT * FROM users WHERE birth_year = 1981 ALLOW FILTERING;
```

> 경고: `ALLOW FILTERING`은 예측 불가능한 성능을 초래할 수 있으므로 운영 환경에서는 가능한 한 사용을 피하고, 적절한 데이터 모델링이나 인덱스로 해결하는 것을 권장함.

---

#### token 함수

`token` 함수는 파티션 키에 대한 토큰(token) 값을 기반으로 조회를 수행할 수 있게 함. 이를 통해 토큰 범위(token range)에 대한 페이징(paging)이나 순회(iteration) 가능.

```sql
SELECT * FROM posts
 WHERE token(userid) > token('tom') AND token(userid) < token('bob');
```

위 쿼리는 `userid`의 토큰 값이 `'tom'`의 토큰보다 크고 `'bob'`의 토큰보다 작은 모든 행을 조회함. 토큰 순서는 파티셔너(partitioner)가 결정하므로, 결과의 순서는 파티션 키 값의 자연스러운 순서와는 다를 수 있음.

---

### INSERT — 데이터 삽입

`INSERT` 문은 하나의 행(row)에 대해 하나 이상의 컬럼 값을 기록함.

```
insert_statement::= INSERT INTO table_name ( names_values | json_clause )
	[ IF NOT EXISTS ]
	[ USING update_parameter ( AND update_parameter )* ]
names_values::= names VALUES tuple_literal
json_clause::= JSON string [ DEFAULT ( NULL | UNSET ) ]
names::= '(' column_name ( ',' column_name )* ')'
```

예시:

```sql
INSERT INTO NerdMovies (movie, director, main_actor, year)
   VALUES ('Serenity', 'Joss Whedon', 'Nathan Fillion', 2005)
   USING TTL 86400;

INSERT INTO NerdMovies JSON '{"movie": "Serenity", "director": "Joss Whedon", "year": 2005}';
```

`INSERT` 문의 특징은 다음과 같음.

- 전체 기본 키(primary key)를 반드시 지정해야 함
- `IF NOT EXISTS` 조건을 사용하면 해당 행이 존재하지 않을 때만 삽입함. 이 조건은 Paxos 프로토콜을 사용하는 경량 트랜잭션(LWT)이므로 성능에 영향을 줌
- `USING TTL`과 `USING TIMESTAMP` 파라미터 지정 가능
- `VALUES` 대신 `JSON` 구문 사용 가능
- Cassandra의 `INSERT`는 기존 행의 존재 여부를 검사하지 않음 → 동일한 기본 키에 대해 `INSERT`를 수행하면 사실상 업서트(upsert, 행을 생성하거나 덮어쓰기)로 동작함. 따라서 `INSERT`와 `UPDATE`는 의미상 거의 동일하게 동작함

---

### UPDATE — 데이터 수정

`UPDATE` 문은 기존 행의 컬럼들을 수정함.

```
update_statement ::=    UPDATE table_name
                        [ USING update_parameter ( AND update_parameter )* ]
                        SET assignment( ',' assignment )*
                        WHERE where_clause
                        [ IF ( EXISTS | condition ( AND condition)*) ]
update_parameter ::= ( TIMESTAMP | TTL ) ( integer | bind_marker )
assignment: simple_selection'=' term
                | column_name'=' column_name ( '+' | '-' ) term
                | column_name'=' list_literal'+' column_name
simple_selection ::= column_name
                        | column_name '[' term']'
                        | column_name'.' field_name
condition ::= simple_selection operator term
```

예시:

```sql
UPDATE NerdMovies USING TTL 400
   SET director   = 'Joss Whedon',
       main_actor = 'Nathan Fillion',
       year       = 2005
 WHERE movie = 'Serenity';

UPDATE UserActions
   SET total = total + 2
   WHERE user = B70DE1D0-9908-4AE3-BE34-5573E5B09F14
     AND action = 'click';
```

`UPDATE` 문의 특징은 다음과 같음.

- `SET` 절에서 컬럼 값을 할당(assignment)함
- 카운터(counter) 컬럼은 증가/감소 가능(예: `c = c + 3`, `total = total + 2`)
- 컬렉션(list, set, map)과 UDT 필드 수정 가능
- `WHERE` 절로 수정 대상 행의 전체 기본 키를 지정해야 함
- `IF` 조건을 사용하면 조건부 수정(경량 트랜잭션) 가능
- 동일한 파티션(partition) 내에서 수행되는 모든 수정은 원자적으로(atomically) 적용됨

`INSERT`와 마찬가지로 `UPDATE`도 대상 행이 존재하지 않으면 새로 생성하는 업서트(upsert)로 동작함.

---

### DELETE — 데이터 삭제

`DELETE` 문은 행(row) 또는 특정 컬럼을 삭제함.

```
delete_statement::= DELETE [ simple_selection ( ',' simple_selection ) ]
	FROM table_name
	[ USING update_parameter ( AND update_parameter )* ]
	WHERE where_clause
	[ IF ( EXISTS | condition ( AND condition)*) ]
```

예시:

```sql
DELETE FROM NerdMovies USING TIMESTAMP 1240003134
 WHERE movie = 'Serenity';

DELETE phone FROM Users
 WHERE userid IN (C73DE1D3-AF08-40F3-B124-3FF3E5109F22, B70DE1D0-9908-4AE3-BE34-5573E5B09F14);
```

`DELETE` 문의 특징은 다음과 같음.

- 컬럼 이름을 지정하면 해당 컬럼만 삭제하고, 컬럼을 지정하지 않으면 행 전체를 삭제함
- `IN` 연산자를 사용하여 여러 행을 한 번에 삭제 가능
- 클러스터링 컬럼에 대한 부등호 연산자(`<`, `>` 등)를 사용한 범위 삭제(range deletion) 가능
- `IF` 조건을 사용한 조건부 삭제(경량 트랜잭션) 가능
- `USING TIMESTAMP` 파라미터 지정 가능

> 참고: Cassandra에서 삭제는 즉시 데이터를 제거하는 것이 아니라 툼스톤(tombstone)이라는 삭제 표식을 기록하며, 컴팩션(compaction) 과정에서 실제로 제거됨.

---

### BATCH — 일괄 처리

`BATCH` 문은 여러 개의 `INSERT`, `UPDATE`, `DELETE` 연산을 하나로 묶어 처리함.

```
batch_statement ::=     BEGIN [ UNLOGGED | COUNTER ] BATCH
                        [ USING update_parameter( AND update_parameter)* ]
                        modification_statement ( ';' modification_statement )*
                        APPLY BATCH
modification_statement ::= insert_statement | update_statement | delete_statement
```

예시:

```sql
BEGIN BATCH
   INSERT INTO users (userid, password, name) VALUES ('user2', 'ch@ngem3b', 'second user');
   UPDATE users SET password = 'ps22dhds' WHERE userid = 'user3';
   INSERT INTO users (userid, password) VALUES ('user4', 'ch@ngem3c');
   DELETE name FROM users WHERE userid = 'user1';
APPLY BATCH;
```

`BATCH`의 종류와 특징은 다음과 같음.

- 로그드 배치(Logged Batch, 기본값): 별도 키워드 없이 `BEGIN BATCH`로 시작함. 배치에 포함된 모든 문장이 적용되거나 적용되지 않음을 보장하는 원자성(atomicity)을 제공하며, 단일 파티션 내에서는 격리성(isolation)도 보장함. 이를 위해 배치 로그(batch log)를 먼저 기록하므로 추가 오버헤드가 발생함
- 언로그드 배치(`UNLOGGED BATCH`): 배치 로그를 사용하지 않음. 원자성을 보장하지 않으므로, 주로 단일 파티션에 대한 여러 변경을 묶어 네트워크 왕복(round trip)을 줄이는 용도로 사용함
- 카운터 배치(`COUNTER BATCH`): 카운터(counter) 컬럼 업데이트 전용 배치

> 주의: 배치는 성능 최적화 도구가 아님. 여러 파티션에 걸친 대량 작업을 하나의 배치로 묶으면 코디네이터(coordinator) 노드에 부하가 집중되어 오히려 성능이 저하될 수 있음. 배치는 관련된 여러 테이블/행을 원자적으로 갱신해야 할 때 사용하는 것이 적절함.

---

### 경량 트랜잭션(Lightweight Transactions, LWT)

경량 트랜잭션(Lightweight Transaction, LWT)은 조건부 실행(conditional execution)을 제공하여, 특정 조건이 만족될 때만 쓰기 작업을 수행하도록 함. 내부적으로 Paxos 합의 프로토콜(consensus protocol)을 사용하므로 일반적인 쓰기보다 비용이 더 큼.

LWT는 다음과 같은 절을 통해 표현됨.

- `IF NOT EXISTS` — 행이 존재하지 않을 때만 `INSERT`를 수행
- `IF EXISTS` — 행이 존재할 때만 `UPDATE`/`DELETE`를 수행
- `IF <condition>` — 지정한 컬럼 값이 조건을 만족할 때만 작업을 수행

```sql
-- 행이 존재하지 않을 때만 삽입
INSERT INTO users (userid, password)
   VALUES ('user1', 'ch@ngem3a')
   IF NOT EXISTS;

-- 조건을 만족할 때만 수정
UPDATE users
   SET password = 'newpass'
 WHERE userid = 'user1'
   IF password = 'ch@ngem3a';

-- 행이 존재할 때만 삭제
DELETE FROM users
 WHERE userid = 'user1'
   IF EXISTS;
```

LWT를 수행하면 Cassandra는 조건의 만족 여부와, 조건이 만족되지 않은 경우 현재 컬럼 값을 함께 결과로 반환함. 성능에 미치는 영향이 크므로 꼭 필요한 경우에만 사용하는 것이 좋음.

---

### 업데이트 파라미터: TTL과 TIMESTAMP

`INSERT`, `UPDATE`, `DELETE`, `BATCH` 문은 `USING` 절을 통해 업데이트 파라미터(update parameter) 지정 가능.

```
update_parameter ::= ( TIMESTAMP | TTL ) ( integer | bind_marker )
```

#### TTL (Time To Live)

`TTL`은 기록된 값의 수명(초 단위)을 지정함. 지정한 시간이 지나면 해당 값은 자동으로 만료(expire)되어 삭제됨.

```sql
INSERT INTO NerdMovies (movie, director, main_actor, year)
   VALUES ('Serenity', 'Joss Whedon', 'Nathan Fillion', 2005)
   USING TTL 86400;

UPDATE NerdMovies USING TTL 400
   SET director   = 'Joss Whedon',
       main_actor = 'Nathan Fillion',
       year       = 2005
 WHERE movie = 'Serenity';
```

- `USING TTL 86400`은 값이 86400초(24시간) 후에 만료됨을 의미함
- `TTL 0`을 지정하면 TTL이 없는 것과 동일하게 동작함
- 컬럼의 남은 TTL은 `TTL(column_name)` 함수로 조회 가능

#### TIMESTAMP

`TIMESTAMP`는 작업의 타임스탬프(마이크로초 단위)를 명시적으로 지정함. Cassandra는 동일한 컬럼에 대한 여러 쓰기 중 타임스탬프가 가장 큰 값을 최종 값으로 결정하는 LWW(Last-Write-Wins) 방식을 사용하므로, 타임스탬프는 충돌 해결에 중요한 역할을 함.

```sql
DELETE FROM NerdMovies USING TIMESTAMP 1240003134
 WHERE movie = 'Serenity';
```

`TIMESTAMP`를 지정하지 않으면 코디네이터 노드의 현재 시각이 자동으로 사용됨.

---

### 연산자(Operators)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/operators.html

CQL은 산술 연산(arithmetic operation)을 지원함. 모든 산술 연산은 숫자(numeric) 타입 또는 카운터(counter)에 대해 수행 가능.

#### 산술 연산자(Arithmetic Operators)

CQL은 다음 연산자를 지원함.

- `-` (단항, unary): 피연산자의 부호를 반전(negation)
- `+`: 덧셈(Addition)
- `-`: 뺄셈(Subtraction)
- `*`: 곱셈(Multiplication)
- `/`: 나눗셈(Division)
- `%`: 나눗셈의 나머지(remainder)를 반환

#### 숫자 연산(Number Arithmetic)

모든 산술 연산은 숫자 타입과 카운터에 대해 동작함. 산술 표현식의 반환 타입(return type)은 피연산자(operand)의 타입에 따라 다음 규칙에 의해 결정됨. 각 항목은 `왼쪽 피연산자 타입 × 오른쪽 피연산자 타입 → 결과 타입`으로 정리함.

- `tinyint`:
  - `× tinyint → tinyint`
  - `× smallint → smallint`
  - `× int → int`
  - `× bigint → bigint`
  - `× counter → bigint`
  - `× float → float`
  - `× double → double`
  - `× varint → varint`
  - `× decimal → decimal`
- `smallint`:
  - `× tinyint → smallint`
  - `× smallint → smallint`
  - `× int → int`
  - `× bigint → bigint`
  - `× counter → bigint`
  - `× float → float`
  - `× double → double`
  - `× varint → varint`
  - `× decimal → decimal`
- `int`:
  - `× tinyint → int`
  - `× smallint → int`
  - `× int → int`
  - `× bigint → bigint`
  - `× counter → bigint`
  - `× float → float`
  - `× double → double`
  - `× varint → varint`
  - `× decimal → decimal`
- `bigint`:
  - `× tinyint → bigint`
  - `× smallint → bigint`
  - `× int → bigint`
  - `× bigint → bigint`
  - `× counter → bigint`
  - `× float → double`
  - `× double → double`
  - `× varint → varint`
  - `× decimal → decimal`
- `counter`:
  - `× tinyint → bigint`
  - `× smallint → bigint`
  - `× int → bigint`
  - `× bigint → bigint`
  - `× counter → bigint`
  - `× float → double`
  - `× double → double`
  - `× varint → varint`
  - `× decimal → decimal`
- `float`:
  - `× tinyint → float`
  - `× smallint → float`
  - `× int → float`
  - `× bigint → double`
  - `× counter → double`
  - `× float → float`
  - `× double → double`
  - `× varint → decimal`
  - `× decimal → decimal`
- `double`:
  - `× tinyint → double`
  - `× smallint → double`
  - `× int → double`
  - `× bigint → double`
  - `× counter → double`
  - `× float → double`
  - `× double → double`
  - `× varint → decimal`
  - `× decimal → decimal`
- `varint`:
  - `× tinyint → varint`
  - `× smallint → varint`
  - `× int → varint`
  - `× bigint → decimal`
  - `× counter → decimal`
  - `× float → decimal`
  - `× double → decimal`
  - `× varint → decimal`
  - `× decimal → decimal`
- `decimal`:
  - `× tinyint → decimal`
  - `× smallint → decimal`
  - `× int → decimal`
  - `× bigint → decimal`
  - `× counter → decimal`
  - `× float → decimal`
  - `× double → decimal`
  - `× varint → decimal`
  - `× decimal → decimal`

예를 들어, `tinyint`와 `smallint`의 연산 결과는 `smallint`가 되며, `bigint`와 `float` 또는 `double`을 연산하면 `double`이 반환됨.

#### 연산자 우선순위(Precedence)

`*`, `/`, `%` 연산자는 `+`, `-` 연산자보다 높은 우선순위(precedence level)를 가짐.

표현식 내에서 두 연산자가 동일한 우선순위를 가지는 경우, 표현식 내 위치에 따라 왼쪽에서 오른쪽으로(left to right) 평가됨.

#### 날짜/시간 연산(Datetime Arithmetic)

기간(duration) 값을 타임스탬프(timestamp)나 날짜(date)에 더하거나(`+`) 빼서(`-`) 새로운 타임스탬프 또는 날짜 생성 가능.

```sql
SELECT * FROM myTable WHERE t = '2017-01-01' - 2d;
```

위 쿼리는 날짜 값 `'2017-01-01'`에서 기간 `2d`(2일)를 빼서, 결과적으로 "2016년의 마지막 2일(the last 2 days of 2016)"에 해당하는 타임스탬프 `t`를 가진 레코드를 조회함.

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL DML](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html)
- [CQL Operators](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/operators.html)

---

## CQL 보조 인덱스와 구체화된 뷰

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/

---

### 목차

1. [보조 인덱스(Secondary Indexes)](#보조-인덱스secondary-indexes)
   - [CREATE INDEX](#create-index)
   - [인덱스 타입(Index Types)](#인덱스-타입index-types)
   - [컬렉션 인덱싱(Collection Indexing)](#컬렉션-인덱싱collection-indexing)
   - [DROP INDEX](#drop-index)
   - [보조 인덱스 사용 시 주의점](#보조-인덱스-사용-시-주의점)
2. [구체화된 뷰(Materialized Views)](#구체화된-뷰materialized-views)
   - [CREATE MATERIALIZED VIEW](#create-materialized-view)
   - [SELECT 문 제약](#select-문-제약)
   - [기본 키(Primary Key) 제약](#기본-키primary-key-제약)
   - [ALTER MATERIALIZED VIEW](#alter-materialized-view)
   - [DROP MATERIALIZED VIEW](#drop-materialized-view)
   - [구체화된 뷰의 한계와 주의점](#구체화된-뷰의-한계와-주의점)
3. [SASI 인덱스(SSTable Attached Secondary Index)](#sasi-인덱스sstable-attached-secondary-index)
   - [SASI 개요](#sasi-개요)
   - [SASI 인덱스 생성](#sasi-인덱스-생성)
   - [인덱스 모드(Index Modes)](#인덱스-모드index-modes)
   - [분석기(Analyzer)와 토큰화 옵션](#분석기analyzer와-토큰화-옵션)
   - [질의(Query) 예시](#질의query-예시)
   - [구현 세부사항과 한계](#구현-세부사항과-한계)
4. [참고 자료](#참고-자료)

---

### 보조 인덱스(Secondary Indexes)

CQL은 테이블의 컬럼에 보조 인덱스(secondary index)를 만드는 기능을 제공함. 보조 인덱스를 사용하면 기본 키(primary key) 이외의 컬럼을 조건으로 질의(query) 가능. 인덱스 이름(index name)은 `[a-zA-Z_0-9]+` 패턴을 따름.

#### CREATE INDEX

`CREATE INDEX` 문은 테이블의 단일 컬럼에 대해 새로운 보조 인덱스(2i, secondary index) 또는 스토리지 연결 인덱스(SAI, storage-attached index)를 정의함.

문법은 다음과 같음.

```
CREATE [ CUSTOM ] INDEX [ IF NOT EXISTS ] [ index_name ]
    ON table_name '(' index_identifier ')'
    [ USING index_type [ WITH OPTIONS = map_literal ] ]
```

`index_identifier`는 다음 중 하나임.

- 컬럼 이름(column name)
- 컬렉션(collection)을 위한 `KEYS(column_name)`, `VALUES(column_name)`, `ENTRIES(column_name)`, `FULL(column_name)`

인덱스 생성 예시는 다음과 같음.

```cql
CREATE INDEX userIndex ON NerdMovies (user);
CREATE INDEX ON users (KEYS(favs));
CREATE INDEX ON users (age) USING 'sai';
CREATE CUSTOM INDEX ON users (email) USING 'path.to.the.IndexClass'
    WITH OPTIONS = {'storage': '/mnt/ssd/indexes/'};
```

주요 동작은 다음과 같음.

- `CREATE INDEX` 문을 실행하면, 컬럼에 이미 데이터가 들어 있는 경우 해당 데이터는 비동기적으로 인덱싱됨. 인덱스가 생성된 후에는 해당 컬럼의 데이터가 변경될 때마다 인덱스가 자동으로 갱신됨
- `IF NOT EXISTS`를 지정하면 같은 이름의 인덱스가 이미 존재할 때 오류를 발생시키지 않고 명령을 무시함
- `CUSTOM` 키워드를 사용하는 커스텀 인덱스(custom index)는 인덱스를 구현하는 클래스의 완전한 이름(fully qualified class name)을 요구함

#### 인덱스 타입(Index Types)

Cassandra는 두 가지 내장(built-in) 인덱스 타입을 제공함.

- `legacy_local_table` (기본값): 기존(legacy)의 보조 인덱스로, 숨겨진 로컬 테이블(hidden local table)로 구현됨
- `sai`: "스토리지 연결(storage-attached)" 인덱스로, SSTable 및 Memtable에 연결되어 최적화된 형태로 구현됨

`USING` 절을 통해 인덱스 타입을 명시적으로 지정할 수 있으며, `WITH OPTIONS` 절로 인덱스 설정 제어 가능.

#### 컬렉션 인덱싱(Collection Indexing)

맵(map) 컬럼에 대해서는 인덱싱 대상 세분화 가능.

- `KEYS()`는 맵의 키(key)를 인덱싱하여 `CONTAINS KEY` 질의를 가능하게 함
- 기본 동작은 맵의 값(value)을 인덱싱함
- `ENTRIES()`는 맵의 항목(키-값 쌍)을 인덱싱함
- `FULL()`은 동결된(frozen) 컬렉션 전체를 인덱싱함

#### DROP INDEX

`DROP INDEX` 문은 기존 보조 인덱스를 제거함.

```
DROP INDEX [ IF EXISTS ] index_name
```

`IF EXISTS`를 지정하면 해당 인덱스가 존재하지 않더라도 오류 없이 안전하게 명령이 수행됨.

#### 보조 인덱스 사용 시 주의점

보조 인덱스는 기본 키가 아닌 컬럼으로의 질의를 가능하게 해 주지만, 동작 원리상 다음과 같은 점을 유의해야 함.

- 보조 인덱스(2i)는 각 노드에 로컬(local)로 구성되므로, 인덱스 컬럼만으로 질의하면 코디네이터(coordinator)가 모든 노드에 요청을 분산해야 할 수 있어 비용이 높음
- 카디널리티(cardinality)가 매우 높거나(거의 유일한 값들) 매우 낮은(소수의 값에 매우 많은 로우가 몰리는) 컬럼은 보조 인덱스에 적합하지 않음
- 자주 갱신되거나 삭제되는 컬럼은 인덱스 유지 비용이 커짐
- 가능한 한 파티션 키(partition key)를 함께 지정하여 질의 범위를 좁히는 것이 좋음

---

### 구체화된 뷰(Materialized Views)

구체화된 뷰(materialized view)는 기반 테이블(base table)로부터 생성되는 데이터베이스 객체로, 데이터가 자동으로 동기화되어 유지됨. 구체화된 뷰는 직접 갱신할 수 없으며, 대신 기반 테이블에 가해진 변경 사항이 해당 뷰로 자동으로 전파됨.

#### CREATE MATERIALIZED VIEW

문법은 다음과 같음.

```
create_materialized_view_statement::= CREATE MATERIALIZED VIEW [ IF NOT EXISTS ] view_name
    AS select_statement
    PRIMARY KEY '(' primary_key ')'
    WITH table_options
```

예시:

```cql
CREATE MATERIALIZED VIEW monkeySpecies_by_population AS
   SELECT * FROM monkeySpecies
   WHERE population IS NOT NULL AND species IS NOT NULL
   PRIMARY KEY (population, species)
   WITH comment='Allow query by population instead of species';
```

`IF NOT EXISTS`를 지정하면 같은 이름의 뷰가 이미 존재하더라도 오류를 발생시키지 않음.

#### SELECT 문 제약

구체화된 뷰의 `SELECT` 문에는 다음과 같은 중요한 제약이 있음.

- 선택 절(selection clause): 기반 테이블의 컬럼만 선택 가능. 함수(function), 형 변환(casting), 별칭(alias), 정적 컬럼(static column)은 사용 불가. 기반 테이블에 정적 컬럼이 포함되어 있으면 와일드카드 선택(`SELECT *`) 사용 불가
- WHERE 절 요구사항:
  - 바인드 마커(bind marker)는 허용되지 않음
  - 기본 키가 아닌 컬럼은 `IS NOT NULL` 제약을 가져야 함
  - 뷰의 기본 키 컬럼은 최소한 `IS NOT NULL` 제약을 포함해야 함
  - 정렬 절(ordering clause), 제한(limit), `ALLOW FILTERING`은 사용 불가

#### 기본 키(Primary Key) 제약

뷰의 기본 키는 다음 조건을 만족해야 함.

- 기반 테이블의 모든 기본 키 컬럼을 포함해야 함
- 기반 테이블의 기본 키가 아닌 컬럼을 최대 하나만 추가로 포함 가능

다음은 기본 키가 아닌 컬럼을 하나만 추가할 수 있다는 규칙을 위반하는 잘못된 예시임(아래는 `v1`, `v2` 두 개의 비기본키 컬럼을 사용하여 규칙을 위반함).

```cql
CREATE MATERIALIZED VIEW mv1 AS
   SELECT * FROM t
   WHERE k IS NOT NULL AND c1 IS NOT NULL AND c2 IS NOT NULL AND v1 IS NOT NULL
   PRIMARY KEY (v1, v2, k, c1, c2);
```

#### ALTER MATERIALIZED VIEW

```
alter_materialized_view_statement::= ALTER MATERIALIZED VIEW [ IF EXISTS ] view_name WITH table_options
```

수정 가능한 옵션은 뷰 생성 시 사용할 수 있는 옵션과 동일함.

#### DROP MATERIALIZED VIEW

```
drop_materialized_view_statement::= DROP MATERIALIZED VIEW [ IF EXISTS ] view_name;
```

`IF EXISTS`를 지정하면 해당 뷰가 존재하지 않더라도 오류 없이 안전하게 수행됨.

#### 구체화된 뷰의 한계와 주의점

- 선택되지 않은 기반 컬럼의 삭제 문제: 뷰에 선택되지 않은 기반 테이블 컬럼을 삭제하는 경우, 힌트(hint)나 리페어(repair)를 통해 수신된 다른 컬럼들의 갱신을 놓칠 수 있음("missed updates to other columns received by hints or repair"). 이 문제(CASSANDRA-13826)가 해결되기 전까지는 이러한 삭제를 신중히 다룰 것을 권장함
- 성능 설정: 구체화된 뷰의 초기 구성(build)은 기본적으로 단일 스레드(single-threaded)로 동작함. `cassandra.yaml`의 `concurrent_materialized_view_builders` 속성을 통해 병렬화 가능
- 구체화된 뷰는 직접 쓰기(write)할 수 없으며, 기반 테이블을 통해서만 데이터가 갱신됨

---

### SASI 인덱스(SSTable Attached Secondary Index)

#### SASI 개요

SASI(SSTable Attached Secondary Index)는 Cassandra의 `Index` 인터페이스를 구현한 것으로, 기존 인덱싱 구현의 대안으로 사용 가능. SASI의 인덱싱과 질의는 Cassandra의 요구사항에 맞춰 특화되어 기존 구현보다 향상된 성능을 제공함.

주요 장점은 다음과 같음.

- 이전에는 필터링(filtering)이 필요했던 질의에서 뛰어난 성능을 제공함
- 메모리·디스크·CPU 사용량 측면에서 기존 구현보다 자원 소모가 현저히 적음("significantly less resource intensive")
- 문자열에 대한 접두사(prefix) 및 부분 문자열(contains) 질의를 지원함(SQL의 `LIKE = "foo*"` 또는 `LIKE = "*foo*"`와 유사)

SASI라는 이름은 Cassandra의 한 번 쓰고(write-once) 변경 불가능하며(immutable) 정렬된(ordered) 데이터 모델을 활용하여, Memtable을 디스크로 플러시(flush)할 때 인덱스를 함께 구성한다는 점에서 비롯됨("SSTable Attached Secondary Index").

#### SASI 인덱스 생성

SASI 인덱스는 `CREATE CUSTOM INDEX` 문으로 생성함.

```cql
CREATE CUSTOM INDEX ON table_name (column_name)
USING 'org.apache.cassandra.index.sasi.SASIIndex'
WITH OPTIONS = { option_key: 'option_value' };
```

전체 예시:

```cql
CREATE TABLE sasi (
  id uuid,
  first_name text,
  last_name text,
  age int,
  height int,
  created_at bigint,
  primary key (id)
);
```

```cql
CREATE CUSTOM INDEX ON sasi (first_name)
USING 'org.apache.cassandra.index.sasi.SASIIndex'
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.NonTokenizingAnalyzer',
  'case_sensitive': 'false'
};

CREATE CUSTOM INDEX ON sasi (last_name)
USING 'org.apache.cassandra.index.sasi.SASIIndex'
WITH OPTIONS = {'mode': 'CONTAINS'};

CREATE CUSTOM INDEX ON sasi (age)
USING 'org.apache.cassandra.index.sasi.SASIIndex';

CREATE CUSTOM INDEX ON sasi (created_at)
USING 'org.apache.cassandra.index.sasi.SASIIndex'
WITH OPTIONS = {'mode': 'SPARSE'};
```

#### 인덱스 모드(Index Modes)

SASI는 컬럼별로 설정할 수 있는 세 가지 인덱싱 모드를 지원함.

- PREFIX (기본값): 각 항목(term)의 정확한 값을 `OnDiskIndex`마다 정확히 한 번씩 기록함. 예를 들어 `Jason`, `Jordan`, `Pavel`이라는 항목이 있으면 세 값 모두 그대로 인덱스에 저장되어, `WHERE first_name LIKE 'M%'`와 같은 접두사 기반 질의가 가능함
- CONTAINS: 각 항목의 모든 접미사(suffix)를 재귀적으로 추가로 기록함. 위 예시를 사용하면 인덱스는 `ason`, `ordan`, `avel`, `son`, `rdan`, `vel` 등을 추가로 저장함 → 문자열의 부분 문자열에 대한 질의(`WHERE last_name LIKE '%a%'`)가 가능해짐
- SPARSE: 예를 들어 타임스탬프(timestamp)처럼 큰 범위의 값을 효율적으로 순회하기 위해 설계됨. PREFIX 모드와 달리, 항목 64개 블록마다 각 항목의 `TokenTree`를 하나로 병합한 단일 `TokenTree`를 구성함. 많은 값이 밀집(dense)되어 있는 수치 범위에 최적화된 방식임

> 주의: SPARSE 모드는 동일한 값에 매우 많은 로우가 연결되는 경우(즉, 항목 하나당 너무 많은 토큰이 매핑되는 경우)에는 적합하지 않음. 이 모드는 항목당 매핑되는 값(토큰)이 적고, 항목의 종류가 매우 많은(밀집된 수치 범위) 데이터에 사용하도록 설계됨.

#### 분석기(Analyzer)와 토큰화 옵션

NonTokenizingAnalyzer: 텍스트에 대해 분석을 수행하지 않고 정확한 값을 보존함.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.NonTokenizingAnalyzer',
  'case_sensitive': 'false'
}
```

StandardAnalyzer: 토큰화(tokenization)와 어간 추출(stemming)을 지원함.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.StandardAnalyzer',
  'tokenization_enable_stemming': 'true',
  'analyzed': 'true',
  'tokenization_normalize_lowercase': 'true',
  'tokenization_locale': 'en'
}
```

DelimiterAnalyzer: 구분자(delimiter)를 기준으로 텍스트를 토큰화함.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.DelimiterAnalyzer',
  'delimiter': ',',
  'mode': 'prefix',
  'analyzed': 'true'
}
```

주요 공통 옵션은 다음과 같음.

- `analyzer_class`: 사용할 텍스트 분석 전략(분석기 클래스) 지정
- `case_sensitive`: 대소문자 구분 여부 제어(`true`/`false`)
- `mode`: 인덱스 모드 선택(`PREFIX`/`CONTAINS`/`SPARSE`)
- `analyzed`: 텍스트 분석(토큰화) 처리 활성화(`true`/`false`)
- `delimiter`: `DelimiterAnalyzer`에서 사용할 구분 문자
- `tokenization_enable_stemming`: 단어 어간 추출(stemming) 활성화
- `tokenization_normalize_lowercase`: 소문자 정규화(lowercase normalization) 수행
- `tokenization_locale`: 분석에 사용할 언어(로캘) 지정
- `is_literal`: 컬럼을 리터럴(literal) 타입으로 설정하여 트라이(trie) 기반 인덱싱 사용

텍스트 분석 기능: `StandardAnalyzer`는 언어적 처리를 지원함.

- 어간 추출(Stemming): 단어를 어근 형태로 변환함. 예를 들어 "distributing", "argued", "working"은 각각 "distribution", "argue", "work"와 매칭됨
- 토큰화(Tokenization): 텍스트를 검색 가능한 항목들로 분리함
- 로캘별 처리: `tokenization_locale`를 통해 언어별 정규화를 지원함

#### 질의(Query) 예시

동등(equality) 및 접두사(prefix) 질의:

```cql
SELECT * FROM table WHERE column_name = 'value';
SELECT * FROM table WHERE column_name LIKE 'M%';
```

부분 문자열(CONTAINS) 질의:

```cql
SELECT * FROM table WHERE column_name LIKE '%substring%';
```

복합(compound) 질의:

```cql
SELECT * FROM table
WHERE first_column LIKE 'M%' AND second_column < 30
ALLOW FILTERING;
```

비인덱스 컬럼 필터링:

```cql
SELECT * FROM table
WHERE indexed_column LIKE '%value%' AND non_indexed_column >= 175
ALLOW FILTERING;
```

#### 구현 세부사항과 한계

핵심 아키텍처: SASI는 Cassandra의 한 번 쓰고 변경 불가능하며 정렬된 데이터 모델을 활용하여, Memtable을 디스크로 플러시할 때 인덱스를 함께 구성함. "SSTable Attached Secondary Index"라는 이름의 유래임.

자료 구조(Data Structures):

- 디스크 상의 인덱싱에는 수정된 접미사 배열(Suffix Array) 자료 구조를 사용함
- 토큰 관리에는 잘 알려진 B+ 트리(B+ tree)의 구현을 사용함
- 메모리 상에서는 리터럴 타입에 대해 `TrieMemIndex`를, 수치 타입에 대해 `SkipListMemIndex`를 사용함

질의 처리(Query Processing): 검색 질의마다 인스턴스화되는 `QueryPlan`이 SASI 질의 구현의 핵심임. Cassandra의 내부 표현인 `IndexExpression`들을 연산 트리(operation tree)로 변환해 질의를 최적화한 뒤, `DecoratedKey`를 생성하는 이터레이터(iterator)를 통해 실행함.

한계(Limitations):

1. 파티셔너(Partitioner) 요구사항: 클러스터는 `LongToken`을 생성하는 파티셔너(예: `Murmur3Partitioner`)를 사용하도록 구성되어야 함. `LongToken`을 생성하지 않는 다른 파티셔너(예: `ByteOrderedPartitioner`, `RandomPartitioner`)는 SASI와 함께 동작하지 않음
2. 제거된 기능: Cassandra 자체의 변경이 이루어지는 동안, 이번 릴리스에서는 부등(Not Equals, `!=`)과 OR 지원이 제거됨

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL Secondary Indexes](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/indexes.html)
- [CQL Materialized Views](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/mvs.html)
- [SASI Index](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/SASI.html)
- [CREATE INDEX (CQL Reference)](https://cassandra.apache.org/doc/latest/cassandra/reference/cql-commands/create-index.html)
