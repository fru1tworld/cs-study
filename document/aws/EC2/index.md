# Amazon EC2 문서

> Amazon Elastic Compute Cloud(EC2) 공식 문서 정리

## 목차

### [개요](./overview.md)
- EC2 소개
- 주요 특징
- 핵심 구성 요소
- 관련 AWS 서비스

---

### 인스턴스

#### [인스턴스 타입](./instances/instance-types.md)
- 인스턴스 타입 명명 규칙
- 인스턴스 패밀리 (범용, 컴퓨팅 최적화, 메모리 최적화, 스토리지 최적화, 가속 컴퓨팅)
- 프로세서 옵션 (Intel, AMD, Graviton)
- 하이퍼바이저 (Nitro, Xen)

#### [인스턴스 라이프사이클](./instances/lifecycle.md)
- 인스턴스 상태 (pending, running, stopping, stopped, terminated)
- 재부팅, 중지/시작, 휴면화, 종료
- 종료 보호 및 중지 보호
- 요금 청구 규칙

#### [AMI (Amazon Machine Image)](./instances/ami.md)
- AMI 구성 요소
- AMI 유형 (EBS-backed, Instance Store-backed)
- AMI 생성 및 관리
- AMI 공유 및 복사
- EC2 Image Builder

#### [인스턴스 메타데이터](./instances/metadata.md)
- IMDS (Instance Metadata Service)
- IMDSv1 vs IMDSv2
- 메타데이터 카테고리
- 사용자 데이터 (User Data)
- 보안 모범 사례

#### [배치 그룹](./instances/placement-groups.md)
- 클러스터 배치 그룹
- 분산 배치 그룹
- 파티션 배치 그룹
- 배치 전략 선택 가이드

---

### 네트워킹

#### [VPC](./networking/vpc.md)
- VPC 기본 구성 요소
- 서브넷 (퍼블릭/프라이빗)
- 인터넷 게이트웨이 및 NAT 게이트웨이
- 라우팅 테이블
- 네트워크 ACL
- VPC 피어링 및 엔드포인트

#### [ENI (Elastic Network Interface)](./networking/eni.md)
- ENI 구성 요소
- 기본 vs 보조 네트워크 인터페이스
- 다중 ENI 아키텍처
- ENI와 장애 조치
- 향상된 네트워킹 (ENA, EFA)

#### [Elastic IP](./networking/elastic-ip.md)
- Elastic IP 특징
- EIP vs 퍼블릭 IP
- EIP 관리 및 요금
- 고가용성을 위한 EIP 사용
- BYOIP (Bring Your Own IP)

---

### 스토리지

#### [Amazon EBS](./storage/ebs.md)
- EBS 볼륨 타입 (gp3, gp2, io2, st1, sc1)
- EBS 볼륨 관리
- EBS 성능 최적화
- EBS 스냅샷
- EBS 암호화
- Multi-Attach

#### [인스턴스 스토어](./storage/instance-store.md)
- 인스턴스 스토어 특징
- 인스턴스 스토어 vs EBS
- 데이터 지속성
- NVMe 인스턴스 스토어
- 사용 사례 및 모범 사례

---

### 보안

#### [보안 그룹](./security/security-groups.md)
- 보안 그룹 특징
- 인바운드/아웃바운드 규칙
- 일반적인 보안 그룹 패턴
- Stateful 특성
- 보안 그룹 참조
- 모범 사례

#### [키 페어](./security/key-pairs.md)
- 키 페어 작동 원리
- RSA vs ED25519
- 키 페어 생성 및 관리
- SSH 연결
- 키 페어 교체
- 프라이빗 키 분실 시 복구

#### [IAM 역할](./security/iam-roles.md)
- IAM 역할 작동 원리
- 인스턴스 프로파일
- 임시 자격 증명
- 일반적인 정책 패턴
- 최소 권한 원칙

---

### 모니터링

#### [CloudWatch](./monitoring/cloudwatch.md)
- CloudWatch 지표
- 기본 모니터링 vs 세부 모니터링
- CloudWatch 에이전트
- CloudWatch 알람
- CloudWatch 대시보드
- 상태 검사 및 자동 복구
- CloudWatch Logs

---

### Auto Scaling

#### [EC2 Auto Scaling](./auto-scaling/overview.md)
- 시작 템플릿 vs 시작 구성
- Auto Scaling 그룹
- 스케일링 정책 (대상 추적, 단계 조정, 단순 조정, 예측)
- 예약된 스케일링
- 수명 주기 후크
- 인스턴스 혼합 (Spot + On-Demand)
- 종료 정책

---

### 요금

#### [EC2 요금](./pricing/overview.md)
- 구매 옵션 비교
- On-Demand 인스턴스
- Savings Plans
- Reserved Instances
- Spot 인스턴스
- Dedicated Hosts/Instances
- 추가 요금 요소 (데이터 전송, IPv4, EBS)
- 비용 최적화 전략

---

## 빠른 참조

### AWS CLI 명령어

```bash
# 인스턴스 시작
aws ec2 run-instances --image-id ami-xxx --instance-type t3.micro

# 인스턴스 목록
aws ec2 describe-instances

# 인스턴스 중지
aws ec2 stop-instances --instance-ids i-xxx

# 인스턴스 시작
aws ec2 start-instances --instance-ids i-xxx

# 인스턴스 종료
aws ec2 terminate-instances --instance-ids i-xxx
```

### 유용한 링크

- [AWS EC2 공식 문서](https://docs.aws.amazon.com/ec2/)
- [EC2 User Guide](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/)
- [EC2 요금](https://aws.amazon.com/ec2/pricing/)
- [EC2 인스턴스 타입](https://aws.amazon.com/ec2/instance-types/)
- [AWS Pricing Calculator](https://calculator.aws/)

---

## 문서 구조

```
AWS/EC2/
├── index.md                    # 목차 (현재 문서)
├── overview.md                 # EC2 개요
├── instances/
│   ├── instance-types.md       # 인스턴스 타입
│   ├── lifecycle.md            # 인스턴스 라이프사이클
│   ├── ami.md                  # AMI
│   ├── metadata.md             # 인스턴스 메타데이터
│   └── placement-groups.md     # 배치 그룹
├── networking/
│   ├── vpc.md                  # VPC
│   ├── eni.md                  # ENI
│   └── elastic-ip.md           # Elastic IP
├── storage/
│   ├── ebs.md                  # EBS
│   └── instance-store.md       # 인스턴스 스토어
├── security/
│   ├── security-groups.md      # 보안 그룹
│   ├── key-pairs.md            # 키 페어
│   └── iam-roles.md            # IAM 역할
├── monitoring/
│   └── cloudwatch.md           # CloudWatch
├── auto-scaling/
│   └── overview.md             # Auto Scaling
└── pricing/
    └── overview.md             # 요금
```
