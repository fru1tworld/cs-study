# Amazon ECS 용어 및 개념

## 핵심 용어

### Container (컨테이너)
애플리케이션 코드, 런타임, 시스템 도구, 라이브러리를 포함하는 경량화된 가상화 단위입니다.

```
┌──────────────────────────┐
│       Container          │
│  ┌────────────────────┐  │
│  │   Application Code │  │
│  ├────────────────────┤  │
│  │   Dependencies     │  │
│  ├────────────────────┤  │
│  │   Runtime          │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

### Task Definition (태스크 정의)
컨테이너 실행을 위한 청사진입니다.

**주요 설정 항목:**
- Docker 이미지 지정
- CPU/메모리 할당
- 네트워크 모드
- 환경 변수
- 볼륨 마운트
- IAM 역할
- 로깅 설정

### Task (태스크)
Task Definition의 인스턴스화된 실행 단위입니다.

| 상태 | 설명 |
|------|------|
| PROVISIONING | 리소스 준비 중 |
| PENDING | 실행 대기 중 |
| ACTIVATING | 시작 중 |
| RUNNING | 실행 중 |
| DEACTIVATING | 중지 중 |
| STOPPING | 종료 중 |
| DEPROVISIONING | 리소스 정리 중 |
| STOPPED | 종료됨 |

### Service (서비스)
원하는 수의 태스크를 지속적으로 유지하는 구성입니다.

**주요 특징:**
- 원하는 태스크 수 유지
- 실패한 태스크 자동 교체
- 로드 밸런서 통합
- 배포 전략 적용

### Cluster (클러스터)
태스크와 서비스의 논리적 그룹화입니다.

**클러스터 상태:**
| 상태 | 설명 |
|------|------|
| ACTIVE | 정상 운영 중 |
| PROVISIONING | 생성 중 |
| DEPROVISIONING | 삭제 중 |
| FAILED | 생성/삭제 실패 |
| INACTIVE | 비활성화됨 |

## 인프라 관련 용어

### Capacity Provider (용량 제공자)
태스크 실행을 위한 인프라 용량을 관리하는 구성입니다.

**유형:**
- **Fargate**: 서버리스 용량
- **Fargate Spot**: 할인된 서버리스 용량
- **Auto Scaling Group**: EC2 기반 용량

### Container Instance (컨테이너 인스턴스)
ECS 에이전트가 실행 중인 EC2 인스턴스입니다.

```
┌─────────────────────────────────────┐
│        Container Instance           │
│  ┌─────────────────────────────┐   │
│  │      ECS Agent              │   │
│  └─────────────────────────────┘   │
│  ┌─────────┐ ┌─────────┐           │
│  │Container│ │Container│           │
│  └─────────┘ └─────────┘           │
│              EC2 Instance           │
└─────────────────────────────────────┘
```

### Launch Type (시작 유형)
태스크가 실행되는 인프라 유형입니다.

| 유형 | 설명 |
|------|------|
| FARGATE | 서버리스 컴퓨팅 |
| EC2 | EC2 인스턴스 사용 |
| EXTERNAL | 온프레미스 서버 (ECS Anywhere) |

## 네트워킹 용어

### Network Mode (네트워크 모드)

| 모드 | 설명 | 지원 플랫폼 |
|------|------|------------|
| awsvpc | 태스크별 ENI 할당 (권장) | EC2, Fargate |
| bridge | Docker 내장 가상 네트워크 | EC2 (Linux) |
| host | 호스트 네트워크 직접 사용 | EC2 (Linux) |
| none | 네트워크 연결 없음 | EC2 (Linux) |
| default | Windows NAT 드라이버 | EC2 (Windows) |

### Service Connect
ECS 서비스 간 통신을 관리하는 기능입니다.

**주요 개념:**
- **Namespace**: 서비스들의 논리적 그룹
- **Port Name**: 태스크 정의의 포트 이름
- **Client Alias**: DNS 이름 및 포트 지정
- **Discovery Name**: Cloud Map 서비스 이름

### Service Discovery
AWS Cloud Map을 사용한 서비스 검색 기능입니다.

## 배포 관련 용어

### Deployment (배포)
서비스의 새 버전을 롤아웃하는 프로세스입니다.

**배포 유형:**
| 유형 | 설명 |
|------|------|
| Rolling Update | 점진적 교체 |
| Blue/Green | 새 환경으로 즉시 전환 |

### Deployment Configuration (배포 구성)

| 파라미터 | 설명 |
|----------|------|
| minimumHealthyPercent | 배포 중 유지해야 할 최소 태스크 비율 |
| maximumPercent | 배포 중 실행 가능한 최대 태스크 비율 |

### Circuit Breaker (서킷 브레이커)
배포 실패 시 자동으로 롤백하는 메커니즘입니다.

## 보안 관련 용어

### Task Role (태스크 역할)
컨테이너가 AWS API 호출 시 사용하는 IAM 역할입니다.

### Task Execution Role (태스크 실행 역할)
ECS 에이전트가 태스크 시작 시 사용하는 IAM 역할입니다.

**용도:**
- ECR에서 이미지 가져오기
- CloudWatch에 로그 전송
- Secrets Manager에서 시크릿 조회

### Container Instance Role (컨테이너 인스턴스 역할)
EC2 인스턴스가 ECS 서비스와 통신하는 데 사용하는 IAM 역할입니다.

## 스토리지 관련 용어

### Volume (볼륨)
컨테이너에서 사용하는 데이터 저장 공간입니다.

| 유형 | 설명 | Fargate 지원 |
|------|------|-------------|
| Amazon EFS | 탄력적 파일 시스템 | O |
| Amazon EBS | 블록 스토리지 | O (Linux만) |
| Bind Mount | 호스트 파일/디렉토리 | X |
| Docker Volume | Docker 관리 볼륨 | X |

### Ephemeral Storage (임시 스토리지)
Fargate 태스크에서 사용하는 임시 저장 공간입니다.

## 모니터링 관련 용어

### Container Insights
ECS 메트릭 및 로그를 CloudWatch로 수집하는 기능입니다.

### ECS Exec
실행 중인 컨테이너에 직접 접속하는 기능입니다.

## 참고 자료

- [AWS ECS 개발자 가이드](https://docs.aws.amazon.com/ecs/)
- [AWS ECS API 레퍼런스](https://docs.aws.amazon.com/AmazonECS/latest/APIReference/)
