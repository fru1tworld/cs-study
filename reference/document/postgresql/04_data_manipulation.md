# Chapter 6. 데이터 조작 (Data Manipulation)

> PostgreSQL 18 공식 문서 번역
> 원문: https://www.postgresql.org/docs/18/dml.html

이전 챕터에서는 데이터를 저장할 테이블과 기타 구조를 생성하는 방법에 대해 논의했습니다. 이제 테이블에 데이터를 채울 차례입니다. 이 챕터에서는 테이블 데이터를 삽입, 업데이트, 삭제하는 방법을 다룹니다.

다음 챕터에서는 마침내 데이터베이스에서 오랫동안 잃어버린 데이터를 추출하는 방법을 설명할 것입니다.

---

## 목차

- [6.1. 데이터 삽입 (Inserting Data)](#61-데이터-삽입-inserting-data)
- [6.2. 데이터 업데이트 (Updating Data)](#62-데이터-업데이트-updating-data)
- [6.3. 데이터 삭제 (Deleting Data)](#63-데이터-삭제-deleting-data)
- [6.4. 수정된 행에서 데이터 반환 (Returning Data from Modified Rows)](#64-수정된-행에서-데이터-반환-returning-data-from-modified-rows)

---

## 6.1. 데이터 삽입 (Inserting Data)

> 원문: https://www.postgresql.org/docs/18/dml-insert.html

테이블이 생성되면 처음에는 데이터가 포함되어 있지 않습니다. 데이터베이스가 유용하려면 먼저 데이터를 삽입해야 합니다. 데이터는 개념적으로 한 번에 한 행씩 삽입됩니다. 물론 여러 행을 삽입할 수도 있지만, 완전한 행보다 적은 것을 삽입하는 방법은 없습니다. 일부 열의 값만 알고 있더라도 완전한 행을 생성해야 합니다.

### 기본 INSERT 문법

새 행을 생성하려면 `INSERT` 명령을 사용합니다. 이 명령은 테이블 이름과 열 값을 필요로 합니다.

예를 들어, 다음과 같은 products 테이블이 있다고 가정합니다:

```sql
CREATE TABLE products (
    product_no integer,
    name text,
    price numeric
);
```

데이터 행을 삽입하는 예제 명령은 다음과 같습니다:

```sql
INSERT INTO products VALUES (1, 'Cheese', 9.99);
```

데이터 값은 테이블에 열이 나타나는 순서대로 쉼표로 구분하여 나열됩니다. 일반적으로 데이터 값은 리터럴(상수)이지만, 스칼라 표현식도 허용됩니다.

### 열을 명시적으로 지정 (권장)

위의 문법은 테이블의 열 순서를 알아야 하는 단점이 있습니다. 이를 피하려면 열을 명시적으로 나열할 수 있습니다. 예를 들어, 다음 두 명령은 위와 동일한 효과를 가집니다:

```sql
INSERT INTO products (product_no, name, price) VALUES (1, 'Cheese', 9.99);
INSERT INTO products (name, price, product_no) VALUES ('Cheese', 9.99, 1);
```

많은 사용자들은 열 이름을 항상 나열하는 것이 좋은 관행이라고 생각합니다.

### 일부 열만 삽입하기

모든 열에 대한 값이 없는 경우 일부 열을 생략할 수 있습니다. 이 경우 생략된 열은 기본값으로 채워집니다. 예를 들어:

```sql
INSERT INTO products (product_no, name) VALUES (1, 'Cheese');
INSERT INTO products VALUES (1, 'Cheese');
```

두 번째 형식은 PostgreSQL의 확장입니다. 이 형식은 지정된 열을 왼쪽에서부터 채우고 나머지는 기본값으로 설정합니다.

### DEFAULT 값 명시적 사용

명확성을 위해 개별 열 또는 전체 행에 대해 기본값을 명시적으로 요청할 수 있습니다:

```sql
INSERT INTO products (product_no, name, price) VALUES (1, 'Cheese', DEFAULT);
INSERT INTO products DEFAULT VALUES;
```

### 여러 행 한 번에 삽입

단일 명령에서 여러 행을 삽입할 수 있습니다:

```sql
INSERT INTO products (product_no, name, price) VALUES
    (1, 'Cheese', 9.99),
    (2, 'Bread', 1.99),
    (3, 'Milk', 2.99);
```

### 쿼리 결과를 삽입

쿼리 결과(0개, 1개 또는 여러 행일 수 있음)를 삽입할 수도 있습니다:

```sql
INSERT INTO products (product_no, name, price)
  SELECT product_no, name, price FROM new_products
    WHERE release_date = 'today';
```

이것은 행을 삽입하기 위한 SQL의 전체 기능을 제공합니다.

### 대량 데이터 로딩에 대한 팁

대량의 데이터를 로딩할 때 `COPY` 명령을 사용하는 것이 권장됩니다. `INSERT` 명령만큼 유연하지는 않지만, 대량의 행을 로딩할 때 훨씬 더 효율적입니다. 자세한 내용은 [데이터베이스 채우기](https://www.postgresql.org/docs/18/populate.html)에서 대량 로딩 성능 향상에 대한 추가 정보를 참조하십시오.

---

## 6.2. 데이터 업데이트 (Updating Data)

> 원문: https://www.postgresql.org/docs/18/dml-update.html

데이터베이스에 이미 존재하는 데이터를 수정하는 것을 업데이트(updating)라고 합니다. 개별 행, 테이블의 모든 행, 또는 행의 부분집합을 업데이트할 수 있습니다. 각 열은 개별적으로 업데이트할 수 있으며, 다른 열은 영향을 받지 않습니다.

### UPDATE 명령의 필수 정보

기존 행을 업데이트하려면 `UPDATE` 명령을 사용합니다. 이를 위해서는 세 가지 정보가 필요합니다:

1. 테이블 이름과 업데이트할 열의 이름
2. 열의 새로운 값
3. 어떤 행을 업데이트할지 지정하는 조건

### 기본 예제

SQL은 일반적으로 행에 대한 고유 식별자를 제공하지 않습니다(참조: [6.3. 데이터 삭제](#63-데이터-삭제-deleting-data)). 따라서 업데이트할 행을 직접 지정하는 것이 항상 가능하지는 않습니다. 대신, 행이 일치해야 하는 조건을 지정합니다. 테이블에 기본 키가 있는 경우에만 (선언했는지 여부에 관계없이) 조건에서 기본 키를 선택하여 개별 행을 신뢰성 있게 지정할 수 있습니다. 그래픽 데이터베이스 액세스 도구는 행을 개별적으로 업데이트할 수 있도록 이 기능에 의존합니다.

#### 예제 1: 조건부 업데이트

예를 들어, 이 명령은 가격이 5인 모든 제품의 가격을 10으로 업데이트합니다:

```sql
UPDATE products SET price = 10 WHERE price = 5;
```

이로 인해 테이블에서 0개, 1개 또는 여러 행이 업데이트될 수 있습니다. 어떤 행도 일치하지 않는 업데이트를 시도해도 오류가 아닙니다.

#### 예제 2: 표현식을 사용한 업데이트

명령의 세부 사항을 살펴보겠습니다. 먼저 키워드 `UPDATE` 다음에 테이블 이름이 옵니다. 평소처럼 테이블 이름은 스키마로 한정될 수 있으며, 그렇지 않으면 경로에서 조회됩니다. 다음으로 키워드 `SET` 다음에 열 이름, 등호 및 새 열 값이 옵니다. 새 열 값은 상수뿐만 아니라 모든 스칼라 표현식이 될 수 있습니다. 예를 들어, 모든 제품의 가격을 10% 인상하려면:

```sql
UPDATE products SET price = price * 1.10;
```

보시다시피 새 값에 대한 표현식은 행의 기존 값을 참조할 수 있습니다. 또한 `WHERE` 절을 생략했습니다. 생략하면 테이블의 모든 행이 업데이트됩니다. 존재하는 경우 `WHERE` 절에서 `true`를 반환하는 행만 업데이트됩니다. `SET` 절의 등호는 할당이지만, `WHERE` 절의 등호는 비교입니다. 그러나 이것이 모호함을 만들지는 않습니다. 물론 `WHERE` 조건이 동등성 테스트일 필요는 없습니다. 다른 많은 연산자를 사용할 수 있습니다(Chapter 9 참조). 그러나 표현식은 부울 결과로 평가되어야 합니다.

#### 예제 3: 여러 열 동시 업데이트

`UPDATE` 명령에서 여러 열을 업데이트하려면 `SET` 절에 둘 이상의 할당을 나열하면 됩니다. 예를 들어:

```sql
UPDATE mytable SET a = 5, b = 3, c = 1 WHERE a > 0;
```

---

## 6.3. 데이터 삭제 (Deleting Data)

> 원문: https://www.postgresql.org/docs/18/dml-delete.html

지금까지 테이블에 데이터를 추가하고 데이터를 변경하는 방법을 설명했습니다. 남은 것은 더 이상 필요하지 않은 데이터를 제거하는 방법입니다. 데이터를 추가할 때 전체 행만 추가할 수 있었던 것처럼, 테이블에서 전체 행만 제거할 수 있습니다.

### 기본 개념

이전 섹션에서 SQL이 개별 행을 지정하는 방법을 제공하지 않는다고 설명했습니다. 따라서 행 제거는 제거할 행이 일치해야 하는 조건을 지정하여 수행됩니다. 테이블에 기본 키가 있는 경우 정확히 하나의 행을 지정할 수 있습니다. 그러나 조건을 일치시키는 행 그룹이나 테이블의 모든 행을 한 번에 제거할 수도 있습니다.

### DELETE 명령 사용

행을 제거하려면 `DELETE` 명령을 사용합니다. 문법은 `UPDATE` 명령과 매우 유사합니다.

#### 예제 1: 조건이 있는 삭제

예를 들어, products 테이블에서 가격이 10인 모든 행을 제거하려면:

```sql
DELETE FROM products WHERE price = 10;
```

#### 예제 2: 조건 없는 삭제 (주의 필요!)

단순히 다음과 같이 작성하면:

```sql
DELETE FROM products;
```

테이블의 모든 행이 삭제 됩니다! 프로그래머는 이 점을 주의해야 합니다(Caveat programmer).

---

## 6.4. 수정된 행에서 데이터 반환 (Returning Data from Modified Rows)

> 원문: https://www.postgresql.org/docs/18/dml-returning.html

때때로 데이터 수정 중에 수정되는 행의 데이터를 얻는 것이 유용합니다. `INSERT`, `UPDATE`, `DELETE`, `MERGE` 명령은 모두 이를 지원하는 선택적 `RETURNING` 절을 가지고 있습니다. `RETURNING`을 사용하면 단순히 데이터를 수정하기 위한 추가 데이터베이스 쿼리를 피할 수 있으며, 수정된 행을 신뢰성 있게 식별하기 어려울 때 특히 유용합니다.

### RETURNING 절의 내용

`RETURNING` 절에 허용되는 내용은 `SELECT` 명령의 출력 목록과 동일합니다(Section 7.3 참조). 명령의 대상 테이블의 열 이름이나 해당 열을 사용하는 값 표현식을 포함할 수 있습니다. 일반적인 축약형은 `RETURNING *`로, 대상 테이블의 모든 열을 순서대로 선택합니다.

### INSERT에서의 RETURNING

`INSERT`에서 반환에 사용할 수 있는 데이터는 기본적으로 삽입된 행입니다. 이는 중요한 기본값에 의존하는 경우 수정된 데이터 획득에 유용합니다. 예를 들어, 연속 열(serial column)을 제공하는 경우 `RETURNING`은 새 행에 할당된 값을 반환합니다:

```sql
CREATE TABLE users (firstname text, lastname text, id serial primary key);

INSERT INTO users (firstname, lastname) VALUES ('Joe', 'Cool') RETURNING id;
```

### UPDATE에서의 RETURNING

`UPDATE`에서 반환에 사용할 수 있는 데이터는 기본적으로 수정된 행의 새로운 내용입니다. 예를 들어:

```sql
UPDATE products SET price = price * 1.10
  WHERE price <= 99.99
  RETURNING name, price AS new_price;
```

### DELETE에서의 RETURNING

`DELETE`에서 반환에 사용할 수 있는 데이터는 기본적으로 삭제된 행의 내용입니다. 예를 들어:

```sql
DELETE FROM products
  WHERE obsoletion_date = 'today'
  RETURNING *;
```

### MERGE에서의 RETURNING

`MERGE`에서 반환에 사용할 수 있는 데이터는 소스 행의 내용과 삽입, 업데이트 또는 삭제된 대상 행의 내용입니다. 소스와 대상이 많은 동일한 열을 가지고 있을 가능성이 높으므로, 반환할 열을 지정하기 위해 `RETURNING *`를 사용하면 많은 중복 열이 발생할 수 있습니다. 따라서 테이블 이름을 지정하여 대상의 해당 열만 또는 소스의 해당 열만 반환하는 것이 더 효율적입니다:

```sql
MERGE INTO products p USING new_products n ON p.product_no = n.product_no
  WHEN NOT MATCHED THEN INSERT VALUES (n.product_no, n.name, n.price)
  WHEN MATCHED THEN UPDATE SET name = n.name, price = n.price
  RETURNING p.*;
```

### 구(OLD) 값과 신(NEW) 값 반환

열에 할당된 새 값 대신 이전 값을 반환하려면 `old` 또는 `new` 열 한정자를 사용하여 원하는 데이터를 명시적으로 지정할 수 있습니다. 예를 들어:

```sql
UPDATE products SET price = price * 1.10
  WHERE price <= 99.99
  RETURNING name, old.price AS old_price, new.price AS new_price,
            new.price - old.price AS price_change;
```

- INSERT의 경우: 일반적으로 `old` 값은 NULL입니다.
- DELETE의 경우: 일반적으로 `new` 값은 NULL입니다.

단, 예외가 있습니다:
- `ON CONFLICT DO UPDATE`의 경우 충돌된 행에서 `old`가 null이 아닐 수 있습니다.
- 재작성 규칙(rule)에 의해 명령이 변환된 경우에도 예외가 발생할 수 있습니다.

### 트리거와의 상호작용

대상 테이블에 트리거가 있는 경우, 반환에 사용할 수 있는 데이터는 트리거에 의해 수정된 행입니다. 따라서 트리거에서 계산한 열을 검사하는 것이 `RETURNING`의 또 다른 일반적인 사용 사례입니다.

---

## 요약

| 명령 | 용도 | 주요 특징 |
|------|------|----------|
| `INSERT` | 새 행 삽입 | 단일/다중 행 삽입, DEFAULT 값 지원, SELECT 결과 삽입 가능 |
| `UPDATE` | 기존 행 수정 | WHERE 절로 대상 행 지정, 표현식으로 새 값 계산 가능 |
| `DELETE` | 행 삭제 | WHERE 절 없으면 모든 행 삭제됨에 주의 |
| `RETURNING` | 수정된 데이터 반환 | INSERT/UPDATE/DELETE/MERGE와 함께 사용, old/new 한정자 지원 |

---

## 참고 링크

- [PostgreSQL 18 공식 문서 - Data Manipulation](https://www.postgresql.org/docs/18/dml.html)
- [PostgreSQL 18 공식 문서 - INSERT](https://www.postgresql.org/docs/18/dml-insert.html)
- [PostgreSQL 18 공식 문서 - UPDATE](https://www.postgresql.org/docs/18/dml-update.html)
- [PostgreSQL 18 공식 문서 - DELETE](https://www.postgresql.org/docs/18/dml-delete.html)
- [PostgreSQL 18 공식 문서 - RETURNING](https://www.postgresql.org/docs/18/dml-returning.html)
