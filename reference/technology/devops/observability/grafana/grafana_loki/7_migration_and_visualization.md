# Loki 스토리지 마이그레이션과 시각화

## Loki 스토리지 마이그레이션

> 원본: https://grafana.com/docs/loki/latest/operations/storage/

---

### 목차

1. [스토리지 종류 개요](#스토리지-종류-개요)
2. [TSDB로 마이그레이션 (BoltDB → TSDB)](#tsdb로-마이그레이션-boltdb--tsdb)
3. [오브젝트 스토리지 변경](#오브젝트-스토리지-변경)
4. [스키마 버전 마이그레이션](#스키마-버전-마이그레이션)
5. [Single Store에서 분리 모드로](#single-store에서-분리-모드로)
6. [Cortex/Loki 1.x → 2.x → 3.x](#cortexloki-1x--2x--3x)
7. [클러스터 간 데이터 이동](#클러스터-간-데이터-이동)
8. [백업과 복구](#백업과-복구)

---

### 스토리지 종류 개요

#### 인덱스 스토어

- `tsdb`: 상태 안정 → 권장(허용)
- `boltdb-shipper`: 상태 Deprecated → 마이그레이션 권장(금지, 마이그레이션 필요)
- `boltdb`: 상태 매우 오래됨 → 금지
- `cassandra`: 상태 Deprecated → 금지
- `bigtable`: 상태 Deprecated → 금지
- `dynamodb`: 상태 Deprecated → 금지
- `aws-dynamo`: 상태 Deprecated → 금지

#### 청크 스토어 (Object Store)

- AWS S3: 허용
- GCS: 허용
- Azure Blob: 허용
- MinIO: 허용(S3 호환)
- Filesystem: 개발/테스트 용도만
- Swift: 허용
- Alibaba OSS: 허용
- Baidu BOS: 허용

---

### TSDB로 마이그레이션 (BoltDB → TSDB)

#### 왜 TSDB인가?

- 압축률 향상: 일반적으로 20~30% 작은 인덱스
- 쿼리 성능: 라벨 매칭 속도 향상
- 샤딩 지원: 쿼리 샤딩 자동 지원
- Bloom 필터 지원: 신규 기능 활용 가능

#### 마이그레이션 전략: 점진적 (Cutover Date)

기존 데이터는 그대로 유지 → 특정 날짜부터 새 스키마 적용.

```yaml
schema_config:
  configs:
    # 기존 BoltDB 스키마 (그대로 유지)
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: s3
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    # 새 TSDB 스키마 (미래 날짜)
    - from: 2024-06-15        # 주의: 반드시 미래 날짜
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
```

#### 주의사항

1. `from` 날짜는 반드시 미래 → 과거 날짜 사용 시 데이터 손실 위험
2. 여유 시간 확보 필요 → 최소 24시간 이후 날짜 권장(모든 인스턴스가 새 설정을 수신할 시간 확보 목적)
3. 모든 컴포넌트 동시 업데이트 필요 → Ingester·Querier·Compactor 전체 적용

#### TSDB Shipper 구성

```yaml
storage_config:
  tsdb_shipper:
    active_index_directory: /loki/tsdb-index
    cache_location: /loki/tsdb-cache
    cache_ttl: 24h
    
    index_gateway_client:
      server_address: dns:///index-gateway:9095
```

#### 검증

```bash
# 새 인덱스 디렉토리 확인
ls -la s3://my-bucket/tsdb_index_*

# 쿼리 작동 확인 (양쪽 시기 모두)
curl "http://loki:3100/loki/api/v1/query_range?query={app=\"test\"}&start=2023-12-01&end=2024-12-01"
```

#### 점진적 활성화 후 모니터링

```promql
# TSDB 쿼리 비율
sum(rate(loki_index_request_duration_seconds_count{store="tsdb"}[5m]))
/
sum(rate(loki_index_request_duration_seconds_count[5m]))
```

#### BoltDB 데이터 정리

보존 기간이 지나면 BoltDB 데이터가 자동으로 만료됨 → 모든 BoltDB 데이터가 만료된 후 schema_config에서 해당 항목 제거 가능.

---

### 오브젝트 스토리지 변경

#### 같은 백엔드 내 버킷 변경

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_new_   # 새 prefix
        period: 24h

storage_config:
  aws:
    s3: s3://access:secret@region/new-bucket   # 새 버킷
```

#### 다른 백엔드로 (예: S3 → GCS)

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: gcs           # GCS로
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h

storage_config:
  aws:
    s3: s3://access:secret@region/old-bucket   # 기존 (읽기용)
  gcs:
    bucket_name: new-loki-bucket               # 새 (쓰기/읽기)
```

#### 데이터 복사 (선택)

기존 데이터를 새 백엔드로 복사하는 방법.

```bash
# rclone 사용 예시
rclone sync s3:old-bucket gcs:new-bucket \
  --transfers 16 \
  --checkers 32
```

복사 후 schema_config의 `from` 날짜를 과거로 변경 가능 → 단, 기존 prefix와 충돌하지 않도록 주의 필요.

---

### 스키마 버전 마이그레이션

#### 버전 변천

- v9: 기본 스키마
- v10: 인덱스 효율 개선
- v11: 더 나은 청크 매핑
- v12: BoltDB Shipper 표준
- v13: TSDB 표준, 현재 권장

#### 적용 방법

동일한 store 내에서 버전을 변경할 때도 새 항목으로 추가.

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13          # ✅ 새 버전
      index:
        prefix: index_v13_   # 새 prefix 권장
        period: 24h
```

---

### Single Store에서 분리 모드로

Loki 2.0 이전에는 인덱스와 청크를 별도의 스토어에 분리하여 저장 → 현재는 인덱스와 청크 모두 오브젝트 스토리지를 사용하는 Single Store 방식이 표준.

#### 마이그레이션 (드물게 필요)

```yaml
schema_config:
  configs:
    # 구식 분리 모드
    - from: 2020-01-01
      store: cassandra
      object_store: s3
      schema: v11
      index:
        prefix: index_
        period: 24h
    
    # 새 Single Store
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h

storage_config:
  cassandra:
    addresses: cassandra1,cassandra2  # 읽기용
    keyspace: loki
  
  aws:
    s3: s3://...
```

기존 Cassandra 데이터는 보존 기간이 만료될 때까지 읽기 전용으로 유지됨.

---

### Cortex/Loki 1.x → 2.x → 3.x

#### Loki 1.x → 2.x

주요 변경:
- BoltDB Shipper 도입
- Single Store 표준화

마이그레이션:

```yaml
# 기존 1.x (Cassandra/DynamoDB 등)
- from: 2020-01-01
  store: cassandra
  ...

# 2.x로 (BoltDB Shipper)
- from: 2024-06-15
  store: boltdb-shipper
  object_store: s3
  schema: v11
  ...
```

#### Loki 2.x → 3.x

주요 변경:
- BoltDB Shipper Deprecated
- TSDB 권장
- Pattern Ingester 추가
- Bloom 필터 (실험적)

마이그레이션:

```yaml
# 2.x BoltDB Shipper
- from: 2023-01-01
  store: boltdb-shipper
  object_store: s3
  schema: v12
  ...

# 3.x TSDB
- from: 2024-06-15
  store: tsdb
  object_store: s3
  schema: v13
  ...
```

#### Breaking Changes 체크리스트

3.0:
- 일부 메트릭 이름 변경 (예: `cortex_*` → `loki_*`)
- 일부 구성 옵션 제거
- ingester WAL 형식 호환성 (재시작 시 비워질 수 있음)

각 버전별 [업그레이드 가이드](https://grafana.com/docs/loki/latest/setup/upgrade/) 반드시 참고 필요.

---

### 클러스터 간 데이터 이동

#### 시나리오 1: 클러스터 분할

하나의 클러스터를 두 클러스터로 분할(예: 테넌트별 분리).

##### 단계

1. 새 클러스터에서 일부 테넌트를 신규 시작(실시간 데이터만)
2. 과거 데이터는 기존 클러스터에서 계속 조회
3. 특정 시점 이후 데이터는 새 클러스터에서 조회
4. 보존 기간 경과 후 기존 클러스터 정리

#### 시나리오 2: 데이터 이전

##### Push API 재전송

```python
# 기존 Loki에서 쿼리
import requests

response = requests.get(
    "http://old-loki:3100/loki/api/v1/query_range",
    headers={"X-Scope-OrgID": "tenant-1"},
    params={
        "query": '{namespace="prod"}',
        "start": "1700000000",
        "end": "1700003600",
        "limit": "5000",
    },
)

# 새 Loki로 푸시
data = response.json()
for stream in data["data"]["result"]:
    push_payload = {
        "streams": [{
            "stream": stream["stream"],
            "values": stream["values"],
        }]
    }
    requests.post(
        "http://new-loki:3100/loki/api/v1/push",
        headers={
            "X-Scope-OrgID": "tenant-1",
            "Content-Type": "application/json",
        },
        json=push_payload,
    )
```

> 주의: 오래된 샘플 거부 제한(`reject_old_samples_max_age`)을 임시로 늘려야 함.
> ```yaml
> limits_config:
>   reject_old_samples: false
> ```

##### Object Storage 직접 복사

동일한 스키마를 사용하는 경우 가장 빠른 방법.

```bash
# AWS CLI
aws s3 sync s3://old-loki-bucket s3://new-loki-bucket \
  --acl bucket-owner-full-control

# rclone (다양한 백엔드)
rclone sync old:loki-bucket new:loki-bucket \
  --transfers 32 \
  --checkers 64
```

복사 후 새 클러스터의 `storage_config`에서 새 버킷 지정 필요.

---

### 백업과 복구

#### 백업 전략

##### 오브젝트 스토리지 백업

가장 중요한 부분 → 다음 옵션 활용 가능.

S3 Versioning + Cross-Region Replication

```json
{
  "Rules": [
    {
      "Status": "Enabled",
      "Priority": 1,
      "Filter": {},
      "Destination": {
        "Bucket": "arn:aws:s3:::loki-backup-us-west-2",
        "ReplicationTime": {
          "Status": "Enabled",
          "Time": { "Minutes": 15 }
        }
      }
    }
  ]
}
```

GCS Multi-Regional Buckets

기본적으로 여러 리전에 자동 복제됨.

Azure GRS

Geo-Redundant Storage로 자동 복제.

##### 구성 백업

- `loki.yaml`
- `runtime-config.yaml`
- 룰 파일들
- Helm values

모든 구성을 Git으로 관리하는 GitOps 방식 권장.

#### 복구 절차

##### 부분 복구

특정 청크가 손상된 경우:
1. S3 Versioning에서 이전 버전 복구
2. 손상된 시간대의 데이터만 영향받음

##### 전체 복구

전체 버킷 손실 시:
1. 백업 버킷에서 새 버킷으로 복사
2. Loki 구성에서 새 버킷 지정
3. 재시작

##### Disaster Recovery

DR 사이트 운영:
- 활성 클러스터: 메인 리전
- 대기 클러스터: 백업 리전 (실시간 데이터 받지 않음)
- 장애 발생 시: 대기 클러스터에 백업 버킷 마운트, DNS 변경

#### WAL 데이터

WAL은 임시 데이터 → 별도 백업 불필요. Ingester 재시작 시 메모리 데이터 복구 용도.

WAL 디스크 손상 시:
- 최근 1~2시간 데이터 손실 가능
- 복제 계수가 3 이상이면 다른 Ingester에서 복구 가능

---

## Loki 시각화 (Grafana 통합)

> 원본 참고: https://grafana.com/docs/loki/latest/visualize/grafana/

---

### 목차

1. [개요](#개요)
2. [Loki 데이터소스 추가](#loki-데이터소스-추가)
3. [Explore 사용법](#explore-사용법)
4. [대시보드 생성](#대시보드-생성)
5. [패널 종류별 LogQL 활용](#패널-종류별-logql-활용)
6. [데이터소스 간 연결](#데이터소스-간-연결)
7. [Annotation](#annotation)
8. [Grafana Alerting 연동](#grafana-alerting-연동)
9. [공식 대시보드](#공식-대시보드)

---

### 개요

Grafana는 Loki의 기본 시각화 도구 → 다음을 지원.

- Explore: 임시 쿼리 및 탐색
- Dashboards: 패널과 변수로 구성된 대시보드
- Alerting: LogQL 기반 알림
- Drilldown: 메트릭 → 트레이스 → 로그 연결

---

### Loki 데이터소스 추가

#### UI에서 추가

1. Grafana → **Connections** → **Data sources** → **Add new data source**
2. **Loki** 선택
3. URL 입력: `http://loki:3100`
4. 인증 설정 (필요시)
5. **Save & test**

#### Provisioning (자동 설정)

```yaml
# /etc/grafana/provisioning/datasources/loki.yaml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      maxLines: 1000
      timeout: 60
      derivedFields:
        - name: TraceID
          matcherRegex: 'trace_id=(\w+)'
          url: '$${__value.raw}'
          datasourceUid: tempo
    secureJsonData:
      httpHeaderValue1: 'tenant-1'
    httpHeaderName1: 'X-Scope-OrgID'
    isDefault: false
    editable: true
    version: 1
```

#### 멀티 테넌시 헤더

```yaml
jsonData:
  httpHeaderName1: X-Scope-OrgID
secureJsonData:
  httpHeaderValue1: tenant-1
```

#### 인증

##### Basic Auth

```yaml
basicAuth: true
basicAuthUser: user
secureJsonData:
  basicAuthPassword: pass
```

##### TLS

```yaml
jsonData:
  tlsAuth: true
  tlsAuthWithCACert: true
secureJsonData:
  tlsCACert: |
    -----BEGIN CERTIFICATE-----
    ...
  tlsClientCert: |
    -----BEGIN CERTIFICATE-----
    ...
  tlsClientKey: |
    -----BEGIN PRIVATE KEY-----
    ...
```

---

### Explore 사용법

#### 기본 사용

1. 좌측 사이드바 → **Explore**
2. 데이터소스: **Loki** 선택
3. 쿼리 입력 또는 빌더 사용
4. **Run query**

#### 쿼리 빌더

UI에서 라벨·필터·파서·연산자를 클릭하여 쿼리 작성 가능 → LogQL을 몰라도 사용 가능.

#### Logs 패널 기능

- Live tailing: 실시간 로그 스트리밍(WebSocket)
- Wrapping: 긴 라인 줄바꿈
- Time order: 오름차순/내림차순
- Show context: 특정 라인 주변 로그
- Pretty Print JSON: JSON 포맷팅
- Histogram: 시간별 분포

#### Logs to Metrics 변환

```logql
# 로그 → 메트릭
sum(rate({app="api"} [5m]))

# 결과를 그래프로 자동 시각화
```

#### Split View

화면을 분할하여 두 데이터소스를 동시에 비교 가능(예: 메트릭 + 로그).

---

### 대시보드 생성

#### 패널 추가

1. 대시보드 → **Add visualization**
2. 데이터소스 선택
3. LogQL 쿼리 입력
4. 시각화 타입 선택

#### 변수 (Variables)

대시보드 변수로 동적 필터링:

```
변수 이름: namespace
타입: Query
데이터소스: Loki
쿼리: label_values(namespace)
```

쿼리에서 사용:
```logql
{namespace="$namespace", app=~".+"}
```

#### 시간 범위 변수

`$__from`, `$__to`, `$__interval`, `$__range` 등 자동 변수 활용.

---

### 패널 종류별 LogQL 활용

#### Logs 패널

원본 로그 표시:

```logql
{namespace="$namespace"} |= "$search"
```

#### Time Series

메트릭 형식 결과:

```logql
sum by (level) (rate({namespace="$namespace"}[5m]))
```

#### Stat / Single Stat

단일 값:

```logql
sum(rate({namespace="$namespace"} |= "error" [5m]))
```

#### Bar Chart / Pie Chart

```logql
sum by (level) (count_over_time({namespace="$namespace"}[$__range]))
```

#### Table

원본 로그 테이블:

```logql
{namespace="$namespace"} | json | line_format "{{.method}} {{.path}}"
```

#### Heatmap

응답 시간 분포:

```logql
sum by (le) (
  rate(
    {app="api"}
    | json
    | unwrap response_time_ms
    | __error__=""
    [5m]
  )
)
```

#### Logs Volume

Explore 상단에 자동으로 표시되는 로그 양 차트 → Volume API 사용.

---

### 데이터소스 간 연결

#### Logs ↔ Traces (Loki ↔ Tempo)

##### Loki → Tempo (Derived Fields)

```yaml
# Loki 데이터소스 설정
jsonData:
  derivedFields:
    - name: TraceID
      matcherRegex: '"trace_id":"(\w+)"'
      url: '$${__value.raw}'
      datasourceUid: tempo-uid
    
    - name: TraceID
      matcherRegex: 'trace_id=(\w+)'
      url: '$${__value.raw}'
      datasourceUid: tempo-uid
```

로그에서 `trace_id` 클릭 → Tempo에서 해당 트레이스 자동 조회됨.

##### Tempo → Loki (Trace to Logs)

```yaml
# Tempo 데이터소스 설정
jsonData:
  tracesToLogsV2:
    datasourceUid: loki-uid
    spanStartTimeShift: '-1m'
    spanEndTimeShift: '1m'
    tags:
      - key: 'service.name'
        value: 'service'
    customQuery: true
    query: '{$${__tags}} |~ "$${__span.traceId}"'
```

#### Logs ↔ Metrics (Loki ↔ Mimir/Prometheus)

##### Logs to Metrics

LogQL 메트릭 쿼리로 직접 변환:

```logql
sum(rate({app="api"} |= "error" [5m]))
```

##### Metrics to Logs (Exemplars)

Prometheus 메트릭에 trace ID exemplar가 있는 경우 → 메트릭 → 트레이스 → 관련 로그로 연결됨.

---

### Annotation

대시보드에 이벤트 표시.

#### 설정

대시보드 설정 → **Annotations** → **Add annotation query**

데이터소스: Loki

쿼리:
```logql
{app="deploy"} |= "deployment completed"
```

이름 템플릿:
```
{{labels.app}} 배포 완료
```

이렇게 설정 → 모든 패널에 배포 시점이 세로선으로 표시됨.

#### 활용 예

- 배포 이벤트
- 인시던트 시작/종료
- 점검 시간

---

### Grafana Alerting 연동

#### 새로운 Unified Alerting (Grafana 8.0+)

LogQL로 알림 정의 가능.

##### 알림 룰 작성

1. **Alerting** → **Alert rules** → **New rule**
2. 데이터소스: Loki 선택
3. 쿼리:
```logql
sum(rate({namespace="prod"} |= "error" [5m]))
```
4. 조건:
```
WHEN last() OF query(A, 5m, now) IS ABOVE 10
```
5. 평가 주기, for, 라벨, 주석 설정
6. Contact Point 연결 (Slack, Email 등)

##### Loki Ruler vs Grafana Alerting

- 평가 위치: Loki Ruler는 Loki 자체 · Grafana Alerting은 Grafana 외부
- 룰 관리: Loki Ruler는 YAML 파일/API · Grafana Alerting은 UI/Provisioning
- 백엔드 통합: Loki Ruler는 Alertmanager · Grafana Alerting은 Grafana Contact Points
- Recording: Loki Ruler는 Mimir/Prometheus로 전송 · Grafana Alerting은 옵션
- 권장 용도: Loki Ruler는 대규모/표준 환경 · Grafana Alerting은 소규모/시각적 관리 환경

---

### 공식 대시보드

#### Loki Mixin

[Loki Mixin](https://github.com/grafana/loki/tree/main/production/loki-mixin) 에서 제공:

```bash
jb install github.com/grafana/loki/production/loki-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

포함:
- Loki / Operational: 컴포넌트별 운영 메트릭
- Loki / Reads: 쿼리 경로
- Loki / Writes: 수집 경로
- Loki / Resources: CPU/메모리/네트워크
- Loki / Chunks: 청크 관련
- Loki / Object Store: 스토리지 사용량
- Loki / Logs: Loki 자체 로그

#### Grafana.com 대시보드

[Grafana Dashboards](https://grafana.com/grafana/dashboards/?search=loki) 에서 검색.

추천:
- ID 13186: Loki Logs/Metrics Dashboard
- ID 12611: Loki Stack Monitoring (Promtail, Loki, Cortex/Mimir)
- ID 14055: Loki Operational

#### Import

```
Grafana → Dashboards → Import → Import via grafana.com → ID 입력
```

#### 커스텀 대시보드 예시 패널

##### 1. 로그 수집 속도

```logql
sum by (job) (rate({}[5m]))
```

##### 2. 에러 발생 시각화

```logql
sum by (namespace) (
  rate({namespace=~".+"} |~ "(?i)error" [5m])
)
```

##### 3. 응답 시간 P95

```logql
quantile_over_time(0.95,
  {app="api"}
  | json
  | unwrap duration_ms
  [5m]
)
```

##### 4. 상위 N개 로그 양

```logql
topk(10,
  sum by (app) (
    sum_over_time(({namespace=~".+"} | label_format size=`{{.size}}`)[5m])
  )
)
```

##### 5. 로그 레벨 분포

```logql
sum by (level) (
  count_over_time({namespace="$namespace"} | json [$__range])
)
```
