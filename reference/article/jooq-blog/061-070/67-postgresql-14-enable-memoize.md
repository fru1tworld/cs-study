# PostgreSQL 14의 enable_memoize로 중첩 루프 조인 성능 개선

> 원문: https://blog.jooq.org/postgresql-14s-enable_memoize-for-improved-performance-of-nested-loop-joins/

PostgreSQL 14에는 정말 흥미로운 새 플래그가 있습니다: `enable_memoize`. 이 기능이 무엇을 하는지, 그리고 어떤 쿼리가 이 기능의 혜택을 받는지 살펴보겠습니다.

## 메모이제이션이란?

부수 효과(side effect)가 없는 완벽한 세계에서(그리고 SQL은 이론적으로 그런 완벽한 세계입니다), 메모이제이션은 `y = f(x)`가 주어졌을 때 모든 계산에서 `f(x)`를 `y`로 대체할 수 있다는 것을 의미합니다.

예를 들어, `UPPER('x')`를 계산하면 항상 `'X'`를 얻게 됩니다. 이러한 함수의 계산이 비용이 많이 들고 가능한 입력 값이 적다면, 왜 해시 맵을 유지하지 않겠습니까?

Oracle 11g에서는 비슷한 기능이 있습니다: 스칼라 서브쿼리 캐싱(scalar subquery caching). 이 기능에 대해 더 알고 싶다면 Connor McDonald의 블로그 포스트를 참조하세요. Oracle에서 이 기능은 서브쿼리에서 PL/SQL 함수를 호출할 때 특히 유용합니다. 불필요한 PL/SQL 컨텍스트 스위치를 피할 수 있기 때문입니다.

PostgreSQL에서는 이 기능이 중첩 루프 조인(Nested Loop Join), 특히 LATERAL 조인에 매우 유용하며, 통계가 이것이 적절하다고 암시할 때 사용됩니다.

## 테스트 스키마

다음은 간단한 스키마입니다. 두 테이블 `t`와 `u` 모두 각각 100,000개의 행을 가지고 있습니다:

```sql
CREATE TABLE t AS
SELECT i, i % 5 AS j
FROM generate_series(1, 100000) AS t(i);

CREATE TABLE u AS
SELECT i, i % 20000 as j
FROM generate_series(1, 100000) AS t(i);

CREATE INDEX uj ON u(j);
```

테이블 `t`에서 `j` 컬럼은 5개의 고유 값만 가지고 있고, 각 값은 20,000번 나타납니다.
테이블 `u`에서 `j` 컬럼은 20,000개의 고유 값을 가지고 있고, 각 값은 5번 나타납니다. 그리고 `u.j`에는 인덱스가 있습니다.

## 기능 켜기/끄기

현재 설정을 확인하려면:

```sql
SELECT current_setting('enable_memoize');
```

결과:
```
on
```

기능을 켜거나 끄려면:

```sql
SET enable_memoize = ON;
SET enable_memoize = OFF;
```

## 테스트 1: 일반 조인

먼저 간단한 조인을 살펴보겠습니다. 메모이제이션이 활성화된 경우:

```sql
EXPLAIN
SELECT *
FROM t JOIN u ON t.j = u.j;
```

실행 계획:
```
Nested Loop  (cost=0.30..8945.41 rows=496032 width=16)
  ->  Seq Scan on t  (cost=0.00..1443.00 rows=100000 width=8)
  ->  Memoize  (cost=0.30..0.41 rows=5 width=8)
        Cache Key: t.j
        ->  Index Scan using uj on u  (cost=0.29..0.40 rows=5 width=8)
              Index Cond: (j = t.j)
```

실행 계획에서 `Memoize` 노드가 있고 `Cache Key: t.j`가 표시됩니다. 옵티마이저는 `t.j`의 고유 값 수가 적다는 것을 통계에서 알고 있으므로, 100,000번의 인덱스 조회 대신 5번만 수행하고 나머지는 캐시에서 가져올 수 있습니다.

메모이제이션이 비활성화된 경우:

```sql
SET enable_memoize = OFF;

EXPLAIN
SELECT *
FROM t JOIN u ON t.j = u.j;
```

실행 계획:
```
Hash Join  (cost=3084.00..11568.51 rows=499351 width=16)
  Hash Cond: (t.j = u.j)
  ->  Seq Scan on t  (cost=0.00..1443.00 rows=100000 width=8)
  ->  Hash  (cost=1443.00..1443.00 rows=100000 width=8)
        ->  Seq Scan on u  (cost=0.00..1443.00 rows=100000 width=8)
```

메모이제이션이 비활성화되면 옵티마이저는 해시 조인을 선택합니다. 이 경우에도 좋은 선택이지만, 메모이제이션된 중첩 루프 조인과 어떻게 비교될까요?

### 벤치마크 결과: 일반 조인

벤치마크를 위해 다음과 같은 함수를 사용합니다. 각 테스트는 5회 반복하고, 각 반복마다 25번의 쿼리를 실행합니다:

메모이제이션 비활성화 (해시 조인):
```
Run 1 of 5: 약 3.77초
Run 2 of 5: 약 3.75초
Run 3 of 5: 약 3.78초
Run 4 of 5: 약 3.76초
Run 5 of 5: 약 3.77초
```

메모이제이션 활성화 (중첩 루프 + Memoize):
```
Run 1 of 5: 약 3.38초
Run 2 of 5: 약 3.39초
Run 3 of 5: 약 3.37초
Run 4 of 5: 약 3.38초
Run 5 of 5: 약 3.38초
```

결과: 약 10% 속도 향상

## 테스트 2: LATERAL 서브쿼리

이제 LATERAL 서브쿼리를 사용하는 경우를 살펴보겠습니다:

```sql
SET enable_memoize = ON;

EXPLAIN
SELECT *
FROM
  t,
  LATERAL (
    SELECT count(*)
    FROM u
    WHERE t.j = u.j
  ) AS u(j);
```

실행 계획:
```
Nested Loop  (cost=4.40..3969.47 rows=100000 width=16)
  ->  Seq Scan on t  (cost=0.00..1443.00 rows=100000 width=8)
  ->  Memoize  (cost=4.40..4.42 rows=1 width=8)
        Cache Key: t.j
        ->  Aggregate  (cost=4.39..4.40 rows=1 width=8)
              ->  Index Only Scan using uj on u
                    (cost=0.29..4.38 rows=5 width=0)
                    Index Cond: (j = t.j)
```

여기서 `Memoize` 노드가 집계(Aggregate) 연산 전체를 캐싱하고 있습니다. `t.j`의 5개 고유 값 각각에 대해 `COUNT(*)` 결과를 한 번만 계산하면 됩니다.

### 벤치마크 결과: LATERAL 서브쿼리

메모이제이션 비활성화:
```
Run 1 of 5: 약 3.42초
Run 2 of 5: 약 3.41초
Run 3 of 5: 약 3.43초
Run 4 of 5: 약 3.42초
Run 5 of 5: 약 3.42초
```

메모이제이션 활성화:
```
Run 1 of 5: 약 1.10초
Run 2 of 5: 약 1.09초
Run 3 of 5: 약 1.10초
Run 4 of 5: 약 1.09초
Run 5 of 5: 약 1.10초
```

결과: 약 68% 속도 향상 (3배 이상 빠름)

이것은 극적인 개선입니다! LATERAL 서브쿼리에서 메모이제이션의 효과가 특히 두드러집니다.

## 테스트 3: 일반 상관 서브쿼리

그렇다면 LATERAL 대신 일반적인 상관 서브쿼리는 어떨까요?

```sql
SET enable_memoize = ON;

EXPLAIN
SELECT
  t.*,
  (
    SELECT count(*)
    FROM u
    WHERE t.j = u.j
  ) j
FROM t;
```

흥미롭게도 이 쿼리의 실행 계획에는 `Memoize` 노드가 나타나지 않습니다. PostgreSQL 14에서 일반 상관 서브쿼리는 메모이제이션의 혜택을 받지 못합니다.

### 벤치마크 결과: 일반 상관 서브쿼리

메모이제이션 비활성화:
```
Run 1 of 5: 약 3.42초
```

메모이제이션 활성화:
```
Run 1 of 5: 약 3.42초
```

결과: 성능 차이 없음

일반 상관 서브쿼리는 메모이제이션 설정과 관계없이 동일한 성능을 보입니다. 이는 PostgreSQL의 향후 버전에서 최적화할 수 있는 잠재적인 영역입니다. 예를 들어, 상관 서브쿼리를 내부적으로 LATERAL 조인으로 변환하면 메모이제이션의 혜택을 받을 수 있을 것입니다.

## 결론

PostgreSQL 14의 `enable_memoize`는 기본적으로 활성화되어 있습니다. 옵티마이저 통계가 부정확하여 잘못된 결정을 내릴 경우 약간의 추가 메모리 소비가 발생할 수 있다는 점 외에는 이 새로운 기능의 단점이 없습니다.

SQL은 이론적으로 부수 효과가 없는 4세대 언어(4GL)입니다. 이는 옵티마이저가 계산의 입력 파라미터에만 의존하는 캐시된 값으로 어떤 계산이든 대체할 수 있다는 것을 의미합니다. 이 최적화의 혜택을 가장 많이 받는 쿼리는:

1. LATERAL 조인: 외부 쿼리에서 적은 수의 고유 입력 값을 받는 경우
2. 중첩 루프 조인: 조인 키의 카디널리티가 낮은 경우

PostgreSQL 14로 업그레이드하면 코드 변경 없이도 많은 기존 쿼리가 자동으로 성능 향상을 경험할 수 있습니다.
