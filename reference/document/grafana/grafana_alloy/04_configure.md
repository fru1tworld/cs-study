# Alloy 구성 (Configuration Syntax)

> 이 문서는 Grafana Alloy 공식 문서의 "Configure Alloy" / "Concepts" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/concepts/configuration-syntax/

---

## 목차

1. [구성 언어 개요](#구성-언어-개요)
2. [블록(Blocks)](#블록blocks)
3. [속성(Attributes)](#속성attributes)
4. [표현식(Expressions)](#표현식expressions)
5. [데이터 타입](#데이터-타입)
6. [참조(Reference)와 그래프](#참조reference와-그래프)
7. [표준 라이브러리](#표준-라이브러리)
8. [모듈(Modules)](#모듈modules)
9. [디스커버리와 라벨링](#디스커버리와-라벨링)
10. [구성 예시 모음](#구성-예시-모음)

---

## 구성 언어 개요

Alloy는 **자체 구성 언어(Alloy syntax)** 를 사용합니다 (이전 명칭: River). HCL과 유사한 문법.

### 특징

- 선언적
- 타입 안전 (정적 타입 체크)
- 표현식 지원
- 컴포넌트 그래프 자동 생성

### 기본 구조

```alloy
// 주석은 // 또는 /* */

// 블록(Block) - 컴포넌트나 설정
component_type "label" {
  attribute_name = value
  
  nested_block {
    nested_attribute = value
  }
}

// 단일 행 주석
attribute = value
```

---

## 블록(Blocks)

### 컴포넌트 블록

```alloy
prometheus.scrape "default" {
  targets    = [{__address__ = "localhost:8080"}]
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

- `prometheus.scrape`: 컴포넌트 종류
- `"default"`: 사용자 정의 라벨 (같은 컴포넌트 종류 내에서 고유)

### 설정 블록 (라벨 없음)

```alloy
logging {
  level = "info"
}

tracing {
  sampling_fraction = 0.1
}

argument "endpoint" {
  optional = false
}

export "receiver" {
  value = loki.write.default.receiver
}
```

### 중첩 블록

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    
    basic_auth {
      username = "user"
      password = sys.env("MIMIR_PASSWORD")
    }
    
    queue_config {
      capacity        = 10000
      max_shards      = 30
    }
  }
  
  external_labels = {
    cluster = "prod",
  }
}
```

---

## 속성(Attributes)

### 단순 속성

```alloy
url      = "http://example.com"
timeout  = "30s"
retries  = 5
enabled  = true
labels   = ["a", "b", "c"]
```

### 복잡한 값

```alloy
endpoint = {
  url = "http://example.com"
  headers = {
    "X-Custom" = "value",
  }
}

targets = [
  {__address__ = "host1:9090", job = "node"},
  {__address__ = "host2:9090", job = "node"},
]
```

### 비밀(Secret) 타입

비밀 값은 `secret` 타입으로 자동 처리되어 로그/UI에 마스킹됩니다.

```alloy
basic_auth {
  username = "user"
  password = sys.env("PASSWORD")  // 자동 secret
}
```

### 라벨 객체

```alloy
external_labels = {
  cluster = "prod",
  region  = "us-east-1",
}
```

---

## 표현식(Expressions)

### 리터럴

```alloy
"hello"           // String
42                // Number
3.14              // Float
true              // Bool
"30s"             // Duration (string)
[1, 2, 3]         // List
{a = 1, b = 2}    // Object
```

### 컴포넌트 출력 참조

```alloy
forward_to = [prometheus.remote_write.mimir.receiver]
targets    = discovery.kubernetes.pods.targets
```

### 산술 연산

```alloy
total = (a + b) * 2
half  = total / 2
```

### 비교 / 논리

```alloy
condition = (count > 100) && (status == "ok")
```

### 문자열 연산

```alloy
url      = "http://" + sys.env("HOST") + ":" + sys.env("PORT")
greeting = format("Hello, %s!", name)
```

### 표준 라이브러리 함수

```alloy
host    = sys.env("HOSTNAME")
content = file.contents("/etc/secret.txt")
data    = json.decode(file.contents("/etc/data.json"))
ts      = constants.timestamp_unix()
```

### 조건부

```alloy
endpoint = sys.env("ENV") == "prod" ? "https://prod.api" : "https://dev.api"
```

### 리스트/객체 컴프리헨션 (간접적)

```alloy
all_targets = concat(
  discovery.kubernetes.pods.targets,
  discovery.kubernetes.services.targets,
)
```

---

## 데이터 타입

| 타입 | 예시 | 설명 |
|------|------|------|
| `number` | `42`, `3.14` | 숫자 |
| `string` | `"hello"` | 문자열 |
| `bool` | `true`, `false` | 불리언 |
| `duration` | `"30s"`, `"1h"` | 기간 (문자열로 표현) |
| `list` | `[1, 2, 3]` | 목록 |
| `map` | `{a = 1}` | 키-값 |
| `secret` | (자동) | 비밀 값 |
| `capsule` | (특수) | 컴포넌트 간 전달 객체 (receiver 등) |
| `null` | `null` | 빈 값 |

### Capsule 타입

컴포넌트 간 전달되는 특수 객체. 예: `receiver`, `targets`.

```alloy
loki.process "extract" {
  forward_to = [loki.write.default.receiver]  // capsule 전달
}
```

---

## 참조(Reference)와 그래프

### 컴포넌트 참조

```
<component_type>.<label>.<exported_field>
```

예:
- `prometheus.exporter.unix.default.targets`
- `loki.write.default.receiver`
- `discovery.kubernetes.pods.targets`

### 의존성 그래프

Alloy는 참조를 분석해 **방향성 그래프(DAG)** 생성. 그래프 순서대로 평가/실행.

순환 참조는 에러.

### 평가 순서

1. 인자(arguments) 변경 감지
2. 의존하는 컴포넌트 재평가
3. 변경된 출력은 다운스트림으로 전파

---

## 표준 라이브러리

### `sys`

| 함수 | 설명 |
|------|------|
| `sys.env(name)` | 환경 변수 읽기 |

### `file`

| 함수 | 설명 |
|------|------|
| `file.contents(path)` | 파일 내용 읽기 (파일 변경 시 자동 리로드) |

### `string`

| 함수 | 설명 |
|------|------|
| `string.format(fmt, args...)` | 포맷팅 |
| `string.join(list, sep)` | 결합 |
| `string.split(s, sep)` | 분할 |
| `string.to_lower(s)` / `to_upper(s)` | 대소문자 |
| `string.trim(s, cutset)` | 트림 |
| `string.replace(s, old, new)` | 치환 |

### `array`

| 함수 | 설명 |
|------|------|
| `array.concat(...lists)` | 리스트 결합 |
| `array.combine_maps(maps...)` | 맵 결합 |

### `encoding`

| 함수 | 설명 |
|------|------|
| `encoding.from_json(s)` | JSON 디코드 |
| `encoding.from_yaml(s)` | YAML 디코드 |
| `encoding.from_base64(s)` | Base64 디코드 |
| `encoding.to_json(v)` | JSON 인코드 |

### `constants`

| 상수 | 설명 |
|------|------|
| `constants.hostname` | 호스트 이름 |
| `constants.os` | OS 이름 |
| `constants.arch` | 아키텍처 |

### 예시

```alloy
loki.write "default" {
  endpoint {
    url = string.format("http://%s/loki/api/v1/push", sys.env("LOKI_HOST"))
  }
  
  external_labels = {
    cluster  = sys.env("CLUSTER"),
    hostname = constants.hostname,
  }
}
```

---

## 모듈(Modules)

재사용 가능한 구성 단위.

### 모듈 정의

`modules/log_pipeline.alloy`:

```alloy
argument "endpoint" {
  optional = false
}

argument "tenant_id" {
  optional = true
  default  = "default"
}

loki.process "main" {
  stage.json {
    expressions = {
      level = "level",
      msg   = "message",
    }
  }
  
  stage.labels {
    values = {
      level = "",
    }
  }
  
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = argument.endpoint.value
    headers = {
      "X-Scope-OrgID" = argument.tenant_id.value,
    }
  }
}

export "receiver" {
  value = loki.process.main.receiver
}
```

### 모듈 사용

```alloy
import.file "log_pipeline" {
  filename = "modules/log_pipeline.alloy"
}

log_pipeline.main "default" {
  endpoint  = "http://loki:3100/loki/api/v1/push"
  tenant_id = "tenant-1"
}

loki.source.file "app_logs" {
  targets    = [{__path__ = "/var/log/app.log"}]
  forward_to = [log_pipeline.main.default.receiver]
}
```

### 임포트 방식

```alloy
// 로컬 파일
import.file "name" { filename = "path.alloy" }

// HTTP
import.http "name" { url = "https://example.com/module.alloy" }

// Git
import.git "name" {
  repository = "https://github.com/org/repo.git"
  path       = "modules/log.alloy"
  revision   = "main"
}

// 문자열
import.string "name" {
  content = "..."
}
```

---

## 디스커버리와 라벨링

### Discovery 컴포넌트

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.dns "service" {
  names = ["service.example.com"]
  type  = "A"
  port  = 80
}

discovery.consul "default" {
  server = "consul:8500"
}

discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}
```

### Relabeling

```alloy
discovery.relabel "filter_pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    regex         = "kube-system"
    action        = "drop"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_name"]
    target_label  = "pod"
  }
}

prometheus.scrape "kubernetes" {
  targets    = discovery.relabel.filter_pods.output
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

### Relabel Actions

| Action | 설명 |
|--------|------|
| `replace` | 라벨 값 치환 |
| `keep` | 매칭되는 것만 유지 |
| `drop` | 매칭되는 것 제거 |
| `keepequal` / `dropequal` | 두 라벨 비교 |
| `hashmod` | 해시 기반 샤딩 |
| `labelmap` | 라벨 이름 패턴 매핑 |
| `labeldrop` | 라벨 제거 |
| `labelkeep` | 라벨 유지 |
| `lowercase` / `uppercase` | 대소문자 변환 |

---

## 구성 예시 모음

### 1. Linux 노드 메트릭 → Mimir

```alloy
prometheus.exporter.unix "node" { }

prometheus.scrape "node" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "15s"
}

prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")
    basic_auth {
      username = sys.env("MIMIR_USER")
      password = sys.env("MIMIR_PASSWORD")
    }
  }
  
  external_labels = {
    cluster  = sys.env("CLUSTER"),
    hostname = constants.hostname,
  }
}
```

### 2. systemd journal → Loki

```alloy
loki.source.journal "journal" {
  forward_to    = [loki.write.default.receiver]
  relabel_rules = loki.relabel.journal.rules
  labels = {
    job = "systemd-journal",
  }
}

loki.relabel "journal" {
  forward_to = []
  
  rule {
    source_labels = ["__journal__systemd_unit"]
    target_label  = "unit"
  }
  
  rule {
    source_labels = ["__journal__hostname"]
    target_label  = "hostname"
  }
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

### 3. OTel 트레이스 → Tempo

```alloy
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
  }
  http {
    endpoint = "0.0.0.0:4318"
  }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}

otelcol.processor.batch "default" {
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
  }
}
```

### 4. Kubernetes Pod 로그 (Alloy DaemonSet)

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
  
  rule {
    source_labels = ["__meta_kubernetes_pod_label_app"]
    target_label  = "app"
  }
}

loki.source.kubernetes "pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki-gateway/loki/api/v1/push"
  }
}
```
