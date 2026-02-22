# Chapter 12. 전문 검색 (Full Text Search)

PostgreSQL 18 공식 문서 기반

---

## 목차

- [12.1 소개](#121-소개)
  - [12.1.1 문서란 무엇인가](#1211-문서란-무엇인가)
  - [12.1.2 기본 텍스트 매칭](#1212-기본-텍스트-매칭)
  - [12.1.3 구성](#1213-구성)
- [12.2 테이블과 인덱스](#122-테이블과-인덱스)
  - [12.2.1 테이블 검색](#1221-테이블-검색)
  - [12.2.2 인덱스 생성](#1222-인덱스-생성)
- [12.3 텍스트 검색 제어](#123-텍스트-검색-제어)
  - [12.3.1 문서 파싱](#1231-문서-파싱)
  - [12.3.2 쿼리 파싱](#1232-쿼리-파싱)
  - [12.3.3 검색 결과 순위 지정](#1233-검색-결과-순위-지정)
  - [12.3.4 결과 강조](#1234-결과-강조)
- [12.4 추가 기능](#124-추가-기능)
  - [12.4.1 문서 조작](#1241-문서-조작)
  - [12.4.2 쿼리 조작](#1242-쿼리-조작)
  - [12.4.3 자동 업데이트 트리거](#1243-자동-업데이트-트리거)
  - [12.4.4 문서 통계 수집](#1244-문서-통계-수집)
- [12.5 파서](#125-파서)
- [12.6 사전](#126-사전)
  - [12.6.1 불용어](#1261-불용어)
  - [12.6.2 Simple 사전](#1262-simple-사전)
  - [12.6.3 Synonym 사전](#1263-synonym-사전)
  - [12.6.4 Thesaurus 사전](#1264-thesaurus-사전)
  - [12.6.5 Ispell 사전](#1265-ispell-사전)
  - [12.6.6 Snowball 사전](#1266-snowball-사전)
- [12.7 구성 예제](#127-구성-예제)
- [12.8 텍스트 검색 테스트 및 디버깅](#128-텍스트-검색-테스트-및-디버깅)
  - [12.8.1 구성 테스트](#1281-구성-테스트)
  - [12.8.2 파서 테스트](#1282-파서-테스트)
  - [12.8.3 사전 테스트](#1283-사전-테스트)
- [12.9 전문 검색을 위한 선호 인덱스 유형](#129-전문-검색을-위한-선호-인덱스-유형)
- [12.10 psql 지원](#1210-psql-지원)
- [12.11 제한사항](#1211-제한사항)

---

## 12.1 소개

전문 검색(Full Text Searching)은 자연 언어 문서 의 집합에서 쿼리를 만족하는 문서를 찾아내고, 선택적으로 쿼리와의 관련성에 따라 정렬하는 기능을 제공합니다. 가장 일반적인 검색 유형은 주어진 쿼리 용어를 포함하는 모든 문서를 찾아 쿼리와의 유사도 순서로 반환하는 것입니다. `쿼리`와 `유사도`의 개념은 매우 유연하며 특정 애플리케이션에 따라 달라집니다. 가장 단순한 검색은 쿼리를 단어들의 집합으로, 유사도를 문서 내 쿼리 단어의 빈도로 간주합니다.

### 기존 텍스트 검색 연산자의 한계

텍스트 검색 연산자는 데이터베이스에서 수년간 존재해왔습니다. PostgreSQL은 텍스트 데이터 타입에 대해 `~`, `~*`, `LIKE`, `ILIKE` 연산자를 제공하지만, 현대적인 정보 시스템에서 요구되는 몇 가지 필수 속성이 부족합니다:

- 언어 지원 부재: 정규 표현식으로는 단어의 파생형을 쉽게 처리할 수 없습니다. 예를 들어 `satisfies`와 `satisfy`를 처리하지 못합니다. 필요한 모든 형태를 나열하기 위해 OR을 사용할 수 있지만, 이는 번거롭고 오류가 발생하기 쉽습니다(일부 단어는 수천 개의 파생형을 가질 수 있음).

- 검색 결과 정렬 불가: 수천 개의 일치하는 문서가 발견될 때 순위를 매기는 방법을 제공하지 않습니다.

- 느린 성능: 인덱스 지원이 없으므로 모든 검색에서 모든 문서를 처리해야 합니다.

### 전문 인덱싱의 전처리 과정

전문 인덱싱을 통해 문서를 사전 처리하고 이후 검색에 사용되는 인덱스를 저장할 수 있습니다. 사전 처리에는 다음이 포함됩니다:

#### 1. 토큰화 (문서를 토큰으로 파싱)

각 토큰 클래스(숫자, 단어, 복합어, 이메일 주소 등)를 식별하여 다르게 처리할 수 있도록 합니다. 원칙적으로 토큰 클래스는 특정 애플리케이션에 따라 다르지만, 대부분의 목적에서 미리 정의된 클래스 집합을 사용하는 것이 적절합니다. PostgreSQL은 파서(parser) 를 사용하여 이 단계를 수행합니다. 표준 파서가 제공되며, 특정 요구사항에 맞게 사용자 정의 파서를 만들 수 있습니다.

#### 2. 렉심 변환 (토큰을 렉심으로 변환)

렉심(lexeme)은 토큰과 같은 문자열이지만, 같은 단어의 다른 형태들이 유사하게 만들어지도록 정규화 됩니다. 예를 들어, 정규화에는 거의 항상 대문자를 소문자로 변환하는 것이 포함되며, 종종 접미사 제거(예: 영어의 `s` 또는 `es`)가 포함됩니다. 이를 통해 같은 단어의 다른 문법적 형태를 사용한 검색이 문서를 일치시킬 수 있으며, 특정 형태가 문서에 존재하지 않아도 됩니다. 또한, 이 단계에서는 일반적으로 불용어(stop words), 즉 너무 흔해서 검색에 쓸모없는 단어를 제거합니다. (간단히 말해, 토큰은 문서 텍스트의 원시 조각이고, 렉심은 인덱싱과 검색에 유용하다고 간주되는 단어입니다.) PostgreSQL은 사전(dictionaries) 을 사용하여 이 단계를 수행합니다. 다양한 표준 사전이 제공되며, 특정 요구사항에 맞게 사용자 정의 사전을 만들 수 있습니다.

#### 3. 전처리된 문서 저장

검색에 최적화된 형태로 저장합니다. 예를 들어, 각 문서는 정규화된 렉심의 정렬된 배열로 표현될 수 있습니다. 근접성 순위 지정(proximity ranking)과 함께 렉심의 위치 정보를 저장하는 것이 바람직합니다. 이를 통해 서로 인접한 쿼리 단어를 포함하는 문서가 떨어져 있는 문서보다 높은 순위를 받을 수 있습니다.

### 사전의 기능

사전을 통해 토큰이 정규화되는 방식을 세밀하게 제어할 수 있습니다. 적절한 사전을 사용하여 다음을 수행할 수 있습니다:

- 인덱싱하지 않을 불용어 정의
- Ispell을 사용한 동의어 매핑
- 시소러스를 사용한 구문을 단일 단어로 매핑
- Ispell 사전을 사용하여 단어의 다양한 변형을 정규형으로 매핑
- Snowball stemmer 규칙을 사용하여 단어의 다양한 변형을 정규형으로 매핑

### 데이터 타입 및 연산자

전처리된 문서를 저장하기 위해 `tsvector` 데이터 타입이 제공되고, 처리된 쿼리를 표현하기 위해 `tsquery` 타입이 제공됩니다(8.11절 참조). 이러한 데이터 타입에 사용할 수 있는 많은 함수와 연산자가 있으며(9.13절 참조), 그 중 가장 중요한 것은 매치 연산자 `@@`로, 12.1.2절에서 소개합니다. 전문 검색은 인덱스를 사용하여 속도를 높일 수 있습니다(12.9절 참조).

---

### 12.1.1 문서란 무엇인가

문서 는 전문 검색 시스템에서 검색의 단위입니다. 예를 들어, 잡지 기사나 이메일 메시지입니다. 텍스트 검색 엔진은 문서를 파싱하고 부모 문서에 대한 참조와 함께 렉심(키워드)을 저장할 수 있어야 합니다. 이후 검색은 쿼리를 포함하는 문서를 찾는 데 사용됩니다.

PostgreSQL에서 검색의 경우, 문서는 일반적으로 데이터베이스 테이블 내의 텍스트 필드 또는 필드들의 조합(연결)입니다. 가능한 경우 여러 테이블에 저장되거나 동적으로 획득될 수 있습니다. 즉, 문서는 인덱싱을 위해 다른 소스에서 구성될 수 있습니다. 예를 들어:

예제 1: 단일 테이블

```sql
SELECT title || ' ' ||  author || ' ' ||  abstract || ' ' || body AS document
FROM messages
WHERE mid = 12;
```

예제 2: 여러 테이블 결합

```sql
SELECT m.title || ' ' || m.author || ' ' || m.abstract || ' ' || d.body AS document
FROM messages m, docs d
WHERE m.mid = d.did AND m.mid = 12;
```

> 참고: 실제로 이러한 쿼리에서는 `NULL` 값을 처리하기 위해 `coalesce`를 사용해야 합니다. 다른 필드가 `NULL`이 아니더라도 하나의 `NULL` 속성이 `NULL` 결과를 만들 수 있습니다.

또 다른 가능성은 파일 시스템에 단순 텍스트 파일로 문서를 저장하는 것입니다. 이 경우 데이터베이스는 전문 인덱스를 저장하고 검색을 실행하는 데 사용될 수 있으며, 검색된 문서를 파일 시스템에서 가져오기 위해 고유 식별자를 사용할 수 있습니다. 그러나 데이터베이스 외부에서 파일을 검색하려면 슈퍼유저 권한이 필요하거나 특별한 함수 지원이 필요하므로, 일반적으로 데이터베이스 내에 모든 데이터를 유지하는 것이 편리합니다. 또한 데이터베이스 내에 모든 것을 유지하면 문서 메타데이터에 쉽게 접근하여 인덱싱 및 표시를 지원할 수 있습니다.

텍스트 검색 목적상, 각 문서는 전처리된 `tsvector` 형식으로 축소되어야 합니다. 검색과 순위 지정은 전적으로 문서의 `tsvector` 표현에서 수행됩니다 - 원본 텍스트는 사용자에게 표시하기 위해 선택된 문서에 대해서만 검색하면 됩니다. 따라서 `tsvector`를 문서의 간략한 요약으로 생각할 수 있습니다.

---

### 12.1.2 기본 텍스트 매칭

PostgreSQL에서 전문 검색은 매치 연산자 `@@`를 기반으로 합니다. 이 연산자는 `tsvector`(문서)가 `tsquery`(쿼리)와 일치하면 `true`를 반환합니다. 어떤 데이터 타입이 먼저 작성되든 상관없습니다:

```sql
SELECT 'a fat cat sat on a mat and ate a fat rat'::tsvector @@ 'cat & rat'::tsquery;
 ?column?
----------
 t
```

```sql
SELECT 'fat & cow'::tsquery @@ 'a fat cat sat on a mat and ate a fat rat'::tsvector;
 ?column?
----------
 f
```

위에서 언급했듯이, `tsquery`는 `@@` 연산자가 검색할 검색어를 포함하며, 검색어는 `&`(AND), `|`(OR), `!`(NOT), `<->`(FOLLOWED BY) 연산자를 사용하여 결합될 수 있습니다. 예를 들어, 쿼리 `'fat & cow'`는 `fat`과 `cow`를 모두 포함하는 줄과만 일치합니다.

### 정규화의 중요성

위의 예에서 주목할 점은 `rats`와 같은 단어를 일치시키지 않는다는 것입니다. 왜냐하면 캐스트가 아닌 `tsvector`의 요소들이 렉심이고, 이는 `rat`과 같은 단어가 정규화되지 않았다고 가정하기 때문입니다. 다음은 동작하지 않습니다:

```sql
SELECT 'fat cats ate fat rats'::tsvector @@ to_tsquery('fat & rat');
 ?column?
----------
 f
```

`rat`은 `rats`와 일치하지 않습니다.

`to_tsvector` 함수는 토큰을 정규화하고 불용어를 제거하여 적절한 `tsvector`를 생성하는 데 사용해야 합니다(자세한 내용은 12.3.1절 참조):

```sql
SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('fat & rat');
 ?column?
----------
 t
```

### 매치 연산자의 변형

`@@` 연산자는 `tsquery`를 암묵적으로 변환하는 `text` 입력도 지원합니다:

| 형식 | 설명 |
|------|------|
| `tsvector @@ tsquery` | 기본 형식 |
| `tsquery @@ tsvector` | 순서 반대 |
| `text @@ tsquery` | `to_tsvector(text) @@ tsquery`와 동일 |
| `text @@ text` | `to_tsvector(x) @@ plainto_tsquery(y)`와 동일 |

`tsquery`보다 `text @@ text` 형식이 더 빠르게 입력할 수 있지만, 덜 유용합니다. 이 형식은 가중치, 접두사 매칭 또는 구문 검색 연산자를 지원하지 않습니다.

### 논리 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `&` (AND) | 두 인자가 모두 나타나야 함 | `fat & rat` |
| `\|` (OR) | 인자 중 하나 이상이 나타나야 함 | `fat \| cow` |
| `!` (NOT) | 인자가 나타나지 않아야 함 | `fat & ! rat` |

### 구문 검색: FOLLOWED BY 연산자 `<->`

`tsquery`에서 구문 검색을 지정하려면 `<->` (FOLLOWED BY) `tsquery` 연산자를 사용합니다. 이 연산자는 왼쪽과 오른쪽 인자가 서로 인접하여 해당 순서로 나타날 때만 일치합니다. 예를 들어:

```sql
SELECT to_tsvector('fatal error') @@ to_tsquery('fatal <-> error');
 ?column?
----------
 t

SELECT to_tsvector('error is not fatal') @@ to_tsquery('fatal <-> error');
 ?column?
----------
 f
```

### 일반화된 FOLLOWED BY 연산자 `<N>`

FOLLOWED BY 연산자의 더 일반적인 버전은 `<N>` 형식으로 작성되며, 여기서 `N`은 일치하는 렉심 위치 간의 차이를 나타내는 정수입니다:

- `<1>`: `<->`와 동일 (인접)
- `<2>`: 정확히 하나의 다른 렉심이 중간에 나타남
- `<0>`: 두 패턴이 같은 단어와 일치해야 함

```sql
SELECT phraseto_tsquery('cats ate rats');
       phraseto_tsquery
-------------------------------
 'cat' <-> 'ate' <-> 'rat'

SELECT phraseto_tsquery('the cats ate the rats');
       phraseto_tsquery
-------------------------------
 'cat' <-> 'ate' <2> 'rat'
```

> 참고: `phraseto_tsquery`는 불용어 위치를 고려하여 `<N>`을 구성합니다.

### 연산자 우선순위

괄호를 사용하지 않을 때 연산자의 우선순위는 (낮음에서 높음으로):

1. `|` (OR) - 가장 낮음
2. `&` (AND)
3. `<->` (FOLLOWED BY)
4. `!` (NOT) - 가장 높음

예를 들어:

```sql
SELECT to_tsquery('fat & rat & ! cat');
    to_tsquery
------------------
 'fat' & 'rat' & !'cat'
```

### FOLLOWED BY 내에서의 NOT 의미 변화

괄호 없이 `!x <-> y`에서 `!`가 `<->`의 인자로 해석되면, `NOT`은 해당 피연산자가 `y`와 인접하지 않아야 함을 의미합니다. 문서의 다른 곳에 `x`가 존재해도 일치하지 않게 만들지 않습니다:

```sql
SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('!cat <-> ate');
 ?column?
----------
 f

SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('cat <-> !ate');
 ?column?
----------
 f
```

---

### 12.1.3 구성

위의 내용은 모두 간단한 텍스트 예제였습니다. 앞서 언급했듯이, 전문 검색 기능에는 다음과 같은 기능이 포함됩니다:

- 특정 단어의 인덱싱 생략(불용어)
- 동의어 처리
- 어간 추출(stemming)을 사용한 정교한 구문 분석

이 모든 것은 텍스트 검색 구성(text search configurations) 에 의해 제어됩니다.

### PostgreSQL 텍스트 검색의 4가지 구성 요소

PostgreSQL에는 많은 언어에 대한 미리 정의된 구성이 제공되며, 사용자 정의 구성도 쉽게 만들 수 있습니다. psql의 `\dF` 명령은 사용 가능한 모든 구성을 보여줍니다.

설치 중에 적절한 구성이 선택되고 `postgresql.conf`에서 `default_text_search_config`가 그에 맞게 설정됩니다. 전체 클러스터에 동일한 텍스트 검색 구성을 사용하는 경우, `postgresql.conf`에서 값을 사용할 수 있습니다. 클러스터 내의 다른 데이터베이스에서 다른 구성을 사용하려면, `ALTER DATABASE ... SET`을 사용합니다. 그렇지 않으면 각 세션에서 `SET default_text_search_config`를 설정할 수 있습니다.

구성에 의존하는 각 텍스트 검색 함수에는 사용할 구성을 명시적으로 지정하기 위한 선택적 `regconfig` 인자가 있습니다. `default_text_search_config`는 이 인자가 생략된 경우에만 사용됩니다.

사용자 정의 텍스트 검색 구성을 더 쉽게 만들기 위해, 구성은 더 간단한 데이터베이스 객체로 구축됩니다:

1. 텍스트 검색 파서: 문서 텍스트를 토큰으로 분할하고 각 토큰을 분류합니다(예: 단어 또는 숫자로)
2. 텍스트 검색 사전: 토큰을 정규화된 형태로 변환하고 불용어를 거부합니다
3. 텍스트 검색 템플릿: 사전의 기반이 되는 함수를 제공합니다(사전 자체는 단순히 템플릿에 대한 매개변수를 지정합니다)
4. 텍스트 검색 구성: 문서 분석에 사용할 파서를 선택하고, 파서 출력 토큰을 사전으로 변환하기 위한 사전-토큰 매핑을 지정합니다

텍스트 검색 파서와 템플릿은 저수준 C 함수로 구축됩니다. 따라서 새로운 것을 개발하려면 C 프로그래밍에 능숙해야 하며, 새로운 것을 설치하려면 슈퍼유저 권한이 필요합니다(12.11절의 예제 참조). 기존의 파서와 템플릿은 많은 언어에 대해 사용 가능하므로 대부분의 사용자는 자체 파서를 만들 필요가 없습니다. 사전과 구성을 만드는 데는 특별한 권한이 필요 없으며, 이 장에서 나중에 예제를 제공합니다.

---

## 12.2 테이블과 인덱스

이전 섹션의 예제들은 단순 상수 문자열과의 전문 매칭을 설명했습니다. 이 섹션에서는 테이블 데이터를 검색하는 방법을 보여주며, 선택적으로 인덱스를 사용합니다.

---

### 12.2.1 테이블 검색

인덱스 없이도 전문 검색을 수행할 수 있습니다. 다음은 `body` 필드에서 `friend` 단어를 포함하는 각 행의 `title`을 출력하는 간단한 쿼리입니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector('english', body) @@ to_tsquery('english', 'friend');
```

특징:

- `friends`, `friendly` 등 관련 단어도 찾습니다(어간 추출로 정규화된 렉심으로 축소되기 때문)
- `english` 구성을 사용하여 문자열 파싱 및 정규화를 수행합니다

또한 위의 쿼리는 명시적으로 구성 이름을 지정하지 않고 작성할 수 있습니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(body) @@ to_tsquery('friend');
```

이 쿼리는 `default_text_search_config`에 의해 설정된 구성을 사용합니다.

### 복잡한 예제

`title` 또는 `body`에서 `create`와 `table`을 포함하는 가장 최신 문서 10개를 선택합니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(title || ' ' || body) @@ to_tsquery('create & table')
ORDER BY last_mod_date DESC
LIMIT 10;
```

> 참고: 명확성을 위해 위에서 `coalesce` 함수 호출을 생략했지만, 두 필드 중 하나가 `NULL`인 행을 찾으려면 필요합니다.

### 성능 고려사항

이러한 쿼리들은 인덱스 없이도 동작하지만, 대부분의 애플리케이션에서 이 접근 방식은 너무 느립니다. 간헐적인 임시 검색을 제외하고 실제 텍스트 검색 사용에는 일반적으로 인덱스 생성이 필요합니다.

---

### 12.2.2 인덱스 생성

검색 속도를 높이기 위해 인덱스를 생성할 수 있습니다. PostgreSQL은 텍스트 검색에 대해 GIN 및 GiST 인덱스 유형을 지원합니다(12.9절 참조). 인덱스가 매우 유용하지만 전문 검색에 필수는 아닙니다.

### 방법 1: GIN 표현식 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector('english', body));
```

중요 규칙:

- `to_tsvector`의 2-인자 버전을 사용해야 합니다
- 구성 이름을 명시적으로 지정하는 텍스트 검색 함수만 표현식 인덱스에 사용할 수 있습니다
- 이는 인덱스 내용이 `default_text_search_config`에 영향을 받지 않아야 하기 때문입니다

쿼리 사용:

```sql
-- 인덱스를 사용함 (같은 구성)
SELECT title
FROM pgweb
WHERE to_tsvector('english', body) @@ to_tsquery('english', 'friend');

-- 인덱스를 사용하지 않음 (구성 미지정)
SELECT title
FROM pgweb
WHERE to_tsvector(body) @@ to_tsquery('friend');
```

### 방법 2: 다중 구성 지원 표현식 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector(config_name, body));
```

- `config_name`은 `pgweb` 테이블의 열입니다
- 같은 인덱스에서 혼합 구성이 가능합니다(각 항목에 대해 어떤 구성이 사용되었는지 기록)
- 다국어 문서 컬렉션에 유용합니다

쿼리:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(config_name, body) @@ to_tsquery('english', 'friend');
```

### 방법 3: 열 연결 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector('english', title || ' ' || body));
```

### 방법 4: 별도의 tsvector 열 (저장된 생성 열)

별도의 `tsvector` 열을 생성하여 `to_tsvector`의 출력을 저장합니다. 저장된 생성 열(stored generated column)을 사용하면 원본 데이터에서 자동으로 업데이트됩니다:

```sql
ALTER TABLE pgweb
    ADD COLUMN textsearchable_index_col tsvector
               GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))) STORED;
```

GIN 인덱스 생성:

```sql
CREATE INDEX textsearch_idx ON pgweb USING GIN (textsearchable_index_col);
```

빠른 검색 실행:

```sql
SELECT title
FROM pgweb
WHERE textsearchable_index_col @@ to_tsquery('create & table')
ORDER BY last_mod_date DESC
LIMIT 10;
```

### 방법 비교

| 방법 | 장점 | 단점 |
|------|------|------|
| 표현식 인덱스 | 설정이 간단, 디스크 공간 절약 | 쿼리에서 구성 명시 필요, 검증 시 `tsvector` 재계산 필요 |
| 별도 열 | 구성 명시 불필요, 더 빠른 검색, `default_text_search_config` 사용 가능 | 더 많은 디스크 공간, 설정이 더 복잡 |

---

## 12.3 텍스트 검색 제어

전문 검색을 구현하려면 문서를 `tsvector`로, 사용자 쿼리를 `tsquery`로 변환하는 함수가 필요합니다. 또한 결과를 유용한 순서로 반환하고 결과를 보기 좋게 표시해야 합니다. PostgreSQL은 이 모든 기능을 지원합니다.

---

### 12.3.1 문서 파싱

PostgreSQL은 문서를 `tsvector` 타입으로 변환하는 `to_tsvector` 함수를 제공합니다.

```sql
to_tsvector([ config regconfig, ] document text) returns tsvector
```

`to_tsvector`는 텍스트 문서를 토큰으로 파싱하고, 토큰을 렉심으로 축소하며, 각 렉심의 위치와 함께 `tsvector`를 반환합니다. 문서는 지정된 또는 기본 텍스트 검색 구성에 따라 처리됩니다. 다음은 간단한 예입니다:

```sql
SELECT to_tsvector('english', 'a fat cat sat on a mat - it ate a fat rats');
                  to_tsvector
-----------------------------------------------------
 'ate':9 'cat':3 'fat':2,11 'mat':7 'rat':12 'sat':4
```

위의 예에서 볼 수 있듯이 결과 `tsvector`에는 불용어(a, on, it)가 포함되지 않고, `rats`가 `rat`으로 어간 추출되었으며, 구두점 기호 `-`가 무시되었습니다.

`to_tsvector` 함수는 내부적으로 파서를 호출하여 문서 텍스트를 토큰으로 분할하고 각 토큰에 타입을 할당합니다. 각 토큰에 대해 사전 목록이 참조되며(12.6절), 이 목록은 토큰 타입에 따라 달라질 수 있습니다. 토큰을 인식하는 첫 번째 사전이 하나 이상의 정규화된 렉심을 출력하여 결과를 나타냅니다. 또는, 사전이 토큰을 불용어로 인식하거나 토큰이 어떤 사전에도 인식되지 않으면 무시되고 인덱싱되지 않습니다. PostgreSQL은 많은 언어에 대한 미리 정의된 구성을 제공하며, 자체 구성을 쉽게 만들 수 있습니다(psql의 `\dF` 명령은 모든 사용 가능한 구성을 표시합니다).

### setweight 함수

```sql
setweight(vector tsvector, weight "char") returns tsvector
```

`setweight`는 `tsvector`의 모든 위치에 주어진 가중치를 레이블로 지정한 입력 벡터의 복사본을 반환합니다. 가중치는 문자 `A`, `B`, `C` 또는 `D`입니다. `D`는 새 벡터의 기본값이므로 출력에 표시되지 않습니다:

```sql
SELECT setweight(to_tsvector('fat cats ate rats'), 'A');
            setweight
----------------------------------
 'ate':3A 'cat':2A 'fat':1A 'rat':4A
```

가중치는 문서의 다른 부분(예: 제목 대 본문)을 표시하는 데 일반적으로 사용됩니다. 나중에 이 정보는 검색 결과의 순위를 매기는 데 사용될 수 있습니다.

가중치가 서로 다른 벡터들은 연결하여 결합할 수 있으므로, 문서의 다른 부분을 다르게 표시하면서 한 번의 연결 작업으로 `tsvector`를 생성하는 것이 중요합니다:

```sql
UPDATE tt SET ti =
    setweight(to_tsvector(coalesce(title,'')), 'A')    ||
    setweight(to_tsvector(coalesce(keyword,'')), 'B')  ||
    setweight(to_tsvector(coalesce(abstract,'')), 'C') ||
    setweight(to_tsvector(coalesce(body,'')), 'D');
```

여기서 `coalesce`를 사용하여 `NULL` 필드가 다른 필드의 결과에 영향을 주지 않도록 합니다.

---

### 12.3.2 쿼리 파싱

PostgreSQL은 쿼리를 `tsquery` 타입으로 변환하는 함수 `to_tsquery`, `plainto_tsquery`, `phraseto_tsquery` 및 `websearch_to_tsquery`를 제공합니다.

#### to_tsquery 함수

```sql
to_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`to_tsquery`는 `querytext`로부터 `tsquery` 값을 생성합니다. `querytext`는 `tsquery` 연산자 `&` (AND), `|` (OR), `!` (NOT), `<->` (FOLLOWED BY)로 구분된 단일 토큰으로 구성되어야 합니다. 괄호로 그룹화할 수 있습니다. 각 토큰은 지정된 또는 기본 구성을 사용하여 렉심으로 정규화됩니다. 예를 들어:

```sql
SELECT to_tsquery('english', 'The & Fat & Rats');
  to_tsquery
---------------
 'fat' & 'rat'
```

위의 예에서 볼 수 있듯이 `to_tsquery`는 불용어를 버릴 뿐만 아니라 쿼리가 유효하도록 그것을 고려하는 연산자도 버립니다.

#### 가중치 지정

가중치를 각 렉심에 붙여 특정 가중치를 가진 `tsvector` 렉심만 일치하도록 제한할 수 있습니다:

```sql
SELECT to_tsquery('english', 'Fat | Rats:AB');
    to_tsquery
------------------
 'fat' | 'rat':AB
```

#### 접두사 매칭

렉심에 `*`를 붙여 접두사가 일치하도록 지정할 수 있습니다:

```sql
SELECT to_tsquery('supern:*A & star:A*B');
        to_tsquery
--------------------------
 'supern':*A & 'star':*AB
```

#### 구문 검색

쌍따옴표로 구문을 지정할 수 있습니다:

```sql
SELECT to_tsquery('''supernovae stars'' & !crab');
  to_tsquery
---------------
 'sn' & !'crab'
```

#### plainto_tsquery 함수

```sql
plainto_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`plainto_tsquery`는 형식이 지정되지 않은 텍스트 `querytext`를 `tsquery` 값으로 변환합니다. 텍스트는 `to_tsvector`와 마찬가지로 파싱되고 정규화된 후, `&` (AND) 불리언 연산자가 남아 있는 단어들 사이에 삽입됩니다.

```sql
SELECT plainto_tsquery('english', 'The Fat Rats');
 plainto_tsquery
-----------------
 'fat' & 'rat'
```

> 참고: `plainto_tsquery`는 입력에서 어떤 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블도 인식하지 않습니다:

```sql
SELECT plainto_tsquery('english', 'The Fat & Rats:C');
   plainto_tsquery
---------------------
 'fat' & 'rat' & 'c'
```

여기서 입력의 모든 구두점이 공백 기호로 무시되었습니다.

#### phraseto_tsquery 함수

```sql
phraseto_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`phraseto_tsquery`는 `plainto_tsquery`와 비슷하게 동작하지만, `&` (AND) 불리언 연산자 대신 `<->` (FOLLOWED BY) 연산자를 남아 있는 단어들 사이에 삽입합니다. 또한 불용어는 단순히 버려지지 않고 `<N>` 연산자에서 고려됩니다. 이 함수는 특정 렉심 시퀀스를 검색하는 데 유용합니다(예: 사전 테스트 용도).

```sql
SELECT phraseto_tsquery('english', 'The Fat Rats');
 phraseto_tsquery
------------------
 'fat' <-> 'rat'

SELECT phraseto_tsquery('english', 'The Fat and Rats');
   phraseto_tsquery
------------------------
 'fat' <2> 'rat'
```

`plainto_tsquery`와 마찬가지로 `phraseto_tsquery` 함수는 입력에서 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블을 인식하지 않습니다.

#### websearch_to_tsquery 함수

```sql
websearch_to_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`websearch_to_tsquery`는 일반적인 웹 검색 엔진에서 사용되는 것과 유사한 대체 구문을 사용하여 `querytext`에서 `tsquery` 값을 생성합니다.

지원 구문:

- 따옴표 없는 텍스트: `plainto_tsquery`처럼 `&` 연산자로 구분된 단어들로 변환
- "따옴표 텍스트": `phraseto_tsquery`처럼 `<->` 연산자로 구분된 단어들로 변환
- OR: `|` 연산자로 변환
- -: `!` (NOT) 연산자로 변환

다른 구두점은 무시됩니다. 따라서 `plainto_tsquery`나 `phraseto_tsquery`와 마찬가지로 `websearch_to_tsquery` 함수는 입력에서 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블을 인식하지 않습니다.

```sql
SELECT websearch_to_tsquery('english', 'The fat rats');
 websearch_to_tsquery
----------------------
 'fat' & 'rat'

SELECT websearch_to_tsquery('english', '"supernovae stars" -crab');
       websearch_to_tsquery
----------------------------------
 'supernova' <-> 'star' & !'crab'

SELECT websearch_to_tsquery('english', '"sad cat" or "fat rat"');
       websearch_to_tsquery
-----------------------------------
 'sad' <-> 'cat' | 'fat' <-> 'rat'

SELECT websearch_to_tsquery('english', 'signal -"segmentation fault"');
         websearch_to_tsquery
---------------------------------------
 'signal' & !( 'segment' <-> 'fault' )

SELECT websearch_to_tsquery('english', '""" )( dummy \\ query <->');
 websearch_to_tsquery
----------------------
 'dummi' & 'queri'
```

---

### 12.3.3 검색 결과 순위 지정

순위 지정은 문서가 특정 쿼리와 얼마나 관련 있는지를 측정하여 가장 관련 있는 문서를 먼저 표시하려고 시도합니다. PostgreSQL은 두 가지 미리 정의된 순위 지정 함수를 제공합니다. 이 함수들은 텍스트 정보, 근접 정보 및 구조 정보를 고려합니다. 즉, 쿼리 용어가 문서에 얼마나 자주, 얼마나 가깝게 나타나는지, 그리고 문서의 어느 부분에 나타나는지를 고려합니다. 그러나 관련성의 개념은 모호하고 매우 애플리케이션별로 다릅니다. 다른 애플리케이션은 정렬 순서를 계산하기 위해 추가 정보(예: 문서 수정 시간)가 필요할 수 있습니다. 내장 순위 지정 함수는 예제일 뿐입니다. 특정 요구 사항에 따라 자체 순위 지정 함수를 작성하거나 그 결과를 추가 요소와 결합할 수 있습니다.

#### ts_rank 함수

```sql
ts_rank([ weights float4[], ] vector tsvector, query tsquery [, normalization integer ]) returns float4
```

일치하는 렉심의 빈도를 기반으로 벡터의 순위를 지정합니다.

#### ts_rank_cd 함수

```sql
ts_rank_cd([ weights float4[], ] vector tsvector, query tsquery [, normalization integer ]) returns float4
```

커버 밀도(cover density) 순위를 계산합니다. 이 함수는 일치하는 렉심 간의 근접성을 고려한다는 점에서 `ts_rank`와 유사합니다.

> 참고: 이 순위 지정 방법은 렉심 위치 정보를 요구하므로 "제거된" 렉심은 무시됩니다. 입력에 제거되지 않은 렉심이 없으면 결과는 0이 됩니다. (위치 정보 없는 `tsvector`에 대한 쿼리의 자세한 내용은 12.4.1절 참조)

#### 가중치 배열

두 함수 모두 선택적 `weights` 인자를 사용하여 단어 인스턴스를 레이블에 따라 다르게 가중치 부여할 수 있습니다. 가중치 배열은 각 단어 범주에 부여할 가중치를 다음 순서로 지정합니다:

```
{D-weight, C-weight, B-weight, A-weight}
```

가중치가 제공되지 않으면 다음 기본값이 사용됩니다:

```
{0.1, 0.2, 0.4, 1.0}
```

일반적인 가중치 사용은 제목과 같은 특수 영역의 단어에 본문 단어보다 높은 가중치를 부여하는 것입니다.

#### 정규화(Normalization) 옵션

두 순위 함수는 정수 `normalization` 옵션을 사용하여 문서 길이에 따라 문서 순위를 계산하는 방법을 지정합니다. 정수 옵션은 여러 동작을 지정합니다. 따라서 비트마스크입니다: `|`를 사용하여 여러 동작을 지정할 수 있습니다(예: `2|4`).

| 값 | 설명 |
|---|------|
| 0 | (기본값) 문서 길이 무시 |
| 1 | 순위를 1 + 문서 길이의 로그로 나눔 |
| 2 | 순위를 문서 길이로 나눔 |
| 4 | 순위를 익스텐트 내 평균 조화 거리로 나눔(이는 `ts_rank_cd`에 의해서만 구현됨) |
| 8 | 순위를 문서 내 고유 단어 수로 나눔 |
| 16 | 순위를 1 + 문서 내 고유 단어 수의 로그로 나눔 |
| 32 | 순위를 자체 + 1로 나눔 |

> 참고: 32 옵션을 사용하면 모든 순위가 0에서 1 범위로 스케일링되지만, 물론 이것은 단조로운 변환이므로 검색 결과의 순서에 영향을 미치지 않습니다.

순위 함수는 전역 정보를 사용하지 않으므로 때때로 원하는 대로 1%와 99% 임계값과 같은 예를 생성하는 것이 불가능합니다. 정규화 옵션 32(`rank/(rank+1)`)는 모든 스케일에 적용하여 순위를 0에서 1 사이 범위로 변환하는 데 사용할 수 있지만, 물론 이것은 단지 화장적인 변경입니다. 실제 스케일링에 영향을 미치는 것은 정규화 옵션 2 뿐입니다.

예제 1: 상위 10개 순위 지정 결과

```sql
SELECT title, ts_rank_cd(textsearch, query) AS rank
FROM apod, to_tsquery('neutrino|(dark & matter)') query
WHERE query @@ textsearch
ORDER BY rank DESC
LIMIT 10;

                     title                     |   rank
-----------------------------------------------+----------
 Neutrinos in the Sun                          |      3.1
 The Sudbury Neutrino Detector                 |      2.4
 A MACHO View of Galactic Dark Matter          |  2.01317
 Hot Gas and Dark Matter                       |  1.91171
 The Virgo Cluster: Hot Plasma and Dark Matter |  1.90953
 Rafting for Solar Neutrinos                   |      1.9
 NGC 4650A: Strange Galaxy and Dark Matter     |  1.85774
 Hot Gas and Dark Matter                       |   1.6123
 Ice Fishing for Cosmic Neutrinos              |      1.6
 Weak Lensing Distorts the Universe            | 0.818218
```

예제 2: 정규화된 순위 (rank/(rank+1))

```sql
SELECT title, ts_rank_cd(textsearch, query, 32 /* rank/(rank+1) */ ) AS rank
FROM apod, to_tsquery('neutrino|(dark & matter)') query
WHERE query @@ textsearch
ORDER BY rank DESC
LIMIT 10;

                     title                     |        rank
-----------------------------------------------+-------------------
 Neutrinos in the Sun                          | 0.756097569485493
 The Sudbury Neutrino Detector                 | 0.705882361190954
 A MACHO View of Galactic Dark Matter          | 0.668123210574724
 Hot Gas and Dark Matter                       |  0.65655958650282
 The Virgo Cluster: Hot Plasma and Dark Matter | 0.656301290640973
 Rafting for Solar Neutrinos                   | 0.655172410958162
 NGC 4650A: Strange Galaxy and Dark Matter     | 0.650072921219637
 Hot Gas and Dark Matter                       | 0.617195790024749
 Ice Fishing for Cosmic Neutrinos              | 0.615384618911517
 Weak Lensing Distorts the Universe            | 0.450010798361481
```

> 성능 주의: 순위 지정은 각 일치하는 문서의 `tsvector`를 검색해야 하므로 비용이 많이 들 수 있습니다. 이것은 일치하는 문서가 많은 경우 종종 "병목 현상"이 됩니다. 불행히도, 이것을 피하기가 거의 불가능합니다. 순위 지정은 일치 정보에 접근해야 하기 때문입니다.

---

### 12.3.4 결과 강조

검색 결과를 표시하기 위해 문서의 일부를 보여주고 쿼리와 관련된 부분을 이상적으로 표시하는 것이 바람직합니다. PostgreSQL은 이 기능을 구현하는 `ts_headline` 함수를 제공합니다.

```sql
ts_headline([ config regconfig, ] document text, query tsquery [, options text ]) returns text
```

`ts_headline`은 쿼리와 일치하는 것을 강조하여 문서의 발췌를 반환합니다.

옵션:

| 옵션 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| `MaxWords` | 정수 | 최대 헤드라인 길이 | 35 |
| `MinWords` | 정수 | 최소 헤드라인 길이 | 15 |
| `ShortWord` | 정수 | 시작/끝에서 제거할 단어 길이 | 3 |
| `HighlightAll` | 불린 | 전체 문서를 헤드라인으로 사용 | false |
| `MaxFragments` | 정수 | 최대 텍스트 조각 수 (0=비조각 모드) | 0 |
| `StartSel` | 문자열 | 쿼리 단어 구분 시작 문자열 | `<b>` |
| `StopSel` | 문자열 | 쿼리 단어 구분 종료 문자열 | `</b>` |
| `FragmentDelimiter` | 문자열 | 조각 구분자 | ` ... ` |

두 가지 강조 모드가 있습니다:

- 비조각 모드 (MaxFragments=0): 단일 일치 항목을 선택하고 해당 항목을 중심으로 발췌를 표시합니다
- 조각 모드 (MaxFragments>0): 여러 일치 항목을 찾아 각각을 별도의 조각으로 표시합니다

> 경고 (Cross-site Scripting 안전): `ts_headline` 출력은 웹 페이지에 직접 포함되기에 안전하지 않습니다. 원본 문서의 HTML 마크업이 포함될 수 있기 때문입니다. HTML sanitizer를 사용하거나 HTML 마크업을 모두 제거해야 합니다.

예제 1: 기본 강조

```sql
SELECT ts_headline('english',
  'The most common type of search
is to find all documents containing given query terms
and return them in order of their similarity to the
query.',
  to_tsquery('english', 'query & similarity'));

                        ts_headline
------------------------------------------------------------
 containing given <b>query</b> terms
 and return them in order of their <b>similarity</b> to the
 <b>query</b>.
```

예제 2: 커스텀 옵션

```sql
SELECT ts_headline('english',
  'Search terms may occur
many times in a document,
requiring ranking of the search matches to decide which
occurrences to display in the result.',
  to_tsquery('english', 'search & term'),
  'MaxFragments=10, MaxWords=7, MinWords=3, StartSel=<<, StopSel=>>');

                        ts_headline
------------------------------------------------------------
 <<Search>> <<terms>> may occur
 many times ... ranking of the <<search>> matches to decide
```

> 성능 주의: `ts_headline`은 `tsvector` 요약이 아닌 원본 문서를 사용하므로 느릴 수 있으며 신중하게 사용해야 합니다. 대부분의 전문 검색 애플리케이션은 다음과 같이 구성됩니다: (1) 검색 쿼리 실행, (2) 상위 N개의 일치 문서를 가져옴, (3) 이 N개 문서에 대해서만 `ts_headline` 호출.

---

## 12.4 추가 기능

이 섹션에서는 문서와 쿼리 조작, 자동 업데이트 트리거, 문서 통계 수집에 유용한 추가 함수와 연산자를 설명합니다.

---

### 12.4.1 문서 조작

12.3.1절에서 원시 텍스트 문서가 `tsvector`로 어떻게 변환되는지 보여주었습니다. PostgreSQL은 또한 이미 `tsvector` 형태인 문서를 조작하는 함수와 연산자를 제공합니다.

#### tsvector 연결 연산자 (`||`)

```sql
tsvector || tsvector
```

`tsvector` 연결 연산자는 두 `tsvector`에서 렉심과 위치 정보를 결합한 벡터를 반환합니다. 오른쪽 벡터의 위치는 왼쪽 벡터의 최대 위치만큼 오프셋됩니다. 가중치 레이블도 유지됩니다. 이 연산자와 동등한 함수는 `tsvector_concat`입니다.

연결의 중요한 용도 중 하나는 문서의 다른 부분을 다른 가중치로 표시하는 것입니다. 예를 들어, 제목 단어에 본문 단어보다 더 높은 가중치를 부여할 수 있습니다:

```sql
UPDATE tt SET ti =
    setweight(to_tsvector(coalesce(title,'')), 'A')    ||
    setweight(to_tsvector(coalesce(keyword,'')), 'B')  ||
    setweight(to_tsvector(coalesce(abstract,'')), 'C') ||
    setweight(to_tsvector(coalesce(body,'')), 'D');
```

#### setweight() 함수

```sql
setweight(vector tsvector, weight "char") returns tsvector
```

입력 벡터의 모든 위치에 가중치(A, B, C, D)를 할당합니다. D는 기본값이며 출력에 표시되지 않습니다.

> 참고: 가중치는 렉심이 아닌 위치 에 적용됩니다.

#### length() 함수

```sql
length(vector tsvector) returns integer
```

벡터에 저장된 렉심의 개수를 반환합니다.

#### strip() 함수

```sql
strip(vector tsvector) returns tsvector
```

위치 및 가중치 정보 없이 렉심 목록을 반환합니다. 이렇게 하면 크기가 더 작아지지만 관련성 순위 기능이 약해집니다.

> 참고: `<->` (FOLLOWED BY) `tsquery` 연산자는 제거된 `tsvector` 인자와 절대 일치하지 않습니다. 일치 시 렉심 위치가 필요하기 때문입니다.

---

### 12.4.2 쿼리 조작

12.3.2절에서 원시 텍스트 쿼리가 `tsquery` 값으로 어떻게 변환되는지 보여주었습니다. PostgreSQL은 또한 이미 `tsquery` 형태인 쿼리를 조작하는 함수와 연산자를 제공합니다.

#### AND 연결 (`&&`)

```sql
tsquery && tsquery
```

두 쿼리의 AND 조합을 반환합니다.

#### OR 연결 (`||`)

```sql
tsquery || tsquery
```

두 쿼리의 OR 조합을 반환합니다.

#### NOT 연산 (`!!`)

```sql
!! tsquery
```

쿼리의 부정(NOT)을 반환합니다.

#### FOLLOWED BY 연산 (`<->`)

```sql
tsquery <-> tsquery
```

첫 번째 쿼리 다음에 두 번째 쿼리가 바로 따라오는 경우를 검색하는 쿼리를 반환합니다.

```sql
SELECT to_tsquery('fat') <-> to_tsquery('cat | rat');
          ?column?
----------------------------
 'fat' <-> ( 'cat' | 'rat' )
```

#### tsquery_phrase() 함수

```sql
tsquery_phrase(query1 tsquery, query2 tsquery [, distance integer]) returns tsquery
```

정확히 `distance` 렉심 거리에 있는 두 쿼리의 일치를 검색하는 쿼리를 만듭니다. 예를 들어:

```sql
SELECT tsquery_phrase(to_tsquery('fat'), to_tsquery('cat'), 10);
  tsquery_phrase
------------------
 'fat' <10> 'cat'
```

#### numnode() 함수

```sql
numnode(query tsquery) returns integer
```

`tsquery`의 노드(렉심 + 연산자) 개수를 반환합니다. 이 함수는 쿼리가 의미 있는지 확인하는 데 유용합니다(0을 반환하면 불용어만 포함):

```sql
SELECT numnode(plainto_tsquery('the any'));
NOTICE:  query contains only stopword(s) or doesn't contain lexeme(s), ignored
 numnode
---------
       0

SELECT numnode('foo & bar'::tsquery);
 numnode
---------
       3
```

#### querytree() 함수

```sql
querytree(query tsquery) returns text
```

인덱스 검색에 사용할 수 있는 `tsquery` 부분을 반환합니다. 이 함수는 검색할 수 없는 쿼리(예: 불용어만 포함하거나 부정만 포함)를 감지하는 데 유용합니다:

```sql
SELECT querytree(to_tsquery('defined'));
 querytree
-----------
 'defin'

SELECT querytree(to_tsquery('!defined'));
 querytree
-----------
 T
```

---

#### 12.4.2.1 쿼리 재작성

`ts_rewrite` 함수군은 `tsquery` 내에서 대상 부분쿼리의 모든 발생을 대체 부분쿼리로 바꿉니다. 본질적으로 이 연산은 대체할 것을 찾는 것을 제외하면 문자열에서의 부분 문자열 치환과 비슷합니다. 대상과 대체의 조합을 재작성 규칙이라고 할 수 있습니다. 이러한 규칙의 모음이 강력한 검색 도구가 될 수 있습니다. 예를 들어, `supernovae`를 `supernovae|sn`으로 확장하거나 원래 쿼리에서 직접 검색하지 않고 규칙을 통해 검색을 지시하는 데 사용할 수 있습니다.

##### ts_rewrite() - 단일 규칙

```sql
ts_rewrite(query tsquery, target tsquery, substitute tsquery) returns tsquery
```

이 `ts_rewrite` 형식은 단순히 단일 재작성 규칙을 적용합니다: `query`에서 `target`이 나타나는 곳마다 `substitute`로 대체됩니다. 예를 들어:

```sql
SELECT ts_rewrite('a & b'::tsquery, 'a'::tsquery, 'c'::tsquery);
 ts_rewrite
------------
 'b' & 'c'
```

##### ts_rewrite() - 테이블 기반

```sql
ts_rewrite(query tsquery, select text) returns tsquery
```

이 `ts_rewrite` 형식은 텍스트 문자열로 제공된 `SELECT` 명령의 결과에서 재작성 규칙 집합을 가져와서 적용합니다. `SELECT`는 두 개의 `tsquery` 타입 열을 반환해야 합니다. 각 행에서 첫 번째 열 값(대상)이 현재 쿼리 값에서 두 번째 열 값(대체)으로 대체됩니다. 예를 들어:

```sql
CREATE TABLE aliases (t tsquery PRIMARY KEY, s tsquery);
INSERT INTO aliases VALUES('a', 'c');

SELECT ts_rewrite('a & b'::tsquery, 'SELECT t,s FROM aliases');
 ts_rewrite
------------
 'b' & 'c'
```

##### 실제 예제 - 천문학 관련

천문학 데이터의 실제 쿼리 재작성 예:

```sql
CREATE TABLE aliases (t tsquery primary key, s tsquery);
INSERT INTO aliases VALUES(to_tsquery('supernovae'), to_tsquery('supernovae|sn'));

SELECT ts_rewrite(to_tsquery('supernovae & crab'), 'SELECT * FROM aliases');
           ts_rewrite
---------------------------------
 'crab' & ( 'supernova' | 'sn' )
```

규칙 업데이트:

```sql
UPDATE aliases
SET s = to_tsquery('supernovae|sn & !nebulae')
WHERE t = to_tsquery('supernovae');

SELECT ts_rewrite(to_tsquery('supernovae & crab'), 'SELECT * FROM aliases');
                 ts_rewrite
---------------------------------------------
 'crab' & ( 'supernova' | 'sn' & !'nebula' )
```

##### 성능 최적화 - 포함 연산자 사용

재작성 규칙이 많으면 각 규칙 확인이 느려질 수 있습니다. 가능한 후보를 사전 필터링하기 위해 포함 연산자를 사용할 수 있습니다. 예를 들어:

```sql
SELECT ts_rewrite('a & b'::tsquery,
                  'SELECT t,s FROM aliases WHERE ''a & b''::tsquery @> t');
 ts_rewrite
------------
 'b' & 'c'
```

---

### 12.4.3 자동 업데이트 트리거

> 참고: 이 방법은 저장된 생성 열(stored generated columns) 사용으로 대체되었습니다(12.2.2절 참조).

별도의 열을 사용하여 문서의 `tsvector` 표현을 저장할 때, 문서 내용 열이 변경될 때마다 `tsvector` 열을 업데이트해야 합니다. PostgreSQL은 이를 자동으로 수행하는 두 가지 내장 트리거 함수를 제공합니다.

```sql
tsvector_update_trigger(tsvector_column_name, config_name, text_column_name [, ... ])
tsvector_update_trigger_column(tsvector_column_name, config_column_name, text_column_name [, ... ])
```

예제:

```sql
CREATE TABLE messages (
    title       text,
    body        text,
    tsv         tsvector
);

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON messages FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(tsv, 'pg_catalog.english', title, body);

INSERT INTO messages VALUES('title here', 'the body text is here');

SELECT * FROM messages;
   title    |         body          |            tsv
------------+-----------------------+----------------------------
 title here | the body text is here | 'bodi':4 'text':5 'titl':1

SELECT title, body FROM messages WHERE tsv @@ to_tsquery('title & body');
   title    |         body
------------+-----------------------
 title here | the body text is here
```

##### 커스텀 트리거 - PL/pgSQL 예제

문서 부분에 다른 가중치를 설정하거나 `tsvector`를 수정해야 하는 경우, 사용자 정의 트리거 함수를 작성해야 합니다. 다음은 PL/pgSQL 예제입니다:

```sql
CREATE FUNCTION messages_trigger() RETURNS trigger AS $$
begin
  new.tsv :=
     setweight(to_tsvector('pg_catalog.english', coalesce(new.title,'')), 'A') ||
     setweight(to_tsvector('pg_catalog.english', coalesce(new.body,'')), 'D');
  return new;
end
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
    ON messages FOR EACH ROW EXECUTE FUNCTION messages_trigger();
```

> 중요: 내장 트리거든 사용자 정의 트리거든, 쿼리에서 스키마 한정 구성 이름을 지정해야 합니다. 그렇지 않으면 텍스트 검색 함수가 `search_path`의 변경으로 잘못된 구성을 사용할 수 있습니다.

---

### 12.4.4 문서 통계 수집

`ts_stat` 함수는 구성 테스트와 더 빠른 검색을 위한 불용어 후보 찾기에 유용합니다.

```sql
ts_stat(sqlquery text, [weights text,]
        OUT word text, OUT ndoc integer,
        OUT nentry integer) returns setof record
```

`sqlquery`는 단일 `tsvector` 열을 반환하는 SQL 쿼리의 텍스트 값입니다. `ts_stat`는 해당 쿼리를 실행하고 `tsvector` 데이터에 포함된 각 고유 렉심(단어)에 대한 통계를 반환합니다.

반환 열:

| 열 | 타입 | 설명 |
|----|------|------|
| `word` | text | 렉심 값 |
| `ndoc` | integer | 단어가 나타난 문서 수 |
| `nentry` | integer | 단어의 총 발생 횟수 |

`weights`가 제공되면 지정된 가중치를 가진 발생만 계산됩니다.

예제 1: 가장 빈번한 10개 단어

```sql
SELECT * FROM ts_stat('SELECT vector FROM apod')
ORDER BY nentry DESC, ndoc DESC, word
LIMIT 10;
```

예제 2: 가중치 A 또는 B만 계산

```sql
SELECT * FROM ts_stat('SELECT vector FROM apod', 'ab')
ORDER BY nentry DESC, ndoc DESC, word
LIMIT 10;
```

---

## 12.5 파서

텍스트 검색 파서는 원시 문서 텍스트를 토큰 으로 분할하고 각 토큰의 타입 을 식별하는 역할을 합니다. 여기서 가능한 타입 집합은 파서 자체에 의해 정의됩니다. 파서는 텍스트를 수정하지 않으며, 단순히 단어 경계를 식별합니다. 이 제한된 범위 때문에, 애플리케이션별 사용자 정의 파서의 필요성이 사용자 정의 사전의 필요성보다 적습니다. 현재 PostgreSQL은 다양한 목적에 적합한 것으로 입증된 단일 내장 파서만 제공합니다.

내장 파서의 이름은 `pg_catalog.default`입니다. 이 파서는 23가지 토큰 타입을 인식합니다:

| 별칭 | 설명 | 예제 |
|------|------|------|
| `asciiword` | ASCII 문자만으로 구성된 단어 | `elephant` |
| `word` | 모든 문자로 구성된 단어 | `mañana` |
| `numword` | 문자와 숫자로 구성된 단어 | `beta1` |
| `asciihword` | 하이픈이 있는 ASCII 단어 | `up-to-date` |
| `hword` | 하이픈이 있는 문자 단어 | `lógico-matemática` |
| `numhword` | 하이픈이 있는 문자+숫자 단어 | `postgresql-beta1` |
| `hword_asciipart` | 하이픈 단어 부분(ASCII) | `postgresql` (in `postgresql-beta1`) |
| `hword_part` | 하이픈 단어 부분(문자) | `lógico` (in `lógico-matemática`) |
| `hword_numpart` | 하이픈 단어 부분(문자+숫자) | `beta1` (in `postgresql-beta1`) |
| `email` | 이메일 주소 | `foo@example.com` |
| `protocol` | 프로토콜 헤드 | `http://` |
| `url` | URL | `example.com/stuff/index.html` |
| `host` | 호스트 | `example.com` |
| `url_path` | URL 경로 | `/stuff/index.html` |
| `file` | 파일 또는 경로명 | `/usr/local/foo.txt` |
| `sfloat` | 과학적 표기법 | `-1.234e56` |
| `float` | 십진 표기법 | `-1.234` |
| `int` | 부호 있는 정수 | `-1234` |
| `uint` | 부호 없는 정수 | `1234` |
| `version` | 버전 번호 | `8.3.0` |
| `tag` | XML 태그 | `<a href="dictionaries.html">` |
| `entity` | XML 엔티티 | `&amp;` |
| `blank` | 공백 기호 | (공백 또는 인식되지 않은 기호) |

### 주요 특징 및 제한사항

#### 1. 로케일(Locale) 의존성

"문자"의 정의는 데이터베이스의 로케일 설정, 특히 `lc_ctype`에 의해 결정됩니다. ASCII 문자만 포함하는 단어는 별도의 토큰 타입으로 보고됩니다. 대부분의 유럽 언어에서는 `word`와 `asciiword` 토큰 타입을 동일하게 처리해야 합니다.

#### 2. 이메일 제한사항

RFC 5322의 모든 유효한 이메일 문자를 지원하지 않습니다. 사용자명에서 지원되는 비영숫자 문자: 마침표(.), 대시(-), 언더스코어(_)만 지원합니다.

#### 3. 겹치는 토큰

파서는 동일한 텍스트에서 여러 개의 겹치는 토큰을 생성할 수 있습니다. 예를 들어, 하이픈이 있는 단어는 전체 단어와 각 구성 요소 모두 보고됩니다:

```sql
SELECT alias, description, token FROM ts_debug('foo-bar-beta1');
      alias      |               description                |     token
-----------------+------------------------------------------+---------------
 numhword        | Hyphenated word, letters and digits      | foo-bar-beta1
 hword_asciipart | Hyphenated word part, all ASCII          | foo
 blank           | Space symbols                            | -
 hword_asciipart | Hyphenated word part, all ASCII          | bar
 blank           | Space symbols                            | -
 hword_numpart   | Hyphenated word part, letters and digits | beta1
```

이 동작 덕분에 전체 복합어(`foo-bar-beta1`)와 각 구성 요소(`foo`, `bar`, `beta1`) 모두에 대해 검색할 수 있습니다.

URL도 유사하게 처리됩니다:

```sql
SELECT alias, description, token FROM ts_debug('http://example.com/stuff/index.html');
  alias   |  description  |            token
----------+---------------+------------------------------
 protocol | Protocol head | http://
 url      | URL           | example.com/stuff/index.html
 host     | Host          | example.com
 url_path | URL path      | /stuff/index.html
```

---

## 12.6 사전

사전은 전문 검색에서 두 가지 주요 역할을 합니다:

1. 불용어 제거: 검색에서 무시해야 할 단어(너무 흔해서 검색에 쓸모없는 단어) 제거
2. 단어 정규화: 같은 단어의 다양한 형태를 하나의 렉심으로 통합

정규화와 불용어 제거를 통해 `tsvector` 표현의 크기를 줄여 성능을 개선합니다.

### 정규화의 예시

정규화가 반드시 언어학적 작업을 의미하는 것은 아닙니다:

- 언어학적 정규화: Ispell 사전으로 단어를 정규형으로 축소하거나 Stemmer로 어미 제거
- URL 정규화: 동등한 URL들을 하나로 정규화
- 색상명 정규화: 색상 이름을 16진수 값으로 변환
- 숫자 정규화: 소수점 자릿수 제한

### 사전의 동작 원리

사전은 입력 토큰에 대해 다음 중 하나를 반환합니다:

| 반환 값 | 설명 |
|---------|------|
| 렉심 배열 | 토큰이 사전에서 인식된 경우 (하나의 토큰이 여러 렉심을 생성할 수 있음) |
| TSL_FILTER 플래그가 있는 단일 렉심 | 토큰을 변경하여 다음 사전으로 전달 (필터링 사전) |
| 빈 배열 | 토큰이 불용어인 경우 |
| NULL | 토큰이 인식되지 않은 경우 |

### 사전 구성의 일반 규칙

좁고 구체적인 순서에서 일반적인 순서로 배열합니다:

```sql
ALTER TEXT SEARCH CONFIGURATION astro_en
    ADD MAPPING FOR asciiword WITH astrosyn, english_ispell, english_stem;
```

이 예에서:
- `astrosyn`: 천문학 동의어 사전 (가장 구체적)
- `english_ispell`: 일반 영어 사전
- `english_stem`: Snowball Stemmer (가장 일반적)

---

### 12.6.1 불용어

불용어는 매우 흔한 단어(거의 모든 문서에 나타남)로 검색 가치가 낮아 무시됩니다. 예를 들어, 영어의 `a`, `the`가 이에 해당합니다. 각 언어마다 불용어 목록이 다르며, 전문 검색 애플리케이션에서 이 목록을 조정해야 할 수도 있습니다.

불용어와 위치의 관계:

불용어는 `tsvector`의 위치에 영향을 줍니다:

```sql
SELECT to_tsvector('english', 'in the list of stop words');
        to_tsvector
----------------------------
 'list':3 'stop':5 'word':6
```

위치 1, 2, 4가 누락되었습니다(불용어: `in`, `the`, `of`). `list`는 위치 3, `stop`은 위치 5, `word`는 위치 6입니다.

불용어의 순위 영향:

```sql
-- 불용어 포함
SELECT ts_rank_cd (to_tsvector('english', 'in the list of stop words'),
                    to_tsquery('list & stop'));
 ts_rank_cd
------------
       0.05

-- 불용어 제외
SELECT ts_rank_cd (to_tsvector('english', 'list stop words'),
                    to_tsquery('list & stop'));
 ts_rank_cd
------------
        0.1
```

---

### 12.6.2 Simple 사전

`simple` 사전 템플릿은 입력 토큰을 소문자로 변환하고 불용어 파일에서 확인합니다. 불용어 목록에 있으면 빈 배열을 반환하여 토큰이 삭제됩니다. 그렇지 않으면 소문자 형태의 단어가 정규화된 렉심으로 반환됩니다.

예제: Simple 사전 정의

```sql
CREATE TEXT SEARCH DICTIONARY public.simple_dict (
    TEMPLATE = pg_catalog.simple,
    STOPWORDS = english
);
```

`english`는 불용어 파일의 기본명입니다. 전체 경로는 `$SHAREDIR/tsearch_data/english.stop`입니다. 파일 형식은 한 줄에 하나의 단어입니다.

동작 테스트:

```sql
-- 불용어가 아닌 단어
SELECT ts_lexize('public.simple_dict', 'YeS');
 ts_lexize
-----------
 {yes}

-- 불용어
SELECT ts_lexize('public.simple_dict', 'The');
 ts_lexize
-----------
 {}
```

#### Accept 파라미터

`Accept` 파라미터의 기본값은 `true`입니다. `Accept = false`로 설정하면:

```sql
ALTER TEXT SEARCH DICTIONARY public.simple_dict ( Accept = false );

-- 불용어가 아닌 단어 -> NULL 반환
SELECT ts_lexize('public.simple_dict', 'YeS');
 ts_lexize
-----------


-- 불용어 -> 빈 배열 반환
SELECT ts_lexize('public.simple_dict', 'The');
 ts_lexize
-----------
 {}
```

| Accept 값 | 위치 | 이유 |
|----------|------|------|
| `true` (기본값) | 목록 끝 | 다음 사전으로 토큰을 전달하지 않음 |
| `false` | 중간 | 인식하지 못한 토큰을 다음 사전으로 전달 |

> 참고: 모든 사전 구성 파일은 UTF-8 인코딩 이어야 합니다. 서버가 다른 인코딩을 사용하면 자동으로 변환됩니다.

---

### 12.6.3 Synonym 사전

동의어 사전은 단어를 동의어로 교체하는 데 사용됩니다. 구문(phrases)은 지원하지 않습니다(구문은 Thesaurus 템플릿 사용).

사용 사례:

언어 문제 해결 예: "Paris"가 어간 추출기에 의해 "pari"로 축소되는 것을 방지:

```sql
-- Before: English stemmer가 "Paris"를 "pari"로 축소
SELECT * FROM ts_debug('english', 'Paris');
   alias   |   description   | token |  dictionaries  |  dictionary  | lexemes
-----------+-----------------+-------+----------------+--------------+---------
 asciiword | Word, all ASCII | Paris | {english_stem} | english_stem | {pari}

-- 사전 생성
CREATE TEXT SEARCH DICTIONARY my_synonym (
    TEMPLATE = synonym,
    SYNONYMS = my_synonyms
);

-- 구성 수정
ALTER TEXT SEARCH CONFIGURATION english
    ALTER MAPPING FOR asciiword
    WITH my_synonym, english_stem;

-- After: Synonym 사전이 먼저 처리하여 "paris" 반환
SELECT * FROM ts_debug('english', 'Paris');
   alias   |   description   | token |       dictionaries        | dictionary | lexemes
-----------+-----------------+-------+---------------------------+------------+---------
 asciiword | Word, all ASCII | Paris | {my_synonym,english_stem} | my_synonym | {paris}
```

#### 구성 파일 형식

```
word1 synonym1
word2 synonym2
...
```

한 줄에 하나의 치환 규칙, 공백으로 단어와 동의어를 구분합니다.

#### 접두사 표시

구성 파일의 동의어 끝에 `*`를 붙이면 접두사로 표시됩니다:

```
indices index*
```

```sql
-- ts_lexize 사용
SELECT ts_lexize('syn', 'indices');
 ts_lexize
-----------
 {index}

-- to_tsquery 사용 (접두사 매치 마커 포함)
SELECT to_tsquery('tst', 'indices');
 to_tsquery
------------
 'index':*
```

---

### 12.6.4 Thesaurus 사전

Thesaurus 사전(TZ)은 단어와 구문의 관계 정보(BT: 광의어, NT: 협의어 등)를 포함하는 단어 집합입니다. Thesaurus 사전은 비선호 용어를 선호 용어로 바꾸고, 선택적으로 원본 용어도 인덱싱할 수 있습니다.

특징:

- Synonym 사전의 확장 버전
- 구문(phrase) 지원 (가장 큰 차이점)

#### 구성 파일 형식

```
# 주석
sample word(s) : indexed word(s)
more sample word(s) : more indexed word(s)
...
```

콜론 `:`이 구문과 치환어를 구분합니다.

#### Subdictionary

Thesaurus 사전은 입력 텍스트를 정규화하기 위해 subdictionary 를 사용합니다. 모든 샘플 단어는 subdictionary에 알려져 있어야 합니다.

아스테리스크를 사용하여 불용어가 나올 수 있는 위치를 지정할 수 있습니다:

```
? one ? two : swsw
```

`a one the two`와 `the one a two` 모두 `swsw`로 치환됩니다.

> 중요: Thesaurus는 인덱싱 중에 사용되므로, 파라미터 변경 시 재인덱싱이 필요 합니다!

#### 12.6.4.1 Thesaurus 구성

사전 정의:

```sql
CREATE TEXT SEARCH DICTIONARY thesaurus_simple (
    TEMPLATE = thesaurus,
    DictFile = mythesaurus,
    Dictionary = pg_catalog.english_stem
);
```

파라미터:

| 파라미터 | 설명 |
|---------|------|
| `DictFile` | Thesaurus 구성 파일 기본명 (전체 경로: `$SHAREDIR/tsearch_data/mythesaurus.ths`) |
| `Dictionary` | Subdictionary (여기선 Snowball English stemmer) |

구성에 바인딩:

```sql
ALTER TEXT SEARCH CONFIGURATION russian
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart
    WITH thesaurus_simple;
```

#### 12.6.4.2 Thesaurus 예제

천문학 Thesaurus 설정:

1단계: Thesaurus 파일 (`thesaurus_astro`)

```
supernovae stars : sn
crab nebulae : crab
```

2단계: 사전 생성

```sql
CREATE TEXT SEARCH DICTIONARY thesaurus_astro (
    TEMPLATE = thesaurus,
    DictFile = thesaurus_astro,
    Dictionary = english_stem
);
```

3단계: 구성 수정

```sql
ALTER TEXT SEARCH CONFIGURATION russian
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart
    WITH thesaurus_astro, english_stem;
```

동작 확인:

```sql
SELECT plainto_tsquery('supernova star');
 plainto_tsquery
-----------------
 'sn'
```

원본 구문도 인덱싱하려면:

```
supernovae stars : sn supernovae stars
```

```sql
SELECT plainto_tsquery('supernova star');
       plainto_tsquery
-----------------------------
 'sn' & 'supernova' & 'star'
```

---

### 12.6.5 Ispell 사전

Ispell 사전 템플릿은 형태소 분석을 지원합니다. 단어의 다양한 언어학적 형태를 같은 렉심으로 정규화할 수 있습니다.

예: English Ispell 사전

단어 `bank`의 모든 형태를 매칭: `banking`, `banked`, `banks`, `banks'`, `bank's`

#### Ispell 사전 생성 절차

1단계: 사전 파일 다운로드

OpenOffice 확장 형식 `.oxt` 파일에서 `.aff` (affixes)와 `.dic` (dictionary) 파일을 추출합니다.

2단계: 파일 인코딩 변환

```bash
iconv -f ISO_8859-1 -t UTF-8 -o nn_no.affix nn_NO.aff
iconv -f ISO_8859-1 -t UTF-8 -o nn_no.dict nn_NO.dic
```

3단계: 파일 복사

```bash
cp nn_no.affix $SHAREDIR/tsearch_data/
cp nn_no.dict $SHAREDIR/tsearch_data/
```

4단계: PostgreSQL에 로드

```sql
CREATE TEXT SEARCH DICTIONARY english_hunspell (
    TEMPLATE = ispell,
    DictFile = en_us,
    AffFile = en_us,
    Stopwords = english
);
```

| 파라미터 | 설명 |
|---------|------|
| `DictFile` | Dictionary 파일 기본명 |
| `AffFile` | Affixes 파일 기본명 |
| `Stopwords` | 불용어 파일 기본명 |

#### 복합어 지원

Ispell은 복합어 분해를 지원합니다:

```sql
SELECT ts_lexize('norwegian_ispell', 'overbuljongterningpakkmesterassistent');
   {over,buljong,terning,pakk,mester,assistent}

SELECT ts_lexize('norwegian_ispell', 'sjokoladefabrikk');
   {sjokoladefabrikk,sjokolade,fabrikk}
```

> 권장사항: Ispell 사전은 제한된 단어 집합만 인식하므로, 더 광범위한 Snowball 사전으로 보완하는 것이 좋습니다.

---

### 12.6.6 Snowball 사전

Snowball 프로젝트는 Martin Porter(Porter's Stemming Algorithm 발명자)가 개발했으며, 다양한 언어의 어간 추출 알고리즘을 제공합니다.

참고: [Snowball 공식 사이트](https://snowballstem.org/)

#### Snowball 사전 생성

```sql
CREATE TEXT SEARCH DICTIONARY english_stem (
    TEMPLATE = snowball,
    Language = english,
    StopWords = english
);
```

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `Language` | 예 | Stemmer 언어 선택 |
| `StopWords` | 아니오 | 불용어 파일명 |

#### 핵심 특징

Snowball 사전은 모든 단어를 인식 합니다(단어를 단순화할 수 있는지 여부와 관계없이).

> 중요: Snowball 사전은 항상 목록의 끝에 배치 해야 합니다. 앞에 배치하면 뒤의 사전에 토큰이 전달되지 않습니다.

예제:

```sql
SELECT to_tsvector('english', 'running runs runner');
 to_tsvector
--------------
 'run':1,2,3
```

---

### 사전 유형 요약

| 사전 유형 | 용도 | 위치 | 특징 |
|-----------|------|------|------|
| Simple | 불용어 제거 | 마지막 | 가장 간단 |
| Synonym | 동의어 치환 | 중간 | 구문 미지원 |
| Thesaurus | 구문 기반 치환 | 중간 | 구문 지원, 재인덱싱 필요 |
| Ispell | 형태소 분석 | 중간 | 제한된 단어 집합 |
| Snowball | 어간 추출 | 마지막 | 모든 단어 인식 |

---

## 12.7 구성 예제

텍스트 검색 구성(Text Search Configuration)은 문서를 `tsvector`로 변환하는 데 필요한 모든 옵션을 지정합니다.

### 주요 구성 요소

- 파서(Parser): 텍스트를 토큰으로 분할
- 사전(Dictionaries): 각 토큰을 렉심으로 변환

`to_tsvector` 또는 `to_tsquery` 호출 시 구성이 필요합니다. 기본 구성은 `default_text_search_config` 파라미터로 지정됩니다.

### 실습 예제: 'pg' 구성 생성

1단계: 기본 구성 복제

```sql
CREATE TEXT SEARCH CONFIGURATION public.pg ( COPY = pg_catalog.english );
```

2단계: 동의어 사전 생성

파일 위치: `$SHAREDIR/tsearch_data/pg_dict.syn`

```
postgres    pg
pgsql       pg
postgresql  pg
```

동의어 사전 정의:

```sql
CREATE TEXT SEARCH DICTIONARY pg_dict (
    TEMPLATE = synonym,
    SYNONYMS = pg_dict
);
```

3단계: Ispell 사전 등록

```sql
CREATE TEXT SEARCH DICTIONARY english_ispell (
    TEMPLATE = ispell,
    DictFile = english,
    AffFile = english,
    StopWords = english
);
```

4단계: 토큰 타입별 매핑 설정

```sql
ALTER TEXT SEARCH CONFIGURATION pg
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart,
                      word, hword, hword_part
    WITH pg_dict, english_ispell, english_stem;
```

5단계: 특정 토큰 타입 제외

```sql
ALTER TEXT SEARCH CONFIGURATION pg
    DROP MAPPING FOR email, url, url_path, sfloat, float;
```

6단계: 구성 테스트

```sql
SELECT * FROM ts_debug('public.pg', '
PostgreSQL, the highly scalable, SQL compliant, open source object-relational
database management system, is now undergoing beta testing of the next
version of our software.
');
```

7단계: 기본 구성 설정

```sql
SET default_text_search_config = 'public.pg';

SHOW default_text_search_config;
 default_text_search_config
----------------------------
 public.pg
```

---

## 12.8 텍스트 검색 테스트 및 디버깅

사용자 정의 텍스트 검색 구성의 동작이 복잡해질 수 있으므로, PostgreSQL은 텍스트 검색 객체를 테스트하기 위한 유틸리티 함수들을 제공합니다.

---

### 12.8.1 구성 테스트

#### ts_debug 함수

```sql
ts_debug([ config regconfig, ] document text,
         OUT alias text,
         OUT description text,
         OUT token text,
         OUT dictionaries regdictionary[],
         OUT dictionary regdictionary,
         OUT lexemes text[])
         returns setof record
```

`ts_debug`는 완전한 텍스트 검색 구성을 테스트하여 문서의 모든 토큰에 대한 정보를 표시합니다.

반환 열:

| 열명 | 타입 | 설명 |
|------|------|------|
| `alias` | text | 토큰 타입의 짧은 이름 |
| `description` | text | 토큰 타입 설명 |
| `token` | text | 토큰의 텍스트 |
| `dictionaries` | regdictionary[] | 이 토큰 타입에 대해 선택된 사전들 |
| `dictionary` | regdictionary | 토큰을 인식한 사전, 또는 NULL |
| `lexemes` | text[] | 인식한 사전이 생성한 렉심, NULL 또는 빈 배열 {} (불용어) |

예제:

```sql
SELECT * FROM ts_debug('english', 'a fat  cat sat on a mat - it ate a fat rats');
   alias   |   description   | token |  dictionaries  |  dictionary  | lexemes
-----------+-----------------+-------+----------------+--------------+---------
 asciiword | Word, all ASCII | a     | {english_stem} | english_stem | {}
 blank     | Space symbols   |       | {}             |              |
 asciiword | Word, all ASCII | fat   | {english_stem} | english_stem | {fat}
 blank     | Space symbols   |       | {}             |              |
 asciiword | Word, all ASCII | cat   | {english_stem} | english_stem | {cat}
 ...
```

결과 해석:

- "a" → 빈 배열 {} = 불용어 (인덱싱 안 됨)
- "fat" → {fat}, "rats" → {rat} (어간 추출 적용)
- 공백 → dictionaries가 비어 있음 (제외됨)

---

### 12.8.2 파서 테스트

#### ts_parse 함수

```sql
ts_parse(parser_name text, document text,
         OUT tokid integer, OUT token text) returns setof record
```

주어진 문서를 파싱하고 각 토큰에 대한 정보를 반환합니다.

```sql
SELECT * FROM ts_parse('default', '123 - a number');
 tokid | token
-------+--------
    22 | 123
    12 |
    12 | -
     1 | a
    12 |
     1 | number
```

#### ts_token_type 함수

```sql
ts_token_type(parser_name text, OUT tokid integer,
              OUT alias text, OUT description text) returns setof record
```

파서가 인식할 수 있는 각 토큰 타입을 설명합니다.

```sql
SELECT * FROM ts_token_type('default');
 tokid |      alias      |               description
-------+-----------------+------------------------------------------
     1 | asciiword       | Word, all ASCII
     2 | word            | Word, all letters
     ...
```

---

### 12.8.3 사전 테스트

#### ts_lexize 함수

```sql
ts_lexize(dict regdictionary, token text) returns text[]
```

사전이 토큰을 어떻게 처리하는지 테스트합니다.

| 반환 값 | 의미 |
|---------|------|
| 렉심 배열 | 토큰이 사전에서 인식됨 |
| 빈 배열 {} | 토큰이 불용어임 |
| NULL | 토큰이 인식되지 않음 |

```sql
SELECT ts_lexize('english_stem', 'stars');
 ts_lexize
-----------
 {star}

SELECT ts_lexize('english_stem', 'a');
 ts_lexize
-----------
 {}
```

> 중요: `ts_lexize`는 단일 토큰 만 처리합니다. 텍스트 파싱을 하지 않습니다. Thesaurus 사전의 구문 테스트에는 `plainto_tsquery`나 `to_tsvector`를 사용하세요.

---

## 12.9 전문 검색을 위한 선호 인덱스 유형

전문 검색 속도를 높이기 위해 두 가지 인덱스 유형을 사용할 수 있습니다: GIN 과 GiST. 인덱스는 필수는 아니지만, 정기적으로 검색되는 열에는 일반적으로 권장됩니다.

### 인덱스 생성 방법

#### GIN 인덱스

```sql
CREATE INDEX name ON table USING GIN (column);
```

- 특징: GIN (Generalized Inverted Index) 기반
- 열 타입: `tsvector` 필수
- 구조: 각 단어(렉심)마다 인덱스 항목 생성, 압축된 위치 목록 포함

#### GiST 인덱스

```sql
CREATE INDEX name ON table USING GIST (column [ { DEFAULT | tsvector_ops } (siglen = number) ] );
```

- 특징: GiST (Generalized Search Tree) 기반
- 열 타입: `tsvector` 또는 `tsquery` 가능
- 옵션: `siglen` 파라미터로 서명 길이 지정 (기본값: 124바이트, 최대: 2024바이트)

### GIN vs GiST 비교

| 특성 | GIN | GiST |
|------|-----|------|
| 선호도 | 권장 | 대안 |
| 인덱스 유형 | Inverted index | Tree 기반 |
| 손실성 | Non-lossy (손실 없음) | Lossy (거짓 일치 가능) |
| 저장 내용 | 렉심만 저장, 가중치 레이블 미포함 | 고정 길이 서명 |
| 재확인 | 가중치 쿼리 시에만 | 항상 (거짓 일치 제거 위해) |
| 커버링 인덱스 | 지원 안함 | `INCLUDE` 절 사용 가능 |

### GIN 인덱스 빌드 시간 개선

```sql
SET maintenance_work_mem = '256MB';  -- 값을 증가시키면 빌드 시간 단축
```

### 서명 길이(siglen) 상세 설명

GiST 인덱스의 `siglen` 파라미터는 각 문서의 서명 길이를 결정합니다:

- 더 긴 서명: 더 정확한 검색, 더 큰 인덱스 크기
- 더 짧은 서명: 더 많은 거짓 일치, 더 작은 인덱스 크기

### 대규모 컬렉션 검색 구현

- 분할(Partitioning): 데이터베이스 수준에서 테이블 상속 사용
- 분산 검색: 여러 서버에 문서 분산 후 외부 검색 결과 수집

---

## 12.10 psql 지원

psql은 텍스트 검색 구성 객체에 대한 정보를 얻을 수 있는 명령어 세트를 제공합니다.

### 기본 문법

```
\dF{d,p,t}[+] [PATTERN]
```

- `+` (선택사항): 더 상세한 정보 표시
- `PATTERN` (선택사항): 텍스트 검색 객체의 이름 (스키마 한정 가능, 정규식 사용 가능)

### 사용 가능한 명령어

#### 1. `\dF[+] [PATTERN]` - 텍스트 검색 구성 나열

```sql
=> \dF russian
            List of text search configurations
   Schema   |  Name   |            Description
------------+---------+------------------------------------
 pg_catalog | russian | configuration for russian language

=> \dF+ russian
Text search configuration "pg_catalog.russian"
Parser: "pg_catalog.default"
      Token      | Dictionaries
-----------------+--------------
 asciihword      | english_stem
 asciiword       | english_stem
 email           | simple
 ...
```

#### 2. `\dFd[+] [PATTERN]` - 텍스트 검색 사전 나열

```sql
=> \dFd
                             List of text search dictionaries
   Schema   |      Name       |                        Description
------------+-----------------+-----------------------------------------------------------
 pg_catalog | arabic_stem     | snowball stemmer for arabic language
 pg_catalog | english_stem    | snowball stemmer for english language
 pg_catalog | simple          | simple dictionary: just lower case and check for stopword
 ...
```

#### 3. `\dFp[+] [PATTERN]` - 텍스트 검색 파서 나열

```sql
=> \dFp
        List of text search parsers
   Schema   |  Name   |     Description
------------+---------+---------------------
 pg_catalog | default | default word parser

=> \dFp+
    Text search parser "pg_catalog.default"
     Method      |    Function    | Description
-----------------+----------------+-------------
 Start parse     | prsd_start     |
 Get next token  | prsd_nexttoken |
 End parse       | prsd_end       |
 Get headline    | prsd_headline  |
 Get token types | prsd_lextype   |

        Token types for parser "pg_catalog.default"
   Token name    |               Description
-----------------+------------------------------------------
 asciihword      | Hyphenated word, all ASCII
 asciiword       | Word, all ASCII
 blank           | Space symbols
 ...
```

#### 4. `\dFt[+] [PATTERN]` - 텍스트 검색 템플릿 나열

```sql
=> \dFt
                           List of text search templates
   Schema   |   Name    |                        Description
------------+-----------+-----------------------------------------------------------
 pg_catalog | ispell    | ispell dictionary
 pg_catalog | simple    | simple dictionary: just lower case and check for stopword
 pg_catalog | snowball  | snowball stemmer
 pg_catalog | synonym   | synonym dictionary: replace word by its synonym
 pg_catalog | thesaurus | thesaurus dictionary: phrase by phrase substitution
```

---

## 12.11 제한사항

PostgreSQL의 전문 검색 기능에는 다음과 같은 제한사항이 있습니다:

| 제한사항 | 값 |
|---------|-----|
| 렉심 길이 | 2킬로바이트 미만 |
| tsvector 길이 | 1메가바이트 미만 (렉심 + 위치) |
| 렉심 개수 | 2^64 미만 |
| 위치 값 | 0보다 크고 16,383 이하 |
| FOLLOWED BY 연산자 거리 | 16,384 이하 |
| 렉심당 위치 수 | 최대 256개 |
| tsquery 노드 개수 | 32,768 미만 (렉심 + 연산자) |

### 비교 예시

실제 데이터에서의 통계:

- PostgreSQL 8.1 문서: 고유 단어 10,441개, 전체 단어 335,420개
- PostgreSQL 메일링 리스트 아카이브: 고유 단어 910,989개, 총 렉심 57,491,343개, 메시지 461,020개

이러한 제한사항들은 대부분의 일반적인 전문 검색 사용 사례에서는 문제가 되지 않습니다.

---

## 참고 자료

- [PostgreSQL 18 공식 문서 - Full Text Search](https://www.postgresql.org/docs/18/textsearch.html)
- [Snowball 공식 사이트](https://snowballstem.org/)
- [Hunspell](https://hunspell.github.io/)
