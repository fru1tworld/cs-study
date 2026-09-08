# Mimir 보안과 프로덕션 운영

## Mimir 보안

> 원본: https://grafana.com/docs/mimir/latest/manage/secure/

<a id="보안-개요"></a>
### 보안 개요

Mimir는 자체 인증 기능을 제공하지 않으므로 리버스 프록시나 API Gateway에서 인증과 권한을 확인해야 한다. 전송 구간에는 TLS를 적용하고, 컴포넌트 간 통신은 mTLS로 보호한다.

#### 보안 계층

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

#### 위협 모델

- 무단 접근: 인증 프록시로 대응
- 데이터 도청: TLS로 대응
- 테넌트 간 침투: 멀티 테넌시 격리로 대응
- 비밀 노출: 시크릿 관리로 대응
- Compliance: 감사 로깅으로 대응

<a id="tls-활성화"></a>

### TLS 활성화

HTTP와 gRPC 서버에 인증서와 개인키를 지정한다. 아래 예제는 클라이언트 인증서도 요구하고 CA로 검증하도록 설정한다.

#### HTTP TLS

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

#### gRPC TLS

```yaml
server:
  grpc_tls_config:
    cert_file: /etc/certs/server.crt
    key_file: /etc/certs/server.key
    client_auth_type: RequireAndVerifyClientCert
    client_ca_file: /etc/certs/ca.crt
```

#### Client Auth Type 옵션

- `NoClientCert`: 클라이언트 인증서 무시(TLS만)
- `RequestClientCert`: 요청하지만 검증 안 함
- `RequireAnyClientCert`: 인증서 필수, 검증 안 함
- `VerifyClientCertIfGiven`: 있으면 검증
- `RequireAndVerifyClientCert`: 인증서 필수, 검증(mTLS)

<a id="컴포넌트-간-mtls"></a>

### 컴포넌트 간 mTLS

서버가 클라이언트 인증서를 요구하면 호출하는 컴포넌트에도 인증서, 개인키, CA 경로를 설정해야 한다. 다음은 각 통신 경로의 클라이언트 설정이다.

#### Distributor → Ingester

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

#### Querier → Store Gateway

```yaml
store_gateway:
  grpc_client_config:
    tls_enabled: true
    tls_cert_path: /etc/certs/querier.crt
    tls_key_path: /etc/certs/querier.key
    tls_ca_path: /etc/certs/ca.crt
```

#### Ruler → Alertmanager

```yaml
ruler:
  alertmanager_client:
    tls_enabled: true
    tls_cert_path: /etc/certs/ruler.crt
    tls_key_path: /etc/certs/ruler.key
    tls_ca_path: /etc/certs/ca.crt
```

#### Memberlist TLS

```yaml
memberlist:
  tls_enabled: true
  tls_cert_path: /etc/certs/memberlist.crt
  tls_key_path: /etc/certs/memberlist.key
  tls_ca_path: /etc/certs/ca.crt
  tls_server_name: memberlist.mimir.svc
```

#### KV Store TLS (Consul)

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

<a id="인증-authentication"></a>

### 인증 (Authentication)

외부 인증 계층은 리버스 프록시, API Gateway, Service Mesh 등으로 구성할 수 있다. 다음 예제에서는 각 방식의 설정을 살펴본다.

#### 옵션 1: 리버스 프록시

- Nginx Basic Auth

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

- oauth2-proxy

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

#### 옵션 2: API Gateway

- Kong, AWS API Gateway, Apigee 등:

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

#### 옵션 3: Service Mesh (mTLS)

- Istio, Linkerd로 자동 mTLS:

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

<a id="인가-authorization--멀티-테넌시"></a>

### 인가 (Authorization) / 멀티 테넌시

#### 활성화

```yaml
multitenancy_enabled: true
```

멀티 테넌시를 활성화하면 모든 요청에 `X-Scope-OrgID` 헤더가 필요하다. 이 헤더로 식별한 테넌트에 따라 저장 경로와 메모리, 캐시를 구분한다.

#### 테넌트 격리

```
오브젝트 스토리지: <bucket>/<tenant>/...
Ingester 메모리: 테넌트별 TSDB
캐시: 테넌트별 키
```

#### 테넌트 ID 검증

```yaml
limits:
  user_label_validity_period: 0s
  
  # 테넌트별 한도로 격리
  ingestion_rate: 25_000
  max_global_series_per_user: 1_500_000
```

#### 테넌트 간 페더레이션 (Cross-tenant)

여러 테넌트를 함께 조회하는 기능은 기본적으로 비활성화되어 있으므로, 사용하려면 명시적으로 활성화해야 한다.

```yaml
limits:
  query_partial_data: false       # 일부 테넌트 실패 허용 안 함
  
multitenancy_enabled: true
```

- 쿼리 시 여러 테넌트:

```http
X-Scope-OrgID: tenant-a|tenant-b
```

#### 인가 정책

리버스 프록시에서는 인증된 사용자가 요청한 테넌트에 접근할 수 있는지도 확인한다. 아래 예제는 `alice`와 `bob`에게 허용한 테넌트를 설정하고 요청 헤더와 비교한다.

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

<a id="네트워크-정책"></a>

### 네트워크 정책

#### Kubernetes NetworkPolicy

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

#### Ingress 제한

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mimir
  annotations:
    nginx.ingress.kubernetes.io/whitelist-source-range: "10.0.0.0/8,172.16.0.0/12"
```

<a id="비밀secret-관리"></a>

### 비밀(Secret) 관리

스토리지 접근에 필요한 인증 정보를 설정 파일에 전달하는 방법은 실행 환경에 따라 다르다. 환경변수와 Kubernetes Secret을 사용하는 예제부터 클라우드의 역할 기반 인증까지 살펴본다.

#### 환경변수 (간단)

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

#### Kubernetes Secrets

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

#### IAM Roles (AWS)

AWS에서는 별도의 비밀 키를 배포하지 않는 IAM Role 방식을 우선한다. 다음 예제는 ServiceAccount에 역할을 연결하고 필요한 S3 작업을 허용한다.

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

#### GCP Workload Identity

```yaml
serviceAccount:
  annotations:
    iam.gke.io/gcp-service-account: mimir@my-project.iam.gserviceaccount.com
```

#### Azure Managed Identity

```yaml
common:
  storage:
    azure:
      account_name: myaccount
      msi_resource: ""    # 빈 값으로 자동 MSI 사용
      user_assigned_id: "/subscriptions/.../identities/mimir"
```

#### HashiCorp Vault

```yaml
# vault-agent 사이드카로 시크릿을 파일로 마운트
volumes:
  - name: secrets
    emptyDir: {}

# vault-agent가 /vault/secrets/mimir.yaml 작성
```

<a id="감사-로깅"></a>

### 감사 로깅

접근 기록은 Mimir의 요청 로그와 리버스 프록시의 인증 사용자 정보를 함께 살펴본다. 아래에서는 JSON으로 기록한 프록시 로그를 Loki로 보내는 설정까지 이어진다.

#### Mimir 자체 로그

- 요청 정보 로깅:

```yaml
server:
  log_level: info
  log_format: json
  
  log_request_headers: true
  log_request_at_info_level_enabled: true
```

- JSON 로그 예시:

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

#### 리버스 프록시 감사 로그

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

#### 로그를 Loki로

```alloy
loki.source.file "mimir_audit" {
  targets = [{__path__ = "/var/log/nginx/mimir-audit.log", job = "mimir-audit"}]
  forward_to = [loki.write.default.receiver]
}
```

#### Compliance 알림

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

<a id="데이터-암호화"></a>

### 데이터 암호화

#### 전송 중 (In-transit)

- 클라이언트 ↔ Mimir: TLS
- 컴포넌트 간: mTLS
- Mimir ↔ Storage: TLS (백엔드 의존)

#### 저장 시 (At-rest)

- S3 SSE

  ```yaml
  common:
    storage:
      s3:
        sse:
          type: SSE-S3                # 또는 SSE-KMS
          kms_key_id: arn:aws:kms:us-east-1:123:key/...
  ```

- GCS

  - 기본적으로 Google이 암호화를 관리함
    - CMEK 사용 시:

  ```yaml
  common:
    storage:
      gcs:
        bucket_name: my-mimir
        # 버킷에 CMEK 설정 (콘솔/gcloud)
  ```

- Azure Blob

  - 기본적으로 암호화됨
    - Customer-managed keys는 스토리지 계정 설정에서 구성 가능

#### Disk 암호화

- Ingester/Compactor의 PV는 호스트/클러스터 레벨 디스크 암호화 사용(LUKS, EBS encryption 등).

<a id="보안-모범-사례"></a>

### 보안 모범 사례

#### 체크리스트

- [ ] TLS 활성화 (HTTP, gRPC)
- [ ] 컴포넌트 간 mTLS
- [ ] 인증 프록시 (Nginx, oauth2-proxy)
- [ ] 멀티 테넌시 활성화
- [ ] 테넌트별 한도 설정
- [ ] Network Policy
- [ ] 시크릿은 IAM/MSI/Workload Identity 우선
- [ ] 감사 로깅
- [ ] 저장 암호화 (S3 SSE 등)
- [ ] RBAC (Kubernetes)
- [ ] 컨테이너 권한 최소화 (root 비실행, readOnlyRootFilesystem)
- [ ] 이미지 취약점 스캔
- [ ] 정기 보안 업데이트

#### Pod Security

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

#### RBAC

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

#### 정기 점검

- 인증서 만료 모니터링
- 시크릿 로테이션
- 의존성 CVE 스캔
- 접근 로그 검토
- 권한 정기 감사

## Mimir 프로덕션 운영

> 원본: https://grafana.com/docs/mimir/latest/manage/run-production-environment/

<a id="프로덕션-체크리스트"></a>
### 프로덕션 체크리스트

#### 배포 전 체크

- 인프라

  - [ ] Microservices 모드 (또는 검증된 SSD)
  - [ ] 3개 이상 Availability Zone에 분산
  - [ ] 오브젝트 스토리지 (Cross-region replication)
  - [ ] Kubernetes 1.25+ 또는 안정 환경
  - [ ] 충분한 노드 리소스
  - [ ] 네트워크 대역폭

- 컴포넌트

  - [ ] Distributor: 3+ replicas
  - [ ] Ingester: 3+ replicas, Replication Factor 3
  - [ ] Querier: 3+ replicas
  - [ ] Query Frontend: 2+ replicas
  - [ ] Query Scheduler: 2+ replicas
  - [ ] Store Gateway: 3+ replicas, Replication Factor 3
  - [ ] Compactor: 1+ instances
  - [ ] Ruler: 2+ replicas
  - [ ] Alertmanager: 3+ replicas
  - [ ] Memcached: 모든 캐시 활성화

- 보안

  - [ ] TLS 활성화
  - [ ] 컴포넌트 간 mTLS
  - [ ] 인증 프록시
  - [ ] 멀티 테넌시 활성화
  - [ ] 시크릿 관리 (IAM/MSI)
  - [ ] Network Policy
  - [ ] 감사 로깅

- 한도/구성

  - [ ] 테넌트별 한도 설정
  - [ ] runtime_config로 동적 조정 가능
  - [ ] 글로벌 안전 한도 (instance_limits)
  - [ ] WAL 설정
  - [ ] 적절한 보존 기간

- 운영

  - [ ] 모니터링 설정 (Mixin)
  - [ ] 알림 룰 배포
  - [ ] Continuous Test 실행
  - [ ] Runbook 준비
  - [ ] 백업 설정
  - [ ] DR 절차 문서화

<a id="용량-계획"></a>

### 용량 계획

활성 시계열 수를 기준으로 Ingester의 규모를 잡고, 샘플 주기와 보존 기간으로 스토리지 용량을 계산한다. 조회 측에서는 Store Gateway의 인덱스 헤더, Querier의 동시 실행 수, 캐시 용량을 각각 살펴본다.

#### 시계열 기반 사이징

- 활성 시계열 1M: 컴포넌트 Ingester ×3, 권장 사양 16GB RAM, 4 vCPU each
- 활성 시계열 10M: 컴포넌트 Ingester ×6, 권장 사양 32GB RAM, 8 vCPU each
- 활성 시계열 100M: 컴포넌트 Ingester ×30, 권장 사양 32GB RAM, 8 vCPU each
- 활성 시계열 1B: 컴포넌트 Ingester ×100, 권장 사양 64GB RAM, 16 vCPU each

#### Ingester 메모리 추정

```
RAM = 활성 시계열 × 8KB × 1.5 (오버헤드)

10M 시계열 → 약 120GB 분산 (3개 RF, 30개 인스턴스 → 12GB/인스턴스)
```

#### 오브젝트 스토리지

```
일일 데이터 = 활성 시계열 × 샘플/일 × ~2 bytes (압축 후)

10M × 8640 (10초 간격) × 2 bytes = ~170GB/일
30일 보존: 5.1TB
1년 보존: 62TB
```

#### Store Gateway

```
RAM = 블록 인덱스 헤더 크기 합계
    ≈ 일일 데이터 × 보존 일수 × 0.001
```

#### Querier

```
인스턴스 수 = 동시 쿼리 / max_concurrent
```

#### 캐시 사이징

- Results: 권장 RAM 1-4GB
- Chunks: 권장 RAM 활성 데이터의 50%
- Metadata: 권장 RAM 1-4GB
- Index: 권장 RAM 16-64GB

<a id="고가용성-ha"></a>

### 고가용성 (HA)

복제본 수와 배치를 함께 설정한다. 여러 Availability Zone에 복제본을 나누고, 같은 노드에 Ingester가 몰리지 않도록 anti-affinity를 적용한다.

#### Availability Zone 분산

```yaml
# 모든 stateful 컴포넌트
ingester:
  zoneAwareReplication:
    enabled: true
    zones: [zone-a, zone-b, zone-c]

store_gateway:
  zoneAwareReplication:
    enabled: true
    zones: [zone-a, zone-b, zone-c]
```

#### Replication Factor

```yaml
ingester:
  ring:
    replication_factor: 3

store_gateway:
  sharding_ring:
    replication_factor: 3
```

#### Prometheus HA

- [09_configure_advanced.md](./04_manage_and_configure.md#ha-중복-제거-ha-tracker) 참조

#### Anti-affinity

```yaml
ingester:
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/component
                operator: In
                values: [ingester]
          topologyKey: kubernetes.io/hostname
```

이 설정은 같은 노드에 Ingester가 여러 개 배치되는 것을 막는다.

#### PodDisruptionBudget

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: mimir-ingester
spec:
  maxUnavailable: 1   # 한 번에 최대 1개만 다운
  selector:
    matchLabels:
      app.kubernetes.io/component: ingester
```

<a id="모니터링과-알림"></a>

### 모니터링과 알림

Mimir Mixin으로 대시보드와 룰을 배포한 뒤, 쓰기와 읽기의 가용성 및 지연 시간을 추적한다. 아래 알림 예제는 인스턴스 상태, 요청 오류, 시계열 한도, 컴팩션과 룰 실행 실패를 다룬다.

#### Mimir Mixin 배포

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json

# Recording rules
jsonnet -J vendor recording-rules.libsonnet > rules.yaml

# Alerting rules
jsonnet -J vendor alerts.libsonnet > alerts.yaml
```

#### 핵심 SLI

```promql
# Write 가용성
1 - (
  sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[5m]))
  /
  sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[5m]))
)

# Read 가용성
1 - (
  sum(rate(cortex_request_duration_seconds_count{route=~"/prometheus/api/v1/.*", status_code=~"5.."}[5m]))
  /
  sum(rate(cortex_request_duration_seconds_count{route=~"/prometheus/api/v1/.*"}[5m]))
)

# Write Latency P99
histogram_quantile(0.99,
  sum by (le) (rate(cortex_request_duration_seconds_bucket{route="/api/v1/push"}[5m]))
)

# Query Latency P99
histogram_quantile(0.99,
  sum by (le) (rate(cortex_request_duration_seconds_bucket{route=~"/prometheus/api/v1/query.*"}[5m]))
)
```

#### 핵심 알림

```yaml
groups:
  - name: mimir_critical
    rules:
      - alert: MimirIngesterUnhealthy
        expr: |
          min by (cluster, namespace) (
            up{job=~".*/ingester"}
          ) == 0
        for: 5m
        labels:
          severity: critical
      
      - alert: MimirRequestErrors
        expr: |
          sum by (cluster, namespace, route) (
            rate(cortex_request_duration_seconds_count{status_code=~"5..", route!~"ready|debug.*|metrics"}[5m])
          )
          /
          sum by (cluster, namespace, route) (
            rate(cortex_request_duration_seconds_count[5m])
          )
          > 0.01
        for: 15m
        labels:
          severity: critical
      
      - alert: MimirIngesterReachingSeriesLimit
        expr: |
          (
            cortex_ingester_memory_series
            /
            ignoring(limit) cortex_ingester_instance_limits{limit="max_series"}
          ) > 0.8
        for: 3h
        labels:
          severity: warning
      
      - alert: MimirCompactorHasNotSuccessfullyRunCompaction
        expr: |
          time() - cortex_compactor_last_successful_run_timestamp_seconds > 24 * 60 * 60
        labels:
          severity: critical
      
      - alert: MimirRulerTooManyFailedQueries
        expr: |
          sum by (cluster, namespace) (
            rate(cortex_ruler_queries_failed_total[5m])
          ) > 0.1
        for: 5m
        labels:
          severity: critical
```

#### 자체 메트릭

```alloy
prometheus.scrape "mimir_self" {
  targets    = discovery.kubernetes.mimir_pods.targets
  forward_to = [prometheus.remote_write.self.receiver]
}

prometheus.remote_write "self" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "mimir-self",
    }
  }
}
```

<a id="성능-튜닝"></a>

### 성능 튜닝

#### Query Sharding

```yaml
query_frontend:
  query_sharding_enabled: true
  query_sharding_total_shards: 16
  query_sharding_max_sharded_queries: 128
```

#### Query Splitting

```yaml
query_frontend:
  split_queries_by_interval: 24h
  align_queries_with_step: true
```

#### Memcached 모든 캐시

```yaml
memcached:
  enabled: true

chunks-cache:
  enabled: true
  replicas: 6
  resources:
    requests:
      cpu: 500m
      memory: 16Gi

metadata-cache:
  enabled: true
  replicas: 3

results-cache:
  enabled: true
  replicas: 3

index-cache:
  enabled: true
  replicas: 6
  resources:
    requests:
      memory: 32Gi
```

#### Ingester 튜닝

```yaml
ingester:
  ring:
    instance_limits:
      max_ingestion_rate: 0
      max_series: 1500000
      max_inflight_push_requests: 30000
  
  blocks_storage:
    tsdb:
      head_compaction_interval: 1m
      head_compaction_concurrency: 5
      stripe_size: 16384
```

#### Distributor 튜닝

```yaml
distributor:
  pool:
    health_check_ingesters: true
  
  remote_timeout: 10s
  
  # 분산 처리 동시성
  ingestion_burst_factor: 0
```

#### Store Gateway 튜닝

```yaml
store_gateway:
  bucket_store:
    sync_interval: 5m
    max_concurrent: 50
    
    # 인덱스 헤더 lazy loading
    index_header_lazy_loading_enabled: true
    index_header_lazy_loading_idle_timeout: 1h
    
    # Streaming series
    series_streaming_enabled: true
    streaming_series_batch_size: 5000
```

<a id="용량-한도-설정"></a>

### 용량 한도 설정

테넌트별 한도와 인스턴스 한도를 나누어 설정한다. 테넌트의 서비스 등급에 따라 적재량과 보존 기간을 다르게 두고, 메트릭과 라벨에도 한도를 적용해 카디널리티를 제한한다.

#### 단계적 한도

```yaml
limits:
  # Soft limit (모니터링용)
  ingestion_rate: 25000
  
  # Hard limit (instance level)
ingester:
  ring:
    instance_limits:
      max_series: 1500000
      max_ingestion_rate: 80000     # 인스턴스 한도
```

#### 테넌트별 차등화

```yaml
# runtime.yaml
overrides:
  enterprise-tenant:
    ingestion_rate: 200000
    ingestion_burst_size: 1000000
    max_global_series_per_user: 10000000
    max_query_lookback: 0
    compactor_blocks_retention_period: 17520h    # 2년
  
  free-tenant:
    ingestion_rate: 1000
    max_global_series_per_user: 50000
    compactor_blocks_retention_period: 168h      # 7일
```

#### Cardinality 보호

```yaml
limits:
  max_global_series_per_metric: 50000
  max_label_names_per_series: 30
  max_label_name_length: 1024
  max_label_value_length: 2048
```

<a id="업그레이드-전략"></a>

### 업그레이드 전략

#### 무중단 업그레이드 순서

- 1\. Compactor (단일 인스턴스, 다운타임 OK)
- 2\. Store Gateway
- 3\. Query Frontend / Scheduler
- 4\. Querier
- 5\. Distributor
- 6\. Ingester (가장 신중)
- 7\. Ruler
- 8\. Alertmanager

#### Ingester 안전 업그레이드

```yaml
ingester:
  ring:
    final_sleep: 30s
  
  blocks_storage:
    tsdb:
      ship_interval: 1m       # 자주 업로드
      flush_blocks_on_shutdown: true
```

- PodDisruptionBudget으로 동시 다운 제한:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: mimir-ingester
spec:
  maxUnavailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/component: ingester
```

#### Zone-aware 업그레이드

Zone을 하나씩 업그레이드하고 안정화된 것을 확인한 뒤 다음 Zone으로 넘어간다.

```bash
# Zone A 업그레이드
helm upgrade mimir grafana/mimir-distributed \
  --set ingester.zoneAwareReplication.zones[0].image.tag=2.10.0

# 안정화 대기 (수 분)

# Zone B
helm upgrade ... zones[1].image.tag=2.10.0

# Zone C
helm upgrade ... zones[2].image.tag=2.10.0
```

#### 카나리 배포

```yaml
# 1개 인스턴스만 새 버전
ingester:
  canary:
    enabled: true
    image:
      tag: 2.11.0-rc1
```

카나리 인스턴스의 동작을 검증한 뒤 전체 인스턴스로 롤아웃한다.

#### Breaking Changes

- 릴리스 노트의 [Breaking Changes](https://grafana.com/docs/mimir/latest/release-notes/) 필독

<a id="장애-복구"></a>

### 장애 복구

#### Ingester 장애

- 단일 인스턴스

  - WAL 활성화로 자동 복구
  - Replication factor 3이면 다른 ingester가 처리

- 다수 (한 Zone 전체)

  - 다른 Zone의 복제본이 응답
  - 복구 자동

- 모든 Ingester 동시 다운

  - 최근 데이터 일부 손실 (메모리 + WAL 손실)
  - 가능한 한 한 Zone씩 복구

#### Object Storage 장애

오브젝트 스토리지에 장애가 발생하면 Ingester는 메모리와 WAL에 데이터를 보관하며 재시도한다. 그동안 Querier는 캐시된 데이터로 응답하고, 스토리지가 복구되면 Ingester가 데이터를 자동으로 업로드한다.

#### KV Store 장애 (Memberlist)

가십 기반의 Memberlist는 스스로 상태를 복구한다. Consul이나 etcd를 사용한다면 KV 클러스터를 먼저 복구한다.

#### Hash Ring Inconsistency

```bash
# Ring 강제 리셋 (마지막 수단)
curl -X POST http://mimir-ingester:9009/ingester/ring/forget?id=<unhealthy-instance>
```

<a id="백업-전략"></a>

### 백업 전략

#### Object Storage

메트릭 데이터가 저장되는 오브젝트 스토리지가 가장 중요한 백업 대상이다. 아래 예제는 S3의 리전 간 복제와 버전 관리를 설정한다.

- S3 Cross-Region Replication

  ```json
  {
    "Role": "arn:aws:iam::123:role/replication-role",
    "Rules": [{
      "Status": "Enabled",
      "Priority": 1,
      "Filter": {},
      "Destination": {
        "Bucket": "arn:aws:s3:::mimir-backup-us-west-2"
      },
      "DeleteMarkerReplication": {"Status": "Enabled"}
    }]
  }
  ```

- Versioning

  ```bash
  aws s3api put-bucket-versioning \
    --bucket my-mimir \
    --versioning-configuration Status=Enabled
  ```

#### 구성 백업

Helm values, runtime config, 룰 파일, Alertmanager 설정은 GitOps 방식으로 관리하고 백업한다.

#### Disaster Recovery

- 활성/대기 (Active/Passive)

  - 메인 리전: 활성 클러스터
  - 백업 리전: 대기 클러스터 (데이터 받지 않음)
  - 장애 시: 대기 클러스터에 백업 버킷 마운트, DNS 변경

- 활성/활성 (Active/Active)

  - 두 리전 모두 활성
  - Prometheus가 양쪽으로 push
  - Querier가 두 리전 페더레이션

  - 구성이 복잡하지만 가용성이 가장 높음

<a id="slo-정의-및-추적"></a>

### SLO 정의 및 추적

#### 권장 SLO

- Write Availability: 목표 99.9%(월 약 43분 다운 허용)
- Read Availability: 목표 99.5%
- Write Latency P99: 목표 < 1s
- Query Latency P99: 목표 < 30s
- Data Freshness: 목표 < 1 minute

#### Burn Rate 알림

- 빠른 burn rate (1시간 윈도우):

```yaml
- alert: MimirWriteSLOBurnRateHigh
  expr: |
    (
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[1h]))
      /
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[1h]))
    ) > (1 - 0.999) * 14.4
  for: 2m
  labels:
    severity: critical
```

- 느린 burn rate (6시간 윈도우):

```yaml
- alert: MimirWriteSLOBurnRateMedium
  expr: |
    (
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[6h]))
      /
      sum(rate(cortex_request_duration_seconds_count{route="/api/v1/push"}[6h]))
    ) > (1 - 0.999) * 6
  for: 15m
  labels:
    severity: warning
```

#### Error Budget 추적

```promql
# 30일 error budget 잔여
1 - (
  (
    sum(increase(cortex_request_duration_seconds_count{route="/api/v1/push", status_code=~"5.."}[30d]))
    /
    sum(increase(cortex_request_duration_seconds_count{route="/api/v1/push"}[30d]))
  )
  /
  (1 - 0.999)
)
```

Error Budget이 소진되는 추세를 대시보드에 표시하고, 이를 바탕으로 변경을 진행할지 판단한다.
