# ECS 보안

## 개요

Amazon ECS 보안은 **AWS 공동 책임 모델**을 따릅니다.

```
┌─────────────────────────────────────────────────────────────┐
│                      고객 책임                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  • 컨테이너 이미지 보안                               │  │
│  │  • 태스크/서비스 구성                                 │  │
│  │  • IAM 정책 및 역할                                   │  │
│  │  • 네트워크 구성 (VPC, 보안 그룹)                     │  │
│  │  • 데이터 암호화                                      │  │
│  │  • 로깅 및 모니터링                                   │  │
│  └───────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                       AWS 책임                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  • 호스트 인프라 보안                                 │  │
│  │  • ECS 서비스 보안                                    │  │
│  │  • 물리적 보안                                        │  │
│  │  • 네트워크 인프라                                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## IAM 역할

### IAM 역할 유형

```
┌─────────────────────────────────────────────────────────────┐
│                     ECS IAM 역할 구조                        │
│                                                              │
│  ┌──────────────────┐                                       │
│  │ Task Execution   │ ← ECS 에이전트가 사용                 │
│  │ Role             │   • ECR 이미지 풀                     │
│  │                  │   • CloudWatch 로그 전송              │
│  │                  │   • Secrets 조회                      │
│  └──────────────────┘                                       │
│                                                              │
│  ┌──────────────────┐                                       │
│  │ Task Role        │ ← 컨테이너 내 앱이 사용               │
│  │                  │   • AWS API 호출                      │
│  │                  │   • S3, DynamoDB 등 접근              │
│  └──────────────────┘                                       │
│                                                              │
│  ┌──────────────────┐                                       │
│  │ Container        │ ← EC2 인스턴스가 사용                 │
│  │ Instance Role    │   • ECS 서비스와 통신                 │
│  │                  │   • (EC2 Launch Type만)               │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

### Task Execution Role (태스크 실행 역할)

ECS 에이전트가 태스크 시작 시 사용하는 역할입니다.

**필요한 경우:**
- ECR에서 프라이빗 이미지 가져오기
- CloudWatch Logs에 로그 전송
- Secrets Manager/Parameter Store에서 시크릿 조회

**신뢰 정책:**

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

**권한 정책 (기본):**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
```

**Secrets Manager 접근 추가:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:region:account:secret:my-secret-*"
      ]
    }
  ]
}
```

### Task Role (태스크 역할)

컨테이너 내 애플리케이션이 AWS API 호출 시 사용하는 역할입니다.

**예시: S3 접근 권한**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ]
    }
  ]
}
```

### Container Instance Role (EC2만)

EC2 인스턴스가 ECS 서비스와 통신하는 데 사용합니다.

**AWS 관리형 정책:** `AmazonEC2ContainerServiceforEC2Role`

## 시크릿 관리

### 민감한 데이터 저장 옵션

| 서비스 | 특징 | 사용 사례 |
|--------|------|----------|
| **Secrets Manager** | 자동 로테이션, 교차 계정 공유 | DB 자격증명, API 키 |
| **Parameter Store** | 계층 구조, 버전 관리 | 구성 값, 환경 변수 |

### Secrets Manager 사용

**Task Definition 설정:**

```json
{
  "containerDefinitions": [
    {
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789:secret:db-password-xxxxx"
        }
      ]
    }
  ]
}
```

**특정 키 참조:**

```json
{
  "valueFrom": "arn:aws:secretsmanager:region:account:secret:my-secret:username::"
}
```

### Parameter Store 사용

```json
{
  "containerDefinitions": [
    {
      "secrets": [
        {
          "name": "API_KEY",
          "valueFrom": "arn:aws:ssm:ap-northeast-2:123456789:parameter/my-app/api-key"
        }
      ]
    }
  ]
}
```

### 시크릿 업데이트

시크릿 값이 변경되면:
- 새 태스크를 시작해야 최신 값 적용
- 또는 서비스 강제 재배포 필요

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --force-new-deployment
```

## 네트워크 보안

### 보안 그룹

태스크별 보안 그룹 적용 (awsvpc 모드):

```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "securityGroups": ["sg-12345678"]
    }
  }
}
```

**최소 권한 원칙:**

```
# 웹 서버 태스크
Inbound:
  - TCP 80 from ALB SG
  - TCP 443 from ALB SG

Outbound:
  - TCP 443 to 0.0.0.0/0 (HTTPS API)
  - TCP 3306 to DB SG
```

### 프라이빗 서브넷 사용

```
┌─────────────────────────────────────────────────────────────┐
│                           VPC                                │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Public Subnets                            │  │
│  │   ┌─────────────┐          ┌─────────────┐            │  │
│  │   │     ALB     │          │ NAT Gateway │            │  │
│  │   └─────────────┘          └─────────────┘            │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Private Subnets                           │  │
│  │   ┌─────────────┐          ┌─────────────┐            │  │
│  │   │   Task A    │          │   Task B    │            │  │
│  │   └─────────────┘          └─────────────┘            │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### VPC 엔드포인트

프라이빗 링크를 통한 AWS 서비스 접근:

| 엔드포인트 | 용도 |
|-----------|------|
| com.amazonaws.region.ecr.api | ECR API |
| com.amazonaws.region.ecr.dkr | ECR Docker |
| com.amazonaws.region.s3 | S3 (ECR 레이어) |
| com.amazonaws.region.logs | CloudWatch Logs |
| com.amazonaws.region.secretsmanager | Secrets Manager |

## 컨테이너 보안

### 읽기 전용 루트 파일시스템

```json
{
  "containerDefinitions": [
    {
      "readonlyRootFilesystem": true
    }
  ]
}
```

### 비특권 사용자

```json
{
  "containerDefinitions": [
    {
      "user": "1000:1000"
    }
  ]
}
```

### Linux Capabilities 제한

```json
{
  "containerDefinitions": [
    {
      "linuxParameters": {
        "capabilities": {
          "drop": ["ALL"],
          "add": ["NET_BIND_SERVICE"]
        }
      }
    }
  ]
}
```

### 권한 모드 비활성화

```json
{
  "containerDefinitions": [
    {
      "privileged": false
    }
  ]
}
```

**주의:** Fargate는 권한 모드를 지원하지 않습니다.

## 이미지 보안

### ECR 이미지 스캐닝

```bash
# 리포지토리 스캔 설정
aws ecr put-image-scanning-configuration \
    --repository-name my-repo \
    --image-scanning-configuration scanOnPush=true
```

### 이미지 서명 (Sigstore)

이미지 무결성 검증을 위한 서명 사용.

### 이미지 모범 사례

1. **최소 베이스 이미지 사용**: Alpine, Distroless
2. **취약점 스캔**: ECR 스캔 또는 서드파티 도구
3. **이미지 불변성**: 태그 대신 다이제스트 사용

```json
{
  "image": "123456789.dkr.ecr.region.amazonaws.com/my-app@sha256:abc123..."
}
```

## 런타임 보안

### ECS Exec

실행 중인 컨테이너에 접속하는 기능입니다.

**활성화:**

```bash
aws ecs create-service \
    --cluster my-cluster \
    --service-name my-service \
    --enable-execute-command \
    ...
```

**접근 제어:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:ExecuteCommand"
      ],
      "Resource": "arn:aws:ecs:region:account:task/cluster-name/*",
      "Condition": {
        "StringEquals": {
          "aws:ResourceTag/Environment": "development"
        }
      }
    }
  ]
}
```

**감사:**
- CloudTrail의 `ExecuteCommand` 이벤트로 모니터링
- CloudWatch Logs 또는 S3로 명령 로깅 가능

### GuardDuty 통합

Amazon GuardDuty가 ECS 런타임 위협을 탐지합니다:

- 악성 코드 실행
- 암호화폐 채굴
- 권한 상승 시도
- 의심스러운 네트워크 활동

## 데이터 보호

### 전송 중 암호화

- ALB/NLB TLS 종료
- Service Connect TLS
- EFS 전송 암호화

### 저장 데이터 암호화

**EFS:**

```json
{
  "efsVolumeConfiguration": {
    "transitEncryption": "ENABLED"
  }
}
```

**CloudWatch Logs:**

KMS 키로 로그 그룹 암호화:

```bash
aws logs associate-kms-key \
    --log-group-name /ecs/my-app \
    --kms-key-id arn:aws:kms:region:account:key/key-id
```

## 컴플라이언스

### 지원 컴플라이언스 프로그램

- SOC 1/2/3
- PCI DSS
- HIPAA
- ISO 27001
- FedRAMP

### FIPS 140-2

FIPS 엔드포인트 사용:

```
ecs-fips.region.amazonaws.com
```

## 모니터링 및 감사

### CloudTrail

ECS API 호출 기록:
- `CreateService`
- `UpdateService`
- `RunTask`
- `ExecuteCommand`

### CloudWatch 알람

비정상 활동 탐지:

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name ECS-TaskFailures \
    --metric-name TaskCount \
    --namespace AWS/ECS \
    --statistic Sum \
    --period 300 \
    --threshold 0 \
    --comparison-operator LessThanThreshold \
    --evaluation-periods 1
```

## 보안 체크리스트

### 필수

- [ ] Task Execution Role 최소 권한 설정
- [ ] Task Role 최소 권한 설정
- [ ] 시크릿은 Secrets Manager/Parameter Store 사용
- [ ] 프라이빗 서브넷 + VPC 엔드포인트 구성
- [ ] 보안 그룹 최소 권한 설정
- [ ] ECR 이미지 스캔 활성화

### 권장

- [ ] 읽기 전용 루트 파일시스템
- [ ] 비특권 사용자로 실행
- [ ] Linux Capabilities 제한
- [ ] Container Insights 활성화
- [ ] GuardDuty ECS 런타임 모니터링
- [ ] CloudTrail 로깅

## 참고 자료

- [ECS 보안 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/security.html)
- [ECS IAM 역할](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-iam-roles.html)
- [민감한 데이터 관리](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/specifying-sensitive-data.html)
