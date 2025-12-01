# IAM Access Analyzer

## IAM Access Analyzer란?

IAM Access Analyzer는 AWS 리소스에 대한 접근을 분석하여 보안 위험을 식별하는 서비스입니다. 외부 엔터티와 공유되는 리소스를 자동으로 탐지하고, 정책을 검증하며, 최소 권한 정책을 생성하는 데 도움을 줍니다.

## 주요 기능

### 1. 외부 접근 분석

외부 엔터티(다른 AWS 계정, 퍼블릭 등)와 공유되는 리소스를 탐지합니다.

```
지원 리소스:
- S3 버킷
- IAM 역할
- KMS 키
- Lambda 함수 및 계층
- SQS 대기열
- Secrets Manager 비밀
- SNS 토픽
- EBS 볼륨 스냅샷
- RDS DB 스냅샷
- ECR 저장소
```

### 2. 미사용 접근 분석

사용되지 않는 권한, 역할, 자격 증명을 식별합니다.

```
분석 대상:
- 미사용 IAM 역할
- 미사용 IAM 사용자 자격 증명
- 미사용 접근 키
- 미사용 비밀번호
- 미사용 권한
```

### 3. 정책 검증

정책 문법 및 모범 사례를 검증합니다.

```
검증 항목 (100개 이상):
- 보안 경고
- 오류
- 제안 사항
- 모범 사례 위반
```

### 4. 정책 생성

CloudTrail 로그를 기반으로 최소 권한 정책을 생성합니다.

## 분석기 생성

### AWS 콘솔에서 생성

```
IAM 콘솔 → Access Analyzer → 분석기 생성
1. 분석기 이름 입력
2. 분석 유형 선택 (외부 접근 또는 미사용 접근)
3. 신뢰 영역 선택 (현재 계정 또는 조직)
4. 생성
```

### AWS CLI로 생성

```bash
# 외부 접근 분석기 생성 (계정 수준)
aws accessanalyzer create-analyzer \
    --analyzer-name my-account-analyzer \
    --type ACCOUNT

# 외부 접근 분석기 생성 (조직 수준)
aws accessanalyzer create-analyzer \
    --analyzer-name my-org-analyzer \
    --type ORGANIZATION

# 미사용 접근 분석기 생성
aws accessanalyzer create-analyzer \
    --analyzer-name my-unused-analyzer \
    --type ACCOUNT_UNUSED_ACCESS \
    --configuration '{"unusedAccess": {"unusedAccessAge": 90}}'
```

## 외부 접근 분석

### 신뢰 영역 (Zone of Trust)

```
계정 수준 분석기:
- 신뢰 영역: 현재 AWS 계정
- 외부: 다른 모든 AWS 계정, 익명 사용자

조직 수준 분석기:
- 신뢰 영역: 조직 내 모든 계정
- 외부: 조직 외부의 모든 엔터티
```

### 발견 사항 (Findings)

분석기가 외부 접근을 탐지하면 발견 사항을 생성합니다.

```json
{
    "analyzedAt": "2024-01-15T10:00:00Z",
    "createdAt": "2024-01-15T10:00:00Z",
    "id": "finding-id",
    "resource": "arn:aws:s3:::my-public-bucket",
    "resourceType": "AWS::S3::Bucket",
    "status": "ACTIVE",
    "principal": {
        "AWS": "*"
    },
    "condition": {},
    "action": ["s3:GetObject"],
    "isPublic": true
}
```

### 발견 사항 상태

| 상태 | 설명 |
|------|------|
| Active | 새로 탐지된 외부 접근 |
| Archived | 사용자가 검토 후 보관 |
| Resolved | 외부 접근이 제거됨 |

### 발견 사항 관리

```bash
# 발견 사항 목록 조회
aws accessanalyzer list-findings \
    --analyzer-arn arn:aws:access-analyzer:us-east-1:123456789012:analyzer/my-analyzer

# 발견 사항 보관 (의도적인 공유인 경우)
aws accessanalyzer update-findings \
    --analyzer-arn arn:aws:access-analyzer:us-east-1:123456789012:analyzer/my-analyzer \
    --ids finding-id \
    --status ARCHIVED

# 보관 규칙 생성 (자동 보관)
aws accessanalyzer create-archive-rule \
    --analyzer-name my-analyzer \
    --rule-name trusted-accounts \
    --filter '{"principal": {"eq": ["123456789012"]}}'
```

## 미사용 접근 분석

### 설정

```bash
# 미사용 접근 분석기 생성 (90일 미사용 기준)
aws accessanalyzer create-analyzer \
    --analyzer-name unused-access-analyzer \
    --type ACCOUNT_UNUSED_ACCESS \
    --configuration '{
        "unusedAccess": {
            "unusedAccessAge": 90
        }
    }'
```

### 분석 결과

```
미사용 IAM 역할:
- 90일 동안 사용되지 않은 역할

미사용 권한:
- 부여되었지만 사용되지 않는 권한
- 마지막 사용 이후 90일 경과

미사용 자격 증명:
- 미사용 액세스 키
- 미사용 콘솔 비밀번호
```

## 정책 검증

### 콘솔에서 검증

```
IAM 콘솔 → 정책 생성/편집
- 실시간으로 검증 결과 표시
- 보안 경고, 오류, 제안 확인
```

### CLI로 검증

```bash
aws accessanalyzer validate-policy \
    --policy-document file://policy.json \
    --policy-type IDENTITY_POLICY

# 응답 예시
{
    "findings": [
        {
            "findingType": "SECURITY_WARNING",
            "issueCode": "PASS_ROLE_WITH_STAR_IN_RESOURCE",
            "learnMoreLink": "https://...",
            "locations": [{"path": [...]}],
            "findingDetails": "..."
        }
    ]
}
```

### 검증 결과 유형

| 유형 | 설명 | 조치 |
|------|------|------|
| ERROR | 정책 문법 오류 | 반드시 수정 |
| SECURITY_WARNING | 보안 위험 | 검토 후 수정 권장 |
| SUGGESTION | 개선 제안 | 선택적 적용 |
| WARNING | 일반 경고 | 검토 권장 |

### 일반적인 검증 결과

```
PASS_ROLE_WITH_STAR_IN_RESOURCE:
→ iam:PassRole에 리소스 "*" 사용

ALLOW_WITH_NOT_PRINCIPAL:
→ NotPrincipal과 Allow 함께 사용

MISSING_VERSION:
→ Version 요소 누락

EMPTY_ARRAY_ACTION:
→ 빈 Action 배열

REDUNDANT_ACTION:
→ 중복된 Action
```

## 정책 생성

CloudTrail 로그를 분석하여 실제 사용된 권한만 포함하는 정책을 생성합니다.

### 정책 생성 시작

```bash
aws accessanalyzer start-policy-generation \
    --policy-generation-details '{
        "principalArn": "arn:aws:iam::123456789012:role/MyRole"
    }' \
    --cloud-trail-details '{
        "trails": [
            {
                "cloudTrailArn": "arn:aws:cloudtrail:us-east-1:123456789012:trail/my-trail",
                "regions": ["us-east-1", "ap-northeast-2"]
            }
        ],
        "accessRole": "arn:aws:iam::123456789012:role/AccessAnalyzerMonitorServiceRole",
        "startTime": "2023-01-01T00:00:00Z",
        "endTime": "2024-01-01T00:00:00Z"
    }'
```

### 생성된 정책 조회

```bash
aws accessanalyzer get-generated-policy \
    --job-id job-id

# 응답
{
    "generatedPolicyResult": {
        "properties": {
            "isComplete": true,
            "principalArn": "..."
        },
        "generatedPolicies": [
            {
                "policy": "{...}"
            }
        ]
    }
}
```

## 리전 및 요금

### 리전 설정

IAM Access Analyzer는 리전별 서비스입니다.

```
각 리전에서 별도로 활성화 필요:
- 분석기는 해당 리전의 리소스만 분석
- 전체 커버리지를 위해 모든 사용 리전에서 활성화
```

### 요금

```
외부 접근 분석:
- 무료

미사용 접근 분석:
- 분석된 IAM 역할 및 사용자 수당 요금
- 월간 분석 대상 수 기준

정책 검증:
- 무료

정책 생성:
- 생성된 정책당 요금
```

## EventBridge 통합

새 발견 사항에 대한 자동 알림을 설정할 수 있습니다.

```json
// EventBridge 규칙
{
    "source": ["aws.access-analyzer"],
    "detail-type": ["Access Analyzer Finding"],
    "detail": {
        "status": ["ACTIVE"],
        "isPublic": [true]
    }
}
```

### 알림 설정 예시

```bash
# SNS 토픽으로 알림
aws events put-rule \
    --name "access-analyzer-public-findings" \
    --event-pattern '{
        "source": ["aws.access-analyzer"],
        "detail-type": ["Access Analyzer Finding"],
        "detail": {"isPublic": [true]}
    }'

aws events put-targets \
    --rule "access-analyzer-public-findings" \
    --targets "Id"="1","Arn"="arn:aws:sns:us-east-1:123456789012:security-alerts"
```

## 모범 사례

### 1. 모든 리전에서 활성화

```
각 리전에서 분석기 생성:
- 조직 수준 분석기 (Organizations 사용 시)
- 계정 수준 분석기 (단일 계정)
```

### 2. 정기적인 발견 사항 검토

```
검토 주기: 주간 또는 월간

검토 프로세스:
1. 새 발견 사항 확인
2. 의도적 공유 여부 판단
3. 불필요한 공유 제거
4. 의도적 공유는 보관
```

### 3. 보관 규칙 활용

```
신뢰할 수 있는 계정에 대한 자동 보관:
- 파트너 계정
- 관련 조직 계정
- 서비스 계정
```

### 4. 자동 알림 설정

```
EventBridge + SNS:
- 퍼블릭 접근 탐지 시 즉시 알림
- 새 외부 계정 접근 탐지 시 알림
```

## 관련 문서

- [보안 모범 사례](../04_security/best-practices.md)
- [정책 평가 로직](../03_policies/evaluation-logic.md)
- [교차 계정 접근](./cross-account-access.md)
