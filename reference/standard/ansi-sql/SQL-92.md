# SQL-92 (SQL2) 표준 완벽 가이드

## ISO/IEC 9075:1992 — Database Language SQL

---

## 목차

1. [개요](#1-개요)
2. [역사적 배경](#2-역사적-배경)
3. [적합성 수준](#3-적합성-수준)
4. [DDL 확장](#4-ddl-확장)
5. [새로운 데이터 타입](#5-새로운-데이터-타입)
6. [JOIN 구문 혁신](#6-join-구문-혁신)
7. [서브쿼리 확장](#7-서브쿼리-확장)
8. [집합 연산](#8-집합-연산)
9. [CASE 표현식](#9-case-표현식)
10. [CAST 변환](#10-cast-변환)
11. [문자열 함수](#11-문자열-함수)
12. [스크롤 가능 커서](#12-스크롤-가능-커서)
13. [트랜잭션 격리 수준](#13-트랜잭션-격리-수준)
14. [INFORMATION_SCHEMA](#14-information_schema)
15. [진단](#15-진단)
16. [동적 SQL](#16-동적-sql)
17. [연결 관리](#17-연결-관리)
18. [임시 테이블](#18-임시-테이블)
19. [제약사항과 한계](#19-제약사항과-한계)
20. [영향과 의의](#20-영향과-의의)

---

## 1. 개요

SQL-92(공식명: ISO/IEC 9075:1992)는 1992년에 ISO와 IEC에 의해 공식 발표된 SQL 표준의 두 번째 주요 개정판이다. SQL2라고도 불리며, 이전 버전인 SQL-86/SQL-89에 비해 기능이 대폭 확장되었다. SQL-92는 약 600페이지에 달하는 방대한 명세로, SQL-89의 약 120페이지에 비해 5배 이상 분량이 증가했다.

### 주요 특징 요약

| 항목 | 내용 |
|------|------|
| 공식 명칭 | ISO/IEC 9075:1992 |
| 별칭 | SQL2, SQL-92 |
| 발표 연도 | 1992년 |
| 이전 표준 | SQL-89 (ISO/IEC 9075:1989) |
| 다음 표준 | SQL:1999 (ISO/IEC 9075:1999) |
| 분량 | 약 626페이지 |
| 적합성 수준 | Entry, Intermediate, Full |

---

## 2. 역사적 배경

### SQL 표준의 흐름

```
1970  - E.F. Codd의 관계형 모델 논문 발표
1974  - IBM System R 프로젝트에서 SEQUEL 개발
1979  - Oracle V2 최초 상용 RDBMS 출시
1986  - SQL-86 (최초 SQL 표준, ISO 9075:1987)
1989  - SQL-89 (무결성 제약사항 추가)
1992  - SQL-92 ★ (대규모 확장)
```

### SQL-89의 한계

SQL-89는 기본적인 데이터 조작과 정의 기능만 제공했으며, 다음과 같은 한계가 있었다:

- 제한적인 데이터 타입: 날짜/시간 타입 부재
- 단순한 JOIN 문법: `FROM` 절에서 쉼표로 구분하는 카테시안 곱만 지원
- 조건부 논리 부재: CASE 표현식 없음
- 타입 변환 부재: CAST 연산자 없음
- 단방향 커서: 스크롤 불가

SQL-92는 이러한 한계를 극복하고 관계형 데이터베이스의 실질적인 표준을 확립하고자 했다.

---

## 3. 적합성 수준

SQL-92는 구현의 현실성을 고려하여 세 가지 적합성 수준(Conformance Level)을 정의했다.

### 수준 비교표

| 수준 | 복잡도 | 포함 기능 |
|------|--------|-----------|
| Entry (입문) | 낮음 | SQL-89 + 약간의 개선. 대부분의 DBMS가 이 수준 충족 |
| Intermediate (중간) | 중간 | Entry + JOIN 구문, CASE, CAST, 도메인, 문자열 함수, 동적 SQL 등 |
| Full (전체) | 높음 | Intermediate + 스크롤 커서, ASSERTION, BIT 타입, 서브쿼리 확장 등 |

### Entry Level 주요 기능

- 기본 DDL (CREATE TABLE, CREATE VIEW)
- 기본 DML (SELECT, INSERT, UPDATE, DELETE)
- 기본 무결성 제약사항 (PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK)
- VARCHAR 데이터 타입
- 날짜/시간 타입 (DATE, TIME, TIMESTAMP)
- 기본 스칼라 표현식

### Intermediate Level 추가 기능

- 명시적 JOIN 구문 (INNER, LEFT, RIGHT, FULL, CROSS, NATURAL)
- CASE 표현식, NULLIF, COALESCE
- CAST 연산자
- 문자열 함수 (SUBSTRING, UPPER, LOWER, TRIM 등)
- CREATE DOMAIN
- ALTER TABLE
- 동적 SQL (PREPARE, EXECUTE)
- 연결 관리 (CONNECT, DISCONNECT)

### Full Level 추가 기능

- 스크롤 가능 커서 (SCROLL CURSOR)
- CREATE ASSERTION
- BIT, BIT VARYING 데이터 타입
- 임시 테이블
- 서브쿼리의 모든 위치 사용
- CORRESPONDING을 사용한 집합 연산
- 자기 참조 UPDATE/DELETE

---

## 4. DDL 확장

### 4.1 ALTER TABLE

SQL-89에서는 테이블 생성 후 구조 변경이 표준에 포함되지 않았으나, SQL-92에서 `ALTER TABLE`이 공식 도입되었다.

```sql
-- 컬럼 추가
ALTER TABLE 직원
  ADD COLUMN 이메일 VARCHAR(100);

-- 컬럼 기본값 추가
ALTER TABLE 직원
  ADD COLUMN 상태 VARCHAR(10) DEFAULT '활성';

-- 컬럼 기본값 변경
ALTER TABLE 직원
  ALTER COLUMN 상태 SET DEFAULT '비활성';

-- 컬럼 기본값 삭제
ALTER TABLE 직원
  ALTER COLUMN 상태 DROP DEFAULT;

-- 컬럼 삭제 (CASCADE: 종속 객체도 삭제)
ALTER TABLE 직원
  DROP COLUMN 이메일 CASCADE;

-- 컬럼 삭제 (RESTRICT: 종속 객체가 있으면 거부)
ALTER TABLE 직원
  DROP COLUMN 이메일 RESTRICT;

-- 제약사항 추가
ALTER TABLE 직원
  ADD CONSTRAINT chk_급여 CHECK (급여 > 0);

-- 제약사항 삭제
ALTER TABLE 직원
  DROP CONSTRAINT chk_급여 CASCADE;
```

### 4.2 DROP 문

```sql
-- 테이블 삭제
DROP TABLE 직원 CASCADE;
DROP TABLE 직원 RESTRICT;

-- 뷰 삭제
DROP VIEW 직원_뷰 CASCADE;
DROP VIEW 직원_뷰 RESTRICT;

-- 스키마 삭제
DROP SCHEMA 인사관리 CASCADE;
DROP SCHEMA 인사관리 RESTRICT;

-- 도메인 삭제
DROP DOMAIN 이메일_타입 CASCADE;
DROP DOMAIN 이메일_타입 RESTRICT;

-- ASSERTION 삭제
DROP ASSERTION 급여_총합_제약;
```

### 4.3 CREATE SCHEMA

스키마는 데이터베이스 객체의 논리적 그룹이다. SQL-92에서 처음 표준화되었다.

```sql
-- 기본 스키마 생성
CREATE SCHEMA 인사관리
  AUTHORIZATION 관리자;

-- 스키마 내에 테이블과 뷰를 함께 생성
CREATE SCHEMA 인사관리
  AUTHORIZATION 관리자

  CREATE TABLE 부서 (
    부서코드    CHAR(4)      PRIMARY KEY,
    부서명      VARCHAR(50)  NOT NULL,
    위치        VARCHAR(100)
  )

  CREATE TABLE 직원 (
    직원번호    INTEGER      PRIMARY KEY,
    이름        VARCHAR(50)  NOT NULL,
    부서코드    CHAR(4)      REFERENCES 부서(부서코드),
    입사일      DATE         NOT NULL,
    급여        DECIMAL(10,2)
  )

  CREATE VIEW 부서별_직원수 AS
    SELECT 부서코드, COUNT(*) AS 인원수
    FROM 직원
    GROUP BY 부서코드

  GRANT SELECT ON 부서별_직원수 TO PUBLIC;
```

### 4.4 CREATE DOMAIN

도메인은 재사용 가능한 컬럼 정의로, 데이터 타입과 제약사항을 묶어서 정의할 수 있다.

```sql
-- 기본 도메인 생성
CREATE DOMAIN 이메일_타입 AS VARCHAR(255)
  CHECK (VALUE LIKE '%@%.%');

-- 양수 금액 도메인
CREATE DOMAIN 금액_타입 AS DECIMAL(12, 2)
  DEFAULT 0.00
  CHECK (VALUE >= 0);

-- 상태 코드 도메인
CREATE DOMAIN 상태_타입 AS CHAR(1)
  DEFAULT 'A'
  CHECK (VALUE IN ('A', 'I', 'D'));

-- 도메인 사용
CREATE TABLE 고객 (
    고객번호    INTEGER       PRIMARY KEY,
    이름        VARCHAR(100)  NOT NULL,
    이메일      이메일_타입,
    보유잔액    금액_타입,
    상태        상태_타입
);
```

### 4.5 CREATE ASSERTION

어서션은 데이터베이스 전체에 적용되는 제약사항으로, 여러 테이블에 걸친 비즈니스 규칙을 정의할 수 있다.

```sql
-- 부서별 급여 총합 제한
CREATE ASSERTION 급여_총합_제약
  CHECK (
    NOT EXISTS (
      SELECT 부서코드
      FROM 직원
      GROUP BY 부서코드
      HAVING SUM(급여) > 50000000
    )
  );

-- 관리자는 반드시 직원이어야 함
CREATE ASSERTION 관리자_직원_제약
  CHECK (
    NOT EXISTS (
      SELECT *
      FROM 부서
      WHERE 관리자번호 IS NOT NULL
        AND 관리자번호 NOT IN (SELECT 직원번호 FROM 직원)
    )
  );

-- 직원 수 상한 제약
CREATE ASSERTION 직원수_상한_제약
  CHECK (
    (SELECT COUNT(*) FROM 직원) <= 10000
  );
```

---

## 5. 새로운 데이터 타입

SQL-92는 SQL-89에 비해 데이터 타입을 대폭 확장했다.

### 전체 데이터 타입 분류

| 분류 | 데이터 타입 | 설명 |
|------|------------|------|
| 문자형 | CHAR(n) | 고정 길이 문자열 |
| | VARCHAR(n) | 가변 길이 문자열 |
| | NATIONAL CHAR(n) | 국가별 문자 세트 고정 길이 |
| | NATIONAL VARCHAR(n) | 국가별 문자 세트 가변 길이 |
| 숫자형 | SMALLINT | 작은 정수 |
| | INTEGER | 정수 |
| | NUMERIC(p,s) | 고정 소수점 |
| | DECIMAL(p,s) | 고정 소수점 |
| | FLOAT(p) | 부동 소수점 |
| | REAL | 단정밀도 부동 소수점 |
| | DOUBLE PRECISION | 배정밀도 부동 소수점 |
| 날짜/시간형 | DATE | 날짜 (YYYY-MM-DD) |
| | TIME | 시간 (HH:MM:SS) |
| | TIMESTAMP | 날짜+시간 |
| | TIME WITH TIME ZONE | 시간대 포함 시간 |
| | TIMESTAMP WITH TIME ZONE | 시간대 포함 날짜+시간 |
| 간격형 | INTERVAL YEAR TO MONTH | 연-월 간격 |
| | INTERVAL DAY TO SECOND | 일-초 간격 |
| 비트형 | BIT(n) | 고정 길이 비트열 |
| | BIT VARYING(n) | 가변 길이 비트열 |

### 5.1 날짜/시간 타입 (DATE, TIME, TIMESTAMP)

```sql
-- DATE: 날짜만 저장 (YYYY-MM-DD)
CREATE TABLE 일정 (
    일정번호    INTEGER PRIMARY KEY,
    시작일      DATE NOT NULL,
    종료일      DATE
);

INSERT INTO 일정 VALUES (1, DATE '2024-01-15', DATE '2024-01-20');

-- TIME: 시간만 저장 (HH:MM:SS)
CREATE TABLE 근무시간 (
    직원번호    INTEGER,
    출근시간    TIME NOT NULL,
    퇴근시간    TIME
);

INSERT INTO 근무시간 VALUES (101, TIME '09:00:00', TIME '18:00:00');

-- TIME WITH TIME ZONE
CREATE TABLE 국제회의 (
    회의번호    INTEGER PRIMARY KEY,
    시작시간    TIME WITH TIME ZONE
);

INSERT INTO 국제회의 VALUES (1, TIME '14:30:00+09:00');

-- TIMESTAMP: 날짜 + 시간
CREATE TABLE 로그 (
    로그번호    INTEGER PRIMARY KEY,
    발생시각    TIMESTAMP NOT NULL,
    메시지      VARCHAR(500)
);

INSERT INTO 로그 VALUES (1, TIMESTAMP '2024-01-15 09:30:45', '시스템 시작');

-- TIMESTAMP WITH TIME ZONE
CREATE TABLE 글로벌_이벤트 (
    이벤트번호  INTEGER PRIMARY KEY,
    발생시각    TIMESTAMP WITH TIME ZONE
);

INSERT INTO 글로벌_이벤트 VALUES (1, TIMESTAMP '2024-01-15 09:30:45+09:00');
```

### 5.2 INTERVAL 타입

```sql
-- INTERVAL YEAR TO MONTH: 연-월 간격
CREATE TABLE 계약 (
    계약번호    INTEGER PRIMARY KEY,
    계약기간    INTERVAL YEAR TO MONTH
);

INSERT INTO 계약 VALUES (1, INTERVAL '2-6' YEAR TO MONTH);  -- 2년 6개월

-- INTERVAL DAY TO SECOND: 일-초 간격
CREATE TABLE 작업 (
    작업번호    INTEGER PRIMARY KEY,
    소요시간    INTERVAL DAY TO SECOND
);

INSERT INTO 작업 VALUES (1, INTERVAL '3 04:30:00' DAY TO SECOND);  -- 3일 4시간 30분

-- INTERVAL 연산
SELECT 시작일 + INTERVAL '30' DAY AS 마감일
FROM 일정;

SELECT 시작일 + INTERVAL '3' MONTH AS 분기후
FROM 일정;

-- 두 날짜 간의 간격
SELECT (종료일 - 시작일) DAY AS 기간_일수
FROM 일정;
```

### 5.3 BIT 타입

```sql
-- BIT: 고정 길이 비트열
CREATE TABLE 권한 (
    사용자번호  INTEGER PRIMARY KEY,
    접근권한    BIT(8)
);

INSERT INTO 권한 VALUES (1, B'10110001');

-- BIT VARYING: 가변 길이 비트열
CREATE TABLE 설정 (
    설정번호    INTEGER PRIMARY KEY,
    플래그      BIT VARYING(32)
);

INSERT INTO 설정 VALUES (1, B'1011');
```

### 5.4 NATIONAL CHARACTER

국제화를 위한 국가별 문자 세트 지원.

```sql
-- NATIONAL CHARACTER: 국가별 문자 세트
CREATE TABLE 다국어_제품 (
    제품코드    CHAR(10) PRIMARY KEY,
    제품명_영문  VARCHAR(100),
    제품명_현지  NATIONAL CHARACTER VARYING(100)
);

-- 약식 표기
CREATE TABLE 다국어_고객 (
    고객번호    INTEGER PRIMARY KEY,
    이름        NCHAR VARYING(50),
    주소        NCHAR VARYING(200)
);

INSERT INTO 다국어_고객 VALUES (1, N'김철수', N'서울시 강남구');
```

---

## 6. JOIN 구문 혁신

SQL-92에서 가장 중요한 변경 중 하나는 명시적 JOIN 구문의 도입이다.

### 예제 테이블 구조

```sql
CREATE TABLE 부서 (
    부서코드    CHAR(4) PRIMARY KEY,
    부서명      VARCHAR(50) NOT NULL,
    위치        VARCHAR(100)
);

CREATE TABLE 직원 (
    직원번호    INTEGER PRIMARY KEY,
    이름        VARCHAR(50) NOT NULL,
    부서코드    CHAR(4) REFERENCES 부서(부서코드),
    급여        DECIMAL(10,2)
);

CREATE TABLE 프로젝트 (
    프로젝트코드 CHAR(6) PRIMARY KEY,
    프로젝트명   VARCHAR(100) NOT NULL,
    담당부서     CHAR(4) REFERENCES 부서(부서코드)
);

INSERT INTO 부서 VALUES ('D001', '개발팀', '서울');
INSERT INTO 부서 VALUES ('D002', '영업팀', '부산');
INSERT INTO 부서 VALUES ('D003', '인사팀', '서울');

INSERT INTO 직원 VALUES (101, '김개발', 'D001', 5000000);
INSERT INTO 직원 VALUES (102, '이영업', 'D002', 4500000);
INSERT INTO 직원 VALUES (103, '박인사', NULL, 4000000);

INSERT INTO 프로젝트 VALUES ('P00001', '시스템 개편', 'D001');
INSERT INTO 프로젝트 VALUES ('P00002', '해외 진출', 'D002');
INSERT INTO 프로젝트 VALUES ('P00003', '내부 감사', NULL);
```

### 6.1 INNER JOIN

```sql
-- SQL-89 방식 (구식)
SELECT 직원.이름, 부서.부서명
FROM 직원, 부서
WHERE 직원.부서코드 = 부서.부서코드;

-- SQL-92 INNER JOIN (ON 절)
SELECT 직원.이름, 부서.부서명
FROM 직원 INNER JOIN 부서
  ON 직원.부서코드 = 부서.부서코드;

-- 결과:
-- 이름      | 부서명
-- ----------|--------
-- 김개발    | 개발팀
-- 이영업    | 영업팀
```

### 6.2 LEFT OUTER JOIN

```sql
SELECT 직원.이름, 부서.부서명
FROM 직원 LEFT OUTER JOIN 부서
  ON 직원.부서코드 = 부서.부서코드;

-- 결과:
-- 이름      | 부서명
-- ----------|--------
-- 김개발    | 개발팀
-- 이영업    | 영업팀
-- 박인사    | NULL      ← 부서 미배정 직원도 포함
```

### 6.3 RIGHT OUTER JOIN

```sql
SELECT 직원.이름, 부서.부서명
FROM 직원 RIGHT OUTER JOIN 부서
  ON 직원.부서코드 = 부서.부서코드;

-- 결과:
-- 이름      | 부서명
-- ----------|--------
-- 김개발    | 개발팀
-- 이영업    | 영업팀
-- NULL      | 인사팀    ← 직원이 없는 부서도 포함
```

### 6.4 FULL OUTER JOIN

```sql
SELECT 직원.이름, 부서.부서명
FROM 직원 FULL OUTER JOIN 부서
  ON 직원.부서코드 = 부서.부서코드;

-- 결과:
-- 이름      | 부서명
-- ----------|--------
-- 김개발    | 개발팀
-- 이영업    | 영업팀
-- 박인사    | NULL
-- NULL      | 인사팀
```

### 6.5 CROSS JOIN

```sql
SELECT 직원.이름, 프로젝트.프로젝트명
FROM 직원 CROSS JOIN 프로젝트;

-- 결과: 직원 3명 x 프로젝트 3개 = 9행
```

### 6.6 NATURAL JOIN

```sql
-- 동일 이름 컬럼으로 자동 조인
SELECT 이름, 부서명
FROM 직원 NATURAL JOIN 부서;

-- NATURAL LEFT OUTER JOIN
SELECT 이름, 부서명
FROM 직원 NATURAL LEFT OUTER JOIN 부서;
```

### 6.7 USING 절

```sql
SELECT 이름, 부서명
FROM 직원 JOIN 부서 USING (부서코드);

-- 복수 컬럼 USING
SELECT *
FROM 테이블A JOIN 테이블B USING (컬럼1, 컬럼2);
```

### 6.8 다중 테이블 조인

```sql
SELECT 직원.이름, 부서.부서명, 프로젝트.프로젝트명
FROM 직원
  INNER JOIN 부서 ON 직원.부서코드 = 부서.부서코드
  INNER JOIN 프로젝트 ON 부서.부서코드 = 프로젝트.담당부서;

-- OUTER JOIN 혼합
SELECT 직원.이름, 부서.부서명, 프로젝트.프로젝트명
FROM 직원
  LEFT OUTER JOIN 부서 ON 직원.부서코드 = 부서.부서코드
  LEFT OUTER JOIN 프로젝트 ON 부서.부서코드 = 프로젝트.담당부서;
```

### JOIN 유형 비교 요약

| JOIN 유형 | 왼쪽 불일치 포함 | 오른쪽 불일치 포함 | 조건 지정 |
|-----------|:---:|:---:|------|
| INNER JOIN | X | X | ON / USING |
| LEFT OUTER JOIN | O | X | ON / USING |
| RIGHT OUTER JOIN | X | O | ON / USING |
| FULL OUTER JOIN | O | O | ON / USING |
| CROSS JOIN | - | - | 없음 (카테시안 곱) |
| NATURAL JOIN | X | X | 자동 (동명 컬럼) |

---

## 7. 서브쿼리 확장

### 7.1 스칼라 서브쿼리

```sql
-- SELECT 절에서 스칼라 서브쿼리
SELECT 이름,
       급여,
       (SELECT AVG(급여) FROM 직원) AS 평균급여,
       급여 - (SELECT AVG(급여) FROM 직원) AS 급여차이
FROM 직원;

-- WHERE 절에서 스칼라 서브쿼리
SELECT 이름, 급여
FROM 직원
WHERE 급여 > (SELECT AVG(급여) FROM 직원);
```

### 7.2 테이블 서브쿼리 (FROM 절)

```sql
SELECT 부서명, 평균급여
FROM (
    SELECT 부서.부서명, AVG(직원.급여) AS 평균급여
    FROM 직원 INNER JOIN 부서 ON 직원.부서코드 = 부서.부서코드
    GROUP BY 부서.부서명
) AS 부서별급여
WHERE 평균급여 > 4000000;
```

### 7.3 상관 서브쿼리

```sql
-- 각 부서에서 최고 급여 직원
SELECT 이름, 부서코드, 급여
FROM 직원 AS e1
WHERE 급여 = (
    SELECT MAX(급여)
    FROM 직원 AS e2
    WHERE e2.부서코드 = e1.부서코드
);

-- EXISTS를 사용한 상관 서브쿼리
SELECT 부서명
FROM 부서
WHERE EXISTS (
    SELECT 1 FROM 직원
    WHERE 직원.부서코드 = 부서.부서코드
      AND 직원.급여 > 5000000
);
```

### 7.4 비교 연산자와 서브쿼리

```sql
-- ANY / SOME
SELECT 이름, 급여
FROM 직원
WHERE 급여 > ANY (SELECT 급여 FROM 직원 WHERE 부서코드 = 'D002');

-- ALL
SELECT 이름, 급여
FROM 직원
WHERE 급여 > ALL (SELECT 급여 FROM 직원 WHERE 부서코드 = 'D002');

-- IN
SELECT 이름
FROM 직원
WHERE 부서코드 IN (SELECT 부서코드 FROM 부서 WHERE 위치 = '서울');

-- NOT IN
SELECT 이름
FROM 직원
WHERE 부서코드 NOT IN (SELECT 부서코드 FROM 부서 WHERE 위치 = '부산');
```

---

## 8. 집합 연산

### 8.1 UNION / UNION ALL

```sql
-- UNION: 중복 제거
SELECT 이름, '직원' AS 유형 FROM 직원
UNION
SELECT 부서명, '부서' AS 유형 FROM 부서;

-- UNION ALL: 중복 유지
SELECT 부서코드 FROM 직원
UNION ALL
SELECT 부서코드 FROM 부서;
```

### 8.2 INTERSECT

```sql
SELECT 부서코드 FROM 직원
INTERSECT
SELECT 부서코드 FROM 프로젝트;

-- INTERSECT ALL
SELECT 부서코드 FROM 직원
INTERSECT ALL
SELECT 부서코드 FROM 프로젝트;
```

### 8.3 EXCEPT

```sql
SELECT 부서코드 FROM 부서
EXCEPT
SELECT 부서코드 FROM 직원;

-- EXCEPT ALL
SELECT 부서코드 FROM 부서
EXCEPT ALL
SELECT 부서코드 FROM 직원;
```

### 8.4 CORRESPONDING

```sql
-- 공통 컬럼만 사용하여 집합 연산
SELECT * FROM 직원
UNION CORRESPONDING
SELECT * FROM 관리자;

-- 특정 컬럼 지정
SELECT * FROM 직원
UNION CORRESPONDING BY (이름, 부서코드)
SELECT * FROM 관리자;
```

### 집합 연산 비교

| 연산 | 중복 처리 | 설명 |
|------|-----------|------|
| UNION | 제거 | 합집합 |
| UNION ALL | 유지 | 합집합 (전체) |
| INTERSECT | 제거 | 교집합 |
| INTERSECT ALL | 유지 | 교집합 (전체) |
| EXCEPT | 제거 | 차집합 |
| EXCEPT ALL | 유지 | 차집합 (전체) |

---

## 9. CASE 표현식

### 9.1 단순 CASE (Simple CASE)

```sql
SELECT 이름,
       부서코드,
       CASE 부서코드
           WHEN 'D001' THEN '개발팀'
           WHEN 'D002' THEN '영업팀'
           WHEN 'D003' THEN '인사팀'
           ELSE '미배정'
       END AS 부서명
FROM 직원;
```

### 9.2 검색 CASE (Searched CASE)

```sql
SELECT 이름,
       급여,
       CASE
           WHEN 급여 >= 5000000 THEN '고급'
           WHEN 급여 >= 4000000 THEN '중급'
           WHEN 급여 >= 3000000 THEN '초급'
           ELSE '인턴'
       END AS 등급
FROM 직원;

-- ORDER BY에서 사용
SELECT 이름, 부서코드
FROM 직원
ORDER BY CASE 부서코드
    WHEN 'D001' THEN 1
    WHEN 'D002' THEN 2
    ELSE 3
END;

-- UPDATE에서 사용
UPDATE 직원
SET 급여 = CASE
    WHEN 부서코드 = 'D001' THEN 급여 * 1.10
    WHEN 부서코드 = 'D002' THEN 급여 * 1.05
    ELSE 급여 * 1.03
END;

-- 집계 함수와 함께
SELECT 부서코드,
       COUNT(CASE WHEN 급여 >= 5000000 THEN 1 END) AS 고급인원,
       COUNT(CASE WHEN 급여 < 5000000 THEN 1 END) AS 일반인원
FROM 직원
GROUP BY 부서코드;
```

### 9.3 NULLIF

```sql
-- 두 값이 같으면 NULL 반환
SELECT NULLIF(급여, 0) AS 급여
FROM 직원;

-- NULLIF(a, b) = CASE WHEN a = b THEN NULL ELSE a END

-- 0으로 나누기 방지
SELECT 총매출 / NULLIF(거래건수, 0) AS 건당매출
FROM 매출요약;
```

### 9.4 COALESCE

```sql
-- 첫 번째 비NULL 값 반환
SELECT 이름,
       COALESCE(휴대폰, 사무실전화, 자택전화, '연락처 없음') AS 연락처
FROM 직원;

-- COALESCE(a, b, c) =
-- CASE WHEN a IS NOT NULL THEN a
--      WHEN b IS NOT NULL THEN b
--      ELSE c END
```

---

## 10. CAST 변환

```sql
-- 정수를 문자열로
SELECT CAST(직원번호 AS VARCHAR(10)) AS 직원번호_문자
FROM 직원;

-- 문자열을 정수로
SELECT CAST('12345' AS INTEGER) AS 숫자값;

-- 문자열을 날짜로
SELECT CAST('2024-01-15' AS DATE) AS 날짜값;

-- 숫자를 소수점 포함 숫자로
SELECT CAST(급여 AS DECIMAL(12, 2)) AS 급여_소수점
FROM 직원;

-- 날짜를 문자열로
SELECT CAST(입사일 AS VARCHAR(10)) AS 입사일_문자
FROM 직원;

-- TIMESTAMP를 DATE로
SELECT CAST(CURRENT_TIMESTAMP AS DATE) AS 오늘날짜;
```

### CAST 변환 가능 매트릭스

| FROM \ TO | CHAR | VARCHAR | INTEGER | DECIMAL | FLOAT | DATE | TIME | TIMESTAMP |
|-----------|:----:|:-------:|:-------:|:-------:|:-----:|:----:|:----:|:---------:|
| CHAR | O | O | O | O | O | O | O | O |
| VARCHAR | O | O | O | O | O | O | O | O |
| INTEGER | O | O | O | O | O | X | X | X |
| DECIMAL | O | O | O | O | O | X | X | X |
| FLOAT | O | O | O | O | O | X | X | X |
| DATE | O | O | X | X | X | O | X | O |
| TIME | O | O | X | X | X | X | O | O |
| TIMESTAMP | O | O | X | X | X | O | O | O |

---

## 11. 문자열 함수

### 11.1 SUBSTRING

```sql
SELECT SUBSTRING('Hello World' FROM 1 FOR 5);   -- 'Hello'
SELECT SUBSTRING('Hello World' FROM 7 FOR 5);   -- 'World'
SELECT SUBSTRING('Hello World' FROM 7);          -- 'World'

SELECT 이름, SUBSTRING(이름 FROM 1 FOR 1) AS 성
FROM 직원;
```

### 11.2 UPPER / LOWER

```sql
SELECT UPPER('hello world');  -- 'HELLO WORLD'
SELECT LOWER('HELLO WORLD');  -- 'hello world'

-- 대소문자 무시 비교
SELECT 이름 FROM 직원
WHERE UPPER(이름) = UPPER('김개발');
```

### 11.3 TRIM

```sql
SELECT TRIM('  Hello  ');                        -- 'Hello'
SELECT TRIM(LEADING FROM '  Hello  ');           -- 'Hello  '
SELECT TRIM(TRAILING FROM '  Hello  ');          -- '  Hello'
SELECT TRIM(BOTH FROM '  Hello  ');              -- 'Hello'
SELECT TRIM(LEADING '0' FROM '000123');          -- '123'
SELECT TRIM(BOTH '*' FROM '*test*');         -- 'test'
```

### 11.4 POSITION

```sql
SELECT POSITION('World' IN 'Hello World');       -- 7
SELECT POSITION('xyz' IN 'Hello World');         -- 0 (찾지 못함)
```

### 11.5 CHAR_LENGTH / OCTET_LENGTH

```sql
SELECT CHAR_LENGTH('Hello');       -- 5 (문자 수)
SELECT CHARACTER_LENGTH('Hello');  -- 5 (동일)
SELECT OCTET_LENGTH('Hello');      -- 5 (바이트 수, ASCII 기준)
```

### 11.6 문자열 연결 (||)

```sql
SELECT 이름 || ' (' || 부서코드 || ')' AS 직원정보
FROM 직원;
-- 결과: '김개발 (D001)'
```

### 문자열 함수 요약

| 함수 | 설명 | 예시 | 결과 |
|------|------|------|------|
| SUBSTRING | 부분 문자열 | SUBSTRING('Hello' FROM 2 FOR 3) | 'ell' |
| UPPER | 대문자 변환 | UPPER('hello') | 'HELLO' |
| LOWER | 소문자 변환 | LOWER('HELLO') | 'hello' |
| TRIM | 공백/문자 제거 | TRIM('  hi  ') | 'hi' |
| POSITION | 위치 찾기 | POSITION('l' IN 'hello') | 3 |
| CHAR_LENGTH | 문자 수 | CHAR_LENGTH('hello') | 5 |
| OCTET_LENGTH | 바이트 수 | OCTET_LENGTH('hello') | 5 |
| \|\| | 연결 | 'a' \|\| 'b' | 'ab' |

---

## 12. 스크롤 가능 커서

```sql
-- 스크롤 가능 커서 선언
DECLARE 직원_커서 SCROLL CURSOR FOR
    SELECT 직원번호, 이름, 급여
    FROM 직원
    ORDER BY 급여 DESC;

OPEN 직원_커서;

-- 다양한 FETCH 방향
FETCH NEXT FROM 직원_커서;          -- 다음 행
FETCH PRIOR FROM 직원_커서;         -- 이전 행
FETCH FIRST FROM 직원_커서;         -- 첫 번째 행
FETCH LAST FROM 직원_커서;          -- 마지막 행
FETCH ABSOLUTE 5 FROM 직원_커서;    -- 5번째 행
FETCH RELATIVE -2 FROM 직원_커서;   -- 현재에서 2행 뒤로
FETCH RELATIVE 3 FROM 직원_커서;    -- 현재에서 3행 앞으로

CLOSE 직원_커서;

-- 감도 옵션
DECLARE 민감_커서 SENSITIVE SCROLL CURSOR FOR
    SELECT * FROM 직원;

DECLARE 비민감_커서 INSENSITIVE SCROLL CURSOR FOR
    SELECT * FROM 직원;
```

| FETCH 방향 | 설명 |
|-----------|------|
| NEXT | 다음 행으로 이동 |
| PRIOR | 이전 행으로 이동 |
| FIRST | 첫 번째 행으로 이동 |
| LAST | 마지막 행으로 이동 |
| ABSOLUTE n | n번째 행으로 이동 |
| RELATIVE n | 현재 위치에서 n행 이동 |

---

## 13. 트랜잭션 격리 수준

```sql
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 읽기 전용/쓰기 트랜잭션
SET TRANSACTION READ ONLY;
SET TRANSACTION READ WRITE;

-- 복합 설정
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE, READ ONLY;
```

### 격리 수준별 현상 허용 여부

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|-----------|:----------:|:-------------------:|:------------:|
| READ UNCOMMITTED | 가능 | 가능 | 가능 |
| READ COMMITTED | 불가 | 가능 | 가능 |
| REPEATABLE READ | 불가 | 불가 | 가능 |
| SERIALIZABLE | 불가 | 불가 | 불가 |

### 현상 설명

- Dirty Read: 다른 트랜잭션이 커밋하지 않은 데이터를 읽음. 롤백 시 무효 데이터가 됨
- Non-Repeatable Read: 같은 행을 두 번 읽었을 때 다른 값을 얻음 (중간에 UPDATE/DELETE 발생)
- Phantom Read: 같은 쿼리를 두 번 실행했을 때 다른 행 집합을 얻음 (중간에 INSERT 발생)

---

## 14. INFORMATION_SCHEMA

### 주요 뷰 목록

| 뷰 이름 | 설명 |
|---------|------|
| SCHEMATA | 스키마 목록 |
| TABLES | 테이블 목록 |
| COLUMNS | 컬럼 목록 |
| TABLE_CONSTRAINTS | 테이블 제약사항 |
| REFERENTIAL_CONSTRAINTS | 참조 제약사항 |
| KEY_COLUMN_USAGE | 키에 사용된 컬럼 |
| CHECK_CONSTRAINTS | CHECK 제약사항 |
| VIEWS | 뷰 목록 |
| VIEW_TABLE_USAGE | 뷰에서 참조하는 테이블 |
| VIEW_COLUMN_USAGE | 뷰에서 참조하는 컬럼 |
| DOMAINS | 도메인 목록 |
| DOMAIN_CONSTRAINTS | 도메인 제약사항 |
| ASSERTIONS | 어서션 목록 |
| TABLE_PRIVILEGES | 테이블 권한 |
| COLUMN_PRIVILEGES | 컬럼 권한 |

### 사용 예시

```sql
-- 모든 테이블 조회
SELECT TABLE_NAME, TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'PUBLIC';

-- 컬럼 정보 조회
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH,
       IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = '직원'
ORDER BY ORDINAL_POSITION;

-- 외래 키 조회
SELECT TC.CONSTRAINT_NAME, TC.TABLE_NAME, KCU.COLUMN_NAME,
       RC.UNIQUE_CONSTRAINT_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC
  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU
    ON TC.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME
  JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC
    ON TC.CONSTRAINT_NAME = RC.CONSTRAINT_NAME
WHERE TC.CONSTRAINT_TYPE = 'FOREIGN KEY';

-- 뷰 정의 조회
SELECT TABLE_NAME, VIEW_DEFINITION
FROM INFORMATION_SCHEMA.VIEWS
WHERE TABLE_SCHEMA = 'PUBLIC';
```

---

## 15. 진단

### 15.1 SQLSTATE 코드

| 클래스 (앞 2자리) | 의미 |
|-------------------|------|
| 00 | 성공 완료 |
| 01 | 경고 |
| 02 | 데이터 없음 |
| 07 | 동적 SQL 오류 |
| 08 | 연결 예외 |
| 0A | 미지원 기능 |
| 21 | 카디널리티 위반 |
| 22 | 데이터 예외 |
| 23 | 무결성 제약 위반 |
| 24 | 잘못된 커서 상태 |
| 25 | 잘못된 트랜잭션 상태 |
| 40 | 트랜잭션 롤백 |
| 42 | 구문 오류 또는 접근 위반 |

### 15.2 GET DIAGNOSTICS

```sql
-- 기본 진단 정보
GET DIAGNOSTICS
    :행수 = ROW_COUNT,
    :조건수 = NUMBER;

-- 조건별 상세 진단
GET DIAGNOSTICS EXCEPTION 1
    :상태코드 = RETURNED_SQLSTATE,
    :메시지 = MESSAGE_TEXT,
    :테이블명 = TABLE_NAME,
    :컬럼명 = COLUMN_NAME,
    :제약명 = CONSTRAINT_NAME;
```

---

## 16. 동적 SQL

```sql
-- SQL 문 준비
PREPARE 동적문장 FROM 'SELECT * FROM 직원 WHERE 부서코드 = ?';

-- 준비된 문장 실행
EXECUTE 동적문장 USING :부서코드_변수;

-- 즉시 실행
EXECUTE IMMEDIATE 'DELETE FROM 임시_테이블';

-- 설명자 관리
ALLOCATE DESCRIPTOR '설명자1' WITH MAX 20;

DESCRIBE 동적문장 INTO SQL DESCRIPTOR '설명자1';

GET DESCRIPTOR '설명자1' :항목수 = COUNT;

GET DESCRIPTOR '설명자1' VALUE 1
    :타입 = TYPE,
    :이름 = NAME,
    :길이 = LENGTH,
    :널가능 = NULLABLE;

SET DESCRIPTOR '설명자1' VALUE 1
    TYPE = 1,
    LENGTH = 50,
    DATA = :값_변수;

-- 동적 커서
DECLARE 동적커서 CURSOR FOR 동적문장;
OPEN 동적커서 USING SQL DESCRIPTOR '입력_설명자';
FETCH 동적커서 INTO SQL DESCRIPTOR '출력_설명자';
CLOSE 동적커서;

DEALLOCATE PREPARE 동적문장;
DEALLOCATE DESCRIPTOR '설명자1';
```

---

## 17. 연결 관리

```sql
-- 데이터베이스 연결
CONNECT TO 'mydb' AS 연결1 USER '사용자명';
CONNECT TO DEFAULT;

-- 연결 전환
SET CONNECTION 연결1;
SET CONNECTION DEFAULT;

-- 연결 해제
DISCONNECT 연결1;
DISCONNECT CURRENT;
DISCONNECT ALL;
```

---

## 18. 임시 테이블

```sql
-- 전역 임시 테이블 (커밋 시 삭제)
CREATE GLOBAL TEMPORARY TABLE 임시_계산결과 (
    항목번호    INTEGER,
    계산값      DECIMAL(12, 2),
    계산일시    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ON COMMIT DELETE ROWS;

-- 전역 임시 테이블 (커밋 후 유지)
CREATE GLOBAL TEMPORARY TABLE 세션_작업대 (
    작업번호    INTEGER,
    작업내용    VARCHAR(500)
) ON COMMIT PRESERVE ROWS;

-- 로컬 임시 테이블
CREATE LOCAL TEMPORARY TABLE 지역_임시 (
    항목번호    INTEGER,
    값          VARCHAR(100)
) ON COMMIT DELETE ROWS;
```

| 옵션 | 범위 | DELETE ROWS | PRESERVE ROWS |
|------|------|:-----------:|:-------------:|
| GLOBAL TEMPORARY | 모든 세션 참조 가능 | 커밋 시 삭제 | 세션 종료까지 유지 |
| LOCAL TEMPORARY | 현재 모듈만 | 커밋 시 삭제 | 세션 종료까지 유지 |

---

## 19. 제약사항과 한계

| 미지원 기능 | 설명 |
|------------|------|
| 재귀 쿼리 | CTE, WITH RECURSIVE 없음 |
| 윈도우 함수 | ROW_NUMBER, RANK 등 없음 |
| 정규 표현식 | SIMILAR TO, LIKE_REGEX 없음 |
| 트리거 | CREATE TRIGGER 없음 |
| 저장 프로시저/함수 | CREATE PROCEDURE/FUNCTION 없음 |
| 사용자 정의 타입 | UDT 없음 |
| BLOB/CLOB | 대용량 객체 타입 없음 |
| BOOLEAN | TRUE/FALSE 리터럴 없음 |
| 배열 | ARRAY 타입 없음 |
| 시퀀스 | CREATE SEQUENCE 없음 |
| MERGE | UPSERT 기능 없음 |
| OLAP | ROLLUP, CUBE 없음 |
| JSON/XML | 없음 |

---

## 20. 영향과 의의

1. SQL의 실질적 표준 확립: 현재까지도 대부분의 SQL 교재와 교육이 SQL-92를 기반으로 한다.
2. JOIN 구문의 혁신: 명시적 JOIN 구문은 SQL의 가독성과 유지보수성을 크게 향상시켰다.
3. CASE 표현식의 도입: 조건부 논리를 SQL 내에서 직접 처리할 수 있게 되었다.
4. 날짜/시간 타입 표준화: 이식성이 크게 향상되었다.
5. INFORMATION_SCHEMA: 메타데이터 조회가 표준화되었다.
6. 트랜잭션 격리 수준 표준화: 동시성 제어에 대한 표준 모델을 제공했다.

SQL-92에서 정의된 기본 문법과 개념은 이후 모든 SQL 표준의 토대가 되었으며, SQL:1999에서 객체-관계형 기능이 추가되면서 더욱 확장되었다.

---

> 참고: SQL-92 표준의 공식 문서는 ISO/IEC 9075:1992로, ISO에서 구매할 수 있다.
