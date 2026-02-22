# JDBC 4.0의 잘 알려지지 않은 Clob.free()와 Blob.free() 메서드

> 원문: https://blog.jooq.org/jdbc-4-0s-lesser-known-clob-free-and-blob-free-methods/

최근 저는 이 6가지 흔한 JDBC 버그에 대해 물었습니다:

```java
import java.sql.*;

public class Fetcher {
    public Clob fetch(Connection connection, String name)
    throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Clob clob = null;
        try {
            String sql = "SELECT clob" +
                        " FROM table"
                        " WHERE name = '" + name + "'";
            statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            resultSet = statement.executeQuery();
            if (resultSet.next())
                clob = resultSet.getClob("clob");
        }
        finally {
            if (resultSet != null)
                resultSet.close();
            if (statement != null)
                statement.close();
        }
        return clob;
    }
}
```

가장 흔한 버그들은 다음과 같이 발견되었습니다:

1. 4번째 줄의 구문 오류 (잘못된 문자열 연결)
2. 7번째 줄의 SQL 인젝션 취약점 (변수를 SQL에 직접 삽입)
3. 8번째 줄의 잘못된 바인드 인덱스 (바인드 변수가 없는데 setString 호출)
4. 14번째 줄의 컬럼명 오류 (부주의한 리팩토링으로 인한 문제)
5. 18번째 줄의 부적절한 리소스 관리

하지만 많은 사람들이 놓친 것은 6번째 버그: 15번째 줄에서 `Clob.free()`를 호출하지 않는 것입니다.

## LOB 리소스 해제의 중요성

JDBC 4.0(Java 6)에서는 `Clob.free()`와 `Blob.free()` 메서드가 도입되었습니다. JDBC 명세에 따르면:

> "Blob, Clob, NClob Java 객체는 적어도 그것들이 생성된 트랜잭션이 지속되는 동안 유효합니다. 이는 장기 실행 트랜잭션 중에 애플리케이션의 리소스가 고갈될 수 있는 잠재적인 원인이 될 수 있습니다."

## 해결책

LOB와 배열 객체는 사용이 끝나면 즉시 `free()` 메서드를 호출해야 합니다. 가비지 컬렉션에 의존하지 마세요. 동일한 원칙이 `Array.free()`에도 적용됩니다.

```java
try {
    // Clob 사용
    clob = resultSet.getClob("clob");
    // clob으로 작업 수행...
} finally {
    if (clob != null)
        clob.free(); // 리소스 즉시 해제
}
```

## 왜 중요한가?

특정 데이터베이스와 JDBC 드라이버에서 LOB는 개별 문장이나 트랜잭션보다 오래 유지될 수 있습니다. 따라서 장기 실행 애플리케이션에서는 조기 리소스 정리가 매우 중요합니다.

저는 이 패턴을 잠재적인 버그로 인식할 수 있도록 FindBugs에 이슈를 제출했습니다.

## 현대적 고려사항

`Clob`과 `Blob`이 `AutoCloseable`을 구현했다면 try-with-resources 문을 사용할 수 있었을 것입니다. 하지만 이는 API를 개조해야 하므로 현실적으로 어렵습니다.

결론적으로, 이것은 많은 개발자들이 간과하는 흔한 JDBC 실수입니다. LOB 객체를 사용할 때는 항상 `free()` 메서드를 호출하는 습관을 들이세요.
