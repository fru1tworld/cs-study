# ECS Task Definition (태스크 정의)

## 개요

Task Definition은 ** 애플리케이션의 청사진** 입니다. JSON 형식의 텍스트 파일로, 컨테이너 실행에 필요한 모든 파라미터를 정의합니다.

## Task Definition 구조

```json
{
  "family": "my-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "nginx:latest",
      "portMappings": [
        {
          "containerPort": 80,
          "protocol": "tcp"
        }
      ],
      "essential": true
    }
  ]
}
```

## 핵심 파라미터

### 태스크 레벨 파라미터

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| **family** | O | 태스크 정의 이름 (버전 관리에 사용) |
| **networkMode** | O | 네트워크 모드 (Fargate는 awsvpc 필수) |
| **requiresCompatibilities** | - | 호환 플랫폼 (FARGATE, EC2) |
| **cpu** | 조건부 | 태스크 CPU 단위 |
| **memory** | 조건부 | 태스크 메모리 (MiB) |
| **executionRoleArn** | 조건부 | 태스크 실행 역할 |
| **taskRoleArn** | - | 태스크 역할 (AWS API 호출용) |

### CPU와 메모리 조합 (Fargate)

| CPU (vCPU) | 메모리 범위 |
|------------|-------------|
| 256 (.25) | 512 MiB - 2 GB |
| 512 (.5) | 1 GB - 4 GB |
| 1024 (1) | 2 GB - 8 GB |
| 2048 (2) | 4 GB - 16 GB |
| 4096 (4) | 8 GB - 30 GB |
| 8192 (8) | 16 GB - 60 GB |
| 16384 (16) | 32 GB - 120 GB |

### 런타임 플랫폼

```json
{
  "runtimePlatform": {
    "operatingSystemFamily": "LINUX",
    "cpuArchitecture": "X86_64"
  }
}
```

| 파라미터 | 옵션 |
|----------|------|
| **operatingSystemFamily** | LINUX, WINDOWS_SERVER_2019_FULL, WINDOWS_SERVER_2022_FULL 등 |
| **cpuArchitecture** | X86_64, ARM64 |

## 컨테이너 정의

### 필수 파라미터

```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "image": "nginx:latest",
      "essential": true
    }
  ]
}
```

| 파라미터 | 설명 |
|----------|------|
| **name** | 컨테이너 이름 (최대 255자) |
| **image** | Docker 이미지 (레지스트리/이미지:태그) |
| **essential** | true면 실패 시 태스크 전체 중지 |

### 리소스 할당

```json
{
  "cpu": 256,
  "memory": 512,
  "memoryReservation": 256
}
```

| 파라미터 | 설명 |
|----------|------|
| **cpu** | CPU 유닛 (1024 = 1 vCPU) |
| **memory** | 하드 제한 (MiB) |
| **memoryReservation** | 소프트 제한 (MiB) |

### 포트 매핑

```json
{
  "portMappings": [
    {
      "containerPort": 80,
      "hostPort": 80,
      "protocol": "tcp",
      "name": "http",
      "appProtocol": "http"
    }
  ]
}
```

| 파라미터 | 설명 |
|----------|------|
| **containerPort** | 컨테이너 포트 (필수) |
| **hostPort** | 호스트 포트 (awsvpc에서는 containerPort와 동일) |
| **protocol** | tcp 또는 udp |
| **name** | 포트 이름 (Service Connect용) |
| **appProtocol** | 애플리케이션 프로토콜 (http, http2, grpc) |

### 환경 변수

```json
{
  "environment": [
    {
      "name": "ENV",
      "value": "production"
    }
  ],
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:region:account:secret:my-secret"
    }
  ],
  "environmentFiles": [
    {
      "value": "arn:aws:s3:::bucket/env-file.env",
      "type": "s3"
    }
  ]
}
```

| 파라미터 | 설명 |
|----------|------|
| **environment** | 평문 환경 변수 |
| **secrets** | Secrets Manager 또는 Parameter Store에서 주입 |
| **environmentFiles** | S3의 .env 파일 (최대 10개) |

### 로깅 설정

```json
{
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "/ecs/my-app",
      "awslogs-region": "ap-northeast-2",
      "awslogs-stream-prefix": "ecs"
    }
  }
}
```

** 지원 로그 드라이버:**
- `awslogs`: CloudWatch Logs
- `splunk`: Splunk
- `awsfirelens`: FireLens (Fluentd/Fluent Bit)
- `json-file`: 로컬 JSON
- `syslog`: Syslog

### 헬스 체크

```json
{
  "healthCheck": {
    "command": ["CMD-SHELL", "curl -f http://localhost/ || exit 1"],
    "interval": 30,
    "timeout": 5,
    "retries": 3,
    "startPeriod": 60
  }
}
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| **command** | 헬스 체크 명령어 | - |
| **interval** | 체크 간격 (초) | 30 |
| **timeout** | 타임아웃 (초) | 5 |
| **retries** | 재시도 횟수 | 3 |
| **startPeriod** | 시작 대기 시간 (초) | 0 |

### 컨테이너 의존성

```json
{
  "dependsOn": [
    {
      "containerName": "init-container",
      "condition": "COMPLETE"
    },
    {
      "containerName": "sidecar",
      "condition": "HEALTHY"
    }
  ]
}
```

| 조건 | 설명 |
|------|------|
| **START** | 컨테이너 시작됨 |
| **COMPLETE** | 컨테이너 정상 종료 |
| **SUCCESS** | 종료 코드 0으로 종료 |
| **HEALTHY** | 헬스 체크 통과 |

## 볼륨 설정

### EFS 볼륨

```json
{
  "volumes": [
    {
      "name": "efs-volume",
      "efsVolumeConfiguration": {
        "fileSystemId": "fs-12345678",
        "rootDirectory": "/",
        "transitEncryption": "ENABLED",
        "authorizationConfig": {
          "accessPointId": "fsap-12345678",
          "iam": "ENABLED"
        }
      }
    }
  ],
  "containerDefinitions": [
    {
      "mountPoints": [
        {
          "sourceVolume": "efs-volume",
          "containerPath": "/data",
          "readOnly": false
        }
      ]
    }
  ]
}
```

### 임시 스토리지 (Fargate)

```json
{
  "ephemeralStorage": {
    "sizeInGiB": 100
  }
}
```

- 기본: 20 GiB
- 최대: 200 GiB

## IAM 역할

### Task Execution Role (태스크 실행 역할)

**ECS 에이전트가 사용하는 역할**

필요한 경우:
- ECR에서 이미지 가져오기
- CloudWatch에 로그 전송
- Secrets Manager/Parameter Store에서 시크릿 조회

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

### Task Role (태스크 역할)

** 컨테이너 내 애플리케이션이 사용하는 역할**

예시: S3 접근 권한

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

## 재시작 정책

```json
{
  "containerDefinitions": [
    {
      "restartPolicy": {
        "enabled": true,
        "ignoredExitCodes": [0, 143],
        "restartAttemptPeriod": 300
      }
    }
  ]
}
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| **enabled** | 재시작 활성화 | false |
| **ignoredExitCodes** | 재시작하지 않을 종료 코드 | - |
| **restartAttemptPeriod** | 재시작 시도 간격 (초) | 300 |

## 전체 예시

```json
{
  "family": "my-web-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789:role/ecsTaskRole",
  "runtimePlatform": {
    "operatingSystemFamily": "LINUX",
    "cpuArchitecture": "X86_64"
  },
  "containerDefinitions": [
    {
      "name": "web",
      "image": "123456789.dkr.ecr.ap-northeast-2.amazonaws.com/my-app:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp",
          "name": "http"
        }
      ],
      "environment": [
        {
          "name": "NODE_ENV",
          "value": "production"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789:secret:db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/my-web-app",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

## Task Definition 등록

```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

## 버전 관리

- 각 등록 시 새 리비전 생성 (예: my-app:1, my-app:2)
- 이전 리비전 참조 가능
- 서비스 업데이트 시 새 리비전 지정

```bash
# 리비전 조회
aws ecs describe-task-definition --task-definition my-app:1

# 최신 리비전 사용 (ACTIVE)
aws ecs describe-task-definition --task-definition my-app
```

## 참고 자료

- [Task Definition 파라미터](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters.html)
- [Task Definition 예시](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/example_task_definitions.html)
