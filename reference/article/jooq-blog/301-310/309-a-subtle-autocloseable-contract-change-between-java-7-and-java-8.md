# Java 7과 Java 8 사이의 미묘한 AutoCloseable 계약 변경

> 원문: https://blog.jooq.org/a-subtle-autocloseable-contract-change-between-java-7-and-java-8/

Java 7에서 도입된 가장 유용한 기능 중 하나는 try-with-resources 문과 함께 사용되는 `AutoCloseable` 인터페이스입니다. 이 기능 덕분에 Eclipse와 같은 IDE에서 리소스 누수 감지 기능이 크게 개선되었습니다.

## JDBC의 초기 문제점

JDBC 리소스가 제대로 관리되지 않을 때 Eclipse가 리소스 누수 경고를 표시하는 것을 볼 수 있습니다:

```java
public static void main(String[] args)
throws Exception {
    Connection c = DriverManager.getConnection(
         "jdbc:h2:~/test", "sa", "");
    Statement s = c.createStatement();
    ResultSet r = s.executeQuery("SELECT 1 + 1");
    r.next();
    System.out.println(r.getInt(1));
}
```

출력: `2`

위 코드는 정상적으로 동작하지만, `Connection`, `Statement`, `ResultSet` 모두 닫히지 않았기 때문에 리소스 누수가 발생합니다.

## Try-With-Resources를 사용한 올바른 해결책

```java
public static void main(String[] args)
throws Exception {
    try (Connection c = DriverManager.getConnection(
             "jdbc:h2:~/test", "sa", "");
         Statement s = c.createStatement();
         ResultSet r = s.executeQuery("SELECT 1 + 1")) {

        r.next();
        System.out.println(r.getInt(1));
    }
}
```

이렇게 하면 모든 리소스가 try 블록이 끝날 때 자동으로 닫힙니다.

## Java 7과 Java 8의 AutoCloseable 계약 차이

여기서 흥미로운 부분이 나옵니다. Java 7과 Java 8 사이에 `AutoCloseable`의 계약이 미묘하게 변경되었습니다.

Java 7 정의:

> "더 이상 필요하지 않을 때 반드시 닫아야 하는 리소스."

Java 7에서는 "must"(반드시)라는 의무적인 언어를 사용했습니다. 즉, `AutoCloseable`을 구현하는 모든 것은 반드시 닫아야 한다는 것을 의미했습니다.

Java 8 정의:

> "닫힐 때까지 리소스(파일 또는 소켓 핸들 등)를 보유할 수 있는 객체... API 참고: 기본 클래스가 AutoCloseable을 구현하더라도 모든 하위 클래스나 인스턴스가 해제 가능한 리소스를 보유하는 것은 아닐 수 있으며, 실제로 이것은 일반적인 경우입니다."

Java 8에서는 이 명세가 상당히 완화되었습니다. `AutoCloseable`을 구현한다고 해서 반드시 리소스가 존재한다는 것을 보장하지 않습니다. 이는 단지 리소스가 *존재할 수도 있다*는 힌트일 뿐입니다.

## 실제 사례: jOOQ

jOOQ 라이브러리는 이 문제를 잘 보여줍니다. `Query` 객체는 `AutoCloseable`을 구현하지만, 일반적인 사용에서는 명시적으로 닫을 필요가 없습니다:

```java
try (Connection c = DriverManager.getConnection(
        "jdbc:h2:~/test", "sa", "")) {

    // 여기서는 새로운 리소스가 생성되지 않음:
    ResultQuery<Record> query =
        DSL.using(c).resultQuery("SELECT 1 + 1");

    // 리소스가 생성되고 즉시 닫힘
    System.out.println(query.fetch());
}
```

출력:
```
+----+
|   2|
+----+
|   2|
+----+
```

위 코드에서 `query` 객체는 `AutoCloseable`이지만, jOOQ의 기본 즉시 실행 패턴을 사용할 때는 리소스가 내부적으로 관리됩니다. `fetch()` 메서드가 호출되면 내부적으로 `PreparedStatement`와 `ResultSet`이 생성되고, 데이터를 가져온 후 즉시 닫힙니다. 따라서 query 자체를 try-with-resources로 감쌀 필요가 없습니다.

그러나 Eclipse는 이것이 올바른 코드임에도 불구하고 경고를 표시합니다.

## jOOQ에서 리소스가 필요한 경우

반면에, 특정 작업은 실제로 적절한 종료가 필요한 리소스를 생성합니다:

```java
try (Connection c = DriverManager.getConnection(
        "jdbc:h2:~/test", "sa", "");

     // ResultQuery에서 statement를 열린 상태로 "유지"함
     ResultQuery<Record> query =
         DSL.using(c)
            .resultQuery("SELECT 1 + 1")
            .keepStatement(true)) {

    // Cursor에서 ResultSet을 열린 상태로 유지
    try (Cursor<Record> cursor = query.fetchLazy()) {
        System.out.println(cursor.fetchOne());
    }
}
```

`keepStatement(true)`를 사용하거나 `fetchLazy()`를 호출할 때는 리소스가 열린 상태로 유지되므로 명시적으로 닫아야 합니다.

## Stream API의 유사한 사례

Java 8의 `Stream` API도 동일한 문제를 보여줍니다:

```java
Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
stream.forEach(System.out::println);
```

대부분의 스트림(인메모리 컬렉션에서 생성된 스트림)은 닫을 필요가 없지만, 모든 스트림이 `AutoCloseable`을 구현합니다. 이는 `Files.list()`와 같은 일부 스트림은 실제로 파일 핸들을 보유하고 있어 닫아야 하기 때문입니다.

```java
// 이 스트림은 닫아야 함
try (Stream<Path> paths = Files.list(Paths.get("."))) {
    paths.forEach(System.out::println);
}

// 이 스트림은 닫을 필요 없음
Arrays.asList(1, 2, 3).stream().forEach(System.out::println);
```

## IDE의 한계

Eclipse와 같은 정적 분석 도구는 서드파티 API에서 "선택적으로 닫을 수 있는" 리소스와 "반드시 닫아야 하는" 리소스를 구분하는 데 어려움을 겪습니다. IDE는 `AutoCloseable`을 구현한 객체가 닫히지 않으면 경고를 표시하지만, Java 8의 완화된 계약에 따르면 이것이 항상 문제가 되는 것은 아닙니다.

## 권장 사항

개별적으로 경고를 억제하는 것보다, 계약이 불명확한 API에 대해서는 IDE 설정에서 리소스 누수 감지를 비활성화하는 것이 좋습니다. jOOQ나 Stream API처럼 `AutoCloseable`을 선택적으로 구현하는 API를 사용할 때는 완벽한 감지가 불가능하다는 점을 인정해야 합니다.

## 결론

Java 8에서 `AutoCloseable` 계약이 완화된 것은 실용적인 결정이었습니다. 이를 통해 기본 클래스나 인터페이스가 `AutoCloseable`을 구현하면서도 모든 하위 클래스가 실제 리소스를 관리할 필요는 없게 되었습니다. 그러나 이로 인해 IDE의 리소스 누수 감지 기능이 덜 정확해졌고, 개발자는 어떤 리소스가 실제로 닫아야 하는지 API 문서를 주의 깊게 읽어야 합니다.

이 변경은 미묘하지만 중요합니다. Java 7의 엄격한 "반드시 닫아야 함" 계약에서 Java 8의 "닫아야 할 수도 있음" 계약으로의 전환은 API 설계자에게 더 많은 유연성을 제공하지만, 도구와 개발자에게는 더 많은 주의를 요구합니다.
