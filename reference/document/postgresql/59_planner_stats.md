# Chapter 75: 플래너가 통계를 사용하는 방법 (How the Planner Uses Statistics)

이 장에서는 PostgreSQL 쿼리 플래너(Query Planner)가 통계 정보를 활용하여 최적의 실행 계획을 수립하는 방법에 대해 설명합니다.

## 목차

1. [개요](#개요)
2. [단일 컬럼 통계](#단일-컬럼-통계)
3. [행 추정 예제](#행-추정-예제)
4. [확장 통계](#확장-통계)
5. [모범 사례](#모범-사례)

---

## 개요

쿼리 플래너는 쿼리가 검색할 행의 수를 추정하여 최적의 실행 계획을 선택합니다. 이러한 통계 정보는 시스템 카탈로그에 저장되며, `VACUUM`, `ANALYZE`, 그리고 DDL 명령에 의해 업데이트됩니다.

플래너가 사용하는 주요 통계 정보:
- 행 수(Row Count): 테이블에 포함된 총 행의 수
- 디스크 페이지 수(Disk Page Count): 테이블이 차지하는 디스크 페이지 수
- 선택도(Selectivity): WHERE 조건을 만족하는 행의 비율
- 가장 빈번한 값(Most Common Values, MCV): 컬럼에서 가장 자주 나타나는 값들
- 히스토그램(Histogram): 값의 분포를 나타내는 버킷 경계값들

---

## 단일 컬럼 통계

### 기본 통계 저장

행 수와 디스크 블록 수는 `pg_class` 시스템 카탈로그의 `reltuples`와 `relpages` 컬럼에 저장됩니다.

```sql
SELECT relname, relkind, reltuples, relpages
FROM pg_class
WHERE relname LIKE 'tenk1%';
```

결과 예시:
```
    relname     | relkind | reltuples | relpages
----------------+---------+-----------+----------
 tenk1          | r       |     10000 |      345
 tenk1_hundred  | i       |     10000 |       11
```

주요 사항:
- `reltuples`와 `relpages`는 실시간으로 업데이트되지 않습니다
- `VACUUM`, `ANALYZE`, DDL 명령에 의해 업데이트됩니다
- 플래너는 현재 물리적 테이블 크기에 맞게 이 값들을 스케일링하여 더 나은 근사치를 얻습니다

### 선택도 통계 (Selectivity Statistics)

플래너는 `pg_statistic` 시스템 카탈로그에 저장된 데이터를 사용하여 WHERE 조건과 일치하는 행의 비율인 선택도(Selectivity) 를 추정합니다. 이 데이터는 `ANALYZE`와 `VACUUM ANALYZE`에 의해 업데이트됩니다.

권장 대안: 슈퍼유저 권한이 필요 없고 더 읽기 쉬운 `pg_stats` 뷰를 사용하세요.

```sql
SELECT attname, inherited, n_distinct,
       array_to_string(most_common_vals, E'\n') as most_common_vals
FROM pg_stats
WHERE tablename = 'road';
```

### 통계 구성

컬럼별로 수집되는 통계의 양을 제어할 수 있습니다:

```sql
ALTER TABLE table_name ALTER COLUMN column_name SET STATISTICS value;
```

또는 `default_statistics_target` 구성 변수를 통해 전역적으로 설정할 수 있습니다 (기본값: 100개 항목).

---

## 행 추정 예제

이 섹션에서는 플래너가 통계를 사용하여 행 수를 추정하는 방법을 구체적인 예제를 통해 설명합니다.

### 예제 1: 범위 조건 (Range Conditions)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE unique1 < 1000;
```

과정:
1. 플래너가 `<` 연산자의 선택도 함수(`scalarltsel`)를 조회합니다
2. `pg_stats`에서 히스토그램을 검색합니다:

```sql
SELECT histogram_bounds FROM pg_stats
WHERE tablename='tenk1' AND attname='unique1';

-- 결과: {0,993,1997,3050,4040,5036,5957,7057,8029,9016,9995}
```

3. 히스토그램 버킷 내에서 선형 분포를 사용하여 선택도를 계산합니다:

```
선택도 = (1 + (1000 - 993)/(1997 - 993))/10
       = 0.100697

추정 행 수 = 10000 * 0.100697 = 1007
```

### 예제 2: MCV를 사용한 등호 조건 (Equality with Common Values)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 = 'CRAAAA';
```

과정:
가장 빈번한 값(MCV)과 빈도를 사용합니다:

```sql
SELECT null_frac, n_distinct, most_common_vals, most_common_freqs
FROM pg_stats
WHERE tablename='tenk1' AND attname='stringu1';
```

결과:
```
most_common_vals  | {EJAAAA,BBAAAA,CRAAAA,...}
most_common_freqs | {0.00333333,0.003,0.003,...}

선택도 = 0.003
추정 행 수 = 10000 * 0.003 = 30
```

### 예제 3: MCV에 없는 값의 등호 조건

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 = 'xxx';
```

과정:
값이 MCV 목록에 없으므로 다음 공식을 사용합니다:

```
선택도 = (1 - sum(mcv_freqs))/(num_distinct - num_mcv)
       = (1 - 0.03033333)/(676 - 10)
       = 0.0014559

추정 행 수 = 10000 * 0.0014559 = 15
```

### 예제 4: MCV와 히스토그램을 사용한 범위 조건

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 WHERE stringu1 < 'IAAAAA';
```

과정:
MCV와 히스토그램 추정을 결합합니다:

```
selectivity_mcv = 0.01833333 (일치하는 MCV 빈도의 합)
selectivity_histogram = 0.298387 * 0.96966667
선택도 = 0.01833333 + 0.298387 * 0.96966667 = 0.307669

추정 행 수 = 10000 * 0.307669 = 3077
```

### 예제 5: 다중 조건 (Multiple Conditions)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1
WHERE unique1 < 1000 AND stringu1 = 'xxx';
```

과정:
독립성을 가정하고 선택도를 곱합니다:

```
선택도 = 0.100697 * 0.0014559 = 0.0001466
추정 행 수 = 10000 * 0.0001466 = 1
```

### 예제 6: 조인 연산 (Join Operations)

쿼리:
```sql
EXPLAIN SELECT * FROM tenk1 t1, tenk2 t2
WHERE t1.unique1 < 50 AND t1.unique2 = t2.unique2;
```

tenk1에 대한 필터:
```
선택도 = (0 + (50 - 0)/(993 - 0))/10 = 0.005035
행 수 = 10000 * 0.005035 = 50
```

조인 선택도 (`eqjoinsel` 사용):
```sql
SELECT tablename, null_frac, n_distinct, most_common_vals
FROM pg_stats
WHERE tablename IN ('tenk1', 'tenk2') AND attname='unique2';
```

유니크 컬럼의 경우:
```
선택도 = (1 - 0) * (1 - 0) / max(10000, 10000) = 0.0001

행 수 = (50 * 10000) * 0.0001 = 50
```

---

## 확장 통계

확장 통계(Extended Statistics)는 일반 단일 컬럼 통계로는 감지할 수 없는 컬럼 간 상관관계를 캡처합니다. `CREATE STATISTICS` 명령으로 생성되며, 실제 데이터 수집은 `ANALYZE` 시 발생합니다.

### 1. 함수적 종속성 (Functional Dependencies)

컬럼 `b`가 컬럼 `a`에 함수적으로 종속되는 경우를 추적합니다 (`a`의 값을 알면 `b`의 값이 결정됨).

#### 문제 상황

확장 통계가 없으면 플래너는 컬럼 조건이 독립적이라고 가정하여 상관관계가 있는 경우 심각하게 과소추정합니다.

예제 설정:
```sql
CREATE TABLE t (a INT, b INT);
INSERT INTO t SELECT i % 100, i % 100 FROM generate_series(1, 10000) s(i);
ANALYZE t;
```

통계 없이 실행:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 1;

-- 추정: 1행 (0.01% 선택도 - 부정확)
-- 실제: 100행
```

플래너가 개별 선택도를 곱합니다 (1% x 1% = 0.01%), 함수적 종속성을 놓칩니다.

#### 해결책

```sql
CREATE STATISTICS stts (dependencies) ON a, b FROM t;
ANALYZE t;

EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 1;

-- 추정: 100행 (정확!)
```

통계 확인:
```sql
SELECT stxname, stxkeys, stxddependencies
FROM pg_statistic_ext JOIN pg_statistic_ext_data ON (oid = stxoid)
WHERE stxname = 'stts';
```

출력:
```
stxname | stxkeys |             stxddependencies
--------+---------+--------------------------------------------
stts    | 1 5     | {"1 => 5": 1.000000, "5 => 1": 0.423130}
```

이는 컬럼 1(ZIP 코드)이 컬럼 5(도시)를 계수 1.0으로 완전히 결정하고, 도시가 ZIP 코드를 42.3%만 결정함을 보여줍니다.

제한사항:
- 컬럼과 상수를 비교하는 단순 등호 조건과 `IN` 절에만 적용됩니다
- 컬럼 간 비교, 범위 절, `LIKE`, 기타 조건 유형에는 사용되지 않습니다
- 실제로 호환되지 않는 조건을 감지할 수 없습니다 (예: `city='San Francisco' AND zip='90210'`)

### 2. 다변량 N-Distinct 수 (Multivariate N-Distinct Counts)

컬럼 조합에 대한 고유 값 수를 수집하여 여러 컬럼이 있는 GROUP BY 쿼리의 추정을 개선합니다.

단일 컬럼 (정확):
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a;

-- 추정 행 수: 100 (정확)
```

다중 컬럼 (통계 없이):
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a, b;

-- 추정: 1000행 (실제: 100 - 10배 차이)
```

해결책:
```sql
CREATE STATISTICS stts (dependencies, ndistinct) ON a, b FROM t;
ANALYZE t;

EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT COUNT(*) FROM t GROUP BY a, b;

-- 추정: 100행 (정확!)
```

통계 확인:
```sql
SELECT stxkeys AS k, stxdndistinct AS nd
FROM pg_statistic_ext JOIN pg_statistic_ext_data ON (oid = stxoid)
WHERE stxname = 'stts';
```

출력:
```
   k   |                                   nd
-------+------------------------------------------------------------------------
 1 2 5 | {"1, 2": 33178, "1, 5": 33178, "2, 5": 27435, "1, 2, 5": 33178}
```

### 3. 다변량 MCV 목록 (Multivariate MCV Lists)

컬럼 조합에 대한 가장 빈번한 값 목록을 수집하여 다중 컬럼 조건에 대한 매우 정확한 추정을 제공합니다.

MCV 통계 생성:
```sql
CREATE STATISTICS stts2 (mcv) ON a, b FROM t;
ANALYZE t;
```

MCV 목록 검사:
```sql
SELECT m.* FROM pg_statistic_ext
JOIN pg_statistic_ext_data ON (oid = stxoid),
     pg_mcv_list_items(stxdmcv) m
WHERE stxname = 'stts2';
```

출력:
```
 index |  values  | nulls | frequency | base_frequency
-------+----------+-------+-----------+----------------
     0 | {0, 0}   | {f,f} |      0.01 |         0.0001
     1 | {1, 1}   | {f,f} |      0.01 |         0.0001
   ...
    99 | {99, 99} | {f,f} |      0.01 |         0.0001
```

#### MCV 목록의 장점

1. 호환되지 않는 값 조합 감지:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a = 1 AND b = 10;

-- 추정: 1행 (정확 - 일치하는 조합이 없음)
-- 실제: 0행
-- (함수적 종속성은 여전히 1%로 추정할 것임)
```

2. 비등호 절 처리:
```sql
EXPLAIN (ANALYZE, TIMING OFF, BUFFERS OFF)
SELECT * FROM t WHERE a <= 49 AND b > 49;

-- 추정: 1행 (정확 - 일치하는 조합 없음)
-- 실제: 0행
-- (함수적 종속성은 등호 조건에서만 작동)
```

### 확장 통계 비교

| 기능 | 함수적 종속성 (Functional Dependencies) | MCV 목록 (MCV Lists) |
|------|----------------------------------------|---------------------|
| 비용 | 매우 저렴 | 더 비쌈 |
| 저장 공간 | 최소 | 더 큼 |
| 절 유형 | 등호만 | 모든 유형 (범위, 부등호 등) |
| 세분화 | 컬럼 수준만 | 개별 값 |
| 호환되지 않는 값 | 감지 불가 | 감지 가능 |

---

## 모범 사례

1. 필요한 경우에만 확장 통계 생성: 쿼리에서 실제로 사용되는 컬럼 그룹에 대해서만 확장 통계를 생성하세요.

2. 함수적 종속성 사용: 강하게 상관된 컬럼에 함수적 종속성을 사용하세요.

3. N-Distinct 통계 적용: GROUP BY에서 사용되는 컬럼 조합에 ndistinct 통계를 적용하세요.

4. MCV 통계 신중히 생성: 잘못된 추정이 나쁜 계획을 유발할 때만 MCV 통계를 생성하세요.

5. 통계 타겟 조정: 불규칙한 데이터 분포를 가진 컬럼에 대해 통계 타겟을 증가시켜 더 나은 정확도를 얻으세요.

```sql
-- 특정 컬럼의 통계 타겟 증가
ALTER TABLE my_table ALTER COLUMN my_column SET STATISTICS 500;

-- 분석 실행
ANALYZE my_table;
```

6. 정기적인 ANALYZE 실행: 데이터 분포가 크게 변경된 후에는 `ANALYZE`를 실행하여 통계를 최신 상태로 유지하세요.

```sql
-- 특정 테이블 분석
ANALYZE my_table;

-- 특정 컬럼만 분석
ANALYZE my_table (column1, column2);
```

---

## 소스 코드 참조

플래너 통계와 관련된 PostgreSQL 소스 코드:

- 테이블 크기 추정: `src/backend/optimizer/util/plancat.c`
- 절 선택도 로직: `src/backend/optimizer/path/clausesel.c`
- 연산자별 함수: `src/backend/utils/adt/selfuncs.c`

---

## 참고 자료

- [PostgreSQL 공식 문서: Using EXPLAIN](https://www.postgresql.org/docs/current/using-explain.html)
- [PostgreSQL 공식 문서: pg_stats 뷰](https://www.postgresql.org/docs/current/view-pg-stats.html)
- [PostgreSQL 공식 문서: CREATE STATISTICS](https://www.postgresql.org/docs/current/sql-createstatistics.html)
- [PostgreSQL 공식 문서: ANALYZE](https://www.postgresql.org/docs/current/sql-analyze.html)
