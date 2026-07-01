# ANSI SQL-86 (SQL-87) - 최초의 SQL 표준

## 목차

1. [개요](#1-개요)
2. [공식 명칭 및 표준 번호](#2-공식-명칭-및-표준-번호)
3. [역사적 배경과 동기](#3-역사적-배경과-동기)
4. [표준화 과정 및 발행 일자](#4-표준화-과정-및-발행-일자)
5. [표준의 구조](#5-표준의-구조)
6. [SQL-86에서 정의된 데이터 타입](#6-sql-86에서-정의된-데이터-타입)
7. [SQL 문(Statements) 상세](#7-sql-문statements-상세)
8. [스키마 정의 언어 (DDL)](#8-스키마-정의-언어-ddl)
9. [데이터 조작 언어 (DML)](#9-데이터-조작-언어-dml)
10. [무결성 제약 조건](#10-무결성-제약-조건)
11. [뷰(View)](#11-뷰view)
12. [권한 관리](#12-권한-관리)
13. [임베디드 SQL](#13-임베디드-sql)
14. [모듈 언어](#14-모듈-언어)
15. [표현식 및 조건](#15-표현식-및-조건)
16. [커서(Cursor)](#16-커서cursor)
17. [트랜잭션 관리](#17-트랜잭션-관리)
18. [두 가지 수준(Level)의 적합성](#18-두-가지-수준level의-적합성)
19. [제한 사항 및 알려진 문제점](#19-제한-사항-및-알려진-문제점)
20. [선행 작업과의 관계](#20-선행-작업과의-관계)
21. [영향과 채택](#21-영향과-채택)
22. [SQL-86과 후속 표준의 비교](#22-sql-86과-후속-표준의-비교)
23. [참고 자료](#23-참고-자료)

---

## 1. 개요

SQL-86은 구조화 질의 언어(Structured Query Language)에 대한 최초의 공식 표준이다. 이 표준은 관계형 데이터베이스 관리 시스템(RDBMS)에서 데이터를 정의하고 조작하기 위한 언어의 구문(syntax)과 의미(semantics)를 최초로 공식 규정한 역사적인 문서이다.

SQL-86이라는 명칭은 ANSI에서 1986년에 채택한 것에서 유래하며, ISO에서 1987년에 채택했기 때문에 SQL-87이라고도 불린다. 두 명칭은 동일한 표준을 가리킨다. 이 표준은 이후 모든 SQL 표준의 기초가 되었으며, 40년이 지난 현재에도 SQL의 핵심 구문과 개념은 SQL-86에서 최초로 공식화된 것들이다.

---

## 2. 공식 명칭 및 표준 번호

| 구분 | 명칭/번호 |
|------|-----------|
| ANSI 표준 번호 | ANSI X3.135-1986 |
| ANSI 공식 명칭 | Database Language SQL |
| ISO 표준 번호 | ISO 9075:1987 |
| ISO 공식 명칭 | Information technology — Database languages — SQL |
| ANSI 채택 연도 | 1986년 |
| ISO 채택 연도 | 1987년 |
| FIPS 표준 | FIPS PUB 127 (Federal Information Processing Standard) |
| 통칭 | SQL-86, SQL-87, SQL1 |

### 관련 표준 기관

- ANSI (American National Standards Institute): 미국 국가표준기관
- ISO (International Organization for Standardization): 국제표준화기구
- IEC (International Electrotechnical Commission): 국제전기기술위원회
- X3 (현 INCITS): ANSI 산하 정보기술 표준 위원회
- X3H2: SQL 표준을 담당한 소위원회 (현재 INCITS DM32.2)
- ISO/IEC JTC 1/SC 21: ISO 측 담당 소위원회

---

## 3. 역사적 배경과 동기

### 3.1 SQL의 기원

SQL의 역사는 1970년대 초 IBM의 연구에서 시작된다.

| 연도 | 사건 |
|------|------|
| 1970 | Edgar F. Codd가 IBM San Jose Research Laboratory에서 관계형 모델 논문 *"A Relational Model of Data for Large Shared Data Banks"* 발표. 데이터를 수학적 관계(relation), 즉 테이블 형태로 표현하고 관계 대수(relational algebra) 및 관계 해석(relational calculus)을 통해 데이터를 조작할 수 있음을 보여줌 |
| 1973-1974 | IBM의 Donald D. Chamberlin과 Raymond F. Boyce가 Codd의 관계형 모델을 구현하기 위한 언어로 SEQUEL(Structured English Query Language) 설계. 비전문가도 영어와 유사한 구문으로 데이터베이스를 질의할 수 있도록 하는 것이 목표 |
| 1974-1979 | IBM이 SEQUEL을 기반으로 System R 프로토타입 개발. 관계형 데이터베이스의 실현 가능성을 입증한 최초의 시스템이며, 트랜잭션 관리, 잠금(locking), 쿼리 최적화 등 현대 RDBMS의 핵심 개념이 이 프로젝트에서 탄생 |
| 1976 | SEQUEL/2 개발. 이후 영국의 항공기 회사 Hawker Siddeley가 "SEQUEL"이라는 상표를 보유하고 있어 명칭을 SQL로 변경 |
| 1977 | Larry Ellison이 SQL을 기반으로 한 소프트웨어 개발 시작 (후에 Oracle이 됨) |
| 1979 | Relational Software, Inc. (후에 Oracle Corporation으로 개명)이 최초의 상용 SQL 기반 RDBMS인 Oracle V2 출시. IBM보다 먼저 SQL 기반 제품을 시장에 출시 |
| 1981 | IBM이 SQL/DS 출시 |
| 1983 | IBM이 DB2 출시 |

### 3.2 표준화의 동기

1980년대 초, 여러 벤더가 각자의 SQL 구현을 제공하면서 다음과 같은 문제가 발생했다:

1. 상호 운용성 부재: Oracle, Ingres, Informix, Sybase 등 각 벤더의 SQL 방언(dialect)이 달라 애플리케이션 이식이 어려웠다
2. 벤더 종속(Vendor Lock-in): 한 DBMS에서 작성한 SQL이 다른 DBMS에서 동작하지 않았다
3. 혼란: SQL의 "올바른" 문법이 무엇인지 합의가 없었다
4. 정부 조달 요구: 미국 연방 정부가 표준화된 데이터베이스 언어를 요구했다
5. 시장 성장: 관계형 데이터베이스 시장의 급속한 성장에 따라 공통 기반이 필요했다

### 3.3 표준화 추진 주체

- ANSI X3H2 위원회 (Database Committee): 1982년부터 SQL 표준화 작업 시작
- 주요 참여 기업: IBM, Oracle, Digital Equipment Corporation (DEC), Tandem Computers 등
- IBM의 SQL/DS와 DB2의 SQL이 표준의 주요 기반이 되었다
- 관계형 데이터베이스 벤더 연합(Relational Database Vendor Group)도 참여
- 표준화 과정에서 가장 큰 논점은 기존 구현의 호환성 유지와 이론적 완결성 사이의 균형이었다

---

## 4. 표준화 과정 및 발행 일자

| 시점 | 내용 |
|------|------|
| 1982년 | ANSI X3H2 위원회에서 SQL 표준화 작업 공식 착수 |
| 1983년 | 초기 초안(draft) 작성 |
| 1984년 | 여러 차례의 검토 및 수정 |
| 1985년 | 공개 검토(public review) 진행 |
| 1986년 10월 | ANSI에서 ANSI X3.135-1986으로 채택 |
| 1987년 | ISO에서 ISO 9075:1987로 채택 (ANSI 표준과 기술적으로 동일) |
| 1987년 | 미국 연방 정부에서 FIPS PUB 127로 채택 |

결과적으로 SQL-86은 당시 주요 벤더들이 이미 구현한 기능들의 최소 공통 부분집합(least common denominator)에 가까운 표준이 되었다.

---

## 5. 표준의 구조

SQL-86 표준 문서는 단일 문서로 구성되며, 대략 약 100페이지 정도의 분량이다. 주요 구성 절(section)은 다음과 같다:

| 섹션 | 내용 |
|------|------|
| 1. 범위(Scope) | 표준이 다루는 범위와 목적 정의 |
| 2. 참조 표준(References) | 관련 표준 문서 참조 |
| 3. 정의, 표기법, 규약(Definitions, notations, and conventions) | 표준에서 사용하는 용어의 정의 |
| 4. 개념(Concepts) | 관계형 모델의 기본 개념 - 테이블, 열, 행, 데이터 타입, NULL, 스키마, 뷰, 권한 등 |
| 5. 공통 요소(Common elements) | 데이터 타입, 리터럴, 값 표현식, 탐색 조건 등 |
| 6. 스키마 정의 언어(Schema definition language) | CREATE TABLE, CREATE VIEW, GRANT 등 |
| 7. 모듈 언어(Module language) | 모듈 기반의 SQL 프로시저 정의 |
| 8. 데이터 조작 언어(Data manipulation language) | SELECT, INSERT, UPDATE, DELETE 등 |
| 9. 임베디드 SQL(Embedded SQL) | 호스트 프로그래밍 언어에 SQL을 내장하는 방법 |
| 10. 레벨 정의(Leveling) | Level 1과 Level 2 적합성 정의 |

표준은 BNF(Backus-Naur Form) 표기법을 사용하여 SQL 문법을 정의한다.

### 두 가지 바인딩 형태

SQL-86은 SQL을 사용하는 두 가지 방식을 정의하였다:

1. 모듈 언어(Module Language): SQL 문을 별도의 모듈에 정의하고, 호스트 언어의 프로시저 호출을 통해 실행
2. 임베디드 SQL(Embedded SQL): COBOL, Fortran, Pascal, PL/I 등의 호스트 프로그래밍 언어 소스 코드 안에 SQL 문을 직접 삽입

> 참고: SQL-86에는 대화형 SQL(Interactive SQL, 직접 실행 SQL / Direct Invocation)에 대한 공식 규정이 포함되어 있지 않다. 부록(Annex)에서 비공식적으로 언급만 되었으며, 이는 SQL-92에서야 공식 추가되었다. 그러나 모든 벤더가 대화형 SQL을 실무적으로 지원했다.

---

## 6. SQL-86에서 정의된 데이터 타입

SQL-86에서 정의한 데이터 타입은 매우 제한적이다.

### 6.1 문자열 타입

| 데이터 타입 | 설명 |
|-------------|------|
| `CHARACTER(n)` | 고정 길이 문자열. `n`은 길이를 지정. 항상 정확히 n자를 차지하며, 부족한 부분은 공백으로 채워짐 |
| `CHAR(n)` | `CHARACTER(n)`의 약어 |

```sql
-- 예시
name CHARACTER(30)
code CHAR(10)
```

- 가변 길이 문자열(`VARCHAR`)은 SQL-86에 포함되지 않았다. 이는 SQL-89에서 추가되었다.
- 문자열 비교 시 후행 공백(trailing spaces)의 처리가 구현에 따라 달라질 수 있었다.

### 6.2 숫자 타입 (정확한 수치형, Exact Numeric)

| 데이터 타입 | 설명 |
|-------------|------|
| `NUMERIC(p, s)` | 정밀도(precision) p, 스케일(scale) s를 갖는 정확한 수치. p는 전체 자릿수, s는 소수점 이하 자릿수. 정확히 정밀도 p를 가져야 함 |
| `DECIMAL(p, s)` | `NUMERIC`과 유사하나 구현이 지정된 정밀도 이상을 허용할 수 있음 (최소 정밀도 p를 보장) |
| `DEC(p, s)` | `DECIMAL`의 약어 |
| `INTEGER` | 구현 정의 정밀도의 정수. 스케일은 0 |
| `INT` | `INTEGER`의 약어 |
| `SMALLINT` | `INTEGER`보다 작거나 같은 정밀도의 정수 |

```sql
-- 예시
price NUMERIC(10, 2)
quantity INTEGER
total DECIMAL(12, 4)
age SMALLINT
```

- `INTEGER`와 `SMALLINT`의 구체적인 범위는 구현에 의존한다(구현 정의, implementation-defined).

### 6.3 숫자 타입 (근사 수치형, Approximate Numeric)

| 데이터 타입 | 설명 |
|-------------|------|
| `FLOAT(p)` | 이진 정밀도 p를 갖는 부동소수점 |
| `REAL` | 구현 정의 정밀도의 단정도 부동소수점 |
| `DOUBLE PRECISION` | 구현 정의 정밀도의 배정도 부동소수점 (REAL보다 정밀도가 높음) |

```sql
-- 예시
rate FLOAT(24)
measurement REAL
calculation DOUBLE PRECISION
```

### 6.4 SQL-86에서 지원하지 않는 데이터 타입

| 부재 타입 | 도입 시기 |
|-----------|-----------|
| `VARCHAR` / `CHARACTER VARYING` (가변 길이 문자열) | SQL-89 |
| `DATE` | SQL-92 |
| `TIME` | SQL-92 |
| `TIMESTAMP` | SQL-92 |
| `INTERVAL` | SQL-92 |
| `BIT` / `BIT VARYING` | SQL-92 |
| `BOOLEAN` | SQL:1999 |
| `BLOB`, `CLOB` (대용량 객체) | SQL:1999 |
| `ARRAY`, `MULTISET` | SQL:1999 / SQL:2003 |
| `XML` | SQL:2003 |
| `JSON` | SQL:2016 |
| 사용자 정의 타입 | SQL:1999 |

> 주목할 점: SQL-86에는 날짜/시간 관련 데이터 타입이 전혀 없다. 날짜와 시간은 문자열(`CHAR`)로 저장해야 했으며, 이는 당시 표준의 가장 심각한 한계 중 하나로 지적되었다.

---

## 7. SQL 문(Statements) 상세

SQL-86에서 정의된 SQL 문은 크게 세 범주로 나뉜다.

### 7.1 스키마 정의 문 (Schema Definition Statements)

| 문(Statement) | 용도 |
|---------------|------|
| `CREATE SCHEMA` | 스키마 생성 (테이블, 뷰, 권한을 묶는 단위) |
| `CREATE TABLE` | 기본 테이블 생성 |
| `CREATE VIEW` | 뷰 생성 |
| `GRANT` | 권한 부여 |

### 7.2 데이터 조작 문 (Data Manipulation Statements)

| 문(Statement) | 용도 |
|---------------|------|
| `SELECT` (단일 행) | 단일 행 조회 (`SELECT ... INTO`) |
| `INSERT` | 데이터 삽입 |
| `UPDATE` (검색) | 조건 기반 데이터 갱신 (searched update) |
| `UPDATE` (위치 지정) | 커서 위치 기반 데이터 갱신 (positioned update) |
| `DELETE` (검색) | 조건 기반 데이터 삭제 (searched delete) |
| `DELETE` (위치 지정) | 커서 위치 기반 데이터 삭제 (positioned delete) |
| `DECLARE CURSOR` | 커서 선언 |
| `OPEN` | 커서 열기 |
| `FETCH` | 커서에서 행 가져오기 |
| `CLOSE` | 커서 닫기 |

### 7.3 트랜잭션 제어 문

| 문(Statement) | 용도 |
|---------------|------|
| `COMMIT WORK` | 트랜잭션 커밋 |
| `ROLLBACK WORK` | 트랜잭션 롤백 |

### 7.4 SQL-86에서 지원하지 않는 주요 문

- `ALTER TABLE` (없음 - 테이블 수정 불가)
- `DROP TABLE` (없음 - 테이블 삭제 불가)
- `DROP VIEW` (없음 - 뷰 삭제 불가)
- `REVOKE` (없음 - 권한 회수 불가)
- `CREATE INDEX` (표준에 포함되지 않음, 벤더 확장으로만 가능)

> 중요: SQL-86에는 `ALTER`와 `DROP` 문이 없었다. 스키마 객체를 생성할 수는 있었지만 변경하거나 삭제하는 것은 표준에 정의되지 않았다. 한번 생성된 테이블의 구조를 표준 SQL만으로는 변경하거나 삭제할 수 없었으며, 이는 이 표준의 가장 큰 한계 중 하나였다.

---

## 8. 스키마 정의 언어 (DDL)

### 8.1 CREATE SCHEMA

SQL-86에서 스키마는 하나의 권한 식별자(authorization identifier) 아래 테이블, 뷰, 권한 부여를 묶는 단위이다. `CREATE SCHEMA` 문을 통해 정의하며, 스키마 내에 테이블, 뷰, 권한 부여를 포함한다.

```sql
CREATE SCHEMA AUTHORIZATION schema_owner
    -- 테이블 정의
    -- 뷰 정의
    -- 권한 부여
;
```

스키마 정의는 단일 문으로 작성되어야 했으며, 대화형(interactive) SQL이 아닌 일괄(batch) 방식으로만 실행할 수 있었다.

### 8.2 CREATE TABLE

```sql
CREATE TABLE 테이블명 (
    컬럼명1  데이터타입  [NOT NULL],
    컬럼명2  데이터타입  [NOT NULL],
    ...
    [UNIQUE (컬럼목록)]
    ...
);
```

#### 전체 예시

```sql
CREATE SCHEMA AUTHORIZATION admin

CREATE TABLE employees (
    emp_id      INTEGER     NOT NULL,
    emp_name    CHAR(30)    NOT NULL,
    dept_id     SMALLINT,
    salary      DECIMAL(10, 2),
    hire_date   CHAR(10),
    UNIQUE (emp_id)
)

CREATE TABLE departments (
    dept_id     SMALLINT    NOT NULL,
    dept_name   CHAR(50)    NOT NULL,
    UNIQUE (dept_id)
)

GRANT SELECT ON employees TO PUBLIC
GRANT SELECT, INSERT ON departments TO user1
;
```

### 8.3 컬럼 정의 옵션

SQL-86에서 컬럼에 사용할 수 있는 옵션은 매우 제한적이었다:

| 옵션 | 설명 |
|------|------|
| `NOT NULL` | NULL 값 비허용 |
| `NOT NULL UNIQUE` | NULL 비허용 + 고유값 제약 (사실상 기본키 역할) |
| `DEFAULT` | SQL-86에는 포함되지 않음 (SQL-92에서 추가) |
| `CHECK` | SQL-86에는 포함되지 않음 (SQL-89에서 추가) |

### 8.4 Level 2에서의 추가 기능

Level 2에서는 외래 키를 지원한다:

```sql
CREATE TABLE orders (
    order_id    INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    order_date  CHAR(10),
    UNIQUE (order_id),
    FOREIGN KEY (customer_id) REFERENCES customers
);
```

---

## 9. 데이터 조작 언어 (DML)

### 9.1 SELECT 문

SQL-86의 `SELECT` 문은 현재의 SQL과 기본 구조가 동일하다.

```sql
SELECT [ALL | DISTINCT] 선택목록
FROM 테이블참조 [, 테이블참조 ...]
[WHERE 검색조건]
[GROUP BY 컬럼목록]
[HAVING 그룹조건]
[ORDER BY 정렬목록]
```

#### 지원되는 SELECT 기능

| 기능 | 설명 | 예시 |
|------|------|------|
| 컬럼 선택 | 특정 컬럼 조회 | `SELECT name, age FROM t` |
| `*` (모든 컬럼) | 모든 컬럼 조회 | `SELECT * FROM t` |
| `DISTINCT` | 중복 제거 | `SELECT DISTINCT dept FROM t` |
| `ALL` (기본값) | 중복 포함 | `SELECT ALL name FROM t` |
| 표현식 | 산술 표현식 사용 | `SELECT salary * 12 FROM t` |
| `WHERE` 절 | 행 필터링 | `SELECT * FROM t WHERE age > 30` |
| `GROUP BY` | 그룹화 | `SELECT dept, COUNT(*) FROM t GROUP BY dept` |
| `HAVING` | 그룹 필터링 | `...GROUP BY dept HAVING COUNT(*) > 5` |
| `ORDER BY` | 결과 정렬 (ASC 기본값, DESC 명시 가능) | `SELECT * FROM t ORDER BY name ASC` |
| 테이블 조인 | FROM 절에서 여러 테이블 | `SELECT * FROM t1, t2 WHERE t1.id = t2.id` |
| 서브쿼리 | WHERE 절 내 서브쿼리 | `SELECT * FROM t WHERE id IN (SELECT ...)` |

#### 집계 함수 (Aggregate Functions)

SQL-86에서 지원하는 집계 함수:

| 함수 | 설명 |
|------|------|
| `COUNT(*)` | 행 수 계산 |
| `COUNT(컬럼)` | NULL이 아닌 값의 수 |
| `COUNT(DISTINCT 컬럼)` | 고유값 수 계산 |
| `SUM(컬럼)` | 합계 |
| `SUM(DISTINCT 컬럼)` | 고유값 합계 |
| `AVG(컬럼)` | 평균 |
| `AVG(DISTINCT 컬럼)` | 고유값 평균 |
| `MAX(컬럼)` | 최대값 |
| `MIN(컬럼)` | 최소값 |

```sql
SELECT COUNT(*), AVG(salary), SUM(salary), MAX(salary), MIN(salary)
FROM employees;
```

#### 비교 연산자

| 연산자 | 의미 |
|--------|------|
| `=` | 같음 |
| `<>` | 다름 |
| `<` | 작음 |
| `>` | 큼 |
| `<=` | 작거나 같음 |
| `>=` | 크거나 같음 |

#### 논리 연산자

| 연산자 | 의미 |
|--------|------|
| `AND` | 논리곱 |
| `OR` | 논리합 |
| `NOT` | 부정 |

#### 조건 키워드

| 키워드 | 설명 | 예시 |
|--------|------|------|
| `BETWEEN` | 범위 조건 (경계값 포함) | `WHERE age BETWEEN 20 AND 30` |
| `NOT BETWEEN` | 범위 외 조건 | `WHERE age NOT BETWEEN 20 AND 30` |
| `IN` | 목록 포함 | `WHERE dept IN ('A', 'B')` |
| `NOT IN` | 목록 미포함 | `WHERE dept NOT IN ('A', 'B')` |
| `LIKE` | 패턴 매칭 | `WHERE name LIKE 'K%'` |
| `IS NULL` | NULL 검사 | `WHERE phone IS NULL` |
| `IS NOT NULL` | NOT NULL 검사 | `WHERE phone IS NOT NULL` |
| `EXISTS` | 서브쿼리 존재 여부 | `WHERE EXISTS (SELECT ...)` |
| `NOT EXISTS` | 서브쿼리 비존재 여부 | `WHERE NOT EXISTS (SELECT ...)` |
| `ANY` / `SOME` | 서브쿼리 비교 (하나라도 만족) | `WHERE salary > ANY (SELECT ...)` |
| `ALL` | 서브쿼리 비교 (전체 만족) | `WHERE salary > ALL (SELECT ...)` |

#### LIKE 패턴 문자

| 패턴 문자 | 의미 |
|-----------|------|
| `%` | 0개 이상의 임의 문자 |
| `_` | 정확히 1개의 임의 문자 |
| `ESCAPE` | 이스케이프 문자 지정 (`%`나 `_` 자체를 리터럴로 검색) |

```sql
-- LIKE 예시
SELECT * FROM employees WHERE emp_name LIKE 'Kim%';
SELECT * FROM employees WHERE emp_name LIKE '_im%';
SELECT * FROM products WHERE code LIKE 'A_B%';
SELECT * FROM data WHERE value LIKE '20\%%' ESCAPE '\';
```

#### 서브쿼리 상세

```sql
-- 스칼라 서브쿼리: 평균 급여보다 높은 직원 조회
SELECT emp_name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);

-- IN 서브쿼리
SELECT emp_name
FROM employees
WHERE dept_id IN (SELECT dept_id FROM departments WHERE dept_name = 'Sales');

-- EXISTS 서브쿼리
SELECT emp_name
FROM employees e
WHERE EXISTS (SELECT * FROM departments d WHERE d.dept_id = e.dept_id);

-- ALL 서브쿼리
SELECT emp_name, salary
FROM employees
WHERE salary > ALL (SELECT salary FROM employees WHERE dept_id = 10);

-- ANY / SOME 서브쿼리
SELECT emp_name, salary
FROM employees
WHERE salary > ANY (SELECT salary FROM employees WHERE dept_id = 20);
```

#### 조인 (Join)

SQL-86에서는 명시적 JOIN 문법(`JOIN ... ON`)을 지원하지 않았다. 조인은 `FROM` 절에 여러 테이블을 나열하고 `WHERE` 절에서 조인 조건을 지정하는 암시적(implicit) 조인 방식만 가능했다.

```sql
-- 등가 조인 (equi-join) - SQL-86 방식 (카르테시안 곱 + WHERE 조건)
SELECT e.emp_name, d.dept_name
FROM employees e, departments d
WHERE e.dept_id = d.dept_id;

-- 셀프 조인
SELECT a.emp_name, b.emp_name
FROM employees a, employees b
WHERE a.dept_id = b.dept_id
  AND a.emp_id < b.emp_id;
```

> 중요: `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL JOIN`, `CROSS JOIN`, `NATURAL JOIN` 등의 명시적 조인 문법은 SQL-92에서 도입되었다.

#### 집합 연산

| 연산 | 지원 여부 | 설명 |
|------|-----------|------|
| `UNION` | 지원 | 합집합 (중복 제거) |
| `UNION ALL` | 지원 | 합집합 (중복 포함) |
| `INTERSECT` | 미지원 | SQL-92에서 추가 |
| `EXCEPT` / `MINUS` | 미지원 | SQL-92에서 추가 |

```sql
-- UNION 예시
SELECT emp_name FROM employees WHERE dept_id = 10
UNION
SELECT emp_name FROM employees WHERE salary > 50000;

-- UNION ALL (중복 허용)
SELECT emp_name FROM employees WHERE dept_id = 10
UNION ALL
SELECT emp_name FROM employees WHERE salary > 50000;
```

### 9.2 INSERT 문

```sql
-- 값 직접 삽입
INSERT INTO employees (emp_id, emp_name, dept_id, salary)
VALUES (101, 'Kim Cheolsu', 10, 45000.00);

-- 서브쿼리 결과 삽입
INSERT INTO high_salary_employees (emp_id, emp_name, salary)
SELECT emp_id, emp_name, salary
FROM employees
WHERE salary > 80000;
```

### 9.3 UPDATE 문

```sql
-- 검색 기반 UPDATE (단순 갱신)
UPDATE employees
SET salary = 50000.00
WHERE emp_id = 101;

-- 여러 열 갱신
UPDATE employees
SET salary = salary * 1.10,
    dept_id = 20
WHERE dept_id = 10;

-- 서브쿼리를 사용한 갱신 (Level 2)
UPDATE employees
SET salary = (SELECT AVG(salary) FROM employees)
WHERE dept_id = 30;

-- 커서 위치 기반 UPDATE (positioned update)
UPDATE employees
SET salary = salary * 1.10
WHERE CURRENT OF emp_cursor;
```

### 9.4 DELETE 문

```sql
-- 조건부 삭제
DELETE FROM employees
WHERE dept_id = 40;

-- 전체 삭제
DELETE FROM employees;

-- 서브쿼리를 사용한 삭제
DELETE FROM employees
WHERE dept_id IN (SELECT dept_id FROM departments WHERE dept_name = 'Closed');

-- 커서 위치 기반 DELETE (positioned delete)
DELETE FROM employees
WHERE CURRENT OF emp_cursor;
```

---

## 10. 무결성 제약 조건

SQL-86에서 지원하는 무결성 제약 조건은 매우 제한적이었다.

### 10.1 지원되는 제약 조건

| 제약 조건 | 설명 | 구문 |
|-----------|------|------|
| `NOT NULL` | 컬럼에 NULL 값 비허용 | `컬럼명 타입 NOT NULL` |
| `UNIQUE` | 컬럼(조합)의 고유성 보장 | `UNIQUE (컬럼1, 컬럼2)` |
| `NOT NULL UNIQUE` | 위 두 가지의 결합 (사실상 기본키 역할) | `컬럼명 타입 NOT NULL UNIQUE` |
| `FOREIGN KEY` (Level 2만) | 참조 무결성 (Level 2에서만 지원) | `FOREIGN KEY (컬럼) REFERENCES 테이블` |

### 10.2 지원되지 않는 제약 조건

| 제약 조건 | 추가된 표준 |
|-----------|------------|
| `PRIMARY KEY` 키워드 | SQL-89 |
| `CHECK` 제약조건 | SQL-89 |
| `DEFAULT` 값 | SQL-92 |
| `ON DELETE` / `ON UPDATE` 참조 동작 (CASCADE 등) | SQL-92 |
| 이름 있는 제약조건 (`CONSTRAINT name ...`) | SQL-92 |
| `ASSERTION` (테이블 간 제약) | SQL-92 |
| `DOMAIN` (사용자 정의 도메인) | SQL-92 |

> 중요: SQL-86에는 `PRIMARY KEY` 키워드가 없었다. 기본키의 개념은 `NOT NULL`과 `UNIQUE`의 조합으로 표현해야 했다. Level 1에서는 외래키를 통한 참조 무결성(referential integrity)도 지원되지 않았다. 이는 SQL-86의 가장 심각한 결함 중 하나로 지적되었으며, SQL-89에서 급히 보완되었다.

### 10.3 NULL 처리

SQL-86은 3값 논리(three-valued logic)를 채택했다. 모든 논리 표현식의 결과는 `TRUE`, `FALSE`, `UNKNOWN` 중 하나이다.

| A | B | A AND B | A OR B | NOT A |
|---|---|---------|--------|-------|
| TRUE | TRUE | TRUE | TRUE | FALSE |
| TRUE | FALSE | FALSE | TRUE | FALSE |
| TRUE | UNKNOWN | UNKNOWN | TRUE | UNKNOWN |
| FALSE | FALSE | FALSE | FALSE | TRUE |
| FALSE | UNKNOWN | FALSE | UNKNOWN | TRUE |
| UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN |

- NULL과의 비교는 항상 UNKNOWN을 반환
- `IS NULL`, `IS NOT NULL` 연산자로 NULL 여부 판별
- `WHERE` 절에서는 결과가 TRUE인 행만 반환됨 (UNKNOWN은 제외)

```sql
-- 올바른 NULL 검사
SELECT * FROM employees WHERE dept_id IS NULL;
SELECT * FROM employees WHERE dept_id IS NOT NULL;

-- 주의: 다음은 항상 UNKNOWN을 반환하므로 올바르지 않음
-- SELECT * FROM employees WHERE dept_id = NULL;
```

---

## 11. 뷰(View)

### 11.1 뷰 생성

```sql
CREATE VIEW 뷰명 [(컬럼목록)]
AS SELECT문
[WITH CHECK OPTION]
```

#### 예시

```sql
-- 단순 뷰
CREATE VIEW high_salary_emp AS
    SELECT emp_id, emp_name, salary
    FROM employees
    WHERE salary > 70000;

-- 컬럼 별칭을 가진 뷰
CREATE VIEW dept_summary (dept_id, emp_count, avg_salary) AS
    SELECT dept_id, COUNT(*), AVG(salary)
    FROM employees
    GROUP BY dept_id;

-- WITH CHECK OPTION (Level 2)
CREATE VIEW dept10_employees AS
    SELECT emp_id, emp_name, dept_id, salary
    FROM employees
    WHERE dept_id = 10
    WITH CHECK OPTION;
```

### 11.2 WITH CHECK OPTION

`WITH CHECK OPTION`이 지정된 뷰에 대한 `INSERT`나 `UPDATE`는 뷰의 `WHERE` 조건을 만족하는 행만 허용된다. Level 2에서 지원.

### 11.3 갱신 가능한 뷰 (Updatable Views)

SQL-86에서 뷰가 갱신 가능하려면 다음 조건을 모두 충족해야 했다:

1. `SELECT` 목록에 `DISTINCT`가 없을 것
2. `FROM` 절에 테이블이 하나만 있을 것 (조인 뷰 불가)
3. `SELECT` 목록의 모든 열이 단순 열 참조일 것 (표현식이나 집계 함수 불가)
4. `WHERE` 절에 서브쿼리가 해당 테이블을 참조하지 않을 것
5. `GROUP BY`가 없을 것
6. `HAVING`이 없을 것

```sql
-- 갱신 가능한 뷰
CREATE VIEW active_employees AS
    SELECT emp_id, emp_name, dept_id, salary
    FROM employees
    WHERE dept_id IS NOT NULL;

-- 갱신 불가능한 뷰 (집계 함수 사용)
CREATE VIEW dept_stats AS
    SELECT dept_id, AVG(salary)
    FROM employees
    GROUP BY dept_id;
```

> DROP VIEW 부재: SQL-86에는 `DROP VIEW` 문이 없다. 한번 생성된 뷰를 표준 SQL만으로 삭제할 수 없었다.

---

## 12. 권한 관리

### 12.1 GRANT 문

```sql
GRANT 권한목록 ON 테이블명 TO 사용자목록 [WITH GRANT OPTION]
```

### 12.2 지원 권한

| 권한 | 설명 |
|------|------|
| `SELECT` | 테이블/뷰 조회 |
| `INSERT` | 데이터 삽입 |
| `UPDATE` | 데이터 갱신 (특정 컬럼 지정 가능) |
| `DELETE` | 데이터 삭제 |
| `ALL PRIVILEGES` | 위 모든 권한 |

#### 예시

```sql
-- 특정 사용자에게 조회 권한 부여
GRANT SELECT ON employees TO user1;

-- 모든 사용자에게 조회 권한 부여
GRANT SELECT ON departments TO PUBLIC;

-- 여러 권한 부여 + 재부여 가능
GRANT SELECT, INSERT, UPDATE ON employees TO user2 WITH GRANT OPTION;

-- 특정 컬럼에 대한 UPDATE 권한
GRANT UPDATE (salary, dept_id) ON employees TO user3;

-- 모든 권한 부여
GRANT ALL PRIVILEGES ON employees TO admin;
```

### 12.3 GRANT OPTION

`WITH GRANT OPTION`을 포함하여 권한을 부여받은 사용자는 해당 권한을 다른 사용자에게 재부여할 수 있었다.

### 12.4 권한 체계의 특징

- 스키마를 생성한 사용자(스키마 소유자)가 해당 스키마 내 모든 객체에 대한 완전한 권한을 가진다.
- 뷰를 통해 간접적인 접근 제어가 가능하다 - 기본 테이블에 대한 직접 접근은 차단하고, 특정 열이나 행만 노출하는 뷰를 생성하여 해당 뷰에 대한 권한을 부여하는 방식.

### 12.5 REVOKE의 부재

> 중요: SQL-86에는 `REVOKE` 문이 없었다. 한번 부여된 권한을 표준 SQL만으로는 회수할 수 없었다. `REVOKE`는 SQL-89에서 추가되었다.

---

## 13. 임베디드 SQL (Embedded SQL)

SQL-86은 SQL을 호스트 프로그래밍 언어에 내장하는 임베디드 SQL을 정의했다. 전처리기(precompiler)가 SQL 문을 호스트 언어의 호출로 변환한다.

### 13.1 지원 호스트 언어

| 언어 | 지원 여부 |
|------|-----------|
| COBOL | 지원 |
| FORTRAN | 지원 |
| Pascal | 지원 |
| PL/I | 지원 |
| C | 미지원 (SQL-89에서 추가) |
| Ada | 미지원 (SQL-92에서 추가) |

### 13.2 임베디드 SQL 구문

임베디드 SQL 문은 호스트 언어 코드 내에서 `EXEC SQL`로 시작하고 언어별 종결자로 끝난다.

```cobol
      * COBOL에서의 임베디드 SQL 예시
       EXEC SQL
           SELECT EMP_NAME, SALARY
           INTO :WS-NAME, :WS-SALARY
           FROM EMPLOYEES
           WHERE EMP_ID = :WS-EMP-ID
       END-EXEC.
```

```fortran
C     FORTRAN에서의 임베디드 SQL 예시
      EXEC SQL SELECT EMP_NAME, SALARY
     +     INTO :ENAME, :ESAL
     +     FROM EMPLOYEES
     +     WHERE EMP_ID = :EID
      END-EXEC
```

### 13.3 호스트 변수

호스트 변수는 콜론(`:`)을 접두사로 사용하여 SQL 문 내에서 참조한다.

```sql
EXEC SQL
    SELECT emp_name INTO :host_name
    FROM employees
    WHERE emp_id = :host_id;
```

### 13.4 SQLCODE

SQL 문 실행 후의 상태를 나타내는 `SQLCODE` 변수:

| SQLCODE 값 | 의미 |
|-------------|------|
| `0` | 정상 완료 |
| `100` | 데이터 없음 (NOT FOUND) |
| 음수 | 오류 발생 |

```cobol
       EXEC SQL
           FETCH EMP_CURSOR INTO :WS-NAME, :WS-SALARY
       END-EXEC.
       IF SQLCODE = 100
           DISPLAY 'NO MORE ROWS'
       END-IF.
```

### 13.5 WHENEVER 문

오류 처리를 위한 선언적 구문:

```sql
EXEC SQL WHENEVER SQLERROR GOTO error_handler;
EXEC SQL WHENEVER NOT FOUND GOTO not_found_handler;
EXEC SQL WHENEVER SQLWARNING CONTINUE;
```

| 조건 | 의미 |
|------|------|
| `SQLERROR` | SQLCODE < 0 (오류) |
| `NOT FOUND` | SQLCODE = 100 (데이터 없음) |
| `SQLWARNING` | 경고 발생 |

| 동작 | 의미 |
|------|------|
| `CONTINUE` | 다음 문 계속 실행 |
| `GOTO 레이블` | 지정된 레이블로 이동 |

### 13.6 지시자 변수 (Indicator Variables)

NULL 값을 처리하기 위해 지시자 변수(indicator variable)를 사용:

```sql
EXEC SQL
    SELECT salary INTO :host_salary :ind_salary
    FROM employees
    WHERE emp_id = :host_id;
```

| 지시자 값 | 의미 |
|-----------|------|
| `0` | 정상 값 |
| `-1` | NULL 값 |
| 양수 | 문자열 절단 발생 (원래 길이 반환) |

---

## 14. 모듈 언어 (Module Language)

SQL-86은 임베디드 SQL 외에 모듈 언어라는 대안적 접근 방식도 정의했다.

### 14.1 개념

모듈 언어에서는 SQL 프로시저들을 별도의 모듈로 정의하고, 호스트 언어에서 이를 호출하는 방식을 사용한다. 모듈은 하나 이상의 프로시저로 구성되며, 각 프로시저는 하나의 SQL 문을 포함한다. 이는 호스트 언어 코드와 SQL 코드의 명확한 분리를 가능하게 했다.

### 14.2 모듈 구조

```sql
MODULE 모듈명
LANGUAGE 호스트언어명
AUTHORIZATION 사용자명

PROCEDURE 프로시저명 (파라미터목록);
    SQL문;

PROCEDURE 프로시저명2 (파라미터목록);
    SQL문;
```

### 14.3 예시

```sql
MODULE employee_module
LANGUAGE COBOL
AUTHORIZATION admin

PROCEDURE get_employee
    (:emp_id INTEGER, :emp_name CHARACTER(30), :salary DECIMAL(10,2), SQLCODE);
    SELECT emp_name, salary
    INTO :emp_name, :salary
    FROM employees
    WHERE emp_id = :emp_id;

PROCEDURE insert_employee
    (:emp_id INTEGER, :emp_name CHARACTER(30), :salary DECIMAL(10,2), SQLCODE);
    INSERT INTO employees (emp_id, emp_name, salary)
    VALUES (:emp_id, :emp_name, :salary);
```

### 14.4 모듈 언어 vs 임베디드 SQL

| 특성 | 모듈 언어 | 임베디드 SQL |
|------|-----------|-------------|
| SQL 위치 | 별도 모듈 | 호스트 코드 내 |
| 전처리기 필요 | 불필요 | 필요 |
| 코드 분리 | 명확한 분리 | 혼합 |
| 실제 채택률 | 낮음 | 높음 |

> 실무에서는 임베디드 SQL이 모듈 언어보다 훨씬 널리 사용되었다.

---

## 15. 표현식 및 조건

### 15.1 산술 표현식

| 연산자 | 의미 |
|--------|------|
| `+` | 더하기 |
| `-` | 빼기 |
| `*` | 곱하기 |
| `/` | 나누기 |
| 단항 `-` | 부호 반전 |
| 단항 `+` | 부호 유지 |

```sql
SELECT emp_name, salary * 12 + 1000 FROM employees;
SELECT price * quantity - discount FROM orders;
```

### 15.2 문자열 표현식

SQL-86에서 문자열 관련 기능은 매우 제한적이었다:

- 문자열 연결(`||` 또는 `CONCAT`) 미지원 (SQL-89에서 추가)
- `SUBSTRING`, `UPPER`, `LOWER`, `TRIM` 등 문자열 함수 미지원 (SQL-92에서 추가)
- 문자열 리터럴은 작은따옴표로 감싼다: `'Hello'`

### 15.3 값 표현

```sql
-- 리터럴 값
'Hello World'       -- 문자열 리터럴
42                  -- 정수 리터럴
3.14                -- 소수 리터럴
1.5E10              -- 지수 표기 리터럴

-- NULL
NULL

-- 사용자 참조
USER                -- 현재 사용자 식별자 반환
```

### 15.4 SQL-86에서 지원하지 않는 표현식

- `CASE` 표현식 (SQL-92에서 추가)
- `CAST` 타입 변환 (SQL-92에서 추가)
- `COALESCE` / `NULLIF` (SQL-92에서 추가)
- 날짜/시간 연산 (날짜/시간 타입 자체가 없음)

---

## 16. 커서(Cursor)

커서는 여러 행을 반환하는 쿼리 결과를 한 행씩 처리하기 위한 메커니즘이다. 프로그래밍 언어에서 다중 행 결과를 처리하기 위해 필수적이다.

### 16.1 커서 사용법

```sql
-- 1. 커서 선언
DECLARE emp_cursor CURSOR FOR
    SELECT emp_id, emp_name, salary
    FROM employees
    WHERE dept_id = :host_dept_id
    ORDER BY emp_name;

-- 2. 커서 열기
OPEN emp_cursor;

-- 3. 행 가져오기 (반복)
FETCH emp_cursor INTO :host_emp_id, :host_name, :host_salary;

-- 4. 위치 기반 갱신 (선택적)
UPDATE employees
SET salary = salary * 1.1
WHERE CURRENT OF emp_cursor;

-- 5. 위치 기반 삭제 (선택적)
DELETE FROM employees
WHERE CURRENT OF emp_cursor;

-- 6. 커서 닫기
CLOSE emp_cursor;
```

### 16.2 커서 특성

SQL-86의 커서는 다음과 같은 특성을 갖는다:

- 전진 전용(forward-only): 앞으로만 이동 가능
- 읽기 전용 또는 갱신 가능: 갱신 가능 커서를 통해 `UPDATE ... WHERE CURRENT OF` 및 `DELETE ... WHERE CURRENT OF` 가능
- 스크롤 커서(scrollable cursor) 미지원: SQL-92에서 추가
- 민감도(sensitivity) 개념 없음: SQL-92에서 도입

---

## 17. 트랜잭션 관리

### 17.1 지원 문

```sql
COMMIT WORK;    -- 현재 트랜잭션을 커밋(확정)
ROLLBACK WORK;  -- 현재 트랜잭션을 롤백(취소)
```

### 17.2 트랜잭션 특성

- 트랜잭션은 암시적으로 시작된다 (명시적 `BEGIN TRANSACTION` 문 없음)
- SQL 문이 실행되면 자동으로 트랜잭션이 시작된다
- `COMMIT WORK` 또는 `ROLLBACK WORK`로 트랜잭션이 종료된다
- 격리 수준(isolation level) 설정 문은 미지원 (SQL-92에서 `SET TRANSACTION ISOLATION LEVEL` 추가)
- `SAVEPOINT` 미지원 (SQL-92에서 추가)

---

## 18. 두 가지 수준(Level)의 적합성

SQL-86은 두 가지 적합성 수준을 정의했다.

### 18.1 Level 1 (최소 적합성)

Level 1은 핵심적인 기본 기능만 포함하며, 당시 대부분의 상용 구현이 지원하는 기능으로 구성되었다.

Level 1에 포함된 주요 기능:
- 기본 테이블 생성 (`CREATE TABLE`)
- `NOT NULL`, `UNIQUE` 제약 조건
- 기본 `SELECT`, `INSERT`, `UPDATE`, `DELETE`
- 기본 `WHERE` 조건 (비교, `AND`, `OR`, `NOT`)
- 기본 집계 함수 (`COUNT`, `SUM`, `AVG`, `MAX`, `MIN`)
- `GROUP BY`, `HAVING`
- `UNION`
- `ORDER BY`
- 커서 처리
- `GRANT`

Level 1에서 제외된 기능:
- `LIKE` (패턴 매칭)
- `BETWEEN`
- 서브쿼리
- `EXISTS`, `ANY`, `ALL` 정량 비교
- `HAVING` 절의 서브쿼리
- `FOREIGN KEY` (외래 키)
- 뷰의 일부 고급 기능
- `WITH CHECK OPTION`

### 18.2 Level 2 (전체 적합성)

Level 2는 SQL-86 표준에 정의된 모든 기능을 포함하며, 보다 완전한 SQL 기능 집합을 제공한다.

Level 2에서 추가된 주요 기능:
- `LIKE` 연산자
- `BETWEEN` 연산자
- 서브쿼리 (스칼라, IN, EXISTS, ANY/ALL)
- `FOREIGN KEY` (외래 키 참조 무결성)
- `UNION`의 완전한 지원
- 뷰의 모든 기능
- `WITH CHECK OPTION`
- 보다 복잡한 표현식

### 18.3 FIPS 적합성

미국 연방 정부의 FIPS PUB 127은 SQL-86의 Level 2에 추가적인 무결성 강화(Integrity Enhancement Feature, IEF)를 요구했으며, 이것이 후에 SQL-89 표준으로 이어졌다. FIPS 127-1은 정부 기관이 구매하는 데이터베이스 시스템이 표준을 준수하도록 요구하여, 표준 채택을 가속화하는 데 큰 역할을 했다.

---

## 19. 제한 사항 및 알려진 문제점

SQL-86은 최초의 SQL 표준으로서 많은 제한이 있었다.

### 19.1 부재하는 DDL 기능

| 부재 기능 | 설명 | 도입 시기 |
|-----------|------|-----------|
| `ALTER TABLE` | 기존 테이블 구조 변경 불가 | SQL-89 (제한적), SQL-92 (완전) |
| `DROP TABLE` | 테이블 삭제 불가 | SQL-89 |
| `DROP VIEW` | 뷰 삭제 불가 | SQL-89 |
| `CREATE INDEX` | 인덱스 생성이 표준에 미포함 | 표준에 공식 포함되지 않음 |
| `PRIMARY KEY` 키워드 | 기본 키 제약조건 키워드 부재 | SQL-89 |
| `CHECK` 제약조건 | 임의의 도메인 무결성 검사 불가 | SQL-89 |
| `DEFAULT` 값 | 열 기본값 지정 불가 | SQL-92 |
| `CASCADE` 참조 동작 | `ON DELETE CASCADE` 등 부재 | SQL-92 |
| `CREATE DOMAIN` | 사용자 정의 도메인 불가 | SQL-92 |
| `REVOKE` | 권한 회수 불가 | SQL-89 |

### 19.2 부재하는 DML/쿼리 기능

| 부재 기능 | 설명 | 도입 시기 |
|-----------|------|-----------|
| 명시적 `JOIN` 구문 | `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL JOIN`, `CROSS JOIN` 없음 | SQL-92 |
| `INTERSECT` | 교집합 연산자 없음 | SQL-92 |
| `EXCEPT` / `MINUS` | 차집합 연산자 없음 | SQL-92 |
| `CASE` 표현식 | 조건부 값 표현식 불가 | SQL-92 |
| `CAST` 타입 변환 | 명시적 타입 변환 불가 | SQL-92 |
| 문자열 결합 (`\|\|`) | 문자열 연결 연산자 없음 | SQL-89 |
| 문자열 함수 | `SUBSTRING`, `UPPER`, `LOWER`, `TRIM` 등 없음 | SQL-92 |
| 날짜/시간 연산 | 날짜/시간 타입 자체가 없음 | SQL-92 |
| `COALESCE` / `NULLIF` | NULL 처리 함수 없음 | SQL-92 |
| `VARCHAR` | 가변 길이 문자열 부재 | SQL-89 |

### 19.3 트랜잭션 관리의 한계

- 명시적 `BEGIN TRANSACTION` 문 없음 (암시적 시작만 가능)
- `SAVEPOINT` 미지원 (SQL-92에서 도입)
- 격리 수준(Isolation Level) 지정 불가 (SQL-92에서 `SET TRANSACTION ISOLATION LEVEL` 도입)

### 19.4 기타 부재 기능

| 부재 기능 | 도입 시기 |
|-----------|-----------|
| 대화형 SQL (Direct SQL) | SQL-92 (부록에서만 비공식 언급) |
| 저장 프로시저/함수 (SQL/PSM) | SQL-92 이후 |
| 트리거(Trigger) | SQL:1999 |
| 재귀 쿼리 | SQL:1999 |
| 윈도우 함수 | SQL:2003 |
| 정보 스키마 (`INFORMATION_SCHEMA`) | SQL-92 |
| 임시 테이블 | SQL-92 |
| 정규 표현식 | SQL:1999 |
| C 언어 바인딩 | SQL-89 |

### 19.5 설계 문제

1. 대화형(Interactive) SQL 미포함: SQL-86은 임베디드 SQL과 모듈 언어만 정의했다. 대화형 SQL(직접 SQL)은 표준에 포함되지 않았다.

2. 스키마 수정 불가: `CREATE`만 있고 `ALTER`, `DROP`이 없어 스키마 진화(schema evolution)가 불가능했다.

3. 데이터 타입 빈약: 날짜/시간, 가변 문자열 등 실무에서 필수적인 타입이 부재했다.

4. 참조 무결성 제한: Level 1에서는 외래키가 지원되지 않고, Level 2에서도 CASCADE 등의 참조 동작이 없었다.

5. 문자열 처리 미흡: 문자열 함수, 연결 연산자 등이 없어 문자열 조작이 거의 불가능했다.

6. 표현력 부족: `CASE`, `CAST`, `COALESCE` 등 현대 SQL에서 필수적인 표현식이 없었다.

7. 구현 정의 사항 과다: 많은 세부사항이 "구현 정의(implementation-defined)"로 남겨져 이식성이 제한되었다.

### 19.6 벤더별 차이

SQL-86은 기존 벤더 구현들 사이의 "최소 공통분모(least common denominator)"를 표준화한 것에 가까웠기 때문에, 각 벤더는 자사 고유의 확장 기능을 광범위하게 제공했고, 이는 표준의 이식성 목표를 크게 약화시켰다.

---

## 20. 선행 작업과의 관계

### 20.1 IBM System R과 SEQUEL/SQL

SQL-86의 직접적인 조상은 IBM의 System R 프로젝트에서 개발된 SEQUEL/SQL이다.

| 요소 | System R SQL | SQL-86 |
|------|-------------|--------|
| `SELECT-FROM-WHERE` | 지원 | 지원 |
| `GROUP BY`, `HAVING` | 지원 | 지원 |
| 서브쿼리 | 지원 | 지원 (Level 2) |
| `INSERT`, `UPDATE`, `DELETE` | 지원 | 지원 |
| 뷰 | 지원 | 지원 |
| `GRANT` | 지원 | 지원 |
| `REVOKE` | 지원 | 미지원 |

### 20.2 Codd의 관계형 모델

Edgar F. Codd의 관계형 모델(1970)이 이론적 기반이지만, SQL-86은 관계형 모델을 완전히 구현하지는 않았다:

- 관계형 모델의 도메인(domain) 개념이 완전히 반영되지 않음
- 중복 행(duplicate rows)을 허용 (관계형 모델에서는 관계가 집합이므로 중복을 허용하지 않음)
- NULL 처리 방식에 대한 이론적 논쟁 존재 (Codd 본인을 포함한 학계에서 SQL이 관계형 모델을 충실히 구현하지 못한다고 비판)

### 20.3 기존 상용 구현의 영향

SQL-86은 기존 상용 RDBMS의 SQL 구현을 기반으로 했다:

- IBM SQL/DS (1981): SQL의 원조 구현
- IBM DB2 (1983): 가장 큰 영향을 미침
- Oracle (1979~): 최초의 상용 SQL RDBMS
- Ingres: 원래 QUEL을 사용했으나 SQL 지원 추가
- Informix, Sybase 등 다양한 벤더들의 SQL 구현

---

## 21. 영향과 채택

### 21.1 직접적 영향

1. 최초의 공식 SQL 표준: 이후 40년에 걸친 SQL 표준화 역사의 출발점
2. 산업 표준 확립: SQL이 관계형 데이터베이스의 사실상(de facto) 표준을 넘어 공식(de jure) 표준이 됨
3. 정부 조달 기준: FIPS PUB 127 채택으로 미국 연방 정부 시스템 조달에서 SQL 적합성이 요구됨
4. 벤더 수렴: 비SQL 데이터베이스 제품들도 SQL 인터페이스를 추가하기 시작함
5. 이식성(Portability)의 기반: 공통 기반을 제공함으로써 이식성 향상의 첫걸음
6. 교육 기반: 대학과 교육기관에서 SQL을 가르치는 공통 기반이 됨

### 21.2 SQL-86 적합성을 주장한 주요 제품

| 제품 | 벤더 | 참고 |
|------|------|------|
| DB2 | IBM | SQL 표준의 주요 기반 |
| SQL/DS | IBM | 메인프레임 환경 |
| Oracle | Oracle Corporation | 독자적 확장 다수 포함 |
| Rdb/VMS | Digital Equipment Corporation | VAX/VMS 환경 |
| NonStop SQL | Tandem Computers | 결함 허용 시스템 |
| INFORMIX | Informix Software | UNIX 환경 |
| Sybase SQL Server | Sybase | 후에 Microsoft SQL Server의 기반 |
| Ingres | Relational Technology Inc. | 원래 QUEL에서 SQL 전환 |

### 21.3 표준의 한계에 대한 비판

SQL-86은 발표 당시에도 상당한 비판을 받았다:

- 너무 보수적: 이미 상용 구현에 존재하던 많은 기능(ALTER TABLE, DROP TABLE, VARCHAR 등)이 표준에 포함되지 않았다
- 최소 공통 분모: 모든 벤더가 동의할 수 있는 최소한의 기능만 포함하다 보니 실용적으로 부족한 부분이 많았다
- 관계형 모델과의 괴리: Codd를 포함한 학계에서 SQL이 관계형 모델을 충실히 구현하지 못한다고 비판
- 날짜/시간 타입 부재: 실무적으로 가장 크게 비판받은 부분 중 하나

### 21.4 한계에 대한 반응

SQL-86의 부족한 점들이 명확해지면서, 업계와 표준 위원회는 빠르게 후속 표준을 추진했다:

- SQL-89 (ANSI X3.135-1989 / ISO 9075:1989): SQL-86의 긴급 보완
  - 무결성 강화: `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`, `DEFAULT`
  - `VARCHAR` 타입 추가
  - `REVOKE` 추가
  - C 언어 바인딩 추가
  - 문자열 연결 연산자(`||`) 추가
  - `ALTER TABLE`, `DROP TABLE` 추가

- SQL-92 (ANSI X3.135-1992 / ISO/IEC 9075:1992): 대규모 확장
  - 약 600페이지로 확장 (SQL-86의 6배)
  - 날짜/시간 타입, 명시적 JOIN, CASE, CAST, 동적 SQL, 대화형 SQL, INFORMATION_SCHEMA, 트랜잭션 격리 수준, 임시 테이블 등 추가

### 21.5 핵심 유산 (Legacy)

SQL-86이 정의한 핵심 구문과 개념은 40년이 지난 현재에도 SQL의 근간을 이루고 있다:

- `SELECT ... FROM ... WHERE ...` 기본 구조
- 집계 함수와 `GROUP BY` / `HAVING`
- `UNION`을 통한 결과 결합
- 서브쿼리 (`IN`, `EXISTS`, `ALL`, `ANY`)
- `INSERT`, `UPDATE`, `DELETE`의 기본 구문
- 뷰(View) 개념
- `GRANT` 기반 권한 관리
- NULL 처리와 3값 논리
- 커서(Cursor) 기반 프로그래밍 인터페이스

---

## 22. SQL-86과 후속 표준의 비교

### 22.1 표준 크기 비교

| 표준 | 연도 | 대략적 페이지 수 |
|------|------|-----------------|
| SQL-86 | 1986 | ~100 페이지 |
| SQL-89 | 1989 | ~120 페이지 |
| SQL-92 | 1992 | ~600 페이지 |
| SQL:1999 | 1999 | ~2,000 페이지 |
| SQL:2003 | 2003 | ~3,600 페이지 |
| SQL:2023 | 2023 | ~4,000+ 페이지 |

### 22.2 주요 기능 추가 연표

| 기능 | SQL-86 | SQL-89 | SQL-92 |
|------|--------|--------|--------|
| `CREATE TABLE` | O | O | O |
| `ALTER TABLE` | X | O | O |
| `DROP TABLE` | X | O | O |
| `PRIMARY KEY` | X | O | O |
| `FOREIGN KEY` (Level 2만) | O (제한적) | O | O |
| `CHECK` 제약 | X | O | O |
| `DEFAULT` | X | O | O |
| `VARCHAR` | X | O | O |
| `REVOKE` | X | O | O |
| 문자열 연결 (`\|\|`) | X | O | O |
| C 언어 바인딩 | X | O | O |
| 날짜/시간 타입 | X | X | O |
| 명시적 `JOIN` | X | X | O |
| `CASE` 표현식 | X | X | O |
| `CAST` 변환 | X | X | O |
| `INTERSECT`, `EXCEPT` | X | X | O |
| 동적 SQL | X | X | O |
| 정보 스키마 | X | X | O |
| 격리 수준 | X | X | O |
| 스크롤 커서 | X | X | O |
| 대화형 SQL | X | X | O |
| 임시 테이블 | X | X | O |
| 도메인 | X | X | O |
| `CASCADE` 참조 동작 | X | X | O |

### 22.3 이후 표준의 주요 추가 사항

| 표준 | 연도 | 주요 추가 사항 |
|------|------|----------------|
| SQL-86 | 1986 | 최초 표준 |
| SQL-89 | 1989 | 무결성 강화 (PRIMARY KEY, CHECK, DEFAULT), REVOKE, VARCHAR, C 언어 바인딩 |
| SQL-92 | 1992 | 명시적 JOIN, 날짜/시간 타입, CASE, CAST, ALTER/DROP TABLE, 임시 테이블, INFORMATION_SCHEMA, 트랜잭션 격리 수준 등 대폭 확장 |
| SQL:1999 | 1999 | 재귀 쿼리, 트리거, 객체지향 기능, 저장 프로시저(SQL/PSM), 정규 표현식, BOOLEAN 타입 |
| SQL:2003 | 2003 | 윈도우 함수, MERGE, 시퀀스, XML 지원 |
| SQL:2006 | 2006 | SQL/XML 확장 |
| SQL:2008 | 2008 | TRUNCATE, FETCH FIRST, 개선된 트리거 |
| SQL:2011 | 2011 | 시간 데이터베이스(temporal database) 지원 |
| SQL:2016 | 2016 | JSON 지원, 행 패턴 매칭 |
| SQL:2023 | 2023 | 속성 그래프 쿼리(SQL/PGQ), JSON 확장 |

---

## 23. 참고 자료

### 23.1 표준 문서

- ANSI X3.135-1986, *"Database Language SQL"*, American National Standards Institute, 1986
- ISO 9075:1987, *"Information technology — Database languages — SQL"*, International Organization for Standardization, 1987
- FIPS PUB 127, *"Database Language SQL"*, National Institute of Standards and Technology, 1987

### 23.2 관련 문헌

- Chamberlin, D.D. and Boyce, R.F., *"SEQUEL: A Structured English Query Language"*, Proceedings of the 1974 ACM SIGFIDET Workshop, 1974
- Codd, E.F., *"A Relational Model of Data for Large Shared Data Banks"*, Communications of the ACM, Vol. 13, No. 6, June 1970
- Date, C.J., *"A Guide to the SQL Standard"*, Addison-Wesley, 1st Edition, 1987
- Melton, Jim and Simon, Alan R., *"Understanding the New SQL: A Complete Guide"*, Morgan Kaufmann, 1993
- Cannan, S.J. and Otten, G.A.M., *"SQL — The Standard Handbook"*, McGraw-Hill, 1993

### 23.3 역사적 맥락

- Date, C.J., *"An Introduction to Database Systems"*, Addison-Wesley (다수 판본)
- Chamberlin, D.D., *"A Summary of User Experience with the SQL Data Sublanguage"*, Proceedings of the International Conference on Databases, 1980
- Chamberlin, D.D. et al., *"A History and Evaluation of System R"*, Communications of the ACM, Vol. 24, No. 10, October 1981

---

*본 문서는 SQL-86 (ANSI X3.135-1986 / ISO 9075:1987) 표준에 대한 포괄적 정리이다.*
