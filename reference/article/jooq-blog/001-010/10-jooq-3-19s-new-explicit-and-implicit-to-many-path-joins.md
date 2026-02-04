# jOOQ 3.19의 새로운 명시적 및 암시적 to-many 경로 조인

> 원문: [jOOQ 3.19's new Explicit and Implicit to-many path joins](https://blog.jooq.org/jooq-3-19s-new-explicit-and-implicit-to-many-path-joins/)

## 이 기능들은 무엇인가?

jOOQ 3.19는 세 가지 주요 경로 기반 조인 기능을 도입합니다:

- 명시적 경로 조인(Explicit path joins)
- to-many 경로 조인(To-many path joins)
- 암시적 조인 경로 상관관계(Implicit join path correlation)

많은 ORM들이 외래 키를 탐색하기 위한 "경로 조인"을 지원합니다. jOOQ에서는 다음과 같이 작성할 수 있습니다:

```java
ctx.select(
    CUSTOMER.FIRST_NAME,
    CUSTOMER.LAST_NAME,
    CUSTOMER.address().city().country().NAME)
.from(CUSTOMER)
.fetch();
```

이 코드는 외래 키 메타데이터를 기반으로 자동 조인이 포함된 SQL을 생성하여, 수동으로 조인 조건을 작성할 필요를 없앱니다.

## 매우 관용적인 SQL

이 개념은 레이블이 지정된 외래 키가 있는 향상된 SQL 표준에 존재할 수 있는 것을 반영하여, 쿼리에서 자연스러운 경로 기반 탐색을 가능하게 합니다.

## 새 기능: 명시적 경로 조인

명시적 경로 조인은 조인 유형에 대한 세밀한 제어를 가능하게 합니다. 이제 다음과 같이 작성할 수 있습니다:

```java
ctx.select(...)
.from(CUSTOMER)
.leftJoin(CUSTOMER.address().city().country())
.fetch();
```

또는 각 단계를 점진적으로 조인할 수도 있습니다. `<implicitJoinPathTableSubtypes/>` 코드 생성이 활성화되어 있으면(기본값) `ON` 절은 선택 사항이 됩니다. 필요한 경우 추가 조건을 덧붙일 수 있습니다:

```java
.leftJoin(CUSTOMER.address().city())
.on(CUSTOMER.address().city().NAME.like("A%"))
```

## 새 기능: to-many 경로 조인

to-many 경로 조인은 부모에서 자식으로의 탐색을 가능하게 합니다. 기본적으로 비활성화되어 있는데, 이는 암시적으로 사용할 경우 프로젝션에서 예상치 못한 카르테시안 곱이 발생하여 SQL의 근본적인 행 생성 모델을 위반할 수 있기 때문입니다.

명시적으로 선언하는 것이 필요합니다:

```java
ctx.select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME, ACTOR.film().TITLE)
.from(ACTOR)
.leftJoin(ACTOR.film())
.fetch();
```

이 기본 동작을 재정의하고 싶다면 `RenderImplicitJoinType` 설정을 사용할 수 있습니다.

## 다대다(Many-to-many) 경로

코드 생성기는 이중 외래 키에 대한 유니크 제약 조건을 통해 관계 테이블을 인식하며, `ACTOR.filmActor().film()`을 통해 탐색하는 대신 `ACTOR.film()`과 같은 축약 표현을 사용할 수 있게 해줍니다.

## 새 기능: 암시적 경로 상관관계

이 기능은 외부 쿼리 테이블의 경로를 사용하여 서브쿼리를 상관시킬 수 있게 해줍니다. 이전에는 명시적 상관관계 작성이 번거로웠습니다:

```java
.where(exists(
    selectOne()
    .from(FILM_ACTOR)
    .where(FILM_ACTOR.ACTOR_ID.eq(ACTOR.ACTOR_ID))
    .and(FILM_ACTOR.film().TITLE.like("A%"))
))
```

이제 다음과 같이 간소화됩니다:

```java
.where(exists(
    selectOne()
    .from(ACTOR.film())
    .where(ACTOR.film().TITLE.like("A%"))
))
```

또는 세미 조인을 사용할 수도 있습니다:

```java
.leftSemiJoin(ACTOR.film())
.on(ACTOR.film().TITLE.like("A%"))
```

### MULTISET과 암시적 경로

`MULTISET` 상관 서브쿼리와 암시적 경로를 결합하면 우아한 중첩 쿼리가 가능해집니다:

```java
ctx.select(
    ACTOR.FIRST_NAME,
    ACTOR.LAST_NAME,
    multiset(select(ACTOR.film().TITLE).from(ACTOR.film())).as("films"),
    multiset(
        selectDistinct(ACTOR.film().category().NAME)
        .from(ACTOR.film().category())
    ).as("categories")
)
.from(ACTOR)
.fetch();
```

이 코드는 완전한 타입 안전성을 갖춘 계층적 JSON과 유사한 구조를 생성합니다. 결과는 레코드에 매핑할 수 있습니다:

```java
record Actor (
    String firstName, String lastName,
    List<String> titles,
    List<String> categories
) {}
```

## 결론

이 세 가지 jOOQ 3.19 기능은 버전 3.11에서 도입된 경로 기반 암시적 조인을 향상시켜, 타입 안전성을 유지하면서 복잡한 쿼리에 대해 더 큰 제어권, 더 명확한 구문, 그리고 놀라운 간결함을 제공합니다.
