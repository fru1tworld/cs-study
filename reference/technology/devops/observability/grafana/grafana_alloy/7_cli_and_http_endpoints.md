# Alloy CLI 레퍼런스와 HTTP 엔드포인트

## Alloy CLI 레퍼런스

> 원본: https://grafana.com/docs/alloy/latest/reference/cli/

---

### 목차

1. [개요](#개요)
2. [`alloy run`](#alloy-run)
3. [`alloy validate`](#alloy-validate)
4. [`alloy fmt`](#alloy-fmt)
5. [`alloy convert`](#alloy-convert)
6. [`alloy tools`](#alloy-tools)
7. [공통 옵션](#공통-옵션)

---

### 개요

```
alloy [global flags] <command> [command flags] [args]
```

#### 명령 목록

- `run`: Alloy 실행
- `validate`: 구성 파일 검증
- `fmt`: 구성 포맷 정리
- `convert`: 다른 형식에서 변환
- `tools`: 보조 도구
- `--version`: 버전 확인
- `--help`: 도움말

---

### `alloy run`

가장 많이 사용하는 명령 → Alloy 실행.

```bash
alloy run [flags] <config-path>
```

#### 주요 플래그

##### Server

- `--server.http.listen-addr`
  - 기본값: `127.0.0.1:12345`
  - 설명: HTTP 서버
- `--server.http.ui-path-prefix`
  - 기본값: `/`
  - 설명: UI 경로
- `--server.http.memory-addr`
  - 기본값: `alloy.internal:12345`
  - 설명: 메모리 listener(테스트용)

##### Storage

- `--storage.path`
  - 기본값: `data-alloy/`
  - 설명: WAL, 중간 상태 저장

##### Config

- `--config.format`
  - 기본값: `alloy`
  - 설명: `alloy`, `prometheus`, `promtail`, `static`, `flow`, `otelcol`
- `--config.bypass-conversion-errors`
  - 기본값: false
  - 설명: 변환 오류 무시
- `--config.extra-args`
  - 기본값: ""
  - 설명: static 변환 시 추가 인자

##### Cluster

- `--cluster.enabled`
  - 기본값: false
  - 설명: 클러스터링 활성화
- `--cluster.node-name`
  - 기본값: (hostname)
  - 설명: 노드 이름
- `--cluster.advertise-address`
  - 기본값: ""
  - 설명: 광고할 주소
- `--cluster.advertise-interfaces`
  - 기본값: (auto)
  - 설명: 자동 광고할 NIC
- `--cluster.join-addresses`
  - 기본값: ""
  - 설명: 가입할 노드
- `--cluster.discover-peers`
  - 기본값: ""
  - 설명: 자동 피어 발견
- `--cluster.rejoin-interval`
  - 기본값: `60s`
  - 설명: 재가입 시도 주기
- `--cluster.max-join-peers`
  - 기본값: 5
  - 설명: 한 번에 가입할 피어 수
- `--cluster.name`
  - 기본값: ""
  - 설명: 클러스터 이름
- `--cluster.wait-for-size`
  - 기본값: 0
  - 설명: 시작 전 대기할 클러스터 크기
- `--cluster.wait-timeout`
  - 기본값: `0s`
  - 설명: 클러스터 대기 타임아웃

##### Cluster TLS (gossip)

- `--cluster.tls-ca-path`: CA 인증서
- `--cluster.tls-cert-path`: 클라이언트 인증서
- `--cluster.tls-key-path`: 클라이언트 키
- `--cluster.tls-server-name`: 서버 이름

##### Stability

- `--stability.level`
  - 기본값: `generally-available`
  - 설명: `experimental`, `public-preview`, `generally-available`

##### Reporting

- `--disable-reporting`
  - 기본값: false
  - 설명: Grafana로 사용 통계 보고 비활성화

##### Live Debugging

- `--feature.community-components.enabled`: 커뮤니티 컴포넌트 활성화

#### 사용 예시

##### 기본 실행

```bash
alloy run /etc/alloy/config.alloy
```

##### 클러스터 모드

```bash
alloy run \
  --server.http.listen-addr=0.0.0.0:12345 \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  /etc/alloy/config.alloy
```

##### Kubernetes 디스커버리

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345" \
  /etc/alloy/config.alloy
```

##### 실험적 컴포넌트 활성화

```bash
alloy run --stability.level=experimental /etc/alloy/config.alloy
```

##### 다른 형식 직접 실행

```bash
alloy run --config.format=prometheus /etc/prometheus/prometheus.yml
```

내부적으로 변환 후 실행 → 일회성 실행에 유용함.

---

### `alloy validate`

구성 파일 검증.

```bash
alloy validate [flags] <config-path>
```

#### 옵션

- `--config.format`: 구성 형식
- `--config.bypass-conversion-errors`: 변환 오류 무시
- `--feature.community-components.enabled`: 커뮤니티 컴포넌트
- `--stability.level`: 안정성 레벨

#### 사용

```bash
# 검증
alloy validate config.alloy

# 성공 시
config.alloy is valid

# 실패 시 (구체적 오류 표시)
config.alloy:5:3: argument "url" required but not provided
```

#### CI/CD 통합

```yaml
# .github/workflows/alloy.yml
- name: Validate Alloy config
  run: |
    docker run --rm -v $(pwd):/etc/alloy \
      grafana/alloy:latest \
      validate /etc/alloy/config.alloy
```

---

### `alloy fmt`

구성 파일 포맷 정리.

```bash
alloy fmt [flags] <config-path>
```

#### 옵션

- `-w, --write`: 파일을 직접 수정
- `-t, --test`: 변경 사항이 있으면 0이 아닌 종료 코드 반환(파일은 수정하지 않음)

#### 사용

```bash
# stdout에 포맷된 결과 출력
alloy fmt config.alloy

# 파일 수정
alloy fmt -w config.alloy

# 변경 사항이 있으면 비정상 종료 코드 반환 (CI 검증용)
alloy fmt -t config.alloy
```

#### Pre-commit hook

```bash
#!/bin/bash
# .git/hooks/pre-commit
for file in $(git diff --cached --name-only --diff-filter=ACM | grep '\.alloy$'); do
  alloy fmt -w "$file"
  git add "$file"
done
```

---

### `alloy convert`

다른 도구의 구성을 Alloy 구성으로 변환.

```bash
alloy convert [flags] <input-file>
```

#### 주요 플래그

- `--source-format=<format>`: 입력 형식(필수)
- `-o, --output=<file>`: 출력 파일
- `-r, --report=<file>`: 변환 보고서
- `-b, --bypass-errors`: 에러 무시
- `--extra-args=<args>`: 형식별 추가 인자

#### 지원 형식

- `prometheus`: Prometheus 구성(메트릭 스크래핑)
- `promtail`: Promtail 구성(Loki 로그)
- `static`: Grafana Agent Static
- `otelcol`: OpenTelemetry Collector

#### 사용 예시

##### Promtail → Alloy

```bash
alloy convert \
  --source-format=promtail \
  -o alloy-config.alloy \
  -r conversion-report.txt \
  promtail-config.yaml
```

##### Prometheus → Alloy

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  prometheus.yml
```

##### Static Agent → Alloy

```bash
alloy convert \
  --source-format=static \
  --extra-args="-enable-features=integrations-next" \
  -o alloy-config.alloy \
  agent-static.yaml
```

##### OTel Collector → Alloy

```bash
alloy convert \
  --source-format=otelcol \
  -o alloy-config.alloy \
  otel-collector.yaml
```

#### 변환 보고서

`--report` 옵션 사용 → 변환 중 발생한 경고와 오류를 파일에 기록.

```
=== Conversion Report ===

Warnings:
  - Line 23: scrape_config 'old_metrics' uses deprecated 'metric_relabel_configs'
  - Line 45: 'kubernetes_sd_configs' converted to 'discovery.kubernetes'

Errors:
  - Line 67: Custom scraper not supported, manual conversion needed

Output written to: alloy-config.alloy
```

---

### `alloy tools`

보조 도구 모음.

```bash
alloy tools <subcommand>
```

#### `alloy tools prometheus.remote_write`

`prometheus.remote_write` 컴포넌트 디버깅.

```bash
# WAL 정리 상태 확인
alloy tools prometheus.remote_write \
  sample-stats \
  --wal-directory=/var/lib/alloy/data/prometheus.remote_write.default

# WAL 마이그레이션
alloy tools prometheus.remote_write \
  migrate-wal \
  --wal-directory=/var/lib/alloy/data
```

#### 향후 추가될 도구

Alloy는 빠르게 발전 중 → `alloy tools --help`로 최신 도구 확인 필요.

---

### 공통 옵션

#### Logging

```bash
--log.level=info       # debug, info, warn, error
--log.format=logfmt    # 또는 json
```

#### Help

```bash
alloy --help
alloy run --help
alloy convert --help
```

#### Version

```bash
alloy --version

# 출력:
# alloy, version 1.0.0 (branch: HEAD, revision: abc123)
#   build user:       ci
#   build date:       2024-01-01
#   go version:       go1.21.5
#   platform:         linux/amd64
```

---

### 환경 변수

Alloy CLI는 환경 변수도 지원함.

- `ALLOY_DEPLOY_DIR`: `--storage.path`와 동등
- `HTTP_PROXY`: HTTP 프록시
- `HTTPS_PROXY`: HTTPS 프록시
- `NO_PROXY`: 프록시 제외

---

### 종합 예시

#### 프로덕션 systemd 실행

```bash
# /etc/default/alloy
CONFIG_FILE="/etc/alloy/config.alloy"
CUSTOM_ARGS="--server.http.listen-addr=0.0.0.0:12345 \
             --cluster.enabled=true \
             --cluster.discover-peers=provider=dns,name=alloy.internal,port=12345 \
             --cluster.advertise-interfaces=eth0 \
             --disable-reporting"
RESTART_ON_UPGRADE=true
ALLOY_DEPLOY_DIR="/var/lib/alloy/data"

# /lib/systemd/system/alloy.service
[Service]
EnvironmentFile=/etc/default/alloy
ExecStart=/usr/bin/alloy run $CUSTOM_ARGS --storage.path=$ALLOY_DEPLOY_DIR $CONFIG_FILE
```

#### Docker

```bash
docker run -d \
  --name alloy \
  -p 12345:12345 \
  -p 4317:4317 \
  -p 4318:4318 \
  -v /etc/alloy:/etc/alloy:ro \
  -v alloy-data:/var/lib/alloy/data \
  grafana/alloy:latest \
  run \
    --server.http.listen-addr=0.0.0.0:12345 \
    --storage.path=/var/lib/alloy/data \
    --disable-reporting \
    /etc/alloy/config.alloy
```

#### Kubernetes (Helm 외 직접)

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: alloy
          image: grafana/alloy:latest
          args:
            - run
            - --server.http.listen-addr=0.0.0.0:12345
            - --storage.path=/var/lib/alloy/data
            - --cluster.enabled=true
            - --cluster.discover-peers=provider=k8s,namespace=$(NAMESPACE),label_selector=app=alloy,port=12345
            - --cluster.advertise-address=$(POD_IP):12345
            - --disable-reporting
            - /etc/alloy/config.alloy
          env:
            - name: NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: POD_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.podIP
```

---

## Alloy HTTP 엔드포인트

> 원본: https://grafana.com/docs/alloy/latest/reference/cli/run/

---

### 목차

1. [개요](#개요)
2. [헬스/상태](#헬스상태)
3. [구성 관리](#구성-관리)
4. [컴포넌트 API](#컴포넌트-api)
5. [클러스터 API](#클러스터-api)
6. [메트릭](#메트릭)
7. [디버그 API](#디버그-api)
8. [UI](#ui)

---

### 개요

Alloy HTTP 서버는 기본 포트 12345에서 실행됨.

```bash
alloy run --server.http.listen-addr=0.0.0.0:12345 config.alloy
```

#### 인증

기본적으로 인증 미제공 → 운영 환경에서는 리버스 프록시 사용 권장.

---

### 헬스/상태

#### `GET /-/healthy`

기본 헬스체크 → Alloy 프로세스 실행 여부 확인.

```bash
curl http://localhost:12345/-/healthy
# OK
```

응답:
- 200: 정상
- 503: 비정상

#### `GET /-/ready`

준비 상태 확인 → 모든 컴포넌트의 준비 여부 반환.

```bash
curl http://localhost:12345/-/ready
# Ready
```

응답:
- 200: Ready
- 503: Not Ready

활용:
- Kubernetes liveness/readiness probe
- Load balancer health check

---

### 구성 관리

#### `POST /-/reload`

구성 파일을 다시 로드함.

```bash
curl -X POST http://localhost:12345/-/reload
```

응답:
- 200: 성공
- 400: 잘못된 구성(롤백됨)

리로드 동작:
1. 구성 파일 다시 읽기
2. 새 그래프 빌드
3. 변경된 컴포넌트만 재시작
4. 이전 컴포넌트 정리

#### `GET /api/v0/web/config`

현재 구성을 텍스트 형식으로 조회함.

```bash
curl http://localhost:12345/api/v0/web/config
```

---

### 컴포넌트 API

#### `GET /api/v0/web/components`

모든 컴포넌트의 목록과 상태를 반환함.

```bash
curl http://localhost:12345/api/v0/web/components
```

응답 예시:
```json
{
  "components": [
    {
      "moduleID": "",
      "localID": "prometheus.scrape.default",
      "label": "default",
      "name": "prometheus.scrape",
      "type": "block",
      "health": {
        "state": "healthy",
        "message": "started component",
        "updatedTime": "2024-01-01T00:00:00Z"
      },
      "original": "prometheus.scrape \"default\" { ... }"
    }
  ]
}
```

#### `GET /api/v0/web/components/<id>`

특정 컴포넌트의 상세 정보를 반환함.

```bash
curl http://localhost:12345/api/v0/web/components/prometheus.scrape.default
```

응답:
```json
{
  "moduleID": "",
  "localID": "prometheus.scrape.default",
  "name": "prometheus.scrape",
  "label": "default",
  "health": {...},
  "arguments": {...},
  "exports": {...},
  "debugInfo": {...},
  "createdModuleIDs": []
}
```

#### `GET /api/v0/web/peers`

클러스터 피어 목록을 반환함(클러스터링 활성화 시).

```bash
curl http://localhost:12345/api/v0/web/peers
```

---

### 클러스터 API

#### `GET /api/v1/cluster/peers`

클러스터의 모든 피어 정보를 반환함.

```bash
curl http://localhost:12345/api/v1/cluster/peers
```

응답:
```json
{
  "peers": [
    {
      "name": "alloy-0",
      "addr": "10.0.1.5:12345",
      "state": "alive",
      "is_self": true
    },
    {
      "name": "alloy-1",
      "addr": "10.0.1.6:12345",
      "state": "alive",
      "is_self": false
    }
  ]
}
```

#### `GET /api/v1/cluster/state`

클러스터 상태를 반환함.

상태 값:
- `alive`: 정상
- `suspect`: 의심
- `dead`: 죽음
- `left`: 떠남

---

### 메트릭

#### `GET /metrics`

Prometheus 형식으로 자체 메트릭을 노출함.

```bash
curl http://localhost:12345/metrics
```

#### 핵심 메트릭

##### Component Controller

```
alloy_component_controller_running_components{}
alloy_component_evaluation_seconds{}
alloy_component_evaluation_slow_seconds{}
alloy_component_dependencies_wait_seconds{}
```

##### Resources

```
alloy_resources_process_cpu_seconds_total
alloy_resources_process_resident_memory_bytes
alloy_resources_machine_memory_*
```

##### Cluster

```
cluster_node_peers
cluster_transport_packet_received_total
cluster_transport_packet_sent_total
cluster_transport_stream_*
```

##### Component-specific

각 컴포넌트가 자체 메트릭을 노출함.

```
prometheus_remote_write_*
loki_write_*
otelcol_*
```

---

### 디버그 API

#### `GET /debug/pprof/*`

Go pprof 프로파일 엔드포인트임.

```bash
# Heap
curl http://localhost:12345/debug/pprof/heap > heap.pprof

# CPU (30초)
curl http://localhost:12345/debug/pprof/profile?seconds=30 > cpu.pprof

# Goroutines
curl http://localhost:12345/debug/pprof/goroutine?debug=2

# Block (lock contention)
curl "http://localhost:12345/debug/pprof/block" > block.pprof

# Mutex
curl "http://localhost:12345/debug/pprof/mutex" > mutex.pprof

# Allocs
curl http://localhost:12345/debug/pprof/allocs > allocs.pprof
```

분석:
```bash
go tool pprof heap.pprof
# (pprof) top10
# (pprof) web
```

#### `GET /debug/fgprof`

Off-CPU 프로파일링 수행(실험적 기능).

```bash
curl "http://localhost:12345/debug/fgprof?seconds=30" > fgprof.pprof
```

---

### UI

#### Web UI

기본 주소: `http://localhost:12345/`

#### 주요 페이지

- `/`: 컴포넌트 그래프
- `/component/<id>`: 컴포넌트 상세
- `/cluster`: 클러스터 노드(클러스터링 활성화 시)
- `/-/ready`: Readiness
- `/-/healthy`: Liveness
- `/-/reload`: 구성 리로드(POST)

#### 컴포넌트 그래프

- 시각적 노드/엣지 표시
- 클릭으로 컴포넌트 상세 이동
- 헬스 상태 색상 표시 (녹색=healthy, 주황=degraded, 빨강=unhealthy)

#### 컴포넌트 상세

각 컴포넌트 페이지에서 확인 가능:

##### Arguments
컴포넌트에 전달된 인자를 표시함.

##### Exports
다른 컴포넌트가 참조할 수 있는 출력 값을 표시함.

##### Health
현재 상태와 마지막 업데이트 시각을 표시함.

##### Debug Info
컴포넌트별 실시간 디버그 정보를 표시함(예: `prometheus.scrape`의 마지막 스크래핑 시간 및 결과).

##### Live Debugging
`livedebugging` 활성화 시 실시간 입출력 데이터 확인 가능.

---

### 보안

#### 리버스 프록시 (Nginx)

```nginx
upstream alloy {
  server alloy-0:12345;
  server alloy-1:12345;
  server alloy-2:12345;
}

server {
  listen 443 ssl;
  server_name alloy.internal;
  
  ssl_certificate /etc/ssl/server.crt;
  ssl_certificate_key /etc/ssl/server.key;
  
  # Basic Auth
  auth_basic "Alloy";
  auth_basic_user_file /etc/nginx/.htpasswd;
  
  # 메트릭은 인증 없이 노출 (Prometheus용)
  location /metrics {
    auth_basic off;
    proxy_pass http://alloy;
  }
  
  # 헬스체크도 무인증
  location ~ ^/-/(healthy|ready)$ {
    auth_basic off;
    proxy_pass http://alloy;
  }
  
  # 나머지는 인증
  location / {
    proxy_pass http://alloy;
  }
}
```

#### Kubernetes RBAC

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: alloy-restrict
spec:
  podSelector:
    matchLabels:
      app: alloy
  policyTypes:
    - Ingress
  ingress:
    # Prometheus만 메트릭 접근
    - from:
        - podSelector:
            matchLabels:
              app: prometheus
      ports:
        - port: 12345
    # 다른 Alloy (클러스터링)
    - from:
        - podSelector:
            matchLabels:
              app: alloy
      ports:
        - port: 12345
```

---

### 운영 시나리오

#### 1. 자동화된 헬스체크

```yaml
# Kubernetes
livenessProbe:
  httpGet:
    path: /-/healthy
    port: 12345
  initialDelaySeconds: 10
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /-/ready
    port: 12345
  initialDelaySeconds: 5
  periodSeconds: 5
```

#### 2. 자동 리로드 (ConfigMap 변경 감지)

```yaml
# Sidecar
- name: config-reloader
  image: jimmidyson/configmap-reload:v0.12.0
  args:
    - --volume-dir=/etc/alloy
    - --webhook-url=http://localhost:12345/-/reload
```

#### 3. 컴포넌트 헬스 모니터링

```promql
# Unhealthy 컴포넌트 수
sum(alloy_component_controller_running_components{health_type="unhealthy"})

# 느린 평가
topk(10, alloy_component_evaluation_slow_seconds)
```

#### 4. 클러스터 검증

```bash
#!/bin/bash
EXPECTED_PEERS=3
ACTUAL=$(curl -s http://alloy:12345/api/v1/cluster/peers | jq '.peers | length')

if [ "$ACTUAL" != "$EXPECTED_PEERS" ]; then
  echo "ALERT: Expected $EXPECTED_PEERS peers, got $ACTUAL"
  exit 1
fi
```

#### 5. 디버그 정보 자동 수집

```bash
#!/bin/bash
# debug-bundle.sh - 문제 발생 시 디버그 정보 수집

OUTPUT_DIR="alloy-debug-$(date +%Y%m%d-%H%M%S)"
mkdir -p $OUTPUT_DIR

# 헬스
curl -s http://localhost:12345/-/healthy > $OUTPUT_DIR/health.txt
curl -s http://localhost:12345/-/ready > $OUTPUT_DIR/ready.txt

# 메트릭
curl -s http://localhost:12345/metrics > $OUTPUT_DIR/metrics.txt

# 컴포넌트
curl -s http://localhost:12345/api/v0/web/components > $OUTPUT_DIR/components.json

# 구성
curl -s http://localhost:12345/api/v0/web/config > $OUTPUT_DIR/config.alloy

# 프로파일
curl -s http://localhost:12345/debug/pprof/heap > $OUTPUT_DIR/heap.pprof
curl -s http://localhost:12345/debug/pprof/goroutine?debug=2 > $OUTPUT_DIR/goroutine.txt

# 클러스터 (있다면)
curl -s http://localhost:12345/api/v1/cluster/peers > $OUTPUT_DIR/peers.json 2>/dev/null

tar -czf $OUTPUT_DIR.tar.gz $OUTPUT_DIR
echo "Debug bundle: $OUTPUT_DIR.tar.gz"
```
