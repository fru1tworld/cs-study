# Java 8로 JDBC ResultSet을 FlatMap하는 방법?

> 원문: https://blog.jooq.org/how-to-flatmap-a-jdbc-resultset-with-java-8/

저자: lukaseder
게시일: 2015년 4월 9일

Stack Overflow에서 흥미로운 질문을 발견했습니다. [JDBC ResultSet에서 Java 8 Stream API 사용하기](https://stackoverflow.com/questions/29549568/using-java-8-stream-api-to-flatten-a-jdbc-resultset). 이 질문은 본질적으로 다음과 같은 결과 집합을 평면화(flatten)하고 싶다는 것입니다:

```
+------+------+------+
| col1 | col2 | col3 |
+------+------+------+
| A    | B    | C    |
| D    | E    | F    |
| G    | H    | I    |
+------+------+------+
```

이 결과를 다음과 같은 List로 변환하고 싶습니다:

```
[A, B, C, D, E, F, G, H, I]
```

## jOOQ를 사용한 해결책

jOOQ를 사용하면 이 작업은 매우 간단합니다. 다음 코드를 작성하기만 하면 됩니다:

```java
List<String> list =
DSL.using(connection)
   .fetch("SELECT col1, col2, col3 FROM t")
   .stream()
   .flatMap(r -> Arrays.stream(r.into(String[].class)))
   .collect(Collectors.toList());
```

이 코드는 다음과 같이 동작합니다:

1. `DSL.using(connection)` - jOOQ DSLContext를 생성합니다
2. `.fetch("SELECT col1, col2, col3 FROM t")` - SQL 쿼리를 실행하고 결과를 jOOQ Result로 가져옵니다
3. `.stream()` - Result를 Java 8 Stream으로 변환합니다
4. `.flatMap(r -> Arrays.stream(r.into(String[].class)))` - 각 레코드를 String 배열로 변환한 후 평면화합니다
5. `.collect(Collectors.toList())` - 최종 결과를 List로 수집합니다

결과는 다음과 같습니다: `[A, B, C, D, E, F, G, H, I]`

## jOOQ 없이 쿼리를 실행하는 경우

jOOQ를 쿼리 실행에 사용하지 않더라도, 기존 JDBC ResultSet을 jOOQ로 감싸서 동일한 스트림 처리를 수행할 수 있습니다:

```java
try (ResultSet rs = ...) {
    List<String> list =
    DSL.using(connection)
       .fetch(rs)
       .stream()
       .flatMap(r -> Arrays.stream(r.into(String[].class)))
       .collect(Collectors.toList());
}
```

이 방법을 사용하면 JDBC ResultSet을 직접 사용하면서도 jOOQ의 강력한 스트림 지원 기능을 활용할 수 있습니다.

## SQL로 해결하는 방법

물론, 이 작업은 SQL만으로도 수행할 수 있습니다. UNION ALL을 사용하면 됩니다:

```sql
SELECT col1 FROM t UNION ALL
SELECT col2 FROM t UNION ALL
SELECT col3 FROM t
ORDER BY 1
```

또는 Oracle이나 SQL Server를 사용하는 경우, 더 우아한 UNPIVOT 구문을 사용할 수 있습니다:

```sql
SELECT c FROM t
UNPIVOT (c FOR col IN (col1, col2, col3))
```

UNPIVOT은 열을 행으로 변환하는 전용 SQL 기능으로, 정확히 우리가 원하는 결과를 제공합니다.

## 핵심 기법

이 예제에서 핵심이 되는 기법은 `flatMap()` 연산입니다. `flatMap()`은 각 레코드를 String 배열로 변환한 다음, 해당 배열들을 단일 스트림으로 평면화합니다. 이를 통해 2차원 구조(행과 열)를 1차원 리스트로 변환할 수 있습니다.

jOOQ의 `Record.into(String[].class)` 메서드는 레코드의 모든 열 값을 String 배열로 변환하는 편리한 방법을 제공합니다. 이 기능과 Java 8의 Stream API를 결합하면, JDBC 결과 집합을 함수형 프로그래밍 스타일로 우아하게 처리할 수 있습니다.
