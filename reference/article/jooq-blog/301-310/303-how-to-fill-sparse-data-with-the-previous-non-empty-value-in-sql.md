# SQL에서 이전 비어있지 않은 값으로 희소 데이터를 채우는 방법

> 원문: https://blog.jooq.org/how-to-fill-sparse-data-with-the-previous-non-empty-value-in-sql/

## 소개

다음은 모든 데이터 관련 기술에서 매우 흔한 문제이며, 이에 대한 두 가지 간결한 SQL 기반 솔루션을 살펴보겠습니다: 희소 데이터 셋의 셀을 '이전 비어있지 않은 값'으로 어떻게 채울 수 있을까요?

## 문제

다음과 같이 0을 "빈" 값으로 포함하는 희소 데이터가 있는 테이블이 있다고 가정해 봅시다:

```
Col1  Col2  Col3  Col4
A     0     1     5
B     0     4     0
C     2     0     0
D     0     0     0
E     3     5     0
F     0     3     0
G     0     3     1
H     0     1     5
I     3     5     0
```

원하는 결과는 빈 값을 가장 최근의 비어있지 않은 값으로 채우는 것입니다:

```
Col1  Col2  Col3  Col4
A     0     1     5
B     0     4     5
C     2     4     5
D     2     4     5
E     3     5     5
F     3     3     5
G     3     3     1
H     3     1     5
I     3     5     5
```

## 윈도우 함수 솔루션

주요 SQL 접근 방식은 Oracle 구문을 사용한 윈도우 함수를 활용합니다:

```sql
WITH t(col1, col2, col3, col4) AS (
  SELECT 'A', 0, 1, 5 FROM DUAL UNION ALL
  SELECT 'B', 0, 4, 0 FROM DUAL UNION ALL
  SELECT 'C', 2, 0, 0 FROM DUAL UNION ALL
  SELECT 'D', 0, 0, 0 FROM DUAL UNION ALL
  SELECT 'E', 3, 5, 0 FROM DUAL UNION ALL
  SELECT 'F', 0, 3, 0 FROM DUAL UNION ALL
  SELECT 'G', 0, 3, 1 FROM DUAL UNION ALL
  SELECT 'H', 0, 1, 5 FROM DUAL UNION ALL
  SELECT 'I', 3, 5, 0 FROM DUAL
)
SELECT
  col1,
  nvl(last_value(nullif(col2, 0)) IGNORE NULLS OVER (ORDER BY col1), 0) col2,
  nvl(last_value(nullif(col3, 0)) IGNORE NULLS OVER (ORDER BY col1), 0) col3,
  nvl(last_value(nullif(col4, 0)) IGNORE NULLS OVER (ORDER BY col1), 0) col4
FROM t
```

## 핵심 개념 설명

NULLIF 함수: 0 값을 NULL로 변환하여 윈도우 함수와 함께 "IGNORE NULLS" 절을 사용할 수 있게 합니다.

IGNORE NULLS가 있는 LAST_VALUE: "col1으로 행을 정렬할 때 현재 행 이전의 마지막 비NULL 값을 가져옵니다."

NVL 래퍼: 남아있는 NULL 값을 제거하고 0으로 대체합니다.

프레임 지정: ORDER BY가 있는 기본 프레임은 "ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW"입니다.

## MODEL 절 솔루션

대안적인 접근 방식은 Oracle의 스프레드시트와 유사한 MODEL 구문을 사용합니다:

```sql
WITH t(col1, col2, col3, col4) AS (
  SELECT 'A', 0, 1, 5 FROM DUAL UNION ALL
  SELECT 'B', 0, 4, 0 FROM DUAL UNION ALL
  SELECT 'C', 2, 0, 0 FROM DUAL UNION ALL
  SELECT 'D', 0, 0, 0 FROM DUAL UNION ALL
  SELECT 'E', 3, 5, 0 FROM DUAL UNION ALL
  SELECT 'F', 0, 3, 0 FROM DUAL UNION ALL
  SELECT 'G', 0, 3, 1 FROM DUAL UNION ALL
  SELECT 'H', 0, 1, 5 FROM DUAL UNION ALL
  SELECT 'I', 3, 5, 0 FROM DUAL
)
SELECT * FROM t
MODEL
  DIMENSION BY (row_number() OVER (ORDER BY col1) rn)
  MEASURES (col1, col2, col3, col4)
  RULES (
    col2[any] = DECODE(col2[cv(rn)], 0, NVL(col2[cv(rn) - 1], 0), col2[cv(rn)]),
    col3[any] = DECODE(col3[cv(rn)], 0, NVL(col3[cv(rn) - 1], 0), col3[cv(rn)]),
    col4[any] = DECODE(col4[cv(rn)], 0, NVL(col4[cv(rn) - 1], 0), col4[cv(rn)])
  )
```

## MODEL 절 구성 요소

DIMENSION BY: 스프레드시트 셀 좌표와 유사하게 row_number()를 사용하여 행 인덱싱을 설정합니다.

MEASURES: 각 셀 컨텍스트 내에서 계산될 열 값을 정의합니다.

RULES: DECODE가 값이 0인지 확인한 다음 cv(rn) - 1을 통해 이전 값을 가져오는 할당 로직을 적용합니다.

## 결론

이 글에서는 윈도우 함수를 더 간단하고 접근하기 쉬운 방법으로 추천합니다. MODEL 절은 강력한 기능을 제공하지만 학습 곡선이 더 가파릅니다. 두 기술 모두 Oracle SQL에서 희소 데이터 채우기를 효과적으로 해결합니다. 관련 개념으로는 누적 합계 계산 방법론이 있습니다.

## 참고 자료

- Stack Overflow 사용자 aljassi의 원래 질문
- Stack Overflow 사용자 nop77svk와 MT0의 솔루션
- MODEL 절 기능에 대한 Oracle 백서
- 윈도우 함수와 누적 합계에 대한 jOOQ 블로그 게시물
