# Amazon Machine Image (AMI)

## 개요

AMI(Amazon Machine Image)는 EC2 인스턴스를 시작하는 데 필요한 소프트웨어 구성이 포함된 템플릿입니다. 운영 체제, 애플리케이션 서버, 애플리케이션 등이 포함됩니다.

## AMI 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                        AMI                                   │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐   │
│  │              루트 볼륨 템플릿                          │   │
│  │  - 운영 체제                                          │   │
│  │  - 애플리케이션 소프트웨어                            │   │
│  │  - 구성 설정                                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              시작 권한                                 │   │
│  │  - 공개 / 비공개 / 특정 계정                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              블록 디바이스 매핑                        │   │
│  │  - EBS 볼륨 또는 인스턴스 스토어                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## AMI 유형

### 루트 볼륨 유형별

| 유형 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **EBS-backed** | 루트 볼륨이 EBS 볼륨 | 중지/시작 가능, 데이터 지속 | 약간 느린 시작 |
| **Instance Store-backed** | 루트 볼륨이 인스턴스 스토어 | 빠른 시작 | 중지 불가, 데이터 비지속 |

### 가상화 유형별

| 유형 | 설명 | 지원 |
|------|------|------|
| **HVM** | Hardware Virtual Machine | 현재 권장, 모든 인스턴스 타입 지원 |
| **PV** | Paravirtual | 레거시, 제한된 인스턴스 타입 |

### 아키텍처별

| 아키텍처 | 설명 |
|----------|------|
| **x86_64** | Intel/AMD 기반 인스턴스 |
| **arm64** | AWS Graviton 기반 인스턴스 |

## AMI 출처

### 1. AWS 제공 AMI
```bash
# Amazon Linux 2023 AMI 검색
aws ec2 describe-images \
    --owners amazon \
    --filters "Name=name,Values=al2023-ami-*-x86_64"
```

### 2. AWS Marketplace
- 타사 소프트웨어가 사전 설치된 AMI
- 무료 또는 유료
- 라이선스 포함

### 3. 커뮤니티 AMI
- 다른 AWS 사용자가 공유한 AMI
- 사용 전 검증 필요

### 4. 직접 생성한 AMI
- 기존 인스턴스에서 생성
- 조직 표준에 맞게 커스터마이징

## AMI 생성

### 콘솔에서 생성

1. EC2 콘솔에서 인스턴스 선택
2. 작업 > 이미지 및 템플릿 > 이미지 생성
3. 이미지 이름과 설명 입력
4. 재부팅 없음 옵션 선택 (선택사항)
5. 이미지 생성 클릭

### CLI로 생성

```bash
# 인스턴스에서 AMI 생성
aws ec2 create-image \
    --instance-id i-xxxxxxxxx \
    --name "My-Custom-AMI" \
    --description "My custom AMI description" \
    --no-reboot

# AMI 상태 확인
aws ec2 describe-images --image-ids ami-xxxxxxxxx
```

### 생성 프로세스

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  EC2 인스턴스 │────▶│  EBS 스냅샷  │────▶│     AMI      │
│              │     │   생성       │     │   등록       │
└──────────────┘     └──────────────┘     └──────────────┘
```

## AMI 공유

### 특정 계정과 공유

```bash
# AMI를 특정 계정과 공유
aws ec2 modify-image-attribute \
    --image-id ami-xxxxxxxxx \
    --launch-permission "Add=[{UserId=123456789012}]"

# 공유 권한 확인
aws ec2 describe-image-attribute \
    --image-id ami-xxxxxxxxx \
    --attribute launchPermission
```

### 공개 AMI로 설정

```bash
aws ec2 modify-image-attribute \
    --image-id ami-xxxxxxxxx \
    --launch-permission "Add=[{Group=all}]"
```

### 공유 시 고려사항

- 암호화된 AMI는 공유 제한이 있음
- 공유받은 AMI의 스냅샷은 복사해야 소유권 획득
- 공유받은 AMI로 시작한 인스턴스의 EBS 요금은 사용자 부담

## AMI 복사

### 리전 간 복사

```bash
# 다른 리전으로 AMI 복사
aws ec2 copy-image \
    --source-image-id ami-xxxxxxxxx \
    --source-region us-east-1 \
    --region ap-northeast-2 \
    --name "My-AMI-Copy"
```

### 복사 사용 사례

- 재해 복구를 위한 다중 리전 배포
- 글로벌 애플리케이션 배포
- 규정 준수를 위한 데이터 지역화

## AMI 암호화

### 암호화된 AMI 생성

```bash
# 암호화된 EBS 스냅샷으로 AMI 생성
aws ec2 register-image \
    --name "Encrypted-AMI" \
    --root-device-name /dev/xvda \
    --block-device-mappings "[{
        \"DeviceName\":\"/dev/xvda\",
        \"Ebs\":{
            \"SnapshotId\":\"snap-xxxxxxxxx\",
            \"Encrypted\":true,
            \"KmsKeyId\":\"arn:aws:kms:region:account:key/key-id\"
        }
    }]"
```

### 암호화 규칙

| 원본 AMI | 복사된 AMI |
|----------|------------|
| 암호화되지 않음 | 암호화 가능 |
| 암호화됨 | 반드시 암호화 |
| 기본 키로 암호화 | 다른 키로 재암호화 가능 |

## AMI 수명 주기 관리

### 사용하지 않는 AMI 정리

```bash
# AMI 등록 취소
aws ec2 deregister-image --image-id ami-xxxxxxxxx

# 연결된 스냅샷 삭제
aws ec2 delete-snapshot --snapshot-id snap-xxxxxxxxx
```

### AMI 수명 주기 자동화

AWS Backup 또는 Amazon Data Lifecycle Manager를 사용하여 AMI 생성 및 삭제를 자동화할 수 있습니다.

## EC2 Image Builder

### 개요

자동화된 이미지 파이프라인을 구축하여 보안 패치가 적용된 최신 AMI를 지속적으로 생성합니다.

### 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                    Image Builder Pipeline                    │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│   소스      │   빌드      │   테스트     │    배포         │
│   이미지    │   컴포넌트   │   컴포넌트   │    설정         │
└─────────────┴─────────────┴─────────────┴─────────────────┘
```

### 주요 기능

- 골든 이미지 자동 생성
- 보안 패치 자동 적용
- 테스트 자동화
- 다중 리전/계정 배포

## 모범 사례

1. ** 버전 관리**: AMI 이름에 날짜나 버전 번호 포함
2. ** 태깅**: 적절한 태그를 사용하여 AMI 구성
3. ** 정기적 업데이트**: 보안 패치가 적용된 새 AMI 주기적 생성
4. ** 최소 권한**: 필요한 소프트웨어만 포함
5. ** 암호화**: 민감한 데이터가 포함된 AMI는 암호화
6. ** 문서화**: AMI에 포함된 소프트웨어 및 구성 문서화

## 참고 자료

- [Amazon Machine Images](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html)
- [AMI 생성](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/creating-an-ami.html)
- [EC2 Image Builder](https://docs.aws.amazon.com/imagebuilder/latest/userguide/)
