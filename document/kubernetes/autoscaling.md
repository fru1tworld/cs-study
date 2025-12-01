# Kubernetes 오토스케일링

> 공식 문서: https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/

## 개요

Kubernetes는 워크로드 수요에 따라 자동으로 리소스를 조정하는 다양한 오토스케일링 메커니즘을 제공합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    오토스케일링 유형                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              Horizontal Pod Autoscaler (HPA)               │ │
│  │                    Pod 수 조정                              │ │
│  │                                                            │ │
│  │     부하 ↑  →  Pod 수 증가 (Scale Out)                     │ │
│  │     부하 ↓  →  Pod 수 감소 (Scale In)                      │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │               Vertical Pod Autoscaler (VPA)                │ │
│  │                  Pod 리소스 조정                            │ │
│  │                                                            │ │
│  │     부하 ↑  →  CPU/Memory 증가 (Scale Up)                  │ │
│  │     부하 ↓  →  CPU/Memory 감소 (Scale Down)                │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                 Cluster Autoscaler                         │ │
│  │                   노드 수 조정                              │ │
│  │                                                            │ │
│  │     Pod Pending  →  노드 추가                              │ │
│  │     노드 유휴    →  노드 제거                              │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Horizontal Pod Autoscaler (HPA)

관측된 메트릭을 기반으로 Pod 복제본 수를 자동으로 조정합니다.

### 작동 원리

```
┌────────────────────────────────────────────────────────────────┐
│                      HPA 제어 루프                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   ┌──────────────┐                                            │
│   │ Metrics API  │  (metrics.k8s.io)                          │
│   │  - CPU       │                                            │
│   │  - Memory    │                                            │
│   └──────┬───────┘                                            │
│          │                                                     │
│          ▼                                                     │
│   ┌──────────────┐      ┌───────────────┐                     │
│   │     HPA      │─────▶│ Deployment    │                     │
│   │  Controller  │      │ StatefulSet   │                     │
│   └──────────────┘      │ ReplicaSet    │                     │
│          │              └───────────────┘                     │
│          │                     │                              │
│          │              ┌──────▼──────┐                       │
│          └─────────────▶│   Pods      │                       │
│           replica 수 조정│ (증가/감소) │                       │
│                         └─────────────┘                       │
└────────────────────────────────────────────────────────────────┘
```

### 기본 HPA (CPU)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # CPU 사용률 70% 목표
```

### kubectl로 HPA 생성

```bash
# 간단한 CPU 기반 HPA
kubectl autoscale deployment my-app \
  --cpu-percent=70 \
  --min=2 \
  --max=10

# HPA 상태 확인
kubectl get hpa

# HPA 상세 정보
kubectl describe hpa my-app
```

### 메트릭 유형

#### 1. Resource 메트릭 (CPU, Memory)

```yaml
metrics:
- type: Resource
  resource:
    name: cpu
    target:
      type: Utilization
      averageUtilization: 70
- type: Resource
  resource:
    name: memory
    target:
      type: AverageValue
      averageValue: 500Mi
```

#### 2. Pod 메트릭

```yaml
metrics:
- type: Pods
  pods:
    metric:
      name: packets-per-second
    target:
      type: AverageValue
      averageValue: 1k
```

#### 3. Object 메트릭

```yaml
metrics:
- type: Object
  object:
    metric:
      name: requests-per-second
    describedObject:
      apiVersion: networking.k8s.io/v1
      kind: Ingress
      name: main-route
    target:
      type: Value
      value: 2k
```

#### 4. External 메트릭

```yaml
metrics:
- type: External
  external:
    metric:
      name: queue_messages_ready
      selector:
        matchLabels:
          queue: worker_tasks
    target:
      type: AverageValue
      averageValue: 30
```

### 복합 메트릭

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: multi-metric-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2
  maxReplicas: 20
  metrics:
  # CPU 기반
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  # Memory 기반
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  # 커스텀 메트릭
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: 100
```

**다중 메트릭 동작:** 각 메트릭에서 계산된 복제본 수 중 가장 큰 값을 선택합니다.

### 스케일링 알고리즘

```
desiredReplicas = ceil(currentReplicas × (currentMetric / desiredMetric))
```

**예시:**
- 현재 복제본: 3
- 현재 CPU: 90%
- 목표 CPU: 60%
- 계산: ceil(3 × (90/60)) = ceil(4.5) = 5

### 스케일링 동작 제어

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: controlled-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2
  maxReplicas: 20
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5분 안정화 기간
      policies:
      - type: Percent
        value: 10         # 최대 10%씩 감소
        periodSeconds: 60
      - type: Pods
        value: 2          # 또는 최대 2개씩 감소
        periodSeconds: 60
      selectPolicy: Min   # 더 보수적인 정책 선택
    scaleUp:
      stabilizationWindowSeconds: 0  # 즉시 확장
      policies:
      - type: Percent
        value: 100        # 최대 100% 증가
        periodSeconds: 15
      - type: Pods
        value: 4          # 또는 최대 4개씩 증가
        periodSeconds: 15
      selectPolicy: Max   # 더 공격적인 정책 선택
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### selectPolicy

| 값 | 설명 |
|----|------|
| Max | 가장 많이 변화하는 정책 선택 (공격적) |
| Min | 가장 적게 변화하는 정책 선택 (보수적) |
| Disabled | 해당 방향 스케일링 비활성화 |

### 스케일링 비활성화

```yaml
behavior:
  scaleDown:
    selectPolicy: Disabled  # Scale Down 비활성화
```

### HPA 요구사항

1. **Metrics Server 설치**

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 확인
kubectl top nodes
kubectl top pods
```

2. **Pod 리소스 요청 정의**

```yaml
spec:
  containers:
  - name: app
    resources:
      requests:
        cpu: "100m"     # HPA가 비율 계산에 사용
        memory: "128Mi"
      limits:
        cpu: "500m"
        memory: "512Mi"
```

### HPA 상태 확인

```bash
# 현재 상태
kubectl get hpa
NAME      REFERENCE        TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
my-app    Deployment/app   45%/70%   2         10        3          5m

# 상세 이벤트
kubectl describe hpa my-app

# 메트릭 확인
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq
```

---

## Vertical Pod Autoscaler (VPA)

Pod의 CPU와 메모리 요청/제한을 자동으로 조정합니다.

### 설치

```bash
# VPA 설치 (클러스터에 별도 설치 필요)
git clone https://github.com/kubernetes/autoscaler.git
cd autoscaler/vertical-pod-autoscaler/
./hack/vpa-up.sh
```

### 기본 VPA

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: my-app-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: "*"
      minAllowed:
        cpu: 100m
        memory: 50Mi
      maxAllowed:
        cpu: 2
        memory: 2Gi
      controlledResources: ["cpu", "memory"]
```

### updateMode

| 모드 | 설명 |
|------|------|
| Off | 권장 사항만 제공 (변경 안 함) |
| Initial | Pod 생성 시에만 적용 |
| Recreate | 필요시 Pod 재시작하여 적용 |
| Auto | Recreate와 동일 (기본값) |

### VPA 권장 사항 확인

```bash
kubectl describe vpa my-app-vpa
```

```
Recommendation:
  Container Recommendations:
    Container Name: app
    Lower Bound:
      Cpu:     25m
      Memory:  262144k
    Target:
      Cpu:     100m
      Memory:  500Mi
    Upper Bound:
      Cpu:     1
      Memory:  2Gi
```

### VPA와 HPA 함께 사용

**주의:** 동일한 메트릭(CPU/Memory)으로 VPA와 HPA를 함께 사용하면 충돌이 발생합니다.

**권장 조합:**
- HPA: CPU/Memory 외 커스텀 메트릭
- VPA: CPU/Memory

```yaml
# HPA - 커스텀 메트릭 사용
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  metrics:
  - type: External
    external:
      metric:
        name: queue_messages
      target:
        type: AverageValue
        averageValue: 50
---
# VPA - CPU/Memory 사용
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  updatePolicy:
    updateMode: Auto
```

---

## Cluster Autoscaler

클러스터의 노드 수를 자동으로 조정합니다.

### 작동 원리

```
┌────────────────────────────────────────────────────────────────┐
│                  Cluster Autoscaler 동작                        │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Scale Up (노드 추가):                                         │
│  ┌───────────┐                                                │
│  │ Pending   │  리소스 부족으로 스케줄링 불가                    │
│  │   Pod     │                                                │
│  └─────┬─────┘                                                │
│        │                                                      │
│        ▼                                                      │
│  ┌───────────────────────────────────────────────────┐       │
│  │ Cluster Autoscaler가 Node Group 확장              │       │
│  │ → 클라우드 API로 새 노드 생성                      │       │
│  └───────────────────────────────────────────────────┘       │
│                                                                │
│  Scale Down (노드 제거):                                       │
│  ┌───────────┐                                                │
│  │ 노드 사용률│  사용률이 낮은 노드 감지                         │
│  │   < 50%   │  (기본 임계값)                                  │
│  └─────┬─────┘                                                │
│        │                                                      │
│        ▼                                                      │
│  ┌───────────────────────────────────────────────────┐       │
│  │ Pod를 다른 노드로 이동 후 노드 삭제               │       │
│  └───────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────┘
```

### AWS EKS 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
    spec:
      serviceAccountName: cluster-autoscaler
      containers:
      - name: cluster-autoscaler
        image: registry.k8s.io/autoscaling/cluster-autoscaler:v1.29.0
        command:
        - ./cluster-autoscaler
        - --v=4
        - --stderrthreshold=info
        - --cloud-provider=aws
        - --skip-nodes-with-local-storage=false
        - --expander=least-waste
        - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/my-cluster
        - --balance-similar-node-groups
        - --skip-nodes-with-system-pods=false
```

### GKE 설정

GKE는 기본적으로 Cluster Autoscaler를 지원합니다:

```bash
# Node Pool에서 Autoscaling 활성화
gcloud container clusters update my-cluster \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=10 \
  --zone=us-central1-a
```

### 주요 설정

| 파라미터 | 설명 |
|----------|------|
| --scale-down-delay-after-add | 노드 추가 후 축소 대기 시간 (기본 10분) |
| --scale-down-delay-after-delete | 노드 삭제 후 다음 축소 대기 시간 |
| --scale-down-unneeded-time | 유휴 노드 감지 후 삭제까지 대기 시간 (기본 10분) |
| --scale-down-utilization-threshold | 축소 임계값 (기본 0.5 = 50%) |
| --max-node-provision-time | 노드 프로비저닝 최대 대기 시간 (기본 15분) |

### Expander 옵션

| Expander | 설명 |
|----------|------|
| random | 무작위 선택 |
| most-pods | 가장 많은 Pod 스케줄링 가능한 그룹 |
| least-waste | 리소스 낭비가 가장 적은 그룹 |
| price | 가장 저렴한 그룹 (클라우드) |
| priority | 우선순위 기반 |

### Scale Down 방지

특정 Pod가 있는 노드의 Scale Down을 방지:

```yaml
metadata:
  annotations:
    cluster-autoscaler.kubernetes.io/safe-to-evict: "false"
```

또는 PodDisruptionBudget 사용:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: critical-app
```

---

## KEDA (Kubernetes Event-driven Autoscaling)

이벤트 기반 오토스케일링을 위한 확장 솔루션입니다.

### 설치

```bash
helm repo add kedacore https://kedacore.github.io/charts
helm install keda kedacore/keda --namespace keda --create-namespace
```

### ScaledObject

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: rabbitmq-scaledobject
spec:
  scaleTargetRef:
    name: worker-deployment
  minReplicaCount: 1
  maxReplicaCount: 30
  triggers:
  - type: rabbitmq
    metadata:
      queueName: tasks
      host: amqp://guest:guest@rabbitmq.default:5672/
      queueLength: "10"  # 큐 길이 10당 1 Pod
```

### 다양한 트리거

```yaml
# Prometheus 트리거
triggers:
- type: prometheus
  metadata:
    serverAddress: http://prometheus:9090
    metricName: http_requests_total
    query: sum(rate(http_requests_total{app="my-app"}[2m]))
    threshold: "100"

# AWS SQS 트리거
triggers:
- type: aws-sqs-queue
  metadata:
    queueURL: https://sqs.us-east-1.amazonaws.com/123456789/my-queue
    queueLength: "5"
    awsRegion: us-east-1

# Kafka 트리거
triggers:
- type: kafka
  metadata:
    bootstrapServers: kafka.default:9092
    consumerGroup: my-group
    topic: my-topic
    lagThreshold: "100"
```

### ScaledJob

일회성 작업에 대한 오토스케일링:

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledJob
metadata:
  name: batch-job
spec:
  jobTargetRef:
    template:
      spec:
        containers:
        - name: worker
          image: worker:1.0
        restartPolicy: Never
  pollingInterval: 30
  maxReplicaCount: 100
  successfulJobsHistoryLimit: 5
  failedJobsHistoryLimit: 5
  triggers:
  - type: rabbitmq
    metadata:
      queueName: batch-tasks
      host: amqp://rabbitmq:5672/
      queueLength: "1"
```

---

## 오토스케일링 모범 사례

### 1. 리소스 요청 정확히 설정

```yaml
resources:
  requests:
    cpu: "100m"     # 실제 사용량 기반
    memory: "256Mi"
  limits:
    cpu: "500m"
    memory: "512Mi"
```

### 2. PodDisruptionBudget 설정

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-pdb
spec:
  minAvailable: 50%  # 최소 50% 가용성 유지
  selector:
    matchLabels:
      app: my-app
```

### 3. 적절한 스케일링 속도

```yaml
behavior:
  scaleUp:
    stabilizationWindowSeconds: 0
    policies:
    - type: Percent
      value: 100
      periodSeconds: 15
  scaleDown:
    stabilizationWindowSeconds: 300  # 5분 안정화
    policies:
    - type: Percent
      value: 10
      periodSeconds: 60
```

### 4. 알림 설정

```yaml
# Prometheus AlertManager 규칙
groups:
- name: hpa-alerts
  rules:
  - alert: HPAMaxReplicasReached
    expr: kube_horizontalpodautoscaler_status_current_replicas == kube_horizontalpodautoscaler_spec_max_replicas
    for: 5m
    annotations:
      summary: "HPA {{ $labels.horizontalpodautoscaler }} has reached max replicas"
```

### 5. 오토스케일링 조합

```
┌─────────────────────────────────────────────────────────────┐
│                    권장 조합                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HPA + Cluster Autoscaler                                  │
│  • HPA가 Pod 수 조정                                       │
│  • Cluster Autoscaler가 노드 수 조정                       │
│                                                             │
│  VPA (Initial mode) + HPA (custom metrics)                 │
│  • VPA가 초기 리소스 설정                                   │
│  • HPA가 커스텀 메트릭으로 Pod 수 조정                      │
│                                                             │
│  KEDA + Cluster Autoscaler                                 │
│  • KEDA가 이벤트 기반 스케일링                              │
│  • Cluster Autoscaler가 노드 수 조정                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 참고 자료

- [Horizontal Pod Autoscaler](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [Vertical Pod Autoscaler](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler)
- [Cluster Autoscaler](https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler)
- [KEDA](https://keda.sh/docs/)
