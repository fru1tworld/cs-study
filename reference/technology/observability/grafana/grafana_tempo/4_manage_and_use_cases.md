# Tempo 운영 관리와 활용 사례

## Tempo 운영 관리

> 원본: https://grafana.com/docs/tempo/latest/operations/

---

### 목차

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

### 멀티 테넌시

#### 활성화

```yaml
multitenancy_enabled: true
```

활성화하면 모든 API 요청에 `X-Scope-OrgID` 헤더가 필수다.

#### 단일 테넌트 모드

```yaml
multitenancy_enabled: false
```

모든 데이터는 `single-tenant` ID로 저장된다.

#### 테넌트별 한도 (overrides.yaml)

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

### 수집(Ingestion) 한도

#### 글로벌 한도

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

#### 테넌트별 Rate Limit

```yaml
overrides:
  defaults:
    ingestion_rate_limit_bytes: 15_000_000     # 15MB/s
    ingestion_burst_size_bytes: 20_000_000     # 20MB 버스트
    max_global_traces_per_user: 100_000
```

#### Rate Limit 동작

- **Local**: Distributor 인스턴스별 적용
- **Global**: 모든 Distributor가 협력

```yaml
distributor:
  rate_limit_strategy: global
```

---

### 스토리지 관리

#### 백엔드 옵션

| 백엔드 | 권장 사용 |
|--------|----------|
| AWS S3 | 가장 일반적 |
| GCS | GCP 환경 |
| Azure Blob | Azure 환경 |
| Filesystem | 개발/테스트 |

#### S3 구성

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

#### Bucket Index

각 테넌트의 블록 목록을 캐싱하여 빠르게 조회한다.

```yaml
storage:
  trace:
    blocklist_poll: 5m            # 블록 목록 폴링 주기
    blocklist_poll_concurrency: 50
    blocklist_poll_jitter_ms: 1000
```

---

### 보존(Retention)

#### 글로벌 보존 기간

```yaml
compactor:
  compaction:
    block_retention: 720h        # 30일
    compacted_block_retention: 1h
```

#### 테넌트별 보존

```yaml
overrides:
  tenant-a:
    block_retention: 8760h       # 1년
  tenant-b:
    block_retention: 168h        # 7일
```

#### 보존 동작

1. Compactor가 주기적으로 블록 메타데이터 검사
2. `block_retention` 지난 블록을 삭제 마킹
3. 일정 시간 후 실제 삭제

---

### Compactor 운영

#### 기본 설정

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

#### 압축 단계

```
[작은 블록 N개] → [Compactor] → [큰 블록 1개]
```

레벨별 압축:
- Level 0: 1시간 블록
- Level 1: 4시간 블록
- Level 2: 24시간 블록

#### 분산 Compactor

```yaml
compactor:
  ring:
    kvstore:
      store: memberlist
    
    instance_addr: ${POD_IP}
    instance_id: ${POD_NAME}
```

여러 Compactor 인스턴스가 테넌트별로 작업을 분산 처리한다.

#### 모니터링

| 메트릭 | 설명 |
|--------|------|
| `tempodb_compaction_blocks_total` | 압축된 블록 수 |
| `tempodb_compaction_bytes_written_total` | 압축으로 쓰인 바이트 |
| `tempodb_compaction_objects_combined_total` | 결합된 오브젝트 수 |
| `tempodb_retention_marked_for_deletion_total` | 삭제 마킹된 블록 수 |
| `tempodb_retention_deleted_total` | 실제 삭제된 블록 수 |

---

### 캐싱

#### 캐시 종류

| 캐시 | 대상 |
|------|------|
| Bloom Filter | 빠른 트레이스 ID 조회 |
| Index | 인덱스 조회 |
| Search | TraceQL 결과 |
| Frontend | 쿼리 결과 |

#### Memcached 설정

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

#### Redis 설정

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

### 모니터링

#### 메트릭 노출

`/metrics` 엔드포인트 (기본 포트 3200).

#### 핵심 메트릭

##### Distributor
- `tempo_distributor_spans_received_total`
- `tempo_distributor_bytes_received_total`
- `tempo_distributor_metric_received_spans_total`

##### Ingester
- `tempo_ingester_traces_created_total`
- `tempo_ingester_blocks_flushed_total`
- `tempo_ingester_live_traces`

##### Querier
- `tempo_query_frontend_queries_within_slo_total`
- `tempo_request_duration_seconds`

##### Compactor
- `tempodb_compaction_blocks_total`
- `tempodb_compaction_errors_total`

#### Mixin

[Tempo Mixin](https://github.com/grafana/tempo/tree/main/operations/tempo-mixin)으로 대시보드/알림 일괄 배포.

```bash
jb install github.com/grafana/tempo/operations/tempo-mixin@main
jsonnet -J vendor mixin.libsonnet | gojsontoyaml > mixin-prom-rules.yaml
```

#### SLO

권장 SLO:
- Ingestion 가용성: > 99.9%
- 쿼리 가용성: > 99.5%
- 쿼리 지연: P99 < 30초

---

### 업그레이드 운영

#### 권장 순서 (Microservices)

1. Compactor (단일 또는 소수)
2. Query Frontend
3. Querier
4. Distributor
5. Ingester (가장 신중)
6. Metrics Generator

#### Ingester 업그레이드 주의

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

#### Parquet 버전 업그레이드

```yaml
storage:
  trace:
    block:
      version: vParquet4
```

기존 vParquet3 블록은 그대로 유지. Compactor가 점진적으로 새 버전으로 변환.

---

### 장애 복구

#### Ingester 장애

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

#### 오브젝트 스토리지 장애

- Ingester는 메모리 + WAL에 데이터 보유 (재시도)
- Querier는 캐시된 데이터로 일부 응답 가능
- 복구 후 자동 재시도

#### 데이터 손상 복구

```bash
# 손상된 블록 식별
tempo-cli list blocks <tenant-id>

# 특정 블록 스키마 검사
tempo-cli view schema <tenant-id> <block-id>

# 특정 트레이스 제거 (마지막 수단)
tempo-cli rewrite-blocks drop-traces <tenant-id> <trace-ids>
```

#### 백업 전략

오브젝트 스토리지 자체 백업:
- S3: Cross-Region Replication, Versioning
- GCS: Object Versioning, Multi-region buckets
- Azure: Geo-Redundant Storage

WAL 디스크는 일시적 데이터이므로 별도 백업이 불필요하다.

---

## Tempo 활용 사례 (Solutions and Use Cases)

> 원본 참고: https://grafana.com/docs/tempo/latest/solutions-with-traces/

---

### 목차

1. [개요](#개요)
2. [APM (애플리케이션 성능 모니터링)](#apm-애플리케이션-성능-모니터링)
3. [장애 디버깅 / RCA](#장애-디버깅--rca)
4. [SLO 모니터링](#slo-모니터링)
5. [의존성 분석 (Service Dependencies)](#의존성-분석-service-dependencies)
6. [성능 최적화 / 병목 식별](#성능-최적화--병목-식별)
7. [데이터베이스 호출 분석](#데이터베이스-호출-분석)
8. [외부 서비스 호출 모니터링](#외부-서비스-호출-모니터링)
9. [에러 분석](#에러-분석)
10. [비즈니스 메트릭 추출](#비즈니스-메트릭-추출)

---

### 개요

분산 트레이싱은 다음 문제 해결에 강점을 가진다:

- "왜 이 요청이 느린가?"
- "어떤 서비스가 에러를 일으키는가?"
- "특정 사용자의 요청은 어떤 경로를 거쳤는가?"
- "서비스 A에 대한 변경이 B에 어떤 영향을 미치는가?"

#### 메트릭/로그와의 비교

| 데이터 | 답할 수 있는 질문 |
|--------|------------------|
| 메트릭 | "얼마나 많이? 얼마나 빠르게?" (집계, 추세) |
| 로그 | "무엇이 일어났는가?" (이벤트 상세) |
| 트레이스 | "어디서 시간이 소비됐는가? 어떻게 흘렀는가?" (인과 관계) |

---

### APM (애플리케이션 성능 모니터링)

#### Tempo + Span Metrics 조합

[Metrics Generator](./05_metrics_generator.md) 활성화 → 자동 RED 메트릭 생성.

#### 표준 APM 대시보드

각 서비스에 대해:

##### Rate (요청 비율)
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

##### Errors (에러율)
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

##### Duration (지연 P50/P95/P99)
```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket[5m])
  )
)
```

#### Service Map

자동 생성된 서비스 그래프로 의존성 시각화.

#### 활용 워크플로

```
1. APM 대시보드에서 이상 감지 (예: P99 급증)
   ↓
2. Service Graph에서 어떤 서비스가 영향 받는지 확인
   ↓
3. 해당 서비스의 트레이스 검색 (TraceQL)
   ↓
4. 느린 트레이스의 스팬 분석 → 병목 식별
```

---

### 장애 디버깅 / RCA

#### 시나리오: 사용자 보고 "결제가 안 됨"

##### 1. 사용자 ID로 트레이스 검색

```traceql
{ .user.id = "u-12345" && resource.service.name = "checkout" }
```

##### 2. 에러 트레이스 필터

```traceql
{ .user.id = "u-12345" && status = error }
```

##### 3. 트레이스 상세 보기

스팬 트리에서:
- 어디서 에러 발생했는지
- 에러 메시지 (스팬 events)
- 관련 속성 (HTTP status, DB 에러 등)

##### 4. 관련 로그 확인

Trace ID로 Loki에서 로그 조회:

```logql
{app=~".+"} |= "trace_id=abc123"
```

#### 시나리오: 503 에러 폭증

##### 1. 메트릭에서 시점 확인

```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[1m])
)
```

##### 2. Exemplars로 트레이스 점프

Grafana 패널의 점을 클릭 → 해당 시점의 트레이스로 이동.

##### 3. 트레이스 패턴 분석

여러 에러 트레이스를 비교하여 공통점 찾기:
- 동일 다운스트림 서비스 호출 실패?
- 특정 DB 쿼리 타임아웃?
- 외부 API 응답 실패?

---

### SLO 모니터링

#### Latency SLO

"95%의 요청이 500ms 이내에 처리되어야 함"

```promql
sum by (service) (
  rate(traces_spanmetrics_latency_bucket{le="0.5"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_latency_count[5m])
)
> 0.95
```

#### Availability SLO

"99.9% 요청 성공"

```promql
1 - (
  sum by (service) (
    rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
  )
  /
  sum by (service) (
    rate(traces_spanmetrics_calls_total[5m])
  )
)
> 0.999
```

#### Error Budget Burn Rate

```promql
# 1시간 윈도우, 14.4배 burn (99.9% SLO 30일 burn)
(
  sum(rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[1h]))
  /
  sum(rate(traces_spanmetrics_calls_total[1h]))
)
> (1 - 0.999) * 14.4
```

---

### 의존성 분석 (Service Dependencies)

#### Service Graph 활용

자동 생성된 그래프로:

- 어떤 서비스가 어떤 서비스를 호출하는지
- 호출 빈도와 에러율
- 병목 의심 구간 식별

#### 변경 영향 분석

서비스 A를 변경할 때:
- 누가 A를 호출하는가? (downstream consumers)
- A의 SLO가 깨지면 누가 영향 받는가?

```promql
sum by (client) (
  rate(traces_service_graph_request_total{server="service-a"}[5m])
)
```

#### 종속성 시각화 PromQL

```promql
# 서비스 간 호출 매트릭스
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)

# 서비스 간 에러율
sum by (client, server) (
  rate(traces_service_graph_request_failed_total[5m])
)
/
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
```

---

### 성능 최적화 / 병목 식별

#### 느린 트레이스 찾기

```traceql
{ resource.service.name = "api" && duration > 1s }
```

#### 가장 느린 스팬 분석

트레이스 내에서 시간을 가장 많이 소비하는 스팬을 찾는다:

```traceql
{ resource.service.name = "api" } 
| max(duration) by (.span.name)
```

#### N+1 쿼리 패턴 감지

```traceql
{ resource.service.name = "api" } 
| count() by (.db.statement) 
> 100
```

같은 트레이스 내에서 동일 SQL이 100번 이상 실행되면 N+1 문제를 의심할 수 있다.

#### 직렬 vs 병렬 호출

스팬 시각화에서:
- 가로로 나란히: 직렬 (느림)
- 위아래로: 병렬 (빠름)

병렬화 가능한 호출을 식별하여 코드를 개선한다.

---

### 데이터베이스 호출 분석

#### 느린 쿼리

```traceql
{ .db.system = "postgresql" && duration > 100ms }
```

#### 가장 자주 호출되는 쿼리

```traceql
{ .db.system = "postgresql" } 
| count() by (.db.statement)
```

#### 쿼리별 평균 지연

```traceql
{ .db.system = "postgresql" } 
| avg(duration) by (.db.operation)
```

#### 트랜잭션 분석

```traceql
{ .db.statement =~ "BEGIN.*" } 
| max(duration) > 5s
```

장기 실행 트랜잭션을 감지한다.

---

### 외부 서비스 호출 모니터링

#### 외부 API 호출 식별

```traceql
{ kind = client && .http.url =~ "https://(?!internal).*" }
```

#### 외부 의존성별 신뢰성

```traceql
{ .http.url =~ "https://payment-gateway.*" } 
| (count(status=error) / count()) > 0.01
```

#### 타임아웃 식별

```traceql
{ kind = client && status = error && events.exception.type =~ ".*Timeout.*" }
```

#### 서드파티 API 비용 분석

```promql
# 외부 API 호출 횟수 (비용 추정)
sum by (target_service) (
  rate(traces_spanmetrics_calls_total{
    span_kind="SPAN_KIND_CLIENT",
    target_service=~"payment.*|sms.*|email.*"
  }[1h])
) * 3600
```

---

### 에러 분석

#### 에러 유형별 분포

```traceql
{ status = error } 
| count() by (events.exception.type)
```

#### 에러 발생 위치

```traceql
{ status = error } 
| count() by (resource.service.name, span.name)
```

#### 에러 전파 추적

```traceql
{ status = error && resource.service.name = "frontend" } 
&& 
{ status = error && resource.service.name != "frontend" }
```

frontend에서 발생한 에러와 다운스트림 에러를 함께 조회한다.

#### 에러 지속 시간 (얼마나 오래 걸리고 실패하는가)

```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket{status_code="STATUS_CODE_ERROR"}[5m])
  )
)
```

빠른 실패와 타임아웃 후 실패를 구분할 수 있다.

---

### 비즈니스 메트릭 추출

#### 사용자 행동 추적

스팬 속성에 비즈니스 정보 포함:

```python
with tracer.start_as_current_span("checkout") as span:
    span.set_attribute("user.tier", user.tier)
    span.set_attribute("cart.items", len(cart))
    span.set_attribute("cart.total", cart.total)
    span.set_attribute("payment.method", payment.method)
```

#### 분석

```traceql
# 결제 방법별 성공률
{ name = "checkout" } 
| (count(status=ok) / count()) by (.payment.method)

# Tier별 평균 카트 크기
{ name = "checkout" } 
| avg(.cart.items) by (.user.tier)

# 환불 트레이스
{ name = "refund" && .refund.amount > 100 }
```

#### TraceQL Metrics로 비즈니스 KPI

```traceql
# 분당 결제 성공
{ name = "checkout" && status = ok } 
| rate() 
by (.payment.method)

# P95 결제 처리 시간
{ name = "checkout" } 
| quantile_over_time(duration, 0.95) 
by (.payment.method)
```

이러한 메트릭은 별도의 메트릭 계측 코드 없이 트레이스에서 바로 추출할 수 있다.
