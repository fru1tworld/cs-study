# 난해한 SQL MERGE 문의 다양한 변형

> 원문: https://blog.jooq.org/the-many-flavours-of-the-arcane-sql-merge-statement/

"SQL MERGE 문은 그 신비로움이 오직 그 강력함에 의해서만 능가되는 도구이다." MERGE 문은 삽입(INSERT), 갱신(UPDATE), 삭제(DELETE) 연산을 하나의 원자적 작업으로 결합한다. 이 문은 대상 테이블과 소스 데이터 간의 레코드를 매칭한 다음, 매치 존재 여부에 따라 조건부 로직을 실행함으로써 정교한 데이터 동기화를 가능하게 한다.

## 테이블 생성 및 설정

```sql
DROP TABLE IF EXISTS prices;
DROP TABLE IF EXISTS staging;

CREATE TABLE prices (
  product_id BIGINT NOT NULL PRIMARY KEY,
  price DECIMAL(10, 2) NOT NULL,
  price_date DATE NOT NULL,
  update_count BIGINT NOT NULL
);

CREATE TABLE staging (
  product_id BIGINT NOT NULL PRIMARY KEY,
  price DECIMAL(10, 2) NOT NULL
);
```

## 초기 데이터 로드

```sql
DELETE FROM prices;
DELETE FROM staging;
INSERT INTO staging
VALUES (1, 100.00),
       (2, 125.00),
       (3, 150.00);
```

## 두 번째 스테이징 로드

```sql
DELETE FROM staging;
INSERT INTO staging
VALUES (1, 100.00),
       (2,  99.00),
       (4, 300.00);
```

## MERGE의 네 가지 핵심 구성 요소

### 1. 대상 테이블 정의

"INSERT 문과 마찬가지로, 데이터를 MERGE INTO 할 위치를 정의할 수 있다. 이것이 가장 간단한 부분이다." MERGE INTO 절에서 별칭과 함께 변경을 수신할 테이블을 지정한다.

### 2. 소스 데이터 (USING 절)

USING 키워드는 병합될 소스 테이블 데이터를 감싼다. 단순히 스테이징 테이블의 이름을 지정하는 대신, 조인과 변환을 통해 소스 데이터를 먼저 보강할 수 있다. 이 예제에서는 기존 레코드와 새 레코드 간의 매칭을 생성하기 위해 FULL JOIN을 사용하여, 두 데이터셋에서 어떤 행이 일치하는지 보여준다.

### 3. 조인 조건 (ON 절)

다음으로, 일반적인 JOIN 문법과 동일하게 ON 절을 사용하여 대상 테이블과 소스 테이블을 조인한다. MERGE는 항상 RIGHT JOIN 의미론을 사용하므로, 매치되지 않은 소스 행도 결과에 나타난다. 매칭 키는 테이블 간에 어떤 레코드가 대응하는지를 결정한다.

### 4. 조건부 액션 (WHEN 절)

"이제 흥미로운 부분이 시작된다!" 여러 WHEN 절이 각 행에 대해 순차적으로 실행된다. 매치된 행은 술어(predicate)에 따라 UPDATE 또는 DELETE 액션을 트리거한다. 매치되지 않은 행은 INSERT 액션을 트리거한다. 중요한 점은, 행당 하나의 절만 적용된다는 것이다—첫 번째로 매칭되는 절이 실행되면, 처리는 다음 행으로 이동한다.

## USING 절 쿼리 (예시)

```sql
SELECT
  COALESCE(p.product_id, s.product_id) AS product_id,
  p.price AS old_price,
  s.price AS new_price
FROM prices AS p
FULL JOIN staging AS s ON p.product_id = s.product_id
ORDER BY product_id
```

첫 번째 FULL JOIN 후 소스 데이터:

| PRODUCT_ID | OLD_PRICE | NEW_PRICE |
|------------|-----------|-----------|
| 1          | 100.00    | 100.00    |
| 2          | 125.00    | 99.00     |
| 3          | 150.00    | (null)    |
| 4          | (null)    | 300.00    |

## 표준 MERGE 문 (Db2/표준 준수)

```sql
MERGE INTO prices AS p
USING (
  SELECT COALESCE(p.product_id, s.product_id) AS product_id, s.price
  FROM prices AS p
  FULL JOIN staging AS s ON p.product_id = s.product_id
) AS s
ON (p.product_id = s.product_id)
WHEN MATCHED AND s.price IS NULL THEN DELETE
WHEN MATCHED AND p.price != s.price THEN UPDATE SET
  price = s.price,
  price_date = CURRENT_DATE,
  update_count = update_count + 1
WHEN NOT MATCHED THEN INSERT
  (product_id, price, price_date, update_count)
VALUES
  (s.product_id, s.price, CURRENT_DATE, 0);
```

MERGE 후 최종 prices 테이블:

| PRODUCT_ID | PRICE  | PRICE_DATE | UPDATE_COUNT |
|------------|--------|------------|--------------|
| 1          | 100.00 | 2020-04-09 | 0            |
| 2          | 99.00  | 2020-04-09 | 1            |
| 4          | 300.00 | 2020-04-09 | 0            |

## 지원되는 데이터베이스

Db2, Derby, Firebird, H2, HSQLDB, Oracle, SQL Server, Sybase SQL Anywhere, Teradata, Vertica가 MERGE를 지원한다. "이 목록에 PostgreSQL은 포함되어 있지 않다."

## 데이터베이스별 특이사항

### AND 절 지원

대부분의 데이터베이스는 WHEN 절 내에서 AND 술어를 지원하여, 액션 실행 전에 여러 조건을 가능하게 한다. 그러나 HSQLDB, Oracle, SQL Server, Teradata에는 제한이 있다. AND 지원이 없으면, 조건을 UPDATE SET 절 내의 CASE 표현식으로 변환해야 하며, 여러 컬럼 할당에 걸쳐 술어를 반복해야 한다.

### 다중 WHEN 절

모든 데이터베이스가 여러 개의 WHEN MATCHED 또는 WHEN NOT MATCHED 절을 허용하는 것은 아니다. 다중 절을 지원하는 데이터베이스는 하나가 매치될 때까지 순차적으로 실행한다. 각 유형당 하나의 절로 제한하는 데이터베이스에서는 모든 가능한 조건에 따라 조건부로 컬럼을 수정하는 CASE 표현식을 통한 에뮬레이션이 필요하다.

### 멱등성 규칙

"모든 행은 한 번만 갱신된다." 여러 WHEN MATCHED 절을 작성할 때, 각 후속 절은 암묵적으로 모든 이전 술어의 부정을 포함한다. 작성된 조건은 단일 행이 여러 절과 매치되는 것을 방지하기 위해 내부적으로 자동 보강된다. 이는 예측 가능하고 결정론적인 동작을 보장한다.

### H2와 HSQLDB 특이점

이 데이터베이스들은 "모든 행은 한 번만 갱신된다" 규칙을 강제하지 않아 SQL 표준을 위반한다. 표준 준수를 달성하려면, 후속 술어에 수동으로 부정 로직을 추가하거나 UPDATE 할당 전체에 CASE 표현식을 사용해야 한다.

### Oracle의 고유한 의미론

Oracle은 AND 지원이 없지만 UPDATE 뒤에 WHERE 절을 제공한다. 흥미롭게도 UPDATE와 DELETE 절이 함께 실행되며, DELETE는 UPDATE 이후에 발생한다. 이는 같은 행이 두 번 처리됨을 의미한다—먼저 갱신되고, 그 다음 삭제 여부가 평가된다. 이 두 단계 동작은 에뮬레이션에 어려움을 만들고 예상치 못한 결과를 초래할 수 있다.

### SQL Server의 BY SOURCE/BY TARGET 확장

SQL Server는 WHEN NOT MATCHED BY TARGET (기본값)과 WHEN NOT MATCHED BY SOURCE 옵션으로 표준 문법을 향상시킨다. BY TARGET은 대상 매치가 없는 소스 행을 의미한다. BY SOURCE는 소스 매치가 없는 대상 행을 의미한다. 이는 효과적으로 FULL OUTER JOIN 의미론을 구현하여, USING 절에서 보조 조인이 필요 없이 전체 동기화 시나리오를 더 단순한 문법으로 가능하게 한다.

### Vertica의 제한사항

Vertica는 특히 DELETE 브랜치 지원이 없어, MERGE를 INSERT와 UPDATE 연산으로만 제한한다. 이 생략은 전체 동기화 기능을 제한하지만, 대부분의 실용적인 사용 사례는 커버한다.

## 기능 비교 테이블

| 데이터베이스 | AND 지원 | 다중 WHEN MATCHED | DELETE 브랜치 | BY SOURCE/BY TARGET |
|--------------|----------|-------------------|---------------|---------------------|
| Db2          | Yes      | Yes               | Yes           | No                  |
| H2           | Yes      | Yes*              | Yes           | No                  |
| HSQLDB       | No       | No                | Yes           | No                  |
| Oracle       | No (WHERE) | No              | Yes           | No                  |
| SQL Server   | Yes      | No                | Yes           | Yes                 |
| Teradata     | No       | No                | Yes           | No                  |
| Vertica      | Yes      | Yes               | No            | No                  |

*H2는 "모든 행은 한 번만 갱신된다" 표준 규칙을 위반한다

## 에뮬레이션 전략

### AND를 CASE 표현식으로 변환

데이터베이스가 WHEN 절 내에서 AND를 지원하지 않을 때, 각 술어를 CASE 표현식으로 변환한다. 수정될 수 있는 모든 컬럼에 대해, 모든 조건을 평가하는 CASE 문을 생성한다. 특정 조건에 의해 변경되지 않는 컬럼은 현재 값을 그대로 반환하여, 해당 조건이 적용되지 않을 때 데이터를 보존한다.

### AND 지원 없는 에뮬레이션 (CASE 표현식 방식)

```sql
WHEN MATCHED THEN UPDATE SET
  price = CASE
    WHEN p.price != s.price THEN s.price
    ELSE p.price
  END,
  price_date = CASE
    WHEN p.price != s.price THEN CURRENT_DATE
    ELSE p.price_date
  END,
  update_count = CASE
    WHEN p.price != s.price THEN update_count + 1
    ELSE p.update_count
  END
WHEN NOT MATCHED THEN INSERT
  (product_id, price, price_date, update_count)
VALUES
  (s.product_id, s.price, CURRENT_DATE, 0);
```

### 다중 절 제한 처리

여러 WHEN MATCHED 절을 지정할 수 없는 경우, 향상된 UPDATE 로직을 가진 단일 절로 결합한다. 각 원래 절의 술어는 컬럼 할당 내의 CASE 조건이 된다. 이 접근 방식은 구문적 제한에도 불구하고 동등한 결과를 달성한다.

### 연쇄 술어

다중 절을 에뮬레이션할 때, AND NOT 연산자를 통해 부정 로직을 중첩한다. 두 번째 절의 조건은 "NOT first_condition AND second_condition"이 되고, 세 번째는 "NOT first AND NOT second AND third"가 되는 식이다. 이 연쇄는 모든 브랜치 간의 상호 배타성을 보장한다.

## SQL Server MERGE (BY SOURCE/BY TARGET 사용)

```sql
MERGE INTO prices AS p
USING staging AS s
ON (p.product_id = s.product_id)
WHEN NOT MATCHED BY SOURCE THEN DELETE
WHEN MATCHED AND p.price != s.price THEN UPDATE SET
  price = s.price,
  price_date = getdate(),
  update_count = update_count + 1
WHEN NOT MATCHED BY TARGET THEN INSERT
  (product_id, price, price_date, update_count)
VALUES
  (s.product_id, s.price, getdate(), 0);
```

## Oracle 우회 방법 (DELETE WHERE 사용)

```sql
WHEN MATCHED
THEN UPDATE SET
  c1 = CASE
    WHEN p1 THEN 1
    WHEN p3 THEN 3
    ELSE c1
  END,
  c2 = CASE
    WHEN p3 THEN 3
    ELSE c2
  END
WHERE p1 OR p2 OR p3 OR p4
DELETE WHERE
  NOT p1 AND p2 OR
  NOT p1 AND NOT p2 AND NOT p3 AND p4
```

## 실용적 동기화 예제

이 글은 완전한 워크플로우를 보여준다: 스테이징에는 초기에 세 개의 제품이 포함되어 있다. 후속 스테이징 리로드는 갱신된 제품과 새 제품을 제시하면서, 다른 제품을 제거한다. MERGE 문은 진정으로 새로운 제품의 삽입, 가격이 변경된 경우에만 갱신, 더 이상 스테이징에 없는 제품의 삭제를 처리한다—델타 로딩이 아닌 전체 동기화를 구현한다.

## 결론

MERGE를 이해하려면 개념적 로직과 벤더별 구문 변형 모두를 파악해야 한다. 표준 SQL MERGE는 우아하고 강력하지만, 데이터베이스 방언 변형은 구현을 복잡하게 만든다. jOOQ 3.14부터 프레임워크가 데이터베이스 간 에뮬레이션을 처리하여, 개발자가 여러 데이터베이스에서 자동으로 변환되는 표준 준수 MERGE 문을 작성할 수 있게 한다.
