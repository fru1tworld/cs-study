# CROSS JOIN, 드물게 사용되는 연산의 좋은 예

> 원문: https://blog.jooq.org/cross-join-a-nice-example-for-a-rarely-used-operation/

95%의 경우, 카테시안 곱(cartesian product)은 실수로 인한 cross join 연산에서 비롯되며, 데이터베이스에 불필요하게 높은 부하를 유발합니다. 결과가 틀리지 않을 수도 있는데, 이는 누군가 원치 않는 중복을 제거하기 위해 `UNION`이나 `DISTINCT` 키워드를 적용했을 수 있기 때문입니다. 하지만 카테시안 곱이 의도적이고, 심지어 바람직하기까지 한 5%의 SQL 쿼리가 존재합니다.

그런 쿼리의 좋은 예시가 있습니다.

다음 두 개의 테이블이 있다고 가정합시다 (단순화를 위해 제약 조건은 생략합니다):

```sql
CREATE TABLE content_hits (
  content_id    INT,
  subscriber_id INT
);

CREATE TABLE content_tags (
  content_id INT,
  tag_id     INT
);
```

위 테이블들은 블로그 콘텐츠를 모델링하는데, `content_hits`는 특정 블로그 글에 얼마나 많은 구독자가 관련되어 있는지를 보여주고, `content_tags`는 특정 블로그 글에 어떤 태그가 적용되어 있는지를 보여줍니다.

이제 특정 구독자/태그 조합이 실제로 몇 번 나타났는지에 대한 통계를 얻고 싶다고 합시다. 0건인 조합을 포함한 완전한 조합 목록을 원한다면, 단순히 `subscriber_id`, `tag_id`로 조인하고 그룹화하는 것만으로는 안 됩니다.

그래서 cross join이 구원투수로 나섭니다!

```sql
SELECT combinations.tag_id,
       combinations.subscriber_id,
       (SELECT count(*)
        FROM content_hits AS h
        JOIN content_tags AS t ON h.content_id = t.content_id
        WHERE h.subscriber_id = combinations.subscriber_id
        AND t.tag_id = combinations.tag_id) AS cnt
FROM (
  SELECT DISTINCT tag_id, subscriber_id
  FROM content_tags
  CROSS JOIN content_hits
) AS combinations
```

상관 서브쿼리를 사용하는 대신, (데이터베이스에 따라) 성능상의 이유로 첫 번째 서브쿼리를 "combinations" 관계에 left outer join한 다음 `subscriber_id`, `tag_id`로 그룹화하고 각 그룹에 대해 `count(content_id)`를 수행하는 것이 더 나을 수 있으며, 그 결과 다음과 같은 쿼리가 됩니다:

```sql
SELECT combinations.tag_id,
       combinations.subscriber_id,
       count(content_relation.content_id)
FROM (
  SELECT DISTINCT tag_id, subscriber_id
  FROM content_tags
  CROSS JOIN content_hits
) AS combinations
LEFT OUTER JOIN (
  SELECT h.content_id,
         h.subscriber_id,
         t.tag_id
  FROM content_hits AS h
  JOIN content_tags AS t ON h.content_id = t.content_id
) AS content_relation
ON  combinations.tag_id = content_relation.tag_id
AND combinations.subscriber_id = content_relation.subscriber_id
GROUP BY combinations.tag_id,
         combinations.subscriber_id
```

원본 Stack Overflow 질문은 여기에서 볼 수 있습니다: https://stackoverflow.com/q/9924433/521799
