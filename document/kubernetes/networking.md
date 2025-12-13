# Kubernetes 네트워킹

> 공식 문서: https://kubernetes.io/docs/concepts/services-networking/

## 개요

Kubernetes 네트워킹은 Pod 간 통신, 외부 트래픽 라우팅, 네트워크 보안을 관리합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                        외부 클라이언트                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Ingress Controller                          │
│              (L7 라우팅, SSL/TLS 종료, 가상 호스팅)               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Service                                 │
│      ┌─────────────┬─────────────┬─────────────────────┐        │
│      │ ClusterIP   │  NodePort   │   LoadBalancer      │        │
│      └─────────────┴─────────────┴─────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       EndpointSlice                              │
│                    (Pod IP 주소 추적)                            │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
    ┌─────────┐          ┌─────────┐          ┌─────────┐
    │  Pod A  │          │  Pod B  │          │  Pod C  │
    └─────────┘          └─────────┘          └─────────┘
```

---

## Service

Service는 Pod 집합을 네트워크로 노출하는 추상화입니다. 동적으로 변화하는 Pod에 안정적인 접근점을 제공합니다.

### Service의 필요성

```
문제: Pod IP는 동적
┌─────────┐     ┌─────────┐
│Frontend │ ──▶ │Backend  │ IP: 10.1.2.3 (변경됨!)
└─────────┘     └─────────┘

해결: Service 사용
┌─────────┐     ┌─────────┐     ┌─────────┐
│Frontend │ ──▶ │ Service │ ──▶ │Backend  │
└─────────┘     │(고정 IP)│     │Pods     │
                └─────────┘     └─────────┘
```

### 기본 구조

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
  - name: http
    protocol: TCP
    port: 80         # Service 포트
    targetPort: 8080 # Pod 포트
  type: ClusterIP
```

### Service 유형

#### 1. ClusterIP (기본값)

클러스터 내부에서만 접근 가능한 가상 IP를 할당합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - port: 80
    targetPort: 8080
```

** 접근:**
```
http://backend-service.default.svc.cluster.local:80
http://backend-service.default:80
http://backend-service:80  # 같은 네임스페이스
```

#### 2. NodePort

각 노드의 고정 포트로 서비스를 노출합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nodeport
spec:
  type: NodePort
  selector:
    app: my-app
  ports:
  - port: 80
    targetPort: 8080
    nodePort: 30080  # 30000-32767 범위
```

** 접근:**
```
http://<NodeIP>:30080
```

#### 3. LoadBalancer

클라우드 제공자의 로드 밸런서를 프로비저닝합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-loadbalancer
  annotations:
    # AWS 예시
    service.beta.kubernetes.io/aws-load-balancer-type: nlb
    service.beta.kubernetes.io/aws-load-balancer-internal: "true"
spec:
  type: LoadBalancer
  selector:
    app: my-app
  ports:
  - port: 80
    targetPort: 8080
```

** 상태 확인:**
```bash
kubectl get svc my-loadbalancer
# EXTERNAL-IP 컬럼에서 로드 밸런서 IP 확인
```

#### 4. ExternalName

외부 서비스를 DNS CNAME으로 매핑합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-db
spec:
  type: ExternalName
  externalName: database.example.com
```

** 사용:**
```
# 클러스터 내에서 external-db로 접근하면
# database.example.com으로 리다이렉트
```

### Service 유형 비교

| 유형 | 접근 범위 | 사용 사례 |
|------|----------|----------|
| ClusterIP | 클러스터 내부 | 내부 마이크로서비스 통신 |
| NodePort | 노드 IP + 포트 | 개발/테스트, 외부 로드밸런서 연동 |
| LoadBalancer | 외부 IP | 프로덕션 외부 서비스 |
| ExternalName | DNS CNAME | 외부 서비스 추상화 |

### Headless Service

ClusterIP를 None으로 설정하면 로드밸런싱 없이 Pod IP를 직접 반환합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: headless-service
spec:
  clusterIP: None
  selector:
    app: my-stateful-app
  ports:
  - port: 80
```

**DNS 조회:**
```bash
# 개별 Pod IP 반환
nslookup headless-service.default.svc.cluster.local
```

** 사용 사례:**
- StatefulSet과 함께 사용
- 클라이언트 측 로드밸런싱
- 서비스 디스커버리

### 셀렉터 없는 Service

외부 엔드포인트를 수동으로 지정합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-service
spec:
  ports:
  - port: 80
---
apiVersion: discovery.k8s.io/v1
kind: EndpointSlice
metadata:
  name: external-service-1
  labels:
    kubernetes.io/service-name: external-service
addressType: IPv4
ports:
- port: 80
endpoints:
- addresses:
  - "192.168.1.100"
  - "192.168.1.101"
```

### 서비스 디스커버리

#### 환경 변수

```bash
# Pod 내에서 자동 주입
MY_SERVICE_SERVICE_HOST=10.0.0.11
MY_SERVICE_SERVICE_PORT=80
```

#### DNS

```
# Service DNS 형식
<service-name>.<namespace>.svc.cluster.local

# 예시
my-service.default.svc.cluster.local
my-service.default.svc
my-service.default
my-service  # 같은 네임스페이스
```

### 세션 어피니티

```yaml
spec:
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800
```

| 옵션 | 설명 |
|------|------|
| None | 라운드 로빈 (기본값) |
| ClientIP | 동일 클라이언트 IP는 같은 Pod로 |

---

## Ingress

HTTP/HTTPS 라우팅을 관리하는 API 객체입니다.

### Ingress vs Service

```
┌────────────────────────────────────────────────────────────┐
│                        Ingress                              │
│  • L7 (HTTP/HTTPS) 라우팅                                   │
│  • 호스트/경로 기반 라우팅                                   │
│  • SSL/TLS 종료                                             │
│  • 단일 IP로 다중 서비스 노출                                │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                        Service                              │
│  • L4 (TCP/UDP) 로드밸런싱                                  │
│  • 서비스별 IP/포트                                         │
└────────────────────────────────────────────────────────────┘
```

### 기본 구조

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 80
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
```

### Ingress Controller

** 중요:** Ingress 리소스만으로는 동작하지 않습니다. 반드시 Ingress Controller가 필요합니다.

** 주요 Ingress Controller:**

| Controller | 설명 |
|------------|------|
| NGINX Ingress | 가장 널리 사용되는 컨트롤러 |
| Traefik | 자동 설정, Let's Encrypt 통합 |
| HAProxy | 고성능 로드밸런싱 |
| AWS ALB | AWS Application Load Balancer |
| GCE | Google Cloud Load Balancer |
| Istio | 서비스 메시 기반 |

### 경로 유형 (pathType)

| 유형 | 설명 | 예시 |
|------|------|------|
| Exact | 정확한 경로 일치 | /foo만 일치, /foo/가 아님 |
| Prefix | 접두사 기반 일치 | /foo는 /foo, /foo/, /foo/bar 일치 |
| ImplementationSpecific | 컨트롤러에 따라 다름 | - |

### 호스트 기반 라우팅

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: multi-host-ingress
spec:
  ingressClassName: nginx
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 80
  - host: app.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: app-service
            port:
              number: 80
```

### 와일드카드 호스트

```yaml
spec:
  rules:
  - host: "*.example.com"
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: wildcard-service
            port:
              number: 80
```

** 주의:** `*.example.com`은 `api.example.com`과 일치하지만, `foo.bar.example.com`과는 일치하지 않습니다.

### TLS 설정

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tls-secret
type: kubernetes.io/tls
data:
  tls.crt: <base64 encoded cert>
  tls.key: <base64 encoded key>
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - example.com
    - www.example.com
    secretName: tls-secret
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
```

### NGINX Ingress 어노테이션

```yaml
metadata:
  annotations:
    # SSL 리다이렉트
    nginx.ingress.kubernetes.io/ssl-redirect: "true"

    # URL 재작성
    nginx.ingress.kubernetes.io/rewrite-target: /$2

    # 프록시 설정
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"

    # Rate limiting
    nginx.ingress.kubernetes.io/limit-rps: "10"

    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"

    # 인증
    nginx.ingress.kubernetes.io/auth-type: basic
    nginx.ingress.kubernetes.io/auth-secret: basic-auth
```

### Default Backend

규칙과 일치하지 않는 요청을 처리합니다.

```yaml
spec:
  defaultBackend:
    service:
      name: default-backend
      port:
        number: 80
```

---

## NetworkPolicy

Pod 간 트래픽 흐름을 IP/포트 수준에서 제어합니다.

### 기본 동작

- NetworkPolicy가 없으면 모든 트래픽 허용
- NetworkPolicy가 적용되면 명시적으로 허용된 트래픽만 통과

### 전제 조건

NetworkPolicy를 지원하는 CNI 플러그인이 필요합니다:
- Calico
- Cilium
- Weave Net
- Romana

** 주의:** Flannel은 NetworkPolicy를 지원하지 않습니다.

### 기본 구조

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: my-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: my-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          role: frontend
    ports:
    - protocol: TCP
      port: 80
  egress:
  - to:
    - podSelector:
        matchLabels:
          role: database
    ports:
    - protocol: TCP
      port: 5432
```

### 셀렉터 유형

#### podSelector

같은 네임스페이스의 특정 Pod 선택

```yaml
ingress:
- from:
  - podSelector:
      matchLabels:
        role: frontend
```

#### namespaceSelector

특정 네임스페이스의 모든 Pod 선택

```yaml
ingress:
- from:
  - namespaceSelector:
      matchLabels:
        project: myproject
```

#### 복합 셀렉터

```yaml
# AND 조건: 특정 네임스페이스의 특정 Pod
ingress:
- from:
  - namespaceSelector:
      matchLabels:
        project: myproject
    podSelector:
      matchLabels:
        role: frontend

# OR 조건: 특정 네임스페이스 또는 특정 Pod
ingress:
- from:
  - namespaceSelector:
      matchLabels:
        project: myproject
  - podSelector:
      matchLabels:
        role: frontend
```

#### ipBlock

CIDR 범위로 IP 선택

```yaml
ingress:
- from:
  - ipBlock:
      cidr: 172.17.0.0/16
      except:
      - 172.17.1.0/24
```

### 일반적인 정책 패턴

#### 모든 인바운드 트래픽 거부

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
spec:
  podSelector: {}
  policyTypes:
  - Ingress
```

#### 모든 아웃바운드 트래픽 거부

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
spec:
  podSelector: {}
  policyTypes:
  - Egress
```

#### 모든 트래픽 거부

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

#### 모든 인바운드 트래픽 허용

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-all-ingress
spec:
  podSelector: {}
  ingress:
  - {}
  policyTypes:
  - Ingress
```

#### 특정 포트만 허용

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-http-https
spec:
  podSelector:
    matchLabels:
      app: web
  policyTypes:
  - Ingress
  ingress:
  - ports:
    - protocol: TCP
      port: 80
    - protocol: TCP
      port: 443
```

#### DNS 허용 (Egress)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns
spec:
  podSelector: {}
  policyTypes:
  - Egress
  egress:
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
```

### 정책 평가 규칙

1. 정책은 ** 가산적(Additive)** 으로 적용
2. 정책 간 충돌 없음 - 하나라도 허용하면 트래픽 통과
3. 연결이 허용되려면:
   - 송신 Pod의 Egress 정책 통과
   - 수신 Pod의 Ingress 정책 통과

---

## DNS

Kubernetes는 클러스터 내 서비스 디스커버리를 위해 DNS를 사용합니다.

### DNS 레코드 형식

**Service:**
```
<service-name>.<namespace>.svc.<cluster-domain>
예: my-service.default.svc.cluster.local
```

**Pod:**
```
<pod-ip-dashed>.<namespace>.pod.<cluster-domain>
예: 10-244-0-5.default.pod.cluster.local
```

**Headless Service의 Pod:**
```
<pod-name>.<service-name>.<namespace>.svc.<cluster-domain>
예: web-0.nginx.default.svc.cluster.local
```

### DNS 정책

```yaml
spec:
  dnsPolicy: ClusterFirst  # 기본값
```

| 정책 | 설명 |
|------|------|
| ClusterFirst | 클러스터 DNS 서버 사용 (기본값) |
| Default | 노드의 DNS 설정 상속 |
| ClusterFirstWithHostNet | hostNetwork: true 시 사용 |
| None | dnsConfig로 직접 설정 |

### 커스텀 DNS 설정

```yaml
spec:
  dnsPolicy: None
  dnsConfig:
    nameservers:
    - 8.8.8.8
    - 8.8.4.4
    searches:
    - my-namespace.svc.cluster.local
    options:
    - name: ndots
      value: "5"
```

---

## 참고 자료

- [Service](https://kubernetes.io/docs/concepts/services-networking/service/)
- [Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)
- [Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
- [DNS for Services and Pods](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
