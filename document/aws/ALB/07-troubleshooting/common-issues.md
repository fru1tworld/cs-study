# AWS ALB 문제 해결

## 일반적인 문제 및 해결 방법

### 1. 타겟 헬스 체크 실패

**증상:** 타겟이 unhealthy 상태

**원인 및 해결:**

| 원인 | 확인 방법 | 해결 |
|------|----------|------|
| 보안 그룹 차단 | 타겟 SG 인바운드 규칙 확인 | ALB SG에서 헬스체크 포트 허용 |
| 네트워크 ACL 차단 | 서브넷 NACL 확인 | 인바운드/아웃바운드 규칙 추가 |
| 헬스체크 경로 없음 | 타겟에서 경로 테스트 | 헬스체크 엔드포인트 구현 |
| 응답 코드 불일치 | 타겟 응답 코드 확인 | Matcher 설정 수정 |
| 응답 타임아웃 | 애플리케이션 성능 확인 | 타임아웃 증가 또는 최적화 |
| 인스턴스 상태 이상 | EC2 상태 확인 | 인스턴스 재시작 |

**디버깅:**
```bash
# 타겟 헬스 상태 확인
aws elbv2 describe-target-health \
    --target-group-arn <arn>

# 타겟에서 직접 헬스체크 테스트
curl -v http://<target-ip>:<port>/health
```

### 2. 클라이언트 연결 실패

**증상:** 클라이언트가 ALB에 연결할 수 없음

**원인 및 해결:**

| 원인 | 확인 방법 | 해결 |
|------|----------|------|
| ALB SG 차단 | ALB SG 인바운드 확인 | 리스너 포트 허용 |
| 프라이빗 서브넷 | 서브넷 유형 확인 | 퍼블릭 서브넷으로 변경 |
| IGW 없음 | VPC 라우팅 테이블 확인 | 인터넷 게이트웨이 연결 |
| DNS 미해석 | DNS 조회 테스트 | Route 53/DNS 설정 확인 |

**디버깅:**
```bash
# DNS 확인
dig <alb-dns-name>

# 연결 테스트
curl -v http://<alb-dns-name>

# ALB 상태 확인
aws elbv2 describe-load-balancers \
    --names my-alb \
    --query 'LoadBalancers[*].State'
```

### 3. HTTPS 인증서 오류

**증상:** SSL/TLS 핸드셰이크 실패, 인증서 경고

**원인 및 해결:**

| 원인 | 확인 방법 | 해결 |
|------|----------|------|
| 도메인 불일치 | 인증서 CN/SAN 확인 | 올바른 인증서 사용 |
| 인증서 만료 | 인증서 유효기간 확인 | 인증서 갱신 |
| 기본 DNS 사용 | 요청 URL 확인 | 커스텀 도메인 사용 |
| 보안 정책 불일치 | TLS 버전 확인 | 보안 정책 업데이트 |

**디버깅:**
```bash
# 인증서 확인
openssl s_client -connect <alb-dns>:443 -servername <domain>

# 인증서 정보 조회
aws elbv2 describe-listener-certificates \
    --listener-arn <listener-arn>
```

## HTTP 에러 코드

### ALB 생성 에러 (ELB_xxx)

#### 400 Bad Request
```yaml
원인:
  - 잘못된 요청 형식
  - 요청 라인 16KB 초과
  - 헤더 크기 제한 초과

해결:
  - 요청 형식 수정
  - 헤더 크기 줄이기
```

#### 401 Unauthorized
```yaml
원인:
  - 인증 액션 실패
  - IdP 인증 오류

해결:
  - IdP 설정 확인
  - 인증 구성 검토
```

#### 403 Forbidden
```yaml
원인:
  - WAF 규칙에 의해 차단
  - 인증 실패

해결:
  - WAF 규칙 검토
  - 인증 설정 확인
```

#### 460 Client Closed Connection
```yaml
원인:
  - 클라이언트가 연결 조기 종료
  - 네트워크 불안정

해결:
  - 클라이언트 측 문제 확인
  - 네트워크 상태 점검
```

#### 463 Too Many IP Addresses
```yaml
원인:
  - X-Forwarded-For 헤더에 IP 30개 초과

해결:
  - 중간 프록시 확인
  - 불필요한 프록시 제거
```

#### 464 Incompatible Protocol
```yaml
원인:
  - 호환되지 않는 프로토콜
  - HTTP/2 요청을 HTTP/1.1 타겟으로 전송

해결:
  - 타겟 그룹 프로토콜 버전 확인
  - 프로토콜 호환성 확인
```

### 타겟 관련 에러 (5xx)

#### 502 Bad Gateway
```yaml
원인:
  - 타겟이 유효하지 않은 응답 반환
  - 타겟 연결 실패
  - Lambda 함수 오류

해결:
  - 타겟 로그 확인
  - 타겟 애플리케이션 디버깅
  - Lambda 에러 확인
```

#### 503 Service Unavailable
```yaml
원인:
  - 정상 타겟 없음
  - 모든 타겟 unhealthy

해결:
  - 타겟 헬스 상태 확인
  - 타겟 등록 확인
  - 헬스체크 설정 검토
```

#### 504 Gateway Timeout
```yaml
원인:
  - 타겟 응답 타임아웃
  - 유휴 타임아웃 초과

해결:
  - 유휴 타임아웃 증가
  - 타겟 처리 시간 최적화
  - 타겟 keep-alive 설정 확인
```

#### 505 HTTP Version Not Supported
```yaml
원인:
  - 지원되지 않는 HTTP 버전

해결:
  - HTTP/1.0, 1.1, 2.0 사용
```

#### 561 Unauthorized
```yaml
원인:
  - IdP 인증 코드가 이미 사용됨
  - 인증 흐름 오류

해결:
  - 인증 재시도
  - IdP 설정 확인
```

## 리소스 맵 활용

### 리소스 맵이란?
ALB 콘솔에서 제공하는 **시각적 문제 진단 도구**입니다.

### 기능
- 타겟 그룹 및 타겟 상태 시각화
- unhealthy 타겟 식별
- 실패 원인 표시
- 구성 문제 감지

### 사용 방법
1. EC2 콘솔 → Load Balancers
2. ALB 선택
3. **Resource map** 탭 클릭
4. 문제가 있는 리소스 확인

## 성능 문제 해결

### 높은 지연 시간

**확인 사항:**
```yaml
CloudWatch 메트릭:
  - TargetResponseTime: 타겟 처리 시간
  - RequestCount: 요청량
  - ConsumedLCUs: 용량 사용량

액세스 로그:
  - request_processing_time
  - target_processing_time
  - response_processing_time
```

**해결 방법:**
```yaml
타겟 측 최적화:
  - 애플리케이션 성능 개선
  - 타겟 수 증가 (Auto Scaling)
  - 더 큰 인스턴스 타입 사용

ALB 측 최적화:
  - 크로스 존 로드 밸런싱 활성화
  - 라우팅 알고리즘 변경 (Least Outstanding Requests)
```

### 높은 에러율

**확인 사항:**
```yaml
CloudWatch 메트릭:
  - HTTPCode_ELB_5XX_Count
  - HTTPCode_Target_5XX_Count
  - TargetConnectionErrorCount

액세스 로그:
  - elb_status_code
  - target_status_code
```

**해결 방법:**
```yaml
5xx 에러:
  - 타겟 로그 확인
  - 타겟 용량 증가
  - 헬스체크 설정 조정

연결 에러:
  - 보안 그룹 확인
  - 타겟 가용성 확인
```

## 디버깅 체크리스트

### 연결 문제
- [ ] ALB 상태가 `active`인지 확인
- [ ] 보안 그룹 인바운드 규칙 확인
- [ ] 서브넷이 퍼블릭인지 확인 (Internet-facing)
- [ ] 인터넷 게이트웨이 연결 확인
- [ ] DNS 해석 확인

### 헬스체크 문제
- [ ] 타겟 보안 그룹에서 ALB 트래픽 허용
- [ ] 헬스체크 경로가 올바른지 확인
- [ ] 예상 응답 코드 확인
- [ ] 타겟 애플리케이션 실행 중인지 확인
- [ ] 네트워크 ACL 확인

### SSL/TLS 문제
- [ ] 인증서 유효기간 확인
- [ ] 도메인 이름 일치 확인
- [ ] 보안 정책 호환성 확인
- [ ] SNI 설정 확인 (다중 도메인)

## 참고 자료

- [AWS 공식 문서 - 트러블슈팅](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-troubleshooting.html)
