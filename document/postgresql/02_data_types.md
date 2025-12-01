# PostgreSQL 데이터 타입

> 공식 문서: https://www.postgresql.org/docs/current/datatype.html

PostgreSQL은 풍부한 내장 데이터 타입을 제공하며, 사용자 정의 타입도 생성할 수 있습니다.

---

## 숫자형 (Numeric Types)

### 정수형

| 타입 | 저장 크기 | 범위 |
|------|----------|------|
| `smallint` (int2) | 2 bytes | -32,768 ~ 32,767 |
| `integer` (int4) | 4 bytes | -2,147,483,648 ~ 2,147,483,647 |
| `bigint` (int8) | 8 bytes | -9,223,372,036,854,775,808 ~ 9,223,372,036,854,775,807 |

```sql
-- integer는 범위, 저장 크기, 성능 간 최고의 균형을 제공
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    age SMALLINT,
    total_orders BIGINT
);
```

### 임의 정밀도 숫자

| 타입 | 설명 |
|------|------|
| `numeric(p, s)` | 정밀도 p, 스케일 s의 정확한 숫자 |
| `decimal(p, s)` | numeric과 동일 |

```sql
-- 화폐 금액 등 정확성이 필수적인 데이터에 권장
CREATE TABLE products (
    price NUMERIC(10, 2),      -- 최대 10자리, 소수점 2자리
    weight NUMERIC(8, 4),      -- 최대 8자리, 소수점 4자리
    tax_rate NUMERIC(5, 4)     -- 예: 0.0825
);

-- 정밀도 미지정 시 임의 정밀도
SELECT NUMERIC '12345678901234567890.123456789';
```

### 부동소수점

| 타입 | 저장 크기 | 정밀도 |
|------|----------|--------|
| `real` (float4) | 4 bytes | 6자리 |
| `double precision` (float8) | 8 bytes | 15자리 |

```sql
-- IEEE 754 표준
-- 정확한 계산이 필요하면 numeric 사용
CREATE TABLE measurements (
    temperature REAL,
    precise_value DOUBLE PRECISION
);

-- 특수값
SELECT 'Infinity'::double precision;
SELECT '-Infinity'::double precision;
SELECT 'NaN'::double precision;
```

### 자동 증가 (Serial)

| 타입 | 내부 타입 | 범위 |
|------|----------|------|
| `smallserial` | smallint | 1 ~ 32,767 |
| `serial` | integer | 1 ~ 2,147,483,647 |
| `bigserial` | bigint | 1 ~ 9,223,372,036,854,775,807 |

```sql
-- serial은 시퀀스를 자동 생성하는 편의 기능
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100)
);

-- 위는 아래와 동일
CREATE SEQUENCE users_id_seq;
CREATE TABLE users (
    id INTEGER DEFAULT nextval('users_id_seq') PRIMARY KEY,
    name VARCHAR(100)
);
ALTER SEQUENCE users_id_seq OWNED BY users.id;

-- PostgreSQL 10+ 권장: IDENTITY 컬럼
CREATE TABLE users (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100)
);
```

---

## 문자형 (Character Types)

| 타입 | 설명 |
|------|------|
| `character varying(n)` / `varchar(n)` | 최대 n자 가변 길이 |
| `character(n)` / `char(n)` | 고정 n자, 공백 패딩 |
| `text` | 무제한 가변 길이 |

```sql
CREATE TABLE articles (
    slug VARCHAR(100),           -- 최대 100자
    title VARCHAR(255) NOT NULL,
    content TEXT,                -- 길이 제한 없음
    status CHAR(1)               -- 고정 1자
);
```

### 성능 특성

- 세 타입 간 ** 성능 차이 없음**
- `char(n)`은 공백 패딩으로 추가 저장 공간 필요
- 대부분의 경우 **`text`** 또는 **`varchar`** 권장

```sql
-- 저장 오버헤드
-- 126바이트 이하: 1바이트 오버헤드
-- 126바이트 초과: 4바이트 오버헤드

-- 길이 제한 검증
SELECT LENGTH('한글테스트');  -- 5 (문자 수)
SELECT OCTET_LENGTH('한글테스트');  -- 15 (바이트 수, UTF-8)
```

---

## 날짜/시간형 (Date/Time Types)

| 타입 | 저장 크기 | 설명 | 범위 |
|------|----------|------|------|
| `date` | 4 bytes | 날짜만 | 4713 BC ~ 5874897 AD |
| `time` | 8 bytes | 시간만 | 00:00:00 ~ 24:00:00 |
| `time with time zone` | 12 bytes | 시간 + 시간대 | - |
| `timestamp` | 8 bytes | 날짜 + 시간 | 4713 BC ~ 294276 AD |
| `timestamp with time zone` | 8 bytes | 날짜 + 시간 + 시간대 | 4713 BC ~ 294276 AD |
| `interval` | 16 bytes | 시간 간격 | -178000000년 ~ 178000000년 |

```sql
CREATE TABLE events (
    event_date DATE,
    start_time TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMPTZ DEFAULT NOW(),
    duration INTERVAL
);

-- 날짜/시간 입력 형식
INSERT INTO events VALUES (
    '2024-12-25',                    -- ISO 8601
    '14:30:00',
    '2024-12-25 14:30:00',
    '2024-12-25 14:30:00+09',        -- 시간대 포함
    '2 hours 30 minutes'
);
```

### 시간대 처리

```sql
-- timestamptz는 내부적으로 UTC로 저장
-- 표시 시 세션 시간대로 변환
SET timezone = 'Asia/Seoul';
SELECT NOW();

-- 시간대 변환
SELECT '2024-12-25 10:00:00 UTC'::timestamptz AT TIME ZONE 'Asia/Seoul';
```

### 날짜/시간 함수

```sql
-- 현재 시간
SELECT CURRENT_DATE;          -- 오늘 날짜
SELECT CURRENT_TIME;          -- 현재 시간
SELECT CURRENT_TIMESTAMP;     -- 현재 타임스탬프
SELECT NOW();                 -- CURRENT_TIMESTAMP와 동일

-- 날짜 추출
SELECT EXTRACT(YEAR FROM NOW());
SELECT EXTRACT(MONTH FROM NOW());
SELECT EXTRACT(DAY FROM NOW());
SELECT DATE_PART('hour', NOW());

-- 날짜 연산
SELECT NOW() + INTERVAL '1 day';
SELECT NOW() - INTERVAL '1 month';
SELECT AGE(NOW(), '2000-01-01');

-- 날짜 자르기
SELECT DATE_TRUNC('month', NOW());  -- 월 시작일
SELECT DATE_TRUNC('year', NOW());   -- 연 시작일
```

---

## 불리언 (Boolean)

| 값 | 표현 |
|-----|------|
| TRUE | true, yes, on, 1, 't', 'y' |
| FALSE | false, no, off, 0, 'f', 'n' |
| NULL | null, 알 수 없음 |

```sql
CREATE TABLE users (
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE
);

-- 불리언 조건
SELECT * FROM users WHERE is_active;
SELECT * FROM users WHERE NOT is_verified;
SELECT * FROM users WHERE is_active IS TRUE;
```

---

## JSON 타입

| 타입 | 설명 |
|------|------|
| `json` | 텍스트로 저장, 처리 시 파싱 |
| `jsonb` | 바이너리로 저장, 인덱싱 가능 |

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    data JSONB
);

INSERT INTO products (data) VALUES
('{"name": "Widget", "price": 29.99, "tags": ["sale", "new"]}');

-- JSON 접근 연산자
SELECT data->'name' FROM products;          -- JSON 타입 반환
SELECT data->>'name' FROM products;         -- TEXT 타입 반환
SELECT data->'tags'->0 FROM products;       -- 배열 요소
SELECT data#>>'{tags,0}' FROM products;     -- 경로로 접근

-- 조건 검색
SELECT * FROM products WHERE data->>'price'::numeric > 20;
SELECT * FROM products WHERE data @> '{"tags": ["sale"]}';

-- JSONB 수정
UPDATE products SET data = data || '{"stock": 100}';
UPDATE products SET data = jsonb_set(data, '{price}', '39.99');
```

---

## 배열 (Arrays)

```sql
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    tags TEXT[],
    scores INTEGER[]
);

INSERT INTO articles (tags, scores) VALUES
(ARRAY['tech', 'database'], ARRAY[85, 90, 78]),
('{"news", "breaking"}', '{100, 95}');

-- 배열 접근 (1-based index)
SELECT tags[1] FROM articles;

-- 배열 검색
SELECT * FROM articles WHERE 'tech' = ANY(tags);
SELECT * FROM articles WHERE tags @> ARRAY['tech'];

-- 배열 함수
SELECT array_length(tags, 1) FROM articles;
SELECT array_append(tags, 'new') FROM articles;
SELECT unnest(tags) FROM articles;  -- 배열을 행으로 확장
```

---

## UUID

```sql
-- UUID 확장 활성화 (PostgreSQL 13 이하)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- PostgreSQL 13+
    user_id INTEGER
);

-- PostgreSQL 18+: UUIDv7 지원
SELECT uuidv7();  -- 시간 기반 정렬 가능한 UUID
```

---

## 네트워크 주소

| 타입 | 설명 |
|------|------|
| `inet` | IPv4/IPv6 호스트 + 서브넷 |
| `cidr` | IPv4/IPv6 네트워크 |
| `macaddr` | MAC 주소 |

```sql
CREATE TABLE servers (
    ip_address INET,
    network CIDR,
    mac MACADDR
);

INSERT INTO servers VALUES
('192.168.1.100/24', '192.168.1.0/24', '08:00:2b:01:02:03');

-- 네트워크 연산
SELECT * FROM servers WHERE ip_address << '192.168.0.0/16';
SELECT host(ip_address), masklen(ip_address) FROM servers;
```

---

## 범위 타입 (Range Types)

| 타입 | 설명 |
|------|------|
| `int4range` | integer 범위 |
| `int8range` | bigint 범위 |
| `numrange` | numeric 범위 |
| `tsrange` | timestamp 범위 |
| `tstzrange` | timestamptz 범위 |
| `daterange` | date 범위 |

```sql
CREATE TABLE reservations (
    room_id INTEGER,
    period TSTZRANGE
);

INSERT INTO reservations VALUES
(101, '[2024-12-25 14:00, 2024-12-25 18:00)');

-- 범위 연산
SELECT * FROM reservations
WHERE period @> '2024-12-25 15:00'::timestamptz;  -- 포함

SELECT * FROM reservations
WHERE period && '[2024-12-25 16:00, 2024-12-25 20:00)';  -- 겹침

-- Exclusion 제약조건으로 중복 예약 방지
ALTER TABLE reservations
ADD CONSTRAINT no_overlap
EXCLUDE USING GIST (room_id WITH =, period WITH &&);
```

---

## 기하학 타입

| 타입 | 설명 |
|------|------|
| `point` | 2D 점 |
| `line` | 무한 직선 |
| `lseg` | 선분 |
| `box` | 직사각형 |
| `path` | 경로 |
| `polygon` | 다각형 |
| `circle` | 원 |

```sql
CREATE TABLE locations (
    name VARCHAR(100),
    position POINT
);

INSERT INTO locations VALUES ('Seoul', '(126.978, 37.566)');

-- 거리 계산
SELECT name FROM locations
WHERE position <-> '(127.0, 37.5)' < 0.1;
```

---

## 타입 변환 (Casting)

```sql
-- 명시적 캐스팅
SELECT '100'::INTEGER;
SELECT CAST('100' AS INTEGER);
SELECT INTEGER '100';

-- 일반적인 캐스팅
SELECT '2024-12-25'::DATE;
SELECT 'true'::BOOLEAN;
SELECT '{"a":1}'::JSONB;

-- 숫자 변환
SELECT 3.7::INTEGER;  -- 4 (반올림)
SELECT FLOOR(3.7);    -- 3
SELECT CEIL(3.2);     -- 4
```

---

## 참고 문서

- [데이터 타입](https://www.postgresql.org/docs/current/datatype.html)
- [숫자형](https://www.postgresql.org/docs/current/datatype-numeric.html)
- [문자형](https://www.postgresql.org/docs/current/datatype-character.html)
- [날짜/시간형](https://www.postgresql.org/docs/current/datatype-datetime.html)
- [JSON 타입](https://www.postgresql.org/docs/current/datatype-json.html)
