# jOOQ 3.10이 지원하는 흥미로운 MySQL 8.0 기능들

> 원문: https://blog.jooq.org/jooq-3-10-supports-exciting-mysql-8-0-features/

MySQL 8.0이 드디어 출시되었고, jOOQ 3.10은 MySQL 8.0의 두 가지 주요 SQL 기능을 지원합니다.

## 재귀 공통 테이블 표현식(Recursive Common Table Expressions)

데이터베이스 전반에서 사용할 수 있는 가장 강력한 SQL 기능 중 하나인 "재귀 공통 테이블 표현식(Recursive CTE)"이 MySQL에서도 사용 가능해졌습니다. 다음은 시퀀스를 생성하는 재귀 쿼리 예제입니다:

```sql
WITH RECURSIVE t(a, b) AS (
  SELECT 1, CAST('a' AS CHAR(15))
  UNION ALL
  SELECT t.a + 1, CONCAT(t.b, 'a')
  FROM t
  WHERE t.a < 10
)
SELECT a, SUM(a) OVER (ORDER BY a) AS ∑, b
FROM t
```

이 쿼리는 다음과 같은 결과를 생성합니다:

```
+----+----+-----------------+
| a  | ∑  | b               |
+----+----+-----------------+
|  1 |  1 | a               |
|  2 |  3 | aa              |
|  3 |  6 | aaa             |
|  4 | 10 | aaaa            |
|  5 | 15 | aaaaa           |
|  6 | 21 | aaaaaa          |
|  7 | 28 | aaaaaaa         |
|  8 | 36 | aaaaaaaa        |
|  9 | 45 | aaaaaaaaa       |
| 10 | 55 | aaaaaaaaaa      |
+----+----+-----------------+
```

## 윈도우 함수(Window Functions)

위의 동일한 쿼리에서 `SUM(a) OVER (ORDER BY a)`를 통해 윈도우 함수도 시연되었습니다. 이를 통해 누적 계산이 가능해집니다.

## 보너스 기능: 비관적 잠금(Pessimistic Locking)

MySQL 8.0은 새로운 비관적 잠금 절도 지원합니다. 특히 `FOR UPDATE SKIP LOCKED`는 메시지 큐나 예약 시스템 같은 시나리오를 처리하는 데 유용합니다.

## jOOQ 구현 예제

jOOQ에서는 유창한(fluent) API를 사용하여 재귀 CTE를 표현할 수 있습니다. `DSL`과 `SQLDataType`에서 정적 임포트를 사용합니다:

```java
// jOOQ로 재귀 CTE 표현하기
CommonTableExpression<Record2<Integer, String>> t =
    name("t").fields("a", "b").as(
        select(
            val(1),
            cast("a", VARCHAR(15))
        )
        .unionAll(
            select(
                field(name("t", "a"), INTEGER).add(1),
                concat(field(name("t", "b"), VARCHAR(15)), "a")
            )
            .from(table(name("t")))
            .where(field(name("t", "a"), INTEGER).lt(10))
        )
    );

Result<?> result =
    ctx.withRecursive(t)
       .select(
           t.field("a"),
           sum(t.field("a", INTEGER)).over(orderBy(t.field("a"))).as("∑"),
           t.field("b")
       )
       .from(t)
       .fetch();
```

jOOQ 3.10은 MySQL 8.0의 이러한 흥미로운 새 기능들을 완벽하게 지원하여, Java 개발자들이 타입 안전한 방식으로 이 강력한 SQL 기능들을 활용할 수 있게 해줍니다.
