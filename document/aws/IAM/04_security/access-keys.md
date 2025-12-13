# IAM 액세스 키 관리

## 액세스 키란?

액세스 키는 IAM 사용자 또는 루트 사용자를 위한 장기 자격 증명입니다. AWS CLI, SDK, API를 통한 프로그래밍 방식의 요청을 인증하는 데 사용됩니다.

## 액세스 키 구성

```
액세스 키 ID:        AKIAIOSFODNN7EXAMPLE
비밀 액세스 키:      wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

두 값을 함께 사용하여 요청 서명
```

## 액세스 키 제한 사항

| 항목 | 제한 |
|------|------|
| 사용자당 최대 액세스 키 수 | 2개 |
| 비밀 키 조회 가능 시점 | 생성 시에만 |
| 비밀 키 분실 시 | 삭제 후 새로 생성 |

## 액세스 키 vs 임시 자격 증명

| 항목 | 액세스 키 | 임시 자격 증명 |
|------|----------|--------------|
| 유효 기간 | 영구 (삭제까지) | 몇 분 ~ 12시간 |
| 교체 필요 | 수동 교체 | 자동 만료 |
| 노출 위험 | 높음 | 낮음 |
| 관리 복잡도 | 높음 | 낮음 |
| 권장 사용 | 불가피한 경우만 | 대부분의 경우 |

## 언제 액세스 키를 사용해야 하나?

### 임시 자격 증명 우선

```
✅ 임시 자격 증명 사용 (권장):
- EC2 인스턴스 → IAM 역할
- Lambda 함수 → 실행 역할
- ECS/EKS → 태스크/서비스 역할
- 연합 사용자 → SAML/OIDC
- CI/CD → GitHub Actions OIDC
```

### 액세스 키가 필요한 경우

```
⚠️ 액세스 키 사용 (불가피한 경우):
- IAM 역할을 지원하지 않는 서드파티 도구
- 로컬 개발 환경
- AWS 외부 서버/서비스
- 특정 레거시 시스템
```

## 액세스 키 생성

### AWS 콘솔에서 생성

```
IAM 콘솔 → 사용자 → 사용자 선택 → 보안 자격 증명 탭
1. 액세스 키 만들기
2. 사용 사례 선택
3. 키 생성
4. 비밀 키 다운로드 또는 복사 (이때만 가능!)
```

### AWS CLI로 생성

```bash
# 액세스 키 생성
aws iam create-access-key --user-name johndoe

# 응답
{
    "AccessKey": {
        "UserName": "johndoe",
        "AccessKeyId": "AKIAIOSFODNN7EXAMPLE",
        "Status": "Active",
        "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        "CreateDate": "2024-01-15T10:00:00Z"
    }
}

⚠️ 비밀 키는 이 응답에서만 확인 가능!
```

## 액세스 키 저장

### 권장하지 않는 방법

```
❌ 절대 금지:
- 코드에 하드코딩
- 버전 관리 시스템에 커밋
- 이메일/메신저로 전송
- 공개 저장소에 업로드
- 공유 문서에 저장
```

### 권장하는 방법

```
✅ 권장:
- AWS CLI 자격 증명 파일 (~/.aws/credentials)
- 환경 변수
- 비밀 관리 서비스 (AWS Secrets Manager)
- 하드웨어 보안 모듈 (HSM)
```

### AWS CLI 자격 증명 파일

```ini
# ~/.aws/credentials
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

[dev]
aws_access_key_id = AKIAIOSFODNN7DEV
aws_secret_access_key = devSecretKeyExample

# 파일 권한 확인
chmod 600 ~/.aws/credentials
```

### 환경 변수

```bash
# Linux/macOS
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Windows PowerShell
$env:AWS_ACCESS_KEY_ID="AKIAIOSFODNN7EXAMPLE"
$env:AWS_SECRET_ACCESS_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
```

## 액세스 키 교체

### 교체 주기

```
권장 주기: 90일

교체 이유:
- 보안 강화
- 노출 위험 감소
- 규정 준수 요구사항
```

### 교체 절차

```bash
# 1. 현재 액세스 키 확인
aws iam list-access-keys --user-name johndoe

# 2. 새 액세스 키 생성 (최대 2개까지 가능)
aws iam create-access-key --user-name johndoe

# 3. 새 키로 애플리케이션 업데이트
# - 설정 파일 수정
# - 환경 변수 업데이트
# - Secrets Manager 업데이트

# 4. 새 키 동작 확인
aws sts get-caller-identity

# 5. 기존 키 비활성화
aws iam update-access-key \
    --user-name johndoe \
    --access-key-id AKIAIOSFODNN7OLD \
    --status Inactive

# 6. 일정 기간 모니터링 후 삭제
aws iam delete-access-key \
    --user-name johndoe \
    --access-key-id AKIAIOSFODNN7OLD
```

### 자동 교체 (AWS Secrets Manager)

```python
# Lambda 함수로 자동 교체 구현 가능
import boto3
import json

def rotate_access_key(event, context):
    iam = boto3.client('iam')
    secrets = boto3.client('secretsmanager')

    # 새 키 생성
    new_key = iam.create_access_key(UserName='service-user')

    # Secrets Manager 업데이트
    secrets.put_secret_value(
        SecretId='service-user-key',
        SecretString=json.dumps({
            'AccessKeyId': new_key['AccessKey']['AccessKeyId'],
            'SecretAccessKey': new_key['AccessKey']['SecretAccessKey']
        })
    )

    # 기존 키 삭제 로직...
```

## 액세스 키 모니터링

### 마지막 사용 시간 확인

```bash
aws iam get-access-key-last-used \
    --access-key-id AKIAIOSFODNN7EXAMPLE

# 응답
{
    "UserName": "johndoe",
    "AccessKeyLastUsed": {
        "LastUsedDate": "2024-01-15T10:30:00Z",
        "ServiceName": "s3",
        "Region": "us-east-1"
    }
}
```

### 미사용 키 식별

```bash
# 자격 증명 보고서 생성
aws iam generate-credential-report

# 보고서 다운로드 및 분석
aws iam get-credential-report \
    --output text \
    --query Content | base64 -d > credential-report.csv

# 보고서에서 확인:
# - access_key_1_last_used_date
# - access_key_2_last_used_date
```

### CloudTrail 로그 분석

```
CloudTrail에서 확인:
- 어떤 액세스 키가 사용되었는지
- 어떤 API 호출에 사용되었는지
- 어디서 요청이 왔는지 (IP 주소)
```

## 노출된 키 대응

### 즉시 조치

```bash
# 1. 키 즉시 비활성화
aws iam update-access-key \
    --user-name johndoe \
    --access-key-id AKIAIOSFODNN7EXPOSED \
    --status Inactive

# 2. 키 삭제
aws iam delete-access-key \
    --user-name johndoe \
    --access-key-id AKIAIOSFODNN7EXPOSED

# 3. 새 키 생성 (필요한 경우)
aws iam create-access-key --user-name johndoe
```

### 추가 조치

```
확인 및 대응:
1. CloudTrail 로그 검토 - 비정상적인 활동 확인
2. 생성된 리소스 확인 - 의심스러운 EC2, Lambda 등
3. IAM 사용자 검토 - 새로운 사용자가 생성되었는지
4. 결제 알림 확인 - 비정상적인 비용 발생
5. AWS Support 연락 - 심각한 경우
```

### AWS 자동 탐지

```
AWS는 다음을 자동으로 탐지:
- GitHub 공개 저장소의 액세스 키
- 탐지 시 자동 이메일 알림
- AWS Health Dashboard에 이벤트 생성
```

## AWS Config 규칙

### access-keys-rotated

```yaml
# 90일 이상 교체되지 않은 키 탐지
rule:
  identifier: ACCESS_KEYS_ROTATED
  inputParameters:
    maxAccessKeyAge: 90
```

### iam-user-unused-credentials-check

```yaml
# 미사용 자격 증명 탐지
rule:
  identifier: IAM_USER_UNUSED_CREDENTIALS_CHECK
  inputParameters:
    maxCredentialUsageAge: 90
```

## 모범 사례 요약

### Do's (해야 할 것)

```
✅ 임시 자격 증명 우선 사용
✅ 정기적 교체 (90일)
✅ 마지막 사용 시간 모니터링
✅ 미사용 키 비활성화/삭제
✅ 안전한 저장소 사용
✅ 사용자당 필요한 최소 키만 유지
✅ 키별 용도 문서화
```

### Don'ts (하지 말아야 할 것)

```
❌ 루트 사용자 액세스 키 생성
❌ 코드에 키 하드코딩
❌ 버전 관리에 키 커밋
❌ 키 공유
❌ 키를 이메일/메신저로 전송
❌ 불필요하게 많은 키 생성
```

## 관련 문서

- [보안 모범 사례](./best-practices.md)
- [MFA 설정](./mfa.md)
- [임시 자격 증명](../02_identities/temporary-credentials.md)
