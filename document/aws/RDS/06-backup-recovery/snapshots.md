# DB 스냅샷

## 개요

DB 스냅샷은 DB 인스턴스의 수동 백업입니다. 자동 백업과 달리 명시적으로 삭제하기 전까지 보존되며, 장기 보존 및 규정 준수 목적에 적합합니다.

## 스냅샷 유형

| 유형 | 생성 방법 | 보존 | 사용 사례 |
|-----|---------|-----|----------|
| ** 수동 스냅샷** | 사용자 생성 | 수동 삭제까지 | 장기 보존, 주요 변경 전 |
| ** 자동 스냅샷** | RDS 자동 생성 | 보존 기간까지 | 일상적 백업 |
| ** 공유 스냅샷** | 다른 계정에서 공유 | 소유자 제어 | 계정 간 데이터 이전 |
| ** 퍼블릭 스냅샷** | 공개 공유 | 소유자 제어 | 오픈 데이터 공유 |

## 수동 스냅샷 생성

### AWS 콘솔

1. RDS 콘솔에서 **Databases** 선택
2. 스냅샷을 생성할 DB 인스턴스 선택
3. **Actions** → **Take snapshot** 클릭
4. 스냅샷 이름 입력
5. **Take snapshot** 클릭

### AWS CLI

```bash
aws rds create-db-snapshot \
    --db-instance-identifier mydb \
    --db-snapshot-identifier mydb-snapshot-2024-01-15
```

### RDS API (Python)

```python
import boto3

rds = boto3.client('rds')

response = rds.create_db_snapshot(
    DBInstanceIdentifier='mydb',
    DBSnapshotIdentifier='mydb-snapshot-2024-01-15',
    Tags=[
        {'Key': 'Environment', 'Value': 'Production'},
        {'Key': 'Purpose', 'Value': 'Pre-upgrade backup'}
    ]
)
```

## 스냅샷 제한

### 계정당 제한
- 리전당 수동 스냅샷: ** 최대 100개**
- 추가 필요 시 AWS 서비스 한도 증가 요청

### 생성 시 고려사항
- 스냅샷 생성 중 I/O 일시 중단 가능 (단일 AZ)
- Multi-AZ 배포 시 대기 인스턴스에서 스냅샷 생성 (영향 최소화)
- 대용량 DB의 경우 생성 시간 증가

## 스냅샷 관리

### 스냅샷 조회

```bash
# 모든 스냅샷 조회
aws rds describe-db-snapshots

# 특정 인스턴스의 스냅샷 조회
aws rds describe-db-snapshots \
    --db-instance-identifier mydb

# 특정 스냅샷 상세 정보
aws rds describe-db-snapshots \
    --db-snapshot-identifier mydb-snapshot-2024-01-15
```

### 스냅샷 삭제

```bash
aws rds delete-db-snapshot \
    --db-snapshot-identifier mydb-snapshot-old
```

⚠️ ** 주의**: 삭제된 수동 스냅샷은 복구 불가

## 스냅샷 복사

### 동일 리전 내 복사

```bash
aws rds copy-db-snapshot \
    --source-db-snapshot-identifier mydb-snapshot \
    --target-db-snapshot-identifier mydb-snapshot-copy \
    --copy-tags
```

### 크로스 리전 복사

```bash
aws rds copy-db-snapshot \
    --source-db-snapshot-identifier arn:aws:rds:us-east-1:123456789012:snapshot:mydb-snapshot \
    --target-db-snapshot-identifier mydb-snapshot-west \
    --source-region us-east-1 \
    --region us-west-2 \
    --kms-key-id arn:aws:kms:us-west-2:123456789012:key/... \
    --copy-tags
```

### 암호화 스냅샷 복사
암호화된 스냅샷을 다른 리전으로 복사할 때는 대상 리전의 KMS 키 필요

## 스냅샷 공유

### 다른 AWS 계정과 공유

```bash
aws rds modify-db-snapshot-attribute \
    --db-snapshot-identifier mydb-snapshot \
    --attribute-name restore \
    --values-to-add 123456789012 987654321098
```

### 공유 취소

```bash
aws rds modify-db-snapshot-attribute \
    --db-snapshot-identifier mydb-snapshot \
    --attribute-name restore \
    --values-to-remove 123456789012
```

### 제한사항
- 암호화된 스냅샷: KMS 키 권한도 함께 공유 필요
- 기본 KMS 키로 암호화된 스냅샷: 공유 불가

## 스냅샷에서 복원

### 새 DB 인스턴스로 복원

```bash
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier mydb-restored \
    --db-snapshot-identifier mydb-snapshot \
    --db-instance-class db.m6g.large \
    --vpc-security-group-ids sg-0123456789abcdef0 \
    --db-subnet-group-name my-subnet-group
```

### 복원 시 재설정되는 항목
- 보안 그룹 (기본값)
- 파라미터 그룹 (기본값)
- 옵션 그룹 (엔진에 따라)
- 퍼블릭 액세스 설정
- VPC 설정 (명시적 지정 필요)

### 복원 후 필수 작업
1. 보안 그룹 재설정
2. 파라미터 그룹 연결
3. 모니터링 설정 복원
4. 읽기 복제본 재생성 (필요 시)

## 스냅샷 내보내기 (S3)

### Amazon S3로 내보내기

```bash
aws rds start-export-task \
    --export-task-identifier mydb-export-task \
    --source-arn arn:aws:rds:us-east-1:123456789012:snapshot:mydb-snapshot \
    --s3-bucket-name my-export-bucket \
    --iam-role-arn arn:aws:iam::123456789012:role/rds-export-role \
    --kms-key-id arn:aws:kms:us-east-1:123456789012:key/...
```

### 내보내기 형식
- Apache Parquet 형식
- Amazon Athena, Amazon Redshift Spectrum, Amazon EMR과 호환

### 사용 사례
- 데이터 분석
- 데이터 레이크 구축
- 규정 준수 아카이빙

## 태그를 통한 스냅샷 관리

### 태그 추가

```bash
aws rds add-tags-to-resource \
    --resource-name arn:aws:rds:us-east-1:123456789012:snapshot:mydb-snapshot \
    --tags Key=Environment,Value=Production Key=RetainUntil,Value=2025-01-15
```

### DB 인스턴스 태그 복사

```bash
aws rds create-db-snapshot \
    --db-instance-identifier mydb \
    --db-snapshot-identifier mydb-snapshot \
    --tags Key=CopiedFrom,Value=mydb
```

## 모범 사례

### 스냅샷 전략
```
✅ 주요 변경 전 수동 스냅샷 생성
✅ 정기적인 수동 스냅샷 스케줄 설정 (EventBridge + Lambda)
✅ 태그를 활용한 스냅샷 분류
✅ 보존 정책 정의 및 준수
```

### 비용 관리
```
✅ 사용하지 않는 오래된 스냅샷 정리
✅ 스냅샷 스토리지 모니터링
✅ 크로스 리전 복사 필요성 검토
```

### 보안
```
✅ 민감한 데이터는 암호화된 스냅샷 사용
✅ 스냅샷 공유 시 최소 필요 계정만 허용
✅ 공유 스냅샷 정기 검토
```

## 관련 문서

- [자동 백업](./automated-backups.md)
- [복구 절차](./restore-procedures.md)
- [암호화](../05-security/encryption.md)
