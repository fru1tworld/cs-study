# Kubernetes 아키텍처

> 공식 문서: https://kubernetes.io/docs/concepts/architecture/

## 개요

Kubernetes 클러스터는 **Control Plane(제어 평면)** 과 **Worker Nodes(워커 노드)** 로 구성됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                       Control Plane                              │
│  ┌────────────┐ ┌────────┐ ┌────────────┐ ┌─────────────────┐  │
│  │ API Server │ │  etcd  │ │ Scheduler  │ │ Controller Mgr  │  │
│  └────────────┘ └────────┘ └────────────┘ └─────────────────┘  │
│                                          ┌─────────────────────┐│
│                                          │ Cloud Controller Mgr││
│                                          └─────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│    Node 1      │  │    Node 2      │  │    Node 3      │
│  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │
│  │ kubelet  │  │  │  │ kubelet  │  │  │  │ kubelet  │  │
│  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │
│  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │
│  │kube-proxy│  │  │  │kube-proxy│  │  │  │kube-proxy│  │
│  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │
│  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │
│  │Container │  │  │  │Container │  │  │  │Container │  │
│  │ Runtime  │  │  │  │ Runtime  │  │  │  │ Runtime  │  │
│  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │
│  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │
│  │   Pods   │  │  │  │   Pods   │  │  │  │   Pods   │  │
│  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │
└────────────────┘  └────────────────┘  └────────────────┘
```

---

## Control Plane 컴포넌트

Control Plane은 클러스터의 전역 결정사항(스케줄링 등)과 클러스터 이벤트 감지 및 대응을 담당합니다.

### kube-apiserver

Kubernetes API를 노출하는 Control Plane의 프론트엔드입니다.

| 특징 | 설명 |
|------|------|
| 역할 | 클러스터의 모든 REST 요청 처리 |
| 확장성 | 수평 확장 가능 (여러 인스턴스 실행) |
| 인증/인가 | 모든 요청에 대한 인증, 인가, 어드미션 컨트롤 수행 |

```bash
# API 서버 상태 확인
kubectl cluster-info
kubectl get componentstatuses
```

### etcd

모든 클러스터 데이터를 저장하는 일관성 있는 고가용성 키-값 저장소입니다.

| 특징 | 설명 |
|------|------|
| 데이터 저장 | 클러스터 상태, 설정, 시크릿 등 모든 데이터 |
| 일관성 | Raft 합의 알고리즘 사용 |
| 고가용성 | 홀수 개의 노드로 클러스터 구성 권장 (3, 5, 7개) |

** 중요**: etcd 데이터는 반드시 정기적으로 백업해야 합니다.

```bash
# etcd 백업 예시
ETCDCTL_API=3 etcdctl snapshot save backup.db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key
```

### kube-scheduler

노드가 할당되지 않은 새로운 Pod를 감지하고, 실행할 노드를 선택합니다.

** 스케줄링 고려 요소:**
- 리소스 요구사항 (CPU, 메모리)
- 하드웨어/소프트웨어/정책 제약 조건
- Affinity/Anti-affinity 설정
- 데이터 지역성
- 워크로드 간 간섭

```yaml
# Pod 리소스 요구사항 예시
spec:
  containers:
  - name: app
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
```

### kube-controller-manager

여러 컨트롤러 프로세스를 단일 바이너리로 실행합니다.

| 컨트롤러 | 역할 |
|----------|------|
| Node Controller | 노드 상태 모니터링 및 응답 |
| Job Controller | 일회성 작업을 위한 Pod 생성 |
| EndpointSlice Controller | Service와 Pod 간 연결 관리 |
| ServiceAccount Controller | 새 네임스페이스에 기본 서비스 계정 생성 |
| Replication Controller | Pod 복제본 수 유지 |
| Deployment Controller | Deployment 상태 관리 |

### cloud-controller-manager

클라우드 제공자별 제어 로직을 포함합니다. 온프레미스 환경에서는 불필요합니다.

** 클라우드별 컨트롤러:**
- Node Controller: 클라우드에서 노드 삭제 여부 확인
- Route Controller: 클라우드 인프라의 라우트 설정
- Service Controller: 클라우드 로드 밸런서 생성/업데이트/삭제

---

## Node 컴포넌트

모든 노드에서 실행되며, Pod를 관리하고 Kubernetes 런타임 환경을 제공합니다.

### kubelet

각 노드에서 실행되는 에이전트로, Pod 내 컨테이너가 실행 중인지 확인합니다.

| 특징 | 설명 |
|------|------|
| 역할 | PodSpec에 따라 컨테이너 실행 및 상태 관리 |
| 헬스체크 | Liveness, Readiness, Startup Probe 실행 |
| 리소스 관리 | cgroups를 통한 리소스 제한 적용 |

** 주요 기능:**
1. API 서버로부터 PodSpec 수신
2. 컨테이너 런타임을 통해 컨테이너 생성
3. 컨테이너/Pod 상태를 API 서버에 보고
4. 노드 상태 보고

### kube-proxy

각 노드에서 네트워크 프록시로 동작하며, Service 개념의 구현을 담당합니다.

** 동작 모드:**

| 모드 | 설명 |
|------|------|
| iptables | iptables 규칙을 통한 트래픽 라우팅 (기본값) |
| IPVS | Linux IPVS를 사용한 고성능 로드밸런싱 |
| nftables | nftables 규칙 사용 (신규) |

```bash
# kube-proxy 모드 확인
kubectl get configmap kube-proxy -n kube-system -o yaml | grep mode
```

### Container Runtime

컨테이너 실행을 담당하는 소프트웨어입니다.

| 런타임 | 설명 |
|--------|------|
| containerd | Docker에서 분리된 경량 런타임 |
| CRI-O | OCI 호환 런타임 |
| Docker Engine | CRI를 통해 지원 (cri-dockerd 필요) |

**Container Runtime Interface (CRI)**
- Kubernetes와 컨테이너 런타임 간의 표준 인터페이스
- gRPC 기반 통신

---

## 클러스터 통신 흐름

```
┌──────────────────────────────────────────────────────────┐
│                    사용자/관리자                          │
│                        │                                 │
│                    kubectl                               │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                   kube-apiserver                         │
│         (인증 → 인가 → 어드미션 컨트롤)                    │
└──────────────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
    ┌────────┐    ┌──────────┐   ┌───────────┐
    │  etcd  │    │Scheduler │   │Controllers│
    └────────┘    └──────────┘   └───────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│                      kubelet                             │
│                        │                                 │
│              Container Runtime                           │
│                        │                                 │
│                      Pods                                │
└──────────────────────────────────────────────────────────┘
```

---

## 고가용성 구성

프로덕션 환경에서는 Control Plane의 고가용성을 위해 다중 마스터 구성을 권장합니다.

### 권장 구성

| 구성 요소 | 권장 개수 | 설명 |
|-----------|-----------|------|
| Control Plane 노드 | 3개 이상 (홀수) | etcd 쿼럼 유지 |
| Worker 노드 | 워크로드에 따라 | 장애 허용성 고려 |
| etcd 클러스터 | 3, 5, 7개 | Raft 합의를 위한 홀수 |

### Stacked etcd vs External etcd

**Stacked etcd (스택형)**
- Control Plane 노드에 etcd가 함께 실행
- 관리 단순, 노드 수 감소
- 결합도가 높아 단일 장애점 위험

**External etcd (외부형)**
- etcd가 별도 노드에서 실행
- Control Plane과 etcd 분리
- 높은 안정성, 관리 복잡성 증가

---

## 참고 자료

- [Kubernetes Components](https://kubernetes.io/docs/concepts/overview/components/)
- [Cluster Architecture](https://kubernetes.io/docs/concepts/architecture/)
- [etcd Documentation](https://etcd.io/docs/)
