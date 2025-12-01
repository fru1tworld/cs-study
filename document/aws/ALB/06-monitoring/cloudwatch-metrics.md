# AWS ALB CloudWatch 메트릭

## 개요

Application Load Balancer는 자동으로 CloudWatch에 메트릭을 게시합니다. 이를 통해 ALB의 성능을 모니터링하고 알람을 설정할 수 있습니다.

## 네임스페이스 및 차원

### 네임스페이스
```
AWS/ApplicationELB
```

### 차원 (Dimensions)

| 차원 | 설명 |
|------|------|
| LoadBalancer | 특정 ALB 필터링 |
| TargetGroup | 특정 타겟 그룹 필터링 |
| AvailabilityZone | 특정 가용 영역 필터링 |

## 로드 밸런서 메트릭

### 연결 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `ActiveConnectionCount` | 활성 TCP 연결 수 | Sum |
| `NewConnectionCount` | 새로 생성된 TCP 연결 수 | Sum |
| `RejectedConnectionCount` | 거부된 연결 수 | Sum |

### 요청 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `RequestCount` | 처리된 요청 수 | Sum |
| `ProcessedBytes` | 처리된 총 바이트 | Sum |

### HTTP 응답 메트릭 (ALB 생성)

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `HTTP_Fixed_Response_Count` | 고정 응답 액션 수 | Sum |
| `HTTP_Redirect_Count` | 리다이렉트 액션 수 | Sum |
| `HTTPCode_ELB_3XX_Count` | ALB의 3xx 응답 수 | Sum |
| `HTTPCode_ELB_4XX_Count` | ALB의 4xx 응답 수 | Sum |
| `HTTPCode_ELB_5XX_Count` | ALB의 5xx 응답 수 | Sum |
| `HTTPCode_ELB_500_Count` | ALB의 500 응답 수 | Sum |
| `HTTPCode_ELB_502_Count` | ALB의 502 응답 수 | Sum |
| `HTTPCode_ELB_503_Count` | ALB의 503 응답 수 | Sum |
| `HTTPCode_ELB_504_Count` | ALB의 504 응답 수 | Sum |

### TLS 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `ClientTLSNegotiationErrorCount` | 클라이언트 TLS 협상 실패 | Sum |

## 타겟 그룹 메트릭

### 상태 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `HealthyHostCount` | 정상 타겟 수 | Average, Minimum, Maximum |
| `UnHealthyHostCount` | 비정상 타겟 수 | Average, Minimum, Maximum |

### 응답 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `HTTPCode_Target_2XX_Count` | 타겟의 2xx 응답 수 | Sum |
| `HTTPCode_Target_3XX_Count` | 타겟의 3xx 응답 수 | Sum |
| `HTTPCode_Target_4XX_Count` | 타겟의 4xx 응답 수 | Sum |
| `HTTPCode_Target_5XX_Count` | 타겟의 5xx 응답 수 | Sum |

### 연결 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `TargetConnectionErrorCount` | 타겟 연결 실패 수 | Sum |
| `TargetTLSNegotiationErrorCount` | 타겟 TLS 협상 실패 | Sum |

### 지연 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `TargetResponseTime` | 타겟 응답 시간 (초) | Average, p50, p95, p99 |

## 용량 메트릭

### LCU (Load Balancer Capacity Unit)

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `ConsumedLCUs` | 사용된 LCU 수 | Sum |
| `ActiveConnectionCount` | LCU 기준: 초당 3,000 활성 연결 | Sum |
| `NewConnectionCount` | LCU 기준: 초당 25 새 연결 | Sum |
| `ProcessedBytes` | LCU 기준: 시간당 1GB | Sum |

## Lambda 관련 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `LambdaTargetProcessedBytes` | Lambda 처리 바이트 | Sum |
| `LambdaInternalError` | Lambda 내부 오류 수 | Sum |
| `LambdaUserError` | Lambda 사용자 오류 수 | Sum |

## 인증 메트릭

| 메트릭 | 설명 | 통계 |
|--------|------|------|
| `ELBAuthError` | 인증 오류 수 | Sum |
| `ELBAuthFailure` | 인증 실패 수 | Sum |
| `ELBAuthLatency` | 인증 지연 시간 | Average, p99 |
| `ELBAuthSuccess` | 인증 성공 수 | Sum |

## CloudWatch 알람 설정

### 권장 알람

**1. 비정상 타겟 알람**
```bash
aws cloudwatch put-metric-alarm \
    --alarm-name "UnhealthyTargets" \
    --metric-name UnHealthyHostCount \
    --namespace AWS/ApplicationELB \
    --statistic Average \
    --period 60 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=LoadBalancer,Value=app/my-alb/1234567890 \
                 Name=TargetGroup,Value=targetgroup/my-tg/1234567890 \
    --evaluation-periods 2 \
    --alarm-actions arn:aws:sns:us-east-1:123456789012:my-topic
```

**2. 5xx 에러 알람**
```bash
aws cloudwatch put-metric-alarm \
    --alarm-name "ALB-5xx-Errors" \
    --metric-name HTTPCode_ELB_5XX_Count \
    --namespace AWS/ApplicationELB \
    --statistic Sum \
    --period 300 \
    --threshold 10 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=LoadBalancer,Value=app/my-alb/1234567890 \
    --evaluation-periods 1 \
    --alarm-actions arn:aws:sns:us-east-1:123456789012:my-topic
```

**3. 응답 시간 알람**
```bash
aws cloudwatch put-metric-alarm \
    --alarm-name "HighLatency" \
    --metric-name TargetResponseTime \
    --namespace AWS/ApplicationELB \
    --extended-statistic p99 \
    --period 60 \
    --threshold 2 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=LoadBalancer,Value=app/my-alb/1234567890 \
                 Name=TargetGroup,Value=targetgroup/my-tg/1234567890 \
    --evaluation-periods 3 \
    --alarm-actions arn:aws:sns:us-east-1:123456789012:my-topic
```

## CloudWatch 대시보드

### 예시 대시보드 위젯

```json
{
    "widgets": [
        {
            "type": "metric",
            "properties": {
                "metrics": [
                    ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", "app/my-alb/1234567890"]
                ],
                "title": "Request Count",
                "period": 60,
                "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "metrics": [
                    ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", "app/my-alb/1234567890", "TargetGroup", "targetgroup/my-tg/1234567890"]
                ],
                "title": "Response Time",
                "period": 60,
                "stat": "Average"
            }
        },
        {
            "type": "metric",
            "properties": {
                "metrics": [
                    ["AWS/ApplicationELB", "HealthyHostCount", "LoadBalancer", "app/my-alb/1234567890", "TargetGroup", "targetgroup/my-tg/1234567890"],
                    ["AWS/ApplicationELB", "UnHealthyHostCount", "LoadBalancer", "app/my-alb/1234567890", "TargetGroup", "targetgroup/my-tg/1234567890"]
                ],
                "title": "Target Health",
                "period": 60,
                "stat": "Average"
            }
        }
    ]
}
```

## 메트릭 수집 특성

### 중요 사항

```yaml
조건:
  - 트래픽이 있을 때만 메트릭 게시
  - 헬스 체크 요청은 메트릭에 포함되지 않음

주기:
  - 기본 1분 단위
  - 일부 메트릭은 5분 단위

보존:
  - 1분 데이터: 15일
  - 5분 데이터: 63일
  - 1시간 데이터: 455일
```

## AWS CLI로 메트릭 조회

```bash
# 요청 수 조회
aws cloudwatch get-metric-statistics \
    --namespace AWS/ApplicationELB \
    --metric-name RequestCount \
    --dimensions Name=LoadBalancer,Value=app/my-alb/1234567890 \
    --start-time 2024-01-15T00:00:00Z \
    --end-time 2024-01-15T23:59:59Z \
    --period 3600 \
    --statistics Sum

# 응답 시간 조회
aws cloudwatch get-metric-statistics \
    --namespace AWS/ApplicationELB \
    --metric-name TargetResponseTime \
    --dimensions Name=LoadBalancer,Value=app/my-alb/1234567890 \
                 Name=TargetGroup,Value=targetgroup/my-tg/1234567890 \
    --start-time 2024-01-15T00:00:00Z \
    --end-time 2024-01-15T23:59:59Z \
    --period 300 \
    --statistics Average
```

## 참고 자료

- [AWS 공식 문서 - CloudWatch 메트릭](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-cloudwatch-metrics.html)
