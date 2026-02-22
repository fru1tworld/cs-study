# Condition은 Field이다

> 원문: https://blog.jooq.org/a-condition-is-a-field/

jOOQ 3.17부터 `Condition` 타입은 `Field<Boolean>` 타입을 확장합니다. SQL 표준이 그렇게 정의하고 있기 때문입니다:

```
<boolean value expression> ::=
  <predicate>
```

정확한 정의에는 중간 규칙들이 포함되어 있지만, 핵심은 이해하실 수 있을 것입니다. `<predicate>`(jOOQ에서 `Condition`에 해당)는 `<boolean value expression>`이 사용될 수 있는 곳이면 어디서든 사용할 수 있으며, 이는 다시 프로젝션, 조건절 및 기타 여러 곳에서 사용될 수 있습니다.

모든 SQL 방언이 이렇게 동작하는 것은 아니며, 사실 SQL:1999에서 `BOOLEAN` 데이터 타입을 표준화하기 전에는 SQL 자체도 이렇게 동작하지 않았습니다. 예를 들어, SQL-92에서는 `<predicate>`를 `<search condition>`의 가능한 대체로만 나열했는데, 이는 예를 들어 `<where clause>`에서는 사용되지만 일반적인 `<value expression>`에서는 사용되지 않습니다.

따라서 표준 SQL `BOOLEAN` 타입을 지원하는 PostgreSQL에서는 다음과 같이 동작합니다:

```sql
SELECT id, id > 2 AS big_id
FROM book
ORDER BY id
```

결과:

| id | big_id |
|----|--------|
| 1  | false  |
| 2  | false  |
| 3  | true   |
| 4  | true   |

하지만 Oracle에서는 동작하지 않으며, 다음과 같은 유용한(?) 에러 메시지를 보여줍니다:

> SQL Error [923] [42000]: ORA-00923: FROM keyword not found where expected

## jOOQ 3.16 이하에서의 동작 방식

jOOQ는 항상 `Condition`과 `Field<Boolean>`을 교환 가능하게 사용하는 방법을 지원해 왔습니다. 다음과 같은 두 개의 래퍼 메서드가 있습니다:

- `DSL.field(Condition)` -- `Field<Boolean>`을 반환
- `DSL.condition(Field<Boolean>)` -- `Condition`을 반환

이 내용은 문서에 기술되어 있습니다. 따라서 앞의 쿼리는 다음과 같이 작성할 수 있었습니다:

```java
Result<Record2<Integer, Boolean>> result =
ctx.select(BOOK.ID, field(BOOK.ID.gt(2)).as("big_id"))
//                  ^^^^^^^^^^^^^^^^^^^^ condition을 field()로 래핑
   .from(BOOK)
   .orderBy(BOOK.ID)
   .fetch();
```

PostgreSQL에 대해 생성되는 SQL은 다음과 같습니다:

```sql
SELECT
  book.id,
  (book.id > 2) AS big_id
FROM book
ORDER BY book.id
```

그리고 Oracle의 경우, 이 기능은 다음과 같이 에뮬레이션됩니다:

```sql
SELECT
  book.id,
  CASE
    WHEN book.id > 2 THEN 1
    WHEN NOT (book.id > 2) THEN 0
  END big_id
FROM book
ORDER BY book.id
```

이 에뮬레이션은 우리가 사랑하는 3값 논리(three-valued logic)를 보존합니다. 즉, `BOOK.ID`가 `NULL`인 경우 `BOOLEAN` 값도 `NULL`이 됩니다.

## jOOQ 3.17에서의 새로운 동작 방식

jOOQ 3.17과 [#11969](https://github.com/jOOQ/jOOQ/issues/11969)부터, `field(Condition)`의 수동 래핑은 더 이상 필요하지 않으며, `Condition`을 직접 프로젝션할 수 있습니다:

```java
Result<Record2<Integer, Boolean>> result =
ctx.select(BOOK.ID, BOOK.ID.gt(2).as("big_id"))
   //               ^^^^^^^^^^^^^ 더 이상 래핑이 필요 없음
   .from(BOOK)
   .orderBy(BOOK.ID)
   .fetch();
```

동작은 condition을 래핑했을 때와 정확히 동일하며(결과 타입 포함), Oracle 및 `BOOLEAN` 값 표현식을 지원하지 않는 다른 방언에 대한 에뮬레이션도 여전히 작동합니다. 이는 `Field` 타입을 받는 다른 절에서도 `Condition`을 사용할 수 있다는 것을 의미하며, 예를 들어 다음과 같은 곳에서 사용할 수 있습니다:

- `GROUP BY` 또는 `PARTITION BY`
- `ORDER BY`

jOOQ 버전을 업그레이드할 시간입니다!
