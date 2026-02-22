# JOIN 제거: 고급 SQL 사용을 위한 필수 옵티마이저 기능

> 원문: https://blog.jooq.org/join-elimination-an-essential-optimiser-feature-for-advanced-sql-usage/

SQL의 멋진 점은 선언적 언어라는 것입니다. SQL은 4세대 프로그래밍 언어(4GL)입니다: "대부분의 4GL은 대량의 정보를 쉽게 추출, 포맷, 표시할 수 있도록 데이터베이스에서 사용하도록 설계되었습니다. [과거 어느 시점에서] 이러한 언어는 대량 처리에 초점을 맞췄습니다."

4세대 프로그래밍 언어의 멋진 점은 옵티마이저(최적화기)입니다. 옵티마이저가 무엇인지 확실하지 않다면, [이 기사를 확인해 보세요](https://blog.jooq.org/is-your-sql-update-slowed-down-by-missing-indexes-maybe/). 핵심은 4GL 자체는 비절차적(non-procedural)이라는 것입니다. 4GL을 작성할 때 컴퓨터에게 "무엇을"(what) 해야 하는지 말하지, "어떻게"(how) 해야 하는지 말하지 않습니다. 훌륭한 예시가 [아래에 보여드릴 SQL입니다](#sql-expression-tree). 고전적인 SQL 쿼리는 다음과 같이 작성합니다:

```sql
SELECT first_name, last_name
FROM customer
WHERE first_name = 'JAMIE'
```

다음과 같이 작성하지 않습니다:

```sql
for (Customer customer : customers)
    if (customer.first_name.equals("JAMIE"))
        result.add(new Object[] { customer.first_name, customer.last_name });
```

비록 우리 대부분은 더 이상 이것에 대해 감사함을 느끼지 못하더라도, 첫 번째 선언적 버전은 두 번째 절차적 버전과 비교했을 때 매우 멋진 것입니다. 왜냐하면 우리는 SQL 옵티마이저에게 결과를 얻는 방법에 대해 전혀 말하지 않기 때문입니다. 그래서 우리의 마음은 더 복잡한 것들에 집중할 수 있습니다.

## SQL 표현식 트리

이것을 좀 더 형식적으로 살펴보겠습니다.

### 표현식 동치(Equivalence)

SQL에서 매우 중요한 개념 중 하나는 표현식 동치(expression equivalence)입니다. 이것은 두 표현식이 서로 다르게 보이지만 동일한 결과를 반환할 때를 의미합니다. 다음은 간단한 수학적 예입니다:

```
A + (A - B) ≡ (2 * A) - B
```

두 표현식은 동치입니다. 동일한 결과를 생성합니다. 하지만 하나가 다른 것보다 "더 나을" 수 있습니다(측정 대상에 따라). 이것을 표현식 트리로 시각화할 수 있습니다:

```
     +              -
    / \            / \
   A   -    ≡     *   B
      / \        / \
     A   B      2   A
```

첫 번째 트리는 두 번째 트리와 동치이며, 두 번째 트리의 뺄셈은 상수 `2`만 포함합니다. 상황에 따라 하나의 표현식이 다른 것보다 평가하기 더 나을 수 있습니다.

SQL 옵티마이저의 핵심 작업 중 하나는 이러한 동치를 찾아내는 것입니다. 그런 다음 가능한 가장 빠른 표현식을 선택합니다. 모든 옵티마이저가 "가장 빠른" 표현식을 찾는다고 보장할 수는 없습니다. 그것을 찾는 것 자체에 상당한 시간이 소요될 수 있기 때문입니다. 하지만 그들은 종종 다소 빠른 표현식을 찾습니다.

다시 SQL로 돌아가서, 다음 두 표현식을 고려해 보세요:

```sql
-- 표현식 1
SELECT first_name, last_name
FROM customer
WHERE first_name = 'JAMIE'

-- 표현식 2
SELECT first_name, last_name
FROM (
  SELECT *
  FROM customer
  WHERE first_name = 'JAMIE'
)
```

분명히 동일한 결과를 생성합니다. 그리고 대부분의 데이터베이스가 동일한 실행 계획을 생성할 것입니다. 왜냐하면 그들은 모든 관계형 데이터베이스 이론의 기반이 되는 관계 대수(relational algebra)를 사용하여 SQL 표현식을 변환하기 때문입니다. 투영(projection, SELECT 절)과 선택(selection, WHERE 절) 사이의 관계에 대한 흥미로운 규칙이 있습니다. 그것은:

- 선택을 먼저 적용한 다음 투영을 적용해도 같은 결과
- 투영을 먼저 적용한 다음 선택을 적용해도 같은 결과

위의 두 쿼리는 동치이며, 옵티마이저는 자유롭게 둘 사이를 변환할 수 있습니다.

## JOIN 제거(JOIN Elimination)

이것은 우리에게 "JOIN 제거"라는 흥미로운 주제로 이끕니다. JOIN 제거(또는 "테이블 제거")는 가장 기본적이면서도 가장 강력한 SQL 변환 중 하나로, 대부분의 현대 데이터베이스에서 어떤 식으로든 구현되어 있습니다.

다음 예를 살펴보겠습니다. [Sakila 데이터베이스](https://dev.mysql.com/doc/sakila/en/)를 사용하겠습니다(또는 적어도 그 일부). 스키마의 일부는 다음과 같습니다:

```sql
CREATE TABLE address (
  address_id INT NOT NULL,
  address VARCHAR(50) NOT NULL,
  CONSTRAINT pk_address PRIMARY KEY (address_id)
);

CREATE TABLE customer (
  customer_id INT NOT NULL,
  first_name VARCHAR(45) NOT NULL,
  last_name VARCHAR(45) NOT NULL,
  address_id INT NOT NULL,
  CONSTRAINT pk_customer PRIMARY KEY (customer_id),
  CONSTRAINT fk_customer_address FOREIGN KEY (address_id)
    REFERENCES address (address_id)
);
```

다음 쿼리를 상상해 보세요:

```sql
SELECT c.first_name, c.last_name
FROM customer c
JOIN address a ON c.address_id = a.address_id
```

쿼리 실행 계획을 살펴보겠습니다. (실행 계획을 실행하는 데 사용되는 구문은 데이터베이스마다 다릅니다)

```sql
-- Oracle
EXPLAIN PLAN FOR ...;
SELECT * FROM TABLE(dbms_xplan.display);

-- PostgreSQL
EXPLAIN ...;
```

어떤 결과가 나올까요? 이상적으로는 옵티마이저가 JOIN이 불필요하다는 것을 인식해야 합니다. `fk_customer_address` 외래 키 제약 조건 덕분에 모든 `CUSTOMER` 레코드가 정확히 하나의 해당 `ADDRESS` 레코드를 가진다는 것이 보장됩니다. JOIN은 `CUSTOMER` 행을 복제하거나 제거하지 않으므로 불필요하며, 따라서 일부(전부는 아닌) 데이터베이스에서 제거됩니다. 이것은 INNER JOIN 제거의 예입니다.

### INNER JOIN 제거

INNER JOIN 제거는 다음과 같은 경우에 작동합니다:

1. UNIQUE 제약 조건이 조인되는 테이블에 있어서 최대 하나의 행이 일치하는 것이 보장됨
2. FOREIGN KEY 제약 조건이 있어서 정확히 하나의 행이 일치하는 것이 보장됨
3. 조인되는 테이블의 열이 SELECT, WHERE 또는 다른 절에서 참조되지 않음

외래 키가 성능에 유용할 수 있다는 또 다른 이유입니다! 외래 키 열에 NOT NULL 제약 조건이 있으면 이 최적화가 작동합니다.

### 어떤 데이터베이스가 이것을 지원합니까?

테스트를 해봅시다!

DB2 LUW 10.5

DB2는 INNER JOIN을 제거합니다:

```
Explain Plan
-----------------------------------------------------------
ID | Operation         | Rows | Cost
 1 | RETURN            |      |   33
 2 |  TBSCAN CUSTOMER  |  599 |   33

Predicate Information
 2 - SARG Q1.ADDRESS_ID IS NOT NULL
```

`CUSTOMER` 테이블만 검색됩니다. 주목할 점은 `ADDRESS_ID IS NOT NULL` 술어입니다. 이것은 제약 조건을 추가하기 전에 null인 기존 레코드가 있었는지 확인합니다.

MySQL 8.0.2

MySQL은 INNER JOIN을 제거하지 않습니다:

```
ID  TABLE  TYPE    KEY      ROWS  FILTERED  EXTRA
1   c      ALL     NULL      599    100.00  NULL
1   a      eq_ref  PRIMARY     1    100.00  Using index
```

두 테이블이 모두 검색됩니다. MySQL에서 외래 키 열을 NOT NULL로 변경해도 결과는 달라지지 않았습니다.

Oracle 12.2.0.1

Oracle은 INNER JOIN을 제거합니다:

```
-----------------------------------------------------------------------------
| Id  | Operation         | Name     | Rows  | Bytes | Cost (%CPU)| Time     |
-----------------------------------------------------------------------------
|   0 | SELECT STATEMENT  |          |   599 | 14376 |     5   (0)| 00:00:01 |
|*  1 |  TABLE ACCESS FULL| CUSTOMER |   599 | 14376 |     5   (0)| 00:00:01 |
-----------------------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   1 - filter("C"."ADDRESS_ID" IS NOT NULL)
```

DB2와 같이 `ADDRESS_ID IS NOT NULL` 필터가 있습니다.

PostgreSQL 9.6

PostgreSQL은 INNER JOIN을 제거하지 않습니다:

```
Hash Join  (cost=19.57..36.37 rows=599 width=13)
  Hash Cond: (c.address_id = a.address_id)
  ->  Seq Scan on customer c  (cost=0.00..14.99 rows=599 width=17)
  ->  Hash  (cost=12.03..12.03 rows=603 width=4)
        ->  Seq Scan on address a  (cost=0.00..12.03 rows=603 width=4)
```

두 테이블이 모두 검색되고 해시 조인됩니다.

SQL Server 2014

SQL Server는 INNER JOIN을 제거합니다(외래 키가 NOT NULL인 경우):

```
  |--Table Scan(OBJECT:([sakila].[dbo].[CUSTOMER] AS [c]))
```

그러나 외래 키 열이 nullable인 경우, SQL Server는 INNER JOIN을 제거하지 않습니다.

### 요약: INNER JOIN 제거

| 데이터베이스 | INNER JOIN 제거 (NOT NULL FK) | INNER JOIN 제거 (Nullable FK) |
|------------|------------------------------|-------------------------------|
| DB2 LUW 10.5 | 예 | 예 |
| MySQL 8.0.2 | 아니오 | 아니오 |
| Oracle 12.2.0.1 | 예 | 예 |
| PostgreSQL 9.6 | 아니오 | 아니오 |
| SQL Server 2014 | 예 | 아니오 |

## OUTER JOIN 제거

이제 OUTER JOIN은 어떨까요? 다음 쿼리를 고려해 보세요:

```sql
SELECT c.first_name, c.last_name
FROM customer c
LEFT JOIN address a ON c.address_id = a.address_id
```

이번에는 `ADDRESS` 테이블에 일치하는 행이 없더라도 `CUSTOMER` 테이블의 모든 행을 원합니다. 우리가 알 수 있듯이 `ADDRESS` 테이블에서 아무것도 선택하지 않으므로, `ADDRESS` 테이블에서 일치 여부는 중요하지 않습니다.

### 어떤 데이터베이스가 이것을 지원합니까?

DB2 LUW 10.5

DB2는 OUTER JOIN을 제거합니다:

```
Explain Plan
-----------------------------------------------------------
ID | Operation         | Rows | Cost
 1 | RETURN            |      |   33
 2 |  TBSCAN CUSTOMER  |  599 |   33
```

MySQL 8.0.2

MySQL은 OUTER JOIN을 제거하지 않습니다:

```
ID  TABLE  TYPE    KEY      ROWS  FILTERED  EXTRA
1   c      ALL     NULL      599    100.00  NULL
1   a      eq_ref  PRIMARY     1    100.00  Using index
```

Oracle 12.2.0.1

Oracle은 OUTER JOIN을 제거합니다:

```
-----------------------------------------------------------------------------
| Id  | Operation         | Name     | Rows  | Bytes | Cost (%CPU)| Time     |
-----------------------------------------------------------------------------
|   0 | SELECT STATEMENT  |          |   599 | 10782 |     5   (0)| 00:00:01 |
|   1 |  TABLE ACCESS FULL| CUSTOMER |   599 | 10782 |     5   (0)| 00:00:01 |
-----------------------------------------------------------------------------
```

PostgreSQL 9.6

PostgreSQL은 OUTER JOIN을 제거합니다:

```
Seq Scan on customer c  (cost=0.00..14.99 rows=599 width=13)
```

흥미롭게도 PostgreSQL은 INNER JOIN은 제거하지 못했지만 OUTER JOIN은 제거합니다. 이것은 OUTER JOIN 제거가 UNIQUE 제약 조건만 필요로 하고 FOREIGN KEY 제약 조건은 필요하지 않기 때문입니다.

SQL Server 2014

SQL Server는 OUTER JOIN을 제거합니다:

```
  |--Table Scan(OBJECT:([sakila].[dbo].[CUSTOMER] AS [c]))
```

### 요약: OUTER JOIN 제거

| 데이터베이스 | OUTER JOIN 제거 |
|------------|----------------|
| DB2 LUW 10.5 | 예 |
| MySQL 8.0.2 | 아니오 |
| Oracle 12.2.0.1 | 예 |
| PostgreSQL 9.6 | 예 |
| SQL Server 2014 | 예 |

## DISTINCT가 있는 OUTER JOIN 제거

이제 조금 더 복잡한 시나리오를 살펴보겠습니다. 일대다(one-to-many) 관계에서 "다"(many) 쪽으로 조인한다면 어떨까요?

```sql
SELECT DISTINCT c.first_name, c.last_name
FROM customer c
LEFT JOIN rental r ON c.customer_id = r.customer_id
```

이 경우 각 고객은 0개 이상의 대여(rental) 레코드를 가질 수 있습니다. 그러나 우리는 `rental` 테이블에서 아무것도 선택하지 않고 `DISTINCT`를 사용하여 결과에서 중복을 제거합니다. 따라서 `rental` 테이블로의 JOIN은 불필요합니다.

### 어떤 데이터베이스가 이것을 지원합니까?

DB2 LUW 10.5

DB2는 이 최적화를 지원합니다:

```
Explain Plan
-----------------------------------------------------------
ID | Operation           | Rows | Cost
 1 | RETURN              |      |   34
 2 |  TBSCAN             |  599 |   34
 3 |   SORT (UNIQUE)     |  599 |   34
 4 |    TBSCAN CUSTOMER  |  599 |   33
```

`RENTAL` 테이블은 검색되지 않습니다.

MySQL 8.0.2

MySQL은 이 최적화를 지원하지 않습니다:

```
ID  TABLE  TYPE  KEY                      ROWS  FILTERED  EXTRA
1   c      ALL   NULL                      599    100.00  Using temporary
1   r      ref   idx_fk_customer_id       16044    100.00  Using index; Distinct
```

두 테이블이 모두 검색됩니다.

Oracle 12.2.0.1

Oracle은 이 최적화를 지원하지 않습니다:

```
-----------------------------------------------------------------------------------
| Id  | Operation             | Name     | Rows  | Bytes | Cost (%CPU)| Time     |
-----------------------------------------------------------------------------------
|   0 | SELECT STATEMENT      |          |   599 | 16173 |   133   (2)| 00:00:01 |
|   1 |  HASH UNIQUE          |          |   599 | 16173 |   133   (2)| 00:00:01 |
|   2 |   HASH JOIN OUTER     |          | 16044 |   423K|   131   (0)| 00:00:01 |
|   3 |    TABLE ACCESS FULL  | CUSTOMER |   599 | 10782 |     5   (0)| 00:00:01 |
|   4 |    INDEX FAST FULL SCAN| IDX_FK_CUSTOMER_ID | 16044 | 64176 |   126   (0)| 00:00:01 |
-----------------------------------------------------------------------------------
```

두 테이블이 모두 검색됩니다.

PostgreSQL 9.6

PostgreSQL은 이 최적화를 지원하지 않습니다:

```
HashAggregate  (cost=381.11..387.10 rows=599 width=13)
  Group Key: c.first_name, c.last_name
  ->  Hash Right Join  (cost=19.48..300.90 rows=16044 width=13)
        Hash Cond: (r.customer_id = c.customer_id)
        ->  Seq Scan on rental r  (cost=0.00..254.44 rows=16044 width=4)
        ->  Hash  (cost=14.99..14.99 rows=599 width=17)
              ->  Seq Scan on customer c  (cost=0.00..14.99 rows=599 width=17)
```

두 테이블이 모두 검색됩니다.

SQL Server 2014

SQL Server는 이 최적화를 지원합니다:

```
  |--Sort(DISTINCT ORDER BY:([c].[first_name] ASC, [c].[last_name] ASC))
       |--Table Scan(OBJECT:([sakila].[dbo].[CUSTOMER] AS [c]))
```

`RENTAL` 테이블은 검색되지 않습니다.

### 요약: DISTINCT가 있는 OUTER JOIN 제거

| 데이터베이스 | DISTINCT OUTER JOIN 제거 |
|------------|-------------------------|
| DB2 LUW 10.5 | 예 |
| MySQL 8.0.2 | 아니오 |
| Oracle 12.2.0.1 | 아니오 |
| PostgreSQL 9.6 | 아니오 |
| SQL Server 2014 | 예 |

## 더 복잡한 예제

더 복잡한 예제를 살펴보겠습니다:

```sql
SELECT
  a.first_name,
  a.last_name,
  count(fa.film_id)
FROM actor a
LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id
LEFT JOIN film f ON fa.film_id = f.film_id
GROUP BY
  a.actor_id,
  a.first_name,
  a.last_name
```

이 쿼리에서 어떤 테이블이 제거될 수 있을까요?

- `ACTOR`: 선택되고 그룹화됨 - 제거 불가
- `FILM_ACTOR`: `count(fa.film_id)`에서 참조됨 - 제거 불가
- `FILM`: 어디에서도 참조되지 않음 - 제거 가능!

실제로 DB2와 SQL Server는 `FILM` 테이블을 제거합니다.

## 실용적 이점

JOIN 제거가 유용한 이유는 무엇일까요? 왜 처음부터 불필요한 JOIN을 작성할까요?

1. 재사용 가능한 뷰(Views): 많은 열을 포함하는 복잡한 뷰를 만들 수 있습니다. 쿼리가 뷰의 일부 열만 사용하더라도, 옵티마이저가 불필요한 JOIN을 제거할 수 있습니다.

2. 개발자 실수: 때때로 개발자들은 불필요한 JOIN을 작성합니다. 좋은 옵티마이저는 이러한 실수를 "수정"해 줍니다.

3. 점진적 쿼리 구축: 쿼리를 점진적으로 구축할 때, 처음에는 필요했지만 나중에는 불필요해진 JOIN이 있을 수 있습니다.

4. 코드 생성: jOOQ와 같은 도구로 SQL을 생성할 때, 모든 경우에 필요한지 여부를 미리 알기 어려운 JOIN을 포함할 수 있습니다.

## 결론

JOIN 제거는 SQL 옵티마이저의 매우 중요한 기능입니다. 이 기사에서 테스트한 데이터베이스들 중:

DB2와 SQL Server가 선두주자이며, 세 가지 유형의 JOIN 제거를 모두 지원합니다.

Oracle이 근접한 2위이며, INNER JOIN과 OUTER JOIN 제거는 지원하지만 DISTINCT가 있는 OUTER JOIN 제거는 지원하지 않습니다.

PostgreSQL에 대한 희망이 있습니다. 현재 OUTER JOIN 제거만 지원하지만, 향후 버전에서 개선될 수 있습니다.

MySQL에 대한 희망은 좀 적습니다. 현재 어떤 유형의 JOIN 제거도 지원하지 않습니다.

| 데이터베이스 | INNER JOIN (NOT NULL) | INNER JOIN (Nullable) | OUTER JOIN | DISTINCT OUTER JOIN |
|------------|----------------------|----------------------|------------|---------------------|
| DB2 LUW 10.5 | 예 | 예 | 예 | 예 |
| MySQL 8.0.2 | 아니오 | 아니오 | 아니오 | 아니오 |
| Oracle 12.2.0.1 | 예 | 예 | 예 | 아니오 |
| PostgreSQL 9.6 | 아니오 | 아니오 | 예 | 아니오 |
| SQL Server 2014 | 예 | 아니오 | 예 | 예 |

결국, 이것은 SQL의 선언적 특성이 빛나는 부분입니다. 옵티마이저에게 무엇을 원하는지 말하면, 옵티마이저가 어떻게 그것을 얻을지 결정합니다. 그리고 때로는 그 "어떻게"에는 불필요한 작업을 전혀 하지 않는 것이 포함됩니다.
