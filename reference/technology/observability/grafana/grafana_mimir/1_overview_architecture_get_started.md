# Grafana Mimir 개요, 아키텍처, 시작 가이드

## Grafana Mimir 개요

> 원본: https://grafana.com/docs/mimir/latest/

---

### 목차

1. [Mimir란 무엇인가](#mimir란-무엇인가)
2. [Mimir의 주요 기능](#mimir의-주요-기능)
3. [핵심 개념](#핵심-개념)
4. [Mimir 메트릭 스택](#mimir-메트릭-스택)
5. [Mimir와 Prometheus 비교](#mimir와-prometheus-비교)
6. [데이터 수집 방식](#데이터-수집-방식)

---

### Mimir란 무엇인가

**Grafana Mimir**는 Prometheus 호환 **장기 저장소(long-term storage) 및 메트릭 저장소**입니다. 주요 특징은 다음과 같습니다.

- **오픈소스**: AGPL-3.0 라이선스
- **대규모 확장성**: 단일 클러스터에서 10억(billion) 단위의 활성 시계열 처리 가능
- **고가용성**: 복제(replication)와 무중단 배포 지원
- **멀티 테넌시**: 단일 클러스터에서 여러 테넌트 격리
- **Prometheus 호환**: PromQL, 원격 쓰기(remote write) API 등 완벽 호환
- **저비용**: 오브젝트 스토리지(S3, GCS, Azure Blob) 활용

#### 메트릭이란?

**메트릭**(Metrics)은 시간에 따라 변화하는 숫자 데이터로, 시스템 상태를 정량적으로 나타냅니다.

- **카운터(Counter)**: 누적 증가 값 (예: HTTP 요청 수)
- **게이지(Gauge)**: 임의의 값 (예: 메모리 사용량)
- **히스토그램(Histogram)**: 값의 분포 (예: 응답 시간 분포)
- **서머리(Summary)**: 분위수 추정

---

### Mimir의 주요 기능

#### 거대한 확장성

Mimir는 **단일 클러스터에서 수십억 개의 활성 시계열을 처리**할 수 있도록 설계되었습니다. Cortex의 후속 프로젝트로, 성능과 운영성을 크게 개선했습니다.

#### 고가용성과 무중단 운영

- 모든 컴포넌트가 수평 확장 가능
- Ingester는 복제(기본 3-way)를 통해 데이터 손실 방지
- 무중단 롤링 업데이트 지원
- WAL(Write-Ahead Log)로 장애 복구

#### 멀티 테넌시

- HTTP 헤더 `X-Scope-OrgID`로 테넌트 식별
- 테넌트별 격리된 데이터 저장
- 테넌트별 Rate Limit 및 Quota 적용

#### Prometheus 호환

- **PromQL** 100% 호환
- **Remote Write API** 호환 (Prometheus, Alloy, OTel Collector 등)
- **Remote Read API** 호환
- **Recording / Alerting Rules** 지원
- **Alertmanager** 내장

#### Native Histograms 지원

Prometheus의 Native Histograms를 지원하여, 기존 히스토그램보다 적은 비용으로 고해상도 분포 데이터를 저장할 수 있습니다.

#### 빠른 쿼리

- 쿼리 분할 및 병렬 실행
- 결과 캐싱
- 청크 캐싱
- 인덱스 캐싱

---

### 핵심 개념

#### 시계열 (Time Series)

특정 메트릭과 라벨 집합의 조합으로 식별되는 데이터 포인트 시퀀스입니다.

```
http_requests_total{method="GET", status="200"} 1234 @1700000000
http_requests_total{method="GET", status="200"} 1245 @1700000060
http_requests_total{method="POST", status="201"} 567 @1700000000
```

각 줄은 별개의 시계열입니다.

#### 활성 시계열 (Active Series)

최근 일정 시간(보통 20분) 내에 데이터가 들어온 시계열입니다. Mimir의 가장 중요한 용량 지표입니다.

#### 라벨 (Labels)

메트릭에 붙는 키-값 쌍 메타데이터입니다.

- **메트릭 이름** (`__name__`): `http_requests_total`
- **라벨**: `{method="GET", status="200", instance="server-1"}`

#### 카디널리티 (Cardinality)

라벨 값의 고유 조합 수입니다. 카디널리티가 높을수록 메모리 사용량이 증가하므로, **카디널리티 폭증**(cardinality explosion)을 피해야 합니다.

##### 카디널리티 안티패턴

- 사용자 ID, 요청 ID 같은 고유 식별자를 라벨로 사용
- 무한히 증가하는 값 (예: timestamp)을 라벨로 사용
- 너무 많은 라벨 차원

#### TSDB 블록 (TSDB Blocks)

Mimir는 Prometheus와 동일한 TSDB 블록 형식을 사용합니다. 기본적으로 2시간마다 인-메모리 데이터가 블록으로 컴팩션되어 오브젝트 스토리지에 업로드됩니다.

블록 구조:
```
block_id/
├── chunks/         (실제 시계열 데이터)
├── index           (시계열 인덱스)
├── meta.json       (블록 메타데이터)
└── tombstones      (삭제 마커)
```

---

### Mimir 메트릭 스택

일반적인 Mimir 기반 메트릭 스택은 다음과 같이 구성됩니다.

#### 1. 메트릭 수집 (Metrics Collection)

- **Prometheus** (Remote Write 사용)
- **Grafana Alloy**
- **OpenTelemetry Collector**
- **Grafana Agent** (Deprecated, Alloy로 마이그레이션 권장)

#### 2. Mimir Cluster

메트릭을 수집, 저장, 쿼리합니다.

#### 3. Grafana

메트릭을 시각화하고 대시보드를 구성합니다.

```
[Targets / Apps]
       |
       v (scrape)
[Prometheus / Alloy / OTel Collector]
       |
       v (Remote Write API)
[Mimir Distributor] --hash--> [Ingester]
                                   |
                                   v
                           [Object Storage]
                                   ^
                                   |
                              [Querier]
                                   ^
                                   |
                          [Query Frontend]
                                   ^
                                   |
                              [Grafana]
```

---

### Mimir와 Prometheus 비교

| 항목 | Prometheus | Mimir |
|------|-----------|-------|
| 목적 | 단일 노드 메트릭 시스템 | 분산 장기 저장소 |
| 확장성 | 수직 확장만 | 수평 확장 (페타바이트) |
| HA | 페어링(pair) 또는 Federation | 네이티브 복제 및 클러스터링 |
| 저장소 | 로컬 디스크 (TSDB) | 오브젝트 스토리지 |
| 멀티 테넌시 | 미지원 | 네이티브 지원 |
| 보존 기간 | 설정 가능 (디스크 한계) | 무제한 (오브젝트 스토리지) |
| 쿼리 언어 | PromQL | PromQL (호환) |
| Recording/Alerting | 내장 | 내장 (Ruler) |
| Alertmanager | 별도 | 내장 |

#### 함께 사용하기

Prometheus와 Mimir는 상호 보완적으로 함께 사용합니다.

```
[Prometheus (수집기 역할)] --remote_write--> [Mimir (장기 저장소)]
       |
       v
[로컬 단기 저장 (수일)]
```

또는 Prometheus 없이 Alloy만으로:

```
[Targets] <--scrape-- [Alloy] --remote_write--> [Mimir]
```

---

### 데이터 수집 방식

Mimir는 **Push 방식**으로 메트릭을 수신합니다(Prometheus의 기본 Pull 방식과 달리).

#### Remote Write API

Prometheus의 `remote_write` 설정으로 데이터를 Mimir에 전송합니다.

```yaml
remote_write:
  - url: http://mimir:9009/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

#### 지원 프로토콜

- **Prometheus Remote Write** (snappy 압축 protobuf)
- **OpenTelemetry OTLP** (HTTP/gRPC)
- **Influx Line Protocol** (선택적 활성화)
- **Datadog Agent Protocol** (선택적 활성화)
- **Graphite** (선택적 활성화)

#### 멀티 테넌트 헤더

```http
POST /api/v1/push HTTP/1.1
X-Scope-OrgID: tenant-1
Content-Encoding: snappy
Content-Type: application/x-protobuf

<snappy-compressed protobuf data>
```

---

### 다음 단계

- [02_architecture.md](./02_architecture.md) - Mimir 아키텍처 상세
- [03_components.md](./03_components.md) - 컴포넌트 (Distributor, Ingester, Querier 등)
- [04_deployment.md](./04_deployment.md) - 배포 (Helm, Jsonnet, Tanka)
- [05_promql.md](./05_promql.md) - PromQL 쿼리
- [06_alertmanager.md](./06_alertmanager.md) - 알림 관리

---

## Mimir 아키텍처

> 원본: https://grafana.com/docs/mimir/latest/references/architecture/

---

### 목차

1. [개요](#개요)
2. [배포 모드](#배포-모드)
3. [핵심 컴포넌트](#핵심-컴포넌트)
4. [쓰기 경로](#쓰기-경로)
5. [읽기 경로](#읽기-경로)
6. [스토리지](#스토리지)
7. [Hash Ring](#hash-ring)
8. [멀티 테넌시](#멀티-테넌시)

---

### 개요

Grafana Mimir는 **마이크로서비스 기반 분산 시스템** 으로, 모든 컴포넌트가 수평 확장 가능합니다. Cortex의 후속 프로젝트로 시작되었으며, 더 나은 성능, 운영성, 안정성을 제공합니다.

#### 주요 특징

- **컴포넌트 분리**: 각 컴포넌트는 독립적인 마이크로서비스
- **단일 바이너리**: 모든 컴포넌트가 단일 바이너리에 포함, `-target`으로 선택
- **무공유(Shared-nothing) 아키텍처**: 컴포넌트 간 직접 의존성 최소화
- **수평 확장**: 모든 컴포넌트가 수평 확장 가능

---

### 배포 모드

#### Monolithic Mode

`-target=all` 플래그로 모든 컴포넌트를 단일 프로세스에서 실행. 개발/소규모용.

#### Read-Write Mode

읽기/쓰기/백엔드 세 그룹으로 분리:
- `-target=write`: Distributor, Ingester
- `-target=read`: Query Frontend, Querier
- `-target=backend`: Store Gateway, Compactor, Ruler, Alertmanager 등

#### Microservices Mode

각 컴포넌트를 개별 프로세스로 실행. 대규모 프로덕션용.

---

### 핵심 컴포넌트

#### Distributor

**역할**: Prometheus, Alloy 등 클라이언트로부터 메트릭을 수신하여 Ingester로 분배합니다.

**주요 동작**:
- 들어오는 시계열 검증 (라벨 형식, 시계열 길이 등)
- HA Tracker를 통한 고가용성 Prometheus 페어 처리
- 일관된 해싱(consistent hashing)으로 Ingester 선택
- Replication Factor(기본 3)에 따라 복제
- 테넌트별 Rate Limit 적용

**상태**: Stateless (수평 확장 자유로움)

#### Ingester

**역할**: 메모리에서 메트릭을 수신하여 임시 저장하고, 주기적으로 TSDB 블록 형태로 오브젝트 스토리지에 영속화합니다.

**주요 동작**:
- 메모리 TSDB에 시계열 추가
- WAL(Write-Ahead Log)로 충돌 시 복구
- 일정 주기(기본 2시간)마다 블록을 오브젝트 스토리지로 업로드
- 최근 데이터에 대한 쿼리 응답 (Querier가 직접 호출)

**상태**: Stateful (메모리 내 시계열 보유)

**복제**: Replication Factor 3 (보통)

#### Querier

**역할**: PromQL 쿼리를 실행합니다.

**주요 동작**:
- Ingester에서 최근 데이터 조회 (보통 12시간)
- Store Gateway를 통해 오브젝트 스토리지의 과거 데이터 조회
- 결과 중복 제거(replication 처리)
- PromQL 평가

**상태**: Stateless

#### Query Frontend

**역할**: Querier 앞단의 선택적 컴포넌트로, 쿼리 가속을 담당합니다.

**주요 동작**:
- **쿼리 분할**: 시간 범위가 긴 쿼리를 작은 시간 범위로 분할
- **결과 캐싱**: Memcached/Redis에 결과 캐시
- **쿼리 큐잉**: 페어 큐(per-tenant queue)로 공정성 확보
- **재시도**: 실패 쿼리 자동 재시도

**상태**: Stateless

#### Query Scheduler (선택적)

**역할**: Query Frontend의 큐를 분리하여 대규모 클러스터에서 독립적으로 운용합니다.

**주요 동작**:
- Frontend 디커플링
- 테넌트별 공정 큐
- Frontend와 Querier의 독립적 확장

#### Store Gateway

**역할**: 오브젝트 스토리지에 저장된 TSDB 블록을 조회합니다.

**주요 동작**:
- 블록 메타데이터 캐싱 (메모리)
- 블록의 인덱스 헤더(index-header) 다운로드 및 메모리 매핑
- 청크/인덱스 캐싱 (Memcached)
- 샤딩(blocks sharding)을 통한 확장
- Querier로부터 시계열 조회 요청 처리

**상태**: Stateful (디스크에 인덱스 헤더 저장)

#### Compactor

**역할**: 작은 블록을 더 큰 블록으로 병합하고, 보존 정책에 따라 만료된 데이터를 삭제합니다.

**주요 동작**:
- 동일 시간 범위의 블록 병합 (deduplication)
- 시간 단위로 블록 압축 (2h → 12h → 24h)
- 보존 기간이 지난 블록 삭제
- 블록 인덱스 헤더 생성
- 단일 인스턴스 또는 샤드 분산 모드

**상태**: Stateful

#### Ruler

**역할**: Recording Rule과 Alerting Rule을 평가합니다.

**주요 동작**:
- 룰 그룹을 주기적으로 평가
- Recording Rules 결과를 Distributor로 전송하여 저장
- Alerting Rules 평가 후 Alertmanager로 알림 전송
- 샤딩을 통한 룰 분산 처리

**상태**: Stateful (룰 평가 상태)

#### Alertmanager

**역할**: Mimir에 내장된 Prometheus Alertmanager로, 알림을 수신하고 관리합니다.

**주요 동작**:
- 알림 그룹화, 억제, 무시 처리
- 알림 채널로 전송 (Slack, PagerDuty, Email 등)
- 멀티 테넌트 지원 (테넌트별 알림 설정)

#### Optional / Helper Components

##### Overrides Exporter
테넌트별 한도(limit) 메트릭을 노출.

##### Query-tee
운영 중 새 버전과 기존 버전의 쿼리 결과를 비교하기 위한 도구.

##### Mimir Continuous Test
Mimir 클러스터의 정상성을 지속적으로 검증.

##### Mimirtool
CLI 도구로 룰 관리, 분석, 변환 등 수행.

---

### 쓰기 경로

```
[Prometheus / Alloy / OTel Collector]
            |
            v (Remote Write API)
       [Distributor]
            |
            v (consistent hash by series labels)
   [Ingester (replication factor=3)]
            |
            v (memory TSDB)
            |
            v (every 2h: upload block)
   [Object Storage (S3/GCS/Azure)]
            |
            v (compaction)
       [Compactor]
            |
            v
   [Larger compacted blocks]
```

#### 단계별 흐름

1. 클라이언트가 Remote Write API로 시계열 푸시
2. Distributor가 검증 및 라벨 정렬
3. 시계열별 일관된 해시로 Ingester 선택
4. 복제 계수만큼 Ingester(들)에 병렬 전송
5. Ingester는 메모리 TSDB에 추가 (WAL 기록)
6. 2시간마다 블록 생성 후 오브젝트 스토리지 업로드
7. 업로드 후 메모리에서 제거 (Querier가 Store Gateway에서 조회)

---

### 읽기 경로

```
[Grafana]
    |
    v (PromQL HTTP API)
[Query Frontend]
    |
    v (split & cache check)
[Query Scheduler]
    |
    v (queue per tenant)
[Querier]
    |
    +-> [Ingester]      (최근 데이터, ~12h)
    |
    +-> [Store Gateway] (과거 데이터)
    |        |
    |        v
    |   [Object Storage]
    |
    v (deduplication & merge)
[Result] -> [Frontend cache] -> [Grafana]
```

#### 단계별 흐름

1. Grafana가 PromQL 쿼리 전송
2. Query Frontend가 시간 범위로 분할, 캐시 확인
3. 캐시 미스 시 Query Scheduler로 큐잉
4. Querier가 큐에서 작업 획득
5. Ingester(최근 데이터)와 Store Gateway(과거 데이터) 병렬 조회
6. 복제본 중복 제거
7. PromQL 평가 후 결과 반환
8. Frontend가 결과 캐시 후 Grafana로 응답

---

### 스토리지

#### TSDB 블록

Mimir는 Prometheus와 동일한 **TSDB 블록 형식** 을 사용합니다.

```
<bucket>/<tenant>/<block_id>/
  ├── chunks/
  │   ├── 000001
  │   ├── 000002
  │   └── ...
  ├── index           (시계열 인덱스, postings)
  ├── meta.json       (블록 메타데이터)
  └── tombstones      (삭제 마커)
```

#### 블록 라이프사이클

```
[Ingester memory] -2h-> [2h block] -compaction-> [12h block] -compaction-> [24h block] -...-
```

#### Bucket Index

테넌트별로 `bucket-index.json.gz` 파일이 유지됩니다. 해당 테넌트의 모든 블록 목록을 포함하며, Querier와 Store Gateway가 블록 목록을 빠르게 조회할 수 있도록 합니다.

#### 캐시

Mimir는 다음과 같은 캐시를 지원합니다.

| 캐시 종류 | 대상 | 백엔드 |
|----------|------|--------|
| Results Cache | 쿼리 결과 | Memcached/Redis |
| Chunks Cache | 청크 데이터 | Memcached/Redis |
| Metadata Cache | 메타데이터 | Memcached/Redis |
| Index Cache | 인덱스 데이터 | Memcached/Redis |

---

### Hash Ring

Mimir의 분산 컴포넌트들은 **해시 링(Hash Ring)** 을 사용하여 데이터를 분배합니다.

#### Hash Ring 사용 컴포넌트

- **Ingester**: 시계열을 어느 Ingester가 저장할지 결정
- **Distributor**: 다른 Distributor와 협력 (HA Tracker)
- **Store Gateway**: 어느 블록을 어느 Store Gateway가 담당할지
- **Compactor**: 어느 테넌트를 어느 Compactor가 처리할지
- **Ruler**: 어느 룰 그룹을 어느 Ruler가 평가할지
- **Alertmanager**: 어느 테넌트를 어느 Alertmanager가 처리할지

#### KV Store 백엔드

해시 링은 KV Store에 저장됩니다.

- **Memberlist** (권장): 가십 프로토콜, 외부 의존성 없음
- **Consul**: HashiCorp Consul
- **etcd**: etcd 클러스터

#### Memberlist

- Hashicorp의 memberlist 라이브러리 사용
- 가십 프로토콜로 클러스터 멤버십 관리
- 외부 KV 스토어 불필요
- 기본 권장 옵션

---

### 멀티 테넌시

#### 테넌트 헤더

모든 API 요청에 `X-Scope-OrgID` 헤더를 포함해야 합니다.

```http
POST /api/v1/push HTTP/1.1
X-Scope-OrgID: tenant-a
```

#### 데이터 격리

- 오브젝트 스토리지: 테넌트별 디렉토리 분리
- Ingester 메모리: 테넌트별 TSDB 분리
- 캐시: 테넌트별 키 분리

#### 테넌트별 한도 (Limits)

테넌트별로 다음 한도를 적용할 수 있습니다.

- **Ingestion Rate**: 초당 샘플/시계열 수
- **Max Series Per User**: 활성 시계열 수
- **Max Series Per Metric**: 메트릭당 시계열 수
- **Max Query Lookback**: 쿼리 가능 시간 범위
- **Max Query Length**: 쿼리당 최대 시간 범위
- **Max Samples Per Query**: 쿼리당 최대 샘플 수

#### 테넌트 간 격리 보장

- Ingester 메모리 사용량 격리 (per-tenant memory)
- 쿼리 큐 분리 (per-tenant queue)
- Rate Limit 분리

#### Cross-Tenant Federation

쿼리 시 여러 테넌트의 데이터를 함께 조회할 수 있습니다 (federated tenant 지원).

---

## Mimir 시작 가이드

> 원본: https://grafana.com/docs/mimir/latest/get-started/

---

### 목차

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

### 개요

Mimir를 빠르게 시작하는 두 가지 방법을 설명합니다.

#### 두 가지 시작 방법

| 방법 | 환경 | 시간 |
|------|------|------|
| **명령형 (Imperative)** | 단일 Mimir 프로세스 | 5분 |
| **선언형 (Declarative)** | Docker Compose 다중 프로세스 | 10분 |

---

### 사전 요구사항

- 64-bit 시스템
- 2 CPU 코어, 4GB RAM 이상
- Docker (Compose 방식 사용 시)
- Prometheus 또는 Grafana Alloy

---

### 단일 프로세스로 시작

#### 1. Mimir 다운로드

```bash
# Linux x86_64
curl -fLo mimir https://github.com/grafana/mimir/releases/latest/download/mimir-linux-amd64
chmod +x mimir
```

#### 2. 구성 파일 작성

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

#### 3. Mimir 실행

```bash
./mimir --config.file=demo.yaml
```

기본 포트는 9009입니다.

#### 4. 헬스체크

```bash
curl http://localhost:9009/ready
```

---

### Docker Compose로 시작

#### docker-compose.yml

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

#### config/mimir.yaml

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

#### config/nginx.conf

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

#### 실행

```bash
docker compose up -d
```

---

### Prometheus 연동

#### Prometheus 구성

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
      X-Scope-OrgID: demo  # multitenancy_enabled: true 일 때 필수
```

#### Prometheus 실행

```bash
prometheus --config.file=prometheus.yml
```

---

### Grafana Alloy 연동

#### config.alloy

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

#### Alloy 실행

```bash
alloy run config.alloy
```

---

### Grafana 데이터 소스 추가

#### UI에서 추가

1. Grafana 접속 (http://localhost:3000)
2. Connections → Data sources → Add new data source
3. **Prometheus** 선택
4. 설정:
   - Name: `Mimir`
   - URL: `http://mimir:9009/prometheus`
   - Custom HTTP Headers: `X-Scope-OrgID: demo` (멀티 테넌시 사용 시에만)
5. **Save & test**

#### 프로비저닝 (자동 설정)

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

### 첫 메트릭 쿼리

#### Grafana Explore에서

1. Grafana → Explore
2. 데이터 소스: **Mimir** 선택
3. 쿼리 입력:
   ```promql
   up
   ```
4. **Run query**

#### API로 직접

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

### 다음 단계

- [04_install_configure.md](./04_install_configure.md) — 프로덕션 설치 (Helm, Tanka)
- [05_send_metrics.md](./05_send_metrics.md) — 메트릭 전송 (Prometheus, Alloy, OTel)
- [06_promql.md](./06_promql.md) — PromQL 쿼리
- [07_alertmanager.md](./07_alertmanager.md) — 알림 관리
- [08_manage.md](./08_manage.md) — 운영
