# Oracle에서 그룹별 Top 1 값 가져오기

> 원문: [Getting Top 1 Values Per Group in Oracle](https://blog.jooq.org/getting-top-1-values-per-group-in-oracle/)

이전에 이 블로그에서 카테고리별 top 1 또는 top n을 가져오는 일반적인 방법에 대해 글을 쓴 적이 있습니다.

해당 게시물에서 Oracle 전용 버전은 난해한 `KEEP` 구문을 사용했습니다:

```sql
SELECT
  max(actor_id)   KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(first_name) KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(last_name)  KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id),
  max(c)          KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id)
FROM (
  SELECT actor_id, first_name, last_name, count(film_id) c
  FROM actor
  LEFT JOIN film_actor USING (actor_id)
  GROUP BY actor_id, first_name, last_name
) t;
```

이 구문은 처음 보면 읽기가 다소 어렵습니다. 그룹별 첫 번째 값을 가져오고 싶다는 것을 복잡하게 표현한 것이라고 생각하면 됩니다. 다음과 같은 가상의 구문이 훨씬 더 깔끔할 것입니다:

```sql
SELECT
  FIRST(actor_id ORDER BY c DESC, actor_id),
  FIRST(first_name ORDER BY c DESC, actor_id),
  FIRST(last_name ORDER BY c DESC, actor_id),
  FIRST(c ORDER BY c DESC, actor_id)
FROM (...) t;
```

즉, 그룹 내용을 `ORDER BY` 절로 정렬했을 때 그룹별로 표현식의 `FIRST`(첫 번째) 값을 가져오는 것입니다.

> Oracle의 구문은 정렬이 비결정적(non-deterministic)일 수 있다는 점을 고려합니다. `ORDER BY` 절에 고유한 값을 포함하지 않으면 동점(tie)이 발생할 수 있습니다. 이 경우 동점인 모든 값을 집계할 수 있는데, 예를 들어 비즈니스 케이스에서 의미가 있다면 `AVG()`를 사용할 수 있습니다. 동점을 신경 쓰지 않거나 동점이 없음을 보장할 수 있다면, `MAX()`가 괜찮은 우회 방법이며, 21c부터는 `ANY_VALUE()`를 사용할 수 있습니다.

이렇게 그룹별로 여러 컬럼을 프로젝션할 때는 상당히 많은 반복이 발생합니다. 윈도우 함수에는 공통 윈도우 사양에 이름을 붙여 재사용할 수 있는 [`WINDOW` 절](https://blog.jooq.org/how-to-reduce-syntactic-overhead-using-the-sql-window-clause/)이 있습니다. 하지만 `GROUP BY`에는 그러한 기능이 없는데, 아마도 이것이 유용한 경우가 드물기 때문일 것입니다.

하지만 다행히 Oracle에는 다음이 있습니다:

- `OBJECT` 타입 - 이름이 지정된(nominally typed) 행 값 표현식입니다.
- `ANY_VALUE` - 그룹별로 임의의 값을 생성하는 집계 함수로, Oracle 21c에서 추가되었습니다.

이 두 가지 유틸리티를 사용하면 다음과 같이 할 수 있습니다:

```sql
CREATE TYPE o AS OBJECT (
  actor_id NUMBER(18),
  first_name VARCHAR2(50),
  last_name VARCHAR2(50),
  c NUMBER(18)
);
```

그리고 이제:

```sql
SELECT
  ANY_VALUE(o(actor_id, first_name, last_name, c))
    KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id)
FROM (...) t;
```

참고로, 이전 Oracle 버전에서는 다음 오류 메시지를 우회할 수 있다면 `MAX()`를 사용하는 것도 가능합니다:

> ORA-22950: cannot order objects without MAP or ORDER method

물론 이것은 우회 방법일 뿐입니다. 집계가 필요한 모든 경우에 대해 이름이 지정된 `OBJECT` 타입을 관리하는 것은 번거롭습니다. 타입 안전성이 필요하지 않다면, 대신 JSON을 사용할 수도 있습니다:

```sql
SELECT
  ANY_VALUE(JSON_OBJECT(actor_id, first_name, last_name, c))
    KEEP (DENSE_RANK FIRST ORDER BY c DESC, actor_id)
FROM (...) t;
```
