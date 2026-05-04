# Loki로 로그 전송

> 이 문서는 Grafana Loki 공식 문서의 "Send logs to Loki" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/send-data/

---

## 목차

1. [개요](#개요)
2. [Grafana Alloy (권장)](#grafana-alloy-권장)
3. [Promtail (Deprecated)](#promtail-deprecated)
4. [OpenTelemetry Collector](#opentelemetry-collector)
5. [Docker Driver](#docker-driver)
6. [Fluentd](#fluentd)
7. [Fluent Bit](#fluent-bit)
8. [Logstash](#logstash)
9. [Vector](#vector)
10. [언어/프레임워크 클라이언트](#언어프레임워크-클라이언트)
11. [Loki HTTP Push API](#loki-http-push-api)

---

## 개요

Loki는 다양한 클라이언트로부터 로그를 받을 수 있습니다. 모두 **HTTP 기반 Push 방식** 으로 데이터를 전송합니다.

### 주요 클라이언트 비교

| 클라이언트 | 용도 | 권장도 |
|----------|------|--------|
| **Grafana Alloy** | 통합 텔레메트리 (메트릭, 로그, 트레이스) | 강력 권장 |
| **OpenTelemetry Collector** | OTel 표준 환경 | 권장 |
| **Promtail** | 기존 사용자 | Deprecated → Alloy로 이전 |
| **Docker Driver** | Docker 환경 단순 통합 | Docker 환경 |
| **Fluent Bit** | Kubernetes, 가벼운 수집기 | 좋음 |
| **Fluentd** | 기존 Fluentd 사용자 | 좋음 |
| **Logstash** | Elastic Stack 전환 | 보조 |
| **Vector** | 고성능 라우팅/처리 | 좋음 |

---

## Grafana Alloy (권장)

Alloy는 OpenTelemetry Collector 배포판으로, 메트릭/로그/트레이스를 통합 수집합니다.

### 파일 로그 수집 예시

```alloy
// 디스커버리: 파일 패턴
local.file_match "logs" {
  path_targets = [
    {__path__ = "/var/log/*.log"},
  ]
}

// 소스: 파일 읽기
loki.source.file "logs" {
  targets    = local.file_match.logs.targets
  forward_to = [loki.process.add_labels.receiver]
}

// 처리: 라벨 추가
loki.process "add_labels" {
  forward_to = [loki.write.default.receiver]
  
  stage.static_labels {
    values = {
      environment = "production",
      cluster     = "us-east-1",
    }
  }
}

// 전송
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

### Kubernetes Pod 로그 수집

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.relabel "pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_name"]
    target_label  = "pod"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_container_name"]
    target_label  = "container"
  }
}

loki.source.kubernetes "pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

### Journal (systemd) 로그

```alloy
loki.source.journal "journal" {
  forward_to = [loki.write.default.receiver]
  labels = {
    job = "systemd-journal",
  }
}
```

### Syslog 수신

```alloy
loki.source.syslog "syslog" {
  listener {
    address = "0.0.0.0:1514"
    protocol = "tcp"
  }
  forward_to = [loki.write.default.receiver]
}
```

---

## Promtail (Deprecated)

Promtail은 Loki 전용 로그 수집기로 Grafana Agent의 일부였습니다. 현재는 **Alloy로 통합** 되어 향후 폐기 예정입니다.

### Promtail 구성 예시

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: varlogs
          __path__: /var/log/*.log
    
    pipeline_stages:
      - regex:
          expression: '^(?P<timestamp>\S+) (?P<level>\S+) (?P<msg>.*)$'
      - labels:
          level:
      - timestamp:
          source: timestamp
          format: RFC3339
```

### Alloy로 변환

```bash
alloy convert --source-format=promtail \
  -o alloy-config.alloy \
  promtail-config.yaml
```

---

## OpenTelemetry Collector

Loki는 **OTLP HTTP** 로그 수신을 네이티브 지원합니다.

### Loki 설정

```yaml
distributor:
  otlp_config:
    default_resource_attributes_as_index_labels:
      - service.name
      - service.namespace
      - deployment.environment
```

### OTel Collector 구성

```yaml
receivers:
  filelog:
    include: ["/var/log/*.log"]

processors:
  batch:
    timeout: 5s

exporters:
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
    headers:
      X-Scope-OrgID: tenant-1

service:
  pipelines:
    logs:
      receivers: [filelog]
      processors: [batch]
      exporters: [otlphttp/loki]
```

### OTel 속성 → Loki 라벨 매핑

OTel Resource Attributes의 마침표는 언더스코어로 변환됩니다.

| OTel Attribute | Loki Label |
|----------------|-----------|
| `service.name` | `service_name` |
| `service.namespace` | `service_namespace` |
| `deployment.environment` | `deployment_environment` |
| `k8s.namespace.name` | `k8s_namespace_name` |

---

## Docker Driver

Docker 컨테이너의 stdout/stderr을 자동으로 Loki로 전송합니다.

### 설치

```bash
docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
```

### 컨테이너 단위 사용

```bash
docker run --log-driver=loki \
  --log-opt loki-url="http://loki:3100/loki/api/v1/push" \
  --log-opt loki-retries=5 \
  --log-opt loki-batch-size=400 \
  nginx
```

### 데몬 기본값으로 설정

```json
// /etc/docker/daemon.json
{
  "log-driver": "loki",
  "log-opts": {
    "loki-url": "http://loki:3100/loki/api/v1/push",
    "loki-batch-size": "400"
  }
}
```

```bash
systemctl restart docker
```

### 자동 추가 라벨

- `container_name`
- `compose_project`, `compose_service`
- `swarm_stack`, `swarm_service`
- `host`

---

## Fluentd

`fluent-plugin-grafana-loki` 사용.

### 설치

```bash
gem install fluent-plugin-grafana-loki
```

### 구성 예시

```xml
<match **>
  @type loki
  url "http://loki:3100"
  extra_labels {"env":"production"}
  
  <label>
    fluentd_worker
  </label>
  
  flush_interval 10s
  flush_at_shutdown true
  buffer_chunk_limit 1m
</match>
```

---

## Fluent Bit

Loki는 Fluent Bit의 공식 출력 플러그인을 가지고 있습니다.

### 구성 예시

```ini
[INPUT]
    Name              tail
    Path              /var/log/*.log
    Parser            docker
    Tag               kube.*
    Refresh_Interval  5

[OUTPUT]
    Name        loki
    Match       *
    Host        loki
    Port        3100
    Labels      job=fluentbit, env=prod
    Label_keys  $level,$app
    Auto_Kubernetes_Labels  on
    Tenant_ID   tenant-1
```

### Helm으로 Kubernetes 배포

```yaml
# values.yaml
config:
  outputs: |
    [OUTPUT]
        Name loki
        Match *
        Host loki-gateway.loki.svc.cluster.local
        Port 80
        Labels job=fluentbit
```

---

## Logstash

`logstash-output-loki` 플러그인 사용.

### 설치

```bash
bin/logstash-plugin install logstash-output-loki
```

### 구성

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => { "message" => "%{COMBINEDAPACHELOG}" }
  }
}

output {
  loki {
    url => "http://loki:3100/loki/api/v1/push"
    username => "user"
    password => "pass"
    batch_size => 112640
    retries => 5
    
    # 라벨 매핑
    tenant_id => "tenant-1"
  }
}
```

---

## Vector

`loki` 싱크 사용.

### 구성 (TOML)

```toml
[sources.app_logs]
type = "file"
include = ["/var/log/app/*.log"]

[transforms.parse]
type = "remap"
inputs = ["app_logs"]
source = '''
  . = parse_json!(.message)
'''

[sinks.loki]
type = "loki"
inputs = ["parse"]
endpoint = "http://loki:3100"
encoding.codec = "json"

  [sinks.loki.labels]
  app = "myapp"
  env = "production"
  level = "{{ level }}"
```

---

## 언어/프레임워크 클라이언트

직접 Loki Push API로 전송하는 라이브러리들.

### Java (Log4j2)

`com.github.loki4j:loki-logback-appender`

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
  <http>
    <url>http://loki:3100/loki/api/v1/push</url>
  </http>
  <format>
    <label>
      <pattern>app=myapp,host=${HOSTNAME},level=%level</pattern>
    </label>
    <message>
      <pattern>%msg %ex</pattern>
    </message>
  </format>
</appender>
```

### Python

`python-logging-loki`:

```python
import logging
import logging_loki

handler = logging_loki.LokiHandler(
    url="http://loki:3100/loki/api/v1/push",
    tags={"app": "myapp"},
    auth=("user", "pass"),
    version="1",
)

logger = logging.getLogger("my-app")
logger.addHandler(handler)
logger.error("error occurred")
```

### Go

`github.com/grafana/loki/pkg/promtail/client` 또는 `github.com/grafana/loki-client-go`

```go
client, _ := loki.New(loki.Config{
    URL: "http://loki:3100/loki/api/v1/push",
})

client.Handle(model.LabelSet{"app": "myapp"}, time.Now(), "log message")
```

### Node.js

`winston-loki`:

```js
const winston = require("winston");
const LokiTransport = require("winston-loki");

const logger = winston.createLogger({
  transports: [
    new LokiTransport({
      host: "http://loki:3100",
      labels: { app: "myapp" },
      json: true,
    }),
  ],
});

logger.info("hello");
```

---

## Loki HTTP Push API

직접 API 호출도 가능합니다.

### 엔드포인트

```
POST /loki/api/v1/push
```

### 요청 형식 (JSON)

```bash
curl -H "Content-Type: application/json" \
     -H "X-Scope-OrgID: tenant-1" \
     -XPOST "http://loki:3100/loki/api/v1/push" \
     --data-raw '{
       "streams": [
         {
           "stream": {"app": "myapp", "level": "info"},
           "values": [
             ["1700000000000000000", "log line 1"],
             ["1700000001000000000", "log line 2"]
           ]
         }
       ]
     }'
```

### 요청 형식 (Protobuf, Snappy 압축)

대용량 환경에서는 Protobuf + Snappy 권장.

- `Content-Type: application/x-protobuf`
- `Content-Encoding: snappy`
- 페이로드: snappy 압축된 protobuf

### 응답

| 상태 코드 | 의미 |
|---------|------|
| 204 | 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 429 | Rate Limit 초과 |
| 500 | 서버 에러 |

### 구조화된 메타데이터 (Structured Metadata)

Loki 3.0부터 지원. 라벨로 인덱싱하지 않지만 쿼리에 사용 가능한 메타데이터.

```json
{
  "streams": [
    {
      "stream": {"app": "api"},
      "values": [
        ["1700000000000000000", "log line", {"trace_id": "abc123", "user_id": "u-123"}]
      ]
    }
  ]
}
```
