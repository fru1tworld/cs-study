# Loki 아키텍처

> 이 문서는 Grafana Loki 공식 문서의 아키텍처 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/get-started/architecture/

---

## 목차

1. [개요](#개요)
2. [멀티 테넌시](#멀티-테넌시)
3. [스토리지](#스토리지)
4. [쓰기 경로 (Write Path)](#쓰기-경로-write-path)
5. [읽기 경로 (Read Path)](#읽기-경로-read-path)
6. [데이터 흐름 다이어그램](#데이터-흐름-다이어그램)

---

## 개요

Grafana Loki는 **마이크로서비스 기반 아키텍처(microservices-based architecture)** 를 채택하여 수평 확장 가능한 분산 시스템으로 동작하도록 설계되었습니다.

모든 컴포넌트는 단일 바이너리로 컴파일되며, `-target` 플래그가 어떤 컴포넌트가 실행될지를 결정합니다. 이를 통해 동일한 바이너리를 다양한 배포 모드(Single Binary, Simple Scalable, Microservices)에서 사용할 수 있습니다.

---

## 멀티 테넌시

Loki는 **단일 인스턴스에서 여러 테넌트의 데이터를 격리하여 저장**할 수 있도록 멀티 테넌시를 지원합니다.

### 테넌트 식별

- 모든 Loki API 요청에는 HTTP 헤더 `X-Scope-OrgID` 가 필요합니다.
- 멀티 테넌시가 비활성화된 경우, Loki는 테넌트 ID를 기본값 `fake` 로 설정하고 모든 데이터에 동일하게 적용합니다.
- 테넌트 ID는 데이터 분할(partitioning)의 기준이 됩니다.

### 데이터 격리

각 테넌트의 데이터와 요청은 다음과 같이 격리됩니다.

- 데이터 저장: 테넌트별로 별도 디렉토리/접두사에 저장
- 인덱스: 테넌트별로 분리
- 쿼리: 다른 테넌트의 데이터에 접근 불가
- Rate Limit: 테넌트별로 적용 가능

---

## 스토리지

Loki는 오브젝트 스토리지를 백엔드로 사용하며, 인덱스와 청크 파일 모두를 동일한 오브젝트 스토리지에 저장하는 **인덱스 시퍼(Index Shipper)** 방식을 사용합니다. 이 방식은 Loki 2.0부터 표준이 되었습니다.

### 지원 백엔드

- **AWS S3**
- **Google Cloud Storage (GCS)**
- **Azure Blob Storage**
- **Filesystem** (개발/PoC 용도)
- **Swift**, **Alibaba OSS**, **Baidu BOS** 등

### 데이터 구조

Loki에서는 두 가지 주요 파일 타입이 존재합니다.

#### 1. 인덱스 (Index)

특정 라벨 집합의 로그를 어디서 찾을지 알려주는 **목차** 역할을 합니다.

#### 2. 청크 (Chunk)

특정 라벨 집합에 대한 로그 항목들의 **컨테이너** 입니다. 청크는 압축되어 오브젝트 스토리지에 저장됩니다.

### 인덱스 형식

| 형식 | 설명 | 권장 여부 |
|------|------|-----------|
| **TSDB** | Prometheus 메인테이너들이 원래 개발 | 권장 |
| **BoltDB** (BoltDB Shipper) | 트랜잭셔널 키-값 스토어 | Deprecated |

TSDB 인덱스는 압축률이 높고 쿼리 성능이 우수하여 현재 권장되는 형식입니다.

---

## 쓰기 경로 (Write Path)

Loki의 쓰기 경로는 **7단계** 로 구성됩니다.

```
[Client Agent]
    |
    v
[Distributor] --consistent hash--> [Ingester #1, #2, #3]
                                        |
                                        v
                                [Object Storage]
```

### 단계별 흐름

1. **요청 수신**: Distributor가 클라이언트(Alloy, Promtail 등)로부터 HTTP `POST` 요청을 받습니다.
2. **검증(Validation)**: 들어오는 스트림에 대해 다음을 검증합니다.
   - 라벨 형식
   - 타임스탬프
   - 라인 길이
   - Rate Limit
3. **전처리(Preprocessing)**: 라벨 정규화 및 정렬을 수행합니다.
4. **해싱(Hashing)**: 테넌트 ID와 라벨 집합을 기반으로 일관된 해시(consistent hash)를 계산합니다.
5. **Ingester 선택**: 해시 링(hash ring)에서 복제 계수(replication factor, 기본 3)에 해당하는 Ingester를 선택합니다.
6. **Ingester로 전송**: 각 Ingester로 병렬 전송 후, 쿼럼(quorum, 보통 2/3) 응답을 기다립니다.
7. **Ingester 처리**: Ingester는 메모리 내 청크에 로그를 추가하고, 일정 조건이 충족되면 오브젝트 스토리지로 플러시합니다.

### 청크 플러시 조건

다음 중 하나의 조건이 충족되면 청크가 오브젝트 스토리지로 저장됩니다.

- 최대 청크 크기 도달 (기본 1.5MB 압축)
- 최대 청크 수명 도달 (기본 1시간)
- Ingester가 정상 종료 시
- 일정 시간 동안 새 로그가 들어오지 않음 (idle timeout)

---

## 읽기 경로 (Read Path)

Loki의 읽기 경로는 **9단계** 로 구성됩니다.

```
[Grafana / API Client]
    |
    v
[Query Frontend] --split & cache--> [Query Scheduler]
                                          |
                                          v
                                    [Querier #1, #2, ...]
                                          |
                            +-------------+--------------+
                            v                            v
                       [Ingester]                 [Object Storage]
                            \                            /
                             \                          /
                              v                        v
                              [Deduplication & Merge]
                                       |
                                       v
                                  [Result]
```

### 단계별 흐름

1. **쿼리 수신**: Query Frontend가 HTTP `GET` 또는 `POST` 요청을 수신합니다.
2. **쿼리 분할(Splitting)**: 시간 범위가 긴 쿼리를 여러 개의 작은 쿼리로 분할합니다 (예: 24시간 → 1시간 단위 24개).
3. **캐시 확인**: 결과 캐시(또는 chunk cache)를 확인하여 캐시된 결과를 재사용합니다.
4. **큐잉**: 분할된 쿼리를 Query Scheduler의 테넌트별 큐에 넣습니다.
5. **Querier 할당**: Querier가 큐에서 쿼리를 가져갑니다.
6. **Ingester 조회**: Querier는 최근 데이터를 위해 Ingester를 직접 쿼리합니다.
7. **스토리지 조회**: 오래된 데이터는 인덱스를 통해 청크 위치를 찾아 오브젝트 스토리지에서 가져옵니다.
8. **중복 제거(Deduplication)**: 복제된 청크에서 중복된 로그를 제거합니다.
9. **결과 반환**: Query Frontend가 결과를 병합하여 클라이언트에 반환합니다.

### 캐싱 전략

Loki는 다양한 캐시를 지원합니다.

- **Results Cache**: 최종 쿼리 결과 캐시
- **Chunk Cache**: 청크 데이터 캐시
- **Index Cache**: 인덱스 조회 캐시
- **Volume Cache**: 라벨 볼륨 쿼리 캐시

캐시 백엔드로는 Memcached, Redis, In-memory 등이 사용됩니다.

---

## 데이터 흐름 다이어그램

### 전체 시스템

```
                    ┌──────────────────────────────────┐
                    │       Client Agents              │
                    │ (Alloy, Promtail, Fluentd 등)    │
                    └──────────────┬───────────────────┘
                                   │  push
                                   v
┌────────────────────────────────────────────────────────────┐
│                     Loki Cluster                            │
│                                                             │
│  ┌─────────────┐                                            │
│  │ Distributor │ ─consistent hash─> ┌──────────────┐        │
│  └─────────────┘                    │  Ingester    │        │
│                                     │  (in-memory) │        │
│                                     └──────┬───────┘        │
│                                            │ flush           │
│                                            v                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │            Object Storage (S3, GCS, ...)             │    │
│  │  ┌─────────┐    ┌─────────┐                         │    │
│  │  │ Indexes │    │ Chunks  │                         │    │
│  │  └─────────┘    └─────────┘                         │    │
│  └─────────────────────────────────────────────────────┘    │
│                          ^                                   │
│                          │ read                              │
│  ┌─────────────┐    ┌────┴─────┐    ┌──────────────────┐   │
│  │   Query     │ -> │  Query   │ -> │     Querier      │   │
│  │  Frontend   │    │ Scheduler│    │                  │   │
│  └─────────────┘    └──────────┘    └──────────────────┘   │
└────────────────────────────────────────────────────────────┘
                                ^
                                │ query
                                │
                       ┌────────┴────────┐
                       │     Grafana     │
                       └─────────────────┘
```
