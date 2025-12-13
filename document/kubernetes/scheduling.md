# Kubernetes 스케줄링

> 공식 문서: https://kubernetes.io/docs/concepts/scheduling-eviction/

## 개요

Kubernetes 스케줄러는 Pod를 적절한 노드에 배치하는 역할을 합니다. 리소스 요구사항, 하드웨어/소프트웨어 제약, 친화성/반친화성 설정 등을 고려합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      스케줄링 결정 요소                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐   │
│  │ 리소스 요구사항 │  │  노드 셀렉터   │  │  Node Affinity    │   │
│  │ (CPU, Memory) │  │ (nodeSelector)│  │  (상세 조건)       │   │
│  └───────────────┘  └───────────────┘  └───────────────────┘   │
│                                                                 │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐   │
│  │  Pod Affinity │  │ Pod AntiAff.  │  │ Taint/Toleration  │   │
│  │  (같이 배치)   │  │ (분리 배치)   │  │  (노드 제외)       │   │
│  └───────────────┘  └───────────────┘  └───────────────────┘   │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              Topology Spread Constraints                  │ │
│  │                   (균등 분산 배치)                          │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## nodeSelector

가장 간단한 노드 선택 방법입니다. 노드 레이블과 일치하는 노드에만 Pod를 스케줄링합니다.

### 노드 레이블 확인 및 추가

```bash
# 노드 레이블 확인
kubectl get nodes --show-labels

# 노드에 레이블 추가
kubectl label nodes node1 disktype=ssd

# 레이블 제거
kubectl label nodes node1 disktype-
```

### nodeSelector 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  nodeSelector:
    disktype: ssd
    kubernetes.io/os: linux
  containers:
  - name: nginx
    image: nginx:1.25
```

### 내장 노드 레이블

| 레이블 | 설명 |
|--------|------|
| kubernetes.io/hostname | 노드 호스트명 |
| kubernetes.io/os | 운영체제 (linux, windows) |
| kubernetes.io/arch | 아키텍처 (amd64, arm64) |
| topology.kubernetes.io/zone | 가용 영역 |
| topology.kubernetes.io/region | 리전 |
| node.kubernetes.io/instance-type | 인스턴스 유형 (클라우드) |

---

## Node Affinity

nodeSelector보다 표현력이 풍부한 노드 선택 방법입니다.

### 유형

| 유형 | 설명 |
|------|------|
| requiredDuringSchedulingIgnoredDuringExecution | 필수 조건 (충족하지 않으면 스케줄링 안 됨) |
| preferredDuringSchedulingIgnoredDuringExecution | 선호 조건 (가능하면 충족, 불가능하면 다른 노드) |

### Required (필수)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: with-node-affinity
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: topology.kubernetes.io/zone
            operator: In
            values:
            - ap-northeast-2a
            - ap-northeast-2b
          - key: disktype
            operator: In
            values:
            - ssd
  containers:
  - name: app
    image: myapp:1.0
```

### Preferred (선호)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: with-preferred-affinity
spec:
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 80  # 가중치 1-100
        preference:
          matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
      - weight: 20
        preference:
          matchExpressions:
          - key: cpu-type
            operator: In
            values:
            - high-performance
  containers:
  - name: app
    image: myapp:1.0
```

### 연산자 (Operator)

| 연산자 | 설명 | 예시 |
|--------|------|------|
| In | 값 목록 중 하나와 일치 | values: [a, b] |
| NotIn | 값 목록에 없음 | values: [a, b] |
| Exists | 키가 존재 | (values 불필요) |
| DoesNotExist | 키가 존재하지 않음 | (values 불필요) |
| Gt | 값보다 큼 (정수) | values: ["5"] |
| Lt | 값보다 작음 (정수) | values: ["10"] |

### Required + Preferred 조합

```yaml
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: kubernetes.io/os
            operator: In
            values:
            - linux
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        preference:
          matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
```

---

## Pod Affinity / Anti-Affinity

다른 Pod의 레이블을 기반으로 스케줄링을 제어합니다.

### Pod Affinity (함께 배치)

특정 Pod와 같은 노드/토폴로지에 배치합니다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web-server
spec:
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - cache
        topologyKey: kubernetes.io/hostname  # 같은 노드
  containers:
  - name: web
    image: nginx:1.25
```

### Pod Anti-Affinity (분리 배치)

특정 Pod와 다른 노드/토폴로지에 배치합니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: web
            topologyKey: kubernetes.io/hostname  # 각 노드에 하나씩
      containers:
      - name: web
        image: nginx:1.25
```

### Topology Key

| topologyKey | 설명 |
|-------------|------|
| kubernetes.io/hostname | 같은/다른 노드 |
| topology.kubernetes.io/zone | 같은/다른 가용 영역 |
| topology.kubernetes.io/region | 같은/다른 리전 |

### Preferred Pod Anti-Affinity

```yaml
spec:
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              app: web
          topologyKey: topology.kubernetes.io/zone
```

### 실전 예시: Redis 클러스터

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
spec:
  replicas: 3
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      affinity:
        # 같은 앱끼리 다른 노드에 배치
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: redis
            topologyKey: kubernetes.io/hostname
        # 가능하면 다른 Zone에도 분산
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 50
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: redis
              topologyKey: topology.kubernetes.io/zone
      containers:
      - name: redis
        image: redis:7
```

---

## Taint와 Toleration

노드가 특정 Pod를 거부하도록 하는 메커니즘입니다.

### 개념

```
Taint (노드)     + Toleration (Pod)   = 스케줄링 허용
  (오염)          + (허용/내성)        = 배치 가능

Taint (노드)     + No Toleration      = 스케줄링 거부
  (오염)          + (허용 없음)        = 배치 불가
```

### Taint 추가/제거

```bash
# Taint 추가
kubectl taint nodes node1 key=value:NoSchedule

# Taint 제거
kubectl taint nodes node1 key=value:NoSchedule-

# 노드 Taint 확인
kubectl describe node node1 | grep Taint
```

### Taint Effect

| Effect | 설명 |
|--------|------|
| NoSchedule | 새로운 Pod 스케줄링 거부 (기존 Pod 유지) |
| PreferNoSchedule | 가능하면 스케줄링 회피 (소프트) |
| NoExecute | 새 Pod 거부 + 기존 Pod도 제거 |

### Toleration 정의

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: tolerant-pod
spec:
  tolerations:
  # 정확히 일치
  - key: "key1"
    operator: "Equal"
    value: "value1"
    effect: "NoSchedule"

  # 키만 일치 (값 무시)
  - key: "key2"
    operator: "Exists"
    effect: "NoExecute"
    tolerationSeconds: 3600  # NoExecute 시 3600초 후 제거

  # 모든 Taint 허용
  - operator: "Exists"
  containers:
  - name: app
    image: myapp:1.0
```

### 일반적인 사용 사례

#### 1. 전용 노드

```bash
# 특정 팀 전용 노드
kubectl taint nodes node1 dedicated=team-a:NoSchedule
```

```yaml
# 해당 팀의 Pod
tolerations:
- key: "dedicated"
  operator: "Equal"
  value: "team-a"
  effect: "NoSchedule"
```

#### 2. GPU 노드

```bash
kubectl taint nodes gpu-node1 nvidia.com/gpu=:NoSchedule
```

```yaml
tolerations:
- key: "nvidia.com/gpu"
  operator: "Exists"
  effect: "NoSchedule"
```

#### 3. Control Plane 노드

기본적으로 Control Plane 노드에는 다음 Taint가 있습니다:

```
node-role.kubernetes.io/control-plane:NoSchedule
```

```yaml
# Control Plane에서 실행하려면
tolerations:
- key: "node-role.kubernetes.io/control-plane"
  operator: "Exists"
  effect: "NoSchedule"
```

### NoExecute와 tolerationSeconds

```yaml
tolerations:
- key: "node.kubernetes.io/unreachable"
  operator: "Exists"
  effect: "NoExecute"
  tolerationSeconds: 300  # 노드 비정상 시 300초 후 제거
```

### 자동 추가되는 Toleration

DaemonSet Pod에 자동 추가:

| Taint | Effect |
|-------|--------|
| node.kubernetes.io/not-ready | NoExecute |
| node.kubernetes.io/unreachable | NoExecute |
| node.kubernetes.io/disk-pressure | NoSchedule |
| node.kubernetes.io/memory-pressure | NoSchedule |
| node.kubernetes.io/unschedulable | NoSchedule |
| node.kubernetes.io/network-unavailable | NoSchedule |

---

## Topology Spread Constraints

Pod를 토폴로지 도메인 (Zone, 노드 등)에 균등하게 분산시킵니다.

### 기본 구조

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: spread-pod
spec:
  topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        app: web
  containers:
  - name: app
    image: myapp:1.0
```

### 주요 필드

| 필드 | 설명 |
|------|------|
| maxSkew | 최대 불균형 정도 (1 = 완전 균등) |
| topologyKey | 분산 기준 (zone, hostname 등) |
| whenUnsatisfiable | 제약 불만족 시 동작 |
| labelSelector | 대상 Pod 선택 |

### whenUnsatisfiable

| 값 | 설명 |
|----|------|
| DoNotSchedule | 조건 만족 못하면 스케줄링 안 함 |
| ScheduleAnyway | 가능한 균등하게, 불가능해도 스케줄링 |

### Zone + Node 분산

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 6
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      topologySpreadConstraints:
      # Zone 레벨 분산
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule
        labelSelector:
          matchLabels:
            app: web
      # Node 레벨 분산
      - maxSkew: 1
        topologyKey: kubernetes.io/hostname
        whenUnsatisfiable: ScheduleAnyway
        labelSelector:
          matchLabels:
            app: web
      containers:
      - name: web
        image: nginx:1.25
```

### maxSkew 이해

```
Zone A: 3 Pods
Zone B: 2 Pods
Zone C: 1 Pod

maxSkew: 1 → Zone 간 차이가 1 이하여야 함
→ 불만족 (A-C = 2)

maxSkew: 2 → Zone 간 차이가 2 이하여야 함
→ 만족 (A-C = 2)
```

---

## 스케줄링 우선순위

### 조합 규칙

```yaml
spec:
  # 1. nodeSelector와 nodeAffinity 모두 지정 시
  #    둘 다 만족해야 함
  nodeSelector:
    disktype: ssd
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: zone
            operator: In
            values:
            - zone-a

  # 2. 여러 nodeSelectorTerms는 OR 조건
  # 3. 한 nodeSelectorTerms 내 matchExpressions는 AND 조건
```

### 스케줄링 결정 순서

```
1. 노드 필터링 (Filtering)
   - nodeSelector
   - nodeAffinity (required)
   - Taint/Toleration
   - 리소스 요구사항

2. 노드 점수 계산 (Scoring)
   - nodeAffinity (preferred)
   - podAffinity/podAntiAffinity (preferred)
   - Topology Spread (when ScheduleAnyway)

3. 최고 점수 노드 선택
```

---

## nodeName (직접 지정)

특정 노드에 직접 스케줄링 (스케줄러 우회)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: fixed-node-pod
spec:
  nodeName: node1
  containers:
  - name: app
    image: myapp:1.0
```

** 주의:**
- 스케줄러를 우회하므로 권장되지 않음
- 노드가 없거나 리소스 부족해도 스케줄링 시도

---

## 스케줄링 프로파일

스케줄러 동작을 커스터마이징합니다.

### 기본 프로파일 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-scheduler
spec:
  schedulerName: my-scheduler  # 커스텀 스케줄러 지정
  containers:
  - name: app
    image: myapp:1.0
```

---

## 실전 패턴

### 1. 고가용성 배포

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ha-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ha-app
  template:
    metadata:
      labels:
        app: ha-app
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: ha-app
            topologyKey: kubernetes.io/hostname
      topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: ScheduleAnyway
        labelSelector:
          matchLabels:
            app: ha-app
      containers:
      - name: app
        image: myapp:1.0
```

### 2. 워크로드 격리

```yaml
# 프로덕션 전용 노드
# kubectl taint nodes prod-node1 environment=production:NoSchedule
# kubectl label nodes prod-node1 environment=production

apiVersion: v1
kind: Pod
metadata:
  name: production-pod
spec:
  nodeSelector:
    environment: production
  tolerations:
  - key: "environment"
    operator: "Equal"
    value: "production"
    effect: "NoSchedule"
  containers:
  - name: app
    image: myapp:1.0
```

### 3. 데이터 지역성

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: data-local-pod
spec:
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchLabels:
            app: database
        topologyKey: kubernetes.io/hostname
  containers:
  - name: app
    image: myapp:1.0
```

---

## 참고 자료

- [Assigning Pods to Nodes](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/)
- [Taints and Tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)
- [Topology Spread Constraints](https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/)
- [Scheduler Configuration](https://kubernetes.io/docs/reference/scheduling/config/)
