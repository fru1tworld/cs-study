# AWS Fargate

## 개요

AWS Fargate는 **서버나 EC2 인스턴스 클러스터를 관리할 필요 없이 컨테이너를 실행**할 수 있는 서버리스 컴퓨팅 엔진입니다.

## Fargate vs EC2

```
┌──────────────────────────────────────────────────────────────┐
│                        EC2 Launch Type                        │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                 사용자 관리 영역                         │  │
│  │  • EC2 인스턴스 프로비저닝                              │  │
│  │  • 클러스터 스케일링                                    │  │
│  │  • OS 패치/업데이트                                     │  │
│  │  • ECS 에이전트 관리                                    │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                   AWS 관리 영역                         │  │
│  │  • 컨테이너 오케스트레이션                              │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                        Fargate                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                   AWS 관리 영역                         │  │
│  │  • 서버 프로비저닝                                      │  │
│  │  • 클러스터 스케일링                                    │  │
│  │  • OS 패치/업데이트                                     │  │
│  │  • 컨테이너 오케스트레이션                              │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                 사용자 관리 영역                         │  │
│  │  • 컨테이너 정의 (Task Definition)                      │  │
│  │  • CPU/메모리 요구사항                                  │  │
│  │  • 네트워킹/IAM 정책                                    │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

| 특성 | Fargate | EC2 |
|------|---------|-----|
| 인프라 관리 | AWS | 사용자 |
| 스케일링 | 자동 | 수동/ASG |
| 요금 모델 | 사용량 기반 | 인스턴스 기반 |
| 시작 속도 | 빠름 | 느림 |
| GPU 지원 | X | O |
| 커스터마이징 | 제한적 | 완전 |

## 작동 방식

1. **애플리케이션 컨테이너화**: Docker 이미지 생성
2. **Task Definition 작성**: CPU, 메모리, 네트워킹 정의
3. **서비스 실행**: Fargate가 자동으로 인프라 프로비저닝
4. **자동 관리**: AWS가 서버, 스케일링, 패치 담당

## 태스크 격리

```
┌─────────────────────────────────────────────────────┐
│                  Fargate Task                        │
│  ┌─────────────────────────────────────────────┐   │
│  │           격리된 실행 환경                    │   │
│  │  • 전용 커널                                 │   │
│  │  • 전용 CPU                                  │   │
│  │  • 전용 메모리                               │   │
│  │  • 전용 네트워크 인터페이스                  │   │
│  │                                              │   │
│  │  다른 태스크와 공유하지 않음                 │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

각 Fargate 태스크는 독립적인 격리 경계를 가지며, 다른 태스크와 커널, CPU, 메모리, 네트워크 인터페이스를 공유하지 않습니다.

## 지원 플랫폼

### 운영체제

| OS | 지원 |
|----|------|
| Amazon Linux 2 | O |
| Amazon Linux 2023 | O |
| Windows Server 2019 | O |
| Windows Server 2022 | O |
| Bottlerocket | O |

### CPU 아키텍처

| 아키텍처 | 지원 |
|----------|------|
| X86_64 (AMD64) | O |
| ARM64 (Graviton) | O |

## 용량 옵션

### 1. Fargate (On-Demand)

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "weight": 1
    }
  ]
}
```

**특징:**
- 표준 가격
- 안정적인 용량
- SLA 보장

### 2. Fargate Spot

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 1
    }
  ]
}
```

**특징:**
- 최대 70% 할인
- 여유 용량에서 실행
- 2분 경고 후 중단 가능

**적합한 워크로드:**
- 배치 처리
- 개발/테스트 환경
- 중단 허용 작업

### 혼합 사용

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "base": 1,
      "weight": 1
    },
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 4
    }
  ]
}
```

## CPU와 메모리

### 지원 조합

| CPU (vCPU) | 메모리 범위 |
|------------|-------------|
| 0.25 | 0.5GB - 2GB |
| 0.5 | 1GB - 4GB |
| 1 | 2GB - 8GB |
| 2 | 4GB - 16GB |
| 4 | 8GB - 30GB |
| 8 | 16GB - 60GB |
| 16 | 32GB - 120GB |

### Task Definition 설정

```json
{
  "family": "my-app",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [...]
}
```

## 네트워킹

### awsvpc 네트워크 모드

Fargate는 **awsvpc 네트워크 모드만 지원**합니다.

```
┌─────────────────────────────────────────────────┐
│                    VPC                           │
│  ┌─────────────────────────────────────────┐    │
│  │              Subnet                      │    │
│  │  ┌─────────────────────────────────┐    │    │
│  │  │         Fargate Task            │    │    │
│  │  │  ┌───────────────────────────┐  │    │    │
│  │  │  │     ENI (Elastic          │  │    │    │
│  │  │  │     Network Interface)    │  │    │    │
│  │  │  │     - Private IP          │  │    │    │
│  │  │  │     - Security Group      │  │    │    │
│  │  │  └───────────────────────────┘  │    │    │
│  │  └─────────────────────────────────┘    │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**특징:**
- 각 태스크에 전용 ENI 할당
- 보안 그룹 직접 연결
- VPC 내 다른 리소스와 직접 통신

### 네트워크 설정

```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-12345", "subnet-67890"],
      "securityGroups": ["sg-12345"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```

| 파라미터 | 설명 |
|----------|------|
| **subnets** | 태스크 배치 서브넷 |
| **securityGroups** | 보안 그룹 |
| **assignPublicIp** | 퍼블릭 IP 할당 여부 |

### 인터넷 접근

**퍼블릭 서브넷:**
```json
{
  "assignPublicIp": "ENABLED"
}
```

**프라이빗 서브넷:**
- NAT Gateway 필요
- 또는 VPC 엔드포인트 사용

## 스토리지

### 임시 스토리지 (Ephemeral Storage)

```json
{
  "ephemeralStorage": {
    "sizeInGiB": 100
  }
}
```

| 항목 | 값 |
|------|-----|
| 기본 크기 | 20 GiB |
| 최대 크기 | 200 GiB |

### EFS 볼륨

```json
{
  "volumes": [
    {
      "name": "efs-volume",
      "efsVolumeConfiguration": {
        "fileSystemId": "fs-12345678",
        "transitEncryption": "ENABLED"
      }
    }
  ]
}
```

### EBS 볼륨 (Linux만)

```json
{
  "volumes": [
    {
      "name": "ebs-volume",
      "configuredAtLaunch": true
    }
  ]
}
```

## 플랫폼 버전

### Linux 플랫폼 버전

| 버전 | 특징 |
|------|------|
| 1.4.0 (LATEST) | 현재 최신 버전 |
| 1.3.0 | 레거시 |
| 1.2.0 | 레거시 |

### Windows 플랫폼 버전

| 버전 | 특징 |
|------|------|
| 1.0.0 (LATEST) | 현재 최신 버전 |

### 플랫폼 버전 지정

```json
{
  "platformVersion": "1.4.0"
}
```

또는

```json
{
  "platformVersion": "LATEST"
}
```

## Task Definition 예시

### Fargate Linux

```json
{
  "family": "fargate-app",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "512",
  "memory": "1024",
  "runtimePlatform": {
    "operatingSystemFamily": "LINUX",
    "cpuArchitecture": "X86_64"
  },
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "nginx:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 80,
          "protocol": "tcp"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/fargate-app",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Fargate Windows

```json
{
  "family": "fargate-windows-app",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "1024",
  "memory": "2048",
  "runtimePlatform": {
    "operatingSystemFamily": "WINDOWS_SERVER_2022_FULL",
    "cpuArchitecture": "X86_64"
  },
  "containerDefinitions": [...]
}
```

## 로드 밸런서 통합

### ALB/NLB 설정

```json
{
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:...",
      "containerName": "app",
      "containerPort": 80
    }
  ]
}
```

**중요**: 대상 그룹의 대상 유형을 `ip`로 설정해야 합니다.

```bash
aws elbv2 create-target-group \
    --name my-targets \
    --protocol HTTP \
    --port 80 \
    --vpc-id vpc-12345 \
    --target-type ip
```

## 제한 사항

| 항목 | Fargate 제한 |
|------|-------------|
| GPU | 미지원 |
| 권한 모드 (privileged) | 미지원 |
| Docker 볼륨 | 미지원 |
| 호스트 네트워크 | 미지원 |
| Windows gMSA | 일부 지원 |

## 요금

### 과금 기준

- **vCPU**: 초당 과금
- **메모리**: 초당 과금
- **스토리지**: 기본 20GB 무료, 초과분 과금

### 요금 계산 예시

```
태스크: 0.5 vCPU, 1GB 메모리
실행 시간: 1시간

비용 = (vCPU 단가 × 0.5 × 3600초) + (메모리 단가 × 1 × 3600초)
```

### Fargate Spot 할인

- On-Demand 대비 최대 70% 할인
- 용량에 따라 가격 변동

## 모범 사례

### 1. 비용 최적화

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "base": 1,
      "weight": 1
    },
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 3
    }
  ]
}
```

### 2. 적절한 리소스 할당

- CloudWatch 메트릭으로 실제 사용량 모니터링
- 필요한 만큼만 할당

### 3. ARM64 (Graviton) 사용

- x86_64 대비 최대 40% 가격 효율
- 호환되는 이미지 필요

```json
{
  "runtimePlatform": {
    "cpuArchitecture": "ARM64"
  }
}
```

### 4. 프라이빗 서브넷 사용

- NAT Gateway 또는 VPC 엔드포인트 구성
- 보안 강화

## 참고 자료

- [AWS Fargate 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Fargate 요금](https://aws.amazon.com/fargate/pricing/)
