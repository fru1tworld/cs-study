# 임시 보안 자격 증명 (AWS STS)

## 임시 자격 증명이란?

AWS Security Token Service(STS)를 통해 생성되는 단기 자격 증명입니다. 장기 액세스 키와 유사하게 작동하지만, 중요한 차이점이 있습니다.

## 특징

### 장기 자격 증명과의 비교

| 항목 | 장기 자격 증명 | 임시 자격 증명 |
|------|--------------|--------------|
| 유효 기간 | 무제한 (삭제까지) | 몇 분 ~ 최대 12시간 |
| 저장 | IAM에 저장됨 | 동적 생성 |
| 갱신 | 수동 교체 | 자동 만료, 새로 요청 |
| 보안 위험 | 노출 시 위험 높음 | 노출 시 위험 제한적 |

### 구성 요소

임시 자격 증명은 세 가지 요소로 구성됩니다:

```
1. Access Key ID      - ASIAIOSFODNN7EXAMPLE
2. Secret Access Key  - wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
3. Session Token      - AQoDYXdzEJr... (긴 문자열)
```

## 장점

### 1. 자격 증명 배포 불필요
- 애플리케이션에 장기 자격 증명을 포함할 필요 없음
- 코드에 비밀 키를 하드코딩하지 않아도 됨

### 2. AWS 외부 사용자 지원
- IAM 사용자를 생성하지 않고도 리소스 접근 가능
- 기업 디렉토리 사용자 지원
- 소셜 로그인 사용자 지원

### 3. 자동 만료
- 만료 후 자동으로 무효화
- 수동으로 취소하거나 교체할 필요 없음
- 노출되어도 피해 기간 제한

## STS API 작업

### AssumeRole

IAM 역할을 맡아 임시 자격 증명을 얻습니다.

```bash
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/MyRole \
    --role-session-name my-session \
    --duration-seconds 3600
```

응답:
```json
{
    "Credentials": {
        "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
        "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        "SessionToken": "AQoDYXdzEJr...",
        "Expiration": "2024-01-15T12:00:00Z"
    },
    "AssumedRoleUser": {
        "AssumedRoleId": "AROA3XFRBF535EXAMPLE:my-session",
        "Arn": "arn:aws:sts::123456789012:assumed-role/MyRole/my-session"
    }
}
```

### AssumeRoleWithSAML

SAML 인증 후 역할을 맡습니다.

```bash
aws sts assume-role-with-saml \
    --role-arn arn:aws:iam::123456789012:role/SAMLRole \
    --principal-arn arn:aws:iam::123456789012:saml-provider/MyIdP \
    --saml-assertion file://saml-assertion.txt
```

### AssumeRoleWithWebIdentity

OIDC 또는 웹 자격 증명으로 역할을 맡습니다.

```bash
aws sts assume-role-with-web-identity \
    --role-arn arn:aws:iam::123456789012:role/WebIdentityRole \
    --role-session-name my-session \
    --web-identity-token file://token.txt
```

### GetFederationToken

연합 사용자를 위한 임시 자격 증명을 생성합니다.

```bash
aws sts get-federation-token \
    --name federation-user \
    --policy '{"Version":"2012-10-17","Statement":[...]}' \
    --duration-seconds 3600
```

### GetSessionToken

MFA 인증 후 임시 자격 증명을 얻습니다.

```bash
aws sts get-session-token \
    --serial-number arn:aws:iam::123456789012:mfa/user \
    --token-code 123456 \
    --duration-seconds 3600
```

## 사용 사례

### 1. EC2 인스턴스의 AWS 접근

```
┌─────────────────────────────────────────────────────────┐
│                     EC2 인스턴스                         │
│                                                         │
│   애플리케이션 ──→ 인스턴스 메타데이터 서비스            │
│        │            (169.254.169.254)                   │
│        │                    │                           │
│        │         임시 자격 증명 자동 제공                │
│        ▼                    ▼                           │
│   AWS SDK/CLI 사용하여 AWS 서비스 호출                   │
│                                                         │
└─────────────────────────────────────────────────────────┘

설정 방법:
1. IAM 역할 생성
2. 인스턴스 프로필 생성
3. EC2에 인스턴스 프로필 연결
4. SDK가 자동으로 자격 증명 획득
```

### 2. Lambda 함수의 AWS 접근

```python
import boto3

# Lambda 함수에서는 역할이 자동으로 할당됨
# SDK가 자동으로 임시 자격 증명 사용
s3 = boto3.client('s3')
response = s3.list_buckets()
```

### 3. 교차 계정 접근

```
계정 A (신뢰하는 계정)          계정 B (신뢰받는 계정)
┌─────────────────────┐      ┌─────────────────────┐
│                     │      │                     │
│   IAM 사용자 ───────┼──→   │    IAM 역할         │
│                     │      │    (신뢰 정책에     │
│   AssumeRole 권한   │      │     계정 A 명시)    │
│                     │      │                     │
└─────────────────────┘      └─────────────────────┘
         │                             │
         └──────── 임시 자격 증명 ──────┘
                    발급받음
```

### 4. 연합 사용자 (SAML)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   사용자    │───→│   기업 IdP  │───→│    AWS      │
│             │    │  (AD 등)    │    │   STS       │
└─────────────┘    └─────────────┘    └─────────────┘
                          │                  │
                    SAML 어설션        임시 자격 증명
                          │                  │
                          └────────┬─────────┘
                                   │
                                   ▼
                          AWS 콘솔/API 접근
```

## 세션 기간

### API별 기본 및 최대 기간

| API | 기본 기간 | 최대 기간 |
|-----|----------|----------|
| AssumeRole | 1시간 | 12시간 (역할 설정) |
| AssumeRoleWithSAML | 1시간 | 12시간 |
| AssumeRoleWithWebIdentity | 1시간 | 12시간 |
| GetFederationToken | 12시간 | 36시간 |
| GetSessionToken | 12시간 | 36시간 |

### 역할 체이닝 시 제한

역할 체이닝(역할에서 다른 역할로 전환)의 경우 최대 1시간으로 제한됩니다.

## 리전 엔드포인트

### 글로벌 엔드포인트 (기본)

```
https://sts.amazonaws.com
```

### 리전 엔드포인트 (권장)

지연 시간 감소를 위해 가장 가까운 리전 사용:

```
https://sts.us-east-1.amazonaws.com
https://sts.ap-northeast-2.amazonaws.com  # 서울
https://sts.eu-west-1.amazonaws.com
```

리전 엔드포인트 활성화:

```python
import boto3

# 리전 엔드포인트 사용
sts = boto3.client('sts', region_name='ap-northeast-2')
```

## 보안 고려사항

### 1. 세션 토큰 보호

```
⚠️ 주의:
- 세션 토큰을 로그에 출력하지 않음
- 세션 토큰을 URL에 포함하지 않음
- 세션 토큰을 버전 관리 시스템에 커밋하지 않음
```

### 2. 최소 권한 세션

세션 정책으로 역할 권한을 더 제한할 수 있습니다:

```bash
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/AdminRole \
    --role-session-name limited-session \
    --policy '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":"s3:GetObject","Resource":"*"}]}'
```

### 3. 소스 자격 증명 설정

역할 체이닝에서 원래 호출자 추적:

```bash
aws sts assume-role \
    --role-arn arn:aws:iam::123456789012:role/MyRole \
    --role-session-name my-session \
    --source-identity original-user
```

## 문제 해결

### 자격 증명 만료 오류

```
ExpiredTokenException: The security token included in the request is expired

해결 방법:
1. 새 임시 자격 증명 요청
2. SDK 자격 증명 캐시 새로고침
3. 더 긴 세션 기간 설정 고려
```

### 권한 거부 오류

```
AccessDenied: User is not authorized to perform: sts:AssumeRole

확인 사항:
1. 역할의 신뢰 정책에 호출자가 포함되어 있는지
2. 호출자에게 sts:AssumeRole 권한이 있는지
3. 외부 ID가 필요한 경우 올바르게 제공했는지
```

## 관련 문서

- [IAM 역할](./roles.md)
- [자격 증명 공급자](./identity-providers.md)
- [MFA 설정](../04_security/mfa.md)
