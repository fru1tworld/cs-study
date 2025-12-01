# Amazon RDS 암호화

## 개요

Amazon RDS는 저장 데이터와 전송 데이터 모두에 대해 암호화를 지원합니다. 업계 표준 AES-256 암호화 알고리즘을 사용하여 데이터를 보호합니다.

## 저장 데이터 암호화 (Encryption at Rest)

### 암호화 대상
- DB 인스턴스 기본 스토리지
- 자동 백업
- 읽기 복제본
- DB 스냅샷
- 로그

### AWS KMS 통합

#### 키 유형

| 키 유형 | 설명 | 사용 사례 |
|--------|------|----------|
| **AWS 관리형 키** | AWS가 생성 및 관리 | 간편한 설정, 추가 비용 없음 |
| **고객 관리형 키** | 고객이 생성 및 관리 | 세부 제어, 규정 준수 요구 |

#### 고객 관리형 키 생성

```bash
aws kms create-key \
    --description "RDS encryption key" \
    --key-usage ENCRYPT_DECRYPT \
    --origin AWS_KMS
```

### 암호화 활성화

#### DB 인스턴스 생성 시

**AWS 콘솔:**
1. DB 인스턴스 생성 페이지에서
2. **Additional configuration** 섹션 확장
3. **Enable encryption** 선택
4. KMS 키 선택

**AWS CLI:**
```bash
aws rds create-db-instance \
    --db-instance-identifier mydb \
    --storage-encrypted \
    --kms-key-id arn:aws:kms:us-east-1:123456789012:key/abcd1234-... \
    ...
```

### 중요 제한사항

⚠️ **주의사항:**
- 암호화는 DB 인스턴스 생성 시에만 활성화 가능
- 이미 생성된 인스턴스에서 암호화 활성화/비활성화 불가
- 암호화된 인스턴스의 KMS 키 변경 불가

### 비암호화 인스턴스 암호화 방법

기존 비암호화 인스턴스를 암호화하려면:

1. **스냅샷 생성**
```bash
aws rds create-db-snapshot \
    --db-instance-identifier mydb \
    --db-snapshot-identifier mydb-snapshot
```

2. **암호화된 스냅샷 복사**
```bash
aws rds copy-db-snapshot \
    --source-db-snapshot-identifier mydb-snapshot \
    --target-db-snapshot-identifier mydb-snapshot-encrypted \
    --kms-key-id arn:aws:kms:us-east-1:123456789012:key/... \
    --copy-tags
```

3. **암호화된 스냅샷에서 복원**
```bash
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier mydb-encrypted \
    --db-snapshot-identifier mydb-snapshot-encrypted
```

### 읽기 복제본 암호화

- 원본 DB 인스턴스가 암호화된 경우, 읽기 복제본도 자동 암호화
- 동일한 KMS 키 사용 (동일 리전) 또는 다른 키 지정 가능 (크로스 리전)

```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb \
    --kms-key-id arn:aws:kms:us-west-2:123456789012:key/...
```

### 크로스 리전 스냅샷 복사

다른 리전으로 암호화된 스냅샷을 복사할 때는 대상 리전의 KMS 키를 지정해야 합니다:

```bash
aws rds copy-db-snapshot \
    --source-db-snapshot-identifier arn:aws:rds:us-east-1:123456789012:snapshot:mydb-snapshot \
    --target-db-snapshot-identifier mydb-snapshot-copy \
    --source-region us-east-1 \
    --kms-key-id arn:aws:kms:us-west-2:123456789012:key/... \
    --region us-west-2
```

## 전송 데이터 암호화 (Encryption in Transit)

### SSL/TLS 연결

모든 RDS 데이터베이스 엔진에서 SSL/TLS 연결을 지원합니다.

#### 지원 엔진별 설정

| 엔진 | SSL 파라미터 | 강제 적용 방법 |
|-----|-------------|--------------|
| MySQL | `require_secure_transport` | 파라미터 그룹 |
| PostgreSQL | `rds.force_ssl` | 파라미터 그룹 |
| MariaDB | `require_secure_transport` | 파라미터 그룹 |
| SQL Server | `rds.force_ssl` | 파라미터 그룹 |
| Oracle | `SQLNET.SSL_VERSION` | 옵션 그룹 |

### SSL 인증서

#### 인증서 다운로드

AWS에서 제공하는 루트 인증서 번들:
```bash
wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
```

리전별 인증서:
```bash
wget https://truststore.pki.rds.amazonaws.com/us-east-1/us-east-1-bundle.pem
```

### 데이터베이스별 SSL 연결

#### MySQL/MariaDB
```bash
mysql -h mydb.xxx.us-east-1.rds.amazonaws.com \
      -u admin \
      -p \
      --ssl-ca=global-bundle.pem \
      --ssl-mode=VERIFY_IDENTITY
```

#### PostgreSQL
```bash
psql "host=mydb.xxx.us-east-1.rds.amazonaws.com \
      port=5432 \
      dbname=mydb \
      user=admin \
      sslmode=verify-full \
      sslrootcert=global-bundle.pem"
```

#### SQL Server
```
Server=mydb.xxx.us-east-1.rds.amazonaws.com;
Database=mydb;
User Id=admin;
Password=xxx;
Encrypt=true;
TrustServerCertificate=false;
```

### SSL 연결 강제

#### MySQL
```sql
-- 사용자별 SSL 필수
ALTER USER 'myuser'@'%' REQUIRE SSL;

-- 파라미터 그룹에서 전역 설정
-- require_secure_transport = 1
```

#### PostgreSQL
```bash
# 파라미터 그룹에서 설정
aws rds modify-db-parameter-group \
    --db-parameter-group-name my-param-group \
    --parameters "ParameterName=rds.force_ssl,ParameterValue=1,ApplyMethod=pending-reboot"
```

## 암호화 모범 사례

### 저장 데이터
```
✅ 모든 프로덕션 DB 인스턴스에서 암호화 활성화
✅ 규정 준수 요구사항에 따라 고객 관리형 키 사용
✅ 정기적인 키 순환 정책 적용
✅ KMS 키에 대한 접근 권한 최소화
```

### 전송 데이터
```
✅ 모든 연결에 SSL/TLS 사용
✅ 가능한 경우 SSL 연결 강제
✅ 인증서 유효성 검사 활성화
✅ 인증서 만료 모니터링
```

### 키 관리
```
✅ 키 순환 자동화
✅ 키 삭제 전 대기 기간 설정
✅ CloudTrail로 키 사용 로깅
✅ 키 정책 정기 검토
```

## 암호화 지원 인스턴스 클래스

대부분의 현재 세대 인스턴스 클래스에서 암호화를 지원합니다.

**미지원 인스턴스 클래스 (레거시):**
- db.m1.*
- db.m2.*
- db.t1.*

## 관련 문서

- [보안 개요](./overview.md)
- [KMS 키 관리](https://docs.aws.amazon.com/kms/latest/developerguide/)
- [SSL/TLS 연결](./ssl-connections.md)
