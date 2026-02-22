# SQL에서 느슨하게 관련된 두 테이블을 연결하는 빠르고 간단한 방법

> 원문: https://blog.jooq.org/a-quick-and-dirty-way-to-concatenate-two-vaguely-related-tables-in-sql/

가끔씩 `NATURAL JOIN` 연산자의 사용 사례를 마주치게 되는데, 특히 `NATURAL FULL JOIN`과 결합하여 사용할 때 그렇다. 이전 블로그 글에서는 이 기법을 사용하여 테이블을 비교하는 방법을 탐구한 적이 있다.

최근에 Reddit의 한 사용자가 이런 질문을 했다: "서로 전혀 관계가 없는 두 테이블을 UNION처럼 동작하도록 조인할 수 있는 방법이 있나요?" 처음에는 `UNION CORRESPONDING` 구문을 생각했는데, 이는 SQL 표준 기능임에도 불구하고 대부분의 SQL 방언에서 아직 사용할 수 없다. 그러나 `NATURAL FULL JOIN`이 이 문제를 다른 방식으로 해결할 수 있다는 것을 깨달았다 - 조인된 테이블들이 일치하는 행을 절대 생성하지 않도록 하여 UNION과 유사한 동작을 달성하는 것이다.

## Sakila 데이터베이스 예제

Sakila 데이터베이스에는 사람과 관련된 세 개의 테이블이 있다: `ACTOR`, `CUSTOMER`, 그리고 `STAFF`. 이 테이블들은 `FIRST_NAME`, `LAST_NAME`, `LAST_UPDATE` 컬럼만 공유하고, 나머지 컬럼들은 각 테이블에 고유하다.

### 테이블 정의

```sql
CREATE TABLE actor (
  actor_id integer NOT NULL PRIMARY KEY,
  first_name varchar(45) NOT NULL,
  last_name varchar(45) NOT NULL,
  last_update timestamp NOT NULL
);

CREATE TABLE customer (
  customer_id integer NOT NULL PRIMARY KEY,
  store_id integer NOT NULL,
  first_name varchar(45) NOT NULL,
  last_name varchar(45) NOT NULL,
  email varchar(50),
  address_id integer NOT NULL,
  active boolean NOT NULL,
  create_date date NOT NULL,
  last_update timestamp
);

CREATE TABLE staff (
  staff_id integer NOT NULL,
  first_name varchar(45) NOT NULL,
  last_name varchar(45) NOT NULL,
  address_id integer NOT NULL,
  email varchar(50),
  store_id integer NOT NULL,
  active boolean NOT NULL,
  username varchar(16) NOT NULL,
  password varchar(40),
  last_update timestamp NOT NULL,
  picture bytea
);
```

### 쿼리

```sql
SELECT *
FROM (SELECT 'actor' AS source, * FROM actor) AS a
NATURAL FULL JOIN (SELECT 'customer' AS source, * FROM customer) AS c
NATURAL FULL JOIN (SELECT 'staff' AS source, * FROM staff) AS s;
```

## 결과 및 관찰

이 쿼리는 일치하는 컬럼들이 먼저 나타나는 출력을 생성하며, 여기에는 합성된 `SOURCE` 컬럼이 포함된다. `SOURCE`가 각 조인 소스마다 다르기 때문에 실제 매칭이 발생하지 않으며, 원하는 UNION 의미론을 달성하게 된다. 테이블별 고유 컬럼들은 그 뒤에 나타나며, 해당 행에 관련된 데이터만 포함한다.

출력 결과의 예시 행을 보면, actor 행에는 customer/staff 컬럼이 null이고, customer 행에는 actor/staff 컬럼이 null이며, staff 행에는 actor/customer 컬럼이 null인 것을 확인할 수 있다.

### 작동 원리

각 서브쿼리에 서로 다른 값을 가진 합성 `SOURCE` 컬럼을 추가한다. 이 컬럼은 각 소스 테이블마다 다른 값(`'actor'`, `'customer'`, `'staff'`)을 포함하므로, `NATURAL FULL JOIN`이 행을 매칭하려 할 때 절대로 일치하지 않는다. 이로 인해 실제 조인이 아닌 연결(concatenation)이 이루어지며, `NATURAL FULL JOIN`을 활용하여 UNION 의미론을 달성하게 된다.

## 타입 충돌 문제

실제 Sakila 스키마에는 충돌이 존재한다: `CUSTOMER` 테이블의 `active` 컬럼은 정수(integer)로 정의되어 있고, `STAFF` 테이블의 `active` 컬럼은 불리언(boolean)으로 정의되어 있다. 이로 인해 다음과 같은 오류가 발생한다: "JOIN/USING types integer and boolean cannot be matched."

### 해결 방법

```sql
WITH customer AS (
  SELECT
    customer_id,
    store_id,
    first_name,
    last_name,
    email,
    address_id,
    activebool as active,
    create_date,
    last_update
  FROM customer
)

SELECT *
FROM (SELECT 'actor' AS source, * FROM actor) AS a
NATURAL FULL JOIN (SELECT 'customer' AS source, * FROM customer) AS c
NATURAL FULL JOIN (SELECT 'staff' AS source, * FROM staff) AS s;
```

CTE(공통 테이블 표현식)를 사용하여 `CUSTOMER` 테이블의 `active` 컬럼을 불리언 타입으로 캐스팅함으로써 타입 충돌 문제를 해결할 수 있다.

## 마무리

이 기법은 일상적인 SQL이라기보다는 다소 난해하고 상황에 따라 사용되는 방법이다. BigQuery의 `* REPLACE (...)` 구문이 더 보편적으로 사용 가능했으면 하는 바람이 있는데, 이를 통해 이러한 변환을 훨씬 단순화할 수 있기 때문이다. 전문적인 성격에도 불구하고, `NATURAL FULL JOIN`은 가끔 발생하는 테이블 연결 문제를 우아하게 해결해 주므로 그 가치를 인정받을 만하다.
