# IAM 사용자 그룹

## IAM 그룹이란?

IAM 사용자 그룹은 IAM 사용자들의 모음입니다. 그룹을 사용하면 여러 사용자에 대한 권한을 한 번에 지정할 수 있어 권한 관리가 효율적입니다.

## 그룹의 특징

### 권한 상속
- 그룹에 연결된 정책은 그룹 내 모든 사용자에게 적용
- 사용자가 그룹에 가입하면 자동으로 그룹의 권한 획득
- 사용자가 그룹에서 탈퇴하면 그룹 권한 상실

### 구조적 특성
- 그룹은 사용자만 포함 가능 (그룹 중첩 불가)
- 한 사용자가 여러 그룹에 속할 수 있음
- 기본적으로 모든 사용자를 포함하는 그룹은 없음

## 그룹 생성 및 관리

### AWS 콘솔에서 생성

```
IAM 콘솔 → 사용자 그룹 → 그룹 생성
1. 그룹 이름 입력
2. 그룹에 추가할 사용자 선택
3. 권한 정책 연결
4. 그룹 생성
```

### AWS CLI로 관리

```bash
# 그룹 생성
aws iam create-group --group-name Developers

# 그룹에 정책 연결
aws iam attach-group-policy \
    --group-name Developers \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ReadOnlyAccess

# 사용자를 그룹에 추가
aws iam add-user-to-group \
    --user-name johndoe \
    --group-name Developers

# 그룹에서 사용자 제거
aws iam remove-user-from-group \
    --user-name johndoe \
    --group-name Developers

# 그룹 삭제
aws iam delete-group --group-name Developers
```

## 일반적인 그룹 구성 예시

### 역할 기반 그룹

```
조직 구조:
├── Administrators    # 전체 관리 권한
├── Developers        # 개발 관련 서비스 접근
├── Testers          # 테스트 환경 접근
├── Operations       # 운영 관련 서비스 접근
├── DataAnalysts     # 데이터 분석 서비스 접근
└── ReadOnly         # 읽기 전용 접근
```

### 프로젝트 기반 그룹

```
프로젝트 구조:
├── ProjectA-Admins   # 프로젝트 A 관리자
├── ProjectA-Members  # 프로젝트 A 멤버
├── ProjectB-Admins   # 프로젝트 B 관리자
└── ProjectB-Members  # 프로젝트 B 멤버
```

### 환경 기반 그룹

```
환경 구조:
├── Dev-Access       # 개발 환경 접근
├── Staging-Access   # 스테이징 환경 접근
└── Prod-Access      # 프로덕션 환경 접근
```

## 그룹 정책 예시

### Administrators 그룹

```json
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
```

### Developers 그룹

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:*",
                "s3:*",
                "lambda:*",
                "dynamodb:*",
                "cloudwatch:*",
                "logs:*"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Deny",
            "Action": [
                "ec2:*ReservedInstances*",
                "ec2:*Purchase*"
            ],
            "Resource": "*"
        }
    ]
}
```

### ReadOnly 그룹

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:Describe*",
                "s3:Get*",
                "s3:List*",
                "rds:Describe*",
                "cloudwatch:Get*",
                "cloudwatch:List*"
            ],
            "Resource": "*"
        }
    ]
}
```

## 그룹 제한 사항

| 항목 | 제한 |
|------|------|
| 계정당 그룹 수 | 300 |
| 그룹당 사용자 수 | 무제한 |
| 사용자당 가입 가능한 그룹 수 | 10 |
| 그룹에 연결 가능한 관리형 정책 수 | 10 |
| 그룹 이름 길이 | 128자 |

## 그룹 사용 시 주의사항

### 그룹은 Principal이 될 수 없음

리소스 기반 정책에서 그룹을 직접 지정할 수 없습니다.

```json
// ❌ 잘못된 예시 - 그룹을 Principal로 지정 불가
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::123456789012:group/Developers"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::my-bucket/*"
        }
    ]
}

// ✅ 올바른 예시 - 개별 사용자 또는 역할 지정
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::123456789012:user/johndoe"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::my-bucket/*"
        }
    ]
}
```

### 그룹은 역할을 맡을 수 없음

그룹이 IAM 역할을 assume하는 것은 불가능합니다. 역할을 맡으려면 개별 사용자가 직접 해야 합니다.

## 그룹 관리 모범 사례

### 1. 직무 기반 그룹 생성

```
✅ 권장:
- 조직의 직무 역할에 맞는 그룹 생성
- 각 그룹에 해당 직무에 필요한 최소 권한만 부여
```

### 2. 중복 그룹 방지

```
❌ 잘못된 예:
- EC2-Access
- S3-Access
- EC2-S3-Access (중복)

✅ 권장:
- Developers (EC2 + S3 권한 포함)
```

### 3. 그룹 멤버십 정기 검토

```
검토 항목:
- 퇴직자 제거
- 직무 변경 시 그룹 재배치
- 불필요한 그룹 멤버십 제거
```

### 4. 그룹 명명 규칙 수립

```
명명 규칙 예시:
- Role-{역할명}: Role-Developers
- Project-{프로젝트명}-{역할}: Project-Alpha-Admin
- Env-{환경명}: Env-Production
```

## 그룹 멤버십 시각화

```
┌─────────────────────────────────────────────────────────┐
│                    AWS 계정                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Administrators│  │  Developers  │  │   ReadOnly   │  │
│  │              │  │              │  │              │  │
│  │  - admin1    │  │  - dev1      │  │  - viewer1   │  │
│  │  - admin2    │  │  - dev2      │  │  - viewer2   │  │
│  │              │  │  - dev3      │  │  - dev1 (중복)│  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│        │                 │                  │          │
│        ▼                 ▼                  ▼          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ AdminAccess  │  │ DevPolicy    │  │ ViewOnly     │  │
│  │   Policy     │  │              │  │   Policy     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘

dev1 사용자의 최종 권한:
= DevPolicy + ViewOnly Policy (두 그룹에 속함)
```

## 관련 문서

- [IAM 사용자](./users.md)
- [IAM 정책](../03_policies/README.md)
- [관리형 정책 vs 인라인 정책](../03_policies/managed-vs-inline.md)
