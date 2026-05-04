# Mimir 프로덕션 운영

> 이 문서는 Grafana Mimir 공식 문서의 "Run in production" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/manage/run-production-environment/

---

## 목차

1. [프로덕션 체크리스트](#프로덕션-체크리스트)
2. [용량 계획](#용량-계획)
3. [고가용성 (HA)](#고가용성-ha)
4. [모니터링과 알림](#모니터링과-알림)
5. [성능 튜닝](#성능-튜닝)
6. [용량 한도 설정](#용량-한도-설정)
7. [업그레이드 전략](#업그레이드-전략)
8. [장애 복구](#장애-복구)
9. [백업 전략](#백업-전략)
10. [SLO 정의 및 추적](#slo-정의-및-추적)

---

## 프로덕션 체크리스트

### 배포 전 체크

#### 인프라
- [ ] Microservices 모드 (또는 검증된 SSD)
- [ ] 3개 이상 Availability Zone에 분산
- [ ] 오브젝트 스토리지 (Cross-region replication)
- [ ] Kubernetes 1.25+ 또는 안정 환경
- [ ] 충분한 노드 리소스
- [ ] 네트워크 대역폭

#### 컴포넌트
- [ ] Distributor: 3+ replicas
- [ ] Ingester: 3+ replicas, Replication Factor 3
- [ ] Querier: 3+ replicas
- [ ] Query Frontend: 2+ replicas
- [ ] Query Scheduler: 2+ replicas
- [ ] Store Gateway: 3+ replicas, Replication Factor 3
- [ ] Compactor: 1+ instances
- [ ] Ruler: 2+ replicas
- [ ] Alertmanager: 3+ replicas
- [ ] Memcached: 모든 캐시 활성화

#### 보안
- [ ] TLS 활성화
- [ ] 컴포넌트 간 mTLS
- [ ] 인증 프록시
- [ ] 멀티 테넌시 활성화
- [ ] 시크릿 관리 (IAM/MSI)
- [ ] Network Policy
- [ ] 감사 로깅

#### 한도/구성
- [ ] 테넌트별 한도 설정
- [ ] runtime_config로 동적 조정 가능
- [ ] 글로벌 안전 한도 (instance_limits)
- [ ] WAL 설정
- [ ] 적절한 보존 기간

#### 운영
- [ ] 모니터링 설정 (Mixin)
- [ ] 알림 룰 배포
- [ ] Continuous Test 실행
- [ ] Runbook 준비
- [ ] 백업 설정
- [ ] DR 절차 문서화

---

## 용량 계획

### 시계열 기반 사이징

| 활성 시계열 | 컴포넌트 | 권장 사양 |
|------------|---------|----------|
| 1M | Ingester ×3 | 16GB RAM, 4 vCPU each |
| 10M | Ingester ×6 | 32GB RAM, 8 vCPU each |
| 100M | Ingester ×30 | 32GB RAM, 8 vCPU each |
| 1B | Ingester ×100 | 64GB RAM, 16 vCPU each |

### Ingester 메모리 추정

```
RAM = 활성 시계열 × 8KB × 1.5 (오버헤드)

10M 시계열 → 약 120GB 분산 (3개 RF, 30개 인스턴스 → 12GB/인스턴스)
```

### 오브젝트 스토리지

```
일일 데이터 = 활성 시계열 × 샘플/일 × ~2 bytes (압축 후)

10M × 8640 (10초 간격) × 2 bytes = ~170GB/일
30일 보존: 5.1TB
1년 보존: 62TB
```

### Store Gateway

```
RAM = 블록 인덱스 헤더 크기 합계
    ≈ 일일 데이터 × 보존 일수 × 0.001
```

### Querier

```
인스턴스 수 = 동시 쿼리 / max_concurrent
```

### 캐시 사이징

| 캐시 | 권장 RAM |
|------|---------|
| Results | 1-4GB |
| Chunks | 활성 데이터의 50% |
| Metadata | 1-4GB |
| Index | 16-64GB |

---

## 고가용성 (HA)

### Availability Zone 분산

```yaml
# 모든 stateful 컴포넌트
ingester:
  zoneAwareReplication:
    enabled: true
    zones: [zone-a, zone-b, zone-c]

store_gateway:
  zoneAwareReplication:
    enabled: true
    zones: [zone-a, zone-b, zone-c]
```

### Replication Factor

```yaml
ingester:
  ring:
    replication_factor: 3

store_gateway:
  sharding_ring:
    replication_factor: 3
```

### Prometheus HA

[09_configure_advanced.md](./09_configure_advanced.md#ha-중복-제거-ha-tracker) 참조.

### Anti-affinity

```yaml
ingester:
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/component
                operator: In
                values: [ingester]
          topologyKey: kubernetes.io/hostname
```

같은 노드에 여러 Ingester 배치 방지.

### PodDisruptionBudget

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: mimir-ingester
spec:
  maxUnavailable: 1   # 한 번에 최대 1개만 다운
  selector:
    matchLabels:
      app.kubernetes.io/component: ingester
```

---

## 모니터링과 알림

### Mimir Mixin 배포

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json

# Recording rules
jsonnet -J vendor recording-rules.libsonnet > rules.yaml

# Alerting rules
jsonnet -J vendor alerts.libsonnet > alerts.yaml
```

### 핵심 SLI

```promql
# Write 가용성
1 - (
  sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[5m]))
  /
  sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[5m]))
)

# Read 가용성
1 - (
  sum(rate(cortex_request_duration_seconds_count{route=~"/prometheus/api/v1/.*", status_code=~"5.."}[5m]))
  /
  sum(rate(cortex_request_duration_seconds_count{route=~"/prometheus/api/v1/.*"}[5m]))
)

# Write Latency P99
histogram_quantile(0.99,
  sum by (le) (rate(cortex_request_duration_seconds_bucket{route="/api/v1/push"}[5m]))
)

# Query Latency P99
histogram_quantile(0.99,
  sum by (le) (rate(cortex_request_duration_seconds_bucket{route=~"/prometheus/api/v1/query.*"}[5m]))
)
```

### 핵심 알림

```yaml
groups:
  - name: mimir_critical
    rules:
      - alert: MimirIngesterUnhealthy
        expr: |
          min by (cluster, namespace) (
            up{job=~".*/ingester"}
          ) == 0
        for: 5m
        labels:
          severity: critical
      
      - alert: MimirRequestErrors
        expr: |
          sum by (cluster, namespace, route) (
            rate(cortex_request_duration_seconds_count{status_code=~"5..", route!~"ready|debug.*|metrics"}[5m])
          )
          /
          sum by (cluster, namespace, route) (
            rate(cortex_request_duration_seconds_count[5m])
          )
          > 0.01
        for: 15m
        labels:
          severity: critical
      
      - alert: MimirIngesterReachingSeriesLimit
        expr: |
          (
            cortex_ingester_memory_series
            /
            ignoring(limit) cortex_ingester_instance_limits{limit="max_series"}
          ) > 0.8
        for: 3h
        labels:
          severity: warning
      
      - alert: MimirCompactorHasNotSuccessfullyRunCompaction
        expr: |
          time() - cortex_compactor_last_successful_run_timestamp_seconds > 24 * 60 * 60
        labels:
          severity: critical
      
      - alert: MimirRulerTooManyFailedQueries
        expr: |
          sum by (cluster, namespace) (
            rate(cortex_ruler_queries_failed_total[5m])
          ) > 0.1
        for: 5m
        labels:
          severity: critical
```

### 자체 메트릭

```alloy
prometheus.scrape "mimir_self" {
  targets    = discovery.kubernetes.mimir_pods.targets
  forward_to = [prometheus.remote_write.self.receiver]
}

prometheus.remote_write "self" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "mimir-self",
    }
  }
}
```

---

## 성능 튜닝

### Query Sharding

```yaml
query_frontend:
  query_sharding_enabled: true
  query_sharding_total_shards: 16
  query_sharding_max_sharded_queries: 128
```

### Query Splitting

```yaml
query_frontend:
  split_queries_by_interval: 24h
  align_queries_with_step: true
```

### Memcached 모든 캐시

```yaml
memcached:
  enabled: true

chunks-cache:
  enabled: true
  replicas: 6
  resources:
    requests:
      cpu: 500m
      memory: 16Gi

metadata-cache:
  enabled: true
  replicas: 3

results-cache:
  enabled: true
  replicas: 3

index-cache:
  enabled: true
  replicas: 6
  resources:
    requests:
      memory: 32Gi
```

### Ingester 튜닝

```yaml
ingester:
  ring:
    instance_limits:
      max_ingestion_rate: 0
      max_series: 1500000
      max_inflight_push_requests: 30000
  
  blocks_storage:
    tsdb:
      head_compaction_interval: 1m
      head_compaction_concurrency: 5
      stripe_size: 16384
```

### Distributor 튜닝

```yaml
distributor:
  pool:
    health_check_ingesters: true
  
  remote_timeout: 10s
  
  # 분산 처리 동시성
  ingestion_burst_factor: 0
```

### Store Gateway 튜닝

```yaml
store_gateway:
  bucket_store:
    sync_interval: 5m
    max_concurrent: 50
    
    # 인덱스 헤더 lazy loading
    index_header_lazy_loading_enabled: true
    index_header_lazy_loading_idle_timeout: 1h
    
    # Streaming series
    series_streaming_enabled: true
    streaming_series_batch_size: 5000
```

---

## 용량 한도 설정

### 단계적 한도

```yaml
limits:
  # Soft limit (모니터링용)
  ingestion_rate: 25000
  
  # Hard limit (instance level)
ingester:
  ring:
    instance_limits:
      max_series: 1500000
      max_ingestion_rate: 80000     # 인스턴스 한도
```

### 테넌트별 차등화

```yaml
# runtime.yaml
overrides:
  enterprise-tenant:
    ingestion_rate: 200000
    ingestion_burst_size: 1000000
    max_global_series_per_user: 10000000
    max_query_lookback: 0
    compactor_blocks_retention_period: 17520h    # 2년
  
  free-tenant:
    ingestion_rate: 1000
    max_global_series_per_user: 50000
    compactor_blocks_retention_period: 168h      # 7일
```

### Cardinality 보호

```yaml
limits:
  max_global_series_per_metric: 50000
  max_label_names_per_series: 30
  max_label_name_length: 1024
  max_label_value_length: 2048
```

---

## 업그레이드 전략

### 무중단 업그레이드 순서

1. **Compactor** (단일 인스턴스, 다운타임 OK)
2. **Store Gateway**
3. **Query Frontend / Scheduler**
4. **Querier**
5. **Distributor**
6. **Ingester** (가장 신중)
7. **Ruler**
8. **Alertmanager**

### Ingester 안전 업그레이드

```yaml
ingester:
  ring:
    final_sleep: 30s
  
  blocks_storage:
    tsdb:
      ship_interval: 1m       # 자주 업로드
      flush_blocks_on_shutdown: true
```

PodDisruptionBudget으로 동시 다운 제한:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: mimir-ingester
spec:
  maxUnavailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/component: ingester
```

### Zone-aware 업그레이드

각 Zone을 순차적으로 업그레이드:

```bash
# Zone A 업그레이드
helm upgrade mimir grafana/mimir-distributed \
  --set ingester.zoneAwareReplication.zones[0].image.tag=2.10.0

# 안정화 대기 (수 분)

# Zone B
helm upgrade ... zones[1].image.tag=2.10.0

# Zone C
helm upgrade ... zones[2].image.tag=2.10.0
```

### 카나리 배포

```yaml
# 1개 인스턴스만 새 버전
ingester:
  canary:
    enabled: true
    image:
      tag: 2.11.0-rc1
```

검증 후 전체 롤아웃.

### Breaking Changes

릴리스 노트의 [Breaking Changes](https://grafana.com/docs/mimir/latest/release-notes/) 필독.

---

## 장애 복구

### Ingester 장애

#### 단일 인스턴스
- WAL 활성화로 자동 복구
- Replication factor 3이면 다른 ingester가 처리

#### 다수 (한 Zone 전체)
- 다른 Zone의 복제본이 응답
- 복구 자동

#### 모든 Ingester 동시 다운
- 최근 데이터 일부 손실 (메모리 + WAL 손실)
- 가능한 한 한 Zone씩 복구

### Object Storage 장애

- Ingester는 메모리 + WAL 보유 (재시도)
- Querier는 캐시된 데이터 응답
- 복구 후 Ingester 자동 업로드

### KV Store 장애 (Memberlist)

- Memberlist는 가십 기반, 자가 치유
- Consul/etcd 사용 시 KV 클러스터 복구 우선

### Hash Ring Inconsistency

```bash
# Ring 강제 리셋 (마지막 수단)
curl -X POST http://mimir-ingester:9009/ingester/ring/forget?id=<unhealthy-instance>
```

---

## 백업 전략

### Object Storage

가장 중요한 백업.

#### S3 Cross-Region Replication

```json
{
  "Role": "arn:aws:iam::123:role/replication-role",
  "Rules": [{
    "Status": "Enabled",
    "Priority": 1,
    "Filter": {},
    "Destination": {
      "Bucket": "arn:aws:s3:::mimir-backup-us-west-2"
    },
    "DeleteMarkerReplication": {"Status": "Enabled"}
  }]
}
```

#### Versioning

```bash
aws s3api put-bucket-versioning \
  --bucket my-mimir \
  --versioning-configuration Status=Enabled
```

### 구성 백업

GitOps:
- Helm values
- runtime config
- 룰 파일
- Alertmanager 설정

### Disaster Recovery

#### 활성/대기 (Active/Passive)

- 메인 리전: 활성 클러스터
- 백업 리전: 대기 클러스터 (데이터 받지 않음)
- 장애 시: 대기 클러스터에 백업 버킷 마운트, DNS 변경

#### 활성/활성 (Active/Active)

- 두 리전 모두 활성
- Prometheus가 양쪽으로 push
- Querier가 두 리전 페더레이션

복잡하지만 가용성 최고.

---

## SLO 정의 및 추적

### 권장 SLO

| SLI | 목표 |
|-----|------|
| Write Availability | 99.9% (월 약 43분 다운 허용) |
| Read Availability | 99.5% |
| Write Latency P99 | < 1s |
| Query Latency P99 | < 30s |
| Data Freshness | < 1 minute |

### Burn Rate 알림

빠른 burn rate (1시간 윈도우):

```yaml
- alert: MimirWriteSLOBurnRateHigh
  expr: |
    (
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[1h]))
      /
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[1h]))
    ) > (1 - 0.999) * 14.4
  for: 2m
  labels:
    severity: critical
```

느린 burn rate (6시간 윈도우):

```yaml
- alert: MimirWriteSLOBurnRateMedium
  expr: |
    (
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[6h]))
      /
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[6h]))
    ) > (1 - 0.999) * 6
  for: 15m
  labels:
    severity: warning
```

### Error Budget 추적

```promql
# 30일 error budget 잔여
1 - (
  (
    sum(increase(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[30d]))
    /
    sum(increase(cortex_request_duration_seconds_count{route="/api/v1/push"}[30d]))
  )
  /
  (1 - 0.999)
)
```

소진 추세 대시보드로 시각화 → 변경 결정 지원.
