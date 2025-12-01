# ECS Auto Scaling

## 개요

Amazon ECS는 두 가지 레벨의 Auto Scaling을 제공합니다:

1. **Service Auto Scaling**: 태스크 수 자동 조정
2. **Cluster Auto Scaling**: EC2 인스턴스 용량 자동 조정

```
┌─────────────────────────────────────────────────────────────┐
│                    ECS Auto Scaling 구조                     │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Service Auto Scaling                      │  │
│  │           (태스크 수 조정: 3 → 6 → 10)                 │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                  │
│                           ▼                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Cluster Auto Scaling                      │  │
│  │         (EC2 인스턴스 수 조정: 2 → 4 → 6)             │  │
│  │              (Capacity Provider 사용)                  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Service Auto Scaling

### 개요

Application Auto Scaling 서비스를 통해 ** 태스크 수를 자동으로 조정** 합니다.

### 스케일링 유형

| 유형 | 설명 | 사용 사례 |
|------|------|----------|
| **Target Tracking** | 목표 메트릭 값 유지 | 일반적인 워크로드 |
| **Step Scaling** | 임계값 기반 단계별 조정 | 급격한 트래픽 변화 |
| **Scheduled Scaling** | 예약된 시간에 조정 | 예측 가능한 패턴 |
| **Predictive Scaling** | 과거 데이터 기반 예측 | 주기적인 패턴 |

### Target Tracking Scaling

목표 메트릭 값을 설정하면 자동으로 태스크 수를 조정합니다.

** 설정:**

```bash
aws application-autoscaling register-scalable-target \
    --service-namespace ecs \
    --scalable-dimension ecs:service:DesiredCount \
    --resource-id service/my-cluster/my-service \
    --min-capacity 2 \
    --max-capacity 10
```

**Target Tracking 정책:**

```bash
aws application-autoscaling put-scaling-policy \
    --service-namespace ecs \
    --scalable-dimension ecs:service:DesiredCount \
    --resource-id service/my-cluster/my-service \
    --policy-name cpu-target-tracking \
    --policy-type TargetTrackingScaling \
    --target-tracking-scaling-policy-configuration '{
        "TargetValue": 70.0,
        "PredefinedMetricSpecification": {
            "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
        },
        "ScaleOutCooldown": 60,
        "ScaleInCooldown": 300
    }'
```

** 사전 정의 메트릭:**

| 메트릭 | 설명 |
|--------|------|
| ECSServiceAverageCPUUtilization | 평균 CPU 사용률 |
| ECSServiceAverageMemoryUtilization | 평균 메모리 사용률 |
| ALBRequestCountPerTarget | 대상당 ALB 요청 수 |

** 커스텀 메트릭:**

```json
{
  "TargetValue": 1000.0,
  "CustomizedMetricSpecification": {
    "MetricName": "RequestCount",
    "Namespace": "MyApp",
    "Dimensions": [
      {
        "Name": "ServiceName",
        "Value": "my-service"
      }
    ],
    "Statistic": "Sum",
    "Unit": "Count"
  }
}
```

### Step Scaling

CloudWatch 알람 임계값에 따라 단계별로 조정합니다.

```bash
aws application-autoscaling put-scaling-policy \
    --service-namespace ecs \
    --scalable-dimension ecs:service:DesiredCount \
    --resource-id service/my-cluster/my-service \
    --policy-name cpu-step-scaling \
    --policy-type StepScaling \
    --step-scaling-policy-configuration '{
        "AdjustmentType": "ChangeInCapacity",
        "StepAdjustments": [
            {
                "MetricIntervalLowerBound": 0,
                "MetricIntervalUpperBound": 20,
                "ScalingAdjustment": 1
            },
            {
                "MetricIntervalLowerBound": 20,
                "MetricIntervalUpperBound": 40,
                "ScalingAdjustment": 2
            },
            {
                "MetricIntervalLowerBound": 40,
                "ScalingAdjustment": 3
            }
        ],
        "Cooldown": 60
    }'
```

** 조정 유형:**

| 유형 | 설명 |
|------|------|
| ChangeInCapacity | 지정된 수만큼 조정 |
| ExactCapacity | 정확한 용량으로 설정 |
| PercentChangeInCapacity | 비율로 조정 |

### Scheduled Scaling

```bash
aws application-autoscaling put-scheduled-action \
    --service-namespace ecs \
    --scalable-dimension ecs:service:DesiredCount \
    --resource-id service/my-cluster/my-service \
    --scheduled-action-name scale-up-morning \
    --schedule "cron(0 9 * * ? *)" \
    --scalable-target-action MinCapacity=5,MaxCapacity=20

aws application-autoscaling put-scheduled-action \
    --service-namespace ecs \
    --scalable-dimension ecs:service:DesiredCount \
    --resource-id service/my-cluster/my-service \
    --scheduled-action-name scale-down-night \
    --schedule "cron(0 22 * * ? *)" \
    --scalable-target-action MinCapacity=2,MaxCapacity=5
```

### Predictive Scaling

과거 로드 데이터를 분석하여 일일/주간 패턴을 감지하고 예측합니다.

```json
{
  "PredictiveScalingConfiguration": {
    "MetricSpecifications": [
      {
        "TargetValue": 70,
        "PredefinedMetricPairSpecification": {
          "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
        }
      }
    ],
    "Mode": "ForecastAndScale",
    "SchedulingBufferTime": 300
  }
}
```

## Cluster Auto Scaling

### 개요

Capacity Provider의 관리형 스케일링을 통해 **EC2 인스턴스 수를 자동으로 조정** 합니다.

### 핵심 메트릭: CapacityProviderReservation

```
CapacityProviderReservation = (필요한 인스턴스 수) / (실행 중인 인스턴스 수) × 100
```

### 스케일링 동작

| 조건 | 동작 |
|------|------|
| Reservation = targetCapacity | 유지 |
| Reservation > targetCapacity | Scale Out (인스턴스 추가) |
| Reservation < targetCapacity | Scale In (인스턴스 종료) |

### Capacity Provider 설정

```bash
aws ecs create-capacity-provider \
    --name my-capacity-provider \
    --auto-scaling-group-provider '{
        "autoScalingGroupArn": "arn:aws:autoscaling:region:account:autoScalingGroup:id:autoScalingGroupName/my-asg",
        "managedScaling": {
            "status": "ENABLED",
            "targetCapacity": 100,
            "minimumScalingStepSize": 1,
            "maximumScalingStepSize": 10,
            "instanceWarmupPeriod": 300
        },
        "managedTerminationProtection": "ENABLED"
    }'
```

### 관리형 스케일링 파라미터

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| targetCapacity | 목표 용량 사용률 (%) | 100 |
| minimumScalingStepSize | 최소 스케일링 단위 | 1 |
| maximumScalingStepSize | 최대 스케일링 단위 | 10000 |
| instanceWarmupPeriod | 웜업 대기 시간 (초) | 300 |

### 여유 용량 유지

```json
{
  "managedScaling": {
    "targetCapacity": 80
  }
}
```

- targetCapacity를 100% 미만으로 설정
- 급격한 트래픽 증가 대비 여유 용량 확보

## 스케일링 정책 조합

### 예시: 복합 스케일링 전략

```
┌─────────────────────────────────────────────────────────────┐
│                    복합 스케일링 전략                        │
│                                                              │
│  Scheduled Scaling:                                          │
│    - 09:00 → min: 5, max: 20 (업무 시간)                   │
│    - 22:00 → min: 2, max: 5 (야간)                         │
│                                                              │
│  Target Tracking:                                            │
│    - CPU 70% 유지                                           │
│    - 요청 수 1000/분 유지                                   │
│                                                              │
│  Step Scaling (비상 대응):                                   │
│    - CPU 90% → 즉시 +3 태스크                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 쿨다운 기간

### Scale Out Cooldown

새 태스크 추가 후 다음 스케일 아웃까지 대기 시간

```json
{
  "ScaleOutCooldown": 60
}
```

### Scale In Cooldown

태스크 제거 후 다음 스케일 인까지 대기 시간

```json
{
  "ScaleInCooldown": 300
}
```

** 권장:**
- Scale Out: 짧게 (60초) - 빠른 대응
- Scale In: 길게 (300초) - 안정성 확보

## 스케일링과 배포

### 배포 중 스케일링

- **Scale Out**: 배포 중에도 계속 진행
- **Scale In**: 배포 중에는 중지

### 헬스 체크와 스케일링

새 태스크가 헬스 체크를 통과해야 스케일링 완료로 간주됩니다.

## 모니터링

### CloudWatch 메트릭

```bash
# 서비스 메트릭 조회
aws cloudwatch get-metric-statistics \
    --namespace AWS/ECS \
    --metric-name CPUUtilization \
    --dimensions Name=ClusterName,Value=my-cluster Name=ServiceName,Value=my-service \
    --start-time 2024-01-01T00:00:00Z \
    --end-time 2024-01-01T01:00:00Z \
    --period 60 \
    --statistics Average
```

### CloudWatch 알람 예시

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name high-cpu-alarm \
    --metric-name CPUUtilization \
    --namespace AWS/ECS \
    --dimensions Name=ClusterName,Value=my-cluster Name=ServiceName,Value=my-service \
    --statistic Average \
    --period 60 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --evaluation-periods 2
```

## 모범 사례

### 1. 적절한 메트릭 선택

| 워크로드 | 권장 메트릭 |
|----------|------------|
| CPU 집약적 | CPUUtilization |
| 메모리 집약적 | MemoryUtilization |
| 웹 서비스 | ALBRequestCountPerTarget |
| 큐 처리 | 큐 깊이 (커스텀 메트릭) |

### 2. 최소/최대 용량 설정

```bash
--min-capacity 2   # 고가용성 보장
--max-capacity 20  # 비용 제한
```

### 3. 비용 최적화

```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 4
    },
    {
      "capacityProvider": "FARGATE",
      "base": 1,
      "weight": 1
    }
  ]
}
```

### 4. 스케일 인 보호

```bash
aws ecs update-service \
    --cluster my-cluster \
    --service my-service \
    --deployment-configuration '{
        "deploymentCircuitBreaker": {
            "enable": true,
            "rollback": true
        }
    }'
```

### 5. 알람 설정

```bash
# 스케일링 실패 알람
aws cloudwatch put-metric-alarm \
    --alarm-name scaling-activity-failed \
    --metric-name FailedScalingActivityCount \
    --namespace AWS/AutoScaling \
    --statistic Sum \
    --period 300 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold
```

## 제한 사항

| 항목 | 한도 |
|------|------|
| 서비스당 스케일링 정책 | 5개 |
| 서비스당 예약 작업 | 200개 |
| Target Tracking 정책당 알람 | 2개 (high/low) |

## 트러블슈팅

### 스케일링이 안 될 때

1. CloudWatch 메트릭 수신 확인
2. IAM 권한 확인
3. 최소/최대 용량 확인
4. 쿨다운 기간 확인

### 스케일링이 너무 자주 발생할 때

1. 쿨다운 기간 늘리기
2. 임계값 조정
3. Step Scaling 검토

## 참고 자료

- [Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)
- [Cluster Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/cluster-auto-scaling.html)
- [Application Auto Scaling](https://docs.aws.amazon.com/autoscaling/application/userguide/)
