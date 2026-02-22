# SQL을 사용하여 Stack Overflow에서 Derby, H2, HSQLDB의 인기도 계산하기

> 원문: https://blog.jooq.org/using-sql-to-calculate-the-popularity-on-stack-overflow-of-derby-h2-and-hsqldb/

Stack Exchange는 공개 SQL 웹 API인 [Stack Exchange Data Explorer](https://data.stackexchange.com/)를 통해 데이터를 공유합니다. 이를 사용하면 분석 쿼리를 실행할 수 있습니다. 이 글에서는 세 가지 Java 인메모리 데이터베이스인 Derby, H2, HSQLDB의 Stack Overflow 질문 수를 기반으로 인기도를 분석하는 방법을 보여드리겠습니다.

## 접근 방식

목표는 시간에 따른 누적 질문 수를 계산하는 것입니다. 이를 위해 러닝 토탈(running total) 방식을 사용합니다. 기본 아이디어는 다음과 같습니다:

1. 각 데이터베이스 태그별로 일별 질문 수를 집계합니다
2. 윈도우 함수를 사용하여 날짜 순서대로 누적 합계를 계산합니다

## SQL 쿼리

쿼리는 중첩된 SELECT 구조와 윈도우 함수를 사용합니다:

```sql
SELECT
  d,
  SUM(h2)     OVER (ORDER BY d) AS h2,
  SUM(hsqldb) OVER (ORDER BY d) AS hsqldb,
  SUM(derby)  OVER (ORDER BY d) AS derby
FROM (
  SELECT
    CAST(CreationDate AS DATE) AS d,
    COUNT(CASE WHEN Tags LIKE '%<h2>%' THEN 1 END) AS h2,
    COUNT(CASE WHEN Tags LIKE '%<hsqldb>%' THEN 1 END) AS hsqldb,
    COUNT(CASE WHEN Tags LIKE '%<derby>%' THEN 1 END) AS derby
  FROM Posts
  GROUP BY CAST(CreationDate AS DATE)
) AS DailyPosts
ORDER BY d ASC
```

## 쿼리 설명

### 내부 쿼리 (서브쿼리)

내부 쿼리는 피벗 테이블을 생성합니다. `Posts` 테이블에서 데이터를 가져와 날짜별로 그룹화하고, `CASE` 표현식을 사용하여 각 데이터베이스 태그가 포함된 질문 수를 조건부로 집계합니다.

- `CAST(CreationDate AS DATE)`: 생성 날짜를 날짜 형식으로 변환합니다
- `COUNT(CASE WHEN Tags LIKE '%<h2>%' THEN 1 END)`: h2 태그가 포함된 질문 수를 계산합니다
- 같은 방식으로 hsqldb와 derby 태그도 집계합니다

이 접근 방식은 PIVOT 절을 사용할 수 없는 경우의 대안적인 집계 방법입니다.

### 외부 쿼리

외부 쿼리는 `SUM() OVER (ORDER BY d)` 윈도우 함수를 적용하여 첫 번째 날부터 현재 날짜까지의 누적 합계(러닝 토탈)를 계산합니다. 이렇게 하면 시간이 지남에 따라 각 데이터베이스에 대한 질문이 어떻게 축적되었는지 볼 수 있습니다.

## 분석 결과

분석 결과에 따르면, 세 데이터베이스 모두 대략적으로 비슷한 '인기도'를 보이지만, H2가 모멘텀을 얻어가는 것으로 보이고 HSQLDB는 약간의 하락세에 있습니다.

## 주의사항

이 지표는 Stack Overflow 질문 수량을 반영하는 것이지 실제 시장 점유율을 나타내는 것은 아닙니다. 질문이 많다는 것은 사용자가 어려움을 겪고 있다는 것을 의미할 수도 있으므로, 반드시 채택 성공을 나타내는 것은 아닙니다.

## 관련 SQL 개념

이 글에서 다룬 기술들과 관련된 SQL 개념들:

- PIVOT 절: 행을 열로 변환하는 기능
- UNPIVOT 연산: 열을 행으로 변환하는 기능
- 러닝 토탈 계산: 윈도우 함수를 사용한 누적 합계 계산
- 조건부 집계: CASE 표현식을 사용한 조건별 집계

## 직접 실행해보기

이 쿼리는 [Stack Exchange Data Explorer](https://data.stackexchange.com/)에서 직접 실행해볼 수 있습니다. 공개 API이므로 누구나 자유롭게 데이터를 탐색할 수 있습니다.
