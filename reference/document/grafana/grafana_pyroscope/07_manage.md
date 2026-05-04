# 운영 및 관리

> 이 문서는 Pyroscope 클러스터의 운영(보안, 멀티테넌시, 한도, 모니터링, 업그레이드)을 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/

---

## 목차

1. [멀티 테넌시 운영](#멀티-테넌시-운영)
2. [인증과 인가](#인증과-인가)
3. [리소스 한도(Limits)](#리소스-한도limits)
4. [Shuffle Sharding](#shuffle-sharding)
5. [모니터링: 메타 모니터링](#모니터링-메타-모니터링)
6. [백업과 재해 복구](#백업과-재해-복구)
7. [업그레이드](#업그레이드)
8. [성능 튜닝](#성능-튜닝)

---

## 멀티 테넌시 운영

Pyroscope는 단일 클러스터로 여러 테넌트(조직/팀/환경)를 분리 운영할 수 있습니다.

### 활성화

```yaml
multitenancy_enabled: true
```

### 테넌트 식별

- HTTP 헤더 `X-Scope-OrgID: tenant-a`
- 모든 API 요청에 필요 (인제스트, 쿼리, 관리)

### 테넌트 분리 단위

- **데이터**: 오브젝트 스토리지의 `<bucket>/<tenant_id>/...` 디렉토리
- **WAL/메모리**: Ingester는 메모리에서 테넌트별 분리 유지
- **인덱스/쿼리**: Querier가 테넌트 ID로만 데이터를 찾음

### 단일 테넌트 운영

소규모 환경에서는 비활성화하면 모든 데이터가 `anonymous` 테넌트에 들어갑니다.

```yaml
multitenancy_enabled: false
```

---

## 인증과 인가

Pyroscope **자체에는 사용자/패스워드 개념이 없습니다**. 인증은 보통 다음 중 하나로 처리합니다.

### 1. Grafana Cloud 또는 Grafana Enterprise

- Grafana Cloud Profiles는 자체 토큰을 발급
- Enterprise는 Access Policy + Cloud Access Token

### 2. 자체 호스팅 + 게이트웨이

```
[Client] -> [Auth Gateway (nginx/oauth2-proxy)] -> [Pyroscope]
                  |
                  └─ X-Scope-OrgID 헤더 주입
```

### 3. Basic Auth (간단한 환경)

nginx-side에서 Basic Auth + 헤더 주입.

```nginx
location / {
  auth_basic "Pyroscope";
  auth_basic_user_file /etc/nginx/.htpasswd;
  proxy_set_header X-Scope-OrgID "team-a";
  proxy_pass http://pyroscope-distributor:4040;
}
```

---

## 리소스 한도(Limits)

테넌트별 또는 글로벌 한도로 운영 안정성을 확보합니다.

### 인제스트 한도

```yaml
limits:
  # 인제스트
  ingestion_rate_mb: 4         # MB/s per tenant
  ingestion_burst_size_mb: 8
  max_global_series_per_user: 5000
  max_label_names_per_series: 30
  max_label_name_length: 1024
  max_label_value_length: 2048

  # 쿼리
  max_query_length: 720h       # 30일
  max_query_lookback: 0s       # 무제한 (보존 한도까지)
  max_query_parallelism: 16
  query_ready_timeout: 1m

  # 보존
  retention_period: 30d
```

### 동적 적용 (Tenant-Settings)

운영 중에 한도를 변경하려면 Tenant-Settings 컴포넌트를 사용합니다(또는 설정 파일 핫 리로드).

### 위반 시 동작

- 인제스트 초과 → HTTP 429
- 카디널리티 초과 → HTTP 400 + 에러 로그
- 쿼리 시간 초과 → HTTP 422

---

## Shuffle Sharding

대규모 멀티 테넌트 환경에서 **시끄러운 이웃(noisy neighbor)** 문제를 완화합니다.

### 개념

- 전체 N개 Ingester 중에서 각 테넌트가 K개(예: 3개)만 사용하도록 제한
- 한 테넌트의 부하가 다른 테넌트에 미치는 영향을 K/N로 줄임

### 설정

```yaml
limits:
  ingestion_tenant_shard_size: 3
  store_gateway_tenant_shard_size: 3
```

### 효과

- N=30, K=3 → 한 테넌트의 폭주가 영향 줄 수 있는 다른 테넌트 = 약 10%
- 선택은 zone-aware로 균형 잡힘

---

## 모니터링: 메타 모니터링

Pyroscope 자체를 모니터링해야 합니다.

### Prometheus 메트릭

`/metrics` 엔드포인트에서 Prometheus 형식의 메트릭 노출.

주요 메트릭:

- `pyroscope_distributor_received_compressed_bytes_total`
- `pyroscope_ingester_memory_series`
- `pyroscope_ingester_blocks_uploaded_total`
- `pyroscope_request_duration_seconds`
- `cortex_ring_members{state="..."}` (해시 링 상태)

### 자기 자신 프로파일링

Pyroscope는 자체 pprof endpoint를 노출합니다.

```yaml
self_profiling:
  enabled: true
  url: http://pyroscope:4040
```

### 알람 규칙 권장

- Ingester가 unhealthy 상태가 5분 이상
- 인제스트 에러율 > 1%
- 쿼리 P99 latency > 임계값
- Compactor가 이전 사이클 미완료

### 대시보드

`mixin` 디렉토리에 공식 대시보드/알람 규칙이 있습니다 (Mimir와 유사한 패턴).

---

## 백업과 재해 복구

### 데이터 위치

- **인제스트 인 메모리/WAL**: 각 Ingester의 PVC
- **장기 저장**: 오브젝트 스토리지 (S3/GCS/Azure)
- **메타데이터**: 오브젝트 스토리지 + (선택) bucket-index

### 백업 전략

- 오브젝트 스토리지는 통상 SLA가 높음 (S3 99.9999999%) → 별도 백업이 큰 의미 없는 경우가 많음
- 정말 중요한 경우 cross-region 복제(Replication) 활성화

### Ingester 노드 손실

- RF=3 이라면 다른 2개 노드가 데이터를 보유 → 데이터 손실 없음
- WAL이 살아있다면 재시작 시 자동 복구
- WAL까지 잃으면 최근(미플러시) 프로파일이 유실될 수 있음

### 클러스터 전체 복구

- 새 Pyroscope 클러스터 기동
- 같은 오브젝트 버킷을 가리키게 설정
- Compactor가 bucket index를 재구성

---

## 업그레이드

### 마이너 버전 업

- 통상 무중단 롤링 업데이트 가능
- Ingester는 PodDisruptionBudget 으로 한 번에 1개씩만 교체
- Distributor/Querier는 자유롭게 교체

### 메이저 버전 업

- 릴리스 노트의 **Breaking changes** 확인
- 설정 deprecated 항목 점검
- 단계적 카나리 (소수 노드만 새 버전)

### 다운그레이드

- 일반적으로 권장하지 않음 (블록 포맷 호환성 이슈 가능)
- 새 버전이 만든 블록은 이전 버전이 못 읽을 수 있음

---

## 성능 튜닝

### Ingester

- `head.max_block_bytes`: 헤드 블록 크기 (기본 1.6GB) — 메모리에 영향
- `head.max_block_duration`: 블록 시간 (기본 1h) — 너무 짧으면 작은 블록 생성
- WAL fsync 정책: `wal.flush_interval`

### Querier

- `max_concurrent_queries`: 노드당 동시 쿼리 수
- `block_sync_concurrency`: 블록 메타 동기화 병렬도

### Compactor

- `compaction_concurrency`: 동시 컴팩션 수
- `compactor_blocks_retention_period`: 블록 보존 기간

### Cache

- Memcached 사용 권장 (chunks_cache, metadata_cache)

```yaml
chunks_cache:
  backend: memcached
  memcached:
    addresses: dns+memcached.default.svc:11211
```

### 쿼리 분할

```yaml
querier:
  query_split_interval: 24h
```

---

## 다음 단계

- [09_configuration.md](./09_configuration.md) - 설정 항목 상세
- [11_http_api.md](./11_http_api.md) - 운영 API
- [03_deployment.md](./03_deployment.md) - 규모별 배포
