# jOOQ의 DiagnosticsConnection을 사용하여 N+1 쿼리 탐지하기

> 원문: https://blog.jooq.org/using-jooqs-diagnosticsconnection-to-detect-n1-queries/

N+1 쿼리는 SQL 쿼리를 실행하는 많은 애플리케이션에서 흔히 발생하는 문제입니다. 이 문제는 다음과 같이 간단하게 설명할 수 있습니다:

- 부모 값을 가져오는 1개의 쿼리가 실행됩니다
- 각각의 개별 자식 값을 가져오는 N개의 쿼리가 실행됩니다

이 문제는 SQL에만 국한되지 않습니다. 배치(batch) 및/또는 벌크(bulk) 처리를 허용하지 않는 잘못 설계된 모든 API에서 발생할 수 있습니다(저장 프로시저조차도). 하지만 SQL에서는 특히 고통스러운데, 많은 경우 단일 쿼리에서 수많은 로직을 실행하는 것이 완전히 가능하기 때문입니다. 특히 jOOQ의 MULTISET과 SQL/XML 또는 SQL/JSON 지원을 사용하면 더욱 그렇습니다.

최악의 경우, N+1 문제는 서드파티 ORM에 의해 발생합니다 - 더 정확히는 그것의 잘못된 구현/설정 때문이지만, 일부 ORM은 N+1 문제로 자기 발에 총을 쏘기 정말 쉽게 만듭니다...

## 실용적인 예제

다음은 Sakila 데이터베이스를 쿼리하는 JDBC 코드입니다:

```java
try (Statement stmt = conn.createStatement()) {

    // 부모 쿼리, 배우(actor)를 가져옴
    try (ResultSet r1 = stmt.executeQuery(
        """
        SELECT actor_id, first_name, last_name
        FROM actor
        LIMIT 5
        """
    )) {
        while (r1.next()) {
            System.out.println();
            System.out.println(
                "Actor: " + r1.getString(2) + " " + r1.getString(2));

            // 자식 쿼리, 배우별 영화를 가져옴
            try (PreparedStatement pstmt = conn.prepareStatement(
                """
                SELECT count(*) FROM film_actor WHERE actor_id = ?
                """
            )) {
                pstmt.setInt(1, r1.getInt(1));

                try (ResultSet r2 = pstmt.executeQuery()) {
                    while (r2.next()) {
                        System.out.println("Films: " + r2.getInt(1));
                    }
                }
            }
        }
    }
}
```

물론 결과는 정확하지만, 이것을 단일 쿼리로 쉽게 실행할 수 있었습니다:

```sql
SELECT
  a.first_name,
  a.last_name,
  count(fa.film_id)
FROM actor AS a
LEFT JOIN film_actor AS fa USING (actor_id)
GROUP BY a.actor_id
```

총 200명의 배우가 있다고 할 때, 어떤 것을 선호하시겠습니까? 1+200개의 쿼리를 실행하는 것과 단 1개의 쿼리를 실행하는 것 중에서요?

SQL을 직접 제어할 수 있다면 이런 실수를 할 가능성이 훨씬 적지만, eager/lazy 로딩 설정과 복잡한 엔티티 그래프 어노테이션에 기반하여 SQL이 생성되기 때문에 (완전한) 제어를 할 수 없는 경우라면, jOOQ의 DiagnosticsConnection의 반복 구문(repeated statements) 진단 기능을 통합 테스트 환경에 연결할 수 있다는 것에 기뻐할 것입니다 (반드시 프로덕션에서는 아닙니다. 모든 SQL 문자열을 파싱하고 정규화하는 데 약간의 오버헤드가 있기 때문입니다).

## DiagnosticsConnection 솔루션

위의 JDBC 예제에 적용하면:

```java
DSLContext ctx = DSL.using(connection);
ctx.configuration().set(new DefaultDiagnosticsListener() {
    @Override
    public void repeatedStatements(DiagnosticsContext c) {

        // 커스텀 콜백, 예외를 던질 수도 있음 등
        System.out.println(
            "Repeated statement: " + c.normalisedStatement());
    }
});

Connection conn = ctx.diagnosticsConnection();
```

보시다시피, 진단 커넥션은 구문의 첫 번째 반복 이후부터 로깅을 시작합니다. 이는 트랜잭션 내에서 구문을 한 번 이상 반복할 필요가 일반적으로 없다는 가정에 기반합니다. 거의 항상 더 나은 방법이 있기 때문입니다.

출력 결과는 다음과 같습니다:

```
Repeated statement: select count(*) from film_actor where actor_id = ?;
```

## JPA / Hibernate와 함께 사용하기

여러분은 아마 이렇게 수동으로 JDBC 구문을 작성하지 않을 것입니다. 하지만 누가 JDBC를 호출하든 상관없습니다 (여러분 자신이든, jOOQ든, JdbcTemplate이든, Hibernate든 등). jOOQ의 DiagnosticsConnection 또는 DiagnosticsDataSource로 커넥션(또는 DataSource)을 프록시하면, 원인에 관계없이 이러한 이벤트를 쉽게 가로챌 수 있습니다.

jOOQ에서 이미 사용 가능한 기능을 확인하려면 공식 매뉴얼을 참조하세요. jOOQ의 향후 버전에서는 [https://github.com/jOOQ/jOOQ/issues/7527](https://github.com/jOOQ/jOOQ/issues/7527)을 통해 훨씬 더 많은 진단 기능이 추가될 예정입니다.
