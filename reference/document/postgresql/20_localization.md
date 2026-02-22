# 제23장. 지역화 (Localization)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/charset.html

이 장에서는 관리자가 사용할 수 있는 지역화 기능에 대해 설명합니다.

목차
- [23.1. 로케일 지원](#231-로케일-지원)
  - [23.1.1. 개요](#2311-개요)
  - [23.1.2. 동작](#2312-동작)
  - [23.1.3. 로케일 선택](#2313-로케일-선택)
  - [23.1.4. 로케일 제공자](#2314-로케일-제공자)
  - [23.1.5. ICU 로케일](#2315-icu-로케일)
  - [23.1.6. 문제 해결](#2316-문제-해결)
- [23.2. 콜레이션 지원](#232-콜레이션-지원)
  - [23.2.1. 개념](#2321-개념)
  - [23.2.2. 콜레이션 관리](#2322-콜레이션-관리)
  - [23.2.3. ICU 사용자 정의 콜레이션](#2323-icu-사용자-정의-콜레이션)
- [23.3. 문자 집합 지원](#233-문자-집합-지원)
  - [23.3.1. 지원되는 문자 집합](#2331-지원되는-문자-집합)
  - [23.3.2. 문자 집합 설정](#2332-문자-집합-설정)
  - [23.3.3. 서버와 클라이언트 간 자동 문자 집합 변환](#2333-서버와-클라이언트-간-자동-문자-집합-변환)
  - [23.3.4. 사용 가능한 문자 집합 변환](#2334-사용-가능한-문자-집합-변환)
  - [23.3.5. 추가 자료](#2335-추가-자료)

---

## 23.1. 로케일 지원

로케일 지원은 알파벳, 정렬, 숫자 형식 등에 관한 문화적 선호도를 애플리케이션이 존중하는 것을 말합니다. PostgreSQL은 서버 운영 체제에서 제공하는 표준 ISO C 및 POSIX 로케일 기능을 사용합니다.

### 23.1.1. 개요

로케일 지원은 `initdb`를 사용하여 데이터베이스 클러스터를 생성할 때 자동으로 초기화됩니다. 기본 로케일은 실행 환경에서 상속됩니다.

#### 기본 로케일 선택

초기화 시 로케일을 지정하려면:

```bash
initdb --locale=sv_SE
```

이 예제는 로케일을 스웨덴(SE)에서 사용되는 스웨덴어(sv)로 설정합니다. 다른 예시:
- `en_US` (미국 영어)
- `fr_CA` (캐나다 프랑스어)
- `fr_BE.UTF-8` (UTF-8 인코딩을 사용하는 벨기에 프랑스어)

#### 로케일 하위 카테고리

로케일 규칙은 하위 카테고리를 사용하여 혼합할 수 있습니다:

| 카테고리 | 용도 |
|----------|------|
| `LC_COLLATE` | 문자열 정렬 순서 |
| `LC_CTYPE` | 문자 분류 (문자란 무엇인가? 대문자 등가는?) |
| `LC_MESSAGES` | 메시지 언어 |
| `LC_MONETARY` | 통화 금액 형식 |
| `LC_NUMERIC` | 숫자 형식 |
| `LC_TIME` | 날짜 및 시간 형식 |

#### 예제: 혼합 로케일

```bash
initdb --locale=fr_CA --lc-monetary=en_US
```

이것은 캐나다 프랑스어 로케일을 사용하지만 통화 형식에는 미국 규칙을 사용합니다.

#### 특수 로케일 이름

로케일 지원을 비활성화하려면:

```bash
initdb --locale=C
# 또는
initdb --locale=POSIX
```

#### 고정 로케일 카테고리

`LC_COLLATE`와 `LC_CTYPE`은 데이터베이스 생성 시 고정 되어야 하며 이후에는 변경할 수 없습니다. 이는 인덱스의 정렬 순서에 영향을 미치기 때문입니다. 다른 카테고리는 서버 구성 매개변수를 통해 동적으로 변경할 수 있습니다.

#### 환경 변수 상속

실행 환경에서 로케일을 상속할 때, 각 카테고리에 대해 다음 변수들이 순서대로 참조됩니다:
1. `LC_ALL`
2. `LC_<CATEGORY>` (예: `LC_COLLATE`)
3. `LANG`
4. 설정된 것이 없으면 기본값 `C`

> 참고: `LANGUAGE` 환경 변수는 메시지 언어에 대해 다른 설정을 재정의합니다.

#### NLS 지원

메시지 번역에는 빌드 시 NLS가 활성화되어야 합니다:

```bash
configure --enable-nls
```

---

### 23.1.2. 동작

로케일 설정은 다음 SQL 기능에 영향을 미칩니다:

1. 텍스트 데이터에 대한 `ORDER BY` 쿼리 및 비교 연산자의 정렬 순서
2. 문자열 함수: `upper`, `lower`, `initcap`
3. 패턴 매칭: `LIKE`, `SIMILAR TO`, POSIX 스타일 정규 표현식
4. 형식 지정 함수: `to_char` 함수 계열
5. `LIKE` 절에서의 인덱스 사용

#### 성능 고려사항

`C` 또는 `POSIX` 이외의 로케일을 사용하면 성능 저하가 있습니다:
- 문자 처리가 느려짐
- `LIKE`에서 일반 인덱스 사용이 방지됨

#### 성능 해결 방법

1. `LIKE` 인덱스에 사용자 정의 연산자 클래스 사용 (Section 11.10 참조)
2. `C` 콜레이션을 사용하여 인덱스 생성 (Section 23.2 참조)

---

### 23.1.3. 로케일 선택

로케일은 여러 범위에서 선택할 수 있으며, 각각 더 세밀한 세분화를 제공합니다:

1. 운영 체제 환경 – 새로 초기화된 클러스터의 기본값
2. `initdb` 명령줄 옵션 – 클러스터 전체 로케일 설정 지정
3. 데이터베이스별 설정 – `CREATE DATABASE` 또는 `createdb` 명령 사용
4. 열별 설정 – SQL 콜레이션 객체 사용 (Section 23.2)
5. 쿼리별 설정 – 임시 변경을 위한 SQL 콜레이션 객체 사용

---

### 23.1.4. 로케일 제공자

로케일 제공자는 콜레이션과 문자 분류에 대한 로케일 동작을 정의하는 라이브러리를 지정합니다.

#### 예제: ICU 제공자로 초기화

```bash
initdb --locale-provider=icu --icu-locale=en
```

로케일 제공자는 다양한 세분화 수준에서 혼합할 수 있습니다(예: 클러스터에는 `libc`를 사용하지만 개별 데이터베이스에는 `icu`를 사용).

#### 사용 가능한 로케일 제공자

##### `builtin`

내장 연산을 사용합니다. `C`, `C.UTF-8`, `PG_UNICODE_FAST` 로케일만 지원합니다.

`C` 로케일:
- libc 제공자의 `C` 로케일과 동일
- 동작은 데이터베이스 인코딩에 따라 달라질 수 있음

`C.UTF-8` 로케일:
- UTF-8 데이터베이스 인코딩에서만 사용 가능
- 유니코드 기반 동작
- 콜레이션에 코드 포인트 값 사용
- "POSIX 호환" 의미론을 기반으로 한 정규 표현식 문자 클래스
- "단순" 변형 대소문자 매핑

`PG_UNICODE_FAST` 로케일:
- UTF-8 데이터베이스 인코딩에서만 사용 가능
- 유니코드 기반 동작
- 콜레이션에 코드 포인트 값 사용
- "표준" 의미론을 기반으로 한 정규 표현식 문자 클래스
- "전체" 변형 대소문자 매핑

##### `icu`

외부 ICU 라이브러리를 사용합니다(PostgreSQL이 지원을 포함하여 구성되어야 함).

장점:
- 콜레이션과 문자 분류가 운영 체제 및 데이터베이스 인코딩과 독립적
- 다른 플랫폼으로 전환할 때 일관된 결과
- `LC_COLLATE`와 `LC_CTYPE`을 독립적으로 설정 가능

> 참고: 결과는 ICU 라이브러리 버전에 따라 달라질 수 있으며, 이는 자연어의 변경 사항을 반영하여 업데이트됩니다.

##### `libc`

운영 체제의 C 라이브러리를 사용합니다.

- 콜레이션과 문자 분류가 `LC_COLLATE`와 `LC_CTYPE`에 의해 제어됨
- 이러한 설정은 독립적으로 설정할 수 없음

> 참고: 동일한 로케일 이름이 다른 플랫폼에서 다른 동작을 할 수 있습니다.

---

### 23.1.5. ICU 로케일

#### 23.1.5.1. ICU 로케일 이름

ICU는 언어 태그 형식을 사용합니다 (23.1.5.3 참조).

```sql
CREATE COLLATION mycollation1 (provider = icu, locale = 'ja-JP');
CREATE COLLATION mycollation2 (provider = icu, locale = 'fr');
```

#### 23.1.5.2. 로케일 정규화 및 검증

새 ICU 콜레이션이나 데이터베이스를 정의할 때, 로케일 이름은 표준 언어 태그 형식으로 변환됩니다:

```sql
CREATE COLLATION mycollation3 (provider = icu, locale = 'en-US-u-kn-true');
NOTICE:  using standard form "en-US-u-kn" for locale "en-US-u-kn-true"

CREATE COLLATION mycollation4 (provider = icu, locale = 'de_DE.utf8');
NOTICE:  using standard form "de-DE" for locale "de_DE.utf8"
```

##### 검증 경고

```sql
CREATE COLLATION nonsense (provider = icu, locale = 'nonsense');
WARNING:  ICU locale "nonsense" has unknown language "nonsense"
HINT:  To disable ICU locale validation, set parameter icu_validation_level to DISABLED.
```

`icu_validation_level` 매개변수는 검증 메시지 보고 방식을 제어합니다. `ERROR`로 설정되지 않은 한 콜레이션은 여전히 생성되지만 동작이 의도한 대로 되지 않을 수 있습니다.

#### 23.1.5.3. 언어 태그

언어 태그 (BCP 47 표준)는 언어, 지역 및 로케일 정보에 대한 표준화된 식별자입니다.

##### 기본 형식

```
language[-region][-script]
```

예시:
- `ja-JP` (일본의 일본어)
- `de` (독일어)
- `fr-CA` (캐나다의 프랑스어)

##### 언어 태그의 콜레이션 설정

콜레이션 사용자 정의를 위해 `-u` 뒤에 `-key-value` 쌍을 추가합니다:

```
language-region-u-key1-value1-key2-value2
```

부울 설정은 값을 생략할 수 있습니다(암시적 `true`).

##### 예제: 사용자 정의 콜레이션 언어 태그

```
en-US-u-kn-ks-level2
```

이것의 의미:
- 미국 지역의 영어
- `kn` (숫자 정렬) = `true` (숫자 시퀀스를 숫자로 처리)
- `ks` (대소문자 구분) = `level2` (대소문자 구분 안함)

##### 사용 예제

```sql
CREATE COLLATION mycollation5 (provider = icu, deterministic = false, locale = 'en-US-u-kn-ks-level2');

SELECT 'aB' = 'Ab' COLLATE mycollation5 as result;
 result
--------
 t
(1 row)

SELECT 'N-45' < 'N-123' COLLATE mycollation5 as result;
 result
--------
 t
(1 row)
```

---

### 23.1.6. 문제 해결

#### 문제 해결 단계

1. OS 로케일 구성 확인:
   ```bash
   locale -a
   ```
   시스템에 설치된 로케일을 나열합니다.

2. 활성 로케일 설정 확인:
   ```sql
   SHOW LC_COLLATE;
   SHOW LC_CTYPE;
   SHOW LC_MESSAGES;
   SHOW LC_MONETARY;
   ```

3. 기억하세요: `LC_COLLATE`와 `LC_CTYPE`은 데이터베이스 생성 시 결정되며 기존 데이터베이스에서는 변경할 수 없습니다.

#### 테스트 스위트

소스 배포판의 `src/test/locale` 디렉토리에는 PostgreSQL의 로케일 지원에 대한 테스트 스위트가 있습니다.

#### 클라이언트 애플리케이션 고려사항

오류 메시지 텍스트를 분석하는 클라이언트 애플리케이션은 서버 메시지가 다른 언어로 되어 있을 때 문제가 발생합니다. 오류 메시지 텍스트를 분석하는 대신 오류 코드를 사용하세요.

#### 번역 기여

메시지 번역 유지에는 자원 봉사자의 노력이 필요합니다. 귀하의 언어에 더 나은 번역 적용 범위가 필요하다면 Chapter 56(네이티브 언어 지원)을 참조하거나 개발자 메일링 리스트에 문의하세요.

---

## 23.2. 콜레이션 지원

콜레이션 기능을 사용하면 열별 또는 연산별로 정렬 순서와 문자 분류 동작을 지정할 수 있어, 데이터베이스 생성 후 `LC_COLLATE`와 `LC_CTYPE` 설정을 변경할 수 없다는 제한을 완화합니다.

### 23.2.1. 개념

#### 콜레이션 기본 개념

콜레이션 가능한 데이터 유형의 모든 표현식(내장: `text`, `varchar`, `char`)에는 콜레이션이 있습니다. 열 참조의 경우 정의된 열 콜레이션이고, 상수의 경우 데이터 유형의 기본 콜레이션입니다.

복합 표현식의 콜레이션은 입력 콜레이션에서 파생됩니다. 표현식의 콜레이션은:
- "기본" 콜레이션(데이터베이스 로케일 설정)
- 불확정(정렬 연산 실패 원인)

#### 콜레이션 파생 규칙

표현식에서 여러 콜레이션을 결합할 때:

1. 명시적 콜레이션 재정의: 입력에 `COLLATE` 절을 통한 명시적 콜레이션 파생이 있는 경우, 모든 명시적 콜레이션이 일치해야 하며 그렇지 않으면 오류가 발생합니다.

2. 암시적 콜레이션 결합: 모든 입력이 동일한 암시적 콜레이션 또는 기본값을 가져야 합니다. 비기본 콜레이션이 우선합니다.

3. 충돌하는 암시적 콜레이션: 불확정 콜레이션이 되며, 연산이 콜레이션을 알아야 하는 경우에만 런타임 오류가 발생합니다.

#### 예제: 콜레이션 충돌 해결

```sql
CREATE TABLE test1 (
    a text COLLATE "de_DE",
    b text COLLATE "es_ES",
    ...
);

-- 작동: 암시적과 기본을 결합
SELECT a < 'foo' FROM test1;

-- 작동: 명시적 콜레이션이 재정의
SELECT a < ('foo' COLLATE "fr_FR") FROM test1;

-- 오류: 충돌하는 암시적 콜레이션
SELECT a < b FROM test1;

-- 수정: 명시적 콜레이션 지정자
SELECT a < b COLLATE "de_DE" FROM test1;
SELECT a COLLATE "de_DE" < b FROM test1;

-- 작동: || 연산자는 콜레이션에 관심이 없음
SELECT a || b FROM test1;

-- 오류: ORDER BY는 콜레이션이 필요하지만 || 결과는 불확정
SELECT * FROM test1 ORDER BY a || b;

-- 수정: ORDER BY에 명시적 콜레이션
SELECT * FROM test1 ORDER BY a || b COLLATE "fr_FR";
```

---

### 23.2.2. 콜레이션 관리

콜레이션은 SQL 이름을 운영 체제 로케일에 매핑하는 SQL 스키마 객체입니다. 두 가지 표준 제공자가 있습니다:

- `libc`: OS C 라이브러리 로케일 사용 (대부분의 도구가 이것을 사용)
- `icu`: 외부 ICU 라이브러리 사용 (빌드 시 구성된 경우에만 사용 가능)

#### 제공자 차이점

libc 콜레이션:
- `setlocale()`을 통해 `LC_COLLATE` 및 `LC_CTYPE` 설정에 매핑
- 문자 집합 인코딩에 연결됨
- 동일한 콜레이션 이름이 다른 인코딩에 존재할 수 있음

icu 콜레이션:
- 명명된 ICU 콜레이터에 매핑
- 별도의 collate/ctype 설정을 지원하지 않음
- 인코딩과 독립적 (데이터베이스에서 이름당 하나의 콜레이션만)

---

#### 23.2.2.1. 표준 콜레이션

모든 플랫폼에서 사용 가능:

| 콜레이션 | 동작 |
|----------|------|
| `unicode` | 기본 유니코드 콜레이션 요소 테이블을 사용한 유니코드 콜레이션 알고리즘; ICU 지원 필요 |
| `ucs_basic` | 유니코드 코드 포인트 정렬; ASCII A-Z만; UTF8만 |
| `pg_unicode_fast` | 유니코드 코드 포인트 정렬; 전체 대소문자 매핑; UTF8만 |
| `pg_c_utf8` | 유니코드 코드 포인트 정렬; 단순 대소문자 매핑; UTF8만 |
| `C`/`POSIX` | 전통적인 C 동작; 바이트 값 정렬; ASCII A-Z만 |
| `default` | 데이터베이스 생성 로케일 사용 |

---

#### 23.2.2.2. 사전 정의된 콜레이션

데이터베이스 클러스터가 초기화될 때(OS가 여러 로케일을 지원하거나 ICU가 구성된 경우), `initdb`는 사용 가능한 로케일로 `pg_collation`을 채웁니다.

사용 가능한 콜레이션 보기:
```sql
SELECT * FROM pg_collation;
-- 또는 psql에서:
\dOS+
```

##### 23.2.2.2.1. libc 콜레이션

예: OS가 `de_DE.utf8` 로케일을 제공 → PostgreSQL이 생성:
- UTF8 인코딩용 `de_DE.utf8` 콜레이션
- `de_DE` 콜레이션 (인코딩 독립적 이름)

새 libc 콜레이션 생성:
```sql
CREATE COLLATION german (provider = libc, locale = 'de_DE');
```

OS 로케일 일괄 가져오기:
```sql
SELECT pg_import_system_collations('pg_catalog');
```

##### 23.2.2.2.2. ICU 콜레이션

ICU는 `-x-icu` 접미사가 있는 BCP 47 언어 태그 형식을 사용합니다.

예제 ICU 콜레이션:
```
de-x-icu           -- 독일어, 기본 변형
de-AT-x-icu        -- 독일어, 오스트리아
und-x-icu          -- ICU 루트 콜레이션 (언어에 구애받지 않음)
```

---

#### 23.2.2.3. 새 콜레이션 객체 생성

##### 23.2.2.3.1. libc 콜레이션

```sql
CREATE COLLATION german (provider = libc, locale = 'de_DE');
```

`locale -a`로 사용 가능한 로케일 찾기

##### 23.2.2.3.2. ICU 콜레이션

```sql
CREATE COLLATION german (provider = icu, locale = 'de-DE');
```

ICU는 BCP 47 언어 태그와 libc 스타일 이름을 수용합니다(자동으로 변환됨).

##### 23.2.2.3.3. 콜레이션 복사

기존 콜레이션에서 새 콜레이션 생성:

```sql
CREATE COLLATION german FROM "de_DE";
CREATE COLLATION french FROM "fr-x-icu";
```

---

#### 23.2.2.4. 비결정적 콜레이션

콜레이션은 결정적 (동일한 문자열이 동일한 바이트 시퀀스를 가짐)이거나 비결정적 (동일한 문자열이 바이트에서 다를 수 있음, 예: 대소문자 구분 안함, 악센트 구분 안함)일 수 있습니다.

비결정적 콜레이션 생성:
```sql
CREATE COLLATION ndcoll (provider = icu, locale = 'und', deterministic = false);

-- 악센트와 대소문자 무시
CREATE COLLATION case_insensitive (provider = icu, locale = 'und-u-ks-level2', deterministic = false);

-- 악센트만 무시
CREATE COLLATION ignore_accents (provider = icu, locale = 'und-u-ks-level1-kc-true', deterministic = false);
```

장단점:
- 장점: 유니코드에서 더 정확한 동작
- 단점: 성능 저하
- 단점: B-트리 중복 제거 비활성화
- 단점: 일부 패턴 매칭 사용 불가

대안: 대신 `normalize()` 및 `is_normalized()` 함수 사용.

---

### 23.2.3. ICU 사용자 정의 콜레이션

ICU는 언어 태그의 콜레이션 설정을 통해 광범위한 사용자 정의를 허용합니다.

예제:
```sql
-- 악센트와 대소문자 차이 무시
CREATE COLLATION ignore_accent_case (provider = icu, deterministic = false, locale = 'und-u-ks-level1');
SELECT 'Å' = 'A' COLLATE ignore_accent_case;  -- true
SELECT 'z' = 'Z' COLLATE ignore_accent_case;  -- true

-- 대문자가 소문자보다 먼저 정렬
CREATE COLLATION upper_first (provider = icu, locale = 'und-u-kf-upper');
SELECT 'B' < 'b' COLLATE upper_first;  -- true

-- 숫자 콜레이션, 구두점 무시
CREATE COLLATION num_ignore_punct (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-kn');
SELECT 'id-45' < 'id-123' COLLATE num_ignore_punct;  -- true
SELECT 'w;x*y-z' = 'wxyz' COLLATE num_ignore_punct;  -- true
```

---

#### 23.2.3.1. ICU 비교 수준

표 23.1: ICU 콜레이션 수준

| 수준 | 설명 | `'f'='f'` | `'ab'=U&'a\2063b'` | `'x-y'='x_y'` | `'g'='G'` | `'n'='ñ'` | `'y'='z'` |
|------|------|----------|-------------------|---------------|-----------|-----------|-----------|
| level1 | 기본 문자 | true | true | true | true | true | false |
| level2 | 악센트 | true | true | true | true | false | false |
| level3 | 대소문자/변형 | true | true | true | false | false | false |
| level4 | 구두점 | true | true | false | false | false | false |
| identic | 모두 | true | false | false | false | false | false |

예제:
```sql
CREATE COLLATION level3 (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-level3');
CREATE COLLATION level4 (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-level4');
CREATE COLLATION identic (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-identic');

-- 보이지 않는 구분자는 identic을 제외한 모든 수준에서 무시됨
SELECT 'ab' = U&'a\\2063b' COLLATE level4;  -- true
SELECT 'ab' = U&'a\\2063b' COLLATE identic;  -- false

-- 구두점은 level3에서는 무시되지만 level4에서는 아님
SELECT 'x-y' = 'x_y' COLLATE level3;  -- true
SELECT 'x-y' = 'x_y' COLLATE level4;  -- false
```

---

#### 23.2.3.2. ICU 로케일의 콜레이션 설정

표 23.2: ICU 콜레이션 설정

| 키 | 값 | 기본값 | 설명 |
|----|-----|--------|------|
| `co` | `emoji`, `phonebk`, `standard`, ... | `standard` | 콜레이션 유형 |
| `ka` | `noignore`, `shifted` | `noignore` | 구두점/공백 무시 (`ks` ≤ level3일 때) |
| `kb` | `true`, `false` | `false` | 역방향 레벨 2 비교 |
| `kc` | `true`, `false` | `false` | 대소문자를 "레벨 2.5"로 분리 |
| `kf` | `upper`, `lower`, `false` | `false` | 대/소문자 정렬 순서 |
| `kn` | `true`, `false` | `false` | 숫자를 숫자 값으로 처리 |
| `kk` | `true`, `false` | `false` | 전체 정규화 활성화 |
| `kr` | `space`, `punct`, `symbol`, `currency`, `digit`, script-id | | 문자 클래스 순서 재정의 |
| `ks` | `level1`, `level2`, `level3`, `level4`, `identic` | `level3` | 민감도/강도 |
| `kv` | `space`, `punct`, `symbol`, `currency` | `punct` | 레벨 3에서 무시할 문자 클래스 |

---

#### 23.2.3.3. 콜레이션 설정 예제

```sql
-- 독일어 전화번호부 콜레이션
CREATE COLLATION "de-u-co-phonebk-x-icu" (provider = icu, locale = 'de-u-co-phonebk');

-- 이모지 콜레이션
CREATE COLLATION "und-u-co-emoji-x-icu" (provider = icu, locale = 'und-u-co-emoji');

-- 그리스 문자가 라틴 문자보다 먼저
CREATE COLLATION latinlast (provider = icu, locale = 'en-u-kr-grek-latn');

-- 대문자가 소문자보다 먼저
CREATE COLLATION upperfirst (provider = icu, locale = 'en-u-kf-upper');

-- 결합된 옵션
CREATE COLLATION special (provider = icu, locale = 'en-u-kf-upper-kr-grek-latn');
```

---

#### 23.2.3.4. ICU 맞춤 규칙

맞춤 규칙을 사용한 사용자 정의 콜레이션 요소 순서:

```sql
-- 간단한 예: W가 V 뒤에 정렬
CREATE COLLATION custom (provider = icu, locale = 'und', rules = '&V << w <<< W');

-- 복잡한 예: EBCDIC 문자 순서
CREATE COLLATION ebcdic (provider = icu, locale = 'und',
rules = $$
& ' ' < '.' < '<' < '(' < '+' < \|
< '&' < '!' < '$' < '*' < ')' < ';'
< '-' < '/' < ',' < '%' < '_' < '>' < '?'
< '`' < ':' < '#' < '@' < \' < '=' < '"'
<*a-r < '~' <*s-z < '^' < '[' < ']'
< '{' <*A-I < '}' <*J-R < '\' <*S-Z <*0-9
$$);

SELECT c
FROM (VALUES ('a'), ('b'), ('A'), ('B'), ('1'), ('2'), ('!'), ('^')) AS x(c)
ORDER BY c COLLATE ebcdic;
 c
---
 !
 a
 b
 ^
 A
 B
 1
 2
```

---

#### 23.2.3.5. ICU 외부 참조

- [Unicode Technical Standard #35](https://www.unicode.org/reports/tr35/tr35-collation.html)
- [BCP 47](https://www.rfc-editor.org/info/bcp47)
- [CLDR 저장소](https://github.com/unicode-org/cldr/blob/master/common/bcp47/collation.xml)
- [ICU 로케일 가이드](https://unicode-org.github.io/icu/userguide/locale/)
- [ICU 콜레이션 가이드](https://unicode-org.github.io/icu/userguide/collation/)
- [ICU 맞춤 규칙](https://unicode-org.github.io/icu/userguide/collation/customization/)

---

## 23.3. 문자 집합 지원

PostgreSQL의 문자 집합 지원을 사용하면 다양한 문자 집합(인코딩)으로 텍스트를 저장할 수 있습니다:
- 단일 바이트 문자 집합: ISO 8859 시리즈
- 다중 바이트 문자 집합: EUC (Extended Unix Code), UTF-8, Mule internal code

지원되는 모든 문자 집합은 클라이언트에서 투명하게 사용할 수 있지만, 일부는 서버 측 사용에 지원되지 않습니다.

### 주요 제약

각 데이터베이스의 문자 집합은 데이터베이스의 `LC_CTYPE`(문자 분류) 및 `LC_COLLATE`(문자열 정렬 순서) 로케일 설정과 호환되어야 합니다:
- `C` 또는 `POSIX` 로케일의 경우: 모든 문자 집합 허용
- 다른 libc 제공 로케일의 경우: 하나의 문자 집합만 올바르게 작동
- Windows에서: UTF-8 인코딩을 모든 로케일과 함께 사용 가능
- ICU 지원이 있는 경우: ICU 제공 로케일은 대부분의(전부는 아닌) 서버 측 인코딩과 작동

---

### 23.3.1. 지원되는 문자 집합

표 23.3. PostgreSQL 문자 집합

| 이름 | 설명 | 언어 | 서버? | ICU? | 바이트/문자 | 별칭 |
|------|------|------|-------|------|------------|------|
| `BIG5` | Big Five | 번체 중국어 | 아니오 | 아니오 | 1–2 | `WIN950`, `Windows950` |
| `EUC_CN` | Extended UNIX Code-CN | 간체 중국어 | 예 | 예 | 1–3 | - |
| `EUC_JP` | Extended UNIX Code-JP | 일본어 | 예 | 예 | 1–3 | - |
| `EUC_JIS_2004` | Extended UNIX Code-JP, JIS X 0213 | 일본어 | 예 | 아니오 | 1–3 | - |
| `EUC_KR` | Extended UNIX Code-KR | 한국어 | 예 | 예 | 1–3 | - |
| `EUC_TW` | Extended UNIX Code-TW | 번체 중국어, 대만어 | 예 | 예 | 1–4 | - |
| `GB18030` | 국가 표준 | 중국어 | 아니오 | 아니오 | 1–4 | - |
| `GBK` | 확장 국가 표준 | 간체 중국어 | 아니오 | 아니오 | 1–2 | `WIN936`, `Windows936` |
| `ISO_8859_5` | ISO 8859-5, ECMA 113 | 라틴/키릴 | 예 | 예 | 1 | - |
| `ISO_8859_6` | ISO 8859-6, ECMA 114 | 라틴/아랍어 | 예 | 예 | 1 | - |
| `ISO_8859_7` | ISO 8859-7, ECMA 118 | 라틴/그리스어 | 예 | 예 | 1 | - |
| `ISO_8859_8` | ISO 8859-8, ECMA 121 | 라틴/히브리어 | 예 | 예 | 1 | - |
| `JOHAB` | JOHAB | 한국어 (한글) | 아니오 | 아니오 | 1–3 | - |
| `KOI8R` | KOI8-R | 키릴 (러시아어) | 예 | 예 | 1 | `KOI8` |
| `KOI8U` | KOI8-U | 키릴 (우크라이나어) | 예 | 예 | 1 | - |
| `LATIN1` | ISO 8859-1, ECMA 94 | 서유럽어 | 예 | 예 | 1 | `ISO88591` |
| `LATIN2` | ISO 8859-2, ECMA 94 | 중앙 유럽어 | 예 | 예 | 1 | `ISO88592` |
| `LATIN3` | ISO 8859-3, ECMA 94 | 남유럽어 | 예 | 예 | 1 | `ISO88593` |
| `LATIN4` | ISO 8859-4, ECMA 94 | 북유럽어 | 예 | 예 | 1 | `ISO88594` |
| `LATIN5` | ISO 8859-9, ECMA 128 | 터키어 | 예 | 예 | 1 | `ISO88599` |
| `LATIN6` | ISO 8859-10, ECMA 144 | 노르딕 | 예 | 예 | 1 | `ISO885910` |
| `LATIN7` | ISO 8859-13 | 발트어 | 예 | 예 | 1 | `ISO885913` |
| `LATIN8` | ISO 8859-14 | 켈트어 | 예 | 예 | 1 | `ISO885914` |
| `LATIN9` | ISO 8859-15 | 유로 및 악센트가 포함된 LATIN1 | 예 | 예 | 1 | `ISO885915` |
| `LATIN10` | ISO 8859-16, ASRO SR 14111 | 루마니아어 | 예 | 아니오 | 1 | `ISO885916` |
| `MULE_INTERNAL` | Mule internal code | 다국어 Emacs | 예 | 아니오 | 1–4 | - |
| `SJIS` | Shift JIS | 일본어 | 아니오 | 아니오 | 1–2 | `Mskanji`, `ShiftJIS`, `WIN932`, `Windows932` |
| `SHIFT_JIS_2004` | Shift JIS, JIS X 0213 | 일본어 | 아니오 | 아니오 | 1–2 | - |
| `SQL_ASCII` | 미지정 (텍스트 참조) | 모두 | 예 | 아니오 | 1 | - |
| `UHC` | Unified Hangul Code | 한국어 | 아니오 | 아니오 | 1–2 | `WIN949`, `Windows949` |
| `UTF8` | Unicode, 8비트 | 모두 | 예 | 예 | 1–4 | `Unicode` |
| `WIN866` | Windows CP866 | 키릴 | 예 | 예 | 1 | `ALT` |
| `WIN874` | Windows CP874 | 태국어 | 예 | 아니오 | 1 | - |
| `WIN1250` | Windows CP1250 | 중앙 유럽어 | 예 | 예 | 1 | - |
| `WIN1251` | Windows CP1251 | 키릴 | 예 | 예 | 1 | `WIN` |
| `WIN1252` | Windows CP1252 | 서유럽어 | 예 | 예 | 1 | - |
| `WIN1253` | Windows CP1253 | 그리스어 | 예 | 예 | 1 | - |
| `WIN1254` | Windows CP1254 | 터키어 | 예 | 예 | 1 | - |
| `WIN1255` | Windows CP1255 | 히브리어 | 예 | 예 | 1 | - |
| `WIN1256` | Windows CP1256 | 아랍어 | 예 | 예 | 1 | - |
| `WIN1257` | Windows CP1257 | 발트어 | 예 | 예 | 1 | - |
| `WIN1258` | Windows CP1258 | 베트남어 | 예 | 예 | 1 | `ABC`, `TCVN`, `TCVN5712`, `VSCII` |

#### SQL_ASCII에 대한 중요 참고사항

`SQL_ASCII` 설정은 다른 설정과 다르게 동작합니다:
- 서버는 바이트 값 0–127을 ASCII 표준에 따라 해석합니다
- 바이트 값 128–255는 해석되지 않은 문자로 취급됩니다
- 인코딩 변환이 발생하지 않습니다
- 이것은 ASCII를 사용한다는 선언이 아니라 인코딩에 대한 무지의 선언 입니다
- 비ASCII 데이터와 함께 `SQL_ASCII`를 사용하는 것은 현명하지 않습니다. PostgreSQL이 비ASCII 문자를 변환하거나 검증할 수 없기 때문입니다

---

### 23.3.2. 문자 집합 설정

#### initdb 사용

PostgreSQL 클러스터의 기본 문자 집합 정의:

```bash
initdb -E EUC_JP
```

또는 긴 형식 사용:

```bash
initdb --encoding EUC_JP
```

`-E` 또는 `--encoding` 옵션이 주어지지 않으면, `initdb`는 지정된 또는 기본 로케일을 기반으로 적절한 인코딩을 결정하려고 시도합니다.

#### 비기본 인코딩으로 데이터베이스 생성

특정 인코딩과 로케일로 데이터베이스 생성 (호환되어야 함):

```bash
createdb -E EUC_KR -T template0 --lc-collate=ko_KR.euckr --lc-ctype=ko_KR.euckr korean
```

또는 SQL 사용:

```sql
CREATE DATABASE korean WITH ENCODING 'EUC_KR'
  LC_COLLATE='ko_KR.euckr'
  LC_CTYPE='ko_KR.euckr'
  TEMPLATE=template0;
```

#### 중요한 제약

항상 `template0` 데이터베이스 복사를 지정하세요. 다른 데이터베이스를 복사할 때는 인코딩 및 로케일 설정을 소스 데이터베이스의 설정에서 변경할 수 없습니다. 이렇게 하면 데이터가 손상될 수 있기 때문입니다.

#### 데이터베이스 인코딩 보기

`psql`을 사용하여 데이터베이스의 인코딩 확인:

```bash
$ psql -l
```

출력 예:

```
                                     List of databases
   Name    |  Owner   | Encoding  |  Collation  |    Ctype    |          Access Privileges
-----------+----------+-----------+-------------+-------------+-------------------------------------
 clocaledb | hlinnaka | SQL_ASCII | C           | C           |
 englishdb | hlinnaka | UTF8      | en_GB.UTF8  | en_GB.UTF8  |
 japanese  | hlinnaka | UTF8      | ja_JP.UTF8  | ja_JP.UTF8  |
 korean    | hlinnaka | EUC_KR    | ko_KR.euckr | ko_KR.euckr |
 postgres  | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  |
 template0 | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  | {=c/hlinnaka,hlinnaka=CTc/hlinnaka}
 template1 | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  | {=c/hlinnaka,hlinnaka=CTc/hlinnaka}
(7 rows)
```

또는 psql에서:

```
\l
```

#### 중요 경고

1. LC_CTYPE/인코딩 일치: 대부분의 현대 운영 체제에서 PostgreSQL은 `LC_CTYPE` 설정에 의해 암시되는 문자 집합을 결정하고 일치하는 데이터베이스 인코딩만 사용되도록 강제합니다. 오래된 시스템에서는 호환성을 확인해야 합니다.

2. SQL_ASCII 위험: PostgreSQL은 `LC_CTYPE`이 `C` 또는 `POSIX`가 아닌 경우에도 슈퍼유저가 `SQL_ASCII` 인코딩으로 데이터베이스를 생성하도록 허용합니다. 그러나 `SQL_ASCII`는 특정 인코딩을 강제하지 않으므로, 이는 로케일 종속 오동작의 위험을 제기하며 권장되지 않습니다.

---

### 23.3.3. 서버와 클라이언트 간 자동 문자 집합 변환

PostgreSQL은 많은 조합에 대해 서버와 클라이언트 간 자동 문자 집합 변환을 지원합니다 (Section 23.3.4 참조).

#### 자동 변환 활성화 방법

##### 1. psql `\encoding` 명령 사용

클라이언트 인코딩을 즉시 변경:

```
\encoding SJIS
```

##### 2. libpq 함수 사용

libpq (Section 32.11)는 클라이언트 인코딩을 제어하는 함수를 제공합니다.

##### 3. SQL 명령 사용

SQL을 사용하여 클라이언트 인코딩 설정:

```sql
SET CLIENT_ENCODING TO 'value';
```

또는 표준 SQL 구문 사용:

```sql
SET NAMES 'value';
```

현재 클라이언트 인코딩 쿼리:

```sql
SHOW client_encoding;
```

기본 인코딩으로 복귀:

```sql
RESET client_encoding;
```

##### 4. PGCLIENTENCODING 환경 변수 사용

연결 시 자동으로 인코딩을 선택하도록 클라이언트 환경에서 정의:

```bash
export PGCLIENTENCODING=SJIS
```

##### 5. client_encoding 구성 변수 사용

연결 시 자동으로 인코딩을 선택하도록 구성에서 설정 (다른 방법으로 재정의 가능).

#### 오류 처리

특정 문자의 변환이 불가능한 경우 오류가 보고됩니다. 예:
- 서버 인코딩: `EUC_JP`
- 클라이언트 인코딩: `LATIN1`
- 문제: `LATIN1` 표현이 없는 일본어 문자가 오류를 발생시킴

#### SQL_ASCII 클라이언트 동작

클라이언트 문자 집합이 `SQL_ASCII`인 경우:
- 서버의 문자 집합에 관계없이 인코딩 변환이 비활성화됨
- 서버의 문자 집합이 `SQL_ASCII`가 아닌 경우, 서버는 여전히 들어오는 데이터가 해당 인코딩에 유효한지 확인함
- 순 효과: 클라이언트 문자 집합이 서버의 것과 일치하는 것처럼 동작
- 용도: 모든 ASCII 데이터로 작업할 때만 현명함

---

### 23.3.4. 사용 가능한 문자 집합 변환

PostgreSQL은 `pg_conversion` 시스템 카탈로그에 변환 함수가 나열된 두 문자 집합 간의 변환을 허용합니다. PostgreSQL에는 사전 정의된 변환이 포함되어 있습니다.

#### 내장 클라이언트/서버 문자 집합 변환

표 23.4. 내장 클라이언트/서버 문자 집합 변환

| 서버 문자 집합 | 사용 가능한 클라이언트 문자 집합 |
|---------------|--------------------------------|
| `BIG5` | 서버 인코딩으로 지원되지 않음 |
| `EUC_CN` | EUC_CN, MULE_INTERNAL, UTF8 |
| `EUC_JP` | EUC_JP, MULE_INTERNAL, SJIS, UTF8 |
| `EUC_JIS_2004` | EUC_JIS_2004, SHIFT_JIS_2004, UTF8 |
| `EUC_KR` | EUC_KR, MULE_INTERNAL, UTF8 |
| `EUC_TW` | EUC_TW, BIG5, MULE_INTERNAL, UTF8 |
| `GB18030` | 서버 인코딩으로 지원되지 않음 |
| `GBK` | 서버 인코딩으로 지원되지 않음 |
| `ISO_8859_5` | ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN866, WIN1251 |
| `ISO_8859_6` | ISO_8859_6, UTF8 |
| `ISO_8859_7` | ISO_8859_7, UTF8 |
| `ISO_8859_8` | ISO_8859_8, UTF8 |
| `JOHAB` | 서버 인코딩으로 지원되지 않음 |
| `KOI8R` | KOI8R, ISO_8859_5, MULE_INTERNAL, UTF8, WIN866, WIN1251 |
| `KOI8U` | KOI8U, UTF8 |
| `LATIN1` | LATIN1, MULE_INTERNAL, UTF8 |
| `LATIN2` | LATIN2, MULE_INTERNAL, UTF8, WIN1250 |
| `LATIN3` | LATIN3, MULE_INTERNAL, UTF8 |
| `LATIN4` | LATIN4, MULE_INTERNAL, UTF8 |
| `LATIN5` | LATIN5, UTF8 |
| `LATIN6` | LATIN6, UTF8 |
| `LATIN7` | LATIN7, UTF8 |
| `LATIN8` | LATIN8, UTF8 |
| `LATIN9` | LATIN9, UTF8 |
| `LATIN10` | LATIN10, UTF8 |
| `MULE_INTERNAL` | MULE_INTERNAL, BIG5, EUC_CN, EUC_JP, EUC_KR, EUC_TW, ISO_8859_5, KOI8R, LATIN1-4, SJIS, WIN866, WIN1250, WIN1251 |
| `SJIS` | 서버 인코딩으로 지원되지 않음 |
| `SHIFT_JIS_2004` | 서버 인코딩으로 지원되지 않음 |
| `SQL_ASCII` | 모두 (변환이 수행되지 않음) |
| `UHC` | 서버 인코딩으로 지원되지 않음 |
| `UTF8` | 지원되는 모든 인코딩 |
| `WIN866` | WIN866, ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN1251 |
| `WIN874` | WIN874, UTF8 |
| `WIN1250` | WIN1250, LATIN2, MULE_INTERNAL, UTF8 |
| `WIN1251` | WIN1251, ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN866 |
| `WIN1252` | WIN1252, UTF8 |
| `WIN1253` | WIN1253, UTF8 |
| `WIN1254` | WIN1254, UTF8 |
| `WIN1255` | WIN1255, UTF8 |
| `WIN1256` | WIN1256, UTF8 |
| `WIN1257` | WIN1257, UTF8 |
| `WIN1258` | WIN1258, UTF8 |

#### 사용자 정의 변환 생성

다음을 사용하여 새 변환 생성:

```sql
CREATE CONVERSION
```

클라이언트/서버 자동 변환에 사용하려면 변환이 해당 문자 집합 쌍에 대해 "기본"으로 표시되어야 합니다.

#### 모든 내장 문자 집합 변환

표 23.5 는 모든 내장 문자 집합 변환을 나열합니다. 주목할 만한 예시:

| 변환 이름 | 소스 인코딩 | 대상 인코딩 |
|-----------|-------------|-------------|
| `big5_to_euc_tw` | BIG5 | EUC_TW |
| `euc_jp_to_sjis` | EUC_JP | SJIS |
| `euc_jp_to_utf8` | EUC_JP | UTF8 |
| `utf8_to_euc_jp` | UTF8 | EUC_JP |
| `sjis_to_utf8` | SJIS | UTF8 |
| `utf8_to_sjis` | UTF8 | SJIS |
| `iso_8859_1_to_utf8` | LATIN1 | UTF8 |
| `utf8_to_iso_8859_1` | UTF8 | LATIN1 |
| `windows_1250_to_utf8` | WIN1250 | UTF8 |
| `utf8_to_windows_1250` | UTF8 | WIN1250 |

*(문서에서 100개 이상의 변환에 대한 전체 목록을 사용할 수 있음)*

---

### 23.3.5. 추가 자료

#### 권장 자료

1. CJKV Information Processing: Chinese, Japanese, Korean & Vietnamese Computing
   - `EUC_JP`, `EUC_CN`, `EUC_KR`, `EUC_TW`에 대한 자세한 설명 포함

2. Unicode Consortium
   - 웹사이트: https://www.unicode.org/

3. RFC 3629
   - UTF-8 (8비트 UCS/유니코드 변환 형식) 정의
   - URL: https://datatracker.ietf.org/doc/html/rfc3629

---

## 요약 표: 핵심 사항

| 주제 | 세부사항 |
|------|----------|
| 기본 설정 | `initdb -E`로 지정 |
| 데이터베이스별 | 생성 시 데이터베이스별로 재정의 가능 |
| 로케일 호환성 | LC_CTYPE 및 LC_COLLATE와 일치해야 함 |
| 서버 인코딩 | 서버 측 사용에 23개 지원 |
| 클라이언트 인코딩 | 클라이언트 측에서 39개 이상의 인코딩 지원 |
| 자동 변환 | SET NAMES 또는 \encoding을 통해 활성화 |
| 변환 방법 | 100개 이상의 내장 변환 함수 |
| UTF-8 장점 | 지원되는 모든 인코딩으로/에서 변환 |

---

## 참고 문서

- [PostgreSQL 18 공식 문서 - Localization](https://www.postgresql.org/docs/current/charset.html)
- [PostgreSQL 18 공식 문서 - Locale Support](https://www.postgresql.org/docs/current/locale.html)
- [PostgreSQL 18 공식 문서 - Collation Support](https://www.postgresql.org/docs/current/collation.html)
- [PostgreSQL 18 공식 문서 - Character Set Support](https://www.postgresql.org/docs/current/multibyte.html)
