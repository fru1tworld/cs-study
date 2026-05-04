# Loki 배포 모드

> 이 문서는 Grafana Loki 공식 문서의 배포 모드 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/get-started/deployment-modes/

---

## 목차

1. [개요](#개요)
2. [모놀리식 모드 (Monolithic Mode)](#모놀리식-모드-monolithic-mode)
3. [Simple Scalable 모드 (SSD)](#simple-scalable-모드-ssd)
4. [마이크로서비스 모드 (Microservices Mode)](#마이크로서비스-모드-microservices-mode)
5. [모드 비교 및 선택 가이드](#모드-비교-및-선택-가이드)

---

## 개요

Loki는 단일 바이너리 내에서 마이크로서비스들을 실행할 수 있으며, **`-target` 플래그**로 배포 모드를 선택합니다. 동일한 바이너리로 모든 모드를 실행할 수 있어 운영 복잡성이 낮습니다.

배포 모드는 다음 세 가지가 있습니다.

1. **Monolithic Mode** (모놀리식): 단일 프로세스에 모든 컴포넌트
2. **Simple Scalable Deployment (SSD)**: 읽기/쓰기/백엔드 분리
3. **Microservices**: 컴포넌트 단위로 분리 실행

---

## 모놀리식 모드 (Monolithic Mode)

### 특징

`-target=all` 플래그를 사용하여 **모든 Loki 마이크로서비스 컴포넌트를 단일 프로세스 내에서 실행**합니다.

### 장점

- **단순성**: 가장 간단한 설정 및 운영
- **빠른 시작**: 즉시 실행 가능
- **실험과 학습에 적합**: 로컬 개발이나 PoC에 이상적
- **소규모 배포**: 일일 약 **20GB 정도**의 읽기/쓰기에 적합

### 단점

- **확장성 한계**: 단일 프로세스이므로 수직 확장(scale-up)만 가능
- **고가용성 부족**: 단일 인스턴스에서는 SPOF 발생
  - 여러 인스턴스를 동작시켜 HA 구성은 가능하지만, 모든 컴포넌트가 함께 확장됨

### 권장 사용 사례

- 개발 환경
- 작은 팀의 내부 도구
- 일일 20GB 이하의 로그 수집
- Loki 기능 학습 및 평가

### 다이어그램

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

## Simple Scalable 모드 (SSD)

### 특징

읽기, 쓰기, 백엔드 **세 가지 실행 경로로 분리**하여 각각 독립적으로 확장 가능하게 합니다.

- `-target=write`: Distributor + Ingester
- `-target=read`: Query Frontend + Querier
- `-target=backend`: Compactor + Ruler + Query Scheduler + Index Gateway

### 장점

- **확장성**: 일일 약 **TB 규모** 의 로그 처리 가능
- **비용 효율성**: 워크로드별 리소스 최적화 가능
- **Helm 차트 기본값**: Grafana Loki 공식 Helm 차트의 기본 설정
- **운영 단순성**: 마이크로서비스 모드보다 운영 부담이 적음

### 단점

- **리버스 프록시 필요**: 읽기/쓰기 경로 라우팅을 위해 필요 (예: Nginx, HAProxy, Caddy)
- **확장 한계**: 매우 큰 규모(수십 TB+/일)에서는 마이크로서비스 모드가 더 적합

### 권장 사용 사례

- 중소규모 프로덕션 환경
- 일일 100GB ~ 수 TB 규모의 로그
- Kubernetes 환경에서 Helm 차트로 배포
- 운영 복잡성을 최소화하고 싶은 경우

### 다이어그램

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

## 마이크로서비스 모드 (Microservices Mode)

### 특징

각 프로세스가 **고유한 `-target`을 지정하여 개별적으로 실행**됩니다. 각 컴포넌트는 독립적인 배포 단위가 됩니다.

가능한 target 예시:
- `-target=distributor`
- `-target=ingester`
- `-target=querier`
- `-target=query-frontend`
- `-target=query-scheduler`
- `-target=compactor`
- `-target=ruler`
- `-target=index-gateway`

### 장점

- **최고의 확장성**: 각 컴포넌트를 독립적으로 확장
- **세밀한 제어**: 특정 컴포넌트의 리소스를 정밀하게 튜닝 가능
- **장애 격리**: 한 컴포넌트의 문제가 다른 컴포넌트에 영향 최소화
- **대규모 처리**: 일일 페타바이트 규모까지 확장 가능

### 단점

- **복잡한 설정**: 가장 복잡한 배포 토폴로지
- **운영 부담**: 많은 컴포넌트를 모니터링하고 관리해야 함
- **복잡한 네트워크**: 컴포넌트 간 통신 설정 필요
- **초기 구축 비용**: 학습 곡선이 가파름

### 권장 사용 사례

- 매우 큰 규모의 프로덕션 환경 (일일 수 TB 이상)
- 컴포넌트별 정밀한 SLO 관리가 필요한 경우
- Kubernetes 등 컨테이너 오케스트레이션 활용
- 전담 SRE/Platform 팀이 있는 조직

### 다이어그램

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

## 모드 비교 및 선택 가이드

### 비교 표

| 항목 | Monolithic | SSD | Microservices |
|------|-----------|-----|--------------|
| 처리량 | ~20GB/일 | ~수 TB/일 | 페타바이트/일 |
| 운영 복잡도 | 낮음 | 중간 | 높음 |
| 확장 단위 | 전체 | 읽기/쓰기/백엔드 | 컴포넌트별 |
| HA 구성 | 가능 (제한적) | 가능 | 완전 지원 |
| 리버스 프록시 | 불필요 | 필요 | 필요 (LB) |
| Helm 기본값 | 아님 | 기본 | 옵션 |
| 적합 환경 | 개발, 소규모 | 중소규모 프로덕션 | 대규모 프로덕션 |

### 마이그레이션 경로

일반적으로 다음 순서로 확장합니다.

```
[Monolithic] ----성장----> [SSD] ----추가 성장----> [Microservices]
```

동일한 바이너리를 사용하므로 모드 간 전환이 비교적 수월합니다. 단, 데이터 마이그레이션이나 설정 변경은 신중하게 계획해야 합니다.

### 선택 의사결정 트리

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
