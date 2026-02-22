# jOOQ로 JDBC 배치 작업

> 원문: https://blog.jooq.org/jdbc-batch-operations-with-jooq/

2011년 10월 25일, lukaseder 작성

jOOQ 사용자가 오래된 JDBC 기능인 `java.sql.Statement.addBatch()` 지원을 요청했습니다.

JDBC를 사용하면 `addBatch()` 메서드를 사용하여 여러 문을 한 번에 실행할 수 있습니다. 두 가지 주요 모드가 있습니다.

1. 바인드 값 없이 여러 쿼리를 실행
2. 바인드 값을 사용하여 하나의 쿼리를 여러 번 실행

### 모드 1 - 여러 쿼리

```java
Statement stmt = connection.createStatement();
stmt.addBatch("INSERT INTO author VALUES (1, 'Erich Gamma')");
stmt.addBatch("INSERT INTO author VALUES (2, 'Richard Helm')");
stmt.addBatch("INSERT INTO author VALUES (3, 'Ralph Johnson')");
stmt.addBatch("INSERT INTO author VALUES (4, 'John Vlissides')");
int[] result = stmt.executeBatch();
```

### 모드 2 - 바인드 값을 사용한 단일 쿼리

```java
PreparedStatement stmt = connection.prepareStatement(
  "INSERT INTO author VALUES (?, ?)");
stmt.setInt(1, 1);
stmt.setString(2, "Erich Gamma");
stmt.addBatch();

stmt.setInt(1, 2);
stmt.setString(2, "Richard Helm");
stmt.addBatch();

stmt.setInt(1, 3);
stmt.setString(2, "Ralph Johnson");
stmt.addBatch();

stmt.setInt(1, 4);
stmt.setString(2, "John Vlissides");
stmt.addBatch();

int[] result = stmt.executeBatch();
```

## jOOQ 지원

곧 출시될 jOOQ 버전 1.6.9에서는 유사한 기능을 jOOQ의 API를 사용하여 배치 작업을 지원합니다.

### 모드 1 - jOOQ에서 여러 쿼리

```java
create.batch(
    create.insertInto(AUTHOR, ID, NAME).values(1, "Erich Gamma"),
    create.insertInto(AUTHOR, ID, NAME).values(2, "Richard Helm"),
    create.insertInto(AUTHOR, ID, NAME).values(3, "Ralph Johnson"),
    create.insertInto(AUTHOR, ID, NAME).values(4, "John Vlissides"))
.execute();
```

### 모드 2 - jOOQ에서 바인드 값을 사용한 단일 쿼리

```java
create.batch(create.insertInto(AUTHOR, ID, NAME).values("?", "?"))
      .bind(1, "Erich Gamma")
      .bind(2, "Richard Helm")
      .bind(3, "Ralph Johnson")
      .bind(4, "John Vlissides")
      .execute();
```
