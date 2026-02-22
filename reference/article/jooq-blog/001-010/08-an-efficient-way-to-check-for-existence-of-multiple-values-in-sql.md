# SQL에서 여러 값의 존재 여부를 효율적으로 확인하는 방법

> 원문: [An Efficient Way to Check for Existence of Multiple Values in SQL](https://blog.jooq.org/an-efficient-way-to-check-for-existence-of-multiple-values-in-sql/)

이전 블로그 글에서, SQL에서 값의 존재 여부를 확인할 때 `COUNT(*)` 대신 `EXISTS`를 사용하라고 권장한 바 있습니다.

즉, Sakila 데이터베이스에서 WAHLBERG라는 성을 가진 배우가 영화에 출연했는지 확인할 때, 아래와 같이 하는 대신:

```sql
SELECT count(*)
FROM actor a
JOIN film_actor fa USING (actor_id)
WHERE a.last_name = 'WAHLBERG'
```

이렇게 작성하세요:

```sql
SELECT EXISTS (
  SELECT 1 FROM actor a
  JOIN film_actor fa USING (actor_id)
  WHERE a.last_name = 'WAHLBERG'
)
```

(사용하는 SQL 방언에 따라 `FROM DUAL` 절이 필요하거나, `BOOLEAN` 타입이 지원되지 않는 경우 `CASE` 표현식이 필요할 수 있습니다.)

## 여러 행 확인하기

그런데 최소 `2`개(또는 `N`개) 이상의 행이 존재하는지 확인하고 싶다면 어떻게 해야 할까요? 이 경우에는 `EXISTS`를 사용할 수 없고 `COUNT(*)`를 사용해야 합니다. 하지만 일치하는 _모든_ 행을 세는 대신, `LIMIT` 절도 함께 추가하면 어떨까요? 따라서 WAHLBERG라는 성을 가진 배우가 최소 2편 이상의 영화에 출연했는지 확인하려면, 아래와 같이 하는 대신:

```sql
SELECT (
  SELECT count(*)
  FROM actor a
  JOIN film_actor fa USING (actor_id)
  WHERE a.last_name = 'WAHLBERG'
) >= 2
```

이렇게 작성하세요:

```sql
SELECT (
  SELECT count(*)
  FROM (
    SELECT *
    FROM actor a
    JOIN film_actor fa USING (actor_id)
    WHERE a.last_name = 'WAHLBERG'
    LIMIT 2
  ) t
) >= 2
```

다시 말해:

1. 파생 테이블에서 `LIMIT 2`를 사용하여 조인 쿼리를 실행합니다
2. 그 파생 테이블에서 행을 `COUNT(*)`합니다 (최대 2개)
3. 마지막으로 카운트가 충분히 높은지 확인합니다

## 차이가 있을까?

원칙적으로, 옵티마이저가 이것을 스스로 파악할 수 있었을 것입니다. 특히 `COUNT(*)` 값과 비교하는 데 상수를 사용했기 때문입니다. 하지만 실제로 이 변환이 적용되었을까요?

다양한 RDBMS에서 실행 계획을 확인하고 쿼리를 벤치마크해 봅시다.

### PostgreSQL 15

`LIMIT` 없이

```
Result  (cost=14.70..14.71 rows=1 width=1) (actual time=0.039..0.039 rows=1 loops=1)
  InitPlan 1 (returns $1)
    ->  Aggregate  (cost=14.69..14.70 rows=1 width=8) (actual time=0.037..0.037 rows=1 loops=1)
          ->  Nested Loop  (cost=0.28..14.55 rows=55 width=0) (actual time=0.009..0.032 rows=56 loops=1)
                ->  Seq Scan on actor a  (cost=0.00..4.50 rows=2 width=4) (actual time=0.006..0.018 rows=2 loops=1)
                      Filter: ((last_name)::text = 'WAHLBERG'::text)
                      Rows Removed by Filter: 198
                ->  Index Only Scan using film_actor_pkey on film_actor fa  (cost=0.28..4.75 rows=27 width=4) (actual time=0.003..0.005 rows=28 loops=2)
                      Index Cond: (actor_id = a.actor_id)
                      Heap Fetches: 0
```

`LIMIT` 사용

```
Result  (cost=0.84..0.85 rows=1 width=1) (actual time=0.023..0.024 rows=1 loops=1)
  InitPlan 1 (returns $1)
    ->  Aggregate  (cost=0.83..0.84 rows=1 width=8) (actual time=0.021..0.022 rows=1 loops=1)
          ->  Limit  (cost=0.28..0.80 rows=2 width=240) (actual time=0.016..0.018 rows=2 loops=1)
                ->  Nested Loop  (cost=0.28..14.55 rows=55 width=240) (actual time=0.015..0.016 rows=2 loops=1)
                      ->  Seq Scan on actor a  (cost=0.00..4.50 rows=2 width=4) (actual time=0.008..0.008 rows=1 loops=1)
                            Filter: ((last_name)::text = 'WAHLBERG'::text)
                            Rows Removed by Filter: 1
                      ->  Index Only Scan using film_actor_pkey on film_actor fa  (cost=0.28..4.75 rows=27 width=4) (actual time=0.005..0.005 rows=2 loops=1)
                            Index Cond: (actor_id = a.actor_id)
                            Heap Fetches: 0
```

차이점을 이해하려면 다음 행들에 주목하세요:

이전:

```
Nested Loop  (cost=0.28..14.55 rows=55 width=0) (actual time=0.009..0.032 rows=56 loops=1)
```

이후:

```
Nested Loop  (cost=0.28..14.55 rows=55 width=240) (actual time=0.015..0.016 rows=2 loops=1)
```

두 경우 모두 조인에 의해 생성되는 예상 행 수는 55입니다 (즉, 통계에 따르면 모든 WAHLBERG가 총 55편의 영화에 출연한 것으로 예상됩니다).

하지만 두 번째 실행에서는 _실제 행(actual rows)_ 값이 훨씬 낮습니다. 위의 `LIMIT` 때문에 2개의 행만 필요하면 연산 실행을 중단할 수 있었기 때문입니다.

벤치마크 결과:

동일한 인스턴스에서 절차적 언어를 사용하여 RDBMS 내부에서 직접 두 쿼리를 여러 번 실행하는 (네트워크 지연 등을 피하기 위해) 우리가 권장하는 SQL 벤치마킹 기법을 사용하면 (이 경우 5회 실행 x 2000번 실행), 다음과 같은 결과를 얻습니다:

```
RUN 1, Statement 1: 2.61927
RUN 1, Statement 2: 1.01506
RUN 2, Statement 1: 2.47193
RUN 2, Statement 2: 1.00614
RUN 3, Statement 1: 2.63533
RUN 3, Statement 2: 1.14282
RUN 4, Statement 1: 2.55228
RUN 4, Statement 2: 1.00000 -- 가장 빠른 실행이 1
RUN 5, Statement 1: 2.53801
RUN 5, Statement 2: 1.02363
```

가장 빠른 실행은 `1` 단위의 시간이고, 더 느린 실행은 그 시간의 배수로 실행됩니다. 완전한 `COUNT(*)` 쿼리는 `LIMIT` 쿼리보다 일관되고 상당히 느립니다.

실행 계획과 벤치마크 결과 모두 스스로를 대변합니다.

### Oracle 23c

Oracle 23c에서는 드디어 `BOOLEAN` 타입을 사용할 수 있고 `DUAL`을 생략할 수 있게 되었습니다, 만세!

`FETCH FIRST` 없이:

```
SQL_ID  40yy0tskvs1zw, child number 0
-------------------------------------
SELECT /*+GATHER_PLAN_STATISTICS*/ (           SELECT count(*)
 FROM actor a           JOIN film_actor fa USING (actor_id)
WHERE a.last_name = 'WAHLBERG'         ) >= 2

Plan hash value: 2539243977

---------------------------------------------------------------------------------------------------------------------------
| Id  | Operation                             | Name                    | Starts | E-Rows | A-Rows |   A-Time   | Buffers |
---------------------------------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                      |                         |      1 |        |      1 |00:00:00.01 |       0 |
|   1 |  SORT AGGREGATE                       |                         |      1 |      1 |      1 |00:00:00.01 |       6 |
|   2 |   NESTED LOOPS                        |                         |      1 |     55 |     56 |00:00:00.01 |       6 |
|   3 |    TABLE ACCESS BY INDEX ROWID BATCHED| ACTOR                   |      1 |      2 |      2 |00:00:00.01 |       2 |
|*  4 |     INDEX RANGE SCAN                  | IDX_ACTOR_LAST_NAME     |      1 |      2 |      2 |00:00:00.01 |       1 |
|*  5 |    INDEX RANGE SCAN                   | IDX_FK_FILM_ACTOR_ACTOR |      2 |     27 |     56 |00:00:00.01 |       4 |
|   6 |  FAST DUAL                            |                         |      1 |      1 |      1 |00:00:00.01 |       0 |
---------------------------------------------------------------------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   4 - access("A"."LAST_NAME"='WAHLBERG')
   5 - access("A"."ACTOR_ID"="FA"."ACTOR_ID")
```

`FETCH FIRST` 사용:

```
SQL_ID  f88t1r0avnr7b, child number 0
-------------------------------------
SELECT /*+GATHER_PLAN_STATISTICS*/(           SELECT count(*)
from (             select *             FROM actor a             JOIN
film_actor fa USING (actor_id)             WHERE a.last_name =
'WAHLBERG'             FETCH FIRST 2 ROWS ONLY           ) t         )
>= 2

Plan hash value: 4019277616

------------------------------------------------------------------------------------------------------------------------------------------------
| Id  | Operation                       | Name                    | Starts | E-Rows | A-Rows |   A-Time   | Buffers |  OMem |  1Mem | Used-Mem |
------------------------------------------------------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                |                         |      1 |        |      1 |00:00:00.01 |       0 |       |       |          |
|   1 |  SORT AGGREGATE                 |                         |      1 |      1 |      1 |00:00:00.01 |       6 |       |       |          |
|*  2 |   VIEW                          |                         |      1 |      2 |      2 |00:00:00.01 |       6 |       |       |          |
|*  3 |    WINDOW BUFFER PUSHED RANK    |                         |      1 |     55 |      2 |00:00:00.01 |       6 |  2048 |  2048 | 2048  (0)|
|   4 |     NESTED LOOPS                |                         |      1 |     55 |     56 |00:00:00.01 |       6 |       |       |          |
|   5 |      TABLE ACCESS BY INDEX ROWID| ACTOR                   |      1 |      2 |      2 |00:00:00.01 |       2 |       |       |          |
|*  6 |       INDEX RANGE SCAN          | IDX_ACTOR_LAST_NAME     |      1 |      2 |      2 |00:00:00.01 |       1 |       |       |          |
|*  7 |      INDEX RANGE SCAN           | IDX_FK_FILM_ACTOR_ACTOR |      2 |     27 |     56 |00:00:00.01 |       4 |       |       |          |
|   8 |  FAST DUAL                      |                         |      1 |      1 |      1 |00:00:00.01 |       0 |       |       |          |
------------------------------------------------------------------------------------------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   2 - filter("from$_subquery$_005"."rowlimit_$$_rownumber"<=2)
   3 - filter(ROW_NUMBER() OVER ( ORDER BY  NULL )<=2)
   6 - access("A"."LAST_NAME"='WAHLBERG')
   7 - access("A"."ACTOR_ID"="FA"."ACTOR_ID")
```

이런, 이건 더 나아 보이지 않습니다. `NESTED LOOPS` 연산이 `WINDOW BUFFER PUSHED RANK` 연산으로부터 쿼리가 중단되었다는 메모를 받지 못한 것 같습니다. `E-Rows`(예상)와 `A-Rows`(실제) 값이 여전히 일치하므로, `JOIN`이 완전히 실행된 것으로 보입니다.

혹시 몰라 다음도 시도해 봅시다:

`ROWNUM` 사용:

Oracle 12c에서 표준 SQL `FETCH` 구문을 도입한 이후 이 좀비 같은 구문이 먼 과거의 기억에만 속하기를 바랐지만, 이 대안으로는 어떤 일이 일어나는지 시도해 봅시다:

```sql
SELECT (
  SELECT count(*)
  FROM (
    SELECT *
    FROM actor a
    JOIN film_actor fa USING (actor_id)
    WHERE a.last_name = 'WAHLBERG'
    AND ROWNUM <= 2 -- 별로지만, 작동합니다
  ) t
) >= 2
```

실행 계획은 이제 다음과 같습니다:

```
SQL_ID  6r7w9d0425j6c, child number 0
-------------------------------------
SELECT /*+GATHER_PLAN_STATISTICS*/(           SELECT count(*)
from (             select *             FROM actor a             JOIN
film_actor fa USING (actor_id)             WHERE a.last_name =
'WAHLBERG'             AND ROWNUM <= 2           ) t         ) >= 2

Plan hash value: 1271700124

-----------------------------------------------------------------------------------------------------------------------------
| Id  | Operation                               | Name                    | Starts | E-Rows | A-Rows |   A-Time   | Buffers |
-----------------------------------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                        |                         |      1 |        |      1 |00:00:00.01 |       0 |
|   1 |  SORT AGGREGATE                         |                         |      1 |      1 |      1 |00:00:00.01 |       4 |
|   2 |   VIEW                                  |                         |      1 |      2 |      2 |00:00:00.01 |       4 |
|*  3 |    COUNT STOPKEY                        |                         |      1 |        |      2 |00:00:00.01 |       4 |
|   4 |     NESTED LOOPS                        |                         |      1 |     55 |      2 |00:00:00.01 |       4 |
|   5 |      TABLE ACCESS BY INDEX ROWID BATCHED| ACTOR                   |      1 |      2 |      1 |00:00:00.01 |       2 |
|*  6 |       INDEX RANGE SCAN                  | IDX_ACTOR_LAST_NAME     |      1 |      2 |      1 |00:00:00.01 |       1 |
|*  7 |      INDEX RANGE SCAN                   | IDX_FK_FILM_ACTOR_ACTOR |      1 |     27 |      2 |00:00:00.01 |       2 |
|   8 |  FAST DUAL                              |                         |      1 |      1 |      1 |00:00:00.01 |       0 |
-----------------------------------------------------------------------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   3 - filter(ROWNUM<=2)
   6 - access("A"."LAST_NAME"='WAHLBERG')
   7 - access("A"."ACTOR_ID"="FA"."ACTOR_ID")
```

자, 이것이 바로 제가 말하고 싶었던 것입니다. `NESTED LOOPS` 연산의 `A-Rows` 값이 예상대로 `2`입니다. `COUNT STOPKEY` 연산이 후속 연산들에게 어떻게 행동해야 하는지 알려줄 수 있습니다.

벤치마크 결과:

```
Run 1, Statement 1 : 1.9564
Run 1, Statement 2 : 2.98499
Run 1, Statement 3 : 1.07291
Run 2, Statement 1 : 1.69192
Run 2, Statement 2 : 2.66905
Run 2, Statement 3 : 1.01144
Run 3, Statement 1 : 1.71051
Run 3, Statement 2 : 2.63831
Run 3, Statement 3 : 1       -- 가장 빠른 실행이 1
Run 4, Statement 1 : 1.61544
Run 4, Statement 2 : 2.67334
Run 4, Statement 3 : 1.00786
Run 5, Statement 1 : 1.72981
Run 5, Statement 2 : 2.77913
Run 5, Statement 3 : 1.02716
```

이런. 실제로 `FETCH FIRST 2 ROWS ONLY` 절은 이 경우에 좋지 않은 것으로 나타났습니다. 심지어 이를 생략하고 전체 결과를 세는 것보다 성능이 더 나빠졌습니다. 하지만 `ROWNUM` 필터는 이전의 PostgreSQL `LIMIT`처럼 크게 도움이 되었습니다. 저는 이것을 Oracle의 옵티마이저 버그로 간주합니다. `FETCH FIRST`는 다양한 다른 연산으로 푸시다운될 수 있어야 하는 연산입니다.

### MySQL

`LIMIT` 없이:

```
-> Rows fetched before execution  (cost=0.00..0.00 rows=1) (actual time=0.000..0.000 rows=1 loops=1)
-> Select #2 (subquery in projection; run only once)
    -> Aggregate: count(0)  (cost=1.35 rows=1) (actual time=0.479..0.479 rows=1 loops=1)
        -> Nested loop inner join  (cost=1.15 rows=2) (actual time=0.077..0.110 rows=56 loops=1)
            -> Covering index lookup on a using idx_actor_last_name (last_name='WAHLBERG')  (cost=0.45 rows=2) (actual time=0.059..0.061 rows=2 loops=1)
            -> Covering index lookup on fa using PRIMARY (actor_id=a.actor_id)  (cost=0.30 rows=1) (actual time=0.011..0.021 rows=28 loops=2)
```

`LIMIT` 사용:

```
-> Rows fetched before execution  (cost=0.00..0.00 rows=1) (actual time=0.000..0.000 rows=1 loops=1)
-> Select #2 (subquery in projection; run only once)
    -> Aggregate: count(0)  (cost=4.08..4.08 rows=1) (actual time=0.399..0.400 rows=1 loops=1)
        -> Table scan on t  (cost=2.62..3.88 rows=2) (actual time=0.394..0.394 rows=2 loops=1)
            -> Materialize  (cost=1.35..1.35 rows=2) (actual time=0.033..0.033 rows=2 loops=1)
                -> Limit: 2 row(s)  (cost=1.15 rows=2) (actual time=0.024..0.025 rows=2 loops=1)
                    -> Nested loop inner join  (cost=1.15 rows=2) (actual time=0.024..0.024 rows=2 loops=1)
                        -> Covering index lookup on a using idx_actor_last_name (last_name='WAHLBERG')  (cost=0.45 rows=2) (actual time=0.014..0.014 rows=1 loops=1)
                        -> Covering index lookup on fa using PRIMARY (actor_id=a.actor_id)  (cost=0.30 rows=1) (actual time=0.008..0.008 rows=2 loops=1)
```

여기서도 원하는 차이를 보여주는 `Nested loop inner join` 행을 확인할 수 있습니다:

이전:

```
Nested loop inner join  (cost=1.15 rows=2) (actual time=0.077..0.110 rows=56 loops=1)
```

이후:

```
Nested loop inner join  (cost=1.15 rows=2) (actual time=0.024..0.024 rows=2 loops=1)
```

벤치마크 결과:

역시 `LIMIT`가 도움이 되지만, 차이는 덜 인상적입니다:

```
0	1	1.2933
0	2	1.0089
1	1	1.2489
1	2	1.0000 -- 가장 빠른 실행이 1
2	1	1.2444
2	2	1.0933
3	1	1.2133
3	2	1.0178
4	1	1.2267
4	2	1.0178
```

### SQL Server

`LIMIT` 없이:

```
  |--Compute Scalar(DEFINE:([Expr1006]=CASE WHEN [Expr1004]>=(2) THEN (1) ELSE (0) END))
       |--Compute Scalar(DEFINE:([Expr1004]=CONVERT_IMPLICIT(int,[Expr1010],0)))
            |--Stream Aggregate(DEFINE:([Expr1010]=Count(*)))
                 |--Nested Loops(Inner Join, OUTER REFERENCES:([a].[actor_id]))
                      |--Table Scan(OBJECT:([sakila].[dbo].[actor] AS [a]), WHERE:([sakila].[dbo].[actor].[last_name] as [a].[last_name]='WAHLBERG'))
                      |--Index Seek(OBJECT:([sakila].[dbo].[film_actor].[PK__film_act__086D31FF6BE587FC] AS [fa]), SEEK:([fa].[actor_id]=[sakila].[dbo].[actor].[actor_id] as [a].[actor_id]) ORDERED FORWARD)
```

`LIMIT` 사용:

```
  |--Compute Scalar(DEFINE:([Expr1007]=CASE WHEN [Expr1005]>=(2) THEN (1) ELSE (0) END))
       |--Compute Scalar(DEFINE:([Expr1005]=CONVERT_IMPLICIT(int,[Expr1011],0)))
            |--Stream Aggregate(DEFINE:([Expr1011]=Count(*)))
                 |--Top(TOP EXPRESSION:((2)))
                      |--Nested Loops(Inner Join, OUTER REFERENCES:([a].[actor_id]))
                           |--Table Scan(OBJECT:([sakila].[dbo].[actor] AS [a]), WHERE:([sakila].[dbo].[actor].[last_name] as [a].[last_name]='WAHLBERG'))
                           |--Index Seek(OBJECT:([sakila].[dbo].[film_actor].[PK__film_act__086D31FF6BE587FC] AS [fa]), SEEK:([fa].[actor_id]=[sakila].[dbo].[actor].[actor_id] as [a].[actor_id]) ORDERED FORWARD)
```

텍스트 버전에서는 `SHOWPLAN_ALL`을 사용해도 실제 행 수가 표시되지 않으므로, 벤치마크에서 어떤 일이 일어나는지만 살펴봅시다:

벤치마크 결과:

```
Run 1, Statement 1: 1.92118
Run 1, Statement 2: 1.00000 -- 가장 빠른 실행이 1
Run 2, Statement 1: 1.95567
Run 2, Statement 2: 1.01724
Run 3, Statement 1: 1.91379
Run 3, Statement 2: 1.01724
Run 4, Statement 1: 1.93842
Run 4, Statement 2: 1.04926
Run 5, Statement 1: 1.95567
Run 5, Statement 2: 1.03448
```

그리고 다시 한번, 이 특정 쿼리에서 인상적인 2배의 성능 향상을 보여줍니다.

## 결론

`COUNT(*)` 대 `EXISTS`에 대한 이전 블로그 글에서와 마찬가지로, 쿼리에서 `N`개 이상의 행이 존재하는지 확인하고 싶은 이번 경우에도 겉으로 보기에 당연한 것이 다시 한번 사실로 확인되었습니다. 모든 행을 맹목적으로 세면, `LIMIT` 또는 `TOP` 절, 또는 Oracle에서는 `ROWNUM`을 사용하여 옵티마이저를 도운 것보다 훨씬 나쁜 성능을 보았습니다.

기술적으로, 옵티마이저가 이 최적화를 스스로 감지할 수 있었겠지만, 비용 모델에 의존하지 않는 최적화에 대한 이전 글에서 보여주었듯이, 옵티마이저가 항상 할 수 있는 모든 것을 하는 것은 아닙니다.

안타깝게도, Oracle의 경우 표준 SQL 구문이 (이 벤치마크에서) 오히려 더 느렸습니다. 이것이 모든 경우에 일반적으로 더 느리다는 의미는 아니지만, 주의해서 살펴볼 만한 가치가 있습니다. 오래된 `ROWNUM` 절이 더 잘 최적화되는 경우가 여전히 있습니다. 이번이 그런 경우 중 하나입니다.

구문 X가 구문 Y보다 빠른지는 실행 계획을 연구하거나 (예상값만이 아니라 실제 값도 포함하여), 간단한 SQL 벤치마크를 실행하여 확인할 수 있습니다. 벤치마크에서는 항상 그렇듯이, 결과를 해석할 때 주의하고, 다시 한번 확인하고, 더 많은 대안을 시도해 보세요.
