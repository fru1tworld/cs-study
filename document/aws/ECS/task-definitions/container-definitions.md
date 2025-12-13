# ECS Container Definition (컨테이너 정의)

## 개요

Container Definition은 Task Definition 내에서 개별 컨테이너의 설정을 정의합니다. 하나의 Task Definition에 여러 컨테이너를 포함할 수 있습니다.

## 기본 구조

```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "image": "nginx:latest",
      "essential": true,
      "cpu": 256,
      "memory": 512
    }
  ]
}
```

## 필수 파라미터

### name

컨테이너 이름입니다.

```json
{
  "name": "my-container"
}
```

** 규칙:**
- 최대 255자
- 문자, 숫자, 하이픈, 언더스코어 허용
- 태스크 내에서 고유해야 함

### image

Docker 이미지를 지정합니다.

```json
{
  "image": "nginx:latest"
}
```

** 형식:**
| 레지스트리 | 형식 |
|-----------|------|
| Docker Hub | `nginx`, `nginx:1.21`, `library/nginx:latest` |
| Amazon ECR | `123456789.dkr.ecr.region.amazonaws.com/repo:tag` |
| 프라이빗 레지스트리 | `registry.example.com/image:tag` |

** 프라이빗 레지스트리 인증:**

```json
{
  "image": "registry.example.com/app:latest",
  "repositoryCredentials": {
    "credentialsParameter": "arn:aws:secretsmanager:region:account:secret:docker-creds"
  }
}
```

## 리소스 설정

### CPU

```json
{
  "cpu": 256
}
```

- 단위: CPU 유닛 (1024 = 1 vCPU)
- Fargate: 태스크 레벨에서 지정한 값 내에서 분배
- EC2: 컨테이너별로 예약

### 메모리

```json
{
  "memory": 512,
  "memoryReservation": 256
}
```

| 파라미터 | 설명 |
|----------|------|
| **memory** | 하드 제한 - 초과 시 컨테이너 종료 |
| **memoryReservation** | 소프트 제한 - 예약량, 필요시 더 사용 가능 |

** 규칙:**
- Fargate: memory 필수
- EC2: memory 또는 memoryReservation 중 하나 필수
- memory > memoryReservation

## 포트 설정

### 기본 포트 매핑

```json
{
  "portMappings": [
    {
      "containerPort": 80,
      "hostPort": 80,
      "protocol": "tcp"
    }
  ]
}
```

### Service Connect용 포트

```json
{
  "portMappings": [
    {
      "containerPort": 8080,
      "protocol": "tcp",
      "name": "http-api",
      "appProtocol": "http"
    }
  ]
}
```

| 파라미터 | 설명 |
|----------|------|
| **name** | 포트 이름 (Service Connect에서 참조) |
| **appProtocol** | 프로토콜 타입 (http, http2, grpc) |

### 포트 범위 (동적 포트)

```json
{
  "portMappings": [
    {
      "containerPortRange": "8000-8100",
      "protocol": "tcp"
    }
  ]
}
```

## 환경 설정

### 환경 변수 (평문)

```json
{
  "environment": [
    {
      "name": "NODE_ENV",
      "value": "production"
    },
    {
      "name": "API_URL",
      "value": "https://api.example.com"
    }
  ]
}
```

### Secrets (암호화된 값)

```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:region:account:secret:my-secret"
    },
    {
      "name": "API_KEY",
      "valueFrom": "arn:aws:ssm:region:account:parameter/my-param"
    }
  ]
}
```

** 지원 소스:**
- AWS Secrets Manager
- AWS Systems Manager Parameter Store

** 특정 키 참조:**

```json
{
  "valueFrom": "arn:aws:secretsmanager:region:account:secret:my-secret:username::"
}
```

### 환경 파일 (S3)

```json
{
  "environmentFiles": [
    {
      "value": "arn:aws:s3:::bucket/config.env",
      "type": "s3"
    }
  ]
}
```

- 최대 10개 파일
- .env 형식 지원
- S3 버킷에 대한 접근 권한 필요

## 로깅 설정

### CloudWatch Logs (awslogs)

```json
{
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "/ecs/my-app",
      "awslogs-region": "ap-northeast-2",
      "awslogs-stream-prefix": "ecs",
      "awslogs-create-group": "true",
      "mode": "non-blocking",
      "max-buffer-size": "4m"
    }
  }
}
```

| 옵션 | 설명 |
|------|------|
| **awslogs-group** | CloudWatch 로그 그룹 |
| **awslogs-region** | 리전 |
| **awslogs-stream-prefix** | 로그 스트림 접두사 |
| **awslogs-create-group** | 로그 그룹 자동 생성 |
| **mode** | blocking 또는 non-blocking |
| **max-buffer-size** | 버퍼 크기 (non-blocking 모드) |

### FireLens

```json
{
  "logConfiguration": {
    "logDriver": "awsfirelens",
    "options": {
      "Name": "cloudwatch",
      "region": "ap-northeast-2",
      "log_group_name": "/ecs/my-app",
      "auto_create_group": "true",
      "log_stream_prefix": "ecs"
    }
  }
}
```

### 로깅 시크릿

```json
{
  "logConfiguration": {
    "logDriver": "splunk",
    "options": {
      "splunk-url": "https://splunk.example.com:8088"
    },
    "secretOptions": [
      {
        "name": "splunk-token",
        "valueFrom": "arn:aws:secretsmanager:region:account:secret:splunk-token"
      }
    ]
  }
}
```

## 헬스 체크

### 설정

```json
{
  "healthCheck": {
    "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
    "interval": 30,
    "timeout": 5,
    "retries": 3,
    "startPeriod": 60
  }
}
```

### 명령어 형식

| 형식 | 설명 | 예시 |
|------|------|------|
| CMD | 명령어 직접 실행 | `["CMD", "/bin/check"]` |
| CMD-SHELL | 쉘을 통해 실행 | `["CMD-SHELL", "curl -f http://localhost/"]` |

### 헬스 체크 상태

| 상태 | 설명 |
|------|------|
| **HEALTHY** | 헬스 체크 통과 |
| **UNHEALTHY** | 헬스 체크 실패 |
| **UNKNOWN** | 평가 중 또는 헬스 체크 없음 |

## 컨테이너 의존성

### 의존성 설정

```json
{
  "containerDefinitions": [
    {
      "name": "init",
      "essential": false
    },
    {
      "name": "app",
      "dependsOn": [
        {
          "containerName": "init",
          "condition": "COMPLETE"
        }
      ]
    }
  ]
}
```

### 의존성 조건

| 조건 | 설명 |
|------|------|
| **START** | 컨테이너가 시작됨 |
| **COMPLETE** | 컨테이너가 종료됨 (종료 코드 무관) |
| **SUCCESS** | 종료 코드 0으로 종료 |
| **HEALTHY** | 헬스 체크 통과 |

### 타임아웃 설정

```json
{
  "startTimeout": 120,
  "stopTimeout": 30
}
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| **startTimeout** | 의존성 해결 대기 시간 | 3분 |
| **stopTimeout** | 강제 종료 대기 시간 | 30초 |

## 컨테이너 유형

### Essential 컨테이너

```json
{
  "essential": true
}
```

- `true`: 이 컨테이너 실패 시 태스크 전체 중지
- `false`: 이 컨테이너 실패해도 태스크 계속 실행

### 사이드카 패턴

```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "essential": true,
      "image": "my-app:latest"
    },
    {
      "name": "log-router",
      "essential": false,
      "image": "fluent-bit:latest"
    },
    {
      "name": "envoy",
      "essential": true,
      "image": "envoy:latest"
    }
  ]
}
```

## 시작 명령어

### command (CMD)

```json
{
  "command": ["node", "server.js"]
}
```

### entryPoint (ENTRYPOINT)

```json
{
  "entryPoint": ["/docker-entrypoint.sh"]
}
```

### workingDirectory

```json
{
  "workingDirectory": "/app"
}
```

## 보안 설정

### 사용자 지정

```json
{
  "user": "1000:1000"
}
```

형식: `UID`, `UID:GID`, `username`, `username:groupname`

### Linux 보안 설정

```json
{
  "linuxParameters": {
    "capabilities": {
      "add": ["SYS_PTRACE"],
      "drop": ["ALL"]
    },
    "initProcessEnabled": true
  }
}
```

### 읽기 전용 루트 파일시스템

```json
{
  "readonlyRootFilesystem": true
}
```

### 권한 모드

```json
{
  "privileged": false
}
```

## 볼륨 마운트

```json
{
  "mountPoints": [
    {
      "sourceVolume": "data-volume",
      "containerPath": "/data",
      "readOnly": false
    }
  ]
}
```

## 리소스 제한 (ulimits)

```json
{
  "ulimits": [
    {
      "name": "nofile",
      "softLimit": 65535,
      "hardLimit": 65535
    }
  ]
}
```

** 지원 제한:**
- `nofile`: 파일 디스크립터 수
- `nproc`: 프로세스 수
- `core`: 코어 덤프 크기

## Docker 라벨

```json
{
  "dockerLabels": {
    "app": "my-app",
    "environment": "production"
  }
}
```

## 재시작 정책

```json
{
  "restartPolicy": {
    "enabled": true,
    "ignoredExitCodes": [0, 143],
    "restartAttemptPeriod": 300
  }
}
```

## 전체 예시

```json
{
  "containerDefinitions": [
    {
      "name": "web-app",
      "image": "123456789.dkr.ecr.ap-northeast-2.amazonaws.com/web-app:latest",
      "essential": true,
      "cpu": 256,
      "memory": 512,
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
          "awslogs-group": "/ecs/web-app",
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
      },
      "linuxParameters": {
        "initProcessEnabled": true
      },
      "readonlyRootFilesystem": false,
      "user": "1000:1000"
    }
  ]
}
```

## 참고 자료

- [Container Definition 파라미터](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters.html#container_definitions)
- [Docker 이미지 지정](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/specifying-images.html)
