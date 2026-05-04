# Tempo 활용 사례 (Solutions and Use Cases)

> 이 문서는 Grafana Tempo 공식 문서의 "Solutions and use cases" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/solutions-with-traces/

---

## 목차

1. [개요](#개요)
2. [APM (애플리케이션 성능 모니터링)](#apm-애플리케이션-성능-모니터링)
3. [장애 디버깅 / RCA](#장애-디버깅--rca)
4. [SLO 모니터링](#slo-모니터링)
5. [의존성 분석 (Service Dependencies)](#의존성-분석-service-dependencies)
6. [성능 최적화 / 병목 식별](#성능-최적화--병목-식별)
7. [데이터베이스 호출 분석](#데이터베이스-호출-분석)
8. [외부 서비스 호출 모니터링](#외부-서비스-호출-모니터링)
9. [에러 분석](#에러-분석)
10. [비즈니스 메트릭 추출](#비즈니스-메트릭-추출)

---

## 개요

분산 트레이싱은 다음 문제 해결에 강점:

- "왜 이 요청이 느린가?"
- "어떤 서비스가 에러를 일으키는가?"
- "특정 사용자의 요청은 어떤 경로를 거쳤는가?"
- "서비스 A에 대한 변경이 B에 어떤 영향을 미치는가?"

### 메트릭/로그와의 비교

| 데이터 | 답할 수 있는 질문 |
|--------|------------------|
| 메트릭 | "얼마나 많이? 얼마나 빠르게?" (집계, 추세) |
| 로그 | "무엇이 일어났는가?" (이벤트 상세) |
| 트레이스 | "어디서 시간이 소비되었는가? 어떻게 흘렀는가?" (인과 관계) |

---

## APM (애플리케이션 성능 모니터링)

### Tempo + Span Metrics 조합

[Metrics Generator](./05_metrics_generator.md) 활성화 → 자동 RED 메트릭 생성.

### 표준 APM 대시보드

각 서비스에 대해:

#### Rate (요청 비율)
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

#### Errors (에러율)
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

#### Duration (지연 P50/P95/P99)
```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket[5m])
  )
)
```

### Service Map

자동 생성된 서비스 그래프로 의존성 시각화.

### 활용 워크플로

```
1. APM 대시보드에서 이상 감지 (예: P99 급증)
   ↓
2. Service Graph에서 어떤 서비스가 영향 받는지 확인
   ↓
3. 해당 서비스의 트레이스 검색 (TraceQL)
   ↓
4. 느린 트레이스의 스팬 분석 → 병목 식별
```

---

## 장애 디버깅 / RCA

### 시나리오: 사용자 보고 "결제가 안 됨"

#### 1. 사용자 ID로 트레이스 검색

```traceql
{ .user.id = "u-12345" && resource.service.name = "checkout" }
```

#### 2. 에러 트레이스 필터

```traceql
{ .user.id = "u-12345" && status = error }
```

#### 3. 트레이스 상세 보기

스팬 트리에서:
- 어디서 에러 발생했는지
- 에러 메시지 (스팬 events)
- 관련 속성 (HTTP status, DB 에러 등)

#### 4. 관련 로그 확인

Trace ID로 Loki에서 로그 조회:

```logql
{app=~".+"} |= "trace_id=abc123"
```

### 시나리오: 503 에러 폭증

#### 1. 메트릭에서 시점 확인

```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[1m])
)
```

#### 2. Exemplars로 트레이스 점프

Grafana 패널의 점을 클릭 → 해당 시점의 트레이스로 이동.

#### 3. 트레이스 패턴 분석

여러 에러 트레이스를 비교하여 공통점 찾기:
- 동일 다운스트림 서비스 호출 실패?
- 특정 DB 쿼리 타임아웃?
- 외부 API 응답 실패?

---

## SLO 모니터링

### Latency SLO

"95%의 요청이 500ms 이내에 처리되어야 함"

```promql
sum by (service) (
  rate(traces_spanmetrics_latency_bucket{le="0.5"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_latency_count[5m])
)
> 0.95
```

### Availability SLO

"99.9% 요청 성공"

```promql
1 - (
  sum by (service) (
    rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
  )
  /
  sum by (service) (
    rate(traces_spanmetrics_calls_total[5m])
  )
)
> 0.999
```

### Error Budget Burn Rate

```promql
# 1시간 윈도우, 14.4배 burn (99.9% SLO 30일 burn)
(
  sum(rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[1h]))
  /
  sum(rate(traces_spanmetrics_calls_total[1h]))
)
> (1 - 0.999) * 14.4
```

---

## 의존성 분석 (Service Dependencies)

### Service Graph 활용

자동 생성된 그래프로:

- 어떤 서비스가 어떤 서비스를 호출하는지
- 호출 빈도와 에러율
- 병목 의심 구간 식별

### 변경 영향 분석

서비스 A를 변경할 때:
- 누가 A를 호출하는가? (downstream consumers)
- A의 SLO가 깨지면 누가 영향 받는가?

```promql
sum by (client) (
  rate(traces_service_graph_request_total{server="service-a"}[5m])
)
```

### 종속성 시각화 PromQL

```promql
# 서비스 간 호출 매트릭스
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)

# 서비스 간 에러율
sum by (client, server) (
  rate(traces_service_graph_request_failed_total[5m])
)
/
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
```

---

## 성능 최적화 / 병목 식별

### 느린 트레이스 찾기

```traceql
{ resource.service.name = "api" && duration > 1s }
```

### 가장 느린 스팬 분석

트레이스 내에서 어떤 스팬이 시간을 가장 많이 소비하는지:

```traceql
{ resource.service.name = "api" } 
| max(duration) by (.span.name)
```

### N+1 쿼리 패턴 감지

```traceql
{ resource.service.name = "api" } 
| count() by (.db.statement) 
> 100
```

같은 트레이스 내에서 동일 SQL이 100번 이상 실행되면 N+1 의심.

### 직렬 vs 병렬 호출

스팬 시각화에서:
- 가로로 나란히: 직렬 (느림)
- 위아래로: 병렬 (빠름)

병렬화 가능한 호출 식별 → 코드 개선.

---

## 데이터베이스 호출 분석

### 느린 쿼리

```traceql
{ .db.system = "postgresql" && duration > 100ms }
```

### 가장 자주 호출되는 쿼리

```traceql
{ .db.system = "postgresql" } 
| count() by (.db.statement)
```

### 쿼리별 평균 지연

```traceql
{ .db.system = "postgresql" } 
| avg(duration) by (.db.operation)
```

### 트랜잭션 분석

```traceql
{ .db.statement =~ "BEGIN.*" } 
| max(duration) > 5s
```

장기 트랜잭션 감지.

---

## 외부 서비스 호출 모니터링

### 외부 API 호출 식별

```traceql
{ kind = client && .http.url =~ "https://(?!internal).*" }
```

### 외부 의존성별 신뢰성

```traceql
{ .http.url =~ "https://payment-gateway.*" } 
| (count(status=error) / count()) > 0.01
```

### 타임아웃 식별

```traceql
{ kind = client && status = error && events.exception.type =~ ".*Timeout.*" }
```

### 서드파티 API 비용 분석

```promql
# 외부 API 호출 횟수 (비용 추정)
sum by (target_service) (
  rate(traces_spanmetrics_calls_total{
    span_kind="SPAN_KIND_CLIENT",
    target_service=~"payment.*|sms.*|email.*"
  }[1h])
) * 3600
```

---

## 에러 분석

### 에러 유형별 분포

```traceql
{ status = error } 
| count() by (events.exception.type)
```

### 에러 발생 위치

```traceql
{ status = error } 
| count() by (resource.service.name, span.name)
```

### 에러 전파 추적

```traceql
{ status = error && resource.service.name = "frontend" } 
&& 
{ status = error && resource.service.name != "frontend" }
```

frontend의 에러가 다운스트림 에러와 함께 발생.

### 에러 지속 시간 (얼마나 오래 걸리고 실패하는가)

```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket{status_code="STATUS_CODE_ERROR"}[5m])
  )
)
```

빠른 실패 vs 타임아웃 후 실패 구분.

---

## 비즈니스 메트릭 추출

### 사용자 행동 추적

스팬 속성에 비즈니스 정보 포함:

```python
with tracer.start_as_current_span("checkout") as span:
    span.set_attribute("user.tier", user.tier)
    span.set_attribute("cart.items", len(cart))
    span.set_attribute("cart.total", cart.total)
    span.set_attribute("payment.method", payment.method)
```

### 분석

```traceql
# 결제 방법별 성공률
{ name = "checkout" } 
| (count(status=ok) / count()) by (.payment.method)

# Tier별 평균 카트 크기
{ name = "checkout" } 
| avg(.cart.items) by (.user.tier)

# 환불 트레이스
{ name = "refund" && .refund.amount > 100 }
```

### TraceQL Metrics로 비즈니스 KPI

```traceql
# 분당 결제 성공
{ name = "checkout" && status = ok } 
| rate() 
by (.payment.method)

# P95 결제 처리 시간
{ name = "checkout" } 
| quantile_over_time(duration, 0.95) 
by (.payment.method)
```

이러한 메트릭은 별도 메트릭 코드 없이 트레이스에서 즉시 추출.
