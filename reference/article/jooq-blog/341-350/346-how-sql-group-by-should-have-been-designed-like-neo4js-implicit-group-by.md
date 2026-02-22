# SQL GROUP BY가 어떻게 설계되었어야 했는가 - Neo4j의 암묵적 GROUP BY처럼

> 원문: https://blog.jooq.org/how-sql-group-by-should-have-been-designed-like-neo4js-implicit-group-by/

저는 최근 `GROUP BY` 절을 추가하면 쿼리가 암묵적으로 어떻게 변환되는지에 대해 블로그에 작성했습니다. 특히 다음과 같은 암묵적인 변환에 대해 설명했습니다:

- "`GROUP BY` 절에 참조된 컬럼 표현식이나, 다른 컬럼 표현식의 집계만 `SELECT` 절에 나타날 수 있다"
- 명시적인 `GROUP BY` 없이 집계를 수행하면 "총계(grand total)" `GROUP BY ()` 절이 암묵적으로 적용된다
- MySQL과 같은 일부 데이터베이스는 `SELECT` 절에 임의의 컬럼 표현식을 허용한다

## Cypher가 다른 방식을 제공합니다

Neo4j의 쿼리 언어인 Cypher에 대해 알게 되었습니다. Cypher를 SQL로 (대략적으로) 매핑하면:

- Cypher `MATCH`는 SQL `FROM`에 해당합니다
- Cypher `WHERE`는 SQL `WHERE`에 해당합니다
- Cypher `RETURN`은 SQL `SELECT`에 해당합니다

Neo4j 매뉴얼의 예제들을 살펴보겠습니다. 첫 번째로, 암묵적 총계 집계 예제입니다:

```
MATCH (me:Person)-->(friend:Person)
                 -->(friend_of_friend:Person)
WHERE me.name = 'A'
RETURN count(DISTINCT friend_of_friend),
       count(friend_of_friend)
```

이 쿼리는 SQL과 동일하게 동작합니다: 집계 함수만 `RETURN`/`SELECT` 절에 있으므로, 결과는 단일 행으로 집계됩니다.

두 번째 예제, 이것이 흥미로운 부분입니다:

```
MATCH (n { name: 'A' })-->(x)
RETURN n, count(*)
```

Cypher에서는 `n`을 참조하는 동시에 `count(*)`를 참조하면 `n`에 대한 암묵적 `GROUP BY`가 생성됩니다 (더 구체적으로는 `GROUP BY n.id`). Cypher에서는 `GROUP BY`를 명시적으로 작성할 필요가 없습니다. 나머지 모든 컬럼들이 자동으로 `GROUP BY`를 형성합니다!

## SQL에도 이 기능이 있었으면 좋겠습니다

진지하게 말씀드리자면, 저는 SQL이 처음부터 이런 방식으로 설계되었기를 바랍니다. SQL을 설계한 사람들은 `GROUP BY` 절을 명시적으로 요구하기 위해 열심히 노력했지만, 왜 그랬을까요? 경험 많은 SQL 개발자에게는 당연해 보입니다. 집계 쿼리에서 집계되지 않는 컬럼은 `GROUP BY`에 있어야 합니다.

명시적인 `GROUP BY`가 반드시 필요하지 않다면 어떻게 동작해야 하는지 다음과 같이 제안합니다:

- `SELECT`에 집계 함수가 없으면, 암묵적인 `GROUP BY` 절이 없어야 합니다
- `SELECT`에 1-N개의 집계 함수가 있으면, 나머지 컬럼들로 구성된 암묵적인 `GROUP BY` 절이 있어야 합니다
- `SELECT`에 집계 함수만 있으면, "총계" `GROUP BY ()`가 적용되어야 합니다
- 명시적인 `GROUP BY` 절은 항상 암묵적인 `GROUP BY` 절보다 우선해야 합니다

마지막 규칙이 중요합니다. 때로는 표현식이 집계 함수인지 아닌지가 항상 명확하지 않을 수 있습니다. 또한 특정 컬럼을 그룹화하면서 다른 컬럼에 대해 "임의의" 값을 선택하고 싶을 수도 있습니다 - 예를 들어, MySQL의 비표준 "아무 값(any value)" 집계 방식처럼 말입니다.

## 표준화 요청

SQL 표준 위원회와 PostgreSQL과 같은 데이터베이스에 대한 제 희망 사항 목록에 이것을 추가합니다: 선택적인 `GROUP BY` 절을 지원해 주세요. 지금 당장의 이전 버전 호환성 문제 없이 도입할 수 있는 방법이 있습니다 - 예를 들어 설정 플래그를 통해서요. 이것은 쿼리를 더 직관적으로 만들고 보일러플레이트 코드를 줄여줄 것입니다.

## 결론

Neo4j의 Cypher 쿼리 언어가 보여주듯이, 암묵적 그룹화는 쿼리를 더 간결하고 읽기 쉽게 만들 수 있습니다. SQL이 처음 설계될 때 이러한 접근 방식을 채택했다면, 오늘날 우리는 더 우아한 집계 쿼리를 작성할 수 있었을 것입니다. 현재로서는 이것이 미래 SQL 표준에 대한 희망 사항으로 남아있지만, 데이터베이스 벤더들이 이 기능을 선택적으로 구현해 준다면 개발자 경험이 크게 향상될 것입니다.
