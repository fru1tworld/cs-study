# Chapter 69. SP-GiST 인덱스 (SP-GiST Indexes)

PostgreSQL 18 공식 문서 번역

---

## 목차

- [69.1. 소개](#691-소개-introduction)
- [69.2. 내장 연산자 클래스](#692-내장-연산자-클래스-built-in-operator-classes)
- [69.3. 확장성](#693-확장성-extensibility)
- [69.4. 구현](#694-구현-implementation)

---

## 69.1. 소개 (Introduction)

SP-GiST는 Space-Partitioned GiST 의 약자로, 분할 검색 트리(partitioned search trees)를 지원하는 인덱스 접근 방법입니다. SP-GiST를 사용하면 다음과 같은 다양한 비균형(non-balanced) 디스크 기반 데이터 구조를 구현할 수 있습니다:

- 쿼드 트리 (Quad-trees): 2차원 공간을 4개의 사분면으로 재귀적으로 분할
- k-d 트리 (K-d trees): k차원 점 데이터를 이진 분할
- 기수 트리 (Radix trees, Tries): 문자열의 공통 접두사를 기반으로 분할

### SP-GiST의 특징

SP-GiST는 검색 공간을 동일하지 않은 크기의 파티션으로 반복적으로 분할 하는 구조를 지원합니다. 쿼리가 분할 규칙과 잘 일치하면 매우 빠른 검색이 가능합니다.

전통적인 메모리 기반 검색 트리에서 발생하는 주요 문제는 노드를 디스크 페이지에 효율적으로 매핑하는 것입니다. SP-GiST는 이 문제를 해결하기 위해 설계되었습니다:

- 포인터 기반 구조 대신 디스크 페이지에 최적화된 구조 사용
- 높은 팬아웃(fanout)으로 I/O 연산 최소화
- 많은 노드를 순회하더라도 소수의 디스크 페이지만 접근

### SP-GiST 트리의 구조

SP-GiST 트리는 두 가지 유형의 튜플로 구성됩니다:

#### 내부 튜플 (Inner Tuples)

검색 트리의 분기점(branch points)으로, 하나 이상의 노드(nodes) 를 포함합니다:

```
내부 튜플 구조:
┌─────────────────────────────────────────────────┐
│ [선택적 접두사] + [노드1] + [노드2] + ... + [노드n] │
└─────────────────────────────────────────────────┘
```

각 노드는 다음을 포함합니다:
- 레이블 (Label): 노드를 설명 (예: 기수 트리에서 다음 문자)
- 다운링크 (Downlink): 하위 내부 튜플이나 리프 튜플 목록을 가리킴
- 접두사 (Prefix): 선택적으로 모든 멤버에 공통적인 값 설명

#### 리프 튜플 (Leaf Tuples)

인덱싱된 컬럼의 실제 값을 포함합니다:

- 인덱싱된 컬럼과 동일한 데이터 타입의 값 저장
- 손실 표현(lossy representation)이나 부분 값 저장 가능
- INCLUDE 컬럼 값 저장 가능 (연산자 클래스에 투명하게 처리)

### 쿼드 트리 예제

점(point) 데이터를 저장하는 쿼드 트리의 구조:

```
                    ┌─────────────────┐
                    │   내부 튜플      │
                    │ (중심점: 5,5)   │
                    └────────┬────────┘
           ┌─────────┬───────┴───────┬─────────┐
           │         │               │         │
        ┌──▼──┐   ┌──▼──┐        ┌──▼──┐   ┌──▼──┐
        │ NE  │   │ NW  │        │ SE  │   │ SW  │
        │사분면│   │사분면│        │사분면│   │사분면│
        └─────┘   └─────┘        └─────┘   └─────┘
```

### 기수 트리 (Radix Tree / Trie) 예제

문자열 데이터를 저장하는 기수 트리:

```
                        ┌─────┐
                        │ 루트│
                        └──┬──┘
              ┌────────────┼────────────┐
              │            │            │
           ┌──▼──┐      ┌──▼──┐      ┌──▼──┐
           │ 'h' │      │ 'w' │      │ 'p' │
           └──┬──┘      └──┬──┘      └──┬──┘
              │            │            │
           ┌──▼───┐     ┌──▼───┐     ┌──▼───┐
           │'ello'│     │'orld'│     │'ost' │
           │      │     │      │     │      │
           │hello │     │world │     │post  │
           └──────┘     └──────┘     └──────┘
```

---

## 69.2. 내장 연산자 클래스 (Built-in Operator Classes)

PostgreSQL의 핵심 배포판에는 다음 표에 나열된 SP-GiST 연산자 클래스가 포함되어 있습니다.

### SP-GiST 내장 연산자 클래스 목록

| 연산자 클래스 | 인덱싱 타입 | 인덱싱 가능 연산자 |
|-------------|-----------|------------------|
| `box_ops` | `box` | `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|&>` `\|>>` |
| `inet_ops` | `inet`, `cidr` | `<<` `<<=` `>>` `>>=` `=` `<>` `<` `<=` `>` `>=` `&&` |
| `kd_point_ops` | `point` | `\|>>` `<->` `<<` `>>` `<<\|` `~=` `<@` |
| `quad_point_ops` | `point` | `\|>>` `<->` `<<` `>>` `<<\|` `~=` `<@` |
| `poly_ops` | `polygon` | `<<` `&<` `&>` `>>` `<@` `@>` `~=` `&&` `<<\|` `&<\|` `\|>>` `\|&>` |
| `range_ops` | 모든 범위 타입 | `=` `&&` `@>` `<@` `<<` `>>` `&<` `&>` `-\|-` |
| `text_ops` | `text` | `=` `<` `<=` `>` `>=` `~<~` `~<=~` `~>=~` `~>~` `^@` |

### 연산자 클래스 상세 설명

#### box_ops (상자 연산자 클래스)

`box` 타입에 대한 SP-GiST 인덱스입니다. 쿼드 트리를 사용하여 2차원 상자를 인덱싱합니다.

지원 연산자:
- `<<`: 왼쪽에 엄격히 위치 (strictly left of)
- `&<`: 오른쪽으로 확장되지 않음 (does not extend to the right of)
- `&>`: 왼쪽으로 확장되지 않음 (does not extend to the left of)
- `>>`: 오른쪽에 엄격히 위치 (strictly right of)
- `<@`: 포함됨 (contained by)
- `@>`: 포함 (contains)
- `~=`: 동일 (same as)
- `&&`: 겹침 (overlaps)
- `<<|`: 아래에 엄격히 위치 (strictly below)
- `&<|`: 위로 확장되지 않음 (does not extend above)
- `|&>`: 아래로 확장되지 않음 (does not extend below)
- `|>>`: 위에 엄격히 위치 (strictly above)

#### inet_ops (IP 주소 연산자 클래스)

`inet` 및 `cidr` 타입에 대한 SP-GiST 인덱스입니다. 기수 트리를 사용합니다.

지원 연산자:
- `<<`: 서브넷에 포함됨 (is subnet)
- `<<=`: 서브넷이거나 같음 (is subnet or equal)
- `>>`: 슈퍼넷에 포함됨 (is supernet)
- `>>=`: 슈퍼넷이거나 같음 (is supernet or equal)
- `=`, `<>`, `<`, `<=`, `>`, `>=`: 비교 연산자
- `&&`: 겹침 (overlaps)

#### quad_point_ops (쿼드 포인트 연산자 클래스)

`point` 타입의 기본 연산자 클래스입니다. 쿼드 트리를 사용하여 2차원 점을 인덱싱합니다.

지원 연산자:
- `|>>`: 위에 엄격히 위치
- `<->`: 거리 (k-NN 검색 지원)
- `<<`: 왼쪽에 엄격히 위치
- `>>`: 오른쪽에 엄격히 위치
- `<<|`: 아래에 엄격히 위치
- `~=`: 동일
- `<@`: 상자/다각형에 포함됨

#### kd_point_ops (k-d 포인트 연산자 클래스)

`point` 타입에 대한 대안적 연산자 클래스입니다. k-d 트리를 사용합니다.

quad_point_ops와의 차이점:
- 균형 잡힌 트리 구조로 더 나은 균형 제공
- 특정 데이터 분포에서 더 효율적일 수 있음
- 두 클래스 모두 k-NN(k-Nearest Neighbor) 검색 지원

```sql
-- quad_point_ops 사용 (기본값)
CREATE INDEX idx_points_quad ON geo_table USING spgist (location);

-- kd_point_ops 명시적 사용
CREATE INDEX idx_points_kd ON geo_table USING spgist (location kd_point_ops);
```

#### range_ops (범위 연산자 클래스)

모든 범위 타입에 대한 SP-GiST 인덱스입니다.

지원 연산자:
- `=`: 같음
- `&&`: 겹침
- `@>`: 포함
- `<@`: 포함됨
- `<<`: 왼쪽에 엄격히 위치
- `>>`: 오른쪽에 엄격히 위치
- `&<`: 오른쪽으로 확장되지 않음
- `&>`: 왼쪽으로 확장되지 않음
- `-|-`: 인접 (adjacent to)

#### text_ops (텍스트 연산자 클래스)

`text` 타입에 대한 SP-GiST 인덱스입니다. 기수 트리(Trie)를 사용합니다.

지원 연산자:
- `=`: 같음
- `<`, `<=`, `>`, `>=`: 비교 연산자
- `~<~`, `~<=~`, `~>=~`, `~>~`: 로케일 비의존적 비교
- `^@`: 접두사 검색 (starts with)

### 사용 예제

```sql
-- 포인트 데이터 인덱싱
CREATE TABLE locations (
    id serial PRIMARY KEY,
    name text,
    position point
);

-- SP-GiST 인덱스 생성
CREATE INDEX idx_location_position ON locations USING spgist (position);

-- k-NN 검색: 가장 가까운 5개 위치 찾기
SELECT name, position, position <-> point(3.5, 2.7) AS distance
FROM locations
ORDER BY position <-> point(3.5, 2.7)
LIMIT 5;

-- 영역 내 점 검색
SELECT name, position
FROM locations
WHERE position <@ box(point(0,0), point(10,10));
```

```sql
-- IP 주소 인덱싱
CREATE TABLE network_hosts (
    id serial PRIMARY KEY,
    hostname text,
    ip_address inet
);

CREATE INDEX idx_ip ON network_hosts USING spgist (ip_address inet_ops);

-- 특정 서브넷 내 호스트 찾기
SELECT hostname, ip_address
FROM network_hosts
WHERE ip_address << '192.168.1.0/24';
```

```sql
-- 텍스트 접두사 검색
CREATE TABLE products (
    id serial PRIMARY KEY,
    name text,
    description text
);

CREATE INDEX idx_product_name ON products USING spgist (name text_ops);

-- 접두사 검색
SELECT name FROM products WHERE name ^@ 'App';

-- 이는 LIKE 'App%' 쿼리보다 효율적입니다
SELECT name FROM products WHERE name LIKE 'App%';
```

---

## 69.3. 확장성 (Extensibility)

SP-GiST는 다양한 유형의 비균형 디스크 기반 데이터 구조를 구현할 수 있는 확장 가능한 인터페이스를 제공합니다. SP-GiST는 분할 규칙과 동등성에 대한 높은 수준의 추상화를 제공합니다. 또한 데이터를 내부 튜플 간에 이동하거나 값 압축과 같은 일반적인 작업은 SP-GiST 코어에서 처리합니다.

### 필수 사용자 정의 메서드

모든 SP-GiST 연산자 클래스는 5개의 필수 메서드와 1개의 선택적 메서드를 구현해야 합니다:

#### 1. config 메서드

인덱스 구현에 대한 정적 정보를 반환합니다.

```c
/* 입력 구조체 */
typedef struct spgConfigIn
{
    Oid         attType;        /* 인덱싱된 컬럼의 데이터 타입 */
} spgConfigIn;

/* 출력 구조체 */
typedef struct spgConfigOut
{
    Oid         prefixType;     /* 내부 튜플 접두사의 데이터 타입 */
    Oid         labelType;      /* 내부 튜플 노드 레이블의 데이터 타입 */
    Oid         leafType;       /* 리프 튜플 값의 데이터 타입 */
    bool        canReturnData;  /* 원본 데이터 재구성 가능 여부 */
    bool        longValuesOK;   /* 1페이지 초과 값 처리 가능 여부 */
} spgConfigOut;
```

설명:
- `prefixType`: 내부 튜플에 저장되는 접두사의 데이터 타입 (필수 아님)
- `labelType`: 노드 레이블의 데이터 타입 (필수 아님)
- `leafType`: 리프 튜플에 저장되는 값의 데이터 타입 (기본값: `attType`)
- `canReturnData`: true이면 인덱스 전용 스캔 지원
- `longValuesOK`: true이면 긴 값에 대한 접두사 제거 지원

#### 2. choose 메서드

새 값을 내부 튜플에 삽입할 방법을 결정합니다.

```c
/* 입력 구조체 */
typedef struct spgChooseIn
{
    Datum       datum;          /* 삽입할 원본 값 */
    Datum       leafDatum;      /* 리프 튜플에 저장될 현재 값 */
    int         level;          /* 트리에서 현재 레벨 */

    /* 내부 튜플 정보 */
    bool        allTheSame;     /* 모든 노드가 동등한지 */
    bool        hasPrefix;      /* 접두사 있음? */
    Datum       prefixDatum;    /* 접두사 값 */
    int         nNodes;         /* 노드 수 */
    Datum      *nodeLabels;     /* 노드 레이블 배열 (NULL 가능) */
} spgChooseIn;

/* 결과 유형 */
typedef enum spgChooseResultType
{
    spgMatchNode = 1,           /* 기존 노드로 하강 */
    spgAddNode,                 /* 내부 튜플에 노드 추가 */
    spgSplitTuple               /* 내부 튜플 분할 (접두사 변경) */
} spgChooseResultType;

/* 출력 구조체 */
typedef struct spgChooseOut
{
    spgChooseResultType resultType;

    union
    {
        struct                  /* spgMatchNode 결과 */
        {
            int         nodeN;          /* 하강할 노드 인덱스 */
            int         levelAdd;       /* 레벨 증가량 */
            Datum       restDatum;      /* 하위 레벨에 전달할 값 */
        } matchNode;

        struct                  /* spgAddNode 결과 */
        {
            Datum       nodeLabel;      /* 새 노드의 레이블 */
            int         nodeN;          /* 새 노드 삽입 위치 */
        } addNode;

        struct                  /* spgSplitTuple 결과 */
        {
            bool        prefixHasPrefix;    /* 새 상위 튜플에 접두사? */
            Datum       prefixPrefixDatum;  /* 새 접두사 */
            int         prefixNNodes;       /* 상위 튜플 노드 수 */
            Datum      *prefixNodeLabels;   /* 상위 튜플 노드 레이블 */
            int         childNodeN;         /* 하위 튜플 연결 노드 */

            bool        postfixHasPrefix;   /* 새 하위 튜플에 접두사? */
            Datum       postfixPrefixDatum; /* 하위 튜플 접두사 */
        } splitTuple;
    } result;
} spgChooseOut;
```

결과 유형 설명:
- spgMatchNode: 기존 노드를 따라 하강. `nodeN`으로 노드 지정
- spgAddNode: 새 자식 노드 추가. `nodeLabel`과 위치 지정
- spgSplitTuple: 내부 튜플 분할. 더 짧은 접두사로 새 상위 튜플 생성

#### 3. picksplit 메서드

리프 튜플 집합에서 새 내부 튜플을 생성하는 방법을 결정합니다.

```c
/* 입력 구조체 */
typedef struct spgPickSplitIn
{
    int         nTuples;        /* 분할할 리프 튜플 수 */
    Datum      *datums;         /* 리프 튜플 값 배열 */
    int         level;          /* 트리에서 현재 레벨 */
} spgPickSplitIn;

/* 출력 구조체 */
typedef struct spgPickSplitOut
{
    bool        hasPrefix;          /* 새 내부 튜플에 접두사? */
    Datum       prefixDatum;        /* 접두사 값 (hasPrefix = true인 경우) */

    int         nNodes;             /* 생성할 노드 수 */
    Datum      *nodeLabels;         /* 노드 레이블 배열 (NULL 가능) */

    int        *mapTuplesToNodes;   /* 각 리프 튜플의 노드 할당 */
    Datum      *leafTupleDatums;    /* 각 새 리프 튜플의 값 */
} spgPickSplitOut;
```

중요 요구사항:
- 여러 리프 튜플을 2개 이상의 노드로 분류 해야 함
- 모든 튜플이 단일 노드에 할당되면 SP-GiST 코어가 결과를 무시함

#### 4. inner_consistent 메서드

트리 검색 시 탐색할 노드(분기)를 반환합니다.

```c
/* 입력 구조체 */
typedef struct spgInnerConsistentIn
{
    ScanKey     scankeys;           /* 스캔 키 배열 */
    ScanKey     orderbys;           /* ORDER BY 키 배열 (k-NN 검색) */
    int         nkeys;              /* 스캔 키 수 */
    int         norderbys;          /* ORDER BY 키 수 */

    Datum       reconstructedValue; /* 재구성된 상위 값 */
    void       *traversalValue;     /* 연산자 클래스 전용 순회 값 */
    MemoryContext traversalMemoryContext;
    int         level;              /* 트리에서 현재 레벨 */
    bool        returnData;         /* 원본 데이터 반환 필요? */

    /* 내부 튜플 정보 */
    bool        allTheSame;         /* 모든 노드가 동등? */
    bool        hasPrefix;          /* 접두사 있음? */
    Datum       prefixDatum;        /* 접두사 값 */
    int         nNodes;             /* 노드 수 */
    Datum      *nodeLabels;         /* 노드 레이블 배열 */
} spgInnerConsistentIn;

/* 출력 구조체 */
typedef struct spgInnerConsistentOut
{
    int         nNodes;                 /* 방문할 노드 수 */
    int        *nodeNumbers;            /* 방문할 노드 인덱스 */
    int        *levelAdds;              /* 각 노드별 레벨 증가량 */
    Datum      *reconstructedValues;    /* 재구성된 값 (NULL 가능) */
    void      traversalValues;        /* 연산자 클래스 전용 순회 값 */
    double    distances;              /* k-NN 검색용 거리 (NULL 가능) */
} spgInnerConsistentOut;
```

#### 5. leaf_consistent 메서드

리프 튜플이 쿼리를 만족하는지 확인합니다.

```c
/* 입력 구조체 */
typedef struct spgLeafConsistentIn
{
    ScanKey     scankeys;           /* 스캔 키 배열 */
    ScanKey     orderbys;           /* ORDER BY 키 배열 */
    int         nkeys;              /* 스캔 키 수 */
    int         norderbys;          /* ORDER BY 키 수 */

    Datum       reconstructedValue; /* 재구성된 값 */
    void       *traversalValue;     /* 순회 값 */
    int         level;              /* 트리에서 현재 레벨 */
    bool        returnData;         /* 원본 데이터 반환 필요? */

    Datum       leafDatum;          /* 리프 튜플 값 */
} spgLeafConsistentIn;

/* 출력 구조체 */
typedef struct spgLeafConsistentOut
{
    Datum       leafValue;          /* 재구성된 원본 데이터 */
    bool        recheck;            /* 연산자 재확인 필요? */
    bool        recheckDistances;   /* 거리 재확인 필요? */
    double     *distances;          /* k-NN 검색용 거리 */
} spgLeafConsistentOut;
```

### 선택적 메서드

#### compress 메서드

데이터 항목을 리프 튜플 저장에 적합한 형식으로 변환합니다.

```c
Datum compress(Datum in)
```

특징:
- 입력: `spgConfigIn.attType` 타입
- 출력: `spgConfigOut.leafType` 타입
- 출력에 외부 라인 TOAST 포인터 포함 불가

#### options 메서드

연산자 클래스 동작을 제어하는 사용자 정의 매개변수를 정의합니다.

```sql
CREATE OR REPLACE FUNCTION my_spgist_options(internal)
RETURNS void
AS 'MODULE_PATHNAME'
LANGUAGE C STRICT;
```

### 연산자 클래스 구현 예제

다음은 간단한 정수 범위에 대한 SP-GiST 연산자 클래스 개념을 보여주는 예제입니다:

```sql
-- 연산자 클래스 생성 예제 (개념적)
CREATE OPERATOR CLASS my_int_ops
    FOR TYPE int4 USING spgist AS
    OPERATOR    1   =  (int4, int4),
    OPERATOR    2   <  (int4, int4),
    OPERATOR    3   <= (int4, int4),
    OPERATOR    4   >= (int4, int4),
    OPERATOR    5   >  (int4, int4),
    FUNCTION    1   my_config(internal, internal),
    FUNCTION    2   my_choose(internal, internal),
    FUNCTION    3   my_picksplit(internal, internal),
    FUNCTION    4   my_inner_consistent(internal, internal),
    FUNCTION    5   my_leaf_consistent(internal, internal);
```

---

## 69.4. 구현 (Implementation)

이 섹션에서는 SP-GiST 인덱스의 구현 세부 사항을 설명합니다.

### SP-GiST 제한 사항

#### 페이지 크기 제약

1. 개별 리프 및 내부 튜플 크기: 단일 인덱스 페이지에 맞아야 함 (기본값 8kB)
2. 긴 값 처리: `longValuesOK = true`인 경우에만 1페이지 초과 값 지원 가능
   - 연속적인 접두사 제거(suffix stripping)를 통해 구현
   - 예: 기수 트리에서 긴 문자열을 접두사와 나머지로 분할

#### 리프 그룹화 제약

하나의 노드가 가리키는 모든 리프 튜플은 동일한 인덱스 페이지에 있어야 합니다. 이는 SP-GiST 코어가 자동으로 처리합니다.

#### 무한 루프 방지

SP-GiST는 리프 데이텀이 10회의 `choose` 호출 내에서 축소되지 않으면 오류를 발생시킵니다.

### 노드 레이블 없는 SP-GiST

일부 트리 알고리즘은 고정된 노드 집합을 사용합니다. 예를 들어, 쿼드 트리는 항상 정확히 4개의 노드를 가집니다:

```
         NW | NE
        ----+----
         SW | SE
```

이 경우:
- `picksplit`에서 `nodeLabels`를 `NULL`로 반환 가능
- `choose`에서 `prefixNodeLabels`를 `NULL`로 반환 가능
- 공간 절약 및 코드 단순화 효과
- 주의: `choose`는 레이블 없는 튜플에 대해 `spgAddNode`를 반환하면 안 됨

### "All-the-Same" 내부 튜플

`picksplit`이 리프 값을 여러 카테고리로 분류하지 못할 경우:

1. SP-GiST 코어가 동등한 여러 노드를 가진 내부 튜플 생성
2. `allTheSame` 플래그가 true로 설정됨
3. `choose`가 `spgMatchNode`를 반환하면 어떤 동등 노드든 하강 가능
4. `inner_consistent`는 모든 노드 또는 아무 노드도 반환해야 함

이 메커니즘은:
- 무한 재귀 방지
- 트리의 균형 유지
- 데이터 분포가 분할 규칙과 잘 맞지 않는 경우 처리

```
allTheSame = true 인 내부 튜플 예시:
┌──────────────────────────────────────┐
│     내부 튜플 (allTheSame = true)    │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐    │
│  │노드1│ │노드2│ │노드3│ │노드4│    │
│  │  ≡  │ │  ≡  │ │  ≡  │ │  ≡  │    │
│  └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘    │
└─────┼──────┼──────┼──────┼─────────┘
      │      │      │      │
      ▼      ▼      ▼      ▼
    리프   리프   리프   리프
    튜플   튜플   튜플   튜플
```

### NULL 처리

SP-GiST 코어는 NULL 항목을 자동으로 처리합니다:

- NULL 값은 인덱스에 저장되지만 연산자 클래스 코드에서는 숨겨짐
- 연산자 클래스 메서드에 NULL 인덱스 항목이나 검색 조건이 전달되지 않음
- 모든 연산자는 strict로 가정됨

```sql
-- NULL 값도 인덱스에 저장됨
CREATE TABLE test_spgist (
    id serial PRIMARY KEY,
    location point
);

INSERT INTO test_spgist (location) VALUES
    (point(1,2)),
    (NULL),         -- NULL 값도 저장됨
    (point(3,4));

CREATE INDEX idx_test_location ON test_spgist USING spgist (location);

-- IS NULL 조건도 인덱스 사용 가능
SELECT * FROM test_spgist WHERE location IS NULL;
```

### 메모리 관리

SP-GiST 지원 메서드는 단기 메모리 컨텍스트에서 호출됩니다:

- `CurrentMemoryContext`는 각 튜플 후 재설정됨
- 예외: `config` 메서드는 메모리 누수를 피해야 함
- 출력 구조체는 메서드 호출 전 0으로 초기화됨

```c
/* config 메서드 예시 - 메모리 누수 주의 */
Datum
my_config(PG_FUNCTION_ARGS)
{
    spgConfigOut *cfg = (spgConfigOut *) PG_GETARG_POINTER(1);

    cfg->prefixType = INT4OID;
    cfg->labelType = INT4OID;
    cfg->leafType = INT4OID;
    cfg->canReturnData = true;
    cfg->longValuesOK = false;

    PG_RETURN_VOID();
}
```

### 예제 구현 위치

PostgreSQL 소스 배포판에는 다음 위치에 예제 구현이 포함되어 있습니다:

- `src/backend/access/spgist/`: SP-GiST 코어 코드
  - `spgutils.c`: 유틸리티 함수
  - `spginsert.c`: 삽입 로직
  - `spgscan.c`: 스캔 로직
  - `spgvacuum.c`: VACUUM 로직

- `src/backend/utils/adt/`: 내장 연산자 클래스 구현
  - `geo_spgist.c`: 기하 타입 (point, box, polygon)
  - `rangetypes_spgist.c`: 범위 타입
  - `network_spgist.c`: 네트워크 타입 (inet, cidr)

---

## 성능 고려 사항

### SP-GiST vs. GiST vs. B-tree

| 특성 | SP-GiST | GiST | B-tree |
|-----|---------|------|--------|
| 데이터 구조 | 분할 트리 | 균형 트리 | 균형 트리 |
| 적합한 데이터 | 공간, 네트워크, 텍스트 | 복잡한 데이터 타입 | 스칼라 값 |
| k-NN 지원 | 예 (일부 타입) | 예 | 아니오 |
| 접두사 검색 | 매우 효율적 | 보통 | 효율적 |
| 메모리 사용 | 낮음 | 중간 | 낮음 |

### 최적 사용 사례

1. 점 데이터의 k-NN 검색
```sql
CREATE INDEX idx_point ON locations USING spgist (position);

SELECT * FROM locations
ORDER BY position <-> point(10, 20)
LIMIT 10;
```

2. IP 주소 서브넷 검색
```sql
CREATE INDEX idx_ip ON hosts USING spgist (ip_address inet_ops);

SELECT * FROM hosts WHERE ip_address <<= '192.168.0.0/16';
```

3. 텍스트 접두사 검색
```sql
CREATE INDEX idx_name ON products USING spgist (name text_ops);

SELECT * FROM products WHERE name ^@ 'App';
```

### 인덱스 생성 시 고려사항

```sql
-- INCLUDE를 사용한 커버링 인덱스
CREATE INDEX idx_location_covering ON locations
    USING spgist (position)
    INCLUDE (name, category);

-- FILLFACTOR 설정 (업데이트가 빈번한 경우)
CREATE INDEX idx_location_fill ON locations
    USING spgist (position)
    WITH (fillfactor = 80);
```

---

## 요약

SP-GiST는 공간 분할 기반의 유연한 인덱스 구조를 제공합니다:

1. 다양한 트리 구조 지원: 쿼드 트리, k-d 트리, 기수 트리 등
2. 효율적인 공간 검색: 점, 상자, 다각형 등의 기하 데이터
3. 네트워크 데이터 지원: IP 주소 및 서브넷 검색
4. 텍스트 접두사 검색: 기수 트리 기반의 빠른 접두사 매칭
5. k-NN 검색 지원: 가장 가까운 이웃 검색

SP-GiST는 GiST와 달리 비균형 트리 구조를 지원하여 특정 유형의 데이터에 대해 더 효율적인 검색이 가능합니다. 데이터 특성에 따라 적절한 인덱스 유형을 선택하는 것이 중요합니다.
