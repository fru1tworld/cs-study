# AWS ALB HTTPS 리스너 및 SSL/TLS

## HTTPS 리스너 개요

HTTPS 리스너를 사용하면 클라이언트와 로드 밸런서 간의 통신이 암호화됩니다. SSL/TLS 종료(termination)가 로드 밸런서에서 수행되어 백엔드 애플리케이션의 부담을 줄입니다.

## 필수 요구사항

### SSL 서버 인증서
- 최소 **1개의 SSL 인증서** 필요
- 인증서 소스:
  - AWS Certificate Manager (ACM) - **권장**
  - AWS Identity and Access Management (IAM)

### 지원되지 않는 인증서
- ED25519 키를 사용하는 인증서는 **지원하지 않음**
- RSA 또는 ECDSA 키만 지원

## 인증서 관리

### 기본 인증서
- 리스너당 **하나의 기본 인증서** 필요
- SNI를 지원하지 않는 클라이언트에 사용됨

### 인증서 목록 (SNI)
```yaml
SNI (Server Name Indication):
  설명: 하나의 리스너에서 여러 도메인 지원
  동작: 클라이언트가 제공한 호스트명에 맞는 인증서 선택
  제한: ALB당 최대 25개 추가 인증서
```

### 인증서 추가
```bash
# 인증서 목록에 추가
aws elbv2 add-listener-certificates \
    --listener-arn <listener-arn> \
    --certificates CertificateArn=<cert-arn>
```

### 기본 인증서 변경
```bash
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --certificates CertificateArn=<new-default-cert-arn>
```

### 인증서 제거
```bash
aws elbv2 remove-listener-certificates \
    --listener-arn <listener-arn> \
    --certificates CertificateArn=<cert-arn>
```

**주의:** 기본 인증서는 제거할 수 없습니다.

## 보안 정책 (Security Policy)

### 보안 정책이란?
SSL/TLS 협상 시 사용할 **프로토콜과 암호화 제품군(cipher suite)**의 조합입니다.

### 권장 정책
| 정책 | 설명 |
|------|------|
| `ELBSecurityPolicy-TLS13-1-2-2021-06` | TLS 1.3 + TLS 1.2 지원 (권장) |
| `ELBSecurityPolicy-TLS13-1-3-2021-06` | TLS 1.3 전용 |
| `ELBSecurityPolicy-FS-1-2-Res-2020-10` | Forward Secrecy + TLS 1.2 |

### FIPS 정책
```yaml
FIPS 정책:
  - ELBSecurityPolicy-TLS13-1-2-FIPS-2023-04
  - ELBSecurityPolicy-TLS13-1-3-FIPS-2023-04
호환성: FIPS 정책끼리만 호환, 비-FIPS 정책과 호환 불가
```

### 보안 정책 변경
```bash
aws elbv2 modify-listener \
    --listener-arn <listener-arn> \
    --ssl-policy ELBSecurityPolicy-TLS13-1-2-2021-06
```

## HTTPS 리스너 생성

### AWS 콘솔
1. **Protocol**: HTTPS 선택
2. **Port**: 443 (또는 원하는 포트)
3. **Security policy**: 정책 선택
4. **Default SSL/TLS certificate**: 인증서 선택
5. **Default action**: 타겟 그룹 지정

### AWS CLI
```bash
aws elbv2 create-listener \
    --load-balancer-arn <lb-arn> \
    --protocol HTTPS \
    --port 443 \
    --ssl-policy ELBSecurityPolicy-TLS13-1-2-2021-06 \
    --certificates CertificateArn=<cert-arn> \
    --default-actions Type=forward,TargetGroupArn=<tg-arn>
```

## HTTP → HTTPS 리다이렉트

### 권장 구성
HTTP(80) 리스너에서 HTTPS(443)로 자동 리다이렉트:

```bash
# HTTP 리스너 생성 (리다이렉트 전용)
aws elbv2 create-listener \
    --load-balancer-arn <lb-arn> \
    --protocol HTTP \
    --port 80 \
    --default-actions \
        Type=redirect,\
        RedirectConfig='{
            "Protocol":"HTTPS",
            "Port":"443",
            "StatusCode":"HTTP_301"
        }'
```

## 백엔드 연결 암호화

### 옵션 1: HTTP (기본)
```yaml
ALB → 타겟: HTTP (암호화되지 않음)
사용 사례: VPC 내부 통신, 성능 우선
```

### 옵션 2: HTTPS
```yaml
ALB → 타겟: HTTPS (종단 간 암호화)
사용 사례: 규정 준수 요구사항, 민감한 데이터
설정: 타겟 그룹 프로토콜을 HTTPS로 설정
```

## TLS 정보 헤더

ALB가 타겟에 전달하는 TLS 정보 헤더:

| 헤더 | 설명 |
|------|------|
| `X-Amzn-TLS-Version` | TLS 버전 (예: TLSv1.3) |
| `X-Amzn-TLS-Cipher-Suite` | 사용된 암호화 제품군 |

### 활성화 방법
```bash
aws elbv2 modify-listener-attributes \
    --listener-arn <listener-arn> \
    --attributes Key=routing.http.xff_header_processing.mode,Value=append
```

## 인증서 자동 갱신

### ACM 인증서
- AWS에서 **자동으로 갱신**
- 갱신된 인증서는 자동으로 ALB에 배포
- 다운타임 없음

### IAM/외부 인증서
- **수동 갱신** 필요
- 만료 전 새 인증서 업로드 필수

## 인증서 선택 로직

```
1. 클라이언트가 SNI로 호스트명 제공
2. ALB가 인증서 목록에서 매칭되는 인증서 검색
3. 매칭되는 인증서가 없으면 기본 인증서 사용
4. SNI를 지원하지 않는 클라이언트는 기본 인증서 사용
```

## 제한사항

| 항목 | 제한 |
|------|------|
| 리스너당 기본 인증서 | 1개 |
| ALB당 추가 인증서 | 25개 (조정 가능) |
| 인증서 키 크기 | RSA 2048-8192 비트 |
| ECDSA 곡선 | secp256r1, secp384r1, secp521r1 |

## 참고 자료

- [AWS 공식 문서 - HTTPS 리스너](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html)
- [AWS 공식 문서 - 보안 정책](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html#describe-ssl-policies)
