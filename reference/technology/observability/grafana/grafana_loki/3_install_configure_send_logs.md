# Loki 설치, 구성, 로그 전송

## Loki 설치 및 설정

> 원본: https://grafana.com/docs/loki/latest/setup/install/

---

### 목차

1. [설치 방법 개요](#설치-방법-개요)
2. [Helm을 사용한 설치 (권장)](#helm을-사용한-설치-권장)
3. [Tanka를 사용한 설치](#tanka를-사용한-설치)
4. [Docker를 사용한 설치](#docker를-사용한-설치)
5. [로컬 바이너리 설치](#로컬-바이너리-설치)
6. [소스에서 빌드](#소스에서-빌드)
7. [업그레이드 가이드](#업그레이드-가이드)
8. [마이그레이션](#마이그레이션)

---

### 설치 방법 개요

Loki는 다음 5가지 방법으로 설치할 수 있습니다.

| 방법 | 권장 환경 | 난이도 |
|------|----------|--------|
| **Helm** | 프로덕션 (Kubernetes) | 중간 (권장) |
| **Tanka** | 프로덕션 (Kubernetes, Jsonnet) | 중상 |
| **Docker / Docker Compose** | 개발, 테스트, 소규모 | 쉬움 |
| **로컬 바이너리** | 개발, 학습 | 쉬움 |
| **소스 빌드** | 개발 기여 | 상 |

#### 일반 설치 흐름

1. **Loki 다운로드 및 설치**
2. **구성 파일 준비** (배포 모드, 스토리지, 보존 등)
3. **(보안) 인증/리버스 프록시 설정** — Loki는 자체 인증을 제공하지 않음
4. **Loki 시작**
5. **Alloy(또는 다른 클라이언트) 설치 및 구성**
6. **Alloy 시작**
7. **Grafana에 Loki 데이터 소스 추가**

> **보안 경고**: Loki는 인증 기능이 내장되어 있지 않습니다. nginx, Caddy 같은 리버스 프록시를 앞단에 배치하여 보안을 확보하거나, Grafana Cloud Loki를 사용해야 합니다.

---

### Helm을 사용한 설치 (권장)

#### 사전 요구사항

- Kubernetes 클러스터 (1.20 이상)
- Helm 3
- 오브젝트 스토리지 (S3, GCS, Azure Blob 등)

#### Helm 차트 종류

| 차트 | 용도 |
|------|------|
| `loki` | Simple Scalable Deployment (SSD) — 기본값 |
| `loki-distributed` | Microservices 모드 |
| `loki-stack` | Loki + Promtail + Grafana 통합 (Deprecated) |

#### 설치 단계

```bash
# 1. Helm 저장소 추가
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# 2. values.yaml 작성 (예시는 아래)

# 3. 네임스페이스 생성
kubectl create namespace loki

# 4. 설치
helm install loki grafana/loki \
  --namespace loki \
  --values values.yaml
```

#### values.yaml 최소 예시 (S3 사용)

```yaml
loki:
  schemaConfig:
    configs:
      - from: "2024-01-01"
        store: tsdb
        object_store: s3
        schema: v13
        index:
          prefix: loki_index_
          period: 24h
  storage:
    type: s3
    s3:
      endpoint: s3.amazonaws.com
      region: us-east-1
      bucketnames: my-loki-bucket
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}

deploymentMode: SimpleScalable

read:
  replicas: 3
write:
  replicas: 3
backend:
  replicas: 3

gateway:
  enabled: true
  replicas: 2
```

#### 검증

```bash
# Pod 상태 확인
kubectl -n loki get pods

# 게이트웨이 포트 포워딩
kubectl -n loki port-forward svc/loki-gateway 3100:80

# 헬스체크
curl http://localhost:3100/ready
```

---

### Tanka를 사용한 설치

#### 사전 요구사항

- Kubernetes 클러스터
- [Tanka](https://tanka.dev/) 및 jsonnet-bundler

#### 설치 단계

```bash
# 1. Tanka 프로젝트 초기화
mkdir loki-prod && cd loki-prod
tk init --k8s=1.27

# 2. Loki Jsonnet 라이브러리 설치
jb install github.com/grafana/loki/production/ksonnet/loki

# 3. 환경 디렉토리에서 main.jsonnet 작성
```

#### main.jsonnet 예시

```jsonnet
local loki = import 'loki/loki.libsonnet';

loki {
  _config+:: {
    namespace: 'loki',
    htpasswd_contents: 'user:$apr1$...',  // basic auth
    storage_backend: 's3',
    s3_bucket_name: 'my-loki-bucket',
    s3_access_key_id: '...',
    s3_secret_access_key: '...',
  },
}
```

#### 배포

```bash
tk apply environments/prod
```

---

### Docker를 사용한 설치

#### 단일 컨테이너

```bash
# 구성 파일 다운로드
wget https://raw.githubusercontent.com/grafana/loki/main/cmd/loki/loki-local-config.yaml -O loki-config.yaml

# 컨테이너 실행
docker run -d \
  --name=loki \
  -p 3100:3100 \
  -v $(pwd)/loki-config.yaml:/etc/loki/local-config.yaml \
  grafana/loki:latest \
  -config.file=/etc/loki/local-config.yaml
```

#### Docker Compose 예시

```yaml
version: "3"
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yaml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml

  alloy:
    image: grafana/alloy:latest
    volumes:
      - ./alloy-config.alloy:/etc/alloy/config.alloy
      - /var/log:/var/log:ro
    command: run --server.http.listen-addr=0.0.0.0:12345 /etc/alloy/config.alloy
    ports:
      - "12345:12345"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin

volumes:
  loki-data:
```

```bash
docker compose up -d
```

---

### 로컬 바이너리 설치

#### 다운로드

[GitHub Releases](https://github.com/grafana/loki/releases)에서 OS/아키텍처에 맞는 바이너리 다운로드.

```bash
# Linux x86_64 예시
curl -O -L "https://github.com/grafana/loki/releases/download/v3.0.0/loki-linux-amd64.zip"
unzip loki-linux-amd64.zip
chmod +x loki-linux-amd64
mv loki-linux-amd64 /usr/local/bin/loki
```

#### 구성 파일 다운로드

```bash
wget https://raw.githubusercontent.com/grafana/loki/main/cmd/loki/loki-local-config.yaml
```

#### 실행

```bash
loki -config.file=loki-local-config.yaml
```

기본 포트는 3100입니다.

#### systemd 서비스 등록 (Linux)

```ini
# /etc/systemd/system/loki.service
[Unit]
Description=Grafana Loki
After=network.target

[Service]
ExecStart=/usr/local/bin/loki -config.file=/etc/loki/loki.yaml
Restart=on-failure
User=loki
Group=loki

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now loki
```

---

### 소스에서 빌드

#### 사전 요구사항

- Go 1.21+
- Make
- Git

#### 빌드

```bash
git clone https://github.com/grafana/loki.git
cd loki
make loki

# 결과: ./cmd/loki/loki
./cmd/loki/loki -config.file=cmd/loki/loki-local-config.yaml
```

#### Promtail / 기타 빌드

```bash
make promtail
make logcli
```

---

### 업그레이드 가이드

#### 버전 호환성 정책

Loki는 시맨틱 버전을 따릅니다.

- **Major (X.0.0)**: 호환성 깨질 수 있음
- **Minor (x.Y.0)**: 하위 호환 유지
- **Patch (x.y.Z)**: 버그 수정만

#### 업그레이드 순서

1. **릴리스 노트 확인** — Breaking changes 확인
2. **백업** — 구성 파일과 가능한 경우 데이터
3. **단계적 업그레이드** — 한 번에 한 마이너 버전씩 권장
4. **순서**:
   - Ingester → Querier 및 나머지 컴포넌트 순으로 권장 (Ingester가 먼저 새 API를 노출해야 Querier가 의존할 수 있음)
   - Microservices 모드의 경우 컴포넌트 의존 관계 고려

#### 주요 업그레이드 시 체크포인트

- **2.x → 3.0**: BoltDB Shipper 폐기, TSDB로 마이그레이션 필요
- **schema_config**: 새 스키마는 미래 시점부터 적용 (`from` 날짜)

---

### 마이그레이션

#### 다른 로깅 시스템에서 Loki로

##### Elasticsearch / OpenSearch에서

- 기존 로그를 백필(backfill)하려면 OTel Collector + Loki Exporter를 활용
- 신규 로그부터 Loki로 전송하고 점진적으로 전환

##### Splunk에서

- Splunk Forwarder를 OTel Collector로 교체
- Splunk to Loki bridge 도구 활용

#### Promtail에서 Alloy로

Promtail은 점진적으로 Alloy로 통합되고 있습니다. 마이그레이션 방법:

```bash
# Promtail 구성을 Alloy로 변환
alloy convert --source-format=promtail \
  --output=alloy-config.alloy \
  promtail-config.yaml
```

#### 스토리지 백엔드 변경

스토리지를 변경할 때는 `schema_config`에 새 스키마를 추가합니다. 기존 데이터는 그대로 유지됩니다.

```yaml
schema_config:
  configs:
    # 기존 설정 (유지)
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h
    # 새 설정 (특정 날짜부터)
    - from: 2024-06-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
```

---

## Loki 구성 레퍼런스

> 원본: https://grafana.com/docs/loki/latest/configure/

---

### 목차

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

### 구성 개요

Loki는 YAML 형식의 구성 파일로 동작합니다. 명령줄 플래그로도 모든 옵션을 설정할 수 있으나, 일반적으로 `-config.file=loki.yaml`을 사용하는 방식이 권장됩니다.

```bash
loki -config.file=/etc/loki/loki.yaml
```

#### 주요 구성 블록 한눈에 보기

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

### 최상위 설정

#### `target`

실행할 컴포넌트를 지정합니다.

```yaml
target: all  # Monolithic
# 또는
target: write,read,backend  # SSD
# 또는
target: distributor  # 단일 컴포넌트
```

#### `auth_enabled`

`true`로 설정하면 멀티 테넌시가 활성화되며, 모든 요청에 `X-Scope-OrgID` 헤더가 필요합니다. `false`이면 테넌트 ID가 `fake`로 고정됩니다.

```yaml
auth_enabled: true
```

#### `ballast_bytes`

GC 최적화를 위해 예약하는 가상 메모리 크기. 일반적으로 사용 가능한 메모리의 10~20%로 설정합니다.

```yaml
ballast_bytes: 1073741824  # 1GB
```

---

### server

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

### distributor

```yaml
distributor:
  ring:
    kvstore:
      store: memberlist  # 또는 consul, etcd
  
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

### ingester

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

### querier

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

### query_scheduler / frontend / query_range

#### query_scheduler

```yaml
query_scheduler:
  max_outstanding_requests_per_tenant: 100
  scheduler_ring:
    kvstore:
      store: memberlist
  
  # 큐 설정
  use_scheduler_ring: true
```

#### frontend

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

#### query_range

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

### storage_config

스토리지 백엔드를 설정합니다.

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

### schema_config

청크 인덱스 스키마와 저장 위치를 정의합니다. **여러 시기의 스키마를 누적하여 정의**할 수 있습니다.

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

#### 스키마 버전

| 버전 | 설명 | 권장 |
|------|------|------|
| v9-v11 | 구식 인덱스 형식 | X |
| v12 | BoltDB Shipper | X (Deprecated) |
| v13 | TSDB | O (현재 권장) |

#### 스토어 종류

- `tsdb` (권장)
- `boltdb-shipper` (Deprecated)
- `cassandra`, `bigtable`, `dynamodb` (구식)

---

### chunk_store_config

청크 캐싱 및 보존 기간을 설정합니다.

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

### limits_config

전역 및 테넌트별 한도를 설정합니다. **runtime_config**를 통해 테넌트별로 오버라이드할 수 있습니다.

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

### compactor

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

### ruler

알림 룰과 Recording 룰을 평가합니다.

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

#### 룰 파일 예시

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

### memberlist

Hashicorp memberlist 기반의 클러스터 멤버십을 설정합니다.

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

### runtime_config

테넌트별 한도 및 설정을 동적으로 오버라이드합니다.

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

### tracing / analytics

#### tracing

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

#### analytics

```yaml
analytics:
  reporting_enabled: false  # Grafana Labs로 사용 통계 전송 비활성화
```

---

## Loki로 로그 전송

> 원본: https://grafana.com/docs/loki/latest/send-data/

---

### 목차

1. [개요](#개요)
2. [Grafana Alloy (권장)](#grafana-alloy-권장)
3. [Promtail (Deprecated)](#promtail-deprecated)
4. [OpenTelemetry Collector](#opentelemetry-collector)
5. [Docker Driver](#docker-driver)
6. [Fluentd](#fluentd)
7. [Fluent Bit](#fluent-bit)
8. [Logstash](#logstash)
9. [Vector](#vector)
10. [언어/프레임워크 클라이언트](#언어프레임워크-클라이언트)
11. [Loki HTTP Push API](#loki-http-push-api)

---

### 개요

Loki는 다양한 클라이언트로부터 로그를 수신할 수 있습니다. 모두 **HTTP 기반 Push 방식**으로 데이터를 전송합니다.

#### 주요 클라이언트 비교

| 클라이언트 | 용도 | 권장도 |
|----------|------|--------|
| **Grafana Alloy** | 통합 텔레메트리 (메트릭, 로그, 트레이스) | 강력 권장 |
| **OpenTelemetry Collector** | OTel 표준 환경 | 권장 |
| **Promtail** | 기존 사용자 | Deprecated → Alloy로 이전 |
| **Docker Driver** | Docker 환경 단순 통합 | Docker 환경 |
| **Fluent Bit** | Kubernetes, 가벼운 수집기 | 좋음 |
| **Fluentd** | 기존 Fluentd 사용자 | 좋음 |
| **Logstash** | Elastic Stack 전환 | 보조 |
| **Vector** | 고성능 라우팅/처리 | 좋음 |

---

### Grafana Alloy (권장)

Alloy는 OpenTelemetry Collector 배포판으로, 메트릭/로그/트레이스를 통합 수집합니다.

#### 파일 로그 수집 예시

```alloy
// 디스커버리: 파일 패턴
local.file_match "logs" {
  path_targets = [
    {__path__ = "/var/log/*.log"},
  ]
}

// 소스: 파일 읽기
loki.source.file "logs" {
  targets    = local.file_match.logs.targets
  forward_to = [loki.process.add_labels.receiver]
}

// 처리: 라벨 추가
loki.process "add_labels" {
  forward_to = [loki.write.default.receiver]
  
  stage.static_labels {
    values = {
      environment = "production",
      cluster     = "us-east-1",
    }
  }
}

// 전송
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

#### Kubernetes Pod 로그 수집

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.relabel "pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_name"]
    target_label  = "pod"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_container_name"]
    target_label  = "container"
  }
}

loki.source.kubernetes "pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

#### Journal (systemd) 로그

```alloy
loki.source.journal "journal" {
  forward_to = [loki.write.default.receiver]
  labels = {
    job = "systemd-journal",
  }
}
```

#### Syslog 수신

```alloy
loki.source.syslog "syslog" {
  listener {
    address = "0.0.0.0:1514"
    protocol = "tcp"
  }
  forward_to = [loki.write.default.receiver]
}
```

---

### Promtail (Deprecated)

Promtail은 Loki 전용 로그 수집기로 Grafana Agent의 일부였습니다. 현재는 **Alloy로 통합** 되어 향후 폐기 예정입니다.

#### Promtail 구성 예시

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: varlogs
          __path__: /var/log/*.log
    
    pipeline_stages:
      - regex:
          expression: '^(?P<timestamp>\S+) (?P<level>\S+) (?P<msg>.*)$'
      - labels:
          level:
      - timestamp:
          source: timestamp
          format: RFC3339
```

#### Alloy로 변환

```bash
alloy convert --source-format=promtail \
  -o alloy-config.alloy \
  promtail-config.yaml
```

---

### OpenTelemetry Collector

Loki는 **OTLP HTTP** 로그 수신을 네이티브 지원합니다.

#### Loki 설정

```yaml
distributor:
  otlp_config:
    default_resource_attributes_as_index_labels:
      - service.name
      - service.namespace
      - deployment.environment
```

#### OTel Collector 구성

```yaml
receivers:
  filelog:
    include: ["/var/log/*.log"]

processors:
  batch:
    timeout: 5s

exporters:
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
    headers:
      X-Scope-OrgID: tenant-1

service:
  pipelines:
    logs:
      receivers: [filelog]
      processors: [batch]
      exporters: [otlphttp/loki]
```

#### OTel 속성 → Loki 라벨 매핑

OTel Resource Attributes의 마침표는 언더스코어로 변환됩니다.

| OTel Attribute | Loki Label |
|----------------|-----------|
| `service.name` | `service_name` |
| `service.namespace` | `service_namespace` |
| `deployment.environment` | `deployment_environment` |
| `k8s.namespace.name` | `k8s_namespace_name` |

---

### Docker Driver

Docker 컨테이너의 stdout/stderr을 자동으로 Loki로 전송합니다.

#### 설치

```bash
docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
```

#### 컨테이너 단위 사용

```bash
docker run --log-driver=loki \
  --log-opt loki-url="http://loki:3100/loki/api/v1/push" \
  --log-opt loki-retries=5 \
  --log-opt loki-batch-size=400 \
  nginx
```

#### 데몬 기본값으로 설정

```json
// /etc/docker/daemon.json
{
  "log-driver": "loki",
  "log-opts": {
    "loki-url": "http://loki:3100/loki/api/v1/push",
    "loki-batch-size": "400"
  }
}
```

```bash
systemctl restart docker
```

#### 자동 추가 라벨

- `container_name`
- `compose_project`, `compose_service`
- `swarm_stack`, `swarm_service`
- `host`

---

### Fluentd

`fluent-plugin-grafana-loki` 사용.

#### 설치

```bash
gem install fluent-plugin-grafana-loki
```

#### 구성 예시

```xml
<match **>
  @type loki
  url "http://loki:3100"
  extra_labels {"env":"production"}
  
  <label>
    fluentd_worker
  </label>
  
  flush_interval 10s
  flush_at_shutdown true
  buffer_chunk_limit 1m
</match>
```

---

### Fluent Bit

Loki는 Fluent Bit의 공식 출력 플러그인을 제공합니다.

#### 구성 예시

```ini
[INPUT]
    Name              tail
    Path              /var/log/*.log
    Parser            docker
    Tag               kube.*
    Refresh_Interval  5

[OUTPUT]
    Name        loki
    Match       *
    Host        loki
    Port        3100
    Labels      job=fluentbit, env=prod
    Label_keys  $level,$app
    Auto_Kubernetes_Labels  on
    Tenant_ID   tenant-1
```

#### Helm으로 Kubernetes 배포

```yaml
# values.yaml
config:
  outputs: |
    [OUTPUT]
        Name loki
        Match *
        Host loki-gateway.loki.svc.cluster.local
        Port 80
        Labels job=fluentbit
```

---

### Logstash

`logstash-output-loki` 플러그인 사용.

#### 설치

```bash
bin/logstash-plugin install logstash-output-loki
```

#### 구성

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => { "message" => "%{COMBINEDAPACHELOG}" }
  }
}

output {
  loki {
    url => "http://loki:3100/loki/api/v1/push"
    username => "user"
    password => "pass"
    batch_size => 112640
    retries => 5
    
    # 라벨 매핑
    tenant_id => "tenant-1"
  }
}
```

---

### Vector

`loki` 싱크 사용.

#### 구성 (TOML)

```toml
[sources.app_logs]
type = "file"
include = ["/var/log/app/*.log"]

[transforms.parse]
type = "remap"
inputs = ["app_logs"]
source = '''
  . = parse_json!(.message)
'''

[sinks.loki]
type = "loki"
inputs = ["parse"]
endpoint = "http://loki:3100"
encoding.codec = "json"

  [sinks.loki.labels]
  app = "myapp"
  env = "production"
  level = "{{ level }}"
```

---

### 언어/프레임워크 클라이언트

Loki Push API로 직접 로그를 전송하는 라이브러리들입니다.

#### Java (Logback)

`com.github.loki4j:loki-logback-appender`

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
  <http>
    <url>http://loki:3100/loki/api/v1/push</url>
  </http>
  <format>
    <label>
      <pattern>app=myapp,host=${HOSTNAME},level=%level</pattern>
    </label>
    <message>
      <pattern>%msg %ex</pattern>
    </message>
  </format>
</appender>
```

#### Python

`python-logging-loki`:

```python
import logging
import logging_loki

handler = logging_loki.LokiHandler(
    url="http://loki:3100/loki/api/v1/push",
    tags={"app": "myapp"},
    auth=("user", "pass"),
    version="1",
)

logger = logging.getLogger("my-app")
logger.addHandler(handler)
logger.error("error occurred")
```

#### Go

`github.com/grafana/loki/pkg/promtail/client` 또는 `github.com/grafana/loki-client-go`

```go
client, _ := loki.New(loki.Config{
    URL: "http://loki:3100/loki/api/v1/push",
})

client.Handle(model.LabelSet{"app": "myapp"}, time.Now(), "log message")
```

#### Node.js

`winston-loki`:

```js
const winston = require("winston");
const LokiTransport = require("winston-loki");

const logger = winston.createLogger({
  transports: [
    new LokiTransport({
      host: "http://loki:3100",
      labels: { app: "myapp" },
      json: true,
    }),
  ],
});

logger.info("hello");
```

---

### Loki HTTP Push API

직접 API를 호출할 수도 있습니다.

#### 엔드포인트

```
POST /loki/api/v1/push
```

#### 요청 형식 (JSON)

```bash
curl -H "Content-Type: application/json" \
     -H "X-Scope-OrgID: tenant-1" \
     -XPOST "http://loki:3100/loki/api/v1/push" \
     --data-raw '{
       "streams": [
         {
           "stream": {"app": "myapp", "level": "info"},
           "values": [
             ["1700000000000000000", "log line 1"],
             ["1700000001000000000", "log line 2"]
           ]
         }
       ]
     }'
```

#### 요청 형식 (Protobuf, Snappy 압축)

기본(default) 형식은 Protobuf + Snappy입니다.

- `Content-Type: application/x-protobuf`
- 페이로드: Snappy 압축된 Protobuf

#### 응답

| 상태 코드 | 의미 |
|---------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 429 | Rate Limit 초과 |
| 500 | 서버 에러 |

#### 구조화된 메타데이터 (Structured Metadata)

Loki 3.0부터 지원. 라벨로 인덱싱하지 않지만 쿼리에 사용 가능한 메타데이터.

```json
{
  "streams": [
    {
      "stream": {"app": "api"},
      "values": [
        ["1700000000000000000", "log line", {"trace_id": "abc123", "user_id": "u-123"}]
      ]
    }
  ]
}
```
