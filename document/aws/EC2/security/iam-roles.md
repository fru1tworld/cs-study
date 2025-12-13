# EC2 IAM 역할

## 개요

IAM 역할을 EC2 인스턴스에 연결하면 인스턴스에서 실행되는 애플리케이션이 AWS 서비스에 안전하게 액세스할 수 있습니다. 자격 증명을 인스턴스에 직접 저장하지 않아도 됩니다.

## 작동 원리

```
┌─────────────────────────────────────────────────────────────┐
│                    IAM 역할 작동 방식                        │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ EC2 인스턴스  │───▶│  IAM 역할    │───▶│ AWS 서비스   │  │
│  │              │    │              │    │ (S3, DDB...) │  │
│  │ 애플리케이션  │◀───│ 임시 자격증명 │    │              │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                    ▲                              │
│         │                    │                              │
│         └────────────────────┘                              │
│         메타데이터 서비스에서                                │
│         자격 증명 요청                                       │
└─────────────────────────────────────────────────────────────┘
```

## 구성 요소

### 1. IAM 역할 (Role)

```json
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

### 2. IAM 정책 (Policy)

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

### 3. 인스턴스 프로파일 (Instance Profile)

IAM 역할을 EC2 인스턴스에 연결하는 컨테이너입니다.

## IAM 역할 생성 및 연결

### 역할 생성

```bash
# 1. 신뢰 정책 파일 생성
cat > trust-policy.json << 'EOF'
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
EOF

# 2. IAM 역할 생성
aws iam create-role \
    --role-name MyEC2Role \
    --assume-role-policy-document file://trust-policy.json

# 3. 정책 연결
aws iam attach-role-policy \
    --role-name MyEC2Role \
    --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess

# 4. 인스턴스 프로파일 생성
aws iam create-instance-profile \
    --instance-profile-name MyEC2InstanceProfile

# 5. 역할을 인스턴스 프로파일에 추가
aws iam add-role-to-instance-profile \
    --instance-profile-name MyEC2InstanceProfile \
    --role-name MyEC2Role
```

### 인스턴스에 연결

```bash
# 새 인스턴스 시작 시
aws ec2 run-instances \
    --image-id ami-xxxxxxxxx \
    --instance-type t3.micro \
    --iam-instance-profile Name=MyEC2InstanceProfile

# 기존 인스턴스에 연결
aws ec2 associate-iam-instance-profile \
    --instance-id i-xxxxxxxxx \
    --iam-instance-profile Name=MyEC2InstanceProfile

# 인스턴스 프로파일 교체
aws ec2 replace-iam-instance-profile-association \
    --association-id iip-assoc-xxxxxxxxx \
    --iam-instance-profile Name=NewInstanceProfile
```

## 자격 증명 사용

### 메타데이터에서 자격 증명 조회

```bash
# 토큰 발급 (IMDSv2)
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")

# 역할 이름 확인
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/

# 자격 증명 조회
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/MyEC2Role
```

응답 예시:
```json
{
    "Code": "Success",
    "LastUpdated": "2024-01-15T12:00:00Z",
    "Type": "AWS-HMAC",
    "AccessKeyId": "ASIAXXXXXXXXXXX",
    "SecretAccessKey": "xxxxxxxxxxxxxxxxxxxxxxxx",
    "Token": "IQoJb3JpZ2luX2VjE...",
    "Expiration": "2024-01-15T18:00:00Z"
}
```

### AWS SDK 자동 사용

AWS SDK는 자동으로 인스턴스 프로파일 자격 증명을 사용합니다.

```python
import boto3

# 자격 증명 자동 사용
s3 = boto3.client('s3')
s3.list_buckets()  # 역할 권한으로 실행
```

```bash
# AWS CLI도 자동 사용
aws s3 ls  # 역할 권한으로 실행
```

## 자격 증명 체인

AWS SDK는 다음 순서로 자격 증명을 검색합니다:

```
┌─────────────────────────────────────────────────────────────┐
│                    자격 증명 검색 순서                        │
├─────────────────────────────────────────────────────────────┤
│  1. 환경 변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)    │
│                        ↓                                    │
│  2. 공유 자격 증명 파일 (~/.aws/credentials)                 │
│                        ↓                                    │
│  3. ECS 컨테이너 자격 증명                                   │
│                        ↓                                    │
│  4. EC2 인스턴스 프로파일 자격 증명  ← EC2에서 사용           │
└─────────────────────────────────────────────────────────────┘
```

## 일반적인 정책 패턴

### S3 액세스

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::my-bucket/*"
        },
        {
            "Effect": "Allow",
            "Action": "s3:ListBucket",
            "Resource": "arn:aws:s3:::my-bucket"
        }
    ]
}
```

### DynamoDB 액세스

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/MyTable"
        }
    ]
}
```

### Systems Manager 액세스

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:GetParameter",
                "ssm:GetParameters",
                "ssm:GetParametersByPath"
            ],
            "Resource": "arn:aws:ssm:*:*:parameter/myapp/*"
        }
    ]
}
```

### CloudWatch Logs

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "*"
        }
    ]
}
```

## AWS 관리형 정책

자주 사용되는 AWS 관리형 정책:

| 정책 이름 | 용도 |
|-----------|------|
| `AmazonS3ReadOnlyAccess` | S3 읽기 전용 |
| `AmazonS3FullAccess` | S3 전체 액세스 |
| `AmazonDynamoDBReadOnlyAccess` | DynamoDB 읽기 전용 |
| `CloudWatchAgentServerPolicy` | CloudWatch 에이전트 |
| `AmazonSSMManagedInstanceCore` | Systems Manager 핵심 |
| `AmazonEC2ContainerRegistryReadOnly` | ECR 읽기 전용 |

## 모범 사례

### 1. 최소 권한 원칙

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::my-bucket/specific-prefix/*",
            "Condition": {
                "StringEquals": {
                    "aws:RequestedRegion": "ap-northeast-2"
                }
            }
        }
    ]
}
```

### 2. 리소스 기반 제한

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "ec2:DescribeInstances",
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "ec2:ResourceTag/Environment": "Production"
                }
            }
        }
    ]
}
```

### 3. 역할 분리

```
┌─────────────────────────────────────────────────────────────┐
│                    역할 분리 예시                            │
├─────────────────────────────────────────────────────────────┤
│  웹 서버 역할                                                │
│  - S3 정적 콘텐츠 읽기                                       │
│  - CloudWatch Logs 쓰기                                     │
│                                                              │
│  앱 서버 역할                                                │
│  - DynamoDB 읽기/쓰기                                        │
│  - SQS 메시지 처리                                          │
│  - Secrets Manager 액세스                                    │
│                                                              │
│  배치 처리 역할                                              │
│  - S3 전체 액세스                                           │
│  - SNS 알림 전송                                            │
└─────────────────────────────────────────────────────────────┘
```

### 4. 권한 경계 사용

```bash
aws iam put-role-permissions-boundary \
    --role-name MyEC2Role \
    --permissions-boundary arn:aws:iam::xxx:policy/MyPermissionsBoundary
```

## 문제 해결

### 권한 확인

```bash
# 현재 자격 증명 확인
aws sts get-caller-identity

# 특정 작업 권한 시뮬레이션
aws iam simulate-principal-policy \
    --policy-source-arn arn:aws:iam::xxx:role/MyEC2Role \
    --action-names s3:GetObject \
    --resource-arns arn:aws:s3:::my-bucket/file.txt
```

### 일반적인 오류

| 오류 | 원인 | 해결 |
|------|------|------|
| `AccessDenied` | 권한 부족 | 정책 확인 및 추가 |
| `InvalidIdentityToken` | 자격 증명 만료 | SDK 자동 갱신 확인 |
| `NoCredentialProviders` | 역할 미연결 | 인스턴스 프로파일 확인 |

## 참고 자료

- [EC2용 IAM 역할](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html)
- [IAM 정책 가이드](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html)
- [보안 모범 사례](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
