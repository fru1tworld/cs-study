# IAM 정책 (Policies)

IAM 정책은 AWS 리소스에 대한 접근 권한을 정의하는 JSON 문서입니다. 이 섹션에서는 IAM 정책의 모든 측면을 다룹니다.

## 목차

1. [정책 개요](./policy-overview.md)
   - IAM 정책이란?
   - 7가지 정책 유형
   - 정책 평가 순서

2. [정책 구조 (JSON)](./policy-structure.md)
   - JSON 정책 기본 구조
   - Statement 요소
   - 정책 변수
   - 실제 정책 예시

3. [관리형 vs 인라인 정책](./managed-vs-inline.md)
   - AWS 관리형 정책
   - 고객 관리형 정책
   - 인라인 정책
   - 선택 기준

4. [조건 키](./condition-keys.md)
   - 조건 연산자
   - 전역 조건 키
   - 서비스별 조건 키
   - 다중 값 조건

5. [정책 평가 로직](./evaluation-logic.md)
   - 단일 계정 평가 흐름
   - 교차 계정 접근 평가
   - 다중 정책 조합

6. [권한 경계](./permissions-boundary.md)
   - 권한 경계란?
   - 권한 위임 시나리오
   - 모범 사례

## 정책 유형 요약

| 정책 유형 | 연결 대상 | Principal 필요 | 용도 |
|-----------|----------|---------------|------|
| 자격 증명 기반 | 사용자, 그룹, 역할 | 아니오 | 권한 부여 |
| 리소스 기반 | 리소스 (S3 등) | 예 | 리소스별 접근 제어 |
| 권한 경계 | 사용자, 역할 | 아니오 | 최대 권한 제한 |
| SCP | 조직, OU, 계정 | 아니오 | 조직 가드레일 |
| 세션 정책 | 역할 세션 | 아니오 | 세션 권한 제한 |

## 정책 평가 우선순위

```
명시적 거부 (Deny) → 항상 최우선
        ↓
SCP (Organizations)
        ↓
리소스 기반 정책 (같은 계정 시 OR, 다른 계정 시 AND)
        ↓
자격 증명 기반 정책
        ↓
권한 경계
        ↓
세션 정책
        ↓
암묵적 거부 (기본값)
```

## 빠른 참조: 일반적인 정책 패턴

### 읽기 전용 접근

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:Get*",
                "s3:List*"
            ],
            "Resource": "*"
        }
    ]
}
```

### 특정 리소스만 접근

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": [
                "arn:aws:s3:::my-bucket",
                "arn:aws:s3:::my-bucket/*"
            ]
        }
    ]
}
```

### 태그 기반 접근 제어

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "ec2:*",
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Environment": "${aws:PrincipalTag/Environment}"
                }
            }
        }
    ]
}
```

### MFA 필수

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Deny",
            "Action": "*",
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

## 다음 단계

- 정책을 안전하게 사용하려면 [보안 모범 사례](../04_security/best-practices.md)를 참조하세요.
- IAM Access Analyzer로 정책을 검증하려면 [고급 기능](../05_advanced/access-analyzer.md)을 참조하세요.
