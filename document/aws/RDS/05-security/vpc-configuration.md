# VPC 구성

## 개요

Amazon VPC(Virtual Private Cloud)를 통해 RDS DB 인스턴스를 가상 프라이빗 클라우드 내에 배포할 수 있습니다. VPC 내 DB 인스턴스 운영에는 추가 비용이 발생하지 않습니다.

## 핵심 개념

### VPC
- 논리적으로 격리된 가상 네트워크
- IP 주소 범위, 서브넷, 라우팅 테이블, 네트워크 게이트웨이 정의
- 리전 수준 리소스

### 서브넷
- VPC 내의 IP 주소 범위 세그먼트
- 특정 가용 영역(AZ)에 연결
- 퍼블릭 또는 프라이빗으로 구성

### DB 서브넷 그룹
- RDS가 사용할 서브넷 집합 정의
- 최소 2개의 가용 영역에 걸친 서브넷 필요
- Multi-AZ 배포를 위한 필수 요구사항

## DB 서브넷 그룹 생성

### AWS 콘솔

1. RDS 콘솔에서 **Subnet groups** 선택
2. **Create DB subnet group** 클릭
3. 이름 및 설명 입력
4. VPC 선택
5. 가용 영역별 서브넷 추가
6. **Create** 클릭

### AWS CLI

```bash
aws rds create-db-subnet-group \
    --db-subnet-group-name my-subnet-group \
    --db-subnet-group-description "My DB subnet group" \
    --subnet-ids subnet-0123456789abcdef0 subnet-0987654321fedcba0
```

## 네트워크 구성 시나리오

### 시나리오 1: 동일 VPC 내 EC2에서 접근

```
┌─────────────────────────────────────────┐
│              VPC (10.0.0.0/16)          │
│  ┌─────────────────┐ ┌─────────────────┐│
│  │ Public Subnet   │ │ Private Subnet  ││
│  │ (10.0.1.0/24)   │ │ (10.0.2.0/24)   ││
│  │                 │ │                 ││
│  │  ┌───────────┐  │ │  ┌───────────┐  ││
│  │  │    EC2    │  │ │  │    RDS    │  ││
│  │  └───────────┘  │ │  └───────────┘  ││
│  └─────────────────┘ └─────────────────┘│
└─────────────────────────────────────────┘
```

**설정 포인트:**
- EC2: 퍼블릭 서브넷 (인터넷 접근 필요 시)
- RDS: 프라이빗 서브넷 (퍼블릭 액세스 비활성화)
- 보안 그룹: EC2에서 RDS로의 인바운드 허용

### 시나리오 2: VPC 피어링을 통한 다른 VPC에서 접근

```
┌─────────────────────┐     ┌─────────────────────┐
│    VPC A            │     │    VPC B            │
│  (10.0.0.0/16)      │     │  (10.1.0.0/16)      │
│                     │     │                     │
│  ┌───────────┐      │     │      ┌───────────┐  │
│  │    EC2    │ ─────┼─────┼───── │    RDS    │  │
│  └───────────┘      │     │      └───────────┘  │
│                     │ VPC │                     │
│                     │Peering                    │
└─────────────────────┘     └─────────────────────┘
```

**설정 포인트:**
- VPC 피어링 연결 생성
- 양쪽 라우팅 테이블 업데이트
- 보안 그룹에서 피어 VPC CIDR 허용

### 시나리오 3: VPN 또는 Direct Connect를 통한 온프레미스 접근

```
┌─────────────────────┐         ┌─────────────────┐
│    On-Premises      │         │      VPC        │
│                     │         │                 │
│  ┌───────────┐      │   VPN   │  ┌───────────┐  │
│  │ 애플리케이션│ ─────┼─────────┼─ │    RDS    │  │
│  └───────────┘      │         │  └───────────┘  │
│                     │         │                 │
└─────────────────────┘         └─────────────────┘
```

**설정 포인트:**
- Site-to-Site VPN 또는 Direct Connect 설정
- VPC 라우팅 테이블에 온프레미스 CIDR 추가
- 보안 그룹에서 온프레미스 IP 허용

## 보안 그룹 구성

### 인바운드 규칙 예시

```bash
# MySQL/MariaDB
aws ec2 authorize-security-group-ingress \
    --group-id sg-0123456789abcdef0 \
    --protocol tcp \
    --port 3306 \
    --source-group sg-ec2-security-group

# PostgreSQL
aws ec2 authorize-security-group-ingress \
    --group-id sg-0123456789abcdef0 \
    --protocol tcp \
    --port 5432 \
    --cidr 10.0.1.0/24
```

### 권장 보안 그룹 규칙

| 유형 | 프로토콜 | 포트 | 소스 |
|-----|---------|-----|------|
| Inbound | TCP | 3306 (MySQL) | 애플리케이션 보안 그룹 |
| Inbound | TCP | 5432 (PostgreSQL) | 애플리케이션 보안 그룹 |
| Outbound | All | All | 0.0.0.0/0 (또는 제한) |

## 퍼블릭 접근 설정

### 퍼블릭 액세스 활성화 (권장하지 않음)

⚠️ **주의**: 프로덕션 환경에서는 권장하지 않습니다.

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --publicly-accessible \
    --apply-immediately
```

### 퍼블릭 액세스 요구사항
- DB 인스턴스가 퍼블릭 서브넷에 위치
- 인터넷 게이트웨이가 VPC에 연결
- 보안 그룹에서 외부 IP 허용

### 더 안전한 대안
1. **Bastion Host (Jump Box)**: EC2를 통한 SSH 터널링
2. **AWS Client VPN**: 관리형 VPN 클라이언트
3. **AWS Systems Manager Session Manager**: 에이전트 기반 접근

## VPC 엔드포인트

### RDS API용 VPC 엔드포인트

AWS 서비스와의 프라이빗 연결을 위해 VPC 엔드포인트 사용:

```bash
aws ec2 create-vpc-endpoint \
    --vpc-id vpc-0123456789abcdef0 \
    --service-name com.amazonaws.us-east-1.rds \
    --vpc-endpoint-type Interface \
    --subnet-ids subnet-0123456789abcdef0 \
    --security-group-ids sg-0123456789abcdef0
```

## VPC 구성 모범 사례

### 네트워크 설계
```
✅ 프라이빗 서브넷에 DB 인스턴스 배치
✅ 최소 2개 가용 영역에 서브넷 생성
✅ NAT 게이트웨이로 아웃바운드 인터넷 접근 제공 (필요 시)
✅ 적절한 CIDR 블록 계획 (향후 확장 고려)
```

### 보안
```
✅ 보안 그룹에서 최소 필요 포트만 허용
✅ 소스를 IP 대신 보안 그룹으로 참조
✅ 네트워크 ACL로 서브넷 수준 추가 보호
✅ VPC Flow Logs 활성화
```

### 고가용성
```
✅ Multi-AZ 배포 사용
✅ 각 AZ에 충분한 IP 주소 확보
✅ 장애 조치 시나리오 테스트
```

## 관련 문서

- [보안 개요](./overview.md)
- [네트워크 접근 제어](./network-access.md)
- [Multi-AZ 배포](../07-high-availability/multi-az.md)
