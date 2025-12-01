# IAM 다중 인증 (MFA)

## MFA란?

다중 인증(Multi-Factor Authentication)은 사용자 이름과 비밀번호 외에 추가적인 인증 요소를 요구하여 보안을 강화하는 방법입니다.

## 인증 요소

```
3가지 인증 요소:
1. 지식 (Something you know) - 비밀번호, PIN
2. 소유 (Something you have) - MFA 디바이스, 스마트폰
3. 생체 (Something you are) - 지문, 얼굴 인식

MFA = 2개 이상의 요소 조합
```

## 지원하는 MFA 유형

### 1. 패스키 및 보안 키 (FIDO2)

가장 안전한 MFA 방식입니다.

```
보안 키 (물리적 디바이스):
- YubiKey
- Feitian
- 기타 FIDO2 인증 키

동기화된 패스키 (소프트웨어):
- Apple iCloud 키체인
- Google 비밀번호 관리자
- 1Password
- Dashlane

장점:
- 피싱 방지 (바인딩된 도메인에서만 작동)
- 사용 편의성
- 공개 키 암호화 기반
```

### 2. 가상 MFA 앱

스마트폰 앱에서 일회용 코드를 생성합니다.

```
지원 앱:
- Google Authenticator
- Microsoft Authenticator
- Authy
- Duo Mobile

특징:
- TOTP (Time-based One-Time Password) 알고리즘
- 30초마다 새로운 6자리 코드 생성
- 여러 계정 등록 가능
```

### 3. 하드웨어 TOTP 토큰

물리적 디바이스에서 코드를 생성합니다.

```
특징:
- AWS 정품 토큰만 호환
- Amazon을 통해 구매
- 배터리 수명 제한
- 스마트폰 없이 사용 가능
```

## MFA 설정

### 루트 사용자 MFA 설정

```
AWS 콘솔:
1. 루트 사용자로 로그인
2. 우측 상단 계정 이름 → 보안 자격 증명
3. MFA 디바이스 할당
4. MFA 유형 선택
5. 설정 완료

⚠️ 루트 사용자에는 반드시 MFA를 설정하세요!
```

### IAM 사용자 MFA 설정

```bash
# 가상 MFA 디바이스 생성
aws iam create-virtual-mfa-device \
    --virtual-mfa-device-name johndoe-mfa \
    --outfile QRCode.png \
    --bootstrap-method QRCodePNG

# MFA 디바이스 활성화
aws iam enable-mfa-device \
    --user-name johndoe \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe-mfa \
    --authentication-code1 123456 \
    --authentication-code2 789012
```

### 콘솔에서 IAM 사용자 MFA 설정

```
IAM 콘솔 → 사용자 → 사용자 선택 → 보안 자격 증명 탭
1. MFA 디바이스 할당
2. 디바이스 유형 선택
3. QR 코드 스캔 (가상 MFA의 경우)
4. 연속된 두 코드 입력
5. 설정 완료
```

## MFA 강제 적용

### 모든 사용자에게 MFA 강제

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowViewAccountInfo",
            "Effect": "Allow",
            "Action": [
                "iam:GetAccountPasswordPolicy",
                "iam:ListVirtualMFADevices"
            ],
            "Resource": "*"
        },
        {
            "Sid": "AllowManageOwnMFA",
            "Effect": "Allow",
            "Action": [
                "iam:CreateVirtualMFADevice",
                "iam:DeleteVirtualMFADevice",
                "iam:EnableMFADevice",
                "iam:ListMFADevices",
                "iam:ResyncMFADevice"
            ],
            "Resource": [
                "arn:aws:iam::*:mfa/${aws:username}",
                "arn:aws:iam::*:user/${aws:username}"
            ]
        },
        {
            "Sid": "AllowDeactivateOwnMFA",
            "Effect": "Allow",
            "Action": "iam:DeactivateMFADevice",
            "Resource": [
                "arn:aws:iam::*:mfa/${aws:username}",
                "arn:aws:iam::*:user/${aws:username}"
            ],
            "Condition": {
                "Bool": {
                    "aws:MultiFactorAuthPresent": "true"
                }
            }
        },
        {
            "Sid": "DenyAllExceptMFASignIn",
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

### 특정 작업에 MFA 필수

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "RequireMFAForSensitiveActions",
            "Effect": "Deny",
            "Action": [
                "ec2:TerminateInstances",
                "rds:DeleteDBInstance",
                "s3:DeleteBucket"
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

## CLI/API에서 MFA 사용

### 임시 자격 증명 획득

```bash
# MFA로 세션 토큰 얻기
aws sts get-session-token \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe \
    --token-code 123456

# 응답
{
    "Credentials": {
        "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
        "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        "SessionToken": "AQoDYXdzEJr...",
        "Expiration": "2024-01-15T12:00:00Z"
    }
}
```

### 임시 자격 증명 사용

```bash
# 환경 변수 설정
export AWS_ACCESS_KEY_ID=ASIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export AWS_SESSION_TOKEN=AQoDYXdzEJr...

# 또는 프로필 설정 (~/.aws/credentials)
[mfa]
aws_access_key_id = ASIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
aws_session_token = AQoDYXdzEJr...
```

### MFA로 역할 맡기

```bash
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/AdminRole \
    --role-session-name admin-session \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe \
    --token-code 123456
```

## MFA 관리

### MFA 디바이스 목록 조회

```bash
# 특정 사용자의 MFA 디바이스
aws iam list-mfa-devices --user-name johndoe

# 계정의 모든 가상 MFA 디바이스
aws iam list-virtual-mfa-devices
```

### MFA 디바이스 비활성화

```bash
# MFA 디바이스 비활성화
aws iam deactivate-mfa-device \
    --user-name johndoe \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe

# 가상 MFA 디바이스 삭제
aws iam delete-virtual-mfa-device \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe
```

### MFA 재동기화

시간 동기화 문제로 코드가 맞지 않을 때:

```bash
aws iam resync-mfa-device \
    --user-name johndoe \
    --serial-number arn:aws:iam::123456789012:mfa/johndoe \
    --authentication-code1 123456 \
    --authentication-code2 789012
```

## 다중 MFA 디바이스

AWS는 사용자당 최대 8개의 MFA 디바이스를 지원합니다.

```
다중 디바이스 권장 이유:
- 백업 디바이스 (기기 분실 대비)
- 다른 위치에서 사용 (사무실, 집)
- 다른 유형 혼합 (보안 키 + 앱)
```

## MFA 조건 키

### aws:MultiFactorAuthPresent

```json
"Condition": {
    "Bool": {
        "aws:MultiFactorAuthPresent": "true"
    }
}
```

** 주의:** 이 키는 임시 자격 증명을 사용할 때만 존재합니다.

### aws:MultiFactorAuthAge

MFA 인증 이후 경과한 시간(초)을 확인합니다.

```json
"Condition": {
    "NumericLessThan": {
        "aws:MultiFactorAuthAge": "3600"
    }
}
```

## MFA 문제 해결

### 일반적인 문제

| 문제 | 원인 | 해결 방법 |
|------|------|----------|
| 코드가 일치하지 않음 | 시간 동기화 오류 | 디바이스 시간 확인, 재동기화 |
| MFA 디바이스 분실 | 물리적 분실 | 관리자에게 MFA 비활성화 요청 |
| MFA 앱 삭제됨 | 앱 재설치 | 관리자에게 MFA 재설정 요청 |

### MFA 복구 절차

```
1. 다른 관리자에게 연락
2. 관리자가 사용자의 MFA 비활성화
3. 사용자가 새 MFA 디바이스 등록
4. 기존 가상 MFA 디바이스 삭제
```

## 모범 사례

### 1. 모든 사용자에게 MFA 강제

```
필수:
- 루트 사용자
- 모든 IAM 사용자
- 민감한 작업 수행 역할
```

### 2. 다중 디바이스 등록

```
권장:
- 주 디바이스 + 백업 디바이스
- 서로 다른 위치에 보관
- 다른 유형 혼합
```

### 3. 피싱 방지 MFA 사용

```
우선순위:
1. FIDO2 보안 키 (가장 안전)
2. 패스키
3. 가상 MFA 앱
4. 하드웨어 TOTP 토큰
```

### 4. 정기적인 MFA 디바이스 검토

```
검토 사항:
- 등록된 디바이스 목록 확인
- 더 이상 사용하지 않는 디바이스 제거
- 퇴직자의 MFA 디바이스 삭제
```

## 관련 문서

- [보안 모범 사례](./best-practices.md)
- [액세스 키 관리](./access-keys.md)
- [임시 자격 증명](../02_identities/temporary-credentials.md)
