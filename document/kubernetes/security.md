# Kubernetes 보안

> 공식 문서: https://kubernetes.io/docs/concepts/security/

## 개요

Kubernetes 보안은 인증(Authentication), 인가(Authorization), 어드미션 컨트롤(Admission Control)의 3단계로 구성됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     API 요청 처리 흐름                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│     요청                                                        │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────┐                                               │
│  │   인증      │  "누구인가?"                                   │
│  │ (AuthN)    │  • X.509 인증서                                │
│  │            │  • Bearer Token                                │
│  │            │  • ServiceAccount Token                        │
│  └─────┬──────┘                                               │
│        │ 인증 성공                                              │
│        ▼                                                        │
│  ┌─────────────┐                                               │
│  │   인가      │  "무엇을 할 수 있는가?"                         │
│  │ (AuthZ)    │  • RBAC                                        │
│  │            │  • ABAC                                        │
│  │            │  • Webhook                                     │
│  └─────┬──────┘                                               │
│        │ 인가 성공                                              │
│        ▼                                                        │
│  ┌─────────────┐                                               │
│  │ 어드미션    │  "요청을 변경하거나 거부할까?"                   │
│  │ 컨트롤     │  • Mutating Webhooks                           │
│  │            │  • Validating Webhooks                         │
│  └─────┬──────┘                                               │
│        │ 승인                                                   │
│        ▼                                                        │
│     실행                                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## RBAC (Role-Based Access Control)

RBAC는 역할 기반으로 API 리소스 접근을 제어합니다.

### 핵심 개념

```
┌──────────────────────────────────────────────────────────────┐
│                      RBAC 구성 요소                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐           ┌─────────────────────────────┐  │
│  │    Role     │           │      RoleBinding            │  │
│  │ (네임스페이스)│◀──────────│    (역할과 주체 연결)        │  │
│  └─────────────┘           └─────────────────────────────┘  │
│                                       │                     │
│  ┌─────────────┐                      │                     │
│  │ ClusterRole │◀─────────────────────┤                     │
│  │  (클러스터)  │                      │                     │
│  └─────────────┘           ┌──────────┴──────────┐         │
│                            │                     │         │
│                     ┌──────▼─────┐      ┌───────▼──────┐   │
│                     │   User     │      │    Group     │   │
│                     └────────────┘      └──────────────┘   │
│                            │                               │
│                     ┌──────▼─────┐                         │
│                     │ServiceAcct │                         │
│                     └────────────┘                         │
└──────────────────────────────────────────────────────────────┘
```

### Role

네임스페이스 범위의 권한 집합을 정의합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: default
rules:
# Pod 읽기 권한
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "watch", "list"]

# Pod 로그 읽기
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]

# ConfigMap 읽기
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list"]
```

### ClusterRole

클러스터 전체 범위의 권한 집합을 정의합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: secret-reader
rules:
# 모든 네임스페이스의 Secret 읽기
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get", "watch", "list"]

# 노드 정보 읽기 (클러스터 리소스)
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list"]

# 비리소스 URL 접근
- nonResourceURLs: ["/healthz", "/healthz/*"]
  verbs: ["get"]
```

### API Groups

| API Group | 리소스 예시 |
|-----------|------------|
| "" (core) | pods, services, configmaps, secrets, nodes |
| apps | deployments, statefulsets, daemonsets, replicasets |
| batch | jobs, cronjobs |
| networking.k8s.io | ingresses, networkpolicies |
| rbac.authorization.k8s.io | roles, rolebindings, clusterroles |
| storage.k8s.io | storageclasses, volumeattachments |

### Verbs (동사)

| Verb | 설명 | HTTP 메서드 |
|------|------|------------|
| get | 단일 리소스 조회 | GET |
| list | 리소스 목록 조회 | GET |
| watch | 리소스 변경 감시 | GET (watch=true) |
| create | 리소스 생성 | POST |
| update | 리소스 전체 업데이트 | PUT |
| patch | 리소스 부분 업데이트 | PATCH |
| delete | 단일 리소스 삭제 | DELETE |
| deletecollection | 리소스 일괄 삭제 | DELETE |

### RoleBinding

Role을 사용자, 그룹, 서비스 계정에 바인딩합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-pods
  namespace: default
subjects:
# 사용자
- kind: User
  name: jane
  apiGroup: rbac.authorization.k8s.io
# 그룹
- kind: Group
  name: developers
  apiGroup: rbac.authorization.k8s.io
# 서비스 계정
- kind: ServiceAccount
  name: my-service-account
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

### ClusterRoleBinding

ClusterRole을 클러스터 전체 범위로 바인딩합니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: read-secrets-global
subjects:
- kind: Group
  name: managers
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

### RoleBinding으로 ClusterRole 참조

ClusterRole을 특정 네임스페이스에만 적용할 수 있습니다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-secrets-dev
  namespace: development
subjects:
- kind: User
  name: dave
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole  # ClusterRole 참조
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

### 기본 ClusterRole

| ClusterRole | 설명 |
|-------------|------|
| cluster-admin | 모든 리소스에 대한 전체 권한 |
| admin | 네임스페이스 내 대부분의 리소스 관리 |
| edit | 네임스페이스 내 리소스 읽기/쓰기 (RBAC 제외) |
| view | 네임스페이스 내 리소스 읽기 전용 |

### 리소스 이름 제한

특정 리소스 인스턴스에만 권한 부여

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: configmap-updater
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  resourceNames: ["my-configmap"]  # 특정 ConfigMap만
  verbs: ["update", "get"]
```

### 권한 집계 (Aggregation)

ClusterRole을 레이블로 집계

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-endpoints
  labels:
    rbac.authorization.k8s.io/aggregate-to-view: "true"
    rbac.authorization.k8s.io/aggregate-to-edit: "true"
rules:
- apiGroups: [""]
  resources: ["services", "endpoints"]
  verbs: ["get", "list", "watch"]
```

---

## ServiceAccount

Pod에서 API 서버에 접근할 때 사용하는 계정입니다.

### ServiceAccount 생성

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-service-account
  namespace: default
automountServiceAccountToken: true  # 토큰 자동 마운트
```

### Pod에서 ServiceAccount 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  serviceAccountName: my-service-account
  automountServiceAccountToken: true
  containers:
  - name: app
    image: myapp:1.0
```

### 토큰 위치 및 구조

```
/var/run/secrets/kubernetes.io/serviceaccount/
├── token      # JWT 토큰
├── ca.crt     # 클러스터 CA 인증서
└── namespace  # 네임스페이스 이름
```

### ServiceAccount 토큰 직접 생성

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-sa-token
  annotations:
    kubernetes.io/service-account.name: my-service-account
type: kubernetes.io/service-account-token
```

### ServiceAccount에 ImagePullSecrets 연결

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-service-account
imagePullSecrets:
- name: docker-registry-secret
```

### ServiceAccount 권한 부여

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: my-sa-binding
  namespace: default
subjects:
- kind: ServiceAccount
  name: my-service-account
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

### 토큰 비마운트

보안 강화를 위해 토큰 마운트 비활성화

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  serviceAccountName: my-service-account
  automountServiceAccountToken: false
  containers:
  - name: app
    image: myapp:1.0
```

---

## Pod Security Standards

Pod의 보안 수준을 정의하는 표준입니다.

### 보안 수준

| 수준 | 설명 |
|------|------|
| Privileged | 제한 없음, 권한 상승 허용 |
| Baseline | 최소한의 제한, 알려진 권한 상승 방지 |
| Restricted | 강력한 제한, Pod 강화 모범 사례 적용 |

### 네임스페이스 레이블로 적용

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: my-namespace
  labels:
    # enforce: 위반 시 Pod 생성 거부
    pod-security.kubernetes.io/enforce: baseline
    pod-security.kubernetes.io/enforce-version: latest

    # warn: 위반 시 경고만 표시
    pod-security.kubernetes.io/warn: restricted
    pod-security.kubernetes.io/warn-version: latest

    # audit: 감사 로그에 기록
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/audit-version: latest
```

### Restricted 수준 요구사항

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: restricted-pod
spec:
  securityContext:
    runAsNonRoot: true
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: myapp:1.0
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
      runAsUser: 1000
      runAsGroup: 1000
```

---

## Security Context

컨테이너와 Pod의 보안 설정을 정의합니다.

### Pod 수준 Security Context

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: security-context-demo
spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
    fsGroupChangePolicy: OnRootMismatch
    supplementalGroups: [4000, 5000]
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: myapp:1.0
```

### 컨테이너 수준 Security Context

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    securityContext:
      runAsUser: 1000
      runAsNonRoot: true
      readOnlyRootFilesystem: true
      allowPrivilegeEscalation: false
      privileged: false
      capabilities:
        drop:
        - ALL
        add:
        - NET_BIND_SERVICE
```

### 주요 필드

| 필드 | 설명 |
|------|------|
| runAsUser | 컨테이너 프로세스의 UID |
| runAsGroup | 컨테이너 프로세스의 GID |
| runAsNonRoot | root로 실행 금지 |
| fsGroup | 볼륨의 그룹 소유권 |
| privileged | 특권 모드 (host와 동등한 권한) |
| allowPrivilegeEscalation | 권한 상승 허용 |
| readOnlyRootFilesystem | 루트 파일시스템 읽기 전용 |
| capabilities | Linux capabilities 설정 |

### Linux Capabilities

```yaml
securityContext:
  capabilities:
    drop:
    - ALL
    add:
    - NET_BIND_SERVICE  # 1024 미만 포트 바인딩
    - SYS_TIME          # 시스템 시간 변경
```

** 주요 Capabilities:**

| Capability | 설명 |
|------------|------|
| NET_BIND_SERVICE | 1024 미만 포트 바인딩 |
| SYS_TIME | 시스템 시간 변경 |
| SYS_ADMIN | 다양한 관리 작업 |
| NET_ADMIN | 네트워크 설정 변경 |
| DAC_OVERRIDE | 파일 읽기/쓰기 권한 무시 |

---

## RBAC 모범 사례

### 1. 최소 권한 원칙

```yaml
# 나쁜 예: 와일드카드 사용
rules:
- apiGroups: ["*"]
  resources: ["*"]
  verbs: ["*"]

# 좋은 예: 필요한 권한만 명시
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]
```

### 2. 네임스페이스 범위 선호

```yaml
# 가능하면 Role + RoleBinding 사용
# ClusterRole + ClusterRoleBinding은 꼭 필요한 경우만
```

### 3. 그룹 활용

```yaml
subjects:
- kind: Group
  name: developers  # 개별 사용자 대신 그룹 사용
  apiGroup: rbac.authorization.k8s.io
```

### 4. 권한 검증

```bash
# 특정 사용자의 권한 확인
kubectl auth can-i list pods --as=jane

# 서비스 계정 권한 확인
kubectl auth can-i list pods \
  --as=system:serviceaccount:default:my-sa

# 모든 권한 확인
kubectl auth can-i --list --as=jane
```

### 5. 위험한 권한 제한

```yaml
# 피해야 할 위험한 권한 조합
rules:
# Secret 전체 접근 - 위험
- resources: ["secrets"]
  verbs: ["*"]

# 워크로드 생성 - Pod 내에서 추가 권한 획득 가능
- resources: ["pods", "deployments"]
  verbs: ["create"]

# RBAC 수정 - 자기 권한 상승 가능
- resources: ["roles", "rolebindings"]
  verbs: ["create", "update"]

# impersonate - 다른 사용자로 위장 가능
- resources: ["users", "groups", "serviceaccounts"]
  verbs: ["impersonate"]
```

### 6. 정기적인 감사

```bash
# 사용되지 않는 RoleBinding 확인
kubectl get rolebindings -A

# ClusterRoleBinding 검토
kubectl get clusterrolebindings

# 특정 ClusterRole 사용자 확인
kubectl get clusterrolebindings -o json | \
  jq '.items[] | select(.roleRef.name=="cluster-admin")'
```

---

## 네트워크 보안

### NetworkPolicy로 트래픽 제어

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all
  namespace: production
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-web-to-api
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: api
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: web
    ports:
    - protocol: TCP
      port: 8080
```

---

## 감사 로그 (Audit Logging)

API 서버의 모든 요청을 기록합니다.

### 감사 정책 예시

```yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
# Secret 접근은 모든 정보 기록
- level: RequestResponse
  resources:
  - group: ""
    resources: ["secrets"]

# Pod 읽기는 메타데이터만
- level: Metadata
  resources:
  - group: ""
    resources: ["pods"]
  verbs: ["get", "list", "watch"]

# 시스템 계정은 무시
- level: None
  users: ["system:kube-proxy"]
  verbs: ["watch"]

# 기본: 요청 정보 기록
- level: Request
```

### 감사 수준

| 수준 | 설명 |
|------|------|
| None | 기록하지 않음 |
| Metadata | 요청 메타데이터만 (사용자, 타임스탬프, 리소스 등) |
| Request | 메타데이터 + 요청 본문 |
| RequestResponse | 메타데이터 + 요청/응답 본문 |

---

## 참고 자료

- [RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)
- [Security Context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/)
- [ServiceAccounts](https://kubernetes.io/docs/concepts/security/service-accounts/)
- [Auditing](https://kubernetes.io/docs/tasks/debug/debug-cluster/audit/)
