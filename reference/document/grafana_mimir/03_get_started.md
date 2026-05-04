# Mimir 시작 가이드

> 이 문서는 Grafana Mimir 공식 문서의 "Get started" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/get-started/

---

## 목차

1. [개요](#개요)
2. [사전 요구사항](#사전-요구사항)
3. [단일 프로세스로 시작](#단일-프로세스로-시작)
4. [Docker Compose로 시작](#docker-compose로-시작)
5. [Prometheus 연동](#prometheus-연동)
6. [Grafana Alloy 연동](#grafana-alloy-연동)
7. [Grafana 데이터 소스 추가](#grafana-데이터-소스-추가)
8. [첫 메트릭 쿼리](#첫-메트릭-쿼리)
9. [다음 단계](#다음-단계)

---

## 개요

이 가이드는 Mimir를 빠르게 시작하기 위한 단계를 다룹니다.

### 두 가지 시작 방법

| 방법 | 환경 | 시간 |
|------|------|------|
| **명령형 (Imperative)** | 단일 Mimir 프로세스 | 5분 |
| **선언형 (Declarative)** | Docker Compose 다중 프로세스 | 10분 |

---

## 사전 요구사항

- 64-bit 시스템
- 2 CPU 코어, 4GB RAM 이상
- Docker (Compose 방식 사용 시)
- Prometheus 또는 Grafana Alloy

---

## 단일 프로세스로 시작

### 1. Mimir 다운로드

```bash
# Linux x86_64
curl -fLo mimir https://github.com/grafana/mimir/releases/latest/download/mimir-linux-amd64
chmod +x mimir
```

### 2. 구성 파일 작성

```yaml
# demo.yaml
multitenancy_enabled: false

blocks_storage:
  backend: filesystem
  bucket_store:
    sync_dir: /tmp/mimir/tsdb-sync
  filesystem:
    dir: /tmp/mimir/data/tsdb
  tsdb:
    dir: /tmp/mimir/tsdb

compactor:
  data_dir: /tmp/mimir/compactor
  sharding_ring:
    kvstore:
      store: memberlist

distributor:
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: memberlist

ingester:
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: memberlist
    replication_factor: 1

ruler_storage:
  backend: filesystem
  filesystem:
    dir: /tmp/mimir/rules

server:
  http_listen_port: 9009
  log_level: error

store_gateway:
  sharding_ring:
    replication_factor: 1
```

### 3. Mimir 실행

```bash
./mimir --config.file=demo.yaml
```

기본 포트는 9009입니다.

### 4. 헬스체크

```bash
curl http://localhost:9009/ready
```

---

## Docker Compose로 시작

### docker-compose.yml

```yaml
version: "3.8"

services:
  mimir-1:
    image: grafana/mimir:latest
    command: ["-config.file=/etc/mimir.yaml"]
    hostname: mimir-1
    depends_on:
      - minio
    volumes:
      - ./config/mimir.yaml:/etc/mimir.yaml
      - ./config/alertmanager-fallback-config.yaml:/etc/alertmanager-fallback-config.yaml
    networks:
      - mimir
    ports:
      - "8001:8080"

  mimir-2:
    image: grafana/mimir:latest
    command: ["-config.file=/etc/mimir.yaml"]
    hostname: mimir-2
    depends_on:
      - minio
    volumes:
      - ./config/mimir.yaml:/etc/mimir.yaml
      - ./config/alertmanager-fallback-config.yaml:/etc/alertmanager-fallback-config.yaml
    networks:
      - mimir
    ports:
      - "8002:8080"

  mimir-3:
    image: grafana/mimir:latest
    command: ["-config.file=/etc/mimir.yaml"]
    hostname: mimir-3
    depends_on:
      - minio
    volumes:
      - ./config/mimir.yaml:/etc/mimir.yaml
      - ./config/alertmanager-fallback-config.yaml:/etc/alertmanager-fallback-config.yaml
    networks:
      - mimir
    ports:
      - "8003:8080"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: mimir
      MINIO_ROOT_PASSWORD: supersecret
    volumes:
      - minio-data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    networks:
      - mimir
  
  load-balancer:
    image: nginx:latest
    volumes:
      - ./config/nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "9009:9009"
    networks:
      - mimir
    depends_on:
      - mimir-1
      - mimir-2
      - mimir-3
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
      - GF_USERS_DEFAULT_THEME=light
    networks:
      - mimir

networks:
  mimir:

volumes:
  minio-data:
```

### config/mimir.yaml

```yaml
multitenancy_enabled: false

common:
  storage:
    backend: s3
    s3:
      endpoint: minio:9000
      access_key_id: mimir
      secret_access_key: supersecret
      insecure: true
      bucket_name: mimir

blocks_storage:
  s3:
    bucket_name: mimir-blocks

ruler_storage:
  s3:
    bucket_name: mimir-ruler

alertmanager_storage:
  s3:
    bucket_name: mimir-alertmanager

server:
  log_level: warn
  http_listen_port: 8080

distributor:
  ring:
    instance_addr: ${POD_IP:-127.0.0.1}
    kvstore:
      store: memberlist

ingester:
  ring:
    replication_factor: 3
    kvstore:
      store: memberlist

memberlist:
  join_members:
    - mimir-1:7946
    - mimir-2:7946
    - mimir-3:7946
```

### config/nginx.conf

```nginx
events {
  worker_connections 1024;
}
http {
  upstream mimir {
    server mimir-1:8080;
    server mimir-2:8080;
    server mimir-3:8080;
  }
  server {
    listen 9009;
    location / {
      proxy_pass http://mimir;
    }
  }
}
```

### 실행

```bash
docker compose up -d
```

---

## Prometheus 연동

### Prometheus 구성

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  external_labels:
    cluster: my-cluster

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['localhost:9090']
  
  - job_name: node
    static_configs:
      - targets: ['node-exporter:9100']

remote_write:
  - url: http://mimir:9009/api/v1/push
    headers:
      X-Scope-OrgID: demo  # multitenancy_enabled=true 시 필수
```

### Prometheus 실행

```bash
prometheus --config.file=prometheus.yml
```

---

## Grafana Alloy 연동

### config.alloy

```alloy
prometheus.exporter.unix "node" { }

prometheus.scrape "default" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "15s"
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "demo",
    }
  }
}
```

### Alloy 실행

```bash
alloy run config.alloy
```

---

## Grafana 데이터 소스 추가

### UI에서 추가

1. Grafana 접속 (http://localhost:3000)
2. Connections → Data sources → Add new data source
3. **Prometheus** 선택
4. 설정:
   - Name: `Mimir`
   - URL: `http://mimir:9009/prometheus`
   - Custom HTTP Headers: `X-Scope-OrgID: demo` (멀티 테넌시 사용 시)
5. **Save & test**

### 프로비저닝 (자동 설정)

```yaml
# datasources.yaml
apiVersion: 1
datasources:
  - name: Mimir
    type: prometheus
    access: proxy
    url: http://mimir:9009/prometheus
    jsonData:
      httpHeaderName1: X-Scope-OrgID
    secureJsonData:
      httpHeaderValue1: demo
    isDefault: true
```

---

## 첫 메트릭 쿼리

### Grafana Explore에서

1. Grafana → Explore
2. 데이터 소스: **Mimir** 선택
3. 쿼리 입력:
   ```promql
   up
   ```
4. **Run query**

### API로 직접

```bash
# 현재 값 (Instant query)
curl -G "http://mimir:9009/prometheus/api/v1/query" \
  -H "X-Scope-OrgID: demo" \
  --data-urlencode 'query=up'

# 시간 범위 (Range query)
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: demo" \
  --data-urlencode 'query=rate(node_cpu_seconds_total[5m])' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

---

## 다음 단계

- [04_install_configure.md](./04_install_configure.md) — 프로덕션 설치 (Helm, Tanka)
- [05_send_metrics.md](./05_send_metrics.md) — 메트릭 전송 (Prometheus, Alloy, OTel)
- [06_promql.md](./06_promql.md) — PromQL 쿼리
- [07_alertmanager.md](./07_alertmanager.md) — 알림 관리
- [08_manage.md](./08_manage.md) — 운영
