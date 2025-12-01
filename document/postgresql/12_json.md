# PostgreSQL JSON 기능

> 공식 문서: https://www.postgresql.org/docs/current/functions-json.html

---

## JSON vs JSONB

| 특성 | JSON | JSONB |
|------|------|-------|
| 저장 방식 | 텍스트 | 바이너리 |
| 공백/순서 | 보존 | 보존 안 함 |
| 중복 키 | 허용 | 마지막 값만 |
| 인덱싱 | 불가 | 가능 |
| 처리 속도 | 파싱 필요 | 빠름 |
| 저장 크기 | 작음 | 약간 큼 |

**권장사항:** 대부분의 경우 **JSONB** 사용

```sql
-- 테이블 생성
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    data JSONB
);

-- JSON 데이터 삽입
INSERT INTO products (data) VALUES
('{"name": "Widget", "price": 29.99, "tags": ["sale", "new"], "specs": {"weight": 100, "color": "blue"}}');
```

---

## 기본 연산자

### 접근 연산자

| 연산자 | 설명 | 반환 타입 |
|--------|------|----------|
| `->` | 키/인덱스로 접근 | JSON/JSONB |
| `->>` | 키/인덱스로 접근 | TEXT |
| `#>` | 경로로 접근 | JSON/JSONB |
| `#>>` | 경로로 접근 | TEXT |

```sql
-- 객체 필드 접근
SELECT data->'name' FROM products;           -- "Widget" (JSONB)
SELECT data->>'name' FROM products;          -- Widget (TEXT)

-- 배열 요소 접근 (0-based)
SELECT data->'tags'->0 FROM products;        -- "sale"
SELECT data->'tags'->>0 FROM products;       -- sale

-- 중첩 접근
SELECT data->'specs'->'weight' FROM products;
SELECT data->'specs'->>'weight' FROM products;

-- 경로로 접근
SELECT data#>'{specs,color}' FROM products;    -- "blue"
SELECT data#>>'{specs,color}' FROM products;   -- blue
```

---

## JSONB 전용 연산자

### 포함 연산자

| 연산자 | 설명 |
|--------|------|
| `@>` | 왼쪽이 오른쪽을 포함 |
| `<@` | 오른쪽이 왼쪽을 포함 |

```sql
-- 포함 검색
SELECT * FROM products
WHERE data @> '{"name": "Widget"}';

SELECT * FROM products
WHERE data @> '{"tags": ["sale"]}';

SELECT * FROM products
WHERE data->'specs' @> '{"color": "blue"}';
```

### 존재 연산자

| 연산자 | 설명 |
|--------|------|
| `?` | 키가 존재하는지 |
| `?|` | 키 중 하나라도 존재 |
| `?&` | 키가 모두 존재 |

```sql
-- 키 존재 확인
SELECT * FROM products WHERE data ? 'name';
SELECT * FROM products WHERE data->'specs' ? 'weight';

-- 키 중 하나라도 존재
SELECT * FROM products WHERE data ?| array['name', 'title'];

-- 키 모두 존재
SELECT * FROM products WHERE data ?& array['name', 'price'];
```

### 연결 및 삭제 연산자

| 연산자 | 설명 |
|--------|------|
| `||` | JSON 병합 |
| `-` | 키 또는 요소 삭제 |
| `#-` | 경로로 삭제 |

```sql
-- 병합
SELECT '{"a": 1}'::jsonb || '{"b": 2}'::jsonb;
-- {"a": 1, "b": 2}

-- 키 삭제
SELECT '{"a": 1, "b": 2}'::jsonb - 'a';
-- {"b": 2}

-- 여러 키 삭제
SELECT '{"a": 1, "b": 2, "c": 3}'::jsonb - array['a', 'b'];
-- {"c": 3}

-- 배열 요소 삭제 (인덱스)
SELECT '["a", "b", "c"]'::jsonb - 1;
-- ["a", "c"]

-- 경로로 삭제
SELECT '{"a": {"b": 1, "c": 2}}'::jsonb #- '{a,b}';
-- {"a": {"c": 2}}
```

---

## JSON 함수

### 생성 함수

```sql
-- 객체 생성
SELECT json_build_object('name', 'John', 'age', 30);
-- {"name": "John", "age": 30}

SELECT jsonb_build_object('name', 'John', 'age', 30);

-- 배열 생성
SELECT json_build_array(1, 2, 'three');
-- [1, 2, "three"]

-- 행을 JSON으로
SELECT row_to_json(users.*) FROM users WHERE id = 1;

-- 집계로 배열 생성
SELECT json_agg(users) FROM users WHERE is_active = TRUE;

-- 집계로 객체 생성
SELECT json_object_agg(name, email) FROM users;
```

### 변환 함수

```sql
-- JSON을 JSONB로
SELECT '{"a": 1}'::json::jsonb;

-- 값 타입 확인
SELECT jsonb_typeof('{"a": 1}'::jsonb);  -- object
SELECT jsonb_typeof('[1, 2, 3]'::jsonb); -- array
SELECT jsonb_typeof('"hello"'::jsonb);   -- string
SELECT jsonb_typeof('123'::jsonb);       -- number
SELECT jsonb_typeof('true'::jsonb);      -- boolean
SELECT jsonb_typeof('null'::jsonb);      -- null
```

### 추출 함수

```sql
-- 객체 키 추출
SELECT jsonb_object_keys('{"a": 1, "b": 2}'::jsonb);
-- a
-- b

-- 배열 요소를 행으로
SELECT jsonb_array_elements('[1, 2, 3]'::jsonb);
-- 1
-- 2
-- 3

SELECT jsonb_array_elements_text('["a", "b", "c"]'::jsonb);
-- a
-- b
-- c

-- 객체를 키-값 쌍으로
SELECT * FROM jsonb_each('{"a": 1, "b": 2}'::jsonb);
-- a | 1
-- b | 2

SELECT * FROM jsonb_each_text('{"a": 1, "b": "two"}'::jsonb);
```

### 수정 함수

```sql
-- 값 설정/추가
SELECT jsonb_set('{"a": 1}'::jsonb, '{b}', '2');
-- {"a": 1, "b": 2}

-- 중첩 경로 설정
SELECT jsonb_set('{"a": {"b": 1}}'::jsonb, '{a,c}', '2');
-- {"a": {"b": 1, "c": 2}}

-- 경로가 없으면 생성하지 않음 (기본)
SELECT jsonb_set('{"a": 1}'::jsonb, '{b,c}', '2', false);
-- {"a": 1}

-- 경로가 없으면 생성
SELECT jsonb_set('{"a": 1}'::jsonb, '{b,c}', '2', true);
-- {"a": 1, "b": {"c": 2}}

-- 배열에 요소 삽입
SELECT jsonb_insert('{"a": [1, 2]}'::jsonb, '{a,1}', '3');
-- {"a": [1, 3, 2]}

-- 배열 끝에 삽입
SELECT jsonb_insert('{"a": [1, 2]}'::jsonb, '{a,2}', '3', true);
-- {"a": [1, 2, 3]}
```

### 경로 추출

```sql
-- 경로로 값 추출
SELECT jsonb_extract_path('{"a": {"b": {"c": 1}}}'::jsonb, 'a', 'b', 'c');
-- 1

SELECT jsonb_extract_path_text('{"a": {"b": {"c": 1}}}'::jsonb, 'a', 'b', 'c');
-- 1 (TEXT)
```

---

## JSON_TABLE (PostgreSQL 17+)

JSON 데이터를 관계형 테이블로 변환합니다.

```sql
SELECT * FROM JSON_TABLE(
    '[{"name": "John", "age": 30}, {"name": "Jane", "age": 25}]',
    '$[*]'
    COLUMNS (
        name TEXT PATH '$.name',
        age INTEGER PATH '$.age'
    )
);
-- name | age
-- John | 30
-- Jane | 25

-- 중첩 데이터 처리
SELECT * FROM JSON_TABLE(
    '{"users": [{"name": "John", "orders": [{"id": 1}, {"id": 2}]}]}',
    '$.users[*]'
    COLUMNS (
        user_name TEXT PATH '$.name',
        NESTED PATH '$.orders[*]' COLUMNS (
            order_id INTEGER PATH '$.id'
        )
    )
);
```

---

## SQL/JSON 표준 함수 (PostgreSQL 17+)

### JSON_EXISTS

```sql
SELECT JSON_EXISTS('{"a": 1}', '$.a');  -- true
SELECT JSON_EXISTS('{"a": 1}', '$.b');  -- false
```

### JSON_QUERY

```sql
SELECT JSON_QUERY('{"a": [1, 2, 3]}', '$.a');  -- [1, 2, 3]
SELECT JSON_QUERY('{"a": {"b": 1}}', '$.a');   -- {"b": 1}
```

### JSON_VALUE

```sql
SELECT JSON_VALUE('{"a": 1}', '$.a');          -- 1
SELECT JSON_VALUE('{"a": "hello"}', '$.a');    -- hello
```

---

## JSONPath

### 기본 문법

```sql
-- $ : 루트
-- . : 객체 접근
-- [] : 배열 접근
-- * : 와일드카드
-- @ : 현재 항목

-- 모든 태그
SELECT jsonb_path_query('{"tags": ["a", "b", "c"]}', '$.tags[*]');

-- 조건 필터
SELECT jsonb_path_query(
    '[{"name": "John", "age": 30}, {"name": "Jane", "age": 25}]',
    '$[*] ? (@.age > 26)'
);
-- {"name": "John", "age": 30}
```

### JSONPath 함수

```sql
-- 경로 쿼리
SELECT jsonb_path_query(data, '$.specs.*') FROM products;

-- 존재 여부
SELECT jsonb_path_exists(data, '$.specs.color') FROM products;

-- 첫 번째 일치
SELECT jsonb_path_query_first(
    '[1, 2, 3, 4, 5]',
    '$[*] ? (@ > 3)'
);
-- 4

-- 모든 일치를 배열로
SELECT jsonb_path_query_array(
    '[1, 2, 3, 4, 5]',
    '$[*] ? (@ > 2)'
);
-- [3, 4, 5]
```

### JSONPath 표현식

```sql
-- 산술 연산
SELECT jsonb_path_query('[{"price": 100, "qty": 2}]', '$[*].price * $[*].qty');

-- 비교 연산
SELECT jsonb_path_query('[1, 2, 3, 4, 5]', '$[*] ? (@ >= 2 && @ <= 4)');

-- 문자열 매칭
SELECT jsonb_path_query(
    '[{"name": "John"}, {"name": "Jane"}]',
    '$[*] ? (@.name like_regex "^J")'
);

-- 배열 크기
SELECT jsonb_path_query('{"arr": [1, 2, 3]}', '$.arr.size()');
-- 3
```

---

## JSONB 인덱싱

### GIN 인덱스

```sql
-- 기본 GIN 인덱스 (모든 연산자 지원)
CREATE INDEX idx_products_data ON products USING GIN (data);

-- jsonb_path_ops (빠르지만 @> 만 지원)
CREATE INDEX idx_products_data_path ON products USING GIN (data jsonb_path_ops);
```

### 표현식 인덱스

```sql
-- 특정 필드에 B-tree 인덱스
CREATE INDEX idx_products_name ON products ((data->>'name'));

-- 특정 필드에 대한 GIN 인덱스
CREATE INDEX idx_products_tags ON products USING GIN ((data->'tags'));
```

### 인덱스 사용 예시

```sql
-- GIN 인덱스 사용 (jsonb_ops)
SELECT * FROM products WHERE data @> '{"name": "Widget"}';
SELECT * FROM products WHERE data ? 'specs';
SELECT * FROM products WHERE data ?| array['name', 'title'];

-- GIN 인덱스 사용 (jsonb_path_ops)
SELECT * FROM products WHERE data @> '{"tags": ["sale"]}';

-- 표현식 인덱스 사용
SELECT * FROM products WHERE data->>'name' = 'Widget';
```

---

## 실용 패턴

### 스키마 없는 데이터

```sql
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 다양한 이벤트 타입
INSERT INTO events (event_type, payload) VALUES
('user_signup', '{"user_id": 1, "email": "john@example.com"}'),
('purchase', '{"user_id": 1, "product_id": 100, "amount": 29.99}'),
('page_view', '{"user_id": 1, "url": "/products/100", "duration": 45}');

-- 이벤트 타입별 쿼리
SELECT * FROM events
WHERE event_type = 'purchase'
AND payload->>'amount'::numeric > 20;
```

### 설정 저장

```sql
CREATE TABLE settings (
    user_id INTEGER PRIMARY KEY,
    preferences JSONB DEFAULT '{}'
);

-- 부분 업데이트
UPDATE settings
SET preferences = preferences || '{"theme": "dark"}'
WHERE user_id = 1;

-- 중첩 업데이트
UPDATE settings
SET preferences = jsonb_set(preferences, '{notifications,email}', 'true')
WHERE user_id = 1;
```

### 태그 시스템

```sql
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    tags JSONB DEFAULT '[]'
);

-- 태그 추가
UPDATE articles
SET tags = tags || '"new_tag"'
WHERE id = 1;

-- 태그로 검색
SELECT * FROM articles WHERE tags ? 'postgresql';

-- 여러 태그 모두 포함
SELECT * FROM articles WHERE tags ?& array['postgresql', 'database'];

-- 태그 개수
SELECT title, jsonb_array_length(tags) AS tag_count FROM articles;
```

### 히스토리 추적

```sql
CREATE TABLE products_v2 (
    id SERIAL PRIMARY KEY,
    current_data JSONB NOT NULL,
    history JSONB DEFAULT '[]'
);

-- 업데이트 시 히스토리 저장
CREATE FUNCTION track_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.history = NEW.history || jsonb_build_object(
        'timestamp', now(),
        'data', OLD.current_data
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER products_history
BEFORE UPDATE ON products_v2
FOR EACH ROW
EXECUTE FUNCTION track_history();
```

---

## 성능 팁

1. **JSONB 사용**: 대부분의 경우 JSON보다 JSONB 권장
2. **적절한 인덱스**: 쿼리 패턴에 맞는 GIN/표현식 인덱스
3. **정규화 고려**: 자주 쿼리되는 필드는 별도 컬럼으로
4. **크기 제한**: 매우 큰 JSON은 성능에 영향

```sql
-- 자주 쿼리되는 필드는 Generated Column으로
CREATE TABLE products_optimized (
    id SERIAL PRIMARY KEY,
    data JSONB,
    name VARCHAR(200) GENERATED ALWAYS AS (data->>'name') STORED,
    price NUMERIC GENERATED ALWAYS AS ((data->>'price')::numeric) STORED
);

CREATE INDEX idx_products_name ON products_optimized(name);
CREATE INDEX idx_products_price ON products_optimized(price);
```

---

## 참고 문서

- [JSON 함수와 연산자](https://www.postgresql.org/docs/current/functions-json.html)
- [JSON 타입](https://www.postgresql.org/docs/current/datatype-json.html)
- [JSONPath](https://www.postgresql.org/docs/current/functions-json.html#FUNCTIONS-SQLJSON-PATH)
