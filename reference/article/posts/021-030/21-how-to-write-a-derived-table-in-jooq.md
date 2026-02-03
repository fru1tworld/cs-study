# jOOQ에서 파생 테이블(Derived Table)을 작성하는 방법

> 원문: https://blog.jooq.org/how-to-write-a-derived-table-in-jooq/

jOOQ 매뉴얼에서는 파생 테이블의 간단한 예시를 보여줍니다:

```sql
SELECT nested.*
FROM (
  SELECT AUTHOR_ID, count(*) books
  FROM BOOK
  GROUP BY AUTHOR_ID
) nested
ORDER BY nested.books DESC
```

jOOQ에서는 다음과 같이 작성합니다:

```java
// Declare the derived table up front:
Table<?> nested =
    select(BOOK.AUTHOR_ID, count().as("books"))
    .from(BOOK)
    .groupBy(BOOK.AUTHOR_ID).asTable("nested");

// Then use it in SQL:
ctx.select(nested.fields())
   .from(nested)
   .orderBy(nested.field("books"))
   .fetch();
```

그리고 이것이 기본적인 전부입니다. 이 질문은 보통 파생 테이블을 다룰 때 놀라울 정도로 타입 안전성이 부족하다는 사실에서 비롯됩니다. 카탈로그에서 생성된 코드와 달리, 파생 테이블은 단지 하나의 표현식일 뿐이며, Java에서 이 표현식에 완전한 타입 안전성을 갖춘 속성을 추가할 좋은 방법이 사실상 없습니다. 파생 테이블의 컬럼은 타입 안전한 방식으로 역참조할 수 없습니다.

Java 언어에서는 아직 어휘적으로(lexically) 선언되지 않은 객체를 참조할 수 없으므로, 파생 테이블을 사용하기 _전에_ 먼저 선언해야 합니다.

그러나 아래와 같이 표현식을 재사용할 수 있습니다:

```java
// Declare a field expression up front:
Field<Integer> count = count().as("books");

// Then use it in the derived table:
Table<?> nested =
    select(BOOK.AUTHOR_ID, count)
    .from(BOOK)
    .groupBy(BOOK.AUTHOR_ID).asTable("nested");

// And use it as well in the outer query, when dereferencing a column:
ctx.select(nested.fields())
   .from(nested)
   .orderBy(nested.field(count))
   .fetch();
```

## 정말로 파생 테이블이 필요했을까?

사실, jOOQ 매뉴얼의 이 예시 자체가 파생 테이블이 전혀 필요하지 않았습니다! SQL 쿼리는 다음과 같이 단순화할 수 있습니다:

```sql
SELECT AUTHOR_ID, count(*) books
FROM BOOK
GROUP BY AUTHOR_ID
ORDER BY books DESC
```

이 단순화로 인해 잃는 것은 아무것도 없습니다.

Stack Overflow나 다른 곳에서 이런 질문에 답할 때, 많은 경우 파생 테이블이 애초에 필요하지 않았다는 것이 드러나곤 합니다. 쿼리가 충분히 단순하다면, "jOOQ에서 파생 테이블을 어떻게 작성하나요?"라는 질문은 "애초에 파생 테이블이 필요했던 걸까?"로 바뀌어야 합니다.

다음은 jOOQ로 작성한 동일한 쿼리입니다:

```java
// We can still assign expressions to local variables
Field<Integer> count = count().as("books");

// And then use them in the query:
ctx.select(BOOK.AUTHOR_ID, count)
   .from(BOOK)
   .groupBy(BOOK.AUTHOR_ID)
   .orderBy(count)
   .fetch();
```

## 결론

불필요한 파생 테이블을 제거하면, 쿼리 전체에서 jOOQ의 생성 코드(generated code)를 활용할 수 있어 가독성과 타입 안전성이 모두 향상됩니다. 이 접근 방식은 jOOQ 코드뿐만 아니라 기반이 되는 SQL 쿼리의 품질도 개선합니다.

파생 테이블을 작성하기 전에, 먼저 자신의 특정 사용 사례에 파생 테이블이 정말로 필요한지 스스로에게 물어보세요.
