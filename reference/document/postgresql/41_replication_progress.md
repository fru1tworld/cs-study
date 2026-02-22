# Chapter 50: 복제 진행 추적 (Replication Progress Tracking)

## 목차

1. [개요](#개요)
2. [복제 원본의 개념](#복제-원본의-개념)
3. [복제 원본 등록 및 관리](#복제-원본-등록-및-관리)
4. [진행 상태 관리](#진행-상태-관리)
5. [세션 및 트랜잭션 설정](#세션-및-트랜잭션-설정)
6. [시스템 카탈로그와 뷰](#시스템-카탈로그와-뷰)
7. [복제 원본 활용 예제](#복제-원본-활용-예제)
8. [고급 사용 사례](#고급-사용-사례)

---

## 개요

복제 원본(Replication Origins)은 논리적 디코딩(Logical Decoding)을 기반으로 구축된 논리적 복제 솔루션을 용이하게 하기 위해 설계된 인프라 구성 요소입니다. 복제 원본은 다음 두 가지 주요 과제를 해결합니다.

### 주요 해결 과제

1. 복제 진행 상태의 안전한 추적
   - 복제 솔루션을 구축할 때 가장 어려운 부분 중 하나는 재생(replay) 진행 상태를 안전하게 추적하는 것입니다.
   - 적용 프로세스(applying process)나 전체 클러스터가 중단될 경우, 데이터가 어디까지 성공적으로 복제되었는지 파악할 수 있어야 합니다.
   - 단순한 해결책(예: 복제된 각 트랜잭션마다 테이블의 행을 업데이트하는 방식)은 런타임 오버헤드와 데이터베이스 비대화(bloat) 문제를 야기합니다.

2. 복제된 행의 재복제 방지
   - 단일 시스템에서 다른 단일 시스템으로의 복제보다 복잡한 복제 토폴로지에서는 이미 복제된 행이 다시 복제되는 것을 방지하기 어렵습니다.
   - 이로 인해 복제 사이클과 비효율성이 발생할 수 있습니다.
   - 복제 원본은 이를 인식하고 방지하는 선택적 메커니즘을 제공합니다.

### 복제 원본 인프라의 장점

복제 원본 인프라를 사용하면:

- 충돌 안전한 진행 추적: 복제 진행 상태가 충돌에 안전한 방식으로 지속됩니다.
- 오버헤드 최소화: 단순한 솔루션(각 트랜잭션마다 행을 업데이트하는 방식)의 런타임 오버헤드와 데이터베이스 비대화 문제를 제거합니다.
- 복구 용이성: 적용 프로세스 또는 클러스터 장애 시 복구 지점을 쉽게 결정할 수 있습니다.

---

## 복제 원본의 개념

### 복제 원본의 속성

각 복제 원본에는 두 가지 속성이 있습니다.

| 속성 | 설명 |
|------|------|
| 이름(Name) | 시스템 간에 사용되는 자유 형식 텍스트 식별자입니다. 충돌을 피하기 위해 복제 솔루션 이름을 접두사로 사용해야 합니다. |
| ID | 공간 효율적인 저장을 위한 숫자 식별자입니다. 시스템 간에 공유되지 않습니다. |

### 복제 원본의 역할

1. 진행 상태 추적: 원격 노드에서 재생 중임을 표시하고 진행 상태를 추적합니다.
2. 원본 태깅: 변경 사항에 생성 세션의 복제 원본을 태그하여 원본에 따라 다르게 처리할 수 있습니다.
3. 필터링 지원: `filter_by_origin_cb` 콜백을 통해 논리적 디코딩 변경 스트림을 원본별로 효율적으로 필터링합니다.

---

## 복제 원본 등록 및 관리

### 복제 원본 생성

```sql
pg_replication_origin_create(node_name text) → oid
```

복제 원본을 생성하고 내부 ID를 반환합니다.

매개변수:
- `node_name`: 외부 이름 (최대 512바이트)

권한: 기본적으로 슈퍼유저만 사용 가능 (GRANT로 권한 부여 가능)

예제:

```sql
-- 복제 원본 생성
SELECT pg_replication_origin_create('pg_cluster_node1');
-- 결과: 1 (내부 OID 반환)

-- 다른 노드의 복제 원본 생성
SELECT pg_replication_origin_create('pg_cluster_node2');
-- 결과: 2
```

### 복제 원본 삭제

```sql
pg_replication_origin_drop(node_name text) → void
```

이전에 생성된 복제 원본과 관련된 모든 재생 진행 상태를 삭제합니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 복제 원본 삭제
SELECT pg_replication_origin_drop('pg_cluster_node1');
```

### 복제 원본 조회

```sql
pg_replication_origin_oid(node_name text) → oid
```

복제 원본 이름으로 내부 ID를 조회합니다. 원본이 없으면 `NULL`을 반환합니다.

예제:

```sql
-- 복제 원본 OID 조회
SELECT pg_replication_origin_oid('pg_cluster_node1');
-- 결과: 1 (또는 NULL)
```

---

## 진행 상태 관리

### 복제 진행 상태 조회

```sql
pg_replication_origin_progress(node_name text, flush boolean) → pg_lsn
```

지정된 복제 원본의 재생 위치를 반환합니다.

매개변수:
- `node_name`: 복제 원본 이름
- `flush`: 해당 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

예제:

```sql
-- 진행 상태 조회 (플러시 보장)
SELECT pg_replication_origin_progress('pg_cluster_node1', true);
-- 결과: 0/1A4B8C0

-- 진행 상태 조회 (플러시 미보장)
SELECT pg_replication_origin_progress('pg_cluster_node1', false);
-- 결과: 0/1A4B8C0
```

### 복제 진행 상태 수동 설정

```sql
pg_replication_origin_advance(node_name text, lsn pg_lsn) → void
```

지정된 노드의 복제 진행 상태를 지정된 위치로 설정합니다. 주로 초기 위치 설정이나 구성 변경 후 새 위치 설정에 사용됩니다.

주의: 부주의한 사용은 일관성 없이 복제된 데이터를 초래할 수 있습니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 초기 복제 위치 설정
SELECT pg_replication_origin_advance('pg_cluster_node1', '0/1A4B8C0');

-- 특정 LSN으로 진행 상태 이동
SELECT pg_replication_origin_advance('pg_cluster_node1', '0/2B5C9D1');
```

---

## 세션 및 트랜잭션 설정

### 세션에 복제 원본 설정

```sql
pg_replication_origin_session_setup(node_name text) → void
```

현재 세션을 지정된 원본에서 재생 중으로 표시하여 재생 진행 상태를 추적할 수 있게 합니다. 이미 원본이 선택된 경우에는 사용할 수 없습니다.

권한: 기본적으로 슈퍼유저만 사용 가능

예제:

```sql
-- 세션에 복제 원본 설정
SELECT pg_replication_origin_session_setup('pg_cluster_node1');
```

### 세션 복제 원본 해제

```sql
pg_replication_origin_session_reset() → void
```

`pg_replication_origin_session_setup()`의 효과를 취소합니다.

예제:

```sql
-- 세션 복제 원본 해제
SELECT pg_replication_origin_session_reset();
```

### 세션 설정 확인

```sql
pg_replication_origin_session_is_setup() → boolean
```

현재 세션에 복제 원본이 선택되어 있으면 `true`를 반환합니다.

예제:

```sql
-- 세션에 복제 원본이 설정되어 있는지 확인
SELECT pg_replication_origin_session_is_setup();
-- 결과: t (true) 또는 f (false)
```

### 세션 진행 상태 조회

```sql
pg_replication_origin_session_progress(flush boolean) → pg_lsn
```

현재 세션에서 선택된 복제 원본의 재생 위치를 반환합니다.

매개변수:
- `flush`: 해당 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

예제:

```sql
-- 세션 진행 상태 조회
SELECT pg_replication_origin_session_progress(true);
-- 결과: 0/1A4B8C0
```

### 트랜잭션 원본 정보 설정

```sql
pg_replication_origin_xact_setup(origin_lsn pg_lsn, origin_timestamp timestamp with time zone) → void
```

현재 트랜잭션을 지정된 LSN과 타임스탬프에서 커밋된 트랜잭션을 재생 중으로 표시합니다. `pg_replication_origin_session_setup()`으로 복제 원본이 선택된 경우에만 호출할 수 있습니다.

매개변수:
- `origin_lsn`: 원본에서 트랜잭션이 커밋된 LSN
- `origin_timestamp`: 트랜잭션이 커밋된 타임스탬프

예제:

```sql
-- 세션 설정 후 트랜잭션 원본 정보 설정
SELECT pg_replication_origin_session_setup('pg_cluster_node1');
SELECT pg_replication_origin_xact_setup('0/1A4B8C0', '2024-01-15 10:30:00+09');

-- 데이터 변경 작업 수행
INSERT INTO my_table (col1, col2) VALUES ('value1', 'value2');
COMMIT;
```

### 트랜잭션 원본 정보 해제

```sql
pg_replication_origin_xact_reset() → void
```

`pg_replication_origin_xact_setup()`의 효과를 취소합니다.

예제:

```sql
-- 트랜잭션 원본 정보 해제
SELECT pg_replication_origin_xact_reset();
```

---

## 시스템 카탈로그와 뷰

### pg_replication_origin 시스템 카탈로그

`pg_replication_origin` 카탈로그는 클러스터에 생성된 모든 복제 원본을 포함합니다. 이것은 공유 시스템 카탈로그 로, 대부분의 다른 시스템 카탈로그와 달리 데이터베이스당 하나가 아닌 클러스터당 하나만 존재합니다.

| 열(Column) | 타입(Type) | 설명 |
|------------|-----------|------|
| `roident` | `oid` | 복제 원본의 고유한 클러스터 전체 식별자입니다. 시스템 외부로 노출되지 않아야 합니다. |
| `roname` | `text` | 복제 원본의 외부 사용자 정의 이름입니다. |

조회 예제:

```sql
-- 모든 복제 원본 조회
SELECT roident, roname FROM pg_replication_origin;

-- 결과 예시:
--  roident |       roname
-- ---------+--------------------
--        1 | pg_cluster_node1
--        2 | pg_cluster_node2
```

### pg_replication_origin_status 뷰

`pg_replication_origin_status` 뷰는 모든 복제 원본의 재생 진행 상태를 표시합니다.

| 열(Column) | 타입(Type) | 설명 |
|------------|-----------|------|
| `local_id` | `oid` | 내부 노드 식별자 (`pg_replication_origin.roident` 참조) |
| `external_id` | `text` | 외부 노드 식별자 (`pg_replication_origin.roname` 참조) |
| `remote_lsn` | `pg_lsn` | 데이터가 복제된 원본 노드의 LSN |
| `local_lsn` | `pg_lsn` | `remote_lsn`이 복제된 이 노드의 LSN. 비동기 커밋 사용 시 데이터를 디스크에 저장하기 전에 커밋 레코드를 플러시하는 데 사용됩니다. |

조회 예제:

```sql
-- 모든 복제 원본의 진행 상태 조회
SELECT
    local_id,
    external_id,
    remote_lsn,
    local_lsn
FROM pg_replication_origin_status;

-- 결과 예시:
--  local_id |    external_id     | remote_lsn | local_lsn
-- ----------+--------------------+------------+------------
--         1 | pg_cluster_node1   | 0/1A4B8C0  | 0/2B5C9D1
--         2 | pg_cluster_node2   | 0/3C6D0E2  | 0/4D7E1F3
```

---

## 복제 원본 활용 예제

### 예제 1: 기본적인 복제 원본 설정

```sql
-- 1. 복제 원본 생성
SELECT pg_replication_origin_create('upstream_node');

-- 2. 생성된 원본 확인
SELECT * FROM pg_replication_origin;

-- 3. 세션에 복제 원본 설정
SELECT pg_replication_origin_session_setup('upstream_node');

-- 4. 세션 설정 확인
SELECT pg_replication_origin_session_is_setup();

-- 5. 트랜잭션 시작 및 원본 정보 설정
BEGIN;
SELECT pg_replication_origin_xact_setup('0/1000000', NOW());

-- 6. 복제할 데이터 적용
INSERT INTO replicated_table (id, data) VALUES (1, 'replicated data');

-- 7. 커밋
COMMIT;

-- 8. 진행 상태 확인
SELECT pg_replication_origin_session_progress(true);

-- 9. 세션 해제
SELECT pg_replication_origin_session_reset();
```

### 예제 2: 복제 진행 상태 모니터링

```sql
-- 모든 복제 원본의 상세 진행 상태 조회
SELECT
    ros.external_id AS origin_name,
    ros.remote_lsn AS replicated_up_to,
    ros.local_lsn AS local_position,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), ros.local_lsn)
    ) AS lag_size
FROM pg_replication_origin_status ros
JOIN pg_replication_origin ro ON ros.local_id = ro.roident;
```

### 예제 3: 복제 재개 시 시작점 결정

```sql
-- 복제를 재개할 때 시작점 결정
DO $$
DECLARE
    v_origin_name TEXT := 'upstream_node';
    v_resume_lsn pg_lsn;
BEGIN
    -- 마지막으로 복제된 LSN 조회
    SELECT pg_replication_origin_progress(v_origin_name, true)
    INTO v_resume_lsn;

    IF v_resume_lsn IS NULL THEN
        RAISE NOTICE 'No previous replication progress found. Starting from scratch.';
    ELSE
        RAISE NOTICE 'Resuming replication from LSN: %', v_resume_lsn;
    END IF;
END $$;
```

### 예제 4: 양방향 복제에서 루프 방지

```sql
-- 양방향 복제 설정에서 각 노드의 원본 등록

-- Node A에서 실행
SELECT pg_replication_origin_create('node_b');

-- Node B에서 실행
SELECT pg_replication_origin_create('node_a');

-- 데이터 적용 시 원본 표시 (Node A에서 Node B의 데이터 적용)
SELECT pg_replication_origin_session_setup('node_b');

BEGIN;
SELECT pg_replication_origin_xact_setup('0/1234567', NOW());
-- 이 변경 사항은 node_b에서 온 것으로 표시됨
INSERT INTO shared_table (id, value) VALUES (1, 'from node_b');
COMMIT;

SELECT pg_replication_origin_session_reset();
```

---

## 고급 사용 사례

### 복잡한 복제 토폴로지에서의 원본 필터링

복잡한 복제 시나리오에서 변경 사항은 생성 세션의 복제 원본으로 태그됩니다. 이를 통해:

1. 출력 플러그인 이 원본에 따라 변경 사항을 다르게 처리할 수 있습니다.
2. `filter_by_origin_cb` 콜백 을 통해 논리적 디코딩 변경 스트림을 원본별로 효율적으로 필터링할 수 있습니다.
3. 출력 플러그인 내에서 직접 필터링하는 것보다 더 효율적입니다.

### 충돌 안전한 진행 추적 구현

```sql
-- 복제 원본을 사용한 안전한 진행 추적 예제
CREATE OR REPLACE FUNCTION safe_apply_transaction(
    p_origin_name TEXT,
    p_origin_lsn pg_lsn,
    p_origin_timestamp TIMESTAMPTZ,
    p_sql_commands TEXT[]
) RETURNS void AS $$
DECLARE
    v_cmd TEXT;
    v_last_applied_lsn pg_lsn;
BEGIN
    -- 이미 적용된 트랜잭션인지 확인
    SELECT pg_replication_origin_progress(p_origin_name, true)
    INTO v_last_applied_lsn;

    IF v_last_applied_lsn >= p_origin_lsn THEN
        RAISE NOTICE 'Transaction at LSN % already applied. Skipping.', p_origin_lsn;
        RETURN;
    END IF;

    -- 세션 설정
    PERFORM pg_replication_origin_session_setup(p_origin_name);

    -- 트랜잭션 원본 정보 설정
    PERFORM pg_replication_origin_xact_setup(p_origin_lsn, p_origin_timestamp);

    -- SQL 명령 실행
    FOREACH v_cmd IN ARRAY p_sql_commands
    LOOP
        EXECUTE v_cmd;
    END LOOP;

    -- 세션 해제
    PERFORM pg_replication_origin_session_reset();

EXCEPTION WHEN OTHERS THEN
    -- 오류 발생 시 세션 해제
    PERFORM pg_replication_origin_session_reset();
    RAISE;
END;
$$ LANGUAGE plpgsql;
```

### 복제 원본 관리 유틸리티

```sql
-- 복제 원본 상태 요약 뷰 생성
CREATE OR REPLACE VIEW replication_origin_summary AS
SELECT
    ro.roident AS origin_id,
    ro.roname AS origin_name,
    ros.remote_lsn,
    ros.local_lsn,
    CASE
        WHEN ros.remote_lsn IS NULL THEN 'Not Started'
        ELSE 'Active'
    END AS status
FROM pg_replication_origin ro
LEFT JOIN pg_replication_origin_status ros ON ro.roident = ros.local_id;

-- 사용
SELECT * FROM replication_origin_summary;
```

---

## 함수 요약 표

| 함수 | 설명 | 권한 |
|------|------|------|
| `pg_replication_origin_create(text)` | 복제 원본 생성 | 슈퍼유저 |
| `pg_replication_origin_drop(text)` | 복제 원본 삭제 | 슈퍼유저 |
| `pg_replication_origin_oid(text)` | 복제 원본 OID 조회 | 모든 사용자 |
| `pg_replication_origin_session_setup(text)` | 세션에 복제 원본 설정 | 슈퍼유저 |
| `pg_replication_origin_session_reset()` | 세션 복제 원본 해제 | 모든 사용자 |
| `pg_replication_origin_session_is_setup()` | 세션 설정 여부 확인 | 모든 사용자 |
| `pg_replication_origin_session_progress(boolean)` | 세션 진행 상태 조회 | 모든 사용자 |
| `pg_replication_origin_xact_setup(pg_lsn, timestamptz)` | 트랜잭션 원본 정보 설정 | 모든 사용자 |
| `pg_replication_origin_xact_reset()` | 트랜잭션 원본 정보 해제 | 모든 사용자 |
| `pg_replication_origin_advance(text, pg_lsn)` | 진행 상태 수동 설정 | 슈퍼유저 |
| `pg_replication_origin_progress(text, boolean)` | 특정 원본 진행 상태 조회 | 모든 사용자 |

---

## 관련 문서

- [Chapter 47: 논리적 디코딩 (Logical Decoding)](40_logical_decoding.md)
- [Chapter 27: 고가용성, 로드 밸런싱, 복제 (High Availability)](19_high_availability.md)
- [Chapter 31: 논리적 복제 (Logical Replication)](24_logical_replication.md)

---

## 참고 자료

- [PostgreSQL 공식 문서: Replication Progress Tracking](https://www.postgresql.org/docs/current/replication-origins.html)
- [PostgreSQL 공식 문서: pg_replication_origin 카탈로그](https://www.postgresql.org/docs/current/catalog-pg-replication-origin.html)
- [PostgreSQL 공식 문서: pg_replication_origin_status 뷰](https://www.postgresql.org/docs/current/view-pg-replication-origin-status.html)
- [PostgreSQL 공식 문서: 시스템 관리 함수](https://www.postgresql.org/docs/current/functions-admin.html)
