# Amazon ECS Service

## 개요

ECS Service는 **지정된 수의 태스크 인스턴스를 클러스터에서 동시에 실행하고 유지**하는 구성입니다. 태스크가 실패하면 자동으로 새 태스크를 시작하여 원하는 상태를 유지합니다.

## Service vs Task

```
┌─────────────────────────────────────────────────────────────┐
│                         Service                              │
│  "원하는 수의 태스크를 지속적으로 실행 및 유지"               │
│                                                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │
│  │  Task   │  │  Task   │  │  Task   │                      │
│  │ (실행중)│  │ (실행중)│  │ (실행중)│                      │
│  └─────────┘  └─────────┘  └─────────┘                      │
│                                                              │
│  ↓ 태스크 실패 시                                            │
│                                                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │
│  │  Task   │  │  Task   │  │  Task   │  ← 자동 재시작        │
│  │ (실행중)│  │ (실행중)│  │  (NEW)  │                      │
│  └─────────┘  └─────────┘  └─────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

| 구분 | Task | Service |
|------|------|---------|
| 목적 | 일회성 실행 | 지속적 실행 |
| 실패 시 | 종료 | 자동 재시작 |
| 스케일링 | 수동 | Auto Scaling 가능 |
| 로드 밸런서 | 직접 연결 불가 | 통합 가능 |

## Service 구성 요소

### 기본 구성

```json
{
  "serviceName": "my-service",
  "cluster": "my-cluster",
  "taskDefinition": "my-app:1",
  "desiredCount": 3,
  "launchType": "FARGATE"
}
```

### 필수 파라미터

| 파라미터 | 설명 |
|----------|------|
| **serviceName** | 서비스 이름 |
| **cluster** | 클러스터 이름 또는 ARN |
| **taskDefinition** | 태스크 정의 (family:revision) |
| **desiredCount** | 원하는 태스크 수 |

### 인프라 선택

**1. Launch Type**

```json
{
  "launchType": "FARGATE"
}
```

| 유형 | 설명 |
|------|------|
| FARGATE | 서버리스 컴퓨팅 |
| EC2 | EC2 인스턴스 |
| EXTERNAL | 온프레미스 (ECS Anywhere) |

**2. Capacity Provider Strategy**

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

## Service Scheduler

### 스케줄러 동작

ECS 서비스 스케줄러는 다음을 보장합니다:

1. **원하는 수 유지**: desiredCount만큼의 태스크 실행
2. **자동 복구**: 실패한 태스크 자동 교체
3. **헬스 체크**: 불건강한 태스크 교체

### Throttle 로직

반복적으로 실패하는 태스크의 재시작 시도를 제한합니다:

```
첫 번째 실패: 즉시 재시작
두 번째 실패: 15초 대기 후 재시작
세 번째 실패: 30초 대기 후 재시작
...
```

## 로드 밸런서 통합

### 지원 로드 밸런서

| 유형 | 설명 | 사용 사례 |
|------|------|----------|
| **Application Load Balancer** | Layer 7, HTTP/HTTPS | 웹 애플리케이션 |
| **Network Load Balancer** | Layer 4, TCP/UDP | 고성능, 저지연 |
| **Gateway Load Balancer** | 가상 어플라이언스 | 방화벽, IDS/IPS |

### ALB 설정

```json
{
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:region:account:targetgroup/my-tg/12345",
      "containerName": "web",
      "containerPort": 8080
    }
  ]
}
```

### 헬스 체크 설정

```json
{
  "healthCheckGracePeriodSeconds": 60
}
```

- 서비스 시작 후 지정 시간 동안 헬스 체크 실패 무시
- 애플리케이션 부팅 시간 고려

## 네트워크 설정 (awsvpc)

```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": [
        "subnet-12345678",
        "subnet-87654321"
      ],
      "securityGroups": [
        "sg-12345678"
      ],
      "assignPublicIp": "ENABLED"
    }
  }
}
```

| 파라미터 | 설명 |
|----------|------|
| **subnets** | 태스크 배치 서브넷 |
| **securityGroups** | 보안 그룹 |
| **assignPublicIp** | 퍼블릭 IP 할당 (ENABLED/DISABLED) |

## Service 생성

### AWS CLI

```bash
aws ecs create-service \
    --cluster my-cluster \
    --service-name my-service \
    --task-definition my-app:1 \
    --desired-count 3 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-12345678],securityGroups=[sg-12345678],assignPublicIp=ENABLED}" \
    --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=web,containerPort=8080"
```

### 전체 JSON 예시

```json
{
  "cluster": "my-cluster",
  "serviceName": "my-web-service",
  "taskDefinition": "my-web-app:1",
  "desiredCount": 3,
  "launchType": "FARGATE",
  "platformVersion": "LATEST",
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-12345678", "subnet-87654321"],
      "securityGroups": ["sg-12345678"],
      "assignPublicIp": "ENABLED"
    }
  },
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:region:account:targetgroup/my-tg/12345",
      "containerName": "web",
      "containerPort": 8080
    }
  ],
  "healthCheckGracePeriodSeconds": 60,
  "schedulingStrategy": "REPLICA",
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 100,
    "deploymentCircuitBreaker": {
      "enable": true,
      "rollback": true
    }
  },
  "enableExecuteCommand": true,
  "tags": [
    {
      "key": "Environment",
      "value": "production"
    }
  ]
}
```

## 스케줄링 전략

### REPLICA (기본)

```json
{
  "schedulingStrategy": "REPLICA",
  "desiredCount": 3
}
```

- 지정된 수의 태스크를 클러스터에 분산
- 가용 영역 간 균등 분산

### DAEMON

```json
{
  "schedulingStrategy": "DAEMON"
}
```

- 각 컨테이너 인스턴스에 정확히 하나의 태스크 실행
- 로그 수집, 모니터링 에이전트에 적합
- Fargate에서는 사용 불가

## 태스크 배치

### 배치 전략 (Placement Strategy)

```json
{
  "placementStrategy": [
    {
      "type": "spread",
      "field": "attribute:ecs.availability-zone"
    },
    {
      "type": "binpack",
      "field": "memory"
    }
  ]
}
```

| 전략 | 설명 |
|------|------|
| **spread** | 지정 필드 기준으로 균등 분산 |
| **binpack** | 리소스 사용 최적화 (빈 공간 채우기) |
| **random** | 무작위 배치 |

### 배치 제약 (Placement Constraints)

```json
{
  "placementConstraints": [
    {
      "type": "distinctInstance"
    },
    {
      "type": "memberOf",
      "expression": "attribute:ecs.instance-type =~ t3.*"
    }
  ]
}
```

| 제약 | 설명 |
|------|------|
| **distinctInstance** | 각 태스크를 다른 인스턴스에 배치 |
| **memberOf** | 표현식을 만족하는 인스턴스에만 배치 |

## Service 업데이트

### 태스크 정의 업데이트

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --task-definition my-app:2
```

### 태스크 수 조정

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --desired-count 5
```

### Force New Deployment

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --force-new-deployment
```

- 같은 태스크 정의로 새 배포 시작
- 모든 태스크 교체

## Service 이벤트

서비스 상태 변화를 CloudWatch Events로 전송합니다:

| 이벤트 | 설명 |
|--------|------|
| SERVICE_STEADY_STATE | 원하는 상태 도달 |
| SERVICE_TASK_PLACEMENT_FAILURE | 태스크 배치 실패 |
| SERVICE_DAEMON_PLACEMENT_UPDATED | 데몬 배치 변경 |

## 모범 사례

### 1. 고가용성 설정

```json
{
  "desiredCount": 3,
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-az1", "subnet-az2", "subnet-az3"]
    }
  },
  "placementStrategy": [
    {
      "type": "spread",
      "field": "attribute:ecs.availability-zone"
    }
  ]
}
```

### 2. 배포 안정성

```json
{
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 100,
    "deploymentCircuitBreaker": {
      "enable": true,
      "rollback": true
    }
  },
  "healthCheckGracePeriodSeconds": 120
}
```

### 3. 비용 최적화

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

## Service 삭제

```bash
# 태스크 수를 0으로 설정
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --desired-count 0

# 서비스 삭제
aws ecs delete-service \
    --cluster my-cluster \
    --service my-service
```

## 참고 자료

- [ECS 서비스 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs_services.html)
- [서비스 로드 밸런싱](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-load-balancing.html)
