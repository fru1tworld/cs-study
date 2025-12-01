# EC2 요금

## 개요

Amazon EC2는 사용한 만큼만 지불하는 종량제 요금 모델을 제공합니다. 다양한 구매 옵션을 통해 워크로드에 맞는 비용 최적화가 가능합니다.

## 구매 옵션 비교

| 옵션 | 할인율 | 약정 | 유연성 | 사용 사례 |
|------|--------|------|--------|----------|
| **On-Demand** | 0% | 없음 | 최고 | 단기, 예측 불가 |
| **Savings Plans** | 최대 72% | 1-3년 | 높음 | 지속적 워크로드 |
| **Reserved** | 최대 72% | 1-3년 | 중간 | 안정적 워크로드 |
| **Spot** | 최대 90% | 없음 | 낮음 | 유연한 워크로드 |
| **Dedicated Hosts** | 다양 | 없음/예약 | 낮음 | 규정 준수, 라이선스 |

```
┌─────────────────────────────────────────────────────────────┐
│                    구매 옵션 선택 가이드                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   워크로드 특성                                              │
│        │                                                     │
│        ├── 단기/테스트/예측 불가 ──▶ On-Demand              │
│        │                                                     │
│        ├── 지속적, 유연성 필요 ──▶ Compute Savings Plans    │
│        │                                                     │
│        ├── 안정적, 특정 인스턴스 ──▶ Reserved Instances     │
│        │                                                     │
│        ├── 중단 허용, 비용 중요 ──▶ Spot Instances          │
│        │                                                     │
│        └── 규정 준수, 라이선스 ──▶ Dedicated Hosts          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## On-Demand 인스턴스

### 특징

- 초 단위 요금 청구 (최소 60초)
- 약정 없음
- 언제든 시작/중지 가능

### 요금 예시 (서울 리전, Linux)

| 인스턴스 타입 | vCPU | 메모리 | 시간당 요금 |
|--------------|------|--------|------------|
| t3.micro | 2 | 1 GiB | ~$0.013 |
| t3.small | 2 | 2 GiB | ~$0.026 |
| m5.large | 2 | 8 GiB | ~$0.118 |
| c5.xlarge | 4 | 8 GiB | ~$0.196 |

### 적합한 워크로드

- 개발/테스트 환경
- 단기 프로젝트
- 예측 불가능한 워크로드
- 처음 사용하는 애플리케이션

## Savings Plans

### 유형

#### 1. Compute Savings Plans

| 특성 | 설명 |
|------|------|
| 적용 범위 | EC2, Fargate, Lambda |
| 유연성 | 리전, 인스턴스 패밀리, 크기, OS 변경 가능 |
| 할인율 | 최대 66% |

#### 2. EC2 Instance Savings Plans

| 특성 | 설명 |
|------|------|
| 적용 범위 | 특정 리전의 EC2만 |
| 유연성 | 인스턴스 크기, OS, 테넌시 변경 가능 |
| 할인율 | 최대 72% |

### 구매

```bash
# Savings Plans 권장 사항 확인
aws ce get-savings-plans-purchase-recommendation \
    --savings-plans-type COMPUTE_SP \
    --lookback-period-in-days SIXTY_DAYS \
    --term-in-years ONE_YEAR \
    --payment-option NO_UPFRONT
```

### 결제 옵션

| 옵션 | 선결제 | 할인율 |
|------|--------|--------|
| **전액 선결제** | 100% | 최고 |
| **부분 선결제** | 50% | 중간 |
| **선결제 없음** | 0% | 최저 |

## Reserved Instances

### 특징

- 특정 인스턴스 구성에 대한 약정
- 1년 또는 3년 약정
- 용량 예약 옵션 (특정 AZ)

### 유형

#### Standard Reserved Instances

- 최대 72% 할인
- 인스턴스 타입 변경 불가

#### Convertible Reserved Instances

- 최대 66% 할인
- 다른 인스턴스 타입으로 교환 가능

### 적용 범위

| 범위 | 설명 | 용량 예약 |
|------|------|----------|
| **리전** | 리전 내 모든 AZ에 적용 | 없음 |
| **영역** | 특정 AZ에만 적용 | 있음 |

### 구매

```bash
# 예약 인스턴스 제안 확인
aws ce get-reservation-purchase-recommendation \
    --service "Amazon Elastic Compute Cloud - Compute" \
    --lookback-period-in-days SIXTY_DAYS
```

## Spot 인스턴스

### 특징

- 여유 EC2 용량 활용
- On-Demand 대비 최대 90% 할인
- AWS가 필요 시 2분 전 경고 후 회수

### 중단 처리

```
┌─────────────────────────────────────────────────────────────┐
│                    Spot 중단 프로세스                        │
│                                                              │
│  용량 회수 결정 ──▶ 2분 경고 ──▶ 인스턴스 중단               │
│                        │                                     │
│                        ▼                                     │
│               인스턴스 메타데이터에서 감지                     │
│               169.254.169.254/latest/meta-data/              │
│               spot/instance-action                           │
│                        │                                     │
│                        ▼                                     │
│               애플리케이션 정상 종료 처리                      │
└─────────────────────────────────────────────────────────────┘
```

### 요청 유형

| 유형 | 설명 |
|------|------|
| **일회성** | 요청 충족 후 종료 |
| **영구** | 인스턴스 중단/종료 시 자동 재요청 |

### Spot Fleet

여러 Spot 인스턴스 풀을 조합하여 용량을 확보합니다.

```bash
aws ec2 request-spot-fleet \
    --spot-fleet-request-config '{
        "TargetCapacity": 10,
        "IamFleetRole": "arn:aws:iam::xxx:role/aws-ec2-spot-fleet-role",
        "LaunchSpecifications": [
            {
                "ImageId": "ami-xxxxxxxxx",
                "InstanceType": "m5.large",
                "SubnetId": "subnet-xxxxxxxxx"
            },
            {
                "ImageId": "ami-xxxxxxxxx",
                "InstanceType": "m5a.large",
                "SubnetId": "subnet-xxxxxxxxx"
            }
        ],
        "AllocationStrategy": "capacityOptimized"
    }'
```

### 할당 전략

| 전략 | 설명 |
|------|------|
| `capacityOptimized` | 가용 용량이 가장 많은 풀 (권장) |
| `lowestPrice` | 가장 저렴한 풀 |
| `diversified` | 모든 풀에 분산 |
| `priceCapacityOptimized` | 용량과 가격 균형 |

### 적합한 워크로드

- 배치 처리
- 빅데이터 분석
- CI/CD
- 웹 서버 (상태 비저장)
- 테스트/개발

## Dedicated Hosts

### 특징

- 물리 서버 전용 사용
- 기존 서버 라이선스 활용 (BYOL)
- 규정 준수 요구사항 충족

### 요금 옵션

| 옵션 | 설명 |
|------|------|
| **On-Demand** | 시간 단위 청구 |
| **Reservation** | 최대 70% 할인 |
| **Savings Plans** | 적용 가능 |

## Dedicated Instances

- 단일 고객 전용 하드웨어
- Dedicated Host보다 유연
- 소켓/코어 가시성 없음

## 추가 요금 요소

### 데이터 전송

| 전송 유형 | 요금 |
|-----------|------|
| 인터넷 → EC2 | 무료 |
| EC2 → 인터넷 | 유료 (계층적) |
| 같은 AZ 내 | 무료 |
| 다른 AZ 간 | 유료 |
| 다른 리전 간 | 유료 |

### 퍼블릭 IPv4 주소

2024년 2월부터 모든 퍼블릭 IPv4 주소에 요금이 부과됩니다.

| 상태 | 시간당 요금 |
|------|------------|
| 실행 중인 인스턴스에 연결 | $0.005 |
| 유휴 EIP | $0.005 |

### EBS 스토리지

| 볼륨 타입 | GB당 월 요금 |
|-----------|-------------|
| gp3 | ~$0.096 |
| gp2 | ~$0.12 |
| io2 | ~$0.15 |
| st1 | ~$0.054 |
| sc1 | ~$0.018 |

## 비용 최적화 도구

### AWS Cost Explorer

```bash
# 비용 및 사용량 확인
aws ce get-cost-and-usage \
    --time-period Start=2024-01-01,End=2024-01-31 \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --filter '{
        "Dimensions": {
            "Key": "SERVICE",
            "Values": ["Amazon Elastic Compute Cloud - Compute"]
        }
    }'
```

### AWS Compute Optimizer

인스턴스 우측 크기 조정 권장 사항을 제공합니다.

```bash
aws compute-optimizer get-ec2-instance-recommendations \
    --instance-arns arn:aws:ec2:ap-northeast-2:xxx:instance/i-xxxxxxxxx
```

### Trusted Advisor

비용 최적화 권장 사항을 제공합니다.

## 프리 티어

| 서비스 | 무료 사용량 | 기간 |
|--------|------------|------|
| EC2 t2.micro/t3.micro | 월 750시간 | 12개월 |

## 비용 최적화 전략

### 1. 우측 크기 조정

```
┌─────────────────────────────────────────────────────────────┐
│  CPU 사용률이 지속적으로 낮은 인스턴스                         │
│  m5.xlarge (4 vCPU) ──▶ m5.large (2 vCPU)                  │
│  월 비용 50% 절감                                            │
└─────────────────────────────────────────────────────────────┘
```

### 2. 구매 옵션 혼합

```
┌─────────────────────────────────────────────────────────────┐
│  기본 용량 (60%) ──▶ Reserved/Savings Plans                 │
│  가변 용량 (30%) ──▶ On-Demand                              │
│  유연한 작업 (10%) ──▶ Spot                                  │
└─────────────────────────────────────────────────────────────┘
```

### 3. 자동 종료 스케줄링

개발/테스트 환경을 업무 시간 외에 종료합니다.

### 4. 미사용 리소스 정리

- 중지된 인스턴스
- 분리된 EBS 볼륨
- 오래된 스냅샷
- 사용하지 않는 EIP

## 참고 자료

- [EC2 요금](https://aws.amazon.com/ec2/pricing/)
- [Savings Plans](https://aws.amazon.com/savingsplans/)
- [Spot 인스턴스](https://aws.amazon.com/ec2/spot/)
- [AWS Pricing Calculator](https://calculator.aws/)
