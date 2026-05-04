# Pyroscope 아키텍처

> 이 문서는 Grafana Pyroscope의 내부 아키텍처와 컴포넌트, 데이터 흐름, 스토리지 구조를 설명합니다.
> 원본: https://grafana.com/docs/pyroscope/latest/reference-pyroscope-architecture/

---

## 목차

1. [전체 아키텍처](#전체-아키텍처)
2. [핵심 컴포넌트](#핵심-컴포넌트)
3. [Write Path (쓰기 경로)](#write-path-쓰기-경로)
4. [Read Path (읽기 경로)](#read-path-읽기-경로)
5. [해시 링과 멤버십](#해시-링과-멤버십)
6. [스토리지 구조](#스토리지-구조)
7. [멀티 테넌시](#멀티-테넌시)

---

## 전체 아키텍처

Pyroscope는 **마이크로서비스 기반 수평 확장 아키텍처** 를 따릅니다. 동일한 바이너리를 다른 `target` 플래그로 실행하면 각각 다른 역할(컴포넌트)을 수행합니다.

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

## 핵심 컴포넌트

### Distributor

**역할**: 클라이언트로부터 프로파일을 수신하고, 검증한 뒤 적절한 Ingester로 라우팅합니다.

- 멀티 테넌시 인증 처리 (`X-Scope-OrgID` 헤더)
- 라벨 검증, 시리즈 카디널리티 한도 검사
- **일관된 해시(consistent hashing)** 로 시리즈를 Ingester에 분배
- HA 시나리오를 위해 N개의 Ingester로 복제 (보통 RF=3)
- 상태 비저장(stateless) → 자유롭게 수평 확장 가능

### Ingester

**역할**: 최근 프로파일을 메모리/로컬 디스크에 보관하고 주기적으로 오브젝트 스토리지로 flush합니다.

- 활성 시리즈를 메모리에 유지하며 쿼리에 즉시 응답
- 일정 주기(예: 1시간)마다 블록을 빌드하여 S3 등에 업로드
- WAL(Write-Ahead-Log)을 로컬 디스크에 유지해 재시작 복구 지원
- 상태 저장(stateful) → 영구 볼륨(PVC) 필요

### Querier

**역할**: 쿼리 시 Ingester(최근)와 Store-Gateway(장기)의 데이터를 합쳐 반환합니다.

- 라벨 셀렉터에 매칭되는 시리즈를 찾고
- 시간 범위에 해당하는 블록을 식별하여 다운로드/스트리밍
- pprof 형식으로 머지(merge)하거나 flame graph 데이터로 변환
- 상태 비저장 → 수평 확장 가능

### Query-Frontend

**역할**: Querier 앞단에서 쿼리를 분할·캐싱하여 처리량과 응답성을 높입니다.

- 큰 시간 범위 쿼리를 여러 작은 단위로 나눠 병렬 처리
- 결과 캐싱
- 큐잉으로 Querier 부하 평탄화
- Query-Scheduler와 함께 사용 가능

### Query-Scheduler (선택)

**역할**: 여러 Query-Frontend와 Querier 사이에서 쿼리 큐를 분리·중앙화합니다.

- 대규모 클러스터에서 더 정교한 부하 분산
- Query-Frontend의 상태를 줄여 무중단 재시작에 유리

### Store-Gateway

**역할**: 오브젝트 스토리지에 저장된 블록 인덱스를 메모리에 두고 장기 데이터 쿼리를 가속합니다.

- 블록 메타데이터, 라벨 인덱스를 메모리/로컬 디스크에 캐시
- 샤딩(shuffle sharding) 가능 → 테넌트 격리
- 상태 저장 (캐시 워밍업 비용 있음)

### Compactor

**역할**: 작은 블록들을 병합해 쿼리 효율과 스토리지 비용을 개선합니다.

- 같은 시간 범위, 같은 테넌트의 블록을 통합
- 보존 기간이 지난 블록 삭제
- 인덱스 재구축으로 쿼리 성능 향상
- 스토리지 일관성 유지(클린업)

### 그 외

- **Tenant-Settings**: 테넌트별 한도/설정을 동적으로 관리
- **Admin API**: 운영용 엔드포인트

---

## Write Path (쓰기 경로)

1. **Application → Distributor**
   - 클라이언트(SDK/Alloy)가 HTTP로 pprof 페이로드를 전송 (`/ingest` 또는 `/push.v1.PusherService/Push`)
2. **Distributor 검증/분배**
   - 라벨 카디널리티, 페이로드 크기 등 검증
   - 라벨 해시로 N개 Ingester를 선택 (Replication Factor)
3. **Ingester 수신**
   - 메모리의 헤드 블록(head block)에 추가
   - WAL에 기록
4. **블록 빌드 & flush**
   - 헤드 블록이 일정 크기/시간에 도달하면 closed
   - 컬럼형 포맷으로 직렬화하여 오브젝트 스토리지에 업로드
5. **Compactor 병합**
   - 일정 주기마다 작은 블록을 큰 블록으로 병합

---

## Read Path (읽기 경로)

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

## 해시 링과 멤버십

Pyroscope는 분산 컴포넌트 간 멤버십과 샤딩을 위해 **해시 링(Hash Ring)** 을 사용합니다.

### 일관된 해시(Consistent Hashing)

- 각 Ingester가 링 상의 N개 토큰 점유
- 시리즈의 라벨을 해시한 값이 가장 가까운 토큰으로 라우팅
- 노드 추가/제거 시 영향 받는 시리즈 비율을 최소화

### 멤버십 백엔드

해시 링 상태를 어떻게 저장/공유할지 선택할 수 있습니다.

- **Memberlist (Gossip)**: 외부 의존성 없음, 권장 기본값
- **Consul**: 운영 경험이 있다면 채택 가능
- **etcd**: K8s 환경에서 옵션
- **In-memory**: 단일 인스턴스 테스트용

### 복제 (Replication)

- Replication Factor(RF) 보통 3
- 한 시리즈가 3개 Ingester에 저장 → 1개 노드 다운 시에도 쿼리/쓰기 가능
- Quorum 기반 응답 (`(RF/2)+1`)

---

## 스토리지 구조

### 블록 (Block)

- TSDB 영감을 받은 디자인
- 시간 범위(예: 2시간)와 테넌트 단위로 묶인 단위
- ULID 기반 식별자

### 블록 디렉토리 레이아웃 (오브젝트 스토리지)

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
- **symbols/**: 스택 트레이스의 함수 이름, 파일, 라인을 dedup하여 저장 → 압축률 ↑
- **index.tsdb**: 라벨 → 시리즈 매핑

### 보존 (Retention)

- 테넌트별 또는 글로벌 보존 기간 설정
- Compactor가 만료된 블록을 정기 삭제
- "라벨 카디널리티 한도" 와 함께 비용 통제의 핵심 수단

---

## 멀티 테넌시

### 테넌트 식별

- HTTP 헤더 `X-Scope-OrgID` 로 테넌트 구분
- 인증/인가는 보통 **앞단의 게이트웨이** (Grafana, nginx, gateway)에서 처리

### 격리 메커니즘

- **데이터 격리**: 모든 데이터가 테넌트 디렉토리 내에서만 저장됨
- **리소스 격리(선택)**:
  - **Shuffle Sharding**: 각 테넌트가 일부 Ingester/Store-Gateway 부분집합만 사용 → 시끄러운 이웃(noisy neighbor) 방지
  - **Per-tenant limits**: 시리즈 수, 인제스트 속도, 쿼리 시간 등을 테넌트별로 제한

### 단일 테넌트 운영

소규모 환경이라면 `auth_enabled: false` 로 두고 모든 데이터를 `anonymous` 테넌트에 저장할 수 있습니다.

---

## 다음 단계

- [03_deployment.md](./03_deployment.md) - 배포 모드 상세
- [09_configuration.md](./09_configuration.md) - 설정 파일 구조
- [07_manage.md](./07_manage.md) - 운영 및 멀티 테넌시 관리
