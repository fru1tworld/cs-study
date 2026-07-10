# CQL 보조 인덱스와 구체화된 뷰

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/

---

## 목차

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

## 보조 인덱스(Secondary Indexes)

CQL은 테이블의 컬럼에 보조 인덱스(secondary index)를 만드는 기능을 제공합니다. 보조 인덱스를 사용하면 기본 키(primary key) 이외의 컬럼을 조건으로 질의(query)할 수 있습니다. 인덱스 이름(index name)은 `[a-zA-Z_0-9]+` 패턴을 따릅니다.

### CREATE INDEX

`CREATE INDEX` 문은 테이블의 단일 컬럼에 대해 새로운 보조 인덱스(2i, secondary index) 또는 스토리지 연결 인덱스(SAI, storage-attached index)를 정의합니다.

문법은 다음과 같습니다.

```
CREATE [ CUSTOM ] INDEX [ IF NOT EXISTS ] [ index_name ]
    ON table_name '(' index_identifier ')'
    [ USING index_type [ WITH OPTIONS = map_literal ] ]
```

`index_identifier`는 다음 중 하나입니다.

- 컬럼 이름(column name)
- 컬렉션(collection)을 위한 `KEYS(column_name)`, `VALUES(column_name)`, `ENTRIES(column_name)`, `FULL(column_name)`

인덱스 생성 예시는 다음과 같습니다.

```cql
CREATE INDEX userIndex ON NerdMovies (user);
CREATE INDEX ON users (KEYS(favs));
CREATE INDEX ON users (age) USING 'sai';
CREATE CUSTOM INDEX ON users (email) USING 'path.to.the.IndexClass'
    WITH OPTIONS = {'storage': '/mnt/ssd/indexes/'};
```

주요 동작은 다음과 같습니다.

- `CREATE INDEX` 문을 실행하면, 컬럼에 이미 데이터가 들어 있는 경우 해당 데이터는 비동기적으로 인덱싱됩니다. 인덱스가 생성된 후에는 해당 컬럼의 데이터가 변경될 때마다 인덱스가 자동으로 갱신됩니다.
- `IF NOT EXISTS`를 지정하면 같은 이름의 인덱스가 이미 존재할 때 오류를 발생시키지 않고 명령을 무시합니다.
- `CUSTOM` 키워드를 사용하는 커스텀 인덱스(custom index)는 인덱스를 구현하는 클래스의 완전한 이름(fully qualified class name)을 요구합니다.

### 인덱스 타입(Index Types)

Cassandra는 두 가지 내장(built-in) 인덱스 타입을 제공합니다.

- **`legacy_local_table`** (기본값): 기존(legacy)의 보조 인덱스로, 숨겨진 로컬 테이블(hidden local table)로 구현됩니다.
- **`sai`**: "스토리지 연결(storage-attached)" 인덱스로, SSTable 및 Memtable에 연결되어 최적화된 형태로 구현됩니다.

`USING` 절을 통해 인덱스 타입을 명시적으로 지정할 수 있으며, `WITH OPTIONS` 절로 인덱스 설정을 제어할 수 있습니다.

### 컬렉션 인덱싱(Collection Indexing)

맵(map) 컬럼에 대해서는 인덱싱 대상을 세분화할 수 있습니다.

- `KEYS()`는 맵의 키(key)를 인덱싱하여 `CONTAINS KEY` 질의를 가능하게 합니다.
- 기본 동작은 맵의 값(value)을 인덱싱합니다.
- `ENTRIES()`는 맵의 항목(키-값 쌍)을 인덱싱합니다.
- `FULL()`은 동결된(frozen) 컬렉션 전체를 인덱싱합니다.

### DROP INDEX

`DROP INDEX` 문은 기존 보조 인덱스를 제거합니다.

```
DROP INDEX [ IF EXISTS ] index_name
```

`IF EXISTS`를 지정하면 해당 인덱스가 존재하지 않더라도 오류 없이 안전하게 명령이 수행됩니다.

### 보조 인덱스 사용 시 주의점

보조 인덱스는 기본 키가 아닌 컬럼으로의 질의를 가능하게 해 주지만, 동작 원리상 다음과 같은 점을 유의해야 합니다.

- 보조 인덱스(2i)는 각 노드에 로컬(local)로 구성되므로, 인덱스 컬럼만으로 질의하면 코디네이터(coordinator)가 모든 노드에 요청을 분산해야 할 수 있어 비용이 높습니다.
- 카디널리티(cardinality)가 매우 높거나(거의 유일한 값들) 매우 낮은(소수의 값에 매우 많은 로우가 몰리는) 컬럼은 보조 인덱스에 적합하지 않습니다.
- 자주 갱신되거나 삭제되는 컬럼은 인덱스 유지 비용이 커집니다.
- 가능한 한 파티션 키(partition key)를 함께 지정하여 질의 범위를 좁히는 것이 좋습니다.

---

## 구체화된 뷰(Materialized Views)

구체화된 뷰(materialized view)는 기반 테이블(base table)로부터 생성되는 데이터베이스 객체로, 데이터가 자동으로 동기화되어 유지됩니다. 구체화된 뷰는 직접 갱신할 수 없으며, 대신 기반 테이블에 가해진 변경 사항이 해당 뷰로 자동으로 전파됩니다.

### CREATE MATERIALIZED VIEW

문법은 다음과 같습니다.

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

`IF NOT EXISTS`를 지정하면 같은 이름의 뷰가 이미 존재하더라도 오류를 발생시키지 않습니다.

### SELECT 문 제약

구체화된 뷰의 `SELECT` 문에는 다음과 같은 중요한 제약이 있습니다.

- **선택 절(selection clause)**: 기반 테이블의 컬럼만 선택할 수 있습니다. 함수(function), 형 변환(casting), 별칭(alias), 정적 컬럼(static column)은 사용할 수 없습니다. 기반 테이블에 정적 컬럼이 포함되어 있으면 와일드카드 선택(`SELECT *`)을 사용할 수 없습니다.

- **WHERE 절 요구사항**:
  - 바인드 마커(bind marker)는 허용되지 않습니다.
  - 기본 키가 아닌 컬럼은 `IS NOT NULL` 제약을 가져야 합니다.
  - 뷰의 기본 키 컬럼은 최소한 `IS NOT NULL` 제약을 포함해야 합니다.
  - 정렬 절(ordering clause), 제한(limit), `ALLOW FILTERING`은 사용할 수 없습니다.

### 기본 키(Primary Key) 제약

뷰의 기본 키는 다음 조건을 만족해야 합니다.

- 기반 테이블의 모든 기본 키 컬럼을 포함해야 합니다.
- 기반 테이블의 기본 키가 아닌 컬럼을 **최대 하나만** 추가로 포함할 수 있습니다.

다음은 기본 키가 아닌 컬럼을 하나만 추가할 수 있다는 규칙을 위반하는 **잘못된 예시**입니다(아래는 `v1`, `v2` 두 개의 비기본키 컬럼을 사용하여 규칙을 위반함).

```cql
CREATE MATERIALIZED VIEW mv1 AS
   SELECT * FROM t
   WHERE k IS NOT NULL AND c1 IS NOT NULL AND c2 IS NOT NULL AND v1 IS NOT NULL
   PRIMARY KEY (v1, v2, k, c1, c2);
```

### ALTER MATERIALIZED VIEW

```
alter_materialized_view_statement::= ALTER MATERIALIZED VIEW [ IF EXISTS ] view_name WITH table_options
```

수정 가능한 옵션은 뷰 생성 시 사용할 수 있는 옵션과 동일합니다.

### DROP MATERIALIZED VIEW

```
drop_materialized_view_statement::= DROP MATERIALIZED VIEW [ IF EXISTS ] view_name;
```

`IF EXISTS`를 지정하면 해당 뷰가 존재하지 않더라도 오류 없이 안전하게 수행됩니다.

### 구체화된 뷰의 한계와 주의점

- **선택되지 않은 기반 컬럼의 삭제 문제**: 뷰에 선택되지 않은 기반 테이블 컬럼을 삭제하는 경우, 힌트(hint)나 리페어(repair)를 통해 수신된 다른 컬럼들의 갱신을 놓칠 수 있습니다("missed updates to other columns received by hints or repair"). 이 문제(CASSANDRA-13826)가 해결되기 전까지는 이러한 삭제를 신중히 다룰 것을 권장합니다.

- **성능 설정**: 구체화된 뷰의 초기 구성(build)은 기본적으로 단일 스레드(single-threaded)로 동작합니다. `cassandra.yaml`의 `concurrent_materialized_view_builders` 속성을 통해 이를 병렬화할 수 있습니다.

- 구체화된 뷰는 직접 쓰기(write)할 수 없으며, 기반 테이블을 통해서만 데이터가 갱신됩니다.

---

## SASI 인덱스(SSTable Attached Secondary Index)

### SASI 개요

SASI(SSTable Attached Secondary Index)는 Cassandra의 `Index` 인터페이스를 구현한 것으로, 기존 인덱싱 구현의 대안으로 사용할 수 있습니다. SASI의 인덱싱과 질의는 Cassandra의 요구사항에 맞춰 특화되어 기존 구현보다 향상된 성능을 제공합니다.

주요 장점은 다음과 같습니다.

- 이전에는 필터링(filtering)이 필요했던 질의에서 뛰어난 성능을 제공합니다.
- 메모리, 디스크, CPU 사용량 측면에서 기존 구현보다 자원 소모가 현저히 적습니다("significantly less resource intensive").
- 문자열에 대한 접두사(prefix) 및 부분 문자열(contains) 질의를 지원합니다(SQL의 `LIKE = "foo*"` 또는 `LIKE = "*foo*"`와 유사).

SASI라는 이름은 Cassandra의 한 번 쓰고(write-once) 변경 불가능하며(immutable) 정렬된(ordered) 데이터 모델을 활용하여, Memtable을 디스크로 플러시(flush)할 때 인덱스를 함께 구성한다는 점에서 비롯됐습니다("SSTable Attached Secondary Index").

### SASI 인덱스 생성

SASI 인덱스는 `CREATE CUSTOM INDEX` 문으로 생성합니다.

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

### 인덱스 모드(Index Modes)

SASI는 컬럼별로 설정할 수 있는 세 가지 인덱싱 모드를 지원합니다.

- **PREFIX** (기본값): 각 항목(term)의 정확한 값을 `OnDiskIndex`마다 정확히 한 번씩 기록합니다. 예를 들어 `Jason`, `Jordan`, `Pavel`이라는 항목이 있으면 세 값 모두 그대로 인덱스에 저장되어, `WHERE first_name LIKE 'M%'`와 같은 접두사 기반 질의가 가능합니다.

- **CONTAINS**: 각 항목의 모든 접미사(suffix)를 재귀적으로 추가로 기록합니다. 위 예시를 사용하면 인덱스는 `ason`, `ordan`, `avel`, `son`, `rdan`, `vel` 등을 추가로 저장합니다. 이를 통해 문자열의 부분 문자열에 대한 질의(`WHERE last_name LIKE '%a%'`)가 가능해집니다.

- **SPARSE**: 예를 들어 타임스탬프(timestamp)처럼 큰 범위의 값을 효율적으로 순회하기 위해 설계되었습니다. PREFIX 모드와 달리, 항목 64개 블록마다 각 항목의 `TokenTree`를 하나로 병합한 단일 `TokenTree`를 구성합니다. 이는 많은 값이 밀집(dense)되어 있는 수치 범위에 최적화된 방식입니다.

> **주의**: SPARSE 모드는 동일한 값에 매우 많은 로우가 연결되는 경우(즉, 항목 하나당 너무 많은 토큰이 매핑되는 경우)에는 적합하지 않습니다. 이 모드는 항목당 매핑되는 값(토큰)이 적고, 항목의 종류가 매우 많은(밀집된 수치 범위) 데이터에 사용하도록 설계되었습니다.

### 분석기(Analyzer)와 토큰화 옵션

**NonTokenizingAnalyzer**: 텍스트에 대해 분석을 수행하지 않고 정확한 값을 보존합니다.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.NonTokenizingAnalyzer',
  'case_sensitive': 'false'
}
```

**StandardAnalyzer**: 토큰화(tokenization)와 어간 추출(stemming)을 지원합니다.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.StandardAnalyzer',
  'tokenization_enable_stemming': 'true',
  'analyzed': 'true',
  'tokenization_normalize_lowercase': 'true',
  'tokenization_locale': 'en'
}
```

**DelimiterAnalyzer**: 구분자(delimiter)를 기준으로 텍스트를 토큰화합니다.

```cql
WITH OPTIONS = {
  'analyzer_class': 'org.apache.cassandra.index.sasi.analyzer.DelimiterAnalyzer',
  'delimiter': ',',
  'mode': 'prefix',
  'analyzed': 'true'
}
```

주요 공통 옵션은 다음과 같습니다.

| 옵션(option) | 설명 |
| --- | --- |
| `analyzer_class` | 사용할 텍스트 분석 전략(분석기 클래스)을 지정합니다. |
| `case_sensitive` | 대소문자 구분 여부를 제어합니다(`true`/`false`). |
| `mode` | 인덱스 모드를 선택합니다(`PREFIX`/`CONTAINS`/`SPARSE`). |
| `analyzed` | 텍스트 분석(토큰화) 처리를 활성화합니다(`true`/`false`). |
| `delimiter` | `DelimiterAnalyzer`에서 사용할 구분 문자입니다. |
| `tokenization_enable_stemming` | 단어 어간 추출(stemming)을 활성화합니다. |
| `tokenization_normalize_lowercase` | 소문자 정규화(lowercase normalization)를 수행합니다. |
| `tokenization_locale` | 분석에 사용할 언어(로캘)를 지정합니다. |
| `is_literal` | 컬럼을 리터럴(literal) 타입으로 설정하여 트라이(trie) 기반 인덱싱을 사용하도록 합니다. |

**텍스트 분석 기능**: `StandardAnalyzer`는 언어적 처리를 지원합니다.

- **어간 추출(Stemming)**: 단어를 어근 형태로 변환합니다. 예를 들어 "distributing", "argued", "working"은 각각 "distribution", "argue", "work"와 매칭됩니다.
- **토큰화(Tokenization)**: 텍스트를 검색 가능한 항목들로 분리합니다.
- **로캘별 처리**: `tokenization_locale`를 통해 언어별 정규화를 지원합니다.

### 질의(Query) 예시

**동등(equality) 및 접두사(prefix) 질의**:

```cql
SELECT * FROM table WHERE column_name = 'value';
SELECT * FROM table WHERE column_name LIKE 'M%';
```

**부분 문자열(CONTAINS) 질의**:

```cql
SELECT * FROM table WHERE column_name LIKE '%substring%';
```

**복합(compound) 질의**:

```cql
SELECT * FROM table
WHERE first_column LIKE 'M%' AND second_column < 30
ALLOW FILTERING;
```

**비인덱스 컬럼 필터링**:

```cql
SELECT * FROM table
WHERE indexed_column LIKE '%value%' AND non_indexed_column >= 175
ALLOW FILTERING;
```

### 구현 세부사항과 한계

**핵심 아키텍처**: SASI는 Cassandra의 한 번 쓰고 변경 불가능하며 정렬된 데이터 모델을 활용하여, Memtable을 디스크로 플러시할 때 인덱스를 함께 구성합니다. "SSTable Attached Secondary Index"라는 이름의 유래입니다.

**자료 구조(Data Structures)**:

- 디스크 상의 인덱싱에는 수정된 접미사 배열(Suffix Array) 자료 구조를 사용합니다.
- 토큰 관리에는 잘 알려진 B+ 트리(B+ tree)의 구현을 사용합니다.
- 메모리 상에서는 리터럴 타입에 대해 `TrieMemIndex`를, 수치 타입에 대해 `SkipListMemIndex`를 사용합니다.

**질의 처리(Query Processing)**: 검색 질의마다 인스턴스화되는 `QueryPlan`이 SASI 질의 구현의 핵심입니다. Cassandra의 내부 표현인 `IndexExpression`들을 연산 트리(operation tree)로 변환해 질의를 최적화한 뒤, `DecoratedKey`를 생성하는 이터레이터(iterator)를 통해 실행합니다.

**한계(Limitations)**:

1. **파티셔너(Partitioner) 요구사항**: 클러스터는 `LongToken`을 생성하는 파티셔너(예: `Murmur3Partitioner`)를 사용하도록 구성되어야 합니다. `LongToken`을 생성하지 않는 다른 파티셔너(예: `ByteOrderedPartitioner`, `RandomPartitioner`)는 SASI와 함께 동작하지 않습니다.

2. **제거된 기능**: Cassandra 자체의 변경이 이루어지는 동안, 이번 릴리스에서는 부등(Not Equals, `!=`)과 OR 지원이 제거되었습니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL Secondary Indexes](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/indexes.html)
- [CQL Materialized Views](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/mvs.html)
- [SASI Index](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/SASI.html)
- [CREATE INDEX (CQL Reference)](https://cassandra.apache.org/doc/latest/cassandra/reference/cql-commands/create-index.html)
