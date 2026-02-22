# PostgreSQL 18 성능 팁 (Performance Tips)

> 원문: https://www.postgresql.org/docs/18/performance-tips.html

쿼리 성능은 여러 요소에 의해 영향을 받을 수 있습니다. 일부는 사용자가 제어할 수 있지만, 다른 일부는 시스템의 기본 설계에 근본적인 것입니다. 이 장에서는 PostgreSQL 성능을 이해하고 튜닝하는 방법에 대한 힌트를 제공합니다.

---

## 목차

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

## 14.1 EXPLAIN 사용하기

PostgreSQL은 수신하는 각 쿼리에 대해 쿼리 계획(query plan) 을 수립합니다. 쿼리의 구조와 데이터의 특성에 맞는 올바른 계획을 선택하는 것은 좋은 성능을 위해 절대적으로 중요하므로, 시스템에는 좋은 계획을 생성하려고 시도하는 복잡한 플래너(planner) 가 포함되어 있습니다. `EXPLAIN` 명령을 사용하여 플래너가 모든 쿼리에 대해 생성하는 쿼리 계획을 볼 수 있습니다. 계획 읽기는 배워야 할 기술이지만, 이 섹션에서는 기본 사항을 다룹니다.

### 14.1.1 EXPLAIN 기초

쿼리 계획의 구조는 계획 노드(plan nodes) 의 트리입니다. 트리의 최하위 레벨에 있는 노드는 스캔 노드로, 테이블에서 원시 행을 반환합니다. 서로 다른 테이블 접근 방법에 대해 서로 다른 유형의 스캔 노드가 있습니다: 순차 스캔(sequential scans), 인덱스 스캔(index scans), 비트맵 인덱스 스캔(bitmap index scans) 등. `VALUES` 절이나 set-returning 함수처럼 테이블이 아닌 행 소스도 있으며, 이들은 자체 스캔 노드 유형을 갖습니다. 쿼리가 조인, 집계, 정렬 또는 원시 행에 대한 기타 작업이 필요한 경우, 스캔 노드 위에 이러한 작업을 수행하는 추가 노드가 있을 것입니다. 다시 말하지만, 이러한 작업을 수행하는 방법은 일반적으로 둘 이상이므로 여기에서도 다른 노드 유형이 나타날 수 있습니다. `EXPLAIN`의 출력은 계획 트리의 각 노드에 대해 한 줄씩 표시하며, 기본 노드 유형과 플래너가 해당 계획 노드의 실행에 대해 만든 비용 추정치를 보여줍니다. 추가 줄이 나타나 노드의 추가 속성을 표시하기 위해 요약 줄에서 들여쓰기될 수 있습니다. 첫 번째 줄(최상위 노드)은 전체 계획에 대한 추정 실행 비용을 가지며, 이것이 플래너가 최소화하려고 하는 숫자입니다.

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

상위 노드의 비용에는 모든 자식 노드의 비용이 포함된다는 것을 이해하는 것이 중요합니다. 비용이 플래너가 관심을 갖는 것만 반영한다는 것도 중요합니다. 특히, 비용은 결과 행을 프론트엔드로 전송하는 데 소요된 시간을 고려하지 않으며, 이는 실제 경과 시간에서 중요한 요소가 될 수 있습니다. 그러나 플래너는 이를 변경할 수 없으므로 무시합니다(어떤 계획을 선택하든 동일한 행이 출력된다고 가정합니다).

행 값은 조금 까다롭습니다. 계획 노드에 의해 처리되거나 스캔된 행의 수가 아니라, 노드에 의해 발행된 행의 수이기 때문입니다. 이것은 노드에 적용되는 모든 `WHERE` 절 조건에 의해 필터링된 행을 반영하여 스캔된 수보다 적은 경우가 많습니다. 이상적으로 최상위 행 추정치는 쿼리가 실제로 반환하는 행 수에 근접할 것입니다.

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

이 쿼리가 선택할 실제 행 수는 7000이지만, 행 추정치는 근사치일 뿐입니다. 이 실험을 두 번 실행하면 약간 다른 추정치를 얻을 수 있습니다. 게다가 각 `ANALYZE` 명령 후에 변경될 수 있는데, `ANALYZE`에 의해 생성된 통계가 테이블에서 무작위로 추출된 샘플에서 가져온 것이기 때문입니다.

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

여기서 플래너는 두 단계 계획을 사용하기로 결정했습니다: 자식 계획 노드는 인덱스를 방문하여 인덱스 조건과 일치하는 행의 위치를 찾고, 그런 다음 상위 계획 노드는 실제로 해당 행을 테이블 자체에서 가져옵니다. 행을 별도로 가져오는 것은 순차적으로 읽는 것보다 훨씬 비쌉니다. 그러나 테이블의 모든 페이지를 방문할 필요가 없기 때문에 이것은 여전히 순차 스캔보다 저렴합니다.

왜 두 개의 계획 레벨이 사용되는지 생각해 볼 수 있습니다: 왜 인덱스 스캔 노드가 디스크에서 행을 직접 검색할 수 없을까요? 그 이유는 상위 계획 노드가 인덱스에 의해 식별된 행 위치를 물리적 순서로 읽을 수 있도록 정렬하기 때문입니다. 그렇지 않으면 디스크 탐색이 많이 발생합니다. "비트맵(bitmap)"으로 알려진 양 레벨 계획은 이러한 이유로 필요합니다.

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

이 유형의 계획에서 테이블 행은 인덱스 순서로 가져오므로 읽기 비용이 더 비싸지만, 행이 너무 적어서 행 위치를 정렬하는 추가 비용의 가치가 없습니다. 이 계획 유형은 단일 행만 가져오는 쿼리와 인덱스의 정렬 순서와 일치하는 `ORDER BY` 조건이 있는 쿼리에서 가장 자주 볼 수 있습니다. 왜냐하면 결과를 정렬하는 추가 단계가 필요 없기 때문입니다.

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

이것은 두 조건 중 하나만 사용하는 것이 필요하지만, 테이블에 대한 방문을 두 인덱스 조건을 모두 만족하는 행으로 제한합니다.

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

이것은 계획과 추정치에서 흥미로운 변화를 보여줍니다. Index Scan 노드의 총 비용과 행 추정치는 `LIMIT` 절이 없을 때와 동일하게 표시됩니다. 이것은 예상된 것입니다. 왜냐하면 `Limit` 노드가 실행을 일찍 중지해야 하지만 자식 노드가 자체적으로 어떤 제약도 인식하지 못하기 때문입니다. 그러나 `Limit` 노드의 비용을 살펴보면 그것이 자식 노드의 총 비용을 기반으로 하여 행당 비용을 계산하고 그것에 원하는 행 수를 곱한 것임을 알 수 있습니다.

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

이 계획에서 우리는 두 개의 입력 스캔 위에 중첩 루프 조인 노드가 있습니다. 노드 요약 줄의 들여쓰기는 계획 트리 구조를 반영합니다. 조인의 첫 번째 또는 "외부" 자식은 위에서 본 것과 유사한 비트맵 스캔입니다. 그 비용과 행 수는 `... WHERE unique1 < 10`을 단독으로 실행한 것과 동일합니다. 왜냐하면 이 노드에서 `t1.unique2 = t2.unique2` 절을 적용하지 않기 때문입니다. 중첩 루프 조인 노드는 외부에서 얻은 각 행에 대해 내부 자식을 한 번 실행합니다. 현재 외부 행의 열 값이 내부 스캔에 삽입될 수 있습니다. 여기서 외부 행의 `t1.unique2` 값을 사용할 수 있으므로 `tenk2`에 대한 `unique1 = 42` 케이스와 유사한 인덱스 스캔을 얻습니다. (분명히 하기 위해, 여기서 `t1.unique2`는 상수가 아니더라도 상수처럼 취급될 것입니다. 왜냐하면 조인 노드의 범위 내에서 현재 행의 값이 변하지 않기 때문입니다.) 따라서 추정 비용 및 중첩 루프의 행 수는 외부 스캔에서 직접 도출하고, 내부 스캔당 하나의 반복을 더합니다(여기서 10 × 7.90). 그러나 실제로 외부와 내부의 시작 비용을 분리하는 것보다 약간 더 복잡합니다. 내부 스캔의 시작 비용은 한 번만 발생하므로 중첩 루프의 총 시작 비용을 계산할 때 내부 시작 비용을 10번 추가하지 않습니다.

이 예에서 조인의 출력 행 수는 두 스캔의 행 수의 곱과 같지만 일반적으로 그렇지 않습니다. 왜냐하면 두 테이블을 언급하는 추가 `WHERE` 절이 있을 수 있고 따라서 조인 노드에만 적용할 수 있기 때문입니다(스캔 노드에는 적용할 수 없습니다). 예를 들어 `t1.hundred < t2.hundred`를 추가하면 조인 노드의 출력 행이 줄어들지만 입력 스캔 중 어느 것도 변경되지 않습니다:

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

조건 `t1.hundred < t2.hundred`는 인덱스에서 사용할 수 없으므로 조인 노드에서 조인 필터로 적용됩니다. 내부 관계도 인덱스 스캔이 필요하지 않도록 조건에 의해 충분히 제한되었습니다. 이것은 "Materialize" 계획 노드를 인덱스 스캔 위에 추가합니다. 이렇게 하면 한 번만 스캔하고 나중에 다시 방문할 수 있도록 행이 저장됩니다. 중첩 루프의 총 비용에서 볼 수 있듯이 외부 행당 10회 재방문하는 것이 그렇게 비싸지 않습니다.

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

여기서 `t1`에 대한 비트맵 스캔 결과가 해시 테이블에 로드됩니다. 그런 다음 `t2`를 순차적으로 스캔하고, 각 `t2` 행에 대해 해시 테이블에서 `t1.unique2`의 가능한 일치를 조사합니다. 해시 조인에서 해시 테이블은 결과에 나타날 수 있는 행을 제공하므로 "내부" 관계라고 합니다. 반면에 `t2`는 각 행에 대해 해시 테이블을 조사하므로 "외부" 관계라고 합니다.

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

플래너가 잘못된 조인 순서를 선택했다고 의심되면 런타임 매개변수를 사용하여 다른 유형의 조인을 비활성화하여 플래너가 선호하는 계획을 사용하도록 강제할 수 있습니다. (이것은 조잡한 도구이지만 유용합니다.)

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

이 쿼리에서 외부 쿼리의 현재 행의 `t.four` 값이 서브쿼리에 전달됩니다. 서브플랜에서 `WHERE o.four = t.four` 조건은 매개변수가 있는 조건입니다.

서브쿼리가 외부 쿼리에서 값을 사용하지 않으면 서브플랜을 "해시"할 수 있습니다. 즉, 서브플랜을 한 번만 실행하고 결과를 해시 테이블에 저장합니다:

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

서브플랜이 외부 쿼리에서 값을 사용하지 않고 외부 계획이 실행되기 전에 한 번만 실행되는 경우 "InitPlan"으로 표시됩니다:

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

### 14.1.2 EXPLAIN ANALYZE

`EXPLAIN`의 `ANALYZE` 옵션을 사용하면 실제로 문을 실행하고, 각 계획 노드 내에서 누적된 실제 행 수와 실제 실행 시간을 추정치와 함께 표시합니다. 이것은 플래너의 추정치가 현실에 가까운지 확인하는 데 유용합니다.

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

"actual time" 값은 밀리초 단위의 실제 시간이고, `cost` 추정치는 임의의 단위로 표현됩니다. 따라서 일치할 가능성은 낮습니다. 보통 살펴봐야 할 중요한 것은 추정 행 수가 실제와 합리적으로 가까운지 여부입니다. 이 예에서 추정치는 모두 정확했습니다.

`loops` 값이 1보다 크면 `actual time`과 `rows` 값이 실행당 평균이라는 점에 유의하세요. 이것은 총 시간과 비교할 수 있도록 합니다. 노드에서 소비된 실제 총 시간을 얻으려면 `actual time`에 `loops` 값을 곱해야 합니다. 위 예에서 우리는 내부 인덱스 스캔을 실행하는 데 총 약 0.030 밀리초를 보냈습니다.

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

`EXPLAIN ANALYZE`는 데이터 수정 문도 분석할 수 있습니다. 그러나 실제로 데이터를 변경하므로 `ROLLBACK`을 사용하는 것이 좋습니다:

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

### 14.1.3 주의사항

`EXPLAIN ANALYZE`로 측정된 실행 시간이 동일한 쿼리의 정상 실행과 크게 다를 수 있는 두 가지 중요한 이유가 있습니다:

1. 네트워크 전송 비용 미포함: 출력 행이 클라이언트로 전송되지 않으므로 네트워크 전송 비용과 I/O 변환 비용이 포함되지 않습니다. `SERIALIZE` 옵션을 사용하면 I/O 변환 비용을 별도로 측정할 수 있습니다.

2. 측정 오버헤드: `EXPLAIN ANALYZE`가 추가하는 측정 오버헤드가 상당할 수 있습니다. 특히 `gettimeofday()` 운영 체제 호출이 느린 시스템에서 그렇습니다. `pg_test_timing` 도구를 사용하여 시스템의 타이밍 오버헤드를 측정할 수 있습니다.

작은 테이블에서 큰 테이블로의 외삽 위험:

```sql
EXPLAIN SELECT * FROM small_table;
```

작은 테이블(예: 하나의 디스크 페이지에 맞는)에서 얻은 결과를 훨씬 큰 테이블에 외삽하는 것은 위험합니다. 플래너의 비용 추정은 선형이 아니며, 따라서 큰 테이블에 대해 다른 계획을 선택할 수 있습니다.

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

Index Scan 노드에 대해 예상 행 수는 10이지만 실제 행 수는 2입니다. 이것은 오류가 아닙니다. 예상 수치는 전체 노드가 완료될 때까지 반환될 행 수에 대한 것인 반면, 실제 수치는 `Limit` 노드가 2행을 받은 후 실행을 중지했기 때문입니다.

Merge Join의 측정 아티팩트:

- 머지 조인은 입력 중 하나의 일부만 읽을 수 있습니다. 다음 키 값이 다른 입력의 마지막 값보다 크면 중단할 수 있기 때문입니다.
- 외부 측에 중복 키 값이 있으면 내부 행을 백업하고 다시 스캔할 수 있어 보고된 실제 행 수가 실제 테이블 행 수보다 많을 수 있습니다.

BitmapAnd/BitmapOr 제한:

구현 제한으로 인해 `BitmapAnd` 및 `BitmapOr` 노드는 항상 실제 행 수를 0으로 보고합니다.

---

## 14.2 플래너가 사용하는 통계

### 14.2.1 단일 칼럼 통계

쿼리 플래너는 좋은 쿼리 계획을 선택하기 위해 검색되는 행의 수를 추정해야 합니다.

통계의 한 가지 구성요소는 각 테이블과 인덱스의 총 항목 수 및 각 테이블과 인덱스가 차지하는 디스크 블록 수입니다. 이 정보는 `pg_class` 테이블의 `reltuples`와 `relpages` 칼럼에 저장됩니다.

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

대부분의 쿼리는 `WHERE` 절로 인해 테이블의 일부 행만 검색합니다. 플래너는 선택도(selectivity) 를 추정해야 합니다. 즉, `WHERE` 절의 각 조건과 일치하는 행의 비율입니다. 이 작업에 사용되는 정보는 `pg_statistic` 시스템 카탈로그에 저장됩니다.

pg_stats 뷰:

`pg_statistic`을 직접 보는 것보다 `pg_stats` 뷰를 사용하는 것이 좋습니다. 더 읽기 쉽게 설계되었고, 모두 읽을 수 있습니다(`pg_statistic`은 슈퍼유저만 읽을 수 있음).

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

더 높은 한계를 설정하면 플래너 추정이 더 정확해지지만, 특히 불규칙한 데이터 분포의 칼럼에 효과적입니다. 대신 `pg_statistic` 공간이 증가하고 추정 계산 시간도 증가합니다.

### 14.2.2 확장 통계

느린 쿼리가 나쁜 실행 계획을 실행하는 일반적인 이유는 쿼리 절에 사용된 여러 칼럼이 상관관계가 있기 때문입니다.

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

#### 14.2.2.1 함수 종속성 (Functional Dependencies)

함수 종속성 은 데이터베이스 정규화 정의에 사용되는 개념입니다. 칼럼 `b`가 칼럼 `a`에 함수적으로 종속 된다는 것은 `a`의 값을 알면 `b`의 값을 결정하기에 충분하다는 의미입니다.

함수 종속성의 존재는 특정 쿼리의 추정 정확도에 직접 영향을 미칩니다. 쿼리에 독립 칼럼과 종속 칼럼 모두에 대한 조건이 포함된 경우, 종속 칼럼의 조건은 결과 크기를 더 이상 줄이지 않습니다. 함수 종속성을 알지 못하면 플래너는 조건이 독립적이라고 가정하여 결과 크기를 과소 추정합니다.

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

또한 플래너는 함수 종속성으로 추정할 때 관련 칼럼의 조건이 호환된다고 가정합니다. 조건이 호환되지 않으면(예: `city = 'San Francisco' AND zip = '90210'`) 정확한 추정은 0행이어야 하지만, 이 가능성은 고려되지 않습니다.

#### 14.2.2.2 다변량 N-Distinct 계수

단일 칼럼 통계는 각 칼럼의 고유 값 수를 저장합니다. 그러나 둘 이상의 칼럼을 결합할 때 고유 값의 수 추정(예: `GROUP BY a, b`)은 플래너가 단일 칼럼 통계만 있을 때 종종 잘못됩니다.

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

#### 14.2.2.3 다변량 MCV 목록

각 칼럼에 대해 저장되는 또 다른 통계 유형은 가장 일반적인 값 목록(most-common value lists) 입니다. 개별 칼럼에 대해서는 매우 정확한 추정을 제공하지만, 여러 칼럼의 조건이 있는 쿼리에 대해서는 상당한 과소 추정을 초래할 수 있습니다.

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

## 14.3 명시적 JOIN 절로 플래너 제어하기

단순한 조인 쿼리의 예:

```sql
SELECT * FROM a, b, c WHERE a.id = b.id AND b.ref = c.id;
```

플래너는 주어진 테이블들을 어떤 순서로든 조인할 자유가 있습니다:
- A와 B를 먼저 조인한 후 C를 조인
- B와 C를 먼저 조인한 후 A를 조인
- A와 C를 먼저 조인한 후 B를 조인 (비효율적)

핵심 포인트: 이러한 다양한 조인 가능성은 의미론적으로 동등한 결과를 주지만 실행 비용은 크게 다를 수 있습니다.

### JOIN 순서 문제의 확장

- 2-3개 테이블: 조인 순서 선택지가 적음
- 10개 이상 테이블: 철저한 탐색이 비현실적
- 6-7개 테이블: 계획 시간이 오래 걸릴 수 있음
- 임계값: `geqo_threshold` 런타임 파라미터로 설정
- 너무 많은 입력 테이블이 있을 때: 유전적 확률 탐색(genetic probabilistic search)으로 전환

### OUTER JOIN과 플래너의 자유도

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

명시적 INNER JOIN 문법(`INNER JOIN`, `CROSS JOIN`, 또는 `JOIN`)은 `FROM`에 관계를 나열하는 것과 의미론적으로 동일하므로 조인 순서를 제약하지 않습니다.

### 의미론적으로 동등한 쿼리들

```sql
-- 쿼리 1: 암시적 JOIN
SELECT * FROM a, b, c WHERE a.id = b.id AND b.ref = c.id;

-- 쿼리 2: 명시적 CROSS JOIN
SELECT * FROM a CROSS JOIN b CROSS JOIN c WHERE a.id = b.id AND b.ref = c.id;

-- 쿼리 3: 명시적 중첩 JOIN
SELECT * FROM a JOIN (b JOIN c ON (b.ref = c.id)) ON (a.id = b.id);
```

논리적으로 동등하지만 계획 시간은 다릅니다. 3개 테이블에서는 차이가 미미하지만, 많은 테이블에서는 계획 시간 차이가 매우 중요합니다.

### join_collapse_limit 파라미터

조인 순서를 플래너가 따르도록 강제하려면:

```sql
SET join_collapse_limit = 1;
```

효과:
- 조인 순서를 부분적으로 제약할 수 있음
- 계획 시간 단축
- 좋은 쿼리 플랜으로 유도

### 부분적 제약 예

```sql
SELECT * FROM a CROSS JOIN b, c, d, e WHERE ...;
```

`join_collapse_limit = 1`일 때:
- A를 B와 먼저 조인하도록 강제
- 다른 선택은 제약하지 않음
- 가능한 조인 순서가 5배 감소

### 서브쿼리 붕괴(Subquery Collapse)

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

### from_collapse_limit 파라미터

- 거대한 조인 탐색 문제에 빠지지 않기 위해 사용
- 부모 쿼리에서 `from_collapse_limit`을 초과하는 FROM 항목이 있을 때 서브쿼리 붕괴 방지

### 두 파라미터의 관계

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

## 14.4 데이터베이스 채우기

대규모 데이터를 데이터베이스에 처음 로드할 때 효율성을 높이기 위한 가이드입니다.

### 14.4.1 자동 커밋 비활성화

여러 `INSERT`를 사용할 때는 자동 커밋을 끄고 마지막에 한 번만 커밋하세요. 표준 SQL에서는 시작 시 `BEGIN`을, 끝에 `COMMIT`을 실행합니다. psql과 같은 일부 클라이언트 라이브러리는 이를 백그라운드에서 수행하므로 효과가 있는지 확인해야 합니다.

각 행을 개별적으로 커밋하면 PostgreSQL이 각 행마다 많은 작업을 수행합니다. 한 트랜잭션 내 모든 삽입의 추가적인 이점은 한 행 삽입이 실패하면 그 시점까지 삽입된 모든 행이 롤백되어 부분적으로 로드된 데이터 문제에 갇히지 않는다는 것입니다.

### 14.4.2 COPY 사용

여러 `INSERT` 대신 `COPY` 명령어로 모든 행을 한 번에 로드하세요. `COPY`는 대량 행 로드에 최적화되어 있으며, `INSERT`보다 유연성은 떨어지지만 훨씬 적은 오버헤드를 발생시킵니다. `COPY`는 단일 명령어이므로 자동 커밋을 비활성화할 필요가 없습니다.

`COPY` 사용이 불가능한 경우 `PREPARE`를 사용하여 준비된 `INSERT` 문을 만든 다음 필요한 만큼 `EXECUTE`를 사용하는 것이 도움이 될 수 있습니다. 이렇게 하면 `INSERT`를 반복적으로 파싱하고 계획하는 오버헤드를 피할 수 있습니다. 다양한 인터페이스에서 이를 다르게 처리합니다. 인터페이스 문서에서 "준비된 문"을 찾아보세요.

WAL 최적화:

`COPY`를 `CREATE TABLE` 또는 `TRUNCATE` 명령어와 같은 트랜잭션 내에서 사용할 때 가장 빠릅니다. 에러 발생 시 새로운 데이터 파일이 제거되므로 WAL 쓰기가 불필요합니다. 단, `wal_level`이 `minimal`일 때만 적용됩니다(다른 경우 모든 명령어가 WAL 작성).

### 14.4.3 인덱스 제거

새로 생성된 테이블에 로드하는 경우 가장 빠른 방법은:
1. 테이블 생성
2. `COPY`로 대량 데이터 로드
3. 필요한 인덱스 생성

기존 데이터에 인덱스를 생성하는 것이 증분으로 업데이트하는 것보다 빠릅니다.

기존 테이블에 대량 데이터를 추가하는 경우:
- 인덱스 제거 -> 테이블 로드 -> 인덱스 재생성을 고려
- 트레이드오프: 로드 속도 향상 vs 인덱스 없는 동안 다른 사용자의 성능 저하
- 주의: 유니크 인덱스는 신중히 제거 (유니크 제약 조건의 오류 검사 상실)

### 14.4.4 외래 키 제약 조건 제거

인덱스와 마찬가지로 외래 키 제약도 행별 검사보다 대량 검사가 더 효율적입니다.

절차:
1. 외래 키 제약 제거
2. 데이터 로드
3. 제약 재생성

중요 고려사항:

외래 키 제약이 있는 테이블에 데이터를 로드할 때, 각 새 행은 서버의 보류 중인 트리거 이벤트 목록에 항목을 추가합니다. 수백만 행 로드 시 트리거 이벤트 큐가 메모리 오버플로우를 일으켜 과도한 스왑 또는 명령 실패가 가능합니다. 따라서 대량 데이터 로드 시 외래 키 제거는 선택이 아닌 필수입니다. 대안으로 로드 작업을 더 작은 트랜잭션으로 분할할 수 있습니다.

### 14.4.5 maintenance_work_mem 증가

`maintenance_work_mem` 설정 변수를 일시적으로 증가시키면 대량 데이터 로드 성능이 향상됩니다.

효과:
- `CREATE INDEX` 명령어 속도 향상
- `ALTER TABLE ADD FOREIGN KEY` 명령어 속도 향상
- `COPY` 자체에는 큰 효과 없음 (위 기술 사용 시에만 유용)

### 14.4.6 max_wal_size 증가

대량 데이터 로드 시 일반적인 체크포인트 빈도보다 더 자주 체크포인트가 발생합니다. 체크포인트 시 모든 더티 페이지를 디스크에 플러시합니다.

최적화:

`max_wal_size`를 일시적으로 증가시키면 필요한 체크포인트 수가 감소하여 속도가 향상됩니다.

### 14.4.7 WAL 아카이빙 및 스트리밍 복제 비활성화

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

### 14.4.8 완료 후 ANALYZE 실행

테이블 내 데이터 분포를 크게 변경한 후에는 `ANALYZE` 실행을 강력히 권장합니다. 여기에는 대량의 데이터를 대량 로드하는 것이 포함됩니다. `ANALYZE`(또는 `VACUUM ANALYZE`)를 실행하면 플래너에게 테이블에 대한 최신 통계를 제공합니다. 통계가 없거나 오래된 통계가 있으면 플래너가 잘못된 쿼리 계획을 선택하여 성능이 저하될 수 있습니다.

자동 진공 데몬이 활성화되어 있으면 자동으로 `ANALYZE`가 실행될 수 있습니다.

### 14.4.9 pg_dump에 대한 참고사항

`pg_dump`가 생성하는 덤프 스크립트는 자동으로 위의 여러 가이드라인을 적용합니다. 완전한 스키마 및 데이터 덤프를 다시 로드할 때, 인덱스 및 외래 키를 생성하기 전에 데이터를 로드합니다. 따라서 수동으로 수행해야 할 작업은 적절한 설정 변수를 설정하는 것뿐입니다.

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

## 14.5 비영속적 설정

영속성(Durability)은 서버 충돌이나 전원 손실 시에도 커밋된 트랜잭션을 기록하는 데이터베이스 기능입니다. 그러나 영속성은 상당한 데이터베이스 오버헤드를 추가하므로, 이러한 보장이 필요하지 않은 경우 PostgreSQL을 훨씬 더 빠르게 실행할 수 있습니다.

주요 주의사항: 아래 설정들은 데이터베이스 소프트웨어 충돌 시에는 여전히 영속성을 보장하며, OS 충돌 시에만 데이터 손실 또는 손상 위험이 발생합니다.

### 비영속적 성능 향상 설정

#### 1. RAM 디스크 사용

데이터베이스 클러스터의 데이터 디렉토리를 메모리 기반 파일 시스템(RAM disk)에 배치합니다. 이렇게 하면 모든 데이터베이스 디스크 I/O를 제거할 수 있습니다.

제약사항: 사용 가능한 메모리(및 swap) 크기로 데이터 저장이 제한됩니다.

#### 2. fsync 끄기

```sql
fsync = off
```

디스크에 데이터를 플러시할 필요가 없습니다.

#### 3. synchronous_commit 끄기

```sql
synchronous_commit = off
```

모든 커밋 시 WAL 쓰기를 디스크에 강제할 필요가 없습니다.

위험성: 데이터베이스 충돌 시 트랜잭션이 손실될 수 있습니다(데이터 손상은 아님).

#### 4. full_page_writes 끄기

```sql
full_page_writes = off
```

부분 페이지 쓰기에 대한 보호가 불필요합니다.

#### 5. 체크포인트 빈도 감소

```sql
max_wal_size = (증가된 값)
checkpoint_timeout = (증가된 값)
```

체크포인트 빈도를 감소시킵니다.

트레이드오프: `/pg_wal` 저장소 요구량이 증가합니다.

#### 6. Unlogged 테이블 생성

```sql
CREATE UNLOGGED TABLE table_name (
    -- 테이블 정의
);
```

WAL 쓰기를 회피합니다.

제약사항: 테이블이 충돌에 안전하지 않습니다.

### 성능 개선 순서

일반적으로 위의 설정들을 조합하여 사용하면 성능을 크게 향상시킬 수 있으나, 각 설정의 위험성을 충분히 이해하고 필요에 따라 선택적으로 적용해야 합니다.
