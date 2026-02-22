# jOOQ 3.19.0 출시 - DuckDB, Trino, Oracle 23c 지원, 조인 경로 개선, 공식 Gradle 플러그인, 상용 Maven 저장소, 정책, UDT 경로, 트리거 메타데이터, 계층 구조 등

> 원문: https://blog.jooq.org/jooq-3-19-0-released-with-duckdb-trino-oracle-23c-support-join-path-improvements-an-official-gradle-plugin-commercial-maven-repositories-policies-udt-paths-trigger-meta-data-hierarchies-and/

## 새로운 데이터베이스 방언(Dialect)

jOOQ 3.19는 두 가지 중요한 새로운 데이터베이스 시스템에 대한 지원을 도입합니다: [DuckDB](https://duckdb.org/) (실험적 지원)와 [Trino](https://trino.io/)이며, 두 데이터베이스 모두 모든 jOOQ 에디션에서 사용할 수 있습니다.

## 새로운 방언 버전

이번 릴리스에서는 [CockroachDB 23](https://www.cockroachlabs.com/)과 [Oracle 23c](https://www.oracle.com/database/)를 포함한 업데이트된 데이터베이스 버전에 대한 상당한 지원이 추가되었습니다. Oracle 23c는 도메인(domain), `UPDATE .. FROM` 구문, `IF [NOT] EXISTS` 절, 테이블 값 생성자(table value constructor), `FROM` 없는 `SELECT` 문 등의 기능을 지원합니다. CockroachDB 23은 트리거, 저장 함수(stored function), UDT, 구체화된 뷰(materialized view), `LATERAL` 조인, 네이티브 DML 절, `NULLS` 위치 지정 옵션 등을 지원합니다.

## 조인 경로 개선

경로 조인 기능이 크게 확장되었습니다:

- [명시적 경로 조인(Explicit path join)](https://www.jooq.org/doc/3.19/manual/sql-building/sql-statements/select-statement/explicit-path-join/)을 통한 세밀한 제어
- [다대(to-many) 경로 조인](https://www.jooq.org/doc/3.19/manual/sql-building/sql-statements/select-statement/implicit-to-many-join/)을 통한 부모-자식 관계 및 다대다(many-to-many) 연산 지원
- [암시적 경로 상관관계(Implicit path correlation)](https://www.jooq.org/doc/3.19/manual/sql-building/sql-statements/select-statement/implicit-path-correlation/)를 통한 상관 서브쿼리 간소화 (MULTISET 변형 포함)
- 조인 제거(Join elimination)를 통한 불필요한 경로 세그먼트 제거

### 예제 비교

3.19 이전:

```java
ctx.select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
   .from(ACTOR)
   .where(exists(
       selectOne()
       .from(FILM_ACTOR)
       .where(FILM_ACTOR.ACTOR_ID.eq(ACTOR.ACTOR_ID))
       .and(FILM_ACTOR.film().TITLE.like("A%"))
   ))
   .fetch();
```

3.19 이후:

```java
ctx.select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
   .from(ACTOR)
   .where(exists(
       selectOne()
       .from(ACTOR.film())
       .where(ACTOR.film().TITLE.like("A%"))
   ))
   .fetch();
```

이 기능은 모든 jOOQ 에디션에서 사용할 수 있습니다.

## Gradle 플러그인

공식 [jooq-codegen-gradle](https://www.jooq.org/doc/3.19/manual/code-generation/codegen-gradle/) 플러그인이 출시되었습니다. 이 플러그인은 "jOOQ 자체와 동일한 릴리스 주기로 배포되면서 Gradle의 태스크 시스템과 긴밀하게 통합"됩니다. 이 플러그인은 관용적인 Groovy 및 Kotlin DSL 구현을 모두 지원하며, 모든 에디션에서 사용할 수 있습니다.

## 상용 Maven 저장소

상용 고객은 [https://repo.jooq.org](https://repo.jooq.org/)에 위치한 전용 저장소에 접근할 수 있습니다. 이 저장소는 과거 및 새로운 상용 전용 아티팩트와 스냅샷 버전을 호스팅하며, 기존 ZIP 다운로드 웹사이트를 보완합니다.

## 정책(Policies)

PostgreSQL의 `POLICY` 기능과 유사하게, jOOQ 3.19에서는 행 수준 보안(row-level security) 구현을 위한 자동 테이블 필터로 작동하는 정책을 선언할 수 있습니다. 단순한 `CUSTOMER` 쿼리가 모든 행에 접근하는 대신 `TENANT_ID`에 의한 자동 `WHERE` 절 필터링과 함께 실행될 수 있습니다.

```java
ctx.select(CUSTOMER.ID, CUSTOMER.NAME)
   .from(CUSTOMER)
   .fetch();
```

이렇게 하면 자동으로 필터링이 적용된 상태로 실행됩니다. 이 기능은 [상용 전용](https://www.jooq.org/doc/3.19/manual/sql-building/queryparts/policies/)입니다.

## UDT 경로

사용자 정의 타입(UDT) 지원이 타입 안전한 속성 경로 접근으로 확장되었습니다. country, name, address와 같은 중첩된 UDT가 주어지면, 개발자가 이를 직접 구조 분해(destructure)할 수 있습니다:

```java
ctx.select(
        CUSTOMER.NAME.FIRST_NAME,
        CUSTOMER.NAME.LAST_NAME,
        CUSTOMER.ADDRESS.COUNTRY.ISO_CODE)
   .from(CUSTOMER)
   .fetchOne();
```

이 기능은 모든 에디션에서 사용할 수 있습니다. 자세한 내용은 [UDT 속성 경로 문서](https://www.jooq.org/doc/3.19/manual/sql-building/column-expressions/user-defined-type-attribute-paths/)를 참고하세요.

## 트리거 메타데이터

코드 생성기가 이제 대부분의 지원 RDBMS에서 트리거 메타데이터를 역공학(reverse engineer)합니다. 이는 SQLite나 SQL Server 같은 방언에서 향상된 `RETURNING` 지원 및 특수 트리거 에뮬레이션을 위한 유용한 런타임 정보를 제공합니다. 이 기능은 상용 전용입니다.

## 계층 구조(Hierarchies)

새로운 Collector가 평탄한(flat) 계층 데이터를 객체 계층 구조로 재귀적으로 변환합니다. 이는 MULTISET 중첩 컬렉션 지원과 잘 통합되어 데이터 구조화를 개선합니다. 자세한 내용은 [Java, SQL 또는 jOOQ에서 평탄한 요소 목록을 계층 구조로 변환하는 방법](https://blog.jooq.org/how-to-turn-a-list-of-flat-elements-into-a-hierarchy-in-java-sql-or-jooq/) 블로그 포스트를 참고하세요.

## Java 8 지원 변경

jOOQ Express 및 Professional 에디션에서 Java 8 지원이 중단됩니다. Java 8이 필요한 사용자는 Enterprise 에디션으로 업그레이드하거나, 지속적인 버그 수정을 위해 버전 3.18에 머무를 수 있습니다. Enterprise 에디션은 향후 몇 차례의 마이너 릴리스 동안 Java 8 지원을 계속할 예정입니다.

## 추가 개선 사항

위에서 언급한 주요 기능 외에도 많은 소규모 개선 사항, 버그 수정 등이 포함되어 있습니다. 자세한 내용은 [릴리스 노트](https://www.jooq.org/notes)를 참고하세요.

jOOQ 3.19는 [다운로드 페이지](https://www.jooq.org/download/versions)에서 받을 수 있습니다.
