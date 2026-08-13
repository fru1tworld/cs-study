# Loki Canary, 고급 운영, 모범 사례

## Loki Canary

> 원본: https://grafana.com/docs/loki/latest/operations/loki-canary/

---

### 목차

1. [개요](#개요)
2. [동작 원리](#동작-원리)
3. [설치 및 실행](#설치-및-실행)
4. [구성 옵션](#구성-옵션)
5. [노출 메트릭](#노출-메트릭)
6. [알림 룰 예시](#알림-룰-예시)
7. [운영 시 활용](#운영-시-활용)

---

### 개요

**Loki Canary**는 Loki 클러스터의 가용성과 데이터 무결성을 지속적으로 검증하는 도구.

#### 목적

- 로그 손실 감지
- 수집 지연 측정
- 쿼리 가용성 검증
- 종단간(end-to-end) SLO 관측

#### 위치

Loki 저장소의 [`cmd/loki-canary`](https://github.com/grafana/loki/tree/main/cmd/loki-canary)에 포함 → 단독 바이너리 또는 컨테이너로 배포.

---

### 동작 원리

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

#### 검증 항목

- 생성된 라인 수: `loki_canary_entries_total`
- 누락된 라인 수: `loki_canary_missing_entries_total`
- 중복된 라인 수: `loki_canary_duplicate_entries_total`
- 쿼리 실패 횟수: `loki_canary_unexpected_entries_total`
- 종단간 지연: `loki_canary_response_latency`

---

### 설치 및 실행

#### Docker

```bash
docker run -d \
  --name loki-canary \
  grafana/loki-canary:latest \
  -addr=loki:3100 \
  -labelname=instance \
  -labelvalue=canary-1
```

#### Kubernetes (DaemonSet)

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

#### Helm 서브차트

`grafana/loki` Helm 차트에 포함됨.

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

### 구성 옵션

#### 핵심 플래그

- `-addr` (기본값 `localhost:3100`): Loki 주소
- `-labelname` (기본값 `name`): 라벨 이름
- `-labelvalue` (기본값 `loki-canary`): 라벨 값
- `-tls` (기본값 false): TLS 사용
- `-cert-file` (기본값 ""): 클라이언트 인증서
- `-key-file` (기본값 ""): 클라이언트 키
- `-ca-file` (기본값 ""): CA 인증서
- `-user` (기본값 ""): Basic Auth 사용자
- `-pass` (기본값 ""): Basic Auth 비밀번호
- `-tenant-id` (기본값 ""): X-Scope-OrgID

#### 생성 옵션

- `-interval` (기본값 `1s`): 라인 생성 간격
- `-size` (기본값 `100`): 라인 크기 (bytes)

#### 검증 옵션

- `-wait` (기본값 `60s`): 라인 생성 후 쿼리 대기 시간
- `-max-wait` (기본값 `5m`): 최대 대기 시간
- `-pruneinterval` (기본값 `1m`): 메모리 정리 주기
- `-buckets` (기본값 `10`): 히스토그램 버킷 수
- `-spot-check-interval` (기본값 `15m`): 스팟 체크 간격
- `-spot-check-max` (기본값 `4h`): 스팟 체크 최대 범위
- `-spot-check-query-rate` (기본값 `1m`): 스팟 체크 쿼리 빈도
- `-spot-check-initial-wait` (기본값 `10s`): 첫 스팟 체크 대기
- `-metric-test-interval` (기본값 `1h`): 메트릭 테스트 간격
- `-metric-test-range` (기본값 `24h`): 메트릭 테스트 범위
- `-query-timeout` (기본값 `10s`): 쿼리 타임아웃

#### 메트릭 노출

- `-port` (기본값 `3500`): 메트릭 포트
- `-out-of-order-min` (기본값 `30s`): 순서 어긋남 최소 되감기 시간
- `-out-of-order-max` (기본값 `1m`): 순서 어긋남 최대 되감기 시간
- `-out-of-order-percentage` (기본값 `10`): 순서 어긋남 비율

---

### 노출 메트릭

#### 핵심 메트릭

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
loki_canary_metric_test_deviation
loki_canary_metric_test_request_total
loki_canary_metric_test_error_total
```

---

### 알림 룰 예시

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

### 운영 시 활용

#### SLO 측정

종단간 가용성 SLO:

```promql
# 가용성 (라인 손실률)
1 - (
  sum(rate(loki_canary_missing_entries_total[5m]))
  /
  sum(rate(loki_canary_entries_total[5m]))
)
```

#### 지연 SLO

```promql
# P99 지연
histogram_quantile(0.99,
  sum by (le) (rate(loki_canary_response_latency_seconds_bucket[5m]))
)
```

#### 위치별 카나리

여러 클러스터/리전에 배포하여 위치별 가용성 비교:

```yaml
- name: ${REGION}-canary
  args:
    - -addr=loki-${REGION}:3100
    - -labelname=region
    - -labelvalue=${REGION}
```

#### Spot Check (장기 데이터 검증)

`-spot-check-*` 옵션으로 과거 데이터를 주기적으로 검증함 → 보존 정책이나 Compactor 문제로 인한 손실 감지 가능.

#### Metric Test (메트릭 쿼리 검증)

`-metric-test-*` 옵션으로 LogQL 메트릭 쿼리(`count_over_time` 등)의 정상 동작 여부 검증.

---

## Loki 고급 운영

> 원본: https://grafana.com/docs/loki/latest/operations/

---

### 목차

1. [Bloom 필터 관리](#bloom-필터-관리)
2. [Shuffle Sharding](#shuffle-sharding)
3. [자동 스트림 샤딩](#자동-스트림-샤딩)
4. [Zone-aware Ingester 롤아웃](#zone-aware-ingester-롤아웃)
5. [Querier Autoscaling](#querier-autoscaling)
6. [Query Fairness](#query-fairness)
7. [Query Blocking](#query-blocking)
8. [Tenant Isolation](#tenant-isolation)
9. [Authentication](#authentication)
10. [Overrides Exporter](#overrides-exporter)

---

### Bloom 필터 관리

#### 개요

Loki의 **Bloom 필터**는 라벨 인덱스로 좁혀지지 않는 텍스트 검색의 성능을 향상시키는 실험적 기능.

#### 동작

1. **Bloom Builder**: 청크 데이터에서 n-gram을 추출하여 Bloom 필터 블록 생성
2. **Bloom Gateway**: Querier로부터 청크 필터링 요청을 받아 Bloom 필터로 미리 거름
3. **Bloom Planner**: 어떤 청크에 대한 Bloom을 만들지 계획

#### 활성화

```yaml
bloom_build:
  enabled: true
  planner:
    planning_interval: 6h
    min_table_offset: 1
  builder:
    planner_address: bloom-planner:9095

bloom_gateway:
  enabled: true
  client:
    cache_results: true
    addresses: dns+bloom-gateway.loki.svc.cluster.local:9095

limits_config:
  bloom_create_enabled: true
  bloom_split_series_keyspace_by: 256
  bloom_gateway_enable_filtering: true
```

#### 효과

- 텍스트 라인 필터(`|=`, `|~`)가 자주 쓰이는 환경에서 청크 스캔량 대폭 감소
- 단, 추가 스토리지(약 5-15%)와 컴퓨팅 비용 발생

#### 모니터링

- `loki_bloom_builder_chunks_processed_total`: 처리된 청크 수
- `loki_bloom_gateway_chunks_filtered_total`: Bloom으로 걸러진 청크 수
- `loki_bloom_gateway_filter_ratio`: 필터 효율

---

### Shuffle Sharding

#### 개념

테넌트의 데이터/쿼리를 모든 인스턴스가 아닌 **일부 인스턴스에만** 분배 → 한 테넌트의 영향이 다른 테넌트로 전파되는 것을 방지.

#### 활성화

##### Ingester

```yaml
limits_config:
  ingestion_partitions_tenant_shard_size: 4
```

각 테넌트가 4개의 Ingester만 사용.

##### Querier

```yaml
limits_config:
  per_tenant_query_concurrency: 16
  max_queriers_per_tenant: 4
```

##### Ruler

```yaml
limits_config:
  ruler_tenant_shard_size: 3
```

#### 장점

- **노이지 네이버(Noisy Neighbor) 방지**: 한 테넌트의 무거운 워크로드가 격리됨
- **메모리 효율**: 모든 인스턴스에 데이터 분산하지 않음
- **장애 격리**: 영향 범위 제한

#### 권장 샤드 크기

```
테넌트 워크로드 크기에 따라:
- 소규모 테넌트: 2-3
- 중규모: 4-6
- 대규모: 8-16
- 매우 큰 단일 테넌트: 모든 인스턴스 사용 (기본값)
```

---

### 자동 스트림 샤딩

#### 문제

단일 스트림이 너무 많은 데이터를 수신하면 특정 Ingester에 부하 집중 → Rate Limit 초과나 메모리 압박으로 이어질 수 있음.

#### 해결: 자동 샤딩

특정 라벨 조합의 트래픽이 임계치를 넘으면 자동으로 추가 라벨(`__stream_shard__`)을 생성하여 여러 Ingester로 분산.

#### 활성화

```yaml
limits_config:
  shard_streams:
    enabled: true
    desired_rate: 3MB    # 스트림당 목표 비율
    logging_enabled: false
```

#### 동작

```
스트림 A (10MB/s) 수신
    ↓
desired_rate(3MB) 초과 감지
    ↓
자동으로 4개 샤드로 분할
    ↓
A{__stream_shard__="0"}, A{__stream_shard__="1"}, A{__stream_shard__="2"}, A{__stream_shard__="3"}
    ↓
각각 다른 Ingester로 분산
```

#### 쿼리 시

쿼리 시점에는 샤드가 자동으로 합쳐지므로 사용자가 신경 쓸 필요 없음.

#### 트레이드오프

- 활성 시계열 수가 증가
- 카디널리티 증가
- 단, 단일 스트림 부하 분산 효과는 큼

---

### Zone-aware Ingester 롤아웃

#### 문제

Ingester는 stateful이며 Replication Factor 3으로 운영 → 한 번에 너무 많이 재시작하면 데이터 손실 위험.

#### 해결: Zone Awareness

Ingester를 여러 Zone으로 묶고, 한 Zone씩 순차적으로 롤아웃.

#### 활성화

```yaml
ingester:
  lifecycler:
    ring:
      replication_factor: 3
      zone_awareness_enabled: true
      instance_availability_zone: zone-a   # zone-a, zone-b, zone-c 중
```

#### Helm 차트 설정

```yaml
ingester:
  zoneAwareReplication:
    enabled: true
    zones:
      - name: zone-a
        nodeSelector:
          topology.kubernetes.io/zone: us-east-1a
      - name: zone-b
        nodeSelector:
          topology.kubernetes.io/zone: us-east-1b
      - name: zone-c
        nodeSelector:
          topology.kubernetes.io/zone: us-east-1c
```

#### 롤아웃 절차

```
zone-a 모두 업그레이드 → 안정화 대기 → zone-b → 안정화 → zone-c
```

각 Zone 내에서는 동시에 여러 인스턴스 재시작 가능 (다른 Zone에 복제본 있음).

#### 효과

- 롤아웃 속도 향상
- 데이터 손실 위험 최소화
- 자연스러운 가용 영역 분산

---

### Querier Autoscaling

#### KEDA 기반 자동 스케일링

Querier는 stateless → 부하에 따라 자동으로 확장/축소 가능.

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: loki-querier
spec:
  scaleTargetRef:
    name: querier
  minReplicaCount: 2
  maxReplicaCount: 50
  pollingInterval: 30
  cooldownPeriod: 300
  
  triggers:
    # Inflight 쿼리 수 기반
    - type: prometheus
      metadata:
        serverAddress: http://prometheus:9090
        metricName: querier_inflight_queries
        threshold: "10"
        query: |
          sum(loki_query_scheduler_inflight_requests{job="loki-querier"})
    
    # 쿼리 큐 길이 기반
    - type: prometheus
      metadata:
        serverAddress: http://prometheus:9090
        metricName: query_scheduler_queue
        threshold: "50"
        query: |
          sum(cortex_query_scheduler_queue_length)
```

#### Helm 통합

```yaml
querier:
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 20
    targetCPUUtilizationPercentage: 80
```

#### 권장 메트릭

- `loki_query_scheduler_inflight_requests` (현재 처리 중)
- `cortex_query_scheduler_queue_length` (큐 대기)
- CPU/메모리 사용률

---

### Query Fairness

#### 문제

큰 쿼리가 작은 쿼리를 차단(Head-of-Line Blocking).

#### 해결: Query Scheduler

테넌트별 큐 분리로 공정성 확보.

```yaml
query_scheduler:
  use_scheduler_ring: true
  max_outstanding_requests_per_tenant: 100
  
  # 우선순위 큐
  querier_forget_delay: 0s
  scheduler_ring:
    kvstore:
      store: memberlist
```

#### 동작

- 각 테넌트마다 독립된 큐
- Querier가 라운드 로빈으로 각 테넌트 큐에서 작업 가져감
- 한 테넌트의 큰 쿼리가 다른 테넌트를 차단하지 않음

#### 추가: Sub-Query Splitting

큰 쿼리는 작은 쿼리로 분할되어 각각 큐에 들어감 → 작은 쿼리도 빠르게 처리 가능.

---

### Query Blocking

#### 목적

특정 패턴의 쿼리(주로 비용/성능 문제 있는 쿼리) 차단.

#### 설정

```yaml
limits_config:
  blocked_queries:
    - pattern: '.*{namespace=~".+"}.*'
      regex: true
      types: ["filter", "limited", "metric"]
      hash: 0  # 또는 특정 쿼리의 해시
    
    - pattern: '{job="bad-job"}'
      types: ["metric"]
```

#### 옵션

- `pattern`: 쿼리 매칭 패턴
- `regex`: true = 정규식
- `types`: 차단할 쿼리 종류 (filter, limited, metric)
- `hash`: 특정 쿼리 해시 직접 차단

#### 활용

- 사용자 실수 방지 (예: `{} |= ""`)
- 너무 비싼 쿼리 차단 (예: 라벨 매처 없이 정규식)
- 알려진 문제 쿼리 블록

---

### Tenant Isolation

#### 격리 수준

- 데이터: 디렉토리/접두사 분리
- 메모리: 테넌트별 청크 분리
- 쿼리 큐: 테넌트별 큐
- Rate: 테넌트별 한도
- 인스턴스: Shuffle Sharding

#### 강한 격리: 별도 클러스터

매우 중요한 테넌트는 별도 Loki 클러스터로 분리하는 것이 안전.

#### Multi-tenant Federation

여러 테넌트를 한 번에 쿼리하려면:

```yaml
limits_config:
  multi_tenant_queries_enabled: true
```

쿼리 시:

```http
X-Scope-OrgID: tenant-a|tenant-b|tenant-c
```

---

### Authentication

#### 기본: 인증 없음

Loki는 자체 인증을 제공하지 않음 → 운영 환경에서는 별도 추가 필요.

#### 옵션 1: 리버스 프록시

##### Nginx Basic Auth

```nginx
upstream loki {
  server loki-1:3100;
  server loki-2:3100;
}

server {
  listen 443 ssl;
  server_name loki.example.com;
  
  ssl_certificate /etc/ssl/loki.crt;
  ssl_certificate_key /etc/ssl/loki.key;
  
  location / {
    auth_basic "Loki";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    # 테넌트 헤더 강제
    proxy_set_header X-Scope-OrgID "$remote_user";
    
    proxy_pass http://loki;
  }
}
```

##### oauth2-proxy

```yaml
- name: oauth2-proxy
  args:
    - --provider=oidc
    - --upstream=http://loki:3100
    - --pass-access-token=true
    - --pass-user-headers=true
    - --set-xauthrequest=true
```

#### 옵션 2: Service Mesh

Istio, Linkerd 등으로 mTLS 강제.

#### 옵션 3: API Gateway

Kong, AWS API Gateway 등으로 API 키 검증.

#### 멀티 테넌시 강제

```yaml
auth_enabled: true
```

활성화 시 모든 요청에 `X-Scope-OrgID` 필수. 인증 프록시가 테넌트별 헤더를 주입하도록 설정.

---

### Overrides Exporter

#### 목적

테넌트별 한도(limit) 설정을 Prometheus 메트릭으로 노출.

#### 활성화

```yaml
target: overrides-exporter   # 또는 모놀리식 모드 일부
```

#### 노출 메트릭

```
loki_overrides_defaults{limit_name="ingestion_rate_mb"} 4
loki_overrides{tenant="tenant-a", limit_name="ingestion_rate_mb"} 100
loki_overrides{tenant="tenant-b", limit_name="ingestion_rate_mb"} 5
```

#### 활용

- 한도 변경 추적
- 한도 vs 실제 사용량 비교 알림
- 테넌트 사용 현황 대시보드

#### 사용량 vs 한도 비교 알림

```yaml
- alert: TenantNearIngestionLimit
  expr: |
    (
      sum by (tenant) (rate(loki_distributor_bytes_received_total[5m]))
      / 1e6
    )
    /
    sum by (tenant) (loki_overrides{limit_name="ingestion_rate_mb"})
    > 0.8
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "{{ $labels.tenant }} 수집 한도의 80% 도달"
```

---

## Loki 모범 사례 (Best Practices)

> 원본: https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/

---

### 목차

1. [라벨 모범 사례](#라벨-모범-사례)
2. [카디널리티 관리](#카디널리티-관리)
3. [청크와 인덱스 튜닝](#청크와-인덱스-튜닝)
4. [쿼리 최적화](#쿼리-최적화)
5. [수집 최적화](#수집-최적화)
6. [스토리지 모범 사례](#스토리지-모범-사례)
7. [멀티 테넌시 모범 사례](#멀티-테넌시-모범-사례)
8. [업그레이드 모범 사례](#업그레이드-모범-사례)
9. [보안 모범 사례](#보안-모범-사례)
10. [모니터링 모범 사례](#모니터링-모범-사례)

---

### 라벨 모범 사례

#### 좋은 라벨 후보

- **소스 식별자**: `cluster`, `region`, `namespace`, `app`, `job`
- **환경**: `environment` (prod/staging/dev)
- **호스트/Pod 식별자** (제한적): `host`, `instance`
- **로그 종류**: `log_type` (access/error/audit)

#### 나쁜 라벨 후보

- 사용자 ID, 요청 ID, 트레이스 ID (높은 카디널리티)
- HTTP path/URL (무한 카디널리티)
- IP 주소 (변동 큼)
- 타임스탬프
- 에러 메시지 본문

#### 라벨 수 가이드

```
권장: 10-15개 이내
경고: 20개 초과 시 검토
위험: 30개 초과 시 카디널리티 폭증
```

#### 정적 vs 동적 라벨

**정적 라벨 (수집 시 고정)**
```alloy
loki.process "labels" {
  stage.static_labels {
    values = {
      cluster = "prod-us-east-1",
      env     = "production",
    }
  }
}
```

**동적 라벨 (로그 내용에서 추출)**
```alloy
loki.process "extract" {
  stage.json {
    expressions = {
      level = "level",
    }
  }
  stage.labels {
    values = {
      level = "",   // level 값을 라벨로
    }
  }
}
```

동적 라벨은 카디널리티가 예측 가능한 경우에만 사용.

---

### 카디널리티 관리

#### 카디널리티 폭증 신호

- Ingester 메모리 급증
- 활성 스트림 수 급증 (`loki_ingester_memory_streams`)
- 쿼리 속도 급격히 저하

#### 분석 도구

```bash
# logcli로 라벨 목록 조회
logcli labels --addr=http://loki:3100 \
  --from="2024-01-01T00:00:00Z" \
  --to="2024-01-02T00:00:00Z"

# 특정 라벨의 값 분포
logcli labels level --addr=http://loki:3100

# 라벨별 카디널리티 분석 (Loki 1.6.0+)
logcli series --analyze-labels --addr=http://loki:3100 \
  --match='{namespace="prod"}'
```

#### Series API

```bash
curl -G "http://loki:3100/loki/api/v1/series" \
  --data-urlencode 'match[]={namespace="prod"}' \
  | jq '.data | length'
```

#### 카디널리티 한도

```yaml
limits_config:
  max_label_name_length: 1024
  max_label_value_length: 4096
  max_label_names_per_series: 30
  cardinality_limit: 100000
  max_streams_per_user: 10000
  max_global_streams_per_user: 5000
```

#### 카디널리티 줄이기

##### 클라이언트 측

```alloy
loki.process "reduce_cardinality" {
  // 라벨 값 정규화
  stage.regex {
    expression = "(?P<path>/api/users/)(\\d+)"
  }
  stage.labels {
    values = {
      path = "",   // /api/users/{id}로 변환된 path
    }
  }
}
```

##### 서버 측 (구조화된 메타데이터로 이전)

카디널리티가 높은 정보는 라벨 대신 **structured metadata**로 이전.

```alloy
loki.process "metadata" {
  stage.structured_metadata {
    values = {
      trace_id = "trace_id",
      user_id  = "user_id",
    }
  }
}
```

라벨 인덱스에 영향을 주지 않으면서 쿼리에서 필터링·추출 가능.

---

### 청크와 인덱스 튜닝

#### 청크 크기 최적화

```yaml
ingester:
  chunk_target_size: 1572864     # 1.5MB (압축 후) - 권장
  chunk_idle_period: 30m         # idle 청크 빨리 플러시 (메모리 절약)
  max_chunk_age: 2h              # 청크 최대 수명
  chunk_block_size: 262144       # 256KB
  chunk_retain_period: 1m        # 플러시 후 메모리 보유
```

#### 너무 작은 청크의 문제

- 오브젝트 스토리지 요청 수 증가 (S3 비용)
- 인덱스 크기 증가
- 쿼리 시 더 많은 fetch 필요

#### 너무 큰 청크의 문제

- Ingester 메모리 압박
- 특정 시간 범위 쿼리 시 불필요한 데이터 다운로드

#### TSDB 인덱스 권장

```yaml
schema_config:
  configs:
    - from: 2024-06-01
      store: tsdb
      object_store: s3
      schema: v13         # 최신
      index:
        prefix: tsdb_index_
        period: 24h
```

---

### 쿼리 최적화

#### 효율적 쿼리 작성

##### 좋은 예 (허용)

```logql
# 스트림 선택자가 구체적
{namespace="prod", app="api"} |= "error"

# 라인 필터 먼저, 파서 나중
{namespace="prod"} |= "error" | json | level="error"

# 시간 범위 좁게
{namespace="prod"}[5m]
```

##### 나쁜 예 (금지)

```logql
# 너무 광범위한 선택자
{} |= "error"

# 파서 먼저, 필터 나중 (모든 라인 파싱)
{namespace="prod"} | json | level="error"

# 정규식만으로 모든 라인 매칭
{namespace="prod"} |~ ".*"
```

#### 쿼리 분할

```yaml
query_range:
  split_queries_by_interval: 30m
  parallelise_shardable_queries: true
```

#### 결과 캐싱

```yaml
query_range:
  cache_results: true
  results_cache:
    cache:
      memcached_client:
        addresses: dns+memcached:11211
```

#### 쿼리 시간 제한

```yaml
limits_config:
  max_query_length: 721h         # 30일
  max_query_lookback: 0          # 보존 기간까지
  query_timeout: 5m
  max_entries_limit_per_query: 5000
```

---

### 수집 최적화

#### 클라이언트 측 배칭

```alloy
loki.write "default" {
  endpoint {
    url        = "http://loki:3100/loki/api/v1/push"
    batch_size = "1MiB"     // 1MB 배치
    batch_wait = "1s"       // 또는 1초
  }
}
```

#### 압축

기본적으로 활성화되어 있으나 설정 확인 필요.

```alloy
loki.write "default" {
  endpoint {
    url     = "..."
    // gzip 자동 사용
  }
}
```

#### 라벨 정렬

라벨은 알파벳 순으로 정렬되어야 캐싱이 일관되게 동작 → 클라이언트가 자동으로 처리함.

#### 시간 정렬

같은 스트림의 로그는 시간 순서대로 전송 필요 → 순서가 어긋난 로그는 거부됨, 허용하려면 별도 설정 필요.

```yaml
limits_config:
  max_label_value_length: 4096
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  creation_grace_period: 10m
```

#### Structured Metadata 활용

라벨 카디널리티를 늘리지 않으면서 메타데이터 추가 가능.

```alloy
stage.structured_metadata {
  values = {
    trace_id = "trace_id",
    span_id  = "span_id",
  }
}
```

---

### 스토리지 모범 사례

#### S3 Lifecycle 정책

오래된 청크를 자동으로 저렴한 스토리지 클래스로 전환.

```json
{
  "Rules": [
    {
      "Id": "MoveToIA",
      "Status": "Enabled",
      "Filter": { "Prefix": "loki/chunks/" },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

> 주의: Glacier 등 저비용 스토리지는 쿼리 시 즉시 접근 불가능. 핫 데이터만 Standard에 두기.

#### 동일 리전 권장

오브젝트 스토리지와 Loki는 같은 리전에 배치 → 네트워크 비용과 지연 최소화.

#### Versioning + Replication

중요 데이터:
- S3 Versioning 활성화
- Cross-Region Replication

#### Compactor 설정

```yaml
compactor:
  retention_enabled: true
  compaction_interval: 10m
  retention_delete_delay: 2h
  retention_delete_worker_count: 150
  
  # 삭제 마킹된 데이터를 빠르게 정리
  shared_store: s3
```

---

### 멀티 테넌시 모범 사례

#### 테넌트 ID 명명

```
✅ 좋음: tenant-a, prod-team1, k8s-cluster-1
❌ 피하기: ../etc, "", tenant/with/slashes
```

#### 테넌트별 한도 차등화

```yaml
overrides:
  large-customer:
    ingestion_rate_mb: 100
    max_streams_per_user: 100000
    retention_period: 8760h
  
  small-customer:
    ingestion_rate_mb: 5
    max_streams_per_user: 5000
    retention_period: 720h
```

#### 전체 한도 보호

테넌트별 한도 외에도 전역 수준의 보호 설정 필요.

```yaml
ingester:
  instance_limits:
    max_ingestion_rate: 0      # 무제한
    max_streams_per_user: 0
    max_streams: 0
```

#### Shuffle Sharding 사용

```yaml
limits_config:
  ingestion_partitions_tenant_shard_size: 4
```

대형 테넌트가 소형 테넌트에 영향을 주지 않도록 격리.

---

### 업그레이드 모범 사례

#### 단계적 업그레이드

```
한 번에 한 마이너 버전씩
2.8 → 2.9 → 2.10 → 3.0
```

#### 백업

업그레이드 전:
- 구성 파일 백업
- 가능하면 schema_config 백업
- 오브젝트 스토리지 버전 관리

#### 카나리 배포

먼저 1~2개 인스턴스만 업그레이드하고 검증한 뒤 나머지 진행.

#### 롤백 계획

청크 형식이나 인덱스 형식이 변경된 경우 롤백 어려움 → 신중하게 진행 필요.

---

### 보안 모범 사례

#### TLS 강제

```yaml
server:
  http_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
    client_auth_type: RequireAndVerifyClientCert
    client_ca_file: /etc/certs/ca.crt
  
  grpc_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
```

#### 컴포넌트 간 mTLS

```yaml
ingester_client:
  grpc_client_config:
    tls_enabled: true
    tls_cert_path: /etc/certs/client.crt
    tls_key_path: /etc/certs/client.key
    tls_ca_path: /etc/certs/ca.crt
```

#### 비밀 관리

환경 변수나 외부 시크릿 관리자 사용:

```yaml
storage_config:
  aws:
    access_key_id: ${AWS_ACCESS_KEY_ID}
    secret_access_key: ${AWS_SECRET_ACCESS_KEY}
```

Kubernetes:

```yaml
env:
  - name: AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: loki-s3
        key: secret-key
```

#### 네트워크 정책

Kubernetes NetworkPolicy로 컴포넌트 간 통신만 허용.

#### Audit Logging

리버스 프록시에서 모든 요청 로깅:

```nginx
log_format loki_audit '$remote_addr - $remote_user - $time_iso8601 - '
                     '$request - $status - X-Scope-OrgID:$http_x_scope_orgid';
access_log /var/log/nginx/loki-audit.log loki_audit;
```

---

### 모니터링 모범 사례

#### 셀프 모니터링

Loki가 Loki를 모니터링:

```alloy
loki.source.kubernetes "loki" {
  targets = discovery.kubernetes.loki_pods.targets
  forward_to = [loki.write.self.receiver]
}

loki.write "self" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "loki-self",
    }
  }
}
```

#### 핵심 SLI 추적

- 수집 가용성: > 99.9%
- 쿼리 가용성: > 99.5%
- 수집 P99 지연: < 1s
- 쿼리 P99 지연: < 30s

#### Loki Mixin 사용

```bash
jb install github.com/grafana/loki/production/loki-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

대시보드, Recording Rules, Alerting Rules를 일괄 제공.

#### Loki Canary 배포

[11_loki_canary.md](./11_loki_canary.md) 참조. 종단간 검증 필수.

#### 용량 추세 추적

```promql
# 일일 수집량 추세
sum by (tenant) (rate(loki_distributor_bytes_received_total[1d])) * 86400

# 활성 스트림 추세
sum by (tenant) (loki_ingester_memory_streams)

# 한도 대비 사용률
(
  sum by (tenant) (rate(loki_distributor_bytes_received_total[5m]))
  / 1e6
)
/
on (tenant) sum by (tenant) (loki_overrides{limit_name="ingestion_rate_mb"})
```
