# 자동 백업

## 개요

Amazon RDS는 DB 인스턴스의 자동 백업을 생성하고 저장합니다. 자동 백업은 전체 데이터베이스 인스턴스의 스토리지 볼륨 스냅샷을 생성하며, 지정한 보존 기간 동안 보관됩니다.

## 자동 백업 구성 요소

### 1. 일일 스냅샷
- 백업 기간(Backup Window) 중 자동 생성
- 전체 DB 인스턴스 스냅샷

### 2. 트랜잭션 로그
- 5분마다 Amazon S3에 저장
- 특정 시점 복구(Point-in-Time Recovery) 지원

## 백업 보존 기간

### 설정 범위
- ** 최소**: 0일 (자동 백업 비활성화)
- ** 최대**: 35일
- ** 기본값**: 7일 (콘솔), 1일 (API/CLI)

### 보존 기간 설정

```bash
# 백업 보존 기간 설정
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --backup-retention-period 14 \
    --apply-immediately
```

## 백업 기간 (Backup Window)

### 개요
- 자동 백업이 수행되는 시간대
- 기본값: AWS가 리전별로 8시간 블록 내에서 자동 할당
- 사용자 지정 가능

### 설정 형식
```
hh24:mi-hh24:mi (UTC)
예: 03:00-04:00
```

### 권장 사항
```
✅ 저부하 시간대 선택
✅ 백업 시간 동안 약간의 I/O 일시 중단 예상
✅ Multi-AZ 배포 시 대기 인스턴스에서 백업 (영향 최소화)
```

## 자동 백업 요구사항

### 백업 수행 조건
1. DB 인스턴스가 **available** 상태
2. 동일 리전에서 DB 스냅샷 복사가 진행 중이지 않음

### 건너뛰는 경우
- 인스턴스가 available 상태가 아닌 경우
- 스토리지 전체 상태인 경우
- 대규모 수정 작업 진행 중인 경우

## 백업 스토리지

### 저장 위치
- Amazon S3에 저장
- 사용자에게 직접 보이지 않음
- 동일 리전에 저장

### 비용
| 항목 | 비용 |
|-----|-----|
| 프로비저닝된 스토리지 크기까지 | 무료 |
| 초과 스토리지 | GB당 요금 부과 |

### 계산 예시
```
DB 인스턴스 스토리지: 100 GB
자동 백업 사용량: 80 GB
→ 추가 비용 없음

자동 백업 사용량: 150 GB
→ 50 GB에 대해 비용 발생
```

## 자동 백업 활성화/비활성화

### 활성화
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --backup-retention-period 7 \
    --preferred-backup-window "03:00-04:00" \
    --apply-immediately
```

### 비활성화
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --backup-retention-period 0 \
    --apply-immediately
```

⚠️ ** 주의**: 보존 기간을 0으로 설정하면:
- 자동 백업 비활성화
- 기존 자동 백업 삭제
- Point-in-Time Recovery 불가

## DB 인스턴스 삭제 시 백업 처리

### 삭제 옵션

| 옵션 | 동작 |
|-----|------|
| 자동 백업 보존 | 보존 기간 동안 유지, 이후 자동 삭제 |
| 자동 백업 삭제 | 즉시 삭제, 복구 불가 |

### CLI 예시
```bash
# 자동 백업 보존하며 삭제
aws rds delete-db-instance \
    --db-instance-identifier mydb \
    --skip-final-snapshot \
    --no-delete-automated-backups

# 자동 백업 함께 삭제
aws rds delete-db-instance \
    --db-instance-identifier mydb \
    --skip-final-snapshot \
    --delete-automated-backups
```

## 보존된 자동 백업 관리

### 조회
```bash
aws rds describe-db-instance-automated-backups \
    --db-instance-identifier mydb
```

### 삭제된 인스턴스의 백업 조회
```bash
aws rds describe-db-instance-automated-backups \
    --filters Name=status,Values=retained
```

### 보존된 백업 삭제
```bash
aws rds delete-db-instance-automated-backup \
    --dbi-resource-id db-ABCDEFGHIJKLMNOP
```

## 특정 시점 복구 (Point-in-Time Recovery)

### 개요
- 보존 기간 내 어느 시점으로든 복구 가능
- 최근 5분 이내까지 복구 가능 (Latest Restorable Time)

### 복구 절차

```bash
aws rds restore-db-instance-to-point-in-time \
    --source-db-instance-identifier mydb \
    --target-db-instance-identifier mydb-restored \
    --restore-time 2024-01-15T10:30:00Z
```

### 최신 복구 가능 시점으로 복구
```bash
aws rds restore-db-instance-to-point-in-time \
    --source-db-instance-identifier mydb \
    --target-db-instance-identifier mydb-restored \
    --use-latest-restorable-time
```

### 복구 시 고려사항
- 새로운 DB 인스턴스로 복구됨 (기존 인스턴스 덮어쓰기 아님)
- 보안 그룹, 파라미터 그룹 등은 기본값으로 설정
- 수동으로 원래 설정 복원 필요

## 크로스 리전 자동 백업

### 활성화
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --enable-cross-region-backup \
    --backup-target-region us-west-2
```

### 제한사항
- 추가 스토리지 비용 발생
- 데이터 전송 비용 발생
- 원본과 동일한 보존 기간 적용

## 모범 사례

### 백업 전략
```
✅ 백업 보존 기간을 비즈니스 요구사항에 맞게 설정
✅ 수동 스냅샷과 자동 백업 병행 사용
✅ 중요 변경 전 수동 스냅샷 생성
✅ 정기적인 복구 테스트 수행
```

### 비용 최적화
```
✅ 필요한 보존 기간만 설정
✅ 사용하지 않는 보존된 백업 삭제
✅ 개발/테스트 환경은 짧은 보존 기간 사용
```

## 관련 문서

- [수동 스냅샷](./snapshots.md)
- [복구 절차](./restore-procedures.md)
- [크로스 리전 백업](./cross-region-backup.md)
