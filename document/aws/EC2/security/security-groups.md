# EC2 보안 그룹

## 개요

보안 그룹은 EC2 인스턴스에 대한 가상 방화벽으로, 인바운드(들어오는)와 아웃바운드(나가는) 트래픽을 제어합니다.

## 특징

| 특성 | 설명 |
|------|------|
| ** 적용 범위** | 인스턴스 수준 (ENI) |
| ** 규칙 유형** | 허용 규칙만 (거부 규칙 없음) |
| ** 상태** | Stateful (응답 트래픽 자동 허용) |
| ** 기본 동작** | 인바운드 - 모두 거부, 아웃바운드 - 모두 허용 |
| ** 규칙 평가** | 모든 규칙 평가 |
| ** 비용** | 무료 |

## 보안 그룹 구조

```
┌─────────────────────────────────────────────────────────────┐
│                       보안 그룹                              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  인바운드 규칙                        │   │
│  │  ┌───────────────────────────────────────────────┐  │   │
│  │  │ 유형 | 프로토콜 | 포트 범위 | 소스           │  │   │
│  │  ├───────────────────────────────────────────────┤  │   │
│  │  │ SSH  | TCP      | 22        | 10.0.0.0/8    │  │   │
│  │  │ HTTP | TCP      | 80        | 0.0.0.0/0     │  │   │
│  │  │ HTTPS| TCP      | 443       | 0.0.0.0/0     │  │   │
│  │  └───────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  아웃바운드 규칙                      │   │
│  │  ┌───────────────────────────────────────────────┐  │   │
│  │  │ 유형  | 프로토콜 | 포트 범위 | 대상          │  │   │
│  │  ├───────────────────────────────────────────────┤  │   │
│  │  │ 모든  | 모든     | 모든      | 0.0.0.0/0    │  │   │
│  │  └───────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 보안 그룹 관리

### 생성

```bash
aws ec2 create-security-group \
    --group-name my-security-group \
    --description "My security group" \
    --vpc-id vpc-xxxxxxxxx
```

### 인바운드 규칙 추가

```bash
# SSH 허용 (특정 IP)
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 22 \
    --cidr 203.0.113.0/24

# HTTP 허용 (모든 곳에서)
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

# 다른 보안 그룹에서 허용
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 3306 \
    --source-group sg-yyyyyyyyy
```

### 아웃바운드 규칙 수정

```bash
# 기본 아웃바운드 규칙 제거
aws ec2 revoke-security-group-egress \
    --group-id sg-xxxxxxxxx \
    --protocol all \
    --cidr 0.0.0.0/0

# 특정 아웃바운드 규칙 추가
aws ec2 authorize-security-group-egress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0
```

### 규칙 삭제

```bash
aws ec2 revoke-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 22 \
    --cidr 203.0.113.0/24
```

## 일반적인 보안 그룹 패턴

### 웹 서버

```
인바운드:
┌──────────────────────────────────────────────┐
│ HTTP (80)   | TCP | 0.0.0.0/0               │
│ HTTPS (443) | TCP | 0.0.0.0/0               │
│ SSH (22)    | TCP | 관리자 IP/32            │
└──────────────────────────────────────────────┘
```

### 데이터베이스 서버

```
인바운드:
┌──────────────────────────────────────────────┐
│ MySQL (3306)   | TCP | 웹 서버 보안 그룹     │
│ PostgreSQL(5432)| TCP | 앱 서버 보안 그룹    │
└──────────────────────────────────────────────┘
```

### 3계층 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│    인터넷                                                    │
│       │                                                      │
│       ▼                                                      │
│  ┌─────────────────────────────────┐                        │
│  │       웹 서버 보안 그룹          │                        │
│  │  인바운드: 80, 443 from 0.0.0.0  │                        │
│  │  아웃바운드: 앱 서버 SG          │                        │
│  └─────────────────────────────────┘                        │
│       │                                                      │
│       ▼                                                      │
│  ┌─────────────────────────────────┐                        │
│  │       앱 서버 보안 그룹          │                        │
│  │  인바운드: 8080 from 웹 서버 SG  │                        │
│  │  아웃바운드: DB 서버 SG          │                        │
│  └─────────────────────────────────┘                        │
│       │                                                      │
│       ▼                                                      │
│  ┌─────────────────────────────────┐                        │
│  │       DB 서버 보안 그룹          │                        │
│  │  인바운드: 3306 from 앱 서버 SG  │                        │
│  │  아웃바운드: 없음 (또는 제한적)   │                        │
│  └─────────────────────────────────┘                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Stateful 특성

보안 그룹은 Stateful입니다. 허용된 인바운드 트래픽에 대한 응답은 아웃바운드 규칙과 관계없이 허용됩니다.

```
┌─────────────────────────────────────────────────────────────┐
│  클라이언트                          서버 (인바운드 80 허용)  │
│      │                                    │                 │
│      │────────── 요청 (80) ──────────────▶│ ← 인바운드 규칙  │
│      │◀───────── 응답 (49152) ───────────│ ← 자동 허용     │
│      │                                    │                 │
└─────────────────────────────────────────────────────────────┘
```

## 보안 그룹 참조

다른 보안 그룹을 소스/대상으로 사용할 수 있습니다.

### 장점

- IP 주소 변경에 독립적
- 인스턴스 추가/제거 시 규칙 수정 불필요
- 명확한 의도 표현

```bash
# 보안 그룹 참조로 규칙 추가
aws ec2 authorize-security-group-ingress \
    --group-id sg-database \
    --protocol tcp \
    --port 3306 \
    --source-group sg-appserver
```

## 기본 보안 그룹

각 VPC에는 기본 보안 그룹이 있습니다.

### 기본 규칙

| 규칙 유형 | 프로토콜 | 포트 | 소스/대상 |
|-----------|----------|------|-----------|
| 인바운드 | 모두 | 모두 | 같은 보안 그룹 |
| 아웃바운드 | 모두 | 모두 | 0.0.0.0/0 |

### 주의 사항

- 기본 보안 그룹은 삭제할 수 없음
- 규칙 수정은 가능
- 프로덕션에서는 커스텀 보안 그룹 사용 권장

## 할당량

| 리소스 | 기본 한도 |
|--------|----------|
| VPC당 보안 그룹 | 2,500 |
| 보안 그룹당 인바운드 규칙 | 60 |
| 보안 그룹당 아웃바운드 규칙 | 60 |
| 네트워크 인터페이스당 보안 그룹 | 5 |

> 인바운드와 아웃바운드 규칙은 별도로 계산됩니다.

## 모범 사례

### 1. 최소 권한 원칙

```bash
# 나쁜 예: 모든 트래픽 허용
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol all \
    --cidr 0.0.0.0/0

# 좋은 예: 필요한 포트만 허용
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0
```

### 2. CIDR 대신 보안 그룹 참조 사용

### 3. 역할별 보안 그룹 분리

- 웹 서버, 앱 서버, DB 서버 각각 별도 보안 그룹
- 관리 접근용 별도 보안 그룹

### 4. 설명 태그 사용

```bash
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --ip-permissions '[{
        "IpProtocol": "tcp",
        "FromPort": 22,
        "ToPort": 22,
        "IpRanges": [{
            "CidrIp": "203.0.113.0/24",
            "Description": "SSH from office network"
        }]
    }]'
```

### 5. 정기적인 감사

```bash
# 모든 보안 그룹의 인바운드 규칙 확인
aws ec2 describe-security-groups \
    --query 'SecurityGroups[*].[GroupName,IpPermissions]'

# 0.0.0.0/0이 있는 규칙 찾기
aws ec2 describe-security-groups \
    --query 'SecurityGroups[?IpPermissions[?IpRanges[?CidrIp==`0.0.0.0/0`]]]'
```

## 문제 해결

### 연결 문제

1. 인바운드 규칙 확인
2. 소스 IP/보안 그룹 확인
3. 포트 번호 확인
4. 네트워크 ACL 확인

### 규칙 추적

```bash
# VPC Flow Logs 활성화로 트래픽 분석
aws ec2 create-flow-logs \
    --resource-type VPC \
    --resource-ids vpc-xxxxxxxxx \
    --traffic-type ALL \
    --log-destination-type cloud-watch-logs \
    --log-group-name my-flow-logs
```

## 참고 자료

- [Amazon EC2 보안 그룹](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html)
- [보안 그룹 규칙](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/security-group-rules.html)
- [VPC Flow Logs](https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html)
