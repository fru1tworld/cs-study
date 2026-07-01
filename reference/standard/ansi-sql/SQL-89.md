# SQL-89 (SQL-1989) ANSI SQL 표준 완벽 가이드

---

## 1. 개요

### 공식 명칭

| 항목 | 내용 |
|------|------|
| ANSI 표준 번호 | ANSI X3.135-1989 |
| ISO 표준 번호 | ISO 9075:1989 |
| 정식 명칭 | *Database Language SQL with Integrity Enhancement* |
| 비공식 명칭 | SQL-89, SQL-1989, SQL1 |
| 채택 연도 | 1989년 |
| 발행 기관 | ANSI (American National Standards Institute), ISO (International Organization for Standardization) |
| 전신 표준 | ANSI X3.135-1986 (SQL-86) |

SQL-89는 최초의 SQL 표준인 SQL-86의 개정판(revision)으로, 공식적으로는 완전히 새로운 표준이 아니라 SQL-86에 대한 부록(addendum) 성격이 강하다. 핵심적인 추가 사항은 무결성 강화 기능(Integrity Enhancement Feature, IEF)이며, 이로 인해 정식 명칭에 "with Integrity Enhancement"가 포함되었다.

### 표준 문서 구조

SQL-89 표준 문서는 다음과 같은 주요 섹션으로 구성된다:

- Part 1: Framework (프레임워크) — ISO/IEC 9075-1:1989
- Part 2: Foundation (기초) — ISO/IEC 9075-2:1989
- Embedded SQL 바인딩: COBOL, FORTRAN, Pascal, PL/I에 대한 호스트 언어 바인딩 명세

---

## 2. 역사적 배경

### SQL 표준화의 흐름

```
1970  ─── E.F. Codd, 관계형 모델 논문 발표
          "A Relational Model of Data for Large Shared Data Banks"

1974  ─── IBM System R 프로젝트에서 SEQUEL 개발 (Donald Chamberlin, Raymond Boyce)

1976  ─── SEQUEL → SQL로 명칭 변경 (상표권 문제)

1979  ─── Oracle (당시 Relational Software Inc.)가 최초 상용 SQL RDBMS 출시

1981  ─── IBM SQL/DS 출시

1982  ─── ANSI에서 SQL 표준화 작업 착수 (X3H2 위원회)

1983  ─── IBM DB2 출시

1986  ─── ANSI X3.135-1986 (SQL-86) 채택 — 최초의 공식 SQL 표준

1987  ─── ISO 9075:1987로 국제 표준 채택

1989  ─── ANSI X3.135-1989 (SQL-89) 채택 — 무결성 강화 기능 추가
          ISO 9075:1989로 국제 표준 채택

1992  ─── SQL-92 (SQL2) 채택 — 대규모 확장
```

### SQL-86에서 SQL-89로의 발전 과정

SQL-86의 한계:

SQL-86은 최초의 SQL 표준이었지만 여러 심각한 한계가 있었다:

1. 무결성 제약조건의 부재: SQL-86에는 `PRIMARY KEY`, `FOREIGN KEY`, `CHECK` 제약조건이 표준에 포함되지 않았다. `NOT NULL`만 지원되었으며, 데이터 무결성은 전적으로 애플리케이션 레벨에서 관리해야 했다.

2. 참조 무결성 미지원: 테이블 간의 관계를 표준 차원에서 정의할 방법이 없었다.

3. 임베디드 SQL의 불완전성: 호스트 언어 바인딩이 표준의 부록(informative annex)으로만 포함되어 규범적(normative) 지위가 없었다.

4. 벤더 간 호환성 문제: 각 DBMS 벤더가 자체적인 무결성 메커니즘을 구현하여 이식성이 극히 낮았다.

SQL-89 개발의 동기:

- 관계형 모델의 핵심인 무결성 제약조건을 표준에 포함시키려는 학계와 산업계의 요구
- IBM DB2, Oracle, Informix 등 주요 DBMS가 이미 자체적으로 무결성 기능을 구현하고 있었으나, 문법이 서로 달라 표준화 필요성 대두
- ANSI X3H2 위원회(현재 INCITS H2)가 주도하여 무결성 강화 사양을 추가
- ISO/IEC JTC 1/SC 21에서 국제 표준으로 동시 채택

---

## 3. 주요 변경사항 — Integrity Enhancement Feature (IEF) 상세

### IEF 개요

SQL-89의 핵심 추가 사항인 Integrity Enhancement Feature(무결성 강화 기능)은 다음 다섯 가지 핵심 기능을 포함한다:

| 기능 | 설명 | SQL-86 | SQL-89 |
|------|------|--------|--------|
| `NOT NULL` | 널 값 금지 | 지원 (제한적) | 공식 지원 |
| `DEFAULT` | 기본값 지정 | 미지원 | 신규 추가 |
| `CHECK` | 도메인 제약조건 | 미지원 | 신규 추가 |
| `PRIMARY KEY` | 기본 키 제약조건 | 미지원 | 신규 추가 |
| `UNIQUE` | 유일성 제약조건 | 미지원 | 신규 추가 |
| `FOREIGN KEY ... REFERENCES` | 참조 무결성 | 미지원 | 신규 추가 |

### 3.1 NOT NULL 제약조건

SQL-86에서도 `NOT NULL`이 존재했으나, SQL-89에서 보다 명확하게 정의되었다.

```sql
-- SQL-89: NOT NULL 제약조건
CREATE TABLE employees (
    emp_id      INTEGER     NOT NULL,
    emp_name    CHAR(50)    NOT NULL,
    dept_id     INTEGER     NOT NULL,
    hire_date   DATE        NOT NULL,
    salary      DECIMAL(10,2) NOT NULL,
    commission  DECIMAL(10,2)          -- NULL 허용 (기본값)
);
```

동작 규칙:
- `NOT NULL`이 지정된 열에 `NULL` 값을 삽입하거나 갱신하려 하면 오류가 발생한다.
- `NOT NULL`이 없으면 해당 열은 `NULL` 값을 허용한다 (기본 동작).

### 3.2 DEFAULT 절

SQL-89에서 처음으로 열에 기본값(default value)을 지정할 수 있게 되었다.

```sql
-- SQL-89: DEFAULT 절 사용 예시
CREATE TABLE orders (
    order_id     INTEGER       NOT NULL,
    order_date   DATE          DEFAULT CURRENT_DATE,
    status       CHAR(10)      DEFAULT 'PENDING',
    quantity     INTEGER       DEFAULT 1,
    total_amount DECIMAL(12,2) DEFAULT 0.00,
    notes        CHAR(200)     DEFAULT NULL
);

-- INSERT 시 기본값 활용
INSERT INTO orders (order_id, quantity, total_amount)
VALUES (1001, 5, 250.00);
-- order_date는 현재 날짜, status는 'PENDING', notes는 NULL로 설정됨

-- 명시적으로 DEFAULT 키워드 사용
INSERT INTO orders (order_id, order_date, status, quantity, total_amount, notes)
VALUES (1002, DEFAULT, DEFAULT, 10, 500.00, DEFAULT);
```

SQL-89에서 허용되는 DEFAULT 값의 종류:

| DEFAULT 값 | 설명 | 예시 |
|-------------|------|------|
| 리터럴 값 | 상수 값 지정 | `DEFAULT 0`, `DEFAULT 'N/A'` |
| `NULL` | NULL을 기본값으로 지정 | `DEFAULT NULL` |
| `USER` | 현재 사용자 이름 | `DEFAULT USER` |
| `CURRENT_DATE` | 현재 날짜 | `DEFAULT CURRENT_DATE` |
| `CURRENT_TIME` | 현재 시각 | `DEFAULT CURRENT_TIME` |
| `CURRENT_TIMESTAMP` | 현재 타임스탬프 | `DEFAULT CURRENT_TIMESTAMP` |

### 3.3 CHECK 제약조건

테이블의 각 행이 만족해야 하는 조건(predicate)을 지정할 수 있게 되었다.

```sql
-- SQL-89: CHECK 제약조건 예시
CREATE TABLE employees (
    emp_id      INTEGER       NOT NULL,
    emp_name    CHAR(50)      NOT NULL,
    age         INTEGER       CHECK (age >= 18 AND age <= 65),
    salary      DECIMAL(10,2) CHECK (salary > 0),
    dept_code   CHAR(4)       CHECK (dept_code IN ('SALE', 'ENGG', 'MGMT', 'ADMN')),
    hire_date   DATE          NOT NULL,
    term_date   DATE,

    -- 테이블 수준 CHECK 제약조건 (여러 열 참조 가능)
    CHECK (term_date IS NULL OR term_date > hire_date),
    CHECK (salary >= 1000.00)
);
```

열 수준(Column-level) vs 테이블 수준(Table-level) CHECK:

```sql
-- 열 수준 CHECK: 단일 열에만 적용
CREATE TABLE products (
    product_id    INTEGER       NOT NULL,
    product_name  CHAR(100)     NOT NULL,
    price         DECIMAL(8,2)  CHECK (price >= 0),        -- 열 수준
    discount      DECIMAL(5,2)  CHECK (discount >= 0),     -- 열 수준

    -- 테이블 수준 CHECK: 여러 열을 참조
    CHECK (discount <= price)
);
```

CHECK 제약조건의 규칙:
- 조건이 `FALSE`를 반환하면 해당 DML 문(INSERT, UPDATE)은 거부된다.
- 조건이 `TRUE` 또는 `UNKNOWN`(NULL 관련)을 반환하면 허용된다.
- 서브쿼리는 CHECK 절 내에서 허용되지 않았다 (SQL-89 기준).

### 3.4 PRIMARY KEY 제약조건

테이블의 기본 키(primary key)를 선언적으로 정의할 수 있게 되었다.

```sql
-- SQL-89: 단일 열 PRIMARY KEY (열 수준 정의)
CREATE TABLE departments (
    dept_id     INTEGER     NOT NULL PRIMARY KEY,
    dept_name   CHAR(50)    NOT NULL,
    location    CHAR(100)
);

-- SQL-89: 복합 PRIMARY KEY (테이블 수준 정의)
CREATE TABLE order_items (
    order_id    INTEGER     NOT NULL,
    item_seq    INTEGER     NOT NULL,
    product_id  INTEGER     NOT NULL,
    quantity    INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price  DECIMAL(8,2) NOT NULL,

    PRIMARY KEY (order_id, item_seq)
);
```

PRIMARY KEY 규칙:
- 하나의 테이블에 단 하나의 PRIMARY KEY만 허용된다.
- PRIMARY KEY로 지정된 열은 암묵적으로 `NOT NULL`이다.
- PRIMARY KEY는 자동으로 `UNIQUE` 제약을 포함한다.
- 중복 값 삽입 시도 시 오류가 발생한다.

### 3.5 UNIQUE 제약조건

기본 키 외에 후보 키(candidate key)를 정의할 수 있다.

```sql
-- SQL-89: UNIQUE 제약조건
CREATE TABLE employees (
    emp_id          INTEGER     NOT NULL PRIMARY KEY,
    social_security CHAR(11)    NOT NULL UNIQUE,      -- 열 수준 UNIQUE
    email           CHAR(100)   UNIQUE,               -- NULL 허용 + UNIQUE
    dept_id         INTEGER     NOT NULL,
    badge_number    INTEGER     NOT NULL,

    -- 테이블 수준 복합 UNIQUE
    UNIQUE (dept_id, badge_number)
);
```

UNIQUE vs PRIMARY KEY:

| 특성 | PRIMARY KEY | UNIQUE |
|------|-------------|--------|
| 테이블당 개수 | 1개만 가능 | 여러 개 가능 |
| NULL 허용 | 불가 (암묵적 NOT NULL) | 가능 (명시적 NOT NULL 없으면) |
| 용도 | 행의 주 식별자 | 후보 키, 대체 키 |

---

## 4. 무결성 제약조건 상세

### 제약조건 명명 (Named Constraints)

SQL-89에서는 `CONSTRAINT` 키워드를 사용하여 제약조건에 이름을 부여할 수 있다.

```sql
CREATE TABLE employees (
    emp_id      INTEGER     NOT NULL
        CONSTRAINT pk_employees PRIMARY KEY,

    emp_name    CHAR(50)    NOT NULL
        CONSTRAINT nn_emp_name CHECK (emp_name IS NOT NULL),

    email       CHAR(100)
        CONSTRAINT uq_emp_email UNIQUE,

    salary      DECIMAL(10,2)
        CONSTRAINT ck_salary CHECK (salary > 0 AND salary < 999999.99),

    dept_id     INTEGER     NOT NULL,

    CONSTRAINT fk_emp_dept
        FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
);
```

명명된 제약조건의 장점:
- 오류 메시지에서 어떤 제약조건이 위반되었는지 명확히 식별 가능
- 향후 제약조건 관리(삭제, 변경 등)에 유용 (SQL-92에서 `ALTER TABLE ... DROP CONSTRAINT` 도입)

### 제약조건의 검사 시점

SQL-89에서 모든 제약조건은 즉시 검사(immediate checking) 방식으로 동작한다:

```
INSERT 문 실행 → 제약조건 검사 → 위반 시 문 거부 (ROLLBACK)
UPDATE 문 실행 → 제약조건 검사 → 위반 시 문 거부 (ROLLBACK)
DELETE 문 실행 → 참조 무결성 검사 → 위반 시 문 거부 (ROLLBACK)
```

> 참고: 지연 검사(deferred checking)는 SQL-89에서는 지원되지 않으며, SQL-92에서 `SET CONSTRAINTS DEFERRED` 구문으로 도입되었다.

### 종합 예제: 완전한 스키마 정의

```sql
-- SQL-89 스타일의 완전한 데이터베이스 스키마

-- 부서 테이블
CREATE TABLE departments (
    dept_id     INTEGER       NOT NULL PRIMARY KEY,
    dept_name   CHAR(50)      NOT NULL UNIQUE,
    budget      DECIMAL(12,2) DEFAULT 0.00,
    created_on  DATE          DEFAULT CURRENT_DATE,

    CHECK (budget >= 0)
);

-- 직원 테이블
CREATE TABLE employees (
    emp_id      INTEGER       NOT NULL,
    emp_name    CHAR(50)      NOT NULL,
    dept_id     INTEGER       NOT NULL,
    manager_id  INTEGER,
    hire_date   DATE          NOT NULL DEFAULT CURRENT_DATE,
    salary      DECIMAL(10,2) NOT NULL DEFAULT 30000.00,
    status      CHAR(1)       NOT NULL DEFAULT 'A',

    CONSTRAINT pk_emp
        PRIMARY KEY (emp_id),

    CONSTRAINT fk_emp_dept
        FOREIGN KEY (dept_id) REFERENCES departments (dept_id),

    CONSTRAINT fk_emp_mgr
        FOREIGN KEY (manager_id) REFERENCES employees (emp_id),

    CONSTRAINT ck_emp_salary
        CHECK (salary >= 0),

    CONSTRAINT ck_emp_status
        CHECK (status IN ('A', 'I', 'T'))
);

-- 프로젝트 테이블
CREATE TABLE projects (
    project_id   INTEGER       NOT NULL PRIMARY KEY,
    project_name CHAR(100)     NOT NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE,
    dept_id      INTEGER       NOT NULL,

    FOREIGN KEY (dept_id) REFERENCES departments (dept_id),
    CHECK (end_date IS NULL OR end_date >= start_date)
);

-- 프로젝트 배정 테이블 (다대다 관계)
CREATE TABLE project_assignments (
    emp_id       INTEGER       NOT NULL,
    project_id   INTEGER       NOT NULL,
    role         CHAR(20)      NOT NULL DEFAULT 'MEMBER',
    assigned_on  DATE          NOT NULL DEFAULT CURRENT_DATE,
    hours_per_wk DECIMAL(4,1)  DEFAULT 40.0,

    PRIMARY KEY (emp_id, project_id),

    FOREIGN KEY (emp_id) REFERENCES employees (emp_id),
    FOREIGN KEY (project_id) REFERENCES projects (project_id),

    CHECK (role IN ('LEAD', 'MEMBER', 'ADVISOR')),
    CHECK (hours_per_wk > 0 AND hours_per_wk <= 60.0)
);
```

---

## 5. 참조 무결성 (Referential Integrity)

### FOREIGN KEY ... REFERENCES 구문

SQL-89에서 도입된 참조 무결성은 테이블 간의 관계를 선언적으로 정의한다.

```sql
-- 기본 구문
CREATE TABLE child_table (
    ...
    column_name  data_type,
    ...
    FOREIGN KEY (column_name)
        REFERENCES parent_table (parent_column)
);

-- 열 수준 참조 (단일 열)
CREATE TABLE orders (
    order_id    INTEGER NOT NULL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers (customer_id),
    order_date  DATE    NOT NULL DEFAULT CURRENT_DATE
);

-- 테이블 수준 참조 (복합 외래 키)
CREATE TABLE shipment_items (
    shipment_id INTEGER NOT NULL,
    order_id    INTEGER NOT NULL,
    item_seq    INTEGER NOT NULL,
    ship_qty    INTEGER NOT NULL,

    PRIMARY KEY (shipment_id, order_id, item_seq),

    FOREIGN KEY (order_id, item_seq)
        REFERENCES order_items (order_id, item_seq)
);
```

### 참조 동작 (Referential Actions)

SQL-89에서의 참조 동작은 매우 제한적이었다. SQL-89 원본 표준에서는 참조 동작(ON DELETE, ON UPDATE)에 대한 지원이 다음과 같았다:

```sql
-- SQL-89에서의 참조 무결성 기본 동작: RESTRICT (암묵적)
-- 부모 행 삭제 시: 자식 행이 존재하면 삭제 거부
-- 부모 키 변경 시: 자식 행이 존재하면 변경 거부

CREATE TABLE orders (
    order_id     INTEGER NOT NULL PRIMARY KEY,
    customer_id  INTEGER NOT NULL,

    -- 기본 동작: 참조되는 부모 행의 삭제/변경이 거부됨
    FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
);
```

SQL-89 표준의 일부 구현체에서는 제한적인 참조 동작을 지원했으며, 표준 문서의 후반부에서 다음과 같은 동작이 언급되었다:

```sql
-- SQL-89에서 언급된 참조 동작 (구현에 따라 차이)

-- CASCADE: 부모 삭제/변경 시 자식도 함께 삭제/변경
FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE

-- SET NULL: 부모 삭제/변경 시 자식의 외래 키를 NULL로 설정
FOREIGN KEY (manager_id) REFERENCES employees (emp_id)
    ON DELETE SET NULL
    ON UPDATE SET NULL

-- SET DEFAULT: 부모 삭제/변경 시 자식의 외래 키를 기본값으로 설정
FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
    ON DELETE SET DEFAULT
    ON UPDATE SET DEFAULT

-- RESTRICT (기본값): 자식 행이 존재하면 부모 행 삭제/변경 거부
FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
```

> 중요: `ON DELETE CASCADE`, `ON UPDATE CASCADE` 등의 참조 동작은 SQL-89에서 논의되었으나, 완전한 표준 지원은 SQL-92에서 공식화되었다. SQL-89에서의 기본 동작은 사실상 `RESTRICT`와 동일하다.

### 참조 무결성 규칙

```
규칙 1: 삽입 규칙 (Insert Rule)
  ─ 자식 테이블에 행을 삽입할 때, 외래 키 값은 반드시
    부모 테이블의 참조된 열에 존재하거나 NULL이어야 한다.

규칙 2: 삭제 규칙 (Delete Rule)
  ─ 부모 테이블에서 행을 삭제할 때, 해당 행을 참조하는
    자식 행이 존재하면 삭제가 거부된다 (기본 동작).

규칙 3: 갱신 규칙 (Update Rule)
  ─ 부모 테이블의 참조 키 값을 변경할 때, 해당 값을
    참조하는 자식 행이 존재하면 변경이 거부된다 (기본 동작).
```

```sql
-- 참조 무결성 동작 예시

-- 부모 테이블
CREATE TABLE customers (
    customer_id  INTEGER     NOT NULL PRIMARY KEY,
    customer_name CHAR(100)  NOT NULL
);

-- 자식 테이블
CREATE TABLE orders (
    order_id     INTEGER     NOT NULL PRIMARY KEY,
    customer_id  INTEGER     NOT NULL,
    order_date   DATE        NOT NULL,

    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);

-- 데이터 삽입
INSERT INTO customers VALUES (1, 'ACME Corp');
INSERT INTO customers VALUES (2, 'Global Inc');

INSERT INTO orders VALUES (101, 1, '1989-06-15');  -- 성공: customer_id=1 존재
INSERT INTO orders VALUES (102, 2, '1989-07-20');  -- 성공: customer_id=2 존재
INSERT INTO orders VALUES (103, 9, '1989-08-01');  -- 오류: customer_id=9 존재하지 않음

DELETE FROM customers WHERE customer_id = 1;
-- 오류: orders 테이블에서 customer_id=1을 참조하는 행이 존재

-- 올바른 삭제 순서
DELETE FROM orders WHERE customer_id = 1;     -- 먼저 자식 행 삭제
DELETE FROM customers WHERE customer_id = 1;  -- 그 다음 부모 행 삭제 (성공)
```

### 자기 참조 (Self-Referencing)

```sql
-- SQL-89: 자기 참조 외래 키 (계층 구조)
CREATE TABLE employees (
    emp_id      INTEGER     NOT NULL PRIMARY KEY,
    emp_name    CHAR(50)    NOT NULL,
    manager_id  INTEGER,    -- NULL 허용: 최상위 관리자는 관리자 없음

    FOREIGN KEY (manager_id) REFERENCES employees (emp_id)
);

-- 데이터 삽입
INSERT INTO employees VALUES (1, 'CEO Kim', NULL);        -- 최상위: manager 없음
INSERT INTO employees VALUES (2, 'VP Park', 1);           -- CEO에게 보고
INSERT INTO employees VALUES (3, 'Director Lee', 2);      -- VP에게 보고
INSERT INTO employees VALUES (4, 'Manager Choi', 3);      -- Director에게 보고
```

---

## 6. 임베디드 SQL (Embedded SQL)

### 개요

SQL-89의 중요한 변경 중 하나는 임베디드 SQL(Embedded SQL) 명세를 표준의 규범적(normative) 부분으로 격상시킨 것이다. SQL-86에서는 임베디드 SQL이 부록(informative annex)으로만 포함되어 있었다.

### 지원 호스트 언어

SQL-89에서 공식적으로 지원하는 호스트 언어 바인딩:

| 언어 | 상태 | 비고 |
|------|------|------|
| COBOL | 공식 지원 | 비즈니스 애플리케이션의 주력 언어 |
| FORTRAN | 공식 지원 | 과학/공학 분야 |
| Pascal | 공식 지원 | 교육용 및 시스템 프로그래밍 |
| PL/I | 공식 지원 | IBM 메인프레임 환경 |
| C | 비공식/확장 | SQL-89 원본에는 미포함, SQL-92에서 추가 |
| Ada | 비공식/확장 | SQL-92에서 공식 추가 |

### 임베디드 SQL 기본 구문

임베디드 SQL 문은 `EXEC SQL`로 시작하고 호스트 언어별 종결자로 끝난다.

#### COBOL에서의 임베디드 SQL

```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. EMP-QUERY.
       DATA DIVISION.
       WORKING-STORAGE SECTION.

      * 호스트 변수 선언
       EXEC SQL BEGIN DECLARE SECTION END-EXEC.
       01 WS-EMP-ID      PIC 9(6).
       01 WS-EMP-NAME    PIC X(50).
       01 WS-SALARY      PIC 9(8)V99.
       01 WS-DEPT-ID     PIC 9(4).
       EXEC SQL END DECLARE SECTION END-EXEC.

      * SQL 통신 영역
       EXEC SQL INCLUDE SQLCA END-EXEC.

       PROCEDURE DIVISION.

      * 단일 행 조회
           MOVE 1001 TO WS-EMP-ID.

           EXEC SQL
               SELECT EMP_NAME, SALARY, DEPT_ID
               INTO :WS-EMP-NAME, :WS-SALARY, :WS-DEPT-ID
               FROM EMPLOYEES
               WHERE EMP_ID = :WS-EMP-ID
           END-EXEC.

           IF SQLCODE = 0
               DISPLAY 'Employee: ' WS-EMP-NAME
               DISPLAY 'Salary:   ' WS-SALARY
           ELSE IF SQLCODE = 100
               DISPLAY 'Employee not found'
           ELSE
               DISPLAY 'SQL Error: ' SQLCODE
           END-IF.

      * 커서를 이용한 다중 행 조회
           EXEC SQL
               DECLARE EMP_CURSOR CURSOR FOR
               SELECT EMP_ID, EMP_NAME, SALARY
               FROM EMPLOYEES
               WHERE DEPT_ID = :WS-DEPT-ID
               ORDER BY EMP_NAME
           END-EXEC.

           EXEC SQL OPEN EMP_CURSOR END-EXEC.

           PERFORM UNTIL SQLCODE NOT = 0
               EXEC SQL
                   FETCH EMP_CURSOR
                   INTO :WS-EMP-ID, :WS-EMP-NAME, :WS-SALARY
               END-EXEC
               IF SQLCODE = 0
                   DISPLAY WS-EMP-ID ' ' WS-EMP-NAME ' ' WS-SALARY
               END-IF
           END-PERFORM.

           EXEC SQL CLOSE EMP_CURSOR END-EXEC.

           STOP RUN.
```

#### FORTRAN에서의 임베디드 SQL

```fortran
      PROGRAM EMPQUERY

C     호스트 변수 선언
      EXEC SQL BEGIN DECLARE SECTION
      INTEGER EMPID
      CHARACTER*50 EMPNAME
      REAL*8 SALARY
      INTEGER DEPTID
      EXEC SQL END DECLARE SECTION

C     SQL 통신 영역
      EXEC SQL INCLUDE SQLCA

C     변수 초기화
      EMPID = 1001

C     단일 행 조회
      EXEC SQL
     +    SELECT EMP_NAME, SALARY, DEPT_ID
     +    INTO :EMPNAME, :SALARY, :DEPTID
     +    FROM EMPLOYEES
     +    WHERE EMP_ID = :EMPID

      IF (SQLCODE .EQ. 0) THEN
          WRITE(*,*) 'Employee: ', EMPNAME
          WRITE(*,*) 'Salary:   ', SALARY
      ELSE IF (SQLCODE .EQ. 100) THEN
          WRITE(*,*) 'Employee not found'
      ELSE
          WRITE(*,*) 'SQL Error: ', SQLCODE
      END IF

C     데이터 삽입
      EMPID = 2001
      EMPNAME = 'NEW EMPLOYEE'
      SALARY = 45000.00
      DEPTID = 10

      EXEC SQL
     +    INSERT INTO EMPLOYEES (EMP_ID, EMP_NAME, SALARY, DEPT_ID)
     +    VALUES (:EMPID, :EMPNAME, :SALARY, :DEPTID)

      IF (SQLCODE .EQ. 0) THEN
          EXEC SQL COMMIT
          WRITE(*,*) 'Insert successful'
      ELSE
          EXEC SQL ROLLBACK
          WRITE(*,*) 'Insert failed: ', SQLCODE
      END IF

      STOP
      END
```

#### Pascal에서의 임베디드 SQL

```pascal
program EmployeeQuery;

(* 호스트 변수 선언 *)
EXEC SQL BEGIN DECLARE SECTION;
var
    empId:    integer;
    empName:  packed array[1..50] of char;
    salary:   real;
    deptId:   integer;
EXEC SQL END DECLARE SECTION;

(* SQL 통신 영역 *)
EXEC SQL INCLUDE SQLCA;

begin
    empId := 1001;

    (* 단일 행 조회 *)
    EXEC SQL
        SELECT EMP_NAME, SALARY, DEPT_ID
        INTO :empName, :salary, :deptId
        FROM EMPLOYEES
        WHERE EMP_ID = :empId;

    if SQLCODE = 0 then
    begin
        writeln('Employee: ', empName);
        writeln('Salary:   ', salary:10:2);
    end
    else if SQLCODE = 100 then
        writeln('Employee not found')
    else
        writeln('SQL Error: ', SQLCODE);

    (* 커서 선언 *)
    EXEC SQL
        DECLARE dept_cursor CURSOR FOR
        SELECT EMP_ID, EMP_NAME, SALARY
        FROM EMPLOYEES
        WHERE DEPT_ID = :deptId
        ORDER BY SALARY DESC;

    (* 커서 열기 *)
    EXEC SQL OPEN dept_cursor;

    (* 반복 fetch *)
    EXEC SQL FETCH dept_cursor INTO :empId, :empName, :salary;

    while SQLCODE = 0 do
    begin
        writeln(empId:6, ' ', empName, ' ', salary:10:2);
        EXEC SQL FETCH dept_cursor INTO :empId, :empName, :salary;
    end;

    (* 커서 닫기 *)
    EXEC SQL CLOSE dept_cursor;
end.
```

#### PL/I에서의 임베디드 SQL

```pli
EMP_QUERY: PROCEDURE OPTIONS(MAIN);

    /* 호스트 변수 선언 */
    EXEC SQL BEGIN DECLARE SECTION;
    DCL EMP_ID      FIXED BIN(31);
    DCL EMP_NAME    CHAR(50);
    DCL SALARY      FIXED DEC(10,2);
    DCL DEPT_ID     FIXED BIN(31);
    EXEC SQL END DECLARE SECTION;

    /* SQL 통신 영역 */
    EXEC SQL INCLUDE SQLCA;

    EMP_ID = 1001;

    EXEC SQL
        SELECT EMP_NAME, SALARY, DEPT_ID
        INTO :EMP_NAME, :SALARY, :DEPT_ID
        FROM EMPLOYEES
        WHERE EMP_ID = :EMP_ID;

    IF SQLCODE = 0 THEN
        PUT SKIP LIST('Employee: ' || EMP_NAME);
    ELSE IF SQLCODE = 100 THEN
        PUT SKIP LIST('Employee not found');
    ELSE
        PUT SKIP LIST('SQL Error: ', SQLCODE);

END EMP_QUERY;
```

### SQLCA (SQL Communication Area)

SQLCA는 SQL 문 실행 결과에 대한 정보를 호스트 프로그램에 전달하는 구조체이다.

```
SQLCA 주요 필드:
┌─────────────┬──────────────────────────────────────────┐
│ 필드        │ 설명                                     │
├─────────────┼──────────────────────────────────────────┤
│ SQLCODE     │ 실행 결과 코드                           │
│             │   = 0:  성공                             │
│             │   = 100: 데이터 없음 (NOT FOUND)         │
│             │   < 0:  오류 발생                        │
│             │   > 0:  경고 (100 제외)                  │
├─────────────┼──────────────────────────────────────────┤
│ SQLERRM     │ 오류 메시지 텍스트                       │
├─────────────┼──────────────────────────────────────────┤
│ SQLERRD     │ 진단 정보 배열                           │
│             │   SQLERRD(3): 처리된 행 수               │
├─────────────┼──────────────────────────────────────────┤
│ SQLWARN     │ 경고 플래그 배열                         │
│             │   SQLWARN(0): 경고 존재 여부             │
│             │   SQLWARN(1): 문자열 절단 발생           │
│             │   SQLWARN(2): NULL 값이 집계 함수에서 제거│
└─────────────┴──────────────────────────────────────────┘
```

### WHENEVER 문

오류 처리를 위한 선언적 구문:

```sql
-- WHENEVER 구문
EXEC SQL WHENEVER SQLERROR GOTO error_handler;
EXEC SQL WHENEVER NOT FOUND GOTO not_found_handler;
EXEC SQL WHENEVER SQLWARNING CONTINUE;

-- 사용 예시 (의사 코드)
EXEC SQL WHENEVER SQLERROR GOTO :ERR_ROUTINE;
EXEC SQL WHENEVER NOT FOUND GOTO :EOF_ROUTINE;

EXEC SQL OPEN emp_cursor;

FETCH_LOOP:
    EXEC SQL FETCH emp_cursor INTO :emp_id, :emp_name;
    -- 정상 처리 로직
    GOTO FETCH_LOOP;

EOF_ROUTINE:
    EXEC SQL CLOSE emp_cursor;
    GOTO DONE;

ERR_ROUTINE:
    EXEC SQL ROLLBACK;
    -- 오류 처리 로직

DONE:
    EXEC SQL COMMIT;
```

---

## 7. 데이터 타입

SQL-89에서 지원하는 데이터 타입은 SQL-86과 동일하며, 변경 사항은 없다.

### 수치형 (Numeric Types)

| 데이터 타입 | 설명 | 예시 |
|-------------|------|------|
| `INTEGER` (또는 `INT`) | 정수, 구현 정의 크기 | `emp_id INTEGER` |
| `SMALLINT` | 작은 정수 | `age SMALLINT` |
| `NUMERIC(p, s)` | 정밀 소수, 정확한 전체 자릿수 p, 소수 자릿수 s | `price NUMERIC(8,2)` |
| `DECIMAL(p, s)` (또는 `DEC`) | NUMERIC과 유사, 최소 p자리 보장 | `salary DECIMAL(10,2)` |
| `FLOAT(p)` | 부동소수점, 이진 정밀도 p | `measurement FLOAT(24)` |
| `REAL` | 단정밀도 부동소수점 | `temperature REAL` |
| `DOUBLE PRECISION` | 배정밀도 부동소수점 | `coordinate DOUBLE PRECISION` |

```sql
-- 수치형 사용 예시
CREATE TABLE financial_data (
    record_id   INTEGER         NOT NULL PRIMARY KEY,
    amount      DECIMAL(12,2)   NOT NULL,
    tax_rate    NUMERIC(5,4)    NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 1),
    quantity    SMALLINT        NOT NULL DEFAULT 1,
    unit_cost   FLOAT           NOT NULL,
    total       DOUBLE PRECISION
);
```

### 문자형 (Character Types)

| 데이터 타입 | 설명 | 예시 |
|-------------|------|------|
| `CHAR(n)` (또는 `CHARACTER(n)`) | 고정 길이 문자열 | `status CHAR(1)` |
| `CHAR` | `CHAR(1)`과 동일 | `flag CHAR` |

```sql
-- 문자형 사용 예시
CREATE TABLE customer_info (
    customer_id   INTEGER     NOT NULL PRIMARY KEY,
    first_name    CHAR(30)    NOT NULL,
    last_name     CHAR(30)    NOT NULL,
    middle_init   CHAR,                  -- CHAR(1)과 동일
    address       CHAR(100),
    city          CHAR(50),
    state_code    CHAR(2),
    zip_code      CHAR(10),
    country_code  CHAR(3)     NOT NULL DEFAULT 'USA'
);
```

> 참고: `VARCHAR` (가변 길이 문자열)은 SQL-89에서 공식적으로는 표준의 일부가 아니었다. 그러나 많은 구현체(Oracle, DB2 등)에서 이미 `VARCHAR`를 지원하고 있었고, SQL-92에서 `CHARACTER VARYING(n)` 및 `VARCHAR(n)`으로 공식 표준에 포함되었다.

### SQL-89에서 지원하지 않는 타입 (SQL-92에서 추가)

| 데이터 타입 | 추가된 표준 |
|-------------|-------------|
| `VARCHAR(n)` / `CHARACTER VARYING(n)` | SQL-92 |
| `DATE` | SQL-92 (공식) |
| `TIME` | SQL-92 |
| `TIMESTAMP` | SQL-92 |
| `INTERVAL` | SQL-92 |
| `BIT` | SQL-92 |
| `BIT VARYING` | SQL-92 |
| `BOOLEAN` | SQL-99 |
| `CLOB`, `BLOB` | SQL-99 |

> 참고: `DATE`와 관련하여, 앞선 예제에서 `DATE` 타입을 사용했는데, 이는 엄밀히 말하면 SQL-89 표준 자체에는 포함되지 않은 타입이다. 그러나 당시 대부분의 상용 DBMS(Oracle, DB2, Informix 등)에서 `DATE`를 구현 확장(implementation extension)으로 지원했으며, 사실상 표준처럼 사용되었다.

---

## 8. 카탈로그 관련 변경사항

### SQL-86의 카탈로그 상태

SQL-86에서는 시스템 카탈로그(데이터 사전)에 대한 표준이 전혀 없었다. 각 DBMS 벤더가 자체적인 시스템 테이블/뷰를 제공했다:

```
IBM DB2:       SYSIBM.SYSTABLES, SYSIBM.SYSCOLUMNS
Oracle:        USER_TABLES, USER_TAB_COLUMNS, DBA_TABLES
Informix:      systables, syscolumns
```

### SQL-89의 카탈로그 변경

SQL-89에서도 시스템 카탈로그에 대한 포괄적인 표준은 도입되지 않았다. 그러나 다음과 같은 최소한의 요구사항이 추가되었다:

1. INFORMATION_SCHEMA 개념의 맹아: SQL-89에서는 카탈로그에 대한 논의가 시작되었으나, 완전한 `INFORMATION_SCHEMA`는 SQL-92에서 도입되었다.

2. 무결성 제약조건 메타데이터: IEF의 도입으로 인해, 시스템이 PRIMARY KEY, FOREIGN KEY, CHECK 등의 제약조건 정보를 내부적으로 관리해야 했다. 그러나 이 정보에 접근하는 표준 방법은 정의되지 않았다.

3. 스키마(SCHEMA) 개념의 도입:

```sql
-- SQL-89: 스키마 정의
-- 스키마는 하나의 인가 식별자(authorization identifier)가 소유하는
-- 테이블, 뷰, 권한의 모음

CREATE SCHEMA AUTHORIZATION schema_owner
    -- 스키마 내 테이블 정의
    CREATE TABLE departments (
        dept_id   INTEGER NOT NULL PRIMARY KEY,
        dept_name CHAR(50) NOT NULL
    )

    CREATE TABLE employees (
        emp_id    INTEGER NOT NULL PRIMARY KEY,
        emp_name  CHAR(50) NOT NULL,
        dept_id   INTEGER NOT NULL,
        FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
    )

    -- 뷰 정의
    CREATE VIEW dept_summary AS
        SELECT d.dept_id, d.dept_name, COUNT(*) AS emp_count
        FROM departments d, employees e
        WHERE d.dept_id = e.dept_id
        GROUP BY d.dept_id, d.dept_name

    -- 권한 부여
    GRANT SELECT ON departments TO PUBLIC
    GRANT SELECT, INSERT, UPDATE ON employees TO user1
;
```

---

## 9. SQL-86과의 상세 비교

### 기능 비교표

| 기능 영역 | SQL-86 | SQL-89 | 비고 |
|-----------|--------|--------|------|
| DDL | | | |
| CREATE TABLE | 지원 | 지원 | 제약조건 추가 |
| CREATE VIEW | 지원 | 지원 | 변경 없음 |
| CREATE SCHEMA | 미지원 | 지원 | 신규 |
| DROP TABLE | 지원 | 지원 | 변경 없음 |
| DROP VIEW | 지원 | 지원 | 변경 없음 |
| ALTER TABLE | 미지원 | 미지원 | SQL-92에서 추가 |
| DML | | | |
| SELECT | 지원 | 지원 | 변경 없음 |
| INSERT | 지원 | 지원 | DEFAULT 키워드 추가 |
| UPDATE | 지원 | 지원 | 변경 없음 |
| DELETE | 지원 | 지원 | 변경 없음 |
| 제약조건 | | | |
| NOT NULL | 지원 (제한적) | 공식 지원 | 명확화 |
| DEFAULT | 미지원 | 지원 | IEF 신규 |
| CHECK | 미지원 | 지원 | IEF 신규 |
| PRIMARY KEY | 미지원 | 지원 | IEF 신규 |
| UNIQUE | 미지원 | 지원 | IEF 신규 |
| FOREIGN KEY | 미지원 | 지원 | IEF 신규 |
| 트랜잭션 | | | |
| COMMIT | 지원 | 지원 | 변경 없음 |
| ROLLBACK | 지원 | 지원 | 변경 없음 |
| SAVEPOINT | 미지원 | 미지원 | SQL-99에서 추가 |
| 권한 | | | |
| GRANT | 지원 | 지원 | 변경 없음 |
| REVOKE | 미지원 | 미지원 | SQL-92에서 추가 |
| 임베디드 SQL | 부록(비규범적) | 규범적 | 지위 격상 |
| 집합 연산 | | | |
| UNION | 지원 | 지원 | 변경 없음 |
| INTERSECT | 미지원 | 미지원 | SQL-92에서 추가 |
| EXCEPT | 미지원 | 미지원 | SQL-92에서 추가 |
| 조인 | | | |
| 내부 조인 (WHERE절) | 지원 | 지원 | 변경 없음 |
| JOIN ... ON | 미지원 | 미지원 | SQL-92에서 추가 |
| LEFT/RIGHT/FULL OUTER JOIN | 미지원 | 미지원 | SQL-92에서 추가 |
| 서브쿼리 | | | |
| 스칼라 서브쿼리 | 지원 | 지원 | 변경 없음 |
| EXISTS | 지원 | 지원 | 변경 없음 |
| IN (서브쿼리) | 지원 | 지원 | 변경 없음 |
| 기타 | | | |
| 카탈로그/INFORMATION_SCHEMA | 미지원 | 미지원 | SQL-92에서 추가 |
| 동적 SQL | 미지원 | 미지원 | SQL-92에서 추가 |
| CASE 표현식 | 미지원 | 미지원 | SQL-92에서 추가 |
| CAST | 미지원 | 미지원 | SQL-92에서 추가 |

### SQL-86 vs SQL-89: CREATE TABLE 비교

```sql
-- ===== SQL-86 스타일 =====
-- 무결성 제약조건 없음, 모든 무결성은 애플리케이션에서 관리
CREATE TABLE employees (
    emp_id      INTEGER NOT NULL,
    emp_name    CHAR(50) NOT NULL,
    dept_id     INTEGER NOT NULL,
    salary      DECIMAL(10,2)
);

-- 애플리케이션 레벨에서 중복 검사, 참조 검사 등을 수행해야 함
-- INSERT 전에 프로그래밍 로직으로 검증


-- ===== SQL-89 스타일 =====
-- 선언적 무결성 제약조건으로 데이터 품질 보장
CREATE TABLE employees (
    emp_id      INTEGER       NOT NULL PRIMARY KEY,
    emp_name    CHAR(50)      NOT NULL,
    dept_id     INTEGER       NOT NULL,
    salary      DECIMAL(10,2) DEFAULT 30000.00,

    CONSTRAINT fk_emp_dept
        FOREIGN KEY (dept_id) REFERENCES departments (dept_id),

    CONSTRAINT ck_salary
        CHECK (salary > 0)
);

-- DBMS가 자동으로 무결성을 보장
-- 잘못된 데이터 삽입 시도 시 자동으로 거부
```

---

## 10. 제약사항과 한계

### SQL-89의 주요 한계

#### 10.1 DDL의 한계

```sql
-- ALTER TABLE 미지원
-- SQL-89에서는 테이블 생성 후 구조 변경이 불가능
-- 다음과 같은 구문은 SQL-92까지 기다려야 했다:

-- (SQL-89에서 불가능)
ALTER TABLE employees ADD COLUMN phone CHAR(20);
ALTER TABLE employees DROP COLUMN middle_name;
ALTER TABLE employees ALTER COLUMN salary SET DEFAULT 50000;
ALTER TABLE employees ADD CONSTRAINT ck_new CHECK (age > 0);
ALTER TABLE employees DROP CONSTRAINT ck_old;

-- SQL-89에서의 우회 방법:
-- 1. 새 테이블 생성
-- 2. 데이터 복사
-- 3. 원래 테이블 삭제
-- 4. 새 테이블 이름 변경 (RENAME도 표준에 없음)
```

#### 10.2 조인의 한계

```sql
-- SQL-89: WHERE 절 조인만 가능 (구식 구문)
SELECT e.emp_name, d.dept_name
FROM employees e, departments d
WHERE e.dept_id = d.dept_id;

-- SQL-89에서는 다음이 불가능:
-- INNER JOIN ... ON
-- LEFT OUTER JOIN
-- RIGHT OUTER JOIN
-- FULL OUTER JOIN
-- CROSS JOIN (명시적)
-- NATURAL JOIN

-- 외부 조인이 필요한 경우, 벤더별 비표준 구문을 사용해야 했음:
-- Oracle:  WHERE e.dept_id = d.dept_id (+)
-- Sybase:  WHERE e.dept_id *= d.dept_id
```

#### 10.3 문자열 처리의 한계

```sql
-- SQL-89: 가변 길이 문자열(VARCHAR) 미지원
-- 모든 문자열은 고정 길이 CHAR
CREATE TABLE notes (
    note_id     INTEGER     NOT NULL PRIMARY KEY,
    note_text   CHAR(500)   -- 항상 500바이트 차지 (낭비)
);

-- 문자열 연결 연산자(||) 미지원
-- SUBSTRING, TRIM, UPPER, LOWER 등 문자열 함수 미표준
```

#### 10.4 날짜/시간 타입의 부재

```sql
-- SQL-89 표준에는 공식적인 날짜/시간 타입이 없음
-- DATE, TIME, TIMESTAMP는 SQL-92에서 공식 추가

-- 벤더별 비표준 구현에 의존해야 했음
-- Oracle: DATE (자체 구현)
-- DB2:    DATE, TIME, TIMESTAMP (자체 구현)
-- 이식성이 낮은 코드가 됨
```

#### 10.5 REVOKE 미지원

```sql
-- SQL-89: GRANT만 지원, REVOKE는 미지원
GRANT SELECT, INSERT ON employees TO user1;       -- 가능
GRANT ALL PRIVILEGES ON departments TO user2;      -- 가능
GRANT SELECT ON emp_view TO PUBLIC;                -- 가능

-- (SQL-89에서 불가능)
REVOKE SELECT ON employees FROM user1;             -- SQL-92에서 추가
```

#### 10.6 기타 주요 한계

```
미지원 기능 목록:
┌──────────────────────────────┬─────────────────────────────────┐
│ 미지원 기능                  │ 도입된 표준                     │
├──────────────────────────────┼─────────────────────────────────┤
│ 동적 SQL (PREPARE, EXECUTE)  │ SQL-92                          │
│ CASE 표현식                  │ SQL-92                          │
│ CAST 표현식                  │ SQL-92                          │
│ 스크롤 커서 (SCROLL CURSOR)  │ SQL-92                          │
│ 임시 테이블 (TEMPORARY TABLE)│ SQL-92                          │
│ 도메인 (CREATE DOMAIN)       │ SQL-92                          │
│ INFORMATION_SCHEMA           │ SQL-92                          │
│ 지연 제약조건 검사 (DEFERRED)│ SQL-92                          │
│ INTERSECT, EXCEPT            │ SQL-92                          │
│ 명시적 JOIN 구문             │ SQL-92                          │
│ 서브쿼리 in CHECK            │ SQL-92 (부분), SQL-99 (완전)    │
│ 트리거 (TRIGGER)             │ SQL-99                          │
│ 재귀 쿼리 (RECURSIVE)        │ SQL-99                          │
│ 사용자 정의 타입 (UDT)       │ SQL-99                          │
│ 저장 프로시저                │ SQL/PSM (SQL-96/99)             │
│ 윈도우 함수                  │ SQL:2003                        │
│ MERGE 문                     │ SQL:2003                        │
│ XML 지원                     │ SQL:2003 (SQL/XML)              │
│ JSON 지원                    │ SQL:2016                        │
└──────────────────────────────┴─────────────────────────────────┘
```

#### 10.7 적합성 수준 (Conformance Levels)

SQL-89는 두 가지 적합성 수준을 정의했다:

```
Level 1 (최소 적합성):
  - 기본 DDL, DML
  - NOT NULL
  - 임베디드 SQL (최소 1개 호스트 언어)

Level 2 (완전 적합성):
  - Level 1의 모든 기능
  - IEF (PRIMARY KEY, FOREIGN KEY, CHECK, UNIQUE, DEFAULT)
  - 모든 호스트 언어 바인딩
```

> 대부분의 상용 DBMS는 Level 2 적합성을 주장했으나, 실제로는 벤더별 확장이 표준을 초과하는 경우가 많았다.

---

## 11. 영향과 의의

### SQL-89의 역사적 의의

#### 11.1 선언적 무결성의 표준화

SQL-89 이전에는 데이터 무결성이 애플리케이션 코드에 의존했다:

```
SQL-86 시대 (무결성을 애플리케이션에서 관리):
┌─────────────────────────┐
│   Application Code      │
│ ┌─────────────────────┐ │
│ │ 중복 검사 로직      │ │
│ │ 참조 검사 로직      │ │
│ │ 범위 검사 로직      │ │
│ │ NULL 검사 로직      │ │
│ └─────────────────────┘ │
│           ↓              │
│      SQL INSERT          │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│     RDBMS (SQL-86)      │
│  단순 데이터 저장/검색  │
└─────────────────────────┘


SQL-89 이후 (무결성을 DBMS에서 관리):
┌─────────────────────────┐
│   Application Code      │
│      SQL INSERT          │
│  (비즈니스 로직에 집중)  │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│     RDBMS (SQL-89)      │
│ ┌─────────────────────┐ │
│ │ PRIMARY KEY 검사    │ │
│ │ FOREIGN KEY 검사    │ │
│ │ CHECK 조건 검사     │ │
│ │ UNIQUE 검사         │ │
│ │ NOT NULL 검사       │ │
│ │ DEFAULT 값 적용     │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

이 변화는 다음과 같은 이점을 가져왔다:

1. 데이터 품질 향상: 어떤 애플리케이션에서 접근하든 일관된 무결성 보장
2. 코드 중복 감소: 여러 애플리케이션이 동일한 데이터베이스에 접근할 때, 각각에서 무결성 로직을 구현할 필요 없음
3. E.F. Codd의 관계형 모델에의 충실: 관계형 모델은 도메인 제약조건과 참조 무결성을 핵심 요소로 정의

#### 11.2 SQL-92로의 교두보

SQL-89는 SQL-86과 SQL-92 사이의 중간 단계(stepping stone)로서 중요한 역할을 했다:

```
SQL-86 (1986)                SQL-89 (1989)               SQL-92 (1992)
  기본 SQL                     + 무결성(IEF)               대규모 확장
  ~45 페이지                   + 임베디드 SQL 격상          ~600+ 페이지
                                ~120 페이지

  핵심 기여:                   핵심 기여:                  핵심 기여:
  - SQL 문법 표준화           - 선언적 무결성              - JOIN 구문
  - SELECT/INSERT/            - PRIMARY KEY               - VARCHAR
    UPDATE/DELETE             - FOREIGN KEY               - DATE/TIME
  - 기본 DDL                  - CHECK/UNIQUE              - CASE/CAST
  - GRANT                     - DEFAULT                   - ALTER TABLE
                              - 임베디드 SQL 공식화        - INFORMATION_SCHEMA
                                                          - 동적 SQL
                                                          - 외부 조인
                                                          - REVOKE 등
```

#### 11.3 산업계에의 영향

SQL-89는 다음과 같은 산업적 영향을 미쳤다:

1. NIST(미국 국립표준기술연구소) FIPS 127-1: 미국 정부의 SQL 적합성 테스트 표준이 SQL-89 기반으로 수립되었다. 연방 정부 기관에 납품되는 모든 DBMS는 이 테스트를 통과해야 했다.

2. 벤더 구현 수렴: Oracle, IBM DB2, Informix, Sybase 등 주요 벤더들이 SQL-89 IEF를 구현하면서, 기본적인 제약조건 문법의 호환성이 향상되었다.

3. 교육과 학술: SQL-89의 무결성 제약조건은 데이터베이스 교과서의 핵심 내용이 되었고, 관계형 데이터베이스 교육의 표준이 되었다.

4. 설계 방법론의 변화: ER(Entity-Relationship) 모델링에서 물리적 스키마로의 변환 시, 관계를 FOREIGN KEY로 직접 매핑할 수 있게 되어 설계와 구현 간의 간극이 줄어들었다.

### SQL 표준 진화의 전체 맥락에서의 SQL-89

```
┌────────────────────────────────────────────────────────────────────────┐
│                    SQL 표준의 진화 (전체 흐름)                        │
├──────────┬─────────────────────────────────────────────────────────────┤
│ SQL-86   │ 최초 표준. 기본 CRUD, DDL, GRANT                          │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL-89   │ ★ 무결성 강화 (IEF): PK, FK, CHECK, UNIQUE, DEFAULT      │
│          │ ★ 임베디드 SQL 공식화                                      │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL-92   │ 대규모 확장: JOIN 구문, VARCHAR, 날짜/시간, CASE/CAST,    │
│          │ ALTER TABLE, INFORMATION_SCHEMA, 동적 SQL, 외부 조인 등    │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:1999 │ 객체 관계형: UDT, 트리거, 재귀 쿼리, BOOLEAN, SAVEPOINT,  │
│          │ 정규 표현식, OLAP 기초                                     │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2003 │ 윈도우 함수, MERGE, XML, 시퀀스(SEQUENCE), 자동 생성 열   │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2006 │ SQL/XML 확장, XQuery 통합                                 │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2008 │ TRUNCATE TABLE, FETCH FIRST, 향상된 MERGE                 │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2011 │ 시간 데이터베이스(Temporal DB), 기간(PERIOD)              │
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2016 │ JSON 지원, 행 패턴 인식(MATCH_RECOGNIZE), 다형 테이블 함수│
├──────────┼─────────────────────────────────────────────────────────────┤
│ SQL:2023 │ JSON 확장, 속성 그래프 쿼리(SQL/PGQ), ANY VALUE 등       │
└──────────┴─────────────────────────────────────────────────────────────┘
```

### 결론

SQL-89(ANSI X3.135-1989 / ISO 9075:1989)는 규모 면에서는 SQL-86에 대한 비교적 작은 개정이었으나, 그 질적 의미는 매우 크다. Integrity Enhancement Feature(IEF)의 도입을 통해 관계형 데이터베이스의 본질적 특성인 선언적 무결성이 국제 표준으로 확립되었고, 이는 이후 모든 SQL 표준과 상용 RDBMS의 근간이 되었다. 오늘날 `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`, `UNIQUE`, `DEFAULT`는 SQL을 배우는 모든 사람이 가장 먼저 접하는 기본 개념이며, 이 모든 것의 표준화된 출발점이 바로 SQL-89이다.

---

*본 문서는 SQL-89 (ANSI X3.135-1989 / ISO 9075:1989) 표준에 대한 포괄적 정리이다.*
