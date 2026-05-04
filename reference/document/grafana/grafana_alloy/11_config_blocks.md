# Alloy 구성 블록 레퍼런스

> 이 문서는 Grafana Alloy 공식 문서의 Configuration Blocks 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/reference/config-blocks/

---

## 목차

1. [개요](#개요)
2. [`logging`](#logging)
3. [`tracing`](#tracing)
4. [`livedebugging`](#livedebugging)
5. [`argument`](#argument)
6. [`export`](#export)
7. [`declare`](#declare)
8. [`import.file`](#importfile)
9. [`import.git`](#importgit)
10. [`import.http`](#importhttp)
11. [`import.string`](#importstring)
12. [`remotecfg`](#remotecfg)
13. [`http`](#http)

---

## 개요

구성 블록(Configuration Blocks)은 컴포넌트가 아닌 Alloy 자체의 동작을 제어합니다.

### 컴포넌트 vs 구성 블록

| 구성 블록 | 컴포넌트 |
|---------|---------|
| 라벨 없음 | 라벨 있음 (`"name"`) |
| Alloy 자체 설정 | 데이터 처리 |
| 인스턴스 1개만 | 여러 개 가능 |

### 종류

| 블록 | 용도 |
|------|------|
| `logging` | 로그 레벨/형식/대상 |
| `tracing` | 자체 트레이싱 |
| `livedebugging` | 라이브 디버그 |
| `argument` | 모듈 입력 |
| `export` | 모듈 출력 |
| `declare` | 인라인 모듈 선언 |
| `import.*` | 외부 모듈 임포트 |
| `remotecfg` | 원격 구성 |
| `http` | HTTP 클라이언트 기본값 |

---

## `logging`

Alloy 자체 로그 설정.

```alloy
logging {
  level    = "info"           // debug, info, warn, error
  format   = "logfmt"         // logfmt, json
  
  // 로그를 다른 컴포넌트로 전달
  write_to = [loki.write.default.receiver]
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `level` | `info` | 로그 레벨 |
| `format` | `logfmt` | 형식 |
| `write_to` | `[]` | Loki receiver 목록 |

### 활용: 자체 로그를 Loki로

```alloy
logging {
  level    = "info"
  format   = "logfmt"
  write_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
  external_labels = {
    job = "alloy",
    instance = constants.hostname,
  }
}
```

---

## `tracing`

Alloy 자체 트레이스 (분산 트레이싱 데이터).

```alloy
tracing {
  sampling_fraction = 0.1     // 10% 샘플링
  
  write_to = [otelcol.exporter.otlp.tempo.input]
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls { insecure = true }
  }
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `sampling_fraction` | `0.1` | 샘플링 비율 (0.0-1.0) |
| `write_to` | `[]` | OTel exporter input |

### 활용

Alloy 내부 동작 디버그:
- 컴포넌트 평가 시간
- 데이터 전달 경로
- 외부 호출 추적

---

## `livedebugging`

라이브 디버깅 활성화 (실험적).

```alloy
livedebugging {
  enabled = true
}
```

UI에서 실시간으로 컴포넌트 입출력 확인 가능.

### 활용

- 파이프라인 디버깅
- 데이터 변환 확인
- 라벨링 검증

---

## `argument`

모듈 입력 인자 정의.

```alloy
argument "endpoint" {
  optional = false
  comment  = "Loki endpoint URL"
}

argument "tenant_id" {
  optional = true
  default  = "default"
  comment  = "Loki tenant ID"
}

argument "external_labels" {
  optional = true
  default  = {}
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `optional` | false | 선택적 인자 여부 |
| `default` | (없음) | 기본값 (optional=true 필요) |
| `comment` | "" | 설명 (UI에 표시) |

### 사용

```alloy
loki.write "default" {
  endpoint {
    url = argument.endpoint.value
    headers = {
      "X-Scope-OrgID" = argument.tenant_id.value,
    }
  }
  external_labels = argument.external_labels.value
}
```

---

## `export`

모듈 출력 정의.

```alloy
export "receiver" {
  value = loki.process.main.receiver
}

export "endpoint_url" {
  value = "http://internal-loki/api/v1/push"
}
```

### 옵션

| 인자 | 설명 |
|------|------|
| `value` | 출력할 값 |

### 사용

다른 구성에서:

```alloy
import.file "log_module" {
  filename = "modules/log.alloy"
}

log_module "default" {
  endpoint = "http://loki:3100/loki/api/v1/push"
}

// 모듈 출력 사용
loki.source.file "app" {
  targets    = [{__path__ = "/var/log/app.log"}]
  forward_to = [log_module.default.receiver]
}
```

---

## `declare`

인라인 모듈 선언 (별도 파일 없이).

```alloy
declare "log_pipeline" {
  argument "url" { }
  
  loki.process "internal" {
    forward_to = [loki.write.internal.receiver]
    
    stage.json {
      expressions = {
        level = "level",
      }
    }
  }
  
  loki.write "internal" {
    endpoint {
      url = argument.url.value
    }
  }
  
  export "receiver" {
    value = loki.process.internal.receiver
  }
}

// 사용
log_pipeline "default" {
  url = "http://loki:3100/loki/api/v1/push"
}

loki.source.file "app" {
  targets    = [{__path__ = "/var/log/app.log"}]
  forward_to = [log_pipeline.default.receiver]
}
```

---

## `import.file`

로컬 파일에서 모듈 임포트.

```alloy
import.file "modules" {
  filename = "modules/loki_pipeline.alloy"
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `filename` | (필수) | 파일 경로 |

### 디렉토리 임포트

```alloy
import.file "all_modules" {
  filename = "modules/"
  // 디렉토리 내 모든 .alloy 파일
}
```

### 사용

```alloy
import.file "loki" {
  filename = "modules/loki.alloy"
}

loki.pipeline "default" {
  endpoint = "http://loki:3100"
}
```

---

## `import.git`

Git 저장소에서 모듈 임포트.

```alloy
import.git "modules" {
  repository     = "https://github.com/grafana/alloy-modules.git"
  revision       = "main"           // branch, tag, commit
  path           = "modules/kubernetes"
  pull_frequency = "5m"
  
  basic_auth {
    username = "user"
    password = sys.env("GIT_TOKEN")
  }
  
  ssh_key {
    username = "git"
    key      = file.contents("/etc/ssh/id_rsa")
  }
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `repository` | (필수) | Git URL |
| `revision` | `HEAD` | branch/tag/commit |
| `path` | `""` | 저장소 내 경로 |
| `pull_frequency` | `0s` | 풀 주기 (0이면 시작 시만) |

### 사용

```alloy
import.git "modules" {
  repository = "https://github.com/grafana/alloy-modules.git"
  revision   = "main"
}

// 저장소 내 모듈 사용
modules.kubernetes.logs.pods "default" {
  forward_to = [loki.write.default.receiver]
}
```

---

## `import.http`

HTTP 엔드포인트에서 모듈 임포트.

```alloy
import.http "module" {
  url = "https://example.com/modules/loki.alloy"
  
  headers = {
    "Authorization" = "Bearer " + sys.env("API_TOKEN"),
  }
  
  poll_frequency = "1m"
  poll_timeout   = "10s"
  
  basic_auth {
    username = "user"
    password = sys.env("PASSWORD")
  }
}
```

### 옵션

| 인자 | 기본값 | 설명 |
|------|--------|------|
| `url` | (필수) | HTTP URL |
| `headers` | `{}` | 추가 헤더 |
| `poll_frequency` | `0s` | 폴링 주기 |
| `poll_timeout` | `30s` | 타임아웃 |

### 활용

- 중앙 집중식 모듈 관리 서버
- CDN을 통한 모듈 배포
- 환경별 다른 모듈

---

## `import.string`

인라인 문자열로 모듈 정의.

```alloy
import.string "inline" {
  content = `
    argument "name" {}
    
    export "greeting" {
      value = "Hello, " + argument.name.value
    }
  `
}

inline "world" {
  name = "World"
}

// inline.world.greeting → "Hello, World"
```

### 활용

- 동적으로 생성된 모듈
- 외부 시크릿 매니저에서 모듈 코드 가져오기

---

## `remotecfg`

원격 구성 서버에서 Alloy 구성 가져오기.

```alloy
remotecfg {
  url            = "https://config-server.example.com"
  id             = constants.hostname
  poll_frequency = "1m"
  poll_timeout   = "10s"
  
  basic_auth {
    username = "alloy"
    password = sys.env("CONFIG_SERVER_PASSWORD")
  }
  
  // 또는
  bearer_token = sys.env("CONFIG_TOKEN")
  
  metadata = {
    cluster = "prod",
    region  = "us-east-1",
  }
}
```

### 옵션

| 인자 | 설명 |
|------|------|
| `url` | 구성 서버 URL |
| `id` | Alloy 인스턴스 ID |
| `poll_frequency` | 폴링 주기 |
| `metadata` | 서버에 보낼 메타데이터 |

### 활용

- 중앙 집중식 구성 관리
- 동적 구성 변경
- Fleet 관리

---

## `http`

전역 HTTP 클라이언트 기본값.

```alloy
http {
  tls {
    cert_file = "/etc/certs/client.crt"
    key_file  = "/etc/certs/client.key"
    ca_file   = "/etc/certs/ca.crt"
  }
  
  // 모든 HTTP 요청에 적용 가능
  follow_redirects = true
  enable_http2     = true
  
  proxy_url           = sys.env("HTTP_PROXY")
  no_proxy            = sys.env("NO_PROXY")
  proxy_from_environment = true
}
```

이 블록은 모든 HTTP 요청 컴포넌트의 기본값으로 적용. 컴포넌트별 오버라이드 가능.

---

## 종합 예시

```alloy
// === 자체 로그 ===
logging {
  level    = "info"
  format   = "json"
  write_to = [loki.write.self.receiver]
}

// === 자체 트레이스 ===
tracing {
  sampling_fraction = 0.1
  write_to = [otelcol.exporter.otlp.tempo.input]
}

// === 외부 모듈 ===
import.git "modules" {
  repository     = "https://github.com/myorg/alloy-modules.git"
  revision       = "main"
  pull_frequency = "5m"
}

// === 인라인 모듈 ===
declare "metric_pipeline" {
  argument "url" { }
  argument "tenant" {
    optional = true
    default  = "default"
  }
  
  prometheus.remote_write "internal" {
    endpoint {
      url = argument.url.value
      headers = {
        "X-Scope-OrgID" = argument.tenant.value,
      }
    }
  }
  
  export "receiver" {
    value = prometheus.remote_write.internal.receiver
  }
}

// === 모듈 사용 ===
metric_pipeline "default" {
  url    = "http://mimir:9009/api/v1/push"
  tenant = "tenant-1"
}

modules.kubernetes.metrics.kubelet "default" {
  forward_to = [metric_pipeline.default.receiver]
}

// === 자체 모니터링 백엔드 ===
loki.write "self" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "alloy-self",
    }
  }
  external_labels = {
    job      = "alloy",
    instance = constants.hostname,
    cluster  = sys.env("CLUSTER"),
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls { insecure = true }
    headers = {
      "X-Scope-OrgID" = "alloy-self",
    }
  }
}
```
