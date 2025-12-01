# AWS ALB 사용자 인증

## 개요

Application Load Balancer는 HTTPS 리스너에서 사용자를 인증한 후 요청을 백엔드로 전달할 수 있습니다. 이를 통해 애플리케이션에서 인증 로직을 분리할 수 있습니다.

## 지원되는 인증 방식

### 1. Amazon Cognito
```yaml
특징:
  - 소셜 IdP 통합 (Amazon, Facebook, Google, Apple)
  - SAML 2.0 IdP 통합
  - 사용자 풀 기반 인증
  - 완전 관리형 서비스
```

### 2. OIDC (OpenID Connect)
```yaml
특징:
  - 모든 OIDC 호환 IdP 지원
  - Auth0, Okta, Keycloak 등
  - 기업용 IdP 통합
  - 유연한 구성
```

## Amazon Cognito 설정

### 사전 요구사항
1. **User Pool 생성**
2. **App Client 생성** (코드 그랜트 플로우 사용)
3. **User Pool Domain 설정**

### 리스너 규칙 구성
```yaml
Actions:
  - Type: authenticate-cognito
    AuthenticateCognitoConfig:
      UserPoolArn: arn:aws:cognito-idp:us-east-1:123456789:userpool/us-east-1_xxxxx
      UserPoolClientId: <client-id>
      UserPoolDomain: <domain-prefix>.auth.us-east-1.amazoncognito.com
      OnUnauthenticatedRequest: authenticate
  - Type: forward
    TargetGroupArn: <tg-arn>
```

### 콜백 URL 설정
Cognito App Client에 다음 콜백 URL을 등록:
```
https://<ALB-DNS-name>/oauth2/idpresponse
```
또는 커스텀 도메인 사용:
```
https://<custom-domain>/oauth2/idpresponse
```

## OIDC 설정

### 사전 요구사항
1. IdP에서 **OIDC 애플리케이션** 생성
2. **Client ID** 및 **Client Secret** 획득
3. 필수 엔드포인트 확인:
   - Authorization endpoint
   - Token endpoint
   - User info endpoint

### 리스너 규칙 구성
```yaml
Actions:
  - Type: authenticate-oidc
    AuthenticateOidcConfig:
      Issuer: https://idp.example.com
      AuthorizationEndpoint: https://idp.example.com/authorize
      TokenEndpoint: https://idp.example.com/token
      UserInfoEndpoint: https://idp.example.com/userinfo
      ClientId: <client-id>
      ClientSecret: <client-secret>
      OnUnauthenticatedRequest: authenticate
  - Type: forward
    TargetGroupArn: <tg-arn>
```

### 콜백 URL 설정
IdP에 다음 리다이렉트 URI를 등록:
```
https://<ALB-DNS-name>/oauth2/idpresponse
```

**중요:** URL은 **소문자**로 작성해야 합니다.

## 인증 흐름

```
1. 클라이언트 → ALB: HTTPS 요청
2. ALB: 세션 쿠키 확인
3. 쿠키 없음 → IdP 인증 엔드포인트로 리다이렉트
4. 사용자: IdP에서 로그인
5. IdP → ALB: 인증 코드와 함께 /oauth2/idpresponse로 리다이렉트
6. ALB → IdP: 인증 코드로 토큰 교환
7. ALB: AWSELB 세션 쿠키 설정
8. ALB → 클라이언트: 원본 URI로 리다이렉트
9. ALB → 타겟: 요청 전달 (사용자 클레임 헤더 포함)
```

## 사용자 클레임 헤더

인증 후 ALB가 타겟에 전달하는 헤더:

| 헤더 | 설명 |
|------|------|
| `x-amzn-oidc-accesstoken` | IdP의 액세스 토큰 |
| `x-amzn-oidc-identity` | 사용자 subject (sub) 필드 |
| `x-amzn-oidc-data` | JWT 형식의 사용자 클레임 |

### 클레임 검증
```yaml
JWT 헤더:
  alg: 서명 알고리즘 (ES256)
  kid: 키 ID

공개 키 조회:
  URL: https://public-keys.auth.elb.<region>.amazonaws.com/<kid>
```

**중요:** 애플리케이션에서 반드시 JWT 서명을 검증해야 합니다.

## 타임아웃 설정

| 설정 | 기본값 | 범위 |
|------|-------|------|
| 세션 타임아웃 | 7일 | 1초 ~ 7일 |
| 클라이언트 로그인 타임아웃 | 15분 | 변경 불가 |
| IdP 토큰 만료 | IdP 설정 | - |

### 세션 갱신
```yaml
액세스 토큰 만료 시:
  1. 리프레시 토큰이 있으면 자동 갱신
  2. 리프레시 토큰이 없거나 만료되면 재인증
  3. 재인증 후 새 세션 쿠키 발급
```

## 미인증 요청 처리

| 옵션 | 동작 |
|------|------|
| `authenticate` | IdP로 리다이렉트하여 인증 (기본값) |
| `allow` | 인증 없이 요청 허용 |
| `deny` | 401 Unauthorized 응답 |

### 사용 예시
```yaml
# API 엔드포인트: 인증 필수
- path-pattern: /api/*
  OnUnauthenticatedRequest: authenticate

# 공개 콘텐츠: 인증 선택
- path-pattern: /public/*
  OnUnauthenticatedRequest: allow
```

## 로그아웃 처리

ALB는 로그아웃 기능을 직접 제공하지 않습니다. 구현 방법:

### 1. 세션 쿠키 삭제
```javascript
// 클라이언트 측
document.cookie = "AWSELBAuthSessionCookie-0=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
```

### 2. IdP 로그아웃 호출
```javascript
// IdP의 로그아웃 엔드포인트로 리다이렉트
window.location.href = 'https://idp.example.com/logout?redirect_uri=https://app.example.com';
```

## 보안 고려사항

### 1. HTTPS 타겟 그룹 사용
민감한 클레임 정보가 평문으로 전송되지 않도록 HTTPS 사용 권장

### 2. 보안 그룹 제한
ALB의 보안 그룹에서만 타겟으로 트래픽 허용

### 3. JWT 서명 검증
애플리케이션에서 `x-amzn-oidc-data` 헤더의 JWT 서명을 반드시 검증

### 4. 클레임 스푸핑 방지
```yaml
로드 밸런서 보안 그룹 외부에서 오는 요청의
x-amzn-oidc-* 헤더는 무시해야 함
```

## 제한사항

| 항목 | 제한 |
|------|------|
| 인증 액션 위치 | forward 액션 앞에 위치해야 함 |
| 프로토콜 | HTTPS 리스너에서만 사용 가능 |
| 세션 쿠키 크기 | 4KB 이하 |
| 지원 IdP | OIDC 호환 IdP만 지원 |

## 참고 자료

- [AWS 공식 문서 - 사용자 인증](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html)
