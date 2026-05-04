# Alloy 표준 라이브러리

> 이 문서는 Grafana Alloy 공식 문서의 Standard Library 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/reference/stdlib/

---

## 목차

1. [개요](#개요)
2. [`sys`](#sys)
3. [`file`](#file)
4. [`string`](#string)
5. [`array`](#array)
6. [`encoding`](#encoding)
7. [`convert`](#convert)
8. [`coalesce`](#coalesce)
9. [`concat`](#concat)
10. [`json_path`](#json_path)
11. [`format`](#format)
12. [`constants`](#constants)
13. [기타 함수](#기타-함수)

---

## 개요

Alloy 표준 라이브러리는 구성 파일에서 표현식 작성에 사용하는 내장 함수/상수.

### 사용 위치

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")          // 표준 라이브러리 사용
    
    basic_auth {
      password = file.contents("/etc/secret")
    }
  }
  
  external_labels = {
    cluster  = sys.env("CLUSTER"),
    hostname = constants.hostname,
  }
}
```

---

## `sys`

시스템 관련 함수.

### `sys.env(name)`

환경 변수 읽기.

```alloy
url      = sys.env("MIMIR_URL")
password = sys.env("API_KEY")
```

존재하지 않으면 빈 문자열 반환.

기본값:
```alloy
url = coalesce(sys.env("MIMIR_URL"), "http://localhost:9009")
```

---

## `file`

파일 관련 함수.

### `file.contents(path)`

파일 내용을 문자열로 읽기.

```alloy
ca_cert  = file.contents("/etc/ssl/ca.crt")
password = file.contents("/run/secrets/mimir-pass")
```

#### 자동 리로드

`file.contents()` 결과는 파일 변경 시 자동으로 갱신되어, 의존하는 컴포넌트가 재평가됨. 시크릿 로테이션에 유용.

#### 사용 예 (TLS)

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    
    tls_config {
      ca_pem   = file.contents("/etc/certs/ca.crt")
      cert_pem = file.contents("/etc/certs/client.crt")
      key_pem  = file.contents("/etc/certs/client.key")
    }
  }
}
```

---

## `string`

문자열 처리.

### `string.format(fmt, args...)`

C 스타일 포맷팅.

```alloy
url = string.format("http://%s:%d/loki/api/v1/push", host, port)
```

### `string.join(list, separator)`

리스트를 문자열로 결합.

```alloy
labels = string.join(["env", "region", "cluster"], ",")
// → "env,region,cluster"
```

### `string.split(s, separator)`

문자열을 리스트로 분할.

```alloy
parts = string.split("a,b,c", ",")
// → ["a", "b", "c"]
```

### `string.to_lower(s)` / `string.to_upper(s)`

대소문자 변환.

```alloy
env = string.to_lower(sys.env("ENVIRONMENT"))   // "PROD" → "prod"
```

### `string.trim(s, cutset)`

양쪽 끝 문자 제거.

```alloy
clean = string.trim("  hello  ", " ")   // → "hello"
```

### `string.trim_prefix(s, prefix)` / `string.trim_suffix(s, suffix)`

접두/접미사 제거.

```alloy
host = string.trim_prefix("http://example.com", "http://")
```

### `string.replace(s, old, new)`

치환.

```alloy
fixed = string.replace("hello world", "world", "alloy")
```

---

## `array`

배열 관련.

### `array.concat(lists...)`

여러 리스트 결합.

```alloy
all_targets = array.concat(
  discovery.kubernetes.pods.targets,
  discovery.docker.containers.targets,
  static_targets,
)
```

### `array.combine_maps(maps...)`

여러 맵 결합 (키 충돌 시 나중 것 우선).

```alloy
labels = array.combine_maps(
  {env = "prod"},
  {cluster = sys.env("CLUSTER")},
  {hostname = constants.hostname},
)
```

---

## `encoding`

인코딩/디코딩.

### `encoding.from_json(s)`

JSON 문자열을 객체로.

```alloy
config = encoding.from_json(file.contents("/etc/config.json"))
url    = config.endpoint
```

### `encoding.from_yaml(s)`

YAML 문자열을 객체로.

```alloy
config = encoding.from_yaml(file.contents("/etc/config.yaml"))
```

### `encoding.from_base64(s)`

Base64 디코드.

```alloy
secret = encoding.from_base64(sys.env("ENCODED_SECRET"))
```

### `encoding.to_json(v)`

객체를 JSON 문자열로.

```alloy
json = encoding.to_json({a = 1, b = "hello"})
// → "{\"a\":1,\"b\":\"hello\"}"
```

---

## `convert`

타입 변환.

### `convert.nonsensitive(secret)`

`secret` 타입을 일반 문자열로 (UI/로그에 표시될 수 있음, 신중히).

```alloy
// 일반적으로 권장하지 않음
visible_pass = convert.nonsensitive(sys.env("PASSWORD"))
```

활용:
- 디버깅용
- 일부 컴포넌트가 secret 타입을 받지 않을 때

---

## `coalesce`

여러 값 중 비어있지 않은 첫 번째 반환.

```alloy
url = coalesce(
  sys.env("MIMIR_URL"),
  sys.env("DEFAULT_URL"),
  "http://localhost:9009",
)
```

null, 빈 문자열, 빈 리스트/맵을 빈 값으로 간주.

---

## `concat`

리스트 결합 (alias로 `array.concat`).

```alloy
all = concat(list1, list2, list3)
```

---

## `json_path`

JSONPath로 값 추출.

```alloy
data = encoding.from_json(file.contents("/etc/config.json"))

// 특정 경로 추출
endpoint = json_path(data, "$.servers[0].endpoint")
all_urls = json_path(data, "$.servers[*].url")
```

JSONPath 문법:
- `$`: 루트
- `.field`: 필드
- `[*]`: 모든 요소
- `[0]`: 인덱스
- `..field`: 재귀 검색

---

## `format`

문자열 포맷팅 (alias).

```alloy
msg = format("server %s on port %d", host, port)
```

`string.format`과 동일.

---

## `constants`

내장 상수.

| 상수 | 설명 |
|------|------|
| `constants.hostname` | 호스트 이름 |
| `constants.os` | OS 이름 (`linux`, `darwin`, `windows`) |
| `constants.arch` | 아키텍처 (`amd64`, `arm64`) |

### 사용

```alloy
external_labels = {
  hostname = constants.hostname,
  os       = constants.os,
  arch     = constants.arch,
}
```

---

## 기타 함수

### `to_lower(s)` / `to_upper(s)`

전역 alias (`string.to_lower` / `string.to_upper`).

### 산술 연산자

```alloy
total  = a + b
diff   = a - b
prod   = a * b
quot   = a / b
mod    = a % b
power  = a ^ b
```

### 비교 연산자

```alloy
condition = (count > 100) && (status == "ok")
```

### 논리 연산자

```alloy
all = a && b
any = a || b
not = !a
```

### 조건부 (Ternary)

```alloy
url = sys.env("ENV") == "prod" ? "https://prod.api" : "https://dev.api"
```

### 인덱싱

```alloy
list  = ["a", "b", "c"]
first = list[0]   // "a"

map = {key = "value"}
val = map.key     // "value"
val = map["key"]  // 동등
```

---

## 실전 예시

### 1. 환경별 설정

```alloy
env = sys.env("ENVIRONMENT")

prometheus.remote_write "mimir" {
  endpoint {
    url = env == "prod" 
      ? "https://prod-mimir/api/v1/push" 
      : "https://staging-mimir/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = string.format("tenant-%s", env),
    }
  }
}
```

### 2. JSON 구성에서 동적 타겟

```alloy
targets_data = encoding.from_json(file.contents("/etc/targets.json"))

prometheus.scrape "dynamic" {
  targets = targets_data.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

`/etc/targets.json`:
```json
{
  "targets": [
    {"__address__": "host1:9090", "job": "node"},
    {"__address__": "host2:9090", "job": "node"}
  ]
}
```

### 3. Secret 파일 사용

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    
    basic_auth {
      username = "alloy"
      password = file.contents("/run/secrets/mimir-pass")
    }
    
    tls_config {
      ca_pem   = file.contents("/etc/certs/ca.crt")
      cert_pem = file.contents("/etc/certs/client.crt")
      key_pem  = file.contents("/etc/certs/client.key")
    }
  }
}
```

비밀 변경 시 자동 리로드.

### 4. 라벨 동적 생성

```alloy
external_labels = array.combine_maps(
  {
    cluster  = sys.env("CLUSTER"),
    region   = sys.env("REGION"),
    hostname = constants.hostname,
  },
  encoding.from_json(file.contents("/etc/extra-labels.json")),
)
```

### 5. 조건부 컴포넌트

```alloy
// env가 prod면 PII 마스킹 활성화
declare "log_pipeline" {
  argument "is_prod" { default = false }
  
  loki.process "pipeline" {
    forward_to = [loki.write.default.receiver]
    
    // is_prod일 때만 마스킹 단계 추가는 현재 직접 지원되지 않으므로
    // 다른 방식 사용
  }
}
```

또는 `import.file`을 환경별로:

```alloy
env = sys.env("ENVIRONMENT")

import.file "pipeline" {
  filename = string.format("modules/%s_pipeline.alloy", env)
}
```

### 6. URL 빌더

```alloy
host  = sys.env("BACKEND_HOST")
port  = sys.env("BACKEND_PORT")
path  = "/api/v1/push"
proto = sys.env("USE_TLS") == "true" ? "https" : "http"

backend_url = string.format("%s://%s:%s%s", proto, host, port, path)
```

### 7. JSONPath로 복잡한 추출

```alloy
config = encoding.from_yaml(file.contents("/etc/services.yaml"))

api_endpoints = json_path(config, "$.services[?(@.type=='api')].endpoint")

prometheus.scrape "api_services" {
  targets    = [for ep in api_endpoints : {__address__ = ep}]   // 가상 예시
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```
