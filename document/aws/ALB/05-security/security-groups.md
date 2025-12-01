# AWS ALB 보안 그룹

## 개요

보안 그룹은 ALB의 **가상 방화벽** 역할을 하며, 인바운드 및 아웃바운드 트래픽을 제어합니다. ALB와 타겟 모두에 적절한 보안 그룹 규칙이 필요합니다.

## ALB 보안 그룹 규칙

### 인바운드 규칙 (필수)

클라이언트 트래픽을 허용하는 규칙:

| 유형 | 프로토콜 | 포트 | 소스 | 설명 |
|------|---------|------|------|------|
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP 트래픽 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS 트래픽 |
| Custom TCP | TCP | 8080 | 10.0.0.0/8 | 내부 API (예시) |

### 아웃바운드 규칙 (필수)

타겟으로의 트래픽을 허용하는 규칙:

| 유형 | 프로토콜 | 포트 | 대상 | 설명 |
|------|---------|------|------|------|
| Custom TCP | TCP | 타겟 포트 | 타겟 보안 그룹 | 애플리케이션 트래픽 |
| Custom TCP | TCP | 헬스체크 포트 | 타겟 보안 그룹 | 헬스 체크 트래픽 |

## 타겟 보안 그룹 규칙

### 인바운드 규칙

ALB로부터의 트래픽만 허용:

| 유형 | 프로토콜 | 포트 | 소스 | 설명 |
|------|---------|------|------|------|
| Custom TCP | TCP | 애플리케이션 포트 | ALB 보안 그룹 | ALB에서 오는 트래픽 |
| Custom TCP | TCP | 헬스체크 포트 | ALB 보안 그룹 | 헬스 체크 트래픽 |

### 보안 그룹 참조 방식

IP 주소 대신 **보안 그룹 ID를 소스로 사용**하는 것이 권장됩니다:

```yaml
인바운드 규칙:
  - Type: Custom TCP
    Protocol: TCP
    Port: 80
    Source: sg-alb-security-group  # 보안 그룹 참조
```

**장점:**
- ALB IP 변경에 자동 대응
- 관리 용이성
- 보안 강화

## 구성 예시

### Internet-facing ALB 구성

```yaml
# ALB 보안 그룹 (sg-alb)
인바운드:
  - HTTP (80): 0.0.0.0/0
  - HTTPS (443): 0.0.0.0/0

아웃바운드:
  - HTTP (80): sg-target
  - Custom (8080): sg-target

# 타겟 보안 그룹 (sg-target)
인바운드:
  - HTTP (80): sg-alb
  - Custom (8080): sg-alb

아웃바운드:
  - All traffic: 0.0.0.0/0  # 또는 필요한 트래픽만
```

### Internal ALB 구성

```yaml
# ALB 보안 그룹 (sg-internal-alb)
인바운드:
  - HTTP (80): 10.0.0.0/16  # VPC CIDR
  - HTTPS (443): 10.0.0.0/16

아웃바운드:
  - HTTP (80): sg-internal-target

# 타겟 보안 그룹 (sg-internal-target)
인바운드:
  - HTTP (80): sg-internal-alb

아웃바운드:
  - All traffic: 0.0.0.0/0
```

## AWS CLI 예시

### ALB 보안 그룹 생성

```bash
# 보안 그룹 생성
aws ec2 create-security-group \
    --group-name alb-sg \
    --description "Security group for ALB" \
    --vpc-id vpc-12345678

# 인바운드 규칙 추가
aws ec2 authorize-security-group-ingress \
    --group-id sg-alb \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-id sg-alb \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0
```

### 타겟 보안 그룹에 ALB 참조 추가

```bash
# ALB 보안 그룹을 소스로 사용
aws ec2 authorize-security-group-ingress \
    --group-id sg-target \
    --protocol tcp \
    --port 80 \
    --source-group sg-alb
```

### ALB에 보안 그룹 연결

```bash
aws elbv2 set-security-groups \
    --load-balancer-arn <alb-arn> \
    --security-groups sg-alb sg-additional
```

## 일반적인 문제

### 1. 헬스 체크 실패

**증상:** 타겟이 계속 unhealthy 상태

**원인:** 타겟 보안 그룹에서 헬스 체크 포트 차단

**해결:**
```yaml
타겟 보안 그룹 인바운드:
  - Type: Custom TCP
    Port: <헬스체크 포트>
    Source: sg-alb
```

### 2. 연결 타임아웃

**증상:** 클라이언트가 ALB에 연결 불가

**원인:** ALB 보안 그룹에서 리스너 포트 차단

**해결:**
```yaml
ALB 보안 그룹 인바운드:
  - Type: HTTPS
    Port: 443
    Source: 0.0.0.0/0
```

### 3. 간헐적 연결 실패

**증상:** 일부 요청만 실패

**원인:** ALB 아웃바운드 규칙에서 타겟 포트 차단

**해결:**
```yaml
ALB 보안 그룹 아웃바운드:
  - Type: Custom TCP
    Port: <타겟 포트>
    Destination: sg-target
```

## 보안 모범 사례

### 1. 최소 권한 원칙

```yaml
권장:
  - 필요한 포트만 허용
  - 가능하면 소스 IP 범위 제한
  - 0.0.0.0/0 대신 특정 CIDR 사용

비권장:
  - 모든 포트 허용 (0-65535)
  - 모든 프로토콜 허용
  - 불필요한 아웃바운드 허용
```

### 2. 보안 그룹 참조 사용

```yaml
권장:
  Source: sg-alb  # 보안 그룹 ID 참조

비권장:
  Source: 10.0.1.0/24  # IP 범위 직접 지정
```

### 3. 별도 보안 그룹 사용

```yaml
구성:
  - ALB 전용 보안 그룹
  - 타겟 전용 보안 그룹
  - 데이터베이스 전용 보안 그룹
```

### 4. 정기적인 감사

```bash
# 사용되지 않는 규칙 확인
aws ec2 describe-security-groups \
    --group-ids sg-alb \
    --query 'SecurityGroups[*].IpPermissions'
```

## Lambda 타겟 참고사항

Lambda 함수를 타겟으로 사용할 때:

```yaml
특징:
  - ALB가 Lambda를 직접 호출 (네트워크 연결 아님)
  - ALB 보안 그룹의 아웃바운드 규칙 불필요
  - Lambda 리소스 기반 정책으로 권한 제어
```

## 참고 자료

- [AWS 공식 문서 - 보안 그룹](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-update-security-groups.html)
- [AWS 보안 모범 사례](https://docs.aws.amazon.com/vpc/latest/userguide/security-group-rules-recommendations.html)
