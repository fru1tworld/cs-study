# Chapter 53: 시스템 카탈로그 (System Catalogs)

## 목차
1. [개요](#개요)
2. [시스템 카탈로그 목록](#시스템-카탈로그-목록)
3. [주요 시스템 카탈로그](#주요-시스템-카탈로그)
   - [pg_class](#pg_class)
   - [pg_attribute](#pg_attribute)
   - [pg_type](#pg_type)
   - [pg_proc](#pg_proc)
   - [pg_namespace](#pg_namespace)
   - [pg_index](#pg_index)
   - [pg_constraint](#pg_constraint)
   - [pg_database](#pg_database)
4. [시스템 카탈로그 활용 예제](#시스템-카탈로그-활용-예제)

---

## 개요

시스템 카탈로그(System Catalogs)는 관계형 데이터베이스 관리 시스템이 스키마 메타데이터를 저장하는 장소입니다. 테이블과 컬럼에 대한 정보, 그리고 내부 관리 정보가 여기에 저장됩니다.

### 핵심 특징

PostgreSQL의 시스템 카탈로그는 일반 테이블 입니다. 기술적으로는 삭제, 재생성, 수정이 가능하지만, 정상적인 상황에서는 직접 수정하지 않는 것이 좋습니다.

> 주의사항: 시스템 카탈로그에 컬럼을 추가하거나, 값을 직접 삽입/수정하면 시스템에 심각한 문제가 발생할 수 있습니다.

### 권장 사항

시스템 카탈로그를 직접 조작하는 대신, 표준 SQL 명령어를 사용해야 합니다:

```sql
-- 직접 카탈로그 조작 (권장하지 않음)
-- INSERT INTO pg_database (...) VALUES (...);

-- 대신 표준 SQL 명령어 사용 (권장)
CREATE DATABASE mydb;
```

예를 들어, `CREATE DATABASE` 명령은 내부적으로 `pg_database` 카탈로그에 행을 삽입하고 디스크에 데이터베이스를 생성합니다.

---

## 시스템 카탈로그 목록

PostgreSQL은 50개 이상의 시스템 카탈로그를 제공합니다. 주요 카탈로그들의 목록은 다음과 같습니다:

| 카탈로그 | 설명 |
|----------|------|
| `pg_aggregate` | 집계 함수 (Aggregate functions) |
| `pg_am` | 접근 방법 (Access methods) |
| `pg_amop` | 접근 방법 연산자 (Access method operators) |
| `pg_amproc` | 접근 방법 프로시저 (Access method procedures) |
| `pg_attrdef` | 속성 기본값 (Attribute defaults) |
| `pg_attribute` | 테이블 컬럼 (Table columns) |
| `pg_authid` | 인증 식별자 (Authentication identifiers) |
| `pg_auth_members` | 역할 멤버십 (Role membership) |
| `pg_cast` | 타입 캐스트 (Type casts) |
| `pg_class` | 테이블, 인덱스, 시퀀스 등 (Tables, indexes, sequences) |
| `pg_collation` | 콜레이션 (Collations) |
| `pg_constraint` | 제약 조건 (Constraints) |
| `pg_conversion` | 인코딩 변환 (Encoding conversions) |
| `pg_database` | 데이터베이스 (Databases) |
| `pg_depend` | 의존성 정보 (Dependency information) |
| `pg_description` | 객체 주석 (Object comments) |
| `pg_enum` | 열거형 타입 (Enumeration types) |
| `pg_event_trigger` | 이벤트 트리거 (Event triggers) |
| `pg_extension` | 확장 (Extensions) |
| `pg_foreign_data_wrapper` | 외부 데이터 래퍼 (Foreign data wrappers) |
| `pg_foreign_server` | 외부 서버 (Foreign servers) |
| `pg_foreign_table` | 외부 테이블 (Foreign tables) |
| `pg_index` | 인덱스 (Indexes) |
| `pg_inherits` | 테이블 상속 (Table inheritance) |
| `pg_language` | 언어 (Languages) |
| `pg_largeobject` | 대용량 객체 (Large objects) |
| `pg_namespace` | 스키마 (Schemas/Namespaces) |
| `pg_opclass` | 연산자 클래스 (Operator classes) |
| `pg_operator` | 연산자 (Operators) |
| `pg_opfamily` | 연산자 패밀리 (Operator families) |
| `pg_partitioned_table` | 파티션 테이블 (Partitioned tables) |
| `pg_policy` | 행 수준 보안 정책 (Row-level security policies) |
| `pg_proc` | 함수 및 프로시저 (Functions and procedures) |
| `pg_publication` | 논리적 복제 발행 (Logical replication publications) |
| `pg_range` | 범위 타입 (Range types) |
| `pg_rewrite` | 규칙 (Rules) |
| `pg_sequence` | 시퀀스 (Sequences) |
| `pg_statistic` | 통계 (Statistics) |
| `pg_subscription` | 논리적 복제 구독 (Logical replication subscriptions) |
| `pg_tablespace` | 테이블스페이스 (Tablespaces) |
| `pg_trigger` | 트리거 (Triggers) |
| `pg_ts_config` | 텍스트 검색 구성 (Text search configurations) |
| `pg_ts_dict` | 텍스트 검색 사전 (Text search dictionaries) |
| `pg_type` | 데이터 타입 (Data types) |
| `pg_user_mapping` | 사용자 매핑 (User mappings) |

---

## 주요 시스템 카탈로그

### pg_class

`pg_class` 카탈로그는 테이블 및 테이블과 유사한 구조를 가진 객체들에 대한 정보를 저장합니다.

#### 저장되는 객체 유형
- 테이블 (Tables)
- 인덱스 (Indexes)
- 시퀀스 (Sequences)
- 뷰 (Views)
- 구체화된 뷰 (Materialized views)
- 복합 타입 (Composite types)
- TOAST 테이블

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 (Row identifier) |
| `relname` | `name` | 테이블, 인덱스, 뷰 등의 이름 |
| `relnamespace` | `oid` | 이 릴레이션을 포함하는 네임스페이스의 OID (`pg_namespace.oid` 참조) |
| `reltype` | `oid` | 테이블의 행 타입에 해당하는 데이터 타입의 OID; 인덱스, 시퀀스, TOAST 테이블의 경우 0 (`pg_type.oid` 참조) |
| `reloftype` | `oid` | 타입이 지정된 테이블의 경우 기본 복합 타입의 OID; 그 외의 경우 0 |
| `relowner` | `oid` | 릴레이션 소유자 (`pg_authid.oid` 참조) |
| `relam` | `oid` | 테이블이나 인덱스에 접근하는 데 사용되는 접근 방법 (`pg_am.oid` 참조) |
| `relfilenode` | `oid` | 디스크 파일의 이름; 0은 "매핑된" 릴레이션을 의미 |
| `reltablespace` | `oid` | 릴레이션이 저장된 테이블스페이스; 0은 기본 테이블스페이스를 의미 |
| `relpages` | `int4` | 페이지(BLCKSZ) 단위의 디스크 표현 크기; VACUUM, ANALYZE, CREATE INDEX에 의해 갱신되는 추정값 |
| `reltuples` | `float4` | 살아있는 행의 수; 플래너가 사용하는 추정값 (-1은 VACUUM/ANALYZE가 실행되지 않았음을 의미) |
| `relallvisible` | `int4` | 가시성 맵에서 all-visible로 표시된 페이지 수 |
| `relallfrozen` | `int4` | 가시성 맵에서 all-frozen으로 표시된 페이지 수 |
| `reltoastrelid` | `oid` | 연결된 TOAST 테이블의 OID; 없으면 0 |
| `relhasindex` | `bool` | 테이블이 인덱스를 가지고 있으면 true |
| `relisshared` | `bool` | 클러스터 내 모든 데이터베이스에서 공유되는 테이블이면 true |
| `relpersistence` | `char` | `p` = 영구(permanent), `u` = 비로그(unlogged), `t` = 임시(temporary) |
| `relkind` | `char` | 릴레이션 종류 (아래 표 참조) |
| `relnatts` | `int2` | 사용자 컬럼 수 (시스템 컬럼 제외) |
| `relchecks` | `int2` | CHECK 제약 조건 수 |
| `relhasrules` | `bool` | 테이블이 규칙을 가지고 있으면 true |
| `relhastriggers` | `bool` | 테이블이 트리거를 가지고 있으면 true |
| `relhassubclass` | `bool` | 테이블/인덱스가 상속 자식이나 파티션을 가지고 있으면 true |
| `relrowsecurity` | `bool` | 행 수준 보안이 활성화되면 true |
| `relforcerowsecurity` | `bool` | RLS가 테이블 소유자에게도 적용되면 true |
| `relispopulated` | `bool` | 릴레이션이 채워져 있으면 true |
| `relreplident` | `char` | 복제 아이덴티티: `d` = 기본(primary key), `n` = 없음, `f` = 모든 컬럼, `i` = 인덱스 |
| `relispartition` | `bool` | 테이블이나 인덱스가 파티션이면 true |
| `relrewrite` | `oid` | DDL 재작성 중 원본 릴레이션의 OID; 그 외에는 0 |
| `relfrozenxid` | `xid` | 동결된 행의 트랜잭션 ID 임계값 |
| `relminmxid` | `xid` | 동결된 멀티트랜잭션의 멀티트랜잭션 ID 임계값 |
| `relacl` | `aclitem[]` | 접근 권한 |
| `reloptions` | `text[]` | 접근 방법별 옵션 ("keyword=value" 형식의 문자열) |
| `relpartbound` | `pg_node_tree` | 파티션인 경우 파티션 경계의 내부 표현 |

#### relkind 값

| 값 | 설명 |
|----|------|
| `r` | 일반 테이블 (ordinary table) |
| `i` | 인덱스 (index) |
| `S` | 시퀀스 (sequence) |
| `t` | TOAST 테이블 |
| `v` | 뷰 (view) |
| `m` | 구체화된 뷰 (materialized view) |
| `c` | 복합 타입 (composite type) |
| `f` | 외부 테이블 (foreign table) |
| `p` | 파티션 테이블 (partitioned table) |
| `I` | 파티션 인덱스 (partitioned index) |

---

### pg_attribute

`pg_attribute` 카탈로그는 테이블 컬럼에 대한 정보를 저장합니다. 데이터베이스의 모든 테이블의 모든 컬럼에 대해 정확히 하나의 `pg_attribute` 행이 존재합니다.

> 참고: "속성(Attribute)"이라는 용어는 "컬럼(Column)"과 동일한 의미로, 역사적인 이유로 사용됩니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `attrelid` | `oid` | 이 컬럼이 속한 테이블 (`pg_class.oid` 참조) |
| `attname` | `name` | 컬럼 이름 |
| `atttypid` | `oid` | 데이터 타입 OID (`pg_type.oid` 참조); 삭제된 컬럼의 경우 0 |
| `attlen` | `int2` | 이 컬럼 타입의 `pg_type.typlen` 복사본 |
| `attnum` | `int2` | 컬럼 번호 (일반 컬럼: 1+, `ctid` 같은 시스템 컬럼: 음수) |
| `atttypmod` | `int4` | 타입별 데이터 (예: varchar의 최대 길이); 불필요한 경우 -1 |
| `attndims` | `int2` | 배열 차원 수; 배열이 아니면 0 |
| `attbyval` | `bool` | `pg_type.typbyval` 복사본 |
| `attalign` | `char` | `pg_type.typalign` 복사본 |
| `attstorage` | `char` | `pg_type.typstorage` 복사본; TOAST 가능 타입의 경우 변경 가능 |
| `attcompression` | `char` | 압축 방법: `'\0'` (기본값), `'p'` (pglz), `'l'` (LZ4) |
| `attnotnull` | `bool` | NOT NULL 제약 조건 보유 여부 |
| `atthasdef` | `bool` | 기본값이나 생성 표현식 보유 여부 (`pg_attrdef` 참조) |
| `atthasmissing` | `bool` | 행에서 컬럼이 완전히 누락된 경우 사용되는 누락 값 보유 여부 |
| `attidentity` | `char` | 아이덴티티 컬럼: `''` (없음), `'a'` (always), `'d'` (by default) |
| `attgenerated` | `char` | 생성된 컬럼: `''` (없음), `'s'` (stored), `'v'` (virtual) |
| `attisdropped` | `bool` | 삭제된 컬럼; 물리적으로 존재하지만 SQL로 접근 불가 |
| `attislocal` | `bool` | 릴레이션에서 로컬로 정의됨 (상속도 가능) |
| `attinhcount` | `int2` | 직접 조상의 수; 0이 아니면 삭제/이름 변경 방지 |
| `attcollation` | `oid` | 콜레이션 OID (`pg_collation.oid` 참조); 콜레이션 불가 타입이면 0 |
| `attstattarget` | `int2` | ANALYZE를 위한 통계 상세 수준 (0=없음, null=기본값, 양수=타입별) |
| `attacl` | `aclitem[]` | 컬럼 수준 접근 권한 |
| `attoptions` | `text[]` | 속성 옵션 ("keyword=value" 형식의 문자열) |
| `attfdwoptions` | `text[]` | 외부 데이터 래퍼 옵션 |
| `attmissingval` | `anyarray` | 누락 값이 있는 경우 하나의 요소를 가진 배열 |

---

### pg_type

`pg_type` 카탈로그는 PostgreSQL의 모든 데이터 타입에 대한 정보를 저장합니다. 기본 타입, 열거형 타입, 도메인, 복합 타입(각 테이블에 대해 자동 생성)이 포함됩니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 |
| `typname` | `name` | 데이터 타입 이름 |
| `typnamespace` | `oid` | 이 타입을 포함하는 네임스페이스의 OID |
| `typowner` | `oid` | 타입 소유자 |
| `typlen` | `int2` | 고정 크기 타입의 바이트 크기; 가변 길이의 경우 음수 (-1: varlena, -2: C 문자열) |
| `typbyval` | `bool` | 값이 값으로 전달되는지 참조로 전달되는지 여부 |
| `typtype` | `char` | 타입 분류자: `b` (기본), `c` (복합), `d` (도메인), `e` (열거형), `p` (의사), `r` (범위), `m` (다중범위) |
| `typcategory` | `char` | 암시적 캐스트 결정을 위한 타입 분류 |
| `typispreferred` | `bool` | 카테고리 내에서 선호되는 캐스트 대상 |
| `typisdefined` | `bool` | 타입이 정의되어 있으면 true |
| `typdelim` | `char` | 배열 요소 구분 문자 |
| `typrelid` | `oid` | 복합 타입의 경우 `pg_class` 참조 |
| `typsubscript` | `regproc` | 첨자 처리기 함수 OID |
| `typelem` | `oid` | 첨자를 위한 요소 타입 |
| `typarray` | `oid` | 이것을 요소로 하는 "진정한" 배열 타입 |
| `typinput` | `regproc` | 입력 변환 함수 (텍스트) |
| `typoutput` | `regproc` | 출력 변환 함수 (텍스트) |
| `typreceive` | `regproc` | 입력 변환 함수 (바이너리) |
| `typsend` | `regproc` | 출력 변환 함수 (바이너리) |
| `typmodin` | `regproc` | 타입 수정자 입력 함수 |
| `typmodout` | `regproc` | 타입 수정자 출력 함수 |
| `typanalyze` | `regproc` | 사용자 정의 ANALYZE 함수 |
| `typalign` | `char` | 저장 정렬: `c` (char), `s` (short), `i` (int), `d` (double) |
| `typstorage` | `char` | TOAST 전략: `p` (plain), `e` (external), `m` (main), `x` (extended) |
| `typnotnull` | `bool` | NOT NULL 제약 조건 (도메인 전용) |
| `typbasetype` | `oid` | 도메인의 기본 타입 |
| `typtypmod` | `int4` | 도메인 기본 타입의 타입 수정자 |
| `typndims` | `int4` | 배열에 대한 도메인의 배열 차원 |
| `typcollation` | `oid` | 콜레이션 OID |
| `typdefaultbin` | `pg_node_tree` | 기본 표현식의 바이너리 표현 |
| `typdefault` | `text` | 사람이 읽을 수 있는 기본값 |
| `typacl` | `aclitem[]` | 접근 권한 |

#### 타입 카테고리 (typcategory)

| 코드 | 카테고리 |
|------|----------|
| `A` | 배열 타입 (Array types) |
| `B` | 불리언 타입 (Boolean types) |
| `C` | 복합 타입 (Composite types) |
| `D` | 날짜/시간 타입 (Date/time types) |
| `E` | 열거형 타입 (Enum types) |
| `G` | 기하 타입 (Geometric types) |
| `I` | 네트워크 주소 타입 (Network address types) |
| `N` | 숫자 타입 (Numeric types) |
| `P` | 의사 타입 (Pseudo-types) |
| `R` | 범위 타입 (Range types) |
| `S` | 문자열 타입 (String types) |
| `T` | 시간 간격 타입 (Timespan types) |
| `U` | 사용자 정의 타입 (User-defined types) |
| `V` | 비트 문자열 타입 (Bit-string types) |
| `X` | `unknown` 타입 |
| `Z` | 내부 사용 타입 (Internal-use types) |

---

### pg_proc

`pg_proc` 시스템 카탈로그는 함수, 프로시저, 집계 함수, 윈도우 함수(통칭하여 루틴)에 대한 정보를 저장합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 |
| `proname` | `name` | 함수 이름 |
| `pronamespace` | `oid` | 이 함수를 포함하는 네임스페이스의 OID |
| `proowner` | `oid` | 함수 소유자 |
| `prolang` | `oid` | 구현 언어 또는 호출 인터페이스 |
| `procost` | `float4` | 예상 실행 비용 (cpu_operator_cost 단위) |
| `prorows` | `float4` | 예상 결과 행 수 |
| `provariadic` | `oid` | 가변 인자 배열 매개변수 요소의 데이터 타입 (없으면 0) |
| `prosupport` | `regproc` | 플래너 지원 함수 OID (없으면 0) |
| `prokind` | `char` | 함수 유형: `f` (일반), `p` (프로시저), `a` (집계), `w` (윈도우) |
| `prosecdef` | `bool` | 보안 정의자 함수 여부 (SECURITY DEFINER) |
| `proleakproof` | `bool` | 부작용이 없는 함수 여부 |
| `proisstrict` | `bool` | 인자가 NULL이면 NULL 반환 여부 |
| `proretset` | `bool` | 함수가 집합(여러 값)을 반환하는지 여부 |
| `provolatile` | `char` | 휘발성: `i` (immutable/불변), `s` (stable/안정), `v` (volatile/휘발) |
| `proparallel` | `char` | 병렬 안전성: `s` (safe/안전), `r` (restricted/제한), `u` (unsafe/안전하지 않음) |
| `pronargs` | `int2` | 입력 인자 수 |
| `pronargdefaults` | `int2` | 기본값이 있는 인자 수 |
| `prorettype` | `oid` | 반환 값의 데이터 타입 |
| `proargtypes` | `oidvector` | 함수 인자의 데이터 타입 (호출 시그니처) |
| `proallargtypes` | `oid[]` | 모든 인자의 데이터 타입 (OUT/INOUT 포함) |
| `proargmodes` | `char[]` | 인자 모드: `i` (IN), `o` (OUT), `b` (INOUT), `v` (VARIADIC), `t` (TABLE) |
| `proargnames` | `text[]` | 함수 인자의 이름 |
| `proargdefaults` | `pg_node_tree` | 마지막 N개 입력 인자의 기본값 표현식 |
| `protrftypes` | `oid[]` | TRANSFORM 절에 대한 인자/결과 타입 |
| `prosrc` | `text` | 함수 호출 세부 정보 (언어에 따라 다름) |
| `probin` | `text` | 추가 호출 정보 (동적 로드된 C 함수용) |
| `prosqlbody` | `pg_node_tree` | 사전 파싱된 SQL 함수 본문 |
| `proconfig` | `text[]` | 로컬 런타임 구성 설정 |
| `proacl` | `aclitem[]` | 접근 권한 |

#### prokind 값

| 값 | 설명 |
|----|------|
| `f` | 일반 함수 (normal function) |
| `p` | 프로시저 (procedure) |
| `a` | 집계 함수 (aggregate function) |
| `w` | 윈도우 함수 (window function) |

---

### pg_namespace

`pg_namespace` 카탈로그는 네임스페이스(Namespaces) 를 저장합니다. 네임스페이스는 SQL 스키마의 기반이 되는 구조입니다.

#### 핵심 개념

각 네임스페이스는 이름 충돌 없이 릴레이션, 타입, 기타 객체의 별도 컬렉션을 가질 수 있습니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 |
| `nspname` | `name` | 네임스페이스 이름 |
| `nspowner` | `oid` | 네임스페이스 소유자 (`pg_authid.oid` 참조) |
| `nspacl` | `aclitem[]` | 접근 권한 |

---

### pg_index

`pg_index` 카탈로그는 PostgreSQL의 인덱스에 대한 메타데이터를 포함합니다. 나머지 인덱스 정보는 `pg_class` 카탈로그에 저장됩니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `indexrelid` | `oid` | 이 인덱스의 `pg_class` 항목 OID |
| `indrelid` | `oid` | 이 인덱스가 속한 테이블의 `pg_class` 항목 OID |
| `indnatts` | `int2` | 인덱스의 총 컬럼 수 (키 + 포함 컬럼) |
| `indnkeyatts` | `int2` | 키 컬럼만의 수 (포함 컬럼 제외) |
| `indisunique` | `bool` | 유니크 인덱스이면 true |
| `indnullsnotdistinct` | `bool` | 유니크 인덱스에서: true면 NULL은 동일하게 취급; false(기본)면 NULL은 구별됨 |
| `indisprimary` | `bool` | 인덱스가 기본 키를 나타내면 true |
| `indisexclusion` | `bool` | 인덱스가 배제 제약 조건을 지원하면 true |
| `indimmediate` | `bool` | 삽입 시 유니크 검사가 즉시 수행되면 true |
| `indisclustered` | `bool` | 테이블이 마지막으로 이 인덱스로 클러스터링되었으면 true |
| `indisvalid` | `bool` | 인덱스가 쿼리에 유효하면 true; false면 불완전 |
| `indcheckxmin` | `bool` | 쿼리가 `xmin`이 `TransactionXmin` 아래가 될 때까지 기다려야 하면 true |
| `indisready` | `bool` | 인덱스가 삽입 준비가 되면 true |
| `indislive` | `bool` | 인덱스가 삭제 중이면 false |
| `indisreplident` | `bool` | 인덱스가 복제 아이덴티티로 선택되면 true |
| `indkey` | `int2vector` | 인덱싱된 테이블 컬럼 번호의 배열; 0은 표현식을 나타냄 |
| `indcollation` | `oidvector` | 각 키 컬럼의 콜레이션 OID |
| `indclass` | `oidvector` | 각 키 컬럼의 연산자 클래스 OID |
| `indoption` | `int2vector` | 컬럼별 플래그 비트 (접근 방법에 의해 의미가 정의됨) |
| `indexprs` | `pg_node_tree` | 단순 컬럼 참조가 아닌 속성에 대한 표현식 트리 |
| `indpred` | `pg_node_tree` | 부분 인덱스 술어에 대한 표현식 트리 (부분 인덱스가 아니면 NULL) |

---

### pg_constraint

`pg_constraint` 카탈로그는 PostgreSQL 테이블과 도메인에 대한 제약 조건 정의를 저장합니다.

#### 저장되는 제약 조건 유형
- CHECK 제약 조건
- NOT NULL 제약 조건
- PRIMARY KEY (기본 키) 제약 조건
- UNIQUE (유니크) 제약 조건
- FOREIGN KEY (외래 키) 제약 조건
- EXCLUSION (배제) 제약 조건
- 사용자 정의 제약 조건 트리거 (`CREATE CONSTRAINT TRIGGER`로 생성)
- 도메인에 대한 CHECK 제약 조건

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 |
| `conname` | `name` | 제약 조건 이름 (반드시 유니크할 필요 없음) |
| `connamespace` | `oid` | 이 제약 조건을 포함하는 네임스페이스의 OID |
| `contype` | `char` | 제약 조건 유형 (아래 표 참조) |
| `condeferrable` | `bool` | 제약 조건이 지연 가능한지 여부 |
| `condeferred` | `bool` | 제약 조건이 기본적으로 지연되는지 여부 |
| `conenforced` | `bool` | 제약 조건이 강제되는지 여부 |
| `convalidated` | `bool` | 제약 조건이 검증되었는지 여부 |
| `conrelid` | `oid` | 테이블 OID (테이블 제약 조건이 아니면 0) |
| `contypid` | `oid` | 도메인 OID (도메인 제약 조건이 아니면 0) |
| `conindid` | `oid` | 유니크/기본 키/외래 키/배제 제약 조건을 지원하는 인덱스 OID |
| `conparentid` | `oid` | 부모 파티션 테이블 제약 조건 OID (파티션 제약 조건이 아니면 0) |
| `confrelid` | `oid` | 외래 키의 참조 테이블 OID |
| `confupdtype` | `char` | 외래 키 갱신 동작 |
| `confdeltype` | `char` | 외래 키 삭제 동작 |
| `confmatchtype` | `char` | 외래 키 매치 유형: `f` (full), `p` (partial), `s` (simple) |
| `conislocal` | `bool` | 제약 조건이 로컬로 정의됨 (동시에 상속될 수 있음) |
| `coninhcount` | `int2` | 직접 상속 조상의 수 |
| `connoinherit` | `bool` | 상속 불가 제약 조건 |
| `conperiod` | `bool` | `WITHOUT OVERLAPS` 또는 `PERIOD`로 정의된 제약 조건 |
| `conkey` | `int2[]` | 제약된 컬럼 속성 번호 목록 |
| `confkey` | `int2[]` | 참조된 컬럼 속성 번호 목록 (외래 키) |
| `conpfeqop` | `oid[]` | PK = FK 비교를 위한 동등 연산자 |
| `conppeqop` | `oid[]` | PK = PK 비교를 위한 동등 연산자 |
| `conffeqop` | `oid[]` | FK = FK 비교를 위한 동등 연산자 |
| `confdelsetcols` | `int2[]` | `SET NULL`/`SET DEFAULT` 삭제 동작에 의해 갱신되는 컬럼 |
| `conexclop` | `oid[]` | 배제 제약 조건을 위한 컬럼별 배제 연산자 |
| `conbin` | `pg_node_tree` | CHECK 제약 조건 표현식의 내부 표현 (`pg_get_constraintdef()` 사용하여 추출) |

#### contype 값

| 값 | 설명 |
|----|------|
| `c` | CHECK 제약 조건 |
| `f` | FOREIGN KEY (외래 키) 제약 조건 |
| `n` | NOT NULL 제약 조건 |
| `p` | PRIMARY KEY (기본 키) 제약 조건 |
| `u` | UNIQUE (유니크) 제약 조건 |
| `t` | 제약 조건 트리거 (Constraint trigger) |
| `x` | EXCLUSION (배제) 제약 조건 |

#### 외래 키 동작 코드 (confupdtype, confdeltype)

| 값 | 설명 |
|----|------|
| `a` | NO ACTION (아무 동작 없음) |
| `r` | RESTRICT (제한) |
| `c` | CASCADE (연쇄) |
| `n` | SET NULL (NULL로 설정) |
| `d` | SET DEFAULT (기본값으로 설정) |

---

### pg_database

`pg_database` 카탈로그는 PostgreSQL 클러스터에서 사용 가능한 데이터베이스에 대한 정보를 저장합니다. 이것은 공유 카탈로그(Shared Catalog) 입니다 - 데이터베이스마다 하나가 아니라 클러스터당 하나의 복사본만 존재합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | `oid` | 행 식별자 |
| `datname` | `name` | 데이터베이스 이름 |
| `datdba` | `oid` | 데이터베이스 소유자 (`pg_authid.oid` 참조) |
| `encoding` | `int4` | 문자 인코딩 (`pg_encoding_to_char()` 사용하여 변환) |
| `datlocprovider` | `char` | 로케일 제공자: `b` = builtin, `c` = libc, `i` = icu |
| `datistemplate` | `bool` | true이면 `CREATEDB` 권한을 가진 사용자가 복제 가능 |
| `datallowconn` | `bool` | false이면 아무도 연결 불가 (`template0` 보호용) |
| `dathasloginevt` | `bool` | 로그인 이벤트 트리거 존재 여부 (내부 사용 전용) |
| `datconnlimit` | `int4` | 최대 동시 연결 수 (-1 = 제한 없음, -2 = 유효하지 않음) |
| `datfrozenxid` | `xid` | VACUUM 추적을 위한 최소 동결 트랜잭션 ID |
| `datminmxid` | `xid` | VACUUM 추적을 위한 최소 멀티트랜잭션 ID |
| `dattablespace` | `oid` | 기본 테이블스페이스 (`pg_tablespace.oid` 참조) |
| `datcollate` | `text` | LC_COLLATE 설정 |
| `datctype` | `text` | LC_CTYPE 설정 |
| `datlocale` | `text` | 콜레이션 제공자 로케일 이름 (libc의 경우 NULL) |
| `daticurules` | `text` | ICU 콜레이션 규칙 |
| `datcollversion` | `text` | 제공자별 콜레이션 버전 |
| `datacl` | `aclitem[]` | 접근 권한 |

---

## 시스템 카탈로그 활용 예제

### 1. 테이블 정보 조회

```sql
-- 특정 스키마의 모든 테이블 조회
SELECT
    c.relname AS table_name,
    n.nspname AS schema_name,
    c.reltuples AS estimated_rows,
    c.relpages AS pages
FROM pg_class c
JOIN pg_namespace n ON c.relnamespace = n.oid
WHERE c.relkind = 'r'  -- 일반 테이블만
  AND n.nspname = 'public'
ORDER BY c.relname;
```

### 2. 컬럼 정보 조회

```sql
-- 특정 테이블의 모든 컬럼 정보 조회
SELECT
    a.attname AS column_name,
    t.typname AS data_type,
    a.attlen AS length,
    a.attnotnull AS not_null,
    a.attnum AS column_position
FROM pg_attribute a
JOIN pg_class c ON a.attrelid = c.oid
JOIN pg_type t ON a.atttypid = t.oid
WHERE c.relname = 'your_table_name'
  AND a.attnum > 0  -- 시스템 컬럼 제외
  AND NOT a.attisdropped  -- 삭제된 컬럼 제외
ORDER BY a.attnum;
```

### 3. 인덱스 정보 조회

```sql
-- 테이블의 인덱스 정보 조회
SELECT
    i.relname AS index_name,
    t.relname AS table_name,
    ix.indisunique AS is_unique,
    ix.indisprimary AS is_primary,
    array_agg(a.attname ORDER BY k.ord) AS indexed_columns
FROM pg_index ix
JOIN pg_class i ON i.oid = ix.indexrelid
JOIN pg_class t ON t.oid = ix.indrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
JOIN LATERAL unnest(ix.indkey) WITH ORDINALITY AS k(attnum, ord) ON true
JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
WHERE n.nspname = 'public'
  AND t.relname = 'your_table_name'
GROUP BY i.relname, t.relname, ix.indisunique, ix.indisprimary;
```

### 4. 제약 조건 정보 조회

```sql
-- 테이블의 모든 제약 조건 조회
SELECT
    con.conname AS constraint_name,
    CASE con.contype
        WHEN 'c' THEN 'CHECK'
        WHEN 'f' THEN 'FOREIGN KEY'
        WHEN 'n' THEN 'NOT NULL'
        WHEN 'p' THEN 'PRIMARY KEY'
        WHEN 'u' THEN 'UNIQUE'
        WHEN 'x' THEN 'EXCLUSION'
    END AS constraint_type,
    pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con
JOIN pg_class c ON c.oid = con.conrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname = 'your_table_name';
```

### 5. 함수 정보 조회

```sql
-- 특정 스키마의 사용자 정의 함수 조회
SELECT
    p.proname AS function_name,
    n.nspname AS schema_name,
    l.lanname AS language,
    CASE p.prokind
        WHEN 'f' THEN 'function'
        WHEN 'p' THEN 'procedure'
        WHEN 'a' THEN 'aggregate'
        WHEN 'w' THEN 'window'
    END AS kind,
    pg_get_function_arguments(p.oid) AS arguments,
    t.typname AS return_type
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
JOIN pg_language l ON p.prolang = l.oid
JOIN pg_type t ON p.prorettype = t.oid
WHERE n.nspname = 'public'
ORDER BY p.proname;
```

### 6. 데이터 타입 정보 조회

```sql
-- 사용자 정의 타입 조회
SELECT
    t.typname AS type_name,
    n.nspname AS schema_name,
    CASE t.typtype
        WHEN 'b' THEN 'base'
        WHEN 'c' THEN 'composite'
        WHEN 'd' THEN 'domain'
        WHEN 'e' THEN 'enum'
        WHEN 'r' THEN 'range'
    END AS type_kind,
    t.typlen AS length
FROM pg_type t
JOIN pg_namespace n ON t.typnamespace = n.oid
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND t.typtype IN ('b', 'c', 'd', 'e', 'r')
ORDER BY t.typname;
```

### 7. 테이블과 인덱스 크기 조회

```sql
-- 테이블과 관련 인덱스의 크기 조회
SELECT
    c.relname AS name,
    CASE c.relkind
        WHEN 'r' THEN 'table'
        WHEN 'i' THEN 'index'
    END AS type,
    pg_size_pretty(pg_relation_size(c.oid)) AS size,
    c.relpages AS pages,
    c.reltuples AS estimated_rows
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind IN ('r', 'i')
ORDER BY pg_relation_size(c.oid) DESC;
```

### 8. 외래 키 관계 조회

```sql
-- 모든 외래 키 관계 조회
SELECT
    tc.relname AS child_table,
    STRING_AGG(ac.attname, ', ' ORDER BY x.n) AS child_columns,
    tp.relname AS parent_table,
    STRING_AGG(ap.attname, ', ' ORDER BY x.n) AS parent_columns,
    c.conname AS constraint_name
FROM pg_constraint c
JOIN pg_class tc ON c.conrelid = tc.oid
JOIN pg_class tp ON c.confrelid = tp.oid
JOIN pg_namespace n ON tc.relnamespace = n.oid
CROSS JOIN LATERAL unnest(c.conkey, c.confkey) WITH ORDINALITY AS x(child_attnum, parent_attnum, n)
JOIN pg_attribute ac ON ac.attrelid = tc.oid AND ac.attnum = x.child_attnum
JOIN pg_attribute ap ON ap.attrelid = tp.oid AND ap.attnum = x.parent_attnum
WHERE c.contype = 'f'
  AND n.nspname = 'public'
GROUP BY tc.relname, tp.relname, c.conname;
```

### 9. 스키마별 객체 수 조회

```sql
-- 각 스키마의 객체 수 조회
SELECT
    n.nspname AS schema_name,
    COUNT(CASE WHEN c.relkind = 'r' THEN 1 END) AS tables,
    COUNT(CASE WHEN c.relkind = 'i' THEN 1 END) AS indexes,
    COUNT(CASE WHEN c.relkind = 'v' THEN 1 END) AS views,
    COUNT(CASE WHEN c.relkind = 'S' THEN 1 END) AS sequences
FROM pg_namespace n
LEFT JOIN pg_class c ON c.relnamespace = n.oid
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
GROUP BY n.nspname
ORDER BY n.nspname;
```

### 10. 데이터베이스 정보 조회

```sql
-- 모든 데이터베이스 정보 조회
SELECT
    datname AS database_name,
    pg_encoding_to_char(encoding) AS encoding,
    datcollate AS collation,
    datconnlimit AS connection_limit,
    pg_size_pretty(pg_database_size(datname)) AS size
FROM pg_database
WHERE datistemplate = false
ORDER BY pg_database_size(datname) DESC;
```

---

## 주의사항

1. 직접 수정 금지: 시스템 카탈로그를 직접 INSERT, UPDATE, DELETE하지 마세요. 대신 표준 DDL 명령어를 사용하세요.

2. 지연된 플래그 갱신: `relhasindex`, `relhasrules`, `relhastriggers`, `relhassubclass` 같은 불리언 플래그는 지연되어 갱신됩니다. 조건이 충족되면 true로 설정되지만, 조건이 해제되어도 즉시 false로 재설정되지 않을 수 있습니다.

3. 추정값 이해: `relpages`, `reltuples`, `relallvisible`, `relallfrozen`은 VACUUM, ANALYZE, 특정 DDL 명령에 의해 갱신되는 추정값 입니다.

4. 공유 카탈로그: `pg_database`, `pg_authid`, `pg_tablespace` 같은 일부 카탈로그는 클러스터 전체에서 공유됩니다.

5. information_schema 활용: 시스템 카탈로그에 직접 접근하는 대신, SQL 표준인 `information_schema`를 사용하는 것이 이식성이 더 좋습니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - System Catalogs](https://www.postgresql.org/docs/current/catalogs.html)
- [PostgreSQL 공식 문서 - pg_class](https://www.postgresql.org/docs/current/catalog-pg-class.html)
- [PostgreSQL 공식 문서 - pg_attribute](https://www.postgresql.org/docs/current/catalog-pg-attribute.html)
- [PostgreSQL 공식 문서 - pg_type](https://www.postgresql.org/docs/current/catalog-pg-type.html)
- [PostgreSQL 공식 문서 - pg_proc](https://www.postgresql.org/docs/current/catalog-pg-proc.html)
- [PostgreSQL 공식 문서 - pg_namespace](https://www.postgresql.org/docs/current/catalog-pg-namespace.html)
