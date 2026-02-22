# 자체 DB 프레임워크로 SQL OFFSET 페이지네이션을 에뮬레이션하려고 하지 마라!

> 원문: https://blog.jooq.org/stop-trying-to-emulate-sql-offset-pagination-with-your-in-house-db-framework/

나는 당신이 지금까지 수많은 방식으로 잘못해 왔다고 확신한다. 그리고 아마 앞으로도 금방 제대로 하기는 어려울 것이다. 그러니 왜 비즈니스 로직을 구현할 수 있는 소중한 시간을 SQL 조정에 낭비하는가?

## 설명해 보겠다...

MySQL 사용자들이 `LIMIT .. OFFSET`으로 알고 있는 것이 다음과 같은 간단한 구문으로 표준화된 것은 최근의 SQL:2008 표준에 이르러서였다:

```sql
SELECT *
FROM BOOK
OFFSET 2 ROWS
FETCH NEXT 1 ROWS ONLY
```

그렇다. 이렇게 많은 키워드가 필요하다. SQL은 정말 장황한 언어다. 개인적으로, 우리는 MySQL/PostgreSQL의 `LIMIT .. OFFSET` 절의 간결함을 정말 좋아해서, jOOQ DSL API에 그것을 선택했다.

SQL에서:

```sql
SELECT * FROM BOOK LIMIT 1 OFFSET 2
```

jOOQ에서:

```java
select().from(BOOK).limit(1).offset(2);
```

이제, 당신이 SQL 프레임워크 벤더이거나, 자체적으로 사내 SQL 추상화를 만들고 있다면, 이 깔끔한 작은 절을 표준화하는 것에 대해 생각해볼 수 있다. 다음은 오프셋 페이지네이션을 네이티브로 지원하는 데이터베이스들의 몇 가지 변형이다:

```sql
-- MySQL, H2, HSQLDB, Postgres, SQLite
SELECT * FROM BOOK LIMIT 1 OFFSET 2

-- CUBRID는 LIMIT .. OFFSET 절의 MySQL 변형을 지원한다
SELECT * FROM BOOK LIMIT 2, 1

-- Derby, SQL Server 2012, Oracle 12, SQL:2008
SELECT * FROM BOOK
OFFSET 2 ROWS FETCH NEXT 1 ROWS ONLY

-- Ingres. 으, 거의 표준이다. 거의!
SELECT * FROM BOOK
OFFSET 2 FETCH FIRST 1 ROWS ONLY

-- Firebird
SELECT * FROM BOOK ROWS 2 TO 3

-- Sybase SQL Anywhere
SELECT TOP 1 ROWS START AT 3 * FROM BOOK

-- DB2 (OFFSET 없이)
SELECT * FROM BOOK FETCH FIRST 1 ROWS ONLY

-- Sybase ASE, SQL Server 2008 (OFFSET 없이)
SELECT TOP 1 * FROM BOOK
```

여기까지는 좋다. 이것들은 모두 처리할 수 있다. 일부 데이터베이스는 오프셋을 리밋 앞에, 다른 것들은 리밋을 오프셋 앞에 두고, T-SQL 계열은 전체 `TOP` 절을 `SELECT` 목록 앞에 둔다. 이것은 에뮬레이션하기 쉽다. 그런데 다음은 어떨까:

*   Oracle 11g 이하
*   SQL Server 2008 이하
*   OFFSET이 있는 DB2

(DB2에서는 다양한 대체 구문을 활성화할 수 있다는 점에 유의하라) 이것을 구글링하면, 그 오래된 데이터베이스들에서 `OFFSET .. FETCH`를 에뮬레이션하는 수백만 가지 방법을 찾을 것이다. 최적의 솔루션은 항상 다음을 포함한다:

*   Oracle에서 `ROWNUM` 필터링과 함께 이중 중첩 파생 테이블 사용
*   SQL Server와 DB2에서 `ROW_NUMBER()` 필터링과 함께 단일 중첩 파생 테이블 사용

그래서 당신은 그것을 _에뮬레이션_하고 있는 것이다.

## 당신이 그것을 제대로 할 수 있을 거라고 생각하는가?

;-) 당신이 생각하지 못했을 수 있는 몇 가지 문제들을 살펴보자. 먼저, Oracle. Oracle은 분명히 최대한의 벤더 종속을 만들고 싶었고, 이것은 Apple의 최근 Swift 도입에 의해서만 능가된다. 이것이 `ROWNUM` 솔루션이 가장 잘 수행되는 이유이며, SQL:2003 표준 윈도우 함수 기반 솔루션보다도 더 좋다. 믿기지 않는가? Oracle 오프셋 페이지네이션 성능에 관한 이 매우 흥미로운 글을 읽어보라. 따라서, Oracle에서의 최적의 솔루션은:

```sql
-- PostgreSQL 구문:
SELECT ID, TITLE
FROM BOOK
LIMIT 1 OFFSET 2

-- Oracle 동등 구문:
SELECT *
FROM (
  SELECT b.*, ROWNUM rn
  FROM (
    SELECT ID, TITLE
    FROM BOOK
  ) b
  WHERE ROWNUM <= 3 -- (1 + 2)
)
WHERE rn > 2
```

### 그래서 그게 정말 동등한가?

물론 아니다. 당신은 추가 컬럼, `rn` 컬럼을 선택하고 있다. 대부분의 경우에는 신경 쓰지 않을 수 있지만, `IN` 조건절과 함께 사용할 제한된 서브쿼리를 만들고 싶다면 어떨까?

```sql
-- PostgreSQL 구문:
SELECT *
FROM BOOK
WHERE AUTHOR_ID IN (
  SELECT ID
  FROM AUTHOR
  LIMIT 1 OFFSET 2
)

-- Oracle 동등 구문:
SELECT *
FROM BOOK
WHERE AUTHOR_ID IN (
  SELECT * -- 아, 이것들은 두 개의 컬럼이다!
  FROM (
    SELECT b.*, ROWNUM rn
    FROM (
      SELECT ID
      FROM AUTHOR
    ) b
    WHERE ROWNUM <= 3
  )
  WHERE rn > 2
)
```

보다시피, 더 정교한 SQL 변환을 해야 한다. `LIMIT .. OFFSET`을 수동으로 에뮬레이션하고 있다면, 서브쿼리에 `ID` 컬럼을 패치할 수 있다:

```sql
SELECT *
FROM BOOK
WHERE AUTHOR_ID IN (
  SELECT ID -- 더 나음
  FROM (
    SELECT b.ID, ROWNUM rn -- 더 나음
    FROM (
      SELECT ID
      FROM AUTHOR
    ) b
    WHERE ROWNUM <= 3
  )
  WHERE rn > 2
)
```

이제 맞는 것 같지? 하지만 매번 이것을 수동으로 작성하는 것이 아니라, 지금까지 경험한 2-3가지 사용 사례를 다루는 자체적인 멋진 사내 SQL 프레임워크를 만들기 시작하려고 하지 않는가? 당신은 할 수 있다. 그래서 위의 것을 생성하기 위해 컬럼 이름을 자동으로 regex-검색-대체할 것이다.

## 그래서 이제 맞는가?

물론 아니다! 최상위 `SELECT`에서는 모호한 컬럼 이름을 가질 수 있지만, 중첩된 select에서는 안 되기 때문이다. 다음을 하고 싶다면 어떨까:

```sql
-- PostgreSQL 구문:
-- 두 개의 ID 컬럼이 완벽하게 유효한 반복
SELECT BOOK.ID, AUTHOR.ID
FROM BOOK
JOIN AUTHOR
ON BOOK.AUTHOR_ID = AUTHOR.ID
LIMIT 1 OFFSET 2

-- Oracle 동등 구문:
SELECT *
FROM (
  SELECT b.*, ROWNUM rn
  FROM (
    -- 아! ORA-00918: column ambiguously defined
    SELECT BOOK.ID, AUTHOR.ID
    FROM BOOK
    JOIN AUTHOR
    ON BOOK.AUTHOR_ID = AUTHOR.ID
  ) b
  WHERE ROWNUM <= 3
)
WHERE rn > 2
```

안 된다. 그리고 이전 예제에서 ID 컬럼을 수동으로 패치하는 트릭은 작동하지 않는다, 여러 개의 `ID` 인스턴스가 있기 때문이다. 그리고 컬럼을 무작위 값으로 이름 바꾸는 것은 지저분하다, 왜냐하면 당신의 자체 개발 사내 데이터베이스 프레임워크 사용자는 잘 정의된 컬럼 이름을 받기를 원하기 때문이다. 즉, `ID`와... `ID`. 따라서, 해결책은 컬럼 이름을 두 번 바꾸는 것이다. 각 파생 테이블에서 한 번씩:

```sql
-- Oracle 동등 구문:
-- 합성 컬럼 이름을 원래대로 다시 이름 변경
SELECT c1 ID, c2 ID
FROM (
  SELECT b.c1, b.c2, ROWNUM rn
  FROM (
    -- 여기서 합성 컬럼 이름
    SELECT BOOK.ID c1, AUTHOR.ID c2
    FROM BOOK
    JOIN AUTHOR
    ON BOOK.AUTHOR_ID = AUTHOR.ID
  ) b
  WHERE ROWNUM <= 3
)
WHERE rn > 2
```

하지만 이제 끝났나?

물론 아니다! 그런 쿼리를 이중으로 중첩하면 어떨까? `ID` 컬럼을 합성 이름으로, 그리고 다시 원래대로 이중으로 이름 변경하는 것을 생각할 것인가? ... ;-) 여기서 멈추고 완전히 다른 것에 대해 이야기해 보자:

## 같은 것이 SQL Server 2008에서도 작동하는가?

물론 아니다! SQL Server 2008에서 가장 인기 있는 접근 방식은 윈도우 함수를 사용하는 것이다. 즉, `ROW_NUMBER()`. 그래서, 다음을 고려해 보자:

```sql
-- PostgreSQL 구문:
SELECT ID, TITLE
FROM BOOK
LIMIT 1 OFFSET 2

-- SQL Server 동등 구문:
SELECT b.*
FROM (
  SELECT ID, TITLE,
    ROW_NUMBER() OVER (ORDER BY ID) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
```

그래서 그게 다인가?

물론 아니다! ;-) 좋다, 우리는 이미 이 문제를 겪었다. `*`를 선택하면 안 된다, 왜냐하면 이것을 `IN` 조건절의 서브쿼리로 사용하는 경우 너무 많은 컬럼이 생성되기 때문이다. 그러므로 합성 컬럼 이름을 사용한 올바른 솔루션을 고려해 보자:

```sql
-- SQL Server 동등 구문:
SELECT b.c1 ID, b.c2 TITLE
FROM (
  SELECT ID c1, TITLE c2,
    ROW_NUMBER() OVER (ORDER BY ID) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
```

하지만 이제 맞게 됐지?

교양 있는 추측을 해보라: _아니다!_

원래 쿼리에 `ORDER BY` 절을 추가하면 어떻게 될까?

```sql
-- PostgreSQL 구문:
SELECT ID, TITLE
FROM BOOK
ORDER BY SOME_COLUMN
LIMIT 1 OFFSET 2

-- 순진한 SQL Server 동등 구문:
SELECT b.c1 ID, b.c2 TITLE
FROM (
  SELECT ID c1, TITLE c2,
    ROW_NUMBER() OVER (ORDER BY ID) rn
  FROM BOOK
  ORDER BY SOME_COLUMN
) b
WHERE rn > 2 AND rn <= 3
```

이것은 SQL Server에서 작동하지 않는다. 서브쿼리는 `TOP` 절(또는 SQL Server 2012의 `OFFSET .. FETCH` 절)도 함께 있지 않는 한 `ORDER BY` 절을 가질 수 없다. 좋다, SQL Server를 행복하게 하기 위해 `TOP 100 PERCENT`를 사용하여 이것을 조정할 수 있다.

```sql
-- 더 나은 SQL Server 동등 구문:
SELECT b.c1 ID, b.c2 TITLE
FROM (
  SELECT TOP 100 PERCENT
    ID c1, TITLE c2,
    ROW_NUMBER() OVER (ORDER BY ID) rn
  FROM BOOK
  ORDER BY SOME_COLUMN
) b
WHERE rn > 2 AND rn <= 3
```

이제 SQL Server에 따르면 올바른 SQL이지만, 파생 테이블의 순서가 쿼리 실행 후에도 유지될 것이라는 보장이 없다. 어떤 영향에 의해 순서가 다시 변경될 수 있다. 외부 쿼리에서 `SOME_COLUMN`으로 정렬하고 싶다면, 또 다른 합성 컬럼을 추가하기 위해 SQL 문을 다시 변환해야 한다:

```sql
-- 더 나은 SQL Server 동등 구문:
SELECT b.c1 ID, b.c2 TITLE
FROM (
  SELECT TOP 100 PERCENT
    ID c1, TITLE c2,
    SOME_COLUMN c99,
    ROW_NUMBER() OVER (ORDER BY ID) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
ORDER BY b.c99
```

이것은 조금 지저분해지기 시작한다. 그리고 다음이 맞는지 추측해 보자:

## 이것이 올바른 솔루션이다!

물론 아니다! 원래 쿼리에 `DISTINCT`가 있었다면 어떨까?

```sql
-- PostgreSQL 구문:
SELECT DISTINCT AUTHOR_ID
FROM BOOK
LIMIT 1 OFFSET 2

-- 순진한 SQL Server 동등 구문:
SELECT b.c1 AUTHOR_ID
FROM (
  SELECT DISTINCT AUTHOR_ID c1,
    ROW_NUMBER() OVER (ORDER BY AUTHOR_ID) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
```

이제, 한 저자가 여러 권의 책을 썼다면 어떻게 될까? 그렇다, `DISTINCT` 키워드는 그런 중복을 제거해야 하고, 실제로 PostgreSQL 쿼리는 올바르게 먼저 중복을 제거한 다음 `LIMIT`과 `OFFSET`을 적용한다. 그러나, `ROW_NUMBER()` 조건은 `DISTINCT`가 그것들을 다시 제거하기 _전에_ 항상 고유한 행 번호를 생성한다. 다시 말해, `DISTINCT`는 효과가 없다. 다행히, 이 깔끔한 작은 트릭을 사용하여 이 SQL을 다시 조정할 수 있다:

```sql
-- 더 나은 SQL Server 동등 구문:
SELECT b.c1 AUTHOR_ID
FROM (
  SELECT DISTINCT AUTHOR_ID c1,
    DENSE_RANK() OVER (ORDER BY AUTHOR_ID) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
```

이 트릭에 대해 여기서 더 읽어보라: SQL 트릭: row_number()가 SELECT에 대한 것이면 dense_rank()는 SELECT DISTINCT에 대한 것이다. `ORDER BY` 절은 `SELECT` 필드 목록의 모든 컬럼을 포함해야 한다는 것에 주의하라. 분명히, 이것은 `SELECT DISTINCT` 필드 목록에서 허용되는 컬럼을 윈도우 함수의 `ORDER BY` 절에서 허용되는 컬럼으로 제한할 것이다(예: 다른 윈도우 함수는 안 됨). 물론 공통 테이블 표현식을 사용하여 그것도 고치려고 시도할 수 있거나, 우리는 다음을 고려한다

## 또 다른 문제??

그렇다, 물론! 윈도우 함수의 `ORDER BY` 절에 있는 컬럼이 무엇이어야 하는지 _알고_ 있는가? 그냥 무작위로 아무 컬럼이나 골랐는가? 그 컬럼에 인덱스가 없다면, 윈도우 함수가 여전히 잘 수행될까? 원래 `SELECT` 문에도 `ORDER BY` 절이 있다면 답은 쉽다, 아마 그것을 사용해야 할 것이다(해당되는 경우 `SELECT DISTINCT` 절의 모든 컬럼과 함께). 하지만 `ORDER BY` 절이 없다면? 또 다른 트릭! "상수" 변수를 사용하라:

```sql
-- 더 나은 SQL Server 동등 구문:
SELECT b.c1 AUTHOR_ID
FROM (
  SELECT AUTHOR_ID c1,
    ROW_NUMBER() OVER (ORDER BY @@version) rn
  FROM BOOK
) b
WHERE rn > 2 AND rn <= 3
```

그렇다, SQL Server에서는 상수가 그 `ORDER BY` 절에서 허용되지 않기 때문에 변수를 사용해야 한다. 고통스럽다, 알고 있다. 이 @@version 트릭에 대해 여기서 더 읽어보라.

## 이제 끝났나!?!?

아마 아닐 것이다 ;-) 하지만 우리는 아마 일반적인 경우와 엣지 케이스의 약 99%를 다뤘을 것이다. 이제 편하게 잘 수 있다. 이러한 모든 SQL 변환이 jOOQ에 구현되어 있다는 점에 유의하라. jOOQ는 SQL을 진지하게 받아들이는 _유일한_ SQL 추상화 프레임워크이며(모든 결점과 주의사항과 함께), 이 모든 혼란을 표준화한다. 처음에 언급했듯이, jOOQ에서는 그냥 이렇게 작성하면 된다:

```java
// 일반적인 에뮬레이션에 대해 걱정하지 마라
select().from(BOOK).limit(1).offset(2);

// 서브셀렉트의 중복 컬럼 이름에 대해 걱정하지 마라
select(BOOK.ID, AUTHOR.ID)
.from(BOOK)
.join(AUTHOR)
.on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
.limit(1).offset(2);

// 잘못된 IN 조건절에 대해 걱정하지 마라
select()
.from(BOOK)
.where(BOOK.AUTHOR_ID).in(
    select(AUTHOR.ID)
    .from(AUTHOR)
    .limit(1).offset(2)
);

// ROW_NUMBER() vs. DENSE_RANK() 구분에 대해 걱정하지 마라
selectDistinct(AUTHOR_ID)
    .from(BOOK).limit(1).offset(2);
```

jOOQ를 사용하면, SQL 배를 완전히 버리고 JPA로 넘어가지 않고도... 당신의 Oracle SQL 또는 Transact SQL을 마치 PostgreSQL처럼 멋지게 작성할 수 있다!

## 키셋 페이징

이제, 물론, 우리 블로그나 파트너 블로그인 SQL Performance Explained를 읽어왔다면, `OFFSET` 페이지네이션이 애초에 종종 나쁜 선택이라는 것을 이제 알아야 한다. 키셋 페이지네이션이 거의 항상 `OFFSET` 페이지네이션보다 성능이 좋다는 것을 알아야 한다. jOOQ가 SEEK 절을 사용하여 키셋 페이지네이션을 네이티브로 지원하는 방법에 대해 여기서 읽어보라.
