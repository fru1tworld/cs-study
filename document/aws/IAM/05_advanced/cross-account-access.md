# 교차 계정 접근 (Cross-Account Access)

## 교차 계정 접근이란?

한 AWS 계정의 사용자나 서비스가 다른 AWS 계정의 리소스에 접근하는 것을 말합니다. IAM 역할을 사용하여 안전하게 구현합니다.

## 사용 사례

### 1. 다중 계정 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Organizations                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 프로덕션 계정 │  │  개발 계정   │  │  로그 계정   │      │
│  │   (Prod)     │  │    (Dev)     │  │   (Logs)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                 │                 ▲               │
│         └─────────────────┴─────────────────┘               │
│                    로그 전송                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. 중앙 집중식 감사

```
개별 계정들 → 감사 계정
- CloudTrail 로그 중앙화
- 보안 스캐닝 중앙화
- 비용 데이터 집계
```

### 3. 공유 서비스

```
공유 서비스 계정:
- 공통 인프라 제공
- 중앙 집중식 서비스
- 각 계정에서 접근
```

### 4. 파트너/고객 접근

```
외부 조직에 제한된 접근 제공:
- 제3자 서비스 제공자
- 감사 업체
- 파트너사
```

## 구현 방법

### 방법 1: IAM 역할 사용 (권장)

가장 안전하고 권장되는 방법입니다.

```
┌─────────────────────┐          ┌─────────────────────┐
│      계정 A         │          │      계정 B         │
│   (신뢰하는 계정)    │          │   (신뢰받는 계정)   │
├─────────────────────┤          ├─────────────────────┤
│                     │          │                     │
│   IAM 사용자 ───────┼──────→   │    IAM 역할         │
│   (AssumeRole      │          │   (신뢰 정책에      │
│    권한 필요)       │          │    계정 A 포함)     │
│                     │          │                     │
└─────────────────────┘          └─────────────────────┘
```

#### 계정 B: 역할 생성 (신뢰받는 계정)

**신뢰 정책:**
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::111111111111:root"
            },
            "Action": "sts:AssumeRole",
            "Condition": {
                "StringEquals": {
                    "sts:ExternalId": "unique-external-id-12345"
                }
            }
        }
    ]
}
```

**권한 정책:**
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::shared-bucket",
                "arn:aws:s3:::shared-bucket/*"
            ]
        }
    ]
}
```

#### 계정 A: 사용자에게 역할 맡기 권한 부여

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "sts:AssumeRole",
            "Resource": "arn:aws:iam::222222222222:role/CrossAccountRole"
        }
    ]
}
```

### 방법 2: 리소스 기반 정책

일부 서비스(S3, KMS 등)에서 지원합니다.

```json
// S3 버킷 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::111111111111:user/johndoe"
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

**주의:** 이 방법은 양쪽 계정에서 허용이 필요합니다.
- 계정 A: 사용자의 자격 증명 기반 정책에서 s3:GetObject 등 허용
- 계정 B: 버킷 정책에서 계정 A의 사용자 허용

## 외부 ID (External ID)

### 혼동된 대리인 문제 (Confused Deputy Problem)

```
문제 시나리오:
1. 공격자가 자신의 서비스를 계정 B의 역할과 연결
2. 서비스 제공자가 의도치 않게 공격자 대신 계정 B에 접근
3. 계정 B의 리소스가 노출됨

해결책: 외부 ID 사용
```

### 외부 ID 구현

```json
// 계정 B의 역할 신뢰 정책
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::111111111111:root"
            },
            "Action": "sts:AssumeRole",
            "Condition": {
                "StringEquals": {
                    "sts:ExternalId": "my-unique-secret-12345"
                }
            }
        }
    ]
}
```

```bash
# 계정 A에서 역할 맡기
aws sts assume-role \
    --role-arn arn:aws:iam::222222222222:role/CrossAccountRole \
    --role-session-name my-session \
    --external-id my-unique-secret-12345
```

### 외부 ID 모범 사례

```
✅ 권장:
- 각 고객에게 고유한 외부 ID 생성
- 충분히 긴 랜덤 문자열 사용
- 외부 ID를 안전하게 보관
- 정기적으로 교체 (필요시)

❌ 금지:
- 예측 가능한 값 사용
- 동일한 외부 ID를 여러 고객에게 사용
- 외부 ID를 공개적으로 노출
```

## AWS Organizations 활용

### 조직 ID 기반 접근

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::shared-bucket/*",
            "Condition": {
                "StringEquals": {
                    "aws:PrincipalOrgID": "o-xxxxxxxxxxx"
                }
            }
        }
    ]
}
```

### 조직 경로 기반 접근

```json
{
    "Condition": {
        "ForAnyValue:StringLike": {
            "aws:PrincipalOrgPaths": [
                "o-xxxxxxxxxxx/r-xxxx/ou-xxxx-xxxxxxxx/*"
            ]
        }
    }
}
```

## 역할 체이닝

하나의 역할에서 다른 역할로 전환합니다.

```
사용자 → 역할 A → 역할 B

제한사항:
- 최대 세션 기간: 1시간
- 원래 자격 증명의 일부 속성 손실
```

```bash
# 역할 A 맡기
aws sts assume-role \
    --role-arn arn:aws:iam::222222222222:role/RoleA \
    --role-session-name sessionA

# 역할 B 맡기 (역할 A의 자격 증명 사용)
aws sts assume-role \
    --role-arn arn:aws:iam::333333333333:role/RoleB \
    --role-session-name sessionB
```

## 콘솔에서 역할 전환

### 역할 전환 설정

```
상단 메뉴 → 계정 이름 → 역할 전환
1. 계정 ID 입력 (12자리)
2. 역할 이름 입력
3. 표시 이름 설정 (선택)
4. 색상 선택 (선택)
5. 역할 전환
```

### 역할 전환 URL

```
https://signin.aws.amazon.com/switchrole
    ?account=222222222222
    &roleName=CrossAccountRole
    &displayName=ProdAccess
```

## 감사 및 모니터링

### CloudTrail 로그

```json
{
    "eventName": "AssumeRole",
    "userIdentity": {
        "type": "IAMUser",
        "accountId": "111111111111",
        "arn": "arn:aws:iam::111111111111:user/johndoe"
    },
    "requestParameters": {
        "roleArn": "arn:aws:iam::222222222222:role/CrossAccountRole",
        "roleSessionName": "my-session"
    }
}
```

### IAM Access Analyzer

```
교차 계정 접근 탐지:
- 의도하지 않은 외부 접근 식별
- 조직 외부 접근 경고
- 정기적인 검토 권장
```

## 모범 사례

### 1. 최소 권한

```
✅ 필요한 권한만 부여
✅ 특정 리소스로 제한
✅ 조건 사용 (IP, 시간 등)
```

### 2. 외부 ID 사용

```
✅ 제3자 접근 시 항상 외부 ID 사용
✅ 고유하고 추측 불가능한 값 사용
```

### 3. 조직 ID 조건

```
✅ 조직 내 접근에 aws:PrincipalOrgID 사용
✅ 개별 계정 ID 열거 대신 조직 조건 사용
```

### 4. 세션 기간 제한

```
✅ 민감한 작업: 짧은 세션 (1시간)
✅ 일반 작업: 적절한 세션 (4시간)
✅ 최대 세션 기간 적절히 설정
```

### 5. 정기적인 검토

```
✅ 교차 계정 역할 정기 감사
✅ 불필요한 접근 제거
✅ IAM Access Analyzer 활용
```

## 문제 해결

### AccessDenied 오류

```
확인 사항:
1. 신뢰 정책에 호출자 계정이 포함되어 있는지
2. 호출자에게 sts:AssumeRole 권한이 있는지
3. 외부 ID가 올바른지 (필요한 경우)
4. SCP가 역할 맡기를 차단하지 않는지
```

### MalformedPolicyDocument 오류

```
확인 사항:
1. Principal ARN 형식이 올바른지
2. JSON 문법이 올바른지
3. Version이 올바른지 (2012-10-17)
```

## 관련 문서

- [IAM 역할](../02_identities/roles.md)
- [임시 보안 자격 증명](../02_identities/temporary-credentials.md)
- [IAM Access Analyzer](./access-analyzer.md)
