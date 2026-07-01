# RFC 6749 - The OAuth 2.0 Authorization Framework

> 발행일: 2012년 10월
> 상태: Standards Track

## 1. 개요

OAuth 2.0은 제3자 애플리케이션이 사용자 자격 증명을 직접 다루지 않고 리소스에 접근할 수 있게 해주는 인가(Authorization) 프레임워크 입니다.

### 핵심 개념

> "사용자의 비밀번호를 제3자에게 공유하지 않고, 제한된 접근 권한을 부여"

| 특징 | 설명 |
|------|------|
| 권한 위임 | 사용자가 앱에 자신의 리소스 접근 권한 부여 |
| 토큰 기반 | 비밀번호 대신 액세스 토큰 사용 |
| 범위 제한 | 필요한 권한만 선택적으로 부여 |
| 시간 제한 | 토큰 만료로 보안 강화 |

---

## 2. 역할 (Roles)

### 2.1 네 가지 역할

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  ┌──────────────┐          ┌──────────────────────┐             │
│  │   Resource   │          │  Authorization       │             │
│  │    Owner     │◄────────►│     Server           │             │
│  │   (사용자)    │          │  (인가 서버)          │             │
│  └──────────────┘          └──────────────────────┘             │
│         │                            │                           │
│         │ 권한 부여                   │ 토큰 발급                  │
│         ▼                            ▼                           │
│  ┌──────────────┐          ┌──────────────────────┐             │
│  │    Client    │─────────►│    Resource          │             │
│  │   (앱/서비스) │  토큰     │      Server          │             │
│  │              │  사용     │   (API 서버)         │             │
│  └──────────────┘          └──────────────────────┘             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

| 역할 | 설명 | 예시 |
|------|------|------|
| Resource Owner | 보호된 리소스의 소유자 | 사용자 (end-user) |
| Resource Server | 리소스를 호스팅하는 서버 | Google Drive API |
| Client | 리소스 접근을 요청하는 애플리케이션 | 3rd party 앱 |
| Authorization Server | 토큰을 발급하는 서버 | Google OAuth Server |

---

## 3. 프로토콜 흐름

### 3.1 추상적 프로토콜 흐름

```
     +--------+                               +---------------+
     |        |--(A)- Authorization Request ->|   Resource    |
     |        |                               |     Owner     |
     |        |<-(B)-- Authorization Grant ---|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(C)-- Authorization Grant -->| Authorization |
     | Client |                               |     Server    |
     |        |<-(D)----- Access Token -------|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(E)----- Access Token ------>|    Resource   |
     |        |                               |     Server    |
     |        |<-(F)--- Protected Resource ---|               |
     +--------+                               +---------------+
```

### 3.2 단계별 설명

| 단계 | 설명 |
|------|------|
| (A) | 클라이언트가 리소스 소유자에게 인가 요청 |
| (B) | 리소스 소유자가 인가 승인 (Authorization Grant) 발급 |
| (C) | 클라이언트가 인가 승인을 인가 서버에 제출 |
| (D) | 인가 서버가 액세스 토큰 발급 |
| (E) | 클라이언트가 액세스 토큰으로 리소스 요청 |
| (F) | 리소스 서버가 보호된 리소스 반환 |

---

## 4. Authorization Grant 유형

### 4.1 개요

| Grant Type | 용도 | 보안 수준 |
|------------|------|-----------|
| Authorization Code | 서버 사이드 앱 | 높음 |
| Implicit | SPA (더 이상 권장하지 않음) | 낮음 |
| Resource Owner Password | 신뢰할 수 있는 앱 | 중간 |
| Client Credentials | 서버 간 통신 | 높음 |

---

### 4.2 Authorization Code Grant

가장 보안성이 높고 권장되는 방식

```
+----------+
| Resource |
|   Owner  |
+----+-----+
     ^
     |
    (B)
+----|----+          Client Identifier      +---------------+
|         |>---(A)-- & Redirection URI ---->|               |
|  User-  |                                 | Authorization |
|  Agent  |<---(B)-- Authorization Code ----|     Server    |
|         |                                 |               |
+----|----+                                 +---------------+
     |                                            ^      v
    (B)                                           |      |
     |                                            |      |
     v                                            |      |
+---------+                                       |      |
|         |>---(C)-- Authorization Code ----------'      |
|  Client |          & Redirection URI                   |
|         |                                              |
|         |<---(D)-- Access Token ----------------------'
+---------+       (w/ Optional Refresh Token)
```

#### Step 1: 인가 요청

```http
GET /authorize?
  response_type=code
  &client_id=s6BhdRkqt3
  &redirect_uri=https://client.example.org/callback
  &scope=openid%20profile
  &state=xyz123
HTTP/1.1
Host: authorization-server.com
```

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| response_type | 필수 | `code` 고정 |
| client_id | 필수 | 클라이언트 식별자 |
| redirect_uri | 선택 | 콜백 URI |
| scope | 선택 | 요청 권한 범위 |
| state | 권장 | CSRF 방지용 랜덤 문자열 |

#### Step 2: 인가 응답 (콜백)

```http
HTTP/1.1 302 Found
Location: https://client.example.org/callback?
  code=SplxlOBeZQQYbYS6WxSbIA
  &state=xyz123
```

#### Step 3: 토큰 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https://client.example.org/callback
```

#### Step 4: 토큰 응답

```json
{
  "access_token": "SlAV32hkKG",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "8xLOxBtZp8",
  "scope": "openid profile"
}
```

#### Authorization Code 제약사항

Authorization Code는 보안을 위해 다음과 같은 제약사항을 가집니다:

| 제약사항 | 설명 | 권장값 |
|----------|------|--------|
| 일회용 (Single Use) | 코드는 한 번만 사용 가능. 재사용 시도 시 해당 코드로 발급된 모든 토큰 무효화 권장 | - |
| 짧은 만료 시간 | 발급 후 빠르게 만료되어야 함 | 10분 이내 (권장: 1-5분) |
| 클라이언트 바인딩 | 특정 client_id에만 유효 | - |
| Redirect URI 바인딩 | 토큰 요청 시 동일한 redirect_uri 필수 | - |

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Authorization Code 수명 주기                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  [발급] ─────────── 유효 기간 (1-10분) ─────────── [만료/사용]            │
│    │                                                    │                │
│    │                                                    │                │
│    └── 사용되면 → 즉시 무효화                            │                │
│    └── 재사용 시도 → 관련 토큰 모두 폐기 (보안 조치)       │                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

보안 권장사항:
- 인가 서버는 코드 사용 여부를 추적해야 함
- 코드 재사용 감지 시, 해당 코드로 발급된 액세스 토큰과 리프레시 토큰을 모두 폐기
- 코드는 암호학적으로 안전한 난수 생성기로 생성해야 함 (최소 128비트 엔트로피 권장)

---

### 4.3 Implicit Grant (더 이상 권장하지 않음)

> 주의: OAuth 2.1에서는 제거될 예정. PKCE를 사용한 Authorization Code Flow 권장.

```
+----------+
| Resource |
|   Owner  |
+----+-----+
     ^
     |
    (B)
+----|----+          Client Identifier      +---------------+
|         |>---(A)-- & Redirection URI ---->|               |
|  User-  |                                 | Authorization |
|  Agent  |<---(B)--- Access Token ---------|     Server    |
|         |          (in URI Fragment)      |               |
+---------+                                 +---------------+
```

요청:
```http
GET /authorize?
  response_type=token
  &client_id=s6BhdRkqt3
  &redirect_uri=https://client.example.org/callback
  &scope=openid
  &state=xyz
```

응답:
```
https://client.example.org/callback#
  access_token=2YotnFZFEjr1zCsicMWpAA
  &token_type=Bearer
  &expires_in=3600
  &state=xyz
```

---

### 4.4 Resource Owner Password Credentials Grant

신뢰할 수 있는 앱에서만 사용 (예: 같은 회사의 공식 앱)

```
+----------+
| Resource |
|  Owner   |
+----+-----+
     |
     |    Resource Owner
    (A) Password Credentials
     |
     v
+---------+                                  +---------------+
|         |>--(B)---- Resource Owner ------->|               |
|         |         Password Credentials     | Authorization |
| Client  |                                  |     Server    |
|         |<--(C)---- Access Token ---------|               |
|         |    (w/ Optional Refresh Token)  |               |
+---------+                                  +---------------+
```

요청:
```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

grant_type=password
&username=johndoe
&password=A3ddj3w
&scope=read write
&client_id=s6BhdRkqt3
&client_secret=7Fjfp0ZBr1KtDRbnfVdmIw
```

---

### 4.5 Client Credentials Grant

서버 간 통신 (사용자 개입 없음)

```
+---------+                                  +---------------+
|         |                                  |               |
|         |>--(A)- Client Authentication --->| Authorization |
| Client  |                                  |     Server    |
|         |<--(B)---- Access Token ---------|               |
|         |                                  |               |
+---------+                                  +---------------+
```

요청:
```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=client_credentials
&scope=api:read
```

---

## 5. 액세스 토큰

### 5.1 토큰 사용

```http
GET /api/user HTTP/1.1
Host: api.example.com
Authorization: Bearer mF_9.B5f-4.1JqM
```

### 5.2 토큰 유형

| 유형 | 설명 |
|------|------|
| Bearer Token | 토큰 소유자에게 권한 부여 (가장 일반적) |
| MAC Token | 메시지 인증 코드 포함 |

### 5.3 Bearer 토큰 전송 방법

```http
# 1. Authorization 헤더 (권장)
Authorization: Bearer mF_9.B5f-4.1JqM

# 2. Form-Encoded Body
POST /resource HTTP/1.1
Content-Type: application/x-www-form-urlencoded

access_token=mF_9.B5f-4.1JqM

# 3. URI Query Parameter (비권장)
GET /resource?access_token=mF_9.B5f-4.1JqM
```

---

## 6. 리프레시 토큰

### 6.1 개념

```
액세스 토큰: 짧은 수명 (분~시간)
리프레시 토큰: 긴 수명 (일~주~월)
```

### 6.2 리프레시 흐름

```
+--------+                                           +---------------+
|        |--(A)------- Authorization Grant --------->|               |
|        |                                           |               |
|        |<-(B)----------- Access Token -------------|               |
|        |               & Refresh Token             |               |
|        |                                           |               |
|        |                            +----------+   |               |
|        |--(C)---- Access Token ---->|          |   |               |
|        |                            |          |   |               |
|        |<-(D)- Protected Resource --|          |   | Authorization |
| Client |                            | Resource |   |     Server    |
|        |--(E)---- Access Token ---->|  Server  |   |               |
|        |                            |          |   |               |
|        |<-(F)- Invalid Token Error -|          |   |               |
|        |                            +----------+   |               |
|        |                                           |               |
|        |--(G)----------- Refresh Token ----------->|               |
|        |                                           |               |
|        |<-(H)----------- Access Token -------------|               |
+--------+           & Optional Refresh Token        +---------------+
```

### 6.3 리프레시 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=refresh_token
&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
&scope=read
```

---

## 7. 클라이언트 등록

### 7.1 클라이언트 유형

| 유형 | 설명 | 예시 |
|------|------|------|
| Confidential | 자격 증명을 안전하게 보관 | 서버 사이드 앱 |
| Public | 자격 증명을 보호할 수 없음 | SPA, 모바일 앱 |

### 7.2 등록 정보

```json
{
  "client_id": "s6BhdRkqt3",
  "client_secret": "7Fjfp0ZBr1KtDRbnfVdmIw",
  "client_name": "My Application",
  "redirect_uris": [
    "https://client.example.org/callback",
    "https://client.example.org/callback2"
  ],
  "grant_types": ["authorization_code", "refresh_token"],
  "scope": "openid profile email"
}
```

---

## 8. 보안 고려사항

### 8.1 CSRF 방지

```
state 파라미터 필수 사용:
1. 클라이언트가 랜덤 state 생성
2. 인가 요청에 포함
3. 콜백에서 state 검증
```

### 8.2 Authorization Code 인터셉션 방지

PKCE (Proof Key for Code Exchange) 사용:

```
1. 클라이언트가 code_verifier (랜덤 문자열) 생성
2. code_challenge = BASE64URL(SHA256(code_verifier))
3. 인가 요청에 code_challenge 포함
4. 토큰 요청에 code_verifier 포함
5. 서버가 검증
```

### 8.3 토큰 보안

| 권장사항 | 설명 |
|----------|------|
| HTTPS 필수 | 모든 통신에 TLS 사용 |
| 짧은 만료 시간 | 액세스 토큰은 짧게 |
| 범위 최소화 | 필요한 scope만 요청 |
| 토큰 저장 | 안전한 저장소 사용 |

### 8.4 Redirect URI 검증

```
- 정확히 일치하는 URI만 허용
- 와일드카드 사용 금지
- localhost는 개발 환경에서만
```

---

## 9. 에러 응답

### 9.1 인가 에러

```http
HTTP/1.1 302 Found
Location: https://client.example.org/callback?
  error=access_denied
  &error_description=The+resource+owner+denied+the+request
  &state=xyz
```

| 에러 코드 | 설명 |
|-----------|------|
| invalid_request | 요청이 잘못됨 |
| unauthorized_client | 클라이언트가 이 grant type을 사용할 수 없음 |
| access_denied | 리소스 소유자가 거부 |
| unsupported_response_type | 지원하지 않는 response_type |
| invalid_scope | 요청한 scope가 유효하지 않음 |
| server_error | 서버 내부 오류 |
| temporarily_unavailable | 일시적 서버 불가 |

### 9.2 토큰 에러

```json
{
  "error": "invalid_grant",
  "error_description": "The authorization code has expired"
}
```

---

## 10. 요약

RFC 6749 OAuth 2.0은 안전한 권한 위임을 위한 프레임워크입니다:

- 4가지 역할: Resource Owner, Client, Authorization Server, Resource Server
- 4가지 Grant Type: Authorization Code, Implicit, Password, Client Credentials
- 토큰 기반: Access Token + Refresh Token
- 권장 방식: Authorization Code + PKCE
- 보안 필수: HTTPS, state 파라미터, 범위 최소화

OAuth 2.0은 현대 웹 서비스의 인증/인가 표준 으로, Google, Facebook, GitHub 등 대부분의 서비스가 채택하고 있습니다.

---

## 참고 자료

- [RFC 6749 원문](https://www.rfc-editor.org/rfc/rfc6749)
- [RFC 6750 Bearer Token](https://www.rfc-editor.org/rfc/rfc6750)
- [RFC 7636 PKCE](https://www.rfc-editor.org/rfc/rfc7636)
- [OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)
