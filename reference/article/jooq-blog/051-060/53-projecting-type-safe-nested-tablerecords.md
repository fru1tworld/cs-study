# jOOQ 3.17에서 타입 안전한 중첩 TableRecord 프로젝션하기

> 원문: https://blog.jooq.org/projecting-type-safe-nested-tablerecords-with-jooq-3-17/

오랫동안 요청되어 온 기능이 있었습니다: `Table<R>`이 `SelectField<R>`을 확장하도록 하는 것입니다. 이 기능이 jOOQ 3.17에서 마침내 구현되었습니다.

## PostgreSQL의 중첩 레코드

PostgreSQL에서는 비한정(unqualified) 테이블 이름을 SELECT 절에서 직접 참조하는 것을 포함하여, 다양한 방식으로 레코드를 중첩할 수 있습니다. 예를 들어 다음과 같은 쿼리를 작성할 수 있습니다:

```sql
SELECT DISTINCT actor, category
FROM actor
JOIN film_actor USING (actor_id)
JOIN film_category USING (film_id)
JOIN category USING (category_id)
```

이 쿼리는 평탄화된 개별 컬럼 대신 중첩된 레코드 구조를 결과 집합으로 생성합니다. DBeaver와 같은 도구에서 이를 계층적으로 확인할 수 있습니다. 각 컬럼에는 해당 테이블의 전체 데이터가 복합 값으로 포함됩니다.

## jOOQ 3.17에서의 구현

jOOQ 3.17부터 개발자는 자바에서 타입 안전한 테이블 프로젝션을 작성할 수 있습니다:

```java
Result<Record2<ActorRecord, CategoryRecord>> result1 =
ctx.selectDistinct(ACTOR, CATEGORY)
   .from(ACTOR)
   .join(FILM_ACTOR).using(FILM_ACTOR.ACTOR_ID)
   .join(FILM_CATEGORY).using(FILM_CATEGORY.FILM_ID)
   .join(CATEGORY).using(CATEGORY.CATEGORY_ID)
   .fetch();
```

`Result`의 타입이 `Record2<ActorRecord, CategoryRecord>`인 것에 주목하세요. 이는 jOOQ가 결과를 두 개의 중첩된 레코드로 타입 안전하게 감싸서 반환한다는 것을 의미합니다.

## 대안적 접근 방식 - Ad-hoc ROW 표현식

jOOQ 3.15에서는 ad-hoc 중첩 레코드 표현식이 도입되었습니다. 더 세밀한 제어가 필요한 경우, ROW 생성자를 사용할 수 있습니다:

```java
Result<Record2<
    Record3<Long, String, String>,
    Record2<Long, String>
>> result2 =
ctx.selectDistinct(
       row(ACTOR.ACTOR_ID, ACTOR.FIRST_NAME, ACTOR.LAST_NAME).as("actor"),
       row(CATEGORY.CATEGORY_ID, CATEGORY.NAME).as("category")
   )
   .from(ACTOR)
   .join(FILM_ACTOR).using(FILM_ACTOR.ACTOR_ID)
   .join(FILM_CATEGORY).using(FILM_CATEGORY.FILM_ID)
   .join(CATEGORY).using(CATEGORY.CATEGORY_ID)
   .fetch();
```

이 방식은 PostgreSQL의 ROW 생성자와 유사한 구문을 제공하며, 더 강력한 중첩 컬렉션 매핑에 대한 접근을 가능하게 합니다. 테이블 전체를 프로젝션하는 대신, 필요한 컬럼만 선택적으로 ROW로 감쌀 수 있어 보다 세밀한 제어가 가능합니다.

## 암시적 조인과의 통합

이 기능은 jOOQ 3.11에서 도입된 암시적 조인(implicit join) 구문과 매끄럽게 동작합니다. 명시적인 조인 선언 없이도 관계를 탐색할 수 있습니다:

```java
Result<Record2<CustomerRecord, CountryRecord>> result =
ctx.select(CUSTOMER, CUSTOMER.address().city().country())
   .from(CUSTOMER)
   .fetch();
```

이 예제에서는 `CUSTOMER` 테이블에서 출발하여 `address()`, `city()`, `country()`로 외래 키 관계를 따라 암시적으로 조인하면서, 동시에 `CUSTOMER`와 `COUNTRY` 테이블을 중첩 레코드로 프로젝션합니다. 별도의 JOIN 절을 작성할 필요가 없습니다.

## 중요한 주의사항

CUSTOMER 테이블을 프로젝션하는 것은 대부분 `CUSTOMER.*`를 프로젝션하는 것에 대한 편의 문법(sugar)에 불과합니다. 이것이 의미하는 바는, 필요하지 않은 데이터를 많이 가져올 수 있다는 것입니다. 자신이 무엇을 하고 있는지, 왜 하고 있는지를 알아야 합니다.

테이블 전체를 프로젝션하면 편리하지만, 불필요한 데이터를 조회하게 되어 성능 트레이드오프가 발생할 수 있습니다. 자주 사용하는 경우에는 데이터베이스 뷰를 생성하고 합성 외래 키(synthetic foreign key)를 설정한 다음, 해당 뷰에서 jOOQ 코드를 생성하는 것이 더 나은 접근 방식입니다. 이렇게 하면 필요한 컬럼만 포함된 뷰를 통해 동일한 타입 안전한 중첩 레코드 기능을 활용하면서도 성능을 최적화할 수 있습니다.
