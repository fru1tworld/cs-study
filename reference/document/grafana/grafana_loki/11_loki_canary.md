# Loki Canary

> 이 문서는 Grafana Loki 공식 문서의 Loki Canary 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/operations/loki-canary/

---

## 목차

1. [개요](#개요)
2. [동작 원리](#동작-원리)
3. [설치 및 실행](#설치-및-실행)
4. [구성 옵션](#구성-옵션)
5. [노출 메트릭](#노출-메트릭)
6. [알림 룰 예시](#알림-룰-예시)
7. [운영 시 활용](#운영-시-활용)

---

## 개요

**Loki Canary** 는 Loki 클러스터의 가용성과 데이터 무결성을 지속적으로 검증하는 도구입니다.

### 목적

- 로그 손실 감지
- 수집 지연 측정
- 쿼리 가용성 검증
- 종단간(end-to-end) SLO 관측

### 위치

Loki 저장소의 [`cmd/loki-canary`](https://github.com/grafana/loki/tree/main/cmd/loki-canary) 에 포함되어 있으며, 단독 바이너리/컨테이너로 배포됩니다.

---

## 동작 원리

```
[Loki Canary]
    |
    +--> 1. 일정 간격으로 고유 타임스탬프 로그 라인 생성
    |       (예: "1700000000123 abc-uuid-line")
    |
    +--> 2. stdout으로 출력 → Promtail/Alloy가 수집 → Loki 푸시
    |
    +--> 3. 일정 시간 후 Loki에 쿼리하여 자기가 생성한 라인 조회
    |
    +--> 4. 결과 비교
            - 생성된 라인 수 vs 쿼리로 받은 라인 수 → 손실 감지
            - 생성 시각 vs 수집 시각 → 지연 측정
            - 쿼리 실패 → 가용성 측정
```

### 검증 항목

| 항목 | 메트릭 |
|------|--------|
| 생성된 라인 수 | `loki_canary_entries_total` |
| 누락된 라인 수 | `loki_canary_missing_entries_total` |
| 중복된 라인 수 | `loki_canary_duplicate_entries_total` |
| 쿼리 실패 횟수 | `loki_canary_unexpected_entries_total` |
| 종단간 지연 | `loki_canary_response_latency` |

---

## 설치 및 실행

### Docker

```bash
docker run -d \
  --name loki-canary \
  grafana/loki-canary:latest \
  -addr=loki:3100 \
  -labelname=instance \
  -labelvalue=canary-1
```

### Kubernetes (DaemonSet)

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: loki-canary
  namespace: loki
spec:
  selector:
    matchLabels:
      app: loki-canary
  template:
    metadata:
      labels:
        app: loki-canary
    spec:
      serviceAccountName: loki-canary
      containers:
        - name: loki-canary
          image: grafana/loki-canary:latest
          args:
            - -addr=loki-gateway.loki.svc.cluster.local:80
            - -labelname=pod
            - -labelvalue=$(POD_NAME)
            - -interval=1s
            - -size=100
            - -wait=3m
            - -tenant-id=fake
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
          ports:
            - name: metrics
              containerPort: 3500
```

### Helm 서브차트

`grafana/loki` Helm 차트에 포함:

```yaml
lokiCanary:
  enabled: true
  args:
    - -interval=1s
    - -size=100
  resources:
    requests:
      cpu: 10m
      memory: 30Mi
```

---

## 구성 옵션

### 핵심 플래그

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `-addr` | `localhost:3100` | Loki 주소 |
| `-labelname` | `name` | 라벨 이름 |
| `-labelvalue` | `loki-canary` | 라벨 값 |
| `-tls` | false | TLS 사용 |
| `-cert-file` | "" | 클라이언트 인증서 |
| `-key-file` | "" | 클라이언트 키 |
| `-ca-file` | "" | CA 인증서 |
| `-user` | "" | Basic Auth 사용자 |
| `-pass` | "" | Basic Auth 비밀번호 |
| `-tenant-id` | "" | X-Scope-OrgID |

### 생성 옵션

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `-interval` | `1s` | 라인 생성 간격 |
| `-size` | `100` | 라인 크기 (bytes) |

### 검증 옵션

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `-wait` | `60s` | 라인 생성 후 쿼리 대기 시간 |
| `-max-wait` | `5m` | 최대 대기 시간 |
| `-pruning-interval` | `60s` | 메모리 정리 주기 |
| `-buckets` | `10` | 히스토그램 버킷 수 |
| `-spot-check-interval` | `15m` | 스팟 체크 간격 |
| `-spot-check-max` | `4h` | 스팟 체크 최대 범위 |
| `-spot-check-query-rate` | `1m` | 스팟 체크 쿼리 빈도 |
| `-spot-check-initial-wait` | `10s` | 첫 스팟 체크 대기 |
| `-metric-test-interval` | `1h` | 메트릭 테스트 간격 |
| `-metric-test-range` | `24h` | 메트릭 테스트 범위 |
| `-query-timeout` | `10s` | 쿼리 타임아웃 |

### 메트릭 노출

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `-port` | `3500` | 메트릭 포트 |
| `-out-of-order-min` | `10` | 순서 어긋남 최소 |
| `-out-of-order-max` | `60` | 순서 어긋남 최대 |
| `-out-of-order-percentage` | `10` | 순서 어긋남 비율 |

---

## 노출 메트릭

### 핵심 메트릭

```
# 생성/누락/중복
loki_canary_entries_total
loki_canary_missing_entries_total
loki_canary_duplicate_entries_total
loki_canary_unexpected_entries_total
loki_canary_out_of_order_entries_total

# 응답 지연 히스토그램
loki_canary_response_latency_seconds_bucket{}
loki_canary_response_latency_seconds_sum
loki_canary_response_latency_seconds_count

# 쿼리 통계
loki_canary_websocket_missing_entries_total
loki_canary_spot_check_entries_total
loki_canary_spot_check_missing_entries_total
loki_canary_spot_check_request_total
loki_canary_spot_check_error_total

# 메트릭 테스트
loki_canary_metric_test_expected
loki_canary_metric_test_actual
loki_canary_metric_test_request_total
loki_canary_metric_test_error_total
```

---

## 알림 룰 예시

```yaml
groups:
  - name: loki_canary
    rules:
      - alert: LokiCanaryHighMissing
        expr: |
          sum(rate(loki_canary_missing_entries_total[5m]))
          /
          sum(rate(loki_canary_entries_total[5m]))
          > 0.01
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Loki Canary가 1% 이상 로그 누락 감지"
      
      - alert: LokiCanaryHighLatency
        expr: |
          histogram_quantile(0.99,
            sum by (le) (rate(loki_canary_response_latency_seconds_bucket[5m]))
          ) > 30
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Loki 종단간 지연 30초 초과 (P99)"
      
      - alert: LokiCanaryDown
        expr: up{job="loki-canary"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Loki Canary 다운"
      
      - alert: LokiCanarySpotCheckFailure
        expr: |
          sum(rate(loki_canary_spot_check_missing_entries_total[1h]))
          /
          sum(rate(loki_canary_spot_check_entries_total[1h]))
          > 0.01
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Loki Canary 스팟 체크 1% 이상 실패"
```

---

## 운영 시 활용

### SLO 측정

종단간 가용성 SLO:

```promql
# 가용성 (라인 손실률)
1 - (
  sum(rate(loki_canary_missing_entries_total[5m]))
  /
  sum(rate(loki_canary_entries_total[5m]))
)
```

### 지연 SLO

```promql
# P99 지연
histogram_quantile(0.99,
  sum by (le) (rate(loki_canary_response_latency_seconds_bucket[5m]))
)
```

### 위치별 카나리

여러 클러스터/리전에 배포하여 위치별 가용성 비교:

```yaml
- name: ${REGION}-canary
  args:
    - -addr=loki-${REGION}:3100
    - -labelname=region
    - -labelvalue=${REGION}
```

### Spot Check (장기 데이터 검증)

`-spot-check-*` 옵션으로 과거 데이터에 대한 주기적 검증. 보존 정책이나 Compactor 문제로 인한 손실 감지.

### Metric Test (메트릭 쿼리 검증)

`-metric-test-*` 옵션으로 LogQL 메트릭 쿼리(`count_over_time` 등)가 정상 동작하는지 검증.
