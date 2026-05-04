# 시각화 및 Grafana 통합

> 이 문서는 Pyroscope 데이터를 Grafana 에서 시각화하는 방법을 다룹니다: 데이터 소스, Explore Profiles 앱, 트레이스/로그/메트릭과의 상관관계.
> 원본: https://grafana.com/docs/grafana/latest/datasources/grafana-pyroscope/

---

## 목차

1. [Pyroscope 데이터 소스](#pyroscope-데이터-소스)
2. [Explore 뷰](#explore-뷰)
3. [Explore Profiles 앱](#explore-profiles-앱)
4. [대시보드 패널](#대시보드-패널)
5. [Trace ↔ Profile 연동 (Span Profiles)](#trace--profile-연동-span-profiles)
6. [Logs ↔ Profile 연동](#logs--profile-연동)
7. [Metrics ↔ Profile 연동 (Exemplars)](#metrics--profile-연동-exemplars)

---

## Pyroscope 데이터 소스

### 추가 방법

Grafana → Connections → Data sources → "Grafana Pyroscope".

### 주요 설정

| 항목 | 값 |
|------|----|
| **URL** | `http://pyroscope:4040` (자체 호스팅) 또는 Grafana Cloud Profiles URL |
| **Auth** | Basic Auth (Cloud는 user=stack id, password=토큰) |
| **Custom HTTP Headers** | `X-Scope-OrgID: team-a` (멀티테넌트 시) |
| **Minimal step** | 시계열의 최소 step (예: `15s`) |

### 멀티 테넌트 라우팅

테넌트마다 다른 데이터 소스를 등록하거나, 한 데이터 소스에 변수 기반 헤더를 사용할 수 있습니다.

### Provisioning

```yaml
apiVersion: 1
datasources:
  - name: Pyroscope
    type: grafana-pyroscope-datasource
    access: proxy
    url: http://pyroscope:4040
    jsonData:
      keepCookies: []
    secureJsonData:
      basicAuthPassword: ${PYROSCOPE_TOKEN}
```

---

## Explore 뷰

Grafana Explore에서 Pyroscope 데이터 소스를 선택하면 다음을 사용할 수 있습니다.

### 쿼리 빌더

- **Service**: 라벨 `service_name` 자동 완성
- **Profile Type**: `process_cpu`, `memory`, `goroutine` 등
- **Label filter**: 추가 라벨 매처

### Display modes

| 모드 | 설명 |
|------|------|
| **Profile** | 단일 시점 / 시간 범위의 머지된 flame graph |
| **Time series** | 시간에 따른 합계 시계열 (선택 라벨로 그룹) |
| **Both** | 위 두 가지 동시 |

### Comparison Mode (Diff)

두 시간/라벨 셋을 비교하는 Diff flame graph.

---

## Explore Profiles 앱

`grafana-pyroscope-app` 플러그인은 Explore 보다 풍부한 분석 UI를 제공합니다.

### 설치

```bash
grafana-cli plugins install grafana-pyroscope-app
```

또는 Helm `GF_INSTALL_PLUGINS` 환경 변수.

### 핵심 기능

- **Service overview**: 서비스 목록, 각 서비스의 CPU/메모리 상위 N 함수
- **Function detail**: 단일 함수의 호출자/피호출자, 시간 추세
- **Flame graph 분석**: Sandwich, Diff 뷰
- **Comparison**: 시간 비교, 라벨 비교
- **Favorites / Bookmarks**: 분석 컨텍스트 저장

### 데이터 소스 선택

Explore Profiles는 등록된 Pyroscope 데이터 소스 중 하나를 사용. 멀티 테넌트라면 상단에서 데이터 소스 변경.

---

## 대시보드 패널

### Flame Graph 패널

- 패널 타입: **Flame Graph**
- 쿼리에서 라벨 셀렉터 + 프로파일 타입 지정
- Display mode: Flame, Table, Both

### Time Series 패널

CPU 시간 합계, 메모리 inuse_space 등을 시계열로 표시.

```
sum by (service_name) (
  rate(process_cpu:cpu:nanoseconds:cpu:nanoseconds[$__rate_interval])
)
```

(Pyroscope 데이터 소스의 시계열 쿼리 형식)

### 변수 사용

```
service_name = label_values(service_name)
env          = label_values(env)
```

대시보드 변수로 다양한 서비스/환경 전환.

---

## Trace ↔ Profile 연동 (Span Profiles)

트레이스의 특정 스팬에서 그 시간/인스턴스의 프로파일로 점프하는 기능. **Span Profiles** 라고 부릅니다.

### 동작 원리

1. 애플리케이션이 트레이스 컨텍스트의 traceID/spanID를 프로파일 라벨에 포함하여 Pyroscope에 전송
2. Tempo의 트레이스 뷰에서 스팬의 시간 범위 + 인스턴스 라벨로 Pyroscope 쿼리 자동 생성
3. 같은 화면에서 flame graph 미니뷰가 표시됨

### 활성화 (Go SDK 예시)

OTel + Pyroscope SDK 통합:

```go
import (
    "github.com/grafana/pyroscope-go/x/k6"   // 또는 OTel 통합 패키지
    pyroscope "github.com/grafana/pyroscope-go"
)

profiler, _ := pyroscope.Start(pyroscope.Config{
    ApplicationName: "checkout",
    ServerAddress:   "http://pyroscope:4040",
    // pprof 라벨에 trace context 추가
})
```

### Tempo 데이터 소스 설정

Tempo 데이터 소스에서 Pyroscope를 "Profiles" 탭의 연결 대상으로 추가하면, 스팬 디테일에서 자동으로 link 노출.

---

## Logs ↔ Profile 연동

Loki의 로그에서 Pyroscope 프로파일로 점프.

### 데이터 소스 derived field

Loki 데이터 소스 설정에서 derived field 추가:

```
Name: profile_link
Regex: trace_id=(\w+)
URL: <Pyroscope explore URL>
Datasource: Pyroscope
```

또는 단순히 `service_name` 라벨을 share 하여 Pyroscope에 자동 매칭.

### 단순 통합

Loki에서 로그 라벨 `service_name`, `env`, `cluster` 가 Pyroscope와 동일하다면 Explore에서 "Open in Pyroscope" 같은 버튼이 자동 활성화됩니다.

---

## Metrics ↔ Profile 연동 (Exemplars)

Mimir/Prometheus의 메트릭에서 이상치 발견 시 관련 프로파일로 이동.

### 동작 원리

- 메트릭의 exemplar에 프로파일 ID 또는 트레이스 ID를 포함
- Grafana 메트릭 패널에서 exemplar 점을 클릭하면 해당 프로파일로 점프

### 일반적인 패턴

1. Grafana 대시보드에서 CPU/메모리 메트릭 그래프 표시
2. 이상 구간 발견 → 같은 시간 + 같은 라벨로 Pyroscope 쿼리
3. **Data link** 으로 자동화:

```
Title: View profile
URL: /a/grafana-pyroscope-app/.../service/${__field.labels.service_name}?from=${__from}&to=${__to}
```

---

## 통합 워크플로 예

### 사례: 응답 지연 회귀 분석

1. **Mimir 대시보드**: P99 latency 그래프에서 회귀 발견 (15:23 부터 증가)
2. **Tempo Explore**: 그 시간대의 느린 트레이스 검색 (`duration > 1s`)
3. **Span 디테일**: 가장 긴 스팬의 "View profile" 클릭
4. **Pyroscope flame graph**: 해당 스팬 시간대의 CPU 프로파일 분석
5. **Diff**: 어제 같은 시간대와 비교 → 회귀 함수 식별
6. **Loki**: 그 함수의 로그 패턴 확인 → 가설 검증

이 모든 과정이 같은 Grafana UI 안에서 1~2분 내에 가능.

---

## 라벨 통일 컨벤션

여러 신호 간 자연스러운 점프를 위한 권장 라벨:

| 라벨 | 예시 값 | 용도 |
|------|--------|------|
| `service_name` | `checkout` | 서비스 식별 (필수) |
| `service_namespace` | `payments` | 서비스 그룹 |
| `env` | `prod`, `staging`, `dev` | 환경 |
| `cluster` | `us-east-1` | 클러스터 |
| `version` | `v1.2.3` | 배포 버전 (Diff에 핵심) |
| `instance` | `host123` | 인스턴스 (고카디널리티 주의) |

OTel Resource Attribute 표준에 맞추는 것이 권장됩니다 (`service.name` 등 → 라벨 변환 시 `_` 사용).

---

## 다음 단계

- [05_flamegraphs.md](./05_flamegraphs.md) - flame graph 분석법
- [08_use_cases.md](./08_use_cases.md) - 통합 트러블슈팅 사례
- [06_instrumentation.md](./06_instrumentation.md) - 라벨 설정
