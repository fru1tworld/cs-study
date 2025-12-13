# Elastic Network Interface (ENI)

## 개요

ENI(Elastic Network Interface)는 VPC에서 가상 네트워크 카드를 나타내는 논리적 네트워킹 구성 요소입니다. EC2 인스턴스에 연결하여 네트워크 연결을 제공합니다.

## ENI 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                   Elastic Network Interface                  │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  주 프라이빗 IPv4 주소 (필수)                         │   │
│  │  예: 10.0.1.100                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  보조 프라이빗 IPv4 주소 (선택, 여러 개 가능)         │   │
│  │  예: 10.0.1.101, 10.0.1.102                          │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Elastic IP 주소 (선택)                              │   │
│  │  예: 52.xx.xx.xx                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  퍼블릭 IPv4 주소 (선택)                             │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  IPv6 주소 (선택)                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  하나 이상의 보안 그룹                               │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  MAC 주소                                            │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  원본/대상 확인 플래그                               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## ENI 유형

### 기본 네트워크 인터페이스

- 인스턴스 시작 시 자동 생성
- 인스턴스에서 분리 불가
- 인스턴스 종료 시 함께 삭제

### 보조 네트워크 인터페이스

- 별도로 생성하여 인스턴스에 연결
- 인스턴스 간 이동 가능
- 장애 조치에 활용

## ENI 관리

### ENI 생성

```bash
# ENI 생성
aws ec2 create-network-interface \
    --subnet-id subnet-xxxxxxxxx \
    --description "My secondary ENI" \
    --groups sg-xxxxxxxxx \
    --private-ip-address 10.0.1.100
```

### 인스턴스에 연결

```bash
# ENI를 인스턴스에 연결
aws ec2 attach-network-interface \
    --network-interface-id eni-xxxxxxxxx \
    --instance-id i-xxxxxxxxx \
    --device-index 1
```

### 인스턴스에서 분리

```bash
# ENI를 인스턴스에서 분리
aws ec2 detach-network-interface \
    --attachment-id eni-attach-xxxxxxxxx
```

### ENI 삭제

```bash
# ENI 삭제 (분리된 상태에서만 가능)
aws ec2 delete-network-interface \
    --network-interface-id eni-xxxxxxxxx
```

## 다중 ENI 아키텍처

### 사용 사례

```
┌─────────────────────────────────────────────────────────────┐
│                      EC2 인스턴스                            │
│  ┌─────────────────┐         ┌─────────────────┐           │
│  │ eth0 (기본)     │         │ eth1 (보조)     │           │
│  │ 관리 트래픽     │         │ 데이터 트래픽    │           │
│  │ 10.0.1.100     │         │ 10.0.2.100     │           │
│  └────────┬────────┘         └────────┬────────┘           │
└───────────┼──────────────────────────┼─────────────────────┘
            │                          │
            ▼                          ▼
     ┌─────────────┐            ┌─────────────┐
     │ 관리 서브넷  │            │ 데이터 서브넷 │
     │ 10.0.1.0/24 │            │ 10.0.2.0/24 │
     └─────────────┘            └─────────────┘
```

### 일반적인 사용 사례

| 사용 사례 | 설명 |
|-----------|------|
| ** 관리 네트워크** | 관리 트래픽과 데이터 트래픽 분리 |
| ** 네트워크 어플라이언스** | 방화벽, 로드 밸런서 |
| ** 다중 홈 인스턴스** | 여러 서브넷에 접근 |
| ** 고가용성** | 장애 조치 시 ENI 이동 |
| ** 라이선스 관리** | MAC 주소 기반 라이선스 |

## ENI와 장애 조치

### 장애 조치 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│  정상 상태                                                   │
│  ┌───────────────┐                                          │
│  │   Primary     │ ←── ENI (10.0.1.100)                    │
│  │   Instance    │                                          │
│  └───────────────┘                                          │
│                                                              │
│  장애 발생 후                                                │
│  ┌───────────────┐     ┌───────────────┐                   │
│  │   Primary     │     │   Standby     │ ←── ENI 이동      │
│  │   (장애)      │     │   Instance    │     (10.0.1.100)  │
│  └───────────────┘     └───────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### 장애 조치 스크립트 예시

```bash
#!/bin/bash
# ENI를 새 인스턴스로 이동

ENI_ID="eni-xxxxxxxxx"
NEW_INSTANCE_ID="i-yyyyyyyyy"
DEVICE_INDEX=1

# 현재 연결 해제
ATTACHMENT_ID=$(aws ec2 describe-network-interfaces \
    --network-interface-ids $ENI_ID \
    --query 'NetworkInterfaces[0].Attachment.AttachmentId' \
    --output text)

aws ec2 detach-network-interface \
    --attachment-id $ATTACHMENT_ID \
    --force

sleep 10

# 새 인스턴스에 연결
aws ec2 attach-network-interface \
    --network-interface-id $ENI_ID \
    --instance-id $NEW_INSTANCE_ID \
    --device-index $DEVICE_INDEX
```

## 원본/대상 확인

### 개념

기본적으로 EC2 인스턴스는 자신이 원본 또는 대상이 아닌 트래픽을 삭제합니다. NAT 인스턴스나 라우터로 사용하려면 이 기능을 비활성화해야 합니다.

### 비활성화

```bash
# 원본/대상 확인 비활성화
aws ec2 modify-network-interface-attribute \
    --network-interface-id eni-xxxxxxxxx \
    --no-source-dest-check
```

## ENI 한도

인스턴스 타입에 따라 연결 가능한 ENI 수와 ENI당 IP 주소 수가 다릅니다.

| 인스턴스 타입 | 최대 ENI | ENI당 IPv4 |
|--------------|----------|------------|
| t2.micro | 2 | 2 |
| t3.medium | 3 | 6 |
| m5.large | 3 | 10 |
| m5.xlarge | 4 | 15 |
| c5.18xlarge | 15 | 50 |

### 한도 확인

```bash
# 인스턴스 타입별 네트워크 한도 확인
aws ec2 describe-instance-types \
    --instance-types m5.large \
    --query 'InstanceTypes[0].NetworkInfo'
```

## 향상된 네트워킹

### ENA (Elastic Network Adapter)

- 최대 100Gbps 네트워크 대역폭
- 높은 PPS (Packets Per Second)
- 낮은 지연 시간

```bash
# ENA 지원 확인
aws ec2 describe-instances \
    --instance-ids i-xxxxxxxxx \
    --query 'Reservations[0].Instances[0].EnaSupport'
```

### EFA (Elastic Fabric Adapter)

- HPC 및 ML 워크로드용
- OS-bypass 지원
- 클러스터 배치 그룹과 함께 사용

## 모범 사례

1. ** 보안 그룹 분리**: 역할별로 다른 ENI에 다른 보안 그룹 적용
2. ** 서브넷 분리**: 관리/데이터 트래픽을 다른 서브넷으로 분리
3. **ENI 태깅**: 용도별로 명확한 태그 지정
4. ** 장애 조치 계획**: 중요 워크로드에 ENI 기반 장애 조치 구성
5. ** 한도 확인**: 인스턴스 타입의 ENI 한도 사전 확인

## 참고 자료

- [Elastic Network Interfaces](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-eni.html)
- [향상된 네트워킹](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/enhanced-networking.html)
- [EFA](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/efa.html)
