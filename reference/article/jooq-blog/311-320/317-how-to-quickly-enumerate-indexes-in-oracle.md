# Oracle 11gR2에서 인덱스를 빠르게 열거하는 방법

> 원문: https://blog.jooq.org/how-to-quickly-enumerate-indexes-in-oracle/

때때로 테이블에 어떤 인덱스가 있는지 빠르게 확인해야 할 때가 있습니다. 다음은 Oracle에서 이를 수행하는 유용한 쿼리입니다:

```sql
SELECT
  i.index_name,
  listagg(c.column_name, ', ')
    WITHIN GROUP (ORDER BY c.column_position)
    AS columns
FROM all_indexes i
JOIN all_ind_columns c
  ON i.index_name = c.index_name
WHERE i.table_name = 'FILM_ACTOR'
GROUP BY i.index_name
```

`FILM_ACTOR`를 원하는 테이블 이름으로 바꾸면, 해당 테이블의 모든 인덱스와 각 인덱스를 구성하는 컬럼들을 확인할 수 있습니다.

출력 결과는 다음과 같습니다:

| INDEX_NAME | COLUMNS |
|------------|---------|
| IDX_FK_FILM_ACTOR_ACTOR | ACTOR_ID |
| IDX_FK_FILM_ACTOR_FILM | FILM_ID |
| PK_FILM_ACTOR | ACTOR_ID, FILM_ID |

`LISTAGG`는 Oracle 11gR2에서 추가된 ordered-set 집계 함수입니다. 이 함수는 그룹화된 데이터 내에서 문자열을 연결하는 데 사용됩니다. 기본 구문 패턴은 다음과 같습니다:

```sql
function(...) WITHIN GROUP (ORDER BY ...)
```

호환성 참고: `LISTAGG` 함수는 Oracle 11gR2 이상에서만 사용할 수 있습니다. Oracle 10g에서는 이 쿼리가 작동하지 않습니다.
