# Mimir Tools (mimirtool, 기타 도구)

> 이 문서는 Grafana Mimir 공식 문서의 Tools 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/manage/tools/

---

## 목차

1. [도구 개요](#도구-개요)
2. [mimirtool](#mimirtool)
3. [Mimir Continuous Test](#mimir-continuous-test)
4. [Query-tee](#query-tee)
5. [mimir-query-tee](#mimir-query-tee)
6. [trafficdump](#trafficdump)
7. [bucket-tool](#bucket-tool)
8. [grafana-mimir-architecture-diagrams](#grafana-mimir-architecture-diagrams)

---

## 도구 개요

| 도구 | 용도 |
|------|------|
| `mimirtool` | 종합 CLI (룰, AM, 분석, 변환) |
| `Mimir Continuous Test` | 운영 중 무결성 검증 |
| `query-tee` | 두 백엔드 응답 비교 |
| `trafficdump` | 트래픽 캡처/분석 |
| `bucket-tool` | 오브젝트 스토리지 직접 조작 |

---

## mimirtool

종합 CLI 도구. Mimir 운영의 거의 모든 작업.

### 설치

```bash
# Homebrew
brew install mimirtool

# 바이너리 다운로드
curl -fLo mimirtool https://github.com/grafana/mimir/releases/latest/download/mimirtool-linux-amd64
chmod +x mimirtool
sudo mv mimirtool /usr/local/bin/

# Docker
docker run grafana/mimirtool --help
```

### 공통 옵션

```bash
--address=http://mimir:9009    # Mimir 주소
--id=tenant-1                   # 테넌트 ID (X-Scope-OrgID)
--user=user                     # Basic Auth
--key=password                  # Basic Auth
--auth-token=<bearer>           # Bearer Token
```

또는 환경 변수:
```bash
export MIMIR_ADDRESS=http://mimir:9009
export MIMIR_TENANT_ID=tenant-1
```

### 명령 카테고리

```
mimirtool
├── rules           # Recording/Alerting Rules
├── alertmanager    # Alertmanager 설정
├── analyse         # 사용 분석
├── backfill        # 블록 백필
├── bucket-validation  # 버킷 검증
├── config          # 구성 변환
├── remote-read     # Remote Read
├── version
└── help
```

---

### `mimirtool rules`

#### 룰 목록

```bash
mimirtool rules list \
  --address=http://mimir:9009 \
  --id=tenant-1
```

#### 룰 출력 (인쇄)

```bash
mimirtool rules print \
  --address=http://mimir:9009 \
  --id=tenant-1
```

#### 룰 업로드

```bash
# 단일 파일
mimirtool rules load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  rules.yaml

# 여러 파일
mimirtool rules load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  alerts.yaml recording.yaml

# 동기화 (현재 로컬 파일이 truth)
mimirtool rules sync \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  rules/*.yaml
```

#### 룰 비교 (diff)

```bash
mimirtool rules diff \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  rules/*.yaml
```

#### 룰 삭제

```bash
mimirtool rules delete \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  --namespace=infra \
  --rule-group=node_alerts

# 네임스페이스 전체
mimirtool rules delete-namespace \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  --namespace=infra
```

#### 룰 검증 (lint)

```bash
mimirtool rules lint rules.yaml

# 자동 수정 (rule order 등)
mimirtool rules format rules.yaml
mimirtool rules check rules.yaml
```

#### Prometheus → Mimir 변환

```bash
mimirtool rules prepare \
  --in-source-tenants=true \
  rules.yaml
```

---

### `mimirtool alertmanager`

#### 구성 업로드

```bash
mimirtool alertmanager load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  alertmanager.yaml \
  template1.tmpl template2.tmpl
```

#### 구성 조회

```bash
mimirtool alertmanager get \
  --address=http://mimir:9009 \
  --id=tenant-1
```

#### 구성 삭제

```bash
mimirtool alertmanager delete \
  --address=http://mimir:9009 \
  --id=tenant-1
```

#### 구성 검증

```bash
mimirtool alertmanager verify \
  alertmanager.yaml \
  template1.tmpl
```

#### 구성 마이그레이션

```bash
# Cortex Alertmanager → Mimir
mimirtool alertmanager migrate-utf8 \
  --in-place \
  alertmanager.yaml
```

---

### `mimirtool analyse`

#### Grafana 대시보드 분석

```bash
mimirtool analyse grafana \
  --address=http://grafana:3000 \
  --key=$GRAFANA_API_KEY
```

대시보드에서 사용된 메트릭 추출.

#### Prometheus → Mimir 분석

```bash
mimirtool analyse prometheus \
  --address=http://prometheus:9090 \
  --grafana-metrics-file=metrics-in-grafana.json \
  --ruler-metrics-file=metrics-in-ruler.json
```

실제로 사용되는 메트릭 vs 수집되는 메트릭 비교 → 사용 안 되는 메트릭 식별.

#### 룰 분석

```bash
mimirtool analyse rule-file rules.yaml
```

#### 모든 메트릭 분석

```bash
mimirtool analyse dashboard dashboard.json
```

---

### `mimirtool backfill`

블록을 직접 Mimir에 업로드 (마이그레이션, 복원).

```bash
mimirtool backfill \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  /path/to/tsdb-blocks/
```

활용:
- Prometheus 데이터 마이그레이션
- 백업 복원
- 다른 Mimir에서 데이터 이동

---

### `mimirtool bucket-validation`

오브젝트 스토리지 작동 검증.

```bash
mimirtool bucket-validation \
  --backend=s3 \
  --s3.bucket-name=my-mimir \
  --s3.endpoint=s3.amazonaws.com \
  --s3.region=us-east-1 \
  --s3.access-key=$AWS_ACCESS_KEY_ID \
  --s3.secret-key=$AWS_SECRET_ACCESS_KEY
```

검증 항목:
- 쓰기 권한
- 읽기 권한
- 삭제 권한
- 목록 권한
- 일관성

---

### `mimirtool config`

구성 변환 (Cortex → Mimir, 구버전 → 신버전).

```bash
# Cortex → Mimir
mimirtool config convert \
  --yaml-file=cortex-config.yaml \
  --output=mimir-config.yaml

# 구버전 Mimir → 신버전
mimirtool config convert \
  --yaml-file=old-mimir.yaml \
  --output=new-mimir.yaml \
  --target-version=2.10
```

---

### `mimirtool remote-read`

Remote Read 엔드포인트 직접 호출.

```bash
mimirtool remote-read dump \
  --address=http://mimir:9009/prometheus/api/v1/read \
  --id=tenant-1 \
  --from=2024-01-01T00:00:00Z \
  --to=2024-01-02T00:00:00Z \
  --selector='{job="prometheus"}'
```

---

## Mimir Continuous Test

### 개요

Mimir 클러스터의 정상성을 지속적으로 검증.

### 동작

```
[Continuous Test]
    |
    +--> 1. 알려진 메트릭을 주기적으로 푸시
    |
    +--> 2. 일정 시간 후 같은 메트릭을 쿼리
    |
    +--> 3. 응답 검증 (값, 라벨, 시간 등)
    |
    +--> 4. 결과를 메트릭으로 노출
```

### 실행

```bash
docker run -d \
  --name=mimir-continuous-test \
  grafana/mimir-continuous-test:latest \
  -tests.write-endpoint=http://mimir:9009 \
  -tests.read-endpoint=http://mimir:9009 \
  -tests.tenant-id=test \
  -tests.write-read-series-test.enabled=true \
  -tests.write-read-series-test.num-series=100 \
  -tests.run-interval=30s
```

### 메트릭

```
mimir_continuous_test_writes_total
mimir_continuous_test_writes_failed_total
mimir_continuous_test_queries_total
mimir_continuous_test_queries_failed_total
mimir_continuous_test_query_result_checks_total
mimir_continuous_test_query_result_checks_failed_total
```

### 알림

```yaml
- alert: MimirContinuousTestFailing
  expr: |
    sum(rate(mimir_continuous_test_query_result_checks_failed_total[5m])) > 0
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Mimir 지속적 테스트 실패 — 데이터 무결성 문제"
```

---

## Query-tee

### 개요

두 Mimir/Cortex/Prometheus 백엔드에 동일 쿼리 전송 후 응답 비교.

### 활용

- 새 버전 검증 (회귀 확인)
- Mimir vs Cortex 마이그레이션 검증
- A/B 테스트

### 실행

```bash
docker run -d \
  --name=query-tee \
  -p 8080:8080 \
  grafana/query-tee:latest \
  -backend.endpoints=http://mimir-old:9009/prometheus,http://mimir-new:9009/prometheus \
  -backend.preferred=mimir-old \
  -log.level=info
```

### 작동

```
[Grafana / 사용자]
       |
       v
   [query-tee]
       |
       +-> [Mimir Old]   (응답을 사용자에게 반환)
       |
       +-> [Mimir New]   (병렬 호출, 응답 비교만)
            |
            v
        [diff metrics]
```

### 메트릭

```
cortex_querytee_request_duration_seconds
cortex_querytee_responses_total{result="success|failed|matching"}
cortex_querytee_responses_compared_total
cortex_querytee_responses_comparison_failures_total
```

### 활용 예

```promql
# Mimir 새 버전 응답이 기존과 다른 비율
sum(rate(cortex_querytee_responses_comparison_failures_total[5m]))
/
sum(rate(cortex_querytee_responses_compared_total[5m]))
```

---

## mimir-query-tee

`query-tee`의 Mimir 특화 버전. 거의 동일.

---

## trafficdump

### 개요

Mimir 트래픽을 캡처하여 분석/리플레이.

### 캡처

```bash
trafficdump capture \
  --listen-address=:8080 \
  --target-address=http://mimir:9009 \
  --output=traffic.dump
```

### 분석

```bash
trafficdump analyse traffic.dump
```

요약:
- 요청 수
- 엔드포인트별 분포
- 테넌트별 분포
- 응답 코드 분포

### 리플레이

```bash
trafficdump replay \
  --target-address=http://test-mimir:9009 \
  --input=traffic.dump
```

활용:
- 새 환경에 같은 부하 테스트
- 회귀 테스트

---

## bucket-tool

### 개요

오브젝트 스토리지의 Mimir 데이터 직접 조작.

### 명령

```bash
# 블록 목록
bucket-tool blocks list \
  --backend=s3 \
  --bucket=my-mimir

# 블록 메타데이터
bucket-tool blocks metadata \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1 \
  --block=01HZX...

# 블록 삭제 (위험)
bucket-tool blocks delete \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1 \
  --block=01HZX...

# 테넌트 목록
bucket-tool tenants list \
  --backend=s3 \
  --bucket=my-mimir

# 일관성 확인
bucket-tool blocks check \
  --backend=s3 \
  --bucket=my-mimir \
  --tenant=tenant-1
```

### 활용

- 손상된 블록 식별/삭제
- 디스크 사용량 분석
- 복구 작업

---

## grafana-mimir-architecture-diagrams

### 개요

현재 Mimir 클러스터 구성을 다이어그램으로 시각화.

### 사용

```bash
docker run -v $(pwd)/diagrams:/output \
  grafana/mimir-architecture-diagrams:latest \
  --config=/etc/mimir.yaml \
  --output-format=svg \
  --output-dir=/output
```

생성되는 다이어그램:
- 컴포넌트 토폴로지
- 데이터 흐름
- 스토리지 구조
- 의존성

---

## 운영 시나리오

### 1. 매일 자동 검증

```bash
#!/bin/bash
# daily-validation.sh

# Mimir 자체 헬스
curl -f http://mimir:9009/ready || exit 1

# 버킷 무결성
mimirtool bucket-validation --backend=s3 --s3.bucket-name=my-mimir

# Continuous Test 메트릭 확인
FAILED=$(curl -s http://mimir-continuous-test:9090/metrics | \
  grep mimir_continuous_test_query_result_checks_failed_total | \
  awk '{print $2}')

if [ "$FAILED" != "0" ]; then
  echo "ALERT: Continuous test failures: $FAILED"
fi
```

### 2. 룰 GitOps

```bash
# CI/CD 파이프라인
mimirtool rules lint rules/*.yaml || exit 1

mimirtool rules diff \
  --address=$MIMIR_URL \
  --id=$TENANT_ID \
  rules/*.yaml

# Diff 검토 후 적용
mimirtool rules sync \
  --address=$MIMIR_URL \
  --id=$TENANT_ID \
  rules/*.yaml
```

### 3. 사용 안 되는 메트릭 정리

```bash
# 1. Grafana 대시보드 분석
mimirtool analyse grafana \
  --address=http://grafana:3000 \
  --key=$GRAFANA_API_KEY \
  --output=grafana-metrics.json

# 2. Prometheus 분석
mimirtool analyse prometheus \
  --address=http://prometheus:9090 \
  --grafana-metrics-file=grafana-metrics.json \
  --ruler-metrics-file=ruler-metrics.json \
  --output=prometheus-metrics.json

# 3. 결과 검토 → write_relabel_configs로 불필요 메트릭 드롭
```
