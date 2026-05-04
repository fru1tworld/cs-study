# Loki 운영 관리

> 이 문서는 Grafana Loki 공식 문서의 "Manage Loki" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/operations/

---

## 목차

1. [멀티 테넌시](#멀티-테넌시)
2. [수집(Ingestion) 관리](#수집ingestion-관리)
3. [스토리지 관리](#스토리지-관리)
4. [보존(Retention)](#보존retention)
5. [로그 삭제](#로그-삭제)
6. [쿼리 성능 관리](#쿼리-성능-관리)
7. [캐싱](#캐싱)
8. [WAL (Write-Ahead Log)](#wal-write-ahead-log)
9. [업그레이드 운영](#업그레이드-운영)
10. [모니터링](#모니터링)

---

## 멀티 테넌시

### 활성화

```yaml
auth_enabled: true
```

활성화 시 모든 API 요청에 `X-Scope-OrgID` 헤더 필수.

### 테넌트 ID 명명 규칙

- 영문 대소문자, 숫자, 일부 특수문자 가능
- 슬래시 `/`, 빈 문자열, `..`, 디렉토리 트래버설 문자 금지
- 테넌트별로 디렉토리/접두사 분리 저장

### 테넌트 페더레이션

`limits_config`에서 활성화하면 여러 테넌트를 한 번에 쿼리 가능.

```yaml
limits_config:
  multi_tenant_queries_enabled: true
```

쿼리:

```
GET /loki/api/v1/query
Headers:
  X-Scope-OrgID: tenant-a|tenant-b
```

`|` 문자로 여러 테넌트 결합.

### 테넌트별 한도 오버라이드

`runtime_config`로 동적 조정.

```yaml
overrides:
  tenant-a:
    ingestion_rate_mb: 20
    max_streams_per_user: 100000
    retention_period: 2160h  # 90일
  
  tenant-b:
    ingestion_rate_mb: 1
    max_streams_per_user: 1000
    retention_period: 168h   # 7일
```

---

## 수집(Ingestion) 관리

### Rate Limiting 전략

#### Local

각 Distributor 인스턴스가 독립적으로 한도 적용. Distributor 수에 따라 실제 한도가 변동.

```yaml
limits_config:
  ingestion_rate_strategy: local
  ingestion_rate_mb: 4
```

#### Global (권장)

모든 Distributor가 협력하여 테넌트당 글로벌 한도 적용.

```yaml
limits_config:
  ingestion_rate_strategy: global
  ingestion_rate_mb: 100  # 모든 Distributor 합산 100MB/s
```

### 스트림 수 한도

```yaml
limits_config:
  max_streams_per_user: 10000
  max_global_streams_per_user: 5000
```

### 라인 크기 한도

```yaml
limits_config:
  max_line_size: 256000  # 256KB
  max_line_size_truncate: false  # true면 잘라서 수용
```

### 카디널리티 보호

```yaml
limits_config:
  max_label_name_length: 1024
  max_label_value_length: 4096
  max_label_names_per_series: 30
  cardinality_limit: 100000
```

### 시간 검증

```yaml
limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h  # 1주일 이전 거부
  creation_grace_period: 10m         # 10분 미래까지 허용
```

### 구조화된 메타데이터 한도

```yaml
limits_config:
  allow_structured_metadata: true
  max_structured_metadata_size: 64KB
  max_structured_metadata_entries_count: 128
```

---

## 스토리지 관리

### 권장 백엔드

| 환경 | 백엔드 |
|------|--------|
| AWS | S3 |
| GCP | GCS |
| Azure | Blob Storage |
| 온프레미스 | MinIO, Ceph S3 호환 |
| 개발 | Filesystem |

### 스키마 마이그레이션

새 스키마 적용은 **미래 시점부터** 추가:

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    - from: 2024-06-01     # 미래 날짜 (시간 여유 두고)
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
```

### 인덱스 게이트웨이 (Index Gateway)

Querier들이 인덱스를 매번 다운로드하지 않도록 중앙화.

```yaml
storage_config:
  tsdb_shipper:
    index_gateway_client:
      server_address: dns:///index-gateway:9095

index_gateway:
  mode: ring
  ring:
    kvstore:
      store: memberlist
```

---

## 보존(Retention)

### Compactor 기반 보존 (권장)

```yaml
compactor:
  working_directory: /loki/compactor
  shared_store: s3
  retention_enabled: true
  retention_delete_delay: 2h
  retention_delete_worker_count: 150
  compaction_interval: 10m

limits_config:
  retention_period: 744h  # 31일 (글로벌)
  
  # 스트림별 세분화 보존
  retention_stream:
    - selector: '{namespace="dev"}'
      priority: 1
      period: 168h    # dev: 7일
    - selector: '{namespace="prod", level="debug"}'
      priority: 2
      period: 72h     # prod debug: 3일
    - selector: '{namespace="prod"}'
      priority: 3
      period: 8760h   # prod: 1년
```

### 우선순위 규칙

`priority` 값이 **클수록** 우선 적용. 매칭되는 규칙이 없으면 글로벌 `retention_period` 적용.

### 테넌트별 보존

```yaml
# runtime-config.yaml
overrides:
  tenant-a:
    retention_period: 8760h  # 1년
    retention_stream:
      - selector: '{level="debug"}'
        priority: 1
        period: 168h
```

### 보존 동작

1. Compactor가 `compaction_interval`마다 실행
2. 보존 기간 지난 청크 식별
3. `retention_delete_delay` 후 실제 삭제
4. 워커들이 병렬 삭제 처리

---

## 로그 삭제

특정 로그를 즉시 삭제하는 API. GDPR 등 컴플라이언스 대응.

### 활성화

```yaml
compactor:
  retention_enabled: true
  delete_request_store: s3
  delete_max_interval: 24h

limits_config:
  deletion_mode: filter-and-delete  # 또는 filter-only, disabled
```

### 삭제 모드

| 모드 | 동작 |
|------|------|
| `disabled` | 삭제 비활성 |
| `filter-only` | 쿼리에서만 필터링 (실제 데이터는 보존) |
| `filter-and-delete` | 쿼리 필터링 + 실제 삭제 |

### 삭제 요청 API

```bash
# 삭제 요청 생성
curl -XPOST -G "http://loki:3100/loki/api/v1/delete" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={app="myapp"} |= "user_id=12345"' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700100000"
```

### 삭제 요청 조회

```bash
curl -XGET "http://loki:3100/loki/api/v1/delete" \
  -H "X-Scope-OrgID: tenant-1"
```

### 삭제 요청 취소

```bash
curl -XDELETE "http://loki:3100/loki/api/v1/delete?request_id=<id>" \
  -H "X-Scope-OrgID: tenant-1"
```

---

## 쿼리 성능 관리

### 쿼리 분할 (Query Splitting)

긴 시간 범위를 작은 단위로 분할.

```yaml
query_range:
  split_queries_by_interval: 30m
  align_queries_with_step: true
```

### 쿼리 샤딩

```yaml
limits_config:
  tsdb_max_query_parallelism: 128
  
query_range:
  parallelise_shardable_queries: true
```

### 쿼리 한도

```yaml
limits_config:
  max_query_length: 721h          # 30일 + 1시간
  max_query_lookback: 0           # 0이면 보존 기간까지
  max_query_parallelism: 32
  max_query_series: 500
  max_entries_limit_per_query: 5000
  max_streams_matchers_per_query: 1000
```

### 느린 쿼리 로깅

```yaml
frontend:
  log_queries_longer_than: 5s
```

---

## 캐싱

### 캐시 종류

| 캐시 | 대상 | 효과 |
|------|------|------|
| Results Cache | 최종 쿼리 결과 | 반복 쿼리 가속 |
| Chunk Cache | 청크 데이터 | 스토리지 IO 감소 |
| Index Cache | 인덱스 조회 | 인덱스 IO 감소 |
| Volume Cache | 볼륨 쿼리 | 라벨 메타 가속 |

### Memcached 사용

```yaml
chunk_store_config:
  chunk_cache_config:
    memcached:
      batch_size: 256
      parallelism: 10
    memcached_client:
      consistent_hash: true
      addresses: dns+memcached-chunks:11211

storage_config:
  index_queries_cache_config:
    memcached:
      batch_size: 100
      parallelism: 10
    memcached_client:
      consistent_hash: true
      addresses: dns+memcached-index:11211

query_range:
  cache_results: true
  results_cache:
    cache:
      memcached_client:
        consistent_hash: true
        addresses: dns+memcached-results:11211
```

### Redis 사용

```yaml
query_range:
  cache_results: true
  results_cache:
    cache:
      redis:
        endpoint: redis:6379
        timeout: 100ms
        expiration: 24h
```

---

## WAL (Write-Ahead Log)

Ingester 충돌 시 데이터 복구.

```yaml
ingester:
  wal:
    enabled: true
    dir: /loki/wal
    flush_on_shutdown: true
    replay_memory_ceiling: 4GB
    checkpoint_duration: 5m
```

### 동작

1. 모든 쓰기는 메모리와 WAL 디스크에 동시 기록
2. 청크가 스토리지로 플러시되면 해당 WAL 항목 정리
3. Ingester 재시작 시 WAL을 리플레이하여 메모리 복원

### 디스크 요구사항

`replay_memory_ceiling` × Ingester 수 이상의 디스크 권장.

---

## 업그레이드 운영

### 무중단 롤링 업데이트

마이크로서비스 모드에서 권장 순서:

1. **Compactor** (단일 인스턴스, 다운타임 허용)
2. **Index Gateway**
3. **Ruler**
4. **Query Frontend / Query Scheduler**
5. **Querier**
6. **Distributor**
7. **Ingester** (가장 신중하게)

### Ingester 업그레이드 주의

- WAL 활성화 필수
- 한 번에 하나씩
- `final_sleep` 설정으로 정상 종료 시간 확보
- `chunk_retain_period` 동안 새 ingester가 데이터 채울 시간 확보

```yaml
ingester:
  lifecycler:
    final_sleep: 30s
  chunk_retain_period: 30s
```

---

## 모니터링

### 자체 메트릭

Loki는 Prometheus 형식 메트릭을 `/metrics` 엔드포인트로 노출.

### 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `loki_distributor_lines_received_total` | 수신 로그 라인 수 |
| `loki_distributor_bytes_received_total` | 수신 바이트 |
| `loki_ingester_chunks_flushed_total` | 플러시된 청크 수 |
| `loki_ingester_memory_streams` | 메모리 내 스트림 수 |
| `loki_request_duration_seconds` | 요청 지연 시간 |
| `loki_logql_querystats_*` | 쿼리 통계 |

### Mixin (대시보드 + 알림)

[Loki Mixin](https://github.com/grafana/loki/tree/main/production/loki-mixin) 사용:

```bash
# Jsonnet으로 빌드
jsonnet -J vendor -m dashboards mixin.libsonnet
```

### 핵심 SLI/SLO

- **수집 가용성**: 4xx/5xx 응답 비율 < 0.1%
- **쿼리 가용성**: 쿼리 5xx 비율 < 0.5%
- **수집 지연**: p99 < 1s
- **쿼리 지연**: p99 < 30s (시간 범위 기준)

### Self-Monitoring

Loki 자체 로그를 Loki에 보내는 셀프 모니터링 권장.

```alloy
loki.source.kubernetes "loki_logs" {
  targets = discovery.kubernetes.loki_pods.targets
  forward_to = [loki.write.self.receiver]
}

loki.write "self" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
  external_labels = {
    cluster = "loki-prod",
    job     = "loki-self",
  }
}
```
