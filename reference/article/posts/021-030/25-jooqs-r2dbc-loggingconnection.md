# jOOQ의 R2DBC LoggingConnection으로 모든 SQL 문 로깅하기

> 원문: https://blog.jooq.org/jooqs-r2dbc-loggingconnection-to-log-all-sql-statements/

jOOQ에는 이미 LoggingConnection이 있으며, 이는 JDBC 프록시 Connection으로서 모든 JDBC 클라이언트(Hibernate, MyBatis, JdbcTemplate, 네이티브 JDBC 등 포함)가 실행하는 모든 SQL 문을 로깅합니다.

jOOQ 3.18.0, 3.17.7, 3.16.13 버전부터 R2DBC 클라이언트에서도 모든 리액티브 쿼리를 로깅할 수 있는 LoggingConnection이 제공됩니다. 일부 R2DBC 드라이버는 이미 자체적으로 DEBUG 로깅을 수행하지만, 그렇지 않은 드라이버도 있으므로 이 유틸리티는 jOOQ 사용자나 R2DBC를 사용하는 모든 사람에게 매우 유용할 것입니다.

jOOQ 의존성을 추가하고 싶지 않다면, [GitHub에서 제공되는 LoggingConnection 코드](https://github.com/jOOQ/jOOQ)를 그대로 사용할 수 있습니다. 어떤 역할을 하는지 살펴보면 다음과 같습니다:

```java
// jOOQ의 DefaultConnection은 단순히 모든 호출을
// delegate Connection에 위임합니다
public class LoggingConnection extends DefaultConnection {

    // 다른 로거를 사용해도 됩니다
    private static final JooqLogger log =
        JooqLogger.getLogger(LoggingConnection.class);

    public LoggingConnection(Connection delegate) {
        super(delegate);
    }

    @Override
    public Publisher<Void> close() {
        return s -> {
            if (log.isDebugEnabled())
                log.debug("Connection::close");

            getDelegate().close().subscribe(s);
        };
    }

    @Override
    public Statement createStatement(String sql) {
        if (log.isDebugEnabled())
            log.debug("Connection::createStatement", sql);

        return new LoggingStatement(getDelegate().createStatement(sql));
    }

    // [...]
}
```

그리고 Statement 또는 Batch에 대해서도 동일한 래퍼가 적용됩니다:

```java
// jOOQ의 DefaultStatement는 단순히 모든 호출을
// delegate Statement에 위임합니다
public class LoggingStatement extends DefaultStatement {

    // 다른 로거를 사용해도 됩니다
    private static final JooqLogger log =
        JooqLogger.getLogger(LoggingStatement.class);

    public LoggingStatement(Statement delegate) {
        super(delegate);
    }

    @Override
    public Statement add() {
        if (log.isDebugEnabled())
            log.debug("Statement::add");

        getDelegate().add();
        return this;
    }

    @Override
    public Publisher<? extends Result> execute() {
        return s -> {
            if (log.isDebugEnabled())
                log.debug("Statement::execute");

            getDelegate().execute().subscribe(s);
        };
    }
}
```

이것이 전부입니다!
