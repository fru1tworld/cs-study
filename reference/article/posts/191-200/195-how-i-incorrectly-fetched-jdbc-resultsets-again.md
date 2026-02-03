# 내가 또 JDBC ResultSet을 잘못 가져온 방법

> 원문: https://blog.jooq.org/how-i-incorrectly-fetched-jdbc-resultsets-again/

2017년 7월 13일 게시 (2022년 8월 24일 업데이트), lukaseder 저

## 소개

JDBC 아시죠? 관계형이든 아니든 거의 모든 데이터베이스와 함께 사용하기 좋아하는 정말 쉽고 간결한 API입니다. 본질적으로 신경 써야 할 타입은 세 가지뿐입니다:

- Connection
- Statement (및 그 하위 타입들)
- ResultSet

다른 모든 타입은 일종의 유틸리티입니다.

이제 위의 세 가지를 가지고, 다음과 같이 정말 멋지고 간결한 Java/SQL 코딩을 할 수 있습니다:

```java
try (Connection c = datasource.getConnection();
     Statement s = c.createStatement();
     ResultSet r = s.executeQuery("SELECT 'hello'")
) {
    while (r.next())
        System.out.println(r.getString(1));
}
```

출력:
```
hello
```

됐죠? 엄청 쉽습니다.

## 그런데...

그런데 SQL 문자열이 무엇인지 모르기 때문에 범용 JDBC 코드를 작성하고 싶다면 어떨까요. SELECT 문일 수도 있습니다. UPDATE일 수도 있습니다. DDL일 수도 있습니다. 문장 배치(여러 문장)일 수도 있습니다. 트리거와 저장 프로시저를 호출할 수도 있고, 그러면 경고, 예외, 업데이트 카운트, 추가 결과 집합 같은 좋은 것들이 또 생성됩니다.

아시다시피, jOOQ의 `ResultQuery.fetchMany()` 같은 범용 유틸리티 메서드로 들어올 수 있는 종류의 것입니다.

(이런 일이 당신에게도 일어날 수 없다고 생각하지 마세요. SQL Server 트리거는 정말 골치 아픈 것들입니다!)

이를 위해, SQL Server에서 훌륭하게 작동하는 다음의 간단한 문장 배치를 실행하는 올바른 방법을 살펴봅시다:

```sql
raiserror('e0', 0, 2, 3);
create table t(i int);
raiserror('e1', 5, 2, 3);
insert into t values (1);
raiserror('e2', 10, 2, 3);
insert into t values (2);
raiserror('e3', 15, 2, 3);
select * from t;
drop table t;
raiserror('e4', 16, 2, 3);
```

결과는 쿼리 결과 창과 출력이 있는 메시지 창을 보여줍니다.

편의를 위해, 위의 문자열을 Java String 변수로 미리 포맷해 두었는데, 이것이 이미 첫 번째 문제입니다. Java에는 아직도 여러 줄 문자열이 없기 때문입니다 (으악):

```java
String sql =
    "\n raiserror('e0', 0, 2, 3);"
  + "\n create table t(i int);"
  + "\n raiserror('e1', 5, 2, 3);"
  + "\n insert into t values (1);"
  + "\n raiserror('e2', 10, 2, 3);"
  + "\n insert into t values (2);"
  + "\n raiserror('e3', 15, 2, 3);"
  + "\n select * from t;"
  + "\n drop table t;"
  + "\n raiserror('e4', 16, 2, 3);";
```

이제 보세요, 우리는 어떤 웹사이트(예: 이 블로그)에서 JDBC 스니펫을 복사해서 붙여넣고 첫 번째 스니펫을 가져와서 다음과 같이 실행하고 싶은 유혹을 받을 수 있습니다:

```java
try (
    Statement s = c.createStatement();
    ResultSet r = s.executeQuery(sql)
) {
    while (r.next())
        System.out.println(r.getString(1));
}
```

네. 이렇게 하면 어떻게 될까요?

젠장:

```
com.microsoft.sqlserver.jdbc.SQLServerException: e3
	at com.microsoft.sqlserver.jdbc.SQLServerException.makeFromDatabaseError(SQLServerException.java:258)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getNextResult(SQLServerStatement.java:1547)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.doExecuteStatement(SQLServerStatement.java:857)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement$StmtExecCmd.doExecute(SQLServerStatement.java:757)
	at com.microsoft.sqlserver.jdbc.TDSCommand.execute(IOBuffer.java:7151)
	at com.microsoft.sqlserver.jdbc.SQLServerConnection.executeCommand(SQLServerConnection.java:2689)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.executeCommand(SQLServerStatement.java:224)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.executeStatement(SQLServerStatement.java:204)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.executeQuery(SQLServerStatement.java:659)
	at SQLServer.main(SQLServer.java:80)
```

e3? 대체 뭐죠? 그래서 제 문장 배치는 어떻게 됐나요? 실행됐나요? 중간까지만요? 아니면 끝까지 갔나요?

좋아요, 분명히 우리는 이것을 더 조심스럽게 해야 합니다. 여기서 `Statement.executeQuery()`를 사용할 수 없습니다. 왜냐하면 결과 집합을 받을지 모르기 때문입니다. 사실 예외를 받았는데, 첫 번째 예외가 아니었습니다.

다른 것을 시도해 봅시다. 이것을 시도해 봅시다:

```java
try (Statement s = c.createStatement()) {
    System.out.println(s.execute(sql));
}
```

이것은 그냥 다음을 출력합니다:

```
false
```

오케이, 데이터베이스에서 실제로 뭔가 실행됐나요? 더 이상 예외가 없네요... SQL Server Profiler를 살펴봅시다...

[SQL Server 배치 실행을 보여주는 이미지]

아니요, 전체 배치가 실행됐습니다. (물론 DROP TABLE 문을 제거하고 SQL Server Management Studio에서 테이블 T의 내용을 확인할 수도 있었을 겁니다).

흠, 우리가 어떤 메서드를 호출하느냐에 따라 꽤 다른 결과가 나오네요. 무섭지 않나요? 당신의 ORM은 이것을 제대로 처리하나요? (jOOQ는 그렇지 않았지만 이제 수정되었습니다).

좋아요, `Statement.execute()`에 대한 Javadoc을 읽어봅시다.

이렇게 말합니다: "주어진 SQL 문을 실행하며, 여러 결과를 반환할 수 있습니다. 일부 (흔치 않은) 상황에서는 단일 SQL 문이 여러 결과 집합 및/또는 업데이트 카운트를 반환할 수 있습니다. 일반적으로 (1) 여러 결과를 반환할 수 있다고 알고 있는 저장 프로시저를 실행하거나 (2) 알 수 없는 SQL 문자열을 동적으로 실행하지 않는 한 이것을 무시할 수 있습니다. execute 메서드는 SQL 문을 실행하고 첫 번째 결과의 형태를 나타냅니다. 그런 다음 결과를 검색하려면 getResultSet 또는 getUpdateCount 메서드를 사용하고, 후속 결과로 이동하려면 getMoreResults를 사용해야 합니다."

흠, 좋아요. `Statement.getResultSet()`과 `getUpdateCount()`를 사용해야 하고, 그 다음 `getMoreResults()`를 사용해야 합니다.

`getMoreResults()` 메서드에도 이 흥미로운 정보가 있습니다:

"다음이 참일 때 더 이상의 결과가 없습니다:

// stmt는 Statement 객체입니다
((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))"

흥미롭네요. -1이라니. 적어도 null이나 얼굴에 주먹을 반환하지 않는 것에 매우 감사해야 할 것 같습니다.

그래서, 다시 시도해 봅시다:

- 먼저 `execute()`를 호출해야 합니다
- true이면 `getResultSet()`을 가져옵니다
- false이면 `getUpdateCount()`를 확인합니다
- 그것이 -1이면 멈출 수 있습니다

또는, 코드로:

```java
fetchLoop:
for (int i = 0, updateCount = 0; i < 256; i++) {
    boolean result = (i == 0)
        ? s.execute(sql)
        : s.getMoreResults();

    if (result)
        try (ResultSet rs = s.getResultSet()) {
            System.out.println("Result      :");

            while (rs.next())
                System.out.println("  " + rs.getString(1));
        }
    else if ((updateCount = s.getUpdateCount()) != -1)
        System.out.println("Update Count: " + updateCount);
    else
        break fetchLoop;
}
```

아름답습니다! 몇 가지 참고 사항:

- 루프가 256번 반복 후에 멈추는 것을 주목하세요. 이런 무한 스트리밍 API를 절대 믿지 마세요, 어딘가에 항상 버그가 있습니다, 저를 믿으세요
- `Statement.execute()`와 `Statement.getMoreResults()`에서 반환되는 boolean 값은 같습니다. 루프 내에서 변수에 할당하고 첫 번째 반복에서만 execute를 호출할 수 있습니다
- true이면 결과 집합을 가져옵니다
- false이면 업데이트 카운트를 확인합니다
- 그것이 -1이면 멈춥니다

실행 시간!

```
Update Count: 0
Update Count: 1
Update Count: 1
com.microsoft.sqlserver.jdbc.SQLServerException: e3
	at com.microsoft.sqlserver.jdbc.SQLServerException.makeFromDatabaseError(SQLServerException.java:258)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getNextResult(SQLServerStatement.java:1547)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getMoreResults(SQLServerStatement.java:1270)
	at SQLServer.main(SQLServer.java:83)
```

젠장. 그런데 완전히 실행됐나요? 네, 그랬습니다. 하지만 그 예외 때문에 e3 이후의 멋진 결과 집합을 얻지 못했습니다. 하지만 적어도 이제 3개의 업데이트 카운트를 얻었습니다. 그런데 잠깐, 왜 e0, e1, e2를 받지 못했죠?

아하, 그것들은 예외가 아니라 경고입니다. 이상한 SQL Server는 특정 심각도 수준 아래의 모든 것이 경고라고 결정했습니다. 뭐든 간에.

어쨌든, 그 경고들도 가져옵시다!

```java
fetchLoop:
for (int i = 0, updateCount = 0; i < 256; i++) {
    boolean result = (i == 0)
        ? s.execute(sql)
        : s.getMoreResults();

    // 여기서 경고 처리
    SQLWarning w = s.getWarnings();
    for (int j = 0; j < 255 && w != null; j++) {
        System.out.println("Warning     : " + w.getMessage());
        w = w.getNextWarning();
    }

    // 이것을 잊지 마세요
    s.clearWarnings();

    if (result)
        try (ResultSet rs = s.getResultSet()) {
            System.out.println("Result      :");

            while (rs.next())
                System.out.println("  " + rs.getString(1));
        }
    else if ((updateCount = s.getUpdateCount()) != -1)
        System.out.println("Update Count: " + updateCount);
    else
        break fetchLoop;
}
```

좋습니다, 이제 업데이트 카운트와 함께 모든 경고 e0, e1, e2와 예외 e3을 얻습니다:

```
Warning     : e0
Update Count: 0
Warning     : e1
Update Count: 1
Warning     : e2
Update Count: 1
com.microsoft.sqlserver.jdbc.SQLServerException: e3
	at com.microsoft.sqlserver.jdbc.SQLServerException.makeFromDatabaseError(SQLServerException.java:258)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getNextResult(SQLServerStatement.java:1547)
	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getMoreResults(SQLServerStatement.java:1270)
	at SQLServer.main(SQLServer.java:82)
```

이것이 더 우리 배치 같습니다. 하지만 여전히 e3 이후에 중단됩니다. 어떻게 하면 결과 집합을 얻을 수 있을까요? 쉽습니다! 그냥 예외를 무시하면 되죠, 맞죠? :)

그리고 하는 김에, `ResultSetMetaData`를 사용해서 알 수 없는 결과 집합 타입을 읽어봅시다.

```java
fetchLoop:
for (int i = 0, updateCount = 0; i < 256; i++) {
    try {
        boolean result = (i == 0)
            ? s.execute(sql)
            : s.getMoreResults();

        SQLWarning w = s.getWarnings();
        for (int j = 0; j < 255 && w != null; j++) {
            System.out.println("Warning     : " + w.getMessage());
            w = w.getNextWarning();
        }

        s.clearWarnings();

        if (result)
            try (ResultSet rs = s.getResultSet()) {
                System.out.println("Result      :");
                ResultSetMetaData m = rs.getMetaData();

                while (rs.next())
                    for (int c = 1; c <= m.getColumnCount(); c++)
                        System.out.println(
                            "  " + m.getColumnName(c) +
                            ": " + rs.getInt(c));
            }
        else if ((updateCount = s.getUpdateCount()) != -1)
            System.out.println("Update Count: " + updateCount);
        else
            break fetchLoop;
        }
    catch (SQLException e) {
        System.out.println("Exception   : " + e.getMessage());
    }
}
```

자, 이제 제대로 됐습니다:

```
Warning     : e0
Update Count: 0
Warning     : e1
Update Count: 1
Warning     : e2
Update Count: 1
Exception   : e3
Result      :
  i: 1
  i: 2
Update Count: 0
Exception   : e4
```

이제 JDBC로 전체 배치를 매우 범용적인 방식으로 실행했습니다.

## 으, 이게 더 쉬웠으면 좋겠어요

물론 그러고 싶으시겠죠, 그래서 jOOQ가 있습니다. jOOQ에는 정말 멋진 `fetchMany()` 메서드가 있어서, 다음의 혼합을 얻기 위해 임의의 SQL 문자열을 실행할 수 있습니다:

- 업데이트 카운트
- 결과 집합
- 예외 / 경고 (jOOQ 3.10+ 전용)

예를 들어, 다음과 같이 작성할 수 있습니다:

```java
// 이 새로운 설정을 사용해서 예외를 던지지 않고
// 위에서 본 것처럼 수집하겠다고 표시합니다
DSLContext ctx = DSL.using(c,
  new Settings().withThrowExceptions(THROW_NONE));

// 그리고 나서, 간단히:
System.out.println(ctx.fetchMany(sql));
```

결과는 다음 형식입니다:

```
Warning: SQL [...]; e0
Update count: 0
Warning: SQL [...]; e1
Update count: 1
Warning: SQL [...]; e2
Update count: 1
Exception: SQL [...]; e3
Result set:
+----+
|   i|
+----+
|   1|
|   2|
+----+
Update count: 0
Exception: SQL [...]; e4
```

훌륭합니다!

## 다루지 않은 것들

오, 엄청나게 많지만, 저도 미래 블로그 포스트를 위한 자료가 필요하잖아요, 맞죠?

- 지금까지 SQL Server만 논의했습니다
- `SQLException.getNextException()`이 여기서 작동하지 않는다는 사실을 논의하지 않았습니다
- OUT 매개변수와 어떻게 결합할 수 있는지 논의하지 않았습니다 (으, 어느 시점에 그것들을 가져오죠)
- 일부 JDBC 드라이버가 이것을 올바르게 구현하지 않는다는 사실을 논의하지 않았습니다 (당신을 보고 있어요, Oracle)
- JDBC 드라이버가 `ResultSetMetaData`를 올바르게 구현하지 않는 깊은 곳까지 들어가지 않았습니다
- 예를 들어 MySQL에서 경고를 가져오는 성능 오버헤드를 다루지 않았습니다
- ... 그리고 훨씬 더 많은 것들

그래서, 아직도 직접 JDBC 코드를 작성하고 계신가요? :)
