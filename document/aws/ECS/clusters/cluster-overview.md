# Amazon ECS 클러스터

## 개요

ECS 클러스터는 **컨테이너화된 애플리케이션을 위한 인프라 용량을 제공하는 태스크 또는 서비스의 논리적 그룹화**입니다. 리소스를 분리하고 관리하기 위한 기본 단위로 사용됩니다.

## 클러스터 유형

### 1. ECS Managed Instances (권장)

AWS가 EC2 인스턴스 전체를 관리하는 방식입니다.

**특징:**
- 자동 프로비저닝
- 자동 패칭
- 자동 스케일링
- 성능과 비용 효율성의 최적 균형

**권장 대상:** 대부분의 새로운 워크로드

### 2. AWS Fargate (서버리스)

인프라 관리 없이 컨테이너를 실행하는 방식입니다.

**특징:**
- 서버 프로비저닝/관리 불필요
- 사용한 리소스에만 비용 지불
- 빠른 배포 및 스케일링
- 태스크 수준의 격리

**권장 대상:** 변동하는 워크로드, 운영 부담 최소화

### 3. Amazon EC2 Instances (완전 제어)

사용자가 EC2 인스턴스를 직접 관리하는 방식입니다.

**특징:**
- 특정 인스턴스 유형 선택 가능
- 기존 인프라 활용 가능
- 최대 제어 권한
- GPU 인스턴스 사용 가능

**권장 대상:** 특수 요구사항이 있는 워크로드

## 클러스터 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                         Cluster                              │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Capacity Providers                   │ │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────────────┐    │ │
│  │  │ Fargate  │  │Fargate   │  │ Auto Scaling Group│    │ │
│  │  │          │  │Spot      │  │                   │    │ │
│  │  └──────────┘  └──────────┘  └───────────────────┘    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Service A  │  │   Service B  │  │   Service C  │      │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │      │
│  │  │ Task 1 │  │  │  │ Task 1 │  │  │  │ Task 1 │  │      │
│  │  │ Task 2 │  │  │  │ Task 2 │  │  │  └────────┘  │      │
│  │  └────────┘  │  │  └────────┘  │  └──────────────┘      │
│  └──────────────┘  └──────────────┘                         │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    VPC & Subnets                        │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 클러스터 상태

| 상태 | 설명 |
|------|------|
| **ACTIVE** | 정상적으로 운영 중 |
| **PROVISIONING** | 클러스터 생성 중 |
| **DEPROVISIONING** | 클러스터 삭제 중 |
| **FAILED** | 생성 또는 삭제 실패 |
| **INACTIVE** | 비활성화됨 |

## 클러스터 구성 요소

### 필수 구성 요소

1. **VPC**: 클러스터가 실행될 네트워크
2. **서브넷**: 태스크가 배치될 서브넷

### 선택적 구성 요소

1. **Capacity Provider**: 자동 스케일링 관리
2. **Service Connect Namespace**: 서비스 간 통신
3. **Container Insights**: 모니터링 활성화
4. **Tags**: 리소스 분류 및 비용 추적

## 클러스터 생성

### AWS CLI 사용

```bash
# 기본 클러스터 생성
aws ecs create-cluster --cluster-name my-cluster

# Fargate 용량 제공자와 함께 생성
aws ecs create-cluster \
    --cluster-name my-cluster \
    --capacity-providers FARGATE FARGATE_SPOT \
    --default-capacity-provider-strategy \
        capacityProvider=FARGATE,weight=1 \
        capacityProvider=FARGATE_SPOT,weight=1

# Container Insights 활성화
aws ecs create-cluster \
    --cluster-name my-cluster \
    --settings name=containerInsights,value=enabled
```

### AWS Management Console

1. ECS 콘솔 접속
2. "Create Cluster" 클릭
3. 클러스터 이름 입력
4. 인프라 옵션 선택 (Fargate, EC2, 또는 혼합)
5. 네트워킹 설정
6. 모니터링 설정
7. 태그 추가 (선택)
8. "Create" 클릭

## 클러스터 설정

### 기본 용량 제공자 전략

```json
{
  "defaultCapacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE",
      "weight": 1,
      "base": 1
    },
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 4
    }
  ]
}
```

| 파라미터 | 설명 |
|----------|------|
| **capacityProvider** | 사용할 용량 제공자 이름 |
| **weight** | 태스크 배치 비율 |
| **base** | 최소 태스크 수 (첫 번째 제공자만) |

### Container Insights 설정

```bash
# 활성화
aws ecs update-cluster-settings \
    --cluster my-cluster \
    --settings name=containerInsights,value=enabled

# 비활성화
aws ecs update-cluster-settings \
    --cluster my-cluster \
    --settings name=containerInsights,value=disabled
```

## 혼합 용량 사용

하나의 클러스터에서 여러 인프라 유형을 함께 사용할 수 있습니다.

```
┌─────────────────────────────────────────────────┐
│                    Cluster                       │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │   Fargate    │  │     EC2      │             │
│  │   Service    │  │   Service    │             │
│  └──────────────┘  └──────────────┘             │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │            Fargate Spot Tasks              │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**사용 사례:**
- 중요 워크로드: Fargate
- 비용 절감 워크로드: Fargate Spot
- GPU 워크로드: EC2

## 클러스터 관리

### 클러스터 조회

```bash
# 모든 클러스터 목록
aws ecs list-clusters

# 클러스터 상세 정보
aws ecs describe-clusters --clusters my-cluster
```

### 클러스터 삭제

```bash
# 클러스터 삭제 전 모든 서비스와 태스크를 먼저 삭제해야 함
aws ecs delete-cluster --cluster my-cluster
```

## 모범 사례

### 1. 환경별 클러스터 분리

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ dev-cluster │  │ stg-cluster │  │ prd-cluster │
└─────────────┘  └─────────────┘  └─────────────┘
```

### 2. 태그 활용

```bash
aws ecs tag-resource \
    --resource-arn arn:aws:ecs:region:account:cluster/my-cluster \
    --tags key=Environment,value=production \
           key=Team,value=platform
```

### 3. 용량 제공자 전략 활용

- **비용 최적화**: Fargate Spot 비중 높이기
- **안정성 우선**: Fargate 또는 EC2 사용
- **혼합**: base로 최소 보장, weight로 분산

### 4. 모니터링 설정

- Container Insights 활성화
- CloudWatch 알람 설정
- 비용 모니터링 활성화

## 제한 사항

| 항목 | 기본 한도 | 조정 가능 |
|------|----------|----------|
| 리전당 클러스터 수 | 10,000 | 요청 시 |
| 클러스터당 서비스 수 | 5,000 | 요청 시 |
| 클러스터당 태스크 수 | 무제한 | - |

## 참고 자료

- [AWS ECS 클러스터 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/clusters.html)
- [Capacity Provider 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/capacity-providers.html)
