# AWS ALB 상호 인증 (mTLS)

## 개요

상호 TLS 인증(Mutual TLS, mTLS)은 클라이언트와 서버가 ** 서로의 신원을 검증** 하는 양방향 인증 방식입니다. 일반 TLS에서는 클라이언트만 서버를 검증하지만, mTLS에서는 서버도 클라이언트의 인증서를 검증합니다.

## 사용 사례

```yaml
Zero Trust 아키텍처:
  - 모든 클라이언트 신원 검증
  - 네트워크 위치에 의존하지 않는 보안

B2B 통신:
  - 파트너 시스템 간 안전한 통신
  - API 클라이언트 인증

IoT 디바이스:
  - 디바이스 인증서 기반 인증
  - 장치 신뢰성 검증

마이크로서비스:
  - 서비스 간 상호 인증
  - 서비스 메시 보안
```

## 운영 모드

### 1. Passthrough 모드

```yaml
동작:
  - ALB가 클라이언트 인증서를 검증하지 않음
  - 인증서 체인 전체를 백엔드로 전달
  - 백엔드 애플리케이션에서 검증 수행

사용 사례:
  - 기존 인증서 검증 로직이 있는 경우
  - 커스텀 검증 정책 필요 시
  - 점진적 mTLS 도입

전달 헤더:
  X-Amzn-Mtls-Clientcert: 전체 인증서 체인 (URL 인코딩)
```

### 2. Verify 모드

```yaml
동작:
  - ALB가 클라이언트 인증서를 직접 검증
  - 신뢰할 수 있는 CA 목록과 대조
  - 선택적으로 인증서 폐기 상태 확인

사용 사례:
  - 중앙화된 인증서 검증
  - 애플리케이션 부담 감소
  - 표준화된 검증 정책

전달 헤더:
  - X-Amzn-Mtls-Clientcert-Serial-Number
  - X-Amzn-Mtls-Clientcert-Issuer
  - X-Amzn-Mtls-Clientcert-Subject
  - X-Amzn-Mtls-Clientcert-Validity
  - X-Amzn-Mtls-Clientcert-Leaf
```

## Trust Store (신뢰 저장소)

### Trust Store란?
클라이언트 인증서를 검증할 때 사용하는 ** 신뢰할 수 있는 CA 인증서** 모음입니다.

### 요구사항

```yaml
형식: PEM
내용: CA 인증서 번들
제약:
  - 개별 인증서가 아닌 번들 파일로 업로드
  - 공백 줄 없어야 함
  - 계정당 최대 20개 Trust Store
  - Trust Store당 최대 25개 CA 인증서
```

### Trust Store 생성

**AWS CLI:**
```bash
# S3에 CA 번들 업로드
aws s3 cp ca-bundle.pem s3://my-bucket/ca-bundle.pem

# Trust Store 생성
aws elbv2 create-trust-store \
    --name my-trust-store \
    --ca-certificates-bundle-s3-bucket my-bucket \
    --ca-certificates-bundle-s3-key ca-bundle.pem
```

### CA 번들 형식 예시

```pem
-----BEGIN CERTIFICATE-----
MIIDxTCCAq2gAwIBAgIQAqxcJmoLQJuPC3nyrkYldzANBgkqhkiG9w0BAQsFADBs
... (CA 1 내용)
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIDxTCCAq2gAwIBAgIQAqxcJmoLQJuPC3nyrkYldzANBgkqhkiG9w0BAQsFADBs
... (CA 2 내용)
-----END CERTIFICATE-----
```

## 인증서 요구사항

### 지원되는 클라이언트 인증서

| 항목 | 요구사항 |
|------|---------|
| 형식 | X.509v3 |
| 공개키 | RSA 2K-8K 비트 또는 ECDSA |
| ECDSA 곡선 | secp256r1, secp384r1, secp521r1 |
| 서명 알고리즘 | SHA256, SHA384, SHA512 |

### 지원되지 않는 항목
- ED25519 키
- SHA1 서명

## mTLS 설정

### HTTPS 리스너에 mTLS 구성

**AWS 콘솔:**
1. 리스너 선택 → **Edit**
2. **Client certificate handling** 섹션
3. **Mutual authentication (mTLS)** 활성화
4. 모드 선택: Passthrough 또는 Verify
5. Verify 모드: Trust Store 선택
6. 저장

**AWS CLI:**
```bash
# Verify 모드로 리스너 수정
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --mutual-authentication \
        Mode=verify,\
        TrustStoreArn=<trust-store-arn>,\
        IgnoreClientCertificateExpiry=false
```

### Passthrough 모드 설정
```bash
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --mutual-authentication Mode=passthrough
```

## 전달되는 HTTP 헤더

### Passthrough 모드

| 헤더 | 설명 |
|------|------|
| `X-Amzn-Mtls-Clientcert` | URL 인코딩된 전체 인증서 체인 |

### Verify 모드

| 헤더 | 예시 |
|------|------|
| `X-Amzn-Mtls-Clientcert-Serial-Number` | 16진수 시리얼 번호 |
| `X-Amzn-Mtls-Clientcert-Issuer` | CN=My CA,O=My Org (RFC2253) |
| `X-Amzn-Mtls-Clientcert-Subject` | CN=Client,O=Client Org |
| `X-Amzn-Mtls-Clientcert-Validity` | NotBefore=2023-01-01T00:00:00Z;NotAfter=2024-01-01T00:00:00Z |
| `X-Amzn-Mtls-Clientcert-Leaf` | URL 인코딩된 리프 인증서 |

## 인증서 폐기 확인

### CRL (Certificate Revocation List)

```yaml
설명: 폐기된 인증서 목록
위치: Trust Store에 CRL 추가
업데이트: S3에서 주기적으로 가져옴
```

**CRL 추가:**
```bash
aws elbv2 add-trust-store-revocations \
    --trust-store-arn <trust-store-arn> \
    --revocation-contents S3Bucket=my-bucket,S3Key=revocation-list.crl
```

### OCSP (Online Certificate Status Protocol)

현재 ALB에서는 OCSP를 직접 지원하지 않습니다. CRL을 사용하세요.

## CA 주체명 광고

TLS 핸드셰이크 중 클라이언트에게 ** 신뢰하는 CA 목록** 을 알려줍니다.

```yaml
장점:
  - 클라이언트가 올바른 인증서 선택
  - 브라우저 인증서 선택 다이얼로그 최적화
  - 잘못된 인증서 제출 방지
```

** 활성화:**
```bash
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --mutual-authentication \
        Mode=verify,\
        TrustStoreArn=<trust-store-arn>,\
        AdvertiseTrustStoreCaNames=on
```

## 연결 로그

mTLS 연결에 대한 상세 로그를 기록합니다.

### 로그 내용
- 클라이언트 IP 및 포트
- TLS 버전 및 암호화 제품군
- 인증서 주체 및 발급자
- 인증서 시리얼 번호
- 인증 결과

### 활성화
```bash
aws elbv2 modify-load-balancer-attributes \
    --load-balancer-arn <lb-arn> \
    --attributes \
        Key=connection_logs.s3.enabled,Value=true \
        Key=connection_logs.s3.bucket,Value=my-log-bucket \
        Key=connection_logs.s3.prefix,Value=conn-logs
```

## 에러 처리

### 일반 mTLS 에러

| 에러 | 원인 | 해결 |
|------|------|------|
| 인증서 없음 | 클라이언트가 인증서 미제출 | 클라이언트 설정 확인 |
| 신뢰되지 않는 CA | CA가 Trust Store에 없음 | Trust Store 업데이트 |
| 인증서 만료 | 클라이언트 인증서 만료 | 인증서 갱신 |
| 인증서 폐기됨 | CRL에 등록된 인증서 | 새 인증서 발급 |

### 만료된 인증서 허용

테스트 환경에서 만료된 인증서를 허용하려면:

```bash
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --mutual-authentication \
        Mode=verify,\
        TrustStoreArn=<trust-store-arn>,\
        IgnoreClientCertificateExpiry=true
```

** 경고:** 프로덕션 환경에서는 사용하지 마세요.

## 제한사항

| 항목 | 제한 |
|------|------|
| ALB당 mTLS 리스너 | 2개 |
| 계정당 Trust Store | 20개 |
| Trust Store당 CA 인증서 | 25개 |
| 클라이언트 인증서 체인 깊이 | 4단계 |

## 참고 자료

- [AWS 공식 문서 - 상호 인증](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/mutual-authentication.html)
