# Mimir Tools와 시각화

## Mimir Tools (mimirtool, 기타 도구)

> 원본: https://grafana.com/docs/mimir/latest/manage/tools/

<a id="도구-개요"></a>
### 도구 개요

- `mimirtool`: 룰과 Alertmanager 설정을 관리하고 데이터를 분석하거나 변환하는 CLI
- `Mimir Continuous Test`: 운영 중 데이터 무결성을 지속적으로 검증하는 도구
- `query-tee`: 두 백엔드에 같은 쿼리를 보내 응답을 비교하는 도구
- `trafficdump`: 트래픽을 캡처하고 분석하는 도구
- `bucket-tool`: 오브젝트 스토리지의 Mimir 데이터를 직접 조작하는 도구

<a id="mimirtool"></a>

### mimirtool

- Mimir 운영에 필요한 거의 모든 작업을 수행하는 종합 CLI 도구

#### 설치

```bash
# Homebrew
brew install mimirtool

# 바이너리 다운로드
curl -fLo mimirtool https://github.com/grafana/mimir/releases/latest/download/mimirtool-linux-amd64
chmod +x mimirtool
sudo mv mimirtool /usr/local/bin/

# Docker
docker run grafana/mimirtool --help
```

#### 공통 옵션

```bash
--address=http://mimir:9009    # Mimir 주소
--id=tenant-1                   # 테넌트 ID (X-Scope-OrgID)
--user=user                     # Basic Auth
--key=password                  # Basic Auth
--auth-token=<bearer>           # Bearer Token
```

- 또는 환경 변수:

```bash
export MIMIR_ADDRESS=http://mimir:9009
export MIMIR_TENANT_ID=tenant-1
```

#### 명령 카테고리

```
mimirtool
├── rules           # Recording/Alerting Rules
├── alertmanager    # Alertmanager 설정
├── analyze         # 사용 분석
├── backfill        # 블록 백필
├── bucket-validation  # 버킷 검증
├── config          # 구성 변환
├── remote-read     # Remote Read
├── version
└── help
```

#### `mimirtool rules`

- 룰 목록

  ```bash
  mimirtool rules list \
    --address=http://mimir:9009 \
    --id=tenant-1
  ```

- 룰 출력 (인쇄)

  ```bash
  mimirtool rules print \
    --address=http://mimir:9009 \
    --id=tenant-1
  ```

- 룰 업로드

  ```bash
  # 단일 파일
  mimirtool rules load \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    rules.yaml

  # 여러 파일
  mimirtool rules load \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    alerts.yaml recording.yaml

  # 동기화 (현재 로컬 파일이 truth)
  mimirtool rules sync \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    rules/*.yaml
  ```

- 룰 비교 (diff)

  ```bash
  mimirtool rules diff \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    rules/*.yaml
  ```

- 룰 삭제

  ```bash
  mimirtool rules delete \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    --namespace=infra \
    --rule-group=node_alerts

  # 네임스페이스 전체
  mimirtool rules delete-namespace \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    --namespace=infra
  ```

- 룰 검증 (lint)

  ```bash
  # lint: YAML 및 PromQL 포맷 자동 수정 (in-place)
  mimirtool rules lint rules.yaml

  # 드라이런 (수정 없이 확인만)
  mimirtool rules lint -n rules.yaml

  # 모범 사례 기준 검사
  mimirtool rules check rules.yaml
  ```

- Prometheus → Mimir 변환

  ```bash
  mimirtool rules prepare \
    --in-source-tenants=true \
    rules.yaml
  ```

#### `mimirtool alertmanager`

- 구성 업로드

  ```bash
  mimirtool alertmanager load \
    --address=http://mimir:9009 \
    --id=tenant-1 \
    alertmanager.yaml \
    template1.tmpl template2.tmpl
  ```

- 구성 조회

  ```bash
  mimirtool alertmanager get \
    --address=http://mimir:9009 \
    --id=tenant-1
  ```

- 구성 삭제

  ```bash
  mimirtool alertmanager delete \
    --address=http://mimir:9009 \
    --id=tenant-1
  ```

- 구성 검증

  ```bash
  mimirtool alertmanager verify \
    alertmanager.yaml \
    template1.tmpl
  ```

- 구성 마이그레이션

  ```bash
  # Cortex Alertmanager → Mimir
  mimirtool alertmanager migrate-utf8 \
    --in-place \
    alertmanager.yaml
  ```

#### `mimirtool analyze`

- Grafana 대시보드 분석

  ```bash
  mimirtool analyze grafana \
    --address=http://grafana:3000 \
    --key=$GRAFANA_API_KEY
  ```

  - 대시보드에서 사용된 메트릭을 추출

- Prometheus → Mimir 분석

  ```bash
  mimirtool analyze prometheus \
    --address=http://prometheus:9090 \
    --grafana-metrics-file=metrics-in-grafana.json \
    --ruler-metrics-file=metrics-in-ruler.json
  ```

  실제로 사용하는 메트릭과 수집 중인 메트릭을 비교해 불필요하게 수집하는 메트릭을 찾는다.

- 룰 파일 분석

  ```bash
  mimirtool analyze rule-file rules.yaml
  ```

- 대시보드 JSON 분석

  ```bash
  mimirtool analyze dashboard dashboard.json
  ```

#### `mimirtool backfill`

TSDB 블록을 Mimir에 직접 업로드한다. Prometheus나 다른 Mimir의 데이터를 옮기거나 백업을 복원할 때 사용한다.

```bash
mimirtool backfill \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  /path/to/tsdb-blocks/
```

#### `mimirtool bucket-validation`

- 오브젝트 스토리지 동작을 검증

```bash
mimirtool bucket-validation \
  --backend=s3 \
  --s3.bucket-name=my-mimir \
  --s3.endpoint=s3.amazonaws.com \
  --s3.region=us-east-1 \
  --s3.access-key=$AWS_ACCESS_KEY_ID \
  --s3.secret-key=$AWS_SECRET_ACCESS_KEY
```

- 검증 항목:
- 쓰기 권한
- 읽기 권한
- 삭제 권한
- 목록 권한
- 일관성

#### `mimirtool config`

- 설정 파일을 변환(Cortex → Mimir, 구버전 → 신버전).

```bash
# Cortex → Mimir
mimirtool config convert \
  --yaml-file=cortex-config.yaml \
  --output=mimir-config.yaml

# 구버전 Mimir → 신버전
mimirtool config convert \
  --yaml-file=old-mimir.yaml \
  --output=new-mimir.yaml \
  --target-version=2.10
```

#### `mimirtool remote-read`

- Remote Read 엔드포인트를 직접 호출

```bash
mimirtool remote-read dump \
  --address=http://mimir:9009/prometheus/api/v1/read \
  --id=tenant-1 \
  --from=2024-01-01T00:00:00Z \
  --to=2024-01-02T00:00:00Z \
  --selector='{job="prometheus"}'
```

<a id="mimir-continuous-test"></a>

### Mimir Continuous Test

<a id="개요"></a>

#### 개요

Mimir Continuous Test는 정해진 메트릭을 주기적으로 보낸 뒤 다시 조회해 값, 라벨, 시간이 일치하는지 확인한다. 이 과정을 반복해 클러스터가 데이터를 정상적으로 저장하고 조회하는지 검증한다.

#### 동작

```
[Continuous Test]
    |
    +--> 1. 알려진 메트릭을 주기적으로 푸시
    |
    +--> 2. 일정 시간 후 같은 메트릭을 쿼리
    |
    +--> 3. 응답 검증 (값, 라벨, 시간 등)
    |
    +--> 4. 결과를 메트릭으로 노출
```

#### 실행

```bash
docker run -d \
  --name=mimir-continuous-test \
  grafana/mimir-continuous-test:latest \
  -tests.write-endpoint=http://mimir:9009 \
  -tests.read-endpoint=http://mimir:9009 \
  -tests.tenant-id=test \
  -tests.write-read-series-test.enabled=true \
  -tests.write-read-series-test.num-series=100 \
  -tests.run-interval=30s
```

#### 메트릭

```
mimir_continuous_test_writes_total
mimir_continuous_test_writes_failed_total
mimir_continuous_test_queries_total
mimir_continuous_test_queries_failed_total
mimir_continuous_test_query_result_checks_total
mimir_continuous_test_query_result_checks_failed_total
```

#### 알림

```yaml
- alert: MimirContinuousTestFailing
  expr: |
    sum(rate(mimir_continuous_test_query_result_checks_failed_total[5m])) > 0
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Mimir 지속적 테스트 실패: 데이터 무결성 문제"
```

<a id="query-tee"></a>

### Query-tee

#### 개요

Query-tee는 두 Mimir/Cortex/Prometheus 백엔드에 같은 쿼리를 보내 응답을 비교한다. 한쪽 응답을 사용자에게 반환하면서 다른 쪽의 결과와 비교하므로, 새 버전이나 마이그레이션 대상의 동작을 확인할 때 사용할 수 있다.

#### 활용

- 새 버전 검증 (회귀 확인)
- Mimir vs Cortex 마이그레이션 검증
- A/B 테스트

#### 실행

```bash
docker run -d \
  --name=query-tee \
  -p 8080:8080 \
  grafana/query-tee:latest \
  -backend.endpoints=http://mimir-old:9009/prometheus,http://mimir-new:9009/prometheus \
  -backend.preferred=mimir-old \
  -log.level=info
```

#### 작동

```
[Grafana / 사용자]
       |
       v
   [query-tee]
       |
       +-> [Mimir Old]   (응답을 사용자에게 반환)
       |
       +-> [Mimir New]   (병렬 호출, 응답 비교만)
            |
            v
        [diff metrics]
```

#### 메트릭

```
cortex_querytee_backend_request_duration_seconds
cortex_querytee_responses_total
cortex_querytee_responses_compared_total
```

### mimir-query-tee

`mimir-query-tee`는 Mimir에 맞춘 `query-tee` 버전으로, 동작은 거의 같다.

<a id="trafficdump"></a>

### trafficdump

#### 개요

`trafficdump`는 Mimir 트래픽을 캡처해 분석하거나 다시 재생하는 도구다. 캡처한 트래픽으로 새 환경에 같은 부하를 주거나 회귀 테스트를 수행할 수 있다.

#### 캡처

```bash
trafficdump capture \
  --listen-address=:8080 \
  --target-address=http://mimir:9009 \
  --output=traffic.dump
```

#### 분석

```bash
trafficdump analyse traffic.dump
```

분석 결과에서는 요청 수와 함께 엔드포인트별, 테넌트별, 응답 코드별 분포를 확인할 수 있다.

#### 리플레이

```bash
trafficdump replay \
  --target-address=http://test-mimir:9009 \
  --input=traffic.dump
```

- 활용:
- 새 환경에 같은 부하 테스트
- 회귀 테스트

<a id="bucket-tool"></a>

### bucket-tool

#### 개요

- 오브젝트 스토리지의 Mimir 데이터를 직접 조작

#### 명령

```bash
# 블록 목록
bucket-tool blocks list \
  --backend=s3 \
  --bucket=my-mimir

# 블록 메타데이터
bucket-tool blocks metadata \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1 \
  --block=01HZX...

# 블록 삭제 (위험)
bucket-tool blocks delete \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1 \
  --block=01HZX...

# 테넌트 목록
bucket-tool tenants list \
  --backend=s3 \
  --bucket=my-mimir

# 일관성 확인
bucket-tool blocks check \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1
```

#### 활용

- 손상된 블록 식별/삭제
- 디스크 사용량 분석
- 복구 작업

<a id="grafana-mimir-architecture-diagrams"></a>

### grafana-mimir-architecture-diagrams

#### 개요

- 현재 Mimir 클러스터 구성을 다이어그램으로 시각화

#### 사용

```bash
docker run -v $(pwd)/diagrams:/output \
  grafana/mimir-architecture-diagrams:latest \
  --config=/etc/mimir.yaml \
  --output-format=svg \
  --output-dir=/output
```

- 생성되는 다이어그램:
- 컴포넌트 토폴로지
- 데이터 흐름
- 스토리지 구조
- 의존성

### 운영 시나리오

#### 1. 매일 자동 검증

```bash
#!/bin/bash
# daily-validation.sh

# Mimir 자체 헬스
curl -f http://mimir:9009/ready || exit 1

# 버킷 무결성
mimirtool bucket-validation --backend=s3 --s3.bucket-name=my-mimir

# Continuous Test 메트릭 확인
FAILED=$(curl -s http://mimir-continuous-test:9090/metrics | \
  grep mimir_continuous_test_query_result_checks_failed_total | \
  awk '{print $2}')

if [ "$FAILED" != "0" ]; then
  echo "ALERT: Continuous test failures: $FAILED"
fi
```

#### 2. 룰 GitOps

```bash
# CI/CD 파이프라인
mimirtool rules lint rules/*.yaml || exit 1

mimirtool rules diff \
  --address=$MIMIR_URL \
  --id=$TENANT_ID \
  rules/*.yaml

# Diff 검토 후 적용
mimirtool rules sync \
  --address=$MIMIR_URL \
  --id=$TENANT_ID \
  rules/*.yaml
```

#### 3. 사용 안 되는 메트릭 정리

```bash
# 1. Grafana 대시보드 분석
mimirtool analyze grafana \
  --address=http://grafana:3000 \
  --key=$GRAFANA_API_KEY \
  --output=grafana-metrics.json

# 2. Prometheus 분석
mimirtool analyze prometheus \
  --address=http://prometheus:9090 \
  --grafana-metrics-file=grafana-metrics.json \
  --ruler-metrics-file=ruler-metrics.json \
  --output=prometheus-metrics.json

# 3. 결과 검토 후 write_relabel_configs로 불필요한 메트릭 드롭
```

## Mimir 시각화 (Grafana 통합)

> 원본: https://grafana.com/docs/mimir/latest/visualize/

### 개요

Mimir는 Prometheus HTTP API와 호환되므로 Grafana에 Prometheus 데이터소스로 등록할 수 있다. 연결한 뒤에는 메트릭 조회와 함께 다음 기능을 활용할 수 있다.

- Native Histograms 시각화
- Exemplars로 트레이스 연결
- Cardinality 분석 UI
- 멀티 테넌트 헤더 지원

<a id="mimir-데이터소스-추가"></a>

### Mimir 데이터소스 추가

#### UI에서

- 1\. Connections → Data sources → Add new
- 2\. Prometheus 선택 (Mimir는 Prometheus 호환)
- 3\. URL: `http://mimir:9009/prometheus`
- 4\. Custom HTTP Headers: `X-Scope-OrgID: tenant-1`
- 5\. Save & test

#### Provisioning

```yaml
# /etc/grafana/provisioning/datasources/mimir.yaml
apiVersion: 1

datasources:
  - name: Mimir
    type: prometheus
    access: proxy
    url: http://mimir:9009/prometheus
    uid: mimir
    isDefault: true
    
    jsonData:
      # 멀티 테넌시
      httpHeaderName1: X-Scope-OrgID
      
      # 쿼리 옵션
      httpMethod: POST
      manageAlerts: false           # Mimir Ruler 사용
      prometheusType: Mimir
      prometheusVersion: 2.50.0
      
      # 시간 범위
      timeInterval: 15s
      queryTimeout: 60s
      
      # Exemplars
      exemplarTraceIdDestinations:
        - name: trace_id
          datasourceUid: tempo
          urlDisplayLabel: 'View Trace'
      
      # Mimir 추가 기능
      cacheLevel: High
      incrementalQuerying: true
      incrementalQueryOverlapWindow: 10m
      
      # Cardinality
      disableRecordingRules: false
    
    secureJsonData:
      httpHeaderValue1: tenant-1
      basicAuthPassword: ${MIMIR_PASSWORD}
```

#### Grafana Cloud의 경우

```yaml
url: https://prometheus-blocks-prod-us-central1.grafana.net/api/prom
basicAuth: true
basicAuthUser: '12345'
secureJsonData:
  basicAuthPassword: ${GRAFANA_CLOUD_API_KEY}
```

<a id="explore-사용"></a>

### Explore 사용

#### PromQL 입력

- 1\. Explore → 데이터소스 Mimir
- 2\. PromQL 입력:

```promql
rate(http_requests_total[5m])
```

#### Builder 모드

Builder 모드에서는 메트릭, 라벨, 연산을 화면에서 선택해 쿼리를 만든다. PromQL 문법을 익히지 않은 사용자도 이 방식으로 쿼리를 작성할 수 있다.

#### Code 모드

Code 모드에서는 PromQL을 직접 작성하며, 자동완성을 사용할 수 있다.

#### 결과 보기

- Graph: 시계열 그래프
- Table: 테이블 형식
- Time series: 패널
- Heatmap: Native Histogram에 적합

#### Range vs Instant

- Range: 시간 범위 (시계열)
- Instant: 단일 시점 값

<a id="native-histograms-시각화"></a>

### Native Histograms 시각화

#### Heatmap 패널

- 가장 적합한 시각화:

- 1\. Add visualization → Heatmap
- 2\. 데이터소스: Mimir
- 3\. 쿼리:

```promql
sum by (le) (
  rate(http_request_duration_seconds_bucket[5m])
)
```
- 4\. 옵션:
  - Calculate from data: Y-Buckets
  - Cell type: Heatmap

#### Native Histogram 자동 감지

- Native Histogram인 경우 Grafana가 자동으로 적절한 시각화를 제공

```promql
# Native Histogram
my_native_histogram

# Bucket 분포 시각화
histogram_quantile(0.95, sum by (le) (rate(my_native_histogram[5m])))
histogram_quantile(0.99, sum by (le) (rate(my_native_histogram[5m])))
```

#### 분위수 시각화

```promql
# 동시에 여러 분위수
histogram_quantile(0.50, sum by (le) (rate(my_metric[5m])))
histogram_quantile(0.95, sum by (le) (rate(my_metric[5m])))
histogram_quantile(0.99, sum by (le) (rate(my_metric[5m])))
```

<a id="exemplars-트레이스-연결"></a>

### Exemplars (트레이스 연결)

#### Exemplars 활성화

- Mimir 측

  ```yaml
  limits:
    max_global_exemplars_per_user: 100000

  ingester:
    max_exemplars: 100000
  ```

- Prometheus 메트릭 코드

  ```go
  // Go 예시
  histogram.(prometheus.ExemplarObserver).ObserveWithExemplar(
      duration.Seconds(),
      prometheus.Labels{"trace_id": traceID},
  )
  ```

#### Grafana 데이터소스 설정

```yaml
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
    urlDisplayLabel: 'View Trace'
```

#### 시각화

- 차트에서 점으로 Exemplar가 표시됨:

```
시계열 그래프
   ●     ●  ←  Exemplar (trace_id 포함)
  ╱│ ╲ ╱│
 ╱ │  ╳ │
   │ ╱╲ │
```

- 점을 클릭하면 Tempo에서 해당 트레이스 조회 가능

#### PromQL Range Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

- 응답에 `exemplars` 필드 포함

<a id="대시보드-구성"></a>

### 대시보드 구성

#### 변수 (Variables)

클러스터를 고르면 그 안의 namespace, 서비스, 인스턴스로 조회 범위를 좁힐 수 있도록 변수를 연결한다. 아래 설정은 앞에서 선택한 값을 다음 변수의 라벨 조건에 사용한다.

```
$cluster = label_values(up, cluster)
$namespace = label_values(up{cluster="$cluster"}, namespace)
$service = label_values(up{cluster="$cluster", namespace="$namespace"}, app)
$instance = label_values(up{cluster="$cluster", namespace="$namespace", app="$service"}, instance)
```

#### 자주 쓰는 패널

- 1. RED 메트릭 (Rate, Errors, Duration)

  - Rate

  ```promql
  sum by (service) (
    rate(http_requests_total{cluster="$cluster"}[5m])
  )
  ```

  - Errors

  ```promql
  sum by (service) (
    rate(http_requests_total{cluster="$cluster", status=~"5.."}[5m])
  )
  /
  sum by (service) (
    rate(http_requests_total{cluster="$cluster"}[5m])
  )
  ```

  - Duration P95

  ```promql
  histogram_quantile(0.95,
    sum by (service, le) (
      rate(http_request_duration_seconds_bucket{cluster="$cluster"}[5m])
    )
  )
  ```

- 2. USE 메트릭 (Utilization, Saturation, Errors)

  - CPU Utilization

  ```promql
  1 - avg by (instance) (
    rate(node_cpu_seconds_total{mode="idle"}[5m])
  )
  ```

  - Memory Utilization

  ```promql
  1 - (
    node_memory_MemAvailable_bytes
    /
    node_memory_MemTotal_bytes
  )
  ```

  - Disk Saturation

  ```promql
  node_disk_io_time_weighted_seconds_total
  ```

- 3. SLO 패널

  - SLO 준수율

  ```promql
  (
    sum(rate(http_requests_total{status!~"5.."}[30d]))
    /
    sum(rate(http_requests_total[30d]))
  )
  > 0.999
  ```

  - Error Budget 소진율

  ```promql
  1 - (
    (1 - 0.999)        # 허용 에러
    -
    (
      sum(increase(http_requests_total{status=~"5.."}[30d]))
      /
      sum(increase(http_requests_total[30d]))
    )
  ) / (1 - 0.999)
  ```

#### 패널 옵션

- Thresholds

  ```yaml
  thresholds:
    steps:
      - value: null
        color: green
      - value: 0.95
        color: yellow
      - value: 0.99
        color: red
  ```

- Color Scheme

  - Green-Yellow-Red: 좋음/주의/나쁨
  - By Value: 값에 따라 자동
  - Classic Palette: 시리즈별 다른 색

<a id="데이터소스-간-연결"></a>

### 데이터소스 간 연결

#### Metrics → Logs

- Exemplar 활용

  차트의 점을 클릭해 Tempo에서 트레이스를 조회하고, 같은 `trace_id`로 Loki의 관련 로그를 확인한다.

- 데이터 링크 (Data Links)

  ```yaml
  # 패널 설정
  links:
    - title: View Logs
      url: '/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{service=\"$service\"}"}]'
  ```

#### Metrics → Traces

```yaml
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
    urlDisplayLabel: 'View Trace'
  - name: traceID
    datasourceUid: tempo
```

#### 변수로 자동 연결

모든 패널과 링크에서 같은 `$service` 변수를 사용하면 선택한 서비스의 메트릭, 로그, 트레이스를 이어서 조회할 수 있다.

```
Metrics 패널: rate(http_requests_total{service="$service"}[5m])
Logs 링크: /explore?...{service="$service"}
Traces 링크: /explore?...resource.service.name="$service"
```

<a id="공식-대시보드"></a>

### 공식 대시보드

#### Mimir Mixin

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

- 포함:
- Mimir / Writes: 수집 경로
- Mimir / Reads: 쿼리 경로
- Mimir / Queries: 쿼리 분석
- Mimir / Object Store: 스토리지
- Mimir / Compactor: 압축
- Mimir / Ruler: 룰 평가
- Mimir / Alertmanager: 알림
- Mimir / Tenants: 테넌트별
- Mimir / Top Tenants: 상위 사용자
- Mimir / Slow Queries: 느린 쿼리
- Mimir / Resources: 리소스 사용
- Mimir / Networking: 네트워크
- Mimir / Overview: 종합

#### Grafana Cloud

- Grafana Cloud Mimir는 모든 Mixin 대시보드를 자동으로 사전 설치

#### Import

```
Dashboards → Import → grafana.com → ID 입력
```

- 추천 Mimir 대시보드 ID:
- 17775: Mimir / Writes
- 17776: Mimir / Reads
- 17779: Mimir / Object Store

<a id="recording-rules와-대시보드"></a>

### Recording Rules와 대시보드

#### 미리 계산된 메트릭

대시보드에서 자주 사용하는 무거운 쿼리는 Recording Rule로 미리 계산해 둔다. 대시보드는 저장된 결과를 조회하므로 로딩 시간을 줄일 수 있다.

```yaml
groups:
  - name: api_dashboard_recording
    interval: 30s
    rules:
      - record: cluster:api_request_rate:5m
        expr: |
          sum by (cluster, service) (
            rate(http_requests_total[5m])
          )
      
      - record: cluster:api_error_rate:5m
        expr: |
          sum by (cluster, service) (
            rate(http_requests_total{status=~"5.."}[5m])
          )
          /
          sum by (cluster, service) (
            rate(http_requests_total[5m])
          )
      
      - record: cluster:api_latency:p95:5m
        expr: |
          histogram_quantile(0.95,
            sum by (cluster, service, le) (
              rate(http_request_duration_seconds_bucket[5m])
            )
          )
```

#### 대시보드에서 사용

```promql
# 무거운 원본 쿼리 대신:
cluster:api_latency:p95:5m{cluster="$cluster"}
```

#### 명명 규칙

```
<aggregation_level>:<metric_name>:<duration>

예시:
- cluster:api_requests:5m
- namespace:cpu_usage:rate1m
- pod:memory_usage:max
```
