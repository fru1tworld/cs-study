# Mimir 운영 관리

> 이 문서는 Grafana Mimir 공식 문서의 "Manage" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/manage/

---

## 목차

1. [멀티 테넌시 운영](#멀티-테넌시-운영)
2. [용량 계획](#용량-계획)
3. [성능 튜닝](#성능-튜닝)
4. [캐싱](#캐싱)
5. [Sharding 전략](#sharding-전략)
6. [Compaction 운영](#compaction-운영)
7. [Cardinality 관리](#cardinality-관리)
8. [모니터링과 SLO](#모니터링과-slo)
9. [장애 대응](#장애-대응)
10. [Runbook](#runbook)

---

## 멀티 테넌시 운영

### 테넌트 격리

```yaml
limits:
  # 테넌트당 활성 시계열 한도
  max_global_series_per_user: 1_500_000
  max_global_series_per_metric: 50_000
  
  # 수집 Rate
  ingestion_rate: 25_000
  ingestion_burst_size: 250_000
  
  # 쿼리
  max_samples_per_query: 50_000_000
  max_query_lookback: 0
  
  # Recording/Alerting
  ruler_max_rules_per_rule_group: 20
  ruler_max_rule_groups_per_tenant: 70
```

### 테넌트별 오버라이드

```yaml
# runtime.yaml
overrides:
  large-tenant:
    ingestion_rate: 100_000
    max_global_series_per_user: 10_000_000
    max_samples_per_query: 200_000_000
  
  small-tenant:
    ingestion_rate: 1_000
    max_global_series_per_user: 50_000
```

### Shuffle Sharding

테넌트별로 일부 인스턴스만 사용해 격리.

```yaml
limits:
  ingestion_tenant_shard_size: 6      # ingester
  store_gateway_tenant_shard_size: 4  # store gateway
  ruler_tenant_shard_size: 2          # ruler
  compactor_blocks_retention_period: 720h
```

장점:
- 한 테넌트의 무거운 워크로드가 다른 테넌트에 영향 최소화
- 모든 인스턴스를 사용하지 않으므로 메모리 효율

---

## 용량 계획

### 활성 시계열 기준 사이징

| 활성 시계열 | Ingester 수 | RAM/Ingester | Querier 수 | Store GW 수 |
|------------|------------|--------------|-----------|------------|
| 1M | 3 | 16GB | 2 | 2 |
| 10M | 6 | 32GB | 4 | 3 |
| 100M | 30 | 32GB | 16 | 10 |
| 1B | 100 | 64GB | 50 | 30 |

### Ingester 시계열 추정

각 활성 시계열은 약 **8KB 메모리** 사용 (압축 후).

```
Ingester RAM ≈ (시계열 수 × 8KB) × 1.5 (오버헤드)
```

### 오브젝트 스토리지

압축률은 일반적으로 **10:1 ~ 30:1** (메모리 대비).

```
일일 데이터 ≈ 활성 시계열 × 샘플/일 × 2 bytes (압축 후)
```

예: 10M 시계열 × 8640 (10초 간격) × 2 = ~170GB/일

### 보존 기간별 총 용량

```
총 용량 = 일일 데이터 × 보존 일수 × 복제 계수 (블록 단계: 3, 압축 후: 1.5)
```

---

## 성능 튜닝

### 쿼리 성능

#### Query Sharding

```yaml
query_frontend:
  query_sharding_enabled: true
  query_sharding_total_shards: 16
  query_sharding_max_sharded_queries: 128
  query_sharding_max_regexp_size_bytes: 4096
```

병렬 처리로 큰 쿼리 가속.

#### Query Splitting

```yaml
query_frontend:
  split_queries_by_interval: 24h
  align_queries_with_step: true
```

#### 결과 캐싱

```yaml
query_frontend:
  results_cache:
    backend: memcached
    memcached:
      addresses: dns+memcached-results:11211
      max_idle_connections: 16
      timeout: 200ms
  cache_results: true
  cache_unaligned_requests: true
```

### Ingester 성능

#### TSDB 튜닝

```yaml
blocks_storage:
  tsdb:
    block_ranges_period: [2h0m0s]
    retention_period: 24h
    head_compaction_interval: 1m
    head_compaction_concurrency: 5
    
    # WAL
    wal_compression: snappy
    wal_segment_size_bytes: 134217728  # 128MB
    
    # Stripe (메모리 lock 분산)
    stripe_size: 16384
    
    # OOO (Out-of-Order) 샘플
    out_of_order_time_window: 0
```

#### Ingester 한도

```yaml
ingester:
  instance_limits:
    max_ingestion_rate: 0
    max_series: 1_500_000
    max_tenants: 0
    max_inflight_push_requests: 30000
```

### Distributor 성능

```yaml
distributor:
  pool:
    health_check_ingesters: true
    client_cleanup_period: 15s
  
  remote_timeout: 20s
  
  ingestion_burst_factor: 0  # 0이면 ingestion_burst_size 사용
```

---

## 캐싱

### 4종 캐시

| 캐시 | 대상 | 권장 용량 |
|------|------|----------|
| Results Cache | 쿼리 결과 | 시간 범위에 따라 |
| Chunks Cache | 청크 데이터 | 활성 데이터의 50% |
| Metadata Cache | 메타데이터 (블록 목록) | 작음 (1-10GB) |
| Index Cache | 인덱스 | 카디널리티에 따라 |

### Memcached 권장 사양

| 캐시 | 노드 수 | RAM/노드 |
|------|--------|---------|
| Results | 3 | 4-16GB |
| Chunks | 3-10 | 16-64GB |
| Metadata | 3 | 1-4GB |
| Index | 3-10 | 16-64GB |

### Redis 옵션

```yaml
query_frontend:
  results_cache:
    backend: redis
    redis:
      endpoint: redis:6379
      timeout: 200ms
      expiration: 24h
      max_connection_age: 0
```

---

## Sharding 전략

### Ingester Sharding

`shuffle-sharding` 권장:

```yaml
limits:
  ingestion_tenant_shard_size: 6  # 테넌트당 6 ingester만 사용
```

### Store Gateway Sharding

```yaml
store_gateway:
  sharding_ring:
    replication_factor: 3
    zone_awareness_enabled: true
    
    # shuffle sharding
    shard_per_tenant: true
```

### Compactor Sharding

```yaml
compactor:
  sharding_ring:
    kvstore:
      store: memberlist
    
    shard_per_tenant: true
```

각 테넌트의 압축을 한 compactor가 담당.

---

## Compaction 운영

### 블록 단계

```
2h 블록 (Ingester) → 12h → 24h → ...
```

### 설정

```yaml
compactor:
  data_dir: /data/compactor
  
  block_ranges: [2h, 12h, 24h]
  block_sync_concurrency: 8
  meta_sync_concurrency: 20
  
  cleanup_interval: 15m
  consistency_delay: 0s
  retention_delete_delay: 12h
  
  symbols_flushers_concurrency: 4
  
  max_opening_blocks_concurrency: 4
  max_closing_blocks_concurrency: 1
  
  # Vertical Compaction (replication 제거)
  deletion_delay: 12h
```

### 모니터링

| 메트릭 | 설명 |
|--------|------|
| `cortex_compactor_runs_completed_total` | 완료된 압축 사이클 |
| `cortex_compactor_runs_failed_total` | 실패한 사이클 |
| `cortex_compactor_block_cleanup_duration_seconds` | 정리 시간 |
| `cortex_compactor_blocks_cleaned_total` | 정리된 블록 수 |

---

## Cardinality 관리

### 카디널리티 폭증 방지

```yaml
limits:
  max_global_series_per_user: 1_500_000
  max_global_series_per_metric: 50_000
  max_label_names_per_series: 30
  max_label_value_length: 2048
  max_label_name_length: 1024
```

### 분석 도구

```bash
# 메트릭별 카디널리티
mimirtool analyze metric \
  --address=http://mimir:9009 \
  --id=tenant-1

# 라벨별 카디널리티
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/label_names" \
  -H "X-Scope-OrgID: tenant-1"

# 활성 시계열 분석
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/active_series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'selector={job="my-app"}'
```

### 라벨 드롭

```yaml
# Prometheus
remote_write:
  - url: http://mimir/api/v1/push
    write_relabel_configs:
      - source_labels: [pod]
        regex: '(.{50}).*'  # pod 이름 50자로 자르기
        target_label: pod
        replacement: '$1'
      - source_labels: [request_id]
        action: labeldrop
```

```alloy
prometheus.relabel "drop_high_cardinality" {
  forward_to = [...]
  
  rule {
    action = "labeldrop"
    regex  = "request_id|trace_id|user_id"
  }
}
```

---

## 모니터링과 SLO

### 자체 메트릭

`/metrics` 엔드포인트 (각 컴포넌트).

### Mimir Mixin

[Mimir Mixin](https://github.com/grafana/mimir/tree/main/operations/mimir-mixin) 사용:

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

대시보드 + Recording Rules + Alerting Rules 일괄 제공.

### 권장 SLO

| SLO | 목표 |
|-----|------|
| Write 가용성 | > 99.9% |
| Read 가용성 | > 99.5% |
| Write Latency P99 | < 1s |
| Read Latency P99 | < 30s |

### 핵심 메트릭

| 메트릭 | 모니터링 |
|--------|---------|
| `cortex_distributor_samples_in_total` | 수집 부하 |
| `cortex_ingester_memory_series` | Ingester 메모리 사용 |
| `cortex_ingester_active_series` | 테넌트별 활성 시계열 |
| `cortex_request_duration_seconds` | 요청 지연 |
| `cortex_querier_request_duration_seconds` | 쿼리 지연 |
| `cortex_compactor_runs_completed_total` | 압축 진행 |

---

## 장애 대응

### Ingester 장애

#### 단일 인스턴스 다운

- WAL이 활성화되어 있으면 데이터 손실 없이 재시작
- Replication factor 3이면 다른 ingester가 처리

#### 다수 다운

- Read 영향: 최근 데이터 일부 누락 (스토리지에 플러시 안 됨)
- Write 영향: Quorum 부족 시 쓰기 실패

복구:
1. 새 인스턴스 시작
2. WAL 리플레이 (자동)
3. Ring 안정화 대기
4. 정상 동작 확인

### 오브젝트 스토리지 장애

- Ingester는 메모리 + WAL에 데이터 보관 (재시도)
- Querier는 캐시된 데이터로 일부 응답
- 복구 후 Ingester가 자동 업로드 재시도

### Hash Ring 분할 (Split Brain)

Memberlist gossip 사용 시 자가 치유. 일시적 분할은 자동 해결.

영구 분할 시:
1. 한쪽 클러스터 종료
2. 다른 쪽 정상화 확인
3. 종료한 쪽 재시작

---

## Runbook

Mimir Mixin이 제공하는 [공식 Runbook](https://github.com/grafana/mimir/blob/main/operations/mimir-mixin/runbook.md) 참조.

### 자주 발생하는 알림

#### MimirIngesterUnhealthy

원인: Ingester가 ring에서 unhealthy 상태

대응:
1. Ingester 로그 확인
2. 메모리/CPU 사용률 확인
3. 필요시 재시작 (한 번에 하나씩)

#### MimirIngesterReachingSeriesLimit

원인: 활성 시계열 한도 접근

대응:
1. 카디널리티 분석
2. 임시로 한도 증가 (overrides)
3. 라벨 드롭으로 카디널리티 감소

#### MimirRequestErrors

원인: 요청 에러율 증가

대응:
1. 어느 컴포넌트가 에러 내는지 확인
2. 에러 종류 파악 (5xx vs 4xx)
3. 백엔드 의존성 확인 (오브젝트 스토리지, KV 스토어)

#### MimirCompactorHasNotSuccessfullyRunCompaction

원인: Compactor가 일정 시간 동안 압축 실패

대응:
1. Compactor 로그 확인
2. 디스크 공간 확인
3. 오브젝트 스토리지 권한 확인
4. 한 테넌트 문제면 해당 테넌트 격리

---

## 백업 전략

### 오브젝트 스토리지 백업

가장 중요. 다음 방법 권장:

- **S3**: Cross-Region Replication, Versioning
- **GCS**: Object Versioning, Multi-region buckets
- **Azure**: Geo-Redundant Storage (GRS)

### 구성 백업

- Helm values
- runtime config (테넌트 오버라이드)
- 룰 파일 (gitops)
- Alertmanager 설정 (gitops)

### Disaster Recovery

1. 새 클러스터 배포 (동일 구성)
2. 기존 오브젝트 스토리지 마운트 (또는 복제 버킷 사용)
3. WAL 데이터는 손실되나 그 외 복원 가능
4. Prometheus의 로컬 데이터로 갭 복구
