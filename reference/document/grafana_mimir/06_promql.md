# Mimir PromQL 쿼리

> 이 문서는 Grafana Mimir 공식 문서의 "Query metric data" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/references/http-api/

---

## 목차

1. [PromQL 개요](#promql-개요)
2. [데이터 타입](#데이터-타입)
3. [Selectors (셀렉터)](#selectors-셀렉터)
4. [연산자](#연산자)
5. [집계 함수](#집계-함수)
6. [Built-in 함수](#built-in-함수)
7. [Recording Rules](#recording-rules)
8. [Mimir HTTP API](#mimir-http-api)
9. [Native Histograms](#native-histograms)
10. [Exemplars](#exemplars)

---

## PromQL 개요

PromQL(Prometheus Query Language)은 Mimir와 Prometheus의 표준 쿼리 언어입니다.

### 4가지 쿼리 타입

| 타입 | 예시 |
|------|------|
| **Instant Vector** | `up` |
| **Range Vector** | `up[5m]` |
| **Scalar** | `42` |
| **String** | `"hello"` (드물게 사용) |

---

## 데이터 타입

### Instant Vector

특정 시점에서 시계열의 현재 값.

```promql
http_requests_total
```

### Range Vector

특정 시점에서 과거 일정 기간의 값들.

```promql
http_requests_total[5m]
```

### Scalar

단일 숫자 값.

```promql
3.14
```

---

## Selectors (셀렉터)

### Label 매칭 연산자

| 연산자 | 의미 |
|--------|------|
| `=` | 정확 일치 |
| `!=` | 일치하지 않음 |
| `=~` | 정규식 일치 |
| `!~` | 정규식 불일치 |

```promql
http_requests_total{method="GET"}
http_requests_total{status=~"5.."}
http_requests_total{job!="prometheus"}
```

### 메트릭 이름 매처

`__name__` 라벨로 매칭 가능:

```promql
{__name__=~"http_requests.*"}
```

### Range 셀렉터

```promql
http_requests_total[5m]      # 5분
http_requests_total[1h]      # 1시간
http_requests_total[1d]      # 1일
http_requests_total[1w]      # 1주
```

### Offset

```promql
http_requests_total offset 5m
http_requests_total[5m] offset 1h
```

### `@` Modifier

특정 시점 평가:

```promql
http_requests_total @ 1700000000
http_requests_total[5m] @ 1700000000
http_requests_total @ start()
http_requests_total @ end()
```

---

## 연산자

### 산술 연산

```promql
+, -, *, /, %, ^
```

```promql
node_memory_MemFree_bytes / node_memory_MemTotal_bytes
```

### 비교 연산

```promql
==, !=, >, <, >=, <=
```

기본은 필터링 (조건 만족하는 시계열만 반환):

```promql
node_memory_MemFree_bytes < 1000000000
```

`bool` 모디파이어로 0/1 반환:

```promql
node_memory_MemFree_bytes < bool 1000000000
```

### 논리/집합 연산

| 연산자 | 의미 |
|--------|------|
| `and` | 두 셋의 교집합 |
| `or` | 합집합 |
| `unless` | 차집합 |

```promql
metric_a and metric_b
metric_a or metric_b
metric_a unless metric_b
```

### 벡터 매칭

```promql
# one-to-one (기본)
metric_a + metric_b

# 라벨 무시
metric_a + ignoring(method) metric_b

# 특정 라벨로만 매칭
metric_a + on(instance) metric_b

# many-to-one
sum_metric * on(instance) group_left small_metric

# one-to-many
small_metric * on(instance) group_right sum_metric
```

---

## 집계 함수

| 함수 | 설명 |
|------|------|
| `sum` | 합계 |
| `avg` | 평균 |
| `min` | 최소 |
| `max` | 최대 |
| `count` | 개수 |
| `count_values` | 고유 값별 개수 |
| `stddev` | 표준편차 |
| `stdvar` | 분산 |
| `topk` | 상위 K |
| `bottomk` | 하위 K |
| `quantile` | 분위수 |
| `group` | 그룹화만 |

### 그룹화

```promql
# 라벨별 그룹화
sum by (instance) (rate(http_requests_total[5m]))

# 특정 라벨만 제외
sum without (method) (rate(http_requests_total[5m]))

# 상위 5개
topk(5, sum by (instance) (rate(http_requests_total[5m])))

# 분위수
quantile by (job) (0.95, http_request_duration_seconds_bucket)
```

---

## Built-in 함수

### 시계열 함수

| 함수 | 설명 |
|------|------|
| `rate(v[d])` | 평균 초당 증가율 (Counter) |
| `irate(v[d])` | 마지막 두 데이터로 즉시 비율 |
| `increase(v[d])` | 시간 윈도우 내 증가량 |
| `delta(v[d])` | Gauge의 변화량 |
| `idelta(v[d])` | Gauge의 마지막 변화량 |
| `deriv(v[d])` | Gauge의 도함수 |
| `predict_linear(v[d], t)` | t초 후 예측값 |
| `resets(v[d])` | Counter 재시작 횟수 |
| `changes(v[d])` | Gauge 변경 횟수 |

### 시간 윈도우 집계

| 함수 | 설명 |
|------|------|
| `avg_over_time(v[d])` | 평균 |
| `min_over_time(v[d])` | 최소 |
| `max_over_time(v[d])` | 최대 |
| `sum_over_time(v[d])` | 합계 |
| `count_over_time(v[d])` | 개수 |
| `quantile_over_time(q, v[d])` | 분위수 |
| `stddev_over_time(v[d])` | 표준편차 |
| `last_over_time(v[d])` | 마지막 값 |
| `present_over_time(v[d])` | 존재 여부 (1) |
| `absent_over_time(v[d])` | 부재 여부 |

### 히스토그램 함수

```promql
# P95 응답시간
histogram_quantile(0.95,
  sum by (le) (rate(http_request_duration_seconds_bucket[5m]))
)

# Native Histogram
histogram_quantile(0.99, sum by (le) (rate(my_native_histogram[5m])))
```

### 라벨 조작

```promql
# 라벨 추가/변경
label_replace(v, "dst", "$1", "src", "(.*)")

# 라벨 결합
label_join(v, "dst", "-", "src1", "src2")
```

### 시간 함수

```promql
time()                    # 현재 Unix 시간
timestamp(v)              # 시계열 샘플 시간
year(v)                   # 연도
month(v)                  # 월
day_of_month(v)
day_of_week(v)
hour(v)
minute(v)
days_in_month(v)
```

### 수학 함수

```promql
abs, ceil, floor, round, exp, ln, log2, log10, sqrt, sgn
sin, cos, tan, asin, acos, atan, sinh, cosh, tanh
deg, rad, pi
```

### 정렬

```promql
sort(v)        # 오름차순
sort_desc(v)   # 내림차순
```

### 클램프

```promql
clamp(v, min, max)
clamp_min(v, min)
clamp_max(v, max)
```

---

## Recording Rules

자주 쓰는 쿼리를 미리 계산하여 새 메트릭으로 저장.

### 룰 파일

```yaml
groups:
  - name: api_recording
    interval: 30s
    rules:
      - record: api:requests:rate5m
        expr: sum by (job, instance) (rate(http_requests_total[5m]))
      
      - record: api:request_duration:p95
        expr: |
          histogram_quantile(0.95,
            sum by (job, le) (rate(http_request_duration_seconds_bucket[5m]))
          )
```

### Mimir Ruler에 등록

```bash
mimirtool rules load --address=http://mimir:9009 \
  --id=tenant-1 \
  recording-rules.yaml
```

---

## Mimir HTTP API

Prometheus HTTP API와 호환.

### Instant Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=up' \
  --data-urlencode 'time=1700000000'
```

### Range Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=rate(http_requests_total[5m])' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

### Series

```bash
curl -G "http://mimir:9009/prometheus/api/v1/series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'match[]={__name__=~"http_.*"}' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600'
```

### Labels

```bash
# 모든 라벨 이름
curl "http://mimir:9009/prometheus/api/v1/labels" \
  -H "X-Scope-OrgID: tenant-1"

# 특정 라벨 값
curl "http://mimir:9009/prometheus/api/v1/label/job/values" \
  -H "X-Scope-OrgID: tenant-1"
```

### 메타데이터

```bash
curl "http://mimir:9009/prometheus/api/v1/metadata" \
  -H "X-Scope-OrgID: tenant-1"
```

### Cardinality 분석

```bash
# 레이블 카디널리티
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/label_names" \
  -H "X-Scope-OrgID: tenant-1"

# 활성 시계열 통계
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/active_series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'selector={job="prometheus"}'
```

---

## Native Histograms

Prometheus 2.40+ 에서 도입. Mimir 완전 지원.

### 장점

- 고해상도 분포 (자동 버킷)
- 적은 시계열 (단일 시계열로 모든 분위수)
- 빠른 쿼리

### 활성화

```yaml
limits:
  native_histograms_ingestion_enabled: true
```

### 쿼리

```promql
# 클래식 히스토그램과 동일
histogram_quantile(0.95, sum by (le) (rate(my_metric[5m])))

# Native Histogram 전용
histogram_count(rate(my_metric[5m]))
histogram_sum(rate(my_metric[5m]))
histogram_avg(rate(my_metric[5m]))
histogram_fraction(0, 100, rate(my_metric[5m]))
```

---

## Exemplars

메트릭 데이터 포인트와 트레이스를 연결.

### 활성화

```yaml
limits:
  max_global_exemplars_per_user: 100000

ingester:
  max_exemplars: 100000
```

### Prometheus에서 생성

OpenMetrics 형식 메트릭에 `# {trace_id="abc"} value` 추가.

### 쿼리 (Range Query에 추가)

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  --data-urlencode 'query=histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

응답에 `exemplars` 필드 포함.

### Grafana 통합

Prometheus/Mimir 데이터 소스에서 **Exemplars** 활성화 + Tempo 데이터 소스 연결로 클릭 시 트레이스 이동.
