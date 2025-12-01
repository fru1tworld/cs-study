# AWS ALB 시작하기

## 사전 준비 사항

### 1. VPC 구성

- 최소 **2개의 가용 영역(AZ)** 에 서브넷 구성
- 각 서브넷은 최소 `/27` CIDR 블록 필요 (8개 이상의 여유 IP 주소)
- Internet-facing ALB의 경우 퍼블릭 서브넷 필요

### 2. 보안 그룹 설정

```
인바운드 규칙:
- 리스너 포트에서 클라이언트 트래픽 허용
- 예: HTTP(80), HTTPS(443)

아웃바운드 규칙:
- 타겟 포트로의 트래픽 허용
- 헬스 체크 포트로의 트래픽 허용
```

### 3. 타겟 준비

- 각 가용 영역에 최소 하나의 EC2 인스턴스 또는 타겟 배포
- 타겟이 실행 중(running) 상태인지 확인

## ALB 생성 단계

### Step 1: 기본 설정

**AWS 콘솔:**

1. EC2 콘솔 → **Load Balancers** → **Create load balancer**
2. **Application Load Balancer** 선택

** 기본 구성:**
| 설정 | 설명 |
|------|------|
| 이름 | 고유한 로드 밸런서 이름 |
| 스킴 | `Internet-facing` 또는 `Internal` |
| IP 주소 타입 | `IPv4` 또는 `Dualstack` |

### Step 2: 네트워크 매핑

```yaml
VPC: 대상 VPC 선택
가용 영역:
  - AZ 1: 서브넷 선택
  - AZ 2: 서브넷 선택
  # 최소 2개 AZ 필수
```

### Step 3: 보안 그룹 할당

- 새 보안 그룹 생성 또는 기존 그룹 선택
- 리스너 포트에 대한 인바운드 규칙 확인

### Step 4: 리스너 구성

** 기본 리스너 설정:**

```yaml
Protocol: HTTP
Port: 80
Default action: Forward to target group
```

**HTTPS 리스너 (선택):**

```yaml
Protocol: HTTPS
Port: 443
SSL Certificate: ACM 또는 IAM 인증서 선택
Default action: Forward to target group
```

### Step 5: 타겟 그룹 생성

** 타겟 그룹 설정:**

```yaml
Target type: Instances | IP addresses | Lambda function
Protocol: HTTP
Port: 80 (또는 애플리케이션 포트)
VPC: 타겟이 위치한 VPC
Health check path: /health (또는 애플리케이션 헬스 체크 경로)
```

### Step 6: 타겟 등록

1. 사용 가능한 인스턴스 목록에서 타겟 선택
2. **Include as pending below** 클릭
3. 포트 확인 후 **Register pending targets**

### Step 7: 검토 및 생성

- 모든 설정 검토
- **Create load balancer** 클릭
- 상태가 `active`가 될 때까지 대기 (보통 몇 분 소요)

## 생성 확인

### 상태 확인

```bash
# AWS CLI로 상태 확인
aws elbv2 describe-load-balancers \
    --names my-alb \
    --query 'LoadBalancers[*].State.Code'
```

### 헬스 체크 확인

```bash
# 타겟 헬스 상태 확인
aws elbv2 describe-target-health \
    --target-group-arn <target-group-arn>
```

### 접속 테스트

```bash
# ALB DNS 이름으로 접속 테스트
curl http://<alb-dns-name>
```

## ALB DNS 이름

생성된 ALB는 다음 형식의 DNS 이름을 받습니다:

```
my-alb-1234567890.us-east-1.elb.amazonaws.com
```

- 이 DNS 이름으로 직접 접속 가능
- Route 53을 통해 커스텀 도메인 연결 가능
- HTTPS 사용 시 인증서의 도메인과 일치해야 함

## 다음 단계

ALB 생성 후 추가로 구성할 수 있는 항목:

1. ** 리스너 규칙 추가**: 경로/호스트 기반 라우팅
2. **HTTPS 설정**: SSL/TLS 인증서 및 보안 정책
3. ** 로깅 활성화**: 액세스 로그를 S3에 저장
4. ** 모니터링 설정**: CloudWatch 알람 구성
5. **WAF 연동**: 웹 애플리케이션 방화벽 적용

## 참고 자료

- [AWS 공식 시작 가이드](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/application-load-balancer-getting-started.html)
