# CQL 데이터 조작 (DML)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html

---

## 목차

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

## 개요

CQL(Cassandra Query Language)은 데이터를 삽입(insert), 수정(update), 삭제(delete), 조회(query)하기 위한 여러 문장(statement)을 제공합니다. 이 문서에서 다루는 데이터 조작(Data Manipulation, DML) 문장은 다음과 같습니다.

- `SELECT` — 데이터 조회
- `INSERT` — 데이터 삽입
- `UPDATE` — 데이터 수정
- `DELETE` — 데이터 삭제
- `BATCH` — 여러 변경 문장을 묶어서 처리

중요한 제약 사항으로, CQL은 조인(join)이나 서브쿼리(sub-query)를 실행하지 않으며, 조회는 항상 단일 테이블(single table)에 대해서만 수행됩니다.

---

## SELECT — 데이터 조회

`SELECT` 문은 하나의 Cassandra 테이블에서 데이터를 조회(query)합니다. `SELECT` 문은 최소한 하나의 선택 절(selection clause)과 조회 대상 테이블의 이름을 포함합니다.

문법(grammar)은 다음과 같습니다.

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

위 예시에서 알 수 있듯이 `SELECT` 문은 다음을 포함할 수 있습니다.

- 선택 절(`select_clause`): 조회할 컬럼이나 표현식을 지정합니다.
- `WHERE` 절: 조회 결과를 좁히는 조건을 지정합니다.
- `ORDER BY` / `LIMIT` 등의 부가 절: 결과를 정렬하거나 제한합니다.

---

### 선택 절(Selection Clause)

선택 절은 어떤 결과를 반환할지를 결정합니다. 선택 절은 셀렉터(selector) 또는 와일드카드 `*`(전체 컬럼 반환)로 구성됩니다.

셀렉터(selector)는 다음 중 하나일 수 있습니다.

- `column_name` — 테이블에 정의된 컬럼 이름. 해당 컬럼의 값을 선택합니다.
- `term` — 리터럴 값(literal). 모든 결과 행에 대해 해당 값을 반환합니다.
- `CAST` — 어떤 셀렉터의 결과를 지정한 CQL 타입으로 변환합니다.
- `function_name(...)` — 함수에 다른 셀렉터를 인자로 적용한 결과입니다. 기본 함수에는 집계 함수(aggregate)도 포함됩니다.
- `COUNT(*)` — 조회된 행의 개수를 셉니다.

`DISTINCT` 키워드를 사용하면 파티션 키(partition key)의 정적(static) 부분만 조회하면서 중복을 제거할 수 있습니다.

---

### 별칭(Aliases)

`AS` 키워드를 사용하면 셀렉터에 별칭(alias)을 부여하여 결과 집합의 컬럼 이름을 변경할 수 있습니다.

```sql
-- 컬럼 이름 변경
SELECT name AS user_name, occupation AS user_occupation FROM users;

-- 함수 결과에 별칭 부여
SELECT COUNT (*) AS user_count FROM users;
```

---

### WRITETIME, MAXWRITETIME, TTL 함수

특정 컬럼에 대한 메타데이터를 조회하기 위해 다음 특수 함수를 사용할 수 있습니다.

```sql
WRITETIME(column_name)
MAXWRITETIME(column_name)
TTL(column_name)
WRITETIME(phones[2..4])
WRITETIME(user.name)
```

- `WRITETIME(column_name)` — 해당 컬럼 값이 기록된 시점의 타임스탬프(timestamp, 마이크로초 단위)를 반환합니다.
- `MAXWRITETIME(column_name)` — 해당 컬럼에 대한 가장 최근의 쓰기 타임스탬프를 반환합니다.
- `TTL(column_name)` — 해당 컬럼 값이 만료되기까지 남은 시간(초 단위, Time To Live)을 반환합니다.

이 함수들은 컬렉션(collection)의 일부 요소(`phones[2..4]`)나 UDT(User-Defined Type)의 필드(`user.name`)에도 적용할 수 있습니다.

---

### WHERE 절

`WHERE` 절은 조회 대상 행(row)을 좁히는 조건을 지정합니다. 조건은 관계식(relation)들을 `AND`로 연결하여 표현합니다.

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

WHERE 절 사용에 대한 규칙은 다음과 같습니다.

- 파티션 키(partition key)의 컬럼들은 `=` 또는 `IN` 연산자로 제한해야 합니다.
- 클러스터링(clustering) 컬럼에는 범위(`<`, `>`, `<=`, `>=`) 제약을 사용할 수 있습니다.
- 인덱스(index)가 있는 컬럼에 대해서도 조건을 줄 수 있습니다.

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

### GROUP BY 절

`GROUP BY` 절은 동일한 값을 가진 행들을 하나의 그룹으로 묶어(condense) 처리합니다. 그룹화는 파티션 키(partition key)와 클러스터링 컬럼(clustering column)으로만 수행할 수 있습니다.

```
group_by_clause::= column_name ( ',' column_name )*
```

`GROUP BY`는 일반적으로 집계 함수(aggregate function, 예: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`)와 함께 사용되어 각 그룹별 집계 결과를 얻습니다.

---

### ORDER BY 절

`ORDER BY` 절은 클러스터링 순서(clustering order) 정의에 따라 결과를 정렬합니다.

```
ordering_clause::= column_name [ ASC | DESC ] ( ',' column_name [ ASC | DESC ] )*
```

- `ASC` — 오름차순(ascending)
- `DESC` — 내림차순(descending)

정렬은 클러스터링 컬럼을 기준으로만 가능하며, 테이블 정의 시 지정된 클러스터링 순서를 따르거나 그 역순으로만 정렬할 수 있습니다.

---

### PER PARTITION LIMIT와 LIMIT

`LIMIT` 절은 쿼리가 반환하는 전체 행의 개수를 제한합니다.

`PER PARTITION LIMIT` 절은 각 파티션(partition)별로 반환되는 행의 개수를 제한합니다. 이는 여러 파티션에 걸쳐 조회할 때 파티션마다 상위 N개의 행만 가져오고 싶은 경우에 유용합니다.

```sql
-- 전체 결과를 최대 10개로 제한
SELECT * FROM events LIMIT 10;

-- 각 파티션마다 최대 2개의 행만 반환
SELECT * FROM events PER PARTITION LIMIT 2;
```

`integer` 값 대신 바인드 마커(bind marker, `?`)를 사용할 수도 있습니다.

---

### ALLOW FILTERING

기본적으로 CQL은 예측 불가능한 성능을 야기할 수 있는 쿼리를 허용하지 않습니다. 즉, 명시적인 인덱스나 파티션 키 제약 없이 전체 데이터를 스캔(scan)해야 하는 쿼리는 거부됩니다.

`ALLOW FILTERING` 키워드를 추가하면 Cassandra가 필요한 데이터를 가져온 뒤 결과를 추가로 필터링(filtering)하도록 허용합니다. 그러나 이 방식은 클러스터 전체를 스캔(full cluster scan)할 수 있어 성능이 크게 저하될 수 있으므로 주의해서 사용해야 합니다.

```sql
SELECT * FROM users WHERE birth_year = 1981 ALLOW FILTERING;
```

> 경고: `ALLOW FILTERING`은 예측 불가능한 성능을 초래할 수 있으므로 운영 환경에서는 가능한 한 사용을 피하고, 적절한 데이터 모델링이나 인덱스를 통해 해결하는 것이 권장됩니다.

---

### token 함수

`token` 함수는 파티션 키에 대한 토큰(token) 값을 기반으로 조회를 수행할 수 있게 합니다. 이를 통해 토큰 범위(token range)에 대한 페이징(paging)이나 순회(iteration)가 가능합니다.

```sql
SELECT * FROM posts
 WHERE token(userid) > token('tom') AND token(userid) < token('bob');
```

위 쿼리는 `userid`의 토큰 값이 `'tom'`의 토큰보다 크고 `'bob'`의 토큰보다 작은 모든 행을 조회합니다. 토큰 순서는 파티셔너(partitioner)가 결정하므로, 결과의 순서는 파티션 키 값의 자연스러운 순서와는 다를 수 있습니다.

---

## INSERT — 데이터 삽입

`INSERT` 문은 하나의 행(row)에 대해 하나 이상의 컬럼 값을 기록합니다.

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

`INSERT` 문의 특징은 다음과 같습니다.

- 전체 기본 키(primary key)를 반드시 지정해야 합니다.
- `IF NOT EXISTS` 조건을 사용하면 해당 행이 존재하지 않을 때만 삽입합니다. 이 조건은 Paxos 프로토콜을 사용하는 경량 트랜잭션(LWT)이므로 성능에 영향을 줍니다.
- `USING TTL`과 `USING TIMESTAMP` 파라미터를 지정할 수 있습니다.
- `VALUES` 대신 `JSON` 구문을 사용할 수 있습니다.
- Cassandra의 `INSERT`는 기존 행의 존재 여부를 검사하지 않으므로, 동일한 기본 키에 대해 `INSERT`를 수행하면 사실상 업서트(upsert, 행을 생성하거나 덮어쓰기)로 동작합니다. 따라서 `INSERT`와 `UPDATE`는 의미상 거의 동일하게 동작합니다.

---

## UPDATE — 데이터 수정

`UPDATE` 문은 기존 행의 컬럼들을 수정합니다.

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

`UPDATE` 문의 특징은 다음과 같습니다.

- `SET` 절에서 컬럼 값을 할당(assignment)합니다.
- 카운터(counter) 컬럼은 증가/감소시킬 수 있습니다(예: `c = c + 3`, `total = total + 2`).
- 컬렉션(list, set, map)과 UDT 필드를 수정할 수 있습니다.
- `WHERE` 절로 수정 대상 행의 전체 기본 키를 지정해야 합니다.
- `IF` 조건을 사용하면 조건부 수정(경량 트랜잭션)을 수행할 수 있습니다.
- 동일한 파티션(partition) 내에서 수행되는 모든 수정은 원자적으로(atomically) 적용됩니다.

`INSERT`와 마찬가지로 `UPDATE`도 대상 행이 존재하지 않으면 새로 생성하는 업서트(upsert)로 동작합니다.

---

## DELETE — 데이터 삭제

`DELETE` 문은 행(row) 또는 특정 컬럼을 삭제합니다.

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

`DELETE` 문의 특징은 다음과 같습니다.

- 컬럼 이름을 지정하면 해당 컬럼만 삭제하고, 컬럼을 지정하지 않으면 행 전체를 삭제합니다.
- `IN` 연산자를 사용하여 여러 행을 한 번에 삭제할 수 있습니다.
- 클러스터링 컬럼에 대한 부등호 연산자(`<`, `>` 등)를 사용한 범위 삭제(range deletion)가 가능합니다.
- `IF` 조건을 사용한 조건부 삭제(경량 트랜잭션)가 가능합니다.
- `USING TIMESTAMP` 파라미터를 지정할 수 있습니다.

> 참고: Cassandra에서 삭제는 즉시 데이터를 제거하는 것이 아니라 툼스톤(tombstone)이라는 삭제 표식을 기록하며, 컴팩션(compaction) 과정에서 실제로 제거됩니다.

---

## BATCH — 일괄 처리

`BATCH` 문은 여러 개의 `INSERT`, `UPDATE`, `DELETE` 연산을 하나로 묶어 처리합니다.

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

`BATCH`의 종류와 특징은 다음과 같습니다.

- **로그드 배치(Logged Batch, 기본값)**: 별도 키워드 없이 `BEGIN BATCH`로 시작합니다. 배치에 포함된 모든 문장이 적용되거나 적용되지 않음을 보장하는 원자성(atomicity)을 제공하며, 단일 파티션 내에서는 격리성(isolation)도 보장합니다. 이를 위해 배치 로그(batch log)를 먼저 기록하므로 추가 오버헤드가 발생합니다.
- **언로그드 배치(`UNLOGGED BATCH`)**: 배치 로그를 사용하지 않습니다. 원자성을 보장하지 않으므로, 주로 단일 파티션에 대한 여러 변경을 묶어 네트워크 왕복(round trip)을 줄이는 용도로 사용합니다.
- **카운터 배치(`COUNTER BATCH`)**: 카운터(counter) 컬럼 업데이트 전용 배치입니다.

> 주의: 배치는 성능 최적화 도구가 아닙니다. 여러 파티션에 걸친 대량 작업을 하나의 배치로 묶으면 코디네이터(coordinator) 노드에 부하가 집중되어 오히려 성능이 저하될 수 있습니다. 배치는 관련된 여러 테이블/행을 원자적으로 갱신해야 할 때 사용하는 것이 적절합니다.

---

## 경량 트랜잭션(Lightweight Transactions, LWT)

경량 트랜잭션(Lightweight Transaction, LWT)은 조건부 실행(conditional execution)을 제공하여, 특정 조건이 만족될 때만 쓰기 작업을 수행하도록 합니다. 내부적으로 Paxos 합의 프로토콜(consensus protocol)을 사용하므로 일반적인 쓰기보다 비용이 더 큽니다.

LWT는 다음과 같은 절을 통해 표현됩니다.

- `IF NOT EXISTS` — 행이 존재하지 않을 때만 `INSERT`를 수행합니다.
- `IF EXISTS` — 행이 존재할 때만 `UPDATE`/`DELETE`를 수행합니다.
- `IF <condition>` — 지정한 컬럼 값이 조건을 만족할 때만 작업을 수행합니다.

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

LWT를 수행하면 Cassandra는 조건의 만족 여부와, 조건이 만족되지 않은 경우 현재 컬럼 값을 함께 결과로 반환합니다. 성능에 미치는 영향이 크므로 꼭 필요한 경우에만 사용하는 것이 좋습니다.

---

## 업데이트 파라미터: TTL과 TIMESTAMP

`INSERT`, `UPDATE`, `DELETE`, `BATCH` 문은 `USING` 절을 통해 업데이트 파라미터(update parameter)를 지정할 수 있습니다.

```
update_parameter ::= ( TIMESTAMP | TTL ) ( integer | bind_marker )
```

### TTL (Time To Live)

`TTL`은 기록된 값의 수명(초 단위)을 지정합니다. 지정한 시간이 지나면 해당 값은 자동으로 만료(expire)되어 삭제됩니다.

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

- `USING TTL 86400`은 값이 86400초(24시간) 후에 만료됨을 의미합니다.
- `TTL 0`을 지정하면 TTL이 없는 것과 동일하게 동작합니다.
- 컬럼의 남은 TTL은 `TTL(column_name)` 함수로 조회할 수 있습니다.

### TIMESTAMP

`TIMESTAMP`는 작업의 타임스탬프(마이크로초 단위)를 명시적으로 지정합니다. Cassandra는 동일한 컬럼에 대한 여러 쓰기 중 타임스탬프가 가장 큰 값을 최종 값으로 결정하는 LWW(Last-Write-Wins) 방식을 사용하므로, 타임스탬프는 충돌 해결에 중요한 역할을 합니다.

```sql
DELETE FROM NerdMovies USING TIMESTAMP 1240003134
 WHERE movie = 'Serenity';
```

`TIMESTAMP`를 지정하지 않으면 코디네이터 노드의 현재 시각이 자동으로 사용됩니다.

---

## 연산자(Operators)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/operators.html

CQL은 산술 연산(arithmetic operation)을 지원합니다. 모든 산술 연산은 숫자(numeric) 타입 또는 카운터(counter)에 대해 수행할 수 있습니다.

### 산술 연산자(Arithmetic Operators)

CQL은 다음 연산자를 지원합니다.

| 연산자(Operator) | 설명(Description) |
|------------------|-------------------|
| `-` (단항, unary) | 피연산자의 부호를 반전(negation)합니다. |
| `+` | 덧셈(Addition) |
| `-` | 뺄셈(Subtraction) |
| `*` | 곱셈(Multiplication) |
| `/` | 나눗셈(Division) |
| `%` | 나눗셈의 나머지(remainder)를 반환합니다. |

### 숫자 연산(Number Arithmetic)

모든 산술 연산은 숫자 타입과 카운터에 대해 동작합니다. 산술 표현식의 반환 타입(return type)은 피연산자(operand)의 타입에 따라 다음 행렬(matrix)에 의해 결정됩니다.

|            | tinyint | smallint | int    | bigint  | counter | float   | double  | varint  | decimal |
|------------|---------|----------|--------|---------|---------|---------|---------|---------|---------|
| **tinyint**  | tinyint | smallint | int    | bigint  | bigint  | float   | double  | varint  | decimal |
| **smallint** | smallint| smallint | int    | bigint  | bigint  | float   | double  | varint  | decimal |
| **int**      | int     | int      | int    | bigint  | bigint  | float   | double  | varint  | decimal |
| **bigint**   | bigint  | bigint   | bigint | bigint  | bigint  | double  | double  | varint  | decimal |
| **counter**  | bigint  | bigint   | bigint | bigint  | bigint  | double  | double  | varint  | decimal |
| **float**    | float   | float    | float  | double  | double  | float   | double  | decimal | decimal |
| **double**   | double  | double   | double | double  | double  | double  | double  | decimal | decimal |
| **varint**   | varint  | varint   | varint | decimal | decimal | decimal | decimal | decimal | decimal |
| **decimal**  | decimal | decimal  | decimal| decimal | decimal | decimal | decimal | decimal | decimal |

예를 들어, `tinyint`와 `smallint`의 연산 결과는 `smallint`가 되며, `bigint`와 `float` 또는 `double`을 연산하면 `double`이 반환됩니다.

### 연산자 우선순위(Precedence)

`*`, `/`, `%` 연산자는 `+`, `-` 연산자보다 높은 우선순위(precedence level)를 가집니다.

표현식 내에서 두 연산자가 동일한 우선순위를 가지는 경우, 표현식 내 위치에 따라 왼쪽에서 오른쪽으로(left to right) 평가됩니다.

### 날짜/시간 연산(Datetime Arithmetic)

기간(duration) 값을 타임스탬프(timestamp)나 날짜(date)에 더하거나(`+`) 빼서(`-`) 새로운 타임스탬프 또는 날짜를 만들 수 있습니다.

```sql
SELECT * FROM myTable WHERE t = '2017-01-01' - 2d;
```

위 쿼리는 날짜 값 `'2017-01-01'`에서 기간 `2d`(2일)를 빼서, 결과적으로 "2016년의 마지막 2일(the last 2 days of 2016)"에 해당하는 타임스탬프 `t`를 가진 레코드를 조회합니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL DML](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html)
- [CQL Operators](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/operators.html)
