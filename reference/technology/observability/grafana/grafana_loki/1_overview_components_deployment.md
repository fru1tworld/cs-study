# Grafana Loki 개요, 컴포넌트, 배포 모드

## Grafana Loki 개요

> 원본: https://grafana.com/docs/loki/latest/get-started/

---

### 목차

1. [Loki란 무엇인가](#loki란-무엇인가)
2. [Loki 로깅 스택](#loki-로깅-스택)
3. [Loki의 주요 기능](#loki의-주요-기능)
4. [핵심 개념](#핵심-개념)
5. [Loki와 Prometheus의 비교](#loki와-prometheus의-비교)
6. [다음 단계](#다음-단계)

---

### Loki란 무엇인가

Grafana Loki는 Prometheus에서 영감을 받아 만들어진 수평 확장 가능(horizontally-scalable)·고가용성(highly-available) 멀티 테넌트(multi-tenant) 로그 집계 시스템임.

Loki는 다음과 같은 차별점을 가짐.

- Prometheus와의 차이점: Prometheus는 메트릭(metrics)에 집중, Loki는 로그(logs)에 집중 → Pull 방식이 아닌 Push 방식으로 로그 수집
- 비용 효율성: 비용 효율적이고 확장성이 뛰어나도록 설계됨
- 인덱싱 방식: 다른 로깅 시스템과 달리 로그의 내용(content)을 인덱싱하지 않음 → 각 로그 스트림에 대한 메타데이터를 라벨(labels) 집합으로만 인덱싱

#### 로그 스트림(Log Stream)

로그 스트림은 동일한 라벨 집합을 공유하는 로그의 모음. 라벨은 Loki가 데이터 저장소에서 로그 스트림을 찾는 데 활용됨 → 효율적인 쿼리를 위해 적절한 라벨 집합 구성이 핵심.

#### 데이터 저장 방식

로그 데이터는 압축되어 청크(chunks) 형태로 오브젝트 스토리지에 저장됨. 사용 가능한 오브젝트 스토리지는 다음과 같음.

- Amazon S3 (Simple Storage Service)
- Google Cloud Storage (GCS)
- Azure Blob Storage
- 개발이나 PoC 용도로는 파일시스템도 가능

작은 인덱스와 고도로 압축된 청크 → 운영 단순화·비용 절감.

---

### Loki 로깅 스택

전형적인 Loki 기반 로깅 스택은 3가지 컴포넌트로 구성됨.

#### 1. 에이전트(Agent)

에이전트 또는 클라이언트(예: Grafana Alloy). 에이전트는 다음 작업을 수행함.

- 로그를 수집(scrape)
- 라벨을 추가하여 로그를 스트림으로 변환
- HTTP API를 통해 스트림을 Loki로 전송(push)

#### 2. Loki

메인 서버로서 다음 역할을 담당함.

- 로그 수집(ingestion) 및 저장(storage)
- 쿼리 처리(query processing)

#### 3. Grafana

로그 데이터의 쿼리 및 시각화 담당.

```
[Agent (Alloy)] --push--> [Loki Server] <--query-- [Grafana]
                              |
                              v
                       [Object Storage]
```

---

### Loki의 주요 기능

#### 확장성 (Scalability)

Loki는 확장성을 고려하여 설계됨. 라즈베리 파이(Raspberry Pi)에서 동작할 수 있을 만큼 작게 실행 가능 · 하루에 페타바이트(petabytes) 규모의 로그를 수집할 수 있을 만큼 크게 확장 가능.

#### 멀티 테넌시 (Multi-tenancy)

Loki는 여러 테넌트가 단일 Loki 인스턴스를 공유할 수 있게 함. 멀티 테넌시를 활성화하면 각 테넌트의 데이터와 요청이 다른 테넌트로부터 완전히 격리됨.

테넌트 식별은 HTTP 헤더 `X-Scope-OrgID` 를 통해 이루어짐. 멀티 테넌시가 비활성화된 경우 테넌트 ID는 기본값 `fake` 로 설정됨.

#### 서드 파티 통합 (Third-party Integrations)

플러그인을 통해 다양한 외부 에이전트(Grafana Alloy, Promtail, Fluentd, Fluent Bit, Logstash, Vector 등) 지원.

#### 효율적인 저장 (Efficient Storage)

압축된 청크와 최소화된 인덱싱으로 운영 비용 절감.

#### LogQL

LogQL은 Loki의 쿼리 언어. Prometheus의 쿼리 언어인 PromQL에 익숙한 사용자라면 LogQL을 친숙하고 유연하게 사용 가능.

#### 알림 (Alerting)

Loki는 Ruler 컴포넌트를 포함함. Ruler는 로그 쿼리를 지속적으로 평가하고, 결과에 따라 액션을 실행 가능.

#### Grafana 통합

Loki는 Grafana, Mimir, Tempo와 통합되어 완전한 관측성(observability) 스택을 제공함.

- Loki: 로그(Logs)
- Mimir: 메트릭(Metrics)
- Tempo: 트레이스(Traces)
- Grafana: 시각화(Visualization)

---

### 핵심 개념

#### 라벨 (Labels)

라벨은 로그를 스트림으로 조직화하고 그룹화하는 키-값 쌍. Loki는 로그 내용이 아닌 라벨을 인덱싱함 → 라벨 전략이 성능에 매우 중요.

##### 기본 라벨

Service Name 라벨

Loki는 다음 순서로 라벨을 검사하여 자동으로 `service_name` 라벨을 채우려고 시도함.

1. `service`
2. `app`
3. `application`
4. `app_name`
5. `name`
6. `app_kubernetes_io_name`
7. `container`
8. `container_name`
9. `k8s_container_name`
10. `component`
11. `workload`
12. `job`
13. `k8s_job_name`

위 라벨이 모두 없으면 `unknown_service` 로 기본 설정됨.

OpenTelemetry Resource Attributes

Grafana Alloy 또는 OpenTelemetry Collector를 사용하면 다음 속성들이 라벨로 변환됨(마침표는 언더스코어로 변환).

- `cloud.availability_zone`, `cloud.region`
- `container.name`
- `deployment.environment.name`
- `k8s.cluster.name`, `k8s.container.name`, `k8s.cronjob.name`, `k8s.daemonset.name`, `k8s.deployment.name`, `k8s.job.name`, `k8s.namespace.name`, `k8s.pod.name`, `k8s.replicaset.name`, `k8s.statefulset.name`
- `service.instance.id`, `service.name`, `service.namespace`

##### 라벨 모범 사례

카디널리티(Cardinality) 관리

높은 카디널리티는 성능을 심각하게 저하시킴. 공식 문서는 높은 카디널리티가 Loki의 성능과 비용 효율성을 크게 감소시킨다고 경고함.

라벨 가이드라인

- 라벨은 최대 10-15개 이내로 사용
- 구체적이고 제한된(bounded) 값을 선택
- 소스 식별을 위한 라벨 사용 (namespace, cluster, region, filename, hostname)
- 로그 내용(level, message, exception names)을 라벨로 사용하지 않기
- 드물게 검색되는 항목이나 매우 특정한 식별자는 라벨로 만들지 않기

라벨 명명 규칙

라벨은 정규식 `[a-zA-Z_:][a-zA-Z0-9_:]*` 패턴과 일치해야 함. 지원되지 않는 문자는 언더스코어로 변환됨. 이중 언더스코어 접두사/접미사는 피해야 함.

##### 구현 접근법

기본 라벨로 시작 → 쿼리 패턴에 따라 반복적으로 라벨을 다듬어 감. 비즈니스 요구사항에 적합한 라벨을 결정하는 데는 여러 차례의 테스트가 필요할 수 있음.

#### 청크 (Chunks)

청크는 특정 라벨 집합에 해당하는 로그 항목들의 컨테이너. 청크는 다음 조건 중 하나가 충족되면 오브젝트 스토리지로 플러시(flush)됨.

- 일정 기간이 경과
- 일정 크기에 도달
- Ingester가 종료될 때

#### 인덱스 (Index)

인덱스는 특정 라벨 집합에 대한 로그를 어디서 찾을 수 있는지를 나타내는 목차(table of contents) 역할.

##### 인덱스 형식

- TSDB (권장): Prometheus 메인테이너들이 원래 개발한 형식
- BoltDB (Deprecated): 트랜잭셔널 키-값 스토어

---

### Loki와 Prometheus의 비교

- 데이터 타입
  - Prometheus: 메트릭 (시계열 숫자)
  - Loki: 로그 (텍스트)
- 수집 방식
  - Prometheus: Pull
  - Loki: Push
- 인덱싱
  - Prometheus: 모든 라벨과 값 인덱싱
  - Loki: 메타데이터(라벨)만 인덱싱
- 쿼리 언어
  - Prometheus: PromQL
  - Loki: LogQL
- 저장소
  - Prometheus: TSDB (로컬 또는 원격)
  - Loki: 오브젝트 스토리지 (S3, GCS 등)
- 멀티 테넌시
  - Prometheus: 기본 미지원
  - Loki: 기본 지원

Loki는 의도적으로 Prometheus와 유사한 디자인을 채택 → Prometheus 사용자가 쉽게 적응 가능. 라벨링 시스템과 쿼리 언어 모두 일관된 사용 경험을 제공함.

---

### 다음 단계

- [02_architecture.md](./02_architecture.md) - Loki 아키텍처와 컴포넌트 상세
- [03_deployment_modes.md](./03_deployment_modes.md) - 배포 모드 (Single Binary, SSD, Microservices)
- [04_install_and_setup.md](./04_install_and_setup.md) - 설치 및 설정
- [05_configuration.md](./05_configuration.md) - 구성 레퍼런스
- [06_send_logs.md](./06_send_logs.md) - 로그 전송 클라이언트
- [07_logql.md](./07_logql.md) - LogQL 쿼리 언어

---

## Loki 컴포넌트

> 원본: https://grafana.com/docs/loki/latest/get-started/components/

---

### 목차

1. [개요](#개요)
2. [쓰기 경로 컴포넌트](#쓰기-경로-컴포넌트)
3. [읽기 경로 컴포넌트](#읽기-경로-컴포넌트)
4. [운영 컴포넌트](#운영-컴포넌트)
5. [실험적 컴포넌트 (Bloom)](#실험적-컴포넌트-bloom)

---

### 개요

Loki는 모듈식 시스템 → 컴포넌트들이 함께 또는 개별적으로 실행 가능. 다음 세 가지 배포 모드 중 하나에서 동작함.

- Single Binary (`-target=all`): 모든 컴포넌트가 단일 프로세스에서 실행
- Simple Scalable (`-target=read|write|backend`): 읽기/쓰기/백엔드 그룹으로 분리
- Microservices: 각 컴포넌트를 개별적으로 실행

---

### 쓰기 경로 컴포넌트

#### Distributor

역할: 클라이언트로부터 들어오는 푸시 요청을 처리.

주요 기능:
- 스트림 유효성 검증 (라벨 형식, 타임스탬프, 라인 길이)
- 테넌트별 Rate Limit 적용
- 일관된 해싱(consistent hashing)을 사용하여 Ingester로 라우팅
- 설정 가능한 복제 계수(replication factor, 기본값 3)
- 멀티 테넌시 강제 적용

스케일링: 무상태(stateless) → 수평 확장 자유로움

#### Ingester

역할: 데이터를 장기 스토리지에 영속화.

주요 기능:
- 메모리 내 청크 관리
- 오브젝트 스토리지로 청크 플러시(flush)
- 최근 데이터에 대한 쿼리 응답 (Querier가 직접 호출)
- WAL(Write-Ahead Log) 지원으로 충돌 시 데이터 복구

상태: 상태가 있는(stateful) 컴포넌트 → 메모리 내 청크 데이터를 보유

복제: 동일 데이터가 N개의 Ingester에 복제되어 가용성 보장

---

### 읽기 경로 컴포넌트

#### Query Frontend

역할: Querier API 엔드포인트를 제공하는 선택적(optional) 서비스.

주요 기능:
- 쿼리 분할(Query Splitting): 시간 범위가 긴 쿼리를 여러 작은 쿼리로 분할하여 병렬 실행
- 결과 캐싱(Caching): 결과를 캐시하여 반복 쿼리 성능 향상
- 기본 큐잉: Query Scheduler 없이 동작할 때 자체 FIFO 큐 사용
- 재시도(Retries): 실패한 쿼리 자동 재시도

스케일링: 무상태 → 수평 확장 가능

#### Query Scheduler

역할: 테넌트별 공정성을 보장하는 고급 큐잉 컴포넌트.

주요 기능:
- 테넌트별 큐 분리 (한 테넌트의 큰 쿼리가 다른 테넌트를 차단하지 않음)
- 쿼리 우선순위 관리
- Querier 가용성에 따른 효율적 분배

참고: 매우 큰 클러스터에서 사용 권장

#### Querier

역할: 실제 LogQL 쿼리를 실행.

주요 기능:
- LogQL 쿼리 실행
- Ingester에서 최근 데이터 조회
- 오브젝트 스토리지에서 과거 데이터 조회
- 결과 중복 제거(deduplication, 복제된 청크 처리)
- 결과 병합 후 Query Frontend로 반환

스케일링: 수평 확장 가능

---

### 운영 컴포넌트

#### Index Gateway

역할: Shipper 기반 스토어(TSDB/BoltDB)에 대한 메타데이터 쿼리를 처리.

주요 기능:
- Querier가 인덱스 파일을 직접 다운로드하지 않고 인덱스 조회 결과만 받을 수 있도록 지원
- 인덱스 파일 캐싱
- 메모리 사용량 감소

#### Compactor

역할: 인덱스 파일 병합과 보존 기간(retention) 관리를 담당.

주요 기능:
- 여러 작은 인덱스 파일을 하나로 병합 (쿼리 성능 향상)
- 보존 정책에 따른 데이터 삭제
- 사용자가 요청한 데이터 삭제(deletion) 처리
- 단일 인스턴스 권장 (분산 모드 옵션도 있음)

#### Ruler

역할: 룰(rules)과 알림 표현식을 관리하고 평가.

주요 기능:
- Recording Rules 평가 (메트릭 생성)
- Alerting Rules 평가 (알림 조건 모니터링)
- Alertmanager로 알림 전송
- 여러 인스턴스 운영 시 일관된 해싱으로 룰 그룹 분산

#### Pattern Ingester

역할: Drain 알고리즘을 사용하여 로그 패턴을 감지하고 집계. 기본값은 비활성화 상태 → 설정 파일에서 명시적으로 활성화 필요.

주요 기능:
- 유사한 로그 라인을 패턴으로 그룹화
- 로그 폭증 시 디버깅 보조
- Grafana Logs Drilldown 통합 시 자동 패턴 감지

---

### 실험적 컴포넌트 (Bloom)

> 주의: Bloom 관련 기능은 실험적(experimental) → 프로덕션 사용 전 검토 필요

#### Bloom Planner

역할: 블룸(bloom) 생성 작업을 계획.

#### Bloom Builder

역할: 로그 메타데이터로부터 블룸 블록을 생성.

#### Bloom Gateway

역할: 청크 필터링 요청을 처리.

Bloom 필터의 목적: 텍스트 검색 성능 향상. 라벨 인덱스만으로는 빠르게 필터링하기 어려운 본문 텍스트 검색에 효과적.

---

### 컴포넌트별 스케일링 특성

- Distributor
  - 상태성: Stateless
  - 스케일링: 수평 확장
  - 비고: 트래픽 비례
- Ingester
  - 상태성: Stateful
  - 스케일링: 수평 확장
  - 비고: 복제 계수 고려
- Query Frontend
  - 상태성: Stateless
  - 스케일링: 수평 확장
  - 비고: 보통 2-3개
- Query Scheduler
  - 상태성: Stateless
  - 스케일링: 수평 확장
  - 비고: 큰 클러스터에서 사용
- Querier
  - 상태성: Stateless
  - 스케일링: 수평 확장
  - 비고: 동시 쿼리 수에 비례
- Index Gateway
  - 상태성: Stateless
  - 스케일링: 수평 확장
  - 비고: 인덱스 크기에 비례
- Compactor
  - 상태성: Stateful
  - 스케일링: 단일 인스턴스
  - 비고: 보존 관리
- Ruler
  - 상태성: Stateful
  - 스케일링: 수평 확장
  - 비고: 룰 수에 비례

---

## Loki 배포 모드

> 원본: https://grafana.com/docs/loki/latest/get-started/deployment-modes/

---

### 목차

1. [개요](#개요)
2. [모놀리식 모드 (Monolithic Mode)](#모놀리식-모드-monolithic-mode)
3. [Simple Scalable 모드 (SSD)](#simple-scalable-모드-ssd)
4. [마이크로서비스 모드 (Microservices Mode)](#마이크로서비스-모드-microservices-mode)
5. [모드 비교 및 선택 가이드](#모드-비교-및-선택-가이드)

---

### 개요

Loki는 단일 바이너리 내에서 마이크로서비스들을 실행할 수 있음. `-target` 플래그로 배포 모드를 선택함. 동일한 바이너리로 모든 모드를 실행 가능 → 운영 복잡성이 낮음.

배포 모드는 다음 세 가지가 있음.

1. Monolithic Mode (모놀리식): 단일 프로세스에 모든 컴포넌트
2. Simple Scalable Deployment (SSD): 읽기/쓰기/백엔드 분리
3. Microservices: 컴포넌트 단위로 분리 실행

---

### 모놀리식 모드 (Monolithic Mode)

#### 특징

`-target=all` 플래그를 사용하여 모든 Loki 마이크로서비스 컴포넌트를 단일 프로세스 내에서 실행.

#### 장점

- 단순성: 가장 간단한 설정 및 운영
- 빠른 시작: 즉시 실행 가능
- 실험과 학습에 적합: 로컬 개발이나 PoC에 이상적
- 소규모 배포: 일일 약 20GB 정도의 읽기/쓰기에 적합

#### 단점

- 확장성 한계: 단일 프로세스 → 수직 확장(scale-up)만 가능
- 고가용성 부족: 단일 인스턴스에서는 SPOF 발생
  - 여러 인스턴스를 동작시켜 HA 구성은 가능하지만, 모든 컴포넌트가 함께 확장됨

#### 권장 사용 사례

- 개발 환경
- 작은 팀의 내부 도구
- 일일 20GB 이하의 로그 수집
- Loki 기능 학습 및 평가

#### 다이어그램

```
┌──────────────────────────────────────┐
│         Loki (single process)        │
│                                       │
│  Distributor + Ingester + Querier +  │
│  Query Frontend + Compactor + Ruler  │
│                                       │
└──────────────────┬───────────────────┘
                   │
                   v
            [Object Storage]
```

---

### Simple Scalable 모드 (SSD)

#### 특징

읽기, 쓰기, 백엔드 세 가지 실행 경로로 분리하여 각각 독립적으로 확장 가능.

- `-target=write`: Distributor + Ingester
- `-target=read`: Query Frontend + Querier
- `-target=backend`: Compactor + Ruler + Query Scheduler + Index Gateway

#### 장점

- 확장성: 일일 약 TB 규모의 로그 처리 가능
- 비용 효율성: 워크로드별 리소스 최적화 가능
- Helm 차트 기본값: Grafana Loki 공식 Helm 차트의 기본 설정
- 운영 단순성: 마이크로서비스 모드보다 운영 부담이 적음

#### 단점

- 리버스 프록시 필요: 읽기/쓰기 경로 라우팅을 위해 필요 (예: Nginx, HAProxy, Caddy)
- 확장 한계: 매우 큰 규모(수십 TB+/일)에서는 마이크로서비스 모드가 더 적합

#### 권장 사용 사례

- 중소규모 프로덕션 환경
- 일일 100GB ~ 수 TB 규모의 로그
- Kubernetes 환경에서 Helm 차트로 배포
- 운영 복잡성을 최소화하고 싶은 경우

#### 다이어그램

```
                  [Reverse Proxy / LB]
                          │
        ┌─────────────────┼─────────────────┐
        v                 v                 v
   ┌─────────┐       ┌─────────┐       ┌──────────┐
   │  Read   │       │  Write  │       │ Backend  │
   │ (N개)   │       │ (N개)   │       │ (N개)    │
   └────┬────┘       └────┬────┘       └─────┬────┘
        │                 │                  │
        └─────────────────┼──────────────────┘
                          v
                   [Object Storage]
```

---

### 마이크로서비스 모드 (Microservices Mode)

#### 특징

각 프로세스에 고유한 `-target`을 지정하여 개별적으로 실행. 각 컴포넌트는 독립적인 배포 단위가 됨.

가능한 target 예시:
- `-target=distributor`
- `-target=ingester`
- `-target=querier`
- `-target=query-frontend`
- `-target=query-scheduler`
- `-target=compactor`
- `-target=ruler`
- `-target=index-gateway`

#### 장점

- 최고의 확장성: 각 컴포넌트를 독립적으로 확장
- 세밀한 제어: 특정 컴포넌트의 리소스를 정밀하게 튜닝 가능
- 장애 격리: 한 컴포넌트의 문제가 다른 컴포넌트에 영향 최소화
- 대규모 처리: 일일 페타바이트 규모까지 확장 가능

#### 단점

- 복잡한 설정: 가장 복잡한 배포 토폴로지
- 운영 부담: 많은 컴포넌트를 모니터링하고 관리해야 함
- 복잡한 네트워크: 컴포넌트 간 통신 설정 필요
- 초기 구축 비용: 학습 곡선이 가파름

#### 권장 사용 사례

- 매우 큰 규모의 프로덕션 환경 (일일 수 TB 이상)
- 컴포넌트별 정밀한 SLO 관리가 필요한 경우
- Kubernetes 등 컨테이너 오케스트레이션 활용
- 전담 SRE/Platform 팀이 있는 조직

#### 다이어그램

```
                  [Ingress / LB]
                        │
        ┌───────────────┼───────────────┐
        v                               v
  [Distributor pod]                [Query Frontend pod]
        │                               │
        v                               v
  [Ingester pod]                  [Query Scheduler pod]
        │                               │
        v                               v
  [Object Storage]  <----------  [Querier pod]
                                        ^
                                        │
                              [Index Gateway pod]
                              [Compactor pod]
                              [Ruler pod]
```

---

### 모드 비교 및 선택 가이드

#### 비교

- 처리량
  - Monolithic: ~20GB/일
  - SSD: ~수 TB/일
  - Microservices: 페타바이트/일
- 운영 복잡도
  - Monolithic: 낮음
  - SSD: 중간
  - Microservices: 높음
- 확장 단위
  - Monolithic: 전체
  - SSD: 읽기/쓰기/백엔드
  - Microservices: 컴포넌트별
- HA 구성
  - Monolithic: 가능 (제한적)
  - SSD: 가능
  - Microservices: 완전 지원
- 리버스 프록시
  - Monolithic: 불필요
  - SSD: 필요
  - Microservices: 필요 (LB)
- Helm 기본값
  - Monolithic: 아님
  - SSD: 기본
  - Microservices: 옵션
- 적합 환경
  - Monolithic: 개발, 소규모
  - SSD: 중소규모 프로덕션
  - Microservices: 대규모 프로덕션

#### 마이그레이션 경로

일반적으로 다음 순서로 확장함.

```
[Monolithic] ----성장----> [SSD] ----추가 성장----> [Microservices]
```

동일한 바이너리를 사용 → 모드 간 전환이 비교적 수월. 단, 데이터 마이그레이션과 설정 변경은 신중하게 계획해야 함.

#### 선택 의사결정 트리

```
일일 로그 양이 얼마인가?
│
├── 20GB 이하 → Monolithic 모드
│
├── 20GB ~ 수 TB → SSD 모드
│
└── 수 TB 이상 → Microservices 모드 검토
   │
   └── SRE 팀이 있는가? Yes → Microservices
                        No  → SSD 유지 + 수직 확장
```
