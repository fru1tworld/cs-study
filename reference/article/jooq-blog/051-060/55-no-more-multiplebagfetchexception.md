# MultipleBagFetchException은 이제 그만: Multiset 중첩 컬렉션 덕분에

> 원문: https://blog.jooq.org/no-more-multiplebagfetchexception-thanks-to-multiset-nested-collections/

최근 Hibernate의 유명한 `MultipleBagFetchException`에 관한 흥미로운 Stack Overflow 질문을 우연히 발견했습니다. 이 질문은 매우 인기가 많고, 답변도 다양합니다. 질문 전반에 걸쳐 여러 제약 사항이 논의되고 있는데, 결국 하나의 단순한 사실로 귀결됩니다:

조인은 컬렉션을 중첩하는 데 적합한 도구가 아닙니다.

Sakila 데이터베이스와 같은 스키마가 주어졌을 때, 다음과 같은 다대다(many-to-many) 관계가 존재합니다:

- `ACTOR`와 `FILM`
- `FILM`과 `CATEGORY`

특별할 것은 없습니다. 문제는 ORM을 사용할 때, O(Object, 객체 지향)의 특성상, 이 데이터를 계층 구조, 그래프, 또는 최소한 트리 형태로 표현하고 싶다는 것입니다. JSON이나 XML로 표현하고자 할 때도 마찬가지입니다.

예를 들어, Java에서 다음과 같은 DTO는 위 스키마의 자연스러운 표현입니다:

```java
record Actor(
    String firstName,
    String lastName
) {}

record Category(
    String name
) {}

record Film(
    String title,
    List<Actor> actors,
    List<Category> categories
) {}
```

JSON에서는 데이터가 다음과 같은 형태일 수 있습니다:

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
      },
      ...
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
      },
  ...
```

## 조인을 사용한 중첩 에뮬레이션

하지만 Hibernate와 SQL 전반에서 문제가 되는 것은 조인이 카테시안 곱(cartesian product)을 생성한다는 사실입니다. 사실 이것은 문제가 아니라 SQL과 관계 대수의 기능입니다. 우리 업계가 벤 다이어그램을 사용하여 조인을 잘못 가르쳐 왔다는 내용의 블로그 포스트가 별도로 있습니다.

조인은 필터링된 카테시안 곱입니다. 다음은 (필터 없는) 카테시안 곱의 예시입니다:

이제 앞서 본 중첩 컬렉션 표현을 조인만으로 만들고 싶다면, 아마 다음과 같이 작성할 것입니다:

```sql
SELECT *
FROM film AS f
  JOIN film_actor AS fa USING (film_id)
    JOIN actor AS a USING (actor_id)
  JOIN film_category AS fc USING (film_id)
    JOIN category AS c USING (category_id)
```

이 비정규화의 트리 구조를 보여주기 위해 조인에 의도적으로 들여쓰기를 했습니다. 각 film에 대해 다음을 조인합니다:

- 다수의 actor (예: `M`개)
- 다수의 category (예: `N`개)

이는 조인이 카테시안 곱이라는 특성 때문에 film이 `M * N`번 중복된다는 것을 의미합니다. 그뿐만 아니라 더 심각한 것은:

- 각 actor가 `N`번 중복됩니다 (카테고리당 한 번)
- 각 category가 `M`번 중복됩니다 (배우당 한 번)

결국, 이는 잘못된 결과로 이어질 수도 있습니다. 예를 들어 집계(aggregation) 시, 결합되어서는 안 되는 조합이 결합될 수 있습니다.

잠재적인 정확성 문제 외에도, 이것은 매우 큰 성능 문제입니다. 널리 알려진 Vlad가 그의 답변에서 설명했듯이, `JOIN FETCH` 구문과 함께 `DISTINCT` 및 여러 쿼리를 우회 방법으로 제안하고 있습니다. 그런 다음 결과를 수동으로 재조립하고 즉시 로딩(eager)과 지연 로딩(lazy)을 적절히 관리해야 합니다. 제가 보기에는 상당히 번거로운 작업입니다!

이 주제에 대해 제가 가장 좋아하는 Google 검색 결과입니다:

> 이것만 남겨두겠습니다 :)

공정하게 말하자면, 과거에는 jOOQ에서도 이런 번거로움이 있었습니다. 다만 최소한 실수로 데이터베이스 전체를 로딩하는 실수는 하지 않았습니다.

## 실제 중첩

ORDBMS가 도입된 이후(예: Informix, Oracle, PostgreSQL), 그리고 더 대중적인 SQL/XML 및 SQL/JSON 확장이 추가된 이후, SQL에서 직접 실제 중첩을 수행하는 것이 가능해졌습니다. 이 블로그에서 이에 대해 여러 차례 글을 쓴 바 있습니다:

- jOOQ 3.14, SQL/XML 및 SQL/JSON 지원과 함께 출시
- jOOQ 3.14의 SQL/XML 또는 SQL/JSON 지원을 활용한 컬렉션 중첩
- jOOQ를 사용하여 다른 RDBMS에서 SQL Server FOR XML 및 FOR JSON 구문 사용하기
- 미들웨어에서 매핑하지 마세요. SQL의 XML 또는 JSON 연산자를 대신 사용하세요
- jOOQ 3.15의 새로운 Multiset 연산자가 SQL에 대한 사고방식을 바꿀 것입니다

컬렉션을 중첩하는 올바른 방법은 위의 세 가지 직렬화 형식(네이티브, JSON, XML) 중 하나를 사용하는 SQL을 통해서입니다.

위의 기법들을 사용하면, Java에서 어떤 중첩 DTO 구조로든, 또는 어떤 중첩 JSON 형식으로든 데이터를 중첩할 수 있습니다. 이는 네이티브 SQL이나 jOOQ로 가능합니다. 향후 Hibernate에서도, 또는 이 분야에서 jOOQ의 선례를 따르는 다른 ORM에서도 가능해질 수 있습니다.

이 Stack Overflow 질문의 인기를 고려하면, 다수의 일대다(to-many) 관계를 중첩하는 문제가 얼마나 중요한지, 그리고 SQL(언어)과 ORM이 이 문제를 얼마나 오랫동안 무시해 왔는지를 간과하기 어렵습니다. 이들은 사용자에게 직렬화를 수동으로 구현하도록 남겨두는 기이한 우회 방법만을 제공해 왔지만, jOOQ는 이것이 얼마나 간단하고 투명하게 될 수 있는지를 보여주었습니다.

지금 바로 jOOQ의 MULTISET 연산자를 사용해 보세요. 기다릴 필요가 없습니다. 다음과 같이 간단합니다:

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

그리고 위 쿼리는 타입 안전합니다! DTO를 수정하는 즉시, 쿼리가 더 이상 컴파일되지 않습니다. 그뿐만이 아닙니다! jOOQ에는 파서도 있으므로, 여러분이 좋아하는 SQL 방언이 이미 오늘날 MULTISET을 지원하는 것처럼 사용할 수 있습니다. 이 쿼리를 여기서 시도해 보세요: https://www.jooq.org/translate/

```sql
SELECT
  f.title,
  MULTISET(
    SELECT a.first_name, a.last_name
    FROM film_actor AS fa
    JOIN actor AS a USING (actor_id)
    WHERE fa.film_id = f.film_id
  ) AS actors,
  MULTISET(
    SELECT c.name
    FROM film_category AS fc
    JOIN category AS c USING (category_id)
    WHERE fc.film_id = f.film_id
  ) AS categories
FROM film AS f
ORDER BY f.title
```

jOOQ의 번역기는 이것을 PostgreSQL에서 다음과 같이 변환합니다:

```sql
SELECT
  f.title,
  (
    SELECT coalesce(
      jsonb_agg(jsonb_build_array("v0", "v1")),
      jsonb_build_array()
    )
    FROM (
      SELECT
        a.first_name AS "v0",
        a.last_name AS "v1"
      FROM film_actor AS fa
        JOIN actor AS a
          USING (actor_id)
      WHERE fa.film_id = f.film_id
    ) AS "t"
  ) AS actors,
  (
    SELECT coalesce(
      jsonb_agg(jsonb_build_array("v0")),
      jsonb_build_array()
    )
    FROM (
      SELECT c.name AS "v0"
      FROM film_category AS fc
        JOIN category AS c
          USING (category_id)
      WHERE fc.film_id = f.film_id
    ) AS "t"
  ) AS categories
FROM film AS f
ORDER BY f.title
```
