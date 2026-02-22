> 원문: https://blog.jooq.org/a-little-known-sql-feature-use-logical-windowing-to-aggregate-sliding-ranges/

# 잘 알려지지 않은 SQL 기능: 논리적 윈도잉(Logical Windowing)을 사용한 슬라이딩 범위 집계

SQL 윈도우 함수(Window Function)에는 강력하지만 잘 활용되지 않는 기능이 있습니다. 바로 논리적 윈도잉(Logical Windowing)을 통한 시간 기반 집계입니다. 이 기능은 특히 시계열 데이터나 시간 관련 리포팅에서 매우 유용합니다.

## 핵심 개념

결제(payment) 데이터를 사용하여 세 가지 서로 다른 집계 패턴을 살펴보겠습니다:

1. 시간별 그룹화: "특정 결제와 동일한 시간대에 몇 건의 결제가 있었는가?"
2. 시간 내 누적: "특정 결제가 발생하기 전, 동일한 시간대에 몇 건의 결제가 있었는가?"
3. 슬라이딩 구간: "특정 결제 시점으로부터 1시간 이내에 몇 건의 결제가 있었는가?"

## SQL 구문 예제

### 첫 번째 - 시간별 파티션

```sql
COUNT(*) OVER (
  PARTITION BY TRUNC(payment_date, 'HH24')
)
```

이 쿼리는 `PARTITION BY TRUNC(payment_date, 'HH24')`를 사용하여 동일한 시간대의 결제 건수를 계산합니다. 예를 들어, 오후 2시대의 모든 결제는 같은 그룹으로 묶입니다.

### 두 번째 - 누적 정렬

```sql
COUNT(*) OVER (
  PARTITION BY TRUNC(payment_date, 'HH24')
  ORDER BY payment_date
)
```

`ORDER BY payment_date`를 추가하면 기본 프레임(frame)이 `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`로 설정됩니다. 이는 동일한 시간대 내에서 현재 행까지의 누적 카운트를 계산합니다.

### 세 번째 - INTERVAL을 사용한 논리적 윈도잉

```sql
COUNT(*) OVER (
  ORDER BY payment_date
  RANGE BETWEEN INTERVAL '1' HOUR PRECEDING AND CURRENT ROW
)
```

이것이 바로 논리적 윈도잉의 핵심입니다. `INTERVAL` 데이터 타입을 사용하여 동적인 시간 기반 프레임을 생성합니다. 이 쿼리는 고정된 시간 경계(예: 정각 기준)가 아닌, 각 행을 기준으로 1시간 이내의 결제 건수를 계산합니다.

## 물리적 윈도잉 vs 논리적 윈도잉

일반적인 윈도우 함수에서 `ROWS BETWEEN`을 사용하면 물리적 윈도잉(Physical Windowing)이 적용됩니다. 이는 고정된 행 수를 기준으로 프레임을 정의합니다:

```sql
-- 물리적 윈도잉: 현재 행 포함 3개 행
COUNT(*) OVER (
  ORDER BY payment_date
  ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
)
```

반면 `RANGE BETWEEN`과 `INTERVAL`을 함께 사용하면 논리적 윈도잉이 적용됩니다. 이는 값의 범위(시간, 숫자 등)를 기준으로 프레임을 정의합니다:

```sql
-- 논리적 윈도잉: 1시간 범위 내의 모든 행
COUNT(*) OVER (
  ORDER BY payment_date
  RANGE BETWEEN INTERVAL '1' HOUR PRECEDING AND CURRENT ROW
)
```

## 데이터베이스 지원

이 기능은 원래 Oracle에서 문서화되었으며, 이후 PostgreSQL 11에서도 지원하게 되었습니다. 시계열 분석에 매우 유용한 기능임에도 불구하고 아직 널리 사용되지 않고 있습니다.

## 실용적인 활용 사례

- 실시간 대시보드: 최근 1시간/24시간 동안의 이벤트 집계
- 이상 탐지(Anomaly Detection): 특정 시간 윈도우 내의 비정상적인 패턴 감지
- 이동 평균(Moving Average): 시간 기반 이동 평균 계산
- 트래픽 분석: 슬라이딩 시간 구간 내의 요청 수 추적

## 결론

논리적 윈도잉은 복잡한 셀프 조인(self-join)이나 서브쿼리 없이도 정교한 시간 기반 리포팅을 가능하게 합니다. 분석 대시보드나 시계열 분석에서 매우 유용한 기능이므로, 지원하는 데이터베이스를 사용한다면 적극 활용해 보시기 바랍니다.
