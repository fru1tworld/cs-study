# jOOQ 3.15와 R2DBC로 리액티브 SQL
> 원문: https://blog.jooq.org/reactive-sql-with-jooq-3-15-and-r2dbc/

게시일: 2021년 7월 15일

저자: lukaseder

## 소개

최근 출시된 jOOQ 3.15의 가장 큰 새 기능 중 하나는 R2DBC를 통한 리액티브 쿼리 지원입니다. 이것은 매우 인기 있는 기능 요청이었고, 마침내 구현하게 되었습니다.

여러분은 기존처럼 jOOQ를 계속 사용할 수 있습니다. Java, Kotlin, Scala에서 타입 안전한 내장 SQL을 제공하면서도, 쿼리 실행이 더 이상 블로킹되지 않습니다. 대신, jOOQ의 `ResultQuery`나 `Query`를 원하는 reactive-streams 구현체에서 `Publisher<R>` 또는 `Publisher<Integer>`로 사용할 수 있습니다.

## 설정

jOOQ `DSLContext`를 JDBC `java.sql.Connection`이나 `javax.sql.DataSource`로 설정하는 대신(또는 추가로), R2DBC `io.r2dbc.spi.Connection`이나 `io.r2dbc.spi.ConnectionFactory`로 설정하면 됩니다:

```java
ConnectionFactory connectionFactory = ConnectionFactories.get(
    ConnectionFactoryOptions
        .parse("r2dbc:h2:file://localhost/~/r2dbc-test")
        .mutate()
        .option(ConnectionFactoryOptions.USER, "sa")
        .option(ConnectionFactoryOptions.PASSWORD, "")
        .build()
);

DSLContext ctx = DSL.using(connectionFactory);
```

또는 Spring Boot를 사용하여 jOOQ를 자동 설정할 수도 있습니다.

## 기본 예제

이 `DSLContext`에서 시작하여 평소처럼 쿼리를 작성할 수 있지만, 일반적인 블로킹 `execute()`나 `fetch()` 메서드를 호출하는 대신, 예를 들어 쿼리를 `Flux`로 감싸면 됩니다. H2 `INFORMATION_SCHEMA`에서 jOOQ 코드 생성기를 실행했다고 가정하면, 다음과 같이 작성할 수 있습니다:

```java
record Table(String schema, String table) {}

Flux.from(ctx
        .select(
            INFORMATION_SCHEMA.TABLES.TABLE_SCHEMA,
            INFORMATION_SCHEMA.TABLES.TABLE_NAME)
        .from(INFORMATION_SCHEMA.TABLES))

    // Record2<String, String>에서 Table::new로의 타입 안전 매핑
    .map(Records.mapping(Table::new))
    .doOnNext(System.out::println)
    .subscribe();
```

## 리소스 관리

jOOQ는 `ConnectionFactory`에서 R2DBC `Connection`을 획득하고, 쿼리 실행 후 다시 해제합니다. 이를 통해 최적화된 리소스 관리가 가능한데, 이는 R2DBC와 Reactor에서 수동으로 처리하기에는 다소 까다로운 부분입니다.

다시 말해, 위의 실행은 다음과 같이 수동으로 작성한 쿼리에 해당합니다:

```java
Flux.usingWhen(
        connectionFactory.create(),
        c -> c.createStatement(
                """
                SELECT table_schema, table_name
                FROM information_schema.tables
                """
             ).execute(),
        c -> c.close()
    )
    .flatMap(it -> it.map((r, m) ->
         new Table(r.get(0, String.class), r.get(1, String.class))
    ))
    .doOnNext(System.out::println)
    .subscribe();
```

## 샘플 출력

두 방법 모두 다음과 같은 출력을 생성합니다:

```
Table[schema=INFORMATION_SCHEMA, table=TABLE_PRIVILEGES]
Table[schema=INFORMATION_SCHEMA, table=REFERENTIAL_CONSTRAINTS]
Table[schema=INFORMATION_SCHEMA, table=TABLE_TYPES]
Table[schema=INFORMATION_SCHEMA, table=QUERY_STATISTICS]
Table[schema=INFORMATION_SCHEMA, table=TABLES]
Table[schema=INFORMATION_SCHEMA, table=SESSION_STATE]
Table[schema=INFORMATION_SCHEMA, table=HELP]
Table[schema=INFORMATION_SCHEMA, table=COLUMN_PRIVILEGES]
Table[schema=INFORMATION_SCHEMA, table=SYNONYMS]
Table[schema=INFORMATION_SCHEMA, table=SESSIONS]
Table[schema=INFORMATION_SCHEMA, table=IN_DOUBT]
Table[schema=INFORMATION_SCHEMA, table=USERS]
Table[schema=INFORMATION_SCHEMA, table=COLLATIONS]
Table[schema=INFORMATION_SCHEMA, table=SCHEMATA]
Table[schema=INFORMATION_SCHEMA, table=TABLE_CONSTRAINTS]
Table[schema=INFORMATION_SCHEMA, table=INDEXES]
Table[schema=INFORMATION_SCHEMA, table=ROLES]
Table[schema=INFORMATION_SCHEMA, table=FUNCTION_COLUMNS]
Table[schema=INFORMATION_SCHEMA, table=CONSTANTS]
Table[schema=INFORMATION_SCHEMA, table=SEQUENCES]
Table[schema=INFORMATION_SCHEMA, table=RIGHTS]
Table[schema=INFORMATION_SCHEMA, table=FUNCTION_ALIASES]
Table[schema=INFORMATION_SCHEMA, table=CATALOGS]
Table[schema=INFORMATION_SCHEMA, table=CROSS_REFERENCES]
Table[schema=INFORMATION_SCHEMA, table=SETTINGS]
Table[schema=INFORMATION_SCHEMA, table=DOMAINS]
Table[schema=INFORMATION_SCHEMA, table=KEY_COLUMN_USAGE]
Table[schema=INFORMATION_SCHEMA, table=LOCKS]
Table[schema=INFORMATION_SCHEMA, table=COLUMNS]
Table[schema=INFORMATION_SCHEMA, table=TRIGGERS]
Table[schema=INFORMATION_SCHEMA, table=VIEWS]
Table[schema=INFORMATION_SCHEMA, table=TYPE_INFO]
Table[schema=INFORMATION_SCHEMA, table=CONSTRAINTS]
```

## JDBC 대안

R2DBC가 아닌 JDBC를 사용하는 경우에도, 위와 동일한 방식으로 리액티브 스트림 라이브러리와 함께 jOOQ API를 블로킹 방식으로 계속 사용할 수 있습니다. 예를 들어, 선호하는 RDBMS가 아직 리액티브 R2DBC 드라이버를 지원하지 않는 경우에 유용합니다.

## 현재 지원되는 R2DBC 드라이버

r2dbc.io에 따르면 현재 지원되는 드라이버는 다음과 같습니다:

- oracle-r2dbc
- r2dbc-h2
- r2dbc-postgresql
- r2dbc-mysql
- r2dbc-mssql
- mariadb-connector-r2dbc

이 모든 드라이버를 jOOQ 3.15 이상에서 통합 테스트하고 있습니다.

## 실행 가능한 예제

여기에서 예제를 실행해 볼 수 있습니다: https://github.com/jOOQ/jOOQ/tree/main/jOOQ-examples/jOOQ-r2dbc-example

다음 스키마를 사용합니다:

```sql
CREATE TABLE r2dbc_example.author (
  id INT NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,

  CONSTRAINT pk_author PRIMARY KEY (id)
);

CREATE TABLE r2dbc_example.book (
  id INT NOT NULL AUTO_INCREMENT,
  author_id INT NOT NULL,
  title VARCHAR(100) NOT NULL,

  CONSTRAINT pk_book PRIMARY KEY (id),
  CONSTRAINT fk_book_author FOREIGN KEY (id)
    REFERENCES r2dbc_example.author
);
```

그리고 다음 코드를 실행합니다:

```java
Flux.from(ctx
        .insertInto(AUTHOR)
        .columns(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
        .values("John", "Doe")
        .returningResult(AUTHOR.ID))
    .flatMap(id -> ctx
        .insertInto(BOOK)
        .columns(BOOK.AUTHOR_ID, BOOK.TITLE)
        .values(id.value1(), "Fancy Book"))
    .thenMany(ctx
        .select(
             BOOK.author().FIRST_NAME,
             BOOK.author().LAST_NAME,
             BOOK.TITLE)
        .from(BOOK))
    .doOnNext(System.out::println)
    .subscribe();
```

두 개의 레코드를 삽입하고 조인된 결과를 다음과 같이 가져옵니다:

```
+----------+---------+----------+
|FIRST_NAME|LAST_NAME|TITLE     |
+----------+---------+----------+
|John      |Doe      |Fancy Book|
+----------+---------+----------+
```

---

## 댓글 섹션

Azamat의 질문 (2021년 8월 3일): "좋은 소식이네요! 그런데 트랜잭션을 지원하나요?"

lukaseder의 답변: "지원할 예정이지만 아직은 아닙니다."

Jooqer의 질문 (2021년 8월 28일): "트랜잭션 지원 일정이 있나요?"

lukaseder의 답변: "준비되는 대로요."

zechuan의 질문 (2021년 10월 22일): "Record API를 지원하나요?"

lukaseder의 답변: "Java 16 레코드를 말씀하시는 건가요? 지원하지 않을 수가 없죠."

Ben의 코멘트 (2022년 1월 5일): Kotlin 코루틴과 reified 타입 매핑을 위한 확장이 있으면 좋겠다고 제안했습니다.

lukaseder의 답변: GitHub 이슈 #9335를 참조하고, reified 타입 매핑에 대한 설명을 요청했습니다.

Xiaofei의 질문 (2022년 4월 23일): "Scala에서 사용하는 예제가 있나요?"

lukaseder의 답변: 블로그에는 예제가 없지만, 리액티브 스트림 라이브러리를 사용한다고 가정하면 Java 예제가 Scala로 1:1 변환됩니다.
