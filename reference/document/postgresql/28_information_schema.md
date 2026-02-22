# PostgreSQL 정보 스키마 (Information Schema)

이 문서는 PostgreSQL 공식 문서의 "Chapter 37. The Information Schema"를 한국어로 번역한 것입니다.

## 개요

정보 스키마(Information Schema) 는 현재 데이터베이스에 정의된 객체들에 대한 정보를 담고 있는 뷰(View)의 집합 입니다. 정보 스키마는 SQL 표준에 정의되어 있으므로 다른 데이터베이스 시스템과의 이식성(portability)과 안정성(stability)이 우수합니다.

---

## 37.1. 정보 스키마란?

정보 스키마는 `information_schema`라는 이름의 스키마로, 모든 데이터베이스에 자동으로 존재합니다. 이 스키마의 소유자는 초기 데이터베이스 사용자이며, 해당 사용자는 당연히 이 스키마를 삭제할 수 있지만, 삭제하지 않는 것이 좋습니다.

### 시스템 카탈로그와의 차이점

| 특성 | 정보 스키마 (Information Schema) | 시스템 카탈로그 (System Catalog) |
|------|----------------------------------|----------------------------------|
| 표준 준수 | SQL 표준 | PostgreSQL 고유 |
| 이식성 | 다른 DBMS와 호환 | PostgreSQL 전용 |
| 안정성 | 버전 간 변경이 적음 | PostgreSQL 내부 변경에 따라 변동 가능 |
| 상세 정보 | 표준에 정의된 정보만 제공 | PostgreSQL 특화 정보까지 제공 |

### 사용 방법

정보 스키마의 뷰를 쿼리하려면 `information_schema` 스키마를 명시적으로 지정해야 합니다:

```sql
-- 스키마를 명시적으로 지정
SELECT * FROM information_schema.tables;

-- search_path에 추가하는 방법
SET search_path TO information_schema, public;
SELECT * FROM tables;
```

### 데이터 타입

정보 스키마는 SQL 표준에 정의된 특별한 데이터 타입을 사용합니다:

| 타입 | 설명 |
|------|------|
| `sql_identifier` | SQL 식별자를 위한 도메인, `character varying(128)` 기반 |
| `character_data` | 문자 데이터를 위한 도메인, `character varying` 기반 |
| `cardinal_number` | 음이 아닌 정수를 위한 도메인, `integer` 기반 |
| `yes_or_no` | 불리언 값을 나타내며, `YES` 또는 `NO` 문자열 |
| `time_stamp` | 타임스탬프를 위한 도메인 |

---

## 37.2. 주요 정보 스키마 뷰

### 37.2.1. information_schema_catalog_name

현재 데이터베이스(카탈로그)의 이름을 포함하는 테이블입니다. 항상 단일 행만 포함합니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `catalog_name` | `sql_identifier` | 현재 데이터베이스 이름 |

```sql
SELECT * FROM information_schema.information_schema_catalog_name;
```

---

## 37.3. schemata - 스키마 정보

`schemata` 뷰는 현재 데이터베이스에 존재하는 모든 스키마에 대한 정보를 제공합니다. 현재 사용자가 접근 권한을 가진 스키마만 표시됩니다.

### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `catalog_name` | `sql_identifier` | 데이터베이스 이름 (항상 현재 데이터베이스) |
| `schema_name` | `sql_identifier` | 스키마 이름 |
| `schema_owner` | `sql_identifier` | 스키마 소유자 이름 |
| `default_character_set_catalog` | `sql_identifier` | PostgreSQL에서 미지원 (항상 null) |
| `default_character_set_schema` | `sql_identifier` | PostgreSQL에서 미지원 (항상 null) |
| `default_character_set_name` | `sql_identifier` | PostgreSQL에서 미지원 (항상 null) |
| `sql_path` | `character_data` | PostgreSQL에서 미지원 (항상 null) |

### 예제

```sql
-- 모든 스키마 조회
SELECT schema_name, schema_owner
FROM information_schema.schemata
ORDER BY schema_name;

-- 특정 사용자가 소유한 스키마 조회
SELECT schema_name
FROM information_schema.schemata
WHERE schema_owner = 'myuser';
```

---

## 37.4. tables - 테이블 정보

`tables` 뷰는 현재 데이터베이스의 모든 테이블과 뷰에 대한 정보를 제공합니다. 현재 사용자가 접근 권한을 가진 객체만 표시됩니다.

### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `table_catalog` | `sql_identifier` | 테이블이 포함된 데이터베이스 이름 |
| `table_schema` | `sql_identifier` | 테이블이 포함된 스키마 이름 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `table_type` | `character_data` | 테이블 유형 (아래 표 참조) |
| `self_referencing_column_name` | `sql_identifier` | PostgreSQL에서 미지원 |
| `reference_generation` | `character_data` | PostgreSQL에서 미지원 |
| `user_defined_type_catalog` | `sql_identifier` | 타입화된 테이블의 기본 타입 데이터베이스 |
| `user_defined_type_schema` | `sql_identifier` | 타입화된 테이블의 기본 타입 스키마 |
| `user_defined_type_name` | `sql_identifier` | 타입화된 테이블의 기본 타입 이름 |
| `is_insertable_into` | `yes_or_no` | 테이블에 삽입 가능 여부 |
| `is_typed` | `yes_or_no` | 타입화된 테이블 여부 |
| `commit_action` | `character_data` | 아직 구현되지 않음 |

### table_type 값

| 값 | 설명 |
|----|------|
| `BASE TABLE` | 일반 테이블 |
| `VIEW` | 뷰 |
| `FOREIGN` | 외부 테이블 (Foreign Table) |
| `LOCAL TEMPORARY` | 임시 테이블 |

### 예제

```sql
-- public 스키마의 모든 테이블 조회
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- 일반 테이블만 조회 (뷰 제외)
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_type = 'BASE TABLE'
  AND table_schema NOT IN ('information_schema', 'pg_catalog');

-- 각 스키마별 테이블 개수
SELECT table_schema, COUNT(*) as table_count
FROM information_schema.tables
WHERE table_type = 'BASE TABLE'
GROUP BY table_schema
ORDER BY table_count DESC;
```

---

## 37.5. columns - 컬럼 정보

`columns` 뷰는 데이터베이스의 모든 테이블 및 뷰 컬럼에 대한 상세 정보를 제공합니다.

### 기본 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `table_catalog` | `sql_identifier` | 테이블이 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 테이블이 속한 스키마 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `column_name` | `sql_identifier` | 컬럼 이름 |
| `ordinal_position` | `cardinal_number` | 테이블 내 컬럼 순서 (1부터 시작) |
| `column_default` | `character_data` | 기본값 표현식 |
| `is_nullable` | `yes_or_no` | NULL 허용 여부 |
| `data_type` | `character_data` | 데이터 타입 이름 |

### 데이터 타입 관련 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `character_maximum_length` | `cardinal_number` | 문자/비트 타입의 최대 길이 |
| `character_octet_length` | `cardinal_number` | 문자 타입의 최대 바이트 수 |
| `numeric_precision` | `cardinal_number` | 숫자 타입의 정밀도 |
| `numeric_precision_radix` | `cardinal_number` | 정밀도 기수 (2 또는 10) |
| `numeric_scale` | `cardinal_number` | 숫자 타입의 스케일 (소수점 이하 자릿수) |
| `datetime_precision` | `cardinal_number` | 날짜/시간 타입의 소수 초 정밀도 |

### 도메인 관련 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `domain_catalog` | `sql_identifier` | 도메인이 정의된 데이터베이스 |
| `domain_schema` | `sql_identifier` | 도메인이 정의된 스키마 |
| `domain_name` | `sql_identifier` | 도메인 이름 |

### 식별 컬럼(Identity Column) 관련

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `is_identity` | `yes_or_no` | 식별 컬럼 여부 |
| `identity_generation` | `character_data` | `ALWAYS` 또는 `BY DEFAULT` |
| `identity_start` | `character_data` | 시퀀스 시작값 |
| `identity_increment` | `character_data` | 시퀀스 증분 |
| `identity_maximum` | `character_data` | 시퀀스 최대값 |
| `identity_minimum` | `character_data` | 시퀀스 최소값 |
| `identity_cycle` | `yes_or_no` | 시퀀스 순환 여부 |

### 생성된 컬럼(Generated Column) 관련

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `is_generated` | `character_data` | `ALWAYS` 또는 `NEVER` |
| `generation_expression` | `character_data` | 생성 표현식 |

### 기타 컬럼

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `udt_catalog` | `sql_identifier` | 컬럼 데이터 타입이 정의된 데이터베이스 |
| `udt_schema` | `sql_identifier` | 컬럼 데이터 타입이 정의된 스키마 |
| `udt_name` | `sql_identifier` | 컬럼 데이터 타입 이름 |
| `is_updatable` | `yes_or_no` | 컬럼 업데이트 가능 여부 |
| `collation_name` | `sql_identifier` | 콜레이션 이름 |

### 예제

```sql
-- 특정 테이블의 컬럼 정보 조회
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'users'
ORDER BY ordinal_position;

-- 모든 TEXT 타입 컬럼 찾기
SELECT
    table_schema,
    table_name,
    column_name
FROM information_schema.columns
WHERE data_type = 'text'
  AND table_schema = 'public';

-- NULL이 허용되지 않는 컬럼 조회
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE is_nullable = 'NO'
  AND table_schema = 'public'
ORDER BY table_name, ordinal_position;

-- 기본값이 있는 컬럼 조회
SELECT
    table_name,
    column_name,
    column_default
FROM information_schema.columns
WHERE column_default IS NOT NULL
  AND table_schema = 'public';

-- 테이블별 컬럼 수 조회
SELECT
    table_name,
    COUNT(*) as column_count
FROM information_schema.columns
WHERE table_schema = 'public'
GROUP BY table_name
ORDER BY column_count DESC;
```

---

## 37.6. views - 뷰 정보

`views` 뷰는 현재 데이터베이스의 모든 뷰에 대한 정보를 제공합니다.

### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `table_catalog` | `sql_identifier` | 뷰가 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 뷰가 속한 스키마 |
| `table_name` | `sql_identifier` | 뷰 이름 |
| `view_definition` | `character_data` | 뷰 정의 쿼리 (소유자가 아니면 null) |
| `check_option` | `character_data` | CHECK OPTION: `CASCADED`, `LOCAL`, 또는 `NONE` |
| `is_updatable` | `yes_or_no` | UPDATE/DELETE 가능 여부 |
| `is_insertable_into` | `yes_or_no` | INSERT 가능 여부 |
| `is_trigger_updatable` | `yes_or_no` | INSTEAD OF UPDATE 트리거 존재 여부 |
| `is_trigger_deletable` | `yes_or_no` | INSTEAD OF DELETE 트리거 존재 여부 |
| `is_trigger_insertable_into` | `yes_or_no` | INSTEAD OF INSERT 트리거 존재 여부 |

### 예제

```sql
-- 모든 뷰 목록 조회
SELECT table_schema, table_name
FROM information_schema.views
WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
ORDER BY table_schema, table_name;

-- 업데이트 가능한 뷰 조회
SELECT table_name, is_updatable, is_insertable_into
FROM information_schema.views
WHERE is_updatable = 'YES'
  AND table_schema = 'public';

-- 뷰 정의 확인 (소유자만 가능)
SELECT table_name, view_definition
FROM information_schema.views
WHERE table_schema = 'public'
  AND view_definition IS NOT NULL;
```

---

## 37.7. 제약조건 관련 뷰

### 37.7.1. table_constraints - 테이블 제약조건

`table_constraints` 뷰는 테이블에 정의된 모든 제약조건 정보를 제공합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `constraint_catalog` | `sql_identifier` | 제약조건이 속한 데이터베이스 |
| `constraint_schema` | `sql_identifier` | 제약조건이 속한 스키마 |
| `constraint_name` | `sql_identifier` | 제약조건 이름 |
| `table_catalog` | `sql_identifier` | 테이블이 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 테이블이 속한 스키마 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `constraint_type` | `character_data` | 제약조건 유형 |
| `is_deferrable` | `yes_or_no` | 지연 가능 여부 |
| `initially_deferred` | `yes_or_no` | 초기 지연 여부 |
| `enforced` | `yes_or_no` | 제약조건 적용 여부 |
| `nulls_distinct` | `yes_or_no` | UNIQUE에서 NULL 구분 여부 |

#### constraint_type 값

| 값 | 설명 |
|----|------|
| `PRIMARY KEY` | 기본 키 제약조건 |
| `UNIQUE` | 고유 제약조건 |
| `FOREIGN KEY` | 외래 키 제약조건 |
| `CHECK` | 검사 제약조건 |

#### 예제

```sql
-- 특정 테이블의 모든 제약조건 조회
SELECT
    constraint_name,
    constraint_type,
    is_deferrable,
    initially_deferred
FROM information_schema.table_constraints
WHERE table_schema = 'public'
  AND table_name = 'orders';

-- 모든 기본 키 조회
SELECT
    table_schema,
    table_name,
    constraint_name
FROM information_schema.table_constraints
WHERE constraint_type = 'PRIMARY KEY'
  AND table_schema = 'public';

-- 모든 외래 키 조회
SELECT
    table_name,
    constraint_name
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
  AND table_schema = 'public';
```

### 37.7.2. check_constraints - CHECK 제약조건

`check_constraints` 뷰는 CHECK 제약조건의 상세 정보를 제공합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `constraint_catalog` | `sql_identifier` | 제약조건이 속한 데이터베이스 |
| `constraint_schema` | `sql_identifier` | 제약조건이 속한 스키마 |
| `constraint_name` | `sql_identifier` | 제약조건 이름 |
| `check_clause` | `character_data` | CHECK 표현식 |

> 참고: SQL 표준에서 NOT NULL 제약조건도 CHECK 제약조건으로 간주되어 `CHECK (column_name IS NOT NULL)` 형식으로 이 뷰에 포함됩니다.

#### 예제

```sql
-- 모든 CHECK 제약조건과 표현식 조회
SELECT
    constraint_name,
    check_clause
FROM information_schema.check_constraints
WHERE constraint_schema = 'public';
```

### 37.7.3. referential_constraints - 참조 제약조건 (외래 키)

`referential_constraints` 뷰는 외래 키 제약조건의 상세 정보를 제공합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `constraint_catalog` | `sql_identifier` | 제약조건이 속한 데이터베이스 |
| `constraint_schema` | `sql_identifier` | 제약조건이 속한 스키마 |
| `constraint_name` | `sql_identifier` | 제약조건 이름 |
| `unique_constraint_catalog` | `sql_identifier` | 참조되는 제약조건의 데이터베이스 |
| `unique_constraint_schema` | `sql_identifier` | 참조되는 제약조건의 스키마 |
| `unique_constraint_name` | `sql_identifier` | 참조되는 UNIQUE/PK 제약조건 이름 |
| `match_option` | `character_data` | 매칭 옵션: `FULL`, `PARTIAL`, `NONE` |
| `update_rule` | `character_data` | UPDATE 규칙 |
| `delete_rule` | `character_data` | DELETE 규칙 |

#### update_rule / delete_rule 값

| 값 | 설명 |
|----|------|
| `CASCADE` | 참조 행을 함께 변경/삭제 |
| `SET NULL` | 참조 컬럼을 NULL로 설정 |
| `SET DEFAULT` | 참조 컬럼을 기본값으로 설정 |
| `RESTRICT` | 참조하는 행이 있으면 거부 |
| `NO ACTION` | RESTRICT와 유사하지만 지연 가능 |

#### 예제

```sql
-- 외래 키와 참조 관계 조회
SELECT
    constraint_name,
    unique_constraint_name,
    update_rule,
    delete_rule
FROM information_schema.referential_constraints
WHERE constraint_schema = 'public';
```

### 37.7.4. key_column_usage - 키 컬럼 사용

`key_column_usage` 뷰는 PRIMARY KEY, UNIQUE, FOREIGN KEY 제약조건에 사용되는 컬럼을 식별합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `constraint_catalog` | `sql_identifier` | 제약조건이 속한 데이터베이스 |
| `constraint_schema` | `sql_identifier` | 제약조건이 속한 스키마 |
| `constraint_name` | `sql_identifier` | 제약조건 이름 |
| `table_catalog` | `sql_identifier` | 테이블이 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 테이블이 속한 스키마 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `column_name` | `sql_identifier` | 컬럼 이름 |
| `ordinal_position` | `cardinal_number` | 키 내 컬럼 순서 (1부터 시작) |
| `position_in_unique_constraint` | `cardinal_number` | FK의 경우 참조 컬럼의 순서 |

#### 예제

```sql
-- 기본 키 컬럼 조회
SELECT
    tc.table_name,
    kcu.column_name,
    kcu.ordinal_position
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'PRIMARY KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.ordinal_position;

-- 외래 키 관계 상세 조회
SELECT
    kcu.table_name AS fk_table,
    kcu.column_name AS fk_column,
    ccu.table_name AS ref_table,
    ccu.column_name AS ref_column,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
    ON kcu.constraint_name = rc.constraint_name
    AND kcu.constraint_schema = rc.constraint_schema
JOIN information_schema.constraint_column_usage ccu
    ON rc.unique_constraint_name = ccu.constraint_name
    AND rc.unique_constraint_schema = ccu.constraint_schema
WHERE kcu.table_schema = 'public';
```

---

## 37.8. 권한 관련 뷰

### 37.8.1. table_privileges - 테이블 권한

`table_privileges` 뷰는 테이블에 부여된 모든 권한을 표시합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `grantor` | `sql_identifier` | 권한을 부여한 역할 |
| `grantee` | `sql_identifier` | 권한을 받은 역할 |
| `table_catalog` | `sql_identifier` | 테이블이 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 테이블이 속한 스키마 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `privilege_type` | `character_data` | 권한 유형 |
| `is_grantable` | `yes_or_no` | 권한 재부여 가능 여부 |
| `with_hierarchy` | `yes_or_no` | 계층 옵션 포함 여부 |

#### privilege_type 값

| 값 | 설명 |
|----|------|
| `SELECT` | 조회 권한 |
| `INSERT` | 삽입 권한 |
| `UPDATE` | 수정 권한 |
| `DELETE` | 삭제 권한 |
| `TRUNCATE` | 테이블 비우기 권한 |
| `REFERENCES` | 외래 키 참조 권한 |
| `TRIGGER` | 트리거 생성 권한 |

#### 예제

```sql
-- 특정 테이블의 모든 권한 조회
SELECT
    grantee,
    privilege_type,
    is_grantable
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND table_name = 'users'
ORDER BY grantee, privilege_type;

-- 특정 사용자의 모든 권한 조회
SELECT
    table_schema,
    table_name,
    privilege_type
FROM information_schema.table_privileges
WHERE grantee = 'myuser'
ORDER BY table_schema, table_name;
```

### 37.8.2. column_privileges - 컬럼 권한

`column_privileges` 뷰는 컬럼 수준의 권한을 표시합니다.

#### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `grantor` | `sql_identifier` | 권한을 부여한 역할 |
| `grantee` | `sql_identifier` | 권한을 받은 역할 |
| `table_catalog` | `sql_identifier` | 테이블이 속한 데이터베이스 |
| `table_schema` | `sql_identifier` | 테이블이 속한 스키마 |
| `table_name` | `sql_identifier` | 테이블 이름 |
| `column_name` | `sql_identifier` | 컬럼 이름 |
| `privilege_type` | `character_data` | 권한 유형 (`SELECT`, `INSERT`, `UPDATE`, `REFERENCES`) |
| `is_grantable` | `yes_or_no` | 권한 재부여 가능 여부 |

#### 예제

```sql
-- 컬럼 수준 권한 조회
SELECT
    table_name,
    column_name,
    grantee,
    privilege_type
FROM information_schema.column_privileges
WHERE table_schema = 'public'
ORDER BY table_name, column_name;
```

---

## 37.9. routines - 함수/프로시저 정보

`routines` 뷰는 현재 데이터베이스의 모든 함수와 프로시저 정보를 제공합니다.

### 주요 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `specific_catalog` | `sql_identifier` | 함수가 속한 데이터베이스 |
| `specific_schema` | `sql_identifier` | 함수가 속한 스키마 |
| `specific_name` | `sql_identifier` | 함수의 고유 식별자 (오버로딩에도 유일) |
| `routine_catalog` | `sql_identifier` | 함수가 속한 데이터베이스 |
| `routine_schema` | `sql_identifier` | 함수가 속한 스키마 |
| `routine_name` | `sql_identifier` | 함수 이름 (오버로딩 시 중복 가능) |
| `routine_type` | `character_data` | `FUNCTION` 또는 `PROCEDURE` |
| `data_type` | `character_data` | 반환 데이터 타입 |
| `routine_body` | `character_data` | `SQL` 또는 `EXTERNAL` |
| `routine_definition` | `character_data` | 함수 소스 코드 |
| `external_language` | `character_data` | 작성 언어 (예: `plpgsql`, `sql`) |
| `is_deterministic` | `yes_or_no` | 불변(IMMUTABLE) 함수 여부 |
| `security_type` | `character_data` | `INVOKER` 또는 `DEFINER` |

### 예제

```sql
-- 모든 사용자 정의 함수 조회
SELECT
    routine_name,
    routine_type,
    data_type,
    external_language
FROM information_schema.routines
WHERE routine_schema = 'public'
ORDER BY routine_name;

-- 프로시저만 조회
SELECT routine_name, routine_definition
FROM information_schema.routines
WHERE routine_type = 'PROCEDURE'
  AND routine_schema = 'public';

-- 특정 언어로 작성된 함수 조회
SELECT routine_name, external_language
FROM information_schema.routines
WHERE external_language = 'plpgsql'
  AND routine_schema = 'public';
```

---

## 37.10. sequences - 시퀀스 정보

`sequences` 뷰는 현재 데이터베이스의 모든 시퀀스 정보를 제공합니다.

### 컬럼 정보

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `sequence_catalog` | `sql_identifier` | 시퀀스가 속한 데이터베이스 |
| `sequence_schema` | `sql_identifier` | 시퀀스가 속한 스키마 |
| `sequence_name` | `sql_identifier` | 시퀀스 이름 |
| `data_type` | `character_data` | 시퀀스 데이터 타입 |
| `numeric_precision` | `cardinal_number` | 정밀도 |
| `numeric_precision_radix` | `cardinal_number` | 정밀도 기수 |
| `numeric_scale` | `cardinal_number` | 스케일 |
| `start_value` | `character_data` | 시작값 |
| `minimum_value` | `character_data` | 최소값 |
| `maximum_value` | `character_data` | 최대값 |
| `increment` | `character_data` | 증분값 |
| `cycle_option` | `yes_or_no` | 순환 여부 |

### 예제

```sql
-- 모든 시퀀스 조회
SELECT
    sequence_name,
    data_type,
    start_value,
    increment,
    maximum_value
FROM information_schema.sequences
WHERE sequence_schema = 'public';

-- 순환하는 시퀀스 조회
SELECT sequence_name
FROM information_schema.sequences
WHERE cycle_option = 'YES';
```

---

## 37.11. 기타 유용한 뷰

### 37.11.1. domains - 도메인

사용자 정의 도메인에 대한 정보를 제공합니다.

```sql
SELECT domain_name, data_type, domain_default
FROM information_schema.domains
WHERE domain_schema = 'public';
```

### 37.11.2. enabled_roles - 활성화된 역할

현재 세션에서 활성화된 역할을 표시합니다.

```sql
SELECT * FROM information_schema.enabled_roles;
```

### 37.11.3. applicable_roles - 적용 가능한 역할

현재 사용자에게 적용 가능한 모든 역할을 표시합니다.

```sql
SELECT * FROM information_schema.applicable_roles;
```

### 37.11.4. sql_features - SQL 기능

PostgreSQL에서 지원하는 SQL 표준 기능 목록을 제공합니다.

```sql
-- 지원되는 SQL 기능 조회
SELECT feature_id, feature_name, is_supported
FROM information_schema.sql_features
WHERE is_supported = 'YES'
LIMIT 20;
```

---

## 37.12. 실용적인 쿼리 예제

### 테이블 스키마 문서화

```sql
-- 테이블 구조 완전 문서화
SELECT
    c.table_name,
    c.column_name,
    c.ordinal_position,
    c.data_type,
    c.character_maximum_length,
    c.numeric_precision,
    c.is_nullable,
    c.column_default,
    CASE
        WHEN pk.column_name IS NOT NULL THEN 'PK'
        ELSE ''
    END as is_pk
FROM information_schema.columns c
LEFT JOIN (
    SELECT kcu.table_name, kcu.column_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'PRIMARY KEY'
      AND tc.table_schema = 'public'
) pk ON c.table_name = pk.table_name
     AND c.column_name = pk.column_name
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;
```

### 데이터베이스 객체 요약

```sql
-- 스키마별 객체 수 요약
SELECT
    table_schema,
    SUM(CASE WHEN table_type = 'BASE TABLE' THEN 1 ELSE 0 END) as tables,
    SUM(CASE WHEN table_type = 'VIEW' THEN 1 ELSE 0 END) as views,
    SUM(CASE WHEN table_type = 'FOREIGN' THEN 1 ELSE 0 END) as foreign_tables
FROM information_schema.tables
WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
GROUP BY table_schema
ORDER BY table_schema;
```

### 외래 키 관계 다이어그램용 데이터

```sql
-- ERD 생성용 외래 키 관계 추출
SELECT DISTINCT
    kcu.table_name AS from_table,
    kcu.column_name AS from_column,
    ccu.table_name AS to_table,
    ccu.column_name AS to_column
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
    ON kcu.constraint_name = rc.constraint_name
    AND kcu.constraint_schema = rc.constraint_schema
JOIN information_schema.constraint_column_usage ccu
    ON rc.unique_constraint_name = ccu.constraint_name
    AND rc.unique_constraint_schema = ccu.constraint_schema
WHERE kcu.table_schema = 'public'
ORDER BY from_table, to_table;
```

### 권한 감사

```sql
-- 테이블별 권한 요약
SELECT
    table_name,
    grantee,
    STRING_AGG(privilege_type, ', ' ORDER BY privilege_type) as privileges
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND grantee != 'postgres'
GROUP BY table_name, grantee
ORDER BY table_name, grantee;
```

---

## 37.13. 주의사항

### 제약조건 이름 중복 문제

SQL 표준에서는 스키마 내에서 제약조건 이름이 고유해야 하지만, PostgreSQL은 같은 이름의 제약조건을 허용합니다. 따라서 다음 뷰에서 같은 이름의 제약조건이 여러 개 반환될 수 있습니다:

- `check_constraints`
- `domain_constraints`
- `referential_constraints`

### PostgreSQL 고유 정보

정보 스키마는 SQL 표준에 정의된 정보만 제공합니다. PostgreSQL 고유 기능(파티션, 상속, OID 등)에 대한 정보가 필요하면 시스템 카탈로그(`pg_catalog`)를 직접 쿼리해야 합니다:

```sql
-- PostgreSQL 시스템 카탈로그 사용 예
SELECT relname, relkind, reltuples
FROM pg_catalog.pg_class
WHERE relnamespace = 'public'::regnamespace;
```

### 성능 고려사항

정보 스키마 뷰는 시스템 카탈로그를 기반으로 한 복잡한 뷰입니다. 대규모 데이터베이스에서 자주 쿼리하면 성능에 영향을 줄 수 있습니다. 성능이 중요한 경우 시스템 카탈로그를 직접 사용하는 것이 더 효율적일 수 있습니다.

---

## 37.14. 정보 스키마 뷰 전체 목록

| 뷰 이름 | 설명 |
|---------|------|
| `administrable_role_authorizations` | 관리 가능한 역할 권한 |
| `applicable_roles` | 적용 가능한 역할 |
| `attributes` | 복합 타입 속성 |
| `character_sets` | 문자 집합 |
| `check_constraint_routine_usage` | CHECK 제약조건에서 사용된 루틴 |
| `check_constraints` | CHECK 제약조건 |
| `collation_character_set_applicability` | 콜레이션과 문자 집합 관계 |
| `collations` | 콜레이션 |
| `column_column_usage` | 생성된 컬럼 종속성 |
| `column_domain_usage` | 도메인을 사용하는 컬럼 |
| `column_options` | 외부 테이블 컬럼 옵션 |
| `column_privileges` | 컬럼 권한 |
| `column_udt_usage` | UDT를 사용하는 컬럼 |
| `columns` | 컬럼 정보 |
| `constraint_column_usage` | 제약조건에 사용된 컬럼 |
| `constraint_table_usage` | 제약조건에 사용된 테이블 |
| `data_type_privileges` | 데이터 타입 권한 |
| `domain_constraints` | 도메인 제약조건 |
| `domain_udt_usage` | UDT를 사용하는 도메인 |
| `domains` | 도메인 |
| `element_types` | 배열 요소 타입 |
| `enabled_roles` | 활성화된 역할 |
| `foreign_data_wrapper_options` | FDW 옵션 |
| `foreign_data_wrappers` | FDW |
| `foreign_server_options` | 외부 서버 옵션 |
| `foreign_servers` | 외부 서버 |
| `foreign_table_options` | 외부 테이블 옵션 |
| `foreign_tables` | 외부 테이블 |
| `information_schema_catalog_name` | 현재 데이터베이스 이름 |
| `key_column_usage` | 키 컬럼 사용 |
| `parameters` | 루틴 매개변수 |
| `referential_constraints` | 참조 제약조건 |
| `role_column_grants` | 역할별 컬럼 권한 |
| `role_routine_grants` | 역할별 루틴 권한 |
| `role_table_grants` | 역할별 테이블 권한 |
| `role_udt_grants` | 역할별 UDT 권한 |
| `role_usage_grants` | 역할별 사용 권한 |
| `routine_column_usage` | 루틴에서 사용된 컬럼 |
| `routine_privileges` | 루틴 권한 |
| `routine_routine_usage` | 루틴에서 사용된 루틴 |
| `routine_sequence_usage` | 루틴에서 사용된 시퀀스 |
| `routine_table_usage` | 루틴에서 사용된 테이블 |
| `routines` | 함수/프로시저 |
| `schemata` | 스키마 |
| `sequences` | 시퀀스 |
| `sql_features` | SQL 기능 |
| `sql_implementation_info` | SQL 구현 정보 |
| `sql_parts` | SQL 표준 부분 |
| `sql_sizing` | SQL 크기 제한 |
| `table_constraints` | 테이블 제약조건 |
| `table_privileges` | 테이블 권한 |
| `tables` | 테이블 |
| `transforms` | 타입 변환 |
| `triggered_update_columns` | 트리거 업데이트 컬럼 |
| `triggers` | 트리거 |
| `udt_privileges` | UDT 권한 |
| `usage_privileges` | 사용 권한 |
| `user_defined_types` | 사용자 정의 타입 |
| `user_mapping_options` | 사용자 매핑 옵션 |
| `user_mappings` | 사용자 매핑 |
| `view_column_usage` | 뷰에서 사용된 컬럼 |
| `view_routine_usage` | 뷰에서 사용된 루틴 |
| `view_table_usage` | 뷰에서 사용된 테이블 |
| `views` | 뷰 |

---

## 참고 자료

- [PostgreSQL 공식 문서 - The Information Schema](https://www.postgresql.org/docs/current/information-schema.html)
- [SQL:2016 표준 - Information Schema](https://www.iso.org/standard/63555.html)
