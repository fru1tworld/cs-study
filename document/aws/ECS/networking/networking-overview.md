# ECS 네트워킹

## 개요

Amazon ECS는 태스크의 네트워크 연결을 위한 여러 네트워크 모드를 제공합니다. 네트워크 모드에 따라 컨테이너가 네트워크에 연결되는 방식이 달라집니다.

## 네트워크 모드

### 네트워크 모드 비교

| 모드 | 설명 | 플랫폼 | Fargate |
|------|------|--------|---------|
| **awsvpc** | 태스크별 ENI 할당 | EC2/Fargate | O (필수) |
| **bridge** | Docker 가상 네트워크 | EC2 (Linux) | X |
| **host** | 호스트 네트워크 사용 | EC2 (Linux) | X |
| **none** | 네트워크 없음 | EC2 (Linux) | X |
| **default** | Windows NAT | EC2 (Windows) | X |

## awsvpc 네트워크 모드 (권장)

### 개요

각 태스크에 **전용 Elastic Network Interface (ENI)**를 할당하여 EC2 인스턴스와 동일한 네트워킹 속성을 제공합니다.

### 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                           VPC                                │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                       Subnet                           │  │
│  │                                                        │  │
│  │  ┌──────────────────┐    ┌──────────────────┐         │  │
│  │  │    Task A        │    │    Task B        │         │  │
│  │  │  ┌────────────┐  │    │  ┌────────────┐  │         │  │
│  │  │  │    ENI     │  │    │  │    ENI     │  │         │  │
│  │  │  │ 10.0.1.10  │  │    │  │ 10.0.1.11  │  │         │  │
│  │  │  │ SG: sg-xxx │  │    │  │ SG: sg-yyy │  │         │  │
│  │  │  └────────────┘  │    │  └────────────┘  │         │  │
│  │  └──────────────────┘    └──────────────────┘         │  │
│  │                                                        │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 특징

- **전용 IP**: 각 태스크에 프라이빗 IP 할당
- **보안 그룹**: 태스크 레벨에서 보안 그룹 적용
- **VPC 기능**: VPC Flow Logs, 라우팅 등 모든 VPC 기능 사용 가능
- **서비스 디스커버리**: DNS 기반 서비스 검색 지원

### 설정

```json
{
  "networkMode": "awsvpc",
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-12345678", "subnet-87654321"],
      "securityGroups": ["sg-12345678"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```

### ENI 제한

EC2 인스턴스 유형별 ENI 제한이 있습니다:

| 인스턴스 유형 | 최대 ENI | 태스크당 ENI |
|--------------|---------|-------------|
| t3.micro | 2 | 1 |
| t3.small | 3 | 1 |
| t3.medium | 3 | 1 |
| m5.large | 3 | 1 |
| m5.xlarge | 4 | 1 |

**ENI Trunking** 활성화 시 더 많은 태스크 실행 가능

## bridge 네트워크 모드

### 개요

Docker의 내장 가상 네트워크를 사용합니다. **Linux EC2의 기본 모드**입니다.

### 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      EC2 Instance                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   docker0 bridge                       │  │
│  │                    172.17.0.1                          │  │
│  │         ┌──────────────┴──────────────┐               │  │
│  │         │                              │               │  │
│  │   ┌─────┴─────┐                  ┌────┴────┐          │  │
│  │   │ Container │                  │Container│          │  │
│  │   │ 172.17.0.2│                  │172.17.0.3│         │  │
│  │   │ Port: 80  │                  │Port: 80 │          │  │
│  │   └───────────┘                  └─────────┘          │  │
│  │         │                              │               │  │
│  │    Host Port: 8080              Host Port: 8081        │  │
│  └───────────────────────────────────────────────────────┘  │
│                         │                                    │
│              Host ENI (10.0.1.5:8080, 8081)                  │
└─────────────────────────────────────────────────────────────┘
```

### 특징

- **동적 포트 매핑**: 호스트 포트 자동 할당 가능
- **포트 충돌 방지**: 같은 컨테이너 포트도 다른 호스트 포트로 매핑
- **컨테이너 간 통신**: 같은 브릿지 네트워크 내 통신 가능

### 설정

```json
{
  "networkMode": "bridge",
  "containerDefinitions": [
    {
      "name": "web",
      "portMappings": [
        {
          "containerPort": 80,
          "hostPort": 0,
          "protocol": "tcp"
        }
      ]
    }
  ]
}
```

`hostPort: 0`은 동적 포트 할당을 의미합니다.

## host 네트워크 모드

### 개요

컨테이너가 **호스트의 네트워크 스택을 직접 사용**합니다.

### 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    EC2 Instance                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Host Network Stack                    │  │
│  │                  10.0.1.5                          │  │
│  │                     │                              │  │
│  │   ┌─────────────────┴─────────────────┐           │  │
│  │   │           Container               │           │  │
│  │   │        Port 80 → 10.0.1.5:80      │           │  │
│  │   └───────────────────────────────────┘           │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 특징

- **네트워크 오버헤드 없음**: 최고 성능
- **동적 포트 매핑 불가**: 같은 포트 사용 컨테이너 중복 실행 불가
- **호스트 포트 직접 사용**

### 설정

```json
{
  "networkMode": "host",
  "containerDefinitions": [
    {
      "name": "web",
      "portMappings": [
        {
          "containerPort": 80,
          "protocol": "tcp"
        }
      ]
    }
  ]
}
```

## none 네트워크 모드

외부 네트워크 연결이 없습니다. 네트워크가 필요 없는 배치 작업에 사용합니다.

```json
{
  "networkMode": "none"
}
```

## default 네트워크 모드 (Windows)

Windows EC2 인스턴스의 기본 모드로, Docker NAT 드라이버를 사용합니다.

```json
{
  "networkMode": "default"
}
```

## 보안 그룹

### awsvpc 모드에서 보안 그룹

태스크 레벨에서 보안 그룹을 직접 적용합니다.

```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "securityGroups": ["sg-12345678", "sg-87654321"]
    }
  }
}
```

### 보안 그룹 규칙 예시

**웹 서버 태스크:**

```
Inbound:
  - Port 80 from ALB Security Group
  - Port 443 from ALB Security Group

Outbound:
  - Port 443 to 0.0.0.0/0 (HTTPS)
  - Port 3306 to DB Security Group (MySQL)
```

## 서브넷 설정

### 퍼블릭 서브넷

```json
{
  "awsvpcConfiguration": {
    "subnets": ["subnet-public-1", "subnet-public-2"],
    "assignPublicIp": "ENABLED"
  }
}
```

- 인터넷 직접 접근 가능
- Internet Gateway 필요

### 프라이빗 서브넷

```json
{
  "awsvpcConfiguration": {
    "subnets": ["subnet-private-1", "subnet-private-2"],
    "assignPublicIp": "DISABLED"
  }
}
```

- 인터넷 접근: NAT Gateway 필요
- AWS 서비스 접근: VPC 엔드포인트 권장

## VPC 엔드포인트

프라이빗 서브넷에서 AWS 서비스에 접근하기 위한 엔드포인트입니다.

### 필수 엔드포인트

| 서비스 | 엔드포인트 | 용도 |
|--------|-----------|------|
| ECR API | com.amazonaws.region.ecr.api | ECR 인증 |
| ECR DKR | com.amazonaws.region.ecr.dkr | 이미지 풀 |
| S3 | com.amazonaws.region.s3 | ECR 이미지 레이어 |
| CloudWatch Logs | com.amazonaws.region.logs | 로그 전송 |
| Secrets Manager | com.amazonaws.region.secretsmanager | 시크릿 조회 |

### 엔드포인트 생성

```bash
aws ec2 create-vpc-endpoint \
    --vpc-id vpc-12345678 \
    --service-name com.amazonaws.ap-northeast-2.ecr.api \
    --vpc-endpoint-type Interface \
    --subnet-ids subnet-12345678 subnet-87654321 \
    --security-group-ids sg-12345678
```

## 로드 밸런서 통합

### Application Load Balancer

```
┌─────────────────────────────────────────────────────────────┐
│                           VPC                                │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                 Public Subnets                        │   │
│  │   ┌──────────────────────────────────────────────┐   │   │
│  │   │        Application Load Balancer             │   │   │
│  │   │              (Internet-facing)               │   │   │
│  │   └────────────────────┬─────────────────────────┘   │   │
│  └────────────────────────┼─────────────────────────────┘   │
│                           │                                  │
│  ┌────────────────────────┼─────────────────────────────┐   │
│  │                Private Subnets                        │   │
│  │           ┌────────────┴────────────┐                │   │
│  │           │                          │                │   │
│  │   ┌───────▼──────┐          ┌───────▼──────┐         │   │
│  │   │   Task 1     │          │   Task 2     │         │   │
│  │   │  10.0.1.10   │          │  10.0.2.20   │         │   │
│  │   └──────────────┘          └──────────────┘         │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 대상 그룹 설정

**awsvpc 모드:** 대상 유형을 `ip`로 설정

```bash
aws elbv2 create-target-group \
    --name my-targets \
    --protocol HTTP \
    --port 80 \
    --vpc-id vpc-12345 \
    --target-type ip
```

**bridge 모드:** 대상 유형을 `instance`로 설정 (동적 포트 매핑)

## IPv6 지원

### IPv6 전용 구성

```json
{
  "awsvpcConfiguration": {
    "subnets": ["subnet-ipv6-only"]
  }
}
```

**요구사항:**
- VPC에 IPv6 CIDR 블록 추가
- IPv6 전용 서브넷 생성
- 라우팅 테이블 및 보안 그룹 IPv6 규칙 구성
- Linux AMI 필수 (Windows 미지원)
- 컨테이너 에이전트 v1.99.1 이상

**제한사항:**
- ECS Exec 미지원
- ECR 듀얼스택 엔드포인트 필요

## 네트워크 트러블슈팅

### 태스크가 시작되지 않을 때

1. **서브넷 확인**: 서브넷에 가용 IP 주소가 있는지 확인
2. **보안 그룹 확인**: 필요한 포트가 열려 있는지 확인
3. **ENI 제한 확인**: 인스턴스 ENI 제한 확인
4. **라우팅 테이블 확인**: 인터넷/서비스 접근 경로 확인

### ECR 이미지 풀 실패

1. VPC 엔드포인트 또는 NAT Gateway 확인
2. 보안 그룹 아웃바운드 규칙 확인
3. Task Execution Role 권한 확인

### 서비스 간 통신 실패

1. 보안 그룹 인바운드 규칙 확인
2. 서브넷 라우팅 확인
3. DNS 해상 확인

## 참고 자료

- [ECS 네트워킹 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-networking.html)
- [VPC 엔드포인트](https://docs.aws.amazon.com/vpc/latest/privatelink/)
