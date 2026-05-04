# Pyroscope 배포 모드

> 이 문서는 Pyroscope의 배포 옵션과 환경별 권장 구성을 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/deploy/

---

## 목차

1. [배포 모드 개요](#배포-모드-개요)
2. [Monolithic 모드](#monolithic-모드)
3. [Microservices 모드](#microservices-모드)
4. [Kubernetes 배포 (Helm)](#kubernetes-배포-helm)
5. [Docker / Docker Compose](#docker--docker-compose)
6. [규모별 권장 구성](#규모별-권장-구성)
7. [용량 산정](#용량-산정)

---

## 배포 모드 개요

Pyroscope는 단일 바이너리이며 실행 시 `-target` 플래그로 어떤 컴포넌트를 활성화할지 결정합니다.

| 모드 | `-target` 값 | 특징 |
|------|--------------|------|
| Monolithic | `all` (기본) | 모든 컴포넌트가 한 프로세스 |
| Read/Write 분리 | `read`, `write` | 읽기/쓰기 경로만 분리 |
| Microservices | `distributor`, `ingester`, ... | 컴포넌트별 독립 프로세스 |

> 동일 바이너리를 다르게 실행하기 때문에 운영 자동화가 단순합니다. (Loki/Mimir와 동일 패턴)

---

## Monolithic 모드

**모든 컴포넌트가 단일 프로세스에서 실행** 됩니다.

### 적합한 환경

- 개발/테스트
- 작은 규모 운영 (단일 인스턴스, 일 인제스트 수십 GB 이하)
- POC / 데모

### 장점

- 단일 바이너리, 단일 포트로 운영 단순
- 외부 의존성 없음 (로컬 파일시스템 또는 단일 S3 버킷만 필요)

### 단점

- 수평 확장 제한
- 컴포넌트별 자원 분리 불가
- HA 구성 시 stateful 부분(Ingester)이 병목

### 실행 예시

```bash
pyroscope -config.file=pyroscope.yaml -target=all
```

---

## Microservices 모드

**각 컴포넌트를 독립 프로세스/디플로이먼트로 운영** 합니다. 대규모 운영의 표준 형태입니다.

### 일반적인 구성

| 컴포넌트 | 상태 | 권장 복제 수 |
|---------|------|--------------|
| Distributor | Stateless | 2~ |
| Ingester | Stateful (PVC) | 3~ (RF에 맞춤) |
| Querier | Stateless | 2~ |
| Query-Frontend | Stateless | 2 |
| Query-Scheduler | Stateless | 2 |
| Store-Gateway | Stateful (캐시) | 2~ |
| Compactor | Stateful | 1~ |

### 장점

- 컴포넌트별로 자원/스케일 정책 독립 적용
- 무중단 롤링 업데이트 용이
- 대규모 멀티 테넌트 운영에 최적

### 단점

- 운영 복잡도 ↑
- 네트워크 트래픽 증가
- 모니터링/디버깅 채널 다양

---

## Kubernetes 배포 (Helm)

Grafana는 공식 Helm 차트를 제공합니다.

### 차트 추가

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

### Monolithic 설치 예

```yaml
# values.yaml
pyroscope:
  structuredConfig:
    storage:
      backend: s3
      s3:
        bucket_name: my-pyroscope
        endpoint: s3.amazonaws.com
        region: us-east-1
        access_key_id: ...
        secret_access_key: ...

  components:
    querier:
      kind: Deployment
      replicaCount: 1
    distributor:
      kind: Deployment
      replicaCount: 1
    ingester:
      kind: StatefulSet
      replicaCount: 1
```

```bash
helm install pyroscope grafana/pyroscope -f values.yaml
```

### Microservices 설치 예

```yaml
pyroscope:
  components:
    distributor:
      kind: Deployment
      replicaCount: 3
    ingester:
      kind: StatefulSet
      replicaCount: 3
    querier:
      kind: Deployment
      replicaCount: 3
    query-frontend:
      kind: Deployment
      replicaCount: 2
    query-scheduler:
      kind: Deployment
      replicaCount: 2
    store-gateway:
      kind: StatefulSet
      replicaCount: 3
    compactor:
      kind: StatefulSet
      replicaCount: 1
```

### 영구 볼륨

- Ingester, Store-Gateway, Compactor는 PVC가 필요합니다.
- Ingester PVC 크기는 보통 10~100Gi 사이 (보유 시간 + 인제스트 속도)

### Ingress / Gateway

- `gateway` (nginx 기반) 컴포넌트로 단일 진입점 제공 가능
- 인증은 통상 외부 Gateway(예: Grafana Cloud, Auth Proxy)에서 처리

---

## Docker / Docker Compose

### 단일 컨테이너

```bash
docker run -d \
  -p 4040:4040 \
  -v $(pwd)/pyroscope.yaml:/etc/pyroscope/config.yaml \
  -v pyroscope-data:/data \
  grafana/pyroscope:latest \
  -config.file=/etc/pyroscope/config.yaml
```

### Docker Compose (Monolithic + Grafana)

```yaml
version: '3.9'
services:
  pyroscope:
    image: grafana/pyroscope:latest
    ports:
      - "4040:4040"
    volumes:
      - ./pyroscope.yaml:/etc/pyroscope.yaml
      - pyroscope-data:/data
    command: ["-config.file=/etc/pyroscope.yaml"]

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_INSTALL_PLUGINS=grafana-pyroscope-app
volumes:
  pyroscope-data:
```

---

## 규모별 권장 구성

### 작은 규모 (일 인제스트 < 50GB)

- **모드**: Monolithic, 1 노드
- **스토리지**: 로컬 디스크 또는 단일 S3 버킷
- **CPU/MEM**: 4 vCPU / 8GB RAM
- 모니터링은 자기 자신을 Prometheus로 스크랩하는 정도

### 중간 규모 (일 인제스트 50GB ~ 1TB)

- **모드**: Microservices
- **Ingester**: 3 인스턴스 (RF=3)
- **Querier**: 2~4 인스턴스
- **Store-Gateway**: 2 인스턴스
- **Object Storage**: S3/GCS 권장
- 데모용 단일 노드와 비교해 5~10× 자원이 필요

### 대규모 (일 인제스트 > 1TB)

- 본격적인 Microservices + Shuffle Sharding
- Ingester를 zone-aware로 배포 (예: 3 AZ × N replicas)
- Query-Scheduler 도입
- Compactor 다중 인스턴스
- 캐시 (memcached) 권장

---

## 용량 산정

대략적인 가이드라인입니다. 실제 환경에서는 테스트가 필수입니다.

### 시리즈 카디널리티

- 시리즈 = (서비스 × 인스턴스 × 라벨 조합)
- 1만~10만 시리즈는 통상 단일 클러스터에서 처리 가능

### 인제스트 (Ingest) 용량

- 일반적으로 1Mi 프로파일/초 당 1~2 vCPU 필요 (Distributor + Ingester)
- WAL/메모리 사용은 시리즈 수에 비례

### 스토리지

- 압축 후 일 보유 데이터 = 일 인제스트 × 압축률(0.2~0.5)
- 30일 보유 시 = 일 보유 데이터 × 30
- 오브젝트 스토리지 비용이 주된 항목

### 쿼리

- Querier는 메모리에 블록 일부를 캐시
- 캐시 미스 시 S3 다운로드 시간이 응답 시간 좌우
- 메모리 8GB+ 권장

---

## 다음 단계

- [04_profile_types.md](./04_profile_types.md) - 어떤 프로파일을 보낼지
- [06_instrumentation.md](./06_instrumentation.md) - 클라이언트 계측
- [09_configuration.md](./09_configuration.md) - 상세 설정
- [07_manage.md](./07_manage.md) - 운영 모범사례
