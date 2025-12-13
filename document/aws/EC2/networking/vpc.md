# EC2와 VPC (Virtual Private Cloud)

## 개요

Amazon VPC는 AWS 클라우드에서 논리적으로 격리된 가상 네트워크입니다. EC2 인스턴스는 VPC 내의 서브넷에서 실행됩니다.

## VPC 기본 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                          VPC (10.0.0.0/16)                  │
│  ┌────────────────────────┐ ┌────────────────────────────┐ │
│  │    가용 영역 A          │ │    가용 영역 B              │ │
│  │  ┌──────────────────┐  │ │  ┌──────────────────────┐  │ │
│  │  │ 퍼블릭 서브넷     │  │ │  │ 퍼블릭 서브넷         │  │ │
│  │  │ 10.0.1.0/24      │  │ │  │ 10.0.2.0/24          │  │ │
│  │  │   ┌──────────┐   │  │ │  │   ┌──────────┐       │  │ │
│  │  │   │ EC2      │   │  │ │  │   │ EC2      │       │  │ │
│  │  │   └──────────┘   │  │ │  │   └──────────┘       │  │ │
│  │  └──────────────────┘  │ │  └──────────────────────┘  │ │
│  │  ┌──────────────────┐  │ │  ┌──────────────────────┐  │ │
│  │  │ 프라이빗 서브넷   │  │ │  │ 프라이빗 서브넷       │  │ │
│  │  │ 10.0.3.0/24      │  │ │  │ 10.0.4.0/24          │  │ │
│  │  │   ┌──────────┐   │  │ │  │   ┌──────────┐       │  │ │
│  │  │   │ EC2      │   │  │ │  │   │ EC2      │       │  │ │
│  │  │   └──────────┘   │  │ │  │   └──────────┘       │  │ │
│  │  └──────────────────┘  │ │  └──────────────────────┘  │ │
│  └────────────────────────┘ └────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 서브넷

### 퍼블릭 서브넷 vs 프라이빗 서브넷

| 특성 | 퍼블릭 서브넷 | 프라이빗 서브넷 |
|------|--------------|----------------|
| 인터넷 접근 | 직접 가능 | NAT를 통해 가능 |
| 퍼블릭 IP | 자동 할당 가능 | 일반적으로 없음 |
| 라우팅 | IGW로 라우팅 | NAT GW로 라우팅 |
| 사용 사례 | 웹 서버, 로드 밸런서 | DB, 백엔드 서버 |

### 서브넷 생성

```bash
# 퍼블릭 서브넷 생성
aws ec2 create-subnet \
    --vpc-id vpc-xxxxxxxxx \
    --cidr-block 10.0.1.0/24 \
    --availability-zone ap-northeast-2a

# 서브넷에 퍼블릭 IP 자동 할당 활성화
aws ec2 modify-subnet-attribute \
    --subnet-id subnet-xxxxxxxxx \
    --map-public-ip-on-launch
```

## 인터넷 연결

### 인터넷 게이트웨이 (IGW)

VPC에서 인터넷으로의 양방향 통신을 가능하게 합니다.

```bash
# IGW 생성 및 연결
aws ec2 create-internet-gateway
aws ec2 attach-internet-gateway \
    --internet-gateway-id igw-xxxxxxxxx \
    --vpc-id vpc-xxxxxxxxx

# 라우팅 테이블에 IGW 경로 추가
aws ec2 create-route \
    --route-table-id rtb-xxxxxxxxx \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id igw-xxxxxxxxx
```

### NAT 게이트웨이

프라이빗 서브넷의 인스턴스가 인터넷에 접근할 수 있게 합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                          VPC                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  │
│  │    퍼블릭 서브넷         │  │    프라이빗 서브넷       │  │
│  │  ┌───────────────┐      │  │  ┌───────────────┐      │  │
│  │  │  NAT Gateway  │◄─────┼──┼──│   EC2         │      │  │
│  │  └───────┬───────┘      │  │  └───────────────┘      │  │
│  └──────────┼──────────────┘  └─────────────────────────┘  │
│             │                                               │
│             ▼                                               │
│       ┌───────────┐                                        │
│       │    IGW    │                                        │
│       └─────┬─────┘                                        │
└─────────────┼───────────────────────────────────────────────┘
              ▼
         인터넷
```

```bash
# NAT Gateway 생성
aws ec2 create-nat-gateway \
    --subnet-id subnet-public \
    --allocation-id eipalloc-xxxxxxxxx

# 프라이빗 서브넷 라우팅 테이블에 NAT GW 경로 추가
aws ec2 create-route \
    --route-table-id rtb-private \
    --destination-cidr-block 0.0.0.0/0 \
    --nat-gateway-id nat-xxxxxxxxx
```

## 라우팅 테이블

### 구성 요소

| 요소 | 설명 |
|------|------|
| 대상 (Destination) | 트래픽의 목적지 CIDR |
| 대상 (Target) | 트래픽이 전달될 게이트웨이 또는 엔드포인트 |
| 상태 | 라우팅 활성 상태 |

### 예시: 퍼블릭 서브넷 라우팅 테이블

| 대상 | 대상 | 용도 |
|------|------|------|
| 10.0.0.0/16 | local | VPC 내부 통신 |
| 0.0.0.0/0 | igw-xxx | 인터넷 통신 |

### 예시: 프라이빗 서브넷 라우팅 테이블

| 대상 | 대상 | 용도 |
|------|------|------|
| 10.0.0.0/16 | local | VPC 내부 통신 |
| 0.0.0.0/0 | nat-xxx | 아웃바운드 인터넷 |

## 네트워크 ACL

서브넷 수준의 방화벽으로 인바운드와 아웃바운드 트래픽을 제어합니다.

### 특징

| 특성 | 설명 |
|------|------|
| 적용 범위 | 서브넷 |
| 규칙 평가 | 번호 순서대로 |
| 상태 | Stateless |
| 기본 동작 | 모든 트래픽 허용 |

### 규칙 예시

```bash
# 인바운드 규칙 추가 (HTTP 허용)
aws ec2 create-network-acl-entry \
    --network-acl-id acl-xxxxxxxxx \
    --rule-number 100 \
    --protocol tcp \
    --port-range From=80,To=80 \
    --cidr-block 0.0.0.0/0 \
    --rule-action allow \
    --ingress
```

### 보안 그룹 vs 네트워크 ACL

| 특성 | 보안 그룹 | 네트워크 ACL |
|------|-----------|--------------|
| 적용 수준 | 인스턴스 | 서브넷 |
| 상태 | Stateful | Stateless |
| 규칙 | 허용만 | 허용/거부 |
| 평가 순서 | 모든 규칙 평가 | 번호 순서 |
| 기본 동작 | 모두 거부 | 모두 허용 |

## VPC 피어링

두 VPC 간의 프라이빗 연결을 제공합니다.

```
┌────────────────┐         ┌────────────────┐
│    VPC A       │         │    VPC B       │
│  10.0.0.0/16   │◄───────▶│  172.16.0.0/16 │
│                │  피어링  │                │
└────────────────┘         └────────────────┘
```

### 제한 사항

- 전이적 피어링 불가 (A↔B, B↔C 피어링 시 A↔C 직접 통신 불가)
- CIDR 중복 불가
- 리전 간 피어링 가능 (Inter-Region VPC Peering)

## VPC 엔드포인트

VPC에서 AWS 서비스로 프라이빗하게 연결합니다.

### 게이트웨이 엔드포인트

- S3와 DynamoDB 지원
- 무료
- 라우팅 테이블에 추가

```bash
# S3 게이트웨이 엔드포인트 생성
aws ec2 create-vpc-endpoint \
    --vpc-id vpc-xxxxxxxxx \
    --service-name com.amazonaws.ap-northeast-2.s3 \
    --route-table-ids rtb-xxxxxxxxx
```

### 인터페이스 엔드포인트

- 대부분의 AWS 서비스 지원
- ENI 기반
- 시간당 요금 + 데이터 처리 요금

```bash
# SSM 인터페이스 엔드포인트 생성
aws ec2 create-vpc-endpoint \
    --vpc-id vpc-xxxxxxxxx \
    --service-name com.amazonaws.ap-northeast-2.ssm \
    --vpc-endpoint-type Interface \
    --subnet-ids subnet-xxxxxxxxx \
    --security-group-ids sg-xxxxxxxxx
```

## 기본 VPC

모든 AWS 계정에는 각 리전에 기본 VPC가 있습니다.

### 기본 VPC 특성

- CIDR: 172.31.0.0/16
- 각 가용 영역에 /20 서브넷
- 인터넷 게이트웨이 연결됨
- 퍼블릭 IP 자동 할당 활성화

### 기본 VPC 복구

```bash
# 삭제된 기본 VPC 복구
aws ec2 create-default-vpc
```

## 참고 자료

- [Amazon VPC User Guide](https://docs.aws.amazon.com/vpc/latest/userguide/)
- [VPC 피어링](https://docs.aws.amazon.com/vpc/latest/peering/)
- [VPC 엔드포인트](https://docs.aws.amazon.com/vpc/latest/privatelink/)
