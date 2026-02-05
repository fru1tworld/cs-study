# jOOQ로 java.sql.ResultSet 변환하기

> 원문: https://blog.jooq.org/use-jooq-to-transform-java-sql-resultset/

*2011년 10월 22일 lukaseder 작성*

jOOQ는 기존의 `java.sql.ResultSet` 객체를 다루기 위한 유틸리티 기능을 제공합니다. 쿼리 실행에 jOOQ의 쿼리 빌더 대신 JDBC를 직접 사용하는 것을 선호하더라도 이 기능을 활용할 수 있습니다.

## 기본 사용 패턴

ResultSet을 수동으로 반복 처리하는 대신, jOOQ의 API를 활용할 수 있습니다:

```java
PreparedStatement stmt = connection.prepareStatement(sql);
ResultSet rs = stmt.executeQuery();

Factory factory = new Factory(connection, dialect);
Result<Record> result = factory.fetch(rs);

for (Record record : result) {
  System.out.print(record.getValue("ID"));
  System.out.print(": ");
  System.out.println(record.getValue("NAME"));
}
```

## 내보내기 기능

이 라이브러리는 다양한 내보내기 형식을 지원합니다:

```java
String xml = result.formatXML();
String csv = result.formatCSV();
String json= result.formatJSON();
String text= result.format();
```

## 엔티티 매핑

결과를 JPA 어노테이션이 적용된 클래스로 변환할 수 있습니다:

```java
List<Value> values = result.into(Value.class);

public class Value {
  @Column(name = "ID")
  public Integer id;

  @Column(name = "NAME")
  public String name;
}
```

## 결론

jOOQ를 전략적 SQL 인터페이스로 사용하고 싶지 않더라도, 일반적인 JDBC 사용 시 가끔 유틸리티로 활용할 수 있습니다. jOOQ는 완전한 아키텍처 전환 없이도 편의성을 제공할 수 있을 만큼 유연합니다.
