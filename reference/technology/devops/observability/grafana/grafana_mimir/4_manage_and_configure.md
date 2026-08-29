# Mimir 운영 관리와 고급 구성

## Mimir 운영 관리

> 원본: https://grafana.com/docs/mimir/latest/manage/

---

### 목차

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

### 멀티 테넌시 운영

#### 테넌트 격리

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

#### 테넌트별 오버라이드

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

#### Shuffle Sharding

테넌트별로 일부 인스턴스만 사용 → 격리함.

```yaml
limits:
  ingestion_tenant_shard_size: 6      # ingester
  store_gateway_tenant_shard_size: 4  # store gateway
  ruler_tenant_shard_size: 2          # ruler
  compactor_blocks_retention_period: 720h
```

장점:
- 한 테넌트의 무거운 워크로드가 다른 테넌트에 미치는 영향을 최소화
- 전체 인스턴스를 사용하지 않으므로 메모리 효율적

---

### 용량 계획

#### 활성 시계열 기준 사이징

- 활성 시계열 1M: Ingester 3대 · RAM/Ingester 16GB · Querier 2대 · Store GW 2대
- 활성 시계열 10M: Ingester 6대 · RAM/Ingester 32GB · Querier 4대 · Store GW 3대
- 활성 시계열 100M: Ingester 30대 · RAM/Ingester 32GB · Querier 16대 · Store GW 10대
- 활성 시계열 1B: Ingester 100대 · RAM/Ingester 64GB · Querier 50대 · Store GW 30대

#### Ingester 시계열 추정

활성 시계열 하나당 약 8KB 메모리 사용(압축 후).

```
Ingester RAM ≈ (시계열 수 × 8KB) × 1.5 (오버헤드)
```

#### 오브젝트 스토리지

압축률은 일반적으로 메모리 대비 10:1 ~ 30:1 수준.

```
일일 데이터 ≈ 활성 시계열 × 샘플/일 × 2 bytes (압축 후)
```

예: 10M 시계열 × 8640 (10초 간격) × 2 = ~170GB/일

#### 보존 기간별 총 용량

```
총 용량 = 일일 데이터 × 보존 일수 × 복제 계수 (블록 단계: 3, 압축 후: 1.5)
```

---

### 성능 튜닝

#### 쿼리 성능

##### Query Sharding

```yaml
query_frontend:
  query_sharding_enabled: true
  query_sharding_total_shards: 16
  query_sharding_max_sharded_queries: 128
  query_sharding_max_regexp_size_bytes: 4096
```

병렬 처리로 대형 쿼리를 가속함.

##### Query Splitting

```yaml
query_frontend:
  split_queries_by_interval: 24h
  align_queries_with_step: true
```

##### 결과 캐싱

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

#### Ingester 성능

##### TSDB 튜닝

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

##### Ingester 한도

```yaml
ingester:
  instance_limits:
    max_ingestion_rate: 0
    max_series: 1_500_000
    max_tenants: 0
    max_inflight_push_requests: 30000
```

#### Distributor 성능

```yaml
distributor:
  pool:
    health_check_ingesters: true
    client_cleanup_period: 15s
  
  remote_timeout: 20s
  
  ingestion_burst_factor: 0  # 0이면 ingestion_burst_size 사용
```

---

### 캐싱

#### 4종 캐시

- Results Cache: 대상 쿼리 결과 · 권장 용량 시간 범위에 따라
- Chunks Cache: 대상 청크 데이터 · 권장 용량 활성 데이터의 50%
- Metadata Cache: 대상 메타데이터(블록 목록) · 권장 용량 작음(1-10GB)
- Index Cache: 대상 인덱스 · 권장 용량 카디널리티에 따라

#### Memcached 권장 사양

- Results: 노드 수 3 · RAM/노드 4-16GB
- Chunks: 노드 수 3-10 · RAM/노드 16-64GB
- Metadata: 노드 수 3 · RAM/노드 1-4GB
- Index: 노드 수 3-10 · RAM/노드 16-64GB

#### Redis 옵션

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

### Sharding 전략

#### Ingester Sharding

`shuffle-sharding` 사용 권장:

```yaml
limits:
  ingestion_tenant_shard_size: 6  # 테넌트당 6 ingester만 사용
```

#### Store Gateway Sharding

```yaml
store_gateway:
  sharding_ring:
    replication_factor: 3
    zone_awareness_enabled: true
    
    # shuffle sharding
    shard_per_tenant: true
```

#### Compactor Sharding

```yaml
compactor:
  sharding_ring:
    kvstore:
      store: memberlist
    
    shard_per_tenant: true
```

각 테넌트의 압축은 하나의 compactor가 전담.

---

### Compaction 운영

#### 블록 단계

```
2h 블록 (Ingester) → 12h → 24h → ...
```

#### 설정

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

#### 모니터링

- `cortex_compactor_runs_completed_total`: 완료된 압축 사이클
- `cortex_compactor_runs_failed_total`: 실패한 사이클
- `cortex_compactor_block_cleanup_duration_seconds`: 정리 시간
- `cortex_compactor_blocks_cleaned_total`: 정리된 블록 수

---

### Cardinality 관리

#### 카디널리티 폭증 방지

```yaml
limits:
  max_global_series_per_user: 1_500_000
  max_global_series_per_metric: 50_000
  max_label_names_per_series: 30
  max_label_value_length: 2048
  max_label_name_length: 1024
```

#### 분석 도구

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

#### 라벨 드롭

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

### 모니터링과 SLO

#### 자체 메트릭

각 컴포넌트의 `/metrics` 엔드포인트를 통해 자체 메트릭을 수집.

#### Mimir Mixin

[Mimir Mixin](https://github.com/grafana/mimir/tree/main/operations/mimir-mixin) 사용:

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

대시보드, Recording Rules, Alerting Rules를 일괄 제공.

#### 권장 SLO

- Write 가용성: 목표 > 99.9%
- Read 가용성: 목표 > 99.5%
- Write Latency P99: 목표 < 1s
- Read Latency P99: 목표 < 30s

#### 핵심 메트릭

- `cortex_distributor_samples_in_total`: 수집 부하 모니터링
- `cortex_ingester_memory_series`: Ingester 메모리 사용 모니터링
- `cortex_ingester_active_series`: 테넌트별 활성 시계열 모니터링
- `cortex_request_duration_seconds`: 요청 지연 모니터링
- `cortex_querier_request_duration_seconds`: 쿼리 지연 모니터링
- `cortex_compactor_runs_completed_total`: 압축 진행 모니터링

---

### 장애 대응

#### Ingester 장애

##### 단일 인스턴스 다운

- WAL이 활성화되어 있으면 데이터 손실 없이 재시작 가능
- Replication factor가 3이면 다른 ingester가 요청을 처리

##### 다수 다운

- Read 영향: 스토리지에 플러시되지 않은 최근 데이터 일부 누락
- Write 영향: Quorum 부족 시 쓰기 실패

복구:
1. 새 인스턴스 시작
2. WAL 리플레이 (자동)
3. Ring 안정화 대기
4. 정상 동작 확인

#### 오브젝트 스토리지 장애

- Ingester는 메모리와 WAL에 데이터를 보관하며 업로드를 재시도
- Querier는 캐시된 데이터로 일부 응답 가능
- 복구 후 Ingester가 자동으로 업로드를 재시도

#### Hash Ring 분할 (Split Brain)

Memberlist gossip 사용 시 자가 치유 → 일시적 분할은 자동으로 해결됨.

영구 분할 시:
1. 한쪽 클러스터 종료
2. 다른 쪽 정상화 확인
3. 종료한 쪽 재시작

---

### Runbook

Mimir Mixin이 제공하는 [공식 Runbook](https://github.com/grafana/mimir/blob/main/operations/mimir-mixin/runbook.md) 참조.

#### 자주 발생하는 알림

##### MimirIngesterUnhealthy

원인: Ingester가 ring에서 unhealthy 상태

대응:
1. Ingester 로그 확인
2. 메모리/CPU 사용률 확인
3. 필요시 재시작 (한 번에 하나씩)

##### MimirIngesterReachingSeriesLimit

원인: 활성 시계열 한도 접근

대응:
1. 카디널리티 분석
2. 임시로 한도 증가 (overrides)
3. 라벨 드롭으로 카디널리티 감소

##### MimirRequestErrors

원인: 요청 에러율 증가

대응:
1. 어느 컴포넌트가 에러 내는지 확인
2. 에러 종류 파악 (5xx vs 4xx)
3. 백엔드 의존성 확인 (오브젝트 스토리지, KV 스토어)

##### MimirCompactorHasNotSuccessfullyRunCompaction

원인: Compactor가 일정 시간 동안 압축 실패

대응:
1. Compactor 로그 확인
2. 디스크 공간 확인
3. 오브젝트 스토리지 권한 확인
4. 한 테넌트 문제면 해당 테넌트 격리

---

### 백업 전략

#### 오브젝트 스토리지 백업

가장 중요한 백업 대상 → 다음 방법 권장:

- S3: Cross-Region Replication, Versioning
- GCS: Object Versioning, Multi-region buckets
- Azure: Geo-Redundant Storage(GRS)

#### 구성 백업

- Helm values
- runtime config (테넌트 오버라이드)
- 룰 파일 (gitops)
- Alertmanager 설정 (gitops)

#### Disaster Recovery

1. 새 클러스터 배포 (동일 구성)
2. 기존 오브젝트 스토리지 마운트 (또는 복제 버킷 사용)
3. WAL 데이터는 손실될 수 있으나 그 외 데이터는 복원 가능
4. Prometheus 로컬 데이터로 갭 복구

---

## Mimir 고급 구성

> 원본: https://grafana.com/docs/mimir/latest/configure/

---

### 목차

1. [HA 중복 제거 (HA Tracker)](#ha-중복-제거-ha-tracker)
2. [HA Tracker 마이그레이션](#ha-tracker-마이그레이션)
3. [오브젝트 스토리지 백엔드 상세](#오브젝트-스토리지-백엔드-상세)
4. [TSDB 블록 업로드](#tsdb-블록-업로드)
5. [비정렬(Out-of-Order) 샘플 수집](#비정렬out-of-order-샘플-수집)
6. [Native Histograms 상세](#native-histograms-상세)
7. [Kafka 백엔드 (실험적)](#kafka-백엔드-실험적)
8. [DNS Service Discovery](#dns-service-discovery)
9. [Reactive Limiter](#reactive-limiter)
10. [Circuit Breaker](#circuit-breaker)
11. [Spread-Minimizing Tokens](#spread-minimizing-tokens)
12. [Zone-aware Replication](#zone-aware-replication)

---

### HA 중복 제거 (HA Tracker)

#### 문제

Prometheus를 HA 페어로 운영 → 동일 메트릭이 두 번 푸시되어 중복 발생.

#### 해결: HA Tracker

각 클러스터에서 한 시점에 하나의 replica만 활성으로 인식.

#### Distributor 설정

```yaml
distributor:
  ha_tracker:
    enable_ha_tracker: true
    
    kvstore:
      store: consul
      consul:
        host: consul:8500
        acl_token: ""
        http_client_timeout: 20s
        consistent_reads: false
    
    ha_tracker_update_timeout: 15s
    ha_tracker_update_timeout_jitter_max: 5s
    ha_tracker_failover_timeout: 30s

limits:
  ha_cluster_label: cluster
  ha_replica_label: __replica__
  accept_ha_samples: true
  drop_replica_label: true
```

#### Prometheus 측 설정

```yaml
# prometheus-1.yml
global:
  external_labels:
    cluster: prod
    __replica__: replica-0
```

```yaml
# prometheus-2.yml  
global:
  external_labels:
    cluster: prod
    __replica__: replica-1
```

#### KV Store 옵션

- `consul`: HA Tracker 표준
- `etcd`: 대안
- `memberlist`: 비권장(deduplication에 부적합)

#### 동작

```
1. Prometheus-1 푸시 → Distributor가 KV에 "active=replica-0" 기록
2. Prometheus-2 푸시 → KV 조회 → 비활성 → 무시
3. Prometheus-1 다운 (timeout) → Prometheus-2가 활성으로 승격
4. 다음 푸시부터 Prometheus-2 데이터 수용
```

---

### HA Tracker 마이그레이션

#### Consul → etcd

1. etcd 클러스터 준비
2. 새 KV에 동일 데이터 복사 (선택적)
3. Mimir 구성 변경
4. Distributor 순차 재시작

```yaml
# 변경 전
distributor:
  ha_tracker:
    kvstore:
      store: consul
      consul:
        host: consul:8500

# 변경 후
distributor:
  ha_tracker:
    kvstore:
      store: etcd
      etcd:
        endpoints:
          - etcd-0:2379
          - etcd-1:2379
          - etcd-2:2379
```

#### 다운타임 최소화

- failover_timeout보다 짧은 시간 내 마이그레이션
- 또는 활성/대기 클러스터 모두 동일 KV 사용 후 점진적 전환

---

### 오브젝트 스토리지 백엔드 상세

#### S3

```yaml
common:
  storage:
    backend: s3
    s3:
      endpoint: s3.amazonaws.com
      region: us-east-1
      bucket_name: my-mimir
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}
      
      # 옵션
      insecure: false
      signature_version: v4
      list_objects_version: ""
      bucket_lookup_type: auto    # auto, virtual-hosted, path
      dualstack_enabled: true
      part_size: 0                # 0 = auto (5MB)
      send_content_md5: false
      
      # SSE
      sse:
        type: SSE-S3              # 또는 SSE-KMS
        kms_key_id: ""
        kms_encryption_context: ""
      
      # HTTP
      http:
        idle_conn_timeout: 90s
        response_header_timeout: 2m
        insecure_skip_verify: false
        tls_handshake_timeout: 10s
        expect_continue_timeout: 1s
        max_idle_connections: 100
        max_idle_connections_per_host: 100
        max_connections_per_host: 0
      
      # Trace
      trace:
        enabled: false
```

#### GCS

```yaml
common:
  storage:
    backend: gcs
    gcs:
      bucket_name: my-mimir
      service_account: ""        # JSON 키 또는 빈 값(워크로드 ID)
      chunk_buffer_size: 0
```

#### Azure Blob

```yaml
common:
  storage:
    backend: azure
    azure:
      account_name: myaccount
      account_key: ${AZURE_STORAGE_KEY}
      container_name: mimir
      endpoint_suffix: blob.core.windows.net
      max_buffers: 4
      buffer_size: 3145728
      max_retries: 20
      
      # MSI (Managed Identity) 사용
      msi_resource: ""
      user_assigned_id: ""
```

#### MinIO (S3 호환)

```yaml
common:
  storage:
    backend: s3
    s3:
      endpoint: minio:9000
      region: us-east-1
      bucket_name: mimir
      access_key_id: minio
      secret_access_key: minio123
      insecure: true             # HTTP
      bucket_lookup_type: path   # MinIO는 path-style
```

#### 컴포넌트별 다른 백엔드

```yaml
common:
  storage:
    backend: s3
    s3:
      bucket_name: mimir-default

# Blocks는 다른 버킷
blocks_storage:
  s3:
    bucket_name: mimir-blocks

# Ruler 별도
ruler_storage:
  s3:
    bucket_name: mimir-rules

# Alertmanager 별도
alertmanager_storage:
  s3:
    bucket_name: mimir-alertmanager
```

---

### TSDB 블록 업로드

Ingester가 블록을 오브젝트 스토리지에 직접 업로드 → Compactor가 이후 압축을 담당.

#### Ingester → Object Storage

```yaml
blocks_storage:
  tsdb:
    dir: /data/tsdb
    block_ranges_period: [2h0m0s]
    retention_period: 24h        # Ingester 메모리/디스크 보존
    
    # 업로드
    ship_interval: 1m            # 블록 업로드 빈도
    ship_concurrency: 10
    
    # 압축
    head_compaction_interval: 1m
    head_compaction_concurrency: 5
    head_compaction_idle_timeout: 30m
    
    # WAL
    wal_compression: snappy
    wal_segment_size_bytes: 134217728  # 128MB
    wal_replay_concurrency: 0
    
    # OOO
    out_of_order_capacity_max: 32
    
    # 메모리 매핑
    memory_snapshot_on_shutdown: false
    head_chunks_write_buffer_size_bytes: 4194304
```

#### 외부 도구로 블록 업로드

```bash
# mimirtool로 블록 업로드
mimirtool backfill \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  --user=user --key=password \
  /path/to/blocks/
```

활용:
- Prometheus → Mimir 마이그레이션
- 백업에서 복원

---

### 비정렬(Out-of-Order) 샘플 수집

#### 기본 동작: 거부

기본적으로 시간 순서가 어긋난 샘플은 거부됨.

#### 활성화

```yaml
limits:
  out_of_order_time_window: 10m   # 10분 내 OOO 샘플 허용

ingester:
  blocks_storage:
    tsdb:
      out_of_order_capacity_max: 32
```

#### 활용 사례

- 배치 데이터 입수
- 지연 도착 메트릭
- 다중 리전 글로벌 집계

#### 트레이드오프

- 메모리 사용량 소폭 증가
- 쿼리 시 OOO 헤드 청크를 추가로 탐색
- 일반적으로 활성화 권장(안전)

#### 모니터링

```promql
# OOO 샘플 수
sum(rate(cortex_ingester_tsdb_out_of_order_samples_appended_total[5m]))

# OOO 거부
sum(rate(cortex_ingester_tsdb_out_of_order_samples_rejected_total[5m]))
```

---

### Native Histograms 상세

#### 활성화

```yaml
limits:
  native_histograms_ingestion_enabled: true
  
  # 버킷 수 제한
  max_native_histogram_buckets: 160     # 0 = 무제한
```

#### Ingester 설정

```yaml
ingester:
  blocks_storage:
    tsdb:
      enable_native_histograms: true
```

#### 장점

- 고해상도: 적절한 버킷을 자동으로 결정(Sparse Histograms)
- 단일 시계열: 클래식 히스토그램의 N개 시계열 대신 1개
- 빠른 쿼리: `histogram_quantile`을 직접 계산 가능

#### Prometheus 측 활성화

```bash
prometheus --enable-feature=native-histograms
```

#### 메트릭 코드 변경

##### Go (prometheus/client_golang)

```go
import "github.com/prometheus/client_golang/prometheus"

histogram := prometheus.NewHistogram(prometheus.HistogramOpts{
    Name: "request_duration_seconds",
    Help: "Request duration",
    NativeHistogramBucketFactor: 1.1,           // 자동 버킷
    NativeHistogramMaxBucketNumber: 100,
    NativeHistogramMinResetDuration: time.Hour,
})
```

#### 쿼리 (PromQL 호환)

```promql
# 클래식과 동일
histogram_quantile(0.95, sum by (le) (rate(my_metric[5m])))

# 또는 Native 전용
histogram_count(rate(my_metric[5m]))
histogram_sum(rate(my_metric[5m]))
histogram_avg(rate(my_metric[5m]))
histogram_fraction(0, 100, rate(my_metric[5m]))
```

---

### Kafka 백엔드 (실험적)

#### 개요

Distributor와 Ingester 사이에 Kafka를 두어 버퍼링 및 내구성을 향상시킴.

```
[Distributor] → [Kafka] → [Ingester]
```

#### 활성화

```yaml
ingest_storage:
  enabled: true
  kafka:
    address: kafka:9092
    topic: mimir-ingest
    
    # 클라이언트
    client_id: ${POD_NAME}
    dial_timeout: 2s
    
    # Producer (Distributor)
    producer_max_record_size_bytes: 15728640    # 15MB
    
    # Consumer (Ingester)
    consume_from_position_at_startup: end       # start, end, last-offset
    consumer_group_offset_commit_interval: 1s
    
    # 토픽 자동 생성
    auto_create_topic_enabled: true
    auto_create_topic_default_partitions: 0
    
    # 인증
    sasl_mechanism: ""                         # PLAIN, SCRAM-SHA-256, SCRAM-SHA-512
    sasl_username: ""
    sasl_password: ""
    
    # TLS
    tls_enabled: false

ingester:
  partition_ring:
    kvstore:
      store: memberlist
```

#### 장점

- 내구성: Kafka가 데이터 유실을 방지
- 비동기: Distributor와 Ingester를 디커플링
- 재처리 가능: 오프셋 리셋으로 재수집 가능

#### 단점

- 운영 복잡도 증가(Kafka 클러스터 추가)
- 지연 소폭 증가
- 실험적 기능 → 프로덕션 적용 시 주의 필요

---

### DNS Service Discovery

#### Memberlist 대신 DNS 기반

```yaml
ingester:
  ring:
    kvstore:
      store: memberlist
    
    instance_addr: ${POD_IP}
    instance_id: ${POD_NAME}

memberlist:
  join_members:
    - dns+mimir-gossip-ring.mimir.svc.cluster.local:7946
```

`dns+` 접두사 사용 시 DNS A/AAAA 레코드의 모든 IP를 자동으로 검색.

#### gRPC 클라이언트

```yaml
ingester_client:
  grpc_client_config:
    grpc_compression: snappy
    
  # DNS 기반 디스커버리
  remote_timeout: 5s
```

---

### Reactive Limiter

#### 개요

서버 부하에 반응해 자동으로 한도를 조정하는 실험적 기능임.

#### 활성화

```yaml
distributor:
  reactive_limiter_metrics:
    enabled: true
  
  reactive_limiter_inflight_writes:
    enabled: true
    initial_inflight_limit: 1000
    max_inflight_limit: 10000
    min_inflight_limit: 100
```

#### 동작

- 응답 지연/에러율 모니터링
- 부하 증가 시 자동 한도 감소
- 부하 감소 시 한도 회복

---

### Circuit Breaker

#### Ingester 회로 차단기

Ingester가 과부하 또는 장애 상태일 때 요청을 거부함.

```yaml
ingester:
  push_circuit_breaker:
    enabled: true
    failure_threshold_percentage: 10
    failure_execution_threshold: 100
    thresholding_period: 1m
    cooldown_period: 10s
    initial_delay: 0s
    request_timeout: 2s
  
  read_circuit_breaker:
    enabled: true
    failure_threshold_percentage: 10
    failure_execution_threshold: 100
    thresholding_period: 1m
    cooldown_period: 10s
    request_timeout: 30s
```

#### 효과

- 연쇄 장애(cascade failure) 방지
- 빠른 실패(fast fail)로 클라이언트 보호
- 자동 복구

---

### Spread-Minimizing Tokens

#### 기존 방식

각 Ingester가 무작위 토큰을 할당받아 데이터 분포 불균형 발생 가능.

#### Spread-Minimizing

토큰을 결정론적으로 할당 → 균등하게 분포시킴.

```yaml
ingester:
  ring:
    tokens_file_path: /data/tokens
    
    # Spread-minimizing 토큰 사용
    token_generation_strategy: spread-minimizing
    spread_minimizing_zones:
      - zone-a
      - zone-b
      - zone-c
    spread_minimizing_join_ring_in_order: true
```

#### 효과

- Ingester 간 시계열을 균등하게 분배
- 메모리 사용량 균일화
- 핫스팟 방지

---

### Zone-aware Replication

#### 활성화

```yaml
ingester:
  ring:
    replication_factor: 3
    zone_awareness_enabled: true
    instance_availability_zone: us-east-1a
```

#### Helm

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

#### 효과

- 특정 Zone 장애 시 데이터 손실 없음
- 동일 Zone 내 동시 업그레이드 가능 (다른 Zone에 복제본 유지)
- 롤아웃 속도 향상

#### Store Gateway에도 적용

```yaml
store_gateway:
  sharding_ring:
    replication_factor: 3
    zone_awareness_enabled: true
```
