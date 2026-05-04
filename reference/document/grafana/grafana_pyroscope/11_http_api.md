# Pyroscope HTTP API

> 이 문서는 Pyroscope 서버가 노출하는 HTTP API 엔드포인트와 사용법을 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/about-the-http-api/

---

## 목차

1. [API 개요](#api-개요)
2. [공통 헤더와 에러](#공통-헤더와-에러)
3. [Ingest API](#ingest-api)
4. [Push API (Connect/protobuf)](#push-api-connectprotobuf)
5. [Query API](#query-api)
6. [Render API](#render-api)
7. [Settings / Admin API](#settings--admin-api)
8. [Status / Ready / Health](#status--ready--health)
9. [Metrics](#metrics)

---

## API 개요

Pyroscope는 두 종류 API를 제공합니다.

- **Pyroscope OG API** (HTTP/JSON 기반): `/ingest`, `/render`, `/labels`, `/label-values` 등
- **Phlare/Connect API** (HTTP/protobuf, gRPC 호환): `/push.v1.PusherService/Push`, `/querier.v1.QuerierService/...`

신규 SDK와 Alloy는 Connect API를 우선 사용하고, 호환성을 위해 OG API도 유지됩니다.

---

## 공통 헤더와 에러

### 멀티 테넌시

```
X-Scope-OrgID: team-a
```

`multitenancy_enabled: true` 일 때 모든 요청에 필수.

### 인증

서버 자체는 인증을 하지 않으며, 앞단 게이트웨이가 처리합니다. Grafana Cloud는 Basic Auth 토큰 사용.

```
Authorization: Basic <base64(user:token)>
```

### 에러 응답

```json
{ "code": 400, "message": "max label name length exceeded: ..." }
```

| 코드 | 의미 |
|------|------|
| 400 | 검증 실패 (라벨, 페이로드) |
| 401 | 인증 실패 |
| 403 | 권한 부족 |
| 422 | 쿼리 파싱/실행 오류 |
| 429 | 인제스트/쿼리 한도 초과 |
| 500 | 서버 오류 |

---

## Ingest API

레거시 호환의 단순한 ingest 엔드포인트.

```
POST /ingest
```

### 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `name` | 애플리케이션 이름 + 라벨 (예: `checkout{env=prod}`) |
| `from` | 시작 시간(unix sec) |
| `until` | 종료 시간(unix sec) |
| `format` | `pprof` (권장), `jfr`, `folded` |
| `sampleRate` | 샘플링 레이트 (Hz) |
| `spyName` | SDK/agent 식별자 |
| `units` | `samples`, `bytes`, `objects` |

### 예시

```bash
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @cpu.pprof \
  "http://pyroscope:4040/ingest?name=checkout{env=prod}&format=pprof&sampleRate=100&from=$(date +%s -d '30s ago')&until=$(date +%s)"
```

---

## Push API (Connect/protobuf)

```
POST /push.v1.PusherService/Push
Content-Type: application/proto
```

요청 본문은 protobuf 메시지 `PushRequest`. 새 SDK들이 사용하는 표준 경로입니다.

### 요청 메시지

```protobuf
message PushRequest {
  repeated RawProfileSeries series = 1;
}

message RawProfileSeries {
  repeated Label labels = 1;
  repeated RawSample samples = 2;
}

message RawSample {
  bytes raw_profile = 1;
  string id = 2;
}
```

`raw_profile`은 gzip 압축된 pprof 바이트.

---

## Query API

Phlare-style 쿼리. Connect 프로토콜.

### 시리즈 목록

```
POST /querier.v1.QuerierService/Series
```

```json
{ "matchers": ["{service_name=\"checkout\"}"], "start": 1700000000000, "end": 1700003600000 }
```

### 라벨 이름 / 값

```
POST /querier.v1.QuerierService/LabelNames
POST /querier.v1.QuerierService/LabelValues
```

### 프로파일 타입 목록

```
POST /querier.v1.QuerierService/ProfileTypes
```

### 프로파일 머지 조회

```
POST /querier.v1.QuerierService/SelectMergeProfile
```

응답으로 pprof 바이트 반환.

### 스택 트리 (flame graph용)

```
POST /querier.v1.QuerierService/SelectMergeStacktraces
```

### 시계열 (time series)

```
POST /querier.v1.QuerierService/SelectSeries
```

특정 라벨로 그룹 지어 시간별 합 반환.

---

## Render API

Pyroscope 내장 UI가 사용하는 렌더링 엔드포인트.

```
GET /render?query=<labels>&from=<ms>&until=<ms>&format=json
```

`format=json` 이면 flame graph 트리 JSON, `format=pprof` 이면 pprof 바이트.

```bash
curl "http://pyroscope:4040/render?query={service_name=\"checkout\"}&from=now-1h&until=now&format=json"
```

응답 예 (간략):

```json
{
  "flamebearer": {
    "names": [...],
    "levels": [...],
    "numTicks": 1500,
    "maxSelf": 234
  },
  "metadata": {
    "format": "single",
    "sampleRate": 100,
    "spyName": "gospy",
    "units": "samples"
  }
}
```

---

## Settings / Admin API

### Tenant 한도 조회

```
GET /api/v1/tenant_limits
```

### 동적 한도 변경

```
PUT /api/v1/tenant_limits
```

### 빌드 정보

```
GET /api/v1/status/buildinfo
```

응답:

```json
{ "version": "1.x.y", "branch": "...", "buildDate": "...", "goVersion": "..." }
```

---

## Status / Ready / Health

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /ready` | 트래픽 수신 준비 완료 (K8s readinessProbe) |
| `GET /-/healthy` | 프로세스 살아있음 (K8s livenessProbe) |
| `GET /api/v1/status/config` | 현재 적용 중인 설정 |
| `GET /memberlist` | gossip 멤버십 상태 |
| `GET /distributor/ring` | distributor ring 상태 |
| `GET /ingester/ring` | ingester ring 상태 |
| `GET /compactor/ring` | compactor ring 상태 |
| `GET /store-gateway/ring` | store-gateway ring 상태 |

ring 페이지는 HTML로도 표시되어 어떤 노드가 healthy/unhealthy 인지 직관적으로 확인 가능.

---

## Metrics

```
GET /metrics
```

Prometheus 형식의 자체 메트릭. 주요 지표:

### Distributor

- `pyroscope_distributor_received_compressed_bytes_total{tenant=}`
- `pyroscope_distributor_received_decompressed_bytes_total{tenant=}`
- `pyroscope_distributor_received_samples_total{tenant=}`

### Ingester

- `pyroscope_ingester_memory_series{tenant=}`
- `pyroscope_ingester_blocks_uploaded_total{tenant=}`
- `pyroscope_ingester_wal_corruption_total`
- `pyroscope_ingester_head_min_time / head_max_time`

### Querier

- `pyroscope_querier_select_merge_profile_duration_seconds`
- `pyroscope_querier_blocks_queried_total`

### 일반

- `pyroscope_request_duration_seconds{route, method, status_code}`
- `pyroscope_inflight_requests`
- `cortex_ring_members{state}` — 링 상태

---

## 사용 예: 헬스 체크 자동화

```bash
#!/bin/bash
URL=http://pyroscope:4040

curl -fs $URL/ready || { echo "not ready"; exit 1; }
curl -fs $URL/-/healthy || { echo "not healthy"; exit 1; }

UNHEALTHY=$(curl -s "$URL/ingester/ring" | grep -c "Unhealthy")
if [ "$UNHEALTHY" -gt 0 ]; then
  echo "ingester ring has unhealthy members"; exit 1
fi
echo "OK"
```

---

## 다음 단계

- [09_configuration.md](./09_configuration.md) - 서버 설정
- [10_pyroscope_cli.md](./10_pyroscope_cli.md) - profilecli 사용
- [07_manage.md](./07_manage.md) - 운영
