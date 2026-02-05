# jOOQ 위에 구축된 RESTful JDBC HTTP 서버

> 원문: https://blog.jooq.org/a-restful-jdbc-http-server-built-on-top-of-jooq/

2013년 9월 9일 | 2022년 10월 20일

저자: lukaseder

---

## 소개

jOOQ를 사용하여 매우 흥미로운 오픈 소스 프로젝트를 발견했습니다. Bjorn Harrtell이 만든 [jdbc-http-server](https://github.com/bjornharrtell/jdbc-http-server)입니다. 이 프로젝트는 "관계형 데이터베이스 인스턴스를 검색 가능한 REST API로 노출하여, 백엔드 코드를 작성하지 않고도 브라우저 애플리케이션에서 간단한 CRUD 작업을 수행할 수 있게 해줍니다."

## 검색 가능한 REST API

이 REST 서버의 핵심 아이디어는 리소스를 검색 가능하게 만드는 것입니다. REST에서 일반적으로 그렇듯이, 최상위 리소스(/)로 시작하여 하위 리소스로의 링크를 따라갈 수 있습니다. 예를 들어, `public` 스키마에 `testtable`이라는 테이블을 포함하는 `testdb`라는 데이터베이스가 있다고 가정해 봅시다.

다음과 같은 방식으로 데이터에 접근할 수 있습니다:

```
/db/testdb/schemas/public/tables/testtable/rows/1
```

위의 URL에서 단일 행을 조회하거나, 업데이트하거나, 삭제할 수 있습니다. 또한 다음 위치에서 여러 행을 조회하거나, 삽입하거나, 업데이트할 수 있습니다:

```
/db/testdb/schemas/public/tables/testtable/rows
```

## 쿼리 파라미터

위의 URL은 다양한 쿼리 파라미터를 지원합니다. `select`, `where`, `limit`, `offset`, `orderby`와 같은 것들을 전달할 수 있습니다. 예를 들어, cost가 100보다 큰 행을 최대 10개까지 검색하려면 다음과 같이 요청할 수 있습니다:

```
/db/testdb/schemas/public/tables/testtable/rows?where=cost>100&limit=10
```

## jOOQ 사용

이 멋진 프로젝트가 jOOQ를 활용하여 "대상 데이터베이스 엔진에 적합한 방언으로 SQL을 생성함으로써 데이터베이스 엔진에 독립적"이 된다는 점이 매우 기쁩니다. 자동화된 테스트는 H2, PostgreSQL, HSQLDB를 포함합니다.

## 데이터 형식

현재 사용되는 유일한 데이터 형식은 JSON입니다.

## Bjorn Harrtell 소개

Bjorn은 스웨덴의 프로그래머로 주로 Java 기반 GIS 시스템을 다루고 있습니다. 그는 GeoTools와 OpenLayers와 같은 다양한 오픈 소스 프로젝트에 기여해왔습니다. jdbc-http-server에 대해 더 알고 싶거나 피드백을 제공하고 싶다면 GitHub 페이지를 방문해 주세요:

https://github.com/bjornharrtell/jdbc-http-server
