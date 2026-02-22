# jOOQ 3.18.0 출시 - 더 많은 진단(Diagnostics), SQL/JSON, Oracle 연관 배열(Associative Arrays), 다차원 배열, R2DBC 1.0 지원

> 원문: [3.18.0 Release with Support for more Diagnostics, SQL/JSON, Oracle Associative Arrays, Multi dimensional Arrays, R2DBC 1.0](https://blog.jooq.org/3-18-0-release-with-support-for-more-diagnostics-sql-json-oracle-associative-arrays-multi-dimensional-arrays-r2dbc-1-0/)

## DiagnosticsListener 개선

많은 추가 진단 기능이 추가되었으며, 여기에는 패턴 대체(pattern replacement)의 자동 감지가 포함되어 SQL 쿼리를 린트(lint)하는 데 도움을 줍니다. 이는 jOOQ를 사용하여 SQL을 작성하든, 기존 애플리케이션의 JDBC / R2DBC 프록시로 사용하든 관계없이 동작합니다.

주요 패턴 변환 예시는 다음과 같습니다:

- `CASE WHEN a = b THEN 1 END` → `CASE a WHEN b THEN 1 END`
- `CASE WHEN x IS NULL THEN y ELSE x END` → `NVL(x, y)`
- `CASE WHEN x = y THEN NULL ELSE x END` → `NULLIF(x, y)`
- `(SELECT COUNT(*) FROM t) > 0` → `EXISTS(SELECT 1 FROM t)`

관련 문서 섹션:

- [진단(Diagnostics)](https://www.jooq.org/doc/3.18/manual/)
- [진단 연결(Diagnostics connection)](https://www.jooq.org/doc/3.18/manual/)
- [진단 로깅(Diagnostics logging)](https://www.jooq.org/doc/3.18/manual/)
- [패턴 변환(Pattern transformations)](https://www.jooq.org/doc/3.18/manual/)

## 더 많은 SQL/JSON 지원

SQL/JSON은 SQL 언어에 가장 최근에 추가된 유망한 기능 중 하나이며, 우리는 항상 이러한 기능에 대한 jOOQ의 지원을 개선하는 데 열의를 가지고 있습니다.

새로 추가된 벤더별 SQL/JSON 함수는 다음과 같습니다:

- `JSON_KEYS` (MySQL)
- `JSON_SET` (MySQL)
- `JSON_INSERT` (MySQL)
- `JSON_REPLACE` (MySQL)
- `JSON_REMOVE` (MySQL)
- `->` 및 `->>` 접근자 연산자 (PostgreSQL)

## 더 많은 QOM 구현

jOOQ 3.16에서 도입된 쿼리 객체 모델(Query Object Model, QOM) API가 더 많은 문(statement), 함수(function), 식(expression) 지원으로 향상되어, 보다 완전한 SQL 변환(transformation) 및 순회(traversal)가 가능해졌습니다.

QOM API는 아직 실험적(experimental) 상태입니다. 근본적인 변경은 더 이상 예상하지 않지만, 마이너 릴리스 간에 소스 비호환성(source incompatibility)이 여전히 발생할 수 있습니다.

## Oracle 연관 배열(Associative Array) 지원

Oracle에서 저장 프로시저를 사용할 때, 사용자는 Oracle PL/SQL 패키지 타입을 많이 사용하게 될 것입니다. 우리는 이전부터 PL/SQL RECORD 타입과 PL/SQL TABLE 타입을 지원해왔으며, 두 가지 모두 과거에는 ojdbc 지원이 제한적이었습니다. 연관 배열 지원은 여전히 ojdbc에서 어려울 수 있지만, jOOQ와 코드 생성기를 사용하면 대부분의 연관 배열을 매우 쉽게 바인딩하고 페치할 수 있습니다.

## PostgreSQL 다차원 배열 타입

PostgreSQL 통합에서 자주 요청받는 기능은 다차원 배열 지원입니다. 이번 버전의 jOOQ는 코드 생성(가능한 경우)과 런타임에서 다차원 Java 배열을 통해 이러한 타입을 지원합니다.

## Kotlin 관련 개선 사항

jOOQ는 Kotlin에서 SQL을 작성하는 가장 좋은 방법이기도 합니다. 우리는 항상 jOOQ-kotlin 확장 모듈을 통한 새로운 편의 기능을 모색하고 있습니다. 예를 들어:

- ResultQuery Collectors: ResultQuery에 대한 Collector 지원
- JSON 접근(JSON access): JSON 접근 유틸리티
- 생성된 코드의 더 많은 null 허용 여부(nullability) 지원: 생성된 코드에서의 null 안전성 개선

## R2DBC 1.0 지원

이번 jOOQ 버전은 R2DBC 의존성을 1.0.0.RELEASE로 업그레이드합니다.

---

전체 릴리스 노트는 [여기](https://groups.google.com/g/jooq-user/c/s76rWaWXZ2Y)에서 확인할 수 있습니다.
