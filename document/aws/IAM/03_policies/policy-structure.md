# IAM 정책 구조 (JSON)

## JSON 정책 기본 구조

```json
{
    "Version": "2012-10-17",
    "Id": "PolicyId",
    "Statement": [
        {
            "Sid": "StatementId",
            "Effect": "Allow",
            "Principal": {"AWS": "arn:aws:iam::123456789012:root"},
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::bucket/*",
            "Condition": {
                "StringEquals": {
                    "s3:prefix": "home/"
                }
            }
        }
    ]
}
```

## 정책 요소 상세 설명

### Version (선택)

정책 언어 버전을 지정합니다.

```json
"Version": "2012-10-17"
```

| 버전 | 설명 |
|------|------|
| `2012-10-17` | 현재 버전, 모든 기능 지원 (권장) |
| `2008-10-17` | 이전 버전, 일부 기능 미지원 |

** 주의:** 버전을 생략하면 `2008-10-17`이 적용되어 정책 변수(`${...}`) 등을 사용할 수 없습니다.

### Id (선택)

정책의 식별자입니다. 주로 리소스 기반 정책에서 사용합니다.

```json
"Id": "S3BucketPolicy-MyBucket"
```

### Statement (필수)

정책의 주요 요소로, 하나 이상의 개별 문장을 포함합니다.

```json
"Statement": [
    {/* 첫 번째 문장 */},
    {/* 두 번째 문장 */}
]
```

## Statement 내부 요소

### Sid (선택)

Statement ID로, 문장을 식별하는 선택적 식별자입니다.

```json
"Sid": "AllowS3Read"
```

### Effect (필수)

해당 문장이 접근을 허용하는지 거부하는지를 지정합니다.

```json
"Effect": "Allow"  // 또는 "Deny"
```

### Principal (조건부 필수)

정책이 적용되는 주체를 지정합니다. 리소스 기반 정책에서만 사용합니다.

```json
// 특정 AWS 계정
"Principal": {"AWS": "arn:aws:iam::123456789012:root"}

// 특정 IAM 사용자
"Principal": {"AWS": "arn:aws:iam::123456789012:user/johndoe"}

// 특정 IAM 역할
"Principal": {"AWS": "arn:aws:iam::123456789012:role/MyRole"}

// AWS 서비스
"Principal": {"Service": "ec2.amazonaws.com"}

// 모든 사람 (퍼블릭)
"Principal": "*"

// 여러 주체
"Principal": {
    "AWS": [
        "arn:aws:iam::123456789012:user/johndoe",
        "arn:aws:iam::123456789012:user/jane"
    ]
}
```

### NotPrincipal

지정된 주체를 제외한 모든 주체에 적용됩니다.

```json
"NotPrincipal": {"AWS": "arn:aws:iam::123456789012:user/admin"}
```

** 주의:** `NotPrincipal`과 `Deny`를 함께 사용하면 의도치 않은 결과가 발생할 수 있습니다.

### Action (필수)

허용하거나 거부할 작업을 지정합니다.

```json
// 단일 작업
"Action": "s3:GetObject"

// 여러 작업
"Action": [
    "s3:GetObject",
    "s3:PutObject"
]

// 와일드카드 사용
"Action": "s3:*"              // S3의 모든 작업
"Action": "s3:Get*"           // Get으로 시작하는 모든 S3 작업
"Action": "*"                 // 모든 서비스의 모든 작업
```

### NotAction

지정된 작업을 제외한 모든 작업에 적용됩니다.

```json
// IAM 외의 모든 작업 허용
{
    "Effect": "Allow",
    "NotAction": "iam:*",
    "Resource": "*"
}
```

### Resource (필수)

작업이 적용되는 리소스를 지정합니다.

```json
// 특정 S3 버킷
"Resource": "arn:aws:s3:::my-bucket"

// 버킷 내 모든 객체
"Resource": "arn:aws:s3:::my-bucket/*"

// 버킷과 객체 모두
"Resource": [
    "arn:aws:s3:::my-bucket",
    "arn:aws:s3:::my-bucket/*"
]

// 모든 리소스
"Resource": "*"

// 와일드카드 패턴
"Resource": "arn:aws:s3:::my-bucket/home/${aws:username}/*"
```

### NotResource

지정된 리소스를 제외한 모든 리소스에 적용됩니다.

```json
// 특정 버킷 외 모든 S3 리소스
"NotResource": "arn:aws:s3:::secure-bucket/*"
```

### Condition (선택)

정책이 적용되는 조건을 지정합니다.

```json
"Condition": {
    "조건연산자": {
        "조건키": "조건값"
    }
}
```

상세 내용은 [조건 키](./condition-keys.md) 문서를 참조하세요.

## 실제 정책 예시

### S3 버킷 읽기 전용 접근

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowS3BucketRead",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::my-bucket",
                "arn:aws:s3:::my-bucket/*"
            ]
        }
    ]
}
```

### EC2 인스턴스 시작/중지 (특정 태그)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowStartStopTaggedInstances",
            "Effect": "Allow",
            "Action": [
                "ec2:StartInstances",
                "ec2:StopInstances"
            ],
            "Resource": "arn:aws:ec2:*:*:instance/*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Environment": "Development"
                }
            }
        }
    ]
}
```

### 자신의 IAM 자격 증명 관리

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowManageOwnCredentials",
            "Effect": "Allow",
            "Action": [
                "iam:ChangePassword",
                "iam:CreateAccessKey",
                "iam:DeleteAccessKey",
                "iam:GetAccessKeyLastUsed",
                "iam:GetUser",
                "iam:ListAccessKeys",
                "iam:UpdateAccessKey"
            ],
            "Resource": "arn:aws:iam::*:user/${aws:username}"
        }
    ]
}
```

### MFA 필수 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DenyAllExceptMFAManagement",
            "Effect": "Deny",
            "NotAction": [
                "iam:CreateVirtualMFADevice",
                "iam:EnableMFADevice",
                "iam:GetUser",
                "iam:ListMFADevices",
                "iam:ListVirtualMFADevices",
                "iam:ResyncMFADevice",
                "sts:GetSessionToken"
            ],
            "Resource": "*",
            "Condition": {
                "BoolIfExists": {
                    "aws:MultiFactorAuthPresent": "false"
                }
            }
        }
    ]
}
```

### 교차 계정 S3 버킷 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "CrossAccountAccess",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::111122223333:root"
            },
            "Action": [
                "s3:GetObject",
                "s3:PutObject"
            ],
            "Resource": "arn:aws:s3:::shared-bucket/*"
        }
    ]
}
```

## 정책 변수

동적 값을 사용할 수 있는 변수입니다.

### 주요 정책 변수

| 변수 | 설명 | 예시 값 |
|------|------|---------|
| `${aws:username}` | IAM 사용자 이름 | johndoe |
| `${aws:userid}` | IAM 사용자 ID | AIDAJQABLZS4A3QDU576Q |
| `${aws:PrincipalTag/key}` | 주체의 태그 값 | ${aws:PrincipalTag/team} |
| `${aws:RequestTag/key}` | 요청의 태그 값 | ${aws:RequestTag/project} |
| `${aws:ResourceTag/key}` | 리소스의 태그 값 | ${aws:ResourceTag/env} |
| `${aws:CurrentTime}` | 현재 시간 | 2024-01-15T10:00:00Z |
| `${aws:SourceIp}` | 요청 IP 주소 | 192.0.2.1 |

### 변수 사용 예시

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": [
                "arn:aws:s3:::my-bucket/home/${aws:username}",
                "arn:aws:s3:::my-bucket/home/${aws:username}/*"
            ]
        }
    ]
}
```

## 정책 제한 사항

| 항목 | 제한 |
|------|------|
| 관리형 정책 크기 | 6,144자 |
| 인라인 정책 크기 | 2,048자 (사용자), 5,120자 (역할), 5,120자 (그룹) |
| Statement 수 | 제한 없음 (크기 제한 내) |
| 정책당 버전 수 | 5 |

## 관련 문서

- [정책 개요](./policy-overview.md)
- [조건 키](./condition-keys.md)
- [정책 평가 로직](./evaluation-logic.md)
