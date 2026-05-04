# Tempo 아키텍처

> 이 문서는 Grafana Tempo 공식 문서의 아키텍처 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/operations/architecture/

---

## 목차

1. [개요](#개요)
2. [컴포넌트](#컴포넌트)
3. [쓰기 경로](#쓰기-경로)
4. [읽기 경로](#읽기-경로)
5. [스토리지](#스토리지)
6. [멀티 테넌시](#멀티-테넌시)
7. [Parquet 백엔드](#parquet-백엔드)

---

## 개요

Grafana Tempo는 **마이크로서비스 기반 아키텍처** 로 동작하며, 각 컴포넌트가 독립적으로 확장 가능합니다. 모든 컴포넌트는 단일 바이너리에 포함되어 있고, `-target` 플래그로 실행할 컴포넌트를 결정합니다.

핵심 디자인 원칙:

1. **인덱스 없음**: TraceID 기반 조회와 TraceQL 검색 사용
2. **오브젝트 스토리지**: S3, GCS, Azure Blob 활용
3. **멀티 테넌트**: 단일 클러스터에서 여러 테넌트 격리
4. **Parquet 포맷**: 컬럼 지향 저장으로 효율적 쿼리

---

## 컴포넌트

### Distributor

**역할**: 다양한 형식의 트레이스 스팬을 받아 Ingester로 라우팅합니다.

**지원 프로토콜**:
- OpenTelemetry Protocol (OTLP) - gRPC, HTTP
- Jaeger - gRPC, Thrift HTTP, Thrift Compact UDP, Thrift Binary UDP
- Zipkin - HTTP

**주요 동작**:
- 트레이스 ID 기반으로 일관된 해싱(consistent hashing)을 통해 Ingester 선택
- 한 트레이스의 모든 스팬이 동일 Ingester로 가도록 보장
- 멀티 테넌시 강제 적용 (`X-Scope-OrgID`)
- Rate Limit 적용

**상태**: Stateless (수평 확장 자유로움)

### Ingester

**역할**: 수신한 스팬 데이터를 일정 단위로 모아 Parquet 형식으로 변환 후 오브젝트 스토리지에 저장합니다.

**주요 동작**:
- 메모리에서 스팬을 모음 (live trace)
- 일정 시간(기본 5분) 또는 크기에 도달 시 트레이스 완성
- Parquet 블록으로 변환
- 블룸 필터 및 인덱스 생성
- 오브젝트 스토리지에 업로드

**복제**: 기본 복제 계수 3 (3개의 Ingester가 동일 데이터 보유)

**상태**: Stateful

### Query Frontend

**역할**: 사용자의 쿼리 요청을 받아 여러 Querier에 분산하여 실행합니다.

**주요 동작**:
- TraceID 조회 요청 분배
- TraceQL 쿼리 분할 및 병렬화
- 결과 캐싱
- 결과 병합

**상태**: Stateless

### Querier

**역할**: 실제 트레이스 데이터를 조회합니다.

**주요 동작**:
- Ingester에서 최근 데이터 조회
- 오브젝트 스토리지에서 과거 데이터 조회
- TraceQL 평가
- 결과 반환

**상태**: Stateless

### Compactor

**역할**: 작은 블록들을 큰 블록으로 병합하고, 중복 스팬을 제거하며, 보존 기간이 지난 데이터를 삭제합니다.

**주요 동작**:
- 압축 작업 스케줄링
- 작은 Parquet 블록 병합
- 보존 기간 관리
- 단일 인스턴스 또는 클러스터 모드

### Scheduler / Worker (새 압축 아키텍처)

Compactor의 차세대 아키텍처:
- **Scheduler**: 압축 작업을 큐에 등록하고 분배
- **Worker**: 큐에서 작업을 받아 실행

향후 Compactor를 완전히 대체할 예정입니다.

### Metrics Generator (선택적)

**역할**: 트레이스에서 메트릭을 생성합니다.

**주요 기능**:
- **Span Metrics**: RED 메트릭 (Rate, Error, Duration) 생성
- **Service Graph Metrics**: 서비스 간 호출 그래프 메트릭 생성
- **Local Blocks**: 로컬에 메트릭용 블록 저장 (TraceQL Metrics 지원)

생성된 메트릭은 Prometheus나 Mimir에 Remote Write로 전송됩니다.

---

## 쓰기 경로

```
[Application + OTel SDK]
         |
         v (OTLP / Jaeger / Zipkin)
[Grafana Alloy / OTel Collector]
         |
         v (OTLP)
[Distributor]
         |
         v (consistent hash by TraceID)
[Ingester (replication factor=3)]
         |
         v (after timeout / size limit)
[Parquet Block]
         |
         v (upload)
[Object Storage (S3/GCS/Azure)]
         |
         v (compaction)
[Compactor] -> [Larger Blocks]
```

### 단계별 흐름

1. 애플리케이션이 OTel SDK 등으로 스팬 생성
2. 수집기(Alloy/OTel Collector)가 스팬을 버퍼링
3. 수집기 → Distributor로 OTLP 전송
4. Distributor가 TraceID 기반으로 일관된 해싱 수행
5. 선택된 Ingester(들)로 스팬 전송 (복제)
6. Ingester는 메모리에서 트레이스를 조립
7. 일정 조건(시간/크기)이 충족되면 Parquet 블록으로 변환
8. 오브젝트 스토리지에 업로드
9. Compactor가 주기적으로 블록 병합

---

## 읽기 경로

### TraceID 조회

```
[Grafana]
   |
   v (HTTP API: /api/traces/<traceID>)
[Query Frontend]
   |
   v (split / route)
[Querier]
   |
   +-> [Ingester]      (최근 데이터)
   |
   +-> [Object Storage] (과거 데이터, 블룸 필터로 빠른 결정)
   |
   v (merge spans)
[Trace Result]
```

### TraceQL 검색

```
[Grafana]
   |
   v (HTTP API: /api/search?q=<traceql>)
[Query Frontend]
   |
   v (시간 범위 분할, 병렬 작업 생성)
[Querier × N]
   |
   +-> [Ingester]       (최근 시간 범위)
   |
   +-> [Object Storage] (Parquet 컬럼 스캔)
   |
   v (merge & limit)
[Search Results]
```

### 블룸 필터를 통한 빠른 검색

각 Parquet 블록에는 트레이스 ID에 대한 **블룸 필터(Bloom Filter)** 가 함께 저장됩니다. TraceID 조회 시 블룸 필터로 해당 블록에 트레이스가 존재할 가능성이 있는지 빠르게 판단할 수 있습니다.

---

## 스토리지

### 데이터 형식

Tempo는 **Apache Parquet** 컬럼 형식을 사용합니다. 이는 다음 이점을 제공합니다.

- **컬럼 지향**: TraceQL 쿼리에서 필요한 컬럼만 읽기
- **압축 효율**: 동일 컬럼 데이터의 압축률이 높음
- **스키마 진화**: 새 컬럼 추가가 용이
- **분석 친화적**: 외부 도구로도 분석 가능

### 블록 구조

```
<bucket>/
  <tenant>/
    <block_id>/
      meta.json          (블록 메타데이터)
      data.parquet       (Parquet 데이터)
      bloom-<shard>      (블룸 필터들)
      index              (블록 인덱스)
```

### 백엔드 옵션

| 백엔드 | 권장 사용 |
|--------|----------|
| AWS S3 | 가장 일반적 |
| Google Cloud Storage | GCP 환경 |
| Azure Blob Storage | Azure 환경 |
| Filesystem | 개발/테스트 |

---

## 멀티 테넌시

### 테넌트 식별

모든 Tempo API 요청에는 **`X-Scope-OrgID`** HTTP 헤더가 필요합니다.

### 단일 테넌트 모드

`auth_enabled: false` 설정 시 모든 데이터가 `single-tenant` ID로 저장됩니다.

### 데이터 격리

- 오브젝트 스토리지에서 테넌트별 디렉토리 분리
- Ingester 메모리에서 테넌트별 분리
- 테넌트별 Rate Limit 및 보존 기간 적용 가능

---

## Parquet 백엔드

### Vparquet 버전

Tempo는 여러 Parquet 스키마 버전을 지원합니다.

- **vParquet**: 1세대
- **vParquet2**: 개선된 스키마
- **vParquet3**: 추가 최적화 (현재 권장 안정 버전)
- **vParquet4**: 새로운 컬럼 추가 (전용 속성 컬럼)

### 전용 속성 컬럼 (Dedicated Attribute Columns)

자주 쿼리되는 속성을 별도 컬럼으로 저장하여 성능을 향상시킬 수 있습니다.

```yaml
storage:
  trace:
    block:
      parquet_dedicated_columns:
        - scope: span
          name: http.status_code
          type: int
        - scope: resource
          name: namespace
          type: string
```

이 설정으로 해당 속성에 대한 쿼리 속도가 크게 향상됩니다.

### TraceQL과의 통합

Parquet의 컬럼 구조 덕분에 TraceQL이 다음을 효율적으로 수행합니다.

- 속성 기반 필터링 (특정 컬럼만 스캔)
- 집계 (컬럼 값 직접 사용)
- 시간 범위 필터링 (인덱스 활용)
