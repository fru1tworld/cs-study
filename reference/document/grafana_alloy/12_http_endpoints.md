# Alloy HTTP 엔드포인트

> 이 문서는 Grafana Alloy 공식 문서의 HTTP Endpoints 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/reference/cli/run/

---

## 목차

1. [개요](#개요)
2. [헬스/상태](#헬스상태)
3. [구성 관리](#구성-관리)
4. [컴포넌트 API](#컴포넌트-api)
5. [클러스터 API](#클러스터-api)
6. [메트릭](#메트릭)
7. [디버그 API](#디버그-api)
8. [UI](#ui)

---

## 개요

Alloy의 HTTP 서버는 기본 포트 **12345** 에서 실행됩니다.

```bash
alloy run --server.http.listen-addr=0.0.0.0:12345 config.alloy
```

### 인증

기본적으로 인증 없음. 운영 환경에서는 리버스 프록시 권장.

---

## 헬스/상태

### `GET /-/healthy`

기본 헬스체크. Alloy 프로세스가 살아있는지.

```bash
curl http://localhost:12345/-/healthy
# OK
```

응답:
- 200: 정상
- 503: 비정상

### `GET /-/ready`

준비 상태. 모든 컴포넌트가 준비되었는지.

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

## 구성 관리

### `POST /-/reload`

구성 파일 리로드.

```bash
curl -X POST http://localhost:12345/-/reload
```

응답:
- 200: 성공
- 400: 잘못된 구성 (롤백됨)

리로드 동작:
1. 구성 파일 다시 읽기
2. 새 그래프 빌드
3. 변경된 컴포넌트만 재시작
4. 이전 컴포넌트 정리

### `GET /api/v0/web/config`

현재 구성을 텍스트로 조회.

```bash
curl http://localhost:12345/api/v0/web/config
```

---

## 컴포넌트 API

### `GET /api/v0/web/components`

모든 컴포넌트 목록과 상태.

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

### `GET /api/v0/web/components/<id>`

특정 컴포넌트 상세.

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

### `GET /api/v0/web/peers`

클러스터 피어 목록 (클러스터링 활성화 시).

```bash
curl http://localhost:12345/api/v0/web/peers
```

---

## 클러스터 API

### `GET /api/v1/cluster/peers`

클러스터의 모든 피어.

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

### `GET /api/v1/cluster/state`

클러스터 상태.

상태 값:
- `alive`: 정상
- `suspect`: 의심
- `dead`: 죽음
- `left`: 떠남

---

## 메트릭

### `GET /metrics`

Prometheus 형식 자체 메트릭.

```bash
curl http://localhost:12345/metrics
```

### 핵심 메트릭

#### Component Controller

```
alloy_component_controller_running_components{}
alloy_component_evaluation_seconds{}
alloy_component_evaluation_slow_seconds{}
alloy_component_dependencies_wait_seconds{}
```

#### Resources

```
alloy_resources_process_cpu_seconds_total
alloy_resources_process_resident_memory_bytes
alloy_resources_machine_memory_*
```

#### Cluster

```
cluster_node_peers
cluster_transport_packet_received_total
cluster_transport_packet_sent_total
cluster_transport_stream_*
```

#### Component-specific

각 컴포넌트가 자체 메트릭 노출:

```
prometheus_remote_write_*
loki_write_*
otelcol_*
```

---

## 디버그 API

### `GET /debug/pprof/*`

Go pprof 프로파일.

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

### `GET /debug/fgprof`

Off-CPU 프로파일링 (실험적).

```bash
curl "http://localhost:12345/debug/fgprof?seconds=30" > fgprof.pprof
```

---

## UI

### Web UI

기본 접속: `http://localhost:12345/`

### 주요 페이지

| 경로 | 내용 |
|------|------|
| `/` | 컴포넌트 그래프 |
| `/component/<id>` | 컴포넌트 상세 |
| `/cluster` | 클러스터 노드 (클러스터링 활성화 시) |
| `/-/ready` | Readiness |
| `/-/healthy` | Liveness |
| `/-/reload` | 구성 리로드 (POST) |

### 컴포넌트 그래프

- 시각적 노드/엣지 표시
- 클릭으로 컴포넌트 상세 이동
- 헬스 상태 색상 표시 (녹색=healthy, 주황=degraded, 빨강=unhealthy)

### 컴포넌트 상세

각 컴포넌트 페이지에서 확인 가능:

#### Arguments
컴포넌트에 전달된 인자.

#### Exports
다른 컴포넌트가 참조 가능한 출력.

#### Health
현재 상태와 마지막 업데이트.

#### Debug Info
컴포넌트별 실시간 디버그 정보 (예: `prometheus.scrape`의 마지막 스크래핑 시간/결과).

#### Live Debugging
`livedebugging` 활성화 시 실시간 입출력 데이터 확인.

---

## 보안

### 리버스 프록시 (Nginx)

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

### Kubernetes RBAC

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

## 운영 시나리오

### 1. 자동화된 헬스체크

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

### 2. 자동 리로드 (ConfigMap 변경 감지)

```yaml
# Sidecar
- name: config-reloader
  image: jimmidyson/configmap-reload:v0.12.0
  args:
    - --volume-dir=/etc/alloy
    - --webhook-url=http://localhost:12345/-/reload
```

### 3. 컴포넌트 헬스 모니터링

```promql
# Unhealthy 컴포넌트 수
sum(alloy_component_controller_running_components{health_type="unhealthy"})

# 느린 평가
topk(10, alloy_component_evaluation_slow_seconds)
```

### 4. 클러스터 검증

```bash
#!/bin/bash
EXPECTED_PEERS=3
ACTUAL=$(curl -s http://alloy:12345/api/v1/cluster/peers | jq '.peers | length')

if [ "$ACTUAL" != "$EXPECTED_PEERS" ]; then
  echo "ALERT: Expected $EXPECTED_PEERS peers, got $ACTUAL"
  exit 1
fi
```

### 5. 디버그 정보 자동 수집

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
