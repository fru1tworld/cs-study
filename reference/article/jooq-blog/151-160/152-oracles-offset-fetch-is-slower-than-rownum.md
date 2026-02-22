# Oracle의 OFFSET .. FETCH는 클래식 ROWNUM 필터링보다 느릴 수 있다

> 원문: https://blog.jooq.org/oracles-offset-fetch-is-slower-than-classic-rownum-filtering/

*Lukas Eder, 2018년 6월 25일*

Oracle 12c의 가장 멋진 기능 중 하나는 SQL 표준 `OFFSET .. FETCH` 절의 도입이었습니다. 이 기능을 통해 개발자들은 더 깔끔한 페이지네이션 쿼리를 작성할 수 있게 되었고, 쿼리 가독성과 상관 서브쿼리 중첩에 한계가 있던 레거시 `ROWNUM` 접근 방식을 대체하게 되었습니다.

## 문제가 무엇인가?

Oracle은 `FETCH FIRST` 구문을 내부적으로 윈도우 함수 필터링으로 변환합니다. 다음 쿼리를:

```sql
SELECT *
FROM film
ORDER BY film_id
FETCH FIRST 1 ROW ONLY;
```

`ROW_NUMBER()`를 사용하는 것과 동등한 로직으로 변환합니다. 그러나 이것은 최적화 문제를 야기합니다. 실행 계획을 보면 중요한 차이점이 드러납니다: 레거시 구문은 `COUNT STOPKEY`를 사용하여 인덱스를 효율적으로 탐색하는 반면, 표준 접근 방식은 `WINDOW SORT PUSHED RANK`와 함께 전체 테이블 스캔을 수행합니다.

테스트 결과 힌트 없이는 극적인 성능 저하가 나타났습니다. 1000행 테이블에서 단일 행 쿼리는 접근 방식 간에 40배의 성능 차이를 보였습니다. 여러 테이블을 조인하여 하나의 결과를 가져올 때, 최적화되지 않은 FETCH 쿼리와 ROWNUM 필터링 간의 차이는 60배 느림으로 벌어졌습니다.

옵티마이저는 명시적인 지침 없이는 낮은 카디널리티 추정을 인식하지 못했고, 인덱스 효율적인 중첩 루프 대신 해시 조인과 전체 테이블 스캔을 선택했습니다.

### 간단한 쿼리 비교

레거시 Oracle 구문:
```sql
SELECT t.*
FROM (
  SELECT *
  FROM film
  ORDER BY film_id
) t
WHERE ROWNUM = 1;
```

힌트가 있는 표준 구문:
```sql
SELECT /*+FIRST_ROWS(1)*/ *
FROM film
ORDER BY film_id
FETCH FIRST 1 ROW ONLY;
```

힌트가 없는 표준 구문:
```sql
SELECT *
FROM film
ORDER BY film_id
FETCH FIRST 1 ROW ONLY;
```

### 실행 계획 분석

계획 1: ROWNUM 기반 쿼리 (간단한 경우)
```
---------------------------------------------------------
| Id  | Operation                     | Name    | Rows  |
---------------------------------------------------------
|   0 | SELECT STATEMENT              |         |       |
|*  1 |  COUNT STOPKEY                |         |       |
|   2 |   VIEW                        |         |     1 |
|   3 |    TABLE ACCESS BY INDEX ROWID| FILM    |  1000 |
|   4 |     INDEX FULL SCAN           | PK_FILM |     1 |
---------------------------------------------------------
```

계획 2: FETCH FIRST 쿼리 (간단한 경우)
```
-------------------------------------------------
| Id  | Operation                | Name | Rows  |
-------------------------------------------------
|   0 | SELECT STATEMENT         |      |       |
|*  1 |  VIEW                    |      |     1 |
|*  2 |   WINDOW SORT PUSHED RANK|      |  1000 |
|   3 |    TABLE ACCESS FULL     | FILM |  1000 |
-------------------------------------------------
```

계획 3: +FIRST_ROWS 힌트가 있는 FETCH FIRST (간단한 경우)
```
---------------------------------------------------------
| Id  | Operation                     | Name    | Rows  |
---------------------------------------------------------
|   0 | SELECT STATEMENT              |         |       |
|*  1 |  VIEW                         |         |     1 |
|*  2 |   WINDOW NOSORT STOPKEY       |         |     1 |
|   3 |    TABLE ACCESS BY INDEX ROWID| FILM    |  1000 |
|   4 |     INDEX FULL SCAN           | PK_FILM |     1 |
---------------------------------------------------------
```

이 실행 계획은 그다지 만족스럽지 않습니다. 힌트 없는 FETCH FIRST를 ROWNUM 필터링 쿼리와 비교할 때, 옵티마이저는 명시적인 힌트 없이는 사용 가능한 인덱스를 활용하지 못합니다.

### 벤치마크 결과: 간단한 film 테이블 - FIRST 1 ROW

Oracle 12.2.0.1.0 Docker 환경에서 각 쿼리를 10,000번씩 실행하고 5회 반복 측정한 결과, 접근 방식 간에 40배의 성능 차이가 있었으며, ROWNUM 기반 필터링이 가장 빠르고, FETCH FIRST + FIRST_ROWS 힌트가 약간 느리며, "네이키드" FETCH FIRST가 매우 느렸습니다.

| 실행 | ROWNUM | FETCH +FIRST_ROWS | FETCH (네이키드) |
|------|--------|-------------------|-----------------|
| 1 | 1.11230 | 1.15508 | 46.92781 |
| 2 | 1.68449 | 1.99465 | 47.32620 |
| 3 | 1.10428 | 1.13904 | 68.06417 |
| 4 | 1.00000 | 6.00535 | 44.88235 |

## 복잡한 조인 쿼리

조인이 포함된 경우 상황은 더 나빠집니다: 60배 차이입니다.

레거시 Oracle 구문:
```sql
SELECT t.*
FROM (
  SELECT *
  FROM customer
  JOIN address USING (address_id)
  JOIN city USING (city_id)
  JOIN country USING (country_id)
  ORDER BY customer_id
) t
WHERE ROWNUM = 1;
```

힌트가 있는 표준 구문:
```sql
SELECT /*+FIRST_ROWS(1)*/ *
FROM customer
JOIN address USING (address_id)
JOIN city USING (city_id)
JOIN country USING (country_id)
ORDER BY customer_id
FETCH FIRST 1 ROW ONLY;
```

힌트가 없는 표준 구문:
```sql
SELECT *
FROM customer
JOIN address USING (address_id)
JOIN city USING (city_id)
JOIN country USING (country_id)
ORDER BY customer_id
FETCH FIRST 1 ROW ONLY;
```

### 복잡한 조인의 실행 계획

계획 4: 조인이 있는 ROWNUM (복잡한 경우)
```
-----------------------------------------------------------------
| Id  | Operation                         | Name        | Rows  |
-----------------------------------------------------------------
|   0 | SELECT STATEMENT                  |             |       |
|*  1 |  COUNT STOPKEY                    |             |       |
|   2 |   VIEW                            |             |     1 |
|   3 |    NESTED LOOPS                   |             |     1 |
|   4 |     NESTED LOOPS                  |             |     1 |
|   5 |      NESTED LOOPS                 |             |     1 |
|   6 |       NESTED LOOPS                |             |     1 |
|   7 |        TABLE ACCESS BY INDEX ROWID| CUSTOMER    |   302 |
|   8 |         INDEX FULL SCAN           | PK_CUSTOMER |     1 |
|   9 |        TABLE ACCESS BY INDEX ROWID| ADDRESS     |     1 |
|* 10 |         INDEX UNIQUE SCAN         | PK_ADDRESS  |     1 |
|  11 |       TABLE ACCESS BY INDEX ROWID | CITY        |     1 |
|* 12 |        INDEX UNIQUE SCAN          | PK_CITY     |     1 |
|* 13 |      INDEX UNIQUE SCAN            | PK_COUNTRY  |     1 |
|  14 |     TABLE ACCESS BY INDEX ROWID   | COUNTRY     |     1 |
-----------------------------------------------------------------
```

계획 5: +FIRST_ROWS 힌트가 있는 FETCH FIRST (복잡한 조인)
```
-----------------------------------------------------------------
| Id  | Operation                         | Name        | Rows  |
-----------------------------------------------------------------
|   0 | SELECT STATEMENT                  |             |       |
|*  1 |  VIEW                             |             |     1 |
|*  2 |   WINDOW NOSORT STOPKEY           |             |     1 |
|   3 |    NESTED LOOPS                   |             |     1 |
|   4 |     NESTED LOOPS                  |             |     1 |
|   5 |      NESTED LOOPS                 |             |     1 |
|   6 |       NESTED LOOPS                |             |     1 |
|   7 |        TABLE ACCESS BY INDEX ROWID| CUSTOMER    |   302 |
|   8 |         INDEX FULL SCAN           | PK_CUSTOMER |     1 |
|   9 |        TABLE ACCESS BY INDEX ROWID| ADDRESS     |     1 |
|* 10 |         INDEX UNIQUE SCAN         | PK_ADDRESS  |     1 |
|  11 |       TABLE ACCESS BY INDEX ROWID | CITY        |     1 |
|* 12 |        INDEX UNIQUE SCAN          | PK_CITY     |     1 |
|* 13 |      INDEX UNIQUE SCAN            | PK_COUNTRY  |     1 |
|  14 |     TABLE ACCESS BY INDEX ROWID   | COUNTRY     |     1 |
-----------------------------------------------------------------
```

계획 6: 힌트 없는 FETCH FIRST (복잡한 조인)
```
---------------------------------------------------------------
| Id  | Operation                        | Name       | Rows  |
---------------------------------------------------------------
|   0 | SELECT STATEMENT                 |            |       |
|*  1 |  VIEW                            |            |     1 |
|*  2 |   WINDOW SORT PUSHED RANK        |            |   599 |
|*  3 |    HASH JOIN                     |            |   599 |
|   4 |     TABLE ACCESS FULL            | CUSTOMER   |   599 |
|*  5 |     HASH JOIN                    |            |   603 |
|   6 |      MERGE JOIN                  |            |   600 |
|   7 |       TABLE ACCESS BY INDEX ROWID| COUNTRY    |   109 |
|   8 |        INDEX FULL SCAN           | PK_COUNTRY |   109 |
|*  9 |       SORT JOIN                  |            |   600 |
|  10 |        TABLE ACCESS FULL         | CITY       |   600 |
|  11 |      TABLE ACCESS FULL           | ADDRESS    |   603 |
---------------------------------------------------------------
```

### 벤치마크 결과: 복잡한 조인 - FIRST 1 ROW

| 실행 | ROWNUM | FETCH +FIRST_ROWS | FETCH (네이키드) |
|------|--------|-------------------|-----------------|
| 1 | 1.26157 | 1.32394 | 66.97384 |
| 2 | 1.31992 | 1.76459 | 72.76056 |
| 3 | 1.00000 | 1.36419 | 74.06439 |
| 4 | 1.08451 | 1.64990 | 66.83702 |

## 면책 조항

성능 차이가 줄어드는 조건들이 있습니다:

- 기본 데이터셋이 상당히 큰 경우(예: 약 16,000행), 차이가 작아지거나 심지어 없어집니다.
- LIMIT이 1-3행을 초과하는 경우(예: 상위 50개 가져오기), 레거시 ROWNUM 접근 방식도 때때로 제대로 최적화되지 않아 두 구문 모두에 힌트가 필요합니다.

### 벤치마크 결과: Payment 테이블 (16K 행) - FIRST 1 ROW

| 실행 | ROWNUM | FETCH +FIRST_ROWS | FETCH (네이키드) |
|------|--------|-------------------|-----------------|
| 1 | 1.00000 | 1.72246 | 1.76165 |
| 2 | 1.03919 | 1.78284 | 1.75742 |
| 3 | 1.25530 | 1.86441 | 2.39089 |
| 4 | 2.28814 | 3.02436 | 2.39407 |
| 5 | 1.31462 | 2.27225 | 1.70975 |

### 벤치마크 결과: 복잡한 조인 - FIRST 50 ROWS

| 실행 | ROWNUM +FIRST_ROWS | ROWNUM | FETCH +FIRST_ROWS | FETCH |
|------|-------------------|--------|-------------------|--------|
| 1 | 1.00545 | 7.24842 | 1.35691 | 7.15264 |
| 2 | 1.08054 | 6.51922 | 1.35960 | 7.94527 |
| 3 | 1.02824 | 7.16228 | 1.19702 | 7.55008 |
| 4 | 1.08364 | 6.66652 | 1.18559 | 7.36938 |
| 5 | 1.00000 | 6.89051 | 1.24211 | 7.15167 |

## 왜 이 구문을 사용해야 하는가?

성능 우려에도 불구하고, FETCH FIRST는 상당한 이점을 제공합니다.

SQL 표준 구문은 작성하기 훨씬 좋고, `CROSS APPLY`나 `LATERAL`을 사용하여 각 배우별 가장 긴 영화 제목 상위 3개를 찾는 것과 같은 멋진 TOP-N 스타일 쿼리를 가능하게 합니다.

```sql
SELECT actor_id, first_name, last_name, title
FROM actor a
OUTER APPLY (
  SELECT /*+FIRST_ROWS(1)*/ title
  FROM film f
  JOIN film_actor fa USING (film_id)
  WHERE fa.actor_id = a.actor_id
  ORDER BY length(title) DESC
  FETCH FIRST 3 ROWS ONLY
) t
ORDER BY actor_id, length(title) DESC;
```

이것은 ROWNUM 접근 방식으로는 훨씬 더 어려웠을 것입니다. 이전 Oracle 버전에서는 이중 중첩된 파생 테이블/상관 서브쿼리에서 A.ACTOR_ID를 참조할 수 없었기 때문에 불가능하기까지 했습니다.

구문적으로, 이것은 페이지네이션 쿼리나 TOP-N 쿼리를 수행하는 훨씬 더 좋은 방법입니다. 하지만 대가가 매우 큽니다.

또한 클래식 접근 방식은 파생 테이블의 ORDER BY 절에 의존하는데, 이것이 가장 바깥쪽 쿼리에서 유지된다는 보장이 없어야 합니다.

## 결론

여기서 본 것은 다소 불행합니다. 어떤 경우에는 한 접근 방식이 성능 면에서 다른 것보다 낫습니다. 다른 경우에는 그 반대입니다. 페이지네이션 쿼리는 Oracle이 제대로 처리하기에 여전히 까다로우며, 우리는 명시적으로 측정해야 합니다.

## jOOQ에서의 해결책

`/*+FIRST_ROWS(1)*/` 힌트를 추가하면 거의 동등한 성능이 복원됩니다. 그러나 이것은 개발자의 수동 최적화가 필요합니다.

이 문제가 Oracle에 의해 수정될 때까지, jOOQ를 사용하는 경우 `SQLDialect.ORACLE11G` 방언을 사용하여 Oracle 12c에서도 클래식 ROWNUM 필터링 쿼리를 실행할 수 있습니다. 또는 향후 버전에서 합리적으로 근사화된 카디널리티와 함께 자동 힌트 생성이 포함될 때까지 기다릴 수 있습니다.

ROWNUM 솔루션이 가장 좋은 성능을 보이며, SQL:2003 표준 윈도우 함수 기반 솔루션보다도 더 좋은 성능을 보입니다.
