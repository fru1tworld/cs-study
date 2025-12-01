# AWS Application Load Balancer (ALB) 개요

## 정의

Application Load Balancer(ALB)는 OSI 7계층(애플리케이션 계층)에서 동작하는 로드 밸런서로, 들어오는 트래픽을 EC2 인스턴스, 컨테이너, IP 주소, Lambda 함수 등 여러 대상에 자동으로 분산합니다.

## 핵심 구성 요소

### 1. 로드 밸런서 (Load Balancer)
- 클라이언트의 **단일 연결 지점** 역할
- 여러 가용 영역에 걸쳐 트래픽 분산
- DNS 이름을 통해 접근

### 2. 리스너 (Listener)
- 클라이언트 연결 요청을 모니터링하는 프로세스
- 구성된 프로토콜과 포트를 사용하여 연결 확인
- 규칙에 따라 트래픽을 적절한 타겟 그룹으로 라우팅

### 3. 타겟 그룹 (Target Group)
- 등록된 대상들의 논리적 그룹
- 헬스 체크를 통해 대상의 상태 모니터링
- 정상 대상으로만 트래픽 전달

## 주요 특징

### Layer 7 라우팅
- URL 경로 기반 라우팅 (`/api/*`, `/images/*`)
- 호스트명 기반 라우팅 (`api.example.com`, `www.example.com`)
- HTTP 헤더 및 쿼리 파라미터 기반 조건 설정

### 프로토콜 지원
| 프로토콜 | 설명 |
|---------|------|
| HTTP | 기본 HTTP 트래픽 |
| HTTPS | SSL/TLS 암호화 트래픽 |
| HTTP/2 | 다중화된 스트림 지원 |
| WebSocket | 양방향 실시간 통신 |
| gRPC | 고성능 RPC 프레임워크 |

### 자동 확장
- 트래픽 변화에 따른 자동 스케일링
- 가용 영역 간 부하 분산
- 고가용성 아키텍처 지원

## Classic Load Balancer 대비 장점

| 기능 | Classic LB | Application LB |
|------|-----------|----------------|
| Layer 7 라우팅 | 제한적 | 완전 지원 |
| 컨테이너 지원 | 제한적 | 완전 지원 |
| Lambda 타겟 | 미지원 | 지원 |
| WebSocket | 미지원 | 지원 |
| HTTP/2 | 미지원 | 지원 |
| 경로 기반 라우팅 | 미지원 | 지원 |
| 호스트 기반 라우팅 | 미지원 | 지원 |

## AWS 서비스 통합

ALB는 다음 AWS 서비스들과 원활하게 통합됩니다:

- **Amazon EC2**: 인스턴스를 타겟으로 등록
- **Amazon ECS/EKS**: 컨테이너화된 애플리케이션 지원
- **AWS Lambda**: 서버리스 함수 직접 호출
- **Auto Scaling**: 동적 용량 조정
- **Amazon CloudWatch**: 메트릭 모니터링 및 알람
- **AWS WAF**: 웹 애플리케이션 방화벽 통합
- **AWS Certificate Manager**: SSL/TLS 인증서 관리
- **Amazon Route 53**: DNS 라우팅 및 헬스 체크
- **Amazon Cognito**: 사용자 인증

## 사용 사례

1. **마이크로서비스 아키텍처**: 서비스별 경로 라우팅
2. **컨테이너 기반 애플리케이션**: ECS/EKS 서비스 부하 분산
3. **서버리스 애플리케이션**: Lambda 함수 트리거
4. **블루/그린 배포**: 가중치 기반 트래픽 분배
5. **A/B 테스팅**: 조건부 라우팅 활용

## 참고 자료

- [AWS 공식 문서](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/introduction.html)
