# 다양한 To-Many 중첩 알고리즘의 성능

> 원문: https://blog.jooq.org/the-performance-of-various-to-many-nesting-algorithms/

jOOQ 3.15가 혁신적인 표준 SQL `MULTISET` 에뮬레이션 기능과 함께 출시된 지 꽤 시간이 지났다. 오랫동안 약속해왔던, jOOQ에서 to-many 관계를 중첩하는 다양한 접근 방식에 대한 벤치마크가 드디어 여기에 있다.

이 글은 적당한 크기의 데이터셋을 사용하는 실제 시나리오에서, jOOQ의 `MULTISET` 에뮬레이션이 다음과 비슷한 수준으로 수행된다는 것을 보여준다:

- 단일 조인 쿼리를 수동으로 실행하고 결과를 중복 제거하는 방식
- 중첩 수준별로 여러 쿼리를 실행하고 클라이언트 측에서 결과를 매칭하는 방식

이 모든 접근 방식은 악명 높은 N+1 쿼리 "전략"보다 훨씬 뛰어난 성능을 보이면서도, 우수한 가독성과 유지보수성을 유지한다.

### 결론은 다음과 같다

- jOOQ 사용자는 nested loop join이 적합한 소규모 데이터셋에서 `MULTISET`을 자유롭게 사용할 수 있다
- hash join이나 merge join이 필요한 대규모 데이터셋(예: 리포팅 시나리오)에서는 `MULTISET` 사용에 주의가 필요하다
- ORM 벤더는 미리 정의된 객체 그래프에 대해 SQL 생성을 제어할 때, 중첩 수준당 여러 쿼리를 실행하는 접근 방식을 선호해야 한다

## 벤치마크 아이디어

이 벤치마크는 유명한 Sakila 데이터베이스를 사용하며, 두 가지 구별되는 쿼리 패턴을 테스트한다:

### 자식 컬렉션을 이중 중첩하는 쿼리 (DN = DoubleNesting)

배우(Actor)와 그 영화(Film), 그리고 각 영화의 카테고리(Category)를 중첩 구조로 반환한다.

### 단일 부모에 두 개의 자식 컬렉션을 중첩하는 쿼리 (MCC = Multiple Child Collections)

영화(Film)에 배우(Actor) 컬렉션과 카테고리(Category) 컬렉션을 계층적이 아닌 형제 관계로 포함하여 반환한다.

### 데이터셋 크기

Sakila 데이터베이스에는 1000개의 `FILM` 항목과 200개의 `ACTOR` 항목이 있으므로, 거대한 데이터셋은 아니다. 테스트에서는 전체 데이터셋 조회와 필터링된 부분 집합(`ACTOR_ID = 1` 또는 `FILM_ID = 1`) 조회를 모두 수행한다. 이는 hash join이 일반적으로 더 큰 결과 집합에 최적화되고, nested loop join은 더 작은 결과 집합에 유리하기 때문이다.

## 벤치마크 테스트

다음의 Java 레코드 타입을 사용하여 중첩 결과를 구조화한다:

```java
record DNName(String firstName, String lastName) {}
record DNCategory(String name) {}
record DNFilm(long id, String title, List<DNCategory> categories) {}
record DNActor(long id, DNName name, List<DNFilm> films) {}

record MCCName(String firstName, String lastName) {}
record MCCActor(long id, MCCName name) {}
record MCCCategory(String name) {}
record MCCFilm(long id, String title, List<MCCActor> actors, List<MCCCategory> categories) {}
```

## 1. 단일 MULTISET 쿼리 (DN)

```java
return state.ctx.select(
    ACTOR.ACTOR_ID,
    row(
        ACTOR.FIRST_NAME,
        ACTOR.LAST_NAME
    ).mapping(DNName::new),
    multiset(
        select(
            FILM_ACTOR.FILM_ID,
            FILM_ACTOR.film().TITLE,
            multiset(
                select(FILM_CATEGORY.category().NAME)
                .from(FILM_CATEGORY)
                .where(FILM_CATEGORY.FILM_ID.eq(FILM_ACTOR.FILM_ID))
            ).convertFrom(r -> r.map(mapping(DNCategory::new)))
        )
        .from(FILM_ACTOR)
        .where(FILM_ACTOR.ACTOR_ID.eq(ACTOR.ACTOR_ID))
    ).convertFrom(r -> r.map(mapping(DNFilm::new))))
.from(ACTOR)
.where(state.filter ? ACTOR.ACTOR_ID.eq(1L) : noCondition())
.fetch(mapping(DNActor::new));
```

## 2. 단일 JOIN 쿼리 (DN)

단일 조인 접근 방식은 평탄화된 결과를 함수형 그룹화를 사용하여 변환한다:

```java
return state.ctx.select(
    FILM_ACTOR.ACTOR_ID,
    FILM_ACTOR.actor().FIRST_NAME,
    FILM_ACTOR.actor().LAST_NAME,
    FILM_ACTOR.FILM_ID,
    FILM_ACTOR.film().TITLE,
    FILM_CATEGORY.category().NAME)
.from(FILM_ACTOR)
.join(FILM_CATEGORY).on(FILM_ACTOR.FILM_ID.eq(FILM_CATEGORY.FILM_ID))
.where(state.filter ? FILM_ACTOR.ACTOR_ID.eq(1L) : noCondition())
.collect(groupingBy(
    r -> new DNActor(
        r.value1(),
        new DNName(r.value2(), r.value3()),
        null
    ),
    groupingBy(r -> new DNFilm(r.value4(), r.value5(), null))
))
.entrySet()
.stream()
.map(a -> new DNActor(
    a.getKey().id(),
    a.getKey().name(),
    a.getValue()
     .entrySet()
     .stream()
     .map(f -> new DNFilm(
         f.getKey().id(),
         f.getKey().title(),
         f.getValue().stream().map(
             c -> new DNCategory(c.value6())
         ).toList()
     ))
     .toList()
))
.toList();
```

## 3. 두 개의 쿼리를 메모리에서 병합 (DN)

별도의 쿼리를 실행하고 클라이언트 측에서 결과를 결합한다:

```java
Result<Record5<Long, String, String, Long, String>> actorAndFilms =
state.ctx
    .select(
        FILM_ACTOR.ACTOR_ID,
        FILM_ACTOR.actor().FIRST_NAME,
        FILM_ACTOR.actor().LAST_NAME,
        FILM_ACTOR.FILM_ID,
        FILM_ACTOR.film().TITLE)
    .from(FILM_ACTOR)
    .where(state.filter ? FILM_ACTOR.ACTOR_ID.eq(1L) : noCondition())
    .fetch();

Map<Long, List<DNCategory>> categoriesPerFilm = state.ctx
    .select(
        FILM_CATEGORY.FILM_ID,
        FILM_CATEGORY.category().NAME)
    .from(FILM_CATEGORY)
    .where(state.filter
        ? FILM_CATEGORY.FILM_ID.in(actorAndFilms.map(r -> r.value4()))
        : noCondition())
    .collect(intoGroups(
        r -> r.value1(),
        r -> new DNCategory(r.value2())
    ));

return actorAndFilms
    .collect(groupingBy(
        r -> new DNActor(
            r.value1(),
            new DNName(r.value2(), r.value3()),
            null
        ),
        groupingBy(r -> new DNFilm(r.value4(), r.value5(), null))
    ))
    .entrySet()
    .stream()
    .map(a -> new DNActor(
        a.getKey().id(),
        a.getKey().name(),
        a.getValue()
         .entrySet()
         .stream()
         .map(f -> new DNFilm(
             f.getKey().id(),
             f.getKey().title(),
             categoriesPerFilm.get(f.getKey().id())
         ))
         .toList()
    ))
    .toList();
```

## 4. N+1 쿼리 (DN)

```java
return state.ctx
    .select(ACTOR.ACTOR_ID, ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
    .from(ACTOR)
    .where(state.filter ? ACTOR.ACTOR_ID.eq(1L) : noCondition())
    .fetch(a -> new DNActor(
        a.value1(),
        new DNName(a.value2(), a.value3()),
        state.ctx
            .select(FILM_ACTOR.FILM_ID, FILM_ACTOR.film().TITLE)
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.ACTOR_ID.eq(a.value1()))
            .fetch(f -> new DNFilm(
                f.value1(),
                f.value2(),
                state.ctx
                    .select(FILM_CATEGORY.category().NAME)
                    .from(FILM_CATEGORY)
                    .where(FILM_CATEGORY.FILM_ID.eq(f.value1()))
                    .fetch(r -> new DNCategory(r.value1()))
            ))
    ));
```

## 1. 단일 MULTISET 쿼리 (MCC)

```java
return state.ctx
    .select(
        FILM.FILM_ID,
        FILM.TITLE,
        multiset(
            select(
                FILM_ACTOR.ACTOR_ID,
                row(
                    FILM_ACTOR.actor().FIRST_NAME,
                    FILM_ACTOR.actor().LAST_NAME
                ).mapping(MCCName::new)
            )
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
        ).convertFrom(r -> r.map(mapping(MCCActor::new))),
        multiset(
            select(FILM_CATEGORY.category().NAME)
            .from(FILM_CATEGORY)
            .where(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
        ).convertFrom(r -> r.map(mapping(MCCCategory::new))))
    .from(FILM)
    .where(state.filter ? FILM.FILM_ID.eq(1L) : noCondition())
    .fetch(mapping(MCCFilm::new));
```

## 2. 단일 JOIN 쿼리 (MCC)

이런 종류의 중첩은 단일 JOIN 쿼리로 수행하기 매우 어렵다. ACTOR와 CATEGORY 사이에 카테시안 곱이 발생하기 때문이다. 이론적으로는 가능하지만, 만약 그렇지 않다면? 중복을 올바르게 제거하는 것이 불가능할 수도 있다. 어렵기 때문에, 여기서 성능을 테스트하는 것은 무의미하다.

## 3. 두 개의 쿼리를 메모리에서 병합 (MCC)

```java
Result<Record5<Long, String, Long, String, String>> filmsAndActors =
state.ctx
    .select(
        FILM_ACTOR.FILM_ID,
        FILM_ACTOR.film().TITLE,
        FILM_ACTOR.ACTOR_ID,
        FILM_ACTOR.actor().FIRST_NAME,
        FILM_ACTOR.actor().LAST_NAME)
    .from(FILM_ACTOR)
    .where(state.filter ? FILM_ACTOR.FILM_ID.eq(1L) : noCondition())
    .fetch();

Map<Long, List<MCCCategory>> categoriesPerFilm = state.ctx
    .select(
        FILM_CATEGORY.FILM_ID,
        FILM_CATEGORY.category().NAME)
    .from(FILM_CATEGORY)
    .where(FILM_CATEGORY.FILM_ID.in(
        filmsAndActors.map(r -> r.value1())
    ))
    .and(state.filter ? FILM_CATEGORY.FILM_ID.eq(1L) : noCondition())
    .collect(intoGroups(
        r -> r.value1(),
        r -> new MCCCategory(r.value2())
    ));

return filmsAndActors
    .collect(groupingBy(
        r -> new MCCFilm(r.value1(), r.value2(), null, null),
        groupingBy(r -> new MCCActor(
            r.value3(),
            new MCCName(r.value4(), r.value5())
        ))
    ))
    .entrySet()
    .stream()
    .map(f -> new MCCFilm(
        f.getKey().id(),
        f.getKey().title(),
        new ArrayList<>(f.getValue().keySet()),
        categoriesPerFilm.get(f.getKey().id())
    ))
    .toList();
```

## 4. N+1 쿼리 (MCC)

```java
return state.ctx
    .select(FILM.FILM_ID, FILM.TITLE)
    .from(FILM)
    .where(state.filter ? FILM.FILM_ID.eq(1L) : noCondition())
    .fetch(f -> new MCCFilm(
        f.value1(),
        f.value2(),
        state.ctx
            .select(
                FILM_ACTOR.ACTOR_ID,
                FILM_ACTOR.actor().FIRST_NAME,
                FILM_ACTOR.actor().LAST_NAME)
            .from(FILM_ACTOR)
            .where(FILM_ACTOR.FILM_ID.eq(f.value1()))
            .fetch(a -> new MCCActor(
                a.value1(),
                new MCCName(a.value2(), a.value3())
            )),
        state.ctx
            .select(FILM_CATEGORY.category().NAME)
            .from(FILM_CATEGORY)
            .where(FILM_CATEGORY.FILM_ID.eq(f.value1()))
            .fetch(c -> new MCCCategory(c.value1()))
    ));
```

## 알고리즘 복잡도

### N+1의 경우

N+1 쿼리의 알고리즘 복잡도는 단일 중첩 컬렉션의 경우 `O(N * log M)`, 즉 M개의 값이 있는 인덱스에서 N번 값을 조회하는 것이다(인덱스가 있다고 가정). 이중 중첩 컬렉션의 경우 훨씬 더 나빠서 `O(N * log M * ? * log L)`이다. 이 모든 값이 매우 작기를 바라는 수밖에 없다.

### MULTISET의 경우

이론적으로는 `MULTISET` 경우에 hash join 스타일의 중첩 컬렉션 알고리즘을 구현하는 것이 가능할 수 있지만, `JSON_ARRAYAGG`와 `XMLAGG`를 사용하는 에뮬레이션이 이런 방식으로 최적화되지는 않을 것으로 보인다.

PostgreSQL 실행 계획 분석은 두 개의 중첩된 스칼라 서브쿼리를 보여주며, 이는 서버 측에서 N+1 상황이 발생하고 있음을 확인해준다. 다만 매번 서버 왕복 지연 시간이 발생하지 않는다는 차이가 있다!

jOOQ는 반복적인 컬럼 이름 전송을 피하고, 역직렬화 시 위치 인덱스를 제공하기 위해 레코드를 JSON 객체가 아닌 JSON 배열로 직렬화한다.

PostgreSQL에서는 `ARRAY(<subquery>)`와 `ARRAY_AGG()`를 사용하여 작업할 수 있으며, 이는 `JSON_AGG`보다 옵티마이저에 더 투명할 수 있다. 만약 그렇다면, 후속 블로그 포스트에서 확실히 다룰 예정이다.

### 단일 JOIN 쿼리의 경우

이 접근 방식은 중첩 컬렉션이 너무 크지 않을 때(즉, 중복 데이터가 너무 많지 않을 때) 허용 가능하게 동작한다. 컬렉션이 커지면 성능이 저하되는데, 이는 서버 측에서 더 많은 중복 데이터가 생성되어야 하고, 네트워크를 통해 전송되어야 하며, 클라이언트 측에서 더 많은 중복 제거가 수행되어야 하기 때문이다.

### 중첩 수준당 1개 쿼리의 경우

이 접근 방식은 N이 커질수록 가장 성능이 좋을 것으로 예상된다. 특히 ORM이 SQL 생성을 제어하는 경우에 효과적이지만, 사용자 쿼리 구문에 혼합하면 잘 동작하지 않는다.

## 벤치마크 결과

벤치마크는 JMH(Java Microbenchmark Harness)를 사용하여 수행되었으며, 쉬운 구성, 워밍업 반복을 통한 워밍업 패널티 제거, 이상치 효과를 처리하기 위한 통계 수집 등의 이점을 활용했다. 프레임워크는 `@Fork(value = 1)`, `@Warmup(iterations = 3, time = 3)`, `@Measurement(iterations = 7, time = 3)` 같은 어노테이션을 적용한다.

결과는 "ops/시간단위" 형식의 성능 측정값으로 표시된다. 실제 측정 단위(예: operations/second)를 상상하면 되지만, 초 단위가 아니라 비공개 시간 단위이다. 기준선은 가장 느린 실행을 1.0으로 표시하고, 더 빠른 결과는 그 기준의 배수로 표시된다.

무언가가 다른 것보다 1.5배 또는 3배 빠르다고 해서 반드시 더 좋다고 결론짓지 말아야 한다.

MySQL, Oracle, PostgreSQL, SQL Server 네 가지 RDBMS 플랫폼에서 테스트가 수행되었다.

### MySQL 결과

이중 중첩 (필터 적용, 단일 배우/영화):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 4413.48 |
| DN MULTISET JSON | 2524.96 |
| DN MULTISET JSONB | 2738.62 |
| DN N+1 | 265.37 |
| DN Two-Query | 2256.38 |

이중 중첩 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 266.27 |
| DN MULTISET JSON | 54.98 |
| DN MULTISET JSONB | 54.05 |
| DN N+1 | 1.00 (기준선) |
| DN Two-Query | 306.23 |

다중 자식 컬렉션 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 3058.68 |
| MCC MULTISET JSONB | 3179.18 |
| MCC N+1 | 1845.75 |
| MCC Two-Query | 2425.76 |

다중 자식 컬렉션 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 91.78 |
| MCC MULTISET JSONB | 92.48 |
| MCC N+1 | 2.84 |
| MCC Two-Query | 171.66 |

### Oracle 결과

이중 중첩 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 669.54 |
| DN MULTISET JSON | 419.13 |
| DN MULTISET JSONB | 432.40 |
| DN MULTISET XML | 351.42 |
| DN N+1 | 251.73 |
| DN Two-Query | 548.80 |

이중 중첩 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 15.59 |
| DN MULTISET JSON | 2.41 |
| DN MULTISET JSONB | 2.40 |
| DN MULTISET XML | 1.91 |
| DN N+1 | 1.00 (기준선) |
| DN Two-Query | 13.63 |

다중 자식 컬렉션 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 1217.79 |
| MCC MULTISET JSONB | 1214.07 |
| MCC MULTISET XML | 702.11 |
| MCC N+1 | 919.47 |
| MCC Two-Query | 1194.05 |

다중 자식 컬렉션 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 2.89 |
| MCC MULTISET JSONB | 3.00 |
| MCC MULTISET XML | 1.04 (가장 느림) |
| MCC N+1 | 1.52 |
| MCC Two-Query | 13.00 |

### PostgreSQL 결과

이중 중첩 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 4128.21 |
| DN MULTISET JSON | 3187.88 |
| DN MULTISET JSONB | 3064.69 |
| DN MULTISET XML | 1973.44 |
| DN N+1 | 267.15 |
| DN Two-Query | 2081.03 |

이중 중첩 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 275.95 |
| DN MULTISET JSON | 53.94 |
| DN MULTISET JSONB | 45.00 |
| DN MULTISET XML | 25.11 |
| DN N+1 | 1.00 (기준선) |
| DN Two-Query | 306.11 |

다중 자식 컬렉션 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 4710.47 |
| MCC MULTISET JSONB | 4391.78 |
| MCC MULTISET XML | 2740.73 |
| MCC N+1 | 1792.94 |
| MCC Two-Query | 2821.82 |

다중 자식 컬렉션 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 68.45 |
| MCC MULTISET JSONB | 58.59 |
| MCC MULTISET XML | 15.58 |
| MCC N+1 | 2.71 |
| MCC Two-Query | 163.03 |

### SQL Server 결과

이중 중첩 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 4081.85 |
| DN MULTISET JSON | 1243.17 |
| DN MULTISET JSONB | 1254.13 |
| DN MULTISET XML | 1077.23 |
| DN N+1 | 264.45 |
| DN Two-Query | 1608.92 |

이중 중첩 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| DN Join | 359.08 |
| DN MULTISET JSON | 8.41 |
| DN MULTISET JSONB | 8.32 |
| DN MULTISET XML | 7.24 |
| DN N+1 | 1.00 (기준선) |
| DN Two-Query | 376.02 |

다중 자식 컬렉션 (필터 적용):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 1735.23 |
| MCC MULTISET JSONB | 1736.01 |
| MCC MULTISET XML | 1339.68 |
| MCC N+1 | 1264.50 |
| MCC Two-Query | 1057.54 |

다중 자식 컬렉션 (전체 데이터셋):

| Benchmark | Score (ops/시간단위) |
|---|---|
| MCC MULTISET JSON | 7.90 |
| MCC MULTISET JSONB | 7.85 |
| MCC MULTISET XML | 5.06 |
| MCC N+1 | 1.77 |
| MCC Two-Query | 255.08 |

## 벤치마크 결론

모든 RDBMS가 유사한 결과를 보였다.

N+1 접근 방식은 일관되게 상당한 성능 패널티를 보였다. `filter = true`이고 다중 자식 컬렉션인 경우(부모 N이 1인 경우)에만 예외적이었다.

MULTISET은 특정 필터링된 경우에서 단일 쿼리 JOIN 기반 접근 방식보다 더 나은 성능을 보이기도 했다.

XML 에뮬레이션은 에뮬레이션 중에서 항상 가장 느렸다. PostgreSQL에서 JSON은 순수 텍스트 기반 형식이기 때문에 JSONB보다 빠른 성능을 보였다.

핵심 결론: MULTISET은 nested loop join이 최적인 경우에 언제든지 사용할 수 있다. hash join이나 merge join이 더 최적인 경우에는 단일 쿼리 JOIN 접근 방식이나 중첩 수준당 1개 쿼리 접근 방식이 더 나은 성능을 보이는 경향이 있다. 궁극적으로, 만능 해결책은 없다.

## 이 블로그 포스트에서 조사하지 않은 것들

서버에서의 직렬화 오버헤드가 있다. 일반적인 JDBC `ResultSet`은 바이너리 네트워크 프로토콜의 이점을 누리는 경향이 있다. JSON이나 XML을 사용하면 그 이점이 사라진다.

클라이언트 측에서도 마찬가지로, 중첩된 JSON이나 XML 문서를 역직렬화해야 한다.

VisualVM 스크린샷에 따르면 역직렬화는 실행 시간의 4.7%를 소비한 반면, 쿼리 실행은 92%를 소비했다. 약간의 오버헤드가 있지만, 실행 시간에 비하면 유의미하지 않다.

## 벤치마크 코드

벤치마크는 JMH를 사용하며, 쉬운 구성, 워밍업 반복을 통한 워밍업 패널티 제거, 이상치 효과를 처리하기 위한 통계 수집 등의 이점을 제공한다. 이 벤치마크를 재현하거나 자신의 필요에 맞게 적용하고자 한다면, 코드를 참고하면 된다.

벤치마크 결론은 특정 조건에 크게 의존한다: 데이터 분포, 벤더 선택, 시스템 부하, 쿼리 다양성. 결과를 보편적으로 외삽할 수 없다.
