# PostgreSQL 이벤트 트리거 (Event Triggers)

## 목차
1. [개요](#1-개요)
2. [이벤트 트리거와 일반 트리거의 차이점](#2-이벤트-트리거와-일반-트리거의-차이점)
3. [이벤트 종류](#3-이벤트-종류)
4. [이벤트 트리거 생성](#4-이벤트-트리거-생성)
5. [이벤트 트리거 지원 함수](#5-이벤트-트리거-지원-함수)
6. [이벤트 트리거 관리](#6-이벤트-트리거-관리)
7. [실용적인 예제](#7-실용적인-예제)
8. [C 언어로 이벤트 트리거 함수 작성](#8-c-언어로-이벤트-트리거-함수-작성)
9. [주의사항 및 제한사항](#9-주의사항-및-제한사항)

---

## 1. 개요

이벤트 트리거(Event Trigger)는 PostgreSQL에서 일반 트리거 메커니즘을 보완하는 기능입니다. 이벤트 트리거는 DDL(Data Definition Language) 이벤트 를 캡처하여 데이터베이스 수준에서 발생하는 스키마 변경 작업을 모니터링하고 제어할 수 있게 해줍니다.

### 주요 특징

- 데이터베이스 전역 범위: 특정 테이블에 연결되지 않고 데이터베이스 전체에서 작동
- DDL 명령 캡처: `CREATE`, `ALTER`, `DROP` 등의 DDL 명령에 반응
- 프로시저 언어 지원: PL/pgSQL, PL/Python, C 등 이벤트 트리거를 지원하는 모든 프로시저 언어로 작성 가능 (순수 SQL은 불가)

---

## 2. 이벤트 트리거와 일반 트리거의 차이점

| 특성 | 일반 트리거 (Regular Trigger) | 이벤트 트리거 (Event Trigger) |
|------|------------------------------|-------------------------------|
| 범위 | 특정 테이블에 연결 | 데이터베이스 전역 |
| 이벤트 유형 | DML (INSERT, UPDATE, DELETE) | DDL (CREATE, ALTER, DROP 등) |
| 작성 언어 | 모든 프로시저 언어 + SQL | 프로시저 언어 또는 C (SQL 제외) |
| 반환 타입 | `trigger` | `event_trigger` |
| 데이터 접근 | 테이블의 행 데이터 | DDL 명령 메타데이터 |

---

## 3. 이벤트 종류

PostgreSQL은 현재 5가지 이벤트 트리거 유형을 지원합니다.

### 3.1 ddl_command_start

DDL 명령이 실행되기 직전 에 발생합니다.

#### 트리거가 발생하는 명령
- `CREATE`, `ALTER`, `DROP`
- `COMMENT`, `GRANT`, `REVOKE`
- `IMPORT FOREIGN SCHEMA`, `REINDEX`
- `REFRESH MATERIALIZED VIEW`, `SECURITY LABEL`
- `SELECT INTO` (`CREATE TABLE AS`와 동등)

#### 트리거가 발생하지 않는 경우
- 공유 객체에 대한 명령 (데이터베이스, 역할, 테이블스페이스, 매개변수 권한)
- `ALTER SYSTEM` 명령
- 이벤트 트리거 자체에 대한 DDL 명령

```sql
-- 예제: DDL 명령 시작 시 로깅
CREATE OR REPLACE FUNCTION log_ddl_start()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE NOTICE 'DDL 명령 시작: %', tg_tag;
END;
$$;

CREATE EVENT TRIGGER ddl_start_logger
    ON ddl_command_start
    EXECUTE FUNCTION log_ddl_start();
```

### 3.2 ddl_command_end

DDL 명령이 완료된 직후 에 발생합니다. 동작은 완료되었지만 트랜잭션이 아직 커밋되지 않은 상태입니다.

```sql
-- 예제: DDL 명령 완료 시 상세 정보 기록
CREATE OR REPLACE FUNCTION log_ddl_end()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        RAISE NOTICE 'DDL 완료: % - 객체: %.%',
                     obj.command_tag,
                     obj.schema_name,
                     obj.object_identity;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER ddl_end_logger
    ON ddl_command_end
    EXECUTE FUNCTION log_ddl_end();
```

### 3.3 sql_drop

객체가 삭제(DROP)되기 직전 에 발생합니다. `ddl_command_end` 이벤트 전에 실행됩니다.

이 이벤트는 `DROP` 명령과 일부 `ALTER` 명령(예: `ALTER TABLE DROP COLUMN`)에서 발생합니다.

```sql
-- 예제: 삭제된 객체 정보 기록
CREATE OR REPLACE FUNCTION log_dropped_objects()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        RAISE NOTICE '삭제됨: % - %.% (%)',
                     obj.object_type,
                     obj.schema_name,
                     obj.object_name,
                     obj.object_identity;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER drop_logger
    ON sql_drop
    EXECUTE FUNCTION log_dropped_objects();
```

### 3.4 table_rewrite

테이블이 재작성(rewrite)되기 직전 에 발생합니다. `ALTER TABLE` 또는 `ALTER TYPE` 명령으로 인해 테이블이 물리적으로 재작성될 때 트리거됩니다.

참고: `CLUSTER`나 `VACUUM`에 의한 재작성에는 트리거되지 않습니다.

#### 테이블 재작성이 발생하는 경우
- 열의 데이터 타입 변경
- 열의 기본값 변경 (기존 행에 영향을 미치는 경우)
- 테이블의 지속성(persistence) 변경
- 테이블의 접근 방법(access method) 변경

```sql
-- 예제: 테이블 재작성 모니터링
CREATE OR REPLACE FUNCTION monitor_table_rewrite()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE NOTICE '테이블 재작성: %, 이유 코드: %',
                 pg_event_trigger_table_rewrite_oid()::regclass,
                 pg_event_trigger_table_rewrite_reason();
END;
$$;

CREATE EVENT TRIGGER table_rewrite_monitor
    ON table_rewrite
    EXECUTE FUNCTION monitor_table_rewrite();
```

### 3.5 login

인증된 사용자가 데이터베이스에 로그인할 때 발생합니다.

#### 주의사항
- 스탠바이 서버에서도 실행됩니다
- 버그가 있으면 시스템 로그인이 불가능해질 수 있습니다
- 스탠바이 서버에서는 데이터베이스 쓰기를 피해야 합니다
- 장기 실행 쿼리를 피해야 합니다

#### 문제 발생 시 해결 방법
- 연결 문자열이나 설정 파일에서 `event_triggers = false` 설정
- 단일 사용자 모드로 재시작

```sql
-- 예제: 로그인 감사 로깅
CREATE OR REPLACE FUNCTION audit_login()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO login_audit (username, login_time, client_addr)
    VALUES (current_user, current_timestamp, inet_client_addr());
EXCEPTION
    WHEN OTHERS THEN
        -- 스탠바이에서 쓰기 오류 무시
        NULL;
END;
$$;

CREATE EVENT TRIGGER login_auditor
    ON login
    EXECUTE FUNCTION audit_login();
```

---

## 4. 이벤트 트리거 생성

### 4.1 기본 구문

```sql
CREATE EVENT TRIGGER trigger_name
    ON event_type
    [ WHEN filter_variable IN (filter_value [, ... ]) [ AND ... ] ]
    EXECUTE FUNCTION function_name();
```

### 4.2 이벤트 트리거 함수 요구사항

1. 반환 타입이 `event_trigger`인 함수를 먼저 생성해야 합니다
2. 함수는 값을 반환할 필요가 없습니다 (반환 타입은 신호 역할만)
3. 동일 이벤트에 대한 여러 트리거는 트리거 이름의 알파벳 순서 로 실행됩니다

### 4.3 WHEN 조건 사용

특정 조건에서만 트리거를 실행하도록 필터링할 수 있습니다.

```sql
-- 예제: 특정 DDL 명령에만 반응하는 트리거
CREATE EVENT TRIGGER restrict_drop
    ON ddl_command_start
    WHEN TAG IN ('DROP TABLE', 'DROP SCHEMA', 'DROP INDEX')
    EXECUTE FUNCTION prevent_dangerous_drops();
```

### 4.4 완전한 예제: DDL 제한 트리거

```sql
-- 1. 이벤트 트리거 함수 생성
CREATE OR REPLACE FUNCTION prevent_ddl_on_production()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    current_db text := current_database();
BEGIN
    -- 프로덕션 데이터베이스에서 DDL 차단
    IF current_db LIKE '%_prod' OR current_db LIKE '%_production' THEN
        IF NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
            RAISE EXCEPTION '프로덕션 환경에서 DDL 명령이 차단되었습니다: %', tg_tag;
        END IF;
    END IF;
END;
$$;

-- 2. 이벤트 트리거 생성
CREATE EVENT TRIGGER protect_production
    ON ddl_command_start
    EXECUTE FUNCTION prevent_ddl_on_production();
```

---

## 5. 이벤트 트리거 지원 함수

PostgreSQL은 이벤트 트리거 내에서 정보를 얻기 위한 여러 지원 함수를 제공합니다.

### 5.1 pg_event_trigger_ddl_commands()

`ddl_command_end` 이벤트에서 실행된 DDL 명령 목록을 반환합니다.

#### 반환 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `classid` | `oid` | 객체가 속한 카탈로그의 OID |
| `objid` | `oid` | 객체 자체의 OID |
| `objsubid` | `integer` | 하위 객체 ID (예: 열의 속성 번호) |
| `command_tag` | `text` | 명령 태그 |
| `object_type` | `text` | 객체 타입 |
| `schema_name` | `text` | 객체가 속한 스키마 이름 (해당되는 경우) |
| `object_identity` | `text` | 스키마 한정 객체 식별자 |
| `in_extension` | `boolean` | 확장 스크립트의 일부인지 여부 |
| `command` | `pg_ddl_command` | 내부 형식의 완전한 명령 표현 |

```sql
-- 예제: DDL 명령 상세 정보 조회
CREATE OR REPLACE FUNCTION track_ddl_commands()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    cmd record;
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        INSERT INTO ddl_audit_log (
            command_tag,
            object_type,
            schema_name,
            object_identity,
            executed_by,
            executed_at
        ) VALUES (
            cmd.command_tag,
            cmd.object_type,
            cmd.schema_name,
            cmd.object_identity,
            current_user,
            current_timestamp
        );
    END LOOP;
END;
$$;
```

### 5.2 pg_event_trigger_dropped_objects()

`sql_drop` 이벤트에서 삭제된 모든 객체 목록을 반환합니다.

#### 반환 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `classid` | `oid` | 객체가 속했던 카탈로그의 OID |
| `objid` | `oid` | 객체 자체의 OID |
| `objsubid` | `integer` | 하위 객체 ID |
| `original` | `boolean` | 삭제의 루트 객체인지 여부 |
| `normal` | `boolean` | 정상 의존성 관계가 있었는지 여부 |
| `is_temporary` | `boolean` | 임시 객체인지 여부 |
| `object_type` | `text` | 객체 타입 |
| `schema_name` | `text` | 객체가 속했던 스키마 이름 |
| `object_name` | `text` | 객체 이름 |
| `object_identity` | `text` | 스키마 한정 객체 식별자 |
| `address_names` | `text[]` | `pg_get_object_address`와 함께 사용할 배열 |
| `address_args` | `text[]` | `address_names`의 보완 배열 |

```sql
-- 예제: 삭제된 객체 감사
CREATE OR REPLACE FUNCTION audit_dropped_objects()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        -- 루트 객체만 기록 (의존성으로 삭제된 객체 제외)
        IF obj.original THEN
            INSERT INTO drop_audit_log (
                object_type,
                schema_name,
                object_name,
                object_identity,
                dropped_by,
                dropped_at
            ) VALUES (
                obj.object_type,
                obj.schema_name,
                obj.object_name,
                obj.object_identity,
                current_user,
                current_timestamp
            );
        END IF;
    END LOOP;
END;
$$;
```

### 5.3 pg_event_trigger_table_rewrite_oid()

`table_rewrite` 이벤트에서 재작성될 테이블의 OID를 반환합니다.

```sql
SELECT pg_event_trigger_table_rewrite_oid()::regclass;
-- 결과: public.my_table
```

### 5.4 pg_event_trigger_table_rewrite_reason()

테이블 재작성 이유를 비트맵 코드로 반환합니다.

| 비트 값 | 의미 |
|---------|------|
| 1 | 테이블의 지속성(persistence) 변경 |
| 2 | 열의 기본값 변경 |
| 4 | 열의 데이터 타입 변경 |
| 8 | 테이블 접근 방법(access method) 변경 |

```sql
-- 예제: 재작성 이유 분석
CREATE OR REPLACE FUNCTION analyze_rewrite_reason()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    reason integer := pg_event_trigger_table_rewrite_reason();
    table_name text := pg_event_trigger_table_rewrite_oid()::regclass::text;
    reasons text := '';
BEGIN
    IF reason & 1 > 0 THEN reasons := reasons || '지속성 변경, '; END IF;
    IF reason & 2 > 0 THEN reasons := reasons || '기본값 변경, '; END IF;
    IF reason & 4 > 0 THEN reasons := reasons || '데이터 타입 변경, '; END IF;
    IF reason & 8 > 0 THEN reasons := reasons || '접근 방법 변경, '; END IF;

    reasons := rtrim(reasons, ', ');

    RAISE NOTICE '테이블 %이(가) 다음 이유로 재작성됩니다: %', table_name, reasons;
END;
$$;
```

---

## 6. 이벤트 트리거 관리

### 6.1 이벤트 트리거 조회

```sql
-- 모든 이벤트 트리거 조회
SELECT * FROM pg_event_trigger;

-- psql에서 이벤트 트리거 목록 보기
\dy

-- 상세 정보와 함께 조회
SELECT
    evtname AS "트리거 이름",
    evtevent AS "이벤트",
    evtenabled AS "상태",
    pg_get_userbyid(evtowner) AS "소유자",
    evtfoid::regproc AS "함수"
FROM pg_event_trigger
ORDER BY evtname;
```

### 6.2 이벤트 트리거 활성화/비활성화

```sql
-- 트리거 비활성화
ALTER EVENT TRIGGER trigger_name DISABLE;

-- 트리거 활성화
ALTER EVENT TRIGGER trigger_name ENABLE;

-- 레플리카에서만 활성화
ALTER EVENT TRIGGER trigger_name ENABLE REPLICA;

-- 항상 활성화 (수퍼유저만)
ALTER EVENT TRIGGER trigger_name ENABLE ALWAYS;
```

### 6.3 이벤트 트리거 이름 변경

```sql
ALTER EVENT TRIGGER old_name RENAME TO new_name;
```

### 6.4 이벤트 트리거 소유자 변경

```sql
ALTER EVENT TRIGGER trigger_name OWNER TO new_owner;
```

### 6.5 이벤트 트리거 삭제

```sql
-- 트리거 삭제
DROP EVENT TRIGGER trigger_name;

-- 존재하는 경우에만 삭제
DROP EVENT TRIGGER IF EXISTS trigger_name;

-- 의존 객체와 함께 삭제
DROP EVENT TRIGGER trigger_name CASCADE;
```

---

## 7. 실용적인 예제

### 7.1 DDL 감사 로깅 시스템

```sql
-- 감사 로그 테이블 생성
CREATE TABLE ddl_audit_log (
    id SERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,
    command_tag TEXT NOT NULL,
    object_type TEXT,
    schema_name TEXT,
    object_identity TEXT,
    executed_by TEXT NOT NULL DEFAULT current_user,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    client_addr INET,
    application_name TEXT
);

-- DDL 감사 함수
CREATE OR REPLACE FUNCTION audit_ddl_changes()
RETURNS event_trigger
LANGUAGE plpgsql
SECURITY DEFINER AS $$
DECLARE
    cmd record;
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        INSERT INTO ddl_audit_log (
            event_type,
            command_tag,
            object_type,
            schema_name,
            object_identity,
            client_addr,
            application_name
        ) VALUES (
            TG_EVENT,
            cmd.command_tag,
            cmd.object_type,
            cmd.schema_name,
            cmd.object_identity,
            inet_client_addr(),
            current_setting('application_name', true)
        );
    END LOOP;
END;
$$;

-- 감사 트리거 생성
CREATE EVENT TRIGGER audit_ddl
    ON ddl_command_end
    EXECUTE FUNCTION audit_ddl_changes();
```

### 7.2 테이블 재작성 정책 구현

```sql
-- 테이블 재작성 정책 함수
CREATE OR REPLACE FUNCTION enforce_rewrite_policy()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    table_oid oid := pg_event_trigger_table_rewrite_oid();
    table_name text := table_oid::regclass::text;
    current_hour integer := extract('hour' from current_time);
    table_pages integer;
    max_pages integer := 100;
BEGIN
    -- 특정 테이블은 재작성 금지
    IF table_name = 'public.critical_data' THEN
        RAISE EXCEPTION '테이블 %은(는) 재작성이 허용되지 않습니다', table_name;
    END IF;

    -- 테이블 크기 확인
    SELECT relpages INTO table_pages
    FROM pg_class
    WHERE oid = table_oid;

    -- 대용량 테이블은 유지보수 시간에만 재작성 허용
    IF table_pages > max_pages THEN
        IF current_hour NOT BETWEEN 2 AND 5 THEN
            RAISE EXCEPTION '% 페이지 이상의 테이블은 오전 2시~5시 사이에만 재작성 가능합니다 (현재 테이블: % 페이지)',
                            max_pages, table_pages;
        END IF;
    END IF;

    RAISE NOTICE '테이블 % 재작성 허용됨 (% 페이지)', table_name, table_pages;
END;
$$;

CREATE EVENT TRIGGER rewrite_policy
    ON table_rewrite
    EXECUTE FUNCTION enforce_rewrite_policy();
```

### 7.3 특정 스키마 보호

```sql
-- 보호된 스키마에서 DDL 차단
CREATE OR REPLACE FUNCTION protect_schema()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    cmd record;
    protected_schemas text[] := ARRAY['core', 'audit', 'security'];
BEGIN
    FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    LOOP
        IF cmd.schema_name = ANY(protected_schemas) THEN
            -- 수퍼유저는 허용
            IF NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
                RAISE EXCEPTION '스키마 %는 보호되어 있습니다. DDL 명령 %이(가) 차단되었습니다.',
                                cmd.schema_name, cmd.command_tag;
            END IF;
        END IF;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER schema_protection
    ON ddl_command_end
    EXECUTE FUNCTION protect_schema();
```

### 7.4 DROP 작업 방지

```sql
-- DROP 명령 차단 및 경고
CREATE OR REPLACE FUNCTION prevent_drops()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
DECLARE
    obj record;
    critical_objects text[] := ARRAY['users', 'orders', 'products', 'transactions'];
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_dropped_objects()
    LOOP
        -- 특정 중요 테이블 보호
        IF obj.object_type = 'table' AND obj.object_name = ANY(critical_objects) THEN
            RAISE EXCEPTION '중요 테이블 %은(는) 삭제할 수 없습니다. DBA에게 문의하세요.',
                            obj.object_identity;
        END IF;

        -- 프로덕션 스키마의 객체 보호
        IF obj.schema_name = 'production' THEN
            RAISE EXCEPTION '프로덕션 스키마의 객체는 삭제할 수 없습니다: %',
                            obj.object_identity;
        END IF;
    END LOOP;
END;
$$;

CREATE EVENT TRIGGER drop_protection
    ON sql_drop
    EXECUTE FUNCTION prevent_drops();
```

---

## 8. C 언어로 이벤트 트리거 함수 작성

### 8.1 EventTriggerData 구조체

C에서 이벤트 트리거 함수를 작성할 때 `EventTriggerData` 구조체를 사용합니다.

```c
typedef struct EventTriggerData
{
    NodeTag     type;       /* 항상 T_EventTriggerData */
    const char *event;      /* 이벤트 이름 */
    Node       *parsetree;  /* 명령의 파싱 트리 */
    CommandTag  tag;        /* 명령 태그 */
} EventTriggerData;
```

| 멤버 | 설명 |
|------|------|
| `type` | 항상 `T_EventTriggerData` |
| `event` | 이벤트 이름: `"login"`, `"ddl_command_start"`, `"ddl_command_end"`, `"sql_drop"`, `"table_rewrite"` |
| `parsetree` | 명령의 파싱 트리 포인터 (구조는 버전에 따라 변경될 수 있음) |
| `tag` | 이벤트와 연관된 명령 태그 (예: `"CREATE FUNCTION"`) |

### 8.2 C 함수 예제: 모든 DDL 차단

```c
#include "postgres.h"
#include "commands/event_trigger.h"
#include "fmgr.h"

PG_MODULE_MAGIC;

PG_FUNCTION_INFO_V1(noddl);

Datum
noddl(PG_FUNCTION_ARGS)
{
    EventTriggerData *trigdata;

    /* 이벤트 트리거 관리자에 의해 호출되었는지 확인 */
    if (!CALLED_AS_EVENT_TRIGGER(fcinfo))
        elog(ERROR, "이벤트 트리거 관리자에 의해 호출되지 않았습니다");

    trigdata = (EventTriggerData *) fcinfo->context;

    /* 오류 발생시켜 DDL 차단 */
    ereport(ERROR,
            (errcode(ERRCODE_INSUFFICIENT_PRIVILEGE),
             errmsg("명령 \"%s\"이(가) 거부되었습니다",
                    GetCommandTagName(trigdata->tag))));

    PG_RETURN_NULL();
}
```

### 8.3 C 함수 등록 및 트리거 생성

```sql
-- 함수 등록
CREATE FUNCTION noddl() RETURNS event_trigger
    AS 'noddl' LANGUAGE C;

-- 이벤트 트리거 생성
CREATE EVENT TRIGGER noddl ON ddl_command_start
    EXECUTE FUNCTION noddl();
```

### 8.4 테스트

```sql
-- 이벤트 트리거 목록 확인
\dy

-- DDL 시도 (차단됨)
CREATE TABLE test_table(id serial);
-- 오류: 명령 "CREATE TABLE"이(가) 거부되었습니다

-- 트리거 일시 비활성화하여 작업 수행
BEGIN;
ALTER EVENT TRIGGER noddl DISABLE;
CREATE TABLE test_table(id serial);
ALTER EVENT TRIGGER noddl ENABLE;
COMMIT;
```

---

## 9. 주의사항 및 제한사항

### 9.1 트랜잭션 중단 시 동작

- 이벤트 트리거는 중단된 트랜잭션에서 실행될 수 없습니다
- DDL 명령이 실패하면 `ddl_command_end` 트리거는 실행되지 않습니다
- `ddl_command_start` 트리거가 실패하면:
  - 추가 트리거가 실행되지 않음
  - 명령이 실행되지 않음
- `ddl_command_end` 트리거가 실패하면:
  - DDL 효과가 롤백됨

### 9.2 트리거가 발생하지 않는 DDL 명령

다음 객체에 대한 DDL 명령은 이벤트 트리거를 발생시키지 않습니다:

- 공유 객체: 데이터베이스, 역할(Role), 테이블스페이스
- 매개변수 권한: `ALTER SYSTEM` 명령
- 이벤트 트리거 자체: 무한 재귀 방지

### 9.3 성능 고려사항

```sql
-- 비효율적: 모든 DDL에 트리거 실행
CREATE EVENT TRIGGER slow_trigger
    ON ddl_command_end
    EXECUTE FUNCTION heavy_processing();

-- 효율적: WHEN 절로 필터링
CREATE EVENT TRIGGER fast_trigger
    ON ddl_command_end
    WHEN TAG IN ('CREATE TABLE', 'DROP TABLE')
    EXECUTE FUNCTION heavy_processing();
```

### 9.4 권한 요구사항

- 이벤트 트리거를 생성하려면 수퍼유저 권한이 필요합니다
- 일반 사용자는 이벤트 트리거를 생성, 수정, 삭제할 수 없습니다

### 9.5 이벤트 트리거 실행 순서

동일한 이벤트에 여러 트리거가 정의된 경우:
- 트리거 이름의 알파벳 순서 로 실행됩니다
- 예: `a_trigger`가 `b_trigger`보다 먼저 실행

```sql
-- 실행 순서 제어를 위한 명명 규칙
CREATE EVENT TRIGGER aa_first_trigger ON ddl_command_start
    EXECUTE FUNCTION first_function();

CREATE EVENT TRIGGER zz_last_trigger ON ddl_command_start
    EXECUTE FUNCTION last_function();
```

### 9.6 디버깅 팁

```sql
-- 이벤트 트리거 디버깅을 위한 로깅
CREATE OR REPLACE FUNCTION debug_event_trigger()
RETURNS event_trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE LOG 'Event Trigger Debug: event=%, tag=%',
              TG_EVENT, TG_TAG;
END;
$$;

-- 로그 레벨 조정
SET client_min_messages = LOG;
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Event Triggers](https://www.postgresql.org/docs/current/event-triggers.html)
- [PostgreSQL 공식 문서 - Event Trigger Functions](https://www.postgresql.org/docs/current/functions-event-triggers.html)
- [CREATE EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-createeventtrigger.html)
- [ALTER EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-altereventtrigger.html)
- [DROP EVENT TRIGGER](https://www.postgresql.org/docs/current/sql-dropeventtrigger.html)
