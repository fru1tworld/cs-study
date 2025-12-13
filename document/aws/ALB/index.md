# AWS Application Load Balancer (ALB) 문서

> AWS Application Load Balancer 공식 문서를 기반으로 정리한 종합 가이드입니다.

## 목차

### 1. 개요 (Overview)
- [ALB 소개](./01-overview/introduction.md) - ALB 정의, 핵심 구성 요소, 주요 특징
- [시작하기](./01-overview/getting-started.md) - 사전 준비, ALB 생성 단계, 테스트

### 2. 로드 밸런서 (Load Balancer)
- [로드 밸런서 구성](./02-load-balancer/configuration.md) - 스킴, 서브넷, IP 주소 유형, 속성
- [로드 밸런서 삭제](./02-load-balancer/deletion.md) - 삭제 전 확인사항, 삭제 방법

### 3. 리스너 (Listeners)
- [리스너 개요](./03-listeners/overview.md) - 프로토콜, HTTP/2, WebSocket, gRPC 지원
- [리스너 규칙](./03-listeners/rules.md) - 조건, 액션, 우선순위, 규칙 관리
- [HTTPS 및 SSL/TLS](./03-listeners/https-ssl.md) - 인증서 관리, 보안 정책, HTTP→HTTPS 리다이렉트
- [사용자 인증](./03-listeners/authentication.md) - OIDC, Amazon Cognito 통합

### 4. 타겟 그룹 (Target Groups)
- [타겟 그룹 개요](./04-target-groups/overview.md) - 타겟 유형, 프로토콜, 라우팅 알고리즘
- [헬스 체크](./04-target-groups/health-checks.md) - 설정, 타겟 상태, 최적화
- [타겟 등록 및 해제](./04-target-groups/registration.md) - 등록 방법, 연결 드레이닝, Slow Start
- [스티키 세션](./04-target-groups/sticky-sessions.md) - Duration-Based, Application-Based 세션 유지
- [Lambda 통합](./04-target-groups/lambda-integration.md) - Lambda 타겟, 요청/응답 형식

### 5. 보안 (Security)
- [상호 인증 (mTLS)](./05-security/mtls.md) - Passthrough/Verify 모드, Trust Store
- [보안 그룹](./05-security/security-groups.md) - ALB 및 타겟 보안 그룹 구성

### 6. 모니터링 (Monitoring)
- [액세스 로그](./06-monitoring/access-logs.md) - S3 저장, 로그 형식, Athena 분석
- [CloudWatch 메트릭](./06-monitoring/cloudwatch-metrics.md) - 주요 메트릭, 알람 설정
- [요청 추적](./06-monitoring/request-tracing.md) - X-Amzn-Trace-Id, X-Ray 통합

### 7. 문제 해결 (Troubleshooting)
- [일반적인 문제](./07-troubleshooting/common-issues.md) - 헬스체크 실패, HTTP 에러 코드, 디버깅
- [할당량](./07-troubleshooting/quotas.md) - 리소스 제한, 증가 요청 방법

---

## 빠른 참조

### 지원 프로토콜
| 프로토콜 | 리스너 | 타겟 |
|---------|--------|------|
| HTTP | Yes | Yes |
| HTTPS | Yes | Yes |
| HTTP/2 | Yes | Yes |
| gRPC | Yes | Yes |
| WebSocket | Yes | Yes |

### 타겟 유형
| 유형 | 설명 |
|------|------|
| Instance | EC2 인스턴스 ID로 지정 |
| IP | IPv4 주소로 지정 |
| Lambda | Lambda 함수 ARN |

### 주요 할당량
| 리소스 | 기본 제한 |
|--------|----------|
| 리전당 ALB | 50 |
| ALB당 리스너 | 50 |
| ALB당 규칙 | 100 |
| 리전당 타겟 그룹 | 3,000 |
| 타겟 그룹당 타겟 | 1,000 |

### 중요 CloudWatch 메트릭
| 메트릭 | 설명 |
|--------|------|
| `RequestCount` | 처리된 요청 수 |
| `TargetResponseTime` | 타겟 응답 시간 |
| `HealthyHostCount` | 정상 타겟 수 |
| `HTTPCode_ELB_5XX_Count` | ALB의 5xx 에러 |
| `HTTPCode_Target_5XX_Count` | 타겟의 5xx 에러 |

---

## 참고 자료

- [AWS 공식 문서 - Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)
- [AWS CLI - elbv2 명령어](https://docs.aws.amazon.com/cli/latest/reference/elbv2/)
- [AWS CloudFormation - ALB 리소스](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-elasticloadbalancingv2-loadbalancer.html)
