# Loki 설치 및 설정

> 이 문서는 Grafana Loki 공식 문서의 "Set up Loki" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/setup/install/

---

## 목차

1. [설치 방법 개요](#설치-방법-개요)
2. [Helm을 사용한 설치 (권장)](#helm을-사용한-설치-권장)
3. [Tanka를 사용한 설치](#tanka를-사용한-설치)
4. [Docker를 사용한 설치](#docker를-사용한-설치)
5. [로컬 바이너리 설치](#로컬-바이너리-설치)
6. [소스에서 빌드](#소스에서-빌드)
7. [업그레이드 가이드](#업그레이드-가이드)
8. [마이그레이션](#마이그레이션)

---

## 설치 방법 개요

Loki는 다음 5가지 방법으로 설치할 수 있습니다.

| 방법 | 권장 환경 | 난이도 |
|------|----------|--------|
| **Helm** | 프로덕션 (Kubernetes) | 중간 (권장) |
| **Tanka** | 프로덕션 (Kubernetes, Jsonnet) | 중상 |
| **Docker / Docker Compose** | 개발, 테스트, 소규모 | 쉬움 |
| **로컬 바이너리** | 개발, 학습 | 쉬움 |
| **소스 빌드** | 개발 기여 | 상 |

### 일반 설치 흐름

1. **Loki 다운로드 및 설치**
2. **구성 파일 준비** (배포 모드, 스토리지, 보존 등)
3. **(보안) 인증/리버스 프록시 설정** — Loki는 자체 인증을 제공하지 않음
4. **Loki 시작**
5. **Alloy(또는 다른 클라이언트) 설치 및 구성**
6. **Alloy 시작**
7. **Grafana에 Loki 데이터 소스 추가**

> **보안 경고**: Loki는 인증 기능이 내장되어 있지 않습니다. nginx, Caddy 같은 리버스 프록시를 앞단에 배치하여 보안을 확보하거나, Grafana Cloud Loki를 사용해야 합니다.

---

## Helm을 사용한 설치 (권장)

### 사전 요구사항

- Kubernetes 클러스터 (1.20 이상)
- Helm 3
- 오브젝트 스토리지 (S3, GCS, Azure Blob 등)

### Helm 차트 종류

| 차트 | 용도 |
|------|------|
| `loki` | Simple Scalable Deployment (SSD) — 기본값 |
| `loki-distributed` | Microservices 모드 |
| `loki-stack` | Loki + Promtail + Grafana 통합 (Deprecated) |

### 설치 단계

```bash
# 1. Helm 저장소 추가
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# 2. values.yaml 작성 (예시는 아래)

# 3. 네임스페이스 생성
kubectl create namespace loki

# 4. 설치
helm install loki grafana/loki \
  --namespace loki \
  --values values.yaml
```

### values.yaml 최소 예시 (S3 사용)

```yaml
loki:
  schemaConfig:
    configs:
      - from: "2024-01-01"
        store: tsdb
        object_store: s3
        schema: v13
        index:
          prefix: loki_index_
          period: 24h
  storage:
    type: s3
    s3:
      endpoint: s3.amazonaws.com
      region: us-east-1
      bucketnames: my-loki-bucket
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}

deploymentMode: SimpleScalable

read:
  replicas: 3
write:
  replicas: 3
backend:
  replicas: 3

gateway:
  enabled: true
  replicas: 2
```

### 검증

```bash
# Pod 상태 확인
kubectl -n loki get pods

# 게이트웨이 포트 포워딩
kubectl -n loki port-forward svc/loki-gateway 3100:80

# 헬스체크
curl http://localhost:3100/ready
```

---

## Tanka를 사용한 설치

### 사전 요구사항

- Kubernetes 클러스터
- [Tanka](https://tanka.dev/) 및 jsonnet-bundler

### 설치 단계

```bash
# 1. Tanka 프로젝트 초기화
mkdir loki-prod && cd loki-prod
tk init --k8s=1.27

# 2. Loki Jsonnet 라이브러리 설치
jb install github.com/grafana/loki/production/ksonnet/loki

# 3. 환경 디렉토리에서 main.jsonnet 작성
```

### main.jsonnet 예시

```jsonnet
local loki = import 'loki/loki.libsonnet';

loki {
  _config+:: {
    namespace: 'loki',
    htpasswd_contents: 'user:$apr1$...',  // basic auth
    storage_backend: 's3',
    s3_bucket_name: 'my-loki-bucket',
    s3_access_key_id: '...',
    s3_secret_access_key: '...',
  },
}
```

### 배포

```bash
tk apply environments/prod
```

---

## Docker를 사용한 설치

### 단일 컨테이너

```bash
# 구성 파일 다운로드
wget https://raw.githubusercontent.com/grafana/loki/main/cmd/loki/loki-local-config.yaml -O loki-config.yaml

# 컨테이너 실행
docker run -d \
  --name=loki \
  -p 3100:3100 \
  -v $(pwd)/loki-config.yaml:/etc/loki/local-config.yaml \
  grafana/loki:latest \
  -config.file=/etc/loki/local-config.yaml
```

### Docker Compose 예시

```yaml
version: "3"
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yaml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml

  alloy:
    image: grafana/alloy:latest
    volumes:
      - ./alloy-config.alloy:/etc/alloy/config.alloy
      - /var/log:/var/log:ro
    command: run --server.http.listen-addr=0.0.0.0:12345 /etc/alloy/config.alloy
    ports:
      - "12345:12345"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin

volumes:
  loki-data:
```

```bash
docker compose up -d
```

---

## 로컬 바이너리 설치

### 다운로드

[GitHub Releases](https://github.com/grafana/loki/releases)에서 OS/아키텍처에 맞는 바이너리 다운로드.

```bash
# Linux x86_64 예시
curl -O -L "https://github.com/grafana/loki/releases/download/v3.0.0/loki-linux-amd64.zip"
unzip loki-linux-amd64.zip
chmod +x loki-linux-amd64
mv loki-linux-amd64 /usr/local/bin/loki
```

### 구성 파일 다운로드

```bash
wget https://raw.githubusercontent.com/grafana/loki/main/cmd/loki/loki-local-config.yaml
```

### 실행

```bash
loki -config.file=loki-local-config.yaml
```

기본 포트는 3100입니다.

### systemd 서비스 등록 (Linux)

```ini
# /etc/systemd/system/loki.service
[Unit]
Description=Grafana Loki
After=network.target

[Service]
ExecStart=/usr/local/bin/loki -config.file=/etc/loki/loki.yaml
Restart=on-failure
User=loki
Group=loki

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now loki
```

---

## 소스에서 빌드

### 사전 요구사항

- Go 1.21+
- Make
- Git

### 빌드

```bash
git clone https://github.com/grafana/loki.git
cd loki
make loki

# 결과: ./cmd/loki/loki
./cmd/loki/loki -config.file=cmd/loki/loki-local-config.yaml
```

### Promtail / 기타 빌드

```bash
make promtail
make logcli
```

---

## 업그레이드 가이드

### 버전 호환성 정책

Loki는 시맨틱 버전을 따릅니다.

- **Major (X.0.0)**: 호환성 깨질 수 있음
- **Minor (x.Y.0)**: 하위 호환 유지
- **Patch (x.y.Z)**: 버그 수정만

### 업그레이드 순서

1. **릴리스 노트 확인** — Breaking changes 확인
2. **백업** — 구성 파일과 가능한 경우 데이터
3. **단계적 업그레이드** — 한 번에 한 마이너 버전씩 권장
4. **순서**:
   - Compactor → Querier → Query Frontend → Ingester → Distributor 순으로 권장
   - Microservices 모드의 경우 컴포넌트 의존 관계 고려

### 주요 업그레이드 시 체크포인트

- **2.x → 3.0**: BoltDB Shipper 폐기, TSDB로 마이그레이션 필요
- **schema_config**: 새 스키마는 미래 시점부터 적용 (`from` 날짜)

---

## 마이그레이션

### 다른 로깅 시스템에서 Loki로

#### Elasticsearch / OpenSearch에서

- 기존 로그를 백필(backfill)하려면 OTel Collector + Loki Exporter
- 새 로그부터 Loki로 보내고 점진적 전환

#### Splunk에서

- Splunk Forwarder를 OTel Collector로 교체
- Splunk to Loki bridge 도구 활용

### Promtail에서 Alloy로

Promtail은 점진적으로 Alloy로 통합됩니다. 마이그레이션:

```bash
# Promtail 구성을 Alloy로 변환
alloy convert --source-format=promtail \
  --output=alloy-config.alloy \
  promtail-config.yaml
```

### 스토리지 백엔드 변경

스토리지 변경 시 `schema_config`에 새 스키마를 추가합니다 (기존 데이터는 그대로).

```yaml
schema_config:
  configs:
    # 기존 설정 (유지)
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h
    # 새 설정 (특정 날짜부터)
    - from: 2024-06-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
```
