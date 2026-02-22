# Chapter 71: BRIN 인덱스 (BRIN Indexes)

## 목차
- [71.1 소개 (Introduction)](#711-소개-introduction)
  - [71.1.1 인덱스 유지보수 (Index Maintenance)](#7111-인덱스-유지보수-index-maintenance)
- [71.2 내장 연산자 클래스 (Built-in Operator Classes)](#712-내장-연산자-클래스-built-in-operator-classes)
  - [71.2.1 연산자 클래스 매개변수 (Operator Class Parameters)](#7121-연산자-클래스-매개변수-operator-class-parameters)
- [71.3 확장성 (Extensibility)](#713-확장성-extensibility)
  - [71.3.1 Minmax 연산자 클래스](#7131-minmax-연산자-클래스)
  - [71.3.2 Inclusion 연산자 클래스](#7132-inclusion-연산자-클래스)
  - [71.3.3 Bloom 연산자 클래스](#7133-bloom-연산자-클래스)
  - [71.3.4 Minmax-Multi 연산자 클래스](#7134-minmax-multi-연산자-클래스)

---

## 71.1 소개 (Introduction)

BRIN 은 Block Range Index 의 약자입니다. BRIN은 특정 컬럼이 테이블 내의 물리적 위치와 자연스러운 상관관계(correlation)를 가지는 매우 큰 테이블을 처리하기 위해 설계되었습니다.

### 블록 범위 (Block Range) 개념

BRIN은 블록 범위 (또는 "페이지 범위")를 기준으로 동작합니다. 블록 범위는 테이블에서 물리적으로 인접한 페이지들의 그룹입니다. 각 블록 범위에 대해 인덱스는 요약 정보(summary information)를 저장합니다.

예를 들어, 판매 주문을 저장하는 테이블에서 날짜 컬럼이 있다고 가정해봅시다. 이전 주문이 테이블의 앞부분에 나타나는 경우, 이 컬럼은 물리적 위치와 자연스러운 상관관계를 가집니다.

### 쿼리 실행 방식

BRIN 인덱스는 일반적인 비트맵 인덱스 스캔(bitmap index scan)을 통해 쿼리를 처리합니다:

1. 요약 정보가 쿼리 조건과 일치하면 해당 범위 내의 모든 페이지에 있는 튜플을 반환합니다
2. BRIN 인덱스는 손실(lossy) 인덱스 입니다 - 쿼리 실행기(query executor)가 튜플을 다시 확인하고 일치하지 않는 것을 제거해야 합니다
3. 매우 작은 인덱스 크기로 순차 스캔에 비해 최소한의 오버헤드를 추가하면서 테이블의 큰 부분을 스캔하지 않을 수 있습니다

### 스토리지와 정밀도

`pages_per_range` 스토리지 매개변수는 인덱스 생성 시 블록 범위의 크기를 결정합니다:

```sql
-- 기본값으로 BRIN 인덱스 생성
CREATE INDEX idx_brin ON sales_orders USING brin (order_date);

-- pages_per_range를 32로 지정
CREATE INDEX idx_brin_custom ON sales_orders USING brin (order_date)
    WITH (pages_per_range = 32);
```

인덱스 항목 수 계산:
```
인덱스 항목 수 = 테이블의 총 페이지 수 / pages_per_range
```

트레이드오프:
- 더 작은 `pages_per_range` 값 → 더 큰 인덱스, 더 정밀한 요약 데이터, 더 많은 데이터 블록 건너뛰기 가능
- 더 큰 `pages_per_range` 값 → 더 작은 인덱스, 덜 정밀한 요약 데이터

---

### 71.1.1 인덱스 유지보수 (Index Maintenance)

#### 요약화 프로세스 (Summarization Process)

초기 생성 단계:
- 기존의 모든 힙(heap) 페이지가 스캔됩니다
- 각 범위에 대해 요약 인덱스 튜플이 생성됩니다
- 끝에 있는 불완전한 범위도 포함됩니다

지속적인 업데이트:
- 이미 요약된 페이지 범위에 새 데이터가 삽입되면 요약 정보가 업데이트됩니다
- 마지막 요약된 범위 이후의 새 페이지는 요약화가 트리거될 때까지 요약되지 않은 상태로 남습니다

#### 요약화 트리거 방법

요약화는 다음 방법으로 트리거될 수 있습니다:

1. 수동 VACUUM: 테이블을 수동으로 또는 autovacuum을 통해 VACUUM 실행

2. autosummarize 매개변수: 활성화되면 autovacuum이 채워진 페이지 범위를 자동으로 요약

3. 함수 사용:

```sql
-- 모든 요약되지 않은 범위를 요약
SELECT brin_summarize_new_values('idx_brin'::regclass);

-- 특정 페이지를 포함하는 범위만 요약
SELECT brin_summarize_range('idx_brin'::regclass, 128);
```

#### 역요약화 (De-summarization)

인덱스 튜플이 더 이상 값을 잘 나타내지 않을 때, `brin_desummarize_range` 함수로 범위의 요약을 해제할 수 있습니다:

```sql
-- 특정 범위의 요약 해제
SELECT brin_desummarize_range('idx_brin'::regclass, 128);
```

#### Autosummarize 세부사항

- 기본적으로 비활성화되어 있습니다
- 활성화되면 삽입이 감지될 때 autovacuum이 블록 범위 요약 요청을 받습니다
- 요청 큐가 가득 차면 서버 로그에 메시지가 나타납니다:

```
LOG: request for BRIN range summarization for index "brin_wi_idx" page 128 was not recorded
```

```sql
-- autosummarize 활성화하여 인덱스 생성
CREATE INDEX idx_brin_auto ON sales_orders USING brin (order_date)
    WITH (autosummarize = on);
```

---

## 71.2 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 네 가지 유형의 BRIN 연산자 클래스가 포함되어 있습니다:

| 유형 | 설명 | 사용 사례 |
|------|------|----------|
| minmax | 범위 내 인덱싱된 컬럼의 최솟값과 최댓값 저장 | 완전 순서 집합(totally ordered set) |
| minmax-multi | 범위 내 값을 나타내는 여러 최솟값/최댓값 저장 | 이상치(outlier)가 있는 데이터 |
| inclusion | 범위 내 값을 포함하는 값 저장 | 기하학적 타입, 범위 타입 |
| bloom | 범위 내 모든 값에 대한 블룸 필터 구축 | 등호 비교만 필요한 경우 |

### 전체 내장 연산자 클래스 목록

#### 숫자 타입 (Numeric Types)

| 데이터 타입 | minmax | bloom | minmax-multi |
|------------|--------|-------|--------------|
| `int2` | `int2_minmax_ops` | `int2_bloom_ops` | `int2_minmax_multi_ops` |
| `int4` | `int4_minmax_ops` | `int4_bloom_ops` | `int4_minmax_multi_ops` |
| `int8` | `int8_minmax_ops` | `int8_bloom_ops` | `int8_minmax_multi_ops` |
| `float4` | `float4_minmax_ops` | `float4_bloom_ops` | `float4_minmax_multi_ops` |
| `float8` | `float8_minmax_ops` | `float8_bloom_ops` | `float8_minmax_multi_ops` |
| `numeric` | `numeric_minmax_ops` | `numeric_bloom_ops` | `numeric_minmax_multi_ops` |

#### 문자/문자열 타입 (Character/String Types)

| 데이터 타입 | minmax | bloom |
|------------|--------|-------|
| `char` | `char_minmax_ops` | `char_bloom_ops` |
| `bpchar` | `bpchar_minmax_ops` | `bpchar_bloom_ops` |
| `text` | `text_minmax_ops` | `text_bloom_ops` |
| `name` | `name_minmax_ops` | `name_bloom_ops` |
| `bytea` | `bytea_minmax_ops` | `bytea_bloom_ops` |

#### 날짜/시간 타입 (Date/Time Types)

| 데이터 타입 | minmax | bloom | minmax-multi |
|------------|--------|-------|--------------|
| `date` | `date_minmax_ops` | `date_bloom_ops` | `date_minmax_multi_ops` |
| `timestamp` | `timestamp_minmax_ops` | `timestamp_bloom_ops` | `timestamp_minmax_multi_ops` |
| `timestamptz` | `timestamptz_minmax_ops` | `timestamptz_bloom_ops` | `timestamptz_minmax_multi_ops` |
| `time` | `time_minmax_ops` | `time_bloom_ops` | `time_minmax_multi_ops` |
| `timetz` | `timetz_minmax_ops` | `timetz_bloom_ops` | `timetz_minmax_multi_ops` |
| `interval` | `interval_minmax_ops` | `interval_bloom_ops` | `interval_minmax_multi_ops` |

#### 네트워크 타입 (Network Types)

| 데이터 타입 | minmax | bloom | minmax-multi | inclusion |
|------------|--------|-------|--------------|-----------|
| `inet` | `inet_minmax_ops` | `inet_bloom_ops` | `inet_minmax_multi_ops` | `inet_inclusion_ops` |
| `macaddr` | `macaddr_minmax_ops` | `macaddr_bloom_ops` | `macaddr_minmax_multi_ops` | - |
| `macaddr8` | `macaddr8_minmax_ops` | `macaddr8_bloom_ops` | `macaddr8_minmax_multi_ops` | - |

#### 특수 타입 (Specialized Types)

| 데이터 타입 | 연산자 클래스 |
|------------|--------------|
| `uuid` | `uuid_minmax_ops`, `uuid_bloom_ops`, `uuid_minmax_multi_ops` |
| `bit` | `bit_minmax_ops` |
| `varbit` | `varbit_minmax_ops` |
| `oid` | `oid_minmax_ops`, `oid_bloom_ops`, `oid_minmax_multi_ops` |
| `tid` | `tid_minmax_ops`, `tid_bloom_ops`, `tid_minmax_multi_ops` |
| `pg_lsn` | `pg_lsn_minmax_ops`, `pg_lsn_bloom_ops`, `pg_lsn_minmax_multi_ops` |
| `box` | `box_inclusion_ops` |
| `range` | `range_inclusion_ops` |

### 사용 예제

```sql
-- minmax 연산자 클래스 사용 (기본값)
CREATE INDEX idx_date_minmax ON orders USING brin (order_date);

-- bloom 연산자 클래스 사용
CREATE INDEX idx_customer_bloom ON orders USING brin (customer_id int4_bloom_ops);

-- minmax-multi 연산자 클래스 사용
CREATE INDEX idx_amount_multi ON orders USING brin (amount numeric_minmax_multi_ops);

-- inclusion 연산자 클래스 사용 (기하학적 타입)
CREATE INDEX idx_location ON locations USING brin (bounding_box box_inclusion_ops);
```

---

### 71.2.1 연산자 클래스 매개변수 (Operator Class Parameters)

bloom 과 minmax-multi 연산자 클래스만 매개변수를 지원합니다.

#### Bloom 연산자 클래스 매개변수

`n_distinct_per_range`
- 블록 범위 내 예상되는 고유한 non-null 값의 수를 정의합니다
- 양수 값: 블록 범위에 이 수만큼의 고유 값이 있다고 가정
- 음수 값 (≥ -1): 고유 non-null 값의 수가 블록 범위의 최대 튜플 수에 비례하여 선형적으로 증가 (블록당 약 290개 행)
- 기본값: `-0.1`
- 최솟값: `16`

`false_positive_rate`
- 블룸 필터 크기 조정을 위한 원하는 위양성률(false positive rate)
- 유효 범위: `0.0001` ~ `0.25`
- 기본값: `0.01` (1% 위양성률)

```sql
-- Bloom 연산자 클래스 매개변수 사용 예제
CREATE INDEX idx_bloom_custom ON orders USING brin (
    customer_id int4_bloom_ops (
        n_distinct_per_range = 100,
        false_positive_rate = 0.05
    )
);
```

#### Minmax-Multi 연산자 클래스 매개변수

`values_per_range`
- 블록 범위를 요약하기 위해 저장할 최대 값의 수
- 각 값은 점(point) 또는 구간 경계(interval boundary)를 나타냅니다
- 유효 범위: `8` ~ `256`
- 기본값: `32`

```sql
-- Minmax-Multi 연산자 클래스 매개변수 사용 예제
CREATE INDEX idx_multi_custom ON orders USING brin (
    amount numeric_minmax_multi_ops (values_per_range = 64)
);
```

---

## 71.3 확장성 (Extensibility)

BRIN 인터페이스는 높은 수준의 추상화를 제공하여, 액세스 메서드 구현자는 액세스되는 데이터 타입의 의미론(semantics)만 구현하면 됩니다. BRIN 레이어 자체가 동시성, 로깅, 인덱스 구조 검색을 처리합니다.

### 필수 메서드 (Required Methods)

BRIN 연산자 클래스는 네 가지 핵심 메서드를 제공해야 합니다:

#### 1. `opcInfo`

```c
BrinOpcInfo *opcInfo(Oid type_oid)
```

인덱싱된 컬럼의 요약 데이터에 대한 내부 정보를 반환합니다:

```c
typedef struct BrinOpcInfo
{
    uint16      oi_nstored;           /* 저장된 컬럼 수 */
    void       *oi_opaque;            /* 개인용 불투명 포인터 */
    TypeCacheEntry *oi_typcache[];    /* 타입 캐시 항목 */
} BrinOpcInfo;
```

#### 2. `consistent`

```c
bool consistent(BrinDesc *bdesc, BrinValues *column, ScanKey *keys, int nkeys)
```

모든 ScanKey 항목이 범위의 인덱싱된 값과 일치하는지 검증합니다. 동일한 속성에 대해 여러 스캔 키를 지원합니다.

이전 버전과의 호환성을 위한 단일 ScanKey 변형:
```c
bool consistent(BrinDesc *bdesc, BrinValues *column, ScanKey key)
```

#### 3. `addValue`

```c
bool addValue(BrinDesc *bdesc, BrinValues *column, Datum newval, bool isnull)
```

인덱스 튜플을 수정하여 추가적인 새 값을 나타내도록 합니다. 튜플이 수정되면 `true`를 반환합니다.

#### 4. `unionTuples`

```c
bool unionTuples(BrinDesc *bdesc, BrinValues *a, BrinValues *b)
```

두 인덱스 튜플을 통합하여 첫 번째 튜플이 두 튜플 모두를 나타내도록 수정합니다.

### 선택적 메서드 (Optional Method)

#### `options`

```c
void options(local_relopts *relopts)
```

연산자 클래스 동작을 제어하는 사용자 표시 매개변수를 정의합니다. 옵션은 `PG_HAS_OPCLASS_OPTIONS()`와 `PG_GET_OPCLASS_OPTIONS()` 매크로를 통해 액세스됩니다.

### 지원 함수 번호 규칙

- 함수 1-10: BRIN 내부 함수용으로 예약
- 함수 11+: SQL 레벨 사용자 정의 구현용

---

### 71.3.1 Minmax 연산자 클래스

단일 연속 구간을 가진 완전 순서 집합(totally ordered set)을 위한 연산자 클래스입니다.

#### 필수 멤버

| 멤버 | 객체 |
|------|------|
| 지원 함수 1 | `brin_minmax_opcinfo()` |
| 지원 함수 2 | `brin_minmax_add_value()` |
| 지원 함수 3 | `brin_minmax_consistent()` |
| 지원 함수 4 | `brin_minmax_union()` |
| 연산자 전략 1 | 미만 (`<`) |
| 연산자 전략 2 | 이하 (`<=`) |
| 연산자 전략 3 | 같음 (`=`) |
| 연산자 전략 4 | 이상 (`>=`) |
| 연산자 전략 5 | 초과 (`>`) |

#### 사용자 정의 Minmax 연산자 클래스 예제

```sql
-- 사용자 정의 타입을 위한 minmax 연산자 클래스 생성
CREATE OPERATOR CLASS my_type_minmax_ops
    DEFAULT FOR TYPE my_type USING brin AS
    OPERATOR 1 <,
    OPERATOR 2 <=,
    OPERATOR 3 =,
    OPERATOR 4 >=,
    OPERATOR 5 >,
    FUNCTION 1 brin_minmax_opcinfo(internal),
    FUNCTION 2 brin_minmax_add_value(internal, internal, internal, internal),
    FUNCTION 3 brin_minmax_consistent(internal, internal, internal),
    FUNCTION 4 brin_minmax_union(internal, internal, internal);
```

---

### 71.3.2 Inclusion 연산자 클래스

다른 타입 내에 포함되는 값을 가진 복잡한 데이터 타입을 위한 연산자 클래스입니다.

#### 필수 멤버

| 멤버 | 객체 | 설명 |
|------|------|------|
| 지원 함수 1 | `brin_inclusion_opcinfo()` | 기본 정보 |
| 지원 함수 2 | `brin_inclusion_add_value()` | 값 추가 |
| 지원 함수 3 | `brin_inclusion_consistent()` | 일관성 검사 |
| 지원 함수 4 | `brin_inclusion_union()` | 통합 |
| 지원 함수 11 | 병합 함수 (필수) | 두 요소 병합 |
| 지원 함수 12 | 병합 가능성 검사 (선택) | 요소 병합 가능 여부 |
| 지원 함수 13 | 포함 검사 (선택) | 요소 포함 여부 |
| 지원 함수 14 | 빈 요소 검사 (선택) | 범위 타입용 |

#### 주요 지원 함수

- 지원 함수 11 (필수): 연산자 클래스와 동일한 데이터 타입의 두 요소를 병합
- 지원 함수 12: 두 요소가 병합 가능한지 검사 (네트워크 주소 패밀리 등)
- 지원 함수 13: 요소가 다른 요소에 포함되는지 검사 (성능 향상에 권장)
- 지원 함수 14: 요소가 비어있는지 검사 (범위 타입용)

#### Inclusion 연산자 클래스 사용 예제

```sql
-- box 타입을 위한 inclusion 인덱스 생성
CREATE TABLE geometric_data (
    id serial PRIMARY KEY,
    bounding_box box
);

CREATE INDEX idx_box ON geometric_data USING brin (bounding_box box_inclusion_ops);

-- 범위 타입을 위한 inclusion 인덱스 생성
CREATE TABLE reservations (
    id serial PRIMARY KEY,
    during daterange
);

CREATE INDEX idx_during ON reservations USING brin (during range_inclusion_ops);
```

---

### 71.3.3 Bloom 연산자 클래스

등호 비교만 지원하고 해싱을 지원하는 데이터 타입을 위한 연산자 클래스입니다.

#### 필수 멤버

| 멤버 | 객체 |
|------|------|
| 지원 프로시저 1 | `brin_bloom_opcinfo()` |
| 지원 프로시저 2 | `brin_bloom_add_value()` |
| 지원 프로시저 3 | `brin_bloom_consistent()` |
| 지원 프로시저 4 | `brin_bloom_union()` |
| 지원 프로시저 5 | `brin_bloom_options()` |
| 지원 프로시저 11 | 해시 계산 함수 |
| 연산자 전략 1 | 같음 (`=`) |

지원 프로시저 11: 연산자 클래스와 동일한 데이터 타입의 인수 하나를 받아 해시 값을 반환해야 합니다.

#### Bloom 연산자 클래스 사용 예제

```sql
-- Bloom 필터를 사용한 인덱스 생성
CREATE TABLE user_sessions (
    id serial PRIMARY KEY,
    session_id uuid,
    user_agent text
);

-- UUID에 bloom 연산자 클래스 사용
CREATE INDEX idx_session_bloom ON user_sessions
    USING brin (session_id uuid_bloom_ops);

-- 매개변수와 함께 사용
CREATE INDEX idx_session_bloom_custom ON user_sessions
    USING brin (
        session_id uuid_bloom_ops (
            n_distinct_per_range = 200,
            false_positive_rate = 0.02
        )
    );
```

---

### 71.3.4 Minmax-Multi 연산자 클래스

완전 순서 집합을 위한 minmax의 확장으로, 단일 연속 구간 대신 여러 개의 작은 구간을 허용합니다. 이상치(outlier) 값이 있는 데이터를 더 효과적으로 처리합니다.

#### 필수 멤버

| 멤버 | 객체 |
|------|------|
| 지원 프로시저 1 | `brin_minmax_multi_opcinfo()` |
| 지원 프로시저 2 | `brin_minmax_multi_add_value()` |
| 지원 프로시저 3 | `brin_minmax_multi_consistent()` |
| 지원 프로시저 4 | `brin_minmax_multi_union()` |
| 지원 프로시저 5 | `brin_minmax_multi_options()` |
| 지원 프로시저 11 | 거리 계산 함수 (범위 길이) |
| 연산자 전략 1-5 | `<`, `<=`, `=`, `>=`, `>` |

#### Minmax vs Minmax-Multi 비교 예제

```sql
-- 이상치가 있는 데이터를 시뮬레이션
CREATE TABLE sales (
    id serial PRIMARY KEY,
    sale_date date,
    amount numeric
);

-- 대부분의 값은 특정 범위 내에 있지만 일부 이상치 존재
INSERT INTO sales (sale_date, amount)
SELECT
    '2024-01-01'::date + (random() * 30)::int,
    CASE
        WHEN random() < 0.95 THEN random() * 100  -- 일반 값: 0-100
        ELSE random() * 10000                      -- 이상치: 0-10000
    END
FROM generate_series(1, 100000);

-- 일반 minmax 인덱스 (이상치로 인해 효율성 저하)
CREATE INDEX idx_amount_minmax ON sales
    USING brin (amount numeric_minmax_ops);

-- minmax-multi 인덱스 (이상치를 더 잘 처리)
CREATE INDEX idx_amount_multi ON sales
    USING brin (amount numeric_minmax_multi_ops (values_per_range = 64));
```

---

## 교차 데이터 타입 연산자 (Cross-Data-Type Operators)

minmax와 inclusion 연산자 클래스 모두 교차 데이터 타입 연산자를 지원합니다:

- Minmax: 동일한 데이터 타입을 가진 전체 연산자 세트가 필요하며, 추가 데이터 타입에 대한 추가 연산자 세트 정의 가능
- Inclusion: 교차 타입 연산자 사용 시 의존성이 더 복잡해짐

```sql
-- 예: float4_minmax_ops는 float4 비교 연산자뿐만 아니라
-- float8과의 비교 연산자도 지원할 수 있음
```

---

## BRIN 인덱스 장점과 제한사항

### 장점

- 매우 작은 인덱스 크기
- 스캔 중 최소한의 오버헤드
- 테이블의 큰 부분 스캔 회피
- 자연적으로 정렬된 대용량 테이블에 이상적
- 사용자 정의 데이터 타입으로 확장 가능
- 자동 및 수동 요약화 옵션

### 제한사항

- 손실 인덱스 (후보 튜플의 재확인 필요)
- 데이터가 물리적 위치와 상관관계가 있을 때만 효과적
- 무작위 데이터 분포에는 효과가 떨어짐

---

## 실제 사용 예제

### 시계열 데이터에 BRIN 인덱스 사용

```sql
-- 로그 테이블 생성
CREATE TABLE application_logs (
    id bigserial PRIMARY KEY,
    log_time timestamptz NOT NULL,
    level text,
    message text
);

-- 시간 기반 BRIN 인덱스 생성 (자연스러운 시간순 삽입)
CREATE INDEX idx_log_time ON application_logs
    USING brin (log_time)
    WITH (pages_per_range = 128);

-- 쿼리 예제
EXPLAIN ANALYZE
SELECT * FROM application_logs
WHERE log_time BETWEEN '2024-01-01' AND '2024-01-31';
```

### 파티션 테이블과 BRIN 인덱스

```sql
-- 파티션 테이블 생성
CREATE TABLE sensor_readings (
    sensor_id int,
    reading_time timestamptz,
    value numeric
) PARTITION BY RANGE (reading_time);

-- 파티션 생성
CREATE TABLE sensor_readings_2024_q1
    PARTITION OF sensor_readings
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

-- 각 파티션에 BRIN 인덱스 생성
CREATE INDEX idx_sensor_time ON sensor_readings_2024_q1
    USING brin (reading_time);
```

### B-tree 대비 BRIN 인덱스 크기 비교

```sql
-- 테스트 테이블 생성
CREATE TABLE size_comparison (
    id serial PRIMARY KEY,
    created_at timestamptz DEFAULT now()
);

-- 데이터 삽입
INSERT INTO size_comparison (created_at)
SELECT timestamp '2024-01-01' + (i || ' seconds')::interval
FROM generate_series(1, 1000000) i;

-- B-tree 인덱스
CREATE INDEX idx_btree ON size_comparison USING btree (created_at);

-- BRIN 인덱스
CREATE INDEX idx_brin ON size_comparison USING brin (created_at);

-- 인덱스 크기 비교
SELECT
    indexrelid::regclass AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
WHERE relid = 'size_comparison'::regclass;
```

---

## 참고 자료

- [PostgreSQL 공식 문서: BRIN Indexes](https://www.postgresql.org/docs/current/brin.html)
- [PostgreSQL 공식 문서: CREATE INDEX](https://www.postgresql.org/docs/current/sql-createindex.html)
