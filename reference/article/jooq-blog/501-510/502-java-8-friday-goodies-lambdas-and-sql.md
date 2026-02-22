# Java 8 금요일 선물: 람다와 SQL

> 원문: https://blog.jooq.org/java-8-friday-goodies-lambdas-and-sql/

Data Geekery에서 우리는 Java와 SQL을 사랑합니다. 그래서 우리는 업계에서 가장 뛰어난 Java와 SQL 통합을 제공하는 jOOQ에 대한 작업을 매우 기쁘게 생각합니다.

Java 8은 이제 몇 주 앞으로 다가왔고, 이것은 인터넷의 모든 곳에서 많은 블로그 포스트가 쏟아지고 있는 매우 흥미로운 주제입니다. 우리는 이 미니 시리즈를 통해 "Java 8 금요일"이라고 불리는 매주 금요일마다 새로운 Java 8 기능을 보여주는 짧은 튜토리얼을 제공할 것입니다. 재미를 위해서입니다.

## Java 8 선물: 람다와 SQL

네, 우리의 첫 번째 포스트에서 람다를 다루겠습니다. 람다, 람다, 람다. 이것은 Java 8에 대해 이야기할 때 모두가 말하고 싶어하는 주제입니다. 그러나 람다 자체는 상당히 지루합니다. 새로운 문법을 배워야 하는데, 새로운 스트림 API는 정말 훌륭합니다. 그리고 이전에 Java에서 불가능했던 다른 모든 종류의 것들도요.

하지만 SQL과 함께 람다의 멋진 점들을 살펴보겠습니다.

### SQL과 람다

우리가 항상 부러워했던 것은 Groovy가 오랫동안 가지고 있던 SQL 통합입니다. 다음을 확인해 보세요:

```groovy
sql.eachRow('select * from information_schema.schemata') {
    println "$it.SCHEMA_NAME -- $it.IS_DEFAULT"
}
```

이것은 Groovy의 문자열 보간(String interpolation)을 활용합니다. 꽤 멋지지 않나요?

Java 8에서도 비슷한 것을 할 수 있을까요? jOOQ, Spring의 JdbcTemplate, 그리고 Apache DbUtils, 세 가지 인기 있는 SQL 라이브러리를 사용하여 시도해 보겠습니다.

### 예제 데이터

모든 세 가지 솔루션에서 재사용할 수 있는 몇 가지 상용구 코드를 설정해 보겠습니다.

H2의 `INFORMATION_SCHEMA`에서 `SCHEMATA` 테이블의 객체를 래핑하기 위한 간단한 POJO:

```java
class Schema {
    final String schemaName;
    final boolean isDefault;

    Schema(String schemaName, boolean isDefault) {
        this.schemaName = schemaName;
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "Schema{" +
               "schemaName='" + schemaName + '\'' +
               ", isDefault=" + isDefault +
               '}';
    }
}
```

... 그리고 모든 예제에서 재사용되는 객체들:

```java
Class.forName("org.h2.Driver");
try (Connection c = DriverManager.getConnection("jdbc:h2:~/test", "sa", "")) {
    String sql = "select schema_name, is_default from information_schema.schemata order by schema_name";

    // 여기에서 작업을 수행
}
```

### jOOQ를 사용한 솔루션

```java
DSL.using(c)
   .fetch(sql)
   .map(r -> new Schema(
       r.getValue("SCHEMA_NAME", String.class),
       r.getValue("IS_DEFAULT", boolean.class)
   ))
   .forEach(System.out::println);
```

### Spring JDBC를 사용한 솔루션

```java
new JdbcTemplate(new SingleConnectionDataSource(c, true))
    .query(sql, (rs, rowNum) ->
        new Schema(rs.getString("SCHEMA_NAME"),
            rs.getBoolean("IS_DEFAULT")))
    .forEach(System.out::println);
```

### Apache DbUtils를 사용한 솔루션

```java
new QueryRunner()
    .query(c, sql, new ArrayListHandler())
    .stream()
    .map(array -> new Schema((String) array[0], (Boolean) array[1]))
    .forEach(System.out::println);
```

### 모든 솔루션 출력

모든 솔루션은 다음과 같은 출력을 생성합니다:

```
Schema{schemaName='INFORMATION_SCHEMA', isDefault=false}
Schema{schemaName='PUBLIC', isDefault=true}
```

### 결론

위의 세 가지 솔루션은 모두 상당히 간결합니다. 다시 말하지만, Java 8은 기존의 모든 API를 개선할 것입니다. API 디자인 측면에서 가장 큰 개선은 단일 추상 메서드(Single Abstract Method, SAM) 인자를 받고 너무 많이 오버로딩하지 않는 메서드에서 볼 수 있습니다. 오버로딩은 람다 표현식 인자와 함께 잘 작동하지 않기 때문입니다. 이러한 람다 사용은 앞으로 모든 API 디자이너가 자신의 공개 API를 설계할 때 정밀하게 분석해야 할 사항입니다.

## 더 많은 Java 8 정보

재사용을 위해, Eugen Paraschiv는 baeldung.com에서 Java 8 리소스 페이지를 생성하고 있습니다. 또한, 다음 주의 Java 8 금요일 튜토리얼을 기대해 주세요. 우리는 `java.util.Map` API의 개선 사항을 살펴볼 것입니다.
