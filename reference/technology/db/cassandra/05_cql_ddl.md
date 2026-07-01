# CQL 데이터 정의 (DDL)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/cql/ddl.html

---

## 목차

1. [개요](#개요)
2. [키스페이스(Keyspace)](#키스페이스keyspace)
3. [CREATE KEYSPACE](#create-keyspace)
   - [복제 전략(Replication Strategy)](#복제-전략replication-strategy)
   - [SimpleStrategy](#simplestrategy)
   - [NetworkTopologyStrategy](#networktopologystrategy)
   - [durable_writes](#durable_writes)
4. [USE](#use)
5. [ALTER KEYSPACE](#alter-keyspace)
6. [DROP KEYSPACE](#drop-keyspace)
7. [CREATE TABLE](#create-table)
   - [컬럼 정의(Column Definition)](#컬럼-정의column-definition)
   - [기본 키(Primary Key)](#기본-키primary-key)
   - [파티션 키(Partition Key)와 클러스터링 컬럼(Clustering Columns)](#파티션-키partition-key와-클러스터링-컬럼clustering-columns)
   - [정적 컬럼(Static Column)](#정적-컬럼static-column)
8. [테이블 옵션(Table Options)](#테이블-옵션table-options)
   - [CLUSTERING ORDER BY](#clustering-order-by)
   - [compaction](#compaction)
   - [compression](#compression)
   - [caching](#caching)
   - [speculative_retry / additional_write_policy](#speculative_retry--additional_write_policy)
   - [read_repair](#read_repair)
   - [그 외 옵션 목록](#그-외-옵션-목록)
9. [ALTER TABLE](#alter-table)
10. [DROP TABLE](#drop-table)
11. [TRUNCATE](#truncate)
12. [참고 자료](#참고-자료)

---

## 개요

데이터 정의(Data Definition, DDL) 구문은 데이터가 어떻게 저장되는지를 정의하고 변경하는 데 사용됩니다. Cassandra에서 데이터는 **키스페이스(keyspace)** 안에 **테이블(table)** 형태로 저장됩니다.

- **키스페이스(keyspace)**: 테이블의 묶음을 위한 네임스페이스(namespace)로, 복제 전략(replication strategy)과 몇 가지 옵션을 정의합니다.
- **테이블(table)**: 행(row)들의 집합이며, 각 행은 미리 정의된 컬럼(column)들로 구성됩니다.

권장되는 모범 사례(best practice)는 "애플리케이션당 하나의 키스페이스(one keyspace per application)"를 두는 것입니다.

### 명명 규칙(Naming Rules)

키스페이스와 테이블의 이름은 영숫자(alphanumeric) 문자와 밑줄(`_`)만 사용할 수 있습니다.

- 키스페이스 이름: 최대 48자(characters)
- 테이블 이름: 최대 222자(characters)

기본적으로 이름은 대소문자를 구분하지 않습니다(case-insensitive). 대소문자를 구분하려면 큰따옴표(`"`)로 감싸야 합니다.

---

## 키스페이스(Keyspace)

키스페이스는 테이블들을 담는 최상위 네임스페이스입니다. 키스페이스는 그 안에 속한 테이블들에 적용될 복제(replication) 설정을 정의합니다. 즉, 데이터를 클러스터의 몇 개 노드에 복제할 것인지, 어떤 전략으로 복제할 것인지가 키스페이스 수준에서 결정됩니다.

---

## CREATE KEYSPACE

키스페이스를 생성하는 구문입니다.

**문법(Grammar):**

```
create_keyspace_statement::= CREATE KEYSPACE [ IF NOT EXISTS ] keyspace_name
	WITH options
```

**예제:**

```cql
CREATE KEYSPACE excelsior
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3}
   AND durable_writes = false;
```

`IF NOT EXISTS`를 사용하면 동일한 이름의 키스페이스가 이미 존재할 때 오류가 발생하지 않습니다(이 경우 아무 작업도 수행되지 않습니다).

`CREATE KEYSPACE`에서 `replication` 속성은 **필수**이며, 사용할 복제 전략 클래스를 정의하는 `'class'` 하위 옵션(sub-option)을 최소한 반드시 포함해야 합니다.

### 복제 전략(Replication Strategy)

Cassandra는 다음 두 가지 주요 복제 전략을 지원합니다.

### SimpleStrategy

`SimpleStrategy`는 클러스터 전체에 대해 단순한 복제 계수(replication factor)를 정의하는 간단한 전략입니다. 필수 하위 옵션으로 `'replication_factor'`를 지정합니다.

```cql
CREATE KEYSPACE excelsior
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
```

`SimpleStrategy`는 데이터센터(datacenter) 레이아웃을 고려하지 않으므로, 일반적으로 **프로덕션(production) 환경에서는 권장되지 않습니다**.

### NetworkTopologyStrategy

`NetworkTopologyStrategy`는 프로덕션 환경에 적합한 전략으로, 데이터센터별(per-datacenter)로 복제 계수를 개별 지정할 수 있습니다.

```cql
CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3};
```

위 예제는 `DC1` 데이터센터에 1개의 복제본을, `DC2` 데이터센터에 3개의 복제본을 둡니다.

`NetworkTopologyStrategy`는 새로운 데이터센터가 추가될 때 복제 계수를 자동으로 확장(auto-expansion)하는 기능을 제공합니다. 또한 **일시적 복제**(transient replication)를 지원하여 `'DC1': '3/1'`과 같은 형식으로 지정할 수 있습니다. 여기서 `3`은 전체 복제본 수이고, `1`은 그 중 일시적 복제본(transient replica) 수를 의미합니다.

### durable_writes

`durable_writes`는 불리언(boolean) 옵션으로 기본값은 `true`입니다. 이 옵션은 커밋 로그(commit log) 사용 여부를 제어합니다. `false`로 설정하면 해당 키스페이스의 쓰기에 커밋 로그를 사용하지 않습니다.

> **경고:** `durable_writes`를 `false`로 설정하면 데이터 손실(data loss)의 위험이 있으므로 주의해서 사용해야 합니다.

```cql
CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3}
   AND durable_writes = false;
```

---

## USE

현재 세션(session)에서 사용할 키스페이스를 지정합니다. 이후 키스페이스를 명시하지 않은 모든 구문은 이 키스페이스를 기준으로 동작합니다.

**문법(Grammar):**

```
use_statement::= USE keyspace_name
```

**예제:**

```cql
USE excelsior;
```

---

## ALTER KEYSPACE

기존 키스페이스의 옵션을 변경합니다. 문법은 `CREATE KEYSPACE`와 동일하게 `WITH` 절을 사용합니다.

**문법(Grammar):**

```
alter_keyspace_statement::= ALTER KEYSPACE [ IF EXISTS ] keyspace_name
	WITH options
```

**예제:**

```cql
ALTER KEYSPACE excelsior
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 4};
```

위 예제는 복제 계수를 4로 변경합니다. `IF EXISTS`를 사용하면 해당 키스페이스가 존재하지 않을 때 오류가 발생하지 않습니다.

---

## DROP KEYSPACE

키스페이스를 삭제합니다. 키스페이스에 속한 모든 테이블과 그 안의 데이터, 인덱스(index) 등도 함께 영구적으로 삭제됩니다.

**문법(Grammar):**

```
drop_keyspace_statement::= DROP KEYSPACE [ IF EXISTS ] keyspace_name
```

**예제:**

```cql
DROP KEYSPACE excelsior;
```

`IF EXISTS`를 사용하면 해당 키스페이스가 존재하지 않을 때 오류가 발생하지 않습니다.

---

## CREATE TABLE

테이블을 생성합니다. 테이블은 행(row)들의 집합이며, 각 행은 이름과 타입(type)으로 정의된 컬럼들로 구성됩니다.

**문법(Grammar):**

```
create_table_statement::= CREATE TABLE [ IF NOT EXISTS ] table_name '('
	column_definition  ( ',' column_definition )*
	[ ',' PRIMARY KEY '(' primary_key ')' ]
	 ')' [ WITH table_options ]
column_definition::= column_name cql_type [ STATIC ] [ column_mask ] [ PRIMARY KEY]
column_mask::= MASKED WITH ( DEFAULT | function_name '(' term ( ',' term )* ')' )
primary_key::= partition_key [ ',' clustering_columns ]
partition_key::= column_name  | '(' column_name ( ',' column_name )* ')'
clustering_columns::= column_name ( ',' column_name )*
table_options::= COMPACT STORAGE [ AND table_options ]
	| CLUSTERING ORDER BY '(' clustering_order ')'
	[ AND table_options ]  | options
clustering_order::= column_name (ASC | DESC) ( ',' column_name (ASC | DESC) )*
```

**주요 예제:**

```cql
CREATE TABLE monkey_species (
    species text PRIMARY KEY,
    common_name text,
    population varint,
    average_size int
) WITH comment='Important biological records';

CREATE TABLE timeline (
    userid uuid,
    posted_month int,
    posted_time uuid,
    body text,
    posted_by text,
    PRIMARY KEY (userid, posted_month, posted_time)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };

CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

`IF NOT EXISTS`를 사용하면 동일한 이름의 테이블이 이미 존재할 때 오류가 발생하지 않습니다.

> **참고:** `COMPACT STORAGE` 옵션은 과거 Thrift 호환성을 위해 존재하던 레거시(legacy) 옵션으로, 현재는 더 이상 사용이 권장되지 않습니다(deprecated).

### 컬럼 정의(Column Definition)

각 컬럼은 이름(name)과 CQL 데이터 타입(data type)으로 정의됩니다. 컬럼 정의에는 다음과 같은 수식어(modifier)를 붙일 수 있습니다.

- `STATIC`: 같은 파티션(partition)의 모든 행이 공유하는 정적 컬럼으로 만듭니다.
- `PRIMARY KEY`: 단일 컬럼을 기본 키로 지정합니다(인라인 형태).
- `MASKED WITH`: 동적 데이터 마스킹(dynamic data masking)을 적용합니다.

### 기본 키(Primary Key)

모든 테이블 정의는 반드시 **하나의 기본 키**(PRIMARY KEY)를 정의해야 하며, 단 하나만 가질 수 있습니다. 기본 키는 두 부분으로 구성됩니다.

```
primary_key::= partition_key [ ',' clustering_columns ]
```

- **파티션 키(partition key)**: 기본 키의 첫 번째 구성 요소
- **클러스터링 컬럼(clustering columns)**: 그 뒤를 따르는 선택적 구성 요소

### 파티션 키(Partition Key)와 클러스터링 컬럼(Clustering Columns)

**파티션 키**(Partition Key)는 해당 행(들)이 어느 노드에 저장될지를 결정합니다. 파티션 키는 단일 컬럼일 수도 있고, 여러 컬럼을 괄호로 묶은 **복합 파티션 키**(composite partition key)일 수도 있습니다. 동일한 파티션 키 값을 가진 행들은 같은 파티션(partition)에 저장됩니다.

```
partition_key::= column_name  | '(' column_name ( ',' column_name )* ')'
```

**클러스터링 컬럼**(Clustering Columns)은 파티션 키 뒤에 위치하며, 파티션 내에서 행들의 정렬 순서(ordering)를 정의합니다. 클러스터링 컬럼은 행에 유일성(uniqueness)을 추가하며, 효율적인 범위 쿼리(range query)를 가능하게 합니다.

테이블의 클러스터링 컬럼은 해당 테이블 파티션의 클러스터링 순서(clustering order)를 정의합니다. 주어진 파티션 내에서 모든 행은 이 클러스터링 순서에 따라 정렬됩니다.

예를 들어 `PRIMARY KEY ((a, b), c)`는 `(a, b)`를 복합 파티션 키로, `c`를 클러스터링 컬럼으로 만듭니다.

```cql
CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

위 예제에서 `(machine, cpu)`는 복합 파티션 키이고, `mtime`은 클러스터링 컬럼이며, 각 파티션 내에서 `mtime` 기준 내림차순(DESC)으로 정렬됩니다.

### 정적 컬럼(Static Column)

`STATIC` 수식어로 선언된 컬럼은 같은 파티션에 속한 모든 행이 "공유(shared)"하는 값을 가집니다. 즉, 같은 파티션 키를 가진 행들은 정적 컬럼에 대해 동일한 값을 보게 됩니다. 이는 파티션 수준의 메타데이터(partition-level metadata)를 저장하는 데 유용합니다.

```cql
CREATE TABLE t (
    pk int,
    t int,
    v text,
    s text static,
    PRIMARY KEY (pk, t)
);
INSERT INTO t (pk, t, v, s) VALUES (0, 0, 'val0', 'static0');
INSERT INTO t (pk, t, v, s) VALUES (0, 1, 'val1', 'static1');
SELECT * FROM t;
```

위 예제에서 `s` 컬럼은 정적 컬럼입니다. 두 번째 `INSERT`가 같은 파티션 키(`pk = 0`)에 `static1`을 쓰면, 첫 번째 행을 포함한 파티션 전체의 `s` 값이 `static1`로 갱신됩니다.

---

## 테이블 옵션(Table Options)

`CREATE TABLE` 또는 `ALTER TABLE` 구문의 `WITH` 절을 통해 테이블의 다양한 속성(property)을 지정할 수 있습니다. 여러 옵션은 `AND`로 연결합니다.

### CLUSTERING ORDER BY

파티션 내 행들의 기본 정렬 순서를 지정합니다.

```
clustering_order::= column_name (ASC | DESC) ( ',' column_name (ASC | DESC) )*
```

```cql
CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

> **중요:** 쿼리 결과는 원래의 클러스터링 순서(original clustering order) 또는 그 역순(reverse clustering order)으로만 정렬할 수 있습니다.
>
> 예를 들어 두 개의 클러스터링 컬럼 `a`와 `b`를 가진 테이블을 `WITH CLUSTERING ORDER BY (a DESC, b ASC)`로 정의했다면, 이 테이블에 대한 쿼리는 `ORDER BY (a DESC, b ASC)` 또는 `ORDER BY (a ASC, b DESC)`만 사용할 수 있습니다.

### compaction

컴팩션(compaction)은 SSTable들을 병합하여 디스크 공간을 회수하고 읽기 성능을 유지하는 과정입니다. `compaction` 옵션은 사용할 컴팩션 전략 클래스(`'class'`)와 전략별 하위 옵션을 지정합니다.

지원되는 컴팩션 전략 클래스:

- `SizeTieredCompactionStrategy` (STCS)
- `LeveledCompactionStrategy` (LCS)
- `TimeWindowCompactionStrategy` (TWCS)

```cql
CREATE TABLE timeline (
    userid uuid,
    posted_month int,
    posted_time uuid,
    body text,
    posted_by text,
    PRIMARY KEY (userid, posted_month, posted_time)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };
```

각 전략은 `enabled`, `tombstone_threshold`, `tombstone_compaction_interval`, `min_threshold`, `max_threshold` 등의 공통 하위 옵션과 함께 전략 고유의 하위 옵션을 갖습니다. 자세한 내용은 공식 문서의 [Compaction](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/index.html) 섹션을 참고합니다.

### compression

`compression` 옵션은 SSTable 압축(compression)을 구성합니다.

| 옵션(Option) | 기본값(Default) | 설명 |
|---|---|---|
| `class` | `LZ4Compressor` | 사용할 압축 알고리즘. 기본 제공 압축기: `LZ4Compressor`, `SnappyCompressor`, `DeflateCompressor`, `ZstdCompressor`. 압축을 비활성화하려면 `'enabled' : false`를 사용합니다. |
| `enabled` | `true` | SSTable 압축 활성화/비활성화. `enabled`가 `false`이면 다른 옵션을 지정해서는 안 됩니다. |
| `chunk_length_in_kb` | `64` | SSTable은 랜덤 읽기(random read)를 허용하기 위해 블록 단위로 압축됩니다. 이 옵션은 해당 블록의 크기(KB 단위)를 정의합니다. |
| `compression_level` | `3` | 압축 레벨. `ZstdCompressor`에만 적용됩니다. `-131072`에서 `22` 사이의 값을 허용합니다. |

```cql
CREATE TABLE simple (
   id int,
   key text,
   value text,
   PRIMARY KEY (key, value)
) WITH compression = {'class': 'LZ4Compressor', 'chunk_length_in_kb': 4};
```

### caching

`caching` 옵션은 키 캐시(key cache)와 행 캐시(row cache) 동작을 제어합니다.

| 옵션(Option) | 기본값(Default) | 설명 |
|---|---|---|
| `keys` | `ALL` | 이 테이블의 키 캐싱 여부. 유효한 값은 `ALL`과 `NONE`입니다. |
| `rows_per_partition` | `NONE` | 파티션당 캐싱할 행의 수(행 캐시). 정수 `n`을 지정하면 파티션에서 처음 조회된 `n`개의 행이 캐싱됩니다. 유효한 값: 파티션의 모든 행을 캐싱하는 `ALL`, 행 캐싱을 비활성화하는 `NONE`, 또는 정수. |

```cql
CREATE TABLE simple (
id int,
key text,
value text,
PRIMARY KEY (key, value)
) WITH caching = {'keys': 'ALL', 'rows_per_partition': 10};
```

### speculative_retry / additional_write_policy

읽기 코디네이터(read coordinator)는 일관성 수준(consistency level)을 만족하는 데 필요한 만큼의 복제본만 조회합니다(예: `ONE`은 1개, `QUORUM`은 정족수). `speculative_retry`는 코디네이터가 **추가 복제본을 조회할 시점**을 결정하며, 복제본이 느리거나 응답하지 않을 때 유용합니다.

`additional_write_policy`는 `speculative_retry`와 동일하지만 쓰기 경로(write path)에 적용됩니다. 두 옵션의 기본값은 모두 `99PERCENTILE`입니다.

**4.0 이전(pre-4.0) 값:**

- `NONE`
- `ALWAYS`
- `99PERCENTILE` (PERCENTILE)
- `50MS` (CUSTOM)

**Cassandra 4.0+ 확장 값:**

| 형식(Format) | 예시(Example) | 설명 |
|---|---|---|
| `XPERCENTILE` | `90.5PERCENTILE` | 코디네이터는 모든 복제본에 대해 테이블별 평균 응답 시간을 기록합니다. 어떤 복제본이 이 테이블 평균 응답 시간의 `X`퍼센트보다 오래 걸리면 코디네이터가 추가 복제본을 조회합니다. `X`는 0~100 사이여야 합니다. |
| `XP` | `90.5P` | `XPERCENTILE`와 동일합니다. |
| `Yms` | `25ms` | 복제본이 응답하는 데 `Y` 밀리초(milliseconds)를 초과하면 코디네이터가 추가 복제본을 조회합니다. |
| `MIN(XPERCENTILE,YMS)` | `MIN(99PERCENTILE,35MS)` | 계산 시점에 더 낮은(lower) 값을 사용하는 하이브리드(hybrid) 정책입니다. |
| `MAX(XPERCENTILE,YMS)` | `MAX(90.5P,25ms)` | 계산 시점에 더 높은(higher) 값을 사용하는 하이브리드(hybrid) 정책입니다. |

Cassandra 4.0은 대소문자 구분 없는(case-insensitive) 표기와 백분위(percentile)·밀리초를 조합하는 `MIN()`/`MAX()` 하이브리드 정책을 추가했습니다.

```cql
ALTER TABLE users WITH speculative_retry = '10ms';
ALTER TABLE users WITH speculative_retry = '99PERCENTILE';
```

### read_repair

`read_repair` 옵션은 읽기 복구(read repair) 동작을 설정합니다. 기본값은 `BLOCKING`입니다.

| 값(Option) | 설명 |
|---|---|
| `BLOCKING` | 읽기 복구가 트리거되면, 다른 복제본으로 보낸 쓰기가 일관성 수준에 도달할 때까지 읽기가 차단(block)됩니다. |
| `NONE` | 코디네이터가 복제본 간의 차이를 조정(reconcile)하기는 하지만, 이를 복구(repair)하려고 시도하지는 않습니다. |

**일관성에 미치는 영향:**

- **단조 정족수 읽기(Monotonic quorum reads):** 읽기가 시간상 과거로 돌아간 것처럼 보이는 현상을 방지합니다. `BLOCKING`이 이 동작을 제공합니다.
- **쓰기 원자성(Write atomicity):** 부분적으로 적용된 쓰기(partially-applied write)가 반환되는 것을 방지합니다. `NONE`이 이 동작을 제공합니다.

### 그 외 옵션 목록

테이블 생성 또는 변경 시 설정할 수 있는 전체 옵션 목록입니다.

| 옵션(Option) | 종류(Kind) | 기본값(Default) | 설명 |
|---|---|---|---|
| `comment` | simple | none | 자유 형식의 사람이 읽을 수 있는 주석(comment). |
| `speculative_retry` | simple | `99PERCENTILE` | 추측성 재시도(speculative retry) 옵션. |
| `additional_write_policy` | simple | `99PERCENTILE` | `speculative_retry`와 동일. |
| `cdc` | boolean | `false` | 테이블에 변경 데이터 캡처(Change Data Capture, CDC) 로그를 생성합니다. |
| `gc_grace_seconds` | simple | `864000` | 툼스톤(tombstone, 삭제 표식)을 가비지 컬렉션하기 전 대기 시간(초). 기본값 864000초는 10일입니다. |
| `bloom_filter_fp_chance` | simple | `0.00075` | SSTable 블룸 필터(bloom filter)의 목표 거짓 양성(false positive) 확률. |
| `default_time_to_live` | simple | `0` | 테이블의 기본 만료 시간(TTL, 초). `0`은 만료 없음을 의미합니다. |
| `compaction` | map | (위 참고) | 컴팩션 옵션. |
| `compression` | map | (위 참고) | 압축 옵션. |
| `caching` | map | (위 참고) | 캐싱 옵션. |
| `memtable_flush_period_in_ms` | simple | `0` | Cassandra가 멤테이블(memtable)을 디스크로 플러시(flush)하기까지의 시간(ms). |
| `read_repair` | simple | `BLOCKING` | 읽기 복구 동작 설정(위 참고). |

```cql
CREATE TABLE monkey_species (
    species text PRIMARY KEY,
    common_name text,
    population varint,
    average_size int
) WITH comment='Important biological records';
```

---

## ALTER TABLE

기존 테이블을 변경합니다. 컬럼 추가/삭제/이름 변경, 마스킹 적용/해제, 테이블 옵션 변경이 가능합니다.

**문법(Grammar):**

```
alter_table_statement::= ALTER TABLE [ IF EXISTS ] table_name alter_table_instruction
alter_table_instruction::= ADD [ IF NOT EXISTS ] column_definition ( ',' column_definition)*
	| DROP [ IF EXISTS ] column_name ( ',' column_name )*
	| RENAME [ IF EXISTS ] column_name to column_name (AND column_name to column_name)*
	| ALTER [ IF EXISTS ] column_name ( column_mask | DROP MASKED )
	| WITH options
column_definition::= column_name cql_type [ column_mask]
column_mask::= MASKED WITH ( DEFAULT | function_name '(' term ( ',' term )* ')' )
```

**예제:**

```cql
ALTER TABLE addamsFamily ADD gravesite varchar;

ALTER TABLE addamsFamily
   WITH comment = 'A most excellent and useful table';
```

**주요 제약 사항:**

- 기본 키(primary key)에 속한 컬럼은 변경할 수 없습니다.
- 컬럼을 추가하는 작업(`ADD`)은 상수 시간 연산(constant-time operation)입니다.
- `ADD`, `DROP`, `RENAME`, `ALTER`(마스킹), `WITH`(옵션 변경) 중 하나의 명령을 지정합니다.

---

## DROP TABLE

테이블과 그 안의 모든 데이터를 영구적으로 삭제합니다.

**문법(Grammar):**

```
drop_table_statement::= DROP TABLE [ IF EXISTS ] table_name
```

`IF EXISTS`를 사용하면 해당 테이블이 존재하지 않을 때 오류가 발생하지 않습니다.

```cql
DROP TABLE monkey_species;
```

---

## TRUNCATE

테이블의 구조(schema)는 유지한 채, 테이블에 담긴 모든 데이터를 삭제합니다.

**문법(Grammar):**

```
truncate_statement::= TRUNCATE [ TABLE ] table_name
```

```cql
TRUNCATE monkey_species;
TRUNCATE TABLE monkey_species;
```

> **참고:** `TRUNCATE`는 모든 노드에서 데이터를 제거하는 작업으로, 클러스터 전체에 영향을 주는 무거운(heavy) 연산입니다. `DROP TABLE`이 테이블 정의 자체를 제거하는 것과 달리, `TRUNCATE`는 정의를 보존하고 데이터만 비웁니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL DDL](https://cassandra.apache.org/doc/latest/cassandra/cql/ddl.html)
