# SQL 오늘의 팁: SELECT COUNT(*)를 조심하라

> 원문: https://blog.jooq.org/sql-tip-of-the-day-be-wary-of-select-count/

2014년 8월 8일 | 작성자: lukaseder

## 문제점

이 글은 개발자들이 데이터베이스에서 특정 조건에 맞는 데이터가 존재하는지 확인할 때 `COUNT(*)`를 사용하는 흔한 성능 안티패턴을 다룹니다. 다음은 비효율적인 PL/SQL 예제입니다:

```sql
DECLARE
  v_var NUMBER(10);
BEGIN
  SELECT COUNT(*)
  INTO   v_var
  FROM   table1
  JOIN   table2 ON table1.t1_id = table2.t1_id
  JOIN   table3 ON table2.t2_id = table3.t2_id
  WHERE  some_predicate;

  IF (v_var = 0) THEN
    do_something
  ELSE
    do_something_else
  END IF;
END;
```

## 핵심 권장 사항

정확한 레코드 수가 필요하지 않다면, 전체 데이터셋을 카운트해서는 안 됩니다. 대신 레코드 존재 여부를 확인할 때는 `EXISTS` 서술자를 사용하세요.

## 최적화된 해결책

```sql
SELECT CASE WHEN EXISTS (
  SELECT 1
  FROM   table1
  JOIN   table2 ON table1.t1_id = table2.t1_id
  JOIN   table3 ON table2.t2_id = table3.t2_id
  WHERE  some_predicate
) THEN 1 ELSE 0 END
INTO   v_var
FROM   dual;
```

## 성능 증거

쿼리 1 (COUNT) 실행 계획:
- SORT AGGREGATE와 HASH JOIN 연산 포함
- 실제 행(A-Rows): 연산 전반에 걸쳐 4-6개

쿼리 2 (EXISTS) 실행 계획:
- 조기 종료 기능이 있는 NESTED LOOPS 사용
- 실제 행(A-Rows): 1개 (첫 번째 일치 항목을 찾으면 즉시 중단)

`EXISTS` 접근 방식은 첫 번째 일치하는 레코드를 찾는 즉시 종료되어 성능이 극적으로 향상됩니다.

## 다른 데이터베이스에서의 적용

이 글에서는 SQL Server에서도 참조된 비디오 데모를 통해 유사한 성능 향상을 확인할 수 있다고 언급합니다.

## 결론

핵심 원칙: "`COUNT(*)` 연산을 마주칠 때마다, 정확한 숫자를 아는 것이 정말 필요한지, 아니면 단순히 일치하는 레코드가 존재하는지만 알면 충분한지 자문해 보세요."

---

## 댓글

Steve Jakob (2014년 8월 15일):

Jakob은 논리적 오류를 지적합니다: 원래 예제는 `IF (v_var = 0)`으로 존재 여부가 아닌 정확히 0개의 레코드인지를 테스트합니다. 제안된 `EXISTS` 해결책은 여러 개의 일치하는 레코드가 있을 때 다르게 동작할 것입니다. 그는 조건이 `IF (v_var > 0)`이었다면 전제가 맞았을 것이라고 지적합니다.

lukaseder의 답변 (2014년 8월 18일):

저자는 이것이 복사-붙여넣기 오류였음을 인정하고 글을 수정하겠다고 답변합니다.
