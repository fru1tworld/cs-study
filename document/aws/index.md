# AWS 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://docs.aws.amazon.com/

## 상세 문서

각 서비스의 상세 문서는 아래 링크에서 확인할 수 있습니다.

| 서비스 | 설명 | 상세 문서 |
|--------|------|-----------|
| EC2 | 가상 서버 (컴퓨팅) | [EC2 문서](./EC2/index.md) |
| ECS | 컨테이너 오케스트레이션 | [ECS 문서](./ECS/index.md) |
| IAM | 자격 증명 및 액세스 관리 | [IAM 문서](./IAM/index.md) |
| ALB | 애플리케이션 로드 밸런서 | [ALB 문서](./ALB/index.md) |
| RDS | 관계형 데이터베이스 | [RDS 문서](./RDS/index.md) |

---

## EC2 (Elastic Compute Cloud)

### 개요

EC2는 AWS 클라우드에서 확장 가능한 컴퓨팅 용량을 제공합니다. 가상 서버(인스턴스)를 실행하여 애플리케이션을 배포할 수 있습니다.

### 인스턴스 유형

| 유형 | 용도 | 예시 |
|------|------|------|
| General Purpose | 균형 잡힌 컴퓨팅/메모리/네트워킹 | t3, m6i |
| Compute Optimized | 고성능 프로세서 필요 작업 | c6i, c7g |
| Memory Optimized | 대용량 메모리 필요 작업 | r6i, x2idn |
| Storage Optimized | 높은 순차적 읽기/쓰기 | i3, d2 |
| Accelerated Computing | GPU/FPGA 사용 작업 | p4, g5 |

### 구매 옵션

- **On-Demand**: 사용한 만큼 지불
- **Reserved**: 1-3년 약정, 최대 72% 할인
- **Spot**: 미사용 용량 활용, 최대 90% 할인
- **Savings Plans**: 유연한 약정 할인

### 참고 문서

- [EC2 User Guide](https://docs.aws.amazon.com/ec2/)

---

## ECS (Elastic Container Service)

### 개요

ECS는 컨테이너화된 애플리케이션을 쉽게 배포, 관리, 확장할 수 있는 완전 관리형 컨테이너 오케스트레이션 서비스입니다.

### 핵심 개념

```
Cluster → Service → Task → Container
```

- **Cluster**: ECS 리소스의 논리적 그룹
- **Service**: 지정된 수의 Task를 유지 관리
- **Task Definition**: 컨테이너 실행 방법 정의 (Docker Compose와 유사)
- **Task**: Task Definition의 인스턴스

### 시작 유형

| 유형 | 설명 |
|------|------|
| Fargate | 서버리스, 인프라 관리 불필요 |
| EC2 | EC2 인스턴스에서 컨테이너 실행 |

### Task Definition 예시

```json
{
  "family": "my-app",
  "networkMode": "awsvpc",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "my-app:latest",
      "portMappings": [
        {
          "containerPort": 3000,
          "protocol": "tcp"
        }
      ],
      "memory": 512,
      "cpu": 256
    }
  ],
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512"
}
```

### ECS Express Mode (2024/2025 신기능)

단일 명령으로 고가용성, 확장 가능한 컨테이너 애플리케이션을 배포합니다.

**특징:**
- 도메인, 네트워킹, 로드 밸런싱, 오토 스케일링 자동 설정
- 최대 25개 서비스가 하나의 ALB 공유 (비용 절감)
- 추가 비용 없음

### 참고 문서

- [ECS Developer Guide](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html)

---

## IAM (Identity and Access Management)

### 개요

IAM을 사용하여 AWS 리소스에 대한 액세스를 안전하게 제어합니다.

### 핵심 개념

- **User**: AWS와 상호작용하는 사람 또는 애플리케이션
- **Group**: User의 모음
- **Role**: 임시 자격 증명을 가진 AWS 아이덴티티
- **Policy**: 권한을 정의하는 JSON 문서

### Policy 구조

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### ECS용 IAM Roles

| Role | 용도 |
|------|------|
| Task Role | 컨테이너가 다른 AWS 서비스 접근 시 사용 |
| Task Execution Role | ECS 에이전트가 컨테이너 관리 시 사용 |
| Container Instance Role | EC2 인스턴스가 ECS 클러스터와 통신 시 사용 |

### Task Role Trust Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### 참고 문서

- [IAM User Guide](https://docs.aws.amazon.com/IAM/latest/UserGuide/)

---

## ALB (Application Load Balancer)

### 개요

ALB는 애플리케이션 레이어(Layer 7)에서 동작하는 로드 밸런서로, HTTP/HTTPS 트래픽을 분산합니다.

### 핵심 구성 요소

```
ALB → Listener → Rules → Target Group → Targets
```

- **Listener**: 지정된 포트와 프로토콜로 연결 요청 수신
- **Rules**: 요청을 어떤 Target Group으로 라우팅할지 결정
- **Target Group**: 요청을 처리할 대상들의 그룹
- **Health Check**: 대상의 상태 확인

### 라우팅 유형

- **Path-based**: `/api/*` → API 서버
- **Host-based**: `api.example.com` → API 서버
- **Query string**: `?platform=mobile` → 모바일 서버
- **HTTP header**: `X-Custom-Header` 기반 라우팅

### ECS + ALB 연동

```
ALB → Target Group (type: ip) → ECS Tasks (awsvpc mode)
```

**중요 사항:**
- `awsvpc` 네트워크 모드 사용 시 Target Type은 `ip`로 설정
- Dynamic Host Port Mapping 지원
- 서비스 연결을 위한 IAM 서비스 링크 역할 필요

### Terraform 예시

```hcl
resource "aws_lb" "main" {
  name               = "my-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "app" {
  name        = "my-target-group"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/health"
    healthy_threshold   = 2
    unhealthy_threshold = 10
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
```

### 참고 문서

- [ALB User Guide](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)
- [ECS with ALB](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/alb.html)

---

## 아키텍처 예시: ECS + ALB + IAM

```
┌─────────────────────────────────────────────────────────┐
│                        VPC                               │
│  ┌─────────────────────────────────────────────────┐    │
│  │              Public Subnets                      │    │
│  │  ┌─────────────────────────────────────────┐    │    │
│  │  │              ALB                         │    │    │
│  │  └─────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────┘    │
│                          │                               │
│  ┌─────────────────────────────────────────────────┐    │
│  │              Private Subnets                     │    │
│  │  ┌─────────────┐  ┌─────────────┐              │    │
│  │  │  ECS Task   │  │  ECS Task   │              │    │
│  │  │ (Fargate)   │  │ (Fargate)   │              │    │
│  │  │ Task Role   │  │ Task Role   │              │    │
│  │  └─────────────┘  └─────────────┘              │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```
