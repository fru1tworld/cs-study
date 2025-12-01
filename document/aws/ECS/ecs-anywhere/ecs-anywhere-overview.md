# ECS Anywhere

## 개요

ECS Anywhere는 ** 온프레미스 서버나 VM을 Amazon ECS 클러스터에 등록** 할 수 있는 기능입니다. 이를 통해 클라우드와 온프레미스 환경을 통합 관리할 수 있습니다.

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                       AWS Cloud                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    ECS Cluster                         │  │
│  │  ┌───────────────┐  ┌───────────────┐                 │  │
│  │  │ Fargate Task  │  │   EC2 Task    │                 │  │
│  │  └───────────────┘  └───────────────┘                 │  │
│  │                                                        │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │              External Instances                  │  │  │
│  │  │  (ECS Anywhere)                                  │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Internet/Direct Connect
                              │
┌─────────────────────────────────────────────────────────────┐
│                    On-Premises Data Center                   │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │  Server A     │  │  Server B     │  │    VM C       │   │
│  │  (External    │  │  (External    │  │  (External    │   │
│  │   Instance)   │  │   Instance)   │  │   Instance)   │   │
│  └───────────────┘  └───────────────┘  └───────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 지원 환경

### 운영체제

**Linux:**
| OS | 지원 버전 |
|----|---------|
| Amazon Linux | 2, 2023 |
| CentOS | Stream 9 |
| RHEL | 7, 8, 9 |
| Fedora | 최신 |
| Ubuntu | 18.04+ |
| Debian | 10+ |
| SUSE | Enterprise |

**Windows:**
| OS | 지원 버전 |
|----|---------|
| Windows Server | 2016, 2019, 2022, 20H2 |

### CPU 아키텍처

- x86_64 (Intel/AMD)
- ARM64

## 사용 사례

### 1. 데이터 로컬리티

데이터가 온프레미스에 있어야 하는 경우:
- 규정 준수 요구사항
- 대용량 데이터 처리
- 저지연 요구사항

### 2. 하이브리드 워크로드

클라우드와 온프레미스를 함께 사용:
- 마이그레이션 과도기
- 버스트 워크로드
- 재해 복구

### 3. 엣지 컴퓨팅

로컬 데이터 처리가 필요한 경우:
- IoT 데이터 처리
- 실시간 분석
- 콘텐츠 캐싱

## 설정 방법

### 1. IAM 역할 생성

** 외부 인스턴스용 IAM 역할:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ssm.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

** 필요한 정책:**
- `AmazonSSMManagedInstanceCore`
- `AmazonEC2ContainerServiceforEC2Role`

### 2. 활성화 키 생성

```bash
aws ssm create-activation \
    --iam-role arn:aws:iam::account:role/ecsAnywhereRole \
    --registration-limit 10 \
    --default-instance-name "my-external-instance"
```

응답:
```json
{
  "ActivationId": "activation-id",
  "ActivationCode": "activation-code"
}
```

### 3. 외부 인스턴스 등록 (Linux)

```bash
# ECS 에이전트 설치 스크립트 실행
curl -o /tmp/ecs-anywhere-install.sh \
    "https://amazon-ecs-agent-packages-prod.s3.amazonaws.com/ecs-anywhere-install-latest.sh"

sudo bash /tmp/ecs-anywhere-install.sh \
    --region ap-northeast-2 \
    --cluster my-cluster \
    --activation-id activation-id \
    --activation-code activation-code
```

### 4. 외부 인스턴스 등록 (Windows)

```powershell
# PowerShell에서 실행
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Invoke-WebRequest -Uri "https://amazon-ecs-agent-packages-prod.s3.amazonaws.com/ecs-anywhere-install-latest.ps1" `
    -OutFile "ecs-anywhere-install.ps1"

.\ecs-anywhere-install.ps1 `
    -Region ap-northeast-2 `
    -Cluster my-cluster `
    -ActivationId activation-id `
    -ActivationCode activation-code
```

### 5. 등록 확인

```bash
aws ecs list-container-instances \
    --cluster my-cluster \
    --filter "attribute:ecs.os-type == external"
```

## 태스크 실행

### EXTERNAL Launch Type

```bash
aws ecs run-task \
    --cluster my-cluster \
    --task-definition my-task:1 \
    --launch-type EXTERNAL
```

### 서비스 생성

```bash
aws ecs create-service \
    --cluster my-cluster \
    --service-name my-external-service \
    --task-definition my-task:1 \
    --launch-type EXTERNAL \
    --desired-count 2
```

### Task Definition 예시

```json
{
  "family": "external-task",
  "requiresCompatibilities": ["EXTERNAL"],
  "networkMode": "bridge",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "nginx:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 80,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/external",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

## 네트워킹

### 지원 네트워크 모드

**Linux:**
| 모드 | 설명 |
|------|------|
| bridge | Docker 가상 네트워크 (기본) |
| host | 호스트 네트워크 직접 사용 |
| none | 네트워크 없음 |

**Windows:**
| 모드 | 설명 |
|------|------|
| default | Docker NAT |

### awsvpc 미지원

ECS Anywhere는 awsvpc 네트워크 모드를 ** 지원하지 않습니다**.

### 필수 AWS 엔드포인트

외부 인스턴스가 접근해야 하는 엔드포인트:

| 엔드포인트 | 용도 |
|-----------|------|
| ecs-a-*.region.amazonaws.com | ECS 태스크 관리 |
| ssm.region.amazonaws.com | Systems Manager |
| ssmmessages.region.amazonaws.com | SSM 메시지 |
| ec2messages.region.amazonaws.com | EC2 메시지 |
| ecr.region.amazonaws.com | 컨테이너 이미지 |

## 인증

### SSM 에이전트 자격증명

SSM Agent가 하드웨어 지문을 사용하여 **30분마다 IAM 자격증명을 자동 갱신** 합니다.

### 태스크 역할 지원

외부 인스턴스에서도 태스크 역할을 사용할 수 있습니다:

```json
{
  "taskRoleArn": "arn:aws:iam::account:role/my-task-role"
}
```

## Active Directory 통합

### 클라우드 기반 AD

```
┌─────────────────┐     Direct Connect     ┌─────────────────┐
│   On-Premises   │◄─────────────────────►│      AWS        │
│                 │                        │                 │
│  ┌───────────┐  │                        │  ┌───────────┐  │
│  │ External  │  │                        │  │ AWS       │  │
│  │ Instance  │  │                        │  │ Directory │  │
│  └───────────┘  │                        │  │ Service   │  │
│                 │                        │  └───────────┘  │
└─────────────────┘                        └─────────────────┘
```

### 온프레미스 AD

기존 온프레미스 Active Directory에 외부 인스턴스를 조인합니다.

## 제한 사항

| 기능 | 지원 여부 |
|------|----------|
| 로드 밸런서 (ELB) | X |
| Service Discovery | X |
| awsvpc 네트워크 모드 | X |
| Capacity Provider | X |
| EFS 볼륨 | X |
| App Mesh | X |
| SELinux | X |
| Fargate | X (EXTERNAL만) |

## 비용

### ECS Anywhere 요금

** 온프레미스 인스턴스:**
- 등록된 인스턴스당 시간당 요금

**AWS 리전 인스턴스와 동일:**
- 데이터 전송
- CloudWatch 로그
- ECR 스토리지

## 모니터링

### CloudWatch 메트릭

외부 인스턴스도 CloudWatch 메트릭을 전송합니다:

```bash
aws cloudwatch get-metric-statistics \
    --namespace AWS/ECS \
    --metric-name CPUUtilization \
    --dimensions Name=ClusterName,Value=my-cluster \
    --start-time ... \
    --end-time ... \
    --period 60 \
    --statistics Average
```

### Container Insights

외부 인스턴스에서도 Container Insights를 사용할 수 있습니다.

## 트러블슈팅

### 인스턴스 등록 실패

1. 네트워크 연결 확인 (AWS 엔드포인트 접근)
2. 활성화 키 유효성 확인
3. IAM 역할 권한 확인
4. 방화벽 규칙 확인

### 태스크 시작 실패

1. Docker 서비스 상태 확인
2. 이미지 풀 가능 여부 확인
3. 리소스 가용성 확인
4. ECS 에이전트 로그 확인

### 에이전트 로그 확인

**Linux:**
```bash
sudo cat /var/log/ecs/ecs-agent.log
```

**Windows:**
```powershell
Get-Content C:\ProgramData\Amazon\ECS\log\ecs-agent.log
```

## 모범 사례

### 1. 네트워크 안정성

- 안정적인 인터넷/Direct Connect 연결
- 필요한 엔드포인트에 대한 방화벽 허용

### 2. 보안

- 최소 권한 IAM 역할
- 정기적인 OS 패치
- 네트워크 세그멘테이션

### 3. 모니터링

- CloudWatch 알람 설정
- SSM 인벤토리 활성화
- 정기적인 상태 점검

### 4. 고가용성

- 여러 외부 인스턴스 사용
- 서비스 desiredCount 적절히 설정

## 참고 자료

- [ECS Anywhere 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-anywhere.html)
- [외부 인스턴스 등록](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-anywhere-registration.html)
