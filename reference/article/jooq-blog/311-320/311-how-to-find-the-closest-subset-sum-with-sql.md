# SQL로 가장 가까운 부분 집합 합을 찾는 방법

> 원문: https://blog.jooq.org/how-to-find-the-closest-subset-sum-with-sql/

## 문제 개요

Stack Overflow의 한 사용자가 숫자 집합의 부분 집합에서 요소들의 합이 예상 목표 합에 "가장 가까운" 값을 찾는 방법을 질문했습니다. 이 글에서는 두 가지 해석을 제시합니다:

1. 정렬된 부분 집합 합 - 엄격한 순서로 누적 합계를 계산
2. 비제한적 부분 집합 합 - 고전적인 NP-완전 문제인 부분 집합 합 문제

## 테이블 스키마

ASSIGN 테이블 (예상 목표 합):
- ID: 1, 2, 3
- ASSIGN_AMT: 25150, 19800, 27511

WORK 테이블 (합산할 값들):
- ID: 1-10
- WORK_AMT: 7120, 8150, 8255, 9051, 1220, 12515, 13555, 5221, 812, 6562

## 솔루션 1: 윈도우 함수를 사용한 정렬된 부분 집합

기본적인 접근 방식은 누적 합계를 사용합니다:

```sql
WITH VALS (ID, WORK_AMT) AS (
    SELECT 1, 7120 FROM DUAL
    UNION ALL SELECT 2, 8150 FROM DUAL
    UNION ALL SELECT 3, 8255 FROM DUAL
    -- ... 나머지 값들
)
SELECT
    ID,
    WORK_AMT,
    SUM(WORK_AMT) OVER (ORDER BY ID) AS SUBSET_SUM
FROM VALS
ORDER BY ID
```

이 쿼리는 각 행에 대해 누적 합계를 생성합니다.

## 솔루션 2: 정량화된 비교 술어를 사용한 가장 가까운 일치 찾기

`<= ALL` 연산자를 사용하여 절대 차이가 최소인 부분 집합 합을 찾습니다:

```sql
WITH ASSIGN(ID, ASSIGN_AMT) AS (
    SELECT 1, 25150 FROM DUAL
    UNION ALL SELECT 2, 19800 FROM DUAL
    UNION ALL SELECT 3, 27511 FROM DUAL
),
VALS(ID, WORK_AMT) AS (
    -- ... work 값들
),
SUMS(ID, WORK_AMT, SUBSET_SUM) AS (
    SELECT VALS.*, SUM(WORK_AMT) OVER (ORDER BY ID)
    FROM VALS
)
SELECT
    ASSIGN.ID,
    ASSIGN.ASSIGN_AMT,
    SUBSET_SUM
FROM ASSIGN
JOIN SUMS
ON ABS(ASSIGN_AMT - SUBSET_SUM) <= ALL (
    SELECT ABS(ASSIGN_AMT - SUBSET_SUM) FROM SUMS
)
```

결과: 세 가지 할당 모두 SUBSET_SUM 23525에 일치합니다.

참고: Oracle에서 튜플을 비교할 때 "이 연산자는 리스트와 함께 사용할 수 없습니다". PostgreSQL은 행 비교를 더 잘 처리합니다.

## 솔루션 3: Oracle의 KEEP FIRST 구문

Oracle은 순서가 있는 집계 함수를 제공합니다:

```sql
SELECT
    ASSIGN.ID,
    ASSIGN.ASSIGN_AMT,
    MIN(SUBSET_SUM) KEEP (
        DENSE_RANK FIRST
        ORDER BY ABS(ASSIGN_AMT - SUBSET_SUM)
    ) AS CLOSEST_SUM
FROM ASSIGN
CROSS JOIN SUMS
GROUP BY ASSIGN.ID, ASSIGN.ASSIGN_AMT
```

이것은 절대 차이 기준으로 첫 번째 순위의 행들을 선택한 다음, 해당 행들에서 최소 SUBSET_SUM을 반환합니다.

## 솔루션 4: LATERAL JOIN (Oracle 12c 이상)

`CROSS JOIN LATERAL`과 `FETCH FIRST`를 사용합니다:

```sql
SELECT
    ASSIGN.ID,
    ASSIGN.ASSIGN_AMT,
    CLOSEST_SUM
FROM ASSIGN
CROSS JOIN LATERAL (
    SELECT SUBSET_SUM AS CLOSEST_SUM
    FROM SUMS
    ORDER BY ABS(ASSIGN.ASSIGN_AMT - SUBSET_SUM)
    FETCH FIRST 1 ROW ONLY
) SUMS
```

이 접근 방식은 왼쪽에서 한 번에 하나의 행을 처리하여 오른쪽에서 ASSIGN 컬럼을 참조할 수 있게 합니다.

## 솔루션 5: 재귀 CTE를 사용한 비제한적 부분 집합

진정한 부분 집합 합 문제(값들의 임의 조합)를 위한 솔루션입니다:

```sql
WITH ASSIGN(ID, ASSIGN_AMT) AS (
    SELECT 1, 25150 FROM DUAL
    UNION ALL SELECT 2, 19800 FROM DUAL
    UNION ALL SELECT 3, 27511 FROM DUAL
),
WORK(ID, WORK_AMT) AS (
    -- ... 모든 work 값들
),
SUMS(SUBSET_SUM, MAX_ID, CALC) AS (
    SELECT WORK_AMT, ID, TO_CHAR(WORK_AMT)
    FROM WORK

    UNION ALL

    SELECT
        WORK_AMT + SUBSET_SUM,
        WORK.ID,
        CALC || '+' || WORK_AMT
    FROM SUMS
    JOIN WORK
    ON SUMS.MAX_ID < WORK.ID
)
SELECT
    ASSIGN.ID,
    ASSIGN.ASSIGN_AMT,
    MIN(SUBSET_SUM) KEEP (
        DENSE_RANK FIRST
        ORDER BY ABS(ASSIGN_AMT - SUBSET_SUM)
    ) AS CLOSEST_SUM,
    MIN(CALC) KEEP (
        DENSE_RANK FIRST
        ORDER BY ABS(ASSIGN_AMT - SUBSET_SUM)
    ) AS CALCULATION
FROM SUMS
CROSS JOIN ASSIGN
GROUP BY ASSIGN.ID, ASSIGN.ASSIGN_AMT
```

결과 (10개 값 사용 시): ID 1은 25133, ID 2는 19768, ID 3은 27488을 얻습니다.

재귀는 MAX_ID < WORK.ID 조건으로 이전 합계와 나머지 값들을 조인하여 가능한 모든 부분 집합 조합을 생성합니다.

## 성능 경고

10개 값: 0.112초
19개 값: 16.3초
68개 값: 3시간 이상 (2^n의 지수적 복잡도)

이 글에서 강조하는 것처럼: "이 알고리즘은 솔루션 기법에 관계없이 전혀 확장되지 않습니다!"

한 댓글 작성자는 16개 값에 대해 65,536개의 가능성을 평가하는 데 1분 49초가 걸렸다고 보고했습니다. n=68인 경우, 이론적 가능성은 295퀸틸리언(10^18) 조합에 달합니다.

## 핵심 요점

- 윈도우 함수는 정렬된 누적 합계를 효율적으로 계산합니다
- 정량화된 술어는 가장 가까운 일치를 찾는 우아한 로직을 제공합니다
- Oracle의 KEEP FIRST와 LATERAL JOIN은 성능이 좋은 대안을 제공합니다
- 재귀 CTE는 비제한적 부분 집합을 해결하지만 지수적 복잡도에 직면합니다
- 큰 데이터셋의 경우, 순수 SQL보다 절차적 접근 방식(PL/SQL, Java)을 고려하세요
