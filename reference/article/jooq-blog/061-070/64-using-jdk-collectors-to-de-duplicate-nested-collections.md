# JDK Collector를 사용하여 부모/자식 중첩 컬렉션 중복 제거하기

> 원문: https://blog.jooq.org/using-jdk-collectors-to-de-duplicate-parent-child-nested-collections/

## 개요

이 글에서는 jOOQ를 사용하여 SQL 데이터베이스에서 부모-자식 데이터 관계를 가져올 때 Java에서 중첩 컬렉션을 처리하는 기법을 살펴봅니다.

## 문제: 조인에서의 비정규화

SQL 조인을 수행하여 관련 데이터를 가져오면 결과가 비정규화됩니다. 예를 들어, 배우와 그들의 영화를 쿼리하면 각 영화 연결마다 배우 행이 중복됩니다:

```
actor_id | first_name | film_id | title
    1    | PENELOPE   |    1    | ACADEMY DINOSAUR
    1    | PENELOPE   |   23    | ANACONDA CONFESSIONS
```

## 해결책 1: fetchGroups() 사용하기

가장 간단한 jOOQ 접근 방식은 `fetchGroups()`를 사용하여 맵 구조를 생성하는 것입니다:

```java
Map<ActorRecord, Result<FilmRecord>> result =
ctx.select(ACTOR.ACTOR_ID, ACTOR.FIRST_NAME, ACTOR.LAST_NAME,
          FILM.FILM_ID, FILM.TITLE)
   .from(ACTOR)
   .leftJoin(FILM_ACTOR).on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID))
   .leftJoin(FILM).on(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
   .orderBy(ACTOR.ACTOR_ID, FILM.FILM_ID)
   .fetchGroups(ACTOR, FILM);
```

제한사항: 이 접근 방식은 영화가 없는 배우에 대한 LEFT JOIN 시맨틱을 제대로 처리하지 못하여, 빈 리스트 대신 null 영화 레코드를 생성합니다.

## 해결책 2: collect()와 JDK Collector 사용하기

`Collectors`를 통해 더 강력한 필터링 및 매핑 기능을 활용할 수 있습니다:

```java
Map<ActorRecord, List<FilmRecord>> result =
ctx.select(...)
   .from(ACTOR)
   .leftJoin(FILM_ACTOR)...
   .leftJoin(FILM)...
   .collect(groupingBy(
       r -> r.into(ACTOR),
       filtering(
           r -> r.get(FILM.FILM_ID) != null,
           mapping(r -> r.into(FILM), toList())
       )
   ));
```

이 접근 방식은:
- 배우별로 결과를 그룹화합니다
- NULL 영화 레코드를 필터링합니다 (fetchGroups로는 불가능)
- 영화 데이터만 포함하도록 콘텐츠를 매핑합니다

## 해결책 3: SQL 네이티브 MULTISET

권장되는 접근 방식은 `MULTISET` 연산자를 통해 SQL의 네이티브 배열 기능을 활용하는 것입니다:

```java
Result<Record3<Film, List<Actor>, List<Category>>> result = ctx
    .select(
        FILM.TITLE.convertFrom(Film::new),
        multiset(select(FILM_ACTOR.actor().FIRST_NAME, ...)
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
        ).convertFrom(r -> r.map(mapping(Actor::new))),
        multiset(select(FILM_CATEGORY.category().NAME)
            .from(FILM_CATEGORY)
            .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
        ).convertFrom(r -> r.map(mapping(Category::new)))
    )
    .from(FILM)
    .fetch();
```

장점: 클라이언트 측 중복 제거 오버헤드 없이 여러 중첩 관계를 효율적으로 처리합니다.

## 핵심 요점

클라이언트 측 중복 제거는 단일 부모-자식 관계에서는 작동하지만, MULTISET을 사용하여 집계 로직을 SQL로 이동하면 더 뛰어난 성능을 제공하고 복잡한 다중 관계 시나리오를 더 우아하게 처리할 수 있습니다.
