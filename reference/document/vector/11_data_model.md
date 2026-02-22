# 데이터 모델 (Data Model)

> 이 문서는 [Vector 공식 문서](https://vector.dev/docs/)의 Architecture / Data Model 섹션을 한국어로 번역한 것입니다.

- 원문: https://vector.dev/docs/architecture/data-model/

---

## 목차

1. [개요](#개요)
2. [이벤트 타입](#이벤트-타입)
3. [계층화된 접근 방식의 이유](#계층화된-접근-방식의-이유)
4. [로그 이벤트 (Log Events)](#로그-이벤트-log-events)
   - [스키마](#스키마)
   - [지원되는 데이터 타입](#지원되는-데이터-타입)
5. [메트릭 이벤트 (Metric Events)](#메트릭-이벤트-metric-events)
   - [메트릭 스키마](#메트릭-스키마)
   - [메트릭 타입](#메트릭-타입)

---

## 개요

Vector의 데이터 모델은 시스템을 통해 흐르는 기본 단위인 이벤트(Events) 를 중심으로 구성됩니다. Vector를 통과하는 개별 데이터 단위를 이벤트라고 하며, 이벤트는 반드시 Vector가 정의한 옵저버빌리티 타입 중 하나에 해당해야 합니다.

---

## 이벤트 타입

Vector는 두 가지 주요 이벤트 카테고리를 정의합니다:

| 이벤트 타입 | 설명 |
|------------|------|
| Log events (로그 이벤트) | 특정 시점에 발생한 사건을 구조화된 데이터로 표현합니다. |
| Metric events (메트릭 이벤트) | 시스템의 상태나 성능을 나타내는 수치 데이터입니다. |

---

## 계층화된 접근 방식의 이유

Vector 팀은 여러 이벤트 타입을 지원하는 설계 철학에 대해 다음과 같이 설명합니다.

풍부한 컨텍스트 데이터를 가진 "이벤트 전용 세계(event-only world)"가 이상적이지만, 현실 세계의 서비스들은 다양한 품질의 로그와 메트릭을 생성합니다. Vector는 이러한 기존 표준들을 수용하도록 설계되어 레거시 시스템과 새로운 옵저버빌리티 접근 방식 사이의 다리 역할을 합니다.

이러한 정교한 데이터 모델은 중요한 상호운용성을 가능하게 합니다. 예를 들어, 적절한 내부 메트릭 데이터 구조 없이는 StatsD 소스와 Prometheus 싱크를 결합하는 파이프라인을 구성하는 것이 불가능합니다. 이 신중한 분류 체계를 통해 Vector는 서로 다른 옵저버빌리티 시스템 간에 효과적으로 변환할 수 있습니다.

---

## 로그 이벤트 (Log Events)

Vector의 로그 이벤트는 특정 시점에 발생한 사건을 구조화된 데이터로 표현합니다. 핵심 원칙은 스키마 중립성(schema neutrality) 으로, Vector는 특정 필드를 강제하지 않아 다양하고 진화하는 데이터 스키마와의 호환성을 보장합니다.

### 스키마

Vector는 스키마 중립적이며 특정 스키마를 요구하지 않습니다. 이러한 유연성 덕분에 수정 없이도 레거시 및 미래의 데이터 형식을 모두 지원할 수 있습니다.

다음은 JSON 형식의 로그 이벤트 예시입니다:

```json
{
  "log": {
    "custom": "field",
    "host": "my.host.com",
    "message": "Hello world",
    "timestamp": "2020-11-01T21:15:47+00:00"
  }
}
```

### 지원되는 데이터 타입

Vector는 다양한 데이터 타입을 지원합니다:

| 타입 | 설명 |
|------|------|
| Strings (문자열) | UTF-8로 인코딩되며, 시스템 메모리에 의해서만 크기가 제한됩니다. |
| Integers (정수) | 부호 있는 64비트 정수 값입니다. |
| Floats (부동소수점) | IEEE 754 64비트 표준을 따릅니다. |
| Booleans (불리언) | 이진 true/false 값입니다. |
| Timestamps (타임스탬프) | Rust `DateTime` 구조체를 사용하여 UTC로 저장됩니다. 타임존 정보가 없는 경우, Vector는 로컬 시간으로 가정하고 자동으로 UTC로 변환합니다. |
| Null 값 | JSON 호환성을 위해 지원됩니다. |
| Maps (맵) | 모든 타입의 값을 지원하는 연관 배열입니다. |
| Arrays (배열) | 모든 타입의 값을 지원하는 시퀀스입니다. |

### 타임스탬프 처리

JSON과 같이 공식적인 타임스탬프 정의가 없는 형식의 경우, Vector는 처음에 타임스탬프를 원시 형태로 수집한 다음 `remap` 변환과 VRL의 `parse_timestamp` 함수를 사용하여 타입 변환을 수행합니다.

```yaml
transforms:
  parse_timestamp:
    type: remap
    inputs:
      - source_id
    source: |
      .timestamp = parse_timestamp!(.timestamp, format: "%Y-%m-%d %H:%M:%S")
```

---

## 메트릭 이벤트 (Metric Events)

Vector는 메트릭을 구조화된 로그가 아닌 일급 시민(first-class citizen)으로 취급합니다. Vector의 메트릭 데이터 모델은 Prometheus, StatsD 등 다양한 시스템의 메트릭 타입을 결합하여 시스템 간 "정확한 상호운용성(correctly interoperable)"을 보장합니다.

### 메트릭 스키마

모든 메트릭 이벤트는 다음 필드를 포함합니다:

#### 필수 필드

| 필드 | 설명 |
|------|------|
| name | 메트릭 식별자 (이름)입니다. |
| kind | 메트릭 값의 종류입니다. `incremental` 또는 `absolute` 중 하나입니다. |
| tags | 태그 이름과 값(문자열 또는 null)의 매핑입니다. |
| timestamp | 메트릭이 생성된 시간입니다. |

#### 선택적 필드

| 필드 | 설명 |
|------|------|
| namespace | 메트릭 이름 앞에 붙거나 네이티브 네임스페이싱을 사용합니다. |
| interval_ms | 메트릭 값이 나타내는 시간 간격(밀리초)입니다. |

다음은 메트릭 이벤트의 예시입니다:

```json
{
  "metric": {
    "name": "requests_total",
    "namespace": "myapp",
    "kind": "incremental",
    "tags": {
      "host": "my.host.com",
      "method": "GET",
      "status": "200"
    },
    "timestamp": "2020-11-01T21:15:47+00:00",
    "counter": {
      "value": 1.0
    }
  }
}
```

### 메트릭 타입

Vector는 다음과 같은 메트릭 타입을 지원합니다:

#### Counter (카운터)

증가하거나 리셋될 수 있지만 감소할 수는 없는 값입니다. 증분을 위해 양수 float 값이 필요합니다.

```json
{
  "metric": {
    "name": "requests_total",
    "kind": "incremental",
    "counter": {
      "value": 1.0
    }
  }
}
```

사용 사례: 요청 수, 오류 수, 완료된 작업 수 등

#### Gauge (게이지)

위아래로 변동할 수 있는 특정 시점의 값을 나타냅니다. 메모리 사용량이나 CPU 사용량과 같은 메트릭을 추적하는 데 유용합니다.

```json
{
  "metric": {
    "name": "memory_usage_bytes",
    "kind": "absolute",
    "gauge": {
      "value": 1073741824.0
    }
  }
}
```

사용 사례: 메모리 사용량, CPU 사용률, 현재 연결 수, 온도 등

#### Histogram (히스토그램)

타이머라고도 불리며, 요청 지속 시간과 같은 관측값을 샘플링하여 구성 가능한 버킷으로 구성합니다. 카운트와 합계 총합을 모두 제공합니다.

```json
{
  "metric": {
    "name": "request_duration_seconds",
    "kind": "incremental",
    "histogram": {
      "buckets": [
        {"upper_limit": 0.005, "count": 0},
        {"upper_limit": 0.01, "count": 1},
        {"upper_limit": 0.025, "count": 0},
        {"upper_limit": 0.05, "count": 1},
        {"upper_limit": 0.1, "count": 0},
        {"upper_limit": 0.25, "count": 0},
        {"upper_limit": 0.5, "count": 0},
        {"upper_limit": 1.0, "count": 0},
        {"upper_limit": 2.5, "count": 0},
        {"upper_limit": 5.0, "count": 0},
        {"upper_limit": 10.0, "count": 0}
      ],
      "count": 2,
      "sum": 0.051
    }
  }
}
```

사용 사례: 요청 지연 시간, 응답 크기, 처리 시간 등

#### Distribution (분포)

글로벌 히스토그램 및 요약을 지원하는 서비스에 대한 샘플링된 값 분포를 나타냅니다. 샘플 값과 통계 타입(histogram 또는 summary)이 모두 필요합니다.

```json
{
  "metric": {
    "name": "request_duration_seconds",
    "kind": "incremental",
    "distribution": {
      "samples": [
        {"value": 0.05, "rate": 1}
      ],
      "statistic": "histogram"
    }
  }
}
```

사용 사례: 분산 시스템에서 집계되어야 하는 메트릭

#### Summary (요약)

히스토그램과 유사하지만 슬라이딩 시간 윈도우에 대해 구성 가능한 분위수(quantile)를 계산합니다. 관측 카운트, 합계, 분위수 값을 포함합니다.

```json
{
  "metric": {
    "name": "request_duration_seconds",
    "kind": "absolute",
    "summary": {
      "quantiles": [
        {"quantile": 0.5, "value": 0.05},
        {"quantile": 0.9, "value": 0.1},
        {"quantile": 0.99, "value": 0.2}
      ],
      "count": 100,
      "sum": 5.5
    }
  }
}
```

사용 사례: 백분위수 기반 SLO/SLA 모니터링

#### Set (집합)

고유한 값들의 배열입니다.

```json
{
  "metric": {
    "name": "unique_users",
    "kind": "incremental",
    "set": {
      "values": ["user_1", "user_2", "user_3"]
    }
  }
}
```

사용 사례: 고유 사용자 수, 고유 IP 주소 수 등

### Kind (종류)

메트릭의 `kind` 필드는 두 가지 값을 가질 수 있습니다:

| 종류 | 설명 |
|------|------|
| incremental | 이전 값에 대한 변화량을 나타냅니다. 이 값들은 시간에 따라 합산되어야 합니다. |
| absolute | 특정 시점의 절대값을 나타냅니다. 이 값들은 합산되지 않고 그대로 사용됩니다. |

---

## 이벤트 흐름

Vector에서 이벤트는 다음과 같은 흐름을 따릅니다:

```
┌─────────┐     ┌────────────┐     ┌─────────┐
│ Sources │ ──▶ │ Transforms │ ──▶ │  Sinks  │
└─────────┘     └────────────┘     └─────────┘
```

1. Sources (소스): 외부 시스템에서 이벤트를 수집하거나 수신합니다.
2. Transforms (변환): 이벤트를 파싱, 필터링, 집계 또는 변환합니다.
3. Sinks (싱크): 처리된 이벤트를 외부 시스템으로 전송합니다.

---

## 데이터 타입 간 변환

Vector의 `remap` 변환과 VRL을 사용하여 이벤트 타입 간 변환을 수행할 수 있습니다.

### 로그를 메트릭으로 변환

`log_to_metric` 변환을 사용하여 로그 이벤트에서 메트릭을 생성할 수 있습니다:

```yaml
transforms:
  log_to_metric:
    type: log_to_metric
    inputs:
      - source_id
    metrics:
      - type: counter
        field: message
        name: log_count
        tags:
          host: "{{host}}"
```

### 메트릭을 로그로 변환

`metric_to_log` 변환을 사용하여 메트릭 이벤트를 로그로 변환할 수 있습니다:

```yaml
transforms:
  metric_to_log:
    type: metric_to_log
    inputs:
      - metrics_source
```

---

## 참고 자료

- [Vector 공식 문서 - Data Model](https://vector.dev/docs/architecture/data-model/)
- [Vector 공식 문서 - Log Events](https://vector.dev/docs/architecture/data-model/log/)
- [Vector 공식 문서 - Metric Events](https://vector.dev/docs/architecture/data-model/metric/)
- [Vector Remap Language (VRL)](https://vector.dev/docs/reference/vrl/)
