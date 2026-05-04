# Tempo 배포

> 이 문서는 Grafana Tempo 공식 문서의 "Set up for tracing" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/setup/

---

## 목차

1. [배포 모드 개요](#배포-모드-개요)
2. [Monolithic 모드](#monolithic-모드)
3. [Microservices 모드](#microservices-모드)
4. [설치 방법](#설치-방법)
5. [Helm으로 설치](#helm으로-설치)
6. [Tempo Operator로 설치](#tempo-operator로-설치)
7. [Tanka로 설치](#tanka로-설치)
8. [Docker로 설치](#docker로-설치)
9. [바이너리 설치](#바이너리-설치)
10. [업그레이드 가이드](#업그레이드-가이드)

---

## 배포 모드 개요

| 모드 | 처리량 | 운영 복잡도 | 권장 환경 |
|------|--------|-----------|----------|
| **Monolithic** | ~수십 GB/일 | 낮음 | 개발, 소규모 |
| **Microservices** | 페타바이트 | 높음 | 프로덕션 |

Tempo는 단일 바이너리에 모든 컴포넌트를 포함하며, `-target` 플래그로 실행할 컴포넌트를 결정합니다.

---

## Monolithic 모드

### 특징

`-target=all` (기본값)로 모든 컴포넌트가 단일 프로세스에서 실행됩니다.

### 장점

- 단순한 배포
- 빠른 시작
- 학습 및 PoC에 이상적

### 단점

- 확장성 제한 (수직 확장만)
- 단일 장애 지점

### 권장 사용 사례

- 로컬 개발 및 테스트
- 소규모 환경 (일일 수십 GB 이하)
- Tempo 평가

---

## Microservices 모드

### 특징

각 컴포넌트가 독립적인 프로세스로 실행됩니다.

```
-target=distributor
-target=ingester
-target=query-frontend
-target=querier
-target=compactor
-target=metrics-generator (선택적)
```

### 장점

- 컴포넌트별 독립 확장
- 정밀한 리소스 관리
- 장애 격리

### 단점

- 복잡한 배포
- 더 많은 운영 부담

### 권장 사용 사례

- 프로덕션 환경
- 대규모 트레이스 처리
- 멀티 테넌트 환경

---

## 설치 방법

| 방법 | 지원 모드 | 환경 |
|------|---------|------|
| **Docker** | Monolithic | 로컬, 단순 환경 |
| **Helm** | 둘 다 | Kubernetes |
| **Tempo Operator** | Microservices | Kubernetes |
| **Tanka** | Microservices | Kubernetes (Jsonnet) |
| **Linux 바이너리** | Monolithic | Linux 호스트 |

### 보안 주의사항

> Tempo는 인증 레이어를 내장하고 있지 않습니다. nginx 등의 인증 리버스 프록시를 앞단에 배치하여 무단 접근을 방지해야 합니다.

---

## Helm으로 설치

### Monolithic 모드 (tempo 차트)

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install tempo grafana/tempo --values values.yaml
```

```yaml
# values.yaml (monolithic)
tempo:
  storage:
    trace:
      backend: s3
      s3:
        bucket: my-tempo-bucket
        endpoint: s3.amazonaws.com
        region: us-east-1
        access_key: ${AWS_ACCESS_KEY_ID}
        secret_key: ${AWS_SECRET_ACCESS_KEY}
  
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
```

### Microservices 모드 (tempo-distributed 차트)

```bash
helm install tempo grafana/tempo-distributed --values values.yaml
```

```yaml
# values.yaml (distributed)
storage:
  trace:
    backend: s3
    s3:
      bucket: my-tempo-bucket
      endpoint: s3.amazonaws.com
      region: us-east-1

distributor:
  replicas: 3

ingester:
  replicas: 3
  persistence:
    enabled: true
    size: 50Gi

queryFrontend:
  replicas: 2

querier:
  replicas: 3

compactor:
  replicas: 1

metricsGenerator:
  enabled: true
  replicas: 2
  config:
    storage:
      remote_write:
        - url: http://mimir:9009/api/v1/push
          headers:
            X-Scope-OrgID: tempo
```

---

## Tempo Operator로 설치

### 설치

```bash
kubectl apply -f https://github.com/grafana/tempo-operator/releases/latest/download/tempo-operator.yaml
```

### TempoStack CR

```yaml
apiVersion: tempo.grafana.com/v1alpha1
kind: TempoStack
metadata:
  name: prod
  namespace: tempo
spec:
  storage:
    secret:
      name: tempo-storage
      type: s3
  
  resources:
    total:
      limits:
        memory: 8Gi
        cpu: 4
  
  template:
    distributor:
      replicas: 3
    ingester:
      replicas: 3
    querier:
      replicas: 2
    queryFrontend:
      replicas: 2
    compactor:
      replicas: 1
```

### Storage Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tempo-storage
  namespace: tempo
type: Opaque
stringData:
  endpoint: s3.amazonaws.com
  bucket: my-tempo-bucket
  region: us-east-1
  access_key_id: ${AWS_ACCESS_KEY_ID}
  access_key_secret: ${AWS_SECRET_ACCESS_KEY}
```

---

## Tanka로 설치

```bash
mkdir tempo-prod && cd tempo-prod
tk init --k8s=1.27
jb install github.com/grafana/tempo/operations/jsonnet/microservices
```

### main.jsonnet 예시

```jsonnet
local tempo = import 'microservices/tempo.libsonnet';

tempo {
  _config+:: {
    namespace: 'tempo',
    
    backend: 's3',
    bucket: 'my-tempo-bucket',
    
    receivers: {
      otlp: {
        protocols: {
          grpc: {},
          http: {},
        },
      },
    },
    
    distributor+: { replicas: 3 },
    ingester+: { replicas: 3 },
    querier+: { replicas: 2 },
  },
}
```

---

## Docker로 설치

### Docker Compose 예시

```yaml
version: "3"
services:
  tempo:
    image: grafana/tempo:latest
    command: ["-config.file=/etc/tempo.yaml"]
    volumes:
      - ./tempo.yaml:/etc/tempo.yaml
      - tempo-data:/tmp/tempo
    ports:
      - "3200:3200"   # tempo HTTP
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "9095:9095"   # tempo gRPC
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin

volumes:
  tempo-data:
```

### tempo.yaml (단순 예시)

```yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318

ingester:
  trace_idle_period: 10s
  max_block_duration: 5m

compactor:
  compaction:
    block_retention: 720h   # 30일

storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/blocks
    wal:
      path: /tmp/tempo/wal
```

---

## 바이너리 설치

```bash
# 다운로드
curl -O -L "https://github.com/grafana/tempo/releases/download/v2.4.0/tempo_2.4.0_linux_amd64.tar.gz"
tar -xvzf tempo_2.4.0_linux_amd64.tar.gz
chmod +x tempo
mv tempo /usr/local/bin/

# 실행
tempo -config.file=tempo.yaml
```

### systemd 서비스

```ini
# /etc/systemd/system/tempo.service
[Unit]
Description=Grafana Tempo
After=network.target

[Service]
ExecStart=/usr/local/bin/tempo -config.file=/etc/tempo/tempo.yaml
Restart=on-failure
User=tempo

[Install]
WantedBy=multi-user.target
```

---

## 업그레이드 가이드

### 권장 업그레이드 순서 (Microservices)

1. **Compactor**
2. **Query Frontend**
3. **Querier**
4. **Distributor**
5. **Ingester** (가장 신중)

### 호환성

- Patch (X.Y.Z): 안전
- Minor (X.Y): 일반적으로 안전, 릴리스 노트 확인
- Major (X): 마이그레이션 필요할 수 있음 (Parquet 스키마 변경 등)

### Parquet 스키마 업그레이드

```yaml
storage:
  trace:
    block:
      version: vParquet4   # 새 버전
```

기존 블록은 Compactor가 점진적으로 변환.
