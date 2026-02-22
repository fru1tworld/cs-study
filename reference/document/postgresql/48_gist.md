# Chapter 68: GiST 인덱스 (GiST Indexes)

## 68.1 소개 (Introduction)

GiST (Generalized Search Tree, 일반화 검색 트리)는 균형 잡힌 트리 구조의 액세스 메서드로, 다양한 인덱싱 스키마를 구현하기 위한 기본 템플릿 역할을 합니다. B-트리, R-트리 및 기타 많은 인덱싱 스키마가 GiST를 사용하여 구현될 수 있습니다.

### 주요 장점

GiST의 가장 큰 장점은 확장성(Extensibility) 입니다:

1. 도메인 전문가가 커스텀 데이터 타입 개발 가능: 적절한 액세스 메서드를 갖춘 새로운 데이터 타입을 개발할 수 있습니다.
2. 높은 수준의 추상화: 구현자는 데이터 타입의 의미론(semantics)만 이해하면 됩니다.
3. GiST 계층이 관리: 동시성(concurrency), 로깅(logging), 트리 구조 관리는 GiST 계층이 처리합니다.

### GiST의 역사

GiST 인덱싱 방법은 Joseph M. Hellerstein, Jeffrey F. Naughton, Avi Pfeffer가 개발했으며, 이들의 논문 "Generalized Search Trees for Database Systems"에서 처음 발표되었습니다.

---

## 68.2 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 다음 표에 나열된 GiST 연산자 클래스가 포함되어 있습니다. `contrib` 컬렉션의 많은 애드온 모듈도 추가적인 GiST 연산자 클래스를 제공합니다.

### 내장 GiST 연산자 클래스 표

| 연산자 클래스 (Operator Class) | 인덱싱 가능 연산자 (Indexable Operators) |
|-------------------------------|----------------------------------------|
| `box_ops` | `<<` `&<` `&&` `&>` `>>` `~=` `@>` `<@` `&<\|` `<<\|` `\|>>` `\|&>` |
| `circle_ops` | `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `\|>>` `<<\|` `&<\|` `\|&>` |
| `inet_ops` | `<<` `<<=` `>>` `>>=` `=` `<>` `<` `<=` `>` `>=` `&&` |
| `point_ops` | `\|>>` `<<` `>>` `<<\|` `~=` `<@` (box/polygon/circle) |
| `poly_ops` | `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|&>` `\|>>` |
| `range_ops` | `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-` |
| `multirange_ops` | `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-` |
| `tsquery_ops` | `<@` `@>` |
| `tsvector_ops` | `@@` |

### inet_ops 사용 시 주의사항

역사적인 이유로 `inet_ops` 연산자 클래스는 `inet`과 `cidr` 타입의 기본 클래스가 아닙니다. 이를 사용하려면 명시적으로 지정해야 합니다:

```sql
CREATE INDEX ON my_table USING GIST (my_inet_column inet_ops);
```

### 기하학적 연산자 설명

| 연산자 | 설명 |
|--------|------|
| `<<` | 왼쪽에 있음 (strictly left of) |
| `>>` | 오른쪽에 있음 (strictly right of) |
| `&<` | 오른쪽으로 확장하지 않음 (does not extend to right of) |
| `&>` | 왼쪽으로 확장하지 않음 (does not extend to left of) |
| `<<\|` | 아래에 있음 (strictly below) |
| `\|>>` | 위에 있음 (strictly above) |
| `&<\|` | 위로 확장하지 않음 (does not extend above) |
| `\|&>` | 아래로 확장하지 않음 (does not extend below) |
| `&&` | 겹침 (overlaps) |
| `@>` | 포함함 (contains) |
| `<@` | 포함됨 (contained by) |
| `~=` | 동일함 (same as) |

---

## 68.3 확장성 (Extensibility)

전통적으로 새로운 인덱스 액세스 메서드를 구현하는 것은 매우 어려운 작업이었습니다. 동시성 및 로깅의 내부 동작을 이해해야 했기 때문입니다. GiST 인터페이스는 높은 수준의 추상화를 제공하여, 액세스 메서드 구현자가 액세스되는 데이터 타입의 의미론만 구현하면 됩니다.

GiST 연산자 클래스를 구현하려면 여러 메서드를 제공해야 합니다.

### 필수 메서드 (Required Methods) - 5개

#### 1. consistent

인덱스 항목이 쿼리 조건과 일치하는지 확인합니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_consistent(internal, data_type, smallint, oid, internal)
RETURNS bool
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_consistent);

Datum
my_consistent(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    data_type  *query = PG_GETARG_DATA_TYPE_P(1);
    StrategyNumber strategy = (StrategyNumber) PG_GETARG_UINT16(2);
    /* Oid subtype = PG_GETARG_OID(3); */
    bool       *recheck = (bool *) PG_GETARG_POINTER(4);
    data_type  *key = DatumGetDataType(entry->key);
    bool        retval;

    /*
     * strategy, key, query를 기반으로 반환 값 결정
     * recheck가 true면 후보(candidate)이며 검증 필요
     * recheck가 false면 정확한 일치
     */
    *recheck = true;

    PG_RETURN_BOOL(retval);
}
```

매개변수 설명:
- `entry`: 인덱스 항목 (GISTENTRY 구조체)
- `query`: 검색할 쿼리 값
- `strategy`: 적용할 연산자를 식별하는 전략 번호 (Strategy Number)
- `subtype`: 연산자의 서브타입 OID
- `recheck`: 결과가 후보인지(true) 정확한 일치인지(false) 표시

#### 2. union

트리의 정보를 통합하여 주어진 모든 항목을 나타내는 새 인덱스 항목을 생성합니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_union(internal, internal)
RETURNS storage_type
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_union);

Datum
my_union(PG_FUNCTION_ARGS)
{
    GistEntryVector *entryvec = (GistEntryVector *) PG_GETARG_POINTER(0);
    GISTENTRY  *ent = entryvec->vector;
    data_type  *out, *tmp, *old;
    int         numranges = entryvec->n;
    int         i;

    tmp = DatumGetDataType(ent[0].key);
    out = tmp;

    if (numranges == 1)
    {
        out = data_type_deep_copy(tmp);
        PG_RETURN_DATA_TYPE_P(out);
    }

    for (i = 1; i < numranges; i++)
    {
        old = out;
        tmp = DatumGetDataType(ent[i].key);
        out = my_union_implementation(out, tmp);
    }

    PG_RETURN_DATA_TYPE_P(out);
}
```

중요 사항:
- 반드시 새로 `palloc()`된 메모리를 반환해야 합니다.
- 결과는 인덱스의 저장 타입이어야 합니다.

#### 3. penalty

트리 분기에 항목을 삽입하는 비용 값을 반환합니다. 항목은 최소 페널티 경로를 따릅니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_penalty(internal, internal, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_penalty);

Datum
my_penalty(PG_FUNCTION_ARGS)
{
    GISTENTRY  *origentry = (GISTENTRY *) PG_GETARG_POINTER(0);
    GISTENTRY  *newentry = (GISTENTRY *) PG_GETARG_POINTER(1);
    float      *penalty = (float *) PG_GETARG_POINTER(2);
    data_type  *orig = DatumGetDataType(origentry->key);
    data_type  *new_val = DatumGetDataType(newentry->key);

    *penalty = my_penalty_implementation(orig, new_val);
    PG_RETURN_POINTER(penalty);
}
```

중요 사항:
- 결과는 세 번째 인수 포인터를 통해 저장됩니다.
- 음수 값은 0으로 처리됩니다.

#### 4. picksplit

페이지 분할 시 어떤 항목이 이전 페이지에 남고 어떤 항목이 새 페이지로 이동할지 결정합니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_picksplit(internal, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_picksplit);

Datum
my_picksplit(PG_FUNCTION_ARGS)
{
    GistEntryVector *entryvec = (GistEntryVector *) PG_GETARG_POINTER(0);
    GIST_SPLITVEC *v = (GIST_SPLITVEC *) PG_GETARG_POINTER(1);
    OffsetNumber maxoff = entryvec->n - 1;
    GISTENTRY  *ent = entryvec->vector;
    int         i;

    /* 왼쪽/오른쪽 분할을 위한 공간 할당 */
    v->spl_left = (OffsetNumber *) palloc((maxoff + 1) * sizeof(OffsetNumber));
    v->spl_right = (OffsetNumber *) palloc((maxoff + 1) * sizeof(OffsetNumber));
    v->spl_nleft = 0;
    v->spl_nright = 0;

    /* 항목 분할 및 유니온 계산 */
    data_type *unionL = NULL;
    data_type *unionR = NULL;

    for (i = FirstOffsetNumber; i <= maxoff; i = OffsetNumberNext(i))
    {
        data_type *tmp = DatumGetDataType(ent[i].key);

        if (my_choice_is_left(unionL, unionR, tmp))
        {
            if (unionL == NULL)
                unionL = data_type_copy(tmp);
            else
                unionL = my_union_implementation(unionL, tmp);

            v->spl_left[v->spl_nleft] = i;
            v->spl_nleft++;
        }
        else
        {
            if (unionR == NULL)
                unionR = data_type_copy(tmp);
            else
                unionR = my_union_implementation(unionR, tmp);

            v->spl_right[v->spl_nright] = i;
            v->spl_nright++;
        }
    }

    v->spl_ldatum = DataTypeGetDatum(unionL);
    v->spl_rdatum = DataTypeGetDatum(unionR);
    PG_RETURN_POINTER(v);
}
```

GIST_SPLITVEC 구조체 필드:
- `spl_left`: 왼쪽 페이지에 남을 항목들의 오프셋 배열
- `spl_right`: 오른쪽 페이지로 이동할 항목들의 오프셋 배열
- `spl_nleft`: 왼쪽 항목 수
- `spl_nright`: 오른쪽 항목 수
- `spl_ldatum`: 왼쪽 항목들의 유니온
- `spl_rdatum`: 오른쪽 항목들의 유니온

#### 5. same

두 인덱스 항목이 동일하면 true, 그렇지 않으면 false를 반환합니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_same(storage_type, storage_type, internal)
RETURNS internal
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_same);

Datum
my_same(PG_FUNCTION_ARGS)
{
    data_type  *v1 = PG_GETARG_DATA_TYPE_P(0);
    data_type  *v2 = PG_GETARG_DATA_TYPE_P(1);
    bool       *result = (bool *) PG_GETARG_POINTER(2);

    *result = my_eq(v1, v2);
    PG_RETURN_POINTER(result);
}
```

---

### 선택적 메서드 (Optional Methods) - 7개

#### 1. compress

데이터를 물리적 저장에 적합한 형식으로 변환합니다. 생략하면 데이터가 수정 없이 저장됩니다.

```c
PG_FUNCTION_INFO_V1(my_compress);

Datum
my_compress(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    GISTENTRY  *retval;

    if (entry->leafkey)  /* 리프 노드인 경우 */
    {
        compressed_data_type *compressed = palloc(sizeof(compressed_data_type));
        /* entry->key로부터 compressed 데이터 채우기 */

        retval = palloc(sizeof(GISTENTRY));
        gistentryinit(*retval, PointerGetDatum(compressed),
                      entry->rel, entry->page, entry->offset, FALSE);
    }
    else
    {
        retval = entry;  /* 비리프 항목은 일반적으로 변경 없음 */
    }

    PG_RETURN_POINTER(retval);
}
```

#### 2. decompress

저장된 표현을 GiST 메서드가 조작할 수 있는 형식으로 다시 변환합니다.

```c
PG_FUNCTION_INFO_V1(my_decompress);

Datum
my_decompress(PG_FUNCTION_ARGS)
{
    /* 압축 해제가 필요 없으면 no-op */
    PG_RETURN_POINTER(PG_GETARG_POINTER(0));
}
```

#### 3. distance

인덱스 항목과 쿼리 값 사이의 거리를 결정합니다. 연산자 클래스에 정렬 연산자가 있는 경우 필수입니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_distance(internal, data_type, smallint, oid, internal)
RETURNS float8
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_distance);

Datum
my_distance(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    data_type  *query = PG_GETARG_DATA_TYPE_P(1);
    StrategyNumber strategy = (StrategyNumber) PG_GETARG_UINT16(2);
    /* Oid subtype = PG_GETARG_OID(3); */
    bool       *recheck = (bool *) PG_GETARG_POINTER(4);
    data_type  *key = DatumGetDataType(entry->key);
    double      retval;

    /* strategy, key, query를 기반으로 거리 계산 */
    retval = calculate_distance(key, query, strategy);

    /* 근사치인 경우 recheck 설정 */
    *recheck = false;

    PG_RETURN_FLOAT8(retval);
}
```

중요 사항:
- 내부 노드의 경우, 반환된 거리는 어떤 자식까지의 거리보다 크면 안 됩니다.
- 근사치인 경우 `*recheck = true`로 설정합니다.

#### 4. fetch

인덱스 전용 스캔(Index-Only Scan)을 위해 압축된 인덱스 표현을 원래 데이터 타입으로 변환합니다.

```c
PG_FUNCTION_INFO_V1(my_fetch);

Datum
my_fetch(PG_FUNCTION_ARGS)
{
    GISTENTRY  *entry = (GISTENTRY *) PG_GETARG_POINTER(0);
    input_data_type *in = DatumGetPointer(entry->key);
    fetched_data_type *fetched;
    GISTENTRY  *retval;

    retval = palloc(sizeof(GISTENTRY));
    fetched = palloc(sizeof(fetched_data_type));

    /* 저장된 데이터를 원래 데이터 타입의 Datum으로 변환 */
    /* ... 변환 로직 ... */

    gistentryinit(*retval, PointerGetDatum(fetched),
                  entry->rel, entry->page, entry->offset, FALSE);

    PG_RETURN_POINTER(retval);
}
```

참고: `compress`가 리프 항목에 대해 손실이 없는 경우에만 필요합니다.

#### 5. options

연산자 클래스 동작을 제어하는 사용자 가시적 매개변수를 허용합니다.

```c
typedef struct
{
    int32   vl_len_;       /* varlena 헤더 */
    int     int_param;     /* 정수 매개변수 */
    double  real_param;    /* 실수 매개변수 */
} MyOptionsStruct;

PG_FUNCTION_INFO_V1(my_options);

Datum
my_options(PG_FUNCTION_ARGS)
{
    local_relopts *relopts = (local_relopts *) PG_GETARG_POINTER(0);

    init_local_reloptions(relopts, sizeof(MyOptionsStruct));
    add_local_int_reloption(relopts, "int_param", "정수 매개변수 설명",
                            100, 0, 1000000,
                            offsetof(MyOptionsStruct, int_param));
    add_local_real_reloption(relopts, "real_param", "실수 매개변수 설명",
                             1.0, 0.0, 1000000.0,
                             offsetof(MyOptionsStruct, real_param));

    PG_RETURN_VOID();
}
```

다른 함수에서 옵션 접근:
```c
if (PG_HAS_OPCLASS_OPTIONS())
{
    MyOptionsStruct *options = (MyOptionsStruct *) PG_GET_OPCLASS_OPTIONS();
    int_param = options->int_param;
    real_param = options->real_param;
}
```

#### 6. sortsupport

지역성(locality)을 유지하면서 데이터를 정렬하는 비교자 함수를 반환합니다. 인덱스 생성 시 더 빠른 빌드를 위해 사용됩니다.

```c
PG_FUNCTION_INFO_V1(my_sortsupport);

static int
my_fastcmp(Datum x, Datum y, SortSupport ssup)
{
    /* 공간 코드(예: Z-order, Hilbert curve) 계산 */
    int z1 = ComputeSpatialCode(x);
    int z2 = ComputeSpatialCode(y);

    if (z1 == z2)
        return 0;
    return (z1 > z2) ? 1 : -1;
}

Datum
my_sortsupport(PG_FUNCTION_ARGS)
{
    SortSupport ssup = (SortSupport) PG_GETARG_POINTER(0);
    ssup->comparator = my_fastcmp;
    PG_RETURN_VOID();
}
```

#### 7. translate_cmptype

`CompareType` 값을 전략 번호(strategy numbers)로 변환합니다. 시간적 인덱스 제약 조건에 사용됩니다.

SQL 함수 정의:
```sql
CREATE OR REPLACE FUNCTION my_translate_cmptype(integer)
RETURNS smallint
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

C 구현:
```c
PG_FUNCTION_INFO_V1(my_translate_cmptype);

Datum
my_translate_cmptype(PG_FUNCTION_ARGS)
{
    CompareType cmptype = PG_GETARG_INT32(0);
    StrategyNumber ret = InvalidStrategy;

    switch (cmptype)
    {
        case COMPARE_EQ:
            ret = BTEqualStrategyNumber;
            break;
        case COMPARE_LT:
            ret = BTLessStrategyNumber;
            break;
        /* ... 기타 케이스 ... */
    }

    PG_RETURN_UINT16(ret);
}
```

연산자 패밀리에 등록:
```sql
ALTER OPERATOR FAMILY my_opfamily USING gist ADD
    FUNCTION 12 ("any", "any") my_translate_cmptype(int);
```

---

## 68.4 구현 상세 (Implementation)

### GiST 인덱스 빌드 방법

GiST 인덱스 구축에는 여러 가지 방법이 있으며, 상황에 따라 각각의 장단점이 있습니다.

#### 1. 정렬 방법 (Sorted Method) - 기본값

- 조건: 모든 연산자 클래스가 `sortsupport` 함수를 제공할 때 사용됩니다.
- 장점: 대부분의 데이터셋에 가장 효율적입니다.
- 동작: 데이터를 먼저 정렬한 후 트리를 bottom-up 방식으로 구축합니다.

```sql
-- sortsupport가 있는 경우 자동으로 정렬 방법 사용
CREATE INDEX idx_point ON locations USING GIST (point_column);
```

#### 2. 버퍼링 방법 (Buffered Method)

- 장점: 정렬되지 않은 데이터셋에 대해 무작위 I/O를 크게 줄입니다.
- 단점: I/O 감소를 위해 CPU를 더 사용하며, 인덱스 크기만큼의 임시 디스크 공간이 필요합니다.
- 자동 활성화: 인덱스 크기가 `effective_cache_size`에 도달하면 자동으로 사용됩니다.

```sql
-- 버퍼링 빌드 명시적 활성화
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=on);

-- 버퍼링 빌드 비활성화
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=off);

-- 자동 결정 (기본값)
CREATE INDEX idx_geo ON geo_data USING GIST (geom) WITH (buffering=auto);
```

### 트리 구조

GiST 인덱스는 B-트리와 유사한 균형 트리 구조를 가집니다:

```
         [루트 노드]
        /    |    \
   [내부]  [내부]  [내부]
   / | \   / | \   / | \
[리프] ... [리프] ... [리프]
```

- 내부 노드 (Internal Nodes): 하위 항목들의 "바운딩 박스" 또는 유니온을 저장합니다.
- 리프 노드 (Leaf Nodes): 실제 인덱스 키를 저장합니다.
- 균형 유지: 모든 리프 노드는 루트로부터 같은 깊이에 있습니다.

### 동시성 제어

GiST는 동시 접근을 위한 정교한 잠금 메커니즘을 사용합니다:

1. 읽기 작업: 페이지 단위 공유 잠금 사용
2. 쓰기 작업: 페이지 단위 배타적 잠금 사용
3. 트리 수정: 필요한 경우에만 부모 노드 잠금

---

## 68.5 예제 (Examples)

### 기본 GiST 인덱스 생성

```sql
-- 기하학적 데이터에 대한 GiST 인덱스
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    position POINT
);

CREATE INDEX idx_locations_pos ON locations USING GIST (position);

-- 인덱스를 사용한 쿼리
SELECT * FROM locations
WHERE position <@ BOX '((0,0),(10,10))';  -- 박스 내의 점 검색
```

### 범위 타입에 대한 GiST 인덱스

```sql
-- 범위 타입 테이블
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    room_id INTEGER,
    during TSRANGE
);

CREATE INDEX idx_reservations_during ON reservations USING GIST (during);

-- 겹치는 예약 검색
SELECT * FROM reservations
WHERE during && '[2024-01-01, 2024-01-07)'::tsrange;

-- 제외 제약 조건 (같은 방에 겹치는 예약 방지)
ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_reservations
EXCLUDE USING GIST (room_id WITH =, during WITH &&);
```

### 전체 텍스트 검색에 대한 GiST 인덱스

```sql
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    body TEXT,
    tsv TSVECTOR
);

-- tsvector 컬럼에 GiST 인덱스 생성
CREATE INDEX idx_documents_tsv ON documents USING GIST (tsv);

-- 전체 텍스트 검색 쿼리
SELECT * FROM documents
WHERE tsv @@ to_tsquery('postgresql & index');
```

### 네트워크 주소에 대한 GiST 인덱스

```sql
CREATE TABLE network_blocks (
    id SERIAL PRIMARY KEY,
    network INET,
    description TEXT
);

-- inet_ops 명시적 지정 필요
CREATE INDEX idx_network ON network_blocks USING GIST (network inet_ops);

-- 서브넷 포함 검색
SELECT * FROM network_blocks
WHERE network >>= '192.168.1.0/24';
```

---

## 68.6 contrib 모듈의 GiST 지원

PostgreSQL `contrib` 컬렉션에는 GiST를 활용하는 여러 유용한 모듈이 있습니다:

| 모듈 | 용도 | 인덱싱 대상 |
|------|------|-------------|
| `btree_gist` | B-트리 동등 기능을 GiST로 제공 | 스칼라 타입 (integer, text 등) |
| `cube` | 다차원 큐브 인덱싱 | N차원 큐브 |
| `hstore` | 키-값 쌍 저장 및 검색 | 키-값 데이터 |
| `intarray` | 정수 배열의 RD-Tree | 1차원 int4 배열 |
| `ltree` | 트리 구조 레이블 경로 | 계층적 레이블 |
| `pg_trgm` | 트라이그램 기반 텍스트 유사도 | 텍스트 유사도 검색 |
| `seg` | 부동소수점 범위 인덱싱 | 숫자 범위 |

### btree_gist 예제

`btree_gist`를 사용하면 스칼라 타입에 대해 GiST 인덱스를 생성하고 제외 제약 조건에서 사용할 수 있습니다:

```sql
-- 확장 설치
CREATE EXTENSION btree_gist;

-- 회의실 예약 테이블
CREATE TABLE meetings (
    id SERIAL PRIMARY KEY,
    room_id INTEGER,
    during TSRANGE
);

-- 같은 방에 겹치는 회의 방지
ALTER TABLE meetings
ADD CONSTRAINT no_overlapping_meetings
EXCLUDE USING GIST (room_id WITH =, during WITH &&);
```

### pg_trgm 예제

```sql
-- 확장 설치
CREATE EXTENSION pg_trgm;

-- 트라이그램 GiST 인덱스 생성
CREATE INDEX idx_trgm ON documents USING GIST (title gist_trgm_ops);

-- 유사도 검색
SELECT * FROM documents
WHERE title % 'PostgreSQL'  -- 유사한 제목 검색
ORDER BY title <-> 'PostgreSQL';  -- 유사도 순 정렬
```

---

## 68.7 메모리 관리 (Memory Management)

GiST 지원 메서드는 일반적으로 각 튜플 처리 후 리셋되는 단기 메모리 컨텍스트에서 실행됩니다. 호출 간 데이터를 캐시하려면 다음과 같이 합니다:

```c
/* 더 오래 지속되는 컨텍스트에 할당 */
if (fcinfo->flinfo->fn_extra == NULL)
{
    MemoryContext oldcxt;

    oldcxt = MemoryContextSwitchTo(fcinfo->flinfo->fn_mcxt);
    fcinfo->flinfo->fn_extra = palloc(sizeof(CachedData));
    MemoryContextSwitchTo(oldcxt);

    /* 캐시 데이터 초기화 */
    initialize_cache((CachedData *) fcinfo->flinfo->fn_extra);
}

/* 데이터는 인덱스 작업 기간 동안 유지됨 */
CachedData *cached = (CachedData *) fcinfo->flinfo->fn_extra;

/* fn_extra를 교체할 때는 이전 값을 pfree()하여 메모리 누수 방지 */
```

---

## 68.8 GiST vs 다른 인덱스 타입

| 특성 | GiST | B-tree | GIN | SP-GiST |
|------|------|--------|-----|---------|
| 용도 | 범용 확장 가능 | 비교 가능한 데이터 | 복합 값 검색 | 불균형 구조 |
| 기하학적 데이터 | 우수 | 미지원 | 미지원 | 지원 |
| 범위 쿼리 | 우수 | 우수 | 제한적 | 지원 |
| 전체 텍스트 | 지원 | 미지원 | 우수 | 미지원 |
| 최근접 이웃 | 지원 | 미지원 | 미지원 | 지원 |
| 인덱스 크기 | 중간 | 작음 | 큼 | 작음 |
| 빌드 속도 | 보통 | 빠름 | 느림 | 보통 |

---

## 68.9 성능 고려사항 (Performance Considerations)

### 인덱스 선택 시 고려사항

1. 데이터 타입: 기하학적 데이터, 범위, 전체 텍스트에는 GiST가 적합합니다.
2. 쿼리 패턴: 포함, 겹침, 최근접 이웃 쿼리에 효과적입니다.
3. 업데이트 빈도: 빈번한 업데이트가 있는 경우 B-tree보다 느릴 수 있습니다.

### 성능 최적화 팁

```sql
-- 인덱스 통계 확인
SELECT * FROM pg_stat_user_indexes
WHERE indexrelname = 'idx_locations_pos';

-- 인덱스 크기 확인
SELECT pg_size_pretty(pg_relation_size('idx_locations_pos'));

-- EXPLAIN ANALYZE로 인덱스 사용 확인
EXPLAIN ANALYZE
SELECT * FROM locations
WHERE position <@ BOX '((0,0),(10,10))';

-- 인덱스 재구축 (필요시)
REINDEX INDEX idx_locations_pos;
```

### fillfactor 설정

자주 업데이트되는 테이블의 경우 fillfactor를 낮춰 페이지 분할을 줄일 수 있습니다:

```sql
CREATE INDEX idx_geo ON geo_data USING GIST (geom)
WITH (fillfactor = 90);
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - GiST Indexes](https://www.postgresql.org/docs/current/gist.html)
- [GiST Development Guide](https://www.postgresql.org/docs/current/gist-extensibility.html)
- Hellerstein, J.M., Naughton, J.F., Pfeffer, A. "Generalized Search Trees for Database Systems"
