# Java, SQL 또는 jOOQ에서 평면(Flat) 요소 리스트를 계층 구조로 변환하는 방법

> 원문: https://blog.jooq.org/how-to-turn-a-list-of-flat-elements-into-a-hierarchy-in-java-sql-or-jooq/

## 문제 상황

때때로 SQL 쿼리를 작성하여 계층적 데이터를 조회하고 싶을 때가 있습니다. 이런 데이터의 평면 표현은 다음과 같은 형태입니다:

| id | parent_id | label |
|----|-----------|-------|
| 1 | NULL | C: |
| 2 | 1 | eclipse |
| 3 | 2 | configuration |
| 4 | 2 | dropins |
| 5 | 2 | features |
| 7 | 2 | plugins |
| 8 | 2 | readme |
| 9 | 8 | readme_eclipse.html |
| 10 | 2 | src |
| 11 | 2 | eclipse.exe |

이것은 디렉토리 구조를 나타내는 `t_directory` 테이블로, `id`와 `parent_id` 컬럼을 통해 부모-자식 관계를 표현합니다. 하지만 우리가 실제로 원하는 것은 이 평면 데이터를 계층적 트리 구조로 변환하는 것입니다.

## SQL 접근법 (PostgreSQL)

PostgreSQL에서는 재귀적 CTE(Common Table Expression)를 사용하여 평면 데이터를 중첩된 JSON 계층 구조로 직접 변환할 수 있습니다:

```sql
WITH RECURSIVE
  d1 (id, parent_id, name) as (
    SELECT id, parent_id, label
    FROM t_directory
  ),
  d2 AS (
    SELECT d1.*, 0 AS level
    FROM d1
    WHERE parent_id IS NULL
    UNION ALL
    SELECT d1.*, d2.level + 1
    FROM d1
    JOIN d2 ON d2.id = d1.parent_id
  ),
  d3 AS (
    SELECT d2.*, null::jsonb children
    FROM d2
    WHERE level = (SELECT max(level) FROM d2)
    UNION (
      SELECT
        (branch_parent).*,
        jsonb_strip_nulls(
          jsonb_agg(branch_child - 'parent_id' - 'level'
            ORDER BY branch_child->>'name'
          ) FILTER (
            WHERE branch_child->>'parent_id' = (branch_parent).id::text
          )
        )
      FROM (
        SELECT
          branch_parent,
          to_jsonb(branch_child) AS branch_child
        FROM d2 branch_parent
        JOIN d3 branch_child
          ON branch_child.level = branch_parent.level + 1
      ) branch
      GROUP BY branch_parent
    )
  )
SELECT
  jsonb_pretty(jsonb_agg(to_jsonb(d3) - 'parent_id' - 'level')) AS tree
FROM d3
WHERE level = 0;
```

이 쿼리는 세 개의 CTE를 사용합니다:

- d1: `t_directory` 테이블에서 기본 데이터를 조회합니다.
- d2: 재귀적으로 계층 레벨을 계산합니다. 루트 노드(`parent_id IS NULL`)는 레벨 0에서 시작하여, 각 자식은 부모의 레벨 + 1을 갖습니다.
- d3: 가장 깊은 레벨부터 시작하여 위로 올라가면서, `jsonb_agg`를 사용해 자식 노드들을 부모에 집계합니다. `jsonb_strip_nulls`로 자식이 없는 경우 `children` 속성을 제거합니다.

결과는 다음과 같은 중첩된 JSON 트리를 생성합니다:

```json
[
    {
        "id": 1,
        "name": "C:",
        "children": [
            {
                "id": 2,
                "name": "eclipse",
                "children": [
                    {"id": 3, "name": "configuration"},
                    {"id": 4, "name": "dropins"},
                    {"id": 11, "name": "eclipse.exe"},
                    {"id": 5, "name": "features"},
                    {"id": 7, "name": "plugins"},
                    {
                        "id": 8,
                        "name": "readme",
                        "children": [
                            {"id": 9, "name": "readme_eclipse.html"}
                        ]
                    },
                    {"id": 10, "name": "src"}
                ]
            }
        ]
    }
]
```

하지만 이것은 꽤 복잡한 SQL 쿼리이며, 아마도 이 작업을 반드시 SQL로 할 필요는 없을 것입니다.

## jOOQ 3.19+ 솔루션

jOOQ 3.19부터 계층 구조 변환을 위한 내장 `Collector`가 제공됩니다. 클라이언트 측에서 다음과 같은 데이터 표현을 사용한다고 가정합니다:

```java
record File(int id, String name, List<File> children) {}
```

이 레코드를 사용하면 다음과 같이 간단하게 작성할 수 있습니다:

```java
List<File> result =
ctx.select(T_DIRECTORY.ID, T_DIRECTORY.PARENT_ID, T_DIRECTORY.LABEL)
   .from(T_DIRECTORY)
   .orderBy(T_DIRECTORY.ID)
   .collect(Records.intoHierarchy(
       r -> r.value1(),
       r -> r.value2(),
       r -> new File(r.value1(), r.value3(), new ArrayList<>()),
       (p, c) -> p.children().add(c)
   ));
```

`Records.intoHierarchy()` 메서드는 네 개의 인자를 받습니다:

1. keyMapper: 각 레코드에서 고유 키(id)를 추출하는 함수
2. parentKeyMapper: 각 레코드에서 부모 키(parent_id)를 추출하는 함수
3. nodeMapper: 레코드를 대상 객체(`File`)로 변환하는 함수
4. parentChildAppender: 부모 노드에 자식 노드를 추가하는 방법을 정의하는 BiConsumer

이 방식은 복잡한 SQL 없이 Java 코드에서 깔끔하고 타입 안전하게 계층 구조를 구축할 수 있게 해줍니다.

## 독립 실행형 Java 구현

jOOQ를 사용하지 않는 프로젝트에서도 동일한 패턴을 적용할 수 있습니다. 다음은 재사용 가능한 `Collector` 구현입니다:

```java
public static final <K, E, R extends Record>
Collector<R, ?, List<E>> intoHierarchy(
    Function<? super R, ? extends K> keyMapper,
    Function<? super R, ? extends K> parentKeyMapper,
    Function<? super R, ? extends E> nodeMapper,
    BiConsumer<? super E, ? super E> parentChildAppender
) {
    return Collectors.collectingAndThen(
        Collectors.toMap(keyMapper, r -> new SimpleImmutableEntry<R, E>(
            r, nodeMapper.apply(r)
        )),
        m -> {
            List<E> r = new ArrayList<>();
            m.forEach((k, v) -> {
                Entry<R, E> parent = m.get(
                    parentKeyMapper.apply(v.getKey())
                );
                if (parent != null)
                    parentChildAppender.accept(
                        parent.getValue(), v.getValue()
                    );
                else
                    r.add(v.getValue());
            });
            return r;
        }
    );
}
```

이 Collector의 동작 원리는 다음과 같습니다:

1. 먼저 `Collectors.toMap`을 사용하여 모든 요소를 키(id)를 기준으로 맵에 수집합니다. 맵의 값은 원본 레코드와 변환된 노드 객체의 쌍(`SimpleImmutableEntry`)입니다.
2. `collectingAndThen`의 후처리(finisher) 단계에서 맵을 순회하며:
   - 각 항목의 부모를 찾습니다 (`parentKeyMapper`를 통해).
   - 부모가 존재하면 `parentChildAppender`를 사용하여 자식을 부모에 추가합니다.
   - 부모가 없으면(즉, 루트 노드이면) 결과 리스트에 추가합니다.
3. 최종적으로 루트 노드들의 리스트를 반환합니다.

이 Collector는 jOOQ 레코드뿐만 아니라 모든 객체 스트림에서 작동합니다.

## 대안적 API 설계에 대하여

초기 버전에서는 타입 추론에 더 의존하는 좀 더 간결한 시그니처를 고려했습니다. 하지만 복잡한 jOOQ 쿼리에서 명시적인 타입 힌트 없이는 타입 추론이 실패하는 경우가 있어 이 설계는 기각되었습니다. 현재의 네 개 인자를 받는 방식이 더 명확하고 안정적입니다.

## MULTISET을 활용한 복잡한 예제

이 기법은 jOOQ의 `MULTISET` 연산자와 결합하면 더욱 강력해집니다. 예를 들어, 블로그 포스트와 스레드형 댓글이 있는 경우를 생각해 봅시다:

```java
record Post(int id, String title, List<Comment> comments) {}
record Comment(int id, String text, List<Comment> replies) {}

List<Post> result =
ctx.select(
       POST.ID,
       POST.TITLE,
       multiset(
           select(COMMENT.ID, COMMENT.PARENT_ID, COMMENT.TEXT)
           .from(COMMENT)
           .where(COMMENT.POST_ID.eq(POST.ID))
       ).convertFrom(r -> r.collect(intoHierarchy(
           r -> r.value1(),
           r -> r.value2(),
           r -> new Comment(r.value1(), r.value3(), new ArrayList<>()),
           (p, c) -> p.replies().add(c)
       )))
   )
   .from(POST)
   .orderBy(POST.ID)
   .fetch(mapping(Post::new));
```

이 예제에서는:

- 각 `Post`에 대해 `MULTISET` 서브쿼리로 해당 포스트의 모든 댓글을 조회합니다.
- `convertFrom`을 사용하여 평면 댓글 리스트를 `intoHierarchy` Collector로 계층적 댓글 트리로 변환합니다.
- 최종적으로 `mapping(Post::new)`을 통해 모든 것을 `Post` 레코드로 매핑합니다.

이렇게 하면 단일 쿼리로 포스트와 중첩된 댓글 계층 구조를 모두 타입 안전하게 가져올 수 있습니다.

## 결론

`Collector`는 JDK에서 매우 과소평가된 API입니다. jOOQ의 함수형 라이브러리인 jOOL(jOOλ)의 `Agg` 클래스에서는 다음과 같은 추가적인 Collector들을 제공합니다:

- 비트 연산(Bitwise) 집계
- 통계(Statistical) 집계

데이터를 계층 구조로 수집하는 것은 사실 그렇게 특별한 일이 아닙니다. 핵심은 `Collector` API의 유연성을 활용하면 복잡한 SQL 없이도 애플리케이션 코드에서 깔끔하고 타입 안전한 방식으로 다양한 데이터 변환 작업을 수행할 수 있다는 것입니다.
