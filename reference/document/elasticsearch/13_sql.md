# SQL

> 이 문서는 Elasticsearch 9.x 공식 문서의 SQL 섹션을 한국어로 번역한 것입니다.
>
> 원문: https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-sql.html

---

## 목차

1. [개요](#1-개요)
2. [시작하기](#2-시작하기)
3. [개념 및 용어](#3-개념-및-용어)
4. [보안](#4-보안)
5. [SQL REST API](#5-sql-rest-api)
6. [SQL Translate API](#6-sql-translate-api)
7. [SQL CLI](#7-sql-cli)
8. [SQL JDBC](#8-sql-jdbc)
9. [SQL ODBC](#9-sql-odbc)
10. [클라이언트 애플리케이션](#10-클라이언트-애플리케이션)
11. [SQL 언어](#11-sql-언어)
12. [함수 및 연산자](#12-함수-및-연산자)
13. [제한 사항](#13-제한-사항)

---

## 1. 개요

### 1.1 Elasticsearch SQL이란?

Elasticsearch SQL은 Elasticsearch에 대해 SQL과 유사한 쿼리를 실시간으로 실행할 수 있게 해주는 X-Pack 구성 요소입니다. SQL과 Elasticsearch의 친밀함을 활용하여 두 환경 모두에서 데이터를 기본적으로 검색하고 집계할 수 있습니다.

Elasticsearch SQL은 SQL 구문과 Elasticsearch의 네이티브 쿼리 기능 간의 번역기 역할을 합니다. 익숙한 SQL 명령을 사용하여 Elasticsearch 데이터를 검색할 수 있습니다.

### 1.2 주요 장점

Elasticsearch SQL은 다음과 같은 핵심 장점을 제공합니다:

네이티브 통합

Elasticsearch SQL은 Elasticsearch를 위해 처음부터 구축되었습니다. 각 쿼리는 기본 저장 아키텍처에 따라 관련 노드에서 효율적으로 실행됩니다.

자체 포함(Self-contained)

Elasticsearch를 쿼리하기 위해 추가 하드웨어, 프로세스, 런타임 또는 라이브러리가 필요하지 않습니다. Elasticsearch SQL은 Elasticsearch 클러스터 _내부_에서 실행되어 추가적인 이동 부품을 제거합니다.

경량 및 효율성

Elasticsearch SQL은 검색을 추상화하지 않고 SQL을 수용하고 노출하여 실시간으로 적절한 전문 검색(full-text search)을 수행할 수 있게 합니다.

### 1.3 인터페이스

Elasticsearch SQL은 다양한 인터페이스를 통해 액세스할 수 있습니다:

- REST API: JSON 형식의 SQL 쿼리 실행
- CLI (Command Line Interface): 명령줄 도구
- JDBC 드라이버: Java 데이터베이스 연결
- ODBC 드라이버: 비즈니스 인텔리전스 도구 연결
- 클라이언트 애플리케이션: Tableau, Power BI, Excel 등

---

## 2. 시작하기

### 2.1 샘플 데이터 준비

Elasticsearch SQL을 사용하기 전에 먼저 쿼리할 데이터가 필요합니다. 다음 예제에서는 "library"라는 인덱스에 도서 데이터를 색인합니다:

```json
PUT /library/_bulk?refresh
{"index":{"_id": "Leviathan Wakes"}}
{"name": "Leviathan Wakes", "author": "James S.A. Corey", "release_date": "2011-06-02", "page_count": 561}
{"index":{"_id": "Hyperion"}}
{"name": "Hyperion", "author": "Dan Simmons", "release_date": "1989-05-26", "page_count": 482}
{"index":{"_id": "Dune"}}
{"name": "Dune", "author": "Frank Herbert", "release_date": "1965-06-01", "page_count": 604}
```

### 2.2 SQL REST API로 첫 쿼리 실행

SQL 검색 API를 사용하여 쿼리를 실행할 수 있습니다. `format` 매개변수를 사용하여 응답 형식을 지정합니다:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library WHERE release_date < '2000-01-01'"
}
```

응답:

```
    author     |     name      |  page_count   | release_date
---------------+---------------+---------------+------------------------
Dan Simmons    |Hyperion       |482            |1989-05-26T00:00:00.000Z
Frank Herbert  |Dune           |604            |1965-06-01T00:00:00.000Z
```

### 2.3 SQL CLI로 쿼리 실행

Elasticsearch의 `bin` 디렉토리에 있는 대화형 SQL CLI 도구를 사용할 수도 있습니다:

```bash
./bin/elasticsearch-sql-cli
```

CLI에서 동일한 쿼리를 실행할 수 있습니다:

```sql
sql> SELECT * FROM library WHERE release_date < '2000-01-01';
```

---

## 3. 개념 및 용어

### 3.1 SQL과 Elasticsearch 간 매핑

Elasticsearch SQL은 SQL 의미론을 가능한 한 따르며 두 개의 다른 데이터 관리 패러다임을 연결합니다. 다음은 SQL과 Elasticsearch 간의 용어 매핑입니다:

| SQL | Elasticsearch | 설명 |
|-----|---------------|------|
| column | field | 하나의 값을 포함하는 명명된 항목. SQL에서 열은 정확히 하나의 값을 포함하지만, Elasticsearch 필드는 동일한 타입의 여러 값을 포함할 수 있습니다. |
| row | document | 열/필드로 구성된 데이터 항목. SQL 행은 "엄격한" 구조를 가지며, Elasticsearch 문서는 "유연한" 구조를 가집니다. |
| table | index | 행/문서의 모음 |
| schema | implicit | SQL에서 스키마는 테이블의 네임스페이스입니다. Elasticsearch는 스키마 수준이 없으며, 각 인덱스에는 매핑이 암시적으로 적용됩니다. |
| catalog (database) | cluster | SQL에서 카탈로그 또는 데이터베이스는 스키마 집합을 나타냅니다. Elasticsearch에서는 클러스터가 배포된 여러 Elasticsearch 인스턴스를 나타냅니다. |

### 3.2 핵심 원칙

Elasticsearch SQL은 "최소 놀라움의 원칙"을 따르며, SQL 의미론을 유지하면서 Elasticsearch의 네이티브 기능을 수용합니다. 이를 통해 많은 개념이 Elasticsearch 전반에 걸쳐 투명하게 이동할 수 있습니다.

---

## 4. 보안

### 4.1 SSL/TLS 구성

클러스터가 암호화된 전송을 사용하는 경우 Elasticsearch SQL에 SSL/TLS 설정이 필요합니다. `ssl` 속성을 `true`로 설정하거나 연결 URL에서 `https` 접두사를 사용하여 활성화합니다.

인증서 설정에 따라 구성이 달라집니다:

- Keystore: 개인 키와 인증서를 저장 (PKI 인증에 필요)
- Truststore: CA 인증서를 저장하여 검증

### 4.2 인증 방법

#### 사용자 이름/비밀번호

연결 설정에서 `user`와 `password` 속성을 통해 인증을 구성합니다.

#### PKI/X.509 인증서

인증서 기반 인증을 위해 다음을 설정합니다:

- `ssl.keystore.location` - 키스토어 경로
- `ssl.keystore.pass` - 키스토어 비밀번호
- `ssl.truststore.location` - 트러스트스토어 경로
- `ssl.truststore.pass` - 트러스트스토어 비밀번호

### 4.3 서버 측 권한

SQL 쿼리를 실행하는 사용자에게는 다음과 같은 최소 권한이 필요합니다:

- 대상 인덱스에 대한 `read` 액세스
- `indices:admin/get` 권한
- 특정 API 작업을 위한 `cluster:monitor/main` 권한

권한은 역할을 통해 할당되며, 다음을 통해 생성할 수 있습니다:

- Kibana UI
- 역할 관리 API
- `roles.yml` 구성 파일

---

## 5. SQL REST API

### 5.1 개요

SQL 검색 API는 JSON 문서 내에서 SQL을 제출하고 결과를 반환받을 수 있게 합니다. 기본 엔드포인트는 `/_sql`입니다.

### 5.2 기본 요청

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library ORDER BY page_count DESC LIMIT 5"
}
```

Kibana Console에서 쿼리를 작성할 때는 삼중 따옴표(`"""`)를 사용하면 내부 인용 부호를 자동으로 이스케이프하고 여러 줄 쿼리 형식을 지원합니다.

### 5.3 응답 형식

`format` URL 매개변수 또는 `Accept` HTTP 헤더를 통해 응답 형식을 지정할 수 있습니다. URL 매개변수가 우선합니다.

#### 지원되는 형식

| 형식 | Content-Type | 설명 |
|------|--------------|------|
| csv | text/csv | 쉼표로 구분된 값 |
| json | application/json | 열 메타데이터와 행이 포함된 구조화된 형식 |
| tsv | text/tab-separated-values | 탭으로 구분된 값 |
| txt | text/plain | CLI 스타일 테이블 표현 |
| yaml | application/yaml | 사람이 읽을 수 있는 구조화된 형식 |
| cbor | application/cbor | 간결한 바이너리 객체 표현 |
| smile | application/smile | CBOR과 유사한 바이너리 형식 |

#### JSON 형식 예제

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5
}
```

응답:

```json
{
  "columns": [
    {"name": "author", "type": "text"},
    {"name": "name", "type": "text"},
    {"name": "page_count", "type": "short"},
    {"name": "release_date", "type": "datetime"}
  ],
  "rows": [
    ["Frank Herbert", "Dune", 604, "1965-06-01T00:00:00.000Z"],
    ["James S.A. Corey", "Leviathan Wakes", 561, "2011-06-02T00:00:00.000Z"],
    ["Dan Simmons", "Hyperion", 482, "1989-05-26T00:00:00.000Z"]
  ]
}
```

#### CSV 형식 예제

```
POST /_sql?format=csv
{
  "query": "SELECT * FROM library ORDER BY page_count DESC"
}
```

선택적으로 `delimiter` 매개변수를 사용하여 구분자를 사용자 정의할 수 있습니다 (기본값: 쉼표).

### 5.4 페이지네이션

대용량 결과 집합의 경우 커서 기반 페이지네이션을 사용합니다. 초기 쿼리에서 `fetch_size`를 지정하면 응답에 `cursor`가 포함됩니다:

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 2
}
```

다음 페이지를 가져오려면 커서를 사용합니다:

```json
POST /_sql?format=json
{
  "cursor": "sDXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAAE..."
}
```

### 5.5 Query DSL 필터링

`filter` 매개변수를 통해 SQL 쿼리가 실행되기 전에 Elasticsearch Query DSL을 사용하여 결과를 필터링할 수 있습니다:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "filter": {
    "range": {
      "page_count": {
        "gte": 100,
        "lte": 200
      }
    }
  },
  "fetch_size": 5
}
```

라우팅 기반 필터링도 가능합니다:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library",
  "filter": {
    "terms": {
      "_routing": ["abc"]
    }
  }
}
```

### 5.6 컬럼형 결과

`columnar` 매개변수를 `true`로 설정하면 행 지향 형식 대신 컬럼형 형식으로 결과를 반환합니다. 이 기능은 `json`, `yaml`, `cbor`, `smile` 형식에서 사용할 수 있습니다:

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5,
  "columnar": true
}
```

응답:

```json
{
  "columns": [
    {"name": "author", "type": "text"},
    {"name": "name", "type": "text"},
    {"name": "page_count", "type": "short"},
    {"name": "release_date", "type": "datetime"}
  ],
  "values": [
    ["Frank Herbert", "James S.A. Corey", "Dan Simmons"],
    ["Dune", "Leviathan Wakes", "Hyperion"],
    [604, 561, 482],
    ["1965-06-01T00:00:00.000Z", "2011-06-02T00:00:00.000Z", "1989-05-26T00:00:00.000Z"]
  ]
}
```

### 5.7 매개변수화된 쿼리

SQL 주입을 방지하기 위해 매개변수화된 쿼리를 사용하는 것이 권장됩니다. 쿼리 문자열에서 물음표 플레이스홀더(`?`)를 사용하고 값을 별도의 매개변수 배열에 추출합니다:

```json
POST /_sql?format=txt
{
  "query": "SELECT YEAR(release_date) AS year FROM library WHERE page_count > ? AND author = ? GROUP BY year HAVING COUNT(*) > ?",
  "params": [300, "Frank Herbert", 0]
}
```

### 5.8 런타임 필드

런타임 필드를 사용하면 기본 매핑을 수정하지 않고도 기존 데이터에서 새 열을 추출하고 생성할 수 있습니다. `runtime_mappings` 매개변수를 사용합니다:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library",
  "runtime_mappings": {
    "release_day_of_week": {
      "type": "keyword",
      "script": "emit(doc['release_date'].value.dayOfWeekEnum.toString())"
    }
  }
}
```

### 5.9 비동기 검색

대용량 데이터셋 또는 동결된 인덱스를 쿼리할 때 시간이 오래 걸리는 작업의 경우 비동기 실행을 활성화할 수 있습니다:

```json
POST /_sql?format=json
{
  "wait_for_completion_timeout": "2s",
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5
}
```

결과가 지정된 시간 내에 도착하지 않으면 요청이 비동기 모드로 전환되고 다음을 반환합니다:

- 고유 검색 식별자
- `is_partial: true` (불완전한 결과 표시)
- `is_running: true` (백그라운드 실행 표시)

검색 진행 상황을 확인하려면:

```json
GET _sql/async/status/<search_id>
```

검색 유지 기간을 사용자 정의하려면 `keep_alive`를 사용합니다:

```json
POST /_sql?format=json
{
  "keep_alive": "2d",
  "wait_for_completion_timeout": "2s",
  "query": "SELECT * FROM library ORDER BY page_count DESC"
}
```

---

## 6. SQL Translate API

### 6.1 개요

SQL Translate API는 SQL 쿼리를 네이티브 Elasticsearch Query DSL 요청으로 변환합니다. 이를 통해 SQL 문이 쿼리 수준에서 어떻게 처리되는지 이해할 수 있습니다.

### 6.2 사용법

```json
POST /_sql/translate
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 10
}
```

응답:

```json
{
  "size": 10,
  "_source": false,
  "fields": [
    {"field": "author"},
    {"field": "name"},
    {"field": "page_count"},
    {"field": "release_date", "format": "strict_date_optional_time_nanos"}
  ],
  "sort": [
    {
      "page_count": {
        "order": "desc",
        "missing": "_first",
        "unmapped_type": "short"
      }
    }
  ],
  "track_total_hits": -1
}
```

### 6.3 활용

이 도구는 다음에 유용합니다:

- SQL 쿼리가 어떤 Elasticsearch API 메서드를 사용하는지 확인
- SQL 쿼리 최적화 이해
- 디버깅

요청 본문은 `cursor`를 제외하고 SQL 검색 API와 동일한 매개변수를 지원합니다.

---

## 7. SQL CLI

### 7.1 개요

Elasticsearch는 Elasticsearch 인스턴스에 대해 SQL 쿼리를 실행하기 위한 SQL CLI 스크립트를 `bin` 디렉토리에 제공합니다.

### 7.2 시작하기

기본 시작:

```bash
./bin/elasticsearch-sql-cli
```

특정 서버에 연결:

```bash
./bin/elasticsearch-sql-cli https://some.server:9200
```

인증 포함:

```bash
./bin/elasticsearch-sql-cli https://username:password@host:port
```

독립 실행형 JAR:

Elasticsearch 설치 없이 실행하려면:

```bash
java -jar elasticsearch-sql-cli-VERSION.jar https://server:9200
```

### 7.3 CLI 명령

#### 구성 명령

| 명령 | 기본값 | 설명 |
|------|--------|------|
| `allow_partial_search_results` | false | 샤드 타임아웃 또는 실패 시 부분 결과 반환 여부 |
| `fetch_size` | 1000 | 페치 작업당 검색되는 레코드 수 |
| `fetch_separator` | 빈 문자열 | 연속 페치 작업 간의 구분자 문자열 |
| `lenient` | false | 활성화하면 배열 필드에서 오류 대신 첫 번째 값 반환 |

#### 유틸리티 명령

| 명령 | 기능 |
|------|------|
| `info` | 서버 및 클러스터 정보 표시 |
| `exit` | CLI 애플리케이션 종료 |
| `cls` | 터미널 화면 지우기 |
| `logo` | Elastic 로고 및 버전 출력 |

### 7.4 쿼리 예제

```sql
sql> SELECT * FROM library WHERE page_count > 500 ORDER BY page_count DESC;
```

---

## 8. SQL JDBC

### 8.1 개요

Elasticsearch의 SQL JDBC 드라이버는 JDBC 호출을 Elasticsearch SQL로 변환하는 Type 4 JDBC 드라이버입니다. 플랫폼 독립적인 순수 Java 솔루션으로 작동합니다.

### 8.2 설치

#### Maven

프로젝트에 다음 의존성을 추가합니다:

```xml
<dependency>
  <groupId>org.elasticsearch.plugin</groupId>
  <artifactId>x-pack-sql-jdbc</artifactId>
  <version>9.2.4</version>
</dependency>
```

리포지토리도 포함합니다:

```xml
<repository>
  <id>elastic.co</id>
  <url>https://artifacts.elastic.co/maven</url>
</repository>
```

#### 직접 다운로드

수동 다운로드는 [elastic.co의 JDBC 페이지](https://www.elastic.co/downloads/jdbc-client)를 방문하세요.

### 8.3 버전 호환성

드라이버 버전은 Elasticsearch 설치 버전과 일치하거나 더 낮아야 합니다. 예를 들어, 9.2.4 드라이버는 Elasticsearch 7.10.0에서 작동하지 않습니다.

### 8.4 드라이버 등록

메인 클래스는 `org.elasticsearch.xpack.sql.jdbc.EsDriver`입니다. 드라이버는 클래스패스에 있을 때 JDBC 4.0의 서비스 프로바이더 메커니즘을 통해 자동 등록됩니다.

### 8.5 연결 URL 형식

```
jdbc:[es|elasticsearch]://[[http|https]://]?[host[:port]]?/[prefix]?[\?[option=value]&]*
```

예제:

```
jdbc:es://http://server:3456/?timezone=UTC&page.size=250
```

### 8.6 필수 구성

| 매개변수 | 기본값 | 목적 |
|---------|--------|------|
| `timezone` | JVM 시간대 | 연결별 시간대 (UTC 권장) |
| `connect.timeout` | 30000ms | 최대 연결 수립 시간 |
| `network.timeout` | 60000ms | 최대 네트워크 작업 시간 |
| `query.timeout` | 90000ms | 최대 쿼리 실행 대기 시간 |
| `page.size` | 1000 | 페이지당 반환되는 결과 수 |
| `page.timeout` | 45000ms | 스크롤 커서 유지 기간 |

### 8.7 인증

기본 인증을 위해 다음 속성을 사용합니다:

- `user`: 사용자 이름
- `password`: 비밀번호

### 8.8 SSL 구성

보안 연결을 활성화하려면 다음 옵션을 사용합니다:

| 옵션 | 설명 |
|------|------|
| `ssl` | `true`로 설정하여 활성화 |
| `ssl.keystore.location` | 키 스토어 파일 경로 |
| `ssl.keystore.pass` | 키 스토어 비밀번호 |
| `ssl.keystore.type` | 형식 (기본 JKS; PKCS12 사용 가능) |
| `ssl.truststore.location` | 트러스트 스토어 경로 |
| `ssl.truststore.pass` | 트러스트 스토어 비밀번호 |
| `ssl.protocol` | SSL 버전 (기본 TLS) |

### 8.9 고급 옵션

프록시 지원:

- `proxy.http`: HTTP 프록시 호스트 이름
- `proxy.socks`: SOCKS 프록시 호스트 이름

쿼리 동작:

- `field.multi.value.leniency`: 다중값 필드의 첫 번째 값 반환 (기본 true)
- `allow.partial.results`: 샤드 실패 시 부분 결과 활성화 (기본 false)
- `index.include.frozen`: 동결된 인덱스 포함 (기본 false)

디버깅:

- `debug`: 디버그 로깅 활성화 (기본 false)
- `debug.output`: 로그 대상 - `err` (기본), `out`, 또는 파일 경로

---

## 9. SQL ODBC

### 9.1 개요

Elasticsearch SQL ODBC 드라이버는 Elasticsearch를 위한 3.80 호환 ODBC 드라이버입니다. 이 드라이버는 ODBC 함수 호출을 Elasticsearch SQL 작업으로 변환하는 코어 레벨 인터페이스로 작동합니다.

### 9.2 요구 사항

이 드라이버를 사용하려면 시스템에 다음이 필요합니다:

- Elasticsearch SQL 설치 및 운영
- 서버의 유효한 라이선스

### 9.3 설치 및 구성

문서는 두 가지 주요 구성 영역을 다룹니다:

1. 드라이버 설치 - 운영 체제에서 ODBC 드라이버를 얻고 설정하는 방법
2. 구성 - 연결 매개변수 및 드라이버 설정 구성 지침

### 9.4 목적

드라이버는 ODBC 호환 애플리케이션과 Elasticsearch의 SQL 기능 사이의 다리 역할을 하여 직접적인 API 지식 없이도 Elasticsearch 데이터에 대해 SQL 쿼리를 실행할 수 있게 합니다.

---

## 10. 클라이언트 애플리케이션

### 10.1 지원되는 애플리케이션

Elasticsearch의 SQL 기능은 JDBC 및 ODBC 인터페이스를 통해 다양한 타사 애플리케이션에 확장됩니다:

데이터베이스 도구:

- DBeaver
- DbVisualizer
- SQL Workbench/J
- SQuirreL SQL

비즈니스 인텔리전스:

- Tableau Desktop
- Tableau Server
- Qlik Sense Desktop
- MicroStrategy Desktop
- Microsoft Power BI Desktop

스프레드시트 및 스크립팅:

- Microsoft Excel
- Microsoft PowerShell

### 10.2 주요 고려 사항

면책 조항: Elastic은 나열된 애플리케이션을 보증, 홍보 또는 지원하지 않습니다. 네이티브 Elasticsearch 통합을 원하는 조직은 공급업체에 직접 문의해야 합니다.

기술적 제한 사항: ODBC 2.x 표준 이하를 구현하는 애플리케이션에 대한 지원은 현재 제한적입니다.

범위: 각 애플리케이션에는 Elasticsearch 문서 범위 외의 고유한 요구 사항과 라이선스 조건이 있습니다. 구성 지원은 Elasticsearch SQL 연결에만 집중됩니다.

---

## 11. SQL 언어

### 11.1 어휘 구조

#### 키워드

SQL 키워드는 `SELECT`, `FROM`, `WHERE`와 같은 고정 의미 단어입니다. 키워드는 대소문자를 구분하지 않으므로 `select`와 `SELECT`는 동일합니다. 가독성을 높이기 위해 키워드를 대문자로 작성하는 것이 관례입니다.

#### 식별자

식별자는 테이블 및 열과 같은 엔티티의 이름을 지정합니다. 인용되지 않은 식별자(예: `ip_address`)와 인용된 식별자(예: `"hosts-*"`)가 있습니다. 인용된 식별자는 "명확성과 모호성 제거"를 제공하므로 특히 사용자 입력을 처리할 때 권장됩니다. 키워드와 달리 식별자는 대소문자를 구분합니다.

#### 리터럴

문자열 리터럴: 작은따옴표로 묶이며, 이스케이프된 따옴표는 반복됩니다 (`'Captain EO''s Voyage'`).

숫자 리터럴: 십진수 또는 과학적 표기법으로 허용됩니다. 소수점이 있는 숫자는 `double` 타입이고, 소수점이 없는 정수는 크기에 따라 `integer` 또는 `long`입니다.

일반 리터럴: 캐스트 연산자 또는 변환 함수를 통한 캐스팅으로 생성됩니다.

#### 작은따옴표 vs 큰따옴표

이 구분은 중요합니다: 작은따옴표(`'`)와 큰따옴표(`"`)는 다른 의미를 가지며 상호 교환할 수 없습니다. 작은따옴표는 문자열 리터럴을 정의하고, 큰따옴표는 식별자를 정의합니다.

#### 특수 문자

별표(`*`), 쉼표(`,`), 마침표(`.`), 괄호(`()`)는 SQL 구문에서 전용 의미를 가집니다.

#### 연산자 및 주석

연산자는 왼쪽 결합성을 가진 표준 우선순위 규칙을 따릅니다. 주석은 한 줄의 경우 `--`를, 여러 줄의 경우 `/* */` 형식을 사용합니다.

### 11.2 SQL 명령

#### SELECT

SELECT 문은 이 일반적인 형식으로 테이블에서 행을 검색합니다:

```sql
SELECT [TOP [count]] select_expr [, ...]
[FROM table_name]
[WHERE condition]
[GROUP BY grouping_element [, ...]]
[HAVING condition]
[ORDER BY expression [ASC | DESC] [, ...]]
[LIMIT [count]]
[PIVOT (aggregation_expr FOR column IN (value...))]
```

SELECT 목록: 출력 열은 `AS` 키워드를 사용하여 명시적으로 이름을 지정하거나 열 참조에서 암시적으로 파생될 수 있습니다.

와일드카드 선택: `*` 연산자는 다중 필드 및 하위 필드를 제외한 모든 최상위 열을 선택합니다.

TOP 절: 결과를 최대 행 수로 제한합니다:

```sql
SELECT TOP 2 first_name, last_name FROM emp
```

동일한 쿼리에서 LIMIT와 결합할 수 없습니다.

FROM 절: 단일 테이블 또는 인덱스 패턴을 지정합니다. 지원 기능:

- 큰따옴표를 사용한 특수 문자가 있는 이스케이프된 테이블 이름
- 여러 인덱스 쿼리를 위한 인덱스 패턴
- `<remote_cluster>:<target>` 구문을 사용한 크로스 클러스터 검색
- 간결함을 위한 선택적 테이블 별칭

WHERE 절: 부울 값으로 평가되는 조건에 따라 행을 필터링합니다.

GROUP BY: 지정된 열의 일치하는 값에 따라 결과를 그룹으로 나눕니다. 표현식은 열 이름, 별칭, 서수 위치 또는 계산된 표현식을 참조할 수 있습니다. 모든 출력 표현식은 집계 함수이거나 그룹화 표현식이어야 합니다.

암시적 그룹화: GROUP BY 없이 집계 함수가 나타나면 단일 기본 그룹이 생성되어 집계된 결과가 있는 하나의 행을 반환합니다.

HAVING: GROUP BY로 생성된 그룹을 필터링합니다. 개별 행이 아닌 집계된 값에 대해 작동하며, 그룹화가 발생한 후에 평가됩니다.

ORDER BY: 표현식, 열, 출력 서수 위치 또는 관련성 점수별로 결과를 정렬합니다. 기본 방향은 오름차순이며 null 값은 마지막에 나타납니다.

LIMIT: `LIMIT count` 또는 제한 없이 `LIMIT ALL`을 사용하여 결과를 제한합니다.

PIVOT: 고유한 열 값을 결과 헤더로 회전하여 교차 집계를 수행합니다:

```sql
SELECT * FROM test_emp PIVOT (SUM(salary) FOR languages IN (1, 2))
```

#### DESCRIBE TABLE

DESCRIBE TABLE 명령은 테이블, 인덱스 또는 데이터 스트림에서 열 정보를 검색합니다.

```sql
DESCRIBE table_name
```

또는

```sql
DESC table_name
```

출력 열:

- column: 필드 이름
- type: SQL 데이터 타입 (VARCHAR, INTEGER, TIMESTAMP 등)
- mapping: 기본 Elasticsearch 매핑 타입 (keyword, text, nested 등)

#### SHOW TABLES

SHOW TABLES 명령은 사용 가능한 테이블(인덱스 및 데이터 스트림)을 타입 및 종류와 함께 나열합니다:

```sql
SHOW TABLES [CATALOG identifier] [INCLUDE FROZEN] [table_identifier | LIKE pattern]
```

구성 요소:

- CATALOG: 와일드카드를 지원하는 선택적 클러스터 식별자
- INCLUDE FROZEN: 동결된 인덱스를 표시하는 선택적 플래그
- table_identifier: 단일 테이블 이름 또는 다중 대상 패턴
- LIKE 절: SQL 패턴 매칭을 사용하여 결과 제한

기본 사용법:

```sql
SHOW TABLES;
```

패턴 매칭:

```sql
SHOW TABLES LIKE 'emp%';
```

다중 인덱스 선택:

```sql
SHOW TABLES "*,-l*";
```

#### SHOW COLUMNS

SHOW COLUMNS 명령은 테이블의 열과 데이터 타입을 나열합니다:

```sql
SHOW COLUMNS [CATALOG identifier] [INCLUDE FROZEN] FROM table_identifier
```

또는

```sql
SHOW COLUMNS [CATALOG identifier] [INCLUDE FROZEN] IN table_identifier
```

#### SHOW FUNCTIONS

SHOW FUNCTIONS 명령은 사용 가능한 모든 SQL 함수와 타입을 나열합니다:

```sql
SHOW FUNCTIONS [LIKE pattern]
```

함수 타입:

- AGGREGATE: AVG, COUNT, SUM, MAX, MIN 등
- GROUPING: HISTOGRAM 등
- CONDITIONAL: CASE, COALESCE, IFNULL, NULLIF 포함
- SCALAR: 수학, 문자열, 날짜/시간 및 타입 변환 함수
- SCORE: 점수 함수

패턴 매칭:

```sql
SHOW FUNCTIONS LIKE 'A%';
```

### 11.3 데이터 타입

#### SQL과 Elasticsearch 타입 매핑

숫자 타입:

| Elasticsearch | SQL | 정밀도 |
|---------------|-----|--------|
| byte | TINYINT | 3 |
| short | SMALLINT | 5 |
| integer | INTEGER | 10 |
| long | BIGINT | 19 |
| unsigned_long | BIGINT | 20 |
| float | REAL | 7 |
| double | DOUBLE | 15 |
| half_float | FLOAT | 3 |
| scaled_float | DOUBLE | 15 |

문자열 및 텍스트 타입:

| Elasticsearch | SQL | 정밀도 |
|---------------|-----|--------|
| keyword 계열 | VARCHAR | 32,766 |
| text | VARCHAR | 2,147,483,647 |
| binary | VARBINARY | 2,147,483,647 |

시간 타입:

| Elasticsearch | SQL | 정밀도 |
|---------------|-----|--------|
| date | datetime | 29 |
| ip | VARCHAR | 39 |

복합 타입:

| Elasticsearch | SQL |
|---------------|-----|
| object | STRUCT |
| nested | STRUCT |

#### SQL 전용 런타임 타입

이러한 타입은 Elasticsearch에 해당하는 것 없이 SQL 쿼리에만 존재합니다:

- DATE 및 TIME (datetime과 별도)
- INTERVAL 타입 (year, month, day, hour, minute, second 및 조합)
- GEO_POINT (정밀도 52)
- GEO_SHAPE 및 SHAPE (정밀도 2,147,483,647)

#### 다중 필드 처리

SQL이 정확한 일치가 필요한 `text` 필드를 만나면 "정규화되지 _않은_ 첫 번째 `keyword`를 검색하여 원래 필드의 _정확한_ 값으로 사용합니다."

### 11.4 인덱스 패턴

Elasticsearch SQL은 여러 인덱스 또는 테이블을 일치시키기 위한 두 가지 접근 방식을 지원합니다:

#### Elasticsearch 다중 대상 구문

이 방법은 큰따옴표를 사용하고 Elasticsearch의 표준 다중 대상 표기법을 따릅니다. 와일드카드를 통해 인덱스의 포함, 제외 및 열거를 활성화합니다.

주요 특성:

- 인용에 `"`를 사용
- 다중 문자 일치에 `*` 지원
- `-` 접두사로 제외
- 특정 대상 열거 가능

예제 사용법:

```sql
SHOW TABLES "*,-l*";
SELECT emp_no FROM "e*p" LIMIT 1;
```

크로스 클러스터 기능: 원격 클러스터는 `"<remote_cluster>:<target>"` 구문을 사용하여 쿼리할 수 있으며 둘 다 와일드카드를 지원합니다.

#### SQL LIKE 표기법

이 접근 방식은 작은따옴표로 표준 SQL 패턴 매칭 규칙을 적용합니다.

주요 특성:

- 인용에 `'`를 사용
- 다중 문자 일치에 `%` 사용
- 단일 문자 일치에 `_` 사용
- 특수 문자 처리를 위한 `ESCAPE` 키워드 지원

예제 사용법:

```sql
SHOW TABLES LIKE 'emp%';
SHOW TABLES LIKE 'emp!%' ESCAPE '!';
```

#### 비교 요약

| 기능 | 다중 인덱스 | SQL LIKE |
|------|-------------|----------|
| 인용 타입 | `"` | `'` |
| 다중 문자 패턴 | `*` | `%` |
| 단일 문자 패턴 | 없음 | `_` |
| 제외 지원 | 예 | 아니오 |
| 열거 지원 | 예 | 아니오 |
| 이스케이프 지원 | 아니오 | ESCAPE |

중요 제약: 다중 인덱스 쿼리에서 해결된 모든 구체적인 테이블은 성공적으로 실행하려면 동일한 매핑을 공유해야 합니다.

---

## 12. 함수 및 연산자

### 12.1 연산자

#### 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `=` | 같음 |
| `<>`, `!=` | 같지 않음 |
| `<` | 보다 작음 |
| `<=` | 보다 작거나 같음 |
| `>` | 보다 큼 |
| `>=` | 보다 크거나 같음 |
| `BETWEEN` | 범위 확인 |
| `IS NULL` | null 여부 |
| `IN` | 목록에 포함 여부 |

#### 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `AND` | 논리 AND |
| `OR` | 논리 OR |
| `NOT` | 논리 NOT |

#### 산술 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | 덧셈 |
| `-` | 뺄셈 |
| `*` | 곱셈 |
| `/` | 나눗셈 |
| `%` | 나머지 (모듈로) |

#### 패턴 매칭

| 연산자 | 설명 |
|--------|------|
| `LIKE` | SQL 표준 패턴 매칭 |
| `RLIKE` | 정규식 기반 패턴 검색 |

#### 타입 캐스팅

값 간 타입 변환을 위해 `::` 연산자를 사용합니다.

### 12.2 집계 함수

| 함수 | 설명 |
|------|------|
| `COUNT` | 행 개수 |
| `SUM` | 합계 |
| `AVG` | 평균 |
| `MIN` | 최소값 |
| `MAX` | 최대값 |
| `STDDEV_POP` | 모집단 표준 편차 |
| `STDDEV_SAMP` | 표본 표준 편차 |
| `VAR_POP` | 모집단 분산 |
| `VAR_SAMP` | 표본 분산 |
| `PERCENTILE` | 백분위수 |
| `KURTOSIS` | 첨도 |
| `SKEWNESS` | 왜도 |
| `MAD` | 중앙값 절대 편차 |

### 12.3 그룹화 함수

| 함수 | 설명 |
|------|------|
| `HISTOGRAM` | 숫자 값을 버킷으로 분류 |

### 12.4 날짜-시간 함수

| 함수 | 설명 |
|------|------|
| `CURRENT_DATE` | 현재 날짜 |
| `CURRENT_TIME` | 현재 시간 |
| `CURRENT_TIMESTAMP` | 현재 타임스탬프 |
| `DATE_ADD` | 날짜 더하기 |
| `DATE_DIFF` | 날짜 차이 |
| `DATE_TRUNC` | 날짜 잘라내기 |
| `EXTRACT` | 날짜 부분 추출 |
| `DATE_PART` | 날짜 부분 추출 |
| `DAY_OF_MONTH` | 월의 날짜 |
| `MONTH_OF_YEAR` | 연의 월 |

### 12.5 문자열 함수

| 함수 | 설명 |
|------|------|
| `CONCAT` | 문자열 연결 |
| `SUBSTRING` | 부분 문자열 추출 |
| `REPLACE` | 문자열 대체 |
| `TRIM` | 공백 제거 |
| `UPPER` | 대문자로 변환 |
| `LOWER` | 소문자로 변환 |
| `LOCATE` | 위치 찾기 |
| `POSITION` | 위치 찾기 |
| `LENGTH` | 길이 |

### 12.6 수학 함수

| 함수 | 설명 |
|------|------|
| `ABS` | 절대값 |
| `ROUND` | 반올림 |
| `FLOOR` | 내림 |
| `CEIL` | 올림 |
| `SQRT` | 제곱근 |
| `POWER` | 거듭제곱 |
| `LOG` | 로그 |
| `EXP` | 지수 |
| `SIN` | 사인 |
| `COS` | 코사인 |
| `TAN` | 탄젠트 |
| `ASIN` | 아크사인 |
| `ACOS` | 아크코사인 |
| `ATAN` | 아크탄젠트 |

### 12.7 조건 함수

| 함수 | 설명 |
|------|------|
| `CASE` | 조건부 표현식 |
| `COALESCE` | 첫 번째 non-null 값 |
| `IFNULL` | null인 경우 대체 값 |
| `IIF` | 인라인 조건 |
| `GREATEST` | 최대값 |
| `LEAST` | 최소값 |
| `NULLIF` | 같으면 null |

### 12.8 전문 검색 함수

| 함수 | 설명 |
|------|------|
| `MATCH` | 매치 쿼리 수행 |
| `QUERY` | 쿼리 문자열 쿼리 수행 |
| `SCORE` | 관련성 점수 |

### 12.9 지리 함수

| 함수 | 설명 |
|------|------|
| `ST_Distance` | 두 지점 간 거리 |
| `ST_AsWKT` | WKT 형식으로 변환 |
| `ST_X` | X 좌표 (경도) |
| `ST_Y` | Y 좌표 (위도) |
| `ST_Z` | Z 좌표 (고도) |

### 12.10 타입 변환

| 함수 | 설명 |
|------|------|
| `CAST` | 명시적 타입 변환 |
| `CONVERT` | 타입 변환 |

---

## 13. 제한 사항

### 13.1 파싱 제한

"매우 큰 쿼리는 파싱 단계에서 너무 많은 메모리를 소비할 수 있으며" `ParsingException`을 발생시킬 수 있습니다. 권장 솔루션은 쿼리를 단순화하거나 더 작은 구성 요소로 분할하는 것입니다.

### 13.2 중첩 필드 제한

중첩 필드는 상위 형식으로 직접 쿼리할 수 없습니다. 사용자는 `[nested_field_name].[sub_field_name]`과 같은 점 표기법을 사용하여 내부 하위 필드를 참조해야 합니다. 또한 스칼라 함수는 `WHERE` 및 `ORDER BY` 절 내의 중첩 필드에 적용할 수 없지만 비교 및 논리 연산자는 허용됩니다.

다중 중첩 문서는 또 다른 제약을 제시합니다 - 쿼리는 다른 수준 또는 동일 수준의 여러 중첩 필드를 동시에 참조할 수 없습니다.

### 13.3 페이지네이션 문제

중첩 필드를 선택할 때 페이지네이션은 정확한 결과가 아닌 "최소한" 페이지 크기의 레코드를 반환합니다. Elasticsearch가 내부 히트가 아닌 루트 문서 수준에서 중첩 쿼리를 처리하기 때문입니다.

### 13.4 집계 제약

집계 함수별 정렬은 최대 65,535행으로 제한됩니다. `ORDER BY` 절은 일반 집계 함수만 참조해야 합니다 - 스칼라 함수나 연산자 조합은 허용되지 않습니다. 또한 `FIRST` 및 `LAST` 집계 함수는 `HAVING` 절에서 사용할 수 없습니다.

### 13.5 데이터 타입 제한

- `TIME` 데이터 타입은 `GROUP BY` 또는 `HISTOGRAM` 함수에서 사용할 수 없습니다.
- 정규화기가 있는 `keyword` 필드는 지원되지 않습니다.
- 배열 필드는 `field_multi_value_leniency` 매개변수를 통한 특수 처리가 필요합니다.

### 13.6 쿼리 구조 제한

하위 선택은 단일 `SELECT` 문으로 "평탄화"되어야 합니다. `GROUP BY`, `HAVING` 또는 복잡한 조건이 있는 복잡한 하위 선택은 지원되지 않습니다. `PIVOT` 집계는 단일 표현식만 허용하며, `PIVOT`의 `IN` 절의 하위 쿼리는 지원되지 않습니다.

### 13.7 지리적 제약

`geo_shape` 필드는 doc values가 없으며 필터링, 그룹화 또는 정렬에 사용할 수 없습니다. `geo_points` 필드의 고도 구성 요소는 인덱싱되거나 저장되지 않습니다.

---

## 실전 예제

### 기본 SELECT 쿼리

```sql
SELECT author, name, page_count
FROM library
WHERE page_count > 300
ORDER BY page_count DESC
LIMIT 10;
```

### 집계 및 그룹화

```sql
SELECT author, COUNT(*) AS book_count, AVG(page_count) AS avg_pages
FROM library
GROUP BY author
HAVING COUNT(*) > 1
ORDER BY book_count DESC;
```

### 날짜 함수 사용

```sql
SELECT name, release_date, YEAR(release_date) AS release_year
FROM library
WHERE release_date BETWEEN '1960-01-01' AND '2000-12-31'
ORDER BY release_date;
```

### PIVOT 사용

```sql
SELECT *
FROM library
PIVOT (COUNT(*) FOR author IN ('Frank Herbert', 'Dan Simmons', 'James S.A. Corey'));
```

### 전문 검색

```sql
SELECT name, author, SCORE()
FROM library
WHERE MATCH(name, 'dune')
ORDER BY SCORE() DESC;
```

### 인덱스 패턴 사용

```sql
SELECT *
FROM "lib*"
WHERE page_count > 400;
```

---

## 참고 자료

- [Elasticsearch SQL 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-sql.html)
- [SQL 개요](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-overview.html)
- [SQL 시작하기](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-getting-started.html)
- [SQL 개념 및 용어](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-concepts.html)
- [SQL REST API](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-rest.html)
- [SQL 언어](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-spec.html)
- [SQL 함수 및 연산자](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-functions.html)
- [SQL 제한 사항](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-limitations.html)
