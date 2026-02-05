# PostgreSQL 구문은 그 힘에 의해서만 능가되는 미스터리다

> 원문: https://blog.jooq.org/postgresql-syntax-is-a-mystery-only-exceeded-by-its-power/

PostgreSQL 공식 매뉴얼에서 발견한 이 놀라운 예제를 살펴보세요:

```sql
WITH upd AS (
  UPDATE employees
  SET sales_count = sales_count + 1
  WHERE id = (
    SELECT sales_person
    FROM accounts
    WHERE name = 'Acme Corporation'
  )
  RETURNING *
)
INSERT INTO employees_log
SELECT *, current_timestamp
FROM upd;
```

단일 구문으로 한 테이블을 업데이트하고, 모든 생성된 값을 포함한 업데이트된 레코드를 다른 테이블에 삽입하기 위한 입력으로 사용할 수 있습니다.

그러면서도 PostgreSQL은 아직 SQL:2003 MERGE 구문을 구현하지 않았습니다.

미친 데이터베이스입니다.
