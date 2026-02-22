# 여러 커서를 반환하는 저장 프로시저

> 원문: https://blog.jooq.org/stored-procedures-returning-multiple-cursors/

jOOQ에 Sybase ASE 지원을 추가하면서, 스키마 메타 정보를 조회하는 `sp_help`라는 독특한 프로시저를 발견했습니다. Sybase ASE에서는 이 프로시저를 호출하면 스키마 내의 모든 테이블을 담은 커서를 반환받을 수 있습니다.

`sp_help`를 호출한 예시 출력은 다음과 같습니다:

| Name | Owner | Object_type |
| --- | --- | --- |
| sysquerymetrics | dbo | view |
| v_author | dbo | view |
| v_book | dbo | view |
| v_library | dbo | view |
| t_639_numbers_table | dbo | user table |

또한 테이블 이름을 매개변수로 지정할 수도 있습니다. 예를 들어 `sp_help 't_author'`를 호출하면 테이블 메타데이터(이름, 소유자, 객체 유형, 생성 날짜)와 그에 이어 컬럼 이름, 데이터 타입, 길이, 정밀도, 스케일 및 기타 속성을 포함한 컬럼 상세 정보가 반환됩니다.

## JDBC 드라이버 지원

JDBC API 설계는 이 기능을 수용합니다. 다음 RDBMS의 JDBC 드라이버는 이를 올바르게 구현합니다:

- DB2
- Derby
- H2
- HSQLDB
- Ingres
- MySQL
- Postgres
- SQLServer
- Sybase ASE (jTDS 드라이버 사용 시)

다음 드라이버는 이 API를 구현하지 않습니다:

- Oracle
- SQLite
- Sybase SQL Anywhere (jconn3 드라이버 사용 시)

## JDBC 코드 예제

JDBC로 여러 커서를 가져오는 패턴은 다음과 같습니다:

```java
ResultSet rs = statement.executeQuery();

// 더 이상 결과 집합이 없을 때까지 반복
for (;;) {

  // 현재 결과 집합을 비움
  while (rs.next()) {
    // [ .. 결과 처리 .. ]
  }

  // 다음 결과 집합이 있으면 가져옴
  if (statement.getMoreResults()) {
    rs = statement.getResultSet();
  }
  else {
    break;
  }
}

// 모든 결과 집합이 닫혔는지 확인
statement.getMoreResults(Statement.CLOSE_ALL_RESULTS);
statement.close();
```

## jOOQ 개선 사항

이것은 jOOQ에 매우 좋은 개선 사항이 될 것입니다. 1.6.7 버전부터 Sybase ASE에서 `sp_help` 프로시저를 호출하는 것이 가능해졌습니다:

```java
Factory create = new ASEFactory(connection);

// 테이블 목록, 사용자 타입 목록 등을 가져옴
List<Result<Record>> tables = create.fetchMany("sp_help");

// t_author 테이블에 대한 정보, 컬럼, 키, 인덱스 등을 가져옴
List<Result<Record>> results = create.fetchMany("sp_help 't_author'");
```
