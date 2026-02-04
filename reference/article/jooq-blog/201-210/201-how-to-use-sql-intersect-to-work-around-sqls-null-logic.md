# SQL INTERSECT를 사용하여 SQL의 NULL 로직 문제를 우회하는 방법

> 원문: https://blog.jooq.org/how-to-use-sql-intersect-to-work-around-sqls-null-logic/

## 문제점

SQL에서 행 값 표현식(row value expression, 튜플)과 함께 IN 조건자(predicate)를 사용할 때 NULL 처리에 어려움이 있습니다. `('a', 'b', NULL)` 또는 `('a', NULL, 'c')`와 일치하는 행을 찾으려고 할 때, 표준 IN 구문을 사용하면 쿼리가 실패합니다. 왜냐하면 "SQL에서 NULL과 같은 것은 아무것도 없기 때문입니다 (NULL 자신조차도 NULL과 같지 않습니다)."

## 해결 방법

### "평범한" 접근 방식

NULL 값을 'yolo'와 같이 절대 존재할 수 없는 문자열 값으로 대체하는 방법입니다:

```sql
WHERE (a, NVL(b, 'yolo'), NVL(c, 'yolo'))
  IN (('a', 'b', 'yolo'), ('a', 'yolo', 'c'))
```

### "힙스터" 접근 방식

INTERSECT 집합 연산을 활용하는 방법입니다. INTERSECT는 두 NULL 값을 "NOT DISTINCT"(구별되지 않음)로 취급합니다:

```sql
WHERE EXISTS (
  SELECT a, b, c FROM dual
  INTERSECT (
    SELECT 'a', 'b', null FROM dual
    UNION ALL
    SELECT 'a', null, 'c' FROM dual
  )
)
```

INTERSECT 방법은 번거로운 NULL 처리를 피하면서, 집합 연산에서 SQL의 일관성 없지만 유용한 3값 로직(three-valued logic) 동작을 활용합니다.

## 결론

SQL의 NULL 처리는 종종 예상치 못한 동작을 일으킬 수 있습니다. IN 조건자와 튜플을 사용할 때 NULL이 포함되어 있다면, INTERSECT를 활용한 EXISTS 패턴이 더 깔끔한 해결책이 될 수 있습니다. 이 방법은 NULL 값에 대한 특별한 처리 없이도 원하는 결과를 얻을 수 있게 해줍니다.
