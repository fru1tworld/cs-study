> 원문: https://blog.jooq.org/how-to-fetch-oracle-12c-implicit-cursors-with-jdbc-and-jooq/

# JDBC와 jOOQ로 Oracle 12c 암시적 커서(Implicit Cursor)를 가져오는 방법

게시일: 2017년 2월 8일 | 작성자: lukaseder

## 암시적 커서(Implicit Cursor)란 무엇인가?

Oracle 12c는 DBMS_SQL 동적 SQL API에 새로운 프로시저를 도입했습니다. 이 플랫폼은 개발자가 커서를 열고 `DBMS_SQL.RETURN_RESULT`를 사용하여 호출자에게 반환할 수 있도록 합니다. 이 메커니즘을 통해 프로시저는 실행이 완료된 후 부수 효과(side effect)로 여러 결과 집합(result set)을 반환할 수 있습니다.

PL/SQL 블록 예제:
```sql
DECLARE
  c1 sys_refcursor;
  c2 sys_refcursor;
BEGIN
  OPEN c1 FOR SELECT 1 AS a FROM dual;
  dbms_sql.return_result(c1);
  OPEN c2 FOR SELECT 2 AS b FROM dual;
  dbms_sql.return_result(c2);
END;
```

Oracle 12c에서 프로시저를 호출할 때, 개발자는 주요 결과와 함께 암시적 커서가 반환될지 여부를 미리 알 수 없습니다.

## JDBC로 암시적 커서 발견하기

일반적으로 JDBC 개발자들은 알 수 없는 결과 유형을 발견하기 위해 `Statement.execute()` 또는 `PreparedStatement.execute()`를 사용합니다. 그러나 "Oracle의 JDBC 드라이버는 JDBC 스펙을 올바르게 구현하지 않았기 때문에" 수정된 접근 방식이 필요합니다.

표준 JDBC 루프 (Oracle에서는 작동하지 않음):
```java
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

Oracle 호환 구현:
```java
try (PreparedStatement s = cn.prepareStatement(sql)) {
    Boolean result = s.execute();

    fetchLoop:
    for (int i = 0;; i++) {

        if (i > 0 && result == null)
            result = s.getMoreResults();
        System.out.println(result);

        if (result) {
            result = null;

            try (ResultSet rs = s.getResultSet()) {
                System.out.println("Fetching result " + i);
            }
            catch (SQLException e) {
                if (e.getErrorCode() == 17283)
                    continue fetchLoop;
                else
                    throw e;
            }
        }
        else if (s.getUpdateCount() == -1)
            if (result = s.getMoreResults())
                continue fetchLoop;
            else
                break fetchLoop;
    }
}
```

핵심 구현 세부사항:

이 솔루션은 3값 불리언 로직(three-valued boolean logic)을 사용하며, Oracle 특유의 두 가지 동작을 처리합니다:

1. 실행이 성공했음에도 결과 집합에 접근할 때 ORA-17283 오류가 발생할 수 있으므로, 오류를 억제해야 합니다.
2. PreparedStatement의 경우, 초기 `execute()` 호출이 false를 반환하고 업데이트 카운트가 -1이지만, 추가 결과 집합이 존재하므로 재시도가 필요합니다.

## jOOQ를 사용한 암시적 커서 처리

jOOQ 3.10 이상 버전은 이러한 복잡성을 추상화합니다. 개발자는 단일 메서드 호출로 여러 결과 집합을 가져올 수 있습니다:

```java
System.out.println(
    DSL.using(cn).fetchMany(sql)
);
```

이것은 포맷된 출력을 포함하는 `org.jooq.Results` 객체를 생성합니다:

```
Result set:
+----+
|   A|
+----+
|   1|
+----+
Result set:
+----+
|   A|
+----+
|   2|
+----+
```

생성된 저장 프로시저 사용법:
```java
MyProcedure p = new MyProcedure();
p.setParameter1(x);
p.setParameter2(y);
p.execute(configuration);
Results results = p.getResults();

for (Result<?> result : results)
  for (Record record : result)
    System.out.println(record);
```
