> 원문: https://blog.jooq.org/how-to-find-redundant-indexes-in-sql/

# SQL에서 중복 인덱스(Redundant Index)를 찾는 방법

## 핵심 개념

두 개의 인덱스가 있을 때, 하나의 인덱스가 다른 인덱스의 모든 컬럼을 접두사(prefix)로 포함하고 있다면 그 인덱스는 중복됩니다. 예를 들어, `(last_name)` 컬럼에 대한 인덱스가 있고, `(last_name, first_name)` 컬럼에 대한 또 다른 인덱스가 있다면, 첫 번째 인덱스는 중복입니다. `last_name`만으로 필터링하는 쿼리는 복합 인덱스(composite index)를 사용할 수 있기 때문입니다.

## Oracle 쿼리

```sql
WITH indexes AS (
  SELECT
    i.owner,
    i.index_name,
    i.table_name,
    listagg(c.column_name, ', ')
      WITHIN GROUP (ORDER BY c.column_position)
      AS columns
  FROM all_indexes i
  JOIN all_ind_columns c
    ON i.owner = c.index_owner
    AND i.index_name = c.index_name
  GROUP BY i.owner, i.table_name, i.index_name, i.leaf_blocks
)
SELECT
  i.owner,
  i.table_name,
  i.index_name AS "Deletion candidate index",
  i.columns AS "Deletion candidate columns",
  j.index_name AS "Existing index",
  j.columns AS "Existing columns"
FROM indexes i
JOIN indexes j
  ON i.owner = j.owner
  AND i.table_name = j.table_name
  AND j.columns LIKE i.columns || ',%'
```

## PostgreSQL 쿼리

```sql
WITH indexes AS (
  SELECT
    tnsp.nspname AS schema_name,
    trel.relname AS table_name,
    irel.relname AS index_name,
    string_agg(a.attname, ', ' ORDER BY c.ordinality) AS columns
  FROM pg_index AS i
  JOIN pg_class AS trel ON trel.oid = i.indrelid
  JOIN pg_namespace AS tnsp ON trel.relnamespace = tnsp.oid
  JOIN pg_class AS irel ON irel.oid = i.indexrelid
  JOIN pg_attribute AS a ON trel.oid = a.attrelid
  JOIN LATERAL unnest(i.indkey)
    WITH ORDINALITY AS c(colnum, ordinality)
      ON a.attnum = c.colnum
  GROUP BY i, tnsp.nspname, trel.relname, irel.relname
)
SELECT
  i.table_name,
  i.index_name AS "Deletion candidate index",
  i.columns AS "Deletion candidate columns",
  j.index_name AS "Existing index",
  j.columns AS "Existing columns"
FROM indexes i
JOIN indexes j
  ON i.schema_name = j.schema_name
  AND i.table_name = j.table_name
  AND j.columns LIKE i.columns || ',%'
```

## SQL Server 쿼리

```sql
WITH
  i AS (
    SELECT
      s.name AS schema_name,
      t.name AS table_name,
      i.name AS index_name,
      c.name AS column_name,
      ic.key_ordinal AS key_ordinal
    FROM sys.indexes i
    JOIN sys.index_columns ic
      ON i.object_id = ic.object_id
      AND i.index_id = ic.index_id
    JOIN sys.columns c
      ON ic.object_id = c.object_id
      AND ic.column_id = c.column_id
    JOIN sys.tables t
      ON i.object_id = t.object_id
    JOIN sys.schemas s
      ON t.schema_id = s.schema_id
  ),
  indexes AS (
    SELECT
      schema_name,
      table_name,
      index_name,
      STUFF((
        SELECT ',' + j.column_name
        FROM i j
        WHERE i.table_name = j.table_name
        AND i.index_name = j.index_name
        ORDER BY j.key_ordinal
        FOR XML PATH('')
      ), 1, 1, '') columns
    FROM i
    GROUP BY schema_name, table_name, index_name
  )
SELECT
  i.schema_name,
  i.table_name,
  i.index_name AS "Deletion candidate index",
  i.columns AS "Deletion candidate columns",
  j.index_name AS "Existing index",
  j.columns AS "Existing columns"
FROM indexes i
JOIN indexes j
  ON i.schema_name = j.schema_name
  AND i.table_name = j.table_name
  AND (j.columns LIKE i.columns + '%'
       OR (j.columns = i.columns
           AND j.index_name != i.index_name))
  AND i.index_name != j.index_name
```

## 중요 고려사항

PostgreSQL과 SQL Server의 부분 인덱스(Partial Index)는 신중한 평가가 필요합니다. 이러한 필터링된 인덱스는 결과에 나타날 수 있지만, 중복으로 보임에도 불구하고 특정 목적을 위해 사용될 수 있습니다. 인덱스를 삭제하기 전에 수동 검증이 필요합니다.

### 중복 인덱스 삭제 시 장단점

장점:
- 삽입(INSERT) 작업 속도 향상
- 저장 공간 오버헤드 감소

단점:
- 일부 쿼리의 성능이 5-13% 정도 저하될 수 있음

결론: 인덱스를 삭제하기 전에 해당 인덱스가 정말로 필요 없는지 신중하게 검토해야 합니다.
