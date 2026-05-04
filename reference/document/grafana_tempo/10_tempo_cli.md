# Tempo CLI

> 이 문서는 Grafana Tempo 공식 문서의 "Tempo CLI" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/operations/tempo_cli/

---

## 목차

1. [개요](#개요)
2. [설치](#설치)
3. [공통 옵션](#공통-옵션)
4. [list 명령](#list-명령)
5. [view 명령](#view-명령)
6. [search 명령](#search-명령)
7. [analyse 명령](#analyse-명령)
8. [migrate 명령](#migrate-명령)
9. [generate 명령](#generate-명령)
10. [기타 명령](#기타-명령)

---

## 개요

`tempo-cli` 는 Tempo의 백엔드 데이터를 직접 조회/관리하는 도구입니다.

### 주요 용도

- 블록 목록 조회
- 블록 메타데이터/내용 검사
- 손상된 블록 식별/삭제
- 트레이스 직접 조회
- 마이그레이션
- 테스트 데이터 생성

---

## 설치

### 바이너리 다운로드

[GitHub Releases](https://github.com/grafana/tempo/releases) 에서 받기.

```bash
curl -O -L https://github.com/grafana/tempo/releases/latest/download/tempo-cli_linux_amd64.tar.gz
tar -xvzf tempo-cli_linux_amd64.tar.gz
chmod +x tempo-cli
sudo mv tempo-cli /usr/local/bin/
```

### Docker

```bash
docker run --rm grafana/tempo:latest tempo-cli <command>
```

### 소스에서 빌드

```bash
git clone https://github.com/grafana/tempo.git
cd tempo
make tempo-cli
```

---

## 공통 옵션

```bash
tempo-cli [global flags] <command> [command flags]
```

### 백엔드 지정

```bash
--backend=s3              # local, s3, gcs, azure
--bucket=my-tempo-bucket
--config-file=/etc/tempo.yaml   # 또는 구성 파일에서
```

### 백엔드별 옵션

#### S3

```bash
--s3-endpoint=s3.amazonaws.com
--s3-region=us-east-1
--s3-access-key=$AWS_ACCESS_KEY_ID
--s3-secret-key=$AWS_SECRET_ACCESS_KEY
```

#### GCS

```bash
--gcs-bucket-name=my-bucket
```

#### Azure

```bash
--azure-storage-account-name=myaccount
--azure-storage-account-key=$AZURE_KEY
--azure-container-name=tempo
```

### 출력 옵션

```bash
--output=table   # table, json, yaml
--verbose
--quiet
```

---

## list 명령

### `list blocks`

테넌트의 블록 목록 조회.

```bash
tempo-cli list blocks \
  --backend=s3 \
  --bucket=my-tempo \
  <tenant-id>
```

출력:
```
+----------------+-----------+-----------+----------+----------+
| ID             | Tenant    | Start     | End      | Size     |
+----------------+-----------+-----------+----------+----------+
| 01HZX...       | tenant-1  | 1700...   | 1700...  | 50MB     |
| 01HZW...       | tenant-1  | 1700...   | 1700...  | 48MB     |
+----------------+-----------+-----------+----------+----------+
```

### `list cache-summary`

캐시 요약.

### `list compaction-summary`

압축 통계.

```bash
tempo-cli list compaction-summary <tenant-id>
```

### `list block`

특정 블록 상세.

```bash
tempo-cli list block <tenant-id> <block-id>
```

### `list index`

블록의 인덱스 정보.

```bash
tempo-cli list index <tenant-id> <block-id>
```

---

## view 명령

### `view index`

인덱스 내용 보기.

```bash
tempo-cli view index <tenant-id> <block-id>
```

### `view summary`

블록 요약 정보.

```bash
tempo-cli view summary <tenant-id> <block-id>
```

출력:
- 트레이스 수
- 시간 범위
- 크기
- Parquet 버전
- 압축 레벨

### `view block`

블록의 모든 트레이스 ID 출력.

```bash
tempo-cli view block <tenant-id> <block-id>
```

---

## search 명령

### `search blocks`

블록에서 TraceQL 검색.

```bash
tempo-cli search blocks \
  --backend=s3 \
  --bucket=my-tempo \
  '{ resource.service.name = "frontend" && status = error }' \
  <tenant-id>
```

옵션:
```bash
--start=2024-01-01T00:00:00Z
--end=2024-01-02T00:00:00Z
--limit=20
```

### 트레이스 ID로 직접 조회

```bash
tempo-cli query trace <tenant-id> <trace-id>
```

---

## analyse 명령

### `analyse blocks`

블록 통계 분석.

```bash
tempo-cli analyse blocks \
  --backend=s3 \
  --bucket=my-tempo \
  <tenant-id>
```

분석 항목:
- 평균 블록 크기
- 트레이스/스팬 수 분포
- 압축 레벨 분포
- 시간 범위 분포

### `analyse block`

특정 블록 분석.

```bash
tempo-cli analyse block <tenant-id> <block-id>
```

출력:
- 스팬 속성 카디널리티
- 자주 사용된 속성 키
- Parquet 컬럼별 크기
- Bloom 필터 통계

### 활용

```bash
# Dedicated columns 후보 식별
tempo-cli analyse block <tenant-id> <block-id> | head -20
```

자주 사용되는 속성 → `parquet_dedicated_columns` 후보.

---

## migrate 명령

### `migrate tenant`

테넌트 ID 변경 또는 데이터 이동.

```bash
tempo-cli migrate tenant \
  --source-tenant=old-tenant \
  --dest-tenant=new-tenant \
  --source-backend=s3 \
  --source-bucket=old-bucket \
  --dest-backend=s3 \
  --dest-bucket=new-bucket
```

### `migrate parquet`

Parquet 버전 마이그레이션 (예: vParquet3 → vParquet4).

```bash
tempo-cli migrate parquet \
  --target-version=vParquet4 \
  <tenant-id>
```

> 주의: Compactor가 자동으로 새 버전으로 변환하지만, 빠르게 변환하려면 이 명령 사용.

---

## generate 명령

### `generate bloom`

블록의 Bloom 필터 재생성.

```bash
tempo-cli generate bloom <tenant-id> <block-id>
```

손상된 Bloom 복구 시.

### `generate index`

블록 인덱스 재생성.

```bash
tempo-cli generate index <tenant-id> <block-id>
```

---

## 기타 명령

### `gen` (테스트 데이터)

테스트용 트레이스 생성.

```bash
tempo-cli gen \
  --address=localhost:4317 \
  --service-name=test-service \
  --operation-name=test-op \
  --duration=100ms \
  --num-traces=10
```

### `rollout-blocks`

블록 압축/배포 롤아웃 트리거 (드물게 사용).

### `clear`

테넌트의 모든 블록 삭제 (위험!).

```bash
tempo-cli clear --backend=s3 --bucket=my-tempo <tenant-id>
```

> ⚠️ 복구 불가능. 백업 후 사용.

### `delete-block`

특정 블록만 삭제.

```bash
tempo-cli delete-block <tenant-id> <block-id>
```

손상된 블록 복구 불가능 시 사용.

### `verify`

블록 무결성 검증.

```bash
tempo-cli verify <tenant-id> <block-id>
```

출력:
- 체크섬 검증
- 인덱스 정합성
- Parquet 스키마 호환성

---

## 운영 시나리오

### 1. 손상된 블록 식별 및 처리

```bash
# 1. 손상 의심 블록 식별
tempo-cli verify <tenant-id> <block-id>

# 실패 시:
# 2. Bloom/인덱스 재생성 시도
tempo-cli generate bloom <tenant-id> <block-id>
tempo-cli generate index <tenant-id> <block-id>

# 3. 여전히 실패 시 삭제
tempo-cli delete-block <tenant-id> <block-id>
```

### 2. 카디널리티 분석 (전용 컬럼 결정)

```bash
# 최근 블록 분석
tempo-cli list blocks <tenant-id> | head -5

# 각 블록의 속성 사용 빈도 분석
for block in $(tempo-cli list blocks <tenant-id> --output=json | jq -r '.[].id' | head -5); do
  tempo-cli analyse block <tenant-id> $block
done

# 자주 사용되는 속성 → tempo.yaml의 parquet_dedicated_columns에 추가
```

### 3. 마이그레이션

```bash
# 1. 새 백엔드로 데이터 복사
aws s3 sync s3://old-tempo-bucket s3://new-tempo-bucket

# 2. 또는 tempo-cli로
tempo-cli migrate tenant \
  --source-tenant=tenant-1 \
  --dest-tenant=tenant-1 \
  --source-backend=s3 \
  --source-bucket=old-tempo-bucket \
  --dest-backend=gcs \
  --dest-bucket=new-tempo-bucket

# 3. Tempo 구성 변경
# storage.trace.backend: gcs
# storage.trace.gcs.bucket_name: new-tempo-bucket

# 4. 재시작
```

### 4. 디스크 사용량 분석

```bash
# 테넌트별 블록 크기 합계
tempo-cli list blocks <tenant-id> --output=json | \
  jq '[.[] | .size] | add'

# 가장 큰 블록 식별
tempo-cli list blocks <tenant-id> --output=json | \
  jq 'sort_by(.size) | reverse | .[0:10]'
```

### 5. 트레이스 백필 (외부 검증)

```bash
# 1. 트레이스 ID 목록 (예: 의심 사례)
TRACE_IDS=("abc123" "def456" "ghi789")

# 2. 각각 조회
for tid in "${TRACE_IDS[@]}"; do
  echo "=== Trace: $tid ==="
  tempo-cli query trace tenant-1 $tid
done
```
