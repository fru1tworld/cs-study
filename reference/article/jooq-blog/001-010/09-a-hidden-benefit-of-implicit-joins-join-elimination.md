# 암묵적 조인의 숨겨진 이점: 조인 제거

> 원문: https://blog.jooq.org/a-hidden-benefit-of-implicit-joins-join-elimination/

지금까지 jOOQ의 핵심 기능 중 하나는 사용자가 기대하는 SQL을 거의 그대로 렌더링하는 것이었습니다. 물론 쿼리가 동작하도록 에뮬레이션이 필요한 경우는 예외입니다. 이는 조인 제거(join elimination)가 많은 RDBMS에서 강력한 기능이지만, 지금까지는 jOOQ의 기능에 포함되지 않았다는 것을 의미합니다.

이것이 jOOQ 3.19와 [#14992](https://github.com/jOOQ/jOOQ/issues/14992)에서 암묵적 경로 조인(implicit path joins)에 한해 어느 정도 변경됩니다. 지금까지 다음과 같이 작성하면:

```java
ctx.select(ACTOR, ACTOR.film().category().NAME)
   .from(ACTOR)
   .fetch();
```

이 쿼리의 결과 조인 트리는 다음과 비슷한 형태였습니다:

```sql
FROM
  actor
    LEFT JOIN film_actor ON actor.actor_id = film_actor.actor_id
    LEFT JOIN film ON film_actor.film_id = film.film_id
    LEFT JOIN film_category ON film.film_id = film_category.film_id
    LEFT JOIN category ON film_category.category_id = category.category_id
```

그러나 이 특정 쿼리에서는 `FILM` 테이블이 실제로 필요하지 않습니다. `FILM` 테이블의 컬럼이 프로젝션되지 않으며, 기본 키/외래 키의 존재가 조인 트리에서 해당 테이블을 건너뛰어도 동등성을 보장하기 때문입니다:

```sql
FROM
  actor
    LEFT JOIN film_actor ON actor.actor_id = film_actor.actor_id
    LEFT JOIN film_category ON film_actor.film_id = film_category.film_id
    LEFT JOIN category ON film_category.category_id = category.category_id
```

`FILM` 테이블의 어떤 컬럼이라도 프로젝션되거나(또는 일반적으로 참조되면), 해당 테이블은 조인 트리에 다시 나타납니다. 예를 들어 다음 쿼리의 경우:

```java
ctx.select(ACTOR, ACTOR.film().category().NAME)
   .from(ACTOR)
   // 이는 FILM 테이블을 조인 트리에 다시 추가해야 함을 의미합니다:
   .where(ACTOR.film().TITLE.like("A%"))
   .fetch();
```

많은 RDBMS에서는 RDBMS 자체가 동일한 최적화를 수행할 수 있기 때문에 이것이 크게 중요하지 않지만, 일부 RDBMS에서는 큰 차이가 있습니다. 이것은 특히 암묵적 경로 조인에서 훌륭한 최적화입니다. 왜냐하면 jOOQ 사용자들은 FROM 절의 조인 트리를 직접 작성하지 않기 때문에, 이러한 최적화를 수동으로 작성할 수 없기 때문입니다.

## 왜 jOOQ 3.19에서야 이것을 구현했는가

jOOQ 3.19 이전에는 다대다(to-many) 경로 조인, 특히 관계 테이블을 건너뛰는 다대다(many-to-many) 경로 조인에 대한 지원이 없었습니다. 하지만 이제 사용자는 다음과 같이 작성할 수 있습니다:

```java
// 이것은
ACTOR.film().category().NAME

// 다음의 축약형이며 (동등합니다):
ACTOR.filmActor().film().filmCategory().category().NAME
```

위의 예제들은 새로운 `Settings.renderImplicitJoinToManyType` 플래그가 `LEFT_JOIN`으로 설정되어 있다고 가정합니다. 기본적으로 암묵적 다대다 조인은 이 블로그 포스트에서 설명한 것처럼 쿼리 카디널리티 측면에서 이상한 의미론을 가지기 때문에 지원되지 않습니다. 기본적으로 이러한 경로는 FROM 절에서 명시적으로 선언해야 합니다:

```java
ctx.select(ACTOR, ACTOR.film().category().NAME)
   .from(
       ACTOR,
       ACTOR.film(),
       ACTOR.film().category())
   .fetch();
```

또는 간단하게:

```java
ctx.select(ACTOR, ACTOR.film().category().NAME)
   .from(
       ACTOR,
       ACTOR.film().category())
   .fetch();
```
