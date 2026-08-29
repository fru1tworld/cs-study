# Tempo CLI, HTTP API, 시각화

## Tempo CLI

> 원본: https://grafana.com/docs/tempo/latest/operations/tempo_cli/

---

### 목차

1. [개요](#개요)
2. [설치](#설치)
3. [공통 옵션](#공통-옵션)
4. [list 명령](#list-명령)
5. [view 명령](#view-명령)
6. [search 명령](#search-명령)
7. [analyse 명령](#analyse-명령)
8. [migrate 명령](#migrate-명령)
9. [generate 명령](#generate-명령)
10. [기타 명령](#기타-명령)

---

### 개요

`tempo-cli` 는 Tempo의 백엔드 데이터를 직접 조회/관리하는 도구.

#### 주요 용도

- 블록 목록 조회
- 블록 메타데이터/내용 검사
- 손상된 블록 식별/삭제
- 트레이스 직접 조회
- 마이그레이션
- 테스트 데이터 생성

---

### 설치

#### 바이너리 다운로드

[GitHub Releases](https://github.com/grafana/tempo/releases) 에서 받기.

```bash
curl -O -L https://github.com/grafana/tempo/releases/latest/download/tempo-cli_linux_amd64.tar.gz
tar -xvzf tempo-cli_linux_amd64.tar.gz
chmod +x tempo-cli
sudo mv tempo-cli /usr/local/bin/
```

#### Docker

```bash
docker run --rm grafana/tempo:latest tempo-cli <command>
```

#### 소스에서 빌드

```bash
git clone https://github.com/grafana/tempo.git
cd tempo
make tempo-cli
```

---

### 공통 옵션

```bash
tempo-cli [global flags] <command> [command flags]
```

#### 백엔드 지정

```bash
--backend=s3              # local, s3, gcs, azure
--bucket=my-tempo-bucket
--config-file=/etc/tempo.yaml   # 또는 구성 파일에서
```

#### 백엔드별 옵션

##### S3

```bash
--s3-endpoint=s3.amazonaws.com
--s3-region=us-east-1
--s3-user=$AWS_ACCESS_KEY_ID
--s3-pass=$AWS_SECRET_ACCESS_KEY
```

##### GCS

```bash
--gcs-bucket-name=my-bucket
```

##### Azure

```bash
--azure-storage-account-name=myaccount
--azure-storage-account-key=$AZURE_KEY
--azure-container-name=tempo
```

#### 출력 옵션

```bash
--output=table   # table, json, yaml
--verbose
--quiet
```

---

### list 명령

#### `list blocks`

테넌트의 블록 목록 조회.

```bash
tempo-cli list blocks \
  --backend=s3 \
  --bucket=my-tempo \
  <tenant-id>
```

출력:
```
+----------------+-----------+-----------+----------+----------+
| ID             | Tenant    | Start     | End      | Size     |
+----------------+-----------+-----------+----------+----------+
| 01HZX...       | tenant-1  | 1700...   | 1700...  | 50MB     |
| 01HZW...       | tenant-1  | 1700...   | 1700...  | 48MB     |
+----------------+-----------+-----------+----------+----------+
```

#### `list cache-summary`

캐시 요약.

#### `list compaction-summary`

압축 통계.

```bash
tempo-cli list compaction-summary <tenant-id>
```

#### `list block`

특정 블록 상세.

```bash
tempo-cli list block <tenant-id> <block-id>
```

#### `list index`

블록의 인덱스 정보.

```bash
tempo-cli list index <tenant-id> <block-id>
```

---

### view 명령

#### `view index`

인덱스 내용 보기.

```bash
tempo-cli view index <tenant-id> <block-id>
```

#### `view summary`

블록 요약 정보.

```bash
tempo-cli view summary <tenant-id> <block-id>
```

출력:
- 트레이스 수
- 시간 범위
- 크기
- Parquet 버전
- 압축 레벨

#### `view block`

블록의 모든 트레이스 ID 출력.

```bash
tempo-cli view block <tenant-id> <block-id>
```

---

### search 명령

#### `search blocks`

블록에서 TraceQL 검색.

```bash
tempo-cli search blocks \
  --backend=s3 \
  --bucket=my-tempo \
  '{ resource.service.name = "frontend" && status = error }' \
  <tenant-id>
```

옵션:
```bash
--start=2024-01-01T00:00:00Z
--end=2024-01-02T00:00:00Z
--limit=20
```

#### 트레이스 ID로 직접 조회

```bash
tempo-cli query trace-id <trace-id> <tenant-id>
```

---

### analyse 명령

#### `analyse blocks`

블록 통계 분석.

```bash
tempo-cli analyse blocks \
  --backend=s3 \
  --bucket=my-tempo \
  <tenant-id>
```

분석 항목:
- 평균 블록 크기
- 트레이스/스팬 수 분포
- 압축 레벨 분포
- 시간 범위 분포

#### `analyse block`

특정 블록 분석.

```bash
tempo-cli analyse block <tenant-id> <block-id>
```

출력:
- 스팬 속성 카디널리티
- 자주 사용된 속성 키
- Parquet 컬럼별 크기
- Bloom 필터 통계

#### 활용

```bash
# Dedicated columns 후보 식별
tempo-cli analyse block <tenant-id> <block-id> | head -20
```

자주 사용되는 속성 → `parquet_dedicated_columns` 후보.

---

### migrate 명령

#### `migrate tenant`

테넌트 ID 변경 또는 데이터 이동.

```bash
tempo-cli migrate tenant <source-tenant> <dest-tenant> \
  --source-config-file=source-tempo.yaml \
  --config-file=dest-tempo.yaml
```

#### Parquet 버전 변환

Parquet 버전 변환 (예: vParquet3 → vParquet4, vParquet4 → vParquet5).

```bash
# vParquet3 → vParquet4
tempo-cli parquet convert-3to4 <in-file> [<out-path>]

# vParquet4 → vParquet5
tempo-cli parquet convert-4to5 <in-file> [<out-path>]
```

> 주의: Compactor가 자동으로 새 버전으로 변환하지만, 빠르게 변환하려면 이 명령 사용.

---

### generate 명령

#### `generate bloom`

블록의 Bloom 필터 재생성.

```bash
tempo-cli generate bloom <tenant-id> <block-id>
```

Bloom 필터가 손상되었을 때 복구에 사용.

#### `generate index`

블록 인덱스 재생성.

```bash
tempo-cli generate index <tenant-id> <block-id>
```

---

### 기타 명령

#### `gen` (테스트 데이터)

테스트용 트레이스 생성.

```bash
tempo-cli gen \
  --address=localhost:4317 \
  --service-name=test-service \
  --operation-name=test-op \
  --duration=100ms \
  --num-traces=10
```

#### `rollout-blocks`

블록 압축/배포 롤아웃 트리거 (드물게 사용).

#### `clear`

테넌트의 모든 블록 삭제 (위험).

```bash
tempo-cli clear --backend=s3 --bucket=my-tempo <tenant-id>
```

> 주의: 복구 불가능. 백업 후 사용.

#### `delete-block`

특정 블록만 삭제.

```bash
tempo-cli delete-block <tenant-id> <block-id>
```

블록이 손상되어 복구할 수 없을 때 사용.

#### `verify`

블록 무결성 검증.

```bash
tempo-cli verify <tenant-id> <block-id>
```

출력:
- 체크섬 검증
- 인덱스 정합성
- Parquet 스키마 호환성

---

### 운영 시나리오

#### 1. 손상된 블록 식별 및 처리

```bash
# 1. 손상 의심 블록 식별
tempo-cli verify <tenant-id> <block-id>

# 실패 시:
# 2. Bloom/인덱스 재생성 시도
tempo-cli generate bloom <tenant-id> <block-id>
tempo-cli generate index <tenant-id> <block-id>

# 3. 여전히 실패 시 삭제
tempo-cli delete-block <tenant-id> <block-id>
```

#### 2. 카디널리티 분석 (전용 컬럼 결정)

```bash
# 최근 블록 분석
tempo-cli list blocks <tenant-id> | head -5

# 각 블록의 속성 사용 빈도 분석
for block in $(tempo-cli list blocks <tenant-id> --output=json | jq -r '.[].id' | head -5); do
  tempo-cli analyse block <tenant-id> $block
done

# 자주 사용되는 속성 → tempo.yaml의 parquet_dedicated_columns에 추가
```

#### 3. 마이그레이션

```bash
# 1. 새 백엔드로 데이터 복사
aws s3 sync s3://old-tempo-bucket s3://new-tempo-bucket

# 2. 또는 tempo-cli로
tempo-cli migrate tenant tenant-1 tenant-1 \
  --source-config-file=old-tempo.yaml \
  --config-file=new-tempo.yaml

# 3. Tempo 구성 변경
# storage.trace.backend: gcs
# storage.trace.gcs.bucket_name: new-tempo-bucket

# 4. 재시작
```

#### 4. 디스크 사용량 분석

```bash
# 테넌트별 블록 크기 합계
tempo-cli list blocks <tenant-id> --output=json | \
  jq '[.[] | .size] | add'

# 가장 큰 블록 식별
tempo-cli list blocks <tenant-id> --output=json | \
  jq 'sort_by(.size) | reverse | .[0:10]'
```

#### 5. 트레이스 백필 (외부 검증)

```bash
# 1. 트레이스 ID 목록 (예: 의심 사례)
TRACE_IDS=("abc123" "def456" "ghi789")

# 2. 각각 조회
for tid in "${TRACE_IDS[@]}"; do
  echo "=== Trace: $tid ==="
  tempo-cli query trace-id $tid tenant-1
done
```

---

## Tempo HTTP API 레퍼런스

> 원본: https://grafana.com/docs/tempo/latest/api_docs/

---

### 목차

1. [개요](#개요)
2. [공통 사항](#공통-사항)
3. [트레이스 조회](#트레이스-조회)
4. [TraceQL 검색](#traceql-검색)
5. [태그/속성 조회](#태그속성-조회)
6. [TraceQL Metrics](#traceql-metrics)
7. [수집 API](#수집-api)
8. [관리/헬스 API](#관리헬스-api)
9. [Echo / Buildinfo](#echo--buildinfo)

---

### 개요

Tempo의 주요 HTTP API:

- 트레이스 조회 (TraceID 기반)
- TraceQL 검색
- 메트릭 쿼리 (TraceQL Metrics)
- 태그/속성 디스커버리

기본 포트: **3200**

---

### 공통 사항

#### 인증 헤더

멀티 테넌시를 활성화한 경우:
```
X-Scope-OrgID: <tenant-id>
```

#### 시간 형식

- 파라미터: `start`, `end`
  - 형식: Unix seconds 또는 RFC3339

#### 응답 코드

- 200: 성공
- 400: 잘못된 쿼리
- 404: 트레이스 없음
- 422: 처리 불가 (한도 초과)
- 429: Rate Limit
- 5xx: 서버 에러

---

### 트레이스 조회

#### `GET /api/traces/<traceID>`

특정 TraceID 조회.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://tempo:3200/api/traces/2c1b3d8f9e7a4c6b
```

##### 응답

OTLP/JSON 형식:

```json
{
  "batches": [
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "frontend"}}
        ]
      },
      "scopeSpans": [
        {
          "spans": [
            {
              "traceId": "...",
              "spanId": "...",
              "name": "HTTP GET /api/users",
              "startTimeUnixNano": "1700000000000000000",
              "endTimeUnixNano": "1700000000100000000",
              "attributes": [...]
            }
          ]
        }
      ]
    }
  ]
}
```

#### `GET /api/v2/traces/<traceID>`

확장된 V2 응답 형식.

옵션:
```bash
?start=1700000000        # 검색 범위 시작
?end=1700003600          # 검색 범위 종료
```

#### Trace 청크 응답

트레이스가 크면 여러 청크로 나뉘어 응답됨:

```json
{
  "trace": { "batches": [...] },
  "metrics": {
    "inspectedBytes": "1234567",
    "inspectedTraces": 5
  }
}
```

---

### TraceQL 검색

#### `GET /api/search`

TraceQL로 트레이스 검색.

```bash
curl -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={ resource.service.name = "frontend" && status = error }' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'limit=20' \
  --data-urlencode 'spss=10'
```

- `q`: TraceQL 쿼리
- `start`: 시작 시간
- `end`: 종료 시간
- `limit`: 반환할 트레이스 수
- `spss`: 트레이스당 최대 스팬 수

##### 응답

```json
{
  "traces": [
    {
      "traceID": "abc123...",
      "rootServiceName": "frontend",
      "rootTraceName": "HTTP GET /api/users",
      "startTimeUnixNano": "1700000000000000000",
      "durationMs": 150,
      "spanSets": [
        {
          "spans": [
            {
              "spanID": "...",
              "startTimeUnixNano": "...",
              "durationNanos": "1000000",
              "attributes": [...]
            }
          ],
          "matched": 1
        }
      ]
    }
  ],
  "metrics": {
    "inspectedTraces": 1234,
    "inspectedBytes": "5678901",
    "completedJobs": 10,
    "totalJobs": 10
  }
}
```

#### 레거시: `GET /api/search` (key=value)

기존 key=value 방식으로, 현재는 TraceQL 사용을 권장함:

```bash
curl -G "http://tempo:3200/api/search" \
  --data-urlencode 'service.name=frontend' \
  --data-urlencode 'minDuration=100ms' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600'
```

---

### 태그/속성 조회

#### `GET /api/search/tags`

사용 가능한 태그 키 목록.

```bash
curl -G "http://tempo:3200/api/search/tags" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'scope=resource'   # span, resource, intrinsic, all
```

응답:
```json
{
  "tagNames": ["service.name", "service.namespace", "cluster", "region"]
}
```

#### `GET /api/v2/search/tags`

V2: scope별로 분리된 응답.

```bash
curl -G "http://tempo:3200/api/v2/search/tags" \
  -H "X-Scope-OrgID: tenant-1"
```

```json
{
  "scopes": [
    {
      "name": "span",
      "tags": ["http.method", "http.status_code", ...]
    },
    {
      "name": "resource",
      "tags": ["service.name", "k8s.namespace.name", ...]
    },
    {
      "name": "intrinsic",
      "tags": ["name", "duration", "status", ...]
    }
  ]
}
```

#### `GET /api/search/tag/<tag>/values`

특정 태그의 값 목록.

```bash
curl -G "http://tempo:3200/api/search/tag/service.name/values" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.cluster="us-east-1"}'   # 옵션: 필터
```

응답:
```json
{
  "tagValues": ["frontend", "backend", "checkout", "payment"]
}
```

#### `GET /api/v2/search/tag/<tag>/values`

타입 정보가 포함됨:

```json
{
  "tagValues": [
    {"type": "string", "value": "frontend"},
    {"type": "string", "value": "backend"}
  ]
}
```

---

### TraceQL Metrics

#### `GET /api/metrics/query`

Instant 메트릭 쿼리.

```bash
curl -G "http://tempo:3200/api/metrics/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'time=1700000000'
```

#### `GET /api/metrics/query_range`

Range 메트릭 쿼리.

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=60s'
```

##### 응답 (Prometheus 호환)

```json
{
  "status": "success",
  "data": {
    "resultType": "matrix",
    "result": [
      {
        "metric": {"__name__": "rate"},
        "values": [
          [1700000000, "12.5"],
          [1700000060, "13.1"]
        ]
      }
    ]
  }
}
```

#### Metric 함수 예시

```bash
# 비율
?q={status=error} | rate()

# 시간 윈도우 카운트
?q={status=error} | count_over_time()

# 분위수
?q={resource.service.name="api"} | quantile_over_time(duration, 0.95)

# 히스토그램
?q={resource.service.name="api"} | histogram_over_time(duration)

# 그룹화
?q={status=error} | rate() by (resource.service.name)

# 비교
?q={status=error} | compare({status=ok})
```

---

### 수집 API

#### OTLP

##### gRPC: `4317`

OpenTelemetry SDK에서 gRPC로 직접 전송함.

##### HTTP: `4318`

```bash
curl -X POST -H "Content-Type: application/x-protobuf" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary @traces.pb \
  http://tempo:4318/v1/traces
```

#### Jaeger

##### Thrift HTTP: `14268`

```bash
curl -X POST -H "Content-Type: application/x-thrift" \
  --data-binary @jaeger.thrift \
  http://tempo:14268/api/traces
```

##### gRPC: `14250`

##### Thrift Compact UDP: `6831`
##### Thrift Binary UDP: `6832`

#### Zipkin: `9411`

```bash
curl -X POST -H "Content-Type: application/json" \
  -d '[{"id":"abc","traceId":"def","name":"op","timestamp":1700000000000000,"duration":100000}]' \
  http://tempo:9411/api/v2/spans
```

#### OpenCensus: `55678`

레거시 프로토콜.

---

### 관리/헬스 API

#### `GET /ready`

준비 상태.

```bash
curl http://tempo:3200/ready
# Ready
```

#### `GET /metrics`

Prometheus 형식 자체 메트릭.

#### `GET /config`

현재 활성 구성을 반환함. 일부 필드는 마스킹됨.

```bash
curl http://tempo:3200/config
```

#### `GET /services`

실행 중인 서비스 목록.

```bash
curl http://tempo:3200/services
```

응답:
```
distributor => Running
ingester => Running
querier => Running
query-frontend => Running
compactor => Running
```

#### `GET /memberlist`

Memberlist 가십 클러스터 상태.

```bash
curl http://tempo:3200/memberlist
```

#### `GET /ingester/ring`

Ingester 해시 링.

```bash
curl http://tempo:3200/ingester/ring
```

#### `GET /compactor/ring`

Compactor 해시 링.

#### `GET /distributor/ring`

Distributor 해시 링 (HA Tracker).

#### `GET /metrics-generator/ring`

Metrics Generator 해시 링.

#### `POST /shutdown`

정상 종료 (블록 플러시 후).

```bash
curl -X POST http://tempo:3200/shutdown
```

#### `GET /flush`

Ingester의 메모리 트레이스를 즉시 플러시함.

```bash
curl http://tempo:3200/flush
```

#### `GET /debug/pprof/*`

Go pprof 프로파일 (디버깅).

```bash
curl http://tempo:3200/debug/pprof/heap > heap.pprof
curl http://tempo:3200/debug/pprof/profile?seconds=30 > cpu.pprof

go tool pprof heap.pprof
```

---

### Echo / Buildinfo

#### `GET /api/echo`

수신한 헤더와 본문을 그대로 응답함 (디버깅용).

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "X-Custom: value" \
  http://tempo:3200/api/echo
```

#### `GET /api/status/buildinfo`

Tempo 버전 정보.

```bash
curl http://tempo:3200/api/status/buildinfo
```

응답:
```json
{
  "version": "2.4.0",
  "branch": "HEAD",
  "buildDate": "2024-04-01",
  "revision": "abc1234",
  "buildUser": "ci"
}
```

#### `GET /api/overrides`

현재 적용 중인 테넌트별 overrides를 조회함 (관리자용).

---

### API 사용 예시 모음

#### 1. 에러 트레이스 모니터링

```bash
# 최근 1시간 에러 트레이스 수
curl -G "http://tempo:3200/api/metrics/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={status=error} | rate()' \
  --data-urlencode 'time='$(date -d '1 hour ago' +%s)
```

#### 2. 서비스별 P95 지연

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name=~".+"} | quantile_over_time(duration, 0.95) by (resource.service.name)' \
  --data-urlencode 'start='$(date -d '1 hour ago' +%s) \
  --data-urlencode 'end='$(date +%s) \
  --data-urlencode 'step=60s'
```

#### 3. 자동화 스크립트: 느린 트레이스 알림

```bash
#!/bin/bash
SLOW_TRACES=$(curl -s -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={duration > 5s}' \
  --data-urlencode "start=$(date -d '5 min ago' +%s)" \
  --data-urlencode "end=$(date +%s)" \
  --data-urlencode 'limit=100' | jq '.traces | length')

if [ "$SLOW_TRACES" -gt 10 ]; then
  echo "ALERT: $SLOW_TRACES slow traces detected!"
fi
```

#### 4. CI/CD에 통합 (Smoke Test)

```bash
# 배포 후 새 버전의 에러 트레이스 확인
ERROR_COUNT=$(curl -s -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "q={resource.service.version=\"$VERSION\" && status=error}" \
  --data-urlencode "start=$(date -d '5 min ago' +%s)" \
  --data-urlencode "end=$(date +%s)" | jq '.traces | length')

if [ "$ERROR_COUNT" -gt 0 ]; then
  echo "Deployment validation failed: $ERROR_COUNT error traces"
  exit 1
fi
```

---

## Tempo 시각화 (Grafana 통합)

> 원본: https://grafana.com/docs/tempo/latest/getting-started/tempo-in-grafana/

---

### 목차

1. [개요](#개요)
2. [Tempo 데이터소스 추가](#tempo-데이터소스-추가)
3. [Explore에서 트레이스 보기](#explore에서-트레이스-보기)
4. [Service Graph](#service-graph)
5. [Node Graph](#node-graph)
6. [Span 패널](#span-패널)
7. [데이터소스 간 연결](#데이터소스-간-연결)
8. [Traces Drilldown (Plugin)](#traces-drilldown-plugin)
9. [APM 대시보드](#apm-대시보드)
10. [공식 대시보드](#공식-대시보드)

---

### 개요

Tempo와 Grafana 통합으로:

- **트레이스 검색** (TraceID, TraceQL)
- **트레이스 시각화** (Span 시계열, Timeline)
- **Service Graph** (자동 의존성 그래프)
- **로그/메트릭 연결** (Trace ↔ Logs ↔ Metrics)
- **APM 대시보드** (Span Metrics)

---

### Tempo 데이터소스 추가

#### UI에서

1. **Connections** → **Data sources** → **Add new**
2. **Tempo** 선택
3. URL: `http://tempo:3200`
4. **Save & test**

#### Provisioning

```yaml
# /etc/grafana/provisioning/datasources/tempo.yaml
apiVersion: 1

datasources:
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    uid: tempo
    
    jsonData:
      # 멀티 테넌시
      httpHeaderName1: X-Scope-OrgID
      
      # Trace to logs (Tempo → Loki)
      tracesToLogsV2:
        datasourceUid: loki
        spanStartTimeShift: '-1m'
        spanEndTimeShift: '1m'
        tags:
          - key: 'service.name'
            value: 'service'
          - key: 'k8s.namespace.name'
            value: 'namespace'
        filterByTraceID: true
        filterBySpanID: false
        customQuery: true
        query: '{$${__tags}} |= "$${__span.traceId}"'
      
      # Trace to metrics (Tempo → Mimir)
      tracesToMetrics:
        datasourceUid: mimir
        spanStartTimeShift: '-2m'
        spanEndTimeShift: '2m'
        tags:
          - key: 'service.name'
            value: 'service'
        queries:
          - name: 'Sample query'
            query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}}[5m]))'
      
      # Service Map
      serviceMap:
        datasourceUid: mimir
      
      # Node Graph
      nodeGraph:
        enabled: true
      
      # Span bar (스팬 표시 옵션)
      spanBar:
        type: 'Tag'
        tag: 'http.method'
      
      # Streaming search (실험적)
      streamingEnabled:
        search: true
        metrics: true
      
      # Trace Drilldown
      traceQuery:
        timeShiftEnabled: true
        spanStartTimeShift: '-1h'
        spanEndTimeShift: '1h'
    
    secureJsonData:
      httpHeaderValue1: 'tenant-1'
```

---

### Explore에서 트레이스 보기

#### Query Type

Tempo Explore는 4가지 쿼리 타입을 제공함:

- Search: TraceQL로 검색
- TraceID: 특정 트레이스 직접 조회
- Service Graph: 서비스 그래프
- Search (legacy): key=value 형식 (구식)

#### TraceQL Search

```traceql
{ resource.service.name = "frontend" && status = error && duration > 100ms }
```

##### Query Builder

UI로 조건 클릭하며 작성:
- 서비스
- 스팬 이름
- 속성
- 시간/지속
- 상태

#### TraceID 조회

직접 입력하거나 다른 데이터에서 클릭으로 이동.

#### 결과 표시

검색 결과:
- 트레이스 ID
- Root Service Name
- Root Span Name
- Start Time
- Duration

행 클릭 → 트레이스 상세 (Timeline view).

---

### Service Graph

#### 자동 생성

Tempo Metrics Generator가 활성화되어 있으면 자동으로 서비스 그래프 메트릭 생성.

#### Grafana에서 보기

Explore → Tempo → **Service Graph** 탭.

#### 그래프 요소

```
┌──────────┐  요청 수    ┌──────────┐
│ frontend │ ─────────> │   api    │
└──────────┘  에러율    └──────────┘
                │ P95 지연    │
                └──────────────┘
                   │
                   v
           ┌──────────────┐
           │  postgresql  │
           └──────────────┘
```

각 노드/엣지에 마우스 오버하면 메트릭 표시:
- 분당 요청 수
- 에러율
- P50/P95/P99 지연

#### 노드 클릭 → 액션

- 해당 서비스의 트레이스 검색
- Span Metrics 패널로 이동
- 관련 로그 조회 (Loki 연결 시)

---

### Node Graph

대시보드 패널로도 사용 가능.

#### 패널 추가

1. Add visualization → **Node Graph**
2. 데이터소스: Tempo (또는 Mimir)
3. 쿼리:
```promql
# Mimir 데이터소스 사용 시
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
```

#### 활용

- 대시보드에 영구 표시
- 시간 범위에 따른 변화 관찰
- 변수와 결합 (네임스페이스/클러스터별)

---

### Span 패널

#### Trace View 패널

대시보드에 트레이스를 직접 표시.

```yaml
# 패널 쿼리
queryType: traceID
query: ${__data.fields.traceID}
```

#### Span 시계열 (Span Bar Display)

각 스팬을 가로 막대로 표시. 시간 축으로 정렬.

#### Span 속성 표시

`spanBar` 설정으로 막대에 추가 정보 표시:

```yaml
spanBar:
  type: Tag           # None, Duration, Tag
  tag: http.method
```

각 스팬 옆에 HTTP method 표시.

---

### 데이터소스 간 연결

#### Logs ↔ Traces

##### Tempo → Loki (Trace to Logs)

트레이스를 보다가 관련 로그로 이동:

```yaml
tracesToLogsV2:
  datasourceUid: loki
  spanStartTimeShift: '-1m'
  spanEndTimeShift: '1m'
  tags:
    - key: service.name
      value: service
  customQuery: true
  query: '{$${__tags}} |= "$${__span.traceId}"'
```

스팬 우측 메뉴 → **Logs for this span** 클릭 → Loki 쿼리로 이동.

##### Loki → Tempo (Derived Fields)

로그에서 trace_id 클릭 → 트레이스로 이동:

```yaml
# Loki 데이터소스
derivedFields:
  - name: TraceID
    matcherRegex: '"trace_id":"(\w+)"'
    url: '$${__value.raw}'
    datasourceUid: tempo
```

#### Metrics ↔ Traces

##### Tempo → Mimir (Trace to Metrics)

```yaml
tracesToMetrics:
  datasourceUid: mimir
  spanStartTimeShift: '-2m'
  spanEndTimeShift: '2m'
  tags:
    - key: service.name
      value: service
  queries:
    - name: Request Rate
      query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}}[5m]))'
    - name: Error Rate
      query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}, status_code="STATUS_CODE_ERROR"}[5m]))'
```

##### Mimir → Tempo (Exemplars)

메트릭 차트의 점에 trace ID가 포함됨:

```yaml
# Mimir 데이터소스
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
```

차트 점을 클릭하면 해당 트레이스로 이동.

---

### Traces Drilldown (Plugin)

#### 개요

쿼리 없이 클릭만으로 트레이스를 탐색하는 Grafana 플러그인.

#### 설치

```bash
grafana-cli plugins install grafana-exploretraces-app
```

또는 Grafana Cloud에서 자동 활성화.

#### 사용

1. Grafana → **Explore** → **Traces Drilldown**
2. 시간 범위 선택
3. 자동 표시:
   - 서비스 목록과 RED 메트릭
   - 가장 느린/에러 많은 서비스 강조
4. 서비스 클릭 → 해당 서비스의 스팬 분석
5. 스팬 클릭 → 트레이스 상세

#### 활용

- 사전 지식 없이도 시스템 파악 가능
- 이상 자동 강조
- 문제 디버깅 시작점

---

### APM 대시보드

#### Tempo + Span Metrics 활용

Metrics Generator로 생성된 메트릭으로 APM 대시보드 구성.

#### 핵심 패널

##### 1. RED 메트릭 (서비스별)

**Rate**:
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

**Errors**:
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

**Duration P95**:
```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket[5m])
  )
)
```

##### 2. Service Graph 패널

Node Graph 패널 사용.

##### 3. Top N 느린 엔드포인트

```promql
topk(10,
  histogram_quantile(0.95,
    sum by (service, span_name, le) (
      rate(traces_spanmetrics_latency_bucket[5m])
    )
  )
)
```

##### 4. 의존성 변화 (변경 감지)

```promql
# 새 의존성 출현
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
unless
sum by (client, server) (
  rate(traces_service_graph_request_total[1h] offset 1h)
)
```

#### 변수

```
$service = label_values(traces_spanmetrics_calls_total, service)
$cluster = label_values(traces_spanmetrics_calls_total, cluster)
```

---

### 공식 대시보드

#### Tempo Mixin

```bash
jb install github.com/grafana/tempo/operations/tempo-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

포함:
- **Tempo / Operational**: 컴포넌트 운영
- **Tempo / Reads**: 검색/조회 경로
- **Tempo / Writes**: 수집 경로
- **Tempo / Resources**: CPU/메모리/네트워크
- **Tempo / Tenants**: 테넌트별 사용량

#### Grafana.com 대시보드

검색: https://grafana.com/grafana/dashboards/?search=tempo

추천:
- Tempo Operational
- APM Dashboard (Tempo + Mimir)
- Service Graph Dashboard

#### Import

```
Dashboards → Import → grafana.com → ID 입력
```

#### 커스텀 대시보드 모범 사례

1. **메트릭 + 트레이스 + 로그 한 화면**
   - 좌측: 시계열 메트릭
   - 중간: 트레이스 검색
   - 우측: 관련 로그
2. **시간 범위 동기화**: 모든 패널이 같은 시간 범위
3. **변수 활용**: `$service`, `$env` 등 드릴다운 가능
4. **Exemplar 활성화**: 메트릭에서 트레이스로 빠른 점프
