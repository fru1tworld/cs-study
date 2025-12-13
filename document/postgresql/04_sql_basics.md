# PostgreSQL SQL 기본 문법

> 공식 문서: https://www.postgresql.org/docs/current/sql.html

---

## SQL 구문 기초

### 어휘 구조

```sql
-- SQL은 대소문자 구분 없음 (키워드)
SELECT * FROM users;
select * from users;

-- 식별자는 소문자로 변환됨
CREATE TABLE MyTable (...);  -- mytable로 저장

-- 대소문자 유지하려면 따옴표 사용
CREATE TABLE "MyTable" (...);  -- MyTable로 저장
```

### 주석

```sql
-- 한 줄 주석

/* 여러 줄
   주석 */
```

### 문자열 상수

```sql
-- 작은따옴표 사용
SELECT 'Hello World';

-- 작은따옴표 이스케이프
SELECT 'It''s a test';
SELECT E'It\'s a test';  -- 이스케이프 문자열

-- 달러 인용
SELECT $$String with 'quotes'$$;
SELECT $tag$Another way$tag$;
```

---

## SELECT 문

### 기본 구문

```sql
SELECT column1, column2
FROM table_name
WHERE condition
ORDER BY column1
LIMIT count OFFSET start;
```

### 전체 구문 순서

```sql
SELECT [DISTINCT] columns
FROM table
[JOIN other_table ON condition]
[WHERE condition]
[GROUP BY columns]
[HAVING condition]
[ORDER BY columns]
[LIMIT count]
[OFFSET start];
```

### 실행 순서

1. FROM / JOIN
2. WHERE
3. GROUP BY
4. HAVING
5. SELECT
6. DISTINCT
7. ORDER BY
8. LIMIT / OFFSET

---

## WHERE 조건

### 비교 연산자

```sql
SELECT * FROM products WHERE price > 100;
SELECT * FROM products WHERE price >= 100;
SELECT * FROM products WHERE price < 100;
SELECT * FROM products WHERE price <= 100;
SELECT * FROM products WHERE price = 100;
SELECT * FROM products WHERE price <> 100;  -- != 도 가능
```

### 논리 연산자

```sql
SELECT * FROM products
WHERE price > 100 AND stock > 0;

SELECT * FROM products
WHERE category = 'A' OR category = 'B';

SELECT * FROM products
WHERE NOT is_deleted;
```

### NULL 처리

```sql
-- NULL 비교는 IS NULL / IS NOT NULL 사용
SELECT * FROM users WHERE phone IS NULL;
SELECT * FROM users WHERE phone IS NOT NULL;

-- NULL 비교 (= 사용 불가)
SELECT * FROM users WHERE phone = NULL;  -- 항상 결과 없음!
```

### BETWEEN

```sql
SELECT * FROM products
WHERE price BETWEEN 100 AND 500;
-- 동일: WHERE price >= 100 AND price <= 500
```

### IN

```sql
SELECT * FROM products
WHERE category IN ('A', 'B', 'C');

-- 서브쿼리와 함께
SELECT * FROM users
WHERE id IN (SELECT user_id FROM orders);
```

### LIKE / ILIKE

```sql
-- 패턴 매칭 (%: 0개 이상 문자, _: 1개 문자)
SELECT * FROM users WHERE name LIKE 'John%';
SELECT * FROM users WHERE name LIKE '%son';
SELECT * FROM users WHERE name LIKE '%oh%';
SELECT * FROM users WHERE name LIKE 'J_hn';

-- 대소문자 무시
SELECT * FROM users WHERE name ILIKE 'john%';
```

### 정규표현식

```sql
-- ~ : 정규식 매칭
SELECT * FROM users WHERE email ~ '^[a-z]+@';

-- ~* : 대소문자 무시
SELECT * FROM users WHERE email ~* '^[A-Z]+@';

-- !~ : 매칭하지 않음
SELECT * FROM users WHERE email !~ '@gmail\.com$';
```

---

## INSERT 문

### 기본 삽입

```sql
INSERT INTO users (name, email)
VALUES ('John', 'john@example.com');
```

### 다중 행 삽입

```sql
INSERT INTO users (name, email)
VALUES
    ('John', 'john@example.com'),
    ('Jane', 'jane@example.com'),
    ('Bob', 'bob@example.com');
```

### 서브쿼리로 삽입

```sql
INSERT INTO user_archive (id, name, email)
SELECT id, name, email FROM users WHERE is_deleted = TRUE;
```

### RETURNING

```sql
INSERT INTO users (name, email)
VALUES ('John', 'john@example.com')
RETURNING id, created_at;
```

### ON CONFLICT (UPSERT)

```sql
-- 충돌 시 무시
INSERT INTO users (email, name)
VALUES ('john@example.com', 'John')
ON CONFLICT (email) DO NOTHING;

-- 충돌 시 업데이트
INSERT INTO users (email, name, updated_at)
VALUES ('john@example.com', 'John Updated', NOW())
ON CONFLICT (email)
DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = EXCLUDED.updated_at;
```

---

## UPDATE 문

### 기본 업데이트

```sql
UPDATE users
SET name = 'John Doe'
WHERE id = 1;
```

### 다중 컬럼 업데이트

```sql
UPDATE users
SET
    name = 'John Doe',
    email = 'johndoe@example.com',
    updated_at = NOW()
WHERE id = 1;
```

### 서브쿼리로 업데이트

```sql
UPDATE products
SET price = subquery.new_price
FROM (
    SELECT product_id, new_price
    FROM price_updates
) AS subquery
WHERE products.id = subquery.product_id;
```

### RETURNING (PostgreSQL 18+: OLD/NEW)

```sql
UPDATE users
SET name = 'New Name'
WHERE id = 1
RETURNING id, name;

-- PostgreSQL 18+
UPDATE users
SET name = 'New Name'
WHERE id = 1
RETURNING OLD.name AS old_name, NEW.name AS new_name;
```

---

## DELETE 문

### 기본 삭제

```sql
DELETE FROM users WHERE id = 1;
```

### 조건부 삭제

```sql
DELETE FROM users
WHERE created_at < NOW() - INTERVAL '1 year'
AND is_active = FALSE;
```

### RETURNING

```sql
DELETE FROM users
WHERE id = 1
RETURNING *;
```

### TRUNCATE

```sql
-- 모든 행 빠르게 삭제 (트리거 실행 안 됨)
TRUNCATE TABLE logs;

-- 여러 테이블
TRUNCATE TABLE logs, events;

-- CASCADE (외래키 참조 테이블도)
TRUNCATE TABLE users CASCADE;

-- IDENTITY 재설정
TRUNCATE TABLE users RESTART IDENTITY;
```

---

## JOIN

### INNER JOIN

```sql
SELECT u.name, o.amount
FROM users u
INNER JOIN orders o ON u.id = o.user_id;
```

### LEFT JOIN

```sql
SELECT u.name, o.amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id;
-- 주문이 없는 사용자도 포함
```

### RIGHT JOIN

```sql
SELECT u.name, o.amount
FROM users u
RIGHT JOIN orders o ON u.id = o.user_id;
```

### FULL OUTER JOIN

```sql
SELECT u.name, o.amount
FROM users u
FULL OUTER JOIN orders o ON u.id = o.user_id;
```

### CROSS JOIN

```sql
SELECT * FROM colors CROSS JOIN sizes;
-- 또는
SELECT * FROM colors, sizes;
```

### SELF JOIN

```sql
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

### 다중 JOIN

```sql
SELECT u.name, p.name AS product, oi.quantity
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id;
```

### LATERAL JOIN

```sql
SELECT u.name, recent_orders.*
FROM users u
CROSS JOIN LATERAL (
    SELECT *
    FROM orders o
    WHERE o.user_id = u.id
    ORDER BY created_at DESC
    LIMIT 3
) recent_orders;
```

---

## GROUP BY와 집계 함수

### 집계 함수

```sql
SELECT
    COUNT(*),                    -- 행 수
    COUNT(DISTINCT category),    -- 고유 값 수
    SUM(amount),                 -- 합계
    AVG(amount),                 -- 평균
    MIN(amount),                 -- 최소값
    MAX(amount),                 -- 최대값
    STRING_AGG(name, ', ')       -- 문자열 연결
FROM orders;
```

### GROUP BY

```sql
SELECT category, COUNT(*), SUM(price)
FROM products
GROUP BY category;

-- 다중 컬럼
SELECT category, status, COUNT(*)
FROM products
GROUP BY category, status;
```

### HAVING

```sql
SELECT category, COUNT(*)
FROM products
GROUP BY category
HAVING COUNT(*) > 10;
```

### GROUPING SETS

```sql
SELECT category, status, COUNT(*)
FROM products
GROUP BY GROUPING SETS (
    (category, status),
    (category),
    (status),
    ()
);
```

### ROLLUP

```sql
-- 계층적 집계
SELECT category, subcategory, SUM(sales)
FROM products
GROUP BY ROLLUP (category, subcategory);
```

### CUBE

```sql
-- 모든 조합
SELECT category, status, SUM(sales)
FROM products
GROUP BY CUBE (category, status);
```

---

## ORDER BY

```sql
-- 오름차순 (기본)
SELECT * FROM products ORDER BY price;
SELECT * FROM products ORDER BY price ASC;

-- 내림차순
SELECT * FROM products ORDER BY price DESC;

-- 다중 정렬
SELECT * FROM products ORDER BY category, price DESC;

-- NULL 처리
SELECT * FROM products ORDER BY price NULLS FIRST;
SELECT * FROM products ORDER BY price NULLS LAST;

-- 표현식으로 정렬
SELECT * FROM products ORDER BY price * quantity DESC;

-- 컬럼 번호로 정렬
SELECT name, price FROM products ORDER BY 2 DESC;
```

---

## LIMIT과 OFFSET

```sql
-- 처음 10개
SELECT * FROM products LIMIT 10;

-- 11~20번째
SELECT * FROM products LIMIT 10 OFFSET 10;

-- SQL 표준 문법
SELECT * FROM products
FETCH FIRST 10 ROWS ONLY;

SELECT * FROM products
OFFSET 10 ROWS
FETCH NEXT 10 ROWS ONLY;
```

---

## CTE (Common Table Expressions)

### 기본 CTE

```sql
WITH active_users AS (
    SELECT * FROM users WHERE is_active = TRUE
)
SELECT * FROM active_users WHERE created_at > '2024-01-01';
```

### 다중 CTE

```sql
WITH
active_users AS (
    SELECT * FROM users WHERE is_active = TRUE
),
user_orders AS (
    SELECT user_id, COUNT(*) as order_count
    FROM orders
    GROUP BY user_id
)
SELECT u.name, uo.order_count
FROM active_users u
LEFT JOIN user_orders uo ON u.id = uo.user_id;
```

### 재귀 CTE

```sql
-- 조직도 조회
WITH RECURSIVE org_tree AS (
    -- 기본 케이스
    SELECT id, name, manager_id, 1 AS level
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- 재귀 케이스
    SELECT e.id, e.name, e.manager_id, t.level + 1
    FROM employees e
    JOIN org_tree t ON e.manager_id = t.id
)
SELECT * FROM org_tree ORDER BY level, name;
```

---

## 서브쿼리

### 스칼라 서브쿼리

```sql
SELECT
    name,
    (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) AS order_count
FROM users;
```

### FROM 절 서브쿼리

```sql
SELECT *
FROM (
    SELECT user_id, SUM(amount) AS total
    FROM orders
    GROUP BY user_id
) AS user_totals
WHERE total > 1000;
```

### EXISTS

```sql
SELECT * FROM users u
WHERE EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id
);
```

### ANY / ALL

```sql
-- ANY: 하나라도 만족
SELECT * FROM products
WHERE price > ANY (SELECT price FROM competitor_products);

-- ALL: 모두 만족
SELECT * FROM products
WHERE price > ALL (SELECT price FROM competitor_products);
```

---

## UNION / INTERSECT / EXCEPT

```sql
-- UNION: 합집합 (중복 제거)
SELECT name FROM employees
UNION
SELECT name FROM contractors;

-- UNION ALL: 합집합 (중복 포함)
SELECT name FROM employees
UNION ALL
SELECT name FROM contractors;

-- INTERSECT: 교집합
SELECT name FROM employees
INTERSECT
SELECT name FROM managers;

-- EXCEPT: 차집합
SELECT name FROM employees
EXCEPT
SELECT name FROM managers;
```

---

## CASE 표현식

```sql
-- 단순 CASE
SELECT
    name,
    CASE status
        WHEN 'A' THEN 'Active'
        WHEN 'I' THEN 'Inactive'
        ELSE 'Unknown'
    END AS status_text
FROM users;

-- 검색 CASE
SELECT
    name,
    CASE
        WHEN price < 100 THEN 'Cheap'
        WHEN price < 500 THEN 'Medium'
        ELSE 'Expensive'
    END AS price_category
FROM products;
```

---

## COALESCE와 NULLIF

```sql
-- COALESCE: 첫 번째 NULL이 아닌 값
SELECT COALESCE(nickname, name, 'Anonymous') FROM users;

-- NULLIF: 두 값이 같으면 NULL
SELECT NULLIF(value, 0);  -- 0이면 NULL (0으로 나누기 방지)
SELECT total / NULLIF(count, 0);
```

---

## 윈도우 함수

### 기본 구조

```sql
function_name() OVER (
    [PARTITION BY column]
    [ORDER BY column]
    [frame_clause]
)
```

### ROW_NUMBER, RANK, DENSE_RANK

```sql
SELECT
    name,
    department,
    salary,
    ROW_NUMBER() OVER (ORDER BY salary DESC) as row_num,
    RANK() OVER (ORDER BY salary DESC) as rank,
    DENSE_RANK() OVER (ORDER BY salary DESC) as dense_rank
FROM employees;
```

### 부서별 순위

```sql
SELECT
    name,
    department,
    salary,
    RANK() OVER (PARTITION BY department ORDER BY salary DESC) as dept_rank
FROM employees;
```

### 집계 윈도우 함수

```sql
SELECT
    name,
    department,
    salary,
    AVG(salary) OVER (PARTITION BY department) as dept_avg,
    SUM(salary) OVER (ORDER BY hire_date) as running_total,
    LAG(salary) OVER (ORDER BY hire_date) as prev_salary,
    LEAD(salary) OVER (ORDER BY hire_date) as next_salary
FROM employees;
```

### FIRST_VALUE, LAST_VALUE

```sql
SELECT
    name,
    salary,
    FIRST_VALUE(name) OVER (ORDER BY salary DESC) as highest_paid,
    LAST_VALUE(name) OVER (
        ORDER BY salary DESC
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) as lowest_paid
FROM employees;
```

---

## 참고 문서

- [SQL 구문](https://www.postgresql.org/docs/current/sql-syntax.html)
- [쿼리](https://www.postgresql.org/docs/current/queries.html)
- [데이터 조작](https://www.postgresql.org/docs/current/dml.html)
- [함수와 연산자](https://www.postgresql.org/docs/current/functions.html)
