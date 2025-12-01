# Kubernetes 설정 관리

> 공식 문서: https://kubernetes.io/docs/concepts/configuration/

## 개요

Kubernetes는 ConfigMap과 Secret을 통해 애플리케이션 설정과 민감 정보를 컨테이너 이미지와 분리하여 관리합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      설정 관리 전략                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────┐  ┌──────────────────────────────┐ │
│  │      ConfigMap          │  │          Secret              │ │
│  │  (비민감 설정 데이터)     │  │  (민감한 정보)               │ │
│  │                         │  │                              │ │
│  │  • 환경 변수            │  │  • 비밀번호                   │ │
│  │  • 설정 파일            │  │  • API 키                    │ │
│  │  • 명령행 인수          │  │  • TLS 인증서                │ │
│  └─────────────────────────┘  └──────────────────────────────┘ │
│              │                            │                    │
│              └────────────┬───────────────┘                    │
│                           │                                    │
│                           ▼                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                        Pod                                │ │
│  │  ┌─────────────────┐  ┌─────────────────┐                │ │
│  │  │   환경 변수      │  │   볼륨 마운트    │                │ │
│  │  └─────────────────┘  └─────────────────┘                │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## ConfigMap

ConfigMap은 비밀이 아닌 데이터를 키-값 쌍으로 저장하는 API 객체입니다.

### 기본 구조

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: default
data:
  # 단순 키-값
  DATABASE_HOST: "postgres.default.svc.cluster.local"
  DATABASE_PORT: "5432"
  LOG_LEVEL: "info"

  # 파일 형태의 설정
  config.yaml: |
    server:
      port: 8080
      host: 0.0.0.0
    logging:
      level: info
      format: json

  nginx.conf: |
    server {
        listen 80;
        server_name localhost;
        location / {
            root /usr/share/nginx/html;
        }
    }

binaryData:
  # base64 인코딩된 바이너리 데이터
  favicon.ico: aWNvbiBkYXRh...
```

### ConfigMap 생성 방법

#### 1. YAML 파일로 생성

```bash
kubectl apply -f configmap.yaml
```

#### 2. 명령어로 생성

```bash
# 리터럴 값으로
kubectl create configmap my-config \
  --from-literal=key1=value1 \
  --from-literal=key2=value2

# 파일로
kubectl create configmap my-config \
  --from-file=config.yaml \
  --from-file=nginx.conf

# 디렉토리로
kubectl create configmap my-config \
  --from-file=/path/to/config/dir/

# 환경 파일로
kubectl create configmap my-config \
  --from-env-file=app.env
```

### ConfigMap 사용 방법

#### 1. 환경 변수로 주입 (개별 키)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    - name: DATABASE_HOST
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: DATABASE_HOST
    - name: DATABASE_PORT
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: DATABASE_PORT
          optional: true  # ConfigMap이 없어도 Pod 시작
```

#### 2. 환경 변수로 주입 (전체)

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    envFrom:
    - configMapRef:
        name: app-config
        optional: true
    - prefix: APP_  # 접두사 추가: APP_DATABASE_HOST
      configMapRef:
        name: app-config
```

#### 3. 볼륨으로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config-volume
      mountPath: /etc/config
      readOnly: true
  volumes:
  - name: config-volume
    configMap:
      name: app-config
```

#### 4. 특정 키만 마운트

```yaml
volumes:
- name: config-volume
  configMap:
    name: app-config
    items:
    - key: config.yaml
      path: app-config.yaml  # /etc/config/app-config.yaml
    - key: nginx.conf
      path: nginx/nginx.conf  # /etc/config/nginx/nginx.conf
```

#### 5. 특정 파일 권한 설정

```yaml
volumes:
- name: config-volume
  configMap:
    name: app-config
    defaultMode: 0644
    items:
    - key: config.yaml
      path: config.yaml
      mode: 0400  # 개별 파일 권한
```

### ConfigMap 자동 업데이트

| 사용 방법 | 자동 업데이트 |
|----------|--------------|
| 환경 변수 | ❌ (Pod 재시작 필요) |
| 볼륨 마운트 | ✅ (kubelet 동기화 주기에 따라) |
| subPath 마운트 | ❌ |

**업데이트 지연:**
- kubelet 동기화 주기 (기본 1분)
- ConfigMap 캐시 전파 지연
- 총 지연: 동기화 주기 + 캐시 TTL

### Immutable ConfigMap

변경 불가능한 ConfigMap을 생성하여 성능 향상 및 실수 방지

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: immutable-config
immutable: true
data:
  key: value
```

**장점:**
- API 서버 부하 감소 (watch 불필요)
- 실수로 인한 설정 변경 방지

**주의:** immutable 설정 후에는 수정 불가, 삭제 후 재생성 필요

### 제한 사항

| 항목 | 제한 |
|------|------|
| 최대 크기 | 1 MiB |
| 네임스페이스 | 같은 네임스페이스에서만 참조 가능 |
| static Pod | ConfigMap 참조 불가 |

---

## Secret

Secret은 비밀번호, 토큰, 키 같은 민감한 정보를 저장하는 객체입니다.

### Secret 유형

| 유형 | 설명 | 필수 키 |
|------|------|--------|
| Opaque | 기본값, 임의의 사용자 정의 데이터 | - |
| kubernetes.io/tls | TLS 인증서 | tls.crt, tls.key |
| kubernetes.io/dockerconfigjson | Docker 레지스트리 인증 | .dockerconfigjson |
| kubernetes.io/basic-auth | 기본 인증 | username, password |
| kubernetes.io/ssh-auth | SSH 인증 | ssh-privatekey |
| kubernetes.io/service-account-token | 서비스 계정 토큰 | - |
| bootstrap.kubernetes.io/token | 부트스트랩 토큰 | token-id, token-secret |

### Secret 생성

#### Opaque Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  # base64 인코딩 필요
  username: YWRtaW4=
  password: cGFzc3dvcmQxMjM=
stringData:
  # 평문으로 작성 (저장 시 자동 base64 인코딩)
  api-key: my-api-key-12345
```

**base64 인코딩:**
```bash
echo -n 'admin' | base64
# YWRtaW4=

echo -n 'password123' | base64
# cGFzc3dvcmQxMjM=
```

#### TLS Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tls-secret
type: kubernetes.io/tls
data:
  tls.crt: <base64 encoded certificate>
  tls.key: <base64 encoded private key>
```

```bash
# 명령어로 생성
kubectl create secret tls tls-secret \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

#### Docker Registry Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: docker-registry-secret
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: <base64 encoded docker config>
```

```bash
# 명령어로 생성
kubectl create secret docker-registry my-registry \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=myuser \
  --docker-password=mypassword \
  --docker-email=myemail@example.com
```

**Pod에서 사용:**
```yaml
spec:
  imagePullSecrets:
  - name: my-registry
```

### Secret 사용 방법

#### 1. 환경 변수로 주입

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: password
    - name: API_KEY
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: api-key
          optional: false  # 필수 (기본값)
```

#### 2. 전체 Secret을 환경 변수로

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    envFrom:
    - secretRef:
        name: app-secrets
```

#### 3. 볼륨으로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: secrets-volume
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets-volume
    secret:
      secretName: app-secrets
      defaultMode: 0400  # 보안을 위해 제한된 권한
```

#### 4. 특정 키만 마운트

```yaml
volumes:
- name: secrets-volume
  secret:
    secretName: app-secrets
    items:
    - key: tls.crt
      path: server.crt
    - key: tls.key
      path: server.key
      mode: 0400
```

### Secret 명령어로 생성

```bash
# 리터럴 값
kubectl create secret generic db-secret \
  --from-literal=username=admin \
  --from-literal=password=secret123

# 파일
kubectl create secret generic ssh-secret \
  --from-file=ssh-privatekey=/path/to/id_rsa

# 환경 파일
kubectl create secret generic env-secret \
  --from-env-file=secrets.env
```

### Immutable Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: immutable-secret
type: Opaque
immutable: true
data:
  key: dmFsdWU=
```

### 보안 고려사항

#### 1. etcd 암호화

기본적으로 Secret은 etcd에 **암호화되지 않은 상태**로 저장됩니다.

```yaml
# EncryptionConfiguration 예시
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
- resources:
  - secrets
  providers:
  - aescbc:
      keys:
      - name: key1
        secret: <base64-encoded-key>
  - identity: {}
```

#### 2. RBAC 설정

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get", "list"]
  resourceNames: ["allowed-secret"]  # 특정 Secret만 허용
```

#### 3. 네임스페이스 분리

민감한 Secret은 별도 네임스페이스에 격리

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: secrets-ns
  labels:
    pod-security.kubernetes.io/enforce: restricted
```

#### 4. 외부 Secret 관리 도구

| 도구 | 설명 |
|------|------|
| HashiCorp Vault | 가장 널리 사용되는 시크릿 관리 도구 |
| AWS Secrets Manager | AWS 네이티브 솔루션 |
| Azure Key Vault | Azure 네이티브 솔루션 |
| GCP Secret Manager | GCP 네이티브 솔루션 |
| External Secrets Operator | 외부 시크릿 저장소와 Kubernetes 연동 |

### Secret vs ConfigMap 비교

| 항목 | ConfigMap | Secret |
|------|-----------|--------|
| 용도 | 비민감 설정 데이터 | 민감한 정보 |
| 인코딩 | 평문 | base64 |
| 최대 크기 | 1 MiB | 1 MiB |
| etcd 저장 | 평문 | 평문 (암호화 설정 가능) |
| RBAC | 일반 접근 | 제한적 접근 권장 |
| 메모리 | 일반 | tmpfs (선택적) |

---

## 실전 패턴

### 1. 환경별 설정 분리

```yaml
# base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  LOG_LEVEL: info

# overlays/dev/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  LOG_LEVEL: debug
  DEBUG_MODE: "true"

# overlays/prod/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  LOG_LEVEL: warn
```

### 2. 설정 파일 교체

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx:1.25
    volumeMounts:
    - name: config
      mountPath: /etc/nginx/nginx.conf
      subPath: nginx.conf
  volumes:
  - name: config
    configMap:
      name: nginx-config
```

### 3. 초기화 설정

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-init
spec:
  initContainers:
  - name: init-config
    image: busybox
    command: ['sh', '-c', 'cp /config-src/* /config/']
    volumeMounts:
    - name: config-src
      mountPath: /config-src
    - name: config
      mountPath: /config
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config
      mountPath: /etc/config
  volumes:
  - name: config-src
    configMap:
      name: app-config
  - name: config
    emptyDir: {}
```

### 4. Projected Volume으로 통합

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-projected
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: all-configs
      mountPath: /etc/app
  volumes:
  - name: all-configs
    projected:
      sources:
      - configMap:
          name: app-config
          items:
          - key: config.yaml
            path: config.yaml
      - secret:
          name: app-secrets
          items:
          - key: credentials
            path: credentials.json
      - downwardAPI:
          items:
          - path: pod-name
            fieldRef:
              fieldPath: metadata.name
```

---

## 참고 자료

- [ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Encrypting Secret Data at Rest](https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/)
- [Managing Secrets using kubectl](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-kubectl/)
