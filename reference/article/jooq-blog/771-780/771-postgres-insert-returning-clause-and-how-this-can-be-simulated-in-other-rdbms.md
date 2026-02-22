# Postgres INSERT .. RETURNING 절과 다른 RDBMS에서 시뮬레이션하는 방법

> 원문: https://blog.jooq.org/postgres-insert-returning-clause-and-how-this-can-be-simulated-in-other-rdbms/

jOOQ의 주요 기능 중 하나는 어떤 RDBMS에서든 가장 유용한 SQL 구문과 절을 가져와서 다른 SQL 방언(dialect)에서도 사용할 수 있게 만드는 것이다.

이전에 MySQL의 `INSERT .. ON DUPLICATE KEY UPDATE` 구문에 대해 이 작업을 수행한 바 있는데, 이는 더 강력한 SQL 표준 `MERGE` 문으로 쉽게 시뮬레이션할 수 있고, 프로시저 SQL 블록이 지원되는 곳에서는 그것을 사용할 수도 있다.

최근의 요청 덕분에 Postgres의 `INSERT .. RETURNING` 절에 대해 생각하게 되었는데, 이것은 아마도 INSERT 문에서 생성된 키를 반환하는 가장 직관적이고 간결한 방법일 것이다.

이 기능의 중요성은 jOOQ의 `UpdatableRecord` 맥락에서 명확해지는데, 레코드가 삽입될 때 자신의 IDENTITY 또는 기본 키(Primary Key) 값을 갱신해야 하기 때문이다.

jOOQ 1.6.4에서는 이것이 다소 "위험한" 방식으로 처리되었는데, 삽입 직후에 테이블에서 MAX(PK) 값을 가져오는 것이었다. 분명히, 다른 트랜잭션이 먼저 커밋되는 고도로 동시성이 높은 시스템에서는 이것이 심각하게 잘못될 수 있다.

그렇다면 `INSERT .. RETURNING`을 다른 RDBMS에 어떻게 매핑할 수 있을까? 다음은 각 데이터베이스/JDBC 드라이버에서 무엇이 지원되는지에 대한 간략한 개요이다:

## 벤더별 SQL 구문 지원

Postgres와 DB2에서는 INSERT 문에 대해서도 다음과 같이 실행할 수 있다:

```java
ResultSet rs = statement.executeQuery();
```

INSERT 문에서 `java.sql.ResultSet`을 가져오기 위한 SQL 구문은 다음과 같이 작동한다:

```sql
-- Postgres
INSERT INTO .. RETURNING *

-- DB2
SELECT * FROM FINAL TABLE (INSERT INTO ..)
```

Oracle에도 유사한 절이 있다

```sql
INSERT INTO .. RETURNING INTO ?, ?, ...
```

하지만 이 PL/SQL 확장은 표준 JDBC로 쉽게 실행할 수 없다. PL/SQL 블록으로 감싸서 `java.sql.CallableStatement`로 실행하거나, 준비된 문(prepared statement)을 `OraclePreparedStatement`로 캐스팅하고 반환 값을 등록해야 한다. 여기에 설명되어 있다: https://stackoverflow.com/questions/682539/return-rowid-parameter-from-insert-statement-using-jdbc-connection-to-oracle

## 최적의 JDBC 지원

일부 RDBMS는 INSERT 문 이후 값을 반환하는 것에 대해 "최적의" 지원을 제공한다. "최적의"라 함은, 실제 키인지 여부에 관계없이 모든 테이블 컬럼을 반환할 수 있다는 의미이다. 이러한 RDBMS는 HSQLDB, Oracle, DB2이다. JDBC 드라이버에서 이것이 지원되면, Postgres의 `INSERT .. RETURNING` 절의 시뮬레이션은 매우 간단하다. 요청된 필드를 준비된 문 초기화 시점에 전달할 수 있기 때문이다:

```java
// 대소문자 구분에 주의하라!
String[] columnNames = // [...] RETURNING 절을 변환
PreparedStatement stmt = connection.prepareStatement(sql, columnNames);
```

## 제한적인 JDBC 지원

다른 RDBMS들은 값을 반환하는 것에 대해 "제한적인" 지원을 한다. 이는 생성된 IDENTITY (AUTO_INCREMENT) 값만 반환된다는 것을 의미한다. 이에 해당하는 것은 Derby, H2, MySQL, SQL Server이다.

```java
PreparedStatement stmt = connection.prepareStatement(sql,
  Statement.RETURN_GENERATED_KEYS);
```

시뮬레이션된 `INSERT .. RETURNING` 절에서 IDENTITY 컬럼 값 이상의 것이 요청된 경우, 추가적인 SELECT 문을 바로 뒤에 실행해야 한다. 트랜잭션이 클라이언트 코드에 의해 올바르게 처리된다면 (즉, SELECT가 INSERT와 동일한 트랜잭션 내에서 실행된다면), 경쟁 조건(race condition)은 발생할 수 없으며 동작은 올바르다.

## JDBC 지원 없음

안타깝게도, INSERT 문에서 값을 반환하는 것을 지원하지 않는 JDBC 드라이버도 있다. 해당 RDBMS는 Sybase, SQLite이다. jOOQ의 향후 버전에서는 `INSERT .. RETURNING` 절을 세 단계로 시뮬레이션할 수 있을 것이다:

1. INSERT 실행
2. Sybase의 `@@identity` / SQLite의 `last_insert_rowid()` 가져오기
3. 요청된 다른 컬럼 가져오기
