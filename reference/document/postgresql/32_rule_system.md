# Chapter 39: 규칙 시스템 (The Rule System)

## 목차

1. [규칙 시스템 개요](#1-규칙-시스템-개요)
2. [쿼리 트리 (The Query Tree)](#2-쿼리-트리-the-query-tree)
3. [뷰와 규칙 시스템](#3-뷰와-규칙-시스템)
4. [구체화된 뷰 (Materialized Views)](#4-구체화된-뷰-materialized-views)
5. [INSERT, UPDATE, DELETE 규칙](#5-insert-update-delete-규칙)
6. [규칙과 권한](#6-규칙과-권한)
7. [규칙과 명령 상태](#7-규칙과-명령-상태)
8. [규칙 vs 트리거](#8-규칙-vs-트리거)

---

## 1. 규칙 시스템 개요

PostgreSQL의 규칙 시스템(Rule System)은 쿼리 재작성 규칙 시스템(Query Rewrite Rule System) 입니다. 이는 다른 데이터베이스 시스템에서 일반적으로 사용하는 저장 프로시저나 트리거와는 근본적으로 다른 접근 방식입니다.

### 1.1 규칙 시스템의 작동 방식

규칙 시스템은 다음과 같은 방식으로 작동합니다:

1. 쿼리 수정: 규칙을 고려하여 쿼리를 수정합니다
2. 수정된 쿼리 전달: 수정된 쿼리를 쿼리 플래너에 전달하여 계획 및 실행합니다

### 1.2 규칙 시스템의 용도

규칙 시스템은 다음과 같은 용도로 사용됩니다:

- 쿼리 언어 프로시저: 복잡한 쿼리 변환
- 뷰 (Views): 가상 테이블 구현
- 버전 관리: 데이터 버전 처리
- 복잡한 쿼리 변환: 쿼리 최적화 및 재작성

### 1.3 핵심 특징

| 특징 | 설명 |
|------|------|
| 쿼리 수준 작동 | 행 수준이 아닌 쿼리 수준에서 작동 |
| 재작성 기반 | 쿼리를 수정하거나 추가 쿼리 생성 |
| 뷰 구현 | PostgreSQL 뷰의 기반 메커니즘 |
| 최적화 가능 | 플래너가 전체 정보를 활용하여 최적화 |

---

## 2. 쿼리 트리 (The Query Tree)

쿼리 트리는 PostgreSQL 규칙 시스템에서 사용하는 SQL 문의 내부 표현입니다. 파서(Parser)와 플래너(Planner) 사이에 위치하며, 파싱된 쿼리와 사용자 정의 재작성 규칙을 0개 이상의 쿼리 트리로 변환합니다.

### 2.1 쿼리 트리의 주요 구성 요소

#### 명령 유형 (Command Type)

쿼리 트리를 생성한 명령을 나타내는 단순 값입니다:

| 명령 유형 | 설명 |
|----------|------|
| `SELECT` | 데이터 조회 |
| `INSERT` | 데이터 삽입 |
| `UPDATE` | 데이터 갱신 |
| `DELETE` | 데이터 삭제 |

#### 범위 테이블 (Range Table)

쿼리에서 사용되는 릴레이션(테이블/뷰)의 목록입니다:

- `SELECT` 문에서는 `FROM` 키워드 뒤에 나열된 릴레이션
- 각 항목은 테이블/뷰와 쿼리 내 별칭을 식별
- 쿼리 트리 구조에서는 이름이 아닌 번호로 참조

#### 결과 릴레이션 (Result Relation)

쿼리 결과가 기록될 위치를 식별하는 범위 테이블의 인덱스입니다:

| 쿼리 유형 | 결과 릴레이션 |
|----------|--------------|
| `SELECT` | 결과 릴레이션 없음 |
| `INSERT`/`UPDATE`/`DELETE` | 수정되는 테이블 또는 뷰 |

#### 대상 목록 (Target List)

쿼리 결과를 정의하는 표현식 목록입니다:

| 쿼리 유형 | 대상 목록 내용 |
|----------|---------------|
| `SELECT` | `SELECT`와 `FROM` 키워드 사이의 표현식 |
| `INSERT` | `VALUES` 또는 `SELECT` 절의 표현식 |
| `UPDATE` | `SET column = expression` 부분의 표현식 |
| `DELETE` | CTID 항목(일반 테이블) 또는 전체 행 변수(뷰) |

#### 자격 조건 (Qualification)

`WHERE` 절에 해당하는 불리언 표현식으로, 작업 실행 여부를 결정합니다:

- 각 행에 대해 true/false로 평가
- 영향받는 행을 제어

#### 조인 트리 (Join Tree)

`FROM` 절의 구조를 나타냅니다:

- 조인 순서와 관계 표시
- `ON` 또는 `USING` 표현식의 제한 저장
- 최상위 `WHERE` 표현식을 자격 조건으로 포함

### 2.2 쿼리 트리 확인 방법

쿼리 트리는 다음 설정 매개변수를 사용하여 서버 로그에 표시할 수 있습니다:

```sql
-- 파싱된 쿼리 트리 출력
SET debug_print_parse = on;

-- 재작성된 쿼리 트리 출력
SET debug_print_rewritten = on;

-- 실행 계획 트리 출력
SET debug_print_plan = on;
```

규칙 액션은 `pg_rewrite` 시스템 카탈로그에 쿼리 트리로 저장됩니다.

---

## 3. 뷰와 규칙 시스템

PostgreSQL에서 뷰는 규칙 시스템을 사용하여 구현됩니다. 뷰는 본질적으로 `ON SELECT DO INSTEAD` 규칙(관례적으로 `_RETURN`이라는 이름)이 있는 빈 테이블입니다.

### 3.1 뷰 생성의 내부 동작

```sql
CREATE VIEW myview AS SELECT * FROM mytab;
```

이는 기능적으로 다음과 동등합니다:

```sql
CREATE TABLE myview (...);
CREATE RULE "_RETURN" AS ON SELECT TO myview DO INSTEAD
    SELECT * FROM mytab;
```

### 3.2 SELECT 규칙의 작동 방식 (How SELECT Rules Work)

#### 핵심 특성

- 모든 쿼리에 마지막 단계로 적용됨
- 새 쿼리 트리를 생성하는 대신 쿼리 트리를 직접 수정
- `INSTEAD`로 표시된 하나의 무조건적 `SELECT` 액션으로 제한

#### 예제: 뷰 쿼리 재작성

다음과 같은 뷰와 쿼리가 있다고 가정합니다:

```sql
-- 뷰 정의
CREATE VIEW shoelace AS
    SELECT s.sl_name, s.sl_avail, s.sl_color, s.sl_len, s.sl_unit,
           s.sl_len * u.un_fact AS sl_len_cm
    FROM shoelace_data s, unit u
    WHERE s.sl_unit = u.un_name;

-- 뷰 쿼리
SELECT * FROM shoelace;
```

규칙 시스템이 이 쿼리를 처리하면:

1. `shoelace` 뷰의 `_RETURN` 규칙을 찾음
2. 규칙의 액션 쿼리로 서브쿼리 범위 테이블 항목을 생성
3. 원래 뷰 참조를 이 서브쿼리로 대체

재작성된 쿼리는 다음과 같이 됩니다:

```sql
SELECT shoelace.sl_name, shoelace.sl_avail, shoelace.sl_color,
       shoelace.sl_len, shoelace.sl_unit, shoelace.sl_len_cm
FROM (SELECT s.sl_name, s.sl_avail, s.sl_color, s.sl_len, s.sl_unit,
             s.sl_len * u.un_fact AS sl_len_cm
      FROM shoelace_data s, unit u
      WHERE s.sl_unit = u.un_name) shoelace;
```

### 3.3 비-SELECT 문에서의 뷰 규칙 (View Rules in Non-SELECT Statements)

`INSERT`, `UPDATE`, `DELETE`, `MERGE` 명령의 경우:

- 쿼리 트리는 `SELECT` 트리와 거의 동일
- 결과 릴레이션이 결과가 저장될 위치를 가리킴
- 갱신/삭제되는 행을 식별하기 위한 특수 `CTID` (현재 튜플 ID) 항목 추가
- 실행기는 이 시스템 컬럼을 사용하여 원본 행을 찾음

### 3.4 뷰의 강력함 (The Power of Views)

규칙 시스템은 상당한 쿼리 최적화 이점을 제공합니다:

- 플래너가 테이블, 관계, 자격 조건에 대한 모든 정보를 단일 쿼리 트리에서 받음
- 다른 뷰를 참조하는 복잡한 뷰는 계획 전에 완전히 확장됨
- 플래너가 완전한 정보로 최적의 결정을 내릴 수 있음

```sql
-- 예제: shoe_ready 뷰에 대한 간단한 SELECT
SELECT * FROM shoe_ready;

-- 내부적으로 4개 테이블의 조인으로 확장되어
-- 플래너에게 모든 정보가 제공됨
```

### 3.5 뷰 업데이트 (Updating a View)

PostgreSQL은 세 가지 메커니즘을 통해 뷰 업데이트를 지원합니다 (평가 순서대로):

#### 1. INSTEAD 규칙 (가장 먼저 평가)

`INSERT`, `UPDATE`, `DELETE` 명령을 기본 테이블에 대한 작업으로 재작성하는 사용자 정의 규칙입니다.

```sql
-- 뷰에 대한 INSERT를 기본 테이블로 리다이렉트하는 규칙
CREATE RULE insert_to_myview AS ON INSERT TO myview
    DO INSTEAD INSERT INTO mytab VALUES (NEW.col1, NEW.col2);
```

#### 2. INSTEAD OF 트리거

- `INSERT`: 뷰가 결과 릴레이션으로 유지됨
- `UPDATE`/`DELETE`/`MERGE`: 뷰가 확장되고, 트리거에 이전 행 값을 제공하기 위해 `wholerow` 항목 추가

```sql
-- INSTEAD OF 트리거 예제
CREATE TRIGGER update_myview_trigger
    INSTEAD OF UPDATE ON myview
    FOR EACH ROW
    EXECUTE FUNCTION update_myview_func();
```

#### 3. 자동 업데이트 가능 뷰 (가장 나중에 평가)

단일 기본 릴레이션에서 선택하는 간단한 뷰는 자동으로 기본 테이블을 업데이트하도록 재작성될 수 있습니다.

중요: 규칙은 트리거보다 먼저 평가됩니다. 규칙이나 트리거가 존재하면 자동 재작성이 우회됩니다.

오류 처리: 이러한 메커니즘 중 어느 것도 적용되지 않으면 실행기가 뷰를 직접 업데이트할 수 없으므로 오류가 발생합니다.

---

## 4. 구체화된 뷰 (Materialized Views)

구체화된 뷰는 규칙 시스템을 사용하여 쿼리 결과를 테이블과 유사한 형태로 저장합니다. 가상적인 일반 뷰와 달리 데이터를 물리적으로 저장합니다.

### 4.1 테이블 및 뷰와의 차이점

```sql
-- 구체화된 뷰 생성
CREATE MATERIALIZED VIEW mymatview AS SELECT * FROM mytab;

-- 일반 테이블 생성 (비교용)
CREATE TABLE mymatview AS SELECT * FROM mytab;
```

#### 주요 차이점

| 특성 | 구체화된 뷰 | 일반 테이블 |
|------|------------|------------|
| 직접 업데이트 | 불가능 | 가능 |
| 쿼리 저장 | 저장됨 | 저장되지 않음 |
| 새로고침 | `REFRESH` 명령으로 가능 | 수동 갱신 필요 |

### 4.2 작동 방식

- PostgreSQL 시스템 카탈로그에서 구체화된 뷰는 테이블이나 뷰와 같은 릴레이션
- 쿼리 시 구체화된 뷰에서 직접 데이터 반환 (테이블처럼)
- 규칙은 뷰를 채우는 데만 사용되며, 접근에는 사용되지 않음

### 4.3 실용적인 예제: 판매 요약

```sql
-- 원본 테이블
CREATE TABLE invoice (
    invoice_no    integer        PRIMARY KEY,
    seller_no     integer,
    invoice_date  date,
    invoice_amt   numeric(13,2)
);

-- 구체화된 뷰 생성
CREATE MATERIALIZED VIEW sales_summary AS
  SELECT
      seller_no,
      invoice_date,
      sum(invoice_amt)::numeric(13,2) as sales_amt
    FROM invoice
    WHERE invoice_date < CURRENT_DATE
    GROUP BY seller_no, invoice_date;

-- 인덱스 생성 (구체화된 뷰의 장점)
CREATE UNIQUE INDEX sales_summary_seller
  ON sales_summary (seller_no, invoice_date);

-- 매일 밤 새로고침
REFRESH MATERIALIZED VIEW sales_summary;
```

### 4.4 사용 사례

| 사용 사례 | 설명 |
|----------|------|
| 과거 데이터 분석 | 현재 완전성 없이 과거 데이터 요약 |
| 원격 데이터 캐싱 | 외부 데이터 래퍼의 데이터에 대한 빠른 로컬 접근 |
| 인덱스 지원 | 일반 뷰와 달리 구체화된 뷰에 인덱스 생성 가능 |

### 4.5 성능 이점

구체화된 뷰와 인덱스를 사용하면 외부 데이터 래퍼를 통해 원격 데이터에 직접 접근하는 것에 비해 극적인 성능 향상을 얻을 수 있습니다 (예: 188ms vs 0.117ms).

---

## 5. INSERT, UPDATE, DELETE 규칙

`INSERT`, `UPDATE`, `DELETE`에 대한 규칙은 뷰 규칙과 크게 다릅니다.

### 5.1 뷰 규칙과의 주요 차이점

| 특성 | 뷰 규칙 | UPDATE 규칙 |
|------|--------|-------------|
| 액션 수 | 하나의 `SELECT` 액션 | 없음, 하나, 또는 여러 개 |
| 실행 방식 | `INSTEAD` 전용 | `INSTEAD` 또는 `ALSO` |
| 의사 릴레이션 | 없음 | `NEW`와 `OLD` 지원 |
| 규칙 자격 조건 | 없음 | 지원됨 |
| 쿼리 트리 처리 | 기존 트리 수정 | 새 쿼리 트리 생성 |

### 5.2 중요 주의사항

> 권장사항: 많은 작업에 대해 규칙 대신 트리거 사용을 권장 합니다.

규칙보다 트리거가 권장되는 이유:

- 트리거가 더 단순하고 예측 가능한 의미론을 가짐
- 휘발성 함수(volatile functions)가 있는 규칙은 예상치 못하게 여러 번 실행될 수 있음
- 규칙은 `WITH` 절이나 `UPDATE` 쿼리의 다중 할당 서브-`SELECT`와 같은 일부 구문을 지원하지 않음

### 5.3 UPDATE 규칙 구문

```sql
CREATE [ OR REPLACE ] RULE name AS ON event
    TO table [ WHERE condition ]
    DO [ ALSO | INSTEAD ] { NOTHING | command | ( command ; ... ) }
```

여기서 `event`는 `INSERT`, `UPDATE`, `DELETE` 중 하나입니다.

### 5.4 실행 순서

| 규칙 유형 | 실행 순서 |
|----------|----------|
| `ON INSERT` | 원래 쿼리가 규칙 액션 전에 실행 |
| `ON UPDATE`/`ON DELETE` | 규칙 액션이 원래 쿼리 전에 실행 |

이 순서는 액션이 수정되는 행을 볼 수 있도록 보장합니다.

### 5.5 일반적인 사용 사례

#### 뷰 보호 규칙

```sql
-- 뷰에 대한 INSERT를 차단하는 규칙
CREATE RULE shoe_ins_protect AS ON INSERT TO shoe
    DO INSTEAD NOTHING;

CREATE RULE shoe_upd_protect AS ON UPDATE TO shoe
    DO INSTEAD NOTHING;

CREATE RULE shoe_del_protect AS ON DELETE TO shoe
    DO INSTEAD NOTHING;
```

#### 실제 테이블로 작업 리다이렉트

```sql
-- shoelace 뷰에 대한 INSERT를 shoelace_data 테이블로 리다이렉트
CREATE RULE shoelace_ins AS ON INSERT TO shoelace
    DO INSTEAD
    INSERT INTO shoelace_data VALUES (
        NEW.sl_name,
        NEW.sl_avail,
        NEW.sl_color,
        NEW.sl_len,
        NEW.sl_unit
    );

-- UPDATE 규칙
CREATE RULE shoelace_upd AS ON UPDATE TO shoelace
    DO INSTEAD
    UPDATE shoelace_data
       SET sl_name = NEW.sl_name,
           sl_avail = NEW.sl_avail,
           sl_color = NEW.sl_color,
           sl_len = NEW.sl_len,
           sl_unit = NEW.sl_unit
     WHERE sl_name = OLD.sl_name;

-- DELETE 규칙
CREATE RULE shoelace_del AS ON DELETE TO shoelace
    DO INSTEAD
    DELETE FROM shoelace_data
     WHERE sl_name = OLD.sl_name;
```

#### 조건부 로깅

```sql
-- 재고량이 변경된 경우에만 로그 기록
CREATE TABLE shoelace_log (
    sl_name    text,
    sl_avail   integer,
    log_who    text,
    log_when   timestamp
);

CREATE RULE log_shoelace AS ON UPDATE TO shoelace_data
    WHERE NEW.sl_avail <> OLD.sl_avail
    DO INSERT INTO shoelace_log VALUES (
        NEW.sl_name,
        NEW.sl_avail,
        current_user,
        current_timestamp
    );
```

### 5.6 컬럼 참조

| 의사 릴레이션 | 설명 |
|--------------|------|
| `NEW` | 새 행의 컬럼 참조 (`INSERT`/`UPDATE`) |
| `OLD` | 이전 행의 컬럼 참조 (`UPDATE`/`DELETE`) |

- 일치하지 않는 `NEW` 참조는 `UPDATE`에서 `OLD`로, `INSERT`에서 NULL로 변환
- 일치하지 않는 `OLD` 참조는 NULL로 변환

### 5.7 복잡한 규칙 예제

```sql
-- 여러 테이블에 영향을 미치는 규칙
CREATE RULE delete_computer AS ON DELETE TO computer
    DO ALSO
    DELETE FROM software WHERE hostname = OLD.hostname;

-- 조건부 INSTEAD 규칙
CREATE RULE update_special AS ON UPDATE TO products
    WHERE OLD.category = 'special'
    DO INSTEAD
    UPDATE special_products
       SET name = NEW.name, price = NEW.price
     WHERE id = OLD.id;
```

---

## 6. 규칙과 권한

이 섹션은 PostgreSQL의 규칙 시스템이 접근 제어 및 보안과 어떻게 상호작용하는지 설명합니다. 규칙에 의해 쿼리가 재작성되면 원래 쿼리에 명시적으로 이름이 지정된 것 이외의 테이블/뷰에 접근할 수 있습니다.

### 6.1 규칙 소유권과 접근 제어

#### 핵심 개념

| 개념 | 설명 |
|------|------|
| 별도 소유자 없음 | 재작성 규칙은 자동으로 릴레이션(테이블/뷰) 소유자가 소유 |
| 권한 검사 | 보안 호출자 뷰의 `SELECT` 규칙을 제외하고, 규칙을 통해 접근되는 모든 릴레이션은 호출 사용자가 아닌 규칙 소유자의 권한 으로 검사됨 |
| 사용자 이점 | 사용자는 쿼리에 명시적으로 이름이 지정된 테이블/뷰에 대한 권한만 필요 |

### 6.2 실용적인 예제

```sql
-- 기본 테이블 생성
CREATE TABLE phone_data (person text, phone text, private boolean);

-- 뷰 생성 (private 번호는 숨김)
CREATE VIEW phone_number AS
    SELECT person, CASE WHEN NOT private THEN phone END AS phone
    FROM phone_data;

-- assistant 역할에 뷰 접근 권한 부여
GRANT SELECT ON phone_number TO assistant;
```

이 예제에서:
- 테이블 소유자만 `phone_data`에 직접 접근 가능
- `assistant`는 권한 부여를 통해 `phone_number` 뷰 쿼리 가능
- 규칙이 쿼리를 재작성하고, 권한 검사는 소유자의 권한 사용
- `assistant`는 `phone_data`에 직접 접근 불가

### 6.3 보안 고려사항

#### security_barrier 없는 취약점

표준 뷰는 함수 실행 순서 조작을 통해 데이터를 유출할 수 있습니다:

```sql
-- 안전하지 않은 뷰
CREATE VIEW phone_number AS
    SELECT person, phone FROM phone_data WHERE phone NOT LIKE '412%';

-- 공격: 사용자 정의 함수가 WHERE 절 전에 실행
CREATE FUNCTION tricky(text, text) RETURNS bool AS $$
BEGIN
    RAISE NOTICE '% => %', $1, $2;  -- 모든 데이터가 여기서 노출됨
    RETURN true;
END;
$$ LANGUAGE plpgsql COST 0.0000000000000000000001;

SELECT * FROM phone_number WHERE tricky(person, phone);
-- 모든 데이터가 NOTICE 메시지를 통해 노출됨
```

#### 해결책: security_barrier 속성

```sql
-- 보안 뷰
CREATE VIEW phone_number WITH (security_barrier) AS
    SELECT person, phone FROM phone_data WHERE phone NOT LIKE '412%';
```

이점:
- 함수/연산자가 보이지 않는 행에 접근하는 것을 방지
- 행 필터가 함수 호출 전에 실행되도록 보장

주의: 성능에 영향을 미칠 수 있음

#### LEAKPROOF 함수

- 쿼리 플래너는 leakproof 함수(부작용이 없는 함수)에 대해 유연성을 가짐
- 예: 동등 연산자, 단순 내장 함수
- Leakproof 함수는 보안을 손상시키지 않고 어느 시점에서든 안전하게 평가될 수 있음

### 6.4 중요한 제한사항

`security_barrier`를 사용하더라도 뷰는 다음에 대해 완전히 안전하지 않습니다:

- `EXPLAIN`을 통한 쿼리 계획 분석
- 타이밍 공격 (쿼리 실행 시간 측정)
- 데이터 분포에 대한 통계적 추론
- 은닉 채널 공격

매우 민감한 데이터의 경우 뷰에만 의존하기보다 접근 자체를 제한하는 것을 고려하세요.

---

## 7. 규칙과 명령 상태

PostgreSQL이 쿼리를 실행할 때 명령 상태 문자열(예: `INSERT 149592 1`)을 반환합니다. 규칙은 어떤 명령 상태가 반환되는지에 영향을 미칠 수 있습니다.

### 7.1 규칙이 명령 상태에 미치는 영향

#### 사례 1: 무조건적 INSTEAD 규칙이 없는 경우

- 원래 쿼리가 실행 되고 평소처럼 명령 상태 반환
- 예외: 조건부 `INSTEAD` 규칙이 있으면 부정된 자격 조건이 원래 쿼리에 추가되어 상태에 반영된 행 수가 줄어들 수 있음

#### 사례 2: 무조건적 INSTEAD 규칙이 있는 경우

- 원래 쿼리가 실행되지 않음
- 서버는 다음 조건을 만족하는 마지막 `INSTEAD` 규칙 의 명령 상태를 반환:
  - 원래 쿼리와 동일한 명령 유형 (`INSERT`, `UPDATE`, `DELETE`)
  - 규칙에 의해 실제로 삽입/실행됨
- 일치하는 규칙 쿼리가 추가되지 않으면, 상태는 원래 쿼리 유형과 행 수 및 OID 필드에 0 을 표시

### 7.2 예측 가능한 명령 상태를 위한 모범 사례

특정 `INSTEAD` 규칙이 명령 상태를 설정하도록 보장하려면:

> 활성 규칙들 사이에서 알파벳 순으로 마지막 규칙 이름을 부여 하여 마지막에 적용되도록 합니다.

```sql
-- 예: 'z_' 접두사를 사용하여 마지막에 실행되도록
CREATE RULE z_final_insert AS ON INSERT TO myview
    DO INSTEAD
    INSERT INTO mytable VALUES (NEW.col1, NEW.col2);
```

---

## 8. 규칙 vs 트리거

규칙과 트리거는 모두 데이터베이스에서 자동화된 동작을 구현하지만, 서로 다른 수준에서 작동합니다.

### 8.1 주요 차이점

| 특성 | 트리거 | 규칙 |
|------|--------|------|
| 실행 수준 | 영향받는 각 행마다 한 번 실행 | 쿼리 수준에서 작동 |
| 개념적 복잡도 | 더 단순하고 초보자에게 쉬움 | 더 복잡하고 학습 곡선이 있음 |
| 작동 방식 | 행별 트리거 함수 호출 | 쿼리 수정 또는 추가 쿼리 생성 |
| 효율성 (대량 작업) | 행 수에 비례하여 작업량 증가 | 행 수와 무관하게 하나의 쿼리 |

### 8.2 사용 시기

#### 트리거 사용이 적합한 경우

| 사용 사례 | 이유 |
|----------|------|
| 제약 조건 및 외래 키 | 규칙은 데이터가 조용히 버려지므로 제약 조건을 제대로 구현할 수 없음 |
| 복잡한 로직 | 명시적 오류 처리 및 유효성 검사가 필요할 때 |
| 단순성 | 로직이 간단할 때 |
| 뷰 업데이트 | `INSTEAD OF` 트리거가 규칙보다 쉬움 |

```sql
-- 트리거가 적합한 예: 데이터 유효성 검사
CREATE FUNCTION validate_order() RETURNS trigger AS $$
BEGIN
    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'Quantity must be positive';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_order
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION validate_order();
```

#### 규칙 사용이 적합한 경우

| 사용 사례 | 이유 |
|----------|------|
| 대량 작업 | 단일 문이 많은 행에 영향을 미칠 때, 하나의 추가 명령을 발행하는 규칙이 일반적으로 더 빠름 |
| 쿼리 수준 변환 | 전체 쿼리를 다른 형태로 변환해야 할 때 |

```sql
-- 규칙이 적합한 예: 로그 테이블로 모든 변경 복제
CREATE RULE log_changes AS ON UPDATE TO products
    DO ALSO
    INSERT INTO products_changelog
    SELECT 'UPDATE', current_timestamp, OLD.*, NEW.*;
```

### 8.3 성능 비교 예제

`computer`와 `software` 테이블 간의 캐스케이딩 삭제 시나리오:

#### 단일 행 삭제

두 접근 방식 모두 인덱스 스캔으로 비슷하게 수행됩니다.

```sql
-- 트리거 접근
DELETE FROM computer WHERE hostname = 'server1';
-- 트리거가 software 테이블에서 관련 행 삭제

-- 규칙 접근
DELETE FROM computer WHERE hostname = 'server1';
-- 규칙이 software 삭제 쿼리 추가
```

#### 대량 삭제 (예: 2000행)

| 접근 방식 | 동작 | 성능 |
|----------|------|------|
| 트리거 | 2000개의 개별 명령을 실행기를 통해 실행, 각각 인덱스 스캔 수행 | 느림 |
| 규칙 | 조인을 사용하는 하나의 최적화된 명령 생성, 영향받는 행 수와 무관 | 빠름 |

```sql
-- 규칙이 생성하는 최적화된 쿼리 예시
DELETE FROM software
 WHERE hostname IN (SELECT hostname FROM computer WHERE condition);
```

### 8.4 요약

| 고려사항 | 권장사항 |
|---------|---------|
| 제약 조건 필요 | 트리거 사용 |
| 대량 작업 속도 필요 | 규칙 사용 |
| 뷰 업데이트 | `INSTEAD OF` 트리거 사용 (규칙보다 쉬움) |
| 단순한 로직 | 트리거 사용 |
| 복잡한 쿼리 변환 | 규칙 사용 |

> 규칙은 그 액션이 잘못 한정된 대규모 조인으로 인해 쿼리 플래너가 실패하는 경우에만 트리거보다 현저히 느립니다.

---

## 요약

PostgreSQL의 규칙 시스템은 쿼리 재작성을 통해 강력한 기능을 제공합니다:

| 기능 | 설명 |
|------|------|
| 쿼리 재작성 | 파서와 플래너 사이에서 쿼리를 변환 |
| 뷰 구현 | 규칙 시스템의 핵심 사용 사례 |
| 구체화된 뷰 | 쿼리 결과를 물리적으로 저장 |
| 데이터 수정 규칙 | INSERT, UPDATE, DELETE에 대한 사용자 정의 동작 |
| 보안 | security_barrier를 통한 행 수준 보안 |
| 성능 최적화 | 대량 작업에서 트리거보다 효율적일 수 있음 |

규칙 시스템을 효과적으로 이해하고 활용하면 복잡한 데이터베이스 로직을 쿼리 수준에서 우아하게 처리할 수 있습니다. 그러나 대부분의 일반적인 사용 사례에서는 트리거가 더 단순하고 예측 가능한 선택입니다.

---

## 참고 자료

- [PostgreSQL 공식 문서 - The Rule System](https://www.postgresql.org/docs/current/rules.html)
- [PostgreSQL 공식 문서 - The Query Tree](https://www.postgresql.org/docs/current/querytree.html)
- [PostgreSQL 공식 문서 - Views and the Rule System](https://www.postgresql.org/docs/current/rules-views.html)
- [PostgreSQL 공식 문서 - Rules on INSERT, UPDATE, DELETE](https://www.postgresql.org/docs/current/rules-update.html)
- [PostgreSQL 공식 문서 - Rules vs Triggers](https://www.postgresql.org/docs/current/rules-triggers.html)
