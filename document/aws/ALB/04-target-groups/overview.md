# AWS ALB 타겟 그룹 개요

## 타겟 그룹이란?

타겟 그룹은 로드 밸런서가 요청을 라우팅하는 ** 대상들의 논리적 그룹** 입니다. 리스너 규칙을 통해 특정 조건에 맞는 요청을 해당 타겟 그룹으로 전달합니다.

## 타겟 유형 (Target Type)

### 1. Instance (인스턴스)
```yaml
대상: EC2 인스턴스
식별: 인스턴스 ID
특징:
  - 인스턴스의 프라이머리 네트워크 인터페이스 사용
  - 여러 포트로 동일 인스턴스 등록 가능
  - Auto Scaling 그룹과 통합 가능
```

### 2. IP Address (IP 주소)
```yaml
대상: IP 주소로 지정
허용 CIDR:
  - 타겟 그룹 VPC의 서브넷
  - 10.0.0.0/8 (RFC 1918)
  - 172.16.0.0/12 (RFC 1918)
  - 192.168.0.0/16 (RFC 1918)
  - 100.64.0.0/10 (RFC 6598)
사용 사례:
  - 컨테이너 (ECS/EKS)
  - 온프레미스 서버 (Direct Connect/VPN 통해)
  - 다른 VPC의 리소스
```

### 3. Lambda Function
```yaml
대상: Lambda 함수 ARN
제한: 타겟 그룹당 1개의 Lambda 함수만 등록
특징:
  - 직접 호출 (네트워크 연결 아님)
  - 보안 그룹 아웃바운드 규칙 불필요
  - 요청 본문 최대 1MB
```

## 프로토콜 및 포트

### 지원 프로토콜
| 프로토콜 | 설명 | 사용 사례 |
|---------|------|----------|
| HTTP | 기본 HTTP | 일반 웹 애플리케이션 |
| HTTPS | 종단 간 암호화 | 규정 준수, 민감 데이터 |

### 포트 범위
- **1 ~ 65535**
- 동일 인스턴스를 다른 포트로 여러 번 등록 가능

## 프로토콜 버전

### HTTP/1.1 (기본)
```yaml
특징: 표준 HTTP/1.1 프로토콜
요청: 텍스트 기반
```

### HTTP/2
```yaml
특징:
  - 바이너리 프레이밍
  - 다중화된 스트림
  - 헤더 압축
요구사항: HTTPS 리스너와 함께 사용 권장
```

### gRPC
```yaml
특징:
  - 고성능 RPC
  - Protocol Buffers 사용
  - 단항/스트리밍 지원
요구사항: HTTPS 리스너 + HTTP/2 필수
```

## 라우팅 알고리즘

### Round Robin (기본)
```yaml
설명: 각 타겟에 순차적으로 요청 분배
사용 사례: 동일한 용량의 타겟들
```

### Least Outstanding Requests
```yaml
설명: 처리 중인 요청이 가장 적은 타겟으로 라우팅
사용 사례:
  - 요청 처리 시간이 다양한 경우
  - 타겟 용량이 다른 경우
설정:
  aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes Key=load_balancing.algorithm.type,Value=least_outstanding_requests
```

### Weighted Random
```yaml
설명: 이상 탐지 완화(Anomaly Mitigation)와 함께 사용
특징:
  - 응답 지연이 높은 타겟의 가중치 자동 감소
  - 장애 타겟으로의 트래픽 자동 감소
설정:
  Key=load_balancing.algorithm.type,Value=weighted_random
  Key=load_balancing.algorithm.anomaly_mitigation,Value=on
```

## 타겟 그룹 속성

| 속성 | 설명 | 기본값 |
|------|------|-------|
| `deregistration_delay.timeout_seconds` | 연결 드레이닝 대기 시간 | 300초 |
| `slow_start.duration_seconds` | 슬로우 스타트 기간 | 0 (비활성화) |
| `stickiness.enabled` | 스티키 세션 활성화 | false |
| `load_balancing.algorithm.type` | 라우팅 알고리즘 | round_robin |

## 타겟 그룹 생성

### AWS 콘솔
1. EC2 콘솔 → **Target Groups** → **Create target group**
2. 타겟 유형 선택
3. 이름, 프로토콜, 포트 설정
4. VPC 선택
5. 헬스 체크 구성
6. 타겟 등록

### AWS CLI
```bash
aws elbv2 create-target-group \
    --name my-targets \
    --protocol HTTP \
    --port 80 \
    --vpc-id vpc-12345678 \
    --target-type instance \
    --health-check-protocol HTTP \
    --health-check-path /health
```

## 타겟 그룹 수정

```bash
# 속성 수정
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes \
        Key=deregistration_delay.timeout_seconds,Value=60 \
        Key=slow_start.duration_seconds,Value=30

# 헬스 체크 수정
aws elbv2 modify-target-group \
    --target-group-arn <arn> \
    --health-check-path /healthcheck \
    --health-check-interval-seconds 15
```

## 타겟 그룹 삭제

```bash
aws elbv2 delete-target-group \
    --target-group-arn <arn>
```

** 주의:** 리스너 규칙에서 참조 중인 타겟 그룹은 삭제할 수 없습니다.

## 제한사항

| 항목 | 제한 |
|------|------|
| 리전당 타겟 그룹 수 | 3,000개 (NLB와 공유) |
| 타겟 그룹당 타겟 수 (인스턴스/IP) | 1,000개 |
| 타겟 그룹당 Lambda 함수 | 1개 |
| ALB당 타겟 그룹 수 | 100개 |
| 타겟 그룹 이름 길이 | 32자 |

## 참고 자료

- [AWS 공식 문서 - 타겟 그룹](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-target-groups.html)
