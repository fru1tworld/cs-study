# Grafana Mimir 개요

> 이 문서는 Grafana Mimir 공식 문서의 "Introduction to Grafana Mimir" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/

---

## 목차

1. [Mimir란 무엇인가](#mimir란-무엇인가)
2. [Mimir의 주요 기능](#mimir의-주요-기능)
3. [핵심 개념](#핵심-개념)
4. [Mimir 메트릭 스택](#mimir-메트릭-스택)
5. [Mimir와 Prometheus 비교](#mimir와-prometheus-비교)
6. [데이터 수집 방식](#데이터-수집-방식)

---

## Mimir란 무엇인가

**Grafana Mimir**는 Prometheus 호환의 **장기 저장소(long-term storage) 및 메트릭 저장소** 입니다. 다음과 같은 특징을 갖습니다.

- **오픈소스**: AGPL-3.0 라이선스
- **대규모 확장성**: 단일 클러스터에서 10억(billion) 단위의 활성 시계열 처리 가능
- **고가용성**: 복제(replication)와 무중단 배포 지원
- **멀티 테넌시**: 단일 클러스터에서 여러 테넌트 격리
- **Prometheus 호환**: PromQL, 원격 쓰기(remote write) API 등 완벽 호환
- **저비용**: 오브젝트 스토리지(S3, GCS, Azure Blob) 활용

### 메트릭이란?

**메트릭(Metrics)** 은 시간에 따라 변하는 숫자 데이터로, 시스템의 상태를 정량적으로 측정합니다.

- **카운터(Counter)**: 누적 증가 값 (예: HTTP 요청 수)
- **게이지(Gauge)**: 임의의 값 (예: 메모리 사용량)
- **히스토그램(Histogram)**: 값의 분포 (예: 응답 시간 분포)
- **서머리(Summary)**: 분위수 추정

---

## Mimir의 주요 기능

### 거대한 확장성

Mimir는 **단일 클러스터에서 수십억 개의 활성 시계열을 처리** 할 수 있도록 설계되었습니다. Cortex의 후속 프로젝트로, 성능과 운영성을 크게 개선했습니다.

### 고가용성과 무중단 운영

- 모든 컴포넌트가 수평 확장 가능
- Ingester는 복제(기본 3-way)를 통해 데이터 손실 방지
- 무중단 롤링 업데이트 지원
- WAL(Write-Ahead Log)로 장애 복구

### 멀티 테넌시

- HTTP 헤더 `X-Scope-OrgID` 로 테넌트 식별
- 테넌트별 격리된 데이터 저장
- 테넌트별 Rate Limit 및 Quota 적용

### Prometheus 호환

- **PromQL** 100% 호환
- **Remote Write API** 호환 (Prometheus, Alloy, OTel Collector 등)
- **Remote Read API** 호환
- **Recording / Alerting Rules** 지원
- **Alertmanager** 내장

### Native Histograms 지원

Prometheus의 새로운 Native Histograms를 완벽 지원하여, 기존 히스토그램보다 훨씬 적은 비용으로 고해상도 분포 데이터를 저장할 수 있습니다.

### 빠른 쿼리

- 쿼리 분할 및 병렬 실행
- 결과 캐싱
- 청크 캐싱
- 인덱스 캐싱

---

## 핵심 개념

### 시계열 (Time Series)

특정 메트릭과 라벨 집합의 조합으로 식별되는 데이터 포인트의 시퀀스입니다.

```
http_requests_total{method="GET", status="200"} 1234 @1700000000
http_requests_total{method="GET", status="200"} 1245 @1700000060
http_requests_total{method="POST", status="201"} 567 @1700000000
```

각 줄은 별개의 시계열입니다.

### 활성 시계열 (Active Series)

최근 일정 시간(보통 20분) 내에 데이터가 들어온 시계열입니다. Mimir의 가장 중요한 용량 지표입니다.

### 라벨 (Labels)

메트릭에 추가되는 키-값 쌍 메타데이터입니다.

- **메트릭 이름** (`__name__`): `http_requests_total`
- **라벨**: `{method="GET", status="200", instance="server-1"}`

### 카디널리티 (Cardinality)

라벨 값의 고유 조합 수입니다. 카디널리티가 높을수록 메모리 사용량이 증가하므로, **카디널리티 폭증(cardinality explosion)** 을 피해야 합니다.

#### 카디널리티 안티패턴

- 사용자 ID, 요청 ID 같은 고유 식별자를 라벨로 사용
- 무한히 증가하는 값 (예: timestamp)을 라벨로 사용
- 너무 많은 라벨 차원

### TSDB 블록 (TSDB Blocks)

Mimir는 Prometheus와 동일한 TSDB 블록 형식을 사용합니다. 일정 시간(기본 2시간)마다 메모리의 데이터가 블록으로 압축되어 오브젝트 스토리지에 저장됩니다.

블록 구조:
```
block_id/
├── chunks/         (실제 시계열 데이터)
├── index           (시계열 인덱스)
├── meta.json       (블록 메타데이터)
└── tombstones      (삭제 마커)
```

---

## Mimir 메트릭 스택

전형적인 Mimir 기반 메트릭 스택은 다음과 같이 구성됩니다.

### 1. 메트릭 수집 (Metrics Collection)

- **Prometheus** (Remote Write 사용)
- **Grafana Alloy**
- **OpenTelemetry Collector**
- **Grafana Agent** (Deprecated, Alloy로 마이그레이션 권장)

### 2. Mimir Cluster

메트릭을 수집, 저장, 쿼리합니다.

### 3. Grafana

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

## Mimir와 Prometheus 비교

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

### 함께 사용하기

Prometheus와 Mimir는 보완 관계입니다.

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

## 데이터 수집 방식

Mimir는 **Push 방식** 으로 메트릭을 수집합니다 (Prometheus의 기본 Pull 방식과 다름).

### Remote Write API

Prometheus의 `remote_write` 설정으로 데이터를 Mimir로 전송합니다.

```yaml
remote_write:
  - url: http://mimir:9009/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

### 지원 프로토콜

- **Prometheus Remote Write** (snappy 압축 protobuf)
- **OpenTelemetry OTLP** (HTTP/gRPC)
- **Influx Line Protocol** (선택적 활성화)
- **Datadog Agent Protocol** (선택적 활성화)
- **Graphite** (선택적 활성화)

### 멀티 테넌트 헤더

```http
POST /api/v1/push HTTP/1.1
X-Scope-OrgID: tenant-1
Content-Encoding: snappy
Content-Type: application/x-protobuf

<snappy-compressed protobuf data>
```

---

## 다음 단계

- [02_architecture.md](./02_architecture.md) - Mimir 아키텍처 상세
- [03_components.md](./03_components.md) - 컴포넌트 (Distributor, Ingester, Querier 등)
- [04_deployment.md](./04_deployment.md) - 배포 (Helm, Jsonnet, Tanka)
- [05_promql.md](./05_promql.md) - PromQL 쿼리
- [06_alertmanager.md](./06_alertmanager.md) - 알림 관리
