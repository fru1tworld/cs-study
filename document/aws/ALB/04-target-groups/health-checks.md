# AWS ALB 헬스 체크

## 개요

Application Load Balancer는 등록된 타겟에 ** 주기적으로 요청을 전송** 하여 상태를 테스트합니다. 헬스 체크를 통과한 정상(healthy) 타겟으로만 트래픽을 라우팅합니다.

## 헬스 체크 설정

### 기본 설정값

| 설정 | 기본값 | 범위 | 설명 |
|------|-------|------|------|
| `HealthCheckProtocol` | HTTP | HTTP/HTTPS | 헬스 체크 프로토콜 |
| `HealthCheckPort` | traffic-port | 1-65535 | 헬스 체크 포트 |
| `HealthCheckPath` | / | URI 경로 | 헬스 체크 경로 |
| `HealthCheckIntervalSeconds` | 30초 | 5-300초 | 체크 간격 |
| `HealthCheckTimeoutSeconds` | 5초 | 2-120초 | 타임아웃 |
| `HealthyThresholdCount` | 5 | 2-10 | 정상 판정 횟수 |
| `UnhealthyThresholdCount` | 2 | 2-10 | 비정상 판정 횟수 |
| `Matcher` | 200 | HTTP 상태 코드 | 성공 응답 코드 |

### Lambda 타겟 기본값

| 설정 | 기본값 |
|------|-------|
| HealthCheckIntervalSeconds | 35초 |
| HealthCheckTimeoutSeconds | 30초 |

## 헬스 체크 경로

### HTTP/HTTPS 타겟
```yaml
경로 형식: /health 또는 /api/health
응답 요구: 구성된 상태 코드 반환
```

### gRPC 타겟
```yaml
경로 형식: /package.service/method
응답 요구: gRPC 상태 코드 반환
```

## 성공 응답 코드 (Matcher)

### HTTP 타겟
```yaml
단일 코드: 200
범위 지정: 200-299
다중 코드: 200,202,204
```

### gRPC 타겟
```yaml
단일 코드: 0
범위 지정: 0-10
다중 코드: 0,1,2
```

## 타겟 상태 (Target Health)

| 상태 | 설명 |
|------|------|
| `healthy` | 헬스 체크 통과 |
| `unhealthy` | 헬스 체크 실패 |
| `initial` | 등록 직후 초기 체크 중 |
| `draining` | 등록 해제 중 (연결 드레이닝) |
| `unused` | 타겟 그룹 미사용 또는 미등록 |
| `unavailable` | 헬스 체크 비활성화됨 |

### 상태 이유 코드

```yaml
unhealthy 사유:
  - Elb.InitialHealthChecking: 초기 체크 중
  - Target.Timeout: 응답 타임아웃
  - Target.ResponseCodeMismatch: 응답 코드 불일치
  - Target.FailedHealthChecks: 연속 실패

unused 사유:
  - Target.NotRegistered: 등록되지 않음
  - Target.NotInUse: 가용 영역 비활성화
  - Target.InvalidState: 인스턴스 상태 이상
```

## 상태 전환

### Unhealthy → Healthy
```yaml
조건: HealthyThresholdCount 만큼 연속 성공
예시: 5회 연속 성공 시 정상 판정
```

### Healthy → Unhealthy
```yaml
조건: UnhealthyThresholdCount 만큼 연속 실패
예시: 2회 연속 실패 시 비정상 판정
```

## Fail-Open 동작

```yaml
조건: 모든 타겟이 헬스 체크 실패
동작: 모든 타겟으로 요청 라우팅 (fail-open)
이유: 완전한 서비스 중단 방지
```

## 헬스 체크 설정

### AWS 콘솔
1. 타겟 그룹 선택 → **Health checks** 탭
2. **Edit** 클릭
3. 설정 변경 후 저장

### AWS CLI
```bash
aws elbv2 modify-target-group \
    --target-group-arn <arn> \
    --health-check-protocol HTTP \
    --health-check-port traffic-port \
    --health-check-path /health \
    --health-check-interval-seconds 15 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 3 \
    --unhealthy-threshold-count 2 \
    --matcher HttpCode=200-299
```

## 헬스 체크 상태 확인

### AWS CLI
```bash
aws elbv2 describe-target-health \
    --target-group-arn <arn>
```

### 응답 예시
```json
{
    "TargetHealthDescriptions": [
        {
            "Target": {
                "Id": "i-1234567890abcdef0",
                "Port": 80
            },
            "HealthCheckPort": "80",
            "TargetHealth": {
                "State": "healthy"
            }
        },
        {
            "Target": {
                "Id": "i-0987654321fedcba0",
                "Port": 80
            },
            "HealthCheckPort": "80",
            "TargetHealth": {
                "State": "unhealthy",
                "Reason": "Target.Timeout",
                "Description": "Request timed out"
            }
        }
    ]
}
```

## 헬스 체크 최적화

### 빠른 장애 감지
```yaml
설정:
  HealthCheckIntervalSeconds: 5
  HealthCheckTimeoutSeconds: 2
  UnhealthyThresholdCount: 2
최소 감지 시간: 5 + 5 = 10초
```

### 빠른 복구 감지
```yaml
설정:
  HealthCheckIntervalSeconds: 5
  HealthyThresholdCount: 2
최소 복구 시간: 5 + 5 = 10초
```

### 안정적인 설정 (기본 권장)
```yaml
설정:
  HealthCheckIntervalSeconds: 30
  HealthCheckTimeoutSeconds: 5
  HealthyThresholdCount: 5
  UnhealthyThresholdCount: 2
```

## 헬스 체크 vs 애플리케이션 트래픽

| 항목 | 헬스 체크 | 애플리케이션 트래픽 |
|------|----------|-------------------|
| 소스 IP | ALB 노드 IP | 클라이언트 IP (X-Forwarded-For) |
| User-Agent | ELB-HealthChecker/2.0 | 클라이언트 브라우저 |
| CloudWatch 메트릭 | 제외됨 | 포함됨 |
| 액세스 로그 | 제외됨 | 포함됨 |

## 주의사항

### 1. 타임아웃 설정
```yaml
규칙: HealthCheckTimeoutSeconds < HealthCheckIntervalSeconds
이유: 타임아웃이 간격보다 길면 체크가 중첩될 수 있음
```

### 2. WebSocket 미지원
```yaml
헬스 체크는 WebSocket 프로토콜을 지원하지 않음
HTTP 엔드포인트로 별도 구성 필요
```

### 3. HTTPS 헬스 체크
```yaml
동작: SSL/TLS 핸드셰이크 수행
인증서: 자체 서명 인증서도 허용
검증: 인증서 유효성 검증하지 않음
```

## 참고 자료

- [AWS 공식 문서 - 헬스 체크](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/target-group-health-checks.html)
