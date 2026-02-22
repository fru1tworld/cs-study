# SQL/JDBC로 레이턴시 시뮬레이션하기

> 원문: https://blog.jooq.org/simulating-latency-with-sql-jdbc/

## 개요

이 블로그 글에서는 느린 쿼리 조건에서 애플리케이션 성능을 테스트하기 위해 개발 환경에서 데이터베이스 레이턴시를 시뮬레이션하는 기법들을 살펴봅니다.

## PostgreSQL과 스칼라 서브쿼리

Gunnar Morling이 제안한 PostgreSQL의 `pg_sleep()` 함수를 사용하는 방식을 참고했습니다. 단순한 구현은 문제를 일으킵니다:

```sql
select 1
from t_book
where pg_sleep(1) is not null;
```

이 쿼리는 행마다 1초씩 대기하므로, 3개의 행이 있는 쿼리는 총 3초가 걸립니다. EXPLAIN ANALYZE 결과는 다음과 같습니다:

```
Execution Time: 3005.401 ms
```

## 해결책: 스칼라 서브쿼리 캐싱 활용

더 나은 접근법은 스칼라 서브쿼리 최적화를 활용하는 것입니다:

```sql
select 1
from t_book
where (select pg_sleep(1)) is not null
limit 3;
```

결과는 한 번만 필터링되는 것을 보여줍니다:

```
Execution Time: 1001.223 ms
```

저자는 이것이 선택적 최적화에 의존하며, 보장되는 동작이 아니라고 언급합니다.

## MATERIALIZED CTE 접근법

보장된 동작을 위해서는:

```sql
with s (x) as materialized (select pg_sleep(1))
select *
from t_book
where (select x from s) is not null;
```

이 방식은 약 1001ms의 실행 시간으로 일관되게 한 번만 필터링됩니다.

## JDBC 기반 접근법

Connection을 프록시하여 `prepareStatement()` 호출을 가로챕니다:

```java
try (Connection c1 = db.getConnection()) {
    Connection c2 = new DefaultConnection(c1) {
        @Override
        public PreparedStatement prepareStatement(String sql)
        throws SQLException {
            sleep(1000L);
            return super.prepareStatement(sql);
        }
    };

    long time = System.nanoTime();
    String sql = "SELECT id FROM book";

    try (PreparedStatement s = c2.prepareStatement(sql);
        ResultSet rs = s.executeQuery()) {
        while (rs.next())
            System.out.println(rs.getInt(1));
    }

    System.out.println("Time taken: " +
       (System.nanoTime() - time) / 1_000_000L + "ms");
}
```

헬퍼 메서드:

```java
public static void sleep(long time) {
    try {
        Thread.sleep(time);
    }
    catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

출력: 시뮬레이션된 레이턴시로 1021ms.

## 대안: executeQuery() 가로채기

```java
try (Connection c = db.getConnection()) {
    long time = System.nanoTime();

    try (PreparedStatement s = new DefaultPreparedStatement(
        c.prepareStatement("SELECT id FROM t_book")
    ) {
        @Override
        public ResultSet executeQuery() throws SQLException {
            sleep(1000L);
            return super.executeQuery();
        };
    };

        ResultSet rs = s.executeQuery()) {
        while (rs.next())
            System.out.println(rs.getInt(1));
    }

    System.out.println("Time taken: " +
        (System.nanoTime() - time) / 1_000_000L + "ms");
}
```

jOOQ의 `DefaultPreparedStatement` 편의 클래스를 사용합니다. 의존성 추가:

```xml
<dependency>
  <groupId>org.jooq</groupId>
  <artifactId>jooq</artifactId>
</dependency>
```

## jOOQ ExecuteListener 솔루션

jOOQ 사용자에게 가장 우아한 방식:

```java
try (Connection c = db.getConnection()) {
    DSLContext ctx = DSL.using(new DefaultConfiguration()
        .set(c)
        .set(new CallbackExecuteListener()
            .onExecuteStart(x -> sleep(1000L))
        )
    );

    long time = System.nanoTime();
    System.out.println(ctx.fetch("SELECT id FROM t_book"));
    System.out.println("Time taken: " +
        (System.nanoTime() - time) / 1_000_000L + "ms");
}
```

결과는 완전한 결과 집합 출력과 함께 1025ms 실행 시간을 보여줍니다.

## 결론

저자는 실제 쿼리를 수정하지 않고 개발 전용 레이턴시 시뮬레이션을 위한 "세 가지 접근법: SQL 기반(PostgreSQL), 순수 JDBC 프록시, 그리고 jOOQ의 ExecuteListener"를 제공합니다.
