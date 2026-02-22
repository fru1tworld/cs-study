# 제54장. 시스템 뷰 (System Views)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/views.html

PostgreSQL은 시스템 카탈로그와 내부 서버 상태에 대한 편리한 접근을 제공하는 내장 시스템 뷰(System Views)를 제공합니다. 이 장에서는 PostgreSQL에서 제공하는 다양한 시스템 뷰의 개요와 주요 뷰들의 상세 정보를 설명합니다.

---

## 목차

- [54.1 개요](#541-개요)
- [54.2 시스템 뷰 목록](#542-시스템-뷰-목록)
- [54.3 주요 시스템 뷰](#543-주요-시스템-뷰)
  - [54.3.1 pg_stat_activity](#5431-pg_stat_activity)
  - [54.3.2 pg_locks](#5432-pg_locks)
  - [54.3.3 pg_settings](#5433-pg_settings)
  - [54.3.4 pg_tables](#5434-pg_tables)
  - [54.3.5 pg_indexes](#5435-pg_indexes)
  - [54.3.6 pg_views](#5436-pg_views)
  - [54.3.7 pg_roles](#5437-pg_roles)
  - [54.3.8 pg_stats](#5438-pg_stats)
  - [54.3.9 pg_cursors](#5439-pg_cursors)
  - [54.3.10 pg_prepared_statements](#54310-pg_prepared_statements)
  - [54.3.11 pg_replication_slots](#54311-pg_replication_slots)
  - [54.3.12 pg_sequences](#54312-pg_sequences)
  - [54.3.13 pg_matviews](#54313-pg_matviews)
  - [54.3.14 pg_policies](#54314-pg_policies)
  - [54.3.15 pg_rules](#54315-pg_rules)
  - [54.3.16 pg_available_extensions](#54316-pg_available_extensions)
- [54.4 통계 뷰](#544-통계-뷰)
- [54.5 유용한 쿼리 예제](#545-유용한-쿼리-예제)

---

## 54.1 개요

PostgreSQL 시스템 뷰는 두 가지 범주로 나눌 수 있습니다:

### 1. 카탈로그 접근 뷰 (Catalog Access Views)

시스템 카탈로그에 대한 자주 사용되는 쿼리에 편리하게 접근할 수 있도록 합니다:

- `pg_tables` - 테이블 정보
- `pg_indexes` - 인덱스 정보
- `pg_views` - 뷰 정의
- `pg_roles` - 데이터베이스 역할
- `pg_sequences` - 시퀀스 정보
- `pg_matviews` - 구체화된 뷰 정보

### 2. 서버 상태 뷰 (Server State Views)

내부 서버 상태 정보에 대한 접근을 제공합니다:

- `pg_stat_activity` - 현재 활동 중인 세션
- `pg_locks` - 현재 잠금 상태
- `pg_settings` - 서버 설정
- `pg_cursors` - 현재 커서
- `pg_prepared_statements` - 준비된 문장

### 정보 스키마 (Information Schema)와의 비교

PostgreSQL은 정보 스키마(Information Schema) 도 제공합니다. 정보 스키마는 SQL 표준을 따르는 뷰를 제공하며, 필요한 정보가 모두 제공되는 경우 PostgreSQL 전용 시스템 뷰 대신 사용하는 것이 좋습니다. 이는 다른 데이터베이스 시스템과의 이식성을 보장하기 때문입니다.

---

## 54.2 시스템 뷰 목록

PostgreSQL은 다음과 같은 시스템 뷰들을 제공합니다:

| 뷰 이름 | 설명 |
|---------|------|
| `pg_aios` | 비동기 I/O 작업 (Asynchronous I/O operations) |
| `pg_available_extensions` | 설치 가능한 확장 모듈 |
| `pg_available_extension_versions` | 설치 가능한 확장 모듈 버전 |
| `pg_backend_memory_contexts` | 백엔드 메모리 컨텍스트 |
| `pg_config` | 설정 정보 |
| `pg_cursors` | 현재 활성 커서 |
| `pg_file_settings` | 파일 기반 설정 |
| `pg_group` | 사용자 그룹 (호환성용) |
| `pg_hba_file_rules` | HBA 파일 규칙 |
| `pg_ident_file_mappings` | Ident 파일 매핑 |
| `pg_indexes` | 인덱스 정보 |
| `pg_locks` | 현재 잠금 상태 |
| `pg_matviews` | 구체화된 뷰 |
| `pg_policies` | 행 수준 보안 정책 |
| `pg_prepared_statements` | 준비된 문장 |
| `pg_prepared_xacts` | 준비된 트랜잭션 |
| `pg_publication_tables` | 게시 테이블 |
| `pg_replication_origin_status` | 복제 원본 상태 |
| `pg_replication_slots` | 복제 슬롯 |
| `pg_roles` | 데이터베이스 역할 |
| `pg_rules` | 규칙 정의 |
| `pg_seclabels` | 보안 레이블 |
| `pg_sequences` | 시퀀스 정보 |
| `pg_settings` | 서버 설정(GUC) |
| `pg_shadow` | 사용자 비밀번호 정보 (구식) |
| `pg_shmem_allocations` | 공유 메모리 할당 |
| `pg_shmem_allocations_numa` | NUMA 공유 메모리 할당 |
| `pg_stats` | 테이블 통계 |
| `pg_stats_ext` | 확장 통계 |
| `pg_stats_ext_exprs` | 확장 통계 표현식 |
| `pg_tables` | 테이블 정보 |
| `pg_timezone_abbrevs` | 시간대 약어 |
| `pg_timezone_names` | 시간대 이름 |
| `pg_user` | 데이터베이스 사용자 |
| `pg_user_mappings` | 외부 서버 사용자 매핑 |
| `pg_views` | 뷰 정의 |
| `pg_wait_events` | 대기 이벤트 정보 |

---

## 54.3 주요 시스템 뷰

### 54.3.1 pg_stat_activity

`pg_stat_activity` 뷰는 서버 프로세스와 현재 활동에 대한 실시간 정보를 제공합니다. 각 서버 프로세스당 하나의 행이 포함됩니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `datid` | `oid` | 백엔드가 연결된 데이터베이스 OID |
| `datname` | `name` | 백엔드가 연결된 데이터베이스 이름 |
| `pid` | `integer` | 백엔드 프로세스 ID |
| `leader_pid` | `integer` | 병렬 그룹 리더의 프로세스 ID |
| `usesysid` | `oid` | 이 백엔드에 로그인한 사용자의 OID |
| `usename` | `name` | 이 백엔드에 로그인한 사용자 이름 |
| `application_name` | `text` | 연결된 애플리케이션 이름 |
| `client_addr` | `inet` | 연결된 클라이언트의 IP 주소 |
| `client_hostname` | `text` | 연결된 클라이언트의 호스트 이름 |
| `client_port` | `integer` | 클라이언트가 사용하는 TCP 포트 번호 |
| `backend_start` | `timestamptz` | 이 프로세스가 시작된 시간 |
| `xact_start` | `timestamptz` | 현재 트랜잭션이 시작된 시간 |
| `query_start` | `timestamptz` | 현재 활성 쿼리가 시작된 시간 |
| `state_change` | `timestamptz` | 상태가 마지막으로 변경된 시간 |
| `wait_event_type` | `text` | 백엔드가 대기 중인 이벤트 유형 |
| `wait_event` | `text` | 대기 중인 특정 이벤트 이름 |
| `state` | `text` | 현재 백엔드 상태 |
| `backend_xid` | `xid` | 최상위 트랜잭션 식별자 |
| `backend_xmin` | `xid` | 현재 백엔드의 xmin 호라이즌 |
| `query_id` | `bigint` | 현재 쿼리의 식별자 |
| `query` | `text` | 가장 최근에 실행된 쿼리 텍스트 |
| `backend_type` | `text` | 백엔드 유형 |

#### state 컬럼 값

| 값 | 설명 |
|----|------|
| `active` | 백엔드가 쿼리를 실행 중 |
| `idle` | 백엔드가 새 클라이언트 명령을 대기 중 |
| `idle in transaction` | 트랜잭션 내에서 명령을 대기 중 |
| `idle in transaction (aborted)` | 실패한 트랜잭션 내에서 대기 중 |
| `fastpath function call` | 빠른 경로 함수를 실행 중 |
| `disabled` | track_activities가 비활성화됨 |

#### 예제

```sql
-- 현재 활성 쿼리 확인
SELECT pid, usename, datname, state, query
FROM pg_stat_activity
WHERE state = 'active';

-- 특정 시간 이상 실행 중인 쿼리 찾기
SELECT pid, usename, datname,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > interval '5 minutes';

-- 대기 중인 프로세스 확인
SELECT pid, usename, wait_event_type, wait_event, query
FROM pg_stat_activity
WHERE wait_event IS NOT NULL;

-- 데이터베이스별 연결 수 확인
SELECT datname, count(*) AS connections
FROM pg_stat_activity
GROUP BY datname;
```

---

### 54.3.2 pg_locks

`pg_locks` 뷰는 PostgreSQL 데이터베이스 서버 내에서 활성 프로세스가 보유한 잠금에 대한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `locktype` | `text` | 잠금 가능한 객체 유형 |
| `database` | `oid` | 잠금 대상이 포함된 데이터베이스 OID |
| `relation` | `oid` | 잠금 대상 릴레이션의 OID |
| `page` | `int4` | 릴레이션 내 페이지 번호 |
| `tuple` | `int2` | 페이지 내 튜플 번호 |
| `virtualxid` | `text` | 잠금 대상 트랜잭션의 가상 ID |
| `transactionid` | `xid` | 잠금 대상 트랜잭션 ID |
| `classid` | `oid` | 잠금 대상이 포함된 시스템 카탈로그 OID |
| `objid` | `oid` | 시스템 카탈로그 내 잠금 대상 OID |
| `objsubid` | `int2` | 잠금 대상 컬럼 번호 |
| `virtualtransaction` | `text` | 잠금을 보유/대기 중인 트랜잭션의 가상 ID |
| `pid` | `int4` | 잠금을 보유/대기 중인 프로세스 ID |
| `mode` | `text` | 보유 또는 요청 중인 잠금 모드 이름 |
| `granted` | `bool` | 잠금 보유 여부 (true: 보유, false: 대기) |
| `fastpath` | `bool` | 빠른 경로를 통해 획득한 잠금 여부 |
| `waitstart` | `timestamptz` | 프로세스가 잠금 대기를 시작한 시간 |

#### locktype 값

| 값 | 설명 |
|----|------|
| `relation` | 테이블 전체에 대한 잠금 |
| `extend` | 릴레이션 확장 권한 |
| `frozenid` | pg_database.datfrozenxid 업데이트 권한 |
| `page` | 릴레이션의 개별 페이지 |
| `tuple` | 릴레이션의 개별 튜플 |
| `transactionid` | 트랜잭션 ID |
| `virtualxid` | 가상 트랜잭션 ID |
| `spectoken` | 투기적 삽입 토큰 |
| `object` | 일반 데이터베이스 객체 |
| `userlock` | 사용자 정의 잠금 |
| `advisory` | 자문 잠금 |
| `applytransaction` | 트랜잭션 적용 |

#### 예제

```sql
-- 모든 잠금 정보 확인
SELECT * FROM pg_locks;

-- pg_stat_activity와 조인하여 세션 정보와 함께 확인
SELECT pl.locktype, pl.relation::regclass, pl.mode, pl.granted,
       psa.pid, psa.usename, psa.query
FROM pg_locks pl
LEFT JOIN pg_stat_activity psa ON pl.pid = psa.pid
WHERE pl.relation IS NOT NULL;

-- 대기 중인 잠금 확인
SELECT pl.pid, pl.virtualtransaction, pl.locktype,
       pl.mode, pl.relation::regclass, psa.query
FROM pg_locks pl
JOIN pg_stat_activity psa ON pl.pid = psa.pid
WHERE NOT pl.granted;

-- 차단 중인 프로세스 식별
SELECT blocked.pid AS blocked_pid,
       blocked.query AS blocked_query,
       blocking.pid AS blocking_pid,
       blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid
JOIN pg_locks blocking_locks ON blocked_locks.relation = blocking_locks.relation
                            AND blocked_locks.pid != blocking_locks.pid
JOIN pg_stat_activity blocking ON blocking_locks.pid = blocking.pid
WHERE NOT blocked_locks.granted;

-- pg_blocking_pids() 함수를 사용한 차단 프로세스 확인
SELECT pid, pg_blocking_pids(pid) AS blocked_by, query
FROM pg_stat_activity
WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

---

### 54.3.3 pg_settings

`pg_settings` 뷰는 서버의 런타임 설정 매개변수에 대한 접근을 제공합니다. `SHOW`와 `SET` 명령의 대안적인 인터페이스이며, 추가적인 메타데이터를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `name` | `text` | 런타임 설정 매개변수 이름 |
| `setting` | `text` | 매개변수의 현재 값 |
| `unit` | `text` | 매개변수의 암묵적 단위 |
| `category` | `text` | 매개변수의 논리적 그룹 |
| `short_desc` | `text` | 매개변수에 대한 간략한 설명 |
| `extra_desc` | `text` | 추가 상세 설명 |
| `context` | `text` | 매개변수 설정에 필요한 컨텍스트 |
| `vartype` | `text` | 매개변수 유형 |
| `source` | `text` | 현재 매개변수 값의 출처 |
| `min_val` | `text` | 최소 허용 값 |
| `max_val` | `text` | 최대 허용 값 |
| `enumvals` | `text[]` | enum 매개변수의 허용 값들 |
| `boot_val` | `text` | 서버 시작 시 가정되는 값 |
| `reset_val` | `text` | RESET이 재설정할 값 |
| `sourcefile` | `text` | 값이 설정된 설정 파일 |
| `sourceline` | `int4` | 설정 파일 내 줄 번호 |
| `pending_restart` | `bool` | 변경되었지만 재시작이 필요한지 여부 |

#### context 컬럼 값 (변경 난이도 순)

| 값 | 설명 |
|----|------|
| `internal` | 직접 변경 불가, 내부적으로 결정된 값 |
| `postmaster` | 서버 시작 시에만 설정 가능, 재시작 필요 |
| `sighup` | postgresql.conf에서 SIGHUP 신호로 변경 가능 |
| `superuser-backend` | 슈퍼유저만 세션 시작 시 설정 가능 |
| `backend` | 모든 사용자가 세션 시작 시 설정 가능 |
| `superuser` | 슈퍼유저만 SET으로 변경 가능 |
| `user` | 모든 사용자가 SET으로 변경 가능 |

#### 예제

```sql
-- 모든 설정 확인
SELECT name, setting, unit, category
FROM pg_settings;

-- 메모리 관련 설정 확인
SELECT name, setting, unit, short_desc
FROM pg_settings
WHERE category LIKE '%Memory%';

-- 재시작이 필요한 변경된 설정 확인
SELECT name, setting, boot_val
FROM pg_settings
WHERE pending_restart = true;

-- 특정 설정 변경 (pg_settings 업데이트)
UPDATE pg_settings
SET setting = '100'
WHERE name = 'work_mem';
-- 이는 SET work_mem = '100'과 동일

-- 설정 파일에서 설정된 값 확인
SELECT name, setting, sourcefile, sourceline
FROM pg_settings
WHERE sourcefile IS NOT NULL;

-- 기본값과 다른 설정 확인
SELECT name, setting, boot_val, source
FROM pg_settings
WHERE setting != boot_val;
```

---

### 54.3.4 pg_tables

`pg_tables` 뷰는 데이터베이스의 각 테이블에 대한 유용한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 테이블을 포함하는 스키마 이름 |
| `tablename` | `name` | 테이블 이름 |
| `tableowner` | `name` | 테이블 소유자 이름 |
| `tablespace` | `name` | 테이블이 포함된 테이블스페이스 이름 (기본값인 경우 null) |
| `hasindexes` | `bool` | 테이블에 인덱스가 있는지 여부 |
| `hasrules` | `bool` | 테이블에 규칙이 있는지 여부 |
| `hastriggers` | `bool` | 테이블에 트리거가 있는지 여부 |
| `rowsecurity` | `bool` | 행 수준 보안이 활성화되어 있는지 여부 |

#### 예제

```sql
-- 모든 사용자 테이블 확인
SELECT schemaname, tablename, tableowner
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');

-- 인덱스가 없는 테이블 확인
SELECT schemaname, tablename
FROM pg_tables
WHERE hasindexes = false
  AND schemaname NOT IN ('pg_catalog', 'information_schema');

-- 행 수준 보안이 활성화된 테이블 확인
SELECT schemaname, tablename
FROM pg_tables
WHERE rowsecurity = true;

-- 특정 스키마의 테이블 목록
SELECT tablename, tableowner
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
```

---

### 54.3.5 pg_indexes

`pg_indexes` 뷰는 데이터베이스의 각 인덱스에 대한 유용한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 테이블과 인덱스를 포함하는 스키마 이름 |
| `tablename` | `name` | 인덱스가 적용된 테이블 이름 |
| `indexname` | `name` | 인덱스 이름 |
| `tablespace` | `name` | 인덱스가 포함된 테이블스페이스 이름 |
| `indexdef` | `text` | 인덱스 정의 (재구성된 CREATE INDEX 명령) |

#### 예제

```sql
-- 특정 테이블의 인덱스 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users';

-- 스키마별 인덱스 수 확인
SELECT schemaname, count(*) AS index_count
FROM pg_indexes
GROUP BY schemaname;

-- 모든 사용자 정의 인덱스와 정의 확인
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY schemaname, tablename;

-- B-tree 인덱스만 확인
SELECT indexname, tablename, indexdef
FROM pg_indexes
WHERE indexdef LIKE '%btree%';
```

---

### 54.3.6 pg_views

`pg_views` 뷰는 데이터베이스의 각 뷰에 대한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 뷰를 포함하는 스키마 이름 |
| `viewname` | `name` | 뷰 이름 |
| `viewowner` | `name` | 뷰 소유자 이름 |
| `definition` | `text` | 뷰 정의 (재구성된 SELECT 쿼리) |

#### 예제

```sql
-- 모든 사용자 정의 뷰 확인
SELECT schemaname, viewname, viewowner, definition
FROM pg_views
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');

-- 특정 뷰의 정의 확인
SELECT definition
FROM pg_views
WHERE viewname = 'my_view';

-- 특정 테이블을 참조하는 뷰 찾기
SELECT viewname, definition
FROM pg_views
WHERE definition LIKE '%users%';
```

---

### 54.3.7 pg_roles

`pg_roles` 뷰는 데이터베이스 역할에 대한 정보를 제공합니다. `pg_authid` 카탈로그 테이블의 공개적으로 읽을 수 있는 뷰이며, 보안을 위해 비밀번호 필드가 숨겨져 있습니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `rolname` | `name` | 역할 이름 |
| `rolsuper` | `bool` | 슈퍼유저 권한 여부 |
| `rolinherit` | `bool` | 멤버인 역할의 권한을 자동으로 상속하는지 여부 |
| `rolcreaterole` | `bool` | 역할 생성 가능 여부 |
| `rolcreatedb` | `bool` | 데이터베이스 생성 가능 여부 |
| `rolcanlogin` | `bool` | 로그인 가능 여부 |
| `rolreplication` | `bool` | 복제 역할 여부 |
| `rolconnlimit` | `int4` | 최대 동시 연결 수 (-1은 제한 없음) |
| `rolpassword` | `text` | 비밀번호 (항상 ``로 표시) |
| `rolvaliduntil` | `timestamptz` | 비밀번호 만료 시간 |
| `rolbypassrls` | `bool` | 행 수준 보안 정책 우회 여부 |
| `rolconfig` | `text[]` | 역할별 런타임 설정 기본값 |
| `oid` | `oid` | 역할 ID |

#### 예제

```sql
-- 모든 역할 확인
SELECT rolname, rolsuper, rolcreatedb, rolcanlogin
FROM pg_roles;

-- 로그인 가능한 역할만 확인
SELECT rolname, rolconnlimit, rolvaliduntil
FROM pg_roles
WHERE rolcanlogin = true;

-- 슈퍼유저 역할 확인
SELECT rolname
FROM pg_roles
WHERE rolsuper = true;

-- 연결 제한이 있는 역할 확인
SELECT rolname, rolconnlimit
FROM pg_roles
WHERE rolconnlimit > 0;
```

---

### 54.3.8 pg_stats

`pg_stats` 뷰는 `pg_statistic` 카탈로그에 저장된 정보를 더 읽기 쉬운 형태로 제공합니다. 사용자가 읽기 권한이 있는 테이블에 대한 통계만 접근할 수 있습니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 테이블을 포함하는 스키마 이름 |
| `tablename` | `name` | 테이블 이름 |
| `attname` | `name` | 컬럼 이름 |
| `inherited` | `bool` | 자식 테이블의 값도 포함하는지 여부 |
| `null_frac` | `float4` | null인 항목의 비율 |
| `avg_width` | `int4` | 컬럼 항목의 평균 바이트 너비 |
| `n_distinct` | `float4` | 고유 값의 추정 개수 |
| `most_common_vals` | `anyarray` | 가장 흔한 값 목록 |
| `most_common_freqs` | `float4[]` | 가장 흔한 값의 빈도 |
| `histogram_bounds` | `anyarray` | 히스토그램 경계 값 |
| `correlation` | `float4` | 물리적 행 순서와 논리적 순서 간의 상관관계 |
| `most_common_elems` | `anyarray` | 가장 자주 나타나는 요소 값 (배열 유형) |
| `most_common_elem_freqs` | `float4[]` | 가장 흔한 요소 값의 빈도 |
| `elem_count_histogram` | `float4[]` | 고유 요소 개수 히스토그램 |
| `range_length_histogram` | `anyarray` | 범위 값 길이 히스토그램 (range 타입) |
| `range_empty_frac` | `float4` | 빈 범위 항목의 비율 (range 타입) |
| `range_bounds_histogram` | `anyarray` | 범위 경계 히스토그램 (range 타입) |

#### 예제

```sql
-- 특정 테이블의 컬럼 통계 확인
SELECT attname, null_frac, n_distinct, avg_width
FROM pg_stats
WHERE tablename = 'users';

-- null 비율이 높은 컬럼 찾기
SELECT schemaname, tablename, attname, null_frac
FROM pg_stats
WHERE null_frac > 0.5
ORDER BY null_frac DESC;

-- 가장 흔한 값 확인
SELECT attname, most_common_vals, most_common_freqs
FROM pg_stats
WHERE tablename = 'orders'
  AND most_common_vals IS NOT NULL;
```

---

### 54.3.9 pg_cursors

`pg_cursors` 뷰는 현재 사용 가능한 모든 커서를 나열합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `name` | `text` | 커서 이름 |
| `statement` | `text` | 커서를 선언하기 위해 제출된 쿼리 문자열 |
| `is_holdable` | `bool` | 트랜잭션 커밋 후에도 접근 가능한지 여부 |
| `is_binary` | `bool` | BINARY로 선언되었는지 여부 |
| `is_scrollable` | `bool` | 비순차적 행 검색이 가능한지 여부 |
| `creation_time` | `timestamptz` | 커서가 선언된 시간 |

#### 예제

```sql
-- 현재 열린 커서 확인
SELECT name, statement, is_holdable, is_scrollable, creation_time
FROM pg_cursors;

-- holdable 커서만 확인
SELECT name, statement
FROM pg_cursors
WHERE is_holdable = true;
```

---

### 54.3.10 pg_prepared_statements

`pg_prepared_statements` 뷰는 현재 세션에서 사용 가능한 모든 준비된 문장을 표시합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `name` | `text` | 준비된 문장의 식별자 |
| `statement` | `text` | 클라이언트가 제출한 쿼리 문자열 |
| `prepare_time` | `timestamptz` | 준비된 문장이 생성된 시간 |
| `parameter_types` | `regtype[]` | 예상 매개변수 유형 배열 |
| `result_types` | `regtype[]` | 반환되는 컬럼 유형 배열 |
| `from_sql` | `bool` | SQL PREPARE 명령으로 생성되었는지 여부 |
| `generic_plans` | `int8` | 제네릭 계획이 선택된 횟수 |
| `custom_plans` | `int8` | 커스텀 계획이 선택된 횟수 |

#### 예제

```sql
-- 현재 세션의 준비된 문장 확인
SELECT name, statement, prepare_time
FROM pg_prepared_statements;

-- 매개변수 정보와 함께 확인
SELECT name, statement, parameter_types, result_types
FROM pg_prepared_statements;

-- 계획 통계 확인
SELECT name, generic_plans, custom_plans
FROM pg_prepared_statements;
```

---

### 54.3.11 pg_replication_slots

`pg_replication_slots` 뷰는 데이터베이스 클러스터에 현재 존재하는 모든 복제 슬롯과 상태 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `slot_name` | `name` | 복제 슬롯의 고유 식별자 |
| `plugin` | `name` | 논리 슬롯이 사용하는 출력 플러그인 이름 |
| `slot_type` | `text` | 슬롯 유형: `physical` 또는 `logical` |
| `datoid` | `oid` | 연결된 데이터베이스 OID |
| `database` | `name` | 연결된 데이터베이스 이름 |
| `temporary` | `bool` | 임시 복제 슬롯 여부 |
| `active` | `bool` | 현재 스트리밍 중인지 여부 |
| `active_pid` | `int4` | 스트리밍 세션의 프로세스 ID |
| `xmin` | `xid` | 유지해야 하는 가장 오래된 트랜잭션 |
| `catalog_xmin` | `xid` | 시스템 카탈로그에 영향을 미치는 가장 오래된 트랜잭션 |
| `restart_lsn` | `pg_lsn` | 필요할 수 있는 가장 오래된 WAL의 주소 |
| `confirmed_flush_lsn` | `pg_lsn` | 수신 확인된 데이터까지의 주소 |
| `wal_status` | `text` | WAL 파일 가용성 상태 |
| `safe_wal_size` | `int8` | 안전하게 쓸 수 있는 WAL 바이트 수 |
| `two_phase` | `bool` | 준비된 트랜잭션 디코딩 활성화 여부 |
| `inactive_since` | `timestamptz` | 슬롯이 비활성화된 시간 |
| `conflicting` | `bool` | 복구와 충돌하여 무효화되었는지 여부 |
| `invalidation_reason` | `text` | 슬롯 무효화 이유 |
| `failover` | `bool` | 장애 조치를 위해 스탠바이로 동기화 활성화 여부 |
| `synced` | `bool` | 기본 서버에서 동기화되었는지 여부 |

#### 예제

```sql
-- 모든 복제 슬롯 확인
SELECT slot_name, slot_type, active, restart_lsn
FROM pg_replication_slots;

-- 활성 복제 슬롯 확인
SELECT slot_name, active_pid, database
FROM pg_replication_slots
WHERE active = true;

-- 비활성 슬롯과 비활성 시간 확인
SELECT slot_name, inactive_since, wal_status
FROM pg_replication_slots
WHERE active = false;
```

---

### 54.3.12 pg_sequences

`pg_sequences` 뷰는 데이터베이스의 각 시퀀스에 대한 유용한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 시퀀스를 포함하는 스키마 이름 |
| `sequencename` | `name` | 시퀀스 이름 |
| `sequenceowner` | `name` | 시퀀스 소유자 이름 |
| `data_type` | `regtype` | 시퀀스의 데이터 유형 |
| `start_value` | `int8` | 시퀀스 시작 값 |
| `min_value` | `int8` | 시퀀스 최소값 |
| `max_value` | `int8` | 시퀀스 최대값 |
| `increment_by` | `int8` | 시퀀스 증가값 |
| `cycle` | `bool` | 시퀀스 순환 여부 |
| `cache_size` | `int8` | 시퀀스 캐시 크기 |
| `last_value` | `int8` | 디스크에 기록된 마지막 시퀀스 값 |

#### 예제

```sql
-- 모든 시퀀스 정보 확인
SELECT schemaname, sequencename, start_value, increment_by, last_value
FROM pg_sequences;

-- 순환하는 시퀀스 확인
SELECT sequencename, min_value, max_value
FROM pg_sequences
WHERE cycle = true;

-- 캐시 크기가 1보다 큰 시퀀스 확인
SELECT sequencename, cache_size
FROM pg_sequences
WHERE cache_size > 1;
```

---

### 54.3.13 pg_matviews

`pg_matviews` 뷰는 데이터베이스의 각 구체화된 뷰(Materialized View)에 대한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 구체화된 뷰를 포함하는 스키마 이름 |
| `matviewname` | `name` | 구체화된 뷰 이름 |
| `matviewowner` | `name` | 구체화된 뷰 소유자 이름 |
| `tablespace` | `name` | 구체화된 뷰가 포함된 테이블스페이스 이름 |
| `hasindexes` | `bool` | 인덱스가 있는지 여부 |
| `ispopulated` | `bool` | 현재 데이터가 채워져 있는지 여부 |
| `definition` | `text` | 구체화된 뷰 정의 (재구성된 SELECT 쿼리) |

#### 예제

```sql
-- 모든 구체화된 뷰 확인
SELECT schemaname, matviewname, ispopulated
FROM pg_matviews;

-- 데이터가 채워지지 않은 구체화된 뷰 확인
SELECT matviewname, definition
FROM pg_matviews
WHERE ispopulated = false;

-- 인덱스가 없는 구체화된 뷰 확인
SELECT matviewname
FROM pg_matviews
WHERE hasindexes = false;
```

---

### 54.3.14 pg_policies

`pg_policies` 뷰는 데이터베이스의 각 행 수준 보안(Row Level Security, RLS) 정책에 대한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 정책이 적용된 테이블의 스키마 이름 |
| `tablename` | `name` | 정책이 적용된 테이블 이름 |
| `policyname` | `name` | 정책 이름 |
| `permissive` | `text` | 정책이 허용적(permissive)인지 제한적(restrictive)인지 |
| `roles` | `name[]` | 정책이 적용되는 역할 |
| `cmd` | `text` | 정책이 적용되는 명령 유형 |
| `qual` | `text` | 쿼리에 추가되는 보안 장벽 조건 표현식 |
| `with_check` | `text` | 행 추가 시 WITH CHECK 조건 표현식 |

#### 예제

```sql
-- 모든 RLS 정책 확인
SELECT schemaname, tablename, policyname, permissive, roles, cmd
FROM pg_policies;

-- 특정 테이블의 정책 확인
SELECT policyname, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'documents';

-- 제한적 정책만 확인
SELECT tablename, policyname, qual
FROM pg_policies
WHERE permissive = 'RESTRICTIVE';
```

---

### 54.3.15 pg_rules

`pg_rules` 뷰는 쿼리 재작성 규칙에 대한 정보를 제공합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `schemaname` | `name` | 테이블을 포함하는 스키마 이름 |
| `tablename` | `name` | 규칙이 적용된 테이블 이름 |
| `rulename` | `name` | 규칙 이름 |
| `definition` | `text` | 규칙 정의 (재구성된 생성 명령) |

> 참고: `pg_rules` 뷰는 뷰와 구체화된 뷰의 `ON SELECT` 규칙을 제외합니다. 뷰의 `ON SELECT` 규칙은 `pg_views`에서, 구체화된 뷰의 것은 `pg_matviews`에서 확인할 수 있습니다.

#### 예제

```sql
-- 모든 규칙 확인
SELECT schemaname, tablename, rulename, definition
FROM pg_rules;

-- 특정 테이블의 규칙 확인
SELECT rulename, definition
FROM pg_rules
WHERE tablename = 'orders';
```

---

### 54.3.16 pg_available_extensions

`pg_available_extensions` 뷰는 PostgreSQL에서 설치할 수 있는 확장 모듈을 나열합니다.

#### 컬럼 정의

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `name` | `name` | 확장 모듈 이름 |
| `default_version` | `text` | 기본 버전 이름 |
| `installed_version` | `text` | 현재 설치된 버전 (설치되지 않은 경우 NULL) |
| `comment` | `text` | 확장 모듈의 제어 파일에서 가져온 설명 |

#### 예제

```sql
-- 사용 가능한 모든 확장 모듈 확인
SELECT name, default_version, installed_version, comment
FROM pg_available_extensions
ORDER BY name;

-- 설치된 확장 모듈만 확인
SELECT name, installed_version
FROM pg_available_extensions
WHERE installed_version IS NOT NULL;

-- 설치되지 않은 확장 모듈 확인
SELECT name, default_version, comment
FROM pg_available_extensions
WHERE installed_version IS NULL;
```

---

## 54.4 통계 뷰

PostgreSQL은 데이터베이스 활동을 모니터링하기 위한 다양한 통계 뷰를 제공합니다. 이러한 뷰들은 누적 통계 시스템(Cumulative Statistics System)의 일부입니다.

### 주요 통계 뷰 목록

| 뷰 이름 | 설명 |
|---------|------|
| `pg_stat_activity` | 서버 프로세스당 하나의 행, 현재 활동 정보 |
| `pg_stat_replication` | WAL 전송자 프로세스당 하나의 행 |
| `pg_stat_replication_slots` | 복제 슬롯당 하나의 행 |
| `pg_stat_wal_receiver` | WAL 수신자당 하나의 행 |
| `pg_stat_subscription` | 구독당 하나의 행 |
| `pg_stat_ssl` | SSL 연결 정보 |
| `pg_stat_gssapi` | GSSAPI 인증 정보 |
| `pg_stat_archiver` | 아카이버 프로세스 통계 |
| `pg_stat_io` | I/O 통계 |
| `pg_stat_bgwriter` | 백그라운드 작성자 통계 |
| `pg_stat_checkpointer` | 체크포인터 통계 |
| `pg_stat_wal` | WAL 활동 통계 |
| `pg_stat_database` | 데이터베이스별 통계 |
| `pg_stat_database_conflicts` | 데이터베이스 충돌 통계 |
| `pg_stat_all_tables` | 모든 테이블 접근 통계 |
| `pg_stat_sys_tables` | 시스템 테이블 접근 통계 |
| `pg_stat_user_tables` | 사용자 테이블 접근 통계 |
| `pg_stat_all_indexes` | 모든 인덱스 접근 통계 |
| `pg_stat_sys_indexes` | 시스템 인덱스 접근 통계 |
| `pg_stat_user_indexes` | 사용자 인덱스 접근 통계 |
| `pg_statio_all_tables` | 모든 테이블 I/O 통계 |
| `pg_statio_all_indexes` | 모든 인덱스 I/O 통계 |
| `pg_statio_all_sequences` | 모든 시퀀스 I/O 통계 |
| `pg_stat_user_functions` | 사용자 함수 통계 |
| `pg_stat_slru` | SLRU 통계 |

### 통계 수집 구성

통계 수집을 활성화하려면 `postgresql.conf`에서 다음 매개변수를 설정합니다:

```sql
-- 현재 명령 모니터링
track_activities = on

-- 테이블/인덱스 접근 통계 수집
track_counts = on

-- 사용자 정의 함수 추적
track_functions = 'all'  -- 또는 'pl' 또는 'none'

-- 블록 I/O 시간 모니터링
track_io_timing = on

-- WAL I/O 시간 모니터링
track_wal_io_timing = on
```

### 통계 재설정 함수

```sql
-- 현재 데이터베이스의 모든 통계 재설정
SELECT pg_stat_reset();

-- 특정 공유 통계 재설정
SELECT pg_stat_reset_shared('archiver');  -- 또는 'bgwriter', 'wal', 'io' 등

-- 단일 테이블 통계 재설정
SELECT pg_stat_reset_single_table_counters(table_oid);

-- 현재 트랜잭션의 캐시된 값 지우기
SELECT pg_stat_clear_snapshot();
```

---

## 54.5 유용한 쿼리 예제

### 현재 데이터베이스 활동 모니터링

```sql
-- 현재 실행 중인 쿼리 확인
SELECT pid, usename, datname, state,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC;
```

### 잠금 대기 확인

```sql
-- 잠금을 대기 중인 쿼리 확인
SELECT blocked.pid AS blocked_pid,
       blocked.usename AS blocked_user,
       blocked.query AS blocked_query,
       blocking.pid AS blocking_pid,
       blocking.usename AS blocking_user,
       blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_stat_activity blocking
  ON blocking.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE cardinality(pg_blocking_pids(blocked.pid)) > 0;
```

### 테이블 사용 통계

```sql
-- 테이블별 순차 스캔 vs 인덱스 스캔
SELECT schemaname, relname,
       seq_scan, seq_tup_read,
       idx_scan, idx_tup_fetch,
       n_live_tup, n_dead_tup
FROM pg_stat_all_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY seq_scan DESC;
```

### 인덱스 사용 현황

```sql
-- 사용되지 않는 인덱스 찾기
SELECT schemaname, relname, indexrelname, idx_scan
FROM pg_stat_all_indexes
WHERE idx_scan = 0
  AND schemaname NOT IN ('pg_catalog', 'information_schema');
```

### 데이터베이스 연결 현황

```sql
-- 데이터베이스별 연결 수와 상태
SELECT datname,
       count(*) AS total,
       count(*) FILTER (WHERE state = 'active') AS active,
       count(*) FILTER (WHERE state = 'idle') AS idle,
       count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_txn
FROM pg_stat_activity
WHERE datname IS NOT NULL
GROUP BY datname;
```

### 캐시 적중률 확인

```sql
-- 테이블 캐시 적중률
SELECT schemaname, relname,
       heap_blks_read, heap_blks_hit,
       CASE WHEN heap_blks_read + heap_blks_hit > 0
            THEN round(100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit), 2)
            ELSE 0
       END AS cache_hit_ratio
FROM pg_statio_all_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY heap_blks_read DESC;
```

### 오래 실행 중인 트랜잭션

```sql
-- 5분 이상 실행 중인 트랜잭션
SELECT pid, usename, datname, state,
       now() - xact_start AS txn_duration,
       now() - query_start AS query_duration,
       query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
  AND now() - xact_start > interval '5 minutes'
ORDER BY txn_duration DESC;
```

### 설정 변경 이력

```sql
-- 기본값에서 변경된 설정
SELECT name, setting, boot_val, source, sourcefile, sourceline
FROM pg_settings
WHERE setting != boot_val
  AND source NOT IN ('default', 'override')
ORDER BY name;
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - System Views](https://www.postgresql.org/docs/current/views.html)
- [PostgreSQL 공식 문서 - Monitoring Database Activity](https://www.postgresql.org/docs/current/monitoring.html)
- [PostgreSQL 공식 문서 - Information Schema](https://www.postgresql.org/docs/current/information-schema.html)
