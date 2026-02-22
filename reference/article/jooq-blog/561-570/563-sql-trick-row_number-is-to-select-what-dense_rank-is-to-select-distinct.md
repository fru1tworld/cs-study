# SQL 트릭: row_number()가 SELECT에 대한 것이면 dense_rank()는 SELECT DISTINCT에 대한 것이다

> 원문: https://blog.jooq.org/sql-trick-row_number-is-to-select-what-dense_rank-is-to-select-distinct/

2013년 10월 9일 lukaseder 작성

SQL:2003 표준 순위 함수들은 훌륭한 동반자이며 때때로 유용한 도구입니다. 거의 모든 데이터베이스에서 지원되는 함수들은 다음과 같습니다:

* ROW_NUMBER(): 파티션 내에서 중복 여부와 관계없이 모든 행에 대해 새로운 행 번호를 생성합니다.
* RANK(): 파티션 내에서 각각의 고유한 행에 대해 새로운 행 번호를 생성하며, 중복 그룹 사이에 간격을 남깁니다.
* DENSE_RANK(): 파티션 내에서 각각의 고유한 행에 대해 새로운 행 번호를 생성하며, 중복 그룹 사이에 간격을 남기지 않습니다.

항상 그렇듯이, 위의 내용은 예제를 통해 훨씬 이해하기 쉽습니다. 8개의 레코드가 있고 그 중 일부가 중복인 테이블을 포함하는 다음 PostgreSQL 스키마를 가정해 봅시다:

```sql
CREATE TABLE t AS
SELECT 'a' v UNION ALL
SELECT 'a'   UNION ALL
SELECT 'a'   UNION ALL
SELECT 'b'   UNION ALL
SELECT 'c'   UNION ALL
SELECT 'c'   UNION ALL
SELECT 'd'   UNION ALL
SELECT 'e'
```

이제 앞서 언급한 세 가지 순위 함수와 함께 각 값을 선택해 봅시다. 재미를 위해 SQL 표준 WINDOW 절을 사용해 보겠습니다! 야호, 반복적인 SQL 코드 15자를 절약했습니다. WINDOW 절은 PostgreSQL과 Sybase SQL Anywhere를 제외하고는 거의 구현되어 있지 않다는 점에 주의하세요...

```sql
SELECT
  v,
  ROW_NUMBER() OVER (window) row_number,
  RANK()       OVER (window) rank,
  DENSE_RANK() OVER (window) dense_rank
FROM t
WINDOW window AS (ORDER BY v)
ORDER BY v
```

위 쿼리의 결과는 다음과 같습니다:

```
+---+------------+------+------------+
| V | ROW_NUMBER | RANK | DENSE_RANK |
+---+------------+------+------------+
| a |          1 |    1 |          1 |
| a |          2 |    1 |          1 |
| a |          3 |    1 |          1 |
| b |          4 |    4 |          2 |
| c |          5 |    5 |          3 |
| c |          6 |    5 |          3 |
| d |          7 |    7 |          4 |
| e |          8 |    8 |          5 |
+---+------------+------+------------+
```

(이 SQLFiddle도 참고하세요)

### SELECT DISTINCT를 작성할 때 DENSE_RANK()가 어떻게 도움이 될 수 있는가

의심할 여지 없이, `ROW_NUMBER()`는 위의 순위 함수들 중에서 가장 유용합니다. 특히 DB2, Oracle(11g 이하), Sybase SQL Anywhere(버전 12 이전), SQL Server(2008 이하)에서 LIMIT .. OFFSET 절을 에뮬레이션해야 할 때 그렇습니다. jOOQ가 다양한 SQL 방언에서 이 SQL 절을 어떻게 에뮬레이션하는지 더 읽어보세요.

하지만 `ROW_NUMBER()`를 `DISTINCT`나 `UNION`과 함께 사용할 때 미묘한 문제가 있습니다. `ROW_NUMBER`는 파티션 내에서 항상 고유한 값을 생성하기 때문에 데이터베이스가 중복을 제거하는 것을 방해합니다. 위의 예제에서 `T.V`에 대한 중복 값은 의도적으로 추가되었습니다. 어떻게 하면 먼저 중복을 제거하고 그 다음에만 행 번호를 매길 수 있을까요? 분명히, 더 이상 `ROW_NUMBER()`를 사용할 수 없습니다. 다음 쿼리는:

```sql
SELECT DISTINCT
  v,
  ROW_NUMBER() OVER (window) row_number
FROM t
WINDOW window AS (ORDER BY v)
ORDER BY v, row_number
```

... 다음을 출력합니다

```
+---+------------+
| V | ROW_NUMBER |
+---+------------+
| a |          1 |
| a |          2 |
| a |          3 |
| b |          4 |
| c |          5 |
| c |          6 |
| d |          7 |
| e |          8 |
+---+------------+
```

(이 SQLFiddle도 참고하세요)

하지만 대신 `DENSE_RANK()`를 사용할 수 있습니다! `DENSE_RANK()`를 사용하면 중복 레코드가 동일한 순위를 받는 방식으로 순위가 적용됩니다. 그리고 순위 사이에 간격이 없습니다. 따라서:

```sql
SELECT DISTINCT
  v,
  DENSE_RANK() OVER (window) row_number
FROM t
WINDOW window AS (ORDER BY v)
ORDER BY v, row_number
```

... 이것은 다음을 출력합니다:

```
+---+------------+
| V | ROW_NUMBER |
+---+------------+
| a |          1 |
| b |          2 |
| c |          3 |
| d |          4 |
| e |          5 |
+---+------------+
```

(이 SQLFiddle도 참고하세요)

### 따라서 기억하세요...

> 따라서 기억하세요: `ROW_NUMBER()`가 `SELECT`에 대한 것이면 `DENSE_RANK()`는 `SELECT DISTINCT`에 대한 것입니다

### 주의사항

위의 내용이 참이 되려면, `SELECT DISTINCT` 절의 모든 표현식이 `DENSE_RANK()`의 `OVER(ORDER BY ...)` 절에 사용되어야 합니다. 예를 들어:

```sql
SELECT DISTINCT
  v1,
  v2,
  v3,
  DENSE_RANK() OVER (window) row_number
FROM t
WINDOW window AS (ORDER BY v1, v2, v3)
```

`v1, v2, v3` 중 어느 것이라도 다른 순위 함수나 집계 함수, 또는 비결정적 표현식 등이라면, 위의 트릭은 작동하지 않습니다. 하지만 고유한 행에 행 번호가 필요한 드문 코너 케이스 쿼리를 위해 소매 속에 간직해 둘 만한 좋은 트릭입니다. 그리고 이 트릭에 대해 항상 생각하고 싶지 않다면, 우리는 이것을 jOOQ에 내장해 두었다는 점을 알아두세요. 왜냐하면 우리는 여러분이 SQL 표준화가 아닌 비즈니스 로직에 대해 걱정해야 한다고 생각하기 때문입니다.
