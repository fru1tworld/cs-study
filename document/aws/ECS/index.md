# Amazon ECS (Elastic Container Service) 문서

> Amazon ECS는 컨테이너화된 애플리케이션을 쉽게 배포, 관리, 확장할 수 있는 완전 관리형 컨테이너 오케스트레이션 서비스입니다.

## 목차

### 1. 개요 (Overview)

- [Amazon ECS란?](./overview/what-is-ecs.md)
  - ECS 소개 및 핵심 아키텍처
  - 주요 기능 및 사용 사례
  - 요금 체계

- [용어 및 개념](./overview/terminology.md)
  - 핵심 용어 정의
  - Task, Service, Cluster 개념
  - Launch Type 및 Capacity Provider

---

### 2. 클러스터 (Clusters)

- [클러스터 개요](./clusters/cluster-overview.md)
  - 클러스터 유형 (ECS Managed, Fargate, EC2)
  - 클러스터 구성 요소
  - 클러스터 생성 및 관리

- [Capacity Provider](./clusters/capacity-providers.md)
  - Fargate Capacity Provider
  - Auto Scaling Group Capacity Provider
  - Managed Scaling
  - Capacity Provider Strategy

---

### 3. 태스크 정의 (Task Definitions)

- [Task Definition 개요](./task-definitions/task-definition-overview.md)
  - Task Definition 구조
  - 필수 및 선택 파라미터
  - CPU/메모리 설정
  - IAM 역할 (Task Role, Execution Role)

- [Container Definition](./task-definitions/container-definitions.md)
  - 컨테이너 설정
  - 환경 변수 및 Secrets
  - 로깅 설정
  - 헬스 체크
  - 컨테이너 의존성

---

### 4. 서비스 (Services)

- [Service 개요](./services/service-overview.md)
  - Service vs Task
  - 서비스 스케줄러
  - 로드 밸런서 통합
  - 스케줄링 전략 (REPLICA, DAEMON)
  - 태스크 배치

- [Service Connect](./services/service-connect.md)
  - Service Connect 개요
  - Namespace 및 Endpoint
  - 서비스 간 통신
  - 설정 방법

---

### 5. AWS Fargate

- [Fargate 개요](./fargate/fargate-overview.md)
  - Fargate vs EC2
  - 태스크 격리
  - 지원 플랫폼 및 CPU/메모리
  - Fargate Spot
  - 네트워킹 및 스토리지
  - 플랫폼 버전

---

### 6. 네트워킹 (Networking)

- [네트워킹 개요](./networking/networking-overview.md)
  - 네트워크 모드 (awsvpc, bridge, host, none)
  - VPC 및 서브넷 설정
  - 보안 그룹
  - VPC 엔드포인트
  - 로드 밸런서 통합

---

### 7. 보안 (Security)

- [보안 개요](./security/security-overview.md)
  - AWS 공동 책임 모델
  - IAM 역할 (Task Role, Execution Role, Instance Role)
  - 시크릿 관리 (Secrets Manager, Parameter Store)
  - 네트워크 보안
  - 컨테이너 보안
  - 이미지 보안
  - 런타임 보안

---

### 8. 배포 (Deployment)

- [배포 전략](./deployment/deployment-strategies.md)
  - Rolling Update
  - Blue/Green 배포
  - Circuit Breaker
  - 배포 설정 파라미터
  - 모범 사례

---

### 9. Auto Scaling

- [Auto Scaling 개요](./auto-scaling/auto-scaling-overview.md)
  - Service Auto Scaling
    - Target Tracking
    - Step Scaling
    - Scheduled Scaling
    - Predictive Scaling
  - Cluster Auto Scaling
  - 스케일링 정책 조합

---

### 10. ECS Anywhere

- [ECS Anywhere 개요](./ecs-anywhere/ecs-anywhere-overview.md)
  - 온프레미스 환경 통합
  - 지원 OS 및 아키텍처
  - 설정 및 등록 방법
  - 네트워킹 제한사항

---

### 11. 운영 (Operations)

- [ECS Exec](./operations/ecs-exec.md)
  - 컨테이너 접속
  - 설정 및 권한
  - 로깅 및 감사
  - 보안 고려사항

---

## 빠른 참조

### 핵심 개념 요약

| 개념 | 설명 |
|------|------|
| **Cluster** | 태스크와 서비스의 논리적 그룹 |
| **Task Definition** | 애플리케이션의 청사진 (JSON) |
| **Task** | Task Definition의 실행 인스턴스 |
| **Service** | 원하는 수의 태스크를 지속적으로 유지 |
| **Capacity Provider** | 인프라 용량 관리 |

### Launch Type 비교

| 특성 | Fargate | EC2 | ECS Anywhere |
|------|---------|-----|--------------|
| 인프라 관리 | AWS | 사용자 | 사용자 |
| 스케일링 | 자동 | ASG | 수동 |
| GPU 지원 | X | O | O |
| 비용 모델 | 사용량 | 인스턴스 | 인스턴스당 |

### 네트워크 모드 비교

| 모드 | 설명 | Fargate |
|------|------|---------|
| awsvpc | 태스크별 ENI | 필수 |
| bridge | Docker 가상 네트워크 | X |
| host | 호스트 네트워크 | X |
| none | 네트워크 없음 | X |

---

## 참고 자료

### AWS 공식 문서
- [AWS ECS 개발자 가이드](https://docs.aws.amazon.com/ecs/)
- [AWS ECS API 레퍼런스](https://docs.aws.amazon.com/AmazonECS/latest/APIReference/)
- [AWS ECS 모범 사례](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/)

### 요금
- [Amazon ECS 요금](https://aws.amazon.com/ecs/pricing/)
- [AWS Fargate 요금](https://aws.amazon.com/fargate/pricing/)

### 도구
- [AWS Copilot CLI](https://aws.github.io/copilot-cli/)
- [ECS CLI](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ECS_CLI.html)
- [ECS Exec Checker](https://github.com/aws-containers/amazon-ecs-exec-checker)

---

## 폴더 구조

```
ECS/
├── index.md                          # 목차 및 개요
├── overview/                         # 개요
│   ├── what-is-ecs.md               # ECS 소개
│   └── terminology.md               # 용어 정의
├── clusters/                         # 클러스터
│   ├── cluster-overview.md          # 클러스터 개요
│   └── capacity-providers.md        # Capacity Provider
├── task-definitions/                 # 태스크 정의
│   ├── task-definition-overview.md  # Task Definition 개요
│   └── container-definitions.md     # Container Definition
├── services/                         # 서비스
│   ├── service-overview.md          # Service 개요
│   └── service-connect.md           # Service Connect
├── fargate/                          # Fargate
│   └── fargate-overview.md          # Fargate 개요
├── networking/                       # 네트워킹
│   └── networking-overview.md       # 네트워킹 개요
├── security/                         # 보안
│   └── security-overview.md         # 보안 개요
├── deployment/                       # 배포
│   └── deployment-strategies.md     # 배포 전략
├── auto-scaling/                     # Auto Scaling
│   └── auto-scaling-overview.md     # Auto Scaling 개요
├── ecs-anywhere/                     # ECS Anywhere
│   └── ecs-anywhere-overview.md     # ECS Anywhere 개요
└── operations/                       # 운영
    └── ecs-exec.md                  # ECS Exec
```
