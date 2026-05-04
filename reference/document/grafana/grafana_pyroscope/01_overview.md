# Grafana Pyroscope 개요

> 이 문서는 Grafana Pyroscope 공식 문서의 "Introduction" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/pyroscope/latest/

---

## 목차

1. [Pyroscope란 무엇인가](#pyroscope란-무엇인가)
2. [Continuous Profiling이란](#continuous-profiling이란)
3. [Pyroscope의 주요 기능](#pyroscope의-주요-기능)
4. [Pyroscope 프로파일링 스택](#pyroscope-프로파일링-스택)
5. [지원 프로파일 타입](#지원-프로파일-타입)
6. [Pyroscope와 다른 프로파일링 도구 비교](#pyroscope와-다른-프로파일링-도구-비교)

---

## Pyroscope란 무엇인가

**Grafana Pyroscope**는 오픈소스이며 대규모(massively scalable)로 동작하는 **연속 프로파일링(continuous profiling) 집계 시스템** 입니다. 2023년 Grafana Labs가 Pyroscope를 인수하면서 기존 Phlare 프로젝트와 통합되어 현재의 Pyroscope v1으로 통합되었습니다.

### 핵심 특징

- **오픈소스**: AGPL-3.0 라이선스
- **대규모 확장성**: Loki/Mimir/Tempo와 동일한 수평 확장 가능한 마이크로서비스 아키텍처
- **저비용**: 오브젝트 스토리지(S3, GCS, Azure Blob)에 프로파일 영속화
- **멀티 테넌시**: 단일 클러스터로 여러 조직/팀 운영 가능
- **고가용성**: 다중 복제와 일관성 있는 해시 링 기반 데이터 분배
- **다른 신호와의 통합**: 메트릭(Mimir), 로그(Loki), 트레이스(Tempo)와 연결

---

## Continuous Profiling이란

### 프로파일링이란?

**프로파일링(Profiling)**은 애플리케이션 실행 중 어느 부분에서 자원(CPU, 메모리, 락 등)이 소모되는지를 측정하는 기법입니다. 전통적으로 프로파일러는 개발 환경에서 잠깐 실행되어 단일 스냅샷을 생성하는 방식이었습니다.

### 연속 프로파일링의 등장

**Continuous Profiling**은 프로덕션 환경에서 **항상 켜둘 수 있을 만큼 가벼운 오버헤드**로 프로파일을 지속적으로 수집·저장·시각화하는 접근입니다. Google의 [Google-Wide Profiling 논문(2010)](https://research.google/pubs/google-wide-profiling-a-continuous-profiling-infrastructure-for-data-centers/)에서 그 가치가 처음 제시되었습니다.

### 왜 Continuous Profiling이 필요한가

- **운영 환경의 실제 동작 관찰**: 개발 환경에서는 재현되지 않는 성능 문제 분석
- **회귀(Regression) 자동 발견**: 배포 전후 프로파일 비교로 성능 저하 탐지
- **비용 최적화**: 클라우드 컴퓨팅 비용에서 코드 효율성이 직접적인 영향을 미침
- **메모리 누수와 OOM 진단**: 메모리 사용 패턴을 시간에 걸쳐 추적
- **디버깅의 마지막 단계**: 메트릭(무엇), 로그(왜), 트레이스(어디)에 더해 **"코드의 어느 라인이 비싼가"** 를 알려줌

### 관측가능성의 4번째 신호

전통적인 관측가능성(Observability)은 **메트릭, 로그, 트레이스** 의 3가지 신호로 이야기되었습니다. Continuous Profiling은 **4번째 신호** 로 자리잡고 있습니다.

| 신호 | 질문 | 도구 |
|------|------|------|
| 메트릭 | 무엇이 일어나고 있는가? | Mimir, Prometheus |
| 로그 | 왜 그런 일이 일어났는가? | Loki |
| 트레이스 | 어디서 시간이 소요되었는가? | Tempo |
| **프로파일** | **어느 코드가 비용을 만드는가?** | **Pyroscope** |

---

## Pyroscope의 주요 기능

### 효율적인 프로파일 저장

- 오브젝트 스토리지(S3, GCS, Azure Blob, MinIO)에 압축된 블록 형태로 영속화
- pprof 표준 형식 기반 + 자체 컬럼형 파일 포맷으로 빠른 쿼리 지원
- 공통 심볼 테이블 등 중복 제거를 통한 압축률

### 강력한 쿼리 모델

- **LabelSelector**: Prometheus PromQL 스타일의 라벨 매처 (`{service_name="frontend", env="prod"}`)
- **시간 범위 쿼리**: 특정 기간의 프로파일 집계
- **태그/레이블 그룹화**: 여러 차원으로 프로파일 분해
- **Diff 분석**: 두 시간 구간 또는 두 라벨 셋 간의 차이 시각화

### Flame Graph 기반 시각화

- 함수 호출 스택을 직관적으로 표현하는 플레임 그래프
- 비교(Diff), 샌드위치(Sandwich) 뷰로 정밀 분석 지원
- 테이블, 트리 뷰 등 다양한 표현

### 다중 신호 상관관계

- **Trace → Profile**: 트레이스에서 특정 스팬의 프로파일로 점프 (Span Profiles)
- **Metric → Profile**: 메트릭 이상치 발견 시 관련 프로파일로 이동
- **Logs → Profile**: 로그 컨텍스트에서 동일 시간대 프로파일 조회

### 자동 계측 옵션

- **언어 SDK**: Go, Java, Python, Node.js, Ruby, .NET, Rust 등
- **Grafana Alloy**: 무계측 자동 수집 파이프라인
- **eBPF**: 커널 레벨 프로파일러로 코드 변경 없이 시스템 전체 프로파일링

---

## Pyroscope 프로파일링 스택

전형적인 Pyroscope 기반 스택은 **4가지 컴포넌트** 로 구성됩니다.

### 1. 클라이언트 계측 (Client Instrumentation)

애플리케이션이 프로파일을 생성·전송합니다. 두 가지 방식이 있습니다.

- **풀(Pull) 방식**: 애플리케이션이 pprof 엔드포인트(예: Go의 `/debug/pprof/profile`)를 노출 → Alloy/Agent가 주기적으로 가져감
- **푸시(Push) 방식**: 언어 SDK가 직접 Pyroscope 서버로 프로파일 전송

### 2. 파이프라인 (Pipeline)

수집·전처리·전달을 담당합니다.

- **Grafana Alloy** (권장): `pyroscope.scrape`, `pyroscope.write` 컴포넌트
- **OpenTelemetry Collector**: Profiles 신호 지원 추가 중
- **언어 SDK 직접 전송**: 별도 파이프라인 없이 SDK가 서버에 직접 전송

### 3. 백엔드 (Backend)

**Pyroscope** 가 프로파일을 저장하고 조회합니다.

### 4. 시각화 (Visualization)

- **Pyroscope 내장 UI**: `http://pyroscope:4040` 의 기본 UI
- **Grafana**: Pyroscope 데이터 소스 + Explore Profiles 앱

```
[Application]
  + SDK / pprof endpoint
      |
      v (push HTTP / pull HTTP)
[Grafana Alloy / OTel Collector]
      |
      v (push HTTP)
[Pyroscope Distributor] -> [Ingester] -> [Object Storage]
                              |              ^
                              v              |
                         [Querier] <---------+
                              ^
                              |
                        [Grafana / Pyroscope UI]
```

---

## 지원 프로파일 타입

Pyroscope는 다양한 자원 사용 측면을 프로파일링할 수 있습니다. 자세한 내용은 [04_profile_types.md](./04_profile_types.md) 참조.

| 프로파일 타입 | 측정 대상 | 주요 사용 |
|---------------|-----------|----------|
| `cpu` / `process_cpu` | CPU 사용 시간 | 핫스팟 식별, CPU 비용 절감 |
| `memory` / `inuse_objects` | 살아있는 객체 수 | 메모리 누수 탐지 |
| `memory` / `inuse_space` | 살아있는 객체 크기 | 메모리 사용량 분석 |
| `memory` / `alloc_objects` | 누적 할당 객체 수 | GC 부담 분석 |
| `memory` / `alloc_space` | 누적 할당 바이트 | 할당 핫스팟 |
| `goroutines` | 활성 고루틴 (Go) | 고루틴 누수 탐지 |
| `mutex` | 락 대기 시간 | 컨텐션(contention) 분석 |
| `block` | I/O 블록 시간 | 동기화 병목 분석 |

---

## Pyroscope와 다른 프로파일링 도구 비교

| 항목 | Pyroscope | Parca | Polar Signals Cloud | Datadog Profiler |
|------|-----------|-------|---------------------|-------------------|
| 라이선스 | AGPL-3.0 | Apache 2.0 | 상용 | 상용 |
| 스토리지 | 오브젝트 스토리지 | 오브젝트 스토리지 | 매니지드 | 매니지드 |
| 비용 | 매우 저렴 (자체 호스팅) | 저렴 | 상용 | 상용 |
| 멀티 테넌시 | 네이티브 | 부분 지원 | 매니지드 | 매니지드 |
| Grafana 통합 | 네이티브 | 데이터 소스 | 데이터 소스 | 외부 |
| eBPF 지원 | Alloy 기반 | 네이티브 | 네이티브 | 네이티브 |
| 언어 SDK | Go, Java, Python, Node, Ruby, .NET, Rust | 제한적 | 다수 | 다수 |
| 확장성 | 페타바이트 규모 | 중간 | 매니지드 | 매니지드 |

### Pyroscope의 핵심 차별점

**"Grafana 스택과의 일관성"**: Loki/Mimir/Tempo와 동일한 아키텍처와 운영 모델을 갖고 있습니다.

- 동일한 마이크로서비스 패턴, 해시 링, 오브젝트 스토리지 모델
- 동일한 Grafana UI에서 트레이스→프로파일, 메트릭→프로파일 간 자연스러운 이동
- Helm 차트, 설정 양식이 다른 LGTM 컴포넌트와 유사

---

## 다음 단계

- [02_architecture.md](./02_architecture.md) - Pyroscope 아키텍처 상세
- [03_deployment.md](./03_deployment.md) - 배포 모드 (Monolithic, Microservices)
- [04_profile_types.md](./04_profile_types.md) - 프로파일 타입과 pprof 형식
- [05_flamegraphs.md](./05_flamegraphs.md) - Flame Graph 분석
- [06_instrumentation.md](./06_instrumentation.md) - 애플리케이션 계측
