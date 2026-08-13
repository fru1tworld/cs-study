# Pyroscope CLI, HTTP API, 시각화

## Profilecli 도구

> 원본: https://grafana.com/docs/pyroscope/latest/configure-client/profile-cli/

---

### 목차

1. [profilecli란](#profilecli란)
2. [설치](#설치)
3. [전역 옵션](#전역-옵션)
4. [profile 관련 명령](#profile-관련-명령)
5. [bucket 관련 명령](#bucket-관련-명령)
6. [admin 명령](#admin-명령)
7. [실전 시나리오](#실전-시나리오)

---

### profilecli란

`profilecli`는 Pyroscope 서버와 상호작용하기 위한 공식 CLI 도구임.

- 로컬 pprof 파일을 서버로 업로드
- 서버에서 프로파일 다운로드/조회
- 오브젝트 스토리지 버킷 검사
- 운영 점검

서버 설정·디버깅 보조 도구이며, 일반 클라이언트 계측에는 SDK나 Alloy를 사용함.

---

### 설치

#### Pre-built 바이너리

릴리스 페이지에서 OS/아키텍처에 맞는 바이너리 다운로드.

```bash
curl -L -o profilecli \
  https://github.com/grafana/pyroscope/releases/download/<VERSION>/profilecli-linux-amd64
chmod +x profilecli
sudo mv profilecli /usr/local/bin/
```

#### Docker

```bash
docker run --rm grafana/profilecli:latest --help
```

#### 소스에서 빌드

```bash
git clone https://github.com/grafana/pyroscope.git
cd pyroscope
go build -o profilecli ./cmd/profilecli
```

---

### 전역 옵션

```
--url string              Pyroscope 서버 주소 (기본 http://localhost:4040)
--tenant-id string        X-Scope-OrgID 헤더 값
--username string         Basic Auth 사용자명 (Grafana Cloud 등)
--password string         Basic Auth 비밀번호 (또는 토큰)
--verbose                 상세 로깅
```

환경 변수로도 설정 가능.

```bash
export PROFILECLI_URL=http://pyroscope:4040
export PROFILECLI_TENANT_ID=team-a
```

---

### profile 관련 명령

#### upload — pprof 파일 업로드

로컬 pprof 파일(또는 표준 입력)을 Pyroscope 서버로 업로드.

```bash
profilecli upload \
  --url http://pyroscope:4040 \
  --tenant-id team-a \
  --extra-labels=service_name=batch-job \
  --extra-labels=env=staging \
  ./profile.pb.gz
```

옵션:

- `--extra-labels`: 추가할 라벨(반복 가능)
- `--from`, `--to`: 시간 범위(겹쳐 쓸 때)

#### query — 프로파일 조회

라벨 매처와 시간 범위로 프로파일을 가져옴. 결과는 표준 pprof 형식으로 출력됨 → `go tool pprof` 등으로 분석 가능.

```bash
profilecli query \
  --url http://pyroscope:4040 \
  --query='{service_name="checkout"}' \
  --profile-type=process_cpu:cpu:nanoseconds:cpu:nanoseconds \
  --from=now-1h --to=now \
  > profile.pb.gz
```

#### query series — 시리즈 목록

```bash
profilecli query series \
  --query='{service_name="checkout"}' \
  --from=now-1h --to=now
```

#### query labels / query label-values

라벨 키/값을 탐색함.

```bash
profilecli query labels --from=now-1h --to=now
profilecli query label-values --label=service_name
```

---

### bucket 관련 명령

#### bucket list-blocks

오브젝트 스토리지에 저장된 블록 목록을 출력.

```bash
profilecli bucket list-blocks \
  --object-store.backend=s3 \
  --object-store.s3.bucket-name=my-pyroscope \
  --tenant-id=team-a
```

#### bucket download-block

특정 블록을 로컬로 다운로드 (디버깅용).

```bash
profilecli bucket download-block \
  --tenant-id=team-a \
  --block-id=01HF...XYZ \
  --output-dir=./blocks
```

#### bucket inspect-block

블록 내부 메타데이터/통계 확인.

```bash
profilecli bucket inspect-block \
  --block-id=01HF...XYZ
```

---

### admin 명령

운영자용 관리 엔드포인트 래퍼 명령임.

#### admin tenant-stats

테넌트별 통계.

```bash
profilecli admin tenant-stats --tenant-id=team-a
```

#### admin user-stats

(레거시) 사용자별 통계.

#### admin flush

Ingester의 헤드 블록을 즉시 플러시함. 점검 또는 종료 전에 사용함.

---

### 실전 시나리오

#### 1) 로컬 pprof 디버깅을 서버에 업로드

```bash
# Go 표준 도구로 30초 CPU 프로파일 캡처
curl -o cpu.pprof http://localhost:6060/debug/pprof/profile?seconds=30

# Pyroscope에 업로드
profilecli upload \
  --url=http://pyroscope:4040 \
  --extra-labels=service_name=local-debug \
  --extra-labels=user=$USER \
  cpu.pprof
```

#### 2) 서버 프로파일을 받아 `go tool pprof` 로 분석

```bash
profilecli query \
  --query='{service_name="checkout"}' \
  --profile-type=process_cpu:cpu:nanoseconds:cpu:nanoseconds \
  --from=now-30m --to=now > recent.pprof

go tool pprof -http=:9000 recent.pprof
```

#### 3) 운영: 어떤 테넌트가 가장 많은 시리즈를 보유하는가

```bash
for t in $(profilecli admin tenants --output=names); do
  echo -n "$t: "
  profilecli admin tenant-stats --tenant-id=$t \
    --output=json | jq '.active_series'
done | sort -k2 -nr | head -10
```

#### 4) 손상 의심 블록 점검

```bash
profilecli bucket inspect-block --block-id=01HF...XYZ \
  --object-store.backend=s3 \
  --object-store.s3.bucket-name=my-pyroscope
```

---

### 다음 단계

- [11_http_api.md](./11_http_api.md) - HTTP API 상세
- [07_manage.md](./07_manage.md) - 운영 가이드

---

## Pyroscope HTTP API

> 원본: https://grafana.com/docs/pyroscope/latest/configure-server/about-the-http-api/

---

### 목차

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

### API 개요

Pyroscope는 두 종류 API를 제공함.

- Pyroscope OG API(HTTP/JSON 기반): `/ingest`, `/render`, `/labels`, `/label-values` 등
- Phlare/Connect API(HTTP/protobuf, gRPC 호환): `/push.v1.PusherService/Push`, `/querier.v1.QuerierService/...`

신규 SDK와 Alloy는 Connect API를 우선 사용하며, 하위 호환성을 위해 OG API도 유지됨.

---

### 공통 헤더와 에러

#### 멀티 테넌시

```
X-Scope-OrgID: team-a
```

`multitenancy_enabled: true`일 때 모든 요청에 필수.

#### 인증

서버 자체는 인증을 처리하지 않으며, 앞단 게이트웨이에서 담당함. Grafana Cloud는 Basic Auth 토큰 사용.

```
Authorization: Basic <base64(user:token)>
```

#### 에러 응답

```json
{ "code": 400, "message": "max label name length exceeded: ..." }
```

- 400: 검증 실패(라벨, 페이로드)
- 401: 인증 실패
- 403: 권한 부족
- 422: 쿼리 파싱/실행 오류
- 429: 인제스트/쿼리 한도 초과
- 500: 서버 오류

---

### Ingest API

레거시 호환을 위한 단순 ingest 엔드포인트임.

```
POST /ingest
```

#### 쿼리 파라미터

- `name`: 애플리케이션 이름 + 라벨(예: `checkout{env=prod}`)
- `from`: 시작 시간(unix sec)
- `until`: 종료 시간(unix sec)
- `format`: `pprof`(권장), `jfr`, `folded`
- `sampleRate`: 샘플링 레이트(Hz)
- `spyName`: SDK/agent 식별자
- `units`: `samples`, `bytes`, `objects`

#### 예시

```bash
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @cpu.pprof \
  "http://pyroscope:4040/ingest?name=checkout{env=prod}&format=pprof&sampleRate=100&from=$(date +%s -d '30s ago')&until=$(date +%s)"
```

---

### Push API (Connect/protobuf)

```
POST /push.v1.PusherService/Push
Content-Type: application/proto
```

요청 본문은 protobuf 메시지 `PushRequest`이며, 신규 SDK가 사용하는 표준 경로임.

#### 요청 메시지

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

### Query API

Phlare-style 쿼리. Connect 프로토콜.

#### 시리즈 목록

```
POST /querier.v1.QuerierService/Series
```

```json
{ "matchers": ["{service_name=\"checkout\"}"], "start": 1700000000000, "end": 1700003600000 }
```

#### 라벨 이름 / 값

```
POST /querier.v1.QuerierService/LabelNames
POST /querier.v1.QuerierService/LabelValues
```

#### 프로파일 타입 목록

```
POST /querier.v1.QuerierService/ProfileTypes
```

#### 프로파일 머지 조회

```
POST /querier.v1.QuerierService/SelectMergeProfile
```

응답으로 pprof 바이트 반환.

#### 스택 트리 (flame graph용)

```
POST /querier.v1.QuerierService/SelectMergeStacktraces
```

#### 시계열 (time series)

```
POST /querier.v1.QuerierService/SelectSeries
```

특정 라벨로 그룹화해 시간별 합계를 반환함.

---

### Render API

Pyroscope 내장 UI가 사용하는 렌더링 엔드포인트.

```
GET /render?query=<labels>&from=<ms>&until=<ms>&format=json
```

`format=json`이면 flame graph 트리 JSON, `format=pprof`이면 pprof 바이트.

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

### Settings / Admin API

#### Tenant 한도 조회

```
GET /api/v1/tenant_limits
```

#### 동적 한도 변경

```
PUT /api/v1/tenant_limits
```

#### 빌드 정보

```
GET /api/v1/status/buildinfo
```

응답:

```json
{ "version": "1.x.y", "branch": "...", "buildDate": "...", "goVersion": "..." }
```

---

### Status / Ready / Health

- `GET /ready`: 트래픽 수신 준비 완료(K8s readinessProbe)
- `GET /-/healthy`: 프로세스 살아있음(K8s livenessProbe)
- `GET /api/v1/status/config`: 현재 적용 중인 설정
- `GET /memberlist`: gossip 멤버십 상태
- `GET /distributor/ring`: distributor ring 상태
- `GET /ingester/ring`: ingester ring 상태
- `GET /compactor/ring`: compactor ring 상태
- `GET /store-gateway/ring`: store-gateway ring 상태

ring 페이지는 HTML로도 표시되므로 어떤 노드가 healthy/unhealthy 상태인지 직관적으로 확인 가능.

---

### Metrics

```
GET /metrics
```

Prometheus 형식의 자체 메트릭. 주요 지표:

#### Distributor

- `pyroscope_distributor_received_compressed_bytes_total{tenant=}`
- `pyroscope_distributor_received_decompressed_bytes_total{tenant=}`
- `pyroscope_distributor_received_samples_total{tenant=}`

#### Ingester

- `pyroscope_ingester_memory_series{tenant=}`
- `pyroscope_ingester_blocks_uploaded_total{tenant=}`
- `pyroscope_ingester_wal_corruption_total`
- `pyroscope_ingester_head_min_time / head_max_time`

#### Querier

- `pyroscope_querier_select_merge_profile_duration_seconds`
- `pyroscope_querier_blocks_queried_total`

#### 일반

- `pyroscope_request_duration_seconds{route, method, status_code}`
- `pyroscope_inflight_requests`
- `cortex_ring_members{state}` — 링 상태

---

### 사용 예: 헬스 체크 자동화

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

### 다음 단계

- [09_configuration.md](./09_configuration.md) - 서버 설정
- [10_pyroscope_cli.md](./10_pyroscope_cli.md) - profilecli 사용
- [07_manage.md](./07_manage.md) - 운영

---

## 시각화 및 Grafana 통합

> 원본: https://grafana.com/docs/grafana/latest/datasources/grafana-pyroscope/

---

### 목차

1. [Pyroscope 데이터 소스](#pyroscope-데이터-소스)
2. [Explore 뷰](#explore-뷰)
3. [Explore Profiles 앱](#explore-profiles-앱)
4. [대시보드 패널](#대시보드-패널)
5. [Trace ↔ Profile 연동 (Span Profiles)](#trace--profile-연동-span-profiles)
6. [Logs ↔ Profile 연동](#logs--profile-연동)
7. [Metrics ↔ Profile 연동 (Exemplars)](#metrics--profile-연동-exemplars)

---

### Pyroscope 데이터 소스

#### 추가 방법

Grafana → Connections → Data sources → "Grafana Pyroscope".

#### 주요 설정

- URL: `http://pyroscope:4040`(자체 호스팅) 또는 Grafana Cloud Profiles URL
- Auth: Basic Auth(Cloud는 user=stack id, password=토큰)
- Custom HTTP Headers: `X-Scope-OrgID: team-a`(멀티테넌트 시)
- Minimal step: 시계열의 최소 step(예: `15s`)

#### 멀티 테넌트 라우팅

테넌트마다 다른 데이터 소스를 등록하거나, 한 데이터 소스에 변수 기반 헤더 사용 가능.

#### Provisioning

```yaml
apiVersion: 1
datasources:
  - name: Pyroscope
    type: grafana-pyroscope-datasource
    access: proxy
    url: http://pyroscope:4040
    jsonData:
      keepCookies: []
    secureJsonData:
      basicAuthPassword: ${PYROSCOPE_TOKEN}
```

---

### Explore 뷰

Grafana Explore에서 Pyroscope 데이터 소스를 선택하면 다음 사용 가능.

#### 쿼리 빌더

- **Service**: 라벨 `service_name` 자동 완성
- **Profile Type**: `process_cpu`, `memory`, `goroutine` 등
- **Label filter**: 추가 라벨 매처

#### Display modes

- Profile: 단일 시점 / 시간 범위의 머지된 flame graph
- Time series: 시간에 따른 합계 시계열(선택 라벨로 그룹)
- Both: 위 두 가지 동시

#### Comparison Mode (Diff)

두 시간/라벨 셋을 비교하는 Diff flame graph.

---

### Explore Profiles 앱

`grafana-pyroscope-app` 플러그인은 Explore보다 풍부한 분석 UI를 제공함.

#### 설치

```bash
grafana-cli plugins install grafana-pyroscope-app
```

또는 Helm `GF_INSTALL_PLUGINS` 환경 변수.

#### 핵심 기능

- **Service overview**: 서비스 목록, 각 서비스의 CPU/메모리 상위 N 함수
- **Function detail**: 단일 함수의 호출자/피호출자, 시간 추세
- **Flame graph 분석**: Sandwich, Diff 뷰
- **Comparison**: 시간 비교, 라벨 비교
- **Favorites / Bookmarks**: 분석 컨텍스트 저장

#### 데이터 소스 선택

Explore Profiles는 등록된 Pyroscope 데이터 소스 중 하나를 사용함. 멀티 테넌트 환경이라면 상단에서 데이터 소스를 변경함.

---

### 대시보드 패널

#### Flame Graph 패널

- 패널 타입: **Flame Graph**
- 쿼리에서 라벨 셀렉터 + 프로파일 타입 지정
- Display mode: Flame, Table, Both

#### Time Series 패널

CPU 시간 합계, 메모리 inuse_space 등을 시계열로 표시.

```
sum by (service_name) (
  rate(process_cpu:cpu:nanoseconds:cpu:nanoseconds[$__rate_interval])
)
```

(Pyroscope 데이터 소스의 시계열 쿼리 형식)

#### 변수 사용

```
service_name = label_values(service_name)
env          = label_values(env)
```

대시보드 변수로 다양한 서비스/환경을 전환함.

---

### Trace ↔ Profile 연동 (Span Profiles)

트레이스의 특정 스팬에서 해당 시간/인스턴스의 프로파일로 이동하는 기능으로, Span Profiles라고 함.

#### 동작 원리

1. 애플리케이션이 트레이스 컨텍스트의 traceID/spanID를 프로파일 라벨에 포함하여 Pyroscope에 전송
2. Tempo의 트레이스 뷰에서 스팬의 시간 범위 + 인스턴스 라벨로 Pyroscope 쿼리 자동 생성
3. 같은 화면에서 flame graph 미니뷰가 표시됨

#### 활성화 (Go SDK 예시)

OTel + Pyroscope SDK 통합:

```go
import (
    "github.com/grafana/pyroscope-go/x/k6"   // 또는 OTel 통합 패키지
    pyroscope "github.com/grafana/pyroscope-go"
)

profiler, _ := pyroscope.Start(pyroscope.Config{
    ApplicationName: "checkout",
    ServerAddress:   "http://pyroscope:4040",
    // pprof 라벨에 trace context 추가
})
```

#### Tempo 데이터 소스 설정

Tempo 데이터 소스의 "Profiles" 탭에서 Pyroscope를 연결 대상으로 추가하면 스팬 상세 화면에서 자동으로 링크가 표시됨.

---

### Logs ↔ Profile 연동

Loki의 로그에서 Pyroscope 프로파일로 점프.

#### 데이터 소스 derived field

Loki 데이터 소스 설정에서 derived field 추가:

```
Name: profile_link
Regex: trace_id=(\w+)
URL: <Pyroscope explore URL>
Datasource: Pyroscope
```

또는 `service_name` 라벨을 공유해 Pyroscope에 자동으로 매칭할 수도 있음.

#### 단순 통합

Loki의 로그 라벨 `service_name`, `env`, `cluster`가 Pyroscope와 동일하다면 Explore에서 "Open in Pyroscope" 버튼이 자동으로 활성화됨.

---

### Metrics ↔ Profile 연동 (Exemplars)

Mimir/Prometheus의 메트릭에서 이상치 발견 시 관련 프로파일로 이동.

#### 동작 원리

- 메트릭의 exemplar에 프로파일 ID 또는 트레이스 ID를 포함
- Grafana 메트릭 패널에서 exemplar 점을 클릭하면 해당 프로파일로 점프

#### 일반적인 패턴

1. Grafana 대시보드에서 CPU/메모리 메트릭 그래프 표시
2. 이상 구간 발견 → 같은 시간 + 같은 라벨로 Pyroscope 쿼리
3. **Data link** 으로 자동화:

```
Title: View profile
URL: /a/grafana-pyroscope-app/.../service/${__field.labels.service_name}?from=${__from}&to=${__to}
```

---

### 통합 워크플로 예

#### 사례: 응답 지연 회귀 분석

1. **Mimir 대시보드**: P99 latency 그래프에서 회귀 발견 (15:23 부터 증가)
2. **Tempo Explore**: 그 시간대의 느린 트레이스 검색 (`duration > 1s`)
3. **Span 디테일**: 가장 긴 스팬의 "View profile" 클릭
4. **Pyroscope flame graph**: 해당 스팬 시간대의 CPU 프로파일 분석
5. **Diff**: 어제 같은 시간대와 비교 → 회귀 함수 식별
6. **Loki**: 그 함수의 로그 패턴 확인 → 가설 검증

이 모든 과정이 같은 Grafana UI 안에서 1~2분 내에 가능함.

---

### 라벨 통일 컨벤션

여러 신호 간 자연스러운 점프를 위한 권장 라벨:

- `service_name`: 예시 값 `checkout` · 용도 서비스 식별(필수)
- `service_namespace`: 예시 값 `payments` · 용도 서비스 그룹
- `env`: 예시 값 `prod`, `staging`, `dev` · 용도 환경
- `cluster`: 예시 값 `us-east-1` · 용도 클러스터
- `version`: 예시 값 `v1.2.3` · 용도 배포 버전(Diff에 핵심)
- `instance`: 예시 값 `host123` · 용도 인스턴스(고카디널리티 주의)

OTel Resource Attribute 표준(`service.name` 등)에 맞추는 것을 권장함. 라벨 변환 시 `.`은 `_`으로 사용함.

---

### 다음 단계

- [05_flamegraphs.md](./05_flamegraphs.md) - flame graph 분석법
- [08_use_cases.md](./08_use_cases.md) - 통합 트러블슈팅 사례
- [06_instrumentation.md](./06_instrumentation.md) - 라벨 설정
