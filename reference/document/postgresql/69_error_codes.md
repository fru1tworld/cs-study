# PostgreSQL 18 오류 코드 (Error Codes)

이 문서는 PostgreSQL 18.1 공식 문서의 "Appendix A. PostgreSQL Error Codes"를 한국어로 번역한 것입니다.

## 개요

PostgreSQL 서버가 발생시키는 모든 메시지에는 5자리 오류 코드가 할당되며, 이는 SQL 표준의 "SQLSTATE" 코드 규약을 따릅니다. 특정 오류 조건을 인식해야 하는 애플리케이션은 일반적으로 텍스트 오류 메시지가 아닌 오류 코드를 테스트해야 합니다. 오류 코드는 PostgreSQL 릴리스 간에 변경될 가능성이 적고, 서버 오류 메시지의 지역화(localization)에 영향을 받지 않습니다.

---

## SQLSTATE 오류 코드 체계

### 코드 구조

SQLSTATE 코드는 5자리 문자열로 구성됩니다:

- 처음 2자리: 오류 클래스(Error Class)를 나타냅니다
- 나머지 3자리: 해당 클래스 내의 특정 조건(Specific Condition)을 나타냅니다

예를 들어, `23505` 코드는:
- `23` = 무결성 제약 위반 클래스 (Integrity Constraint Violation)
- `505` = 고유 위반 (Unique Violation)

### 코드 분류

| 코드 패턴 | 의미 |
|-----------|------|
| `XX000` | 클래스 내 정의되지 않은 오류 |
| `XXPXX` | PostgreSQL 전용 오류 코드 (P 포함) |

```sql
-- 오류 코드 확인 예제
DO $$
BEGIN
    INSERT INTO users(id, email) VALUES (1, 'test@example.com');
EXCEPTION
    WHEN unique_violation THEN  -- SQLSTATE '23505'
        RAISE NOTICE '중복된 키가 존재합니다: %', SQLERRM;
    WHEN foreign_key_violation THEN  -- SQLSTATE '23503'
        RAISE NOTICE '외래 키 제약 위반: %', SQLERRM;
END;
$$;
```

---

## 오류 클래스 및 코드 목록

### Class 00 - 성공적 완료 (Successful Completion)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `00000` | `successful_completion` | 작업이 성공적으로 완료됨 |

---

### Class 01 - 경고 (Warning)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `01000` | `warning` | 일반 경고 |
| `0100C` | `dynamic_result_sets_returned` | 동적 결과 집합이 반환됨 |
| `01008` | `implicit_zero_bit_padding` | 암시적 제로 비트 패딩 |
| `01003` | `null_value_eliminated_in_set_function` | 집합 함수에서 NULL 값이 제거됨 |
| `01007` | `privilege_not_granted` | 권한이 부여되지 않음 |
| `01006` | `privilege_not_revoked` | 권한이 취소되지 않음 |
| `01004` | `string_data_right_truncation` | 문자열 데이터 오른쪽 잘림 |
| `01P01` | `deprecated_feature` | 더 이상 사용되지 않는 기능 |

---

### Class 02 - 데이터 없음 (No Data)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `02000` | `no_data` | 데이터 없음 |
| `02001` | `no_additional_dynamic_result_sets_returned` | 추가 동적 결과 집합이 반환되지 않음 |

---

### Class 03 - SQL 문 미완료 (SQL Statement Not Yet Complete)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `03000` | `sql_statement_not_yet_complete` | SQL 문이 아직 완료되지 않음 |

---

### Class 08 - 연결 예외 (Connection Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `08000` | `connection_exception` | 연결 예외 |
| `08003` | `connection_does_not_exist` | 연결이 존재하지 않음 |
| `08006` | `connection_failure` | 연결 실패 |
| `08001` | `sqlclient_unable_to_establish_sqlconnection` | SQL 클라이언트가 SQL 연결을 설정할 수 없음 |
| `08004` | `sqlserver_rejected_establishment_of_sqlconnection` | SQL 서버가 SQL 연결 설정을 거부함 |
| `08007` | `transaction_resolution_unknown` | 트랜잭션 해결 상태 알 수 없음 |
| `08P01` | `protocol_violation` | 프로토콜 위반 |

```sql
-- 연결 오류 처리 예제 (PL/pgSQL)
DO $$
BEGIN
    PERFORM dblink_connect('myconn', 'host=remote_host dbname=mydb');
EXCEPTION
    WHEN connection_exception THEN
        RAISE NOTICE '연결 오류 발생: %', SQLERRM;
    WHEN SQLSTATE '08006' THEN
        RAISE NOTICE '연결 실패: 원격 서버에 연결할 수 없습니다';
END;
$$;
```

---

### Class 09 - 트리거 동작 예외 (Triggered Action Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `09000` | `triggered_action_exception` | 트리거 동작 예외 |

---

### Class 0A - 지원되지 않는 기능 (Feature Not Supported)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0A000` | `feature_not_supported` | 기능이 지원되지 않음 |

```sql
-- 지원되지 않는 기능 예제
DO $$
BEGIN
    -- 지원되지 않는 작업 시도
    EXECUTE 'SOME_UNSUPPORTED_FEATURE';
EXCEPTION
    WHEN feature_not_supported THEN
        RAISE NOTICE '이 기능은 현재 PostgreSQL 버전에서 지원되지 않습니다';
END;
$$;
```

---

### Class 0B - 잘못된 트랜잭션 시작 (Invalid Transaction Initiation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0B000` | `invalid_transaction_initiation` | 잘못된 트랜잭션 시작 |

---

### Class 0F - 로케이터 예외 (Locator Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0F000` | `locator_exception` | 로케이터 예외 |
| `0F001` | `invalid_locator_specification` | 잘못된 로케이터 지정 |

---

### Class 0L - 잘못된 권한 부여자 (Invalid Grantor)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0L000` | `invalid_grantor` | 잘못된 권한 부여자 |
| `0LP01` | `invalid_grant_operation` | 잘못된 권한 부여 작업 |

---

### Class 0P - 잘못된 역할 지정 (Invalid Role Specification)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0P000` | `invalid_role_specification` | 잘못된 역할 지정 |

---

### Class 0Z - 진단 예외 (Diagnostics Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `0Z000` | `diagnostics_exception` | 진단 예외 |
| `0Z002` | `stacked_diagnostics_accessed_without_active_handler` | 활성 핸들러 없이 스택 진단에 접근함 |

---

### Class 10 - XQuery 오류 (XQuery Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `10608` | `invalid_argument_for_xquery` | XQuery에 대한 잘못된 인수 |

---

### Class 20 - Case를 찾을 수 없음 (Case Not Found)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `20000` | `case_not_found` | CASE 문에서 일치하는 조건을 찾을 수 없음 |

```sql
-- CASE NOT FOUND 예제
CREATE OR REPLACE FUNCTION get_grade(score INTEGER)
RETURNS TEXT AS $$
BEGIN
    CASE
        WHEN score >= 90 THEN RETURN 'A';
        WHEN score >= 80 THEN RETURN 'B';
        WHEN score >= 70 THEN RETURN 'C';
        -- ELSE 절이 없으면 case_not_found 발생 가능
    END CASE;
EXCEPTION
    WHEN case_not_found THEN
        RETURN 'F';
END;
$$ LANGUAGE plpgsql;
```

---

### Class 21 - 카디널리티 위반 (Cardinality Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `21000` | `cardinality_violation` | 카디널리티 위반 (서브쿼리가 여러 행 반환) |

```sql
-- 카디널리티 위반 예제
DO $$
DECLARE
    v_name TEXT;
BEGIN
    -- 여러 행을 반환하는 서브쿼리를 단일 값에 할당하려고 할 때
    SELECT name INTO STRICT v_name FROM users;  -- 여러 행이 있으면 오류
EXCEPTION
    WHEN cardinality_violation THEN
        RAISE NOTICE '쿼리가 여러 행을 반환했습니다';
    WHEN too_many_rows THEN
        RAISE NOTICE '결과가 너무 많습니다';
END;
$$;
```

---

### Class 22 - 데이터 예외 (Data Exception)

데이터 처리 중 발생하는 다양한 오류를 포함합니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `22000` | `data_exception` | 데이터 예외 |
| `2202E` | `array_subscript_error` | 배열 첨자 오류 |
| `22021` | `character_not_in_repertoire` | 문자가 레퍼토리에 없음 |
| `22008` | `datetime_field_overflow` | 날짜/시간 필드 오버플로우 |
| `22012` | `division_by_zero` | 0으로 나누기 |
| `22005` | `error_in_assignment` | 할당 오류 |
| `2200B` | `escape_character_conflict` | 이스케이프 문자 충돌 |
| `22022` | `indicator_overflow` | 지시자 오버플로우 |
| `22015` | `interval_field_overflow` | 간격 필드 오버플로우 |
| `2201E` | `invalid_argument_for_logarithm` | 로그 함수에 대한 잘못된 인수 |
| `22014` | `invalid_argument_for_ntile_function` | NTILE 함수에 대한 잘못된 인수 |
| `22016` | `invalid_argument_for_nth_value_function` | NTH_VALUE 함수에 대한 잘못된 인수 |
| `2201F` | `invalid_argument_for_power_function` | 거듭제곱 함수에 대한 잘못된 인수 |
| `2201G` | `invalid_argument_for_width_bucket_function` | WIDTH_BUCKET 함수에 대한 잘못된 인수 |
| `22018` | `invalid_character_value_for_cast` | 캐스트에 대한 잘못된 문자 값 |
| `22007` | `invalid_datetime_format` | 잘못된 날짜/시간 형식 |
| `22019` | `invalid_escape_character` | 잘못된 이스케이프 문자 |
| `2200D` | `invalid_escape_octet` | 잘못된 이스케이프 옥텟 |
| `22025` | `invalid_escape_sequence` | 잘못된 이스케이프 시퀀스 |
| `22P06` | `nonstandard_use_of_escape_character` | 비표준 이스케이프 문자 사용 |
| `22010` | `invalid_indicator_parameter_value` | 잘못된 지시자 매개변수 값 |
| `22023` | `invalid_parameter_value` | 잘못된 매개변수 값 |
| `22013` | `invalid_preceding_or_following_size` | 잘못된 PRECEDING 또는 FOLLOWING 크기 |
| `2201B` | `invalid_regular_expression` | 잘못된 정규 표현식 |
| `2201W` | `invalid_row_count_in_limit_clause` | LIMIT 절의 잘못된 행 수 |
| `2201X` | `invalid_row_count_in_result_offset_clause` | 결과 오프셋 절의 잘못된 행 수 |
| `2202H` | `invalid_tablesample_argument` | 잘못된 TABLESAMPLE 인수 |
| `2202G` | `invalid_tablesample_repeat` | 잘못된 TABLESAMPLE REPEAT |
| `22009` | `invalid_time_zone_displacement_value` | 잘못된 시간대 변위 값 |
| `2200C` | `invalid_use_of_escape_character` | 잘못된 이스케이프 문자 사용 |
| `2200G` | `most_specific_type_mismatch` | 가장 구체적인 타입 불일치 |
| `22004` | `null_value_not_allowed` | NULL 값이 허용되지 않음 |
| `22002` | `null_value_no_indicator_parameter` | NULL 값에 지시자 매개변수 없음 |
| `22003` | `numeric_value_out_of_range` | 숫자 값이 범위를 벗어남 |
| `2200H` | `sequence_generator_limit_exceeded` | 시퀀스 생성기 한계 초과 |
| `22026` | `string_data_length_mismatch` | 문자열 데이터 길이 불일치 |
| `22001` | `string_data_right_truncation` | 문자열 데이터 오른쪽 잘림 |
| `22011` | `substring_error` | SUBSTRING 오류 |
| `22027` | `trim_error` | TRIM 오류 |
| `22024` | `unterminated_c_string` | 종료되지 않은 C 문자열 |
| `2200F` | `zero_length_character_string` | 길이가 0인 문자열 |
| `22P01` | `floating_point_exception` | 부동 소수점 예외 |
| `22P02` | `invalid_text_representation` | 잘못된 텍스트 표현 |
| `22P03` | `invalid_binary_representation` | 잘못된 바이너리 표현 |
| `22P04` | `bad_copy_file_format` | 잘못된 COPY 파일 형식 |
| `22P05` | `untranslatable_character` | 변환할 수 없는 문자 |
| `2200L` | `not_an_xml_document` | XML 문서가 아님 |
| `2200M` | `invalid_xml_document` | 잘못된 XML 문서 |
| `2200N` | `invalid_xml_content` | 잘못된 XML 내용 |
| `2200S` | `invalid_xml_comment` | 잘못된 XML 주석 |
| `2200T` | `invalid_xml_processing_instruction` | 잘못된 XML 처리 명령 |

#### JSON 관련 오류

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `22030` | `duplicate_json_object_key_value` | 중복된 JSON 객체 키 값 |
| `22031` | `invalid_argument_for_sql_json_datetime_function` | SQL/JSON 날짜/시간 함수에 대한 잘못된 인수 |
| `22032` | `invalid_json_text` | 잘못된 JSON 텍스트 |
| `22033` | `invalid_sql_json_subscript` | 잘못된 SQL/JSON 첨자 |
| `22034` | `more_than_one_sql_json_item` | SQL/JSON 항목이 하나 이상 |
| `22035` | `no_sql_json_item` | SQL/JSON 항목 없음 |
| `22036` | `non_numeric_sql_json_item` | 숫자가 아닌 SQL/JSON 항목 |
| `22037` | `non_unique_keys_in_a_json_object` | JSON 객체에 고유하지 않은 키 |
| `22038` | `singleton_sql_json_item_required` | 단일 SQL/JSON 항목 필요 |
| `22039` | `sql_json_array_not_found` | SQL/JSON 배열을 찾을 수 없음 |
| `2203A` | `sql_json_member_not_found` | SQL/JSON 멤버를 찾을 수 없음 |
| `2203B` | `sql_json_number_not_found` | SQL/JSON 숫자를 찾을 수 없음 |
| `2203C` | `sql_json_object_not_found` | SQL/JSON 객체를 찾을 수 없음 |
| `2203D` | `too_many_json_array_elements` | JSON 배열 요소가 너무 많음 |
| `2203E` | `too_many_json_object_members` | JSON 객체 멤버가 너무 많음 |
| `2203F` | `sql_json_scalar_required` | SQL/JSON 스칼라 필요 |
| `2203G` | `sql_json_item_cannot_be_cast_to_target_type` | SQL/JSON 항목을 대상 타입으로 캐스트할 수 없음 |

```sql
-- 데이터 예외 처리 예제
DO $$
DECLARE
    v_result NUMERIC;
BEGIN
    -- 0으로 나누기
    v_result := 10 / 0;
EXCEPTION
    WHEN division_by_zero THEN
        RAISE NOTICE '0으로 나눌 수 없습니다';
    WHEN numeric_value_out_of_range THEN
        RAISE NOTICE '숫자가 허용 범위를 초과했습니다';
END;
$$;

-- 잘못된 텍스트 표현 예제
DO $$
BEGIN
    PERFORM '123abc'::INTEGER;
EXCEPTION
    WHEN invalid_text_representation THEN
        RAISE NOTICE '문자열을 숫자로 변환할 수 없습니다: %', SQLERRM;
END;
$$;
```

---

### Class 23 - 무결성 제약 위반 (Integrity Constraint Violation)

데이터 무결성과 관련된 중요한 오류 클래스입니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `23000` | `integrity_constraint_violation` | 무결성 제약 위반 |
| `23001` | `restrict_violation` | RESTRICT 위반 |
| `23502` | `not_null_violation` | NOT NULL 위반 |
| `23503` | `foreign_key_violation` | 외래 키 위반 |
| `23505` | `unique_violation` | 고유 제약 위반 |
| `23514` | `check_violation` | CHECK 제약 위반 |
| `23P01` | `exclusion_violation` | 배제 제약 위반 |

```sql
-- 무결성 제약 위반 처리 예제
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price NUMERIC CHECK (price > 0),
    category_id INTEGER REFERENCES categories(id)
);

-- 오류 처리 함수
CREATE OR REPLACE FUNCTION safe_insert_product(
    p_name VARCHAR,
    p_price NUMERIC,
    p_category_id INTEGER
) RETURNS TEXT AS $$
BEGIN
    INSERT INTO products (name, price, category_id)
    VALUES (p_name, p_price, p_category_id);
    RETURN '성공';
EXCEPTION
    WHEN not_null_violation THEN
        RETURN '오류: 필수 필드가 누락되었습니다';
    WHEN unique_violation THEN
        RETURN '오류: 중복된 값이 존재합니다';
    WHEN foreign_key_violation THEN
        RETURN '오류: 참조하는 카테고리가 존재하지 않습니다';
    WHEN check_violation THEN
        RETURN '오류: 가격은 0보다 커야 합니다';
    WHEN exclusion_violation THEN
        RETURN '오류: 배제 제약 조건을 위반했습니다';
END;
$$ LANGUAGE plpgsql;

-- 사용 예제
SELECT safe_insert_product('노트북', 1500000, 1);
SELECT safe_insert_product(NULL, 1000, 1);        -- not_null_violation
SELECT safe_insert_product('키보드', -100, 1);    -- check_violation
SELECT safe_insert_product('마우스', 50000, 999); -- foreign_key_violation
```

---

### Class 24 - 잘못된 커서 상태 (Invalid Cursor State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `24000` | `invalid_cursor_state` | 잘못된 커서 상태 |

---

### Class 25 - 잘못된 트랜잭션 상태 (Invalid Transaction State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `25000` | `invalid_transaction_state` | 잘못된 트랜잭션 상태 |
| `25001` | `active_sql_transaction` | 활성 SQL 트랜잭션 |
| `25002` | `branch_transaction_already_active` | 분기 트랜잭션이 이미 활성화됨 |
| `25008` | `held_cursor_requires_same_isolation_level` | 보유 커서가 동일한 격리 수준 필요 |
| `25003` | `inappropriate_access_mode_for_branch_transaction` | 분기 트랜잭션에 부적절한 접근 모드 |
| `25004` | `inappropriate_isolation_level_for_branch_transaction` | 분기 트랜잭션에 부적절한 격리 수준 |
| `25005` | `no_active_sql_transaction_for_branch_transaction` | 분기 트랜잭션을 위한 활성 SQL 트랜잭션 없음 |
| `25006` | `read_only_sql_transaction` | 읽기 전용 SQL 트랜잭션 |
| `25007` | `schema_and_data_statement_mixing_not_supported` | 스키마와 데이터 문 혼합이 지원되지 않음 |
| `25P01` | `no_active_sql_transaction` | 활성 SQL 트랜잭션 없음 |
| `25P02` | `in_failed_sql_transaction` | 실패한 SQL 트랜잭션 내에 있음 |
| `25P03` | `idle_in_transaction_session_timeout` | 유휴 트랜잭션 세션 타임아웃 |
| `25P04` | `transaction_timeout` | 트랜잭션 타임아웃 |

```sql
-- 트랜잭션 상태 오류 처리 예제
DO $$
BEGIN
    -- 읽기 전용 트랜잭션에서 쓰기 시도
    SET TRANSACTION READ ONLY;
    INSERT INTO logs (message) VALUES ('test');
EXCEPTION
    WHEN read_only_sql_transaction THEN
        RAISE NOTICE '읽기 전용 트랜잭션에서는 데이터를 수정할 수 없습니다';
    WHEN in_failed_sql_transaction THEN
        RAISE NOTICE '이전 오류로 인해 트랜잭션이 실패한 상태입니다';
END;
$$;
```

---

### Class 26 - 잘못된 SQL 문 이름 (Invalid SQL Statement Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `26000` | `invalid_sql_statement_name` | 잘못된 SQL 문 이름 |

---

### Class 27 - 트리거된 데이터 변경 위반 (Triggered Data Change Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `27000` | `triggered_data_change_violation` | 트리거된 데이터 변경 위반 |

---

### Class 28 - 잘못된 인증 지정 (Invalid Authorization Specification)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `28000` | `invalid_authorization_specification` | 잘못된 인증 지정 |
| `28P01` | `invalid_password` | 잘못된 비밀번호 |

---

### Class 2B - 종속 권한 기술자 존재 (Dependent Privilege Descriptors Still Exist)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2B000` | `dependent_privilege_descriptors_still_exist` | 종속 권한 기술자가 여전히 존재함 |
| `2BP01` | `dependent_objects_still_exist` | 종속 객체가 여전히 존재함 |

```sql
-- 종속 객체 존재 오류 처리
DO $$
BEGIN
    DROP TABLE parent_table;  -- 종속된 자식 테이블이 있는 경우
EXCEPTION
    WHEN dependent_objects_still_exist THEN
        RAISE NOTICE '이 테이블을 참조하는 다른 객체가 존재합니다. CASCADE를 사용하세요.';
END;
$$;
```

---

### Class 2D - 잘못된 트랜잭션 종료 (Invalid Transaction Termination)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2D000` | `invalid_transaction_termination` | 잘못된 트랜잭션 종료 |

---

### Class 2F - SQL 루틴 예외 (SQL Routine Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `2F000` | `sql_routine_exception` | SQL 루틴 예외 |
| `2F005` | `function_executed_no_return_statement` | 함수가 RETURN 문 없이 실행됨 |
| `2F002` | `modifying_sql_data_not_permitted` | SQL 데이터 수정이 허용되지 않음 |
| `2F003` | `prohibited_sql_statement_attempted` | 금지된 SQL 문 시도 |
| `2F004` | `reading_sql_data_not_permitted` | SQL 데이터 읽기가 허용되지 않음 |

---

### Class 34 - 잘못된 커서 이름 (Invalid Cursor Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `34000` | `invalid_cursor_name` | 잘못된 커서 이름 |

---

### Class 38 - 외부 루틴 예외 (External Routine Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `38000` | `external_routine_exception` | 외부 루틴 예외 |
| `38001` | `containing_sql_not_permitted` | 포함된 SQL이 허용되지 않음 |
| `38002` | `modifying_sql_data_not_permitted` | SQL 데이터 수정이 허용되지 않음 |
| `38003` | `prohibited_sql_statement_attempted` | 금지된 SQL 문 시도 |
| `38004` | `reading_sql_data_not_permitted` | SQL 데이터 읽기가 허용되지 않음 |

---

### Class 39 - 외부 루틴 호출 예외 (External Routine Invocation Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `39000` | `external_routine_invocation_exception` | 외부 루틴 호출 예외 |
| `39001` | `invalid_sqlstate_returned` | 잘못된 SQLSTATE 반환됨 |
| `39004` | `null_value_not_allowed` | NULL 값이 허용되지 않음 |
| `39P01` | `trigger_protocol_violated` | 트리거 프로토콜 위반 |
| `39P02` | `srf_protocol_violated` | SRF(Set-Returning Function) 프로토콜 위반 |
| `39P03` | `event_trigger_protocol_violated` | 이벤트 트리거 프로토콜 위반 |

---

### Class 3B - 세이브포인트 예외 (Savepoint Exception)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3B000` | `savepoint_exception` | 세이브포인트 예외 |
| `3B001` | `invalid_savepoint_specification` | 잘못된 세이브포인트 지정 |

---

### Class 3D - 잘못된 카탈로그 이름 (Invalid Catalog Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3D000` | `invalid_catalog_name` | 잘못된 카탈로그 이름 |

---

### Class 3F - 잘못된 스키마 이름 (Invalid Schema Name)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `3F000` | `invalid_schema_name` | 잘못된 스키마 이름 |

---

### Class 40 - 트랜잭션 롤백 (Transaction Rollback)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `40000` | `transaction_rollback` | 트랜잭션 롤백 |
| `40002` | `transaction_integrity_constraint_violation` | 트랜잭션 무결성 제약 위반 |
| `40001` | `serialization_failure` | 직렬화 실패 |
| `40003` | `statement_completion_unknown` | 문 완료 상태 알 수 없음 |
| `40P01` | `deadlock_detected` | 교착 상태 감지 |

```sql
-- 트랜잭션 롤백 및 교착 상태 처리 예제
CREATE OR REPLACE FUNCTION transfer_funds(
    p_from_account INTEGER,
    p_to_account INTEGER,
    p_amount NUMERIC
) RETURNS TEXT AS $$
DECLARE
    v_retry_count INTEGER := 0;
    v_max_retries INTEGER := 3;
BEGIN
    LOOP
        BEGIN
            -- 자금 이체 로직
            UPDATE accounts
            SET balance = balance - p_amount
            WHERE id = p_from_account;

            UPDATE accounts
            SET balance = balance + p_amount
            WHERE id = p_to_account;

            RETURN '이체 성공';

        EXCEPTION
            WHEN deadlock_detected THEN
                v_retry_count := v_retry_count + 1;
                IF v_retry_count >= v_max_retries THEN
                    RAISE EXCEPTION '교착 상태로 인해 최대 재시도 횟수를 초과했습니다';
                END IF;
                RAISE NOTICE '교착 상태 감지, 재시도 중... (%/%)', v_retry_count, v_max_retries;
                PERFORM pg_sleep(0.1 * v_retry_count);  -- 백오프

            WHEN serialization_failure THEN
                v_retry_count := v_retry_count + 1;
                IF v_retry_count >= v_max_retries THEN
                    RAISE EXCEPTION '직렬화 실패로 인해 최대 재시도 횟수를 초과했습니다';
                END IF;
                RAISE NOTICE '직렬화 실패, 재시도 중... (%/%)', v_retry_count, v_max_retries;
                PERFORM pg_sleep(0.1 * v_retry_count);
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

---

### Class 42 - 구문 오류 또는 접근 규칙 위반 (Syntax Error or Access Rule Violation)

가장 흔하게 발생하는 오류 클래스 중 하나입니다.

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `42000` | `syntax_error_or_access_rule_violation` | 구문 오류 또는 접근 규칙 위반 |
| `42601` | `syntax_error` | 구문 오류 |
| `42501` | `insufficient_privilege` | 권한 부족 |
| `42846` | `cannot_coerce` | 변환할 수 없음 |
| `42803` | `grouping_error` | 그룹화 오류 |
| `42P20` | `windowing_error` | 윈도우 함수 오류 |
| `42P19` | `invalid_recursion` | 잘못된 재귀 |
| `42830` | `invalid_foreign_key` | 잘못된 외래 키 |
| `42602` | `invalid_name` | 잘못된 이름 |
| `42622` | `name_too_long` | 이름이 너무 김 |
| `42939` | `reserved_name` | 예약된 이름 |
| `42804` | `datatype_mismatch` | 데이터 타입 불일치 |
| `42P18` | `indeterminate_datatype` | 불확정 데이터 타입 |
| `42P21` | `collation_mismatch` | 정렬 규칙 불일치 |
| `42P22` | `indeterminate_collation` | 불확정 정렬 규칙 |
| `42809` | `wrong_object_type` | 잘못된 객체 타입 |
| `428C9` | `generated_always` | GENERATED ALWAYS 제약 |
| `42703` | `undefined_column` | 정의되지 않은 열 |
| `42883` | `undefined_function` | 정의되지 않은 함수 |
| `42P01` | `undefined_table` | 정의되지 않은 테이블 |
| `42P02` | `undefined_parameter` | 정의되지 않은 매개변수 |
| `42704` | `undefined_object` | 정의되지 않은 객체 |
| `42701` | `duplicate_column` | 중복된 열 |
| `42P03` | `duplicate_cursor` | 중복된 커서 |
| `42P04` | `duplicate_database` | 중복된 데이터베이스 |
| `42723` | `duplicate_function` | 중복된 함수 |
| `42P05` | `duplicate_prepared_statement` | 중복된 준비된 문 |
| `42P06` | `duplicate_schema` | 중복된 스키마 |
| `42P07` | `duplicate_table` | 중복된 테이블 |
| `42712` | `duplicate_alias` | 중복된 별칭 |
| `42710` | `duplicate_object` | 중복된 객체 |
| `42702` | `ambiguous_column` | 모호한 열 |
| `42725` | `ambiguous_function` | 모호한 함수 |
| `42P08` | `ambiguous_parameter` | 모호한 매개변수 |
| `42P09` | `ambiguous_alias` | 모호한 별칭 |
| `42P10` | `invalid_column_reference` | 잘못된 열 참조 |
| `42611` | `invalid_column_definition` | 잘못된 열 정의 |
| `42P11` | `invalid_cursor_definition` | 잘못된 커서 정의 |
| `42P12` | `invalid_database_definition` | 잘못된 데이터베이스 정의 |
| `42P13` | `invalid_function_definition` | 잘못된 함수 정의 |
| `42P14` | `invalid_prepared_statement_definition` | 잘못된 준비된 문 정의 |
| `42P15` | `invalid_schema_definition` | 잘못된 스키마 정의 |
| `42P16` | `invalid_table_definition` | 잘못된 테이블 정의 |
| `42P17` | `invalid_object_definition` | 잘못된 객체 정의 |

```sql
-- 구문 및 접근 오류 처리 예제
DO $$
BEGIN
    -- 존재하지 않는 테이블 참조
    PERFORM * FROM nonexistent_table;
EXCEPTION
    WHEN undefined_table THEN
        RAISE NOTICE '테이블이 존재하지 않습니다: %', SQLERRM;
    WHEN undefined_column THEN
        RAISE NOTICE '열이 존재하지 않습니다: %', SQLERRM;
    WHEN insufficient_privilege THEN
        RAISE NOTICE '이 작업을 수행할 권한이 없습니다';
    WHEN syntax_error THEN
        RAISE NOTICE 'SQL 구문 오류: %', SQLERRM;
END;
$$;

-- 중복 객체 처리 예제
DO $$
BEGIN
    CREATE TABLE existing_table (id INT);
EXCEPTION
    WHEN duplicate_table THEN
        RAISE NOTICE '테이블이 이미 존재합니다. CREATE TABLE IF NOT EXISTS를 사용하세요.';
    WHEN duplicate_object THEN
        RAISE NOTICE '객체가 이미 존재합니다';
END;
$$;
```

---

### Class 44 - WITH CHECK OPTION 위반 (WITH CHECK OPTION Violation)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `44000` | `with_check_option_violation` | WITH CHECK OPTION 위반 |

```sql
-- WITH CHECK OPTION 위반 예제
CREATE VIEW active_users AS
    SELECT * FROM users WHERE status = 'active'
    WITH CHECK OPTION;

-- 뷰를 통해 비활성 상태로 변경하려고 하면 오류 발생
DO $$
BEGIN
    UPDATE active_users SET status = 'inactive' WHERE id = 1;
EXCEPTION
    WHEN with_check_option_violation THEN
        RAISE NOTICE '뷰의 WITH CHECK OPTION 조건을 위반하는 변경은 허용되지 않습니다';
END;
$$;
```

---

### Class 53 - 리소스 부족 (Insufficient Resources)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `53000` | `insufficient_resources` | 리소스 부족 |
| `53100` | `disk_full` | 디스크 가득 참 |
| `53200` | `out_of_memory` | 메모리 부족 |
| `53300` | `too_many_connections` | 연결이 너무 많음 |
| `53400` | `configuration_limit_exceeded` | 구성 한계 초과 |

```sql
-- 리소스 부족 오류 처리 (애플리케이션 레벨)
DO $$
BEGIN
    -- 대용량 데이터 처리
    PERFORM process_large_dataset();
EXCEPTION
    WHEN out_of_memory THEN
        RAISE NOTICE '메모리가 부족합니다. work_mem 설정을 확인하세요.';
    WHEN disk_full THEN
        RAISE NOTICE '디스크 공간이 부족합니다. 디스크 정리가 필요합니다.';
    WHEN too_many_connections THEN
        RAISE NOTICE '최대 연결 수를 초과했습니다. max_connections 설정을 확인하세요.';
END;
$$;
```

---

### Class 54 - 프로그램 한계 초과 (Program Limit Exceeded)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `54000` | `program_limit_exceeded` | 프로그램 한계 초과 |
| `54001` | `statement_too_complex` | 문이 너무 복잡함 |
| `54011` | `too_many_columns` | 열이 너무 많음 |
| `54023` | `too_many_arguments` | 인수가 너무 많음 |

---

### Class 55 - 객체가 필수 상태가 아님 (Object Not In Prerequisite State)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `55000` | `object_not_in_prerequisite_state` | 객체가 필수 상태가 아님 |
| `55006` | `object_in_use` | 객체가 사용 중 |
| `55P02` | `cant_change_runtime_param` | 런타임 매개변수를 변경할 수 없음 |
| `55P03` | `lock_not_available` | 잠금을 사용할 수 없음 |
| `55P04` | `unsafe_new_enum_value_usage` | 안전하지 않은 새 열거형 값 사용 |

```sql
-- 잠금 관련 오류 처리
DO $$
BEGIN
    -- NOWAIT 옵션으로 잠금 시도
    LOCK TABLE critical_table IN ACCESS EXCLUSIVE MODE NOWAIT;
    -- 작업 수행
EXCEPTION
    WHEN lock_not_available THEN
        RAISE NOTICE '테이블이 다른 세션에서 사용 중입니다. 나중에 다시 시도하세요.';
    WHEN object_in_use THEN
        RAISE NOTICE '객체가 현재 사용 중입니다';
END;
$$;
```

---

### Class 57 - 운영자 개입 (Operator Intervention)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `57000` | `operator_intervention` | 운영자 개입 |
| `57014` | `query_canceled` | 쿼리가 취소됨 |
| `57P01` | `admin_shutdown` | 관리자 종료 |
| `57P02` | `crash_shutdown` | 충돌 종료 |
| `57P03` | `cannot_connect_now` | 현재 연결할 수 없음 |
| `57P04` | `database_dropped` | 데이터베이스가 삭제됨 |
| `57P05` | `idle_session_timeout` | 유휴 세션 타임아웃 |

```sql
-- 쿼리 취소 처리 예제
DO $$
BEGIN
    -- 장시간 실행되는 쿼리
    PERFORM long_running_query();
EXCEPTION
    WHEN query_canceled THEN
        RAISE NOTICE '쿼리가 사용자 또는 타임아웃에 의해 취소되었습니다';
    WHEN admin_shutdown THEN
        RAISE NOTICE '서버가 관리자에 의해 종료 중입니다';
END;
$$;
```

---

### Class 58 - 시스템 오류 (System Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `58000` | `system_error` | 시스템 오류 |
| `58030` | `io_error` | I/O 오류 |
| `58P01` | `undefined_file` | 정의되지 않은 파일 |
| `58P02` | `duplicate_file` | 중복된 파일 |
| `58P03` | `file_name_too_long` | 파일 이름이 너무 김 |

---

### Class F0 - 구성 파일 오류 (Configuration File Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `F0000` | `config_file_error` | 구성 파일 오류 |
| `F0001` | `lock_file_exists` | 잠금 파일이 존재함 |

---

### Class HV - 외부 데이터 래퍼 오류 (Foreign Data Wrapper Error - SQL/MED)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `HV000` | `fdw_error` | FDW 오류 |
| `HV005` | `fdw_column_name_not_found` | FDW 열 이름을 찾을 수 없음 |
| `HV002` | `fdw_dynamic_parameter_value_needed` | FDW 동적 매개변수 값 필요 |
| `HV010` | `fdw_function_sequence_error` | FDW 함수 순서 오류 |
| `HV021` | `fdw_inconsistent_descriptor_information` | FDW 일관성 없는 기술자 정보 |
| `HV024` | `fdw_invalid_attribute_value` | FDW 잘못된 속성 값 |
| `HV007` | `fdw_invalid_column_name` | FDW 잘못된 열 이름 |
| `HV008` | `fdw_invalid_column_number` | FDW 잘못된 열 번호 |
| `HV004` | `fdw_invalid_data_type` | FDW 잘못된 데이터 타입 |
| `HV006` | `fdw_invalid_data_type_descriptors` | FDW 잘못된 데이터 타입 기술자 |
| `HV091` | `fdw_invalid_descriptor_field_identifier` | FDW 잘못된 기술자 필드 식별자 |
| `HV00B` | `fdw_invalid_handle` | FDW 잘못된 핸들 |
| `HV00C` | `fdw_invalid_option_index` | FDW 잘못된 옵션 인덱스 |
| `HV00D` | `fdw_invalid_option_name` | FDW 잘못된 옵션 이름 |
| `HV090` | `fdw_invalid_string_length_or_buffer_length` | FDW 잘못된 문자열 또는 버퍼 길이 |
| `HV00A` | `fdw_invalid_string_format` | FDW 잘못된 문자열 형식 |
| `HV009` | `fdw_invalid_use_of_null_pointer` | FDW NULL 포인터의 잘못된 사용 |
| `HV014` | `fdw_too_many_handles` | FDW 핸들이 너무 많음 |
| `HV001` | `fdw_out_of_memory` | FDW 메모리 부족 |
| `HV00P` | `fdw_no_schemas` | FDW 스키마 없음 |
| `HV00J` | `fdw_option_name_not_found` | FDW 옵션 이름을 찾을 수 없음 |
| `HV00K` | `fdw_reply_handle` | FDW 응답 핸들 |
| `HV00Q` | `fdw_schema_not_found` | FDW 스키마를 찾을 수 없음 |
| `HV00R` | `fdw_table_not_found` | FDW 테이블을 찾을 수 없음 |
| `HV00L` | `fdw_unable_to_create_execution` | FDW 실행을 생성할 수 없음 |
| `HV00M` | `fdw_unable_to_create_reply` | FDW 응답을 생성할 수 없음 |
| `HV00N` | `fdw_unable_to_establish_connection` | FDW 연결을 설정할 수 없음 |

---

### Class P0 - PL/pgSQL 오류 (PL/pgSQL Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `P0000` | `plpgsql_error` | PL/pgSQL 오류 |
| `P0001` | `raise_exception` | RAISE EXCEPTION |
| `P0002` | `no_data_found` | 데이터를 찾을 수 없음 |
| `P0003` | `too_many_rows` | 행이 너무 많음 |
| `P0004` | `assert_failure` | ASSERT 실패 |

```sql
-- PL/pgSQL 오류 처리 예제
CREATE OR REPLACE FUNCTION get_user_by_id(p_user_id INTEGER)
RETURNS users AS $$
DECLARE
    v_user users;
BEGIN
    SELECT * INTO STRICT v_user
    FROM users
    WHERE id = p_user_id;

    RETURN v_user;
EXCEPTION
    WHEN no_data_found THEN
        RAISE EXCEPTION '사용자 ID %를 찾을 수 없습니다', p_user_id
            USING ERRCODE = 'P0002';
    WHEN too_many_rows THEN
        RAISE EXCEPTION '사용자 ID %에 대해 여러 행이 반환되었습니다', p_user_id
            USING ERRCODE = 'P0003';
END;
$$ LANGUAGE plpgsql;

-- 사용자 정의 예외 발생
CREATE OR REPLACE FUNCTION validate_age(p_age INTEGER)
RETURNS VOID AS $$
BEGIN
    IF p_age < 0 OR p_age > 150 THEN
        RAISE EXCEPTION '잘못된 나이 값: %', p_age
            USING ERRCODE = 'P0001',
                  HINT = '나이는 0에서 150 사이여야 합니다';
    END IF;
END;
$$ LANGUAGE plpgsql;
```

---

### Class XX - 내부 오류 (Internal Error)

| SQLSTATE | 조건명 (Condition Name) | 설명 |
|----------|------------------------|------|
| `XX000` | `internal_error` | 내부 오류 |
| `XX001` | `data_corrupted` | 데이터 손상 |
| `XX002` | `index_corrupted` | 인덱스 손상 |

---

## 오류 코드 활용 가이드

### 1. PL/pgSQL에서 예외 처리

조건명을 사용하여 예외를 처리할 수 있습니다 (대소문자 구분 없음):

```sql
CREATE OR REPLACE FUNCTION safe_divide(a NUMERIC, b NUMERIC)
RETURNS NUMERIC AS $$
BEGIN
    RETURN a / b;
EXCEPTION
    WHEN division_by_zero THEN
        RETURN NULL;
    WHEN numeric_value_out_of_range THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

### 2. SQLSTATE 코드 직접 사용

조건명 대신 SQLSTATE 코드를 직접 사용할 수도 있습니다:

```sql
DO $$
BEGIN
    -- 작업 수행
EXCEPTION
    WHEN SQLSTATE '23505' THEN  -- unique_violation
        RAISE NOTICE '고유 제약 위반';
    WHEN SQLSTATE '23503' THEN  -- foreign_key_violation
        RAISE NOTICE '외래 키 위반';
END;
$$;
```

### 3. 사용자 정의 SQLSTATE 코드

사용자 정의 오류에 대해 자체 SQLSTATE 코드를 사용할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION custom_validation()
RETURNS VOID AS $$
BEGIN
    -- 사용자 정의 오류 발생
    RAISE EXCEPTION '사용자 정의 유효성 검사 실패'
        USING ERRCODE = 'A0001';  -- 사용자 정의 코드
END;
$$ LANGUAGE plpgsql;
```

### 4. 애플리케이션에서 오류 코드 처리

애플리케이션은 오류 메시지 텍스트가 아닌 오류 코드를 테스트해야 합니다:

```python
# Python (psycopg2) 예제
import psycopg2
from psycopg2 import errors

try:
    cursor.execute("INSERT INTO users (email) VALUES ('duplicate@email.com')")
except errors.UniqueViolation:  # SQLSTATE 23505
    print("이메일이 이미 존재합니다")
except errors.ForeignKeyViolation:  # SQLSTATE 23503
    print("참조하는 외래 키가 존재하지 않습니다")
except psycopg2.DatabaseError as e:
    print(f"데이터베이스 오류: {e.pgcode} - {e.pgerror}")
```

```java
// Java (JDBC) 예제
try {
    statement.executeUpdate("INSERT INTO users (email) VALUES ('duplicate@email.com')");
} catch (SQLException e) {
    String sqlState = e.getSQLState();
    switch (sqlState) {
        case "23505":  // unique_violation
            System.out.println("이메일이 이미 존재합니다");
            break;
        case "23503":  // foreign_key_violation
            System.out.println("참조하는 외래 키가 존재하지 않습니다");
            break;
        default:
            System.out.println("데이터베이스 오류: " + sqlState);
    }
}
```

---

## 오류 정보 조회

### GET DIAGNOSTICS 사용

PL/pgSQL에서 예외 발생 시 상세 정보를 조회할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION detailed_error_handling()
RETURNS TEXT AS $$
DECLARE
    v_sqlstate TEXT;
    v_message TEXT;
    v_detail TEXT;
    v_hint TEXT;
    v_context TEXT;
BEGIN
    -- 오류를 유발하는 작업
    INSERT INTO nonexistent_table VALUES (1);
    RETURN '성공';
EXCEPTION
    WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS
            v_sqlstate = RETURNED_SQLSTATE,
            v_message = MESSAGE_TEXT,
            v_detail = PG_EXCEPTION_DETAIL,
            v_hint = PG_EXCEPTION_HINT,
            v_context = PG_EXCEPTION_CONTEXT;

        RAISE NOTICE 'SQLSTATE: %', v_sqlstate;
        RAISE NOTICE '메시지: %', v_message;
        RAISE NOTICE '상세: %', v_detail;
        RAISE NOTICE '힌트: %', v_hint;
        RAISE NOTICE '컨텍스트: %', v_context;

        RETURN format('오류 발생 - SQLSTATE: %s, 메시지: %s', v_sqlstate, v_message);
END;
$$ LANGUAGE plpgsql;
```

---

## 주요 참고사항

1. 이식성: 오류 코드는 텍스트 오류 메시지보다 PostgreSQL 버전 간에 더 안정적입니다.

2. 지역화 독립성: 오류 코드는 서버의 언어 설정에 관계없이 동일하게 유지됩니다.

3. SQL 표준 준수: 많은 코드가 SQL 표준에 정의되어 있으며, 일부는 PostgreSQL 전용입니다.

4. PostgreSQL 전용 코드: `P`가 포함된 코드(예: `23P01`, `08P01`)는 PostgreSQL 전용입니다.

5. 오류 클래스 테스트: 특정 조건 대신 오류 클래스 전체를 테스트할 수 있습니다:
   ```sql
   EXCEPTION
       WHEN integrity_constraint_violation THEN  -- Class 23 전체
           -- 모든 무결성 제약 위반 처리
   ```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Appendix A. PostgreSQL Error Codes](https://www.postgresql.org/docs/current/errcodes-appendix.html)
- [SQL 표준 SQLSTATE 코드](https://en.wikipedia.org/wiki/SQLSTATE)
- [PL/pgSQL 예외 처리](https://www.postgresql.org/docs/current/plpgsql-control-structures.html#PLPGSQL-ERROR-TRAPPING)
