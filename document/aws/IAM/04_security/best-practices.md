# IAM 보안 모범 사례

AWS가 권장하는 IAM 보안 모범 사례 14가지를 상세히 설명합니다.

## 1. 루트 사용자 보호

### 루트 자격 증명 보안

```
✅ 필수 조치:
- 루트 사용자에 MFA 즉시 활성화
- 루트 액세스 키 생성 금지 (이미 있다면 삭제)
- 루트 자격 증명을 타인과 공유하지 않음
- 강력하고 고유한 비밀번호 사용
```

### 루트 사용자 사용 제한

```
루트 사용자가 필요한 작업만:
- 계정 설정 변경 (이메일, 비밀번호)
- 계정 폐쇄
- IAM 관리자 권한 복구
- 특정 결제 관련 작업

일상 작업에는 절대 사용하지 않음
```

## 2. 다중 인증 (MFA) 필수화

### MFA 적용 대상

```
필수 적용:
- 루트 사용자
- 모든 IAM 사용자 (특히 관리자)
- 민감한 작업을 수행하는 역할

권장 MFA 유형:
1. 하드웨어 보안 키 (FIDO2) - 가장 안전
2. 가상 MFA 앱 (Authenticator)
3. 하드웨어 TOTP 토큰
```

### MFA 강제 정책

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowMFAManagement",
            "Effect": "Allow",
            "Action": [
                "iam:CreateVirtualMFADevice",
                "iam:EnableMFADevice",
                "iam:GetUser",
                "iam:ListMFADevices",
                "iam:ListVirtualMFADevices",
                "iam:ResyncMFADevice"
            ],
            "Resource": "*"
        },
        {
            "Sid": "DenyAllExceptMFA",
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

## 3. 연합 인증 사용 (인력 사용자)

### IAM Identity Center 활용

```
권장 아키텍처:
┌──────────────────┐     ┌──────────────────┐
│   기업 IdP       │ ──→ │ IAM Identity     │ ──→ AWS 계정들
│ (Azure AD, Okta) │     │     Center       │
└──────────────────┘     └──────────────────┘

장점:
- 중앙 집중식 사용자 관리
- SSO(Single Sign-On) 제공
- 임시 자격 증명만 사용
- 기존 기업 디렉토리 활용
```

### IAM 사용자 대신 연합 사용

```
❌ 피해야 할 방식:
- 각 직원에게 IAM 사용자 생성
- 장기 자격 증명 배포
- 개별 비밀번호 관리

✅ 권장 방식:
- 기업 디렉토리와 연합
- SAML 2.0 또는 OIDC 사용
- 임시 자격 증명으로 접근
```

## 4. 최소 권한 원칙 (Least Privilege)

### 권한 부여 접근법

```
권장 접근법:
1. 최소한의 권한으로 시작
2. 필요에 따라 점진적으로 추가
3. 정기적으로 미사용 권한 검토
4. 불필요한 권한 제거

잘못된 접근법:
1. 광범위한 권한 부여
2. 필요 시 제거 (거의 일어나지 않음)
```

### 권한 분석 도구 활용

```
IAM Access Analyzer 활용:
- 마지막 접근 정보 확인
- 미사용 권한 식별
- CloudTrail 기반 정책 생성
- 100개 이상의 정책 검증
```

### 예시: 세분화된 권한

```json
// ❌ 너무 광범위함
{
    "Effect": "Allow",
    "Action": "s3:*",
    "Resource": "*"
}

// ✅ 적절히 제한됨
{
    "Effect": "Allow",
    "Action": [
        "s3:GetObject",
        "s3:PutObject"
    ],
    "Resource": "arn:aws:s3:::my-app-bucket/uploads/${aws:username}/*"
}
```

## 5. 워크로드에 임시 자격 증명 사용

### EC2 인스턴스

```
권장:
- IAM 역할을 인스턴스 프로필로 연결
- SDK가 자동으로 임시 자격 증명 획득

금지:
- 인스턴스에 액세스 키 저장
- 코드에 자격 증명 하드코딩
```

### Lambda 함수

```
권장:
- 함수에 실행 역할 연결
- 필요한 최소 권한만 부여

금지:
- 환경 변수에 액세스 키 저장
- 코드에 자격 증명 포함
```

### 컨테이너 (ECS, EKS)

```
ECS:
- 태스크 역할 사용
- 태스크 정의에 역할 ARN 지정

EKS:
- IRSA (IAM Roles for Service Accounts) 사용
- 서비스 계정에 역할 연결
```

## 6. 액세스 키 관리

### 액세스 키가 필요한 경우

```
불가피한 경우만:
- IAM 역할을 지원하지 않는 도구
- 로컬 개발 환경 (AWS CLI)
- 서드파티 서비스 연동
```

### 액세스 키 보안

```
✅ 필수 조치:
- 정기적 교체 (90일 권장)
- 미사용 키 비활성화 및 삭제
- 코드, 설정 파일에 포함 금지
- 버전 관리 시스템에 커밋 금지
- 환경 변수 또는 자격 증명 파일 사용
```

### 키 교체 절차

```bash
# 1. 새 액세스 키 생성
aws iam create-access-key --user-name myuser

# 2. 애플리케이션 업데이트 (새 키 사용)

# 3. 기존 키 비활성화
aws iam update-access-key \
    --user-name myuser \
    --access-key-id AKIAIOSFODNN7OLD \
    --status Inactive

# 4. 테스트 후 기존 키 삭제
aws iam delete-access-key \
    --user-name myuser \
    --access-key-id AKIAIOSFODNN7OLD
```

## 7. AWS 관리형 정책 활용

### 시작점으로 활용

```
권장 접근법:
1. AWS 관리형 정책으로 빠르게 시작
2. IAM Access Analyzer로 실제 사용 분석
3. 필요에 맞게 고객 관리형 정책 생성
4. 최소 권한으로 점진적 전환
```

### 직무별 관리형 정책

| 직무 | 정책 |
|------|------|
| 관리자 | `AdministratorAccess` |
| 파워 유저 | `PowerUserAccess` |
| 보안 감사 | `SecurityAudit` |
| 결제 관리 | `Billing` |
| 읽기 전용 | `ReadOnlyAccess` |

## 8. 정책에 조건 사용

### 일반적인 조건 활용

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "*",
            "Resource": "*",
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "true"
                },
                "IpAddress": {
                    "aws:SourceIp": "10.0.0.0/8"
                },
                "Bool": {
                    "aws:MultiFactorAuthPresent": "true"
                }
            }
        }
    ]
}
```

### 효과적인 조건 예시

```
리전 제한:
"aws:RequestedRegion": ["us-east-1", "ap-northeast-2"]

시간 제한:
"aws:CurrentTime": 업무 시간 내만

암호화 강제:
"s3:x-amz-server-side-encryption": "aws:kms"

태그 기반 접근:
"aws:ResourceTag/Environment": "${aws:PrincipalTag/Environment}"
```

## 9. 정기적인 권한 검토

### 검토 항목

```
검토 주기: 최소 분기별

확인 사항:
- 미사용 IAM 사용자
- 미사용 IAM 역할
- 미사용 자격 증명 (비밀번호, 액세스 키)
- 과도한 권한
- 불필요한 그룹 멤버십
```

### 자격 증명 보고서 활용

```bash
# 자격 증명 보고서 생성
aws iam generate-credential-report

# 보고서 다운로드
aws iam get-credential-report \
    --output text \
    --query Content | base64 -d > report.csv
```

### 마지막 접근 정보 확인

```bash
# 서비스 마지막 접근 정보
aws iam generate-service-last-accessed-details \
    --arn arn:aws:iam::123456789012:user/johndoe

# 결과 조회
aws iam get-service-last-accessed-details \
    --job-id <job-id>
```

## 10. IAM Access Analyzer 활용

### 외부 접근 분석

```
분석 대상:
- S3 버킷
- IAM 역할
- KMS 키
- Lambda 함수
- SQS 대기열
- Secrets Manager 비밀

발견 사항:
- 외부 계정에 공유된 리소스
- 퍼블릭 접근 가능 리소스
```

### 정책 검증

```
100개 이상의 정책 검사:
- 보안 경고
- 오류
- 제안 사항
- 최소 권한 위반
```

### 정책 생성

```
CloudTrail 기반 정책 생성:
1. 실제 API 호출 분석
2. 사용된 권한만 포함하는 정책 생성
3. 최소 권한 원칙 자동 적용
```

## 11. 조직 전체 가드레일

### 서비스 제어 정책 (SCP)

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
        },
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

### 리소스 제어 정책 (RCP)

```
조직 전체 리소스 보호:
- 특정 리전만 허용
- 특정 서비스 차단
- 암호화 강제
```

## 12. 권한 경계 활용

### 권한 위임 시 사용

```
시나리오:
- 개발팀에 IAM 사용자 생성 권한 위임
- 생성되는 사용자의 최대 권한 제한
- 권한 상승 공격 방지
```

### 구현 예시

```json
{
    "Effect": "Allow",
    "Action": "iam:CreateUser",
    "Resource": "*",
    "Condition": {
        "StringEquals": {
            "iam:PermissionsBoundary": "arn:aws:iam::123456789012:policy/DeveloperBoundary"
        }
    }
}
```

## 13. 교차 계정 접근 검증

### IAM Access Analyzer 활용

```
분석 범위:
- 계정 수준: 외부 접근 발견
- 조직 수준: 조직 외부 접근 발견

검토 대상:
- 의도적인 공유인지 확인
- 불필요한 공유 제거
- 접근 범위 최소화
```

## 14. 감사 및 모니터링

### CloudTrail 활성화

```
필수 설정:
- 모든 리전에서 활성화
- S3에 로그 저장
- 로그 파일 무결성 검증
- CloudWatch Logs로 스트리밍
```

### CloudWatch 경보

```
경보 설정 권장:
- 루트 사용자 활동
- IAM 정책 변경
- 콘솔 로그인 실패
- 액세스 키 생성/삭제
```

### AWS Config 규칙

```
IAM 관련 규칙:
- iam-password-policy
- iam-root-access-key-check
- iam-user-mfa-enabled
- iam-user-unused-credentials-check
- access-keys-rotated
```

## 체크리스트

```
□ 루트 사용자 MFA 활성화
□ 루트 액세스 키 없음
□ 모든 사용자 MFA 활성화
□ 연합 인증 구성 (가능한 경우)
□ 최소 권한 정책 적용
□ 액세스 키 정기 교체
□ 미사용 자격 증명 제거
□ IAM Access Analyzer 활성화
□ SCP 가드레일 설정
□ CloudTrail 활성화
□ 정기적 권한 검토 일정
```

## 관련 문서

- [MFA 설정](./mfa.md)
- [액세스 키 관리](./access-keys.md)
- [IAM Access Analyzer](../05_advanced/access-analyzer.md)
