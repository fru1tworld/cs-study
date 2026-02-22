# Chapter 62. 유전 쿼리 최적화기 (Genetic Query Optimizer)

PostgreSQL의 유전 쿼리 최적화기(GEQO, Genetic Query Optimizer)는 복잡한 조인 쿼리에 대한 효율적인 실행 계획을 탐색하기 위해 유전 알고리즘을 사용하는 쿼리 최적화 모듈입니다.

---

## 목차

1. [복잡한 최적화 문제로서의 쿼리 처리](#1-복잡한-최적화-문제로서의-쿼리-처리)
2. [유전 알고리즘](#2-유전-알고리즘)
3. [PostgreSQL에서의 유전 쿼리 최적화](#3-postgresql에서의-유전-쿼리-최적화)
4. [GEQO 설정 파라미터](#4-geqo-설정-파라미터)
5. [예제](#5-예제)
6. [참고 문헌](#6-참고-문헌)

---

## 1. 복잡한 최적화 문제로서의 쿼리 처리

### 1.1 조인 최적화의 어려움

관계형 데이터베이스에서 가장 처리와 최적화가 어려운 연산자는 조인(Join) 입니다. 조인 최적화가 복잡한 이유는 다음과 같습니다:

- 지수적 증가(Exponential Growth): 쿼리에 포함된 조인의 수가 증가할수록 가능한 쿼리 계획의 수는 기하급수적으로 증가합니다
- 다양한 조인 방법: PostgreSQL은 중첩 루프 조인(Nested Loop Join), 해시 조인(Hash Join), 병합 조인(Merge Join)을 지원합니다
- 다양한 인덱스 유형: B-tree, Hash, GiST, GIN 인덱스 등이 서로 다른 접근 경로를 제공합니다
- 탐색 복잡도: 전통적인 쿼리 최적화는 모든 대안 전략에 대한 철저한 탐색이 필요합니다

### 1.2 전통적인 PostgreSQL 쿼리 최적화기

PostgreSQL의 표준 쿼리 최적화기는 대안 전략들에 대해 거의 완전한 탐색(Near-Exhaustive Search) 을 수행합니다:

- 기원: IBM의 System R 데이터베이스에서 처음 도입
- 결과: 거의 최적에 가까운 조인 순서를 생성
- 한계: 조인 수가 많을 경우 지수적인 시간과 메모리 요구로 인해 실행 불가능

### 1.3 유전 알고리즘의 필요성

전통적인 접근 방식은 다음과 같은 경우에 부적합합니다:

- 대규모 조인 쿼리 (많은 테이블 포함)
- 복잡한 추론이 필요한 의사 결정 지원 시스템
- 상당한 쿼리 부하가 있는 지식 기반 시스템

해결책: 많은 수의 조인을 포함하는 쿼리에서 조인 순서 문제를 효율적으로 해결하기 위한 유전 알고리즘 구현

조인 가능한 테이블 수에 따른 가능한 조인 순서의 수:

| 테이블 수 | 가능한 조인 순서 |
|-----------|------------------|
| 2 | 2 |
| 3 | 12 |
| 4 | 120 |
| 5 | 1,680 |
| 6 | 30,240 |
| 7 | 665,280 |
| 10 | 17,643,225,600 |
| 12 | 약 1.76 × 10^13 |

---

## 2. 유전 알고리즘

### 2.1 개요

유전 알고리즘(Genetic Algorithm, GA)은 무작위 탐색을 통해 작동하는 휴리스틱 최적화 방법(Heuristic Optimization Method) 입니다. PostgreSQL의 GEQO는 복잡한 쿼리 최적화 문제에 대한 최적 솔루션을 찾기 위해 이 알고리즘을 사용합니다.

### 2.2 핵심 개념

#### 개체군과 적합도 (Population and Fitness)

- 가능한 솔루션의 집합은 개체(Individual) 들의 개체군(Population) 으로 간주됩니다
- 각 개체가 환경에 얼마나 잘 적응했는지는 적합도(Fitness) 로 지정됩니다

#### 유전적 구조 (Genetic Structure)

- 염색체(Chromosome): 탐색 공간에서 개체의 좌표를 나타냅니다. 본질적으로 문자열의 집합입니다
- 유전자(Gene): 최적화되는 단일 파라미터의 값을 인코딩하는 염색체의 하위 섹션입니다
- 일반적인 인코딩: 이진(Binary) 또는 정수(Integer) 표현

### 2.3 진화 연산 (Evolutionary Operations)

알고리즘은 세 가지 주요 연산을 통해 탐색 지점의 새로운 세대를 생성합니다:

1. 재조합(Recombination): 여러 개체의 유전 물질을 결합
2. 돌연변이(Mutation): 유전 물질에 무작위 변화를 적용
3. 선택(Selection): 더 높은 적합도를 가진 개체를 선택하여 번식

### 2.4 중요한 구분

comp.ai.genetic FAQ에 따르면, GA는 순수한 무작위 탐색이 아닙니다:

> "GA는 확률적 과정을 사용하지만, 결과는 명백히 비무작위적입니다 (무작위보다 더 나음)."

이는 무작위화가 관여하지만, 유전 알고리즘이 단순한 무작위 탐색에 비해 세대를 거듭하면서 체계적으로 더 나은 솔루션을 생성한다는 것을 의미합니다.

### 2.5 유전 알고리즘의 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│                    초기 개체군 생성                          │
│              (무작위 조인 순서 생성)                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    적합도 평가                               │
│           (각 조인 순서의 실행 비용 계산)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  종료 조건 충족? │
                    └─────────────────┘
                      │           │
                     예          아니오
                      │           │
                      ▼           ▼
            ┌──────────────┐   ┌─────────────────────────────┐
            │ 최적 계획 반환 │   │         선택               │
            └──────────────┘   │  (높은 적합도 개체 선택)     │
                               └─────────────────────────────┘
                                              │
                                              ▼
                               ┌─────────────────────────────┐
                               │         재조합              │
                               │  (에지 재조합 교차 사용)     │
                               └─────────────────────────────┘
                                              │
                                              ▼
                               ┌─────────────────────────────┐
                               │       새로운 세대 생성       │
                               └─────────────────────────────┘
                                              │
                                              └──────────────┐
                                                             │
                              ┌───────────────────────────────┘
                              │
                              ▼
                    (적합도 평가로 돌아감)
```

---

## 3. PostgreSQL에서의 유전 쿼리 최적화

### 3.1 개요

GEQO(Genetic Query Optimization)는 쿼리 최적화를 외판원 문제(Traveling Salesman Problem, TSP) 로 접근하는 PostgreSQL의 모듈입니다. 가능한 쿼리 계획을 조인 순서를 나타내는 정수 문자열로 인코딩합니다.

### 3.2 조인 순서 인코딩

쿼리 계획은 각 숫자가 릴레이션 ID를 나타내는 정수 문자열로 표현됩니다:

```
예제 조인 트리:
      /\
     /\ 2
    /\ 3
   4  1

인코딩: '4-1-3-2'
(릴레이션 4를 1과 조인, 그 다음 3, 그 다음 2)
```

### 3.3 PostgreSQL GEQO의 주요 특성

1. 정상 상태 GA (Steady State GA): 전체 세대 교체 대신 가장 적합도가 낮은 개체를 교체하여 빠른 수렴을 가능하게 합니다

2. 에지 재조합 교차 (Edge Recombination Crossover): 최소한의 에지 손실로 TSP 솔루션에 특히 적합합니다

3. 돌연변이 연산자 없음: 합법적인 투어를 생성하기 위한 복구 메커니즘이 필요 없습니다

### 3.4 계획 생성 과정

1. 표준 플래너 코드를 사용하여 개별 릴레이션 스캔을 생성합니다
2. 초기 무작위 조인 순서를 생성합니다
3. 각 순서에 대해 표준 플래너를 호출하여 실행 비용을 추정합니다
4. 각 단계에서 세 가지 가능한 조인 전략을 모두 평가합니다
5. 가장 적합도가 낮은 후보를 폐기합니다
6. 저비용 순서의 일부를 결합하여 새로운 후보를 생성합니다
7. 사전 설정된 수의 순서가 평가될 때까지 반복합니다
8. 발견된 최적의 계획을 사용합니다

### 3.5 GEQO 소스 코드 구조

주요 루틴은 `src/backend/optimizer/geqo/` 디렉토리에 있습니다:

| 파일 | 설명 |
|------|------|
| `geqo_main.c` | GEQO 메인 루틴 |
| `geqo_pool.c` | 개체군 풀 관리 |
| `geqo_selection.c` | 선택 연산 |
| `geqo_recombination.c` | 재조합 연산 |
| `geqo_erx.c` | 에지 재조합 교차 |
| `geqo_ox1.c`, `geqo_ox2.c` | 순서 교차 연산 |
| `geqo_pmx.c` | 부분 매핑 교차 |
| `geqo_random.c` | 난수 생성 |

### 3.6 알려진 제한 사항

1. 비용 재계산: 각 후보에 대해 비용 추정을 다시 계산해야 하므로 반복 작업이 발생합니다

2. 메모리 문제: 하위 조인 비용 추정을 캐싱할 때 메모리 문제가 발생할 수 있습니다

3. TSP 적합성: TSP 알고리즘이 쿼리 최적화에 이상적이지 않을 수 있습니다 (하위 문자열의 비용이 TSP와 달리 컨텍스트에 의존)

4. 에지 재조합 효과: 쿼리 최적화에 대한 에지 재조합 교차의 효과가 의문시됩니다

---

## 4. GEQO 설정 파라미터

### 4.1 geqo (boolean)

유전 쿼리 최적화를 활성화하거나 비활성화합니다.

```sql
-- GEQO 비활성화
SET geqo = off;

-- GEQO 활성화 (기본값)
SET geqo = on;
```

- 기본값: `on`
- 참고: 프로덕션 환경에서는 일반적으로 끄지 않는 것이 좋습니다. 더 세밀한 제어를 위해서는 `geqo_threshold`를 사용하세요

### 4.2 geqo_threshold (integer)

이 수 이상의 FROM 항목이 포함된 쿼리에 대해 유전 쿼리 최적화를 사용합니다.

```sql
-- 8개 이상의 테이블 조인에 GEQO 사용
SET geqo_threshold = 8;

-- 기본값: 12개 이상의 테이블에서 GEQO 사용
SET geqo_threshold = 12;
```

- 기본값: `12`
- 참고: `FULL OUTER JOIN`은 단일 FROM 항목으로 계산됩니다. 단순한 쿼리의 경우 일반적인 철저한 탐색 플래너가 더 좋으며, 많은 테이블이 있는 복잡한 쿼리의 경우 GEQO가 과도한 계획 시간을 방지합니다

### 4.3 geqo_effort (integer)

계획 시간과 쿼리 계획 품질 간의 균형을 제어합니다.

```sql
-- 최소 노력 (빠른 계획, 품질 저하 가능)
SET geqo_effort = 1;

-- 기본값
SET geqo_effort = 5;

-- 최대 노력 (느린 계획, 더 나은 품질)
SET geqo_effort = 10;
```

- 범위: 1 ~ 10
- 기본값: `5`
- 참고: 큰 값은 계획 시간을 증가시키지만 효율적인 계획의 가능성을 높입니다. 직접적인 동작에 영향을 미치지 않으며 다른 GEQO 변수의 기본값을 계산하는 데만 사용됩니다

### 4.4 geqo_pool_size (integer)

유전 개체군 크기 (개체 수)를 제어합니다.

```sql
-- 작은 풀 크기 (빠른 실행)
SET geqo_pool_size = 128;

-- 큰 풀 크기 (더 나은 탐색)
SET geqo_pool_size = 1024;

-- 자동 결정 (기본값)
SET geqo_pool_size = 0;
```

- 최소값: 2
- 일반적인 범위: 100 ~ 1000
- 기본값: `0` (geqo_effort와 테이블 수에 따라 자동 선택)

### 4.5 geqo_generations (integer)

알고리즘의 반복 횟수를 제어합니다.

```sql
-- 더 많은 세대 (더 나은 결과, 더 오래 걸림)
SET geqo_generations = 500;

-- 자동 결정 (기본값)
SET geqo_generations = 0;
```

- 최소값: 1
- 일반적인 범위: 풀 크기와 동일
- 기본값: `0` (geqo_pool_size에 따라 자동 선택)

### 4.6 geqo_selection_bias (floating point)

개체군 내 선택 압력을 제어합니다.

```sql
-- 낮은 선택 압력
SET geqo_selection_bias = 1.5;

-- 높은 선택 압력 (기본값)
SET geqo_selection_bias = 2.0;
```

- 범위: 1.50 ~ 2.00
- 기본값: `2.0`

### 4.7 geqo_seed (floating point)

조인 순서 탐색 공간을 통한 무작위 경로를 선택하는 데 사용되는 난수 생성기의 초기값입니다.

```sql
-- 특정 시드 설정 (재현 가능한 결과)
SET geqo_seed = 0.5;

-- 기본값
SET geqo_seed = 0;
```

- 범위: 0 ~ 1
- 기본값: `0`
- 참고: 이 값을 변경하면 탐색되는 조인 경로 집합이 변경되어 더 나은 또는 더 나쁜 계획이 될 수 있습니다. 동일한 시드와 동일한 GEQO 파라미터를 사용하면 동일한 쿼리 계획이 생성됩니다

### 4.8 설정 예제

```sql
-- postgresql.conf 또는 세션에서 설정
-- 복잡한 쿼리에 대해 GEQO를 더 공격적으로 사용
SET geqo = on;
SET geqo_threshold = 8;
SET geqo_effort = 7;
SET geqo_pool_size = 256;
SET geqo_generations = 256;
SET geqo_selection_bias = 1.75;
SET geqo_seed = 0.5;
```

---

## 5. 예제

### 5.1 GEQO 동작 확인

```sql
-- GEQO 활성화 상태 확인
SHOW geqo;
SHOW geqo_threshold;

-- 현재 GEQO 설정 확인
SELECT name, setting, unit, short_desc
FROM pg_settings
WHERE name LIKE 'geqo%';
```

결과 예시:
```
         name          | setting |  unit   |                      short_desc
-----------------------+---------+---------+-----------------------------------------------------
 geqo                  | on      |         | Enables genetic query optimization.
 geqo_effort           | 5       |         | GEQO: effort is used to set the default for ...
 geqo_generations      | 0       |         | GEQO: number of iterations of the algorithm.
 geqo_pool_size        | 0       |         | GEQO: number of individuals in the population.
 geqo_seed             | 0       |         | GEQO: seed for random path selection.
 geqo_selection_bias   | 2       |         | GEQO: selective pressure within the population.
 geqo_threshold        | 12      |         | Sets the threshold of FROM items beyond ...
```

### 5.2 GEQO가 활성화되는 쿼리 예제

```sql
-- 12개 이상의 테이블을 조인하는 복잡한 쿼리
-- GEQO가 기본적으로 활성화됨 (geqo_threshold = 12)
EXPLAIN ANALYZE
SELECT *
FROM table1 t1
JOIN table2 t2 ON t1.id = t2.t1_id
JOIN table3 t3 ON t2.id = t3.t2_id
JOIN table4 t4 ON t3.id = t4.t3_id
JOIN table5 t5 ON t4.id = t5.t4_id
JOIN table6 t6 ON t5.id = t6.t5_id
JOIN table7 t7 ON t6.id = t7.t6_id
JOIN table8 t8 ON t7.id = t8.t7_id
JOIN table9 t9 ON t8.id = t9.t8_id
JOIN table10 t10 ON t9.id = t10.t9_id
JOIN table11 t11 ON t10.id = t11.t10_id
JOIN table12 t12 ON t11.id = t12.t11_id;
```

### 5.3 GEQO 성능 비교 테스트

```sql
-- 테스트용 테이블 생성
CREATE TABLE test_geqo AS
SELECT generate_series(1, 10000) AS id,
       random() AS value;

-- 인덱스 생성
CREATE INDEX ON test_geqo(id);

-- GEQO 비활성화 상태에서 계획 시간 측정
SET geqo = off;
EXPLAIN ANALYZE
SELECT * FROM test_geqo t1, test_geqo t2, test_geqo t3,
              test_geqo t4, test_geqo t5, test_geqo t6,
              test_geqo t7, test_geqo t8, test_geqo t9,
              test_geqo t10, test_geqo t11, test_geqo t12
WHERE t1.id = t2.id AND t2.id = t3.id AND t3.id = t4.id
  AND t4.id = t5.id AND t5.id = t6.id AND t6.id = t7.id
  AND t7.id = t8.id AND t8.id = t9.id AND t9.id = t10.id
  AND t10.id = t11.id AND t11.id = t12.id
LIMIT 10;

-- GEQO 활성화 상태에서 계획 시간 측정
SET geqo = on;
SET geqo_threshold = 12;
EXPLAIN ANALYZE
SELECT * FROM test_geqo t1, test_geqo t2, test_geqo t3,
              test_geqo t4, test_geqo t5, test_geqo t6,
              test_geqo t7, test_geqo t8, test_geqo t9,
              test_geqo t10, test_geqo t11, test_geqo t12
WHERE t1.id = t2.id AND t2.id = t3.id AND t3.id = t4.id
  AND t4.id = t5.id AND t5.id = t6.id AND t6.id = t7.id
  AND t7.id = t8.id AND t8.id = t9.id AND t9.id = t10.id
  AND t10.id = t11.id AND t11.id = t12.id
LIMIT 10;
```

### 5.4 GEQO 시드를 이용한 계획 다양성 테스트

```sql
-- 시드 값을 변경하여 다른 실행 계획 탐색
SET geqo_seed = 0.0;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.25;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.5;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;

SET geqo_seed = 0.75;
EXPLAIN SELECT * FROM t1 JOIN t2 ON ... JOIN t12 ON ...;
```

### 5.5 특정 쿼리에서 GEQO 임시 비활성화

```sql
-- 트랜잭션 내에서 GEQO 임시 비활성화
BEGIN;
SET LOCAL geqo = off;
-- 완전한 탐색이 필요한 중요한 쿼리 실행
SELECT ...;
COMMIT;
-- 트랜잭션 종료 후 원래 설정으로 복원
```

### 5.6 GEQO 디버깅을 위한 로깅

```sql
-- 쿼리 계획 시간을 로그에 기록하도록 설정
SET log_statement = 'all';
SET log_duration = on;

-- 또는 자동 설명 확장 사용
LOAD 'auto_explain';
SET auto_explain.log_min_duration = 0;
SET auto_explain.log_analyze = true;
```

---

## 6. 참고 문헌

### 6.1 온라인 리소스

1. The Hitch-Hiker's Guide to Evolutionary Computation
   - URL: http://www.faqs.org/faqs/ai-faq/genetic/part1/
   - 출처: news://comp.ai.genetic FAQ

2. Evolutionary Computation and its application to art and design
   - 저자: Craig Reynolds
   - URL: https://www.red3d.com/cwr/evolve.html

### 6.2 참고 문헌

3. Fundamentals of Database Systems
   - 저자: Elmasri, R. and Navathe, S.B.
   - 데이터베이스 시스템의 기초를 다루는 표준 교과서

4. The design and implementation of the POSTGRES query optimizer
   - 저자: Fong, Z.
   - University of California, Berkeley
   - POSTGRES 쿼리 최적화기의 설계와 구현에 대한 논문

### 6.3 GEQO 개발 이력

GEQO 모듈은 Martin Utesch 에 의해 독일 프라이베르크 광업 기술 대학교(University of Mining and Technology in Freiberg, Germany)의 자동 제어 연구소(Institute of Automatic Control)를 위해 개발되었습니다.

---

## 요약

| 항목 | 설명 |
|------|------|
| 목적 | 많은 테이블이 포함된 복잡한 조인 쿼리의 효율적인 계획 탐색 |
| 알고리즘 | 유전 알고리즘 (Genetic Algorithm) |
| 인코딩 | 조인 순서를 정수 문자열로 표현 |
| 기본 임계값 | 12개 이상의 FROM 항목 |
| 주요 장점 | 지수적 탐색 공간에서 합리적인 시간 내에 좋은 계획 발견 |
| 주요 단점 | 최적 계획을 보장하지 않음, 일부 오버헤드 발생 |

GEQO는 PostgreSQL이 매우 복잡한 쿼리를 처리할 수 있게 해주는 중요한 구성 요소입니다. 기본 설정은 대부분의 워크로드에 적합하지만, 특정 사용 사례에 맞게 파라미터를 조정하여 성능을 최적화할 수 있습니다.
