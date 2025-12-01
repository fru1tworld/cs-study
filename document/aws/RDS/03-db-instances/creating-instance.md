# DB 인스턴스 생성

## 사전 조건

### 네트워크 구성

#### VPC 요구사항
- DB 인스턴스는 VPC 내에서만 생성 가능
- 최소 2개의 가용 영역을 지원하는 AWS 리전 필요

#### DB 서브넷 그룹
- 최소 2개의 가용 영역에 걸친 서브넷 포함 필요
- 프라이빗 서브넷 사용 권장

### IAM 권한

EC2 자동 연결 설정을 위한 필수 권한:
- `ec2:AssociateRouteTable`
- `ec2:AuthorizeSecurityGroupIngress`
- `ec2:AuthorizeSecurityGroupEgress`
- `ec2:CreateRouteTable`
- `ec2:CreateSubnet`
- `ec2:CreateSecurityGroup`
- `ec2:DescribeInstances`
- `ec2:DescribeNetworkInterfaces`
- `ec2:DescribeSecurityGroups`
- `ec2:DescribeSubnets`
- `ec2:DescribeVpcs`

## AWS 콘솔을 통한 생성

### 1단계: RDS 콘솔 접속
- AWS Management Console 로그인
- RDS 서비스 선택
- 원하는 AWS 리전 선택

### 2단계: 데이터베이스 생성 시작
- 탐색 창에서 **Databases** 선택
- **Create database** 클릭
- **Standard create** 선택 (상세 구성 옵션 제공)

### 3단계: 엔진 선택
사용 가능한 엔진:
- Db2
- MariaDB
- MySQL
- Oracle
- PostgreSQL
- SQL Server

#### 관리 유형 선택 (Oracle/SQL Server)
- **Amazon RDS**: 완전 관리형
- **RDS Custom**: 더 많은 제어권 제공

### 4단계: 템플릿 선택

| 템플릿 | 설명 | 권장 환경 |
|-------|------|----------|
| **Production** | 고가용성, 성능 최적화 | 프로덕션 |
| **Dev/Test** | 비용 효율적 | 개발/테스트 |
| **Free tier** | 무료 사용 한도 | 학습/실험 |

### 5단계: 자격 증명 설정

#### 마스터 사용자
- 고유한 마스터 사용자 이름 지정
- 예약어 사용 불가

#### 비밀번호 관리
1. **AWS Secrets Manager 사용** (권장)
   - 자동 비밀번호 생성
   - 자동 순환 지원
   - 추가 비용 발생

2. ** 자체 관리**
   - 사용자가 직접 비밀번호 설정
   - 비밀번호 정책 준수 필요

### 6단계: 인스턴스 구성

#### DB 인스턴스 클래스 선택
```
db.<class-type>.<size>
예: db.m6g.large, db.r6i.xlarge, db.t3.micro
```

#### 스토리지 설정
- 스토리지 유형 선택 (gp3, io1, io2)
- 할당 용량 지정
- 자동 확장 활성화 여부

### 7단계: 연결 설정

#### EC2 연결 설정
- 기존 EC2 인스턴스와 자동 연결 가능
- 자동으로 보안 그룹 규칙 구성

#### 수동 연결 설정
- VPC 선택
- 서브넷 그룹 선택
- 퍼블릭 액세스: ** 아니요** (프로덕션 권장)
- VPC 보안 그룹 선택

### 8단계: 추가 구성

#### 데이터베이스 옵션
- 초기 데이터베이스 이름
- DB 파라미터 그룹
- 옵션 그룹

#### 백업
- 자동 백업 활성화
- 백업 보존 기간 (1-35일)
- 백업 기간

#### 모니터링
- Enhanced Monitoring
- Performance Insights
- CloudWatch 로그 내보내기

### 9단계: 생성 완료
- 모든 설정 검토
- **Create database** 클릭
- 상태가 **Available** 로 변경될 때까지 대기

## AWS CLI를 통한 생성

### 기본 명령어

```bash
aws rds create-db-instance \
    --db-instance-identifier mydbinstance \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 8.0.35 \
    --master-username admin \
    --master-user-password MySecurePassword123! \
    --allocated-storage 20 \
    --storage-type gp3 \
    --vpc-security-group-ids sg-0123456789abcdef0 \
    --db-subnet-group-name my-subnet-group \
    --backup-retention-period 7 \
    --no-publicly-accessible \
    --auto-minor-version-upgrade
```

### 자동 생성 비밀번호 사용

```bash
aws rds create-db-instance \
    --db-instance-identifier mydbinstance \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --master-username admin \
    --manage-master-user-password \
    --allocated-storage 20
```

### 필수 파라미터

| 파라미터 | 설명 |
|---------|------|
| `--db-instance-identifier` | 인스턴스 식별자 |
| `--db-instance-class` | 인스턴스 클래스 |
| `--engine` | 데이터베이스 엔진 |
| `--master-username` | 마스터 사용자 이름 |
| `--allocated-storage` | 스토리지 용량 (GB) |

## RDS API를 통한 생성

```python
import boto3

rds = boto3.client('rds')

response = rds.create_db_instance(
    DBInstanceIdentifier='mydbinstance',
    DBInstanceClass='db.t3.micro',
    Engine='mysql',
    MasterUsername='admin',
    MasterUserPassword='MySecurePassword123!',
    AllocatedStorage=20,
    VpcSecurityGroupIds=['sg-0123456789abcdef0'],
    DBSubnetGroupName='my-subnet-group'
)

print(response['DBInstance']['DBInstanceIdentifier'])
```

## 생성 후 확인

### 엔드포인트 확인
```bash
aws rds describe-db-instances \
    --db-instance-identifier mydbinstance \
    --query 'DBInstances[0].Endpoint'
```

### 상태 확인
```bash
aws rds describe-db-instances \
    --db-instance-identifier mydbinstance \
    --query 'DBInstances[0].DBInstanceStatus'
```

## 주의사항

1. ** 비밀번호 보관**: 마스터 비밀번호는 생성 후 재확인 불가
2. ** 리전 선택**: 한번 생성된 인스턴스는 다른 리전으로 이동 불가
3. ** 생성 시간**: 몇 분에서 수십 분까지 소요될 수 있음
4. ** 비용**: 생성 즉시 시간당 요금 부과 시작

## 관련 문서

- [DB 인스턴스 개요](./overview.md)
- [스토리지 구성](../04-storage/storage-types.md)
- [VPC 구성](../05-security/vpc-configuration.md)
