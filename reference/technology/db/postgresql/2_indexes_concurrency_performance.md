# PostgreSQL 인덱스, 전문 검색, 동시성, 성능

## Chapter 11. 인덱스 (Indexes)

PostgreSQL 18 공식 문서 번역

---

### 목차

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

### 11.1. 소개 (Introduction)

인덱스는 데이터베이스 성능을 향상시키는 일반적인 방법입니다. 인덱스를 사용하면 데이터베이스 서버가 인덱스 없이 수행할 수 있는 것보다 훨씬 빠르게 특정 행을 찾고 검색할 수 있습니다. 그러나 인덱스는 데이터베이스 시스템에 오버헤드를 추가하므로 현명하게 사용해야 합니다.

#### 기본 예제

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

#### 인덱스 생성

`id` 컬럼에 인덱스를 생성하려면 다음 명령을 사용합니다:

```sql
CREATE INDEX test1_id_index ON test1 (id);
```

인덱스 이름 `test1_id_index`는 자유롭게 선택할 수 있지만, 인덱스의 용도를 기억할 수 있는 이름을 선택하는 것이 좋습니다.

#### 인덱스 삭제

인덱스를 삭제하려면 `DROP INDEX` 명령을 사용합니다:

```sql
DROP INDEX test1_id_index;
```

인덱스는 테이블이 생성된 후 언제든지 추가하고 제거할 수 있습니다.

#### 인덱스 사용 시점

인덱스가 생성되면 추가적인 개입이 필요하지 않습니다: 시스템은 테이블이 수정될 때 인덱스를 업데이트하고, 순차 테이블 스캔보다 효율적이라고 판단되면 쿼리에서 인덱스를 사용합니다. 그러나 쿼리 플래너가 합리적인 판단을 내리려면 `ANALYZE` 명령을 정기적으로 실행해야 할 수 있습니다. 통계 정보를 수집하는 방법에 대한 정보는 [Section 14.2](https://www.postgresql.org/docs/18/planner-stats.html)와 [Section 24.1.3](https://www.postgresql.org/docs/18/routine-vacuuming.html#VACUUM-FOR-STATISTICS)을 참조하세요.

```sql
ANALYZE;
```

#### 인덱스 적용 범위

인덱스는 다음에서 사용될 수 있습니다:

- `WHERE` 절의 검색 조건
- `JOIN` 절의 조인 조건
- `UPDATE`, `DELETE` 명령어의 검색 조건

#### 인덱스 가능한 쿼리 형태

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

#### 인덱스 생성 시 주의사항

대규모 테이블 인덱싱

인덱스 생성 중에는:
- 기본 동작: `SELECT` 문은 병렬 실행 가능하지만, `INSERT`, `UPDATE`, `DELETE`는 차단됩니다
- 동시 쓰기 허용: 인덱스를 동시에 생성하여 쓰기 작업을 차단하지 않을 수 있습니다. 자세한 내용은 [Building Indexes Concurrently](https://www.postgresql.org/docs/18/sql-createindex.html#SQL-CREATEINDEX-CONCURRENTLY)를 참조하세요.

유지보수 오버헤드

- 데이터가 변경되면 인덱스도 자동으로 동기화됩니다
- 인덱스가 많으면 Heap-only tuples (HOT) 생성이 방해될 수 있습니다
- 권장사항: 사용되지 않는 인덱스는 제거하세요

---

### 11.2. 인덱스 유형 (Index Types)

PostgreSQL은 여러 인덱스 유형을 제공합니다: B-tree, Hash, GiST, SP-GiST, GIN, BRIN. 각 인덱스 유형은 서로 다른 유형의 쿼리에 가장 적합한 다른 알고리즘을 사용합니다. 기본적으로 `CREATE INDEX` 명령은 가장 일반적인 상황에 적합한 B-tree 인덱스를 생성합니다.

#### 기본 문법

```sql
CREATE INDEX name ON table USING index_type (column);
```

#### 11.2.1. B-Tree

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

#### 11.2.2. Hash

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

#### 11.2.3. GiST

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

#### 11.2.4. SP-GiST

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

#### 11.2.5. GIN

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

#### 11.2.6. BRIN

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

#### 인덱스 유형 비교 요약

| 인덱스 유형 | 최적 용도 | 지원 연산자 | 저장 공간 |
|-----------|---------|-----------|---------|
| B-tree | 범용, 범위 검색, 정렬 | `<, <=, =, >=, >, BETWEEN, IN, LIKE` | 중간 |
| Hash | 동등 비교만 | `=` | 중간 |
| GiST | 기하학적, 공간적, 최근접 이웃 검색 | 연산자 클래스에 따라 다름 | 큼 |
| SP-GiST | 비균형 구조, 공간적 데이터 | `<<, >>, ~=, <@, \|>>` | 중간 |
| GIN | 다중 컴포넌트 값, 배열, 전체 텍스트 검색 | `<@, @>, =, &&` | 가변적 |
| BRIN | 대규모, 자연 정렬된 테이블 | `<, <=, =, >=, >` | 매우 작음 |

---

### 11.3. 다중 컬럼 인덱스 (Multicolumn Indexes)

인덱스는 테이블의 둘 이상의 컬럼에 대해 정의될 수 있습니다.

#### 기본 예제

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

#### 지원하는 인덱스 유형

현재 B-tree, GiST, GIN, BRIN 인덱스 유형만 다중 컬럼 인덱스를 지원합니다. `INCLUDE` 컬럼을 포함하여 최대 32개 컬럼 을 지정할 수 있습니다. (이 제한은 PostgreSQL을 빌드할 때 변경할 수 있습니다; `pg_config_manual.h` 파일 참조)

#### B-tree 다중 컬럼 인덱스

다중 컬럼 B-tree 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있지만, 선행(왼쪽) 컬럼에 제약 조건이 있을 때 가장 효율적 입니다.

정확한 규칙은 다음과 같습니다:
- 등호 제약 조건이 있는 선행 컬럼 + 첫 번째 비등호 제약 조건이 있는 컬럼이 인덱스 스캔 범위를 제한합니다
- 이러한 컬럼 오른쪽에 있는 컬럼에 대한 제약 조건은 인덱스에서 확인되므로 적절한 테이블 방문이 절약됩니다

예를 들어, `(a, b, c)`에 대한 인덱스와 `WHERE a = 5 AND b >= 42 AND c < 77` 쿼리 조건이 주어지면:
- `a = 5`인 첫 번째 항목부터 `a = 5`인 마지막 항목까지 인덱스를 스캔합니다
- `b >= 42` 및 `c < 77` 조건에 맞지 않는 인덱스 항목은 건너뜁니다

##### Skip Scan 최적화

B-tree는 Skip Scan 최적화 를 지원하여 선행 컬럼이 쿼리에서 제약되지 않은 경우에도 인덱스를 사용할 수 있습니다.

예를 들어, `(x, y)` 인덱스에서 `WHERE y = 7700` 쿼리:
- 플래너가 모든 가능한 `N` 값에 대해 `WHERE x = N AND y = 7700`을 검색하는 것이 더 빠르다고 판단하면 적용됩니다
- `x` 값이 적을 때 효과적입니다

이러한 이유로, 인덱스의 컬럼 순서가 중요합니다. 컬럼 순서를 효과적으로 지정하면 단일 다중 컬럼 인덱스로 여러 쿼리 유형을 지원할 수 있습니다.

#### GiST 다중 컬럼 인덱스

다중 컬럼 GiST 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. 추가 컬럼에 대한 조건은 인덱스에서 반환되는 항목을 제한하지만, 첫 번째 컬럼에 대한 조건이 스캔해야 하는 인덱스의 양을 결정하는 데 가장 중요합니다. 첫 번째 컬럼의 고유 값이 적은 GiST 인덱스는 고유 값이 많은 인덱스보다 상대적으로 비효율적입니다. 동일한 값이 많은 첫 번째 컬럼을 가진 인덱스는 비효율적입니다.

#### GIN 다중 컬럼 인덱스

다중 컬럼 GIN 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. B-tree 또는 GiST와 달리, 인덱스 검색 효율성은 쿼리 조건이 어떤 인덱스 컬럼을 사용하는지에 관계없이 동일 합니다.

#### BRIN 다중 컬럼 인덱스

다중 컬럼 BRIN 인덱스는 인덱스 컬럼의 모든 부분집합 과 관련된 쿼리 조건에서 사용될 수 있습니다. GIN과 마찬가지로, 인덱스 검색 효율성은 쿼리 조건이 어떤 인덱스 컬럼을 사용하는지에 관계없이 동일 합니다. 동일한 테이블에 단일 다중 컬럼 BRIN 인덱스 대신 여러 BRIN 인덱스를 사용하는 유일한 이유는 다른 `pages_per_range` 저장 파라미터를 갖기 위함입니다.

#### 권장사항

다중 컬럼 인덱스는 절약적으로 사용해야 합니다. 대부분의 경우 단일 컬럼 인덱스로 충분하며 공간과 시간을 절약합니다. 3개 이상의 컬럼 을 가진 인덱스는 테이블 사용이 극도로 특화된 경우가 아니면 거의 도움이 되지 않습니다. 여러 인덱스 구성의 상대적 이점에 대한 논의는 [Section 11.5](#115-다중-인덱스-결합-combining-multiple-indexes)와 [Section 11.9](#119-인덱스-전용-스캔과-커버링-인덱스-index-only-scans-and-covering-indexes)를 참조하세요.

---

### 11.4. 인덱스와 ORDER BY

B-tree 인덱스 스캔은 정렬된 순서로 출력을 생성하므로, `ORDER BY` 사양을 만족하기 위해 인덱스를 사용할 수 있습니다. 이것이 항상 가장 빠른 옵션은 아닙니다. 물리적 순서로 테이블을 스캔한 다음 명시적으로 정렬하는 것이 대부분의 테이블을 검색하는 쿼리에서는 더 빠를 수 있습니다. PostgreSQL 인덱스 유형 중 B-tree만 정렬된 출력을 생성 할 수 있습니다. 다른 인덱스 유형은 일치하는 행을 지정되지 않은 구현 종속 순서로 반환합니다.

#### 인덱스 스캔 vs 명시적 정렬

쿼리 플래너는 다음 두 가지 방식을 고려합니다:

1. 인덱스 스캔: `ORDER BY` 사양과 일치하는 인덱스 사용
2. 테이블 물리적 순서 스캔 + 명시적 정렬: 대규모 데이터 처리 시 더 빠를 수 있음

플래너는 두 가지 방법의 비용을 비교하고 더 빠른 것을 선택합니다.

인덱스가 특히 유용한 경우:

- 소수의 행만 가져올 때
- `ORDER BY`와 `LIMIT n`이 결합된 경우: 인덱스가 있으면 처음 n개 행만 직접 검색할 수 있습니다

#### B-tree 인덱스의 정렬 순서

기본적으로 B-tree 인덱스는 오름차순, NULL은 마지막 순서로 저장됩니다.

- 정방향 스캔: `ORDER BY x` (= `ORDER BY x ASC NULLS LAST`)
- 역방향 스캔: `ORDER BY x DESC` (= `ORDER BY x DESC NULLS FIRST`)

#### 인덱스 생성 시 정렬 옵션 지정

인덱스 생성 시 `ASC`, `DESC`, `NULLS FIRST`, `NULLS LAST` 옵션을 지정할 수 있습니다:

```sql
CREATE INDEX test2_info_nulls_low ON test2 (info NULLS FIRST);
CREATE INDEX test3_desc_index ON test3 (id DESC NULLS LAST);
```

규칙: 오름차순 + NULLS FIRST로 저장된 인덱스는 정방향/역방향 스캔으로 두 가지 `ORDER BY`를 모두 만족할 수 있습니다:
- `ORDER BY x ASC NULLS FIRST` (정방향)
- `ORDER BY x DESC NULLS LAST` (역방향)

#### 다중 컬럼 인덱스에서의 활용

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

맞춤형 정렬 순서를 가진 인덱스는 특정 쿼리에서 상당한 성능 향상을 제공할 수 있지만, 인덱스 유지 비용이 발생하므로 사용 빈도를 고려하여 적용해야 합니다.

---

### 11.5. 다중 인덱스 결합 (Combining Multiple Indexes)

#### 단일 인덱스 스캔의 제한

단일 인덱스 스캔은 해당 인덱스의 컬럼을 사용하고 `AND`로 결합된 쿼리 절만 사용할 수 있습니다. 예를 들어, `(a, b)` 인덱스가 주어지면 `WHERE a = 5 AND b = 6` 같은 쿼리 조건은 인덱스를 사용할 수 있지만, `WHERE a = 5 OR b = 6` 같은 조건은 인덱스를 직접 사용할 수 없습니다.

예시:

```sql
-- 가능: (a, b) 인덱스 사용
WHERE a = 5 AND b = 6

-- 불가능: 직접 인덱스 사용 불가
WHERE a = 5 OR b = 6
```

#### 비트맵 스캔을 통한 다중 인덱스 결합

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

#### 주요 특성

| 특성 | 설명 |
|------|------|
| 행 방문 순서 | 물리적 순서 (정렬 순서 상실) |
| ORDER BY 영향 | 별도의 정렬 단계 필요 |
| 성능 | 인덱스 스캔 추가 시 오버헤드 발생 |

#### 인덱스 전략 결정 가이드

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

#### 의사 결정 원칙

- 자주 사용되지 않는 쿼리 유형: 가장 흔한 유형 2개를 처리하는 인덱스만 생성
- 균형 잡힌 패턴: 별도 인덱스 + 인덱스 결합 활용
- 높은 업데이트 빈도: 인덱스 수 최소화

이 접근 방식은 쿼리 성능과 유지보수 오버헤드 사이의 균형을 맞추는 데 도움이 됩니다.

---

### 11.6. 고유 인덱스 (Unique Indexes)

인덱스는 컬럼 값 또는 여러 컬럼의 결합 값에 대한 유일성을 강제하는 데에도 사용할 수 있습니다.

#### 문법

```sql
CREATE UNIQUE INDEX name ON table (column [, ...]) [ NULLS [ NOT ] DISTINCT ];
```

#### 지원 인덱스 유형

현재 B-tree 인덱스만 고유로 선언할 수 있습니다.

#### NULL 값 처리

- 기본 동작: 고유 컬럼의 NULL 값들은 서로 같지 않은 것으로 간주되어 여러 NULL 값 허용
- NULLS NOT DISTINCT: NULL 값들을 같은 것으로 취급하여 하나의 NULL만 허용

```sql
-- 여러 NULL 허용 (기본)
CREATE UNIQUE INDEX idx_unique ON table (column);

-- 하나의 NULL만 허용
CREATE UNIQUE INDEX idx_unique ON table (column) NULLS NOT DISTINCT;
```

#### 다중 컬럼 고유 인덱스

다중 컬럼 고유 인덱스의 경우, 모든 인덱싱된 컬럼이 여러 행에서 동일한 경우에만 거부됩니다.

#### 자동 생성

PostgreSQL은 다음 경우 자동으로 고유 인덱스를 생성 합니다:

- UNIQUE 제약 조건 정의 시
- PRIMARY KEY 정의 시

자동 생성된 인덱스는 제약 조건을 강제하는 메커니즘으로 작동합니다.

#### 주의사항

> 참고: 테이블에 고유 제약 조건이나 기본 키로 정의된 컬럼에 대해 수동으로 인덱스를 생성할 필요가 없습니다. 그렇게 하면 자동으로 생성된 인덱스와 중복될 뿐입니다.

---

### 11.7. 표현식 인덱스 (Indexes on Expressions)

인덱스 컬럼은 단순한 테이블 컬럼이 아니라 테이블의 하나 이상의 컬럼에서 계산된 함수나 스칼라 표현식이 될 수 있습니다. 이 기능은 계산 결과를 기준으로 테이블에 빠르게 접근해야 할 때 유용합니다.

#### 예제 1: 대소문자 구분 없는 비교

애플리케이션에서 대소문자 구분 없는 검색을 자주 수행하는 경우:

```sql
SELECT * FROM test1 WHERE lower(col1) = 'value';
```

이 쿼리가 `lower(col1)` 값에 대해 정의된 인덱스를 사용하도록 할 수 있습니다:

```sql
CREATE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

#### 예제 2: 여러 컬럼 연결 표현식

```sql
SELECT * FROM people WHERE (first_name || ' ' || last_name) = 'John Smith';
```

이 쿼리를 위한 인덱스:

```sql
CREATE INDEX people_names ON people ((first_name || ' ' || last_name));
```

#### 문법 규칙

- 복합 표현식: 괄호 필수

```sql
CREATE INDEX people_names ON people ((first_name || ' ' || last_name));
```

- 함수 호출: 괄호 생략 가능

```sql
CREATE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

#### 성능 특성

| 측면 | 특징 |
|------|------|
| 유지보수 비용 | 높음 - 행 삽입/업데이트 시 표현식을 재계산 |
| 검색 성능 | 빠름 - 표현식이 이미 인덱스에 저장되어 있음 |
| 최적 사용 시기 | 검색 속도가 삽입/업데이트 속도보다 중요할 때 |

#### 인덱스 사용 조건

인덱스 표현식이 인덱스 정의와 일치해야 합니다. 예를 들어, `lower(col1)` 인덱스는 다음 쿼리에서 사용됩니다:

```sql
-- 인덱스 사용
SELECT * FROM test1 WHERE lower(col1) = 'value';

-- 인덱스 사용 안 함 (col1과 lower(col1)은 다름)
SELECT * FROM test1 WHERE col1 = 'VALUE';
```

#### 고급 활용: 표현식에 대한 UNIQUE 제약 조건

표현식 인덱스를 `UNIQUE`로 선언하면, 단순 UNIQUE 제약 조건으로 정의할 수 없는 제약 조건 을 강제할 수 있습니다.

예: 대소문자만 다른 `col1` 값의 중복 생성을 방지

```sql
CREATE UNIQUE INDEX test1_lower_col1_idx ON test1 (lower(col1));
```

이 인덱스는 'ABC', 'abc', 'Abc' 등의 값 중 하나만 허용합니다.

---

### 11.8. 부분 인덱스 (Partial Indexes)

부분 인덱스(Partial Index)는 테이블의 부분집합에 대해 구축된 인덱스입니다. 부분집합은 조건식(predicate)이라 불리는 조건 표현식으로 정의됩니다. 인덱스는 해당 조건을 만족하는 테이블 행에 대한 항목만 포함합니다.

부분 인덱스는 다소 특수한 기능이지만 여러 상황에서 유용합니다. 부분 인덱스를 사용하는 주된 이유는 쿼리 워크로드의 관심 대상이 아닌 공통 값을 인덱스에서 제외하여 인덱스 크기를 줄이는 것입니다. 이렇게 하면 인덱스를 사용하는 쿼리가 빨라지고, 인덱스 업데이트 작업도 빨라집니다.

#### 예제 11.1: 공통값 제외하기

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

#### 예제 11.2: 관심 없는 값 제외하기

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

#### 예제 11.3: 부분 고유 인덱스

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

#### 중요한 제한사항: Predicate 매칭 규칙

- 쿼리의 `WHERE` 조건이 인덱스의 predicate를 수학적으로 함축 해야 합니다
- PostgreSQL은 단순한 부등식 함축만 인식합니다 (예: "x < 1" implies "x < 2")
- 정확한 매칭이 필요합니다 (쿼리 계획 시간에 검사)
- 파라미터화된 쿼리는 작동하지 않을 수 있습니다 (예: prepared statement with "x < ?")

#### 예제 11.4: 안티패턴 - 분할의 대체로 사용하지 말 것

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

#### 부분 인덱스의 장점

1. 인덱스 크기 감소: 메모리 효율성 증가
2. 쿼리 성능 향상: 인덱스 사용 쿼리에서
3. 업데이트 성능 향상: 인덱스 갱신 범위 감소
4. 선택적 유니크 제약 적용 가능

#### 주의사항

- 공통값이 미리 결정 되어야 합니다
- 데이터 분포가 변하면 인덱스 재생성이 필요할 수 있습니다
- 쿼리 플래너보다 나은 지식이 있을 때만 사용을 권장합니다
- 대부분의 경우 일반 인덱스와의 성능 차이가 미미할 수 있습니다

---

### 11.9. 인덱스 전용 스캔과 커버링 인덱스 (Index-Only Scans and Covering Indexes)

PostgreSQL의 모든 인덱스는 보조 인덱스(secondary index)입니다. 즉, 각 인덱스는 테이블의 주 데이터 영역(힙)과 별도로 저장됩니다. 따라서 일반 인덱스 스캔에서는 각 행 검색 시 인덱스와 힙 모두에서 데이터를 가져와야 합니다. 또한 인덱스 조건과 일치하는 힙 항목이 힙 내 어디에나 존재할 수 있으므로 힙 접근은 다수의 랜덤 I/O를 수반합니다. 이는 특히 기계식 디스크에서 느릴 수 있습니다.

#### 인덱스 전용 스캔 (Index-Only Scan)

이 성능 문제를 해결하기 위해 PostgreSQL은 힙 접근 없이 쿼리를 처리하는 인덱스 전용 스캔(index-only scan)을 지원합니다.

#### 인덱스 전용 스캔 가능 조건

##### 1. 인덱스 타입 지원

- B-tree: 항상 지원
- GiST, SP-GiST: 일부 연산자 클래스에서만 지원
- GIN: 지원 불가 (원본 데이터의 일부만 저장)

필수 요건: 인덱스가 원본 데이터 값을 물리적으로 저장하거나 재구성 가능해야 합니다.

##### 2. 쿼리가 인덱스에 저장된 컬럼만 참조

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

##### 3. MVCC 가시성 확인

PostgreSQL은 가시성 맵(Visibility Map) 을 사용하여 이 문제를 해결합니다:

- PostgreSQL은 각 힙 페이지에서 모든 행이 모든 활성 및 미래 트랜잭션에 가시적인지 추적하는 비트를 유지합니다
- 인덱스 전용 스캔은 가시성 맵 비트를 먼저 확인합니다
  - 비트 설정됨: 행은 즉시 반환 (힙 접근 불필요)
  - 비트 미설정: 힙 접근 필수 (가시성 확인)
- 장점: 가시성 맵은 힙보다 훨씬 작아 물리 I/O가 훨씬 적습니다
- 효율성: 변경이 적은 테이블에서 매우 유용합니다

#### 커버링 인덱스 (Covering Index)

인덱스 전용 스캔을 최대한 활용하려면 커버링 인덱스를 생성하여, 특정 쿼리 유형에서 인덱스에 "페이로드"로 포함할 추가 컬럼을 지정할 수 있습니다.

##### 기본 문법

```sql
CREATE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

##### 실제 예시

자주 실행되는 쿼리:

```sql
SELECT y FROM tab WHERE x = 'key';
```

인덱스 전용 스캔이 가능한 커버링 인덱스:

```sql
CREATE INDEX tab_x_y ON tab(x) INCLUDE (y);
```

##### 커버링 인덱스의 특징

비-키 컬럼(Payload Column)의 성질:

- 인덱스의 검색 키 부분이 아닙니다
- 데이터 타입 제약이 없습니다 (인덱스가 처리하지 않음)
- 인덱스에 저장되기만 합니다

#### UNIQUE 제약과의 조합

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

#### 성능 고려사항

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

#### 기존 방식과의 비교

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

#### 서픽스 절단 (Suffix Truncation)

- 비-키 컬럼은 B-tree 상위 레벨에서 항상 제거 됩니다
- 인덱스 스캔을 위해 사용되지 않습니다
- 명시적 INCLUDE 사용 시 상위 레벨의 튜플 크기가 안정적으로 축소 됩니다

#### 표현식 인덱스와의 상호작용

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

#### 부분 인덱스와의 상호작용

```sql
-- 부분 유니크 인덱스
CREATE UNIQUE INDEX tests_success_constraint ON tests (subject, target)
    WHERE success;

-- 이 쿼리는 인덱스 전용 스캔 가능
SELECT target FROM tests
WHERE subject = 'some-subject' AND success;
```

작동 원리:
- WHERE 절의 `success` 컬럼은 인덱스에 저장되지 않습니다
- 하지만 인덱스에 있는 모든 항목은 `success = true` 보장
- 런타임 재확인 불필요 (인덱스 전용 스캔 가능)

---

### 11.10. 연산자 클래스와 연산자 패밀리 (Operator Classes and Operator Families)

인덱스 정의는 선택적으로 각 인덱스 컬럼에 대한 연산자 클래스(operator class) 를 지정할 수 있습니다.

#### 기본 문법

```sql
CREATE INDEX name ON table (column opclass [ ( opclass_options ) ] [sort options] [, ...]);
```

#### 연산자 클래스의 역할

연산자 클래스는 해당 컬럼에 대해 인덱스가 사용할 연산자를 지정합니다. 예를 들어, `int4` 타입에 대한 B-tree 인덱스는 4바이트 정수 비교 함수를 제공하는 `int4_ops` 클래스를 사용합니다. 실제로는 컬럼의 데이터 타입에 대한 기본 연산자 클래스로 대부분의 경우 충분합니다.

#### 연산자 클래스가 필요한 경우

동일한 데이터 타입에 대해 여러 의미 있는 인덱싱 동작이 있을 때 연산자 클래스를 명시적으로 지정해야 합니다. 예를 들어:

- 복소수의 절대값 기준 정렬 vs 실수부 기준 정렬
- 연산자 클래스가 기본 정렬 순서를 결정합니다
- `COLLATE`, `ASC`/`DESC`, `NULLS FIRST`/`NULLS LAST` 등으로 수정 가능합니다

#### 내장 연산자 클래스: 패턴 매칭용

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

#### 정의된 연산자 클래스 조회

##### 1. 모든 연산자 클래스 조회

```sql
SELECT am.amname AS index_method,
       opc.opcname AS opclass_name,
       opc.opcintype::regtype AS indexed_type,
       opc.opcdefault AS is_default
    FROM pg_am am, pg_opclass opc
    WHERE opc.opcmethod = am.oid
    ORDER BY index_method, opclass_name;
```

##### 2. 연산자 패밀리와 함께 조회

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

#### 연산자 패밀리 (Operator Families)

연산자 클래스는 실제로 연산자 패밀리(operator family)라는 더 큰 구조의 일부입니다. 일반적으로 연산자 클래스에는 단일 데이터 타입에 대해 동작하는 연산자만 포함되지만, 인덱스에서 서로 다른 데이터 타입 간의 비교를 허용하는 것이 유용한 경우도 있습니다. 이러한 교차 데이터 타입 연산자는 패밀리의 멤버이지만 특정 클래스에는 속하지 않습니다.

##### 3. 모든 연산자 패밀리와 포함된 연산자 조회

```sql
SELECT am.amname AS index_method,
       opf.opfname AS opfamily_name,
       amop.amopopr::regoperator AS opfamily_operator
    FROM pg_am am, pg_opfamily opf, pg_amop amop
    WHERE opf.opfmethod = am.oid AND
          amop.amopfamily = opf.oid
    ORDER BY index_method, opfamily_name, opfamily_operator;
```

#### psql 유용한 명령어

```
\dAc  - 연산자 클래스 정보 (더 정교한 버전)
\dAf  - 연산자 패밀리 정보
\dAo  - 연산자 패밀리에 포함된 연산자 정보
```

#### 실무적 팁

1. 일반적으로: 기본 연산자 클래스가 대부분의 경우 충분합니다
2. 패턴 매칭: C 로케일이 아닌 경우 `xxx_pattern_ops` 사용
3. 여러 인덱스: 같은 열에 다른 연산자 클래스를 가진 여러 인덱스 생성 가능
4. C 로케일 사용: `xxx_pattern_ops`가 불필요 (기본 클래스로 충분)

---

### 11.11. 인덱스와 콜레이션 (Indexes and Collations)

인덱스는 인덱스 컬럼당 하나의 콜레이션만 지원할 수 있습니다. 여러 콜레이션이 필요한 경우, 여러 인덱스를 생성해야 합니다.

#### 기본 개념

인덱스는 자동으로 기본 컬럼의 콜레이션을 사용합니다. 다른 콜레이션으로 쿼리를 실행하면 해당 인덱스를 활용할 수 없습니다.

#### 예제

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

#### 다른 콜레이션을 위한 추가 인덱스

다른 콜레이션("y")으로 쿼리하는 경우:

```sql
SELECT * FROM test1c WHERE content > 'constant' COLLATE "y";
```

위 쿼리를 지원하려면 별도의 인덱스를 생성해야 합니다:

```sql
CREATE INDEX test1c_content_y_index ON test1c (content COLLATE "y");
```

다양한 콜레이션으로 쿼리해야 한다면, 각 콜레이션별로 별도의 인덱스를 생성하여 성능을 최적화할 수 있습니다.

---

### 11.12. 인덱스 사용 검사 (Examining Index Usage)

실제 쿼리 워크로드에서 인덱스가 사용되고 있는지 확인하는 것이 좋습니다. PostgreSQL에서 인덱스 사용을 검사하려면 실험이 필요합니다.

#### 핵심 개념

PostgreSQL의 인덱스는 자동으로 유지보수되지만, 실제 워크로드에서 인덱스가 실제로 사용되는지 확인하는 것이 중요합니다.

#### 인덱스 사용 검사 방법

##### 1. EXPLAIN 명령 사용

개별 쿼리에 대해 인덱스가 사용되는지 확인합니다. 실제 실행 시간을 포함한 정보를 보려면 `EXPLAIN ANALYZE`를 사용합니다:

```sql
EXPLAIN ANALYZE SELECT * FROM test1 WHERE id = 100;
```

##### 2. 통계 수집

실행 중인 서버의 전체 인덱스 사용 통계를 수집합니다. [Section 27.2](https://www.postgresql.org/docs/18/monitoring-stats.html)를 참조하세요.

#### 인덱스 최적화를 위한 팁

| 항목 | 설명 |
|------|------|
| ANALYZE 실행 | 쿼리 플래너의 정확한 비용 추정을 위해 필수 |
| 실제 데이터 사용 | 테스트 데이터로는 부정확한 결과 초래 |
| 인덱스 사용 강제 | `enable_seqscan`, `enable_nestloop` 파라미터로 테스트 |
| EXPLAIN ANALYZE | 실제 성능 측정 및 비교 |
| 비용 조정 | Section 19.7.2의 플래너 비용 상수 조정 |

#### 인덱스 사용 강제 (테스트 목적)

플래너가 순차 스캔을 사용하지 않도록 강제하여 인덱스 스캔을 테스트할 수 있습니다:

```sql
SET enable_seqscan = off;
EXPLAIN ANALYZE SELECT * FROM test1 WHERE id = 100;
SET enable_seqscan = on;  -- 테스트 후 원복
```

이 방법은 플래너가 추정하는 비용과 실제 비용을 비교하는 데 유용합니다.

#### 주의사항

- 매우 작은 테스트 데이터는 인덱스 필요성을 잘못 판단하게 합니다
- 통계가 없으면 인덱스 사용 분석은 무의미합니다
- 인덱스가 사용되지 않는 이유는 여러 가지가 있을 수 있습니다:
  - 테이블이 너무 작아서 순차 스캔이 더 빠름
  - 쿼리 조건이 인덱스와 맞지 않음
  - 통계가 오래되어 플래너가 잘못된 추정을 함

#### 인덱스 사용 여부를 확인하는 이유

- 사용되지 않는 인덱스는 디스크 공간을 차지하고 업데이트 성능을 저하시킵니다
- 주기적으로 인덱스 사용을 검토하고 불필요한 인덱스를 제거하는 것이 좋습니다

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 11. Indexes](https://www.postgresql.org/docs/18/indexes.html)
- [CREATE INDEX 문서](https://www.postgresql.org/docs/18/sql-createindex.html)
- [GiST 인덱스](https://www.postgresql.org/docs/18/gist.html)
- [SP-GiST 인덱스](https://www.postgresql.org/docs/18/spgist.html)
- [GIN 인덱스](https://www.postgresql.org/docs/18/gin.html)
- [BRIN 인덱스](https://www.postgresql.org/docs/18/brin.html)

---

## Chapter 12. 전문 검색 (Full Text Search)

PostgreSQL 18 공식 문서 기반

---

### 목차

- [12.1 소개](#121-소개)
  - [12.1.1 문서란 무엇인가](#1211-문서란-무엇인가)
  - [12.1.2 기본 텍스트 매칭](#1212-기본-텍스트-매칭)
  - [12.1.3 구성](#1213-구성)
- [12.2 테이블과 인덱스](#122-테이블과-인덱스)
  - [12.2.1 테이블 검색](#1221-테이블-검색)
  - [12.2.2 인덱스 생성](#1222-인덱스-생성)
- [12.3 텍스트 검색 제어](#123-텍스트-검색-제어)
  - [12.3.1 문서 파싱](#1231-문서-파싱)
  - [12.3.2 쿼리 파싱](#1232-쿼리-파싱)
  - [12.3.3 검색 결과 순위 지정](#1233-검색-결과-순위-지정)
  - [12.3.4 결과 강조](#1234-결과-강조)
- [12.4 추가 기능](#124-추가-기능)
  - [12.4.1 문서 조작](#1241-문서-조작)
  - [12.4.2 쿼리 조작](#1242-쿼리-조작)
  - [12.4.3 자동 업데이트 트리거](#1243-자동-업데이트-트리거)
  - [12.4.4 문서 통계 수집](#1244-문서-통계-수집)
- [12.5 파서](#125-파서)
- [12.6 사전](#126-사전)
  - [12.6.1 불용어](#1261-불용어)
  - [12.6.2 Simple 사전](#1262-simple-사전)
  - [12.6.3 Synonym 사전](#1263-synonym-사전)
  - [12.6.4 Thesaurus 사전](#1264-thesaurus-사전)
  - [12.6.5 Ispell 사전](#1265-ispell-사전)
  - [12.6.6 Snowball 사전](#1266-snowball-사전)
- [12.7 구성 예제](#127-구성-예제)
- [12.8 텍스트 검색 테스트 및 디버깅](#128-텍스트-검색-테스트-및-디버깅)
  - [12.8.1 구성 테스트](#1281-구성-테스트)
  - [12.8.2 파서 테스트](#1282-파서-테스트)
  - [12.8.3 사전 테스트](#1283-사전-테스트)
- [12.9 전문 검색을 위한 선호 인덱스 유형](#129-전문-검색을-위한-선호-인덱스-유형)
- [12.10 psql 지원](#1210-psql-지원)
- [12.11 제한사항](#1211-제한사항)

---

### 12.1 소개

전문 검색(Full Text Searching)은 자연 언어 문서의 집합에서 쿼리를 만족하는 문서를 찾아내고, 선택적으로 쿼리와의 관련성에 따라 정렬하는 기능을 제공합니다. 가장 일반적인 검색 유형은 주어진 쿼리 용어를 포함하는 모든 문서를 찾아 유사도 순서로 반환하는 것입니다. `쿼리`와 `유사도`의 개념은 매우 유연하며 특정 애플리케이션에 따라 달라집니다. 가장 단순한 검색은 쿼리를 단어들의 집합으로, 유사도를 문서 내 쿼리 단어의 빈도로 간주합니다.

#### 기존 텍스트 검색 연산자의 한계

텍스트 검색 연산자는 데이터베이스에서 수년간 존재해왔습니다. PostgreSQL은 텍스트 데이터 타입에 대해 `~`, `~*`, `LIKE`, `ILIKE` 연산자를 제공하지만, 현대적인 정보 시스템에서 요구되는 몇 가지 필수 속성이 부족합니다:

- 언어 지원 부재: 정규 표현식으로는 단어의 파생형을 쉽게 처리할 수 없습니다. 예를 들어 `satisfies`와 `satisfy`를 처리하지 못합니다. 필요한 모든 형태를 나열하기 위해 OR을 사용할 수 있지만, 이는 번거롭고 오류가 발생하기 쉽습니다(일부 단어는 수천 개의 파생형을 가질 수 있음).

- 검색 결과 정렬 불가: 수천 개의 일치하는 문서가 발견될 때 순위를 매기는 방법을 제공하지 않습니다.

- 느린 성능: 인덱스 지원이 없으므로 모든 검색에서 모든 문서를 처리해야 합니다.

#### 전문 인덱싱의 전처리 과정

전문 인덱싱을 통해 문서를 사전 처리하고 이후 검색에 사용되는 인덱스를 저장할 수 있습니다. 사전 처리에는 다음이 포함됩니다:

##### 1. 토큰화 (문서를 토큰으로 파싱)

각 토큰 클래스(숫자, 단어, 복합어, 이메일 주소 등)를 식별하여 다르게 처리할 수 있도록 합니다. 원칙적으로 토큰 클래스는 특정 애플리케이션에 따라 다르지만, 대부분의 목적에서 미리 정의된 클래스 집합을 사용하는 것이 적절합니다. PostgreSQL은 파서(parser) 를 사용하여 이 단계를 수행합니다. 표준 파서가 제공되며, 특정 요구사항에 맞게 사용자 정의 파서를 만들 수 있습니다.

##### 2. 렉심 변환 (토큰을 렉심으로 변환)

렉심(lexeme)은 토큰과 같은 문자열이지만, 같은 단어의 다른 형태들이 동일하게 처리되도록 정규화됩니다. 예를 들어, 정규화에는 거의 항상 대문자를 소문자로 변환하는 것이 포함되며, 종종 접미사 제거(예: 영어의 `s` 또는 `es`)가 포함됩니다. 이를 통해 문서에 특정 형태가 없더라도 같은 단어의 다른 문법적 형태로 검색하면 해당 문서가 일치될 수 있습니다. 또한, 이 단계에서는 일반적으로 불용어(stop words), 즉 너무 흔해서 검색에 쓸모없는 단어를 제거합니다. (간단히 말해, 토큰은 문서 텍스트의 원시 조각이고, 렉심은 인덱싱과 검색에 유용하다고 간주되는 단어입니다.) PostgreSQL은 사전(dictionaries)을 사용하여 이 단계를 수행합니다. 다양한 표준 사전이 제공되며, 특정 요구사항에 맞게 사용자 정의 사전을 만들 수 있습니다.

##### 3. 전처리된 문서 저장

검색에 최적화된 형태로 저장합니다. 예를 들어, 각 문서는 정규화된 렉심의 정렬된 배열로 표현될 수 있습니다. 근접성 순위 지정(proximity ranking)을 위해 렉심의 위치 정보를 함께 저장하는 것이 바람직합니다. 이를 통해 쿼리 단어가 서로 인접한 문서가 떨어져 있는 문서보다 높은 순위를 받을 수 있습니다.

#### 사전의 기능

사전을 통해 토큰이 정규화되는 방식을 세밀하게 제어할 수 있습니다. 적절한 사전을 사용하여 다음을 수행할 수 있습니다:

- 인덱싱하지 않을 불용어 정의
- Ispell을 사용한 동의어 매핑
- 시소러스를 사용한 구문을 단일 단어로 매핑
- Ispell 사전을 사용하여 단어의 다양한 변형을 정규형으로 매핑
- Snowball stemmer 규칙을 사용하여 단어의 다양한 변형을 정규형으로 매핑

#### 데이터 타입 및 연산자

전처리된 문서를 저장하기 위해 `tsvector` 데이터 타입이 제공되고, 처리된 쿼리를 표현하기 위해 `tsquery` 타입이 제공됩니다(8.11절 참조). 이 데이터 타입에 사용할 수 있는 다양한 함수와 연산자가 있으며(9.13절 참조), 그 중 가장 중요한 것은 매치 연산자 `@@`로, 12.1.2절에서 소개합니다. 전문 검색은 인덱스를 사용하여 속도를 높일 수 있습니다(12.9절 참조).

---

#### 12.1.1 문서란 무엇인가

문서는 전문 검색 시스템에서 검색의 단위입니다. 예를 들어, 잡지 기사나 이메일 메시지가 해당됩니다. 텍스트 검색 엔진은 문서를 파싱하고, 부모 문서에 대한 참조와 함께 렉심(키워드)을 저장할 수 있어야 합니다. 이후 검색은 쿼리를 포함하는 문서를 찾는 데 사용됩니다.

PostgreSQL에서 검색의 경우, 문서는 일반적으로 데이터베이스 테이블 내의 텍스트 필드 또는 필드들의 조합(연결)입니다. 가능한 경우 여러 테이블에 저장되거나 동적으로 획득될 수 있습니다. 즉, 문서는 인덱싱을 위해 다른 소스에서 구성될 수 있습니다. 예를 들어:

예제 1: 단일 테이블

```sql
SELECT title || ' ' ||  author || ' ' ||  abstract || ' ' || body AS document
FROM messages
WHERE mid = 12;
```

예제 2: 여러 테이블 결합

```sql
SELECT m.title || ' ' || m.author || ' ' || m.abstract || ' ' || d.body AS document
FROM messages m, docs d
WHERE m.mid = d.did AND m.mid = 12;
```

> 참고: 실제로 이러한 쿼리에서는 `NULL` 값을 처리하기 위해 `coalesce`를 사용해야 합니다. 다른 필드가 `NULL`이 아니더라도 하나의 `NULL` 속성이 `NULL` 결과를 만들 수 있습니다.

또 다른 가능성은 파일 시스템에 단순 텍스트 파일로 문서를 저장하는 것입니다. 이 경우 데이터베이스는 전문 인덱스를 저장하고 검색을 실행하는 데 사용될 수 있으며, 검색된 문서를 파일 시스템에서 가져오기 위해 고유 식별자를 사용할 수 있습니다. 그러나 데이터베이스 외부에서 파일을 검색하려면 슈퍼유저 권한이 필요하거나 특별한 함수 지원이 필요하므로, 일반적으로 데이터베이스 내에 모든 데이터를 유지하는 것이 편리합니다. 또한 데이터베이스 내에 모든 것을 유지하면 문서 메타데이터에 쉽게 접근하여 인덱싱 및 표시를 지원할 수 있습니다.

텍스트 검색 목적상, 각 문서는 전처리된 `tsvector` 형식으로 변환되어야 합니다. 검색과 순위 지정은 전적으로 문서의 `tsvector` 표현을 기반으로 수행됩니다. 원본 텍스트는 사용자에게 표시할 문서를 선택한 경우에만 조회하면 됩니다. 따라서 `tsvector`를 문서의 간략한 요약으로 생각할 수 있습니다.

---

#### 12.1.2 기본 텍스트 매칭

PostgreSQL에서 전문 검색은 매치 연산자 `@@`를 기반으로 합니다. 이 연산자는 `tsvector`(문서)가 `tsquery`(쿼리)와 일치하면 `true`를 반환합니다. 어떤 데이터 타입이 먼저 작성되든 상관없습니다:

```sql
SELECT 'a fat cat sat on a mat and ate a fat rat'::tsvector @@ 'cat & rat'::tsquery;
 ?column?
----------
 t
```

```sql
SELECT 'fat & cow'::tsquery @@ 'a fat cat sat on a mat and ate a fat rat'::tsvector;
 ?column?
----------
 f
```

`tsquery`는 `@@` 연산자가 검색할 검색어를 포함하며, 검색어는 `&`(AND), `|`(OR), `!`(NOT), `<->`(FOLLOWED BY) 연산자를 사용하여 결합할 수 있습니다. 예를 들어, 쿼리 `'fat & cow'`는 `fat`과 `cow`를 모두 포함하는 문서와만 일치합니다.

#### 정규화의 중요성

위의 예에서 주목할 점은 `rats`와 같은 단어가 일치하지 않는다는 것입니다. 직접 캐스트한 `tsvector`의 요소들은 렉심으로 취급되는데, 이는 `rat` 같은 단어가 이미 정규화된 상태라고 가정하기 때문입니다. 다음은 동작하지 않습니다:

```sql
SELECT 'fat cats ate fat rats'::tsvector @@ to_tsquery('fat & rat');
 ?column?
----------
 f
```

`rat`은 `rats`와 일치하지 않습니다.

`to_tsvector` 함수는 토큰을 정규화하고 불용어를 제거하여 적절한 `tsvector`를 생성하는 데 사용해야 합니다(자세한 내용은 12.3.1절 참조):

```sql
SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('fat & rat');
 ?column?
----------
 t
```

#### 매치 연산자의 변형

`@@` 연산자는 `tsquery`를 암묵적으로 변환하는 `text` 입력도 지원합니다:

| 형식 | 설명 |
|------|------|
| `tsvector @@ tsquery` | 기본 형식 |
| `tsquery @@ tsvector` | 순서 반대 |
| `text @@ tsquery` | `to_tsvector(text) @@ tsquery`와 동일 |
| `text @@ text` | `to_tsvector(x) @@ plainto_tsquery(y)`와 동일 |

`text @@ text` 형식은 입력이 더 간편하지만, 기능적으로는 제한적입니다. 이 형식은 가중치, 접두사 매칭 또는 구문 검색 연산자를 지원하지 않습니다.

#### 논리 연산자

| 연산자 | 설명 | 예제 |
|--------|------|------|
| `&` (AND) | 두 인자가 모두 나타나야 함 | `fat & rat` |
| `\|` (OR) | 인자 중 하나 이상이 나타나야 함 | `fat \| cow` |
| `!` (NOT) | 인자가 나타나지 않아야 함 | `fat & ! rat` |

#### 구문 검색: FOLLOWED BY 연산자 `<->`

`tsquery`에서 구문 검색을 지정하려면 `<->` (FOLLOWED BY) `tsquery` 연산자를 사용합니다. 이 연산자는 왼쪽과 오른쪽 인자가 서로 인접하여 해당 순서로 나타날 때만 일치합니다. 예를 들어:

```sql
SELECT to_tsvector('fatal error') @@ to_tsquery('fatal <-> error');
 ?column?
----------
 t

SELECT to_tsvector('error is not fatal') @@ to_tsquery('fatal <-> error');
 ?column?
----------
 f
```

#### 일반화된 FOLLOWED BY 연산자 `<N>`

FOLLOWED BY 연산자의 더 일반적인 버전은 `<N>` 형식으로 작성되며, 여기서 `N`은 일치하는 렉심 위치 간의 차이를 나타내는 정수입니다:

- `<1>`: `<->`와 동일 (인접)
- `<2>`: 정확히 하나의 다른 렉심이 중간에 나타남
- `<0>`: 두 패턴이 같은 단어와 일치해야 함

```sql
SELECT phraseto_tsquery('cats ate rats');
       phraseto_tsquery
-------------------------------
 'cat' <-> 'ate' <-> 'rat'

SELECT phraseto_tsquery('the cats ate the rats');
       phraseto_tsquery
-------------------------------
 'cat' <-> 'ate' <2> 'rat'
```

> 참고: `phraseto_tsquery`는 불용어 위치를 고려하여 `<N>`을 구성합니다.

#### 연산자 우선순위

괄호를 사용하지 않을 때 연산자의 우선순위는 (낮음에서 높음으로):

1. `|` (OR) - 가장 낮음
2. `&` (AND)
3. `<->` (FOLLOWED BY)
4. `!` (NOT) - 가장 높음

예를 들어:

```sql
SELECT to_tsquery('fat & rat & ! cat');
    to_tsquery
------------------
 'fat' & 'rat' & !'cat'
```

#### FOLLOWED BY 내에서의 NOT 의미 변화

괄호 없이 `!x <-> y`에서 `!`가 `<->`의 인자로 해석되면, `NOT`은 해당 피연산자가 `y`와 인접하지 않아야 함을 의미합니다. 문서의 다른 곳에 `x`가 존재해도 일치하지 않게 만들지 않습니다:

```sql
SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('!cat <-> ate');
 ?column?
----------
 f

SELECT to_tsvector('fat cats ate fat rats') @@ to_tsquery('cat <-> !ate');
 ?column?
----------
 f
```

---

#### 12.1.3 구성

위의 내용은 모두 간단한 텍스트 예제였습니다. 앞서 언급했듯이, 전문 검색 기능에는 다음과 같은 기능이 포함됩니다:

- 특정 단어의 인덱싱 생략(불용어)
- 동의어 처리
- 어간 추출(stemming)을 사용한 정교한 구문 분석

이 모든 것은 텍스트 검색 구성(text search configurations) 에 의해 제어됩니다.

#### PostgreSQL 텍스트 검색의 4가지 구성 요소

PostgreSQL에는 많은 언어에 대한 미리 정의된 구성이 제공되며, 사용자 정의 구성도 쉽게 만들 수 있습니다. psql의 `\dF` 명령은 사용 가능한 모든 구성을 보여줍니다.

설치 중에 적절한 구성이 선택되고 `postgresql.conf`의 `default_text_search_config`가 그에 맞게 설정됩니다. 클러스터 전체에 동일한 텍스트 검색 구성을 사용하는 경우 `postgresql.conf` 값을 그대로 사용할 수 있습니다. 클러스터 내 데이터베이스마다 다른 구성을 사용하려면 `ALTER DATABASE ... SET`을 사용합니다. 세션 단위로 변경하려면 `SET default_text_search_config`를 사용할 수 있습니다.

구성에 의존하는 각 텍스트 검색 함수에는 사용할 구성을 명시적으로 지정하기 위한 선택적 `regconfig` 인자가 있습니다. `default_text_search_config`는 이 인자가 생략된 경우에만 사용됩니다.

사용자 정의 텍스트 검색 구성을 더 쉽게 만들기 위해, 구성은 더 간단한 데이터베이스 객체로 구축됩니다:

1. 텍스트 검색 파서: 문서 텍스트를 토큰으로 분할하고 각 토큰을 분류합니다(예: 단어 또는 숫자로)
2. 텍스트 검색 사전: 토큰을 정규화된 형태로 변환하고 불용어를 거부합니다
3. 텍스트 검색 템플릿: 사전의 기반이 되는 함수를 제공합니다(사전 자체는 단순히 템플릿에 대한 매개변수를 지정합니다)
4. 텍스트 검색 구성: 문서 분석에 사용할 파서를 선택하고, 파서 출력 토큰을 사전으로 변환하기 위한 사전-토큰 매핑을 지정합니다

텍스트 검색 파서와 템플릿은 저수준 C 함수로 구현됩니다. 따라서 새로 개발하려면 C 프로그래밍 능력이 필요하며, 설치하려면 슈퍼유저 권한이 필요합니다(12.11절의 예제 참조). 다양한 언어에 대한 기존 파서와 템플릿이 이미 제공되므로 대부분의 사용자는 직접 파서를 만들 필요가 없습니다. 사전과 구성을 만드는 데는 특별한 권한이 필요 없으며, 이 장 뒷부분에서 예제를 제공합니다.

---

### 12.2 테이블과 인덱스

이전 섹션의 예제들은 단순 상수 문자열과의 전문 매칭을 설명했습니다. 이 섹션에서는 테이블 데이터를 검색하는 방법을 보여주며, 선택적으로 인덱스를 사용합니다.

---

#### 12.2.1 테이블 검색

인덱스 없이도 전문 검색을 수행할 수 있습니다. 다음은 `body` 필드에서 `friend` 단어를 포함하는 각 행의 `title`을 출력하는 간단한 쿼리입니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector('english', body) @@ to_tsquery('english', 'friend');
```

특징:

- `friends`, `friendly` 등 관련 단어도 찾습니다(어간 추출로 정규화된 렉심으로 축소되기 때문)
- `english` 구성을 사용하여 문자열 파싱 및 정규화를 수행합니다

또한 위의 쿼리는 명시적으로 구성 이름을 지정하지 않고 작성할 수 있습니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(body) @@ to_tsquery('friend');
```

이 쿼리는 `default_text_search_config`에 의해 설정된 구성을 사용합니다.

#### 복잡한 예제

`title` 또는 `body`에서 `create`와 `table`을 포함하는 가장 최신 문서 10개를 선택합니다:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(title || ' ' || body) @@ to_tsquery('create & table')
ORDER BY last_mod_date DESC
LIMIT 10;
```

> 참고: 명확성을 위해 위에서 `coalesce` 함수 호출을 생략했지만, 두 필드 중 하나가 `NULL`인 행을 찾으려면 필요합니다.

#### 성능 고려사항

이러한 쿼리들은 인덱스 없이도 동작하지만, 대부분의 애플리케이션에서 이 접근 방식은 너무 느립니다. 간헐적인 임시 검색을 제외하고 실제 텍스트 검색 사용에는 일반적으로 인덱스 생성이 필요합니다.

---

#### 12.2.2 인덱스 생성

검색 속도를 높이기 위해 인덱스를 생성할 수 있습니다. PostgreSQL은 텍스트 검색에 대해 GIN 및 GiST 인덱스 유형을 지원합니다(12.9절 참조). 인덱스가 매우 유용하지만 전문 검색에 필수는 아닙니다.

#### 방법 1: GIN 표현식 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector('english', body));
```

중요 규칙:

- `to_tsvector`의 2-인자 버전을 사용해야 합니다
- 구성 이름을 명시적으로 지정하는 텍스트 검색 함수만 표현식 인덱스에 사용할 수 있습니다
- 이는 인덱스 내용이 `default_text_search_config`에 영향을 받지 않아야 하기 때문입니다

쿼리 사용:

```sql
-- 인덱스를 사용함 (같은 구성)
SELECT title
FROM pgweb
WHERE to_tsvector('english', body) @@ to_tsquery('english', 'friend');

-- 인덱스를 사용하지 않음 (구성 미지정)
SELECT title
FROM pgweb
WHERE to_tsvector(body) @@ to_tsquery('friend');
```

#### 방법 2: 다중 구성 지원 표현식 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector(config_name, body));
```

- `config_name`은 `pgweb` 테이블의 열입니다
- 같은 인덱스에서 혼합 구성이 가능합니다(각 항목에 대해 어떤 구성이 사용되었는지 기록)
- 다국어 문서 컬렉션에 유용합니다

쿼리:

```sql
SELECT title
FROM pgweb
WHERE to_tsvector(config_name, body) @@ to_tsquery('english', 'friend');
```

#### 방법 3: 열 연결 인덱스

```sql
CREATE INDEX pgweb_idx ON pgweb USING GIN (to_tsvector('english', title || ' ' || body));
```

#### 방법 4: 별도의 tsvector 열 (저장된 생성 열)

별도의 `tsvector` 열을 생성하여 `to_tsvector`의 출력을 저장합니다. 저장된 생성 열(stored generated column)을 사용하면 원본 데이터에서 자동으로 업데이트됩니다:

```sql
ALTER TABLE pgweb
    ADD COLUMN textsearchable_index_col tsvector
               GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))) STORED;
```

GIN 인덱스 생성:

```sql
CREATE INDEX textsearch_idx ON pgweb USING GIN (textsearchable_index_col);
```

빠른 검색 실행:

```sql
SELECT title
FROM pgweb
WHERE textsearchable_index_col @@ to_tsquery('create & table')
ORDER BY last_mod_date DESC
LIMIT 10;
```

#### 방법 비교

| 방법 | 장점 | 단점 |
|------|------|------|
| 표현식 인덱스 | 설정이 간단, 디스크 공간 절약 | 쿼리에서 구성 명시 필요, 검증 시 `tsvector` 재계산 필요 |
| 별도 열 | 구성 명시 불필요, 더 빠른 검색, `default_text_search_config` 사용 가능 | 더 많은 디스크 공간, 설정이 더 복잡 |

---

### 12.3 텍스트 검색 제어

전문 검색을 구현하려면 문서를 `tsvector`로, 사용자 쿼리를 `tsquery`로 변환하는 함수가 필요합니다. 또한 결과를 유용한 순서로 반환하고 결과를 보기 좋게 표시해야 합니다. PostgreSQL은 이 모든 기능을 지원합니다.

---

#### 12.3.1 문서 파싱

PostgreSQL은 문서를 `tsvector` 타입으로 변환하는 `to_tsvector` 함수를 제공합니다.

```sql
to_tsvector([ config regconfig, ] document text) returns tsvector
```

`to_tsvector`는 텍스트 문서를 토큰으로 파싱하고, 토큰을 렉심으로 축소하며, 각 렉심의 위치와 함께 `tsvector`를 반환합니다. 문서는 지정된 또는 기본 텍스트 검색 구성에 따라 처리됩니다. 다음은 간단한 예입니다:

```sql
SELECT to_tsvector('english', 'a fat cat sat on a mat - it ate a fat rats');
                  to_tsvector
-----------------------------------------------------
 'ate':9 'cat':3 'fat':2,11 'mat':7 'rat':12 'sat':4
```

위의 예에서 볼 수 있듯이 결과 `tsvector`에는 불용어(a, on, it)가 포함되지 않고, `rats`가 `rat`으로 어간 추출되었으며, 구두점 기호 `-`가 무시되었습니다.

`to_tsvector` 함수는 내부적으로 파서를 호출하여 문서 텍스트를 토큰으로 분할하고 각 토큰에 타입을 할당합니다. 각 토큰에 대해 사전 목록이 참조되며(12.6절), 이 목록은 토큰 타입에 따라 달라질 수 있습니다. 토큰을 인식하는 첫 번째 사전이 하나 이상의 정규화된 렉심을 출력하여 결과를 나타냅니다. 또는, 사전이 토큰을 불용어로 인식하거나 토큰이 어떤 사전에도 인식되지 않으면 무시되고 인덱싱되지 않습니다. PostgreSQL은 많은 언어에 대한 미리 정의된 구성을 제공하며, 자체 구성을 쉽게 만들 수 있습니다(psql의 `\dF` 명령은 모든 사용 가능한 구성을 표시합니다).

#### setweight 함수

```sql
setweight(vector tsvector, weight "char") returns tsvector
```

`setweight`는 `tsvector`의 모든 위치에 주어진 가중치를 레이블로 지정한 입력 벡터의 복사본을 반환합니다. 가중치는 문자 `A`, `B`, `C` 또는 `D`입니다. `D`는 새 벡터의 기본값이므로 출력에 표시되지 않습니다:

```sql
SELECT setweight(to_tsvector('fat cats ate rats'), 'A');
            setweight
----------------------------------
 'ate':3A 'cat':2A 'fat':1A 'rat':4A
```

가중치는 문서의 다른 부분(예: 제목 대 본문)을 표시하는 데 일반적으로 사용됩니다. 나중에 이 정보는 검색 결과의 순위를 매기는 데 사용될 수 있습니다.

가중치가 서로 다른 벡터들은 연결하여 결합할 수 있으므로, 문서의 다른 부분을 다르게 표시하면서 한 번의 연결 작업으로 `tsvector`를 생성하는 것이 중요합니다:

```sql
UPDATE tt SET ti =
    setweight(to_tsvector(coalesce(title,'')), 'A')    ||
    setweight(to_tsvector(coalesce(keyword,'')), 'B')  ||
    setweight(to_tsvector(coalesce(abstract,'')), 'C') ||
    setweight(to_tsvector(coalesce(body,'')), 'D');
```

여기서 `coalesce`를 사용하여 `NULL` 필드가 다른 필드의 결과에 영향을 주지 않도록 합니다.

---

#### 12.3.2 쿼리 파싱

PostgreSQL은 쿼리를 `tsquery` 타입으로 변환하는 함수 `to_tsquery`, `plainto_tsquery`, `phraseto_tsquery` 및 `websearch_to_tsquery`를 제공합니다.

##### to_tsquery 함수

```sql
to_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`to_tsquery`는 `querytext`로부터 `tsquery` 값을 생성합니다. `querytext`는 `tsquery` 연산자 `&` (AND), `|` (OR), `!` (NOT), `<->` (FOLLOWED BY)로 구분된 단일 토큰으로 구성되어야 합니다. 괄호로 그룹화할 수 있습니다. 각 토큰은 지정된 또는 기본 구성을 사용하여 렉심으로 정규화됩니다. 예를 들어:

```sql
SELECT to_tsquery('english', 'The & Fat & Rats');
  to_tsquery
---------------
 'fat' & 'rat'
```

위의 예에서 볼 수 있듯이 `to_tsquery`는 불용어를 버릴 뿐만 아니라 쿼리가 유효하도록 그것을 고려하는 연산자도 버립니다.

##### 가중치 지정

가중치를 각 렉심에 붙여 특정 가중치를 가진 `tsvector` 렉심만 일치하도록 제한할 수 있습니다:

```sql
SELECT to_tsquery('english', 'Fat | Rats:AB');
    to_tsquery
------------------
 'fat' | 'rat':AB
```

##### 접두사 매칭

렉심에 `*`를 붙여 접두사가 일치하도록 지정할 수 있습니다:

```sql
SELECT to_tsquery('supern:*A & star:A*B');
        to_tsquery
--------------------------
 'supern':*A & 'star':*AB
```

##### 구문 검색

쌍따옴표로 구문을 지정할 수 있습니다:

```sql
SELECT to_tsquery('''supernovae stars'' & !crab');
  to_tsquery
---------------
 'sn' & !'crab'
```

##### plainto_tsquery 함수

```sql
plainto_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`plainto_tsquery`는 형식이 지정되지 않은 텍스트 `querytext`를 `tsquery` 값으로 변환합니다. 텍스트는 `to_tsvector`와 마찬가지로 파싱되고 정규화된 후, `&` (AND) 불리언 연산자가 남아 있는 단어들 사이에 삽입됩니다.

```sql
SELECT plainto_tsquery('english', 'The Fat Rats');
 plainto_tsquery
-----------------
 'fat' & 'rat'
```

> 참고: `plainto_tsquery`는 입력에서 어떤 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블도 인식하지 않습니다:

```sql
SELECT plainto_tsquery('english', 'The Fat & Rats:C');
   plainto_tsquery
---------------------
 'fat' & 'rat' & 'c'
```

여기서 입력의 모든 구두점이 공백 기호로 무시되었습니다.

##### phraseto_tsquery 함수

```sql
phraseto_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`phraseto_tsquery`는 `plainto_tsquery`와 비슷하게 동작하지만, `&` (AND) 불리언 연산자 대신 `<->` (FOLLOWED BY) 연산자를 남아 있는 단어들 사이에 삽입합니다. 또한 불용어는 단순히 버려지지 않고 `<N>` 연산자에서 고려됩니다. 이 함수는 특정 렉심 시퀀스를 검색하는 데 유용합니다(예: 사전 테스트 용도).

```sql
SELECT phraseto_tsquery('english', 'The Fat Rats');
 phraseto_tsquery
------------------
 'fat' <-> 'rat'

SELECT phraseto_tsquery('english', 'The Fat and Rats');
   phraseto_tsquery
------------------------
 'fat' <2> 'rat'
```

`plainto_tsquery`와 마찬가지로 `phraseto_tsquery` 함수는 입력에서 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블을 인식하지 않습니다.

##### websearch_to_tsquery 함수

```sql
websearch_to_tsquery([ config regconfig, ] querytext text) returns tsquery
```

`websearch_to_tsquery`는 일반적인 웹 검색 엔진에서 사용되는 것과 유사한 대체 구문을 사용하여 `querytext`에서 `tsquery` 값을 생성합니다.

지원 구문:

- 따옴표 없는 텍스트: `plainto_tsquery`처럼 `&` 연산자로 구분된 단어들로 변환
- "따옴표 텍스트": `phraseto_tsquery`처럼 `<->` 연산자로 구분된 단어들로 변환
- OR: `|` 연산자로 변환
- -: `!` (NOT) 연산자로 변환

다른 구두점은 무시됩니다. 따라서 `plainto_tsquery`나 `phraseto_tsquery`와 마찬가지로 `websearch_to_tsquery` 함수는 입력에서 `tsquery` 연산자, 가중치 레이블 또는 접두사 레이블을 인식하지 않습니다.

```sql
SELECT websearch_to_tsquery('english', 'The fat rats');
 websearch_to_tsquery
----------------------
 'fat' & 'rat'

SELECT websearch_to_tsquery('english', '"supernovae stars" -crab');
       websearch_to_tsquery
----------------------------------
 'supernova' <-> 'star' & !'crab'

SELECT websearch_to_tsquery('english', '"sad cat" or "fat rat"');
       websearch_to_tsquery
-----------------------------------
 'sad' <-> 'cat' | 'fat' <-> 'rat'

SELECT websearch_to_tsquery('english', 'signal -"segmentation fault"');
         websearch_to_tsquery
---------------------------------------
 'signal' & !( 'segment' <-> 'fault' )

SELECT websearch_to_tsquery('english', '""" )( dummy \\ query <->');
 websearch_to_tsquery
----------------------
 'dummi' & 'queri'
```

---

#### 12.3.3 검색 결과 순위 지정

순위 지정은 문서가 특정 쿼리와 얼마나 관련 있는지를 측정하여 가장 관련성 높은 문서를 먼저 표시하는 것을 목표로 합니다. PostgreSQL은 두 가지 미리 정의된 순위 지정 함수를 제공합니다. 이 함수들은 텍스트 정보, 근접 정보, 구조 정보를 고려합니다. 즉, 쿼리 용어가 문서에 얼마나 자주, 얼마나 가깝게, 그리고 어느 부분에 나타나는지를 고려합니다. 그러나 관련성의 개념은 모호하며 애플리케이션마다 다릅니다. 일부 애플리케이션은 정렬 순서를 계산하기 위해 추가 정보(예: 문서 수정 시간)가 필요할 수 있습니다. 내장 순위 지정 함수는 예시일 뿐이며, 필요에 따라 자체 순위 지정 함수를 작성하거나 기존 결과를 추가 요소와 결합할 수 있습니다.

##### ts_rank 함수

```sql
ts_rank([ weights float4[], ] vector tsvector, query tsquery [, normalization integer ]) returns float4
```

일치하는 렉심의 빈도를 기반으로 벡터의 순위를 지정합니다.

##### ts_rank_cd 함수

```sql
ts_rank_cd([ weights float4[], ] vector tsvector, query tsquery [, normalization integer ]) returns float4
```

커버 밀도(cover density) 순위를 계산합니다. 이 함수는 일치하는 렉심 간의 근접성을 고려한다는 점에서 `ts_rank`와 유사합니다.

> 참고: 이 순위 지정 방법은 렉심 위치 정보를 요구하므로 "제거된" 렉심은 무시됩니다. 입력에 제거되지 않은 렉심이 없으면 결과는 0이 됩니다. (위치 정보 없는 `tsvector`에 대한 쿼리의 자세한 내용은 12.4.1절 참조)

##### 가중치 배열

두 함수 모두 선택적 `weights` 인자를 사용하여 레이블별로 단어 인스턴스에 서로 다른 가중치를 부여할 수 있습니다. 가중치 배열은 각 단어 범주에 부여할 가중치를 다음 순서로 지정합니다:

```
{D-weight, C-weight, B-weight, A-weight}
```

가중치가 제공되지 않으면 다음 기본값이 사용됩니다:

```
{0.1, 0.2, 0.4, 1.0}
```

일반적인 가중치 사용은 제목과 같은 특수 영역의 단어에 본문 단어보다 높은 가중치를 부여하는 것입니다.

##### 정규화(Normalization) 옵션

두 순위 함수는 정수형 `normalization` 옵션을 사용하여 문서 길이에 따른 순위 계산 방식을 지정합니다. 이 옵션은 비트마스크이며, `|`를 사용하여 여러 동작을 동시에 지정할 수 있습니다(예: `2|4`).

| 값 | 설명 |
|---|------|
| 0 | (기본값) 문서 길이 무시 |
| 1 | 순위를 1 + 문서 길이의 로그로 나눔 |
| 2 | 순위를 문서 길이로 나눔 |
| 4 | 순위를 익스텐트 내 평균 조화 거리로 나눔(이는 `ts_rank_cd`에 의해서만 구현됨) |
| 8 | 순위를 문서 내 고유 단어 수로 나눔 |
| 16 | 순위를 1 + 문서 내 고유 단어 수의 로그로 나눔 |
| 32 | 순위를 자체 + 1로 나눔 |

> 참고: 32 옵션을 사용하면 모든 순위가 0에서 1 범위로 스케일링되지만, 물론 이것은 단조로운 변환이므로 검색 결과의 순서에 영향을 미치지 않습니다.

순위 함수는 전역 정보를 사용하지 않으므로, 때로는 1%와 99% 임계값과 같은 원하는 결과를 얻기가 어렵습니다. 정규화 옵션 32(`rank/(rank+1)`)를 사용하면 순위를 0에서 1 사이 범위로 변환할 수 있지만, 이는 표면적인 변환일 뿐입니다. 실제 스케일링에 영향을 주는 것은 정규화 옵션 2뿐입니다.

예제 1: 상위 10개 순위 지정 결과

```sql
SELECT title, ts_rank_cd(textsearch, query) AS rank
FROM apod, to_tsquery('neutrino|(dark & matter)') query
WHERE query @@ textsearch
ORDER BY rank DESC
LIMIT 10;

                     title                     |   rank
-----------------------------------------------+----------
 Neutrinos in the Sun                          |      3.1
 The Sudbury Neutrino Detector                 |      2.4
 A MACHO View of Galactic Dark Matter          |  2.01317
 Hot Gas and Dark Matter                       |  1.91171
 The Virgo Cluster: Hot Plasma and Dark Matter |  1.90953
 Rafting for Solar Neutrinos                   |      1.9
 NGC 4650A: Strange Galaxy and Dark Matter     |  1.85774
 Hot Gas and Dark Matter                       |   1.6123
 Ice Fishing for Cosmic Neutrinos              |      1.6
 Weak Lensing Distorts the Universe            | 0.818218
```

예제 2: 정규화된 순위 (rank/(rank+1))

```sql
SELECT title, ts_rank_cd(textsearch, query, 32 /* rank/(rank+1) */ ) AS rank
FROM apod, to_tsquery('neutrino|(dark & matter)') query
WHERE query @@ textsearch
ORDER BY rank DESC
LIMIT 10;

                     title                     |        rank
-----------------------------------------------+-------------------
 Neutrinos in the Sun                          | 0.756097569485493
 The Sudbury Neutrino Detector                 | 0.705882361190954
 A MACHO View of Galactic Dark Matter          | 0.668123210574724
 Hot Gas and Dark Matter                       |  0.65655958650282
 The Virgo Cluster: Hot Plasma and Dark Matter | 0.656301290640973
 Rafting for Solar Neutrinos                   | 0.655172410958162
 NGC 4650A: Strange Galaxy and Dark Matter     | 0.650072921219637
 Hot Gas and Dark Matter                       | 0.617195790024749
 Ice Fishing for Cosmic Neutrinos              | 0.615384618911517
 Weak Lensing Distorts the Universe            | 0.450010798361481
```

> 성능 주의: 순위 지정은 각 일치하는 문서의 `tsvector`를 검색해야 하므로 비용이 많이 들 수 있습니다. 이것은 일치하는 문서가 많은 경우 종종 "병목 현상"이 됩니다. 불행히도, 이것을 피하기가 거의 불가능합니다. 순위 지정은 일치 정보에 접근해야 하기 때문입니다.

---

#### 12.3.4 결과 강조

검색 결과를 표시할 때는 문서의 일부를 보여주되 쿼리와 관련된 부분을 강조하는 것이 이상적입니다. PostgreSQL은 이 기능을 구현하는 `ts_headline` 함수를 제공합니다.

```sql
ts_headline([ config regconfig, ] document text, query tsquery [, options text ]) returns text
```

`ts_headline`은 쿼리와 일치하는 것을 강조하여 문서의 발췌를 반환합니다.

옵션:

| 옵션 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| `MaxWords` | 정수 | 최대 헤드라인 길이 | 35 |
| `MinWords` | 정수 | 최소 헤드라인 길이 | 15 |
| `ShortWord` | 정수 | 시작/끝에서 제거할 단어 길이 | 3 |
| `HighlightAll` | 불린 | 전체 문서를 헤드라인으로 사용 | false |
| `MaxFragments` | 정수 | 최대 텍스트 조각 수 (0=비조각 모드) | 0 |
| `StartSel` | 문자열 | 쿼리 단어 구분 시작 문자열 | `<b>` |
| `StopSel` | 문자열 | 쿼리 단어 구분 종료 문자열 | `</b>` |
| `FragmentDelimiter` | 문자열 | 조각 구분자 | ` ... ` |

두 가지 강조 모드가 있습니다:

- 비조각 모드 (MaxFragments=0): 단일 일치 항목을 선택하고 해당 항목을 중심으로 발췌를 표시합니다
- 조각 모드 (MaxFragments>0): 여러 일치 항목을 찾아 각각을 별도의 조각으로 표시합니다

> 경고 (Cross-site Scripting 안전): `ts_headline` 출력은 웹 페이지에 직접 포함되기에 안전하지 않습니다. 원본 문서의 HTML 마크업이 포함될 수 있기 때문입니다. HTML sanitizer를 사용하거나 HTML 마크업을 모두 제거해야 합니다.

예제 1: 기본 강조

```sql
SELECT ts_headline('english',
  'The most common type of search
is to find all documents containing given query terms
and return them in order of their similarity to the
query.',
  to_tsquery('english', 'query & similarity'));

                        ts_headline
------------------------------------------------------------
 containing given <b>query</b> terms
 and return them in order of their <b>similarity</b> to the
 <b>query</b>.
```

예제 2: 커스텀 옵션

```sql
SELECT ts_headline('english',
  'Search terms may occur
many times in a document,
requiring ranking of the search matches to decide which
occurrences to display in the result.',
  to_tsquery('english', 'search & term'),
  'MaxFragments=10, MaxWords=7, MinWords=3, StartSel=<<, StopSel=>>');

                        ts_headline
------------------------------------------------------------
 <<Search>> <<terms>> may occur
 many times ... ranking of the <<search>> matches to decide
```

> 성능 주의: `ts_headline`은 `tsvector` 요약이 아닌 원본 문서를 사용하므로 느릴 수 있으며 신중하게 사용해야 합니다. 대부분의 전문 검색 애플리케이션은 다음과 같이 구성됩니다: (1) 검색 쿼리 실행, (2) 상위 N개의 일치 문서를 가져옴, (3) 이 N개 문서에 대해서만 `ts_headline` 호출.

---

### 12.4 추가 기능

이 섹션에서는 문서와 쿼리 조작, 자동 업데이트 트리거, 문서 통계 수집에 유용한 추가 함수와 연산자를 설명합니다.

---

#### 12.4.1 문서 조작

12.3.1절에서 원시 텍스트 문서가 `tsvector`로 어떻게 변환되는지 보여주었습니다. PostgreSQL은 또한 이미 `tsvector` 형태인 문서를 조작하는 함수와 연산자를 제공합니다.

##### tsvector 연결 연산자 (`||`)

```sql
tsvector || tsvector
```

`tsvector` 연결 연산자는 두 `tsvector`의 렉심과 위치 정보를 합친 벡터를 반환합니다. 오른쪽 벡터의 위치는 왼쪽 벡터의 최대 위치만큼 오프셋됩니다. 가중치 레이블도 유지됩니다. 이 연산자와 동등한 함수는 `tsvector_concat`입니다.

연결의 중요한 용도 중 하나는 문서의 다른 부분을 다른 가중치로 표시하는 것입니다. 예를 들어, 제목 단어에 본문 단어보다 더 높은 가중치를 부여할 수 있습니다:

```sql
UPDATE tt SET ti =
    setweight(to_tsvector(coalesce(title,'')), 'A')    ||
    setweight(to_tsvector(coalesce(keyword,'')), 'B')  ||
    setweight(to_tsvector(coalesce(abstract,'')), 'C') ||
    setweight(to_tsvector(coalesce(body,'')), 'D');
```

##### setweight() 함수

```sql
setweight(vector tsvector, weight "char") returns tsvector
```

입력 벡터의 모든 위치에 가중치(A, B, C, D)를 할당합니다. D는 기본값이며 출력에 표시되지 않습니다.

> 참고: 가중치는 렉심이 아닌 위치 에 적용됩니다.

##### length() 함수

```sql
length(vector tsvector) returns integer
```

벡터에 저장된 렉심의 개수를 반환합니다.

##### strip() 함수

```sql
strip(vector tsvector) returns tsvector
```

위치 및 가중치 정보 없이 렉심 목록을 반환합니다. 이렇게 하면 크기가 더 작아지지만 관련성 순위 기능이 약해집니다.

> 참고: `<->` (FOLLOWED BY) `tsquery` 연산자는 제거된 `tsvector` 인자와 절대 일치하지 않습니다. 일치 시 렉심 위치가 필요하기 때문입니다.

---

#### 12.4.2 쿼리 조작

12.3.2절에서 원시 텍스트 쿼리가 `tsquery` 값으로 어떻게 변환되는지 보여주었습니다. PostgreSQL은 또한 이미 `tsquery` 형태인 쿼리를 조작하는 함수와 연산자를 제공합니다.

##### AND 연결 (`&&`)

```sql
tsquery && tsquery
```

두 쿼리의 AND 조합을 반환합니다.

##### OR 연결 (`||`)

```sql
tsquery || tsquery
```

두 쿼리의 OR 조합을 반환합니다.

##### NOT 연산 (`!!`)

```sql
!! tsquery
```

쿼리의 부정(NOT)을 반환합니다.

##### FOLLOWED BY 연산 (`<->`)

```sql
tsquery <-> tsquery
```

첫 번째 쿼리 다음에 두 번째 쿼리가 바로 따라오는 경우를 검색하는 쿼리를 반환합니다.

```sql
SELECT to_tsquery('fat') <-> to_tsquery('cat | rat');
          ?column?
----------------------------
 'fat' <-> ( 'cat' | 'rat' )
```

##### tsquery_phrase() 함수

```sql
tsquery_phrase(query1 tsquery, query2 tsquery [, distance integer]) returns tsquery
```

정확히 `distance` 렉심 거리에 있는 두 쿼리의 일치를 검색하는 쿼리를 만듭니다. 예를 들어:

```sql
SELECT tsquery_phrase(to_tsquery('fat'), to_tsquery('cat'), 10);
  tsquery_phrase
------------------
 'fat' <10> 'cat'
```

##### numnode() 함수

```sql
numnode(query tsquery) returns integer
```

`tsquery`의 노드(렉심 + 연산자) 개수를 반환합니다. 이 함수는 쿼리가 의미 있는지 확인하는 데 유용합니다(0을 반환하면 불용어만 포함):

```sql
SELECT numnode(plainto_tsquery('the any'));
NOTICE:  query contains only stopword(s) or doesn't contain lexeme(s), ignored
 numnode
---------
       0

SELECT numnode('foo & bar'::tsquery);
 numnode
---------
       3
```

##### querytree() 함수

```sql
querytree(query tsquery) returns text
```

인덱스 검색에 사용할 수 있는 `tsquery` 부분을 반환합니다. 이 함수는 검색할 수 없는 쿼리(예: 불용어만 포함하거나 부정만 포함)를 감지하는 데 유용합니다:

```sql
SELECT querytree(to_tsquery('defined'));
 querytree
-----------
 'defin'

SELECT querytree(to_tsquery('!defined'));
 querytree
-----------
 T
```

---

##### 12.4.2.1 쿼리 재작성

`ts_rewrite` 함수군은 `tsquery` 내에서 대상 부분쿼리가 나타나는 모든 위치를 대체 부분쿼리로 바꿉니다. 이 연산은 본질적으로 문자열의 부분 문자열 치환과 유사하지만, 검색 대상이 텍스트가 아닌 쿼리 구조입니다. 대상과 대체의 조합을 재작성 규칙이라 하며, 이러한 규칙의 모음은 강력한 검색 도구가 될 수 있습니다. 예를 들어, `supernovae`를 `supernovae|sn`으로 확장하거나, 원래 쿼리를 직접 수정하지 않고 규칙을 통해 검색 범위를 조정하는 데 사용할 수 있습니다.

###### ts_rewrite() - 단일 규칙

```sql
ts_rewrite(query tsquery, target tsquery, substitute tsquery) returns tsquery
```

이 `ts_rewrite` 형식은 단순히 단일 재작성 규칙을 적용합니다: `query`에서 `target`이 나타나는 곳마다 `substitute`로 대체됩니다. 예를 들어:

```sql
SELECT ts_rewrite('a & b'::tsquery, 'a'::tsquery, 'c'::tsquery);
 ts_rewrite
------------
 'b' & 'c'
```

###### ts_rewrite() - 테이블 기반

```sql
ts_rewrite(query tsquery, select text) returns tsquery
```

이 `ts_rewrite` 형식은 텍스트 문자열로 제공된 `SELECT` 명령의 결과에서 재작성 규칙 집합을 가져와서 적용합니다. `SELECT`는 두 개의 `tsquery` 타입 열을 반환해야 합니다. 각 행에서 첫 번째 열 값(대상)이 현재 쿼리 값에서 두 번째 열 값(대체)으로 대체됩니다. 예를 들어:

```sql
CREATE TABLE aliases (t tsquery PRIMARY KEY, s tsquery);
INSERT INTO aliases VALUES('a', 'c');

SELECT ts_rewrite('a & b'::tsquery, 'SELECT t,s FROM aliases');
 ts_rewrite
------------
 'b' & 'c'
```

###### 실제 예제 - 천문학 관련

천문학 데이터의 실제 쿼리 재작성 예:

```sql
CREATE TABLE aliases (t tsquery primary key, s tsquery);
INSERT INTO aliases VALUES(to_tsquery('supernovae'), to_tsquery('supernovae|sn'));

SELECT ts_rewrite(to_tsquery('supernovae & crab'), 'SELECT * FROM aliases');
           ts_rewrite
---------------------------------
 'crab' & ( 'supernova' | 'sn' )
```

규칙 업데이트:

```sql
UPDATE aliases
SET s = to_tsquery('supernovae|sn & !nebulae')
WHERE t = to_tsquery('supernovae');

SELECT ts_rewrite(to_tsquery('supernovae & crab'), 'SELECT * FROM aliases');
                 ts_rewrite
---------------------------------------------
 'crab' & ( 'supernova' | 'sn' & !'nebula' )
```

###### 성능 최적화 - 포함 연산자 사용

재작성 규칙이 많으면 각 규칙 확인이 느려질 수 있습니다. 가능한 후보를 사전 필터링하기 위해 포함 연산자를 사용할 수 있습니다. 예를 들어:

```sql
SELECT ts_rewrite('a & b'::tsquery,
                  'SELECT t,s FROM aliases WHERE ''a & b''::tsquery @> t');
 ts_rewrite
------------
 'b' & 'c'
```

---

#### 12.4.3 자동 업데이트 트리거

> 참고: 이 방법은 저장된 생성 열(stored generated columns) 사용으로 대체되었습니다(12.2.2절 참조).

별도의 열에 문서의 `tsvector` 표현을 저장할 경우, 문서 내용 열이 변경될 때마다 `tsvector` 열을 업데이트해야 합니다. PostgreSQL은 이를 자동으로 처리하는 두 가지 내장 트리거 함수를 제공합니다.

```sql
tsvector_update_trigger(tsvector_column_name, config_name, text_column_name [, ... ])
tsvector_update_trigger_column(tsvector_column_name, config_column_name, text_column_name [, ... ])
```

예제:

```sql
CREATE TABLE messages (
    title       text,
    body        text,
    tsv         tsvector
);

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON messages FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(tsv, 'pg_catalog.english', title, body);

INSERT INTO messages VALUES('title here', 'the body text is here');

SELECT * FROM messages;
   title    |         body          |            tsv
------------+-----------------------+----------------------------
 title here | the body text is here | 'bodi':4 'text':5 'titl':1

SELECT title, body FROM messages WHERE tsv @@ to_tsquery('title & body');
   title    |         body
------------+-----------------------
 title here | the body text is here
```

###### 커스텀 트리거 - PL/pgSQL 예제

문서 부분에 다른 가중치를 설정하거나 `tsvector`를 수정해야 하는 경우, 사용자 정의 트리거 함수를 작성해야 합니다. 다음은 PL/pgSQL 예제입니다:

```sql
CREATE FUNCTION messages_trigger() RETURNS trigger AS $$
begin
  new.tsv :=
     setweight(to_tsvector('pg_catalog.english', coalesce(new.title,'')), 'A') ||
     setweight(to_tsvector('pg_catalog.english', coalesce(new.body,'')), 'D');
  return new;
end
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
    ON messages FOR EACH ROW EXECUTE FUNCTION messages_trigger();
```

> 중요: 내장 트리거든 사용자 정의 트리거든, 쿼리에서 스키마 한정 구성 이름을 지정해야 합니다. 그렇지 않으면 텍스트 검색 함수가 `search_path`의 변경으로 잘못된 구성을 사용할 수 있습니다.

---

#### 12.4.4 문서 통계 수집

`ts_stat` 함수는 구성 테스트와 더 빠른 검색을 위한 불용어 후보 찾기에 유용합니다.

```sql
ts_stat(sqlquery text, [weights text,]
        OUT word text, OUT ndoc integer,
        OUT nentry integer) returns setof record
```

`sqlquery`는 단일 `tsvector` 열을 반환하는 SQL 쿼리의 텍스트 값입니다. `ts_stat`는 해당 쿼리를 실행하고 `tsvector` 데이터에 포함된 각 고유 렉심(단어)에 대한 통계를 반환합니다.

반환 열:

| 열 | 타입 | 설명 |
|----|------|------|
| `word` | text | 렉심 값 |
| `ndoc` | integer | 단어가 나타난 문서 수 |
| `nentry` | integer | 단어의 총 발생 횟수 |

`weights`가 제공되면 지정된 가중치를 가진 발생만 계산됩니다.

예제 1: 가장 빈번한 10개 단어

```sql
SELECT * FROM ts_stat('SELECT vector FROM apod')
ORDER BY nentry DESC, ndoc DESC, word
LIMIT 10;
```

예제 2: 가중치 A 또는 B만 계산

```sql
SELECT * FROM ts_stat('SELECT vector FROM apod', 'ab')
ORDER BY nentry DESC, ndoc DESC, word
LIMIT 10;
```

---

### 12.5 파서

텍스트 검색 파서는 원시 문서 텍스트를 토큰으로 분할하고 각 토큰의 타입을 식별하는 역할을 합니다. 가능한 타입 집합은 파서 자체에 의해 정의됩니다. 파서는 텍스트를 수정하지 않으며, 단순히 단어 경계를 식별합니다. 이처럼 역할이 제한적이기 때문에, 애플리케이션별 사용자 정의 파서의 필요성은 사용자 정의 사전보다 적습니다. 현재 PostgreSQL은 다양한 목적에 적합한 것으로 입증된 단일 내장 파서만 제공합니다.

내장 파서의 이름은 `pg_catalog.default`입니다. 이 파서는 23가지 토큰 타입을 인식합니다:

| 별칭 | 설명 | 예제 |
|------|------|------|
| `asciiword` | ASCII 문자만으로 구성된 단어 | `elephant` |
| `word` | 모든 문자로 구성된 단어 | `mañana` |
| `numword` | 문자와 숫자로 구성된 단어 | `beta1` |
| `asciihword` | 하이픈이 있는 ASCII 단어 | `up-to-date` |
| `hword` | 하이픈이 있는 문자 단어 | `lógico-matemática` |
| `numhword` | 하이픈이 있는 문자+숫자 단어 | `postgresql-beta1` |
| `hword_asciipart` | 하이픈 단어 부분(ASCII) | `postgresql` (in `postgresql-beta1`) |
| `hword_part` | 하이픈 단어 부분(문자) | `lógico` (in `lógico-matemática`) |
| `hword_numpart` | 하이픈 단어 부분(문자+숫자) | `beta1` (in `postgresql-beta1`) |
| `email` | 이메일 주소 | `foo@example.com` |
| `protocol` | 프로토콜 헤드 | `http://` |
| `url` | URL | `example.com/stuff/index.html` |
| `host` | 호스트 | `example.com` |
| `url_path` | URL 경로 | `/stuff/index.html` |
| `file` | 파일 또는 경로명 | `/usr/local/foo.txt` |
| `sfloat` | 과학적 표기법 | `-1.234e56` |
| `float` | 십진 표기법 | `-1.234` |
| `int` | 부호 있는 정수 | `-1234` |
| `uint` | 부호 없는 정수 | `1234` |
| `version` | 버전 번호 | `8.3.0` |
| `tag` | XML 태그 | `<a href="dictionaries.html">` |
| `entity` | XML 엔티티 | `&amp;` |
| `blank` | 공백 기호 | (공백 또는 인식되지 않은 기호) |

#### 주요 특징 및 제한사항

##### 1. 로케일(Locale) 의존성

"문자"의 정의는 데이터베이스의 로케일 설정, 특히 `lc_ctype`에 의해 결정됩니다. ASCII 문자만 포함하는 단어는 별도의 토큰 타입으로 보고됩니다. 대부분의 유럽 언어에서는 `word`와 `asciiword` 토큰 타입을 동일하게 처리해야 합니다.

##### 2. 이메일 제한사항

RFC 5322의 모든 유효한 이메일 문자를 지원하지 않습니다. 사용자명에서 지원되는 비영숫자 문자: 마침표(.), 대시(-), 언더스코어(_)만 지원합니다.

##### 3. 겹치는 토큰

파서는 동일한 텍스트에서 여러 개의 겹치는 토큰을 생성할 수 있습니다. 예를 들어, 하이픈이 있는 단어는 전체 단어와 각 구성 요소 모두 보고됩니다:

```sql
SELECT alias, description, token FROM ts_debug('foo-bar-beta1');
      alias      |               description                |     token
-----------------+------------------------------------------+---------------
 numhword        | Hyphenated word, letters and digits      | foo-bar-beta1
 hword_asciipart | Hyphenated word part, all ASCII          | foo
 blank           | Space symbols                            | -
 hword_asciipart | Hyphenated word part, all ASCII          | bar
 blank           | Space symbols                            | -
 hword_numpart   | Hyphenated word part, letters and digits | beta1
```

이 동작 덕분에 전체 복합어(`foo-bar-beta1`)와 각 구성 요소(`foo`, `bar`, `beta1`) 모두에 대해 검색할 수 있습니다.

URL도 유사하게 처리됩니다:

```sql
SELECT alias, description, token FROM ts_debug('http://example.com/stuff/index.html');
  alias   |  description  |            token
----------+---------------+------------------------------
 protocol | Protocol head | http://
 url      | URL           | example.com/stuff/index.html
 host     | Host          | example.com
 url_path | URL path      | /stuff/index.html
```

---

### 12.6 사전

사전은 전문 검색에서 두 가지 주요 역할을 합니다:

1. 불용어 제거: 검색에서 무시해야 할 단어(너무 흔해서 검색에 쓸모없는 단어) 제거
2. 단어 정규화: 같은 단어의 다양한 형태를 하나의 렉심으로 통합

정규화와 불용어 제거를 통해 `tsvector` 표현의 크기가 줄어들어 성능이 개선됩니다.

#### 정규화의 예시

정규화가 반드시 언어학적 작업을 의미하는 것은 아닙니다:

- 언어학적 정규화: Ispell 사전으로 단어를 정규형으로 축소하거나 Stemmer로 어미 제거
- URL 정규화: 동등한 URL들을 하나로 정규화
- 색상명 정규화: 색상 이름을 16진수 값으로 변환
- 숫자 정규화: 소수점 자릿수 제한

#### 사전의 동작 원리

사전은 입력 토큰에 대해 다음 중 하나를 반환합니다:

| 반환 값 | 설명 |
|---------|------|
| 렉심 배열 | 토큰이 사전에서 인식된 경우 (하나의 토큰이 여러 렉심을 생성할 수 있음) |
| TSL_FILTER 플래그가 있는 단일 렉심 | 토큰을 변경하여 다음 사전으로 전달 (필터링 사전) |
| 빈 배열 | 토큰이 불용어인 경우 |
| NULL | 토큰이 인식되지 않은 경우 |

#### 사전 구성의 일반 규칙

좁고 구체적인 순서에서 일반적인 순서로 배열합니다:

```sql
ALTER TEXT SEARCH CONFIGURATION astro_en
    ADD MAPPING FOR asciiword WITH astrosyn, english_ispell, english_stem;
```

이 예에서:
- `astrosyn`: 천문학 동의어 사전 (가장 구체적)
- `english_ispell`: 일반 영어 사전
- `english_stem`: Snowball Stemmer (가장 일반적)

---

#### 12.6.1 불용어

불용어는 거의 모든 문서에 나타나는 매우 흔한 단어로, 검색 가치가 낮아 무시됩니다. 영어의 `a`, `the`가 대표적인 예입니다. 언어마다 불용어 목록이 다르며, 전문 검색 애플리케이션에서 이 목록을 조정해야 할 수도 있습니다.

불용어와 위치의 관계:

불용어는 `tsvector`의 위치에 영향을 줍니다:

```sql
SELECT to_tsvector('english', 'in the list of stop words');
        to_tsvector
----------------------------
 'list':3 'stop':5 'word':6
```

위치 1, 2, 4가 누락되었습니다(불용어: `in`, `the`, `of`). `list`는 위치 3, `stop`은 위치 5, `word`는 위치 6입니다.

불용어의 순위 영향:

```sql
-- 불용어 포함
SELECT ts_rank_cd (to_tsvector('english', 'in the list of stop words'),
                    to_tsquery('list & stop'));
 ts_rank_cd
------------
       0.05

-- 불용어 제외
SELECT ts_rank_cd (to_tsvector('english', 'list stop words'),
                    to_tsquery('list & stop'));
 ts_rank_cd
------------
        0.1
```

---

#### 12.6.2 Simple 사전

`simple` 사전 템플릿은 입력 토큰을 소문자로 변환하고 불용어 파일에서 확인합니다. 불용어 목록에 있으면 빈 배열을 반환하여 토큰이 삭제됩니다. 그렇지 않으면 소문자 형태의 단어가 정규화된 렉심으로 반환됩니다.

예제: Simple 사전 정의

```sql
CREATE TEXT SEARCH DICTIONARY public.simple_dict (
    TEMPLATE = pg_catalog.simple,
    STOPWORDS = english
);
```

`english`는 불용어 파일의 기본명입니다. 전체 경로는 `$SHAREDIR/tsearch_data/english.stop`입니다. 파일 형식은 한 줄에 하나의 단어입니다.

동작 테스트:

```sql
-- 불용어가 아닌 단어
SELECT ts_lexize('public.simple_dict', 'YeS');
 ts_lexize
-----------
 {yes}

-- 불용어
SELECT ts_lexize('public.simple_dict', 'The');
 ts_lexize
-----------
 {}
```

##### Accept 파라미터

`Accept` 파라미터의 기본값은 `true`입니다. `Accept = false`로 설정하면:

```sql
ALTER TEXT SEARCH DICTIONARY public.simple_dict ( Accept = false );

-- 불용어가 아닌 단어 -> NULL 반환
SELECT ts_lexize('public.simple_dict', 'YeS');
 ts_lexize
-----------


-- 불용어 -> 빈 배열 반환
SELECT ts_lexize('public.simple_dict', 'The');
 ts_lexize
-----------
 {}
```

| Accept 값 | 위치 | 이유 |
|----------|------|------|
| `true` (기본값) | 목록 끝 | 다음 사전으로 토큰을 전달하지 않음 |
| `false` | 중간 | 인식하지 못한 토큰을 다음 사전으로 전달 |

> 참고: 모든 사전 구성 파일은 UTF-8 인코딩 이어야 합니다. 서버가 다른 인코딩을 사용하면 자동으로 변환됩니다.

---

#### 12.6.3 Synonym 사전

동의어 사전은 단어를 동의어로 대체하는 데 사용됩니다. 구문(phrases)은 지원하지 않으며, 구문 치환이 필요한 경우 Thesaurus 템플릿을 사용해야 합니다.

사용 사례:

언어 문제 해결 예: "Paris"가 어간 추출기에 의해 "pari"로 축소되는 것을 방지:

```sql
-- Before: English stemmer가 "Paris"를 "pari"로 축소
SELECT * FROM ts_debug('english', 'Paris');
   alias   |   description   | token |  dictionaries  |  dictionary  | lexemes
-----------+-----------------+-------+----------------+--------------+---------
 asciiword | Word, all ASCII | Paris | {english_stem} | english_stem | {pari}

-- 사전 생성
CREATE TEXT SEARCH DICTIONARY my_synonym (
    TEMPLATE = synonym,
    SYNONYMS = my_synonyms
);

-- 구성 수정
ALTER TEXT SEARCH CONFIGURATION english
    ALTER MAPPING FOR asciiword
    WITH my_synonym, english_stem;

-- After: Synonym 사전이 먼저 처리하여 "paris" 반환
SELECT * FROM ts_debug('english', 'Paris');
   alias   |   description   | token |       dictionaries        | dictionary | lexemes
-----------+-----------------+-------+---------------------------+------------+---------
 asciiword | Word, all ASCII | Paris | {my_synonym,english_stem} | my_synonym | {paris}
```

##### 구성 파일 형식

```
word1 synonym1
word2 synonym2
...
```

한 줄에 하나의 치환 규칙, 공백으로 단어와 동의어를 구분합니다.

##### 접두사 표시

구성 파일의 동의어 끝에 `*`를 붙이면 접두사로 표시됩니다:

```
indices index*
```

```sql
-- ts_lexize 사용
SELECT ts_lexize('syn', 'indices');
 ts_lexize
-----------
 {index}

-- to_tsquery 사용 (접두사 매치 마커 포함)
SELECT to_tsquery('tst', 'indices');
 to_tsquery
------------
 'index':*
```

---

#### 12.6.4 Thesaurus 사전

Thesaurus 사전(TZ)은 단어와 구문의 관계 정보(BT: 광의어, NT: 협의어 등)를 포함하는 단어 집합입니다. 비선호 용어를 선호 용어로 대체하며, 선택적으로 원본 용어도 함께 인덱싱할 수 있습니다.

특징:

- Synonym 사전의 확장 버전
- 구문(phrase) 지원 (가장 큰 차이점)

##### 구성 파일 형식

```
# 주석
sample word(s) : indexed word(s)
more sample word(s) : more indexed word(s)
...
```

콜론 `:`이 구문과 치환어를 구분합니다.

##### Subdictionary

Thesaurus 사전은 입력 텍스트를 정규화하기 위해 subdictionary 를 사용합니다. 모든 샘플 단어는 subdictionary에 알려져 있어야 합니다.

아스테리스크를 사용하여 불용어가 나올 수 있는 위치를 지정할 수 있습니다:

```
? one ? two : swsw
```

`a one the two`와 `the one a two` 모두 `swsw`로 치환됩니다.

> 중요: Thesaurus는 인덱싱 중에 사용되므로, 파라미터 변경 시 재인덱싱이 필요 합니다!

##### 12.6.4.1 Thesaurus 구성

사전 정의:

```sql
CREATE TEXT SEARCH DICTIONARY thesaurus_simple (
    TEMPLATE = thesaurus,
    DictFile = mythesaurus,
    Dictionary = pg_catalog.english_stem
);
```

파라미터:

| 파라미터 | 설명 |
|---------|------|
| `DictFile` | Thesaurus 구성 파일 기본명 (전체 경로: `$SHAREDIR/tsearch_data/mythesaurus.ths`) |
| `Dictionary` | Subdictionary (여기선 Snowball English stemmer) |

구성에 바인딩:

```sql
ALTER TEXT SEARCH CONFIGURATION russian
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart
    WITH thesaurus_simple;
```

##### 12.6.4.2 Thesaurus 예제

천문학 Thesaurus 설정:

1단계: Thesaurus 파일 (`thesaurus_astro`)

```
supernovae stars : sn
crab nebulae : crab
```

2단계: 사전 생성

```sql
CREATE TEXT SEARCH DICTIONARY thesaurus_astro (
    TEMPLATE = thesaurus,
    DictFile = thesaurus_astro,
    Dictionary = english_stem
);
```

3단계: 구성 수정

```sql
ALTER TEXT SEARCH CONFIGURATION russian
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart
    WITH thesaurus_astro, english_stem;
```

동작 확인:

```sql
SELECT plainto_tsquery('supernova star');
 plainto_tsquery
-----------------
 'sn'
```

원본 구문도 인덱싱하려면:

```
supernovae stars : sn supernovae stars
```

```sql
SELECT plainto_tsquery('supernova star');
       plainto_tsquery
-----------------------------
 'sn' & 'supernova' & 'star'
```

---

#### 12.6.5 Ispell 사전

Ispell 사전 템플릿은 형태소 분석을 지원하며, 단어의 다양한 언어학적 형태를 동일한 렉심으로 정규화할 수 있습니다.

예: English Ispell 사전

단어 `bank`의 모든 형태를 매칭: `banking`, `banked`, `banks`, `banks'`, `bank's`

##### Ispell 사전 생성 절차

1단계: 사전 파일 다운로드

OpenOffice 확장 형식 `.oxt` 파일에서 `.aff` (affixes)와 `.dic` (dictionary) 파일을 추출합니다.

2단계: 파일 인코딩 변환

```bash
iconv -f ISO_8859-1 -t UTF-8 -o nn_no.affix nn_NO.aff
iconv -f ISO_8859-1 -t UTF-8 -o nn_no.dict nn_NO.dic
```

3단계: 파일 복사

```bash
cp nn_no.affix $SHAREDIR/tsearch_data/
cp nn_no.dict $SHAREDIR/tsearch_data/
```

4단계: PostgreSQL에 로드

```sql
CREATE TEXT SEARCH DICTIONARY english_hunspell (
    TEMPLATE = ispell,
    DictFile = en_us,
    AffFile = en_us,
    Stopwords = english
);
```

| 파라미터 | 설명 |
|---------|------|
| `DictFile` | Dictionary 파일 기본명 |
| `AffFile` | Affixes 파일 기본명 |
| `Stopwords` | 불용어 파일 기본명 |

##### 복합어 지원

Ispell은 복합어 분해를 지원합니다:

```sql
SELECT ts_lexize('norwegian_ispell', 'overbuljongterningpakkmesterassistent');
   {over,buljong,terning,pakk,mester,assistent}

SELECT ts_lexize('norwegian_ispell', 'sjokoladefabrikk');
   {sjokoladefabrikk,sjokolade,fabrikk}
```

> 권장사항: Ispell 사전은 제한된 단어 집합만 인식하므로, 더 광범위한 Snowball 사전으로 보완하는 것이 좋습니다.

---

#### 12.6.6 Snowball 사전

Snowball 프로젝트는 Martin Porter(Porter's Stemming Algorithm 발명자)가 개발했으며, 다양한 언어의 어간 추출 알고리즘을 제공합니다.

참고: [Snowball 공식 사이트](https://snowballstem.org/)

##### Snowball 사전 생성

```sql
CREATE TEXT SEARCH DICTIONARY english_stem (
    TEMPLATE = snowball,
    Language = english,
    StopWords = english
);
```

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `Language` | 예 | Stemmer 언어 선택 |
| `StopWords` | 아니오 | 불용어 파일명 |

##### 핵심 특징

Snowball 사전은 모든 단어를 인식 합니다(단어를 단순화할 수 있는지 여부와 관계없이).

> 중요: Snowball 사전은 항상 목록의 끝에 배치 해야 합니다. 앞에 배치하면 뒤의 사전에 토큰이 전달되지 않습니다.

예제:

```sql
SELECT to_tsvector('english', 'running runs runner');
 to_tsvector
--------------
 'run':1,2,3
```

---

#### 사전 유형 요약

| 사전 유형 | 용도 | 위치 | 특징 |
|-----------|------|------|------|
| Simple | 불용어 제거 | 마지막 | 가장 간단 |
| Synonym | 동의어 치환 | 중간 | 구문 미지원 |
| Thesaurus | 구문 기반 치환 | 중간 | 구문 지원, 재인덱싱 필요 |
| Ispell | 형태소 분석 | 중간 | 제한된 단어 집합 |
| Snowball | 어간 추출 | 마지막 | 모든 단어 인식 |

---

### 12.7 구성 예제

텍스트 검색 구성(Text Search Configuration)은 문서를 `tsvector`로 변환하는 데 필요한 모든 옵션을 지정합니다.

#### 주요 구성 요소

- 파서(Parser): 텍스트를 토큰으로 분할
- 사전(Dictionaries): 각 토큰을 렉심으로 변환

`to_tsvector` 또는 `to_tsquery` 호출 시 구성이 필요합니다. 기본 구성은 `default_text_search_config` 파라미터로 지정됩니다.

#### 실습 예제: 'pg' 구성 생성

1단계: 기본 구성 복제

```sql
CREATE TEXT SEARCH CONFIGURATION public.pg ( COPY = pg_catalog.english );
```

2단계: 동의어 사전 생성

파일 위치: `$SHAREDIR/tsearch_data/pg_dict.syn`

```
postgres    pg
pgsql       pg
postgresql  pg
```

동의어 사전 정의:

```sql
CREATE TEXT SEARCH DICTIONARY pg_dict (
    TEMPLATE = synonym,
    SYNONYMS = pg_dict
);
```

3단계: Ispell 사전 등록

```sql
CREATE TEXT SEARCH DICTIONARY english_ispell (
    TEMPLATE = ispell,
    DictFile = english,
    AffFile = english,
    StopWords = english
);
```

4단계: 토큰 타입별 매핑 설정

```sql
ALTER TEXT SEARCH CONFIGURATION pg
    ALTER MAPPING FOR asciiword, asciihword, hword_asciipart,
                      word, hword, hword_part
    WITH pg_dict, english_ispell, english_stem;
```

5단계: 특정 토큰 타입 제외

```sql
ALTER TEXT SEARCH CONFIGURATION pg
    DROP MAPPING FOR email, url, url_path, sfloat, float;
```

6단계: 구성 테스트

```sql
SELECT * FROM ts_debug('public.pg', '
PostgreSQL, the highly scalable, SQL compliant, open source object-relational
database management system, is now undergoing beta testing of the next
version of our software.
');
```

7단계: 기본 구성 설정

```sql
SET default_text_search_config = 'public.pg';

SHOW default_text_search_config;
 default_text_search_config
----------------------------
 public.pg
```

---

### 12.8 텍스트 검색 테스트 및 디버깅

사용자 정의 텍스트 검색 구성은 동작이 복잡해질 수 있으므로, PostgreSQL은 텍스트 검색 객체를 테스트하기 위한 유틸리티 함수들을 제공합니다.

---

#### 12.8.1 구성 테스트

##### ts_debug 함수

```sql
ts_debug([ config regconfig, ] document text,
         OUT alias text,
         OUT description text,
         OUT token text,
         OUT dictionaries regdictionary[],
         OUT dictionary regdictionary,
         OUT lexemes text[])
         returns setof record
```

`ts_debug`는 완전한 텍스트 검색 구성을 테스트하여 문서의 모든 토큰에 대한 정보를 표시합니다.

반환 열:

| 열명 | 타입 | 설명 |
|------|------|------|
| `alias` | text | 토큰 타입의 짧은 이름 |
| `description` | text | 토큰 타입 설명 |
| `token` | text | 토큰의 텍스트 |
| `dictionaries` | regdictionary[] | 이 토큰 타입에 대해 선택된 사전들 |
| `dictionary` | regdictionary | 토큰을 인식한 사전, 또는 NULL |
| `lexemes` | text[] | 인식한 사전이 생성한 렉심, NULL 또는 빈 배열 {} (불용어) |

예제:

```sql
SELECT * FROM ts_debug('english', 'a fat  cat sat on a mat - it ate a fat rats');
   alias   |   description   | token |  dictionaries  |  dictionary  | lexemes
-----------+-----------------+-------+----------------+--------------+---------
 asciiword | Word, all ASCII | a     | {english_stem} | english_stem | {}
 blank     | Space symbols   |       | {}             |              |
 asciiword | Word, all ASCII | fat   | {english_stem} | english_stem | {fat}
 blank     | Space symbols   |       | {}             |              |
 asciiword | Word, all ASCII | cat   | {english_stem} | english_stem | {cat}
 ...
```

결과 해석:

- "a" → 빈 배열 {} = 불용어 (인덱싱 안 됨)
- "fat" → {fat}, "rats" → {rat} (어간 추출 적용)
- 공백 → dictionaries가 비어 있음 (제외됨)

---

#### 12.8.2 파서 테스트

##### ts_parse 함수

```sql
ts_parse(parser_name text, document text,
         OUT tokid integer, OUT token text) returns setof record
```

주어진 문서를 파싱하고 각 토큰에 대한 정보를 반환합니다.

```sql
SELECT * FROM ts_parse('default', '123 - a number');
 tokid | token
-------+--------
    22 | 123
    12 |
    12 | -
     1 | a
    12 |
     1 | number
```

##### ts_token_type 함수

```sql
ts_token_type(parser_name text, OUT tokid integer,
              OUT alias text, OUT description text) returns setof record
```

파서가 인식할 수 있는 각 토큰 타입을 설명합니다.

```sql
SELECT * FROM ts_token_type('default');
 tokid |      alias      |               description
-------+-----------------+------------------------------------------
     1 | asciiword       | Word, all ASCII
     2 | word            | Word, all letters
     ...
```

---

#### 12.8.3 사전 테스트

##### ts_lexize 함수

```sql
ts_lexize(dict regdictionary, token text) returns text[]
```

사전이 토큰을 어떻게 처리하는지 테스트합니다.

| 반환 값 | 의미 |
|---------|------|
| 렉심 배열 | 토큰이 사전에서 인식됨 |
| 빈 배열 {} | 토큰이 불용어임 |
| NULL | 토큰이 인식되지 않음 |

```sql
SELECT ts_lexize('english_stem', 'stars');
 ts_lexize
-----------
 {star}

SELECT ts_lexize('english_stem', 'a');
 ts_lexize
-----------
 {}
```

> 중요: `ts_lexize`는 단일 토큰 만 처리합니다. 텍스트 파싱을 하지 않습니다. Thesaurus 사전의 구문 테스트에는 `plainto_tsquery`나 `to_tsvector`를 사용하세요.

---

### 12.9 전문 검색을 위한 선호 인덱스 유형

전문 검색 속도를 높이기 위해 GIN과 GiST 두 가지 인덱스 유형을 사용할 수 있습니다. 인덱스는 필수가 아니지만, 정기적으로 검색되는 열에는 일반적으로 권장됩니다.

#### 인덱스 생성 방법

##### GIN 인덱스

```sql
CREATE INDEX name ON table USING GIN (column);
```

- 특징: GIN (Generalized Inverted Index) 기반
- 열 타입: `tsvector` 필수
- 구조: 각 단어(렉심)마다 인덱스 항목 생성, 압축된 위치 목록 포함

##### GiST 인덱스

```sql
CREATE INDEX name ON table USING GIST (column [ { DEFAULT | tsvector_ops } (siglen = number) ] );
```

- 특징: GiST (Generalized Search Tree) 기반
- 열 타입: `tsvector` 또는 `tsquery` 가능
- 옵션: `siglen` 파라미터로 서명 길이 지정 (기본값: 124바이트, 최대: 2024바이트)

#### GIN vs GiST 비교

| 특성 | GIN | GiST |
|------|-----|------|
| 선호도 | 권장 | 대안 |
| 인덱스 유형 | Inverted index | Tree 기반 |
| 손실성 | Non-lossy (손실 없음) | Lossy (거짓 일치 가능) |
| 저장 내용 | 렉심만 저장, 가중치 레이블 미포함 | 고정 길이 서명 |
| 재확인 | 가중치 쿼리 시에만 | 항상 (거짓 일치 제거 위해) |
| 커버링 인덱스 | 지원 안함 | `INCLUDE` 절 사용 가능 |

#### GIN 인덱스 빌드 시간 개선

```sql
SET maintenance_work_mem = '256MB';  -- 값을 증가시키면 빌드 시간 단축
```

#### 서명 길이(siglen) 상세 설명

GiST 인덱스의 `siglen` 파라미터는 각 문서의 서명 길이를 결정합니다:

- 더 긴 서명: 더 정확한 검색, 더 큰 인덱스 크기
- 더 짧은 서명: 더 많은 거짓 일치, 더 작은 인덱스 크기

#### 대규모 컬렉션 검색 구현

- 분할(Partitioning): 데이터베이스 수준에서 테이블 상속 사용
- 분산 검색: 여러 서버에 문서 분산 후 외부 검색 결과 수집

---

### 12.10 psql 지원

psql은 텍스트 검색 구성 객체에 대한 정보를 얻을 수 있는 명령어 세트를 제공합니다.

#### 기본 문법

```
\dF{d,p,t}[+] [PATTERN]
```

- `+` (선택사항): 더 상세한 정보 표시
- `PATTERN` (선택사항): 텍스트 검색 객체의 이름 (스키마 한정 가능, 정규식 사용 가능)

#### 사용 가능한 명령어

##### 1. `\dF[+] [PATTERN]` - 텍스트 검색 구성 나열

```sql
=> \dF russian
            List of text search configurations
   Schema   |  Name   |            Description
------------+---------+------------------------------------
 pg_catalog | russian | configuration for russian language

=> \dF+ russian
Text search configuration "pg_catalog.russian"
Parser: "pg_catalog.default"
      Token      | Dictionaries
-----------------+--------------
 asciihword      | english_stem
 asciiword       | english_stem
 email           | simple
 ...
```

##### 2. `\dFd[+] [PATTERN]` - 텍스트 검색 사전 나열

```sql
=> \dFd
                             List of text search dictionaries
   Schema   |      Name       |                        Description
------------+-----------------+-----------------------------------------------------------
 pg_catalog | arabic_stem     | snowball stemmer for arabic language
 pg_catalog | english_stem    | snowball stemmer for english language
 pg_catalog | simple          | simple dictionary: just lower case and check for stopword
 ...
```

##### 3. `\dFp[+] [PATTERN]` - 텍스트 검색 파서 나열

```sql
=> \dFp
        List of text search parsers
   Schema   |  Name   |     Description
------------+---------+---------------------
 pg_catalog | default | default word parser

=> \dFp+
    Text search parser "pg_catalog.default"
     Method      |    Function    | Description
-----------------+----------------+-------------
 Start parse     | prsd_start     |
 Get next token  | prsd_nexttoken |
 End parse       | prsd_end       |
 Get headline    | prsd_headline  |
 Get token types | prsd_lextype   |

        Token types for parser "pg_catalog.default"
   Token name    |               Description
-----------------+------------------------------------------
 asciihword      | Hyphenated word, all ASCII
 asciiword       | Word, all ASCII
 blank           | Space symbols
 ...
```

##### 4. `\dFt[+] [PATTERN]` - 텍스트 검색 템플릿 나열

```sql
=> \dFt
                           List of text search templates
   Schema   |   Name    |                        Description
------------+-----------+-----------------------------------------------------------
 pg_catalog | ispell    | ispell dictionary
 pg_catalog | simple    | simple dictionary: just lower case and check for stopword
 pg_catalog | snowball  | snowball stemmer
 pg_catalog | synonym   | synonym dictionary: replace word by its synonym
 pg_catalog | thesaurus | thesaurus dictionary: phrase by phrase substitution
```

---

### 12.11 제한사항

PostgreSQL의 전문 검색 기능에는 다음과 같은 제한사항이 있습니다:

| 제한사항 | 값 |
|---------|-----|
| 렉심 길이 | 2킬로바이트 미만 |
| tsvector 길이 | 1메가바이트 미만 (렉심 + 위치) |
| 렉심 개수 | 2^64 미만 |
| 위치 값 | 0보다 크고 16,383 이하 |
| FOLLOWED BY 연산자 거리 | 16,384 이하 |
| 렉심당 위치 수 | 최대 256개 |
| tsquery 노드 개수 | 32,768 미만 (렉심 + 연산자) |

#### 비교 예시

실제 데이터에서의 통계:

- PostgreSQL 8.1 문서: 고유 단어 10,441개, 전체 단어 335,420개
- PostgreSQL 메일링 리스트 아카이브: 고유 단어 910,989개, 총 렉심 57,491,343개, 메시지 461,020개

이러한 제한사항들은 대부분의 일반적인 전문 검색 사용 사례에서는 문제가 되지 않습니다.

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Full Text Search](https://www.postgresql.org/docs/18/textsearch.html)
- [Snowball 공식 사이트](https://snowballstem.org/)
- [Hunspell](https://hunspell.github.io/)

---

## 제13장. 동시성 제어 (Concurrency Control)

이 장에서는 여러 세션이 동시에 같은 데이터에 접근할 때 PostgreSQL의 동작을 설명합니다. 목표는 모든 세션에 대해 효율적인 접근을 허용하면서 엄격한 데이터 무결성을 유지하는 것입니다.

---

### 13.1. 소개 (Introduction)

PostgreSQL은 동시 데이터 접근을 관리하면서 데이터 일관성을 유지하기 위한 핵심 메커니즘으로 다중 버전 동시성 제어(MVCC, Multiversion Concurrency Control)를 사용합니다.

#### MVCC란?

MVCC는 다중 버전 모델로, 각 SQL 문은 특정 시점에 존재했던 데이터 스냅샷(데이터베이스 버전)을 봅니다. 이는 현재 기반 데이터의 상태와 무관합니다.

- 같은 행을 동시에 수정하는 트랜잭션이 만들어낸 일관성 없는 데이터가 다른 문장에 보이는 것을 방지합니다.
- 각 데이터베이스 세션에 트랜잭션 격리를 제공합니다.

#### 주요 장점

1. 논블로킹 동시성
   - 읽기 잠금과 쓰기 잠금이 충돌하지 않습니다.
   - 읽기가 쓰기를 절대 차단하지 않습니다.
   - 쓰기가 읽기를 절대 차단하지 않습니다.

2. 잠금 경합 최소화
   - 전통적인 잠금 방법론을 회피합니다.
   - 다중 사용자 환경에서 합리적인 성능을 제공합니다.

3. 엄격한 격리 수준
   - PostgreSQL은 가장 엄격한 격리 수준에서도 읽기/쓰기 논블로킹 보장을 유지합니다.
   - 직렬화 가능 스냅샷 격리(SSI, Serializable Snapshot Isolation)를 사용합니다.

#### 대체 잠금 옵션

PostgreSQL은 다음과 같은 애플리케이션을 위해 명시적 잠금 메커니즘도 제공합니다:
- 전체 트랜잭션 격리가 필요하지 않은 경우
- 수동으로 충돌 지점을 관리하고 싶은 경우:
  - 테이블 수준 잠금
  - 행 수준 잠금
  - 권고 잠금(Advisory Lock) - 애플리케이션 정의, 단일 트랜잭션에 묶이지 않음

#### 성능 고려사항

> MVCC를 적절히 사용하면 일반적으로 명시적 잠금보다 더 나은 성능을 제공합니다.

---

### 13.2. 트랜잭션 격리 (Transaction Isolation)

PostgreSQL은 SQL 표준의 네 가지 트랜잭션 격리 수준을 구현하지만, 내부적으로는 세 가지 구분된 수준만 구현됩니다. Read Uncommitted는 Read Committed와 동일하게 동작합니다. 이는 PostgreSQL의 다중 버전 동시성 제어(MVCC) 아키텍처에 맞춰 의도적으로 설계된 것입니다.

#### 트랜잭션 격리 현상

표준은 다양한 격리 수준에서 방지해야 하는 네 가지 현상을 정의합니다:

- 더티 읽기(Dirty Read): 트랜잭션이 커밋되지 않은 동시 트랜잭션이 쓴 데이터를 읽음
- 비반복적 읽기(Nonrepeatable Read): 트랜잭션이 데이터를 다시 읽을 때 다른 커밋된 트랜잭션에 의해 수정되어 있음
- 팬텀 읽기(Phantom Read): 트랜잭션이 쿼리를 다시 실행할 때 다른 트랜잭션으로 인해 결과 집합이 변경됨
- 직렬화 이상(Serialization Anomaly): 커밋된 트랜잭션 그룹이 모든 가능한 순차 실행 순서와 일치하지 않는 결과를 생성함

#### 격리 수준 비교표

| 격리 수준 | 더티 읽기 | 비반복적 읽기 | 팬텀 읽기 | 직렬화 이상 |
|-----------|-----------|---------------|-----------|-------------|
| Read Uncommitted | 허용됨 (PG에서는 아님) | 가능 | 가능 | 가능 |
| Read Committed | 불가능 | 가능 | 가능 | 가능 |
| Repeatable Read | 불가능 | 불가능 | 허용됨 (PG에서는 아님) | 가능 |
| Serializable | 불가능 | 불가능 | 불가능 | 불가능 |

#### 격리 수준 설정

`SET TRANSACTION` 명령을 사용합니다:

```sql
SET TRANSACTION ISOLATION LEVEL { SERIALIZABLE | REPEATABLE READ | READ COMMITTED | READ UNCOMMITTED };
```

---

#### 13.2.1. Read Committed 격리 수준

Read Committed 는 PostgreSQL의 기본 격리 수준입니다.

##### 동작 방식

- `SELECT` 쿼리는 쿼리가 시작되기 전에 커밋된 데이터만 봅니다.
- 커밋되지 않은 데이터나 쿼리 실행 중에 동시 트랜잭션이 커밋한 변경사항은 보지 않습니다.
- 쿼리 시작 시점의 데이터베이스 스냅샷을 사용합니다.
- `SELECT`는 자신의 트랜잭션 내에서 이전 업데이트의 효과를 봅니다.
- 단일 트랜잭션 내의 두 연속 `SELECT` 명령은 다른 트랜잭션이 그 사이에 커밋하면 다른 데이터를 볼 수 있습니다.

##### 명령 동작

`UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR SHARE`:
- 명령 시작 시점에 커밋된 대상 행을 찾습니다.
- 대상 행이 다른 동시 트랜잭션에 의해 업데이트되었으면 대기합니다.
- 첫 번째 업데이터가 롤백하면: 두 번째 업데이터가 원래 발견한 행으로 진행할 수 있습니다.
- 첫 번째 업데이터가 커밋하면: 두 번째 업데이터는 삭제된 행을 무시하거나 업데이트된 버전으로 진행합니다.
- 업데이트된 행에서 검색 조건을 다시 평가합니다.

`INSERT ... ON CONFLICT DO UPDATE`:
- 각 행은 삽입되거나 업데이트됩니다.
- Read Committed 모드에서는 UPDATE 절이 명령에 통상적으로 보이지 않는 행에 영향을 줄 수 있습니다(충돌이 다른 트랜잭션에서 발생한 경우).

`INSERT ... ON CONFLICT DO NOTHING`:
- INSERT 스냅샷에 아직 보이지 않는 다른 트랜잭션의 결과로 인해 삽입이 진행되지 않을 수 있습니다.

`MERGE`:
- INSERT, UPDATE, DELETE의 다양한 조합을 지정할 수 있습니다.
- 행이 동시에 업데이트되었지만 조인 조건이 여전히 통과하면: MERGE는 업데이트된 버전에서 작업을 수행합니다.
- 조인 조건이 실패하면: MERGE는 NOT MATCHED 작업을 평가합니다.
- 행이 삭제되면: MERGE는 NOT MATCHED [BY TARGET] 작업을 평가합니다.
- 조건은 업데이트된 버전의 행에서 다시 평가됩니다.

##### 예제: 계좌 이체

```sql
BEGIN;
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 12345;
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 7534;
COMMIT;
```

각 명령은 영향을 주는 행의 업데이트된 버전을 보며, 불일치를 방지합니다.

##### 문제가 되는 예제

```sql
BEGIN;
UPDATE website SET hits = hits + 1;
-- 다른 세션에서 실행:  DELETE FROM website WHERE hits = 10;
COMMIT;
```

`DELETE`는 아무 효과가 없습니다. 업데이트 전 행 값(9)이 건너뛰어지고, DELETE가 잠금을 획득할 때 새 값은 11(10이 아님)이기 때문입니다. 이는 Read Committed 모드가 단일 명령에 걸쳐 절대적인 일관성을 제공하지 않을 수 있음을 보여줍니다.

##### 요약

- 빠르고 사용하기 간단합니다.
- 많은 애플리케이션에 적합합니다.
- 엄격한 일관성이 필요한 복잡한 쿼리와 업데이트에는 불충분합니다.

---

#### 13.2.2. Repeatable Read 격리 수준

##### 동작 방식

- 트랜잭션이 시작되기 전에 커밋된 데이터만 봅니다.
- 커밋되지 않은 데이터나 트랜잭션 실행 중에 동시 트랜잭션이 커밋한 변경사항을 절대 보지 않습니다.
- 각 쿼리는 자신의 트랜잭션 내에서 이전 업데이트의 효과를 봅니다(아직 커밋되지 않음).
- SQL 표준보다 강한 보장: 더티 읽기, 비반복적 읽기, 팬텀 읽기를 방지합니다(직렬화 이상 제외).

##### Read Committed와의 주요 차이점

- 스냅샷은 각 문장이 아닌 트랜잭션의 첫 번째 비제어 문장 시작 시 고정됩니다.
- 단일 트랜잭션 내에서 연속적인 `SELECT` 명령은 같은 데이터를 봅니다.
- 트랜잭션이 시작된 후 커밋된 다른 트랜잭션의 변경사항을 보지 않습니다.

##### 명령 동작

`UPDATE`, `DELETE`, `MERGE`, `SELECT FOR UPDATE`, `SELECT FOR SHARE`:
- 트랜잭션 시작 시점에 커밋된 대상 행을 찾습니다.
- 대상 행이 다른 동시 트랜잭션에 의해 업데이트되었으면 대기합니다.
- 첫 번째 업데이터가 롤백하면: Repeatable Read 트랜잭션은 원래 발견한 행으로 진행합니다.
- 첫 번째 업데이터가 커밋하면: Repeatable Read 트랜잭션은 오류와 함께 롤백됩니다:
  ```
  ERROR:  could not serialize access due to concurrent update
  ```

##### 오류 처리

애플리케이션은 트랜잭션을 재시도할 준비가 되어 있어야 합니다:

```
직렬화 오류를 받으면:
1. 현재 트랜잭션을 중단
2. 처음부터 전체 트랜잭션을 재시도
3. 두 번째 시도는 이전에 커밋된 변경을 초기 뷰의 일부로 봄
4. 새 버전의 행을 시작점으로 사용하여 논리적 충돌 없음
```

참고: 업데이트하는 트랜잭션만 재시도가 필요합니다. 읽기 전용 트랜잭션은 직렬화 충돌이 절대 없습니다.

##### 중요한 주의사항

Repeatable Read가 안정적인 뷰를 제공하지만, 일부 순차 실행과 일관되지 않을 수 있습니다. 예:

> 이 수준의 읽기 전용 트랜잭션도 배치가 완료되었음을 나타내도록 업데이트된 제어 레코드를 볼 수 있지만, 제어 레코드의 이전 리비전을 읽었기 때문에 (배치의 논리적 일부인) 상세 레코드를 보지 못할 수 있습니다.

비즈니스 규칙을 강제하기 위해 명시적 잠금을 신중히 사용해야 합니다.

##### 구현

- _스냅샷 격리_ 기술을 사용합니다.
- 전통적인 잠금 시스템과 동작 및 성능의 차이가 있습니다.
- 자세한 정보는 학술 문헌을 참조하세요(문서 참조 섹션 참조).

##### 역사적 참고

PostgreSQL 9.1 이전에는 Serializable 격리를 요청하면 여기 설명된 동작을 제공했습니다. 레거시 동작을 유지하려면 Repeatable Read를 요청하세요.

---

#### 13.2.3. Serializable 격리 수준

##### 동작 방식

- 가장 엄격한 트랜잭션 격리
- 모든 커밋된 트랜잭션에 대해 직렬 트랜잭션 실행을 에뮬레이션합니다.
- 마치 트랜잭션이 동시가 아닌 순차적으로 하나씩 실행된 것처럼 동작합니다.
- 애플리케이션은 직렬화 실패로 인해 트랜잭션을 재시도할 준비가 되어 있어야 합니다.
- Repeatable Read와 정확히 같지만 비직렬 동작을 유발할 수 있는 조건을 모니터링합니다.
- _직렬화 이상_을 유발할 수 있는 조건을 감지합니다.

##### 모니터링

- Repeatable Read를 넘어서는 추가 차단 없음
- 모니터링에 약간의 오버헤드
- 직렬화 이상 조건 감지 시 직렬화 실패를 트리거합니다.

##### 예제: 직렬화 이상

초기 테이블 상태:
```
 class | value
-------+-------
     1 |    10
     1 |    20
     2 |   100
     2 |   200
```

트랜잭션 A:
```sql
SELECT SUM(value) FROM mytab WHERE class = 1;
-- 결과: 30
INSERT INTO mytab (class, value) VALUES (2, 30);
```

트랜잭션 B (동시):
```sql
SELECT SUM(value) FROM mytab WHERE class = 2;
-- 결과: 300
INSERT INTO mytab (class, value) VALUES (1, 300);
```

Repeatable Read에서: 둘 다 커밋됩니다 (일관성 없음)

Serializable에서: 하나는 커밋되고, 다른 하나는 롤백됩니다:
```
ERROR:  could not serialize access due to read/write dependencies among transactions
```

이유:
- A가 B보다 먼저 실행되면: B는 합계 = 330 (300이 아님)을 계산할 것입니다.
- B가 A보다 먼저 실행되면: A는 합계 != 30을 계산할 것입니다.

##### 지연 가능 트랜잭션 (Deferrable Transactions)

영구 테이블에서 읽은 데이터는 다음 경우를 제외하고, 읽기 트랜잭션이 성공적으로 커밋될 때까지 유효하다고 간주해서는 안 됩니다:
- _지연 가능_ 읽기 전용 트랜잭션: 데이터는 읽는 즉시 유효합니다.
- 이러한 트랜잭션은 이상(anomaly)이 발생하지 않음이 보장된 스냅샷을 획득할 때까지 읽기 전에 대기합니다.

##### 술어 잠금 (Predicate Locking)

PostgreSQL은 진정한 직렬화 가능성을 보장하기 위해 _술어 잠금_을 사용합니다:

주요 특성:
- 이전 읽기에 쓰기가 영향을 미쳤는지 판단하기 위한 잠금을 유지합니다.
- 차단을 유발하지 않으며 교착 상태를 유발할 수 없습니다.
- 동시 Serializable 트랜잭션 간의 종속성을 식별하고 표시합니다.
- 트랜잭션이 실제로 접근한 데이터를 기반으로 합니다.

Read Committed/Repeatable Read와의 대조:
- 전체 테이블 잠금 또는 `SELECT FOR UPDATE`/`SELECT FOR SHARE`가 필요할 수 있습니다.
- 다른 트랜잭션을 차단하고 디스크 접근을 유발할 수 있습니다.

##### SIRead 잠금

- `pg_locks` 시스템 뷰에 `SIReadLock` 모드로 나타납니다.
- 메모리 고갈을 방지하기 위해 여러 세분화된 잠금이 더 조대한 잠금으로 결합됩니다.
- READ ONLY 트랜잭션은 완료 전에 SIRead 잠금을 해제할 수 있습니다.
- 시작 시 술어 잠금을 취하지 않고 종종 해제됩니다.
- `SERIALIZABLE READ ONLY DEFERRABLE`은 이상 충돌이 불가능함이 확립될 때까지 차단됩니다.

##### 개발 단순화

Serializable을 일관되게 사용하면 다음 보장을 얻을 수 있습니다: 성공적으로 커밋된 동시 Serializable 트랜잭션의 집합은 어떤 직렬 실행과 동일한 효과를 냅니다.

주요 요구사항:
- 단일 트랜잭션이 단독으로 실행될 때 올바르게 동작함을 보장하세요.
- Serializable 트랜잭션의 모든 조합에서 정상 동작한다는 확신이 생깁니다.
- 일반화된 직렬화 실패 처리 구현 (SQLSTATE '40001')
- 어떤 트랜잭션이 충돌에 관여하는지 미리 예측하기 어렵습니다.

##### 잠재적 문제

PostgreSQL의 Serializable은 진정한 직렬 실행에서 발생하지 않는 오류를 허용할 수 있습니다:

예제: 고유 제약 조건 위반
```
명시적으로 키가 존재하지 않음을 확인한 후에도 동시 Serializable
트랜잭션에서 고유 제약 조건 위반을 볼 수 있습니다.
```

해결책: 잠재적으로 충돌하는 키를 삽입하는 모든 Serializable 트랜잭션은 명시적으로 먼저 확인해야 합니다:
- 기존 최대 키를 선택하고 1을 더합니다.
- 또는 키가 존재하지 않음을 명시적으로 선택하여 확인합니다.

##### 성능 최적화 권장사항

1. 가능한 경우 트랜잭션을 `READ ONLY`로 선언

2. 연결 풀을 사용하여 활성 연결 제어
   - Serializable 트랜잭션이 있는 바쁜 시스템에서 중요합니다.

3. 트랜잭션 범위 최소화
   - 무결성에 필요한 것만 포함합니다.

4. 유휴 트랜잭션 방지
   - `idle_in_transaction_session_timeout` 구성 매개변수를 사용합니다.

5. 가능한 경우 명시적 잠금 제거
   - `SELECT FOR UPDATE` 및 `SELECT FOR SHARE` 제거
   - Serializable이 자동 보호를 제공합니다.

6. 술어 잠금 메모리 관리
   - `max_pred_locks_per_transaction` 증가
   - `max_pred_locks_per_relation` 증가
   - `max_pred_locks_per_page` 증가
   - 여러 페이지 잠금을 관계 수준 잠금으로 결합하는 것을 방지합니다.

7. 순차 스캔 최적화
   - 순차 스캔은 관계 수준 술어 잠금을 필요로 합니다.
   - 직렬화 실패율을 증가시킵니다.
   - `random_page_cost` 감소
   - `cpu_tuple_cost` 증가
   - 감소된 롤백과 쿼리 실행 시간 변화의 균형을 고려합니다.

##### 구현

- _직렬화 가능 스냅샷 격리_ 기술을 사용합니다.
- 스냅샷 격리에 직렬화 이상 검사를 추가하여 구축합니다.
- 전통적인 잠금 시스템과 동작/성능의 차이가 있습니다.

#### 중요 참고 사항

##### 시퀀스 동작

일부 PostgreSQL 데이터 유형은 특별한 트랜잭션 규칙을 가집니다:

시퀀스 (`serial` 열 포함):
- 변경 사항은 모든 트랜잭션에 즉시 표시됩니다.
- 트랜잭션이 중단되어도 변경 사항은 롤백되지 않습니다.
- 참조: 섹션 9.17 및 섹션 8.1.4

---

### 13.3. 명시적 잠금 (Explicit Locking)

PostgreSQL은 테이블의 데이터에 대한 동시 접근을 제어하기 위해 다양한 잠금 모드를 제공합니다. 이러한 모드는 MVCC가 원하는 동작을 제공하지 않을 때 애플리케이션 제어 잠금을 가능하게 합니다. 대부분의 PostgreSQL 명령은 실행 중 테이블 안전을 보장하기 위해 적절한 잠금을 자동으로 획득합니다.

미결 잠금을 보려면 `pg_locks` 시스템 뷰를 사용하세요.

---

#### 13.3.1. 테이블 수준 잠금

모든 잠금 모드는 테이블 수준 잠금이지만, 일부 이름은 역사적입니다. 잠금은 호환성에 따라 충돌합니다 - 두 트랜잭션은 같은 테이블에 충돌하는 잠금을 동시에 유지할 수 없습니다.

##### 테이블 수준 잠금 모드

| 잠금 모드 | 충돌 대상 | 용도 |
|-----------|-----------|------|
| ACCESS SHARE | ACCESS EXCLUSIVE만 | `SELECT` 및 읽기 전용 쿼리 |
| ROW SHARE | EXCLUSIVE, ACCESS EXCLUSIVE | `SELECT FOR UPDATE`, `FOR NO KEY UPDATE`, `FOR SHARE`, `FOR KEY SHARE` |
| ROW EXCLUSIVE | SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `UPDATE`, `DELETE`, `INSERT`, `MERGE` |
| SHARE UPDATE EXCLUSIVE | SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `VACUUM`, `ANALYZE`, `CREATE INDEX CONCURRENTLY`, `CREATE STATISTICS`, `COMMENT ON`, `REINDEX CONCURRENTLY` |
| SHARE | ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `CREATE INDEX` (CONCURRENTLY 없이) |
| SHARE ROW EXCLUSIVE | ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `CREATE TRIGGER`, 일부 `ALTER TABLE` 형태 |
| EXCLUSIVE | ROW SHARE, ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `REFRESH MATERIALIZED VIEW CONCURRENTLY` |
| ACCESS EXCLUSIVE | 모든 모드 | `DROP TABLE`, `TRUNCATE`, `REINDEX`, `CLUSTER`, `VACUUM FULL`, `REFRESH MATERIALIZED VIEW` |

중요: `ACCESS EXCLUSIVE`만 `SELECT` 문(`FOR UPDATE/SHARE` 없이)을 차단합니다.

##### 잠금 동작

- 잠금은 트랜잭션 종료까지 유지됩니다.
- 세이브포인트 후에 획득한 잠금은 세이브포인트가 롤백되면 해제됩니다.
- PL/pgSQL 예외 블록 내의 잠금은 오류 시 해제됩니다.

##### 표 13.2: 충돌하는 잠금 모드

| 요청된 잠금 | ACCESS SHARE | ROW SHARE | ROW EXCL. | SHARE UPDATE EXCL. | SHARE | SHARE ROW EXCL. | EXCL. | ACCESS EXCL. |
|-------------|--------------|-----------|-----------|-------------------|-------|-----------------|-------|--------------|
| ACCESS SHARE | | | | | | | | X |
| ROW SHARE | | | | | | | X | X |
| ROW EXCL. | | | | | X | X | X | X |
| SHARE UPDATE EXCL. | | | | X | X | X | X | X |
| SHARE | | | X | X | | X | X | X |
| SHARE ROW EXCL. | | | X | X | X | X | X | X |
| EXCL. | | X | X | X | X | X | X | X |
| ACCESS EXCL. | X | X | X | X | X | X | X | X |

---

#### 13.3.2. 행 수준 잠금

행 수준 잠금은 같은 행에 대한 쓰기자와 잠금자만 차단하며, 데이터 조회는 차단하지 않습니다. 트랜잭션 종료 시 또는 세이브포인트 롤백 중에 해제됩니다.

##### 행 수준 잠금 모드

FOR UPDATE
- 업데이트를 위해 행을 잠급니다.
- 다른 트랜잭션의 `UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR NO KEY UPDATE`, `SELECT FOR SHARE`, `SELECT FOR KEY SHARE`를 방지합니다.
- `REPEATABLE READ` 또는 `SERIALIZABLE`에서 트랜잭션 시작 이후 행이 변경되면 오류를 발생시킵니다.
- `DELETE` 및 일부 `UPDATE` 작업에서 획득됩니다.

FOR NO KEY UPDATE
- `FOR UPDATE`보다 약합니다.
- `SELECT FOR KEY SHARE`를 차단하지 않습니다.
- `FOR UPDATE`를 획득하지 않는 `UPDATE` 작업에서 획득됩니다.

FOR SHARE
- 배타적 잠금이 아닌 공유 잠금을 획득합니다.
- `UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR NO KEY UPDATE`를 차단합니다.
- `SELECT FOR SHARE` 또는 `SELECT FOR KEY SHARE`를 방지하지 않습니다.

FOR KEY SHARE
- `FOR SHARE`보다 약합니다.
- `DELETE` 및 키 변경 `UPDATE` 작업을 차단합니다.
- `SELECT FOR NO KEY UPDATE`, `SELECT FOR SHARE`, 또는 `SELECT FOR KEY SHARE`를 차단하지 않습니다.

##### 표 13.3: 충돌하는 행 수준 잠금

| 요청된 잠금 | FOR KEY SHARE | FOR SHARE | FOR NO KEY UPDATE | FOR UPDATE |
|-------------|---------------|-----------|-------------------|------------|
| FOR KEY SHARE | | | | X |
| FOR SHARE | | | X | X |
| FOR NO KEY UPDATE | | X | X | X |
| FOR UPDATE | X | X | X | X |

참고: PostgreSQL은 잠긴 행 수에 제한이 없지만, 잠금은 디스크 쓰기를 유발할 수 있습니다(예: `SELECT FOR UPDATE`는 행을 잠금으로 표시함).

---

#### 13.3.3. 페이지 수준 잠금

페이지 수준 공유/배타적 잠금은 공유 버퍼 풀의 테이블 페이지에 대한 읽기/쓰기 접근을 제어합니다. 행을 가져오거나 업데이트한 직후에 해제됩니다. 애플리케이션 개발자는 일반적으로 페이지 수준 잠금을 직접 다룰 필요가 없습니다.

---

#### 13.3.4. 교착 상태 (Deadlocks)

교착 상태는 두 개 이상의 트랜잭션이 다른 트랜잭션이 필요로 하는 잠금을 유지할 때 발생합니다. PostgreSQL은 교착 상태를 자동으로 감지하고 한 트랜잭션을 중단하여 해결합니다.

##### 교착 상태 시나리오 예제

트랜잭션 1:
```sql
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 11111;
```

트랜잭션 2:
```sql
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 22222;
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 11111;
```

트랜잭션 1 (계속):
```sql
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 22222;
```

이것은 교착 상태를 만듭니다: 트랜잭션 1은 트랜잭션 2의 acctnum 22222에 대한 잠금을 기다리고, 트랜잭션 2는 트랜잭션 1의 acctnum 11111에 대한 잠금을 기다립니다.

##### 교착 상태 방지

1. 모든 애플리케이션에서 일관된 순서로 잠금 획득
2. 각 객체에 대해 가장 제한적인 잠금 모드를 먼저 획득
3. 트랜잭션을 장기간 열어두지 않기
4. 교착 상태로 인해 중단된 트랜잭션 재시도

---

#### 13.3.5. 권고 잠금 (Advisory Locks)

권고 잠금은 애플리케이션 정의 의미를 가집니다. 시스템은 사용을 강제하지 않습니다 - 애플리케이션이 올바르게 사용해야 합니다. 비관적 잠금 전략에 유용합니다.

##### 획득 수준

세션 수준:
- 명시적으로 해제되거나 세션이 종료될 때까지 유지됩니다.
- 트랜잭션 의미를 따르지 않습니다.
- 롤백이 잠금을 해제하지 않습니다.
- 다중 획득은 각각에 대응하는 잠금 해제가 필요합니다.

트랜잭션 수준:
- 트랜잭션 종료 시 자동으로 해제됩니다.
- 명시적 잠금 해제 작업이 없습니다.
- 단기 사용에 더 편리합니다.

##### 주요 특성

- 테이블 플래그보다 빠릅니다.
- 테이블 블로트를 방지합니다.
- 세션 종료 시 자동으로 정리됩니다.
- 세션과 트랜잭션 수준 요청은 예상대로 서로를 차단합니다.
- 프로세스는 같은 잠금을 여러 번 유지할 수 있습니다(여러 잠금 해제 필요).

##### 중요 고려사항

권고 잠금과 일반 잠금은 다음에 의해 정의된 메모리 풀을 공유합니다:
- `max_locks_per_transaction`
- `max_connections`

LIMIT 및 정렬에 대한 주의:

```sql
-- OK
SELECT pg_advisory_lock(id) FROM foo WHERE id = 12345;

-- 위험: LIMIT이 잠금 전에 적용되지 않을 수 있음
SELECT pg_advisory_lock(id) FROM foo WHERE id > 12345 LIMIT 100;

-- OK: 하위 쿼리를 사용하여 잠금 전에 LIMIT 보장
SELECT pg_advisory_lock(q.id) FROM
(
  SELECT id FROM foo WHERE id > 12345 LIMIT 100
) q;
```

위험한 형태는 예상치 못한 잠금(댕글링 잠금)을 획득할 수 있으며 `pg_locks`에서 볼 수 있지만 세션 종료까지 해제되지 않습니다.

##### 권고 잠금 함수

권고 잠금을 조작하기 위한 함수는 PostgreSQL 문서의 섹션 9.28.10에 설명되어 있습니다.

---

### 13.4. 애플리케이션 수준의 데이터 일관성 검사

애플리케이션 수준의 데이터 일관성 검사는 특히 동시 환경에서 데이터 무결성과 관련된 비즈니스 규칙을 유지하는 데 중요합니다. 접근 방식은 사용 중인 트랜잭션 격리 수준에 따라 다릅니다.

---

#### 13.4.1. Serializable 트랜잭션으로 일관성 강제하기

##### 핵심 개념

Serializable 트랜잭션 격리 수준 이 모든 쓰기와 데이터의 일관된 뷰가 필요한 모든 읽기에 사용되면, 일관성을 보장하기 위한 추가 노력이 필요하지 않습니다.

##### 작동 방식

- Serializable 트랜잭션은 위험한 읽기/쓰기 충돌 패턴을 논블로킹 방식으로 모니터링하는 기능을 추가한 Repeatable Read 트랜잭션입니다.
- 가능한 실행 순서에서 사이클을 유발할 수 있는 패턴이 감지되면, 관련 트랜잭션 중 하나가 사이클을 해소하기 위해 롤백됩니다.

##### 모범 사례

1. 일관성이 중요한 작업에 Serializable 사용: Serializable 트랜잭션을 사용하도록 작성된 소프트웨어는 PostgreSQL에서 "그냥 작동"해야 합니다.

2. 자동 재시도 메커니즘: 직렬화 실패로 롤백된 트랜잭션을 자동으로 재시도하는 프레임워크 사용

3. 기본 격리 수준 설정:
   ```sql
   SET default_transaction_isolation = serializable;
   ```

4. 격리 수준 강제: 다른 격리 수준이 부주의하게 사용되거나 무결성 검사를 우회하지 않도록 트리거 검사를 추가합니다.

##### 경고: 데이터 복제 제한

Serializable 트랜잭션의 무결성 보호는 다음 환경에서는 적용되지 않습니다:
- 핫 스탠바이 모드
- 논리적 복제본

이러한 환경에서는 기본 서버에서 명시적 잠금을 함께 사용하는 Repeatable Read를 대신 사용하세요.

---

#### 13.4.2. 명시적 차단 잠금으로 일관성 강제하기

##### 사용 시기

비직렬화 가능 쓰기가 발생할 수 있는 환경에서는 명시적 잠금을 사용하여 행의 유효성을 보장하고 동시 업데이트로부터 보호합니다.

##### 잠금 메커니즘

```sql
SELECT FOR UPDATE      -- 동시 업데이트에 대해 반환된 행을 잠금
SELECT FOR SHARE       -- 공유 모드로 반환된 행을 잠금
LOCK TABLE table_name  -- 전체 테이블을 잠금
```

##### 중요한 동작 참고

중요한 주의사항: `SELECT FOR UPDATE`는 잠금이 해제된 후 다른 트랜잭션이 해당 행을 업데이트하거나 삭제하는 것을 방지하지 않습니다.

동시 수정을 방지하려면:
- 값이 바뀌지 않더라도 실제로 행을 UPDATE 해야 합니다.
- `SELECT FOR UPDATE`는 다른 트랜잭션의 동일 잠금 획득이나 UPDATE/DELETE 실행을 일시적으로만 차단합니다.
- 트랜잭션이 커밋되거나 롤백되면, 실제 UPDATE가 수행되지 않은 경우 차단된 트랜잭션이 진행됩니다.

##### 예제 시나리오: 글로벌 유효성 검사

문제: 은행 애플리케이션이 두 테이블에서 대변 합계가 차변 합계와 같은지 확인해야 함

잘못된 접근 방식 (Read Committed):
```sql
-- 이것들은 신뢰할 수 없게 작동함 - 두 번째 쿼리는 커밋되지 않은 트랜잭션을 포함
SELECT sum(credits) FROM credits_table;
SELECT sum(debits) FROM debits_table;
```

더 나은 접근 방식 (명시적 잠금이 있는 Repeatable Read):
```sql
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
LOCK TABLE credits_table IN SHARE MODE;
LOCK TABLE debits_table IN SHARE MODE;
SELECT sum(credits) FROM credits_table;
SELECT sum(debits) FROM debits_table;
COMMIT;
```

이유: SHARE 모드 잠금은 현재 트랜잭션을 제외하고, 잠긴 테이블에 커밋되지 않은 변경이 없음을 보장합니다.

##### 중요한 타이밍 고려사항

Repeatable Read 모드에서: 쿼리를 수행하기 전에 잠금을 획득하세요.
- Repeatable Read 트랜잭션의 스냅샷은 첫 번째 쿼리 또는 데이터 수정 명령(`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `MERGE`) 시작 시 고정됩니다.
- 트랜잭션이 획득한 잠금은 다른 수정 트랜잭션이 실행 중이지 않음을 보장합니다.
- 단, 스냅샷이 잠금 획득보다 먼저 고정된 경우, 현재 커밋된 변경사항을 반영하지 못할 수 있습니다.

모범 사례: 스냅샷이 고정되기 전에 명시적으로 잠금을 획득하세요.

---

##### 요약 비교

| 접근 방식 | 격리 수준 | 장점 | 단점 |
|-----------|-----------|------|------|
| Serializable 트랜잭션 | SERIALIZABLE | 추가 코딩 불필요; 자동 충돌 감지 | 핫 스탠바이/논리적 복제와 작동 안 함; 재시도 로직 필요 |
| 명시적 차단 잠금 | READ COMMITTED 또는 REPEATABLE READ | 복제와 작동; 세밀한 제어 | 신중한 애플리케이션 로직 필요; 수동 잠금 관리 |

---

### 13.5. 직렬화 실패 처리

Repeatable Read 와 Serializable 격리 수준 모두 직렬화 이상을 방지하기 위해 설계된 오류를 생성할 수 있습니다. 이러한 수준을 사용하는 애플리케이션은 직렬화 오류로 인해 실패한 트랜잭션을 재시도할 준비가 되어 있어야 합니다.

#### 오류 코드와 유형

##### 주요 오류
- SQLSTATE 코드: `40001` (`serialization_failure`)
  - 오류 메시지 텍스트는 상황에 따라 다릅니다.
  - 트랜잭션 재시도가 필요한 직렬화 이상을 나타냅니다.

##### 관련 오류 (재시도가 필요할 수 있음)

1. 교착 상태 실패
   - SQLSTATE 코드: `40P01` (`deadlock_detected`)
   - 이것도 재시도하는 것이 좋습니다.

2. 고유 키 실패 (주의해서 사용)
   - SQLSTATE 코드: `23505` (`unique_violation`)
   - 여러 인스턴스가 동시에 같은 새 기본 키 값을 선택할 때 직렬화 실패를 나타낼 수 있습니다.
   - 일시적 실패가 아닌 지속적 오류일 수도 있으므로 신중한 처리가 필요합니다.

3. 제외 제약 조건 실패 (주의해서 사용)
   - SQLSTATE 코드: `23P01` (`exclusion_violation`)
   - 고유 키 실패와 유사한 고려사항

#### 트랜잭션 재시도를 위한 주요 요구사항

1. 전체 트랜잭션 재시도
   - 어떤 SQL을 발행할지 결정하는 모든 로직 포함
   - 어떤 값을 사용할지 결정하는 모든 로직 포함
   - 정확성 문제로 인해 PostgreSQL은 자동 재시도 기능을 제공하지 않습니다.

2. 여러 번의 시도가 필요할 수 있음
   - 트랜잭션 재시도가 첫 번째 재시도에서 완료를 보장하지 않습니다.
   - 높은 경합 시나리오에서는 많은 시도가 필요할 수 있습니다.
   - 충돌하는 준비된 트랜잭션은 커밋되거나 롤백될 때까지 진행을 차단할 수 있습니다.

3. 예외 처리 전략
   - `serialization_failure` 오류를 무조건 재시도
   - 다른 오류 코드는 일시적 실패가 아닌 지속적 조건을 나타낼 수 있으므로 주의해야 합니다.

---

### 13.6. 주의사항 (Caveats)

#### 1. MVCC 안전하지 않은 DDL 명령

영향 받는 명령:
- `TRUNCATE`
- 테이블을 다시 작성하는 형태의 `ALTER TABLE`

문제: 이러한 명령은 MVCC 안전하지 않습니다. DDL 명령이 커밋된 후, DDL 명령 커밋 이전에 고정된 스냅샷을 사용하는 동시 트랜잭션에게 테이블이 비어 있는 것처럼 보입니다.

중요 참고: 이것은 DDL 명령이 시작되기 전에 테이블에 접근하지 않은 트랜잭션에만 영향을 미칩니다. 이전에 테이블에 접근한 트랜잭션은 `ACCESS SHARE` 테이블 잠금을 유지하며, 이는 DDL 명령이 완료될 때까지 차단합니다.

의미:
- 대상 테이블에 대한 연속 쿼리에서 명백한 불일치 없음
- 대상 테이블과 데이터베이스의 다른 테이블 간에 가시적 불일치를 유발할 수 있음

#### 2. 핫 스탠바이에서의 Serializable 격리 수준

제한: Serializable 트랜잭션 격리 수준은 핫 스탠바이 복제 대상(섹션 26.4)에서 지원되지 않습니다.

현재 지원: 핫 스탠바이 모드에서 사용 가능한 가장 엄격한 격리 수준은 Repeatable Read 입니다.

고려사항: 기본 서버에서 모든 영구 데이터베이스 쓰기를 Serializable 트랜잭션 내에서 수행하면 모든 스탠바이가 결국 일관된 상태에 도달하도록 보장하지만, 스탠바이에서의 Repeatable Read 트랜잭션은 때때로 기본 서버에서의 어떤 직렬 실행과도 일치하지 않는 일시적 상태를 볼 수 있습니다.

#### 3. 시스템 카탈로그 접근 이상

문제: 시스템 카탈로그에 대한 내부 접근은 현재 트랜잭션의 격리 수준을 사용하지 않습니다.

결과:
- 새로 생성된 데이터베이스 객체(테이블 등)는 포함된 행이 보이지 않더라도 동시 Repeatable Read 및 Serializable 트랜잭션에 보입니다.
- 더 높은 격리 수준에서의 명시적 시스템 카탈로그 쿼리는 동시에 생성된 객체를 나타내는 행을 보지 못합니다.

이것은 암시적과 명시적 카탈로그 접근 사이에 비대칭을 만듭니다.

---

### 13.7. 잠금과 인덱스 (Locking and Indexes)

PostgreSQL은 테이블 데이터에 대한 논블로킹 읽기/쓰기 접근을 제공하지만, 논블로킹 읽기/쓰기 접근은 현재 모든 인덱스 접근 방법에 대해 제공되지 않습니다. 다른 인덱스 유형은 잠금을 다르게 처리합니다.

#### 인덱스 유형과 잠금 동작

##### B-tree, GiST, SP-GiST 인덱스
- 잠금 유형: 단기 공유/배타적 페이지 수준 잠금
- 잠금 해제: 각 인덱스 행을 가져오거나 삽입한 직후
- 동시성: 교착 상태 없이 가장 높은 동시성 제공

##### Hash 인덱스
- 잠금 유형: 공유/배타적 해시 버킷 수준 잠금
- 잠금 해제: 전체 버킷이 처리된 후
- 동시성: 인덱스 수준 잠금보다 나은 동시성이지만, 잠금이 하나의 인덱스 작업보다 더 오래 유지되므로 교착 상태 가능

##### GIN 인덱스
- 잠금 유형: 단기 공유/배타적 페이지 수준 잠금
- 잠금 해제: 각 인덱스 행을 가져오거나 삽입한 직후
- 참고: GIN 인덱싱된 값을 삽입하면 행 하나당 여러 인덱스 키 삽입이 발생하므로, 단일 값 삽입에도 상당한 작업이 수반될 수 있습니다.

#### 권장사항

동시 애플리케이션의 경우:
- B-tree 인덱스는 스칼라 데이터를 인덱싱하는 동시 애플리케이션에 권장되는 인덱스 유형으로, 최고의 성능을 제공합니다.
- Hash 인덱스보다 더 많은 기능을 제공합니다.
- 비스칼라 데이터의 경우, B-tree가 적합하지 않으므로 GiST, SP-GiST, 또는 GIN 인덱스를 사용하세요.

---

### 요약

PostgreSQL의 명시적 잠금 시스템은 테이블 수준 잠금, 행 수준 잠금, 권고 잠금을 통해 동시 접근에 대한 세밀한 제어를 제공합니다. 적절한 잠금 전략 - 일관된 잠금 순서, 적절한 잠금 모드, 교착 상태 처리 - 은 강력한 동시 데이터베이스 애플리케이션에 필수적입니다.

---

## PostgreSQL 18 성능 팁 (Performance Tips)

> 원문: https://www.postgresql.org/docs/18/performance-tips.html

쿼리 성능은 여러 요소의 영향을 받습니다. 일부는 사용자가 제어할 수 있지만, 나머지는 시스템의 기본 설계에 따른 근본적인 것입니다. 이 장에서는 PostgreSQL 성능을 이해하고 튜닝하는 방법을 설명합니다.

---

### 목차

- [14.1 EXPLAIN 사용하기](#141-explain-사용하기)
  - [14.1.1 EXPLAIN 기초](#1411-explain-기초)
  - [14.1.2 EXPLAIN ANALYZE](#1412-explain-analyze)
  - [14.1.3 주의사항](#1413-주의사항)
- [14.2 플래너가 사용하는 통계](#142-플래너가-사용하는-통계)
  - [14.2.1 단일 칼럼 통계](#1421-단일-칼럼-통계)
  - [14.2.2 확장 통계](#1422-확장-통계)
- [14.3 명시적 JOIN 절로 플래너 제어하기](#143-명시적-join-절로-플래너-제어하기)
- [14.4 데이터베이스 채우기](#144-데이터베이스-채우기)
  - [14.4.1 자동 커밋 비활성화](#1441-자동-커밋-비활성화)
  - [14.4.2 COPY 사용](#1442-copy-사용)
  - [14.4.3 인덱스 제거](#1443-인덱스-제거)
  - [14.4.4 외래 키 제약 조건 제거](#1444-외래-키-제약-조건-제거)
  - [14.4.5 maintenance_work_mem 증가](#1445-maintenance_work_mem-증가)
  - [14.4.6 max_wal_size 증가](#1446-max_wal_size-증가)
  - [14.4.7 WAL 아카이빙 및 스트리밍 복제 비활성화](#1447-wal-아카이빙-및-스트리밍-복제-비활성화)
  - [14.4.8 완료 후 ANALYZE 실행](#1448-완료-후-analyze-실행)
  - [14.4.9 pg_dump에 대한 참고사항](#1449-pg_dump에-대한-참고사항)
- [14.5 비영속적 설정](#145-비영속적-설정)

---

### 14.1 EXPLAIN 사용하기

PostgreSQL은 수신하는 각 쿼리에 대해 쿼리 계획(query plan) 을 수립합니다. 쿼리의 구조와 데이터의 특성에 맞는 올바른 계획을 선택하는 것은 좋은 성능을 위해 절대적으로 중요하므로, 시스템에는 좋은 계획을 생성하려고 시도하는 복잡한 플래너(planner) 가 포함되어 있습니다. `EXPLAIN` 명령을 사용하여 플래너가 모든 쿼리에 대해 생성하는 쿼리 계획을 볼 수 있습니다. 계획 읽기는 배워야 할 기술이지만, 이 섹션에서는 기본 사항을 다룹니다.

#### 14.1.1 EXPLAIN 기초

쿼리 계획의 구조는 계획 노드(plan nodes)의 트리입니다. 트리의 최하위 레벨 노드는 스캔 노드로, 테이블에서 원시 행을 반환합니다. 테이블 접근 방법에 따라 순차 스캔(sequential scans), 인덱스 스캔(index scans), 비트맵 인덱스 스캔(bitmap index scans) 등 다양한 스캔 노드 유형이 있습니다. `VALUES` 절이나 set-returning 함수처럼 테이블이 아닌 행 소스도 있으며, 이들은 고유한 스캔 노드 유형을 가집니다. 쿼리가 조인, 집계, 정렬 또는 원시 행에 대한 기타 작업을 필요로 하면, 스캔 노드 위에 해당 작업을 수행하는 추가 노드가 생깁니다. 이러한 작업을 수행하는 방법은 대개 둘 이상이므로 노드 유형도 여러 가지가 나타날 수 있습니다. `EXPLAIN` 출력은 계획 트리의 각 노드에 대해 한 줄씩 표시하며, 기본 노드 유형과 플래너가 추정한 해당 노드의 실행 비용을 보여줍니다. 노드의 추가 속성을 나타내는 줄이 요약 줄 아래에 들여쓰기되어 나타날 수 있습니다. 첫 번째 줄(최상위 노드)은 전체 계획의 추정 실행 비용을 나타내며, 플래너가 최소화하려는 값입니다.

다음은 계획이 어떻게 생겼는지 보여주는 간단한 예입니다:

```sql
EXPLAIN SELECT * FROM tenk1;

                         QUERY PLAN
-------------------------------------------------------------
 Seq Scan on tenk1  (cost=0.00..445.00 rows=10000 width=244)
```

`EXPLAIN`이 인용하는 숫자는 다음과 같습니다(왼쪽에서 오른쪽으로):

- 추정 시작 비용(Estimated start-up cost): 출력 단계가 시작되기 전에 소요된 비용입니다. 예를 들어, 정렬 노드에서 정렬을 수행하는 시간입니다.

- 추정 총 비용(Estimated total cost): 이것은 계획 노드가 완료될 때까지 실행된다는 가정 하에 명시됩니다. 즉, 모든 사용 가능한 행이 검색됩니다. 실제로 노드의 부모가 모든 사용 가능한 행을 읽지 않을 수 있습니다(`LIMIT`이 있는 경우의 예 참조).

- 추정 행 수(Estimated number of rows): 이 계획 노드가 출력하는 행 수입니다. 다시 말하지만, 노드가 완료될 때까지 실행된다고 가정합니다.

- 추정 평균 너비(Estimated average width): 이 계획 노드가 출력하는 행의 평균 너비(바이트 단위)입니다.

비용은 플래너의 비용 매개변수에 의해 결정되는 임의의 단위로 측정됩니다. 전통적인 관행은 디스크 페이지 가져오기 단위로 비용을 측정하는 것입니다. 즉, `seq_page_cost`가 관례적으로 `1.0`으로 설정되고 다른 비용 매개변수가 그에 상대적으로 설정됩니다.

상위 노드의 비용에는 모든 자식 노드의 비용이 포함된다는 점이 중요합니다. 비용은 플래너가 관심을 갖는 항목만 반영한다는 점도 마찬가지입니다. 특히, 비용에는 결과 행을 프론트엔드로 전송하는 시간이 포함되지 않는데, 이는 실제 경과 시간에서 중요한 요소가 될 수 있습니다. 다만 플래너는 이 부분을 변경할 수 없으므로 무시합니다(어떤 계획을 선택하든 동일한 행이 출력된다고 가정합니다).

행 값은 다소 까다롭습니다. 계획 노드가 처리하거나 스캔한 행 수가 아니라, 노드가 출력하는 행 수이기 때문입니다. 이는 노드에 적용된 모든 `WHERE` 절 조건으로 필터링된 결과를 반영하므로, 스캔된 수보다 적은 경우가 많습니다. 이상적으로는 최상위 행 추정치가 쿼리가 실제로 반환하는 행 수에 근접해야 합니다.

예제로 돌아가서, `tenk1`에 10000개의 행과 345개의 디스크 페이지가 있음을 알 수 있습니다:

```sql
SELECT relpages, reltuples FROM pg_class WHERE relname = 'tenk1';

 relpages | reltuples
----------+-----------
      345 |     10000
```

비용은 (디스크 페이지 읽기 × `seq_page_cost`) + (스캔된 행 × `cpu_tuple_cost`)로 추정됩니다. 기본적으로 `seq_page_cost`는 1.0이고 `cpu_tuple_cost`는 0.01이므로 추정 비용은 (345 × 1.0) + (10000 × 0.01) = 445입니다.

이제 `WHERE` 조건을 추가하여 쿼리를 수정해 보겠습니다:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 7000;

                         QUERY PLAN
------------------------------------------------------------
 Seq Scan on tenk1  (cost=0.00..470.00 rows=7000 width=244)
   Filter: (unique1 < 7000)
```

`EXPLAIN` 출력은 `WHERE` 절이 "필터" 조건으로 적용됨을 보여줍니다. 즉, 계획 노드는 스캔하는 각 행에 대해 조건을 확인하고 조건을 통과하는 행만 출력합니다. 필터 조건으로 인해 출력 행 추정치가 감소했습니다. 그러나 스캔은 여전히 10000개의 모든 행을 방문해야 하므로 비용이 감소하지 않았습니다. 실제로 필터링을 반영하는 일부 추가 CPU 비용으로 인해 약간 증가했습니다.

이 쿼리가 실제로 선택하는 행 수는 7000이지만, 행 추정치는 근사치일 뿐입니다. 같은 실험을 두 번 실행하면 약간 다른 추정치를 얻을 수 있습니다. 또한 `ANALYZE`가 생성하는 통계는 테이블에서 무작위로 추출한 샘플에 기반하므로, `ANALYZE` 실행마다 추정치가 달라질 수 있습니다.

이제 조건을 더 제한적으로 만들어 보겠습니다:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 100;

                                  QUERY PLAN
------------------------------------------------------------------------------
 Bitmap Heap Scan on tenk1  (cost=5.06..224.98 rows=100 width=244)
   Recheck Cond: (unique1 < 100)
   ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0)
         Index Cond: (unique1 < 100)
```

여기서 플래너는 두 단계 계획을 선택했습니다. 자식 계획 노드가 인덱스를 탐색하여 인덱스 조건에 일치하는 행의 위치를 찾고, 상위 계획 노드가 해당 행을 테이블에서 실제로 가져옵니다. 행을 개별적으로 가져오는 것은 순차 읽기보다 훨씬 비싸지만, 테이블의 모든 페이지를 방문하지 않아도 되므로 순차 스캔보다는 여전히 저렴합니다.

왜 두 계획 레벨이 사용되는지 궁금할 수 있습니다. 왜 인덱스 스캔 노드가 디스크에서 행을 직접 가져오지 않을까요? 그 이유는 상위 계획 노드가 인덱스로 식별한 행 위치를 물리적 순서로 정렬한 뒤 읽기 때문입니다. 이렇게 하지 않으면 디스크 탐색이 빈번하게 발생합니다. "비트맵(bitmap)"이라고 알려진 이 두 레벨 계획은 바로 이 이유로 필요합니다.

이제 테이블에서 발견되는지 여부가 무관하게 각 행을 가져오도록 보장하는 다른 조건을 추가해 보겠습니다:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 100 AND stringu1 = 'xxx';

                                  QUERY PLAN
------------------------------------------------------------------------------
 Bitmap Heap Scan on tenk1  (cost=5.04..225.20 rows=1 width=244)
   Recheck Cond: (unique1 < 100)
   Filter: (stringu1 = 'xxx'::name)
   ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0)
         Index Cond: (unique1 < 100)
```

추가된 조건 `stringu1 = 'xxx'`는 추정 출력 행 수를 줄이지만 비용은 줄이지 않습니다. 왜냐하면 여전히 동일한 행 세트를 방문해야 하기 때문입니다. `stringu1`은 여기서 사용되는 인덱스의 일부가 아니기 때문에 인덱스 조건으로 적용될 수 없습니다. 대신 인덱스에서 검색된 행에 필터로 적용됩니다. 따라서 비용은 필터링을 반영하기 위해 실제로 약간 증가했습니다.

어떤 경우에는 플래너가 "단순한" 인덱스 스캔 계획을 선호할 것입니다:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 = 42;

                                 QUERY PLAN
-----------------------------------------------------------------------------
 Index Scan using tenk1_unique1 on tenk1  (cost=0.29..8.30 rows=1 width=244)
   Index Cond: (unique1 = 42)
```

이 유형의 계획에서는 테이블 행을 인덱스 순서로 가져오므로 읽기 비용이 더 비싸지만, 행 수가 너무 적어 행 위치를 정렬하는 추가 비용을 들일 가치가 없습니다. 이 계획 유형은 단일 행만 가져오는 쿼리와 인덱스 정렬 순서와 일치하는 `ORDER BY` 조건이 있는 쿼리에서 가장 흔히 볼 수 있습니다. 별도의 정렬 단계가 필요 없기 때문입니다.

`ORDER BY`를 만족시키는 방법이 여러 가지인 경우도 있습니다:

```sql
EXPLAIN SELECT * FROM tenk1 ORDER BY unique1;
                            QUERY PLAN
-------------------------------------------------------------------
 Sort  (cost=1109.39..1134.39 rows=10000 width=244)
   Sort Key: unique1
   ->  Seq Scan on tenk1  (cost=0.00..445.00 rows=10000 width=244)
```

순차 스캔과 정렬은 인덱스 스캔보다 저렴할 수 있습니다. 왜냐하면 인덱스 스캔은 비순차 디스크 접근을 필요로 하기 때문입니다. 테이블의 일부만 가져오는 쿼리에서는 비용 계산이 다르게 진행됩니다.

`LIMIT`이 있을 때 특별히 흥미롭습니다. 왜냐하면 플래너가 `ORDER BY` 열의 첫 번째 키에서 정렬된 인덱스가 있다면 종종 증분 정렬을 사용하기 때문입니다:

```sql
EXPLAIN SELECT * FROM tenk1 ORDER BY hundred, ten LIMIT 100;
                                              QUERY PLAN
-------------------------------------------------------------------------------------------------------
 Limit  (cost=19.35..39.49 rows=100 width=244)
   ->  Incremental Sort  (cost=19.35..2033.39 rows=10000 width=244)
         Sort Key: hundred, ten
         Presorted Key: hundred
         ->  Index Scan using tenk1_hundred on tenk1  (cost=0.29..1574.20 rows=10000 width=244)
```

증분 정렬은 첫 번째 정렬 키(여기서는 `hundred`)의 각 값에 대해 별도로 정렬합니다. 이를 통해 `LIMIT`이 있을 때 더 빨리 결과를 반환하기 시작할 수 있고, 메모리 사용량을 크게 줄일 수 있습니다.

`WHERE` 절의 여러 인덱스 조건을 결합할 수 있다는 것도 언급할 가치가 있습니다. 위에서 본 것처럼 비트맵 인덱스 스캔 노드를 결합하여:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 100 AND unique2 > 9000;

                                     QUERY PLAN
-------------------------------------------------------------------------------------
 Bitmap Heap Scan on tenk1  (cost=25.07..60.11 rows=10 width=244)
   Recheck Cond: ((unique1 < 100) AND (unique2 > 9000))
   ->  BitmapAnd  (cost=25.07..25.07 rows=10 width=0)
         ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0)
               Index Cond: (unique1 < 100)
         ->  Bitmap Index Scan on tenk1_unique2  (cost=0.00..19.78 rows=999 width=0)
               Index Cond: (unique2 > 9000)
```

두 조건 중 하나만으로도 인덱스를 사용할 수 있지만, 이 방식으로 테이블 방문을 두 인덱스 조건을 모두 만족하는 행으로 제한합니다.

`LIMIT`이 있을 때 추정 행의 일부만 필요하다는 것을 고려하여 비용 추정치의 변화를 살펴보겠습니다:

```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 100 AND unique2 > 9000 LIMIT 2;

                                     QUERY PLAN
-------------------------------------------------------------------------------------
 Limit  (cost=0.29..14.28 rows=2 width=244)
   ->  Index Scan using tenk1_unique2 on tenk1  (cost=0.29..70.27 rows=10 width=244)
         Index Cond: (unique2 > 9000)
         Filter: (unique1 < 100)
```

계획과 추정치에서 흥미로운 변화를 볼 수 있습니다. Index Scan 노드의 총 비용과 행 추정치는 `LIMIT` 절이 없을 때와 동일하게 표시됩니다. `Limit` 노드가 실행을 일찍 중단하더라도 자식 노드는 스스로 어떤 제약도 인식하지 못하기 때문에 예상된 결과입니다. 반면 `Limit` 노드의 비용은 자식 노드의 총 비용으로 행당 비용을 계산한 뒤 원하는 행 수를 곱한 값입니다.

더 복잡한 쿼리에서 `EXPLAIN`의 출력을 살펴보겠습니다. 먼저 해시 조인을 사용하는 예:

```sql
EXPLAIN SELECT *
FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 10 AND t1.unique2 = t2.unique2;

                                      QUERY PLAN
--------------------------------------------------------------------------------------
 Nested Loop  (cost=4.65..118.50 rows=10 width=488)
   ->  Bitmap Heap Scan on tenk1 t1  (cost=4.36..39.38 rows=10 width=244)
         Recheck Cond: (unique1 < 10)
         ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..4.36 rows=10 width=0)
               Index Cond: (unique1 < 10)
   ->  Index Scan using tenk2_unique2 on tenk2 t2  (cost=0.29..7.90 rows=1 width=244)
         Index Cond: (unique2 = t1.unique2)
```

이 계획에서는 두 입력 스캔 위에 중첩 루프 조인 노드가 있습니다. 노드 요약 줄의 들여쓰기는 계획 트리 구조를 반영합니다. 조인의 첫 번째, 즉 "외부" 자식은 앞서 본 것과 유사한 비트맵 스캔입니다. 이 노드에서는 `t1.unique2 = t2.unique2` 절을 적용하지 않으므로, 비용과 행 수는 `... WHERE unique1 < 10`을 단독으로 실행한 것과 동일합니다. 중첩 루프 조인 노드는 외부 자식에서 얻은 각 행마다 내부 자식을 한 번 실행합니다. 현재 외부 행의 열 값을 내부 스캔에 전달할 수 있어서, 외부 행의 `t1.unique2` 값을 이용해 `tenk2`에 대해 앞서 본 `unique1 = 42` 케이스와 유사한 인덱스 스캔을 수행합니다. (참고로, `t1.unique2`는 상수가 아니지만 조인 노드 범위 내에서 현재 행이 처리되는 동안은 값이 변하지 않으므로 상수처럼 취급됩니다.) 따라서 중첩 루프의 추정 비용과 행 수는 외부 스캔에서 직접 도출하고, 내부 스캔 반복당 비용을 더합니다(여기서 10 × 7.90). 다만 실제로는 외부와 내부의 시작 비용을 분리하는 방식이 조금 더 복잡합니다. 내부 스캔의 시작 비용은 한 번만 발생하므로, 중첩 루프의 총 시작 비용 계산 시 내부 시작 비용을 10번 더하지는 않습니다.

이 예에서 조인의 출력 행 수는 두 스캔의 행 수를 곱한 것과 같지만, 일반적으로 항상 그렇지는 않습니다. 두 테이블을 모두 참조하는 추가 `WHERE` 절이 있을 경우, 해당 조건은 스캔 노드가 아닌 조인 노드에서만 적용할 수 있기 때문입니다. 예를 들어 `t1.hundred < t2.hundred`를 추가하면 조인 노드의 출력 행이 줄어들지만 입력 스캔 중 어느 것도 변경되지 않습니다:

```sql
EXPLAIN SELECT *
FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 10 AND t2.unique2 < 10 AND t1.hundred < t2.hundred;

                                         QUERY PLAN
---------------------------------------------------------------------------------------------
 Nested Loop  (cost=4.65..49.36 rows=33 width=488)
   Join Filter: (t1.hundred < t2.hundred)
   ->  Bitmap Heap Scan on tenk1 t1  (cost=4.36..39.38 rows=10 width=244)
         Recheck Cond: (unique1 < 10)
         ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..4.36 rows=10 width=0)
               Index Cond: (unique1 < 10)
   ->  Materialize  (cost=0.29..8.51 rows=10 width=244)
         ->  Index Scan using tenk2_unique2 on tenk2 t2  (cost=0.29..8.46 rows=10 width=244)
               Index Cond: (unique2 < 10)
```

조건 `t1.hundred < t2.hundred`는 인덱스에서 사용할 수 없으므로 조인 노드에서 조인 필터로 적용됩니다. 내부 관계도 인덱스 스캔이 필요 없을 만큼 조건으로 충분히 제한되었습니다. 이에 따라 인덱스 스캔 위에 "Materialize" 계획 노드가 추가됩니다. 이 노드는 행을 한 번만 스캔하고 이후 재방문할 수 있도록 결과를 저장합니다. 중첩 루프의 총 비용에서 볼 수 있듯이, 외부 행당 10회 재방문하는 비용은 그리 크지 않습니다.

`t1`에서 가져오는 행 수가 늘어나면 해시 조인이 사용됩니다:

```sql
EXPLAIN SELECT *
FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 100 AND t1.unique2 = t2.unique2;

                                        QUERY PLAN
------------------------------------------------------------------------------------------
 Hash Join  (cost=226.23..709.73 rows=100 width=488)
   Hash Cond: (t2.unique2 = t1.unique2)
   ->  Seq Scan on tenk2 t2  (cost=0.00..445.00 rows=10000 width=244)
   ->  Hash  (cost=224.98..224.98 rows=100 width=244)
         ->  Bitmap Heap Scan on tenk1 t1  (cost=5.06..224.98 rows=100 width=244)
               Recheck Cond: (unique1 < 100)
               ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0)
                     Index Cond: (unique1 < 100)
```

`t1`에 대한 비트맵 스캔 결과가 해시 테이블에 적재됩니다. 이후 `t2`를 순차 스캔하면서 각 `t2` 행마다 해시 테이블에서 `t1.unique2`와 일치하는 항목을 조회합니다. 해시 조인에서 해시 테이블은 결과에 나타날 수 있는 행을 제공하므로 "내부(inner)" 관계라고 하며, `t2`는 각 행에 대해 해시 테이블을 조회하므로 "외부(outer)" 관계라고 합니다.

다른 조인 유형인 머지 조인도 사용할 수 있습니다:

```sql
EXPLAIN SELECT *
FROM tenk1 t1, onek t2
WHERE t1.unique1 < 100 AND t1.unique2 = t2.unique2;

                                        QUERY PLAN
------------------------------------------------------------------------------------------
 Merge Join  (cost=0.56..233.49 rows=10 width=488)
   Merge Cond: (t1.unique2 = t2.unique2)
   ->  Index Scan using tenk1_unique2 on tenk1 t1  (cost=0.29..643.28 rows=100 width=244)
         Filter: (unique1 < 100)
   ->  Index Scan using onek_unique2 on onek t2  (cost=0.28..166.28 rows=1000 width=244)
```

머지 조인은 입력 데이터가 조인 키에 의해 정렬되어야 합니다. 이 예에서 각 입력은 인덱스 스캔을 사용하여 올바른 순서로 행을 생성합니다. 그러나 명시적인 정렬 단계가 필요한 경우도 있습니다.

플래너가 잘못된 조인 순서를 선택했다고 의심되면, 런타임 매개변수로 특정 조인 유형을 비활성화하여 플래너가 원하는 계획을 사용하도록 강제할 수 있습니다. (다소 거친 방법이지만 유용합니다.)

```sql
SET enable_mergejoin = off;

EXPLAIN SELECT *
FROM tenk1 t1, onek t2
WHERE t1.unique1 < 100 AND t1.unique2 = t2.unique2;

                                        QUERY PLAN
------------------------------------------------------------------------------------------
 Hash Join  (cost=226.23..344.08 rows=10 width=488)
   Hash Cond: (t2.unique2 = t1.unique2)
   ->  Seq Scan on onek t2  (cost=0.00..114.00 rows=1000 width=244)
   ->  Hash  (cost=224.98..224.98 rows=100 width=244)
         ->  Bitmap Heap Scan on tenk1 t1  (cost=5.06..224.98 rows=100 width=244)
               Recheck Cond: (unique1 < 100)
               ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0)
                     Index Cond: (unique1 < 100)
```

플래너가 특정 계획 노드를 비활성화하도록 지시받았지만 그 노드가 유일한 옵션인 경우(예: 인덱스가 없을 때 `enable_seqscan = off`), 플래너는 여전히 해당 노드를 사용하지만 "Disabled"로 표시합니다:

```sql
SET enable_seqscan = off;
EXPLAIN SELECT * FROM unit;

                       QUERY PLAN
---------------------------------------------------------
 Seq Scan on unit  (cost=0.00..21.30 rows=1130 width=44)
   Disabled: true
```

서브쿼리를 사용하는 경우 서브플랜이 생성됩니다:

```sql
EXPLAIN VERBOSE SELECT unique1
FROM tenk1 t
WHERE t.ten < ALL (SELECT o.ten FROM onek o WHERE o.four = t.four);

                               QUERY PLAN
------------------------------------------------------------------------
 Seq Scan on public.tenk1 t  (cost=0.00..586095.00 rows=5000 width=4)
   Output: t.unique1
   Filter: (ALL (t.ten < (SubPlan 1).col1))
   SubPlan 1
     ->  Seq Scan on public.onek o  (cost=0.00..116.50 rows=250 width=4)
           Output: o.ten
           Filter: (o.four = t.four)
```

이 쿼리에서는 외부 쿼리의 현재 행에서 `t.four` 값이 서브쿼리로 전달됩니다. 서브플랜의 `WHERE o.four = t.four` 조건은 매개변수화된 조건입니다.

서브쿼리가 외부 쿼리의 값을 사용하지 않으면 서브플랜을 "해시"할 수 있습니다. 서브플랜을 한 번만 실행하고 결과를 해시 테이블에 저장하는 방식입니다.

```sql
EXPLAIN SELECT *
FROM tenk1 t
WHERE t.unique1 NOT IN (SELECT o.unique1 FROM onek o);

                                         QUERY PLAN
--------------------------------------------------------------------------------------------
 Seq Scan on tenk1 t  (cost=61.77..531.77 rows=5000 width=244)
   Filter: (NOT (ANY (unique1 = (hashed SubPlan 1).col1)))
   SubPlan 1
     ->  Index Only Scan using onek_unique1 on onek o  (cost=0.28..59.27 rows=1000 width=4)
```

서브플랜이 외부 쿼리의 값을 사용하지 않고 외부 계획 실행 전에 한 번만 실행되는 경우 "InitPlan"으로 표시됩니다.

```sql
EXPLAIN VERBOSE SELECT unique1
FROM tenk1 t1 WHERE t1.ten = (SELECT (random() * 10)::integer);

                             QUERY PLAN
------------------------------------------------------------------------
 Seq Scan on public.tenk1 t1  (cost=0.02..470.02 rows=1000 width=4)
   Output: t1.unique1
   Filter: (t1.ten = (InitPlan 1).col1)
   InitPlan 1
     ->  Result  (cost=0.00..0.02 rows=1 width=4)
           Output: ((random() * '10'::double precision))::integer
```

#### 14.1.2 EXPLAIN ANALYZE

`EXPLAIN`의 `ANALYZE` 옵션을 사용하면 실제로 쿼리를 실행하고, 각 계획 노드에서 누적된 실제 행 수와 실제 실행 시간을 추정치와 함께 표시합니다. 플래너의 추정치가 실제와 얼마나 일치하는지 확인하는 데 유용합니다.

```sql
EXPLAIN ANALYZE SELECT *
FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 10 AND t1.unique2 = t2.unique2;

                                                           QUERY PLAN
---------------------------------------------------------------------------------------------------------------------------------
 Nested Loop  (cost=4.65..118.50 rows=10 width=488) (actual time=0.017..0.051 rows=10.00 loops=1)
   Buffers: shared hit=36 read=6
   ->  Bitmap Heap Scan on tenk1 t1  (cost=4.36..39.38 rows=10 width=244) (actual time=0.009..0.017 rows=10.00 loops=1)
         Recheck Cond: (unique1 < 10)
         Heap Blocks: exact=10
         Buffers: shared hit=3 read=5 written=4
         ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..4.36 rows=10 width=0) (actual time=0.004..0.004 rows=10.00 loops=1)
               Index Cond: (unique1 < 10)
               Index Searches: 1
               Buffers: shared hit=2
   ->  Index Scan using tenk2_unique2 on tenk2 t2  (cost=0.29..7.90 rows=1 width=244) (actual time=0.003..0.003 rows=1.00 loops=10)
         Index Cond: (unique2 = t1.unique2)
         Index Searches: 10
         Buffers: shared hit=24 read=6
 Planning:
   Buffers: shared hit=15 dirtied=9
 Planning Time: 0.485 ms
 Execution Time: 0.073 ms
```

"actual time" 값은 밀리초 단위의 실제 시간이고, `cost` 추정치는 임의의 단위로 표현되므로 두 값이 일치할 가능성은 낮습니다. 주로 살펴봐야 할 것은 추정 행 수가 실제 행 수와 합리적으로 일치하는지 여부입니다. 이 예에서는 추정치가 모두 정확했습니다.

`loops` 값이 1보다 크면 `actual time`과 `rows` 값은 실행당 평균임에 유의하세요. 노드에서 소비된 실제 총 시간을 구하려면 `actual time`에 `loops` 값을 곱해야 합니다. 위 예에서 내부 인덱스 스캔을 실행하는 데 총 약 0.030 밀리초가 소요되었습니다.

Sort 노드에 대한 추가 정보의 예:

```sql
EXPLAIN ANALYZE SELECT *
FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 100 AND t1.unique2 = t2.unique2 ORDER BY t1.fivethous;

                                                                 QUERY PLAN
---------------------------------------------------------------------------------------------------------------------------------------------
 Sort  (cost=713.05..713.30 rows=100 width=488) (actual time=2.995..3.002 rows=100.00 loops=1)
   Sort Key: t1.fivethous
   Sort Method: quicksort  Memory: 74kB
   Buffers: shared hit=440
   ->  Hash Join  (cost=226.23..709.73 rows=100 width=488) (actual time=0.515..2.920 rows=100.00 loops=1)
         Hash Cond: (t2.unique2 = t1.unique2)
         Buffers: shared hit=437
         ->  Seq Scan on tenk2 t2  (cost=0.00..445.00 rows=10000 width=244) (actual time=0.026..1.790 rows=10000.00 loops=1)
               Buffers: shared hit=345
         ->  Hash  (cost=224.98..224.98 rows=100 width=244) (actual time=0.476..0.477 rows=100.00 loops=1)
               Buckets: 1024  Batches: 1  Memory Usage: 35kB
               Buffers: shared hit=92
               ->  Bitmap Heap Scan on tenk1 t1  (cost=5.06..224.98 rows=100 width=244) (actual time=0.030..0.450 rows=100.00 loops=1)
                     Recheck Cond: (unique1 < 100)
                     Heap Blocks: exact=90
                     Buffers: shared hit=92
                     ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0) (actual time=0.013..0.013 rows=100.00 loops=1)
                           Index Cond: (unique1 < 100)
                           Index Searches: 1
                           Buffers: shared hit=2
 Planning:
   Buffers: shared hit=12
 Planning Time: 0.187 ms
 Execution Time: 3.036 ms
```

Sort 노드는 사용된 정렬 방법(여기서는 인메모리 quicksort)과 필요한 메모리 양을 보여줍니다. Hash 노드는 해시 버킷과 배치 수, 해시 테이블에 사용된 최대 메모리 양을 보여줍니다.

인덱스 스캔에 대해 "Index Searches" 값이 표시됩니다:

```sql
EXPLAIN ANALYZE SELECT * FROM tenk1 WHERE thousand IN (1, 500, 700, 999);

                                                            QUERY PLAN
-----------------------------------------------------------------------------------------------------------------------------------
 Bitmap Heap Scan on tenk1  (cost=9.45..73.44 rows=40 width=244) (actual time=0.012..0.028 rows=40.00 loops=1)
   Recheck Cond: (thousand = ANY ('{1,500,700,999}'::integer[]))
   Heap Blocks: exact=39
   Buffers: shared hit=47
   ->  Bitmap Index Scan on tenk1_thous_tenthous  (cost=0.00..9.44 rows=40 width=0) (actual time=0.009..0.009 rows=40.00 loops=1)
         Index Cond: (thousand = ANY ('{1,500,700,999}'::integer[]))
         Index Searches: 4
         Buffers: shared hit=8
 Planning Time: 0.029 ms
 Execution Time: 0.034 ms
```

여기서 `IN` 목록의 4개 값 각각에 대해 별도의 인덱스 검색이 필요합니다. 값이 인접하면 상황이 달라집니다:

```sql
EXPLAIN ANALYZE SELECT * FROM tenk1 WHERE thousand IN (1, 2, 3, 4);

                                                            QUERY PLAN
-----------------------------------------------------------------------------------------------------------------------------------
 Bitmap Heap Scan on tenk1  (cost=9.45..73.44 rows=40 width=244) (actual time=0.009..0.019 rows=40.00 loops=1)
   Recheck Cond: (thousand = ANY ('{1,2,3,4}'::integer[]))
   Heap Blocks: exact=38
   Buffers: shared hit=40
   ->  Bitmap Index Scan on tenk1_thous_tenthous  (cost=0.00..9.44 rows=40 width=0) (actual time=0.005..0.005 rows=40.00 loops=1)
         Index Cond: (thousand = ANY ('{1,2,3,4}'::integer[]))
         Index Searches: 1
         Buffers: shared hit=2
 Planning Time: 0.029 ms
 Execution Time: 0.026 ms
```

인접한 값들은 단일 인덱스 검색으로 처리될 수 있습니다.

다중 열 인덱스의 선행 열에 범위 조건이 있을 때 "스킵 스캔" 최적화의 예:

```sql
EXPLAIN ANALYZE SELECT four, unique1 FROM tenk1 WHERE four BETWEEN 1 AND 3 AND unique1 = 42;

                                                              QUERY PLAN
---------------------------------------------------------------------------------------------------------------------------------------
 Index Only Scan using tenk1_four_unique1_idx on tenk1  (cost=0.29..6.90 rows=1 width=8) (actual time=0.006..0.007 rows=1.00 loops=1)
   Index Cond: ((four >= 1) AND (four <= 3) AND (unique1 = 42))
   Heap Fetches: 0
   Index Searches: 3
   Buffers: shared hit=7
 Planning Time: 0.029 ms
 Execution Time: 0.012 ms
```

인덱스 검색이 `four` 값 1, 2, 3에 대해 별도로 수행됩니다.

필터 조건에 의해 제거된 행도 표시될 수 있습니다:

```sql
EXPLAIN ANALYZE SELECT * FROM tenk1 WHERE ten < 7;

                                               QUERY PLAN
---------------------------------------------------------------------------------------------------------
 Seq Scan on tenk1  (cost=0.00..470.00 rows=7000 width=244) (actual time=0.030..1.995 rows=7000.00 loops=1)
   Filter: (ten < 7)
   Rows Removed by Filter: 3000
   Buffers: shared hit=345
 Planning Time: 0.102 ms
 Execution Time: 2.145 ms
```

GiST 인덱스와 같은 "손실" 인덱스의 경우 인덱스 재검사에 의해 제거된 행이 표시됩니다:

```sql
SET enable_seqscan TO off;

EXPLAIN ANALYZE SELECT * FROM polygon_tbl WHERE f1 @> polygon '(0.5,2.0)';

                                                        QUERY PLAN
---------------------------------------------------------------------------------------------------------------------------
 Index Scan using gpolygonind on polygon_tbl  (cost=0.13..8.15 rows=1 width=85) (actual time=0.074..0.074 rows=0.00 loops=1)
   Index Cond: (f1 @> '((0.5,2))'::polygon)
   Rows Removed by Index Recheck: 1
   Index Searches: 1
   Buffers: shared hit=1
 Planning Time: 0.039 ms
 Execution Time: 0.098 ms
```

버퍼 정보는 기본적으로 포함됩니다(BUFFERS 옵션으로 제어 가능):

- shared hit: 공유 버퍼 캐시 적중
- read: 디스크에서 읽음
- written: 디스크에 쓰기
- dirtied: 수정됨

`EXPLAIN ANALYZE`는 데이터 수정 문도 분석할 수 있습니다. 단, 실제로 데이터를 변경하므로 `ROLLBACK`을 사용하는 것이 좋습니다.

```sql
BEGIN;

EXPLAIN ANALYZE UPDATE tenk1 SET hundred = hundred + 1 WHERE unique1 < 100;

                                                           QUERY PLAN
---------------------------------------------------------------------------------------------------------------------------------
 Update on tenk1  (cost=5.06..225.23 rows=0 width=0) (actual time=1.634..1.635 rows=0.00 loops=1)
   ->  Bitmap Heap Scan on tenk1  (cost=5.06..225.23 rows=100 width=10) (actual time=0.065..0.141 rows=100.00 loops=1)
         Recheck Cond: (unique1 < 100)
         Heap Blocks: exact=90
         Buffers: shared hit=4 read=2
         ->  Bitmap Index Scan on tenk1_unique1  (cost=0.00..5.04 rows=100 width=0) (actual time=0.031..0.031 rows=100.00 loops=1)
               Index Cond: (unique1 < 100)
               Index Searches: 1
               Buffers: shared read=2
 Planning Time: 0.151 ms
 Execution Time: 1.856 ms

ROLLBACK;
```

분할 테이블의 UPDATE 예:

```sql
EXPLAIN UPDATE gtest_parent SET f1 = CURRENT_DATE WHERE f2 = 101;

                                       QUERY PLAN
----------------------------------------------------------------------------------------
 Update on gtest_parent  (cost=0.00..3.06 rows=0 width=0)
   Update on gtest_child gtest_parent_1
   Update on gtest_child2 gtest_parent_2
   Update on gtest_child3 gtest_parent_3
   ->  Append  (cost=0.00..3.06 rows=3 width=14)
         ->  Seq Scan on gtest_child gtest_parent_1  (cost=0.00..1.01 rows=1 width=14)
               Filter: (f2 = 101)
         ->  Seq Scan on gtest_child2 gtest_parent_2  (cost=0.00..1.01 rows=1 width=14)
               Filter: (f2 = 101)
         ->  Seq Scan on gtest_child3 gtest_parent_3  (cost=0.00..1.01 rows=1 width=14)
               Filter: (f2 = 101)
```

Planning Time vs Execution Time:
- Planning Time: 파싱 및 재작성을 제외한 쿼리 계획 최적화 시간
- Execution Time: 실행기 시작/종료 시간, 트리거 실행 시간 포함 (파싱, 재작성, 계획 시간 제외)

`SERIALIZE` 옵션을 사용하면 쿼리 출력을 표시 가능한 형식으로 변환하는 시간을 별도로 측정할 수 있습니다:

```sql
EXPLAIN (ANALYZE, SERIALIZE ON) SELECT ...
```

#### 14.1.3 주의사항

`EXPLAIN ANALYZE`로 측정된 실행 시간이 동일한 쿼리의 정상 실행과 크게 다를 수 있는 두 가지 중요한 이유가 있습니다:

1. 네트워크 전송 비용 미포함: 출력 행이 클라이언트로 전송되지 않으므로 네트워크 전송 비용과 I/O 변환 비용이 포함되지 않습니다. `SERIALIZE` 옵션을 사용하면 I/O 변환 비용을 별도로 측정할 수 있습니다.

2. 측정 오버헤드: `EXPLAIN ANALYZE`가 추가하는 측정 오버헤드가 상당할 수 있습니다. 특히 `gettimeofday()` 운영 체제 호출이 느린 시스템에서 그렇습니다. `pg_test_timing` 도구를 사용하여 시스템의 타이밍 오버헤드를 측정할 수 있습니다.

작은 테이블에서 큰 테이블로의 외삽 위험:

```sql
EXPLAIN SELECT * FROM small_table;
```

작은 테이블(예: 디스크 페이지 하나에 들어가는 크기)에서 얻은 결과를 훨씬 큰 테이블에 그대로 적용하는 것은 위험합니다. 플래너의 비용 추정은 선형이 아니므로, 큰 테이블에서는 다른 계획을 선택할 수 있습니다.

LIMIT과의 예상-실제 불일치:

```sql
EXPLAIN ANALYZE SELECT * FROM tenk1 WHERE unique1 < 100 AND unique2 > 9000 LIMIT 2;

                                                          QUERY PLAN
------------------------------------------------------------------------------------------------------------------------------
 Limit  (cost=0.29..14.33 rows=2 width=244) (actual time=0.051..0.071 rows=2.00 loops=1)
   Buffers: shared hit=16
   ->  Index Scan using tenk2_unique2 on tenk1  (cost=0.29..70.50 rows=10 width=244) (actual time=0.051..0.070 rows=2.00 loops=1)
         Index Cond: (unique2 > 9000)
         Filter: (unique1 < 100)
         Rows Removed by Filter: 287
         Index Searches: 1
         Buffers: shared hit=16
 Planning Time: 0.077 ms
 Execution Time: 0.086 ms
```

Index Scan 노드의 예상 행 수는 10이지만 실제 행 수는 2입니다. 이는 오류가 아닙니다. 예상 수치는 노드가 완전히 실행되었을 때 반환할 행 수를 나타내는 반면, 실제 수치는 `Limit` 노드가 2행을 받은 후 실행을 중단했기 때문에 2에 그친 것입니다.

Merge Join의 측정 아티팩트:

- 머지 조인은 입력 중 하나의 일부만 읽을 수 있습니다. 다음 키 값이 다른 입력의 마지막 값보다 크면 중단할 수 있기 때문입니다.
- 외부 측에 중복 키 값이 있으면 내부 행을 백업하고 다시 스캔할 수 있어 보고된 실제 행 수가 실제 테이블 행 수보다 많을 수 있습니다.

BitmapAnd/BitmapOr 제한:

구현 제한으로 인해 `BitmapAnd` 및 `BitmapOr` 노드는 항상 실제 행 수를 0으로 보고합니다.

---

### 14.2 플래너가 사용하는 통계

#### 14.2.1 단일 칼럼 통계

쿼리 플래너는 좋은 쿼리 계획을 선택하기 위해 검색되는 행의 수를 추정해야 합니다.

통계의 한 구성요소는 각 테이블과 인덱스의 총 항목 수, 그리고 각 테이블과 인덱스가 차지하는 디스크 블록 수입니다. 이 정보는 `pg_class` 테이블의 `reltuples`와 `relpages` 칼럼에 저장됩니다.

```sql
SELECT relname, relkind, reltuples, relpages
FROM pg_class
WHERE relname LIKE 'tenk1%';

       relname        | relkind | reltuples | relpages
----------------------+---------+-----------+----------
 tenk1                | r       |     10000 |      345
 tenk1_hundred        | i       |     10000 |       11
 tenk1_thous_tenthous | i       |     10000 |       30
 tenk1_unique1        | i       |     10000 |       30
 tenk1_unique2        | i       |     10000 |       30
(5 rows)
```

reltuples와 relpages의 업데이트:

- `reltuples`와 `relpages`는 실시간으로 업데이트되지 않으므로 대부분 오래된 값을 포함합니다.
- 업데이트는 `VACUUM`, `ANALYZE`, `CREATE INDEX` 같은 DDL 명령에 의해 수행됩니다.
- 전체 테이블을 스캔하지 않는 `VACUUM` 또는 `ANALYZE` 작업은 스캔한 부분을 기반으로 `reltuples` 수를 증분적으로 업데이트합니다.
- 플래너는 현재 물리적 테이블 크기와 일치하도록 값을 스케일링합니다.

선택도(Selectivity) 추정:

대부분의 쿼리는 `WHERE` 절로 인해 테이블의 일부 행만 검색합니다. 플래너는 선택도(selectivity), 즉 `WHERE` 절의 각 조건에 일치하는 행의 비율을 추정해야 합니다. 이에 사용되는 정보는 `pg_statistic` 시스템 카탈로그에 저장됩니다.

pg_stats 뷰:

`pg_statistic`을 직접 조회하는 것보다 `pg_stats` 뷰를 사용하는 것이 좋습니다. 더 읽기 쉽게 설계되어 있고, 모든 사용자가 읽을 수 있습니다(`pg_statistic`은 슈퍼유저만 읽을 수 있음).

```sql
SELECT attname, inherited, n_distinct,
       array_to_string(most_common_vals, E'\n') as most_common_vals
FROM pg_stats
WHERE tablename = 'road';

 attname | inherited | n_distinct |          most_common_vals
---------+-----------+------------+------------------------------------
 name    | f         | -0.5681108 | I- 580                        Ramp
         |           |            | I- 880                        Ramp
         |           |            | Sp Railroad
         |           |            | I- 580
         |           |            | I- 680                        Ramp
...
```

통계 정보 양 조정:

`ANALYZE`가 `pg_statistic`에 저장하는 정보의 양, 특히 각 칼럼의 `most_common_vals`과 `histogram_bounds` 배열의 최대 항목 수는 다음과 같이 설정할 수 있습니다:

- 칼럼별 설정: `ALTER TABLE SET STATISTICS` 명령
- 전역 설정: `default_statistics_target` 설정 변수
- 기본값: 100개 항목

값을 높게 설정하면 플래너 추정이 더 정확해지며, 특히 데이터 분포가 불규칙한 칼럼에서 효과적입니다. 대신 `pg_statistic`이 차지하는 공간과 추정 계산 시간이 늘어납니다.

#### 14.2.2 확장 통계

느린 쿼리가 잘못된 실행 계획을 사용하는 흔한 원인 중 하나는 쿼리 조건에 사용된 여러 칼럼 사이에 상관관계가 있기 때문입니다.

문제점:
- 플래너는 기본적으로 여러 조건이 서로 독립적이라고 가정합니다.
- 칼럼 값이 상관관계가 있을 때는 이 가정이 성립하지 않습니다.
- 일반 통계는 칼럼별 특성상 교차 칼럼 상관관계를 캡처할 수 없습니다.

해결책:

PostgreSQL은 다변량 통계(multivariate statistics) 를 계산할 수 있습니다. 가능한 칼럼 조합의 수가 매우 크기 때문에 다변량 통계를 자동으로 계산하는 것은 비실용적입니다. 대신 통계 객체(statistics objects) 를 생성하여 서버에 흥미로운 칼럼 세트 전체에서 통계를 얻도록 지시할 수 있습니다.

프로세스:
1. `CREATE STATISTICS` 명령으로 통계 객체 생성 (카탈로그 항목만 생성)
2. `ANALYZE` 명령으로 실제 데이터 수집 (수동 또는 자동 백그라운드)
3. 수집된 값은 `pg_statistic_ext_data` 카탈로그에서 확인 가능

##### 14.2.2.1 함수 종속성 (Functional Dependencies)

함수 종속성 은 데이터베이스 정규화 정의에 사용되는 개념입니다. 칼럼 `b`가 칼럼 `a`에 함수적으로 종속 된다는 것은 `a`의 값을 알면 `b`의 값을 결정하기에 충분하다는 의미입니다.

함수 종속성의 존재는 특정 쿼리의 추정 정확도에 직접 영향을 미칩니다. 쿼리에 독립 칼럼과 종속 칼럼 모두에 대한 조건이 포함된 경우, 종속 칼럼의 조건은 결과 크기를 추가로 줄이지 않습니다. 함수 종속성을 모르면 플래너는 조건들이 독립적이라고 가정하여 결과 크기를 과소 추정합니다.

```sql
CREATE STATISTICS stts (dependencies) ON city, zip FROM zipcodes;

ANALYZE zipcodes;

SELECT stxname, stxkeys, stxddependencies
  FROM pg_statistic_ext join pg_statistic_ext_data on (oid = stxoid)
  WHERE stxname = 'stts';

 stxname | stxkeys |             stxddependencies
---------+---------+------------------------------------------
 stts    | 1 5     | {"1 => 5": 1.000000, "5 => 1": 0.423130}
(1 row)
```

해석:
- 칼럼 1 (우편번호)이 칼럼 5 (도시)를 완전히 결정: 계수 1.0
- 도시가 우편번호를 약 42% 결정: 0.423130 (58%의 도시는 둘 이상의 우편번호를 가짐)

함수 종속성의 제한사항:

함수 종속성은 현재 다음의 경우에만 적용됩니다:
- 칼럼을 상수 값과 비교하는 단순 동등 조건
- 상수 값이 있는 `IN` 절

지원하지 않음:
- 두 칼럼을 비교하는 동등 조건
- 칼럼을 표현식과 비교하는 조건
- 범위 절
- `LIKE` 또는 다른 유형의 조건

또한 플래너는 함수 종속성으로 추정할 때 관련 칼럼의 조건들이 서로 호환된다고 가정합니다. 조건이 호환되지 않는 경우(예: `city = 'San Francisco' AND zip = '90210'`)에는 정확한 추정 결과가 0행이어야 하지만, 이 가능성은 고려되지 않습니다.

##### 14.2.2.2 다변량 N-Distinct 계수

단일 칼럼 통계는 각 칼럼의 고유 값 수를 저장합니다. 그러나 둘 이상의 칼럼을 조합할 때(예: `GROUP BY a, b`) 고유 값 수 추정은 단일 칼럼 통계만 있을 경우 종종 부정확합니다.

```sql
CREATE STATISTICS stts2 (ndistinct) ON city, state, zip FROM zipcodes;

ANALYZE zipcodes;

SELECT stxkeys AS k, stxdndistinct AS nd
  FROM pg_statistic_ext join pg_statistic_ext_data on (oid = stxoid)
  WHERE stxname = 'stts2';

-[ RECORD 1 ]------------------------------------------------------
k  | 1 2 5
nd | {"1, 2": 33178, "1, 5": 33178, "2, 5": 27435, "1, 2, 5": 33178}
(1 row)
```

해석:
- 칼럼 1 (우편번호)과 칼럼 2 (상태): 33,178개의 고유 값
- 칼럼 1 (우편번호)과 칼럼 5 (도시): 33,178개의 고유 값
- 칼럼 2 (상태)와 칼럼 5 (도시): 27,435개의 고유 값
- 칼럼 1, 2, 5 모두: 33,178개의 고유 값

권장사항:

`ndistinct` 통계 객체를 생성할 때:
- 실제로 그룹화에 사용되는 칼럼 조합에만 생성합니다.
- 그룹 수의 과소 추정으로 인해 나쁜 계획이 발생하는 경우에만 생성합니다.

##### 14.2.2.3 다변량 MCV 목록

각 칼럼에 저장되는 또 다른 통계 유형은 가장 빈번한 값 목록(most-common value lists)입니다. 개별 칼럼에 대해서는 매우 정확한 추정을 제공하지만, 여러 칼럼 조건이 함께 사용되는 쿼리에서는 상당한 과소 추정이 발생할 수 있습니다.

```sql
CREATE STATISTICS stts3 (mcv) ON city, state FROM zipcodes;

ANALYZE zipcodes;

SELECT m.* FROM pg_statistic_ext join pg_statistic_ext_data on (oid = stxoid),
                pg_mcv_list_items(stxdmcv) m WHERE stxname = 'stts3';

 index |         values         | nulls | frequency | base_frequency
-------+------------------------+-------+-----------+----------------
     0 | {Washington, DC}       | {f,f} |  0.003467 |        2.7e-05
     1 | {Apo, AE}              | {f,f} |  0.003067 |        1.9e-05
     2 | {Houston, TX}          | {f,f} |  0.002167 |       0.000133
     3 | {El Paso, TX}          | {f,f} |     0.002 |       0.000113
     4 | {New York, NY}         | {f,f} |  0.001967 |       0.000114
...
(99 rows)
```

해석:

- `frequency`: 샘플에서의 실제 빈도
- `base_frequency`: 단순 칼럼별 빈도로 계산한 예상 빈도
- Washington DC가 가장 흔하며, 실제 빈도(약 0.35%)가 기본 빈도(0.0027%)보다 약 100배 높음

권장사항:

MCV 통계 객체를 생성할 때:
- 실제로 조건에서 함께 사용되는 칼럼 조합에만 생성합니다.
- 과소 추정으로 인해 나쁜 계획이 발생하는 경우에만 생성합니다.

---

### 14.3 명시적 JOIN 절로 플래너 제어하기

단순한 조인 쿼리의 예:

```sql
SELECT * FROM a, b, c WHERE a.id = b.id AND b.ref = c.id;
```

플래너는 주어진 테이블들을 어떤 순서로든 자유롭게 조인할 수 있습니다.
- A와 B를 먼저 조인한 후 C를 조인
- B와 C를 먼저 조인한 후 A를 조인
- A와 C를 먼저 조인한 후 B를 조인 (비효율적)

핵심 포인트: 이러한 다양한 조인 가능성은 의미상 동등한 결과를 내지만, 실행 비용은 크게 다를 수 있습니다.

#### JOIN 순서 문제의 확장

- 2-3개 테이블: 조인 순서 선택지가 적음
- 10개 이상 테이블: 철저한 탐색이 비현실적
- 6-7개 테이블: 계획 시간이 오래 걸릴 수 있음
- 임계값: `geqo_threshold` 런타임 파라미터로 설정
- 너무 많은 입력 테이블이 있을 때: 유전적 확률 탐색(genetic probabilistic search)으로 전환

#### OUTER JOIN과 플래너의 자유도

```sql
SELECT * FROM a LEFT JOIN (b JOIN c ON (b.ref = c.id)) ON (a.id = b.id);
```

특징:
- A의 각 행에 대해 B와 C의 조인에서 일치하는 행이 없는 경우를 포함해야 함
- 플래너는 반드시 B를 C와 조인한 후 A를 조인해야 함
- 더 빠른 계획 시간

```sql
SELECT * FROM a LEFT JOIN b ON (a.bid = b.id) LEFT JOIN c ON (a.cid = c.id);
```

- A를 B 또는 C 중 어느 것과 먼저 조인해도 유효
- FULL JOIN만이 조인 순서를 완전히 제약

명시적 INNER JOIN 문법(`INNER JOIN`, `CROSS JOIN`, 또는 `JOIN`)은 `FROM`에 관계를 나열하는 것과 의미상 동일하므로 조인 순서를 제약하지 않습니다.

#### 의미론적으로 동등한 쿼리들

```sql
-- 쿼리 1: 암시적 JOIN
SELECT * FROM a, b, c WHERE a.id = b.id AND b.ref = c.id;

-- 쿼리 2: 명시적 CROSS JOIN
SELECT * FROM a CROSS JOIN b CROSS JOIN c WHERE a.id = b.id AND b.ref = c.id;

-- 쿼리 3: 명시적 중첩 JOIN
SELECT * FROM a JOIN (b JOIN c ON (b.ref = c.id)) ON (a.id = b.id);
```

세 쿼리는 논리적으로 동등하지만 계획 시간은 다릅니다. 테이블이 3개일 때는 차이가 미미하지만, 테이블 수가 많아지면 계획 시간 차이가 매우 중요해집니다.

#### join_collapse_limit 파라미터

조인 순서를 플래너가 따르도록 강제하려면:

```sql
SET join_collapse_limit = 1;
```

효과:
- 조인 순서를 부분적으로 제약할 수 있음
- 계획 시간 단축
- 좋은 쿼리 플랜으로 유도

#### 부분적 제약 예

```sql
SELECT * FROM a CROSS JOIN b, c, d, e WHERE ...;
```

`join_collapse_limit = 1`일 때:
- A를 B와 먼저 조인하도록 강제
- 다른 선택은 제약하지 않음
- 가능한 조인 순서가 5배 감소

#### 서브쿼리 붕괴(Subquery Collapse)

```sql
SELECT *
FROM x, y,
    (SELECT * FROM a, b, c WHERE something) AS ss
WHERE somethingelse;
```

정상 동작:

```sql
SELECT * FROM x, y, a, b, c WHERE something AND somethingelse;
```

- 보통 더 나은 플랜 생성 (예: 외부 WHERE 조건이 A의 많은 행을 제거할 수 있음)
- 비용: 계획 시간 증가 (5-way vs 두 개의 3-way)

#### from_collapse_limit 파라미터

- 거대한 조인 탐색 문제에 빠지지 않기 위해 사용
- 부모 쿼리에서 `from_collapse_limit`을 초과하는 FROM 항목이 있을 때 서브쿼리 붕괴 방지

#### 두 파라미터의 관계

| 파라미터 | 기능 |
|---------|------|
| `from_collapse_limit` | 서브쿼리를 언제 평탄화할지 제어 |
| `join_collapse_limit` | 명시적 JOIN을 언제 평탄화할지 제어 |

설정 전략:

옵션 1: 동등하게 설정
```sql
join_collapse_limit = from_collapse_limit
```
(명시적 JOIN과 서브쿼리가 유사하게 동작)

옵션 2: JOIN 순서 제어
```sql
join_collapse_limit = 1
```
(명시적 JOIN으로 조인 순서 제어)

옵션 3: 미세 조정
계획 시간 vs 실행 시간의 trade-off를 위해 다르게 설정

---

### 14.4 데이터베이스 채우기

#### 14.4.1 자동 커밋 비활성화

여러 `INSERT`를 사용할 때는 자동 커밋을 끄고 마지막에 한 번만 커밋하세요. 표준 SQL에서는 시작 시 `BEGIN`을, 끝에 `COMMIT`을 실행합니다. psql 같은 일부 클라이언트 라이브러리는 이를 내부적으로 처리하므로, 실제로 효과가 있는지 확인이 필요합니다.

행을 각각 개별 트랜잭션으로 커밋하면 PostgreSQL이 행마다 상당한 작업을 수행합니다. 모든 삽입을 하나의 트랜잭션으로 묶으면 추가적인 이점도 있습니다. 한 행 삽입이 실패하면 그 시점까지 삽입된 모든 행이 롤백되므로, 부분적으로만 로드된 데이터 상태에 빠지지 않습니다.

#### 14.4.2 COPY 사용

여러 `INSERT` 대신 `COPY` 명령으로 모든 행을 한 번에 로드하세요. `COPY`는 대량 행 로드에 최적화되어 있으며, `INSERT`보다 유연성은 낮지만 오버헤드가 훨씬 적습니다. `COPY`는 단일 명령이므로 자동 커밋을 별도로 비활성화할 필요가 없습니다.

`COPY`를 사용할 수 없는 경우에는 `PREPARE`로 `INSERT` 문을 미리 준비한 뒤 필요한 만큼 `EXECUTE`를 호출하는 것이 도움이 됩니다. `INSERT`를 매번 파싱하고 계획하는 오버헤드를 줄일 수 있습니다. 각 인터페이스마다 이를 처리하는 방식이 다르므로, 인터페이스 문서에서 "준비된 문(prepared statement)"을 참고하세요.

WAL 최적화:

`COPY`는 `CREATE TABLE` 또는 `TRUNCATE` 명령과 같은 트랜잭션 안에서 사용할 때 가장 빠릅니다. 오류 발생 시 새 데이터 파일이 제거되므로 WAL 쓰기가 불필요합니다. 단, `wal_level`이 `minimal`일 때만 적용됩니다(다른 경우 모든 명령이 WAL을 작성합니다).

#### 14.4.3 인덱스 제거

새로 생성된 테이블에 로드하는 경우 가장 빠른 방법은:
1. 테이블 생성
2. `COPY`로 대량 데이터 로드
3. 필요한 인덱스 생성

기존 데이터에 인덱스를 생성하는 것이 증분으로 업데이트하는 것보다 빠릅니다.

기존 테이블에 대량 데이터를 추가하는 경우:
- 인덱스 제거 -> 테이블 로드 -> 인덱스 재생성을 고려
- 트레이드오프: 로드 속도 향상 vs 인덱스 없는 동안 다른 사용자의 성능 저하
- 주의: 유니크 인덱스는 신중히 제거 (유니크 제약 조건의 오류 검사 상실)

#### 14.4.4 외래 키 제약 조건 제거

인덱스와 마찬가지로, 외래 키 제약도 행별로 검사하는 것보다 일괄 검사가 더 효율적입니다.

절차:
1. 외래 키 제약 제거
2. 데이터 로드
3. 제약 재생성

중요 고려사항:

외래 키 제약이 있는 테이블에 데이터를 로드하면 각 새 행이 서버의 보류 중인 트리거 이벤트 목록에 항목을 추가합니다. 수백만 행을 로드할 경우 트리거 이벤트 큐가 메모리를 초과하여 과도한 스왑이나 명령 실패가 발생할 수 있습니다. 따라서 대량 데이터 로드 시 외래 키 제거는 선택이 아닌 필수입니다. 대안으로 로드 작업을 더 작은 트랜잭션으로 분할할 수 있습니다.

#### 14.4.5 maintenance_work_mem 증가

`maintenance_work_mem` 설정 변수를 일시적으로 증가시키면 대량 데이터 로드 성능이 향상됩니다.

효과:
- `CREATE INDEX` 명령어 속도 향상
- `ALTER TABLE ADD FOREIGN KEY` 명령어 속도 향상
- `COPY` 자체에는 큰 효과 없음 (위 기술 사용 시에만 유용)

#### 14.4.6 max_wal_size 증가

대량 데이터 로드 시 일반적인 체크포인트 빈도보다 더 자주 체크포인트가 발생합니다. 체크포인트 시 모든 더티 페이지를 디스크에 플러시합니다.

최적화:

`max_wal_size`를 일시적으로 증가시키면 필요한 체크포인트 수가 감소하여 속도가 향상됩니다.

#### 14.4.7 WAL 아카이빙 및 스트리밍 복제 비활성화

WAL 아카이빙 또는 스트리밍 복제를 사용하는 설치에 대량 데이터를 로드할 때:

1. `wal_level`을 `minimal`로 설정
2. `archive_mode`를 `off`로 설정
3. `max_wal_senders`를 0으로 설정

장점:
- 아카이버 또는 WAL 송신자가 WAL 데이터 처리하는 시간 절약
- 로드 완료 후 새로운 기본 백업 수행
- 특정 명령어가 WAL 작성 불필요 (더 빠른 충돌 안전성)

경고:
- 이 설정 변경에는 서버 재시작이 필요합니다
- 이전의 기본 백업은 아카이브 복구 및 스탠바이 서버에 사용할 수 없습니다
- 데이터 손실 위험이 있을 수 있습니다

#### 14.4.8 완료 후 ANALYZE 실행

테이블의 데이터 분포를 크게 변경한 후에는 `ANALYZE` 실행을 강력히 권장합니다. 대량 데이터 로드도 이에 해당합니다. `ANALYZE`(또는 `VACUUM ANALYZE`)를 실행하면 플래너에게 최신 통계를 제공합니다. 통계가 없거나 오래된 경우 플래너가 잘못된 쿼리 계획을 선택하여 성능이 저하될 수 있습니다.

자동 진공 데몬이 활성화되어 있으면 자동으로 `ANALYZE`가 실행될 수 있습니다.

#### 14.4.9 pg_dump에 대한 참고사항

`pg_dump`가 생성하는 덤프 스크립트는 위의 여러 가이드라인을 자동으로 적용합니다. 전체 스키마 및 데이터 덤프를 다시 로드할 때, 인덱스와 외래 키를 생성하기 전에 데이터를 먼저 로드합니다. 따라서 수동으로 해야 할 작업은 적절한 설정 변수를 지정하는 것뿐입니다.

수동으로 수행할 사항:

1. 설정 값 조정
```sql
maintenance_work_mem = (더 큰 값)
max_wal_size = (더 큰 값)
```

2. WAL 아카이빙 및 스트리밍 복제 비활성화
```sql
archive_mode = off
wal_level = minimal
max_wal_senders = 0
```
덤프 로드 전에 설정 변경하고, 로드 후 올바른 값으로 복원 및 새 기본 백업 수행

3. 병렬 덤프/복원 모드 실험
`pg_dump`와 `pg_restore`의 `-j` 옵션을 사용하여 최적의 동시 작업 수를 찾습니다. 직렬 모드보다 훨씬 높은 성능을 얻을 수 있습니다.

4. 단일 트랜잭션 고려
```bash
psql -1 < dump_file
# 또는
pg_restore --single-transaction dump_file
```
최소 오류 시 전체 복원이 롤백됩니다. 최적: WAL 아카이빙 끄고 단일 트랜잭션 사용

5. 병렬 복원
```bash
pg_restore --jobs=4 dump_file
```
여러 CPU 사용 가능 시 권장. 동시 데이터 로드 및 인덱스 생성이 가능합니다.

6. ANALYZE 실행
```sql
ANALYZE;
```

데이터 전용 덤프:

데이터 전용 덤프 로드 시:
- `COPY` 사용됨
- 인덱스 제거/재생성이 자동으로 되지 않음
- 외래 키도 일반적으로 건드리지 않음

로드 시 수동 작업:
1. 인덱스 및 외래 키 제거를 원하면 직접 수행
2. 데이터 로드 시 `max_wal_size` 증가 (유용)
3. `maintenance_work_mem` 증가는 인덱스/외래 키 수동 재생성 시에만
4. 완료 후 `ANALYZE` 실행

외래 키 비활성화 옵션:
```bash
pg_restore --disable-triggers dump_file
```
주의: 외래 키 유효성 검사를 연기하지 않고 제거합니다. 잘못된 데이터 삽입 가능성이 있습니다.

---

### 14.5 비영속적 설정

영속성(Durability)은 서버 충돌이나 전원 손실 시에도 커밋된 트랜잭션을 보존하는 데이터베이스 기능입니다. 그러나 영속성은 상당한 오버헤드를 수반하므로, 이러한 보장이 필요하지 않은 경우 PostgreSQL을 훨씬 빠르게 실행할 수 있습니다.

주요 주의사항: 아래 설정들은 데이터베이스 소프트웨어 충돌 시에는 여전히 영속성을 보장하며, OS 충돌 시에만 데이터 손실 또는 손상 위험이 발생합니다.

#### 비영속적 성능 향상 설정

##### 1. RAM 디스크 사용

데이터베이스 클러스터의 데이터 디렉토리를 메모리 기반 파일 시스템(RAM disk)에 배치합니다. 이렇게 하면 모든 데이터베이스 디스크 I/O를 제거할 수 있습니다.

제약사항: 사용 가능한 메모리(및 swap) 크기로 데이터 저장이 제한됩니다.

##### 2. fsync 끄기

```sql
fsync = off
```

디스크에 데이터를 플러시할 필요가 없습니다.

##### 3. synchronous_commit 끄기

```sql
synchronous_commit = off
```

모든 커밋 시 WAL 쓰기를 디스크에 강제할 필요가 없습니다.

위험성: 데이터베이스 충돌 시 트랜잭션이 손실될 수 있습니다(데이터 손상은 아님).

##### 4. full_page_writes 끄기

```sql
full_page_writes = off
```

부분 페이지 쓰기에 대한 보호가 불필요합니다.

##### 5. 체크포인트 빈도 감소

```sql
max_wal_size = (증가된 값)
checkpoint_timeout = (증가된 값)
```

체크포인트 빈도를 감소시킵니다.

트레이드오프: `/pg_wal` 저장소 요구량이 증가합니다.

##### 6. Unlogged 테이블 생성

```sql
CREATE UNLOGGED TABLE table_name (
    -- 테이블 정의
);
```

WAL 쓰기를 회피합니다.

제약사항: 테이블이 충돌에 안전하지 않습니다.

#### 성능 개선 순서

위 설정들을 조합해 사용하면 성능을 크게 향상시킬 수 있으나, 각 설정의 위험성을 충분히 이해하고 필요에 따라 선택적으로 적용해야 합니다.

---

## Chapter 15. 병렬 쿼리 (Parallel Query)

PostgreSQL은 여러 CPU를 활용하여 쿼리에 더 빠르게 응답하는 쿼리 계획을 수립할 수 있습니다. 이 기능을 병렬 쿼리(parallel query) 라고 합니다.

많은 쿼리는 병렬 쿼리의 이점을 얻을 수 없습니다. 현재 구현의 제한 사항이거나, 순차 실행보다 빠를 것으로 예상되는 병렬 계획이 존재하지 않기 때문입니다. 그러나 병렬화가 가능한 쿼리라면 속도 향상이 상당합니다. 많은 쿼리가 2배 이상 빨라지며, 일부는 4배 이상 빨라집니다. 대량의 데이터를 처리하지만 반환하는 행 수가 적은 쿼리가 일반적으로 가장 큰 이점을 얻습니다. 이 장에서는 병렬 쿼리가 언제 사용될 수 있는지, 그리고 이를 효과적으로 활용하는 방법을 설명합니다.

### 목차

- [15.1. 병렬 쿼리의 작동 방식](#151-병렬-쿼리의-작동-방식)
- [15.2. 병렬 쿼리를 사용할 수 있는 경우](#152-병렬-쿼리를-사용할-수-있는-경우)
- [15.3. 병렬 계획](#153-병렬-계획)
  - [15.3.1. 병렬 스캔](#1531-병렬-스캔)
  - [15.3.2. 병렬 조인](#1532-병렬-조인)
  - [15.3.3. 병렬 집계](#1533-병렬-집계)
  - [15.3.4. Parallel Append](#1534-parallel-append)
  - [15.3.5. 병렬 계획 팁](#1535-병렬-계획-팁)
- [15.4. 병렬 안전성](#154-병렬-안전성)
  - [15.4.1. 함수 및 집계에 대한 병렬 라벨링](#1541-함수-및-집계에-대한-병렬-라벨링)

---

### 15.1. 병렬 쿼리의 작동 방식

옵티마이저가 특정 쿼리에 대해 병렬 쿼리가 가장 빠른 실행 전략이라고 판단하면, Gather 또는 Gather Merge 노드를 포함하는 쿼리 계획을 생성합니다. 다음은 간단한 예제입니다:

```sql
EXPLAIN SELECT * FROM pgbench_accounts WHERE filler LIKE '%x%';
                                     QUERY PLAN
-------------------------------------------------------------------
 Gather  (cost=1000.00..217018.43 rows=1 width=97)
   Workers Planned: 2
   ->  Parallel Seq Scan on pgbench_accounts  (cost=0.00..216018.33 rows=1 width=97)
         Filter: (filler ~~ '%x%'::text)
(4 rows)
```

#### Gather 및 Gather Merge 노드

모든 경우에 `Gather` 또는 `Gather Merge` 노드는 정확히 하나의 자식 계획을 가지며, 이 계획은 병렬로 실행되는 계획의 일부입니다. `Gather` 또는 `Gather Merge` 노드가 계획 트리의 최상위에 있으면 전체 쿼리가 병렬로 실행됩니다. 다른 곳에 있으면 해당 노드 아래 부분만 병렬로 실행됩니다.

#### 워커 프로세스 관리

위의 예제에서 `Gather` 노드 아래에는 정확히 하나의 자식 계획이 있으므로, 쿼리의 병렬 부분을 실행하는 프로세스당 하나의 실행 계획만 있습니다. `EXPLAIN` 출력에 표시된 대로 `Workers Planned: 2`는 쿼리에 2개의 워커가 사용됨을 나타냅니다. 따라서 이 쿼리의 병렬 부분은 총 3개의 프로세스(워커 2개 + 리더 1개)에서 실행됩니다.

쿼리 실행 중 `Gather` 노드에 도달하면, 쿼리를 실행하는 프로세스(리더 프로세스)가 해당 계획에서 구성된 워커 수만큼의 백그라운드 워커 프로세스를 요청합니다. 사용되는 워커 수는 다음에 의해 제한됩니다:

- `max_parallel_workers_per_gather`: 쿼리당 최대 워커 수
- `max_worker_processes`: 시스템 전체 백그라운드 워커 최대 수
- `max_parallel_workers`: 병렬 작업을 위한 워커 최대 수

중요: 병렬 쿼리는 계획된 것보다 적은 수의 워커로 실행되거나, 리소스가 부족한 경우 워커 없이 실행될 수도 있습니다.

#### 실행 모델

쿼리의 병렬 부분을 시작하는 모든 백그라운드 워커 프로세스는 리더의 계획 복사본을 실행합니다. 이러한 워커들이 생성하는 모든 튜플은 `Gather` 노드로 전송됩니다.

리더 프로세스도 계획의 해당 부분을 실행합니다. 단, 워커로부터 튜플을 읽고 `Gather` 또는 `Gather Merge` 노드 위의 계획 노드에서 필요한 추가 처리도 수행해야 합니다.

- 리더가 생성하는 튜플이 적을 때: 추가 워커처럼 동작하여 쿼리 실행을 가속화합니다.
- 리더가 생성하는 튜플이 많을 때: 튜플 읽기와 추가 처리에 거의 전적으로 점유되어 추가 워커로서의 역할을 하지 못할 수 있습니다.

#### Gather vs. Gather Merge

- Gather: 워커로부터 튜플을 편리한 순서대로 읽습니다. 이는 정렬 순서를 파괴합니다.
- Gather Merge: 각 워커가 정렬된 튜플을 생성하면, 리더가 순서를 유지하는 병합을 수행합니다.

#### 성능 최적화

병렬 쿼리가 계획된 것보다 적은 워커로 자주 실행된다면 다음을 고려하세요:

- `max_worker_processes` 증가
- `max_parallel_workers` 증가
- `max_parallel_workers_per_gather` 감소 (더 적은 워커를 요청하도록)

---

### 15.2. 병렬 쿼리를 사용할 수 있는 경우

병렬 쿼리 계획이 생성되려면 여러 조건이 충족되어야 합니다.

#### 필수 구성 설정

병렬 쿼리 계획이 생성되려면 다음이 참이어야 합니다:

- `max_parallel_workers_per_gather` 가 0보다 큰 값 으로 설정되어야 합니다. 이는 단일 Gather 작업에 사용할 수 있는 최대 워커 수를 제어합니다.

- 시스템이 단일 사용자 모드로 실행 중이 아니어야 합니다. 단일 사용자 모드에서는 백그라운드 워커를 사용할 수 없습니다.

#### 병렬 계획 생성을 방해하는 조건

다음 조건 중 하나라도 해당되면 플래너는 쿼리에 대한 병렬 쿼리 계획을 생성하지 않습니다:

##### 1. 데이터 수정 또는 행 잠금

데이터를 쓰거나 데이터베이스 행을 잠그는 쿼리는 병렬 계획을 사용할 수 없습니다.

예외: 다음 명령은 기본 `SELECT`를 병렬화할 수 있습니다:
- `CREATE TABLE ... AS`
- `SELECT INTO`
- `CREATE MATERIALIZED VIEW`
- `REFRESH MATERIALIZED VIEW`

##### 2. 실행 중 쿼리 일시 중단

- `DECLARE CURSOR`로 생성된 커서는 병렬 계획을 사용할 수 없습니다.
- `FOR x IN query LOOP .. END LOOP`과 같은 PL/pgSQL 루프는 병렬 계획을 사용할 수 없습니다. 병렬 실행 중에 루프 코드가 안전한지 시스템이 확인할 수 없기 때문입니다.

##### 3. 안전하지 않은 함수

`PARALLEL UNSAFE`로 표시된 함수를 사용하는 쿼리는 병렬화할 수 없습니다.

- 사용자 정의 함수는 기본적으로 `PARALLEL UNSAFE`입니다 (섹션 15.4 참조).
- 대부분의 시스템 정의 함수는 `PARALLEL SAFE`입니다.

##### 4. 중첩된 병렬 쿼리

이미 병렬로 실행 중인 쿼리 내부에서 실행되는 쿼리는 병렬 계획을 사용할 수 없습니다.

병렬 쿼리에서 호출되는 함수가 자체 SQL 쿼리를 실행하면, 해당 중첩 쿼리는 병렬화되지 않습니다.

#### 런타임 실행 제한

병렬 계획이 생성되더라도 다음 경우에는 순차적으로 실행될 수 있습니다:

- 백그라운드 워커 부족: `max_worker_processes` 제한 초과
- 병렬 워커 부족: `max_parallel_workers` 제한 초과
- 확장 쿼리 프로토콜에서 0이 아닌 페치 횟수: 순차 실행으로 대체

이러한 경우, 리더 프로세스가 `Gather` 노드 아래의 계획을 단독으로 실행합니다.

---

### 15.3. 병렬 계획

PostgreSQL에서 병렬 계획을 사용하려면 "부분 계획(partial plan)" 구조가 필요합니다. 각 워커 프로세스는 출력 행의 일부만 생성하며, 필요한 각 출력 행이 정확히 하나의 프로세스에 의해 생성됩니다. 드라이빙 테이블의 스캔은 병렬 인식 스캔이어야 합니다.

#### 15.3.1. 병렬 스캔

세 가지 유형의 병렬 인식 테이블 스캔이 지원됩니다:

##### 1. Parallel Sequential Scan (병렬 순차 스캔)

- 테이블 블록이 범위로 나뉘어 협력하는 프로세스 간에 공유됩니다.
- 각 워커는 자신의 범위 스캔을 완료한 후 추가 블록을 요청합니다.

##### 2. Parallel Bitmap Heap Scan (병렬 비트맵 힙 스캔)

- 하나의 프로세스가 리더 역할을 하여 하나 이상의 인덱스를 스캔합니다.
- 방문해야 할 테이블 블록을 나타내는 비트맵을 구축합니다.
- 블록이 협력하는 프로세스 간에 나뉩니다 (힙 스캔은 병렬화되지만, 인덱스 스캔은 병렬화되지 않습니다).

##### 3. Parallel Index Scan / Parallel Index-Only Scan (병렬 인덱스 스캔 / 병렬 인덱스 전용 스캔)

- 협력하는 프로세스가 번갈아가며 인덱스에서 읽습니다.
- 현재 btree 인덱스 에서만 지원됩니다.
- 각 프로세스는 단일 인덱스 블록을 요청하고 참조된 모든 튜플을 스캔합니다.
- 결과는 각 워커 프로세스 내에서 정렬된 순서로 반환됩니다.

#### 15.3.2. 병렬 조인

드라이빙 테이블은 중첩 루프, 해시 조인 또는 병합 조인을 사용하여 다른 테이블과 조인될 수 있습니다:

##### 1. Nested Loop Join (중첩 루프 조인)

- 내부 측은 항상 비병렬입니다.
- 내부 측이 인덱스 스캔인 경우 효율적입니다 (외부 튜플/루프가 프로세스 간에 분할됨).

##### 2. Merge Join (병합 조인)

- 내부 측은 항상 비병렬입니다 (전체적으로 실행됨).
- 정렬이 필요한 경우 비효율적일 수 있습니다 (작업이 프로세스 간에 중복됨).

##### 3. Hash Join (해시 조인)

- 표준 해시 조인: 내부 측이 모든 프로세스에서 전체적으로 실행됩니다 (해시 테이블이 중복됨).
- Parallel Hash Join: 내부 측이 병렬 해시로, 공유 해시 테이블 구축 작업을 분할합니다.

#### 15.3.3. 병렬 집계

PostgreSQL은 두 단계로 병렬 집계를 지원합니다:

1. 부분 집계 (Partial Aggregation): 각 워커가 집계 단계를 수행하여 그룹당 부분 결과를 생성합니다 (`Partial Aggregate` 노드).

2. 데이터 전송: 부분 결과가 `Gather` 또는 `Gather Merge`를 통해 리더로 전송됩니다.

3. 최종 집계 (Finalize Aggregation): 리더가 결과를 다시 집계하여 최종 결과를 생성합니다 (`Finalize Aggregate` 노드).

##### 제한 사항

- 모든 상황에서 지원되지 않습니다.
- 각 집계는 병렬 처리에 안전해야 하며 결합 함수(combine function)가 있어야 합니다.
- `internal` 전이 상태를 가진 집계는 직렬화/역직렬화 함수가 필요합니다.
- 집계에 `DISTINCT` 또는 `ORDER BY` 절이 포함된 경우 지원되지 않습니다.
- 순서 집합 집계(ordered set aggregates)나 `GROUPING SETS`에는 지원되지 않습니다.
- 모든 조인이 병렬 부분의 일부인 경우에만 사용할 수 있습니다.

#### 15.3.4. Parallel Append

여러 소스의 행을 단일 결과 집합으로 결합할 때 사용됩니다 (예: `UNION ALL`, 파티션된 테이블):

- 일반 Append: 모든 프로세스가 자식 계획을 순서대로 순차적으로 실행합니다.
- Parallel Append: 프로세스가 자식 계획에 고르게 분산되어 동시에 여러 개를 실행합니다.
  - 경합 및 시작 비용을 피합니다.
  - 부분(partial) 및 비부분(non-partial) 자식 계획을 모두 가질 수 있습니다.
  - 비부분 자식은 단일 프로세스에서만 스캔됩니다.

기능 제어: `enable_parallel_append`를 사용하여 이 기능을 비활성화할 수 있습니다.

#### 15.3.5. 병렬 계획 팁

##### 쿼리가 병렬 계획을 생성하지 않는 경우

- `parallel_setup_cost` 또는 `parallel_tuple_cost` 구성 매개변수를 줄이세요.
- 둘 다 0으로 설정하면 문제 진단에 도움이 될 수 있습니다.
- 제한 사항에 대해서는 섹션 15.2 (병렬 쿼리를 사용할 수 있는 경우)와 섹션 15.4 (병렬 안전성)를 참조하세요.

##### 성능 분석

다음 명령을 사용하여 각 계획 노드에 대한 워커별 통계를 표시하고 작업이 고르게 분산되는지 확인하세요:

```sql
EXPLAIN (ANALYZE, VERBOSE)
```

---

### 15.4. 병렬 안전성

PostgreSQL 플래너는 쿼리의 작업을 세 가지 병렬 안전성 범주로 분류합니다:

#### 병렬 안전성 범주

1. Parallel Safe (병렬 안전): 병렬 쿼리 실행과 충돌하지 않는 작업

2. Parallel Restricted (병렬 제한): 병렬 워커에서는 실행할 수 없지만 병렬 쿼리 실행 중 리더 프로세스에서는 실행할 수 있는 작업

3. Parallel Unsafe (병렬 안전하지 않음): 병렬 쿼리 실행 중에 전혀 실행할 수 없는 작업으로, 전체 쿼리에 대해 병렬 쿼리를 완전히 비활성화합니다.

#### 항상 병렬 제한되는 작업

- 공통 테이블 표현식(CTE)의 스캔
- 임시 테이블의 스캔
- 외부 테이블의 스캔 (외부 데이터 래퍼가 `IsForeignScanParallelSafe` API를 통해 다르게 표시하지 않는 한)
- 상관 `SubPlan`을 참조하는 계획 노드

#### 15.4.1. 함수 및 집계에 대한 병렬 라벨링

##### 자동 분류의 한계

플래너는 사용자 정의 함수나 집계의 병렬 안전성을 자동으로 결정할 수 없습니다. 이 결정은 함수가 수행할 수 있는 모든 가능한 작업을 예측해야 하며, 이는 정지 문제(Halting Problem)와 동등합니다.

기본 동작: 모든 사용자 정의 함수는 명시적으로 표시하지 않는 한 `PARALLEL UNSAFE`로 간주됩니다.

##### CREATE/ALTER FUNCTION을 사용한 수동 표시

함수를 생성하거나 변경할 때 다음 절을 사용합니다:

```sql
CREATE FUNCTION ... PARALLEL SAFE | PARALLEL RESTRICTED | PARALLEL UNSAFE
ALTER FUNCTION ... PARALLEL SAFE | PARALLEL RESTRICTED | PARALLEL UNSAFE
```

##### CREATE AGGREGATE를 사용한 수동 표시

```sql
CREATE AGGREGATE ... ( PARALLEL = SAFE | PARALLEL = RESTRICTED | PARALLEL = UNSAFE, ... )
```

#### 함수 표시 요구 사항

##### PARALLEL UNSAFE로 표시해야 하는 경우

함수가 다음을 수행하는 경우:
- 데이터베이스에 쓰기
- 트랜잭션 상태 변경 (오류 복구를 위한 서브트랜잭션 제외)
- 시퀀스에 액세스
- 설정에 영구적인 변경을 가함

##### PARALLEL RESTRICTED로 표시해야 하는 경우

함수가 다음에 액세스하는 경우:
- 임시 테이블
- 클라이언트 연결 상태
- 커서 또는 준비된 문
- 워커 간에 동기화할 수 없는 기타 백엔드 로컬 상태
- 예: `setseed` 및 `random`

#### 중요 참고 사항

- 잘못된 라벨링의 위험: 잘못 라벨링된 함수는 병렬 쿼리에서 오류를 발생시키거나 잘못된 결과를 생성할 수 있습니다.

- 잠금 동작: 병렬 워커가 획득한 잠금(리더가 보유하지 않은 잠금)은 트랜잭션 종료 시가 아니라 워커 종료 시 해제됩니다.

- 플래너 제한 사항: 쿼리 플래너는 더 나은 계획을 위해 병렬 제한 함수의 평가를 지연시키지 않습니다. 예를 들어, `WHERE` 절이 병렬 제한으로 분류되면, 그렇게 하는 것이 효율적일지라도 테이블 스캔이 계획의 병렬 부분에 포함되지 않습니다.

- 확실하지 않을 때: 안전을 위해 함수를 `UNSAFE`로 라벨링하세요.

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Parallel Query](https://www.postgresql.org/docs/18/parallel-query.html)
- [15.1. How Parallel Query Works](https://www.postgresql.org/docs/18/how-parallel-query-works.html)
- [15.2. When Can Parallel Query Be Used?](https://www.postgresql.org/docs/18/when-can-parallel-query-be-used.html)
- [15.3. Parallel Plans](https://www.postgresql.org/docs/18/parallel-plans.html)
- [15.4. Parallel Safety](https://www.postgresql.org/docs/18/parallel-safety.html)
