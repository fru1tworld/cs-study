# Mimir 고급 구성

> 이 문서는 Grafana Mimir 공식 문서의 Configure 고급 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/configure/

---

## 목차

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

## HA 중복 제거 (HA Tracker)

### 문제

Prometheus를 HA 페어로 운영하면 동일 메트릭이 두 번 푸시되어 중복.

### 해결: HA Tracker

각 클러스터에서 한 시점에 한 replica만 활성화로 인식.

### Distributor 설정

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

### Prometheus 측 설정

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

### KV Store 옵션

| Store | 권장 |
|-------|------|
| `consul` | HA Tracker 표준 |
| `etcd` | 대안 |
| `memberlist` | 권장하지 않음 (deduplication에는 부적합) |

### 동작

```
1. Prometheus-1 푸시 → Distributor가 KV에 "active=replica-0" 기록
2. Prometheus-2 푸시 → KV 조회 → 비활성 → 무시
3. Prometheus-1 다운 (timeout) → Prometheus-2가 활성으로 승격
4. 다음 푸시부터 Prometheus-2 데이터 수용
```

---

## HA Tracker 마이그레이션

### Consul → etcd

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

### 다운타임 최소화

- failover_timeout보다 짧은 시간 내 마이그레이션
- 또는 활성/대기 클러스터 모두 동일 KV 사용 후 점진적 전환

---

## 오브젝트 스토리지 백엔드 상세

### S3

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

### GCS

```yaml
common:
  storage:
    backend: gcs
    gcs:
      bucket_name: my-mimir
      service_account: ""        # JSON 키 또는 빈 값(워크로드 ID)
      chunk_buffer_size: 0
```

### Azure Blob

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

### MinIO (S3 호환)

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

### 컴포넌트별 다른 백엔드

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

## TSDB 블록 업로드

Compactor가 블록을 업로드 (또는 Ingester가 직접).

### Ingester → Object Storage

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

### 외부 도구로 블록 업로드

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

## 비정렬(Out-of-Order) 샘플 수집

### 기본: 거부

기본적으로 시간 순서가 어긋난 샘플은 거부.

### 활성화

```yaml
limits:
  out_of_order_time_window: 10m   # 10분 내 OOO 샘플 허용

ingester:
  blocks_storage:
    tsdb:
      out_of_order_capacity_max: 32
```

### 활용 사례

- 배치 데이터 입수
- 지연 도착 메트릭
- 다중 리전 글로벌 집계

### 트레이드오프

- 메모리 사용량 약간 증가
- 쿼리 시 OOO 헤드 청크 추가 검색
- 일반적으로 활성화 권장 (안전)

### 모니터링

```promql
# OOO 샘플 수
sum(rate(cortex_ingester_tsdb_out_of_order_samples_appended_total[5m]))

# OOO 거부
sum(rate(cortex_ingester_tsdb_out_of_order_samples_rejected_total[5m]))
```

---

## Native Histograms 상세

### 활성화

```yaml
limits:
  native_histograms_ingestion_enabled: true
  
  # 한도
  max_native_histogram_buckets: 160     # 0 = 무제한
```

### Ingester 설정

```yaml
ingester:
  blocks_storage:
    tsdb:
      enable_native_histograms: true
```

### 장점

- **고해상도**: 자동으로 적절한 버킷 (Sparse Histograms)
- **단일 시계열**: 클래식의 N개 vs 1개
- **빠른 쿼리**: histogram_quantile 직접 계산

### Prometheus 측 활성화

```bash
prometheus --enable-feature=native-histograms
```

### 메트릭 코드 변경

#### Go (prometheus/client_golang)

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

### 쿼리 (PromQL 호환)

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

## Kafka 백엔드 (실험적)

### 개요

Distributor와 Ingester 사이에 Kafka를 두어 버퍼링/내구성 향상.

```
[Distributor] → [Kafka] → [Ingester]
```

### 활성화

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

### 장점

- **내구성**: Kafka가 데이터 보장
- **비동기**: Distributor와 Ingester 디커플링
- **재처리 가능**: 오프셋 리셋으로 재수집

### 단점

- 운영 복잡도 (Kafka 클러스터 추가)
- 지연 약간 증가
- 실험적 (프로덕션 신중)

---

## DNS Service Discovery

### Memberlist 대신 DNS 기반

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

`dns+` 접두사로 DNS A/AAAA 레코드의 모든 IP를 자동 검색.

### gRPC 클라이언트

```yaml
ingester_client:
  grpc_client_config:
    grpc_compression: snappy
    
  # DNS 기반 디스커버리
  remote_timeout: 5s
```

---

## Reactive Limiter

### 개요

서버 부하에 반응하여 자동으로 한도 조정 (실험적).

### 활성화

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

### 동작

- 응답 지연/에러율 모니터링
- 부하 증가 시 자동 한도 감소
- 부하 감소 시 한도 회복

---

## Circuit Breaker

### Ingester 회로 차단기

Ingester가 과부하/장애 시 요청 거부.

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

### 효과

- 캐스케이드 실패 방지
- 빠른 실패로 클라이언트 보호
- 자동 복구

---

## Spread-Minimizing Tokens

### 기존 방식

각 Ingester가 무작위 토큰 → 데이터 분포 불균형 가능.

### Spread-Minimizing

토큰을 결정론적으로 할당하여 균등 분포.

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

### 효과

- Ingester 간 시계열 균등 분배
- 메모리 사용 균일
- 핫스팟 방지

---

## Zone-aware Replication

### 활성화

```yaml
ingester:
  ring:
    replication_factor: 3
    zone_awareness_enabled: true
    instance_availability_zone: us-east-1a
```

### Helm

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

### 효과

- 한 Zone 장애 시 데이터 손실 없음
- 같은 Zone 내 동시 업그레이드 가능 (다른 Zone에 복제본)
- 롤아웃 속도 향상

### Store Gateway에도 적용

```yaml
store_gateway:
  sharding_ring:
    replication_factor: 3
    zone_awareness_enabled: true
```
