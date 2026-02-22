# JDBC와 jOOQ로 SQL 배치(Batch) 실행하는 방법

> 원문: https://blog.jooq.org/how-to-execute-sql-batches-with-jdbc-and-jooq/

## 개요

이 글에서는 MySQL이나 SQL Server와 같은 데이터베이스에서 여러 SQL 문을 단일 배치 작업으로 실행하는 방법을 설명합니다.

여기서 말하는 "배치(batch)"는 `Statement.addBatch()` 메서드와는 다른 개념입니다. 여러 SQL 문을 순차적으로 실행하는 것을 의미합니다.

## JDBC 방식

기본 JDBC 방식으로 배치를 실행하려면 Statement API의 상태 머신(State Machine)을 이해해야 합니다.

```java
String sql =
    "\n  -- Statement #1                              "
  + "\n  DECLARE @table AS TABLE (id INT);            "
  + "\n                                               "
  + "\n  -- Statement #2                              "
  + "\n  SELECT * FROM @table;                        "
  + "\n                                               "
  + "\n  -- Statement #3                              "
  + "\n  INSERT INTO @table VALUES (1),(2),(3);       "
  + "\n                                               "
  + "\n  -- Statement #4                              "
  + "\n  SELECT * FROM @table;                        ";

try (PreparedStatement s = c.prepareStatement(sql)) {
    fetchLoop:
    for (int i = 0, updateCount = 0;; i++) {
        boolean result = (i == 0)
            ? s.execute()
            : s.getMoreResults();

        if (result)
            try (ResultSet rs = s.getResultSet()) {
                System.out.println("\nResult:");

                while (rs.next())
                    System.out.println("  " + rs.getInt(1));
            }
        else if ((updateCount = s.getUpdateCount()) != -1)
            System.out.println("\nUpdate Count: " + updateCount);
        else
            break fetchLoop;
    }
}
```

### 주요 JDBC 메서드

- `Statement.execute()` - 결과가 ResultSet인지 여부를 나타내는 boolean을 반환합니다.
- `Statement.getMoreResults()` - 다음 문장의 결과로 이동합니다.
- `Statement.getResultSet()` - 현재 ResultSet을 가져옵니다.
- `Statement.getUpdateCount()` - 행 개수를 가져오며, 배치가 끝나면 -1을 반환합니다.

이 알고리즘은 ResultSet과 업데이트 카운트가 혼합된 결과를 순회하기 위해 약 20줄의 코드가 필요합니다.

## jOOQ 방식

jOOQ를 사용하면 동일한 작업이 훨씬 간결해집니다.

```java
System.out.println(
    DSL.using(c).fetchMany(sql)
);
```

`DSLContext.fetchMany()` 메서드는 모든 결과와 업데이트 카운트를 한 번에 즉시 가져옵니다. 반환 타입인 `Results`는 `List<Result>`를 확장하므로 편리하게 순회할 수 있습니다.

## 보안 참고사항

배치 실행 기능 자체가 SQL 인젝션(SQL Injection) 취약점을 증가시키지는 않습니다. 일반적인 SQL 인젝션 위협과 동일한 주의가 필요합니다.
