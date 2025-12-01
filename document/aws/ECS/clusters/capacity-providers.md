# ECS Capacity Provider (용량 제공자)

## 개요

Capacity Provider는 태스크 실행을 위한 인프라 용량을 관리하는 구성 요소입니다. Capacity Provider를 통해 ECS가 자동으로 인프라를 확장/축소할 수 있습니다.

## Capacity Provider 유형

### 1. Fargate Capacity Provider

서버리스 컴퓨팅 용량을 제공합니다.

| 제공자 | 설명 | 특징 |
|--------|------|------|
| **FARGATE** | 표준 Fargate 용량 | 안정적, 즉시 사용 가능 |
| **FARGATE_SPOT** | Spot 용량 | 최대 70% 할인, 중단 가능 |

**Fargate Spot 특징:**
- 여유 용량에서 실행
- 2분 경고 후 중단 가능
- 중단 허용 워크로드에 적합

### 2. Auto Scaling Group Capacity Provider

EC2 인스턴스 기반 용량을 제공합니다.

```
┌─────────────────────────────────────────────────┐
│           Capacity Provider                      │
│  ┌─────────────────────────────────────────┐    │
│  │         Auto Scaling Group               │    │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐   │    │
│  │  │   EC2   │ │   EC2   │ │   EC2   │   │    │
│  │  │Instance │ │Instance │ │Instance │   │    │
│  │  └─────────┘ └─────────┘ └─────────┘   │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

## Capacity Provider 생성

### Fargate Capacity Provider

Fargate Provider는 기본 제공되며 별도 생성 불필요합니다.

```bash
# 클러스터에 Fargate 용량 제공자 연결
aws ecs put-cluster-capacity-providers \
    --cluster my-cluster \
    --capacity-providers FARGATE FARGATE_SPOT \
    --default-capacity-provider-strategy \
        capacityProvider=FARGATE,weight=1,base=1 \
        capacityProvider=FARGATE_SPOT,weight=4
```

### Auto Scaling Group Capacity Provider

```bash
# Auto Scaling Group 기반 Capacity Provider 생성
aws ecs create-capacity-provider \
    --name my-asg-provider \
    --auto-scaling-group-provider \
        autoScalingGroupArn=arn:aws:autoscaling:region:account:autoScalingGroup:id:autoScalingGroupName/my-asg,\
        managedScaling='{
            "status": "ENABLED",
            "targetCapacity": 100,
            "minimumScalingStepSize": 1,
            "maximumScalingStepSize": 10
        }',\
        managedTerminationProtection="ENABLED"
```

## Managed Scaling (관리형 스케일링)

Capacity Provider가 자동으로 인프라를 조정하는 기능입니다.

### 핵심 메트릭: CapacityProviderReservation

```
CapacityProviderReservation = (필요한 인스턴스 수) / (실행 중인 인스턴스 수) × 100
```

### 스케일링 동작

| 조건 | 동작 |
|------|------|
| Reservation = targetCapacity | 스케일링 불필요 |
| Reservation > targetCapacity | **Scale-Out**: 인스턴스 추가 |
| Reservation < targetCapacity | **Scale-In**: 인스턴스 종료 |

### 설정 파라미터

```json
{
  "managedScaling": {
    "status": "ENABLED",
    "targetCapacity": 100,
    "minimumScalingStepSize": 1,
    "maximumScalingStepSize": 10,
    "instanceWarmupPeriod": 300
  }
}
```

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| **status** | 관리형 스케일링 활성화 | DISABLED |
| **targetCapacity** | 목표 용량 사용률 (%) | 100 |
| **minimumScalingStepSize** | 최소 스케일링 단위 | 1 |
| **maximumScalingStepSize** | 최대 스케일링 단위 | 10000 |
| **instanceWarmupPeriod** | 웜업 대기 시간 (초) | 300 |

## Capacity Provider Strategy

태스크를 여러 Capacity Provider에 분산하는 전략입니다.

### 전략 구성

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "base": 2,
      "weight": 1
    },
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 4
    }
  ]
}
```

### 파라미터 설명

| 파라미터 | 설명 |
|----------|------|
| **base** | 최소 태스크 수 (첫 번째 제공자만 적용) |
| **weight** | 추가 태스크 배치 비율 |

### 배치 예시

**설정:** Fargate (base=2, weight=1), Fargate Spot (weight=4)
**총 태스크: 10개**

```
Step 1: base 적용
  - Fargate: 2개

Step 2: weight 비율로 나머지 8개 배치
  - Fargate: 2 + (8 × 1/5) = 2 + 1.6 ≈ 4개
  - Fargate Spot: (8 × 4/5) = 6.4 ≈ 6개

최종 결과:
  - Fargate: 4개
  - Fargate Spot: 6개
```

## 클러스터 Auto Scaling

EC2 기반 Capacity Provider의 자동 스케일링입니다.

### 생성되는 리소스

Capacity Provider 생성 시 자동 생성:
1. CloudWatch 저가 알람 (Scale-In)
2. CloudWatch 고가 알람 (Scale-Out)
3. 목표 추적 스케일링 정책

### IAM 요구사항

**서비스 연결 역할:** `AWSServiceRoleForECS`

**필수 권한:**
- `autoscaling:CreateOrUpdateTags`
- Auto Scaling Group 관리 권한

### 제약 사항

1. Capacity Provider와 연결된 ASG의 DesiredCapacity를 다른 정책으로 변경하면 안 됨
2. `AmazonECSManaged` 태그 제거 금지
3. MinCapacity와 MaxCapacity는 자동 수정되지 않음
4. 관리형 스케일링 활성화 시 하나의 클러스터에만 연결 가능

### 최적화 전략

**Binpack 전략 권장:**
```json
{
  "placementStrategy": [
    {
      "type": "binpack",
      "field": "memory"
    }
  ]
}
```

- 용량 효율성이 가장 우수
- targetCapacity < 100%일 때, binpack이 spread보다 우선 적용

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
      "weight": 4
    }
  ]
}
```

- base로 최소 안정 용량 보장
- Spot으로 비용 절감

### 2. 안정성 우선

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

### 3. 혼합 인프라

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "base": 2,
      "weight": 1
    },
    {
      "capacityProvider": "my-ec2-provider",
      "weight": 2
    }
  ]
}
```

### 4. 여유 용량 유지

```json
{
  "managedScaling": {
    "targetCapacity": 80
  }
}
```

- 급격한 트래픽 증가 대비
- 20% 여유 용량 유지

## 트러블슈팅

### 스케일링이 안 될 때

1. IAM 권한 확인
2. `AmazonECSManaged` 태그 확인
3. ASG Min/Max 설정 확인
4. CloudWatch 알람 상태 확인

### 0개에서 스케일 아웃

- 관리형 스케일링 활성화 시 자동으로 2개 인스턴스 시작
- 정상 동작임

### Spot 중단 처리

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "base": 1
    },
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 4
    }
  ]
}
```

- base로 최소 On-Demand 보장
- Spot 중단 시 자동 대체

## 참고 자료

- [Capacity Provider 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/capacity-providers.html)
- [클러스터 Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/cluster-auto-scaling.html)
