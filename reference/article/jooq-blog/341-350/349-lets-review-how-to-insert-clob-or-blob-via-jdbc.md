# JDBC로 Clob이나 Blob을 삽입하는 방법을 복습하자

> 원문: https://blog.jooq.org/lets-review-how-to-insert-clob-or-blob-via-jdbc/

LOB(Large Object)는 모든 데이터베이스에서, 그리고 JDBC에서도 골칫거리입니다. 이들을 올바르게 처리하려면 몇 줄의 코드가 필요하며, 결국에는 실수를 하게 될 것이라고 확신할 수 있습니다.

Hibernate나 jOOQ 같은 추상화 라이브러리를 사용한다면 이러한 세부 사항에서 대부분 벗어날 수 있습니다. 하지만 직접 JDBC를 사용한다면, 이런 유틸리티를 직접 작성해야 합니다.

LOB에 대해 알아야 할 몇 가지 사항들이 있습니다:

- LOB는 특별한 생명주기 관리가 필요합니다. LOB를 할당했다면, GC(가비지 컬렉터)에 대한 부담을 줄이기 위해 반드시 `free()`를 호출해야 합니다
- LOB를 할당하고 해제하는 타이밍은 매우 중요합니다. LOB는 ResultSet, PreparedStatement 또는 Connection보다 오래 살아남을 수 있습니다
- 작거나 중간 크기의 LOB를 바인딩할 때 `Clob` 대신 `String`을 사용하거나 `Blob` 대신 `byte[]`를 사용하면, 실제 대용량 데이터를 바인딩해야 할 때 Oracle의 `ORA-01461`과 같은 심각한 오류가 발생할 수 있습니다

## 유틸리티 클래스

다음은 이 모든 것을 추상화하는 간단한 유틸리티 클래스입니다:

```java
public class LOB implements AutoCloseable {
    private final Connection connection;
    private final List<Blob> blobs;
    private final List<Clob> clobs;

    public LOB(Connection connection) {
        this.connection = connection;
        this.blobs = new ArrayList<>();
        this.clobs = new ArrayList<>();
    }

    public final Blob blob(byte[] bytes)
    throws SQLException {
        Blob blob;

        // Oracle은 표준 JDBC와 다른 방식으로 임시 LOB를 생성해야 합니다
        if (connection.getMetaData()
                      .getDatabaseProductName()
                      .toLowerCase()
                      .contains("oracle")) {
            blob = BLOB.createTemporary(connection,
                       false, BLOB.DURATION_SESSION);
        }
        else {
            blob = connection.createBlob();
        }

        blob.setBytes(1, bytes);
        blobs.add(blob);
        return blob;
    }

    public final Clob clob(String string)
    throws SQLException {
        Clob clob;

        // Oracle은 표준 JDBC와 다른 방식으로 임시 LOB를 생성해야 합니다
        if (connection.getMetaData()
                      .getDatabaseProductName()
                      .toLowerCase()
                      .contains("oracle")) {
            clob = CLOB.createTemporary(connection,
                       false, CLOB.DURATION_SESSION);
        }
        else {
            clob = connection.createClob();
        }

        clob.setString(1, string);
        clobs.add(clob);
        return clob;
    }

    @Override
    public final void close() throws Exception {
        blobs.forEach(JDBCUtils::safeFree);
        clobs.forEach(JDBCUtils::safeFree);
    }
}
```

## 사용 방법

이 클래스를 사용하려면, 다음과 같이 간단히 작성하면 됩니다:

```java
try (
    LOB lob = new LOB(connection);
    PreparedStatement stmt = connection.prepareStatement(
        "insert into lobs (id, lob) values (?, ?)")
) {
    stmt.setInt(1, 1);
    stmt.setClob(2, lob.clob("abc"));
    stmt.executeUpdate();
}
```

## 이 유틸리티 클래스의 장점

이 접근 방식에는 세 가지 주요 장점이 있습니다:

- AutoCloseable 지원: try-with-resources 문을 사용할 수 있어 자동으로 리소스가 정리됩니다
- SQL 방언 추상화: Oracle 전용 방식이나 표준 JDBC 방식을 기억할 필요가 없습니다. 유틸리티가 알아서 처리합니다
- 간소화된 리소스 관리: LOB에 대한 참조를 유지하거나, null이 아닌지 확인하고 안전하게 해제하거나, 예외로부터 올바르게 복구하는 등의 작업이 필요 없습니다

## 마무리

`Clob.free()`와 `Blob.free()`를 호출하는 것이 왜 중요한지, 그리고 이것이 어떻게 `OutOfMemoryError`를 방지하는지에 대한 자세한 내용은 관련 글 "JDBC 4.0의 잘 알려지지 않은 Clob.free()와 Blob.free() 메서드"를 참조하세요.
