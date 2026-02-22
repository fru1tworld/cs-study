# Chapter 67. B-Tree 인덱스 (B-Tree Indexes)

PostgreSQL 18 공식 문서 번역

---

## 목차

- [67.1. 소개](#671-소개-introduction)
- [67.2. B-Tree 연산자 클래스의 동작](#672-b-tree-연산자-클래스의-동작-behavior-of-b-tree-operator-classes)
- [67.3. B-Tree 지원 함수](#673-b-tree-지원-함수-b-tree-support-functions)
- [67.4. 구현](#674-구현-implementation)

---

## 67.1. 소개 (Introduction)

PostgreSQL은 표준 B-tree(다중 경로 균형 트리, multi-way balanced tree) 인덱스 구현을 포함합니다. 명확한 선형 순서로 정렬 가능한 모든 데이터 타입을 B-tree로 인덱싱할 수 있습니다.

### B-tree 인덱스의 기본 특성

- 정렬 가능한 모든 데이터 타입에 대해 인덱스 생성 가능
- PostgreSQL의 기본 인덱스 유형으로, `CREATE INDEX` 시 별도 지정이 없으면 B-tree가 생성됨
- 제한사항: 인덱스 엔트리는 페이지의 약 1/3을 초과할 수 없음 (TOAST 압축 후 기준)

### B-tree의 역할

B-tree 연산자 클래스는 단순한 인덱싱을 넘어 PostgreSQL 시스템 전반에서 정렬 의미론(sorting semantics) 을 일반적으로 표현하는 데 사용됩니다. 이는 시스템 전역에서 정렬 관련 기능을 지원하는 핵심 역할을 합니다.

### 지원 연산자

B-tree 인덱스는 다음 연산자를 사용하는 비교에서 활용됩니다:

```
<     <=     =     >=     >
```

또한 다음 구성에서도 사용될 수 있습니다:

- `BETWEEN`
- `IN`
- `IS NULL`
- `IS NOT NULL`
- `LIKE` (패턴이 상수이고 시작 부분에 고정된 경우)
- `~` (정규식, 패턴이 상수이고 시작 부분에 고정된 경우)

### 기본 예제

```sql
-- B-tree 인덱스 생성 (기본값)
CREATE INDEX idx_name ON table_name (column_name);

-- 명시적으로 B-tree 지정
CREATE INDEX idx_name ON table_name USING BTREE (column_name);

-- 다중 컬럼 B-tree 인덱스
CREATE INDEX idx_multi ON table_name (col1, col2, col3);

-- 고유 B-tree 인덱스
CREATE UNIQUE INDEX idx_unique ON table_name (column_name);
```

---

## 67.2. B-Tree 연산자 클래스의 동작 (Behavior of B-Tree Operator Classes)

B-tree 인덱스가 올바르게 작동하려면 연산자 클래스가 특정 규칙과 동작을 따라야 합니다.

### 필수 연산자 (5개)

B-tree 연산자 클래스는 다음 5개의 비교 연산자를 제공해야 합니다:

| 연산자 | 전략 번호 | 설명 |
|--------|-----------|------|
| `<` | 1 | 미만 (Less Than) |
| `<=` | 2 | 이하 (Less Than or Equal) |
| `=` | 3 | 동등 (Equal) |
| `>=` | 4 | 이상 (Greater Than or Equal) |
| `>` | 5 | 초과 (Greater Than) |

> 참고: `<>` (부등호) 연산자는 포함되지 않습니다. B-tree 인덱스 검색에서 유용하지 않기 때문입니다.

### 연산자 패밀리 (Operator Families)

유사한 정렬 의미론을 가진 여러 데이터 타입은 연산자 패밀리(Operator Family) 로 그룹화됩니다:

- 단일 타입 연산자: 각 연산자 클래스에 포함
- 교차 타입 연산자(Cross-type Operators): 패밀리 전체에서 느슨하게 포함
- 플래너가 교차 타입 비교에 대해 추론 가능

### 필수 수학적 원칙

B-tree 연산자 클래스가 올바르게 작동하려면 다음 수학적 원칙을 만족해야 합니다:

#### 동등 연산자 `=` (동치 관계, Equivalence Relation)

```
반사성(Reflexive):    A = A
대칭성(Symmetric):    A = B  =>  B = A
이행성(Transitive):   A = B AND B = C  =>  A = C
```

#### 미만 연산자 `<` (강한 순서 관계, Strict Total Order)

```
비반사성(Irreflexive):   NOT (A < A)
이행성(Transitive):      A < B AND B < C  =>  A < C
삼분법(Trichotomy):      정확히 하나만 참: A < B, A = B, B < A
```

### 다중 데이터 타입 패밀리의 제약사항

다중 데이터 타입을 포함하는 연산자 패밀리에서는 데이터 타입 간 암시적/이진 강제 변환(coercion)이 정렬 순서를 변경하면 안 됩니다.

반례 - float8과 numeric:

`float8`과 `numeric`은 같은 연산자 패밀리에 포함될 수 없습니다:
- `float8`의 제한된 정확도로 인해 서로 다른 `numeric` 값이 같은 `float8` 값과 비교될 수 있음
- 이는 이행성 법칙을 위반합니다

```sql
-- 예시: 이행성 위반
-- numeric: 1.0000000000000001, 1.0000000000000002가 서로 다름
-- 하지만 float8로 변환 시 둘 다 동일한 값이 될 수 있음
```

### 다른 연산자들과의 관계

5개의 기본 연산자로부터 다음 관계가 유도됩니다:

```sql
A <= B  ==  A < B OR A = B
A >= B  ==  A > B OR A = B
A > B   ==  B < A
```

---

## 67.3. B-Tree 지원 함수 (B-Tree Support Functions)

B-tree 연산자 클래스는 여러 지원 함수를 제공할 수 있습니다. 일부는 필수이고 일부는 선택적입니다.

### 지원 함수 요약

| 번호 | 함수명 | 필수 | 용도 |
|------|--------|------|------|
| 1 | `order` | 필수 | 값 비교 (-1/0/+1 반환) |
| 2 | `sortsupport` | 선택 | 정렬 최적화 |
| 3 | `in_range` | 선택 | 윈도우 함수 RANGE OFFSET 지원 |
| 4 | `equalimage` | 선택 | 중복 제거(Deduplication) 안전성 판단 |
| 5 | `options` | 선택 | 사용자 정의 파라미터 |
| 6 | `skipsupport` | 선택 | Skip scan 최적화 |

### 67.3.1. order 함수 (Support Function #1) - 필수

```c
int32 order(A type, B type)
```

기능:
- 두 값을 비교하여 정수를 반환
- 반환 값:
  - `< 0`: A < B
  - `0`: A = B
  - `> 0`: A > B

예시 구현 위치: `src/backend/access/nbtree/nbtcompare.c`

```c
// 정수 비교 예시
Datum
btint4cmp(PG_FUNCTION_ARGS)
{
    int32 a = PG_GETARG_INT32(0);
    int32 b = PG_GETARG_INT32(1);

    if (a > b)
        PG_RETURN_INT32(1);
    else if (a < b)
        PG_RETURN_INT32(-1);
    else
        PG_RETURN_INT32(0);
}
```

중요 규칙:
- 모든 값은 비교 가능해야 함
- NULL 결과 반환 불허
- Collation 지원 시 `PG_GET_COLLATION()` 메커니즘 사용

### 67.3.2. sortsupport 함수 (Support Function #2) - 선택

목적: 정렬 목적의 비교를 기본 비교 함수보다 효율적으로 구현

API 정의 위치: `src/include/utils/sortsupport.h`

```c
// sortsupport 사용 예시 구조
typedef struct SortSupportData
{
    MemoryContext ssup_cxt;
    Oid ssup_collation;
    bool ssup_reverse;
    bool ssup_nulls_first;

    // 비교 함수 포인터
    int (*comparator) (Datum x, Datum y, SortSupport ssup);

    // 추가 최적화를 위한 필드들...
} SortSupportData;
```

### 67.3.3. in_range 함수 (Support Function #3) - 선택

```c
bool in_range(val type1, base type1, offset type2, sub bool, less bool)
```

사용 사례: 윈도우 함수의 `RANGE OFFSET PRECEDING/FOLLOWING` 프레임 경계 지원

의미론:

| sub | less | 의미 |
|-----|------|------|
| false | false | val >= (base + offset) |
| false | true | val <= (base + offset) |
| true | false | val >= (base - offset) |
| true | true | val <= (base - offset) |

예시 쿼리:

```sql
-- RANGE BETWEEN 사용 예시
SELECT
    product_id,
    sale_date,
    amount,
    SUM(amount) OVER (
        ORDER BY sale_date
        RANGE BETWEEN INTERVAL '7 days' PRECEDING AND CURRENT ROW
    ) as weekly_sum
FROM sales;
```

필수 검증:
- offset이 음수이면 에러 발생: `ERRCODE_INVALID_PRECEDING_OR_FOLLOWING_SIZE (22013)`
- 가능하면 오버플로우 회피 시도

### 67.3.4. equalimage 함수 (Support Function #4) - 선택

```c
bool equalimage(opcintype oid)
```

목적: B-tree 중복 제거(Deduplication) 최적화의 안전성 판단

반환 값 의미:
- `true`: `order` 함수가 `0`을 반환하는 경우, 인수들이 의미 정보 손실 없이 교환 가능
- `false` 또는 미등록: 조건 확보 불가

핵심 개념 - Image Equality:

```
datum_image_eq() C 함수가 order() 함수와 항상 동의하면 true 반환
```

데이터 타입별 지원 현황:

| 타입 | 함수 | 중복 제거 안전 여부 |
|------|------|---------------------|
| 대부분 기본 타입 | `btequalimage()` | 안전 |
| `text`, `varchar`, `char` | `btvarstrequalimage()` | 결정적(deterministic) collation만 안전 |
| `numeric` | - | 불안전 (표시 스케일 보존 필요) |
| `jsonb` | - | 불안전 (내부적으로 numeric 사용) |
| `float4`, `float8` | - | 불안전 (`-0`과 `0` 구별 필요) |

### 67.3.5. options 함수 (Support Function #5) - 선택

```c
void options(local_relopts *relopts)
```

목적: 연산자 클래스별 사용자 정의 파라미터 정의

현재 상태: B-tree에는 현재 구현된 options 함수가 없으며, 향후 확장을 위해 추가됨

### 67.3.6. skipsupport 함수 (Support Function #6) - 선택

API 정의 위치: `src/include/utils/skipsupport.h`

목적: Skip scan 최적화 지원

특징:
- 없어도 skip scan 가능하지만 최적화가 부족할 수 있음
- 연속 타입에는 일반적으로 불필요
- 교차 타입 함수는 지원되지 않음

Skip Scan 예시:

```sql
-- (x, y) 인덱스에서 y만으로 검색
-- Skip scan이 모든 가능한 x 값에 대해 검색 수행
SELECT * FROM table WHERE y = 100;
```

---

## 67.4. 구현 (Implementation)

### 67.4.1. B-Tree 구조

B-tree 인덱스는 계층적 구조로 구성됩니다:

```
메타페이지 (Meta Page)
    - 고정 위치 (첫 번째 세그먼트 파일)
    - 인덱스 메타데이터 저장
       │
       ▼
루트 페이지 (Root Page)
    - 트리의 최상위 레벨
       │
       ▼
내부 페이지 (Internal Pages)
    - 중간 레벨들
    - 다운링크(downlink) 포함
       │
       ▼
리프 페이지 (Leaf Pages)
    - 최하단 레벨
    - 전체 인덱스의 99% 이상 차지
    - 테이블 행(TID)을 가리키는 튜플 저장
```

### 페이지 분할 (Page Split)

인덱스 항목 삽입 시 페이지가 가득 차면 분할이 발생합니다:

```sql
-- 페이지 분할 과정
1. 기존 리프 페이지가 크기 초과
2. 일부 항목을 새 페이지로 이동
3. 부모 페이지에 새 다운링크 추가
4. 부모도 분할 필요 시 재귀적 분할
5. 루트 페이지 분할 시 트리 높이 증가
```

### 67.4.2. 상향식 인덱스 삭제 (Bottom-up Index Deletion)

배경: MVCC 환경에서 UPDATE로 인한 "version churn" 누적 문제

- 한 컬럼만 변경해도 모든 인덱스에 새 튜플이 필요
- HOT(Heap Only Tuple) 최적화가 불가능한 경우 특히 심각

메커니즘:

```
1. 예상되는 "version churn 페이지 분할" 시점 감지
2. Bottom-up 삭제 패스 트리거
3. 단일 리프 페이지의 추측되는 가비지 튜플 대상 삭제
4. 페이지 분할 회피 또는 성능 개선
```

Simple Index Deletion과의 비교:

| 특성 | Bottom-up Deletion | Simple Deletion |
|------|-------------------|-----------------|
| 동작 | 버전 churn 타겟 | 이미 안전한 튜플 삭제 |
| 트리거 | 페이지 분할 예상 시 | 페이지 분할 예상 시 |
| 기반 | 정성적 구별 | LP_DEAD 비트 설정 |
| 도입 | PostgreSQL 14+ | 14 이전 |

### 67.4.3. 중복 제거 (Deduplication)

정의: 리프 페이지 튜플에서 모든 인덱스 키 컬럼 값이 동일한 경우를 중복으로 취급

작동 방식:

```
중복 튜플 그룹
    │
    ▼
단일 posting list 튜플로 병합
    │
    ▼
키 값 1회 저장 + 정렬된 TID 배열
```

예시:

```sql
-- 중복이 많은 컬럼에 인덱스 생성
CREATE INDEX idx_status ON orders (status);

-- status 값이 'pending', 'completed', 'cancelled' 등 소수의 값만 가지면
-- 동일한 status 값을 가진 여러 행의 TID가 하나의 posting list로 병합됨
```

효과:
- 저장 공간 대폭 감소
- 쿼리 지연 시간 단축
- 처리량 증가
- VACUUM 오버헤드 감소

### Deduplication 제어

```sql
-- 인덱스별 중복 제거 비활성화
CREATE INDEX idx_name ON table_name (col)
  WITH (deduplicate_items = off);

-- 기본값은 on
CREATE INDEX idx_name ON table_name (col)
  WITH (deduplicate_items = on);
```

### Deduplication 불가능한 경우

의미론적 차이로 인한 제한:

| 타입 | 이유 |
|------|------|
| `text`, `varchar`, `char` | 비결정적(nondeterministic) collation 사용 시 대소문자/악센트 차이 보존 필요 |
| `numeric` | 표시 스케일(display scale) 보존 필요 |
| `jsonb` | 내부적으로 numeric 사용 |
| `float4`, `float8` | `-0`과 `0`의 구분 필요 (동일하지만 다른 표현) |

구현 제한:

| 타입 | 이유 | 향후 |
|------|------|------|
| 컨테이너 타입 (복합, 배열, 범위) | 구현 복잡성 | 개선 가능성 있음 |
| INCLUDE 인덱스 | 설계상 제한 | 영구적 제한 |

---

## 실용적인 B-Tree 인덱스 사용 예제

### 기본 인덱스 생성

```sql
-- 단일 컬럼 인덱스
CREATE INDEX idx_user_email ON users (email);

-- 다중 컬럼 인덱스
CREATE INDEX idx_order_status_date ON orders (status, created_at);

-- 고유 인덱스
CREATE UNIQUE INDEX idx_user_username ON users (username);
```

### 정렬 순서 지정

```sql
-- 내림차순 인덱스
CREATE INDEX idx_created_desc ON posts (created_at DESC);

-- NULL 처리 지정
CREATE INDEX idx_score ON scores (score DESC NULLS LAST);

-- 복합 정렬 순서
CREATE INDEX idx_multi_sort ON products (category ASC, price DESC);
```

### 부분 인덱스 (Partial Index)

```sql
-- 활성 사용자만 인덱싱
CREATE INDEX idx_active_users ON users (email)
  WHERE is_active = true;

-- 특정 상태만 인덱싱
CREATE INDEX idx_pending_orders ON orders (order_id)
  WHERE status = 'pending';
```

### 표현식 인덱스 (Expression Index)

```sql
-- 대소문자 구분 없는 검색용
CREATE INDEX idx_email_lower ON users (lower(email));

-- 날짜 일부 추출
CREATE INDEX idx_order_year ON orders (EXTRACT(YEAR FROM created_at));

-- JSON 필드 인덱싱
CREATE INDEX idx_data_name ON documents ((data->>'name'));
```

### 커버링 인덱스 (Covering Index)

```sql
-- INCLUDE를 사용한 커버링 인덱스
CREATE INDEX idx_user_covering ON users (email) INCLUDE (name, created_at);

-- 이 쿼리는 인덱스 전용 스캔 가능
SELECT name, created_at FROM users WHERE email = 'test@example.com';
```

### 인덱스 사용 확인

```sql
-- EXPLAIN으로 인덱스 사용 확인
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- 예상 출력:
-- Index Scan using idx_user_email on users  (cost=0.29..8.31 rows=1 width=100)
--   Index Cond: (email = 'test@example.com'::text)
```

---

## B-Tree 인덱스 성능 최적화 가이드

### 인덱스가 효과적인 경우

1. 높은 선택성(Selectivity): 조건이 전체 행의 작은 비율만 선택
2. 범위 쿼리: `BETWEEN`, `>`, `<` 등의 범위 조건
3. 정렬된 출력: `ORDER BY` 절과 일치하는 경우
4. 조인 조건: 외래 키 컬럼

### 인덱스가 비효율적인 경우

1. 낮은 선택성: 대부분의 행을 반환하는 조건
2. 매우 작은 테이블: 순차 스캔이 더 빠름
3. 빈번한 UPDATE가 있는 컬럼: 인덱스 유지 비용 증가

### 권장 사항

```sql
-- 통계 업데이트로 플래너 정확도 향상
ANALYZE table_name;

-- 인덱스 팽창(bloat) 확인 및 재구축
REINDEX INDEX idx_name;

-- 인덱스 사용 통계 확인
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'your_table';
```

---

## 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 67. B-Tree Indexes](https://www.postgresql.org/docs/current/btree.html)
- [PostgreSQL 소스 코드 - nbtree](https://github.com/postgres/postgres/tree/master/src/backend/access/nbtree)
- [CREATE INDEX 문서](https://www.postgresql.org/docs/current/sql-createindex.html)
- [인덱스 유지보수](https://www.postgresql.org/docs/current/routine-reindex.html)
