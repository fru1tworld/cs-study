# EC2 인스턴스 메타데이터

## 개요

인스턴스 메타데이터는 실행 중인 EC2 인스턴스에서 접근할 수 있는 인스턴스에 대한 데이터입니다. 인스턴스를 구성하거나 관리하는 데 사용됩니다.

## 인스턴스 메타데이터 서비스 (IMDS)

### 엔드포인트

| 프로토콜 | 엔드포인트 | 비고 |
|----------|------------|------|
| IPv4 | `http://169.254.169.254` | 기본 활성화 |
| IPv6 | `http://[fd00:ec2::254]` | Nitro 인스턴스, 명시적 활성화 필요 |

### IMDSv1 vs IMDSv2

| 특성 | IMDSv1 | IMDSv2 |
|------|--------|--------|
| 인증 | 없음 | 세션 토큰 필수 |
| 보안 | 기본 | 강화됨 |
| SSRF 방어 | 취약 | 보호됨 |
| 기본값 (2024~) | 아니오 | 예 |

## IMDSv2 사용법

### 토큰 발급

```bash
# 세션 토큰 발급 (유효 시간: 21600초 = 6시간)
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
```

### 메타데이터 조회

```bash
# 토큰을 사용하여 메타데이터 조회
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/
```

## 메타데이터 카테고리

### 인스턴스 정보

```bash
# 인스턴스 ID
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/instance-id

# 인스턴스 타입
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/instance-type

# AMI ID
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/ami-id

# 가용 영역
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/placement/availability-zone

# 리전
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/placement/region
```

### 네트워크 정보

```bash
# 퍼블릭 IP
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/public-ipv4

# 프라이빗 IP
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/local-ipv4

# 퍼블릭 호스트명
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/public-hostname

# MAC 주소
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/mac

# 보안 그룹
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/security-groups
```

### IAM 역할 자격 증명

```bash
# 연결된 IAM 역할 이름
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/

# IAM 역할 임시 자격 증명
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/[role-name]
```

응답 예시:
```json
{
  "Code" : "Success",
  "LastUpdated" : "2024-01-15T12:00:00Z",
  "Type" : "AWS-HMAC",
  "AccessKeyId" : "ASIAXXX...",
  "SecretAccessKey" : "xxx...",
  "Token" : "xxx...",
  "Expiration" : "2024-01-15T18:00:00Z"
}
```

### 인스턴스 태그

```bash
# 태그 목록
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/tags/instance/

# 특정 태그 값
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/tags/instance/Name
```

> 태그 메타데이터는 인스턴스에서 명시적으로 활성화해야 합니다.

## 사용자 데이터 (User Data)

### 사용자 데이터란?

인스턴스 시작 시 스크립트나 구성 데이터를 전달하는 방법입니다.

### 사용자 데이터 조회

```bash
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/user-data
```

### 사용자 데이터 예시

#### 쉘 스크립트

```bash
#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "Hello from $(hostname)" > /var/www/html/index.html
```

#### cloud-init 설정

```yaml
#cloud-config
packages:
  - httpd
  - php
runcmd:
  - systemctl start httpd
  - systemctl enable httpd
```

### 사용자 데이터 제한

- 최대 크기: 16KB (base64 인코딩 전)
- 부팅 시 한 번만 실행 (기본값)
- mime 멀티파트를 사용하여 여러 스크립트 조합 가능

## IMDS 설정

### IMDSv2 필수 설정

```bash
# 기존 인스턴스에 IMDSv2 필수 적용
aws ec2 modify-instance-metadata-options \
    --instance-id i-xxxxxxxxx \
    --http-tokens required \
    --http-endpoint enabled
```

### 인스턴스 시작 시 설정

```bash
aws ec2 run-instances \
    --image-id ami-xxxxxxxxx \
    --instance-type t3.micro \
    --metadata-options "HttpTokens=required,HttpEndpoint=enabled"
```

### 응답 홉 제한

```bash
# 메타데이터 응답 홉 제한 설정
aws ec2 modify-instance-metadata-options \
    --instance-id i-xxxxxxxxx \
    --http-put-response-hop-limit 2
```

## 동적 데이터

인스턴스의 동적 정보를 조회합니다.

```bash
# 인스턴스 신원 문서
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/dynamic/instance-identity/document

# 서명된 인스턴스 신원 문서
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/dynamic/instance-identity/signature
```

## 보안 모범 사례

### 1. IMDSv2 사용

```bash
# 계정 수준에서 IMDSv2 기본값 설정
aws ec2 modify-instance-metadata-defaults \
    --region us-east-1 \
    --http-tokens required
```

### 2. 메타데이터 접근 제한

```bash
# 메타데이터 서비스 비활성화
aws ec2 modify-instance-metadata-options \
    --instance-id i-xxxxxxxxx \
    --http-endpoint disabled
```

### 3. 최소 권한 IAM 역할

- 인스턴스에 필요한 최소한의 권한만 부여
- 정기적인 역할 권한 검토

### 4. 네트워크 수준 보호

- iptables나 Windows 방화벽으로 169.254.169.254 접근 제한
- 컨테이너 환경에서 메타데이터 프록시 사용

## 문제 해결

### HTTP 401 오류

토큰이 유효하지 않음. 새 토큰 발급 필요.

```bash
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
```

### HTTP 403 오류

- IMDS가 비활성화됨
- IMDSv2 필수인데 IMDSv1으로 접근 시도
- 네트워크 설정 문제

### 연결 시간 초과

```bash
# 라우팅 확인
ip route show | grep 169.254.169.254
```

## 참고 자료

- [인스턴스 메타데이터](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html)
- [IMDSv2 사용](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html)
- [사용자 데이터](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html)
