# Chapter 11. 인덱스 (Indexes)

PostgreSQL 18 공식 문서 번역

---

## 목차

- [11.1. 소개](#111-소개-introduction)
- [11.2. 인덱스 유형](#112-인덱스-유형-index-types)
- [11.3. 다중 컬럼 인덱스](#113-다중-컬럼-인덱스-multicolumn-indexes)
- [11.4. 인덱스와 ORDER BY](#114-인덱스와-order-by)
- [11.5. 다중 인덱스 결합](#115-다중-인덱스-결합-combining-multiple-indexes)
- [11.6. 고유 인덱스](#116-고유-인덱스-unique-indexes)
- [11.7. 표현식 인덱스](#117-표현식-인덱스-indexes-on-expressions)
- [11.8. 부분 인덱스](#118-부분-인덱스-partial-indexes)
- [11.9. 인덱스 전용 스캔과 커버링 인덱스](#119-인덱스-전용-스캔과-커버링-인덱스-index-only-scans-and-covering-indexes)
- [11.10. 연산자 클래스와 연산자 패밀리](#1110-연산자-클래스와-연산자-패밀리-operator-classes-and-operator-families)
- [11.11. 인덱스와 콜레이션](#1111-인덱스와-콜레이션-indexes-and-collations)
- [11.12. 인덱스 사용 검사](#1112-인덱스-사용-검사-examining-index-usage)

---

## 11.1. 소개 (Introduction)

인덱스는 데이터베이스 성능을 향상시키는 일반적인 방법입니다. 인덱스를 사용하면 데이터베이스 서버가 인덱스 없이 수행할 수 있는 것보다 훨씬 빠르게 특정 행을 찾고 검색할 수 있습니다. 그러나 인덱스는 데이터베이스 시스템에 오버헤드를 추가하므로 현명하게 사용해야 합니다.

### 기본 예제

다음 테이블이 있다고 가정해봅시다:

```sql
CREATE TABLE test1 (
    id integer,
    content varchar
);
```

그리고 애플리케이션에서 다음과 같은 형태의 쿼리를 많이 실행한다고 가정합니다:

```sql
SELECT content FROM test1 WHERE id = constant;
```

사전 준비 없이는 시스템이 일치하는 항목을 찾기 위해 전체 `test1` 테이블을 행 단위로 스캔해야 합니다. `test1`에 많은 행이 있고 이 쿼리가 소수의 행(아마도 0개 또는 1개)만 반환하는 경우, 이는 분명히 비효율적인 방법입니다. 그러나 시스템이 `id` 컬럼에 인덱스를 유지하도록 지시받은 경우, 일치하는 행을 찾기 위해 훨씬 효율적인 방법을 사용할 수 있습니다. 예를 들어, 트리에서 몇 레벨만 내려가면 될 수 있습니다.

### 인덱스 생성

`id` 컬럼에 인덱스를 생성하려면 다음 명령을 사용합니다:

```sql
CREATE INDEX test1_id_index ON test1 (id);
```

인덱스 이름 `test1_id_index`는 자유롭게 선택할 수 있지만, 인덱스의 용도를 기억할 수 있는 이름을 선택하는 것이 좋습니다.

### 인덱스 삭제

인덱스를 삭제하려면 `DROP INDEX` 명령을 사용합니다:

```sql
DROP INDEX test1_id_index;
```

인덱스는 테이블이 생성된 후 언제든지 추가하고 제거할 수 있습니다.

### 인덱스 사용 시점

인덱스가 생성되면 추가적인 개입이 필요하지 않습니다: 시스템은 테이블이 수정될 때 인덱스를 업데이트하고, 순차 테이블 스캔보다 효율적이라고 생각되면 쿼리에서 인덱스를 사용합니다. 그러나 쿼리 플래너가 합리적인 결정을 내리려면 `ANALYZE` 명령을 정기적으로 실행해야 할 수 있습니다. 통계 정보를 수집하는 방법에 대한 정보는 [Section 14.2](https://www.postgresql.org/docs/18/planner-stats.html)와 [Section 24.1.3](https://www.postgresql.org/docs/18/routine-vacuuming.html#VACUUM-FOR-STATISTICS)을 참조하세요.

```sql
ANALYZE;
```

### 인덱스 적용 범위

인덱스는 다음에서 사용될 수 있습니다:

- `WHERE` 절의 검색 조건
- `JOIN` 절의 조인 조건
- `UPDATE`, `DELETE` 명령어의 검색 조건

### 인덱스 가능한 쿼리 형태

쿼리 조건이 인덱스를 사용하려면 일반적으로 다음 형태여야 합니다:

```
인덱싱된-컬럼 인덱스가능한-연산자 비교값
```

여기서:
- 인덱싱된-컬럼: 인덱스가 정의된 컬럼 또는 표현식
- 인덱스가능한-연산자: 인덱스의 연산자 클래스에 속하는 연산자
- 비교값: 휘발성이 아닌 표현식(volatile function을 포함하지 않는)

원본 연산자에 교환 연산자(commutator)가 있는 경우, 다음과 같은 형태도 자동으로 변환되어 사용될 수 있습니다:

```
비교값 연산자 인덱싱된-컬럼
```

### 인덱스 생성 시 주의사항

대규모 테이블 인덱싱

인덱스 생성 중에는:
- 기본 동작: `SELECT` 문은 병렬 실행 가능하지만, `INSERT`, `UPDATE`, `DELETE`는 차단됩니다
- 동시 쓰기 허용: 인덱스를 동시에 생성하여 쓰기 작업을 차단하지 않을 수 있습니다. 자세한 내용은 [Building Indexes Concurrently](https://www.postgresql.org/docs/18/sql-createindex.html#SQL-CREATEINDEX-CONCURRENTLY)를 참조하세요.

유지보수 오버헤드

- 데이터가 변경되면 인덱스도 자동으로 동기화됩니다
- 인덱스가 많으면 Heap-only tuples (HOT) 생성이 방해될 수 있습니다
- 권장사항: 사용되지 않는 인덱스는 제거하세요

---

## 11.2. 인덱스 유형 (Index Types)

PostgreSQL은 여러 인덱스 유형을 제공합니다: B-tree, Hash, GiST, SP-GiST, GIN, BRIN. 각 인덱스 유형은 서로 다른 유형의 쿼리에 가장 적합한 다른 알고리즘을 사용합니다. 기본적으로 `CREATE INDEX` 명령은 가장 일반적인 상황에 적합한 B-tree 인덱스를 생성합니다.

### 기본 문법

```sql
CREATE INDEX name ON table USING index_type (column);
```

### 11.2.1. B-Tree

B-tree 인덱스는 정렬 가능한 데이터에 대한 동등성 및 범위 쿼리를 처리할 수 있습니다. 특히, PostgreSQL 쿼리 플래너는 인덱싱된 컬럼이 다음 연산자 중 하나를 사용하는 비교에 관련될 때마다 B-tree 인덱스 사용을 고려합니다:

지원 연산자

```
<     <=     =     >=     >
```

이러한 연산자의 조합과 동등한 구성, 예를 들어 `BETWEEN`과 `IN`도 B-tree 인덱스 검색으로 구현될 수 있습니다. 또한 인덱스 컬럼에 대한 `IS NULL` 또는 `IS NOT NULL` 조건도 B-tree 인덱스와 함께 사용될 수 있습니다.

패턴 매칭

옵티마이저는 `LIKE`와 `~`(정규식)과 같은 패턴 매칭 연산자에 대해서도 B-tree 인덱스를 사용할 수 있습니다. 단, 패턴이 상수이고 패턴의 시작 부분에 고정되어 있어야 합니다. 예를 들어:

```sql
-- 인덱스 사용 가능
col LIKE 'foo%'
col ~ '^foo'

-- 인덱스 사용 불가
col LIKE '%bar'
```

그러나 데이터베이스가 C 로케일을 사용하지 않는 경우, 패턴 매칭 쿼리의 인덱싱을 지원하려면 특수 연산자 클래스로 인덱스를 생성해야 합니다. [Section 11.10](#1110-연산자-클래스와-연산자-패밀리-operator-classes-and-operator-families)을 참조하세요.

대소문자 구분 없는 버전인 `ILIKE`와 `~*`의 경우, 패턴이 알파벳이 아닌 문자로 시작하는 경우에만 B-tree 인덱스를 사용할 수 있습니다. 알파벳이 아닌 문자는 대소문자 변환의 영향을 받지 않기 때문입니다.

B-tree 인덱스는 정렬된 순서로 데이터를 검색하는 데에도 사용될 수 있습니다. 이것이 항상 단순 스캔과 정렬보다 빠른 것은 아니지만, 종종 도움이 됩니다.

예제

```sql
CREATE INDEX idx_btree ON table_name USING BTREE (column_name);
```

### 11.2.2. Hash

Hash 인덱스는 32비트 해시 코드를 저장하므로 단순 동등 비교만 처리할 수 있습니다.

지원 연산자

```
=
```

예제

```sql
CREATE INDEX idx_hash ON table_name USING HASH (column_name);
```

제한사항

Hash 인덱스는 동등(`=`) 비교만 처리할 수 있으며, 범위 쿼리나 패턴 매칭은 처리할 수 없습니다.

### 11.2.3. GiST

GiST 인덱스는 단일 종류의 인덱스가 아니라 많은 다른 인덱싱 전략이 구현될 수 있는 인프라입니다. 따라서 GiST 인덱스와 함께 사용할 수 있는 특정 연산자는 인덱싱 전략(연산자 클래스)에 따라 다릅니다. 예를 들어, PostgreSQL의 표준 배포판에는 여러 2차원 기하학적 데이터 유형에 대한 GiST 연산자 클래스가 포함되어 있습니다.

2차원 기하학 데이터 지원 연산자

```
<<    &<    &>    >>    <<|    &<|    |&>    |>>    @>    <@    ~=    &&
```

자세한 내용은 [Table 65.1](https://www.postgresql.org/docs/18/gist-builtin-opclasses.html#GIST-BUILTIN-OPCLASSES-TABLE)을 참조하세요.

GiST 인덱스는 "최근접 이웃" 검색을 최적화할 수 있습니다:

```sql
SELECT * FROM places ORDER BY location <-> point '(101,456)' LIMIT 10;
```

이 쿼리는 주어진 목표점에 가장 가까운 10개의 장소를 찾습니다. `location` 컬럼에 GiST 인덱스가 있으면 빠르게 수행됩니다:

```sql
CREATE INDEX idx_gist ON places USING GIST (location);
```

많은 다른 GiST 연산자 클래스가 contrib 컬렉션에서 사용 가능하거나 별도의 프로젝트로 사용 가능합니다. 자세한 정보는 [Chapter 65](https://www.postgresql.org/docs/18/gist.html)를 참조하세요.

### 11.2.4. SP-GiST

SP-GiST 인덱스는 GiST 인덱스와 마찬가지로 다양한 종류의 검색을 지원하는 인프라를 제공합니다. SP-GiST는 쿼드트리, k-d 트리, 기수 트리(tries)와 같은 광범위한 비균형 디스크 기반 데이터 구조의 구현을 허용합니다. 예를 들어, PostgreSQL의 표준 배포판에는 2차원 포인트에 대한 SP-GiST 연산자 클래스가 포함되어 있습니다.

2차원 포인트 지원 연산자

```
<<    >>    ~=    <@    <<|    |>>
```

자세한 내용은 [Table 65.2](https://www.postgresql.org/docs/18/spgist-builtin-opclasses.html#SPGIST-BUILTIN-OPCLASSES-TABLE)를 참조하세요.

GiST와 마찬가지로 SP-GiST는 "최근접 이웃" 검색을 지원합니다. 거리 정렬을 지원하는 연산자 클래스의 경우, 해당 연산자를 `ORDER BY` 절에서 사용하여 최근접 이웃 검색을 수행할 수 있습니다.

```sql
CREATE INDEX idx_spgist ON points USING SPGIST (location);
```

자세한 정보는 [Chapter 66](https://www.postgresql.org/docs/18/spgist.html)을 참조하세요.

### 11.2.5. GIN

GIN 인덱스는 인덱싱되는 값이 단일 키 값이 아닌 여러 컴포넌트 값(예: 배열)을 포함하는 경우를 처리하도록 설계된 "역 인덱스"입니다. 이러한 인덱스는 각 컴포넌트 값에 대해 별도의 항목을 저장합니다. 인덱스는 특정 컴포넌트 값의 존재 여부를 테스트하는 쿼리를 효율적으로 처리할 수 있습니다.

배열 지원 연산자

```
<@    @>    =    &&
```

GIN은 전체 텍스트 검색 및 JSONB 데이터에도 널리 사용됩니다.

예제

```sql
CREATE INDEX idx_gin ON table_name USING GIN (array_column);
```

GiST 및 SP-GiST와 마찬가지로 GIN은 많은 다른 사용자 정의 인덱싱 전략을 지원할 수 있으며, GIN 인덱스와 함께 사용할 수 있는 특정 연산자는 인덱싱 전략에 따라 다릅니다. 자세한 정보는 [Chapter 67](https://www.postgresql.org/docs/18/gin.html)을 참조하세요.

### 11.2.6. BRIN

BRIN 인덱스(Block Range INdex의 약자)는 테이블의 연속적인 물리적 블록 범위에 저장된 값에 대한 요약을 저장합니다. 따라서 값이 테이블의 물리적 순서와 잘 상관관계가 있는 컬럼에 가장 효과적입니다. GiST, SP-GiST, GIN과 마찬가지로 BRIN은 많은 다른 인덱싱 전략을 지원할 수 있으며, BRIN 인덱스와 함께 사용할 수 있는 특정 연산자는 인덱싱 전략에 따라 다릅니다.

선형 정렬 순서를 가진 데이터 유형의 경우, 인덱싱된 데이터는 각 블록 범위 내의 컬럼 값의 최소값과 최대값에 해당합니다.

지원 연산자

```
<    <=    =    >=    >
```

예제

```sql
CREATE INDEX idx_brin ON large_table USING BRIN (column_name);
```

특징
- 연속적인 블록 범위에 대한 최소/최대 값을 저장합니다
- 데이터가 물리적으로 정렬되어 있을 때 가장 효과적입니다
- B-tree 인덱스보다 훨씬 작은 크기입니다

자세한 정보는 [Chapter 68](https://www.postgresql.org/docs/18/brin.html)을 참조하세요.

### 인덱스 유형 비교 요약

| 인덱스 유형 | 최적 용도 | 지원 연산자 | 저장 공간 |
|-----------|---------|-----------|---------|
| B-tree | 범용, 범위 검색, 정렬 | `<, <=, =, >=, >, BETWEEN, IN, LIKE` | 중간 |
| Hash | 동등 비교만 | `=` | 중간 |
| GiST | 기하학적, 공간적, 최근접 이웃 검색 | 연산자 클래스에 따라 다름 | 큼 |
| SP-GiST | 비균형 구조, 공간적 데이터 | `<<, >>, ~=, <@, \|>>` | 중간 |
| GIN | 다중 컴포넌트 값, 배열, 전체 텍스트 검색 | `<@, @>, =, &&` | 가변적 |
| BRIN | 대규모, 자연 정렬된 테이블 | `<, <=, =, >=, >` | 매우 작음 |

---

## 11.3. 다중 컬럼 인덱스 (Multicolumn Indexes)

인덱스는 테이블의 둘 이상의 컬럼에 대해 정의될 수 있습니다.

### 기본 예제

다음 테이블이 있다고 가정합니다:

```sql
CREATE TABLE test2 (
    major int,
    minor int,
    name varchar
);
```

그리고 다음과 같은 쿼리를 자주 실행한다고 가정합니다:

```sql
SELECT name FROM test2 WHERE major = constant AND minor = constant;
```

`major`와 `minor` 컬럼 모두에 대해 인덱스를 정의하는 것이 적절할 수 있습니다:

```sql
CREATE INDEX test2_mm_idx ON test2 (major, minor);
```

### 지원하는 인덱스 유형

현재 B-tree, GiST, GIN, BRIN 인덱스 유형만 다중 컬럼 인덱스를 지원합니다. `INCLUDE` 컬럼을 포함하여 최대 32개 컬럼 을 지정할 수 있습니다. (이 제한은 PostgreSQL을 빌드할 때 변경할 수 있습니다; `pg_config_manual.h` 파일 참조)

### B-tree 다중 컬럼 인덱스

다중 컬럼 B-tree 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있지만, 선행(왼쪽) 컬럼에 제약 조건이 있을 때 가장 효율적 입니다.

정확한 규칙은 다음과 같습니다:
- 등호 제약 조건이 있는 선행 컬럼 + 첫 번째 비등호 제약 조건이 있는 컬럼이 인덱스 스캔 범위를 제한합니다
- 이러한 컬럼 오른쪽에 있는 컬럼에 대한 제약 조건은 인덱스에서 확인되므로 적절한 테이블 방문이 절약됩니다

예를 들어, `(a, b, c)`에 대한 인덱스와 `WHERE a = 5 AND b >= 42 AND c < 77` 쿼리 조건이 주어지면:
- `a = 5`인 첫 번째 항목부터 `a = 5`인 마지막 항목까지 인덱스를 스캔합니다
- `b >= 42` 및 `c < 77` 조건에 맞지 않는 인덱스 항목은 건너뜁니다

#### Skip Scan 최적화

B-tree는 Skip Scan 최적화 를 지원하여 선행 컬럼이 쿼리에서 제약되지 않은 경우에도 인덱스를 사용할 수 있습니다.

예를 들어, `(x, y)` 인덱스에서 `WHERE y = 7700` 쿼리:
- 플래너가 모든 가능한 `N` 값에 대해 `WHERE x = N AND y = 7700`을 검색하는 것이 더 빠르다고 판단하면 적용됩니다
- `x` 값이 적을 때 효과적입니다

이러한 이유로, 인덱스의 컬럼 순서가 중요합니다. 컬럼 순서를 효과적으로 지정하면 단일 다중 컬럼 인덱스로 여러 쿼리 유형을 지원할 수 있습니다.

### GiST 다중 컬럼 인덱스

다중 컬럼 GiST 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. 추가 컬럼에 대한 조건은 인덱스에서 반환되는 항목을 제한하지만, 첫 번째 컬럼에 대한 조건이 스캔해야 하는 인덱스의 양을 결정하는 데 가장 중요합니다. 첫 번째 컬럼의 고유 값이 적은 GiST 인덱스는 고유 값이 많은 인덱스보다 상대적으로 비효율적입니다. 동일한 값이 많은 첫 번째 컬럼을 가진 인덱스는 비효율적입니다.

### GIN 다중 컬럼 인덱스

다중 컬럼 GIN 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. B-tree 또는 GiST와 달리, 인덱스 검색 효율성은 쿼리 조건이 어떤 인덱스 컬럼을 사용하는지에 관계없이 동일 합니다.

### BRIN 다중 컬럼 인덱스

다중 컬럼 BRIN 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. GIN과 마찬가지로, 인덱스 검색 효율성은 쿼리 조건이 어떤 인덱스 컬럼을 사용하는지에 관계없이 동일 합니다. 동일한 테이블에 단일 다중 컬럼 BRIN 인덱스 대신 여러 BRIN 인덱스를 사용하는 유일한 이유는 다른 `pages_per_range` 저장 파라미터를 갖기 위함입니다.

### 권장사항

다중 컬럼 인덱스는 절약적으로 사용해야 합니다. 대부분의 경우 단일 컬럼 인덱스로 충분하며 공간과 시간을 절약합니다. 3개 이상의 컬럼 을 가진 인덱스는 테이블 사용이 극도로 특화된 경우가 아니면 거의 도움이 되지 않습니다. 여러 인덱스 구성의 상대적 이점에 대한 논의는 [Section 11.5](#115-다중-인덱스-결합-combining-multiple-indexes)와 [Section 11.9](#119-인덱스-전용-스캔과-커버링-인덱스-index-only-scans-and-covering-indexes)를 참조하세요.

---

## 11.4. 인덱스와 ORDER BY

B-tree 인덱스 스캔은 정렬된 순서로 출력을 생성하므로, `ORDER BY` 사양을 만족하기 위해 인덱스를 사용할 수 있습니다. 이것이 항상 가장 빠른 옵션은 아닙니다. 물리적 순서로 테이블을 스캔한 다음 명시적으로 정렬하는 것이 대부분의 테이블을 검색하는 쿼리에서는 더 빠를 수 있습니다. PostgreSQL 인덱스 유형 중 B-tree만 정렬된 출력을 생성 할 수 있습니다. 다른 인덱스 유형은 일치하는 행을 지정되지 않은 구현 종속 순서로 반환합니다.

### 인덱스 스캔 vs 명시적 정렬

쿼리 플래너는 다음 두 가지 방식을 고려합니다:

1. 인덱스 스캔: `ORDER BY` 사양과 일치하는 인덱스 사용
2. 테이블 물리적 순서 스캔 + 명시적 정렬: 대규모 데이터 처리 시 더 빠를 수 있음

플래너는 두 가지 방법의 비용을 비교하고 더 빠른 것을 선택합니다.

인덱스가 특히 유용한 경우:

- 소수의 행만 가져올 때
- `ORDER BY`와 `LIMIT n`이 결합된 경우: 인덱스가 있으면 처음 n개 행만 직접 검색할 수 있습니다

### B-tree 인덱스의 정렬 순서

기본적으로 B-tree 인덱스는 오름차순, NULL은 마지막 순서로 저장됩니다.

- 정방향 스캔: `ORDER BY x` (= `ORDER BY x ASC NULLS LAST`)
- 역방향 스캔: `ORDER BY x DESC` (= `ORDER BY x DESC NULLS FIRST`)

### 인덱스 생성 시 정렬 옵션 지정

인덱스 생성 시 `ASC`, `DESC`, `NULLS FIRST`, `NULLS LAST` 옵션을 지정할 수 있습니다:

```sql
CREATE INDEX test2_info_nulls_low ON test2 (info NULLS FIRST);
CREATE INDEX test3_desc_index ON test3 (id DESC NULLS LAST);
```

규칙: 오름차순 + NULLS FIRST로 저장된 인덱스는 정방향/역방향 스캔으로 두 가지 `ORDER BY`를 모두 만족할 수 있습니다:
- `ORDER BY x ASC NULLS FIRST` (정방향)
- `ORDER BY x DESC NULLS LAST` (역방향)

### 다중 컬럼 인덱스에서의 활용

단일 컬럼에서는 기본 옵션과 그 반대로 충분하지만, 다중 컬럼 인덱스에서는 맞춤형 정렬 옵션이 매우 유용 합니다.

예제 분석: 2컬럼 인덱스 `(x, y)`

| 인덱스 정의 | 만족하는 ORDER BY |
|-----------|-----------------|
| 기본 `(x, y)` | `ORDER BY x, y` (정방향) |
| 기본 `(x, y)` | `ORDER BY x DESC, y DESC` (역방향) |
| `(x ASC, y DESC)` | `ORDER BY x ASC, y DESC` (정방향) |
| `(x ASC, y DESC)` | `ORDER BY x DESC, y ASC` (역방향) |

실제 필요한 경우:

애플리케이션에서 `ORDER BY x ASC, y DESC`가 자주 필요하다면:

```sql
CREATE INDEX idx_custom ON table_name (x ASC, y DESC);
```

또는 동등하게:

```sql
CREATE INDEX idx_custom ON table_name (x DESC, y ASC);
```

두 번째 인덱스는 역방향으로 스캔하여 동일한 결과를 얻을 수 있습니다.

### 결론

맞춤형 정렬 순서를 가진 인덱스는 특정 쿼리에서 상당한 성능 향상 을 제공할 수 있지만, 인덱스 유지 비용이 발생하므로 사용 빈도를 고려하여 적용해야 합니다.

---

## 11.5. 다중 인덱스 결합 (Combining Multiple Indexes)

### 단일 인덱스 스캔의 제한

단일 인덱스 스캔은 해당 인덱스의 컬럼을 사용하고 `AND`로 결합된 쿼리 절만 사용할 수 있습니다. 예를 들어, `(a, b)` 인덱스가 주어지면 `WHERE a = 5 AND b = 6` 같은 쿼리 조건은 인덱스를 사용할 수 있지만, `WHERE a = 5 OR b = 6` 같은 조건은 인덱스를 직접 사용할 수 없습니다.

예시:

```sql
-- 가능: (a, b) 인덱스 사용
WHERE a = 5 AND b = 6

-- 불가능: 직접 인덱스 사용 불가
WHERE a = 5 OR b = 6
```

### 비트맵 스캔을 통한 다중 인덱스 결합

다행히도 PostgreSQL은 여러 인덱스(다중 컬럼 인덱스 포함)를 결합하여 단일 인덱스 스캔으로 구현할 수 없는 경우를 처리할 수 있습니다. 시스템은 여러 인덱스 스캔에서 비트맵을 생성하고 조건에 필요한 대로 비트맵을 `AND`하거나 `OR`할 수 있습니다. 그런 다음 실제 테이블 행을 방문합니다.

동작 원리:

1. 각 인덱스마다 메모리에 비트맵(bitmap) 생성
2. 비트맵은 해당 인덱스 조건과 일치하는 테이블 행의 위치 저장
3. 비트맵들을 `AND`/`OR` 연산으로 결합
4. 실제 테이블 행을 물리적 순서로 방문

예시 쿼리:

```sql
-- 별도 인덱스 결합
WHERE x = 5 AND y = 6
-- x 인덱스 + y 인덱스 스캔 후 결과를 AND로 결합

-- 4개의 별도 스캔으로 분해
WHERE x = 42 OR x = 47 OR x = 53 OR x = 99
```

### 주요 특성

| 특성 | 설명 |
|------|------|
| 행 방문 순서 | 물리적 순서 (정렬 순서 상실) |
| ORDER BY 영향 | 별도의 정렬 단계 필요 |
| 성능 | 인덱스 스캔 추가 시 오버헤드 발생 |

### 인덱스 전략 결정 가이드

쿼리가 열 `x`만, 열 `y`만, 또는 둘 다 포함하는 경우의 인덱스 전략:

옵션 1: 두 개의 별도 인덱스

```sql
CREATE INDEX idx_x ON table(x);
CREATE INDEX idx_y ON table(y);
```

- 장점: 단일 열 쿼리에 효율적, 인덱스 결합으로 양 열 쿼리 처리
- 용도: 혼합된 쿼리 패턴에 적합

옵션 2: 복합 인덱스

```sql
CREATE INDEX idx_xy ON table(x, y);
```

- 장점: 양 열 쿼리에 더 효율적
- 단점: `y`만 검색하는 경우 효율 감소 (B-tree skip scan 최적화에 따라 달라짐)

옵션 3: 복합 + 추가 인덱스

```sql
CREATE INDEX idx_xy ON table(x, y);
CREATE INDEX idx_y ON table(y);
```

- 모든 쿼리 패턴에 좋은 성능

옵션 4: 모든 인덱스

```sql
CREATE INDEX idx_x ON table(x);
CREATE INDEX idx_y ON table(y);
CREATE INDEX idx_xy ON table(x, y);
```

- 용도: 읽기 빈도가 쓰기보다 훨씬 많고 모든 쿼리 유형이 공통적일 때만

### 의사 결정 원칙

- 자주 사용되지 않는 쿼리 유형: 가장 흔한 유형 2개를 처리하는 인덱스만 생성
- 균형 잡힌 패턴: 별도 인덱스 + 인덱스 결합 활용
- 높은 업데이트 빈도: 인덱스 수 최소화

이 접근 방식은 쿼리 성능과 유지보수 오버헤드 간의 균형을 맞추는 데 도움이 됩니다.

---

## 11.6. 고유 인덱스 (Unique Indexes)

인덱스는 컬럼의 값이나 여러 컬럼의 결합된 값의 유일성을 강제 하는 데에도 사용될 수 있습니다.

### 문법

```sql
CREATE UNIQUE INDEX name ON table (column [, ...]) [ NULLS [ NOT ] DISTINCT ];
```

### 지원 인덱스 유형

현재 B-tree 인덱스만 고유로 선언할 수 있습니다.

### NULL 값 처리

- 기본 동작: 고유 컬럼의 NULL 값들은 서로 같지 않은 것으로 간주되어 여러 NULL 값 허용
- NULLS NOT DISTINCT: NULL 값들을 같은 것으로 취급하여 하나의 NULL만 허용

```sql
-- 여러 NULL 허용 (기본)
CREATE UNIQUE INDEX idx_unique ON table (column);

-- 하나의 NULL만 허용
CREATE UNIQUE INDEX idx_unique ON table (column) NULLS NOT DISTINCT;
```

### 다중 컬럼 고유 인덱스

다중 컬럼 고유 인덱스의 경우, 모든 인덱싱된 컬럼이 여러 행에서 동일한 경우에만 거부됩니다.

### 자동 생성

PostgreSQL은 다음 경우 자동으로 고유 인덱스를 생성 합니다:

- UNIQUE 제약 조건 정의 시
- PRIMARY KEY 정의 시

자동 생성된 인덱스는 제약 조건을 강제하는 메커니즘으로 작동합니다.

### 주의사항

> 참고: 테이블에 고유 제약 조건이나 기본 키로 정의된 컬럼에 대해 수동으로 인덱스를 생성할 필요가 없습니다. 그렇게 하면 자동으로 생성된 인덱스와 중복될 뿐입니다.

---

## 11.7. 표현식 인덱스 (Indexes on Expressions)

인덱스 컬럼은 단순한 테이블 컬럼이 아니라 테이블의 하나 이상의 컬럼에서 계산된 함수나 스칼라 표현식 이 될 수 있습니다. 이 기능은 계산 결과를 기반으로 테이블에 빠르게 접근하는 데 유용합니다.

### 예제 1: 대소문자 구분 없는 비교

애플리케이션에서 대소문자 구분 없는 검색을 자주 수행하는 경우:

```sql
SELECT * FROM test1 WHERE lower(col1) = 'value';
```

이 쿼리가 `lower(col1)` 값에 대해 정의된 인덱스를 사용하도록 할 수 있습니다:

```sql
CREATE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

### 예제 2: 여러 컬럼 연결 표현식

```sql
SELECT * FROM people WHERE (first_name || ' ' || last_name) = 'John Smith';
```

이 쿼리를 위한 인덱스:

```sql
CREATE INDEX people_names ON people ((first_name || ' ' || last_name));
```

### 문법 규칙

- 복합 표현식: 괄호 필수

```sql
CREATE INDEX people_names ON people ((first_name || ' ' || last_name));
```

- 함수 호출: 괄호 생략 가능

```sql
CREATE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

### 성능 특성

| 측면 | 특징 |
|------|------|
| 유지보수 비용 | 높음 - 행 삽입/업데이트 시 표현식을 재계산 |
| 검색 성능 | 빠름 - 표현식이 이미 인덱스에 저장되어 있음 |
| 최적 사용 시기 | 검색 속도가 삽입/업데이트 속도보다 중요할 때 |

### 인덱스 사용 조건

인덱스 표현식이 인덱스 정의와 일치해야 합니다. 예를 들어, `lower(col1)` 인덱스는 다음 쿼리에서 사용됩니다:

```sql
-- 인덱스 사용
SELECT * FROM test1 WHERE lower(col1) = 'value';

-- 인덱스 사용 안 함 (col1과 lower(col1)은 다름)
SELECT * FROM test1 WHERE col1 = 'VALUE';
```

### 고급 활용: 표현식에 대한 UNIQUE 제약 조건

표현식 인덱스를 `UNIQUE`로 선언하면, 단순 UNIQUE 제약 조건으로 정의할 수 없는 제약 조건 을 강제할 수 있습니다.

예: 대소문자만 다른 `col1` 값의 중복 생성을 방지

```sql
CREATE UNIQUE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

이 인덱스는 'ABC', 'abc', 'Abc' 등의 값 중 하나만 허용합니다.

---

## 11.8. 부분 인덱스 (Partial Indexes)

부분 인덱스(Partial Index) 는 테이블의 부분집합에 대해 구축된 인덱스입니다. 부분집합은 조건식(predicate) 이라고 하는 조건 표현식으로 정의됩니다. 인덱스는 조건을 만족하는 테이블 행에 대한 항목만 포함합니다.

부분 인덱스는 다소 특수한 기능이지만, 여러 상황에서 유용합니다. 부분 인덱스가 사용되는 주요 이유는 인덱스에서 쿼리 워크로드의 관심 대상이 아닌 공통 값을 제외하여 인덱스 크기를 줄이는 것입니다. 이렇게 하면 인덱스를 사용하는 쿼리가 더 빨라지고, 많은 인덱스 업데이트 작업도 더 빨라집니다.

### 예제 11.1: 공통값 제외하기

웹 서버 로그를 저장하는 테이블이 있다고 가정합니다. 대부분의 요청은 조직의 IP 범위에서 오지만, 외부 IP 요청을 검색해야 합니다:

```sql
CREATE TABLE access_log (
    url varchar,
    client_ip inet,
    ...
);

-- 조직 내부 IP 범위를 제외한 부분 인덱스 생성
CREATE INDEX access_log_client_ip_ix ON access_log (client_ip)
WHERE NOT (client_ip > inet '192.168.100.0' AND
           client_ip < inet '192.168.100.255');
```

사용 가능한 쿼리:

```sql
SELECT *
FROM access_log
WHERE url = '/index.html' AND client_ip = inet '212.78.10.32';
```

사용 불가능한 쿼리:

```sql
SELECT *
FROM access_log
WHERE url = '/index.html' AND client_ip = inet '192.168.100.23';
```

이 인덱스가 더 작기 때문에 외부 IP 검색 쿼리에서 더 빠른 성능을 제공합니다.

### 예제 11.2: 관심 없는 값 제외하기

미청구 주문만 자주 검색하는 경우:

```sql
CREATE INDEX orders_unbilled_index ON orders (order_nr)
    WHERE billed is not true;
```

사용 가능한 쿼리들:

```sql
-- order_nr 포함
SELECT * FROM orders
WHERE billed is not true AND order_nr < 10000;

-- order_nr 미포함 (덜 효율적이지만 사용 가능)
SELECT * FROM orders
WHERE billed is not true AND amount > 5000.00;
```

사용 불가능한 쿼리:

```sql
SELECT * FROM orders WHERE order_nr = 3501;
-- 3501이 청구된 것인지 미청구인지 불확실하므로 사용 불가
```

이 인덱스는 작업 테이블처럼 테이블의 일부에 많은 업데이트가 집중되고 그 부분이 상대적으로 작은 테이블 설정에서도 사용될 수 있습니다.

### 예제 11.3: 부분 고유 인덱스

테이블의 부분집합에만 유니크 제약을 적용합니다:

```sql
CREATE TABLE tests (
    subject text,
    target text,
    success boolean,
    ...
);

-- 성공한 테스트만 unique 제약
CREATE UNIQUE INDEX tests_success_constraint ON tests (subject, target)
    WHERE success;
```

이것은 특정 주제와 대상 조합에 대해 하나의 성공 항목만 있을 수 있지만, 실패 항목은 여러 개 있을 수 있을 때 특히 효율적인 접근 방식입니다.

### 중요한 제한사항: Predicate 매칭 규칙

- 쿼리의 `WHERE` 조건이 인덱스의 predicate를 수학적으로 함축 해야 합니다
- PostgreSQL은 단순한 부등식 함축만 인식합니다 (예: "x < 1" implies "x < 2")
- 정확한 매칭이 필요합니다 (쿼리 계획 시간에 검사)
- 파라미터화된 쿼리는 작동하지 않을 수 있습니다 (예: prepared statement with "x < ?")

### 예제 11.4: 안티패턴 - 분할의 대체로 사용하지 말 것

하지 말아야 할 방법:

```sql
CREATE INDEX mytable_cat_1 ON mytable (data) WHERE category = 1;
CREATE INDEX mytable_cat_2 ON mytable (data) WHERE category = 2;
CREATE INDEX mytable_cat_3 ON mytable (data) WHERE category = 3;
...
CREATE INDEX mytable_cat_N ON mytable (data) WHERE category = N;
```

권장하는 방법:

```sql
CREATE INDEX mytable_cat_data ON mytable (category, data);
```

또는 테이블이 매우 큰 경우 테이블 분할 을 사용합니다. ([Section 5.12](https://www.postgresql.org/docs/18/ddl-partitioning.html) 참고)

### 부분 인덱스의 장점

1. 인덱스 크기 감소: 메모리 효율성 증가
2. 쿼리 성능 향상: 인덱스 사용 쿼리에서
3. 업데이트 성능 향상: 인덱스 갱신 범위 감소
4. 선택적 유니크 제약 적용 가능

### 주의사항

- 공통값이 미리 결정 되어야 합니다
- 데이터 분포가 변하면 인덱스 재생성이 필요할 수 있습니다
- 쿼리 플래너보다 나은 지식이 있을 때만 사용을 권장합니다
- 대부분의 경우 일반 인덱스와의 성능 차이가 미미할 수 있습니다

---

## 11.9. 인덱스 전용 스캔과 커버링 인덱스 (Index-Only Scans and Covering Indexes)

PostgreSQL의 모든 인덱스는 보조 인덱스(secondary index) 입니다. 즉, 각 인덱스는 테이블의 주 데이터 영역(힙)과 별도로 저장됩니다. 이는 일반 인덱스 스캔에서 각 행 검색이 인덱스와 힙 모두에서 데이터를 가져와야 함을 의미합니다. 또한, 주어진 인덱스 가능 `WHERE` 조건과 일치하는 힙 항목은 힙의 어디에나 있을 수 있으므로 힙 부분의 접근은 많은 랜덤 접근을 수반합니다. 이는 특히 기계식 디스크에서 느릴 수 있습니다.

### 인덱스 전용 스캔 (Index-Only Scan)

이 성능 문제를 해결하기 위해 PostgreSQL은 힙 접근 없이 쿼리에 응답하는 인덱스 전용 스캔(index-only scan) 을 지원합니다.

### 인덱스 전용 스캔 가능 조건

#### 1. 인덱스 타입 지원

- B-tree: 항상 지원
- GiST, SP-GiST: 일부 연산자 클래스에서만 지원
- GIN: 지원 불가 (원본 데이터의 일부만 저장)

필수 요건: 인덱스가 원본 데이터 값을 물리적으로 저장하거나 재구성 가능해야 합니다.

#### 2. 쿼리가 인덱스에 저장된 컬럼만 참조

가능한 쿼리 예시 (`(x, y)` 인덱스):

```sql
SELECT x, y FROM tab WHERE x = 'key';
SELECT x FROM tab WHERE x = 'key' AND y < 42;
```

불가능한 쿼리 예시:

```sql
SELECT x, z FROM tab WHERE x = 'key';           -- z가 인덱스에 없음
SELECT x FROM tab WHERE x = 'key' AND z < 42;   -- z가 인덱스에 없음
```

#### 3. MVCC 가시성 확인

PostgreSQL은 가시성 맵(Visibility Map) 을 사용하여 이 문제를 해결합니다:

- PostgreSQL은 각 힙 페이지에서 모든 행이 모든 활성 및 미래 트랜잭션에 가시적인지 추적하는 비트를 유지합니다
- 인덱스 전용 스캔은 가시성 맵 비트를 먼저 확인합니다
  - 비트 설정됨: 행은 즉시 반환 (힙 접근 불필요)
  - 비트 미설정: 힙 접근 필수 (가시성 확인)
- 장점: 가시성 맵은 힙보다 훨씬 작아 물리 I/O가 훨씬 적습니다
- 효율성: 변경이 적은 테이블에서 매우 유용합니다

### 커버링 인덱스 (Covering Index)

인덱스 전용 스캔을 최대한 활용하려면, 커버링 인덱스 를 생성하여 특정 쿼리 유형에서 인덱스에 "페이로드"로 포함될 추가 컬럼을 지정할 수 있습니다.

#### 기본 문법

```sql
CREATE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

#### 실제 예시

자주 실행되는 쿼리:

```sql
SELECT y FROM tab WHERE x = 'key';
```

인덱스 전용 스캔이 가능한 커버링 인덱스:

```sql
CREATE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

#### 커버링 인덱스의 특징

비-키 컬럼(Payload Column)의 성질:

- 인덱스의 검색 키 부분이 아닙니다
- 데이터 타입 제약이 없습니다 (인덱스가 처리하지 않음)
- 인덱스에 저장되기만 합니다

### UNIQUE 제약과의 조합

```sql
CREATE UNIQUE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

- 유니크 제약은 x에만 적용 됩니다 (x와 y의 조합이 아님)
- PRIMARY KEY 제약에도 적용 가능합니다:

```sql
CREATE TABLE tab (
    x INT PRIMARY KEY INCLUDE (y),
    y INT
);
```

### 성능 고려사항

주의사항:

1. 인덱스 크기 증가
   - 비-키 컬럼은 데이터 중복을 발생시킵니다
   - 넓은 컬럼 추가 시 인덱스 팽창 가능 (검색 속도 저하 가능)
   - 인덱스 항목의 최대 크기를 초과하면 삽입 실패

2. 효율성 조건
   - 테이블이 충분히 느리게 변경 되어야 합니다
   - 변경이 빠르면 가시성 맵 비트가 설정되지 않음 (힙 접근 필수)
   - 이 경우 페이로드 컬럼의 이점이 없습니다

3. 지원 인덱스 타입
   - B-tree, GiST, SP-GiST만 INCLUDE 지원
   - 표현식(Expression)은 포함 컬럼으로 지원되지 않습니다

### 기존 방식과의 비교

예전 방식 (INCLUDE 미지원 시):

```sql
-- 페이로드 컬럼을 일반 컬럼으로 추가
CREATE INDEX tab_x_y ON tab(x, y);
```

문제점:
- 뒤쪽 컬럼만 가능 (앞쪽은 검색 성능 저하)
- UNIQUE 제약 적용 불가능

현재 방식 (권장):

```sql
CREATE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

장점:
- 명확한 의도 표현
- UNIQUE/PRIMARY KEY 지원
- 상위 B-tree 레벨에서 비-키 컬럼이 확실히 제외됩니다

### 서픽스 절단 (Suffix Truncation)

- 비-키 컬럼은 B-tree 상위 레벨에서 항상 제거 됩니다
- 인덱스 스캔을 위해 사용되지 않습니다
- 명시적 INCLUDE 사용 시 상위 레벨의 튜플 크기가 안정적으로 축소 됩니다

### 표현식 인덱스와의 상호작용

현재 플래너의 한계:

```sql
-- 인덱스 정의
CREATE INDEX tab_f_x ON tab (f(x));

-- 이 쿼리는 인덱스 전용 스캔 불가능
SELECT f(x) FROM tab WHERE f(x) < 1;
```

이유: 플래너가 x 컬럼 가용성을 감지하지 못합니다.

해결 방법:

```sql
CREATE INDEX tab_f_x ON tab (f(x)) INCLUDE (x);

-- 이제 인덱스 전용 스캔 가능
SELECT f(x) FROM tab WHERE f(x) < 1;
```

### 부분 인덱스와의 상호작용

```sql
-- 부분 유니크 인덱스
CREATE UNIQUE INDEX tests_success_constraint ON tests (subject, target)
    WHERE success;

-- 이 쿼리는 인덱스 전용 스캔 가능 (PostgreSQL 9.6+)
SELECT target FROM tests
WHERE subject = 'some-subject' AND success;
```

작동 원리:
- WHERE 절의 `success` 컬럼은 인덱스에 저장되지 않습니다
- 하지만 인덱스에 있는 모든 항목은 `success = true` 보장
- 런타임 재확인 불필요 (인덱스 전용 스캔 가능)

---

## 11.10. 연산자 클래스와 연산자 패밀리 (Operator Classes and Operator Families)

인덱스 정의는 선택적으로 각 인덱스 컬럼에 대한 연산자 클래스(operator class) 를 지정할 수 있습니다.

### 기본 문법

```sql
CREATE INDEX name ON table (column opclass [ ( opclass_options ) ] [sort options] [, ...]);
```

### 연산자 클래스의 역할

연산자 클래스는 해당 컬럼에 대해 인덱스가 사용할 연산자를 식별합니다. 예를 들어, `int4` 타입에 대한 B-tree 인덱스는 4바이트 정수를 비교하기 위한 함수를 제공하는 `int4_ops` 클래스를 사용합니다. 실제로 컬럼의 데이터 타입에 대한 기본 연산자 클래스가 일반적으로 충분합니다.

### 연산자 클래스가 필요한 경우

동일한 데이터 타입에 대해 여러 의미 있는 인덱싱 동작이 있을 때 연산자 클래스를 명시적으로 지정해야 합니다. 예를 들어:

- 복소수의 절대값 기준 정렬 vs 실수부 기준 정렬
- 연산자 클래스가 기본 정렬 순서를 결정합니다
- `COLLATE`, `ASC`/`DESC`, `NULLS FIRST`/`NULLS LAST` 등으로 수정 가능합니다

### 내장 연산자 클래스: 패턴 매칭용

B-tree 연산자 클래스 중에서 `text_pattern_ops`, `varchar_pattern_ops`, `bpchar_pattern_ops`는 각각 `text`, `varchar`, `char` 타입에 대한 특수한 패턴 매칭 연산자 클래스입니다.

```sql
CREATE INDEX test_index ON test_table (col varchar_pattern_ops);
```

특징:

- 문자 단위의 엄격한 비교 (로케일별 콜레이션 규칙 무시)
- `LIKE` 또는 POSIX 정규식 패턴 매칭에 적합
- C 로케일이 아닌 경우 권장

주의:

- 일반 `<`, `<=`, `>`, `>=` 비교는 기본 연산자 클래스 인덱스 필요
- 동등성(`=`) 비교는 `xxx_pattern_ops` 사용 가능

### 정의된 연산자 클래스 조회

#### 1. 모든 연산자 클래스 조회

```sql
SELECT am.amname AS index_method,
       opc.opcname AS opclass_name,
       opc.opcintype::regtype AS indexed_type,
       opc.opcdefault AS is_default
    FROM pg_am am, pg_opclass opc
    WHERE opc.opcmethod = am.oid
    ORDER BY index_method, opclass_name;
```

#### 2. 연산자 패밀리와 함께 조회

```sql
SELECT am.amname AS index_method,
       opc.opcname AS opclass_name,
       opf.opfname AS opfamily_name,
       opc.opcintype::regtype AS indexed_type,
       opc.opcdefault AS is_default
    FROM pg_am am, pg_opclass opc, pg_opfamily opf
    WHERE opc.opcmethod = am.oid AND
          opc.opcfamily = opf.oid
    ORDER BY index_method, opclass_name;
```

### 연산자 패밀리 (Operator Families)

연산자 클래스는 실제로 연산자 패밀리(operator family) 라는 더 큰 구조의 일부입니다. 일반적인 경우 연산자 클래스에는 단일 데이터 타입에 대해 작동하는 연산자만 포함되지만, 인덱스에서 서로 다른 데이터 타입의 비교를 허용하는 것이 유용한 경우가 있습니다. 이는 교차 데이터 타입 연산자 로, 패밀리의 멤버이지만 특정 클래스와 연결되지 않습니다.

#### 3. 모든 연산자 패밀리와 포함된 연산자 조회

```sql
SELECT am.amname AS index_method,
       opf.opfname AS opfamily_name,
       amop.amopopr::regoperator AS opfamily_operator
    FROM pg_am am, pg_opfamily opf, pg_amop amop
    WHERE opf.opfmethod = am.oid AND
          amop.amopfamily = opf.oid
    ORDER BY index_method, opfamily_name, opfamily_operator;
```

### psql 유용한 명령어

```
\dAc  - 연산자 클래스 정보 (더 정교한 버전)
\dAf  - 연산자 패밀리 정보
\dAo  - 연산자 패밀리에 포함된 연산자 정보
```

### 실무적 팁

1. 일반적으로: 기본 연산자 클래스가 대부분의 경우 충분합니다
2. 패턴 매칭: C 로케일이 아닌 경우 `xxx_pattern_ops` 사용
3. 여러 인덱스: 같은 열에 다른 연산자 클래스를 가진 여러 인덱스 생성 가능
4. C 로케일 사용: `xxx_pattern_ops`가 불필요 (기본 클래스로 충분)

---

## 11.11. 인덱스와 콜레이션 (Indexes and Collations)

인덱스는 인덱스 컬럼당 하나의 콜레이션만 지원할 수 있습니다. 여러 콜레이션이 필요한 경우, 여러 인덱스를 생성해야 합니다.

### 기본 개념

인덱스는 자동으로 기본 컬럼의 콜레이션을 사용합니다. 다른 콜레이션으로 쿼리를 실행하면 해당 인덱스를 활용할 수 없습니다.

### 예제

테이블 생성:

```sql
CREATE TABLE test1c (
    id integer,
    content varchar COLLATE "x"
);
```

기본 인덱스 생성:

```sql
CREATE INDEX test1c_content_index ON test1c (content);
```

이 인덱스는 컬럼의 기본 콜레이션("x")을 사용합니다.

인덱스 활용 가능한 쿼리:

```sql
SELECT * FROM test1c WHERE content > 'constant';
```

### 다른 콜레이션을 위한 추가 인덱스

다른 콜레이션("y")으로 쿼리하는 경우:

```sql
SELECT * FROM test1c WHERE content > 'constant' COLLATE "y";
```

위 쿼리를 지원하려면 별도의 인덱스를 생성해야 합니다:

```sql
CREATE INDEX test1c_content_y_index ON test1c (content COLLATE "y");
```

### 결론

다양한 콜레이션으로 쿼리할 필요가 있다면, 각 콜레이션별로 별도의 인덱스를 생성하여 쿼리 성능을 최적화할 수 있습니다.

---

## 11.12. 인덱스 사용 검사 (Examining Index Usage)

실제 쿼리 워크로드에서 인덱스가 사용되고 있는지 확인하는 것이 좋습니다. PostgreSQL에서 인덱스 사용을 검사하려면 실험 이 필요합니다.

### 핵심 개념

PostgreSQL의 인덱스는 유지보수가 필요하지 않지만, 실제 워크로드에서 인덱스가 실제로 사용되는지 확인하는 것이 중요합니다.

### 인덱스 사용 검사 방법

#### 1. EXPLAIN 명령 사용

개별 쿼리에 대해 인덱스가 사용되는지 확인합니다. 실제 실행 시간을 포함한 정보를 보려면 `EXPLAIN ANALYZE`를 사용합니다:

```sql
EXPLAIN ANALYZE SELECT * FROM test1 WHERE id = 100;
```

#### 2. 통계 수집

실행 중인 서버의 전체 인덱스 사용 통계를 수집합니다. [Section 27.2](https://www.postgresql.org/docs/18/monitoring-stats.html)를 참조하세요.

### 인덱스 최적화를 위한 팁

| 항목 | 설명 |
|------|------|
| ANALYZE 실행 | 쿼리 플래너의 정확한 비용 추정을 위해 필수 |
| 실제 데이터 사용 | 테스트 데이터로는 부정확한 결과 초래 |
| 인덱스 사용 강제 | `enable_seqscan`, `enable_nestloop` 파라미터로 테스트 |
| EXPLAIN ANALYZE | 실제 성능 측정 및 비교 |
| 비용 조정 | Section 19.7.2의 플래너 비용 상수 조정 |

### 인덱스 사용 강제 (테스트 목적)

플래너가 순차 스캔을 사용하지 않도록 강제하여 인덱스 스캔을 테스트할 수 있습니다:

```sql
SET enable_seqscan = off;
EXPLAIN ANALYZE SELECT * FROM test1 WHERE id = 100;
SET enable_seqscan = on;  -- 테스트 후 원복
```

이 방법은 플래너가 생각하는 비용과 실제 비용을 비교하는 데 유용합니다.

### 주의사항

- 매우 작은 테스트 데이터는 인덱스 필요성을 잘못 판단하게 합니다
- 통계가 없으면 인덱스 사용 분석은 무의미합니다
- 인덱스가 사용되지 않는 이유는 여러 가지가 있을 수 있습니다:
  - 테이블이 너무 작아서 순차 스캔이 더 빠름
  - 쿼리 조건이 인덱스와 맞지 않음
  - 통계가 오래되어 플래너가 잘못된 추정을 함

### 인덱스 사용 여부를 확인하는 이유

- 사용되지 않는 인덱스는 디스크 공간을 차지하고 업데이트 성능을 저하시킵니다
- 주기적으로 인덱스 사용을 검토하고 불필요한 인덱스를 제거하는 것이 좋습니다

---

## 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 11. Indexes](https://www.postgresql.org/docs/18/indexes.html)
- [CREATE INDEX 문서](https://www.postgresql.org/docs/18/sql-createindex.html)
- [GiST 인덱스](https://www.postgresql.org/docs/18/gist.html)
- [SP-GiST 인덱스](https://www.postgresql.org/docs/18/spgist.html)
- [GIN 인덱스](https://www.postgresql.org/docs/18/gin.html)
- [BRIN 인덱스](https://www.postgresql.org/docs/18/brin.html)
