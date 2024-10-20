# jOOQ로 저장 프로시저를 통합 테스트하는 방법

> 원문: https://blog.jooq.org/how-to-integration-test-stored-procedures-with-jooq/

## 저장 프로시저 테스트 소개

데이터베이스에서 저장 프로시저와 함수를 개발할 때, JUnit을 사용한 Java 단위 테스트와 마찬가지로 해당 프로시저의 정확성을 검증하고 싶을 것입니다. 예를 들어, 간단한 Java 함수는 다음과 같을 수 있습니다:

```java
public static int add(int a, int b) {
    return a + b;
}
```

이에 대응하는 테스트는 다음과 같습니다:

```java
@Test
public void testAdd() {
    assertEquals(3, add(1, 2));
}
```

## 데이터베이스 테스트의 어려움

저장 프로시저를 테스트하는 것은 여러 가지 어려움을 수반합니다:

- 데이터베이스 전용 테스트 라이브러리(예: Oracle의 utPLSQL)는 Maven/Gradle 빌드와 긴밀하게 통합되지 않을 수 있습니다
- JUnit 프레임워크에 비해 IDE 지원이 제한적일 수 있습니다
- 모든 데이터베이스 제품에 테스트 유틸리티가 제공되는 것은 아닙니다
- 여러 데이터베이스를 지원하려면 별도의 테스트 스위트를 유지 관리해야 합니다
- Java 코드와의 통합이 필요하므로 Java 기반 테스트가 요구됩니다

JDBC를 통해 프로시저를 직접 바인딩하는 대신, 기존 Java 테스트 인프라를 재사용하는 것이 개발자에게 이점이 됩니다.

## jOOQ와 Testcontainers 사용하기

Testcontainers는 Docker 환경에서 데이터베이스 통합 테스트를 위한 인기 있는 프레임워크를 제공합니다. 스키마, 저장 프로시저, 함수, 사용자 정의 타입이 포함된 데이터베이스 인스턴스를 신속하게 배포할 수 있습니다. 위의 Java 함수에 대응하는 PostgreSQL 함수는 다음과 같습니다:

```sql
CREATE OR REPLACE FUNCTION add(a integer, b integer)
RETURNS integer AS
$$
BEGIN
  RETURN a + b;
END;
$$
LANGUAGE PLPGSQL;
```

## 전통적인 JDBC 접근 방식

이 함수를 JDBC를 통해 호출하려면 상당한 보일러플레이트 코드가 필요합니다:

```java
try (CallableStatement s = connection.prepareCall(
    "{ ? = call add(?, ?) }"
)) {
    s.registerOutParameter(1, Types.INTEGER);
    s.setInt(2, 1);
    s.setInt(3, 2);
    s.executeUpdate();
    System.out.println(s.getInt(1));
}
```

이 코드는 `3`을 출력합니다.

그러나 수동 JDBC 코드는 유지 보수 부담을 야기합니다. 프로시저를 리팩토링하면 컴파일 타임 오류가 아닌 런타임 오류가 발생하게 됩니다.

## jOOQ 코드 생성 솔루션

jOOQ의 코드 생성기는 `Routines` 클래스를 생성하여 보다 간단한 프로시저 호출을 가능하게 합니다:

```java
System.out.println(Routines.add(configuration, 1, 2));
```

여기서 `configuration`은 JDBC 커넥션을 jOOQ 타입으로 래핑한 것입니다.

## JUnit을 사용한 테스트 설정

JUnit의 `ClassRule`을 사용하여 Testcontainers PostgreSQL 인스턴스를 설정합니다:

```java
@ClassRule
public static PostgreSQLContainer<?> db =
    new PostgreSQLContainer<>("postgres:14")
        .withUsername("postgres")
        .withDatabaseName("postgres")
        .withPassword("test");
```

`db.getJdbcUrl()`은 jOOQ 설정에 필요한 연결 정보를 제공합니다.

## 통합 테스트 구현

최종 통합 테스트는 매우 간결해집니다:

```java
@Test
public void testAdd() {
    try (CloseableDSLContext ctx = DSL.using(
        db.getJdbcUrl(), "postgres", "test"
    )) {
        assertEquals(3, Routines.add(ctx.configuration(), 1, 2));
    }
}
```

더 깔끔한 코드 구성을 위해 DSLContext 초기화를 `@Before` 블록으로 이동할 수 있습니다. 이 접근 방식은 익숙한 JUnit 패턴과 유사합니다. 코드 생성 자체에 대해서도 Testcontainers 인스턴스는 코드 생성과 통합 테스트 실행이라는 두 가지 목적으로 활용할 수 있습니다.
