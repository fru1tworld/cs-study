# IAM 사용자

## IAM 사용자란?

IAM 사용자는 AWS 계정 내에서 생성하는 엔터티로, 사람이나 서비스가 AWS와 상호작용하기 위해 사용합니다. 각 사용자는 고유한 자격 증명과 권한을 가집니다.

## 사용자 식별 방법

AWS는 IAM 사용자를 세 가지 방식으로 식별합니다:

| 식별자 | 설명 | 예시 |
|--------|------|------|
| ** 친화적 이름** | 생성 시 지정한 읽기 쉬운 이름 | `johndoe`, `admin-user` |
| **ARN** | AWS 전체에서 고유한 리소스 이름 | `arn:aws:iam::123456789012:user/johndoe` |
| ** 고유 ID** | API/CLI로 생성 시 반환되는 ID | `AIDAJQABLZS4A3QDU576Q` |

## 사용자 생성

### AWS 콘솔에서 생성

```
IAM 콘솔 → 사용자 → 사용자 추가
1. 사용자 이름 입력
2. AWS 접근 유형 선택 (콘솔/프로그래밍)
3. 권한 설정 (그룹 추가 또는 직접 연결)
4. 태그 추가 (선택)
5. 검토 및 생성
```

### AWS CLI로 생성

```bash
# 사용자 생성
aws iam create-user --user-name johndoe

# 콘솔 로그인 프로필 생성
aws iam create-login-profile \
    --user-name johndoe \
    --password MyP@ssword123 \
    --password-reset-required

# 그룹에 추가
aws iam add-user-to-group \
    --user-name johndoe \
    --group-name Developers
```

## 자격 증명 유형

### 1. 콘솔 비밀번호

AWS Management Console 로그인에 사용됩니다.

```
특징:
- 사용자당 하나의 비밀번호
- 비밀번호 정책 적용 가능
- 만료 기간 설정 가능
- 첫 로그인 시 변경 강제 가능
```

### 2. 액세스 키

AWS CLI, SDK, API 호출에 사용됩니다.

```
구성:
- 액세스 키 ID: AKIAIOSFODNN7EXAMPLE
- 비밀 액세스 키: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

제한:
- 사용자당 최대 2개
- 비밀 키는 생성 시에만 조회 가능
- 분실 시 새로 생성해야 함
```

### 3. SSH 키

AWS CodeCommit 접근에 사용됩니다.

### 4. 서버 인증서

일부 AWS 서비스의 HTTPS 연결에 사용됩니다.

## 권한 관리

### 새 사용자의 기본 권한

```
⚠️ 새로 생성된 IAM 사용자는 기본적으로 어떤 권한도 없습니다!

관리자가 명시적으로 권한을 부여해야 합니다.
```

### 권한 부여 방법

```
1. 그룹 기반 (권장)
   사용자 → 그룹 가입 → 그룹에 연결된 정책 상속

2. 직접 연결
   사용자 → 정책 직접 연결

3. 인라인 정책
   사용자 → 사용자 전용 정책 생성
```

### 권한 경계 설정

사용자가 가질 수 있는 최대 권한을 제한합니다.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:*",
                "ec2:Describe*"
            ],
            "Resource": "*"
        }
    ]
}
```

## 사용자 경로 (Path)

사용자를 조직 구조에 맞게 그룹화할 수 있습니다.

```
/                           # 기본 경로
/division_abc/              # 부서별
/division_abc/subdivision_xyz/  # 하위 부서
/application/               # 애플리케이션용
```

ARN에 경로 반영:
```
arn:aws:iam::123456789012:user/division_abc/johndoe
```

## 사용자 제한 사항

| 항목 | 제한 |
|------|------|
| 계정당 IAM 사용자 수 | 5,000 |
| 사용자가 속할 수 있는 그룹 수 | 10 |
| 사용자에게 연결 가능한 관리형 정책 수 | 10 |
| 사용자 이름 길이 | 64자 |
| 경로 길이 | 512자 |

## IAM 사용자 vs IAM 역할

### 사용자를 선택해야 하는 경우

- 장기적으로 고정된 자격 증명이 필요한 경우
- 역할을 지원하지 않는 서드파티 도구 사용
- CodeCommit 접근 필요
- 비상 접근 시나리오

### 역할을 선택해야 하는 경우

- EC2, Lambda 등 AWS 서비스에서 실행되는 애플리케이션
- 교차 계정 접근
- 임시 접근 권한 필요
- 연합 사용자 접근

## 사용자 관리 모범 사례

### 1. 개별 사용자 생성

```
❌ 잘못된 방법: 팀 전체가 하나의 계정 공유
✅ 올바른 방법: 각 팀원에게 개별 IAM 사용자 생성
```

### 2. 최소 권한 원칙

```
❌ 잘못된 방법: AdministratorAccess 정책 연결
✅ 올바른 방법: 필요한 권한만 부여
```

### 3. 그룹 활용

```
❌ 잘못된 방법: 각 사용자에게 개별적으로 정책 연결
✅ 올바른 방법: 그룹에 정책 연결 후 사용자를 그룹에 추가
```

### 4. MFA 활성화

```
모든 사용자에게 MFA 필수화:
- 특히 관리자 권한이 있는 사용자
- 콘솔 접근 사용자
- 민감한 리소스 접근 사용자
```

### 5. 정기적 검토

```
검토 항목:
- 미사용 자격 증명 식별 및 제거
- 불필요한 권한 제거
- 액세스 키 교체
- 퇴직자 계정 삭제
```

## 자격 증명 보고서

계정의 모든 IAM 사용자와 자격 증명 상태를 CSV로 다운로드할 수 있습니다.

포함 정보:
- 사용자 생성 시간
- 비밀번호 마지막 사용 시간
- 비밀번호 마지막 변경 시간
- MFA 활성화 여부
- 액세스 키 상태 및 마지막 사용 시간

```bash
# CLI로 보고서 생성
aws iam generate-credential-report

# 보고서 다운로드
aws iam get-credential-report --output text --query Content | base64 -d
```

## 관련 문서

- [IAM 그룹](./groups.md)
- [액세스 키 관리](../04_security/access-keys.md)
- [MFA 설정](../04_security/mfa.md)
