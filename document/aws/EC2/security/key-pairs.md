# EC2 키 페어

## 개요

키 페어는 퍼블릭 키와 프라이빗 키로 구성된 보안 자격 증명입니다. EC2 인스턴스에 연결할 때 신원을 증명하는 데 사용됩니다.

## 작동 원리

```
┌─────────────────────────────────────────────────────────────┐
│                       키 페어 작동 방식                       │
│                                                              │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │    사용자 PC      │         │   EC2 인스턴스    │         │
│  │                  │         │                  │         │
│  │  ┌────────────┐  │         │  ┌────────────┐  │         │
│  │  │프라이빗 키  │  │         │  │ 퍼블릭 키   │  │         │
│  │  │(my-key.pem)│  │         │  │(authorized │  │         │
│  │  │            │  │────────▶│  │   _keys)   │  │         │
│  │  └────────────┘  │  인증   │  └────────────┘  │         │
│  │                  │         │                  │         │
│  └──────────────────┘         └──────────────────┘         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 키 페어 유형

| 유형 | 설명 | 지원 |
|------|------|------|
| **RSA** | 전통적인 암호화 알고리즘 | 모든 리전 |
| **ED25519** | 현대적인 타원 곡선 알고리즘 | 대부분 리전 |

### RSA vs ED25519

| 특성 | RSA | ED25519 |
|------|-----|---------|
| 키 크기 | 2048/4096 비트 | 256 비트 |
| 보안 수준 | 높음 | 매우 높음 |
| 성능 | 느림 | 빠름 |
| 호환성 | 더 넓음 | 최신 클라이언트 필요 |

## 키 페어 관리

### AWS에서 키 페어 생성

```bash
# RSA 키 페어 생성
aws ec2 create-key-pair \
    --key-name my-key-pair \
    --key-type rsa \
    --query 'KeyMaterial' \
    --output text > my-key-pair.pem

# ED25519 키 페어 생성
aws ec2 create-key-pair \
    --key-name my-ed25519-key \
    --key-type ed25519 \
    --query 'KeyMaterial' \
    --output text > my-ed25519-key.pem

# 키 파일 권한 설정 (필수)
chmod 400 my-key-pair.pem
```

### 기존 키 가져오기

```bash
# 로컬에서 키 생성
ssh-keygen -t rsa -b 4096 -f my-key-pair

# AWS에 퍼블릭 키 가져오기
aws ec2 import-key-pair \
    --key-name my-imported-key \
    --public-key-material fileb://my-key-pair.pub
```

### 키 페어 조회

```bash
# 모든 키 페어 목록
aws ec2 describe-key-pairs

# 특정 키 페어 정보
aws ec2 describe-key-pairs --key-names my-key-pair
```

### 키 페어 삭제

```bash
aws ec2 delete-key-pair --key-name my-key-pair
```

## 인스턴스에서 키 페어 사용

### 인스턴스 시작 시 지정

```bash
aws ec2 run-instances \
    --image-id ami-xxxxxxxxx \
    --instance-type t3.micro \
    --key-name my-key-pair \
    --subnet-id subnet-xxxxxxxxx
```

### SSH로 연결 (Linux)

```bash
# 기본 연결
ssh -i my-key-pair.pem ec2-user@public-ip

# Amazon Linux 2/2023
ssh -i my-key-pair.pem ec2-user@public-ip

# Ubuntu
ssh -i my-key-pair.pem ubuntu@public-ip

# RHEL
ssh -i my-key-pair.pem ec2-user@public-ip

# Debian
ssh -i my-key-pair.pem admin@public-ip
```

### SSH 설정 파일 사용

`~/.ssh/config` 파일:

```
Host my-ec2
    HostName 52.xx.xx.xx
    User ec2-user
    IdentityFile ~/.ssh/my-key-pair.pem
```

사용:
```bash
ssh my-ec2
```

## 키 페어 교체

### 새 키 추가 (Linux)

```bash
# 1. 기존 키로 인스턴스에 연결
ssh -i old-key.pem ec2-user@public-ip

# 2. 새 퍼블릭 키를 authorized_keys에 추가
echo "새-퍼블릭-키-내용" >> ~/.ssh/authorized_keys

# 3. 새 키로 연결 테스트 (새 터미널에서)
ssh -i new-key.pem ec2-user@public-ip

# 4. 기존 키 제거 (선택사항)
# authorized_keys에서 해당 줄 제거
```

### User Data를 통한 키 추가

```bash
#!/bin/bash
echo "ssh-rsa AAAA..." >> /home/ec2-user/.ssh/authorized_keys
```

### Systems Manager를 통한 키 관리

키 페어 없이 Session Manager로 연결:

```bash
# SSM Session Manager로 연결
aws ssm start-session --target i-xxxxxxxxx
```

## 프라이빗 키 분실 시

AWS는 프라이빗 키 사본을 보관하지 않습니다. 분실 시 복구가 불가능합니다.

### 복구 방법

#### 방법 1: EBS 볼륨을 통한 복구

```bash
# 1. 인스턴스 중지
aws ec2 stop-instances --instance-ids i-xxxxxxxxx

# 2. 루트 볼륨 분리
aws ec2 detach-volume --volume-id vol-xxxxxxxxx

# 3. 볼륨을 복구 인스턴스에 연결
aws ec2 attach-volume \
    --volume-id vol-xxxxxxxxx \
    --instance-id i-recovery \
    --device /dev/xvdf

# 4. 복구 인스턴스에서 마운트하고 새 키 추가
mount /dev/xvdf1 /mnt
echo "새-퍼블릭-키" >> /mnt/home/ec2-user/.ssh/authorized_keys
umount /mnt

# 5. 볼륨을 원래 인스턴스에 다시 연결
```

#### 방법 2: EC2 Instance Connect

```bash
# 임시 SSH 키로 연결
aws ec2-instance-connect send-ssh-public-key \
    --instance-id i-xxxxxxxxx \
    --availability-zone ap-northeast-2a \
    --instance-os-user ec2-user \
    --ssh-public-key file://my-new-key.pub
```

#### 방법 3: Systems Manager Run Command

```bash
aws ssm send-command \
    --instance-ids i-xxxxxxxxx \
    --document-name AWS-RunShellScript \
    --parameters 'commands=["echo \"새-퍼블릭-키\" >> /home/ec2-user/.ssh/authorized_keys"]'
```

## Windows 인스턴스

Windows 인스턴스에서 키 페어는 관리자 비밀번호를 복호화하는 데 사용됩니다.

### 관리자 비밀번호 가져오기

```bash
aws ec2 get-password-data \
    --instance-id i-xxxxxxxxx \
    --priv-launch-key my-key-pair.pem
```

### RDP 연결

1. 복호화된 비밀번호로 RDP 클라이언트 연결
2. 사용자 이름: `Administrator`

## 모범 사례

### 1. 키 관리

```
┌─────────────────────────────────────────────────────────────┐
│                    키 관리 모범 사례                         │
├─────────────────────────────────────────────────────────────┤
│  1. 환경별 키 페어 분리 (dev, staging, prod)                 │
│  2. 정기적인 키 교체                                        │
│  3. 프라이빗 키를 안전하게 보관 (AWS Secrets Manager)        │
│  4. 사용하지 않는 키 삭제                                    │
│  5. 키 파일 권한 제한 (chmod 400)                           │
└─────────────────────────────────────────────────────────────┘
```

### 2. 대안 고려

| 방법 | 장점 | 단점 |
|------|------|------|
| **EC2 Instance Connect** | 키 관리 불필요 | 일부 AMI만 지원 |
| **Session Manager** | 키/포트 불필요 | IAM 권한 필요 |
| **EC2 Serial Console** | 네트워크 문제 해결 | 제한된 사용 사례 |

### 3. Secrets Manager 통합

```python
import boto3
import json

def get_key_from_secrets_manager(secret_name):
    client = boto3.client('secretsmanager')
    response = client.get_secret_value(SecretId=secret_name)
    return response['SecretString']
```

## 참고 자료

- [Amazon EC2 키 페어](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)
- [EC2 Instance Connect](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-connect-methods.html)
- [Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
