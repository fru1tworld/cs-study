# Mimir 설치 및 구성

> 이 문서는 Grafana Mimir 공식 문서의 "Set up and configure" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/set-up/

---

## 목차

1. [설치 방법 개요](#설치-방법-개요)
2. [Helm으로 설치 (권장)](#helm으로-설치-권장)
3. [Jsonnet/Tanka로 설치](#jsonnettanka로-설치)
4. [Puppet로 설치](#puppet로-설치)
5. [Docker로 설치](#docker로-설치)
6. [구성 파일 개요](#구성-파일-개요)
7. [핵심 구성 블록](#핵심-구성-블록)
8. [Limits 구성](#limits-구성)
9. [Runtime Config](#runtime-config)
10. [업그레이드](#업그레이드)

---

## 설치 방법 개요

| 방법 | 환경 | 권장 |
|------|------|------|
| **Helm** | Kubernetes | 권장 (표준) |
| **Jsonnet/Tanka** | Kubernetes (코드 기반) | 고급 사용자 |
| **Puppet** | VM/베어메탈 | 전통적 환경 |
| **Docker** | 단순 환경, 테스트 | 학습용 |
| **바이너리** | 커스텀 환경 | 드물게 사용 |

---

## Helm으로 설치 (권장)

### 사전 요구사항

- Kubernetes 1.25+
- Helm 3.8+
- 오브젝트 스토리지 (S3, GCS, Azure Blob, MinIO)

### 설치

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# 네임스페이스 생성
kubectl create namespace mimir

# 시크릿 생성 (S3 자격증명)
kubectl create secret generic mimir-bucket-secret \
  --from-literal=AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  --from-literal=AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -n mimir

# 설치
helm install mimir grafana/mimir-distributed \
  --namespace mimir \
  --values values.yaml
```

### values.yaml 최소 예시

```yaml
mimir:
  structuredConfig:
    multitenancy_enabled: true
    
    common:
      storage:
        backend: s3
        s3:
          endpoint: s3.amazonaws.com
          region: us-east-1
          bucket_name: my-mimir-bucket
          access_key_id: ${AWS_ACCESS_KEY_ID}
          secret_access_key: ${AWS_SECRET_ACCESS_KEY}
    
    blocks_storage:
      s3:
        bucket_name: my-mimir-blocks
    
    ruler_storage:
      s3:
        bucket_name: my-mimir-ruler
    
    alertmanager_storage:
      s3:
        bucket_name: my-mimir-alertmanager
    
    limits:
      ingestion_rate: 25_000          # samples/s per tenant
      ingestion_burst_size: 250_000
      max_global_series_per_user: 1_500_000

# 컴포넌트 복제본 (small/medium/large 프리셋 가능)
distributor:
  replicas: 3
  resources:
    requests:
      cpu: 1
      memory: 4Gi

ingester:
  replicas: 3
  zoneAwareReplication:
    enabled: true
  persistentVolume:
    size: 50Gi
    storageClass: gp3
  resources:
    requests:
      cpu: 4
      memory: 16Gi

querier:
  replicas: 3
  resources:
    requests:
      cpu: 2
      memory: 8Gi

query_frontend:
  replicas: 2

query_scheduler:
  replicas: 2

store_gateway:
  replicas: 3
  zoneAwareReplication:
    enabled: true
  persistentVolume:
    size: 50Gi

compactor:
  replicas: 1
  persistentVolume:
    size: 50Gi

ruler:
  replicas: 2

alertmanager:
  replicas: 3
  persistentVolume:
    size: 10Gi

memcached:
  enabled: true
chunks-cache:
  enabled: true
metadata-cache:
  enabled: true
results-cache:
  enabled: true
index-cache:
  enabled: true

minio:
  enabled: false   # 외부 스토리지 사용
```

### Zone Aware Replication

여러 가용 영역에 컴포넌트를 분산해 가용성 향상.

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

### 검증

```bash
kubectl -n mimir get pods
kubectl -n mimir port-forward svc/mimir-nginx 9009:80

curl http://localhost:9009/ready
```

---

## Jsonnet/Tanka로 설치

### 프로젝트 초기화

```bash
mkdir mimir-prod && cd mimir-prod
tk init --k8s=1.27
jb install github.com/grafana/mimir/operations/mimir@main
```

### main.jsonnet 예시

```jsonnet
local mimir = import 'mimir/mimir.libsonnet';

mimir {
  _config+:: {
    namespace: 'mimir',
    storage_backend: 's3',
    storage_s3_bucket_name: 'my-mimir-bucket',
    storage_s3_region: 'us-east-1',
    
    multi_zone_ingester_enabled: true,
    multi_zone_ingester_replicas: 3,
    
    ingester_replicas: 3,
    distributor_replicas: 3,
    querier_replicas: 3,
    query_frontend_replicas: 2,
    store_gateway_replicas: 3,
    compactor_replicas: 1,
    
    ruler_enabled: true,
    ruler_replicas: 2,
    
    alertmanager_enabled: true,
    alertmanager_replicas: 3,
    
    cache_chunks_backend: 'memcached',
    cache_metadata_backend: 'memcached',
    cache_results_backend: 'memcached',
    cache_index_backend: 'memcached',
  },
}
```

### 배포

```bash
tk apply environments/prod
```

---

## Puppet로 설치

```puppet
class { 'mimir':
  config_file_content => template('mimir/config.yaml.erb'),
  package_version     => '2.10.0',
  service_ensure      => 'running',
  service_enable      => true,
}
```

자세한 내용은 [grafana/puppet-mimir](https://github.com/grafana/puppet-mimir) 참조.

---

## Docker로 설치

### 단일 컨테이너

```bash
docker run -d \
  --name=mimir \
  -p 9009:9009 \
  -v $(pwd)/mimir.yaml:/etc/mimir.yaml \
  grafana/mimir:latest \
  -config.file=/etc/mimir.yaml
```

### Docker Compose

[03_get_started.md](./03_get_started.md#docker-compose로-시작) 참조.

---

## 구성 파일 개요

```yaml
multitenancy_enabled: true
target: all                        # 또는 distributor, ingester, ...

common:
  storage: { ... }                 # 공통 스토리지

server:
  http_listen_port: 9009
  grpc_listen_port: 9095

api:
  prometheus_http_prefix: /prometheus

distributor: { ... }
ingester: { ... }
querier: { ... }
query_frontend: { ... }
query_scheduler: { ... }
store_gateway: { ... }
compactor: { ... }
ruler: { ... }
alertmanager: { ... }

blocks_storage: { ... }
ruler_storage: { ... }
alertmanager_storage: { ... }

limits: { ... }
runtime_config: { ... }

memberlist: { ... }
```

---

## 핵심 구성 블록

### server

```yaml
server:
  http_listen_address: 0.0.0.0
  http_listen_port: 9009
  grpc_listen_address: 0.0.0.0
  grpc_listen_port: 9095
  
  grpc_server_max_recv_msg_size: 104857600
  grpc_server_max_send_msg_size: 104857600
  
  log_level: info
  log_format: logfmt
```

### distributor

```yaml
distributor:
  ring:
    kvstore:
      store: memberlist
  
  ha_tracker:
    enable_ha_tracker: true
    kvstore:
      store: consul
    ha_tracker_update_timeout: 15s
    ha_tracker_failover_timeout: 30s
  
  pool:
    health_check_ingesters: true
```

### ingester

```yaml
ingester:
  ring:
    kvstore:
      store: memberlist
    replication_factor: 3
    zone_awareness_enabled: true
    instance_availability_zone: us-east-1a
  
  instance_limits:
    max_ingestion_rate: 0
    max_series: 1500000
    max_tenants: 0
    max_inflight_push_requests: 30000

# 메모리 TSDB 설정
blocks_storage:
  tsdb:
    dir: /data/tsdb
    block_ranges_period: [2h0m0s]
    retention_period: 24h
    ship_interval: 1m
    ship_concurrency: 10
    head_compaction_interval: 1m
    head_compaction_concurrency: 5
```

### querier

```yaml
querier:
  query_store_after: 12h         # 12시간 이전 데이터는 store gateway
  max_concurrent: 16
  timeout: 2m
  
  store_gateway_client:
    grpc_compression: snappy
```

### query_frontend / query_scheduler

```yaml
query_frontend:
  scheduler_address: query-scheduler:9095
  log_queries_longer_than: 10s
  
  results_cache:
    backend: memcached
    memcached:
      addresses: dns+memcached-results:11211
  
  query_sharding_enabled: true
  query_sharding_total_shards: 16

query_scheduler:
  max_outstanding_requests_per_tenant: 100
```

### store_gateway

```yaml
store_gateway:
  sharding_ring:
    kvstore:
      store: memberlist
    replication_factor: 3
    zone_awareness_enabled: true
  
  bucket_store:
    sync_dir: /data/tsdb-sync
    sync_interval: 5m
    
    chunks_cache:
      backend: memcached
      memcached:
        addresses: dns+memcached-chunks:11211
    
    index_cache:
      backend: memcached
      memcached:
        addresses: dns+memcached-index:11211
```

### compactor

```yaml
compactor:
  data_dir: /data/compactor
  
  sharding_ring:
    kvstore:
      store: memberlist
  
  block_ranges: [2h, 12h, 24h]
  block_sync_concurrency: 8
  meta_sync_concurrency: 20
  
  consistency_delay: 0s
  retention_delete_delay: 12h
  
  cleanup_interval: 15m
```

### ruler

```yaml
ruler:
  ring:
    kvstore:
      store: memberlist
  
  rule_path: /data/rules
  alertmanager_url: http://alertmanager:8080/alertmanager
  
  evaluation_interval: 1m
  poll_interval: 1m
  
  enable_api: true
  enable_alertmanager_v2: true
  
  remote_write:
    enabled: false  # ruler 결과를 외부에 쓸 때
```

### alertmanager

```yaml
alertmanager:
  data_dir: /data/alertmanager
  
  external_url: https://alertmanager.example.com
  
  sharding_ring:
    kvstore:
      store: memberlist
    replication_factor: 3
  
  enable_api: true
  
  fallback_config_file: /etc/alertmanager/fallback.yaml
```

---

## Limits 구성

### 글로벌 한도

```yaml
limits:
  # 수집 한도
  ingestion_rate: 25000          # samples/s per tenant
  ingestion_burst_size: 250000
  
  # 시계열 한도
  max_global_series_per_user: 1_500_000
  max_global_series_per_metric: 50_000
  max_label_names_per_series: 30
  max_label_name_length: 1024
  max_label_value_length: 2048
  max_metadata_length: 1024
  
  # 쿼리 한도
  max_query_lookback: 0           # 0 = 보존 기간까지
  max_query_length: 12000h        # 약 500일
  max_query_parallelism: 14
  max_partial_query_length: 720h  # 30일
  max_samples_per_query: 50_000_000
  
  # 보존
  compactor_blocks_retention_period: 8760h  # 1년
  
  # Recording Rules
  ruler_max_rules_per_rule_group: 20
  ruler_max_rule_groups_per_tenant: 70
  
  # Alertmanager
  alertmanager_max_template_size_bytes: 0
  alertmanager_notification_rate_limit: 0
```

### 테넌트별 오버라이드

[Runtime Config](#runtime-config) 사용.

---

## Runtime Config

테넌트별 한도 동적 변경.

```yaml
runtime_config:
  file: /etc/mimir/runtime.yaml
  period: 10s
```

`runtime.yaml`:

```yaml
overrides:
  tenant-a:
    ingestion_rate: 100000
    max_global_series_per_user: 5_000_000
    compactor_blocks_retention_period: 4380h  # 6개월
    
    ruler_evaluation_delay_duration: 30s
    
    alertmanager_receivers_firewall_block_cidr_networks: ""
  
  tenant-b:
    ingestion_rate: 1000
    max_global_series_per_user: 100_000
    compactor_blocks_retention_period: 168h   # 7일
```

---

## 업그레이드

### 권장 순서 (Microservices)

1. **Compactor** (단일 인스턴스, 다운타임 OK)
2. **Store Gateway**
3. **Query Frontend / Scheduler**
4. **Querier**
5. **Distributor**
6. **Ingester** (가장 신중)
7. **Ruler**
8. **Alertmanager** (마지막)

### Ingester 업그레이드 주의

```yaml
ingester:
  ring:
    final_sleep: 30s
  
  blocks_storage:
    tsdb:
      ship_interval: 1m  # 자주 업로드해야 손실 최소
```

순차 재시작:
1. 한 번에 1개 (또는 1개 zone) 업그레이드
2. Ready 상태와 ring 안정화 대기
3. 다음 인스턴스로

### 메이저 버전 업그레이드

릴리스 노트의 Breaking Changes 섹션 필수 확인.

```bash
# Helm 업그레이드 예시
helm upgrade mimir grafana/mimir-distributed \
  --namespace mimir \
  --values values.yaml \
  --version 5.3.0
```
