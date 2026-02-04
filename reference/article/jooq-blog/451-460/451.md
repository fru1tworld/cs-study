# Java 8 금요일: Nashorn과 jOOQ로 JavaScript가 SQL을 만나다

> 원문: https://blog.jooq.org/java-8-friday-javascript-goes-sql-with-nashorn-and-jooq/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 우리는 jOOQ의 유창한(fluent) API와 쿼리 DSL에 대해 정말 열광하고 있습니다. 왜냐하면 우리는 Java 8이 우리 생태계에 가져다 줄 모든 것에 대해 흥분하고 있기 때문입니다.

## Java 8 금요일

매주 금요일마다 우리는 Java 8의 새로운 튜토리얼 스타일의 블로그 게시물을 보여드립니다. 람다 표현식, 확장 메서드, 그리고 기타 훌륭한 것들을 다룹니다. 소스 코드는 GitHub에서 찾아볼 수 있습니다.

## JavaScript에서 SQL 스크립팅하기

오늘 우리가 Nashorn과 Java 8로 할 수 있는 것을 탐구해보고 싶습니다: 서버 사이드 SQL 스크립팅입니다. JDBC를 수동으로 관리하면서 고통스러운 리소스 처리와 SQL 문자열 조합을 하는 대신, 우리는 jOOQ를 사용할 수 있습니다 - 그리고 이것은 매끄럽게 작동합니다.

### JDBC 기초 설정

먼저 JDBC를 사용한 기본 설정부터 살펴보겠습니다:

```javascript
var someDatabaseFun = function() {
    var Properties = Java.type("java.util.Properties");
    var Driver = Java.type("org.h2.Driver");

    var driver = new Driver();
    var properties = new Properties();

    properties.setProperty("user", "sa");
    properties.setProperty("password", "");

    try {
        var conn = driver.connect(
            "jdbc:h2:~/test", properties);

        // 여기에 데이터베이스 코드 작성
    }
    finally {
        try {
            if (conn) conn.close();
        } catch (e) {}
    }
}

someDatabaseFun();
```

### 전통적인 JDBC 방식

전통적인 JDBC로 쿼리를 실행하려면 상당한 양의 리소스 관리 보일러플레이트 코드가 필요합니다:

```javascript
try {
    var stmt = conn.prepareStatement(
        "select table_schema, table_name " +
        "from information_schema.tables");
    var rs = stmt.executeQuery();

    while (rs.next()) {
        print(rs.getString("TABLE_SCHEMA") + "."
            + rs.getString("TABLE_NAME"))
    }
}
finally {
    if (rs)
        try {
            rs.close();
        }
        catch(e) {}

    if (stmt)
        try {
            stmt.close();
        }
        catch(e) {}
}
```

위 코드는 다음과 같은 결과를 출력합니다:

```
INFORMATION_SCHEMA.FUNCTION_COLUMNS
INFORMATION_SCHEMA.CONSTANTS
INFORMATION_SCHEMA.SEQUENCES
INFORMATION_SCHEMA.RIGHTS
INFORMATION_SCHEMA.TRIGGERS
INFORMATION_SCHEMA.CATALOGS
INFORMATION_SCHEMA.CROSS_REFERENCES
INFORMATION_SCHEMA.SETTINGS
INFORMATION_SCHEMA.FUNCTION_ALIASES
INFORMATION_SCHEMA.VIEWS
INFORMATION_SCHEMA.TYPE_INFO
INFORMATION_SCHEMA.CONSTRAINTS
...
```

보시다시피, 단순한 쿼리를 실행하는 것조차 ResultSet과 Statement를 닫는 것에 대한 상당한 보일러플레이트가 필요합니다.

### jOOQ 평문 SQL 방식

jOOQ를 사용하면 복잡성이 극적으로 줄어듭니다:

```javascript
var DSL = Java.type("org.jooq.impl.DSL");

print(
    DSL.using(conn)
       .fetch("select table_schema, table_name " +
              "from information_schema.tables")
);
```

이것이 전부입니다! jOOQ가 모든 리소스 관리를 자동으로 처리해주고, 결과는 깔끔하게 포맷된 테이블로 출력됩니다:

```
+------------------+--------------------+
|TABLE_SCHEMA      |TABLE_NAME          |
+------------------+--------------------+
|INFORMATION_SCHEMA|FUNCTION_COLUMNS    |
|INFORMATION_SCHEMA|CONSTANTS           |
|INFORMATION_SCHEMA|SEQUENCES           |
|INFORMATION_SCHEMA|RIGHTS              |
|INFORMATION_SCHEMA|TRIGGERS            |
|INFORMATION_SCHEMA|CATALOGS            |
|INFORMATION_SCHEMA|CROSS_REFERENCES    |
|INFORMATION_SCHEMA|SETTINGS            |
|INFORMATION_SCHEMA|FUNCTION_ALIASES    |
|INFORMATION_SCHEMA|VIEWS               |
|INFORMATION_SCHEMA|TYPE_INFO           |
|INFORMATION_SCHEMA|CONSTRAINTS         |
...
+------------------+--------------------+
```

JDBC의 장황한 코드와 비교하면 훨씬 적은 보일러플레이트로 동일한 결과를 얻을 수 있습니다.

### jOOQ DSL의 진정한 강점

하지만 jOOQ의 진정한 강점은 DSL API에 있습니다. DSL은 벤더별 SQL 세부 사항을 추상화하고 유창한 쿼리 구성을 가능하게 합니다. 생성된 클래스들을 사용하면 타입 안전한 SQL을 작성할 수 있습니다:

```javascript
var DSL = Java.type("org.jooq.impl.DSL");
var Tables = Java.type(
    "org.jooq.db.h2.information_schema.Tables");
var t = Tables.TABLES;
var c = Tables.COLUMNS;

var count = DSL.count;
var row = DSL.row;

print(
    DSL.using(conn)
       .select(
           t.TABLE_SCHEMA,
           t.TABLE_NAME,
           c.COLUMN_NAME)
       .from(t)
       .join(c)
       .on(row(t.TABLE_SCHEMA, t.TABLE_NAME)
           .eq(c.TABLE_SCHEMA, c.TABLE_NAME))
       .orderBy(
           t.TABLE_SCHEMA.asc(),
           t.TABLE_NAME.asc(),
           c.ORDINAL_POSITION.asc())
       .fetch()
);
```

이 코드는 타입 안전하고, 자동 완성이 가능하며, 리팩토링에 강합니다. 테이블 구조가 변경되면 생성된 코드도 함께 변경되어 컴파일 타임에 오류를 잡아낼 수 있습니다.

### Java 8 Streams와의 통합

가장 강력한 시연은 jOOQ를 Java 8 Streams API와 결합하는 것입니다. JavaScript 내에서 결과를 함수형으로 처리할 수 있습니다:

```javascript
DSL.using(conn)
   .select(
       t.TABLE_SCHEMA,
       t.TABLE_NAME,
       count().as("CNT"))
   .from(t)
   .join(c)
   .on(row(t.TABLE_SCHEMA, t.TABLE_NAME)
       .eq(c.TABLE_SCHEMA, c.TABLE_NAME))
   .groupBy(t.TABLE_SCHEMA, t.TABLE_NAME)
   .orderBy(
       t.TABLE_SCHEMA.asc(),
       t.TABLE_NAME.asc())
   .fetchMaps()
   .stream()
   .forEach(function (r) {
       print(r.TABLE_SCHEMA + '.'
           + r.TABLE_NAME + ' has '
           + r.CNT + ' columns.');
   });
```

이 코드는 다음과 같은 읽기 쉬운 출력을 생성합니다:

```
INFORMATION_SCHEMA.CATALOGS has 1 columns.
INFORMATION_SCHEMA.COLLATIONS has 2 columns.
INFORMATION_SCHEMA.COLUMNS has 23 columns.
INFORMATION_SCHEMA.COLUMN_PRIVILEGES has 8 columns.
INFORMATION_SCHEMA.CONSTANTS has 7 columns.
INFORMATION_SCHEMA.CONSTRAINTS has 13 columns.
INFORMATION_SCHEMA.CROSS_REFERENCES has 14 columns.
...
```

### 배열 지원

배열을 지원하는 데이터베이스의 경우, 배열 컬럼에 인덱스로 접근할 수 있습니다. 예를 들어 `r.COLUMN_NAME[3]`과 같이 사용할 수 있습니다.

## 결론

서버 사이드 JavaScript 개발자라면 jOOQ를 다운로드하고 JavaScript로 SQL을 작성하기 시작하세요. Nashorn과 jOOQ의 조합은 JDBC의 복잡성에서 벗어나면서 데이터베이스에 구애받지 않는 SQL 구성 능력을 제공합니다. 이를 통해 JavaScript 기반 데이터베이스 작업이 더 유지보수하기 쉽고 표현력이 풍부해집니다.

다음 주에도 Java 8에 대한 더 많은 훌륭한 콘텐츠로 찾아뵙겠습니다!

## 독자 코멘트 참고

일부 독자들이 MySQL에서 jOOQ의 `RenderNameStyle` 설정이 FROM 절의 테이블 이름에 제대로 적용되지 않아 식별자 주위에 원치 않는 따옴표 문자가 생기는 SQL 오류를 보고했습니다. 이는 Nashorn 엔진의 버그와 관련된 것으로 확인되었으며, Settings 적용 및 SQLDialect 지정에 대한 후속 논의가 있었습니다.
