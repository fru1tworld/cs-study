# SQL과 람다 표현식의 만남

> 원문: [When SQL Meets Lambda Expressions](https://blog.jooq.org/when-sql-meets-lambda-expressions/)

`ARRAY` 타입은 ISO/IEC 9075 SQL 표준의 일부입니다. 이 표준은 다음 사항들을 명시합니다:

- 배열 생성
- 데이터를 배열로 중첩하기 (예: 집계 또는 서브쿼리를 통해)
- 배열에서 테이블로 데이터를 펼치기(unnest)

하지만 함수 지원에 대해서는 매우 의견이 없는(unopinionated) 편입니다. ISO/IEC 9075-2:2023(E)의 6.47 `<array value expression>` 절은 배열의 연결(concatenation)을 명시하고 있으며, 6.48 `<array value function>` 절은 그다지 유용하지 않은 `TRIM_ARRAY` 함수만을 단독으로 나열하고 있습니다 (이 함수를 사용하면 배열의 마지막 `N`개 요소를 제거할 수 있는데, 아직까지 이에 대한 사용 사례를 접해본 적이 없습니다).

실제 구현체들은 더 나은 상황입니다. 많은 구현체들이 유용한 함수를 다수 제공하고 있으며, 최근에는 `ARRAY` 타입을 다룰 때 SQL에서 람다 표현식을 실험적으로 도입하기 시작한 더 현대적인 SQL 방언(dialect)들이 등장했습니다. 이러한 방언에는 주로 다음이 포함됩니다:

- ClickHouse
- Databricks
- DuckDB
- Snowflake
- Trino

예를 들어, `ARRAY_FILTER` 함수를 살펴보겠습니다. jOOQ를 사용하면 다음과 같이 배열에서 짝수만 유지하는 필터를 적용하는 코드를 작성할 수 있습니다:

```java
arrayFilter(array(1, 2, 2, 3), e -> e.mod(2).eq(0))
```

해당하는 jOOQ API는 간단합니다:

```java
public static <T> Field<T[]> arrayFilter(
    Field<T[]> array,
    Function1<? super Field<T>, ? extends Condition> predicate
) { ... }
```

따라서 jOOQ는 Java (또는 Kotlin, Scala)의 람다 표현식을 별다른 마법 없이 SQL 람다 표현식으로 간단하게 매핑할 수 있습니다. jOOQ에서 항상 하듯이, 올바른 타입의 표현식을 구성하기만 하면 됩니다.

이러한 표현식의 결과는 다음과 같을 수 있습니다:

```
+--------------+
| array_filter |
+--------------+
| [ 2, 2 ]     |
+--------------+
```

예를 들어, DuckDB에서는 위의 코드가 다음과 같이 변환됩니다:

```sql
array_filter(
  ARRAY[1, 2, 2, 3],
  e -> (e % 2) = 0
)
```

해당 방언이 람다 스타일 구문을 지원하지 않는 경우, 배열을 펼치고(unnest), 람다에 해당하는 `WHERE` 절을 적용한 다음, 결과를 다시 배열로 수집하는 서브쿼리를 사용하여 쉽게 에뮬레이션할 수 있습니다. 예를 들어, PostgreSQL에서는 다음과 같습니다:

```sql
(
  SELECT coalesce(
    array_agg(e),
    CAST(ARRAY[] AS int[])
  )
  FROM UNNEST(ARRAY[1, 2, 2, 3]) t (e)
  WHERE mod(e, 2) = 0
)
```

이 방식은 배열이 단순한 정적 배열 리터럴이 아니라 배열 표현식(예: `TABLE.ARRAY_FIELD`)인 경우에도 동일하게 작동합니다.

관련 함수에는 다음이 포함됩니다:

- `ARRAY_MAP`
- `ARRAY_ALL_MATCH`
- `ARRAY_ANY_MATCH`
- `ARRAY_NONE_MATCH`
