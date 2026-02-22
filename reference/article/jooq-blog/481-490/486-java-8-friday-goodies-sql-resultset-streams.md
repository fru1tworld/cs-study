# Java 8 금요일 선물: SQL ResultSet 스트림

> 원문: https://blog.jooq.org/java-8-friday-goodies-sql-resultset-streams/

Data Geekery에서는 Java 8에 대해 굉장히 기대하고 있으며, 이것이 바로 Data Geekery 팀이 새로운 블로그 시리즈를 시작하는 이유입니다. Java 8 금요일 시리즈입니다. 매주 금요일마다 람다 표현식, 확장 메서드 및 기타 훌륭한 기능들을 활용하는 멋진 새로운 튜토리얼 스타일의 Java 8 기능들을 보여드릴 것입니다. 소스 코드는 GitHub에서 확인할 수 있습니다.

## SQL ResultSet 스트림

지난 주에 우리는 람다 표현식에서 체크 예외를 던져야 할 때 어떻게 해야 하는지 보여드렸습니다. 이것은 Java 8의 매우 중요한 문제입니다. Java 8과 체크 예외는 서로 잘 맞지 않기 때문입니다.

이 문제는 Stack Overflow에서 이미 여러 번 다뤄졌습니다.

다음과 같은 것을 쓸 수 없습니다:

```java
// 컴파일 오류가 발생하는 코드
Arrays.stream(dir.listFiles()).forEach(file -> {
    System.out.println(file.getCanonicalPath());
});
```

대신 다음과 같이 작성해야 합니다:

```java
// 장황한 try-catch로 감싼 코드
Arrays.stream(dir.listFiles()).forEach(file -> {
    try {
        System.out.println(file.getCanonicalPath());
    }
    catch (IOException e) {
        throw new RuntimeException(e);
    }
});
```

Java 8 설계자들은 여기서 잘못된 결정을 내렸다고 생각합니다. 물론 이런 트레이드오프에는 항상 단점이 있지만, 자세한 내용은 이전 글을 참조하시기 바랍니다.

## jOOλ - 체크 예외 처리하기

우리는 이 문제를 해결하기 위해 jOOλ(jOO-Lambda)를 만들었습니다. jOOλ는 Apache Software License 2.0 하에 배포됩니다. jOOλ는 `java.util.function` 패키지의 모든 FunctionalInterface 타입을 복제하여 체크 예외를 던질 수 있도록 합니다.

이제 다음과 같이 작성할 수 있습니다:

```java
Arrays.stream(dir.listFiles()).forEach(
    Unchecked.consumer(file -> {
        System.out.println(file.getCanonicalPath());
    })
);
```

또는 예외 핸들러를 사용하려면:

```java
Arrays.stream(dir.listFiles())
      .forEach(Unchecked.consumer(
          file -> {
              System.out.println(file.getCanonicalPath());
          },
          e -> {
              log.error("파일을 처리하는 중 오류 발생", e);
              throw new RuntimeException(e);
          }
      ));
```

## JDBC와 스트림

jOOλ를 사용하면 JDBC ResultSet을 Java 8 스트림으로 변환할 수 있습니다. 전통적으로 ResultSet을 반복할 때는 다음과 같이 해야 했습니다:

```java
try (PreparedStatement stmt = connection.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {

    while (rs.next()) {
        // rs에서 데이터를 가져와서 처리
    }
}
```

하지만 스트림을 사용하면 훨씬 더 함수형 스타일로 작성할 수 있습니다:

```java
Class.forName("org.h2.Driver");
try (Connection c = DriverManager.getConnection(
        "jdbc:h2:~/sql-goodies-with-jooq", "sa", "")) {

    String sql = "SELECT SCHEMA_NAME, IS_DEFAULT " +
                 "FROM INFORMATION_SCHEMA.SCHEMATA " +
                 "ORDER BY SCHEMA_NAME";

    try (PreparedStatement stmt = c.prepareStatement(sql)) {
        SQL.stream(stmt, Unchecked.function(rs ->
            new Schema(
                rs.getString("SCHEMA_NAME"),
                rs.getBoolean("IS_DEFAULT")
            )
        ))
        .forEach(System.out::println);
    }
}
```

여기서 `Schema`는 간단한 POJO입니다:

```java
static class Schema {
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

## SQL.stream() 메서드

`SQL.stream()` 메서드는 여러 오버로드 버전을 제공합니다:

```java
// 기본 버전
public static <T> Stream<T> stream(
    PreparedStatement stmt,
    Function<ResultSet, T> rowFunction
)

// 예외 변환기를 포함한 버전
public static <T> Stream<T> stream(
    PreparedStatement stmt,
    Function<ResultSet, T> rowFunction,
    Function<SQLException, RuntimeException> exceptionTranslator
)
```

## 내부 구현

내부적으로 스트림을 생성하기 위해 커스텀 `ResultSetIterator`를 만들고 이를 `StreamSupport`로 감싸야 합니다:

```java
StreamSupport.stream(
    Spliterators.spliteratorUnknownSize(
        new ResultSetIterator<>(
            supplier,
            rowFunction,
            exceptionTranslator
        ), 0
    ), false
);
```

`ResultSetIterator`는 다음과 같이 구현됩니다:

```java
class ResultSetIterator<T> implements Iterator<T> {
    private final Supplier<? extends ResultSet> supplier;
    private final Function<ResultSet, T> rowFunction;
    private final Function<SQLException, RuntimeException> exceptionTranslator;

    private ResultSet rs;
    private boolean hasNext;
    private boolean moved;

    ResultSetIterator(
        Supplier<? extends ResultSet> supplier,
        Function<ResultSet, T> rowFunction,
        Function<SQLException, RuntimeException> exceptionTranslator
    ) {
        this.supplier = supplier;
        this.rowFunction = rowFunction;
        this.exceptionTranslator = exceptionTranslator;
    }

    private ResultSet rs() {
        // 지연 초기화
        if (rs == null) {
            rs = supplier.get();
        }
        return rs;
    }

    @Override
    public boolean hasNext() {
        try {
            if (!moved) {
                hasNext = rs().next();
                moved = true;
            }
            return hasNext;
        }
        catch (SQLException e) {
            throw exceptionTranslator.apply(e);
        }
    }

    @Override
    public T next() {
        try {
            if (!moved) {
                rs().next();
            }
            moved = false;
            return rowFunction.apply(rs());
        }
        catch (SQLException e) {
            throw exceptionTranslator.apply(e);
        }
    }
}
```

## 이상적인 세계에서

이상적인 세계에서는 이렇게 복잡한 보일러플레이트 코드 없이 더 간단한 API를 사용할 수 있을 것입니다. Java 8의 Stream API에 다음과 같은 메서드가 있었다면 좋았을 것입니다:

```java
// 가상의 API - 실제로 존재하지 않음
public static<T> Stream<T> generate(
    Supplier<T> supplier,
    Predicate<T> hasNext
)
```

이렇게 되면 다음과 같이 훨씬 간단하게 작성할 수 있었을 것입니다:

```java
// 가상의 코드 - 실제로 작동하지 않음
Stream.generate(() -> rs.next() ? map(rs) : null, Objects::nonNull)
```

하지만 안타깝게도 현재 Java 8의 Stream API는 이러한 유한 스트림 생성을 위한 편리한 메서드를 제공하지 않습니다.

## 결론

JDBC ResultSet은 본질적으로 Java 8 스트림이어야 합니다. 불행히도 Java 8의 Stream API는 병렬 컴퓨팅에 최적화되어 있어서, 데이터베이스와의 통합에 대한 고려가 부족합니다.

jOOλ와 같은 라이브러리는 이러한 간극을 메우기 위한 실용적인 해결책을 제공합니다. 물론 Guava나 다른 라이브러리들도 비슷한 솔루션을 개발하고 있을 것입니다.

체크 예외와 람다 표현식의 불편한 조합은 Java 8의 주요 아쉬운 점 중 하나입니다. jOOλ의 `Unchecked` 유틸리티를 사용하면 이 문제를 우아하게 해결할 수 있으며, JDBC와 같은 레거시 API를 함수형 스타일로 사용할 수 있게 됩니다.

## 추가 논의: ResultSet이 정말 스트림이 될 수 있는가?

한 가지 고려해야 할 점이 있습니다. ResultSet은 본질적으로 상태를 가진 커서입니다. 현재 행의 위치를 추적하고, 데이터를 수정할 수도 있습니다. 이는 함수형 프로그래밍의 불변성 원칙과 충돌합니다.

하지만 데이터베이스 트랜잭션의 관점에서 보면, 트랜잭션 내의 데이터는 일관된 스냅샷을 제공합니다. 이런 의미에서 ResultSet을 읽기 전용 스트림으로 취급하는 것은 합리적입니다. 병렬 스트림 연산은 문제가 될 수 있지만, 순차적 스트림 처리에서는 충분히 유용합니다.

jOOQ를 사용하면 이러한 복잡성을 추상화하고, 타입 안전한 SQL과 함께 Java 8 스트림의 강력함을 활용할 수 있습니다.
