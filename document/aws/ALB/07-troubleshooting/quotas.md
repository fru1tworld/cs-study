# AWS ALB 할당량 (Quotas)

## 개요

AWS Application Load Balancer에는 리소스별 기본 할당량이 있습니다. 일부 할당량은 AWS 지원을 통해 증가를 요청할 수 있습니다.

## 로드 밸런서 할당량

| 리소스 | 기본 제한 | 조정 가능 |
|--------|----------|----------|
| 리전당 ALB 수 | 50 | Yes |
| ALB당 리스너 수 | 50 | Yes |
| ALB당 타겟 그룹 수 | 100 | No |
| ALB당 타겟 수 | 1,000 | Yes |
| ALB당 인증서 수 (기본 제외) | 25 | Yes |
| ALB당 보안 그룹 수 | 5 | No |
| ALB당 규칙 수 (기본 제외) | 100 | Yes |

## 타겟 그룹 할당량

| 리소스 | 기본 제한 | 조정 가능 |
|--------|----------|----------|
| 리전당 타겟 그룹 수 | 3,000 | Yes |
| 타겟 그룹당 타겟 수 (인스턴스/IP) | 1,000 | Yes |
| 타겟 그룹당 Lambda 함수 | 1 | No |

**참고:** 타겟 그룹 할당량은 NLB(Network Load Balancer)와 공유됩니다.

## 리스너 규칙 할당량

| 리소스 | 기본 제한 | 조정 가능 |
|--------|----------|----------|
| 리스너당 규칙 수 (기본 제외) | 100 | Yes |
| 규칙당 조건 수 | 5 | No |
| 규칙당 조건 값 수 | 5 | No |
| 규칙당 와일드카드 수 | 6 | No |
| 규칙당 가중치 기반 타겟 그룹 수 | 5 | No |

## 보안 할당량

| 리소스 | 기본 제한 | 조정 가능 |
|--------|----------|----------|
| 계정당 Trust Store 수 | 20 | Yes |
| Trust Store당 CA 인증서 수 | 25 | Yes |
| Trust Store당 CRL 수 | 20 | Yes |
| ALB당 mTLS 리스너 수 | 2 | No |

## HTTP 요청/응답 제한

### 요청 제한

| 항목 | 최대 크기 |
|------|----------|
| 요청 라인 | 16 KB |
| 단일 요청 헤더 | 16 KB |
| 전체 요청 헤더 | 64 KB |
| 호스트 헤더 | 128자 |
| 경로 패턴 | 128자 |

### 응답 제한

| 항목 | 최대 크기 |
|------|----------|
| 전체 응답 헤더 | 32 KB |

### Lambda 타겟 제한

| 항목 | 최대 크기 |
|------|----------|
| 요청 본문 | 1 MB |
| 응답 JSON | 1 MB |

## 연결 제한

| 항목 | 제한 |
|------|------|
| 유휴 타임아웃 | 1-4,000초 (기본 60초) |
| HTTP/2 연결당 스트림 수 | 128 |
| X-Forwarded-For 헤더 IP 수 | 30개 |

## 할당량 증가 요청

### AWS Service Quotas 콘솔

1. AWS 콘솔 → **Service Quotas** 서비스
2. **AWS services** → **Elastic Load Balancing**
3. 증가할 할당량 선택
4. **Request quota increase** 클릭
5. 새로운 값 입력 및 제출

### AWS CLI

```bash
# 현재 할당량 확인
aws service-quotas get-service-quota \
    --service-code elasticloadbalancing \
    --quota-code L-53DA6B97

# 할당량 증가 요청
aws service-quotas request-service-quota-increase \
    --service-code elasticloadbalancing \
    --quota-code L-53DA6B97 \
    --desired-value 100
```

### 할당량 코드

| 리소스 | 할당량 코드 |
|--------|-----------|
| 리전당 ALB | L-53DA6B97 |
| ALB당 리스너 | L-B6DF7632 |
| 리전당 타겟 그룹 | L-B22855CB |
| 타겟 그룹당 타겟 | L-7E6692B2 |

## 제한 초과 시 동작

### 리소스 생성 실패
```yaml
에러: LimitExceededException
메시지: "You have reached the limit for the number of..."
해결: 할당량 증가 요청 또는 미사용 리소스 정리
```

### 성능 저하
```yaml
증상:
  - 연결 거부 (RejectedConnectionCount 증가)
  - 높은 지연 시간
  - 502/503 에러 증가

해결:
  - 리소스 분산 (여러 ALB 사용)
  - 할당량 증가 요청
  - 아키텍처 최적화
```

## 모범 사례

### 1. 할당량 모니터링

```yaml
CloudWatch 알람 설정:
  - 타겟 수가 제한에 근접할 때 알림
  - 규칙 수 모니터링
  - 연결 수 추적
```

### 2. 리소스 정리

```bash
# 미사용 타겟 그룹 확인
aws elbv2 describe-target-groups \
    --query 'TargetGroups[?LoadBalancerArns==`[]`].TargetGroupArn'

# 미사용 ALB 확인
aws elbv2 describe-load-balancers \
    --query 'LoadBalancers[?State.Code!=`active`]'
```

### 3. 효율적인 규칙 사용

```yaml
권장:
  - 유사한 조건을 하나의 규칙으로 통합
  - 와일드카드 활용 (예: /api/*)
  - 불필요한 규칙 제거

예시:
  # 비효율적 (3개 규칙)
  - path: /api/users
  - path: /api/orders
  - path: /api/products

  # 효율적 (1개 규칙)
  - path: /api/*
```

### 4. 다중 ALB 아키텍처

대규모 환경에서는 여러 ALB로 분산:

```yaml
구성 예시:
  - ALB 1: api.example.com (API 트래픽)
  - ALB 2: www.example.com (웹 트래픽)
  - ALB 3: internal.example.com (내부 트래픽)

장점:
  - 할당량 분산
  - 장애 격리
  - 독립적 스케일링
```

## 참고 자료

- [AWS 공식 문서 - ALB 할당량](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-limits.html)
- [AWS Service Quotas 문서](https://docs.aws.amazon.com/servicequotas/latest/userguide/)
