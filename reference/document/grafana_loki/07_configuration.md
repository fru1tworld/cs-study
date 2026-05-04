# Loki 구성 레퍼런스

> 이 문서는 Grafana Loki 공식 문서의 "Configure Loki" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/configure/

---

## 목차

1. [구성 개요](#구성-개요)
2. [최상위 설정](#최상위-설정)
3. [server](#server)
4. [distributor](#distributor)
5. [ingester](#ingester)
6. [querier](#querier)
7. [query_scheduler / frontend / query_range](#query_scheduler--frontend--query_range)
8. [storage_config](#storage_config)
9. [schema_config](#schema_config)
10. [chunk_store_config](#chunk_store_config)
11. [limits_config](#limits_config)
12. [compactor](#compactor)
13. [ruler](#ruler)
14. [memberlist](#memberlist)
15. [runtime_config](#runtime_config)
16. [tracing / analytics](#tracing--analytics)

---

## 구성 개요

Loki는 YAML 형식의 구성 파일로 동작합니다. 명령줄 플래그로도 모든 옵션을 설정할 수 있지만, 일반적으로 `-config.file=loki.yaml` 사용이 권장됩니다.

```bash
loki -config.file=/etc/loki/loki.yaml
```

### 주요 구성 블록 한눈에 보기

```yaml
auth_enabled: false        # 멀티 테넌시 활성화 여부
target: all                # 실행할 컴포넌트

server: { ... }            # HTTP/gRPC 서버
distributor: { ... }
ingester: { ... }
querier: { ... }
query_scheduler: { ... }
frontend: { ... }
query_range: { ... }
query_engine: { ... }      # 실험적 차세대 엔진

storage_config: { ... }
schema_config: { ... }
chunk_store_config: { ... }
index_gateway: { ... }

ruler: { ... }
compactor: { ... }
pattern_ingester: { ... }

limits_config: { ... }
runtime_config: { ... }
memberlist: { ... }

tracing: { ... }
analytics: { ... }
```

---

## 최상위 설정

### `target`

실행할 컴포넌트를 지정합니다.

```yaml
target: all  # Monolithic
# 또는
target: write,read,backend  # SSD
# 또는
target: distributor  # 단일 컴포넌트
```

### `auth_enabled`

`true`로 설정하면 멀티 테넌시가 활성화되어 모든 요청에 `X-Scope-OrgID` 헤더가 필요합니다. `false`이면 테넌트 ID는 `fake`로 고정됩니다.

```yaml
auth_enabled: true
```

### `ballast_bytes`

GC 최적화를 위한 가상 메모리 예약. 일반적으로 사용 가능 메모리의 10-20%로 설정.

```yaml
ballast_bytes: 1073741824  # 1GB
```

---

## server

HTTP 및 gRPC 서버 설정.

```yaml
server:
  http_listen_address: 0.0.0.0
  http_listen_port: 3100
  grpc_listen_address: 0.0.0.0
  grpc_listen_port: 9095
  
  # 요청 크기 제한
  grpc_server_max_recv_msg_size: 104857600  # 100MB
  grpc_server_max_send_msg_size: 104857600
  
  # 로그 레벨
  log_level: info
  log_format: logfmt   # 또는 json
  
  # 그레이스풀 종료
  graceful_shutdown_timeout: 30s
  
  # HTTP 타임아웃
  http_server_read_timeout: 30s
  http_server_write_timeout: 30s
  http_server_idle_timeout: 120s
```

---

## distributor

```yaml
distributor:
  ring:
    kvstore:
      store: memberlist  # 또는 consul, etcd
  
  # 클라이언트로부터 받는 라벨에 추가할 라벨
  rate_store:
    ingestion_burst_size_mb: 6
  
  # OTLP 수신 설정
  otlp_config:
    default_resource_attributes_as_index_labels:
      - service.name
      - service.namespace
      - cloud.region
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
  
  # 청크 설정
  chunk_idle_period: 1h           # idle 청크 플러시 시간
  chunk_block_size: 262144        # 청크 블록 크기 (256KB)
  chunk_target_size: 1572864      # 청크 목표 크기 (1.5MB)
  chunk_retain_period: 30s        # 플러시 후 메모리 보유 시간
  max_chunk_age: 1h               # 청크 최대 수명
  
  # WAL 설정
  wal:
    enabled: true
    dir: /loki/wal
    flush_on_shutdown: true
    replay_memory_ceiling: 4GB
  
  # 동시성
  concurrent_flushes: 16
  flush_check_period: 30s
```

---

## querier

```yaml
querier:
  # 동시 쿼리 수
  max_concurrent: 10
  
  # 쿼리 시간 범위 제한
  query_timeout: 5m
  
  # Ingester 쿼리 설정
  query_ingesters_within: 3h  # 최근 3시간은 Ingester 우선
  
  # 통합된 ingester 쿼리
  query_store_only: false     # true면 스토리지만 쿼리
  query_ingester_only: false  # true면 ingester만 쿼리
  
  # 멀티 테넌트 쿼리
  multi_tenant_queries_enabled: false
```

---

## query_scheduler / frontend / query_range

### query_scheduler

```yaml
query_scheduler:
  max_outstanding_requests_per_tenant: 100
  scheduler_ring:
    kvstore:
      store: memberlist
  
  # 큐 설정
  use_scheduler_ring: true
```

### frontend

```yaml
frontend:
  scheduler_address: query-scheduler:9095
  log_queries_longer_than: 5s
  compress_responses: true
  
  # 결과 캐싱
  max_outstanding_per_tenant: 256
  
  # tail 쿼리
  tail_proxy_url: http://querier:3100
```

### query_range

```yaml
query_range:
  align_queries_with_step: true
  max_retries: 5
  parallelise_shardable_queries: true
  
  # 분할
  split_queries_by_interval: 30m
  
  # 캐싱
  cache_results: true
  results_cache:
    cache:
      memcached_client:
        addresses: dns+memcached:11211
        max_idle_conns: 16
        timeout: 100ms
```

---

## storage_config

스토리지 백엔드 설정.

```yaml
storage_config:
  # AWS S3
  aws:
    s3: s3://access:secret@region/bucket
    region: us-east-1
    s3forcepathstyle: false
  
  # GCS
  gcs:
    bucket_name: my-loki-bucket
    chunk_buffer_size: 0
  
  # Azure
  azure:
    account_name: myaccount
    account_key: mykey
    container_name: loki
  
  # 파일시스템 (개발용)
  filesystem:
    directory: /loki/chunks
  
  # TSDB Shipper 설정
  tsdb_shipper:
    active_index_directory: /loki/tsdb-index
    cache_location: /loki/tsdb-cache
    cache_ttl: 24h
    shared_store: s3       # 위에서 설정한 백엔드
  
  # BoltDB Shipper (Deprecated)
  boltdb_shipper:
    active_index_directory: /loki/boltdb-index
    cache_location: /loki/boltdb-cache
    shared_store: filesystem
  
  # 인덱스 캐시
  index_queries_cache_config:
    memcached_client:
      addresses: dns+memcached:11211
```

---

## schema_config

청크 인덱스 스키마와 저장 위치 정의. **여러 시기의 스키마를 누적해서 정의**합니다.

```yaml
schema_config:
  configs:
    # 기존 스키마 (유지)
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    # 새 스키마 (특정 날짜부터)
    - from: 2024-06-01
      store: tsdb
      object_store: s3
      schema: v13   # 권장 최신 스키마
      index:
        prefix: tsdb_index_
        period: 24h
```

### 스키마 버전

| 버전 | 설명 | 권장 |
|------|------|------|
| v9-v11 | 구식 인덱스 형식 | X |
| v12 | BoltDB Shipper | X (Deprecated) |
| v13 | TSDB | O (현재 권장) |

### 스토어 종류

- `tsdb` (권장)
- `boltdb-shipper` (Deprecated)
- `cassandra`, `bigtable`, `dynamodb` (구식)

---

## chunk_store_config

청크 캐싱 및 보존 설정.

```yaml
chunk_store_config:
  # 청크 캐시
  chunk_cache_config:
    memcached_client:
      addresses: dns+memcached:11211
  
  # 쓰기 디덕플리케이션 캐시
  write_dedupe_cache_config:
    memcached_client:
      addresses: dns+memcached:11211
  
  # 보존 (Compactor 사용 시 비활성화)
  max_look_back_period: 0s
```

---

## limits_config

전역 및 테넌트별 한도. **runtime_config**로 테넌트별 오버라이드 가능.

```yaml
limits_config:
  # 수집 Rate Limit
  ingestion_rate_strategy: global  # 또는 local
  ingestion_rate_mb: 4             # MB/s per tenant (global)
  ingestion_burst_size_mb: 6
  
  # 시계열/스트림 한도
  max_streams_per_user: 10000
  max_global_streams_per_user: 5000
  max_line_size: 256000  # bytes
  max_line_size_truncate: false
  max_label_name_length: 1024
  max_label_value_length: 4096
  max_label_names_per_series: 30
  
  # 쿼리 한도
  max_query_length: 721h        # 30일 + 1시간
  max_query_parallelism: 32
  max_query_series: 500
  max_entries_limit_per_query: 5000
  cardinality_limit: 100000
  
  # 보존 (Compactor용)
  retention_period: 744h        # 31일
  retention_stream:
    - selector: '{namespace="dev"}'
      priority: 1
      period: 24h
  
  # 거부 정책
  reject_old_samples: true
  reject_old_samples_max_age: 168h  # 1주일
  creation_grace_period: 10m
  
  # 분할
  split_queries_by_interval: 30m
  
  # 볼륨 활성화
  volume_enabled: true
  
  # 샤딩
  tsdb_max_query_parallelism: 128
```

---

## compactor

```yaml
compactor:
  working_directory: /loki/compactor
  shared_store: s3   # 또는 위에 정의한 스토리지
  
  # 압축 주기
  compaction_interval: 10m
  
  # 보존 (Compactor가 retention 처리)
  retention_enabled: true
  retention_delete_delay: 2h
  retention_delete_worker_count: 150
  
  # 삭제 (Logs Deletion API)
  delete_request_store: s3
  delete_max_interval: 24h
```

---

## ruler

알림 룰과 Recording 룰 평가.

```yaml
ruler:
  # 룰 저장소
  storage:
    type: local      # 또는 s3, gcs, azure
    local:
      directory: /etc/loki/rules
  
  # 룰 디렉토리 구조: /rules/<tenant_id>/*.yaml
  
  rule_path: /tmp/loki/rules-temp
  
  # Alertmanager
  alertmanager_url: http://alertmanager:9093
  external_url: http://loki:3100
  enable_alertmanager_v2: true
  
  # 평가 주기
  evaluation_interval: 1m
  poll_interval: 1m
  
  # 링 (분산 모드)
  ring:
    kvstore:
      store: memberlist
  
  enable_api: true
  enable_sharding: true
```

### 룰 파일 예시

```yaml
# /etc/loki/rules/tenant1/alert.yaml
groups:
  - name: log_alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate({app="myapp"} |= "error" [5m]))
          > 0.1
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
```

---

## memberlist

Hashicorp memberlist 기반 클러스터 멤버십.

```yaml
memberlist:
  join_members:
    - loki-memberlist:7946
  bind_port: 7946
  bind_addr:
    - 0.0.0.0
  abort_if_cluster_join_fails: false
  
  # 가십 설정
  gossip_interval: 200ms
  gossip_nodes: 3
  
  # TLS
  tls_enabled: false
```

---

## runtime_config

테넌트별 한도 및 설정 동적 오버라이드.

```yaml
runtime_config:
  file: /etc/loki/runtime-config.yaml
  period: 10s   # 재로드 주기
```

`runtime-config.yaml` 예시:

```yaml
overrides:
  tenant-a:
    ingestion_rate_mb: 10
    ingestion_burst_size_mb: 20
    max_streams_per_user: 50000
    retention_period: 2160h   # 90일
  
  tenant-b:
    ingestion_rate_mb: 1
    max_streams_per_user: 1000
    retention_period: 168h    # 7일
```

---

## tracing / analytics

### tracing

```yaml
tracing:
  enabled: true
  # Jaeger / OTLP 환경변수로 설정
```

환경변수 예:
- `JAEGER_AGENT_HOST=jaeger-agent`
- `JAEGER_AGENT_PORT=6831`
- `JAEGER_SAMPLER_TYPE=const`
- `JAEGER_SAMPLER_PARAM=1`

### analytics

```yaml
analytics:
  reporting_enabled: false  # Grafana Labs로 사용 통계 전송 비활성화
```
