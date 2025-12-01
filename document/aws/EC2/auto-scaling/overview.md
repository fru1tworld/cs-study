# EC2 Auto Scaling

## 개요

EC2 Auto Scaling은 애플리케이션 가용성을 유지하면서 EC2 용량을 자동으로 확장하거나 축소합니다. 수요에 따라 인스턴스 수를 동적으로 조정하여 비용을 최적화합니다.

## 핵심 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                    Auto Scaling 구성 요소                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  시작 템플릿     │  │  시작 구성       │                  │
│  │ (권장)          │  │ (레거시)        │                  │
│  │                 │  │                 │                  │
│  │ AMI, 인스턴스   │  │ AMI, 인스턴스   │                  │
│  │ 타입, 키페어,   │  │ 타입, 키페어    │                  │
│  │ 보안그룹 등     │  │                 │                  │
│  └────────┬────────┘  └────────┬────────┘                  │
│           │                    │                            │
│           └──────────┬─────────┘                            │
│                      ▼                                      │
│           ┌─────────────────────┐                           │
│           │  Auto Scaling 그룹   │                           │
│           │                     │                           │
│           │  - 최소/최대/희망 용량│                           │
│           │  - 가용 영역         │                           │
│           │  - 상태 검사         │                           │
│           └──────────┬──────────┘                           │
│                      │                                      │
│                      ▼                                      │
│           ┌─────────────────────┐                           │
│           │    스케일링 정책     │                           │
│           │                     │                           │
│           │  - 대상 추적        │                           │
│           │  - 단계 조정        │                           │
│           │  - 단순 조정        │                           │
│           │  - 예측 스케일링    │                           │
│           └─────────────────────┘                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 시작 템플릿 vs 시작 구성

| 특성 | 시작 템플릿 | 시작 구성 |
|------|------------|----------|
| 버전 관리 | 지원 | 미지원 |
| 수정 가능 | 새 버전 생성 | 불가 (교체만) |
| 혼합 인스턴스 | 지원 | 미지원 |
| Spot + On-Demand | 지원 | 미지원 |
| 상태 | **권장** | 지원 중단 예정 |

> 2024년 10월 이후 생성된 계정은 시작 구성을 생성할 수 없습니다.

### 시작 템플릿 생성

```bash
aws ec2 create-launch-template \
    --launch-template-name my-launch-template \
    --version-description "Version 1" \
    --launch-template-data '{
        "ImageId": "ami-xxxxxxxxx",
        "InstanceType": "t3.micro",
        "KeyName": "my-key-pair",
        "SecurityGroupIds": ["sg-xxxxxxxxx"],
        "UserData": "IyEvYmluL2Jhc2gKZWNobyAiSGVsbG8gV29ybGQi"
    }'
```

## Auto Scaling 그룹

### 그룹 생성

```bash
aws autoscaling create-auto-scaling-group \
    --auto-scaling-group-name my-asg \
    --launch-template LaunchTemplateId=lt-xxxxxxxxx,Version='$Latest' \
    --min-size 1 \
    --max-size 5 \
    --desired-capacity 2 \
    --vpc-zone-identifier "subnet-xxx,subnet-yyy" \
    --health-check-type ELB \
    --health-check-grace-period 300 \
    --tags Key=Name,Value=ASG-Instance,PropagateAtLaunch=true
```

### 용량 설정

| 설정 | 설명 |
|------|------|
| **최소 용량** | 항상 유지할 최소 인스턴스 수 |
| **최대 용량** | 허용되는 최대 인스턴스 수 |
| **희망 용량** | 현재 유지하려는 인스턴스 수 |

```
최소 용량 ≤ 희망 용량 ≤ 최대 용량
```

### 상태 검사 유형

| 유형 | 설명 |
|------|------|
| **EC2** | EC2 상태 검사만 사용 |
| **ELB** | ELB 상태 검사도 포함 |
| **VPC Lattice** | VPC Lattice 상태 검사 포함 |

## 스케일링 정책

### 1. 대상 추적 스케일링 (권장)

지정된 대상 값을 유지하도록 자동 조정합니다.

```bash
aws autoscaling put-scaling-policy \
    --auto-scaling-group-name my-asg \
    --policy-name cpu-target-tracking \
    --policy-type TargetTrackingScaling \
    --target-tracking-configuration '{
        "PredefinedMetricSpecification": {
            "PredefinedMetricType": "ASGAverageCPUUtilization"
        },
        "TargetValue": 50.0
    }'
```

**사전 정의 지표:**
- `ASGAverageCPUUtilization`: 평균 CPU 사용률
- `ASGAverageNetworkIn`: 평균 네트워크 수신
- `ASGAverageNetworkOut`: 평균 네트워크 송신
- `ALBRequestCountPerTarget`: 대상당 요청 수

### 2. 단계 조정 스케일링

지표 값에 따라 다른 조정량을 적용합니다.

```bash
aws autoscaling put-scaling-policy \
    --auto-scaling-group-name my-asg \
    --policy-name step-scaling-policy \
    --policy-type StepScaling \
    --adjustment-type ChangeInCapacity \
    --step-adjustments '[
        {"MetricIntervalLowerBound": 0, "MetricIntervalUpperBound": 10, "ScalingAdjustment": 1},
        {"MetricIntervalLowerBound": 10, "MetricIntervalUpperBound": 20, "ScalingAdjustment": 2},
        {"MetricIntervalLowerBound": 20, "ScalingAdjustment": 3}
    ]'
```

### 3. 단순 조정 스케일링

단일 조정 값을 적용합니다.

```bash
aws autoscaling put-scaling-policy \
    --auto-scaling-group-name my-asg \
    --policy-name simple-scaling-policy \
    --policy-type SimpleScaling \
    --adjustment-type ChangeInCapacity \
    --scaling-adjustment 1 \
    --cooldown 300
```

### 4. 예측 스케일링

ML을 사용하여 트래픽 패턴을 예측합니다.

```bash
aws autoscaling put-scaling-policy \
    --auto-scaling-group-name my-asg \
    --policy-name predictive-scaling-policy \
    --policy-type PredictiveScaling \
    --predictive-scaling-configuration '{
        "MetricSpecifications": [{
            "TargetValue": 50.0,
            "PredefinedMetricPairSpecification": {
                "PredefinedMetricType": "ASGCPUUtilization"
            }
        }],
        "Mode": "ForecastAndScale",
        "SchedulingBufferTime": 300
    }'
```

## 예약된 스케일링

특정 시간에 용량을 조정합니다.

```bash
# 매일 오전 9시에 확장
aws autoscaling put-scheduled-update-group-action \
    --auto-scaling-group-name my-asg \
    --scheduled-action-name scale-out-morning \
    --recurrence "0 9 * * *" \
    --min-size 5 \
    --max-size 10 \
    --desired-capacity 5

# 매일 오후 6시에 축소
aws autoscaling put-scheduled-update-group-action \
    --auto-scaling-group-name my-asg \
    --scheduled-action-name scale-in-evening \
    --recurrence "0 18 * * *" \
    --min-size 2 \
    --max-size 5 \
    --desired-capacity 2
```

## 수명 주기 후크

인스턴스 시작/종료 시 사용자 지정 작업을 수행합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    수명 주기 후크                            │
│                                                              │
│  시작 시:                                                    │
│  Pending ──▶ Pending:Wait ──▶ Pending:Proceed ──▶ InService │
│                   │                                          │
│                   └── 사용자 지정 작업                        │
│                       (소프트웨어 설치, 설정 등)              │
│                                                              │
│  종료 시:                                                    │
│  InService ──▶ Terminating:Wait ──▶ Terminating:Proceed     │
│                      │                                       │
│                      └── 사용자 지정 작업                     │
│                          (로그 저장, 연결 드레인 등)          │
└─────────────────────────────────────────────────────────────┘
```

```bash
aws autoscaling put-lifecycle-hook \
    --auto-scaling-group-name my-asg \
    --lifecycle-hook-name my-launch-hook \
    --lifecycle-transition autoscaling:EC2_INSTANCE_LAUNCHING \
    --heartbeat-timeout 300 \
    --default-result CONTINUE
```

## 인스턴스 혼합

Spot과 On-Demand 인스턴스를 혼합 사용합니다.

```bash
aws autoscaling create-auto-scaling-group \
    --auto-scaling-group-name my-mixed-asg \
    --mixed-instances-policy '{
        "LaunchTemplate": {
            "LaunchTemplateSpecification": {
                "LaunchTemplateId": "lt-xxxxxxxxx",
                "Version": "$Latest"
            },
            "Overrides": [
                {"InstanceType": "m5.large"},
                {"InstanceType": "m5a.large"},
                {"InstanceType": "m4.large"}
            ]
        },
        "InstancesDistribution": {
            "OnDemandBaseCapacity": 1,
            "OnDemandPercentageAboveBaseCapacity": 25,
            "SpotAllocationStrategy": "capacity-optimized"
        }
    }' \
    --min-size 2 \
    --max-size 10 \
    --vpc-zone-identifier "subnet-xxx,subnet-yyy"
```

## 인스턴스 워밍업

새 인스턴스가 준비될 때까지 스케일링 지표에서 제외합니다.

```bash
aws autoscaling update-auto-scaling-group \
    --auto-scaling-group-name my-asg \
    --default-instance-warmup 300
```

## 종료 정책

축소 시 어떤 인스턴스를 먼저 종료할지 결정합니다.

| 정책 | 설명 |
|------|------|
| `Default` | 기본 종료 정책 |
| `OldestInstance` | 가장 오래된 인스턴스 |
| `NewestInstance` | 가장 최근 인스턴스 |
| `OldestLaunchConfiguration` | 가장 오래된 시작 구성 |
| `OldestLaunchTemplate` | 가장 오래된 시작 템플릿 |
| `ClosestToNextInstanceHour` | 다음 결제 시간에 가장 가까운 인스턴스 |
| `AllocationStrategy` | 할당 전략 기반 |

```bash
aws autoscaling update-auto-scaling-group \
    --auto-scaling-group-name my-asg \
    --termination-policies "OldestInstance" "Default"
```

## 모니터링

### CloudWatch 지표

| 지표 | 설명 |
|------|------|
| `GroupMinSize` | 최소 그룹 크기 |
| `GroupMaxSize` | 최대 그룹 크기 |
| `GroupDesiredCapacity` | 희망 용량 |
| `GroupInServiceInstances` | 서비스 중인 인스턴스 수 |
| `GroupPendingInstances` | 대기 중인 인스턴스 수 |
| `GroupTerminatingInstances` | 종료 중인 인스턴스 수 |

### 활동 기록 확인

```bash
aws autoscaling describe-scaling-activities \
    --auto-scaling-group-name my-asg
```

## 모범 사례

1. **시작 템플릿 사용**: 시작 구성 대신 시작 템플릿 사용
2. **다중 가용 영역**: 고가용성을 위해 여러 AZ에 배포
3. **적절한 상태 검사**: ELB 사용 시 ELB 상태 검사 활성화
4. **워밍업 시간 설정**: 애플리케이션 초기화 시간 고려
5. **대상 추적 스케일링**: 가장 쉽고 효과적인 스케일링 방법
6. **인스턴스 혼합**: Spot 인스턴스로 비용 절감

## 참고 자료

- [EC2 Auto Scaling 사용자 가이드](https://docs.aws.amazon.com/autoscaling/ec2/userguide/)
- [시작 템플릿](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/launch-templates.html)
- [스케일링 정책](https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-scaling-target-tracking.html)
