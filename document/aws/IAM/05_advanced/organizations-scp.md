# AWS Organizations 서비스 제어 정책 (SCP)

## SCP란?

서비스 제어 정책(Service Control Policy)은 AWS Organizations에서 조직 내 계정의 최대 권한을 제어하는 정책입니다. SCP는 권한을 부여하지 않고, 자격 증명 기반 정책이 부여할 수 있는 권한의 상한선을 정의합니다.

## SCP의 특징

### 권한을 부여하지 않음

```
SCP의 역할:
- 사용 가능한 권한의 가드레일 설정
- 실제 권한 부여는 IAM 정책이 담당
- SCP에서 허용해도 IAM 정책 없으면 접근 불가

유효 권한 = IAM 정책 ∩ SCP
```

### 적용 범위

```
적용 대상:
- 멤버 계정의 모든 IAM 사용자
- 멤버 계정의 모든 IAM 역할
- 멤버 계정의 루트 사용자

적용 안 됨:
- 관리 계정 (Management Account)
- 서비스 연결 역할 (SLR)
- 조직의 보안 주체가 아닌 외부 접근
```

### 적용 계층

```
루트 (Root)
    │
    ├── OU: Production
    │       ├── 계정 A (SCP 상속)
    │       └── 계정 B (SCP 상속)
    │
    └── OU: Development
            ├── OU: Dev-Team1
            │       └── 계정 C (중첩 SCP 상속)
            └── 계정 D (SCP 상속)

SCP는 조직 루트, OU, 계정 수준에서 연결 가능
하위 항목은 상위의 모든 SCP를 상속
```

## SCP 유형

### 거부 목록 (Deny List) 전략

기본적으로 모든 작업을 허용하고, 특정 작업만 명시적으로 거부합니다.

```json
// FullAWSAccess SCP (기본)
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "*",
            "Resource": "*"
        }
    ]
}

// 특정 서비스 거부 SCP (추가)
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Deny",
            "Action": [
                "iam:CreateUser",
                "iam:DeleteUser"
            ],
            "Resource": "*"
        }
    ]
}
```

### 허용 목록 (Allow List) 전략

기본 FullAWSAccess를 제거하고, 필요한 서비스만 명시적으로 허용합니다.

```json
// 필요한 서비스만 허용
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:*",
                "s3:*",
                "lambda:*",
                "cloudwatch:*"
            ],
            "Resource": "*"
        }
    ]
}
```

**주의:** 허용 목록 전략 사용 시 필요한 모든 서비스와 작업을 명시해야 합니다.

## 일반적인 SCP 예시

### 리전 제한

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyOutsideApprovedRegions",
            "Effect": "Deny",
            "NotAction": [
                "iam:*",
                "organizations:*",
                "route53:*",
                "cloudfront:*",
                "support:*",
                "budgets:*"
            ],
            "Resource": "*",
            "Condition": {
                "StringNotEquals": {
                    "aws:RequestedRegion": [
                        "us-east-1",
                        "ap-northeast-2"
                    ]
                }
            }
        }
    ]
}
```

### 루트 사용자 제한

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyRootUser",
            "Effect": "Deny",
            "Action": "*",
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "aws:PrincipalArn": "arn:aws:iam::*:root"
                }
            }
        }
    ]
}
```

### VPC 삭제 방지

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyVPCDeletion",
            "Effect": "Deny",
            "Action": [
                "ec2:DeleteVpc",
                "ec2:DeleteSubnet",
                "ec2:DeleteInternetGateway"
            ],
            "Resource": "*"
        }
    ]
}
```

### CloudTrail 비활성화 방지

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "ProtectCloudTrail",
            "Effect": "Deny",
            "Action": [
                "cloudtrail:DeleteTrail",
                "cloudtrail:StopLogging",
                "cloudtrail:UpdateTrail"
            ],
            "Resource": "*"
        }
    ]
}
```

### 특정 인스턴스 유형만 허용

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyLargeInstances",
            "Effect": "Deny",
            "Action": "ec2:RunInstances",
            "Resource": "arn:aws:ec2:*:*:instance/*",
            "Condition": {
                "ForAnyValue:StringNotLike": {
                    "ec2:InstanceType": [
                        "t3.*",
                        "t3a.*",
                        "m5.*"
                    ]
                }
            }
        }
    ]
}
```

### 암호화 강제

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyUnencryptedVolumes",
            "Effect": "Deny",
            "Action": "ec2:CreateVolume",
            "Resource": "*",
            "Condition": {
                "Bool": {
                    "ec2:Encrypted": "false"
                }
            }
        },
        {
            "Sid": "DenyUnencryptedS3Uploads",
            "Effect": "Deny",
            "Action": "s3:PutObject",
            "Resource": "*",
            "Condition": {
                "Null": {
                    "s3:x-amz-server-side-encryption": "true"
                }
            }
        }
    ]
}
```

### IMDSv2 강제

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "RequireIMDSv2",
            "Effect": "Deny",
            "Action": "ec2:RunInstances",
            "Resource": "arn:aws:ec2:*:*:instance/*",
            "Condition": {
                "StringNotEquals": {
                    "ec2:MetadataHttpTokens": "required"
                }
            }
        }
    ]
}
```

## SCP 관리

### SCP 생성

```bash
aws organizations create-policy \
    --name "DenyUnapprovedRegions" \
    --description "Deny access to unapproved regions" \
    --content file://scp-deny-regions.json \
    --type SERVICE_CONTROL_POLICY
```

### SCP 연결

```bash
# 루트에 연결
aws organizations attach-policy \
    --policy-id p-xxxxxxxx \
    --target-id r-xxxx

# OU에 연결
aws organizations attach-policy \
    --policy-id p-xxxxxxxx \
    --target-id ou-xxxx-xxxxxxxx

# 계정에 연결
aws organizations attach-policy \
    --policy-id p-xxxxxxxx \
    --target-id 123456789012
```

### SCP 분리

```bash
aws organizations detach-policy \
    --policy-id p-xxxxxxxx \
    --target-id ou-xxxx-xxxxxxxx
```

## SCP 평가 로직

### 단일 계정 내 평가

```
요청 → SCP 확인 → IAM 정책 확인 → 권한 경계 확인 → 최종 결정

SCP에서:
- 명시적 거부 → 즉시 거부
- 허용 없음 → 암묵적 거부
- 허용 → IAM 정책으로 이동
```

### 계층적 SCP 평가

```
상위 OU의 SCP + 현재 수준의 SCP = 효과적인 SCP

모든 수준에서 허용되어야 최종 허용
어느 수준에서든 거부되면 최종 거부
```

## SCP 제한 사항

| 항목 | 제한 |
|------|------|
| 조직당 최대 SCP 수 | 1,000 |
| 루트/OU/계정당 연결된 SCP 수 | 5 |
| 중첩 OU 최대 깊이 | 5 |
| SCP 문서 최대 크기 | 5,120자 |

## 모범 사례

### 1. 거부 목록 전략 권장

```
✅ FullAWSAccess 유지
✅ 필요한 거부만 추가
✅ 관리가 더 용이함
```

### 2. 단계적 적용

```
1. 테스트 OU에서 먼저 적용
2. 영향 분석
3. 점진적으로 확대
```

### 3. 필수 서비스 예외

```
거부 정책에서 제외해야 할 서비스:
- CloudTrail (감사용)
- AWS Config (규정 준수)
- Organizations (관리용)
- IAM (자격 증명 관리)
```

### 4. 테스트 환경 구성

```
OU 구조:
├── Root
│   ├── Production (엄격한 SCP)
│   ├── Development (완화된 SCP)
│   └── Sandbox (테스트용)
```

### 5. 문서화

```
각 SCP에 대해 문서화:
- 목적
- 적용 대상
- 영향 범위
- 예외 사항
```

## 문제 해결

### AccessDenied 원인 파악

```
확인 순서:
1. SCP에서 거부되는지 확인
2. IAM 정책에서 허용하는지 확인
3. 권한 경계에서 허용하는지 확인
4. 리소스 기반 정책 확인
```

### SCP 시뮬레이션

IAM Policy Simulator는 SCP를 고려하지 않으므로, 실제 테스트가 필요합니다.

```bash
# 테스트 계정에서 실제 API 호출
aws ec2 run-instances ... --dry-run
```

## 관련 문서

- [정책 평가 로직](../03_policies/evaluation-logic.md)
- [권한 경계](../03_policies/permissions-boundary.md)
- [IAM Access Analyzer](./access-analyzer.md)
