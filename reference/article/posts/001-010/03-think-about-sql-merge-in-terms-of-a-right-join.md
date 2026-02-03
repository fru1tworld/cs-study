# SQL MERGE를 RIGHT JOIN의 관점에서 생각하기

> 원문: [Think About SQL MERGE in Terms of a RIGHT JOIN](https://blog.jooq.org/think-about-sql-merge-in-terms-of-a-right-join/)

`RIGHT JOIN`은 SQL 언어에서 잘 쓰이지 않는 기능이며, 실제 현업에서는 거의 볼 수 없습니다. 거의 모든 `RIGHT JOIN`은 동등한 `LEFT JOIN`으로 표현할 수 있기 때문입니다. 다음 두 구문은 동일합니다:

```sql
-- 일반적인 방식
SELECT c.first_name, c.last_name, p.amount
FROM customer AS c
LEFT JOIN payment AS p ON c.customer_id = p.customer_id

-- 잘 쓰이지 않는 방식
SELECT c.first_name, c.last_name, p.amount
FROM payment AS p
RIGHT JOIN customer AS c ON c.customer_id = p.customer_id
```

이 두 구문이 논리적으로 동일하다는 점을 고려하면, 대부분의 RDBMS에서 동일한 실행 계획을 생성할 것이라고 기대하는 것은 무리가 아닙니다. 우리는 왼쪽에서 오른쪽으로, 위에서 아래로 읽는 것에 익숙하기 때문에, `RIGHT JOIN`이 가까운 시일 내에 더 대중화될 것이라고는 생각하지 않습니다.

하지만 SQL 언어에서 `RIGHT JOIN`이 놀라울 정도로 보편적으로 사용되는 곳이 하나 있습니다!

## MERGE 구문

왜 이것이 놀라운 일일까요? 그 곳에서는 두 테이블을 조인하는 데 동일한 문법을 사용하지 않기 때문입니다. 하지만 `MERGE` 구문에서 일어나는 일이 바로 그것입니다. 다음과 같은 `MERGE` 구문을 살펴보겠습니다:

- 데이터를 로드할 스테이징 테이블 (`SOURCE` 테이블)
- 데이터를 저장할 일반 테이블 (`TARGET` 테이블)

스키마는 다음과 같습니다:

```sql
CREATE TABLE book_to_book_store (
  book_id BIGINT NOT NULL REFERENCES book,
  name TEXT NOT NULL REFERENCES book_store,
  stock INT NOT NULL,

  PRIMARY KEY (book_id, name)
);

CREATE TABLE book_to_book_store_staging AS
SELECT * FROM book_to_book_store
WITH NO DATA;
```

ETL 작업에서 전형적으로 볼 수 있는 쿼리입니다:

```sql
-- 타겟 테이블
MERGE INTO book_to_book_store AS t

-- 소스 테이블
USING book_to_book_store_staging AS s

-- RIGHT JOIN 조건
ON t.book_id = s.book_id AND t.name = s.name

-- RIGHT JOIN 매칭 결과에 따른 각 행의 액션
WHEN MATCHED THEN UPDATE SET stock = s.stock
WHEN NOT MATCHED THEN INSERT (book_id, name, stock)
VALUES (s.book_id, s.name, s.stock);
```

이것은 단순히 `BOOK_TO_BOOK_STORE_STAGING` 테이블의 모든 행을 가져와서 `BOOK_TO_BOOK_STORE`에 병합하는 것입니다:

- 행이 이미 존재하는 경우 (`MATCH`가 있는 경우), `STOCK`이 업데이트됩니다
- 행이 아직 존재하지 않는 경우 (`MATCH`가 없는 경우), 행이 삽입됩니다

하지만 우리는 source -> target 순서의 문법을 사용하지 않습니다. 먼저 타겟 테이블 `BOOK_TO_BOOK_STORE`를 지정하고, 그 다음에 `BOOK_TO_BOOK_STORE_STAGING` 테이블을 `RIGHT JOIN`합니다. 다음과 같이 생각해 보세요:

```sql
SELECT *
FROM book_to_book_store AS t
RIGHT JOIN book_to_book_store_staging AS s
ON t.book_id = s.book_id AND t.name = s.name
```

그리고 `RIGHT JOIN`을 벤 다이어그램이 아닌 다음과 같은 카테시안 곱으로 생각하면, 각 `MATCH` 또는 비`MATCH`별로 어떤 작업이 수행되는지 쉽게 알 수 있습니다:

| t.name | t.book_id | t.stock | s.name | s.book_id | s.stock | |
|--------|-----------|---------|--------|-----------|---------|---|
| | | | Faraway Land | 1 | 9 | <-- NOT MATCHED |
| Faraway Land | 2 | 10 | Faraway Land | 2 | 12 | <-- MATCHED |
| Faraway Land | 3 | 10 | Faraway Land | 3 | 5 | <-- MATCHED |
| | | | Paper Trail | 1 | 1 | <-- NOT MATCHED |
| Paper Trail | 3 | 2 | Paper Trail | 3 | 0 | <-- MATCHED |

`RIGHT JOIN`에서 항상 그렇듯이, 오른쪽의 모든 행은 왼쪽의 매칭되는 행과 결합되거나, 매칭되는 행이 없으면 `NULL` 값의 빈 행과 결합됩니다. 이 `MERGE` 이후, 결과 데이터는 다음과 같이 업데이트되어야 합니다:

| t.name | t.book_id | t.stock | s.name | s.book_id | s.stock | |
|--------|-----------|---------|--------|-----------|---------|---|
| Faraway Land | 1 | 9 | Faraway Land | 1 | 9 | <-- NOT MATCHED |
| Faraway Land | 2 | 12 | Faraway Land | 2 | 12 | <-- MATCHED |
| Faraway Land | 3 | 5 | Faraway Land | 3 | 5 | <-- MATCHED |
| Faraway Land | 1 | 1 | Paper Trail | 1 | 1 | <-- NOT MATCHED |
| Paper Trail | 3 | 0 | Paper Trail | 3 | 0 | <-- MATCHED |

이것이 `MERGE` 구문이 작동하는 방식입니다.

참고로, 앞서 `JOIN`이 카테시안 곱을 생성한다고 말했습니다. 하지만 `SELECT` 구문과 달리, `MERGE`에는 카테시안 곱이 `TARGET` 행당 중복 매칭을 생성해서는 안 된다는 제한이 있습니다. `TARGET` 행 하나에 여러 `SOURCE` 행이 매칭되면 액션의 순서가 정의되지 않기 때문입니다.

## 행 삭제하기

`MERGE`는 단순히 `INSERT`와 `UPDATE`를 수행하는 것보다 더 강력합니다. 행을 `DELETE`할 수도 있습니다. 스테이징 테이블에서 `STOCK = 0`이 `STOCK`을 `0`으로 설정하는 것이 아니라 해당 행을 삭제해야 한다는 의미라고 가정해 보겠습니다. 그러면 다음과 같이 작성할 수 있습니다:

```sql
MERGE INTO book_to_book_store AS t
USING book_to_book_store_staging AS s
ON t.book_id = s.book_id AND t.name = s.name
WHEN MATCHED AND s.stock = 0 THEN DELETE
WHEN MATCHED THEN UPDATE SET stock = s.stock
WHEN NOT MATCHED THEN INSERT (book_id, name, stock)
VALUES (s.book_id, s.name, s.stock);
```

이제 위의 스테이징 데이터로, 마지막 행을 업데이트하는 대신 삭제하게 됩니다:

| t.name | t.book_id | t.stock | s.name | s.book_id | s.stock | |
|--------|-----------|---------|--------|-----------|---------|---|
| Faraway Land | 1 | 9 | Faraway Land | 1 | 9 | <-- NOT MATCHED : INSERT |
| Faraway Land | 2 | 10 | Faraway Land | 2 | 12 | <-- MATCHED : UPDATE |
| Faraway Land | 3 | 10 | Faraway Land | 3 | 5 | <-- MATCHED : UPDATE |
| Paper Trail | 1 | 1 | Paper Trail | 1 | 1 | <-- NOT MATCHED : INSERT |
| | | | Paper Trail | 3 | 0 | <-- MATCHED : DELETE |

`RIGHT JOIN`의 의미론은 여전히 동일하며, `WHEN MATCHED` 절의 추가적인 `AND` 조건에 따라 액션만 달라집니다.

## 소스 기준 매칭

일부 RDBMS는 `MERGE`의 더욱 강력한 벤더 고유 변형을 지원하며, 제 의견으로는 이것이 IEC/ISO 9075 표준에 추가되어야 합니다. 바로 `BY TARGET` / `BY SOURCE` 절입니다. 다음 구문을 살펴보겠습니다:

```sql
MERGE INTO book_to_book_store AS t
USING book_to_book_store_staging AS s
ON t.book_id = s.book_id AND t.name = s.name
WHEN MATCHED THEN UPDATE SET stock = s.stock
WHEN NOT MATCHED BY TARGET THEN INSERT (book_id, name, stock)
VALUES (s.book_id, s.name, s.stock)
WHEN NOT MATCHED BY SOURCE THEN DELETE;
```

`WHEN NOT MATCHED BY SOURCE` 절을 추가하면 `RIGHT JOIN` 연산이 `FULL JOIN` 연산으로 전환되는 간단한 효과가 있습니다. 다음과 같이 생각해 보세요:

```sql
SELECT *
FROM book_to_book_store AS t
FULL JOIN book_to_book_store_staging AS s
ON t.book_id = s.book_id AND t.name = s.name
```

이제 결과는 다음과 같을 수 있습니다:

| t.name | t.book_id | t.stock | s.name | s.book_id | s.stock | |
|--------|-----------|---------|--------|-----------|---------|---|
| | | | Faraway Land | 1 | 9 | <-- NOT MATCHED BY TARGET |
| Faraway Land | 2 | 10 | Faraway Land | 2 | 12 | <-- MATCHED |
| Faraway Land | 3 | 10 | Faraway Land | 3 | 5 | <-- MATCHED |
| | | | Paper Trail | 1 | 1 | <-- NOT MATCHED BY TARGET |
| Paper Trail | 3 | 2 | | | | <-- NOT MATCHED BY SOURCE |

`NOT MATCHED BY TARGET`과 `NOT MATCHED BY SOURCE`라는 용어는 위와 같이 시각화했을 때 매우 자명하며, 아마도 초보자에게 `LEFT JOIN`과 `RIGHT JOIN`보다 덜 혼란스러울 것입니다. SQL 문법이 `OUTER JOIN`에서 발생하는 `NULL` 값이 다음 중 어떤 경우인지 구분할 수 있도록 개선된다면 좋겠습니다:

- 소스 데이터에 `NULL` 값이 포함된 경우
- `OUTER JOIN`의 "반대편"에 의해 `NOT MATCHED`된 행인 경우

다음과 같은 가상의 문법을 상상해 보세요:

```sql
SELECT c.first_name, c.last_name, p.amount
FROM customer AS c
LEFT JOIN payment AS p ON c.customer_id = p.customer_id
WHERE p IS NOT MATCHED BY JOIN -- 사실상 ANTI JOIN
```

어쨌든...

행을 삭제할 때, 이 접근 방식은 `STOCK = 0`이 삭제를 의미하는 것처럼 데이터의 의미론적 해석에 의존하는 것보다 훨씬 편리합니다. 이제 `SOURCE` 테이블(스테이징 테이블)에서 _부재하는_ 행이 있으며, 이것은 우리가 그렇게 모델링하고자 한다면 단순히 해당 행이 삭제되어야 함을 의미합니다. 따라서 위의 `MERGE` 구문을 실행한 후, 다시 다음과 같은 결과를 얻게 됩니다:

| t.name | t.book_id | t.stock | s.name | s.book_id | s.stock | |
|--------|-----------|---------|--------|-----------|---------|---|
| Faraway Land | 1 | 9 | Faraway Land | 1 | 9 | <-- NOT MATCHED BY TARGET : INSERT |
| Faraway Land | 2 | 12 | Faraway Land | 2 | 12 | <-- MATCHED : UPDATE |
| Faraway Land | 3 | 5 | Faraway Land | 3 | 5 | <-- MATCHED : UPDATE |
| Faraway Land | 1 | 1 | Paper Trail | 1 | 1 | <-- NOT MATCHED BY TARGET : INSERT |
| | | | | | | <-- NOT MATCHED BY SOURCE : DELETE |

최소한 다음 RDBMS들이 `BY SOURCE` 및 `BY TARGET` 절을 지원합니다:

- Databricks
- Firebird 5
- PostgreSQL 17
- SQL Server

이것이 얼마나 유용한지를 고려하면, 더 많은 RDBMS가 이 T-SQL 문법을 곧 채택할 것으로 예상합니다. jOOQ 3.20에서 이에 대한 지원이 추가되었으며, 향후 jOOQ 버전에서는 `FULL JOIN`을 `USING` 절로 이동시켜 이를 에뮬레이션할 수 있을 것입니다.
