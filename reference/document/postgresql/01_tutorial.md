# PostgreSQL 18 튜토리얼

이 문서는 PostgreSQL 18.1 공식 문서의 "Part I. Tutorial" 챕터를 한국어로 번역한 것입니다.

## 개요

PostgreSQL 튜토리얼은 다음을 목표로 합니다:
- PostgreSQL과 관계형 데이터베이스 개념 소개
- SQL 언어 학습
- PostgreSQL 시스템을 통한 실습 경험 제공
- Unix나 프로그래밍에 대한 사전 경험 불필요

---

# 1장. 시작하기

## 1.1. 설치

PostgreSQL을 사용하기 전에 먼저 설치해야 합니다. PostgreSQL이 이미 시스템에 설치되어 있을 수도 있습니다. 운영체제 배포판에 포함되어 있거나 시스템 관리자가 이미 설치했을 수 있습니다.

### 사전 요구사항

- 루트 권한 불필요: PostgreSQL은 일반 사용자 권한으로 설치할 수 있습니다. root 접근이 필요하지 않습니다.
- 기존 설치 확인: 설치하기 전에 PostgreSQL이 이미 시스템에 설치되어 있는지 확인하세요.

### 설치 옵션

1. PostgreSQL이 이미 설치된 경우
   - 운영체제 문서나 시스템 관리자로부터 정보를 얻으세요.
   - 기존 PostgreSQL 설치에 어떻게 접근하는지 문의하세요.

2. 직접 설치하는 경우
   - 소스 코드로부터의 자세한 설치 방법은 17장을 참조하세요.
   - 적절한 환경 변수 설정 섹션을 주의 깊게 따르세요.
   - 설치가 완료되면 시작하기 가이드로 돌아오세요.

### 환경 설정

사이트 관리자가 기본 방식으로 설정하지 않은 경우, 환경 변수를 구성해야 할 수 있습니다:

- `PGHOST`: 데이터베이스 서버가 원격 머신에 있는 경우 이 변수를 서버 머신 이름으로 설정하세요.
- `PGPORT`: 구성에 따라 설정이 필요할 수 있습니다.

### 문제 해결

애플리케이션이 데이터베이스에 연결할 수 없는 경우:
1. 사이트 관리자에게 문의하세요.
2. 문서를 검토하여 환경 설정이 올바른지 확인하세요.
3. 필요한 모든 환경 변수가 적절하게 구성되어 있는지 확인하세요.

---

## 1.2. 아키텍처 기초

PostgreSQL은 클라이언트/서버 모델 을 사용하며, 여러 협력 프로세스가 함께 작동하여 데이터베이스 작업을 관리합니다.

### 주요 구성 요소

#### 1. 서버 프로세스 (`postgres`)
- 데이터베이스 파일을 관리합니다.
- 클라이언트 애플리케이션의 연결을 수락합니다.
- 클라이언트를 대신하여 데이터베이스 작업을 수행합니다.
- 슈퍼바이저 프로세스로 지속적으로 실행되며 들어오는 연결을 기다립니다.

#### 2. 클라이언트 (프론트엔드) 애플리케이션
- 데이터베이스 작업을 수행하는 사용자 애플리케이션입니다.
- 다양한 형태일 수 있습니다:
  - 텍스트 기반 도구
  - 그래픽 애플리케이션
  - 데이터베이스에 접근하는 웹 서버
  - 특수 데이터베이스 유지보수 도구
- PostgreSQL과 함께 제공되는 것도 있고, 대부분은 사용자가 개발합니다.

### 작동 방식

#### 연결 처리
- 슈퍼바이저 `postgres` 프로세스가 클라이언트 연결을 대기합니다.
- 클라이언트가 연결하면 서버가 해당 연결을 위한 새 프로세스를 fork 합니다.
- 클라이언트와 새 서버 프로세스는 원래 `postgres` 프로세스 없이 직접 통신합니다.
- 여러 동시 연결이 이 방식으로 처리됩니다.

#### 네트워크 통신
- 클라이언트와 서버는 다른 호스트에서 실행될 수 있습니다.
- 통신은 TCP/IP 네트워크 연결 을 통해 이루어집니다.
- 중요한 고려사항: 클라이언트 머신에서 접근 가능한 파일이 서버 머신에서는 접근 불가능하거나 다른 파일 경로가 필요할 수 있습니다.

### 핵심 요점
PostgreSQL의 아키텍처는 사용자에게 투명합니다. 슈퍼바이저 프로세스가 지속적으로 연결을 관리하면서 개별 클라이언트-서버 쌍이 각자의 작업을 독립적으로 처리합니다.

---

## 1.3. 데이터베이스 생성

### 기본 명령

커맨드 라인에서 새 데이터베이스를 생성하려면 `createdb` 명령을 사용합니다. 예를 들어, `mydb`라는 데이터베이스를 생성하려면:

```bash
$ createdb mydb
```

명령이 응답 없이 실행되면 데이터베이스가 성공적으로 생성된 것입니다.

### 사용자 이름으로 데이터베이스 생성

현재 사용자 계정과 같은 이름의 데이터베이스를 생성할 수도 있습니다:

```bash
$ createdb
```

많은 도구가 기본적으로 사용자 이름과 일치하는 데이터베이스 이름을 가정하기 때문에 이 방법이 편리합니다.

### 데이터베이스 삭제

데이터베이스를 삭제하려면 (소유자인 경우):

```bash
$ dropdb mydb
```

경고: 이 작업은 데이터베이스와 관련된 모든 파일을 물리적으로 제거하며 되돌릴 수 없습니다.

### 데이터베이스 명명 규칙

- 데이터베이스 이름은 알파벳 문자로 시작해야 합니다.
- 길이는 63바이트로 제한됩니다.
- PostgreSQL은 주어진 사이트에서 원하는 만큼 많은 데이터베이스를 생성할 수 있습니다.

### 일반적인 오류와 해결 방법

| 오류 | 원인 | 해결 방법 |
|------|------|----------|
| `createdb: command not found` | PostgreSQL이 설치되지 않았거나 PATH에 없음 | 절대 경로 사용 또는 설치 확인 |
| `connection to server on socket failed` | PostgreSQL 서버가 실행되지 않음 | PostgreSQL 서버 시작 |
| `role "joe" does not exist` | 사용자를 위한 PostgreSQL 사용자 계정이 생성되지 않음 | 관리자가 계정을 생성하거나 `-U` 플래그 사용 |
| `permission denied to create database` | 사용자에게 데이터베이스 생성 권한이 없음 | 관리자가 권한 부여 필요 |

---

## 1.4. 데이터베이스 접근

데이터베이스를 생성한 후, 세 가지 주요 방법으로 접근할 수 있습니다:

1. PostgreSQL 대화형 터미널 (`psql`) - SQL 명령의 대화형 입력, 편집 및 실행을 허용합니다.
2. 그래픽 프론트엔드 도구 - pgAdmin이나 ODBC/JDBC를 지원하는 오피스 스위트 등
3. 사용자 정의 애플리케이션 - 사용 가능한 언어 바인딩 사용 (문서 Part IV에서 다룸)

### psql 시작

`mydb` 데이터베이스에 접근하려면:

```bash
$ psql mydb
```

참고: 데이터베이스 이름을 제공하지 않으면 기본적으로 사용자 계정 이름으로 설정됩니다.

### 초기 연결

연결하면 다음과 같이 표시됩니다:

```
psql (18.1)
Type "help" for help.

mydb=>
```

또는:

```
mydb=#
```

`#` 프롬프트는 데이터베이스 슈퍼유저 로 연결되어 있음을 나타냅니다 (일반적으로 PostgreSQL을 직접 설치한 경우). 이는 접근 제어를 받지 않음을 의미합니다.

### 주요 psql 명령

- 내부 명령 - 백슬래시(`\`)로 시작
  - `\?` - 내부 명령에 대한 도움말 얻기
  - `\h` - SQL 명령 구문에 대한 도움말 얻기

- psql 종료 - `\q` 입력 또는 Ctrl+D 누르기

---

# 2장. SQL 언어

## 2.1. 소개

이 섹션은 PostgreSQL에서 간단한 작업을 수행하기 위해 SQL을 사용하는 방법에 대한 소개를 제공합니다. 이것은 기초 튜토리얼이지 완전한 SQL 가이드가 아닙니다.

### 범위

- 튜토리얼은 SQL 기초만 소개합니다.
- 완전한 SQL 튜토리얼이 아닙니다 (포괄적인 내용은 "Understanding the New SQL"이나 "A Guide to the SQL Standard" 같은 책을 참조하세요).
- PostgreSQL에는 SQL 표준의 확장인 일부 언어 기능이 포함되어 있습니다.

### 사전 요구사항

- 이전 장에서 `mydb`라는 데이터베이스를 생성했어야 합니다.
- `psql`이 시작되어 사용 준비가 되어 있어야 합니다.

### 튜토리얼 파일 작업

튜토리얼 예제는 PostgreSQL 소스 배포판의 `src/tutorial/`에 포함되어 있습니다 (참고: 바이너리 배포판에는 이 파일들이 포함되지 않을 수 있습니다).

설정 단계:
```bash
$ cd src/tutorial/
$ make
```

이것은 사용자 정의 함수와 타입을 포함하는 스크립트와 C 파일을 컴파일합니다.

튜토리얼 실행:
```bash
$ psql -s -f basics.sql
```

주요 명령:
- `\i` - 지정된 파일에서 명령을 읽습니다.
- `-s` 플래그 - 단일 단계 모드를 활성화하여 각 문을 서버로 보내기 전에 일시 중지합니다.
- 튜토리얼 명령은 `basics.sql`에 있습니다.

---

## 2.2. 개념

PostgreSQL은 관계형 데이터베이스 관리 시스템(RDBMS) 입니다. 이것은 관계(relation)에 저장된 데이터를 관리하는 시스템으로, 관계는 본질적으로 테이블 을 의미하는 수학적 용어입니다.

### 주요 개념

#### 테이블
- 테이블은 행의 명명된 컬렉션 입니다.
- 각 행은 같은 이름의 열 집합 을 포함합니다.
- 각 열은 특정 데이터 타입 을 가집니다.
- 열은 각 행 내에서 고정된 순서를 가집니다.
- 중요: SQL은 테이블 내 행의 순서를 보장하지 않습니다 (하지만 행은 표시를 위해 명시적으로 정렬할 수 있습니다).

#### 데이터베이스 조직 계층
PostgreSQL은 다음 구조로 데이터를 구성합니다:
1. 열(Column) - 특정 데이터 타입을 가진 명명된 필드
2. 행(Row) - 열 값의 컬렉션
3. 테이블(Table) - 일관된 구조를 가진 행의 명명된 컬렉션
4. 데이터베이스(Database) - 테이블의 그룹
5. 데이터베이스 클러스터(Database Cluster) - 단일 PostgreSQL 서버 인스턴스가 관리하는 데이터베이스 모음

### 역사적 맥락

관계형 데이터베이스 모델(테이블 사용)은 현재 일반적이지만, 다른 데이터베이스 구성 방법도 존재합니다:
- 계층적 데이터베이스 - Unix 계열 운영체제의 파일과 디렉토리와 같은 구조
- 객체 지향 데이터베이스 - 더 현대적인 개발 방식

---

## 2.3. 새 테이블 생성

### 기본 구문

PostgreSQL에서 새 테이블을 생성하려면, 테이블 이름과 모든 열 이름 및 데이터 타입을 지정합니다:

```sql
CREATE TABLE weather (
    city            varchar(80),
    temp_lo         int,           -- 최저 온도
    temp_hi         int,           -- 최고 온도
    prcp            real,          -- 강수량
    date            date
);
```

### 주요 포인트

- 줄 바꿈: SQL 명령에서 줄 바꿈을 자유롭게 사용할 수 있습니다. `psql`은 세미콜론까지 명령이 종료되지 않았음을 인식합니다.
- 공백: SQL 명령에서 공백, 탭, 줄 바꿈을 자유롭게 사용할 수 있습니다.
- 주석: 두 개의 대시(`--`)는 줄 끝까지 무시되는 주석을 도입합니다.
- 대소문자 구분: SQL은 키워드와 식별자에 대해 대소문자를 구분하지 않습니다. 단, 대소문자를 유지하기 위해 쌍따옴표로 묶인 식별자는 예외입니다.

### 데이터 타입

PostgreSQL은 다음을 포함한 표준 SQL 타입을 지원합니다:

| 타입 | 설명 |
|------|------|
| `int` | 일반 정수 타입 |
| `smallint` | 작은 정수 |
| `real` | 단정밀도 부동소수점 숫자 |
| `double precision` | 배정밀도 부동소수점 숫자 |
| `varchar(N)` | 최대 N자의 가변 길이 문자열 |
| `char(N)` | 고정 길이 문자열 |
| `date` | 날짜 값 |
| `time` | 시간 값 |
| `timestamp` | 날짜와 시간 값 |
| `interval` | 시간 간격 |

PostgreSQL은 지리적 데이터를 위한 `point`와 같은 PostgreSQL 전용 타입도 지원합니다:

```sql
CREATE TABLE cities (
    name            varchar(80),
    location        point
);
```

### 테이블 삭제

더 이상 필요하지 않은 테이블을 제거하려면:

```sql
DROP TABLE tablename;
```

---

## 2.4. 테이블에 행 채우기

### INSERT 문

`INSERT` 문은 테이블에 행을 채우는 데 사용됩니다. 주요 접근 방식은 다음과 같습니다:

#### 기본 구문
열 순서대로 값을 삽입:
```sql
INSERT INTO weather VALUES ('San Francisco', 46, 50, 0.25, '1994-11-27');
```

#### 명시적 열 나열 (권장)
더 나은 스타일과 유연성을 위해 열을 명시적으로 나열:
```sql
INSERT INTO weather (city, temp_lo, temp_hi, prcp, date)
    VALUES ('San Francisco', 43, 57, 0.0, '1994-11-29');
```

장점:
- 열을 어떤 순서로든 나열할 수 있습니다.
- 필요하지 않은 열은 생략할 수 있습니다.

#### 열 생략 예제
```sql
INSERT INTO weather (date, city, temp_hi, temp_lo)
    VALUES ('1994-11-29', 'Hayward', 54, 37);
```

### 특수 데이터 타입 고려사항

- 문자열/텍스트 값: 작은따옴표(`'`)로 감쌉니다.
- 날짜 타입: 유연한 입력이 가능하지만, 모호하지 않은 형식을 사용하세요.
- Point 타입: 좌표 쌍 형식이 필요합니다.
  ```sql
  INSERT INTO cities VALUES ('San Francisco', '(-194.0, 53.0)');
  ```

### 대량 로딩을 위한 COPY 명령

플랫 텍스트 파일에서 대량의 데이터를 로드하려면:
```sql
COPY weather FROM '/home/user/weather.txt';
```

주요 포인트:
- 일반적으로 대량 작업에서 `INSERT`보다 빠릅니다.
- 파일 경로는 백엔드 서버 에서 접근 가능해야 합니다 (클라이언트가 아님).
- 기본적으로 탭으로 구분된 값 형식입니다.

#### 파일 형식 예제
```
San Francisco    46    50    0.25    1994-11-27
San Francisco    43    57    0.0    1994-11-29
Hayward          37    54    \N     1994-11-29
```
(NULL 값을 나타내려면 `\N`을 사용합니다)

---

## 2.5. 테이블 쿼리

### 개요
PostgreSQL에서 테이블의 데이터를 검색하려면 SQL `SELECT` 문을 사용합니다. 이 문은 세 가지 주요 부분으로 구성됩니다:
- 선택 목록: 반환할 열
- 테이블 목록: 데이터를 검색할 테이블
- 조건 (선택 사항): 반환할 행에 대한 제한

### 기본 SELECT 예제

#### 모든 행과 열 검색
```sql
SELECT * FROM weather;
```

또는 열을 명시적으로 나열:
```sql
SELECT city, temp_lo, temp_hi, prcp, date FROM weather;
```

#### 표현식과 열 별칭 사용
```sql
SELECT city, (temp_hi+temp_lo)/2 AS temp_avg, date FROM weather;
```

`AS` 절은 출력 열의 이름을 변경합니다 (선택적 구문).

### WHERE 절을 사용한 필터링

반환할 행을 지정하기 위해 `AND`, `OR`, `NOT`을 사용한 Boolean 표현식으로 `WHERE` 절을 추가합니다:

```sql
SELECT * FROM weather
    WHERE city = 'San Francisco' AND prcp > 0.0;
```

### 결과 정렬

정렬된 순서로 결과를 반환하려면 `ORDER BY`를 사용합니다:

```sql
SELECT * FROM weather
    ORDER BY city;
```

다중 열 정렬의 경우:
```sql
SELECT * FROM weather
    ORDER BY city, temp_lo;
```

### 중복 제거

중복 행을 제거하려면 `DISTINCT`를 사용합니다:

```sql
SELECT DISTINCT city FROM weather;
```

일관된 결과를 위해 `ORDER BY`와 결합:
```sql
SELECT DISTINCT city FROM weather
    ORDER BY city;
```

참고: 현재 PostgreSQL에서 `DISTINCT`만으로는 순서를 보장하지 않습니다. 일관된 결과가 필요하면 `ORDER BY`를 명시적으로 사용하세요.

---

## 2.6. 테이블 간 조인

### 개요
조인은 쿼리가 지정된 조건을 기반으로 한 테이블의 행을 다른 테이블의 행과 결합하여 여러 테이블에 동시에 접근할 수 있게 합니다.

### 조인 유형

#### 1. 내부 조인 (명시적 구문)
조인 조건이 일치하는 행을 결합합니다. 일치하지 않는 행은 제외됩니다.

```sql
SELECT * FROM weather JOIN cities ON city = name;
```

결과: 두 테이블 모두에 일치하는 항목이 있는 행만 반환합니다.

#### 2. 내부 조인 (암시적/레거시 구문)
`JOIN`/`ON` 대신 `WHERE` 절을 사용하는 SQL-92 이전 구문:

```sql
SELECT * FROM weather, cities WHERE city = name;
```

#### 3. 왼쪽 외부 조인
왼쪽 테이블의 모든 행과 오른쪽 테이블의 일치하는 행을 반환합니다. 일치하지 않는 오른쪽 테이블 열은 NULL 값을 포함합니다.

```sql
SELECT * FROM weather LEFT OUTER JOIN cities ON weather.city = cities.name;
```

결과에 포함:
- 모든 날씨 레코드 (NULL 도시 세부 정보가 있는 Hayward 포함)
- 사용 가능한 경우 일치하는 도시 정보

#### 4. 셀프 조인
테이블이 자기 자신과 조인되며, 같은 테이블 내의 행을 비교하는 데 유용합니다:

```sql
SELECT w1.city, w1.temp_lo AS low, w1.temp_hi AS high,
       w2.city, w2.temp_lo AS low, w2.temp_hi AS high
    FROM weather w1 JOIN weather w2
        ON w1.temp_lo < w2.temp_lo AND w1.temp_hi > w2.temp_hi;
```

### 모범 사례

1. 조인에서 모호성을 피하기 위해 열 이름을 한정 합니다:
   ```sql
   SELECT weather.city, cities.location
       FROM weather JOIN cities ON weather.city = cities.name;
   ```

2. 간결성을 위해 테이블 별칭 을 사용합니다:
   ```sql
   SELECT * FROM weather w JOIN cities c ON w.city = c.name;
   ```

3. 명확성을 위해 암시적 WHERE 구문보다 명시적 JOIN/ON 구문을 사용 합니다.

### 추가 조인 유형
문서에서는 오른쪽 외부 조인 과 완전 외부 조인 이 존재한다고 언급하지만 이 섹션에서는 자세히 다루지 않습니다.

---

## 2.7. 집계 함수

### 개요
PostgreSQL은 여러 입력 행에서 단일 결과를 계산하는 집계 함수 를 지원합니다. 일반적인 집계 함수는 다음과 같습니다:
- `count` - 행 수 세기
- `sum` - 값 합계
- `avg` - 평균 값
- `max` - 최대 값
- `min` - 최소 값

### 기본 예제

#### 간단한 집계
```sql
SELECT max(temp_lo) FROM weather;
```

#### 집계와 함께 서브쿼리 사용
집계 함수는 `WHERE` 절에서 사용할 수 없으므로 서브쿼리를 사용합니다:
```sql
SELECT city FROM weather
    WHERE temp_lo = (SELECT max(temp_lo) FROM weather);
```

### GROUP BY 절
집계는 `GROUP BY`와 함께 사용하여 그룹별 결과를 계산하는 데 유용합니다:
```sql
SELECT city, count(*), max(temp_lo)
    FROM weather
    GROUP BY city;
```

### HAVING 절
`HAVING`을 사용하여 그룹화된 결과를 필터링합니다 (집계와 함께 작동):
```sql
SELECT city, count(*), max(temp_lo)
    FROM weather
    GROUP BY city
    HAVING max(temp_lo) < 40;
```

### WHERE vs HAVING
| 절 | 적용 시점 | 집계 사용 가능? |
|------|----------|---------------|
| `WHERE` | 그룹과 집계가 계산되기 전 | 아니오 |
| `HAVING` | 그룹과 집계가 계산된 후 | 예 |

### FILTER 절
특정 집계 함수에 조건을 적용합니다:
```sql
SELECT city, count(*) FILTER (WHERE temp_lo < 45), max(temp_lo)
    FROM weather
    GROUP BY city;
```

`FILTER` 절은 연결된 특정 집계의 입력에서만 행을 제거하며, 다른 집계는 여전히 모든 행을 처리합니다.

---

## 2.8. 업데이트

### 개요
`UPDATE` 명령은 PostgreSQL 테이블의 기존 행을 수정하는 데 사용됩니다.

### 기본 구문
```sql
UPDATE table_name
    SET column1 = value1, column2 = value2, ...
    WHERE condition;
```

### 예제
문서는 1994년 11월 28일 이후의 날짜에 대해 `temp_hi`와 `temp_lo` 열 모두에서 2도를 빼서 온도 판독값을 수정해야 하는 실용적인 예제를 제공합니다:

```sql
UPDATE weather
    SET temp_hi = temp_hi - 2, temp_lo = temp_lo - 2
    WHERE date > '1994-11-28';
```

### 결과
UPDATE 문을 실행한 후 영향을 받은 행이 수정됩니다. 예를 들어:

| city          | temp_lo | temp_hi | prcp | date       |
|---------------|---------|---------|------|------------|
| San Francisco | 46      | 50      | 0.25 | 1994-11-27 |
| San Francisco | 41      | 55      | 0    | 1994-11-29 |
| Hayward       | 35      | 52      |      | 1994-11-29 |

### 주요 포인트
- 업데이트할 행을 지정하기 위해 `WHERE` 절을 사용합니다.
- 단일 UPDATE 문에서 여러 열을 업데이트할 수 있습니다.
- 열 값은 표현식 (산술 연산, 함수 등)을 사용하여 수정할 수 있습니다.

---

## 2.9. 삭제

### 개요
`DELETE` 명령은 PostgreSQL 테이블에서 행을 제거합니다.

### 기본 구문
```sql
DELETE FROM tablename WHERE condition;
```

### 예제
특정 도시 (Hayward)의 모든 날씨 레코드를 삭제하려면:

```sql
DELETE FROM weather WHERE city = 'Hayward';
```

이 삭제 후 남은 테이블은 다음과 같습니다:

```
     city      | temp_lo | temp_hi | prcp |    date
---------------+---------+---------+------+------------
 San Francisco |      46 |      50 | 0.25 | 1994-11-27
 San Francisco |      41 |      55 |    0 | 1994-11-29
(2 rows)
```

### 중요 경고

조건 없는 DELETE 문을 피하세요:
```sql
DELETE FROM tablename;
```

`WHERE` 절 없이 이 명령은 테이블의 모든 행을 삭제 하여 빈 상태로 만듭니다. PostgreSQL은 이 작업을 실행하기 전에 확인을 요청하지 않습니다.

의도적으로 모든 데이터를 제거하려는 것이 아니라면 항상 삭제할 행을 지정하는 `WHERE` 절을 포함하세요.

---

# 3장. 고급 기능

## 3.1. 소개

이전 장에서는 PostgreSQL에서 데이터를 저장하고 접근하기 위한 SQL 사용의 기초를 다루었습니다. 이제 더 정교한 쿼리를 관리하는 데 도움이 되는 SQL의 고급 기능에 대해 논의할 것입니다.

---

## 3.2. 뷰

### 개요
PostgreSQL의 뷰 는 일반 테이블처럼 참조할 수 있는 명명된 쿼리입니다. 복잡한 쿼리를 캡슐화하고 데이터에 대한 일관된 인터페이스를 제공하는 방법을 제공합니다.

### 기본 구문

```sql
CREATE VIEW view_name AS
    SELECT column1, column2, ...
    FROM table1, table2, ...
    WHERE condition;
```

### 예제

문서 예제를 기반으로:

```sql
CREATE VIEW myview AS
    SELECT name, temp_lo, temp_hi, prcp, date, location
        FROM weather, cities
        WHERE city = name;

SELECT * FROM myview;
```

이 뷰는 날씨 레코드와 도시 위치 정보를 결합하므로 조인 쿼리를 반복적으로 작성할 필요가 없습니다.

### 주요 이점

1. 쿼리 단순화: 간단한 뷰 이름으로 복잡한 쿼리 참조
2. 추상화: 일관된 인터페이스 뒤에 테이블 구조 세부 정보 캡슐화
3. 유지보수성: 기본 테이블 구조가 발전해도 애플리케이션 로직이 안정적으로 유지
4. 유연성: 뷰는 실제 테이블을 사용할 수 있는 거의 모든 곳에서 사용 가능
5. 조합성: 다른 뷰 위에 뷰를 구축할 수 있음

### 설계 모범 사례

뷰를 자유롭게 사용하는 것은 좋은 SQL 데이터베이스 설계 의 핵심 측면으로 간주됩니다. 물리적 데이터베이스 스키마와 애플리케이션이 작업하는 논리적 데이터 구조 간의 깔끔한 분리를 유지하는 데 도움이 됩니다.

---

## 3.3. 외래 키

### 개요

외래 키는 데이터의 참조 무결성 을 유지하기 위한 PostgreSQL의 메커니즘입니다. 다른 테이블에 존재하지 않는 항목을 참조하는 행을 테이블에 삽입할 수 없도록 보장합니다.

### 예제

외래 키 제약 조건이 있는 테이블을 생성하는 방법:

```sql
CREATE TABLE cities (
        name     varchar(80) primary key,
        location point
);

CREATE TABLE weather (
        city      varchar(80) references cities(name),
        temp_lo   int,
        temp_hi   int,
        prcp      real,
        date      date
);
```

이 예제에서 `weather` 테이블은 `cities` 테이블의 `name` 열을 참조하는 `city` 열에 대한 외래 키 제약 조건을 가집니다.

### 오류 처리

외래 키 제약 조건을 위반하는 유효하지 않은 레코드를 삽입하려고 하면:

```sql
INSERT INTO weather VALUES ('Berkeley', 45, 53, 0.0, '1994-11-28');
```

PostgreSQL은 오류를 반환합니다:

```
ERROR:  insert or update on table "weather" violates foreign key constraint "weather_city_fkey"
DETAIL:  Key (city)=(Berkeley) is not present in table "cities".
```

### 이점

외래 키를 올바르게 사용하면:
- 유효하지 않은 데이터 삽입 방지
- 데이터 일관성과 무결성 유지
- 데이터베이스 애플리케이션의 품질 향상

### 추가 정보

외래 키의 동작은 특정 애플리케이션 요구에 맞게 사용자 정의할 수 있습니다. 더 고급 구성 옵션은 PostgreSQL 문서의 5장 (데이터 정의)을 참조하세요.

---

## 3.4. 트랜잭션

### 정의
트랜잭션 은 여러 SQL 단계를 단일의 전부 또는 전무 작업으로 묶는 기본적인 데이터베이스 작업입니다. 단계 사이의 중간 상태는 다른 동시 트랜잭션에 보이지 않습니다.

### 주요 속성

#### 1. 원자성 (Atomicity)
- 트랜잭션의 모든 업데이트가 발생하거나 아무것도 발생하지 않음
- 부분 실패 방지 (예: 한 계정에서 출금하고 다른 계정에 입금하지 않는 상황)

#### 2. 지속성 (Durability)
- 트랜잭션이 완료되고 승인되면 디스크에 영구적으로 기록됨
- 완료 직후 충돌이 발생해도 손실되지 않음

#### 3. 격리성 (Isolation)
- 동시 트랜잭션은 다른 트랜잭션의 불완전한 변경을 볼 수 없음
- 업데이트는 트랜잭션이 완료될 때 동시에 표시됨

### 기본 구문

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100.00
    WHERE name = 'Alice';
UPDATE accounts SET balance = balance + 100.00
    WHERE name = 'Bob';
COMMIT;
```

대안: 모든 업데이트를 취소하려면 `COMMIT` 대신 `ROLLBACK`을 사용합니다.

### 암시적 트랜잭션

PostgreSQL은 모든 SQL 문을 트랜잭션 내에 있는 것으로 처리합니다:
- `BEGIN`이 실행되지 않으면 각 문에 암시적인 `BEGIN`과 `COMMIT`이 감싸집니다.
- `BEGIN`과 `COMMIT`으로 둘러싸인 문 그룹을 트랜잭션 블록 이라고 합니다.

### 세이브포인트

트랜잭션의 일부를 선택적으로 폐기하여 세밀한 제어를 허용합니다:

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100.00
    WHERE name = 'Alice';
SAVEPOINT my_savepoint;
UPDATE accounts SET balance = balance + 100.00
    WHERE name = 'Bob';
ROLLBACK TO my_savepoint;  -- Bob의 입금 취소
UPDATE accounts SET balance = balance + 100.00
    WHERE name = 'Wally';  -- 대신 Wally에게 입금
COMMIT;
```

참고: 세이브포인트를 해제하거나 롤백하면 그 이후에 정의된 모든 세이브포인트가 자동으로 해제됩니다.

---

## 3.5. 윈도우 함수

### 정의
윈도우 함수 는 집계 함수처럼 단일 출력 행으로 그룹화하지 않고 관련 테이블 행 집합에 대해 계산을 수행합니다. 각 행은 별도의 정체성을 유지하면서 함수가 여러 행의 데이터에 접근할 수 있습니다.

### 주요 구문 구성 요소

#### 기본 구조
```sql
SELECT column, window_function() OVER (window_specification) FROM table;
```

#### 필수 절

1. OVER 절 - 모든 윈도우 함수에 필요; 행이 처리되는 방식 지정
2. PARTITION BY - 같은 값을 공유하는 그룹으로 행을 나눔 (선택 사항)
3. ORDER BY - 윈도우 내에서 행 처리 순서 제어 (선택 사항)

### 실용적인 예제

#### 예제 1: 부서 급여 비교
```sql
SELECT depname, empno, salary, avg(salary) OVER (PARTITION BY depname)
FROM empsalary;
```
모든 행을 별도로 유지하면서 각 직원의 급여를 부서 평균과 비교합니다.

#### 예제 2: 행 번호 매기기
```sql
SELECT depname, empno, salary,
       row_number() OVER (PARTITION BY depname ORDER BY salary DESC)
FROM empsalary;
```
급여 내림차순으로 정렬된 각 부서 파티션 내의 행에 순차 번호를 할당합니다.

#### 예제 3: 누적 합계
```sql
SELECT salary, sum(salary) OVER (ORDER BY salary)
FROM empsalary;
```
최저 급여에서 최고 급여까지 누적 급여 합계를 계산합니다.

### 중요한 제약 사항

- 허용된 위치: `SELECT` 목록과 `ORDER BY` 절만
- 허용되지 않음: `GROUP BY`, `HAVING` 또는 `WHERE` 절
- 실행 순서: 윈도우 함수는 비윈도우 집계 함수 후에 실행됨

### 윈도우 프레임

- ORDER BY가 있는 경우 기본값: 파티션 시작부터 현재 행까지의 모든 행 (현재 행의 중복 포함)
- ORDER BY가 없는 경우 기본값: 파티션의 모든 행

### 명명된 윈도우

반복을 피하기 위해 재사용 가능한 윈도우 사양을 정의합니다:
```sql
SELECT sum(salary) OVER w, avg(salary) OVER w
FROM empsalary
WINDOW w AS (PARTITION BY depname ORDER BY salary DESC);
```

### 고급 사용 사례: 결과 필터링

서브쿼리를 사용하여 윈도우 계산 후 필터링:
```sql
SELECT depname, empno, salary, enroll_date
FROM (
  SELECT depname, empno, salary, enroll_date,
    row_number() OVER (PARTITION BY depname ORDER BY salary DESC, empno) AS pos
  FROM empsalary
) AS ss
WHERE pos < 3;
```

---

## 3.6. 상속

### 상속이란?

상속은 객체 지향 데이터베이스의 개념으로 PostgreSQL에서 흥미로운 데이터베이스 설계 가능성을 제공합니다. 테이블이 부모 테이블의 모든 열을 상속하면서 자체 추가 열을 추가할 수 있습니다.

### 기본 개념

#### 전통적 접근 방식의 문제점

순진한 접근 방식은 별도의 테이블을 만들고 UNION 뷰를 사용합니다:

```sql
CREATE TABLE capitals (
  name       text,
  population real,
  elevation  int,
  state      char(2)
);

CREATE TABLE non_capitals (
  name       text,
  population real,
  elevation  int
);

CREATE VIEW cities AS
  SELECT name, population, elevation FROM capitals
    UNION
  SELECT name, population, elevation FROM non_capitals;
```

이것은 쿼리에는 작동하지만 여러 행을 업데이트할 때 문제가 됩니다.

### 더 나은 해결책: 테이블 상속

```sql
CREATE TABLE cities (
  name       text,
  population real,
  elevation  int
);

CREATE TABLE capitals (
  state      char(2) UNIQUE NOT NULL
) INHERITS (cities);
```

`capitals` 테이블은 `cities`의 모든 열 (`name`, `population`, `elevation`)을 상속하고 자체 열 (`state`)을 추가합니다.

### 상속을 사용한 쿼리

#### 자식 테이블 포함 (기본값)
```sql
SELECT name, elevation
  FROM cities
  WHERE elevation > 500;
```
`cities`와 `capitals` 테이블 모두에서 결과를 반환합니다.

#### 자식 테이블 제외 (ONLY 키워드)
```sql
SELECT name, elevation
    FROM ONLY cities
    WHERE elevation > 500;
```
상속된 테이블을 제외하고 `cities` 테이블에서만 결과를 반환합니다.

### 주요 기능

- 테이블은 0개 이상 의 다른 테이블에서 상속할 수 있습니다.
- `ONLY` 표기법은 `SELECT`, `UPDATE`, `DELETE` 명령과 함께 작동합니다.
- 자식 테이블은 모든 부모 열을 자동으로 포함합니다.

### 중요한 제한 사항

참고: 상속은 고유 제약 조건 이나 외래 키 와 완전히 통합되지 않아 유용성이 제한됩니다. 이러한 제한 사항에 대한 자세한 내용은 PostgreSQL 문서의 5.11절을 참조하세요.

---

## 3.7. 결론

### 요약

이 섹션은 PostgreSQL의 입문 튜토리얼의 끝을 표시합니다.

이 튜토리얼에서 다룬 내용:
- 튜토리얼은 SQL의 새로운 사용자를 위한 소개를 제공했습니다.
- 기본부터 고급 기능까지 다루었지만, PostgreSQL에는 논의되지 않은 많은 추가 기능이 있습니다.

### 다음 단계

1. 추가 정보: [PostgreSQL 웹 사이트](https://www.postgresql.org)를 방문하여 추가 리소스와 입문 자료에 대한 링크를 확인하세요.

2. 학습 계속: PostgreSQL 문서의 나머지 부분은 기능을 더 자세히 다루며, 다음부터 시작합니다:
   - Part II: SQL 언어 - 포괄적인 SQL 언어 참조
   - Part III: 서버 관리 - PostgreSQL 설치 관리
   - Part IV: 클라이언트 인터페이스 - 애플리케이션 개발

---

# 참고 자료

- [PostgreSQL 18 공식 문서](https://www.postgresql.org/docs/18/)
- [PostgreSQL 튜토리얼](https://www.postgresql.org/docs/18/tutorial.html)
