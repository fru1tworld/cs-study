# Kubernetes 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://kubernetes.io/docs/

## 개요

Kubernetes(K8s)는 컨테이너화된 워크로드와 서비스를 관리하기 위한 이식 가능하고 확장 가능한 오픈 소스 플랫폼입니다. 선언적 구성과 자동화를 촉진합니다.

---

## 문서 목록

### 아키텍처

- [아키텍처](./architecture.md) - Control Plane, Node 컴포넌트, 클러스터 구조

### 워크로드

- [워크로드 리소스](./workloads.md) - Pod, Deployment, ReplicaSet, StatefulSet, DaemonSet, Job, CronJob

### 네트워킹

- [네트워킹](./networking.md) - Service, Ingress, NetworkPolicy, DNS

### 스토리지

- [스토리지](./storage.md) - Volume, PersistentVolume, PersistentVolumeClaim, StorageClass

### 설정 관리

- [설정 관리](./configuration.md) - ConfigMap, Secret

### 보안

- [보안](./security.md) - RBAC, ServiceAccount, Pod Security Standards, Security Context

### 스케줄링

- [스케줄링](./scheduling.md) - nodeSelector, Node Affinity, Pod Affinity/Anti-Affinity, Taint/Toleration, Topology Spread Constraints

### 오토스케일링

- [오토스케일링](./autoscaling.md) - HPA, VPA, Cluster Autoscaler, KEDA

### 패키지 관리

- [Helm](./helm.md) - Chart, Repository, Release, 템플릿, 의존성 관리

### 도구

- [kubectl 명령어](./kubectl.md) - 클러스터 관리를 위한 CLI 레퍼런스

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                       Control Plane                              │
│  ┌────────────┐ ┌────────┐ ┌────────────┐ ┌─────────────────┐  │
│  │ API Server │ │  etcd  │ │ Scheduler  │ │ Controller Mgr  │  │
│  └────────────┘ └────────┘ └────────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│    Node 1      │  │    Node 2      │  │    Node 3      │
│  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │
│  │ kubelet  │  │  │  │ kubelet  │  │  │  │ kubelet  │  │
│  │kube-proxy│  │  │  │kube-proxy│  │  │  │kube-proxy│  │
│  │Container │  │  │  │Container │  │  │  │Container │  │
│  │ Runtime  │  │  │  │ Runtime  │  │  │  │ Runtime  │  │
│  │  Pods    │  │  │  │  Pods    │  │  │  │  Pods    │  │
│  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │
└────────────────┘  └────────────────┘  └────────────────┘
```

---

## 핵심 리소스 요약

### 워크로드 리소스

| 리소스 | 용도 |
|--------|------|
| Pod | 가장 작은 배포 단위 |
| Deployment | 무상태 애플리케이션 관리 |
| StatefulSet | 상태 있는 애플리케이션 관리 |
| DaemonSet | 모든 노드에서 Pod 실행 |
| Job | 일회성 작업 |
| CronJob | 정기 작업 |

### 네트워킹 리소스

| 리소스 | 용도 |
|--------|------|
| Service | Pod를 네트워크로 노출 |
| Ingress | HTTP/HTTPS 라우팅 |
| NetworkPolicy | 네트워크 트래픽 제어 |

### 스토리지 리소스

| 리소스 | 용도 |
|--------|------|
| PersistentVolume | 클러스터 스토리지 리소스 |
| PersistentVolumeClaim | 스토리지 요청 |
| StorageClass | 스토리지 프로비저닝 정책 |

### 설정 리소스

| 리소스 | 용도 |
|--------|------|
| ConfigMap | 비민감 설정 데이터 |
| Secret | 민감한 정보 |

### 보안 리소스

| 리소스 | 용도 |
|--------|------|
| Role/ClusterRole | 권한 정의 |
| RoleBinding/ClusterRoleBinding | 권한 바인딩 |
| ServiceAccount | Pod 신원 |

---

## 자주 사용하는 kubectl 명령어

```bash
# 리소스 조회
kubectl get pods
kubectl get deployments
kubectl get services
kubectl get all

# 상세 정보
kubectl describe pod <pod-name>
kubectl describe deployment <deployment-name>

# 로그 확인
kubectl logs <pod-name>
kubectl logs -f <pod-name>  # follow

# 실행 중인 컨테이너 접속
kubectl exec -it <pod-name> -- /bin/bash

# 리소스 적용/삭제
kubectl apply -f deployment.yaml
kubectl delete -f deployment.yaml

# 스케일링
kubectl scale deployment <name> --replicas=5

# 포트 포워딩
kubectl port-forward pod/<pod-name> 8080:80
kubectl port-forward svc/<service-name> 8080:80

# 롤아웃 관리
kubectl rollout status deployment/<name>
kubectl rollout history deployment/<name>
kubectl rollout undo deployment/<name>
```

---

## 참고 자료

### 공식 문서

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kubernetes API Reference](https://kubernetes.io/docs/reference/kubernetes-api/)
- [kubectl Reference](https://kubernetes.io/docs/reference/kubectl/)

### Helm

- [Helm Documentation](https://helm.sh/docs/)
- [Artifact Hub](https://artifacthub.io/)

### 관리형 Kubernetes

- [Amazon EKS](https://docs.aws.amazon.com/eks/)
- [Google GKE](https://cloud.google.com/kubernetes-engine/docs)
- [Azure AKS](https://docs.microsoft.com/azure/aks/)

### 학습 리소스

- [Kubernetes Basics](https://kubernetes.io/docs/tutorials/kubernetes-basics/)
- [Kubernetes the Hard Way](https://github.com/kelseyhightower/kubernetes-the-hard-way)
