# Amazon EC2 개요

## Amazon EC2란?

Amazon Elastic Compute Cloud(Amazon EC2)는 AWS 클라우드에서 확장 가능한 컴퓨팅 용량을 제공하는 웹 서비스입니다. EC2를 사용하면 하드웨어에 선투자할 필요 없이 더 빠르게 애플리케이션을 개발하고 배포할 수 있습니다.

## 주요 특징

### 탄력성(Elasticity)
- 몇 분 만에 용량을 늘리거나 줄일 수 있음
- 수백 또는 수천 개의 서버 인스턴스를 동시에 관리 가능

### 완전한 제어
- 각 인스턴스에 대한 루트 액세스 권한
- 일반 시스템처럼 인스턴스를 중지하고 데이터를 유지한 채 재시작 가능

### 유연성
- 여러 인스턴스 타입, 운영 체제, 소프트웨어 패키지 선택 가능
- 메모리, CPU, 스토리지, 부트 파티션 크기를 선택하여 최적 구성 가능

### 통합성
- 대부분의 AWS 서비스와 통합
- Amazon S3, Amazon RDS, Amazon VPC 등과 함께 완전한 솔루션 구축 가능

### 신뢰성
- SLA 99.99% 가용성 보장
- 각 리전은 격리된 다중 가용 영역으로 구성

### 보안
- Amazon VPC와 연동하여 네트워크 보안 강화
- 보안 그룹과 네트워크 ACL로 트래픽 제어

### 비용 효율성
- 사용한 만큼만 지불
- 다양한 요금 옵션(On-Demand, Reserved, Spot, Savings Plans)

## EC2의 핵심 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| ** 인스턴스** | 클라우드에서 실행되는 가상 서버 |
| **AMI** | 인스턴스 시작에 필요한 소프트웨어 구성이 포함된 템플릿 |
| ** 인스턴스 타입** | CPU, 메모리, 스토리지, 네트워킹 용량의 다양한 조합 |
| ** 키 페어** | 인스턴스 연결을 위한 보안 자격 증명 |
| ** 보안 그룹** | 인스턴스의 가상 방화벽 |
| **EBS 볼륨** | 영구 블록 스토리지 |
| ** 인스턴스 스토어** | 임시 블록 스토리지 |
| **VPC** | 논리적으로 격리된 가상 네트워크 |
| ** 리전과 가용 영역** | 인스턴스와 리소스의 물리적 위치 |

## EC2 접근 방법

### AWS Management Console
웹 기반 인터페이스로 EC2 리소스를 생성하고 관리

### AWS CLI (Command Line Interface)
```bash
# 인스턴스 목록 조회
aws ec2 describe-instances

# 인스턴스 시작
aws ec2 run-instances --image-id ami-xxxxxxxx --instance-type t2.micro

# 인스턴스 중지
aws ec2 stop-instances --instance-ids i-xxxxxxxxx
```

### AWS SDK
다양한 프로그래밍 언어(Python, Java, JavaScript 등)로 EC2 리소스 제어

### AWS CloudFormation
인프라를 코드로 정의하고 배포

### EC2 Query API
HTTPS 요청을 통한 직접 API 호출

## 관련 AWS 서비스

| 서비스 | 설명 |
|--------|------|
| **EC2 Auto Scaling** | 수요에 따라 인스턴스 수 자동 조절 |
| **Amazon CloudWatch** | 인스턴스 및 리소스 모니터링 |
| **Elastic Load Balancing** | 여러 인스턴스에 트래픽 분산 |
| **Amazon EBS** | 영구 블록 스토리지 |
| **Amazon VPC** | 격리된 가상 네트워크 |
| **AWS IAM** | EC2 리소스 접근 제어 |
| **AWS Systems Manager** | 인스턴스 관리 및 자동화 |

## 참고 자료

- [AWS EC2 공식 문서](https://docs.aws.amazon.com/ec2/)
- [Amazon EC2 User Guide](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/)
- [EC2 시작하기](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html)
