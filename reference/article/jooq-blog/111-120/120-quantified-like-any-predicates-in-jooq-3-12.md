# jOOQ 3.12의 정량화된 LIKE ANY 술어

> 원문: https://blog.jooq.org/quantified-like-any-predicates-in-jooq-3-12/

게시일: 2019년 9월 5일 | 저자: lukaseder

## 정량화된 비교 술어

SQL의 가장 특이한 기능 중 하나는 정량화된 비교 술어입니다. 저는 실제로 이것들이 사용되는 것을 거의 본 적이 없습니다:

```sql
SELECT *
FROM t
WHERE id = ANY (1, 2, 3)
```

위의 예제는 훨씬 더 읽기 쉬운 `IN` 술어를 사용하는 것과 동일합니다:

```sql
SELECT *
FROM t
WHERE id IN (1, 2, 3)
```

이 동등성은 SQL 표준에 정의되어 있습니다. 이러한 정량화된 비교 술어를 사용하면 다른 방법보다 더 편리하게 해결할 수 있는 더 난해한 경우들이 있습니다. 예를 들어:

```sql
SELECT *
FROM t
WHERE (a, b) > ALL (
  SELECT x, y
  FROM u
)
```

이것은 더 장황하고, 제 생각에는 조금 덜 읽기 쉬운 다음과 같은 방식으로 작성하는 것과 같은 의미입니다:

```sql
SELECT *
FROM t
WHERE (a, b) > (
  SELECT x, y
  FROM u
  ORDER BY x, y
  FETCH FIRST ROW ONLY
)
```

물론, 이것은 여러분의 RDBMS가 그런 식으로 행 값 표현식을 비교할 수 있다고 가정했을 때의 이야기입니다.

## 정량화된 LIKE 술어

안타깝게도, SQL 표준과 대부분의 구현체들은 위의 정량화된 비교 술어를 `<, <=, >, >=, =, !=` 비교 연산자에 대해서만 지원합니다. 다른 술어 유형에 대해서는 지원하지 않습니다. 예를 들어, `LIKE` 술어는 이런 문법이 있다면 크게 이득을 볼 수 있을 것입니다:

```sql
SELECT *
FROM customers
WHERE last_name LIKE ANY ('A%', 'B%', 'C%')
```

이 문법은 즉시 이해할 수 있으며 다음과 같이 변환됩니다:

```sql
SELECT *
FROM customers
WHERE last_name LIKE 'A%'
OR last_name LIKE 'B%'
OR last_name LIKE 'C%'
```

…이것은 작성하기가 훨씬 덜 편리합니다! 더 나아가, 서브쿼리에서 이러한 패턴들을 생성하는 것을 상상해 보세요:

```sql
SELECT *
FROM customers
WHERE last_name LIKE ANY (
  SELECT pattern
  FROM patterns
  WHERE pattern.customer_type = customer.customer_type
)
```

이것은 표준 SQL로 에뮬레이션하기가 조금 더 까다롭습니다. 예를 들어, PostgreSQL에서는 다음과 같이 작성할 수 있습니다:

```sql
SELECT *
FROM customers
WHERE true = ANY (
  SELECT last_name LIKE pattern
  FROM patterns
  WHERE pattern.customer_type = customer.customer_type
)
```

이 경우에는 boolean 타입을 사용할 수 있습니다. Oracle에서는 이것이 조금 더 어려워집니다:

```sql
SELECT *
FROM customers
WHERE 1 = ANY (
  SELECT CASE
    WHEN last_name LIKE pattern THEN 1
    WHEN NOT(last_name LIKE pattern) THEN 0
    ELSE NULL
  END
  FROM patterns
  WHERE pattern.customer_type = customer.customer_type
)
```

이것은 지원할 만한 유용한 SQL 기능이 아닐까요?

## jOOQ 3.12의 지원

jOOQ는 jOOQ 3.12부터 이 문법을 지원합니다. 이제 다음과 같이 작성할 수 있습니다:

```java
ctx.selectFrom(CUSTOMERS)
   .where(CUSTOMERS.LAST_NAME.like(any("A%", "B%", "C%")))
   .fetch();

ctx.selectFrom(CUSTOMERS)
   .where(CUSTOMERS.LAST_NAME.like(any(
      select(PATTERNS.PATTERN)
      .from(PATTERNS)
      .where(PATTERN.CUSTOMER_TYPE.eq(CUSTOMER.CUSTOMER_TYPE))
   )))
   .fetch();
```

앞서 언급한 모든 에뮬레이션이 사용 가능합니다. jOOQ를 다운로드하여 직접 사용해 볼 수 있습니다: https://www.jooq.org/download 또는 웹사이트에서 직접 테스트해 볼 수 있습니다: https://www.jooq.org/translate
