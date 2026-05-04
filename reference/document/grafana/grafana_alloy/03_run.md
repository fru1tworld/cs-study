# Alloy 실행

> 이 문서는 Grafana Alloy 공식 문서의 "Run Alloy" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/set-up/run/

---

## 목차

1. [실행 명령어](#실행-명령어)
2. [systemd 서비스](#systemd-서비스)
3. [환경 변수](#환경-변수)
4. [구성 다시 로드](#구성-다시-로드)
5. [로그 확인](#로그-확인)
6. [UI 사용](#ui-사용)
7. [메트릭 노출](#메트릭-노출)
8. [디버그 모드](#디버그-모드)
9. [구성 검증](#구성-검증)

---

## 실행 명령어

### 기본 실행

```bash
alloy run /etc/alloy/config.alloy
```

### 주요 옵션

```bash
alloy run \
  --server.http.listen-addr=0.0.0.0:12345 \
  --storage.path=/var/lib/alloy/data \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --disable-reporting \
  /etc/alloy/config.alloy
```

### 명령어 목록

| 명령 | 설명 |
|------|------|
| `alloy run` | Alloy 실행 |
| `alloy validate` | 구성 파일 검증 |
| `alloy fmt` | 구성 포맷 정리 |
| `alloy convert` | 다른 포맷에서 변환 |
| `alloy tools` | 보조 도구 (예: prometheus.relabel 평가) |
| `alloy --version` | 버전 확인 |

### `alloy run` 주요 플래그

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--server.http.listen-addr` | `127.0.0.1:12345` | HTTP 서버 |
| `--server.http.ui-path-prefix` | `/` | UI 경로 |
| `--storage.path` | `data-alloy/` | WAL 등 저장 |
| `--config.format` | `alloy` | 구성 형식 |
| `--config.bypass-conversion-warnings` | false | 변환 경고 무시 |
| `--cluster.enabled` | false | 클러스터링 활성화 |
| `--cluster.join-addresses` | "" | 가입할 노드들 |
| `--cluster.advertise-address` | "" | 광고 주소 |
| `--cluster.discover-peers` | "" | 피어 자동 발견 (DNS, K8s) |
| `--disable-reporting` | false | Grafana로 사용 통계 보고 비활성 |
| `--stability.level` | generally-available | 활성화할 안정성 레벨 (experimental, public-preview, generally-available) |

---

## systemd 서비스

### 패키지 설치 시 자동 등록

```bash
sudo systemctl status alloy
sudo systemctl restart alloy
sudo systemctl enable alloy
```

### 기본 유닛 파일

`/lib/systemd/system/alloy.service`:

```ini
[Unit]
Description=Vendor-neutral programmable observability pipelines.
Documentation=https://grafana.com/docs/alloy/latest/
After=network-online.target
Wants=network-online.target

[Service]
EnvironmentFile=/etc/default/alloy
User=alloy
ExecStart=/usr/bin/alloy run $CUSTOM_ARGS --storage.path=$ALLOY_DEPLOY_DIR $CONFIG_FILE
Restart=always
RestartSec=60

[Install]
WantedBy=multi-user.target
```

### 환경 파일

`/etc/default/alloy`:

```bash
## 구성 파일
CONFIG_FILE="/etc/alloy/config.alloy"

## User-defined arguments to pass to the run command
CUSTOM_ARGS="--server.http.listen-addr=0.0.0.0:12345"

## Restart on system upgrade. Defaults to true.
RESTART_ON_UPGRADE=true

## 데이터 디렉토리
ALLOY_DEPLOY_DIR="/var/lib/alloy/data"
```

### 사용자 정의

```bash
sudo systemctl edit alloy
```

```ini
[Service]
Environment="CUSTOM_ARGS=--server.http.listen-addr=0.0.0.0:12345 --cluster.enabled=true"
```

---

## 환경 변수

Alloy 구성에서 `sys.env()`로 환경변수 참조 가능.

### 구성에서 사용

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")
    
    basic_auth {
      username = sys.env("MIMIR_USER")
      password = sys.env("MIMIR_PASSWORD")
    }
  }
  
  external_labels = {
    cluster = sys.env("CLUSTER_NAME"),
    region  = sys.env("AWS_REGION"),
  }
}
```

### 실행 시 전달

```bash
MIMIR_URL=https://mimir.example.com \
MIMIR_USER=alloy \
MIMIR_PASSWORD=$(cat /run/secrets/mimir-pass) \
alloy run config.alloy
```

### Docker에서

```bash
docker run -e MIMIR_URL=... -e MIMIR_USER=... grafana/alloy ...
```

### Kubernetes에서

```yaml
env:
  - name: MIMIR_URL
    value: https://mimir.example.com
  - name: MIMIR_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mimir-credentials
        key: password
```

---

## 구성 다시 로드

### SIGHUP으로 리로드

```bash
sudo systemctl reload alloy
# 또는
kill -HUP $(pgrep alloy)
```

### HTTP 엔드포인트로 리로드

```bash
curl -X POST http://localhost:12345/-/reload
```

### Kubernetes ConfigReloader

`configReloader.enabled: true` 설정 시 ConfigMap 변경을 자동 감지.

```yaml
configReloader:
  enabled: true
  image:
    repository: jimmidyson/configmap-reload
    tag: v0.12.0
```

---

## 로그 확인

### systemd

```bash
# 실시간
journalctl -u alloy -f

# 최근 1시간
journalctl -u alloy --since "1 hour ago"

# 에러만
journalctl -u alloy -p err

# JSON 형식
journalctl -u alloy -o json | jq
```

### Docker

```bash
docker logs -f alloy
docker logs alloy --tail 100
```

### Kubernetes

```bash
kubectl logs -f -n alloy daemonset/alloy
kubectl logs -n alloy alloy-xxxxx --previous
```

### 로그 레벨 변경

구성 파일:

```alloy
logging {
  level  = "debug"      // info, debug, warn, error
  format = "logfmt"     // 또는 json
}
```

### 로그를 다른 컴포넌트로

```alloy
logging {
  level  = "info"
  format = "logfmt"
  write_to = [loki.write.self.receiver]
}

loki.write "self" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
  external_labels = {
    job = "alloy",
  }
}
```

---

## UI 사용

### 접속

기본 URL: `http://localhost:12345`

### 주요 화면

| 페이지 | 내용 |
|--------|------|
| **Components** | 모든 컴포넌트 그래프 |
| **Component Detail** | 컴포넌트별 상태, 인자, 출력, 디버그 정보 |
| **Cluster** | 클러스터링 활성화 시 노드 목록 |
| **Targets** | 스크래핑 타겟 상태 |

### URL 경로 예시

- `/` - 컴포넌트 목록
- `/component/<id>` - 컴포넌트 상세
- `/cluster` - 클러스터 상태
- `/-/healthy` - 헬스체크
- `/-/ready` - 준비 상태
- `/-/reload` - 구성 리로드 (POST)
- `/metrics` - Prometheus 메트릭

### 보안 (인증)

Alloy 자체는 인증 미제공. 리버스 프록시 권장:

```nginx
server {
  listen 80;
  server_name alloy.internal;
  
  location / {
    auth_basic "Restricted";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://localhost:12345;
  }
}
```

---

## 메트릭 노출

`/metrics` 엔드포인트로 자체 메트릭 노출.

### Prometheus로 수집

```yaml
scrape_configs:
  - job_name: alloy
    static_configs:
      - targets: ['alloy:12345']
```

### Alloy로 자기 자신 수집

```alloy
prometheus.scrape "self" {
  targets = [
    {__address__ = "127.0.0.1:12345"},
  ]
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  job_name = "alloy"
}
```

### 핵심 메트릭

| 메트릭 | 설명 |
|--------|------|
| `alloy_component_controller_running_components` | 실행 중인 컴포넌트 수 |
| `alloy_component_evaluation_seconds` | 컴포넌트 평가 시간 |
| `alloy_resources_process_*` | 프로세스 리소스 |
| `prometheus_remote_write_*` | Remote Write 통계 |
| `loki_write_*` | Loki Write 통계 |

---

## 디버그 모드

### 디버그 로깅

```alloy
logging {
  level = "debug"
}
```

### 컴포넌트별 디버그 정보

UI 컴포넌트 상세 페이지의 **Debug Info** 섹션에서 실시간 정보 확인.

예: `prometheus.scrape`의 디버그 정보
- 마지막 스크래핑 시간
- 스크래핑된 메트릭 수
- 에러 메시지

### Stability Level 활성화

실험적 컴포넌트 사용 시:

```bash
alloy run --stability.level=experimental config.alloy
```

레벨:
- `generally-available` (기본)
- `public-preview`
- `experimental`

### Trace 활성화

```alloy
tracing {
  sampling_fraction = 0.1
  
  write_to = [otelcol.exporter.otlp.tempo.input]
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls { insecure = true }
  }
}
```

---

## 구성 검증

### 실행 전 검증

```bash
alloy validate config.alloy
```

성공:
```
config.alloy is valid
```

실패 시 오류 위치와 원인 표시.

### 포맷 정리

```bash
alloy fmt config.alloy        # stdout에 출력
alloy fmt -w config.alloy     # 파일을 직접 수정
```

### 변환

```bash
# Promtail → Alloy
alloy convert --source-format=promtail \
  -o alloy-config.alloy \
  promtail-config.yaml

# Prometheus → Alloy (scrape 부분)
alloy convert --source-format=prometheus \
  -o alloy-config.alloy \
  prometheus.yml

# Static Agent → Alloy
alloy convert --source-format=static \
  -o alloy-config.alloy \
  agent-static.yaml

# Flow → Alloy
alloy convert --source-format=flow \
  -o alloy-config.alloy \
  flow-config.river

# OpenTelemetry → Alloy
alloy convert --source-format=otelcol \
  -o alloy-config.alloy \
  otel-collector.yaml
```

### 변환 옵션

```bash
--bypass-errors   # 에러 무시하고 가능한 부분만 변환
--report=report.txt   # 변환 보고서 저장
```
