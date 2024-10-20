# jOOQ 3.17은 DML에서도 암묵적 조인(Implicit Join)을 지원합니다

> 원문: https://blog.jooq.org/jooq-3-17-supports-implicit-join-also-in-dml/

jOOQ 3.11부터 암묵적 조인(implicit join)이 지원되어 왔습니다. 암묵적 조인이란 경로 표현식(path expression)의 존재로 인해 암묵적으로 생성되는 `JOIN`(대부분 `LEFT JOIN`)입니다. SQL이 이 구문을 네이티브로 지원한다면, 다음과 같이 작성할 수 있을 것입니다:

```sql
SELECT
  cu.first_name,
  cu.last_name,
  cu.address.city.country.country
FROM customer AS cu
```

이 모든 것은 명시적으로 작성된 `LEFT JOIN` 표현식들의 편의 구문에 불과합니다:

```sql
SELECT
  cu.first_name,
  cu.last_name,
  co.country
FROM customer AS cu
LEFT JOIN address AS a USING (address_id)
LEFT JOIN city AS ci USING (city_id)
LEFT JOIN country AS co USING (country_id)
```

jOOQ에서는 코드 생성(code generation)을 사용하는 경우 이 기능을 활용할 수 있습니다.

```java
ctx.select(
      CUSTOMER.FIRST_NAME,
      CUSTOMER.LAST_NAME,
      CUSTOMER.address().city().country().COUNTRY_)
   .from(CUSTOMER)
   .fetch();
```

지금까지 이 기능은 `SELECT` 문에서만 사용할 수 있었으며, `UPDATE`나 `DELETE`에서는 사용할 수 없었습니다.

## DML에서의 암묵적 조인 지원

jOOQ 3.17과 [#7508](https://github.com/jOOQ/jOOQ/issues/7508)부터, 강력한 경로 표현식을 `UPDATE`나 `DELETE`와 같은 DML 문에서도 사용할 수 있게 되었습니다. 예를 들어, 언어가 영어인 모든 도서를 업데이트해 보겠습니다. 가상의 SQL 방언(hypothetical SQL dialect)으로는 다음과 같이 작성할 수 있을 것입니다:

```sql
UPDATE book
SET book.status = 'SOLD OUT'
WHERE book.language.cd = 'en';

DELETE book
WHERE book.language.cd = 'en';
```

jOOQ에서는 다음과 같이 작성합니다:

```java
ctx.update(BOOK)
   .set(BOOK.STATUS, SOLD_OUT)
   .where(BOOK.language().CD.eq("en"))
   .execute();

ctx.delete(BOOK)
   .where(BOOK.language().CD.eq("en"))
   .execute();
```

## 상관 서브쿼리 사용 (Using correlated subqueries)

이 에뮬레이션은 간단합니다. 이 방식은 `SELECT` 쿼리에서의 암묵적 `JOIN` 에뮬레이션에도 사용할 수 있지만, `LEFT JOIN` 접근 방식이 더 최적입니다. 더 많은 RDBMS가 상관 서브쿼리(correlated subquery)에 비해 조인을 더 잘 최적화할 수 있고(둘이 동등함에도 불구하고), 여러 컬럼이 공유 경로에서 프로젝션되는 경우 기존 `JOIN` 트리를 재사용할 수 있기 때문입니다. 현재 예제에서는 암묵적으로 조인되는 컬럼이 하나뿐이므로 위의 내용은 그다지 중요하지 않습니다.

```sql
UPDATE book
SET status = 'SOLD OUT'
WHERE (
  SELECT language.cd
  FROM language
  WHERE book.language_id = language.id
) = 'en';

DELETE FROM book
WHERE (
  SELECT language.cd
  FROM language
  WHERE book.language_id = language.id
) = 'en';
```

이 접근 방식은 모든 RDBMS에서 동작하며, 여러 경로 세그먼트에 대해 재귀적으로도 작동합니다.

## DML JOIN 사용 (Using DML JOIN)

일부 RDBMS는 DML 문에서도 일종의 `JOIN` 구문을 지원하며, jOOQ는 이를 활용할 수 있습니다. 현재 이 방식은 MariaDB, MySQL, MemSQL에서만, 그리고 `UPDATE` 문에서만 적용됩니다:

```sql
UPDATE (book JOIN language AS l ON book.language_id = l.id)
SET book.status = 'SOLD OUT'
WHERE l.cd = 'en';
```

이것은 `SELECT` 문에서 이미 수행해 온 것과 거의 동일한 방식입니다. 이것이 바로 사용할 수 있다는 것이 꽤 멋진 점입니다. 사실, jOOQ 3.17 이전에도 이미 동작했지만, 공식적으로 지원하지는 않았습니다.

## 업데이트 가능한 뷰 사용 (Using updatable views)

일부 RDBMS는 표준 SQL 업데이트 가능한 뷰(updatable views)를 지원하며, 업데이트할 수 있는 인라인 뷰(inline views)도 포함됩니다. Oracle이 그 중 하나입니다. Oracle에서는 위의 MySQL의 `UPDATE .. JOIN` 구문이 지원되지 않지만, 훨씬 더 강력한 기능을 사용할 수 있습니다:

```sql
UPDATE (
  SELECT b.*, l.cd
  FROM book b
  LEFT JOIN language l ON b.language_id = l.id
) b
SET b.status = 'SOLD OUT'
WHERE b.cd = 'en'
```

이 구문을 jOOQ에서 수동으로 사용하는 것은 이미 가능하지만, jOOQ가 암묵적 `JOIN` 경로 표현식을 위의 구문으로 자동 변환하는 것은 아직 지원되지 않습니다. 하지만 곧 지원할 예정입니다. [#13917](https://github.com/jOOQ/jOOQ/issues/13917)을 참고하세요.

다른 RDBMS들은 다중 테이블 DML 문을 지원합니다. 예를 들어 PostgreSQL의 `UPDATE` 문에는 `FROM` 절이 있고, `DELETE` 문에는 `USING` 절이 있습니다. 안타깝게도 이 `FROM` 절은 `INNER JOIN` 의미론만 허용하므로, 이 구문으로는 아직 구현할 수 없는 몇 가지 엣지 케이스가 있습니다.
