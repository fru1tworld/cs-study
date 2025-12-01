# Amazon RDS 스토리지 유형

## 개요

Amazon RDS는 Amazon EBS(Elastic Block Store) 볼륨을 스토리지로 사용합니다. 워크로드 특성에 따라 적합한 스토리지 유형을 선택할 수 있습니다.

## 스토리지 유형 비교

| 특성 | gp3 | gp2 | io2 Block Express | io1 | Magnetic |
|-----|-----|-----|-------------------|-----|----------|
| **권장** | ✅ 권장 | 이전 세대 | ✅ 권장 | 이전 세대 | ❌ 레거시 |
| **최대 용량** | 64 TiB | 64 TiB | 64 TiB | 64 TiB | 3 TiB |
| **최대 IOPS** | 16,000 | 16,000 | 256,000 | 64,000 | - |
| **최대 처리량** | 1,000 MiB/s | 250 MiB/s | 4,000 MiB/s | 1,000 MiB/s | - |
| **사용 사례** | 일반 | 일반 | I/O 집약 | I/O 집약 | 하위 호환 |

## General Purpose SSD (gp3)

### 특징
- **권장**: 새로운 배포에 권장되는 범용 스토리지
- 기본 3,000 IOPS 및 125 MiB/s 처리량 제공
- 스토리지 용량과 독립적으로 IOPS 및 처리량 프로비저닝 가능
- gp2 대비 최대 20% 비용 절감

### 성능 사양
```
기본 성능:
- IOPS: 3,000 (최대 16,000까지 증가 가능)
- 처리량: 125 MiB/s (최대 1,000 MiB/s까지 증가 가능)

프로비저닝 범위:
- 스토리지: 20 GiB ~ 64 TiB
- IOPS: 3,000 ~ 16,000
- 처리량: 125 ~ 1,000 MiB/s
```

### 적합한 워크로드
- 개발 및 테스트 환경
- 중소 규모 데이터베이스
- 일반적인 OLTP 워크로드
- 비용 효율적인 프로덕션 환경

### CLI 예시
```bash
aws rds create-db-instance \
    --db-instance-identifier mydb \
    --storage-type gp3 \
    --allocated-storage 400 \
    --iops 12000 \
    --storage-throughput 500 \
    ...
```

## General Purpose SSD (gp2)

### 특징
- 이전 세대 범용 SSD
- 스토리지 용량에 따라 IOPS 자동 할당
- 버스트 기능 제공 (크레딧 시스템)

### 성능 사양
```
IOPS 계산:
- 기본: 100 IOPS ~ 3,000 IOPS (최소 보장)
- 3 IOPS/GiB (최대 16,000 IOPS)
- 버스트: 최대 3,000 IOPS (버스트 크레딧 사용)

예시:
- 100 GB: 300 IOPS
- 1,000 GB: 3,000 IOPS
- 5,334 GB 이상: 16,000 IOPS
```

### 버스트 크레딧
- 버스트 크레딧 잔액 소진 시 기본 성능으로 저하
- CloudWatch에서 `BurstBalance` 메트릭으로 모니터링

## Provisioned IOPS SSD (io2 Block Express)

### 특징
- **권장**: I/O 집약적 워크로드에 최적화
- 일관된 저지연 성능 제공
- 최대 256,000 IOPS 지원
- 99.999% 내구성 (io1 대비 100배)

### 성능 사양
```
IOPS 범위: 1,000 ~ 256,000
처리량: 최대 4,000 MiB/s

IOPS-to-Storage 비율:
- 최대 1,000:1 (스토리지 GiB당 1,000 IOPS)
- 예: 400 GiB → 최대 400,000 IOPS 프로비저닝 가능
      (단, 실제 최대는 256,000 IOPS)
```

### 적합한 워크로드
- 대규모 미션 크리티컬 데이터베이스
- 고성능 OLTP 시스템
- 지연 시간에 민감한 애플리케이션
- SAP, Oracle, SQL Server 엔터프라이즈 워크로드

### 지원 인스턴스 클래스
- db.r6i, db.r5b, db.m6i 등 특정 인스턴스 클래스에서만 사용 가능

## Provisioned IOPS SSD (io1)

### 특징
- 이전 세대 Provisioned IOPS 스토리지
- 최대 64,000 IOPS 지원
- io2 Block Express로 마이그레이션 권장

### 성능 사양
```
IOPS 범위: 1,000 ~ 64,000
처리량: 최대 1,000 MiB/s

IOPS-to-Storage 비율:
- 최대 50:1 (스토리지 GiB당 50 IOPS)
- 예: 1,000 GiB → 최대 50,000 IOPS 프로비저닝 가능
```

## Magnetic (표준)

### 특징
⚠️ **레거시**: 하위 호환성을 위해서만 지원

- 최대 3 TiB
- 약 100 IOPS (평균)
- 새로운 배포에는 권장하지 않음

### 마이그레이션 권장
기존 Magnetic 스토리지 사용 시 gp3로 마이그레이션 권장

## 스토리지 선택 가이드

### 워크로드별 권장 스토리지

| 워크로드 | 권장 스토리지 | 이유 |
|---------|-------------|------|
| 개발/테스트 | gp3 | 비용 효율적, 충분한 성능 |
| 중소규모 프로덕션 | gp3 | 성능과 비용의 균형 |
| 대규모 OLTP | io2 Block Express | 일관된 고성능, 저지연 |
| 데이터 웨어하우스 | gp3 또는 io1 | 순차적 I/O에 최적화 |
| SAP HANA | io2 Block Express | 엄격한 지연 시간 요구사항 |

### 선택 기준

1. **IOPS 요구사항**
   - 16,000 IOPS 이하: gp3
   - 16,000 ~ 64,000 IOPS: io1
   - 64,000 IOPS 초과: io2 Block Express

2. **예산**
   - 비용 우선: gp3
   - 성능 우선: io2 Block Express

3. **일관성 요구사항**
   - 버스트 허용: gp2/gp3
   - 일관된 성능 필요: io1/io2

## 스토리지 자동 확장

### 개요
스토리지가 부족해지면 자동으로 용량을 확장합니다.

### 활성화 방법
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --max-allocated-storage 1000 \
    --apply-immediately
```

### 확장 트리거 조건
다음 조건이 모두 충족될 때 자동 확장:
- 사용 가능한 공간이 할당된 스토리지의 10% 미만
- 저용량 상태가 5분 이상 지속
- 마지막 스토리지 수정 후 6시간 이상 경과

### 확장 크기
- 다음 중 더 큰 값:
  - 5 GiB
  - 현재 할당 스토리지의 10%
  - 최근 1시간 FreeStorageSpace 변화량 기준 예측

## 성능 모니터링

### 주요 CloudWatch 메트릭

| 메트릭 | 설명 |
|-------|------|
| `ReadIOPS` | 초당 읽기 I/O 작업 수 |
| `WriteIOPS` | 초당 쓰기 I/O 작업 수 |
| `ReadLatency` | 읽기 작업당 평균 지연 시간 |
| `WriteLatency` | 쓰기 작업당 평균 지연 시간 |
| `ReadThroughput` | 초당 읽기 바이트 |
| `WriteThroughput` | 초당 쓰기 바이트 |
| `DiskQueueDepth` | 대기 중인 I/O 요청 수 |
| `BurstBalance` | gp2 버스트 크레딧 잔액 (%) |

## 관련 문서

- [DB 인스턴스 개요](../03-db-instances/overview.md)
- [모니터링](../08-monitoring/overview.md)
- [성능 최적화](./performance-optimization.md)
