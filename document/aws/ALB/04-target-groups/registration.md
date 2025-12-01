# AWS ALB 타겟 등록 및 해제

## 타겟 등록

### 등록 요구사항

** 인스턴스 타겟:**
```yaml
조건:
  - 인스턴스 상태: running
  - VPC: 타겟 그룹과 동일한 VPC
  - 네트워크 인터페이스: 프라이머리 ENI 사용
```

**IP 주소 타겟:**
```yaml
허용 CIDR:
  - 타겟 그룹 VPC의 서브넷
  - 10.0.0.0/8 (RFC 1918)
  - 172.16.0.0/12 (RFC 1918)
  - 192.168.0.0/16 (RFC 1918)
  - 100.64.0.0/10 (RFC 6598)
```

**Lambda 함수 타겟:**
```yaml
조건:
  - 동일 계정 및 리전
  - ALB 호출 권한 필요
  - 타겟 그룹당 1개만 등록 가능
```

### 동일 인스턴스 다중 포트 등록

하나의 인스턴스를 여러 포트로 등록할 수 있습니다:

```bash
aws elbv2 register-targets \
    --target-group-arn <arn> \
    --targets \
        Id=i-1234567890abcdef0,Port=80 \
        Id=i-1234567890abcdef0,Port=8080
```

### 타겟 등록 방법

**AWS 콘솔:**
1. 타겟 그룹 선택 → **Targets** 탭
2. **Register targets** 클릭
3. 사용 가능한 인스턴스/IP 선택
4. 포트 지정 후 **Include as pending below**
5. **Register pending targets** 클릭

**AWS CLI - 인스턴스:**
```bash
aws elbv2 register-targets \
    --target-group-arn <arn> \
    --targets Id=i-1234567890abcdef0 Id=i-0987654321fedcba0
```

**AWS CLI - IP 주소:**
```bash
aws elbv2 register-targets \
    --target-group-arn <arn> \
    --targets Id=10.0.1.100 Id=10.0.2.200
```

**AWS CLI - Lambda 함수:**
```bash
# Lambda 권한 추가 (필수)
aws lambda add-permission \
    --function-name my-function \
    --statement-id elb-permission \
    --action lambda:InvokeFunction \
    --principal elasticloadbalancing.amazonaws.com \
    --source-arn <target-group-arn>

# 타겟 등록
aws elbv2 register-targets \
    --target-group-arn <arn> \
    --targets Id=arn:aws:lambda:us-east-1:123456789012:function:my-function
```

## 타겟 해제 (Deregistration)

### 연결 드레이닝 (Connection Draining)

타겟 해제 시 ** 진행 중인 요청이 완료될 때까지 대기** 합니다.

```yaml
동작:
  1. 타겟을 "draining" 상태로 전환
  2. 새 요청은 다른 타겟으로 라우팅
  3. 기존 연결은 deregistration_delay까지 유지
  4. 타임아웃 후 강제 종료
```

### 해제 지연 시간 (Deregistration Delay)

| 설정 | 기본값 | 범위 |
|------|-------|------|
| `deregistration_delay.timeout_seconds` | 300초 | 0-3600초 |

** 설정 변경:**
```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes Key=deregistration_delay.timeout_seconds,Value=60
```

### 타겟 해제 방법

**AWS 콘솔:**
1. 타겟 그룹 선택 → **Targets** 탭
2. 해제할 타겟 선택
3. **Deregister** 클릭

**AWS CLI:**
```bash
aws elbv2 deregister-targets \
    --target-group-arn <arn> \
    --targets Id=i-1234567890abcdef0
```

### 해제 상태 모니터링

```bash
aws elbv2 describe-target-health \
    --target-group-arn <arn>
```

**draining 상태 응답:**
```json
{
    "TargetHealth": {
        "State": "draining",
        "Reason": "Target.DeregistrationInProgress",
        "Description": "Target is in the process of being deregistered"
    }
}
```

## Slow Start 모드

새로 등록된 타겟에 ** 점진적으로 트래픽을 증가** 시킵니다.

### 설정

| 설정 | 기본값 | 범위 |
|------|-------|------|
| `slow_start.duration_seconds` | 0 (비활성화) | 30-900초 |

** 활성화:**
```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes Key=slow_start.duration_seconds,Value=60
```

### 동작 방식

```yaml
1. 타겟 등록 및 첫 헬스 체크 통과
2. Slow Start 기간 시작
3. 트래픽이 0%에서 점진적으로 증가
4. 기간 종료 후 정상 라우팅
```

### 제한사항
- Round Robin 알고리즘에서만 동작
- 이미 healthy 상태인 타겟에는 적용 안 됨
- 기간 중 unhealthy가 되면 Slow Start 중단

## Auto Scaling 통합

### Auto Scaling 그룹에 타겟 그룹 연결

```bash
aws autoscaling attach-load-balancer-target-groups \
    --auto-scaling-group-name my-asg \
    --target-group-arns <target-group-arn>
```

### 동작
- 새 인스턴스: 자동 등록
- 종료 인스턴스: 자동 해제 (연결 드레이닝 후)
- 스케일 인: Lifecycle Hook으로 드레이닝 제어 가능

## Target Optimizer

동시 요청 수를 제한하여 타겟 과부하를 방지합니다.

### 요구사항
- Docker 에이전트 설치 필요
- 타겟 그룹에서 Target Optimizer 활성화

### 설정

```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes \
        Key=target_optimizer.enabled,Value=true \
        Key=target_optimizer.max_concurrency,Value=100
```

**max_concurrency 범위:** 0-1000

## 가용 영역별 타겟 배치

### 권장 사항
- 각 활성화된 가용 영역에 ** 최소 1개 이상** 의 타겟 등록
- 균등 분포로 크로스 존 트래픽 최소화

### 예시
```yaml
가용 영역 설정:
  us-east-1a: subnet-aaa (활성화)
  us-east-1b: subnet-bbb (활성화)

타겟 배치:
  - i-111 (us-east-1a): 80
  - i-222 (us-east-1b): 80
```

## 참고 자료

- [AWS 공식 문서 - 타겟 등록](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/target-group-register-targets.html)
