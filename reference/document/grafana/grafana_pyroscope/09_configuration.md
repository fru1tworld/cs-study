# Pyroscope 설정 (Configuration)

> 이 문서는 Pyroscope 서버의 설정 파일 구조와 주요 블록을 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/reference-configuration-parameters/

---

## 목차

1. [설정 파일 형식](#설정-파일-형식)
2. [최상위 블록 개요](#최상위-블록-개요)
3. [server 블록](#server-블록)
4. [storage 블록](#storage-블록)
5. [memberlist / ring](#memberlist--ring)
6. [ingester 블록](#ingester-블록)
7. [querier / query-frontend / query-scheduler](#querier--query-frontend--query-scheduler)
8. [compactor 블록](#compactor-블록)
9. [limits 블록](#limits-블록)
10. [analytics와 자기 프로파일링](#analytics와-자기-프로파일링)
11. [실전 예시 모음](#실전-예시-모음)

---

## 설정 파일 형식

YAML 파일로 작성하며 CLI 플래그와 환경 변수로도 일부 오버라이드 가능합니다.

```bash
pyroscope -config.file=pyroscope.yaml -target=all
```

### 환경 변수 치환

```yaml
storage:
  s3:
    access_key_id: ${AWS_ACCESS_KEY_ID}
```

`-config.expand-env=true` 플래그로 활성화.

### 핫 리로드

`SIGHUP` 으로 일부 항목(limits 등) 재로드 가능. 모든 항목이 핫 리로드 가능한 것은 아닙니다.

---

## 최상위 블록 개요

```yaml
target: all                # 활성화할 컴포넌트
multitenancy_enabled: false
http_listen_port: 4040

server: { ... }
storage: { ... }
memberlist: { ... }
ingester: { ... }
distributor: { ... }
querier: { ... }
query_scheduler: { ... }
query_frontend: { ... }
store_gateway: { ... }
compactor: { ... }
limits: { ... }
runtime_config: { ... }
analytics: { ... }
self_profiling: { ... }
tracing: { ... }
```

---

## server 블록

HTTP/gRPC 리스너 설정.

```yaml
server:
  http_listen_port: 4040
  grpc_listen_port: 9095
  log_level: info             # debug, info, warn, error
  log_format: logfmt          # logfmt, json
  http_server_read_timeout: 30s
  http_server_write_timeout: 30s
  grpc_server_max_recv_msg_size: 104857600   # 100MB
  grpc_server_max_send_msg_size: 104857600
```

---

## storage 블록

오브젝트 스토리지(또는 로컬 파일시스템) 설정.

### S3 (또는 호환)

```yaml
storage:
  backend: s3
  s3:
    endpoint: s3.us-east-1.amazonaws.com
    bucket_name: my-pyroscope
    region: us-east-1
    access_key_id: ${AWS_ACCESS_KEY_ID}
    secret_access_key: ${AWS_SECRET_ACCESS_KEY}
    insecure: false
    sse:
      type: SSE-S3
```

### GCS

```yaml
storage:
  backend: gcs
  gcs:
    bucket_name: my-pyroscope
    service_account: |
      {
        "type": "service_account",
        ...
      }
```

### Azure Blob

```yaml
storage:
  backend: azure
  azure:
    account_name: myaccount
    account_key: ${AZURE_KEY}
    container_name: pyroscope
```

### 로컬 파일시스템 (개발/테스트용)

```yaml
storage:
  backend: filesystem
  filesystem:
    dir: /data
```

---

## memberlist / ring

해시 링과 멤버십을 위한 gossip 설정.

```yaml
memberlist:
  bind_port: 7946
  join_members:
    - dns+pyroscope-memberlist:7946
  abort_if_cluster_join_fails: false
  rejoin_interval: 0s
  left_ingesters_timeout: 5m
```

각 컴포넌트의 ring 설정:

```yaml
ingester:
  lifecycler:
    ring:
      kvstore:
        store: memberlist
      replication_factor: 3

distributor:
  ring:
    kvstore:
      store: memberlist
```

대안: `consul`, `etcd`, `inmemory`.

---

## ingester 블록

```yaml
ingester:
  lifecycler:
    join_after: 0s
    final_sleep: 30s
    num_tokens: 128
    ring:
      kvstore:
        store: memberlist
      replication_factor: 3
      heartbeat_period: 5s
      heartbeat_timeout: 1m

phlaredb:
  data_path: /data/pyroscope
  max_block_duration: 1h
  row_group_target_size: 1342177280   # 1.25GiB
```

---

## querier / query-frontend / query-scheduler

```yaml
querier:
  max_concurrent_queries: 10
  query_timeout: 2m
  query_store_after: 12h          # 이 시간 이전은 store-gateway에서 조회
  max_query_lookback: 0s

query_scheduler:
  max_outstanding_requests_per_tenant: 100
  scheduler_ring:
    kvstore: { store: memberlist }

query_frontend:
  scheduler_address: query-scheduler:9095
  log_queries_longer_than: 10s
  query_split_interval: 24h
  cache_results: false
```

---

## compactor 블록

```yaml
compactor:
  data_dir: /data/compactor
  block_ranges: [2h, 12h, 24h]
  compaction_concurrency: 1
  cleanup_concurrency: 1
  block_sync_concurrency: 8
  retention_period: 30d
  sharding_ring:
    kvstore: { store: memberlist }
```

---

## limits 블록

테넌트별/글로벌 한도. (자세한 설명은 [07_manage.md](./07_manage.md))

```yaml
limits:
  ingestion_rate_mb: 4
  ingestion_burst_size_mb: 8
  max_global_series_per_user: 5000
  max_label_names_per_series: 30
  max_query_length: 720h
  max_query_parallelism: 16
  retention_period: 30d
  ingestion_tenant_shard_size: 0    # 0=비활성, >0이면 shuffle sharding
```

### 테넌트별 오버라이드

```yaml
runtime_config:
  file: /etc/pyroscope/overrides.yaml
  reload_period: 10s
```

`overrides.yaml`:

```yaml
overrides:
  tenant-a:
    ingestion_rate_mb: 10
    retention_period: 60d
  tenant-b:
    ingestion_rate_mb: 1
```

---

## analytics와 자기 프로파일링

### analytics

Grafana Labs로 익명 사용 통계 전송. 비활성화 가능.

```yaml
analytics:
  reporting_enabled: false
```

### self_profiling

자기 자신을 프로파일링.

```yaml
self_profiling:
  enabled: true
  url: http://pyroscope:4040
  application_name: pyroscope
  sample_rate: 100
```

---

## 실전 예시 모음

### 단일 노드 Monolithic + S3

```yaml
target: all
multitenancy_enabled: false

server:
  http_listen_port: 4040
  log_level: info

storage:
  backend: s3
  s3:
    endpoint: s3.us-east-1.amazonaws.com
    bucket_name: my-pyroscope
    region: us-east-1
    access_key_id: ${AWS_ACCESS_KEY_ID}
    secret_access_key: ${AWS_SECRET_ACCESS_KEY}

phlaredb:
  data_path: /data/pyroscope
```

### 마이크로서비스 + Memberlist + GCS

```yaml
multitenancy_enabled: true

server:
  http_listen_port: 4040

storage:
  backend: gcs
  gcs:
    bucket_name: prod-pyroscope

memberlist:
  join_members:
    - dns+pyroscope-memberlist.observability.svc.cluster.local:7946

ingester:
  lifecycler:
    ring:
      kvstore: { store: memberlist }
      replication_factor: 3

distributor:
  ring:
    kvstore: { store: memberlist }

querier:
  query_store_after: 6h

compactor:
  retention_period: 30d
  sharding_ring:
    kvstore: { store: memberlist }

store_gateway:
  sharding_ring:
    kvstore: { store: memberlist }

limits:
  ingestion_rate_mb: 8
  retention_period: 30d
  max_global_series_per_user: 100000
```

---

## 다음 단계

- [03_deployment.md](./03_deployment.md) - 배포 모드별 설정 차이
- [07_manage.md](./07_manage.md) - 운영 가이드
- [11_http_api.md](./11_http_api.md) - 상태 점검 API
