# IAM 권한 경계 (Permissions Boundary)

## 권한 경계란?

권한 경계는 IAM 엔터티(사용자 또는 역할)가 가질 수 있는 ** 최대 권한** 을 설정하는 고급 기능입니다. 관리형 정책을 사용하여 권한의 상한선을 정의합니다.

## 핵심 개념

### 권한 경계의 역할

```
┌─────────────────────────────────────────────────────────────┐
│                     권한 경계                                │
│              (최대 허용 가능한 권한)                         │
│                                                             │
│    ┌─────────────────────────────────────────────────┐     │
│    │           자격 증명 기반 정책                    │     │
│    │             (부여된 권한)                        │     │
│    │                                                  │     │
│    │    ┌─────────────────────────────────────┐     │     │
│    │    │         유효 권한                   │     │     │
│    │    │      (실제 사용 가능)               │     │     │
│    │    └─────────────────────────────────────┘     │     │
│    │                                                  │     │
│    └─────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

유효 권한 = 자격증명 기반 정책 ∩ 권한 경계
```

### 주요 특징

1. ** 권한을 부여하지 않음**: 권한 경계 자체는 어떤 권한도 부여하지 않습니다
2. ** 최대 권한만 설정**: 자격 증명 기반 정책이 부여할 수 있는 상한선만 정의
3. **IAM 사용자와 역할에만 적용**: 그룹에는 적용 불가

## 권한 경계 설정

### AWS 콘솔에서 설정

```
IAM 콘솔 → 사용자/역할 선택 → 권한 경계 → 권한 경계 설정
1. 사용할 관리형 정책 선택
2. 설정 저장
```

### AWS CLI로 설정

```bash
# 사용자에게 권한 경계 설정
aws iam put-user-permissions-boundary \
    --user-name johndoe \
    --permissions-boundary arn:aws:iam::123456789012:policy/MyBoundaryPolicy

# 역할에게 권한 경계 설정
aws iam put-role-permissions-boundary \
    --role-name MyRole \
    --permissions-boundary arn:aws:iam::123456789012:policy/MyBoundaryPolicy

# 권한 경계 제거
aws iam delete-user-permissions-boundary --user-name johndoe
aws iam delete-role-permissions-boundary --role-name MyRole
```

### 사용자 생성 시 설정

```bash
aws iam create-user \
    --user-name newuser \
    --permissions-boundary arn:aws:iam::123456789012:policy/MyBoundaryPolicy
```

## 권한 경계 예시

### 기본 권한 경계 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowedServices",
            "Effect": "Allow",
            "Action": [
                "s3:*",
                "ec2:*",
                "lambda:*",
                "dynamodb:*",
                "cloudwatch:*",
                "logs:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "AllowManageOwnCredentials",
            "Effect": "Allow",
            "Action": [
                "iam:ChangePassword",
                "iam:GetUser",
                "iam:CreateAccessKey",
                "iam:DeleteAccessKey",
                "iam:ListAccessKeys",
                "iam:UpdateAccessKey",
                "iam:GetAccessKeyLastUsed"
            ],
            "Resource": "arn:aws:iam::*:user/${aws:username}"
        },
        {
            "Sid": "DenyBoundaryModification",
            "Effect": "Deny",
            "Action": [
                "iam:DeleteUserPermissionsBoundary",
                "iam:DeleteRolePermissionsBoundary",
                "iam:PutUserPermissionsBoundary",
                "iam:PutRolePermissionsBoundary"
            ],
            "Resource": "*"
        }
    ]
}
```

### 리소스 제한 권한 경계

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowS3InRegion",
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:RequestedRegion": "ap-northeast-2"
                }
            }
        },
        {
            "Sid": "DenySpecificBucket",
            "Effect": "Deny",
            "Action": "s3:*",
            "Resource": [
                "arn:aws:s3:::confidential-bucket",
                "arn:aws:s3:::confidential-bucket/*"
            ]
        }
    ]
}
```

## 권한 위임 시나리오

### 시나리오: 개발팀 리더의 권한 위임

개발팀 리더 Maria가 팀원 Zhang에게 사용자 생성 권한을 위임하되, 특정 규칙을 따르도록 제한합니다.

#### 1단계: 팀원이 생성할 사용자의 권한 경계 (DevTeamBoundary)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowDevServices",
            "Effect": "Allow",
            "Action": [
                "s3:*",
                "lambda:*",
                "dynamodb:*",
                "cloudwatch:*",
                "logs:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "DenyProductionResources",
            "Effect": "Deny",
            "Action": "*",
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Environment": "Production"
                }
            }
        }
    ]
}
```

#### 2단계: Zhang의 권한 경계 (DelegatedAdminBoundary)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowIAMWithBoundary",
            "Effect": "Allow",
            "Action": [
                "iam:CreateUser",
                "iam:DeleteUser",
                "iam:AttachUserPolicy",
                "iam:DetachUserPolicy",
                "iam:PutUserPermissionsBoundary"
            ],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "iam:PermissionsBoundary": "arn:aws:iam::123456789012:policy/DevTeamBoundary"
                }
            }
        },
        {
            "Sid": "DenyBoundaryModification",
            "Effect": "Deny",
            "Action": [
                "iam:CreatePolicyVersion",
                "iam:DeletePolicy",
                "iam:DeletePolicyVersion",
                "iam:SetDefaultPolicyVersion"
            ],
            "Resource": [
                "arn:aws:iam::123456789012:policy/DevTeamBoundary",
                "arn:aws:iam::123456789012:policy/DelegatedAdminBoundary"
            ]
        },
        {
            "Sid": "DenyBoundaryRemoval",
            "Effect": "Deny",
            "Action": [
                "iam:DeleteUserPermissionsBoundary",
                "iam:DeleteRolePermissionsBoundary"
            ],
            "Resource": "*"
        }
    ]
}
```

#### 3단계: Zhang의 자격 증명 기반 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "iam:CreateUser",
                "iam:DeleteUser",
                "iam:GetUser",
                "iam:ListUsers",
                "iam:AttachUserPolicy",
                "iam:DetachUserPolicy",
                "iam:PutUserPermissionsBoundary",
                "iam:ListAttachedUserPolicies"
            ],
            "Resource": "*"
        }
    ]
}
```

#### 결과

Zhang이 새 사용자를 생성할 때:
- ✅ DevTeamBoundary를 권한 경계로 설정하면 생성 가능
- ❌ 권한 경계 없이 생성 시도하면 실패
- ❌ 다른 권한 경계 설정 시도하면 실패
- ❌ 권한 경계 제거 시도하면 실패

## 리소스 기반 정책과의 상호작용

### IAM 사용자의 경우

```
같은 계정 내 리소스 기반 정책의 직접 권한 부여:
- 사용자 ARN을 Principal로 지정한 경우
- 권한 경계의 암묵적 거부에 제한되지 않음

예외:
- 명시적 거부(Deny)는 항상 적용됨
```

### IAM 역할의 경우

```
역할 ARN을 Principal로 지정:
- 권한 경계의 제한을 받음

역할 세션 ARN을 Principal로 지정:
- 권한 경계의 제한을 받지 않음
```

## 암묵적 거부 vs 명시적 거부

### 암묵적 거부

권한 경계에 서비스가 포함되지 않은 경우:

```json
// 권한 경계: S3만 허용
{
    "Effect": "Allow",
    "Action": "s3:*",
    "Resource": "*"
}

// 자격증명 정책: EC2 허용
{
    "Effect": "Allow",
    "Action": "ec2:*",
    "Resource": "*"
}

// 결과: EC2 접근 불가 (권한 경계에서 암묵적 거부)
// 하지만 리소스 기반 정책으로 EC2 접근 가능할 수 있음
```

### 명시적 거부

권한 경계에서 명시적으로 Deny한 경우:

```json
// 권한 경계
{
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "*",
            "Resource": "*"
        },
        {
            "Effect": "Deny",
            "Action": "s3:DeleteBucket",
            "Resource": "*"
        }
    ]
}

// 결과: s3:DeleteBucket은 어떤 경우에도 거부
// 리소스 기반 정책으로도 재정의 불가
```

## 모범 사례

### 1. 권한 경계 정책 보호

```json
{
    "Effect": "Deny",
    "Action": [
        "iam:DeletePolicy",
        "iam:CreatePolicyVersion",
        "iam:DeletePolicyVersion",
        "iam:SetDefaultPolicyVersion"
    ],
    "Resource": "arn:aws:iam::*:policy/MyBoundaryPolicy"
}
```

### 2. 경계 제거 방지

```json
{
    "Effect": "Deny",
    "Action": [
        "iam:DeleteUserPermissionsBoundary",
        "iam:DeleteRolePermissionsBoundary",
        "iam:PutUserPermissionsBoundary",
        "iam:PutRolePermissionsBoundary"
    ],
    "Resource": "*",
    "Condition": {
        "StringNotEquals": {
            "iam:PermissionsBoundary": "arn:aws:iam::*:policy/MyBoundaryPolicy"
        }
    }
}
```

### 3. 생성 시 경계 강제

```json
{
    "Effect": "Allow",
    "Action": "iam:CreateUser",
    "Resource": "*",
    "Condition": {
        "StringEquals": {
            "iam:PermissionsBoundary": "arn:aws:iam::123456789012:policy/RequiredBoundary"
        }
    }
}
```

## 문제 해결

### 권한 거부 시 확인 사항

1. 자격 증명 기반 정책에서 허용하는지 확인
2. 권한 경계에서 허용하는지 확인
3. 두 정책의 교집합이 요청 작업을 포함하는지 확인
4. 명시적 거부가 있는지 확인

### 정책 시뮬레이터 활용

```bash
aws iam simulate-principal-policy \
    --policy-source-arn arn:aws:iam::123456789012:user/johndoe \
    --action-names s3:GetObject \
    --resource-arns arn:aws:s3:::my-bucket/file.txt
```

## 관련 문서

- [정책 개요](./policy-overview.md)
- [정책 평가 로직](./evaluation-logic.md)
- [보안 모범 사례](../04_security/best-practices.md)
