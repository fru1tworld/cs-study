# 직렬화 시 JSON 객체 대신 JSON 배열 사용을 고려하라

> 원문: https://blog.jooq.org/consider-using-json-arrays-instead-of-json-objects-for-serialisation/

jOOQ에서 멋진 `MULTISET` 연산자를 구현할 때, 그 구현은 대부분 다양한 RDBMS의 SQL/JSON 지원에 의존했다.

간단히 말하면, 표준 SQL은 다음과 같이 `ARRAY` 또는 `MULTISET` 연산자를 통해 중첩 컬렉션을 지원한다:

```sql
SELECT
  f.title,
  MULTISET(
    SELECT a.first_name, a.last_name
    FROM actor AS a
    JOIN film_actor AS fa ON a.actor_id = fa.actor_id
    WHERE fa.film_id = f.film_id
  )
FROM film AS f;
```

이것은 대부분의 RDBMS에서 제대로 지원되지 않기 때문에, jOOQ는 다음과 같이 (또는 비슷한 형태로) SQL/JSON을 사용하여 에뮬레이션한다:

```sql
SELECT
  f.title,
  (
    SELECT json_arrayagg(json_array(a.first_name, a.last_name))
    FROM actor AS a
    JOIN film_actor AS fa ON a.actor_id = fa.actor_id
    WHERE fa.film_id = f.film_id
  )
FROM film AS f;
```

잠깐만. JSON 배열의 배열이라고??

보통 디버깅 용이성 등등을 위해 JSON의 중첩 컬렉션은 다음과 같은 형태일 것이라고 기대할 것이다:

```json
[
  {
    "first_name": "John",
    "last_name": "Doe"
  }, {
    "first_name": "Jane",
    "last_name": "Smith"
  }, ...
]
```

그런데 jOOQ는 대신 이렇게 직렬화한다고??

```json
[
  [ "John", "Doe" ],
  [ "Jane", "Smith" ],
  ...
]
```

그렇다, 당연히!

jOOQ는 SQL 문을 완전히 제어하고 있으며, 어떤 컬럼(그리고 데이터 타입)이 어떤 위치에 있는지 정확히 알고 있다. 왜냐하면 여러분이 jOOQ가 쿼리 객체 모델뿐만 아니라 결과 구조도 구성하도록 도왔기 때문이다.

따라서 훨씬 느린 컬럼 이름 접근 방식에 비해, 훨씬 빠른 인덱스 접근이 가능하다.

일반적인 결과 집합(ResultSet)에서도 마찬가지인데, jOOQ는 항상 JDBC의 `ResultSet.getString(int)`을 호출하지, 예를 들어 `ResultSet.getString(String)`을 호출하지 않는다.

이것은 더 빠를 뿐만 아니라, 더 신뢰할 수 있기도 하다.

중복 컬럼 이름에 대해 생각해 보자. 예를 들어, 두 테이블을 조인할 때 둘 다 `ID` 컬럼을 포함하고 있는 경우를 말이다.

JSON은 중복 객체 키에 대해 특별한 입장을 취하지 않지만, 모든 JSON 파서가 이를 지원하는 것은 아니며, Java의 `Map` 타입은 말할 것도 없다.

재미있는 사실은, `Map` API 명세에서는 중복 키를 금지하고 있지만, 구현체는 원칙적으로 이 명세를 무시하고 `Map::entrySet`과 같은 반복 메서드를 통해 중복 키에 접근할 수 있는 비표준 동작을 정의할 수 있다는 것이다.

이 이슈에서 몇 가지 실험을 확인할 수 있다: https://github.com/jOOQ/jOOQ/issues/11889

어쨌든, 이름 기반 접근이 위치 기반 접근으로 대체되면, 중복 컬럼 이름은 더 이상 문제가 되지 않는다.

## SQL과 관련이 없다

jOOQ는 이것을 SQL 맥락에서 수행하지만, 이것은 양쪽에 어떤 프로그래밍 언어가 있든 상관없이 모든 클라이언트-서버 애플리케이션 간에 적용할 수 있다는 점에 유의하라.

이것은 SQL 자체와는 전혀 관련이 없다!

## 이 조언을 적용해야 할 때

성능 관련 조언은 항상 어려운 문제이다.

"항상" (JSON 배열을 사용하라) 또는 "절대로" (JSON 객체를 사용하지 마라) 같은 것은 없다.

이것은 트레이드오프이다.

jOOQ의 경우, JSON 배열의 배열은 사용자가 전혀 보지 않는 데이터를 직렬화하는 데 사용된다. 사용자들은 `MULTISET` 연산자를 사용할 때 JSON 관점에서 생각하지도 않으며, 이제 충분히 성숙해졌기 때문에 직렬화를 디버깅할 일도 거의 없다.

가능하다면, 더 빠른 결과를 위해 서버와 클라이언트 사이에 바이너리 직렬화 형식을 사용할 것이다. 안타깝게도, SQL에는 바이너리 데이터 집계 같은 것이 존재하지 않는다.

API를 설계할 때(특히 다른 사람들이 사용하는 API), 몇 퍼센트의 속도 향상보다 명확성이 훨씬 더 중요할 수 있다.

JSON 객체는 데이터 타입을 모델링할 때 위치 기반 필드 접근만 지원하는 JSON 배열에 비해 훨씬 더 명확하다.

이 조언은 JSON을 사용해야 하지만, 가능하다면 바이너리 형식이 선호되는 옵션인 많은 기술적 JSON 직렬화 사용 사례에 적용된다고 생각한다.

하지만 확신이 없다면, 항상 먼저 측정하라!

## 압축

사람들은 이것이 데이터 전송에 관한 것이라고 생각하기 쉬운데, 그런 경우 압축이 반복적인 객체 키 오버헤드의 일부를 완화할 수 있다.

이것은 확실히 사실이며, 특히 HTTP 맥락에서 그렇다.

하지만 압축과 압축 해제의 비용은 전송 양쪽 끝에서 CPU 오버헤드 측면에서 여전히 존재하므로, 전송 데이터 크기는 줄이지만 다른 데이터 레이아웃으로 이를 수동으로 처리하는 것이 훨씬 더 간단할 수 있다.

## 벤치마크

재현 가능한 벤치마크 없이 무언가가 더 빠르다는 주장이 무슨 의미가 있겠는가?

다음 벤치마크는 다음을 사용한다:

- H2 인메모리 데이터베이스, 물론 Oracle이나 PostgreSQL 등 다른 곳에서도 재현할 수 있다
- JMH, 이것이 마이크로 벤치마크는 아니지만. JMH는 뛰어난 재현성과 통계 도구(평균, 표준 편차 등)를 제공한다

이 두 가지 도구를 사용함으로써 벤치마크는 다음을 모두 측정할 수 있다:

- 데이터 직렬화의 서버 측 영향
- 데이터 파싱의 클라이언트 측 영향
- 네트워크 전송 오버헤드 (H2의 경우 측정되지 않으므로, 여기서는 압축이 도움이 되지 않을 것이다)

JDBC를 직접 사용하며, jOOQ를 사용하지 않으므로 매핑 오버헤드는 제외된다.

다만 jOOQ의 내장 JSON 파서를 사용하는데, 다른 JSON 파서(예: Jackson)를 사용해도 비슷한 결과를 얻을 수 있을 것이다.

내 컴퓨터에서의 결과는 다음과 같다 (높을수록 좋다):

H2 (인메모리):

| 벤치마크 | 모드 | 횟수 | 점수 | 오차 | 단위 |
|-----------|------|------|-------|-------|-------|
| JSONObjectVsArrayBenchmark.testJsonArray | thrpt | 7 | 1005894.475 | ± 57611.223 | ops/s |
| JSONObjectVsArrayBenchmark.testJsonObject | thrpt | 7 | 809580.238 | ± 19848.664 | ops/s |

PostgreSQL:

| 벤치마크 | 모드 | 횟수 | 점수 | 오차 | 단위 |
|-----------|------|------|-------|-------|-------|
| JSONObjectVsArrayBenchmark.testJsonArray | thrpt | 7 | 1588.895 | ± 433.826 | ops/s |
| JSONObjectVsArrayBenchmark.testJsonObject | thrpt | 7 | 1387.053 | ± 1132.281 | ops/s |

각 경우 작은 데이터 세트(2개 컬럼의 10개 행)에 대해 대략 15% ~ 25%의 개선이 있다.

더 큰 결과에 대해서는 더 유의미할 것으로 예상되는데, 예를 들어 다시 10,000개 행의 경우:

H2:

| 벤치마크 | 모드 | 횟수 | 점수 | 오차 | 단위 |
|-----------|------|------|-------|-------|-------|
| JSONObjectVsArrayBenchmark.testJsonArray | thrpt | 7 | 2932.684 | ± 41.095 | ops/s |
| JSONObjectVsArrayBenchmark.testJsonObject | thrpt | 7 | 1643.838 | ± 31.943 | ops/s |

PostgreSQL:

| 벤치마크 | 모드 | 횟수 | 점수 | 오차 | 단위 |
|-----------|------|------|-------|-------|-------|
| JSONObjectVsArrayBenchmark.testJsonArray | thrpt | 7 | 122.875 | ± 7.133 | ops/s |
| JSONObjectVsArrayBenchmark.testJsonObject | thrpt | 7 | 71.916 | ± 3.232 | ops/s |

이 결과는 가독성 저하를 정당화할 만큼 충분히 유의미하다. `MULTISET`을 사용하는 _모든_ jOOQ 쿼리가 이 속도 향상의 혜택을 받을 것이기 때문이다.

벤치마크 코드는 아래에 있다.

이 코드는 매우 간단한 JSON 파서 핸들러를 사용하는데, 객체 키나 배열 인덱스를 실제로 추적하지 않고, 정확히 1개의 숫자 컬럼과 1개의 문자열 컬럼이 있다고 가정한다.

그러나 이것이 더 복잡한 결과에 대해 크게 문제가 되지는 않을 것이다.

```java
package org.jooq.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jooq.DSLContext;
import org.jooq.test.benchmarks.local.PGQueryBenchmark.BenchmarkState;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@Fork(value = 1, jvmArgsAppend = "-Dorg.jooq.no-logo=true")
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 7, time = 3)
public class JSONObjectVsArrayBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        DSLContext ctx;
        Connection connection;
        Statement  statement;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            // H2 setup
            connection = DriverManager.getConnection("jdbc:h2:mem:json-benchmark", "sa", "");
            ctx = DSL.using(connection);

            statement = connection.createStatement();
            statement.executeUpdate("drop table if exists t");
            statement.executeUpdate("create table t (number_column int, string_column varchar(100))");
            statement.executeUpdate("insert into t select i, i from system_range(1, 10) as t(i)");

            // PostgreSQL setup
//            try (InputStream is = BenchmarkState.class.getResourceAsStream("/config.postgres.properties")) {
//                Properties p = new Properties();
//                p.load(is);
//                Class.forName(p.getProperty("db.postgres.driver"));
//                connection = DriverManager.getConnection(
//                    p.getProperty("db.postgres.url"),
//                    p.getProperty("db.postgres.username"),
//                    p.getProperty("db.postgres.password")
//                );
//                ctx = DSL.using(connection);
//
//                statement = connection.createStatement();
//                statement.executeUpdate("drop table if exists t");
//                statement.executeUpdate("create table t (number_column int, string_column varchar(100))");
//                statement.executeUpdate("insert into t select i, i from generate_series(1, 10) as t(i)");
//            }
        }

        @TearDown(Level.Trial)
        public void teardown() throws Exception {
            statement.executeUpdate("drop table if exists t");
            statement.close();
            connection.close();
        }
    }

    // Just show that both methods really do the same thing
    public static void main(String[] args) throws Exception {
        JSONObjectVsArrayBenchmark f = new JSONObjectVsArrayBenchmark();
        BenchmarkState state = new BenchmarkState();
        state.setup();

        System.out.println(f.testJsonObject(state));
        System.out.println(f.testJsonArray(state));
    }

    record R(int n, String s) {}

    @Benchmark
    public List<R> testJsonObject(BenchmarkState state) throws SQLException {
        List<R> result = new ArrayList<>();

        try (ResultSet rs = state.statement.executeQuery(

            // H2 syntax (if only there was a way to abstract over syntax ;)
            """
            select
              json_arrayagg(json_object(
                key number_column value number_column,
                key string_column value string_column
              ))
            from t
            """

            // PostgreSQL syntax
//            """
//            select
//              json_agg(json_build_object(
//                number_column, number_column,
//                string_column, string_column
//              ))
//            from t
//            """
        )) {
            while (rs.next()) {
                new JSONParser(state.ctx, rs.getString(1), ch(result)).parse();
            }
        }

        return result;
    }

    @Benchmark
    public List<R> testJsonArray(BenchmarkState state) throws SQLException {
        List<R> result = new ArrayList<>();

        try (ResultSet rs = state.statement.executeQuery(

            // H2 syntax
            """
            select
              json_arrayagg(json_array(number_column, string_column))
            from t
            """

            // PostgreSQL syntax
//            """
//            select
//              json_agg(json_build_array(number_column, string_column))
//            from t
//            """
        )) {
            while (rs.next()) {
                new JSONParser(state.ctx, rs.getString(1), ch(result)).parse();
            }
        }

        return result;
    }

    private JSONContentHandler ch(List<R> result) {
        return new JSONContentHandler() {

            int level;
            int n;
            String s;

            @Override
            public void valueTrue() {
            }

            @Override
            public void valueString(String string) {
                s = string;
            }

            @Override
            public void valueNumber(String string) {
                n = Integer.parseInt(string);
            }

            @Override
            public void valueNull() {
            }

            @Override
            public void valueFalse() {
            }

            @Override
            public void startObject() {
                if (level >= 1) {
                    n = 0;
                    s = null;
                }
            }

            @Override
            public void startArray() {
                if (level++ >= 1) {
                    n = 0;
                    s = null;
                }
            }

            @Override
            public void property(String key) {
            }

            @Override
            public void endObject() {
                result.add(new R(n, s));
            }

            @Override
            public void endArray() {
                if (level-- >= 1) {
                    result.add(new R(n, s));
                }
            }
        };
    }
}
```
