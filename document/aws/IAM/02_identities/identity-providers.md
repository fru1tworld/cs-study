# IAM 자격 증명 공급자 (Identity Provider)

## 자격 증명 공급자란?

AWS 외부에서 사용자 자격 증명을 관리하고, 이들에게 AWS 리소스에 대한 접근 권한을 부여할 수 있는 엔터티입니다.

## 연합 인증의 이점

### IAM 사용자 생성 없이 접근 가능
- 기존 조직의 사용자 디렉토리 활용
- 별도의 AWS 자격 증명 관리 불필요
- 중앙 집중식 사용자 관리 유지

### 장기 자격 증명 배포 불필요
- 임시 자격 증명만 사용
- 액세스 키 유출 위험 감소
- 자격 증명 자동 만료

### SSO(Single Sign-On) 지원
- 사용자가 한 번만 로그인
- 여러 애플리케이션과 AWS에 접근
- 사용자 경험 향상

## 지원하는 연합 표준

### 1. SAML 2.0

Security Assertion Markup Language의 약자로, 기업 환경에서 많이 사용됩니다.

```
지원 IdP 예시:
- Microsoft Active Directory Federation Services (ADFS)
- Okta
- OneLogin
- Ping Identity
- Azure Active Directory
```

### 2. OpenID Connect (OIDC)

OAuth 2.0 기반의 인증 프로토콜로, 웹/모바일 애플리케이션에 적합합니다.

```
지원 IdP 예시:
- Google
- GitHub
- GitLab
- Salesforce
- Amazon Cognito
```

## AWS의 연합 방식

### 1. IAM Identity Center (권장)

다중 AWS 계정 환경에서 인력 자격 증명을 관리하는 권장 방법입니다.

```
특징:
- AWS Organizations과 통합
- 여러 계정에 대한 SSO
- 권한 세트 중앙 관리
- 자체 자격 증명 소스 또는 외부 IdP 연동
```

### 2. IAM 연합 (단일 계정)

단일 AWS 계정 환경에서 직접 연합을 설정합니다.

```
특징:
- 계정별로 설정 필요
- IAM 역할 직접 연결
- 소규모 배포에 적합
```

### 3. Amazon Cognito (앱 사용자)

모바일 및 웹 애플리케이션 사용자를 위한 연합입니다.

```
특징:
- 사용자 풀 관리
- 소셜 로그인 지원
- 게스트 접근 지원
- 수백만 사용자 지원
```

## SAML 기반 연합 설정

### 1단계: IdP에서 SAML 앱 생성

```
IdP 설정:
1. AWS를 서비스 공급자로 등록
2. 어설션 소비자 서비스(ACS) URL 설정
   - https://signin.aws.amazon.com/saml
3. 청중(Audience) URI 설정
   - urn:amazon:webservices
```

### 2단계: IAM에서 IdP 생성

```bash
# SAML 메타데이터 파일로 IdP 생성
aws iam create-saml-provider \
    --saml-metadata-document file://saml-metadata.xml \
    --name MyCompanyIdP
```

### 3단계: IAM 역할 생성

신뢰 정책:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::123456789012:saml-provider/MyCompanyIdP"
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

### 4단계: IdP에서 속성 매핑

```
필수 속성:
- https://aws.amazon.com/SAML/Attributes/Role
  → ARN of the IAM role and SAML provider

- https://aws.amazon.com/SAML/Attributes/RoleSessionName
  → 세션을 식별하는 이름

선택 속성:
- https://aws.amazon.com/SAML/Attributes/SessionDuration
  → 세션 기간 (초)
```

## OIDC 기반 연합 설정

### 1단계: IAM에서 OIDC 공급자 생성

```bash
aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list a]9e24510a91c8c9a3e3b72e1c5a9c0c5c5c5c5c5c5
```

### 2단계: IAM 역할 생성

GitHub Actions용 신뢰 정책:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                },
                "StringLike": {
                    "token.actions.githubusercontent.com:sub": "repo:myorg/myrepo:*"
                }
            }
        }
    ]
}
```

## 일반적인 연합 시나리오

### 기업 사용자의 AWS 콘솔 접근

```
┌──────────┐     ┌───────────┐     ┌──────────┐
│  사용자  │────→│ 기업 IdP  │────→│ AWS STS  │
│          │     │ (ADFS 등) │     │          │
└──────────┘     └───────────┘     └──────────┘
     │                 │                 │
     │    회사 자격 증명    SAML 어설션    │
     │    으로 로그인       전송           │
     │                 │                 │
     │                 └────────┬────────┘
     │                          │
     │                   임시 자격 증명
     │                          │
     │                          ▼
     └─────────────────→ AWS 콘솔 접근
```

### CI/CD 파이프라인의 AWS 접근 (GitHub Actions)

```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS

on: push

jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read

    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
          aws-region: us-east-1

      - name: Deploy
        run: aws s3 sync ./dist s3://my-bucket
```

### 모바일 앱의 AWS 접근 (Cognito)

```
┌──────────────┐    ┌─────────────────┐    ┌──────────────┐
│  모바일 앱   │───→│ Amazon Cognito  │───→│   AWS STS    │
│              │    │                 │    │              │
└──────────────┘    └─────────────────┘    └──────────────┘
       │                    │                     │
       │    소셜 로그인     │    자격 증명        │
       │    (Google 등)     │    풀 인증          │
       │                    │                     │
       │                    └─────────┬───────────┘
       │                              │
       │                       임시 자격 증명
       │                              │
       │                              ▼
       └──────────────────→ S3, DynamoDB 접근
```

## 자격 증명 공급자 관리

### 공급자 목록 조회

```bash
aws iam list-saml-providers
aws iam list-open-id-connect-providers
```

### 공급자 삭제

```bash
# SAML 공급자 삭제
aws iam delete-saml-provider \
    --saml-provider-arn arn:aws:iam::123456789012:saml-provider/MyIdP

# OIDC 공급자 삭제
aws iam delete-open-id-connect-provider \
    --open-id-connect-provider-arn arn:aws:iam::123456789012:oidc-provider/example.com
```

### 메타데이터 업데이트

```bash
aws iam update-saml-provider \
    --saml-provider-arn arn:aws:iam::123456789012:saml-provider/MyIdP \
    --saml-metadata-document file://new-metadata.xml
```

## 보안 고려사항

### 1. 조건 키 활용

```json
{
    "Condition": {
        "StringEquals": {
            "SAML:aud": "https://signin.aws.amazon.com/saml",
            "SAML:iss": "https://idp.example.com"
        }
    }
}
```

### 2. 세션 기간 제한

```json
{
    "Condition": {
        "NumericLessThanEquals": {
            "saml:SessionDuration": "3600"
        }
    }
}
```

### 3. 정기적인 인증서/키 교체

```
SAML: IdP 서명 인증서 정기 업데이트
OIDC: 썸프린트 정기 확인 및 업데이트
```

## 관련 문서

- [IAM 역할](./roles.md)
- [임시 보안 자격 증명](./temporary-credentials.md)
- [보안 모범 사례](../04_security/best-practices.md)
