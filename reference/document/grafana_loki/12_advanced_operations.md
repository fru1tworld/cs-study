# Loki 고급 운영

> 이 문서는 Grafana Loki 공식 문서 Operations 섹션의 고급 주제들을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/operations/

---

## 목차

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

## Bloom 필터 관리

### 개요

Loki의 **Bloom 필터** 는 라벨 인덱스로 좁혀지지 않는 텍스트 검색의 성능을 향상시키는 실험적 기능입니다.

### 동작

1. **Bloom Builder**: 청크 데이터에서 n-gram을 추출하여 Bloom 필터 블록 생성
2. **Bloom Gateway**: Querier로부터 청크 필터링 요청을 받아 Bloom 필터로 미리 거름
3. **Bloom Planner**: 어떤 청크에 대한 Bloom을 만들지 계획

### 활성화

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

### 효과

- 텍스트 라인 필터(`|=`, `|~`)가 자주 쓰이는 환경에서 청크 스캔량 대폭 감소
- 단, 추가 스토리지(약 5-15%)와 컴퓨팅 비용 발생

### 모니터링

| 메트릭 | 설명 |
|--------|------|
| `loki_bloom_builder_chunks_processed_total` | 처리된 청크 수 |
| `loki_bloom_gateway_chunks_filtered_total` | Bloom으로 걸러진 청크 수 |
| `loki_bloom_gateway_filter_ratio` | 필터 효율 |

---

## Shuffle Sharding

### 개념

테넌트의 데이터/쿼리를 모든 인스턴스가 아니라 **일부 인스턴스에만** 분배. 한 테넌트의 영향이 다른 테넌트로 번지는 것을 막음.

### 활성화

#### Ingester

```yaml
limits_config:
  ingestion_partitions_tenant_shard_size: 4
```

각 테넌트가 4개의 Ingester만 사용.

#### Querier

```yaml
limits_config:
  per_tenant_query_concurrency: 16
  max_queriers_per_tenant: 4
```

#### Ruler

```yaml
limits_config:
  ruler_tenant_shard_size: 3
```

### 장점

- **노이지 네이버(Noisy Neighbor) 방지**: 한 테넌트의 무거운 워크로드가 격리됨
- **메모리 효율**: 모든 인스턴스에 데이터 분산하지 않음
- **장애 격리**: 영향 범위 제한

### 권장 샤드 크기

```
테넌트 워크로드 크기에 따라:
- 소규모 테넌트: 2-3
- 중규모: 4-6
- 대규모: 8-16
- 매우 큰 단일 테넌트: 모든 인스턴스 사용 (기본값)
```

---

## 자동 스트림 샤딩

### 문제

단일 스트림이 너무 많은 데이터를 받으면 한 Ingester에 부하 집중. Rate Limit 초과나 메모리 압박 발생.

### 해결: 자동 샤딩

특정 라벨 조합의 트래픽이 임계치를 넘으면 자동으로 추가 라벨(`__stream_shard__`)을 생성하여 여러 Ingester로 분산.

### 활성화

```yaml
limits_config:
  shard_streams:
    enabled: true
    desired_rate: 3MB    # 스트림당 목표 비율
    logging_enabled: false
```

### 동작

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

### 쿼리 시

쿼리 시점에는 샤드가 자동으로 합쳐지므로 사용자가 신경 쓸 필요 없음.

### 트레이드오프

- 활성 시계열 수가 증가
- 카디널리티 증가
- 단, 단일 스트림 부하 분산 효과는 큼

---

## Zone-aware Ingester 롤아웃

### 문제

Ingester는 stateful이고 Replication Factor 3으로 운영. 한 번에 너무 많이 재시작하면 데이터 손실 위험.

### 해결: Zone Awareness

Ingester를 여러 Zone으로 묶고, 한 Zone씩 순차적으로 롤아웃.

### 활성화

```yaml
ingester:
  lifecycler:
    ring:
      replication_factor: 3
      zone_awareness_enabled: true
      instance_availability_zone: zone-a   # zone-a, zone-b, zone-c 중
```

### Helm 차트 설정

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

### 롤아웃 절차

```
zone-a 모두 업그레이드 → 안정화 대기 → zone-b → 안정화 → zone-c
```

각 Zone 내에서는 동시에 여러 인스턴스 재시작 가능 (다른 Zone에 복제본 있음).

### 효과

- 롤아웃 속도 향상
- 데이터 손실 위험 최소화
- 자연스러운 가용 영역 분산

---

## Querier Autoscaling

### KEDA 기반 자동 스케일링

Querier는 stateless이므로 부하에 따라 자동 확장/축소 가능.

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
          sum(querier_inflight_requests{job="loki-querier"})
    
    # 쿼리 큐 길이 기반
    - type: prometheus
      metadata:
        serverAddress: http://prometheus:9090
        metricName: query_scheduler_queue
        threshold: "50"
        query: |
          sum(cortex_query_scheduler_queue_length)
```

### Helm 통합

```yaml
querier:
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 20
    targetCPUUtilizationPercentage: 80
```

### 권장 메트릭

- `loki_querier_inflight_requests` (현재 처리 중)
- `cortex_query_scheduler_queue_length` (큐 대기)
- CPU/메모리 사용률

---

## Query Fairness

### 문제

큰 쿼리가 작은 쿼리를 차단(Head-of-Line Blocking).

### 해결: Query Scheduler

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

### 동작

- 각 테넌트마다 독립된 큐
- Querier가 라운드 로빈으로 각 테넌트 큐에서 작업 가져감
- 한 테넌트의 큰 쿼리가 다른 테넌트를 차단하지 않음

### 추가: Sub-Query Splitting

큰 쿼리는 작은 쿼리로 분할되어 각각 큐에 들어감 → 작은 쿼리도 빠르게 처리 가능.

---

## Query Blocking

### 목적

특정 패턴의 쿼리(주로 비용/성능 문제 있는 쿼리) 차단.

### 설정

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

### 옵션

| 필드 | 설명 |
|------|------|
| `pattern` | 쿼리 매칭 패턴 |
| `regex` | true면 정규식 |
| `types` | 차단할 쿼리 종류 (filter, limited, metric) |
| `hash` | 특정 쿼리 해시 직접 차단 |

### 활용

- 사용자 실수 방지 (예: `{} |= ""`)
- 너무 비싼 쿼리 차단 (예: 라벨 매처 없이 정규식)
- 알려진 문제 쿼리 블록

---

## Tenant Isolation

### 격리 수준

| 레벨 | 격리 수단 |
|------|----------|
| 데이터 | 디렉토리/접두사 분리 |
| 메모리 | 테넌트별 청크 분리 |
| 쿼리 큐 | 테넌트별 큐 |
| Rate | 테넌트별 한도 |
| 인스턴스 | Shuffle Sharding |

### 강한 격리: 별도 클러스터

매우 중요한 테넌트는 별도 Loki 클러스터로 분리하는 것이 안전.

### Multi-tenant Federation

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

## Authentication

### 기본: 인증 없음

Loki는 자체 인증을 제공하지 않습니다. 운영 환경에서는 필수로 추가:

### 옵션 1: 리버스 프록시

#### Nginx Basic Auth

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

#### oauth2-proxy

```yaml
- name: oauth2-proxy
  args:
    - --provider=oidc
    - --upstream=http://loki:3100
    - --pass-access-token=true
    - --pass-user-headers=true
    - --set-xauthrequest=true
```

### 옵션 2: Service Mesh

Istio, Linkerd 등으로 mTLS 강제.

### 옵션 3: API Gateway

Kong, AWS API Gateway 등으로 API 키 검증.

### 멀티 테넌시 강제

```yaml
auth_enabled: true
```

활성화 시 모든 요청에 `X-Scope-OrgID` 필수. 인증 프록시가 테넌트별 헤더를 주입하도록 설정.

---

## Overrides Exporter

### 목적

테넌트별 한도(limit) 설정을 Prometheus 메트릭으로 노출.

### 활성화

```yaml
target: overrides-exporter   # 또는 모놀리식 모드 일부
```

### 노출 메트릭

```
loki_overrides_defaults{limit_name="ingestion_rate_mb"} 4
loki_overrides{tenant="tenant-a", limit_name="ingestion_rate_mb"} 100
loki_overrides{tenant="tenant-b", limit_name="ingestion_rate_mb"} 5
```

### 활용

- 한도 변경 추적
- 한도 vs 실제 사용량 비교 알림
- 테넌트 사용 현황 대시보드

### 사용량 vs 한도 비교 알림

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
