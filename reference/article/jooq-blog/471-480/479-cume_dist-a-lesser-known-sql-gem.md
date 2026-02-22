# CUME_DIST(), 잘 알려지지 않은 SQL 보석

> 원문: https://blog.jooq.org/cume_dist-a-lesser-known-sql-gem/

저는 이 블로그에서 분석 함수/윈도우 함수에 대해 많이 이야기했습니다. 모든 개발자가 알아야 할 SQL 어휘에서 매우 핵심적인 기능이지만, 많은 개발자들은 여전히 그것의 존재조차 인식하지 못하기 때문입니다. 가장 과소평가된 윈도우 함수 중 하나가 바로 `CUME_DIST()`입니다.

## CUME_DIST()란 무엇인가?

이 함수는 정렬된 결과 집합 전체(또는 파티션 내)에서 값이 얼마나 진행되었는지를 보여줍니다. 결과는 항상 다음 범위 내에 있습니다:

```
0 < CUME_DIST() <= 1
```

이것이 무엇을 의미하는지 예제를 통해 살펴보겠습니다:

```sql
SELECT
  ename,
  CUME_DIST() OVER (ORDER BY ename) fraction1,
  ROWNUM / (MAX(ROWNUM) OVER()) fraction2
FROM emp
ORDER BY ename
```

위 쿼리의 결과는 다음과 같습니다:

| ENAME  | FRACTION1 | FRACTION2 |
|--------|-----------|-----------|
| ALLEN  | 0.08      | 0.08      |
| BLAKE  | 0.17      | 0.17      |
| CLARK  | 0.25      | 0.25      |
| FORD   | 0.33      | 0.33      |
| JAMES  | 0.42      | 0.42      |
| JONES  | 0.50      | 0.50      |
| KING   | 0.58      | 0.58      |
| MARTIN | 0.67      | 0.67      |
| MILLER | 0.75      | 0.75      |
| SCOTT  | 0.83      | 0.83      |
| SMITH  | 0.92      | 0.92      |
| WARD   | 1.00      | 1.00      |

보시다시피, `fraction1`과 `fraction2`는 동일한 값을 산출합니다. 이것이 누적 분포가 의미하는 바입니다. 정렬 순서상 첫 번째 직원 ALLEN은 8%에 위치하고, 마지막 직원 WARD는 100%에 위치합니다.

## 두 가지 함수 형태

`CUME_DIST()`는 두 가지 방식으로 사용할 수 있습니다:

윈도우 함수로서:

```sql
CUME_DIST() OVER (ORDER BY ename)
```

순서 집계 함수(Ordered Aggregate Function)로서:

Oracle과 SQL 표준은 `CUME_DIST()`를 "순서 집계 함수"로도 지원합니다. 이 형태는 `WITHIN GROUP` 구문을 사용합니다:

```sql
CUME_DIST(value) WITHIN GROUP (ORDER BY column_name)
```

이 형태는 그룹 기반 계산에 유용하며, 특정 값이 그룹 내에서 어디에 위치하는지 계산할 수 있습니다.

## jOOQ에서의 지원

jOOQ 라이브러리는 윈도우 함수 지원을 위해 `cumeDist()` 메서드를 제공합니다. 버전 3.4부터는 순서 집계 함수 기능도 추가되었습니다.

## 결론

보고서 및 통계 쿼리를 작성할 때, 특히 데이터셋 내에서 상대적 위치를 분석해야 할 때 `CUME_DIST()`를 SQL 어휘에 추가하는 것이 좋습니다. 이 함수는 데이터 분포를 이해하는 데 매우 유용한 도구입니다.
