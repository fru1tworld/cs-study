# 관리형 정책 vs 인라인 정책

## 정책 연결 방식

IAM 정책은 자격 증명(사용자, 그룹, 역할)에 두 가지 방식으로 연결할 수 있습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                        정책 유형                             │
├────────────────────────────┬────────────────────────────────┤
│        관리형 정책          │         인라인 정책             │
├────────────────────────────┼────────────────────────────────┤
│  ┌──────────────────────┐  │  ┌──────────────────────────┐  │
│  │    AWS 관리형 정책    │  │  │  자격 증명에 직접 내장    │  │
│  │  (AWS가 생성/관리)    │  │  │                          │  │
│  └──────────────────────┘  │  │  사용자 ←──→ 정책        │  │
│                            │  │  그룹  ←──→ 정책         │  │
│  ┌──────────────────────┐  │  │  역할  ←──→ 정책         │  │
│  │   고객 관리형 정책    │  │  │                          │  │
│  │  (사용자가 생성/관리) │  │  │  (1:1 관계)              │  │
│  └──────────────────────┘  │  └──────────────────────────┘  │
│                            │                                │
│  (여러 자격 증명에 연결)   │  (하나의 자격 증명에만 연결)    │
└────────────────────────────┴────────────────────────────────┘
```

## 관리형 정책 (Managed Policies)

### AWS 관리형 정책

AWS가 생성하고 관리하는 독립형 정책입니다.

```
특징:
- AWS가 권한 변경을 자동으로 업데이트
- 일반적인 사용 사례에 맞게 설계
- 삭제 불가 (사용만 가능)
- arn:aws:iam::aws:policy/... 형식
```

** 일반적인 AWS 관리형 정책:**

| 정책 이름 | 설명 |
|-----------|------|
| `AdministratorAccess` | 모든 AWS 서비스와 리소스에 전체 접근 |
| `PowerUserAccess` | IAM 제외 모든 서비스 전체 접근 |
| `ReadOnlyAccess` | 모든 서비스 읽기 전용 접근 |
| `ViewOnlyAccess` | 리소스 및 메타데이터 조회만 가능 |
| `AmazonS3FullAccess` | S3 전체 접근 |
| `AmazonEC2ReadOnlyAccess` | EC2 읽기 전용 접근 |
| `AmazonDynamoDBFullAccess` | DynamoDB 전체 접근 |

### 고객 관리형 정책

사용자가 직접 생성하고 관리하는 정책입니다.

```
특징:
- 조직의 요구사항에 맞게 커스터마이징
- 여러 자격 증명에 재사용 가능
- 버전 관리 지원 (최대 5개 버전)
- 정책 수정 시 연결된 모든 자격 증명에 적용
```

### 관리형 정책 생성

```bash
# CLI로 고객 관리형 정책 생성
aws iam create-policy \
    --policy-name MyCustomPolicy \
    --policy-document file://policy.json

# 정책을 사용자에게 연결
aws iam attach-user-policy \
    --user-name johndoe \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy

# 정책을 그룹에 연결
aws iam attach-group-policy \
    --group-name Developers \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy

# 정책을 역할에 연결
aws iam attach-role-policy \
    --role-name MyRole \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy
```

### 버전 관리

```bash
# 새 버전 생성
aws iam create-policy-version \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy \
    --policy-document file://updated-policy.json \
    --set-as-default

# 버전 목록 조회
aws iam list-policy-versions \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy

# 기본 버전 변경
aws iam set-default-policy-version \
    --policy-arn arn:aws:iam::123456789012:policy/MyCustomPolicy \
    --version-id v2
```

## 인라인 정책 (Inline Policies)

단일 자격 증명에 직접 내장되는 정책입니다.

```
특징:
- 자격 증명과 1:1 관계
- 자격 증명 삭제 시 함께 삭제
- 재사용 불가
- 버전 관리 없음
```

### 인라인 정책 생성

```bash
# 사용자에게 인라인 정책 추가
aws iam put-user-policy \
    --user-name johndoe \
    --policy-name MyInlinePolicy \
    --policy-document file://policy.json

# 그룹에 인라인 정책 추가
aws iam put-group-policy \
    --group-name Developers \
    --policy-name MyInlinePolicy \
    --policy-document file://policy.json

# 역할에 인라인 정책 추가
aws iam put-role-policy \
    --role-name MyRole \
    --policy-name MyInlinePolicy \
    --policy-document file://policy.json
```

## 비교 표

| 항목 | AWS 관리형 | 고객 관리형 | 인라인 |
|------|-----------|-----------|--------|
| 생성자 | AWS | 사용자 | 사용자 |
| 재사용 가능 | 예 | 예 | 아니오 |
| 버전 관리 | AWS 관리 | 예 (최대 5개) | 아니오 |
| 자격 증명 삭제 시 | 유지됨 | 유지됨 | 함께 삭제 |
| 중앙 관리 | 예 | 예 | 아니오 |
| 커스터마이징 | 불가 | 가능 | 가능 |
| 최대 크기 | 6,144자 | 6,144자 | 2,048-5,120자 |

## 사용 권장 사항

### 관리형 정책 사용 (권장)

```
✅ 관리형 정책 사용이 권장되는 경우:
- 여러 자격 증명에 동일한 권한 부여
- 중앙에서 권한 관리 필요
- 정책 변경 이력 추적 필요
- 대부분의 일반적인 상황
```

### AWS 관리형 정책으로 시작

```
권장 접근 방식:
1. AWS 관리형 정책으로 빠르게 시작
2. IAM Access Analyzer로 실제 사용 권한 분석
3. 필요에 맞게 고객 관리형 정책 생성
4. 최소 권한 원칙 적용
```

### 인라인 정책 사용

```
⚠️ 인라인 정책 사용이 적절한 경우:
- 특정 자격 증명에만 적용되는 고유한 권한
- 권한이 다른 자격 증명과 공유되면 안 되는 경우
- 정책이 해당 자격 증명에만 종속되어야 하는 경우
```

## 정책 연결 제한

### 자격 증명당 정책 제한

| 자격 증명 | 관리형 정책 | 인라인 정책 크기 |
|----------|-----------|----------------|
| IAM 사용자 | 10개 | 2,048자 |
| IAM 그룹 | 10개 | 5,120자 |
| IAM 역할 | 10개 | 10,240자 |

### 계정 제한

| 항목 | 기본 제한 |
|------|----------|
| 고객 관리형 정책 수 | 1,500 |
| 관리형 정책 버전 수 | 5 |

## 정책 마이그레이션

### 인라인 → 관리형 변환

```bash
# 1. 인라인 정책 내용 가져오기
aws iam get-user-policy \
    --user-name johndoe \
    --policy-name MyInlinePolicy \
    --query PolicyDocument \
    --output json > policy.json

# 2. 관리형 정책 생성
aws iam create-policy \
    --policy-name MyManagedPolicy \
    --policy-document file://policy.json

# 3. 관리형 정책 연결
aws iam attach-user-policy \
    --user-name johndoe \
    --policy-arn arn:aws:iam::123456789012:policy/MyManagedPolicy

# 4. 인라인 정책 삭제
aws iam delete-user-policy \
    --user-name johndoe \
    --policy-name MyInlinePolicy
```

## 관련 문서

- [정책 개요](./policy-overview.md)
- [정책 구조](./policy-structure.md)
- [보안 모범 사례](../04_security/best-practices.md)
