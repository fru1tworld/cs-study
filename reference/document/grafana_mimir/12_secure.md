# Mimir 보안

> 이 문서는 Grafana Mimir 공식 문서의 "Secure" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/manage/secure/

---

## 목차

1. [보안 개요](#보안-개요)
2. [TLS 활성화](#tls-활성화)
3. [컴포넌트 간 mTLS](#컴포넌트-간-mtls)
4. [인증 (Authentication)](#인증-authentication)
5. [인가 (Authorization) / 멀티 테넌시](#인가-authorization--멀티-테넌시)
6. [네트워크 정책](#네트워크-정책)
7. [비밀(Secret) 관리](#비밀secret-관리)
8. [감사 로깅](#감사-로깅)
9. [데이터 암호화](#데이터-암호화)
10. [보안 모범 사례](#보안-모범-사례)

---

## 보안 개요

Mimir는 자체 인증을 제공하지 않으므로 외부 보안 계층 필수.

### 보안 계층

```
[클라이언트]
     |
     v (TLS)
[리버스 프록시 / API Gateway]
     | (인증, 권한)
     v
[Mimir]
     | (mTLS)
[다른 컴포넌트]
     | (TLS)
[Object Storage]
```

### 위협 모델

| 위협 | 대응 |
|------|------|
| 무단 접근 | 인증 프록시 |
| 데이터 도청 | TLS |
| 테넌트 간 침투 | 멀티 테넌시 격리 |
| 비밀 노출 | 시크릿 관리 |
| Compliance | 감사 로깅 |

---

## TLS 활성화

### HTTP TLS

```yaml
server:
  http_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
    
    # 클라이언트 인증서 요구
    client_auth_type: RequireAndVerifyClientCert
    client_ca_file: /etc/certs/ca.crt
    
    # TLS 버전
    min_version: VersionTLS12
    cipher_suites:
      - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
      - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

### gRPC TLS

```yaml
server:
  grpc_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
    client_auth_type: RequireAndVerifyClientCert
    client_ca_file: /etc/certs/ca.crt
```

### Client Auth Type 옵션

| 값 | 설명 |
|----|------|
| `NoClientCert` | 클라이언트 인증서 무시 (TLS만) |
| `RequestClientCert` | 요청하지만 검증 안 함 |
| `RequireAnyClientCert` | 인증서 필수, 검증 안 함 |
| `VerifyClientCertIfGiven` | 있으면 검증 |
| `RequireAndVerifyClientCert` | 인증서 필수, 검증 (mTLS) |

---

## 컴포넌트 간 mTLS

### Distributor → Ingester

```yaml
ingester_client:
  grpc_client_config:
    tls_enabled: true
    tls_cert_path: /etc/certs/distributor.crt
    tls_key_path: /etc/certs/distributor.key
    tls_ca_path: /etc/certs/ca.crt
    tls_server_name: ingester.mimir.svc
    tls_insecure_skip_verify: false
    tls_min_version: VersionTLS12
```

### Querier → Store Gateway

```yaml
store_gateway:
  grpc_client_config:
    tls_enabled: true
    tls_cert_path: /etc/certs/querier.crt
    tls_key_path: /etc/certs/querier.key
    tls_ca_path: /etc/certs/ca.crt
```

### Ruler → Alertmanager

```yaml
ruler:
  alertmanager_client:
    tls_enabled: true
    tls_cert_path: /etc/certs/ruler.crt
    tls_key_path: /etc/certs/ruler.key
    tls_ca_path: /etc/certs/ca.crt
```

### Memberlist TLS

```yaml
memberlist:
  tls_enabled: true
  tls_cert_path: /etc/certs/memberlist.crt
  tls_key_path: /etc/certs/memberlist.key
  tls_ca_path: /etc/certs/ca.crt
  tls_server_name: memberlist.mimir.svc
```

### KV Store TLS (Consul)

```yaml
distributor:
  ring:
    kvstore:
      consul:
        host: consul:8501
        consul_client_config:
          ca_file: /etc/certs/ca.crt
          cert_file: /etc/certs/consul-client.crt
          key_file: /etc/certs/consul-client.key
```

---

## 인증 (Authentication)

Mimir는 자체 인증 X. 옵션:

### 옵션 1: 리버스 프록시

#### Nginx Basic Auth

```nginx
upstream mimir {
  server mimir-1:9009;
  server mimir-2:9009;
  server mimir-3:9009;
}

server {
  listen 443 ssl;
  server_name mimir.example.com;
  
  ssl_certificate /etc/ssl/server.crt;
  ssl_certificate_key /etc/ssl/server.key;
  
  location / {
    auth_basic "Mimir";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    # 사용자명을 테넌트 ID로
    proxy_set_header X-Scope-OrgID "$remote_user";
    
    proxy_pass http://mimir;
  }
}
```

#### oauth2-proxy

```yaml
- name: oauth2-proxy
  image: quay.io/oauth2-proxy/oauth2-proxy
  args:
    - --provider=oidc
    - --oidc-issuer-url=https://accounts.example.com
    - --upstream=http://mimir:9009
    - --client-id=$CLIENT_ID
    - --client-secret=$CLIENT_SECRET
    - --pass-access-token=true
    - --pass-user-headers=true
    - --set-xauthrequest=true
    - --email-domain=*
```

### 옵션 2: API Gateway

Kong, AWS API Gateway, Apigee 등:

```yaml
# Kong 예시
plugins:
  - name: jwt
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp
  
  - name: request-transformer
    config:
      add:
        headers:
          - "X-Scope-OrgID:${jwt.tenant_id}"
```

### 옵션 3: Service Mesh (mTLS)

Istio, Linkerd로 자동 mTLS:

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: mimir-mtls
  namespace: mimir
spec:
  mtls:
    mode: STRICT
```

---

## 인가 (Authorization) / 멀티 테넌시

### 활성화

```yaml
multitenancy_enabled: true
```

활성화 시 모든 요청에 `X-Scope-OrgID` 필수.

### 테넌트 격리

```
오브젝트 스토리지: <bucket>/<tenant>/...
Ingester 메모리: 테넌트별 TSDB
캐시: 테넌트별 키
```

### 테넌트 ID 검증

```yaml
limits:
  user_label_validity_period: 0s
  
  # 테넌트별 한도로 격리
  ingestion_rate: 25_000
  max_global_series_per_user: 1_500_000
```

### 테넌트 간 페더레이션 (Cross-tenant)

기본은 비활성. 명시적 활성화:

```yaml
limits:
  query_partial_data: false       # 일부 테넌트 실패 허용 안 함
  
multitenancy_enabled: true
```

쿼리 시 여러 테넌트:
```http
X-Scope-OrgID: tenant-a|tenant-b
```

### 인가 정책

리버스 프록시에서:

```nginx
# 사용자별 허용 테넌트
location / {
  set $allowed_tenants "";
  
  if ($remote_user = "alice") {
    set $allowed_tenants "tenant-a";
  }
  if ($remote_user = "bob") {
    set $allowed_tenants "tenant-a|tenant-b";
  }
  
  if ($http_x_scope_orgid !~ "^($allowed_tenants)$") {
    return 403;
  }
  
  proxy_pass http://mimir;
}
```

---

## 네트워크 정책

### Kubernetes NetworkPolicy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: mimir-ingester
  namespace: mimir
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: mimir
      app.kubernetes.io/component: ingester
  
  policyTypes:
    - Ingress
    - Egress
  
  ingress:
    # Distributor와 Querier만 허용
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: mimir
              app.kubernetes.io/component: distributor
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: mimir
              app.kubernetes.io/component: querier
      ports:
        - protocol: TCP
          port: 9095
        - protocol: TCP
          port: 9009
    
    # Memberlist 가십
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: mimir
      ports:
        - protocol: TCP
          port: 7946
        - protocol: UDP
          port: 7946
  
  egress:
    # Object Storage
    - to:
        - namespaceSelector: {}
      ports:
        - protocol: TCP
          port: 443
    
    # DNS
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

### Ingress 제한

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mimir
  annotations:
    nginx.ingress.kubernetes.io/whitelist-source-range: "10.0.0.0/8,172.16.0.0/12"
```

---

## 비밀(Secret) 관리

### 환경변수 (간단)

```yaml
common:
  storage:
    s3:
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}
```

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
mimir -config.file=mimir.yaml -config.expand-env=true
```

### Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mimir-bucket-secret
type: Opaque
stringData:
  AWS_ACCESS_KEY_ID: "..."
  AWS_SECRET_ACCESS_KEY: "..."
```

```yaml
env:
  - name: AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: mimir-bucket-secret
        key: AWS_ACCESS_KEY_ID
  - name: AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: mimir-bucket-secret
        key: AWS_SECRET_ACCESS_KEY
```

### IAM Roles (AWS)

가장 안전. 비밀 키 불필요.

```yaml
serviceAccount:
  create: true
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789:role/mimir-s3-role
```

```yaml
# IAM Role 정책
{
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-mimir-bucket/*",
        "arn:aws:s3:::my-mimir-bucket"
      ]
    }
  ]
}
```

### GCP Workload Identity

```yaml
serviceAccount:
  annotations:
    iam.gke.io/gcp-service-account: mimir@my-project.iam.gserviceaccount.com
```

### Azure Managed Identity

```yaml
common:
  storage:
    azure:
      account_name: myaccount
      msi_resource: ""    # 빈 값으로 자동 MSI 사용
      user_assigned_id: "/subscriptions/.../identities/mimir"
```

### HashiCorp Vault

```yaml
# vault-agent 사이드카로 시크릿을 파일로 마운트
volumes:
  - name: secrets
    emptyDir: {}

# vault-agent가 /vault/secrets/mimir.yaml 작성
```

---

## 감사 로깅

### Mimir 자체 로그

요청 정보 로깅:

```yaml
server:
  log_level: info
  log_format: json
  
  log_request_headers: true
  log_request_at_info_level_enabled: true
```

JSON 로그 예시:
```json
{
  "ts": "2024-01-01T12:00:00Z",
  "caller": "logging.go:76",
  "level": "info",
  "method": "POST",
  "path": "/api/v1/push",
  "status": 204,
  "duration_seconds": 0.012,
  "user_id": "tenant-1",
  "remote_addr": "10.0.1.5"
}
```

### 리버스 프록시 감사 로그

```nginx
log_format mimir_audit escape=json '{'
  '"ts":"$time_iso8601",'
  '"client_ip":"$remote_addr",'
  '"user":"$remote_user",'
  '"tenant":"$http_x_scope_orgid",'
  '"method":"$request_method",'
  '"path":"$request_uri",'
  '"status":$status,'
  '"bytes_sent":$body_bytes_sent,'
  '"duration":$request_time,'
  '"user_agent":"$http_user_agent"'
'}';

access_log /var/log/nginx/mimir-audit.log mimir_audit;
```

### 로그를 Loki로

```alloy
loki.source.file "mimir_audit" {
  targets = [{__path__ = "/var/log/nginx/mimir-audit.log", job = "mimir-audit"}]
  forward_to = [loki.write.default.receiver]
}
```

### Compliance 알림

```yaml
- alert: SuspiciousMimirAccess
  expr: |
    sum by (client_ip) (
      rate({job="mimir-audit"} | json | status >= 400 [5m])
    ) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "{{ $labels.client_ip }}에서 의심스러운 Mimir 접근"
```

---

## 데이터 암호화

### 전송 중 (In-transit)

- 클라이언트 ↔ Mimir: TLS
- 컴포넌트 간: mTLS
- Mimir ↔ Storage: TLS (백엔드 의존)

### 저장 시 (At-rest)

#### S3 SSE

```yaml
common:
  storage:
    s3:
      sse:
        type: SSE-S3                # 또는 SSE-KMS
        kms_key_id: arn:aws:kms:us-east-1:123:key/...
```

#### GCS

기본 암호화 (Google 관리). CMEK 옵션:

```yaml
common:
  storage:
    gcs:
      bucket_name: my-mimir
      # 버킷에 CMEK 설정 (콘솔/gcloud)
```

#### Azure Blob

기본 암호화. Customer-managed keys 가능 (스토리지 계정 설정).

### Disk 암호화

Ingester/Compactor의 PV는 호스트/클러스터 레벨 디스크 암호화 사용 (LUKS, EBS encryption 등).

---

## 보안 모범 사례

### 체크리스트

- [ ] **TLS 활성화** (HTTP, gRPC)
- [ ] **컴포넌트 간 mTLS**
- [ ] **인증 프록시** (Nginx, oauth2-proxy)
- [ ] **멀티 테넌시 활성화**
- [ ] **테넌트별 한도 설정**
- [ ] **Network Policy**
- [ ] **시크릿은 IAM/MSI/Workload Identity 우선**
- [ ] **감사 로깅**
- [ ] **저장 암호화** (S3 SSE 등)
- [ ] **RBAC** (Kubernetes)
- [ ] **컨테이너 권한 최소화** (root 비실행, readOnlyRootFilesystem)
- [ ] **이미지 취약점 스캔**
- [ ] **정기 보안 업데이트**

### Pod Security

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 10001
  runAsGroup: 10001
  fsGroup: 10001
  
containers:
  - name: mimir
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
          - ALL
```

### RBAC

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: mimir
  namespace: mimir
rules:
  - apiGroups: [""]
    resources: ["pods", "endpoints"]
    verbs: ["get", "list"]
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["mimir-bucket-secret"]
    verbs: ["get"]
```

### 정기 점검

- 인증서 만료 모니터링
- 시크릿 로테이션
- 의존성 CVE 스캔
- 접근 로그 검토
- 권한 정기 감사
