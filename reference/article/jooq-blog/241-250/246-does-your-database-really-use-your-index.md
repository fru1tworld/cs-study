> 원문: https://blog.jooq.org/does-your-database-really-use-your-index/

# 데이터베이스가 정말로 인덱스를 사용하고 있을까?

## 소개

적절한 인덱스를 추가하는 것은 쿼리 성능에 매우 중요하지만, 시간이 지나면서 시스템이 발전함에 따라 많은 인덱스가 불필요해집니다. 일부 중복 사례는 명확하지만, 다른 경우에는 조사가 필요합니다. 예를 들어:

- 데이터베이스가 중첩 루프 조인(nested loop join)에서 해시 조인(hash join)으로 전환되면서 외래 키의 인덱스가 사용되지 않게 될 수 있습니다
- 쿼리 패턴이 완전히 변경되어 이전에 유용했던 인덱스가 더 이상 필요하지 않을 수 있습니다
- 데이터 분포가 변경되면 인덱스가 비효율적이 될 수 있습니다 (예: "고객들이 갑자기 모두 Smith라는 이름을 가지게 된" 경우)

## 사용되지 않는 인덱스 찾기

프로덕션 접근 권한이 있는 Oracle 데이터베이스 사용자의 경우, 커서 캐시(cursor cache)를 사용하는 진단 방법이 있습니다. 제안된 쿼리는 `v$sql`과 `v$sql_plan` 테이블을 검사합니다:

```sql
SELECT sql_fulltext
FROM v$sql
WHERE sql_id IN (
  SELECT sql_id
  FROM v$sql_plan
  WHERE (object_owner, object_name)
     = (('OWNER', 'IDX_CUSTOMER_FIRST_NAME'))
)
ORDER BY sql_text;
```

이 쿼리는 특정 인덱스를 현재 사용하고 있는 SQL 문을 식별합니다. 그러나 결과가 인덱스가 전혀 사용되지 않는다는 것을 보장하지는 않습니다 - "1년에 한 번 발생하는 드문 쿼리"는 캐시에서 제거될 수 있기 때문입니다.

## 불필요한 인덱스 발견하기

잠재적으로 사용되지 않는 인덱스를 찾으려면:

```sql
SELECT owner, index_name
FROM all_indexes
WHERE owner = 'OWNER'
AND (owner, index_name) NOT IN (
  SELECT object_owner, object_name
  FROM v$sql_plan
  WHERE object_owner IS NOT NULL
  AND object_name IS NOT NULL
)
ORDER BY 1, 2
```

이 쿼리는 최근 실행 계획(execution plan)에 없는 인덱스를 보여주지만, 여전히 가끔 사용될 수 있습니다.

## Oracle 12cR2 업데이트

Oracle 12c에서는 `DBA_INDEX_USAGE` 기능이 도입되어 네이티브 인덱스 사용 모니터링 기능을 제공합니다 (댓글 작성자 Dan McGhan이 언급).

## 핵심 요점

사용되지 않는 인덱스는 쓰기 작업의 오버헤드를 줄이기 위해 제거해야 하지만, 삭제 전에 신중한 모니터링이 필수적입니다.
