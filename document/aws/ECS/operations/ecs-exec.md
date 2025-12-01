# ECS Exec

## 개요

ECS Exec는 **실행 중인 컨테이너와 직접 상호작용**할 수 있는 기능입니다. SSH 키 관리나 인바운드 포트 개방 없이 컨테이너에 접속할 수 있습니다.

## 사용 사례

### 개발 환경
- 컨테이너 프로세스 디버깅
- 로그 확인
- 설정 파일 점검

### 프로덕션 환경
- 긴급 문제 해결 (Break-glass access)
- 임시 진단
- 트러블슈팅

## 지원 환경

| 환경 | Linux | Windows |
|------|-------|---------|
| EC2 | O | O |
| Fargate | O | O |
| ECS Anywhere | O | O |

## 설정 방법

### 1. 필수 조건

**AWS CLI 버전:**
- AWS CLI v1: 1.22.3 이상
- AWS CLI v2: 2.3.6 이상

**Session Manager 플러그인:**
```bash
# macOS
brew install session-manager-plugin

# Linux
curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" -o "session-manager-plugin.deb"
sudo dpkg -i session-manager-plugin.deb
```

### 2. IAM 권한 설정

**Task Role에 추가:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssmmessages:CreateControlChannel",
        "ssmmessages:CreateDataChannel",
        "ssmmessages:OpenControlChannel",
        "ssmmessages:OpenDataChannel"
      ],
      "Resource": "*"
    }
  ]
}
```

**사용자 IAM 정책:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:ExecuteCommand"
      ],
      "Resource": [
        "arn:aws:ecs:region:account:task/cluster-name/*"
      ]
    }
  ]
}
```

### 3. 서비스에서 활성화

**서비스 생성 시:**

```bash
aws ecs create-service \
    --cluster my-cluster \
    --service-name my-service \
    --task-definition my-task:1 \
    --desired-count 1 \
    --enable-execute-command
```

**기존 서비스 업데이트:**

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --enable-execute-command \
    --force-new-deployment
```

**독립 태스크 실행 시:**

```bash
aws ecs run-task \
    --cluster my-cluster \
    --task-definition my-task:1 \
    --enable-execute-command
```

### 4. 설정 확인

```bash
aws ecs describe-tasks \
    --cluster my-cluster \
    --tasks task-id \
    --query 'tasks[0].{enableExecuteCommand:enableExecuteCommand, containers:containers[*].{name:name, managedAgents:managedAgents}}'
```

확인 사항:
- `enableExecuteCommand`: true
- `ExecuteCommandAgent` lastStatus: RUNNING

## 사용 방법

### 기본 실행

```bash
aws ecs execute-command \
    --cluster my-cluster \
    --task task-id \
    --container container-name \
    --interactive \
    --command "/bin/bash"
```

### 특정 명령어 실행

```bash
aws ecs execute-command \
    --cluster my-cluster \
    --task task-id \
    --container container-name \
    --interactive \
    --command "cat /etc/hosts"
```

### 여러 컨테이너가 있는 경우

```bash
# 특정 컨테이너 지정
aws ecs execute-command \
    --cluster my-cluster \
    --task task-id \
    --container app \
    --interactive \
    --command "/bin/sh"
```

## 로깅 설정

### CloudWatch Logs

```bash
aws ecs create-cluster \
    --cluster-name my-cluster \
    --configuration '{
        "executeCommandConfiguration": {
            "logging": "OVERRIDE",
            "logConfiguration": {
                "cloudWatchLogGroupName": "/ecs/exec-logs",
                "cloudWatchEncryptionEnabled": false
            }
        }
    }'
```

### S3

```json
{
  "executeCommandConfiguration": {
    "logging": "OVERRIDE",
    "logConfiguration": {
      "s3BucketName": "my-exec-logs-bucket",
      "s3KeyPrefix": "exec-logs",
      "s3EncryptionEnabled": true
    }
  }
}
```

### 암호화 (KMS)

```json
{
  "executeCommandConfiguration": {
    "kmsKeyId": "arn:aws:kms:region:account:key/key-id",
    "logging": "OVERRIDE",
    "logConfiguration": {
      "cloudWatchLogGroupName": "/ecs/exec-logs",
      "cloudWatchEncryptionEnabled": true
    }
  }
}
```

## 접근 제어

### 특정 태그 기반 제한

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecs:ExecuteCommand",
      "Resource": "arn:aws:ecs:region:account:task/cluster/*",
      "Condition": {
        "StringEquals": {
          "aws:ResourceTag/Environment": "development"
        }
      }
    }
  ]
}
```

### 특정 컨테이너만 허용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecs:ExecuteCommand",
      "Resource": "arn:aws:ecs:region:account:task/cluster/*",
      "Condition": {
        "StringEquals": {
          "ecs:container-name": "debug-container"
        }
      }
    }
  ]
}
```

### 특정 클러스터만 허용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecs:ExecuteCommand",
      "Resource": "arn:aws:ecs:region:account:task/dev-cluster/*"
    }
  ]
}
```

## 감사 및 모니터링

### CloudTrail 이벤트

모든 `ExecuteCommand` 호출은 CloudTrail에 기록됩니다:

```json
{
  "eventName": "ExecuteCommand",
  "userIdentity": {
    "userName": "developer"
  },
  "requestParameters": {
    "cluster": "my-cluster",
    "task": "task-id",
    "container": "app",
    "command": "/bin/bash",
    "interactive": true
  }
}
```

### 명령어 로깅

실행된 명령어와 출력을 CloudWatch Logs 또는 S3에 저장:

```
2024-01-15T10:30:00Z [session-id] $ ls -la
2024-01-15T10:30:01Z [session-id] total 64
2024-01-15T10:30:01Z [session-id] drwxr-xr-x 1 root root 4096 Jan 15 10:00 .
```

## 제한 사항

| 제한 | 설명 |
|------|------|
| PID 네임스페이스 | 하나의 PID 네임스페이스당 1개 세션만 가능 |
| 유휴 타임아웃 | 20분 (변경 불가) |
| 기존 태스크 | 활성화 후 새로 시작된 태스크만 지원 |
| IPv6 전용 | 미지원 |
| Nano Server | 미지원 |

## 보안 고려사항

### 1. 최소 권한 원칙

- 필요한 사용자에게만 `ecs:ExecuteCommand` 권한 부여
- 태그나 조건으로 범위 제한

### 2. 프로덕션 환경

- 프로덕션에서는 긴급 상황에만 사용
- 모든 세션 로깅 활성화
- 정기적인 감사 로그 검토

### 3. 루트 권한

명령은 기본적으로 root 사용자로 실행됩니다. 컨테이너 내 사용자를 지정하려면:

```json
{
  "containerDefinitions": [
    {
      "user": "1000:1000"
    }
  ]
}
```

## 트러블슈팅

### ECS Exec 체커 도구

```bash
# Amazon ECS Exec Checker 설치
git clone https://github.com/aws-containers/amazon-ecs-exec-checker.git
cd amazon-ecs-exec-checker
./check-ecs-exec.sh my-cluster task-id
```

### 일반적인 문제

**1. "Unable to start command" 오류**

- Task Role에 SSM 권한 확인
- ExecuteCommandAgent 상태 확인

**2. "Session Manager plugin not found"**

- Session Manager 플러그인 설치 확인
- PATH 환경변수 확인

**3. "execute-command is not enabled"**

- 서비스/태스크에서 ECS Exec 활성화 확인
- 새 배포 필요 (기존 태스크는 미지원)

**4. 연결 타임아웃**

- 네트워크 연결 확인
- VPC 엔드포인트 확인 (프라이빗 서브넷)

### 필요한 VPC 엔드포인트

프라이빗 서브넷에서 사용 시:

| 엔드포인트 | 서비스 |
|-----------|--------|
| com.amazonaws.region.ssmmessages | SSM Messages |
| com.amazonaws.region.ssm | Systems Manager |
| com.amazonaws.region.kms | KMS (암호화 사용 시) |
| com.amazonaws.region.logs | CloudWatch Logs (로깅 시) |
| com.amazonaws.region.s3 | S3 (로깅 시) |

## 모범 사례

### 1. 환경별 정책

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecs:ExecuteCommand",
      "Resource": "arn:aws:ecs:region:account:task/dev-*/*"
    },
    {
      "Effect": "Deny",
      "Action": "ecs:ExecuteCommand",
      "Resource": "arn:aws:ecs:region:account:task/prod-*/*"
    }
  ]
}
```

### 2. 로깅 필수화

```json
{
  "executeCommandConfiguration": {
    "logging": "OVERRIDE",
    "logConfiguration": {
      "cloudWatchLogGroupName": "/ecs/exec-logs"
    }
  }
}
```

### 3. 알림 설정

CloudWatch 알람으로 ExecuteCommand 사용 감지:

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name ecs-exec-usage \
    --metric-name ExecuteCommandCount \
    --namespace AWS/ECS \
    --statistic Sum \
    --period 300 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold
```

## 참고 자료

- [ECS Exec 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-exec.html)
- [ECS Exec Checker](https://github.com/aws-containers/amazon-ecs-exec-checker)
