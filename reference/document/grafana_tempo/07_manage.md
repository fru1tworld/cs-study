# Tempo 운영 관리

> 이 문서는 Grafana Tempo 공식 문서의 "Manage Tempo" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/operations/

---

## 목차

1. [멀티 테넌시](#멀티-테넌시)
2. [수집(Ingestion) 한도](#수집ingestion-한도)
3. [스토리지 관리](#스토리지-관리)
4. [보존(Retention)](#보존retention)
5. [Compactor 운영](#compactor-운영)
6. [캐싱](#캐싱)
7. [모니터링](#모니터링)
8. [업그레이드 운영](#업그레이드-운영)
9. [장애 복구](#장애-복구)

---

## 멀티 테넌시

### 활성화

```yaml
multitenancy_enabled: true
```

활성화되면 모든 API 요청에 `X-Scope-OrgID` 헤더 필수.

### 단일 테넌트 모드

```yaml
multitenancy_enabled: false
```

모든 데이터는 `single-tenant` ID로 저장.

### 테넌트별 한도 (overrides.yaml)

```yaml
overrides:
  tenant-a:
    ingestion_rate_limit_bytes: 50_000_000  # 50MB/s
    ingestion_burst_size_bytes: 100_000_000
    max_traces_per_user: 100_000
    max_bytes_per_trace: 5_000_000
    block_retention: 720h  # 30일
    
  tenant-b:
    ingestion_rate_limit_bytes: 5_000_000
    max_traces_per_user: 10_000
    block_retention: 168h  # 7일
```

---

## 수집(Ingestion) 한도

### 글로벌 한도

```yaml
ingester:
  max_block_bytes: 524288000   # 500MB
  max_block_duration: 30m      # 블록 최대 시간
  trace_idle_period: 10s       # idle 트레이스 처리 시간
  
  # 트레이스당 한도
  max_traces_per_user: 10_000
  max_bytes_per_trace: 5_000_000  # 5MB

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          max_recv_msg_size_mib: 4
```

### 테넌트별 Rate Limit

```yaml
overrides:
  defaults:
    ingestion_rate_limit_bytes: 15_000_000     # 15MB/s
    ingestion_burst_size_bytes: 20_000_000     # 20MB 버스트
    max_global_traces_per_user: 100_000
```

### Rate Limit 동작

- **Local**: Distributor 인스턴스별 적용
- **Global**: 모든 Distributor가 협력

```yaml
distributor:
  rate_limit_strategy: global
```

---

## 스토리지 관리

### 백엔드 옵션

| 백엔드 | 권장 사용 |
|--------|----------|
| AWS S3 | 가장 일반적 |
| GCS | GCP 환경 |
| Azure Blob | Azure 환경 |
| Filesystem | 개발/테스트 |

### S3 구성

```yaml
storage:
  trace:
    backend: s3
    s3:
      bucket: my-tempo-bucket
      endpoint: s3.amazonaws.com
      region: us-east-1
      access_key: ${AWS_ACCESS_KEY_ID}
      secret_key: ${AWS_SECRET_ACCESS_KEY}
      forcepathstyle: false
    
    pool:
      max_workers: 100
      queue_depth: 10000
    
    block:
      version: vParquet4
      v2_encoding: zstd
      v2_index_downsample_bytes: 1000
      v2_index_page_size_bytes: 250000
      bloom_filter_false_positive: 0.01
      bloom_filter_shard_size_bytes: 100000
    
    wal:
      path: /var/tempo/wal
      encoding: snappy
```

### Bucket Index

각 테넌트의 블록 목록을 캐싱하여 빠른 조회.

```yaml
storage:
  trace:
    blocklist_poll: 5m            # 블록 목록 폴링 주기
    blocklist_poll_concurrency: 50
    blocklist_poll_jitter_ms: 1000
```

---

## 보존(Retention)

### 글로벌 보존 기간

```yaml
compactor:
  compaction:
    block_retention: 720h        # 30일
    compacted_block_retention: 1h
```

### 테넌트별 보존

```yaml
overrides:
  tenant-a:
    block_retention: 8760h       # 1년
  tenant-b:
    block_retention: 168h        # 7일
```

### 보존 동작

1. Compactor가 주기적으로 블록 메타데이터 검사
2. `block_retention` 지난 블록을 삭제 마킹
3. 일정 시간 후 실제 삭제

---

## Compactor 운영

### 기본 설정

```yaml
compactor:
  ring:
    kvstore:
      store: memberlist
  
  compaction:
    chunk_size_bytes: 5_000_000
    flush_size_bytes: 30_000_000
    
    # 압축 윈도우
    compaction_window: 1h
    
    # 동시 압축
    max_compaction_objects: 1_000_000
    max_block_bytes: 100_000_000_000  # 100GB
    
    # 보존
    block_retention: 720h
    compacted_block_retention: 1h
    
    # 재시도
    retry_delay: 1m
    max_time_per_tenant: 5m
    compaction_cycle: 30s
```

### 압축 단계

```
[작은 블록 N개] → [Compactor] → [큰 블록 1개]
```

레벨별 압축:
- Level 0: 1시간 블록
- Level 1: 4시간 블록
- Level 2: 24시간 블록

### 분산 Compactor

```yaml
compactor:
  ring:
    kvstore:
      store: memberlist
    
    instance_addr: ${POD_IP}
    instance_id: ${POD_NAME}
```

여러 Compactor 인스턴스가 테넌트별로 작업을 분산.

### 모니터링

| 메트릭 | 설명 |
|--------|------|
| `tempodb_compaction_blocks_total` | 압축된 블록 수 |
| `tempodb_compaction_bytes_written_total` | 압축으로 쓰인 바이트 |
| `tempodb_compaction_objects_combined_total` | 결합된 오브젝트 수 |
| `tempodb_retention_marked_for_deletion_total` | 삭제 마킹된 블록 수 |
| `tempodb_retention_deleted_total` | 실제 삭제된 블록 수 |

---

## 캐싱

### 캐시 종류

| 캐시 | 대상 |
|------|------|
| Bloom Filter | 빠른 트레이스 ID 조회 |
| Index | 인덱스 조회 |
| Search | TraceQL 결과 |
| Frontend | 쿼리 결과 |

### Memcached 설정

```yaml
cache:
  background:
    writeback_goroutines: 10
  
  caches:
    - roles:
        - bloom
        - parquet-page
        - frontend-search
      memcached:
        host: memcached
        service: memcached-client
        consistent_hash: true
        timeout: 200ms
        max_idle_conns: 16
```

### Redis 설정

```yaml
cache:
  caches:
    - roles:
        - bloom
      redis:
        endpoint: redis:6379
        timeout: 100ms
        expiration: 24h
```

---

## 모니터링

### 메트릭 노출

`/metrics` 엔드포인트 (기본 포트 3200).

### 핵심 메트릭

#### Distributor
- `tempo_distributor_spans_received_total`
- `tempo_distributor_bytes_received_total`
- `tempo_distributor_metric_received_spans_total`

#### Ingester
- `tempo_ingester_traces_created_total`
- `tempo_ingester_blocks_flushed_total`
- `tempo_ingester_live_traces`

#### Querier
- `tempo_query_frontend_queries_within_slo_total`
- `tempo_request_duration_seconds`

#### Compactor
- `tempodb_compaction_blocks_total`
- `tempodb_compaction_errors_total`

### Mixin

[Tempo Mixin](https://github.com/grafana/tempo/tree/main/operations/tempo-mixin)으로 대시보드/알림 일괄 배포.

```bash
jb install github.com/grafana/tempo/operations/tempo-mixin@main
jsonnet -J vendor mixin.libsonnet | gojsontoyaml > mixin-prom-rules.yaml
```

### SLO

권장 SLO:
- Ingestion 가용성: > 99.9%
- 쿼리 가용성: > 99.5%
- 쿼리 지연: P99 < 30초

---

## 업그레이드 운영

### 권장 순서 (Microservices)

1. Compactor (단일 또는 소수)
2. Query Frontend
3. Querier
4. Distributor
5. Ingester (가장 신중)
6. Metrics Generator

### Ingester 업그레이드 주의

WAL 활성화 필수:

```yaml
ingester:
  trace_idle_period: 10s
  max_block_duration: 5m
  
storage:
  trace:
    wal:
      path: /var/tempo/wal
```

순차 재시작:
1. 한 번에 1개 ingester만
2. Ready 상태 확인 후 다음 진행
3. `complete_block_timeout` 동안 대기

### Parquet 버전 업그레이드

```yaml
storage:
  trace:
    block:
      version: vParquet4
```

기존 vParquet3 블록은 그대로 유지. Compactor가 점진적으로 새 버전으로 변환.

---

## 장애 복구

### Ingester 장애

WAL 활성화 시 자동 복구:

```yaml
storage:
  trace:
    wal:
      path: /var/tempo/wal
      encoding: snappy
      ingestion_slack: 2m
```

재시작 시 WAL 리플레이로 메모리 복구.

### 오브젝트 스토리지 장애

- Ingester는 메모리 + WAL에 데이터 보유 (재시도)
- Querier는 캐시된 데이터로 일부 응답 가능
- 복구 후 자동 재시도

### 데이터 손상 복구

```bash
# 손상된 블록 식별
tempo-cli list blocks --backend=s3 --bucket=my-tempo

# 특정 블록 검사
tempo-cli view-block <block-id>

# 블록 삭제 (마지막 수단)
tempo-cli delete-block <block-id>
```

### 백업 전략

오브젝트 스토리지 자체 백업:
- S3: Cross-Region Replication, Versioning
- GCS: Object Versioning, Multi-region buckets
- Azure: Geo-Redundant Storage

WAL 디스크는 일시적이므로 백업 불필요.
