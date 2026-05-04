# Grafana Loki 개요

> 이 문서는 Grafana Loki 공식 문서의 "Learn about Loki" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/get-started/

---

## 목차

1. [Loki란 무엇인가](#loki란-무엇인가)
2. [Loki 로깅 스택](#loki-로깅-스택)
3. [Loki의 주요 기능](#loki의-주요-기능)
4. [핵심 개념](#핵심-개념)
5. [Loki와 Prometheus의 비교](#loki와-prometheus의-비교)
6. [다음 단계](#다음-단계)

---

## Loki란 무엇인가

**Grafana Loki** 는 Prometheus에서 영감을 받아 만들어진, 수평 확장 가능하고(horizontally-scalable), 고가용성(highly-available)을 갖춘 멀티 테넌트(multi-tenant) 로그 집계 시스템입니다.

Loki는 다음과 같은 차별점을 가집니다.

- **Prometheus와의 차이점**: Prometheus가 메트릭(metrics)에 집중하는 반면, Loki는 로그(logs)에 집중합니다. 또한 Pull 방식이 아닌 Push 방식으로 로그를 수집합니다.
- **비용 효율성**: Loki는 매우 비용 효율적이고 확장성이 뛰어나도록 설계되었습니다.
- **인덱싱 방식**: 다른 로깅 시스템과 달리 Loki는 로그의 **내용(content)을 인덱싱하지 않습니다**. 대신 각 로그 스트림에 대한 **메타데이터를 라벨(labels) 집합으로만 인덱싱**합니다.

### 로그 스트림(Log Stream)

**로그 스트림**은 동일한 라벨 집합을 공유하는 로그들의 모음입니다. 라벨은 Loki가 데이터 저장소에서 로그 스트림을 찾는 데 도움이 되므로, 효율적인 쿼리 실행을 위해서는 양질의 라벨 집합을 갖추는 것이 핵심입니다.

### 데이터 저장 방식

로그 데이터는 압축되어 **청크(chunks)** 형태로 오브젝트 스토리지에 저장됩니다. 사용 가능한 오브젝트 스토리지는 다음과 같습니다.

- Amazon S3 (Simple Storage Service)
- Google Cloud Storage (GCS)
- Azure Blob Storage
- 개발이나 PoC 용도로는 파일시스템도 가능

작은 인덱스와 고도로 압축된 청크 덕분에 Loki는 운영이 단순하고 비용이 크게 절감됩니다.

---

## Loki 로깅 스택

전형적인 Loki 기반 로깅 스택은 **3가지 컴포넌트**로 구성됩니다.

### 1. 에이전트(Agent)

에이전트 또는 클라이언트(예: Grafana Alloy)입니다. 에이전트는 다음 작업을 수행합니다.

- 로그를 수집(scrape)
- 라벨을 추가하여 로그를 스트림으로 변환
- HTTP API를 통해 스트림을 Loki로 전송(push)

### 2. Loki

메인 서버로서 다음 역할을 담당합니다.

- 로그 수집(ingestion) 및 저장(storage)
- 쿼리 처리(query processing)

### 3. Grafana

로그 데이터의 쿼리 및 시각화를 담당합니다.

```
[Agent (Alloy)] --push--> [Loki Server] <--query-- [Grafana]
                              |
                              v
                       [Object Storage]
```

---

## Loki의 주요 기능

### 확장성 (Scalability)

Loki는 확장성을 고려하여 설계되었습니다. 라즈베리 파이(Raspberry Pi)에서 동작할 수 있을 만큼 작게 실행할 수도 있고, 하루에 페타바이트(petabytes) 규모의 로그를 수집할 수 있을 만큼 크게 확장할 수도 있습니다.

### 멀티 테넌시 (Multi-tenancy)

Loki는 여러 테넌트가 단일 Loki 인스턴스를 공유할 수 있도록 합니다. 멀티 테넌시를 통해 각 테넌트의 데이터와 요청은 다른 테넌트로부터 완전히 격리됩니다.

테넌트 식별은 HTTP 헤더 `X-Scope-OrgID` 를 통해 이루어지며, 멀티 테넌시가 비활성화된 경우 테넌트 ID는 기본값 `fake` 로 설정됩니다.

### 서드 파티 통합 (Third-party Integrations)

플러그인을 통해 다양한 외부 에이전트(Grafana Alloy, Promtail, Fluentd, Fluent Bit, Logstash, Vector 등)를 지원합니다.

### 효율적인 저장 (Efficient Storage)

압축된 청크와 최소화된 인덱싱으로 운영 비용을 줄입니다.

### LogQL

LogQL은 Loki의 쿼리 언어입니다. Prometheus의 쿼리 언어인 PromQL에 익숙한 사용자라면 LogQL을 친숙하고 유연하게 사용할 수 있습니다.

### 알림 (Alerting)

Loki는 **Ruler** 라는 컴포넌트를 포함합니다. Ruler는 로그에 대한 쿼리를 지속적으로 평가하고, 결과에 따라 액션을 수행할 수 있습니다.

### Grafana 통합

Loki는 Grafana, Mimir, Tempo와 통합되어 완전한 관측성(observability) 스택을 제공합니다.

- **Loki**: 로그(Logs)
- **Mimir**: 메트릭(Metrics)
- **Tempo**: 트레이스(Traces)
- **Grafana**: 시각화(Visualization)

---

## 핵심 개념

### 라벨 (Labels)

라벨은 로그를 스트림으로 조직화하고 그룹화하는 키-값 쌍입니다. Loki는 로그 내용이 아닌 라벨을 인덱싱하므로, 라벨 전략은 성능에 매우 중요합니다.

#### 기본 라벨

**Service Name 라벨**
Loki는 다음 순서로 라벨을 검사하여 자동으로 `service_name` 라벨을 채우려고 시도합니다.

1. `service_name`
2. `service`
3. `app`
4. `application`
5. `name`
6. `app_kubernetes_io_name`
7. `container`
8. `container_name`
9. `component`
10. `workload`
11. `job`

위 라벨이 모두 없으면 `unknown_service` 로 기본 설정됩니다.

**OpenTelemetry Resource Attributes**
Grafana Alloy 또는 OpenTelemetry Collector를 사용하면 다음 속성들이 라벨로 변환됩니다 (마침표는 언더스코어로 변환).

- `cloud.availability_zone`, `cloud.region`
- `container.name`
- `deployment.environment.name`
- `k8s.cluster.name`, `k8s.container.name`, `k8s.job.name`, `k8s.namespace.name`, `k8s.pod.name`
- `service.instance.id`, `service.name`, `service.namespace`

#### 라벨 모범 사례

**카디널리티(Cardinality) 관리**
높은 카디널리티는 성능을 심각하게 저하시킵니다. 공식 문서는 "높은 카디널리티는 Loki의 성능과 비용 효율성을 크게 감소시킨다"고 경고합니다.

**라벨 가이드라인**

- 라벨은 **최대 10-15개** 이내로 사용
- 구체적이고 제한된(bounded) 값을 선택
- 소스 식별을 위한 라벨 사용 (namespace, cluster, region, filename, hostname)
- 로그 내용(level, message, exception names)을 라벨로 사용하지 않기
- 드물게 검색되는 항목이나 매우 특정한 식별자는 라벨로 만들지 않기

**라벨 명명 규칙**
라벨은 정규식 `[a-zA-Z_:][a-zA-Z0-9_:]*` 패턴과 일치해야 합니다. 지원되지 않는 문자는 언더스코어로 변환됩니다. 또한 이중 언더스코어 접두사/접미사는 피해야 합니다.

#### 구현 접근법

기본 라벨로 시작한 후, 쿼리 패턴에 따라 반복적으로 라벨을 다듬어 가야 합니다. 비즈니스 요구사항에 적합한 라벨을 결정하는 데는 여러 차례의 테스트가 필요할 수 있습니다.

### 청크 (Chunks)

청크는 특정 라벨 집합에 대한 로그 항목들의 컨테이너입니다. 청크는 다음 조건 중 하나가 충족되면 오브젝트 스토리지로 플러시(flush)됩니다.

- 일정 기간이 경과
- 일정 크기에 도달
- Ingester가 종료될 때

### 인덱스 (Index)

인덱스는 특정 라벨 집합에 대한 로그를 어디서 찾을 수 있는지를 나타내는 **목차(table of contents)** 역할을 합니다.

#### 인덱스 형식

- **TSDB** (권장): Prometheus 메인테이너들이 원래 개발한 형식
- **BoltDB** (Deprecated): 트랜잭셔널 키-값 스토어

---

## Loki와 Prometheus의 비교

| 항목 | Prometheus | Loki |
|------|-----------|------|
| 데이터 타입 | 메트릭 (시계열 숫자) | 로그 (텍스트) |
| 수집 방식 | Pull | Push |
| 인덱싱 | 모든 라벨과 값 인덱싱 | 메타데이터(라벨)만 인덱싱 |
| 쿼리 언어 | PromQL | LogQL |
| 저장소 | TSDB (로컬 또는 원격) | 오브젝트 스토리지 (S3, GCS 등) |
| 멀티 테넌시 | 기본 미지원 | 기본 지원 |

Loki는 의도적으로 Prometheus와 유사한 디자인을 채택하여, Prometheus 사용자가 쉽게 적응할 수 있도록 했습니다. 라벨링 시스템과 쿼리 언어 모두 일관된 사용 경험을 제공합니다.

---

## 다음 단계

- [02_architecture.md](./02_architecture.md) - Loki 아키텍처와 컴포넌트 상세
- [03_deployment_modes.md](./03_deployment_modes.md) - 배포 모드 (Single Binary, SSD, Microservices)
- [04_install_and_setup.md](./04_install_and_setup.md) - 설치 및 설정
- [05_configuration.md](./05_configuration.md) - 구성 레퍼런스
- [06_send_logs.md](./06_send_logs.md) - 로그 전송 클라이언트
- [07_logql.md](./07_logql.md) - LogQL 쿼리 언어
