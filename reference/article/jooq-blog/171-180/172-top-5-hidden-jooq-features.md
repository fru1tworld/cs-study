# jOOQ의 숨겨진 기능 Top 5
> 원문: https://blog.jooq.org/top-5-hidden-jooq-features/

jOOQ의 핵심 가치 제안은 명확합니다: Java에서의 타입 안전한 임베디드 SQL입니다. 이러한 SQL 빌더를 적극적으로 찾는 사람들은 필연적으로 jOOQ를 발견하고 사랑하게 됩니다. 하지만 많은 사람들은 실제로 SQL 빌더가 필요하지 않습니다 - 그럼에도 불구하고 jOOQ는 잘 알려지지 않은 기능들을 통해 다른 상황에서도 매우 유용할 수 있습니다.

## 1. ResultSet 다루기

jOOQ를 사용하지 않고 JDBC(또는 Spring JdbcTemplate 등)를 직접 사용하는 경우에도, 가장 귀찮은 것 중 하나는 `ResultSet`을 다루는 것입니다.

JDBC `ResultSet`은 데이터베이스 커서를 모델링하며, 이는 본질적으로 서버의 컬렉션을 가리키는 포인터로, 예를 들어 `ResultSet.absolute(50)`을 통해 50번째 레코드로 위치를 이동할 수 있습니다(1부터 세기 시작한다는 것을 기억하세요). JDBC `ResultSet`은 지연 데이터 처리에 최적화되어 있어, 서버가 생성한 전체 데이터 세트를 클라이언트에서 구체화할 필요가 없습니다.

그러나 대부분의 경우 우리는 작은 결과만 가져오며, 전체 결과를 메모리에 저장하고 싶습니다. jOOQ의 `Result` 타입으로 이렇게 할 수 있습니다. `DSL.using(connection).fetch(rs)`를 사용하여 JDBC `ResultSet`을 jOOQ `Result`로 변환한 다음, 결과를 TEXT로 포맷하는 것과 같은 모든 멋진 jOOQ 유틸리티에 접근할 수 있습니다:

```java
try (ResultSet rs = stmt.executeQuery()) {
    Result<?> result = DSL.using(connection).fetch(rs);
    System.out.println(result.format());
}
```

이것은 다음과 같은 출력을 생성합니다:

```
+----+---------+-------------+
|  ID|AUTHOR_ID|TITLE        |
+----+---------+-------------+
|   1|        1|1984         |
|   2|        1|Animal Farm  |
+----+---------+-------------+
```

그 반대도 항상 가능합니다. jOOQ `Result`에서 JDBC `ResultSet`이 필요하신가요? `Result.intoResultSet()`을 호출하면 JDBC `ResultSet`에서 작동하는 모든 애플리케이션에 더미 결과를 주입할 수 있습니다.

## 2. 결과를 여러 형식으로 내보내기

jOOQ Result 타입에는 멋진 포맷팅 기능이 있습니다. 단순 텍스트 대신 XML, CSV, JSON, HTML, 그리고 다시 TEXT로도 포맷할 수 있습니다. 포맷은 보통 필요에 맞게 조정할 수 있습니다.

### TEXT 포맷
```java
System.out.println(result.format());
```

출력:
```
+----+---------+-------------+
|  ID|AUTHOR_ID|TITLE        |
+----+---------+-------------+
|   1|        1|1984         |
|   2|        1|Animal Farm  |
+----+---------+-------------+
```

### CSV 포맷
```java
System.out.println(result.formatCSV());
```

출력:
```
ID,AUTHOR_ID,TITLE
1,1,1984
2,1,Animal Farm
```

구분자 문자를 지정하거나 NULL 값을 나타내는 특별한 문자열을 지정할 수도 있습니다:
```java
result.formatCSV(';')                // ";"를 구분자로 사용
result.formatCSV(';', "{null}")      // NULL 값의 표현 지정
```

### JSON 포맷
```java
System.out.println(result.formatJSON());
```

출력:
```json
{"fields":[{"name":"ID","type":"INTEGER"},
           {"name":"AUTHOR_ID","type":"INTEGER"},
           {"name":"TITLE","type":"VARCHAR"}],
 "records":[[1,1,"1984"],[2,1,"Animal Farm"]]}
```

JSON 출력 형식은 `JSONFormat`을 사용하여 커스터마이즈할 수 있습니다:

```java
// 객체 레이아웃 사용
System.out.println(result.formatJSON(
    new JSONFormat()
        .header(false)
        .recordFormat(JSONFormat.RecordFormat.OBJECT)));
```

출력:
```json
[{"ID":1,"AUTHOR_ID":1,"TITLE":"1984"},
 {"ID":2,"AUTHOR_ID":1,"TITLE":"Animal Farm"}]
```

```java
// 배열 레이아웃 사용
System.out.println(result.formatJSON(
    new JSONFormat()
        .header(false)
        .recordFormat(JSONFormat.RecordFormat.ARRAY)));
```

출력:
```json
[[1,1,"1984"],[2,1,"Animal Farm"]]
```

### XML 포맷
```java
System.out.println(result.formatXML());
```

출력:
```xml
<result>
  <fields>
    <field name="ID" type="INTEGER"/>
    <field name="AUTHOR_ID" type="INTEGER"/>
    <field name="TITLE" type="VARCHAR"/>
  </fields>
  <records>
    <record>
      <value field="ID">1</value>
      <value field="AUTHOR_ID">1</value>
      <value field="TITLE">1984</value>
    </record>
    <record>
      <value field="ID">2</value>
      <value field="AUTHOR_ID">1</value>
      <value field="TITLE">Animal Farm</value>
    </record>
  </records>
</result>
```

### HTML 포맷
```java
System.out.println(result.formatHTML());
```

출력:
```html
<table>
<thead>
<tr><th>ID</th><th>AUTHOR_ID</th><th>TITLE</th></tr>
</thead>
<tbody>
<tr><td>1</td><td>1</td><td>1984</td></tr>
<tr><td>2</td><td>1</td><td>Animal Farm</td></tr>
</tbody>
</table>
```

### ASCII 차트
보너스로, `Result`를 ASCII 차트로도 내보낼 수 있습니다. jOOQ 3.10은 결과를 ASCII 영역 차트(영역, 누적, 100% 누적)로 포맷하는 것을 지원합니다:

```java
System.out.println(result.formatChart());
```

## 3. 텍스트 형식 가져오기

내보내기 기능이 있으면 자연스럽게 데이터를 더 사용하기 쉬운 형식으로 다시 가져오는 방법에 대해 생각하게 됩니다. 예를 들어, 통합 테스트를 작성할 때 데이터베이스 쿼리가 다음과 같은 결과를 반환할 것으로 예상할 수 있습니다:

```
ID  AUTHOR_ID  TITLE
--  ---------  -----------
 1  1          1984
 2  1          Animal Farm
```

위의 결과 세트의 텍스트 표현을 `Result.fetchFromTXT(String)`을 사용하여 실제 jOOQ `Result`로 간단히 가져올 수 있으며, jOOQ `Result`(또는 JDBC `ResultSet`!)에서 계속 작업할 수 있습니다.

```java
Result<?> result = ctx.fetchFromTXT(
    "ID  AUTHOR_ID  TITLE       \n" +
    "--  ---------  -----------\n" +
    " 1  1          1984        \n" +
    " 2  1          Animal Farm \n"
);

// JDBC ResultSet으로 변환
ResultSet rs = result.intoResultSet();
```

이러한 타입들은 이제 서비스나 DAO가 jOOQ `Result` 또는 JDBC `ResultSet`을 생성하는 모든 곳에 주입할 수 있습니다.

가장 명백한 적용 분야는 모킹입니다. 두 번째로 명백한 적용 분야는 테스팅입니다. 서비스가 위의 형식의 예상 결과를 생성하는지 쉽게 테스트할 수 있습니다.

대부분의 다른 내보내기 형식(차트 제외)도 가져올 수 있습니다.

## 4. JDBC 모킹

때때로 모킹은 멋집니다. jOOQ의 도구들을 사용하면 jOOQ가 완전한 JDBC 기반 모킹 SPI를 제공하는 것이 자연스럽습니다. 본질적으로 `MockDataProvider`라는 단일 `FunctionalInterface`를 구현할 수 있습니다.

가장 간단하게 만드는 방법은 `Mock.of()` 메서드를 사용하는 것입니다:

```java
MockDataProvider provider = Mock.of(
    DSL.using(SQLDialect.DEFAULT)
       .selectFrom(BOOK)
       .where(BOOK.ID.eq(1))
       .getSQL(),
    DSL.using(SQLDialect.DEFAULT)
       .newResult(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
       .values(1, 1, "1984")
);

// 모킹된 연결 생성
Connection connection = new MockConnection(provider);
DSLContext ctx = DSL.using(connection, SQLDialect.DEFAULT);

// 이제 실제 데이터베이스 없이 쿼리 실행
Result<?> result = ctx.selectFrom(BOOK).where(BOOK.ID.eq(1)).fetch();
```

람다를 사용하여 조금 더 정교하게 만들 수도 있습니다. 어떤 쿼리가 실행되는지에 따라 조건부 로직을 구현할 수 있습니다:

```java
MockDataProvider provider = ctx -> {
    // 쿼리 내용에 따라 다른 결과 반환
    if (ctx.sql().contains("BOOK")) {
        Result<Record3<Integer, Integer, String>> result =
            DSL.using(SQLDialect.DEFAULT)
               .newResult(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE);
        result.add(DSL.using(SQLDialect.DEFAULT)
                      .newRecord(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
                      .values(1, 1, "1984"));
        result.add(DSL.using(SQLDialect.DEFAULT)
                      .newRecord(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
                      .values(2, 1, "Animal Farm"));
        return new MockResult[] { new MockResult(result.size(), result) };
    }

    // 기본 빈 결과
    return new MockResult[] { new MockResult(0, null) };
};

Connection connection = new MockConnection(provider);
```

jOOQ를 Hibernate 기반 애플리케이션을 포함한 모든 JDBC 기반 애플리케이션에서 JDBC 모킹 프레임워크로 사용할 수 있습니다.

중요한 참고사항: 이 jOOQ API로 JDBC 연결을 모킹하는 일반적인 아이디어는 매우 간단한 JDBC 추상화를 사용하여 빠른 해결책, 주입 지점 등을 제공하는 것입니다. 이 모킹 API를 사용하여 전체 데이터베이스(복잡한 상태 전환, 트랜잭션, 락 등 포함)를 에뮬레이션하는 것은 권장되지 않습니다. 이러한 요구 사항이 있다면, `MockDataProvider` 내에서 테스트 데이터베이스를 구현하는 대신 통합 테스트를 위한 실제 데이터베이스(예: testcontainers 사용)를 고려하세요.

## 5. 파싱 연결

우리의 JDBC 기반 애플리케이션을 jOOQ를 사용하도록 업그레이드하는 것은 대안적인 옵션처럼 보일 수 있지만, 오래된 레거시 코드로 인해 너무 많은 노력이 필요할 수 있습니다.

jOOQ는 `DSLContext.parser()`를 통해 완전한 SQL 파서를 노출하며, 더 흥미롭게는 `DSLContext.parsingConnection()`을 통해서도 노출합니다. 파싱 연결은 JDBC `java.sql.Connection`으로, 모든 명령을 jOOQ 파서를 통해 프록시하여 `SQLDialect`와 `Settings`를 포함한 jOOQ의 전체 `Configuration`에 따라 인바운드 SQL 문(및 바인드 변수)을 변환합니다.

이를 통해 JPA를 포함한 모든 JDBC 클라이언트가 생성하는 SQL의 투명한 SQL 변환이 가능합니다.

예를 들어, 이전의 쿼리를 다른 데이터베이스 방언으로 변환할 수 있습니다:

```java
// Oracle 방언으로 설정된 파싱 연결 생성
DSLContext ctx = DSL.using(connection, SQLDialect.ORACLE);
Connection parsingConnection = ctx.parsingConnection();

// MySQL 구문의 SQL이 Oracle 구문으로 변환됨
try (Statement stmt = parsingConnection.createStatement();
     ResultSet rs = stmt.executeQuery(
         "SELECT * FROM book WHERE id = 1 LIMIT 10")) {
    // LIMIT 절이 Oracle의 FETCH FIRST로 변환됨
    while (rs.next()) {
        System.out.println(rs.getString("title"));
    }
}
```

레거시 JPA 애플리케이션에서 가끔 네이티브 쿼리를 사용하거나 Hibernate `@Formula` 또는 Spring Data `@Query` 어노테이션에 벤더 특정 네이티브 SQL이 포함되어 있다면, jOOQ의 파싱 연결과 파싱 데이터 소스를 사용하여 jOOQ 도입을 전면적으로 진행하지 않고도 방언 간에 변환할 수 있습니다.

jOOQ의 주요 목표 중 하나는 SQL 텍스트 기반 마이그레이션 스크립트(예: Flyway로 실행)를 모든 종류의 SQL 방언으로 변환할 수 있도록 하는 것입니다. DDL은 정말로 벤더 독립적으로 유지하기 가장 지루한 SQL 부분이기 때문입니다.

## 결론

jOOQ는 단순한 SQL 빌더 그 이상입니다. 이러한 숨겨진 기능들은 다음과 같은 상황에서 매우 유용합니다:

1. ResultSet 처리: JDBC 코드를 사용하면서도 jOOQ의 편리한 유틸리티 활용
2. 다양한 포맷 내보내기: 디버깅, 로깅, 데이터 교환을 위한 여러 출력 형식 지원
3. 텍스트 포맷 가져오기: 테스트와 모킹을 위한 데이터 주입
4. JDBC 모킹: 실제 데이터베이스 없이 테스트 수행
5. SQL 변환: 레거시 애플리케이션의 점진적 마이그레이션 지원

jOOQ를 완전히 도입하지 않더라도 이러한 기능들을 활용하여 Java 데이터베이스 작업을 더욱 효율적으로 수행할 수 있습니다.
