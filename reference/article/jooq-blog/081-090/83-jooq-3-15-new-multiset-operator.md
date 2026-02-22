# jOOQ 3.15의 새로운 Multiset 연산자가 SQL에 대한 생각을 바꿀 것이다

> 원문: https://blog.jooq.org/jooq-3-15s-new-multiset-operator-will-change-how-you-think-about-sql/

게시일: 2021년 7월 6일 | 저자: lukaseder

---

## 서론

"이것이 SQL이 처음부터 사용되었어야 할 방식이다."

이 글에서는 The Third Manifesto, ORDBMS(객체-관계형 데이터베이스 관리 시스템)를 언급하며, 이러한 접근 방식들이 이론적 이점에도 불구하고 벤더들 간의 문법 합의 부재로 인해 "실제로 널리 보급되지 못했다"고 언급한다. 하지만 저자는 SQL/JSON 지원과 중첩 컬렉션을 통해 이러한 상황이 변화하고 있다고 주장한다.

---

## 기존 방식: JOIN 사용

전통적인 접근 방식:

이 예제는 Sakila 데이터베이스(DVD 대여점)를 사용하여 다음 요구사항을 충족한다: "모든 영화와 그 배우들, 그리고 카테고리를 가져와라"

전통적인 jOOQ 쿼리:

```java
ctx.select(
      FILM.TITLE,
      ACTOR.FIRST_NAME,
      ACTOR.LAST_NAME,
      CATEGORY.NAME
   )
   .from(ACTOR)
   .join(FILM_ACTOR)
     .on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID))
   .join(FILM)
     .on(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
   .join(FILM_CATEGORY)
     .on(FILM.FILM_ID.eq(FILM_CATEGORY.FILM_ID))
   .join(CATEGORY)
     .on(FILM_CATEGORY.CATEGORY_ID.eq(CATEGORY.CATEGORY_ID))
   .orderBy(1, 2, 3, 4)
   .fetch();
```

결과의 문제점:

출력은 비정규화되어 엄청난 반복이 발생한다:

```
+----------------+----------+---------+-----------+
|title           |first_name|last_name|name       |
+----------------+----------+---------+-----------+
|ACADEMY DINOSAUR|CHRISTIAN |GABLE    |Documentary|
|ACADEMY DINOSAUR|JOHNNY    |CAGE     |Documentary|
|ACADEMY DINOSAUR|LUCILLE   |TRACY    |Documentary|
|ACADEMY DINOSAUR|MARY      |KEITEL   |Documentary|
|ACADEMY DINOSAUR|MENA      |TEMPLE   |Documentary|
|ACADEMY DINOSAUR|OPRAH     |KILMER   |Documentary|
|ACADEMY DINOSAUR|PENELOPE  |GUINESS  |Documentary|
|ACADEMY DINOSAUR|ROCK      |DUKAKIS  |Documentary|
|ACADEMY DINOSAUR|SANDRA    |PECK     |Documentary|
|ACADEMY DINOSAUR|WARREN    |NOLTE    |Documentary|
|ACE GOLDFINGER  |BOB       |FAWCETT  |Horror     |
|ACE GOLDFINGER  |CHRIS     |DEPP     |Horror     |
|ACE GOLDFINGER  |MINNIE    |ZELLWEGER|Horror     |
```

문제점: 개발자가 수동으로 중복을 제거하고 데이터를 재구성해야 한다. 중첩 컬렉션 간의 카테시안 곱은 문제가 되어 종종 여러 쿼리가 필요하게 된다.

---

## MULTISET의 등장

새로운 jOOQ 구현:

```java
var result =
dsl.select(
      FILM.TITLE,
      multiset(
        select(
          FILM_ACTOR.actor().FIRST_NAME,
          FILM_ACTOR.actor().LAST_NAME)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
      ).as("actors"),
      multiset(
        select(FILM_CATEGORY.category().NAME)
        .from(FILM_CATEGORY)
        .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
      ).as("categories")
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch();
```

쿼리 로직:

이 접근 방식은 다음을 조회한다: 모든 영화를, 그 다음 각 영화에 대해 모든 배우를 중첩 컬렉션으로, 그 다음 각 영화에 대해 모든 카테고리를 중첩 컬렉션으로.

타입 구조:

```java
Result<Record3<
  String,                          // FILM.TITLE
  Result<Record2<String, String>>, // ACTOR.FIRST_NAME, ACTOR.LAST_NAME
  Result<Record1<String>>          // CATEGORY.NAME
>>
```

---

## 결과 표현

텍스트 형식:

```
+---------------------------+--------------------------------------------------+---------------+
|title                      |actors                                            |categories     |
+---------------------------+--------------------------------------------------+---------------+
|ACADEMY DINOSAUR           |[(PENELOPE, GUINESS), (CHRISTIAN, GABLE), (LUCI...|(Documentary) |
|ACE GOLDFINGER             |[(BOB, FAWCETT), (MINNIE, ZELLWEGER), (SEAN, GU...|(Horror)      |
|ADAPTATION HOLES           |[(NICK, WAHLBERG), (BOB, FAWCETT), (CAMERON, ST...|(Documentary) |
|AFFAIR PREJUDICE           |[(JODIE, DEGENERES), (SCARLETT, DAMON), (KENNET...|(Horror)      |
|AFRICAN EGG                |[(GARY, PHOENIX), (DUSTIN, TAUTOU), (MATTHEW, L...|(Family)      |
|AGENT TRUMAN               |[(KIRSTEN, PALTROW), (SANDRA, KILMER), (JAYNE, ...|(Foreign)     |
```

JSON 출력 (샘플):

```json
[
  {
    "title": "ACADEMY DINOSAUR",
    "actors": [
      {
        "first_name": "PENELOPE",
        "last_name": "GUINESS"
      },
      {
        "first_name": "CHRISTIAN",
        "last_name": "GABLE"
      },
      {
        "first_name": "LUCILLE",
        "last_name": "TRACY"
      },
      {
        "first_name": "SANDRA",
        "last_name": "PECK"
      }
    ],
    "categories": [
      { "name": "Documentary" }
    ]
  },
  {
    "title": "ACE GOLDFINGER",
    "actors": [
      {
        "first_name": "BOB",
        "last_name": "FAWCETT"
      }
    ]
  }
]
```

XML 출력 (샘플):

```xml
<result>
  <record>
    <title>ACADEMY DINOSAUR</title>
    <actors>
      <result>
        <record>
          <first_name>PENELOPE</first_name>
          <last_name>GUINESS</last_name>
        </record>
        <record>
          <first_name>CHRISTIAN</first_name>
          <last_name>GABLE</last_name>
        </record>
        <record>
          <first_name>LUCILLE</first_name>
          <last_name>TRACY</last_name>
        </record>
        <record>
          <first_name>SANDRA</first_name>
          <last_name>PECK</last_name>
        </record>
      </result>
    </actors>
    <categories>
      <result>
        <record>
          <name>Documentary</name>
        </record>
      </result>
    </categories>
  </record>
  <record>
    <title>ACE GOLDFINGER</title>
    <actors>
      <result>
        <record>
          <first_name>BOB</first_name>
          <last_name>FAWCETT</last_name>
        </record>
      </result>
    </actors>
  </record>
</result>
```

---

## 생성되는 SQL

PostgreSQL 예제:

```sql
select
  film.title,
  (
    select coalesce(
      jsonb_agg(jsonb_build_object(
        'first_name', t.first_name,
        'last_name', t.last_name
      )),
      jsonb_build_array()
    )
    from (
      select
        alias_78509018.first_name,
        alias_78509018.last_name
      from (
        film_actor
          join actor as alias_78509018
            on film_actor.actor_id = alias_78509018.actor_id
        )
      where film_actor.film_id = film.film_id
    ) as t
  ) as actors,
  (
    select coalesce(
      jsonb_agg(jsonb_build_object('name', t.name)),
      jsonb_build_array()
    )
    from (
      select alias_130639425.name
      from (
        film_category
          join category as alias_130639425
            on film_category.category_id = alias_130639425.category_id
        )
      where film_category.film_id = film.film_id
    ) as t
  ) as categories
from film
order by film.title
```

저자는 Db2, MySQL, Oracle, SQL Server에 대해서도 유사한 버전이 존재한다고 언급한다.

---

## 결과를 DTO로 매핑하기

Java 16 Record 사용:

```java
record Actor(String firstName, String lastName) {}
record Film(
  String title,
  List<Actor> actors,
  List<String> categories
) {}
```

일회성 필드 변환:

```java
record Title(String title) {}

// 이 필드를 어떤 쿼리에서든 사용
Field<Title> title = FILM.TITLE.convertFrom(Title::new);
```

쿼리에 convertFrom() 적용:

```java
Result<Record3<
    String,
    List<Actor>,
    Result<Record1<String>>
>> result =
dsl.select(
      FILM.TITLE,
      multiset(
        select(
          FILM_ACTOR.actor().FIRST_NAME,
          FILM_ACTOR.actor().LAST_NAME)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
      ).as("actors").convertFrom(r -> r.map(mapping(Actor::new))),
      multiset(
        select(FILM_CATEGORY.category().NAME)
        .from(FILM_CATEGORY)
        .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
      ).as("categories")
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch();
```

리플렉션 대안 사용:

```java
Result<Record3<
    String,
    List<Actor>,
    Result<Record1<String>>
>> result =
dsl.select(
      FILM.TITLE,
      multiset(
        select(
          FILM_ACTOR.actor().FIRST_NAME,
          FILM_ACTOR.actor().LAST_NAME)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
      ).as("actors").convertFrom(r -> r.into(Actor.class)),
      multiset(
        select(FILM_CATEGORY.category().NAME)
        .from(FILM_CATEGORY)
        .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
      ).as("categories")
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch();
```

카테고리 타입 단순화:

```java
Result<Record3<
    String,
    List<Actor>,
    List<String>
>> result =
dsl.select(
      FILM.TITLE,
      multiset(
        select(
          FILM_ACTOR.actor().FIRST_NAME,
          FILM_ACTOR.actor().LAST_NAME)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
      ).as("actors").convertFrom(r -> r.map(mapping(Actor::new))),
      multiset(
        select(FILM_CATEGORY.category().NAME)
        .from(FILM_CATEGORY)
        .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
      ).as("categories").convertFrom(r -> r.map(Record1::value1))
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch();
```

최종 DTO 매핑:

```java
List<Film> result =
dsl.select(
      FILM.TITLE,
      multiset(
        select(
          FILM_ACTOR.actor().FIRST_NAME,
          FILM_ACTOR.actor().LAST_NAME)
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
      ).as("actors").convertFrom(r -> r.map(mapping(Actor::new))),
      multiset(
        select(FILM_CATEGORY.category().NAME)
        .from(FILM_CATEGORY)
        .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
      ).as("categories").convertFrom(r -> r.map(Record1::value1))
   )
   .from(FILM)
   .orderBy(FILM.TITLE)
   .fetch(mapping(Film::new));
```

---

## 복잡한 예제: 다중 레벨 중첩

요구사항:

"모든 영화, 그 영화에 출연한 배우들, 영화를 분류하는 카테고리들, 그 영화를 대여한 고객들, 그리고 각 고객별 해당 영화에 대한 모든 결제 내역을 가져와라"

복잡한 타입 구조:

```java
Result<Record4<
    String,                   // FILM.TITLE
    Result<Record2<
        String,               // ACTOR.FIRST_NAME
        String                // ACTOR.LAST_NAME
    >>,                       // "actors"
    Result<Record1<String>>,  // CATEGORY.NAME
    Result<Record4<
        String,               // CUSTOMER.FIRST_NAME
        String,               // CUSTOMER.LAST_NAME
        Result<Record2<
            LocalDateTime,    // PAYMENT.PAYMENT_DATE
            BigDecimal        // PAYMENT.AMOUNT
        >>,
        BigDecimal            // "total"
    >>                        // "customers"
>>
```

구현:

```java
var result =
dsl.select(
        // 영화 조회
        FILM.TITLE,

        // ... 그리고 영화에 출연한 모든 배우들
        multiset(
            select(
                FILM_ACTOR.actor().FIRST_NAME,
                FILM_ACTOR.actor().LAST_NAME
            )
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
        ).as("actors"),

        // ... 그리고 영화를 분류하는 모든 카테고리들
        multiset(
            select(FILM_CATEGORY.category().NAME)
            .from(FILM_CATEGORY)
            .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
        ).as("categories"),

        // ... 그리고 영화를 대여한 모든 고객들과
        // 그들의 결제 내역
        multiset(
            select(
                PAYMENT.rental().customer().FIRST_NAME,
                PAYMENT.rental().customer().LAST_NAME,
                multisetAgg(
                    PAYMENT.PAYMENT_DATE,
                    PAYMENT.AMOUNT
                ).as("payments"),
                sum(PAYMENT.AMOUNT).as("total"))
            .from(PAYMENT)
            .where(PAYMENT
                .rental().inventory().FILM_ID.eq(FILM.FILM_ID))
            .groupBy(
                PAYMENT.rental().customer().CUSTOMER_ID,
                PAYMENT.rental().customer().FIRST_NAME,
                PAYMENT.rental().customer().LAST_NAME)
        ).as("customers")
    )
    .from(FILM)
    .where(FILM.TITLE.like("A%"))
    .orderBy(FILM.TITLE)
    .limit(5)
    .fetch();
```

생성되는 SQL (PostgreSQL):

```sql
select
  film.title,
  (
    select coalesce(
      jsonb_agg(jsonb_build_object(
        'first_name', t.first_name,
        'last_name', t.last_name
      )),
      jsonb_build_array()
    )
    from (
      select alias_78509018.first_name, alias_78509018.last_name
      from (
        film_actor
          join actor as alias_78509018
            on film_actor.actor_id = alias_78509018.actor_id
        )
      where film_actor.film_id = film.film_id
    ) as t
  ) as actors,
  (
    select coalesce(
      jsonb_agg(jsonb_build_object('name', t.name)),
      jsonb_build_array()
    )
    from (
      select alias_130639425.name
      from (
        film_category
          join category as alias_130639425
            on film_category.category_id =
               alias_130639425.category_id
        )
      where film_category.film_id = film.film_id
    ) as t
  ) as categories,
  (
    select coalesce(
      jsonb_agg(jsonb_build_object(
        'first_name', t.first_name,
        'last_name', t.last_name,
        'payments', t.payments,
        'total', t.total
      )),
      jsonb_build_array()
    )
    from (
      select
        alias_63965917.first_name,
        alias_63965917.last_name,
        jsonb_agg(jsonb_build_object(
          'payment_date', payment.payment_date,
          'amount', payment.amount
        )) as payments,
        sum(payment.amount) as total
      from (
        payment
          join (
            rental as alias_102068213
              join customer as alias_63965917
                on alias_102068213.customer_id =
                   alias_63965917.customer_id
              join inventory as alias_116526225
                on alias_102068213.inventory_id =
                   alias_116526225.inventory_id
          )
            on payment.rental_id = alias_102068213.rental_id
        )
      where alias_116526225.film_id = film.film_id
      group by
        alias_63965917.customer_id,
        alias_63965917.first_name,
        alias_63965917.last_name
    ) as t
  ) as customers
from film
where film.title like 'A%'
order by film.title
fetch next 5 rows only
```

JSON 결과 (샘플):

```json
[
  {
    "title": "ACADEMY DINOSAUR",
    "actors": [
      {
        "first_name": "PENELOPE",
        "last_name": "GUINESS"
      },
      {
        "first_name": "CHRISTIAN",
        "last_name": "GABLE"
      },
      {
        "first_name": "LUCILLE",
        "last_name": "TRACY"
      }
    ],
    "categories": [{ "name": "Documentary" }],
    "customers": [
      {
        "first_name": "SUSAN",
        "last_name": "WILSON",
        "payments": [
          {
            "payment_date": "2005-07-31T22:08:29",
            "amount": 0.99
          }
        ],
        "total": 0.99
      },
      {
        "first_name": "REBECCA",
        "last_name": "SCOTT",
        "payments": [
          {
            "payment_date": "2005-08-18T18:36:16",
            "amount": 0.99
          }
        ],
        "total": 0.99
      }
    ]
  }
]
```

---

## 결론

저자는 MULTISET 기능이 이전에도 존재했지만 클라이언트 API를 통해 쉽게 접근할 수 없었고, 여러 RDBMS 벤더들이 문법에 대한 합의를 이루지 못했다고 강조한다.

주요 장점:

- 타입 안전한 구현 (구조적 타이핑과 명목적 타이핑 모두 지원)
- 리플렉션 불필요
- 데이터베이스가 SQL/XML 또는 SQL/JSON을 사용하여 중첩 실행
- 쿼리 옵티마이저가 전체 중첩 쿼리를 계획할 수 있음
- 불필요한 컬럼, 쿼리, 또는 데이터베이스 작업 없음

지원 데이터베이스:

- MySQL
- Oracle
- PostgreSQL
- SQL Server

모든 제공 사항은 jOOQ의 표준 라이선스 조건을 따른다.

---

## 부록: SQL/XML 또는 SQL/JSON 직접 사용

XML이나 JSON을 직접 소비하는 SQL 클라이언트의 경우, 저자는 JSON에서 jOOQ 결과로 변환한 후 다시 JSON으로 변환하는 대신 jOOQ의 네이티브 SQL/XML 또는 SQL/JSON 지원(jOOQ 3.14에서 도입)을 사용할 것을 권장한다. 이를 통해 JSON/XML을 프론트엔드로 직접 스트리밍할 수 있다.
