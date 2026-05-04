# Mimir 아키텍처

> 이 문서는 Grafana Mimir 공식 문서의 아키텍처 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/references/architecture/

---

## 목차

1. [개요](#개요)
2. [배포 모드](#배포-모드)
3. [핵심 컴포넌트](#핵심-컴포넌트)
4. [쓰기 경로](#쓰기-경로)
5. [읽기 경로](#읽기-경로)
6. [스토리지](#스토리지)
7. [Hash Ring](#hash-ring)
8. [멀티 테넌시](#멀티-테넌시)

---

## 개요

Grafana Mimir는 **마이크로서비스 기반 분산 시스템** 으로, 모든 컴포넌트가 수평 확장 가능합니다. Cortex의 후속 프로젝트로 시작되었으며, 더 나은 성능, 운영성, 안정성을 제공합니다.

### 주요 특징

- **컴포넌트 분리**: 각 컴포넌트는 독립적인 마이크로서비스
- **단일 바이너리**: 모든 컴포넌트가 단일 바이너리에 포함, `-target`으로 선택
- **무공유(Shared-nothing) 아키텍처**: 컴포넌트 간 직접 의존성 최소화
- **수평 확장**: 모든 컴포넌트가 수평 확장 가능

---

## 배포 모드

### Monolithic Mode

`-target=all` 플래그로 모든 컴포넌트를 단일 프로세스에서 실행. 개발/소규모용.

### Read-Write Mode

읽기/쓰기/백엔드 세 그룹으로 분리:
- `-target=write`: Distributor, Ingester
- `-target=read`: Query Frontend, Querier
- `-target=backend`: Store Gateway, Compactor, Ruler, Alertmanager 등

### Microservices Mode

각 컴포넌트를 개별 프로세스로 실행. 대규모 프로덕션용.

---

## 핵심 컴포넌트

### Distributor

**역할**: Prometheus나 Alloy 등 클라이언트로부터 메트릭을 받아 Ingester로 분배합니다.

**주요 동작**:
- 들어오는 시계열 검증 (라벨 형식, 시계열 길이 등)
- HA Tracker를 통한 고가용성 Prometheus 페어 처리
- 일관된 해싱(consistent hashing)으로 Ingester 선택
- Replication Factor(기본 3)에 따라 복제
- 테넌트별 Rate Limit 적용

**상태**: Stateless (수평 확장 자유로움)

### Ingester

**역할**: 메모리 내에서 메트릭을 받아 임시 저장하고, 주기적으로 오브젝트 스토리지에 TSDB 블록으로 영속화합니다.

**주요 동작**:
- 메모리 TSDB에 시계열 추가
- WAL(Write-Ahead Log)로 충돌 시 복구
- 일정 주기(기본 2시간)마다 블록을 오브젝트 스토리지로 업로드
- 최근 데이터에 대한 쿼리 응답 (Querier가 직접 호출)

**상태**: Stateful (메모리 내 시계열 보유)

**복제**: Replication Factor 3 (보통)

### Querier

**역할**: PromQL 쿼리를 실행합니다.

**주요 동작**:
- Ingester에서 최근 데이터 조회 (보통 12시간)
- Store Gateway를 통해 오브젝트 스토리지의 과거 데이터 조회
- 결과 중복 제거(replication 처리)
- PromQL 평가

**상태**: Stateless

### Query Frontend

**역할**: Querier 앞단의 선택적 컴포넌트로, 쿼리 가속을 담당합니다.

**주요 동작**:
- **쿼리 분할**: 시간 범위가 긴 쿼리를 작은 시간 범위로 분할
- **결과 캐싱**: Memcached/Redis에 결과 캐시
- **쿼리 큐잉**: 페어 큐(per-tenant queue)로 공정성 확보
- **재시도**: 실패 쿼리 자동 재시도

**상태**: Stateless

### Query Scheduler (선택적)

**역할**: Query Frontend의 큐를 분리하여 더 큰 클러스터에서 사용합니다.

**주요 동작**:
- Frontend 디커플링
- 테넌트별 공정 큐
- Frontend와 Querier의 독립적 확장

### Store Gateway

**역할**: 오브젝트 스토리지에 저장된 TSDB 블록을 쿼리합니다.

**주요 동작**:
- 블록 메타데이터 캐싱 (메모리)
- 블록의 인덱스 헤더(index-header) 다운로드 및 메모리 매핑
- 청크/인덱스 캐싱 (Memcached)
- 샤딩(blocks sharding)을 통한 확장
- Querier로부터 시계열 조회 요청 처리

**상태**: Stateful (디스크에 인덱스 헤더 저장)

### Compactor

**역할**: 작은 블록을 큰 블록으로 병합하고, 보존 정책에 따라 데이터를 삭제합니다.

**주요 동작**:
- 동일 시간 범위의 블록 병합 (deduplication)
- 시간 단위로 블록 압축 (2h → 12h → 24h)
- 보존 기간이 지난 블록 삭제
- 블록 인덱스 헤더 생성
- 단일 인스턴스 또는 샤드 분산 모드

**상태**: Stateful

### Ruler

**역할**: Recording Rules와 Alerting Rules를 평가합니다.

**주요 동작**:
- 룰 그룹을 주기적으로 평가
- Recording Rules 결과를 Distributor로 전송하여 저장
- Alerting Rules 평가 후 Alertmanager로 알림 전송
- 샤딩을 통한 룰 분산 처리

**상태**: Stateful (룰 평가 상태)

### Alertmanager

**역할**: Mimir에 내장된 Prometheus Alertmanager로, 알림을 관리합니다.

**주요 동작**:
- 알림 그룹화, 억제, 무시 처리
- 알림 채널로 전송 (Slack, PagerDuty, Email 등)
- 멀티 테넌트 지원 (테넌트별 알림 설정)

### Optional / Helper Components

#### Overrides Exporter
테넌트별 한도(limit) 메트릭을 노출.

#### Query-tee
운영 중 새 버전과 기존 버전의 쿼리 결과를 비교하기 위한 도구.

#### Mimir Continuous Test
Mimir 클러스터의 정상성을 지속적으로 검증.

#### Mimirtool
CLI 도구로 룰 관리, 분석, 변환 등 수행.

---

## 쓰기 경로

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

### 단계별 흐름

1. 클라이언트가 Remote Write API로 시계열 푸시
2. Distributor가 검증 및 라벨 정렬
3. 시계열별 일관된 해시로 Ingester 선택
4. 복제 계수만큼 Ingester(들)에 병렬 전송
5. Ingester는 메모리 TSDB에 추가 (WAL 기록)
6. 2시간마다 블록 생성 후 오브젝트 스토리지 업로드
7. 업로드 후 메모리에서 제거 (Querier가 Store Gateway에서 조회)

---

## 읽기 경로

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

### 단계별 흐름

1. Grafana가 PromQL 쿼리 전송
2. Query Frontend가 시간 범위로 분할, 캐시 확인
3. 캐시 미스 시 Query Scheduler로 큐잉
4. Querier가 큐에서 작업 획득
5. Ingester(최근 데이터)와 Store Gateway(과거 데이터) 병렬 조회
6. 복제본 중복 제거
7. PromQL 평가 후 결과 반환
8. Frontend가 결과 캐시 후 Grafana로 응답

---

## 스토리지

### TSDB 블록

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

### 블록 라이프사이클

```
[Ingester memory] -2h-> [2h block] -compaction-> [12h block] -compaction-> [24h block] -...-
```

### Bucket Index

각 테넌트별로 `bucket-index.json.gz`가 유지됩니다. 이는 해당 테넌트의 모든 블록 목록을 포함하여, Querier와 Store Gateway가 빠르게 블록 목록을 가져올 수 있게 합니다.

### 캐시

Mimir는 다양한 캐시를 지원합니다.

| 캐시 종류 | 대상 | 백엔드 |
|----------|------|--------|
| Results Cache | 쿼리 결과 | Memcached/Redis |
| Chunks Cache | 청크 데이터 | Memcached/Redis |
| Metadata Cache | 메타데이터 | Memcached/Redis |
| Index Cache | 인덱스 데이터 | Memcached/Redis |

---

## Hash Ring

Mimir의 분산 컴포넌트들은 **해시 링(Hash Ring)** 을 사용하여 데이터를 분배합니다.

### Hash Ring 사용 컴포넌트

- **Ingester**: 시계열을 어느 Ingester가 저장할지 결정
- **Distributor**: 다른 Distributor와 협력 (HA Tracker)
- **Store Gateway**: 어느 블록을 어느 Store Gateway가 담당할지
- **Compactor**: 어느 테넌트를 어느 Compactor가 처리할지
- **Ruler**: 어느 룰 그룹을 어느 Ruler가 평가할지
- **Alertmanager**: 어느 테넌트를 어느 Alertmanager가 처리할지

### KV Store 백엔드

해시 링은 KV Store에 저장됩니다.

- **Memberlist** (권장): 가십 프로토콜, 외부 의존성 없음
- **Consul**: HashiCorp Consul
- **etcd**: etcd 클러스터

### Memberlist

- Hashicorp의 memberlist 라이브러리 사용
- 가십 프로토콜로 클러스터 멤버십 관리
- 외부 KV 스토어 불필요
- 기본 권장 옵션

---

## 멀티 테넌시

### 테넌트 헤더

모든 API 요청에 `X-Scope-OrgID` 헤더를 포함해야 합니다.

```http
POST /api/v1/push HTTP/1.1
X-Scope-OrgID: tenant-a
```

### 데이터 격리

- 오브젝트 스토리지: 테넌트별 디렉토리 분리
- Ingester 메모리: 테넌트별 TSDB 분리
- 캐시: 테넌트별 키 분리

### 테넌트별 한도 (Limits)

테넌트별로 다음 한도를 적용할 수 있습니다.

- **Ingestion Rate**: 초당 샘플/시계열 수
- **Max Series Per User**: 활성 시계열 수
- **Max Series Per Metric**: 메트릭당 시계열 수
- **Max Query Lookback**: 쿼리 가능 시간 범위
- **Max Query Length**: 쿼리당 최대 시간 범위
- **Max Samples Per Query**: 쿼리당 최대 샘플 수

### 테넌트 간 격리 보장

- Ingester 메모리 사용량 격리 (per-tenant memory)
- 쿼리 큐 분리 (per-tenant queue)
- Rate Limit 분리

### Cross-Tenant Federation

쿼리 시 여러 테넌트의 데이터를 함께 조회할 수도 있습니다 (federated tenant 지원).
