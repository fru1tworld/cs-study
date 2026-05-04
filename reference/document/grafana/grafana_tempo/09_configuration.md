# Tempo 구성 레퍼런스

> 이 문서는 Grafana Tempo 공식 문서의 "Configure Tempo" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/configuration/

---

## 목차

1. [구성 파일 개요](#구성-파일-개요)
2. [server](#server)
3. [distributor](#distributor)
4. [ingester](#ingester)
5. [compactor](#compactor)
6. [storage](#storage)
7. [querier](#querier)
8. [query_frontend](#query_frontend)
9. [metrics_generator](#metrics_generator)
10. [overrides](#overrides)
11. [memberlist](#memberlist)
12. [usage_report / cache](#usage_report--cache)

---

## 구성 파일 개요

Tempo는 YAML 구성 파일을 사용. 명령줄 플래그로도 모든 옵션 설정 가능.

```bash
tempo -config.file=/etc/tempo.yaml
```

### 최상위 구조

```yaml
target: all              # 또는 distributor, ingester, ...
multitenancy_enabled: false
http_api_prefix: ""
auth_enabled: false      # 멀티 테넌시 활성화

server: { ... }
distributor: { ... }
ingester: { ... }
compactor: { ... }
querier: { ... }
query_frontend: { ... }
metrics_generator: { ... }
storage: { ... }
overrides: { ... }
memberlist: { ... }
cache: { ... }
usage_report: { ... }
```

---

## server

```yaml
server:
  http_listen_address: 0.0.0.0
  http_listen_port: 3200
  grpc_listen_address: 0.0.0.0
  grpc_listen_port: 9095
  
  # gRPC 메시지 크기
  grpc_server_max_recv_msg_size: 16777216    # 16MB
  grpc_server_max_send_msg_size: 16777216
  grpc_server_max_concurrent_streams: 100
  
  # 로그
  log_level: info
  log_format: logfmt
  
  # TLS
  http_tls_config:
    cert_file: ""
    key_file: ""
    client_auth_type: ""
    client_ca_file: ""
  
  # 그레이스풀 종료
  graceful_shutdown_timeout: 30s
```

---

## distributor

```yaml
distributor:
  ring:
    kvstore:
      store: memberlist
  
  receivers:
    # OpenTelemetry Protocol
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
          max_recv_msg_size_mib: 4
        http:
          endpoint: 0.0.0.0:4318
          cors:
            allowed_origins: ["*"]
    
    # Jaeger
    jaeger:
      protocols:
        grpc:
          endpoint: 0.0.0.0:14250
        thrift_http:
          endpoint: 0.0.0.0:14268
        thrift_compact:
          endpoint: 0.0.0.0:6831
        thrift_binary:
          endpoint: 0.0.0.0:6832
    
    # Zipkin
    zipkin:
      endpoint: 0.0.0.0:9411
    
    # OpenCensus (레거시)
    opencensus:
      endpoint: 0.0.0.0:55678
  
  # 멀티 테넌시
  log_received_spans:
    enabled: false
    include_all_attributes: false
    filter_by_status_error: false
  
  # 메트릭 활성화
  metric_received_spans:
    enabled: false
    root_only: false
  
  # 외부 라벨
  extend_writes: true
  
  # Forward to OTel
  forwarders:
    - dev-tempo
```

---

## ingester

```yaml
ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: memberlist
      replication_factor: 3
    
    final_sleep: 0s
    num_tokens: 128
  
  # 트레이스 처리
  trace_idle_period: 10s          # idle 트레이스 처리 시간
  max_block_bytes: 524288000      # 500MB - 블록 최대 크기
  max_block_duration: 30m         # 블록 최대 시간
  
  # 메모리 한도
  complete_block_timeout: 1h
  
  # 트레이스당 한도
  max_traces_per_user: 10000
  
  # 동시성
  flush_check_period: 10s
  flush_op_timeout: 5m
  
  # WAL
  override_ring_key: tempo
```

---

## compactor

```yaml
compactor:
  ring:
    kvstore:
      store: memberlist
  
  compaction:
    # 블록 보존
    block_retention: 720h            # 30일
    compacted_block_retention: 1h    # 압축 후 보존
    
    # 압축 윈도우
    compaction_window: 1h
    
    # 동시성
    chunk_size_bytes: 5000000
    flush_size_bytes: 30000000
    iterator_buffer_size: 1000
    
    # 한도
    max_compaction_objects: 1000000
    max_block_bytes: 100000000000   # 100GB
    
    # 처리
    retry_delay: 1m
    max_time_per_tenant: 5m
    compaction_cycle: 30s
    
    # 보존 워커
    retention_concurrency: 10
```

---

## storage

```yaml
storage:
  trace:
    backend: s3                    # local, s3, gcs, azure
    
    # WAL (모든 백엔드 공통)
    wal:
      path: /var/tempo/wal
      encoding: snappy             # none, gzip, lz4-64k, lz4-256k, lz4-1M, lz4, snappy, zstd, s2
      ingestion_slack: 2m
    
    # 블록 설정
    block:
      version: vParquet4           # vParquet, vParquet2, vParquet3, vParquet4
      v2_encoding: zstd
      v2_index_downsample_bytes: 1000
      v2_index_page_size_bytes: 250000
      bloom_filter_false_positive: 0.01
      bloom_filter_shard_size_bytes: 100000
      
      # Parquet 전용 컬럼
      parquet_dedicated_columns:
        - scope: span
          name: http.status_code
          type: int
        - scope: resource
          name: namespace
          type: string
    
    # 풀 (워커)
    pool:
      max_workers: 100
      queue_depth: 10000
    
    # 블록 목록 폴링
    blocklist_poll: 5m
    blocklist_poll_concurrency: 50
    blocklist_poll_jitter_ms: 1000
    
    # === 백엔드별 설정 ===
    
    # 로컬 파일시스템
    local:
      path: /var/tempo/blocks
    
    # AWS S3
    s3:
      bucket: my-tempo-bucket
      endpoint: s3.amazonaws.com
      region: us-east-1
      access_key: ${AWS_ACCESS_KEY_ID}
      secret_key: ${AWS_SECRET_ACCESS_KEY}
      forcepathstyle: false
      insecure: false
      hedge_requests_at: 0s
      hedge_requests_up_to: 2
      enable_dual_stack: false
      part_size_mb: 5
    
    # Google Cloud Storage
    gcs:
      bucket_name: my-tempo-bucket
      chunk_buffer_size: 10485760    # 10MB
      hedge_requests_at: 0s
      hedge_requests_up_to: 2
    
    # Azure Blob Storage
    azure:
      container_name: tempo
      storage_account_name: myaccount
      storage_account_key: ${AZURE_STORAGE_KEY}
      max_buffers: 4
      buffer_size: 3145728
```

---

## querier

```yaml
querier:
  # Ingester 클라이언트
  frontend_worker:
    frontend_address: query-frontend:9095
    grpc_client_config:
      max_send_msg_size: 16777216
      max_recv_msg_size: 16777216
  
  # 검색 동시성
  max_concurrent_queries: 20
  
  # Trace by ID 조회
  trace_by_id:
    query_timeout: 10s
  
  # 검색
  search:
    query_timeout: 30s
    prefer_self: 10
    external_endpoints: []          # 외부 검색 노드
  
  # Ingester 클라이언트
  ingester_client:
    grpc_client_config:
      max_send_msg_size: 16777216
      max_recv_msg_size: 16777216
```

---

## query_frontend

```yaml
query_frontend:
  # 분할
  max_outstanding_per_tenant: 2000
  max_batch_size: 7
  
  # Trace by ID
  trace_by_id:
    duration_slo: 5s
    throughput_bytes_slo: 0
  
  # 검색
  search:
    target_bytes_per_job: 104857600  # 100MB
    concurrent_jobs: 1000
    max_duration: 168h               # 1주
    
    # 검색 슬로
    duration_slo: 5s
    throughput_bytes_slo: 1.073741824e+09
  
  # 메트릭 (TraceQL Metrics)
  metrics:
    concurrent_jobs: 1000
    target_bytes_per_job: 1.073741824e+09
    duration_slo: 5s
    throughput_bytes_slo: 0
```

---

## metrics_generator

```yaml
metrics_generator:
  ring:
    kvstore:
      store: memberlist
  
  registry:
    external_labels:
      source: tempo
      cluster: prod
    collection_interval: 15s
    stale_duration: 15m
    max_label_name_length: 1024
    max_label_value_length: 4096
  
  # 프로세서별 설정
  processor:
    service_graphs:
      max_items: 10000
      workers: 10
      histogram_buckets: [0.1, 0.2, 0.4, 0.8, 1.6, 3.2, 6.4, 12.8]
      dimensions: []
      peer_attributes:
        - peer.service
        - db.name
        - db.system
      enable_client_server_prefix: false
    
    span_metrics:
      histogram_buckets: [0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.128, 0.256, 0.512, 1.02, 2.05, 4.10]
      dimensions:
        - http.method
        - http.status_code
      additional_dimensions:
        - http.route
      enable_target_info: true
    
    local_blocks:
      max_live_traces: 10000
      max_block_duration: 5m
      max_block_bytes: 500000000
      flush_check_period: 10s
      trace_idle_period: 10s
      complete_block_timeout: 1h
      filter_server_spans: false
  
  # 스토리지 (Remote Write)
  storage:
    path: /tmp/tempo/generator/wal
    remote_write_flush_deadline: 1m
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
        headers:
          X-Scope-OrgID: tempo
        timeout: 30s
  
  # 트레이스 저장 (Local Blocks용)
  traces_storage:
    path: /tmp/tempo/generator/traces
```

---

## overrides

```yaml
overrides:
  # 글로벌 기본값
  defaults:
    ingestion:
      rate_strategy: local
      rate_limit_bytes: 15000000      # 15MB/s
      burst_size_bytes: 20000000      # 20MB
      max_traces_per_user: 10000
      max_global_traces_per_user: 0
    
    forwarders: []
    
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics
      collection_interval: 15s
      disable_collection: false
      
      processor:
        service_graphs:
          dimensions: []
          peer_attributes: []
          enable_client_server_prefix: false
        
        span_metrics:
          dimensions: []
          enable_target_info: false
    
    read:
      max_search_duration: 0s         # 0 = 무제한
      max_metrics_duration: 0s
      max_bytes_per_trace_search: 0
      max_bytes_per_tag_values_query: 5000000
      max_blocks_per_tag_values_query: 0
    
    storage:
      block_retention: 0s             # 0 = 글로벌 사용
      max_search_lookback: 0s
    
    global:
      max_bytes_per_trace: 5000000    # 5MB
  
  # 테넌트별 오버라이드
  per_tenant_override_config: /etc/tempo/overrides.yaml
  per_tenant_override_period: 10s
```

### per_tenant overrides.yaml

```yaml
overrides:
  tenant-a:
    ingestion:
      rate_limit_bytes: 50000000       # 50MB/s
      max_traces_per_user: 100000
    storage:
      block_retention: 8760h           # 1년
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics
        - local-blocks
  
  tenant-b:
    ingestion:
      rate_limit_bytes: 5000000
    storage:
      block_retention: 168h            # 7일
```

---

## memberlist

```yaml
memberlist:
  abort_if_cluster_join_fails: false
  
  # 가입할 멤버
  join_members:
    - tempo-memberlist:7946
  
  # 바인딩
  bind_addr:
    - 0.0.0.0
  bind_port: 7946
  
  # 가십
  packet_dial_timeout: 5s
  packet_write_timeout: 5s
  pull_push_interval: 30s
  gossip_interval: 200ms
  gossip_nodes: 3
  gossip_to_dead_nodes_time: 30s
  dead_node_reclaim_time: 0s
  
  # TLS
  tls_enabled: false
  tls_cert_path: ""
  tls_key_path: ""
  tls_ca_path: ""
  tls_server_name: ""
  tls_insecure_skip_verify: false
```

---

## usage_report / cache

### usage_report

```yaml
usage_report:
  reporting_enabled: false  # Grafana Labs로 익명 사용 통계 전송
```

### cache

```yaml
cache:
  background:
    writeback_goroutines: 10
    writeback_buffer: 10000
  
  caches:
    # 캐시별 역할 지정
    - roles:
        - bloom            # 블룸 필터
        - parquet-page     # Parquet 페이지
        - frontend-search  # 프론트엔드 검색 결과
        - parquet-footer   # Parquet 푸터
      memcached:
        host: memcached
        service: memcached-client
        consistent_hash: true
        timeout: 200ms
        max_idle_conns: 16
        max_item_size: 1048576
    
    # Redis 옵션
    - roles:
        - frontend-search
      redis:
        endpoint: redis:6379
        timeout: 100ms
        expiration: 24h
        max_connection_age: 0
        password: ""
        db: 0
```

---

## 전체 구성 예시 (Microservices, S3, Grafana Cloud 호환)

```yaml
target: all
multitenancy_enabled: true

server:
  http_listen_port: 3200
  log_level: info

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
  ring:
    kvstore:
      store: memberlist

ingester:
  lifecycler:
    ring:
      replication_factor: 3
      kvstore:
        store: memberlist
  trace_idle_period: 10s
  max_block_bytes: 524288000
  max_block_duration: 30m

compactor:
  compaction:
    block_retention: 720h
  ring:
    kvstore:
      store: memberlist

querier:
  max_concurrent_queries: 20
  search:
    query_timeout: 30s

query_frontend:
  search:
    target_bytes_per_job: 104857600

metrics_generator:
  ring:
    kvstore:
      store: memberlist
  processor:
    service_graphs:
      max_items: 10000
    span_metrics:
      histogram_buckets: [0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.128, 0.256, 0.512, 1.02, 2.05, 4.10]
  storage:
    path: /var/tempo/generator/wal
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
        headers:
          X-Scope-OrgID: tempo

storage:
  trace:
    backend: s3
    s3:
      bucket: my-tempo
      endpoint: s3.amazonaws.com
      region: us-east-1
    block:
      version: vParquet4
    wal:
      path: /var/tempo/wal
    pool:
      max_workers: 100
      queue_depth: 10000

memberlist:
  join_members:
    - tempo-memberlist:7946

overrides:
  defaults:
    ingestion:
      rate_limit_bytes: 15000000
      burst_size_bytes: 20000000
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics
```
