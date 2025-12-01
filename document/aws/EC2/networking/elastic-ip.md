# Elastic IP (EIP)

## 개요

Elastic IP는 동적 클라우드 컴퓨팅을 위해 설계된 정적 IPv4 주소입니다. AWS 계정에 할당되며, 명시적으로 해제할 때까지 유지됩니다.

## 특징

| 특성 | 설명 |
|------|------|
| **유형** | 정적 퍼블릭 IPv4 주소 |
| **범위** | 리전별 |
| **이동성** | 인스턴스 간 이동 가능 |
| **지속성** | 계정에서 해제할 때까지 유지 |
| **IPv6** | 미지원 (IPv6는 자동으로 정적) |

## Elastic IP vs 퍼블릭 IP

| 특성 | Elastic IP | 퍼블릭 IP |
|------|------------|-----------|
| 지속성 | 명시적 해제 전까지 유지 | 인스턴스 중지 시 변경 |
| 비용 | 유휴 시 요금 | 무료 (실행 중) |
| 이동성 | 인스턴스 간 이동 가능 | 불가능 |
| 할당 | 명시적 할당 | 자동 할당 |

## Elastic IP 관리

### 할당

```bash
# EIP 할당 (Amazon 풀에서)
aws ec2 allocate-address --domain vpc

# 특정 풀에서 할당
aws ec2 allocate-address \
    --domain vpc \
    --address 203.0.113.25 \
    --customer-owned-pool-id ipv4pool-coip-xxx
```

### 인스턴스에 연결

```bash
# EIP를 인스턴스에 연결
aws ec2 associate-address \
    --instance-id i-xxxxxxxxx \
    --allocation-id eipalloc-xxxxxxxxx

# 또는 ENI에 연결
aws ec2 associate-address \
    --network-interface-id eni-xxxxxxxxx \
    --allocation-id eipalloc-xxxxxxxxx \
    --private-ip-address 10.0.1.100
```

### 연결 해제

```bash
# EIP 연결 해제
aws ec2 disassociate-address \
    --association-id eipassoc-xxxxxxxxx
```

### 해제

```bash
# EIP 해제 (계정에서 제거)
aws ec2 release-address --allocation-id eipalloc-xxxxxxxxx
```

## 요금

### 요금 정책 (2024년 2월~)

| 상태 | 요금 |
|------|------|
| 실행 중인 인스턴스에 연결 | 시간당 요금 |
| 중지된 인스턴스에 연결 | 시간당 요금 |
| 연결되지 않음 | 시간당 요금 |
| 추가 EIP (인스턴스당 2개째부터) | 시간당 요금 |

> AWS는 2024년 2월부터 모든 퍼블릭 IPv4 주소에 대해 요금을 부과합니다.

### 비용 최적화

```bash
# 사용하지 않는 EIP 확인
aws ec2 describe-addresses \
    --query 'Addresses[?AssociationId==null]'
```

## 할당량

| 리소스 | 기본 할당량 |
|--------|------------|
| VPC당 EIP | 5 |

### 할당량 증가 요청

Service Quotas 콘솔에서 증가를 요청할 수 있습니다.

```bash
# 현재 할당량 확인
aws service-quotas get-service-quota \
    --service-code ec2 \
    --quota-code L-0263D0A3
```

## 고가용성을 위한 EIP 사용

### 장애 조치 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    정상 상태                                 │
│                                                              │
│     사용자 ───▶ EIP (52.xx.xx.xx) ───▶ Primary Instance     │
│                                                              │
└─────────────────────────────────────────────────────────────┘

                           │
                           ▼ 장애 발생

┌─────────────────────────────────────────────────────────────┐
│                    장애 조치 후                              │
│                                                              │
│     사용자 ───▶ EIP (52.xx.xx.xx) ───▶ Standby Instance     │
│                        ▲                                     │
│                        │                                     │
│                   EIP 재연결                                 │
└─────────────────────────────────────────────────────────────┘
```

### 장애 조치 스크립트

```bash
#!/bin/bash
# EIP를 새 인스턴스로 이동

EIP_ALLOCATION_ID="eipalloc-xxxxxxxxx"
NEW_INSTANCE_ID="i-yyyyyyyyy"

# 기존 연결 확인 및 해제
ASSOCIATION_ID=$(aws ec2 describe-addresses \
    --allocation-ids $EIP_ALLOCATION_ID \
    --query 'Addresses[0].AssociationId' \
    --output text)

if [ "$ASSOCIATION_ID" != "None" ]; then
    aws ec2 disassociate-address --association-id $ASSOCIATION_ID
fi

# 새 인스턴스에 연결
aws ec2 associate-address \
    --instance-id $NEW_INSTANCE_ID \
    --allocation-id $EIP_ALLOCATION_ID
```

## BYOIP (Bring Your Own IP)

자체 보유한 퍼블릭 IP 주소 범위를 AWS로 가져올 수 있습니다.

### 요구 사항

- ROA (Route Origin Authorization) 생성
- RDAP에 등록된 IP 주소 범위
- /24 이상의 IPv4 주소 범위

### 프로세스

```bash
# 1. IP 주소 범위 프로비저닝
aws ec2 provision-byoip-cidr \
    --cidr 203.0.113.0/24 \
    --cidr-authorization-context Message="...",Signature="..."

# 2. IP 주소 광고 시작
aws ec2 advertise-byoip-cidr --cidr 203.0.113.0/24

# 3. EIP로 사용
aws ec2 allocate-address \
    --domain vpc \
    --address 203.0.113.25
```

## DNS 매핑

### 퍼블릭 DNS 호스트 이름

EIP가 연결된 인스턴스는 퍼블릭 DNS 호스트 이름을 받습니다:
```
ec2-52-xx-xx-xx.ap-northeast-2.compute.amazonaws.com
```

### Route 53 연동

```bash
# Route 53 레코드 생성
aws route53 change-resource-record-sets \
    --hosted-zone-id Z123456789 \
    --change-batch '{
        "Changes": [{
            "Action": "UPSERT",
            "ResourceRecordSet": {
                "Name": "app.example.com",
                "Type": "A",
                "TTL": 300,
                "ResourceRecords": [{"Value": "52.xx.xx.xx"}]
            }
        }]
    }'
```

## 모범 사례

### 1. 필요한 경우에만 사용

- DNS 기반 서비스 검색이나 로드 밸런서 사용 고려
- 비용 절감을 위해 불필요한 EIP 해제

### 2. 자동화된 장애 조치 구현

```python
import boto3

def failover_eip(allocation_id, new_instance_id):
    ec2 = boto3.client('ec2')

    # 기존 연결 해제
    addresses = ec2.describe_addresses(AllocationIds=[allocation_id])
    if addresses['Addresses'][0].get('AssociationId'):
        ec2.disassociate_address(
            AssociationId=addresses['Addresses'][0]['AssociationId']
        )

    # 새 인스턴스에 연결
    ec2.associate_address(
        InstanceId=new_instance_id,
        AllocationId=allocation_id
    )
```

### 3. 태깅

```bash
# EIP에 태그 추가
aws ec2 create-tags \
    --resources eipalloc-xxxxxxxxx \
    --tags Key=Name,Value=Production-Web Key=Environment,Value=Production
```

### 4. 모니터링

```bash
# CloudWatch에서 EIP 연결 상태 모니터링
# EventBridge 규칙으로 EIP 변경 감지
```

## 참고 자료

- [Elastic IP 주소](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html)
- [BYOIP](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-byoip.html)
- [퍼블릭 IPv4 주소 요금](https://aws.amazon.com/vpc/pricing/)
