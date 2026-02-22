# 단일 행 쿼리에 대해 JDBC Statement.setFetchSize()를 1로 설정하기

> 원문: https://blog.jooq.org/setting-the-jdbc-statement-setfetchsize-to-1-for-single-row-queries/

Vladimir Sitnikov의 흥미로운 힌트가 jOOQ를 위한 새로운 벤치마크에 대해 생각하게 만들었다: 단일 행 쿼리가 자동으로 JDBC의 `Statement.setFetchSize(1)`을 호출해야 할까?

`setFetchSize()`의 Javadoc은 다음과 같이 설명한다:

> "이 `Statement`에 의해 생성된 `ResultSet` 객체에 더 많은 행이 필요할 때 데이터베이스에서 가져와야 할 행 수에 대한 힌트를 JDBC 드라이버에 제공합니다. 지정된 값이 0이면 힌트는 무시됩니다."

jOOQ와 같은 ORM이 단일 행만 조회될 것임을 알 수 있는 경우, 이 힌트를 적용하는 것은 직관적으로 합리적이다. 관련된 시나리오는 다음과 같다:

- `fetchSingle()`, `fetchOne()`, 또는 `fetchOptional()`과 같이 0-1개의 행을 기대하는 메서드를 호출할 때
- 쿼리에 `LIMIT 1` 절이 포함된 경우
- UNIQUE 제약 조건에 대한 동등 조건이 단일 행 결과를 보장하는 경우

데이터베이스 쿼리 옵티마이저는 이러한 패턴을 독립적으로 인식한다. 그러나 JDBC 드라이버는 일반적으로 이러한 맥락적 지식이 부족하기 때문에, 애플리케이션 계층에서의 힌트가 잠재적으로 가치가 있을 수 있다. 하지만 이러한 최적화가 실제로 유익한지에 대한 의문이 남는다.

Sitnikov는 pgjdbc가 데이터가 도착하면 셀을 동적으로 생성하므로, `setFetchSize()`가 반드시 불필요한 버퍼 배열의 즉각적인 인스턴스화를 유발하지는 않을 것이라고 언급했다. 그는 향후 개선 사항이 이 설정을 더 효과적으로 활용할 수 있을 것이라고 제안했다.

## 벤치마크 방법론

가정에 의존하기보다는, JMH(Java Microbenchmark Harness)를 사용한 경험적 측정을 수행했다. 엄밀히 말해 마이크로벤치마크는 아니지만, JIT 워밍업을 고려하고 분산 지표를 생성하는 JMH의 방법론이 이 비교 분석에 적합했다.

독점적 데이터베이스 시스템 간의 원시 성능 비교를 게시하는 것은 문제가 될 수 있으므로 결과를 정규화할 필요가 있었다. 각 시스템에서 더 빠른 실행이 기준(점수 1.0)이 되었고, 더 느린 결과는 비례 분수로 표현되어 벤더 간 비교 없이 공정한 자체 비교가 가능했다.

벤치마크는 새로운 JDBC 문을 반복적으로 생성하면서 간단한 단일 행 쿼리를 실행했다. `fetchSize1` 변형은 페치 크기 힌트를 포함했고, `noFetchSize`는 기본 설정을 유지했다.

벤치마크 코드는 다음과 같다:

```java
package org.jooq.test.benchmarks.local;

import java.sql.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 7, time = 3)
public class JDBCFetchSizeBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        Connection connection;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres",
                "postgres",
                "test"
            );
        }

        @TearDown(Level.Trial)
        public void teardown() throws Exception {
            connection.close();
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws SQLException;
    }

    private void run(
        Blackhole blackhole,
        BenchmarkState state,
        ThrowingConsumer<Statement> c
    ) throws SQLException {
        try (Statement s = state.connection.createStatement()) {
            c.accept(s);

            try (ResultSet rs = s.executeQuery(
                "select title from t_book where id = 1")
            ) {
                while (rs.next())
                    blackhole.consume(rs.getString(1));
            }
        }
    }

    @Benchmark
    public void fetchSize1(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        run(blackhole, state, s -> s.setFetchSize(1));
    }

    @Benchmark
    public void noFetchSize(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        run(blackhole, state, s -> {});
    }
}
```

## 벤치마크 결과

5개의 데이터베이스 시스템에 걸친 결과는 서로 다른 결과를 보여주었다. 3개의 시스템(MySQL, PostgreSQL, SQL Server)은 무시할 만한 차이를 보였다. 반면, 2개의 시스템(Db2, Oracle)은 `setFetchSize(1)`을 적용했을 때 상당히 나쁜 처리량을 보였다.

Db2:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   0.677
JDBCFetchSizeBenchmark.noFetchSize  thrpt   1.000
```

MySQL:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   0.985
JDBCFetchSizeBenchmark.noFetchSize  thrpt   1.000
```

Oracle:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   0.485
JDBCFetchSizeBenchmark.noFetchSize  thrpt   1.000
```

PostgreSQL:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   1.000
JDBCFetchSizeBenchmark.noFetchSize  thrpt   0.998
```

SQL Server:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   0.972
JDBCFetchSizeBenchmark.noFetchSize  thrpt   1.000
```

테스트된 데이터베이스 및 드라이버 버전은 다음과 같다: Db2 11.5.6.0(jcc-11.5.6.0), MySQL 8.0.29(mysql-connector-java-8.0.28), Oracle 21c(ojdbc11-21.5.0.0), PostgreSQL 14.1(postgresql-42.3.3), SQL Server 2019(mssql-jdbc-10.2.0).

## 분석

이 벤치마크는 최적화에 대한 근본적인 원칙을 보여준다: 이론적으로 타당한 접근법이 반드시 측정 가능한 개선으로 이어지지는 않는다. 페치 크기 힌트를 지지하는 직관적인 논리에도 불구하고, 경험적 테스트는 특정 환경에서 예상치 못한 성능 저하를 보여주었다.

`ArrayList`나 `StringBuilder`의 초기 크기를 최적화하려는 이전 시도들도 마찬가지로 일관성 없는 결과를 보였다. 일부 최적화는 미미한 이득을 보였고, 다른 것들은 오히려 역효과를 냈다. 이러한 예측 불가능한 결과 패턴은 포괄적인 테스트 없이 이러한 "개선"에 의존하는 것을 주저하게 만들었다.

명확한 이득이 없고(이해된 것이기는 하지만, 벤치마크 결과도 맹목적으로 신뢰하지 말라), 개선을 달성하지 못했으며 5건 중 2건에서 상황이 상당히 악화되었기 때문에, 이러한 개선에 대한 확신을 잃었고 결국 jOOQ에 구현하지 않았다.

## 후속 조사

/r/java에서의 후속 토론에서 조사할 가치가 있는 두 가지 추가 고려사항이 확인되었다: 2와 같은 대안적 페치 크기 테스트, 그리고 `PreparedStatement` 사용이 정적 문과 다른 결과에 영향을 미치는지 평가하는 것이다.

### fetchSize(2) 테스트

Oracle에서 `fetchSize(2)`를 테스트한 결과, 1로 설정했을 때의 페널티가 대부분 사라졌지만, 기본 동작에 비해 성능 이점은 나타나지 않았다. 이는 버퍼 관리 및 행 페칭 메커니즘에 관한 기본 드라이버 동작의 설명과 일치한다.

Oracle fetchSize 변형 테스트:

```
Benchmark                            Mode   Score
JDBCFetchSizeBenchmark.fetchSize1   thrpt   0.513
JDBCFetchSizeBenchmark.fetchSize2   thrpt   0.968
JDBCFetchSizeBenchmark.noFetchSize  thrpt   1.000
```

### PreparedStatement vs 정적 Statement 비교

추가 Oracle 테스트에서는 여러 페치 크기 구성에 걸쳐 정적 문과 프리페어드 문을 비교했다. 두 접근 방식 모두 동일한 성능 특성을 보여주어, 이 특정 시나리오에서 문 준비가 무시할 만한 이점을 제공하며, 프리페어드 문을 무조건 선호하는 가정에 반하는 결과를 보였다.

```
Benchmark                                    Mode     Score
JDBCFetchSizeBenchmark.fetchSizePrepared1    thrpt    0.503
JDBCFetchSizeBenchmark.fetchSizeStatic1      thrpt    0.518
JDBCFetchSizeBenchmark.fetchSizePrepared2    thrpt    0.939
JDBCFetchSizeBenchmark.fetchSizeStatic2      thrpt    0.994
JDBCFetchSizeBenchmark.noFetchSizePrepared   thrpt    1.000
JDBCFetchSizeBenchmark.noFetchSizeStatic     thrpt    0.998
```

업데이트된 벤치마크 코드는 다음과 같다:

```java
package org.jooq.test.benchmarks.local;

import java.sql.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 7, time = 3)
public class JDBCFetchSizeBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        Connection connection;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            Class.forName("oracle.jdbc.OracleDriver");
            connection = DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "TEST",
                "TEST"
            );
        }

        @TearDown(Level.Trial)
        public void teardown() throws Exception {
            connection.close();
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws SQLException;
    }

    private void runPrepared(
        Blackhole blackhole,
        BenchmarkState state,
        ThrowingConsumer<Statement> c
    ) throws SQLException {
        try (PreparedStatement s = state.connection.prepareStatement(
            "select title from t_book where id = 1")
        ) {
            c.accept(s);

            try (ResultSet rs = s.executeQuery()) {
                while (rs.next())
                    blackhole.consume(rs.getString(1));
            }
        }
    }

    private void runStatic(
        Blackhole blackhole,
        BenchmarkState state,
        ThrowingConsumer<Statement> c
    ) throws SQLException {
        try (Statement s = state.connection.createStatement()) {
            c.accept(s);

            try (ResultSet rs = s.executeQuery(
                "select title from t_book where id = 1")
            ) {
                while (rs.next())
                    blackhole.consume(rs.getString(1));
            }
        }
    }

    @Benchmark
    public void fetchSizeStatic1(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runStatic(blackhole, state, s -> s.setFetchSize(1));
    }

    @Benchmark
    public void fetchSizeStatic2(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runStatic(blackhole, state, s -> s.setFetchSize(2));
    }

    @Benchmark
    public void noFetchSizeStatic(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runStatic(blackhole, state, s -> {});
    }

    @Benchmark
    public void fetchSizePrepared1(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runPrepared(blackhole, state, s -> s.setFetchSize(1));
    }

    @Benchmark
    public void fetchSizePrepared2(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runPrepared(blackhole, state, s -> s.setFetchSize(2));
    }

    @Benchmark
    public void noFetchSizePrepared(Blackhole blackhole, BenchmarkState state)
    throws SQLException {
        runPrepared(blackhole, state, s -> {});
    }
}
```

정적 문과 프리페어드 문 간의 동등한 성능에 대한 잠재적 설명으로는 다음이 있다:

- JDBC 드라이버가 두 유형의 문을 모두 캐싱하고 있을 수 있음
- 단일 문 벤치마크에서 준비 오버헤드가 무관하여 프로덕션 패턴을 대표하지 않을 수 있음
- 서버 측 커서 캐싱 효과가 클라이언트 측 준비 이점보다 지배적일 수 있음

## 결론

최적화는 까다로운 존재이다. 이론적으로 추론할 때는 매우 합리적으로 보이지만, 실제 측정에서는 겉보기에 더 최적인 것이 실제로는 더 나쁘거나 무관한 경우가 많다. 또한 `LIMIT`을 `ROW_NUMBER()`나 `ROWNUM` 필터링으로 에뮬레이션해야 할 때는 상황이 매우 복잡해지므로, 단지 최적화를 위해 사용자를 대신하여 그렇게 하고 싶지 않다.
