# ECS 배포 전략

## 개요

Amazon ECS는 서비스 업데이트를 위한 여러 배포 전략을 제공합니다.

## 배포 유형 비교

| 배포 유형 | 설명 | 다운타임 | 롤백 |
|----------|------|---------|------|
| **Rolling Update** | 점진적 교체 | 최소화 | 수동 |
| **Blue/Green** | 환경 전환 | 없음 | 자동/수동 |

## Rolling Update 배포

### 개요

ECS 서비스 스케줄러가 **현재 실행 중인 태스크를 새 태스크로 점진적으로 교체**합니다.

### 동작 방식

```
┌─────────────────────────────────────────────────────────────┐
│                    Rolling Update 과정                       │
│                                                              │
│  Initial State:     [v1] [v1] [v1] [v1]                     │
│                          ↓                                   │
│  Step 1:            [v1] [v1] [v1] [v2] [v2]  (새 태스크 추가)│
│                          ↓                                   │
│  Step 2:            [v1] [v2] [v2] [v2]       (구 태스크 제거)│
│                          ↓                                   │
│  Step 3:            [v2] [v2] [v2] [v2]       (완료)         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 주요 파라미터

```json
{
  "deploymentConfiguration": {
    "minimumHealthyPercent": 100,
    "maximumPercent": 200,
    "deploymentCircuitBreaker": {
      "enable": true,
      "rollback": true
    }
  }
}
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| **minimumHealthyPercent** | 배포 중 유지해야 할 최소 태스크 비율 | 100 |
| **maximumPercent** | 배포 중 실행 가능한 최대 태스크 비율 | 200 |

### 배포 시나리오

**시나리오 1: 무중단 배포 (권장)**

```json
{
  "desiredCount": 4,
  "deploymentConfiguration": {
    "minimumHealthyPercent": 100,
    "maximumPercent": 200
  }
}
```

동작:
1. 새 태스크 4개 시작 (총 8개)
2. 새 태스크 정상 확인
3. 기존 태스크 4개 종료
4. 최종: 새 태스크 4개

**시나리오 2: 리소스 제약 환경**

```json
{
  "desiredCount": 4,
  "deploymentConfiguration": {
    "minimumHealthyPercent": 50,
    "maximumPercent": 100
  }
}
```

동작:
1. 기존 태스크 2개 종료 (2개 유지)
2. 새 태스크 2개 시작 (4개)
3. 나머지 기존 태스크 2개 종료
4. 새 태스크 2개 시작

**시나리오 3: 빠른 배포**

```json
{
  "desiredCount": 4,
  "deploymentConfiguration": {
    "minimumHealthyPercent": 0,
    "maximumPercent": 100
  }
}
```

동작:
1. 모든 기존 태스크 종료
2. 새 태스크 4개 시작
- **주의**: 다운타임 발생

## Circuit Breaker (서킷 브레이커)

### 개요

배포 실패 시 **자동으로 롤백**하는 메커니즘입니다.

### 설정

```json
{
  "deploymentConfiguration": {
    "deploymentCircuitBreaker": {
      "enable": true,
      "rollback": true
    }
  }
}
```

### 동작 원리

```
┌─────────────────────────────────────────────────────────────┐
│                Circuit Breaker 동작                          │
│                                                              │
│  배포 시작 → 태스크 시작 시도                                │
│                    ↓                                         │
│              태스크 실패?                                    │
│              /        \                                      │
│           No           Yes                                   │
│            ↓            ↓                                    │
│       계속 진행    실패 횟수 증가                            │
│                         ↓                                    │
│                 임계값 초과?                                 │
│                 /        \                                   │
│              No           Yes                                │
│               ↓            ↓                                 │
│          재시도      배포 실패 선언                          │
│                         ↓                                    │
│                 rollback: true?                              │
│                 /        \                                   │
│              No           Yes                                │
│               ↓            ↓                                 │
│         배포 중단    이전 버전으로 롤백                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 실패 임계값

기본 공식:
```
임계값 = min(10, max(3, desiredCount / 2))
```

| desiredCount | 임계값 |
|--------------|--------|
| 1-6 | 3 |
| 7-20 | desiredCount/2 |
| 20+ | 10 |

## Blue/Green 배포

### 개요

AWS CodeDeploy로 제어되는 배포 모델입니다. **새 배포를 프로덕션 트래픽 전환 전에 검증**할 수 있습니다.

### 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                  Blue/Green 배포 아키텍처                    │
│                                                              │
│                    ┌─────────────────┐                      │
│                    │  Load Balancer  │                      │
│                    └────────┬────────┘                      │
│                             │                                │
│              ┌──────────────┼──────────────┐                │
│              │              │              │                │
│              ▼              │              ▼                │
│     ┌──────────────┐       │     ┌──────────────┐          │
│     │ Target Group │       │     │ Target Group │          │
│     │   (Blue)     │       │     │   (Green)    │          │
│     └──────┬───────┘       │     └──────┬───────┘          │
│            │               │            │                   │
│     ┌──────▼───────┐       │     ┌──────▼───────┐          │
│     │  v1 Tasks    │◄──────┘     │  v2 Tasks    │          │
│     │  (Current)   │  Production │  (New)       │          │
│     └──────────────┘  Traffic    └──────────────┘          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 트래픽 전환 방식

| 방식 | 설명 |
|------|------|
| **Canary** | 트래픽을 두 단계로 이동 |
| **Linear** | 균등한 간격으로 점진적 이동 |
| **All-at-Once** | 모든 트래픽을 즉시 전환 |

### 사전 정의 배포 구성

| 구성 | 설명 |
|------|------|
| ECSAllAtOnce | 즉시 전환 |
| ECSLinear10PercentEvery1Minutes | 1분마다 10% |
| ECSLinear10PercentEvery3Minutes | 3분마다 10% |
| ECSCanary10Percent5Minutes | 10% 후 5분 뒤 90% |
| ECSCanary10Percent15Minutes | 10% 후 15분 뒤 90% |

### 요구사항

1. **로드 밸런서**: ALB 또는 NLB 필수
2. **대상 그룹**: 2개 필요 (Blue, Green)
3. **리스너**: 프로덕션 리스너 필수, 테스트 리스너 선택

### CodeDeploy 설정

**appspec.yaml:**

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: "arn:aws:ecs:region:account:task-definition/my-app:2"
        LoadBalancerInfo:
          ContainerName: "web"
          ContainerPort: 80
        PlatformVersion: "LATEST"
```

### Blue/Green 배포 생성

```bash
aws deploy create-deployment \
    --application-name my-app \
    --deployment-group-name my-deployment-group \
    --revision '{
        "revisionType": "AppSpecContent",
        "appSpecContent": {
            "content": "..."
        }
    }'
```

### 검증 및 롤백

**테스트 리스너로 검증:**
1. 테스트 트래픽을 Green 환경으로 라우팅
2. 수동/자동 테스트 수행
3. 문제 없으면 프로덕션 트래픽 전환

**롤백:**
```bash
aws deploy stop-deployment \
    --deployment-id d-XXXXX \
    --auto-rollback-enabled
```

## CloudWatch 알람 기반 배포 제어

### 설정

```json
{
  "alarms": {
    "alarmNames": [
      "my-app-error-rate-alarm",
      "my-app-latency-alarm"
    ],
    "enable": true,
    "rollback": true
  }
}
```

### 동작

1. 배포 중 지정된 CloudWatch 알람 모니터링
2. 알람이 ALARM 상태가 되면 배포 중단
3. `rollback: true`면 이전 버전으로 롤백

## 배포 전략 선택 가이드

### Rolling Update 선택 시

- 간단한 배포 프로세스 원할 때
- 추가 인프라 비용 없이 배포할 때
- 빠른 롤백보다 점진적 롤백 선호 시

### Blue/Green 선택 시

- 배포 전 새 버전 검증 필요 시
- 즉각적인 롤백이 중요할 때
- 트래픽 전환 제어가 필요할 때
- 다운타임이 절대 허용되지 않을 때

## 배포 모범 사례

### 1. 헬스 체크 설정

```json
{
  "healthCheck": {
    "command": ["CMD-SHELL", "curl -f http://localhost/health || exit 1"],
    "interval": 30,
    "timeout": 5,
    "retries": 3,
    "startPeriod": 60
  }
}
```

### 2. 헬스 체크 유예 기간

```json
{
  "healthCheckGracePeriodSeconds": 120
}
```

- 애플리케이션 시작 시간 고려
- 유예 기간 동안 헬스 체크 실패 무시

### 3. 배포 모니터링

```bash
# 배포 상태 확인
aws ecs describe-services \
    --cluster my-cluster \
    --services my-service \
    --query 'services[0].deployments'
```

### 4. 점진적 롤아웃

**대규모 서비스의 경우:**

```json
{
  "deploymentConfiguration": {
    "minimumHealthyPercent": 90,
    "maximumPercent": 110
  }
}
```

- 적은 수의 새 태스크로 시작
- 문제 발생 시 영향 최소화

### 5. 배포 알림

```bash
# SNS 토픽으로 이벤트 전송 설정
aws events put-rule \
    --name ecs-deployment-events \
    --event-pattern '{
        "source": ["aws.ecs"],
        "detail-type": ["ECS Deployment State Change"]
    }'
```

## 트러블슈팅

### 배포가 멈춤

1. 태스크 정의 확인 (이미지, 리소스)
2. 헬스 체크 설정 확인
3. 네트워크/보안 그룹 확인
4. CloudWatch Logs 확인

### 롤백 발생

1. Circuit Breaker 이벤트 확인
2. 실패한 태스크 로그 확인
3. 리소스 부족 여부 확인

### 느린 배포

1. minimumHealthyPercent/maximumPercent 조정
2. 헬스 체크 간격 최적화
3. 이미지 크기 최소화

## 참고 자료

- [Rolling Update 배포](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-ecs.html)
- [Blue/Green 배포](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html)
- [CodeDeploy ECS 배포](https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-groups-create-ecs.html)
