# jOOQ의 MockDataProvider로 간단한 JDBC ResultSet 캐시 해킹하기

> 원문: https://blog.jooq.org/hack-up-a-simple-jdbc-resultset-cache-using-jooqs-mockdataprovider/

## 소개

특정 쿼리들은 반복적으로 데이터베이스에 접근할 필요가 없습니다. 시스템 설정, 언어, 번역 데이터와 같은 마스터 데이터 쿼리는 불필요한 네트워크 트래픽을 발생시킵니다. Oracle에는 `RESULT_CACHE` 힌트를 사용하는 내장 결과 집합 캐싱 기능이 있습니다. 하지만 대부분의 데이터베이스와 JDBC 드라이버에는 이러한 기능이 없습니다. 결과적으로 개발자들은 일반적으로 서비스 계층이나 데이터 액세스 계층에서 캐싱을 구현하며, 애플리케이션 코드 내에서 캐시 로직을 수동으로 관리합니다.

## 서비스 계층 캐싱의 문제점

여러 캐시된 쿼리를 관리하는 것이 어떻게 문제가 되는지 살펴보겠습니다. 추가 필터가 있는 부분 쿼리를 구현할 때 개발자들은 여러 결정에 직면합니다: 각 필터 변형을 별도로 캐시해야 할까요? 필터를 캐시된 데이터에만 적용해야 할까요, 아니면 적어도 한 번은 데이터베이스에 쿼리해야 할까요? 이러한 질문들이 복잡성을 가중시켜 포괄적인 서비스 계층 캐싱을 지루하고 오류가 발생하기 쉽게 만듭니다.

## 해결책: Map<String, Result<?>> 사용하기

제안하는 접근 방식은 SQL 쿼리 문자열을 키로 사용하여 jOOQ `Result` 객체(원시 JDBC `ResultSet` 객체가 아닌)를 캐시하는 Map 구조를 사용합니다. 이를 통해 JDBC 계층에서 투명한 캐싱이 가능해지며, 애플리케이션 수준의 캐시 관리가 필요 없어집니다.

## ResultCache 클래스 구현

```java
class ResultCache implements MockDataProvider {
    final Map<String, Result<?>> cache =
        new ConcurrentHashMap<>();
    final Connection connection;

    ResultCache(Connection connection) {
        this.connection = connection;
    }

    @Override
    public MockResult[] execute(MockExecuteContext ctx)
    throws SQLException {
        Result<?> result;

        // 더 정교한 캐싱 기준을 추가하세요
        if (ctx.sql().contains("from language")) {

            // 원자적 캐시 값 계산을 위해 매우 유용한
            // Java 8 API를 사용하고 있습니다
            result = cache.computeIfAbsent(
                ctx.sql(),
                sql -> DSL.using(connection).fetch(
                    ctx.sql(),
                    ctx.bindings()
                )
            );
        }

        // 다른 모든 쿼리는 데이터베이스로 전달됩니다
        else {
            result = DSL.using(connection).fetch(
                ctx.sql(),
                ctx.bindings()
            );
        }

        return new MockResult[] {
            new MockResult(result.size(), result)
        };
    }
}
```

## 캐시를 사용하는 Connection 래핑하기

```java
DSLContext normal = DSL.using(connection);
DSLContext cached = DSL.using(
    new MockConnection(new ResultCache(connection))
);
```

## 예제 쿼리

다음과 같은 쿼리들을 사용하여 캐시 동작을 확인할 수 있습니다:

```sql
SELECT COUNT(*) FROM language
SELECT COUNT(*), COUNT(*) FROM language
```

## 실행 결과

초기 캐시 적중:

```java
assertEquals(4, cached.fetchCount(LANGUAGE));
```

일반 연결을 통해 새 레코드를 삽입한 후, 캐시된 버전은 여전히 오래된 결과(4)를 반환하는 반면, 일반 버전은 업데이트된 개수(5)를 반영합니다.

## 주의사항과 제한 사항

이 구현은 "매우 단순한" 것으로 설명됩니다. 실제 구현에서는 캐시 무효화 메커니즘(시간 기반, 업데이트 기반)이 필요합니다. 현재 접근 방식은 정확한 SQL 문자열 매칭에 의존합니다; SQL의 사소한 변형조차도 캐시 미스를 발생시킵니다. 저자는 이것이 JDBC 수준 캐싱에 대한 권장 사항이 아님을 강조합니다 - 개발자들은 이 접근 방식을 독립적으로 평가해야 합니다.

## 결론

캐싱은 네이밍과 오프 바이 원 오류와 함께 컴퓨팅의 근본적인 도전 중 하나라는 것을 저자는 인정합니다. 이 글은 JDBC 수준 캐시 구현에 대한 어떤 권장 사항도 명시적으로 부인합니다. 그러나 개발자들이 이 경로를 선택할 때, jOOQ의 `MockDataProvider` 프레임워크가 간단한 구현을 가능하게 한다는 것을 보여줍니다. 중요한 장점은 jOOQ가 전체 애플리케이션에 퍼질 필요가 없다는 것입니다 - 이 사용 사례나 모킹 요구사항을 구체적으로 해결할 수 있으며, 다른 프레임워크들(JDBC, MyBatis, Hibernate)은 패치된 JDBC 연결을 통해 다른 데이터 액세스 관심사를 계속 처리할 수 있습니다.
