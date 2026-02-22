> 원문: https://blog.jooq.org/how-to-prevent-jdbc-resource-leaks-with-jdbc-and-with-jooq/

# JDBC와 jOOQ에서 JDBC 리소스 누수를 방지하는 방법

## 개요

이 글에서는 피크 부하 시 커넥션 풀(Connection Pool) 고갈이 발생한 실제 프로덕션 이슈를 살펴봅니다. 이 문제는 레거시 Java 코드의 JDBC 리소스 누수로 인해 발생했습니다.

## 문제 상황

데이터베이스 부하가 정상임에도 불구하고, 데이터베이스 작업을 시도하는 모든 Java 프로세스가 대기열에 쌓이는 현상이 발생했습니다. 조사 결과, 개발자들이 JDBC 커넥션, 스테이트먼트(Statement), 결과 집합(ResultSet)을 제대로 닫지 않는 것이 원인이었습니다.

일반적인 누수 패턴은 다음과 같습니다:

- 커넥션 닫기 누락: `PreparedStatement`는 닫았지만 기본 `Connection`은 해제하지 않는 경우
- 유틸리티 메서드에서의 정리 누락: 커넥션을 전달받은 헬퍼 함수가 닫기에 대한 책임을 지지 않는 경우
- 부적절한 복사-붙여넣기 코드: 완전한 리소스 관리 없이 패턴을 잘못 복제하는 경우

심지어 Oracle의 공식 JDBC 튜토리얼에도 리소스 누수가 있는 예제가 포함되어 있습니다.

## 해결책 #1: Try-With-Resources

권장되는 접근 방식은 Java의 try-with-resources 문을 사용하는 것입니다:

```java
try (Connection con = DriverManager.getConnection(
        "jdbc:myDriver:myDatabase", username, password);
     Statement stmt = con.createStatement();
     ResultSet rs = stmt.executeQuery("SELECT a, b, c FROM Table1")) {

    while (rs.next()) {
        // 결과 처리
    }
}
```

이렇게 하면 스코프를 벗어날 때 모든 리소스가 자동으로 닫힙니다.

## 해결책 #2: jOOQ 사용

jOOQ는 지연/즉시(lazy/eager) 리소스 패러다임을 역전시킵니다. 주요 차이점은 다음과 같습니다:

- 자동 커넥션 관리: `DataSource`를 사용할 때 jOOQ가 자동으로 커넥션을 획득하고 해제합니다
- 스테이트먼트 노출 없음: 스테이트먼트가 내부적으로 생성되고 소멸됩니다
- 즉시 결과 가져오기(Eager Fetching): 결과는 기본적으로 메모리로 즉시 가져옵니다

jOOQ 사용 예제:

DataSource 사용 시 (자동 관리):
```java
for (Record record : DSL
       .using(ds, SQLDialect.ORACLE)
       .fetch("SELECT a, b, c FROM Table1")) {
    // 레코드 처리
}
```

지연 스트리밍 사용 시 (수동 리소스 관리 필요):
```java
try (Cursor<Record> cursor : DSL
        .using(ds, SQLDialect.ORACLE)
        .fetchLazy("SELECT a, b, c FROM Table1")) {
    for (Record record : cursor) {
        // 레코드 처리
    }
}
```

## 핵심 원칙

저자가 강조하는 핵심 원칙: *"리소스를 획득한 스코프가 리소스를 닫는다."* 이 원칙은 명확한 소유권과 책임을 유지함으로써 리소스 누수를 방지합니다.

## 추가 도구

HikariCP와 같은 커넥션 풀 라이브러리는 `leakDetectionThreshold` 설정을 통해 누수를 감지할 수 있으며, 닫히지 않은 커넥션의 스택 트레이스(Stack Trace)를 로깅합니다.
