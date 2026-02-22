# SQL 문자열/결과 쌍 세트를 사용하여 JDBC 모킹하기

> 원문: https://blog.jooq.org/mocking-jdbc-using-a-set-of-sql-string-result-pairs/

이 글에서는 jOOQ의 `MockFileDatabase` 구현을 사용하여 JDBC를 모킹하는 방법을 설명합니다. 이전 글에서 `MockDataProvider` 함수형 인터페이스 접근 방식에 대해 다룬 바 있습니다.

전통적인 프로그래매틱 접근 방식은 함수형 인터페이스를 사용합니다:

```java
MockDataProvider provider = context -> {
    // 위의 컨텍스트에 따라 업데이트 카운트, 결과 집합 등을
    // 정의합니다.
    return new MockResult[] { ... }
};
```

정적 SQL 문자열을 사용하는 더 간단한 시나리오의 경우, `MockFileDatabase`가 텍스트 파일 기반의 편리한 대안을 제공합니다.

## 모킹 데이터 파일 형식

`mocking.txt` 파일은 SQL 쿼리와 그에 따른 결과 집합 및 행 수를 포함하는 구조를 보여줍니다:

```
select first_name, last_name from actor;
> first_name last_name
> ---------- ---------
> GINA       DEGENERES
> WALTER     TORN
> MARY       KEITEL
@ rows: 3

select first_name, last_name, count(*)
from actor
join film_actor using (actor_id)
group by actor_id, first_name, last_name
order by count(*) desc;
> first_name last_name count
> ---------- --------- -----
> GINA       DEGENERES 42
> WALTER     TORN      41
> MARY       KEITEL    40
@ rows: 3
```

## 완전한 Java 구현

```java
import static java.lang.System.out;
import java.sql.*;
import org.jooq.tools.jdbc.*;

public class Mocking {
    public static void main(String[] args) throws Exception {
        MockDataProvider db = new MockFileDatabase(
            Mocking.class.getResourceAsStream("/mocking.txt"));

        try (Connection c = new MockConnection(db);
            Statement s = c.createStatement()) {

            out.println("Actors:");
            out.println("-------");
            try (ResultSet rs = s.executeQuery(
                "select first_name, last_name from actor")) {
                while (rs.next())
                    out.println(rs.getString(1)
                        + " " + rs.getString(2));
            }

            out.println();
            out.println("Actors and their films:");
            out.println("-----------------------");
            try (ResultSet rs = s.executeQuery(
                "select first_name, last_name, count(*)\n" +
                "from actor\n" +
                "join film_actor using (actor_id)\n" +
                "group by actor_id, first_name, last_name\n" +
                "order by count(*) desc")) {
                while (rs.next())
                    out.println(rs.getString(1)
                        + " " + rs.getString(2)
                        + " (" + rs.getInt(3) + ")");
            }
        }
    }
}
```

## 예상 출력

```
Actors:
-------
GINA DEGENERES
WALTER TORN
MARY KEITEL

Actors and their films:
-----------------------
GINA DEGENERES (42)
WALTER TORN (41)
MARY KEITEL (40)
```

## 핵심 포인트

이 접근 방식은 "완전한 기능을 갖춘 데이터베이스를 구현/모킹하는 데 사용해서는 안 되지만", jOOQ, Hibernate 또는 순수 JDBC를 사용하는지 여부와 관계없이 "몇 가지 쿼리만 가로채서 하드코딩된 결과를 JDBC 기반 호출자에게 반환하는 데 매우 유용합니다."
