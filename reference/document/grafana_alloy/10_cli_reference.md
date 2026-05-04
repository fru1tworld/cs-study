# Alloy CLI 레퍼런스

> 이 문서는 Grafana Alloy 공식 문서의 CLI 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/reference/cli/

---

## 목차

1. [개요](#개요)
2. [`alloy run`](#alloy-run)
3. [`alloy validate`](#alloy-validate)
4. [`alloy fmt`](#alloy-fmt)
5. [`alloy convert`](#alloy-convert)
6. [`alloy tools`](#alloy-tools)
7. [공통 옵션](#공통-옵션)

---

## 개요

```
alloy [global flags] <command> [command flags] [args]
```

### 명령 목록

| 명령 | 설명 |
|------|------|
| `run` | Alloy 실행 |
| `validate` | 구성 파일 검증 |
| `fmt` | 구성 포맷 정리 |
| `convert` | 다른 형식에서 변환 |
| `tools` | 보조 도구 |
| `--version` | 버전 확인 |
| `--help` | 도움말 |

---

## `alloy run`

가장 많이 쓰는 명령. Alloy 실행.

```bash
alloy run [flags] <config-path>
```

### 주요 플래그

#### Server

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--server.http.listen-addr` | `127.0.0.1:12345` | HTTP 서버 |
| `--server.http.ui-path-prefix` | `/` | UI 경로 |
| `--server.http.memory-addr` | `alloy.internal:12345` | 메모리 listener (테스트) |

#### Storage

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--storage.path` | `data-alloy/` | WAL, 중간 상태 저장 |

#### Config

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--config.format` | `alloy` | `alloy`, `prometheus`, `promtail`, `static`, `flow`, `otelcol` |
| `--config.bypass-conversion-warnings` | false | 변환 경고 무시 |
| `--config.extra-args` | "" | static 변환 시 추가 인자 |

#### Cluster

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--cluster.enabled` | false | 클러스터링 활성화 |
| `--cluster.node-name` | (hostname) | 노드 이름 |
| `--cluster.advertise-address` | "" | 광고할 주소 |
| `--cluster.advertise-interfaces` | (auto) | 자동 광고할 NIC |
| `--cluster.join-addresses` | "" | 가입할 노드 |
| `--cluster.discover-peers` | "" | 자동 피어 발견 |
| `--cluster.rejoin-interval` | `60s` | 재가입 시도 주기 |
| `--cluster.max-join-peers` | 5 | 한 번에 가입할 피어 수 |
| `--cluster.name` | "" | 클러스터 이름 |
| `--cluster.wait-for-size` | 1 | 시작 전 대기할 클러스터 크기 |
| `--cluster.wait-timeout` | `0s` | 클러스터 대기 타임아웃 |

#### Cluster TLS (gossip)

| 플래그 | 설명 |
|--------|------|
| `--cluster.tls-ca-path` | CA 인증서 |
| `--cluster.tls-cert-path` | 클라이언트 인증서 |
| `--cluster.tls-key-path` | 클라이언트 키 |
| `--cluster.tls-server-name` | 서버 이름 |

#### Stability

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--stability.level` | `generally-available` | `experimental`, `public-preview`, `generally-available` |

#### Reporting

| 플래그 | 기본값 | 설명 |
|--------|--------|------|
| `--disable-reporting` | false | Grafana로 사용 통계 보고 비활성화 |

#### Live Debugging

| 플래그 | 설명 |
|--------|------|
| `--feature.community-components.enabled` | 커뮤니티 컴포넌트 활성화 |

### 사용 예시

#### 기본 실행

```bash
alloy run /etc/alloy/config.alloy
```

#### 클러스터 모드

```bash
alloy run \
  --server.http.listen-addr=0.0.0.0:12345 \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  /etc/alloy/config.alloy
```

#### Kubernetes 디스커버리

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345" \
  /etc/alloy/config.alloy
```

#### 실험적 컴포넌트 활성화

```bash
alloy run --stability.level=experimental /etc/alloy/config.alloy
```

#### 다른 형식 직접 실행

```bash
alloy run --config.format=prometheus /etc/prometheus/prometheus.yml
```

내부적으로 변환 후 실행. 일회성 실행에 유용.

---

## `alloy validate`

구성 파일 검증.

```bash
alloy validate [flags] <config-path>
```

### 옵션

| 플래그 | 설명 |
|--------|------|
| `--config.format` | 구성 형식 |
| `--config.bypass-conversion-warnings` | 변환 경고 무시 |
| `--feature.community-components.enabled` | 커뮤니티 컴포넌트 |
| `--stability.level` | 안정성 레벨 |

### 사용

```bash
# 검증
alloy validate config.alloy

# 성공 시
config.alloy is valid

# 실패 시 (구체적 오류 표시)
config.alloy:5:3: argument "url" required but not provided
```

### CI/CD 통합

```yaml
# .github/workflows/alloy.yml
- name: Validate Alloy config
  run: |
    docker run --rm -v $(pwd):/etc/alloy \
      grafana/alloy:latest \
      validate /etc/alloy/config.alloy
```

---

## `alloy fmt`

구성 파일 포맷 정리.

```bash
alloy fmt [flags] <config-path>
```

### 옵션

| 플래그 | 설명 |
|--------|------|
| `-w, --write` | 파일을 직접 수정 |
| `-d, --diff` | diff만 출력 |

### 사용

```bash
# stdout에 포맷된 결과 출력
alloy fmt config.alloy

# 파일 수정
alloy fmt -w config.alloy

# 변경 사항 미리보기
alloy fmt -d config.alloy
```

### Pre-commit hook

```bash
#!/bin/bash
# .git/hooks/pre-commit
for file in $(git diff --cached --name-only --diff-filter=ACM | grep '\.alloy$'); do
  alloy fmt -w "$file"
  git add "$file"
done
```

---

## `alloy convert`

다른 도구의 구성을 Alloy 구성으로 변환.

```bash
alloy convert [flags] <input-file>
```

### 주요 플래그

| 플래그 | 설명 |
|--------|------|
| `--source-format=<format>` | 입력 형식 (필수) |
| `-o, --output=<file>` | 출력 파일 |
| `-r, --report=<file>` | 변환 보고서 |
| `-b, --bypass-errors` | 에러 무시 |
| `--extra-args=<args>` | 형식별 추가 인자 |

### 지원 형식

| Source Format | 변환 대상 |
|---------------|----------|
| `prometheus` | Prometheus 구성 (메트릭 스크래핑) |
| `promtail` | Promtail 구성 (Loki 로그) |
| `static` | Grafana Agent Static |
| `flow` | Grafana Agent Flow (River) |
| `otelcol` | OpenTelemetry Collector |

### 사용 예시

#### Promtail → Alloy

```bash
alloy convert \
  --source-format=promtail \
  -o alloy-config.alloy \
  -r conversion-report.txt \
  promtail-config.yaml
```

#### Prometheus → Alloy

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  prometheus.yml
```

#### Static Agent → Alloy

```bash
alloy convert \
  --source-format=static \
  --extra-args="-enable-features=integrations-next" \
  -o alloy-config.alloy \
  agent-static.yaml
```

#### Flow → Alloy

```bash
alloy convert \
  --source-format=flow \
  -o alloy-config.alloy \
  flow-config.river
```

#### OTel Collector → Alloy

```bash
alloy convert \
  --source-format=otelcol \
  -o alloy-config.alloy \
  otel-collector.yaml
```

### 변환 보고서

`--report` 옵션으로 변환 시 발생한 경고/에러 기록:

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

## `alloy tools`

보조 도구 모음.

```bash
alloy tools <subcommand>
```

### `alloy tools prometheus.remote_write`

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

### 향후 추가될 도구

Alloy는 빠르게 진화 중. `alloy tools --help` 로 최신 도구 확인.

---

## 공통 옵션

### Logging

```bash
--log.level=info       # debug, info, warn, error
--log.format=logfmt    # 또는 json
```

### Help

```bash
alloy --help
alloy run --help
alloy convert --help
```

### Version

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

## 환경 변수

Alloy CLI는 환경변수도 지원.

| 환경변수 | 동등 플래그 |
|---------|-----------|
| `ALLOY_DEPLOY_DIR` | `--storage.path` |
| `HTTP_PROXY` | HTTP 프록시 |
| `HTTPS_PROXY` | HTTPS 프록시 |
| `NO_PROXY` | 프록시 제외 |

---

## 종합 예시

### 프로덕션 systemd 실행

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

### Docker

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

### Kubernetes (Helm 외 직접)

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
