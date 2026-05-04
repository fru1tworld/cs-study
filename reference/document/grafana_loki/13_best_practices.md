# Loki 모범 사례 (Best Practices)

> 이 문서는 Grafana Loki 공식 문서의 Best Practices 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/

---

## 목차

1. [라벨 모범 사례](#라벨-모범-사례)
2. [카디널리티 관리](#카디널리티-관리)
3. [청크와 인덱스 튜닝](#청크와-인덱스-튜닝)
4. [쿼리 최적화](#쿼리-최적화)
5. [수집 최적화](#수집-최적화)
6. [스토리지 모범 사례](#스토리지-모범-사례)
7. [멀티 테넌시 모범 사례](#멀티-테넌시-모범-사례)
8. [업그레이드 모범 사례](#업그레이드-모범-사례)
9. [보안 모범 사례](#보안-모범-사례)
10. [모니터링 모범 사례](#모니터링-모범-사례)

---

## 라벨 모범 사례

### 좋은 라벨 후보

- **소스 식별자**: `cluster`, `region`, `namespace`, `app`, `job`
- **환경**: `environment` (prod/staging/dev)
- **호스트/Pod 식별자** (제한적): `host`, `instance`
- **로그 종류**: `log_type` (access/error/audit)

### 나쁜 라벨 후보

- 사용자 ID, 요청 ID, 트레이스 ID (높은 카디널리티)
- HTTP path/URL (무한 카디널리티)
- IP 주소 (변동 큼)
- 타임스탬프
- 에러 메시지 본문

### 라벨 수 가이드

```
권장: 10-15개 이내
경고: 20개 초과 시 검토
위험: 30개 초과 시 카디널리티 폭증
```

### 정적 vs 동적 라벨

**정적 라벨 (수집 시 고정)**
```alloy
loki.process "labels" {
  stage.static_labels {
    values = {
      cluster = "prod-us-east-1",
      env     = "production",
    }
  }
}
```

**동적 라벨 (로그 내용에서 추출)**
```alloy
loki.process "extract" {
  stage.json {
    expressions = {
      level = "level",
    }
  }
  stage.labels {
    values = {
      level = "",   // level 값을 라벨로
    }
  }
}
```

동적 라벨은 카디널리티가 예측 가능한 경우만 사용.

---

## 카디널리티 관리

### 카디널리티 폭증 신호

- Ingester 메모리 급증
- 활성 스트림 수 급증 (`loki_ingester_memory_streams`)
- 쿼리 속도 급격히 저하

### 분석 도구

```bash
# logcli로 라벨 카디널리티 확인
logcli labels --addr=http://loki:3100 \
  --from="2024-01-01T00:00:00Z" \
  --to="2024-01-02T00:00:00Z"

# 특정 라벨의 값 분포
logcli labels level --addr=http://loki:3100
```

### Series API

```bash
curl -G "http://loki:3100/loki/api/v1/series" \
  --data-urlencode 'match[]={namespace="prod"}' \
  | jq '.data | length'
```

### 카디널리티 한도

```yaml
limits_config:
  max_label_name_length: 1024
  max_label_value_length: 4096
  max_label_names_per_series: 30
  cardinality_limit: 100000
  max_streams_per_user: 10000
  max_global_streams_per_user: 5000
```

### 카디널리티 줄이기

#### 클라이언트 측

```alloy
loki.process "reduce_cardinality" {
  // 라벨 값 정규화
  stage.regex {
    expression = "(?P<path>/api/users/)(\\d+)"
  }
  stage.labels {
    values = {
      path = "",   // /api/users/{id}로 변환된 path
    }
  }
}
```

#### 서버 측 (구조화된 메타데이터로 이전)

높은 카디널리티 정보는 라벨 대신 **structured metadata** 로:

```alloy
loki.process "metadata" {
  stage.structured_metadata {
    values = {
      trace_id = "trace_id",
      user_id  = "user_id",
    }
  }
}
```

라벨 인덱스에는 영향 없으면서 쿼리에서 필터/추출 가능.

---

## 청크와 인덱스 튜닝

### 청크 크기 최적화

```yaml
ingester:
  chunk_target_size: 1572864     # 1.5MB (압축 후) - 권장
  chunk_idle_period: 30m         # idle 청크 빨리 플러시 (메모리 절약)
  max_chunk_age: 2h              # 청크 최대 수명
  chunk_block_size: 262144       # 256KB
  chunk_retain_period: 1m        # 플러시 후 메모리 보유
```

### 너무 작은 청크의 문제

- 오브젝트 스토리지 요청 수 증가 (S3 비용)
- 인덱스 크기 증가
- 쿼리 시 더 많은 fetch 필요

### 너무 큰 청크의 문제

- Ingester 메모리 압박
- 특정 시간 범위 쿼리 시 불필요한 데이터 다운로드

### TSDB 인덱스 권장

```yaml
schema_config:
  configs:
    - from: 2024-06-01
      store: tsdb
      object_store: s3
      schema: v13         # 최신
      index:
        prefix: tsdb_index_
        period: 24h
```

---

## 쿼리 최적화

### 효율적 쿼리 작성

#### ✅ 좋은 예

```logql
# 스트림 선택자가 구체적
{namespace="prod", app="api"} |= "error"

# 라인 필터 먼저, 파서 나중
{namespace="prod"} |= "error" | json | level="error"

# 시간 범위 좁게
{namespace="prod"}[5m]
```

#### ❌ 나쁜 예

```logql
# 너무 광범위한 선택자
{} |= "error"

# 파서 먼저, 필터 나중 (모든 라인 파싱)
{namespace="prod"} | json | level="error"

# 정규식만으로 모든 라인 매칭
{namespace="prod"} |~ ".*"
```

### 쿼리 분할

```yaml
query_range:
  split_queries_by_interval: 30m
  parallelise_shardable_queries: true
```

### 결과 캐싱

```yaml
query_range:
  cache_results: true
  results_cache:
    cache:
      memcached_client:
        addresses: dns+memcached:11211
```

### 쿼리 시간 제한

```yaml
limits_config:
  max_query_length: 721h         # 30일
  max_query_lookback: 0          # 보존 기간까지
  query_timeout: 5m
  max_entries_limit_per_query: 5000
```

---

## 수집 최적화

### 클라이언트 측 배칭

```alloy
loki.write "default" {
  endpoint {
    url        = "http://loki:3100/loki/api/v1/push"
    batch_size = "1MiB"     // 1MB 배치
    batch_wait = "1s"       // 또는 1초
  }
}
```

### 압축

기본적으로 활성화되어 있지만 확인:

```alloy
loki.write "default" {
  endpoint {
    url     = "..."
    // gzip 자동 사용
  }
}
```

### 라벨 정렬

라벨은 알파벳 순으로 정렬되어야 캐싱이 일관됨. 클라이언트가 자동 처리.

### 시간 정렬

같은 스트림의 로그는 시간 순서대로 보내야 함. Out-of-order는 거부됨 (또는 별도 설정 필요):

```yaml
limits_config:
  max_label_value_length: 4096
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  creation_grace_period: 10m
```

### Structured Metadata 활용

라벨 카디널리티를 늘리지 않고 메타데이터 추가:

```alloy
stage.structured_metadata {
  values = {
    trace_id = "trace_id",
    span_id  = "span_id",
  }
}
```

---

## 스토리지 모범 사례

### S3 Lifecycle 정책

오래된 청크를 자동으로 저렴한 스토리지 클래스로 이전:

```json
{
  "Rules": [
    {
      "Id": "MoveToIA",
      "Status": "Enabled",
      "Filter": { "Prefix": "loki/chunks/" },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

> 주의: Glacier 등 저비용 스토리지는 쿼리 시 즉시 접근 불가능. 핫 데이터만 Standard에 두기.

### 동일 리전 권장

오브젝트 스토리지와 Loki는 같은 리전에 두어 네트워크 비용/지연 최소화.

### Versioning + Replication

중요 데이터:
- S3 Versioning 활성화
- Cross-Region Replication

### Compactor 설정

```yaml
compactor:
  retention_enabled: true
  compaction_interval: 10m
  retention_delete_delay: 2h
  retention_delete_worker_count: 150
  
  # 삭제 마킹된 데이터를 빠르게 정리
  shared_store: s3
```

---

## 멀티 테넌시 모범 사례

### 테넌트 ID 명명

```
✅ 좋음: tenant-a, prod-team1, k8s-cluster-1
❌ 피하기: ../etc, "", tenant/with/slashes
```

### 테넌트별 한도 차등화

```yaml
overrides:
  large-customer:
    ingestion_rate_mb: 100
    max_streams_per_user: 100000
    retention_period: 8760h
  
  small-customer:
    ingestion_rate_mb: 5
    max_streams_per_user: 5000
    retention_period: 720h
```

### 전체 한도 보호

테넌트별 한도 외에도 전역 보호:

```yaml
ingester:
  instance_limits:
    max_ingestion_rate: 0      # 무제한
    max_streams_per_user: 0
    max_streams: 0
```

### Shuffle Sharding 사용

```yaml
limits_config:
  ingestion_partitions_tenant_shard_size: 4
```

큰 테넌트가 작은 테넌트에 영향 안 미치게.

---

## 업그레이드 모범 사례

### 단계적 업그레이드

```
한 번에 한 마이너 버전씩
2.8 → 2.9 → 2.10 → 3.0
```

### 백업

업그레이드 전:
- 구성 파일 백업
- 가능하면 schema_config 백업
- 오브젝트 스토리지 버전 관리

### 카나리 배포

먼저 1-2개 인스턴스만 업그레이드, 검증 후 나머지.

### 롤백 계획

새 청크 형식이나 인덱스 형식이 변경되면 롤백 어려움. 신중히.

---

## 보안 모범 사례

### TLS 강제

```yaml
server:
  http_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
    client_auth_type: RequireAndVerifyClientCert
    client_ca_file: /etc/certs/ca.crt
  
  grpc_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
```

### 컴포넌트 간 mTLS

```yaml
ingester_client:
  grpc_client_config:
    tls_enabled: true
    tls_cert_path: /etc/certs/client.crt
    tls_key_path: /etc/certs/client.key
    tls_ca_path: /etc/certs/ca.crt
```

### 비밀 관리

환경 변수나 외부 시크릿 관리자 사용:

```yaml
storage_config:
  aws:
    access_key_id: ${AWS_ACCESS_KEY_ID}
    secret_access_key: ${AWS_SECRET_ACCESS_KEY}
```

Kubernetes:

```yaml
env:
  - name: AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: loki-s3
        key: secret-key
```

### 네트워크 정책

Kubernetes NetworkPolicy로 컴포넌트 간 통신만 허용.

### Audit Logging

리버스 프록시에서 모든 요청 로깅:

```nginx
log_format loki_audit '$remote_addr - $remote_user - $time_iso8601 - '
                     '$request - $status - X-Scope-OrgID:$http_x_scope_orgid';
access_log /var/log/nginx/loki-audit.log loki_audit;
```

---

## 모니터링 모범 사례

### 셀프 모니터링

Loki가 Loki를 모니터링:

```alloy
loki.source.kubernetes "loki" {
  targets = discovery.kubernetes.loki_pods.targets
  forward_to = [loki.write.self.receiver]
}

loki.write "self" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "loki-self",
    }
  }
}
```

### 핵심 SLI 추적

| SLI | 목표 |
|-----|------|
| 수집 가용성 | > 99.9% |
| 쿼리 가용성 | > 99.5% |
| 수집 P99 지연 | < 1s |
| 쿼리 P99 지연 | < 30s |

### Loki Mixin 사용

```bash
jb install github.com/grafana/loki/production/loki-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

대시보드 + Recording Rules + Alerting Rules 일괄 제공.

### Loki Canary 배포

[11_loki_canary.md](./11_loki_canary.md) 참조. 종단간 검증 필수.

### 용량 추세 추적

```promql
# 일일 수집량 추세
sum by (tenant) (rate(loki_distributor_bytes_received_total[1d])) * 86400

# 활성 스트림 추세
sum by (tenant) (loki_ingester_memory_streams)

# 한도 대비 사용률
(
  sum by (tenant) (rate(loki_distributor_bytes_received_total[5m]))
  / 1e6
)
/
on (tenant) sum by (tenant) (loki_overrides{limit_name="ingestion_rate_mb"})
```
