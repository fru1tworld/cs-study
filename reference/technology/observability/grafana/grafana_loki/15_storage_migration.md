# Loki 스토리지 마이그레이션

> 원본: https://grafana.com/docs/loki/latest/operations/storage/

---

## 목차

1. [스토리지 종류 개요](#스토리지-종류-개요)
2. [TSDB로 마이그레이션 (BoltDB → TSDB)](#tsdb로-마이그레이션-boltdb--tsdb)
3. [오브젝트 스토리지 변경](#오브젝트-스토리지-변경)
4. [스키마 버전 마이그레이션](#스키마-버전-마이그레이션)
5. [Single Store에서 분리 모드로](#single-store에서-분리-모드로)
6. [Cortex/Loki 1.x → 2.x → 3.x](#cortexloki-1x--2x--3x)
7. [클러스터 간 데이터 이동](#클러스터-간-데이터-이동)
8. [백업과 복구](#백업과-복구)

---

## 스토리지 종류 개요

### 인덱스 스토어

| 형식 | 상태 | 권장 |
|------|------|------|
| `tsdb` | 안정 | ✅ 권장 |
| `boltdb-shipper` | Deprecated | ❌ 마이그레이션 권장 |
| `boltdb` | 매우 오래됨 | ❌ |
| `cassandra` | Deprecated | ❌ |
| `bigtable` | Deprecated | ❌ |
| `dynamodb` | Deprecated | ❌ |
| `aws-dynamo` | Deprecated | ❌ |

### 청크 스토어 (Object Store)

| 백엔드 | 권장 |
|--------|------|
| AWS S3 | ✅ |
| GCS | ✅ |
| Azure Blob | ✅ |
| MinIO | ✅ (S3 호환) |
| Filesystem | 개발/테스트만 |
| Swift | ✅ |
| Alibaba OSS | ✅ |
| Baidu BOS | ✅ |

---

## TSDB로 마이그레이션 (BoltDB → TSDB)

### 왜 TSDB인가?

- **압축률 향상**: 일반적으로 20~30% 작은 인덱스
- **쿼리 성능**: 라벨 매칭이 빠름
- **샤딩 지원**: 쿼리 샤딩 자동 지원
- **Bloom 필터 지원**: 신규 기능 활용 가능

### 마이그레이션 전략: 점진적 (Cutover Date)

기존 데이터는 그대로 유지하고, **특정 날짜부터 새 스키마를 적용**합니다.

```yaml
schema_config:
  configs:
    # 기존 BoltDB 스키마 (그대로 유지)
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: s3
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    # 새 TSDB 스키마 (미래 날짜)
    - from: 2024-06-15        # ⚠️ 반드시 미래 날짜
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
```

### 주의사항

1. **`from` 날짜는 반드시 미래** — 과거 날짜를 사용하면 데이터 손실 위험
2. **여유 시간 확보** — 최소 24시간 이후 날짜 권장 (모든 인스턴스가 새 설정을 수신할 시간 확보)
3. **모든 컴포넌트 동시 업데이트** — Ingester, Querier, Compactor 전체 적용

### TSDB Shipper 구성

```yaml
storage_config:
  tsdb_shipper:
    active_index_directory: /loki/tsdb-index
    cache_location: /loki/tsdb-cache
    cache_ttl: 24h
    
    index_gateway_client:
      server_address: dns:///index-gateway:9095
```

### 검증

```bash
# 새 인덱스 디렉토리 확인
ls -la s3://my-bucket/tsdb_index_*

# 쿼리 작동 확인 (양쪽 시기 모두)
curl "http://loki:3100/loki/api/v1/query_range?query={app=\"test\"}&start=2023-12-01&end=2024-12-01"
```

### 점진적 활성화 후 모니터링

```promql
# TSDB 쿼리 비율
sum(rate(loki_index_request_duration_seconds_count{store="tsdb"}[5m]))
/
sum(rate(loki_index_request_duration_seconds_count[5m]))
```

### BoltDB 데이터 정리

보존 기간이 지나면 BoltDB 데이터가 자동으로 만료됩니다. 모든 BoltDB 데이터가 만료된 후 schema_config에서 해당 항목을 제거할 수 있습니다.

---

## 오브젝트 스토리지 변경

### 같은 백엔드 내 버킷 변경

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_new_   # 새 prefix
        period: 24h

storage_config:
  aws:
    s3: s3://access:secret@region/new-bucket   # 새 버킷
```

### 다른 백엔드로 (예: S3 → GCS)

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: gcs           # GCS로
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h

storage_config:
  aws:
    s3: s3://access:secret@region/old-bucket   # 기존 (읽기용)
  gcs:
    bucket_name: new-loki-bucket               # 새 (쓰기/읽기)
```

### 데이터 복사 (선택)

기존 데이터를 새 백엔드로 복사하는 방법:

```bash
# rclone 사용 예시
rclone sync s3:old-bucket gcs:new-bucket \
  --transfers 16 \
  --checkers 32
```

복사 후 schema_config의 `from` 날짜를 과거로 변경할 수 있습니다. 단, 기존 prefix와 충돌하지 않도록 주의하세요.

---

## 스키마 버전 마이그레이션

### 버전 변천

| 버전 | 변경 사항 |
|------|----------|
| v9 | 기본 스키마 |
| v10 | 인덱스 효율 개선 |
| v11 | 더 나은 청크 매핑 |
| v12 | BoltDB Shipper 표준 |
| v13 | TSDB 표준, 현재 권장 |

### 적용 방법

동일한 store 내에서 버전을 변경할 때도 새 항목으로 추가합니다.

```yaml
schema_config:
  configs:
    - from: 2023-01-01
      store: tsdb
      object_store: s3
      schema: v12
      index:
        prefix: index_
        period: 24h
    
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13          # ✅ 새 버전
      index:
        prefix: index_v13_   # 새 prefix 권장
        period: 24h
```

---

## Single Store에서 분리 모드로

Loki 2.0 이전에는 인덱스와 청크를 별도의 스토어에 분리하여 저장했습니다. 현재는 인덱스와 청크 모두 오브젝트 스토리지를 사용하는 **Single Store** 방식이 표준입니다.

### 마이그레이션 (드물게 필요)

```yaml
schema_config:
  configs:
    # 구식 분리 모드
    - from: 2020-01-01
      store: cassandra
      object_store: s3
      schema: v11
      index:
        prefix: index_
        period: 24h
    
    # 새 Single Store
    - from: 2024-06-15
      store: tsdb
      object_store: s3
      schema: v13
      index:
        prefix: tsdb_index_
        period: 24h

storage_config:
  cassandra:
    addresses: cassandra1,cassandra2  # 읽기용
    keyspace: loki
  
  aws:
    s3: s3://...
```

기존 Cassandra 데이터는 보존 기간이 만료될 때까지 읽기 전용으로 유지됩니다.

---

## Cortex/Loki 1.x → 2.x → 3.x

### Loki 1.x → 2.x

주요 변경:
- BoltDB Shipper 도입
- Single Store 표준화

마이그레이션:

```yaml
# 기존 1.x (Cassandra/DynamoDB 등)
- from: 2020-01-01
  store: cassandra
  ...

# 2.x로 (BoltDB Shipper)
- from: 2024-06-15
  store: boltdb-shipper
  object_store: s3
  schema: v11
  ...
```

### Loki 2.x → 3.x

주요 변경:
- BoltDB Shipper Deprecated
- TSDB 권장
- Pattern Ingester 추가
- Bloom 필터 (실험적)

마이그레이션:

```yaml
# 2.x BoltDB Shipper
- from: 2023-01-01
  store: boltdb-shipper
  object_store: s3
  schema: v12
  ...

# 3.x TSDB
- from: 2024-06-15
  store: tsdb
  object_store: s3
  schema: v13
  ...
```

### Breaking Changes 체크리스트

3.0:
- 일부 메트릭 이름 변경 (예: `cortex_*` → `loki_*`)
- 일부 구성 옵션 제거
- ingester WAL 형식 호환성 (재시작 시 비워질 수 있음)

각 버전별 [업그레이드 가이드](https://grafana.com/docs/loki/latest/setup/upgrade/)를 반드시 참고하세요.

---

## 클러스터 간 데이터 이동

### 시나리오 1: 클러스터 분할

하나의 클러스터를 두 클러스터로 분할합니다 (예: 테넌트별 분리).

#### 단계

1. 새 클러스터에서 일부 테넌트를 신규 시작 (실시간 데이터만)
2. 과거 데이터는 기존 클러스터에서 계속 조회
3. 특정 시점 이후 데이터는 새 클러스터에서 조회
4. 보존 기간 경과 후 기존 클러스터 정리

### 시나리오 2: 데이터 이전

#### Push API 재전송

```python
# 기존 Loki에서 쿼리
import requests

response = requests.get(
    "http://old-loki:3100/loki/api/v1/query_range",
    headers={"X-Scope-OrgID": "tenant-1"},
    params={
        "query": '{namespace="prod"}',
        "start": "1700000000",
        "end": "1700003600",
        "limit": "5000",
    },
)

# 새 Loki로 푸시
data = response.json()
for stream in data["data"]["result"]:
    push_payload = {
        "streams": [{
            "stream": stream["stream"],
            "values": stream["values"],
        }]
    }
    requests.post(
        "http://new-loki:3100/loki/api/v1/push",
        headers={
            "X-Scope-OrgID": "tenant-1",
            "Content-Type": "application/json",
        },
        json=push_payload,
    )
```

> 주의: 오래된 샘플 거부 제한(`reject_old_samples_max_age`)을 임시로 늘려야 합니다.
> ```yaml
> limits_config:
>   reject_old_samples: false
> ```

#### Object Storage 직접 복사

동일한 스키마를 사용하는 경우 가장 빠른 방법입니다.

```bash
# AWS CLI
aws s3 sync s3://old-loki-bucket s3://new-loki-bucket \
  --acl bucket-owner-full-control

# rclone (다양한 백엔드)
rclone sync old:loki-bucket new:loki-bucket \
  --transfers 32 \
  --checkers 64
```

복사 후 새 클러스터의 `storage_config`에서 새 버킷을 지정합니다.

---

## 백업과 복구

### 백업 전략

#### 오브젝트 스토리지 백업

가장 중요한 부분으로, 다음 옵션을 활용할 수 있습니다.

**S3 Versioning + Cross-Region Replication**

```json
{
  "Rules": [
    {
      "Status": "Enabled",
      "Priority": 1,
      "Filter": {},
      "Destination": {
        "Bucket": "arn:aws:s3:::loki-backup-us-west-2",
        "ReplicationTime": {
          "Status": "Enabled",
          "Time": { "Minutes": 15 }
        }
      }
    }
  ]
}
```

**GCS Multi-Regional Buckets**

기본적으로 여러 리전에 자동 복제됩니다.

**Azure GRS**

Geo-Redundant Storage로 자동 복제.

#### 구성 백업

- `loki.yaml`
- `runtime-config.yaml`
- 룰 파일들
- Helm values

모든 구성을 Git으로 관리하는 GitOps 방식을 권장합니다.

### 복구 절차

#### 부분 복구

특정 청크가 손상된 경우:
1. S3 Versioning에서 이전 버전 복구
2. 손상된 시간대의 데이터만 영향받음

#### 전체 복구

전체 버킷 손실 시:
1. 백업 버킷에서 새 버킷으로 복사
2. Loki 구성에서 새 버킷 지정
3. 재시작

#### Disaster Recovery

DR 사이트 운영:
- 활성 클러스터: 메인 리전
- 대기 클러스터: 백업 리전 (실시간 데이터 받지 않음)
- 장애 발생 시: 대기 클러스터에 백업 버킷 마운트, DNS 변경

### WAL 데이터

WAL은 임시 데이터이므로 별도 백업이 필요하지 않습니다. Ingester 재시작 시 메모리 데이터 복구 용도로 사용됩니다.

WAL 디스크 손상 시:
- 최근 1~2시간 데이터 손실 가능
- 복제 계수가 3 이상이면 다른 Ingester에서 복구 가능
