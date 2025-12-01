# Amazon RDS 시작하기

## 개요

이 문서에서는 Amazon RDS를 사용하여 DB 인스턴스를 생성하고 연결하는 방법을 설명합니다.

## 사전 요구사항

### 1. AWS 계정 설정
- AWS 계정 생성
- IAM 사용자 생성 및 적절한 권한 부여
- AWS Management Console 접근 확인

### 2. 네트워크 환경 구성

#### VPC 설정
- DB 인스턴스를 위한 VPC 생성 또는 기본 VPC 사용
- 최소 2개의 가용 영역에 서브넷 생성

#### DB 서브넷 그룹
- 최소 2개의 가용 영역에 걸친 서브넷을 포함하는 DB 서브넷 그룹 생성
- 프라이빗 서브넷 사용 권장

#### 보안 그룹
- 데이터베이스 포트에 대한 인바운드 규칙 설정
- 필요한 IP 주소 또는 EC2 인스턴스에서만 접근 허용

### 3. 기본 포트 번호

| 데이터베이스 엔진 | 기본 포트 |
|-----------------|----------|
| MySQL/MariaDB | 3306 |
| PostgreSQL | 5432 |
| Oracle | 1521 |
| SQL Server | 1433 |
| Db2 | 50000 |

## DB 인스턴스 생성

### AWS 콘솔을 통한 생성

1. **RDS 콘솔 접속**
   - AWS Management Console에서 RDS 선택
   - 적절한 AWS 리전 선택

2. ** 데이터베이스 생성 시작**
   - "데이터베이스 생성" 클릭
   - "표준 생성" 선택 (더 많은 구성 옵션 제공)

3. ** 엔진 선택**
   - 데이터베이스 엔진 유형 선택 (MySQL, PostgreSQL 등)
   - 엔진 버전 선택

4. ** 템플릿 선택**
   - ** 프로덕션**: 고가용성 및 성능 최적화
   - ** 개발/테스트**: 비용 효율적인 개발 환경
   - ** 프리 티어**: 무료 사용 한도 내 설정

5. ** 설정 구성**
   - DB 인스턴스 식별자 입력 (고유해야 함)
   - 마스터 사용자 이름 설정
   - 마스터 비밀번호 설정 (또는 AWS Secrets Manager 사용)

6. ** 인스턴스 구성**
   - DB 인스턴스 클래스 선택
   - 스토리지 유형 및 할당 용량 설정

7. ** 연결 설정**
   - VPC 선택
   - 서브넷 그룹 선택
   - 퍼블릭 액세스 설정 (프로덕션에서는 비활성화 권장)
   - VPC 보안 그룹 선택 또는 생성

8. ** 추가 구성**
   - 초기 데이터베이스 이름
   - 백업 보존 기간
   - 모니터링 옵션
   - 유지 관리 기간

9. ** 생성 완료**
   - "데이터베이스 생성" 클릭
   - 상태가 "사용 가능"으로 변경될 때까지 대기 (수 분 소요)

### AWS CLI를 통한 생성

```bash
aws rds create-db-instance \
    --db-instance-identifier mydbinstance \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 8.0 \
    --master-username admin \
    --master-user-password mypassword \
    --allocated-storage 20 \
    --vpc-security-group-ids sg-xxxxxxxx \
    --db-subnet-group-name mysubnetgroup
```

## DB 인스턴스에 연결

### 엔드포인트 확인
- RDS 콘솔에서 DB 인스턴스 선택
- "연결 및 보안" 탭에서 엔드포인트 확인
- 형식: `mydbinstance.abcdefghijkl.us-east-1.rds.amazonaws.com`

### 데이터베이스별 연결 방법

#### MySQL/MariaDB
```bash
mysql -h mydbinstance.abcdefghijkl.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p
```

#### PostgreSQL
```bash
psql -h mydbinstance.abcdefghijkl.us-east-1.rds.amazonaws.com \
     -p 5432 \
     -U admin \
     -d mydb
```

#### SQL Server
```bash
sqlcmd -S mydbinstance.abcdefghijkl.us-east-1.rds.amazonaws.com,1433 \
       -U admin \
       -P mypassword
```

## EC2 인스턴스와 자동 연결

RDS 콘솔에서 EC2 인스턴스와 DB 인스턴스를 자동으로 연결할 수 있습니다:

1. 동일한 VPC 내에 EC2 인스턴스 필요
2. DB 인스턴스 생성 또는 수정 시 "EC2 연결 설정" 옵션 사용
3. 자동으로 보안 그룹 규칙 구성

### 필요한 IAM 권한
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:AssociateRouteTable",
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:AuthorizeSecurityGroupEgress",
                "ec2:CreateRouteTable",
                "ec2:CreateSubnet",
                "ec2:CreateSecurityGroup",
                "ec2:DescribeInstances",
                "ec2:DescribeNetworkInterfaces",
                "ec2:DescribeSecurityGroups",
                "ec2:DescribeSubnets",
                "ec2:DescribeVpcs"
            ],
            "Resource": "*"
        }
    ]
}
```

## 다음 단계

- [DB 인스턴스 관리](../03-db-instances/overview.md)
- [보안 구성](../05-security/overview.md)
- [백업 및 복구](../06-backup-recovery/automated-backups.md)
- [모니터링 설정](../08-monitoring/overview.md)
