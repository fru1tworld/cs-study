# IAM 역할

## IAM 역할이란?

IAM 역할은 특정 권한을 가진 AWS 자격 증명으로, 사용자와 달리 특정 개인에게 고유하게 연결되지 않습니다. 필요한 누구든(또는 무엇이든) 역할을 "맡아서(assume)" 임시로 해당 권한을 사용할 수 있습니다.

## 역할의 핵심 특징

### 임시 자격 증명
- 역할을 맡으면 임시 보안 자격 증명을 받음
- 비밀번호나 액세스 키 같은 장기 자격 증명이 없음
- 자격 증명은 일정 시간 후 자동 만료

### 위임 가능
- 사용자, 애플리케이션, AWS 서비스에 권한 위임
- 교차 계정 접근 가능
- 연합 사용자 지원

## 역할을 맡을 수 있는 대상

| 대상 | 설명 |
|------|------|
| IAM 사용자 | 같은 계정 또는 다른 AWS 계정의 사용자 |
| IAM 역할 | 같은 계정의 다른 역할 (역할 체이닝) |
| AWS 서비스 | EC2, Lambda, ECS 등 |
| 외부 사용자 | SAML 2.0, OIDC 인증 사용자 |
| 웹 자격 증명 | Amazon, Facebook, Google 인증 사용자 |

## IAM 사용자 vs IAM 역할

### 역할 선택이 권장되는 경우

```
✅ 역할 사용:
- EC2 인스턴스에서 실행되는 애플리케이션
- Lambda 함수
- 교차 계정 접근
- 임시 접근 권한
- 연합 사용자 (기업 ID 연동)
- 모바일 앱 사용자
```

### 사용자가 필요한 경우

```
⚠️ 사용자 필요:
- 역할을 지원하지 않는 워크로드 (일부 서드파티 플러그인)
- 타사 AWS 클라이언트 도구
- AWS CodeCommit 접근
- 비상 접근 시나리오
```

## 역할 구성 요소

### 1. 신뢰 정책 (Trust Policy)

역할을 맡을 수 있는 보안 주체를 정의합니다.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::123456789012:user/johndoe"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

### 2. 권한 정책 (Permissions Policy)

역할이 수행할 수 있는 작업을 정의합니다.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject"
            ],
            "Resource": "arn:aws:s3:::my-bucket/*"
        }
    ]
}
```

## 역할 유형별 사용 사례

### 1. AWS 서비스 역할

AWS 서비스가 사용자를 대신하여 작업을 수행할 때 사용합니다.

```json
// EC2 서비스 역할 신뢰 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ec2.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

일반적인 서비스 역할:
- EC2 인스턴스 프로필
- Lambda 실행 역할
- ECS 태스크 역할
- RDS 향상된 모니터링

### 2. 교차 계정 역할

다른 AWS 계정의 사용자에게 접근 권한을 부여합니다.

```json
// 계정 B의 역할 신뢰 정책 (계정 A 신뢰)
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::111111111111:root"
            },
            "Action": "sts:AssumeRole",
            "Condition": {
                "StringEquals": {
                    "sts:ExternalId": "unique-external-id"
                }
            }
        }
    ]
}
```

### 3. 서비스 연결 역할 (Service-Linked Role)

AWS 서비스가 자동으로 생성하고 관리하는 특별한 역할입니다.

```
특징:
- 서비스가 자동 생성
- 사전 정의된 권한
- 삭제 시 서비스 영향 고려 필요
- 이름 형식: AWSServiceRoleFor{ServiceName}
```

### 4. 연합 역할 (Federation Role)

외부 자격 증명 공급자로 인증된 사용자가 사용합니다.

```json
// SAML 연합 신뢰 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::123456789012:saml-provider/MyIdP"
            },
            "Action": "sts:AssumeRoleWithSAML",
            "Condition": {
                "StringEquals": {
                    "SAML:aud": "https://signin.aws.amazon.com/saml"
                }
            }
        }
    ]
}
```

## 역할 맡기 (Assuming a Role)

### AWS 콘솔에서 역할 전환

```
상단 메뉴 → 계정 이름 → 역할 전환
1. 계정 ID 입력
2. 역할 이름 입력
3. 표시 이름 설정 (선택)
4. 역할 전환 클릭
```

### AWS CLI로 역할 맡기

```bash
# 역할 맡기
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/MyRole \
    --role-session-name my-session

# 반환된 자격 증명 사용
export AWS_ACCESS_KEY_ID="ASIAXXXXXX"
export AWS_SECRET_ACCESS_KEY="xxxxxxxxxx"
export AWS_SESSION_TOKEN="xxxxxxxxxx"
```

### SDK에서 역할 맡기 (Python)

```python
import boto3

# STS 클라이언트 생성
sts_client = boto3.client('sts')

# 역할 맡기
response = sts_client.assume_role(
    RoleArn='arn:aws:iam::123456789012:role/MyRole',
    RoleSessionName='my-session',
    DurationSeconds=3600
)

# 임시 자격 증명으로 새 세션 생성
credentials = response['Credentials']
s3_client = boto3.client(
    's3',
    aws_access_key_id=credentials['AccessKeyId'],
    aws_secret_access_key=credentials['SecretAccessKey'],
    aws_session_token=credentials['SessionToken']
)
```

## 역할 체이닝 (Role Chaining)

한 역할에서 다른 역할로 전환하는 기능입니다.

```
사용자 → 역할 A → 역할 B → 역할 C

제한사항:
- 각 전환 시 새 세션 생성
- 최대 세션 기간 1시간으로 제한
- 원래 자격 증명 정보 일부 손실
```

## 역할 세션

### 세션 기간 설정

```json
// 역할의 최대 세션 기간 설정 (최대 12시간)
{
    "MaxSessionDuration": 43200
}
```

### 세션 정책

역할을 맡을 때 추가 제한을 적용할 수 있습니다.

```bash
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/MyRole \
    --role-session-name my-session \
    --policy '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":"s3:GetObject","Resource":"arn:aws:s3:::my-bucket/*"}]}'
```

## 역할 생성

### AWS 콘솔에서 생성

```
IAM 콘솔 → 역할 → 역할 생성
1. 신뢰할 엔터티 선택 (AWS 서비스, 계정, 웹 자격 증명 등)
2. 사용 사례 선택
3. 권한 정책 연결
4. 역할 이름 및 설명 입력
5. 역할 생성
```

### AWS CLI로 생성

```bash
# 신뢰 정책 파일 (trust-policy.json)
# 역할 생성
aws iam create-role \
    --role-name MyRole \
    --assume-role-policy-document file://trust-policy.json

# 권한 정책 연결
aws iam attach-role-policy \
    --role-name MyRole \
    --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

## 역할 제한 사항

| 항목 | 제한 |
|------|------|
| 계정당 역할 수 | 1,000 (증가 요청 가능) |
| 역할 이름 길이 | 64자 |
| 신뢰 정책 크기 | 2,048자 |
| 역할에 연결 가능한 관리형 정책 수 | 10 |
| 인라인 정책 크기 | 10,240자 |

## 역할 모범 사례

### 1. 최소 권한 원칙

```
✅ 권장:
- 필요한 최소 권한만 부여
- 정기적으로 사용하지 않는 권한 제거
- IAM Access Analyzer로 권한 분석
```

### 2. 외부 ID 사용

교차 계정 접근 시 혼동된 대리인 문제 방지:

```json
{
    "Condition": {
        "StringEquals": {
            "sts:ExternalId": "unique-random-string"
        }
    }
}
```

### 3. 세션 기간 제한

```
✅ 권장:
- 민감한 작업: 1시간 이하
- 일반 작업: 1-4시간
- 배치 작업: 필요한 만큼 (최대 12시간)
```

### 4. 태그 활용

```bash
aws iam tag-role \
    --role-name MyRole \
    --tags Key=Environment,Value=Production Key=Team,Value=DevOps
```

## 관련 문서

- [임시 보안 자격 증명](./temporary-credentials.md)
- [자격 증명 공급자](./identity-providers.md)
- [교차 계정 접근](../05_advanced/cross-account-access.md)
