# Kubernetes 워크로드 리소스

> 공식 문서: https://kubernetes.io/docs/concepts/workloads/

## 개요

워크로드는 Kubernetes에서 실행되는 애플리케이션입니다. Pod 내에서 컨테이너 집합으로 실행되며, 워크로드 리소스를 통해 Pod를 관리합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    워크로드 리소스 계층                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Deployment  │  │ StatefulSet │  │  DaemonSet  │         │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
│         │                │                │                │
│         ▼                ▼                ▼                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ ReplicaSet  │  │    Pods     │  │    Pods     │         │
│  └──────┬──────┘  │  (Ordered)  │  │ (Per Node)  │         │
│         │         └─────────────┘  └─────────────┘         │
│         ▼                                                   │
│  ┌─────────────┐                                           │
│  │    Pods     │      ┌───────────┐  ┌─────────────┐       │
│  └─────────────┘      │    Job    │  │   CronJob   │       │
│                       └─────┬─────┘  └──────┬──────┘       │
│                             │               │              │
│                             ▼               ▼              │
│                       ┌─────────────┐ ┌─────────────┐      │
│                       │    Pods     │ │    Jobs     │      │
│                       │ (완료까지)   │ │  (스케줄)   │      │
│                       └─────────────┘ └─────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## Pod

Pod는 Kubernetes에서 배포 가능한 가장 작은 단위입니다.

### 특징

| 특징 | 설명 |
|------|------|
| 컨테이너 그룹 | 하나 이상의 컨테이너를 포함 |
| 공유 리소스 | 스토리지, 네트워크(IP 주소) 공유 |
| 일시적 | 생성되고 삭제되는 임시 리소스 |
| 고유 IP | 각 Pod는 클러스터 내 고유 IP 할당 |

### Pod 정의

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
    environment: production
spec:
  containers:
  - name: nginx
    image: nginx:1.25
    ports:
    - containerPort: 80
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
    livenessProbe:
      httpGet:
        path: /healthz
        port: 80
      initialDelaySeconds: 3
      periodSeconds: 3
    readinessProbe:
      httpGet:
        path: /ready
        port: 80
      initialDelaySeconds: 3
      periodSeconds: 3
    volumeMounts:
    - name: config
      mountPath: /etc/nginx/conf.d
  volumes:
  - name: config
    configMap:
      name: nginx-config
```

### Pod 생명주기

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌───────────┐
│ Pending │───▶│ Running │───▶│Succeeded│ or │  Failed   │
└─────────┘    └─────────┘    └─────────┘    └───────────┘
                    │
                    ▼
              ┌─────────┐
              │ Unknown │
              └─────────┘
```

| 단계 | 설명 |
|------|------|
| Pending | Pod가 승인되었지만 컨테이너가 아직 준비되지 않음 |
| Running | Pod가 노드에 바인딩되고 최소 하나의 컨테이너가 실행 중 |
| Succeeded | 모든 컨테이너가 성공적으로 종료됨 |
| Failed | 모든 컨테이너가 종료되었고 최소 하나가 실패 |
| Unknown | Pod 상태를 확인할 수 없음 |

### Init Containers

Pod의 앱 컨테이너가 시작되기 전에 실행되는 특수 컨테이너입니다.

```yaml
spec:
  initContainers:
  - name: init-myservice
    image: busybox:1.28
    command: ['sh', '-c', 'until nslookup myservice; do echo waiting; sleep 2; done']
  - name: init-mydb
    image: busybox:1.28
    command: ['sh', '-c', 'until nslookup mydb; do echo waiting; sleep 2; done']
  containers:
  - name: myapp
    image: myapp:1.0
```

**특징:**
- 순차적으로 실행 (이전 완료 후 다음 시작)
- 모든 init container가 완료되어야 앱 컨테이너 시작
- 실패 시 Pod가 다시 시작됨

### Sidecar Containers

주 애플리케이션 컨테이너와 함께 실행되는 보조 컨테이너입니다.

```yaml
spec:
  initContainers:
  - name: log-collector
    image: fluentd
    restartPolicy: Always  # Sidecar로 동작
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
  containers:
  - name: myapp
    image: myapp:1.0
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
```

**일반적인 Sidecar 패턴:**
- 로그 수집기 (Fluentd, Filebeat)
- 프록시 (Envoy, Istio sidecar)
- 설정 동기화

### Probe 종류

| Probe | 용도 | 실패 시 동작 |
|-------|------|-------------|
| livenessProbe | 컨테이너 실행 여부 확인 | 컨테이너 재시작 |
| readinessProbe | 트래픽 수신 준비 여부 | Service 엔드포인트에서 제외 |
| startupProbe | 애플리케이션 시작 완료 확인 | 다른 probe 비활성화 |

```yaml
# Probe 유형
livenessProbe:
  # HTTP GET
  httpGet:
    path: /healthz
    port: 8080
  # TCP Socket
  tcpSocket:
    port: 8080
  # Exec
  exec:
    command:
    - cat
    - /tmp/healthy
```

---

## Deployment

상태를 유지하지 않는 애플리케이션 워크로드에 사용되는 가장 일반적인 리소스입니다.

### 기본 구조

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.25
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "64Mi"
            cpu: "250m"
          limits:
            memory: "128Mi"
            cpu: "500m"
```

### 배포 전략

| 전략 | 설명 | 사용 사례 |
|------|------|----------|
| RollingUpdate | 점진적으로 Pod 교체 (기본값) | 무중단 배포 |
| Recreate | 모든 Pod 종료 후 새로 생성 | 다운타임 허용 가능한 경우 |

#### RollingUpdate 설정

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 25%        # 원하는 Pod 수 대비 최대 추가 Pod
    maxUnavailable: 25%  # 원하는 Pod 수 대비 최대 비가용 Pod
```

**동작 예시 (replicas: 4):**
- maxSurge: 1 → 최대 5개 Pod 동시 실행
- maxUnavailable: 0 → 항상 4개 이상 가용

### 롤아웃 관리

```bash
# 배포 상태 확인
kubectl rollout status deployment/nginx-deployment

# 롤아웃 히스토리
kubectl rollout history deployment/nginx-deployment

# 이전 버전으로 롤백
kubectl rollout undo deployment/nginx-deployment

# 특정 리비전으로 롤백
kubectl rollout undo deployment/nginx-deployment --to-revision=2

# 롤아웃 일시 중지/재개
kubectl rollout pause deployment/nginx-deployment
kubectl rollout resume deployment/nginx-deployment
```

### 스케일링

```bash
# 수동 스케일링
kubectl scale deployment/nginx-deployment --replicas=5

# 조건부 스케일링
kubectl scale deployment/nginx-deployment --replicas=3 --current-replicas=5
```

---

## ReplicaSet

지정된 수의 Pod 복제본이 항상 실행되도록 보장합니다.

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-replicaset
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.25
```

**중요:** 일반적으로 ReplicaSet을 직접 생성하지 않고 Deployment를 사용합니다.

---

## StatefulSet

상태를 유지하는 애플리케이션을 관리합니다.

### 특징

| 특징 | 설명 |
|------|------|
| 안정적인 네트워크 ID | 순서가 있는 고유한 Pod 이름 (web-0, web-1, web-2) |
| 안정적인 스토리지 | Pod별 전용 PersistentVolume |
| 순서 보장 | 배포/삭제/스케일링 시 순차적 처리 |
| Headless Service 필수 | 네트워크 ID 관리용 |

### 기본 구조

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-headless
spec:
  clusterIP: None  # Headless Service
  selector:
    app: nginx
  ports:
  - port: 80
    name: web
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: "nginx-headless"
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.25
        ports:
        - containerPort: 80
          name: web
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "standard"
      resources:
        requests:
          storage: 1Gi
```

### Pod Identity

```
Pod 이름: $(statefulset name)-$(ordinal)
DNS: $(pod name).$(service name).$(namespace).svc.cluster.local

예시:
web-0.nginx-headless.default.svc.cluster.local
web-1.nginx-headless.default.svc.cluster.local
web-2.nginx-headless.default.svc.cluster.local
```

### 배포/삭제 순서

**배포 (Scale Up):**
```
web-0 (Ready) → web-1 (Ready) → web-2 (Ready)
```

**삭제 (Scale Down):**
```
web-2 (Terminated) → web-1 (Terminated) → web-0 (Terminated)
```

### 업데이트 전략

```yaml
spec:
  updateStrategy:
    type: RollingUpdate
    rollingUpdate:
      partition: 2  # ordinal >= 2인 Pod만 업데이트
```

### 사용 사례

- 데이터베이스 클러스터 (MySQL, PostgreSQL, MongoDB)
- 메시지 큐 (Kafka, RabbitMQ)
- 분산 시스템 (ZooKeeper, etcd, Consul)

---

## DaemonSet

모든 노드(또는 일부)에서 Pod 복사본을 실행합니다.

### 기본 구조

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: kube-system
  labels:
    app: fluentd
spec:
  selector:
    matchLabels:
      app: fluentd
  template:
    metadata:
      labels:
        app: fluentd
    spec:
      tolerations:
      - key: node-role.kubernetes.io/control-plane
        effect: NoSchedule
      containers:
      - name: fluentd
        image: fluentd:v1.14
        resources:
          limits:
            memory: 200Mi
          requests:
            cpu: 100m
            memory: 200Mi
        volumeMounts:
        - name: varlog
          mountPath: /var/log
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
```

### 노드 선택

```yaml
spec:
  template:
    spec:
      # 방법 1: nodeSelector
      nodeSelector:
        disk: ssd

      # 방법 2: nodeAffinity
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: kubernetes.io/os
                operator: In
                values:
                - linux
```

### 자동 추가되는 Tolerations

DaemonSet Pod에는 다음 Toleration이 자동으로 추가됩니다:

| Toleration | Effect |
|------------|--------|
| node.kubernetes.io/not-ready | NoExecute |
| node.kubernetes.io/unreachable | NoExecute |
| node.kubernetes.io/disk-pressure | NoSchedule |
| node.kubernetes.io/memory-pressure | NoSchedule |
| node.kubernetes.io/unschedulable | NoSchedule |

### 사용 사례

- 로그 수집 (Fluentd, Filebeat)
- 모니터링 에이전트 (Prometheus Node Exporter, Datadog Agent)
- 네트워크 플러그인 (Calico, Flannel)
- 스토리지 데몬 (Ceph, GlusterFS)

---

## Job

완료될 때까지 실행되는 일회성 작업입니다.

### 기본 구조

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: pi-calculation
spec:
  completions: 1
  parallelism: 1
  backoffLimit: 4
  activeDeadlineSeconds: 100
  template:
    spec:
      containers:
      - name: pi
        image: perl:5.34
        command: ["perl", "-Mbignum=bpi", "-wle", "print bpi(2000)"]
      restartPolicy: Never
```

### 작업 유형

| 유형 | completions | parallelism | 설명 |
|------|-------------|-------------|------|
| 비병렬 | 1 | 1 | 단일 Pod 실행 |
| 고정 완료 개수 | n | 1-n | n개의 성공적인 완료 필요 |
| 작업 큐 | 미지정 | n | Pod들이 상호 조율 |

### 병렬 처리 예시

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: parallel-job
spec:
  completions: 10      # 10개의 성공적인 완료 필요
  parallelism: 3       # 동시에 3개 Pod 실행
  completionMode: Indexed  # 각 Pod에 인덱스 할당
  template:
    spec:
      containers:
      - name: worker
        image: busybox
        command:
        - sh
        - -c
        - echo "Processing index $JOB_COMPLETION_INDEX"
      restartPolicy: Never
```

### 실패 처리

```yaml
spec:
  backoffLimit: 4              # 최대 재시도 횟수
  activeDeadlineSeconds: 600   # 전체 실행 시간 제한
  ttlSecondsAfterFinished: 100 # 완료 후 자동 삭제 시간
  template:
    spec:
      restartPolicy: Never     # 또는 OnFailure
```

| restartPolicy | 동작 |
|---------------|------|
| Never | Pod 실패 시 새 Pod 생성 |
| OnFailure | 같은 Pod에서 컨테이너 재시작 |

---

## CronJob

정기적인 일정에 따라 Job을 생성합니다.

### 기본 구조

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: backup-job
spec:
  schedule: "0 2 * * *"  # 매일 오전 2시
  timeZone: "Asia/Seoul"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1
  startingDeadlineSeconds: 200
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: backup-tool:v1
            command: ["/bin/sh", "-c", "backup.sh"]
          restartPolicy: OnFailure
```

### 스케줄 문법

```
┌───────────── 분 (0 - 59)
│ ┌───────────── 시간 (0 - 23)
│ │ ┌───────────── 일 (1 - 31)
│ │ │ ┌───────────── 월 (1 - 12)
│ │ │ │ ┌───────────── 요일 (0 - 6, 일요일 = 0)
│ │ │ │ │
* * * * *
```

**예시:**

| 스케줄 | 설명 |
|--------|------|
| `*/15 * * * *` | 매 15분마다 |
| `0 * * * *` | 매시간 정각 |
| `0 0 * * *` | 매일 자정 |
| `0 0 * * 0` | 매주 일요일 자정 |
| `0 0 1 * *` | 매월 1일 자정 |

**매크로:**

| 매크로 | 동등한 표현 |
|--------|-------------|
| @yearly | 0 0 1 1 * |
| @monthly | 0 0 1 * * |
| @weekly | 0 0 * * 0 |
| @daily | 0 0 * * * |
| @hourly | 0 * * * * |

### 동시 실행 정책

| 정책 | 설명 |
|------|------|
| Allow | 동시 실행 허용 (기본값) |
| Forbid | 이전 Job 실행 중이면 스킵 |
| Replace | 이전 Job을 취소하고 새 Job 실행 |

### 주의사항

- 100회 이상 누락된 스케줄은 자동 스킵
- Job 이름은 최대 52자 (자동으로 11자 추가됨)
- 멱등한 작업으로 설계 필요 (중복 실행 가능성)

---

## 워크로드 리소스 비교

| 리소스 | 용도 | Pod 수 | 상태 |
|--------|------|--------|------|
| Deployment | 무상태 애플리케이션 | 가변 (replicas) | 상태 없음 |
| StatefulSet | 상태 유지 애플리케이션 | 가변 (replicas) | 순서 있는 ID |
| DaemonSet | 노드별 데몬 | 노드 수 | 노드당 1개 |
| Job | 일회성 작업 | completions | 완료까지 |
| CronJob | 정기 작업 | Job별 | 스케줄 기반 |

---

## 참고 자료

- [Pods](https://kubernetes.io/docs/concepts/workloads/pods/)
- [Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [StatefulSets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)
- [DaemonSets](https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/)
- [Jobs](https://kubernetes.io/docs/concepts/workloads/controllers/job/)
- [CronJobs](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)
