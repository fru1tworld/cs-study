# Grafana Pyroscope 개요, 아키텍처, 배포 모드

## Grafana Pyroscope 개요

> 원본 참고: https://grafana.com/docs/pyroscope/latest/

---

### 목차

1. [Pyroscope란 무엇인가](#pyroscope란-무엇인가)
2. [Continuous Profiling이란](#continuous-profiling이란)
3. [Pyroscope의 주요 기능](#pyroscope의-주요-기능)
4. [Pyroscope 프로파일링 스택](#pyroscope-프로파일링-스택)
5. [지원 프로파일 타입](#지원-프로파일-타입)
6. [Pyroscope와 다른 프로파일링 도구 비교](#pyroscope와-다른-프로파일링-도구-비교)

---

### Pyroscope란 무엇인가

Grafana Pyroscope는 오픈소스이며 대규모(massively scalable)로 동작하는 연속 프로파일링(continuous profiling) 집계 시스템임. 2023년 Grafana Labs가 Pyroscope를 인수 → 기존 Phlare 프로젝트와 합쳐 현재의 Pyroscope v1이 됨.

#### 핵심 특징

- **오픈소스**: AGPL-3.0 라이선스
- **대규모 확장성**: Loki/Mimir/Tempo와 동일한 수평 확장 가능한 마이크로서비스 아키텍처
- **저비용**: 오브젝트 스토리지(S3, GCS, Azure Blob)에 프로파일 영속화
- **멀티 테넌시**: 단일 클러스터로 여러 조직/팀 운영 가능
- **고가용성**: 다중 복제와 일관성 있는 해시 링 기반 데이터 분배
- **다른 신호와의 통합**: 메트릭(Mimir), 로그(Loki), 트레이스(Tempo)와 연결

---

### Continuous Profiling이란

#### 프로파일링이란?

프로파일링(Profiling)은 애플리케이션 실행 중 어느 부분에서 자원(CPU, 메모리, 락 등)이 소모되는지를 측정하는 기법임. 전통적인 프로파일러는 개발 환경에서 짧게 실행해 단일 스냅샷을 얻는 방식이었음.

#### 연속 프로파일링의 등장

Continuous Profiling은 프로덕션 환경에서 항상 켜둘 수 있을 만큼 가벼운 오버헤드로 프로파일을 지속적으로 수집·저장·시각화하는 접근임. Google의 [Google-Wide Profiling 논문(2010)](https://research.google/pubs/google-wide-profiling-a-continuous-profiling-infrastructure-for-data-centers/)에서 그 가치가 처음 제시됨.

#### 왜 Continuous Profiling이 필요한가

- **운영 환경의 실제 동작 관찰**: 개발 환경에서는 재현되지 않는 성능 문제 분석
- **회귀(Regression) 자동 발견**: 배포 전후 프로파일 비교로 성능 저하 탐지
- **비용 최적화**: 코드 효율성이 클라우드 컴퓨팅 비용에 직접적인 영향을 미침
- **메모리 누수와 OOM 진단**: 메모리 사용 패턴을 시간에 걸쳐 추적
- **디버깅의 마지막 단계**: 메트릭(무엇), 로그(왜), 트레이스(어디)에 더해 **"코드의 어느 라인이 비싼가"** 를 알려줌

#### 관측가능성의 4번째 신호

전통적인 관측가능성(Observability)은 메트릭, 로그, 트레이스의 3가지 신호로 구성됨. Continuous Profiling은 4번째 신호로 자리잡음.

- 메트릭: 질문 "무엇이 일어나고 있는가?" · 도구 Mimir, Prometheus
- 로그: 질문 "왜 그런 일이 일어났는가?" · 도구 Loki
- 트레이스: 질문 "어디서 시간이 소요되었는가?" · 도구 Tempo
- 프로파일: 질문 "어느 코드가 비용을 만드는가?" · 도구 Pyroscope

---

### Pyroscope의 주요 기능

#### 효율적인 프로파일 저장

- 오브젝트 스토리지(S3, GCS, Azure Blob, MinIO)에 압축된 블록 형태로 영속화
- pprof 표준 형식 기반 + 자체 컬럼형 파일 포맷으로 빠른 쿼리 지원
- 공통 심볼 테이블 등 중복 제거를 통한 높은 압축률

#### 강력한 쿼리 모델

- **LabelSelector**: Prometheus PromQL 스타일의 라벨 매처 (`{service_name="frontend", env="prod"}`)
- **시간 범위 쿼리**: 특정 기간의 프로파일 집계
- **태그/라벨 그룹화**: 여러 차원으로 프로파일 분해
- **Diff 분석**: 두 시간 구간 또는 두 라벨 셋 간의 차이 시각화

#### Flame Graph 기반 시각화

- 함수 호출 스택을 직관적으로 표현하는 플레임 그래프
- 비교(Diff), 샌드위치(Sandwich) 뷰로 정밀 분석 지원
- 테이블, 트리 뷰 등 다양한 표현

#### 다중 신호 상관관계

- **Trace → Profile**: 트레이스에서 특정 스팬의 프로파일로 점프 (Span Profiles)
- **Metric → Profile**: 메트릭 이상치 발견 시 관련 프로파일로 이동
- **Logs → Profile**: 로그 컨텍스트에서 동일 시간대 프로파일 조회

#### 자동 계측 옵션

- **언어 SDK**: Go, Java, Python, Node.js, Ruby, .NET, Rust 등
- **Grafana Alloy**: 무계측 자동 수집 파이프라인
- **eBPF**: 커널 레벨 프로파일러로 코드 변경 없이 시스템 전체 프로파일링

---

### Pyroscope 프로파일링 스택

일반적인 Pyroscope 기반 스택은 4가지 컴포넌트로 구성됨.

#### 1. 클라이언트 계측 (Client Instrumentation)

애플리케이션이 프로파일을 생성·전송함. 두 가지 방식이 있음.

- **풀(Pull) 방식**: 애플리케이션이 pprof 엔드포인트(예: Go의 `/debug/pprof/profile`)를 노출 → Alloy/Agent가 주기적으로 가져감
- **푸시(Push) 방식**: 언어 SDK가 직접 Pyroscope 서버로 프로파일 전송

#### 2. 파이프라인 (Pipeline)

수집·전처리·전달을 담당함.

- **Grafana Alloy** (권장): `pyroscope.scrape`, `pyroscope.write` 컴포넌트
- **OpenTelemetry Collector**: Profiles 신호 지원 추가 중
- **언어 SDK 직접 전송**: 별도 파이프라인 없이 SDK가 서버에 직접 전송

#### 3. 백엔드 (Backend)

Pyroscope가 프로파일을 저장하고 조회함.

#### 4. 시각화 (Visualization)

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

### 지원 프로파일 타입

Pyroscope는 다양한 자원 사용 측면을 프로파일링 가능. 자세한 내용은 [04_profile_types.md](./04_profile_types.md) 참조.

- `cpu` / `process_cpu`: 측정 대상 CPU 사용 시간 · 주요 사용 핫스팟 식별, CPU 비용 절감
- `memory` / `inuse_objects`: 측정 대상 살아있는 객체 수 · 주요 사용 메모리 누수 탐지
- `memory` / `inuse_space`: 측정 대상 살아있는 객체 크기 · 주요 사용 메모리 사용량 분석
- `memory` / `alloc_objects`: 측정 대상 누적 할당 객체 수 · 주요 사용 GC 부담 분석
- `memory` / `alloc_space`: 측정 대상 누적 할당 바이트 · 주요 사용 할당 핫스팟
- `goroutines`: 측정 대상 활성 고루틴(Go) · 주요 사용 고루틴 누수 탐지
- `mutex`: 측정 대상 락 대기 시간 · 주요 사용 컨텐션(contention) 분석
- `block`: 측정 대상 I/O 블록 시간 · 주요 사용 동기화 병목 분석

---

### Pyroscope와 다른 프로파일링 도구 비교

- 라이선스: Pyroscope AGPL-3.0 · Parca Apache 2.0 · Polar Signals Cloud 상용 · Datadog Profiler 상용
- 스토리지: Pyroscope 오브젝트 스토리지 · Parca 오브젝트 스토리지 · Polar Signals Cloud 매니지드 · Datadog Profiler 매니지드
- 비용: Pyroscope 매우 저렴(자체 호스팅) · Parca 저렴 · Polar Signals Cloud 상용 · Datadog Profiler 상용
- 멀티 테넌시: Pyroscope 네이티브 · Parca 부분 지원 · Polar Signals Cloud 매니지드 · Datadog Profiler 매니지드
- Grafana 통합: Pyroscope 네이티브 · Parca 데이터 소스 · Polar Signals Cloud 데이터 소스 · Datadog Profiler 외부
- eBPF 지원: Pyroscope Alloy 기반 · Parca 네이티브 · Polar Signals Cloud 네이티브 · Datadog Profiler 네이티브
- 언어 SDK: Pyroscope Go, Java, Python, Node, Ruby, .NET, Rust · Parca 제한적 · Polar Signals Cloud 다수 · Datadog Profiler 다수
- 확장성: Pyroscope 페타바이트 규모 · Parca 중간 · Polar Signals Cloud 매니지드 · Datadog Profiler 매니지드

#### Pyroscope의 핵심 차별점

"Grafana 스택과의 일관성": Loki/Mimir/Tempo와 동일한 아키텍처와 운영 모델을 따름.

- 동일한 마이크로서비스 패턴, 해시 링, 오브젝트 스토리지 모델
- 동일한 Grafana UI에서 트레이스→프로파일, 메트릭→프로파일 간 자연스러운 이동
- Helm 차트, 설정 양식이 다른 LGTM 컴포넌트와 유사

---

### 다음 단계

- [02_architecture.md](./02_architecture.md) - Pyroscope 아키텍처 상세
- [03_deployment.md](./03_deployment.md) - 배포 모드 (Monolithic, Microservices)
- [04_profile_types.md](./04_profile_types.md) - 프로파일 타입과 pprof 형식
- [05_flamegraphs.md](./05_flamegraphs.md) - Flame Graph 분석
- [06_instrumentation.md](./06_instrumentation.md) - 애플리케이션 계측

---

## Pyroscope 아키텍처

> 원본: https://grafana.com/docs/pyroscope/latest/reference-pyroscope-architecture/

---

### 목차

1. [전체 아키텍처](#전체-아키텍처)
2. [핵심 컴포넌트](#핵심-컴포넌트)
3. [Write Path (쓰기 경로)](#write-path-쓰기-경로)
4. [Read Path (읽기 경로)](#read-path-읽기-경로)
5. [해시 링과 멤버십](#해시-링과-멤버십)
6. [스토리지 구조](#스토리지-구조)
7. [멀티 테넌시](#멀티-테넌시)

---

### 전체 아키텍처

Pyroscope는 마이크로서비스 기반 수평 확장 아키텍처를 따름. 동일한 바이너리를 다른 `target` 플래그로 실행하면 각각 다른 역할(컴포넌트)을 수행함.

```
                        ┌──────────────────────────────────┐
[Apps + SDK / Alloy] -> │ Distributor (수신, 검증, 분배)   │
                        └─────────────┬────────────────────┘
                                      │
                          (consistent hash on labels)
                                      ▼
                        ┌──────────────────────────────────┐
                        │ Ingester (메모리 + 로컬 디스크)    │
                        │   주기적으로 블록을 오브젝트       │
                        │   스토리지로 flush                 │
                        └─────────────┬────────────────────┘
                                      │
                                      ▼
                        ┌──────────────────────────────────┐
                        │ Object Storage (S3/GCS/Azure)    │
                        └─────────────┬────────────────────┘
                                      │
                          ┌───────────┴───────────┐
                          ▼                       ▼
              ┌──────────────────┐     ┌──────────────────┐
              │ Compactor        │     │ Store-Gateway    │
              │ (블록 병합/정리)  │     │ (장기 데이터 조회)│
              └──────────────────┘     └─────────┬────────┘
                                                 │
[Grafana / UI] -> Query-Frontend -> Querier ─────┤
                       │                         │
                       └────────► Ingester ──────┘
                                  (최근 데이터)
```

---

### 핵심 컴포넌트

#### Distributor

역할: 클라이언트로부터 프로파일을 수신하고, 검증한 뒤 적절한 Ingester로 라우팅함.

- 멀티 테넌시 인증 처리 (`X-Scope-OrgID` 헤더)
- 라벨 검증, 시리즈 카디널리티 한도 검사
- **일관된 해시(consistent hashing)** 로 시리즈를 Ingester에 분배
- HA 시나리오를 위해 N개의 Ingester로 복제 (보통 RF=3)
- 상태 비저장(stateless) → 자유롭게 수평 확장 가능

#### Ingester

역할: 최근 프로파일을 메모리/로컬 디스크에 보관하고 주기적으로 오브젝트 스토리지로 flush함.

- 활성 시리즈를 메모리에 유지하며 쿼리에 즉시 응답
- 일정 주기(예: 1시간)마다 블록을 빌드하여 S3 등에 업로드
- WAL(Write-Ahead-Log)을 로컬 디스크에 유지해 재시작 복구 지원
- 상태 저장(stateful) → 영구 볼륨(PVC) 필요

#### Querier

역할: 쿼리 시 Ingester(최근)와 Store-Gateway(장기)의 데이터를 합쳐 반환함.

- 라벨 셀렉터에 매칭되는 시리즈를 탐색하고
- 시간 범위에 해당하는 블록을 식별해 다운로드/스트리밍
- pprof 형식으로 머지(merge)하거나 flame graph 데이터로 변환
- 상태 비저장 → 수평 확장 가능

#### Query-Frontend

역할: Querier 앞단에서 쿼리를 분할·캐싱해 처리량과 응답성을 높임.

- 큰 시간 범위 쿼리를 여러 작은 단위로 나눠 병렬 처리
- 결과 캐싱
- 큐잉으로 Querier 부하 평탄화
- Query-Scheduler와 함께 사용 가능

#### Query-Scheduler (선택)

역할: 여러 Query-Frontend와 Querier 사이에서 쿼리 큐를 분리·중앙화함.

- 대규모 클러스터에서 더 정교한 부하 분산
- Query-Frontend의 상태를 줄여 무중단 재시작에 유리

#### Store-Gateway

역할: 오브젝트 스토리지에 저장된 블록 인덱스를 메모리에 두고 장기 데이터 쿼리를 가속함.

- 블록 메타데이터, 라벨 인덱스를 메모리/로컬 디스크에 캐시
- 샤딩(shuffle sharding) 가능 → 테넌트 격리
- 상태 저장 (캐시 워밍업 비용 있음)

#### Compactor

역할: 작은 블록들을 병합해 쿼리 효율과 스토리지 비용을 개선함.

- 같은 시간 범위, 같은 테넌트의 블록을 통합
- 보존 기간이 지난 블록 삭제
- 인덱스 재구축으로 쿼리 성능 향상
- 스토리지 일관성 유지(클린업)

#### 그 외

- **Tenant-Settings**: 테넌트별 한도/설정을 동적으로 관리
- **Admin API**: 운영용 엔드포인트

---

### Write Path (쓰기 경로)

1. **Application → Distributor**
   - 클라이언트(SDK/Alloy)가 HTTP로 pprof 페이로드를 전송 (`/ingest` 또는 `/push.v1.PusherService/Push`)
2. **Distributor 검증/분배**
   - 라벨 카디널리티, 페이로드 크기 등 검증
   - 라벨 해시로 N개 Ingester를 선택 (Replication Factor)
3. **Ingester 수신**
   - 메모리의 헤드 블록(head block)에 추가
   - WAL에 기록
4. **블록 빌드 & flush**
   - 헤드 블록이 일정 크기/시간에 도달하면 닫힘(closed)
   - 컬럼형 포맷으로 직렬화하여 오브젝트 스토리지에 업로드
5. **Compactor 병합**
   - 일정 주기마다 작은 블록을 큰 블록으로 병합

---

### Read Path (읽기 경로)

1. **Grafana → Query-Frontend**
   - 사용자가 시간 범위 + 라벨 매처로 쿼리 요청
2. **분할(Split) & 캐싱**
   - Query-Frontend가 큰 범위 쿼리를 분할
   - 결과 캐시 확인
3. **Querier 분배**
   - 각 분할 쿼리를 Querier에 분배 (또는 Query-Scheduler 경유)
4. **Querier 데이터 수집**
   - 최근 데이터: 모든 Ingester에 동시 요청 (replicas merge)
   - 장기 데이터: Store-Gateway 통해 블록 메타 조회 → 필요한 블록 다운로드/스트리밍
5. **머지 & 응답**
   - 시리즈/프로파일을 머지하여 flame graph용 데이터 또는 pprof로 반환

---

### 해시 링과 멤버십

Pyroscope는 분산 컴포넌트 간 멤버십과 샤딩을 위해 해시 링(Hash Ring)을 사용함.

#### 일관된 해시(Consistent Hashing)

- 각 Ingester가 링 상의 N개 토큰 점유
- 시리즈의 라벨을 해시한 값이 가장 가까운 토큰으로 라우팅
- 노드 추가/제거 시 영향 받는 시리즈 비율을 최소화

#### 멤버십 백엔드

해시 링 상태를 어떻게 저장/공유할지 선택 가능.

- **Memberlist (Gossip)**: 외부 의존성 없음, 권장 기본값
- **Consul**: 운영 경험이 있다면 채택 가능
- **etcd**: K8s 환경에서 옵션
- **In-memory**: 단일 인스턴스 테스트용

#### 복제 (Replication)

- Replication Factor(RF) 보통 3
- 한 시리즈가 3개 Ingester에 저장 → 1개 노드 다운 시에도 쿼리/쓰기 가능
- Quorum 기반 응답 (`(RF/2)+1`)

---

### 스토리지 구조

#### 블록 (Block)

- TSDB에서 영감을 받은 설계
- 시간 범위(예: 2시간)와 테넌트 단위로 묶인 단위
- ULID 기반 식별자

#### 블록 디렉토리 레이아웃 (오브젝트 스토리지)

```
<bucket>/
└── <tenant_id>/
    └── phlaredb/
        └── <block_id>/
            ├── meta.json           # 블록 메타데이터
            ├── index.tsdb           # TSDB 스타일 라벨 인덱스
            ├── profiles.parquet     # 컬럼형 프로파일 데이터
            ├── symbols/             # 함수, 매핑, 위치, 문자열 테이블
            └── tsdb/                # 추가 인덱싱 자료
```

- **profiles.parquet**: 프로파일 본체. Apache Parquet으로 저장되어 컬럼별 압축·필터링이 가능
- **symbols/**: 스택 트레이스의 함수 이름, 파일, 라인을 중복 제거(dedup)하여 저장 → 압축률 ↑
- **index.tsdb**: 라벨 → 시리즈 매핑

#### 보존 (Retention)

- 테넌트별 또는 글로벌 보존 기간 설정
- Compactor가 만료된 블록을 정기 삭제
- "라벨 카디널리티 한도" 와 함께 비용 통제의 핵심 수단

---

### 멀티 테넌시

#### 테넌트 식별

- HTTP 헤더 `X-Scope-OrgID` 로 테넌트 구분
- 인증/인가는 보통 **앞단의 게이트웨이** (Grafana, nginx, gateway)에서 처리

#### 격리 메커니즘

- **데이터 격리**: 모든 데이터가 테넌트 디렉토리 내에서만 저장됨
- **리소스 격리(선택)**:
  - **Shuffle Sharding**: 각 테넌트가 일부 Ingester/Store-Gateway 부분집합만 사용 → 시끄러운 이웃(noisy neighbor) 방지
  - **Per-tenant limits**: 시리즈 수, 인제스트 속도, 쿼리 시간 등을 테넌트별로 제한

#### 단일 테넌트 운영

소규모 환경이라면 `multitenancy_enabled: false`(기본값)로 두고 모든 데이터를 `anonymous` 테넌트에 저장 가능.

---

### 다음 단계

- [03_deployment.md](./03_deployment.md) - 배포 모드 상세
- [09_configuration.md](./09_configuration.md) - 설정 파일 구조
- [07_manage.md](./07_manage.md) - 운영 및 멀티 테넌시 관리

---

## Pyroscope 배포 모드

> 원본: https://grafana.com/docs/pyroscope/latest/deploy/

---

### 목차

1. [배포 모드 개요](#배포-모드-개요)
2. [Monolithic 모드](#monolithic-모드)
3. [Microservices 모드](#microservices-모드)
4. [Kubernetes 배포 (Helm)](#kubernetes-배포-helm)
5. [Docker / Docker Compose](#docker--docker-compose)
6. [규모별 권장 구성](#규모별-권장-구성)
7. [용량 산정](#용량-산정)

---

### 배포 모드 개요

Pyroscope는 단일 바이너리이며 실행 시 `-target` 플래그로 어떤 컴포넌트를 활성화할지 결정함.

- Monolithic: `-target` 값 `all`(기본) · 특징 모든 컴포넌트가 한 프로세스
- Read/Write 분리: `-target` 값 `read`, `write` · 특징 읽기/쓰기 경로만 분리
- Microservices: `-target` 값 `distributor`, `ingester`, ... · 특징 컴포넌트별 독립 프로세스

> 동일 바이너리를 다르게 실행 → 운영 자동화가 단순함(Loki/Mimir와 동일 패턴)

---

### Monolithic 모드

모든 컴포넌트가 단일 프로세스에서 실행됨.

#### 적합한 환경

- 개발/테스트
- 작은 규모 운영 (단일 인스턴스, 일 인제스트 수십 GB 이하)
- POC / 데모

#### 장점

- 단일 바이너리, 단일 포트로 운영 단순
- 외부 의존성 없음 (로컬 파일시스템 또는 단일 S3 버킷만 필요)

#### 단점

- 수평 확장 제한
- 컴포넌트별 자원 분리 불가
- HA 구성 시 stateful 부분(Ingester)이 병목

#### 실행 예시

```bash
pyroscope -config.file=pyroscope.yaml -target=all
```

---

### Microservices 모드

각 컴포넌트를 독립 프로세스/디플로이먼트로 운영함. 대규모 운영의 표준 형태임.

#### 일반적인 구성

- Distributor: 상태 Stateless · 권장 복제 수 2~
- Ingester: 상태 Stateful(PVC) · 권장 복제 수 3~(RF에 맞춤)
- Querier: 상태 Stateless · 권장 복제 수 2~
- Query-Frontend: 상태 Stateless · 권장 복제 수 2
- Query-Scheduler: 상태 Stateless · 권장 복제 수 2
- Store-Gateway: 상태 Stateful(캐시) · 권장 복제 수 2~
- Compactor: 상태 Stateful · 권장 복제 수 1~

#### 장점

- 컴포넌트별로 자원/스케일 정책 독립 적용
- 무중단 롤링 업데이트 용이
- 대규모 멀티 테넌트 운영에 최적

#### 단점

- 운영 복잡도 ↑
- 네트워크 트래픽 증가
- 모니터링/디버깅 채널 다양

---

### Kubernetes 배포 (Helm)

Grafana는 공식 Helm 차트를 제공함.

#### 차트 추가

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

#### Monolithic 설치 예

```yaml
# values.yaml
pyroscope:
  structuredConfig:
    storage:
      backend: s3
      s3:
        bucket: my-pyroscope
        endpoint: s3.amazonaws.com
        region: us-east-1
        access_key_id: ...
        secret_access_key: ...

  components:
    querier:
      kind: Deployment
      replicaCount: 1
    distributor:
      kind: Deployment
      replicaCount: 1
    ingester:
      kind: StatefulSet
      replicaCount: 1
```

```bash
helm install pyroscope grafana/pyroscope -f values.yaml
```

#### Microservices 설치 예

```yaml
pyroscope:
  components:
    distributor:
      kind: Deployment
      replicaCount: 3
    ingester:
      kind: StatefulSet
      replicaCount: 3
    querier:
      kind: Deployment
      replicaCount: 3
    query-frontend:
      kind: Deployment
      replicaCount: 2
    query-scheduler:
      kind: Deployment
      replicaCount: 2
    store-gateway:
      kind: StatefulSet
      replicaCount: 3
    compactor:
      kind: StatefulSet
      replicaCount: 1
```

#### 영구 볼륨

- Ingester, Store-Gateway, Compactor는 PVC 필요.
- Ingester PVC 크기는 보통 10~100Gi 사이 (보유 시간 + 인제스트 속도)

#### Ingress / Gateway

- `gateway` (nginx 기반) 컴포넌트로 단일 진입점 제공 가능
- 인증은 일반적으로 외부 Gateway(예: Grafana Cloud, Auth Proxy)에서 처리

---

### Docker / Docker Compose

#### 단일 컨테이너

```bash
docker run -d \
  -p 4040:4040 \
  -v $(pwd)/pyroscope.yaml:/etc/pyroscope/config.yaml \
  -v pyroscope-data:/data \
  grafana/pyroscope:latest \
  -config.file=/etc/pyroscope/config.yaml
```

#### Docker Compose (Monolithic + Grafana)

```yaml
version: '3.9'
services:
  pyroscope:
    image: grafana/pyroscope:latest
    ports:
      - "4040:4040"
    volumes:
      - ./pyroscope.yaml:/etc/pyroscope.yaml
      - pyroscope-data:/data
    command: ["-config.file=/etc/pyroscope.yaml"]

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_INSTALL_PLUGINS=grafana-pyroscope-app
volumes:
  pyroscope-data:
```

---

### 규모별 권장 구성

#### 작은 규모 (일 인제스트 < 50GB)

- **모드**: Monolithic, 1 노드
- **스토리지**: 로컬 디스크 또는 단일 S3 버킷
- **CPU/MEM**: 4 vCPU / 8GB RAM
- 모니터링은 자기 자신을 Prometheus로 스크랩하는 정도

#### 중간 규모 (일 인제스트 50GB ~ 1TB)

- **모드**: Microservices
- **Ingester**: 3 인스턴스 (RF=3)
- **Querier**: 2~4 인스턴스
- **Store-Gateway**: 2 인스턴스
- **Object Storage**: S3/GCS 권장
- 데모용 단일 노드와 비교해 5~10× 자원이 필요

#### 대규모 (일 인제스트 > 1TB)

- 본격적인 Microservices + Shuffle Sharding
- Ingester를 zone-aware로 배포 (예: 3 AZ × N replicas)
- Query-Scheduler 도입
- Compactor 다중 인스턴스
- 캐시 (memcached) 권장

---

### 용량 산정

대략적인 가이드라인이며, 실제 환경에서는 반드시 테스트 필요.

#### 시리즈 카디널리티

- 시리즈 = (서비스 × 인스턴스 × 라벨 조합)
- 1만~10만 시리즈는 통상 단일 클러스터에서 처리 가능

#### 인제스트 (Ingest) 용량

- 일반적으로 1Mi 프로파일/초 당 1~2 vCPU 필요 (Distributor + Ingester)
- WAL/메모리 사용은 시리즈 수에 비례

#### 스토리지

- 압축 후 일 보유 데이터 = 일 인제스트 × 압축률(0.2~0.5)
- 30일 보유 시 = 일 보유 데이터 × 30
- 오브젝트 스토리지 비용이 주된 항목

#### 쿼리

- Querier는 메모리에 블록 일부를 캐시
- 캐시 미스 시 S3 다운로드 시간이 응답 시간을 좌우
- 메모리 8GB+ 권장

---

### 다음 단계

- [04_profile_types.md](./04_profile_types.md) - 어떤 프로파일을 보낼지
- [06_instrumentation.md](./06_instrumentation.md) - 클라이언트 계측
- [09_configuration.md](./09_configuration.md) - 상세 설정
- [07_manage.md](./07_manage.md) - 운영 모범사례
