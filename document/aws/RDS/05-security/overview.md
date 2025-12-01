# Amazon RDS 보안 개요

## 보안 원칙

AWS에서 "클라우드 보안은 최우선 순위"입니다. Amazon RDS 보안은 공유 책임 모델을 따릅니다:

| 책임 영역 | AWS | 고객 |
|----------|-----|------|
| 물리적 인프라 | ✅ | |
| 네트워크 인프라 | ✅ | |
| 가상화 계층 | ✅ | |
| DB 엔진 패치 | ✅ | |
| 네트워크 접근 제어 | | ✅ |
| IAM 권한 관리 | | ✅ |
| 데이터 암호화 | | ✅ |
| DB 사용자 관리 | | ✅ |

## 보안 계층

### 1. 네트워크 보안

#### VPC (Virtual Private Cloud)
- DB 인스턴스를 VPC 내에 배포하여 네트워크 격리
- 프라이빗 서브넷 사용으로 인터넷 직접 접근 차단
- VPC 피어링 또는 Transit Gateway를 통한 제어된 연결

#### 보안 그룹
- 인스턴스 수준 방화벽
- 인바운드/아웃바운드 트래픽 제어
- IP 주소, CIDR 블록, 또는 다른 보안 그룹 참조로 규칙 정의

#### 서브넷 그룹
- DB 인스턴스가 위치할 서브넷 지정
- Multi-AZ 배포를 위해 여러 가용 영역의 서브넷 포함

### 2. 접근 제어

#### IAM (Identity and Access Management)
- RDS 리소스 관리 권한 제어
- API 호출 및 콘솔 접근 권한 관리
- 정책 기반 세분화된 권한 부여

#### 데이터베이스 인증
- **기본 인증**: 마스터 사용자 자격 증명
- **IAM 데이터베이스 인증**: IAM 사용자/역할로 DB 연결
- **Kerberos 인증**: Active Directory 통합

### 3. 데이터 암호화

#### 저장 데이터 암호화 (Encryption at Rest)
- AES-256 암호화 알고리즘 사용
- AWS KMS 관리형 키 또는 고객 관리형 키
- DB 인스턴스, 자동 백업, 스냅샷, 읽기 복제본 모두 암호화

#### 전송 데이터 암호화 (Encryption in Transit)
- SSL/TLS 연결 지원
- 모든 데이터베이스 엔진에서 사용 가능
- 인증서 기반 서버 신원 확인

### 4. 감사 및 모니터링

#### CloudTrail
- API 호출 로깅
- 관리 작업 추적
- 보안 감사 및 규정 준수

#### 데이터베이스 로그
- 오류 로그, 쿼리 로그, 감사 로그
- CloudWatch Logs로 내보내기 가능

## 보안 모범 사례

### 네트워크 구성
```
✅ 프라이빗 서브넷에 DB 인스턴스 배치
✅ 퍼블릭 액세스 비활성화
✅ 보안 그룹에서 필요한 포트만 허용
✅ 특정 IP 주소/CIDR만 접근 허용
✅ VPC 엔드포인트 사용 (AWS 서비스 연결 시)
```

### 접근 제어
```
✅ 마스터 사용자 사용 최소화
✅ 애플리케이션별 DB 사용자 생성
✅ 최소 권한 원칙 적용
✅ IAM 데이터베이스 인증 활용
✅ AWS Secrets Manager로 자격 증명 관리
```

### 암호화
```
✅ 저장 데이터 암호화 활성화
✅ SSL/TLS 연결 강제
✅ 고객 관리형 KMS 키 사용 (규정 준수 필요 시)
✅ 정기적인 키 순환
```

### 모니터링
```
✅ CloudTrail 로깅 활성화
✅ CloudWatch 경보 설정
✅ 비정상 접근 패턴 모니터링
✅ 정기적인 보안 감사
```

## 규정 준수

Amazon RDS는 다양한 규정 준수 프로그램을 지원합니다:

| 프로그램 | 지원 |
|---------|-----|
| SOC 1, 2, 3 | ✅ |
| PCI DSS | ✅ |
| HIPAA | ✅ (BAA 필요) |
| FedRAMP | ✅ (GovCloud) |
| ISO 27001 | ✅ |
| GDPR | ✅ |

### HIPAA 준수
- Business Associate Agreement (BAA) 체결 필요
- 지원 데이터베이스 엔진: 모든 RDS 엔진
- 암호화 필수

### FedRAMP
- AWS GovCloud 리전에서 FedRAMP HIGH 승인
- 정부 기관 워크로드 지원

## 관련 문서

- [VPC 구성](./vpc-configuration.md)
- [암호화](./encryption.md)
- [IAM 인증](./iam-authentication.md)
- [네트워크 접근 제어](./network-access.md)
