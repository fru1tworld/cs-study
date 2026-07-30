# Pyroscope 운영·관리, 사용 사례, 설정

## 운영 및 관리

> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/

---

### 목차

1. [멀티 테넌시 운영](#멀티-테넌시-운영)
2. [인증과 인가](#인증과-인가)
3. [리소스 한도(Limits)](#리소스-한도limits)
4. [Shuffle Sharding](#shuffle-sharding)
5. [모니터링: 메타 모니터링](#모니터링-메타-모니터링)
6. [백업과 재해 복구](#백업과-재해-복구)
7. [업그레이드](#업그레이드)
8. [성능 튜닝](#성능-튜닝)

---

### 멀티 테넌시 운영

Pyroscope는 단일 클러스터로 여러 테넌트(조직/팀/환경)를 분리 운영할 수 있습니다.

#### 활성화

```yaml
multitenancy_enabled: true
```

#### 테넌트 식별

- HTTP 헤더 `X-Scope-OrgID: tenant-a`
- 모든 API 요청에 필요 (인제스트, 쿼리, 관리)

#### 테넌트 분리 단위

- **데이터**: 오브젝트 스토리지의 `<bucket>/<tenant_id>/...` 디렉토리
- **WAL/메모리**: Ingester는 메모리에서 테넌트별 분리 유지
- **인덱스/쿼리**: Querier가 테넌트 ID로만 데이터를 찾음

#### 단일 테넌트 운영

소규모 환경에서는 멀티테넌시를 비활성화하면 모든 데이터가 `anonymous` 테넌트에 저장됩니다.

```yaml
multitenancy_enabled: false
```

---

### 인증과 인가

Pyroscope **자체에는 사용자/패스워드 개념이 없습니다**. 인증은 보통 다음 중 하나로 처리합니다.

#### 1. Grafana Cloud 또는 Grafana Enterprise

- Grafana Cloud Profiles는 자체 토큰을 발급
- Enterprise는 Access Policy + Cloud Access Token

#### 2. 자체 호스팅 + 게이트웨이

```
[Client] -> [Auth Gateway (nginx/oauth2-proxy)] -> [Pyroscope]
                  |
                  └─ X-Scope-OrgID 헤더 주입
```

#### 3. Basic Auth (간단한 환경)

nginx에서 Basic Auth를 처리하고 헤더를 주입합니다.

```nginx
location / {
  auth_basic "Pyroscope";
  auth_basic_user_file /etc/nginx/.htpasswd;
  proxy_set_header X-Scope-OrgID "team-a";
  proxy_pass http://pyroscope-distributor:4040;
}
```

---

### 리소스 한도(Limits)

테넌트별 또는 글로벌 한도로 운영 안정성을 확보합니다.

#### 인제스트 한도

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

#### 동적 적용 (Tenant-Settings)

운영 중에 한도를 변경하려면 Tenant-Settings 컴포넌트를 사용하거나 설정 파일 핫 리로드를 활용합니다.

#### 위반 시 동작

- 인제스트 초과 → HTTP 429
- 카디널리티 초과 → HTTP 400 + 에러 로그
- 쿼리 시간 초과 → HTTP 422

---

### Shuffle Sharding

대규모 멀티 테넌트 환경에서 **시끄러운 이웃(noisy neighbor)** 문제를 완화합니다.

#### 개념

- 전체 N개 Ingester 중에서 각 테넌트가 K개(예: 3개)만 사용하도록 제한
- 한 테넌트의 부하가 다른 테넌트에 미치는 영향을 K/N로 줄임

#### 설정

```yaml
limits:
  ingestion_tenant_shard_size: 3
  store_gateway_tenant_shard_size: 3
```

#### 효과

- N=30, K=3 → 한 테넌트의 폭주가 영향 줄 수 있는 다른 테넌트 = 약 10%
- 선택은 zone-aware로 균형 잡힘

---

### 모니터링: 메타 모니터링

Pyroscope 자체를 모니터링해야 합니다.

#### Prometheus 메트릭

`/metrics` 엔드포인트에서 Prometheus 형식의 메트릭 노출.

주요 메트릭:

- `pyroscope_distributor_received_compressed_bytes_total`
- `pyroscope_ingester_memory_series`
- `pyroscope_ingester_blocks_uploaded_total`
- `pyroscope_request_duration_seconds`
- `cortex_ring_members{state="..."}` (해시 링 상태)

#### 자기 자신 프로파일링

Pyroscope는 자체 pprof 엔드포인트를 노출합니다.

```yaml
self_profiling:
  disable_push: false
  tenant_id: ""
```

#### 알람 규칙 권장

- Ingester가 5분 이상 unhealthy 상태 유지
- 인제스트 에러율 > 1%
- 쿼리 P99 latency > 임계값
- Compactor가 이전 사이클 미완료

#### 대시보드

`mixin` 디렉토리에 공식 대시보드/알람 규칙이 있습니다 (Mimir와 유사한 패턴).

---

### 백업과 재해 복구

#### 데이터 위치

- **인제스트 인 메모리/WAL**: 각 Ingester의 PVC
- **장기 저장**: 오브젝트 스토리지 (S3/GCS/Azure)
- **메타데이터**: 오브젝트 스토리지 + (선택) bucket-index

#### 백업 전략

- 오브젝트 스토리지는 일반적으로 SLA가 높으므로 (S3 99.9999999%) 별도 백업이 불필요한 경우가 많습니다.
- 고가용성이 필수인 경우 크로스 리전 복제(Cross-Region Replication)를 활성화합니다.

#### Ingester 노드 손실

- RF=3이면 나머지 2개 노드가 데이터를 보유하므로 데이터 손실이 없습니다.
- WAL이 남아 있으면 재시작 시 자동 복구됩니다.
- WAL까지 손실되면 아직 플러시되지 않은 최근 프로파일이 유실될 수 있습니다.

#### 클러스터 전체 복구

- 새 Pyroscope 클러스터 기동
- 같은 오브젝트 버킷을 가리키게 설정
- Compactor가 버킷 인덱스를 재구성합니다.

---

### 업그레이드

#### 마이너 버전 업

- 통상 무중단 롤링 업데이트 가능
- Ingester는 PodDisruptionBudget으로 한 번에 1개씩만 교체
- Distributor/Querier는 자유롭게 교체

#### 메이저 버전 업

- 릴리스 노트의 **Breaking changes** 확인
- 설정 deprecated 항목 점검
- 단계적 카나리 (소수 노드만 새 버전)

#### 다운그레이드

- 일반적으로 권장하지 않음 (블록 포맷 호환성 이슈 가능)
- 새 버전이 만든 블록은 이전 버전이 못 읽을 수 있음

---

### 성능 튜닝

#### Ingester

- `head.max_block_bytes`: 헤드 블록 크기 (기본 1.6 GB) — 메모리 사용량에 영향
- `head.max_block_duration`: 블록 보존 시간 (기본 1h) — 너무 짧으면 소규모 블록이 다수 생성됨
- WAL fsync 정책: `wal.flush_interval`

#### Querier

- `max_concurrent_queries`: 노드당 동시 쿼리 수
- `block_sync_concurrency`: 블록 메타데이터 동기화 병렬 처리 수

#### Compactor

- `compaction_concurrency`: 동시 컴팩션 수
- `compactor_blocks_retention_period`: 블록 보존 기간

#### Cache

- Memcached 사용을 권장합니다 (chunks_cache, metadata_cache).

```yaml
chunks_cache:
  backend: memcached
  memcached:
    addresses: dns+memcached.default.svc:11211
```

#### 쿼리 분할

```yaml
querier:
  query_split_interval: 24h
```

---

### 다음 단계

- [09_configuration.md](./09_configuration.md) - 설정 항목 상세
- [11_http_api.md](./11_http_api.md) - 운영 API
- [03_deployment.md](./03_deployment.md) - 규모별 배포

---

## 사용 사례 및 트러블슈팅

> 원본: https://grafana.com/docs/pyroscope/latest/

---

### 목차

1. [성능 회귀 탐지](#1-성능-회귀-탐지)
2. [CPU 핫스팟 분석](#2-cpu-핫스팟-분석)
3. [메모리 누수 진단](#3-메모리-누수-진단)
4. [고루틴 누수 (Go)](#4-고루틴-누수-go)
5. [클라우드 비용 절감](#5-클라우드-비용-절감)
6. [Tail Latency 디버깅](#6-tail-latency-디버깅)
7. [락 컨텐션 분석](#7-락-컨텐션-분석)
8. [Continuous Profiling 도입 단계](#continuous-profiling-도입-단계)

---

### 1. 성능 회귀 탐지

#### 시나리오

새로운 버전을 배포한 후 응답 시간이 미묘하게 늘었습니다. 메트릭 그래프로는 명확히 보이지만, 어느 코드 변경이 원인인지 모릅니다.

#### 절차

1. **Diff 뷰 사용**
   - 베이스라인: `service_name="checkout", version="v1.2.0"`
   - 비교: `service_name="checkout", version="v1.2.1"`
2. **빨간 박스 분석**
   - 가장 큰 빨간 박스 = 가장 비싸진 함수
3. **코드 변경과 매칭**
   - Git diff로 v1.2.0 → v1.2.1 변경 사항 중 그 함수 영역 확인
4. **수정 후 재검증**
   - v1.2.2 배포 후 v1.2.0 vs v1.2.2 Diff → 회귀 사라졌는지 확인

#### 자동화 아이디어

- CI에서 PR마다 staging 환경의 Pyroscope에 부하 테스트 프로파일을 보냄
- 메인 브랜치와 자동 Diff
- 일정 % 이상 회귀가 있으면 PR 코멘트로 알림

---

### 2. CPU 핫스팟 분석

#### 시나리오

CPU 사용률이 항상 80%를 넘고 스케일 아웃 비용이 큽니다. 어디를 최적화해야 할지 모릅니다.

#### 절차

1. **CPU 프로파일 수집**
   - `process_cpu` 또는 `cpu` 타입
2. **Table 뷰에서 Self 정렬**
   - 진짜 CPU를 직접 사용하는 leaf 함수 식별
3. **Top 5 후보 검토**
   - 정상인지(이미 최적화된 라이브러리), 개선 여지가 있는지 판단
4. **Sandwich 뷰로 호출 컨텍스트 확인**
   - 그 함수를 누가, 얼마나 자주 호출하는지

#### 흔히 발견되는 패턴

- **반복적인 정규식 컴파일**: `regexp.MustCompile` 을 매 요청마다 호출
- **JSON 마샬링/언마샬링 비용**: 캐시 가능한 결과를 매번 직렬화
- **로깅 비용**: 디버그 로그가 production 에서 활성화
- **암호화 핫스팟**: TLS handshake가 connection pooling 부재로 매번 발생

---

### 3. 메모리 누수 진단

#### 시나리오

서비스 메모리가 시간에 따라 계속 증가하고 결국 OOM Kill 됩니다.

#### 절차

1. **inuse_space 시계열 보기**
   - 시간이 지나며 어떤 함수의 살아있는 객체가 늘어나는가
2. **Diff: 시작 시점 vs 현재**
   - 빨간 박스 = 메모리를 보유 중인 함수
3. **alloc_space 와 비교**
   - alloc만 크고 inuse가 평탄하면 → GC가 잘 동작 (누수 아님)
   - alloc과 inuse가 동시에 증가 → 누수
4. **코드 점검**
   - 캐시가 만료되지 않거나
   - 글로벌 슬라이스/맵에 항목이 무한 추가되거나
   - 고루틴/타이머가 살아있어 클로저 변수가 잡혀 있거나

#### 사례

- `time.AfterFunc` 에 해제하지 않은 타이머
- HTTP 응답 본문을 close 하지 않아 connection이 살아있음
- `sync.Pool` 의 잘못된 사용으로 객체가 풀에 누적

---

### 4. 고루틴 누수 (Go)

#### 시나리오

서비스의 고루틴 수가 시간이 지나면서 계속 증가합니다. 결국 메모리/스케줄링 비용 증가로 OOM 또는 latency 폭증이 발생합니다.

#### 절차

1. **`goroutines` 프로파일 수집**
2. **시계열 차트**: 시간에 따른 고루틴 수 추세 확인
3. **flame graph**에서 가장 많은 비중을 차지하는 콜 스택 식별
4. **흔한 원인** 점검:
   - `chan` 송신/수신이 영원히 차단됨 (상대방이 close 안 함)
   - HTTP 클라이언트의 timeout 미설정 → 응답 대기 무한
   - `select` 의 default 케이스 부재
   - `time.After` 가 GC되지 않는 패턴

---

### 5. 클라우드 비용 절감

#### 시나리오

EC2/GCE 비용이 급증했고 일부 마이크로서비스의 CPU 사용량이 크게 늘었습니다. 가능한 코드 최적화로 노드 수를 줄이고 싶습니다.

#### 절차

1. **CPU 비용이 큰 서비스 Top N 식별**
   - 메트릭(`container_cpu_usage_seconds_total`)으로 후보 선정
2. **각 서비스의 CPU 프로파일 분석**
   - leaf 함수 self 비중 → 최적화 후보
3. **개선 → 측정 → 반복**
   - 개선 후 같은 서비스의 CPU 프로파일을 Diff
   - "코드 최적화로 CPU 평균 -23%" 처럼 정량 결과 산출

#### 사례 (실제 사례에서 자주 인용)

- 정규식 미리 컴파일로 30% 절감
- JSON 라이브러리 교체로 15% 절감
- HTTP keep-alive 활성화로 TLS handshake 비용 90% 감소

---

### 6. Tail Latency 디버깅

#### 시나리오

P50은 정상인데 P99가 가끔 10배 튑니다. 트레이스에서는 `process` 스팬이 길게 잡힙니다.

#### 절차

1. **트레이스에서 느린 요청 찾기**
   - Tempo에서 `duration > 1s` 검색
2. **해당 스팬 Span Profile 클릭**
   - 그 트레이스의 시간 윈도우 + 인스턴스 라벨로 Pyroscope 쿼리 자동 작성
3. **flame graph에서 비싼 박스 확인**
   - 일반적인 평균과 다른 패턴 찾기

#### 흔한 원인

- GC 멈춤 → `runtime.gcBgMarkWorker` 비중 ↑
- 락 컨텐션 → `mutex` 프로파일에서 박스 큰 락
- 외부 API timeout 후 retry → I/O 대기 (wall clock)
- 콜드 캐시 → 첫 요청에서만 큰 비용

---

### 7. 락 컨텐션 분석

#### 시나리오

CPU 여유는 충분한데 처리량이 늘지 않습니다. 멀티 코어가 제대로 활용되지 않는 것으로 의심됩니다.

#### 절차

1. **Mutex 프로파일 활성화** (Go: `runtime.SetMutexProfileFraction(5)`)
2. **flame graph**에서 큰 박스 = 컨텐션 큰 락
3. **Sandwich** 로 누가 그 락을 가지고/기다리고 있는지 분석

#### 개선 패턴

- **shard 락**: 하나의 큰 mutex → N개로 분할 (`sync.Map` 등)
- **read-write 분리**: `sync.RWMutex` 로 읽기 동시성 ↑
- **lock-free 자료구조**: atomic, channel
- **임계 영역 축소**: 락 내부에서 I/O를 하지 않기

---

### Continuous Profiling 도입 단계

조직에 Pyroscope를 도입할 때 권장 순서입니다.

#### 단계 1: 핵심 서비스부터 (1~2주)

- 비용이 가장 큰 서비스 1~2개에 SDK 또는 Alloy 도입
- 단일 인스턴스 Pyroscope (Monolithic) 운영
- 팀에 flame graph 읽는 법 교육

#### 단계 2: 표준화 (2~4주)

- 모든 서비스에 동일한 라벨 컨벤션 적용 (`service_name`, `env`, `cluster`)
- 자동 계측 → Alloy로 중앙 관리
- 첫 회귀 탐지 사례 만들기

#### 단계 3: 통합 (1~2개월)

- Tempo, Loki, Mimir 와 라벨 통일 → 신호 간 점프
- Span Profiles 활성화
- 대시보드/Explore Profiles 정착

#### 단계 4: 자동화 (지속)

- CI에서 PR별 자동 Diff
- 알람 규칙 (특정 함수가 임계 비중 넘으면)
- 정기적인 비용/성능 리뷰 미팅

---

### 다음 단계

- [05_flamegraphs.md](./05_flamegraphs.md) - 분석 도구 사용법
- [12_visualization.md](./12_visualization.md) - Grafana 통합
- [04_profile_types.md](./04_profile_types.md) - 어떤 프로파일을 쓸지

---

## Pyroscope 설정 (Configuration)

> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/reference-configuration-parameters/

---

### 목차

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

### 설정 파일 형식

YAML 파일로 작성하며, CLI 플래그와 환경 변수로 일부 값을 오버라이드할 수 있습니다.

```bash
pyroscope -config.file=pyroscope.yaml -target=all
```

#### 환경 변수 치환

```yaml
storage:
  s3:
    access_key_id: ${AWS_ACCESS_KEY_ID}
```

`-config.expand-env=true` 플래그로 활성화합니다.

#### 핫 리로드

`SIGHUP` 시그널로 일부 항목(limits 등)을 재로드할 수 있습니다. 단, 모든 항목이 핫 리로드를 지원하지는 않습니다.

---

### 최상위 블록 개요

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

### server 블록

HTTP/gRPC 리스너 설정입니다.

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

### storage 블록

오브젝트 스토리지 또는 로컬 파일시스템 설정입니다.

#### S3 (또는 호환)

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

#### GCS

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

#### Azure Blob

```yaml
storage:
  backend: azure
  azure:
    account_name: myaccount
    account_key: ${AZURE_KEY}
    container_name: pyroscope
```

#### 로컬 파일시스템 (개발/테스트용)

```yaml
storage:
  backend: filesystem
  filesystem:
    dir: /data
```

---

### memberlist / ring

해시 링과 멤버십 관리를 위한 gossip 설정입니다.

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

kvstore 대안으로 `consul`, `etcd`, `inmemory`를 사용할 수 있습니다.

---

### ingester 블록

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

pyroscopedb:
  data_path: /data/pyroscope
  max_block_duration: 1h
  row_group_target_size: 1342177280   # 1.25GiB
```

---

### querier / query-frontend / query-scheduler

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

### compactor 블록

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

### limits 블록

테넌트별 및 글로벌 한도 설정입니다. (자세한 설명은 [07_manage.md](./07_manage.md))

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

#### 테넌트별 오버라이드

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

### analytics와 자기 프로파일링

#### analytics

Grafana Labs에 익명 사용 통계를 전송합니다. 비활성화할 수 있습니다.

```yaml
analytics:
  reporting_enabled: false
```

#### self_profiling

Pyroscope 자신을 프로파일링합니다.

```yaml
self_profiling:
  enabled: true
  url: http://pyroscope:4040
  application_name: pyroscope
  sample_rate: 100
```

---

### 실전 예시 모음

#### 단일 노드 Monolithic + S3

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

pyroscopedb:
  data_path: /data/pyroscope
```

#### 마이크로서비스 + Memberlist + GCS

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

### 다음 단계

- [03_deployment.md](./03_deployment.md) - 배포 모드별 설정 차이
- [07_manage.md](./07_manage.md) - 운영 가이드
- [11_http_api.md](./11_http_api.md) - 상태 점검 API
