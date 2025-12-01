# IAM 정책 조건 키

## 조건 (Condition) 개요

정책의 `Condition` 요소를 사용하면 정책이 적용되는 상황을 세밀하게 제어할 수 있습니다. 조건이 충족될 때만 정책이 적용됩니다.

## 조건 구조

```json
"Condition": {
    "조건연산자": {
        "조건키": "조건값"
    }
}
```

### 복합 조건 예시

```json
"Condition": {
    "StringEquals": {
        "aws:RequestTag/Environment": "Production"
    },
    "IpAddress": {
        "aws:SourceIp": "192.0.2.0/24"
    }
}
```

** 중요:** 같은 수준의 여러 조건은 **AND** 로직으로 평가됩니다.

## 조건 연산자

### 문자열 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `StringEquals` | 정확히 일치 (대소문자 구분) |
| `StringNotEquals` | 일치하지 않음 |
| `StringEqualsIgnoreCase` | 정확히 일치 (대소문자 무시) |
| `StringNotEqualsIgnoreCase` | 일치하지 않음 (대소문자 무시) |
| `StringLike` | 와일드카드(*,?) 패턴 일치 |
| `StringNotLike` | 와일드카드 패턴 불일치 |

```json
// StringEquals 예시
"Condition": {
    "StringEquals": {
        "s3:prefix": "home/johndoe"
    }
}

// StringLike 예시 (와일드카드)
"Condition": {
    "StringLike": {
        "s3:prefix": "home/*"
    }
}
```

### 숫자 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `NumericEquals` | 같음 |
| `NumericNotEquals` | 같지 않음 |
| `NumericLessThan` | 미만 |
| `NumericLessThanEquals` | 이하 |
| `NumericGreaterThan` | 초과 |
| `NumericGreaterThanEquals` | 이상 |

```json
// 요청 시간 제한
"Condition": {
    "NumericLessThanEquals": {
        "s3:max-keys": "10"
    }
}
```

### 날짜 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `DateEquals` | 특정 날짜/시간 일치 |
| `DateNotEquals` | 특정 날짜/시간 불일치 |
| `DateLessThan` | 이전 |
| `DateLessThanEquals` | 이전 또는 같음 |
| `DateGreaterThan` | 이후 |
| `DateGreaterThanEquals` | 이후 또는 같음 |

```json
// 특정 기간 동안만 허용
"Condition": {
    "DateGreaterThan": {"aws:CurrentTime": "2024-01-01T00:00:00Z"},
    "DateLessThan": {"aws:CurrentTime": "2024-12-31T23:59:59Z"}
}
```

### 부울 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `Bool` | true 또는 false |

```json
// HTTPS만 허용
"Condition": {
    "Bool": {
        "aws:SecureTransport": "true"
    }
}
```

### IP 주소 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `IpAddress` | IP 범위 내 |
| `NotIpAddress` | IP 범위 외 |

```json
// 특정 IP 범위만 허용
"Condition": {
    "IpAddress": {
        "aws:SourceIp": ["192.0.2.0/24", "203.0.113.0/24"]
    }
}
```

### ARN 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `ArnEquals` | ARN 일치 |
| `ArnLike` | ARN 패턴 일치 (와일드카드 지원) |
| `ArnNotEquals` | ARN 불일치 |
| `ArnNotLike` | ARN 패턴 불일치 |

```json
"Condition": {
    "ArnLike": {
        "aws:SourceArn": "arn:aws:sns:*:123456789012:my-topic"
    }
}
```

### Null 조건 연산자

| 연산자 | 설명 |
|--------|------|
| `Null` | 키가 존재하는지 확인 |

```json
// 태그가 있는 경우만 허용
"Condition": {
    "Null": {
        "aws:RequestTag/Environment": "false"
    }
}
```

### IfExists 연산자

조건 키가 존재할 때만 평가합니다. 키가 없으면 조건을 무시합니다.

```json
// MFA가 있으면 확인, 없으면 무시
"Condition": {
    "BoolIfExists": {
        "aws:MultiFactorAuthPresent": "true"
    }
}
```

## 전역 조건 키

모든 AWS 서비스에서 사용 가능한 조건 키입니다.

### 주체 (Principal) 관련 키

| 키 | 설명 |
|----|------|
| `aws:PrincipalArn` | 요청자의 ARN |
| `aws:PrincipalAccount` | 요청자의 계정 ID |
| `aws:PrincipalOrgID` | 요청자의 조직 ID |
| `aws:PrincipalTag/키` | 요청자의 태그 값 |
| `aws:PrincipalType` | 주체 유형 |
| `aws:userid` | 요청자의 사용자 ID |
| `aws:username` | 요청자의 사용자 이름 |

```json
// 특정 조직의 주체만 허용
"Condition": {
    "StringEquals": {
        "aws:PrincipalOrgID": "o-xxxxxxxxxxx"
    }
}
```

### 요청 관련 키

| 키 | 설명 |
|----|------|
| `aws:CurrentTime` | 현재 날짜/시간 |
| `aws:EpochTime` | Unix 타임스탬프 |
| `aws:RequestedRegion` | 요청 대상 리전 |
| `aws:SecureTransport` | HTTPS 사용 여부 |
| `aws:SourceIp` | 요청 IP 주소 |
| `aws:UserAgent` | HTTP User-Agent |

```json
// 특정 리전만 허용
"Condition": {
    "StringEquals": {
        "aws:RequestedRegion": ["us-east-1", "ap-northeast-2"]
    }
}
```

### 리소스 관련 키

| 키 | 설명 |
|----|------|
| `aws:ResourceAccount` | 리소스 소유 계정 |
| `aws:ResourceOrgID` | 리소스 소유 조직 |
| `aws:ResourceTag/키` | 리소스의 태그 값 |

```json
// 특정 태그가 있는 리소스만 접근
"Condition": {
    "StringEquals": {
        "aws:ResourceTag/Environment": "Production"
    }
}
```

### 태그 관련 키

| 키 | 설명 |
|----|------|
| `aws:RequestTag/키` | 요청에 포함된 태그 |
| `aws:TagKeys` | 요청의 모든 태그 키 |

```json
// 특정 태그를 포함해야만 리소스 생성 가능
"Condition": {
    "StringEquals": {
        "aws:RequestTag/CostCenter": "12345"
    },
    "ForAllValues:StringEquals": {
        "aws:TagKeys": ["CostCenter", "Environment"]
    }
}
```

### MFA 관련 키

| 키 | 설명 |
|----|------|
| `aws:MultiFactorAuthPresent` | MFA 인증 여부 |
| `aws:MultiFactorAuthAge` | MFA 인증 후 경과 시간(초) |

```json
// MFA 필수
"Condition": {
    "Bool": {
        "aws:MultiFactorAuthPresent": "true"
    }
}

// MFA 후 1시간 이내
"Condition": {
    "NumericLessThan": {
        "aws:MultiFactorAuthAge": "3600"
    }
}
```

### VPC 관련 키

| 키 | 설명 |
|----|------|
| `aws:SourceVpc` | 요청이 시작된 VPC ID |
| `aws:SourceVpce` | VPC 엔드포인트 ID |
| `aws:VpcSourceIp` | VPC 내 소스 IP |

```json
// 특정 VPC에서만 접근 가능
"Condition": {
    "StringEquals": {
        "aws:SourceVpc": "vpc-1234567890abcdef0"
    }
}
```

## 서비스별 조건 키

각 서비스는 고유한 조건 키를 제공합니다.

### S3 조건 키 예시

| 키 | 설명 |
|----|------|
| `s3:prefix` | 객체 키 접두사 |
| `s3:max-keys` | 나열할 최대 키 수 |
| `s3:x-amz-acl` | 요청된 ACL |
| `s3:x-amz-server-side-encryption` | 서버 측 암호화 |

```json
// 특정 접두사만 접근
"Condition": {
    "StringLike": {
        "s3:prefix": "home/${aws:username}/*"
    }
}
```

### EC2 조건 키 예시

| 키 | 설명 |
|----|------|
| `ec2:InstanceType` | 인스턴스 유형 |
| `ec2:Region` | 리전 |
| `ec2:ResourceTag/키` | 리소스 태그 |

```json
// 특정 인스턴스 유형만 허용
"Condition": {
    "StringEquals": {
        "ec2:InstanceType": ["t3.micro", "t3.small"]
    }
}
```

## 다중 값 조건

### ForAllValues

배열의 모든 값이 조건을 만족해야 합니다.

```json
// 모든 요청 태그가 허용된 키여야 함
"Condition": {
    "ForAllValues:StringEquals": {
        "aws:TagKeys": ["Environment", "CostCenter", "Owner"]
    }
}
```

### ForAnyValue

배열의 하나 이상의 값이 조건을 만족하면 됩니다.

```json
// 주체가 특정 그룹 중 하나에 속하면 허용
"Condition": {
    "ForAnyValue:StringEquals": {
        "aws:PrincipalTag/Department": ["Engineering", "Operations"]
    }
}
```

## 조건 조합 예시

### 업무 시간 및 IP 제한

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "*",
            "Resource": "*",
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": "10.0.0.0/8"
                },
                "DateGreaterThan": {
                    "aws:CurrentTime": "2024-01-01T09:00:00Z"
                },
                "DateLessThan": {
                    "aws:CurrentTime": "2024-01-01T18:00:00Z"
                }
            }
        }
    ]
}
```

### 태그 기반 접근 제어 (ABAC)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:StartInstances",
                "ec2:StopInstances"
            ],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:ResourceTag/Team": "${aws:PrincipalTag/Team}"
                }
            }
        }
    ]
}
```

## 관련 문서

- [정책 구조](./policy-structure.md)
- [정책 평가 로직](./evaluation-logic.md)
- [보안 모범 사례](../04_security/best-practices.md)
